;; Before changing this file, read the papers of this part again:
;; src/proposal/backend-on-rama-2026-10-02/run-again/PLAN-the-bases-gate.md
;; src/proposal/backend-on-rama-2026-10-02/run-again/PART-A.md
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md

(ns softland.store.module.walk
  "The walk of one thing: the decision of one thing that arrived on a gate's
  depot, as one Rama operation, `decide`, for both gates. It requires Rama.

  `decide` drives `softland.store.gate.walk` (`begin`, `reads`, `resume`,
  `outcome`): one loop of reads on the task, then the writes in the order
  given. The order of a decision and every check live in that pure
  namespace and under `softland.store.gate`, so the module and the pure
  in-memory record (`softland.store.record.in-memory`) cannot decide apart.

  It never gives its task back. Between the first read and the last write
  stands no partitioner, no yield, no read that may yield and no append,
  and no change to this file may add one. No function that it calls throws
  on what arrives: a refusal is data, an answer with a reason word.

  One act-id has one answer: that of the first thing that the gate decided
  under it, whether a later thing is the same act again or a different one.
  `copies-of` gives the writes that the microbatch gate copies to every
  other task (`:fact`, `:by-value`, and `:holder-permission`), in the order
  in which they were written; `:owner`, `:answer`, `:last-stamp`, `:line`,
  `:thing`, `:by-who` and `:stray` stay on the deciding task alone."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [softland.store.gate.walk :as gate-walk]))

(defn copies-of
  "The writes that a gate may copy to other tasks, in the order given:
  `:fact`, `:by-value`, and `:holder-permission`. A row that is nil says
  that the key is taken out."
  [writes]
  (into []
        (filter (fn [[which]]
                  (or (= :fact which)
                      (= :by-value which)
                      (= :holder-permission which))))
        writes))

(deframafn decide
  "Emits exactly once: what `copies-of` gives for an accept, in the order in
  which the rows were written, and an empty vector for a refusal, for a
  thing under an act-id that has an answer, and for a stray. The walk
  copies nothing itself.

  It holds no partitioner, no yield, no read that may yield and no append,
  and none may be added."
  [$$answers $$facts $$facts-by-value $$permissions-by-holder $$task $$line $$things *gate *thing]
  (gate-walk/begin *gate *thing :> *state0)
  (loop<- [*state *state0
           :> *done-state]
    (gate-walk/reads *state :> *wanted)
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
          (nth *read 1 :> *key)
          (<<cond
            (case> (= *which :answer))
            (local-select> (keypath *key) $$answers :> *val)

            (case> (= *which :fact))
            (local-select> (keypath *key) $$facts :> *val)

            (case> (= *which :owners))
            (local-select> [(keypath :owners *key) (view count)] $$task :> *val)

            (case> (= *which :last-stamp))
            (local-select> (keypath :last-stamp *key) $$task :> *val)

            (default>)
            (identity nil :> *val))
          (continue> (rest *todo) (assoc *got *read *val))))
      (gate-walk/resume *state *round-got :> *next-state)
      (continue> *next-state)))

  (get *done-state :lane-text :> *lane-text)
  (gate-walk/outcome *done-state :> *outcome)
  (get *outcome :writes :> *writes)
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

        (case> (and> (= *which :holder-permission) (nil? *row)))
        (local-transform> [(keypath *key) NONE>] $$permissions-by-holder)

        (case> (= *which :holder-permission))
        (local-transform> [(keypath *key) (termval *row)] $$permissions-by-holder)

        (case> (and> (= *which :owner) (nil? *row)))
        (local-transform> [(keypath :owners *lane-text) (set-elem *key) NONE>] $$task)

        (case> (= *which :owner))
        (local-transform> [(keypath :owners *lane-text) NONE-ELEM (termval *key)] $$task)

        (case> (= *which :answer))
        (local-transform> [(keypath *key) (termval *row)] $$answers)

        (case> (= *which :last-stamp))
        (local-transform> [(keypath :last-stamp *lane-text) (termval *row)] $$task)

        (case> (= *which :line))
        (local-transform> [(keypath *key) (termval *row)] $$line)

        (case> (= *which :thing))
        (local-transform> [(keypath *key) (termval *row)] $$things)

        (case> (= *which :by-who))
        (local-transform> [(keypath :by-who *key) (termval *row)] $$task)

        (case> (= *which :stray))
        (local-transform> [(keypath :strays *key) NONE-ELEM (termval *row)] $$task)

        (default>)
        (identity nil :> *no-row))
      (continue> (rest *writes-left))))
  (copies-of *writes :> *copies)
  (:> *copies))
