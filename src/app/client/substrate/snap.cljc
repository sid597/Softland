(ns app.client.substrate.snap
  "Pure world-space object snapping and smart-guide derivation.

   The applied delta, alignment set, guide geometry, and equal-gap ticks are
   minted by one gesture-step result so paint cannot advertise a snap that the
   arrangement did not actually take."
  (:require [app.client.substrate.chrome-material :as chrome-material]
            [app.client.workspace.containers :as containers]
            [app.client.workspace.selection :as selection]))

(def excluded-candidate-families
  #{:render.family/connector :render.family/chrome})

(defn empty-state []
  {:snap/version 1
   :snap-resolutions 0
   :last-token nil
   :last-result nil})

(defn- target-id [row]
  {:vi (:vi row) :address (:address row)})

(defn- target-world-row [row effective-transforms]
  (when-let [effective (get effective-transforms (:container row))]
    (assoc row
           :target (target-id row)
           :world-bounds (containers/transform-bounds effective (:bounds row)))))

(defn- axis-features [axis {:keys [x y w h]}]
  (if (= :x axis)
    [{:kind :edge :coordinate x}
     {:kind :center :coordinate (+ x (/ w 2.0))}
     {:kind :edge :coordinate (+ x w)}]
    [{:kind :edge :coordinate y}
     {:kind :center :coordinate (+ y (/ h 2.0))}
     {:kind :edge :coordinate (+ y h)}]))

(defn extract-candidates
  "Compose the full clip-blind address population to world and emit its
   horizontal/vertical edge and center candidates."
  [targets-by-address effective-transforms dragged-identity]
  (let [dragged (when dragged-identity
                  (selection/validate-identity! dragged-identity))]
    (into []
          (mapcat
           (fn [row]
             (when-let [world-row (and (not (contains? excluded-candidate-families
                                                       (:family row)))
                                       (not= dragged (target-id row))
                                       (target-world-row row effective-transforms))]
               (for [axis [:x :y]
                     feature (axis-features axis (:world-bounds world-row))]
                 (merge feature
                        {:axis axis
                         :target (:target world-row)
                         :target-family (:family world-row)
                         :target-bounds (:world-bounds world-row)})))))
          (selection/target-rows targets-by-address))))

(defn threshold-world [zoom]
  (when-not (and (number? zoom) (<= 0.01 zoom 1000.0))
    (throw (ex-info "Snap zoom is outside the legal envelope"
                    {:zoom zoom :legal [0.01 1000.0]})))
  (/ chrome-material/snap-threshold-screen-px (double zoom)))

(defn- kind-rank [kind]
  (if (= :center kind) 0 1))

(defn resolve-axis
  "Choose one alignment by distance, then center-over-edge, then coordinate."
  [axis moving-bounds candidates threshold]
  (let [moving-features (axis-features axis moving-bounds)
        matches
        (for [moving moving-features
              candidate candidates
              :when (= axis (:axis candidate))
              :let [delta (- (:coordinate candidate) (:coordinate moving))
                    distance (abs delta)
                    ;; Threshold equality is part of the screen-px contract.
                    ;; Coordinate subtraction may round one ulp above it after
                    ;; zoom division, so admit only a scale-relative numerical
                    ;; epsilon—not a larger semantic snap radius.
                    epsilon (* 1.0e-12
                               (max 1.0 distance threshold
                                    (abs (:coordinate candidate))
                                    (abs (:coordinate moving))))]
              :when (<= distance (+ threshold epsilon))]
          {:axis axis
           :kind (if (or (= :center (:kind moving))
                         (= :center (:kind candidate)))
                   :center :edge)
           :moving-kind (:kind moving)
           :candidate-kind (:kind candidate)
           :moving-coordinate (:coordinate moving)
           :candidate-coordinate (:coordinate candidate)
           :delta delta
           :distance distance
           :target (:target candidate)
           :target-bounds (:target-bounds candidate)})]
    (first
     (sort-by (fn [match]
                [(:distance match)
                 (kind-rank (:kind match))
                 (:candidate-coordinate match)
                 (pr-str (:target match))])
              matches))))

(defn- shift-bounds [bounds dx dy]
  (-> bounds (update :x + dx) (update :y + dy)))

(defn- guide-for [moving-bounds alignment]
  (let [target (:target-bounds alignment)
        coord (:candidate-coordinate alignment)]
    (if (= :x (:axis alignment))
      {:axis :x :kind (:kind alignment) :candidate-coordinate coord
       :from [coord (min (:y moving-bounds) (:y target))]
       :to [coord (max (+ (:y moving-bounds) (:h moving-bounds))
                       (+ (:y target) (:h target)))]
       :target (:target alignment)}
      {:axis :y :kind (:kind alignment) :candidate-coordinate coord
       :from [(min (:x moving-bounds) (:x target)) coord]
       :to [(max (+ (:x moving-bounds) (:w moving-bounds))
                 (+ (:x target) (:w target))) coord]
       :target (:target alignment)})))

(defn- unique-target-bounds [candidates]
  (vals
   (reduce (fn [m {:keys [target target-bounds]}]
             (assoc m target {:target target :bounds target-bounds}))
           {} candidates)))

(defn- gap-match-x [moving targets tolerance]
  (let [lefts (filter #(<= (+ (get-in % [:bounds :x])
                               (get-in % [:bounds :w]))
                            (:x moving)) targets)
        rights (filter #(>= (get-in % [:bounds :x])
                            (+ (:x moving) (:w moving))) targets)]
    (first
     (sort-by
      (fn [{:keys [difference left right]}]
        [difference (get-in left [:bounds :x]) (get-in right [:bounds :x])
         (pr-str (:target left)) (pr-str (:target right))])
      (for [left lefts right rights
            :let [left-edge (+ (get-in left [:bounds :x])
                               (get-in left [:bounds :w]))
                  right-edge (get-in right [:bounds :x])
                  left-gap (- (:x moving) left-edge)
                  right-gap (- right-edge (+ (:x moving) (:w moving)))
                  difference (abs (- left-gap right-gap))]
            :when (<= difference tolerance)]
        {:axis :x :left left :right right
         :left-gap left-gap :right-gap right-gap :difference difference})))))

(defn- gap-match-y [moving targets tolerance]
  (let [tops (filter #(<= (+ (get-in % [:bounds :y])
                              (get-in % [:bounds :h]))
                           (:y moving)) targets)
        bottoms (filter #(>= (get-in % [:bounds :y])
                             (+ (:y moving) (:h moving))) targets)]
    (first
     (sort-by
      (fn [{:keys [difference top bottom]}]
        [difference (get-in top [:bounds :y]) (get-in bottom [:bounds :y])
         (pr-str (:target top)) (pr-str (:target bottom))])
      (for [top tops bottom bottoms
            :let [top-edge (+ (get-in top [:bounds :y])
                              (get-in top [:bounds :h]))
                  bottom-edge (get-in bottom [:bounds :y])
                  top-gap (- (:y moving) top-edge)
                  bottom-gap (- bottom-edge (+ (:y moving) (:h moving)))
                  difference (abs (- top-gap bottom-gap))]
            :when (<= difference tolerance)]
        {:axis :y :top top :bottom bottom
         :top-gap top-gap :bottom-gap bottom-gap :difference difference})))))

(defn equal-gap-ticks
  "Return constant-screen tick anchors when the moving box completes an equal
   neighboring gap on either axis. The tick line itself is expanded in px by
   chrome-material; its world anchor is only the gap midpoint."
  [moving-bounds candidates tolerance]
  (let [targets (unique-target-bounds candidates)
        x-match (gap-match-x moving-bounds targets tolerance)
        y-match (gap-match-y moving-bounds targets tolerance)
        cy (+ (:y moving-bounds) (/ (:h moving-bounds) 2.0))
        cx (+ (:x moving-bounds) (/ (:w moving-bounds) 2.0))]
    (cond-> []
      x-match
      (into (let [left-edge (+ (get-in x-match [:left :bounds :x])
                               (get-in x-match [:left :bounds :w]))
                  right-edge (get-in x-match [:right :bounds :x])]
              [{:axis :x :at [(/ (+ left-edge (:x moving-bounds)) 2.0) cy]
                :gap (:left-gap x-match)}
               {:axis :x :at [(/ (+ (+ (:x moving-bounds) (:w moving-bounds))
                                     right-edge) 2.0) cy]
                :gap (:right-gap x-match)}]))
      y-match
      (into (let [top-edge (+ (get-in y-match [:top :bounds :y])
                              (get-in y-match [:top :bounds :h]))
                  bottom-edge (get-in y-match [:bottom :bounds :y])]
              [{:axis :y :at [cx (/ (+ top-edge (:y moving-bounds)) 2.0)]
                :gap (:top-gap y-match)}
               {:axis :y :at [cx (/ (+ (+ (:y moving-bounds) (:h moving-bounds))
                                     bottom-edge) 2.0)]
                :gap (:bottom-gap y-match)}])))))

(defn resolve-snap
  "Resolve one raw moving AABB. The returned position, guides, and alignments
   share the exact same deltas."
  [{:keys [raw-position raw-bounds candidates zoom]}]
  (let [threshold (threshold-world zoom)
        x-alignment (resolve-axis :x raw-bounds candidates threshold)
        y-alignment (resolve-axis :y raw-bounds candidates threshold)
        dx (double (or (:delta x-alignment) 0.0))
        dy (double (or (:delta y-alignment) 0.0))
        snapped-bounds (shift-bounds raw-bounds dx dy)
        [px py] (or raw-position [(:x raw-bounds) (:y raw-bounds)])
        alignments (into #{}
                         (map (fn [a] [(:axis a) (:kind a)
                                      (:candidate-coordinate a)]))
                         (remove nil? [x-alignment y-alignment]))
        guides (mapv #(guide-for snapped-bounds %)
                     (remove nil? [x-alignment y-alignment]))]
    {:raw-position [px py]
     :position [(+ px dx) (+ py dy)]
     :raw-bounds raw-bounds
     :bounds snapped-bounds
     :delta [dx dy]
     :alignments alignments
     :guides guides
     :ticks (equal-gap-ticks snapped-bounds candidates threshold)
     :threshold threshold}))

(defn gesture-step
  "State-in/state-out static-step cache. One changed input increments the
   resolution counter once; an identical candidate set and raw position does
   zero derivation work."
  [state input]
  (let [state (merge (empty-state) (or state {}))
        token (select-keys input [:raw-position :raw-bounds :candidates :zoom])]
    (if (= token (:last-token state))
      {:state state :result (:last-result state) :derived? false}
      (let [result (resolve-snap input)
            state (-> state
                      (update :snap-resolutions inc)
                      (assoc :last-token token :last-result result))]
        {:state state :result result :derived? true}))))
