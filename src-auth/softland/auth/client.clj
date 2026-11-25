(ns softland.auth.client
  "Foreign client for the Rama module `Auth`: the 18 functions of Auth's
  contract (`src/proposal/runtime-2026-11-21/spine/auth/SPEC.md`). A client
  holds no Auth truth.

  No public function throws. Arguments that do not fit
  (`softland.auth.fit`), a first argument that is no client among them, give
  `{:could-not :read-the-event}` and append nothing to any depot. A closed
  client gives `{:could-not :closed}`, from `watch-root` too, in place of
  the stop function. A transport failure or a timeout gives
  `{:could-not :reach-the-module ...}`.

  `watch-root` holds one `foreign-proxy-async` for each `(client, root)`, on
  that root's own map in `$$live`. The first `ResyncDiff`, and a
  `DestroyedDiff`, are not delivered. Liveness is watched with
  `\"ping-task\"` (2 s cadence, 5 s timeout + 3 s partial-outage probe = 8 s
  ceiling), so that `:softland.auth.client/not-reached` is delivered exactly
  once within seconds of loss."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [softland.auth.fit :as fit]
            [softland.auth.schema :as s])
  (:import [com.rpl.rama.diffs DestroyedDiff Diff Diff$Processor KeyDiff KeyDiff$Processor
            KeyRemoveDiff KeyRemoveDiff$Processor ResyncDiff UngracefulTerminationDiff]
           [java.util ArrayList UUID]
           [java.util.concurrent CompletableFuture CountDownLatch ExecutionException Executors
            ScheduledExecutorService ThreadFactory TimeUnit TimeoutException]
           [java.util.concurrent.atomic AtomicBoolean AtomicLong]
           [java.util.function BiConsumer]))

;; ----------------------------------------------------------------- constants

(def ^:private default-timeout-ms
  10000)

(def ^:private proxy-open-timeout-ms
  5000)

(def ^:private ping-cadence-ms
  2000)

(def ^:private ping-timeout-ms
  "Revision 2: >= 5 s."
  5000)

(def ^:private fallback-probe-timeout-ms
  "Revision 2: >= 3 s."
  3000)

(def ^:private topology-name
  "The key of Auth's `ack-return>` in Rama's `AckReturnMap`."
  "auth")

(def ^:private not-reached-sentinel
  :softland.auth.client/not-reached)

(def ^:private ^ThreadLocal current-callback-wid
  "The id of the watcher whose callback runs on this thread, so that a
  re-entrant `stop` or `close!` never deadlocks waiting for its own turn."
  (ThreadLocal.))

(def ^:private ^AtomicLong next-watch-id
  (AtomicLong. 0))

;; ------------------------------------------------------------- client handle

(deftype Client [module-name
                 num-partitions
                 d-root-events
                 d-asks
                 d-invites
                 d-enters
                 p-asks
                 p-live
                 q-who-is
                 q-under
                 q-ping-task
                 closed?
                 watch-lock
                 proxies
                 scheduler
                 monitor-task])

(defn- client?
  [x]
  (instance? Client x))

(defn- unwrap-cause
  [^Throwable t]
  (loop [cur t]
    (if (and (instance? ExecutionException cur) (.getCause cur))
      (recur (.getCause cur))
      cur)))

(defn- find-module-error?
  "True for a handle that is no cluster, or a module name that is not
  there; false for a network outage."
  [^Throwable t]
  (let [cause (unwrap-cause t)
        msg (or (.getMessage cause) "")]
    (or (instance? ClassCastException cause)
        (instance? IllegalArgumentException cause)
        (boolean (re-find #"(?i)not alive|does not exist|not found|no module|unknown module|no such" msg)))))

(defn- await-future
  [^CompletableFuture fut ^long timeout-ms]
  (.get fut (max 0 timeout-ms) TimeUnit/MILLISECONDS))

(defn- close-proxy-quietly!
  [proxy]
  (when proxy
    (try (rama/close! proxy) (catch Throwable _ nil))))

(defn- append-and-ack!
  [depot event]
  (try
    (let [ack-map (await-future (rama/foreign-append-async! depot event :ack) default-timeout-ms)
          res (get ack-map topology-name)]
      (if (some? res)
        res
        {:could-not :reach-the-module :error "missing auth ack-return"}))
    (catch TimeoutException t
      {:could-not :reach-the-module :error (str "timeout: " (.getMessage t))})
    (catch Throwable t
      (let [c (unwrap-cause t)]
        {:could-not :reach-the-module :error (or (.getMessage c) (str c))}))))

(defn- guarded-write
  [client kind input depot-fn make-event]
  (if-not (client? client)
    {:could-not :read-the-event}
    (if @(.-closed? ^Client client)
      {:could-not :closed}
      (if-let [clean (fit/fit-write kind input)]
        (append-and-ack! (depot-fn ^Client client) (make-event clean))
        {:could-not :read-the-event}))))

;; ------------------------------------------------------- watch-root engine

(defn- make-daemon-scheduler
  ^ScheduledExecutorService []
  (Executors/newSingleThreadScheduledExecutor
   (reify ThreadFactory
     (newThread [_ r]
       (doto (Thread. r "softland-auth-watch-monitor")
         (.setDaemon true))))))

(defn- deliver-value!
  [w val]
  (let [wid (:id w)
        f (:f w)
        st (:state w)]
    (when (= :active @st)
      (locking (:turn w)
        (when (= :active @st)
          (let [prev (.get current-callback-wid)]
            (.set current-callback-wid wid)
            (try
              (f val)
              (catch Throwable _ nil)
              (finally
                (.set current-callback-wid prev)))))))))

(defn- fail-watcher!
  [w]
  (let [wid (:id w)
        f (:f w)
        st (:state w)]
    (when (= :active @st)
      (locking (:turn w)
        (when (compare-and-set! st :active :dead)
          (let [prev (.get current-callback-wid)]
            (.set current-callback-wid wid)
            (try
              (f not-reached-sentinel)
              (catch Throwable _ nil)
              (finally
                (.set current-callback-wid prev)))))))))

(defn- stop-watcher-sync!
  "The empty `locking` waits for a callback in flight on another thread; a
  re-entrant call does not wait."
  [w]
  (let [st (:state w)]
    (when (= :active @st)
      (compare-and-set! st :active :stopped))
    (when (not= (:id w) (.get current-callback-wid))
      (locking (:turn w) nil))))

(defn- detach-proxy-entry!
  [^Client client root entry]
  (locking (.-watch-lock client)
    (let [curr (get @(.-proxies client) root)]
      (when (identical? curr entry)
        (swap! (.-proxies client) dissoc root)))
    (reset! (:closed? entry) true)
    (let [ws (vec (vals @(:watchers entry)))]
      (reset! (:watchers entry) {})
      [@(:proxy entry) ws])))

(defn- fail-proxy-entry!
  [^Client client root entry]
  (let [[proxy ws] (detach-proxy-entry! client root entry)]
    (close-proxy-quietly! proxy)
    (doseq [w ws]
      (fail-watcher! w))))

(defn- fail-all-proxies!
  [^Client client]
  (let [entries (locking (.-watch-lock client)
                  (let [es (vec (vals @(.-proxies client)))]
                    (reset! (.-proxies client) {})
                    (doseq [e es] (reset! (:closed? e) true))
                    es))]
    (doseq [e entries]
      (let [ws (locking (.-watch-lock client)
                 (let [v (vec (vals @(:watchers e)))]
                   (reset! (:watchers e) {})
                   v))]
        (close-proxy-quietly! @(:proxy e))
        (doseq [w ws]
          (fail-watcher! w))))))

(defn- extract-key-ops
  "`:full` when any sub-diff requires rebuilding from `new-val`."
  [diff]
  (try
    (let [ops (ArrayList.)
          unhandled? (AtomicBoolean. false)
          proc (reify
                 Diff$Processor
                 (unhandled [_] (.set unhandled? true))
                 KeyDiff$Processor
                 (processKeyDiff [_ d] (.add ops [:put (.getKey ^KeyDiff d)]))
                 KeyRemoveDiff$Processor
                 (processKeyRemoveDiff [_ d] (.add ops [:out (.getKey ^KeyRemoveDiff d)])))]
      (.process ^Diff diff proc)
      (if (.get unhandled?) :full (vec ops)))
    (catch Throwable _ :full)))

(defn- apply-diff-to-projected
  [prev-proj new-val diff]
  (let [ops (extract-key-ops diff)]
    (if (= :full ops)
      (s/project-live-map new-val)
      (reduce
       (fn [acc [op k]]
         (case op
           :put
           (if-let [entry (get new-val k)]
             (if-let [[section id proj] (s/project-live-entry entry)]
               (assoc-in acc [section id] proj)
               acc)
             (if-let [[section id] (s/parse-live-key k)]
               (update acc section dissoc id)
               acc))
           :out
           (if-let [[section id] (s/parse-live-key k)]
             (update acc section dissoc id)
             acc)))
       prev-proj
       ops))))

(defn- on-proxy-callback!
  [^Client client root entry new-val diff _old-val]
  (when-not (or @(.-closed? client) @(:closed? entry))
    (cond
      (instance? DestroyedDiff diff)
      nil

      (instance? UngracefulTerminationDiff diff)
      (fail-proxy-entry! client root entry)

      (instance? ResyncDiff diff)
      (let [proj (s/project-live-map new-val)
            deliver? (locking (:proj-lock entry)
                       (if (compare-and-set! (:initialized? entry) false true)
                         (do (reset! (:projected entry) proj)
                             false)
                         (let [prev @(:projected entry)]
                           (when (not= prev proj)
                             (reset! (:projected entry) proj)
                             true))))]
        (when deliver?
          (doseq [w (vals @(:watchers entry))]
            (deliver-value! w proj))))

      :else
      (let [next-proj (locking (:proj-lock entry)
                        (let [prev @(:projected entry)
                              nxt (apply-diff-to-projected prev new-val diff)]
                          (when (not= prev nxt)
                            (reset! (:projected entry) nxt)
                            nxt)))]
        (when (some? next-proj)
          (doseq [w (vals @(:watchers entry))]
            (deliver-value! w next-proj)))))))

(defn- probe-root
  ^CompletableFuture [^Client client root]
  (rama/foreign-select-async [(path/keypath root) (path/sorted-map-range-from s/live-lo {:max-amt 1})]
                             (.-p-live client)
                             {:pkey root}))

(defn- unreached-roots
  "All probes wait against ONE deadline, `fallback-probe-timeout-ms` from the
  start: so the sweep takes that long at most, however many roots are
  watched."
  [^Client client roots]
  (let [deadline (+ (System/currentTimeMillis) fallback-probe-timeout-ms)
        probes (mapv (fn [root] [root (try (probe-root client root) (catch Throwable _ nil))])
                     roots)]
    (into #{}
          (keep (fn [[root fut]]
                  (when-not (and fut
                                 (try (await-future fut (max 0 (- deadline (System/currentTimeMillis))))
                                      true
                                      (catch Throwable _ false)))
                    root)))
          probes)))

(defn- run-liveness-sweep!
  [^Client client]
  (when-not @(.-closed? client)
    (let [active-entries (locking (.-watch-lock client)
                           (vec (vals @(.-proxies client))))]
      (when (seq active-entries)
        (let [n (.-num-partitions client)
              q-ping (.-q-ping-task client)
              deadline (+ (System/currentTimeMillis) ping-timeout-ms)
              futs (mapv (fn [tid]
                           (try
                             [tid (rama/foreign-invoke-query-async q-ping tid)]
                             (catch Throwable _ [tid nil])))
                         (range n))
              outcomes (mapv (fn [[tid fut]]
                               (if (nil? fut)
                                 [tid false]
                                 (let [rem-ms (max 0 (- deadline (System/currentTimeMillis)))]
                                   (try
                                     [tid (true? (await-future fut rem-ms))]
                                     (catch Throwable _ [tid false])))))
                             futs)
              failed-cnt (count (remove second outcomes))]
          (cond
            (zero? failed-cnt)
            nil

            (= failed-cnt n)
            (fail-all-proxies! client)

            :else
            (let [live (filterv (fn [e] (not @(:closed? e))) active-entries)
                  dead (when (and (seq live) (not @(.-closed? client)))
                         (unreached-roots client (mapv :root live)))]
              (doseq [e live]
                (when (and (contains? dead (:root e))
                           (not @(.-closed? client))
                           (not @(:closed? e)))
                  (fail-proxy-entry! client (:root e) e))))))))))

(defn- ensure-monitor-started!
  "The caller must hold `watch-lock`."
  [^Client client]
  (when (and (not @(.-closed? client)) (nil? @(.-monitor-task client)))
    (let [^ScheduledExecutorService exec @(.-scheduler client)
          exec (or exec
                   (let [e (make-daemon-scheduler)]
                     (reset! (.-scheduler client) e)
                     e))
          fut (.scheduleWithFixedDelay exec
                                       ^Runnable (fn [] (try (run-liveness-sweep! client) (catch Throwable _ nil)))
                                       ping-cadence-ms
                                       ping-cadence-ms
                                       TimeUnit/MILLISECONDS)]
      (reset! (.-monitor-task client) fut))))

(defn- schedule-not-reached!
  "On a virtual thread of its own: never on the scheduler's one thread, which
  a liveness sweep can hold for seconds, and never on the caller's, which is
  inside `watch-root`."
  [^Client _client w]
  (.start (Thread/ofVirtual) ^Runnable (fn [] (fail-watcher! w))))

;; ---------------------------------------------------------------- lifecycle

(defn open
  [cluster module-name]
  (if (or (nil? cluster) (not (string? module-name)))
    {:could-not :find-the-module :module module-name}
    (try
      (let [d-root    (rama/foreign-depot cluster module-name "*root-events")
            d-asks    (rama/foreign-depot cluster module-name "*asks")
            d-invites (rama/foreign-depot cluster module-name "*invites")
            d-enters  (rama/foreign-depot cluster module-name "*enters")
            p-asks    (rama/foreign-pstate cluster module-name "$$asks")
            p-live    (rama/foreign-pstate cluster module-name "$$live")
            q-who-is  (rama/foreign-query cluster module-name "who-is")
            q-under   (rama/foreign-query cluster module-name "under")
            q-ping    (rama/foreign-query cluster module-name "ping-task")
            info      (rama/foreign-object-info p-live)
            n-parts   (long (or (:num-partitions info) 1))]
        (->Client module-name
                  n-parts
                  d-root d-asks d-invites d-enters
                  p-asks p-live
                  q-who-is q-under q-ping
                  (atom false)
                  (Object.)
                  (atom {})
                  (atom nil)
                  (atom nil)))
      (catch Throwable t
        (if (find-module-error? t)
          {:could-not :find-the-module :module module-name :error (or (.getMessage (unwrap-cause t)) (str t))}
          {:could-not :reach-the-module :module module-name :error (or (.getMessage (unwrap-cause t)) (str t))})))))

(defn close!
  "No watcher is told `:not-reached`. It waits for any callback in flight on
  another thread, so no callback runs after `close!` returns."
  [client]
  (when (client? client)
    (let [^Client c client
          [entries watchers ^ScheduledExecutorService exec mon-fut]
          (locking (.-watch-lock c)
            (reset! (.-closed? c) true)
            (let [es (vec (vals @(.-proxies c)))
                  ws (vec (mapcat #(vals @(:watchers %)) es))
                  ex @(.-scheduler c)
                  mf @(.-monitor-task c)]
              (reset! (.-proxies c) {})
              (reset! (.-monitor-task c) nil)
              (reset! (.-scheduler c) nil)
              (doseq [e es]
                (reset! (:closed? e) true)
                (reset! (:watchers e) {}))
              (doseq [w ws]
                (compare-and-set! (:state w) :active :stopped))
              [es ws ex mf]))]
      (when mon-fut
        (try (.cancel ^java.util.concurrent.Future mon-fut true) (catch Throwable _ nil)))
      (when exec
        (try (.shutdownNow exec) (catch Throwable _ nil)))
      (doseq [e entries]
        (close-proxy-quietly! @(:proxy e)))
      (doseq [w watchers]
        (stop-watcher-sync! w))))
  nil)

;; ---------------------------------------------------------------- 12 writes

(defn invite
  [client input]
  (guarded-write
   client :invite input #(.-d-invites ^Client %)
   (fn [{:keys [person claim invited-by at]}]
     (let [ck (s/claim-key-from-map claim)
           cv (s/claim-val-from-map claim)
           ib-kind (if (= :system invited-by) :system :actor)
           ib-id   (when (uuid? invited-by) invited-by)]
       (s/->InviteEvent person ck (:by claim) cv ib-kind ib-id at)))))

(defn enter
  [client input]
  (guarded-write
   client :enter input #(.-d-enters ^Client %)
   (fn [{:keys [account says email code-hash own-lane making at]}]
     (let [[provider subject] account
           ak (s/account-key provider subject)]
       (s/->EnterEvent (UUID/randomUUID) ak provider subject says email code-hash
                       own-lane (:uuid making) (:session making) at)))))

(defn own-lane-made
  [client input]
  (guarded-write
   client :own-lane-made input #(.-d-root-events ^Client %)
   (fn [{:keys [person at]}]
     (s/->OwnLaneMadeEvent person at))))

(defn begin-sign-in
  [client input]
  (guarded-write
   client :begin-sign-in input #(.-d-root-events ^Client %)
   (fn [{:keys [root actor sign-in kind hash until label at]}]
     (s/->BeginSignInEvent root actor sign-in kind hash until label at))))

(defn replace-secret
  [client input]
  (guarded-write
   client :replace-secret input #(.-d-root-events ^Client %)
   (fn [{:keys [root sign-in old-hash new-hash until previous-until at]}]
     (s/->ReplaceSecretEvent root sign-in old-hash new-hash until previous-until at))))

(defn end-sign-in
  [client input]
  (guarded-write
   client :end-sign-in input #(.-d-root-events ^Client %)
   (fn [{:keys [root sign-in at]}]
     (s/->EndSignInEvent root sign-in at))))

(defn make-actor
  [client input]
  (guarded-write
   client :make-actor input #(.-d-root-events ^Client %)
   (fn [{:keys [root actor made-by label at]}]
     (s/->MakeActorEvent root actor made-by label at))))

(defn end-actor
  [client input]
  (guarded-write
   client :end-actor input #(.-d-root-events ^Client %)
   (fn [{:keys [root actor at]}]
     (s/->EndActorEvent root actor at))))

(defn open-session
  [client input]
  (guarded-write
   client :open-session input #(.-d-root-events ^Client %)
   (fn [{:keys [root sign-in session until driven-by at]}]
     (s/->OpenSessionEvent root sign-in session until (s/to-driven-by driven-by) at))))

(defn close-session
  [client input]
  (guarded-write
   client :close-session input #(.-d-root-events ^Client %)
   (fn [{:keys [root session at]}]
     (s/->CloseSessionEvent root session at))))

(defn ask
  [client input]
  (guarded-write
   client :ask input #(.-d-asks ^Client %)
   (fn [{:keys [code hash asks ends at]}]
     (s/->AskEvent code hash asks ends at))))

(defn answer-ask
  [client input]
  (guarded-write
   client :answer-ask input #(.-d-asks ^Client %)
   (fn [{:keys [code yes? root actor new-agent sign-in until at]}]
     (s/->AnswerAskEvent (UUID/randomUUID) code yes? root actor
                         (when new-agent (:label new-agent))
                         sign-in until at))))

;; ----------------------------------------------------------------- 4 reads

(defn who-is
  [client query]
  (if-not (client? client)
    {:could-not :read-the-event}
    (if @(.-closed? ^Client client)
      {:could-not :closed}
      (if-let [{:keys [root sign-in session]} (fit/fit-who-is query)]
        (try
          (await-future (rama/foreign-invoke-query-async (.-q-who-is ^Client client) root sign-in session)
                        default-timeout-ms)
          (catch TimeoutException t
            {:could-not :reach-the-module :error (str "timeout: " (.getMessage t))})
          (catch Throwable t
            (let [c (unwrap-cause t)]
              {:could-not :reach-the-module :error (or (.getMessage c) (str c))})))
        {:could-not :read-the-event}))))

(defn ask-of
  [client code]
  (if-not (client? client)
    {:could-not :read-the-event}
    (if @(.-closed? ^Client client)
      {:could-not :closed}
      (if-let [u (fit/fit-ask-of code)]
        (try
          (let [rec (await-future (rama/foreign-select-one-async [(path/keypath u)] (.-p-asks ^Client client))
                                  default-timeout-ms)]
            (s/project-ask rec))
          (catch TimeoutException t
            {:could-not :reach-the-module :error (str "timeout: " (.getMessage t))})
          (catch Throwable t
            (let [c (unwrap-cause t)]
              {:could-not :reach-the-module :error (or (.getMessage c) (str c))})))
        {:could-not :read-the-event}))))

(defn under
  [client root]
  (if-not (client? client)
    {:could-not :read-the-event}
    (if @(.-closed? ^Client client)
      {:could-not :closed}
      (if-let [p (fit/fit-under root)]
        (try
          (await-future (rama/foreign-invoke-query-async (.-q-under ^Client client) p)
                        default-timeout-ms)
          (catch TimeoutException t
            {:could-not :reach-the-module :error (str "timeout: " (.getMessage t))})
          (catch Throwable t
            (let [c (unwrap-cause t)]
              {:could-not :reach-the-module :error (or (.getMessage c) (str c))})))
        {:could-not :read-the-event}))))

(defn- make-stop-fn
  [^Client client root entry w]
  (let [wid (:id w)]
    (fn stop []
      (let [proxy-to-close
            (locking (.-watch-lock client)
              (swap! (:watchers entry) dissoc wid)
              (when (and (empty? @(:watchers entry))
                         (identical? (get @(.-proxies client) root) entry))
                (swap! (.-proxies client) dissoc root)
                (reset! (:closed? entry) true)
                @(:proxy entry)))]
        (stop-watcher-sync! w)
        (close-proxy-quietly! proxy-to-close)
        nil))))

(defn watch-root
  "`f` gets `{:sign-ins {...} :sessions {...}}` on each change, without
  `:driven-by`, and no call at registration; and
  `:softland.auth.client/not-reached` once when the module or the task
  cannot be reached. Gives a stop function of no argument."
  [client root f]
  (if-not (client? client)
    {:could-not :read-the-event}
    (let [^Client c client]
      (if @(.-closed? c)
        {:could-not :closed}
        (if-let [[p cb] (fit/fit-watch-root root f)]
          (let [wid (.incrementAndGet next-watch-id)
                w {:id wid :root p :f cb :state (atom :active) :turn (Object.)}
                [status entry]
                (locking (.-watch-lock c)
                  (if @(.-closed? c)
                    [:closed nil]
                    (do
                      (ensure-monitor-started! c)
                      (if-let [existing (get @(.-proxies c) p)]
                        (do
                          (swap! (:watchers existing) assoc wid w)
                          [:joined existing])
                        (let [e {:root p
                                 :proxy (atom nil)
                                 :open-done (CountDownLatch. 1)
                                 :open-err (atom nil)
                                 :initialized? (atom false)
                                 :projected (atom {:sign-ins {} :sessions {}})
                                 :proj-lock (Object.)
                                 :watchers (atom {wid w})
                                 :closed? (atom false)}]
                          (swap! (.-proxies c) assoc p e)
                          [:created e])))))]
            (case status
              :closed
              {:could-not :closed}

              :created
              (let [^CompletableFuture pf-fut
                    (try
                      (rama/foreign-proxy-async
                       [(path/keypath p) (path/sorted-map-range s/live-lo s/live-hi)]
                       (.-p-live c)
                       {:pkey p
                        :callback-fn (fn [new-val diff old-val]
                                       (on-proxy-callback! c p entry new-val diff old-val))})
                      (catch Throwable t (CompletableFuture/failedFuture t)))]
                (try
                  (let [proxy (await-future pf-fut proxy-open-timeout-ms)]
                    (let [should-close?
                          (locking (.-watch-lock c)
                            (reset! (:proxy entry) proxy)
                            (or @(.-closed? c) @(:closed? entry)))]
                      (if should-close?
                        (close-proxy-quietly! proxy)
                        (locking (:proj-lock entry)
                          (when (compare-and-set! (:initialized? entry) false true)
                            (try
                              (reset! (:projected entry) (s/project-live-map @proxy))
                              (catch Throwable _ nil))))))
                    (.countDown ^CountDownLatch (:open-done entry))
                    (make-stop-fn c p entry w))
                  (catch Throwable t
                    ;; The open did not finish within the wait, or failed. A
                    ;; proxy that Rama finishes opening after this is held by
                    ;; nothing and counts against pstate.proxy.limit, so it is
                    ;; closed when it comes, off Rama's completing thread.
                    (.whenCompleteAsync pf-fut
                                        (reify BiConsumer
                                          (accept [_ proxy _err] (close-proxy-quietly! proxy))))
                    (reset! (:open-err entry) t)
                    (.countDown ^CountDownLatch (:open-done entry))
                    (let [[proxy ws] (detach-proxy-entry! c p entry)]
                      (close-proxy-quietly! proxy)
                      (doseq [ww ws]
                        (schedule-not-reached! c ww)))
                    (fn stop []
                      (stop-watcher-sync! w)
                      nil))))

              :joined
              (do
                (try
                  (.await ^CountDownLatch (:open-done entry) proxy-open-timeout-ms TimeUnit/MILLISECONDS)
                  (catch InterruptedException _ nil))
                (if (some? @(:open-err entry))
                  (do
                    (schedule-not-reached! c w)
                    (fn stop []
                      (stop-watcher-sync! w)
                      nil))
                  (make-stop-fn c p entry w)))))
          {:could-not :read-the-event})))))
