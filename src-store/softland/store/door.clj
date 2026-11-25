(ns softland.store.door
  "The door: the one piece of code by which a maker's act goes into the
  store and its answer comes back. Of Rama it uses four calls of the client:
  `foreign-depot`, `foreign-query`, `foreign-append!` and
  `foreign-invoke-query`.

  A gate is the authority. The door checks nothing of the act's form: it
  tests what its own additions need and nothing else, and anything can
  append to a depot without the door. So an act that a gate will refuse is
  appended, is refused under its own act-id with the gate's word, and leaves
  no fact in the record. A refused act sent in a loop leaves one record on a
  depot for each try, and one answer for each act-id that it is sent under.
  A value of any size that Rama's client can write is appended whole; a gate
  refuses a value over its bound, and the log keeps it. Nothing paces a
  sender.

  Everything goes plain into a depot: the maker's act as she gave it, with
  the act-id, the depot and the time added. A depot keeps the thing for
  good, whatever a gate decides of it, and any client of the cluster that
  reads a depot reads each value.

  One kind of value is not kept as given: a string that holds half of a
  surrogate pair alone. Rama's writing of a record puts `?` in the place of
  the half. The digest is taken of UTF-8, which makes the same replacement,
  so the door's digest is still a gate's and the maker reads her answer as
  her own; she is not told of the replacement.

  The sender. The door does not check that the sender is the who that the
  act names, and neither does a gate: that is the sending program's to
  hold, in front of the door, and none is built
  (src-store/softland/store/README.md, \"What the gate does not check, said
  plainly\"). `:system` is a who like any other.

  A word is one of the door's own seven (`softland.store.door.words`): a
  map that holds `:could-not`. No call throws for what a maker gave. How far
  that reaches: a thing that has no end does not come back (`send-act`), and
  a count of act-ids that the memory of the process does not hold ends in
  the JVM's own error (`act-ids-for`).

  The door keeps nothing between two calls: no act-id that it gave, no
  thing that it sent, no digest, no time, no lane's depot. So any opened
  door serves any maker, and an act-id and a digest that one door gave read
  the answer through another, in this process or a later one. It reads the
  answer of an act from the record, by the query, and never from a log. It
  borrows four handles of Rama's client; the opener owns the cluster
  handle, and nothing closes the door."
  {:softland/whose-call
   [{:state :not-decided
     :what "Sign-in, in front of the door, is an open row of the queue, and
            the door decides nothing of it: it stands before this code runs,
            and changes none of it."
     :source "K7 to K10, Q16"}
    {:state :not-decided
     :what "Who may act as the system is an open row of the queue, and the
            door decides nothing of it: `:system` is treated as any who."
     :source "K9"}]}
  (:require [com.rpl.rama :as rama]
            [softland.store.door.additions :as additions]
            [softland.store.door.digest :as digest]
            [softland.store.door.wait :as wait]
            [softland.store.door.words :as words]
            [softland.store.gate.first-act :as first-act]
            [softland.store.record.rows :as record])
  (:import [java.io Writer]
           [java.util UUID]))

;; --------------------------------------------------------- the door as a value

(defn- printed
  "The text that a door prints as. No handle is in it, whatever prints the
  door. That is care and no guard: code of the same process can reach a
  field of any object."
  [module]
  (str "#<door " module ">"))

(deftype Door [module
               streaming-depot
               microbatch-depot
               answer-query
               depot-query
               numbers]
  Object
  (toString [_] (printed module)))

(defmethod print-method Door [^Door door ^Writer w]
  (.write w (.toString door)))

(defmethod print-dup Door [^Door door ^Writer w]
  (.write w (.toString door)))

(alter-meta! #'->Door assoc
             :private true
             :doc "Makes an opened door: the module's name, four borrowed handles
  of Rama's client, and its numbers.")

;; ------------------------------------------------ the calls of Rama's client

(defn- what-threw
  "When the throw is an `InterruptedException`, the thread's interrupt mark
  is set again first: Rama's client clears the mark when it throws, the door
  turns the throw into a word, and without the mark the maker's thread would
  not know that it was interrupted."
  [^Throwable t]
  (when (instance? InterruptedException t)
    (.interrupt (Thread/currentThread)))
  {:threw (.getName (class t)) :said (.getMessage t)})

(defn- attempt
  "Every call of Rama's client that the door makes after its opening goes
  through here, so none of them throws out of a call of the door."
  [f]
  (try [:ok (f)]
       (catch Throwable t [:threw (what-threw t)])))

(defn- append!
  "One round trip. `depot` is the second slot of the thing's act-id.

  The streaming depot is appended to with the ack level :ack: the call
  returns when the streaming gate has processed the thing and its rows are
  visible. The microbatch depot with :append-ack: the call returns when the
  depot holds the thing. No stream topology reads that depot, so :ack would
  wait for nothing more, and the level says what is meant. The microbatch
  gate decides the thing in a batch of its own time, which the append does
  not wait for. Either return is a signal and never the answer.

  An append that threw may have gone through. Rama holds a caller in one
  append for as long as its client's own time limit
  (`foreign.depot.operation.timeout.millis`); the door sets none of Rama's
  settings."
  [^Door door depot thing]
  (let [[outcome got] (attempt (fn []
                                 (if (= :microbatch depot)
                                   (rama/foreign-append! (.-microbatch-depot door) thing :append-ack)
                                   (rama/foreign-append! (.-streaming-depot door) thing :ack))))]
    (if (= :threw outcome) [:threw got] [:ok])))

(defn- read-answer
  "One round trip, to one task, the task of the act-id's lane.

  Before it calls, it makes the test that the head of the query makes
  (`softland.store.record.rows/answer-read-by-routing-id`). For a thing
  that fails it, it makes no call and gives [:ok nil], which is what the
  query would give: a first argument that Rama's client cannot hash is
  reached by no proof."
  [^Door door act-id]
  (let [routing-id (record/routing-id act-id)]
    (if (record/answer-read-by-routing-id routing-id act-id)
      (attempt (fn [] (rama/foreign-invoke-query (.-answer-query door) routing-id act-id)))
      [:ok nil])))

(defn- depot-of
  "One round trip, to the lane's task. What must hold: the lane passed
  `softland.store.record.rows/depot-read`.

  A read that gives nil says that the lane is not made, and the depot is
  the one that the lane starts with. A read that threw has not said \"not
  made\", so it gives a word and no depot. In this build the read and the
  starting depot always agree, since no act changes a depot; the read is
  made all the same, and it is what leaves room for a lane whose depot can
  change. The door remembers no lane's depot between two calls: that would
  be a kept copy of what the cluster holds."
  {:softland/whose-call
   [{:state :not-decided
     :what "A lane whose depot changes is an open row of the queue, and the
            door decides nothing of it. The door reads the lane's depot for
            each act-id that it makes and remembers none, which is the room
            that it leaves."
     :source "?9"}]}
  [^Door door lane]
  (let [[outcome got] (attempt (fn [] (rama/foreign-invoke-query (.-depot-query door) lane)))]
    (if (= :threw outcome)
      [:word (words/word :read-the-lanes-depot (assoc got :lane lane))]
      [:ok (or got (first-act/depot-at-making lane))])))

;; ------------------------------------------------------------------ the clock

(defn- server-clock
  "The time on the act: milliseconds since 1970. It is another clock than
  the one that the waits count with (`after`): this one says a time of day
  and can go back when the server's clock is set."
  []
  (System/currentTimeMillis))

;; ------------------------------------------------------------------ the waits

(defn- after
  "A moment and a span, both in nanoseconds of the JVM's `nanoTime`. That
  clock never goes back, its numbers mean something only as differences,
  and they may wrap round the range of a long. So moments are added and
  compared without the overflow check; a span is
  `softland.store.door.wait/longest-wait-ms` at most, which keeps each
  difference in range."
  [moment span]
  (unchecked-add (long moment) (long span)))

(defn- left
  "Nanoseconds until a moment: zero or less when it has passed."
  [moment]
  (unchecked-subtract (long moment) (System/nanoTime)))

(defn- passed?
  [moment]
  (not (pos? (left moment))))

(defn- sleep-for
  "`span` is in nanoseconds; the sleep is a millisecond at least. False says
  that the thread was interrupted in the sleep; its interrupt mark is set
  again, and a wait that gets false ends as if its time had passed."
  [span]
  (try (Thread/sleep (long (max 1 (quot (+ span 999999) 1000000))))
       true
       (catch InterruptedException _
         (.interrupt (Thread/currentThread))
         false)))

(defn- read-answer-until
  "The wait. `ends` is the moment of the `nanoTime` clock at which the door
  stops starting reads; `answer-of` says how far that reaches. A read that
  threw ends the wait at once."
  [^Door door act-id ends]
  (let [every (* 1000000 (long (:read-every-ms (.-numbers door))))
        nap (fn [] (sleep-for (min every (left ends))))]
    (loop []
      (let [[outcome got] (read-answer door act-id)]
        (cond
          (= :threw outcome) [:threw got]
          (some? got) [:answer got]
          (passed? ends) [:nothing]
          (nap) (recur)
          :else [:nothing])))))

;; ------------------------------------------------------------------ the calls

(defn open
  "`module-name` is the name that Rama gives the module `Store`. `numbers`,
  when the opener wants, is a map with `:read-every-ms`
  (`softland.store.door.wait/default-numbers`). Gives a door, or the word
  `:find-the-module`; no door is then opened.

  Four round trips, once for a process and on no act's way. The door uses
  the handles from every maker's thread. A door holds nothing that a call
  changes, so a process can open one door or several. Opening checks
  nothing of who opens it."
  ([cluster module-name] (open cluster module-name nil))
  ([cluster module-name numbers]
   (try
     (->Door module-name
             (rama/foreign-depot cluster module-name "*streaming-depot")
             (rama/foreign-depot cluster module-name "*microbatch-depot")
             (rama/foreign-query cluster module-name "answer-by-act-id")
             (rama/foreign-query cluster module-name "depot-of-lane")
             (wait/numbers numbers))
     (catch Throwable t
       (words/word :find-the-module (assoc (what-threw t) :module module-name))))))

(defn act-id-for
  "An act-id for a lane, ahead of sending: [lane depot uuid], or a word.

  The uuid is a random one of the JVM's. The door keeps no id that it gave:
  it gives no id twice only by the chance of a random uuid, and it cannot
  tell an id of its own from another.

  One round trip, to the lane's task; none for a lane that is no id within
  the limit. A maker that gives the act-id back to `send-act` pays no
  second read there."
  [^Door door lane]
  (if-not (record/depot-read lane)
    (words/word :make-an-act-id {:at :lane})
    (let [[outcome got] (depot-of door lane)]
      (if (= :word outcome)
        got
        [lane got (UUID/randomUUID)]))))

(defn act-ids-for
  "The act-ids of a series in one lane, before the first of them is sent.
  A maker may use an id of the series in a later act of it before anything
  is sent: as `:because-of`, and as the act-id of a fact id in `:replaces`
  and `:based-on`.

  One read of the lane's depot serves the whole series, where `act-id-for`
  would read once for each id, on the lane's task, whose one thread the
  streaming gate also needs.

  Limit: the count has no bound but the memory of the process. The whole
  vector is made before the call returns, so a count that the memory does
  not hold ends in the JVM's own error, which is no word of the door's."
  [^Door door lane n]
  (or (when-not (record/depot-read lane)
        (words/word :make-an-act-id {:at :lane}))
      (additions/series-word n)
      (let [[outcome got] (depot-of door lane)]
        (if (= :word outcome)
          got
          (vec (repeatedly n (fn [] [lane got (UUID/randomUUID)])))))))

(defn send-act
  "Takes a maker's act into its depot. `act-id` is the one that the maker
  asked for ahead, or nil. Gives {:act-id :digest :ack}, or one of the
  door's own words.

  `:digest` is the digest of the thing that was appended, taken without
  its time, as 64 hex characters: the digest that a gate keeps with its
  answer. `:ack` is a signal and never the answer
  (`softland.store.door.words/acks`): for a streaming lane the answer is
  there when the call returns, and a maker of the base reads with a wait
  (`answer-of`).

  An act-id that is given is used as it is, also when its depot slot is not
  the lane's depot; the thing then goes to the depot that it names and a
  gate refuses it there. The digest is taken after the act-id is made: it
  is of the thing with its act-id in it. A thing that throws while it is
  read ends in `:read-the-act`, and nothing is appended.

  When the append throws, the word is `:append-cleanly`, with the act-id
  and the digest: the door does not know whether the depot holds the thing.
  It is the one word of which \"nothing was appended\" cannot be said. The
  door sends nothing again; the maker reads the record.

  The door changes the act by its three additions and no more. It gives no
  value a normal form and makes no part a vector, so a maker who gives her
  facts as a list gets another digest than one who gives the same facts as
  a vector: the one text writes the two apart.

  The time on the act. `:door-time` is the clock of the server that runs
  the door, read once in a call. It is the one read of a clock on the way
  of an act through the store. No gate reads a clock: a gate's decisions
  are a function of its depot's order, so that a run again gives the same
  record. The time is a label that no rule reads, and the stamp holds no
  time (`softland.store.gate.stamp`). Two doors on two servers write two
  clocks, and nothing holds one against the other: a wrong clock gives a
  wrong label, and nothing else.

  A second send of one act under one act-id carries the clock of the
  second send. The digest leaves the time out, so both sends have one
  digest and read one answer as their own; a gate decides the first of the
  two to arrive, with the time of that one. A maker cannot hand the time of
  her first send in: an act that already holds a time ends in
  `:add-its-parts`.

  The order of two sends. When the call returns, its append is over: it was
  acked or it threw. So two sends that one maker makes one after the other
  reach the depot in that order. A lane is one partition of its depot, in
  the streaming depot and in the microbatch depot. So the order of the two
  sends is the order of the two decisions, without a failure, and a maker
  need not read the answer of the first before she sends the second. Each
  gate's namespace says what a failure does
  (`softland.store.module.streaming-gate`,
  `softland.store.module.microbatch-gate`).

  The call runs on the maker's thread and holds it until it returns. A
  thread whose interrupt mark is set when a call of Rama's client is made
  gets that call's throw at once, as the word of that call, with its mark
  set again; such an append can have gone through.

  Limits. A thing that has no end in any part does not come back: the
  digest's walk goes through the whole thing. Such a call holds its maker's
  thread there, and nothing of the door that another maker waits for. The
  door has no bound on the count of facts, on the size of a value or on how
  deep a thing goes: each is a gate's to judge, of a thing that is on its
  depot by then."
  {:softland/whose-call
   [{:state :decided
     :what "The time on the act is a label that no rule reads: the stamp is
            the lane's count of decisions and holds no time."
     :source "`softland.store.gate.stamp`; Sid's decision of 14 October 2026"}
    {:state :not-decided
     :what "What a second send of the same act carries as its time is Sid's
            to say. Kept for now: it carries the door's clock of the second
            send. Another answer would be made in three places:
            `softland.store.door/send-act`, which reads the clock at every
            send; the door's test of its own parts
            (`softland.store.door.additions/reading`), which ends in
            `:add-its-parts` for an act that already holds a time, so a maker
            cannot hand the time of her first send in; and the digest's
            leaving the time out (`softland.store.door.digest`), which is
            what makes two sends with two times one thing."}]}
  ([door act] (send-act door act nil))
  ([^Door door act act-id]
   (let [reading (additions/reading act act-id)]
     (if (:could-not reading)
       reading
       (let [[outcome act-id] (if (some? act-id)
                                [:ok act-id]
                                (let [lane (:lane reading)
                                      [outcome got] (depot-of door lane)]
                                  (if (= :word outcome)
                                    [:word got]
                                    [:ok [lane got (UUID/randomUUID)]])))]
         (if (= :word outcome)
           act-id
           (let [built (additions/thing-to-append reading act-id (server-clock))]
             (if (:could-not built)
               built
               (let [thing (:thing built)
                     depot (nth act-id 1)
                     digest (digest/readable-hex-of thing)]
                 (if (nil? digest)
                   (words/word :read-the-act nil)
                   (let [[outcome threw] (append! door depot thing)]
                     (if (= :threw outcome)
                       (words/word :append-cleanly (assoc threw :act-id act-id :digest digest))
                       {:act-id act-id
                        :digest digest
                        :ack (get words/acks depot)}))))))))))))

(defn answer-of
  "`digest` is the one that `send-act` gave. `bound-ms`, when the maker
  wants to wait, is a bound in milliseconds. Gives one of four answers
  (`softland.store.door.words/answer`), or the word `:read-the-answer` when
  a read threw: a read that failed has not said \"nothing yet\".

  The door compares two hex texts: it holds no thing to take a digest of.
  So the call gives the same for an act-id and a digest that a maker kept
  from a process before, or got through another door.

  \"Nothing yet\" does not mean refused, and it does not mean that nothing
  is on a depot. It is the answer in states that the door cannot tell
  apart: nothing was ever sent under the act-id; a thing is on its depot
  and not decided; a microbatch act is decided and its batch is not yet
  committed on the lane's task; the thing is a stray, which the door
  itself never sends; the thing given is no act-id, for which the door
  makes no read at all.

  \"Not mine\" is said only of a thing that differs from the one that the
  answer is of: a maker's own second send reads the answer as her own
  (`send-act`).

  The wait is one question about one act-id, with no subscription. With no
  bound, or one that is not above zero: one read. It ends at the first read
  that gives an answer, \"not mine\" too, or that throws. A thread that is
  interrupted in the sleep ends the wait as if the bound had passed, with
  its mark set again.

  How far the bound reaches. It says when the door stops starting reads:
  the last read starts at the bound and none after it. A read that is on
  its way then takes its own time, and Rama decides how long that is, by
  the module's option `topology.query.timeout.millis`; the door sets none
  of Rama's settings. So a wait can outlast its bound by one read that the
  cluster does not answer."
  ([door act-id digest] (answer-of door act-id digest 0))
  ([^Door door act-id digest bound-ms]
   (if-not (record/answer-read-by-routing-id (record/routing-id act-id) act-id)
     (words/answer nil digest)
     (let [bound (if (and (number? bound-ms) (try (pos? bound-ms) (catch Throwable _ false)))
                   (try (long (min bound-ms wait/longest-wait-ms)) (catch Throwable _ 0))
                   0)
           ends (after (System/nanoTime) (* 1000000 bound))
           [outcome got] (read-answer-until door act-id ends)]
       (case outcome
         :threw (words/word :read-the-answer (assoc got :act-id act-id))
         :answer (words/answer got digest)
         (words/answer nil digest))))))
