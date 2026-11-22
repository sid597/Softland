(ns softland.runtime.standing
  "Who is told when what is live under a person changes: one watch of Auth
  for each root that has something open in this process, shared by
  everything of that root. It requires Auth's client.

  A connection stays open for hours, and its sign-in can end at any moment,
  through any copy of the runtime. So each open thing listens here for its
  root. Auth pushes what is live under a root to every copy that watches it
  (`softland.auth.client/watch-root`), and this namespace hands that to each
  listener of the root.

  What a listener may take from it. What Auth pushed is a signal that
  something under the root changed, and what the value held at the push. It
  is not the judge. A push can be behind a write that has already returned:
  a session that was opened a moment ago can be missing from the value that
  arrives next. So a listener that sees itself gone reads Auth before it
  ends anything (`softland.runtime.seam`), and a read decides. An end is
  never taken back, so a read that says ended is final.

  The calls:

    open     Auth's client -> the standing
    listen!  the standing, a root and a function of one argument -> a
             function of no argument that stops the listening
    close!   the standing -> nil

  A listener's function is called with what is live under the root,
  `{:sign-ins {...} :sessions {...}}` (Auth's spec, \"The client's
  contract\"), or with `not-reached` when this process can no longer say
  what is live there. It is called on a thread of this namespace's own,
  never on Rama's. A value is handed on one call at a time for one root,
  and always the newest: a listener that is slower than two changes is
  handed the later one alone; so a listener hands a value on and does not
  block (the seam's does). `not-reached` is told to each listener on a
  thread of its own, so a listener that blocks while it ends what rests on
  it holds back no other listener of the root.

  State. Nothing that lasts. In the memory of the process: `roots`, an atom
  from a root to its one watch and its listeners, and an executor of
  virtual threads. All of it is lost when the process ends, with no loss of
  truth: what is live is Auth's."
  (:require [softland.auth.client :as auth])
  (:import [java.io Writer]
           [java.util.concurrent ExecutorService Executors]))

(def not-reached
  "What a listener is called with when this process can no longer say what
  is live under its root: Auth's own word for it, handed on."
  ::auth/not-reached)

(deftype Standing [client roots executor]
  Object
  (toString [_] "#<standing>"))

(defmethod print-method Standing [^Standing standing ^Writer w]
  (.write w (.toString standing)))

(alter-meta! #'->Standing assoc
             :private true
             :doc "Makes what `open` gives: Auth's client, borrowed; `roots`, an atom from
  a root to its entry (`entry-for`); and an executor of virtual threads.")

(defn open
  "Takes Auth's client (`softland.auth.client/open`). Gives the standing.
  It makes no call to Rama. The opener owns the client."
  [client]
  (->Standing client (atom {}) (Executors/newVirtualThreadPerTaskExecutor)))

(defn- drain!
  "Hands the newest value of one root to each of its listeners, and again
  while a newer one came in the meantime. Takes the root's entry. One
  thread at a time does this for a root: `draining` says so. A listener
  that throws is dropped for that call."
  [{:keys [latest draining listeners]}]
  (loop []
    (let [value @latest]
      (doseq [f (vals @listeners)]
        (try (f value) (catch Throwable _ nil)))
      (reset! draining false)
      (when (and (not (identical? value @latest))
                 (compare-and-set! draining false true))
        (recur)))))

(defn- pushed!
  "One callback of Auth's watch of a root: it runs on a thread of Rama's
  client and returns at once. It keeps the value as the root's newest and,
  when no thread is handing values on for the root, starts one.

  `not-reached` is the watch's last word. The root's entry is taken out
  first, so that a listener that comes after it opens a watch anew, and
  then the listeners that the lost watch had are told, each once and each
  on a virtual thread of its own: one that blocks holds back no other. When
  the executor is shut, a listener is told on the calling thread."
  [^Standing standing root entry value]
  (if (= not-reached value)
    (let [roots (.-roots standing)]
      (swap! roots (fn [m] (if (identical? entry (get m root)) (dissoc m root) m)))
      (doseq [f (vals @(:listeners entry))]
        (let [tell (fn [] (try (f not-reached) (catch Throwable _ nil)))]
          (try (.execute ^ExecutorService (.-executor standing) ^Runnable tell)
               (catch Throwable _ (tell))))))
    (let [{:keys [latest draining]} entry]
      (reset! latest value)
      (when (compare-and-set! draining false true)
        (try (.execute ^ExecutorService (.-executor standing) (fn [] (drain! entry)))
             (catch Throwable _ (reset! draining false)))))))

(defn- entry-for
  "Makes the entry of a root: the atoms of its newest value, of whether a
  thread hands values on, and of its listeners, and a delay around the one
  call that opens Auth's watch. The callback is given the entry itself, so
  that what a lost watch says reaches its own listeners and no others."
  [^Standing standing root]
  (let [entry (volatile! nil)]
    (vreset! entry {:latest (atom nil)
                    :draining (atom false)
                    :listeners (atom {})
                    :opening (delay (auth/watch-root (.-client standing) root
                                                     (fn [value] (pushed! standing root @entry value))))})))

(defn- stop-watch!
  "Stops the watch of an entry, when it was opened."
  [entry]
  (let [stop (try (force (:opening entry)) (catch Throwable _ nil))]
    (when (fn? stop) (try (stop) (catch Throwable _ nil)))))

(defn listen!
  "Listens for the changes of what is live under one root. Takes the
  standing, the root's id and a function of one argument, which is called
  as the namespace docstring says. Gives a function of no argument that
  stops the listening; it can be called more than once.

  The first listener of a root opens Auth's watch of it, and the last one
  to stop closes it: a root with nothing open in this process costs Rama no
  push. Two listeners that come at the same moment share one opening."
  [^Standing standing root f]
  (let [roots (.-roots standing)
        id (Object.)
        ;; An entry can be taken out, and its watch stopped, by its last
        ;; listener between the moment it is found here and the moment
        ;; this listener is in it. So the listener is put in, the entry is
        ;; looked for again, and one that is no longer the root's is left
        ;; for a new one.
        entry (loop []
                (let [entry (get (swap! roots (fn [m] (if (contains? m root) m (assoc m root (entry-for standing root)))))
                                 root)]
                  (swap! (:listeners entry) assoc id f)
                  (if (identical? entry (get @roots root))
                    entry
                    (do (swap! (:listeners entry) dissoc id)
                        (recur)))))]
    (let [stop (try (force (:opening entry))
                    (catch Throwable t
                      (pushed! standing root entry not-reached)
                      (throw t)))]
      (when-not (fn? stop)
        (pushed! standing root entry not-reached)))
    (fn []
      (swap! (:listeners entry) dissoc id)
      (let [[before after] (swap-vals! roots (fn [m]
                                               (if (and (identical? entry (get m root))
                                                        (empty? @(:listeners entry)))
                                                 (dissoc m root)
                                                 m)))]
        (when (and (identical? entry (get before root)) (not (contains? after root)))
          (stop-watch! entry)))
      nil)))

(defn watched
  "Takes the standing. Gives how many roots this process watches now."
  [^Standing standing]
  (count @(.-roots standing)))

(defn close!
  "Stops every watch and the executor. The listeners are told nothing."
  [^Standing standing]
  (doseq [[_ entry] (first (reset-vals! (.-roots standing) {}))]
    (stop-watch! entry))
  (.shutdown ^ExecutorService (.-executor standing))
  nil)
