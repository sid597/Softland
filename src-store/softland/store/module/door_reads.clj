;; Before changing this file, read the plans of the two parts again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-microbatch-gate.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-base-for-a-reader.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-read-path.md

(ns softland.store.module.door-reads
  "The two query topologies by which the door reads the module, for layers
  of both depots. It requires Rama. A query writes nothing.

    answer-by-act-id  [routing-id act-id] -> the recorded answer, or nil
        {:verdict :accepted or :refused, :reason the word or nil,
         :stamp the stamp, :digest the digest of the decided thing as hex}.
        nil is \"nothing yet\": no decision is readable under the act-id. It
        does not mean refused. The door gave its maker the digest of what
        it sent, and compares the two hex texts, and so does not take
        another act's answer as its own.
    depot-of-layer    [layer] -> the layer's depot, or nil
        nil for a layer that is not made.

  The routing id is `softland.store.record.rows/routing-id` of the act-id: the
  layer of a streaming act-id, the uuid of a microbatch act-id. The caller
  still needs the act-id alone.

  What they read. PStates that the two gates own and these borrow. For a
  streaming layer: `$$streaming-answers` and `$$streaming-facts`. For a
  microbatch layer: `$$microbatch-answers`, `$$microbatch-facts`, and
  `:closed-through` of `$$microbatch-task`. A query sees only committed
  state. Each read is of one key on the one task that the query runs on:
  one read for an answer or a depot of a streaming layer; for a microbatch
  layer one read, and one more, of `:closed-through`, only when the first
  found a row.

  What a read of a microbatch layer gives. An answer's row or a fact's row
  only when it is of a closed batch: its stamp is not later than the
  `:closed-through` of its task (`softland.store.record.rows/closed?`). Rama can
  run a batch again after one task has shown its part, with the other of
  two words for an act into a layer of the
  other gate; a closed batch cannot run again. A stamp and a layer's depot
  would be the same on a second attempt, since each attempt takes the
  batch's kept clock; the two reads are held to closing all the same, as
  one rule. So an answer or a
  layer's depot that a read gave never changes.

  Routing. Each begins with the hash partitioner on its first argument.
  Rama's client evaluates a leading partitioner on a query's own arguments,
  so the query goes straight to one task and never hops: the layer's task
  for a streaming act-id and for a layer's depot, the task of the uuid for a
  microbatch act-id.
  The depot row of a microbatch layer is on every task, so on the layer's.
  Which record holds a layer's depot is told by the depot that the layer
  starts with (`softland.store.gate.first-act/depot-at-making`): no act of this
  build changes a depot, so the other record cannot hold it.

  The head of each query. `softland.store.record.rows`'s
  `answer-read-by-routing-id` and
  `depot-read` test what the caller gave, before any key is built from it:
  an id over the limit, a thing that is no act-id, or a routing id that is
  not the one of the act-id's depot costs no read and gets nil. Those tests
  run on the task, after the client has hashed the first
  argument; a caller that may hold a thing that is no id tests it with the
  same functions before it calls.

  Any client of the cluster can call these two queries, and can read every
  value: in a table of facts, and in a depot of acts, where every value is
  plain. In the runtime the door
  (`softland.store.door`) is the one client of the queries. A
  guard of the record is not built (?11 is open). The PStates are not
  declared private. The read path (`softland.store.read-path`) selects and
  watches the tables of facts, and of facts by value, from outside, by
  design: it is the way by which a fact comes back out to a caller. It
  reads no answer, and it reads a row straight, not held to
  `:closed-through`."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [softland.store.gate.first-act :as first-act]
            [softland.store.record.rows :as record]))

(defn add-to
  "Adds the two queries to a module. Takes the module's `topologies`. What
  must hold: the module has the two gates, which declare the PStates that
  these read."
  [topologies]
  (<<query-topology topologies "answer-by-act-id" [*routing-id *act-id :> *answer]
    (|hash *routing-id)
    (record/answer-read-by-routing-id *routing-id *act-id :> *read)
    (first *read :> *depot)
    (<<cond
      (case> (= :streaming *depot))
      (second *read :> *key)
      (local-select> (keypath *key) $$streaming-answers :> *row)
      (record/answer-of *row :> *answer)

      (case> (= :microbatch *depot))
      (second *read :> *key)
      (local-select> (keypath *key) $$microbatch-answers :> *row)
      (<<if *row
        (local-select> (keypath :closed-through) $$microbatch-task :> *closed-through)
        (<<if (record/closed? *row *closed-through)
          (record/answer-of *row :> *answer)
         (else>)
          (identity nil :> *answer))
       (else>)
        (identity nil :> *answer))

      (default>)
      (identity nil :> *answer))
    (|origin))

  (<<query-topology topologies "depot-of-layer" [*layer :> *depot]
    (|hash *layer)
    (record/depot-read *layer :> *key)
    (<<cond
      (case> (nil? *key))
      (identity nil :> *depot)

      (case> (= :microbatch (first-act/depot-at-making *layer)))
      (local-select> (keypath *key) $$microbatch-facts :> *row)
      (<<if *row
        (local-select> (keypath :closed-through) $$microbatch-task :> *closed-through)
        (<<if (record/closed? *row *closed-through)
          (record/value-of *row :> *depot)
         (else>)
          (identity nil :> *depot))
       (else>)
        (identity nil :> *depot))

      (default>)
      (local-select> (keypath *key) $$streaming-facts :> *row)
      (record/value-of *row :> *depot))
    (|origin)))
