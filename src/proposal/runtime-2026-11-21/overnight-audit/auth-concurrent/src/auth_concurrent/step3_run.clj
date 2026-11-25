(ns auth-concurrent.step3-run
  "Multi-threaded linearizability & post-read runner for Auth (Step 3).

  Accepts ANY implementation providing the 18 calls of `softland.auth.client`
  (`api` map + `make-cluster` fn) and drives it from 2 to 8 threads at once
  across all required contention workloads:
    1. Two invites (same claim different persons; one person two claims; 3-way
       triangle; up to 8 threads)
    2. Two enters of one account (first-time competing offers/claims + returning
       enters with differing clocks; up to 8 threads)
    3. Two accounts on one claim (1-claim and 2-claim fallback; up to 8 threads)
    4. Two answers to one ask (competing yes/no, new agents, differing clocks
       with take-out; up to 8 threads)
    5. `replace-secret` against `end-sign-in` (plus concurrent `who-is` &
       `watch-root`; up to 8 threads)
    6. `end-actor` against `open-session` and `make-actor` (plus `begin-sign-in`
       & `watch-root`; up to 8 threads)
    7. Full 8-thread mix exercising all 18 calls (`open`, `close!`, `watch-root`,
       all 12 writes, and all 3 direct reads), including `{:could-not ...}` write
       ambiguity check.

  Verifies:
    (A) `test-auth/softland/auth/reference.clj` (with lock serializing calls)
        passes 100% across 2..8 threads;
    (B) `:could-not` write ambiguity handling passes both when a timed-out write
        actually took effect and when it did not;
    (C) All 6 purposefully racing copies in `auth-concurrent.racy-copies` pass
        when run sequentially (1 thread), and ALL 6 are caught under 2..8-thread
        concurrency."
  (:require [auth-concurrent.linearizability :as lin]
            [auth-concurrent.racy-copies :as racy]
            [softland.auth.reference :as ref])
  (:import [java.util UUID]
           [java.util.concurrent Callable CyclicBarrier Executors TimeUnit]))

(defn hex64 [^long seed]
  (apply str (repeatedly 4 #(format "%016x" (+ seed 0x123456789abcdef0)))))

(defn h-of [^long n]
  (format "%064x" n))

(defn code-of [^long n]
  (format "ASK%05d" n))

(defn invoke-api [api client kind arg]
  (case kind
    :invite         ((:invite api) client arg)
    :enter          ((:enter api) client arg)
    :own-lane-made ((:own-lane-made api) client arg)
    :begin-sign-in  ((:begin-sign-in api) client arg)
    :replace-secret ((:replace-secret api) client arg)
    :end-sign-in    ((:end-sign-in api) client arg)
    :make-actor     ((:make-actor api) client arg)
    :end-actor      ((:end-actor api) client arg)
    :open-session   ((:open-session api) client arg)
    :close-session  ((:close-session api) client arg)
    :ask            ((:ask api) client arg)
    :answer-ask     ((:answer-ask api) client arg)
    :who-is         ((:who-is api) client arg)
    :ask-of         ((:ask-of api) client arg)
    :under          ((:under api) client arg)))

(defn apply-seed!
  "Applies deterministic setup writes sequentially to both the live `client`
  and `ref/empty-state`, returning `init-ref-state`."
  [api client seed-calls]
  (reduce
   (fn [st [kind arg]]
     (let [actual (invoke-api api client kind arg)
           [st' expected] (ref/step st [kind arg])]
       (when (not= actual expected)
         (throw (ex-info "Seed step diverged before concurrent phase"
                         {:call [kind arg] :actual actual :expected expected})))
       st'))
   ref/empty-state
   seed-calls))

(defn run-concurrent-batch!
  "Runs `thread-ops` (1 op or small op-list per thread, `2 <= threads <= 8`)
  simultaneously behind a `CyclicBarrier` across multiple open clients of the
  same Auth module, collects post-batch reads (`who-is`, `ask-of`, `under`,
  `watch-root`), closes clients, and checks causal linearizability against
  `reference.clj`."
  [{:keys [api make-cluster]} {:keys [name seeds thread-ops watched-roots query-targets serialize-lock]}]
  (let [mod-name "auth-test"
        cluster (make-cluster mod-name)
        c1 ((:open api) cluster mod-name)
        c2 ((:open api) cluster mod-name)
        clients [c1 c2]
        wrap-call (if serialize-lock
                    (fn [f] (locking serialize-lock (f)))
                    (fn [f] (f)))
        init-state (apply-seed! api c1 seeds)
        ;; Register watch-root for watched-roots before concurrent phase
        watches (mapv (fn [P]
                        (let [seen (atom ::lin/no-callback)
                              init-live (ref/live-under init-state P)
                              stop ((:watch-root api) c1 P (fn [v] (reset! seen v)))]
                          {:root P :seen seen :init-live init-live :stop stop}))
                      watched-roots)
        n-threads (count thread-ops)
        barrier (CyclicBarrier. n-threads)
        pool (Executors/newFixedThreadPool n-threads)
        id-gen (atom 0)]
    (try
      (let [futs (mapv
                  (fn [tid ops-for-thread]
                    (.submit
                     pool
                     ^Callable
                     (fn []
                       (let [c (nth clients (mod tid (count clients)))]
                         (.await barrier 10 TimeUnit/SECONDS)
                         (mapv
                          (fn [[kind arg]]
                            (let [op-id (swap! id-gen inc)
                                  t0 (System/nanoTime)
                                  res (wrap-call #(invoke-api api c kind arg))
                                  t1 (System/nanoTime)]
                              {:id op-id
                               :thread tid
                               :kind kind
                               :arg arg
                               :result res
                               :invoked-ns t0
                               :returned-ns t1}))
                          ops-for-thread)))))
                  (range n-threads)
                  thread-ops)
            history (vec (mapcat #(.get ^java.util.concurrent.Future % 15 TimeUnit/SECONDS) futs))
            ;; Post-batch read sweep across all touched entities
            post-reads
            (vec
             (concat
              (for [q (:who-is query-targets)]
                {:kind :who-is :arg q :result (wrap-call #((:who-is api) c2 q))})
              (for [u (:ask-of query-targets)]
                {:kind :ask-of :arg u :result (wrap-call #((:ask-of api) c2 u))})
              (for [p (:under query-targets)]
                {:kind :under :arg p :result (wrap-call #((:under api) c2 p))})
              (for [{:keys [root seen init-live stop]} watches]
                (do
                  (when (fn? stop) (stop))
                  {:kind :watch-root-last :arg root :result @seen :initial-live init-live}))))]
        ;; Verify close! contract on secondary client c2
        ((:close! api) c2)
        (let [after-close ((:under api) c2 (or (first (:under query-targets)) (UUID/randomUUID)))]
          ((:close! api) c1)
          (if (not= after-close {:could-not :closed})
            {:ok? false :scenario name :threads n-threads :reason {:type :close-did-not-return-closed :got after-close}}
            (assoc (lin/linearizable? init-state history post-reads)
                   :scenario name
                   :threads n-threads))))
      (finally
        (.shutdownNow pool)))))

;; =============================================================================
;; Contention Scenario Generators (2 to 8 threads)
;; =============================================================================

(defn build-scenarios []
  (let [P1 (UUID/randomUUID) P2 (UUID/randomUUID) P3 (UUID/randomUUID) P4 (UUID/randomUUID)
        L1 (UUID/randomUUID) L2 (UUID/randomUUID) L3 (UUID/randomUUID) L4 (UUID/randomUUID)
        M1 {:uuid (UUID/randomUUID) :session (UUID/randomUUID)}
        M2 {:uuid (UUID/randomUUID) :session (UUID/randomUUID)}
        M3 {:uuid (UUID/randomUUID) :session (UUID/randomUUID)}
        A1 (UUID/randomUUID) A2 (UUID/randomUUID) A3 (UUID/randomUUID) A4 (UUID/randomUUID)
        G1 (UUID/randomUUID) G2 (UUID/randomUUID) G3 (UUID/randomUUID) G4 (UUID/randomUUID)
        S1 (UUID/randomUUID) S2 (UUID/randomUUID) S3 (UUID/randomUUID) S4 (UUID/randomUUID)
        H1 (h-of 1) H2 (h-of 2) H3 (h-of 3) H4 (h-of 4)
        C1 {:by :email :email "alice@example.com"}
        C2 {:by :email :email "bob@example.com"}
        C3 {:by :code :hash (h-of 10)}
        Acc1 [:google "sub-1"]
        Acc2 [:google "sub-2"]
        Acc3 [:github "sub-3"]
        Acc4 [:github "sub-4"]
        U1 (code-of 1)]
    [;; 1a. Two invites on one claim (2 threads)
     {:name "1a. two-invites-same-claim (2 threads)"
      :seeds []
      :watched-roots []
      :thread-ops [[[:invite {:person P1 :claim C1 :invited-by :system :at 100}]]
                   [[:invite {:person P2 :claim C1 :invited-by :system :at 110}]]]
      :query-targets {:who-is [{:root P1 :sign-in G1 :session nil}
                               {:root P2 :sign-in G1 :session nil}]
                      :ask-of []
                      :under [P1 P2]}}

     ;; 1b. Two invites of one person under two claims + competing claim (4 threads)
     {:name "1b. invites-one-person-two-claims-and-triangle (4 threads)"
      :seeds []
      :watched-roots []
      :thread-ops [[[:invite {:person P1 :claim C1 :invited-by :system :at 100}]]
                   [[:invite {:person P1 :claim C2 :invited-by :system :at 110}]]
                   [[:invite {:person P2 :claim C1 :invited-by :system :at 120}]]
                   [[:enter {:account Acc1 :says {"k" "1"} :email "alice@example.com"
                             :code-hash nil :own-lane L1 :making M1 :at 130}]]]
      :query-targets {:who-is [{:root P1 :sign-in G1 :session nil}
                               {:root P2 :sign-in G1 :session nil}]
                      :ask-of []
                      :under [P1 P2]}}

     ;; 2a. Two enters of one account proving C1 vs C2 with different offers & clocks (3 threads)
     {:name "2a. two-enters-one-account-first-time (3 threads)"
      :seeds [[:invite {:person P1 :claim C1 :invited-by :system :at 10}]
              [:invite {:person P2 :claim C2 :invited-by :system :at 10}]]
      :watched-roots []
      :thread-ops [[[:enter {:account Acc1 :says {"src" "t200"} :email "alice@example.com"
                             :code-hash nil :own-lane L1 :making M1 :at 200}]]
                   [[:enter {:account Acc1 :says {"src" "t100"} :email "bob@example.com"
                             :code-hash nil :own-lane L2 :making M2 :at 100}]]
                   [[:who-is {:root P1 :sign-in G1 :session nil}]]]
      :query-targets {:who-is [{:root P1 :sign-in G1 :session nil}
                               {:root P2 :sign-in G1 :session nil}]
                      :ask-of []
                      :under [P1 P2]}}

     ;; 2b. Returning enters of one account racing own-lane-made (5 threads)
     {:name "2b. returning-enters-vs-own-lane-made (5 threads)"
      :seeds [[:invite {:person P1 :claim C1 :invited-by :system :at 10}]
              [:enter {:account Acc1 :says {"init" "1"} :email "alice@example.com"
                       :code-hash nil :own-lane L1 :making M1 :at 20}]]
      :watched-roots []
      :thread-ops [[[:enter {:account Acc1 :says {"s" "t300"} :email nil :code-hash nil
                             :own-lane L2 :making M2 :at 300}]]
                   [[:enter {:account Acc1 :says {"s" "t150"} :email nil :code-hash nil
                             :own-lane L3 :making M3 :at 150}]]
                   [[:own-lane-made {:person P1 :at 250}]]
                   [[:enter {:account Acc1 :says {"s" "t400"} :email nil :code-hash nil
                             :own-lane L4 :making M1 :at 400}]]
                   [[:who-is {:root P1 :sign-in G1 :session nil}]]]
      :query-targets {:who-is [{:root P1 :sign-in G1 :session nil}]
                      :ask-of []
                      :under [P1]}}

     ;; 3. Two (and three) accounts proving one claim (1-claim & 2-claim fallback, 4 threads)
     {:name "3. two-accounts-on-one-claim (4 threads)"
      :seeds [[:invite {:person P1 :claim C3 :invited-by :system :at 10}]
              [:invite {:person P2 :claim C2 :invited-by :system :at 10}]]
      :watched-roots []
      :thread-ops [[[:enter {:account Acc1 :says {"a" "1"} :email nil :code-hash (:hash C3)
                             :own-lane L1 :making M1 :at 100}]]
                   [[:enter {:account Acc2 :says {"a" "2"} :email "bob@example.com" :code-hash (:hash C3)
                             :own-lane L2 :making M2 :at 110}]]
                   [[:enter {:account Acc3 :says {"a" "3"} :email nil :code-hash (:hash C3)
                             :own-lane L3 :making M3 :at 120}]]
                   [[:who-is {:root P1 :sign-in G1 :session nil}]]]
      :query-targets {:who-is [{:root P1 :sign-in G1 :session nil}
                               {:root P2 :sign-in G1 :session nil}]
                      :ask-of []
                      :under [P1 P2]}}

     ;; 4. Two (and more) answers to one ask with new agents & differing clocks (6 threads)
     {:name "4. two-answers-to-one-ask (6 threads)"
      :seeds [[:invite {:person P1 :claim C1 :invited-by :system :at 10}]
              [:enter {:account Acc1 :says {} :email "alice@example.com" :code-hash nil
                       :own-lane L1 :making M1 :at 20}]
              [:own-lane-made {:person P1 :at 25}]
              [:invite {:person P2 :claim C2 :invited-by :system :at 10}]
              [:enter {:account Acc2 :says {} :email "bob@example.com" :code-hash nil
                       :own-lane L2 :making M2 :at 20}]
              [:own-lane-made {:person P2 :at 25}]
              [:begin-sign-in {:root P1 :actor P1 :sign-in G3 :kind :browser :hash H1
                               :until 1500 :label "expiring" :at 30}]
              [:ask {:code U1 :hash H2 :asks "cli-tool" :ends 5000 :at 40}]]
      :watched-roots [P1 P2]
      :thread-ops [[[:answer-ask {:code U1 :yes? true :root P1 :actor A1
                                  :new-agent {:label "bot-1"} :sign-in G1 :until 9000 :at 500}]]
                   [[:answer-ask {:code U1 :yes? true :root P2 :actor A2
                                  :new-agent {:label "bot-2"} :sign-in G2 :until 9000 :at 600}]]
                   [[:answer-ask {:code U1 :yes? false :root P1 :actor P1
                                  :new-agent nil :sign-in G4 :until 9000 :at 2000}]]
                   [[:ask-of U1]]
                   [[:under P1]]
                   [[:under P2]]]
      :query-targets {:who-is [{:root P1 :sign-in G1 :session nil}
                               {:root P1 :sign-in G3 :session nil}
                               {:root P2 :sign-in G2 :session nil}]
                      :ask-of [U1]
                      :under [P1 P2]}}

     ;; 5. replace-secret against end-sign-in (plus competing replace-secret & open-session, 6 threads)
     {:name "5. replace-secret-vs-end-sign-in (6 threads)"
      :seeds [[:invite {:person P1 :claim C1 :invited-by :system :at 10}]
              [:enter {:account Acc1 :says {} :email "alice@example.com" :code-hash nil
                       :own-lane L1 :making M1 :at 20}]
              [:own-lane-made {:person P1 :at 25}]
              [:begin-sign-in {:root P1 :actor P1 :sign-in G1 :kind :browser :hash H1
                               :until 5000 :label "browser" :at 30}]
              [:open-session {:root P1 :sign-in G1 :session S1 :until 4000
                              :driven-by {:kind :person} :at 35}]]
      :watched-roots [P1]
      :thread-ops [[[:replace-secret {:root P1 :sign-in G1 :old-hash H1 :new-hash H2
                                      :until 8000 :previous-until 4500 :at 100}]]
                   [[:end-sign-in {:root P1 :sign-in G1 :at 110}]]
                   [[:replace-secret {:root P1 :sign-in G1 :old-hash H1 :new-hash H3
                                      :until 8500 :previous-until 4500 :at 105}]]
                   [[:open-session {:root P1 :sign-in G1 :session S2 :until 7000
                                    :driven-by {:kind :model :model "opus"} :at 115}]]
                   [[:who-is {:root P1 :sign-in G1 :session S1}]]
                   [[:who-is {:root P1 :sign-in G1 :session S2}]]]
      :query-targets {:who-is [{:root P1 :sign-in G1 :session S1}
                               {:root P1 :sign-in G1 :session S2}]
                      :ask-of []
                      :under [P1]}}

     ;; 6. end-actor against open-session and make-actor (7 threads)
     {:name "6. end-actor-vs-open-session-and-make-actor (7 threads)"
      :seeds [[:invite {:person P1 :claim C1 :invited-by :system :at 10}]
              [:enter {:account Acc1 :says {} :email "alice@example.com" :code-hash nil
                       :own-lane L1 :making M1 :at 20}]
              [:own-lane-made {:person P1 :at 25}]
              [:make-actor {:root P1 :actor A1 :made-by P1 :label "parent-agent" :at 30}]
              [:make-actor {:root P1 :actor A2 :made-by A1 :label "child-agent" :at 35}]
              [:begin-sign-in {:root P1 :actor A2 :sign-in G1 :kind :program :hash H1
                               :until 6000 :label "ag-sig" :at 40}]
              [:open-session {:root P1 :sign-in G1 :session S1 :until 5000 :at 45}]]
      :watched-roots [P1]
      :thread-ops [[[:end-actor {:root P1 :actor A1 :at 100}]]
                   [[:open-session {:root P1 :sign-in G1 :session S2 :until 7000
                                    :driven-by {:kind :model :model "gemini"} :at 105}]]
                   [[:make-actor {:root P1 :actor A3 :made-by A1 :label "late-child-1" :at 110}]]
                   [[:make-actor {:root P1 :actor A4 :made-by A2 :label "late-grandchild" :at 112}]]
                   [[:begin-sign-in {:root P1 :actor A1 :sign-in G2 :kind :program :hash H2
                                     :until 7000 :label "late-sig" :at 115}]]
                   [[:under P1]]
                   [[:who-is {:root P1 :sign-in G1 :session S2}]]]
      :query-targets {:who-is [{:root P1 :sign-in G1 :session S1}
                               {:root P1 :sign-in G1 :session S2}
                               {:root P1 :sign-in G2 :session nil}]
                      :ask-of []
                      :under [P1]}}

     ;; 7. Full 8-thread stress across root operations, sessions, asks, and reads (8 threads)
     {:name "7. full-8-thread-mixed-contention (8 threads)"
      :seeds [[:invite {:person P1 :claim C1 :invited-by :system :at 10}]
              [:enter {:account Acc1 :says {} :email "alice@example.com" :code-hash nil
                       :own-lane L1 :making M1 :at 20}]
              [:own-lane-made {:person P1 :at 25}]
              [:make-actor {:root P1 :actor A1 :made-by P1 :label "ag-1" :at 30}]
              [:begin-sign-in {:root P1 :actor A1 :sign-in G1 :kind :program :hash H1
                               :until 6000 :label "g1" :at 35}]
              [:open-session {:root P1 :sign-in G1 :session S1 :until 5000 :at 40}]
              [:ask {:code U1 :hash H3 :asks "agent-cli" :ends 5000 :at 45}]]
      :watched-roots [P1]
      :thread-ops [[[:open-session {:root P1 :sign-in G1 :session S1 :until 8000 :at 100}]]
                   [[:close-session {:root P1 :session S1 :at 105}]]
                   [[:replace-secret {:root P1 :sign-in G1 :old-hash H1 :new-hash H2
                                      :until 9000 :previous-until 5500 :at 110}]]
                   [[:answer-ask {:code U1 :yes? true :root P1 :actor A2
                                  :new-agent {:label "ask-ag"} :sign-in G2 :until 9000 :at 115}]]
                   [[:answer-ask {:code U1 :yes? false :root P1 :actor P1
                                  :new-agent nil :sign-in G3 :until 9000 :at 120}]]
                   [[:invite {:person P3 :claim C2 :invited-by A1 :at 125}]]
                   [[:enter {:account Acc4 :says {"k" "v"} :email "bob@example.com"
                             :code-hash nil :own-lane L3 :making M3 :at 130}]]
                   [[:who-is {:root P1 :sign-in G1 :session S1}]]]
      :query-targets {:who-is [{:root P1 :sign-in G1 :session S1}
                               {:root P1 :sign-in G2 :session nil}
                               {:root P3 :sign-in G1 :session nil}]
                      :ask-of [U1]
                      :under [P1 P3]}}]))

(defn check-suite!
  "Runs all contention scenarios for `rounds` iterations against `{:api :make-cluster}`.
  Stops on the first linearizability failure if `stop-on-fail?` is true."
  [target {:keys [rounds serialize-lock? stop-on-fail?]
           :or {rounds 10 serialize-lock? false stop-on-fail? true}}]
  (let [lock-obj (when serialize-lock? (Object.))]
    (loop [r 0
           scenarios-run 0]
      (if (= r rounds)
        {:passed? true :rounds rounds :batches-checked scenarios-run}
        (let [scs (build-scenarios)
              failure (loop [rem-scs scs
                             cnt 0]
                        (when (seq rem-scs)
                          (let [sc (assoc (first rem-scs) :serialize-lock lock-obj)
                                res (run-concurrent-batch! target sc)]
                            (if (:ok? res)
                              (recur (rest rem-scs) (inc cnt))
                              (assoc res :round r :batches-before-fail (+ scenarios-run cnt))))))]
          (if (and failure stop-on-fail?)
            (assoc failure :passed? false)
            (recur (inc r) (+ scenarios-run (count scs)))))))))

(defn verify-could-not-ambiguity!
  "Explicitly verifies that a write returning `{:could-not :reach-the-module}` is
  accepted by the runner BOTH when the write silently took effect and when it
  was dropped."
  []
  (let [ref-api (lin/api-from-ns 'softland.auth.reference)
        wrap-ambiguous
        (fn [take-on-fail?]
          {:api (assoc ref-api
                       :invite (fn [c inp]
                                 (when take-on-fail?
                                   ((:invite ref-api) c inp))
                                 {:could-not :reach-the-module :simulated true}))
           :make-cluster #(ref/cluster %)})
        P1 (UUID/randomUUID)
        C1 {:by :email :email "ambig@example.com"}
        Acc1 [:google "ambig-1"]
        L1 (UUID/randomUUID)
        M1 {:uuid (UUID/randomUUID) :session (UUID/randomUUID)}
        sc {:name "ambiguous-could-not-write"
            :seeds []
            :watched-roots []
            :thread-ops [[[:invite {:person P1 :claim C1 :invited-by :system :at 100}]]
                         [[:enter {:account Acc1 :says {} :email "ambig@example.com"
                                   :code-hash nil :own-lane L1 :making M1 :at 110}]]]
            :query-targets {:who-is [{:root P1 :sign-in (UUID/randomUUID) :session nil}]
                            :ask-of []
                            :under [P1]}}
        r-taken (run-concurrent-batch! (wrap-ambiguous true) sc)
        r-dropped (run-concurrent-batch! (wrap-ambiguous false) sc)]
    {:taken-accepted? (:ok? r-taken)
     :dropped-accepted? (:ok? r-dropped)}))

(defn -main [& _]
  (println "=== STEP 3: Multi-Thread Concurrent Linearizability Runner (2..8 Threads) ===")
  (let [t0 (System/nanoTime)
        ref-target {:api (lin/api-from-ns 'softland.auth.reference)
                    :make-cluster #(ref/cluster %)}
        ;; 1. Verify reference.clj passes under explicit lock + native atom lock (25 rounds × 8 scenarios = 200 multi-thread batches)
        res-locked-ref (check-suite! ref-target {:rounds 15 :serialize-lock? true})
        res-native-ref (check-suite! ref-target {:rounds 25 :serialize-lock? false})
        ;; 2. Verify {:could-not ...} ambiguity handling (taken vs not-taken)
        res-ambig (verify-could-not-ambiguity!)
        ;; 3. Verify all 6 racy copies pass when serialized with a lock, and ALL 6 are caught when racing on 2..8 threads!
        racy-results
        (mapv
         (fn [[mode desc]]
           (let [target {:api racy/racy-api
                         :make-cluster #(racy/make-cluster mode %)}
                 locked-pass (check-suite! target {:rounds 2 :serialize-lock? true})
                 unlocked-race (check-suite! target {:rounds 40 :serialize-lock? false :stop-on-fail? true})]
             {:mode mode
              :description desc
              :passes-when-locked? (:passed? locked-pass)
              :caught-when-racing? (not (:passed? unlocked-race))
              :caught-in-scenario (:scenario unlocked-race)
              :threads (:threads unlocked-race)
              :batches-before-catch (:batches-before-fail unlocked-race)
              :failure-reason (:reason unlocked-race)}))
         racy/racy-copy-modes)
        elapsed-ms (/ (- (System/nanoTime) t0) 1e6)
        caught-count (count (filter :caught-when-racing? racy-results))
        all-ok? (and (:passed? res-locked-ref)
                     (:passed? res-native-ref)
                     (:taken-accepted? res-ambig)
                     (:dropped-accepted? res-ambig)
                     (every? :passes-when-locked? racy-results)
                     (>= caught-count 5)
                     (= caught-count (count racy-results)))]
    (println "\n1. Reference under explicit external lock (15 rounds × 8 scenarios = 120 batches):")
    (println "  " res-locked-ref)
    (println "\n2. Reference (`test-auth/softland/auth/reference.clj` internal lock, 25 rounds × 8 scenarios = 200 batches):")
    (println "  " res-native-ref)
    (println "\n3. `{:could-not ...}` write ambiguity check (both taken and not-taken allowed):")
    (println "  " res-ambig)
    (println (str "\n4. Purposefully racing copies caught (" caught-count "/" (count racy-results) "):"))
    (doseq [r racy-results]
      (println (format "   - %-36s locked-pass=%-5s caught=%-5s scenario=%s (threads=%s, batch=%s) reason=%s"
                       (name (:mode r))
                       (:passes-when-locked? r)
                       (:caught-when-racing? r)
                       (:caught-in-scenario r)
                       (:threads r)
                       (:batches-before-catch r)
                       (pr-str (get-in r [:failure-reason :type])))))
    (println (format "\nCompleted Step 3 verification in %.1f ms. ALL_CHECKS_PASSED=%s" elapsed-ms all-ok?))
    (shutdown-agents)
    (System/exit (if all-ok? 0 1))))
