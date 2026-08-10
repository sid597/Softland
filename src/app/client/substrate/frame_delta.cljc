(ns app.client.substrate.frame-delta
  "Small render-seam delta values.

   Deltas are minted by the owner that already knows the changed key.  This
   namespace classifies and shapes those values; it never scans a frame to
   discover that something changed."
  (:require [app.client.substrate.frame-effects :as frame-effects]))

(def semantic-delta-kinds #{:entry :container :region-topology :global})
(def binding-delta-kinds #{:region-lease :region-payload :viewport-size})

(defn semantic-delta? [delta]
  (contains? semantic-delta-kinds (:delta/kind delta)))

(defn binding-delta? [delta]
  (contains? binding-delta-kinds (:binding/kind delta)))

(defn entry-delta
  [op old-entry new-entry old-key new-key]
  (cond-> {:delta/kind :entry
           :op op
           :entry/id (or (:entry/id new-entry) (:entry/id old-entry))
           :old-entry old-entry
           :new-entry new-entry
           :old-key old-key
           :new-key new-key}
    (= op :insert) (dissoc :old-entry :old-key)
    (= op :remove) (dissoc :new-entry :new-key)))

(defn entry-deltas
  "Return the exact entry deltas applied by family-scoped arrangement
   maintenance.  `remove-keys` and `insert-entries` are already keyed at the
   family producer; this function only pairs a move/update and orders the
   resulting values by their old/new arrangement roads."
  [old-ordered new-ordered remove-keys insert-entries entry-key]
  (let [removed (into {}
                      (keep (fn [key]
                              (when-let [entry (get old-ordered key)]
                                [key entry])))
                      remove-keys)
        removed-by-id (into {} (map (fn [[key entry]]
                                      [(:entry/id entry) [key entry]]))
                            removed)
        insert-by-key (into {} (map (juxt entry-key identity)) insert-entries)
        consumed (volatile! #{})
        forward
        (into []
              (keep
               (fn [[key new-entry]]
                 (when (contains? insert-by-key key)
                   (let [old-at-key (get old-ordered key)
                         [old-key old-by-id]
                         (get removed-by-id (:entry/id new-entry))]
                     (cond
                       (= old-at-key new-entry) nil

                       old-at-key
                       (entry-delta :update old-at-key new-entry key key)

                       old-by-id
                       (do (vswap! consumed conj old-key)
                           (entry-delta :move old-by-id new-entry
                                        old-key key))

                       :else
                       (entry-delta :insert nil new-entry nil key))))))
              new-ordered)
        backward
        (into []
              (keep (fn [[key old-entry]]
                      (when (and (contains? removed key)
                                 (not (contains? @consumed key)))
                        (entry-delta :remove old-entry nil key nil))))
              old-ordered)]
    (into forward backward)))

(defn- raw-container-chain [registry cid]
  (loop [id cid chain () seen #{}]
    (cond
      (nil? id) (vec chain)
      (contains? seen id)
      (throw (ex-info "container parent cycle while minting frame delta"
                      {:container/id cid :cycle-at id}))
      :else
      (let [row (get-in registry [:containers id])]
        (when-not row
          (throw (ex-info "container missing while minting frame delta"
                          {:container/id cid :missing id})))
        (recur (:parent row) (conj chain [id row]) (conj seen id))))))

(defn- effect-kind-set [effects]
  (into #{}
        (keep (fn [[kind value]]
                (when (and (some? value)
                           (not (and (boolean? value) (false? value))))
                  kind)))
        effects))

(defn topology-signature
  "The exact plan-shape part of one effect declaration.  Numeric/color
   parameters stay outside this signature."
  [{:keys [parent/container-id stack-path depth effects]}]
  {:parent/container-id container-id
   :stack-path stack-path
   :depth depth
   :effect-kinds (effect-kind-set effects)
   :mask? (contains? (or effects {}) :mask)})

(defn container-declaration
  "Read one container by its held id.  Only its ancestor chain is visited;
   registry population is never a write-side discovery mechanism."
  [registry cid]
  (when-let [row (get-in registry [:containers cid])]
    (let [chain (raw-container-chain registry cid)
          stack-path
          (mapv (fn [[id ancestor]]
                  [id (:layer ancestor 0) (:sibling-rank ancestor 0)])
                chain)
          effects (frame-effects/validate-effects! (:effects row))
          effectful-ancestors
          (filter (fn [[_ ancestor]]
                    (frame-effects/effectful?
                     (frame-effects/validate-effects! (:effects ancestor))))
                  (butlast chain))
          parent-id (some-> effectful-ancestors last first)]
      {:container/id cid
       :parent/container-id parent-id
       :stack-path stack-path
       :depth (count stack-path)
       :effects effects
       :topology-signature
       (topology-signature {:parent/container-id parent-id
                            :stack-path stack-path
                            :depth (count stack-path)
                            :effects effects})})))

(defn container-declarations
  "Batch/bootstrap oracle input.  Live writes call `container-declaration`
   with their already-held id instead."
  [registry]
  (into {}
        (keep (fn [cid]
                (when-let [decl (container-declaration registry cid)]
                  (when (frame-effects/effectful? (:effects decl))
                    [cid decl]))))
        (keys (:containers registry))))

(defn container-delta
  "Classify one registry write.  Effectless-to-effectless writes have no
   render-effect consumer and therefore mint no container delta."
  [cid old-decl new-decl]
  (let [old-effectful? (and old-decl
                            (frame-effects/effectful? (:effects old-decl)))
        new-effectful? (and new-decl
                            (frame-effects/effectful? (:effects new-decl)))]
    (when (and (or old-effectful? new-effectful?)
               (not= old-decl new-decl))
      {:delta/kind :container
       :container/id cid
       :class (if (= (:topology-signature old-decl)
                     (:topology-signature new-decl))
                :parameter :topology)
       :old-decl (when old-effectful? old-decl)
       :new-decl (when new-effectful? new-decl)})))

(defn region-topology-delta [region-id op old-row new-row]
  {:delta/kind :region-topology
   :region/id region-id :op op
   :old (some-> old-row (select-keys [:region/id :shadow?]))
   :new (some-> new-row (select-keys [:region/id :shadow?]))})

(defn region-lease-delta [region-id old-key new-key]
  {:binding/kind :region-lease
   :region/id region-id
   :old-lease-key old-key
   :new-lease-key new-key})

(defn region-payload-delta [region-id old-row new-row changed-fields]
  {:binding/kind :region-payload
   :region/id region-id
   :changed-fields (set changed-fields)
   :old (select-keys old-row changed-fields)
   :new (select-keys new-row changed-fields)})

(defn viewport-size-delta [old-size new-size]
  (when (not= old-size new-size)
    {:binding/kind :viewport-size :old old-size :new new-size}))

(defn global-deltas [old-values new-values]
  (into []
        (keep (fn [field]
                (let [old (get old-values field)
                      new (get new-values field)]
                  (when (not= old new)
                    {:delta/kind :global :field field
                     :old old :new new}))))
        [:viewport-format :capabilities :forced-color-mode]))
