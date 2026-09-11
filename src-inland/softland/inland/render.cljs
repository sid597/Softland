(ns softland.inland.render
  "Owned presentation adapter for Electric. Each named node owns its engine
   resources; changes prepare only that node. A requested frame composites
   current GPU resources. No authored-state mirror or application result cache."
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
(defn count! [key]
  (let [stats (or (.-__inlandGPU js/window) #js {})]
    (aset stats key (inc (or (aget stats key) 0)))
    (set! (.-__inlandGPU js/window) stats)))

(defn work! [id key]
  (let [stats (or (.-__inlandWork js/window) #js {})
        row (or (aget stats (str id)) #js {})]
    (aset row key (inc (or (aget row key) 0)))
    (aset stats (str id) row)
    (set! (.-__inlandWork js/window) stats)))

(declare present! remove-node!)

(defn request-frame! [r]
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

(defn point [r event]
  (let [box (.getBoundingClientRect (:canvas r))]
    [(* width (/ (- (.-clientX event) (.-left box)) (.-width box)))
     (* height (/ (- (.-clientY event) (.-top box)) (.-height box)))]))

(defn inside? [[x y w h] [px py]] (and (<= x px (+ x w)) (<= y py (+ y h))))

(defn target-at [r point]
  (some (fn [[_ {:keys [box action]}]] (when (inside? box point) action))
        (reverse (vec (sort-by (comp :order val) @(:!hits r))))))

(defn scene-hit [r [px py]]
  (when-let [{:keys [camera maintained material]} (get @(:!prepared (:region r)) :workbench)]
    (let [rect (or @(:!scene-rect r) [0 0 0 0])]
      (when (inside? rect [px py])
        (:object-id (region-scene/pick-region
                      {:maintained maintained :camera camera
                       :region-point [(- px (nth rect 0)) (- py (nth rect 1))]}))))))

(defn dispose! [r]
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

(defn load-fonts! [manifest]
  (-> (js/Promise.allSettled (clj->js (mapv fonts/load-font-assets (:fonts manifest))))
      (.then (fn [results]
               (if-let [failed (some #(when (= "rejected" (.-status %)) %) (array-seq results))]
                 (do (doseq [result (array-seq results) :when (= "fulfilled" (.-status result))]
                       (when-let [dispose! (get-in (.-value result) [:layout-provider :dispose!])] (dispose!)))
                     (throw (.-reason failed)))
                 (mapv #(.-value %) (array-seq results)))))))

(defn acquire! [deliver]
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

(defn open [deliver]
  (reactive/resource #(acquire! deliver) dispose!))

(defn remove-node! [r id]
  (when-let [{:keys [kind system]} (get @(:!nodes r) id)]
    (case kind
      :text (text-renderer/destroy-text-system! system)
      :path (when-not @(:!closed r) (path-renderer/push! (:paths r) {:remove #{id}}))
      nil)
    (swap! (:!nodes r) dissoc id)
    (swap! (:!hits r) dissoc id)
    (work! id "closed")
    (request-frame! r)))

(defn node [r id kind order face]
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

(defn text! [r id text [x y w viewport-height cursor] size rgba]
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

(defn path! [r id material]
  (let [receipt (path-renderer/push! (:paths r) {:upsert {id {:path/material material :container 0}}})]
    (work! id "path-updates")
    (when (seq (get-in receipt [:reran :geometry])) (work! id "geometry-preparations"))
    (request-frame! r)))

(defn hit! [r id box action order]
  (swap! (:!hits r) assoc id {:box box :action action :order order}))

(defn projected-box [camera points [ox oy] margin]
  (when-let [screen (seq (keep #(some-> (region-scene/project-point camera %) :screen) points))]
    (let [xs (map first screen) ys (map second screen)
          x (apply min xs) y (apply min ys)]
      [(+ ox x (- margin)) (+ oy y (- margin))
       (+ (- (apply max xs) x) (* 2 margin)) (+ (- (apply max ys) y) (* 2 margin))])))

(defn projection [r rect group]
  (let [{:keys [camera maintained]} (get @(:!prepared (:region r)) :workbench)
        points (into {} (for [[id triangles] (:triangles-by-object maintained)]
                          [id (mapcat (juxt :a :b :c) triangles)]))]
    (into {group (projected-box camera (mapcat val points) rect 22)}
      (for [[id vertices] points] [id (projected-box camera vertices rect 14)]))))

(defn scene! [r shapes [x y w h :as rect] options]
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

(defn present! [{:keys [^js gpu ^js canvas camera compositor paths region] :as r}]
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
