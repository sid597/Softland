;; Before changing this file, read the plan of this part again:
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-read-path.md
;; and, for where a microbatch layer is read, the section "The read path" of
;; src/proposal/backend-on-rama-2026-10-02/run-again/PLAN-the-bases-gate.md
;; and the rulings that came after its check, which the plan does not hold:
;; 78 and 80; and 86, 87, 89 and 91, which the review of the whole built
;; (FULL_SPEC_REVIEW.md beside the plan). They are in
;; src/proposal/backend-on-rama-2026-10-02/reads/PROGRESS.md

(ns softland.store.read-path
  "The read path: the one piece of code by which a fact comes back out of
  the store to a caller in the runtime's process, once or kept live. It
  requires Rama, and of Rama's client only this: `foreign-pstate`,
  `foreign-select-async`, `foreign-proxy-async` and `close!`. It is the one
  namespace of the store's source that opens a
  table to read a fact; the door (`softland.store.door`) is the way in, and
  neither calls the other. So in the runtime the module has two clients.

  A caller hands the read path a query written as data
  (`softland.store.read-path.query`): a place; an entity, or a run of
  neighbouring places; what points at a thing; with who asks, and a list of
  layers or a layer to take the list from. The read path reads whether the
  asker may see each layer, reads each layer that she may see, and puts the
  layers together: for each place the nearest layer that holds a fact wins.
  It reads the gates' four tables of facts straight; no query of the store
  stands between.

  The calls, each of which but `open` takes the read path first:

    open        a cluster handle, the module's name, and when the opener
                wants, a map of the read path's numbers -> a read path, or
                a word
    read-once   a query -> an answer, or a word
    read-live   a query, and a function of one argument ->
                {:now an answer, :leave-with a handle}, or a word. The
                function is called first with the whole answer, and then
                with each change
    leave       a handle that `read-live` gave -> :left, or :not-open
    open-count  nothing more -> {:live-reads n :pushes n}

  An answer is a plain map (`softland.store.read-path.answer`). A word is
  one of the read path's own eight (`softland.store.read-path.words`): a
  map that holds `:could-not`. No call throws for a query, a function or a
  handle that a caller gave, or for what Rama did, and a read that failed
  has never said \"no fact\". What must hold: the first thing that a call
  is given is a read path that `open` gave. Every
  wait for Rama has the read path's own bound, `:wait-ms`. The read path
  tries nothing a second time by itself: the caller asks again.

  How it reads. `softland.store.read-path.sources/reading` says, from what
  is known so far, which sources a read wants and which of them are
  missing; a source is one row or one run of neighbouring rows of one table
  on one task. This file only fetches what is missing and asks again, at
  most `max-rounds` times, and hands `softland.store.read-path.answer` what
  it fetched. Asked once, every missing source of one table under one
  routing id goes in one call of `foreign-select-async`. Kept live, every source is one push,
  `foreign-proxy-async`, shared by every live read that wants the same
  source and closed when the last of them leaves.

  Every path that the read path gives Rama is one of five forms that
  `path-of` builds from texts of `softland.store.record.rows`. No caller
  gives a path, a table or a routing id. So the read path never opens a
  push on a whole table: the widest thing that a query can name is the
  whole of one layer, which is a run. Rama does not refuse a path that it
  cannot serve cleanly (one such select stopped a worker of the in-process
  cluster, in a probe of this build's plan), so the read path sends no
  other form. A run is read whole only after a bounded read has shown that
  it holds no more places than one read may (`open-push`, `run-now`).

  What crosses from Rama to the runtime, and what a caller is handed. Rama
  sends the runtime each whole row of what a read reads: the value, who
  wrote the fact, the permission that its act named. A caller is handed who
  wrote a fact, or the permission, only when her query asked for it
  (`softland.store.read-path.answer/place-answer`). A read asked once reads
  a layer's fact in the same call as its rights, so the runtime can read
  one row, or the first places of a run, of a layer that the asker may then
  not see; they are dropped, and nothing of them is handed over. Each
  program that can open Rama's ports is trusted with the whole store
  (src-store/README.md, decisions of 11 and 12 October 2026, entry 4).

  State. The read path owns no durable state, and it writes nothing: it
  appends to no depot and keeps no record of who read what. An opened read
  path borrows four handles of Rama's client, the tables of facts and of
  facts by value of both gates; the opener owns the cluster handle, and
  nothing closes the read path. It holds its seven numbers, fixed at the
  opening. Its one memory is `held`, an atom of three maps, in the memory of
  one process:

    :pushes  a source -> {:opening a delay around the one call that opens
             its push, :readers the ids of the live reads that hold it,
             :called a mark that its first callback came}
    :counts  a gate, a table and a routing id -> how many entries of
             `:pushes` are under them (`counted-under`): the count that the
             read path's own bound on its pushes is held to
    :reads   a read's id -> its query, its function, its turn, the set of
             the sources that it holds, whether it is opened, and whether
             the whole answer is owed

  That is who looks at what, what each asked for, the pushes with a count
  of them, and one mark for each read. It holds no row, no value, no answer
  and no place-answer.
  The values are in Rama's pushes and nowhere else: each holds the current
  value of its path, and Rama keeps it level (Sid, 11 October 2026: \"i
  also don't consider it a cache\"). What a live read gives is worked out
  at each change, from the values that the pushes hold at that moment, for
  the places that the change names. The mark \"the whole answer is owed\" is
  true or false and no value of the tables: one step sets it when it finds
  that what shapes the read changed, and the step that gives the whole
  answer takes it off. A read asked once holds what it fetched until it
  returns, and it fetches every source from the tables by a select of its
  own: it takes no value of a push, also where a live read holds one open
  on the same source (`read-once`). Nothing shown to a caller is anything
  but what the tables held: there is no optimistic value.

  All of `held` is lost when the process ends. Rama clears a push that
  sends no heartbeat (read in Rama's pages, not run). Nothing tells a
  caller; each asks again. A process opens one read path: two read paths
  share no push.

  A live read, and the read's turn. `read-live` calls the asker's function
  with one map at a time:

    {:anew answer}                    the whole answer, in the place of
                                      everything that came before. The first
                                      call of the function is always this
    {:changed {place place-answer}}   the places that a change names, each
                                      with what the read gives there now
    {:ended word}                     the read ended; the function is not
                                      called again

  One call of an asker's function runs at a time for one read, in the order
  in which the calls were worked out. What holds that is called the read's
  turn here, and never a lock: in this tree a lock is an encryption key,
  and the read path reads none.

  The first call is made by `read-live` itself, on the caller's own thread,
  before it returns and before any change is told. Every later call is made
  on a thread of Rama's client: the thread of the push whose change it is,
  or the thread on which a push that the read came to need was given; for
  an `{:ended ...}` after a wait that passed, on the thread of the JVM's
  own timer. The function must return at once. What was run for the plan's
  check, on an in-process cluster whose client had two such threads: while
  one function stayed for 2.5 s, every push whose callbacks ran on that
  thread waited, pushes of other askers among them, and so did selects and
  openings of pushes from other threads when Rama's client gave their
  answer on that thread. So one slow function holds back the reads of
  others, `read-once` and `read-live`, up to their own bound. A caller that
  does more than hand the change on takes a thread of its own. Rama's own
  callback has this rule, and the read path passes it on.

  The function may call `leave`, for its own read or for another: `leave`
  takes no turn and waits for nothing. It must not call `read-once` or
  `read-live`: they wait for Rama on a thread that Rama may need for the
  answer. The read path turns such a call away at once, with the word
  `:read-inside-an-askers-function` and no call to Rama.

  What Rama sends at a change, and how the read path takes it. Three kinds
  of diff reach a callback. A diff that names keys: what a push on a run
  gets when a row of the run is written or removed, and the other rows stay
  the objects that they were. A new value that names no key: what a push on
  one row gets. And a whole value sent anew: the first callback of every
  push, and what Rama sends a reader that it left out of a change. The read
  path takes the three the same way: among the keys that the diff names, or
  among every key of the value before and of the value after when it names
  none, it handles the keys whose rows differ
  (`softland.store.read-path.sources/named`). So an asker is told the
  places that changed, and not a whole answer, whichever way Rama sent the
  change, and nothing when no row differs. That a push is told each write
  by a diff of its own rests on what a row holds: no field of the two
  tables that the read path reads is a byte array
  (`softland.store.record.tables/facts` says what was run).

  How many pushes. Rama bounds the pushes on one table on one task, by a
  dynamic option of its own, `pstate.proxy.limit`, whose default is 1,000
  (the name is read in Rama's own message; no run of this build set the
  option). As run on the store's module, in-process, with the default:
  1,000 pushes opened on one table on one task, the next one was refused,
  and a push that was closed made room for one more; and Rama refuses one
  opening at a time, so a read that asked for about 10,000 pushes was
  turned away only after seconds. A streaming layer is whole on one task,
  so every live read through it, of every asker, counts toward that one
  bound, and so do the other layers of that task. A microbatch layer is
  whole on every task, and a read holds its pushes on the task of the
  read's home; so the live reads of that layer are spread over the tasks as
  their homes are, and on one task its table counts the pushes of every
  home that Rama puts there.

  So the read path has a bound of its own, under Rama's. It counts the
  pushes that it holds on each table under each routing id
  (`counted-under`, `:counts`), and a read kept live whose next pushes
  would take a count past `:max-pushes-under-one-routing-id`, 900, is
  turned away at once, with the word `:keep-more-reads-live`: none of the
  pushes of that round is opened, and the pushes that the read joined in
  earlier rounds are left. An open read that comes to need such a push
  ends, with `:no-room-for-a-push-it-came-to-need`. A read asked once
  opens no push and is not held to the bound. And one read of what points
  at a thing, kept live, may need 250 checks of nearer layers, each of
  them a push (`:max-checks-of-one-live-read`); a read that needs more is
  turned away with `:serve-a-read-that-large` before one check is opened.

  What the count bounds. For a streaming layer the routing id is the
  layer, so the count is of every push of every asker through that layer.
  For a microbatch layer the routing id is the home of the read
  (`softland.store.read-path.sources`), so the count is of the pushes that
  the askers of one home hold on one table of the microbatch gate: the 900
  bounds what the reads that start from one layer hold on the base, and
  not what all reads hold on the base.

  What that bound does not reach. It counts by routing id, since Rama's
  client does not say which task a routing id is on: two streaming layers
  that Rama puts on one task are counted apart, and so are two homes on one
  task, whose pushes on the base are on one table of that task. A live read
  of the base holds two pushes at least on the base's facts under its home,
  the base's own rows and one fact source, and no home shares them with
  another. So Rama's bound holds the homes of one task that read the base
  live to half its number at most, and the read path's count does not see
  that (derived from the two counts; no run of this build opened that
  many). And it counts one process, where Rama's bound holds for every
  client of the cluster together. Past either, Rama refuses the opening, and the read
  gets `:reach-the-store`, as any opening that throws does: `read-live`
  gives that word and leaves every push that it joined, and an open read
  that came to need the push ends. The read path sets none of Rama's
  options. Its 900 stands under the default of
  `pstate.proxy.limit`; to raise that option, and this number with it, is
  a setting for Sid to weigh. Both numbers are the build's, kept and not
  decided, and so is the way of the checks that needs a push for each.

  A screen is shown the newest state; a caller that must see every act
  cannot rest on this call. The function is not called once for each act:
  two acts on one place can come as one change, Rama can leave a reader out
  of a change and send the value anew, and a second attempt of a batch that
  writes the same row gives nothing. When two changes cross, a place can
  come a second time with the answer that it already had; it never comes
  with an answer that is older than one that came before.

  A microbatch layer. A layer's gate is the depot that it starts with
  (`softland.store.gate.first-act/depot-at-making`). In a streaming layer
  every row is on the layer's task. In a microbatch layer every row of the
  facts, and of the facts by value, is on every task, the same rows
  everywhere: its gate decides an act on the layer's task and writes the
  rows of an accept on each task. So a read of such a layer needs one task,
  and the read path reads it on the task of the read's home, the layer that
  the read starts from: an asker reads the base where her own layer lives,
  and a read that starts from the base reads it on the base's own task
  (`softland.store.read-path.sources`, \"Where a layer is read\"). A run
  across entities, and a run of the facts by value, come whole from that
  one task. A row of a microbatch layer is read straight, as a row of a
  streaming layer is, and nothing of a batch is read: a second attempt of a
  batch writes the same row again, and a push that calls back with a row
  that is the same tells an asker nothing. The layers of one read are read
  at moments that differ, and two tasks show one batch at two instants
  (read in Rama's page on microbatch topologies), so two askers with two
  homes can be told of one act of the base at two instants; nothing lines
  them up. What the read path rests on and does
  not test at a read: that every task holds the rows. A task that held
  fewer would give the askers of its homes fewer, and no read would
  notice.

  Room that this code leaves, each a thing that is not in this build and
  that this code decides nothing of:

    - sign-in, in front of the read path: who asks is what the caller says,
      and `:system` is a who like any other;
    - a network edge, a screen, a view, a tool, an agent: the proof is the
      only asker, and no inhabitant uses the read path yet;
    - a record of who read what: nothing is kept of a read;
    - a new word of a query (finding by text, looking back, what stands on
      a fact, everything one person wrote): one entry in each of three maps
      (`softland.store.read-path.query/words`);
    - a permission that ends: the push on the asker's permission going from
      a row to none takes the same road as a setting that goes to
      \"members\".

  Approach. The file has these sections, in this order: the read path as a
  value; the calls of Rama's client and the five forms of path; a read
  asked once; the pushes and their sharing; the steps of a live read; the
  five calls."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
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
  "Takes the name of the module that a read path is opened on. Gives the
  text that the read path prints as: the words \"read-path\" and that name,
  and nothing else. No handle and no asker's query is in it, whatever
  prints the read path."
  [module]
  (str "#<read-path " module ">"))

(deftype ReadPath [module
                   streaming-facts
                   microbatch-facts
                   streaming-facts-by-value
                   microbatch-facts-by-value
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
             :doc "Makes an opened read path, an object of a type of its own.
  `open` is the one caller. A read path holds the module's name; the four
  handles of Rama's client, borrowed; its seven numbers; `held`, an atom of
  {:pushes :counts :reads} (the namespace docstring says what each holds); and the
  count from which the ids of its live reads are made. It prints as
  `printed` says, by the type's two print methods and its `toString`.")

(deftype LiveRead [held id]
  Object
  (toString [_] (str "#<live-read " id ">")))

(defmethod print-method LiveRead [^LiveRead live-read ^Writer w]
  (.write w (.toString live-read)))

(defmethod print-dup LiveRead [^LiveRead live-read ^Writer w]
  (.write w (.toString live-read)))

(alter-meta! #'->LiveRead assoc
             :private true
             :doc "Makes the handle of a live read, which `read-live` gives and
  `leave` takes: the `held` atom of the read path that opened the read, so
  that a handle of another read path is told apart, and the read's id. It
  prints as the words \"live-read\" and that id.")

(def ^:private max-rounds
  "At how many rounds a read stops when it does not settle. A round fetches
  every source that is missing and asks what is wanted again. Five are
  derived from the five ranks of a source
  (`softland.store.read-path.sources`); three more are for a list or a
  setting that is written while a live read opens. A number of the build,
  kept and not decided."
  8)

(defn- now-ms
  "Gives the JVM's steady clock in milliseconds, to bound a wait. It is no
  time of day."
  []
  (quot (System/nanoTime) 1000000))

;; ------------------------------------------------ the calls of Rama's client

(defn- what-threw
  "Takes what a call of Rama's client threw. Gives {:threw the name of its
  class, :said its message}, for a word to carry. When it is an
  `InterruptedException`, the thread's interrupt mark is set again first:
  the read path turns the throw into a word, and without the mark the
  caller's thread would not know that it was interrupted."
  [^Throwable t]
  (when (instance? InterruptedException t)
    (.interrupt (Thread/currentThread)))
  {:threw (.getName (class t)) :said (.getMessage t)})

(defn- attempt
  "Calls a function of no argument that makes one call of Rama's client.
  Gives [:ok what it gave], or [:threw what `what-threw` gives]. Every call
  of the client that the read path makes after its opening goes through
  here, so none of them throws out of a call of the read path, and none
  leaves a thrown thing in the delay of a push."
  [f]
  (try [:ok (f)]
       (catch Throwable t [:threw (what-threw t)])))

(defn- within
  "Waits for a future of Rama's client, with a bound in milliseconds. Gives
  [:ok its value], [:threw info] when it ended with a throw or the wait was
  interrupted, or [:late] when the bound passed first. A push that is given
  after its own bound is closed where its future is made (`open-push`)."
  [^CompletableFuture future ms]
  (try [:ok (.get future (max 0 (long ms)) TimeUnit/MILLISECONDS)]
       (catch TimeoutException _ [:late])
       (catch ExecutionException e [:threw (what-threw (or (.getCause e) e))])
       (catch Throwable t [:threw (what-threw t)])))

(defn- pstate-of
  "Takes the read path and a source. Gives the handle of the source's table:
  the facts, or the facts by value, of the source's gate. nil for a source
  whose gate or table the read path does not know."
  [^ReadPath read-path source]
  (case [(:gate source) (:table source)]
    [:streaming :facts]           (.-streaming-facts read-path)
    [:microbatch :facts]          (.-microbatch-facts read-path)
    [:streaming :facts-by-value]  (.-streaming-facts-by-value read-path)
    [:microbatch :facts-by-value] (.-microbatch-facts-by-value read-path)
    nil))

(defn- path-of
  "The one function that makes a path of Rama's. Takes a source, the numbers
  and a step. Gives one of four forms, or nil for a source or a step of any
  other kind; the read path then makes no call.

    a source of one row, `:whole`    (keypath key)
    the layer's own rows, `:whole`   (sorted-map-range from to)
    a run, `:whole`                  (sorted-map-range from to): a select
                                     of the run, or its push
    a run, `:first`                  a walk of `:run-start-places` and one
                                     rows from the run's start, cut at the
                                     run's end: the first read of a run. Only
                                     for a run whose two ends Rama cuts
                                     rightly in memory
                                     (`softland.store.read-path.sources/first-read-holds?`)
    a run, `:count`                  the same, counted on the task
    a run, `:size`                   a walk of `:max-places-of-one-run` and
                                     one rows from the run's start, with no
                                     end: its count, and the key of its last
                                     row. Alone in its call, since it gives
                                     one value or two

  The fifth form is a `multi-path` of these, which `select-now` makes for
  the reads of one call. What must hold: the two texts of a source were
  made by `softland.store.record.rows` of ids that were tested first. A
  push is opened on the first three only."
  [source numbers step]
  (let [[kind a b] (:path source)
        ok? (and (string? a) (or (= :row kind) (string? b)))]
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
        nil))))

(defn- select-async
  "Makes one select of a source's table, which does not block. Takes the
  read path, a source, which names the table and the routing id, and a path
  of `path-of`'s. Gives [:ok a future of the values that the path navigates
  to], [:threw info] when the call threw, or [:no-path] when the path or
  the table is nil: no call is then made."
  [read-path source the-path]
  (let [pstate (pstate-of read-path source)]
    (if (or (nil? pstate) (nil? the-path))
      [:no-path]
      (attempt (fn [] (rama/foreign-select-async the-path pstate {:pkey (:routing-id source)}))))))

(defn- close-push!
  "Closes a push of Rama's. It returns at once and waits for nothing, also
  inside a callback of that push (run for the plan's check). A throw is
  dropped: a push that is closed a second time throws nothing, and a push
  that cannot be closed is left to Rama, which clears a push that sends no
  heartbeat."
  [push]
  (try (rama/close! push)
       (catch Throwable _ nil)))

;; ------------------------------------------------------ a read asked once

(defn- late
  "Takes the read path. Gives the word of a wait that passed, with the wait
  that the read path holds."
  [^ReadPath read-path]
  (words/word :get-an-answer-within-its-wait {:waited-ms (:wait-ms (.-numbers read-path))}))

(defn- too-large
  "Takes the read path. Gives the word of a run that holds more places than
  one read may."
  [^ReadPath read-path]
  (words/word :serve-a-read-that-large
              {:what :places-of-one-run :most (:max-places-of-one-run (.-numbers read-path))}))

(defn- no-room
  "Takes the read path. Gives what the word of the read path's own bound on
  its pushes carries: what was too many, and the bound."
  [^ReadPath read-path]
  {:what :pushes-under-one-routing-id
   :most (:max-pushes-under-one-routing-id (.-numbers read-path))})

(defn- gather
  "Waits for selects that were all sent already. Takes the read path, a
  sequence of what `select-async` gave, and the moment at which the call's
  one wait ends. Gives [:ok the values of each select, in order], or
  [:word a word]: `:reach-the-store` for a select that threw or whose
  source has no path, `:get-an-answer-within-its-wait` when the wait
  passed."
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
  "Reads the sources of the calls of one round, each call one round trip to
  one task, all sent before any is waited for. The sources of one call have
  one table and one routing id, so the first of them names both. Takes the read path, the
  calls (`softland.store.read-path.sources/calls-for-once`) and the moment
  at which the wait ends. Gives [:ok a map from each source to its value]
  or [:word a word]. Each call is one `multi-path` of its sources' paths: a
  row for a source of one row, the rows for the layer's own rows, and the
  first read for a run. A call whose values are not one for each path, in
  the paths' order, cannot be told apart from another, so it ends in
  `:reach-the-store`."
  [^ReadPath read-path calls deadline]
  (let [numbers (.-numbers read-path)
        step-of (fn [source] (if (= :run (first (:path source))) :first :whole))
        sent (mapv (fn [{:keys [sources]}]
                     (let [paths (mapv #(path-of % numbers (step-of %)) sources)]
                       (if (some nil? paths)
                         [:no-path]
                         (select-async read-path (first sources) (apply path/multi-path paths)))))
                   calls)
        [outcome got] (gather read-path sent deadline)]
    (if (= :word outcome)
      [:word got]
      (if (every? true? (map (fn [call values] (= (count (:sources call)) (count values))) calls got))
        [:ok (into {} (mapcat (fn [call values] (map vector (:sources call) values)) calls got))]
        [:word (words/word :reach-the-store
                           {:said "a call gave another count of values than it had paths"})]))))

(defn- run-now
  "Reads whole the runs whose first read was not the whole run: the second
  and the third step of a run. Takes the read path, the sources of those
  runs, and the moment at which the wait ends. Gives [:ok a map from each
  source to its rows], or [:word a word]: `:serve-a-read-that-large` when
  one of them holds more places than one read may, by a bounded walk on its
  task (`softland.store.read-path.sources/over-the-limit?`). The sizes are
  asked at once, then the runs. A run that grew past the number between its
  size and its read ends in the same word.

  What must hold: the asker's standing in each source's layer is `:yes`.
  `read-once` calls it for no other, so the size of a layer that she may
  not see is neither read past the first read nor told."
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

;; ------------------------------------------------- the pushes, and their sharing

(declare on-callback)

(defn- open-push
  "The body of the delay of an entry of `:pushes`: the one call that opens
  the push of a source. Takes the read path, the source and the entry's
  mark. Gives a future that ends, and never with a throw, in {:push the
  push} or in {:word a word}. It waits for nothing: a callback of Rama's
  can call it, under a read's turn.

  A source of one row, and a layer's own rows, are opened at once. A run is
  bounded first: its count on the task, by a walk of the first places; when
  that walk is full, or the run has no first read
  (`softland.store.read-path.sources/first-read-holds?`), its size, by a
  bounded walk; each a select that the next step is chained to. A run that holds more places than one read may
  ends in `:serve-a-read-that-large` and no push. The value that the push
  gives at its opening is counted too. A call that throws is caught where
  it is made and ends in `:reach-the-store`. The whole opening has one
  bound, `:wait-ms`: when it passes, the future ends in
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
  "Takes the read path. Gives its three maps as they are now: {:pushes
  :counts :reads}."
  [^ReadPath read-path]
  @(.-held read-path))

(defn- read-of
  "Takes the read path and a read's id. Gives what `:reads` holds for the
  read now, or nil for a read that left or ended."
  [read-path id]
  (get-in (held-now read-path) [:reads id]))

(defn- given
  "Takes an entry of `:pushes`. Gives what its opening ended in, {:push
  push} or {:word word}; or nil while the opening is not done, or was not
  begun."
  [entry]
  (let [opening (:opening entry)]
    (when (realized? opening)
      (let [^CompletableFuture future @opening]
        (when (.isDone future)
          (.getNow future nil))))))

(defn- counted-under
  "Takes a source. Gives what its push is counted under: the source's gate,
  its table and its routing id. Rama's client sends one routing id to one
  task, so every push of one such count is on one task of one table.

  What one count holds. In a streaming layer the routing id is the layer,
  so every push of every asker through one layer is one count. In a
  microbatch layer the routing id is the home of the read, so one count
  holds the pushes on that gate's table of the askers whose reads have one
  home: on the base, the pushes of the askers of one home, and not those of
  all who read the base.

  Why by routing id and not by task. Rama bounds the pushes on one table on
  one task, and its client gives no call that turns a routing id into a
  task (looked for in `com.rpl.rama`, `PState`, `ProxyState` and `Helpers`
  of Rama 1.6.0). So the read path counts by what it can know. Two routing
  ids that Rama puts on one task are counted apart, and their pushes
  together can pass Rama's bound while each count is under the read path's
  own: Rama then refuses the opening, and the read gets `:reach-the-store`,
  as before this count. That is so for two streaming layers on one task,
  and for two homes on one task, whose pushes on a microbatch layer are on
  one table of that task."
  [source]
  [(:gate source) (:table source) (:routing-id source)])

(defn- one-fewer
  "Takes the counts of `held` and a source whose entry leaves `:pushes`.
  Gives the counts with one fewer under the source's count; a count that
  comes to nothing leaves the map."
  [counts source]
  (let [under (counted-under source)
        n (dec (long (get counts under 1)))]
    (if (pos? n) (assoc counts under n) (dissoc counts under))))

(defn- join!
  "Makes a live read hold sources: one swap of `held` for all of them, so
  that its maps never disagree. Takes the read path, the read's id and
  the sources. For a read that `:reads` still holds: where `:pushes` holds
  no entry for a source, the swap puts one in, with a new delay and the read
  as its one reader, and counts it (`counted-under`); else it adds the read
  to the entry. The read's own set
  gets the source as the very object that is the entry's key, so a source
  is held once however many reads hold it. After the swap each entry's
  delay is forced: for
  an entry that another read opened, that is no call to Rama.

  The read path's own bound. When the entries that the swap would put in
  take a count past `:max-pushes-under-one-routing-id`, the swap changes
  nothing: the read joins none of the sources, also not those that are open
  already, and no push is opened. The test and the join are one swap, so
  two reads that join at once cannot pass the bound together. A source that
  another read holds open is no new push and is not counted again.

  Gives `:joined`; `:past-the-bound`; or nil when there was nothing to
  join, or the read was left in the meantime and joined nothing.

  Two reads that open the same source at once end with one entry that
  holds both, and both force the same delay; a delay runs its body once,
  so the push is opened once. A swap that is tried again makes a delay that
  nobody forces."
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
  "Takes an entry that a swap took out of `:pushes`. Closes its push: now
  when its opening is done, or when it is given later. It forces the
  entry's delay to do so. A delay that its joiner had not forced yet then
  opens a push that is closed as soon as it is given: that is the price of
  never leaving a push open that no entry holds."
  [entry]
  (try
    (let [^CompletableFuture opening (force (:opening entry))]
      (.thenAccept opening (reify Consumer
                             (accept [_ got]
                               (when-let [push (:push got)]
                                 (close-push! push))))))
    (catch Throwable _ nil)))

(defn- leave-sources!
  "Makes a live read let go of sources, or leave whole: one swap of `held`.
  Takes the read path, the read's id, the sources (or `:all` for every
  source that the read holds), and whether the read itself leaves `:reads`.
  The swap takes the read out of each entry; an entry with no reader left
  is taken out of the map, and its count goes down by one (`one-fewer`):
  at once, before Rama has closed the push, which is one reason why the
  read path's bound stands under Rama's. After the swap, the push of each entry that this
  swap took out is closed (`close-when-given!`). So a push is closed only
  after its entry left the map, and a read that joins meanwhile is never
  handed a push that is closing: it puts a new entry in and opens a new
  push. It takes no turn and waits for nothing. Gives true when `:reads`
  held the read before the swap."
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
  "Takes the read path and what `:reads` holds for a live read. Gives a map
  from each source that the read holds, and whose push is given, to the
  value that the push holds now: `known`, as
  `softland.store.read-path.sources` takes it. A source whose push is not
  given yet, or whose opening ended in a word, is not in it. Gives nil when
  a push throws as it is read, which stops the step: Rama's client throws
  for a push that another thread's `leave` closed in that moment."
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
  it. `read-once` and `read-live` look at it first, and turn a call from
  inside an asker's function away."
  (ThreadLocal.))

(defn- telling?
  "Gives true on a thread on which the read path is calling an asker's
  function."
  []
  (some? (.get telling)))

(defn- call-marked
  "Calls an asker's function with one map, with the thread marked for the
  length of the call. Gives `:returned`, or `:threw` when the function
  threw; the thrown thing is dropped."
  [f change]
  (let [before (.get telling)]
    (.set telling Boolean/TRUE)
    (try (f change)
         :returned
         (catch Throwable _ :threw)
         (finally (if before (.set telling before) (.remove telling))))))

(defn- end-read!
  "Ends a live read that the read path can no longer keep right. Takes the
  read path, the read's id, why (`softland.store.read-path.words/because`)
  and what the word carries, or nil. In the read's turn: the read leaves
  `:reads` and each push that it holds; then, when the read was opened, its
  function is called once with {:ended the word `:keep-the-read-live`}. A
  throw of the function there is dropped. A read that left or ended before
  gets nothing. The read path opens nothing again by itself: the caller
  asks again.

  A read that is not opened yet is taken out and its function is not
  called: the first thing that a function is given is the whole answer, and
  such a read has none. `read-live`, which is still opening it, then finds
  it gone and gives its caller a word. Whether a read is opened changes
  only in its turn, which is why the turn is taken before the read is taken
  out: an opening and an ending of one read never cross."
  [read-path id because carries]
  (when-let [{:keys [turn]} (read-of read-path id)]
    (locking turn
      (let [{:keys [tell opened?]} (read-of read-path id)]
        (when (and (leave-sources! read-path id :all true) opened?)
          (call-marked tell
                       {:ended (words/word :keep-the-read-live (assoc carries :because because))}))))))

(defn- tell!
  "Calls the function of a live read with one change. Takes the read path,
  the read's id and the change. What must hold: the thread holds the read's
  turn. It looks, just before it calls, whether `:reads` still holds the
  read; a read that was left is given nothing, so no call is begun after
  `leave` returned. A throw of the function ends the read with
  `:its-function-threw`."
  [read-path id change]
  (when-let [read (read-of read-path id)]
    (when (= :threw (call-marked (:tell read) change))
      (end-read! read-path id :its-function-threw nil))))

(defn- settle!
  "The first line of a step of an opened read: asks what the read wants on
  the values that its pushes hold now, joins what is wanted and not held,
  leaves what is held and no longer wanted, and makes `known` again; and
  the same once more while that changes something, since a source that the
  step joined can be open already, for another read. Takes the read path
  and the read's id. What must hold: the thread holds the read's turn.
  Gives {:reading :known :joined :left}: what `reading` gave on the last
  `known`, that `known`, and every source that the step joined and left.
  `known` holds no source that the step left, and a source that the step
  joined only when its push was open already. Gives nil when the step
  stops: the read is gone, a push threw as it was read, or the read came to
  need more checks than one read may, or a push past the read path's own
  bound (`join!`); each of the last two ends it."
  [^ReadPath read-path id]
  (loop [round 0 joined #{} left #{}]
    (when-let [read (read-of read-path id)]
      (when-let [known (known-of read-path read)]
        (let [reading (sources/reading (:query read) known :live (.-numbers read-path) (:sources read))
              {:keys [join leave]} (sources/moves reading (:sources read))]
          (cond
            (:too-large reading)
            (do (end-read! read-path id :a-run-grew-too-large (:too-large reading)) nil)

            (or (and (empty? join) (empty? leave)) (<= max-rounds round))
            {:reading reading :known known :joined joined :left left}

            :else
            (do (leave-sources! read-path id leave false)
                (if (= :past-the-bound (join! read-path id join))
                  (do (end-read! read-path id :no-room-for-a-push-it-came-to-need (no-room read-path)) nil)
                  (recur (inc round) (into joined join) (into left leave))))))))))

(declare on-arrival)

(defn- when-given!
  "Takes the read path, the id of an opened read, and sources that a step of
  that read joined. For each that the read still holds, has `on-arrival`
  run when the opening of its push is done: at once, on this thread, for a
  push that was open already; else on the thread that gives it, a thread of
  Rama's client or the JVM's timer. No thread waits for such a push."
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
  "Does what a pure function said a step does. Takes the read path, the
  read's id, {:tell :owed?} as `softland.store.read-path.answer` gives it,
  and the sources that the step joined. It writes the read's mark, calls
  the function when there is a thing to tell, and has the arrival of each
  joined source handled. What must hold: the thread holds the read's
  turn."
  [^ReadPath read-path id {:keys [tell owed?]} joined]
  (swap! (.-held read-path)
         (fn [held] (if (contains? (:reads held) id) (assoc-in held [:reads id :owed?] owed?) held)))
  (when tell
    (tell! read-path id tell))
  (when-given! read-path id joined))

(defn- on-arrival
  "A push that an opened read came to need is given: the sixth step of a
  live read. Takes the read path, the read's id, the source, and what its
  opening ended in. In the read's turn. A read that no longer holds the
  source, and a read that is not opened yet, do nothing. An opening that
  ended in a word ends the read: the read cannot be kept right. Else
  `settle!`, and what `softland.store.read-path.answer/after-arrival`
  says."
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
  "A push of an opened read changed: the fifth step of a live read, for one
  read. Takes the read path, the read's id, the source, the keys that
  changed, and the push's value before and after. In the read's turn. A read that no longer holds the source,
  and a read that is not opened yet, do nothing: `read-live` reads the
  pushes' values itself, in the same turn, so a change that falls in the
  opening is in the first answer or is told after it. Else `settle!`, and
  what `softland.store.read-path.answer/after-change` says."
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
  "Takes a diff of Rama's. Gives the keys that it names, as a vector of
  `[:put key]` and `[:out key]`, by a processor of the two interfaces of a
  key's change and a key's removal; Rama expands a diff of several changes
  into those. Gives `:all` for a diff that names no key that way: a whole
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
  "Takes a diff of Rama's. Gives what kind it is: `:own-close`, the last
  call of a push that the read path closed; `:ended-by-the-store`, Rama
  ending the push after too many errors; `:anew`, a whole value sent anew,
  which is what the first callback of a push is, and what Rama sends a
  reader that it left out of a change; or `:change`."
  [diff]
  (cond
    (instance? DestroyedDiff diff)             :own-close
    (instance? UngracefulTerminationDiff diff) :ended-by-the-store
    (instance? ResyncDiff diff)                :anew
    :else                                      :change))

(defn- on-callback
  "One callback of a push of Rama's. Takes the read path, the push's source,
  the mark of the entry that opened the push, and what Rama hands a
  callback: the new value, the diff and the old value. It never throws into
  Rama's thread. In order:

  1. A callback of a push that no entry holds any more, by the mark, does
     nothing. So does the last call of a push that the read path closed.
     Rama ending a push ends each read on it, with
     `:the-store-ended-a-push`. The first callback of a push, a value sent
     anew, is the value that the opening already gave: the mark is set,
     and nothing more.
  2. Which keys the change names (`keys-of-diff`), and which of them
     changed (`softland.store.read-path.sources/named`): a key whose row is
     the same before and after is dropped. A whole value that Rama sent
     anew names no key, so every key of the value before and of the value
     after is held to that test: it is handled as a change of the keys
     whose rows differ, and gives nothing when no row differs. When no key
     is left, the callback ends here.
  3. A run whose new value holds more places than one read may ends each
     read on it, with `:a-run-grew-too-large`.
  4. For each read that holds the push, `on-change`, in that read's turn."
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
  "Opens the read path. Takes a cluster handle of Rama's and the name that
  Rama gives the module `Store`; and, when the opener wants, a map of the
  read path's numbers (`softland.store.read-path.query/default-numbers`
  says what each is, with its default and its reason). Gives a read path,
  or the word `:find-the-module` when the cluster gave one of the module's
  four tables of facts not under that name; no read path is then opened.

  Four fetches of a client, once for a process and on no read's way: the
  facts and the facts by value of each gate. The read path borrows the
  handles and uses them from every caller's thread; the opener owns the
  cluster handle, and nothing closes the read path. A read path starts
  with no live read and no push.

  A process opens one read path: two read paths share no push, so those who
  look at the same thing would not share one, and every push would be open
  twice. The read path and the door are opened apart; neither knows the
  other. Opening checks nothing of who opens it."
  ([cluster module-name] (open cluster module-name nil))
  ([cluster module-name numbers]
   (try
     (->ReadPath module-name
                 (rama/foreign-pstate cluster module-name "$$streaming-facts")
                 (rama/foreign-pstate cluster module-name "$$microbatch-facts")
                 (rama/foreign-pstate cluster module-name "$$streaming-facts-by-value")
                 (rama/foreign-pstate cluster module-name "$$microbatch-facts-by-value")
                 (query/numbers numbers)
                 (atom {:pushes {} :counts {} :reads {}})
                 (AtomicLong. 0))
     (catch Throwable t
       (words/word :find-the-module (assoc (what-threw t) :module module-name))))))

(defn- checked
  "The head of `read-once` and `read-live`. Takes what a caller gave as a
  query. Gives [:ok the query in its one form], or [:word a word]:
  `:read-inside-an-askers-function` on a thread on which the read path is
  calling an asker's function; `:read-the-query`, with the check's word and
  the part, for a thing that is no query. It makes no call to Rama."
  [thing]
  (if (telling?)
    [:word (words/word :read-inside-an-askers-function nil)]
    (let [{query :ok why :refuse at :at} (query/check thing)]
      (if why
        [:word (words/word :read-the-query {:why why :at at})]
        [:ok query]))))

(defn- read-once*
  "The body of `read-once`, for a query that passed its head. Takes the read
  path and the query in its one form. Gives an answer or a word. It goes round: what is wanted
  (`softland.store.read-path.sources/reading`); the runs whose first read
  was not whole, in the layers that she may see, are read whole
  (`run-now`); when nothing is missing, the answer; else each missing
  source is read by a select, and round again. It reads `held` nowhere: a
  source that a live read holds open is selected all the same, so a read
  asked once gives what the tables held when Rama served its calls, and
  never a value that a push holds. A run that has no first read
  (`softland.store.read-path.sources/first-read-holds?`) is wanted only in
  a layer that she may see, and is read by its size and then whole."
  [^ReadPath read-path query]
  (let [numbers (.-numbers read-path)
        deadline (+ (now-ms) (:wait-ms numbers))]
    (loop [round 0 known {} first-reads #{}]
      (let [reading (sources/reading query known :once numbers)
            to-read-whole (filterv #(= :yes (get (:standing reading) (:layer %))) first-reads)]
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

          (<= max-rounds round)
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
  "Reads once. Takes the read path and a query
  (`softland.store.read-path.query`). Gives an answer
  (`softland.store.read-path.answer`), or one of the read path's words:
  `:read-the-query`, `:read-inside-an-askers-function`, `:reach-the-store`,
  `:get-an-answer-within-its-wait` or `:serve-a-read-that-large`. It never
  throws, and it holds nothing when it returns.

  What it costs. Each round is the calls that are missing, one for each
  table under each routing id, sent at once: a place through three layers
  is three calls in one round; from a layer, one call and then the others;
  what points at a thing, one round for the rights and the marks, one for
  the orders, and one for the checks of nearer layers, each check one seek.
  A run of more than `:run-start-places` places costs two calls more. A
  microbatch layer is read on the task of the read's home, in a call of its
  own table: a run across its entities is one call, as in a streaming
  layer. One bound, `:wait-ms`, holds for the whole call. No count of pushes bounds it: it
  opens none.

  It reads the tables, and takes no value of a push. Where a live read
  holds a push open on a source that this read wants, the read makes its
  own call all the same, and pays that round trip. A push holds nothing
  that the tables do not hold, and it can be behind them by as long as the
  push is late; a read asked once that took its value could give a caller
  the state before her own act, straight after that act was acked. So what
  `read-once` gives is what the tables held when Rama served its calls: an
  act whose ack a caller has, in a streaming layer, is in it. An act of a
  microbatch layer is in it once the task that serves the read holds its
  rows, which is a task of the read's home and can be another than the one
  that decided the act.

  What it reads before it knows the asker's right, and what it then drops,
  is in the namespace docstring. The layers are read at moments that
  differ; nothing lines them up."
  [^ReadPath read-path query]
  (try
    (let [[outcome query] (checked query)]
      (if (= :word outcome)
        query
        (read-once* read-path query)))
    (catch Throwable t
      (words/word :reach-the-store (what-threw t)))))

(defn- wait-for-held!
  "Waits until the push of each source that a live read holds is given, or
  the call's one wait ends. Takes the read path, the read's id and the
  moment at which the wait ends. It holds no turn while it waits: a
  callback that waited for the read's turn would hold a thread of Rama's
  client, and the opening of a push can need that thread. Gives [:ok], or
  [:word the word] of the first opening that ended in one, or of a wait
  that passed."
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
  "The body of `read-live`, for a query that passed its head. Takes the read
  path, the query in its one form and the asker's function. Gives {:now
  :leave-with} or a word.

  It makes the read's entry, not opened, and goes round: what is wanted,
  join what is missing and leave what is not wanted, wait for the pushes,
  again. A round whose join would take the read path past its own bound on
  pushes (`join!`) opens none of that round's pushes, and the read is
  turned away at once with `:keep-more-reads-live`. When nothing is missing it takes the read's turn, reads the
  pushes' values and asks once more. If a change that came meanwhile made
  a source wanted, it lets the turn go and goes round again. Else, in that
  turn, it makes the answer, marks the read opened, and calls the function
  with {:anew answer}: so the first thing that the function is given is
  the whole answer, and no change is told before it. A callback that comes
  while the read opens takes the turn, finds the read not opened and does
  nothing; a push holds its new value before its callback runs, so that
  change is in the answer, or it finds the read opened and is told after
  it. A read that ends in a word leaves every push that it joined.

  A read that is ended while it opens, because Rama ended a push that it
  had joined or a run of it grew too large, is gone from `:reads` before it
  was opened: its function was not called, and this call gives the word of
  a read that got no answer."
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
            (or (<= max-rounds round) (< deadline (now-ms)))
            (give-up (late read-path))

            (nil? reading)
            (give-up (late read-path))

            (:too-large reading)
            (give-up (words/word :serve-a-read-that-large (:too-large reading)))

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
                                                         (assoc-in now [:reads id :opened?] true)
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
  "Reads, and keeps the read live. Takes the read path, a query
  (`softland.store.read-path.query`) and a function of one argument. Gives
  {:now the answer, :leave-with a handle}, or one of the read path's
  words: those of `read-once`, and `:keep-more-reads-live`. It never throws.

  The function is called with one map at a time, as the namespace docstring
  says: first, on the caller's own thread and before `read-live` returns,
  with {:anew answer}, the same answer that `:now` holds; then, on a thread
  of Rama's client, with each change, {:changed {place place-answer}} or
  {:anew answer}; and at most once with {:ended word}. A caller who keeps a
  screen uses the function's calls alone: a screen that is made only from
  what the function was given is right, in order. `:now` is there for a
  caller that keeps none.

  What ends a read: Rama ended a push; a run grew past
  `:max-places-of-one-run`, or the checks of nearer layers past
  `:max-checks-of-one-live-read`; a push that the open read came to need
  was not given, or would have taken the read path past its own bound on
  pushes; or the function threw. The read path opens nothing again by itself; the caller asks
  again. A function that throws at its first call ends the read then:
  `read-live` still gives {:now :leave-with}, and the handle is not open.

  What the function must not do, and what a slow function holds back, is
  in the namespace docstring. It must return at once. It may call `leave`.
  A call of `read-once` or `read-live` from inside it is turned away. A
  thing that is no function is as a function that throws at its first
  call.

  What it follows. Every fact that shapes the read, each by a push that
  those who need it share: the layer's own rows (whether it is made, who
  may see it, what it builds on, its marked keys) as one push for a layer;
  the asker's permission, only while the layer is made and its setting is
  `:members`; and the facts. When what shapes the read changes, the asker
  is given the whole answer anew. When a layer is no longer rightful for
  her, her read leaves that layer's facts at once; the answer then says
  that the layer is left out, and nothing of the layer's facts. A fact of
  that layer that Rama called back before the setting's change reached the
  read path can still be told: as late as a push is, and no sooner.

  What it costs. One push for each source: for a place through k layers of
  which g are \"members\", k + g + k. For what points at a thing, one push
  more for each place of a farther order and each nearer layer. Each open
  push costs its layer's gate at each write. Rama opens a bounded count of
  pushes on one table on one task, and the read path holds a bound of its
  own under that: a read whose pushes would pass it is turned away at once,
  with `:keep-more-reads-live`, and has opened none of them (the namespace
  docstring, \"How many pushes\"). Nothing bounds how long a caller stays
  but the end of the process: the read and the pushes of a caller that
  never leaves stay, count toward that bound, and `open-count` shows them."
  [^ReadPath read-path query f]
  (try
    (let [[outcome query] (checked query)]
      (if (= :word outcome)
        query
        (read-live* read-path query f)))
    (catch Throwable t
      (words/word :reach-the-store (what-threw t)))))

(defn leave
  "Says that an asker left. Takes the read path and the handle that
  `read-live` gave. The read leaves each push that it holds, and each push
  that no read holds any more is closed. Gives `:left`; or `:not-open` for
  a read that was left or ended before, and for a thing that is no handle
  of this read path. It takes no turn and waits for nothing, so an asker's
  function may call it, for its own read or for another. A call of the
  function that another thread had begun to make can still arrive in the
  moment in which `leave` returns; none is begun after that. It never
  throws."
  [^ReadPath read-path handle]
  (try
    (if (and (instance? LiveRead handle)
             (identical? (.-held read-path) (.-held ^LiveRead handle))
             (leave-sources! read-path (.-id ^LiveRead handle) :all true))
      :left
      :not-open)
    (catch Throwable _ :not-open)))

(defn open-count
  "Takes the read path. Gives {:live-reads how many live reads are open,
  :pushes how many pushes they hold between them}. A caller that never
  leaves shows here. It reads the read path's own memory and makes no call
  to Rama."
  [^ReadPath read-path]
  (let [held (held-now read-path)]
    {:live-reads (count (:reads held))
     :pushes     (count (:pushes held))}))
