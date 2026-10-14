(ns softland.store.door
  "The door: the one piece of code by which a maker's act goes into the
  store and its answer comes back. It requires Rama, and of Rama only four
  calls of its client: `foreign-depot`, `foreign-query`, `foreign-append!`
  and `foreign-invoke-query`.

  A maker is whatever makes an act: a screen, a converter, an agent. A maker
  gives the door its act in the one form (`softland.store.act`), with plain
  values. The door makes an act-id; adds to the act the act-id, the depot
  that it names, and the time of the clock of the server that runs the
  door; appends the thing to that depot; and gives the maker
  the act-id, the digest of what it sent, and the ack as a signal. The
  maker reads the answer back by the act-id and the digest. A gate is the
  authority: the door checks nothing of the act's form, and anything can
  still append to a depot without the door.

  The calls, each of which but `open` takes the door first:

    open         a cluster handle, the module's name, and when the opener
                 wants, a map with the door's one number -> a door, or a
                 word
    act-id-for   a layer -> an act-id [layer depot uuid], or a word
    act-ids-for  a layer and a count -> a vector of that many act-ids of
                 the layer, made with one read, or a word
    send-act     a maker's act, and when the maker asked for one ahead, its
                 act-id -> {:act-id :digest :ack}, or a word
    answer-of    an act-id, the digest that `send-act` gave, and when the
                 maker wants to wait, a bound in milliseconds -> one of
                 four answers, or a word

  A word is one of the door's own seven (`softland.store.door.words`): a
  map that holds `:could-not`. It says what the door could not do, and
  whether nothing was appended for the act or the door does not know. No
  call throws for what a maker gave. How far that reaches: a thing that has
  no end does not come back (`send-act`, \"Limits\"), and a count of
  act-ids that the memory of the process does not hold ends in the JVM's
  own error (`act-ids-for`).

  State. The door owns no state: none that lasts, and none of the process.
  An opened door borrows four handles of Rama's client, the two depots of
  acts and the two queries of the module `Store`; the opener owns the
  cluster handle, and nothing closes the door. Those two depots are all
  the depots of the module (`softland.store.module`). The door reads the
  answer of an act from the record, by the query, and never from a log.
  The door holds one number, fixed at the opening: how long a wait sleeps
  between two reads (`softland.store.door.wait`). It keeps nothing between
  two calls: no act-id that it gave, no thing that it sent, no digest, no
  time that it wrote, and no layer's depot.

  The clocks. The door reads two clocks of its JVM. For the time on an act
  it reads the clock of the server that runs it, once for each thing that
  it builds (`send-act`, `server-clock`). For a wait it counts with
  `nanoTime` (`after`). It sets neither.

  So any opened door serves any maker. An act-id and a digest that one door
  gave read the answer through another, in this process or a later one. In
  the runtime the module has two clients: the door for the way in, and the
  read path (`softland.store.read-path`) for the way out. Neither calls the
  other.

  What it requires outside the door's own folder, and why. Each is a pure
  function that holds one rule that the door needs, so that no rule is
  written a second time. `softland.store.gate.first-act/depot-at-making`:
  the depot that a layer starts with, for a layer that is not made. From
  `softland.store.record.rows`: `routing-id`, the first argument of the
  query of an answer, which is the act-id's layer for both depots; and
  `depot-read` and `answer-read-by-routing-id`, the tests that the head of
  each query makes of what a caller passes, which a caller that may hold a
  thing that is no id makes itself before it calls.
  The door opens no PState, makes no row and no text of a row.

  The sender. The door takes `:who`, `:session` and `:permission` as the
  maker gives them. It cannot check that the sender is the who that the act
  names, and does not. Neither can a gate. `:system` is a who like any
  other. The door makes no person and no id for one.

  What goes plain into a depot through the door. Everything. The thing that
  the door appends is the maker's act as she gave it, with the act-id, the
  depot and the time added: each value, each id, `:facts` as the vector or the list
  that it is, and each part that the form does not know
  (`softland.store.door.additions`). The door gives no value a normal form.
  A depot keeps the thing for good, whatever a gate decides of it, and any
  client of the cluster that reads a depot reads each value.

  One kind of value is not kept as given: a string that holds half of a
  surrogate pair alone. Rama's writing of a record puts `?` in the place of
  the half. The digest is taken of UTF-8, which makes the same replacement,
  so the door's digest is still a gate's and the maker reads her answer as
  her own; she is not told of the replacement.

  What the door can cost when it checks nothing. The door stops no act: it
  tests what its additions need and nothing else. So each of these is
  appended, is refused by a gate under its own act-id with the gate's word,
  and leaves no fact in the record: an act of an unknown version; an act
  with no session; an act under a permission of another layer, under one
  that does not stand, under one that says see, or under one whose holder
  is not the act's who; an act into a layer that is not made; and an act
  whose act-id names the other gate's depot. A refused act sent in a loop
  leaves one record on a depot for each try, and one answer for each act-id
  that it is sent under. A value of any size that Rama's client can write is appended
  whole; a gate refuses a value over its bound, and the log keeps it.
  Nothing paces a sender.

  Room that the door leaves, each an open row of the queue that this code
  decides nothing of:

    - a layer whose depot changes (?9): the door reads the layer's depot
      for each act-id that it makes and remembers none;
    - sign-in, in front of the door (K7 to K10, Q16): it would refuse a
      sender before this code runs, and changes none of it;
    - who may act as the system (K9): `:system` is treated as any who.

  The door reads no fact of the record for a maker, and its wait is one
  question about one act-id, with no subscription. A read's steps run in the
  read path (`softland.store.read-path`), which is also what tells a caller
  that a fact changed.

  Approach. The file has these sections, in this order: the door as a value;
  the calls of Rama's client; the clock; the waits; the five calls."
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
  "Takes the name of the module that a door is opened on. Gives the text
  that the door prints as: the word \"door\" and that name, and nothing
  else. No handle is in it, whatever prints the door: `str`, `pr`,
  `println` or a pretty-printer. That is care and no guard: code of the
  same process can reach a field of any object."
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
             :doc "Makes an opened door, an object of a type of its own.
  `open` is the one caller. A door holds six things: the module's name; the
  four handles of Rama's client, borrowed, which are the two depots of acts
  and the two queries; and its numbers (`softland.store.door.wait`). It
  prints as `printed` says, by the type's two print methods and its
  `toString`.")

;; ------------------------------------------------ the calls of Rama's client

(defn- what-threw
  "Takes what a call of Rama's client threw. Gives {:threw the name of its
  class, :said its message}, for a word to carry. When it is an
  `InterruptedException`, the thread's interrupt mark is set again first:
  Rama's client clears the mark when it throws, the door turns the throw
  into a word, and without the mark the maker's thread would not know that
  it was interrupted."
  [^Throwable t]
  (when (instance? InterruptedException t)
    (.interrupt (Thread/currentThread)))
  {:threw (.getName (class t)) :said (.getMessage t)})

(defn- attempt
  "Calls a function of no argument that makes one call of Rama's client.
  Gives [:ok what it gave], or [:threw what `what-threw` gives]. Every call
  of the client that the door makes after its opening goes through here, so
  none of them throws out of a call of the door."
  [f]
  (try [:ok (f)]
       (catch Throwable t [:threw (what-threw t)])))

(defn- append!
  "Appends a thing to the depot that a depot word names: one round trip.
  Takes the door, the depot word, which is the second slot of the thing's
  act-id, and the thing. Gives [:ok] or [:threw info].

  The streaming depot is appended to with the ack level :ack: the call
  returns when the streaming gate has processed the thing and its rows are
  visible. The microbatch depot with :append-ack: the call returns when the
  depot holds the thing; no stream topology reads that depot, so :ack would
  wait for nothing more, and the level says what is meant. The microbatch
  gate decides the thing in a batch of its own time, which the append does
  not wait for. Either return is a signal and never the answer.

  How far a throw reaches: an append that threw may have gone through. Rama
  holds a caller in one append for as long as its client's own time limit
  (`foreign.depot.operation.timeout.millis`); the door sets none of Rama's
  settings."
  [^Door door depot thing]
  (let [[outcome got] (attempt (fn []
                                 (if (= :microbatch depot)
                                   (rama/foreign-append! (.-microbatch-depot door) thing :append-ack)
                                   (rama/foreign-append! (.-streaming-depot door) thing :ack))))]
    (if (= :threw outcome) [:threw got] [:ok])))

(defn- read-answer
  "Reads the answer under an act-id: one round trip, the query
  `answer-by-act-id`, to one task, the task of the act-id's layer. Takes
  the door and an act-id. Gives [:ok the answer or nil] or [:threw info].
  The first argument of the query is the act-id's routing id
  (`softland.store.record.rows/routing-id`): its layer, for a streaming
  act-id and for a microbatch one.

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
  "Reads the depot of a layer: one round trip, the query `depot-of-layer`,
  to the layer's task. Takes the door and a layer that passed
  `softland.store.record.rows/depot-read`. Gives [:ok the depot word], or
  [:word `:read-the-layers-depot`] when the read threw.

  When the read gives nil the layer is not made, and the depot is the one
  that the layer starts with
  (`softland.store.gate.first-act/depot-at-making`). A read that threw has
  not said \"not made\", so it gives a word and no depot. In this build the
  read and the starting depot always agree, since no act changes a depot;
  the read is made all the same, and it is what leaves room for a layer
  whose depot can change (?9, not decided). The door remembers no layer's
  depot between two calls: that would be a kept copy of what the cluster
  holds."
  [^Door door layer]
  (let [[outcome got] (attempt (fn [] (rama/foreign-invoke-query (.-depot-query door) layer)))]
    (if (= :threw outcome)
      [:word (words/word :read-the-layers-depot (assoc got :layer layer))]
      [:ok (or got (first-act/depot-at-making layer))])))

;; ------------------------------------------------------------------ the clock

(defn- server-clock
  "Reads the clock of the server that runs the door: the JVM's
  `currentTimeMillis`, milliseconds since 1970. `send-act` is the one
  caller, once for a thing that it builds, and what this gives is the time
  on the act. It is another clock than the one that the waits count with
  (`after`): this one says a time of day and can go back when the server's
  clock is set."
  []
  (System/currentTimeMillis))

;; ------------------------------------------------------------------ the waits

(defn- after
  "Takes a moment of the clock and a span, both in nanoseconds. Gives the
  moment that is the span later. The clock is the JVM's `nanoTime`: it never
  goes back, its numbers mean something only as differences, and they may
  wrap round the range of a long. So moments are added and compared without
  the overflow check; a span is `softland.store.door.wait/longest-wait-ms`
  at most, which keeps each difference in range."
  [moment span]
  (unchecked-add (long moment) (long span)))

(defn- left
  "Takes a moment of the clock. Gives how many nanoseconds are left until
  it: zero or less when it has passed."
  [moment]
  (unchecked-subtract (long moment) (System/nanoTime)))

(defn- passed?
  "Takes a moment of the clock. Gives true when it has passed."
  [moment]
  (not (pos? (left moment))))

(defn- sleep-for
  "Sleeps on the caller's thread for a span in nanoseconds, a millisecond at
  least. Gives true; or false when the thread was interrupted in the sleep,
  with the thread's interrupt mark set again. A wait that gets false ends as
  if its time had passed."
  [span]
  (try (Thread/sleep (long (max 1 (quot (+ span 999999) 1000000))))
       true
       (catch InterruptedException _
         (.interrupt (Thread/currentThread))
         false)))

(defn- read-answer-until
  "The wait: reads the answer under one act-id until a read gives one. Takes
  the door; an act-id; and `ends`, the moment of the clock at which the wait
  ends. Gives one of:

    [:answer what the read gave]   a read gave an answer
    [:threw info]                  a read threw; the wait ends at once
    [:nothing]                     no answer, and the wait is over

  It reads; while a read gives nil and `ends` has not passed, it sleeps for
  `:read-every-ms`, or for what is left of the wait when that is less, and
  reads again. So the last read starts at `ends` and none starts after it.
  A thread that is interrupted in the sleep ends the wait as [:nothing].

  How far `ends` reaches: it says when the door stops starting reads. A
  read that is on its way then takes its own time, up to the time limit of
  a query in Rama (the module's option `topology.query.timeout.millis`).
  The clock is the JVM's `nanoTime`, which never goes back (`after`)."
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
  "Opens the door. Takes a cluster handle of Rama's and the name that Rama
  gives the module `Store`; and, when the opener wants, a map with the
  door's one number, `:read-every-ms`
  (`softland.store.door.wait/default-numbers` says what it is, with its
  default and its reason). Gives a door, or the word `:find-the-module`
  when the cluster gave no depot or no query of the module under that name;
  no door is then opened.

  Four round trips, once for a process and on no act's way: one fetch of a
  client for each of the module's two depots and its two queries. The door
  borrows the handles and uses them from every maker's thread; the opener
  owns the cluster handle, and nothing closes the door.

  A door holds nothing that a call changes, so a process can open one door
  or several, and each serves every maker alike. Opening checks nothing of
  who opens it."
  ([cluster module-name] (open cluster module-name nil))
  ([cluster module-name numbers]
   (try
     (->Door module-name
             (rama/foreign-depot cluster module-name "*streaming-depot")
             (rama/foreign-depot cluster module-name "*microbatch-depot")
             (rama/foreign-query cluster module-name "answer-by-act-id")
             (rama/foreign-query cluster module-name "depot-of-layer")
             (wait/numbers numbers))
     (catch Throwable t
       (words/word :find-the-module (assoc (what-threw t) :module module-name))))))

(defn act-id-for
  "Gives an act-id for a layer, ahead of sending. Takes the door and a
  layer. Gives [layer depot uuid], or a word.

  The depot is the layer's depot as the module gives it, or the depot that
  the layer starts with when the layer is not made (`depot-of`). The uuid
  is a random one of the JVM's, made from nothing of the act, the maker, the
  person or the time. The door keeps no id that it gave, so it gives no id
  twice only by the chance of a random uuid being none, and it cannot tell
  an id of its own from another.

  One round trip, to the layer's task; none for a layer that is no id
  within the limit, which ends in `:make-an-act-id` at `:layer`. A read
  that threw ends in `:read-the-layers-depot`. A maker that gives the
  act-id back to `send-act` pays no second read there."
  [^Door door layer]
  (if-not (record/depot-read layer)
    (words/word :make-an-act-id {:at :layer})
    (let [[outcome got] (depot-of door layer)]
      (if (= :word outcome)
        got
        [layer got (UUID/randomUUID)]))))

(defn act-ids-for
  "Gives the act-ids of a series in one layer, before the first of them is
  sent. Takes the door, a layer, and a count, a fixed-width integer of 1 or
  more. Gives a vector of that many act-ids of the layer, each with a uuid
  of its own, or a word.

  A maker may use an id of the series in a later act of it before anything
  is sent: as `:because-of`, and as the act-id of a fact id in `:replaces`
  and `:based-on`. One read of the layer's depot serves the whole series,
  where `act-id-for` would read once for each id, on the layer's task,
  whose one thread the streaming gate also needs.

  The words are those of `act-id-for`, and `:make-an-act-id` at `:count`
  for a count that is no fixed-width integer of 1 or more. Limit: the count
  has no bound but the memory of the process. The whole vector is made
  before the call returns, so a count that the memory does not hold ends in
  the JVM's own error, which is no word of the door's."
  [^Door door layer n]
  (or (when-not (record/depot-read layer)
        (words/word :make-an-act-id {:at :layer}))
      (additions/series-word n)
      (let [[outcome got] (depot-of door layer)]
        (if (= :word outcome)
          got
          (vec (repeatedly n (fn [] [layer got (UUID/randomUUID)])))))))

(defn send-act
  "Takes a maker's act into its depot. Takes the door; the act, in the one
  form with plain values; and, when the maker asked for one ahead, its
  act-id (nil is none). Gives {:act-id :digest :ack}, or one of the door's
  own words.

  `:digest` is the digest of the thing that was appended, taken without
  its time (`softland.store.door.digest`), as 64 hex characters: the digest
  that a gate keeps with its answer. `:ack` is
  `:gate-ran` for the streaming depot and `:depot-holds` for the microbatch
  depot (`softland.store.door.words/acks`). It is a signal and never the
  answer: the maker reads the answer with `answer-of`. Once the call
  returns, the door keeps nothing of what it sent; the maker holds the
  act-id and the digest.

  The steps, in their order. A step that ends in a word ends the call. The
  tests of step 1 come before the first round trip. The walk of step 3
  comes after the read of step 2, when the door makes the act-id: the
  digest is of the thing with its act-id in it.

    1. The door reads the thing, with no round trip
       (`softland.store.door.additions/reading`): `:read-the-act`,
       `:add-its-parts`, `:make-an-act-id`.
    2. The act-id. Given: used as it is, also when its depot slot is not
       the layer's depot; the thing then goes to the depot that it names
       and a gate refuses it there. Not given: one read of the layer's
       depot, and a random uuid: `:read-the-layers-depot`.
    3. The door reads the clock of its server, once. The thing is built,
       the maker's map with `:act-id`, `:depot` and `:door-time` added, and
       its digest is taken by a walk that tells a throw
       (`softland.store.door.digest/readable-hex-of`). The digest leaves
       the time out. A thing that throws while it is read ends in
       `:read-the-act`, and nothing is appended.
    4. One append, to the depot that the act-id names. When it throws:
       `:append-cleanly`, with the act-id and the digest, since the door
       does not know whether the depot holds the thing. It is the one
       word of which \"nothing was appended\" cannot be said. The door
       sends nothing again; the maker reads the record.

  The door changes the act by its three additions and no more. It fills no
  missing part, reorders no fact, makes no part a vector, gives no value a
  normal form, and names no permission. So a maker who gives her facts as a
  list gets another digest than one who gives the same facts as a vector,
  since the one text writes the two apart.

  The time on the act. `:door-time` is the clock of the server that runs
  the door, in milliseconds since 1970, a fixed-width integer
  (`server-clock`). The door reads it once in a call, at step 3; a call
  that ends in a word before that step reads no clock. It is the one read
  of a clock on the way of an act through the store, in a streaming layer
  and in the base alike. No gate reads a clock: a gate's decisions are a
  function of its depot's order, so that a run again gives the same
  record. The time is a label. A gate puts it beside the stamp, in the
  answer and in each row of the act's facts, and no rule reads it: the
  stamp is the layer's count of decisions and holds no time
  (`softland.store.gate.stamp`; Sid's decision of 14 October 2026). Two
  doors on two servers write two clocks, and nothing holds one against
  the other: a wrong clock gives a wrong label, and nothing else.

  A second send. A second send of one act under one act-id is the same
  thing but for its time: it carries the door's clock of the second send,
  and the door does nothing special for it. The digest leaves the time
  out, so both sends have one digest and read one answer as their own; a
  gate decides the first of the two to arrive, with the time of that one.
  Not decided: kept for now. What a second send of the same act carries as
  its time is Sid's to say. Another answer would be made in three places:
  this call, which reads the clock at every send; the door's test of its
  own parts (`softland.store.door.additions/reading`), which ends in
  `:add-its-parts` for an act that already holds a time, so a maker cannot
  hand the time of her first send in; and the digest's leaving the time
  out (`softland.store.door.digest`), which is what makes two sends with
  two times one thing.

  The door stops no act. An act that a gate will refuse, under a permission
  that does not stand or into a layer that is not made, goes into its depot
  like any other, and its refusal is recorded under its own act-id.

  The order of two sends. When the call returns, its append is over: it was
  acked or it threw. So two sends that one maker makes one after the other
  reach the depot in that order. A layer is one partition of its depot, in
  the streaming depot and in the microbatch depot: the base is one line.
  So in a streaming layer and in the base alike the order of the two sends
  is the order of the two decisions, without a failure, and a maker need
  not read the answer of the first before she sends the second. Each gate's
  namespace says what a failure does
  (`softland.store.module.streaming-gate`,
  `softland.store.module.microbatch-gate`).

  When the answer is there. For a streaming layer, when `send-act` returns
  with its ack: the gate has run. For the base, when the batch that decided
  the act is committed on the base's task; the ack says only that the depot
  holds the thing, so a maker of the base reads with a wait
  (`answer-of`).

  The call runs on the maker's thread and holds it until it returns. A
  thread whose interrupt mark is set when a call of Rama's client is made
  gets that call's throw at once, as the word of that call, with its mark
  set again; such an append can have gone through.

  Limits. A thing that has no end in any part does not come back: facts
  with no end, an entity, a who, a session, a permission, a part that the
  form does not know. The digest's walk goes through the whole thing, and
  such a call holds its maker's thread there; it holds nothing of the door
  that another maker waits for. The door has no bound on the count of
  facts, on the size of a value or on how deep a thing goes: each is a
  gate's to judge, of a thing that is on its depot by then."
  ([door act] (send-act door act nil))
  ([^Door door act act-id]
   (let [reading (additions/reading act act-id)]
     (if (:could-not reading)
       reading
       (let [[outcome act-id] (if (some? act-id)
                                [:ok act-id]
                                (let [layer (:layer reading)
                                      [outcome got] (depot-of door layer)]
                                  (if (= :word outcome)
                                    [:word got]
                                    [:ok [layer got (UUID/randomUUID)]])))]
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
  "Reads the answer of an act back. Takes the door; an act-id; the digest
  that `send-act` gave for the thing sent under it; and, when the maker
  wants to wait, a bound in milliseconds. Gives one of four answers
  (`softland.store.door.words/answer`), or the word `:read-the-answer` when
  a read threw: a read that failed has not said \"nothing yet\".

    {:answer :accepted :stamp s :time t}      the record's answer is of the
                                              thing with that digest
    {:answer :refused :reason word :stamp s :time t}
                                              the same, with the gate's
                                              reason word; no `:time` when
                                              the thing held no usable one
    {:answer :nothing-yet}                    the read gave nothing
    {:answer :not-mine}                       the record holds an answer
                                              under the act-id with another
                                              digest; nothing more is said

  The door compares two hex texts: it holds no thing to take a digest of.
  The call needs nothing of the door but its handles, so it gives the same
  for an act-id and a digest that a maker kept from a process before, or
  got through another door. It writes nothing.

  \"Nothing yet\" does not mean refused, and it does not mean that nothing
  is on a depot. It is the answer in states that the door cannot tell
  apart: nothing was ever sent under the act-id; a thing is on its depot
  and not decided; a microbatch act is decided and its batch is not yet
  committed on the layer's task; the thing is a stray, which the door
  itself never sends; the thing given is no act-id, for which the door
  makes no read at all and comes back at once.

  A maker's own second send reads its answer as her own. A second send of
  one act under one act-id has the digest of the first, since a digest
  leaves the time on the act out (`send-act`): a gate answers the act-id
  once, for the first of the two to arrive, and both sends read that one
  answer. \"Not mine\" is said only of a thing that differs from the
  one that the answer is of. The door never makes one id twice; it does not
  refuse a second send.

  The wait. It is one question about one act-id, the same for a streaming
  act-id and a microbatch one, and it offers no subscription. With no
  bound, or one that is not above zero: one read. With a bound, the door
  reads at once, and while a read gives nothing and the bound has not
  passed, it sleeps for the door's `:read-every-ms`, or for what is left of
  the bound when that is less, and reads again. It ends at the first read
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
