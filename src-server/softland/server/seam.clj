(ns softland.server.seam
  "The seam: the one place that every face of the server stands on. It
  turns a credential into an actor, and it puts that actor in front of the
  door's calls and the read path's. It is Clojure and Missionary, and it
  requires no Electric: a task or a flow is called by an Electric program
  and by everything else, where an Electric function is called by Electric
  alone.

  A face is whatever speaks to one kind of system: the words over HTTP for
  a program that we do not compile, the server half of the browser's
  Electric program. A face proves nothing and writes no who. It hands the
  seam a credential and gets an actor, and each other call takes the actor
  first.

  The calls, each of which but `open` takes the seam first:

    open       the door, the read path, the sessions, and numbers -> a seam
    actor      a token, or nil for a visitor -> a task of an actor, or of
               one of the server's words
    standing   a token -> a flow of what the credential stands for, for as
               long as somebody listens
    begin      a who and the hash of a credential -> a task: the session
               begun, or a word
    end        a hash -> a task: [:ok] or a word
    refuse     an actor and a hash -> a task: [:ok] or a word
    sign-out   an actor -> a task: [:ok] or a word
    act-ids    an actor, a layer, a count -> a task of the door's act-ids
    send       an actor, an act without `:who` and `:session`, and an
               act-id or nil -> a task of the door's receipt
    send-all   an actor and [[act-id act] ...] -> a task of the receipts
    answer     an actor, an act-id, a digest, a wait -> a task of the
               door's answer
    read       an actor and a query without its asker -> a task of the
               read path's answer
    watch      the same two -> a flow: the whole answer, then the changes
               by place, then one end

  A task and a flow are Missionary's. A task does nothing until it is run,
  and a face runs it with `m/?` or inside a process of its own. Each result
  is the door's or the read path's own, as it is: an act-id, a receipt, one
  of the four answers, an answer of a read, or one of their words. The seam
  hides nothing of them. A receipt holds the act-id and the digest, because
  an act is done once only when it is sent again under the same act-id, and
  its answer is read with both (`softland.store.door`).

  The actor. An actor is an object that only this namespace makes: `actor`
  from a credential, `standing` from a credential, `system` for the
  server's own steps. It holds who she is and her session, and it prints as
  nothing but the word. It is no data, so no face can be handed one by a
  browser and believe it: a who that crosses the wire back cannot become an
  actor. A visitor is an actor with no who. She can read what is open to
  everyone and watch it; a word that needs a who gives her
  `:find-her-session`.

  What the seam adds to a form of the store's. To an act: `:who` and
  `:session`, the actor's. To a query: `:asker`, or `:not-signed-in true`
  for a visitor. A face has tested that the thing holds neither
  (`softland.server.words/check`); `send` and `read` test it again, so that
  no face that forgot can pass a name in.

  A session that ends, ends at once on every face. `actor` reads the
  credential's row at each call: one round trip, on the hash's task, and no
  copy of the row is kept between two requests. `standing` and `watch`
  stay open, so each listens on the feed of the sessions that changed
  (`softland.server.sessions`): when her session ends, through this copy of
  the server or another, the flow says so and ends. A session whose time
  passes ends the same way, by a timer of this process.

  The thread rule. A function that the read path calls for a change runs
  on a thread of Rama's client, and one that stays there holds back the
  reads of others (`softland.store.read-path`, \"A live read, and the
  read's turn\"). The same holds for a listener of the feed. The seam takes
  that rule on itself: what such a thread does here is put the change with
  what waits and hand it to a thread of the seam's own. Whatever a face
  does with a thing of a flow, it does on that other thread and never on
  Rama's. What waits for a slow reader is merged by place
  (`softland.server.watching`), so it never grows past one answer and one
  change for each place.

  One limit on a face. The thread that hands a thing on is a virtual
  thread, and it is inside Missionary's own process while the face is
  handed the thing. A face that waits right there, in the function that
  takes the thing, holds a carrier thread of the JVM for that long. So a
  face takes the thing and does its slow work after that returns: the face
  over HTTP writes to its socket from the request's own thread
  (`softland.server.http`, `write-stream`).

  State. The seam owns nothing that lasts. It borrows the door, the read
  path and the sessions. In the memory of the process it holds its numbers,
  the id of the server's own session, an executor of virtual threads and
  one timer thread. It keeps no row, no answer and no actor between two
  calls.

  The duties that are named here and not built. The server is where a
  stranger first meets a log that is never trimmed, so these belong in
  front of the door, and the store's page says when each is due: \"before a
  stranger can reach the door\" (src-store/README.md, 12 October 2026,
  entry 7). `ration` is the one place where they would be made:

    - a ration of acts and of live reads for each actor, and a block;
    - the keeping of a model's reads (the same page, entry 4): the seam is
      where \"this asker is a model\" would first be known, and nothing
      says yet who is one.

  The bound on a frame is a face's: it is tested before a frame is read,
  and the number is `softland.server.words/max-frame-bytes`.

  The system. `system` gives the actor of the system, for the server's own
  steps: the acts of a first sign-in (`softland.server.sign-in`). No face
  calls it, and the pure proof reads the source to hold that. Who may act
  as the system is not decided (src-store/README.md, 9 October 2026, K9)."
  (:refer-clojure :exclude [read send])
  (:require [missionary.core :as m]
            [softland.server.session.record :as record]
            [softland.server.session.credential :as credential]
            [softland.server.sessions :as sessions]
            [softland.server.watching :as watching]
            [softland.server.words :as words]
            [softland.store.door :as door]
            [softland.store.gate.first-act :as first-act]
            [softland.store.read-path :as read-path])
  (:import [java.io Writer]
           [java.util UUID]
           [java.util.concurrent ExecutorService Executors ScheduledExecutorService
            ScheduledFuture ThreadFactory TimeUnit]
           [missionary Cancelled]))

;; ------------------------------------------------------------- the numbers

(def default-numbers
  "The seam's numbers, each with its default. Each is the builder's, kept
  and not decided, and an opener can give another at `open`.

    :session-ms  how long a session lasts from its beginning: 30 days, for
                 a sitting at a browser and for a run of a program alike"
  {:session-ms (* 30 24 60 60 1000)})

;; ------------------------------------------------------------------ the actor

(deftype Actor [who session hash until]
  Object
  (toString [_] (if (some? who) "#<actor>" "#<visitor>"))
  (equals [_ other]
    (and (instance? Actor other)
         (= who (.-who ^Actor other))
         (= session (.-session ^Actor other))
         (= hash (.-hash ^Actor other))
         (= until (.-until ^Actor other))))
  (hashCode [_]
    (clojure.core/hash [who session hash until])))

(defmethod print-method Actor [^Actor actor ^Writer w]
  (.write w (.toString actor)))

(defmethod print-dup Actor [^Actor actor ^Writer w]
  (.write w (.toString actor)))

(alter-meta! #'->Actor assoc
             :private true
             :doc "Makes an actor: who she is, an id, or nil for a visitor; her session's
  id; the hash of her credential; and the time until which the session
  lasts. `actor-of`, `visitor` and `system` are the callers. It prints as
  the word \"actor\" or \"visitor\" and nothing else. Two actors of one
  session are equal, so a reactive program that is handed her actor a
  second time, as an Electric program is when a standing is told again,
  does none of its work again.")

(def ^:private the-visitor
  "The actor of everybody who is not signed in: no who, no session."
  (->Actor nil nil nil nil))

(defn actor?
  "Takes anything. Gives true for an actor that this namespace made."
  [x]
  (instance? Actor x))

(defn who-of
  "Takes an actor. Gives her id, or nil for a visitor."
  [^Actor actor]
  (.-who actor))

(defn session-of
  "Takes an actor. Gives her session's id, or nil for a visitor."
  [^Actor actor]
  (.-session actor))

(defn about
  "Takes an actor. Gives what a face may tell the system that holds her
  credential: {:who :session :until}, or {:visitor true}. It is data, and
  it is no actor."
  [^Actor actor]
  (if (some? (.-who actor))
    {:who (.-who actor) :session (.-session actor) :until (.-until actor)}
    {:visitor true}))

;; ------------------------------------------------------------------- the seam

(deftype Seam [door read-path sessions numbers system-session executor timer]
  Object
  (toString [_] "#<seam>"))

(defmethod print-method Seam [^Seam seam ^Writer w]
  (.write w (.toString seam)))

(alter-meta! #'->Seam assoc
             :private true
             :doc "Makes an opened seam. `open` is the one caller. It holds the door, the
  read path and the sessions, borrowed; its numbers; the id of the server's
  own session, made at the opening; an executor of virtual threads, on
  which every call of the store and every handing on of a flow runs; and
  one timer thread, for the end of a session whose time passes.")

(defn open
  "Opens the seam. Takes a map: `:door`, an opened door
  (`softland.store.door/open`); `:read-path`, an opened read path
  (`softland.store.read-path/open`); `:sessions`
  (`softland.server.sessions/open`); and, when the opener wants, `:numbers`
  (`default-numbers`). Gives a seam. It makes no call to Rama. The opener
  owns the three things given and closes them; `close!` stops the seam's
  own threads."
  [{:keys [door read-path sessions numbers]}]
  (->Seam door read-path sessions
          (merge default-numbers (select-keys numbers (keys default-numbers)))
          (UUID/randomUUID)
          (Executors/newVirtualThreadPerTaskExecutor)
          (Executors/newSingleThreadScheduledExecutor
           (reify ThreadFactory
             (newThread [_ runnable]
               (doto (Thread. runnable "softland-seam-timer") (.setDaemon true)))))))

(defn close!
  "Stops the seam's executor and its timer. A flow that is still open is
  told nothing; its face cancels it."
  [^Seam seam]
  (.shutdown ^ExecutorService (.-executor seam))
  (.shutdownNow ^ScheduledExecutorService (.-timer seam))
  nil)

(defn- now-ms
  "Reads the clock of the server: milliseconds since 1970. A session's
  `:until` is held against it."
  []
  (System/currentTimeMillis))

(defn- on-its-own-thread
  "Takes the seam and a function of no argument that makes blocking calls.
  Gives a task that runs it on a new virtual thread of the seam's executor
  and completes with what it gave, or fails with what it threw. Cancelling
  the task interrupts that thread; the door and the read path turn an
  interrupt into one of their words. A task that is cancelled before its
  thread began fails as cancelled and runs nothing.

  It is written by hand and not with Missionary's `via`: `via` holds a
  monitor around its body, and a virtual thread that waits while it holds a
  monitor keeps its carrier thread. With `via`, as many waits for an answer
  as the machine has cores would hold every virtual thread of the process
  back. That was seen with the JVM's own trace of pinned threads, on the
  seam's proof."
  [^Seam seam f]
  (fn [success failure]
    (let [state (atom :not-begun)]
      (.execute ^ExecutorService (.-executor seam)
                (fn []
                  (if (compare-and-set! state :not-begun (Thread/currentThread))
                    (let [[outcome got] (try [:gave (f)] (catch Throwable t [:threw t]))]
                      (if (= :gave outcome) (success got) (failure got)))
                    (failure (Cancelled. "The task was cancelled before it began.")))))
      (fn []
        (let [now @state]
          (cond
            (instance? Thread now) (.interrupt ^Thread now)
            (= :not-begun now) (when-not (compare-and-set! state :not-begun :cancelled)
                                 (let [now @state]
                                   (when (instance? Thread now) (.interrupt ^Thread now))))))))))

;; ---------------------------------------------------------------------- who

(defn- actor-of
  "Takes the hash of a credential and a live standing
  (`softland.server.session.record/standing`). Gives her actor."
  [hash standing]
  (->Actor (:who standing) (:session standing) hash (:until standing)))

(defn- no-session
  "Takes a standing that is not live. Gives the word `:find-her-session`
  with why."
  [standing]
  (words/could-not :find-her-session {:why (:state standing)}))

(defn- actor-now
  "The body of `actor`. Takes the seam and a token or nil. Gives an actor
  or a word. One read of the hash's row, and none for a visitor or for a
  text that has not the form of a token."
  [^Seam seam token]
  (if (nil? token)
    the-visitor
    (if-let [hash (credential/hash-of token)]
      (let [[outcome got] (sessions/row-of (.-sessions seam) hash)]
        (if (= :threw outcome)
          (words/could-not :reach-the-sessions got)
          (let [standing (record/standing got (now-ms))]
            (if (= :live (:state standing))
              (actor-of hash standing)
              (no-session standing)))))
      (words/could-not :find-her-session {:why :not-a-token}))))

(defn actor
  "Turns a credential into an actor. Takes the seam and a token, or nil for
  a system that sent none. Gives a task of an actor, or of a word:

    nil                      -> the visitor
    a token of a live row    -> her actor
    a token of no row, of an ended row, or of a row whose time has passed
                             -> `:find-her-session`, with `:why`
                                `:not-known`, `:ended` or `:over`
    a text of another form   -> `:find-her-session`, `:why :not-a-token`
    a read that threw        -> `:reach-the-sessions`

  A credential that stands for nobody is told so and is not made a
  visitor: the system that holds it must learn that it has to sign in
  again. One round trip to the task of the hash, at each call: the seam
  keeps no row between two requests."
  [seam token]
  (on-its-own-thread seam (fn [] (actor-now seam token))))

(defn visitor
  "Gives the actor of everybody who is not signed in."
  []
  the-visitor)

(defn system
  "Gives the actor of the system, for the server's own steps: who is the
  store's `system` id, and the session is the id that this seam made at
  its opening, so each start of the server is one session of the system.
  `softland.server.sign-in` is the one caller; no face calls it."
  [^Seam seam]
  (->Actor first-act/system (.-system-session seam) nil nil))

(defn standing
  "What a credential stands for, over time. Takes the seam and a token.
  Gives a flow of standings (`softland.server.session.record/standing`):

    {:state :not-known}                       nobody has said yes to it
    {:state :live :who :session :until :actor}  with her actor
    {:state :ended}   {:state :over}          the last thing of the flow
    {:state :not-reached}                     the sessions could not be
                                              read; the last thing too
    {:state :not-a-token}                     the only thing, for a text of
                                              another form

  It gives the standing now, then each standing that differs from the one
  before, and ends after one that is final. A program that waits for a
  person's yes holds this flow on a token that stands for nobody yet; a
  socket holds it for as long as it is open. Cancelling it stops the
  listening.

  It registers on the feed first and reads the row after, and takes the
  join of the two, so no change falls between them. Each thing is handed on
  from a thread of the seam's own."
  [^Seam seam token]
  (let [hash (credential/hash-of token)
        final? (fn [standing] (contains? #{:ended :over :not-reached :not-a-token} (:state standing)))
        subject
        (fn [!]
          (if-not hash
            (do (! {:state :not-a-token}) (fn [] nil))
            (let [lock (Object.)
                  state (atom {:row nil :told nil :timer nil :done? false})
                  with-actor (fn [standing]
                               (cond-> standing
                                 (= :live (:state standing)) (assoc :actor (actor-of hash standing))))
                  say! (fn [standing]
                         (when-not (or (:done? @state) (= standing (:told @state)))
                           (swap! state assoc :told standing :done? (final? standing))
                           (! (with-actor standing))))
                  take! (fn take! [thing]
                          (locking lock
                            (if (= sessions/not-reached thing)
                              (say! {:state :not-reached})
                              (let [row (:row (swap! state update :row record/join thing))
                                    now (now-ms)
                                    standing (record/standing row now)]
                                (when (and (= :live (:state standing)) (nil? (:timer @state)))
                                  (swap! state assoc :timer
                                         (.schedule ^ScheduledExecutorService (.-timer seam)
                                                    ^Runnable (fn [] (take! nil))
                                                    (long (max 1 (- (:until standing) now)))
                                                    TimeUnit/MILLISECONDS)))
                                (say! standing)))))
                  stop (sessions/listen! (.-sessions seam) hash take!)]
              (.execute ^ExecutorService (.-executor seam)
                        (fn []
                          (let [[outcome got] (sessions/row-of (.-sessions seam) hash)]
                            (take! (if (= :threw outcome) sessions/not-reached got)))))
              (fn []
                (stop)
                (when-let [timer (:timer @state)]
                  (.cancel ^ScheduledFuture timer false))))))]
    (m/eduction
     (watching/through final?)
     (m/ap (let [standing (m/?> (m/relieve (fn [_ newer] newer) (m/observe subject)))]
             (m/? (on-its-own-thread seam (fn [] standing))))))))

(defn- begin-now
  "The body of `begin`. One append, then one read of the row."
  [^Seam seam who hash]
  (if-not (record/hash-text? hash)
    (words/could-not :begin-her-session {:why :not-a-hash})
    (let [now (now-ms)
          session (UUID/randomUUID)
          until (+ now (long (:session-ms (.-numbers seam))))
          [outcome threw] (sessions/begin! (.-sessions seam) (record/began hash who session until now))]
      (if (= :threw outcome)
        (words/could-not :begin-her-session (assoc threw :why :append-threw))
        (let [[outcome row] (sessions/row-of (.-sessions seam) hash)]
          (cond
            (= :threw outcome) (words/could-not :reach-the-sessions row)
            (and (= :live (:state row)) (= session (:session row))) {:who who :session session :until until}
            :else (words/could-not :begin-her-session {:why :credential-in-use})))))))

(defn begin
  "Begins a session: from now on the credential stands for a who. Takes the
  seam, the who, and the hash of the credential. Gives a task of
  {:who :session :until}, or of a word: `:begin-her-session` with `:why
  :credential-in-use` when the hash already has a row, since a hash is
  bound once; or what Rama threw.

  The caller has proven who it is for: a face that checked Google's token
  (`softland.server.sign-in`), or a signed-in person saying yes to a
  program's hash, where the who is her own actor's. The seam cannot test
  that, and takes the who as given."
  [seam who hash]
  (on-its-own-thread seam (fn [] (begin-now seam who hash))))

(defn end
  "Ends the session of a credential. Takes the seam and the hash of the
  credential. Gives a task of [:ok], or of the word `:reach-the-sessions`.
  Every flow that rests on the hash, in every copy of the server, is told.
  What must hold: the caller holds the credential itself, as a face does
  that read it from a cookie at a sign-out. For a hash that somebody only
  knows, `refuse`."
  [^Seam seam hash]
  (on-its-own-thread
   seam
   (fn []
     (if-not (record/hash-text? hash)
       [:ok]
       (let [[outcome threw] (sessions/end! (.-sessions seam) (record/ended hash (now-ms)))]
         (if (= :threw outcome)
           (words/could-not :reach-the-sessions threw)
           [:ok]))))))

(defn sign-out
  "Ends an actor's own session. Takes the seam and an actor. Gives a task of
  [:ok], or of a word: `:find-her-session` for a visitor, who has none. The
  actor was made from the credential itself, so who says this holds it."
  [^Seam seam ^Actor actor]
  (if-let [hash (.-hash actor)]
    (end seam hash)
    (m/sp (words/could-not :find-her-session {:why :no-credential}))))

(defn refuse
  "Says no to a credential, as a signed-in person. Takes the seam, her
  actor, and the hash of the credential. Gives a task of [:ok], or of a
  word. A hash that stands for nobody is refused for good; a hash that
  stands for her is ended, which signs that program out. A hash that
  stands for somebody else is left as it is, with `:find-her-session` and
  `:why :not-hers`: a hash is shown on a link, so knowing one must not be
  enough to end another person's session."
  [^Seam seam ^Actor actor hash]
  (on-its-own-thread
   seam
   (fn []
     (if-not (record/hash-text? hash)
       [:ok]
       (let [[outcome row] (sessions/row-of (.-sessions seam) hash)]
         (cond
           (= :threw outcome) (words/could-not :reach-the-sessions row)
           (and (some? (:who row)) (not= (:who row) (.-who actor)))
           (words/could-not :find-her-session {:why :not-hers})
           :else
           (let [[outcome threw] (sessions/end! (.-sessions seam) (record/ended hash (now-ms)))]
             (if (= :threw outcome)
               (words/could-not :reach-the-sessions threw)
               [:ok]))))))))

;; ------------------------------------------------- what the seam adds, and holds

(defn- needs-a-who
  "Takes an actor. Gives nil for one that has a who and whose session's
  time has not passed, or the word `:find-her-session`: for a visitor, and
  for an actor that a face held past its time. It reads the server's clock
  and nothing else. It does not read her row again: a face that holds an
  actor for longer than a request holds `standing` beside it, and lets the
  actor go when that ends."
  [^Actor actor]
  (cond
    (nil? (.-who actor)) (words/could-not :find-her-session {:why :no-credential})
    (and (.-until actor) (<= (long (.-until actor)) (now-ms))) (no-session {:state :over})))

(defn- holds-a-part
  "Takes the name of a form, `:act` or `:query`, and a thing. Gives nil, or
  the word `:read-the-word` when the thing is no plain map or already holds
  a part that the seam writes. A face tests this first
  (`softland.server.words/check`); this is the test again, at the one place
  where the part is written."
  [form thing]
  (cond
    (not (and (map? thing) (not (record? thing))))
    (words/could-not :read-the-word {:why :not-a-plain-map :at form})

    :else
    (when-let [held (some (fn [k] (when (contains? thing k) k)) (get words/server-parts form))]
      (words/could-not :read-the-word {:why :holds-a-part-that-the-server-writes :at held}))))

(defn- as-maker
  "Takes an actor that has a who and an act without `:who` and `:session`.
  Gives the maker's act: the same map with the two parts, the actor's."
  [^Actor actor act]
  (assoc act :who (.-who actor) :session (.-session actor)))

(defn- as-asker
  "Takes an actor and a query without its asker. Gives the query with
  `:asker`, her id, or with `:not-signed-in true` for a visitor."
  [^Actor actor query]
  (if (some? (.-who actor))
    (assoc query :asker (.-who actor))
    (assoc query :not-signed-in true)))

(defn- ration
  "The one place where a sender is paced and a block is held, and where a
  model's reads would be kept. Takes the seam, the actor and what she is
  about to do, `:act` or `:read`. Gives nil: nothing is built. What is due
  here, and when, is in the namespace docstring."
  [_seam _actor _doing]
  nil)

;; ---------------------------------------------------------------- the tasks

(defn act-ids
  "Gives act-ids of one layer, ahead of sending. Takes the seam, an actor, a
  layer and a count. Gives a task of what
  `softland.store.door/act-ids-for` gives: a vector of that many act-ids,
  or one of the door's words; or `:find-her-session` for a visitor. A maker
  that must not do an act twice asks for its act-id first and keeps it: a
  second send under the same act-id is the same act."
  [^Seam seam actor layer n]
  (on-its-own-thread
   seam
   (fn []
     (or (needs-a-who actor)
         (door/act-ids-for (.-door seam) layer n)))))

(defn- send-now
  "Sends one act as the actor: the one call of the door's `send-act` in the
  server. Takes the seam, an actor that has a who, an act without `:who`
  and `:session`, and an act-id or nil. Gives the door's receipt or word,
  or the seam's word for a thing to which it cannot add its parts."
  [^Seam seam actor act act-id]
  (or (holds-a-part :act act)
      (ration seam actor :act)
      (door/send-act (.-door seam) (as-maker actor act) act-id)))

(defn send
  "Sends one act. Takes the seam; an actor; the act, in the store's one
  form without `:who` and `:session`; and the act-id that the maker asked
  for ahead, or nil. Gives a task of what `softland.store.door/send-act`
  gives: the receipt {:act-id :digest :ack}, or one of the door's words; or
  `:find-her-session` for a visitor.

  The seam checks nothing of the act's form, as the door checks nothing: a
  gate refuses a wrong act under its act-id, and the log keeps it. The
  receipt is a signal and never the answer; the maker reads the answer with
  `answer`."
  [seam actor act act-id]
  (on-its-own-thread
   seam
   (fn []
     (or (needs-a-who actor)
         (send-now seam actor act act-id)))))

(defn send-all
  "Sends the acts of one maker in order, with no round trip between two of
  them for the system that sent them. Takes the seam, an actor and a vector
  of [act-id act] pairs. Gives a task of a vector with one result for each
  act that was tried, in order: a receipt, or, as its last thing, the word
  with which a send ended. No act after that one is tried, since a later
  act of a series can rest on an earlier one; the maker sends the rest
  again under the same act-ids.

  The door returns from one append before the next begins, so the acts
  reach a layer's depot in this order (`softland.store.door/send-act`,
  \"The order of two sends\")."
  [seam actor acts]
  (on-its-own-thread
   seam
   (fn []
     (or (needs-a-who actor)
         (reduce (fn [results [act-id act]]
                   (let [result (send-now seam actor act act-id)]
                     (if (:could-not result)
                       (reduced (conj results result))
                       (conj results result))))
                 []
                 acts)))))

(defn answer
  "Reads the answer of an act back. Takes the seam, an actor, an act-id,
  the digest that the receipt held, and a wait in milliseconds, 0 for one
  read. Gives a task of what `softland.store.door/answer-of` gives: one of
  the four answers, or the door's word; or `:find-her-session` for a
  visitor. Who holds the act-id and the digest of a thing reads its
  answer; the seam asks nothing more of the actor than a who."
  [^Seam seam actor act-id digest wait-ms]
  (on-its-own-thread
   seam
   (fn []
     (or (needs-a-who actor)
         (door/answer-of (.-door seam) act-id digest wait-ms)))))

(defn read
  "Reads once. Takes the seam, an actor, and a query in the read path's
  form without `:asker` and `:not-signed-in`. Gives a task of what
  `softland.store.read-path/read-once` gives: an answer, or one of the read
  path's words. A visitor reads as somebody who is not signed in."
  [^Seam seam actor query]
  (on-its-own-thread
   seam
   (fn []
     (or (holds-a-part :query query)
         (ration seam actor :read)
         (read-path/read-once (.-read-path seam) (as-asker actor query))))))

;; ---------------------------------------------------------------- the flow

(defn watch
  "Reads, and keeps the read live. Takes the seam, an actor, and a query
  without its asker. Gives a flow of the read path's own messages
  (`softland.store.read-path/read-live`):

    {:anew answer}                    first, and whenever what shapes the
                                      read changed
    {:changed {place place-answer}}   the places that changed, each with
                                      what the read gives there now
    {:ended word}                     once, as the last thing

  The read is opened when the flow is first listened to, on a thread of the
  seam's own, and left when the flow is cancelled or has ended. A read that
  could not be opened gives its word as `{:ended word}` and nothing else.
  Before it opens the read, the seam listens on the feed and then reads her
  row once more: one round trip, so that a session which ended after her
  actor was made opens no read.

  What ends it: the read path ended the read, with its word; her session
  ended or its time passed, with `:find-her-session`; the sessions could
  not be read after the feed missed changes, with `:reach-the-sessions`.
  A visitor's read ends only by the read path or by the face.

  A slow reader. What waits for her is merged by place
  (`softland.server.watching/merged`): she is handed fewer and larger
  things, in order, and never a present that is wrong. Each thing is handed
  on from a thread of the seam's own: the thread of Rama's client that
  brought a change has returned by then. What a face may do while it is
  handed a thing is in the namespace docstring, \"One limit on a face\"."
  [^Seam seam ^Actor actor query]
  (let [subject
        (fn [!]
          (let [lock (Object.)
                state (atom {:left? false :handle nil :ended? false :timer nil :stop nil})
                rp (.-read-path seam)
                tell! (fn [message]
                        ;; One call of `!` at a time, and none after an end.
                        (locking lock
                          (when-not (:ended? @state)
                            (when (contains? message :ended)
                              (swap! state assoc :ended? true))
                            (! message))))
                leave! (fn []
                         (let [{:keys [handle stop timer]} (swap! state assoc :left? true)]
                           (when stop (stop))
                           (when timer (.cancel ^ScheduledFuture timer false))
                           (when handle (read-path/leave rp handle))))
                end-with! (fn [word]
                            (tell! {:ended word})
                            (leave!))
                fault (or (holds-a-part :query query) (ration seam actor :read))]
            (if fault
              (tell! {:ended fault})
              (do
                (when-let [hash (.-hash actor)]
                  (swap! state assoc
                         :stop (sessions/listen!
                                (.-sessions seam) hash
                                (fn [thing]
                                  (if (= sessions/not-reached thing)
                                    (end-with! (words/could-not :reach-the-sessions nil))
                                    (let [standing (record/standing thing (now-ms))]
                                      (when-not (= :live (:state standing))
                                        (end-with! (no-session standing)))))))
                         :timer (.schedule ^ScheduledExecutorService (.-timer seam)
                                           ^Runnable (fn [] (end-with! (no-session {:state :over})))
                                           (long (max 1 (- (.-until actor) (now-ms))))
                                           TimeUnit/MILLISECONDS)))
                (.execute ^ExecutorService (.-executor seam)
                          (fn []
                            ;; Her session can have ended between the making
                            ;; of the actor and the listening above, and the
                            ;; feed does not tell a change twice. So the row
                            ;; is read once more, after the listening began.
                            (let [gone (when-let [hash (.-hash actor)]
                                         (let [[outcome row] (sessions/row-of (.-sessions seam) hash)
                                               standing (record/standing row (now-ms))]
                                           (cond
                                             (= :threw outcome) (words/could-not :reach-the-sessions row)
                                             (not= :live (:state standing)) (no-session standing))))
                                  got (or gone (read-path/read-live rp (as-asker actor query) tell!))]
                              (if (:could-not got)
                                (end-with! got)
                                (let [handle (:leave-with got)]
                                  ;; The flow can be left while the read opens.
                                  (when (:left? (swap! state assoc :handle handle))
                                    (read-path/leave rp handle)))))))))
            leave!))]
    (m/eduction
     (watching/through (fn [message] (contains? message :ended)))
     (m/ap (let [waits (m/?> (m/relieve watching/merged (m/observe subject)))
                 waits (m/? (on-its-own-thread seam (fn [] waits)))]
             (m/?> (m/seed (watching/messages waits))))))))
