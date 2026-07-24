(ns app.shared.material-inspector
  "Deterministic, both-sides helpers for the read-only P2 material inspector.
   The client derives current wearers from rendered contribution stamps; the
   server canonicalizes that snapshot before joining it to durable OC truth."
  (:require [app.shared.provenance-material :as provenance-material]))

(def contribution-sites
  [:fold-header :machine-rail :episode-boundary])

(def ^:private site-rank
  (zipmap contribution-sites (range)))

(defn- site-sort-key
  [site]
  [(get site-rank site (count contribution-sites)) (pr-str site)])

(defn- contribution-stamp?
  [x]
  (and (map? x)
       (= provenance-material/master-id (:material/master x))
       (string? (:material/revision x))
       (contains? site-rank (:material/site x))))

(defn contribution-stamps
  "Find every provenance contribution stamp nested in an arbitrary rendered
   tree value. Result order is canonical and duplicates are removed."
  [x]
  (letfn [(walk [v acc]
            (cond
              (map? v)
              (reduce (fn [a child] (walk child a))
                      (cond-> acc
                        (contribution-stamp? v)
                        (conj (select-keys v
                                           [:material/master
                                            :material/revision
                                            :material/site])))
                      (vals v))

              (coll? v)
              (reduce (fn [a child] (walk child a)) acc v)

              :else acc))]
    (->> (walk x #{})
         (sort-by (juxt :material/revision
                        (comp site-sort-key :material/site)))
         vec)))

(defn normalize-wearers
  "Normalize a projected wearer snapshot into deterministic entity order. A
   wearer is derived current-scene evidence, never a durable attachment row."
  [wearers]
  (->> wearers
       (reduce
        (fn [by-entity wearer]
          (let [entity-id (:wearer/entity-id wearer)]
            (if-not (string? entity-id)
              by-entity
              (-> by-entity
                  (update-in [entity-id :revision-ids]
                             (fnil into #{})
                             (filter string? (:wearer/revision-ids wearer)))
                  (update-in [entity-id :sites]
                             (fnil into #{})
                             (filter site-rank
                                     (:wearer/contribution-sites wearer)))
                  (update-in [entity-id :view-instances]
                             (fnil into #{})
                             (:wearer/view-instances wearer))))))
        {})
       (map (fn [[entity-id {:keys [revision-ids sites view-instances]}]]
              {:wearer/entity-id entity-id
               :wearer/revision-ids (vec (sort revision-ids))
               :wearer/contribution-sites
               (vec (sort-by site-sort-key sites))
               :wearer/view-instances
               (vec (sort-by pr-str view-instances))}))
       (sort-by :wearer/entity-id)
       vec))

(defn wearers-from-scene-store
  "Project current provenance wearers from the scene store's ground-block
   slots. This is an ephemeral render snapshot: entity identity comes from the
   slot metadata, and contribution evidence comes only from P1's stamps."
  [store]
  (->> (vals (:slots store))
       (keep (fn [{:keys [vi tree meta]}]
               (when-let [entity-id (:ground-block meta)]
                 (let [stamps (contribution-stamps tree)]
                   (when (seq stamps)
                     {:wearer/entity-id entity-id
                      :wearer/revision-ids
                      (mapv :material/revision stamps)
                      :wearer/contribution-sites
                      (mapv :material/site stamps)
                      :wearer/view-instances [vi]})))))
       normalize-wearers))

(defn- compare-edn
  [a b]
  (compare (pr-str a) (pr-str b)))

(defn canonicalize
  "Recursively remove hash iteration order from an EDN value. Maps become
   pr-str-key-sorted maps, sets become sorted vectors, and other collections
   become vectors. Records are intentionally reduced to ordinary maps."
  [x]
  (cond
    (map? x)
    (into (sorted-map-by compare-edn)
          (map (fn [[k v]] [k (canonicalize v)]))
          x)

    (set? x)
    (->> x (map canonicalize) (sort-by pr-str) vec)

    (coll? x)
    (mapv canonicalize x)

    :else x))

(defn canonical-edn
  [x]
  (pr-str (canonicalize x)))
