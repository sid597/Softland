(ns app.server.rama.probe-harness-test
  "The probe harness is test infrastructure for every later fix session, so it
   gets its own proof: each probe must pass against a guarded (atom-simulated)
   kernel AND fail against an unguarded one. A probe that cannot fail proves
   nothing."
  (:require [app.server.rama.core :as core]
            [app.server.rama.probe-harness :as probe]
            [clojure.test :refer [deftest is testing]]))

(def fast {:settle-ms 10})

;; ── await-materialized ───────────────────────────────────────────────────────

(deftest await-materialized-test
  (testing "returns as soon as the predicate holds"
    (let [!row (atom nil)]
      (future (Thread/sleep 50) (reset! !row {:status :pending}))
      (is (= {:status :pending}
             (probe/await-materialized #(deref !row) some? 2000)))))
  (testing "returns the last value on timeout instead of hanging"
    (is (nil? (probe/await-materialized (constantly nil) some? 100)))))

;; ── duplicate id, same payload ───────────────────────────────────────────────

(deftest probe-duplicate-id-same-payload-test
  (let [request {:run/id "run_1" :argv ["echo" "hi"] :status :pending}]
    (testing "passes against a kernel that guards initial inserts"
      (let [!rows (atom {"run_1" (assoc request :status :running)})
            result (probe/probe-duplicate-id-same-payload!
                     (merge fast
                            {:read-state #(get @!rows "run_1")
                             :append! #(swap! !rows update "run_1"
                                              (fn [row] (core/write-if-absent row request)))}))]
        (is (true? (:pass? result)))
        (is (= :running (get-in result [:after :status])))))
    (testing "fails against a kernel whose replay resets the row"
      (let [!rows (atom {"run_1" (assoc request :status :running)})
            result (probe/probe-duplicate-id-same-payload!
                     (merge fast
                            {:read-state #(get @!rows "run_1")
                             :append! #(swap! !rows assoc "run_1" request)}))]
        (is (false? (:pass? result)))
        (testing "the result carries the semantic regression, not just a boolean"
          (is (= :running (get-in result [:before :status])))
          (is (= :pending (get-in result [:after :status]))))))))

;; ── duplicate id, different payload ──────────────────────────────────────────

(deftest probe-duplicate-id-different-payload-test
  (let [committed {:run/id "run_1" :argv ["echo" "hi"] :status :running}
        impostor {:run/id "run_1" :argv ["rm" "-rf" "/"] :status :pending}]
    (testing "passes when the conflict is rejected and recorded as a signal"
      (let [!rows (atom {"run_1" committed})
            !conflicts (atom [])
            result (probe/probe-duplicate-id-different-payload!
                     (merge fast
                            {:read-state #(get @!rows "run_1")
                             :append! #(swap! !conflicts conj
                                              {:reason :request-id-conflict
                                               :run/id (:run/id impostor)})
                             :read-signal #(deref !conflicts)}))]
        (is (true? (:pass? result)))
        (is (= [] (:signal-before result)))
        (is (= :request-id-conflict (-> result :signal-after first :reason)))))
    (testing "fails when the impostor overwrites committed truth"
      (let [!rows (atom {"run_1" committed})
            result (probe/probe-duplicate-id-different-payload!
                     (merge fast
                            {:read-state #(get @!rows "run_1")
                             :append! #(swap! !rows assoc "run_1" impostor)}))]
        (is (false? (:pass? result)))
        (is (= ["rm" "-rf" "/"] (get-in result [:after :argv])))))))

;; ── append before request ────────────────────────────────────────────────────

(deftest probe-append-before-request-test
  (let [orphan-obs {:run/id "run_orphan" :obs/seq 1 :line "ghost"}]
    (testing "passes when an orphan observation dead-letters instead of minting a row"
      (let [!rows (atom {})
            !dead (atom [])
            result (probe/probe-append-before-request!
                     (merge fast
                            {:read-state #(get @!rows "run_orphan")
                             :append-obs! #(let [{:auth/keys [status dead-letter]}
                                                 (core/authorize-mutation
                                                   (get @!rows "run_orphan") orphan-obs
                                                   {:status-key :status})]
                                             (if (= :accepted status)
                                               (swap! !rows assoc "run_orphan" orphan-obs)
                                               (swap! !dead conj dead-letter)))
                             :read-signal #(deref !dead)}))]
        (is (true? (:pass? result)))
        (is (= :target-not-found
               (-> result :signal-after first :dead-letter/reason)))))
    (testing "fails when the observation creates a phantom row"
      (let [!rows (atom {})
            result (probe/probe-append-before-request!
                     (merge fast
                            {:read-state #(get @!rows "run_orphan")
                             :append-obs! #(swap! !rows assoc "run_orphan" orphan-obs)}))]
        (is (false? (:pass? result)))
        (is (= "ghost" (get-in result [:after :line])))))))

;; ── unauthorized observation ─────────────────────────────────────────────────

(deftest probe-unauthorized-observation-test
  (let [row {:run/id "run_1" :status :running :claim/token "good"}
        wrong-obs {:run/id "run_1" :claim/token "wrong" :stdout "evil"}
        auth-opts {:status-key :status
                   :accepting-statuses #{:running}
                   :terminal-statuses #{:succeeded :failed}
                   :claim-token-key :claim/token}]
    (testing "passes when the wrong-token observation is fenced out"
      (let [!rows (atom {"run_1" row})
            result (probe/probe-unauthorized-observation!
                     (merge fast
                            {:read-state #(get @!rows "run_1")
                             :append-obs! #(when (= :accepted
                                                    (:auth/status (core/authorize-mutation
                                                                    (get @!rows "run_1")
                                                                    wrong-obs auth-opts)))
                                             (swap! !rows update "run_1" merge wrong-obs))}))]
        (is (true? (:pass? result)))
        (is (nil? (get-in result [:after :stdout])))))
    (testing "fails when any token is good enough"
      (let [!rows (atom {"run_1" row})
            result (probe/probe-unauthorized-observation!
                     (merge fast
                            {:read-state #(get @!rows "run_1")
                             :append-obs! #(swap! !rows update "run_1" merge wrong-obs)}))]
        (is (false? (:pass? result)))
        (is (= "evil" (get-in result [:after :stdout])))))))

;; ── post-terminal write ──────────────────────────────────────────────────────

(deftest probe-post-terminal-write-test
  (let [terminal #{:succeeded :failed}
        done {:run/id "run_1" :status :succeeded :exit-code 0}
        late {:run/id "run_1" :status :running :exit-code nil}]
    (testing "passes when terminals are sticky"
      (let [!rows (atom {"run_1" done})
            result (probe/probe-post-terminal-write!
                     (merge fast
                            {:read-state #(get @!rows "run_1")
                             :append-late! #(swap! !rows update "run_1"
                                                   (fn [row]
                                                     (core/sticky-terminal-fence
                                                       terminal :status row late)))}))]
        (is (true? (:pass? result)))
        (is (= :succeeded (get-in result [:after :status])))))
    (testing "fails when a late write regresses the terminal row"
      (let [!rows (atom {"run_1" done})
            result (probe/probe-post-terminal-write!
                     (merge fast
                            {:read-state #(get @!rows "run_1")
                             :append-late! #(swap! !rows assoc "run_1" late)}))]
        (is (false? (:pass? result)))
        (is (= :running (get-in result [:after :status])))))))

;; ── collection past its bound ────────────────────────────────────────────────

(deftest probe-collection-bound-test
  (testing "passes when the collection enforces its cap"
    (let [bound 20
          !lines (atom [])
          result (probe/probe-collection-bound!
                   (merge fast
                          {:read-count #(count @!lines)
                           :push-n! (fn [n]
                                      (dotimes [i n]
                                        (swap! !lines #(vec (take-last bound (conj % i))))))
                           :bound bound
                           :overshoot 15}))]
      (is (true? (:pass? result)))
      (is (= bound (:count result)))))
  (testing "fails when the claimed bound is prose, not code"
    (let [bound 20
          !lines (atom [])
          result (probe/probe-collection-bound!
                   (merge fast
                          {:read-count #(count @!lines)
                           :push-n! (fn [n] (dotimes [i n] (swap! !lines conj i)))
                           :bound bound
                           :overshoot 15}))]
      (is (false? (:pass? result)))
      (is (= 35 (:count result))))))

;; ── matrix plumbing ──────────────────────────────────────────────────────────

(deftest failed-probes-test
  (let [results [{:probe :a :pass? true}
                 {:probe :b :pass? false :after {:status :pending}}
                 {:probe :c :pass? true}]]
    (is (= [{:probe :b :pass? false :after {:status :pending}}]
           (probe/failed-probes results)))))

(deftest run-adversary-matrix-returns-results-test
  (let [results (probe/run-adversary-matrix!
                  [(fn [] {:probe :a :pass? true})
                   (fn [] {:probe :b :pass? true})])]
    (is (= [:a :b] (mapv :probe results)))
    (is (every? :pass? results))))
