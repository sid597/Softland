(ns app.client.workspace.chrome-runtime
  "Flag-boot wiring for CHROME. The namespace is load-pure: boot installs the
   two verb stranglers, selection clear/tap hooks, live providers, and the
   session-only slot lifecycle."
  (:require [app.client.substrate.chrome-derive :as chrome-derive]
            [app.client.substrate.snap :as snap]
            [app.client.workspace.containers :as containers]
            [app.client.workspace.ground :as ground]
            [app.client.workspace.scene-runtime :as scene-runtime]
            [app.client.workspace.scene-store :as scene-store]
            [app.client.workspace.selection :as selection]))

(defonce ^:private !booted? (atom false))
(defonce ^:private !derive-state (atom (chrome-derive/empty-state)))
(defonce ^:private !registrations (atom {}))
(defonce ^:private !selection-io (atom nil))
(defonce ^:private !snap-state (atom (snap/empty-state)))
(defonce ^:private !gesture (atom nil))
(defonce ^:private !drag (atom nil))
(defonce ^:private !receipt (atom nil))

(defn- publish-live-receipt! []
  (when-let [receipt @!receipt]
    (aset js/globalThis "__softlandChromeReceipt" (clj->js receipt)))
  @!receipt)

(defn- observe-fixture-press! [kind]
  (when @!receipt
    (swap! !receipt
           #(-> %
                (update :fixture-press-observed (fnil inc 0))
                (assoc :last-fixture-press-kind kind
                       :last-fixture-press-route :selection/marquee-begin)))
    (publish-live-receipt!)))

(defn- store-frame []
  (scene-store/derive-store-frame (scene-runtime/store-snapshot)))

(defn- target-row [identity frame]
  (some (fn [row]
          (when (= identity (selection/row-identity row)) row))
        (selection/target-rows (:targets-by-address frame))))

(defn- hit-node [hit]
  (last (:path hit)))

(defn- hit-identity [hit]
  (when (and (:vi hit) (:address hit))
    {:vi (:vi hit) :address (:address hit)}))

(defn- close-registration! [slot-id]
  (when-let [{:keys [vi]} (get @!registrations slot-id)]
    (scene-runtime/close-instance! vi)
    (swap! !registrations dissoc slot-id)))

(defn- update-registration-tree! [vi tree]
  (when-let [slot (scene-store/slot (scene-runtime/store-snapshot) vi)]
    (swap! scene-runtime/!scene-store scene-store/upsert-slot vi
           {:tree tree
            :container (:container slot)
            :container-slot (:container-slot slot)
            :stack-path (:stack-path slot)
            :meta (:meta slot)
            :stratum :overlay})))

(defn- install-slot! [slot-id spec effective]
  (let [vi [:chrome/slot slot-id]
        registration
        (scene-runtime/register-face-instance!
         vi (:tree spec)
         {:x 0.0 :y 0.0 :scale 1.0 :layer -1 :sibling-rank -1
          :stratum :overlay
          :meta {:chrome? true :material/id slot-id
                 :material/revision chrome-derive/algorithm-version}})]
    (when-let [target-effective (get effective (:target-container spec))]
      (scene-runtime/set-transform! (:container registration)
                                    {:affine (:affine target-effective)}))
    (swap! !registrations assoc slot-id
           {:vi vi :container (:container registration) :spec spec})
    registration))

(defn- reconcile-slots! [derive-state]
  (let [desired (:slots derive-state)
        effective (scene-runtime/effective-transforms)
        existing @!registrations]
    (doseq [slot-id (remove #(contains? desired %) (keys existing))]
      (close-registration! slot-id))
    (doseq [[slot-id desired-spec] desired]
      (if-let [{:keys [vi spec]} (get @!registrations slot-id)]
        (when (not= (:tree spec) (:tree desired-spec))
          (update-registration-tree! vi (:tree desired-spec))
          (swap! !registrations assoc-in [slot-id :spec] desired-spec))
        (install-slot! slot-id desired-spec effective)))
    derive-state))

(defn- project-legacy! [selection-state]
  (when-let [{:keys [write-legacy! block-unit-ids]} @!selection-io]
    (write-legacy!
     (selection/legacy-projection selection-state (block-unit-ids)))))

(defn- install-derived! [next-state]
  (reset! !derive-state next-state)
  (reconcile-slots! next-state)
  (project-legacy! (:selection next-state))
  next-state)

(defn- publish-selection-census! [derive-state frame]
  (when @!receipt
    (swap! !receipt assoc
           :selection-census
           (selection/selection-census (:selection derive-state)
                                       (:targets-by-address frame)))
    (publish-live-receipt!))
  derive-state)

(defn- apply-selection-transition! [transition]
  (let [frame (store-frame)
        next-state (chrome-derive/apply-transition
                    @!derive-state transition frame @!gesture
                    (:snap-result @!gesture))]
    (-> (install-derived! next-state)
        (publish-selection-census! frame))))

(defn- refresh-gesture! [gesture snap-result]
  (reset! !gesture (assoc (or gesture {}) :snap-result snap-result))
  (let [frame (store-frame)
        next-state (chrome-derive/apply-selection
                    @!derive-state (:selection @!derive-state) frame
                    @!gesture snap-result)]
    (install-derived! next-state)))

(defn- clear-gesture! []
  (reset! !gesture nil)
  (reset! !snap-state (snap/empty-state))
  (let [frame (store-frame)]
    (install-derived!
     (chrome-derive/apply-selection
      @!derive-state (:selection @!derive-state) frame nil nil))))

(defn- clear-selection! []
  (apply-selection-transition! {:op :clear}))

(defn- shift-tap! [{:keys [hit]}]
  (let [node (hit-node hit)
        chrome-form (get-in node [:data :chrome/material :chrome/form])
        identity (hit-identity hit)]
    (when (and identity (nil? chrome-form))
      (when @!receipt
        (swap! !receipt assoc :last-shift-tap-identity identity))
      (apply-selection-transition! {:op :toggle :identity identity}))))

(defn frame-edge!
  "Value-diff target effective transforms and update only their bound root-level
   chrome containers. Static frames and camera-only motion write nothing."
  []
  (when @!booted?
    (let [effective (scene-runtime/effective-transforms)
          next-state (chrome-derive/follow-transforms @!derive-state effective)]
      (doseq [slot-id (:transform-writes next-state)]
        (when-let [spec (get-in next-state [:slots slot-id])]
          (when-let [target-effective (get effective (:target-container spec))]
            (when-let [chrome-container (get-in @!registrations [slot-id :container])]
              (scene-runtime/set-transform! chrome-container
                                            {:affine (:affine target-effective)})))))
      (reset! !derive-state next-state)
      (:frame-receipt next-state))))

(defn- call-original! [!original continuation ctx]
  (when-let [impl (get @!original continuation)]
    (impl ctx)))

(defn- current-zoom []
  (double (or (:zoom (ground/camera-snapshot)) 1.0)))

(defn- snap-step! [identity raw-position raw-bounds]
  (let [frame (store-frame)
        candidates (snap/extract-candidates
                    (:targets-by-address frame)
                    (scene-runtime/effective-transforms)
                    identity)
        step (snap/gesture-step
              @!snap-state
              {:raw-position raw-position :raw-bounds raw-bounds
               :candidates candidates :zoom (current-zoom)})]
    (reset! !snap-state (:state step))
    (:result step)))

(defn- begin-block-drag! [ctx]
  (let [{:keys [press subject]} ctx
        identity {:vi (get-in press [:press/hit :vi]) :address subject}
        frame (store-frame)
        row (target-row identity frame)
        bounds (when row (selection/row-world-bounds
                          (assoc row :address subject)
                          (scene-runtime/effective-transforms)))
        [wx wy] (:press/world press)
        [gx gy] (:press/grab press)]
    (when (and row bounds gx gy)
      (reset! !drag {:kind :block :identity identity
                     :bounds bounds :start-position [(+ wx gx) (+ wy gy)]}))))

(defn- block-drag-wrapper [!original]
  {:begin
   (fn [ctx]
     (call-original! !original :begin ctx)
     (begin-block-drag! ctx))
   :move
   (fn [{:keys [world] :as ctx}]
     (if-let [{:keys [identity bounds start-position]} @!drag]
       (let [[wx wy] world
             [gx gy] (get-in ctx [:press :press/grab])
             raw-position [(+ wx gx) (+ wy gy)]
             [sx sy] start-position
             raw-bounds (-> bounds
                            (update :x + (- (first raw-position) sx))
                            (update :y + (- (second raw-position) sy)))
             result (snap-step! identity raw-position raw-bounds)
             [dx dy] (:delta result)]
         (call-original! !original :move
                         (assoc ctx :world [(+ wx dx) (+ wy dy)]))
         (refresh-gesture! {:id :chrome/block-drag} result)
         (frame-edge!))
       (call-original! !original :move ctx)))
   :end
   (fn [ctx]
     (call-original! !original :end ctx)
     (reset! !drag nil)
     (clear-gesture!))})

(defn- fixture-drag-context [ctx]
  (let [hit (get-in ctx [:press :press/hit])
        node (hit-node hit)
        handle-material (get-in node [:data :chrome/material])
        handle? (= :handle (:chrome/form handle-material))
        identity (if handle? (:chrome/derived-from handle-material)
                     (hit-identity hit))
        frame (store-frame)
        row (when identity (target-row identity frame))
        fixture? (contains? chrome-derive/fixture-families (:family row))
        effective (when row (get (scene-runtime/effective-transforms)
                                 (:container row)))
        bounds (when row (selection/row-world-bounds
                          (assoc row :address (:address identity))
                          (scene-runtime/effective-transforms)))]
    (cond
      (and handle? row effective)
      {:kind :fixture-scale :identity identity :row row :effective effective
       :bounds bounds :corner (:chrome/corner handle-material)}

      (and fixture? effective)
      {:kind :fixture-translate :identity identity :row row :effective effective
       :bounds bounds}

      (nil? hit)
      {:kind :marquee}

      :else {:kind :noop})))

(defn- opposite-corner [corner]
  ({:nw :se :ne :sw :sw :ne :se :nw} corner))

(defn- local-corner [{:keys [x y w h]} corner]
  (case corner
    :nw [x y] :ne [(+ x w) y]
    :sw [x (+ y h)] :se [(+ x w) (+ y h)]))

(defn- begin-marquee-or-fixture! [{:keys [press world] :as ctx}]
  (let [mode (fixture-drag-context ctx)
        [wx wy] world]
    (when (contains? #{:fixture-translate :fixture-scale} (:kind mode))
      (observe-fixture-press! (:kind mode)))
    (case (:kind mode)
      :marquee
      (do (clear-selection!)
          (reset! !drag (assoc mode :press-world (:press/world press)))
          (refresh-gesture!
           {:id :chrome/marquee
            :marquee (selection/marquee-rect (:press/world press) [wx wy])}
           nil))

      :fixture-translate
      (let [[_a _b _c _d tx ty] (:affine (:effective mode))]
        (when-not (contains? (get-in @!derive-state [:selection :members])
                             (:identity mode))
          (apply-selection-transition! {:op :toggle :identity (:identity mode)}))
        (reset! !drag (assoc mode :press-world (:press/world press)
                            :start-position [tx ty])))

      :fixture-scale
      (let [opposite (opposite-corner (:corner mode))
            opposite-local (local-corner (get-in mode [:row :bounds]) opposite)
            opposite-world (containers/forward-point (:effective mode)
                                                     opposite-local)]
        (reset! !drag (assoc mode :opposite-local opposite-local
                            :opposite-world opposite-world)))

      (reset! !drag mode))))

(defn- move-marquee-or-fixture! [{:keys [press world]}]
  (let [[wx wy] world]
    (case (:kind @!drag)
      :marquee
      (refresh-gesture!
       {:id :chrome/marquee
        :marquee (selection/marquee-rect (:press/world press) [wx wy])}
       nil)

      :fixture-translate
      (let [{:keys [identity row bounds press-world start-position]} @!drag
            [px py] press-world
            [tx ty] start-position
            raw-position [(+ tx (- wx px)) (+ ty (- wy py))]
            raw-bounds (-> bounds
                           (update :x + (- (first raw-position) tx))
                           (update :y + (- (second raw-position) ty)))
            result (snap-step! identity raw-position raw-bounds)
            [x y] (:position result)]
        (scene-runtime/set-transform! (:container row) {:x x :y y})
        (refresh-gesture! {:id :chrome/fixture-drag} result)
        (frame-edge!))

      :fixture-scale
      (let [{:keys [row bounds corner opposite-local opposite-world]} @!drag
            moving-local (local-corner (get row :bounds) corner)
            [ox oy] opposite-local
            [mx my] moving-local
            local-dx (abs (- mx ox))
            local-dy (abs (- my oy))
            [owx owy] opposite-world
            scale-x (when (pos? local-dx) (/ (abs (- wx owx)) local-dx))
            scale-y (when (pos? local-dy) (/ (abs (- wy owy)) local-dy))
            scale (max 0.05 (double (or (max (or scale-x 0.0)
                                              (or scale-y 0.0)) 1.0)))
            tx (- owx (* scale ox))
            ty (- owy (* scale oy))]
        (scene-runtime/set-transform! (:container row)
                                      {:affine [scale 0.0 0.0 scale tx ty]})
        (frame-edge!))
      nil)))

(defn- end-marquee-or-fixture! [{:keys [press world]}]
  (when (= :marquee (:kind @!drag))
    (let [[wx wy] world
          frame (store-frame)
          hits (selection/marquee-hits
                (selection/marquee-rect (:press/world press) [wx wy])
                (:targets-by-address frame)
                (scene-runtime/effective-transforms))]
      (apply-selection-transition! {:op :marquee-commit :identities hits})))
  (reset! !drag nil)
  (clear-gesture!))

(defn- marquee-wrapper [_!original]
  {:begin begin-marquee-or-fixture!
   :move move-marquee-or-fixture!
   :end end-marquee-or-fixture!})

(defn- publish-receipt! [chrome-system]
  (let [receipt {:verbs-rebound [:selection/marquee-begin :placement/drag-group]
                 :prev-impls-captured true
                 :clear-seam-installed true
                 :providers-installed true
                 :fixture-press-binding :selection/marquee-begin
                 :fixture-press-observed 0
                 :last-fixture-press-route nil
                 :last-shift-tap-identity nil
                 :selection-census {:selected 0 :pruned 0
                                    :blocks 0 :fixtures 0 :edges 0}
                 :camera-pan-rebound false
                 :chrome-system (boolean chrome-system)}]
    (reset! !receipt receipt)
    (publish-live-receipt!)))

(defn boot!
  "Activate the dark lane once. Re-registration is the checked last-wins door;
   each wrapper captures the implementation it composes or replaces."
  [chrome-system]
  (if @!booted?
    @!receipt
    (do
      (chrome-derive/set-live-camera-provider! ground/camera-snapshot)
      (chrome-derive/set-live-effective-provider!
       scene-runtime/effective-transforms)
      (reset! !selection-io
              (ground/install-chrome-selection-hooks!
               {:clear clear-selection! :shift-tap shift-tap!}))
      (let [!marquee-original (atom nil)
            !drag-original (atom nil)
            marquee-previous
            (ground/register-verb! :selection/marquee-begin
                                   (marquee-wrapper !marquee-original))
            drag-previous
            (ground/register-verb! :placement/drag-group
                                   (block-drag-wrapper !drag-original))]
        (reset! !marquee-original marquee-previous)
        (reset! !drag-original drag-previous)
        (when-not (and (map? marquee-previous) (map? drag-previous))
          (throw (ex-info "Chrome verb re-registration did not capture prior impls"
                          {:marquee (boolean marquee-previous)
                           :drag (boolean drag-previous)}))))
      (reset! !booted? true)
      (frame-edge!)
      (publish-receipt! chrome-system))))

(defn receipt [] @!receipt)
