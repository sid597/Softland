(ns app.client.workspace.region3d-runtime
  "Flag-only Region3D focus, camera, pick, gizmo, and felt-fixture runtime.

   Loading is pure. `boot!` owns every capture listener and every fixture slot;
   the shared ground/mouse/event grammar stays byte-untouched. Session values
   join rendering through the renderer-edge snapshot and never enter store
   derivation or durable event vocabulary."
  (:require [app.client.substrate.region3d-material :as material]
            [app.client.substrate.region3d-placement :as placement]
            [app.client.substrate.region3d-scene :as scene]
            [app.client.substrate.webgpu.region3d-gpu :as region3d-gpu]
            [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.scene-runtime :as scene-runtime]
            [app.client.workspace.scene-store :as scene-store]))

(def fixture-underlay-vi :region3d/fixture-underlay)
(def fixture-region-vi :region3d/fixture-region)
(def fixture-overlay-vi :region3d/fixture-overlay)
(def fixture-region-id :region3d/felt-region)

(def ^:private panel-origin [236.0 146.0])
(def ^:private panel-members
  [[fixture-underlay-vi [0.0 0.0]]
   [fixture-region-vi [20.0 20.0]]
   [fixture-overlay-vi [20.0 20.0]]])
(def ^:private panel-handle-addresses
  #{:region3d/fixture-underlay
    :region3d/fixture-titlebar
    :region3d/fixture-title})
(def ^:private selectable-object-routes #{:object :object-glyph})
(def ^:private placed-content-routes #{:placed-text :placed-ink})

(defonce !session
  (atom {:version 1 :enabled? false :focused-region nil :regions {}
         :panel-position panel-origin
         :settled-diffs [] :focus-opens 0 :focus-closes 0}))
(defonce ^:private !mounted? (atom false))
(defonce ^:private !canvas (atom nil))
(defonce ^:private !io (atom {}))
(defonce ^:private !listeners (atom []))
(defonce ^:private !registrations (atom {}))
(defonce ^:private !pick-cache (atom {}))
(defonce ^:private !fixture-hooks (atom {}))

(defn install-fixture-hook! [id hook]
  (if hook
    (swap! !fixture-hooks assoc id hook)
    (swap! !fixture-hooks dissoc id))
  true)

(defn flag-enabled-search? [search]
  (= "1" (.get (js/URLSearchParams. (or search "")) "region3d")))

(defn enabled? []
  (flag-enabled-search? (.-search js/location)))

(defn fixture-mounted? [] @!mounted?)

(defn session-snapshot [] @!session)

(defn- tagged [r g b a]
  {:rgba [r g b a] :color-space :srgb :alpha-association :straight})

(defn- transform
  ([translation] (transform translation [1.0 1.0 1.0]))
  ([translation scale]
   {:translation translation :rotation [0.0 0.0 0.0 1.0] :scale scale}))

(defn- mesh-object [id kind translation scale color metallic roughness]
  {:object/id id :object/kind :mesh :parent nil
   :transform (transform translation scale)
   :provenance {:asserted-by :sid}
   :mesh {:kind kind :params (get material/primitive-defaults kind)}
   :material {:base-color color :metallic metallic :roughness roughness
              :emissive (tagged 0.0 0.0 0.0 1.0)}})

(defn fixture-region []
  (material/validate-region!
   {:region3d/version 1
    :extent {:width 720.0 :height 480.0 :depth 100.0}
    :background {:kind :opaque :color (tagged 0.075 0.085 0.105 1.0)}
    :ambient {:color (tagged 0.72 0.80 1.0 1.0) :intensity 0.14}
    :view-default {:pivot [0.0 0.7 0.0] :distance 10.5
                   :yaw 0.52 :pitch 0.34
                   :lens material/default-perspective-lens}
    :scene
    {:floor (mesh-object :floor :plane [0.0 -1.15 0.0] [9.0 1.0 7.0]
                         (tagged 0.30 0.32 0.36 1.0) 0.0 0.82)
     :left-box (mesh-object :left-box :box [-1.35 0.0 0.0] [2.2 2.2 2.2]
                            (tagged 0.56 0.61 0.69 1.0) 0.05 0.56)
     :cross-box (mesh-object :cross-box :box [0.25 0.25 -0.55] [2.5 1.3 1.7]
                             (tagged 0.32 0.48 0.72 1.0) 0.28 0.34)
     :sphere (mesh-object :sphere :sphere [2.15 -0.15 0.45] [1.55 1.55 1.55]
                          (tagged 0.72 0.42 0.22 1.0) 0.62 0.24)
     :glass (mesh-object :glass :box [0.75 0.85 1.5] [1.3 1.3 1.3]
                         (tagged 0.20 0.82 0.72 0.48) 0.08 0.20)
     :sun {:object/id :sun :object/kind :light :parent nil
           :transform (transform [4.0 7.0 5.0])
           :provenance {:asserted-by :sid}
           :light {:kind :directional :color (tagged 1.0 0.91 0.78 1.0)
                   :intensity 3.0 :cast-shadow true}}
     :fill {:object/id :fill :object/kind :light :parent nil
            :transform (transform [-3.0 2.5 4.0])
            :provenance {:asserted-by :sid}
            :light {:kind :point :color (tagged 0.38 0.58 1.0 1.0)
                    :intensity 38.0 :range 18.0 :cast-shadow false}}
     :spot {:object/id :spot :object/kind :light :parent nil
            :transform (transform [2.0 5.0 3.0])
            :provenance {:asserted-by :sid}
            :light {:kind :spot :color (tagged 1.0 0.36 0.24 1.0)
                    :intensity 26.0 :range 20.0
                    :cone {:inner-deg 18.0 :outer-deg 32.0}
                    :cast-shadow false}}
     :origin {:object/id :origin :object/kind :empty :parent nil
              :transform (transform [0.0 0.0 0.0])
              :provenance {:asserted-by :sid}}}}))

(defn- underlay-tree []
  (rt/rt-node
   :region3d/fixture-underlay-root :rect
   {:x 0.0 :y 0.0 :w 760.0 :h 520.0}
   :style {:bg [0.035 0.04 0.055 1.0] :radius 24.0
           :border-width 2.0 :border-color [0.20 0.24 0.32 1.0]}
   :data {:address :region3d/fixture-underlay}))

(defn- region-tree []
  (rt/rt-node
   :region3d/fixture-region-root :group
   {:x 0.0 :y 0.0 :w 720.0 :h 480.0}
   :children
   [(rt/rt-node
     :region3d/fixture-region-node :region3d
     {:x 0.0 :y 0.0 :w 720.0 :h 480.0}
     :data {:address :region3d/fixture-region
            :region3d/id fixture-region-id
            :region3d/scene (fixture-region)})]))

(defn- overlay-tree []
  (rt/rt-node
   :region3d/fixture-overlay-root :group
   {:x 0.0 :y 0.0 :w 720.0 :h 480.0}
   :children
   [(rt/rt-node
     :region3d/fixture-titlebar :rect
     {:x 0.0 :y 0.0 :w 720.0 :h 44.0}
     :style {:bg [0.045 0.052 0.070 0.94]
             :border-width 1.0 :border-color [0.22 0.27 0.36 1.0]}
     :data {:address :region3d/fixture-titlebar})
    (rt/rt-node
     :region3d/fixture-title :text {:x 18.0 :y 12.0 :w 680.0 :h 30.0}
     :text [{:text "REGION 3D · DRAG BAR · CLICK OBJECT · W MOVE  E ROTATE  R SCALE"
             :x 0.0 :y 0.0 :size 15.0 :type :keyword
             :r 0.88 :g 0.91 :b 0.98 :a 1.0}]
     :data {:address :region3d/fixture-title})]))

(defn- register! [vi tree x y sibling-rank]
  (scene-runtime/close-instance! vi)
  (let [registration
        (scene-runtime/register-face-instance!
         vi tree {:x x :y y :scale 1.0 :layer sibling-rank
                  :sibling-rank sibling-rank :stratum :world
                  :meta {:region3d-fixture? true
                         :material/id vi :material/revision 1}})]
    (swap! !registrations assoc vi registration)
    registration))

(defn install-fixture! []
  (when (enabled?)
    (register! fixture-underlay-vi (underlay-tree) 236.0 146.0 40)
    (register! fixture-region-vi (region-tree) 256.0 166.0 41)
    (register! fixture-overlay-vi (overlay-tree) 256.0 166.0 42)
    (swap! !session assoc :panel-position panel-origin)
    true))

(defn- session-region-value [region row]
  (let [region (reduce-kv
                (fn [value object-id transform]
                  (assoc-in value [:scene object-id :transform] transform))
                region (or (:settled-transforms row) {}))
        preview (:preview-transform row)]
    (if (and (:object-id preview) (:transform preview))
      (assoc-in region [:scene (:object-id preview) :transform]
                (:transform preview))
      region)))

(defn- pick-maintained [region-id region row]
  (let [region (session-region-value region row)
        key [region-id region]]
    (or (get @!pick-cache key)
        (let [maintained (assoc (scene/derive-scene region)
                                :region-id region-id)]
          (reset! !pick-cache {key maintained})
          maintained))))

(defn- selected-origin [maintained object-id]
  (when object-id
    (scene/transform-point
     (get-in maintained [:effective-transforms object-id])
     [0.0 0.0 0.0])))

(defn- gizmo-handles [maintained camera row]
  (scene/gizmo-handles (:effective-transforms maintained) camera
                       (:selection row) (or (:gizmo-mode row) :translate)))

(defn resolve-region-pick [hit]
  (let [region-id (:region-id hit)
        row (get-in @!session [:regions region-id] {})
        region (material/validate-region! (:region-material hit))
        prepared (region3d-gpu/prepared-pick-state region-id)
        maintained (or (:maintained prepared)
                       (pick-maintained region-id region row))
        viewport (or (:region-size hit)
                     [(get-in region [:extent :width])
                      (get-in region [:extent :height])])
        view (or (:view row) (:view-default region))
        camera (or (:camera prepared)
                   (scene/camera-matrices view viewport))
        resolved (scene/pick-region
                  {:maintained maintained :camera camera
                   :region-point (:region-local hit)
                   :gizmo-handles (gizmo-handles maintained camera row)
                   :placements (:placements prepared)
                   :placement-picker placement/pick-placement})]
    ;; The GPU-prepared maintained view is intentionally session-free and does
    ;; not carry the outer store row's region id.  Reassert that semantic id at
    ;; the edge so an internal background hit cannot overwrite it with nil.
    (merge hit resolved {:region-id region-id
                         :camera camera :region-material region})))

(defn- canvas-point [event]
  (let [rect (.getBoundingClientRect ^js @!canvas)]
    [(- (.-clientX event) (.-left rect))
     (- (.-clientY event) (.-top rect))]))

(defn- event-points [event]
  (let [screen (canvas-point event)
        world (if-let [screen->world (:screen->world @!io)]
                (screen->world screen)
                screen)]
    {:screen screen :world world}))

(defn- pick-at [event]
  (let [{:keys [screen world]} (event-points event)]
    (scene-runtime/pick-world {:screen screen :world world})))

(defn- panel-handle-hit? [hit]
  (contains? panel-handle-addresses (:address hit)))

(defn- current-container [vi]
  (:container (scene-store/slot (scene-runtime/store-snapshot) vi)))

(defn- set-panel-position! [[x y :as position]]
  ;; Resolve each current slot by VI. THE SEAM DEMO replaces the Region3D slot
  ;; after fixture boot, so a cached registration would move a stale container.
  (doseq [[vi [dx dy]] panel-members]
    (when-let [cid (current-container vi)]
      (scene-runtime/set-transform! cid {:x (+ x dx) :y (+ y dy)})))
  (swap! !session assoc :panel-position position)
  position)

(defn- start-panel-drag! [event]
  (let [world (:world (event-points event))]
    (swap! !session assoc :panel-drag
           {:start-world world
            :start-position (:panel-position @!session panel-origin)})
    (when (.-pointerId event)
      (.setPointerCapture ^js @!canvas (.-pointerId event)))
    true))

(defn- update-panel-drag! [event]
  (when-let [{[start-x start-y] :start-world
              [panel-x panel-y] :start-position} (:panel-drag @!session)]
    (let [[world-x world-y] (:world (event-points event))]
      (set-panel-position! [(+ panel-x (- world-x start-x))
                            (+ panel-y (- world-y start-y))]))
    true))

(defn- release-pointer! [event]
  (let [pointer-id (.-pointerId event)]
    (when (and @!canvas (some? pointer-id)
               (.hasPointerCapture ^js @!canvas pointer-id))
      (.releasePointerCapture ^js @!canvas pointer-id))))

(defn- consume! [event]
  (.preventDefault ^js event)
  (.stopPropagation ^js event)
  true)

(defn- record-hit! [hit]
  ;; Demo-facing receipt of the real reverse route.  Keep only semantic values;
  ;; camera matrices and material payloads stay owned by their existing rows.
  (swap! !session assoc :last-hit
         (select-keys hit [:route :region-id :object-id :address :t
                           :layout/id :material-local :text-hit :handle-id]))
  hit)

(defn- focus-region! [hit]
  (let [region-id (:region-id hit)
        region (:region-material hit)
        selectable? (contains? selectable-object-routes (:route hit))]
    (swap! !session
           (fn [state]
             (-> state
                 (assoc :enabled? true :focused-region region-id)
                 (update :focus-opens inc)
                 (update-in [:regions region-id]
                            (fn [row]
                              (cond-> (merge {:view (:view-default region)
                                              :display-mode :lit
                                              :gizmo-mode :translate
                                              :selection nil}
                                             row)
                                selectable?
                                (assoc :selection (:object-id hit))))))))
    true))

(defn end-focus! []
  (when (:focused-region @!session)
    (swap! !session #(-> % (assoc :focused-region nil)
                         (update :focus-closes inc))))
  true)

(defn- focused-hit? [hit]
  (= (:focused-region @!session) (:region-id hit)))

(defn- ray-plane-point [{:keys [origin direction]} plane-point plane-normal]
  (let [denominator (scene/dot direction plane-normal)]
    (when (> (js/Math.abs denominator) scene/ray-epsilon)
      (let [distance (/ (scene/dot (scene/v- plane-point origin) plane-normal)
                        denominator)]
        (when (pos? distance)
          (scene/v+ origin (scene/v* direction distance)))))))

(defn- start-drag! [event hit]
  (let [region-id (:region-id hit)
        row (get-in @!session [:regions region-id])
        point (canvas-point event)
        local (:region-local hit)
        drag
        (if (= :gizmo (:route hit))
          (let [object-id (:object-id hit)
                region (session-region-value (:region-material hit) row)
                maintained (pick-maintained region-id region row)
                before (get-in region [:scene object-id :transform])
                mode (first (:handle-id hit))
                axis-name (second (:handle-id hit))
                camera (:camera hit)
                pivot (or (:pivot hit)
                          (selected-origin maintained object-id))
                view-normal (scene/normalize (scene/v- (:eye camera) pivot))
                axis (or (:axis hit)
                         (get {:x [1.0 0.0 0.0] :y [0.0 1.0 0.0]
                               :z [0.0 0.0 1.0]} axis-name))
                plane-normal
                (or (:plane-normal hit)
                    (case mode
                      :rotate axis
                      :scale (if axis
                               (scene/normalize
                                (scene/cross axis
                                             (scene/cross view-normal axis)))
                               view-normal)
                      :translate
                      (if axis
                        (scene/normalize
                         (scene/cross axis (scene/cross view-normal axis)))
                        view-normal)
                      view-normal))
                start-ray (scene/ray-from-region-point camera local)]
            {:kind :gizmo :object-id object-id :handle-id (:handle-id hit)
             :before before :start-local local
             :start-ray start-ray :start-point (ray-plane-point
                                                 start-ray pivot plane-normal)
             :pivot pivot :plane-point pivot :plane-normal plane-normal :axis axis
             :camera camera})
          {:kind (if (.-shiftKey event) :pan :orbit)
           :start-screen point :last-screen point})]
    (swap! !session assoc-in [:regions region-id :drag] drag)
    (when (.-pointerId event)
      (.setPointerCapture ^js @!canvas (.-pointerId event)))
    drag))

(defn- axis-angle-quaternion [axis angle]
  (let [[x y z] (scene/normalize axis)
        half (/ angle 2.0)
        sine (js/Math.sin half)]
    [(* x sine) (* y sine) (* z sine) (js/Math.cos half)]))

(defn- quaternion-mul [[ax ay az aw] [bx by bz bw]]
  [(+ (* aw bx) (* ax bw) (* ay bz) (- (* az by)))
   (+ (* aw by) (- (* ax bz)) (* ay bw) (* az bx))
   (+ (* aw bz) (* ax by) (- (* ay bx)) (* az bw))
   (- (* aw bw) (* ax bx) (* ay by) (* az bz))])

(defn- update-gizmo-preview! [region-id drag local]
  (let [{:keys [before object-id handle-id camera start-ray plane-point
                plane-normal axis pivot start-point]} drag
        mode (first handle-id)
        axis-name (second handle-id)
        current-ray (scene/ray-from-region-point camera local)
        current-point (ray-plane-point current-ray pivot plane-normal)
        transform
        (case mode
          :translate
          (if-let [delta (scene/translate-delta start-ray current-ray
                                                plane-point plane-normal axis)]
            (update before :translation scene/v+ delta)
            before)

          :rotate
          (if (and start-point current-point axis)
            (let [angle (scene/rotate-delta start-point current-point pivot axis)]
              (assoc before :rotation
                     (material/normalize-quaternion
                      (quaternion-mul (axis-angle-quaternion axis angle)
                                      (:rotation before)))))
            before)

          :scale
          (if axis
            (if (and start-point current-point)
              (let [start-distance (scene/dot (scene/v- start-point pivot) axis)
                    current-distance (scene/dot (scene/v- current-point pivot) axis)
                    ratio (if (< (js/Math.abs start-distance) scene/ray-epsilon)
                            1.0
                            (max 0.001 (/ current-distance start-distance)))
                    axis-index ({:x 0 :y 1 :z 2} axis-name)]
                (update-in before [:scale axis-index]
                           #(max 0.001 (* % ratio))))
              before)
            (let [delta (+ (- (first local) (first (:start-local drag)))
                           (- (second (:start-local drag)) (second local)))
                  ratio (js/Math.exp (* delta 0.006))]
              (update before :scale
                      #(mapv (fn [value] (max 0.001 (* value ratio))) %))))

          before)]
    (swap! !session assoc-in [:regions region-id :preview-transform]
           {:object-id object-id :transform transform})))

(defn- settle-gizmo! [region-id]
  (let [row (get-in @!session [:regions region-id])
        drag (:drag row)
        preview (:preview-transform row)]
    (when (and (= :gizmo (:kind drag)) preview)
      (let [object-id (:object-id drag)
            after (:transform preview)
            diff (material/edit-diff
                  {:op :region3d/set-transform :region-id region-id
                   :object-id object-id :before (:before drag) :after after
                   :asserted-by :sid})]
        (swap! !session
               (fn [state]
                 (-> state
                     (assoc-in [:regions region-id :settled-transforms object-id]
                               after)
                     (update-in [:regions region-id] dissoc :preview-transform)
                     (update :settled-diffs conj diff))))))))

(defn- on-dblclick [event]
  (when-let [hit (pick-at event)]
    (record-hit! hit)
    (when (:region-id hit)
      (focus-region! hit)
      (consume! event))))

(defn- on-pointerdown [event]
  (let [focused (:focused-region @!session)
        hit (pick-at event)]
    (when hit (record-hit! hit))
    (cond
      ;; The title bar and exposed frame own panel relocation. Keeping this
      ;; outside the Region3D interior prevents a tiny hand wobble from turning
      ;; a panel move into orbit or object selection.
      (panel-handle-hit? hit)
      (do (start-panel-drag! event)
          (consume! event))

      ;; The first Region3D click is also its focus/selection gesture. Requiring
      ;; an undiscoverable double-click first made meshes and light glyphs look
      ;; inert and left W/E/R with no focused selection to operate on.
      (and (nil? focused) (:region-id hit))
      (do (focus-region! hit)
          (consume! event))

      (nil? focused)
      nil

      (not (focused-hit? hit))
      (end-focus!)

      :else
      (cond
        (= :gizmo (:route hit))
        (do (start-drag! event hit)
            (consume! event))

        (contains? selectable-object-routes (:route hit))
        (do
          (swap! !session assoc-in [:regions focused :selection]
                 (:object-id hit))
          (consume! event))

        ;; Placed text/ink owns its own semantic click route. It must not turn
        ;; into camera orbit merely because the pointer moved a few pixels.
        (contains? placed-content-routes (:route hit))
        (consume! event)

        :else
        (do (start-drag! event hit)
            (consume! event))))))

(defn- on-pointermove [event]
  (if (:panel-drag @!session)
    (do (update-panel-drag! event)
        (consume! event))
    (when-let [focused (:focused-region @!session)]
      (when-let [drag (get-in @!session [:regions focused :drag])]
        (let [hit (pick-at event)
              current (canvas-point event)
              [last-x last-y] (:last-screen drag current)
              [x y] current]
          (case (:kind drag)
            :orbit
            (swap! !session update-in [:regions focused :view]
                   scene/orbit (- x last-x) (- y last-y))

            :pan
            (swap! !session update-in [:regions focused :view]
                   scene/pan (- x last-x) (- y last-y)
                   (or (:region-size hit) [720.0 480.0]))

            :gizmo
            (when (:region-local hit)
              (update-gizmo-preview! focused drag (:region-local hit)))
            nil)
          (swap! !session assoc-in [:regions focused :drag :last-screen] current)
          (consume! event))))))

(defn- on-pointerup [event]
  (if (:panel-drag @!session)
    (do (swap! !session dissoc :panel-drag)
        (release-pointer! event)
        (consume! event))
    (when-let [focused (:focused-region @!session)]
      (when (get-in @!session [:regions focused :drag])
        (settle-gizmo! focused)
        (swap! !session update-in [:regions focused] dissoc :drag)
        (release-pointer! event)
        (consume! event)))))

(defn- on-wheel [event]
  (when-let [focused (:focused-region @!session)]
    (let [hit (pick-at event)]
      (when (focused-hit? hit)
        (swap! !session update-in [:regions focused :view]
               scene/dolly (.-deltaY event))
        (consume! event)))))

(defn- on-keydown [event]
  (when-let [focused (:focused-region @!session)]
    (case (.-code event)
      "Escape" (do (end-focus!) (consume! event))
      "KeyW" (do (swap! !session assoc-in [:regions focused :gizmo-mode]
                          :translate) (consume! event))
      "KeyE" (do (swap! !session assoc-in [:regions focused :gizmo-mode]
                          :rotate) (consume! event))
      "KeyR" (do (swap! !session assoc-in [:regions focused :gizmo-mode]
                          :scale) (consume! event))
      nil)))

(defn- listen! [target event handler opts]
  (.addEventListener ^js target event handler opts)
  (swap! !listeners conj [target event handler opts]))

(defn unmount! []
  (doseq [[target event handler opts] @!listeners]
    (.removeEventListener ^js target event handler opts))
  (reset! !listeners [])
  (doseq [vi [fixture-underlay-vi fixture-region-vi fixture-overlay-vi]]
    (scene-runtime/close-instance! vi))
  (reset! !registrations {})
  (reset! !mounted? false)
  (reset! !canvas nil)
  (scene-runtime/install-region-pick-resolver! nil)
  true)

(defn boot!
  "Mount the production-shaped fixture and its session listeners only behind
   `?region3d=1`. Repeated calls refresh IO without duplicating listeners."
  [canvas io]
  (reset! !io io)
  (when (and (enabled?) canvas)
    (reset! !canvas canvas)
    (scene-runtime/install-region-pick-resolver! resolve-region-pick)
    (when (compare-and-set! !mounted? false true)
      (install-fixture!)
      (doseq [[_ hook] @!fixture-hooks] (hook))
      (let [capture #js {:capture true}]
        (listen! canvas "dblclick" on-dblclick capture)
        (listen! canvas "pointerdown" on-pointerdown capture)
        (listen! canvas "pointermove" on-pointermove capture)
        (listen! canvas "pointerup" on-pointerup capture)
        (listen! canvas "pointercancel" on-pointerup capture)
        (listen! canvas "wheel" on-wheel #js {:capture true :passive false})
        (listen! js/window "keydown" on-keydown capture))
      (swap! !session assoc :enabled? true)
      (set! (.-region3d js/window)
            #js {:session (fn [] (clj->js @!session))
                 :focus (fn [] (name (or (:focused-region @!session) :none)))
                 :close end-focus!
                 :unmount unmount!})
      (js/console.log
       "[REGION3D] mounted — drag title/frame; click object; W/E/R gizmo; background orbit")))
  @!session)
