(ns app.server.rama.envelope-guards-test
  "Unit tests for the shared guarded-fold and observation/control authorization
   helpers in app.server.rama.envelope. Assertions target semantic payloads
   (statuses, reasons, fingerprints, dead-letter contents), not just presence."
  (:require [app.server.rama.envelope :as envelope]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

;; ── Fingerprints ─────────────────────────────────────────────────────────────

(deftest request-fingerprint-determinism-test
  (testing "equal values fingerprint identically regardless of construction order"
    (let [a (array-map :x 1 :y {:b 2 :a 1} :tags #{:p :q})
          b (array-map :tags #{:q :p} :y (hash-map :a 1 :b 2) :x 1)]
      (is (= a b))
      (is (= (envelope/canonical-str a) (envelope/canonical-str b)))
      (is (= (envelope/request-fingerprint a) (envelope/request-fingerprint b)))))
  (testing "different content yields a different fingerprint"
    (is (not= (envelope/request-fingerprint {:payload {:argv ["echo" "a"]}})
              (envelope/request-fingerprint {:payload {:argv ["echo" "b"]}})))))

;; ── Decision dedup gate ──────────────────────────────────────────────────────

(defn- sample-request
  [payload]
  (envelope/action-request {:request-id "req_1"
                        :request-type :compute/run-command
                        :time-ms 1
                        :payload payload}))

(deftest dedup-gate-fresh-request-proceeds-test
  (is (= {:gate/status :proceed}
         (envelope/decision-dedup-gate nil (sample-request {:argv ["echo" "hi"]})))))

(deftest dedup-gate-replay-returns-committed-decision-test
  (let [request (sample-request {:argv ["echo" "hi"]})
        event (envelope/kernel-event {:event-type :compute/run-accepted
                                  :target (:target request)})
        stored (-> (envelope/accepted-decision request event)
                   (envelope/with-request-fingerprint request))
        {:gate/keys [status decision]} (envelope/decision-dedup-gate stored request)]
    (is (= :replay status))
    (testing "the committed decision comes back unchanged apart from the replay mark"
      (is (true? (:decision/replay? decision)))
      (is (= :accepted (:decision/status decision)))
      (is (= (:decision/id stored) (:decision/id decision)))
      (is (= (:event/id event) (:event/id decision))))))

(deftest dedup-gate-conflict-rejects-without-touching-truth-test
  (let [request (sample-request {:argv ["echo" "hi"]})
        event (envelope/kernel-event {:event-type :compute/run-accepted
                                  :target (:target request)})
        stored (-> (envelope/accepted-decision request event)
                   (envelope/with-request-fingerprint request))
        impostor (sample-request {:argv ["rm" "-rf" "/"]})
        {:gate/keys [status decision]} (envelope/decision-dedup-gate stored impostor)]
    (is (= :conflict status))
    (testing "the conflict decision is a rejection that cannot alias the committed row"
      (is (= :rejected (:decision/status decision)))
      (is (= :request-id-conflict (:decision/reason decision)))
      (is (true? (:decision/conflict? decision)))
      (is (not= (:decision/id stored) (:decision/id decision)))
      (is (str/ends-with? (:decision/id decision) "/conflict")))
    (testing "both fingerprints are carried for forensics"
      (let [{:existing/keys [fingerprint] :as err} (first (:errors decision))]
        (is (= :request/id-conflict (:type err)))
        (is (= (:request/fingerprint stored) fingerprint))
        (is (= (envelope/request-fingerprint impostor) (:incoming/fingerprint err)))))))

(deftest dedup-gate-legacy-decision-without-fingerprint-replays-test
  (testing "a stored decision with no fingerprint cannot prove conflict — safe default is replay, no writes"
    (let [request (sample-request {:argv ["echo" "hi"]})
          stored (envelope/accepted-decision
                   request
                   (envelope/kernel-event {:event-type :compute/run-accepted
                                       :target (:target request)}))
          changed (sample-request {:argv ["echo" "DIFFERENT"]})
          {:gate/keys [status decision]} (envelope/decision-dedup-gate stored changed)]
      (is (= :replay status))
      (is (= :accepted (:decision/status decision))))))

;; ── Row guards ───────────────────────────────────────────────────────────────

(deftest write-if-absent-test
  (testing "absent row takes the proposed initial row"
    (is (= {:status :pending} (envelope/write-if-absent nil {:status :pending}))))
  (testing "a replayed initial insert never resets a row that has progressed"
    (is (= {:status :running :pid 42}
           (envelope/write-if-absent {:status :running :pid 42} {:status :pending}))))
  (testing "false is a present value, not absence"
    (is (false? (envelope/write-if-absent false {:status :pending})))))

(def terminal #{:succeeded :failed :cancelled})

(deftest sticky-status-test
  (is (= :running (envelope/sticky-status terminal :pending :running)))
  (testing "terminal statuses never regress"
    (is (= :succeeded (envelope/sticky-status terminal :succeeded :running)))
    (is (= :failed (envelope/sticky-status terminal :failed :pending)))))

(deftest sticky-terminal-fence-test
  (let [done {:status :succeeded :exit-code 0 :stdout-tail ["ok"]}
        late {:status :running :exit-code nil}]
    (testing "a live row takes the update"
      (is (= late (envelope/sticky-terminal-fence terminal :status
                                              {:status :running} late))))
    (testing "a terminal row keeps its full committed payload against a late write"
      (is (= done (envelope/sticky-terminal-fence terminal :status done late))))
    (testing "a nil row is not terminal — the proposed row lands"
      (is (= late (envelope/sticky-terminal-fence terminal :status nil late))))))

(deftest monotonic-watermark-test
  (is (= 7 (envelope/monotonic-watermark nil 7)))
  (is (= 7 (envelope/monotonic-watermark 7 nil)))
  (is (= 9 (envelope/monotonic-watermark 7 9)))
  (testing "a replayed older watermark never rewinds progress"
    (is (= 9 (envelope/monotonic-watermark 9 3)))
    (is (= 9 (envelope/monotonic-watermark 9 9)))))

;; ── Observation/control authorization ───────────────────────────────────────

(def auth-opts
  {:status-key :status
   :accepting-statuses #{:launching :running}
   :terminal-statuses #{:succeeded :failed}
   :claim-token-key :claim/token
   :record-token-key :claim/token
   :seq-key :obs/seq
   :watermark 5
   :context {:run/id "run_1"}})

(def live-row {:status :running :claim/token "granted-secret-token"})

(deftest authorize-mutation-accepted-test
  (is (= {:auth/status :accepted}
         (envelope/authorize-mutation live-row
                                  {:claim/token "granted-secret-token" :obs/seq 6}
                                  auth-opts))))

(deftest authorize-mutation-sequence-replay-test
  (testing "a sequence at or below the watermark is a duplicate delivery, not an error"
    (doseq [n [5 4 1]]
      (is (= {:auth/status :replay :auth/reason :sequence-replayed}
             (envelope/authorize-mutation live-row
                                      {:claim/token "granted-secret-token" :obs/seq n}
                                      auth-opts))))))

(deftest authorize-mutation-rejection-reasons-test
  (let [reason-of (fn [row record]
                    (let [{:auth/keys [status reason dead-letter]}
                          (envelope/authorize-mutation row record auth-opts)]
                      (is (= :rejected status))
                      (is (= reason (:dead-letter/reason dead-letter)))
                      reason))]
    (testing "garbage record shape"
      (is (= :record-not-map (reason-of live-row "not-a-map"))))
    (testing "observation before its request exists"
      (is (= :target-not-found
             (reason-of nil {:claim/token "granted-secret-token" :obs/seq 6}))))
    (testing "terminal rejects late writes, with the precise reason"
      (let [{:auth/keys [reason dead-letter]}
            (envelope/authorize-mutation {:status :succeeded :claim/token "granted-secret-token"}
                                     {:claim/token "granted-secret-token" :obs/seq 6}
                                     auth-opts)]
        (is (= :target-terminal reason))
        (is (= :succeeded (get-in dead-letter [:dead-letter/context :target/status])))))
    (testing "non-accepting state"
      (let [{:auth/keys [reason dead-letter]}
            (envelope/authorize-mutation {:status :pending :claim/token "granted-secret-token"}
                                     {:claim/token "granted-secret-token" :obs/seq 6}
                                     auth-opts)]
        (is (= :state-rejects-mutation reason))
        (is (= :pending (get-in dead-letter [:dead-letter/context :target/status])))))
    (testing "row with no claim granted yet cannot be observed"
      (is (= :no-claim-granted
             (reason-of {:status :running} {:claim/token "anything" :obs/seq 6}))))
    (testing "missing and wrong tokens"
      (is (= :token-missing (reason-of live-row {:obs/seq 6})))
      (is (= :token-mismatch
             (reason-of live-row {:claim/token "wrong" :obs/seq 6}))))
    (testing "non-numeric sequence"
      (is (= :sequence-invalid
             (reason-of live-row {:claim/token "granted-secret-token" :obs/seq "six"}))))))

(deftest authorize-mutation-never-leaks-expected-token-test
  (let [{:auth/keys [dead-letter]}
        (envelope/authorize-mutation live-row
                                 {:claim/token "wrong" :obs/seq 6}
                                 auth-opts)]
    (is (not (str/includes? (pr-str dead-letter) "granted-secret-token")))
    (is (str/includes? (:dead-letter/record-preview dead-letter) "wrong"))))

(deftest authorize-mutation-never-throws-test
  (testing "any input shape yields a value, never a throw"
    (is (= :record-not-map
           (:auth/reason (envelope/authorize-mutation nil nil {}))))
    (is (= :record-not-map
           (:auth/reason (envelope/authorize-mutation 42 "garbage" auth-opts))))
    (testing "even broken opts (contains? on a list throws) are fenced into a rejection"
      (let [{:auth/keys [status reason]}
            (envelope/authorize-mutation {:status :succeeded} {:ok true}
                                     {:status-key :status
                                      :terminal-statuses '(:succeeded)})]
        (is (= :rejected status))
        (is (= :authorization-error reason))))
    (testing "a non-map :context cannot break the fallback fence either"
      (let [{:auth/keys [status reason dead-letter]}
            (envelope/authorize-mutation nil {:ok true}
                                     {:status-key :status :context 42})]
        (is (= :rejected status))
        (is (= :authorization-error reason))
        (is (string? (get-in dead-letter [:dead-letter/context :error/class])))))))

(deftest bounded-dead-letter-test
  (testing "small records pass through with full preview"
    (let [dl (envelope/bounded-dead-letter :token-mismatch {:obs/seq 6}
                                       {:context {:run/id "run_1"}})]
      (is (= :token-mismatch (:dead-letter/reason dl)))
      (is (= (pr-str {:obs/seq 6}) (:dead-letter/record-preview dl)))
      (is (false? (:dead-letter/record-truncated? dl)))
      (is (= "run_1" (get-in dl [:dead-letter/context :run/id])))))
  (testing "unbounded payloads are truncated to the cap"
    (let [huge {:payload (apply str (repeat 10000 "x"))}
          dl (envelope/bounded-dead-letter :state-rejects-mutation huge)]
      (is (= envelope/default-dead-letter-preview-chars
             (count (:dead-letter/record-preview dl))))
      (is (true? (:dead-letter/record-truncated? dl))))))
