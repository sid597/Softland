(ns softland.store.door
  "The door: the one piece of code by which a maker's act goes into the
  store and its answer comes back. It requires Rama, and of Rama only four
  calls of its client: `foreign-depot`, `foreign-query`, `foreign-append!`
  and `foreign-invoke-query`.

  A maker is whatever makes an act: a screen, a converter, an agent. A maker
  gives the door its act in the one form (`softland.store.act`), with plain
  values. The door makes an act-id, asks the store for locks by an act of
  its own, seals each value with a lock of its own, appends the thing to the
  depot that the act-id names, and gives the maker the act-id, the digest of
  what it sent, and the ack as a signal. The maker reads the answer back by
  the act-id and the digest. A gate is the authority: the door checks
  nothing of the act's form, and anything can still append to a depot
  without the door.

  The calls, each of which but `open` takes the door first:

    open         a cluster handle, the module's name, and when the opener
                 wants, a map of the door's five numbers -> a door, or a word
    act-id-for   a layer -> an act-id [layer depot uuid], or a word
    act-ids-for  a layer and a count -> a vector of that many act-ids of
                 the layer, made with one read, or a word
    send-act     a maker's act, and when the maker asked for one ahead, its
                 act-id -> {:act-id :digest :ack}, or a word
    answer-of    an act-id, the digest that `send-act` gave, and when the
                 maker wants to wait, a bound in milliseconds -> one of
                 four answers, or a word
    stock-count  nothing more -> three numbers about the locks that the
                 door holds

  A word is one of the door's own fourteen (`softland.store.door.words`): a
  map that holds `:could-not`. It says what the door could not do, and
  whether nothing was appended for the act or the door does not know. No
  call throws for what a maker gave.

  State. The door owns no durable state. An opened door borrows five handles
  of Rama's client, the two depots and the three queries of the module
  `Store`; the opener owns the cluster handle, and nothing closes the door.
  It holds its five numbers, fixed at the opening. Its one memory is its
  stocks of locks, the memory of one process, empty at each opening whatever
  the record holds from a process before. A stock is for one layer, one
  depot and one who (`softland.store.door.stock`). The door keeps no act-id
  that it gave, no thing that it sent and no digest once a call returns, and
  no layer's depot between two calls.

  A process opens one door. Two opened doors are two sets of stocks: each
  asks for itself and neither uses the other's locks. No row is wrong for
  it; rows are wasted. In the runtime the door is the module's only client.

  What the stocks cost. A lock that a stock holds when the process ends is a
  row of the record that seals nothing, for good: the door reads locks only
  of requests that it made itself in this process, and it never destroys a
  lock. The size rule (`softland.store.door.stock/ask-for`) keeps that
  small for each who. A stock that gave out a lock stays for the life of
  the process, so the door's memory grows with the whos that wrote a value
  through it: a few hundred bytes for each, and its unused locks. Nothing
  bounds that but the end of the process.

  What it requires outside the door's own folder, and why. Each is a pure
  function that holds one rule that the door needs, so that no rule is
  written a second time. `softland.store.gate.first-act/depot-at-making`:
  the depot that a layer starts with, for a layer that is not made. From
  `softland.store.record.rows`: `routing-id`, the first argument of two of
  the queries; and `depot-read`, `answer-read-by-routing-id` and
  `lock-range-read-by-routing-id`, the tests that the head of each query
  makes of what a caller passes, which a caller that may hold a thing that
  is no id makes itself before it calls. The door opens no PState, makes no
  row and no text of a row.

  The sender. The door takes `:who`, `:session` and `:permission` as the
  maker gives them. It cannot check that the sender is the who that the act
  names, and does not. Neither can a gate. `:system` is a who like any
  other. The door makes no person and no id for one.

  What goes plain into a depot through the door. The door seals the `:v` of
  a fact under a plain key, when the fact is a plain map in a sequence under
  `:facts`. Everything else that a maker gives is appended as given, and the
  depot keeps it for good: a value under a store key, each id, each part
  that the form does not know, and what a thing holds where the door finds
  no fact (`softland.store.door.additions`).

  What the door can cost when it checks nothing. The door as built is no
  guard of a gate. A refused act sent in a loop leaves one depot record for
  each try, and one lock that seals nothing for each of its values; with an
  empty stock it leaves a refused request for each try. An act of 256
  values, each just under the limit of sealed bytes, is sealed and appended
  whole. Facts with no end and fewer than 257 values to seal do not come
  back.

  Room that the door leaves, each an open row of the queue that this code
  decides nothing of:

    - a layer whose depot changes (?9): the door reads the layer's depot
      for each act-id that it makes and remembers none, and a stock is for
      one depot;
    - the switch of a layer to one lock for each act, and forget (K11, H10,
      H11): a lock is asked for under one who in one layer and is used only
      for values of that who in that layer, so a forget of a person can
      find her locks by her requests. The door never destroys a lock;
    - sign-in, in front of the door (K7 to K10, Q16): it would refuse a
      sender before this code runs, and changes none of it. A lock that was
      asked for in one session can seal a value of the same who in another;
    - who may act as the system (K9): `:system` is treated as any who;
    - where a read's steps run (W3), and what tells a screen that a fact
      changed (W4): the door reads no fact of the record for a maker, and
      its wait is one question about one act-id, with no subscription.

  Approach. The file has these sections, in this order: the door as a value;
  the calls of Rama's client; the stocks; the getting of locks; the six
  calls."
  (:require [com.rpl.rama :as rama]
            [softland.store.door.additions :as additions]
            [softland.store.door.digest :as digest]
            [softland.store.door.stock :as stock]
            [softland.store.door.words :as words]
            [softland.store.gate.first-act :as first-act]
            [softland.store.record.rows :as record])
  (:import [java.io Writer]
           [java.util UUID]
           [java.util.concurrent Semaphore TimeUnit]))

;; --------------------------------------------------------- the door as a value

(defn- printed
  "Takes the name of the module that a door is opened on. Gives the text
  that the door prints as: the word \"door\" and that name, and nothing
  else. No handle, no stock and no lock is in it, whatever prints the door:
  `str`, `pr`, `println` or a pretty-printer. That is care and no guard:
  code of the same process can reach a field of any object, and any client
  of the cluster can read a lock."
  [module]
  (str "#<door " module ">"))

(deftype Door [module
               streaming-depot
               microbatch-depot
               answer-query
               locks-query
               depot-query
               numbers
               stocks]
  Object
  (toString [_] (printed module)))

(defmethod print-method Door [^Door door ^Writer w]
  (.write w (.toString door)))

(defmethod print-dup Door [^Door door ^Writer w]
  (.write w (.toString door)))

(alter-meta! #'->Door assoc
             :private true
             :doc "Makes an opened door, an object of a type of its own.
  `open` is the one caller. A door holds the module's name; the five handles
  of Rama's client, borrowed; its five numbers; and its stocks, an atom of
  a map from [layer depot who] to {:calls :cell :seat} (`enter!`). It
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
  wait for nothing more, and the level says what is meant. Either return is
  a signal and never the answer.

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
  `answer-by-act-id`, to one task. Takes the door and an act-id. Gives
  [:ok the answer or nil] or [:threw info]. The first argument of the query
  is the act-id's routing id, the layer of a streaming act-id and the uuid
  of a microbatch one.

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

(defn- read-locks
  "Reads the locks of a request that this door sent: one round trip, the
  query `locks-of-request`, to the task of the request's answer. Takes the
  door, the request's act-id, and how many locks the request asked for, n.
  Gives [:ok a map from index to {:bytes :way}] or [:threw info]. The range
  is exactly what the request asked for, the indexes 0 to n - 1, so the
  door knows when a read gave fewer locks than the request made.

  Before it calls, it makes the test that the head of the query makes
  (`softland.store.record.rows/lock-range-read-by-routing-id`), as
  `read-answer` does; a request of the door's own making always passes it."
  [^Door door request-act-id n]
  (let [routing-id (record/routing-id request-act-id)]
    (if (record/lock-range-read-by-routing-id routing-id request-act-id 0 (dec n))
      (attempt (fn [] (rama/foreign-invoke-query (.-locks-query door) routing-id request-act-id 0 (dec n))))
      [:ok {}])))

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

;; ------------------------------------------------------------------ the waits

(defn- after
  "Takes a moment of the clock and a span, both in nanoseconds. Gives the
  moment that is the span later. The clock is the JVM's `nanoTime`: it never
  goes back, its numbers mean something only as differences, and they may
  wrap round the range of a long. So moments are added and compared without
  the overflow check; a span is `softland.store.door.stock/longest-wait-ms`
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
  the door; an act-id; `ends`, the moment of the clock at which the wait
  ends; whether to sleep once before the first read; and, for a request of
  the door's own that is in doubt, that open request, else nil. Gives one
  of:

    [:answer what the read gave]   a read gave an answer
    [:threw info]                  a read threw; the wait ends at once
    [:given-up]                    the request is in doubt, and a read that
                                   began the door's wait or more after its
                                   moment found nothing
                                   (`softland.store.door.stock/kept-long-enough?`)
    [:nothing]                     no answer, and the wait is over

  It reads; while a read gives nil and `ends` has not passed, it sleeps for
  `:read-every-ms`, or for what is left of the wait when that is less, and
  reads again. So the last read starts at `ends` and none starts after it.
  A thread that is interrupted in the sleep ends the wait as [:nothing].

  How far `ends` reaches: it says when the door stops starting reads. A
  read that is on its way then takes its own time, up to the time limit of
  a query in Rama (the module's option `topology.query.timeout.millis`).
  The clock is the JVM's `nanoTime`, which never goes back (`after`)."
  [^Door door act-id ends sleep-first? in-doubt]
  (let [every (* 1000000 (long (:read-every-ms (.-numbers door))))
        span (* 1000000 (long (:request-wait-ms (.-numbers door))))
        nap (fn []
              (sleep-for (cond-> (min every (left ends))
                           in-doubt (min (left (after (:since in-doubt) span))))))]
    (if (and sleep-first? (not (nap)))
      [:nothing]
      (loop []
        (let [began (System/nanoTime)
              [outcome got] (read-answer door act-id)]
          (cond
            (= :threw outcome) [:threw got]
            (some? got) [:answer got]
            (and in-doubt (stock/kept-long-enough? in-doubt began span)) [:given-up]
            (passed? ends) [:nothing]
            (nap) (recur)
            :else [:nothing]))))))

;; ----------------------------------------------------------------- the stocks

(defn- enter!
  "A call counts itself into the stock of [layer depot who], and makes the
  stock when the door has none for them. One atomic step of the door's map.
  Gives what the map holds for the stock: `:calls`, how many calls are
  counted in; `:cell`, an atom of the stock's value
  (`softland.store.door.stock`); `:seat`, the stock's seat.

  The seat is what lets one call at a time ask for this stock: a semaphore
  of one permit that gives the permit in the order of arrival, and that a
  thread can stop waiting for at a time limit. A call that got in must come
  out through `leave!`."
  [^Door door triple]
  (get (swap! (.-stocks door)
              (fn [stocks]
                (if-let [held (get stocks triple)]
                  (assoc stocks triple (update held :calls inc))
                  (assoc stocks triple {:calls 1
                                        :cell  (atom stock/nothing)
                                        :seat  (Semaphore. 1 true)}))))
       triple))

(defn- leave!
  "A call counts itself out of the stock of [layer depot who]. One atomic
  step of the door's map. In that step, when no call is counted in any more
  and the stock holds nothing at all
  (`softland.store.door.stock/holds-nothing?`), the stock leaves the map:
  it is the stock of a who whose every request was refused, and a maker
  that sends value acts under whos that do not stand then grows no memory
  in the door.

  Whether the stock holds nothing is read inside the step, after the count
  has come to none, and never before it. Only a call that is counted in can
  change a stock. A call that read \"nothing\" first and counted itself out
  later could take away a stock that another call had filled in between,
  and its locks would be lost with no word and no count. A stock that holds
  an open request stays, until a later call of the same layer, depot and
  who reads that request or forgets it."
  [^Door door triple]
  (swap! (.-stocks door)
         (fn [stocks]
           (let [held (get stocks triple)
                 calls (dec (:calls held))]
             (if (and (zero? calls) (stock/holds-nothing? @(:cell held)))
               (dissoc stocks triple)
               (assoc stocks triple (assoc held :calls calls))))))
  nil)

(defn- take!
  "Takes the locks of one act from a stock's cell, by compare-and-set. Takes
  the cell and a count, m. Gives the locks, a vector of m, or nil when the
  stock holds fewer: all m leave the stock together, or none does. It never
  waits and needs no seat, so two takes at once cannot give one lock twice,
  and a call whose act the stock can serve never waits behind a call that
  asks."
  [cell m]
  (loop []
    (let [before @cell]
      (when-let [[after locks] (stock/take-locks before m)]
        (if (compare-and-set! cell before after)
          locks
          (recur))))))

;; ------------------------------------------------------ the getting of locks

(defn- waited-word
  "The word `:get-locks-within-its-wait`. Takes the moment at which the
  call's wait began, and the act-id of the request that the call read or
  sent, or nil for none."
  [began request-act-id]
  (words/word :get-locks-within-its-wait
              (cond-> {:waited-ms (quot (unchecked-subtract (System/nanoTime) (long began)) 1000000)}
                request-act-id (assoc :request request-act-id))))

(defn- settle
  "The call in the seat reads the stock's open request to its end. Takes the
  door; the stock's cell; the open request, as the cell holds it; the depot
  word; how many locks the call's act needs, m; the moment at which the
  call's wait began and the moment at which it ends; and whether this call
  sent the request itself. Gives one of:

    {:locks the act's locks}   the request's locks were read and the act's
                               own were taken, in one step
    {:word a word}             the call ends; the request stays open as it
                               was
    {:ended :short}            the locks were read and are in the stock,
                               which is still short of m; the request is
                               ended
    {:ended :no-accept :answer a}  the record's answer to the request is no
                               accept, a refusal or an answer of another
                               thing; the request is forgotten
    {:ended :given-up}         the request was in doubt with no answer for
                               long enough; it is forgotten

  The order is the contract's: the answer by act-id, with its digest held
  against the digest of the request as sent, and then the locks, each one
  round trip. The stock keeps no answer that was read, so after a read of
  locks that threw, the next call reads the answer again.

  The first read of the answer comes at once, but for a request of a
  microbatch layer that this call has just sent: its answer is readable
  only when its batch is closed, so a read at once is a round trip that
  can find nothing, and the first read comes after one interval.

  No step is started after the call's wait has ended: not the read of the
  locks either, and the request then stays open for the next call. The
  words: `:read-its-requests-answer` when a read of the answer threw;
  `:get-locks-within-its-wait` when the wait ended; and
  `:read-its-requests-locks`, with the class of the throw and no message,
  when the read of the locks threw."
  [^Door door cell open depot m began ends own?]
  (let [request-act-id (:act-id open)
        in-doubt (when (= :in-doubt (:state open)) open)
        [outcome got] (read-answer-until door request-act-id ends
                                         (and own? (= :microbatch depot))
                                         in-doubt)]
    (case outcome
      :threw
      {:word (words/word :read-its-requests-answer (assoc got :request request-act-id))}

      :nothing
      {:word (waited-word began request-act-id)}

      :given-up
      (do (swap! cell stock/without-request)
          {:ended :given-up})

      :answer
      (let [answer (words/answer got (:digest open))]
        (cond
          (not= :accepted (:answer answer))
          (do (swap! cell stock/without-request)
              {:ended :no-accept :answer answer})

          (passed? ends)
          {:word (waited-word began request-act-id)}

          :else
          (let [[outcome got] (read-locks door request-act-id (:count open))]
            (if (= :threw outcome)
              {:word (words/word :read-its-requests-locks {:request request-act-id
                                                           :threw (:threw got)})}
              (loop []
                (let [before @cell
                      [after locks] (stock/with-locks-read before request-act-id got m)]
                  (if (compare-and-set! cell before after)
                    (if locks {:locks locks} {:ended :short})
                    (recur)))))))))))

(defn- ask
  "The call in the seat sends its own request for locks, and reads it to its
  end. Takes the door, the stock's cell, the reading of the maker's act, the
  depot word, how many locks the act needs, and the two moments of the
  call's wait. Gives {:locks the act's locks} or {:word a word}.

  The request is an act (`softland.store.door.additions/request`): under
  the maker's who and permission, in the act's layer, with an act-id of its
  own in the depot of the act's act-id, asking for as many locks as the size
  rule gives (`softland.store.door.stock/ask-for`). It is written into the
  stock before it is appended, as in doubt. An append that returned makes it
  sent. An append that threw leaves it in doubt from the moment of the
  throw, and the call ends in `:append-its-request-cleanly`: the door sends
  nothing a second time.

  A maker is handed the refusal only of a request that its own call sent,
  which is this one: `:have-its-request-accepted`, with the request's
  act-id and the request's answer. The request carries the act's own who,
  permission, session and version by the way that it is made. When the
  request's locks were read and the stock is still short, locks of it
  cannot seal, because it asked for as many as the act needs: the word is
  `:read-its-requests-locks`, and no second request is sent."
  [^Door door cell reading depot m began ends]
  (let [request-act-id [(:layer reading) depot (UUID/randomUUID)]
        n (stock/ask-for (.-numbers door) depot m (:given-out @cell))
        request (additions/request reading request-act-id n)]
    (swap! cell stock/with-request
           {:act-id request-act-id :digest (digest/hex-of request) :count n}
           (System/nanoTime))
    (let [[outcome got] (append! door depot request)]
      (if (= :threw outcome)
        (do (swap! cell stock/request-in-doubt-since (System/nanoTime))
            {:word (words/word :append-its-request-cleanly (assoc got :request request-act-id))})
        (let [open (:open (swap! cell stock/request-sent))
              settled (settle door cell open depot m began ends true)]
          (case (:ended settled)
            :short
            {:word (words/word :read-its-requests-locks {:request request-act-id})}

            :no-accept
            {:word (words/word :have-its-request-accepted {:request request-act-id
                                                           :request-answer (:answer settled)})}

            settled))))))

(defn- in-the-seat
  "What the one call in a stock's seat does. Takes what `ask` takes. Gives
  {:locks the act's locks} or {:word a word}.

  First it takes again: the call that sat here before may have left locks.
  Then, when the stock holds an open request that an earlier call left, it
  reads that request to its end (`settle`). Then, when the stock is still
  short, it sends its own request (`ask`). So one call reads one earlier
  request at most and sends one request at most, and the four ways in which
  an earlier request ends without giving this act its locks (refused,
  another digest, too few locks, in doubt for long enough) all go on to the
  call's own request. The refusal of an earlier call's request is handed to
  nobody: that call was told at its own time what the door then knew.

  A step is started only while the call's wait has not ended; when it has,
  the word is `:get-locks-within-its-wait`."
  [^Door door cell reading depot m began ends]
  (if-let [locks (take! cell m)]
    {:locks locks}
    (let [earlier (:open @cell)
          met (when earlier (settle door cell earlier depot m began ends false))]
      (cond
        (or (:locks met) (:word met))
        met

        (passed? ends)
        {:word (waited-word began (:act-id earlier))}

        :else
        (ask door cell reading depot m began ends)))))

(defn- locks-for
  "Gets the locks of one act. Takes the door, the reading of the maker's
  act, the depot word of the act's act-id, and how many locks the act
  needs, m, 1 or more. Gives {:locks a vector of m} or {:word a word}.

  The stock is the one of the act's `:layer`, the depot slot of its act-id
  and its `:who`, each as given. A lock goes only to an act of that layer
  and that who; a gate holds a lock to its layer and its depot, and not to
  who asked for it, so the door alone holds this. With the depot in it, an
  act under an act-id whose depot slot is not the layer's depot finds a
  stock of its own, which is empty: its request goes to the same wrong
  depot and is refused, and no lock of the right depot is lost. `:who` may
  be anything; a who that is no id gets a stock that never fills, because a
  gate refuses its request. A `:who` that cannot be hashed ends in
  `:read-the-act`.

  Taking never waits. Only when a take comes back short does the call go
  for the stock's seat, so the door asks for locks only at need, inside the
  call of a maker whose act needs them, and one call at a time asks for one
  stock. If each maker that found the stock short sent its own request, a
  who that sends many acts into the base in the second that a request takes
  there would send as many requests, and the stock would have no bound.

  How long a maker waits. One time limit for all of a call's getting of
  locks: the door's `:request-wait-ms`, counted from the moment when the
  first take came back short. Waiting for the seat, reading an earlier
  call's request and waiting for its own request's answer all end at that
  moment, with `:get-locks-within-its-wait`. How far the limit reaches: it
  says when the door stops waiting for the seat and stops starting a step.
  A call of Rama's client that is on its way then takes its own time, and
  only the call in the seat can be in one. So a call that waits for the
  seat ends at the limit, and the call in the seat can outlast it by one
  append and by one query. A thread that is interrupted while it waits for
  the seat ends its wait as if the time had passed, with its mark set
  again.

  The seat is given up, and the call counts itself out of the stock, on
  every way out."
  [^Door door reading depot m]
  (let [triple [(:layer reading) depot (:who reading)]]
    (if-not (try (int? (hash triple)) (catch Throwable _ false))
      {:word (words/word :read-the-act {:at :who})}
      (let [{:keys [cell seat]} (enter! door triple)
            ^Semaphore seat seat]
        (try
          (if-let [locks (take! cell m)]
            {:locks locks}
            (let [began (System/nanoTime)
                  ends (after began (* 1000000 (long (:request-wait-ms (.-numbers door)))))
                  seated? (try (.tryAcquire seat (long (max 0 (left ends))) TimeUnit/NANOSECONDS)
                               (catch InterruptedException _
                                 (.interrupt (Thread/currentThread))
                                 false))]
              (if-not seated?
                {:word (waited-word began nil)}
                (try (in-the-seat door cell reading depot m began ends)
                     (finally (.release seat))))))
          (finally (leave! door triple)))))))

;; ------------------------------------------------------------------ the calls

(defn open
  "Opens the door. Takes a cluster handle of Rama's and the name that Rama
  gives the module `Store`; and, when the opener wants, a map of the door's
  five numbers (`softland.store.door.stock/default-numbers` says what each
  is, with its default and its reason). Gives a door, or the word
  `:find-the-module` when the cluster gave no depot or no query of the
  module under that name; no door is then opened.

  Five round trips, once for a process and on no act's way: one fetch of a
  client for each of the two depots and the three queries. The door borrows
  the handles and uses them from every maker's thread; the opener owns the
  cluster handle, and nothing closes the door. A door starts with no stock.

  A process opens one door: two doors are two sets of stocks, and neither
  uses the other's locks. Opening checks nothing of who opens it."
  ([cluster module-name] (open cluster module-name nil))
  ([cluster module-name numbers]
   (try
     (->Door module-name
             (rama/foreign-depot cluster module-name "*streaming-depot")
             (rama/foreign-depot cluster module-name "*microbatch-depot")
             (rama/foreign-query cluster module-name "answer-by-act-id")
             (rama/foreign-query cluster module-name "locks-of-request")
             (rama/foreign-query cluster module-name "depot-of-layer")
             (stock/numbers numbers)
             (atom {}))
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
  that threw ends in `:read-the-layers-depot`. It neither reads nor changes
  a stock. A maker that gives the act-id back to `send-act` pays no second
  read there."
  [^Door door layer]
  (if-not (record/depot-read layer)
    (words/word :make-an-act-id {:at :layer})
    (let [[outcome got] (depot-of door layer)]
      (if (= :word outcome)
        got
        [layer got (UUID/randomUUID)]))))

(defn act-ids-for
  "Gives the act-ids of a series in one layer, before the first of them is
  sent. Takes the door, a layer, and a count, an integer of 1 or more. Gives
  a vector of that many act-ids of the layer, each with a uuid of its own,
  or a word.

  A maker may use an id of the series in a later act of it before anything
  is sent: as `:because-of`, and as the act-id of a fact id in `:replaces`
  and `:based-on`. One read of the layer's depot serves the whole series,
  where `act-id-for` would read once for each id, on the layer's task,
  whose one thread the streaming gate also needs.

  The words are those of `act-id-for`, and `:make-an-act-id` at `:count`
  for a count that is no integer of 1 or more. Limit: the count has no
  bound but the memory of the process."
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

  `:digest` is the digest of the very thing that was appended, as 64 hex
  characters: the digest that a gate keeps with its answer. `:ack` is
  `:gate-ran` for the streaming depot and `:depot-holds` for the microbatch
  depot (`softland.store.door.words/acks`). It is a signal and never the
  answer: the maker reads the answer with `answer-of`. Once the call
  returns, the door keeps nothing of what it sent; the maker holds the
  act-id and the digest.

  The steps, in their order. A step that ends in a word ends the call.
  Every test that needs no cluster comes before the first round trip, and
  every step that can fail without a lock comes before the taking of locks,
  because a lock that is taken never comes back.

    1-4. The door reads the thing and walks its facts, with no round trip
         (`softland.store.door.additions/reading`): `:read-the-act`,
         `:add-its-parts`, `:make-an-act-id`, `:seal-a-value`,
         `:seal-that-many-values`.
    5.   The act-id. Given: used as it is, also when its depot slot is not
         the layer's depot; the thing then goes to the depot that it names
         and a gate refuses it there. Not given: one read of the layer's
         depot, and a random uuid: `:read-the-layers-depot`.
    6.   The locks, one for each value fact, from the stock of the act's
         layer, the act-id's depot and the act's who (`locks-for`). When the
         stock holds them, no round trip. When it does not, the door asks:
         three round trips in a streaming layer, and in a microbatch layer
         the time until the request's batch is closed. The five words about
         a request.
    7.   Each value is sealed with its lock to the place [layer entity key],
         the thing is built, and its digest is taken: `:seal-a-value`.
    8.   One append, to the depot that the act-id names. When it throws:
         `:append-cleanly`, with the act-id and the digest, since the door
         does not know whether the depot holds the thing. It is the one
         word of which \"nothing was appended\" cannot be said. The door
         sends nothing again; the maker reads the record.

  The door changes the act by its three additions and no more. It fills no
  missing part, reorders no fact, and names no permission.

  One faulty act has two outcomes, by the state of the stock. Take an act
  with a value under a permission that does not stand. When its stock holds
  locks, the act goes in, and its refusal is recorded under its act-id.
  When its stock is empty, the door's request is refused: the word is
  `:have-its-request-accepted`, and nothing is ever recorded under the
  act's own act-id.

  The order of two sends. When the call returns, its append is over: it was
  acked or it threw. So two sends that one maker makes one after the other
  reach the depot in that order. In a streaming layer that is the order of
  the decisions, without a failure. In the base two acts on two first
  entities have no order in the depot: a maker that needs the first decided
  first reads its answer before it sends the second.

  A second send of one act is another thing when the act has a value: other
  locks and another random part, so another digest (`answer-of` says how a
  maker reads that).

  The call runs on the maker's thread and holds it until it returns. A
  thread whose interrupt mark is set when a call of Rama's client is made
  gets that call's throw at once, as the word of that call, with its mark
  set again; such an append can have gone through.

  Limits. Facts with no end that hold fewer than 257 value facts do not
  come back, and neither does a thing that has no end in another part: an
  entity, a who, a part that the form does not know. The door has no bound
  on the count of facts as such; that count is a gate's to judge."
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
           (let [depot (nth act-id 1)
                 m (:values reading)
                 got (if (pos? m) (locks-for door reading depot m) {:locks []})]
             (if (:word got)
               (:word got)
               (let [built (additions/thing-to-append reading act-id (:locks got))]
                 (if (:could-not built)
                   built
                   (let [thing (:thing built)
                         digest (digest/hex-of thing)
                         [outcome threw] (append! door depot thing)]
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

    {:answer :accepted :stamp s}              the record's answer is of the
                                              thing with that digest
    {:answer :refused :reason word :stamp s}  the same, with the gate's
                                              reason word
    {:answer :nothing-yet}                    the read gave nothing
    {:answer :not-mine}                       the record holds an answer
                                              under the act-id with another
                                              digest; nothing more is said

  The door compares two hex texts: it holds no thing to take a digest of.
  The call needs nothing that the door remembers, so it gives the same for
  an act-id and a digest that a maker kept from a process before. It reads
  no stock and writes nothing.

  \"Nothing yet\" does not mean refused, and it does not mean that nothing
  is on a depot. It is the answer in states that the door cannot tell
  apart: nothing was ever sent under the act-id; a thing is on its depot
  and not decided; a microbatch act is decided and its batch is not closed;
  the thing is a stray, which the door itself never sends; the thing given
  is no act-id, for which the door makes no read at all and comes back at
  once.

  A maker's own second send reads \"not mine\". The door keeps nothing, so
  a second send of one act with a value is another thing. A maker that
  sends again under one act-id holds each digest that it was given: at most
  one of them reads an answer that is its own, and each other reads \"not
  mine\". The door never makes one id twice; it does not refuse a second
  send.

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
                   (try (long (min bound-ms stock/longest-wait-ms)) (catch Throwable _ 0))
                   0)
           ends (after (System/nanoTime) (* 1000000 bound))
           [outcome got] (read-answer-until door act-id ends false nil)]
       (case outcome
         :threw (words/word :read-the-answer (assoc got :act-id act-id))
         :answer (words/answer got digest)
         (words/answer nil digest))))))

(defn stock-count
  "Says how many locks the door holds. Takes the door. Gives three numbers
  (`softland.store.door.stock/count-of`): `:unused-locks`, the unused locks
  in the door's stocks; `:stocks`, how many stocks hold at least one;
  `:open-requests`, how many requests the door sent and has not read the
  locks of. Numbers only, never a lock's bytes or an act-id.

  The first number is what an end of the process now would leave in the
  record as rows that seal nothing, beside the locks of the open requests
  that a gate accepted. No round trip: it reads the door's memory, each
  stock at its own moment."
  [^Door door]
  (stock/count-of (map (fn [held] @(:cell held)) (vals @(.-stocks door)))))
