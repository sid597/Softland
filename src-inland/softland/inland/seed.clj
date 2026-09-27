(ns softland.inland.seed
  "The workbench's genesis records, put into the store's base as facts by the
   operator. Takes the packaged seed (resources/inland/seed.edn); gives the acts
   that make the base's records say what the seed says. At the store's first
   launch `store/connect!` puts every record once the base is made; `-main`
   (`bin/inland seed`) puts again the records whose genesis changed, each change
   a new fact replacing the head it changes, never a rewrite. It reads the base
   through the operator's own view (rig.store.client `read-as-of`), which knows
   the base's one-owner era only: after the first group re-classes the base, a
   change to genesis is written through the editor like any other."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [rig.store.client :as c]
            [rig.store.micro-client :as mc]
            [softland.inland.facts :as f]))

(defn genesis
  "The packaged seed's records. Reads the resource; nothing is evaluated."
  []
  (edn/read-string (slurp (io/resource "inland/seed.edn"))))

(defn- base-rows
  "The base's facts as the operator's view holds them now, as read rows."
  [store]
  (for [fact (:facts (c/read-as-of store :base (c/clock store :base)))]
    (cond-> {:fid (:id fact) :e (:e fact) :k (:k fact) :stamp (:stamp fact) :replaces (:replaces fact)}
      (contains? fact :value) (assoc :value (:value fact))
      (contains? fact :erased-at) (assoc :erased-at (:erased-at fact)))))

(defn put-genesis!
  "One operator act per genesis record whose facts in the base differ from
   the seed's: the changed facts, each replacing its head. The answers."
  [store]
  (when (= :by-entity (mc/layer-class store :base))
    (throw (ex-info "The base is shared now; change its records through the editor." {})))
  (let [by-e (group-by :e (base-rows store))
        answers (vec (for [record (genesis)
                           :let [before (f/record-in (:name record) "base" (by-e (f/entity (:name record))))
                                 {:keys [facts]} (f/changes :base before record (:basis before))]
                           :when (seq facts)]
                       (assoc (mc/offer-into! store (c/build {:who :operator :layer :base :class (mc/layer-class store :base)
                                                              :facts facts}))
                              :record (:name record))))
        refused (remove #(= :yes (:answer %)) answers)]
    (when (seq refused)
      (throw (ex-info "Genesis records were refused." {:refused (mapv #(select-keys % [:record :answer :reason]) refused)})))
    answers))
