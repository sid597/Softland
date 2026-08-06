(ns app.client.workspace.selection
  "Pure session selection truth for the flagged chrome lane.

   Selection identities are the same occurrence identities returned by scene
   picking: {:vi view-instance :address semantic-address}.  The model owns no
   atoms and has no persistence surface; callers carry state from one
   transition to the next and project only block addresses to the legacy group
   selection lane."
  (:require [clojure.set :as set]
            [app.client.workspace.containers :as containers]))

(def excluded-sweep-families
  #{:render.family/connector :render.family/chrome})

(defn selection-identity? [identity]
  (and (map? identity)
       (contains? identity :vi)
       (some? (:vi identity))
       (contains? identity :address)
       (some? (:address identity))))

(defn validate-identity! [identity]
  (when-not (selection-identity? identity)
    (throw (ex-info "Selection identity requires non-nil :vi and :address"
                    {:identity identity})))
  (select-keys identity [:vi :address]))

(defn empty-selection []
  {:selection/version 1
   :members #{}
   :member-revisions {}
   :revision 0
   :last-pruned 0})

(defn- replace-members [state members pruned]
  (let [state (merge (empty-selection) (or state {}))
        members (into #{} (map validate-identity!) members)]
    (if (= members (:members state))
      (assoc state :last-pruned (long (or pruned 0)))
      (let [revision (inc (:revision state))
            prior-stamps (:member-revisions state)
            stamps (into {}
                         (map (fn [identity]
                                [identity (get prior-stamps identity revision)]))
                         members)]
        (assoc state :members members :member-revisions stamps
               :revision revision :last-pruned (long (or pruned 0)))))))

(defn clear [state]
  (replace-members state #{} 0))

(defn toggle [state identity]
  (let [identity (validate-identity! identity)
        members (:members (merge (empty-selection) (or state {})))]
    (replace-members state
                     (if (contains? members identity)
                       (disj members identity)
                       (conj members identity))
                     0)))

(defn marquee-commit
  "A fresh marquee replaces the member set; it is never additive in v1."
  [state identities]
  (replace-members state identities 0))

(defn frame-prune
  "Drop identities no longer projected by the current store frame."
  [state live-identities]
  (let [state (merge (empty-selection) (or state {}))
        live (into #{} (map validate-identity!) live-identities)
        kept (set/intersection (:members state) live)
        pruned (- (count (:members state)) (count kept))]
    (replace-members state kept pruned)))

(defn marquee-rect
  "Normalize press and pointer world positions to a non-negative world AABB."
  [[x0 y0] [x1 y1]]
  (when-not (every? number? [x0 y0 x1 y1])
    (throw (ex-info "Marquee endpoints must be numeric world points"
                    {:press [x0 y0] :pointer [x1 y1]})))
  {:x (min x0 x1)
   :y (min y0 y1)
   :w (abs (- x1 x0))
   :h (abs (- y1 y0))})

(defn bounds-intersect?
  "Closed AABB intersection. Touching a marquee boundary is a hit."
  [a b]
  (and (<= (:x a) (+ (:x b) (:w b)))
       (<= (:x b) (+ (:x a) (:w a)))
       (<= (:y a) (+ (:y b) (:h b)))
       (<= (:y b) (+ (:y a) (:h a)))))

(defn target-rows [targets-by-address]
  (for [[address occurrences] targets-by-address
        occurrence occurrences]
    (assoc occurrence :address address)))

(defn row-identity [row]
  (validate-identity! {:vi (:vi row) :address (:address row)}))

(defn row-world-bounds
  "Compose an index row's absolute container-local bounds to world."
  [row effective-transforms]
  (when-let [effective (get effective-transforms (:container row))]
    (containers/transform-bounds effective (:bounds row))))

(defn live-identities [targets-by-address]
  (into #{} (map row-identity) (target-rows targets-by-address)))

(defn marquee-hits
  "Clip-blind v1 sweep over the store frame's address index.

   Connector occurrences and chrome/gesture slots are explicitly excluded.
   Rows whose container left the effective map are stale and fail closed."
  [world-rect targets-by-address effective-transforms]
  (into #{}
        (keep (fn [row]
                (when (and (not (contains? excluded-sweep-families
                                           (:family row)))
                           (some-> (row-world-bounds row effective-transforms)
                                   (bounds-intersect? world-rect)))
                  (row-identity row))))
        (target-rows targets-by-address)))

(defn legacy-projection
  "Project the model's block-unit subset for Task-18's legacy readers."
  [state block-unit-ids]
  (let [blocks (set block-unit-ids)]
    (into #{}
          (keep (fn [{:keys [address]}]
                  (when (contains? blocks address) address)))
          (:members (merge (empty-selection) (or state {}))))))

(defn selection-census
  "Describe selected occurrence kinds without changing selection truth."
  [state targets-by-address]
  (let [rows-by-identity
        (into {} (map (juxt row-identity identity)) (target-rows targets-by-address))
        members (:members (merge (empty-selection) (or state {})))
        families (map #(get-in rows-by-identity [% :family]) members)]
    {:selected (count members)
     :pruned (:last-pruned state 0)
     :blocks (count (filter #{:render.family/rect} families))
     :fixtures (count (remove #{:render.family/rect
                               :render.family/connector
                               :render.family/chrome nil}
                             families))
     :edges (count (filter #{:render.family/connector} families))}))

(defn transition
  "Closed transition vocabulary used by the incremental chrome derivation."
  [state {:keys [op identity identities live-identities]}]
  (case op
    :toggle (toggle state identity)
    :marquee-commit (marquee-commit state identities)
    :clear (clear state)
    :frame-prune (frame-prune state live-identities)
    (throw (ex-info "Unknown selection transition" {:op op}))))
