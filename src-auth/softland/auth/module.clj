(ns softland.auth.module
  "The Rama module `Auth`. One stream topology, `\"auth\"`, decides every
  write and acks its contract result. Four query topologies serve
  `who-is`, `under`, `accounts-of` and the client's probe of a task;
  `ask-of` and `invitation-of` read one record each, by the client. The module has no global depot, no global PState, no `|global` or
  `|all` partitioner and no `TaskGlobal`.

  Each depot's dataflow first holds the record against the form that the
  client gives it (`softland.auth.schema/invite-record-fits?` and the like):
  a record that another appender wrote, of another type or with a field of
  another type, is acked with `{:could-not :read-the-event}` and changes
  nothing, so that it can never throw in a step and, under `:all-after`,
  hold its partition.

  `invite` and `take-back` are one step each at the claim's task, with no
  hop. `enter`, `unlink` and `answer-ask` run the pure single-task step
  functions of `softland.auth.schema` via `loop<-` + `|hash` + `continue>`,
  so every cross-task hop flushes and replicates local PState mutations
  before the child event executes.

  What holds of those steps, and what does not. The protocol of `enter` and
  `answer-ask` was model-checked as another writing of it, not as these
  functions: carbon's checker
  (`src/proposal/runtime-2026-11-21/overnight-audit/auth-concurrent`) ran
  `spine/auth/agreement/src/auth_concurrent/plan_steps.clj`, under faults and
  retries of every mode. Since 28 November the steps of `invite`,
  `take-back`, `enter` and `unlink` are new (a claim that comes back, an
  invitation that ends, several accounts for one person, a link that is
  removed), and no model check has run them: not the helping steps of
  `enter` with claims that come back, not the round of a linking, not the
  release of step 5, not `unlink`. What holds them: the generated histories
  of `softland.auth.module-check` through this module against the contract's
  reference, on a healthy cluster, where on this module no hop has failed and
  no record has been replayed in any run; the concurrent scenarios of
  `softland.auth.module-harness` (`invitations`), each of which allows only
  the outcomes that some order of its two writes gives; carbon's scenarios
  of invites and enters (`step3`), brought to the contract; and, for a
  record that runs again, the steps
  called by hand in the orders of events that each rule is for
  (`softland.auth.module-harness`, `reruns`), which holds the steps and not
  this module."
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.ops :as ops]
            [softland.auth.schema :as s])
  (:import [java.util UUID]
           [softland.auth.schema
            AskEvent AnswerAskEvent BeginSignInEvent CloseSessionEvent
            EndActorEvent EndSignInEvent EnterEvent IAccountState IAskState
            IClaimState IEnterOpState IInviteOpState ILiveEntry IRootDatum InviteEvent
            MakeActorEvent OpenSessionEvent OwnLaneMadeEvent
            ReplaceSecretEvent TakeBackEvent UnlinkEvent]))

(def ^:private source-opts
  {:start-from :beginning :retry-mode :all-after})

(deframaop apply-root-plan!
  "The task must be `H(*root)`. Flat `<<atomic` + `ops/explode` blocks: O(1)
  JVM stack depth regardless of cascade size, and no yield. A live write is
  `[(keypath *root *k) (termval v)]` or `NONE>`: no read."
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
  ;; In the event of the end itself, so a record that is tried again finds
  ;; the thing ended and counts nothing.
  (<<if (s/plan-ended-something? *plan)
    (local-transform> [(keypath *root s/lk-ends) (term s/bump-ends)] *live))
  (:>))

(deframaop thin-watched-on-root!
  "The task must be `H(*root)`. One seek of `exp:` up to `*at`, and no
  further read: what `$$live` holds of a thing is named by its `exp:`
  entry."
  [*root *at *rd *live]
  (local-select> [(sorted-map-range (s/k-exp-lo *root) (s/k-exp-hi *root *at))] *rd :> *exp-map)
  (<<if (seq *exp-map)
    (s/plan-thin-watched *exp-map :> *tp)
    (apply-root-plan! *root *tp *rd *live))
  (:>))

(deframaop signin-agent-rec
  "The task must be `H(*root)`. No read for a sign-in of the person herself,
  nor for what is no sign-in."
  [*root *g *rd]
  (s/signin-agent *root *g :> *agent)
  (<<if (some? *agent)
    (local-select> [(keypath (s/k-actor *root *agent))] *rd :> *arec)
   (else>)
    (identity nil :> *arec))
  (:> *arec))

(deframaop write-act!
  "The task must be the one of the step's table. Writes `s/act-writes` of
  `*act` to `*p`: each pair, a nil value removing its key; no read."
  [*act *p]
  (<<atomic
    (ops/explode (s/act-writes *act) :> [*k *v])
    (<<if (some? *v)
      (local-transform> [(keypath *k) (termval *v)] *p)
     (else>)
      (local-transform> [(keypath *k) NONE>] *p)))
  (:>))

(defn- invites-source!
  [st]
  (<<sources st
    ;; ====================================================================
    ;; 1. *invites -> invite and take-back: one step each on H(C), no hop
    ;; ====================================================================
    (source> *invites source-opts :> *ev)
    (<<if (s/invite-record-fits? *ev)
      (<<subsource *ev
        (case> InviteEvent)
        (get *ev :claim-key :> *ck)
        (get *ev :op-id :> *op)
        (local-select> [(keypath *ck)] $$claims :> *c)
        (local-select> [(keypath *op)] $$invite-ops :> *done)
        (s/invite-step *ev *c *done :> *act)
        (write-act! *act $$claims)
        (s/act-op-v *act :> *ov)
        (<<if (some? *ov)
          (local-transform> [(keypath *op) (termval *ov)] $$invite-ops))
        (ack-return> (s/act-result *act))

        (case> TakeBackEvent)
        (get *ev :claim-key :> *ck)
        (local-select> [(keypath *ck)] $$claims :> *c)
        (s/take-back-step *ev *c :> *act)
        (write-act! *act $$claims)
        (ack-return> (s/act-result *act)))
     (else>)
      (ack-return> s/does-not-fit))))

(defmacro ^:private hop-or-done
  "The end of a step of a chain: its result, or the next step, on the task
  of the act's hop key when it has one, else on this task."
  [act]
  (list '<<if (list 's/act-done? act)
        (list :> (list 's/act-result act))
        '(else>)
        (list 's/act-hop-key act :> '*hk)
        (list '<<if '(some? *hk) '(|hash *hk))
        (list 'continue> (list 's/act-next-step act) (list 's/act-next-st act))))

(defn- enters-source!
  "Hops between H(Acc), H(C), H(op) and H(P)."
  [st]
  (<<sources st
    ;; ====================================================================
    ;; 2. *enters -> enter (uniform choose-at-account) and unlink
    ;; ====================================================================
    (source> *enters source-opts :> *ev)
    (<<if (s/enter-record-fits? *ev)
      (<<subsource *ev
        (case> EnterEvent)
        (loop<- [*step :enter/step-1
                 *st {}
                 :> *result]
          (<<cond
            (case> (= *step :enter/step-1))
            (get *ev :account-key :> *ak)
            (local-select> [(keypath *ak)] $$accounts :> *acc)
            (s/enter-step-1 *ev *acc :> *act)
            (write-act! *act $$accounts)
            (hop-or-done *act)

            (case> (= *step :enter/step-1u-claim))
            (get (get *st :mark) :claim-key :> *ck)
            (local-select> [(keypath *ck)] $$claims :> *c)
            (s/enter-step-1u-claim *ev *st *c :> *act)
            (write-act! *act $$claims)
            (hop-or-done *act)

            (case> (= *step :enter/step-1u-person))
            (s/k-account (get (get *st :mark) :person) (get *ev :account-key) :> *ek)
            (local-select> [(keypath *ek)] $$root-data :> *e)
            (s/enter-step-1u-person *ev *st *e :> *act)
            (write-act! *act $$root-data)
            (hop-or-done *act)

            (case> (= *step :enter/step-1u-account))
            (get *ev :account-key :> *ak)
            (local-select> [(keypath *ak)] $$accounts :> *acc)
            (s/enter-step-1u-account *ev *st *acc :> *act)
            (write-act! *act $$accounts)
            (hop-or-done *act)

            (case> (= *step :enter/step-1a-check-op-returning))
            (get *ev :op-id :> *op)
            (local-select> [(keypath *op)] $$enter-ops :> *eo)
            (s/enter-step-1a-check-op-returning *ev *st *eo :> *act)
            (hop-or-done *act)

            (case> (= *step :enter/step-1c-update-says))
            (get *ev :account-key :> *ak)
            (local-select> [(keypath *ak)] $$accounts :> *acc)
            (s/enter-step-1c-update-says *ev *st *acc :> *act)
            (write-act! *act $$accounts)
            (hop-or-done *act)

            (case> (= *step :enter/step-1b-read-flag))
            (s/k-root (get *st :linked-person) :> *rk)
            (local-select> [(keypath *rk)] $$root-data :> *r)
            (s/enter-step-1b-read-flag *st *r :> *act)
            (:> (s/act-result *act))

            (case> (= *step :enter/step-2-probe))
            (get *st :curr-claim :> *ck)
            (local-select> [(keypath *ck)] $$claims :> *c)
            (s/enter-step-2-probe *ev *st *c :> *act)
            (hop-or-done *act)

            (case> (= *step :enter/step-2b-choose))
            (get *ev :account-key :> *ak)
            (local-select> [(keypath *ak)] $$accounts :> *acc)
            (s/enter-step-2b-choose *ev *st *acc :> *act)
            (write-act! *act $$accounts)
            (hop-or-done *act)

            (case> (= *step :enter/step-2c-confirm-choice))
            (get (get *st :target) :op-id :> *op)
            (local-select> [(keypath *op)] $$enter-ops :> *eo)
            (s/enter-step-2c-confirm-choice *ev *st *eo :> *act)
            (hop-or-done *act)

            (case> (= *step :enter/step-2c-mark-failed))
            (get (get *st :target) :op-id :> *op)
            (local-select> [(keypath *op)] $$enter-ops :> *eo)
            (s/enter-step-2c-mark-failed *ev *st *eo :> *act)
            (write-act! *act $$enter-ops)
            (hop-or-done *act)

            (case> (= *step :enter/step-2c-mark-failed-direct))
            (get *ev :op-id :> *op)
            (local-select> [(keypath *op)] $$enter-ops :> *eo)
            (s/enter-step-2c-mark-failed-direct *ev *eo :> *act)
            (write-act! *act $$enter-ops)
            (hop-or-done *act)

            (case> (= *step :enter/step-2d-finish-failed))
            (get *ev :account-key :> *ak)
            (local-select> [(keypath *ak)] $$accounts :> *acc)
            (s/enter-step-2d-finish-failed *ev *st *acc :> *act)
            (write-act! *act $$accounts)
            (hop-or-done *act)

            (case> (= *step :enter/step-3-use))
            (get *st :curr-claim :> *ck)
            (local-select> [(keypath *ck)] $$claims :> *c)
            (s/enter-step-3-use *ev *st *c :> *act)
            (write-act! *act $$claims)
            (hop-or-done *act)

            (case> (= *step :enter/step-3b-clear-chosen))
            (get *ev :account-key :> *ak)
            (local-select> [(keypath *ak)] $$accounts :> *acc)
            (s/enter-step-3b-clear-chosen *ev *st *acc :> *act)
            (write-act! *act $$accounts)
            (hop-or-done *act)

            (case> (= *step :enter/step-4-root))
            (get (get *st :target) :won-person :> *P)
            (local-select> [(keypath (s/k-root *P))] $$root-data :> *r)
            (s/enter-step-4-root *ev *st *r :> *act)
            (write-act! *act $$root-data)
            (hop-or-done *act)

            (case> (= *step :enter/step-5-link))
            (get *ev :account-key :> *ak)
            (local-select> [(keypath *ak)] $$accounts :> *acc)
            (s/enter-step-5-link *ev *st *acc :> *act)
            (write-act! *act $$accounts)
            (hop-or-done *act)

            (case> (= *step :enter/step-5r-claim))
            (get (get *st :target) :won-claim-key :> *ck)
            (local-select> [(keypath *ck)] $$claims :> *c)
            (s/enter-step-5r-claim *ev *st *c :> *act)
            (write-act! *act $$claims)
            (hop-or-done *act)

            (case> (= *step :enter/step-5r-person))
            (s/k-account (get (get *st :target) :won-person) (get *ev :account-key) :> *ek)
            (local-select> [(keypath *ek)] $$root-data :> *e)
            (s/enter-step-5r-person *ev *st *e :> *act)
            (write-act! *act $$root-data)
            (hop-or-done *act)))
        (ack-return> *result)

        (case> UnlinkEvent)
        (loop<- [*step :unlink/step-1
                 *st {}
                 :> *result]
          (<<cond
            (case> (= *step :unlink/step-1))
            (get *ev :account-key :> *ak)
            (local-select> [(keypath *ak)] $$accounts :> *acc)
            (s/unlink-step-1 *ev *acc :> *act)
            (write-act! *act $$accounts)
            (hop-or-done *act)

            (case> (= *step :unlink/step-2))
            (get (get *st :mark) :claim-key :> *ck)
            (local-select> [(keypath *ck)] $$claims :> *c)
            (s/unlink-step-2 *ev *st *c :> *act)
            (write-act! *act $$claims)
            (hop-or-done *act)

            (case> (= *step :unlink/step-3))
            (s/k-account (get (get *st :mark) :person) (get *ev :account-key) :> *ek)
            (local-select> [(keypath *ek)] $$root-data :> *e)
            (s/unlink-step-3 *ev *st *e :> *act)
            (write-act! *act $$root-data)
            (hop-or-done *act)

            (case> (= *step :unlink/step-5))
            (get *ev :account-key :> *ak)
            (local-select> [(keypath *ak)] $$accounts :> *acc)
            (s/unlink-step-5 *ev *st *acc :> *act)
            (write-act! *act $$accounts)
            (hop-or-done *act)))
        (ack-return> *result))
     (else>)
      (ack-return> s/does-not-fit))))

(defn- root-events-source!
  [st]
  (<<sources st
    ;; ====================================================================
    ;; 3. *root-events -> single-task writes on H(P) (0 hops, atomic batch)
    ;; ====================================================================
    (source> *root-events source-opts :> *ev)
    (<<if (s/root-record-fits? *ev)
      (<<subsource *ev
        (case> OwnLaneMadeEvent)
        (s/k-root (get *ev :root-id) :> *rk)
        (local-select> [(keypath *rk)] $$root-data :> *r)
        (s/own-lane-made-step *ev *r :> *act)
        (s/act-write-k *act :> *wk)
        (<<if (some? *wk)
          (local-transform> [(keypath *wk) (termval (s/act-write-v *act))] $$root-data))
        (ack-return> (s/act-result *act))

        (case> BeginSignInEvent)
        (get *ev :root-id :> *P)
        (get *ev :actor :> *A)
        (get *ev :sign-in :> *G)
        (thin-watched-on-root! *P (get *ev :at) $$root-data $$live)
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
        (thin-watched-on-root! *P (get *ev :at) $$root-data $$live)
        (local-select> [(keypath (s/k-signin *P *G))] $$root-data :> *g)
        (signin-agent-rec *P *g $$root-data :> *arec)
        (s/plan-replace-secret *ev *g *arec :> *plan)
        (apply-root-plan! *P *plan $$root-data $$live)
        (ack-return> (s/plan-result *plan))

        (case> EndSignInEvent)
        (get *ev :root-id :> *P)
        (get *ev :sign-in :> *G)
        (thin-watched-on-root! *P (get *ev :at) $$root-data $$live)
        (local-select> [(keypath (s/k-signin *P *G))] $$root-data :> *g)
        (signin-agent-rec *P *g $$root-data :> *arec)
        (<<if (s/signin-needs-end? *P *g *arec)
          (local-select> [(keypath *P) (sorted-map-range s/live-session-lo s/live-session-hi)] $$live :> *live-ss)
         (else>)
          (identity nil :> *live-ss))
        (s/plan-end-sign-in *ev *g *arec *live-ss :> *plan)
        (apply-root-plan! *P *plan $$root-data $$live)
        (ack-return> (s/plan-result *plan))

        (case> MakeActorEvent)
        (get *ev :root-id :> *P)
        (get *ev :actor :> *A)
        (get *ev :made-by :> *B)
        (thin-watched-on-root! *P (get *ev :at) $$root-data $$live)
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
        (thin-watched-on-root! *P (get *ev :at) $$root-data $$live)
        (local-select> [(keypath (s/k-root *P))] $$root-data :> *r)
        (<<if (not= *A *P)
          (local-select> [(keypath (s/k-actor *P *A))] $$root-data :> *a)
         (else>)
          (identity nil :> *a))
        (<<if (s/actor-needs-end? *P *A *r *a)
          (local-select> [(sorted-map-range (s/k-ao *P (get *a :seq)) (s/k-ao-hi *P))] $$root-data :> *ao-map)
          (local-select> [(keypath *P) (sorted-map-range s/live-things-lo s/live-things-hi)] $$live :> *live-map)
          (s/plan-end-actor-2scan *P *a *ao-map *live-map :> *plan)
         (else>)
          (s/plan-end-actor-noop *ev *r *a :> *plan))
        (apply-root-plan! *P *plan $$root-data $$live)
        (ack-return> (s/plan-result *plan))

        (case> OpenSessionEvent)
        (get *ev :root-id :> *P)
        (get *ev :sign-in :> *G)
        (get *ev :session :> *S)
        (thin-watched-on-root! *P (get *ev :at) $$root-data $$live)
        (local-select> [(keypath (s/k-session *P *S))] $$root-data :> *s)
        (local-select> [(keypath (s/k-signin *P *G))] $$root-data :> *g)
        (signin-agent-rec *P *g $$root-data :> *arec)
        (s/plan-open-session *ev *s *g *arec :> *plan)
        (apply-root-plan! *P *plan $$root-data $$live)
        (ack-return> (s/plan-result *plan))

        (case> CloseSessionEvent)
        (get *ev :root-id :> *P)
        (get *ev :session :> *S)
        (thin-watched-on-root! *P (get *ev :at) $$root-data $$live)
        (local-select> [(keypath (s/k-session *P *S))] $$root-data :> *s)
        (s/open-session-signin *s :> *sg)
        (<<if (some? *sg)
          (local-select> [(keypath (s/k-signin *P *sg))] $$root-data :> *g)
         (else>)
          (identity nil :> *g))
        (signin-agent-rec *P *g $$root-data :> *arec)
        (s/plan-close-session *ev *s *g *arec :> *plan)
        (apply-root-plan! *P *plan $$root-data $$live)
        (ack-return> (s/plan-result *plan)))
     (else>)
      (ack-return> s/does-not-fit))))

(defn- asks-source!
  [st]
  (<<sources st
    ;; ====================================================================
    ;; 4. *asks -> ask (0 hops) & answer-ask (H(U) <-> H(P), Revision 4)
    ;; ====================================================================
    (source> *asks source-opts :> *ev)
    (<<if (s/ask-record-fits? *ev)
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
                (thin-watched-on-root! *P *at $$root-data $$live)
                (local-transform> [(keypath *ok-k) (termval (s/no-such-ask-outcome))] $$root-data))
             (else>)
              (thin-watched-on-root! *P *at $$root-data $$live))
            (:> (get *st :pending-result))

            (case> (= *step :answer-ask/step-2))
            (get *st :target :> *t)
            (get *t :root :> *P)
            (get *t :op-id :> *op)
            ;; Revision 4: check ok:<op> BEFORE the thinning so replays never re-run it!
            (local-select> [(keypath (s/k-ok *P *op))] $$root-data :> *existing-ok)
            (<<if (some? *existing-ok)
              (identity *existing-ok :> *out)
             (else>)
              (thin-watched-on-root! *P (get *t :at) $$root-data $$live)
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
        (ack-return> *result))
     (else>)
      (ack-return> s/does-not-fit))))

(defmodule Auth [setup topologies]
  (declare-depot setup *root-events (hash-by :root-id))
  (declare-depot setup *asks (hash-by :code))
  (declare-depot setup *invites (hash-by :claim-key))
  (declare-depot setup *enters (hash-by :account-key))

  (let [st (stream-topology topologies "auth")]
    ;; No Object position and no byte array in any schema.
    (declare-pstate st $$root-data {String IRootDatum})
    ;; $$live: under each root, a subindexed map from an inner key ("g:<G>",
    ;; "s:<S>", "ends") to what its watchers are pushed
    ;; (softland.auth.schema). A watch of a root is a proxy on that root's
    ;; map alone (softland.auth.client/watch-root), so a write is matched
    ;; against the watches of its own root and no other. Its size is never
    ;; read, so it is not tracked: no extra read on each write.
    (declare-pstate st $$live {UUID (map-schema String ILiveEntry {:subindex-options {:track-size? false}})})
    (declare-pstate st $$accounts {String IAccountState})
    (declare-pstate st $$claims {String IClaimState})
    (declare-pstate st $$invite-ops {UUID IInviteOpState})
    (declare-pstate st $$asks {String IAskState})
    (declare-pstate st $$enter-ops {UUID IEnterOpState})

    ;; Allow 20,000 watched roots per task on $$live (SPEC.md): one proxy
    ;; for each root that a client watches.
    (set-launch-pstate-dynamic-option! setup "$$live" "pstate.proxy.limit" 20000)

    ;; Each depot's dataflow is a function of its own: in one body the
    ;; module's code is more than one JVM method can hold.
    (invites-source! st)
    (enters-source! st)
    (root-events-source! st)
    (asks-source! st))

  ;; ========================================================================
  ;; Query Topology 1: "who-is" (1 RT to 1 task; a person's sign-in and its
  ;; session: 2 seeks)
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
    ;; An agent's sign-in costs one seek more than a person's: whether its
    ;; actor ended is read from the actor's record, since an end of the
    ;; actor is not written on a sign-in that had left `$$live`.
    (<<if *entered?
      (signin-agent-rec *root *sig $$root-data :> *actor-rec)
     (else>)
      (identity nil :> *actor-rec))
    (s/other-signin-of-session *sess *sign-in :> *other)
    (<<if (some? *other)
      (local-select> [(keypath (s/k-signin *root *other))] $$root-data :> *sess-sig)
      (signin-agent-rec *root *sess-sig $$root-data :> *sess-arec)
     (else>)
      (identity *sig :> *sess-sig)
      (identity *actor-rec :> *sess-arec))
    (s/build-who-is-result *root *sig *r *sess *actor-rec *sess-sig *sess-arec :> *result)
    (|origin))

  ;; ========================================================================
  ;; Query Topology 2: "under" (1 RT to 1 task, the watched sign-ins from $$live)
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
      (local-select> [(sorted-map-range (s/k-ae-lo *root) (s/k-ae-hi *root))]
                     $$root-data
                     {:allow-yield? true}
                     :> *ae-map)
     (else>)
      (identity nil :> *ao-map)
      (identity nil :> *ae-map))
    (<<if (s/root-has-signins? *r)
      (local-select> [(keypath *root) (sorted-map-range s/live-signin-lo s/live-signin-hi)]
                     $$live
                     :> *live-g-map)
     (else>)
      (identity nil :> *live-g-map))
    (s/build-under-result *ao-map *ae-map *live-g-map :> *result)
    (|origin))

  ;; ========================================================================
  ;; Query Topology: "accounts-of" (1 RT to 1 task: one seek and a scan of the
  ;; person's entries, which are few)
  ;; ========================================================================
  (<<query-topology topologies "accounts-of"
    [*person :> *result]
    (|hash *person)
    (local-select> [(sorted-map-range (s/k-account-lo *person) (s/k-account-hi *person))]
                   $$root-data
                   :> *entries)
    (s/project-accounts *entries :> *result)
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
