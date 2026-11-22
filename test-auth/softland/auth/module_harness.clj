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
                                      :code-hash nil :own-layer L1 :making M1 :at 500})
          row-refused (read-account-row ipc mod-name acc-refused)
          ;; 2. Invite + first enter at t=200
          r-inv (client/invite c1 {:person P1 :claim {:by :email :email email1} :invited-by :system :at 10})
          r-e1 (client/enter c1 {:account acc1 :says {"name" "v200"} :email email1 :code-hash nil
                                 :own-layer L1 :making M1 :at 200})
          row-1 (read-account-row ipc mod-name acc1)
          ;; 3. Returning enter at equal t=200 -> leaves :says untouched!
          _ (client/enter c2 {:account acc1 :says {"name" "v200-tie"} :email nil :code-hash nil
                              :own-layer (UUID/randomUUID) :making M1 :at 200})
          row-tie (read-account-row ipc mod-name acc1)
          ;; 4. Returning enter at earlier t=100 -> leaves :says untouched!
          _ (client/enter c1 {:account acc1 :says {"name" "v100-old"} :email nil :code-hash nil
                              :own-layer (UUID/randomUUID) :making M1 :at 100})
          row-old (read-account-row ipc mod-name acc1)
          ;; 5. Returning enter at later t=350 -> replaces :says!
          _ (client/enter c2 {:account acc1 :says {"name" "v350-new"} :email nil :code-hash nil
                              :own-layer (UUID/randomUUID) :making M1 :at 350})
          row-new (read-account-row ipc mod-name acc1)
          ;; 6. Four concurrent returning enters (t = 400, 700, 500, 600) -> settles on t=700!
          pool (Executors/newFixedThreadPool 4)
          barrier (CyclicBarrier. 4)
          futs (mapv (fn [[t lbl]]
                       (.submit pool ^Callable
                                (fn []
                                  (.await barrier 10 TimeUnit/SECONDS)
                                  (client/enter c1 {:account acc1 :says {"name" lbl} :email nil :code-hash nil
                                                    :own-layer L1 :making M1 :at t}))))
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
          (let [scs (cond->> (build-sc)
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
  2-seek `who-is` (before and after `own-layer-made`), and live-only `under`."
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
                          :code-hash nil :own-layer L :making M :at 20}]
                 ;; Sign-in begun BEFORE own-layer-made (tests who-is fallback path!)
                 [:begin-sign-in {:root P :actor P :sign-in G-early :kind :browser :hash (h 1)
                                  :until 5000 :label "early" :at 25}]
                 [:own-layer-made {:person P :at 30}]
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
            dispatch {:invite client/invite :enter client/enter :own-layer-made client/own-layer-made
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

(defn -main
  "Entry point for slice checks:
    `says`    — run direct `$$accounts` `:says` check;
    `slice-a` — run `:says` check + carbon step3 scenarios 1a, 1b, 2a, 2b, 3;
    `slice-b` — run Slice b root rules vs reference + carbon step3 scenarios 1a..3, 5, 6;
    `slice-c` — run `:says` check + carbon step3 all 8 scenarios (without watches);
    `step3`   — run full carbon step3 suite (all 8 scenarios with live watches, 2..8 threads)."
  [& [mode rounds-str]]
  (let [rounds (or (some-> rounds-str parse-long) 2)
        ok? (case mode
              "says" (check-says-table!)
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
              "step3" (and (check-says-table!)
                           (run-step3-scenarios! {:allowed-prefixes nil
                                                  :rounds rounds
                                                  :strip-watches? false}))
              (do (println "Unknown mode:" mode) false))]
    (flush)
    (shutdown-agents)
    (System/exit (if ok? 0 1))))
