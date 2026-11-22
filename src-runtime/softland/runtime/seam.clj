(ns softland.runtime.seam
  "The seam: the one place that every face of the runtime stands on. It
  turns a credential into a caller, holds each thing that a caller sends
  against who that caller is, and puts the store's door, the store's read
  path and Auth behind the words of the wire. It is Clojure and Missionary,
  and it requires no Electric.

  A face is whatever speaks to one kind of system: the line, for everything
  that speaks the words (`softland.runtime.line`), and the pages by which a
  person signs in (`softland.runtime.http`). A face proves nothing. It
  hands the seam a credential and gets a caller, and each other call takes
  the caller first.

  The calls, each of which but `open` takes the seam first:

    open       the door, the read path, Auth's client, the standing, and
               numbers -> a seam
    caller     what came with a request -> a task of a caller, or of one of
               the runtime's words
    visitor    nothing more -> the caller of everybody who is not signed in
    about      a caller -> what a face may tell the system that holds the
               credential, as data
    call       a caller and a word in its one form -> a task of the word's
               result
    watch      a caller and a query -> a flow: the whole answer, then the
               changes by place, then one end
    standing   a caller -> a flow that says when the caller stops being
               live, and why

  The seam's contract is data. A word goes in as the wire has it
  (`softland.wire.words`) and a result comes out as plain data: the door's
  receipt and answer, the read path's answer, Auth's result, or a word that
  holds `:could-not`. So where a face runs is no wall: a face in another
  process speaks the same words over the line.

  Nobody acts for another (Sid, 20 November 2026: \"so 1-A it is\"). The
  whole act arrives, with its `:who` and its `:session` (\"3A\"). The seam
  writes neither. It holds them against the caller: an act whose `:who` is
  not the caller's who, or whose `:session` is not the caller's session, is
  refused with `:send-as-another`, and nothing is sent for it. The same
  holds for a query's `:asker`. That the runtime refuses, and does not
  write the try into a log, is a session's choice: an act under a who that
  did not send it would be a false line in a log that is never trimmed.

  The caller. A caller is an object that only this namespace makes. It
  holds the actor's who, the person at the root, the sign-in, the session,
  the sign-in's kind, the id of the root's own layer, and what the face
  said of the request. It prints as the word and nothing else, and it is no
  data: a who that crosses the wire back cannot become a caller.

  How a credential becomes a caller. One read of Auth, `who-is`, on the
  root's task (`softland.wire.credential` says how a credential names its
  root). The sign-in must be there and not ended, its actor not ended, its
  secret's time not passed on this process's clock, and the secret's hash
  the sign-in's hash now, or the one before it while that still stands.
  A browser's sign-in is taken from a cookie alone, and a program's from a
  header alone: a program that holds its own secret cannot say a person's
  yes (the review of 20 November 2026 ran that it could). No row is kept
  between two requests.

  The session. A face that will send acts names the session that it is in,
  or asks for a new one. A new session's id is made here at random and
  opened in Auth; a named one must be open under this sign-in, with its
  time not passed. A session has a time of its own, and the seam moves it
  on while the session is in use: when a caller is made in it, and while a
  connection holds it. The session outlives the secret: `:replace-secret`
  changes no session, so an act that is sent again after a break, under the
  same UUID, has the same digest and is taken once.

  No system chooses an id. The id of a session is made here at random. The
  id of an agent, of its sign-in, and of a person who is invited is derived
  here, by a hash, from the caller's root and a UUID that the system sent
  (`derived`): the same UUID again gives the same id, so a word that is
  said twice is one making; and no UUID that a system can pick gives the id
  of a thing that is not its own. The UUID is kept nowhere: not by the
  runtime, not in Auth and in no act. So a derived id cannot be worked back
  to the root that it was made from, and it says as little as a random one
  (src-store/README.md, 9 October 2026, entry 1: nothing that is written
  plain in an act is made from the person). Auth trusts the runtime for that (its
  spec, \"The client's contract\"): an actor's id is the who of its acts,
  so a system that could choose one could become somebody else.

  The act-id. A maker makes the random part, a UUID, and sends it with the
  act. The seam asks the door for an act-id of the act's layer, which reads
  the layer's depot, and puts the maker's UUID in its place
  (`softland.store.door/act-id-for`). The door then takes the act-id as
  given. So the maker needs no knowledge of a layer's depot, and a repeat
  is the same thing.

  A caller that stops being live. `standing` listens for the caller's root
  (`softland.runtime.standing`) and holds a timer for the secret's time.
  Whatever it is told, it reads Auth before it says an end: a push can be
  behind a write. A sign-in or a session that ended, through this copy of
  the runtime or another, ends the flow with the word, and the face ends
  what rests on it.

  The thread rule. A function that the read path calls for a change runs on
  a thread of Rama's client, and one that stays there holds back the reads
  of others. What such a thread does here is put the change with what
  waits and hand it to a thread of the seam's own. What waits for a slow
  reader is merged by place (`softland.wire.watching`).

  State. The seam owns nothing that lasts. It borrows the door, the read
  path, Auth's client and the standing. In the memory of the process it
  holds its numbers, an executor of virtual threads and one timer thread.
  It keeps no row, no answer and no caller between two calls.

  The duties that are named here and not built. `ration` is the one place
  where a sender would be paced and a block held; it is handed the caller,
  with what the face said of the request, and what the caller is about to
  do. The keeping of reads is the trail's (the runtime's page, piece 5):
  `call` and `watch` write nothing of a read today.

  The system. `softland.runtime.sign-in` sends the acts that only the
  system sends: the making of a person's own layer, and the first acts of a
  deployment. It calls `send-as-system` here, the one way in, and no face
  can reach that call with a thing that a system sent. Who may act as the
  system is not decided (src-store/README.md, 9 October 2026, K9)."
  (:refer-clojure :exclude [read send])
  (:require [clojure.string :as str]
            [missionary.core :as m]
            [softland.auth.client :as auth]
            [softland.runtime.standing :as standing]
            [softland.store.door :as door]
            [softland.store.gate.first-act :as first-act]
            [softland.store.read-path :as read-path]
            [softland.wire.credential :as credential]
            [softland.wire.watching :as watching]
            [softland.wire.words :as words])
  (:import [java.io Writer]
           [java.util Locale UUID]
           [java.util.concurrent ExecutorService Executors ScheduledExecutorService
            ScheduledFuture ScheduledThreadPoolExecutor ThreadFactory TimeUnit]
           [java.util.concurrent.locks ReentrantLock]))

;; ------------------------------------------------------------- the numbers

(def default-numbers
  "The seam's numbers, each with its default. Each is the builder's, kept
  and not decided (marked for Sid to turn), and an opener can give another
  at `open`.

    :secret-ms    how long a secret stands from its making or its
                  replacing: 7 days. A system that is in use replaces its
                  secret before then; a sign-in whose secret's time passes
                  is over
    :previous-ms  how long the secret before stands after a replacing: one
                  minute, for the requests that were on their way
    :session-ms   how long a session stands from its opening, and from each
                  time that the runtime moves its time on: one day. A
                  session that is in use is moved on when less than half of
                  that is left; one that nobody uses for a day is over
    :sign-in-window-ms
                  the sliding window, in milliseconds, over which posts to
                  `/sign-in/*` from one remote address are counted: 10 seconds
    :max-sign-in-posts-per-address
                  the most posts to `/sign-in/*` from one remote address in
                  `:sign-in-window-ms` before further posts are refused with
                  429 before reaching Google or Auth: 30
    :max-watches-per-root
                  the most live watches one root (a person and every agent
                  beneath her, across all connections) may hold open at once
                  in one copy of the runtime: 256
    :max-visitor-watches
                  the most live watches all visitors together may hold open
                  at once in one copy of the runtime: 64"
  {:secret-ms (* 7 24 60 60 1000)
   :previous-ms 60000
   :session-ms (* 24 60 60 1000)
   :sign-in-window-ms 10000
   :max-sign-in-posts-per-address 30
   :max-watches-per-root 256
   :max-visitor-watches 64})

;; ------------------------------------------------------------------ the caller

(deftype Caller [who root sign-in session kind hash until own-layer driven-by from]
  Object
  (toString [_] (if (some? who) "#<caller>" "#<visitor>")))

(defmethod print-method Caller [^Caller caller ^Writer w]
  (.write w (.toString caller)))

(defmethod print-dup Caller [^Caller caller ^Writer w]
  (.write w (.toString caller)))

(alter-meta! #'->Caller assoc
             :private true
             :doc "Makes a caller: the actor's who, or nil for a visitor; the person at the
  root; the sign-in's id; the session's id, or nil when the face named and
  asked for none; the sign-in's kind; an atom holding the latest hash of the
  secret for this connection (updated on `:replace-secret`); the time until
  which that secret stands; the id of the root's own layer; what drives the
  session (`:driven-by` from `who-is`, or nil); and what the face said of
  the request. `caller-now` and `visitor` are the callers. It prints as the
  word \"caller\" or \"visitor\" and nothing else.")

(defn caller?
  "Takes anything. Gives true for a caller that this namespace made."
  [x]
  (instance? Caller x))

(defn who-of
  "Takes a caller. Gives the actor's id, or nil for a visitor."
  [^Caller caller]
  (.-who caller))

(defn root-of
  "Takes a caller. Gives the id of the person at the root, or nil for a
  visitor."
  [^Caller caller]
  (.-root caller))

(defn session-of
  "Takes a caller. Gives the session's id, or nil."
  [^Caller caller]
  (.-session caller))

(defn own-layer-of
  "Takes a caller. Gives the id of the root's own layer, or nil."
  [^Caller caller]
  (.-own-layer caller))

(defn person?
  "Takes a caller. Gives true when the caller is a person herself: her who
  is her root."
  [^Caller caller]
  (and (some? (.-who caller)) (= (.-who caller) (.-root caller))))

(defn about
  "Takes a caller. Gives what a face may tell the system that holds the
  credential: {:who :root :sign-in :session :kind :own-layer :until}, plus
  `:driven-by` (from `who-is`, nil when not said) when the caller is in a
  session; or {:visitor true}. It is data, and it is no caller."
  [^Caller caller]
  (if (some? (.-who caller))
    (cond-> {:who (.-who caller) :root (.-root caller) :sign-in (.-sign-in caller)
             :session (.-session caller) :kind (.-kind caller)
             :own-layer (.-own-layer caller) :until (.-until caller)}
      (some? (.-session caller)) (assoc :driven-by (.-driven-by caller)))
    {:visitor true}))

;; ------------------------------------------------------------------- the seam

(deftype Seam [door read-path client standing numbers ration-state executor timer]
  Object
  (toString [_] "#<seam>"))

(defmethod print-method Seam [^Seam seam ^Writer w]
  (.write w (.toString seam)))

(alter-meta! #'->Seam assoc
             :private true
             :doc "Makes an opened seam. `open` is the one caller. It holds the door, the
  read path, Auth's client and the standing, borrowed; its numbers; its
  in-memory copy-local ration state (`:sign-in` per-address timestamps and
  `:watches` live counts by root and for visitors); an executor of virtual
  threads, on which every call of the store and of Auth and every handing on
  of a flow runs; and one timer thread, for a secret whose time passes.")

(defn open
  "Opens the seam. Takes a map: `:door`, an opened door
  (`softland.store.door/open`); `:read-path`, an opened read path
  (`softland.store.read-path/open`); `:auth`, Auth's client
  (`softland.auth.client/open`); `:standing`
  (`softland.runtime.standing/open`); and, when the opener wants,
  `:numbers` (`default-numbers`). Gives a seam. It makes no call to Rama.
  The opener owns the four things given and closes them; `close!` stops the
  seam's own threads."
  [{:keys [door read-path auth standing numbers]}]
  (->Seam door read-path auth standing
          (merge default-numbers (select-keys numbers (keys default-numbers)))
          {:sign-in (atom {}) :watches (atom {})}
          (Executors/newVirtualThreadPerTaskExecutor)
          ;; A timer that is cancelled leaves the queue at once: a timer of a
          ;; standing can be set hours ahead, and a cancelled one that stayed
          ;; queued until its moment would hold its connection's memory.
          (doto (ScheduledThreadPoolExecutor.
                 1
                 (reify ThreadFactory
                   (newThread [_ runnable]
                     (doto (Thread. runnable "softland-seam-timer") (.setDaemon true)))))
            (.setRemoveOnCancelPolicy true))))

(defn close!
  "Stops the seam's executor and its timer. A flow that is still open is
  told nothing; its face cancels it."
  [^Seam seam]
  (.shutdown ^ExecutorService (.-executor seam))
  (.shutdownNow ^ScheduledExecutorService (.-timer seam))
  nil)

(defn now-ms
  "Reads the clock of this process: milliseconds since 1970. It is the time
  that the runtime writes on each event of Auth, and the time that a
  secret's end is held against."
  []
  (System/currentTimeMillis))

(defn numbers-of
  "Takes the seam. Gives its numbers."
  [^Seam seam]
  (.-numbers seam))

(defn auth-of
  "Takes the seam. Gives Auth's client, for the one namespace beside the
  seam that writes what sign-in writes (`softland.runtime.sign-in`)."
  [^Seam seam]
  (.-client seam))

(defn- on-its-own-thread
  "Takes the seam and a function of no argument that makes blocking calls.
  Gives a task that runs it on a new virtual thread of the seam's executor:
  Missionary's `via`. Cancelling the task interrupts that thread while the
  function runs; the door, the read path and Auth's client turn an
  interrupt into one of their words."
  [^Seam seam f]
  (m/via (.-executor seam) (f)))

;; ---------------------------------------------------------------------- who

(def ^:private the-visitor
  "The caller of everybody who is not signed in."
  (->Caller nil nil nil nil nil nil nil nil nil nil))

(defn visitor
  "Gives the caller of everybody who is not signed in. Takes, when a face
  has it, what the face says of the request."
  ([] the-visitor)
  ([from] (->Caller nil nil nil nil nil nil nil nil nil from)))

(defn- no-sign-in
  "Takes why. Gives the word `:find-her-sign-in`."
  [why]
  (words/could-not :find-her-sign-in {:why why}))

(defn- proves?
  "Takes a sign-in as `who-is` gave it, the hash of the secret that came,
  the carrier that it came in (`:header` or `:cookie`) and the time now.
  Gives true when the secret is the sign-in's: its hash is the sign-in's
  hash now, or the one before it while that still stands; and the carrier
  is the one of the sign-in's kind, a cookie for a browser's and a header
  for a program's."
  [{:keys [kind previous] :as sign-in} hash carrier now]
  (and (some? sign-in)
       (= kind (case carrier :cookie :browser :header :program nil))
       (or (credential/same-hash? hash (:hash sign-in))
           (and (some? previous)
                (credential/same-hash? hash (:hash previous))
                (< now (long (:until previous)))))))

(defn- lasting
  "Takes a sign-in as `who-is` gave it, or nil, and the time now. Gives
  [:live until], or [:not why], with why one of `:not-known`, `:ended` and
  `:over`. It reads no secret: it says whether the sign-in stands."
  [{:keys [until ended? actor-ended?] :as sign-in} now]
  (cond
    (nil? sign-in) [:not :not-known]
    (or ended? actor-ended?) [:not :ended]
    (<= (long until) now) [:not :over]
    :else [:live (long until)]))

(defn- judged
  "Takes what `who-is` gave, the hash of the secret that came, the carrier
  that it came in and the time now. Gives [:live until], or [:not why].

  A secret that is not the sign-in's, and a sign-in that came in the wrong
  carrier, are told as `:not-known`: whoever holds a wrong secret learns
  nothing of the sign-in."
  [{:keys [sign-in]} hash carrier now]
  (if (proves? sign-in hash carrier now)
    (lasting sign-in now)
    [:not :not-known]))

(defn- session-fault
  "Takes what `who-is` gave, the sign-in's id, and the time now. Gives nil
  when the session that was asked about is open under the sign-in and its
  time has not passed, or the word `:find-her-session`."
  [{:keys [session]} sign-in now]
  (cond
    (nil? session) (words/could-not :find-her-session {:why :not-known})
    (not= sign-in (:sign-in session)) (words/could-not :find-her-session {:why :not-hers})
    (or (:closed? session) (<= (long (:until session)) now)) (words/could-not :find-her-session {:why :closed})))

(defn- moved-on!
  "Moves a session's time on when less than half of `:session-ms` is left
  of it. Takes the seam, the root, the sign-in, the session's id and the
  time until which it stands now. One write of Auth when it is due, and
  none otherwise. Gives the time that it held, written or not: a repeat of
  `open-session` gives `{:open S}` even on a closed session, so the next
  look must read the session's real standing from Auth."
  [^Seam seam root sign-in session until]
  (let [now (now-ms)
        span (long (:session-ms (.-numbers seam)))]
    (when (< (* 2 (- (long until) now)) span)
      (auth/open-session (.-client seam) {:root root :sign-in sign-in :session session
                                          :until (+ now span) :at now}))
    until))

(defn- caller-now
  "The body of `caller`. One read of Auth; and, for a face that asked for a
  new session, one write and one `who-is` read so `:driven-by` comes from
  Auth."
  [^Seam seam {:keys [credential carrier session new-session? driven-by from]}]
  (if-let [{:keys [root sign-in secret]} (credential/parts credential)]
    (let [client (.-client seam)
          got (auth/who-is client {:root root :sign-in sign-in :session session})]
      (if (:could-not got)
        (words/could-not :reach-auth {:got got})
        (let [hash (credential/hash-of secret)
              [verdict until-or-why] (judged got hash carrier (now-ms))
              {:keys [actor kind]} (:sign-in got)
              made (fn [session session-driven-by]
                     (->Caller actor root sign-in session kind (atom hash) until-or-why
                               (:own-layer got) session-driven-by from))]
          (cond
            (= :not verdict) (no-sign-in until-or-why)
            (some? session) (or (session-fault got sign-in (now-ms))
                                (do (moved-on! seam root sign-in session (:until (:session got)))
                                    (made session (get-in got [:session :driven-by]))))
            new-session?
            (let [session (UUID/randomUUID)
                  now (now-ms)
                  opened (auth/open-session client
                                            (cond-> {:root root :sign-in sign-in :session session
                                                     :until (+ now (long (:session-ms (.-numbers seam))))
                                                     :at now}
                                              (some? driven-by) (assoc :driven-by driven-by)))]
              (cond
                (:could-not opened) (words/could-not :reach-auth {:got opened})
                (:refused opened) (no-sign-in :ended)
                :else (let [after (auth/who-is client {:root root :sign-in sign-in :session session})]
                        (if (:could-not after)
                          (words/could-not :reach-auth {:got after})
                          (made session (get-in after [:session :driven-by]))))))
            :else (made nil nil)))))
    (no-sign-in :not-a-credential)))

(defn caller
  "Turns what came with a request into a caller. Takes the seam and a map:

    :credential    the credential's text
    :carrier       `:header` or `:cookie`: where the face found it
    :session       the id of the session that the system named, or nil
    :new-session?  whether to open a new session when none was named
    :from          what the face says of the request, for `ration`

  Gives a task of a caller, or of a word:

    a live sign-in, in its own carrier      -> her caller
    no such sign-in, a wrong secret, or the wrong carrier
                                            -> `:find-her-sign-in`,
                                               `:why :not-known`
    an ended sign-in or actor               -> the same, `:why :ended`
    a secret whose time has passed          -> the same, `:why :over`
    a text of another form                  -> the same,
                                               `:why :not-a-credential`
    a session that is not open under it     -> `:find-her-session`
    a call of Auth that failed              -> `:reach-auth`

  A credential that stands for nobody is told so and is not made a
  visitor: the system that holds it must learn that it has to sign in
  again. A face that got no credential at all asks for `visitor`."
  [seam request]
  (on-its-own-thread seam (fn [] (caller-now seam request))))

;; ------------------------------------------------- what the seam holds, and adds

(defn- needs-a-who
  "Takes a caller. Gives nil for one that has a who, or the word
  `:find-her-sign-in` for a visitor. It reads nothing: a caller was live
  when it was made, and a face that holds one for longer than a request
  holds `standing` beside it and ends what rests on the caller when that
  says so. The time that a caller was made with is not held against it
  here, since a replacing of the secret moves it."
  [^Caller caller]
  (when (nil? (.-who caller))
    (no-sign-in :no-credential)))

(defn- needs-a-session
  "Takes a caller. Gives nil for one that is in a session, or the word
  `:find-her-session`: an act names its session, so a caller that is in
  none sends no act."
  [^Caller caller]
  (when (nil? (.-session caller))
    (words/could-not :find-her-session {:why :not-known})))

(defn- needs-the-person
  "Takes a caller. Gives nil for a person herself, or the word
  `:do-that-as-an-agent`."
  [caller]
  (when-not (person? caller)
    (words/could-not :do-that-as-an-agent nil)))

(defn- plain-map?
  "True for a map that is no record."
  [x]
  (and (map? x) (not (record? x))))

(defn- act-fault
  "Holds an act against its caller. Takes a caller that has a who and a
  session, and the act as it arrived. Gives nil, or a word: the thing is no
  plain map; or its `:who` or its `:session` is not the caller's."
  [^Caller caller act]
  (cond
    (not (plain-map? act)) (words/could-not :read-the-word {:why :not-a-plain-map :at :act})
    (not= (.-who caller) (get act :who)) (words/could-not :send-as-another {:at :who})
    (not= (.-session caller) (get act :session)) (words/could-not :send-as-another {:at :session})))

(defn- query-fault
  "Holds a query against its caller. Takes a caller and the query as it
  arrived. Gives nil, or a word: the thing is no plain map; a signed-in
  caller's query names another asker than her who, or none; a visitor's
  query names an asker; or the query names another session than the
  caller's."
  [^Caller caller query]
  (cond
    (not (plain-map? query)) (words/could-not :read-the-word {:why :not-a-plain-map :at :query})
    (some? (.-who caller))
    (cond
      (not (and (= (.-who caller) (get query :asker))
                (not (contains? query :not-signed-in))))
      (words/could-not :send-as-another {:at :asker})

      (and (some? (get query :session)) (not= (.-session caller) (get query :session)))
      (words/could-not :send-as-another {:at :session}))
    (contains? query :asker) (words/could-not :send-as-another {:at :asker})
    (some? (get query :session)) (words/could-not :send-as-another {:at :session})))

(def ^:private max-paced-addresses
  "Upper bound on remote addresses tracked concurrently in one copy's
  sign-in pacer so a distributed flood cannot grow memory without bound."
  10000)

(defn ration-sign-in!
  "First-layer ration in this copy's memory: paces posts to `/sign-in/*` by
  remote address over a sliding window (`:sign-in-window-ms`,
  `:max-sign-in-posts-per-address`) before any call to Google or Auth. Gives
  nil when allowed, or the word `:take-more` (`:why :sign-in-paced`)."
  [^Seam seam address]
  (let [numbers (.-numbers seam)
        limit (long (:max-sign-in-posts-per-address numbers))
        window-ns (* 1000000 (long (:sign-in-window-ms numbers)))
        addr (let [s (str address)] (if (seq s) s "unknown"))
        now (System/nanoTime)
        cutoff (unchecked-subtract now window-ns)
        recent? (fn [ts] (pos? (unchecked-subtract (long ts) cutoff)))
        allowed? (volatile! false)
        sign-in-atom (:sign-in (.-ration-state seam))]
    (swap! sign-in-atom
           (fn [m]
             (let [m (if (>= (count m) max-paced-addresses)
                       (let [pruned (into {}
                                          (keep (fn [[k v]]
                                                  (let [kept (filterv recent? v)]
                                                    (when (seq kept) [k kept]))))
                                          m)]
                         (if (and (>= (count pruned) max-paced-addresses) (not (contains? pruned addr)))
                           (let [oldest (key (apply min-key (fn [[_ v]] (peek v)) pruned))]
                             (dissoc pruned oldest))
                           pruned))
                       m)
                   kept (filterv recent? (get m addr []))]
               (if (< (count kept) limit)
                 (do (vreset! allowed? true)
                     (assoc m addr (conj kept now)))
                 (do (vreset! allowed? false)
                     (if (seq kept)
                       (assoc m addr kept)
                       (dissoc m addr)))))))
    (when-not @allowed?
      (words/could-not :take-more {:why :sign-in-paced :most limit}))))

(defn- watch-bucket
  "The ration key and cap for live watches opened by `caller` in this copy."
  [^Seam seam ^Caller caller]
  (let [numbers (.-numbers seam)]
    (if-let [root (.-root caller)]
      [root (long (:max-watches-per-root numbers))]
      [:visitor (long (:max-visitor-watches numbers))])))

(defn- acquire-watch!
  "Atomically reserves one live-watch slot for `caller` in this copy's
  memory. Gives nil when reserved, or `:take-more` (`:why :too-many-watches`)
  when the cap for that root (or for visitors) is reached."
  [^Seam seam ^Caller caller]
  (let [[k cap] (watch-bucket seam caller)
        watches-atom (:watches (.-ration-state seam))
        acquired? (volatile! false)]
    (swap! watches-atom
           (fn [m]
             (let [n (long (get m k 0))]
               (if (< n cap)
                 (do (vreset! acquired? true)
                     (assoc m k (inc n)))
                 (do (vreset! acquired? false)
                     m)))))
    (when-not @acquired?
      (words/could-not :take-more {:why :too-many-watches :most cap}))))

(defn- release-watch!
  "Releases one live-watch slot previously reserved for `caller`."
  [^Seam seam ^Caller caller]
  (let [[k _] (watch-bucket seam caller)
        watches-atom (:watches (.-ration-state seam))]
    (swap! watches-atom
           (fn [m]
             (let [n (dec (long (get m k 0)))]
               (if (pos? n) (assoc m k n) (dissoc m k))))))
  nil)

(defn- ration
  "The one place where per-person act/read pacing and account blocks will
  sit once counts by person live in Auth. Copy-local `/sign-in/*` address
  pacing (`ration-sign-in!`) and copy-local live-watch caps
  (`acquire-watch!` / `release-watch!`) are active now; per-person counts
  and blocks in Auth are not built yet."
  [_seam _caller _doing]
  nil)

(defn- act-id-of
  "Completes a maker's act-id. Takes the seam, a layer and the maker's
  UUID. Gives [layer depot uuid], or one of the door's words. One round
  trip, to the layer's task: the door reads the layer's depot
  (`softland.store.door/act-id-for`), and the maker's UUID takes the place
  of the one that the door made."
  [^Seam seam layer uuid]
  (let [got (door/act-id-for (.-door seam) layer)]
    (if (:could-not got) got (assoc got 2 uuid))))

(defn- sent
  "Sends one act that is already held against its sender, and reads its
  answer. Takes the seam, the act, its act-id, and until when to wait, a
  moment of `System/nanoTime`. Gives the door's receipt with `:answer`, the
  door's answer; or one of the door's words."
  [^Seam seam act act-id ends]
  (let [receipt (door/send-act (.-door seam) act act-id)]
    (if (:could-not receipt)
      receipt
      (let [left-ms (max 0 (quot (unchecked-subtract (long ends) (System/nanoTime)) 1000000))]
        (assoc receipt :answer (door/answer-of (.-door seam) (:act-id receipt) (:digest receipt) left-ms))))))

(defn- ends-after
  "Takes a wait in milliseconds. Gives the moment of `System/nanoTime` at
  which it is over."
  [wait-ms]
  (unchecked-add (System/nanoTime) (* 1000000 (long wait-ms))))

(defn- send-now
  "The word `:send`. Holds the act against the caller, completes the
  act-id, sends, and waits for the answer."
  [^Seam seam caller {:keys [act act-id wait-ms]}]
  (or (act-fault caller act)
      (ration seam caller :act)
      (let [full (act-id-of seam (get act :layer) act-id)]
        (if (:could-not full)
          full
          (sent seam act full (ends-after wait-ms))))))

(defn- send-all-now
  "The word `:send-all`. Sends the acts in order, each held against the
  caller, and gives {:results [...]} with one result for each act that was
  tried: a receipt with its answer, or, as the last thing, the word with
  which a send ended. No act after that one is tried, since a later act of
  a series can rest on an earlier one; the maker sends the rest again under
  the same UUIDs. The wait is one span for the whole series."
  [^Seam seam caller {:keys [acts wait-ms]}]
  (let [ends (ends-after wait-ms)]
    {:results
     (reduce (fn [results {:keys [act act-id]}]
               (if (.isInterrupted (Thread/currentThread))
                 (reduced (conj results (words/could-not :find-her-sign-in {:why :ended})))
                 (let [result (or (act-fault caller act)
                                  (ration seam caller :act)
                                  (let [full (act-id-of seam (get act :layer) act-id)]
                                    (if (:could-not full) full (sent seam act full ends))))]
                   (if (:could-not result)
                     (reduced (conj results result))
                     (conj results result)))))
             []
             acts)}))

(defn- from-auth
  "Takes what a write of Auth gave. Gives it as it is; or `:reach-auth` for
  a call that failed; or `:take-that` for a refusal, with Auth's word."
  [got]
  (cond
    (:could-not got) (words/could-not :reach-auth {:got got})
    (:refused got) (words/could-not :take-that {:why (:refused got)})
    :else got))

(defn- derived
  "Derives an id. Takes what the id is of, a keyword; the caller's root;
  and the UUID that the system sent. Gives a UUID: the first 16 bytes of
  the SHA-256 of the three as one text, marked as a UUID of version 8. The
  same three give the same id, and no choice of the UUID gives an id that
  another root, or another kind of thing, has."
  [kind root uuid]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                        (.getBytes (str "softland/" (name kind) "/" root "/" uuid)
                                   java.nio.charset.StandardCharsets/UTF_8))
        buffer (java.nio.ByteBuffer/wrap digest)
        high (-> (.getLong buffer) (bit-and (unchecked-long 0xFFFFFFFFFFFF0FFF)) (bit-or 0x8000))
        low (-> (.getLong buffer) (bit-and 0x3FFFFFFFFFFFFFFF) (bit-or (unchecked-long 0x8000000000000000)))]
    (UUID. high low)))

(defn- beneath?
  "Takes the actors of a root (`softland.auth.client/under`, `:actors`),
  the id of an actor and the id of another. Gives true when the first was
  made by the second, at any depth. The walk goes up from the first and
  stops at the root or after as many steps as there are actors."
  [actors actor above]
  (let [made-by (into {} (map (juxt :actor :made-by)) actors)]
    (loop [at (get made-by actor) steps (count actors)]
      (cond
        (nil? at) false
        (= at above) true
        (zero? steps) false
        :else (recur (get made-by at) (dec steps))))))

(defn- make-agent-now
  "The word `:make-agent`. The agent's id and its sign-in's id are derived
  from the caller's root and the UUID that came (`derived`). Then two
  writes of Auth, the actor and its sign-in, each of which holds when it is
  made twice: a maker that was cut off between them says the word again."
  [^Seam seam ^Caller caller {:keys [uuid label hash]}]
  (let [client (.-client seam)
        root (.-root caller)
        agent (derived :agent root uuid)
        sign-in (derived :sign-in root uuid)
        now (now-ms)
        made (from-auth (auth/make-actor client {:root root :actor agent :made-by (.-who caller)
                                                 :label label :at now}))]
    (if (:could-not made)
      made
      (let [until (+ now (long (:secret-ms (.-numbers seam))))
            begun (from-auth (auth/begin-sign-in client {:root root :actor agent :sign-in sign-in
                                                         :kind :program :hash hash :until until
                                                         :label label :at now}))]
        (if (:could-not begun)
          begun
          {:made agent :root root :sign-in sign-in :until until})))))

(defn- end-agent-now
  "The word `:end-agent`. Reads the actors of the caller's root, and ends
  the agent when the caller made it, at any depth: an actor is ended by its
  maker or by anyone above."
  [^Seam seam ^Caller caller {:keys [agent]}]
  (let [client (.-client seam)
        root (.-root caller)
        got (auth/under client root)]
    (cond
      (:could-not got) (words/could-not :reach-auth {:got got})
      (not-any? (fn [actor] (= agent (:actor actor))) (:actors got))
      (words/could-not :find-the-agent {:why :not-known})
      (not (beneath? (:actors got) agent (.-who caller)))
      (words/could-not :find-the-agent {:why :not-beneath})
      :else (from-auth (auth/end-actor client {:root root :actor agent :at (now-ms)})))))

(defn- call-now
  "The body of `call`: one word, on the calling thread."
  [^Seam seam ^Caller caller word]
  (let [client (.-client seam)
        root (.-root caller)]
    (case (:word word)
      :who-am-i (about caller)

      :send (or (needs-a-who caller) (needs-a-session caller) (send-now seam caller word))

      :send-all (or (needs-a-who caller) (needs-a-session caller) (send-all-now seam caller word))

      :answer (or (needs-a-who caller)
                  (door/answer-of (.-door seam) (:act-id word) (:digest word) (:wait-ms word)))

      :read (or (query-fault caller (:query word))
                (ration seam caller :read)
                (read-path/read-once (.-read-path seam) (:query word)))

      :replace-secret
      (or (needs-a-who caller)
          (let [now (now-ms)
                until (+ now (long (:secret-ms (.-numbers seam))))
                old-hash @(.-hash caller)
                got (from-auth (auth/replace-secret client {:root root :sign-in (.-sign-in caller)
                                                            :old-hash old-hash :new-hash (:hash word)
                                                            :until until
                                                            :previous-until (+ now (long (:previous-ms (.-numbers seam))))
                                                            :at now}))]
            (if (:could-not got)
              got
              (do (reset! (.-hash caller) (:hash word))
                  {:replaced (.-sign-in caller) :until until}))))

      :sign-out
      (or (needs-a-who caller)
          (from-auth (auth/end-sign-in client {:root root :sign-in (.-sign-in caller) :at (now-ms)})))

      :open-session
      (or (needs-a-who caller)
          (let [session (UUID/randomUUID)
                now (now-ms)
                until (+ now (long (:session-ms (.-numbers seam))))
                driven-by (:driven-by word)
                got (from-auth (auth/open-session client
                                                  (cond-> {:root root :sign-in (.-sign-in caller)
                                                           :session session :until until :at now}
                                                    (some? driven-by) (assoc :driven-by driven-by))))]
            (if (:could-not got)
              got
              {:open session :until until :driven-by driven-by})))

      :close-session
      (or (needs-a-who caller) (needs-a-session caller)
          (from-auth (auth/close-session client {:root root :session (.-session caller) :at (now-ms)})))

      :invite
      (or (needs-a-who caller) (needs-the-person caller)
          (from-auth (auth/invite client {:person (derived :person root (:uuid word))
                                          :claim (let [claim (:claim word)]
                                                   (cond-> claim
                                                     (= :email (:by claim)) (update :email #(.toLowerCase ^String % Locale/ROOT))))
                                          :invited-by (.-who caller) :at (now-ms)})))

      :make-agent (or (needs-a-who caller) (make-agent-now seam caller word))

      :end-agent (or (needs-a-who caller) (end-agent-now seam caller word))

      :under
      (or (needs-a-who caller) (needs-the-person caller)
          (let [got (auth/under client root)]
            (if (:could-not got)
              (words/could-not :reach-auth {:got got})
              (let [now (now-ms)]
                (update got :sign-ins (fn [sign-ins]
                                        (filterv (fn [s] (< now (long (:until s)))) sign-ins)))))))

      :end-sign-in
      (or (needs-a-who caller) (needs-the-person caller)
          (let [got (auth/end-sign-in client {:root root :sign-in (:sign-in word) :at (now-ms)})]
            (if (= :no-such-sign-in (:refused got))
              (words/could-not :find-the-sign-in nil)
              (from-auth got))))

      (words/could-not :read-the-word {:why :word-not-known :at :word}))))

(defn call
  "Does one word whose result is one thing. Takes the seam, a caller, and a
  word in its one form (`softland.wire.words/check`); `:watch` and `:leave`
  are a face's, since a face holds the watches of its connection. Gives a
  task of the word's result, as plain data:

    :who-am-i        what `about` gives
    :send            the door's receipt {:act-id :digest :ack}, with
                     `:answer`, the door's answer; or one of the door's
                     words
    :send-all        {:results [...]}, one for each act that was tried
    :answer          one of the door's four answers
    :read            the read path's answer, or one of its words
    :replace-secret  {:replaced sign-in :until t}
    :sign-out, :close-session, :invite, :end-agent, :end-sign-in
                     what Auth gave
    :make-agent      {:made agent :root root :sign-in sign-in :until t}
    :under           {:actors [...] :sign-ins [...]}

  or one of the runtime's words (`softland.wire.words/could-nots`). A
  visitor reads; every other word but `:who-am-i` gives her
  `:find-her-sign-in`.

  The seam checks nothing of an act's form, as the door checks nothing: a
  gate refuses a wrong act under its act-id, and the log keeps it."
  [seam caller word]
  (on-its-own-thread seam (fn [] (call-now seam caller word))))

(defn send-as-system
  "Sends one act as the system and waits for its answer. Takes the seam;
  the act in the store's one form, whose `:who` must be the store's
  `system` id; the act-id's UUID; and a wait in milliseconds. Gives the
  door's receipt with `:answer`, or one of the door's words.

  `softland.runtime.sign-in` is the one caller, for the making of a
  person's own layer and for a deployment's first acts. No face calls it,
  and the pure proof reads the source to hold that."
  [^Seam seam act uuid wait-ms]
  (if-not (= first-act/system (get act :who))
    (words/could-not :send-as-another {:at :who})
    (let [full (act-id-of seam (get act :layer) uuid)]
      (if (:could-not full)
        full
        (sent seam act full (ends-after wait-ms))))))

(defn read-as-system
  "Reads once, with the system as the asker. Takes the seam and a query,
  whole, whose `:asker` must be the store's `system` id. Gives the read
  path's answer or word. `softland.runtime.sign-in` is the one caller."
  [^Seam seam query]
  (if-not (= first-act/system (get query :asker))
    (words/could-not :send-as-another {:at :asker})
    (read-path/read-once (.-read-path seam) query)))

;; ---------------------------------------------------------------- the flows

(defn- live-now
  "Reads whether a caller is live now. Takes the seam and a caller that has
  a who. Gives [:live {:until :session-until}], [:not word], or
  [:not-reached word]. One read of Auth. The caller proved its secret when
  it was made; what is read here is whether its sign-in and its session
  still stand. So a caller outlives a replacing of its secret, as its
  session does."
  [^Seam seam ^Caller caller]
  (let [got (auth/who-is (.-client seam) {:root (.-root caller) :sign-in (.-sign-in caller)
                                          :session (.-session caller)})]
    (if (:could-not got)
      [:not-reached (words/could-not :reach-auth {:got got})]
      (let [now (now-ms)
            [verdict until-or-why] (lasting (:sign-in got) now)
            fault (when (.-session caller) (session-fault got (.-sign-in caller) now))]
        (cond
          (= :not verdict) [:not (no-sign-in until-or-why)]
          fault [:not fault]
          :else [:live {:until until-or-why :session-until (:until (:session got))}])))))

(defn- seems-gone?
  "Takes a caller, the end that is held for her sign-in, and what Auth
  pushed as live under her root. Gives true when the value does not hold
  her sign-in with that end, or does not hold her session under that
  sign-in. It is a reason to read, and no judgment."
  [^Caller caller until value]
  (let [sign-in (get-in value [:sign-ins (.-sign-in caller)])]
    (or (nil? sign-in)
        (not= until (:until sign-in))
        (and (.-session caller)
             (not= (.-sign-in caller) (get-in value [:sessions (.-session caller) :sign-in]))))))

(defn standing
  "Says when a caller stops being live. Takes the seam and a caller. Gives
  a flow that gives one thing and ends: the word with which she stopped,
  `:find-her-sign-in`, `:find-her-session` or `:reach-auth`. For as long as
  she is live it gives nothing. A visitor's flow gives nothing and never
  ends by itself. Cancelling it stops the listening.

  It listens for her root first and reads Auth after, so no change falls
  between the two. Then, at each push that does not show her as she is
  held, and when the time of her secret or of her session comes near, it
  reads Auth again, and the read decides: she is live, the session's time
  is moved on when it is due, and the timer is set anew; or she is not, and
  the flow says so. A secret that was replaced moves the sign-in's end; a
  session is never ended by that.

  When Auth's watch of her root is lost, the flow ends with `:reach-auth`
  and reads nothing: a caller that nothing watches could outlive its
  sign-in. The system connects again, and is watched anew.

  Each read runs on a thread of the seam's own, and so does the telling of
  a lost watch: the standing's thread that hands it on returns at once, and
  one listener that blocks while it ends its connection holds back no
  other. The end is said outside the flow's lock."
  [^Seam seam ^Caller caller]
  (let [subject
        (fn [!]
          (if (nil? (.-who caller))
            (fn [] nil)
            (let [state (atom {:until (.-until caller) :timer nil :done? false})
                  ^ReentrantLock lock (ReentrantLock.)
                  with-lock (fn [f]
                              (.lock lock)
                              (try (f) (finally (.unlock lock))))
                  executor ^ExecutorService (.-executor seam)
                  ;; Under the lock: marks the flow done and gives the word
                  ;; when this is the first end, else nil.
                  ending (fn [word]
                           (when-not (:done? @state)
                             (swap! state assoc :done? true)
                             word))
                  ;; The word is said outside the lock: what rests on the
                  ;; flow (a face's connection) can block while it tells its
                  ;; system, and holds no lock of the standing meanwhile.
                  end! (fn [word]
                         (when-let [word (with-lock (fn [] (ending word)))]
                           (! word)))
                  failed (fn [^Throwable t]
                           (words/could-not :reach-auth {:why :standing-failed
                                                         :threw (.getName (class t))}))
                  check! (fn check! []
                           (when-let [word
                                      (with-lock
                                        (fn []
                                          (when-not (:done? @state)
                                            (try
                                              (let [[verdict got] (live-now seam caller)]
                                                (if (= :live verdict)
                                                  (let [session-until (when-let [held (:session-until got)]
                                                                        (moved-on! seam (.-root caller) (.-sign-in caller)
                                                                                   (.-session caller) held))
                                                        ;; The next look: when the secret's
                                                        ;; time passes, or right after half of
                                                        ;; the session's span is left (always
                                                        ;; strictly before session expiry).
                                                        now (now-ms)
                                                        span (long (:session-ms (.-numbers seam)))
                                                        next (cond-> (- (long (:until got)) now)
                                                               session-until
                                                               (min (let [left (- (long session-until) now)
                                                                          half (quot span 2)
                                                                          pad (min 50 (max 1 (quot half 2)))]
                                                                      (if (< (* 2 left) span)
                                                                        (min left 1000)
                                                                        (min (max 1 (dec left)) (+ pad (- left half)))))))]
                                                    (when-let [timer (:timer @state)]
                                                      (.cancel ^ScheduledFuture timer false))
                                                    (swap! state assoc
                                                           :until (:until got)
                                                           :timer (.schedule ^ScheduledExecutorService (.-timer seam)
                                                                             ^Runnable
                                                                             (fn []
                                                                               (try (.execute executor check!)
                                                                                    (catch Throwable t (end! (failed t)))))
                                                                             (long (max 1 next))
                                                                             TimeUnit/MILLISECONDS))
                                                    nil)
                                                  (ending got)))
                                              (catch Throwable t
                                                (ending (failed t)))))))]
                             (! word)))
                  stop (standing/listen!
                        (.-standing seam) (.-root caller)
                        (fn [value]
                          (cond
                            ;; Handed to a thread of the seam's own: the
                            ;; standing's thread returns at once.
                            (= standing/not-reached value)
                            (let [lost (fn [] (end! (words/could-not :reach-auth {:why :watch-lost})))]
                              (try (.execute executor ^Runnable lost)
                                   (catch Throwable _ (lost))))

                            (seems-gone? caller (:until @state) value)
                            (try (.execute executor check!)
                                 (catch Throwable t (end! (failed t)))))))]
              (.execute executor check!)
              (fn []
                (stop)
                (with-lock
                  (fn []
                    (swap! state assoc :done? true)
                    (when-let [timer (:timer @state)]
                      (.cancel ^ScheduledFuture timer false))))))))]
    (m/eduction (take 1) (m/observe subject))))

(defn watch
  "Reads, and keeps the read live. Takes the seam, a caller, and a query as
  it arrived, with its asker. Gives a flow of the read path's own messages
  (`softland.store.read-path/read-live`):

    {:anew answer}                    first, and whenever what shapes the
                                      read changed
    {:changed {place place-answer}}   the places that changed, each with
                                      what the read gives there now
    {:ended word}                     once, as the last thing

  The read is opened when the flow is first listened to, on a thread of the
  seam's own, and left when the flow is cancelled or has ended. A query
  that names another asker than the caller, and a read that could not be
  opened, give the word as `{:ended word}` and nothing else.

  What ends it: the read path ended the read, with its word; or the face
  cancelled it. The seam does not end a watch when its caller stops being
  live: the face holds `standing` beside its watches and cancels them, so
  that one listening serves every watch of a connection.

  A slow reader. What waits for her is merged by place
  (`softland.wire.watching/merged`): she is handed fewer and larger things,
  in order, and never a present that is wrong. Each thing is handed on from
  a thread of the seam's own: the thread of Rama's client that brought a
  change has returned by then."
  [^Seam seam ^Caller caller query]
  (let [subject
        (fn [!]
          (let [^ReentrantLock lock (ReentrantLock.)
                state (atom {:left? false :handle nil :ended? false})
                rp (.-read-path seam)
                released? (atom true)
                release-once! (fn []
                                (when (compare-and-set! released? false true)
                                  (release-watch! seam caller)))
                tell! (fn [message]
                        ;; One call of `!` at a time, and none after an end.
                        (.lock lock)
                        (try
                          (when-not (:ended? @state)
                            (when (contains? message :ended)
                              (swap! state assoc :ended? true)
                              (release-once!))
                            (! message))
                          (finally (.unlock lock))))
                leave! (fn []
                         (let [{:keys [handle]} (swap! state assoc :left? true)]
                           (release-once!)
                           (when handle (read-path/leave rp handle))))
                fault (or (query-fault caller query)
                          (ration seam caller :watch)
                          (let [capped (acquire-watch! seam caller)]
                            (when-not capped (reset! released? false))
                            capped))]
            (if fault
              (tell! {:ended fault})
              (try
                (.execute ^ExecutorService (.-executor seam)
                          (fn []
                            (try
                              (let [got (read-path/read-live rp query tell!)]
                                (if (:could-not got)
                                  (do (tell! {:ended got}) (leave!))
                                  (let [handle (:leave-with got)]
                                    ;; The flow can be left while the read opens.
                                    (when (:left? (swap! state assoc :handle handle))
                                      (read-path/leave rp handle)))))
                              (catch Throwable t
                                (tell! {:ended (words/could-not :do-the-word {:threw (.getName (class t))})})
                                (leave!)))))
                (catch Throwable t
                  (tell! {:ended (words/could-not :do-the-word {:threw (.getName (class t))})})
                  (leave!))))
            leave!))]
    (m/eduction
     (watching/through (fn [message] (contains? message :ended)))
     (m/ap (let [waits (m/?> (m/relieve watching/merged (m/observe subject)))
                 waits (m/? (on-its-own-thread seam (fn [] waits)))]
             (m/?> (m/seed (watching/messages waits))))))))
