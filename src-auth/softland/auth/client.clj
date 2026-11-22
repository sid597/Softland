(ns softland.auth.client
  "Foreign client for the Rama module `Auth`, exposing the 18 functions of
  Auth's contract (`src/proposal/runtime-2026-11-21/spine/auth/SPEC.md`).

  Every call validates its arguments at the client edge (`softland.auth.fit`)
  before touching Rama: invalid arguments (including a non-client first
  argument) return `{:could-not :read-the-event}` and append nothing to any
  depot. A closed client returns `{:could-not :closed}` (including from
  `watch-root` in place of the stop function). No public function throws;
  transport or timeout failures return `{:could-not :reach-the-module ...}`.

  State and effects: each open `Client` holds Rama foreign handles to one
  launched `Auth` module, a closed flag, and (from Slice d) its in-JVM
  `watch-root` proxy registry and liveness monitor. It holds no Auth truth."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [softland.auth.fit :as fit]
            [softland.auth.schema :as s])
  (:import [java.util UUID]
           [java.util.concurrent CompletableFuture ExecutionException TimeUnit TimeoutException]))

;; ----------------------------------------------------------------- constants

(def ^:private default-timeout-ms
  "Bounded wait for blocking depot appends, query topologies, and point reads."
  10000)

(def ^:private topology-name
  "Name of Auth's stream topology, used as key in Rama's `AckReturnMap`."
  "auth")

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
                 watches-state])

(defn- client?
  "Whether `x` is an Auth client returned by `open`."
  [x]
  (instance? Client x))

(defn- unwrap-cause
  "Unwraps `ExecutionException` wrappers to the underlying cause."
  [^Throwable t]
  (loop [cur t]
    (if (and (instance? ExecutionException cur) (.getCause cur))
      (recur (.getCause cur))
      cur)))

(defn- find-module-error?
  "Heuristically determines whether throwable `t` from `open` indicates a
  non-cluster handle or a missing module name rather than a network outage."
  [^Throwable t]
  (let [cause (unwrap-cause t)
        msg (or (.getMessage cause) "")]
    (or (instance? ClassCastException cause)
        (instance? IllegalArgumentException cause)
        (boolean (re-find #"(?i)not alive|does not exist|not found|no module|unknown module|no such" msg)))))

(defn- await-future
  "Blocks on `CompletableFuture` `fut` up to `timeout-ms` milliseconds."
  [^CompletableFuture fut ^long timeout-ms]
  (.get fut timeout-ms TimeUnit/MILLISECONDS))

(defn- append-and-ack!
  "Appends `event` to Rama depot `depot` with `:ack` up to `default-timeout-ms`
  and extracts Auth's `ack-return>` map."
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
  "Checks client identity, closed flag, and input form for `kind`, then builds
  the depot record with `make-event` and appends it to `(depot-fn client)`."
  [client kind input depot-fn make-event]
  (if-not (client? client)
    {:could-not :read-the-event}
    (if @(.-closed? ^Client client)
      {:could-not :closed}
      (if-let [clean (fit/fit-write kind input)]
        (append-and-ack! (depot-fn ^Client client) (make-event clean))
        {:could-not :read-the-event}))))

;; ---------------------------------------------------------------- lifecycle

(defn open
  "Opens a client to the launched Rama `Auth` module named `module-name` on
  `cluster`. Returns a `Client` handle, `{:could-not :find-the-module ...}`
  when `cluster` is invalid or `module-name` is absent, or
  `{:could-not :reach-the-module ...}` on transport error. Never throws."
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
                  (atom {:proxies {} :monitor nil})))
      (catch Throwable t
        (if (find-module-error? t)
          {:could-not :find-the-module :module module-name :error (or (.getMessage (unwrap-cause t)) (str t))}
          {:could-not :reach-the-module :module module-name :error (or (.getMessage (unwrap-cause t)) (str t))})))))

(defn close!
  "Idempotently closes `client`: stops any open watches and marks the client
  closed so every subsequent call returns `{:could-not :closed}`. Returns `nil`
  for any argument, including non-clients."
  [client]
  (when (client? client)
    (reset! (.-closed? ^Client client) true))
  nil)

;; ---------------------------------------------------------------- 12 writes

(defn invite
  "Invites a person under a claim (`{:person :claim :invited-by :at}`)."
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
  "Enters an account (`{:account :says :email :code-hash :own-layer :making :at}`)."
  [client input]
  (guarded-write
   client :enter input #(.-d-enters ^Client %)
   (fn [{:keys [account says email code-hash own-layer making at]}]
     (let [[provider subject] account
           ak (s/account-key provider subject)]
       (s/->EnterEvent (UUID/randomUUID) ak provider subject says email code-hash
                       own-layer (:uuid making) (:session making) at)))))

(defn own-layer-made
  "Marks an entered person's own layer as made (`{:person :at}`)."
  [client input]
  (guarded-write
   client :own-layer-made input #(.-d-root-events ^Client %)
   (fn [{:keys [person at]}]
     (s/->OwnLayerMadeEvent person at))))

(defn begin-sign-in
  "Begins a sign-in under a root (`{:root :actor :sign-in :kind :hash :until :label :at}`)."
  [client input]
  (guarded-write
   client :begin-sign-in input #(.-d-root-events ^Client %)
   (fn [{:keys [root actor sign-in kind hash until label at]}]
     (s/->BeginSignInEvent root actor sign-in kind hash until label at))))

(defn replace-secret
  "Replaces a sign-in's secret (`{:root :sign-in :old-hash :new-hash :until :previous-until :at}`)."
  [client input]
  (guarded-write
   client :replace-secret input #(.-d-root-events ^Client %)
   (fn [{:keys [root sign-in old-hash new-hash until previous-until at]}]
     (s/->ReplaceSecretEvent root sign-in old-hash new-hash until previous-until at))))

(defn end-sign-in
  "Ends a sign-in and closes its open sessions (`{:root :sign-in :at}`)."
  [client input]
  (guarded-write
   client :end-sign-in input #(.-d-root-events ^Client %)
   (fn [{:keys [root sign-in at]}]
     (s/->EndSignInEvent root sign-in at))))

(defn make-actor
  "Makes an agent under a root (`{:root :actor :made-by :label :at}`)."
  [client input]
  (guarded-write
   client :make-actor input #(.-d-root-events ^Client %)
   (fn [{:keys [root actor made-by label at]}]
     (s/->MakeActorEvent root actor made-by label at))))

(defn end-actor
  "Ends an agent and its subtree, sign-ins, and sessions (`{:root :actor :at}`)."
  [client input]
  (guarded-write
   client :end-actor input #(.-d-root-events ^Client %)
   (fn [{:keys [root actor at]}]
     (s/->EndActorEvent root actor at))))

(defn open-session
  "Opens or extends a session under a live sign-in (`{:root :sign-in :session :until :at}` plus optional `:driven-by`)."
  [client input]
  (guarded-write
   client :open-session input #(.-d-root-events ^Client %)
   (fn [{:keys [root sign-in session until driven-by at]}]
     (s/->OpenSessionEvent root sign-in session until (s/to-driven-by driven-by) at))))

(defn close-session
  "Closes a session under a root (`{:root :session :at}`)."
  [client input]
  (guarded-write
   client :close-session input #(.-d-root-events ^Client %)
   (fn [{:keys [root session at]}]
     (s/->CloseSessionEvent root session at))))

(defn ask
  "Creates a device-grant ask bound to an 8-char code (implemented in Slice c)."
  [client input]
  (if-not (client? client)
    {:could-not :read-the-event}
    (if @(.-closed? ^Client client)
      {:could-not :closed}
      (if (fit/fit-write :ask input)
        {:could-not :not-yet-built}
        {:could-not :read-the-event}))))

(defn answer-ask
  "Answers a device-grant ask yes or no (implemented in Slice c)."
  [client input]
  (if-not (client? client)
    {:could-not :read-the-event}
    (if @(.-closed? ^Client client)
      {:could-not :closed}
      (if (fit/fit-write :answer-ask input)
        {:could-not :not-yet-built}
        {:could-not :read-the-event}))))

;; ----------------------------------------------------------------- 4 reads

(defn who-is
  "Reads standing for `{:root P :sign-in G :session S-or-nil}` in one round trip
  to task `H(P)` using the 2-seek fast path."
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
  "Reads the standing state of ask code `U` directly from `$$asks` (Slice c)."
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
  "Reads all agents (live and ended) and all live sign-ins under root `P` in
  one round trip to task `H(P)`."
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

(defn watch-root
  "Watches live sign-ins and open sessions under root `P` (implemented in Slice d)."
  [client root f]
  (if-not (client? client)
    {:could-not :read-the-event}
    (if @(.-closed? ^Client client)
      {:could-not :closed}
      (if (fit/fit-watch-root root f)
        {:could-not :not-yet-built}
        {:could-not :read-the-event}))))
