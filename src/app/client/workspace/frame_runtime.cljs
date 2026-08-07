(ns app.client.workspace.frame-runtime
  "Flag-only W4 product join.

   Loading this namespace is pure. `boot!` installs session-only effect
   fixtures after chrome has published its selection census; `mount!` owns the
   export chord; `sync-pulse-deadline!` is the sink-edge bridge from selection
   state to the scheduler registry. No clock value enters scene derivation."
  (:require [app.client.substrate.frame-effects :as frame-effects]
            [app.client.substrate.frame-scheduler :as frame-scheduler]
            [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.scene-runtime :as scene-runtime]))

(def pulse-deadline-id :frame-runtime/selection-pulse)
(def pulse-cadence-ms (/ 1000.0 30.0))

(def opacity-underlay-vi :frame-runtime/opacity-underlay)
(def opacity-vi :frame-runtime/opacity-group)
(def nested-outer-vi :frame-runtime/nested-outer)
(def nested-inner-vi :frame-runtime/nested-inner)
(def mask-group-vi :frame-runtime/mask-group)
(def mask-source-vi :frame-runtime/mask-source)
(def backdrop-vi :frame-runtime/backdrop)
(def clip-vi :frame-runtime/gpu-clip)

(def opacity-underlay-origin {:x 70.0 :y 70.0})
(def opacity-underlay-size {:w 840.0 :h 340.0})
(def opacity-underlay-stripe-width 60.0)
(def backdrop-origin {:x 210.0 :y 210.0})
(def backdrop-size {:w 230.0 :h 130.0})

(defonce ^:private !booted? (atom false))
(defonce ^:private !mounted? (atom false))
(defonce ^:private !pulse-armed? (atom false))
(defonce ^:private !registrations (atom {}))
(defonce ^:private !receipt (atom nil))

(defn flag-enabled-search? [search]
  (= "1" (.get (js/URLSearchParams. (or search "")) "live-atoms")))

(defn enabled? []
  (flag-enabled-search? (.-search js/location)))

(defn- selection-count []
  (or (some-> (aget js/globalThis "__softlandChromeReceipt")
              (aget "selection-census")
              (aget "selected"))
      0))

(defn selection-active? []
  (pos? (selection-count)))

(defn- paint-rect [id bounds color radius]
  (rt/rt-node id :rect bounds
              :style {:bg color :radius radius}
              :data {:address id :frame-runtime? true}))

(defn- decorative-rect [id bounds color]
  (rt/rt-node id :rect bounds
              :style {:bg color :radius 0.0}
              :data {:frame-runtime? true :decorative? true}))

(defn- opacity-underlay-tree []
  ;; A non-selectable, non-effectful comparison surface. The real image/path/
  ;; connector children remain the opacity-group content; these alternating
  ;; bars sit behind them so 50% group coverage is visible instead of merely
  ;; reading as darker solids over the product's black ground.
  (rt/rt-node
   :frame-runtime/opacity-underlay-root :group
   (merge {:x 0.0 :y 0.0} opacity-underlay-size)
   :children
   (mapv (fn [index]
           (decorative-rect
            [:frame-runtime/opacity-underlay index]
            {:x (* index opacity-underlay-stripe-width)
             :y 0.0
             :w opacity-underlay-stripe-width
             :h (:h opacity-underlay-size)}
            (if (even? index)
              [0.08 0.10 0.14 1.0]
              [0.38 0.42 0.48 1.0])))
         (range 14))))

(defn- opacity-tree []
  (rt/rt-node
   :frame-runtime/opacity-root :group {:x 0.0 :y 0.0 :w 250.0 :h 150.0}
   :children
   [(paint-rect :frame-runtime/opacity-a
                {:x 8.0 :y 20.0 :w 150.0 :h 105.0}
                [0.96 0.32 0.38 1.0] 22.0)
    (paint-rect :frame-runtime/opacity-b
                {:x 92.0 :y 20.0 :w 150.0 :h 105.0}
                [0.22 0.66 0.98 1.0] 22.0)]))

(defn- live-opacity-group-tree []
  ;; The real image/path/connector fixtures remain the paint owners. This node
  ;; contributes only their common container path and therefore proves group
  ;; opacity over mixed content instead of introducing verifier-lookalike art.
  (rt/rt-node :frame-runtime/live-opacity-root :group
              {:x 0.0 :y 0.0 :w 1040.0 :h 520.0}
              :children []))

(defn- nested-outer-tree []
  (rt/rt-node
   :frame-runtime/nested-outer-root :group
   {:x 0.0 :y 0.0 :w 190.0 :h 125.0}
   :children [(paint-rect :frame-runtime/nested-base
                          {:x 0.0 :y 0.0 :w 190.0 :h 125.0}
                          [0.13 0.16 0.24 0.9] 18.0)]))

(defn- nested-inner-tree []
  (rt/rt-node
   :frame-runtime/nested-inner-root :group
   {:x 0.0 :y 0.0 :w 150.0 :h 90.0}
   :children [(paint-rect :frame-runtime/nested-inner-a
                          {:x 0.0 :y 0.0 :w 100.0 :h 90.0}
                          [0.95 0.72 0.20 1.0] 16.0)
              (paint-rect :frame-runtime/nested-inner-b
                          {:x 50.0 :y 0.0 :w 100.0 :h 90.0}
                          [0.36 0.90 0.62 1.0] 16.0)]))

(defn- mask-group-tree []
  (rt/rt-node
   :frame-runtime/mask-root :group {:x 0.0 :y 0.0 :w 210.0 :h 130.0}
   :children [(paint-rect :frame-runtime/mask-content-a
                          {:x 0.0 :y 0.0 :w 210.0 :h 130.0}
                          [0.75 0.24 0.92 1.0] 12.0)
              (paint-rect :frame-runtime/mask-content-b
                          {:x 22.0 :y 22.0 :w 166.0 :h 86.0}
                          [0.18 0.86 0.80 1.0] 8.0)]))

(defn- mask-source-tree []
  (rt/rt-node
   :frame-runtime/mask-source-root :group {:x 0.0 :y 0.0 :w 210.0 :h 130.0}
   :children [(paint-rect :frame-runtime/mask-source-shape
                          {:x 35.0 :y 15.0 :w 140.0 :h 100.0}
                          [1.0 1.0 1.0 0.88] 50.0)]))

(defn- backdrop-tree []
  (rt/rt-node
   :frame-runtime/backdrop-root :group
   (merge {:x 0.0 :y 0.0} backdrop-size)
   :children [(paint-rect :frame-runtime/backdrop-panel
                          (merge {:x 0.0 :y 0.0} backdrop-size)
                          [0.82 0.90 1.0 0.26] 20.0)]))

(def clip-label-overhang-px -6.0)

(defn- clip-card [id x color label]
  (rt/rt-node
   [id :clip] :group {:x x :y 0.0 :w 145.0 :h 112.0}
   :clip? true
   :data {:gpu-clip? true}
   :children
   [(paint-rect [id :rounded]
                {:x 0.0 :y 12.0 :w 138.0 :h 88.0}
                color 30.0)
    (rt/rt-node
     [id :text] :text {:x clip-label-overhang-px :y 34.0
                       :w 145.0 :h 42.0}
     :text [{:text label :x 0.0 :y 0.0 :size 20.0
             :r 1.0 :g 1.0 :b 1.0 :a 1.0 :type :normal}]
     :data {:address [id :text] :frame-runtime? true})]))

(defn- clip-tree []
  (rt/rt-node
   :frame-runtime/clip-root :group {:x 0.0 :y 0.0 :w 330.0 :h 112.0}
   :children [(clip-card :frame-runtime/clip-a 0.0
                         [0.96 0.45 0.22 1.0] "partial glyph")
              (clip-card :frame-runtime/clip-b 180.0
                         [0.22 0.72 0.96 1.0] "second clip")]))

(defn felt-fixture-receipt
  "Structural receipt for the three W4 demonstrations Sid must be able to
   recognize without reconstructing the engine contract from the picture."
  []
  (let [underlay (opacity-underlay-tree)
        clip (clip-tree)
        clip-text-nodes (->> (tree-seq (comp seq :children) :children clip)
                             (filter #(= :text (:type %)))
                             vec)
        backdrop-left (:x backdrop-origin)
        backdrop-right (+ backdrop-left (:w backdrop-size))
        stripe-left (:x opacity-underlay-origin)
        stripe-count (count (:children underlay))
        detail-boundaries
        (->> (range 1 stripe-count)
             (map #(+ stripe-left (* % opacity-underlay-stripe-width)))
             (filter #(< backdrop-left % backdrop-right))
             vec)]
    {:opacity-underlay-stripes (count (:children underlay))
     :opacity-underlay-colors
     (count (set (map #(get-in % [:style :bg]) (:children underlay))))
     :backdrop-detail-boundaries detail-boundaries
     :backdrop-overlaps-detail? (>= (count detail-boundaries) 2)
     :clip-text-overhangs (mapv #(get-in % [:bounds :x]) clip-text-nodes)
     :clip-labels (mapv #(get-in % [:text 0 :text]) clip-text-nodes)}))

(defn- register! [vi tree opts]
  (scene-runtime/close-instance! vi)
  (let [registration (scene-runtime/register-face-instance!
                      vi tree
                      (merge {:scale 1.0 :stratum :world
                              :meta {:live-atoms? true
                                     :frame-runtime? true
                                     :material/id vi
                                     :material/revision 1}}
                             opts))]
    (swap! !registrations assoc vi registration)
    registration))

(defn install-live-opacity-group!
  "Mint the common 50% parent used by live-atoms' real image, path, and
   connector fixtures. Called before those children register and before boot!
   installs the remaining felt cases."
  []
  (when (enabled?)
    (register! opacity-vi (live-opacity-group-tree)
               {:x 0.0 :y 0.0 :layer 8 :sibling-rank 8
                :effects {:opacity 0.5}})))

(defn- install-fixtures! []
  (let [opacity-underlay
        (register! opacity-underlay-vi (opacity-underlay-tree)
                   (merge opacity-underlay-origin
                          {:layer 7 :sibling-rank 7}))
        opacity (or (get @!registrations opacity-vi)
                    (register! opacity-vi (opacity-tree)
                               {:x 70.0 :y 520.0 :layer 12 :sibling-rank 12
                                :effects {:opacity 0.5}}))
        nested-outer (register! nested-outer-vi (nested-outer-tree)
                                {:x 360.0 :y 520.0 :layer 13 :sibling-rank 13
                                 :effects {:opacity 0.72 :isolate? true}})
        nested-inner (register! nested-inner-vi (nested-inner-tree)
                                {:x 20.0 :y 18.0 :layer 1 :sibling-rank 1
                                 :parent (:container nested-outer)
                                 :effects {:opacity 0.55}})
        mask-group (register! mask-group-vi (mask-group-tree)
                              {:x 600.0 :y 510.0 :layer 14 :sibling-rank 14
                               :effects {:mask mask-source-vi}})
        mask-source (register! mask-source-vi (mask-source-tree)
                               {:x 0.0 :y 0.0 :layer 1 :sibling-rank 1
                                :parent (:container mask-group)})
        backdrop (register! backdrop-vi (backdrop-tree)
                            ;; Cross the real pressure/self-cross path and the
                            ;; striped comparison surface: crisp detail remains
                            ;; visible outside the panel, blurred detail inside.
                            (merge backdrop-origin
                                   {:layer 15 :sibling-rank 15
                             :effects
                             {:backdrop-blur
                              {:radius-world 16.0 :max-px 64.0
                               :algorithm-version
                               frame-effects/blur-algorithm-version}}}))
        clip (register! clip-vi (clip-tree)
                        {:x 850.0 :y 500.0 :layer 16 :sibling-rank 16})]
    {:opacity-underlay opacity-underlay
     :opacity opacity :nested-outer nested-outer :nested-inner nested-inner
     :mask-group mask-group :mask-source mask-source
     :backdrop backdrop :clip clip}))

(defn- compositor-receipt []
  (when-let [provider (aget js/globalThis "__softlandFrameCompositor")]
    (when-let [receipt-fn (aget provider "receipt")]
      (js->clj (receipt-fn) :keywordize-keys true))))

(defn- publish-receipt! [& [export]]
  (when @!booted?
    (let [plan (some-> (aget js/globalThis "__softlandFramePlanReceipt")
                       (js->clj :keywordize-keys true))
          compositor (compositor-receipt)
          receipt (cond-> (merge {:enabled true
                                  :fixtures (vec (keys @!registrations))
                                  :color-mode (or (:color-mode plan)
                                                  (:color-mode compositor)
                                                  :legacy)
                                  :passes (or (:passes plan)
                                              (:passes compositor)
                                              [])}
                                 (select-keys (or @!receipt {}) [:export]))
                    export (assoc :export export))]
      (reset! !receipt receipt)
      (aset js/globalThis "__softlandFrameRuntimeReceipt" (clj->js receipt))
      receipt)))

(defn sync-pulse-deadline!
  "Re-arm the 30 Hz deadline while selection is live, and retire it immediately
   after selection clears. Called only at the frame sink before scheduler decide."
  [logical-time]
  (when @!booted?
    (let [active? (selection-active?)]
      (cond
        (and active? (not @!pulse-armed?))
        (do
          (frame-scheduler/register-deadline!
           pulse-deadline-id
           {:next-deadline (double logical-time)
            :cadence pulse-cadence-ms
            :stop-predicate #(not (selection-active?))})
          (reset! !pulse-armed? true))

        (and (not active?) @!pulse-armed?)
        (do (frame-scheduler/unregister-deadline! pulse-deadline-id)
            (reset! !pulse-armed? false)))
      active?)))

(defn- download-png! [bytes]
  (let [blob (js/Blob. #js [bytes] #js {:type "image/png"})
        url (.createObjectURL js/URL blob)
        anchor (.createElement js/document "a")]
    (set! (.-href anchor) url)
    (set! (.-download anchor) "softland-world.png")
    (.click anchor)
    (js/setTimeout #(.revokeObjectURL js/URL url) 0)))

(defn export-viewport! []
  (if-let [provider (aget js/globalThis "__softlandFrameCompositor")]
    (if-let [export-fn (aget provider "exportViewport")]
      (-> (export-fn)
          (.then (fn [result]
                   (let [row (js->clj result :keywordize-keys true)
                         bytes (or (aget result "bytes") (:bytes row))
                         metadata (or (:metadata row) {})]
                     (download-png! bytes)
                     (publish-receipt! metadata)
                     result)))
          (.catch (fn [error]
                    (js/console.error "[FRAME-RUNTIME/EXPORT-FAIL]" error)
                    (throw error))))
      (js/Promise.reject (js/Error. "Frame export provider is incomplete")))
    (js/Promise.reject
     (js/Error. "Frame export becomes available after the first effect frame"))))

(defn- export-keydown! [event]
  (when (and (.-shiftKey event)
             (or (.-ctrlKey event) (.-metaKey event))
             (= "e" (.toLowerCase (.-key event))))
    (.preventDefault event)
    (export-viewport!)))

(defn mount!
  "Install the flag-only export chord (Ctrl/Cmd+Shift+E), once."
  []
  (when (and @!booted? (compare-and-set! !mounted? false true))
    (.addEventListener js/window "keydown" export-keydown!))
  (publish-receipt!))

(defn boot!
  "Install the W4 felt fixtures after chrome boot. Idempotent across hot reload."
  []
  (when (enabled?)
    (when (compare-and-set! !booted? false true)
      (install-fixtures!))
    (publish-receipt!)))

(defn receipt []
  (publish-receipt!))
