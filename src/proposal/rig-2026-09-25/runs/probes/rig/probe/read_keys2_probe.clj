(ns rig.probe.read-keys2-probe
  "Second probe for PLAN-read-exit.md: how Rama 1.6.0 orders standalone
  String keys and Long keys in a subindexed map, and whether a String range
  selects a prefix. Run as read-keys-probe."
  (:require [clojure.test :refer [deftest is]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]))

(defmodule KeyOrder2 [setup topologies]
  (declare-depot setup *in (hash-by first))
  (let [s (stream-topology topologies "s")]
    (declare-pstate s $$sidx {clojure.lang.Keyword
                              (map-schema String Long {:subindex-options {:track-size? false}})})
    (declare-pstate s $$lidx {clojure.lang.Keyword
                              (map-schema Long Long {:subindex-options {:track-size? false}})})
    (<<sources s
      (source> *in :> [*layer *kind *k *v])
      (<<if (= *kind :s)
        (local-transform> [(keypath *layer *k) (termval *v)] $$sidx)
       (else>)
        (local-transform> [(keypath *layer *k) (termval *v)] $$lidx)))))

(def z "\u0000")
(def big (+ (* 1790000000000 65536) 3))
(defn hx [n] (format "%016x" n))
(def skeys [(str "e0" z "note" z (hx 5)) (str "e0" z "note" z (hx 12)) (str "e0" z "note" z (hx big))
            (str "e0" z "mention" z (hx 7)) (str "e1" z "note" z (hx 3)) (str "e0a" z "note" z (hx 4))
            (str "e" z "note" z (hx 9)) "b" "ab" "abc" "été" "z"])
(def lkeys [5 12 300 big 7 -1 0])

(deftest key-order2
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc KeyOrder2 {:tasks 2 :threads 1})
    (let [mn (get-module-name KeyOrder2)
          d (foreign-depot ipc mn "*in")
          ps (foreign-pstate ipc mn "$$sidx")
          pl (foreign-pstate ipc mn "$$lidx")]
      (doseq [[i k] (map-indexed vector skeys)] (foreign-append! d [:L :s k i]))
      (doseq [[i k] (map-indexed vector lkeys)] (foreign-append! d [:L :l k i]))
      (let [show (fn [ks] (mapv #(clojure.string/replace % z "|") ks))
            all (foreign-select [(keypath :L) MAP-KEYS] ps)
            r-ek (foreign-select [(keypath :L) (sorted-map-range (str "e0" z "note" z) (str "e0" z "note" z (hx 13))) MAP-KEYS] ps)
            r-e (foreign-select [(keypath :L) (sorted-map-range (str "e0" z) (str "e0" "\u0001")) MAP-KEYS] ps)
            r-to (foreign-select [(keypath :L) (sorted-map-range-to (str "e0" z "note" z (hx 13)) {:max-amt 1}) MAP-KEYS] ps)
            lall (foreign-select [(keypath :L) MAP-KEYS] pl)
            lr (foreign-select [(keypath :L) (sorted-map-range 0 13) MAP-KEYS] pl)]
        (println "S-ALL-ORDER" (pr-str (show all)))
        (println "S-RANGE e0|note| .. e0|note|<13>" (pr-str (show r-ek)))
        (println "S-RANGE e0| .. e0\\u0001" (pr-str (show r-e)))
        (println "S-RANGE-TO e0|note|<13> max 1 (tail read)" (pr-str (show r-to)))
        (println "L-ALL-ORDER" (pr-str lall))
        (println "L-RANGE 0..13" (pr-str lr))
        (is (= (count skeys) (count all)))))))
