(ns softland.store.door.stock
  "The door's stock of locks, as a value: the unused locks of one who in one
  layer, how they are taken for one act, how many a request asks for, and
  the one request whose locks are not read yet. Pure code. It needs no
  cluster, and it reads no clock.

  \"Lock\" is an encryption key. \"Key\" is a fact's key. Never one word for
  both. What lets one call at a time ask for a stock is a seat, and it is
  the door's (`softland.store.door`), not this namespace's.

  A stock is for one layer, one depot and one who. The door seals a value
  with a lock that the store made, and a lock comes from a request for
  locks, which is an act: a whole round to the store. A stock is what lets
  a value be sealed without that round: the door asks for more locks than
  one act needs and keeps the rest here. A lock that a stock holds when its
  process ends is a row of the record that seals nothing, for good: the door
  reads locks only of requests that it made itself in this process. So the
  size rule keeps a stock small (`ask-for`).

  A stock is a map:

    :runs       the unused locks, as a vector of runs, the oldest first. A
                run is the unused locks of one request, in the order of
                their indexes: {:request the request's act-id, :way the
                number of their way of sealing, :locks a list of
                [index bytes]}. The bytes are the array that the read gave;
                no copy is made
    :given-out  how many locks this stock has handed to acts since the door
                was opened. The size of the next request is read from it
    :open       nil, or the one open request: a request that the door sent
                and whose locks it has not read:
                {:act-id :digest :count :state :since}. `:digest` is the
                digest of the request as it was sent, `:count` how many
                locks it asked for. `:state` is :sent, its append returned;
                or :in-doubt, its append threw or has not returned yet, so
                the door does not know whether a depot holds it. `:since`
                is the moment from which a request in doubt is kept, as the
                door's clock gave it

  A stock keeps no answer that the door read, and no lock that it gave: a
  lock that is taken leaves its run, and nothing here refers to it again.
  A lock that was taken never comes back, whatever becomes of the act that
  it was taken for. After an append that threw the door does not know
  whether the lock seals a value; if it were given to a second act and the
  first went in, a gate would refuse the second for no fault of its maker.

  Inputs and results, by call:

    numbers         what the opener of the door gave -> the door's five
                    numbers
    ask-for         the five numbers, a depot word, the locks that an act
                    needs, the locks given out -> how many a request asks for
    unused          a stock -> how many unused locks it holds
    holds-nothing?  a stock -> true when it holds nothing at all
    take-locks      a stock, a count -> [the stock after, the locks], or nil
    with-request    a stock, a request, a moment -> the stock with that
                    request open, in doubt
    request-sent    a stock -> the stock with its open request sent
    request-in-doubt-since  a stock, a moment -> the stock with its open
                    request in doubt since that moment
    without-request a stock -> the stock with no open request
    kept-long-enough?  an open request, a moment, a span -> true or false
    with-locks-read a stock, a request's act-id, what the read of its locks
                    gave, a count -> [the stock after, the locks or nil]
    count-of        stocks -> three numbers

  State and effects. None. Each function is pure: it takes a stock and gives
  a stock. The door holds each stock in a place that changes by
  compare-and-set, so a step of this namespace is one step for every caller
  or does not happen.

  Callers. `softland.store.door`, and the pure proof.

  Not here. The seat, the clock, the waits and every call of Rama's client:
  `softland.store.door`. The thing of a request for locks:
  `softland.store.door.additions`. The most locks that one act can take:
  `softland.store.door.additions/most-values-in-one-act`, which stops an act
  before it comes here."
  (:require [softland.store.door.declaration :as declaration]
            [softland.store.door.seal :as seal]))

;; -------------------------------------------------------- the five numbers

(def longest-wait-ms
  "The longest wait that the door counts, in milliseconds: about 73 years. A
  wait is counted in nanoseconds of a clock that never goes back, and a
  longer one would leave the range of a long there. A number of the build."
  (quot Long/MAX_VALUE 4000000))

(def default-numbers
  "The door's five numbers, with the default of each. The opener of the door
  may give another for each (`numbers`); none is measured, and none is a
  decision of Sid's.

    :request-wait-ms 10,000   how long one call waits for locks, in all,
        before it ends in a word. It is about ten times the time in which a
        request's answer was readable in the base on the in-process cluster,
        so a maker of the base does not meet the word in ordinary work. And
        it is the time for which Rama's client holds a caller in one append
        by default (`foreign.depot.operation.timeout.millis`), so the door's
        own wait is no longer than one append can hold a maker anyway. A
        request in doubt is kept for the same span.
    :read-every-ms 200   how long a wait sleeps between two reads of an
        answer: the maker's wait in `answer-of`, and the door's wait for its
        own request. Each read of a wait but the last finds nothing, and
        each is a round trip and a read on a task whose one thread a gate
        also needs. At 200 ms an answer that is there after 0.9 s costs 6
        reads and is learnt 100 ms late at the mean; at 20 ms it costs 46.
        A microbatch layer is not for what a person does directly, so a
        tenth of a second there is a small price.
    :fewest-at-once 4   the fewest locks that the size rule asks for in one
        request. A request leaves about as many bytes in the cluster as
        five unused locks, so asking for fewer than about that many saves
        nothing even when the who never comes back.
    :most-at-once-streaming 64   the most that the size rule asks for in one
        request of a streaming layer. Past 64 the gain is small: one request
        for each 64 values is 1.6% more events for the gate. What grows with
        it is not small: the rows that a restart leaves, and the length of
        the gate's one event for the request, which writes a row for each
        lock while it holds the layer's task.
    :most-at-once-microbatch 256   the same for a microbatch layer: the
        declaration's ceiling. There a request costs its maker the time
        until the request's batch is closed, about a second on the
        in-process cluster, and all that time the stock gives nothing. So
        the largest request stops a who least often."
  {:request-wait-ms         10000
   :read-every-ms           200
   :fewest-at-once          4
   :most-at-once-streaming  64
   :most-at-once-microbatch 256})

(defn numbers
  "Takes what the opener of the door gave for the five numbers: a map, or
  anything. Gives the five numbers as a map. A number that is missing, or is
  no integer of 1 or more, takes its default. A count of locks over the
  declaration's ceiling of one request
  (`softland.store.door.declaration/max-locks-in-request`) is cut to it, and
  a wait over `longest-wait-ms` is cut to that. A name that is none of the
  five is left out. It never throws."
  [given]
  (let [read (fn [k] (try (get given k) (catch Throwable _ nil)))
        ceiling (fn [k] (if (contains? #{:request-wait-ms :read-every-ms} k)
                          longest-wait-ms
                          declaration/max-locks-in-request))]
    (into {}
          (map (fn [[k default]]
                 (let [x (read k)]
                   [k (if (and (int? x) (<= 1 x)) (min (long x) (ceiling k)) default)])))
          default-numbers)))

(defn ask-for
  "The size rule: how many locks one request asks for. Takes the door's five
  numbers, the depot word of the stock, how many locks the act needs, m, and
  how many locks the stock has given out so far. Gives

    the smallest of:  the ceiling of one request, 256
                      the greatest of:  m
                                        the smallest of:  the most at once for the depot
                                                          the greatest of:  the fewest at once
                                                                            the locks given out so far

  So a request asks for as many locks as its stock has given out, between
  the fewest and the most, and never fewer than the act needs. A who that
  goes on writing one value at a time in a streaming layer makes requests
  of 4, 4, 8, 16, 32, 64 and then 64 each.

  Why the rule follows the who. A request costs the cluster about 550 bytes
  for good, about as much as five unused locks, and one event of a gate. A
  fixed large batch is good for a who that writes much and leaves 63 rows
  for a who that wrote one value; a fixed small batch gives the gate one
  more event for each few values. Under this rule a stock holds fewer unused
  locks than its last request asked for, and that request asked for no more
  than the stock had given out before it, or the fewest, or what one act
  needed. That is what a restart loses for one who. One state is outside
  that sentence: a call read the locks of an earlier call's request, the
  stock was still short of its act, and the call ended before it sent its
  own. The stock then holds fewer locks than that act needed.

  The rule reads no clock. What must hold: m is no more than the ceiling,
  which `softland.store.door.additions/most-values-in-one-act` holds before
  any lock is asked for; one request then always covers one act."
  [numbers depot m given-out]
  (let [most (if (= :microbatch depot)
               (:most-at-once-microbatch numbers)
               (:most-at-once-streaming numbers))]
    (min declaration/max-locks-in-request
         (max m (min most (max (:fewest-at-once numbers) given-out))))))

;; ---------------------------------------------------------------- the stock

(def nothing
  "A stock that holds nothing: no run, nothing given out, no open request.
  Each stock of a door starts as this, whatever the record holds from a
  process before."
  {:runs [] :given-out 0 :open nil})

(defn unused
  "Takes a stock. Gives how many unused locks it holds, over all its runs."
  [stock]
  (reduce + 0 (map (fn [run] (count (:locks run))) (:runs stock))))

(defn holds-nothing?
  "Takes a stock. Gives true when it holds nothing at all: no run, no open
  request, and nothing given out yet. Such a stock is the stock of a who
  whose every request was refused, and the door lets it go. A stock that has
  given out a lock is never such a stock: its count is what the next request
  is sized by. A stock with an open request is not one either: a later call
  reads that request or forgets it."
  [stock]
  (and (empty? (:runs stock))
       (zero? (:given-out stock))
       (nil? (:open stock))))

(defn take-locks
  "Takes the locks of one act. Takes a stock and a count, m, of 1 or more.
  Gives [the stock after, the locks], or nil when the stock holds fewer than
  m: a take that is short changes nothing. The locks are a vector of m, each
  {:lock-id [the request's act-id, index] :lock {:bytes :way}}, in the order
  of the stock: the oldest run first, in the order of its indexes, and then
  the run after it. The door gives them to the act's value facts in the
  order of the facts.

  All m leave the stock in this one step. So the locks that an act names are
  neighbours: of one request, or of two where the act falls on the border
  between two runs. A gate reads one range of lock rows for each request
  that an act names, from the least index that the act names to the
  greatest, and neighbours make each range exactly the locks named.

  Why the oldest first, across a border. To take from one run whenever one
  run can serve the act would leave a rest behind in the older run. Rests
  add up, a rest that no later act fits is a row that seals nothing, and an
  act that at last takes several rests names several requests. Taking
  across the border leaves nothing behind and costs a gate one more range
  for one act of each request.

  A run that gave its last lock is gone from the stock, and the stock after
  holds no reference to a lock that was taken."
  [stock m]
  (when (<= m (unused stock))
    (loop [runs (seq (:runs stock))
           need m
           taken []]
      (if (zero? need)
        [(assoc stock :runs (vec runs) :given-out (+ (:given-out stock) m)) taken]
        (let [{:keys [request way locks] :as run} (first runs)
              k (min need (count locks))
              from-run (map (fn [[index bytes]] {:lock-id [request index] :lock {:bytes bytes :way way}})
                            (take k locks))
              left (nthnext locks k)]
          (recur (if left (cons (assoc run :locks left) (next runs)) (next runs))
                 (- need k)
                 (into taken from-run)))))))

;; ------------------------------------------------------- the open request

(defn with-request
  "Takes a stock; a request, {:act-id :digest :count}; and a moment of the
  door's clock. Gives the stock with that request open, in doubt since that
  moment. The door writes a request into its stock before it appends it: if
  the call ends in any way between the append and the answer, the stock
  still knows the request. What must hold: the stock has no open request."
  [stock request moment]
  (assoc stock :open (assoc (select-keys request [:act-id :digest :count])
                            :state :in-doubt
                            :since moment)))

(defn request-sent
  "Takes a stock. Gives it with its open request sent: the append returned,
  so a depot holds the request and a gate will decide it. A request that is
  sent is never forgotten before its answer is read."
  [stock]
  (if (:open stock)
    (assoc-in stock [:open :state] :sent)
    stock))

(defn request-in-doubt-since
  "Takes a stock and a moment of the door's clock. Gives it with its open
  request in doubt since that moment: the append threw at that moment, and
  an append that threw may have gone through."
  [stock moment]
  (if (:open stock)
    (update stock :open assoc :state :in-doubt :since moment)
    stock))

(defn without-request
  "Takes a stock. Gives it with no open request: the request's answer was no
  accept, or its locks were read, or it was in doubt for long enough with no
  answer."
  [stock]
  (assoc stock :open nil))

(defn kept-long-enough?
  "Takes an open request; the moment at which a read of its answer began;
  and a span, the door's wait for locks, both in the unit of the door's
  clock. Gives true when the request is in doubt and the read began that
  span or more after the request's moment. A read that then finds nothing
  ends the request: the door forgets it. The moments are compared by their
  difference, with no overflow check, as the numbers of a clock that never
  goes back ask.

  Why by its age. Rama's client can throw for an append that went through,
  so a request whose append threw is kept, and a later call reads its
  answer before it asks anew: a door that forgot at once would send one
  more request into a gate that is already behind. If the append truly
  failed, no answer ever comes. By its age each such request gets the whole
  span and no call waits for one that is older. A request that is sent is
  never such a request."
  [open read-began span]
  (boolean (and (= :in-doubt (:state open))
                (<= span (unchecked-subtract (long read-began) (long (:since open)))))))

;; ------------------------------------------------------ the locks of a read

(defn- can-seal?
  "Takes a lock as the read of a request's locks gave it. Gives true when it
  has its bytes and a way of sealing that the code knows. A lock of the base
  that seals a value comes from that read with nil for its bytes, and a lock
  of a way that this code does not know seals nothing."
  [lock]
  (and (map? lock)
       (bytes? (:bytes lock))
       (seal/known-way? (:way lock))))

(defn- runs-of
  "Takes the act-id of a request and what the read of its locks gave, a map
  from index to {:bytes :way}. Gives the locks that can seal as runs, in the
  order of their indexes. The locks of one request are one run when they
  are all of one way, as they are in this build; where the way changes, a
  new run begins, so that a run keeps the way once. A lock that cannot seal
  is left out, and the indexes of a run then have a gap."
  [request locks]
  (if (map? locks)
    (->> (sort-by key locks)
         (filter (fn [[_ lock]] (can-seal? lock)))
         (partition-by (fn [[_ lock]] (:way lock)))
         (mapv (fn [group]
                 {:request request
                  :way     (:way (val (first group)))
                  :locks   (apply list (map (fn [[index lock]] [index (:bytes lock)]) group))})))
    []))

(defn with-locks-read
  "One step, for a read of a request's locks that came back. Takes the stock;
  the act-id of the open request; what the read gave, a map from index to
  {:bytes :way}; and how many locks the call's own act needs, m. Gives
  [the stock after, the locks of the act], or [the stock after, nil] when
  the stock is still short of m.

  In the stock after: the locks that can seal are a run at the end; the open
  request is forgotten; and the act's m locks are taken when the stock now
  holds them, as `take-locks` takes them.

  A read that came back ends its request, whatever it gave. A second read of
  the same request can give no more: a gate writes all the locks of a
  request in the event or the batch that accepts it. So no stock is held at
  one request for good."
  [stock request locks m]
  (let [filled (-> stock
                   (update :runs into (runs-of request locks))
                   (without-request))]
    (or (take-locks filled m)
        [filled nil])))

;; ------------------------------------------------------------------ a count

(defn count-of
  "Takes stocks. Gives three numbers:

    :unused-locks   the unused locks that the stocks hold
    :stocks         how many of the stocks hold at least one
    :open-requests  how many requests are open: sent or in doubt, their
                    locks not read

  Numbers only: no lock's bytes, no act-id. The first number is what the end
  of the process would leave in the record as rows that seal nothing, beside
  the locks of each open request that a gate accepted."
  [stocks]
  {:unused-locks  (reduce + 0 (map unused stocks))
   :stocks        (count (filter (fn [stock] (pos? (unused stock))) stocks))
   :open-requests (count (filter :open stocks))})
