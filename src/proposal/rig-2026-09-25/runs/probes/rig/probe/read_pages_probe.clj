(ns rig.probe.read-pages-probe
  "Probe for PLAN_VALIDATION-read-exit.md, fix F2: whether pages of a
  subindexed map keyed by PersistentVector names (the shape of `:answers`)
  can be walked with `sorted-map-range-from-start n` and then
  `sorted-map-range-from last {:max-amt n :inclusive? false}`, visiting
  every key exactly once, in the map's own order, although vector keys do
  not sort by element (read_keys_probe). Run as read-keys-probe was."
  (:require [clojure.test :refer [deftest is]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]))

(defmodule PageOrder [setup topologies]
  (declare-depot setup *in (hash-by first))
  (let [s (stream-topology topologies "s")]
    (declare-pstate s $$answers {clojure.lang.Keyword
                                 (map-schema clojure.lang.PersistentVector Long
                                             {:subindex-options {:track-size? false}})})
    (<<sources s
      (source> *in :> [*layer *nm *v])
      (local-transform> [(keypath *layer *nm) (termval *v)] $$answers))))

(def names
  (vec (for [i (range 23)]
         [:alice-hand :by-layer (if (even? i) :offer :crossing)
          (str (java.util.UUID/randomUUID))])))

(deftest page-order
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc PageOrder {:tasks 2 :threads 1})
    (let [mn (get-module-name PageOrder)
          d (foreign-depot ipc mn "*in")
          ps (foreign-pstate ipc mn "$$answers")]
      (doseq [[i nm] (map-indexed vector names)] (foreign-append! d [:L nm i]))
      (let [all (foreign-select [(keypath :L) MAP-KEYS] ps)
            pages (loop [acc [] after nil guard 0]
                    (let [page (if (nil? after)
                                 (foreign-select [(keypath :L) (sorted-map-range-from-start 4) MAP-KEYS] ps)
                                 (foreign-select [(keypath :L) (sorted-map-range-from after {:max-amt 4 :inclusive? false}) MAP-KEYS] ps))]
                      (if (or (empty? page) (> guard 50))
                        acc
                        (recur (conj acc page) (last page) (inc guard)))))
            walked (vec (apply concat pages))]
        (println "ALL-COUNT" (count all) "PAGES" (count pages) "PAGE-SIZES" (mapv count pages))
        (println "WALKED-COUNT" (count walked) "DISTINCT" (count (distinct walked)))
        (println "WALKED-EQUALS-MAP-ORDER" (= walked (vec all)))
        (println "WALKED-SET-EQUALS-NAMES" (= (set walked) (set names)))
        (is (= (count names) (count all)))
        (is (= walked (vec all)))
        (is (= (set walked) (set names)))))))
