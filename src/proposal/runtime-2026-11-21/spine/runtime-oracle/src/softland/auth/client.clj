(ns softland.auth.client
  "Thin non-production adapter onto `softland.auth.reference` for running the
  runtime against Auth's reference implementation before real `src-auth/` lands.

  Every write and read passes its input straight to `softland.auth.reference`:
  nothing is added, dropped or loosened, so what the reference refuses, this
  adapter refuses. `open` maps a Rama cluster handle and deployed module name
  to a shared `reference/cluster` (one per `(cluster, module-name)` pair in a
  `WeakHashMap`). `watch-root` hands each callback to a dedicated single-thread
  daemon executor per client, in the order of changes, because the real client
  delivers watch callbacks asynchronously on another thread."
  (:require [com.rpl.rama :as rama]
            [softland.auth.reference :as reference])
  (:import [java.util Collections WeakHashMap]
           [java.util.concurrent ExecutorService Executors RejectedExecutionException ThreadFactory]))

(defonce ^:private clusters
  (Collections/synchronizedMap (WeakHashMap.)))

(defn- ref-cluster-for
  "Returns the shared `reference/cluster` backing `(cluster, module-name)`."
  [cluster module-name]
  (locking clusters
    (let [by-mod (or (.get clusters cluster)
                     (let [m (atom {})]
                       (.put clusters cluster m)
                       m))]
      (or (get @by-mod module-name)
          (let [rc (reference/cluster module-name)]
            (swap! by-mod assoc module-name rc)
            rc)))))

(defn- new-notifier
  "Creates the single-threaded daemon executor that delivers watch callbacks
  for one client in exact change order."
  ^ExecutorService []
  (Executors/newSingleThreadExecutor
   (reify ThreadFactory
     (newThread [_ runnable]
       (doto (Thread. runnable "softland-auth-reference-watch") (.setDaemon true))))))

(deftype AdapterClient [ref-client closed? cb-lock notifier])

(defn- adapter-client?
  "True when `x` is a client opened by this adapter."
  [x]
  (instance? AdapterClient x))

(defn open
  "Opens an Auth client backed by `softland.auth.reference` for `module-name`
  deployed on `cluster`. Gives `{:could-not :find-the-module}` when `cluster`
  is no cluster or does not have `module-name` deployed."
  [cluster module-name]
  (try
    (let [deployed (when (and cluster (string? module-name))
                     (set (rama/deployed-module-names cluster)))]
      (if (contains? deployed module-name)
        (let [ref-client (reference/open (ref-cluster-for cluster module-name) module-name)]
          (if (:could-not ref-client)
            ref-client
            (->AdapterClient ref-client (atom false) (Object.) (new-notifier))))
        {:could-not :find-the-module}))
    (catch Throwable _
      {:could-not :find-the-module})))

(defn close!
  "Closes the client, stops all watches opened through it, and guarantees no
  watch callback runs after `close!` returns. Gives nil for any argument."
  [client]
  (when (adapter-client? client)
    (let [^AdapterClient c client]
      (reference/close! (.-ref-client c))
      (locking (.-cb-lock c)
        (reset! (.-closed? c) true))
      (.shutdown ^ExecutorService (.-notifier c))))
  nil)

(defn- call-ref
  "Forwards a call unchanged to `f` in `softland.auth.reference`."
  [f client arg]
  (if (adapter-client? client)
    (f (.-ref-client ^AdapterClient client) arg)
    {:could-not :read-the-event}))

;; --------------------------------------------------------------------- writes

(defn invite
  "Forwards `invite` unchanged to `softland.auth.reference/invite`."
  [client input]
  (call-ref reference/invite client input))

(defn enter
  "Forwards `enter` unchanged to `softland.auth.reference/enter`."
  [client input]
  (call-ref reference/enter client input))

(defn own-layer-made
  "Forwards `own-layer-made` unchanged to `softland.auth.reference/own-layer-made`."
  [client input]
  (call-ref reference/own-layer-made client input))

(defn begin-sign-in
  "Forwards `begin-sign-in` unchanged to `softland.auth.reference/begin-sign-in`."
  [client input]
  (call-ref reference/begin-sign-in client input))

(defn replace-secret
  "Forwards `replace-secret` unchanged to `softland.auth.reference/replace-secret`."
  [client input]
  (call-ref reference/replace-secret client input))

(defn end-sign-in
  "Forwards `end-sign-in` unchanged to `softland.auth.reference/end-sign-in`."
  [client input]
  (call-ref reference/end-sign-in client input))

(defn make-actor
  "Forwards `make-actor` unchanged to `softland.auth.reference/make-actor`."
  [client input]
  (call-ref reference/make-actor client input))

(defn end-actor
  "Forwards `end-actor` unchanged to `softland.auth.reference/end-actor`."
  [client input]
  (call-ref reference/end-actor client input))

(defn open-session
  "Forwards `open-session` unchanged to `softland.auth.reference/open-session`."
  [client input]
  (call-ref reference/open-session client input))

(defn close-session
  "Forwards `close-session` unchanged to `softland.auth.reference/close-session`."
  [client input]
  (call-ref reference/close-session client input))

(defn ask
  "Forwards `ask` unchanged to `softland.auth.reference/ask`."
  [client input]
  (call-ref reference/ask client input))

(defn answer-ask
  "Forwards `answer-ask` unchanged to `softland.auth.reference/answer-ask`."
  [client input]
  (call-ref reference/answer-ask client input))

;; ---------------------------------------------------------------------- reads

(defn who-is
  "Forwards `who-is` unchanged to `softland.auth.reference/who-is`."
  [client query]
  (call-ref reference/who-is client query))

(defn ask-of
  "Forwards `ask-of` unchanged to `softland.auth.reference/ask-of`."
  [client code]
  (call-ref reference/ask-of client code))

(defn under
  "Forwards `under` unchanged to `softland.auth.reference/under`."
  [client root]
  (call-ref reference/under client root))

(defn watch-root
  "Forwards `watch-root` to `softland.auth.reference/watch-root`, handing each
  callback to this client's single-threaded daemon executor in change order."
  [client root f]
  (if-not (adapter-client? client)
    {:could-not :read-the-event}
    (let [^AdapterClient c client
          ref-client (.-ref-client c)]
      (if-not (fn? f)
        (reference/watch-root ref-client root f)
        (let [stopped? (atom false)
              ^ExecutorService notifier (.-notifier c)
              cb-lock (.-cb-lock c)
              closed? (.-closed? c)
              wrapper (fn [v]
                        (when-not (or @closed? @stopped?)
                          (try
                            (.execute notifier
                                      ^Runnable
                                      (fn []
                                        (locking cb-lock
                                          (when-not (or @closed? @stopped?)
                                            (try (f v) (catch Throwable _ nil))))))
                            (catch RejectedExecutionException _ nil)))
                        nil)
              got (reference/watch-root ref-client root wrapper)]
          (if (fn? got)
            (fn stop []
              (got)
              (locking cb-lock
                (reset! stopped? true))
              nil)
            got))))))
