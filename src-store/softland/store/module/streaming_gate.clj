;; Before changing this file, read the plans of this part again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-base-for-a-reader.md

(ns softland.store.module.streaming-gate
  "The streaming gate: the stream topology that decides each thing of the
  streaming depot, and the five PStates of its record. It requires Rama.

  What it takes. Each thing that arrives on `*streaming-depot`, which the
  module declares (`softland.store.module`) and which partitions by the
  thing's `:layer`. Anything can arrive: the door is not the only sender.

  What it changes. It owns five PStates, the record of the streaming layers,
  and it alone writes them (in Rama only the owning topology writes a
  PState): `$$streaming-answers`, `$$streaming-facts`,
  `$$streaming-facts-by-value`, `$$streaming-locks` and `$$streaming-task`.
  Their schemas are `softland.store.record.tables`'s and their rows
  `softland.store.record.rows`'s. They are durable state of the cluster. The
  gate keeps nothing in the memory of a process: the row of a layer's marked
  keys is read from the PState in each event that needs it.

  What it borrows. Two PStates of the microbatch gate, to read on its own
  task: `$$microbatch-facts` and `$$microbatch-task`. It reads them only for
  an act into a layer of which its own record holds nothing and that starts
  with the other gate's depot, which is the base.

  Approach. The dataflow only reads rows, calls pure functions and writes
  rows. The test for a stray and the check of what arrived are
  `softland.store.door.declaration`'s, the checks against the record are
  `softland.store.gate.decision`'s, and each row comes from
  `softland.store.record.rows`. None of them throws, so no thing can stop a
  partition: a refusal is data, an answer with a reason word.

  The walk of one event, for one thing:

    0. a stray?   A thing with no usable act-id, or whose act-id is of the
                  other gate or of another layer, gets no answer. Its digest
                  is added to the set of its reason word, and the event
                  ends.
    1. check (a)  Read the answer under the act-id. If one is there, the
                  event ends and writes nothing, whether the thing is the
                  same act again or a different one. So a repeat is
                  answered, not decided twice, and one act-id has one
                  answer.
    2. the digest of the thing as it arrived, and check (b), the door's
                  declaration.
    3. the clock, read once; the 10-second rule on a carried stamp.
    4. standing   Read the place of `:who`'s permission; only when it is
                  empty, the place of the layer's depot; only when that is
                  empty too, and the layer starts with the other gate's
                  depot, whether the other gate's record holds the layer:
                  the layer's depot place in `$$microbatch-facts`, which
                  every task holds, and this task's `:closed-through`. A
                  permission that stands and says `:see` is refused here,
                  with no read more: an act needs \"act\".
    5. places     Read the place of each fact that names something to
                  replace and of each fact under
                  `:softland.store/permission`; then checks (e), (f), (g),
                  who may write a key that takes a second write, and what a
                  store key does not take.
    6. the seal   Read one range of locks for each request whose locks the
                  act names; then each sealed value is opened once, and the
                  plain bytes are kept for its row.
    6b. the marked keys   Only for an act that no check refused, and only
                  when it holds a fact under a plain key or under
                  `:softland.store/marked-keys`: read the row at the layer's
                  marked-keys place, unless step 5 read it. Then read the
                  place of each fact under a key that the row marks, unless
                  step 5 read it: the row before gives the entry to take out
                  of the facts by value.
    7. the stamp  Read the task's last stamp. A refusal writes the answer
                  and the last stamp and nothing else. An accept writes the
                  row of each fact, with its plain value, who wrote it and
                  the permission that the act named; for a fact under a
                  marked key the entry of its value in the facts by value,
                  and the entry of the value before taken out; the row of
                  each lock that it uses; a new lock at each key that a
                  request asks for; the answer and the last stamp.

  A later stage's rows are read only when the earlier checks passed, and no
  check reads the marked keys. So an act whose permission does not stand
  costs 4 reads at most, whatever it carries, and an act into the base on
  this depot 6 at most. One accepted act with one sealed value costs 5 reads
  and 4 row writes: the answer, the permission, the lock, the marked keys,
  the last stamp. The read of the marked keys is paid by every accepted act
  that holds a fact under a plain key, also in a layer that marks no key.
  When the fact's key is marked it
  costs 6 reads, and with them the row before, and 5 row writes, or 6 when
  the entry of a value before goes out. An act with no fact under a plain
  key, a request for locks, a give, a setting or a list, reads no marked
  keys.

  The event has no partitioner and does not yield. It starts and ends on the
  task of the depot's partition, so all its writes are one group: they
  become visible together or not at all, and a reader sees an accepted
  answer, each fact of its act, the entry that came into the facts by value
  and the entry that went out at once. Rama's docs say this of a batch of
  streaming events on one task. No other event of the task runs between
  this event's reads and its writes.

  The marked keys. An act is decided with the layer's marks as they were
  before it. So a fact of the act that marks its own key is not in the order
  by value; the key gets the stamp of that act; and every fact with a later
  stamp under that key is in the order. The depot's order is the order of a
  mark and of a fact after it.

  When Rama runs an event again. A stream topology can process a record
  again, also after its writes were committed. Each write of a decision is
  behind check (a), and the answer commits with the rest: a second run after
  a commit ends at check (a) and writes nothing, so no lock is made again
  for a value that is sealed, no stamp is given twice, and no entry of the
  facts by value that a later act put back is taken out again. A second run after
  a discard is the first that counts; the lock bytes and the stamp of the
  discarded run were never visible and may differ. The stray's write is not
  behind check (a); it is an add to a set, the same write however often it
  runs. The source's retry mode is \"all after\": when a record fails, each
  record after it on the partition is processed again, in order.

  What this does not close. Without a failure, the acts of a layer are
  decided in the depot's order. When something throws while a batch runs,
  that batch is discarded, and a later record can be decided before the
  failed one is retried. One setting of Rama would close that window, at a
  cost in throughput; the store's map, one folder up, names it.

  What the gate rests on and does not test. The depot's partitioner puts a
  thing on its layer's task, and the gate makes no hop. A thing that a
  topology appends to another partition is decided against that task's
  record; the store's map, one folder up, says what follows.

  The source starts from the beginning of the depot, so no thing that is in
  the depot is skipped whenever the topology first runs on a partition.
  Nothing is given back in the ack: the ack is a signal, and the record is
  the truth, read by act-id."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :refer :all]
            [softland.store.gate.decision :as decision]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.gate.first-act :as first-act]
            [softland.store.record.rows :as record]
            [softland.store.record.tables :as record-pstates]
            [softland.store.door.seal :as seal]))

(defn other-gates-record-holds-layer?
  "Takes the row at a layer's depot place in the microbatch gate's record,
  as this task holds it, or nil; and this task's `:closed-through` of that
  gate, or nil. Gives whether the other gate's record holds the layer: true
  for a row that is there and is of a closed batch
  (`softland.store.record.rows/closed?`). A row of a batch that is not closed
  does not count. Rama can run that batch again, and the row would be the
  same on a second attempt: both take the batch's kept clock. The question
  stays held to closing so that this gate and the read `depot-of-layer` say
  the same of the base at one moment: if this gate counted the base as made
  one run earlier, a door could read a refusal that says the layer is made
  while its own read still says that it is not. The gate asks only when its own record holds nothing for the
  layer and the layer starts with the other gate's depot. An act into such
  a layer is refused :layer-belongs-to-the-other-gate once the layer is
  made, and :layer-not-made before."
  [other-depot-row closed-through]
  (record/closed? other-depot-row closed-through))

(defn new-lock-row
  "Gives the row of a new lock: fresh random bytes and the number of the way
  that new locks are made for. The bytes are made here, in the gate's event,
  at the write, and go to the locks PState only; they are never on a depot.
  Each call gives other bytes."
  []
  (record/lock-row (seal/new-lock)))

(defn rows-as-map
  "Takes the rows of a range of a PState, as the range navigator gives them.
  Gives them as a plain map from key to row, which can be merged and carried
  as a value."
  [rows]
  (into {} rows))

(defn add-to
  "Adds the streaming gate to a module. Takes the module's `topologies`.
  Declares the stream topology `streaming-gate` and its five PStates, and
  the dataflow of one event, as the namespace docstring walks it. Each write
  of an accept is a whole row at a fixed key, or a fixed key taken out: the
  same write however often it runs. What must hold: the module declares the
  depot `*streaming-depot`, and has the microbatch gate, which declares the
  two PStates that step 4 borrows."
  [topologies]
  (let [gate (stream-topology topologies "streaming-gate")]
    (declare-pstate gate $$streaming-answers (record-pstates/answers))
    (declare-pstate gate $$streaming-facts (record-pstates/facts))
    (declare-pstate gate $$streaming-facts-by-value (record-pstates/facts-by-value))
    (declare-pstate gate $$streaming-locks (record-pstates/locks))
    (declare-pstate gate $$streaming-task (record-pstates/task))
    (<<sources gate
      (source> *streaming-depot {:start-from :beginning :retry-mode :all-after} :> *thing)

      ;; 0. A stray gets no answer: its digest goes to the set of its word.
      (declaration/stray-reason :streaming *thing :> *stray)
      (<<if *stray
        (digest/hex-of *thing :> *stray-digest)
        (local-transform> [(keypath :strays *stray) NONE-ELEM (termval *stray-digest)]
                          $$streaming-task)

       (else>)
        ;; 1. Check (a): an act-id that has an answer ends the event.
        (get *thing :act-id :> *act-id)
        (nth *act-id 0 :> *layer)
        (nth *act-id 2 :> *act-uuid)
        (record/answer-key *layer *act-uuid :> *answer-key)
        (local-select> (keypath *answer-key) $$streaming-answers :> *answered)
        (<<if (nil? *answered)

          ;; 2 and 3. The digest, check (b), the clock, the 10-second rule.
          (digest/digest *thing :> *digest)
          (declaration/check *thing :> {*act :ok *declarations-reason :refuse})
          (System/currentTimeMillis :> *now)
          (decision/refused-early *now *act *declarations-reason :> *refused-early)

          ;; 4. Standing: checks (c) and (d), and the first act of a layer.
          (<<if *refused-early
            (identity *refused-early :> *refused-standing)
            (identity false :> *makes-the-layer)
           (else>)
            (decision/permission-place *act :> *permission-key)
            (local-select> (keypath *permission-key) $$streaming-facts :> *permission-row)
            (<<if (nil? *permission-row)
              (decision/depot-place *act :> *depot-key)
              (local-select> (keypath *depot-key) $$streaming-facts :> *depot-row)
             (else>)
              (identity nil :> *depot-row))
            (<<if (and> (nil? *permission-row)
                        (nil? *depot-row)
                        (not= :streaming (first-act/depot-at-making *layer)))
              (decision/depot-place *act :> *other-depot-key)
              (local-select> (keypath *other-depot-key) $$microbatch-facts :> *other-depot-row)
              (local-select> (keypath :closed-through) $$microbatch-task :> *closed-through)
              (other-gates-record-holds-layer? *other-depot-row *closed-through :> *other-holds)
             (else>)
              (identity false :> *other-holds))
            (decision/standing :streaming *act *permission-row *depot-row *other-holds
                               :> *refused-standing)
            (decision/makes-the-layer? :streaming *act *permission-row *depot-row *other-holds
                                       :> *makes-the-layer))

          ;; 5. Places: checks (e), (f), (g), who may write a key that takes a
          ;; second write, and what a store key does not take.
          (<<if *refused-standing
            (identity *refused-standing :> *refused-places)
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
                (local-select> (keypath *place-key) $$streaming-facts :> *place-row)
                (continue> (rest *keys-left) (assoc *rows-read *place-key *place-row))))
            (decision/places :streaming *act *makes-the-layer *place-rows :> *refused-places))

          ;; 6. The seal: one range of locks for each request that the act names.
          (<<if *refused-places
            (identity *refused-places :> *refused)
            (identity {} :> *lock-rows)
            (identity nil :> *plain)
           (else>)
            (decision/lock-ranges :streaming *act :> *ranges)
            (loop<- [*ranges-left *ranges
                     *locks-read {}
                     :> *lock-rows]
              (<<if (empty? *ranges-left)
                (:> *locks-read)
               (else>)
                (first *ranges-left :> [*first-key *last-key])
                (local-select> [(sorted-map-range *first-key *last-key {:inclusive-end? true})
                                (view rows-as-map)]
                               $$streaming-locks
                               :> *range-rows)
                (continue> (rest *ranges-left) (merge *locks-read *range-rows))))
            (decision/opened :streaming *act *lock-rows :> {*refused :refused *plain :plain}))

          ;; 6b. Only for an act that no check refused: the layer's marked
          ;; keys, and the row before of each place under a marked key. Each
          ;; row is read once: step 5 may have read it.
          (<<if (and> (nil? *refused) (decision/reads-marked-keys? *act))
            (decision/marked-keys-place *act :> *marks-key)
            (<<if (contains? *place-rows *marks-key)
              (get *place-rows *marks-key :> *marks-row)
             (else>)
              (local-select> (keypath *marks-key) $$streaming-facts :> *marks-row))
            (decision/since-by-place *act *marks-row :> *since-by-place)
            (decision/rows-before-needed *since-by-place *place-rows :> *before-keys)
            (loop<- [*before-left *before-keys
                     *before-read {}
                     :> *before-rows]
              (<<if (empty? *before-left)
                (:> *before-read)
               (else>)
                (first *before-left :> *before-key)
                (local-select> (keypath *before-key) $$streaming-facts :> *before-row)
                (continue> (rest *before-left) (assoc *before-read *before-key *before-row))))
            (assoc (merge *place-rows *before-rows) *marks-key *marks-row :> *rows-in-hand)
           (else>)
            (identity {} :> *since-by-place)
            (identity *place-rows :> *rows-in-hand))

          ;; 7. The stamp, and the writes: each a whole row at a fixed key, or
          ;; a fixed key taken out.
          (local-select> (keypath :last-stamp) $$streaming-task :> *last-stamp)
          (decision/stamp-of *now *last-stamp *act *refused :> *stamp)
          ;; What the gate has in hand for an accept; nothing for a refusal.
          ;; A key that this act newly marks gets the act's own stamp.
          (<<if *refused
            (identity {} :> *in-hand)
           (else>)
            (hash-map :lock-rows *lock-rows
                      :plain *plain
                      :since-by-place *since-by-place
                      :place-rows *rows-in-hand
                      :level-from *stamp
                      :> *in-hand))
          (decision/writes *act-id *act *refused *stamp *digest *in-hand :> *writes)
          (ops/explode *writes :> [*which *key *row])
          (<<cond
            (case> (= *which :fact))
            (local-transform> [(keypath *key) (termval *row)] $$streaming-facts)

            (case> (and> (= *which :by-value) (nil? *row)))
            (local-transform> [(keypath *key) NONE>] $$streaming-facts-by-value)

            (case> (= *which :by-value))
            (local-transform> [(keypath *key) (termval *row)] $$streaming-facts-by-value)

            (case> (= *which :lock))
            (local-transform> [(keypath *key) (termval *row)] $$streaming-locks)

            (case> (= *which :new-lock))
            (new-lock-row :> *new-lock)
            (local-transform> [(keypath *key) (termval *new-lock)] $$streaming-locks)

            (case> (= *which :answer))
            (local-transform> [(keypath *key) (termval *row)] $$streaming-answers)

            (case> (= *which :last-stamp))
            (local-transform> [(keypath :last-stamp) (termval *row)] $$streaming-task)))))))
