;; Before changing this file, read the plans of this part again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-base-for-a-reader.md
;; src/proposal/backend-on-rama-2026-10-02/before-real-data/PLAN-answers-in-a-log.md

(ns softland.store.module.streaming-gate
  "The streaming gate: the stream topology that decides each thing of the
  streaming depot, the four PStates of its record, and its depot of answers.
  It requires Rama.

  What it takes. Each thing that arrives on `*streaming-depot`, which the
  module declares (`softland.store.module`) and which partitions by the
  thing's `:layer`. Anything can arrive: the door is not the only sender.

  What it changes. It owns four PStates, the record of the streaming layers,
  and it alone writes them (in Rama only the owning topology writes a
  PState): `$$streaming-answers`, `$$streaming-facts`,
  `$$streaming-facts-by-value` and `$$streaming-task`.
  Their schemas are `softland.store.record.tables`'s and their rows
  `softland.store.record.rows`'s. They are durable state of the cluster. The
  gate keeps nothing in the memory of a process: the row of a layer's marked
  keys is read from the PState in each event that needs it.

  It declares the depot `*streaming-answer-depot`, its depot of answers, and
  it alone appends to it: the depot is declared :disallow, so no client can
  append. A record is a map of entries, as
  `softland.store.record.rows/log-record` makes it. For a thing that has an
  answer it is {:answers [entry]}, the entry
  {:act-id [layer depot uuid] :stamp :digest}, with `:refused` and the word
  for a refusal. For a stray it is {:strays [entry]}, the entry
  {:stray :digest :task}. No value of a fact is in a record. The gate sets
  no trimming option, so Rama keeps every record. No topology consumes the
  depot and no read of the store opens it: the PStates are the record that
  is read, and the depot is the log of what the gate decided. An append from
  a topology goes to the partition of the task that the code is on, so a
  record sits on the partition of the task that decided.

  What it borrows. Two PStates of the microbatch gate, to read on its own
  task: `$$microbatch-facts` and `$$microbatch-task`. It reads them only for
  an act into a layer of which its own record holds nothing and that starts
  with the other gate's depot, which is the base.

  Approach. The dataflow reads rows, calls pure functions, writes rows, and
  at its end appends one record. The test for a stray and the check of what
  arrived are `softland.store.door.declaration`'s, the checks against the
  record are `softland.store.gate.decision`'s, and each row and each entry
  comes from `softland.store.record.rows`. None of them throws, so no thing
  can stop a partition: a refusal is data, an answer with a reason word.

  The walk of one event, for one thing:

    0. a stray?   A thing with no usable act-id, or whose act-id is of the
                  other gate or of another layer, gets no answer. Its digest
                  is added to the set of its reason word, and the event goes
                  to step 8 with what a stray's entry is made of: the word,
                  the digest and the task of the set.
    1. check (a)  Read the answer under the act-id. If one is there, the
                  event decides nothing and writes no row, whether the thing
                  is the same act again or a different one, and goes to
                  step 8 with the row that it read. So a repeat is
                  answered, not decided twice, and one act-id has one
                  answer.
    2. the digest of the thing as it arrived, and check (b), the door's
                  declaration. Its last step makes the one text of each
                  value and holds the bound on one value and the bound on
                  the act, so a thing over either is refused here, before
                  any row of its standing is read.
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
    6. the marked keys   Only for an act that no check refused, and only
                  when it holds a fact under a plain key or under
                  `:softland.store/marked-keys`: read the row at the layer's
                  marked-keys place, unless step 5 read it. Then read the
                  place of each fact under a key that the row marks, unless
                  step 5 read it: the row before gives the entry to take out
                  of the facts by value.
    7. the stamp  Read the task's last stamp. A refusal writes the answer
                  and the last stamp and nothing else. An accept writes the
                  row of each fact, with the text of its value, who wrote it
                  and the permission that the act named; for a fact under a
                  marked key the entry of its value in the facts by value,
                  and the entry of the value before taken out; the answer
                  and the last stamp. The event goes on with the answer's
                  row as it was written.
    8. the commit A partitioner to the event's own task. Rama commits the
                  writes of the steps before it, and then the event goes on.
    9. the entry  The entry is made here, behind the commit: an answer's
                  entry of the layer and the uuid of the act-id and the row
                  that step 7 wrote or step 1 read, or a stray's entry of
                  what step 0 handed on. Then one append to
                  `*streaming-answer-depot`, of the record that holds the
                  entry, with the ack level :append-ack: the event goes on
                  when the record is on the depot's partition and
                  replicated. It is the event's last step.

  A later stage's rows are read only when the earlier checks passed, and no
  check reads the marked keys. So an act whose permission does not stand
  costs 4 reads at most, whatever it carries, and an act into the base on
  this depot 6 at most. A thing that the declaration refuses, a value over
  the bound among them, costs 2 reads, the answer and the last stamp, and 2
  row writes. One accepted act with one value under a plain key costs 4
  reads and 3 row writes: the reads are the answer, the permission, the
  marked keys and the last stamp; the writes are the fact, the answer and
  the last stamp. The read of the marked keys is paid by every accepted act
  that holds a fact under a plain key, also in a layer that marks no key.
  When the fact's key is marked it
  costs 5 reads, and with them the row before, and 4 row writes, or 5 when
  the entry of a value before goes out. An act with no fact under a plain
  key, a give, a setting or a list, reads no marked
  keys.

  Steps 8 and 9 add no read and no row write. Every event pays them, on
  whichever road it came: one commit boundary, one append that is waited
  for, and one record that the depot keeps. So a thing under an act-id that
  has an answer costs 1 read, the boundary and the append, and a stray one
  add to a set, the boundary and the append.

  Between the event's first read and its last write stands nothing that
  gives the task back: no partitioner, no yield, no append. The event starts
  and ends on the task of the depot's partition, so all its writes are one
  group: they become visible together or not at all, and a reader sees an
  accepted answer, each fact of its act, the entry that came into the facts
  by value and the entry that went out at once. Rama's docs say this of a
  batch of streaming events on one task. No other event of the task runs
  between this event's reads and its writes. The event gives its task back
  at step 8, behind its last write, and while the append of step 9 is
  waited for. Behind step 8 it reads nothing and writes no row: it makes the
  entry of what it carried over the boundary, by pure functions, and
  appends. So nothing that runs on the task meanwhile changes what it
  decided or what it appends.

  Why the entry and the append stand behind a commit. Rama's docs say that
  the writes before a partitioner are committed before the event goes past
  it. So an entry is made and appended only when the row that it copies is
  committed, and the depot holds no entry of an answer that the PStates do
  not hold. A reader by act-id can be given an answer a moment before its
  entry is in the depot, never the other way round. The making of the entry
  (`softland.store.record.rows/answer-record`, `stray-record`,
  `log-record`) is the one step of the store's own code between the commit
  and the append: a failure there, as in the append, leaves a committed
  answer with no entry, which the next run of the record makes good. Why
  :append-ack: Rama gives no
  guarantee that an append with no ack level goes through, and :ack would
  wait for any stream topology that came to consume the depot.

  The marked keys. An act is decided with the layer's marks as they were
  before it. So a fact of the act that marks its own key is not in the order
  by value; the key gets the stamp of that act; and every fact with a later
  stamp under that key is in the order. The depot's order is the order of a
  mark and of a fact after it.

  When Rama runs an event again. A stream topology can process a record
  again, also after its writes were committed. Each write of a decision is
  behind check (a), and the answer commits with the rest: a second run after
  a commit finds the answer at check (a), writes no row and appends one copy
  of its entry. So one act-id has one answer, no stamp is given twice, and
  no entry of the facts by value that a later act put back is taken out
  again. A second run after a discard is the first that counts; the rows of
  the discarded run were never visible, and it made no append, which stands
  behind the commit. Its rows are those of the discarded run but for the
  stamp, which holds the clock: no write of a decision holds anything but
  what the thing, the rows read and the clock give. The stray's write is not
  behind check (a); it is an add to a set, the same write however often it
  runs. The source's retry mode is \"all after\": when a record fails, each
  record after it on the partition is processed again, in order.

  The append is the one write that is not the same however often it runs:
  each run of an event that reaches step 9 leaves one record. Every entry
  under one act-id is made of the one committed row of that act-id, so all
  hold the same answer, and a reader of the depot takes one. A stray's entry
  names a member that the set of its word holds on its task, so the entries
  of one stray give that member once. The gate cannot tell a sender's second
  thing under an act-id from Rama's second run of the first, so it appends a
  copy at every hit of check (a). That copy is what leaves no answer without
  an entry. When an event fails between the commit of step 8 and the append,
  its answer is committed and has no entry; the event of its record did not
  complete, Rama runs the record again, and that run appends the copy. Until
  that run the answer has no entry, and in a store that never runs again it
  has none for good.

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
  the truth, read by act-id. A sender's append with the ack level :ack
  returns when the event is done, and the append of step 9 is the event's
  last step."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :refer :all]
            [softland.store.gate.decision :as decision]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.gate.first-act :as first-act]
            [softland.store.record.rows :as record]
            [softland.store.record.tables :as record-pstates]))

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

(defn add-to
  "Adds the streaming gate to a module. Takes the module's `setup` and its
  `topologies`. Declares the depot `*streaming-answer-depot`, closed to a
  client's append; the stream topology `streaming-gate` and its four
  PStates; and the dataflow of one event, as the namespace docstring walks
  it. Each write of a decision is a whole row at a fixed key, or a fixed key
  taken out: the same write however often it runs. The rows are written by a
  loop that ends, so what stands behind it runs once for the event, on
  whichever of the three roads the event came: one partitioner to the
  event's own task; behind it the making of the entry and of its record, of
  what the event carried over the partitioner; and one append with the ack
  level :append-ack. Every
  read, every write and the append of the event are on the task of the
  depot's partition. What must hold: the module declares the depot
  `*streaming-depot`, and has the microbatch gate, which declares the two
  PStates that step 4 borrows."
  [setup topologies]
  (declare-depot setup *streaming-answer-depot :disallow)
  (let [gate (stream-topology topologies "streaming-gate")]
    (declare-pstate gate $$streaming-answers (record-pstates/answers))
    (declare-pstate gate $$streaming-facts (record-pstates/facts))
    (declare-pstate gate $$streaming-facts-by-value (record-pstates/facts-by-value))
    (declare-pstate gate $$streaming-task (record-pstates/task))
    (<<sources gate
      (source> *streaming-depot {:start-from :beginning :retry-mode :all-after} :> *thing)

      ;; 0. A stray gets no answer: its digest goes to the set of its word.
      ;; The event goes on with what step 9 makes a stray's entry of: with
      ;; the word, the digest and the task of the set.
      (declaration/stray-reason :streaming *thing :> *stray)
      (<<if *stray
        (digest/hex-of *thing :> *stray-digest)
        (local-transform> [(keypath :strays *stray) NONE-ELEM (termval *stray-digest)]
                          $$streaming-task)
        (ops/current-task-id :> *task)
        (vector *stray-digest *task :> *entry-parts)

       (else>)
        ;; 1. Check (a): an act-id that has an answer is not decided again.
        ;; The event goes on with the row that it found.
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
                (local-select> (keypath *place-key) $$streaming-facts :> *place-row)
                (continue> (rest *keys-left) (assoc *rows-read *place-key *place-row))))
            (decision/places :streaming *act *makes-the-layer *place-rows :> *refused))

          ;; 6. Only for an act that no check refused: the layer's marked
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
            (hash-map :since-by-place *since-by-place
                      :place-rows *rows-in-hand
                      :level-from *stamp
                      :> *in-hand))
          (decision/writes *act-id *act *refused *stamp *digest *in-hand :> *writes)
          ;; A loop that ends, so that steps 8 and 9 run once for the event
          ;; and not once for each row. It does not yield. It gives the
          ;; answer's row as it was written.
          (loop<- [*writes-left *writes
                   *answer-so-far nil
                   :> *answer-row]
            (<<if (empty? *writes-left)
              (:> *answer-so-far)
             (else>)
              (first *writes-left :> [*which *key *row])
              (<<cond
                (case> (= *which :fact))
                (local-transform> [(keypath *key) (termval *row)] $$streaming-facts)

                (case> (and> (= *which :by-value) (nil? *row)))
                (local-transform> [(keypath *key) NONE>] $$streaming-facts-by-value)

                (case> (= *which :by-value))
                (local-transform> [(keypath *key) (termval *row)] $$streaming-facts-by-value)

                (case> (= *which :answer))
                (local-transform> [(keypath *key) (termval *row)] $$streaming-answers)

                (case> (= *which :last-stamp))
                (local-transform> [(keypath :last-stamp) (termval *row)] $$streaming-task)

                (default>)
                (identity nil :> *no-row))
              (<<if (= *which :answer)
                (identity *row :> *answer-now)
               (else>)
                (identity *answer-so-far :> *answer-now))
              (continue> (rest *writes-left) *answer-now)))

         (else>)
          (identity *answered :> *answer-row))
        ;; What step 9 makes the answer's entry of.
        (vector *layer *act-uuid *answer-row :> *entry-parts))

      ;; 8. The commit boundary, behind the event's last write: the rows are
      ;; committed before an entry is made or appended.
      (|direct (ops/current-task-id))

      ;; 9. The entry, made here behind the commit, and one append of its
      ;; record, on the task that decided. No read and no write of a row.
      (<<if *stray
        (identity *entry-parts :> [*entry-digest *entry-task])
        (record/stray-record *stray *entry-digest *entry-task :> *stray-record)
        (record/log-record nil (vector *stray-record) :> *to-append)
       (else>)
        (identity *entry-parts :> [*entry-layer *entry-uuid *entry-row])
        (record/answer-record :streaming *entry-layer *entry-uuid *entry-row :> *answer-record)
        (record/log-record (vector *answer-record) nil :> *to-append))
      (depot-partition-append! *streaming-answer-depot *to-append :append-ack))))
