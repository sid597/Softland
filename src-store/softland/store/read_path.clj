;; Before changing this file, read the plan of this part again:
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-read-path.md
;; and, for where a microbatch lane is read, the section "The read path" of
;; src/proposal/backend-on-rama-2026-10-02/run-again/PLAN-the-bases-gate.md
;; and the rulings that came after its check, which the plan does not hold:
;; 78 and 80; and 86, 87, 89 and 91, which the review of the whole built
;; (FULL_SPEC_REVIEW.md beside the plan). They are in
;; src/proposal/backend-on-rama-2026-10-02/reads/PROGRESS.md

(ns softland.store.read-path
  "The read path: the one piece of code by which a fact comes back out of
  the store to a caller in the runtime's process, once or kept live. Of
  Rama's client it uses only `foreign-pstate`, `foreign-select-async`,
  `foreign-proxy-async` and `close!`. It reads the gates' tables straight;
  no query of the store stands between. The door (`softland.store.door`) is
  the way in, and neither calls the other: in the runtime the module has
  two clients.

  No call throws for what a caller gave or for what Rama did, and a read
  that failed has never said \"no fact\". What must hold: the first thing
  that a call is given is a read path that `open` gave. Who asks is what the
  caller says, and `:system` is a who like any other. Every wait for Rama
  has the read path's own bound, `:wait-ms`. The read path tries nothing a
  second time by itself: the caller asks again.

  How it reads. A source is one row or one run of neighbouring rows of one
  table on one task (`softland.store.read-path.sources`). Asked once, every
  missing source of one table under one routing id goes in one call of
  `foreign-select-async`. Kept live, every source is one push,
  `foreign-proxy-async`, shared by every live read that wants the same
  source and closed when the last of them leaves. A read of a lane's acts
  (`:acts`) is asked once, and is not kept live.

  Every path that the read path gives Rama is one of the forms that
  `path-of` builds from texts of `softland.store.record.rows`; no caller
  gives a path, a table or a routing id. Rama does not refuse a path that it
  cannot serve cleanly (one such select stopped a worker of the in-process
  cluster, in a probe of this build's plan), so the read path sends no other
  form. It never opens a push on a whole table: the widest thing that a
  query can name is the whole of one lane, which is a run, and a run is read
  whole only after a bounded read has shown that it holds no more places
  than one read may (`open-push`, `run-now`).

  What crosses from Rama to the runtime. Rama sends the runtime each whole
  row of what a read reads: the value, who wrote the fact, the permission
  that its act named. A caller is handed who wrote a fact, or the
  permission, only when her query asked for it. A read asked once reads a
  lane's fact in the same call as its rights, so the runtime can read one
  row, or the first places of a run, of a lane that the asker may then not
  see; they are dropped, and nothing of them is handed over. Each program
  that can open Rama's ports is trusted with the whole store.

  State. The read path owns no durable state, and it writes nothing: it
  appends to no depot and keeps no record of who read what. It borrows ten
  handles of Rama's client; the opener owns the cluster handle, and nothing
  closes the read path. Its one memory is `held`, an atom of three maps, in
  the memory of one process:

    :pushes  a source -> {:opening a delay around the one call that opens
             its push, :readers the ids of the live reads that hold it,
             :called a mark that its first callback came}
    :counts  a gate, a table and a routing id -> how many entries of
             `:pushes` are under them (`counted-under`)
    :reads   a read's id -> its query, its function, its turn, the set of
             the sources that it holds, whether it is opened, and its mark
             (`softland.store.read-path.answer/mark-of`)

  It holds no row, no value and no place-answer. The values are in Rama's
  pushes and nowhere else: each holds the current value of its path, and
  Rama keeps it level. Nothing shown to a caller is anything but what the
  tables held: there is no optimistic value. All of `held` is lost when the
  process ends, and Rama clears a push that sends no heartbeat (read in
  Rama's pages, not run); nothing tells a caller, and each asks again.

  The read's turn. One call of an asker's function runs at a time for one
  read, in the order in which the calls were worked out. What holds that is
  called the read's turn, and never a lock: in this tree a lock is an
  encryption key.

  The first call is made by `read-live` itself, on the caller's own thread,
  before it returns and before any change is told. Every later call is made
  on a thread of Rama's client: the thread of the push whose change it is,
  or the thread on which a push that the read came to need was given; for an
  `{:ended ...}` after a wait that passed, on the thread of the JVM's own
  timer. The function must return at once: Rama's own callback has this
  rule, and the read path passes it on. What was run for the plan's check,
  on an in-process cluster whose client had two such threads: while one
  function stayed for 2.5 s, every push whose callbacks ran on that thread
  waited, pushes of other askers among them, and so did selects and openings
  of pushes from other threads when Rama's client gave their answer on that
  thread. So one slow function holds back the reads of others, `read-once`
  and `read-live`, up to their own bound. A caller that does more than hand
  the change on takes a thread of its own.

  The function may call `leave`. It must not call `read-once` or
  `read-live`: they wait for Rama on a thread that Rama may need for the
  answer. The read path turns such a call away at once, with the word
  `:read-inside-an-askers-function` and no call to Rama.

  What Rama sends at a change. Three kinds of diff reach a callback. A diff
  that names keys: what a push on a run gets when a row of the run is
  written or removed, and the other rows stay the objects that they were. A
  new value that names no key: what a push on one row gets. And a whole
  value sent anew: the first callback of every push, and what Rama sends a
  reader that it left out of a change. The read path takes the three the
  same way: it handles the keys whose rows differ
  (`softland.store.read-path.sources/named`). So an asker is told the places
  that changed, and not a whole answer, whichever way Rama sent the change,
  and nothing when no row differs. That a push is told each write by a diff
  of its own rests on what a row holds: no field of the two tables that the
  read path reads is a byte array (`softland.store.record.tables/facts` says
  what was run).

  How many pushes. Rama bounds the pushes on one table on one task, by a
  dynamic option of its own, `pstate.proxy.limit`, whose default is 1,000
  (the name is read in Rama's own message; no run of this build set the
  option). As run on the store's module, in-process, with the default: 1,000
  pushes opened on one table on one task, the next one was refused, and a
  push that was closed made room for one more; and Rama refuses one opening
  at a time, so a read that asked for about 10,000 pushes was turned away
  only after seconds. A streaming lane is whole on one task, so every live
  read through it, of every asker, counts toward that one bound, and so do
  the other lanes of that task. A microbatch lane is whole on every task,
  and a read holds its pushes on the task of the read's home; so on one task
  its table counts the pushes of every home that Rama puts there.

  So the read path has a bound of its own, under Rama's: it counts the
  pushes that it holds on each table under each routing id
  (`counted-under`), and turns away, before it opens one, a live read whose
  next pushes would pass `:max-pushes-under-one-routing-id` (`join!`) or
  whose checks of nearer lanes, each of them a push, would pass
  `:max-checks-of-one-live-read`.

  What that bound does not reach. It counts by routing id and not by task
  (`counted-under` says why). A live read of the base holds two pushes at
  least on the base's facts under its home, the base's own rows and one fact
  source, and no home shares them with another. So Rama's bound holds the
  homes of one task that read the base live to half its number at most, and
  the read path's count does not see that (derived from the two counts; no
  run of this build opened that many). And it counts one process, where
  Rama's bound holds for every client of the cluster together. Past either,
  Rama refuses the opening, and the read gets `:reach-the-store`, as any
  opening that throws does. The read path sets none of Rama's options.

  A screen is shown the newest state; a caller that must see every act
  cannot rest on a live read. The function is not called once for each act:
  two acts on one place can come as one change, Rama can leave a reader out
  of a change and send the value anew, and a second attempt of a batch that
  writes the same row gives nothing. When two changes cross, a place can
  come a second time with the answer that it already had; it never comes
  with an answer that is older than one that came before.

  A microbatch lane. A lane's gate is the depot that it starts with
  (`softland.store.gate.first-act/depot-at-making`). In a streaming lane
  every row is on the lane's task. In a microbatch lane every row of the
  facts, and of the facts by value, is on every task, the same rows
  everywhere: its gate decides an act on the lane's task and writes the rows
  of an accept on each task. So the read path reads such a lane on the task
  of the read's home, the lane that the read starts from: an asker reads the
  base where her own lane lives, and a read that starts from the base reads
  it on the base's own task (`softland.store.read-path.sources`). A run
  across entities, and a run of the facts by value, come whole from that one
  task. Nothing of a batch is read: a second attempt of a batch writes the
  same row again, and a push that calls back with a row that is the same
  tells an asker nothing. Two tasks show one batch at two instants (read in
  Rama's page on microbatch topologies), so two askers with two homes can be
  told of one act of the base at two instants; nothing lines them up. What
  the read path rests on and does not test at a read: that every task holds
  the rows. A task that held fewer would give the askers of its homes fewer,
  and no read would notice."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [softland.store.read-path.acts :as acts]
            [softland.store.read-path.answer :as answer]
            [softland.store.read-path.query :as query]
            [softland.store.read-path.sources :as sources]
            [softland.store.read-path.words :as words])
  (:import [com.rpl.rama.diffs DestroyedDiff Diff Diff$Processor KeyDiff KeyDiff$Processor
            KeyRemoveDiff KeyRemoveDiff$Processor ResyncDiff UngracefulTerminationDiff]
           [java.io Writer]
           [java.util ArrayList]
           [java.util.concurrent CompletableFuture ExecutionException TimeUnit TimeoutException]
           [java.util.concurrent.atomic AtomicBoolean AtomicLong]
           [java.util.function BiConsumer Consumer]))

;; ------------------------------------------------- the read path as a value

(defn- printed
  "The text that a read path prints as. No handle and no asker's query is in
  it, whatever prints the read path."
  [module]
  (str "#<read-path " module ">"))

(deftype ReadPath [module
                   streaming-facts
                   microbatch-facts
                   streaming-facts-by-value
                   microbatch-facts-by-value
                   streaming-line
                   microbatch-line
                   streaming-things
                   microbatch-things
                   streaming-task
                   microbatch-task
                   numbers
                   held
                   next-id]
  Object
  (toString [_] (printed module)))

(defmethod print-method ReadPath [^ReadPath read-path ^Writer w]
  (.write w (.toString read-path)))

(defmethod print-dup ReadPath [^ReadPath read-path ^Writer w]
  (.write w (.toString read-path)))

(alter-meta! #'->ReadPath assoc
             :private true
             :doc "An opened read path. Its handles of Rama's client are borrowed;
  `next-id` is the count from which the ids of its live reads are made.")

(deftype LiveRead [held id]
  Object
  (toString [_] (str "#<live-read " id ">")))

(defmethod print-method LiveRead [^LiveRead live-read ^Writer w]
  (.write w (.toString live-read)))

(defmethod print-dup LiveRead [^LiveRead live-read ^Writer w]
  (.write w (.toString live-read)))

(alter-meta! #'->LiveRead assoc
             :private true
             :doc "The handle of a live read. It holds the `held` atom of the read
  path that opened the read, so that a handle of another read path is told
  apart.")

(def ^:private max-rounds
  "How many rounds a read is given, by how it is asked, before it stops with
  `:get-an-answer-within-its-wait`. Set from the chains of a read's sources
  (`softland.store.read-path.sources` derives them).

  A read asked once settles within 6 rounds whatever is written while it
  reads; a read of acts takes 6 at most. A live read opens within 8 rounds
  on tables that do not change while it opens, and is given 8 more for
  writes that make a part of it wanted anew while it opens: a list that
  gains a lane asks 5. A step of an opened read (`settle!`) is held to the
  same 16.

  A read that writes keep from settling is turned away, and its caller asks
  again; `:wait-ms` bounds every read in time as well."
  {:once 6 :live 16})

(defn- now-ms
  "The JVM's steady clock in milliseconds, to bound a wait. It is no time of
  day."
  []
  (quot (System/nanoTime) 1000000))

;; ------------------------------------------------ the calls of Rama's client

(defn- what-threw
  "For an `InterruptedException` the thread's interrupt mark is set again
  first: the read path turns the throw into a word, and without the mark
  the caller's thread would not know that it was interrupted."
  [^Throwable t]
  (when (instance? InterruptedException t)
    (.interrupt (Thread/currentThread)))
  {:threw (.getName (class t)) :said (.getMessage t)})

(defn- attempt
  "Every call of Rama's client that the read path makes after its opening
  goes through here, so none of them throws out of a call of the read path,
  and none leaves a thrown thing in the delay of a push."
  [f]
  (try [:ok (f)]
       (catch Throwable t [:threw (what-threw t)])))

(defn- within
  [^CompletableFuture future ms]
  (try [:ok (.get future (max 0 (long ms)) TimeUnit/MILLISECONDS)]
       (catch TimeoutException _ [:late])
       (catch ExecutionException e [:threw (what-threw (or (.getCause e) e))])
       (catch Throwable t [:threw (what-threw t)])))

(defn- pstate-of
  [^ReadPath read-path source]
  (case [(:gate source) (:table source)]
    [:streaming :facts]           (.-streaming-facts read-path)
    [:microbatch :facts]          (.-microbatch-facts read-path)
    [:streaming :facts-by-value]  (.-streaming-facts-by-value read-path)
    [:microbatch :facts-by-value] (.-microbatch-facts-by-value read-path)
    [:streaming :line]            (.-streaming-line read-path)
    [:microbatch :line]           (.-microbatch-line read-path)
    [:streaming :things]          (.-streaming-things read-path)
    [:microbatch :things]         (.-microbatch-things read-path)
    [:streaming :task]            (.-streaming-task read-path)
    [:microbatch :task]           (.-microbatch-task read-path)
    nil))

(defn- window
  "The path of a window: `n` rows of the table from the run's first end, cut
  at its last end. For a run of which an end holds a UTF-16 unit from
  U+D800 on, the walk alone and no cut: this build did not find Rama to
  make such a cut rightly
  (`softland.store.read-path.sources/first-read-holds?`), so the reader
  drops the keys past the end itself
  (`softland.store.read-path.sources/keys-of-run`). A lane and a who are
  each such an id, so the run of one who's rows by who takes the same care
  as the run of a lane's line. What must hold: the count is a whole number
  of 1 or more."
  [from to n]
  (if (or (sources/outside-the-agreed-range? from) (sources/outside-the-agreed-range? to))
    (path/sorted-map-range-from from {:max-amt n})
    [(path/sorted-map-range-from from {:max-amt n}) (path/sorted-map-range from to)]))

(defn- path-of
  "The one function that makes a path of Rama's. Nil for a source or a step
  of any other kind; the read path then makes no call. What must hold: the
  texts of a source were made by `softland.store.record.rows` of ids that
  were tested first.

  A push is opened only on `:whole` of a row, of a lane's own rows and of a
  run. `[:run :first]` and `[:run :count]` walk one row more than
  `:run-start-places`, and are only for a run whose two ends Rama cuts
  rightly in memory (`softland.store.read-path.sources/first-read-holds?`).
  `[:run :size]` walks one row more than `:max-places-of-one-run` from the
  run's start, with no end, and gives its count and the key of its last row:
  alone in its call, since it gives one value or two."
  [source numbers step]
  (let [[kind a b n] (:path source)
        window? (contains? #{:window :by-who} kind)
        ok? (and (string? a)
                 (or (contains? #{:row :last-stamp} kind) (string? b))
                 (or (not window?) (and (int? n) (pos? (long n)))))]
    (when ok?
      (case [kind step]
        [:row :whole]      (path/keypath a)
        [:own-rows :whole] (path/sorted-map-range a b)
        [:run :whole]      (path/sorted-map-range a b)
        [:run :first]      [(path/sorted-map-range-from a {:max-amt (inc (:run-start-places numbers))})
                            (path/sorted-map-range a b)]
        [:run :count]      [(path/sorted-map-range-from a {:max-amt (inc (:run-start-places numbers))})
                            (path/sorted-map-range a b)
                            (path/view count)]
        [:run :size]       [(path/sorted-map-range-from a {:max-amt (inc (:max-places-of-one-run numbers))})
                            (path/multi-path (path/view count) [path/LAST path/FIRST])]
        [:window :whole]       (window a b n)
        [:last-stamp :whole]   (path/keypath :last-stamp a)
        [:by-who :whole]       [(path/keypath :by-who) (window a b n)]
        nil))))

(defn- select-async
  [read-path source the-path]
  (let [pstate (pstate-of read-path source)]
    (if (or (nil? pstate) (nil? the-path))
      [:no-path]
      (attempt (fn [] (rama/foreign-select-async the-path pstate {:pkey (:routing-id source)}))))))

(defn- close-push!
  "It returns at once and waits for nothing, also inside a callback of that
  push (run for the plan's check). A throw is dropped: a push that is
  closed a second time throws nothing, and a push that cannot be closed is
  left to Rama, which clears a push that sends no heartbeat."
  [push]
  (try (rama/close! push)
       (catch Throwable _ nil)))

;; ------------------------------------------------------ a read asked once

(defn- late
  [^ReadPath read-path]
  (words/word :get-an-answer-within-its-wait {:waited-ms (:wait-ms (.-numbers read-path))}))

(defn- too-large
  [^ReadPath read-path]
  (words/word :serve-a-read-that-large
              {:what :places-of-one-run :most (:max-places-of-one-run (.-numbers read-path))}))

(defn- no-room
  [^ReadPath read-path]
  {:what :pushes-under-one-routing-id
   :most (:max-pushes-under-one-routing-id (.-numbers read-path))})

(defn- gather
  "Waits for selects that were all sent already, up to the moment at which
  the call's one wait ends."
  [read-path sent deadline]
  (loop [todo sent values []]
    (if-let [[outcome got] (first todo)]
      (case outcome
        :ok (let [[outcome got] (within got (- deadline (now-ms)))]
              (case outcome
                :ok    (recur (rest todo) (conj values got))
                :late  [:word (late read-path)]
                :threw [:word (words/word :reach-the-store got)]))
        :threw [:word (words/word :reach-the-store got)]
        [:word (words/word :reach-the-store
                           {:said "a source of a kind for which the read path makes no path; no call was made"})])
      [:ok values])))

(defn- select-now
  "Reads the sources of the calls of one round
  (`softland.store.read-path.sources/calls-for-once`), each call one round
  trip to one task, all sent before any is waited for. The sources of one
  call have one table and one routing id, so the first of them names both.
  A call whose values are not one for each path, in the paths' order,
  cannot be told apart from another, so it ends in `:reach-the-store`."
  [^ReadPath read-path calls deadline]
  (let [numbers (.-numbers read-path)
        step-of (fn [source] (if (= :run (first (:path source))) :first :whole))
        reads-of (fn [call] (vec (distinct (map sources/read-of (:sources call)))))
        sent (mapv (fn [call]
                     (let [reads (reads-of call)
                           paths (mapv #(path-of % numbers (step-of %)) reads)]
                       (if (some nil? paths)
                         [:no-path]
                         (select-async read-path (first reads) (apply path/multi-path paths)))))
                   calls)
        [outcome got] (gather read-path sent deadline)]
    (if (= :word outcome)
      [:word got]
      (if (every? true? (map (fn [call values] (= (count (reads-of call)) (count values))) calls got))
        [:ok (into {} (mapcat (fn [call values]
                                (let [by-read (zipmap (reads-of call) values)]
                                  (map (fn [source] [source (get by-read (sources/read-of source))])
                                       (:sources call))))
                              calls got))]
        [:word (words/word :reach-the-store
                           {:said "a call gave another count of values than it had paths"})]))))

(defn- run-now
  "Reads whole the runs whose first read was not the whole run, each after a
  bounded walk on its task has given its size. A run that grew past the
  number between its size and its read ends in `:serve-a-read-that-large`
  too.

  What must hold: the asker's standing in each source's lane is `:yes`.
  `read-once` calls it for no other, so the size of a lane that she may not
  see is neither read past the first read nor told."
  [^ReadPath read-path run-sources deadline]
  (let [numbers (.-numbers read-path)
        ask (fn [step] (mapv #(select-async read-path % (path-of % numbers step)) run-sources))
        [outcome sizes] (gather read-path (ask :size) deadline)]
    (cond
      (= :word outcome)
      [:word sizes]

      (some true? (map (fn [source size] (sources/over-the-limit? size (nth (:path source) 2) numbers))
                       run-sources sizes))
      [:word (too-large read-path)]

      :else
      (let [[outcome runs] (gather read-path (ask :whole) deadline)]
        (cond
          (= :word outcome) [:word runs]
          (some #(< (:max-places-of-one-run numbers) (count (first %))) runs) [:word (too-large read-path)]
          :else [:ok (zipmap run-sources (map first runs))])))))

(defn- read-acts-once
  "The body of `read-once` for a query of `:acts`. A reading that wants
  nothing and gives neither an answer nor a stamp at which the line is not
  whole cannot be reached, and would end in `:reach-the-store`. No run of
  this read needs its size first, since each window is bounded by its own
  count."
  [^ReadPath read-path query]
  (let [numbers (.-numbers read-path)
        deadline (+ (now-ms) (:wait-ms numbers))]
    (loop [round 0 known {}]
      (let [{:keys [answer fault missing]} (acts/reading query known numbers)]
        (cond
          answer answer
          fault (words/word :find-the-line-whole fault)
          (empty? missing) (words/word :reach-the-store
                                       {:said "a read of acts wanted nothing and gave no answer"})
          (<= (:once max-rounds) round) (late read-path)
          :else (let [[outcome got] (select-now read-path (sources/calls-for-once missing numbers) deadline)]
                  (if (= :word outcome)
                    got
                    (recur (inc round) (merge known got)))))))))

;; ------------------------------------------------- the pushes, and their sharing

(declare on-callback)

(defn- open-push
  "The body of the delay of an entry of `:pushes`: the one call that opens
  the push of a source. Gives a future that ends, and never with a throw,
  in {:push the push} or in {:word a word}. It waits for nothing: a
  callback of Rama's can call it, under a read's turn.

  A run is bounded first: its count on the task, by a walk of the first
  places; when that walk is full, or the run has no first read
  (`softland.store.read-path.sources/first-read-holds?`), its size, by a
  bounded walk; each a select that the next step is chained to. The value
  that the push gives at its opening is counted too. The whole opening has
  one bound, `:wait-ms`: when it passes, the future ends in
  `:get-an-answer-within-its-wait`, on the thread of the JVM's timer, and a
  push that is given after that is closed.

  The push's callback hands each call to `on-callback` with the entry's
  mark, so that a late callback of a push that no entry holds any more
  reaches no read."
  [^ReadPath read-path source ^AtomicBoolean called]
  (let [numbers (.-numbers read-path)
        out (CompletableFuture.)
        give! (fn [x] (.complete out x))
        threw! (fn [info] (give! {:word (words/word :reach-the-store info)}))
        large! (fn [] (give! {:word (too-large read-path)}))
        run? (= :run (first (:path source)))
        after (fn [[outcome got] then]
                (case outcome
                  :ok (.whenComplete ^CompletableFuture got
                                     (reify BiConsumer
                                       (accept [_ value thrown]
                                         (try (if thrown (threw! (what-threw thrown)) (then value))
                                              (catch Throwable t (threw! (what-threw t)))))))
                  :threw (threw! got)
                  (threw! {:said "a source of a kind for which the read path makes no path; no call was made"})))
        open! (fn []
                (let [pstate (pstate-of read-path source)
                      the-path (path-of source numbers :whole)]
                  (after (if (or (nil? pstate) (nil? the-path))
                           [:no-path]
                           (attempt (fn []
                                      (rama/foreign-proxy-async
                                       the-path pstate
                                       {:pkey (:routing-id source)
                                        :callback-fn (fn [new diff old]
                                                       (on-callback read-path source called new diff old))}))))
                         (fn [push]
                           (if (and run? (< (:max-places-of-one-run numbers) (count @push)))
                             (do (close-push! push) (large!))
                             (when-not (give! {:push push})
                               (close-push! push)))))))]
    (try
      (let [size-then-open! (fn []
                              (after (select-async read-path source (path-of source numbers :size))
                                     (fn [size]
                                       (if (sources/over-the-limit? size (nth (:path source) 2) numbers)
                                         (large!)
                                         (open!)))))]
        (cond
          (not run?)
          (open!)

          (sources/first-read-holds? source)
          (after (select-async read-path source (path-of source numbers :count))
                 (fn [[n]] (if (sources/run-is-whole? n numbers) (open!) (size-then-open!))))

          :else
          (size-then-open!)))
      (catch Throwable t (threw! (what-threw t))))
    (.completeOnTimeout out {:word (late read-path)} (long (:wait-ms numbers)) TimeUnit/MILLISECONDS)
    out))

(defn- held-now
  [^ReadPath read-path]
  @(.-held read-path))

(defn- read-of
  [read-path id]
  (get-in (held-now read-path) [:reads id]))

(defn- given
  "What the opening of an entry of `:pushes` ended in; nil while it is not
  done, or was not begun. It forces no delay."
  [entry]
  (let [opening (:opening entry)]
    (when (realized? opening)
      (let [^CompletableFuture future @opening]
        (when (.isDone future)
          (.getNow future nil))))))

(defn- counted-under
  "What a source's push is counted under. Rama's client sends one routing id
  to one task, so every push of one count is on one task of one table. In a
  streaming lane the routing id is the lane, so every push of every asker
  through one lane is one count. In a microbatch lane the routing id is the
  home of the read, so one count holds the pushes on that gate's table of
  the askers whose reads have one home: on the base, the pushes of the
  askers of one home, and not those of all who read the base.

  Why by routing id and not by task. Rama bounds the pushes on one table on
  one task, and its client gives no call that turns a routing id into a task
  (looked for in `com.rpl.rama`, `PState`, `ProxyState` and `Helpers` of
  Rama 1.6.0). Two routing ids that Rama puts on one task, two streaming
  lanes or two homes, are counted apart, and their pushes together can pass
  Rama's bound while each count is under the read path's own: Rama then
  refuses the opening, and the read gets `:reach-the-store`."
  [source]
  [(:gate source) (:table source) (:routing-id source)])

(defn- one-fewer
  [counts source]
  (let [under (counted-under source)
        n (dec (long (get counts under 1)))]
    (if (pos? n) (assoc counts under n) (dissoc counts under))))

(defn- join!
  "Makes a live read hold sources: one swap of `held` for all of them, so
  that its maps never disagree. The read's own set gets the source as the
  very object that is the entry's key, so a source is held once however
  many reads hold it. After the swap each entry's delay is forced: for an
  entry that another read opened, that is no call to Rama.

  When the entries that the swap would put in take a count past
  `:max-pushes-under-one-routing-id`, the swap changes nothing: the read
  joins none of the sources, also not those that are open already. The test
  and the join are one swap, so two reads that join at once cannot pass the
  bound together. A source that another read holds open is no new push and
  is not counted again.

  Gives `:joined`; `:past-the-bound`; or nil when there was nothing to join,
  or the read was left in the meantime and joined nothing.

  Two reads that open the same source at once end with one entry that holds
  both, and both force the same delay; a delay runs its body once, so the
  push is opened once. A swap that is tried again makes a delay that nobody
  forces."
  [^ReadPath read-path id sources]
  (when (seq sources)
    (let [most (:max-pushes-under-one-routing-id (.-numbers read-path))
          [_ after]
          (swap-vals! (.-held read-path)
                      (fn [held]
                        (if-not (contains? (:reads held) id)
                          held
                          (let [to-open (remove #(contains? (:pushes held) %) sources)
                                counts (reduce (fn [counts source]
                                                 (update counts (counted-under source) (fnil inc 0)))
                                               (:counts held)
                                               to-open)]
                            (if (some #(< most (get counts (counted-under %))) to-open)
                              held
                              (reduce (fn [held source]
                                        (let [pushes (update (:pushes held) source
                                                             (fn [entry]
                                                               (if entry
                                                                 (update entry :readers conj id)
                                                                 (let [called (AtomicBoolean. false)]
                                                                   {:opening (delay (open-push read-path source called))
                                                                    :readers #{id}
                                                                    :called called}))))]
                                          (-> held
                                              (assoc :pushes pushes)
                                              (update-in [:reads id :sources] conj (key (find pushes source))))))
                                      (assoc held :counts counts)
                                      sources))))))]
      (cond
        (not (contains? (:reads after) id))
        nil

        (not-every? #(contains? (get-in after [:reads id :sources]) %) sources)
        :past-the-bound

        :else
        (do (doseq [source sources]
              (when-let [entry (get-in after [:pushes source])]
                (force (:opening entry))))
            :joined)))))

(defn- close-when-given!
  "Closes the push of an entry that a swap took out of `:pushes`: at once
  when its opening is done, else when it is given. It forces the entry's
  delay to do so. A delay that its joiner had not forced yet then opens a
  push that is closed as soon as it is given: that is the price of never
  leaving a push open that no entry holds."
  [entry]
  (try
    (let [^CompletableFuture opening (force (:opening entry))]
      (.thenAccept opening (reify Consumer
                             (accept [_ got]
                               (when-let [push (:push got)]
                                 (close-push! push))))))
    (catch Throwable _ nil)))

(defn- leave-sources!
  "Makes a live read let go of sources (`:all` for every source that it
  holds), or leave whole: one swap of `held`. The count of an entry with no
  reader left goes down at once, before Rama has closed the push, which is
  one reason why the read path's bound stands under Rama's. A push is
  closed only after its entry left the map, so a read that joins meanwhile
  is never handed a push that is closing: it puts a new entry in and opens
  a new push. It takes no turn and waits for nothing. Gives true when
  `:reads` held the read before the swap."
  [^ReadPath read-path id sources whole?]
  (let [[before after]
        (swap-vals! (.-held read-path)
                    (fn [held]
                      (if-let [read (get-in held [:reads id])]
                        (let [leaving (if (= :all sources) (:sources read) (filter (:sources read) sources))
                              [pushes counts] (reduce (fn [[pushes counts] source]
                                                        (if-let [entry (get pushes source)]
                                                          (let [readers (disj (:readers entry) id)]
                                                            (if (empty? readers)
                                                              [(dissoc pushes source) (one-fewer counts source)]
                                                              [(assoc pushes source (assoc entry :readers readers)) counts]))
                                                          [pushes counts]))
                                                      [(:pushes held) (:counts held)]
                                                      leaving)
                              held (assoc held :pushes pushes :counts counts)]
                          (if whole?
                            (update held :reads dissoc id)
                            (update-in held [:reads id :sources] #(reduce disj % leaving))))
                        held)))
        read (get-in before [:reads id])]
    (when read
      (doseq [source (if (= :all sources) (:sources read) sources)
              :let [entry (get-in before [:pushes source])]
              :when (and entry
                         (contains? (:readers entry) id)
                         (not (contains? (:pushes after) source)))]
        (close-when-given! entry)))
    (some? read)))

(defn- known-of
  "`known`, as `softland.store.read-path.sources` takes it, for a live read:
  each source that the read holds and whose push is given, with the value
  that the push holds now. Nil when a push throws as it is read, which
  stops the step: Rama's client throws for a push that another thread's
  `leave` closed in that moment."
  [read-path read]
  (try
    (let [pushes (:pushes (held-now read-path))]
      (persistent!
       (reduce (fn [known source]
                 (if-let [push (:push (some-> (get pushes source) given))]
                   (assoc! known source @push)
                   known))
               (transient {})
               (:sources read))))
    (catch Throwable _ nil)))

;; ----------------------------------------------------- the steps of a live read

(def ^:private ^ThreadLocal telling
  "A mark on a thread, for the length of a call of an asker's function on
  it."
  (ThreadLocal.))

(defn- telling?
  []
  (some? (.get telling)))

(defn- call-marked
  [f change]
  (let [before (.get telling)]
    (.set telling Boolean/TRUE)
    (try (f change)
         :returned
         (catch Throwable _ :threw)
         (finally (if before (.set telling before) (.remove telling))))))

(defn- end-read!
  "Ends a live read that the read path can no longer keep right: in the
  read's turn, the read leaves `:reads` and each push that it holds, and
  its function is called once with {:ended word}. A throw of the function
  there is dropped.

  A read that is not opened yet is taken out and its function is not called:
  the first thing that a function is given is the whole answer, and such a
  read has none. `read-live`, which is still opening it, then finds it gone
  and gives its caller a word. Whether a read is opened changes only in its
  turn, which is why the turn is taken before the read is taken out: an
  opening and an ending of one read never cross."
  [read-path id because carries]
  (when-let [{:keys [turn]} (read-of read-path id)]
    (locking turn
      (let [{:keys [tell opened?]} (read-of read-path id)]
        (when (and (leave-sources! read-path id :all true) opened?)
          (call-marked tell
                       {:ended (words/word :keep-the-read-live (assoc carries :because because))}))))))

(defn- tell!
  "What must hold: the thread holds the read's turn. It looks, just before
  it calls, whether `:reads` still holds the read, so no call is begun
  after `leave` returned."
  [read-path id change]
  (when-let [read (read-of read-path id)]
    (when (= :threw (call-marked (:tell read) change))
      (end-read! read-path id :its-function-threw nil))))

(defn- settle!
  "The first part of a step of an opened read: joins what the read wants and
  does not hold, leaves what it holds and no longer wants, and makes
  `known` again; and the same once more while that changes something, since
  a source that the step joined can be open already, for another read. What
  must hold: the thread holds the read's turn. `known` holds no source that
  the step left, and a source that the step joined only when its push was
  open already. Nil when the step stops: the read is gone, a push threw as
  it was read, or the step ended the read.

  Why a step that does not settle ends its read. What `reading` gives at the
  bound wants sources that the read does not hold, so `known` lacks them,
  and an answer worked out from it would be owed and never given: no push
  that the read holds is the one that it waits for, and no later change
  needs to come."
  [^ReadPath read-path id]
  (loop [round 0 joined #{} left #{}]
    (when-let [read (read-of read-path id)]
      (when-let [known (known-of read-path read)]
        (let [reading (sources/reading (:query read) known :live (.-numbers read-path) (:sources read))
              {:keys [join leave]} (sources/moves reading (:sources read))]
          (cond
            (:too-large reading)
            (do (end-read! read-path id :a-run-grew-too-large (:too-large reading)) nil)

            (and (empty? join) (empty? leave))
            {:reading reading :known known :joined joined :left left}

            (<= (:live max-rounds) round)
            (do (end-read! read-path id :a-step-did-not-settle nil) nil)

            :else
            (do (leave-sources! read-path id leave false)
                (if (= :past-the-bound (join! read-path id join))
                  (do (end-read! read-path id :no-room-for-a-push-it-came-to-need (no-room read-path)) nil)
                  (recur (inc round) (into joined join) (into left leave))))))))))

(declare on-arrival)

(defn- when-given!
  "Has `on-arrival` run when the opening of each joined push is done: at
  once, on this thread, for a push that was open already; else on the
  thread that gives it, a thread of Rama's client or the JVM's timer. No
  thread waits for such a push."
  [read-path id sources]
  (let [pushes (:pushes (held-now read-path))]
    (doseq [source sources]
      (when-let [entry (get pushes source)]
        (when (contains? (:readers entry) id)
          (.whenComplete ^CompletableFuture (force (:opening entry))
                         (reify BiConsumer
                           (accept [_ got _thrown]
                             (try (on-arrival read-path id source got)
                                  (catch Throwable _ nil))))))))))

(defn- after-step!
  "Does what a pure function of `softland.store.read-path.answer` said a
  step does. What must hold: the thread holds the read's turn."
  [^ReadPath read-path id {:keys [tell owed?]} joined]
  (swap! (.-held read-path)
         (fn [held] (if (contains? (:reads held) id) (assoc-in held [:reads id :owed?] owed?) held)))
  (when tell
    (tell! read-path id tell))
  (when-given! read-path id joined))

(defn- on-arrival
  "A push that an opened read came to need is given. An opening that ended
  in a word ends the read: the read cannot be kept right."
  [read-path id source got]
  (when-let [{:keys [turn]} (read-of read-path id)]
    (locking turn
      (let [read (read-of read-path id)]
        (when (and read (:opened? read) (contains? (:sources read) source))
          (if-let [word (:word got)]
            (if (= :serve-a-read-that-large (:could-not word))
              (end-read! read-path id :a-run-grew-too-large (select-keys word [:what :most]))
              (end-read! read-path id :a-read-it-came-to-need-failed (dissoc word :could-not)))
            (when-let [{:keys [reading known joined left]} (settle! read-path id)]
              (after-step! read-path id
                           (answer/after-arrival (:query read) reading known source joined left
                                                 (:owed? (read-of read-path id)))
                           joined))))))))

(defn- on-change
  "A push of an opened read changed. A read that is not opened yet does
  nothing: `read-live` reads the pushes' values itself, in the same turn,
  so a change that falls in the opening is in the first answer or is told
  after it."
  [read-path id source changed old new]
  (when-let [{:keys [turn]} (read-of read-path id)]
    (locking turn
      (let [read (read-of read-path id)]
        (when (and read (:opened? read) (contains? (:sources read) source))
          (when-let [{:keys [reading known joined left]} (settle! read-path id)]
            (let [query (:query read)
                  [meant-before meant-after] (answer/meanings query known source old new)]
              (after-step! read-path id
                           (answer/after-change query reading known source
                                                (sources/places-of query source changed)
                                                meant-before meant-after joined left
                                                (:owed? (read-of read-path id)))
                           joined))))))))

(defn- keys-of-diff
  "The keys that a diff of Rama's names, as `[:put key]` and `[:out key]`;
  Rama expands a diff of several changes into those of a key's change and
  of a key's removal. `:all` for a diff that names no key that way: a whole
  new value, a value sent anew, or a diff of a kind that the processor does
  not handle. For those the read path compares the value before with the
  value after (`softland.store.read-path.sources/named`)."
  [diff]
  (try
    (let [named (ArrayList.)
          unhandled? (AtomicBoolean. false)
          processor (reify
                      Diff$Processor
                      (unhandled [_] (.set unhandled? true))
                      KeyDiff$Processor
                      (processKeyDiff [_ d] (.add named [:put (.getKey ^KeyDiff d)]))
                      KeyRemoveDiff$Processor
                      (processKeyRemoveDiff [_ d] (.add named [:out (.getKey ^KeyRemoveDiff d)])))]
      (.process ^Diff diff processor)
      (if (.get unhandled?) :all (vec named)))
    (catch Throwable _ :all)))

(defn- diff-kind
  "`:own-close` is the last call of a push that the read path closed.
  `:ended-by-the-store` is Rama ending the push after too many errors.
  `:anew` is a whole value sent anew."
  [diff]
  (cond
    (instance? DestroyedDiff diff)             :own-close
    (instance? UngracefulTerminationDiff diff) :ended-by-the-store
    (instance? ResyncDiff diff)                :anew
    :else                                      :change))

(defn- on-callback
  "One callback of a push of Rama's. It never throws into Rama's thread.

  A callback of a push that no entry holds any more, by the mark, does
  nothing. The first callback of a push, a value sent anew, is the value
  that the opening already gave: the mark is set, and nothing more. A later
  value sent anew names no key, so every key of the value before and of the
  value after is compared; a key whose row is the same before and after is
  dropped, and the callback gives nothing when no key is left."
  [^ReadPath read-path source ^AtomicBoolean called new diff old]
  (try
    (let [entry (get-in (held-now read-path) [:pushes source])
          kind (diff-kind diff)
          first? (and (= :anew kind) (.compareAndSet called false true))]
      (when (and entry (identical? called (:called entry)) (not= :own-close kind) (not first?))
        (.set called true)
        (if (= :ended-by-the-store kind)
          (doseq [id (:readers entry)]
            (end-read! read-path id :the-store-ended-a-push nil))
          (let [changed (sources/named source (if (= :anew kind) :all (keys-of-diff diff)) old new)
                most (:max-places-of-one-run (.-numbers read-path))]
            (when (seq changed)
              (if (and (= :run (first (:path source))) (< most (count new)))
                (doseq [id (:readers entry)]
                  (end-read! read-path id :a-run-grew-too-large {:what :places-of-one-run :most most}))
                (doseq [id (:readers entry)]
                  (on-change read-path id source changed old new))))))))
    (catch Throwable _ nil)))

;; ------------------------------------------------------------------ the calls

(defn open
  "Gives a read path, or the word `:find-the-module` when the cluster gave
  one of the ten tables not under that name; no read path is then opened.
  `numbers` is a map of the read path's numbers
  (`softland.store.read-path.query/default-numbers`), or nil.

  The ten fetches of a client are made once for a process and on no read's
  way. The read path borrows the handles and uses them from every caller's
  thread; the opener owns the cluster handle, and nothing closes the read
  path.

  A process opens one read path: two read paths share no push, so every push
  of those who look at the same thing would be open twice. Opening checks
  nothing of who opens it."
  ([cluster module-name] (open cluster module-name nil))
  ([cluster module-name numbers]
   (try
     (->ReadPath module-name
                 (rama/foreign-pstate cluster module-name "$$streaming-facts")
                 (rama/foreign-pstate cluster module-name "$$microbatch-facts")
                 (rama/foreign-pstate cluster module-name "$$streaming-facts-by-value")
                 (rama/foreign-pstate cluster module-name "$$microbatch-facts-by-value")
                 (rama/foreign-pstate cluster module-name "$$streaming-line")
                 (rama/foreign-pstate cluster module-name "$$microbatch-line")
                 (rama/foreign-pstate cluster module-name "$$streaming-things")
                 (rama/foreign-pstate cluster module-name "$$microbatch-things")
                 (rama/foreign-pstate cluster module-name "$$streaming-task")
                 (rama/foreign-pstate cluster module-name "$$microbatch-task")
                 (query/numbers numbers)
                 (atom {:pushes {} :counts {} :reads {}})
                 (AtomicLong. 0))
     (catch Throwable t
       (words/word :find-the-module (assoc (what-threw t) :module module-name))))))

(defn- checked
  "The head of `read-once` and `read-live`. It makes no call to Rama."
  [thing]
  (if (telling?)
    [:word (words/word :read-inside-an-askers-function nil)]
    (let [{query :ok why :refuse at :at} (query/check thing)]
      (if why
        [:word (words/word :read-the-query {:why why :at at})]
        [:ok query]))))

(defn- read-once*
  "It reads `held` nowhere: a source that a live read holds open is selected
  all the same (`read-once` says why). A run whose first read was not whole
  is read whole only in a lane that she may see. A run that has no first
  read (`softland.store.read-path.sources/first-read-holds?`) is wanted
  only in such a lane, and is read by its size and then whole."
  [^ReadPath read-path query]
  (let [numbers (.-numbers read-path)
        deadline (+ (now-ms) (:wait-ms numbers))]
    (loop [round 0 known {} first-reads #{}]
      (let [reading (sources/reading query known :once numbers)
            to-read-whole (filterv #(= :yes (get (:standing reading) (:lane %))) first-reads)]
        (cond
          (:too-large reading)
          (words/word :serve-a-read-that-large (:too-large reading))

          (seq to-read-whole)
          (let [[outcome got] (run-now read-path to-read-whole deadline)]
            (if (= :word outcome)
              got
              (recur round (merge known got) (reduce disj first-reads to-read-whole))))

          (empty? (:missing reading))
          (answer/whole query reading known)

          (<= (:once max-rounds) round)
          (late read-path)

          :else
          (let [unread (:missing reading)
                no-first-read? (fn [source] (and (= :run (first (:path source)))
                                                 (not (sources/first-read-holds? source))))
                [outcome got] (select-now read-path
                                          (sources/calls-for-once (remove no-first-read? unread) numbers)
                                          deadline)
                [outcome-whole got-whole] (if (= :word outcome)
                                            [outcome got]
                                            (let [runs (filterv no-first-read? unread)]
                                              (if (seq runs) (run-now read-path runs deadline) [:ok {}])))]
            (cond
              (= :word outcome) got
              (= :word outcome-whole) got-whole
              :else
              (recur (inc round)
                     (merge known got got-whole)
                     (into first-reads
                           (filter (fn [source]
                                     (and (= :run (first (:path source)))
                                          (not (sources/run-is-whole? (get got source) numbers)))))
                           (keys got))))))))))

(defn read-once
  "Gives an answer (`softland.store.read-path.answer`; of a lane's acts,
  `softland.store.read-path.acts`, which says what that read costs), or one
  of the read path's words. It never throws, and it holds nothing when it
  returns.

  What it costs. Each round is the calls that are missing, one for each
  table under each routing id, sent at once; at most 6 rounds. A lane's
  rights and a place in it are one call: a place through three lanes is
  three calls in one round, when no permission of hers in a lane that is
  open to its members was given her by another; for each that was, the rows
  above it are one round more, one call for each such lane, in one round for
  all of them. From a lane, the start lane's calls come first and the others
  after them. What points at a thing takes one round more for the orders and
  one for the checks of nearer lanes, each check one seek. A run of more
  than `:run-start-places` places costs two calls more. A microbatch lane is
  read on the task of the read's home, in a call of its own table: a run
  across its entities is one call, as in a streaming lane.

  A query that names its run reads the row of the run's permission in the
  call that reads her own. When that row holds a permission of hers, the
  rows above it are read in the round that reads the rows above her own
  permissions: one round more, with one call, only when she holds none in
  that lane or holds its first permission. When the row holds another
  holder's permission, nothing above it is read. Counted by the read path's
  pure proof (`the-loop-ends`).

  It reads the tables, and takes no value of a push, also where a live read
  holds one open on a source that this read wants: it pays that round trip.
  A push can be behind the tables by as long as the push is late; a read
  asked once that took its value could give a caller the state before her
  own act, straight after that act was acked. So what `read-once` gives is
  what the tables held when Rama served its calls: an act whose ack a caller
  has, in a streaming lane, is in it. An act of a microbatch lane is in it
  once the task that serves the read holds its rows, which is a task of the
  read's home and can be another than the one that decided the act.

  The lanes are read at moments that differ; nothing lines them up."
  [^ReadPath read-path query]
  (try
    (let [[outcome query] (checked query)]
      (cond
        (= :word outcome) query
        (= :acts (first (:asks query))) (read-acts-once read-path query)
        :else (read-once* read-path query)))
    (catch Throwable t
      (words/word :reach-the-store (what-threw t)))))

(defn- wait-for-held!
  "It holds no turn while it waits: a callback that waited for the read's
  turn would hold a thread of Rama's client, and the opening of a push can
  need that thread."
  [read-path id deadline]
  (let [pushes (:pushes (held-now read-path))]
    (loop [todo (seq (:sources (read-of read-path id)))]
      (if-let [source (first todo)]
        (if-let [entry (get pushes source)]
          (let [[outcome got] (within (force (:opening entry)) (- deadline (now-ms)))]
            (case outcome
              :ok    (if-let [word (:word got)] [:word word] (recur (next todo)))
              :late  [:word (late read-path)]
              :threw [:word (words/word :reach-the-store got)]))
          (recur (next todo)))
        [:ok]))))

(defn- read-live*
  "The body of `read-live`. When nothing is missing it takes the read's
  turn, reads the pushes' values and asks once more. If a change that came
  meanwhile made a source wanted, it lets the turn go and goes round again.
  Else, in that turn, it makes the answer, marks the read opened, and calls
  the function with {:anew answer}: so the first thing that the function is
  given is the whole answer, and no change is told before it. A callback
  that comes while the read opens takes the turn, finds the read not opened
  and does nothing; a push holds its new value before its callback runs, so
  that change is in the answer, or it finds the read opened and is told
  after it. A read that ends in a word leaves every push that it joined.

  A read that is ended while it opens, because Rama ended a push that it had
  joined or a run of it grew too large, is gone from `:reads` before it was
  opened: its function was not called, and this call gives the word of a
  read that got no answer."
  [^ReadPath read-path query f]
  (let [numbers (.-numbers read-path)
        held (.-held read-path)
        id (.incrementAndGet ^AtomicLong (.-next-id read-path))
        turn (Object.)
        deadline (+ (now-ms) (:wait-ms numbers))
        give-up (fn [word] (leave-sources! read-path id :all true) word)
        wanted-now (fn []
                     (when-let [read (read-of read-path id)]
                       (when-let [known (known-of read-path read)]
                         (let [reading (sources/reading query known :live numbers (:sources read))]
                           (assoc (sources/moves reading (:sources read)) :reading reading :known known)))))]
    (swap! held assoc-in [:reads id]
           {:query query :tell f :turn turn :sources #{} :opened? false :owed? false})
    (try
      (loop [round 0]
        (let [{:keys [reading join leave]} (wanted-now)]
          (cond
            (< deadline (now-ms))
            (give-up (late read-path))

            (nil? reading)
            (give-up (late read-path))

            (:too-large reading)
            (give-up (words/word :serve-a-read-that-large (:too-large reading)))

            (and (or (seq join) (seq leave) (seq (:missing reading)))
                 (<= (:live max-rounds) round))
            (give-up (late read-path))

            (or (seq join) (seq leave) (seq (:missing reading)))
            (do (leave-sources! read-path id leave false)
                (if (= :past-the-bound (join! read-path id join))
                  (give-up (words/word :keep-more-reads-live (no-room read-path)))
                  (let [[outcome word] (wait-for-held! read-path id deadline)]
                    (if (= :word outcome)
                      (give-up word)
                      (recur (inc round))))))

            :else
            (let [opened (locking turn
                           (let [{:keys [reading known join leave]} (wanted-now)]
                             (when (and reading
                                        (not (:too-large reading))
                                        (empty? join) (empty? leave) (empty? (:missing reading)))
                               (let [answer (answer/whole query reading known)]
                                 (swap! held (fn [now] (if (contains? (:reads now) id)
                                                         (-> now
                                                             (assoc-in [:reads id :opened?] true)
                                                             (assoc-in [:reads id :owed?] (answer/mark-of answer)))
                                                         now)))
                                 (tell! read-path id {:anew answer})
                                 {:now answer :leave-with (->LiveRead held id)}))))]
              (cond
                opened opened
                (nil? (read-of read-path id)) (give-up (late read-path))
                :else (recur (inc round)))))))
      (catch Throwable t
        (give-up (words/word :reach-the-store (what-threw t)))))))

(defn read-live
  "Gives {:now the answer, :leave-with a handle}, or one of the read path's
  words. A query of a lane's acts is not kept live: it gives
  `:keep-a-read-of-acts-live` at once, with no call to Rama. It never
  throws.

  The function is called with one map at a time:

    {:anew answer}                    the whole answer, in the place of
                                      everything that came before. The first
                                      call is always this, with the answer
                                      that `:now` holds
    {:changed {place place-answer}}   the places that a change names, each
                                      with what the read gives there now
    {:ended word}                     the read ended; the function is not
                                      called again

  A caller who keeps a screen uses the function's calls alone: a screen that
  is made only from what the function was given is right, in order. `:now`
  is there for a caller that keeps none. The namespace docstring says on
  which thread each call is made, what the function must not do, and what a
  slow function holds back.

  A function that throws at its first call ends the read then: `read-live`
  still gives {:now :leave-with}, and the handle is not open. A thing that
  is no function is as a function that throws at its first call. The read
  path opens nothing again by itself; the caller asks again.

  What it follows. Every fact that shapes the read, each by a push that
  those who need it share: the lane's own rows (whether it is made, who may
  see it, what it builds on, its marked keys) as one push for a lane; the
  asker's permission, and the permission of each holder above hers, only
  while the lane is made and its setting is `:members`; for a query that
  names its `:session`, the permission of that run in each such lane and of
  each holder above it; and the facts. She may see such a lane while either
  stands (`softland.store.read-path.lanes/either`): when the last of hers
  that stood ends, or one above it, her read leaves the lane as it does when
  a setting goes to `:members`. When what shapes the read changes, the asker
  is given the whole answer anew. When a lane is no longer rightful for her,
  her read leaves that lane's facts at once; the answer then says that the
  lane is left out, and nothing of the lane's facts. A fact of that lane
  that Rama called back before the setting's change reached the read path
  can still be told: as late as a push is, and no sooner.

  What it costs. One push for each source: for a place through k lanes of
  which g are \"members\", k + g + k, and one more in each such lane for
  each permission above hers there: none when she is the lane's first
  holder. A query that names its run holds, in each such lane, one push more
  on the run's row, and one for each permission above the one that the row
  holds. A push on a row above hers is read after her own row and names its
  stamp, so askers share it only when their rows were written by one
  decision (`softland.store.read-path.sources/chain-sources` says why). For
  what points at a thing, one push more for each place of a farther order
  and each nearer lane. Each open push costs its lane's gate at each write.
  Nothing bounds how long a caller stays but the end of the process: the
  read and the pushes of a caller that never leaves stay, count toward the
  read path's bound on pushes, and `open-count` shows them."
  [^ReadPath read-path query f]
  (try
    (let [[outcome query] (checked query)]
      (cond
        (= :word outcome) query
        (= :acts (first (:asks query))) (words/word :keep-a-read-of-acts-live nil)
        :else (read-live* read-path query f)))
    (catch Throwable t
      (words/word :reach-the-store (what-threw t)))))

(defn leave
  "Gives `:left`; or `:not-open` for a read that was left or ended before,
  and for a thing that is no handle of this read path. It takes no turn and
  waits for nothing, so an asker's function may call it, for its own read
  or for another. A call of the function that another thread had begun to
  make can still arrive in the moment in which `leave` returns; none is
  begun after that. It never throws."
  [^ReadPath read-path handle]
  (try
    (if (and (instance? LiveRead handle)
             (identical? (.-held read-path) (.-held ^LiveRead handle))
             (leave-sources! read-path (.-id ^LiveRead handle) :all true))
      :left
      :not-open)
    (catch Throwable _ :not-open)))

(defn open-count
  "A caller that never leaves shows here. It makes no call to Rama."
  [^ReadPath read-path]
  (let [held (held-now read-path)]
    {:live-reads (count (:reads held))
     :pushes     (count (:pushes held))}))
