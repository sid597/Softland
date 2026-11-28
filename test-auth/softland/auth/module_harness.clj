(ns softland.auth.module-harness
  "Cluster launcher and checks for `softland.auth.module/Auth` and
  `softland.auth.client`:
  - `launch-module`: 0-arg factory passed as the 3rd argument to
    `softland.auth.module-check/-main`;
  - `check-says-table!`: reads `$$accounts` directly on an `InProcessCluster`
    to verify Sid's Call 2 (`:says` kept from the enter with the latest `:at`,
    which no contract read exposes);
  - `run-slice-check`: runs Slice-scoped checks including carbon's unchanged
    `auth-concurrent.step3-run` 2..8 thread linearizability scenarios against a
    live `InProcessCluster`."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [softland.auth.client :as client]
            [softland.auth.module :as mod]
            [softland.auth.module-check :as mc]
            [softland.auth.schema :as s])
  (:import [java.util UUID]
           [java.util.concurrent Callable CyclicBarrier Executors TimeUnit]))

(defn launch-module
  "Launches a fresh `Auth` module on a 4-task `InProcessCluster` and returns
  the implementation map expected by `softland.auth.module-check/-main`."
  []
  (let [ipc (rtest/create-ipc)
        mod-name (rama/get-module-name mod/Auth)]
    (rtest/launch-module! ipc mod/Auth {:tasks 4 :threads 2})
    (mc/implementation (mc/calls-of 'softland.auth.client)
                       ipc
                       mod-name
                       :name (str "Rama module " mod-name " via softland.auth.client")
                       :watch-bound-ms 10000
                       :settle-ms 250
                       :close (fn [] (.close ipc)))))

(defn- read-account-row
  "Reads `$$accounts[account-key]` directly from `ipc` for `[provider subject]`."
  [ipc mod-name [provider subject]]
  (let [p-acc (rama/foreign-pstate ipc mod-name "$$accounts")
        ak (s/account-key provider subject)]
    (rama/foreign-select-one [(path/keypath ak)] p-acc)))

(defn check-says-table!
  "Direct PState check on `$$accounts` (no contract read exposes `:says`):
  verifies that a linked account keeps `:says` of the enter with the strictly
  latest `:at` (ignoring equal or earlier `:at`), that refused enters leave
  `$$accounts[Acc] == nil`, and that 4 concurrent returning enters with
  differing `:at`s settle on the highest `:at`'s `:says`."
  []
  (with-open [ipc (rtest/create-ipc)]
    (let [mod-name (rama/get-module-name mod/Auth)
          _ (rtest/launch-module! ipc mod/Auth {:tasks 4 :threads 2})
          c1 (client/open ipc mod-name)
          c2 (client/open ipc mod-name)
          P1 (UUID/randomUUID)
          L1 (UUID/randomUUID)
          M1 {:uuid (UUID/randomUUID) :session (UUID/randomUUID)}
          acc1 [:google "says-user-1"]
          acc-refused [:google "says-refused-user"]
          email1 "says1@example.com"
          ;; 1. Refused enter leaves $$accounts[acc-refused] == nil
          r-refused (client/enter c1 {:account acc-refused :says {"x" "no"} :email "missing@example.com"
                                      :code-hash nil :own-lane L1 :making M1 :at 500})
          row-refused (read-account-row ipc mod-name acc-refused)
          ;; 2. Invite + first enter at t=200
          r-inv (client/invite c1 {:person P1 :claim {:by :email :email email1} :invited-by :system :at 10})
          r-e1 (client/enter c1 {:account acc1 :says {"name" "v200"} :email email1 :code-hash nil
                                 :own-lane L1 :making M1 :at 200})
          row-1 (read-account-row ipc mod-name acc1)
          ;; 3. Returning enter at equal t=200 -> leaves :says untouched!
          _ (client/enter c2 {:account acc1 :says {"name" "v200-tie"} :email nil :code-hash nil
                              :own-lane (UUID/randomUUID) :making M1 :at 200})
          row-tie (read-account-row ipc mod-name acc1)
          ;; 4. Returning enter at earlier t=100 -> leaves :says untouched!
          _ (client/enter c1 {:account acc1 :says {"name" "v100-old"} :email nil :code-hash nil
                              :own-lane (UUID/randomUUID) :making M1 :at 100})
          row-old (read-account-row ipc mod-name acc1)
          ;; 5. Returning enter at later t=350 -> replaces :says!
          _ (client/enter c2 {:account acc1 :says {"name" "v350-new"} :email nil :code-hash nil
                              :own-lane (UUID/randomUUID) :making M1 :at 350})
          row-new (read-account-row ipc mod-name acc1)
          ;; 6. Four concurrent returning enters (t = 400, 700, 500, 600) -> settles on t=700!
          pool (Executors/newFixedThreadPool 4)
          barrier (CyclicBarrier. 4)
          futs (mapv (fn [[t lbl]]
                       (.submit pool ^Callable
                                (fn []
                                  (.await barrier 10 TimeUnit/SECONDS)
                                  (client/enter c1 {:account acc1 :says {"name" lbl} :email nil :code-hash nil
                                                    :own-lane L1 :making M1 :at t}))))
                     [[400 "v400"] [700 "v700-max"] [500 "v500"] [600 "v600"]])
          _ (doseq [f futs] (.get ^java.util.concurrent.Future f 15 TimeUnit/SECONDS))
          _ (.shutdownNow pool)
          row-conc (read-account-row ipc mod-name acc1)]
      (client/close! c1)
      (client/close! c2)
      (let [checks [["Refused enter returns :not-invited and leaves $$accounts[Acc] nil"
                     (and (= {:refused :not-invited} r-refused) (nil? row-refused))
                     {:res r-refused :row row-refused}]
                    ["Invite + first enter at t=200 stores :says={\"name\" \"v200\"} and :says-at=200"
                     (and (= {:invited P1} r-inv)
                          (= P1 (:person r-e1))
                          (= {"name" "v200"} (:says row-1))
                          (= 200 (:says-at row-1)))
                     row-1]
                    ["Equal :at=200 leaves kept :says unchanged (Sid's Call 2)"
                     (and (= {"name" "v200"} (:says row-tie)) (= 200 (:says-at row-tie)))
                     row-tie]
                    ["Earlier :at=100 leaves kept :says unchanged (Sid's Call 2)"
                     (and (= {"name" "v200"} (:says row-old)) (= 200 (:says-at row-old)))
                     row-old]
                    ["Later :at=350 replaces kept :says and :says-at"
                     (and (= {"name" "v350-new"} (:says row-new)) (= 350 (:says-at row-new)))
                     row-new]
                    ["Four concurrent returning enters (400, 700, 500, 600) keep :says of latest :at=700"
                     (and (= {"name" "v700-max"} (:says row-conc)) (= 700 (:says-at row-conc)))
                     row-conc]]]
        (doseq [[label ok? seen] checks]
          (if ok?
            (println "PASSED" label)
            (println "FAILED" label (str "seen=" (pr-str seen)))))
        (every? second checks)))))

(defn- with-asking-hashes
  "Carbon's scenarios were written before an ask took a second hash
  (`:asking-hash`): without one their asks would not fit, and the scenarios
  of an answer would meet no ask. Each ask of a scenario is given one here,
  made from its code, so that carbon's file stays as the audit left it."
  [scenario]
  (let [given (fn [[kind input :as call]]
                (if (and (= :ask kind) (map? input) (not (contains? input :asking-hash)))
                  [kind (assoc input :asking-hash (format "%064x" (bit-and 0x7fffffff (hash (:code input)))))]
                  call))]
    (-> scenario
        (update :seeds (fn [calls] (mapv given calls)))
        (update :thread-ops (fn [threads] (mapv (fn [calls] (mapv given calls)) threads))))))

(defn run-step3-scenarios!
  "Runs carbon's unchanged `auth-concurrent.step3-run/run-concurrent-batch!` over
  the scenarios whose prefix matches `allowed-prefixes` (or all scenarios when
  `allowed-prefixes` is nil), for `rounds` rounds on a live `InProcessCluster`
  (launching and destroying a fresh `Auth` module per batch so fixed emails and
  ask codes in `build-scenarios` start clean every time). When `strip-watches?`
  is true (for slices before Slice d), clears `:watched-roots` on each scenario."
  [{:keys [allowed-prefixes rounds strip-watches?] :or {rounds 3 strip-watches? false}}]
  (let [build-sc (requiring-resolve 'auth-concurrent.step3-run/build-scenarios)
        run-batch! (requiring-resolve 'auth-concurrent.step3-run/run-concurrent-batch!)
        lin-api ((requiring-resolve 'auth-concurrent.linearizability/api-from-ns) 'softland.auth.client)
        mod-name (rama/get-module-name mod/Auth)]
    (with-open [ipc (rtest/create-ipc)]
      (loop [r 0
             total-batches 0]
        (if (= r rounds)
          (do (println (format "PASSED carbon step3-run: %d rounds, %d multi-thread batches (0 failures)."
                               rounds total-batches))
              true)
          (let [scs (cond->> (map with-asking-hashes (build-sc))
                      allowed-prefixes
                      (filter (fn [sc]
                                (some #(.startsWith ^String (:name sc) ^String %) allowed-prefixes)))
                      strip-watches?
                      (map #(assoc % :watched-roots [])))
                res (reduce
                     (fn [cnt sc]
                       (rtest/launch-module! ipc mod/Auth {:tasks 4 :threads 2})
                       (try
                         (let [target {:api (assoc lin-api
                                                   :open (fn [cl _] (client/open cl mod-name)))
                                       :make-cluster (fn [_] ipc)}
                               batch-res (run-batch! target sc)]
                           (if (:ok? batch-res)
                             (do (println (format "  [round %d] PASSED %-58s (%d threads)"
                                                  (inc r) (:name sc) (:threads batch-res)))
                                 (inc cnt))
                             (do (println (format "  [round %d] FAILED %-58s reason=%s"
                                                  (inc r) (:name sc) (pr-str (:reason batch-res))))
                                 (reduced {:failed batch-res}))))
                         (finally
                           (try (rtest/destroy-module! ipc mod-name) (catch Throwable _ nil)))))
                     total-batches
                     scs)]
            (if (map? res)
              false
              (recur (inc r) res))))))))

(defn check-slice-b-root-rules!
  "Verifies Slice b root operations against `softland.auth.reference`: 32-deep
  agent chain + `:too-deep` at 33 + 2-scan `end-actor` cascade, take-out
  cascading expired sign-ins to longer-lived sessions, born-expired sign-ins/
  sessions/`replace-secret`, `:driven-by` preservation across close/repeat,
  2-seek `who-is` (before and after `own-lane-made`), and live-only `under`."
  []
  (let [ref-step (requiring-resolve 'softland.auth.reference/step)
        ref-who  (requiring-resolve 'softland.auth.reference/who-is)
        ref-und  (requiring-resolve 'softland.auth.reference/under)
        ref-empty @(requiring-resolve 'softland.auth.reference/empty-state)
        h (fn [n] (format "%064x" n))
        P (UUID/randomUUID)
        L (UUID/randomUUID)
        M {:uuid (UUID/randomUUID) :session (UUID/randomUUID)}
        As (vec (repeatedly 33 #(UUID/randomUUID)))
        G-early (UUID/randomUUID)
        G-exp (UUID/randomUUID)
        G-live (UUID/randomUUID)
        G-deep (UUID/randomUUID)
        S-short (UUID/randomUUID)
        S-long (UUID/randomUUID)
        S-stay (UUID/randomUUID)
        S-deep (UUID/randomUUID)
        calls (vec
               (concat
                [[:invite {:person P :claim {:by :email :email "sliceb@example.com"} :invited-by :system :at 10}]
                 [:enter {:account [:google "sb-1"] :says {"k" "v"} :email "sliceb@example.com"
                          :code-hash nil :own-lane L :making M :at 20}]
                 ;; Sign-in begun BEFORE own-lane-made (tests who-is fallback path!)
                 [:begin-sign-in {:root P :actor P :sign-in G-early :kind :browser :hash (h 1)
                                  :until 5000 :label "early" :at 25}]
                 [:own-lane-made {:person P :at 30}]
                 [:begin-sign-in {:root P :actor P :sign-in G-exp :kind :browser :hash (h 2)
                                  :until 1000 :label "exp" :at 35}]
                 [:begin-sign-in {:root P :actor P :sign-in G-live :kind :program :hash (h 3)
                                  :until 9000 :label nil :at 40}]
                 [:open-session {:root P :sign-in G-exp :session S-short :until 900 :driven-by {:kind :person} :at 45}]
                 [:open-session {:root P :sign-in G-exp :session S-long :until 5000 :driven-by {:kind :model :model "opus"} :at 46}]
                 [:open-session {:root P :sign-in G-live :session S-stay :until 2000 :at 47}]
                 ;; Extend S-stay without overwriting driven-by (nil)
                 [:open-session {:root P :sign-in G-live :session S-stay :until 3000 :driven-by {:kind :person} :at 48}]
                 ;; Trigger take-out at t=1000 (ends G-exp, cascades S-long despite until=5000, closes S-short)
                 [:replace-secret {:root P :sign-in G-live :old-hash (h 3) :new-hash (h 4)
                                   :until 9500 :previous-until 8000 :at 1000}]
                 ;; Repeat of replace-secret
                 [:replace-secret {:root P :sign-in G-live :old-hash (h 3) :new-hash (h 4)
                                   :until 9999 :previous-until 9999 :at 1001}]
                 ;; Repeat of begin-sign-in after replace-secret (checks immutable :initial-hash)
                 [:begin-sign-in {:root P :actor P :sign-in G-live :kind :program :hash (h 3)
                                  :until 1234 :label "ignored" :at 1002}]]
                ;; Build 32-deep agent chain A0..A31 (depths 1..32), then A32 refused :too-deep
                (for [i (range 33)]
                  [:make-actor {:root P :actor (nth As i) :made-by (if (zero? i) P (nth As (dec i)))
                                :label (str "ag-" i) :at (+ 1100 i)}])
                [[:begin-sign-in {:root P :actor (nth As 31) :sign-in G-deep :kind :program :hash (h 5)
                                  :until 9000 :label "deep" :at 1200}]
                 [:open-session {:root P :sign-in G-deep :session S-deep :until 8000 :driven-by {:kind :model :model "m"} :at 1201}]
                 ;; End top agent A0 -> 2-scan cascades all 32 agents, G-deep, S-deep
                 [:end-actor {:root P :actor (nth As 0) :at 1300}]
                 ;; Repeat end-actor on A31 (already ended by cascade)
                 [:end-actor {:root P :actor (nth As 31) :at 1301}]]))]
    (with-open [ipc (rtest/create-ipc)]
      (let [mod-name (rama/get-module-name mod/Auth)
            _ (rtest/launch-module! ipc mod/Auth {:tasks 4 :threads 2})
            c (client/open ipc mod-name)
            dispatch {:invite client/invite :enter client/enter :own-lane-made client/own-lane-made
                      :begin-sign-in client/begin-sign-in :replace-secret client/replace-secret
                      :end-sign-in client/end-sign-in :make-actor client/make-actor
                      :end-actor client/end-actor :open-session client/open-session
                      :close-session client/close-session}
            final-ref
            (reduce
             (fn [st [k arg :as call]]
               (let [actual ((get dispatch k) c arg)
                     [st' expected] (ref-step st call)]
                 (when (not= actual expected)
                   (throw (ex-info "Slice b mismatch vs reference"
                                   {:call call :actual actual :expected expected})))
                 st'))
             ref-empty
             calls)
            reads-ok?
            (and (= (client/under c P) (ref-und final-ref P))
                 (every? (fn [q]
                           (= (client/who-is c q) (ref-who final-ref q)))
                         [{:root P :sign-in G-early :session nil}
                          {:root P :sign-in G-exp :session S-long}
                          {:root P :sign-in G-live :session S-stay}
                          {:root P :sign-in G-deep :session S-deep}
                          {:root P :sign-in (UUID/randomUUID) :session nil}
                          {:root (UUID/randomUUID) :sign-in G-live :session nil}]))]
        (client/close! c)
        (if reads-ok?
          (do (println "PASSED Slice b root rules (32-deep chain, :too-deep, 2-scan end-actor, take-out cascade, :driven-by, 2-seek who-is, live-only under) match reference 100%.")
              true)
          (do (println "FAILED Slice b read comparison against reference")
              false))))))

(defn check-watch-not-reached!
  "Verifies `watch-root` lifecycle guarantees that `module_check.clj` cannot
  trigger on a healthy module:
  1. Destroying the `Auth` module delivers `:softland.auth.client/not-reached`
     exactly once within seconds to every active watcher;
  2. A watcher whose callback throws still receives later updates and does not
     disrupt a sibling watcher on the same root;
  3. Calling `stop` or `close!` re-entrantly from inside a watch callback does
     not deadlock and stops further callbacks."
  []
  (with-open [ipc (rtest/create-ipc)]
    (let [mod-name (rama/get-module-name mod/Auth)
          _ (rtest/launch-module! ipc mod/Auth {:tasks 4 :threads 2})
          c1 (client/open ipc mod-name)
          c2 (client/open ipc mod-name)
          P (UUID/randomUUID)
          L (UUID/randomUUID)
          M {:uuid (UUID/randomUUID) :session (UUID/randomUUID)}
          G1 (UUID/randomUUID)
          G2 (UUID/randomUUID)
          S1 (UUID/randomUUID)
          H1 (format "%064x" 101)
          H2 (format "%064x" 102)
          _ (client/invite c1 {:person P :claim {:by :email :email "watch@example.com"} :invited-by :system :at 10})
          _ (client/enter c1 {:account [:google "watch-1"] :says {} :email "watch@example.com"
                              :code-hash nil :own-lane L :making M :at 20})
          _ (client/own-lane-made c1 {:person P :at 25})
          ;; 1. Throwing callback + sibling watcher + re-entrant stop watcher
          throw-calls (atom 0)
          sib-vals (atom [])
          reent-calls (atom 0)
          reent-stop (atom nil)
          stop-throw (client/watch-root c1 P (fn [_]
                                               (swap! throw-calls inc)
                                               (throw (RuntimeException. "boom"))))
          stop-sib (client/watch-root c1 P (fn [v] (swap! sib-vals conj v)))
          _ (reset! reent-stop
                    (client/watch-root c1 P (fn [_]
                                              (swap! reent-calls inc)
                                              (when-let [s @reent-stop] (s)))))
          _ (client/begin-sign-in c2 {:root P :actor P :sign-in G1 :kind :browser :hash H1
                                      :until 5000 :label "w1" :at 30})
          _ (client/open-session c2 {:root P :sign-in G1 :session S1 :until 4000
                                     :driven-by {:kind :person} :at 35})
          ;; Wait briefly for async proxy pushes
          _ (let [deadline (+ (System/currentTimeMillis) 3000)]
              (while (and (< (System/currentTimeMillis) deadline)
                          (or (empty? @sib-vals)
                              (nil? (get-in (last @sib-vals) [:sessions S1]))))
                (Thread/sleep 20)))
          last-sib (last @sib-vals)
          driven-by-omitted? (and (some? (get-in last-sib [:sessions S1]))
                                  (not (contains? (get-in last-sib [:sessions S1]) :driven-by)))
          _ (stop-throw)
          _ (stop-sib)
          ;; 2. Module loss -> exact-once :softland.auth.client/not-reached within seconds!
          nr-vals (atom [])
          nr-latch (java.util.concurrent.CountDownLatch. 1)
          _ (client/watch-root c2 P (fn [v]
                                      (swap! nr-vals conj v)
                                      (when (= v :softland.auth.client/not-reached)
                                        (.countDown nr-latch))))
          ;; Confirm a live change arrives before destroy
          _ (client/begin-sign-in c1 {:root P :actor P :sign-in G2 :kind :program :hash H2
                                      :until 9000 :label "w2" :at 40})
          _ (Thread/sleep 150)
          t-destroy (System/nanoTime)
          _ (rtest/destroy-module! ipc mod-name)
          got-nr? (.await nr-latch 10 TimeUnit/SECONDS)
          nr-ms (/ (- (System/nanoTime) t-destroy) 1e6)
          _ (Thread/sleep 500)
          nr-count (count (filter #(= % :softland.auth.client/not-reached) @nr-vals))
          ;; Re-entrant close! check on fresh client (no deadlock)
          _ (client/close! c1)
          _ (client/close! c2)]
      (let [checks [["Throwing callback still gets later calls and sibling watcher receives live updates without :driven-by"
                     (and (>= @throw-calls 2) driven-by-omitted?)
                     {:throw-calls @throw-calls :last-sib last-sib}]
                    ["Re-entrant stop inside callback does not deadlock and stops after 1 call"
                     (= 1 @reent-calls)
                     {:reent-calls @reent-calls}]
                    [(format "destroy-module! delivers :softland.auth.client/not-reached exactly once within seconds (%.1f ms)" nr-ms)
                     (and got-nr? (= 1 nr-count) (< nr-ms 10000.0))
                     {:got-nr? got-nr? :nr-count nr-count :nr-ms nr-ms :nr-vals @nr-vals}]]]
        (doseq [[label ok? seen] checks]
          (if ok?
            (println "PASSED" label)
            (println "FAILED" label (str "seen=" (pr-str seen)))))
        (every? second checks)))))

(defn- wait-until
  "Calls `f` every 20 ms until it gives a truthy value or `ms` pass. Gives
  the value, or nil."
  [f ms]
  (let [deadline (+ (System/currentTimeMillis) ms)]
    (loop []
      (or (f)
          (when (< (System/currentTimeMillis) deadline)
            (Thread/sleep 20)
            (recur))))))

(defn- seed-person!
  "Invites, enters and begins one browser sign-in of a person `P` with
  sign-in `G`, at small times, through client `c`."
  [c P G tag]
  (client/invite c {:person P :claim {:by :email :email (str tag "@example.com")} :invited-by :system :at 10})
  (client/enter c {:account [:google tag] :says {} :email (str tag "@example.com") :code-hash nil
                   :own-lane (UUID/randomUUID) :making {:uuid (UUID/randomUUID) :session (UUID/randomUUID)} :at 20})
  (client/own-lane-made c {:person P :at 25})
  (client/begin-sign-in c {:root P :actor P :sign-in G :kind :browser :hash (format "%064x" 777)
                           :until 999999999 :label nil :at 30}))

(defn- told-of-new-session?
  "Watches root `P` on client `c`, opens a new session under its sign-in
  `G`, and gives {:told? :not-reached}: whether the watch was told of the
  session within 8 s, and how many times it was told `:not-reached`."
  [c P G]
  (let [seen (atom [])
        lost (atom 0)
        stop (client/watch-root c P (fn [v]
                                      (if (= v :softland.auth.client/not-reached)
                                        (swap! lost inc)
                                        (swap! seen conj v))))
        S (UUID/randomUUID)
        _ (client/open-session c {:root P :sign-in G :session S :until 999999999 :at 100})
        told? (boolean (wait-until (fn [] (some #(contains? (:sessions %) S) @seen)) 8000))]
    (when (fn? stop) (stop))
    {:told? told? :not-reached @lost}))

(defn check-late-open!
  "Verifies that a watch whose opening outlasts `watch-root`'s wait leaves no
  proxy behind. With the wait forced to 0 ms, three rounds of 7,000 watches
  of roots that nobody writes are opened on one task, 21,000 in all, more
  than `pstate.proxy.limit` (20,000); each round's callers are told
  `:not-reached` (or got a watch, when the open beat the wait) and stop
  their watches. Then, with the wait back, a normal watch of a root is told
  of a new session and never `:not-reached`. Under the client before this
  repair, each late proxy stayed open, and the review of 22 November saw
  the normal watch told `:not-reached` with Rama logging that the proxies
  of the task exceeded the limit."
  []
  (with-open [ipc (rtest/create-ipc)]
    (let [mod-name (rama/get-module-name mod/Auth)
          _ (rtest/launch-module! ipc mod/Auth {:tasks 1 :threads 1})
          c (client/open ipc mod-name)
          P (UUID/randomUUID)
          G (UUID/randomUUID)
          _ (seed-person! c P G "late-open")
          before (told-of-new-session? c P G)
          rounds 3
          per-round 7000
          told (java.util.concurrent.atomic.AtomicLong. 0)
          pool (Executors/newFixedThreadPool 16)
          watched (atom 0)
          _ (try
              (dotimes [_ rounds]
                (let [stops (with-redefs [softland.auth.client/proxy-open-timeout-ms 0]
                              (mapv (fn [^java.util.concurrent.Future f] (.get f 60 TimeUnit/SECONDS))
                                    (mapv (fn [_]
                                            (.submit pool ^Callable
                                                     (fn []
                                                       (client/watch-root c (UUID/randomUUID)
                                                                          (fn [v]
                                                                            (when (= v :softland.auth.client/not-reached)
                                                                              (.incrementAndGet told)))))))
                                          (range per-round))))]
                  (swap! watched + (count (filter fn? stops)))
                  (doseq [stop stops] (when (fn? stop) (stop)))
                  ;; The late opens of this round finish and are closed.
                  (Thread/sleep 3000)))
              (finally (.shutdownNow pool)))
          after (told-of-new-session? c P G)
          _ (client/close! c)
          checks [["Before: a normal watch of a root is told of a new session and never :not-reached."
                   (and (:told? before) (zero? (:not-reached before)))
                   before]
                  [(str "With watch-root's wait forced to 0 ms, 21,000 watches on one task (more than pstate.proxy.limit, 20,000), each told :not-reached or given a watch, are stopped by their callers.")
                   (= (* rounds per-round) @watched)
                   {:stop-fns @watched :told-not-reached (.get told)}]
                  ["After them, with the wait back: a normal watch of a root is still told of a new session and never :not-reached, so the proxies that opened after their wait were closed and none is left counting against the limit."
                   (and (:told? after) (zero? (:not-reached after)))
                   after]]]
      (doseq [[label ok? seen] checks]
        (if ok?
          (println "PASSED" label)
          (println "FAILED" label (str "seen=" (pr-str seen)))))
      (every? second checks))))

(defn check-partial-outage!
  "Verifies that in a partial outage `:not-reached` reaches every watcher of
  an unreached root within one probe deadline, however many roots are
  watched. Forty roots are watched on a 4-task module; then the client's
  calls are stubbed so that the ping of tasks 0 and 1 fails and the probe
  of 20 of the roots (`probe-root`) never answers, and one liveness sweep
  is run (the client's
  own timer is stopped first, so that it runs once). It must end within
  6 s (the probe deadline is 3 s; under the client before this repair the
  20 probes ran one after another, up to 60 s), tell each watcher of an
  unreached root `:not-reached` once, and tell no other."
  []
  (with-open [ipc (rtest/create-ipc)]
    (let [mod-name (rama/get-module-name mod/Auth)
          _ (rtest/launch-module! ipc mod/Auth {:tasks 4 :threads 2})
          c (client/open ipc mod-name)
          roots (vec (repeatedly 40 #(UUID/randomUUID)))
          dead (set (take 20 roots))
          lost (atom {})
          stops (mapv (fn [r]
                        (client/watch-root c r (fn [v]
                                                 (when (= v :softland.auth.client/not-reached)
                                                   (swap! lost update r (fnil inc 0))))))
                      roots)
          ;; Stop the client's own timer: the sweep below is the one that runs.
          _ (when-let [^java.util.concurrent.Future f @(.-monitor-task ^softland.auth.client.Client c)]
              (.cancel f false))
          orig-query rama/foreign-invoke-query-async
          sweep-ms (with-redefs [rama/foreign-invoke-query-async
                                 (fn [q & args]
                                   (if (and (= 1 (count args)) (contains? #{0 1} (first args)))
                                     (java.util.concurrent.CompletableFuture/failedFuture (RuntimeException. "task down"))
                                     (apply orig-query q args)))
                                 softland.auth.client/probe-root
                                 (fn [_client root]
                                   (if (contains? dead root)
                                     (java.util.concurrent.CompletableFuture.)
                                     (java.util.concurrent.CompletableFuture/completedFuture [])))]
                     (let [t0 (System/nanoTime)]
                       (#'softland.auth.client/run-liveness-sweep! c)
                       (/ (- (System/nanoTime) t0) 1e6)))
          _ (wait-until (fn [] (= dead (set (keys @lost)))) 2000)
          told @lost
          _ (doseq [s stops] (when (fn? s) (s)))
          _ (client/close! c)
          checks [[(format "A partial outage (tasks 0 and 1 unreached, 20 of 40 watched roots unanswered) is swept in %.1f ms: within 6 s, one probe deadline and a margin, however many roots are watched." sweep-ms)
                   (< sweep-ms 6000.0)
                   {:sweep-ms sweep-ms}]
                  ["Each watcher of an unreached root is told :not-reached once, and the watchers of the roots that answered are told nothing."
                   (and (= dead (set (keys told))) (every? #(= 1 %) (vals told)))
                   {:told (count told) :counts (frequencies (vals told))}]]]
      (doseq [[label ok? seen] checks]
        (if ok?
          (println "PASSED" label)
          (println "FAILED" label (str "seen=" (pr-str seen)))))
      (every? second checks))))

(defn check-unfit-records!
  "Verifies that a depot record that does not have the client's form is
  refused in the topology and never holds its partition. Records are
  appended straight to Auth's depots (`foreign-depot`, as an appender other
  than the client would): on each of the four depots a record of Auth's own
  type with a field of another type, and on `*root-events` a map that is no
  record of Auth's and a thing that is no map. Each must be acked with
  `{:could-not :read-the-event}` within 10 s; and right after each, a write
  through the client that lands on the same partition (the same root,
  claim, account or code) must be taken. Under the module before this
  repair, such a record throws in the topology, is tried again for ever
  under `:all-after`, and the write behind it on its partition is not
  taken."
  []
  (with-open [ipc (rtest/create-ipc)]
    (let [mod-name (rama/get-module-name mod/Auth)
          _ (rtest/launch-module! ipc mod/Auth {:tasks 4 :threads 2})
          c (client/open ipc mod-name)
          appended (fn [depot-name rec]
                     (try
                       (get (.get ^java.util.concurrent.CompletableFuture
                                  (rama/foreign-append-async! (rama/foreign-depot ipc mod-name depot-name) rec :ack)
                                  10 TimeUnit/SECONDS)
                            "auth")
                       (catch Throwable t {:threw (.getName (class t))})))
          unfit {:could-not :read-the-event}
          P (UUID/randomUUID)
          G (UUID/randomUUID)
          _ (seed-person! c P G "unfit-1")
          open! (fn [] (client/open-session c {:root P :sign-in G :session (UUID/randomUUID)
                                               :until 999999999 :at 50}))
          ;; *root-events: a begin whose :until is a text.
          r1 (appended "*root-events" (s/->BeginSignInEvent P P (UUID/randomUUID) :program (format "%064x" 5)
                                                            "later" nil 40))
          w1 (open!)
          ;; *root-events: a map of no type of Auth's, on P's partition.
          r2 (appended "*root-events" {:root-id P :what "a map"})
          w2 (open!)
          ;; *root-events: a thing that is no map.
          r3 (appended "*root-events" "a text")
          w3 (open!)
          ;; *invites: an invite whose :at is a text, on the claim's partition.
          P2 (UUID/randomUUID)
          r4 (appended "*invites" (s/->InviteEvent P2 "e:unfit-2@example.com" :email "unfit-2@example.com"
                                                   :system nil "now"))
          w4 (client/invite c {:person P2 :claim {:by :email :email "unfit-2@example.com"}
                               :invited-by :system :at 60})
          ;; *enters: an enter whose :says is a vector, on the account's partition.
          r5 (appended "*enters" (s/->EnterEvent (UUID/randomUUID) (s/account-key :google "unfit-2") :google "unfit-2"
                                                 ["not" "a" "map"] "unfit-2@example.com" nil (UUID/randomUUID)
                                                 (UUID/randomUUID) (UUID/randomUUID) 70))
          w5 (client/enter c {:account [:google "unfit-2"] :says {} :email "unfit-2@example.com" :code-hash nil
                              :own-lane (UUID/randomUUID)
                              :making {:uuid (UUID/randomUUID) :session (UUID/randomUUID)} :at 80})
          ;; *asks: an ask whose :ends is nil, on the code's partition.
          r6 (appended "*asks" (s/->AskEvent "UNFIT001" (format "%064x" 6) (format "%064x" 7) "a program" nil 90))
          w6 (client/ask c {:code "UNFIT001" :hash (format "%064x" 6) :asking-hash (format "%064x" 7)
                            :asks "a program" :ends 999999 :at 95})
          _ (client/close! c)
          checks [["A record on *root-events of Auth's own type with a field of another type, a map of no type of Auth's, and a thing that is no map are each refused with {:could-not :read-the-event}; and after each, a write of the same root on the same partition is taken."
                   (and (= unfit r1 r2 r3) (every? #(contains? % :open) [w1 w2 w3]))
                   {:refused [r1 r2 r3] :after [w1 w2 w3]}]
                  ["A record on *invites, on *enters and on *asks with a field of another type is refused with {:could-not :read-the-event}; and after each, a write of the same claim, account or code is taken."
                   (and (= unfit r4 r5 r6)
                        (= {:invited P2} w4)
                        (= P2 (:person w5))
                        (= {:asked "UNFIT001"} w6))
                   {:refused [r4 r5 r6] :after [w4 w5 w6]}]]]
      (doseq [[label ok? seen] checks]
        (if ok?
          (println "PASSED" label)
          (println "FAILED" label (str "seen=" (pr-str seen)))))
      (every? second checks))))

(defn check-ends-with!
  "Needs no cluster: no write points an agent at another actor than its
  maker, so the records in which the two differ are made by hand and given
  to the plan of `end-actor` itself."
  []
  (let [P (UUID/randomUUID)
        [A1 A2 A3 A4] (repeatedly 4 #(UUID/randomUUID))
        ;; who made it, whose ending ends it
        agents [[A1 P P] [A2 A1 P] [A3 P A1] [A4 A2 A3]]
        ao-map (into {} (map-indexed (fn [i [A made-by ends-with]]
                                       [(s/k-ao P i) (s/->ActorOrderRec A made-by ends-with 1 "an agent" false i)]))
                     agents)
        plan (s/plan-end-actor-2scan P (s/->ActorRec A1 P P 1 "an agent" false 0) ao-map {})
        ended (into {} (keep (fn [[k v]] (when (instance? softland.auth.schema.ActorRec v)
                                           [(:actor v) [(:made-by v) (:ends-with v) (:ended? v)]])))
                    (s/plan-rd-upserts plan))
        checks [["The plan of end-actor follows whose ending ends an agent, in any depth, and reads who made it for nothing: of agent 2, which agent 1 made and which ends with the person, agent 3, which the person made and which ends with agent 1, and agent 4, which agent 2 made and which ends with agent 3, ending agent 1 ends agents 3 and 4 and leaves agent 2; and each record that it writes keeps who made the agent and whose ending ends it."
                 (= {A1 [P P true] A3 [P A1 true] A4 [A2 A3 true]} ended)
                 ended]]]
    (doseq [[label ok? seen] checks]
      (if ok?
        (println "PASSED" label)
        (println "FAILED" label (str "seen=" (pr-str seen)))))
    (every? second checks)))

(defn -main
  "Entry point for slice checks:
    `says`      — run direct `$$accounts` `:says` check;
    `not-reached` — run `watch-root` callback isolation, re-entrancy & `:not-reached` check;
    `slice-a`   — run `:says` check + carbon step3 scenarios 1a, 1b, 2a, 2b, 3;
    `slice-b`   — run Slice b root rules vs reference + carbon step3 scenarios 1a..3, 5, 6;
    `slice-c`   — run `:says` check + carbon step3 all 8 scenarios (without watches);
    `step3`     — run full carbon step3 suite (all 9 scenarios with live watches, 2..8 threads) + `:says` + `:not-reached`;
    `late-open` — a watch whose opening outlasts its wait leaves no proxy behind;
    `partial-outage` — a partial outage is swept within one probe deadline;
    `unfit-record` — a depot record of another form is refused and holds no partition;
    `ends-with` — the plan of `end-actor` follows whose ending ends an agent (no cluster)."
  [& [mode rounds-str]]
  (let [rounds (or (some-> rounds-str parse-long) 2)
        ok? (case mode
              "says" (check-says-table!)
              "not-reached" (check-watch-not-reached!)
              "slice-a" (and (check-says-table!)
                             (run-step3-scenarios! {:allowed-prefixes ["1a." "1b." "2a." "2b." "3."]
                                                    :rounds rounds}))
              "slice-b" (and (check-slice-b-root-rules!)
                             (run-step3-scenarios! {:allowed-prefixes ["1a." "1b." "2a." "2b." "3." "5." "6."]
                                                    :rounds rounds
                                                    :strip-watches? true}))
              "slice-c" (and (check-says-table!)
                             (run-step3-scenarios! {:allowed-prefixes nil
                                                    :rounds rounds
                                                    :strip-watches? true}))
              "late-open" (check-late-open!)
              "partial-outage" (check-partial-outage!)
              "unfit-record" (check-unfit-records!)
              "ends-with" (check-ends-with!)
              "step3" (and (check-says-table!)
                           (check-watch-not-reached!)
                           (run-step3-scenarios! {:allowed-prefixes nil
                                                  :rounds rounds
                                                  :strip-watches? false}))
              (do (println "Unknown mode:" mode) false))]
    (flush)
    (shutdown-agents)
    (System/exit (if ok? 0 1))))
