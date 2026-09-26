;; IMPORTANT: Before modifying this file, re-read PLAN-stream-store.md (the
;; one event), PLAN-locks-and-forgetting.md (the gate event, steps 3 to 11)
;; and PLAN-read-exit.md (the index writes in the decision's group).
(ns rig.store.gate-event
  "The stream gate's one event after intake, as two ops, moved verbatim
  from rig.store.module's `<<sources` body (wave 2, PLAN-promotion.md), so
  that every act this gate decides goes through one path:

  - `record-or-decide>`: the name's record first, then the record path or
    the fresh decision and its writes, all on the layer's home task, with
    no partitioner (RQ 1);
  - `write-decided>`: the writes of a decided act, in the decision's one
    atomic group.

  The module's flow calls `record-or-decide>` for every record of
  `*offers` that passed intake. The store's own steps call the same ops
  (stage 4): a promotion's crossing is written by `write-decided>`, and a
  landing into a layer this gate orders is decided by `record-or-decide>`
  on that layer's home.

  Neither op returns through the ack or fans out a person act: the caller
  does both, so a step the store takes inside another record's event tree
  never overwrites that record's answer.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [com.rpl.rama :as rama :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :refer :all]
            [rig.store.gate :as gate]
            [rig.store.inject :as inject]
            [rig.store.locks :as locks]
            [rig.store.reads :as reads]))

(deframaop write-decided>
  "The writes of a decided act (`gate/decide`'s `:kind :decide`), in the
  decision's one atomic group on the layer's home: the answer record; for
  a yes the act's rows, what it stood on, its heads, its index entries
  (stage 5a), and the settings and permission rows as they stand after
  it; stage 2's lock writes, yes or no; a value forget's purge of the read
  exit's indexes, dated by its stamp; and the task's clock. The crash
  hook's `:before-writes` and `:after-writes` points bracket it. Every
  write is a set of a value computed before any write, so a replay that
  reaches here writes the same rows (I-G2). Emits once."
  [*layer *offer *name *d]
  (<<with-substitutions [$$layers (rama/this-module-pobject-task-global "$$layers")
                         $$clock (rama/this-module-pobject-task-global "$$clock")]
    (inject/point! :before-writes *name)
    (get *d :record :> *record)
    (local-transform> [(keypath *layer :answers *name) (termval *record)] $$layers)
    (<<if (= :yes (get *record :answer))
      ;; the act's rows, one whole-vector write into the subindexed vector (F1, F14)
      (local-transform> [(keypath *layer :log *name) (termval (get *d :log))] $$layers)
      ;; what the act stood on, one set per carried entry (F2)
      (<<atomic
        (ops/explode-map (get *d :stood-on) :> *sf *ss)
        (local-transform> [(keypath *layer :stood-on *name *sf) (termval *ss)] $$layers))
      (<<atomic
        (ops/explode (get *d :heads-del) :> *hk)
        (local-transform> [(keypath *layer :heads *hk) NONE>] $$layers))
      (<<atomic
        (ops/explode (get *d :heads-put) :> [*hk *hs])
        (local-transform> [(keypath *layer :heads *hk) (termval *hs)] $$layers))
      ;; stage 5a: the act's index entries, computed in decide (reads/index-writes);
      ;; the same three blocks apply a purge's and a rebuild's lists
      (<<atomic
        (ops/explode (get *d :index-put) :> [*ix *ia *ie])
        (local-transform> [(keypath *layer *ix *ia) (termval *ie)] $$layers))
      (<<atomic
        (ops/explode (get *d :index-of) :> [*ofid *ias])
        (local-transform> [(keypath *layer :ix-of *ofid) (termval *ias)] $$layers))
      (<<atomic
        (ops/explode (get *d :index-del) :> [*dx *da])
        (local-transform> [(keypath *layer *dx *da) NONE>] $$layers))
      ;; settings and permission rows as they stand after the act; both were
      ;; read in this event, so each is one write with no read
      (get *d :settings :> *new-settings)
      (<<if (some? *new-settings)
        (local-transform> [(keypath *layer :settings) (termval *new-settings)] $$layers))
      (<<atomic
        (ops/explode (get *d :permissions) :> [*pp *prow])
        (local-transform> [(keypath *layer :permissions *pp) (termval *prow)] $$layers))
      ;; phase 6: the key rows the act changed, each whole (a grammar, a first use)
      (<<atomic
        (ops/explode (get *d :key-rows) :> [*kk *krow])
        (local-transform> [(keypath *layer :key-rows *kk) (termval *krow)] $$layers)))
    ;; stage 2's lock writes, yes or no, in the same group
    (locks/write-decision> *layer *offer *d)
    ;; a value forget purges the read exit's indexes of every value its lock
    ;; erased, in the same group, dated by its stamp (the ledger's date)
    (get (get *d :locks) :purge :> *purge)
    (<<if (seq *purge)
      (reads/purge> *layer *purge (get *d :stamp)))
    (local-transform> [(termval (get *d :stamp))] $$clock)
    (inject/point! :after-writes *name)
    (:>)))

(deframaop record-or-decide>
  "The name's record, then the record path or the fresh decision, on the
  layer's home task with no partitioner, so every read sees this task's
  state and every write commits in one group (RQ 1). `*in` is the
  intake's map (`gate/intake`): its `:pids` and `:heads` name the
  permission rows and heads rows the decision reads. Emits once `[ack
  fan-out]`: the answer the ack carries, and the person a person act fans
  out to (nil for any other act), which the caller hands to
  `locks/fan-out>` after it sets the answer.
  - decided before (F4: the record first): the recorded answer, or the
    name is taken; the one write is the consumption of the cited lease rows
    (stage 2, V-F1); a recorded person act fans out again (L9);
  - a cited lock not delivered: refused on its face, nothing recorded,
    consumed or written (stage 2's delivery);
  - else the settings, clock, wall, permission rows and heads rows
    (`loop<-`, never explode: an act with no replaces must still reach the
    decision, F7), the lock reads and fresh draw (stage 2), `gate/decide`
    (total), and its writes (`write-decided>`)."
  [*layer *offer *name *digest *in]
  (<<with-substitutions [$$layers (rama/this-module-pobject-task-global "$$layers")
                         $$clock (rama/this-module-pobject-task-global "$$clock")]
    (local-select> (keypath *layer :answers *name) $$layers :> *rec)
    (<<if (some? *rec)
      ;; decided before: the recorded answer, or the name is taken; the one
      ;; write is the consumption of the cited lease rows (stage 2, V-F1)
      (gate/answer-from-record *rec *digest *name :> *d0)
      (locks/record-path> *layer *offer *rec *d0 :> *d)
      (inject/point! :recorded *name)
      (locks/consume-locks> *layer (get *offer :session) (get *d :consume))
      (:> (get *d :ack) (get *d :fan-out))
     (else>)
      (local-select> (keypath *layer :settings) $$layers :> *settings)
      ;; stage 2: the delivery; a missing lock is refused on its face
      (locks/deliver-all> *layer *offer *settings :> *lk)
      (<<if (get *lk :missing?)
        (:> (gate/no-such-lock-ack *name) nil)
       (else>)
        (local-select> STAY $$clock :> *clock)
        (gate/wall-now :> *wall)
        (get *in :pids :> *pids)
        (loop<- [*todo *pids *acc {} :> *rows]
          (<<if (empty? *todo)
            (:> *acc)
           (else>)
            (first *todo :> *pid)
            (local-select> (keypath *layer :permissions *pid) $$layers :> *row)
            (continue> (rest *todo) (assoc *acc *pid *row))))
        (get *in :heads :> *hkeys)
        (loop<- [*todo *hkeys *acc {} :> *heads]
          (<<if (empty? *todo)
            (:> *acc)
           (else>)
            (first *todo :> *hk)
            (local-select> (keypath *layer :heads *hk) $$layers :> *hstamp)
            (continue> (rest *todo) (assoc *acc *hk *hstamp))))
        ;; phase 6: the rows of the act's keys and of the keys its grammar facts
        ;; govern, before the value checks that take their grammars (4.7 step 2)
        (get *in :keys :> *kkeys)
        (loop<- [*todo *kkeys *acc {} :> *krows]
          (<<if (empty? *todo)
            (:> *acc)
           (else>)
            (first *todo :> *kk)
            (local-select> (keypath *layer :key-rows *kk) $$layers :> *krow)
            (continue> (rest *todo) (assoc *acc *kk *krow))))
        (locks/decision-reads> *layer *offer *settings *lk *krows :> *lx)
        (gate/decide *offer *settings *rows *heads *clock *wall *digest *lx :> *d)
        (<<if (= :decide (get *d :kind))
          (write-decided> *layer *offer *name *d))
        (:> (get *d :ack) (get (get *d :locks) :fan-out))))))
