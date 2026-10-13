;; Before changing this file, read the papers of this part again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-base-for-a-reader.md
;; src/proposal/backend-on-rama-2026-10-02/run-again/PROGRESS.md

(ns softland.store.module.streaming-gate
  "The streaming gate: the stream topology that decides each thing of the
  streaming depot, and the four PStates of its record. It requires Rama.

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
  keys and the layer's last stamp are read from the PStates in each event
  that needs them.

  What a decision is made of. The thing, and rows of the gate's own record
  on the task of the event. Nothing else: the gate reads no clock, it
  borrows no PState and so reads no table of the other gate, and it takes
  no random number. It declares no depot and appends to none.

  Approach. The dataflow reads rows, calls pure functions and writes rows.
  The test for a stray and the check of what arrived are
  `softland.store.door.declaration`'s, the checks against the record and
  the stamp are `softland.store.gate.decision`'s, and each row comes from
  `softland.store.record.rows`. None of them throws, so no thing can stop a
  partition: a refusal is data, an answer with a reason word.

  The walk of one event, for one thing:

    0. a stray?   A thing with no usable act-id, or whose act-id is of the
                  other gate or of another layer, gets no answer. Its digest
                  is added to the set of its reason word on the task, and
                  the event ends.
    1. check (a)  Read the answer under the act-id. If one is there, the
                  event decides nothing, writes no row and ends, whether
                  the thing is the same act again or a different one. So a
                  repeat is not decided twice, and one act-id has one
                  answer: that of the first thing to arrive under it.
    2. the digest of the thing as it arrived, which leaves the time on the
                  act out (`softland.store.door.digest`), and check (b),
                  the door's declaration. A thing with no usable time is
                  refused `:no-door-time` here. The declaration's last step
                  makes the one text of each value and holds the bound on
                  one value and the bound on the act, so a thing over
                  either is refused here, before any row of its standing is
                  read.
    3. the time   The time on the act, which the door wrote
                  (`softland.store.gate.decision/time-on`); nil for a thing
                  with no usable time. A thing that the declaration refuses
                  for another fault keeps its time. The 10-second rule
                  holds each carried stamp against it.
    4. standing   Read the place of `:who`'s permission; only when it is
                  empty, the place of the layer's depot. When both are
                  empty and the layer starts with the other gate's depot,
                  which is the base, the act is refused
                  `:layer-belongs-to-the-other-gate`, whether or not the
                  other gate has made the layer: the gate asks the layer id
                  (`softland.store.gate.first-act/starts-with-the-other-gates-depot?`)
                  and reads nothing for it. A permission that stands and
                  says `:see` is refused here, with no read more: an act
                  needs \"act\".
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
                  the layer's text in `:last-stamp` of `$$streaming-task`.
                  The stamp is made of the time on the act, that last
                  stamp, and the stamps that the act carries
                  (`softland.store.gate.decision/stamp-of`). A refusal
                  writes the answer and the layer's last stamp and nothing
                  else. An accept writes the row of each fact, with the
                  text of its value, who wrote it and the permission that
                  the act named; for a fact under a marked key the entry of
                  its value in the facts by value, and the entry of the
                  value before taken out; the answer and the layer's last
                  stamp. The last write is the event's last step.

  The stamp is counted in the layer. Each layer has its own last stamp, so
  a decision in one layer does not raise the stamps of another layer of the
  task, and two layers of one task can hold the same stamp. In one layer
  each stamp is later than the one before, in the order in which the gate
  decided. A thing with no usable time gets the layer's last stamp plus
  one, or 0 in a layer with no last stamp.

  What an event costs, in reads and writes of a row. A later stage's rows
  are read only when the earlier checks passed, and no check reads the
  marked keys.

    - A stray: no read, and one add to a set.
    - A thing under an act-id that has an answer: 1 read, no write.
    - A thing that the declaration refuses, one with no usable time or with
      a value over the bound among them: 2 reads, the answer and the layer's
      last stamp, and 2 writes, the answer and the layer's last stamp.
    - An act whose permission does not stand: 4 reads at most, whatever it
      carries, the answer, the two places of its standing and the layer's
      last stamp, and the same 2 writes. An act into the base on this depot
      is one of these.
    - A making act: those 4 reads, and 4 writes: its two facts, the answer
      and the layer's last stamp.
    - One accepted act with one value under a plain key: 4 reads and 3
      writes. The reads are the answer, the permission, the marked keys and
      the layer's last stamp; the writes are the fact, the answer and the
      layer's last stamp. The read of the marked keys is paid by every
      accepted act that holds a fact under a plain key, also in a layer
      that marks no key. When the fact's key is marked it costs 5 reads,
      and with them the row before, and 4 writes, or 5 when the entry of a
      value before goes out.
    - An act with no fact under a plain key, a give, a setting or a list,
      reads no marked keys.

  An event makes no hop to another task, appends nothing and waits for
  nothing. The read and the write of a layer's last stamp are each counted
  as one above; the number sits one level down in its table, under a field
  and then the layer's key (`softland.store.record.tables` says what that
  costs and why it is so).

  Between the event's first read and its last write stands nothing that
  gives the task back: no partitioner, no yield, no append. The event starts
  and ends on the task of the depot's partition, so all its writes are one
  group: they become visible together or not at all, and a reader sees an
  accepted answer, each fact of its act, the entry that came into the facts
  by value and the entry that went out at once. Rama's docs say this of a
  batch of streaming events on one task. No other event of the task runs
  between this event's reads and its writes.

  The marked keys. An act is decided with the layer's marks as they were
  before it. So a fact of the act that marks its own key is not in the order
  by value; the key gets the stamp of that act; and every fact with a later
  stamp under that key is in the order. The depot's order is the order of a
  mark and of a fact after it.

  When Rama runs an event again. A stream topology can process a record
  again, also after its writes were committed. Each write of a decision is
  behind check (a), and the answer commits with the rest: a second run after
  a commit finds the answer at check (a) and writes no row. So one act-id
  has one answer, a layer's last stamp is not raised twice for one thing,
  and no entry of the facts by value that a later act put back is taken out
  again. A second run after a discard is the first that counts; the rows of
  the discarded run were never visible. No write of a decision holds
  anything but what the thing and the rows read give, and no clock is among
  them: when the second run reads the rows that the discarded run read, it
  writes the same rows, with the same stamp. The stray's write is not
  behind check (a); it is an add to a set, the same write however often it
  runs. The source's retry mode is \"all after\": when a record fails, each
  record after it on the partition is processed again, in order.

  What this does not close: the order of a layer's acts after a failure.
  Without a failure, the acts of a layer are decided in the depot's order.
  When something throws while a batch of events runs, Rama discards the
  writes of that batch and processes its records again. It is not shown
  whether acts of a layer that were on their way then can be decided in
  another order than the depot's. If they can, their stamps and their
  answers can differ from those of a run of the gate over the same depot
  that meets no failure there. A run of this build looks into that (Sid's
  decision 7 of 13 October 2026), and this docstring says nothing of what
  it finds. The gate's own code does not throw on what arrives, so such a
  failure needs a fault of the machine, of Rama or of this module. The
  store's map, one folder up, names the setting of Rama that orders a
  layer's acts after a failure, and what it costs.

  What the gate rests on and does not test. The depot's partitioner puts a
  thing on its layer's task, and the gate makes no hop. A thing that a
  topology appends to another partition is decided against that task's
  record, with that task's last stamp of the layer; the store's map, one
  folder up, says what follows.

  The source starts from the beginning of the depot, so no thing that is in
  the depot is skipped whenever the topology first runs on a partition.
  Nothing is given back in the ack: the ack is a signal, and the record is
  the truth, read by act-id. A sender's append with the ack level :ack
  returns when the event is done, and the event's last write is its last
  step."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :refer :all]
            [softland.store.gate.decision :as decision]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.gate.first-act :as first-act]
            [softland.store.record.rows :as record]
            [softland.store.record.tables :as record-pstates]))

(defn add-to
  "Adds the streaming gate to a module. Takes the module's `topologies`.
  Declares the stream topology `streaming-gate` and its four PStates, and
  the dataflow of one event, as the namespace docstring walks it. Each write
  of a decision is a whole row at a fixed key, or a fixed key taken out: the
  same write however often it runs. Every read and every write of the event
  are on the task of the depot's partition: the event has no partitioner,
  no append and no wait. The source starts from the beginning of the depot,
  with the retry mode \"all after\". What must hold: the module declares the
  depot `*streaming-depot`. The gate needs no other part of the module: it
  declares no depot and reads no PState of another topology."
  [topologies]
  (let [gate (stream-topology topologies "streaming-gate")]
    (declare-pstate gate $$streaming-answers (record-pstates/answers))
    (declare-pstate gate $$streaming-facts (record-pstates/facts))
    (declare-pstate gate $$streaming-facts-by-value (record-pstates/facts-by-value))
    (declare-pstate gate $$streaming-task (record-pstates/task))
    (<<sources gate
      (source> *streaming-depot {:start-from :beginning :retry-mode :all-after} :> *thing)

      ;; 0. A stray gets no answer: its digest goes to the set of its word,
      ;; and the event ends.
      (declaration/stray-reason :streaming *thing :> *stray)
      (<<if *stray
        (digest/hex-of *thing :> *stray-digest)
        (local-transform> [(keypath :strays *stray) NONE-ELEM (termval *stray-digest)]
                          $$streaming-task)

       (else>)
        ;; 1. Check (a): an act-id that has an answer is not decided again.
        ;; The event then writes nothing and ends.
        (get *thing :act-id :> *act-id)
        (nth *act-id 0 :> *layer)
        (nth *act-id 2 :> *act-uuid)
        (record/answer-key *layer *act-uuid :> *answer-key)
        (local-select> (keypath *answer-key) $$streaming-answers :> *answered)
        (<<if (nil? *answered)

          ;; 2 and 3. The digest, check (b), the time on the act, the
          ;; 10-second rule. No clock is read: the time is the thing's own.
          (digest/digest *thing :> *digest)
          (declaration/check *thing :> {*act :ok *declarations-reason :refuse})
          (decision/time-on *thing :> *act-time)
          (decision/refused-early *act-time *act *declarations-reason :> *refused-early)

          ;; 4. Standing: checks (c) and (d), and the first act of a layer.
          ;; Whether the layer is the other gate's is asked of the layer id,
          ;; and no table of the other gate is read.
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
            (first-act/starts-with-the-other-gates-depot? :streaming *layer
                                                          :> *layer-is-the-other-gates)
            (decision/standing :streaming *act *permission-row *depot-row *layer-is-the-other-gates
                               :> *refused-standing)
            (decision/makes-the-layer? :streaming *act *permission-row *depot-row
                                       *layer-is-the-other-gates
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

          ;; 7. The stamp, of the time on the act and of the layer's last
          ;; stamp, which is kept under the layer's text.
          (declaration/id-text *layer :> *layer-text)
          (local-select> (keypath :last-stamp *layer-text) $$streaming-task :> *last-stamp)
          (decision/stamp-of *act-time *last-stamp *act *refused :> *stamp)
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
          ;; The writes: each a whole row at a fixed key, or a fixed key taken
          ;; out. One row at a time, with no yield between two. Nothing
          ;; stands behind them: the last write is the event's last step.
          (ops/explode *writes :> [*which *key *row])
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
            (local-transform> [(keypath :last-stamp *layer-text) (termval *row)] $$streaming-task)

            (default>)
            (identity nil :> *no-row)))))))
