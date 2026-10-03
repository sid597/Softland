(ns rig.probe.read-keys-probe
  "Probe for PLAN-read-exit.md: how Rama 1.6.0 orders PersistentVector keys
  in a subindexed map, and whether a range bounded by shorter vectors
  selects a prefix. Run from the rig folder:
  flock <lock> clojure -Sdeps '{:aliases {:probe {:extra-paths [\"runs/probes\"]}}}' -M:test:probe rig.probe.read-keys-probe"
  (:require [clojure.test :refer [deftest is]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]))

(defmodule KeyOrder [setup topologies]
  (declare-depot setup *in (hash-by first))
  (let [s (stream-topology topologies "s")]
    (declare-pstate s $$idx {clojure.lang.Keyword
                             (map-schema clojure.lang.PersistentVector Long
                                         {:subindex-options {:track-size? false}})})
    (<<sources s
      (source> *in :> [*layer *k *v])
      (local-transform> [(keypath *layer *k) (termval *v)] $$idx))))

(def nm1 [:alice :by-layer :offer #uuid "01890000-0000-7000-8000-000000000001"])
(def nm2 [:alice :by-layer :offer #uuid "01890000-0000-7000-8000-000000000002"])
(def lmax Long/MAX_VALUE)
(def big (+ (* 1790000000000 65536) 3))   ; a hybrid stamp: ms x 65536 + counter

(def ks [[:e0 :note 5 [nm1 0]]
         [:e0 :note 12 [nm2 1]]
         [:e0 :note big [nm2 0]]
         [:e0 :note 300 [nm1 1]]
         [:e0 :mention 7 [nm1 2]]
         [:e1 :note 3 [nm2 2]]
         [:e0a :note 4 [nm1 3]]
         [:e :note 9 [nm1 4]]
         [:e0 :note 12 [nm1 5]]])

(deftest key-order
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc KeyOrder {:tasks 2 :threads 1})
    (let [mn (get-module-name KeyOrder)
          d (foreign-depot ipc mn "*in")
          p (foreign-pstate ipc mn "$$idx")]
      (doseq [[i k] (map-indexed vector ks)] (foreign-append! d [:L k i]))
      (let [all (foreign-select [(keypath :L) MAP-KEYS] p)
            r-ek (foreign-select [(keypath :L) (sorted-map-range [:e0 :note] [:e0 :note 13]) MAP-KEYS] p)
            r-ek-all (foreign-select [(keypath :L) (sorted-map-range [:e0 :note] [:e0 :note lmax]) MAP-KEYS] p)
            r-e (foreign-select [(keypath :L) (sorted-map-range [:e0] [:e0 :zzzzzz]) MAP-KEYS] p)
            r-from (foreign-select [(keypath :L) (sorted-map-range-from [:e0 :note] {:max-amt 2}) MAP-KEYS] p)]
        (println "ALL-ORDER" (pr-str all))
        (println "RANGE [e0 note]..[e0 note 13)" (pr-str r-ek))
        (println "RANGE [e0 note]..[e0 note MAX)" (pr-str r-ek-all))
        (println "RANGE [e0]..[e0 :zzzzzz)" (pr-str r-e))
        (println "FROM [e0 note] max 2" (pr-str r-from))
        (is (= 9 (count all)))))))
