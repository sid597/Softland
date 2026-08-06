(ns app.client.substrate.chrome-derive
  "Pure maintained chrome slots, independent full-recompute oracle, and live
   handle-pick providers. Runtime owns atoms and store mutation; this namespace
   owns the data transition laws and proportionality receipts."
  (:require [app.client.substrate.chrome-material :as chrome-material]
            [app.client.workspace.selection :as selection]))

(def seam-declarations
  {:keyed-inputs [:selection/revision :member-identities+bounds
                  :gesture-state :snap-alignment-set :zoom-for-pick-only]
   :doors [:selection-event :gesture-event :effective-transform-value-diff]
   :ownership :flagged-chrome-session-state
   :projections [:paint-forward :pick-reverse :census]
   :oracle :full-recompute-selection+store-frame})

(def algorithm-version chrome-material/algorithm-version)
(def fixture-families
  #{:render.family/image :render.family/path})
(def no-visual-families
  #{:render.family/connector :render.family/chrome})
(def broad-phase-radius 1000000.0)

(defonce ^:private !live-camera-provider (atom (constantly {:x 0.0 :y 0.0 :zoom 1.0})))
(defonce ^:private !live-effective-provider (atom (constantly {})))

(defn set-live-camera-provider! [provider]
  (when-not (ifn? provider)
    (throw (ex-info "Chrome camera provider must be callable" {:provider provider})))
  (reset! !live-camera-provider provider)
  true)

(defn set-live-effective-provider! [provider]
  (when-not (ifn? provider)
    (throw (ex-info "Chrome effective-transform provider must be callable"
                    {:provider provider})))
  (reset! !live-effective-provider provider)
  true)

(defn- sqrt [x]
  #?(:clj (Math/sqrt (double x)) :cljs (js/Math.sqrt x)))

(defn effective-scale [{[a b] :affine}]
  (let [scale (sqrt (+ (* a a) (* b b)))]
    (if (pos? scale) scale 1.0)))

(defn handle-slop-local-radius [zoom effective]
  (/ chrome-material/handle-slop-screen-px
     (* (double zoom) (effective-scale effective))))

(defn handle-hit?
  "Narrow pick for chrome handles. `local-point` is relative to the deliberately
   conservative handle broad-phase node; its true handle center is recorded as
   node data. The px body and slop are divided by camera zoom and the followed
   container's effective scale."
  [node local-point]
  (let [material (get-in node [:data :chrome/material])]
    (if (not= :handle (:chrome/form material))
      false
      (let [camera (@!live-camera-provider)
            effective-map (@!live-effective-provider)
            effective (get effective-map (:chrome/container material))]
        (if-not effective
          false
          (let [zoom (double (or (:zoom camera) (:scale camera) 1.0))
                scale (effective-scale effective)
                half-body (/ chrome-material/handle-size-screen-px
                             (* 2.0 zoom scale))
                slop (handle-slop-local-radius zoom effective)
                [cx cy] (get-in node [:data :chrome/hit-center-local])
                [px py] local-point
                radius (+ half-body slop)
                epsilon (* 1.0e-13
                           (max 1.0 radius (abs px) (abs py)
                                (abs cx) (abs cy)))]
            (and (<= (abs (- px cx)) (+ radius epsilon))
                 (<= (abs (- py cy)) (+ radius epsilon)))))))))

(defn empty-state []
  {:chrome/version 1
   :selection (selection/empty-selection)
   :slots {}
   :bound-chrome-by-container {}
   :last-effective nil
   :chrome-derives 0
   :chrome-transform-updates 0
   :frame-receipt {:chrome-derives 0 :chrome-transform-updates 0}})

(defn- rows-by-identity [store-frame]
  (into {}
        (map (fn [row] [(selection/row-identity row) row]))
        (selection/target-rows (:targets-by-address store-frame))))

(defn- chrome-id [form identity corner]
  [form (:vi identity) (:address identity) corner])

(defn- material [form identity row revision & {:keys [corner gesture-id from to alignment]}]
  (cond-> {:chrome/form form
           :chrome/anchor-bounds (:bounds row)
           :chrome/derived-from identity
           :chrome/selection-rev revision
           :chrome/pick (if (= :handle form) :interior :none)
           :chrome/container (:container row)}
    corner (assoc :chrome/corner corner)
    gesture-id (assoc :chrome/gesture-id gesture-id)
    from (assoc :chrome/from from)
    to (assoc :chrome/to to)
    alignment (assoc :chrome/alignment alignment)))

(defn- root-relative-bounds [{:keys [x y w h]}]
  {:x (+ broad-phase-radius x)
   :y (+ broad-phase-radius y)
   :w w :h h})

(defn- form-node [id bounds material & [hit-center-local]]
  {:id id
   :type :chrome
   :bounds bounds
   :style {}
   :actions {}
   :children []
   :text []
   :clip? false
   :data (cond-> {:address id
                  :render/family :render.family/chrome
                  :chrome/material (chrome-material/validate-material! material)}
           hit-center-local (assoc :chrome/hit-center-local hit-center-local))
   :layout nil})

(defn- corner-point [{:keys [x y w h]} corner]
  (case corner
    :nw [x y]
    :ne [(+ x w) y]
    :sw [x (+ y h)]
    :se [(+ x w) (+ y h)]))

(defn- target-nodes [identity row revision]
  (let [bounds (:bounds row)
        outline-id (chrome-id :selection-outline identity nil)
        outline (form-node outline-id (root-relative-bounds bounds)
                           (material :selection-outline identity row revision))]
    (cond-> [outline]
      (contains? fixture-families (:family row))
      (into
       (for [corner [:nw :ne :sw :se]
             :let [[x y] (corner-point bounds corner)
                   id (chrome-id :handle identity corner)
                   ;; Root origin is [-R,-R]. A child at [x,y] therefore has
                   ;; absolute broad-phase bounds [x-R,y-R,2R,2R], with its
                   ;; true handle anchor exactly at local [R,R].
                   node-bounds {:x x
                                :y y
                                :w (* 2.0 broad-phase-radius)
                                :h (* 2.0 broad-phase-radius)}
                   anchor-row (assoc row :bounds {:x x :y y :w 0.0 :h 0.0})]]
         (form-node id node-bounds
                    (material :handle identity anchor-row revision :corner corner)
                    [broad-phase-radius broad-phase-radius]))))))

(defn slot-for-target [identity row revision]
  (when-not (contains? no-visual-families (:family row))
    (let [slot-id [:chrome/target (:vi identity) (:address identity)]
          nodes (target-nodes identity row revision)]
      {:slot/id slot-id
       :target identity
       :target-container (:container row)
       :stratum :overlay
       :nodes nodes
       :tree {:id [:chrome/root slot-id]
              :type :group
              :bounds {:x (- broad-phase-radius) :y (- broad-phase-radius)
                       :w (* 2.0 broad-phase-radius)
                       :h (* 2.0 broad-phase-radius)}
              :style {} :actions {} :text [] :clip? false :layout nil
              :data {:render/family :render.family/chrome}
              :children nodes}})))

(defn- gesture-material [form id bounds & {:keys [from to alignment]}]
  (let [row {:bounds bounds :container 0}]
    (material form id row 0 :gesture-id id :from from :to to
              :alignment alignment)))

(defn gesture-slot
  "Derive the one world-anchored gesture slot for marquee/guides/ticks."
  [gesture snap-result]
  (let [gesture-id (or (:id gesture) :chrome/gesture)
        marquee (:marquee gesture)
        marquee-node
        (when marquee
          (let [id [gesture-id :marquee]
                mat (gesture-material :marquee gesture-id marquee)]
            (form-node id (root-relative-bounds marquee) mat)))
        guide-nodes
        (map-indexed
         (fn [index {:keys [from to] :as guide}]
           (let [bounds {:x (min (first from) (first to))
                         :y (min (second from) (second to))
                         :w (abs (- (first to) (first from)))
                         :h (abs (- (second to) (second from)))}
                 id [gesture-id :guide index]
                 mat (gesture-material :guide-line gesture-id bounds
                                       :from from :to to :alignment guide)]
             (form-node id (root-relative-bounds bounds) mat)))
         (:guides snap-result))
        tick-nodes
        (map-indexed
         (fn [index {:keys [at axis] :as tick}]
           (let [bounds {:x (first at) :y (second at) :w 0.0 :h 0.0}
                 id [gesture-id :tick index]
                 mat (gesture-material :gap-tick gesture-id bounds
                                       :from at :to at :alignment tick)]
             (form-node id (root-relative-bounds bounds) mat)))
         (:ticks snap-result))
        nodes (into (cond-> [] marquee-node (conj marquee-node))
                    (concat guide-nodes tick-nodes))]
    (when (seq nodes)
      {:slot/id [:chrome/gesture gesture-id]
       :gesture-id gesture-id
       :target-container 0
       :stratum :overlay
       :nodes nodes
       :alignment-set (:alignments snap-result)
       :tree {:id [:chrome/gesture-root gesture-id]
              :type :group
              :bounds {:x (- broad-phase-radius) :y (- broad-phase-radius)
                       :w (* 2.0 broad-phase-radius)
                       :h (* 2.0 broad-phase-radius)}
              :style {} :actions {} :text [] :clip? false :layout nil
              :data {:render/family :render.family/chrome}
              :children nodes}})))

(defn full-recompute-oracle
  "Independent batch oracle: selection × store-frame × gesture × snap."
  [{:keys [selection store-frame gesture snap-result]}]
  (let [rows (rows-by-identity store-frame)
        target-slots
        (into {}
              (keep (fn [identity]
                      (when-let [row (get rows identity)]
                        (when-let [slot
                                   (slot-for-target
                                    identity row
                                    (get-in selection [:member-revisions identity]
                                            (:revision selection)))]
                          [(:slot/id slot) slot]))))
              (:members selection))
        gesture-slot* (gesture-slot gesture snap-result)]
    (cond-> target-slots
      gesture-slot* (assoc (:slot/id gesture-slot*) gesture-slot*))))

(defn- bound-index [slots]
  (reduce-kv
   (fn [index slot-id slot]
     (let [container (:target-container slot)]
       (if (or (nil? container) (= 0 container))
         index
         (update index container (fnil conj #{}) slot-id))))
   {} slots))

(defn apply-selection
  "Incremental transition application. It maintains current slots from the
   transition result; it does not call the full-recompute oracle."
  [state next-selection store-frame gesture snap-result]
  (let [state (merge (empty-state) (or state {}))
        rows (rows-by-identity store-frame)
        desired-targets
        (into {}
              (keep (fn [identity]
                      (when-let [row (get rows identity)]
                        (when-let [slot
                                   (slot-for-target
                                    identity row
                                    (get-in next-selection
                                            [:member-revisions identity]
                                            (:revision next-selection)))]
                          [(:slot/id slot) slot]))))
              (:members next-selection))
        gesture-slot* (gesture-slot gesture snap-result)
        desired (cond-> desired-targets
                  gesture-slot* (assoc (:slot/id gesture-slot*) gesture-slot*))
        prior (:slots state)
        changed (into #{}
                      (for [slot-id (into (set (keys prior)) (keys desired))
                            :when (not= (get prior slot-id) (get desired slot-id))]
                        slot-id))]
    (-> state
        (assoc :selection next-selection
               :slots desired
               :bound-chrome-by-container (bound-index desired)
               :frame-receipt {:chrome-derives (count changed)
                               :chrome-transform-updates 0})
        (update :chrome-derives + (count changed)))))

(defn apply-transition
  [state transition store-frame gesture snap-result]
  (let [next-selection
        (selection/transition (:selection (merge (empty-state) (or state {})))
                              transition)]
    (apply-selection state next-selection store-frame gesture snap-result)))

(defn effective-value-diff [prior current]
  (if (nil? prior)
    #{}
    (into #{}
          (for [container (into (set (keys prior)) (keys current))
                :when (let [before (get prior container)
                            after (get current container)]
                        (not (or (identical? before after) (= before after))))]
            container))))

(defn follow-transforms
  "Value-diff the effective map and return exactly the bound chrome slots whose
   target container changed. The first frame establishes a baseline at zero."
  [state effective]
  (let [state (merge (empty-state) (or state {}))
        changed-containers (effective-value-diff (:last-effective state) effective)
        writes (into #{}
                     (mapcat #(get (:bound-chrome-by-container state) % #{}))
                     changed-containers)
        count* (count writes)]
    (-> state
        (assoc :last-effective effective
               :transform-writes writes
               :frame-receipt (assoc (:frame-receipt state)
                                     :chrome-transform-updates count*))
        (update :chrome-transform-updates + count*))))
