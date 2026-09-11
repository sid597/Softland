(ns softland.inland.render
  "Owned adapter from Electric node lifetimes to Softland's existing renderers.
   Takes text, paths, scene material and event delivery; gives a WebGPU canvas and
   positioned/hit geometry. Each surface owns its device, font providers, compositor,
   node systems, listeners and frame request. Nodes retain target realization only;
   accepted material and recipe results remain outside this layer. Node updates
   prepare scoped resources; each requested frame composites retained draw items.
   One surface currently has one shared workbench Region3D slot. Diagnostic window
   counters outlive disposed owners and are used by browser receipts."
  (:require [missionary.core :as m]
            [softland.inland.reactive :as reactive]
            [app.client.engine.device :as device]
            [app.client.engine.color :as color]
            [app.client.engine.transform :as transform]
            [app.client.engine.compositor :as compositor]
            [app.client.engine.leases :as leases]
            [app.client.path.renderer :as path-renderer]
            [app.client.text.fonts :as fonts]
            [app.client.text.layout :as layout]
            [app.client.text.renderer :as text-renderer]
            [app.client.region3d.renderer :as region-renderer]
            [app.client.region3d.scene :as region-scene]
            [softland.inland.scene :as scene]))

(def width 1440)
(def height 960)
(defn count!
  "Metric key → incremented browser diagnostic counter.
   Counters are page-scoped and intentionally survive render-surface disposal."
  [key]
  (let [stats (or (.-__inlandGPU js/window) #js {})]
    (aset stats key (inc (or (aget stats key) 0)))
    (set! (.-__inlandGPU js/window) stats)))

(defn work!
  "Occurrence id and metric key → incremented per-node diagnostic counter.
   Counts are evidence instrumentation, not retained application results."
  [id key]
  (let [stats (or (.-__inlandWork js/window) #js {})
        row (or (aget stats (str id)) #js {})]
    (aset row key (inc (or (aget row key) 0)))
    (aset stats (str id) row)
    (set! (.-__inlandWork js/window) stats)))

(declare present! remove-node!)

(defn request-frame!
  "Open surface → at most one scheduled animation frame.
   Coalesces requests, ignores closed owners, and records presentation exceptions
   for the browser receipt. It does not retry a failed draw automatically."
  [r]
  (when (and (not @(:!closed r)) (nil? @(:!raf r)))
    (reset! (:!raf r)
      (js/requestAnimationFrame
        (fn [_]
          (reset! (:!raf r) nil)
          (when-not @(:!closed r)
            (try (present! r)
                 (catch :default error
                   (js/console.error "Softland presentation failed" error)
                   (set! (.-__inlandRenderError js/window) (.-message error))))))))))

(defn point
  "Surface and pointer event → coordinates in the fixed 1440 × 960 canvas space.
   Requires a nonzero displayed canvas extent."
  [r event]
  (let [box (.getBoundingClientRect (:canvas r))]
    [(* width (/ (- (.-clientX event) (.-left box)) (.-width box)))
     (* height (/ (- (.-clientY event) (.-top box)) (.-height box)))]))

(defn inside?
  "[x y width height] and point → inclusive rectangle containment."
  [[x y w h] [px py]] (and (<= x px (+ x w)) (<= y py (+ y h))))

(defn target-at
  "Surface and logical point → highest-order matching registered hit action.
   Equal-order overlap has no separately specified tie-breaking policy."
  [r point]
  (some (fn [[_ {:keys [box action]}]] (when (inside? box point) action))
        (reverse (vec (sort-by (comp :order val) @(:!hits r))))))

(defn scene-hit
  "Surface and logical point → picked object id in the retained workbench scene.
   Checks scene rectangle before the shared Region3D ray picker; nil means no hit."
  [r [px py]]
  (when-let [{:keys [camera maintained material]} (get @(:!prepared (:region r)) :workbench)]
    (let [rect (or @(:!scene-rect r) [0 0 0 0])]
      (when (inside? rect [px py])
        (:object-id (region-scene/pick-region
                      {:maintained maintained :camera camera
                       :region-point [(- px (nth rect 0)) (- py (nth rect 1))]}))))))

(defn dispose!
  "Surface → one-time release of events, nodes, text parents/font providers,
   path/scene systems, compositor, buffers, canvas configuration and GPU device.
   Cancels scheduled frames and clears diagnostic hooks only if owned by this surface.
   Normal repeated disposal is ignored. Cleanup calls are not individually guarded;
   a throwing destructor can interrupt the sequence."
  [r]
  (when (compare-and-set! (:!closed r) false true)
    (when-let [raf @(:!raf r)] (js/cancelAnimationFrame raf))
    ((:stop-events r))
    (doseq [id (keys @(:!nodes r))] (remove-node! r id))
    (doseq [[_ parent] (:text-parents r)] (text-renderer/destroy-text-system! parent))
    (doseq [[_ asset] (:fonts r)]
      ((get-in asset [:layout-provider :dispose!] (fn [])))
      (count! "providers-closed"))
    (path-renderer/destroy-path-system! (:paths r))
    (region-renderer/destroy-region3d-system! (:region r))
    (compositor/destroy-compositor! (:compositor r))
    (.destroy (:camera r)) (.destroy (:groups r))
    (.unconfigure ^js (.getContext (:canvas r) "webgpu"))
    (.destroy (:gpu r))
    (.remove (:canvas r))
    (when (= (:id r) (.-__inlandRenderOwner js/window))
      (set! (.-__inlandTargetBox js/window) nil)
      (set! (.-__inlandPickAt js/window) nil)
      (set! (.-__inlandRenderOwner js/window) nil))
    (let [disposed (or (.-__inlandDisposed js/window) #js [])]
      (.push disposed (:id r))
      (set! (.-__inlandDisposed js/window) disposed))
    (count! "closed")))

(defn load-fonts!
  "Font manifest → promise of loaded assets in manifest order.
   Waits for every load; if one fails, disposes successful providers before rejecting."
  [manifest]
  (-> (js/Promise.allSettled (clj->js (mapv fonts/load-font-assets (:fonts manifest))))
      (.then (fn [results]
               (if-let [failed (some #(when (= "rejected" (.-status %)) %) (array-seq results))]
                 (do (doseq [result (array-seq results) :when (= "fulfilled" (.-status result))]
                       (when-let [dispose! (get-in (.-value result) [:layout-provider :dispose!])] (dispose!)))
                     (throw (.-reason failed)))
                 (mapv #(.-value %) (array-seq results)))))))

(defn acquire!
  "Generic event delivery callback → promise of an owned render surface.
   Requires WebGPU and the two manifest fonts in sans/mono order. Allocates canvas,
   device and shared renderer systems, then installs hit/pointer/resize delivery.
   Acquisition failures remove the canvas and release known font/device resources;
   normal complete acquisition is paired with dispose! by reactive/resource."
  [deliver]
  (when-not (.-gpu js/navigator)
    (throw (js/Error. "This workbench needs a browser with WebGPU enabled.")))
  (let [canvas (.createElement js/document "canvas")
        _ (set! (.-id canvas) "softland")
        _ (.setAttribute canvas "aria-label" "Softland instrument workbench")
        _ (.appendChild (.-body js/document) canvas)]
    (-> (.requestAdapter (.-gpu js/navigator))
        (.then (fn [^js adapter]
                 (when-not adapter (throw (js/Error. "No WebGPU adapter is available.")))
                 (let [^js info (.-info adapter)]
                   (set! (.-__inlandAdapter js/window)
                         #js {:vendor (.-vendor info) :architecture (.-architecture info)
                              :device (.-device info) :description (.-description info)}))
                 (.requestDevice adapter)))
        (.then
          (fn [gpu]
            (let [!assets (atom nil)]
              (-> (fonts/load-font-manifest-async)
                (.then load-fonts!)
                (.then
                  (fn [assets]
                    (reset! !assets assets)
                    (let [assets {:sans (nth assets 0) :mono (nth assets 1)}
                          camera (device/create-camera-buffer gpu)
                          groups (device/create-groups-buffer gpu)
                          transforms (transform/world-transforms (transform/empty-registry))
                          format (.getPreferredCanvasFormat (.-gpu js/navigator))
                          comp (compositor/create-compositor! gpu format)
                          region (region-renderer/init-region3d-system! gpu camera groups)
                          paths (path-renderer/init-path-system gpu "rgba16float" camera groups
                                  {:zoom 1 :pan [0 0]} :scene-color color/linear-premultiplied-color)
                          parents (into {} (for [[face asset] assets]
                                             [face (text-renderer/init-text-system gpu "rgba16float" camera asset
                                                     :groups-buffer groups :initial-capacity 64
                                                     :scene-color color/linear-premultiplied-color)]))
                          r {:id (str (random-uuid)) :gpu gpu :canvas canvas :format format :camera camera :groups groups
                             :transforms transforms :compositor comp :region region :paths paths
                             :fonts assets :text-parents parents
                             :!nodes (atom {}) :!hits (atom {}) :!raf (atom nil) :!closed (atom false)
                             :!scene-rect (atom nil) :!scene-dirty (atom false)}
                          click (fn [event]
                                  (let [p (point r event)]
                                    (when-let [action (or (target-at r p)
                                                         (when-let [id (scene-hit r p)] {:kind :point :id id}))]
                                      (deliver (assoc action :gesture-id (str (random-uuid)) :point p)))))
                          move (fn [event]
                                 (let [p (point r event)
                                       action (target-at r p)
                                       hit (or (when (= :point (:kind action)) (:id action)) (scene-hit r p))]
                                   (set! (.. canvas -style -cursor) (if (or action hit) "crosshair" "default"))
                                   (when hit (deliver {:kind :aim :id hit}))))
                          resize (fn [_] (request-frame! r))
                          observer (js/ResizeObserver. resize)]
                      (device/write-groups! gpu groups transforms)
                      (path-renderer/push! paths {:groups transforms})
                      (region-renderer/attach-compositor! region comp)
                      (.configure (.getContext canvas "webgpu") #js {:device gpu :format format :alphaMode "premultiplied"})
                      (.addEventListener canvas "click" click)
                      (.addEventListener canvas "pointermove" move)
                      (.observe observer canvas)
                      (.addEventListener gpu "uncapturederror"
                        #(do (count! "errors") (js/console.error "WebGPU" (.. % -error -message))))
                      (set! (.-__inlandTargetBox js/window)
                        (fn [key]
                          (when-let [box (:box (get @(:!hits r) key))]
                            (let [rect (.getBoundingClientRect canvas) scale (/ (.-width rect) width)]
                              (clj->js {:x (+ (.-left rect) (* scale (nth box 0)))
                                        :y (+ (.-top rect) (* scale (nth box 1)))
                                        :w (* scale (nth box 2)) :h (* scale (nth box 3))})))))
                      (set! (.-__inlandPickAt js/window)
                        (fn [x y] (scene-hit r (point r #js {:clientX x :clientY y}))))
                      (set! (.-__inlandRenderOwner js/window) (:id r))
                      (count! "opened")
                      (assoc r :stop-events (fn [] (.disconnect observer)
                                             (.removeEventListener canvas "click" click)
                                             (.removeEventListener canvas "pointermove" move))))))
                (.catch (fn [error]
                          (doseq [asset @!assets]
                            (when-let [dispose! (get-in asset [:layout-provider :dispose!])] (dispose!)))
                          (.destroy gpu) (.remove canvas) (throw error)))))))
        (.catch (fn [error] (.remove canvas) (throw error))))))

(defn open
  "Event callback → flow owning asynchronous surface acquisition and disposal.
   Cancellation during acquisition disposes a late surface through reactive/resource."
  [deliver]
  (reactive/resource #(acquire! deliver) dispose!))

(defn remove-node!
  "Surface and id → remove node/hit entries and release node-owned text work.
   Path removal reaches the path system while the surface is open. Scene preparation
   is surface-owned and is not cleared here; the single shared scene slot is not a
   per-node resource. Requests a frame unless the surface is already closed."
  [r id]
  (when-let [{:keys [kind system]} (get @(:!nodes r) id)]
    (case kind
      :text (text-renderer/destroy-text-system! system)
      :path (when-not @(:!closed r) (path-renderer/push! (:paths r) {:remove #{id}}))
      nil)
    (swap! (:!nodes r) dissoc id)
    (swap! (:!hits r) dissoc id)
    (work! id "closed")
    (request-frame! r)))

(defn node
  "Surface, unique id, kind, order and face → flow owning one node entry.
   Text clones borrow parent GPU assets but own their instance storage. Duplicate
   ids assert; cancellation removes the node and its hit entry."
  [r id kind order face]
  (m/observe
    (fn [emit]
      (let [n (cond-> {:id id :kind kind :order order}
                (= :text kind) (assoc :face face :system
                                 (text-renderer/clone-text-system (:gpu r) (get (:text-parents r) face) 64)))]
        (assert (not (contains? @(:!nodes r) id)) (str "Duplicate render identity " id))
        (swap! (:!nodes r) assoc id n)
        (work! id "opened")
        (emit id)
        #(remove-node! r id)))))

(defn text!
  "Mounted text node, content, box, size and RGBA → positioned layout.
   Shapes/wraps text and updates only that node's instance data. Optional viewport
   and cursor scroll the layout to the caret and install a paint clip; requests a frame."
  [r id text [x y w viewport-height cursor] size rgba]
  (let [{:keys [face system]} (get @(:!nodes r) id)
        asset (get (:fonts r) face)
        options {:text text :provider (:layout-provider asset)
                 :font-size size :line-height (* size 1.45)
                 :origin [x y] :inline-size w :wrap-policy :word}
        initial (layout/layout options)
        line (when cursor (:line (layout/source-offset->line-col initial cursor)))
        scroll (if (and viewport-height line)
                 (max 0 (- (* (+ line 2) size 1.45) viewport-height)) 0)
        positioned (if (pos? scroll) (layout/layout (assoc options :origin [x (- y scroll)])) initial)
        [red green blue alpha] rgba
        items (layout/line-paint-draw-items positioned
                {:container 0 :r red :g green :b blue :a alpha :size size})
        updated (text-renderer/update-text-data (:gpu r) system (mapv vector items) asset size :world-transforms (:transforms r))]
    (swap! (:!nodes r) update id assoc :system updated
      :clip (when viewport-height [x (- y size) w viewport-height]))
    (work! id "text-preparations")
    (request-frame! r)
    positioned))

(defn path!
  "Mounted path id and material → target upsert and scheduled frame.
   Shared path receipts distinguish geometry preparation from other path updates."
  [r id material]
  (let [receipt (path-renderer/push! (:paths r) {:upsert {id {:path/material material :container 0}}})]
    (work! id "path-updates")
    (when (seq (get-in receipt [:reran :geometry])) (work! id "geometry-preparations"))
    (request-frame! r)))

(defn hit!
  "Mounted node id, box, action and order → replaced hit entry.
   Stores target interaction geometry only; removal follows the node owner."
  [r id box action order]
  (swap! (:!hits r) assoc id {:box box :action action :order order}))

(defn projected-box
  "Camera, 3D points, canvas origin and margin → screen bounding box or nil.
   Only projectable points contribute; this is an interaction bound, not visibility proof."
  [camera points [ox oy] margin]
  (when-let [screen (seq (keep #(some-> (region-scene/project-point camera %) :screen) points))]
    (let [xs (map first screen) ys (map second screen)
          x (apply min xs) y (apply min ys)]
      [(+ ox x (- margin)) (+ oy y (- margin))
       (+ (- (apply max xs) x) (* 2 margin)) (+ (- (apply max ys) y) (* 2 margin))])))

(defn projection
  "Prepared surface, scene rectangle and group id → group/object screen boxes.
   Borrows maintained triangle vertices and camera; caller must prepare scene first."
  [r rect group]
  (let [{:keys [camera maintained]} (get @(:!prepared (:region r)) :workbench)
        points (into {} (for [[id triangles] (:triangles-by-object maintained)]
                          [id (mapcat (juxt :a :b :c) triangles)]))]
    (into {group (projected-box camera (mapcat val points) rect 22)}
      (for [[id vertices] points] [id (projected-box camera vertices rect 14)]))))

(defn scene!
  "Surface, authored shapes, rectangle and options → prepared scene and hit boxes.
   Updates the one :workbench region slot, marks its render pass dirty, and requests
   a frame. Scene identity/lifetime belong to the surface, not multiple occurrences."
  [r shapes [x y w h :as rect] options]
  (let [material (assoc (scene/region shapes w h options) :region/rect {:x x :y y :w w :h h})]
    (region-renderer/prepare-region3d-frame!
      (:region r) {:regions [{:region/material material :container 0}]} {}
      {:zoom 1 :dpr 1 :world-transforms (:transforms r)
       :session-layout-snapshot {:address :smalltalk :revision 0}
       :max-lease-size (:max-lease-size (:compositor r))})
    (reset! (:!scene-rect r) rect)
    (reset! (:!scene-dirty r) true)
    (count! "scene-preparations")
    (set! (.-__inlandScenePartCount js/window) (count shapes))
    (request-frame! r)
    (projection r rect (:group options))))

(defn present!
  "Open prepared surface → one submitted WebGPU frame.
   Resizes the canvas, encodes dirty 3D work, draws retained ordered text/path/scene
   nodes, presents via the shared compositor and releases frame leases after submit.
   Compositing all nodes here does not re-run their authored recipes or text layout."
  [{:keys [^js gpu ^js canvas camera compositor paths region] :as r}]
  (let [box (.getBoundingClientRect canvas)
        scale (* (/ (.-width box) width) (min 2 (.-devicePixelRatio js/window)))
        pw (js/Math.round (* width scale)) ph (js/Math.round (* height scale))
        _ (when (not= pw (.-width canvas)) (set! (.-width canvas) pw))
        _ (when (not= ph (.-height canvas)) (set! (.-height canvas) ph))
        _ (device/update-camera gpu camera (js/Float32Array. 6) 0 0 scale pw ph)
        {:keys [active stale]} (compositor/active-region-leases! compositor (region-renderer/binding-owner region))
        encoder (.createCommandEncoder gpu)
        _ (when @(:!scene-dirty r)
            (doseq [{id :region/id shadow? :shadow?} (leases/desired-rows (region-renderer/binding-owner region))
                    role (if shadow? [:shadow :interior] [:interior])]
              (region-renderer/encode-region-pass! region encoder id role (get active id)))
            (reset! (:!scene-dirty r) false))
        target (compositor/acquire-target! (:target-pool compositor) "rgba16float" pw ph "smalltalk/view")
        pass (compositor/begin-target-pass! encoder target "clear")]
    (doseq [[id {:keys [kind system clip]}] (sort-by (comp :order val) @(:!nodes r))]
      (when clip
        (let [[x y w h] (mapv #(js/Math.max 0 (js/Math.floor (* scale %))) clip)
              x (min pw x) y (min ph y)]
          (.setScissorRect pass x y (min w (- pw x)) (min h (- ph y)))))
      (case kind
        :path (let [[offset count] (path-renderer/item-range paths id)]
                (path-renderer/draw-path-instances! pass paths offset count))
        :text (text-renderer/draw-instances! pass
                {:pipeline (:pipeline system) :bind-group (:bind-group system)
                 :buffer (:instance-buffer system) :vertex-count 6
                 :instance-count (:num-instances system 0) :first-vertex 0 :first-instance 0
                 :scissor (when clip (mapv #(js/Math.max 0 (js/Math.floor (* scale %))) clip))}
                [pw ph])
        :scene (when @(:!scene-rect r) (region-renderer/composite-region! pass region :workbench))
        nil)
      (when clip (.setScissorRect pass 0 0 pw ph)))
    (.end pass)
    (compositor/draw-present! compositor encoder target
      (.createView ^js (.getCurrentTexture ^js (.getContext canvas "webgpu"))) (:format r))
    (.submit (.-queue gpu) #js [(.finish encoder)])
    (compositor/release-after-submit! compositor [target] [] stale)
    (count! "draws")))
