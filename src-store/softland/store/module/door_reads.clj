;; Before changing this file, read the plans of the two parts again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-microbatch-gate.md

(ns softland.store.module.door-reads
  "The three query topologies by which the door reads the module, for layers
  of both classes. It requires Rama. A query writes nothing.

    answer-by-act-id  [routing-id act-id] -> the recorded answer, or nil
        {:verdict :accepted or :refused, :reason the word or nil,
         :stamp the stamp, :digest the digest of the decided thing as hex}.
        nil is \"nothing yet\": no decision is readable under the act-id. It
        does not mean refused. The sender compares the digest with the
        digest of what it sent (`softland.store.door.digest/is-digest-of?`), and
        so does not take another act's answer as its own.
    locks-of-request  [routing-id request-act-id from to] -> {index lock}
        the locks that the request made, of the indexes from `from` to `to`,
        both in the range, each as {:bytes :way}. An empty map when the
        request made no lock or its answer is not yet readable. One call
        gives all the locks of one request. In a streaming layer a lock is
        given with its bytes in each state. In a microbatch layer a lock
        that seals a value is given with its way and with nil for its
        bytes: they are in one row, beside the value, on the value's task.
        That holds also where the lock's home is the value's task and the
        one row there holds the bytes: the read leaves them out
        (`softland.store.record.rows/locks-by-index-used-without-bytes`).
    class-of-layer    [layer] -> the layer's class, or nil
        nil for a layer that is not made.

  The routing id is `softland.store.record.rows/routing-id` of the act-id: the
  layer of a streaming act-id, the uuid of a microbatch act-id. The caller
  still needs the act-id alone.

  What they read. PStates that the two gates own and these borrow. For a
  streaming layer: `$$streaming-answers`, `$$streaming-locks`,
  `$$streaming-facts`. For a microbatch layer: `$$microbatch-answers`,
  `$$microbatch-locks`, `$$microbatch-facts`, and `:closed-through` of
  `$$microbatch-task`. A query sees only committed state.

  What a read of a microbatch layer gives. An answer's row or a fact's row
  only when it is of a closed batch: its stamp is not later than the
  `:closed-through` of its task (`softland.store.record.rows/closed?`). Rama can
  run a batch again after one task has shown its part, with another clock
  and other lock bytes; a closed batch cannot run again. So an answer, a
  class or a lock's bytes that a read gave never changes. A lock's row holds
  no stamp: the read of locks holds the stamp of the request's answer, which
  is on the same task, and gives locks only for an accepted request. So it
  is held to the closing of the request's batch, and not of a later batch in
  which an act uses one of the locks: from the moment when that batch is
  visible on this task, the read gives the lock without its bytes, one run
  before that batch is closed. The second read, of `:closed-through`, is
  made only when the first found a row.

  Routing. Each begins with the hash partitioner on its first argument.
  Rama's client evaluates a leading partitioner on a query's own arguments,
  so the query goes straight to one task and never hops: the layer's task
  for a streaming act-id and for a class, the task of the uuid for a
  microbatch act-id. A request's answer and its locks are on that one task.
  The class row of a microbatch layer is on every task, so on the layer's.
  Which record holds a layer's class is told by the class that the layer
  starts with (`softland.store.gate.first-act/class-at-making`): no act of this
  build changes a class, so the other record cannot hold it.

  The head of each query. `softland.store.record.rows`'s
  `answer-read-by-routing-id`, `lock-range-read-by-routing-id` and
  `class-read` test what the caller gave, before any key is built from it:
  an id over the limit, a thing that is no act-id, or a routing id that is
  not the one of the act-id's class costs no read and gets nil or an empty
  map. Those tests run on the task, after the client has hashed the first
  argument; a caller that may hold a thing that is no id tests it with the
  same functions before it calls.

  Any caller of the module can read a lock's bytes in this build; the door
  proper is the guard, later. The PStates are not declared private, so a
  select of one from outside is a second way to a row, and that way is not
  held to `:closed-through`."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [softland.store.gate.first-act :as first-act]
            [softland.store.record.rows :as record]))

(defn add-to
  "Adds the three queries to a module. Takes the module's `topologies`. What
  must hold: the module has the two gates, which declare the PStates that
  these read."
  [topologies]
  (<<query-topology topologies "answer-by-act-id" [*routing-id *act-id :> *answer]
    (|hash *routing-id)
    (record/answer-read-by-routing-id *routing-id *act-id :> *read)
    (first *read :> *class)
    (<<cond
      (case> (= :streaming *class))
      (second *read :> *key)
      (local-select> (keypath *key) $$streaming-answers :> *row)
      (record/answer-of *row :> *answer)

      (case> (= :microbatch *class))
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

  (<<query-topology topologies "locks-of-request" [*routing-id *request-act-id *from *to :> *locks]
    (|hash *routing-id)
    (record/lock-range-read-by-routing-id *routing-id *request-act-id *from *to :> *read)
    (first *read :> *class)
    (<<cond
      (case> (= :streaming *class))
      (nth *read 1 :> *first-key)
      (nth *read 2 :> *last-key)
      (local-select> [(sorted-map-range *first-key *last-key {:inclusive-end? true})
                      (view record/locks-by-index)]
                     $$streaming-locks
                     :> *locks)

      (case> (= :microbatch *class))
      (nth *read 1 :> *first-key)
      (nth *read 2 :> *last-key)
      (record/answer-key (nth *request-act-id 0) (nth *request-act-id 2) :> *answer-key)
      (local-select> (keypath *answer-key) $$microbatch-answers :> *request-row)
      ;; Locks are given only for an accepted request whose batch is closed.
      (<<if (and> *request-row (nil? (get *request-row :refused)))
        (local-select> (keypath :closed-through) $$microbatch-task :> *closed-through)
        (<<if (record/closed? *request-row *closed-through)
          (local-select> [(sorted-map-range *first-key *last-key {:inclusive-end? true})
                          (view record/locks-by-index-used-without-bytes)]
                         $$microbatch-locks
                         {:allow-yield? true}
                         :> *locks)
         (else>)
          (identity {} :> *locks))
       (else>)
        (identity {} :> *locks))

      (default>)
      (identity {} :> *locks))
    (|origin))

  (<<query-topology topologies "class-of-layer" [*layer :> *class]
    (|hash *layer)
    (record/class-read *layer :> *key)
    (<<cond
      (case> (nil? *key))
      (identity nil :> *class)

      (case> (= :microbatch (first-act/class-at-making *layer)))
      (local-select> (keypath *key) $$microbatch-facts :> *row)
      (<<if *row
        (local-select> (keypath :closed-through) $$microbatch-task :> *closed-through)
        (<<if (record/closed? *row *closed-through)
          (record/plain-value-of *row :> *class)
         (else>)
          (identity nil :> *class))
       (else>)
        (identity nil :> *class))

      (default>)
      (local-select> (keypath *key) $$streaming-facts :> *row)
      (record/plain-value-of *row :> *class))
    (|origin)))
