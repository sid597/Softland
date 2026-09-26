;; IMPORTANT: Before modifying this file, re-read PLAN-promotion.md ("The
;; protocol, act by act", "Topologies and PStates", "Query Topologies",
;; PR7, PR8, PR15, and the [F6] fix).
(ns rig.store.promote-flow
  "Promotion's dataflow (stage 4, PLAN-promotion.md): the continuation the
  stream gate runs after a promotion request's answer, and the
  `promotion-status` query.

  The continuation (PR7): after the request's yes, fresh, on a replay or
  on a door's resend answered from the record, the gate's event goes on
  on the same task past a commit boundary, so the request is durable
  first; then the read-out, one event with no partitioner inside: the
  crossing's record, else the source opened through its lock, the copy
  sealed and its lock boxed to the landing lease, the crossing decided by
  `gate/decide` and written by `gate-event/write-decided>` with the stored
  forward, one atomic group; then another commit boundary, so the crossing
  is durable before any landing exists; then the forward, the stored
  landing as stored (PR8): appended to `*micro-offers` on the task its
  partitioner picks, with a commit boundary right before the append
  ([F6]), or, for a target this gate orders, decided on the target's home
  by `gate-event/record-or-decide>`, the gate's one path. The request's
  ack carries the crossing's answer (and a stream landing's), set by the
  last `ack-return>` of its event tree. Every step answers from its record
  by name, so a replay repeats nothing but the append, whose duplicates
  the target's gate answers by name.

  Test hooks (R3, PR15): `inject/held?` at `:before-read-out` and
  `:before-forward` ends the record's processing there; the crash points
  `:before-read-out`, `:before-forward` and `:after-forward` throw where a
  test arms them.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [com.rpl.rama :as rama :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :refer :all]
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.gate-event :as gate-event]
            [rig.store.inject :as inject]
            [rig.store.locks :as locks]
            [rig.store.promote :as promote]))

;; ------------------------------------------------------------ the read-out

(deframaop read-out>
  "The read-out's event on the owner layer's home (PLAN step 2): one event
  with no partitioner, needing nothing from another task (the source's
  lock row is here, `$$persons` is on every task, the public key came in
  the request). The crossing's name has a record: its answer, by name
  (`promote/recorded-ack`), and for a yes the stored forward. Else,
  fresh: the settings, clock and wall; the source's answer and row,
  opened by `locks/open-row>` (also after a re-class of the layer: the
  read-out is placed where the source's lock is and claims no class, as
  in the model; the plan's F8 refused it); the draw; the read-out's
  verdict; the crossing decided by `gate/decide` and written by
  `gate-event/write-decided>` with the stored forward, in one atomic
  group. Emits once `[crossing-ack forward]`, the forward nil for
  anything but a yes."
  [*layer *offer *req-stamp]
  (<<with-substitutions [$$layers (rama/this-module-pobject-task-global "$$layers")
                         $$clock (rama/this-module-pobject-task-global "$$clock")]
    (get *offer :name :> *req)
    (env/crossing-name *req :> *cname)
    (local-select> (keypath *layer :answers *cname) $$layers :> *crec)
    (<<if (some? *crec)
      ;; read out before (a replay, a resend): the recorded answer, by name, and the stored forward
      (promote/recorded-ack *crec *cname :> *cack)
      (<<if (= :yes (get *cack :answer))
        (local-select> (keypath *layer :forwards *req) $$layers :> *rfwd)
       (else>)
        (identity nil :> *rfwd))
      (:> *cack *rfwd)
     (else>)
      (local-select> (keypath *layer :settings) $$layers :> *settings)
      (local-select> STAY $$clock :> *clock)
      (gate/wall-now :> *wall)
      (promote/source-of *offer :> *sfid)
      (first *sfid :> *sname)
      (second *sfid :> *sidx)
      (local-select> (keypath *layer :answers *sname) $$layers :> *srec)
      (<<if (= :yes (get *srec :answer))
        (local-select> (keypath *layer :log *sname *sidx) $$layers :> *srow)
       (else>)
        (identity nil :> *srow))
      (locks/open-row> *layer *sfid *srow (get *srec :stamp) nil :> *opened)
      ;; placed where the source's lock is, the read-out claims no class (the model's refusal)
      (promote/crossing-offer *offer *req-stamp *srow (gate/class-in-force *offer *settings) :> *crossing)
      (env/digest *crossing :> *cdigest)
      ;; the randomness, bound before the decision (as phase 2 binds its locks and nonces)
      (promote/fresh-read-out :> *fresh)
      (<<if (promote/fresh-ok? *fresh)
        (promote/read-out-verdict *offer *srow *opened *fresh :> *ro)
        (gate/decide *crossing *settings {} {} *clock *wall *cdigest (promote/crossing-context *ro) :> *d)
        (<<if (= :decide (get *d :kind))
          (gate-event/write-decided> *layer *crossing *cname *d)
          (promote/forward *offer *d *ro *srow :> *fwd)
          (<<if (some? *fwd)
            ;; the stored forward, in the crossing's group (PR8, [F4])
            (local-transform> [(keypath *layer :forwards *req) (termval *fwd)] $$layers))
         (else>)
          (identity nil :> *fwd))
        (:> (get *d :ack) *fwd)
       (else>)
        ;; a draw that failed: nothing decided or recorded; a resend retries
        (:> (promote/gate-error-ack *cname) nil)))))

;; ------------------------------------------------------------- the forward

(deframaop forward>
  "The forward (PLAN step 3), the stored landing as stored (PR8): for a
  target on the micro gate, `(|hash route)`, the task `*micro-offers`'
  partitioner picks for it (its landing lease's name, where the lease row
  is), a commit boundary right before the append ([F6]), and the append;
  for a target this gate orders (the base while one-owner), a hop to its
  home and its decision there by the gate's one path, with no depot on the
  road (PR7), the request's ack then carrying the landing's answer too."
  [*fwd *req *ack *cack]
  (<<with-substitutions [*micro-offers (rama/this-module-pobject-task-global "*micro-offers")]
    (get *fwd :landing :> *landing)
    (get *fwd :route :> *route)
    (<<if (some? *route)
      (|hash *route)
      (|direct (ops/current-task-id))
      (depot-partition-append! *micro-offers *landing :append-ack)
      (inject/point! :after-forward *req)
     (else>)
      (get *landing :layer :> *T)
      (|hash *T)
      (promote/gate-intake *landing :> *lin)
      (get *lin :offer :> *loffer)
      (get *loffer :name :> *lname)
      (gate-event/record-or-decide> *T *loffer *lname (get *lin :digest) *lin :> *lack *lfan)
      (inject/point! :after-forward *req)
      (ack-return> (assoc *ack :crossing *cack :landing *lack)))))

;; -------------------------------------------------------- the continuation

(deframaop continue>
  "The gate's event after a record's answer is set (rig.store.module): a
  promotion request answered yes, fresh or from its record, goes on to its
  read-out and forward (PR7); every other act passes straight through,
  emitted once. Nothing is emitted after a continuation: what follows an
  answer in the gate's flow is the person fan-out, which a request never
  has."
  [*layer *offer *ack]
  (<<if (promote/continues? *offer *ack)
    (get *offer :name :> *req)
    ;; a commit boundary: the request is durable before its read-out
    (|direct (ops/current-task-id))
    (inject/point! :before-read-out *req)
    (<<if (not (inject/held? :before-read-out *req))
      (read-out> *layer *offer (get *ack :stamp) :> *cack *fwd)
      (ack-return> (assoc *ack :crossing *cack))
      (<<if (and> (some? *fwd) (not (inject/held? :before-forward *req)))
        ;; a commit boundary: the crossing and its forward are durable before any landing exists
        (|direct (ops/current-task-id))
        (inject/point! :before-forward *req)
        (forward> *fwd *req *ack *cack)))
   (else>)
    (:>)))

;; ------------------------------------------------------------- the status

(deframafn status-home>
  "`promotion-status`' first part, on the owner layer's home: the
  request's answer; for a yes at or before T the crossing's; for a yes of
  that at or before T the stored forward's summary (the landing's name,
  road and target; its sealed bytes stay on this task).
  Each read only when the state before it does not already decide
  ([F5])."
  [*layer *req *as-of]
  (<<with-substitutions [$$layers (rama/this-module-pobject-task-global "$$layers")]
    (local-select> (keypath *layer :answers *req) $$layers :> *rrec)
    (<<if (promote/admitted-by? *rrec *as-of)
      (env/crossing-name *req :> *cname)
      (local-select> (keypath *layer :answers *cname) $$layers :> *crec)
      (<<if (promote/crossed-by? *crec *as-of)
        (local-select> (keypath *layer :forwards *req) $$layers :> *fwd)
        (:> {:req *rrec :crossing *crec :forward (promote/forward-summary *fwd)})
       (else>)
        (:> {:req *rrec :crossing *crec}))
     (else>)
      (:> {:req *rrec}))))

(deframaop micro-landing>
  "The landing's answer on its name's task at the micro gate, and, only
  when there is one, that task's frontier ([F5]). Emits once `[rec F]`."
  [*lname]
  (<<with-substitutions [$$micro-names (rama/this-module-pobject-task-global "$$micro-names")
                         $$micro-task (rama/this-module-pobject-task-global "$$micro-task")]
    (local-select> [(keypath *lname :answer)] $$micro-names :> *lrec)
    (<<if (some? *lrec)
      (local-select> [(keypath :frontier)] $$micro-task :> *F)
      (:> *lrec *F)
     (else>)
      (:> nil nil))))

(deframafn stream-landing>
  "The landing's answer on the target's home at the stream gate."
  [*T *lname]
  (<<with-substitutions [$$layers (rama/this-module-pobject-task-global "$$layers")]
    (local-select> (keypath *T :answers *lname) $$layers :> *lrec)
    (:> *lrec)))

(defn declare-queries!
  "Declares `promotion-status [layer req as-of]` (PLAN 'Query
  Topologies'): the owner layer's home, then, for a crossed promotion,
  the landing's name task (micro) or the target's home (stream); a map
  with `:status` (`:none`, `:pending`, `:crossed`, `:done`, `:refused`),
  the stamps found, a refusal's `:reason` and `:at`, and the statement."
  [topologies]
  ;; the Rama kondo hook reads <<query-topology inside defmodule only
  #_:clj-kondo/ignore
  (<<query-topology topologies "promotion-status" [*layer *req *as-of :> *result]
    (|hash *layer)
    (status-home> *layer *req *as-of :> *home)
    (get *home :forward :> *fwd)
    (<<if (nil? *fwd)
      (identity nil :> *lrec)
      (identity nil :> *F)
     (else>)
      (get-in *fwd [:landing :name] :> *lname)
      (<<if (some? (get *fwd :route))
        (|hash *lname)
        (micro-landing> *lname :> *lrec *F)
       (else>)
        (get-in *fwd [:landing :layer] :> *T)
        (|hash *T)
        (stream-landing> *T *lname :> *lrec)
        (identity nil :> *F)))
    (promote/status-of *home *lrec *F *as-of :> *st)
    (|origin)
    (identity *st :> *result)))
