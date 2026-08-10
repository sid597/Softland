(ns app.client.substrate.frame-effect-view
  "Incrementally maintained effect membership and execution-event view.

   Durable state is keyed by semantic entry/container ids.  Numeric entry
   ranges are projected only for the compositor's forward walk."
  (:require [app.client.substrate.frame-delta :as frame-delta]
            [app.client.substrate.frame-effects :as frame-effects]
            [app.client.substrate.scene-tape :as scene-tape]))

(defn empty-state []
  {:containers {}
   :by-stack-path {}
   :generation 0})

(defn- entry-key [entry] (scene-tape/entry-key entry))

(defn- ordered-values [arrangement]
  (vals (:ordered arrangement)))

(defn- member? [decl entry]
  (frame-effects/stack-prefix?
   (:stack-path decl)
   (get-in entry [:order :stack-path])))

(defn- reuse-run [old-runs start-key end-key]
  (or (some (fn [run]
              (when (and (= start-key (:start-key run))
                         (= end-key (:end-key run)))
                run))
            old-runs)
      {:start-key start-key :end-key end-key}))

(defn- runs-from-boundaries [boundaries old-runs]
  (let [keys-in-order (sort scene-tape/entry-key-compare (keys boundaries))]
    (loop [remaining keys-in-order start nil result []]
      (if-let [key (first remaining)]
        (let [{:keys [open? close?]} (get boundaries key)
              start (if open? key start)
              result (if close?
                       (conj result (reuse-run old-runs (or start key) key))
                       result)]
          (recur (next remaining) (if close? nil start) result))
        result))))

(defn- build-container [decl arrangement old-container]
  (let [entries (vec (ordered-values arrangement))
        members (into #{}
                      (keep (fn [entry]
                              (when (member? decl entry) (entry-key entry))))
                      entries)
        boundaries
        (into {}
              (keep-indexed
               (fn [index entry]
                 (let [key (entry-key entry)
                       here? (contains? members key)
                       before? (and (pos? index)
                                    (contains? members
                                               (entry-key (nth entries (dec index)))))
                       after? (and (< (inc index) (count entries))
                                   (contains? members
                                              (entry-key (nth entries (inc index)))))]
                   (when (and here? (or (not before?) (not after?)))
                     [key {:open? (not before?) :close? (not after?)}]))))
              entries)]
    {:declaration decl
     :members members
     :boundaries boundaries
     :runs (runs-from-boundaries boundaries (:runs old-container))
     :parameter-revision (or (:parameter-revision old-container) 0)}))

(defn bootstrap [registry arrangement]
  (let [declarations (if registry
                       (frame-delta/container-declarations registry)
                       {})]
    {:containers
     (into {}
           (map (fn [[cid decl]]
                  [cid (build-container decl arrangement nil)]))
           declarations)
     :by-stack-path
     (into {} (map (fn [[cid decl]] [(:stack-path decl) cid])) declarations)
     :generation 1}))

(defn topology-rows [state]
  (->> (:containers state)
       vals
       (map :declaration)
       (sort-by (juxt (comp - :depth) (comp pr-str :container/id)))
       vec))

(defn- chain-ids [state entry]
  (let [path (get-in entry [:order :stack-path])]
    (into []
          (keep (fn [length]
                  (get (:by-stack-path state) (subvec path 0 length))))
          (range 1 (inc (count path))))))

(defn- predecessor [arrangement key]
  (some-> (rsubseq (:ordered arrangement) < key) first val))

(defn- successor [arrangement key]
  (some-> (subseq (:ordered arrangement) > key) first val))

(defn- boundary-entries [arrangement key]
  (when key
    (keep identity [(get (:ordered arrangement) key)
                    (predecessor arrangement key)
                    (successor arrangement key)])))

(defn- touched-entry-context
  [old-state new-state old-arrangement new-arrangement delta]
  (let [old-context (boundary-entries old-arrangement (:old-key delta))
        new-context (boundary-entries new-arrangement (:new-key delta))]
    {:containers
     (into #{}
           (concat (mapcat #(chain-ids old-state %) old-context)
                   (mapcat #(chain-ids new-state %) new-context)))
     :keys
     (into #{}
           (keep entry-key)
           (concat old-context new-context))}))

(defn- assoc-container-declaration [state cid decl]
  (let [old (get-in state [:containers cid])
        state (if-let [old-path (get-in old [:declaration :stack-path])]
                (update state :by-stack-path dissoc old-path)
                state)]
    (if decl
      (-> state
          (assoc-in [:containers cid :declaration] decl)
          (assoc-in [:by-stack-path (:stack-path decl)] cid))
      (update state :containers dissoc cid))))

(defn- apply-container-delta [state arrangement delta]
  (let [cid (:container/id delta)
        old-container (get-in state [:containers cid])]
    (case (:class delta)
      :parameter
      (if-let [decl (:new-decl delta)]
        (-> state
            (assoc-container-declaration cid decl)
            (update-in [:containers cid :parameter-revision] (fnil inc 0)))
        state)

      :topology
      (let [without-old (assoc-container-declaration state cid nil)]
        (if-let [decl (:new-decl delta)]
          (-> without-old
              (assoc-in [:containers cid]
                        (build-container decl arrangement old-container))
              (assoc-in [:by-stack-path (:stack-path decl)] cid))
          without-old))

      state)))

(defn- membership-after-delta [members cid old-state new-state delta]
  (let [old-member? (and (:old-entry delta)
                         (some #{cid} (chain-ids old-state (:old-entry delta))))
        new-member? (and (:new-entry delta)
                         (some #{cid} (chain-ids new-state (:new-entry delta))))]
    (cond-> members
      (and old-member? (:old-key delta)) (disj (:old-key delta))
      (and new-member? (:new-key delta)) (conj (:new-key delta)))))

(defn- set-local-boundary [container arrangement key]
  (let [members (:members container)
        entry (get (:ordered arrangement) key)
        here? (and entry (contains? members key))
        before-key (some-> (predecessor arrangement key) entry-key)
        after-key (some-> (successor arrangement key) entry-key)
        boundary (when here?
                   (let [open? (not (contains? members before-key))
                         close? (not (contains? members after-key))]
                     (when (or open? close?)
                       {:open? open? :close? close?})))
        boundaries (if boundary
                     (assoc (:boundaries container) key boundary)
                     (dissoc (:boundaries container) key))]
    (if (identical? boundaries (:boundaries container))
      container
      (assoc container
             :boundaries boundaries
             :runs (runs-from-boundaries boundaries (:runs container))))))

(defn apply-deltas
  "Apply explicit entry/container deltas.  Returns state plus work counters;
   an entry that touches no effect chain preserves the state identity."
  [state {:keys [old-arrangement new-arrangement entry-deltas
                 container-deltas registry]}]
  (if-not state
    {:state (bootstrap registry new-arrangement)
     :work {:effect-containers-touched 0
            :container-declarations-inspected
            (count (frame-delta/container-declarations (or registry {:containers {}})))
            :bootstrap? true}}
    (let [container-deltas (vec (or container-deltas []))
          topology-cids (into #{}
                              (keep #(when (= :topology (:class %))
                                       (:container/id %)))
                              container-deltas)
          after-containers
          (reduce #(apply-container-delta %1 new-arrangement %2)
                  state container-deltas)
          contexts (mapv #(touched-entry-context
                           state after-containers old-arrangement new-arrangement %)
                         (or entry-deltas []))
          touched (-> (reduce into #{} (map :containers contexts))
                      (into (map :container/id container-deltas))
                      (into topology-cids))
          local-touched (remove topology-cids touched)
          candidate-keys (reduce into #{} (map :keys contexts))
          after-members
          (reduce
           (fn [next-state cid]
             (if-let [container (get-in next-state [:containers cid])]
               (let [members
                     (reduce #(membership-after-delta
                               %1 cid state after-containers %2)
                             (:members container) (or entry-deltas []))]
                 (assoc-in next-state [:containers cid :members] members))
               next-state))
           after-containers local-touched)
          next-state
          (reduce
           (fn [next-state cid]
             (if-let [container (get-in next-state [:containers cid])]
               (assoc-in next-state [:containers cid]
                         (reduce #(set-local-boundary %1 new-arrangement %2)
                                 container candidate-keys))
               next-state))
           after-members local-touched)
          changed? (or (seq container-deltas)
                       (and (seq entry-deltas) (seq touched)))
          next-state (if changed?
                       (update next-state :generation inc)
                       state)]
      {:state next-state
       :work {:effect-containers-touched (count touched)
              :container-declarations-inspected (count container-deltas)
              :touched-container-ids touched
              :bootstrap? false}})))

(defn project-spans
  "Project durable boundary keys to the batch span shape.  The arrangement is
   walked once to assign ephemeral numeric positions; positions never enter
   maintained truth."
  [state arrangement]
  (let [index-by-key
        (into {} (map-indexed (fn [index entry]
                                [(entry-key entry) index]))
              (ordered-values arrangement))]
    (->> (:containers state)
         (keep (fn [[_ {:keys [declaration runs]}]]
                 (let [ranges
                       (into []
                             (keep (fn [{:keys [start-key end-key]}]
                                     (when-let [start (get index-by-key start-key)]
                                       (when-let [end (get index-by-key end-key)]
                                         [start (inc end)]))))
                             runs)
                       count (reduce + 0 (map (fn [[start end]] (- end start))
                                              ranges))]
                   (when (pos? count)
                     (assoc (dissoc declaration :topology-signature)
                            :entry-ranges ranges
                            :entry-count count
                            :derived-effects
                            (frame-effects/derived-effect-declarations
                             (:effects declaration)))))))
         (sort-by (juxt (comp - :depth) (comp pr-str :container/id)))
         vec)))

(defn oracle-equal? [state registry arrangement]
  (= (project-spans state arrangement)
     (frame-effects/derive-effect-spans registry
                                        (vec (ordered-values arrangement)))))
