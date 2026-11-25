;; Before changing this file, read the plans of the two parts again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md
;; src/proposal/backend-on-rama-2026-10-02/run-again/PLAN-the-bases-gate.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-base-for-a-reader.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-read-path.md

(ns softland.store.module.door-reads
  "The two query topologies by which the door reads the module, for lanes
  of both depots. It requires Rama. A query writes nothing.

    answer-by-act-id  [routing-id act-id] -> the recorded answer, or nil
        {:verdict :accepted or :refused, :reason the word or nil,
         :stamp the stamp, :digest the digest of the decided thing as hex}.
        nil is \"nothing yet\": no decision is readable under the act-id. It
        does not mean refused. The door gave its maker the digest of what
        it sent, and compares the two hex texts, and so does not take
        another act's answer as its own.
    depot-of-lane    [lane] -> the lane's depot, or nil
        nil for a lane that is not made.

  The routing id is `softland.store.record.rows/routing-id` of the act-id:
  the act-id's lane, for both depots. The caller still needs the act-id
  alone.

  What they read. PStates that the two gates own and these borrow:
  `$$streaming-answers` and `$$streaming-facts` for a streaming lane,
  `$$microbatch-answers` and `$$microbatch-facts` for a microbatch lane. A
  query sees only committed state. Each query is one read of one key on the
  one task that it runs on: the answer's row, in the table of the act-id's
  depot; or the row at the lane's depot place, in the table of the depot
  that the lane starts with.

  Routing. Each begins with the hash partitioner on its first argument,
  which is a lane. Rama's client evaluates a leading partitioner on a
  query's own arguments, so the query goes straight to the lane's task and
  never hops. Each depot partitions by the hash of the thing's `:lane`
  (`softland.store.module`), so that task is the task of the lane's depot
  partition: where the streaming gate writes every row of a streaming
  lane, and where the microbatch gate decides a microbatch lane and
  writes its answers. The depot row of a microbatch lane is on every task,
  so on the lane's. Which record holds a lane's depot is told by the
  depot that the lane starts with
  (`softland.store.gate.first-act/depot-at-making`): no act of this build
  changes a depot, so the other record cannot hold it.

  A row that a read gave does not change. An answer's row is written once:
  a gate writes no second answer under an act-id. A lane's depot place
  takes one write. For a streaming lane a query sees a row when the
  event that wrote it is committed. For a microbatch lane a query sees a
  row when its batch is committed on the lane's task, and Rama can run a
  batch again after one task has shown its part. The second attempt makes
  every row as the first made it: a decision is made of the thing and of
  rows of the gate's own tables on the lane's task, with no clock and
  nothing of the attempt in it (`softland.store.module.walk`), and Rama
  sets those tables back before the attempt. That a retry makes the same
  tables was run on a stand-in gate (the plan of the base's gate, claim
  5); that a reader was given a row before a retry was not run. So neither
  read waits for anything after the commit that showed the row.

  The head of each query. `softland.store.record.rows`'s
  `answer-read-by-routing-id` and
  `depot-read` test what the caller gave, before any key is built from it:
  an id over the limit, a thing that is no act-id, or a routing id that is
  not the act-id's lane costs no read and gets nil. Those tests run on the
  task, after the client has hashed the first argument; a caller that may
  hold a thing that is no id tests it with the same functions before it
  calls.

  Any client of the cluster can call these two queries, and can read every
  value: in a table of facts, and in a depot of acts, where every value is
  plain. In the runtime the door
  (`softland.store.door`) is the one client of the queries. A
  guard of the record is not built (?11 is open). The PStates are not
  declared private. The read path (`softland.store.read-path`) selects and
  watches the tables of facts, and of facts by value, from outside, by
  design: it is the way by which a fact comes back out to a caller. It
  reads no answer."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [softland.store.gate.first-act :as first-act]
            [softland.store.record.rows :as record]))

(defn add-to
  "Adds the two queries to a module. Takes the module's `topologies`. What
  must hold: the module has the two gates, which declare the PStates that
  these read, and each of its two depots of acts partitions by the thing's
  `:lane`, so that a lane's answers are on the task that a query by the
  lane reaches."
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
      (record/answer-of *row :> *answer)

      (default>)
      (identity nil :> *answer))
    (|origin))

  (<<query-topology topologies "depot-of-lane" [*lane :> *depot]
    (|hash *lane)
    (record/depot-read *lane :> *key)
    (<<cond
      (case> (nil? *key))
      (identity nil :> *depot)

      (case> (= :microbatch (first-act/depot-at-making *lane)))
      (local-select> (keypath *key) $$microbatch-facts :> *row)
      (record/value-of *row :> *depot)

      (default>)
      (local-select> (keypath *key) $$streaming-facts :> *row)
      (record/value-of *row :> *depot))
    (|origin)))
