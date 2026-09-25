(ns rig.probe.read-build-probe
  "The read exit build's probe of the plan's [build checks]
  (PLAN-read-exit.md, 'Open questions', 'For the build'): a fixed-keys field
  chosen by a var in keypath; keypath with an index into a subindexed
  vector, in a query; <<query-topology, declare-depot and a second
  <<sources call from functions in another namespace; the page walk from
  'last address + U+0000'; and whether an exception in a query topology
  restarts the worker. Run from the rig folder:
  clojure -Sdeps '{:aliases {:probe {:extra-paths [\"runs/probes\"]}}}' -M:test:probe rig.probe.read-build-probe"
  (:require [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.test :as rtest]
            [rig.probe.read-build-helpers :as h]))

(def entry (fixed-keys-schema {:e clojure.lang.Keyword :v String :fid clojure.lang.PersistentVector :stamp Long}))

(def schema
  {clojure.lang.Keyword
   (fixed-keys-schema
    {:ix-a (map-schema String entry {:subindex-options {:track-size? false}})
     :ix-b (map-schema String entry {:subindex-options {:track-size? false}})
     :log (map-schema clojure.lang.PersistentVector (vector-schema entry {:subindex? true})
                      {:subindex-options {:track-size? false}})})})

(defmodule BuildProbe [setup topologies]
  (declare-depot setup *in (hash-by :layer))
  (h/declare-ops-depot! setup)
  (let [s (stream-topology topologies "s")]
    (declare-pstate s $$layers schema)
    (declare-pstate s $$clock Long {:initial-value 0})
    (<<sources s
      (source> *in {:retry-mode :all-after} :> *raw)
      (get *raw :layer :> *layer)
      (get *raw :puts :> *puts)
      (<<atomic
        (ops/explode *puts :> [*ix *ia *ie])
        (local-transform> [(keypath *layer *ix *ia) (termval *ie)] $$layers))
      (get *raw :log :> *log)
      (<<if (some? *log)
        (get *log :name :> *nm)
        (get *log :rows :> *rows)
        (local-transform> [(keypath *layer :log *nm) (termval *rows)] $$layers))
      (get *raw :stamp :> *stamp)
      (local-transform> [(termval *stamp)] $$clock)
      (ack-return> :ok))
    (h/add-ops-source! s))
  (h/declare-queries! topologies))

(def z "\u0000")
(defn- addr [& parts] (apply str (interpose z parts)))
(defn- ent [e v i] {:e e :v v :fid [[:L :by-layer :offer i] 0] :stamp (long i)})

(deftest read-build-probe
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc BuildProbe {:tasks 4 :threads 2})
    (let [mn (get-module-name BuildProbe)
          in (foreign-depot ipc mn "*in")
          ops (foreign-depot ipc mn "*ops")
          q (fn [qn & args] (apply foreign-invoke-query (foreign-query ipc mn qn) args))
          addrs (vec (for [e ["e0" "e0a" "e1"] k ["note" "mention"] s [5 12]]
                       (addr e k (format "%016x" s) (str "[n" s "]"))))
          puts (vec (concat (for [[i a] (map-indexed vector addrs)] [:ix-a a (ent :e0 (str "v" i) i)])
                            (for [[i a] (map-indexed vector addrs)] [:ix-b a (ent :e1 (str "w" i) i)])))
          nm [:L :by-layer :offer 1]
          rows [(ent :e0 "r0" 0) (ent :e0 "r1" 1) (ent :e0 "r2" 2)]]
      (testing "a fixed-keys field chosen by a var in keypath, in the stream topology"
        (is (= {"s" :ok} (foreign-append! in {:layer :L :puts puts :log {:name nm :rows rows} :stamp 77}))))
      (testing "the fields read back whole, sorted as Java strings"
        (is (= (sort addrs) (foreign-select [(keypath :L :ix-a) MAP-KEYS] (foreign-pstate ipc mn "$$layers"))))
        (is (= (count addrs) (count (foreign-select [(keypath :L :ix-b) MAP-KEYS] (foreign-pstate ipc mn "$$layers"))))))
      (testing "<<query-topology from a function: a page from an address, bare count, allow-yield; $$clock read"
        (let [r (q "page" :L :ix-a (addr "e0" "note" "") 3)]
          (println "PAGE" (pr-str r))
          (is (= (take 3 (drop-while #(neg? (compare % (addr "e0" "note" ""))) (sort addrs))) (:keys r)))
          (is (= 77 (:clock r)))))
      (testing "a tail read: the one entry just below a bound"
        (let [r (q "tail" :L :ix-a (addr "e0" "note" (format "%016x" 13)))]
          (println "TAIL" (pr-str r))
          (is (= [(addr "e0" "note" (format "%016x" 12) "[n12]")] (:keys r)))))
      (testing "keypath with an index into a subindexed vector, in a query"
        (is (= (rows 1) (q "row" :L nm 1)))
        (is (nil? (q "row" :L nm 7)) "past the end: nil")
        (is (nil? (q "row" :L [:L :by-layer :offer 99] 0)) "no such name: nil"))
      (testing "the page walk from 'last address + U+0000' reads every address once, in order"
        (let [r (q "walk" :L :ix-a 2)]
          (println "WALK" (count r) "addresses")
          (is (= (sort addrs) r))))
      (testing "a second <<sources call on the topology, from a function: the ops source deletes"
        (is (= {"s" {:deleted 5}} (foreign-append! ops {:layer :L :field :ix-b :n 5})))
        (is (= (drop 5 (sort addrs)) (foreign-select [(keypath :L :ix-b) MAP-KEYS] (foreign-pstate ipc mn "$$layers")))))
      (testing "a query that throws: what the client sees, and whether the worker restarts"
        (let [t0 (System/currentTimeMillis)
              r (try (q "boom" :boom) (catch Throwable e (str "THREW " (.getName (class e)) ": " (.getMessage e))))
              t1 (System/currentTimeMillis)
              a (foreign-append! in {:layer :L :puts [] :stamp 78})
              t2 (System/currentTimeMillis)
              p (q "page" :L :ix-a "" 1)
              t3 (System/currentTimeMillis)]
          (println "BOOM" (pr-str r) "| query ms" (- t1 t0) "| next append ms" (- t2 t1) (pr-str a)
                   "| next query ms" (- t3 t2) "clock" (:clock p))
          (is (= {"s" :ok} a))
          (is (= 78 (:clock p)) "the next append was processed and is visible"))))))
