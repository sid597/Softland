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

**One discrepancy in phase 3's plan this stage leans on.** Its §A (change
2, and the validation's "routing by lease name") routes a sealed act by the
lease name of its first cited lock, so it arrives where its lease rows are;
its "Depots" section still carries a revision note routing a sealed act by
its `:session` (PLAN-micro-store.md line 1448). This plan follows §A: the
forward hops to hash(lease-name) because the landing lease's row is there.
If phase 3's build routes by session instead, the lease rows must follow
(they would sit on hash(s)), and the forward hops to hash(s): one change in
`route` of the stored forward, no record touched.

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

## Partitioning efficiency

**Optimal placement first.** Every step of a promotion wants to run where
its state already is, and the rulings put that state in three places: the
source's value and lock row on the owner layer's home (`hash(L) mod N`,
phase 1's P2; phase 2: lock rows sit with their values), which is where the
request, the read-out and the stored forward go; the landing lease on the
task where the target's gate does a sealed act's value work
(`hash(lease-name) mod N` on the micro gate, phase 3's M4; T's home on the
stream gate), which is where the landing arrives; the copy's rows on its
entity's task (phase 3). So `f(request) = hash(L)`, `f(landing) =
hash(lease-name)` or `hash(T)`, and the partitioners are the ones that
exist: `hash-by :layer` on `*offers`, `|hash` on the lease name before the
append (the same function as `*micro-offers`' `hash-by route-key`, probed
by phase 3's §G for a name vector), `|hash` on T for a stream target. No
placement state, because each `f` is a pure function of what the step
already holds.

**The dominant read** is `promotion-status`. Seeks are point reads summed
over every task the read touches (all are point reads into subindexed maps
or top-level keys; no range scans, so iterator reads are 0). The mix is
assumed, not measured: most promotions a person looks at have landed.

### N = 1 task (single-task baseline)
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| landed, micro target (done) | 0.60 | 5 | 0 |
| landed, base on the stream gate (done) | 0.05 | 4 | 0 |
| crossed, not landed | 0.05 | 5 | 0 |
| refused at the landing | 0.05 | 5 | 0 |
| pending | 0.05 | 2 | 0 |
| refused at the read-out | 0.10 | 2 | 0 |
| not a request | 0.10 | 1 | 0 |
Weighted seeks = 4.10   |   Weighted iterator reads = 0

### N = 16 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| landed, micro target (done) | 0.60 | 5 | 0 |
| landed, base on the stream gate (done) | 0.05 | 4 | 0 |
| crossed, not landed | 0.05 | 5 | 0 |
| refused at the landing | 0.05 | 5 | 0 |
| pending | 0.05 | 2 | 0 |
| refused at the read-out | 0.10 | 2 | 0 |
| not a request | 0.10 | 1 | 0 |
Weighted seeks = 4.10   |   Weighted iterator reads = 0

### N = 128 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| landed, micro target (done) | 0.60 | 5 | 0 |
| landed, base on the stream gate (done) | 0.05 | 4 | 0 |
| crossed, not landed | 0.05 | 5 | 0 |
| refused at the landing | 0.05 | 5 | 0 |
| pending | 0.05 | 2 | 0 |
| refused at the read-out | 0.10 | 2 | 0 |
| not a request | 0.10 | 1 | 0 |
Weighted seeks = 4.10   |   Weighted iterator reads = 0

Flat: the read touches one task, or two, never a number that grows with N.
At N = 1 the second "task" is the same task and the seeks are the same.

**The write path, per promotion, for the same check.** Landing lease: a
lease act (phase 2 or 3's cost, one row, one key pair, 0.09 ms). Request:
phase 1's decision, no added read (the control value is checked in memory).
Read-out, one event on L's home: the crossing name's record (1),
`open-value>` of the source (its answer, row, lock row, and the owner's
`$$persons` entry: 4), the settings and the clock (2): 7 seeks, 1 box and
1 seal (0.2 ms), 5 writes. Forward: 1 hop and 1 append. Landing: one sealed
act of phase 3 (or phase 2 on the base) plus 1 unbox (0.08 ms). Every term
is independent of N.

## Design Decisions

- **Subindexing.** `:forwards` is subindexed (a layer promotes without
  bound; a replay reads one entry). Lease rows keep phase 2's and phase
  3's subindexing. Nothing else is new.
- **Colocation.** The read-out is on the task that holds the source's lock
  row, so the one-event rule holds with no partitioner inside; the landing
  arrives on the task that holds its lease row, so the delivery is local;
  the stored forward sits beside the crossing it belongs to.
- **The read-out is a continuation, not a depot record** (PR7). The model
  queues it as the store's own offer in the partition's inbox; the rig runs
  it as the next event of the request's own record, past a commit boundary,
  on the same task. The same order results (whatever the task decides in
  between, a forget among it, is decided before the read-out, and the task
  orders them), the reserved schemes and `:who :store` never pass a client
  depot, the request's ack carries the crossing's answer, and a replay or a
  resend of the request re-enters the same path by name. Weighed: a
  `:disallow` stream depot for store steps, which the stream topology could
  source beside `*offers`; it adds a depot and a second source for no
  property the continuation lacks.
- **The forward is stored, and every send is the stored bytes** (PR8).
  Without it, a crash between the crossing's commit and the append, then a
  forget of the source before the replay, leaves the replay nothing to send:
  a crossed copy lost, against "after it does not recall". With it, the
  target's gate sees one landing however many times it is sent.
- **Landing leases are bare, bound to one landing, and live as leases
  live** (PR9). Bare, because the copy must not die with its former owner
  (B case 4). Bound to one landing name, so no other act can spend it and a
  landing cannot spend another promotion's. Destroyed at their session's
  close like every lease (default 1's sub-pick), because a second lifetime
  rule would keep unused key pairs for ever, and the door keeps the session
  open in T until each promotion citing one of its landing leases is done
  or refused.
- **A landing that cannot get its lock is recorded as refused** (PR5), so
  every crossed promotion ends in done or refused.
- **No check against the other store** anywhere in the protocol. The read-
  out does not look at the target (T's class, permission or heads are T's
  gate's), and the landing's gate does not look at the source or the
  crossing (the model's `:landing-checks-source false`; phase 3's R6 pick:
  the offer carries its stood-on stamps). Sid's rule: order between the
  stores exists only through stood-on.

## State primitive selection

- `$$layers :forwards` (PState): one entry per crossed promotion, written
  once in the crossing's yes; durable because a replay after a restart
  needs it. Bounded by the promotions the layer's owner makes.
- Lease rows with `:public` and `:for` (PState, phase 2's and 3's): one per
  landing lease, deleted at the landing's decision or the session's close.
- The crossing's and the landing's answers and rows (PState, the existing
  records): as every act's.
- No TaskGlobal. The in-memory holder was weighed for the landing's lock and
  rejected: a worker restart between the read-out and the landing would
  lose a crossed copy. `K`, the ephemeral private key and the plaintext are
  locals of one event and are never stored in the clear.
- No external system.

## Resource usage analysis

### Disk usage (PStates), per promotion

- **Owner's home, kept:** the request's record and row (phase 1's sizes,
  about 400 bytes with a 60-byte base64 public key in the control value);
  the crossing's record, row and two stood-on entries (about 300 bytes);
  the forward (the copy's sealed bytes, value size + 28 bytes of nonce and
  tag, plus the 104-byte box and about 350 bytes of envelope). About 1.2 KB
  + the value, per crossed promotion, on L's home; a refused read-out keeps
  only the first two (about 700 bytes).
- **Landing lease row:** 48 (private) + 44 (public) + about 120 (the
  landing name, layer, session, batch) = about 210 bytes, from the lease
  until the landing's decision or the session's close.
- **The copy:** one sealed act in T, as any.
- **Depots:** the request in `*offers` (about 500 bytes); the landing in
  `*micro-offers` (the forward's size), once per send.

At a thousand promotions a day per person, the owner's home grows by about
1.2 MB a day plus the values promoted: small beside the values themselves.

### Memory usage (TaskGlobals)

None.

### Minimization

- The forward could keep only its sealed bytes and box (about the value +
  130 bytes) and rebuild the envelope from the request's control value on a
  replay, one seek more. Not taken: a byte-identical re-send is what makes
  a replayed landing answer from the record by its digest without a second
  mechanism, and it holds across a code change between the sends.
- The request's public key (44 bytes, 60 as base64) is the only new field
  a door sends; it replaces no field that exists.
- Nothing is duplicated to serve a read faster: the status is read from
  the records, not kept.

## Rig choices proposed

Numbered PR1 onward; the build copies the ones it keeps into RIG.md with
the next free R numbers. **First-record** marks a pick that decides what a
kept record carries; each is the simplest placeholder, and each is listed
for Sid in the receipt.

- **PR1, first-record: the request.** A control fact `:promote-request` on
  the source's entity in the owner's layer, with the value above (source,
  target, class, lease, public key, permission, replaces, subjects); its
  act stands on the source. Why: a promotion must be a fact in the owner's
  layer (Sid's phase 4, "a request act in the owner's layer"), and the
  landing's name, road and permission must be fixed before the first gate.
- **PR2, first-record: the crossing.** Named `[L nil :crossing uuid]`,
  `:who :store`, no permission, one `:crossed` fact on the source's entity
  with value `{:request req :source src-fid}`, standing on the source and the
  request; its recorded refusals `:source-erased` and `:source-has-no-value`
  under the same name. Why: the model's read-out, as phase 1 named it.
- **PR3, first-record: the landing.** Named `[T C :landing uuid]`, C the
  class the request carries (phase 1's `landing-name` gains the class);
  `:who` the requester, `:because-of` the request, standing on the source
  and the crossing with their stamps; one value fact on the source's e and
  k, carrying the request's replaces and subjects; the envelope's fact parts
  gain `:box`, accepted on a landing name only. Why: the names sharpening,
  the model's `forward`, and default 1 (the lock must travel boxed).
- **PR4, first-record: the landing lease.** A lease act with `{:count 1
  :landing <landing-name>}`; its row bare, with `:public` and `:for`. Why:
  the one place an opener can wait for the landing's decision.
- **PR5, first-record: `:landing-lock-gone`**, a recorded refusal of a
  landing whose lease is missing or bound to another landing. Why: a crossed
  promotion must end.
- **PR6: the sealed box.** X25519 (JDK `XDH`), the wrapping lock HMAC-SHA256
  over the shared secret with the label `softland/landing-box/v1` and both
  public keys, AES-GCM with a 12-byte nonce over the 32-byte lock. Can
  change without touching a record: a box is read only at a landing's
  decision, and a decided landing is answered from its record.
- **PR7: the read-out as a continuation** of the request's record, past a
  commit boundary; the stream-gate landing as a hop, never a depot record.
- **PR8: the stored forward** `[L :forwards req]`, and every send of a
  landing is those bytes.
- **PR9: landing leases** are bare, bound to one landing, and destroyed at
  their session's close like every lease; the door does not close a session
  in T while a promotion citing one of its landing leases is neither done
  nor refused.
- **PR10: the reservation on the micro depot.** A `:landing` name is taken
  only as a landing (one value fact citing a landing lease for that name);
  a `:crossing` name is refused on its face (`:reserved-scheme`); an
  `:offer` name citing a landing lease is `:no-such-lock`.
- **PR11: no target-kind check.** The request names any layer other than
  its own; the landing's gate decides with its ordinary checks (a landing
  into someone else's one-owner layer lacks a permission there). Why: the
  stream gate cannot read T's settings inside the request's event, and the
  permissions already say who may write where.
- **PR12: the read-out neither needs nor re-checks a permission** (the
  model's `exempt?`), so a revoke in L between the request and the
  read-out does not stop it; the landing's permission is checked in T by
  T's gate.
- **PR13: a value act's `:who` is not checked against `$$persons`** on
  either gate (only its subjects and a lease act's `:who` are), so the
  requester's forget after the read-out does not refuse the landing. Why:
  B case 4. The build confirms phase 3's gather keeps to this; if it reads
  `:who`, the landing is the exception.
- **PR14: `promotion-status`** as specified, with the model's precedence,
  and the statements at the point of promotion as data.
- **PR15: the test hold** (R3): `inject/hold!` on a request name stops the
  continuation before the read-out, so a test can order a forget before
  it; after `release!`, the door's resend continues the promotion. A test
  device only; in production the continuation always runs.

## What this stage changes in stages 1 to 3 (for the build)

- `rig.store.envelope`: `landing-name` takes the class; `:box` joins the
  fact parts on a landing name only; `:promote-request` joins the control
  facts with its value check; R13's list grows by it. `parse` still
  refuses `:crossing`/`:landing` names and `:who :store` from `*offers`.
- `rig.store.gate` and `module.clj`: the continuation (read-out, forward,
  stream landing); `$$layers` gains `:forwards` and the two lease row
  fields; the lease act mints a key pair when `:landing` is set.
- `rig.store.locks`: `keypair`, `box`, `unbox` (the probe's functions,
  total: nil on any failure, never a throw); `deliver-lock>`'s lease body
  unboxes when the row has `:public` and the fact a `:box`; `lease-locks`
  returns a landing row's public key only.
- The micro gate: `parse` takes a `:landing` name as a landing; the gather
  unboxes on the arrival task; `:landing-lock-gone` rides the skeleton to a
  recorded no; the lease act mints a key pair; `micro-lease` returns a
  landing row's public key only.
- `rig.store.client`: `lease-landing!` (the lease act and the public key),
  `promote!` (the request; its answer with the crossing's and the
  statements), `promotion-status`.
- New: `rig.store.promote` (the store-made crossing and landing offers,
  pure; the status function over three answers, pure; the statements).

## Namespaces and tests

`rig.store.promote-unit-test` (no cluster) and `rig.store.promote-test`
(in-process cluster, every run under `flock
/mnt/data/projects/rig-relay-2026-09-26/cluster.lock`), run by name, so
`gate_test.clj` never loads. The world is the model's (Alice, Bob, `:alice`,
`:alice-hand`, `:group`, the base; entities `:e0` to `:e3`; keys `:note`,
`:mention`). "Status" below is `promotion-status` as of now; "reads open"
and "reads erased" are `open-value>` through the read exit.

**Holding a step** (R3, global atoms, PR15). `inject/hold!` on a request
name stops the stream gate's continuation at one of two points,
`:before-read-out` or `:before-forward`; the record's processing ends there
with the request's answer (and, at the second point, the crossing's). After
`release!` the door's resend of the request, answered from the record,
continues the promotion from where it stopped. Phase 3's batch hold (the
one its revocation race uses) holds a landing already in `*micro-offers`;
where it is a crash rather than a hold, the `:before-forward` hold stands
in, and the test says so.

**Unit (no cluster).**

- U1. The box: `box` then `unbox` with the lease's private key gives the
  lock back; with any other private key, a changed byte of the box, or a
  public key of the wrong length, `unbox` is nil and never throws.
- U2. The store-made offers: `crossing-offer` and `landing-offer` from a
  request and a source are the shapes in PR2 and PR3; the landing's name is
  `[T C :landing uuid]` with the request's uuid and the carried class; its
  `:subjects` are the request's, never the source act's; its stood-on
  carries the source's and the crossing's stamps.
- U3. The status function over three answers gives the model's
  `promotion-status` for every combination (none, pending, crossed, done,
  refused at either step; stamps before and after the as-of moment; the
  landing above the frontier reads crossed).
- U4. The request's control value check: each malformed shape is
  `:malformed-control`; the lease name tagged for another layer or class,
  a 43-byte public key, a permission in another layer, 257 subjects.

**Cluster.**

- **T1 to T4, the model's B cases**, each played as scenarios.clj plays it,
  each asserting the model's `:values` and its `:shown` sequence, the
  closing read last:
  - **T1** "a value forget queued before the read-out: pending, then
    refused". Alice's note in `:alice`; a landing lease in `:group`; hold at
    `:before-read-out`; `promote!` → status pending; Alice forgets the note
    → status pending; release, resend → the read-out records
    `:source-erased`; closing status refused. Shown `[:pending :pending
    :refused]`; Alice's note reads erased. No landing was ever appended
    (`*micro-offers` has no record under the landing name).
  - **T2** "Alice forgotten before the read-out: pending, then refused".
    Hold at `:before-read-out`; `promote!` → pending; Alice's person forget
    (phase 2's fan-out, waited for on `:alice`'s home); release, resend →
    `:source-erased`; closing refused. Shown `[:pending :refused]`.
  - **T3** "a value forget after the read-out: crossed, then done; the copy
    stays". `promote!` with the landing held on the micro side → crossed;
    Alice forgets the note → crossed; the landing's batch runs → done;
    closing done. Shown `[:crossed :crossed :done :done]`; Alice's note
    reads erased, the group's copy reads open.
  - **T4** "Alice forgotten after the read-out: crossed, then done; the copy
    stays". As T3 with Alice's person forget in place of the value forget.
    Shown `[:crossed :done :done]`; the copy reads open. The landing's
    `:who` is Alice, forgotten: PR13 is what lets it land.
- **T5, both sides of the read-out line, by stamp.** In T3's history, a
  status read as of the request's stamp is pending, as of the crossing's
  stamp crossed, as of the landing's stamp done; in T1's, as of the
  crossing's stamp refused. The line is the crossing's stamp on `:alice`'s
  home, where the forget's stamp falls on one side of it.
- **T6, a retried request lands once.** The door resends the request under
  its name five times: twice while held before the read-out, once between
  the crossing and the landing, twice after the landing. Exactly one
  crossing record, one forward, one landing answer, one copy row in
  `:group`; every resend's ack carries the same request and crossing
  answers; the landing lease row is gone after the first landing; status
  done. Repeated with the base on the stream gate as T.
- **T7, a crash between the forward and the landing** (a failover in the
  rig: a worker restart).
  - (a) `inject/point! :after-forward` throws once after the append: the
    record replays, the request and the crossing answer from their
    records, the stored forward is appended again; the micro gate decides
    the landing once (a second record in `*micro-offers` under the same
    name, answered from the record, or collapsed in one batch by phase 3's
    F3); one copy.
  - (b) `:before-forward` throws once after the crossing's commit and before
    the append, and the test forgets the source before the replay: the
    replay re-sends the stored forward, and the copy lands. This is the case
    the model's `forward` would not re-send ("while the source can still be
    opened"); in the model the first send cannot be lost, so the outcomes
    agree.
  - (c) The landing's batch fails once on the micro gate (phase 3's crash
    hook): the batch retries whole; the copy is admitted once and the lease
    consumed once.
- **T8, a forget of the source after the landing.** T3's history run to
  done, then Alice forgets the source: the copy reads open, the source
  erased. The depot check (as T11) finds the source's plaintext under no
  live lock; the forward's and the micro depot's landing bytes open only
  under the copy's record lock. Then the copy is forgotten in the base on
  the stream gate (phase 2's forget of a record lock; the group's excision
  is phase 3's to offer, and the test runs it there if phase 3 has it):
  nothing anywhere opens it.
- **T9, a landing refused by the target.** (a) Between the crossing and the
  landing, the operator revokes Alice's permission in `:group` (in the
  group, where it lives): the landing is `:permission-revoked`, status
  refused with that reason; the lease row is gone; the landing's bytes in
  `*micro-offers` and in `:forwards` open under no lock the store holds.
  (b) Bob writes the same entity and key in `:group` after the request: the
  landing is `:stale-replaces`. (c) Alice's session in `:group` is closed
  before the landing: `:landing-lock-gone`, recorded. Each: crossed, then
  refused; Alice's source still reads open (the promotion is not a forget).
- **T10, the base before and after its re-class.** (a) No group yet, the
  base one-owner on the stream gate: `promote!` into the base lands through
  the stream gate's hop (no record in any depot), the copy in the base
  wrapped by the no-owner row, status done. (b) After the re-class: the same
  promotion lands through `*micro-offers`. (c) The request made before the
  re-class, with the forward held; the re-class decided; released: the
  landing, named `[base :by-layer :landing uuid]`, reaches the stream gate,
  which refuses it `:class-mismatch` (recorded), and consumes its lease;
  status refused.
- **T11, nothing in any depot opens after the source value's forget before
  the read-out.** After T1's and T2's histories: every record of `*offers`
  and `*micro-offers`, read raw (`foreign-depot-read`), and every box in
  them, tried under every lock the store still holds (lock rows and record
  locks unwrapped with every live person lock, every symmetric lease row,
  every landing lease's private key, the live person locks themselves):
  none gives the source's plaintext; no landing record exists; the unused
  landing lease's private key opens no box anywhere. Then Alice's session
  in `:group` closes and the lease row is gone.
- **T12, the reservation.** A door's `:landing` name or `:crossing` name
  on `*offers` is `:reserved-scheme`; a door's `:crossing` name on
  `*micro-offers` is `:reserved-scheme`; an `:offer` name citing a landing
  lease is `:no-such-lock`; a landing lease act of count 2 is
  `:malformed-control`.
- **T13, whose the copy is.** Alice's `:mention` of Bob in `:alice`,
  promoted into `:group`: Alice's person forget leaves the copy open; Bob's
  erases it (about one person in a shared layer, 7b as written). A plain
  note about no one survives both. The copy is about whoever the target's
  grammar and the request's tool name, not its former owner.
- **T14, what is said.** `promote!` returns `:pending` or `:crossed` with
  the statements of "What is said at the point of promotion";
  `promotion-status` returns the statement of the state it found.

## What later stages consume, and where it is

- **Reads (phase 5).** `promotion-status` and the statements; the crossing
  is an ordinary fact in the owner's layer, shown by reads with its stamp;
  a read of a `:promote-request` row gets `:promotion` from the status
  query (one call per such row in the exit). Ruling 3's "agent session
  layers may default to none, with the line added on promotion": under
  default 4 every read is recorded, so nothing is added; were agent layers
  to record none, the read-out is where the line belongs (it is the read
  that happened). Named, not built.
- **Tools and grammars (phase 6).** The copy's subjects are the target
  gate's to compute, as for any value: the key's grammar over the copy and
  the carried `:subjects`, which a tool fills from its signature through the
  request. Phase 6 replaces the rig's grammar with facts; nothing here
  changes.
- **The numbers (phase 7).** No new number. **Replays (phase 8).** The B
  cases; the differences to report: a landing lease act in the target
  before each request (the model has none), the read-out as a continuation
  (the same order), and the re-send from the stored forward (T7 b).

## Design difficulty log

**How the landing's lock reaches the target.** This was the whole stage,
and it was not close once I had the three forgets and the one-event rule
side by side. My first design put a lock minted at the read-out in a row on
the owner's task for the micro gate to read; it fell on consumption,
because the micro gate cannot delete a stream gate's row, and a message
back would make two places consume one lock and leave a window where the
copy's forget misses it. My second had the micro gate mint a symmetric
lease that the stream gate fetches in a hop before the read-out's event.
That one works, and it was genuinely competitive: no new cryptography, two
hops per promotion. It lost on two things I could name: a bare lock
travels between tasks, which phase 3 has as its rule against, and the
read-out would need the micro store reachable, a read of the other store
in the promotion's path. The sealed box removed both at the price of a
key agreement the JDK has, which I probed rather than assumed. Then B case
4 caught me: phase 2 and phase 3 seal lease rows under the lease act's
writer, and a landing lease under Alice's lock would die with her after the
read-out. So landing leases are bare; that is what the case needs, and I
checked what a bare row retains (nothing before the read-out, the copy after
it) before keeping it.

**The micro depot and the reserved scheme.** I wanted the reservation to be
structural, a `:disallow` depot for landings, and dropped it when
microbatch.md reminded me that each `source>` is its own dataflow section:
phase 3's single fold over the batch's skeletons could not see both. The
choice left is to take a `:landing` name on `*micro-offers` only as a
landing. It is weaker, and I say so in open question 1 rather than dress
it up; the ruling "no signing, a later edition can add the part" is where
the strong form belongs.

**The read-out's place in the event flow.** The model queues the read-out
as an offer; a depot record for it looked faithful. Traced, it added a
depot and a second source, and the order it bought is the order the task
already gives a continuation. Not close.

**The stored forward.** The model's `forward` re-sends only while the
source opens. I nearly copied that, and then traced Rama's order: the
append follows the commit, so there is a crash window the model does not
have, and a forget inside it would lose a crossed copy. Storing the
landing was forced once that was on the page.

**A missing landing lease: face or recorded.** Phase 2 made a missing lock
a face refusal for good reasons (a door resends under a new lease). A
landing is never resent under another lease, and a face refusal would leave
the promotion crossed for ever. Recorded, then; forced once I asked what
the person would see.

**What I did not settle and did not pretend to:** whether a landing lease
should outlive its session (open question 2), and whether a door should be
able to forge a landing at all (open question 1).

## Self-validation against `artifact-plan-validation.md`

- **Query topology.** `promotion-status`: reads counted per state, the
  second task reached only when the crossing is a yes; no empty read issued
  where the state already decided. Pass.
- **PState schemas.** Two fields and two optional lease fields, fully typed,
  no `Object`; `:forwards` subindexed; no new PState, with the rejected
  `$$promotions` costed. Pass.
- **Partitioning.** Placement from `f` first; the table flat at 4.10
  weighted seeks at N = 1, 16 and 128; the write path's terms independent
  of N. Pass.
- **Topologies.** No new topology; the stream gate's continuation is in the
  topology that owns `$$layers`; at most one stream topology holds. Pass.
- **Production readiness.** Every refusal is data: the box functions are
  total, the read-out's refusals are recorded, the landing's missing lease
  is recorded; no throw on any offer. Crash at each boundary traced (T7):
  before the crossing's commit (nothing visible, the replay decides fresh,
  and no landing exists yet), after it (the record path re-sends the stored
  forward), after the append (a duplicate answered by name), in the
  landing's batch (retried whole). Pass.
- **Internal depot usage.** The one append (`*micro-offers`) follows a
  commit boundary `(|direct (ops/current-task-id))`, uses `:append-ack`
  (never `:ack`: the depot is not consumed by the appending stream
  topology, but `:append-ack` is all the forward needs), and runs on the
  task the depot's partitioner would pick. Pass.
- **Cross-topology correctness.** The stream gate writes only `$$layers`;
  the micro gate only its own PStates; neither writes the other's. The
  micro gate reads nothing of the stream store for a landing. Pass.
- **Stream topology correctness.** No partitioner inside the read-out's or
  the stream landing's decision event; every write idempotent under replay;
  randomness bound before the decision. Pass.
- **In-memory state.** None. Pass.
- **Minimality.** Delete the landing lease: no opener reaches the target's
  task by a path the depot never sees (default 1 fails). Delete the box:
  the lease's lock would have to reach the read-out by a hop (the
  alternative costed above). Delete the stored forward: T7 b loses a
  crossed copy ("after it does not recall" fails). Delete the class in the
  landing name: every promotion into the base before its re-class is
  refused on its face at the micro gate (`:wrong-gate`), default 6 fails.
  Delete `:landing-lock-gone`: a crossed promotion can stay crossed for
  ever. Delete `promotion-status`: "the read says which" fails. Delete the
  statements: the sharpening's "say all of this at the point of promotion"
  fails. Delete PR10: the micro gate either refuses the store's landings or
  takes any landing name without its lease. Each stays.
- **Throughput.** Per promotion: 7 seeks and 0.26 ms of cryptography on the
  owner's home, 1 hop and 1 append, then an ordinary sealed act at the
  target plus 0.08 ms. The hop alternative costs 2 more hops per promotion
  and a read on the micro store's task; the back-channel alternative an
  extra stream act per promotion. Promotions are rare beside ordinary acts;
  nothing here touches the ordinary offer's path.
- **Spec coverage.** Sid's phase 4, clause by clause: the request act in the
  owner's layer (step 1, PR1); the read-out opening the value through its
  lock on the owner's task and writing the crossing fact there (step 2,
  PR2); a landing offer named from the request under a reserved scheme
  (step 4, Names, PR3, PR10); pending with two states (step 5,
  `promotion-status`); a forget before the read-out refuses, after it does
  not recall (the forgets walked; T1 to T4; T11); the tests: both sides of
  the read-out line (T1 to T5), a retried request landing once (T6), a
  failover between forward and landing (T7). The sharpening: the copy's
  subjects (step 4, T13); crossed does not promise done (T9, T10 c); say it
  at the point of promotion (step 6, T14); the uniformity rule (the
  protocol section, PR1 to PR5). Sid's rule on order between the stores: no
  cross-store check anywhere; the landing stands on the crossing. Default
  1: the hard question. Default 2: stamps. Default 6: T10. Pass, with the
  open questions below.

## Open questions (for Sid, or for the build's first check)

1. **The reservation on `*micro-offers` is by rule, not by channel.** A door
   that deliberately makes a landing name for a request it sent, and seals
   to its own landing lease, can land content no read-out made. Built: the
   rule PR10; the strong form is a store signature on its landings, the
   part the ruling defers ("no signing; a later edition can add the part").
2. **A landing lease dies with its session** (PR9), so a requester whose
   session in the target closes before the landing sees crossed, then
   refused (`:landing-lock-gone`). The door waits; a crashed door's session
   (open item 82) may not. Should a landing lease outlive its session, at
   the cost of a second lifetime rule and key pairs kept after abandoned
   requests?
3. **A person's forget against a read-out** (PROGRESS.md, open): the rig
   orders them on the owner's home task, where both are decided; the
   fan-out's order on other tasks does not reach the read-out. Reasoned,
   then tested by T2 and T4.
4. **A copy about someone already forgotten** is refused by the target's
   gate as any such write is (phase 3's `:person-forgotten`): crossed, then
   refused. "Writes about someone already forgotten" is open.
5. **A re-class of the target between the request and the landing** refuses
   the landing (T10 c), because the name fixed before the first gate names
   the old gate. The ruling's order; stated so Sid sees the consequence.
6. **Promotion out of a re-classed one-owner layer** is not built: its
   requests would reach the micro gate, which refuses `:promote-request` as
   an unknown control fact.
7. **For the build:** whether `ack-return>` called once at the end of the
   continuation behaves as the plan assumes (phase 1 calls it once per
   record); and whether phase 3's gather reads a value act's `:who` against
   `$$persons` (PR13 needs it not to). Both are checks, not designs.
