;; Before changing this file, read the plans of the two parts again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md
;; src/proposal/backend-on-rama-2026-10-02/run-again/PLAN-the-bases-gate.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-base-for-a-reader.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-read-path.md

(ns softland.store.module.door-reads
  "The query topologies of the module: the two by which the door reads an
  answer and a lane's depot, and `lane-rights`, by which the read path reads
  an asker's rights in one atomic turn on the task. It requires Rama.

    answer-by-act-id  [routing-id act-id] -> the recorded answer, or nil
    depot-of-lane     [lane] -> what the lane's depot fact says, or nil for
                      a lane that is not made
    lane-rights       [routing-id lane question asker session] ->
                      {:standing (:yes | :may-not-see | :holder | :visitor |
                                  :outside | :too-many-permissions)
                       :read-set {read-key expected-stamp-or-run-stamps ...}}

  Each query begins with `(|hash ...)` on its first argument, holds zero
  intermediate partitioners and zero yields, and ends with `(|origin)`. So
  every read inside one invocation runs in one single-threaded turn on the
  target task against committed state.

  Where a lane's depot is found. An act-id names no depot. The one home of
  a lane's depot is the lane's own fact, at `[lane :softland.store/depot]`,
  which its making act writes into the record of the gate that makes it
  (`softland.store.gate.first-act/depot-at-making`). `depot-of-lane` gives
  that fact's value to the door, which then knows where to append; and
  `answer-by-act-id` reads it in the same turn, before the answer, to know
  which gate's answers to read: one seek more on the lane's task and no
  round trip more. A thing that was appended to the depot of the gate that
  does not have its lane is refused there, in that gate's own answers,
  which this read never reads for the lane."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [softland.store.gate.first-act :as first-act]
            [softland.store.read-path.lanes :as lanes]
            [softland.store.record.rows :as record]))

(deframafn read-lane-rights
  "Drives `softland.store.read-path.lanes` (`rights-begin`, `rights-reads`,
  `rights-resume`, `rights-snapshot`) on one task with zero partitioners and
  zero yields. Holder scans are bounded at `max-holder-permissions + 1` so a
  planted flood of permissions never scans unbounded keys on the task."
  [$$facts $$permissions-by-holder *lane *question *asker *session]
  (inc lanes/max-holder-permissions :> *scan-limit)
  (lanes/rights-begin *lane *question *asker *session :> *state0)
  (loop<- [*state *state0
           :> *done-state]
    (lanes/rights-reads *state :> *wanted)
    (<<if (empty? *wanted)
      (:> *state)
     (else>)
      (loop<- [*todo *wanted
               *got {}
               :> *round-got]
        (<<if (empty? *todo)
          (:> *got)
         (else>)
          (first *todo :> *read)
          (nth *read 0 :> *which)
          (nth *read 1 :> *a)
          (<<cond
            (case> (= *which :fact))
            (local-select> (keypath *a) $$facts :> *val)

            (case> (= *which :holders))
            (nth *read 2 :> *b)
            (<<if (lanes/holder-range-agreed? *a *b)
              (local-select> [(sorted-map-range-from *a {:max-amt *scan-limit})
                              (sorted-map-range *a *b)]
                             $$permissions-by-holder :> *raw-run)
             (else>)
              (local-select> (sorted-map-range-from *a {:max-amt *scan-limit})
                             $$permissions-by-holder :> *raw-run))
            (lanes/clean-holder-run *a *b *raw-run :> *val)

            (default>)
            (identity nil :> *val))
          (continue> (rest *todo) (assoc *got *read *val))))
      (lanes/rights-resume *state *round-got :> *next-state)
      (continue> *next-state)))
  (lanes/rights-snapshot *done-state :> *snapshot)
  (:> *snapshot))

(deframafn depot-said
  "What the lane's depot fact says, read on the lane's task from the record
  of the gate that makes the lane; nil for a lane that is not made, and for
  a thing that is no usable id. A microbatch lane's facts are on every
  task, the lane's own among them."
  [$$streaming-facts $$microbatch-facts *lane]
  (record/depot-read *lane :> *key)
  (<<cond
    (case> (nil? *key))
    (identity nil :> *row)

    (case> (= :microbatch (first-act/depot-at-making *lane)))
    (local-select> (keypath *key) $$microbatch-facts :> *row)

    (default>)
    (local-select> (keypath *key) $$streaming-facts :> *row))
  (record/value-of *row :> *said)
  (:> *said))

(defn add-to
  "What must hold: the module has the two gates, which declare the PStates
  that these read, and each of its two depots of acts partitions by the
  thing's `:lane`."
  [topologies]
  (<<query-topology topologies "answer-by-act-id" [*routing-id *act-id :> *answer]
    (|hash *routing-id)
    (record/answer-read-by-routing-id *routing-id *act-id :> *key)
    (<<if (nil? *key)
      (identity nil :> *answer)
     (else>)
      (depot-said $$streaming-facts $$microbatch-facts *routing-id :> *said)
      (first-act/depot-as-said *routing-id *said :> *depot)
      (<<if (= :microbatch *depot)
        (local-select> (keypath *key) $$microbatch-answers :> *row)
       (else>)
        (local-select> (keypath *key) $$streaming-answers :> *row))
      (record/answer-of *row :> *answer))
    (|origin))

  (<<query-topology topologies "depot-of-lane" [*lane :> *depot]
    (|hash *lane)
    (depot-said $$streaming-facts $$microbatch-facts *lane :> *depot)
    (|origin))

  (<<query-topology topologies "lane-rights" [*routing-id *lane *question *asker *session :> *snapshot]
    (|hash *routing-id)
    (<<cond
      (case> (not (lanes/valid-rights-query? *routing-id *lane *question *asker *session)))
      (identity nil :> *snapshot)

      (case> (= :microbatch (first-act/depot-at-making *lane)))
      (read-lane-rights $$microbatch-facts $$microbatch-permissions-by-holder
                        *lane *question *asker *session :> *snapshot)

      (default>)
      (read-lane-rights $$streaming-facts $$streaming-permissions-by-holder
                        *lane *question *asker *session :> *snapshot))
    (|origin)))
