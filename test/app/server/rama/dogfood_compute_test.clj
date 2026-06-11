(ns app.server.rama.dogfood-compute-test
  (:require [app.server.rama.dogfood.compute :as compute]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(defn terminal-view?
  [view]
  (contains? #{:succeeded :failed} (:status view)))

(defn with-compute-runtime
  [f]
  (let [runtime (compute/start-compute-runtime!)]
    (try
      (f runtime)
      (finally
        (compute/close-compute-runtime! runtime)))))

(defn append-run-command-and-await-pending!
  [runtime run-id argv]
  (let [executor-task-id compute/pending-task-id
        request (compute/run-command-request
                  argv
                  {:run-id run-id
                   :request-id (str run-id "/request")
                   :time-ms 1
                   :executor-task-id executor-task-id})]
    (compute/append-run-command! runtime request)
    (compute/await-decision runtime run-id)
    (compute/await-run runtime run-id #(= :pending (:status %)))
    (compute/await-materialized
      #(compute/read-pending runtime executor-task-id)
      #(contains? % run-id))
    request))

(deftest task-global-executor-runs-command-automatically-test
  (with-compute-runtime
    (fn [runtime]
      (testing "normal compute requests are picked up by the module-owned TaskGlobal executor"
        (let [run-id "run_auto_echo"
              request (compute/run-command-request
                        ["echo" "task-global"]
                        {:run-id run-id
                         :request-id (str run-id "/request")
                         :time-ms 1})]
          (compute/append-run-command! runtime request)
          (compute/await-decision runtime run-id)
          (let [view (compute/await-view runtime run-id terminal-view? 5000)
                run-row (compute/read-run runtime run-id)]
            (is (= :succeeded (:status view)))
            (is (= 0 (:exit-code view)))
            (is (some #{"task-global"} (:stdout-tail view)))
            (is (string? (:executor/task-id run-row)))
            (is (str/starts-with? (:claimed-by run-row) "compute-executor-"))
            (is (not (contains? view :claim-token)))))))))

(deftest echo-command-runs-through-rama-owned-lifecycle-test
  (with-compute-runtime
    (fn [runtime]
      (testing "echo is accepted, claimed before spawn, observed, and materialized live"
        (let [run-id "run_echo"
              _ (append-run-command-and-await-pending! runtime run-id ["echo" "hello"])
              decision (compute/read-decision runtime run-id)
              run-before (compute/read-run runtime run-id)
              pending-before (compute/read-pending runtime)
              result (compute/run-one-pending-local!
                       runtime
                       {:executor-id "executor-echo"
                        :timeout-ms 5000})
              view (compute/await-view runtime run-id terminal-view? 5000)]
          (is (= :accepted (:decision/status decision)))
          (is (= :compute/run-command (:request/type decision)))
          (is (= :pending (:status run-before)))
          (is (contains? pending-before run-id))
          (is (= :granted-to-us (:claim-state result)))
          (is (:spawned? result))
          (is (:spawned-after-grant? result))
          (is (= :succeeded (:status view)))
          (is (= 0 (:exit-code view)))
          (is (some #{"hello"} (:stdout-tail view)))
          (is (not (contains? view :claim-token))))))))

(deftest false-command-materializes-failed-status-test
  (with-compute-runtime
    (fn [runtime]
      (testing "false is a successful compute request with a failed process exit"
        (let [run-id "run_false"
              _ (append-run-command-and-await-pending! runtime run-id ["false"])
              result (compute/run-one-pending-local!
                       runtime
                       {:executor-id "executor-false"
                        :timeout-ms 5000})
              view (compute/await-view runtime run-id terminal-view? 5000)]
          (is (= :accepted (:decision/status (compute/read-decision runtime run-id))))
          (is (= :granted-to-us (:claim-state result)))
          (is (:spawned? result))
          (is (:spawned-after-grant? result))
          (is (= :failed (:status view)))
          (is (pos? (:exit-code view))))))))

(deftest double-claim-and-wrong-token-observation-test
  (with-compute-runtime
    (fn [runtime]
      (testing "the first claim wins and the second claimant cannot spawn or write output"
        (let [run-id "run_double_claim"
              _ (append-run-command-and-await-pending! runtime run-id ["echo" "unused"])
              claim-a (compute/claim-record
                        run-id
                        "executor-a"
                        {:claim-token "token-a"
                         :claimed-at 10
                         :executor-task-id compute/pending-task-id})
              claim-b (compute/claim-record
                        run-id
                        "executor-b"
                        {:claim-token "token-b"
                         :claimed-at 11
                         :executor-task-id compute/pending-task-id})]
          (compute/append-claim! runtime claim-a :ack)
          ;; microbatch: depot ack does not imply PState visibility — poll
          (let [row-a (compute/await-run runtime run-id #(= :launching (:status %)))]
            (is (= :launching (:status row-a)))
            (is (= "executor-a" (:claimed-by row-a)))
            (is (= "token-a" (:claim-token row-a)))
            (is (not (contains? (compute/read-pending runtime) run-id)))
            (is (= :granted-to-us (compute/claim-state row-a claim-a))))

          (compute/append-claim! runtime claim-b :ack)
          (let [resolution (compute/await-claim-resolution runtime claim-b)
                row-b (:run resolution)]
            (is (= "executor-a" (:claimed-by row-b)))
            (is (= "token-a" (:claim-token row-b)))
            (is (= :conflict-or-past (compute/claim-state row-b claim-b)))
            (is (nil? (compute/run-one-pending-local!
                        runtime
                        {:executor-id "executor-b"
                         :timeout-ms 5000}))))

          (compute/append-observation!
            runtime
            (compute/observation run-id "token-b" :stdout 0 {:line "bad"})
            :ack)
          ;; microbatch: poll until the not-authorized error materializes
          (let [row-c (compute/await-run
                        runtime run-id
                        (fn [row]
                          (some #(= :observation/not-authorized (:reason %))
                                (:observation-errors row))))
                view-c (compute/read-view runtime run-id)]
            (is (= :launching (:status row-c)))
            (is (empty? (:stdout-tail row-c)))
            (is (some #(= :observation/not-authorized (:reason %))
                      (:observation-errors row-c)))
            (is (some #(= :observation/not-authorized (:reason %))
                      (:observation-errors view-c)))))))))
