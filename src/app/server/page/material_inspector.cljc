(ns app.server.page.material-inspector
  "Wearer rows derived from a client scene snapshot.
   Takes: rendered stamps, wearer ids, and master metadata.
   Gives: canonical wearer and material-inspector maps.
   Holds nothing."
  (:require [clojure.string :as str]))

(def ^:private stamp-keys
  [:material/subject
   :material/attachment
   :material/master
   :material/revision
   :material/site
   :material/role
   :material/slot])

(defn- contribution-stamp?
  [x]
  (and (map? x)
       (string? (:material/subject x))
       (vector? (:material/attachment x))
       (string? (:material/master x))
       (str/starts-with? (:material/master x) "fm:")
       (string? (:material/revision x))
       (keyword? (:material/site x))
       (keyword? (:material/role x))
       (keyword? (:material/slot x))))

(defn contribution-stamps
  "Find every facet contribution stamp nested in an arbitrary rendered tree
   value. Result order is canonical and duplicates are removed."
  [x]
  (letfn [(walk [v acc]
            (cond
              (map? v)
              (reduce (fn [a child] (walk child a))
                      (cond-> acc
                        (contribution-stamp? v)
                        (conj (select-keys v stamp-keys)))
                      (vals v))

              (coll? v)
              (reduce (fn [a child] (walk child a)) acc v)

              :else acc))]
    (->> (walk x #{})
         (sort-by (juxt :material/master
                        :material/revision
                        (comp pr-str :material/site)
                        (comp pr-str :material/role)
                        (comp pr-str :material/slot)
                        (comp pr-str :material/attachment)))
         vec)))

(defn normalize-wearers
  "Normalize the causal wearer snapshot into deterministic entity and facet
   order. Wearers are current-scene evidence, never durable attachment rows."
  [wearers]
  (->> wearers
       (reduce
        (fn [by-entity wearer]
          (let [entity-id (:wearer/entity-id wearer)]
            (if-not (string? entity-id)
              by-entity
              (reduce
               (fn [result facet]
                 (let [master-id (:wearer/master-id facet)
                       revision-id (:wearer/revision-id facet)]
                   (if-not (and (string? master-id)
                                (str/starts-with? master-id "fm:")
                                (string? revision-id))
                     result
                     (let [path [entity-id :facets [master-id revision-id]]]
                       (-> result
                           (update-in (conj path :attachments)
                                      (fnil into #{})
                                      (filter vector?
                                              (:wearer/attachments facet)))
                           (update-in (conj path :subjects)
                                      (fnil into #{})
                                      (filter string?
                                              (:wearer/subjects facet)))
                           (update-in (conj path :sites)
                                      (fnil into #{})
                                      (filter keyword?
                                              (:wearer/contribution-sites facet)))
                           (update-in (conj path :roles)
                                      (fnil into #{})
                                      (filter keyword?
                                              (:wearer/contribution-roles facet)))
                           (update-in (conj path :slots)
                                      (fnil into #{})
                                      (filter keyword?
                                              (:wearer/contribution-slots facet))))))))
               (update-in by-entity [entity-id :view-instances]
                          (fnil into #{})
                          (:wearer/view-instances wearer))
               (:wearer/facets wearer)))))
        {})
       (map
        (fn [[entity-id {:keys [facets view-instances]}]]
          {:wearer/entity-id entity-id
           :wearer/facets
           (->> facets
                (map
                 (fn [[[master-id revision-id]
                       {:keys [attachments subjects sites roles slots]}]]
                   {:wearer/master-id master-id
                    :wearer/revision-id revision-id
                    :wearer/attachments (vec (sort-by pr-str attachments))
                    :wearer/subjects (vec (sort subjects))
                    :wearer/contribution-sites
                    (vec (sort-by pr-str sites))
                    :wearer/contribution-roles
                    (vec (sort-by pr-str roles))
                    :wearer/contribution-slots
                    (vec (sort-by pr-str slots))}))
                (sort-by (juxt :wearer/master-id :wearer/revision-id))
                vec)
           :wearer/view-instances
           (vec (sort-by pr-str view-instances))}))
       (sort-by :wearer/entity-id)
       vec))

(defn wearers-from-scene-store
  "Project all current facet wearers from the scene store's ground-block
   slots. Entity identity comes from slot metadata; material evidence comes
   only from contribution stamps."
  [store]
  (->> (vals (:slots store))
       (keep
        (fn [{:keys [vi tree meta]}]
          (when-let [entity-id (:ground-block meta)]
            (let [stamps (contribution-stamps tree)]
              (when (seq stamps)
                {:wearer/entity-id entity-id
                 :wearer/facets
                 (->> stamps
                      (group-by
                       (juxt :material/master :material/revision))
                      (map
                       (fn [[[master-id revision-id] contribution-stamps]]
                         {:wearer/master-id master-id
                          :wearer/revision-id revision-id
                          :wearer/attachments
                          (mapv :material/attachment contribution-stamps)
                          :wearer/subjects
                          (mapv :material/subject contribution-stamps)
                          :wearer/contribution-sites
                          (mapv :material/site contribution-stamps)
                          :wearer/contribution-roles
                          (mapv :material/role contribution-stamps)
                          :wearer/contribution-slots
                          (mapv :material/slot contribution-stamps)}))
                      vec)
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
