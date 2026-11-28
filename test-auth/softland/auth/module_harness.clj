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
    live `InProcessCluster`, each call brought to the contract (`adapted`);
  - `check-invitations!` and `check-reruns!`: two writes of invitations and
    accounts at one moment on a cluster, and the steps of a record that runs
    again, called by hand. `-main` lists every mode."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [softland.auth.client :as client]
            [softland.auth.module :as mod]
            [softland.auth.module-check :as mc]
            [softland.auth.schema :as s])
  (:import [java.util UUID]
           [java.util.concurrent Callable CyclicBarrier Executors TimeUnit]))

(defn email-hash
  "The hash of an email as the runtime makes it, by the spec's suggestion:
  SHA-256, as 64 lower-case hex characters, of `softland/email/` and the
  address in lower case."
  [^String address]
  (let [d (java.security.MessageDigest/getInstance "SHA-256")
        bs (.digest d (.getBytes (str "softland/email/" (.toLowerCase address java.util.Locale/ROOT)) "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) bs))))

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
          r-refused (client/enter c1 {:account acc-refused :says {"x" "no"} :email-hash (email-hash "missing@example.com")
                                      :code-hash nil :own-lane L1 :making M1 :at 500})
          row-refused (read-account-row ipc mod-name acc-refused)
          ;; 2. Invite + first enter at t=200
          r-inv (client/invite c1 {:person P1 :claim {:by :email :hash (email-hash email1)} :invited-by :system :at 10 :ends 1000000 :op-id (UUID/randomUUID)})
          r-e1 (client/enter c1 {:account acc1 :says {"name" "v200"} :email-hash (email-hash email1) :code-hash nil
                                 :own-lane L1 :making M1 :at 200})
          row-1 (read-account-row ipc mod-name acc1)
          ;; 3. Returning enter at equal t=200 -> leaves :says untouched!
          _ (client/enter c2 {:account acc1 :says {"name" "v200-tie"} :email-hash nil :code-hash nil
                              :own-lane (UUID/randomUUID) :making M1 :at 200})
          row-tie (read-account-row ipc mod-name acc1)
          ;; 4. Returning enter at earlier t=100 -> leaves :says untouched!
          _ (client/enter c1 {:account acc1 :says {"name" "v100-old"} :email-hash nil :code-hash nil
                              :own-lane (UUID/randomUUID) :making M1 :at 100})
          row-old (read-account-row ipc mod-name acc1)
          ;; 5. Returning enter at later t=350 -> replaces :says!
          _ (client/enter c2 {:account acc1 :says {"name" "v350-new"} :email-hash nil :code-hash nil
                              :own-lane (UUID/randomUUID) :making M1 :at 350})
          row-new (read-account-row ipc mod-name acc1)
          ;; 6. Four concurrent returning enters (t = 400, 700, 500, 600) -> settles on t=700!
          pool (Executors/newFixedThreadPool 4)
          barrier (CyclicBarrier. 4)
          futs (mapv (fn [[t lbl]]
                       (.submit pool ^Callable
                                (fn []
                                  (.await barrier 10 TimeUnit/SECONDS)
                                  (client/enter c1 {:account acc1 :says {"name" lbl} :email-hash nil :code-hash nil
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

(defn- adapted
  "Carbon's scenarios were written to the contract before an ask took a
  second hash and before Auth was given an email's hash in place of the
  address (28 November): as written their asks, invites and enters would
  not fit. Each call of a scenario is brought to the contract here, so that
  carbon's file stays as the audit left it: an ask is given an asking hash
  made from its code; an invite's email claim becomes the hash of the
  address, and the invite an end far off and an op made from the call; an
  enter's `:email` becomes `:email-hash`. Carbon's checker holds the
  results against the reference, which takes the rules of 28 November."
  [scenario]
  (let [claim (fn [c] (if (= :email (:by c)) {:by :email :hash (email-hash (:email c))} c))
        given (fn [[kind input :as call]]
                (if-not (map? input)
                  call
                  (case kind
                    :ask (if (contains? input :asking-hash)
                           call
                           [kind (assoc input :asking-hash (format "%064x" (bit-and 0x7fffffff (hash (:code input)))))])
                    :invite [kind (-> input
                                      (update :claim claim)
                                      (assoc :ends 1000000000)
                                      (assoc :op-id (UUID/nameUUIDFromBytes (.getBytes (pr-str call) "UTF-8"))))]
                    :enter [kind (-> input
                                     (dissoc :email)
                                     (assoc :email-hash (some-> (:email input) email-hash)))]
                    call)))]
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
          (let [scs (cond->> (map adapted (build-sc))
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
  agent chain + `:too-deep` at 33 + 2-scan `end-actor` cascade, the thinning
  of what is watched at a write whose `:at` a sign-in's and a session's time
  have passed, which ends neither, `:driven-by` preservation across
  close/repeat, `who-is` (before and after `own-lane-made`), and `under` of
  the watched sign-ins."
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
                [[:invite {:person P :claim {:by :email :hash (email-hash "sliceb@example.com")} :invited-by :system :at 10
                           :ends 1000000 :op-id (UUID/randomUUID)}]
                 [:enter {:account [:google "sb-1"] :says {"k" "v"} :email-hash (email-hash "sliceb@example.com")
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
                 ;; Thins at t=1000: G-exp and S-short leave what is watched and are not
                 ;; ended; S-long stays by its own until=5000, though its sign-in left.
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
          (do (println "PASSED Slice b root rules (32-deep chain, :too-deep, 2-scan end-actor, the thinning of what is watched, :driven-by, who-is, under of the watched sign-ins) match reference 100%.")
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
          _ (client/invite c1 {:person P :claim {:by :email :hash (email-hash "watch@example.com")} :invited-by :system
                               :at 10 :ends 1000000 :op-id (UUID/randomUUID)})
          _ (client/enter c1 {:account [:google "watch-1"] :says {} :email-hash (email-hash "watch@example.com")
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
  (client/invite c {:person P :claim {:by :email :hash (email-hash (str tag "@example.com"))} :invited-by :system
                    :at 10 :ends 1000000 :op-id (UUID/randomUUID)})
  (client/enter c {:account [:google tag] :says {} :email-hash (email-hash (str tag "@example.com")) :code-hash nil
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
          r4 (appended "*invites" (s/->InviteEvent (UUID/randomUUID) P2 (str "e:" (email-hash "unfit-2@example.com"))
                                                   :system nil 1000000 "now"))
          w4 (client/invite c {:person P2 :claim {:by :email :hash (email-hash "unfit-2@example.com")}
                               :invited-by :system :at 60 :ends 1000000 :op-id (UUID/randomUUID)})
          ;; *enters: an enter whose :says is a vector, on the account's partition.
          r5 (appended "*enters" (s/->EnterEvent (UUID/randomUUID) (s/account-key :google "unfit-2") :google "unfit-2"
                                                 ["not" "a" "map"] (email-hash "unfit-2@example.com") nil (UUID/randomUUID)
                                                 (UUID/randomUUID) (UUID/randomUUID) 70))
          w5 (client/enter c {:account [:google "unfit-2"] :says {} :email-hash (email-hash "unfit-2@example.com") :code-hash nil
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

;; ------------------------------------- invitations, accounts, on a cluster

(defn- at-once
  "Runs the functions `fs` on threads of their own, let go at one moment.
  Gives what each gave, in order."
  [fs]
  (let [pool (Executors/newFixedThreadPool (count fs))
        barrier (CyclicBarrier. (count fs))
        futs (mapv (fn [f] (.submit pool ^Callable (fn [] (.await barrier 10 TimeUnit/SECONDS) (f)))) fs)]
    (try
      (mapv (fn [^java.util.concurrent.Future fut] (.get fut 30 TimeUnit/SECONDS)) futs)
      (finally (.shutdownNow pool)))))

(def ^:private reading-keys
  "The keys by which `every-record` reaches the partitions of a table.
  That they meet every partition is checked, by `partitions-met`, where
  it is leaned on."
  (mapv #(UUID. 7 %) (range 64)))

(defn- every-record
  "Every key and value of the PState `pstate-name` on each partition that
  one of `reading-keys` meets: the union of what each holds, read with
  `{:pkey ...}`."
  [ipc mod-name pstate-name]
  (let [p (rama/foreign-pstate ipc mod-name pstate-name)]
    (into {} (mapcat (fn [k] (rama/foreign-select [path/ALL] p {:pkey k}))) reading-keys)))

(defn- depot-records
  "Every record of the depot `depot-name`, each with the index of its
  partition: `[[i record] ...]`."
  [ipc mod-name depot-name]
  (let [d (rama/foreign-depot ipc mod-name depot-name)
        n (or (:num-partitions (rama/foreign-object-info d))
              (count (take-while #(try (rama/foreign-depot-partition-info d %) true (catch Throwable _ false)) (range 64))))]
    (vec (for [i (range n)
               :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info d i)]
               from (range start-offset end-offset 100)
               r (rama/foreign-depot-read d i from (min end-offset (+ from 100)))]
           [i r]))))

(defn- partitions-met
  "Appends, for each of `reading-keys`, an `own-lane-made` of a person who
  is none, which Auth refuses and of which it keeps nothing but the record;
  `*root-events` is partitioned by that key, as a table is, so where each
  record lands is the partition that its key meets. Gives the set of
  partitions that the keys met and how many partitions there are."
  [ipc mod-name c]
  (doseq [k reading-keys] (client/own-lane-made c {:person k :at 1}))
  (let [records (depot-records ipc mod-name "*root-events")
        keyed (set reading-keys)
        d (rama/foreign-depot ipc mod-name "*root-events")]
    {:met (into #{} (keep (fn [[i r]] (when (contains? keyed (:root-id r)) i))) records)
     :partitions (or (:num-partitions (rama/foreign-object-info d)) (inc (reduce max 0 (map first records))))}))

(defn check-invitations!
  "On a cluster, the scenarios of invitations and accounts in which two
  writes meet at one moment, each `rounds` times with ids of its own: two
  invitations of one claim for two persons; two accounts that prove one
  claim; an enter and a take-back of its claim; an enter and an unlink of
  the same account; an unlink and a new invite of the claim that it frees;
  and enters at and before an invitation's end. Each allows the outcomes
  that some order of the two writes gives, read through the contract and
  from `$$accounts`, and no other. Then, with the keys of the tables' reads
  checked to meet every partition, that after an invitation neither the
  records of `*invites` nor the tables hold her address; and the three
  cases of Sid's yes, at Auth's edge: two accounts enter as one person;
  after unlinking each
  account that `accounts-of` gives and taking back the invitation that is
  named, no key and no value of Auth's tables holds her hash, her subject
  or her `:says`, her own record stays, and her address can be invited as
  another person; an invitation that is over or taken back lets nobody in."
  [rounds]
  (with-open [ipc (rtest/create-ipc)]
    (let [mod-name (rama/get-module-name mod/Auth)
          _ (rtest/launch-module! ipc mod/Auth {:tasks 4 :threads 2})
          c1 (client/open ipc mod-name)
          c2 (client/open ipc mod-name)
          p-acc (rama/foreign-pstate ipc mod-name "$$accounts")
          acc-row (fn [account] (rama/foreign-select-one [(path/keypath (apply s/account-key account))] p-acc))
          n (atom 0)
          fresh (fn [] (swap! n inc))
          mk (fn [] {:uuid (UUID/randomUUID) :session (UUID/randomUUID)})
          inv (fn [cl P C at ends] (client/invite cl {:person P :claim C :invited-by :system :at at :ends ends :op-id (UUID/randomUUID)}))
          ent (fn ent
                ([cl account C at] (ent cl account C at {"name" "Asha"}))
                ([cl account C at says] (client/enter cl {:account account :says says :email-hash (:hash C)
                                                          :code-hash nil :own-lane (UUID/randomUUID) :making (mk) :at at})))
          ;; What a provider says holds her address, as Google's does.
          her-says {"name" "Asha Erasable" "email" "asha@example.org"}
          scenarios
          [["Two invitations of one claim for two persons at one moment: one is taken and the other is :claim-in-use, and the claim names the one that was taken."
            (fn []
              (let [k (fresh) C {:by :email :hash (email-hash (str "two-inv-" k "@x.org"))}
                    P (UUID/randomUUID) Q (UUID/randomUUID)
                    [a b] (at-once [#(inv c1 P C 10 100000) #(inv c2 Q C 10 100000)])
                    named (:person (client/invitation-of c1 C))]
                (or (and (= {:invited P} a) (= {:refused :claim-in-use} b) (= P named))
                    (and (= {:invited Q} b) (= {:refused :claim-in-use} a) (= Q named)))))]
           ["Two accounts that prove one claim at one moment: one becomes the person and the other is refused with :not-invited and leaves no record; the claim holds one link and she has one account."
            (fn []
              (let [k (fresh) C {:by :email :hash (email-hash (str "two-acc-" k "@x.org"))}
                    P (UUID/randomUUID) A1 [:google (str "two-acc-" k ".1")] A2 [:github (str "two-acc-" k ".2")]
                    _ (inv c1 P C 10 100000)
                    rs (at-once [#(ent c1 A1 C 20) #(ent c2 A2 C 20)])
                    [won lost] (if (= P (:person (first rs))) [A1 A2] [A2 A1])]
                (and (= #{P :not-invited} (set (map #(or (:person %) (:refused %)) rs)))
                     (= 1 (:links (client/invitation-of c1 C)))
                     (= [won] (map :account (:accounts (client/accounts-of c1 P))))
                     (nil? (acc-row lost)))))]
           ["An enter and a take-back of its claim at one moment: the enter is let in and the claim holds its link, or it is refused, the claim is free and the account has no record."
            (fn []
              (let [k (fresh) C {:by :email :hash (email-hash (str "tb-" k "@x.org"))}
                    P (UUID/randomUUID) A [:google (str "tb-" k)] I (UUID/randomUUID)
                    _ (client/invite c1 {:person P :claim C :invited-by :system :at 10 :ends 100000 :op-id I})
                    [e t] (at-once [#(ent c1 A C 20) #(client/take-back c2 {:person P :claim C :invite I :at 20})])
                    io (client/invitation-of c1 C)]
                (and (= {:taken-back P} t)
                     (or (and (= P (:person e)) (= {:person P :invitation nil :links 1} io)
                              (= [A] (map :account (:accounts (client/accounts-of c1 P)))))
                         (and (= {:refused :not-invited} e) (nil? io) (nil? (acc-row A))
                              (= [] (:accounts (client/accounts-of c1 P))))))))]
           ["An enter and an unlink of the same account at one moment: the unlink gives {:unlinked P}, the enter gives her or is refused, and after both the account has no record, she has no account and the claim is free."
            (fn []
              (let [k (fresh) C {:by :email :hash (email-hash (str "eu-" k "@x.org"))}
                    P (UUID/randomUUID) A [:google (str "eu-" k)]
                    _ (inv c1 P C 10 100000)
                    _ (ent c1 A C 20)
                    L (:link (first (:accounts (client/accounts-of c1 P))))
                    [e u] (at-once [#(ent c1 A C 30) #(client/unlink c2 {:account A :link L :person P :at 30})])]
                (and (= {:unlinked P} u)
                     (or (= P (:person e)) (= {:refused :not-invited} e))
                     (nil? (acc-row A))
                     (= [] (:accounts (client/accounts-of c1 P)))
                     (nil? (client/invitation-of c1 C)))))]
           ["An unlink and a new invite of the claim that it frees, at one moment: the invite of another person is :claim-in-use and the claim is then free, or it is taken and the claim names him, with his invitation and no link."
            (fn []
              (let [k (fresh) C {:by :email :hash (email-hash (str "ui-" k "@x.org"))}
                    P (UUID/randomUUID) Q (UUID/randomUUID) A [:google (str "ui-" k)] I (UUID/randomUUID)
                    _ (inv c1 P C 10 100000)
                    _ (ent c1 A C 20)
                    L (:link (first (:accounts (client/accounts-of c1 P))))
                    [u i] (at-once [#(client/unlink c1 {:account A :link L :person P :at 30})
                                    #(client/invite c2 {:person Q :claim C :invited-by :system :at 30 :ends 100000 :op-id I})])
                    io (client/invitation-of c1 C)]
                (and (= {:unlinked P} u)
                     (or (and (= {:refused :claim-in-use} i) (nil? io))
                         (and (= {:invited Q} i) (= Q (:person io)) (= I (get-in io [:invitation :invite])) (= 0 (:links io)))))))]
           ["Enters at and before an invitation's end, at one moment: the one at its end is refused with :not-invited, and the one before it is let in."
            (fn []
              (let [k (fresh) C {:by :email :hash (email-hash (str "end-" k "@x.org"))}
                    P (UUID/randomUUID) A1 [:google (str "end-" k ".1")] A2 [:github (str "end-" k ".2")]
                    _ (inv c1 P C 10 100)
                    [late early] (at-once [#(ent c1 A1 C 100) #(ent c2 A2 C 99)])]
                (and (= {:refused :not-invited} late) (= P (:person early)) (nil? (acc-row A1)))))]]
          scenario-results
          (vec (for [[label f] scenarios]
                 (let [fails (vec (keep (fn [r] (let [ok? (try (f) (catch Throwable t (str t)))] (when-not (true? ok?) [r ok?])))
                                        (range rounds)))]
                   [(str label " (" rounds " times)") (empty? fails) fails])))
          ;; The three cases of the yes, at Auth's edge.
          address "asha@example.org"
          C {:by :email :hash (email-hash address)}
          C2 {:by :code :hash (email-hash "asha's other code")}
          P (UUID/randomUUID) Q (UUID/randomUUID)
          A1 [:google "asha-google-108200000000000000001"]
          A2 [:github "asha-github-5551"]
          I-self (UUID/randomUUID)
          {:keys [met partitions]} (partitions-met ipc mod-name c1)
          all-tables ["$$claims" "$$accounts" "$$invite-ops" "$$enter-ops" "$$root-data"]
          _ (inv c1 P C 10 100000)
          invited-only (pr-str (into {} (map (fn [t] [t (every-record ipc mod-name t)])) all-tables))
          r1 (ent c1 A1 C 20 her-says)
          _ (client/invite c1 {:person P :claim C :invited-by P :at 30 :ends 100000 :op-id I-self})
          r2 (ent c2 A2 C 40 her-says)
          I-named (UUID/randomUUID)
          _ (client/invite c1 {:person P :claim C2 :invited-by P :at 45 :ends 100000 :op-id I-named})
          two (client/accounts-of c1 P)
          unlinked (mapv (fn [{:keys [account link]}] (client/unlink c1 {:account account :link link :person P :at 50}))
                         (:accounts two))
          taken (client/take-back c1 {:person P :claim C2 :invite I-named :at 55})
          tables (into {} (map (fn [t] [t (every-record ipc mod-name t)])) ["$$claims" "$$accounts" "$$invite-ops" "$$enter-ops"])
          roots (every-record ipc mod-name "$$root-data")
          hers (into {} (filter (fn [[k _]] (.startsWith ^String k (str P)))) roots)
          held (pr-str [tables hers])
          names [(:hash C) (:hash C2) address (second A1) (second A2) "Asha Erasable"]
          after-in (ent c1 A1 C 60)
          Q-in (inv c1 Q C 70 100000)
          invite-log (pr-str (mapv second (depot-records ipc mod-name "*invites")))
          enter-log (pr-str (mapv second (depot-records ipc mod-name "*enters")))
          ;; Over, or taken back: nobody comes in.
          C3 {:by :email :hash (email-hash "bo@example.org")}
          C4 {:by :email :hash (email-hash "cy@example.org")}
          I4 (UUID/randomUUID)
          _ (inv c1 (UUID/randomUUID) C3 10 100)
          over (ent c1 [:google "bo-1"] C3 100)
          _ (client/invite c1 {:person (UUID/randomUUID) :claim C4 :invited-by :system :at 10 :ends 100000 :op-id I4})
          _ (client/take-back c1 {:person P :claim C4 :invite I4 :at 20})
          back (ent c1 [:google "cy-1"] C4 30)
          cases [["The 64 keys with which the tables are read here meet every partition: an own-lane-made under each, which Auth refuses and keeps nothing of, lands on the partition of *root-events that its key meets, and the 64 land on all of them."
                  (and (pos? partitions) (= (set (range partitions)) met))
                  {:met met :partitions partitions}]
                 ["After her invitation, no record of *invites holds her address, though they hold its hash; and before she enters, no key and no value of $$claims, $$accounts, $$invite-ops, $$enter-ops or $$root-data holds it. The search finds her address where it is: in the records of *enters, in what her provider said."
                  (and (not (.contains ^String invite-log ^String address))
                       (.contains ^String invite-log ^String (:hash C))
                       (not (.contains ^String invited-only ^String address))
                       (.contains ^String invited-only ^String (:hash C))
                       (.contains ^String enter-log ^String address))
                  {:invites-hold-address (.contains ^String invite-log ^String address)
                   :tables-hold-address (.contains ^String invited-only ^String address)
                   :enters-hold-address (.contains ^String enter-log ^String address)}]
                 ["Two accounts enter as one person: her first account by an invitation, her second by an invitation that names herself under the same address; accounts-of gives both."
                  (and (= P (:person r1)) (= P (:person r2)) (= [A1 A2] (map :account (:accounts two))))
                  [r1 r2 two]]
                 ["After unlinking each account that accounts-of gives and taking back the invitation that is named, no key and no value of $$claims, $$accounts, $$invite-ops, $$enter-ops or her entries holds the hash of her address or of her code, her address, her subjects or her :says, which held her address; her own record stays and holds none of them; her first account is refused, and her address can be invited as another person. It reads the tables, not the log of *enters, whose records keep her :says until a migration."
                  (and (= [{:unlinked P} {:unlinked P}] unlinked) (= {:taken-back P} taken)
                       (not-any? #(.contains ^String held ^String %) names)
                       (= [(s/k-root P)] (keys hers))
                       (= {:refused :not-invited} after-in)
                       (= {:invited Q} Q-in))
                  [unlinked taken (filter #(.contains ^String held ^String %) names) (keys hers) after-in Q-in]]
                 ["An invitation that is over, and one that was taken back, let nobody in."
                  (and (= {:refused :not-invited} over) (= {:refused :not-invited} back))
                  [over back]]]]
      (client/close! c1)
      (client/close! c2)
      (doseq [[label ok? seen] (concat scenario-results cases)]
        (if ok?
          (println "PASSED" label)
          (println "FAILED" label (str "seen=" (pr-str seen)))))
      (every? second (concat scenario-results cases)))))

;; ------------------------------------------------- the steps, by hand

(def ^:private enter-steps
  "Each step of `enter` and `unlink` as `softland.auth.module` runs it: the
  table that it reads, the key that it reads there, and how the step is
  called, through its var. The step writes to the table that it reads. A
  step that removes an entry of hers reads nothing: the entry's key names
  its link."
  {:enter/step-1 [:accounts (fn [ev _] (:account-key ev)) (fn [ev _ x] (s/enter-step-1 ev x))]
   :enter/step-1u-claim [:claims (fn [_ st] (:claim-key (:mark st))) #'s/enter-step-1u-claim]
   :enter/step-1u-person [:root-data (fn [_ _] nil) (fn [ev st _] (s/enter-step-1u-person ev st))]
   :enter/step-1u-account [:accounts (fn [ev _] (:account-key ev)) #'s/enter-step-1u-account]
   :enter/step-1a-check-op-returning [:enter-ops (fn [ev _] (:op-id ev)) #'s/enter-step-1a-check-op-returning]
   :enter/step-1c-update-says [:accounts (fn [ev _] (:account-key ev)) #'s/enter-step-1c-update-says]
   :enter/step-1b-read-flag [:root-data (fn [_ st] (s/k-root (:linked-person st))) (fn [_ st x] (s/enter-step-1b-read-flag st x))]
   :enter/step-2-probe [:claims (fn [_ st] (:curr-claim st)) #'s/enter-step-2-probe]
   :enter/step-2b-choose [:accounts (fn [ev _] (:account-key ev)) #'s/enter-step-2b-choose]
   :enter/step-2c-confirm-choice [:enter-ops (fn [_ st] (:op-id (:target st))) #'s/enter-step-2c-confirm-choice]
   :enter/step-2c-mark-failed [:enter-ops (fn [_ st] (:op-id (:target st))) #'s/enter-step-2c-mark-failed]
   :enter/step-2c-mark-failed-direct [:enter-ops (fn [ev _] (:op-id ev)) (fn [ev _ x] (s/enter-step-2c-mark-failed-direct ev x))]
   :enter/step-2d-finish-failed [:accounts (fn [ev _] (:account-key ev)) #'s/enter-step-2d-finish-failed]
   :enter/step-3-use [:claims (fn [_ st] (:curr-claim st)) #'s/enter-step-3-use]
   :enter/step-3b-clear-chosen [:accounts (fn [ev _] (:account-key ev)) #'s/enter-step-3b-clear-chosen]
   :enter/step-4-root [:root-data (fn [_ st] (s/k-root (:won-person (:target st)))) #'s/enter-step-4-root]
   :enter/step-5-link [:accounts (fn [ev _] (:account-key ev)) #'s/enter-step-5-link]
   :enter/step-5r-claim [:claims (fn [_ st] (:won-claim-key (:target st))) #'s/enter-step-5r-claim]
   :enter/step-5r-person [:root-data (fn [_ _] nil) (fn [ev st _] (s/enter-step-5r-person ev st))]
   :unlink/step-1 [:accounts (fn [ev _] (:account-key ev)) (fn [ev _ x] (s/unlink-step-1 ev x))]
   :unlink/step-2 [:claims (fn [_ st] (:claim-key (:mark st))) #'s/unlink-step-2]
   :unlink/step-3 [:root-data (fn [_ _] nil) (fn [ev st _] (s/unlink-step-3 ev st))]
   :unlink/step-5 [:accounts (fn [ev _] (:account-key ev)) #'s/unlink-step-5]})

(def ^:private no-tables
  {:accounts {} :claims {} :root-data {} :enter-ops {} :invite-ops {}})

(defn- written
  "The table `t` with what the act writes."
  [t act]
  (reduce (fn [t [k v]] (if (some? v) (assoc t k v) (dissoc t k))) t (s/act-writes act)))

(defn- one-step
  "Runs one step of the chain `[step st]` of the event `ev` on the tables.
  Gives the tables and either `{:done result}` or `{:next [step st]}`."
  [tables ev [step st]]
  (let [[table key-of f] (get enter-steps step)
        act (f ev st (get-in tables [table (key-of ev st)]))
        tables (update tables table written act)]
    [tables (if (s/act-done? act)
              {:done (s/act-result act)}
              {:next [(s/act-next-step act) (s/act-next-st act)]})]))

(defn- first-step
  [ev]
  (if (instance? softland.auth.schema.UnlinkEvent ev) [:unlink/step-1 {}] [:enter/step-1 {}]))

(defn- run-until
  "Runs the chain of `ev`, from `chain` or its first step, until it is done
  or its next step is `stop`, the `skip`th time that it comes to it. Gives
  the tables and `{:done result}` or `{:next chain}`."
  ([tables ev] (run-until tables ev (first-step ev) nil 0))
  ([tables ev chain] (run-until tables ev chain nil 0))
  ([tables ev chain stop skip]
   (loop [tables tables chain chain seen 0 n 0]
     (when (> n 200) (throw (ex-info "a chain that does not end" {:ev ev :chain chain})))
     (let [[tables out] (one-step tables ev chain)]
       (cond
         (contains? out :done) [tables out]
         (and stop (= stop (first (:next out))) (= seen skip)) [tables out]
         (and stop (= stop (first (:next out)))) (recur tables (:next out) (inc seen) (inc n))
         :else (recur tables (:next out) seen (inc n)))))))

(defn- invited
  "Runs an invite's one step on the tables."
  [tables ev]
  (let [ck (:claim-key ev)
        act (#'s/invite-step ev (get-in tables [:claims ck]) (get-in tables [:invite-ops (:op-id ev)]))]
    [(cond-> (update tables :claims written act)
       (s/act-op-v act) (assoc-in [:invite-ops (:op-id ev)] (s/act-op-v act)))
     (s/act-result act)]))

(defn- taken-back
  "Runs a take-back's one step on the tables."
  [tables ev]
  (let [act (s/take-back-step ev (get-in tables [:claims (:claim-key ev)]))]
    [(update tables :claims written act) (s/act-result act)]))

(defn- result-of
  [[tables out]]
  [tables (:done out)])

(defn- accounts-in
  "What `accounts-of` gives of the person `P` over the tables: her entries
  in their range, as the query reads them."
  [tables P]
  (let [lo (s/k-account-lo P) hi (s/k-account-hi P)]
    (s/project-accounts (into {} (filter (fn [[k _]] (and (string? k) (neg? (compare k hi)) (not (neg? (compare k lo))))))
                              (:root-data tables)))))

(defn check-reruns!
  "Needs no cluster. A record that runs again is not something the
  generated histories make: on a healthy cluster no hop fails and no record
  is replayed. So the two rules of running again, and the round and the
  release of `enter`, are held here by calling the step functions of
  `softland.auth.schema` by hand, over tables held as maps, reading and
  writing as `softland.auth.module` does, in the order of events that each
  is for. What this holds is the steps, not the module: not Rama's order of
  hops, and not that a record runs again from its first step."
  []
  (let [h (fn [n] (format "%064x" n))
        P (UUID. 1 1) Q (UUID. 1 2)
        C (s/claim-key-from-parts :email (h 1))
        C2 (s/claim-key-from-parts :code (h 2))
        A (s/account-key :google "a")
        A2 (s/account-key :github "a")
        [i1 i2 i3 e1 e2 x f u h1 h2] (map #(UUID. 2 %) (range 10))
        lane (UUID. 3 1)
        mk (UUID. 3 2) ms (UUID. 3 3)
        invite (fn [op who ck at] (s/->InviteEvent op who ck :system nil (+ at 100000) at))
        ;; She invites herself, as she adds an account: beside her links,
        ;; only this is written.
        her-invite (fn [op who ck at] (s/->InviteEvent op who ck :actor who (+ at 100000) at))
        enter (fn [op ak at & {:keys [email code]}]
                (s/->EnterEvent op ak (if (= ak A2) :github :google) "a" {"name" "A"} email code lane mk ms at))
        unlink (fn [ak L who at] (s/->UnlinkEvent ak L who at))
        t0 no-tables
        ;; P invited under C, and her account A linked by e1.
        [t _] (invited t0 (invite i1 P C 10))
        [t-in r-e1] (result-of (run-until t (enter e1 A 20 :email (h 1))))
        results
        [;; A
         (let [x-ev (enter x A 30 :email (h 1))
               [t r-x] (result-of (run-until t-in x-ev))
               [t _] (result-of (run-until t (unlink A e1 P 40)))
               [t _] (invited t (invite i2 Q C 50))
               [t r-again] (result-of (run-until t x-ev))]
           ["A: a returning enter that runs again after its account was unlinked and its claim was invited for another person is refused, and links nothing: the account has no record, and the claim's invitation of the other person still waits."
            (and (= P (:person r-x)) (= {:refused :not-invited} r-again)
                 (nil? (get-in t [:accounts A]))
                 (= Q (get-in t [:claims C :person])) (= i2 (get-in t [:claims C :waiting :invite-op])))
            [r-x r-again (get-in t [:claims C])]])
         ;; E(i)
         (let [[t _] (invited t0 (invite i1 P C 10))
               [t _] (taken-back t (s/->TakeBackEvent C P i1 20))
               [t r] (invited t (invite i1 P C 10))
               between (get-in t [:claims C])
               [t r2] (taken-back t (s/->TakeBackEvent C P i1 20))]
           ["E(i): an invite that runs again after its invitation was taken back changes nothing and gives the answer it gave, and so does the take-back after it: no invitation waits between the two, nor after."
            (and (= {:invited P} r) (= {:taken-back P} r2) (nil? between) (nil? (get-in t [:claims C])))
            [between (get-in t [:claims C])]])
         ;; E(ii)
         (let [[t _] (result-of (run-until t-in (unlink A e1 P 40)))
               [t r] (invited t (invite i1 P C 10))]
           ["E(ii): an invite that runs again after the link that its invitation let in was removed changes nothing and gives the answer it gave: she is not invited again."
            (and (= {:invited P} r) (nil? (get-in t [:claims C])))
            (get-in t [:claims C])])
         ;; take-back again
         (let [[t _] (invited t0 (invite i1 P C 10))
               [t _] (taken-back t (s/->TakeBackEvent C P i1 20))
               [t _] (invited t (invite i2 P C 30))
               [t r] (taken-back t (s/->TakeBackEvent C P i1 20))]
           ["A take-back that runs again after the same person was invited anew under the claim leaves the new invitation."
            (and (= {:taken-back P} r) (= i2 (get-in t [:claims C :waiting :invite-op])))
            (get-in t [:claims C])])
         ;; unlink again
         (let [u-ev (unlink A e1 P 40)
               [t _] (result-of (run-until t-in u-ev))
               [t _] (invited t (invite i2 P C 50))
               [t r-e2] (result-of (run-until t (enter e2 A 60 :email (h 1))))
               [t r] (result-of (run-until t u-ev))]
           ["An unlink that runs again after the account was linked anew leaves the new link, her new entry and the claim's new link."
            (and (= P (:person r-e2)) (= {:unlinked P} r)
                 (= e2 (get-in t [:accounts A :link-op]))
                 (= [e2] (map :link (:accounts (accounts-in t P))))
                 (= #{e2} (set (keys (get-in t [:claims C :links])))))
            [r-e2 r (get-in t [:accounts A]) (get-in t [:claims C])]])
         ;; F(i)
         (let [u-ev (unlink A e1 P 40)
               [t _] (invited t-in (invite i2 Q C2 35))
               [t out] (run-until t u-ev [:unlink/step-1 {}] :unlink/step-2 0)
               mark (get-in t [:accounts A])
               [t r-f] (result-of (run-until t (enter f A 60 :code (h 2))))
               [t r] (result-of (run-until t u-ev))]
           ["F(i): an unlink that failed after its mark, which an enter then finished before it linked the account to another person, gives {:unlinked P} when it runs again, and leaves the new link."
            (and (instance? softland.auth.schema.AccountUnlinking mark) (= Q (:person r-f)) (= {:unlinked P} r)
                 (= f (get-in t [:accounts A :link-op])) (= Q (get-in t [:accounts A :person]))
                 (nil? (get-in t [:claims C])))
            [out r-f r (get-in t [:accounts A])]])
         ;; F(ii)
         (let [[t _] (invited t0 (invite i1 P C 10))
               e-ev (enter e1 A 20 :email (h 1))
               [t out] (run-until t e-ev [:enter/step-1 {}] :enter/step-5-link 0)
               entry (get-in t [:root-data (s/k-account P A e1)])
               [t r] (result-of (run-until t (unlink A e1 P 25)))
               [t r-e] (result-of (run-until t e-ev (:next out)))]
           ["F(ii): an unlink of a link that the enter is still making, which her accounts show already, is refused with :linking and changes nothing; the enter then links."
            (and (= e1 (:link-op entry)) (= {:refused :linking} r) (= P (:person r-e))
                 (= e1 (get-in t [:accounts A :link-op])))
            [r r-e]])
         ;; D, the nine steps
         (let [[t _] (invited t0 (invite i1 P C 10))
               E (enter e1 A 20 :email (h 1))
               H1 (enter h1 A 21 :email (h 1))
               H2 (enter h2 A 22 :email (h 1))
               ;; 1. E and H1 come to the use of C, chosen in round 0.
               [t oE] (run-until t E [:enter/step-1 {}] :enter/step-3-use 0)
               [t oH1] (run-until t H1 [:enter/step-1 {}] :enter/step-3-use 0)
               ;; 2. take-back.
               [t _] (taken-back t (s/->TakeBackEvent C P i1 23))
               ;; 3. E and H1 use: gone.
               [t oE] (one-step t E (:next oE))
               [t oH1] (one-step t H1 (:next oH1))
               ;; 4. E's 3b resets to probing, round 1; E probes, finds nothing; its 2b is slow.
               [t oE] (run-until t E (:next oE) :enter/step-2b-choose 0)
               round-after-reset (get-in t [:accounts A :round])
               ;; 5. invite again.
               [t _] (invited t (invite i2 P C 15))
               ;; 6. H2: probe, choose, confirm, use for e1; it hops to P.
               [t oH2] (run-until t H2 [:enter/step-1 {}] :enter/step-4-root 0)
               ;; 7. H1's late 3b.
               before-7 (get-in t [:accounts A])
               [t oH1] (one-step t H1 (:next oH1))
               after-7 (get-in t [:accounts A])
               ;; 8. E's late 2b, then E to its end.
               [t rE] (result-of (run-until t E (:next oE)))
               ;; 9. H2 and H1 to their ends.
               [t rH2] (result-of (run-until t H2 (:next oH2)))
               [t rH1] (result-of (run-until t H1 (:next oH1)))]
           ["D, the nine steps, with the round of a linking: the late reset of H1 in the round that has passed changes nothing, the target is linked by its own op, the claim holds its link, and every chain gives her."
            (and (= 1 round-after-reset) (= before-7 after-7)
                 (= [P P P] (map :person [rE rH2 rH1]))
                 (= e1 (get-in t [:accounts A :link-op]))
                 (= #{e1} (set (keys (get-in t [:claims C :links]))))
                 (= [e1] (map :link (:accounts (accounts-in t P)))))
            [rE rH2 rH1 (get-in t [:accounts A]) (get-in t [:claims C])]])
         ;; D, the second order
         (let [[t _] (invited t0 (invite i1 P C 10))
               E (enter e1 A 20 :email (h 1))
               H1 (enter h1 A 21 :email (h 1))
               [t oE] (run-until t E [:enter/step-1 {}] :enter/step-3-use 0)
               [t oH1] (run-until t H1 [:enter/step-1 {}] :enter/step-3-use 0)
               [t _] (taken-back t (s/->TakeBackEvent C P i1 23))
               ;; E's use is lost; E probes again, finds nothing, fails and is cleared.
               [t rE] (result-of (run-until t E (:next oE)))
               cleared (get-in t [:accounts A])
               ;; The claim is invited again, by a clock behind E's.
               [t _] (invited t (invite i2 P C 15))
               ;; H1's use of round 0 lands and takes the new invitation for e1.
               [t oH1] (one-step t H1 (:next oH1))
               took (get-in t [:claims C :links])
               [t rH1] (result-of (run-until t H1 (:next oH1)))]
           ["D, the second order: a use that lands after its target was refused takes the new invitation and is released at step 5: the claim holds no link of that op, she has no entry of the account, and nobody is given as a person; the invitation that it used is spent."
            (and (= {:refused :not-invited} rE) (nil? cleared) (contains? took e1)
                 (= {:refused :not-invited} rH1)
                 (nil? (get-in t [:claims C]))
                 (= [] (:accounts (accounts-in t P)))
                 (nil? (get-in t [:accounts A])))
            [rE rH1 (get-in t [:claims C]) (accounts-in t P) (get-in t [:accounts A])]])
         ;; A linking that is :failing at step 5
         (let [[t _] (invited t0 (invite i1 P C 10))
               E (enter e1 A 20 :email (h 1))
               H1 (enter h1 A 21 :email (h 1))
               [t oE] (run-until t E [:enter/step-1 {}] :enter/step-3-use 0)
               [t oH1] (run-until t H1 [:enter/step-1 {}] :enter/step-3-use 0)
               [t _] (taken-back t (s/->TakeBackEvent C P i1 23))
               ;; E's use is lost; E probes again and finds nothing: its
               ;; linking is :failing and its op is marked failed, and E's
               ;; last step, which clears the linking, is slow.
               [t oE] (run-until t E (:next oE) :enter/step-2d-finish-failed 0)
               failing (get-in t [:accounts A :status])
               ;; The claim is invited again, by a clock behind E's, and
               ;; H1's use of round 0 lands and takes it for e1.
               [t _] (invited t (invite i2 P C 15))
               [t rH1] (result-of (run-until t H1 (:next oH1)))
               [t rE] (result-of (run-until t E (:next oE)))]
           ["A linking that is :failing when its step 5 comes is released, as a link of another op would be: the enter that was told :not-invited links nothing, the claim holds no link of its op, she has no entry of the account, and the account has no record."
            (and (= :failing failing) (= {:refused :not-invited} rE)
                 (not (instance? softland.auth.schema.AccountLinked (get-in t [:accounts A])))
                 (nil? (get-in t [:accounts A]))
                 (not (contains? (get-in t [:claims C :links]) e1))
                 (= [] (:accounts (accounts-in t P))))
            [rE rH1 (get-in t [:accounts A]) (get-in t [:claims C]) (accounts-in t P)]])
         ;; A late step 4 of an old link, after the account was linked anew
         (let [[t _] (invited t0 (invite i1 P C 10))
               [lt ht lnew] (map #(UUID. 4 %) (range 3))
               T (enter lt A 30 :email (h 1))
               H (enter ht A 31 :email (h 1))
               [t oT] (run-until t T [:enter/step-1 {}] :enter/step-3-use 0)
               [t oH] (run-until t H [:enter/step-1 {}] :enter/step-3-use 0)
               ;; T uses and links; H's use of T's op comes after, and its
               ;; hop to step 4 is late.
               [t rT] (result-of (run-until t T (:next oT)))
               [t oH] (one-step t H (:next oH))
               [t _] (result-of (run-until t (unlink A lt P 40)))
               ;; She is invited again and an enter by a clock behind T's
               ;; links the account anew.
               [t _] (invited t (invite i2 P C 15))
               [t rN] (result-of (run-until t (enter lnew A 20 :email (h 1))))
               [t rH] (result-of (run-until t H (:next oH)))]
           ["A late step 4 of a link that was unlinked, landing after the account was linked anew by an enter whose :at is before the old one's, and then released, leaves the new link's entry: the account is linked by the new link and accounts-of gives exactly it."
            (and (= P (:person rT)) (= P (:person rN))
                 (= lnew (get-in t [:accounts A :link-op]))
                 (= [lnew] (map :link (:accounts (accounts-in t P)))))
            [rT rN rH (get-in t [:accounts A]) (accounts-in t P)]])
         ;; A late step 4 of an old link, then the step 4 of a new one
         (let [[t _] (invited t0 (invite i1 P C 10))
               [ll hl lt] (map #(UUID. 5 %) (range 3))
               L (enter ll A 30 :email (h 1))
               HL (enter hl A 31 :email (h 1))
               [t oL] (run-until t L [:enter/step-1 {}] :enter/step-3-use 0)
               [t oHL] (run-until t HL [:enter/step-1 {}] :enter/step-3-use 0)
               [t rL] (result-of (run-until t L (:next oL)))
               [t oHL] (one-step t HL (:next oHL))
               [t _] (result-of (run-until t (unlink A ll P 40)))
               [t _] (invited t (invite i2 P C 15))
               T (enter lt A 20 :email (h 1))
               [t oT] (run-until t T [:enter/step-1 {}] :enter/step-4-root 0)
               ;; L's late step 4, then T's step 4, then L's release, then
               ;; T's link.
               [t oHL] (one-step t HL (:next oHL))
               [t oT] (one-step t T (:next oT))
               both (accounts-in t P)
               [t rHL] (result-of (run-until t HL (:next oHL)))
               [t rT] (result-of (run-until t T (:next oT)))]
           ["A late step 4 of a link that was unlinked, then the step 4 of a new link of the same account, then the old one's release, then the new one's link: the account is linked by the new link and accounts-of gives exactly it, though for a moment it gave the account twice, under the two links."
            (and (= P (:person rL)) (= P (:person rT))
                 (= #{ll lt} (set (map :link (:accounts both))))
                 (= lt (get-in t [:accounts A :link-op]))
                 (= [lt] (map :link (:accounts (accounts-in t P)))))
            [rL rT rHL (:accounts both) (get-in t [:accounts A]) (accounts-in t P)]])
         ;; The probe's recheck of the code
         (let [[t _] (invited t0 (invite i1 P C 10))
               E (enter e1 A 20 :email (h 1) :code (h 2))
               ;; E probes the code's claim, which has no record, and goes
               ;; on to the email's.
               [t oE] (run-until t E [:enter/step-1 {}] :enter/step-2-probe 1)
               ;; Meanwhile Q is invited under the code, and P's invitation
               ;; under the email is taken back: at every moment one of the
               ;; two lets E in.
               [t _] (invited t (invite i2 Q C2 20))
               [t _] (taken-back t (s/->TakeBackEvent C P i1 20))
               [t rE] (result-of (run-until t E (:next oE)))]
           ["The probe rechecks the code once when both probes failed: an enter whose email's invitation is taken back while an invitation under its code is made, one of the two letting it in at every moment, is let in by the code."
            (and (= Q (:person rE)) (= e1 (get-in t [:accounts A :link-op])))
            [rE (get-in t [:accounts A]) (get-in t [:claims C2])]])
         ;; C
         (let [[t _] (invited t-in (her-invite i2 P C 30))
               [t r-2] (result-of (run-until t (enter e2 A2 40 :email (h 1))))
               [t u1] (result-of (run-until t (unlink A e1 P 50)))
               names-after-1 (get-in t [:claims C :person])
               [t r-2again] (result-of (run-until t (enter x A2 60 :email (h 1))))
               [t u2] (result-of (run-until t (unlink A2 e2 P 70)))
               [t r-q] (invited t (invite i3 Q C 80))]
           ["C: she invites herself under the claim of her first account and enters with a second account of the same address; after the first is unlinked the claim still names her and the second account still enters; after the second is unlinked the claim is free."
            (and (= [P P P] (map :person [r-e1 r-2 r-2again])) (= {:unlinked P} u1) (= {:unlinked P} u2)
                 (= P names-after-1) (= {:invited Q} r-q)
                 (empty? (get-in t [:claims C :links])))
            [r-2 u1 r-2again u2 r-q]])]]
    (doseq [[label ok? seen] results]
      (if ok?
        (println "PASSED" (str "The steps, called by hand and not through the module. " label))
        (println "FAILED" (str "The steps, called by hand and not through the module. " label) (str "seen=" (pr-str seen)))))
    (every? second results)))

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
    `ends-with` — the plan of `end-actor` follows whose ending ends an agent (no cluster);
    `reruns`    — the steps of invite, take-back, enter and unlink in the orders of events of a record that runs again (no cluster);
    `invitations` — two writes of invitations and accounts at one moment, `rounds` times each, and the three cases of the yes of 28 November with Auth's tables read."
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
              "reruns" (check-reruns!)
              "invitations" (check-invitations! rounds)
              "step3" (and (check-says-table!)
                           (check-watch-not-reached!)
                           (run-step3-scenarios! {:allowed-prefixes nil
                                                  :rounds rounds
                                                  :strip-watches? false}))
              (do (println "Unknown mode:" mode) false))]
    (flush)
    (shutdown-agents)
    (System/exit (if ok? 0 1))))
