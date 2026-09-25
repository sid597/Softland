# Plan — stage 4, "promotion"

<!-- The rama skill's phase 1 (phase-1-plan.md, template artifact-plan.md),
written on 26 September 2026 from 02:38 IST by a fresh-context session
(Claude Opus 5.5, effort max) on branch rig-plan-promotion, worktree
/mnt/data/projects/Softland-rig-plan-promotion. Design only: no topology
code; one probe (runs/phase4-probe-sealed-box.clj and .txt).

Spec for this stage: SPEC.md (Sid's phase 4 verbatim, "What Rama showed",
binding); PROGRESS.md "Now" on main (the promotion sharpening, the names
sharpening, the two consequences, Sid's rule on order between the two
stores, rulings 7, 7b, 8, 9); RIG.md "Defaults taken overnight, not ruled"
(defaults 1, 2, 5 and 6 bind this stage); the formal model (model.clj
`send-read-out`, `forward`, `continue-promotion`, `refusal`,
`promotion-status`, `exempt?`, the `baseline` readings; scenarios.clj
`b-cases`). Built on: PLAN-stream-store.md and the phase 1 code (main rig
worktree, `envelope.clj` already reserves `:crossing` and `:landing` and
derives both names); PLAN-locks-and-forgetting.md as revised and validated
(e3989912); PLAN-micro-store.md at 848020f0 in Softland-rig-plan-micro,
with its PLAN_VALIDATION-micro-store.md (fixes F1 to F13, all taken as
applied); PLAN-read-exit.md at b0d5f138 in Softland-rig-plan-reads.

Vocabulary: "key" is a fact's key; "lock" is an encryption key. "The copy"
is the landed value in the target layer; "the source" the promoted value
in the owner's layer. -->

## Scope of this stage, in one paragraph

A person asks to move one of their values from a one-owner layer (their
own, or a hand or agent session of theirs) into a shared layer: a group,
or the base. Sid's phase 4: "A request act in the owner's layer; the
read-out opens the value through its lock on the owner's task and writes
the crossing fact there; a landing offer named from the request under a
scheme reserved to the store; pending with two states; a forget before the
read-out refuses, after it does not recall." This stage builds that
protocol on the two gates that exist: the stream gate (phase 1, with
phase 2's lease road) decides the request and the read-out on the owner
layer's home task, and the target's gate decides the landing: the micro
gate (phase 3) for a group and for the base after its re-class, the
stream gate for the base before it (default 6). It adds no topology, no
depot and no PState; it adds fields to `$$layers` and `$$micro-names`, one
query topology, one continuation in the stream gate's event flow, a
landing body in the one delivery function, and a kind of lease. Its tests
are the model's four B cases with their `:shown` sequences, both sides of
the read-out line, a retried request landing once, a crash between the
forward and the landing, a forget of the source after the landing, a
landing refused by the target, a promotion into the base before and after
its re-class, and the depot check after a forget before the read-out.

## The hard question, answered whole

**The problem.** Under default 1 nothing that could open a value sits in a
depot. The landing reaches the micro gate through `*micro-offers`, a depot,
so the copy must be sealed in it, and the lock that opens it must reach
the task where the target's gate decides the landing by a path the depot
never sees. Three forgets pull on that lock: a forget of the source before
the read-out must refuse the promotion; one after it must not recall the
copy (B cases 3 and 4: after Alice's value forget, and after Alice's
person forget, the group's copy stays open); and a later forget of the
copy, or a refused landing, must leave nothing that opens it (P6 widened:
the depot, lease rows, lock rows, anything the read-out leaves). The
stream gate's decision is one event with no partitioner inside (phase 1),
so the read-out cannot fetch anything from another task while it decides,
and the micro gate decides in a batch whose value work runs on the task
that holds the cited lease rows (phase 3, §A).

**The answer: the landing is sealed to a landing lease.**

1. **Who mints the opener.** The target's gate, before the request: the
   person's door makes a *landing lease* in the target layer, a lease act
   of count 1 bound to the one landing name the request will cause
   (`:landing <landing-name>`; names are made before the first gate, the
   names sharpening). The gate mints an X25519 key pair, not a symmetric
   lock, into one lease row beside its other leases: on the micro gate
   `$$micro-names [lease-name :leases 0]` on hash(lease-name); on the
   stream gate (the base while one-owner) `$$layers [base :leases s
   [lease-name 0]]` on the base's home. The row keeps the private key
   bare (`:under nil`), the public key, and the landing name it is for.
2. **What the door carries.** Only the public key, which opens nothing.
   The door takes it by the lease query (`micro-lease`, `lease-locks`),
   which for a landing row returns the public key and never the private
   one, and puts it, with the lease's lock id, into the request's control
   value. The request's depot record therefore holds ids, a layer, a
   permission id and a public key: nothing that opens anything.
3. **Who seals the landing, under which lock.** The stream gate, in the
   read-out's one event on the owner layer's home task, where the source's
   lock row is: it opens the source through its lock (phase 2's
   `open-value>`), draws a fresh 32-byte lock `K` for the copy (bound
   before the decision, with the event's nonces and an ephemeral X25519
   key pair, as phase 2 binds its nonces), seals the copy's plaintext
   under `K`, and seals `K` itself to the landing lease's public key (a
   sealed box: X25519 agreement with the ephemeral key, HMAC-SHA256 over
   the shared secret, both public keys and a fixed label as the wrapping
   lock, AES-GCM over `K`). Probed on this machine's JDK 21 with no new
   dependency (`runs/phase4-probe-sealed-box.txt`): the box opens with the
   lease's private key and with no other; 0.09 ms a key pair, 0.18 ms a
   box, 0.08 ms an unbox; 104 bytes a box. The ephemeral private key and
   the plaintext exist only inside the event.
4. **Where the opener sits until the landing's decision consumes it.** In
   the landing lease row, on the task where the target's gate does the
   landing's value work: the micro gate's arrival task, hash(lease-name),
   which is where the landing arrives (phase 3's route-key is the lease
   name of the first cited lock), or the base's home on the stream gate.
   `K` itself is nowhere at rest except inside boxes that only that
   private key opens: the landing in the depot, and the landing the
   read-out keeps for its replays (`:forwards`, below).
5. **What the landing's decision does with it.** The one delivery
   function gains a landing body: the lease row's private key opens the
   fact's box to `K`. From there the landing is decided as every offer
   into the target is: `K` opens the copy, the copy's own subjects come
   from the target's rules (the target's owner, none for a group, the
   base's no-owner row; the key's grammar over the copy; the landing's
   carried subjects, which the request's tool named), `K` is re-wrapped
   under that wrap into the record (group layers and the base keep locks
   in the record, ruling 7), the value digest is keyed by `K`, and the
   lease row is consumed in the same atomic group, whatever the decision
   (phase 2's `consume-locks>`, the one call site, V-F3).
6. **What happens when the landing is refused.** The lease row is deleted
   in the refusal's commit. The private key was the only opener of the box,
   so the landing's bytes in the depot and in `:forwards` open under no lock
   the store holds. A landing whose lease is already gone (its session
   closed before the landing was decided) is refused and *recorded*
   (`:landing-lock-gone`), so the promotion reads as refused, never as
   crossed for ever.

**The forgets against it, walked.**

- *A value forget of the source before the read-out.* Phase 2's forget
  deletes the source's lock row on the owner's home. The read-out, later
  on the same task, gets `{:erased-at d}` from `open-value>` and records a
  no, `:source-erased`. No copy was sealed, no box made, nothing forwarded.
  The landing lease was never used: its public key sealed nothing, so its
  private key opens nothing retained; it goes when its session closes.
  B case 1.
- *A person forget of the owner before the read-out.* Phase 2's fan-out
  destroys Alice's person lock on every task, the owner's home among them;
  in a one-owner layer the owner's lock is always required, so the source's
  wrap is closed and `open-value>` answers `{:erased-at d}`. The same
  refusal. The two events are ordered by the owner's home task's one
  thread, which is the order the model's B case 2 needs; "the lock store's
  order of a person's forget against a read-out" stays open in PROGRESS.md,
  and this is the rig's answer to it, reasoned and then tested.
- *Either forget after the read-out.* The copy is sealed under `K`, which
  nothing of the source's protects: not the source's lock (deleted by the
  value forget), not Alice's person lock (destroyed by the person forget),
  because the landing lease is bare. The landing is decided as it would
  have been. B cases 3 and 4: the copy is open. The read-out was a read
  that happened.
- *A forget of the copy after the landing.* `K` is then only in the copy's
  record lock (re-wrapped), so the copy's forget in the target reaches it:
  on the stream gate's base, phase 2's forget of a record lock (the
  excision the ruling names for record-default values); in a group, the
  micro gate's forget of a marked value or the operator's excision. After
  it the depot's landing, the micro depot's bytes and `:forwards` open under
  nothing: the lease row was consumed at the landing.
- *A person forget of someone the copy is about.* The copy's wrap is the
  target's (7b as written in shared layers), so it dies or survives with
  that person as any value in the target does; its depot bytes and
  `:forwards` follow the record lock, because the lease row is gone.

**The one-event rule and the batch.** The read-out needs nothing from
another task: the source's lock row is on the owner's home (phase 2: lock
rows sit with their values), `$$persons` is on every task, and the public
key came in the request. So the read-out is one event with no partitioner
inside. The forward comes after a commit boundary, `(|direct
(ops/current-task-id))` (stream.md: always a commit before an internal
append), so the crossing fact is durable before any landing exists. On the
micro side the landing is one more sealed act in a batch: it arrives on
its lease's task, where block 1 gathers the lease row and does the value
work, and nothing of it reaches the leader's fold but its skeleton (M3).

**Alternatives weighed and rejected** (the difficulty log has the working):

- *A symmetric landing lock minted by the micro gate and fetched by the
  stream gate in a hop before the read-out's event.* Works, with no new
  cryptography, but a bare lock then travels between tasks (phase 3's rule:
  a lock never leaves the task it is read on), and the stream gate's
  read-out depends on the micro store being reachable at that moment, a
  read of the other store in the promotion's path. Rejected for the sealed
  box, which needs nothing from the other store until the landing.
- *A lock minted at the read-out and kept in a row on the owner's task for
  the micro gate to read.* The micro gate can read that row but cannot
  delete it (a PState has one owning topology), so consumption needs a
  message back to the stream gate, two places consume one lock (against
  phase 2's V-F3), and between the landing and that message the copy's
  forget cannot reach the row. Rejected.
- *The copy's lock wrapped under the target subjects' person locks in the
  landing offer.* The store holds every person lock, so the depot would
  hold something that opens with a lock the store keeps; the copy's
  excision would not reach the depot's wrapped copy. Rejected by default 1.
- *The landing sealed under the source's lock, or a lease wrapped under the
  requester's person lock.* A forget after the read-out would recall the
  copy: B cases 3 and 4 fail. Rejected by the ruling.
- *A symmetric landing lock carried in the request, sealed under a stream
  lease, destroyed by the read-out.* The request's value would be a lock a
  read of the owner's layer could open, the door would hold the copy's
  future lock, and the read-out would have to forget part of the request.
  Rejected: more moving parts than the box, and a secret as a value.
- *An in-memory holder on the target's task.* Lost at a worker restart
  between the read-out and the landing, which would lose a crossed copy.
  Rejected, as phase 2 rejected it for the door.
- *A lock derived from a store secret and the landing name.* A long-lived
  opener of every landing in the depot, which no forget reaches. Rejected.
- *The read-out re-opening the source on a replay to re-send the landing*
  (the model's `forward` re-sends "while the source can still be opened").
  In the model the first send is atomic with the read-out's decision; in
  Rama the append follows the commit, so a crash between them followed by
  a forget would lose a crossed copy. Rejected for a stored forward, below.

## The protocol, act by act: the uniformity rule, chosen once

The sharpening: "two steps, a uniformity rule chosen once". A uniformity
rule is one every record follows, and a change splits the store into
before and after (Sid's frame for a corner). So this is stated once, for
every promotion whatever its source layer's kind (personal, hand, agent)
and whatever its target (a group; the base on either gate), and the record
shapes it writes are first-record (PR1 to PR5 below). Nothing in it
depends on which gate lands the copy except the road of the forward.

**0. The landing lease** (the target's gate). The door makes one lease act
into the target layer T, named `[T C :offer uuid']` where C is T's class
now, with the one control fact `{:e s :k :lease :v {:count 1 :landing
L*}}`, where `L*` is the landing name the request will cause (step 1). A
lease act is phase 2's and phase 3's, decided as they decide it (it needs
a permission in T, RIG.md "For Sid" 1); the new part is the `:landing`
field, which makes the gate mint one X25519 key pair instead of n
symmetric locks, write its row bare (`:under nil`) with `:public` and `:for
L*`, and refuse a count other than 1 (`:malformed-control`). The door takes
the public key through the lease query, which for a landing row returns
`{:public bytes :for L*}` and never `:sealed`.

**1. The request act** (the stream gate, the owner layer L's home). An
ordinary act by the owner, named `[L :by-layer :offer uuid]`, citing a
permission in L, under a session, standing on the source with its stamp
carried (`:stood-on {src-fid s}`), with one control fact on the source's
entity:

    {:e <source's e> :k :promote-request
     :v {:source    <src-fid>            ; a fact id in L
         :target    T                    ; a layer other than L
         :class     C                    ; T's class as the door saw it
         :lease     [lease-name 0]       ; the landing lease's lock id; lease-name tagged [T C ...]
         :public    "<base64, 44 bytes>" ; the landing lease's public key
         :permission <pid in T>          ; the landing's permission, checked by T's gate
         :replaces  <fid in T> | nil     ; the head the copy replaces
         :subjects  #{...}}}             ; the copy's carried subjects, named by the tool

A control fact, so its value is plaintext (the model's `control-keys` has
`:promote-request`; phase 2 seals value facts only), and nothing in it is
a value. The gate adds `:promote-request` to R13's control facts a
non-operator may write, for anyone whose permission covers L, and checks
the value's shape (`:malformed-control` otherwise: the keys exactly these,
the source's name in L, T ≠ L, C a class, the lease name tagged `[T C]`,
the public key 44 bytes, the permission's layer T, the subjects at most
256 readable keywords). Everything else about the source is the
read-out's to check, as in the model, where the request is only a request.

**2. The read-out** (the store's own step, on L's home). After the
request's yes, whether decided now, on a replay, or on a door's resend
answered from the record, the gate's event flow continues on the same
task past a commit boundary with the read-out: an act named
`(env/crossing-name req)` = `[L nil :crossing uuid]`, `:who :store`, no
permission (the model's `exempt?`: the store's own steps act at the root),
`:because-of req`, standing on `{src-fid s, [req 0] r}`, with one fact
`{:e <source's e> :k :crossed :v {:request req :source src-fid}}` (a placed
control fact, class nil, phase 1's P4). Decided in one event:

- the crossing name has a record → the record path (below);
- `open-value>` of the source gives `{:value v :stamp s}` → **yes**: the
  crossing's answer record and log row, its stood-on, the stored forward
  `[L :forwards req]`, and the task's clock, one atomic group;
- `{:erased-at d}` (a value forget, or the owner's person forget closing
  the wrap) → a recorded **no**, `:source-erased`;
- anything else (no such fact, a control fact, a retract) → a recorded
  **no**, `:source-has-no-value` (the model's two read-out refusals).

The stamp is `rig.store.clock/next-stamp` over the wall, the task's last
stamp and the largest stood-on stamp, so the crossing is after the request
and the source. A refusal is recorded with its stamp, so a read as of a
moment can say when it was refused.

**3. The forward.** After the crossing's yes (fresh or recorded), past
`(|direct (ops/current-task-id))` so the crossing is durable first:

- C = `:by-entity` (a group, or the base after its re-class): `(|hash
  lease-name)`, the task `*micro-offers`' `hash-by micro/route-key` picks
  for this landing (route-key is the lease name of the first cited lock,
  phase 3's M4), then `(depot-partition-append! *micro-offers landing
  :append-ack)`;
- C = `:by-layer` (the base while one-owner, on the stream gate): `(|hash
  T)`, T's home (phase 1's P2), and the landing's decision there in one
  event, with no depot on the road at all.

The landing is the one kept in `[L :forwards req]`, byte for byte: every
send of it, first or replayed, is the same record, so the target's gate
answers every send after the first from its record, and a forget of the
source after the read-out cannot stop a re-send (the read-out never opens
the source again).

**4. The landing** (the target's gate). An ordinary act into T:

    {:version 1 :name [T C :landing uuid]  :who <the request's who>
     :layer T :class C :permission <the request's landing permission>
     :session <the request's session> :because-of req
     :stood-on {src-fid s, [crossing-name 0] S}
     :subjects <the request's :subjects>
     :facts [{:e <source's e> :k <source's k> :replaces <the request's replaces>
              :sealed <copy sealed under K> :lock-id [lease-name 0]
              :box {:eph <44 bytes> :nonce <12> :wrapped <48>}}]}

Decided by the target's gate like any sealed act into T: face checks, the
record path, the permission in T (checked by the gate that orders T, the
permissions sharpening), T's class, stale replaces, the delivery (landing
body), the copy's own subjects and wrap, the value digest, consumption.
The one difference from a door's act: its lock comes out of a box, and a
lease that no longer delivers is a recorded refusal, `:landing-lock-gone`,
because a landing is never resent under another lease and a promotion must
end as done or refused. The copy's subjects never include the source's
owner unless T's grammar or the request's tool names her: the landing
carries the request's `:subjects`, not the source act's subject slot.

**5. Pending, its two states, and the read.** The model's
`promotion-status` as of T, without optimism:

- **pending**: the request is admitted, and no crossing answer at or
  before T: "not yet read out, still forgettable";
- **crossed**: the crossing is a yes at or before T, and no landing answer
  at or before T that is settled: "read out and not landed";
- **done**: the landing is a yes at or before T, settled (on the micro
  gate, in a batch at or below the frontier, R5, so the copy reads on every
  task);
- **refused**: the crossing or the landing is a no at or before T, with its
  reason.

The query `promotion-status` (below) says which. The model's precedence
is kept: done, then refused, then crossed, then pending.

**6. What is said at the point of promotion** (the sharpening: "Say all of
this at the point of promotion"). The door's `promote!` returns, with the
request's and the crossing's answers, the statements as data, keyword and
sentence, so a surface can show them where the person promotes:

- `:pending` "Not yet read out. Forgetting the value now refuses this
  promotion.";
- `:crossed` "Read out at <date>. Forgetting the value now does not
  recall the copy; the read-out is a read that happened. The copy is about
  whoever <T>'s grammar and tool name, not you. Crossed does not promise
  done: the landing can still be refused.";
- `:done` "Landed in <T> at <date>.";
- `:refused` "Refused: <reason>." with the reason's sentence.

The same map comes back from `promotion-status`, so a read shows the
statement that fits the state it found.

## Names

- The request: `[L :by-layer :offer uuid]`, made by the door before the
  first gate. It carries the landing's name implicitly: the uuid.
- The read-out: `(env/crossing-name req)` = `[L nil :crossing uuid]`,
  phase 1's, unchanged: the request's uuid under the reserved scheme, in
  L, placed by the store.
- The landing: `[T C :landing uuid]`. Phase 1's `(env/landing-name req
  target)` fixes C to `:by-entity`; this stage gives it the class,
  `(env/landing-name req target class)`, because the base before its
  re-class is one-owner and its landings must reach the stream gate (a
  name reaches one gate only, the names sharpening). The class is the one
  the request carries, so the name is fixed before the first gate, and a
  re-class of T between the request and the landing makes the landing
  reach the gate that no longer orders T: the stream gate refuses it
  `:class-mismatch` (recorded, phase 1), and the promotion reads refused.
- The landing lease act: `[T C :offer uuid']`, the door's, and its one
  lock id `[lease-name 0]` (phase 2's `lease-ids`).
- **The reservation.** The stream gate keeps refusing `:crossing` and
  `:landing` names from `*offers` on their face (phase 1's
  `:reserved-scheme`, unchanged): the store's steps on that gate never
  pass a depot. `*micro-offers` carries both the doors' acts and the
  store's landings, and a depot cannot tell a topology's append from a
  client's, so the micro gate takes a `:landing` name as a landing and only
  as one: exactly one value fact, citing a landing lease whose `:for` is
  this very name, and every such record decided and recorded. A `:crossing`
  name there stays refused on its face, and an `:offer` name citing a
  landing lease is `:no-such-lock`. The limit, named for Sid (open
  question 1): a door that deliberately makes a landing name for a
  request it sent could land content no read-out made. "Operator trusted
  at launch. No signing; ... a later edition can add the part": a store
  signature on its landings is that part.

## Stamps

From `rig.store.clock` throughout (default 2): the request's stamp is after
the source's (carried stood-on), the crossing's after the request's and the
source's (both stood on), the landing's after the crossing's (carried in the
landing's stood-on; phase 3's M6 takes the largest carried stamp plus one,
as the stream gate's `next-stamp` does). So in stamp order the source comes
before the request, the request before the crossing, the crossing before the
landing, on any tasks and across the two stores, by stood-on alone (Sid's
rule: order between the stores exists only through stood-on). A forget of
the source and the read-out are ordered by the owner's home task, where
both are decided; a forget of the source against the landing is ordered by
nothing, and nothing here needs it to be.

## Reads

1. **`promotion-status [*layer *req *as-of]`**, a query topology (it needs
   more than one read, and on two tasks when crossed). Below.
2. **The landing lease's public key**: phase 3's `micro-lease [lease-name
   F]` and phase 2's `lease-locks [layer s]`, each returning for a landing
   row `{:public bytes :for name}` in place of a plaintext lock. No new
   read, one more case in each.
3. **The request's and the landing's answers by name**: the door's
   `lookup` (phase 1 on the stream gate; phase 3's name row on the micro
   gate). Unchanged.
4. **The source and the copy as values**: the read exit and `read-as-of`
   (phase 2's `open-value>`), unchanged. The copy reads as any value of T;
   the source reads erased after its forget.

## Writes

| Operation | Enters by | Event |
|---|---|---|
| landing lease | `*offers` (T one-owner) or `*micro-offers` | lease act with `:landing` |
| request | `*offers`, hash-by `:layer` → L's home | `:promote-request` act |
| read-out | no depot: the request's continuation on L's home | `:crossed` act |
| landing, T on the micro gate | `depot-partition-append! *micro-offers` on hash(lease-name) | the landing act |
| landing, T on the stream gate | no depot: a hop to T's home | the landing act |

## PState Design

No new PState. Every piece this stage keeps shares a key and partitioner
with a PState that exists, so it is a field there (phase-1-plan.md: one
PState per key and partitioner).

**`$$layers`** (stream gate, `gate` topology; key a layer, on its home):

- **`:forwards`**, new: `(map-schema PersistentVector <forward> {:subindex-options
  {:track-size? false}})`, request name → the landing as sent:

      (fixed-keys-schema
       {:name       PersistentVector            ; [T C :landing uuid]
        :route      PersistentVector            ; lease-name, the append's hash key, or nil for a stream target
        :who        Keyword  :layer Keyword  :class Keyword  :session Keyword
        :permission PersistentVector  :because-of PersistentVector
        :stood-on   (map-schema PersistentVector Long)
        :subjects   (set-schema Keyword)
        :e Keyword  :k Keyword  :replaces PersistentVector
        :sealed     bytes  :lock-id PersistentVector
        :box        (fixed-keys-schema {:eph bytes :nonce bytes :wrapped bytes})})

  Written once, in the crossing's yes, with `termval`. Read only on the
  crossing's record path (a replay, a resend, the status read). Subindexed
  because a layer can promote without bound. Considered: the forward inside
  the crossing's answer record (no extra seek on the record path) against
  its own field (one seek more on a replay or a status read of a crossed
  promotion, and an answer schema the same for every act). Chosen: its own
  field; the record path of a crossing is rare, and phase 1's answer lookup,
  which every offer makes, should not carry a promotion's bytes.
- **Lease rows** `[layer :leases s lock-id]` gain two optional fields,
  `:public bytes` and `:for PersistentVector`, nil for every lease but a
  landing lease. In the base while one-owner only.
- **`:answers`, `:log`, `:stood-on`** hold the request's and the crossing's
  records exactly as phase 1 holds any act's (the crossing's `:who :store`,
  class nil, permission nil), and the stream-gate landing's in T's layer.

**`$$micro-names`** (micro gate, `micro` topology; key a name): the lease
row `[lease-name :leases 0]` gains `:public` and `:for`, as above. The
landing's answer lives in its name row, written in the deciding batch
(phase 3), which is what the status read and the door's lookup find.

**Considered and rejected: a `$$promotions` PState** keyed by request
name with the status. The status is a function of three answers the gates
already record (request, crossing, landing); a second copy is a
denormalization with its own window between the gates' commits and its
own writes on two stores, which the micro gate cannot make on the stream
side anyway. The status read costs at most two tasks.

## Depots

None new. `*offers` (phase 1) is unchanged in its refusals. `*micro-offers`
(phase 3) takes the stream gate's landing appends (phase 3 declared it so:
"a client depot accepts topology appends"); its parse takes a `:landing`
name as a landing only (Names, "The reservation"). A `:disallow` depot for
landings was weighed: it would make the reservation structural, but each
`source>` in a microbatch topology is its own dataflow section with its own
scope (microbatch.md), so landings from a second depot could not join
phase 3's one fold over the batch's skeletons, and a landing and a door's
act on the same head in one batch would be decided by two folds. Rejected.

## Topologies and PStates

**`gate` — stream** (phase 1's reasons stand: the door waits on its ack,
and the answer returns through it). No new topology: the continuation is
more event flow in the same `<<sources` block, because only the gate writes
`$$layers` and the read-out's writes must be there.

- **The request's continuation**: after the request's decision or record
  answer is a yes and its fact is `:promote-request`: commit boundary; the
  read-out's event (record lookup of the crossing name; `open-value>` of the
  source; the pre-bound randomness; `decide` over the store-made crossing
  offer; the writes, one atomic group); commit boundary; the forward; one
  `ack-return>` at the end with `{:request <ack> :crossing <ack>}`.
  `:retry-mode :all-after` (phase 1) replays the whole record after a
  failure, and every step answers from its record by name, so a replay
  repeats nothing but the append.
- **Writes and idempotence**: every write is a `termval` by name (the
  crossing's answer, log, stood-on, forward) or a `NONE>` (lease
  consumption), idempotent under replay; the clock's `termval` takes the
  decision's stamp, which a replay recomputes only if the first attempt did
  not commit (and then nothing of it was visible). The depot append is
  at-least-once; its duplicates are answered by name at the target.
- **The stream-gate landing** (T the base while one-owner): one event on
  T's home through the gate's ordinary decision path (settings,
  permissions, heads, clock, the landing lease row, `$$persons`, all local),
  with the delivery's landing body. It never passes `parse`, which keeps
  refusing landing names from clients.
- **The landing lease** on this gate: a lease act with `:landing` mints one
  key pair (bound before `decide`, as phase 2's locks are) into a bare row.

**`micro` — microbatch** (phase 3's). Changes, all inside its blocks:
`parse` takes a `:landing` name as a landing (one value fact, citing a
landing lease); the gather on the arrival task reads that lease row as it
reads any (the landing arrived on hash(lease-name), so the row is local),
runs the delivery's landing body, and carries `:landing-lock-gone` into the
skeleton as a recorded reason when the row is missing or `:for` another
name; the lease act mints a key pair when `:landing` is set; consumption is
unchanged (`consume-locks>` in the deciding group). The micro gate appends
nothing (microbatch appends are not exactly-once, microbatch.md).

PStates: `$$layers` (gate, above) and `$$micro-names` (micro, above), with
the full typed schemas above; no `Object` anywhere.

## Query Topologies

**`promotion-status [*layer *req *as-of :> *status]`**, invoked on
`(|hash *layer)`, L's home:

1. `[L :answers req]` (the request's record): nil → `{:status :none}`; a
   no → `{:status :none :request-refused reason}` (a refused request is no
   promotion, as in the model, which lists admitted requests only);
2. `[L :answers crossing]`: nil, or stamped after `*as-of` → **pending**;
   a no → **refused** with its reason and stamp;
3. a yes: `[L :forwards req]` for the landing's name and road; then
   `(|hash landing-name)` for a micro target: the name row's answer and
   `$$micro-task`'s frontier F on that task (phase 3's frontier; a batch
   above F is not yet settled everywhere) → a yes in a batch ≤ F stamped at
   or before `*as-of` → **done**; a no → **refused**; else **crossed**; or
   `(|hash T)` for a stream target: `[T :answers landing]` → done, refused
   or crossed by the same rule.

Input examples and meaningful reads: an admitted request not yet read out:
2 reads, both meaningful (the second's nil is the answer); refused at the
read-out: 2; crossed or landed on the micro gate: 3 on L's home plus 2 on
the landing's name task; on the stream gate: 3 plus 1; a name that was
never a request: 1. **Variable**, handled with `<<if` on each answer: the
second task is reached only when the crossing is a yes, and no read is
issued that the state already decided. The door's `promote!` and the read
exit (a `:promote-request` row gets `:promotion` from this query) call it.
