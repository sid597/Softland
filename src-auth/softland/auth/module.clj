(ns softland.auth.module
  "The Rama module `Auth`: declares its four depots (`*root-events`, `*asks`,
  `*invites`, `*enters`), six PStates (`$$root-data`, `$$live`, `$$accounts`,
  `$$claims`, `$$asks`, `$$enter-ops`), single stream topology `\"auth\"`, and
  three query topologies (`\"who-is\"`, `\"under\"`, `\"ping-task\"`).

  All writes execute inside `\"auth\"` with `{:start-from :beginning
  :retry-mode :all-after}` and return their exact contract result map via
  `(ack-return> result)`. No global depot, no global PState, no `|global` or
  `|all` partitioner, and no `TaskGlobal` exist anywhere in the module.

  Cross-key agreement (`invite`, `enter`, `answer-ask`) executes the
  model-checked single-task step functions in `softland.auth.schema` via
  `loop<-` + `|hash` + `continue>` so every cross-task hop flushes and
  replicates local PState mutations before the child event executes.

  State and effects: durable Rama depots and PStates owned by `Auth`."
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.ops :as ops]
            [softland.auth.schema :as s])
  (:import [java.util UUID]
           [softland.auth.schema
            AskEvent AnswerAskEvent BeginSignInEvent CloseSessionEvent
            EndActorEvent EndSignInEvent EnterEvent IAccountState IAskState
            IClaimState IEnterOpState ILiveEntry IRootDatum InviteEvent
            MakeActorEvent OpenSessionEvent OwnLayerMadeEvent
            ReplaceSecretEvent]))

(def ^:private source-opts
  "Depot consumption options for all four sources in stream topology `\"auth\"`."
  {:start-from :beginning :retry-mode :all-after})

(deframaop apply-root-plan!
  "Applies all `$$root-data` upserts/deletes of `plan`, and its upserts and
  deletes of the inner keys of root `*root`'s map in `$$live`, on the current
  task (which must be `H(*root)`) using flat `<<atomic` + `ops/explode` blocks
  (O(1) JVM stack depth regardless of cascade size, zero yielding). A live
  write is `[(keypath *root *k) (termval v)]` or `NONE>`: no read."
  [*root *plan *rd *live]
  (<<atomic
    (ops/explode (s/plan-rd-upserts *plan) :> [*k *v])
    (local-transform> [(keypath *k) (termval *v)] *rd))
  (<<atomic
    (ops/explode (s/plan-rd-deletes *plan) :> *k)
    (local-transform> [(keypath *k) NONE>] *rd))
  (<<atomic
    (ops/explode (s/plan-live-upserts *plan) :> [*k *v])
    (local-transform> [(keypath *root *k) (termval *v)] *live))
  (<<atomic
    (ops/explode (s/plan-live-deletes *plan) :> *k)
    (local-transform> [(keypath *root *k) NONE>] *live))
  (:>))

(deframaop take-out-on-root!
  "Executes `take-out` for `*root` at `*at` on task `H(*root)`: scans `exp:`
  up to `*at` (1 seek, 0 further reads when nothing expired), and when non-empty
  scans the root's map in `$$live` once to end expired sign-ins, cascade their
  open sessions, and close expired sessions atomically."
  [*root *at *rd *live]
  (local-select> [(sorted-map-range (s/k-exp-lo *root) (s/k-exp-hi *root *at))] *rd :> *exp-map)
  (<<if (seq *exp-map)
    (local-select> [(keypath *root) (sorted-map-range s/live-lo s/live-hi)] *live :> *live-map)
    (s/plan-take-out *root *exp-map *live-map :> *tp)
    (apply-root-plan! *root *tp *rd *live))
  (:>))

(defmodule Auth [setup topologies]
  ;; Four partitioned depots (zero global or internal depots).
  (declare-depot setup *root-events (hash-by :root-id))
  (declare-depot setup *asks (hash-by :code))
  (declare-depot setup *invites (hash-by :claim-key))
  (declare-depot setup *enters (hash-by :account-key))

  (let [st (stream-topology topologies "auth")]
    ;; Six strictly typed PStates (zero Object positions, zero byte arrays).
    (declare-pstate st $$root-data {String IRootDatum})
    ;; $$live: under each root, a subindexed map from an inner key ("g:<G>",
    ;; "s:<S>") to what is live there. A watch of a root is a proxy on that
    ;; root's map alone (softland.auth.client/watch-root), so a write is
    ;; matched against the watches of its own root and no other. Its size is
    ;; never read, so it is not tracked: no extra read on each write.
    (declare-pstate st $$live {UUID (map-schema String ILiveEntry {:subindex-options {:track-size? false}})})
    (declare-pstate st $$accounts {String IAccountState})
    (declare-pstate st $$claims {String IClaimState})
    (declare-pstate st $$asks {String IAskState})
    (declare-pstate st $$enter-ops {UUID IEnterOpState})

    ;; Allow 20,000 watched roots per task on $$live (SPEC.md): one proxy
    ;; for each root that a client watches.
    (set-launch-pstate-dynamic-option! setup "$$live" "pstate.proxy.limit" 20000)

    (<<sources st
      ;; ====================================================================
      ;; 1. *invites -> invite (cross-key H(C) <-> H(P), Revision 1)
      ;; ====================================================================
      (source> *invites source-opts :> *ev)
      (loop<- [*step :invite/step-1
               *st {}
               :> *result]
        (<<cond
          (case> (= *step :invite/step-1))
          (get *ev :claim-key :> *ck)
          (local-select> [(keypath *ck)] $$claims :> *c)
          (s/invite-step-1 *ev *c :> *act)
          (<<if (s/act-done? *act)
            (:> (s/act-result *act))
           (else>)
            (|hash (s/act-hop-key *act))
            (continue> (s/act-next-step *act) (s/act-next-st *act)))

          (case> (= *step :invite/step-2))
          (s/k-root (get *ev :person) :> *rk)
          (local-select> [(keypath *rk)] $$root-data :> *r)
          (s/invite-step-2 *ev *r :> *act)
          (s/act-write-k *act :> *wk)
          (<<if (some? *wk)
            (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$root-data))
          (<<if (s/act-done? *act)
            (:> (s/act-result *act))
           (else>)
            (|hash (s/act-hop-key *act))
            (continue> (s/act-next-step *act) (s/act-next-st *act)))

          (case> (= *step :invite/step-2-help-claim))
          (get *st :help-claim :> *hc)
          (local-select> [(keypath *hc)] $$claims :> *c0)
          (s/invite-step-2-help-claim *ev *st *c0 :> *act)
          (s/act-write-k *act :> *wk)
          (<<if (some? *wk)
            (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$claims))
          (|hash (s/act-hop-key *act))
          (continue> (s/act-next-step *act) (s/act-next-st *act))

          (case> (= *step :invite/step-2-help-root))
          (s/k-root (get *ev :person) :> *rk)
          (local-select> [(keypath *rk)] $$root-data :> *r)
          (s/invite-step-2-help-root *ev *st *r :> *act)
          (s/act-write-k *act :> *wk)
          (<<if (some? *wk)
            (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$root-data))
          (<<if (s/act-done? *act)
            (:> (s/act-result *act))
           (else>)
            (|hash (s/act-hop-key *act))
            (continue> (s/act-next-step *act) (s/act-next-st *act)))

          (case> (= *step :invite/step-2-recheck))
          (get *ev :claim-key :> *ck)
          (local-select> [(keypath *ck)] $$claims :> *c)
          (s/invite-step-2-recheck *ev *c :> *act)
          (<<if (s/act-done? *act)
            (:> (s/act-result *act))
           (else>)
            (|hash (s/act-hop-key *act))
            (continue> (s/act-next-step *act) (s/act-next-st *act)))

          (case> (= *step :invite/step-3))
          (get *ev :claim-key :> *ck)
          (local-select> [(keypath *ck)] $$claims :> *c)
          (s/invite-step-3 *ev *c :> *act)
          (s/act-write-k *act :> *wk)
          (<<if (some? *wk)
            (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$claims))
          (|hash (s/act-hop-key *act))
          (continue> (s/act-next-step *act) (s/act-next-st *act))

          (case> (= *step :invite/step-3-clear))
          (s/k-root (get *ev :person) :> *rk)
          (local-select> [(keypath *rk)] $$root-data :> *r)
          (s/invite-step-3-clear *ev *r :> *act)
          (s/act-write-k *act :> *wk)
          (<<if (some? *wk)
            (local-transform> [(keypath *wk) NONE>] $$root-data))
          (:> (s/act-result *act))

          (case> (= *step :invite/step-4))
          (s/k-root (get *ev :person) :> *rk)
          (local-select> [(keypath *rk)] $$root-data :> *r)
          (s/invite-step-4 *ev *r :> *act)
          (s/act-write-k *act :> *wk)
          (<<if (some? *wk)
            (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$root-data))
          (:> (s/act-result *act))))
      (ack-return> *result)

      ;; ====================================================================
      ;; 2. *enters -> enter (uniform choose-at-account, Revisions 1 & 5)
      ;; ====================================================================
      (source> *enters source-opts :> *ev)
      (loop<- [*step :enter/step-1
               *st {}
               :> *result]
        (<<cond
          (case> (= *step :enter/step-1))
          (get *ev :account-key :> *ak)
          (local-select> [(keypath *ak)] $$accounts :> *acc)
          (s/enter-step-1 *ev *acc :> *act)
          (s/act-write-k *act :> *wk)
          (<<if (some? *wk)
            (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$accounts))
          (<<if (s/act-done? *act)
            (:> (s/act-result *act))
           (else>)
            (|hash (s/act-hop-key *act))
            (continue> (s/act-next-step *act) (s/act-next-st *act)))

          (case> (= *step :enter/step-1a-check-op-returning))
          (get *ev :op-id :> *op)
          (local-select> [(keypath *op)] $$enter-ops :> *eo)
          (s/enter-step-1a-check-op-returning *ev *st *eo :> *act)
          (<<if (s/act-done? *act)
            (:> (s/act-result *act))
           (else>)
            (|hash (s/act-hop-key *act))
            (continue> (s/act-next-step *act) (s/act-next-st *act)))

          (case> (= *step :enter/step-1c-update-says))
          (get *ev :account-key :> *ak)
          (local-select> [(keypath *ak)] $$accounts :> *acc)
          (s/enter-step-1c-update-says *ev *st *acc :> *act)
          (s/act-write-k *act :> *wk)
          (<<if (some? *wk)
            (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$accounts))
          (|hash (s/act-hop-key *act))
          (continue> (s/act-next-step *act) (s/act-next-st *act))

          (case> (= *step :enter/step-1b-read-flag))
          (s/k-root (get *st :linked-person) :> *rk)
          (local-select> [(keypath *rk)] $$root-data :> *r)
          (s/enter-step-1b-read-flag *st *r :> *act)
          (:> (s/act-result *act))

          (case> (= *step :enter/step-2-probe))
          (get *st :curr-claim :> *ck)
          (local-select> [(keypath *ck)] $$claims :> *c)
          (s/enter-step-2-probe *ev *st *c :> *act)
          (|hash (s/act-hop-key *act))
          (continue> (s/act-next-step *act) (s/act-next-st *act))

          (case> (= *step :enter/step-2b-choose))
          (get *ev :account-key :> *ak)
          (local-select> [(keypath *ak)] $$accounts :> *acc)
          (s/enter-step-2b-choose *ev *st *acc :> *act)
          (s/act-write-k *act :> *wk)
          (<<if (some? *wk)
            (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$accounts))
          (<<if (s/act-done? *act)
            (:> (s/act-result *act))
           (else>)
            (|hash (s/act-hop-key *act))
            (continue> (s/act-next-step *act) (s/act-next-st *act)))

          (case> (= *step :enter/step-2c-confirm-choice))
          (get (get *st :target) :op-id :> *op)
          (local-select> [(keypath *op)] $$enter-ops :> *eo)
          (s/enter-step-2c-confirm-choice *ev *st *eo :> *act)
          (|hash (s/act-hop-key *act))
          (continue> (s/act-next-step *act) (s/act-next-st *act))

          (case> (= *step :enter/step-2c-mark-failed))
          (get (get *st :target) :op-id :> *op)
          (local-select> [(keypath *op)] $$enter-ops :> *eo)
          (s/enter-step-2c-mark-failed *ev *st *eo :> *act)
          (s/act-write-k *act :> *wk)
          (<<if (some? *wk)
            (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$enter-ops))
          (|hash (s/act-hop-key *act))
          (continue> (s/act-next-step *act) (s/act-next-st *act))

          (case> (= *step :enter/step-2c-mark-failed-direct))
          (get *ev :op-id :> *op)
          (local-select> [(keypath *op)] $$enter-ops :> *eo)
          (s/enter-step-2c-mark-failed-direct *ev *eo :> *act)
          (s/act-write-k *act :> *wk)
          (<<if (some? *wk)
            (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$enter-ops))
          (:> (s/act-result *act))

          (case> (= *step :enter/step-2d-finish-failed))
          (get *ev :account-key :> *ak)
          (local-select> [(keypath *ak)] $$accounts :> *acc)
          (s/enter-step-2d-finish-failed *ev *st *acc :> *act)
          (s/act-write-k *act :> *wk)
          (<<if (some? *wk)
            (local-transform> [(keypath *wk) NONE>] $$accounts))
          (<<if (s/act-done? *act)
            (:> (s/act-result *act))
           (else>)
            (continue> (s/act-next-step *act) (s/act-next-st *act)))

          (case> (= *step :enter/step-3-use))
          (get *st :curr-claim :> *ck)
          (local-select> [(keypath *ck)] $$claims :> *c)
          (s/enter-step-3-use *ev *st *c :> *act)
          (s/act-write-k *act :> *wk)
          (<<if (some? *wk)
            (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$claims))
          (|hash (s/act-hop-key *act))
          (continue> (s/act-next-step *act) (s/act-next-st *act))

          (case> (= *step :enter/step-3b-clear-chosen))
          (get *ev :account-key :> *ak)
          (local-select> [(keypath *ak)] $$accounts :> *acc)
          (s/enter-step-3b-clear-chosen *ev *st *acc :> *act)
          (s/act-write-k *act :> *wk)
          (<<if (some? *wk)
            (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$accounts))
          (continue> (s/act-next-step *act) (s/act-next-st *act))

          (case> (= *step :enter/step-4-root))
          (get (get *st :target) :won-person :> *P)
          (s/k-root *P :> *rk)
          (local-select> [(keypath *rk)] $$root-data :> *r)
          (s/enter-step-4-root *ev *st *r :> *act)
          (s/act-write-k *act :> *wk)
          (<<if (some? *wk)
            (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$root-data))
          (|hash (s/act-hop-key *act))
          (continue> (s/act-next-step *act) (s/act-next-st *act))

          (case> (= *step :enter/step-5-link))
          (get *ev :account-key :> *ak)
          (local-select> [(keypath *ak)] $$accounts :> *acc)
          (s/enter-step-5-link *ev *st *acc :> *act)
          (s/act-write-k *act :> *wk)
          (<<if (some? *wk)
            (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$accounts))
          (<<if (s/act-done? *act)
            (:> (s/act-result *act))
           (else>)
            (|hash (s/act-hop-key *act))
            (continue> (s/act-next-step *act) (s/act-next-st *act)))))
      (ack-return> *result)

      ;; ====================================================================
      ;; 3. *root-events -> single-task writes on H(P) (0 hops, atomic batch)
      ;; ====================================================================
      (source> *root-events source-opts :> *ev)
      (<<subsource *ev
        (case> OwnLayerMadeEvent)
        (s/k-root (get *ev :root-id) :> *rk)
        (local-select> [(keypath *rk)] $$root-data :> *r)
        (s/own-layer-made-step *ev *r :> *act)
        (s/act-write-k *act :> *wk)
        (<<if (some? *wk)
          (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$root-data))
        (ack-return> (s/act-result *act))

        (case> BeginSignInEvent)
        (get *ev :root-id :> *P)
        (get *ev :actor :> *A)
        (get *ev :sign-in :> *G)
        (take-out-on-root! *P (get *ev :at) $$root-data $$live)
        (local-select> [(keypath (s/k-root *P))] $$root-data :> *r)
        (local-select> [(keypath (s/k-signin *P *G))] $$root-data :> *g)
        (<<if (not= *A *P)
          (local-select> [(keypath (s/k-actor *P *A))] $$root-data :> *arec)
         (else>)
          (identity nil :> *arec))
        (s/plan-begin-sign-in *ev *r *g *arec :> *plan)
        (apply-root-plan! *P *plan $$root-data $$live)
        (ack-return> (s/plan-result *plan))

        (case> ReplaceSecretEvent)
        (get *ev :root-id :> *P)
        (get *ev :sign-in :> *G)
        (take-out-on-root! *P (get *ev :at) $$root-data $$live)
        (local-select> [(keypath (s/k-signin *P *G))] $$root-data :> *g)
        (<<if (s/replace-secret-born-ended? *ev *g)
          (local-select> [(keypath *P) (sorted-map-range s/live-session-lo s/live-session-hi)] $$live :> *live-ss)
         (else>)
          (identity nil :> *live-ss))
        (s/plan-replace-secret *ev *g *live-ss :> *plan)
        (apply-root-plan! *P *plan $$root-data $$live)
        (ack-return> (s/plan-result *plan))

        (case> EndSignInEvent)
        (get *ev :root-id :> *P)
        (get *ev :sign-in :> *G)
        (take-out-on-root! *P (get *ev :at) $$root-data $$live)
        (local-select> [(keypath (s/k-signin *P *G))] $$root-data :> *g)
        (<<if (s/signin-needs-end? *g)
          (local-select> [(keypath *P) (sorted-map-range s/live-session-lo s/live-session-hi)] $$live :> *live-ss)
         (else>)
          (identity nil :> *live-ss))
        (s/plan-end-sign-in *ev *g *live-ss :> *plan)
        (apply-root-plan! *P *plan $$root-data $$live)
        (ack-return> (s/plan-result *plan))

        (case> MakeActorEvent)
        (get *ev :root-id :> *P)
        (get *ev :actor :> *A)
        (get *ev :made-by :> *B)
        (take-out-on-root! *P (get *ev :at) $$root-data $$live)
        (local-select> [(keypath (s/k-root *P))] $$root-data :> *r)
        (local-select> [(keypath (s/k-actor *P *A))] $$root-data :> *existing-a)
        (<<if (not= *B *P)
          (local-select> [(keypath (s/k-actor *P *B))] $$root-data :> *maker-rec)
         (else>)
          (identity nil :> *maker-rec))
        (s/plan-make-actor *ev *r *existing-a *maker-rec :> *plan)
        (apply-root-plan! *P *plan $$root-data $$live)
        (ack-return> (s/plan-result *plan))

        (case> EndActorEvent)
        (get *ev :root-id :> *P)
        (get *ev :actor :> *A)
        (take-out-on-root! *P (get *ev :at) $$root-data $$live)
        (local-select> [(keypath (s/k-root *P))] $$root-data :> *r)
        (<<if (not= *A *P)
          (local-select> [(keypath (s/k-actor *P *A))] $$root-data :> *a)
         (else>)
          (identity nil :> *a))
        (<<if (s/actor-needs-end? *P *A *r *a)
          (local-select> [(sorted-map-range (s/k-ao *P (get *a :seq)) (s/k-ao-hi *P))] $$root-data :> *ao-map)
          (local-select> [(keypath *P) (sorted-map-range s/live-lo s/live-hi)] $$live :> *live-map)
          (s/plan-end-actor-2scan *P *a *ao-map *live-map :> *plan)
         (else>)
          (s/plan-end-actor-noop *ev *r *a :> *plan))
        (apply-root-plan! *P *plan $$root-data $$live)
        (ack-return> (s/plan-result *plan))

        (case> OpenSessionEvent)
        (get *ev :root-id :> *P)
        (get *ev :sign-in :> *G)
        (get *ev :session :> *S)
        (take-out-on-root! *P (get *ev :at) $$root-data $$live)
        (local-select> [(keypath (s/k-session *P *S))] $$root-data :> *s)
        (local-select> [(keypath (s/k-signin *P *G))] $$root-data :> *g)
        (s/plan-open-session *ev *s *g :> *plan)
        (apply-root-plan! *P *plan $$root-data $$live)
        (ack-return> (s/plan-result *plan))

        (case> CloseSessionEvent)
        (get *ev :root-id :> *P)
        (get *ev :session :> *S)
        (take-out-on-root! *P (get *ev :at) $$root-data $$live)
        (local-select> [(keypath (s/k-session *P *S))] $$root-data :> *s)
        (s/plan-close-session *ev *s :> *plan)
        (apply-root-plan! *P *plan $$root-data $$live)
        (ack-return> (s/plan-result *plan)))

      ;; ====================================================================
      ;; 4. *asks -> ask (0 hops) & answer-ask (H(U) <-> H(P), Revision 4)
      ;; ====================================================================
      (source> *asks source-opts :> *ev)
      (<<subsource *ev
        (case> AskEvent)
        (get *ev :code :> *U)
        (local-select> [(keypath *U)] $$asks :> *k)
        (s/ask-step *ev *k :> *act)
        (s/act-write-k *act :> *wk)
        (<<if (some? *wk)
          (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$asks))
        (ack-return> (s/act-result *act))

        (case> AnswerAskEvent)
        (loop<- [*step :answer-ask/step-1
                 *st {}
                 :> *result]
          (<<cond
            (case> (= *step :answer-ask/step-1))
            (get *ev :code :> *U)
            (local-select> [(keypath *U)] $$asks :> *k)
            (s/answer-ask-step-1 *ev *k :> *act)
            (s/act-write-k *act :> *wk)
            (<<if (some? *wk)
              (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$asks))
            (|hash (s/act-hop-key *act))
            (continue> (s/act-next-step *act) (s/act-next-st *act))

            (case> (= *step :answer-ask/step-1b))
            (get *st :takeout-root :> *P)
            (get *st :takeout-at :> *at)
            (<<if (get *st :no-such-ask?)
              (s/k-ok *P (get *ev :op-id) :> *ok-k)
              (local-select> [(keypath *ok-k)] $$root-data :> *existing-ok)
              (<<if (nil? *existing-ok)
                (take-out-on-root! *P *at $$root-data $$live)
                (local-transform> [(keypath *ok-k) (termval (s/no-such-ask-outcome))] $$root-data))
             (else>)
              (take-out-on-root! *P *at $$root-data $$live))
            (:> (get *st :pending-result))

            (case> (= *step :answer-ask/step-2))
            (get *st :target :> *t)
            (get *t :root :> *P)
            (get *t :op-id :> *op)
            ;; Revision 4: check ok:<op> BEFORE take-out so replays never re-run take-out!
            (local-select> [(keypath (s/k-ok *P *op))] $$root-data :> *existing-ok)
            (<<if (some? *existing-ok)
              (identity *existing-ok :> *out)
             (else>)
              (take-out-on-root! *P (get *t :at) $$root-data $$live)
              (local-select> [(keypath (s/k-root *P))] $$root-data :> *r)
              (get *t :actor :> *A)
              (<<if (not= *A *P)
                (local-select> [(keypath (s/k-actor *P *A))] $$root-data :> *arec)
               (else>)
                (identity nil :> *arec))
              (local-select> [(keypath (s/k-signin *P (get *t :sign-in)))] $$root-data :> *g)
              (s/answer-ask-eval-root (get *ev :code) *t *r *arec *g :> *plan)
              (apply-root-plan! *P *plan $$root-data $$live)
              (s/plan-outcome *plan :> *out))
            (|hash (get *ev :code))
            (continue> :answer-ask/step-3 (assoc *st :outcome *out))

            (case> (= *step :answer-ask/step-3))
            (get *ev :code :> *U)
            (local-select> [(keypath *U)] $$asks :> *k)
            (s/answer-ask-step-3 *ev *st *k :> *act)
            (s/act-write-k *act :> *wk)
            (<<if (some? *wk)
              (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$asks))
            (<<if (s/act-done? *act)
              (:> (s/act-result *act))
             (else>)
              (s/act-hop-key *act :> *hk)
              (<<if (some? *hk)
                (|hash *hk)
                (continue> (s/act-next-step *act) (s/act-next-st *act))
               (else>)
                (continue> (s/act-next-step *act) (s/act-next-st *act))))))
        (ack-return> *result))))

  ;; ========================================================================
  ;; Query Topology 1: "who-is" (1 RT to 1 task, Revision 3: 2-seek fast path)
  ;; ========================================================================
  (<<query-topology topologies "who-is"
    [*root *sign-in *session :> *result]
    (|hash *root)
    (local-select> [(keypath (s/k-signin *root *sign-in))] $$root-data :> *sig)
    (<<if (s/signin-fast-path? *sig)
      (identity nil :> *r)
      (identity true :> *entered?)
     (else>)
      (local-select> [(keypath (s/k-root *root))] $$root-data :> *r)
      (s/root-entered? *r :> *entered?))
    (<<if (and> *entered? (some? *session))
      (local-select> [(keypath (s/k-session *root *session))] $$root-data :> *sess)
     (else>)
      (identity nil :> *sess))
    (<<if (and> *entered? (s/need-actor-seek? *root *sig))
      (local-select> [(keypath (s/k-actor *root (s/signin-actor *sig)))] $$root-data :> *actor-rec)
     (else>)
      (identity nil :> *actor-rec))
    (s/build-who-is-result *root *sig *r *sess *actor-rec :> *result)
    (|origin))

  ;; ========================================================================
  ;; Query Topology 2: "under" (1 RT to 1 task, live sign-ins only from $$live)
  ;; ========================================================================
  (<<query-topology topologies "under"
    [*root :> *result]
    (|hash *root)
    (local-select> [(keypath (s/k-root *root))] $$root-data :> *r)
    (<<if (s/root-has-actors? *r)
      (local-select> [(sorted-map-range (s/k-ao-lo *root) (s/k-ao-hi *root))]
                     $$root-data
                     {:allow-yield? true}
                     :> *ao-map)
     (else>)
      (identity nil :> *ao-map))
    (<<if (s/root-has-signins? *r)
      (local-select> [(keypath *root) (sorted-map-range s/live-signin-lo s/live-signin-hi)]
                     $$live
                     :> *live-g-map)
     (else>)
      (identity nil :> *live-g-map))
    (s/build-under-result *ao-map *live-g-map :> *result)
    (|origin))

  ;; ========================================================================
  ;; Query Topology 3: "ping-task" (0 seeks, |direct liveness probe)
  ;; ========================================================================
  (<<query-topology topologies "ping-task"
    [*task-id :> *ok]
    (|direct *task-id)
    (ops/current-task-id :> *tid)
    (= *tid *task-id :> *ok)
    (|origin)))
