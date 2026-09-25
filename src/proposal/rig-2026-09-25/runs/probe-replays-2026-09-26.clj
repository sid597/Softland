;; Phase 8 plan-validation probe, 26 September 2026 (PLAN_VALIDATION-replays.md).
;; Two questions whose answers bear on PLAN-replays.md's "How it runs":
;;  1. RP2: can one in-process cluster launch, destroy and relaunch phase 1's
;;     Store (4 tasks, 2 threads) per case, and at what cost?
;;  2. B3/B4: while a microbatch topology is paused, does a stream
;;     topology's fan-out to every task (|all) still complete under an acked
;;     append; does the paused topology resume and process what waited; and
;;     is a module destroyed while paused relaunched paused or running?
;; Run from the rig folder, under the cluster lock:
;;   flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock \
;;     clojure -Sdeps '{:aliases {:probe {:jvm-opts ["-Xss16m" "-Xmx4g"]}}}' \
;;     -M:probe -i runs/probe-replays-2026-09-26.clj
(ns probe.replays
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [rig.store.module :as m]))

(defn ms [t0] (quot (- (System/nanoTime) t0) 1000000))

(defmodule Toy
  [setup topologies]
  (declare-depot setup *d1 (hash-by identity))
  (declare-depot setup *d2 (hash-by identity))
  (let [s (stream-topology topologies "s")
        mb (microbatch-topology topologies "mb")]
    (declare-pstate s $$all {Long Long})
    (declare-pstate mb $$mb {Long Long})
    (<<sources s
      (source> *d1 :> *x)
      (|all)
      (local-transform> [(keypath *x) (termval *x)] $$all))
    (<<sources mb
      (source> *d2 :> %mb)
      (%mb :> *x)
      (local-transform> [(keypath *x) (termval *x)] $$mb))))

(defn toy-state [ipc mn]
  (let [all (foreign-pstate ipc mn "$$all")
        mbp (foreign-pstate ipc mn "$$mb")]
    ;; 40 routing keys hash across the 4 tasks; #{7} = key 7 on every task reached
    {:all-7-seen (set (for [p (range 40)] (foreign-select-one (keypath 7) all {:pkey p})))
     :mb-3 (foreign-select-one (keypath 3) mbp {:pkey 3})}))

(with-open [ipc (rtest/create-ipc)]
  ;; 1. Store: launch, destroy, relaunch, three times
  (let [mn (get-module-name m/Store)]
    (doseq [i (range 3)]
      (let [t0 (System/nanoTime)]
        (rtest/launch-module! ipc m/Store {:tasks 4 :threads 2})
        (let [launched (ms t0)
              depot (foreign-depot ipc mn "*offers")
              t1 (System/nanoTime)
              r (foreign-append! depot {:layer :probe})
              appended (ms t1)
              t2 (System/nanoTime)]
          (rtest/destroy-module! ipc mn)
          (println "store round" i ": launch" launched "ms; one face-refused append"
                   appended "ms, ack keys" (pr-str (some-> r keys sort))
                   "; destroy" (ms t2) "ms")))))
  ;; 2. Toy: pause the microbatch topology, fan out on the stream side
  (let [mn (get-module-name Toy)]
    (rtest/launch-module! ipc Toy {:tasks 4 :threads 2})
    (let [d1 (foreign-depot ipc mn "*d1")
          d2 (foreign-depot ipc mn "*d2")]
      (foreign-append! d2 1)
      (rtest/wait-for-microbatch-processed-count ipc mn "mb" 1)
      (rtest/pause-microbatch-topology! ipc mn "mb")
      (foreign-append! d2 3)
      (let [t0 (System/nanoTime)]
        (foreign-append! d1 7)
        (println "paused: stream |all fan-out acked in" (ms t0) "ms"))
      (Thread/sleep 1500)
      (println "paused, 1.5 s later:" (pr-str (toy-state ipc mn))
               "(all-7-seen #{7} = the fan-out reached every task probed; mb-3 nil = micro held)")
      (rtest/resume-microbatch-topology! ipc mn "mb")
      (let [t0 (System/nanoTime)]
        (rtest/wait-for-microbatch-processed-count ipc mn "mb" 2)
        (println "resumed: the held record processed in" (ms t0) "ms;" (pr-str (toy-state ipc mn))))
      ;; destroyed while paused: is the relaunch paused?
      (rtest/pause-microbatch-topology! ipc mn "mb")
      (rtest/destroy-module! ipc mn)
      (rtest/launch-module! ipc Toy {:tasks 4 :threads 2})
      (let [d2 (foreign-depot ipc mn "*d2")]
        (foreign-append! d2 3)
        (let [t0 (System/nanoTime)
              done (deref (future (rtest/wait-for-microbatch-processed-count ipc mn "mb" 1) :ok)
                          10000 :timed-out)]
          (println "relaunched after a destroy while paused: micro processed?" done "in" (ms t0) "ms")))
      (rtest/destroy-module! ipc mn))))
(println "probe done")
(shutdown-agents)
(System/exit 0)
