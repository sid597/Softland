;; Before changing this file, read the papers of this part again:
;; src/proposal/backend-on-rama-2026-10-02/run-again/PLAN-the-bases-gate.md
;; src/proposal/backend-on-rama-2026-10-02/run-again/PART-A.md
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md

(ns softland.store.module.walk
  "The walk of one thing: the decision of one thing that arrived on a gate's
  depot, as one Rama operation, `decide`, for both gates. It requires Rama.

  What it takes. The four tables of the calling gate's record; the gate's
  depot word, `:streaming` or `:microbatch`; and one thing as it arrived on
  that gate's depot. Anything can arrive: the door is not the only sender.
  The tables are PStates that the calling topology owns, with the schemas
  of `softland.store.record.tables`: `answers`, `facts`, `facts-by-value` and
  `task`. The walk borrows them for the one call. It declares nothing and
  keeps nothing in the memory of a process.

  What it changes. Rows of those four tables on the task where it is
  called, and nothing else. Each row is `softland.store.record.rows`'s. The
  two gates differ in the names of their tables and in their word; the walk
  is handed both, so one decision is written once.

  What it gives. For an accept, the writes that a gate may copy to other
  tasks, as a vector in the order in which
  `softland.store.gate.decision/writes` gave them and the walk wrote them:
  each [:fact key row], and each [:by-value key row], where a row that is
  nil says that the key is taken out. The answer and the layer's last stamp
  are never in it: they stay on the task of the decision. For a refusal,
  for a thing under an act-id that has an answer, and for a stray, it gives
  an empty vector. The walk copies nothing itself, and a gate may ignore
  what it gives.

  What a decision is made of. The thing, the gate's word, and rows of the
  four tables on the task of the call. Nothing else: the walk reads no
  clock, it reads no table of the other gate, it takes no random number, and
  it knows nothing of a batch. So the same thing against the same rows
  gives the same rows, with the same stamp, whenever it is decided.

  Approach. The dataflow reads rows, calls pure functions and writes rows.
  The test for a stray and the check of what arrived are
  `softland.store.door.declaration`'s, the checks against the record and
  the stamp are `softland.store.gate.decision`'s, and each row comes from
  `softland.store.record.rows`. None of them throws, so no thing can stop a
  partition: a refusal is data, an answer with a reason word.

  The walk, for one thing:

    0. a stray?   A thing with no usable act-id, or whose act-id is of the
                  other gate or of another layer, gets no answer. Its digest
                  is added to the set of its reason word in the task's
                  table, and the walk ends.
    1. check (a)  Read the answer under the act-id. If one is there, the
                  walk decides nothing, writes no row and ends, whether the
                  thing is the same act again or a different one. So a
                  repeat is not decided twice, and one act-id has one
                  answer: that of the first thing that the gate decided
                  under it.
    2. the digest of the thing as it arrived, which leaves the time on the
                  act out (`softland.store.door.digest`), and check (b),
                  the door's declaration. A thing with no usable time is
                  refused `:no-door-time` here. The declaration's last step
                  makes the one text of each value and holds the bound on
                  one value and the bound on the act, so a thing over
                  either is refused here, before any row of its standing is
                  read.
    3. the time   The time on the act, which the door writes, or whatever
                  else appended the thing
                  (`softland.store.gate.decision/time-on`); nil for a thing
                  with no usable time. The 10-second rule holds each
                  carried stamp against it.
    4. standing   Read the place of `:who`'s permission; only when it is
                  empty, the place of the layer's depot. When both are
                  empty and the layer starts with the other gate's depot,
                  the act is refused `:layer-belongs-to-the-other-gate`,
                  whether or not the other gate has made the layer: the
                  check asks the layer id
                  (`softland.store.gate.decision/standing`) and the walk
                  reads nothing for it. For the streaming gate such a
                  layer is the base; for the microbatch gate it is every
                  other layer. A permission that stands and says `:see` is
                  refused here, with no read more: an act needs \"act\".
    5. places     Read the place of each fact that names something to
                  replace and of each fact under
                  `:softland.store/permission`; then checks (e), (f), (g),
                  who may write a key that takes a second write, and what a
                  store key does not take.
    6. the marked keys   Only for an act that no check refused, and only
                  when it holds a fact under a plain key or under
                  `:softland.store/marked-keys`: read the row at the layer's
                  marked-keys place, unless step 5 read it. Then read the
                  place of each fact under a key that the row marks, unless
                  step 5 read it: the row before gives the entry to take out
                  of the facts by value.
    7. the stamp  Read the last stamp of the act's layer: the number under
                  the layer's text in `:last-stamp` of the task's table.
                  The stamp of an accept is made of the time on the act,
                  that last stamp, and the stamps that the act carries.
                  The stamp of a refusal is that last stamp plus one, or 0
                  with none, whatever the thing carries
                  (`softland.store.gate.decision/stamp-of`). A refusal
                  writes the answer and the layer's last stamp and nothing
                  else. An accept writes the row of each fact, with the
                  text of its value, who wrote it and the permission that
                  the act named; for a fact under a marked key the entry of
                  its value in the facts by value, and the entry of the
                  value before taken out; the answer and the layer's last
                  stamp. The last write is the walk's last step before it
                  gives its result.

  The stamp is counted in the layer, in both gates. Each layer has its own
  last stamp, so a decision in one layer does not raise the stamps of
  another layer of the task, and two layers of one task can hold the same
  stamp. In one layer each stamp is later than the one before, in the order
  in which the walk was called. A refused thing, one with no usable time
  among them, gets the layer's last stamp plus one, or 0 in a layer with
  no last stamp: only an accept moves a layer's time.

  The marked keys. An act is decided with the layer's marks as they were
  before it. So a fact of the act that marks its own key is not in the order
  by value; the key gets the stamp of that act, in both gates; and every
  fact with a later stamp under that key is in the order. The order of the
  calls is the order of a mark and of a fact after it.

  What one call costs, in reads and writes. A later stage's rows are read
  only when the earlier checks passed, and no check reads the marked keys.

  How a read is counted: as Rama's skill text counts a read on disk, one
  for each level of a path. A row of the answers or of the facts is one
  key, so one read. A layer's last stamp is two: the number sits one level
  down in its table, under a field and then the layer's key
  (`softland.store.record.tables` says why it is kept so). A write is one
  for each row, the last stamp too: each is a whole value at a fixed key,
  which reads nothing. Counted from the dataflow; no run here measured a
  read on disk.

    - A stray: no row is read, and one add to a set. The add is not free
      of a read: the set tracks its size, and Rama's docs say that size
      tracking adds a read on disk to each write.
    - A thing under an act-id that has an answer: 1 read, no write.
    - A thing that the declaration refuses, one with no usable time or with
      a value over the bound among them: 3 reads, the answer and the two of
      the layer's last stamp, and 2 writes, the answer and the layer's last
      stamp.
    - An act whose permission does not stand: 5 reads at most, whatever it
      carries, the answer, the two places of its standing and the two of
      the layer's last stamp, and the same 2 writes. An act into a layer of
      the other gate is one of these.
    - A making act: those 5 reads, and 4 writes: its two facts, the answer
      and the layer's last stamp.
    - A give, one fact under `:softland.store/permission`: 5 reads, the
      answer, the giver's permission, the place of the permission given and
      the two of the layer's last stamp, and 3 writes.
    - One accepted act with one value under a plain key: 5 reads and 3
      writes. The reads are the answer, the permission, the marked keys and
      the two of the layer's last stamp; the writes are the fact, the
      answer and the layer's last stamp. A fact that names something to
      replace costs the read of its place more. The read of the marked keys
      is paid by every accepted act that holds a fact under a plain key,
      also in a layer that marks no key. When the fact's key is marked it
      costs 6 reads, and with them the row before, and 4 writes, or 5 when
      the entry of a value before goes out.
    - An act with no fact under a plain key, a give, a setting or a list,
      reads no marked keys.

  The greatest call: an act holds 256 facts at most
  (`softland.store.door.declaration/max-facts`), and each place is read
  once, so a call makes 261 reads at most, the 256 places, the answer, the
  permission, the marked keys and the two of the last stamp, and 770
  writes at most, three for each fact and two more. Each of its three
  loops is over the act's own rows and ends.

  It never gives its task back. Between the first read and the last write
  stands no partitioner, no yield, no read that may yield and no append:
  every read and every write is on the task of the call, and the walk runs
  to its result in one stretch. This is a contract of the operation, and a
  gate rests on it. A gate decides the things of a layer in the order of
  the layer's depot partition, each against the rows that the things before
  it wrote. Rama runs other events of a task while one event waits at a
  partitioner or a yield. A walk that waited there would let the next thing
  of the partition be decided first, against a record that lacks this
  thing's rows: an act that replaces the fact of the act before it would be
  refused, and the stamps of a layer would leave the depot's order. The
  plan of this part (PLAN-the-bases-gate.md, claim 4) holds a run of a
  stand-in gate whose walk did yield: things were decided out of the
  depot's order and refused. So no change to this file may add one of the
  four. What the contract costs: the walk holds the task's thread for the
  whole of one act, and a read of the task waits that long.

  `decide` is a `deframafn`: Rama's form of an operation that emits exactly
  once. The writes of a decision are therefore a loop, where a gate's own
  dataflow could emit once for each write.

  When a thing is run again. Each write of a decision is behind check (a),
  and the answer is written with the rest. What follows depends on the
  topology that calls.

    - A stream topology can process a record again, also after its writes
      were committed. A second run after a commit finds the answer at check
      (a), writes no row and gives an empty vector. So one act-id has one
      answer, a layer's last stamp is not raised twice for one thing, and no
      entry of the facts by value that a later act put back is taken out
      again. A second run after a discard is the first that counts.
    - A microbatch topology sets its tables back when an attempt of a batch
      fails, on every task, and processes the batch's records again. The
      second attempt reads the rows that the first read, so it writes the
      same rows with the same stamps and gives the same copies.

  The stray's write is not behind check (a); it is an add to a set, the
  same write however often it runs.

  A limit for a gate that copies. A thing whose act-id has an answer gives
  no copies. So the copies of a decision and its answer must be committed
  together or not at all: were the answer committed and a copy lost, the
  next run would find the answer and copy nothing. A microbatch topology
  commits a whole batch on every task or on none. A stream topology
  commits each task's writes apart, so a streaming gate that copied to
  other tasks would not hold this.

  What it rests on and does not test.

    - The caller calls it on the task that holds the layer's rows, which is
      the task of the thing's depot partition, with tables that the calling
      topology owns. On another task it decides against that task's record.
    - The caller calls it for the things of a partition in the depot's
      order, and starts the next call when this one has given its result.
    - The gate's word is the depot word of the calling gate. With the other
      word every thing of the depot is a stray.
    - No function that it calls throws on what arrives. A throw in the walk
      fails the event or the attempt of the batch that called it."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.gate.decision :as decision]
            [softland.store.record.rows :as record]))

(defn copies-of
  "Takes what `softland.store.gate.decision/writes` gave for one decision.
  Gives, in that order, the writes that a gate may copy to other tasks: each
  [:fact key row] and each [:by-value key row], one whose row is nil among
  them. The answer and the last stamp are left out. An empty vector for a
  refusal, which writes no fact. Pure code."
  [writes]
  (into [] (filter (fn [[which]] (or (= :fact which) (= :by-value which)))) writes))

(deframafn decide
  "Decides one thing, on the task where it is called, and writes what the
  decision writes. Takes the calling gate's four tables, as PStates with the
  schemas `answers`, `facts`, `facts-by-value` and `task` of
  `softland.store.record.tables`; the gate's depot word, `:streaming` or
  `:microbatch`; and a thing as it arrived on that gate's depot, which can
  be anything. Emits exactly once: for an accept the writes that a gate may
  copy to other tasks (`copies-of`), in the order in which they were
  written; an empty vector for a refusal, for a thing under an act-id that
  has an answer, and for a stray. The steps, what each kind of thing costs,
  and what a second run gives are in the namespace docstring.

  Each write is a whole row at a fixed key, or a fixed key taken out: the
  same write however often it runs. Every read and every write is a local
  one, on the task of the call.

  What must hold: the caller's topology owns the four tables; the call is
  on the task that holds the rows of the thing's layer; and the things of
  one partition are handed to it in the depot's order, one call ended
  before the next begins. Limits: it holds the task's thread from its
  first read to its last write, for 261 reads and 770 writes at most. It
  holds no partitioner, no yield, no read that may yield and no append, and
  none may be added: the namespace docstring says why."
  [$$answers $$facts $$facts-by-value $$task *gate *thing]

  ;; 0. A stray gets no answer: its digest goes to the set of its word,
  ;; and the walk ends.
  (declaration/stray-reason *gate *thing :> *stray)
  (<<if *stray
    (digest/hex-of *thing :> *stray-digest)
    (local-transform> [(keypath :strays *stray) NONE-ELEM (termval *stray-digest)] $$task)
    (identity [] :> *copies)

   (else>)
    ;; 1. Check (a): an act-id that has an answer is not decided again.
    ;; The walk then writes nothing and ends.
    (get *thing :act-id :> *act-id)
    (nth *act-id 0 :> *layer)
    (nth *act-id 2 :> *act-uuid)
    (record/answer-key *layer *act-uuid :> *answer-key)
    (local-select> (keypath *answer-key) $$answers :> *answered)
    (<<if (some? *answered)
      (identity [] :> *copies)

     (else>)
      ;; 2 and 3. The digest, check (b), the time on the act, the
      ;; 10-second rule. No clock is read: the time is the thing's own.
      (digest/digest *thing :> *digest)
      (declaration/check *thing :> {*act :ok *declarations-reason :refuse})
      (decision/time-on *thing :> *act-time)
      (decision/refused-early *act-time *act *declarations-reason :> *refused-early)

      ;; 4. Standing: checks (c) and (d), and the first act of a layer.
      ;; Whether the layer is the other gate's is asked of the layer id, in
      ;; `standing` itself, and no table of the other gate is read.
      (<<if *refused-early
        (identity *refused-early :> *refused-standing)
        (identity false :> *makes-the-layer)
       (else>)
        (decision/permission-place *act :> *permission-key)
        (local-select> (keypath *permission-key) $$facts :> *permission-row)
        (<<if (nil? *permission-row)
          (decision/depot-place *act :> *depot-key)
          (local-select> (keypath *depot-key) $$facts :> *depot-row)
         (else>)
          (identity nil :> *depot-row))
        (decision/standing *gate *act *permission-row *depot-row :> *refused-standing)
        (decision/makes-the-layer? *gate *act *permission-row *depot-row :> *makes-the-layer))

      ;; 5. Places: checks (e), (f), (g), who may write a key that takes a
      ;; second write, and what a store key does not take. The last
      ;; check: what it gives is the act's refusal, or nil.
      (<<if *refused-standing
        (identity *refused-standing :> *refused)
        (identity {} :> *place-rows)
       (else>)
        (decision/places-needed *act *makes-the-layer :> *place-keys)
        (loop<- [*keys-left *place-keys
                 *rows-read {}
                 :> *place-rows]
          (<<if (empty? *keys-left)
            (:> *rows-read)
           (else>)
            (first *keys-left :> *place-key)
            (local-select> (keypath *place-key) $$facts :> *place-row)
            (continue> (rest *keys-left) (assoc *rows-read *place-key *place-row))))
        (decision/places *gate *act *makes-the-layer *place-rows :> *refused))

      ;; 6. Only for an act that no check refused: the layer's marked
      ;; keys, and the row before of each place under a marked key. Each
      ;; row is read once: step 5 may have read it.
      (<<if (and> (nil? *refused) (decision/reads-marked-keys? *act))
        (decision/marked-keys-place *act :> *marks-key)
        (<<if (contains? *place-rows *marks-key)
          (get *place-rows *marks-key :> *marks-row)
         (else>)
          (local-select> (keypath *marks-key) $$facts :> *marks-row))
        (decision/since-by-place *act *marks-row :> *since-by-place)
        (decision/rows-before-needed *since-by-place *place-rows :> *before-keys)
        (loop<- [*before-left *before-keys
                 *before-read {}
                 :> *before-rows]
          (<<if (empty? *before-left)
            (:> *before-read)
           (else>)
            (first *before-left :> *before-key)
            (local-select> (keypath *before-key) $$facts :> *before-row)
            (continue> (rest *before-left) (assoc *before-read *before-key *before-row))))
        (assoc (merge *place-rows *before-rows) *marks-key *marks-row :> *rows-in-hand)
       (else>)
        (identity {} :> *since-by-place)
        (identity *place-rows :> *rows-in-hand))

      ;; 7. The stamp, of the time on the act and of the layer's last
      ;; stamp, which is kept under the layer's text.
      (declaration/id-text *layer :> *layer-text)
      (local-select> (keypath :last-stamp *layer-text) $$task :> *last-stamp)
      (decision/stamp-of *act-time *last-stamp *act *refused :> *stamp)
      ;; What the walk has in hand for an accept; nothing for a refusal.
      ;; A key that this act newly marks gets the act's own stamp.
      (<<if *refused
        (identity {} :> *in-hand)
       (else>)
        (hash-map :since-by-place *since-by-place
                  :place-rows *rows-in-hand
                  :level-from *stamp
                  :> *in-hand))
      (decision/writes *act-id *act *refused *stamp *digest *in-hand :> *writes)
      ;; The writes: each a whole row at a fixed key, or a fixed key taken
      ;; out. One row at a time, in the order given, with no yield between
      ;; two. The loop ends when each is written, and then the walk gives
      ;; its result: nothing stands behind the last write.
      (loop<- [*writes-left *writes
               :> *written]
        (<<if (empty? *writes-left)
          (:> true)
         (else>)
          (first *writes-left :> [*which *key *row])
          (<<cond
            (case> (= *which :fact))
            (local-transform> [(keypath *key) (termval *row)] $$facts)

            (case> (and> (= *which :by-value) (nil? *row)))
            (local-transform> [(keypath *key) NONE>] $$facts-by-value)

            (case> (= *which :by-value))
            (local-transform> [(keypath *key) (termval *row)] $$facts-by-value)

            (case> (= *which :answer))
            (local-transform> [(keypath *key) (termval *row)] $$answers)

            (case> (= *which :last-stamp))
            (local-transform> [(keypath :last-stamp *layer-text) (termval *row)] $$task)

            (default>)
            (identity nil :> *no-row))
          (continue> (rest *writes-left))))
      (copies-of *writes :> *copies)))
  (:> *copies))
