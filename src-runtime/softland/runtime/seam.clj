(ns softland.runtime.seam
  "The seam: the one place that every face of the runtime stands on. It
  turns a credential into a caller, holds each thing that a caller sends
  against who that caller is, and puts the store's door, the store's read
  path and Auth behind the words of the wire. Clojure and Missionary; it
  requires no Electric.

  A face is whatever speaks to one kind of system: the line
  (`softland.runtime.line`), and the pages by which a person signs in
  (`softland.runtime.http`). A face proves nothing: it hands the seam a
  credential and gets a caller. A caller is an object that only this
  namespace makes. It prints as the word and nothing else, and it is no
  data: a who that crosses the wire back cannot become a caller.

  The seam's contract is data: a word goes in as the wire has it
  (`softland.wire.words`) and plain data comes out. So where a face runs is
  no wall: a face in another process speaks the same words over the line.

  The whole act arrives, with its `:who` and its `:session`. The seam writes
  neither and holds them against the caller, and a query's `:asker` the
  same. What is sent as another is refused `:send-as-another` and written
  to no log: an act under a who that did not send it would be a false line
  in a log that is never trimmed.

  A browser's sign-in is taken from a cookie alone, and a program's from a
  header alone: a program that holds its own secret cannot say a person's
  yes (the review of 20 November 2026 ran that it could).

  The session outlives the secret: `:replace-secret` changes no session, so
  an act that is sent again after a break, under the same UUID, has the
  same digest and is taken once.

  No system chooses an id. A session's id is random. The id of an agent, of
  its sign-in and of an invited person is a hash of the caller's root and a
  UUID that the system sent (`derived`): the same UUID gives the same id,
  so a word said twice is one making, and no UUID that a system can pick
  gives the id of a thing that is not its own. The UUID is kept nowhere,
  not by the runtime, not in Auth and in no act, so a derived id cannot be
  worked back to its root. A person's id says nothing (src-store/README.md,
  9 October 2026, entry 1), and neither does an id made from it. Auth
  trusts the runtime for that (its spec, \"The client's contract\"): an
  actor's id is the who of its acts, so a system that could choose one
  could become somebody else.

  The act-id. A maker sends the random part, a UUID, with the act. The seam
  asks the door for an act-id of the act's lane and puts the maker's UUID
  in its place. So a maker needs no knowledge of a lane's depot, and a
  repeat is the same thing.

  The thread rule. A function that the read path calls for a change runs on
  a thread of Rama's client, and one that stays there holds back the reads
  of others. Such a thread only puts the change with what waits and hands
  it to a thread of the seam's own.

  The seam owns nothing that lasts, and keeps no row, no answer and no
  caller between two calls. Pacing is in this copy's memory. `call` and
  `watch` write nothing of a read: the keeping of reads is the trail's (the
  runtime's page, piece 5)."
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
  "The seam's numbers, each with its default. An opener can give another at
  `open`.

    :secret-ms    how long a secret stands from its making or its
                  replacing. A system that is in use replaces its secret
                  before then; a sign-in whose secret's time passes is over
    :previous-ms  how long the secret before stands after a replacing, for
                  the requests that were on their way
    :session-ms   how long a session stands from its opening, and from each
                  time that the runtime moves its time on
    :sign-in-window-ms
                  the sliding window over which posts to `/sign-in/*` from
                  one remote address, and the polls of one ask, are counted
    :max-sign-in-posts-per-address
                  beyond it a post is refused with 429 before it reaches
                  Google or Auth
    :max-watches-per-root
                  live watches of one root, a person and every agent
                  beneath her, across all connections, in one copy
    :max-visitor-watches
                  live watches of all visitors together, in one copy
    :max-watches-per-connection, :max-visitor-watches-per-connection
                  so that one connection cannot take all of its root's, or
                  one visitor all of the visitors'
    :max-polls-per-ask
                  polls of one ask that are let in after they proved the ask
    :session-window-ms, :max-sessions-opened-per-sign-in
                  sessions opened under one sign-in in one copy: by a
                  connection that names none, by `:open-session`, or by a
                  page"
  {:secret-ms (* 7 24 60 60 1000)
   :previous-ms 60000
   :session-ms (* 24 60 60 1000)
   :sign-in-window-ms 10000
   :max-sign-in-posts-per-address 30
   :max-watches-per-root 256
   :max-visitor-watches 64
   :max-watches-per-connection 128
   :max-visitor-watches-per-connection 8
   :max-polls-per-ask 20
   :session-window-ms 60000
   :max-sessions-opened-per-sign-in 30})

;; ------------------------------------------------------------------ the caller

(deftype Caller [who actor root sign-in session kind hash until own-lane driven-by from]
  Object
  (toString [_] (if (some? who) "#<caller>" "#<visitor>")))

(defmethod print-method Caller [^Caller caller ^Writer w]
  (.write w (.toString caller)))

(defmethod print-dup Caller [^Caller caller ^Writer w]
  (.write w (.toString caller)))

(alter-meta! #'->Caller assoc
             :private true
             :doc "`who` is the store party keyword (`:person.<uuid>` or `:agent.<uuid>`, nil
  for a visitor), `actor` is Auth's internal actor UUID (nil for a visitor),
  and `session` is nil when the face named and asked for none. `hash` is an
  atom: the latest hash of the secret for this connection, reset on
  `:replace-secret`. `until` is the time until which that secret stands.
  `from` is what the face said of the request.")

(defn caller?
  [x]
  (instance? Caller x))

(defn who-of
  "The store party id (`:person.<uuid>` or `:agent.<uuid>`), or nil for a visitor."
  [^Caller caller]
  (.-who caller))

(defn root-of
  "The person UUID at the root. Nil for a visitor."
  [^Caller caller]
  (.-root caller))

(defn session-of
  "Nil when the caller is in no session."
  [^Caller caller]
  (.-session caller))

(defn own-lane-of
  [^Caller caller]
  (.-own-lane caller))

(defn person?
  [^Caller caller]
  (and (some? (.-actor caller)) (= (.-actor caller) (.-root caller))))

(defn about
  "What a face may tell the system that holds the credential. It is data,
  and it is no caller."
  [^Caller caller]
  (if (some? (.-who caller))
    (cond-> {:who (.-who caller) :root (.-root caller) :sign-in (.-sign-in caller)
             :session (.-session caller) :kind (.-kind caller)
             :own-lane (.-own-lane caller) :until (.-until caller)}
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
             :doc "The door, the read path, Auth's client and the standing are borrowed.
  Every call of the store and of Auth, and every handing on of a flow, runs
  on the executor's virtual threads. The one timer thread is for a secret
  whose time passes.")

(defn open
  "Makes no call to Rama. The opener owns the door, the read path, Auth's
  client and the standing, and closes them. `:numbers` is optional."
  [{:keys [door read-path auth standing numbers]}]
  (->Seam door read-path auth standing
          (merge default-numbers (select-keys numbers (keys default-numbers)))
          {:sign-in (atom {}) :polls (atom {}) :sessions (atom {}) :watches (atom {})}
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
  "A flow that is still open is told nothing; its face cancels it."
  [^Seam seam]
  (.shutdown ^ExecutorService (.-executor seam))
  (.shutdownNow ^ScheduledExecutorService (.-timer seam))
  nil)

(defn now-ms
  "Milliseconds since 1970, by the clock of this process: the time that the
  runtime writes on each event of Auth, and the time that a secret's end is
  held against."
  []
  (System/currentTimeMillis))

(defn numbers-of
  [^Seam seam]
  (.-numbers seam))

(defn auth-of
  "For `softland.runtime.sign-in`, the one namespace beside the seam that
  writes what sign-in writes."
  [^Seam seam]
  (.-client seam))

(defn- on-its-own-thread
  "`f` takes no argument and may block. Cancelling the task interrupts its
  thread while `f` runs; the door, the read path and Auth's client turn an
  interrupt into one of their words."
  [^Seam seam f]
  (m/via (.-executor seam) (f)))

;; ---------------------------------------------------------------------- who

(def ^:private the-visitor
  "The caller of everybody who is not signed in."
  (->Caller nil nil nil nil nil nil nil nil nil nil nil))

(defn visitor
  "`from` is what the face says of the request."
  ([] the-visitor)
  ([from] (->Caller nil nil nil nil nil nil nil nil nil nil from)))

(defn- actor-party
  [actor root]
  (when (some? actor)
    (keyword (str (if (= actor root) "person." "agent.") actor))))

(defn- no-sign-in
  [why]
  (words/could-not :find-her-sign-in {:why why}))

(defn- proves?
  [{:keys [kind previous] :as sign-in} hash carrier now]
  (and (some? sign-in)
       (= kind (case carrier :cookie :browser :header :program nil))
       (or (credential/same-hash? hash (:hash sign-in))
           (and (some? previous)
                (credential/same-hash? hash (:hash previous))
                (< now (long (:until previous)))))))

(defn- lasting
  "Reads no secret: it says only whether the sign-in stands."
  [{:keys [until ended? actor-ended?] :as sign-in} now]
  (cond
    (nil? sign-in) [:not :not-known]
    (or ended? actor-ended?) [:not :ended]
    (<= (long until) now) [:not :over]
    :else [:live (long until)]))

(defn- judged
  "A secret that is not the sign-in's, and a sign-in that came in the wrong
  carrier, are told as `:not-known`: whoever holds a wrong secret learns
  nothing of the sign-in."
  [{:keys [sign-in]} hash carrier now]
  (if (proves? sign-in hash carrier now)
    (lasting sign-in now)
    [:not :not-known]))

(defn- session-fault
  [{:keys [session]} sign-in now]
  (cond
    (nil? session) (words/could-not :find-her-session {:why :not-known})
    (not= sign-in (:sign-in session)) (words/could-not :find-her-session {:why :not-hers})
    (or (:closed? session) (<= (long (:until session)) now)) (words/could-not :find-her-session {:why :closed})))

(defn- moved-on!
  "One write of Auth when less than half of `:session-ms` is left, and none
  otherwise. Gives the time that it held, written or not: a repeat of
  `open-session` gives `{:open S}` even on a closed session, so the next
  look must read the session's real standing from Auth."
  [^Seam seam root sign-in session until]
  (let [now (now-ms)
        span (long (:session-ms (.-numbers seam)))]
    (when (< (* 2 (- (long until) now)) span)
      (auth/open-session (.-client seam) {:root root :sign-in sign-in :session session
                                          :until (+ now span) :at now}))
    until))

(declare derived session-paced)

(defn- opened-in
  "`made` takes the session and its `:driven-by` and makes the caller. After
  the write, one `who-is` read, so that `:driven-by` comes from Auth and a
  session that is closed is not taken: a repeat of `open-session` gives
  `{:open S}` for a session that is closed too."
  [^Seam seam root sign-in session driven-by made]
  (or (session-paced seam sign-in)
      (let [client (.-client seam)
            now (now-ms)
            opened (auth/open-session client
                                      (cond-> {:root root :sign-in sign-in :session session
                                               :until (+ now (long (:session-ms (.-numbers seam))))
                                               :at now}
                                        (some? driven-by) (assoc :driven-by driven-by)))]
        (cond
          (:could-not opened) (words/could-not :reach-auth {:got opened})
          (= :in-use (:refused opened)) (words/could-not :find-her-session {:why :not-hers})
          (:refused opened) (no-sign-in :ended)
          :else (let [after (auth/who-is client {:root root :sign-in sign-in :session session})]
                  (if (:could-not after)
                    (words/could-not :reach-auth {:got after})
                    (or (session-fault after sign-in (now-ms))
                        (made session (get-in after [:session :driven-by])))))))))

(defn- caller-now
  [^Seam seam {:keys [credential carrier session new-session? session-for driven-by from]}]
  (if-let [{:keys [root sign-in secret]} (credential/parts credential)]
    (let [client (.-client seam)
          got (auth/who-is client {:root root :sign-in sign-in :session session})]
      (if (:could-not got)
        (words/could-not :reach-auth {:got got})
        (let [hash (credential/hash-of secret)
              [verdict until-or-why] (judged got hash carrier (now-ms))
              {:keys [actor kind]} (:sign-in got)
              party (actor-party actor root)
              made (fn [session session-driven-by]
                     (->Caller party actor root sign-in session kind (atom hash) until-or-why
                               (:own-lane got) session-driven-by from))]
          (cond
            (= :not verdict) (no-sign-in until-or-why)
            (some? session) (or (session-fault got sign-in (now-ms))
                                (do (moved-on! seam root sign-in session (:until (:session got)))
                                    (made session (get-in got [:session :driven-by]))))
            (some? session-for)
            (opened-in seam root sign-in (derived :session root session-for) driven-by made)

            new-session?
            (opened-in seam root sign-in (UUID/randomUUID) driven-by made)

            :else (made nil nil)))))
    (no-sign-in :not-a-credential)))

(defn caller
  "Gives a task of a caller, or of one of the runtime's words. `request`:

    :credential    the credential's text
    :carrier       `:header` or `:cookie`: where the face found it
    :session       the id of the session that the system named, or nil
    :new-session?  whether to open a new session when none was named
    :session-for   a UUID that a page made for its one act, when none was
                   named: the session is derived from the root and it
                   (`derived`), so that a form that is posted twice is in
                   one session
    :driven-by     what drives a session that is opened here, or nil
    :from          what the face says of the request, for `ration`

  A credential that stands for nobody is told so and is not made a
  visitor: the system that holds it must learn that it has to sign in
  again. A face that got no credential at all asks for `visitor`."
  [seam request]
  (on-its-own-thread seam (fn [] (caller-now seam request))))

;; ------------------------------------------------- what the seam holds, and adds

(defn- needs-a-who
  "Reads nothing: a caller was live when it was made, and a face that holds
  one for longer than a request holds `standing` beside it and ends what
  rests on the caller when that says so. The time that a caller was made
  with is not held against it here, since a replacing of the secret moves
  it."
  [^Caller caller]
  (when (nil? (.-who caller))
    (no-sign-in :no-credential)))

(defn- needs-a-session
  "An act names its session, so a caller that is in none sends no act."
  [^Caller caller]
  (when (nil? (.-session caller))
    (words/could-not :find-her-session {:why :not-known})))

(defn- needs-the-person
  [caller]
  (when-not (person? caller)
    (words/could-not :do-that-as-an-agent nil)))

(defn- plain-map?
  [x]
  (and (map? x) (not (record? x))))

(defn- act-fault
  "The caller must have a who and a session."
  [^Caller caller act]
  (cond
    (not (plain-map? act)) (words/could-not :read-the-word {:why :not-a-plain-map :at :act})
    (not= (.-who caller) (get act :who)) (words/could-not :send-as-another {:at :who})
    (not= (.-session caller) (get act :session)) (words/could-not :send-as-another {:at :session})))

(defn- query-fault
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

(def ^:private max-paced-keys
  "The most keys (addresses, asks, sign-ins) that one pacer tracks at once,
  so that a flood of new keys cannot grow memory without bound. When the
  table is full, the keys whose moments have all passed are let go, and
  then the one with the oldest last moment."
  10000)

(defn- paced!
  "One sliding window. `pacer` is an atom of a map from a key to the moments
  (by `System/nanoTime`) of what it counted. True counts this one; false
  counts nothing."
  [pacer k limit window-ms]
  (let [limit (long limit)
        now (System/nanoTime)
        cutoff (unchecked-subtract now (* 1000000 (long window-ms)))
        recent? (fn [ts] (pos? (unchecked-subtract (long ts) cutoff)))
        allowed? (volatile! false)]
    (swap! pacer
           (fn [m]
             (let [m (if (>= (count m) max-paced-keys)
                       (let [pruned (into {}
                                          (keep (fn [[k' v]]
                                                  (let [kept (filterv recent? v)]
                                                    (when (seq kept) [k' kept]))))
                                          m)]
                         (if (and (>= (count pruned) max-paced-keys) (not (contains? pruned k)))
                           (dissoc pruned (key (apply min-key (fn [[_ v]] (peek v)) pruned)))
                           pruned))
                       m)
                   kept (filterv recent? (get m k []))]
               (if (< (count kept) limit)
                 (do (vreset! allowed? true)
                     (assoc m k (conj kept now)))
                 (do (vreset! allowed? false)
                     (if (seq kept) (assoc m k kept) (dissoc m k)))))))
    @allowed?))

(defn- given-back!
  [pacer k]
  (swap! pacer (fn [m]
                 (let [v (get m k)]
                   (cond
                     (empty? v) m
                     (= 1 (count v)) (dissoc m k)
                     :else (assoc m k (pop v))))))
  nil)

(defn ration-sign-in!
  "Paces posts to `/sign-in/*` by the address that the face says that they
  came from, before any call to Google or Auth. A post that proved a
  program's own ask is given back (`give-back-sign-in!`): a program's
  waiting does not use up the address."
  [^Seam seam address]
  (let [numbers (.-numbers seam)
        limit (long (:max-sign-in-posts-per-address numbers))
        addr (let [s (str address)] (if (seq s) s "unknown"))]
    (when-not (paced! (:sign-in (.-ration-state seam)) addr limit (:sign-in-window-ms numbers))
      (words/could-not :take-more {:why :sign-in-paced :most limit}))))

(defn give-back-sign-in!
  [^Seam seam address]
  (given-back! (:sign-in (.-ration-state seam)) (let [s (str address)] (if (seq s) s "unknown"))))

(defn ration-poll!
  "Paces the polls of one ask that proved it. `code` is the ask's code."
  [^Seam seam code]
  (let [numbers (.-numbers seam)
        limit (long (:max-polls-per-ask numbers))]
    (when-not (paced! (:polls (.-ration-state seam)) code limit (:sign-in-window-ms numbers))
      (words/could-not :take-more {:why :polls-paced :most limit}))))

(defn- session-paced
  [^Seam seam sign-in]
  (let [numbers (.-numbers seam)
        limit (long (:max-sessions-opened-per-sign-in numbers))]
    (when-not (paced! (:sessions (.-ration-state seam)) sign-in limit (:session-window-ms numbers))
      (words/could-not :take-more {:why :sessions-paced :most limit}))))

(defn watches-per-connection
  [^Seam seam ^Caller caller]
  (long (get (.-numbers seam) (if (.-root caller)
                                :max-watches-per-connection
                                :max-visitor-watches-per-connection))))

(defn- watch-bucket
  [^Seam seam ^Caller caller]
  (let [numbers (.-numbers seam)]
    (if-let [root (.-root caller)]
      [root (long (:max-watches-per-root numbers))]
      [:visitor (long (:max-visitor-watches numbers))])))

(defn- acquire-watch!
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
  [^Seam seam ^Caller caller]
  (let [[k _] (watch-bucket seam caller)
        watches-atom (:watches (.-ration-state seam))]
    (swap! watches-atom
           (fn [m]
             (let [n (dec (long (get m k 0)))]
               (if (pos? n) (assoc m k n) (dissoc m k))))))
  nil)

(defn- ration
  "The place where pacing of acts and reads by person, and blocks, will sit
  once counts by person live in Auth. Those are not built: it gives nil."
  [_seam _caller _doing]
  nil)

(defn- act-id-of
  "One round trip, to the lane's task: the door reads the lane's depot
  (`softland.store.door/act-id-for`). The maker's UUID takes the place of
  the one that the door made."
  [^Seam seam lane uuid]
  (let [got (door/act-id-for (.-door seam) lane)]
    (if (:could-not got) got (assoc got 2 uuid))))

(defn- sent
  "The act must already be held against its sender. `ends` is a moment of
  `System/nanoTime`."
  [^Seam seam act act-id ends]
  (let [receipt (door/send-act (.-door seam) act act-id)]
    (if (:could-not receipt)
      receipt
      (let [left-ms (max 0 (quot (unchecked-subtract (long ends) (System/nanoTime)) 1000000))]
        (assoc receipt :answer (door/answer-of (.-door seam) (:act-id receipt) (:digest receipt) left-ms))))))

(defn- ends-after
  [wait-ms]
  (unchecked-add (System/nanoTime) (* 1000000 (long wait-ms))))

(defn- send-now
  [^Seam seam caller {:keys [act act-id wait-ms]}]
  (or (act-fault caller act)
      (ration seam caller :act)
      (let [full (act-id-of seam (get act :lane) act-id)]
        (if (:could-not full)
          full
          (sent seam act full (ends-after wait-ms))))))

(defn- send-all-now
  "One result for each act that was tried. It stops at the first send that
  ends with a word, since a later act of a series can rest on an earlier
  one; the maker sends the rest again under the same UUIDs. The wait is one
  span for the whole series."
  [^Seam seam caller {:keys [acts wait-ms]}]
  (let [ends (ends-after wait-ms)]
    {:results
     (reduce (fn [results {:keys [act act-id]}]
               (if (.isInterrupted (Thread/currentThread))
                 (reduced (conj results (words/could-not :find-her-sign-in {:why :ended})))
                 (let [result (or (act-fault caller act)
                                  (ration seam caller :act)
                                  (let [full (act-id-of seam (get act :lane) act-id)]
                                    (if (:could-not full) full (sent seam act full ends))))]
                   (if (:could-not result)
                     (reduced (conj results result))
                     (conj results result)))))
             []
             acts)}))

(defn- from-auth
  [got]
  (cond
    (:could-not got) (words/could-not :reach-auth {:got got})
    (:refused got) (words/could-not :take-that {:why (:refused got)})
    :else got))

(defn- derived
  "The first 16 bytes of a SHA-256, marked as a UUID of version 8. No choice
  of the UUID gives an id that another root, or another kind of thing, has."
  [kind root uuid]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                        (.getBytes (str "softland/" (name kind) "/" root "/" uuid)
                                   java.nio.charset.StandardCharsets/UTF_8))
        buffer (java.nio.ByteBuffer/wrap digest)
        high (-> (.getLong buffer) (bit-and (unchecked-long 0xFFFFFFFFFFFF0FFF)) (bit-or 0x8000))
        low (-> (.getLong buffer) (bit-and 0x3FFFFFFFFFFFFFFF) (bit-or (unchecked-long 0x8000000000000000)))]
    (UUID. high low)))

(defn- beneath?
  "`actors` is `:actors` of `softland.auth.client/under`. True when `actor`
  was made by `above`, at any depth. The walk stops after as many steps as
  there are actors."
  [actors actor above]
  (let [made-by (into {} (map (juxt :actor :made-by)) actors)]
    (loop [at (get made-by actor) steps (count actors)]
      (cond
        (nil? at) false
        (= at above) true
        (zero? steps) false
        :else (recur (get made-by at) (dec steps))))))

(defn- make-agent-now
  "Two writes of Auth, the actor and its sign-in, each of which holds when
  it is made twice: a maker that was cut off between them says the word
  again."
  [^Seam seam ^Caller caller {:keys [uuid label hash]}]
  (let [client (.-client seam)
        root (.-root caller)
        agent (derived :agent root uuid)
        sign-in (derived :sign-in root uuid)
        now (now-ms)
        made (from-auth (auth/make-actor client {:root root :actor agent :made-by (.-actor caller)
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
  "An actor is ended by its maker or by anyone above, at any depth."
  [^Seam seam ^Caller caller {:keys [agent]}]
  (let [client (.-client seam)
        root (.-root caller)
        got (auth/under client root)]
    (cond
      (:could-not got) (words/could-not :reach-auth {:got got})
      (not-any? (fn [actor] (= agent (:actor actor))) (:actors got))
      (words/could-not :find-the-agent {:why :not-known})
      (not (beneath? (:actors got) agent (.-actor caller)))
      (words/could-not :find-the-agent {:why :not-beneath})
      :else (from-auth (auth/end-actor client {:root root :actor agent :at (now-ms)})))))

(defn- call-now
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
          (session-paced seam (.-sign-in caller))
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
                                          :invited-by (.-actor caller) :at (now-ms)})))

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
  "`word` must be in its one form (`softland.wire.words/check`). `:watch`
  and `:leave` are a face's, since a face holds the watches of its
  connection. Gives a task of the word's result as plain data, or of one of
  the runtime's words. A visitor reads; every other word but `:who-am-i`
  gives her `:find-her-sign-in`.

  The seam checks nothing of an act's form, as the door checks nothing: a
  gate refuses a wrong act under its act-id, and the log keeps it."
  [seam caller word]
  (on-its-own-thread seam (fn [] (call-now seam caller word))))

(defn send-as-system
  "No face calls it: `softland.runtime.sign-in` is the one caller, for the
  making of a person's own lane and for a deployment's first acts, and the
  pure proof reads the source to hold that."
  [^Seam seam act uuid wait-ms]
  (if-not (= first-act/system (get act :who))
    (words/could-not :send-as-another {:at :who})
    (let [full (act-id-of seam (get act :lane) uuid)]
      (if (:could-not full)
        full
        (sent seam act full (ends-after wait-ms))))))

(defn read-as-system
  "`softland.runtime.sign-in` is the one caller."
  [^Seam seam query]
  (if-not (= first-act/system (get query :asker))
    (words/could-not :send-as-another {:at :asker})
    (read-path/read-once (.-read-path seam) query)))

;; ---------------------------------------------------------------- the flows

(defn- live-now
  "The caller must have a who. One read of Auth. The caller proved its
  secret when it was made; what is read here is whether its sign-in and its
  session still stand. So a caller outlives a replacing of its secret, as
  its session does."
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
  "`until` is the end that is held for her sign-in, and `value` what Auth
  pushed as live under her root. True is a reason to read, and no judgment."
  [^Caller caller until value]
  (let [sign-in (get-in value [:sign-ins (.-sign-in caller)])]
    (or (nil? sign-in)
        (not= until (:until sign-in))
        (and (.-session caller)
             (not= (.-sign-in caller) (get-in value [:sessions (.-session caller) :sign-in]))))))

(defn standing
  "A flow that gives nothing while the caller is live, then one word,
  `:find-her-sign-in`, `:find-her-session` or `:reach-auth`, and ends. A
  visitor's flow gives nothing and never ends by itself. Cancelling it
  stops the listening.

  It listens for her root first and reads Auth after, so no change falls
  between the two. A push that does not show her as she is held is only a
  reason to read, since a push can be behind a write: the read decides. A
  read also comes when the time of her secret or of her session comes near,
  and it moves the session's time on when that is due. A secret that was
  replaced moves the sign-in's end; a session is never ended by that.

  When Auth's watch of her root is lost, the flow ends with `:reach-auth`
  and reads nothing: a caller that nothing watches could outlive its
  sign-in. The system connects again, and is watched anew.

  Each read, and the telling of a lost watch, runs on a thread of the
  seam's own: the standing's thread that hands it on returns at once, and
  one listener that blocks while it ends its connection holds back no
  other."
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
                  ;; Called under the lock. Gives the word when this is the
                  ;; first end, else nil.
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
  "A flow of the read path's own messages
  (`softland.store.read-path/read-live`), with `{:ended word}` once, as the
  last thing. The read is opened when the flow is first listened to, on a
  thread of the seam's own, and left when the flow is cancelled or has
  ended. A query that names another asker than the caller, and a read that
  could not be opened, give `{:ended word}` and nothing else.

  The seam does not end a watch when its caller stops being live: the face
  holds `standing` beside its watches and cancels them, so that one
  listening serves every watch of a connection.

  What waits for a slow reader is merged by place
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
