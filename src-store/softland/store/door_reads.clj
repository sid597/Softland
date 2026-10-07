;; Before changing this file, read the plan of this part again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md

(ns softland.store.door-reads
  "The three query topologies by which the door reads the module. It requires
  Rama. A query writes nothing.

    answer-by-act-id  [layer act-id] -> the recorded answer, or nil
        {:verdict :accepted or :refused, :reason the word or nil,
         :stamp the stamp, :digest the digest of the decided thing as hex}.
        nil is \"nothing yet\": no decision is visible under the act-id. It
        does not mean refused. The sender compares the digest with the
        digest of what it sent (`softland.store.digest/is-digest-of?`), and
        so does not take another act's answer as its own.
    locks-of-request  [layer request-act-id from to] -> {index lock}
        the locks that the request made, of the indexes from `from` to `to`,
        both in the range, each as {:bytes :way}; in each state of the lock.
        An empty map when the request made no lock or its answer is not yet
        visible. One call gives all the locks of one request.
    class-of-layer    [layer] -> the layer's class, or nil
        nil for a layer that is not made.

  What they read. The streaming gate's PStates, which that gate owns and
  these borrow: `$$streaming-answers`, `$$streaming-locks`,
  `$$streaming-facts`. A query sees only committed state, so it gives an
  accepted answer only when each row of that act is visible too.

  Routing. Each takes the layer as its first argument and begins with the
  hash partitioner on it. Rama's client evaluates a leading partitioner on a
  query's own arguments, so the query goes straight to the layer's task and
  never hops. Each costs one seek there; the locks cost one step more for
  each lock. The caller still needs the act-id alone: the layer is its first
  slot.

  The head of each query. `softland.store.record`'s `answer-read`,
  `lock-range-read` and `class-read` test what the caller gave, before any
  key is built from it: an id over the limit, a thing that is no act-id, or
  a layer that is not the act-id's layer slot costs no read and gets nil or
  an empty map. Those tests run on the task, after the client has hashed the
  first argument; a caller that may hold a thing that is no id tests it with
  the same functions before it calls.

  A branch by class. Each query looks at the class of what it was asked for.
  In this build only the streaming gate has a record, so the branch for the
  class microbatch gives nothing.

  Any caller of the module can read a lock's bytes in this build; the door
  proper is the guard, later. The locks PState is not declared private, so a
  select of it from outside is a second way to a lock's bytes."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [softland.store.record :as record]))

(defn add-to
  "Adds the three queries to a module. Takes the module's `topologies`. What
  must hold: the module has the streaming gate, which declares the PStates
  that these read."
  [topologies]
  (<<query-topology topologies "answer-by-act-id" [*layer *act-id :> *answer]
    (|hash *layer)
    (record/answer-read *layer *act-id :> *read)
    (first *read :> *class)
    (<<cond
      (case> (= :streaming *class))
      (second *read :> *key)
      (local-select> (keypath *key) $$streaming-answers :> *row)
      (record/answer-of *row :> *answer)

      (default>)
      (identity nil :> *answer))
    (|origin))

  (<<query-topology topologies "locks-of-request" [*layer *request-act-id *from *to :> *locks]
    (|hash *layer)
    (record/lock-range-read *layer *request-act-id *from *to :> *read)
    (first *read :> *class)
    (<<cond
      (case> (= :streaming *class))
      (nth *read 1 :> *first-key)
      (nth *read 2 :> *last-key)
      (local-select> [(sorted-map-range *first-key *last-key {:inclusive-end? true})
                      (view record/locks-by-index)]
                     $$streaming-locks
                     :> *locks)

      (default>)
      (identity {} :> *locks))
    (|origin))

  (<<query-topology topologies "class-of-layer" [*layer :> *class]
    (|hash *layer)
    (record/class-read *layer :> *key)
    (<<if *key
      (local-select> (keypath *key) $$streaming-facts :> *row)
      (record/plain-value-of *row :> *class)
     (else>)
      (identity nil :> *class))
    (|origin)))
