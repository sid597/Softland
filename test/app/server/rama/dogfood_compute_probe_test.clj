(ns app.server.rama.dogfood-compute-probe-test
  "Depot-adversary probes for the compute kernel (fix session 1, batches 1-3).

   One IPC launch, one deftest, testing blocks ordered so the probes that can
   poison/kill an unfixed topology (blank run-id, unknown-run observation) and
   the one that closes the runtime run LAST — a pre-fix run then still shows
   every benign failure before the fatal ones.

   Every probe asserts the semantic payload the spec promises (status, argv,
   exit code, error reason), not just row existence."
  (:require [app.server.rama.dogfood.compute :as compute]
            [app.server.rama.probe-harness :as probe]
            [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer [foreign-append!]]))

(defn terminal-view?
  [view]
  (contains? #{:succeeded :failed} (:status view)))

(defn fence!
  "Settle fence: append a throwaway valid submit and await its decision. Once
   the fence's decision is visible, every record appended before the fence has
   been consumed by the topology (per-partition depot order + batch snapshot).
   The fence run targets a dangling inbox so it is never executed."
  [runtime]
  (let [fence-id (str "run_fence_" (System/nanoTime))]
    (compute/append-run-command!
      runtime
      (compute/run-command-request ["true"]
                                   {:run-id fence-id
                                    :request-id (str fence-id "/request")
                                    :executor-task-id "fence-inbox"}))
    (compute/await-decision runtime fence-id 5000)))

(defn submit-and-await-pending!
  [runtime run-id argv inbox-key]
  (let [request (compute/run-command-request argv
                                             {:run-id run-id
                                              :request-id (str run-id "/request")
                                              :time-ms 1
                                              :executor-task-id inbox-key})]
    (compute/append-run-command! runtime request)
    (compute/await-decision runtime run-id)
    (compute/await-run runtime run-id #(= :pending (:status %)))
    (compute/await-materialized
      #(compute/read-pending runtime inbox-key)
      #(contains? % run-id))
    request))

(defn claim-and-await-grant!
  [runtime run-id executor-id token inbox-key]
  (let [claim (compute/claim-record run-id executor-id
                                    {:claim-token token
                                     :executor-task-id inbox-key})]
    (compute/append-claim! runtime claim)
    (compute/await-run runtime run-id #(= :launching (:status %)))
    claim))

(defn run-to-terminal!
  [runtime run-id argv inbox-key]
  (let [request (submit-and-await-pending! runtime run-id argv inbox-key)
        result (compute/run-one-pending-local!
                 runtime
                 {:executor-id (str "exec-" run-id)
                  :executor-task-id inbox-key
                  :timeout-ms 5000})]
    (compute/await-view runtime run-id terminal-view? 5000)
    {:request request :result result}))

(defn run-truth
  "Full committed-truth snapshot for unchanged-truth probes."
  [runtime run-id inbox-key]
  {:decision (compute/read-decision runtime run-id)
   :run (compute/read-run runtime run-id)
   :view (compute/read-view runtime run-id)
   :pending? (contains? (compute/read-pending runtime inbox-key) run-id)})

(deftest compute-adversary-probe-matrix-test
  (let [runtime (compute/start-compute-runtime!)]
    (try
      (testing "B1-P1 duplicate submit after terminal: total no-op, no respawn surface"
        (let [run-id "run_b1p1"
              inbox "inbox-b1p1"
              {:keys [request]} (run-to-terminal! runtime run-id ["echo" "first"] inbox)
              result (probe/probe-duplicate-id-same-payload!
                       {:read-state #(run-truth runtime run-id inbox)
                        :append! #(compute/append-run-command! runtime request)
                        :settle! #(fence! runtime)})]
          (is (:pass? result) (pr-str (dissoc result :before :after)))
          (is (= :succeeded (get-in result [:after :run :status]))
              "terminal status must survive an identical redelivery")
          (is (= ["first"] (get-in result [:after :run :stdout-tail])))
          (is (false? (get-in result [:after :pending?]))
              "inbox must not re-arm the spawn path")))

      (testing "B1-P2 duplicate submit with different payload: decision never flips, no second inbox entry"
        (let [run-id "run_b1p2"
              inbox "inbox-b1p2"
              inbox-alt "inbox-b1p2-alt"
              _ (run-to-terminal! runtime run-id ["echo" "orig"] inbox)
              conflicting (compute/run-command-request
                            ["echo" "evil"]
                            {:run-id run-id
                             :request-id (str run-id "/request-2")
                             :time-ms 2
                             :executor-task-id inbox-alt})
              result (probe/probe-duplicate-id-different-payload!
                       {:read-state #(assoc (run-truth runtime run-id inbox)
                                            :pending-alt?
                                            (contains? (compute/read-pending runtime inbox-alt)
                                                       run-id))
                        :append! #(compute/append-run-command! runtime conflicting)
                        :settle! #(fence! runtime)})]
          (is (:pass? result) (pr-str (dissoc result :before :after)))
          (is (= :accepted (get-in result [:after :decision :decision/status]))
              "original decision must stand")
          (is (= (str run-id "/request")
                 (get-in result [:after :decision :request/id]))
              "decision must still record the FIRST request")
          (is (= ["echo" "orig"] (get-in result [:after :run :argv])))
          (is (false? (get-in result [:after :pending-alt?]))
              "reused run-id must not land in a second inbox")))

      (testing "B1-P4 winner-claim redelivery: grant fields byte-identical, inbox stays empty"
        (let [run-id "run_b1p4"
              inbox "inbox-b1p4"
              _ (submit-and-await-pending! runtime run-id ["echo" "unused"] inbox)
              claim (claim-and-await-grant! runtime run-id "exec-b1p4" "tok-b1p4" inbox)
              result (probe/probe-duplicate-id-same-payload!
                       {:read-state #(select-keys (compute/read-run runtime run-id)
                                                  [:status :claimed-by :claim-token
                                                   :claimed-at :updated-at])
                        :append! #(compute/append-claim! runtime claim)
                        :settle! #(fence! runtime)})]
          (is (:pass? result) (pr-str result))
          (is (= "exec-b1p4" (get-in result [:after :claimed-by])))
          (is (false? (contains? (compute/read-pending runtime inbox) run-id))
              "grant+removal pair must hold atomically across redelivery")))

      (testing "B2-P2 tokenless observation against a :pending run: not authorized, run stays claimable"
        (let [run-id "run_b2p2"
              inbox "inbox-b2p2"
              _ (submit-and-await-pending! runtime run-id ["echo" "ok"] inbox)
              forged {:run/id run-id
                      :observation/type :exit
                      :sequence 0
                      :exit-code 0
                      :observed-at 1}
              result (probe/probe-unauthorized-observation!
                       {:read-state #(select-keys (compute/read-run runtime run-id)
                                                  [:status :exit-code :stdout-tail
                                                   :finished-at :last-seq])
                        :append-obs! #(compute/append-observation! runtime forged)
                        :settle! #(fence! runtime)
                        :read-signal #(mapv :reason (:observation-errors
                                                      (compute/read-run runtime run-id)))})]
          (is (:pass? result) (pr-str result))
          (is (= :pending (get-in result [:after :status]))
              "a tokenless :exit must never close an ungranted run")
          (is (some #{:observation/not-authorized} (:signal-after result))
              "the forgery must be auditable in run truth")
          (is (some #(= :observation/not-authorized (:reason %))
                    (:observation-errors (compute/read-view runtime run-id)))
              "the forgery must be auditable in the view")
          (is (contains? (compute/read-pending runtime inbox) run-id)
              "run must remain discoverable")
          (let [res (compute/run-one-pending-local!
                      runtime {:executor-id "exec-b2p2"
                               :executor-task-id inbox
                               :timeout-ms 5000})
                view (compute/await-view runtime run-id terminal-view? 5000)]
            (is (= :granted-to-us (:claim-state res)) "run must still be claimable")
            (is (= :succeeded (:status view)) "and must complete normally"))))

      (testing "B2-P3 out-of-order drain + buffered duplicate keeps the FIRST payload"
        (let [run-id "run_b2p3"
              inbox "inbox-b2p3"
              token "tok-b2p3"
              _ (submit-and-await-pending! runtime run-id ["echo" "unused"] inbox)
              _ (claim-and-await-grant! runtime run-id "exec-b2p3" token inbox)]
          (compute/append-observation! runtime (compute/observation run-id token :exit 2 {:exit-code 0}))
          (compute/append-observation! runtime (compute/observation run-id token :stdout 1 {:line "ooo-line"}))
          ;; duplicate of buffered seq 2 with a DIFFERENT payload while the gap is open
          (compute/append-observation! runtime (compute/observation run-id token :exit 2 {:exit-code 1}))
          (compute/append-observation! runtime (compute/observation run-id token :started 0 {:pid 4242}))
          (let [view (compute/await-view runtime run-id terminal-view? 5000)
                row (compute/read-run runtime run-id)]
            (is (= :succeeded (:status view)) "first buffered :exit payload (code 0) must win")
            (is (= 0 (:exit-code view)))
            (is (= ["ooo-line"] (:stdout-tail view)) "drain must apply buffered output in order")
            (is (= 4242 (:pid row)))
            (is (= 2 (:last-seq row))))))

      (testing "B2-P4 heartbeat and unknown observation types never wedge the sequence"
        (let [run-id "run_b2p4"
              inbox "inbox-b2p4"
              token "tok-b2p4"
              _ (submit-and-await-pending! runtime run-id ["echo" "unused"] inbox)
              _ (claim-and-await-grant! runtime run-id "exec-b2p4" token inbox)]
          (compute/append-observation! runtime (compute/observation run-id token :started 0 {:pid 1}))
          (compute/append-observation! runtime (compute/observation run-id token :heartbeat 1 {}))
          (compute/append-observation! runtime (compute/observation run-id token :mystery-type 2 {:x 1}))
          (compute/append-observation! runtime (compute/observation run-id token :stdout 3 {:line "after-gap"}))
          (compute/append-observation! runtime (compute/observation run-id token :exit 4 {:exit-code 0}))
          (let [view (compute/await-view runtime run-id terminal-view? 5000)
                row (compute/read-run runtime run-id)]
            (is (= :succeeded (:status view)) "spec-named/unknown types must not block closure")
            (is (number? (:last-heartbeat-ms row)) ":heartbeat must record liveness")
            (is (some #{"after-gap"} (:stdout-tail view)))
            (is (some #(= :observation/type-invalid (:reason %))
                      (:observation-errors row))
                "unknown type must be auditable")
            (is (= 4 (:last-seq row)) "every type consumes its sequence"))))

      (testing "B2-P5 post-terminal authorized redelivery: silent, zero new error entries"
        (let [run-id "run_b2p5"
              inbox "inbox-b2p5"
              token "tok-b2p5"
              _ (submit-and-await-pending! runtime run-id ["echo" "unused"] inbox)
              _ (claim-and-await-grant! runtime run-id "exec-b2p5" token inbox)
              obs-stdout (compute/observation run-id token :stdout 1 {:line "done"})]
          (compute/append-observation! runtime (compute/observation run-id token :started 0 {:pid 7}))
          (compute/append-observation! runtime obs-stdout)
          (compute/append-observation! runtime (compute/observation run-id token :exit 2 {:exit-code 0}))
          (compute/await-view runtime run-id terminal-view? 5000)
          (let [result (probe/probe-post-terminal-write!
                         {:read-state #(compute/read-run runtime run-id)
                          :append-late! (fn []
                                          ;; redelivered applied seq + a NEW post-terminal exit
                                          (compute/append-observation! runtime obs-stdout)
                                          (compute/append-observation!
                                            runtime
                                            (compute/observation run-id token :exit 3 {:exit-code 1})))
                          :settle! #(fence! runtime)})]
            (is (:pass? result) (pr-str (dissoc result :before :after)))
            (is (= :succeeded (get-in result [:after :status])))
            (is (= 0 (get-in result [:after :exit-code])) "terminal exit code immutable")
            (is (= ["done"] (get-in result [:after :stdout-tail])) "no double-applied output")
            (is (empty? (get-in result [:after :observation-errors]))
                "authorized redelivery must NOT mint audit errors (C-12)"))))

      (testing "B2-P6 buffered later-seq observation cannot reopen a terminal run"
        (let [run-id "run_b2p6"
              inbox "inbox-b2p6"
              token "tok-b2p6"
              _ (submit-and-await-pending! runtime run-id ["echo" "unused"] inbox)
              _ (claim-and-await-grant! runtime run-id "exec-b2p6" token inbox)]
          ;; buffer order: :started AFTER :exit — once the exit drains the run
          ;; terminal, the buffered :started must be discarded, not applied
          (compute/append-observation! runtime (compute/observation run-id token :started 2 {:pid 9999}))
          (compute/append-observation! runtime (compute/observation run-id token :exit 1 {:exit-code 0}))
          (compute/append-observation! runtime (compute/observation run-id token :stdout 0 {:line "early"}))
          (let [view (compute/await-view runtime run-id terminal-view? 5000)]
            (is (= :succeeded (:status view))))
          (fence! runtime)
          (let [row (compute/read-run runtime run-id)]
            (is (= :succeeded (:status row)) "drain must not reopen terminal truth")
            (is (nil? (:pid row)) "the discarded buffered :started must not apply")
            (is (= {} (:obs-buffer row)) "buffer must be cleared at terminal"))))

      (testing "B2-P7 unauthorized observation with oversized fields: audit entry stays bounded"
        (let [run-id "run_b2p7"
              inbox "inbox-b2p7"
              token "tok-b2p7"
              _ (submit-and-await-pending! runtime run-id ["echo" "unused"] inbox)
              _ (claim-and-await-grant! runtime run-id "exec-b2p7" token inbox)
              huge (apply str (repeat 100000 "y"))
              hostile {:run/id run-id
                       :claim-token "wrong-token"
                       :observation/type huge
                       :sequence huge
                       :observed-at huge}]
          (compute/append-observation! runtime hostile)
          (let [row (compute/await-run
                      runtime run-id
                      (fn [r] (some #(= :observation/not-authorized (:reason %))
                                    (:observation-errors r))))
                entry (first (filter #(= :observation/not-authorized (:reason %))
                                     (:observation-errors row)))]
            (is (some? entry) "the forgery must be auditable")
            (is (nil? (:observation/type entry)) "oversized field must not be copied")
            (is (nil? (:sequence entry)) "oversized field must not be copied")
            (is (number? (:observed-at entry)) "garbage timestamp must be replaced")
            (is (= :launching (:status row)) "truth untouched"))))

      (testing "B3-P1 live logs: output visible before the process exits"
        (let [run-id "run_b3p1"
              request (compute/run-command-request
                        ["sh" "-c" "echo first; sleep 3; echo second"]
                        {:run-id run-id
                         :request-id (str run-id "/request")})]
          (compute/append-run-command! runtime request)
          (let [view (compute/await-view runtime run-id
                                         #(some #{"first"} (:stdout-tail %))
                                         2500)]
            (is (some #{"first"} (:stdout-tail view)) "stdout must stream live, not at EOF")
            (is (not (terminal-view? view)) "first line must be visible BEFORE terminal"))
          (let [final (compute/await-view runtime run-id terminal-view? 10000)]
            (is (= :succeeded (:status final)))
            (is (some #{"second"} (:stdout-tail final))))))

      (testing "B3-P3 grandchild pipe-holder cannot block run closure"
        (let [run-id "run_b3p3"
              inbox "inbox-b3p3"
              ;; the backgrounded sleep inherits stdout and holds the pipe open
              _ (submit-and-await-pending! runtime run-id
                                           ["sh" "-c" "(sleep 8 &); echo done"] inbox)
              t0 (System/currentTimeMillis)
              res (compute/run-one-pending-local!
                    runtime {:executor-id "exec-b3p3"
                             :executor-task-id inbox
                             :timeout-ms 4000})
              elapsed (- (System/currentTimeMillis) t0)]
          (is (= :granted-to-us (:claim-state res)))
          (let [view (compute/await-view runtime run-id terminal-view? 5000)]
            (is (= :succeeded (:status view)) "run must close although the pipe is held")
            (is (some #{"done"} (:stdout-tail view))))
          (is (< elapsed 6000)
              (str "pump-thread join must time out, not wait for the grandchild (took "
                   elapsed "ms)"))))

      (testing "B1-P3 blank/nil run-id submit: dropped without state, topology not poisoned"
        (let [base (compute/run-command-request ["echo" "x"]
                                                {:run-id "tmp_blank"
                                                 :request-id "req_blank"
                                                 :time-ms 1})
              blank (-> base
                        (assoc :run/id ""
                               :routing/key (compute/compute-routing-key ""))
                        (assoc-in [:payload :run/id] ""))
              nil-id (-> base
                         (assoc :run/id nil
                                :request/id "req_blank_nil"
                                :routing/key (compute/compute-routing-key nil))
                         (assoc-in [:payload :run/id] nil))]
          ;; raw appends — bypass the client-side refusal on purpose
          (foreign-append! (:compute-depot runtime) blank :append-ack)
          (foreign-append! (:compute-depot runtime) nil-id :append-ack)
          (fence! runtime)
          (is (nil? (compute/read-decision runtime "")) "no decision row under \"\"")
          (is (nil? (compute/read-run runtime "")) "no run row under \"\"")
          (is (nil? (compute/read-view runtime "")) "no view row under \"\"")
          (let [alive-id "run_b1p3_alive"]
            (submit-and-await-pending! runtime alive-id ["echo" "y"] "inbox-b1p3")
            (is (= :accepted (:decision/status (compute/read-decision runtime alive-id)))
                "topology must keep processing after nil/blank-keyed records"))))

      (testing "B2-P1 unknown-run observation: no phantom state, no poison record"
        (let [orphan {:run/id "run_never_submitted"
                      :claim-token "ghost"
                      :observation/type :stdout
                      :sequence 0
                      :line "boo"
                      :observed-at 1}
              result (probe/probe-append-before-request!
                       {:read-state #(compute/read-run runtime "run_never_submitted")
                        :append-obs! #(compute/append-observation! runtime orphan)
                        :settle! #(fence! runtime)})]
          (is (:pass? result) (pr-str result))
          (is (nil? (compute/read-view runtime "run_never_submitted")))
          (let [alive-id "run_b2p1_alive"]
            (submit-and-await-pending! runtime alive-id ["echo" "z"] "inbox-b2p1")
            (is (= :accepted (:decision/status (compute/read-decision runtime alive-id)))
                "topology must survive the orphan observation"))))

      (testing "B3-P2 close kills live worker processes (runs last: closes the runtime)"
        (let [run-id "run_b3p2"
              request (compute/run-command-request
                        ["sleep" "30"]
                        {:run-id run-id
                         :request-id (str run-id "/request")})]
          (compute/append-run-command! runtime request)
          (let [row (compute/await-run runtime run-id #(= :running (:status %)) 8000)
                pid (:pid row)]
            (is (= :running (:status row)))
            (is (number? pid))
            (compute/close-compute-runtime! runtime)
            (let [handle (.orElse (java.lang.ProcessHandle/of (long pid)) nil)
                  dead? (loop [i 0]
                          (cond
                            (or (nil? handle) (not (.isAlive ^java.lang.ProcessHandle handle))) true
                            (>= i 30) false
                            :else (do (Thread/sleep 100) (recur (inc i)))))]
              (is dead? "close must destroy live worker processes, not leak them")))))
      (finally
        (compute/close-compute-runtime! runtime)))))
