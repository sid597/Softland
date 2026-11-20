(ns softland.server.sessions
  "The server's one client of its own module
  (`softland.server.module/Sessions`): it begins and ends a session, reads
  the row of a credential, and tells a listener when a row changed, through
  whichever copy of the server the change came. It requires Rama.

  The calls, each of which but `open` takes what `open` gave first:

    open      a cluster handle and the module's name -> the sessions, or
              {:could-not :find-the-module}
    row-of    a hash -> [:ok the row or nil], or [:threw info]
    begin!    a `:began` event -> [:ok], or [:threw info]
    end!      an `:ended` event -> [:ok], or [:threw info]
    listen!   a hash and a function of one argument -> a function of no
              argument that stops the listening
    close!    nothing more -> nil

  The events and the rows are `softland.server.session.record`'s.

  State. Nothing that lasts: the rows and the feed are the module's. In the
  memory of one process it holds three handles of Rama's client, borrowed
  from the opener's cluster handle; one push on the feed; `listeners`, an
  atom from a hash to the functions that listen for it; `seen`, the number
  of the feed's next change as this process last saw it; and `broken`,
  whether Rama ended the push. All of it
  is lost when the process ends, with no loss of truth.

  A listener. `listen!` registers a function for one hash. It is called
  with the hash's row each time the feed tells a change of that hash. It is
  called with `softland.server.sessions/not-reached` when this process can
  no longer say whether the session stands: the feed said that changes
  were missed and the row could not be read again, or is not there; or
  Rama ended the push on the feed, after which every listener, and each
  one that comes later, is told so. A caller that
  must know the row at the start registers first and then reads the row
  itself, and takes the join of what the two give
  (`softland.server.session.record/join`): so no change falls between the
  read and the listening, whichever arrives first.

  The listener's function runs on a thread of Rama's client, the thread of
  the feed's push, and must return at once: while it stays, no other
  listener of this process is told anything. A function that throws is
  dropped for that call and stays registered.

  When changes were missed (`softland.server.session.lately/news`), the
  rows of all hashes that are listened for are read again, on a thread of
  this namespace's own and not on Rama's.

  No call throws for what a caller gave or for what Rama did."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [softland.server.session.lately :as lately]
            [softland.server.session.record :as record])
  (:import [com.rpl.rama.diffs DestroyedDiff UngracefulTerminationDiff]
           [java.io Writer]
           [java.util.concurrent ExecutorService Executors]))

(def not-reached
  "What a listener is called with when this process can no longer say
  whether the hash's session stands (the namespace docstring says when).
  The listener ends what rests on it, and its caller asks again."
  ::not-reached)

(deftype Sessions [module depot table proxy listeners seen broken executor]
  Object
  (toString [_] (str "#<sessions " module ">")))

(defmethod print-method Sessions [^Sessions sessions ^Writer w]
  (.write w (.toString sessions)))

(alter-meta! #'->Sessions assoc
             :private true
             :doc "Makes what `open` gives. It holds the module's name; the depot, the
  table of rows and the push on the feed, of Rama's client; `listeners`,
  `seen` and `broken`; and the executor for reading rows again. It prints as the word
  \"sessions\" and the module's name.")

(defn- what-threw
  "Takes what a call of Rama's client threw. Gives {:threw the name of its
  class, :said its message}. An interrupt sets the thread's mark again."
  [^Throwable t]
  (when (instance? InterruptedException t)
    (.interrupt (Thread/currentThread)))
  {:threw (.getName (class t)) :said (.getMessage t)})

(defn- read-row
  "Reads the row under a hash from the table: one round trip, to the task
  of the hash. Gives [:ok the row or nil] or [:threw info]. A thing that is
  no hash's text is asked of nothing and gives [:ok nil]."
  [table hash]
  (if-not (record/hash-text? hash)
    [:ok nil]
    (try [:ok (rama/foreign-select-one (path/keypath hash) table)]
         (catch Throwable t [:threw (what-threw t)]))))

(defn- tell!
  "Calls each function that listens for a hash with one thing. A function
  that throws is dropped for this call."
  [listeners hash thing]
  (doseq [f (vals (get @listeners hash))]
    (try (f thing) (catch Throwable _ nil))))

(defn- read-all-again!
  "Reads the row of each hash that is listened for, and tells its
  listeners: the row; or `not-reached` when the read threw, and when there
  is no row. It runs after the feed said that changes were missed.

  Why no row tells `not-reached`. No event takes a row away, so a hash
  that had a row and has none now belongs to a module that was launched
  anew: the credential that a listener rests on is gone, and a listener
  that was told nothing would keep its watch open until the session's time
  passed. A listener cannot go back from live to none by the order of rows,
  so it is told that the sessions could not be reached, and ends. A program
  that waits for a person's yes has no row either; it is told the same, and
  asks again."
  [table listeners]
  (doseq [hash (keys @listeners)]
    (let [[outcome row] (read-row table hash)]
      (tell! listeners hash (if (and (= :ok outcome) (some? row)) row not-reached)))))

(defn- on-feed
  "One callback of the push on the feed. Takes the table, `listeners`,
  `seen`, `broken`, the executor, the feed's value now and Rama's diff. It
  takes what this process had not seen
  (`softland.server.session.lately/news`), tells the listeners of each
  changed hash its row, and, when changes were missed, hands the reading of
  every listened row to the executor. It never throws into Rama's thread.

  When Rama ends the push after too many errors, nothing more will ever be
  told through it. `broken` is set, and each listener is told
  `not-reached`: a copy of the server whose push is gone cannot say that a
  session still stands. The last callback of a push that `close!` closed
  does nothing."
  [table listeners seen broken ^ExecutorService executor feed diff]
  (try
    (cond
      (instance? DestroyedDiff diff) nil

      (instance? UngracefulTerminationDiff diff)
      (do (reset! broken true)
          (doseq [hash (keys @listeners)]
            (tell! listeners hash not-reached)))

      :else
      (let [{:keys [rows missed?] now-seen :seen} (lately/news @seen feed)]
        (reset! seen now-seen)
        (doseq [[hash row] rows]
          (tell! listeners hash row))
        (when missed?
          (.execute executor (fn [] (read-all-again! table listeners))))))
    (catch Throwable _ nil)))

(defn open
  "Opens the server's client of its module. Takes a cluster handle of
  Rama's and the name that Rama gives the module `Sessions`. Gives the
  sessions, or {:could-not :find-the-module} with the module's name and
  what Rama threw.

  Four round trips, once for a process: a client for the depot and for each
  of the two PStates, and the opening of the one push on the feed. The
  opener owns the cluster handle. `close!` closes the push."
  [cluster module-name]
  (try
    (let [listeners (atom {})
          seen (atom nil)
          broken (atom false)
          executor (Executors/newVirtualThreadPerTaskExecutor)
          depot (rama/foreign-depot cluster module-name "*session-events")
          table (rama/foreign-pstate cluster module-name "$$sessions")
          feed (rama/foreign-pstate cluster module-name "$$sessions-lately")
          proxy (rama/foreign-proxy path/STAY feed
                                    {:callback-fn (fn [new diff _old]
                                                    (on-feed table listeners seen broken executor new diff))})]
      ;; The first callback can come after this call returns. A listener
      ;; that registers before it must not lose a change, so what the push
      ;; holds now is what this process has seen.
      (compare-and-set! seen nil (long (:next @proxy 0)))
      (->Sessions module-name depot table proxy listeners seen broken executor))
    (catch Throwable t
      (assoc (what-threw t) :could-not :find-the-module :module module-name))))

(defn row-of
  "Reads the row of a credential. Takes the sessions and the credential's
  hash. Gives [:ok the row or nil], or [:threw info]: a read that failed
  has not said that there is no session."
  [^Sessions sessions hash]
  (read-row (.-table sessions) hash))

(defn- append!
  "Appends one event with the ack level `:ack`: the call returns when the
  topology has written the row and the feed. Gives [:ok] or [:threw info];
  an append that threw may have gone through."
  [^Sessions sessions event]
  (try (rama/foreign-append! (.-depot sessions) event :ack)
       [:ok]
       (catch Throwable t [:threw (what-threw t)])))

(defn begin!
  "Begins a session. Takes the sessions and a `:began` event
  (`softland.server.session.record/began`). Gives [:ok] or [:threw info].
  [:ok] says that the event is processed, not that it bound the hash: a
  hash that already has a row keeps it. A caller that must know reads the
  row."
  [sessions event]
  (append! sessions event))

(defn end!
  "Ends a session, or refuses a hash for good. Takes the sessions and an
  `:ended` event (`softland.server.session.record/ended`). Gives [:ok] or
  [:threw info]."
  [sessions event]
  (append! sessions event))

(defn listen!
  "Listens for the changes of one hash's row. Takes the sessions, a hash
  and a function of one argument, which is called as the namespace
  docstring says. Gives a function of no argument that stops the
  listening; it can be called more than once."
  [^Sessions sessions hash f]
  (let [listeners (.-listeners sessions)
        id (Object.)]
    (swap! listeners assoc-in [hash id] f)
    ;; A push that Rama ended tells nothing more, so a listener that comes
    ;; after it is told at once that the sessions cannot be reached.
    (when @(.-broken sessions)
      (try (f not-reached) (catch Throwable _ nil)))
    (fn []
      (swap! listeners (fn [m]
                         (let [left (dissoc (get m hash) id)]
                           (if (empty? left) (dissoc m hash) (assoc m hash left)))))
      nil)))

(defn close!
  "Closes the push on the feed and stops the executor. The listeners are
  told nothing."
  [^Sessions sessions]
  (try (rama/close! (.-proxy sessions)) (catch Throwable _ nil))
  (.shutdown ^ExecutorService (.-executor sessions))
  nil)
