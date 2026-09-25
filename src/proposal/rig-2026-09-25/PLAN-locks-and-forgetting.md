# Plan — stage 2, "locks-and-forgetting"

<!-- Phase 1 Step 5 of the rama skill, for the second entry of DECOMPOSITION.json.
Written 25 September 2026 by a fresh-context session (Claude Fable 5.1, effort
max). Plan only: no module or test code. Sources: SPEC.md and everything it
names (PROGRESS.md "Now" 9-195; the model README 42-152; model.clj in full,
`baseline`; scenarios.clj); RIG.md (phase 0, R1 to R14); IMPLICIT_SPEC.md
(cited as I-xx, OPn, RDn, En, On, Dn); DECOMPOSITION.json (the
"locks-and-forgetting" scope); PLAN-stream-store.md as validated (cited as
"stage 1", its picks P1 to P16 and fixes [F n]) and
PLAN_VALIDATION-stream-store.md; the rama skill's references (stream,
pstate-schema, paths, depot-reference "Depot Trimming", depot-migration,
app-design, depot-design, patterns, microbatch, testing, unique-ids,
task-globals, artifact-plan, artifact-plan-validation). Where a line here
summarises a source, the source wins. Vocabulary: "key" is a fact's key;
"lock" is an encryption key; never one word for both. Planned against
stage 1's plan, not its code (RIG.md "Restarted").

Revised 26 September 2026 by a fresh-context session (Claude Opus 5.5,
effort max) for the forget road taken tonight as a default: nothing that
could open a value sits in the depot (store-next-2026-09-25/CONCLUSION.md,
R1 with its riders; R2 for stamps). Tonight's defaults are Sid's proposals
taken as defaults, not rulings; this plan names them as defaults. Sources
added for the revision: CONCLUSION.md "Eight rulings" (R1 whole, R2),
PROGRESS.md "Now" on main, stage 1's code (`src/rig/store/*.clj`),
BENCH_NOTES-locks.md and runs/phase7-lock-growth.txt. Where the revision and
the 25 September text differ, the revision wins. -->

## Revision, 26 September

Each change, with its reason in a line. Section by section the text below
carries the change; the new mechanisms sit together under "The door and the
lease road".

1. **Values are sealed at the door, not by the gate.** The default: nothing
   that could open a value sits in the depot (R1); the gate's own sealing of
   25 September left the plaintext in `*offers` for ever.
2. **A lease act.** The gate mints a batch of locks in an ordinary act
   answered by name and writes them as lease rows on the layer's home (R1:
   "a lease act is an offer like any other").
3. **A query, `lease-locks [layer session]`, through which the door takes
   the leased locks' plaintext.** A path the depot never sees; locks are for
   forgetting, not for hiding from the operator (ruled, PROGRESS "Now").
4. **The offer's value fact carries `:sealed` bytes and a `:lock-id` in
   place of a plaintext `:v`, and the log row keeps those sealed bytes as
   offered.** One ciphertext in the depot and the log, dying with one lock
   (first-record).
5. **A lock id is `[lease-name i]`, minted at the lease.** The lock now
   exists before the value, so `[:value fid]` and `[:act name]` cannot name
   it (first-record).
6. **One delivery function, `deliver-lock>`, gives the gate each cited lock
   at decision.** Its lease body reads the lease row on the gate's task; the
   holder road (the 25 September text's road for a kept store) is described
   as a replacement body and not built (R1's alternative).
7. **The gate re-wraps each delivered lock under the value's own subjects.**
   Without the re-wrap a marked value would open with the owner's lock
   alone (R1); the wrap table itself stands.
8. **A lease row is consumed at decision whatever the decision.** Admitted,
   it becomes the value's lock; refused, it is destroyed, so no depot bytes
   stay openable that no forget can name (R1).
9. **The digest splits: a parts digest over the offer without its value
   bytes, and a value digest per value fact, an HMAC over the plaintext
   keyed by the value's lock.** R1's first rider; the 25 September text kept
   a digest over plaintext under a constant secret, "Unchanged", which
   outlived every forget.
10. **The record path opens a resend with the recorded lock and recomputes
    its value digests; after a forget it skips the forgotten values.** R1's
    riders: a resend is sealed again, and the price of forgetting.
11. **Refusal order: everything up to and including the delivery is refused
    on the offer's face, unrecorded; everything after it is recorded and
    destroys the cited lease rows.** R1: a missing lock is refused on its
    face so a resend is still allowed; a refused lease is destroyed.
    `:value-shape`, `:too-many-subjects` and stage 1's value-domain checks
    move behind the delivery, because the gate sees a value only once it
    opens it.
12. **A `:session-closed` act destroys a session's unconsumed lease rows in
    a layer.** The default: unconsumed lease rows are destroyed when their
    session closes; when a session layer closes stays Sid's open item 82.
13. **The depot's plaintext gap (O2, old L2) is gone.** The depot holds
    sealed bytes, lock ids and control values only; what stays named is the
    substrate and the door's own memory.
14. **P6 widened is checked in three places.** CONCLUSION "Also going into
    the model": the depot, the lease rows and the lock store each hold
    nothing that opens or confirms a forgotten value.
15. **Raw bytes (`byte/1`) in every sealed or wrapped slot, not base64
    text.** The bench measured 169 bytes a lock row raw against 189 as
    base64 (runs/phase7-lock-growth.txt), `byte/1` ran as a schema class
    there (BENCH_NOTES-locks.md), and nothing needs text.
16. **Stamps come from `rig.store.clock/next-stamp`, the hybrid clock
    (ms × 65536 + counter) being built tonight.** CONCLUSION R2, a default;
    every date this plan writes is such a stamp.
17. **`rig.store.crypto` and `rig.store.lock` fold into one public
    namespace, `rig.store.locks`.** The interface tonight's parallel stages
    build against (seal, open, wrap, unwrap, the delivery, `open-value>`).
18. **`read-as-of` goes through `open-value>`, and a value forget calls the
    read exit's purge by value id in its own event.** Point and pattern
    reads are the read exit's (PLAN-read-exit.md, planned in parallel).
19. **module.clj gets one install line per concern where Rama allows it;
    the gate event's added steps are call sites inside stage 1's one
    event.** They must commit in its one atomic group (SPEC "What Rama
    showed" 1).
20. **The model has no lease act yet.** The A cases run with a lease before
    each offer; phase 8 reports the lease acts as a difference by
    construction.
21. **Tests added** for the missing lock and its resend, lease consumption
    three ways, the raw depot after a forget, a resend with other content
    before and after a forget, a resent lease, and the two crashes.

Picks that stand unchanged: the wrap table and `wrap-closed` (L3's key
wrapping, now over bytes); rows for personal and hand layers, record locks
for agent layers, `:own-row` overriding (L5's placement); per-act grain's
union wrap (L6); `$$persons` on every task with its fan-out (L1, L9);
persons as data in `:people` (L7, L8); the forget acts and their effects
(L10, L16, L17); retracts and control facts unsealed (L14); size tracking
off (L15); the tool's subjects joining (L19); the grammar map (L12's
reading, now after the delivery); the ledger; `:by-stamp`; one stream
topology; the depot `*offers`, `hash-by :layer`.

## Scope of this stage, in one paragraph

Sid's phase 2 (SPEC.md): real encryption, kept simple. Every value gets its
own small symmetric lock at write (leased by the gate, sealed over the value
at the door, revised 26 September), wrapped under the person locks of the
people it is about, as `model.clj`'s `wrap` reads it under the `baseline`
entries `:wrap :owner-required` and `:lock-subjects :fact`; the grain
setting (per value, per act); the lock rows in a lock store for personal and
hand layers, on the same task as their values, and in the record for agent
layers, a mark overriding; the person locks and where they live; value
forget (the row deleted, or the record's lock excised), person forget (the
person lock destroyed), every forget a fact; and a read as of a moment that
shows an erasure only as its date. The subjects of a value come from the
layer's owner, the tool, and the one grammar the model has (`:mention`
names people), in a form stage 6 can move into facts. The stage extends
stage 1's stream gate and its `$$layers` PState on the one-owner side; the
shared layers' locks in the record are stage 3's, promotion's read-out is
stage 4's, point and pattern reads with visibility are stage 5's. Later
stages are black boxes: this plan says what state it leaves and where.

The rulings this plan implements are I-L1 to I-L8, I-P5 (locks fixed at
write), D2, D3, D9, D12, and the operations OP8 (its lock effect), OP9,
OP10, OP11 (as "no restore"), OP12 (as "not this stage"), RD4 (the erasure
part), RD7; the matrix rows of E2 (forget and person-forget rows), E3 (L1 ×
grain switch, L2 × value forget), E5, E6 and E9 (G0, G1, G2 as far as
subjects go). The open items that fall to this stage are O2, O7, O8, O11 to
O16, plus O1's forget-related refusals and O20's subject-extraction half;
each pick is under "Rig choices proposed", never as a ruling.

## The shapes every section below uses

Stage 1's shapes (name, offer, fact, permission id, digest, answer record,
stood-on map, log entry, stamp) stand unchanged. Added or changed here:

**Control keys (D2).** Facts the store itself acts on get no lock and their
value slot stays plaintext canonical EDN (P12): the model's `#{:forget
:lock-grain :class :promote-request :crossed :permission :revoke}`, stage 1's
`:kind` and `:owner`, and this stage's `:person`, `:forget-person`,
`:lease` and `:session-closed` (the last two revised 26 September).
**Placed keys (P4)** grow by `:person` and `:forget-person`: an act whose
every fact key is placed carries a name with class nil. `:lease` and
`:session-closed` are not placed: their acts are tagged with the layer's
class like any act into it (L20).

**Value fact.** Any fact whose key is not a control key and whose value slot
is not nil (a retract, P13, has nothing to lock and gets none, L14).

**Grammar** (ruling 8; `fact-subjects` in the model; stage 6 moves it into
facts): a map from fact key to a grammar entry, a constant in
`rig.store.grammar` for this stage:

```
{:mention {:subjects-at [:persons]}}   ; the value's :persons collection names people
```

`(subjects-of grammars k v)` → the set of person ids the value names: the
keywords found at `:subjects-at` in `v` when `v` is a map and that position
holds a collection of keywords; refused as data otherwise (L12). A key with
no entry names no one.

**Own subjects of a value** (the sharpening of rulings 7, 7b, 8: "the three
sources applied to that fact"): `owner ∪ grammar(k, v) ∪ carried`, where
`owner` is the layer's owner from `:settings` (nil in shared layers),
`grammar` is above, and `carried` is the offer's `:subjects` part (the
tool's, P15). **The act's subject slot** (ruling 8, "for finding") is the
union of its facts' own subjects, on the answer record as stage 1 keeps it,
now including the grammar's. The union is capped at 256 persons (L13).

**Wrap** (`wrap` in the model under `:owner-required`): `{:required [p ...]
:any-of [p ...]}`, sorted vectors of person ids, fixed at write:

- marked `:die-with-any`: required = own subjects (owner included), any-of
  empty;
- unmarked, the layer has an owner: required = `[owner]`, any-of empty;
- unmarked, no owner (shared layers, stage 3): required empty, any-of = own
  subjects (empty when the value is about no one).

A wrap opens while every required person's lock is alive and, when any-of is
non-empty, at least one any-of person's lock is alive (`wrap-closed`).

**Lock id** (revised 26 September; first-record, L21): `[lease-name i]`,
the name of the lease act that minted the lock and the lock's index in that
batch, `i` a long in 0..n-1. The gate and the door compute the ids of a
lease from its name and count alone (`locks/lease-ids`), so a resent lease
returns the same ids. Under per-value grain every value fact of an act
cites its own id; under per-act grain every value fact cites the one id of
the act (L6, L30). The lock keeps its id for life: lease row, then lock row
or record lock, then the ledger's key once erased. (The 25 September ids
`[:value fid]` and `[:act name]` are gone: the lock exists before the value.)

**Lock record** (the wrapped value lock as stored, in a lock row or in the
log row):

```
{:required [p ...]  :any-of [p ...]          ; the wrap
 :blob     bytes | nil                       ; K sealed under the required chain; nil when any-of is non-empty
 :any-blobs {p bytes, ...} | nil}            ; per any-of person: the required-sealed K sealed under that person's lock
```

where `K` is the value lock (32 random bytes), "seal" is AES-256-GCM with a
fresh 12-byte nonce prepended to the ciphertext and tag, the required chain
seals K under each required person's lock in sorted order, and every slot
is raw bytes, `byte/1` (L4, revised). An empty wrap stores K bare in
`:blob` (shared layers, a value about no one: stage 3). The byte layout
(nonce, ciphertext, tag) is first-record (L3).

**Sealed fact** (the offer's form of a value fact, revised 26 September;
first-record, L24): `{:e :k :sealed bytes :lock-id [lease-name i]
:replaces :mark}`, no `:v`. `:sealed` is `locks/seal` of K over the UTF-8
bytes of the value's canonical EDN (P12's text), made by the door. A control
fact keeps `:v` as plaintext EDN and has neither part; a retract keeps `:v
nil` and has neither.

**Log row** (stage 1's `{:e :k :v :replaces :mark}`) gains four fields
(revised 26 September; first-record, L24): `:sealed`, the offer's sealed
bytes exactly as they came through the depot, for a value fact (nil
otherwise); `:v` stays plaintext EDN text for a control fact and nil for a
value fact and a retract; `:lock-id` as above (nil for control facts and
retracts); `:lock`, the lock record when the lock is kept in the record,
nil for a row lock, nil again after an excision; `:digest`, the value
digest (32 bytes, L26), nil for control facts and retracts.

**Value digest** (L26): HMAC-SHA256 keyed by K over the value's canonical
EDN bytes. It confirms a guess only to a holder of K, so it dies with the
lock. **Parts digest**: stage 1's `env/digest` (HMAC under the rig secret)
over the offer minus its name with every `:sealed` removed; `:lock-id`
stays in, so a resend must cite the same locks (L26).

**Lease act** (L20): an act into the layer, tagged with its class, one
control fact `{:e session :k :lease :v {:count n}}`, `n` a long in 1..256.
**Lease row** (L22): `{:under p :sealed bytes}` at `$$layers [layer :leases
session lock-id]`: K sealed under person `p`'s lock, `p` the session owner,
which in a one-owner layer is the layer's owner (L23). **Session close**
(L28): an act into the layer with one control fact `{:e session :k
:session-closed :v {:session session}}`.

**Lock row**: the lock record under its lock id in the layer's `:locks`
map. A value has a row when its layer's kind is personal or hand, or when
it is marked `:own-row`; otherwise the record holds its lock (ruling 7; D9:
the mark overrides in every layer).

**Erasure ledger entry**: under a lock id in the layer's `:erased` map,
`{:stamp long :how :row-deleted | :excised}`: the date a value forget
erased that lock. A wrap closed by a person forget has no entry; its date is
computed from the person locks (below).

**Person lock**: `{:lock bytes-or-nil :erased-at long-or-nil}` under the
person id in `$$persons`, on every task. `:lock` is 32 random bytes made by
the gate at the person's making act and nil once destroyed; `:erased-at` is
the stamp of the forget-person fact (a `rig.store.clock` stamp, R2).

**Erasure of a value** (`erasure` in the model), computed where the value
is: the ledger entry for its lock id if present (the value forget's date,
O14), else `wrap-closed` over `$$persons`: closed when a required person is
erased or every any-of person is; its date is the earliest of these
candidates: each dead required person's `:erased-at`, and, only when every
any-of person is dead, the latest any-of `:erased-at` (the model's `first
(sort-by :order ...)` over exactly those candidates, with stamps in place of
`:order`; all person forgets are stamped on one task, so their stamps are a
total order, L8).

**Forget acts.** A value forget is an act in the value's layer, tag class
nil, by the layer's owner (citing their own-layer permission) or the
operator, with one fact `{:e e :k :forget :v {:target fid}}`, standing on
the target (`op-forget-value`). A person forget is an operator act in the
store layer `:people` with one fact `{:e p :k :forget-person :v {:person
p}}` (L7, L8). A person is made by an operator act in `:people` with one
fact `{:e p :k :person :v {:id p}}` (L7).

## The door and the lease road (revised 26 September)

The default this section implements (CONCLUSION R1, taken tonight as a
default, not a ruling): nothing that could open a value sits in the depot.
Every mechanism below is on the layer's home task, inside stage 1's one
decision event or in a query topology routed there.

### The door

The door is the offerer's side: `rig.store.client` in the rig, operator
code, trusted (ruled: "the gate reads every value it matches on"; locks are
for forgetting, not for hiding from the operator). For an act with value
facts into layer L under session s, it:

1. **Leases.** Offers a lease act into L (name `[L class :offer uuid7]`,
   its `:who`, a `:permission` covering L, `:session s`, the one `:lease`
   fact with `n`), through `offer-until-answered!`. The yes answer carries
   `:lock-ids`, `(lease-ids name n)`, from the ack or, on a resend, from the
   record: the same ids.
2. **Takes the locks.** `foreign-invoke-query` of `lease-locks [L s]` →
   `{:grain g :locks {lock-id K}}`: every unconsumed lock leased to s in L,
   in plaintext, and the grain in force when the query ran. The door holds
   them in memory until it uses them or closes the session; a restarted door
   takes them again by the same query.
3. **Seals.** Under per-value grain each value fact under a lock of its
   own; under per-act grain every value fact of the act under one lock.
   `(locks/seal K (canonical-bytes v))`, and the fact goes out as a sealed
   fact citing that lock's id. Control facts and retracts go out as stage 1
   sends them.
4. **Offers**, and on an error resends under the same name (P6's road). It
   keeps the plaintext and the locks until answered; a resend may be sealed
   again (new nonces, other bytes) under the same lock ids, and answers the
   same (the value digest is over the plaintext).
5. **Closes.** At the session's end, one `:session-closed` act into each
   layer it leased in, only after every offer citing those leases is
   answered.

The door never sends a lock or a plaintext value through the depot. It
cites only lock ids it took from a lease's answer (see "The missing lock").

### The lease act and its answer

- An ordinary act into L, tagged with L's class (not a placed key), decided
  on L's home by stage 1's checks in order. The permission check treats
  `:lease` and `:session-closed` like a value write: R13's list of control
  facts a non-operator may write grows from `:lock-grain` and `:forget` (L10)
  by these two, for anyone whose cited permission covers L. A `:count` that
  is not a long in 1..256 is `:malformed-control` (R13).
- Added reads when undecided: `$$persons [owner]` (the owner from
  `:settings`). Added recorded refusals: `:no-such-person` (the owner has no
  entry) and `:person-forgotten` (the owner's lock is destroyed), after stage
  1's list, as L11.
- On a yes: n fresh 32-byte locks, bound before `decide` from
  `SecureRandom` (as stage 1 binds the wall), each sealed under the owner's
  person lock into a lease row at `[L :leases s (lease-name i)]`, one
  `termval` each; the answer record as stage 1; the log row of the `:lease`
  fact (a control fact, plaintext); a `:by-stamp` entry. The ack adds
  `:lock-ids`.
- Answered by name: a resend or a replay takes the record path (record
  first, [F4]), writes nothing and returns the same `:lock-ids`, because the
  ids are a function of the name and the count, and the parts digest makes
  the count the same. A crash before the lease's commit leaves nothing: the
  replay mints fresh bytes under the same ids, which no door has seen (its
  ack never arrived). A crash after the commit: the replay answers from the
  record, so the rows the door takes are the rows that stand.

### Lease rows

- **Where:** `$$layers [layer :leases session lock-id]`, on the layer's
  home, beside `:locks`: same key and partitioner as the layer, so a field of
  its value by the merge rule (L22). "In the lock store": the layer's lock
  store is `:leases` and `:locks` together.
- **Keyed how:** session first, then lock id; both levels subindexed, size
  tracking off. Session first makes a session close one direct delete of
  the session's subindexed map (pstate-schema.md, "Deleting Subindexed
  Structures": delete the structure itself, never a parent), and makes
  `lease-locks` one range read of the session's rows.
- **Wrapped under whom:** the session owner's person lock. In a one-owner
  layer the session owner is the layer's owner (L23): every session writing
  a one-owner layer writes for its owner, and the owner's lock is the one
  person lock the gate reads for every act there. A forget of the owner
  therefore makes every unconsumed lease in her layers unopenable at the
  fan-out, with no write to the rows.
- **Marked with the session:** by the key. A delivery looks under the
  offer's own `:session`; a row leased to another session is not found
  (missing) and is left alone.

### The delivery function

- **Signature:** `(deliver-lock> *layer *session *lock-id *persons :>
  *lock)`, a `deframafn` in `rig.store.locks` that runs on the layer's home
  inside the gate's event. `*persons` is the map of `$$persons` entries the
  event has read (the owner's at least), which is "the task's state" beside
  the PStates themselves. `*lock` is the 32 lock bytes, or nil for missing.
  Total: nil on any failure, never a throw. PStates are reached through
  `<<with-substitutions` (dataflow.md; module-scoped PStates in a
  `deframafn` body) or passed in; no partitioner inside.
- **The lease body (built):** `(local-select> [(keypath *layer :leases
  *session *lock-id)] $$layers :> *row)`, one seek; nil → nil; else the
  entry of the row's `:under` person from `*persons` (read from `$$persons`
  only when absent) → a destroyed lock → nil; else `(unwrap person-lock
  (:sealed row))` → K, or nil when it does not open. The pure half, `(unlease
  row person-entry) → K | nil`, is public, so a gate that keeps lease rows
  in a PState of its own can call it over a row it read (stage 3 is a black
  box; nothing here assumes it does).
- **The holder body (described, not built; the 25 September road):** the
  door hands each lock, before the append, to a query topology that puts it
  into a bounded per-task TaskGlobal holder under `[session lock-id]`
  (task-globals.md: a query may mutate a TaskGlobal synchronously); the body
  reads the holder, removes the entry, and returns K or nil. The gate's
  decision code does not change: the same call, the same nil, the same face
  refusal. What changes around it: the lease act, the lease rows and
  `lease-locks` go; `:session-closed` clears the session's holder entries; a
  worker restart empties the holder, so an undecided offer after a restart
  is refused `:no-such-lock` until the door hands its locks again (R1: "the
  holder is empty after a worker restart"); an offer decided before the
  restart takes the record path, which needs only recorded locks. Its cost:
  one round trip per offer (R1).

### Re-wrap under the value's subjects

At decision, per value fact: its own subjects (owner ∪ grammar over the
opened plaintext ∪ carried, "The shapes") → its wrap by the table (owner
required in one-owner layers; 7b among the other subjects; `:die-with-any`
makes every subject required) → `(locks/wrap K wrap persons)` → the lock
record, written as the value's lock row `[layer :locks lock-id]` in personal
and hand layers or under `:own-row`, else into the row's `:lock` (agent
sessions; group layers and the base are stage 3's). Per-act grain: the one K
of the act, one wrap over the act's union, placed by L6. K's bytes do not
change, so the depot's sealed bytes and the log's are the same ciphertext
under the same lock; only the lock's wrapping changes (a re-encoding, which
the ruling on meaning allows). Without the re-wrap, a marked value would
open with the owner's lock alone (R1).

### The digest and the resend check

- **Parts digest** (the answer record's `:digest`, a String as in stage 1):
  `env/digest` over the offer minus its name, every `:sealed` removed. It
  covers who, layer, class, permission, session, stood-on, because-of,
  claimed-when, subjects, and per fact e, k, replaces, mark, lock id and any
  control value. Nothing in it is a value (P6 stands for what is not a
  value).
- **Value digest** (the log row's `:digest`, 32 bytes): HMAC-SHA256 keyed by
  the value's lock over the plaintext's canonical EDN bytes, never over the
  sealed bytes, because a resend sealed again has other bytes (R1's rider).
  Written only for a yes: a refused act has no rows, and its leases are
  destroyed.
- **The record path** (the name has a record on this task), in order: the
  parts digest differs → `:name-taken`, on the face (stage 1). It matches
  and the record is a no → the recorded answer. It matches and the record is
  a yes → for each value fact of the resend, the recorded lock: the row's
  lock id (equal to the cited one, since the parts digest covers it), then
  the lock row or the row's `:lock`, unwrapped with `$$persons`. Where it
  opens, the gate opens the resend's sealed bytes with it and recomputes the
  value digest under it; bytes that do not open or a digest that differs →
  `:name-taken`. Where it does not open (the value forgotten, or its wrap
  closed by a person forget), that value is not checked. Every value checked
  or skipped → the recorded answer. The record path writes nothing.
- **The client's `lookup`** compares the parts digest only; the value check
  is the gate's, on a resend (P6's [F11] road: the gate computes both sides).

### The answer after a forget

A same-name offer after one of its values is forgotten: the parts digest is
checked as always, the forgotten value's check is skipped, the other values
of the act are checked while their locks live, and the recorded answer
returns. So a reused name with other content in a forgotten value cannot be
told from a retry: the price of forgetting (R1's rider).

### Consumption of lease rows

In the decision's one atomic group, whatever the decision:

- **admitted:** `NONE>` on each cited lease row; its lock is written as the
  value's lock row or record lock in the same commit;
- **refused (recorded):** `NONE>` on each cited lease row and nothing else of
  the lock kept; the depot's sealed bytes of that offer never open again;
- **answered from the record:** no cited lease row can exist when the parts
  digest matched. A lock id is minted once (a lease name is decided once),
  the matched digest means the resend cites the recorded ids, and the first
  decision consumed exactly those in its own commit. So the record path
  stays write-free and the default holds by construction; the test asserts
  the rows are gone after a record answer. When the parts digest differs the
  answer is `:name-taken` on the face and the cited rows are left alone, as
  for any face refusal.

### The missing lock

- **Where in the order:** after stage 1's structural face checks and
  `:not-sealed` (a non-control fact with a `:v` other than nil, or a
  `:sealed` without a well-formed `:lock-id`, or a control fact with
  `:sealed`: L27); after the record lookup (a decided name answers from its
  record whatever its leases); after the reads of `:settings` and the
  owner's `$$persons` entry; at the delivery, one `deliver-lock>` per
  distinct cited id; before every check that needs a plaintext and before
  every recorded refusal. Reason `:no-such-lock`: a cited id delivered nil
  (never leased, leased to another session, already consumed, destroyed at a
  session close, or unopenable because the owner was forgotten).
- **Nothing recorded, nothing consumed, nothing written.** The answer goes
  through the ack only (P7's road), so a resend under the same name with a
  lock that delivers is decided fresh.
- **The same answer on a replay**, for a door that cites only ids it took
  from a lease's answer: the lease row was committed before the offer was
  appended, so a replay finds it as the first attempt did, or finds it
  consumed by this offer's own committed decision, which the record path
  answers first. A door that cites an id before its lease is answered, or
  closes a session with an offer in flight, can see a replay (`:all-after`
  replays later records' effects first) answer otherwise; the door's rule
  above excludes both.

### Lease rows at session close

- `:session-closed` is an act into the layer by the session itself (the
  offer's `:session` equals the value's `:session`) or by the operator,
  citing a permission covering the layer; decided on the home; on a yes,
  `(local-transform> [(keypath *layer :leases *s) NONE>] $$layers)`, one
  direct delete of the session's map. A second close finds nothing and is a
  yes that changes nothing.
- It closes nothing else: when a session layer closes (open item 82), a
  session's permissions (CONCLUSION R7) and agent read entries (R5) are not
  this act's.
- **The retention it bounds, named:** until the close or the owner's forget,
  an unconsumed lease row opens the depot bytes of an offer that cited it
  and was refused on its face or never decided. Such bytes are not a value
  of the store (never admitted), so no forget can name them; the close, the
  owner's forget, or a later decision citing the row reaches them.

### P6 widened: the three places the tests check

"Nothing retained opens or confirms a forgotten value" (CONCLUSION, P6
widened), checked after every forget in `forget_test.clj`:

1. **The depot.** The forgotten value's offer, read raw with
   `foreign-depot-read`: its `:sealed` bytes open under none of the locks the
   store still holds (each lease row, each lock row and each record lock,
   unwrapped with each live person lock, and the live person locks
   themselves), and nothing else in the record is the value.
2. **The lease rows.** The forgotten value's lease row is gone (consumed at
   its admission); no remaining lease row unwraps to a lock that opens the
   depot's or the log's bytes.
3. **The lock store.** After a value forget the lock row is gone or the
   record lock excised; after a person forget the lock record remains and
   unwraps with no live person lock; no remaining lock reproduces the row's
   value digest from the value's plaintext, so the digest confirms nothing;
   the parts digest covers no value.

### Interfaces tonight's parallel stages build against

- **`rig.store.clock`** (tonight's clock build, CONCLUSION R2): `next-stamp`
  and the packing, ms × 65536 + counter, one long. Every stamp this stage
  writes is the decision's stamp taken from it (the answer, `:by-stamp`, the
  ledger's `:stamp`, a person's `:erased-at`); a date is shown by the
  clock's own unpacking, never by arithmetic here.
- **`rig.store.locks`** (this stage): `(seal K plain) → sealed`, `(open K
  sealed) → plain | nil`, `(wrap K wrap persons) → lock record | nil`,
  `(unwrap record persons) → K | nil`, `(value-digest K plain) → 32 bytes`,
  `(lease-ids name n)`, `(unlease row person-entry)`, `deliver-lock>` above,
  and `open-value>`: `(open-value> *layer *fid *T :> *r)`, a `deframafn` on
  the layer's home, with `*r` one of `{:value v :stamp s}`, `{:erased-at
  date}` (the ledger's date, else the person-forget date `wrap-closed`
  computes), or `{:unreadable reason}` (`:no-such-fact`; `:after-moment`
  when the fact's stamp is after T, so a read as of T shows nothing admitted
  after it; `:does-not-open` when a live lock fails on the bytes, which no
  write of this plan produces). Its reads: the act's answer (the stamp), the
  row, the ledger entry, the lock row when the row's `:lock` is nil, the
  wrap's person entries. A control fact or a retract returns its plaintext
  or nil. The read exit calls it; this stage's `read-as-of` calls its
  twin `open-row>` (the same, given a row and its act's stamp the caller
  already read), so no row is read twice.
  Install functions: `(layer-fields)`, the fields this stage adds to
  `$$layers`'s fixed-keys schema; `(declare-pstates! s)` for `$$persons` on
  the gate topology `s`; `(declare-queries! topologies)` for `lease-locks`
  and `read-as-of`.
- **The read exit** (PLAN-read-exit.md, parallel): its read entries go
  through the door like any act, so their values are sealed once this stage
  lands; its indexes over values are purgeable by value id, and a value
  forget calls that purge from its own event on the home, at one call site
  after the ledger write, under whatever name that plan gives it. If its
  indexes are owned by a topology other than the gate, the call cannot write
  them from this event; named under "What this plan could not settle".
- **Phase 3** (parallel): the micro gate calls `seal`, `open`, `wrap`,
  `unwrap`, `value-digest`, `unlease` and, over `$$layers`-shaped lease
  rows, `deliver-lock>`; the lock record's shape is shared.
- **module.clj:** where Rama allows, one line per install function:
  `$$layers`'s schema becomes `(fixed-keys-schema (merge stage-1-fields
  (locks/layer-fields)))`, one merge argument per stage;
  `(locks/declare-pstates! s)`; `(locks/declare-queries! topologies)`. Where
  it does not: the gate event is one `<<sources` block whose reads and
  writes must commit in one atomic group (SPEC "What Rama showed" 1), and a
  PState is written only by the topology that declares it, so this stage's
  steps are call sites inside stage 1's event (the delivery, the second
  persons read, the decision's lock writes and lease consumption, the forget
  writes and the purge call, the person fan-out), each a `deframaop` or
  `deframafn` of `rig.store.locks` so the bodies live there and module.clj
  carries about six calls.

## Reads

Stage 1's reads stand. Added reads, each on the value's task; the ones that
need more than one PState read go through the query topology `read-as-of`
(Step 1's second question), the rest are one `foreign-select-one`:

| read | who | path | seeks | note |
|---|---|---|---|---|
| RD4 a layer as of T: every yes act stamped ≤ T, each fact with its value or its erasure date, and every erasure by now | tests, stage 5 | query topology `read-as-of [layer T]` (below) | 1 + acts ≤ T + about 3 per value fact | the erasure part of RD4 is this stage's; visibility (I-P2), points and patterns are stage 5's |
| RD7 whether one value opens, and since when | tests, stage 4 (in the event), stage 5 and the read exit | `open-value> [layer fid T]` (revised 26 September): the answer's stamp `[(keypath layer :answers name) :stamp]`, the row `[(keypath layer :log name idx)]`, the ledger `[(keypath layer :erased lock-id)]`, the lock row `[(keypath layer :locks lock-id)]` when the row's `:lock` is nil, and `[(keypath p)]` on `$$persons` per wrap person | 1 + 1 + 1 + 1 + w | all local; `locks/erasure`, `locks/unwrap` and `locks/open` are the pure steps |
| the leased locks of a session, in plaintext (the door's step 2) | the door | query topology `lease-locks [layer session]` (below) | 1 settings + 1 owner + 1 range seek + rows | revised 26 September; the one path by which a lock leaves the module |
| a session's lease rows, raw | tests (consumption, P6 check 2) | `foreign-select [(keypath layer :leases session) ALL]` | 1 + rows | revised 26 September |
| RD7s the lock store's size for a layer | phase 7 (M2), tests | `foreign-select [(keypath layer :locks) ALL]` | 2 + rows iterated | count and serialized bytes by iteration; size tracking is off (L15) |
| a lock row | tests | `[(keypath layer :locks lock-id)]` | 2 | the lock record; nil once deleted |
| an erasure ledger entry | tests, stage 5 | `[(keypath layer :erased lock-id)]` | 2 | `{:stamp :how}` or nil |
| a person's lock state | the gate (locally), tests | `[(keypath p)]` on `$$persons` | 1 | on every task; a foreign read routes by `p` to some task and every task holds the same entry once the person act's ack returned |
| the by-stamp index of a layer | `read-as-of`, stage 5 | `[(keypath layer :by-stamp) (sorted-map-range-to (inc T))]` | 1 seek + entries | stamp → name, yes answers only |
| the depot record of an offer, raw | the P6 check 1 (revised 26 September) | `foreign-depot-read` | — | sealed bytes, lock ids and control values only; no longer "the known gap" |

The gate's own reads per offer (revised 26 September), in the event's
order. On the record path (a record under the name): for a recorded yes
with value facts, the act's rows `[(keypath layer :log name)]` as one
subindexed read, the lock row per distinct lock id when the row's `:lock` is
nil, and `$$persons` per wrap person, for the value check. When the record
does not decide the offer: `:settings`; `[(keypath owner)]` on `$$persons`;
the lease row per distinct cited lock id through `deliver-lock>`; then stage
1's clock, wall, permission row, heads loop; then `[(keypath p)]` on
`$$persons` for every other distinct person in the act's wraps (none for an
ordinary act; more only for marked values naming others), read in a
`loop<-` that emits a map even when empty (never `ops/explode`, [F7]); for a `:forget`
fact, the target row `[(keypath layer :log tname tidx)]` and then, when the
row has a lock id, the ledger entry `[(keypath layer :erased lock-id)]`; for
a `:person` or `:forget-person` fact, `[(keypath p)]` on `$$persons`; for a
making act (`:owner` fact), `[(keypath owner)]` on `$$persons`. The counts
are in "Partitioning efficiency".

## Writes

One depot, stage 1's `*offers`; every write is an offer. By operation:

| op | offer | facts | decided by, and the lock effect |
|---|---|---|---|
| lease (revised 26 September) | the session's `:who`, a permission covering the layer, `:session s`, tag the layer's class | `{:e s :k :lease :v {:count n}}` | the gate on the home; the owner's person lock must be live (`:no-such-person`, `:person-forgotten`); n fresh locks, each sealed under the owner's lock into `:leases[s][[name i]]`; the ack carries `:lock-ids` |
| OP1 offer an act (stage 1, revised 26 September) | as stage 1, value facts sealed at the door | sealed facts citing leased lock ids | the gate on the home; each cited lock through `deliver-lock>` (any missing: `:no-such-lock` on the face, nothing written); each value opened; K re-wrapped under the fact's wrap into a lock record, which goes to `:locks[lock-id]` (row) or the row's `:lock` (record); the row keeps the offered `:sealed` bytes and the value digest; each cited lease row deleted, admitted or refused; a fact whose wrap names a person with no lock or a destroyed one is refused (`:no-such-person`, `:person-forgotten`, L11); `:by-stamp[stamp] = name` |
| OP1 under per-act grain | as stage 1 | sealed facts all citing one lock id | one K for the act, re-wrapped under the act's union with marked? = any value fact marked (L6); the lock record at `:locks[lock-id]` when the layer's kind is personal or hand or any fact is marked `:own-row`, else in every row's `:lock`; an act whose citations do not fit the grain in force is refused `:grain-mismatch`, recorded (L30) |
| session close (revised 26 September) | the session itself or `:operator`, a permission covering the layer | `{:e s :k :session-closed :v {:session s}}` | the gate on the home; `:leases[s]` deleted whole (`NONE>`, one direct delete); nothing else |
| OP8 grain switch (stage 1) | as stage 1 | `:lock-grain` | as stage 1; the next act decided on the home reads the new grain (I-P5) |
| OP9 forget a value | `:who` the layer's owner with `[owner L L]`, or `:operator`; `:layer` the target name's layer; tag class nil; `:stood-on {target-fid stamp}` | `{:e e :k :forget :v {:target fid}}` | the gate on the home; the target row must be in this layer's log on this task, else `:no-such-value` (L10); a target with no lock id, or a lock already in the ledger: admitted, nothing changes; else a row lock is deleted (`NONE>` on `:locks[lock-id]`, `:how :row-deleted`) or a record lock is excised (`termval nil` on the row's `:lock`, every row of the act under per-act, `:how :excised`), and `:erased[lock-id] = {:stamp s :how}` |
| OP10 make a person | `:who :operator`, `:layer :people`, tag class nil | `{:e p :k :person :v {:id p}}` | the gate on `:people`'s home: `:person-already-made` when `$$persons[p]` exists; else `$$persons[p] = {:lock fresh :erased-at nil}` on the home in the decision's group, then `(|all)` and `termval` of the home's entry on every task (L7, L9) |
| OP10 forget a person | `:who :operator`, `:layer :people`, tag class nil, `:stood-on {person-fid stamp}` | `{:e p :k :forget-person :v {:person p}}` | the gate on `:people`'s home: `:no-such-person` when no entry; an entry already erased: admitted, nothing changes (the first date stays); else `$$persons[p] = {:lock nil :erased-at s}` on the home in the decision's group, then `(|all)` and `termval` of the home's entry on every task |
| seed | the operator's acts | the `:people` layer (`:kind :store`, `:class :by-layer`, `:lock-grain :per-value`, no owner), then a `:person` act per person of the world (`:alice`, `:bob`), then stage 1's layers and grants | the gate, before any history; a making act naming an owner with no person lock is refused `:no-such-person` (L11), so persons come first |

The gate's writes on a fresh decision, all on the home task in the one
decision event as stage 1, extended (revised 26 September): for any decided
act citing locks, one `NONE>` per cited lease row, yes or no; on a yes, per
value fact the row (`termval`, now with `:sealed` as offered, `:lock-id`,
`:lock`, `:digest`), per row lock one `termval` into `:locks`, one `termval`
into `:by-stamp`; for a lease n `termval`s into `:leases`; for a session
close one `NONE>` on `:leases[s]`; for a forget one `NONE>` or one or more
`termval nil`, one ledger `termval` and the read exit's purge; for a person
act one `termval` into `$$persons`. Every one is a set or a delete keyed by
name, fact id, lock id, session, stamp or person id. The fresh random bytes
a decision writes are now only the gate's own: the locks a lease mints,
person locks, and the nonces of a re-wrap (the value's ciphertext and its
lock are the door's, fixed in the depot record). A replay that reaches the
writes (possible only when nothing was committed) produces the same facts
under a different wrapping or, for a lease, other lock bytes under the same
ids that no door has seen, which I-L8 allows and I-G2 does not forbid; a
replay that finds the record writes nothing and, for a person act, repeats
the idempotent fan-out (L9). The person fan-out is the one place a
partitioner enters the gate's event (see "Topologies").

## PState Design

Two PStates change or appear: stage 1's `$$layers` grows four per-layer maps
(`:locks`, `:leases`, `:erased`, `:by-stamp`) and four row fields
(`:sealed`, `:lock-id`, `:lock`, `:digest`), and `$$persons` is new.
`$$clock` stands as stage 1 has it, holding the hybrid clock's last stamp
(tonight's clock build owns its meaning). Revised 26 September: `:leases`
and `:sealed` added, `:digest` moved onto the row for values, every sealed
or wrapped slot raw bytes.

### Where the lock rows and the ledger live: in `$$layers`, by the merge rule

Everything this stage keeps about a value is keyed by its lock id or its
fact id and wanted on the value's task (I-L2), which is the layer's home
(ruling 2, P2). Stage 1's hand-off said it: anything so keyed shares
`$$layers`'s key and partitioner, so by the merge rule (phase-1-plan Step 2)
it is a field of the layer's value, not a PState of its own. The candidates
costed for the two reads that matter, the gate's decision (every offer) and
the erasure check (every open, in the gate for stage 4 and in `read-as-of`):

- **Option A, a separate `$$locks {layer {lock-id record}}`** (and
  `$$erased`, `$$by-stamp`): same key, same partitioner as `$$layers`. The
  decision's lock write is a no-read set either way; the erasure check pays
  one more top-level seek per PState (the layer's entry in each) before the
  element seek: 2 + 2 + 2 against 2 + 1 + 1 when the maps sit under the one
  layer entry already loaded for `:settings`. Three more partitions per
  task for the same key. Rejected: no read is cheaper, every task pays the
  memory.
- **Option B, the lock record inside the log row for every value** (no
  `:locks` map): deleting a row lock would be a `termval nil` on the row's
  `:lock`, indistinguishable from an excision; the ruling separates the two
  ("own row in the lock store" versus "in the record", with excision the
  operator's fallback) and phase 7's M2 measures the lock store's growth
  under hand layers as its own number. Rejected on the ruling; costs are
  equal.
- **Option C, chosen: three subindexed maps under the layer's fixed-keys
  value** (`:locks`, `:erased`, `:by-stamp`) and three fields on the log
  row. The decision reads nothing new for a value's lock (the wrap needs
  `$$persons`, below); a value forget reads the target row (2 seeks) and the
  ledger (1 more under the loaded entry); the erasure check is 1 + 1 (+ 1
  for a row lock) under the loaded entry.

Schema, stage 1's `$$layers` with the additions marked `; +2` (revised 26
September: `byte/1` for bytes, `:sealed`, `:digest` on the row, `:leases`;
stage 1's row vector is `{:subindex? true}` as its code has it, since Rama
1.6.0's `vector-schema` takes no `:subindex-options`):

```clojure
(declare-pstate s $$layers
  {clojure.lang.Keyword
   (fixed-keys-schema
     {:settings    ...                                            ; stage 1
      :answers     ...                                            ; stage 1; :digest is now the parts digest (L26); :subjects includes the grammar's, capped at 256, L13
      :log         (map-schema clojure.lang.PersistentVector      ; name
                               (vector-schema
                                 (fixed-keys-schema
                                   {:e        clojure.lang.Keyword
                                    :k        clojure.lang.Keyword
                                    :v        String              ; EDN text for a control fact; nil for a value fact and a retract
                                    :sealed   byte/1              ; +2 the offer's sealed bytes, as they came; nil unless a value fact
                                    :replaces clojure.lang.PersistentVector
                                    :mark     (set-schema clojure.lang.Keyword)
                                    :lock-id  clojure.lang.PersistentVector   ; +2 [lease-name i] | nil
                                    :digest   byte/1                          ; +2 the value digest, HMAC-SHA256 keyed by the lock; nil unless a value fact
                                    :lock     (fixed-keys-schema              ; +2 the lock record when kept in the record; nil for a row lock; nil once excised
                                                {:required  clojure.lang.PersistentVector
                                                 :any-of    clojure.lang.PersistentVector
                                                 :blob      byte/1
                                                 :any-blobs (map-schema clojure.lang.Keyword byte/1)})})
                                 {:subindex? true})
                               {:subindex-options {:track-size? false}})
      :stood-on    ...                                            ; stage 1
      :heads       ...                                            ; stage 1
      :permissions ...                                            ; stage 1
      :locks       (map-schema clojure.lang.PersistentVector      ; +2 lock id -> the lock record: the value locks of this layer
                               (fixed-keys-schema
                                 {:required  clojure.lang.PersistentVector
                                  :any-of    clojure.lang.PersistentVector
                                  :blob      byte/1
                                  :any-blobs (map-schema clojure.lang.Keyword byte/1)})
                               {:subindex-options {:track-size? false}})
      :leases      (map-schema clojure.lang.Keyword               ; +2 session ->
                               (map-schema clojure.lang.PersistentVector    ; lock id -> the lease row, unconsumed
                                           (fixed-keys-schema {:under  clojure.lang.Keyword
                                                               :sealed byte/1})
                                           {:subindex-options {:track-size? false}})
                               {:subindex-options {:track-size? false}})
      :erased      (map-schema clojure.lang.PersistentVector      ; +2 lock id -> {:stamp :how}: the erasure ledger
                               (fixed-keys-schema {:stamp Long :how clojure.lang.Keyword})
                               {:subindex-options {:track-size? false}})
      :by-stamp    (map-schema Long clojure.lang.PersistentVector ; +2 stamp -> name, yes answers only
                               {:subindex-options {:track-size? false}})})})
```

Why each part is shaped so:

- `:locks` keyed by lock id: the erasure check and the forget are point
  reads and a no-read delete by id; unbounded (one row per value in
  personal and hand layers, one per act under per-act), so subindexed.
  Size tracking off: M2 counts and sizes rows by iteration once per
  measurement point, and tracking would add a read to every hand-layer
  write, the very writes M2 times (L15).
- `:erased` keyed by lock id: `erasure` in the model looks the ledger up
  by lock id first. It is an index over lock ids, not values; rebuildable
  from the log's `:forget` facts (plaintext control values, D2), so I-L5
  holds. Unbounded, subindexed. It is not folded into `:locks` as a row with
  a date because the ruling says the row is deleted (SPEC phase 2) and M2's
  count must fall when it is.
- `:by-stamp` keyed by stamp: a read as of T (I-L7, this stage's erasure
  part of RD4) is one range `(sorted-map-range-to (inc T))` plus the acts
  it covers, instead of a scan of `:answers`; stamps are strictly increasing
  per task and a layer sits on one task, so a stamp names at most one act.
  Rebuildable from `:answers` (stage 1 said it "belongs in `$$layers` if
  kept"). Unbounded, subindexed. Refusals get a stamp but no entry.
- The lock record is a `fixed-keys-schema`: one shape for every wrapped
  lock, with `:blob` nil exactly when `:any-of` is non-empty and
  `:any-blobs` nil otherwise (nullable fields on one shape, allowed).
  `:any-blobs` is not subindexed: it is bounded by the 256 cap on subjects
  (L13), an enforced bound.
- `:required` and `:any-of` are sorted vectors, not sets: the required
  chain's sealing order must be reproducible from the record, and a vector
  of keywords is one of stage 1's known classes.
- `:lock-id` on the row lets a reader find the lock without knowing the
  layer's grain at the value's write time (I-P5: the pick sticks per
  value), and it is the id the offer cited, so the record path compares
  like with like.
- `:leases` keyed by session, then lock id (revised 26 September): the
  delivery is one point read under the offer's session, `lease-locks` one
  range read of the session's map, a session close one direct delete of it
  (pstate-schema.md: delete the subindexed structure itself). Unbounded per
  session in principle (a door may lease more than it uses), so both levels
  are subindexed; a lease writes at most 256 rows, bounded by its offer.
  Not folded into `:locks`: a lease row is wrapped under the session owner
  only and belongs to a session, a lock row is wrapped under the value's
  subjects and belongs to a value; M2's count is of `:locks` alone.
- `:sealed` beside `:v`, not in it (revised 26 September): one slot cannot
  hold text for a control value and bytes for a sealed one without an
  `Object` or a polymorphic record; two nullable fields on one shape keep
  stage 1's control-value decoding as it is.
- `:digest` on the row, not on the answer record (revised 26 September):
  one value digest per value fact, keyed by that value's lock, written with
  the row it checks; the answer record keeps the parts digest, a String as
  stage 1 has it.
- No `Object`: person ids are keywords (P3), ids are vectors rebuilt by the
  parser ([F6]), ciphertext, digests and locks are `byte/1` (L4, revised;
  `byte/1` ran as a schema class in the lock-growth bench,
  BENCH_NOTES-locks.md).

### `$$persons` — every person's lock, on every task

The person locks are the one datum every open needs (the read-out on the
owner's task in stage 4, the gate's wrap at every write, every read that
shows a value) and that no layer owns. Placement `f(person) → task(s)`,
derived from those reads first:

- **On one task** (`|hash` by person, or `:global? true` on task 0): every
  write of a value wrapped under Alice needs Alice's lock from that task,
  so the gate's decision event would hop there and back (two partitioner
  boundaries, three atomic groups) or read it through a query from the
  client before appending (a plaintext lock on the wire and in the offer,
  which then sits in the depot: rejected on I-L4). The hop breaks stage 1's
  one-event, no-hop decision (RQ 1's atomic group; "same answer after a
  crash"), and every later open pays it too. Cost per ordinary act: 5 seeks
  plus one network round trip; per read-out (stage 4): the same.
- **On every task** (`|all`), chosen: a person lock is about 60 bytes,
  written twice in its life (made, destroyed), read at every write about
  the person and every open. pstate-schema.md names this exactly as the
  case for `|all`: small, rarely written, read locally everywhere. Cost per
  ordinary act: 1 local seek; per person act: N local writes, once. The
  memory is one entry per person per task.

```clojure
(declare-pstate s $$persons
  {clojure.lang.Keyword                                 ; person id
   (fixed-keys-schema {:lock byte/1                     ; 32 random bytes; nil once destroyed (revised: raw bytes)
                       :erased-at Long})})              ; the forget-person fact's stamp (rig.store.clock); nil while alive
```

Owned by the stream gate (the only topology that writes; stage 3's micro
gate reads it as committed state on its own tasks). Its key class equals
`$$layers`'s but its partitioner does not (every task, by `|all`, versus
the layer's home), which is the one justification the merge rule accepts
for a separate PState. Not `:global?`: a global PState lives on task 0 only.
Top-level map, RocksDB-backed, so a person's entry is one seek; no
subindexing (each value is two small fields).

### What is not a PState

No index over plaintext values: `:heads` holds ids and stamps (stage 1),
`:erased` and `:by-stamp` hold ids and stamps. No copy of a value lock in
the store but its one lock record, or, before its decision, its one lease
row (never both: the decision that writes the one deletes the other in one
commit); outside the store, the door's memory until the session closes. No
in-memory holder: the lease road is tonight's default and the holder is a
described replacement body (see "The delivery function"). No TaskGlobal: person locks are read from
`$$persons` at every use, one local seek, block-cached for a hot person; a
cache of locks in memory would be a second place a destroyed lock could
survive and one more thing to keep right across restarts, for no seek that
matters (the CLAUDE.md rule on caching applies: nothing here makes a cache
look necessary).

## Depots

Stage 1's `*offers`, `(hash-by :layer)`, unchanged: every operation of this
stage is an offer into a layer, decided on that layer's home, and the
forget of a value is order-dependent with the value's other operations
(a read-out, a replace) on the same task, which one depot keyed by layer
gives (OP9 "Concurrency": a forget and a read-out on one task are ordered by
that task). The person acts go into the store layer `:people`, so they too
ride `*offers` and are ordered among themselves on `:people`'s home; their
effect reaches every task by the fan-out inside the event, not by a second
depot. No internal depot: nothing here needs a second topology to wait on
the first, and no datum the fan-out carries is unavailable to the stream
gate's own event.

**Depot retention (revised 26 September; O2, I-L4).** `*offers` keeps every
offer for ever (no trimming), and an offer now carries only sealed bytes,
lock ids and control values: nothing in it opens a value once the value's
lock is gone, and nothing in it ever carried the lock (the door took it by
`lease-locks`, which is not a depot). The 25 September gap, plaintext values
in the depot for ever, is closed by the door's sealing; what stays named is
under "Design Decisions", O2. The depot is still partitioned `hash-by
:layer`, so a lease, the offers citing it and the session close are ordered
on the layer's home, which is what makes "the lease row exists before the
offer that cites it" hold for a door that waits for the lease's answer.

## Topologies and PStates

One stream topology, stage 1's `gate`, extended; it owns `$$layers`,
`$$clock` and `$$persons`. One query topology, `read-as-of`, new (below).
No microbatch topology in this stage (stage 3's). Topology count: still one
stream topology; a second is not declared because every concern here is a
part of the one decision per offer and must be visible when the answer is.

### `gate` — stream (stage 1's reasons stand)

**Why stream** (both of artifact-plan.md's reasons; ruling 1 names the
type): the person's own write, now with its lock, is visible to their next
read without optimism (I-O6); the answer, including a forget's, returns
through the append's ack, and OP9 needs the forget "complete at the answer,
never eventual". A microbatch would give neither.

**Every concern in it needs stream.** Minting a lease, delivering and
re-wrapping a lock, writing the lock row, consuming the lease rows, deleting
or excising a lock on a forget, writing the ledger, closing a session's
leases and destroying a person lock are each part of one decision whose
answer the offerer takes from the ack: if the lock row were written later by
a microbatch, a read-out (stage 4) between the answer and the row would find
a value with no lock; if a lease row outlived its decision, a refused
offer's depot bytes would stay openable; a lease's rows must stand when its
answer returns, or the door's `lease-locks` would miss them; if the deletion
were later, a forget answered yes would still open (OP9's invariant broken).
So none moves.

**The event, on the home task** (revised 26 September). Stage 1's steps,
extended; the decision is still one atomic group with no hop except for the
person fan-out at the end:

1. `source>` as stage 1 (`:all-after`, P11).
2. `intake` as stage 1, extended, structural only (face, unrecorded, P7):
   the sealed-fact shape (`:not-sealed`, L27: a non-control fact with a
   non-nil `:v`; a `:sealed` that is not bytes or has no well-formed
   `:lock-id`; a control fact or a retract carrying `:sealed`); the parts
   digest. Control values are checked as the 25 September text has them
   (`:forget` carries `{:target fid}` with a well-formed name and a
   non-negative long index; `:person` `{:id p}` and `:forget-person`
   `{:person p}` with a keyword), and now `:lease` `{:count n}` with a long
   in 1..256 and an offer `:session`, and `:session-closed` `{:session s}`
   with a keyword; a bad one is `:malformed-control`, recorded where R13
   puts it. Stage 1's value-domain checks (R17, the 32-level bound [F6]) no
   longer run here: the values are sealed.
3. The record, first [F4]: `[(keypath layer :answers name)]`. Found: the
   record path ("The digest and the resend check"): for a recorded yes with
   value facts, the act's rows in one subindexed read, the lock row per
   distinct lock id where the row's `:lock` is nil, the wrap persons in a
   `loop<-` [F7]; then the pure `locks/check-resend` → the recorded answer
   or `:name-taken`; nothing written; `ack-return>`.
4. Not found: `:settings`; `$$persons [owner]` (nil when the layer has no
   owner or does not exist); then `deliver-lock>` per distinct cited lock id
   in a `loop<-` emitting a map `{lock-id K-or-nil}`, possibly empty [F7].
   Any nil: `:no-such-lock` through the ack, nothing written (face; "The
   missing lock").
5. Open and read the values, pure (`locks/read-values`): `locks/open` per
   value fact under its delivered K, the canonical decode, stage 1's
   value-domain check, `grammar/subjects-of` per value fact and the act's
   union, the citation-against-grain check. Its reasons are recorded (step
   8), and when it has one, the next step's persons loop reads nothing.
6. Stage 1's reads (clock, wall, permission rows, heads loop); then
   `[(keypath p)]` on `$$persons` for each distinct wrap person not yet read,
   in a `loop<-` emitting a map [F7]; for a `:forget` fact the target row
   `[(keypath layer :log tname tidx)]` and, inside a `<<if` on its lock id,
   the ledger entry; for a `:person`, `:forget-person` or `:owner` fact
   `[(keypath p)]` on `$$persons`. Each such var is bound nil on the
   branches that do not read it, so the attach point unifies (dataflow.md;
   [F7]).
7. Fresh randomness, bound before the pure decision: `(locks/fresh ...)`,
   n locks for a lease, one person lock for a `:person` fact, and the nonces
   the re-wraps' seals take. A `defn` over `SecureRandom`; never
   `ops/random-uuid7` or any generator inside `decide`, which stays pure and
   total. The value locks themselves are not fresh here: they are the
   delivered ones.
8. `decide`, pure and total, extended. Refusals: stage 1's list, then this
   stage's recorded reasons in this order: `:does-not-open`,
   `:malformed-value`, `:value-shape` (L12), `:too-many-subjects` (L13),
   `:grain-mismatch` (L30), `:no-such-person` (a wrap person, a lease's or
   a making act's owner, or a `:forget-person` target with no entry),
   `:person-forgotten` (a wrap person or a lease's owner whose lock is
   destroyed), `:person-already-made`, `:no-such-value`. Every earlier check
   reads nothing more than stage 1 read (I-G5's spirit), and every recorded
   reason sits after the delivery, so a recorded no always consumes its
   leases. `:control-not-allowed` (R13) now admits the layer's owner for
   `:forget` as well as `:lock-grain` (L10), and anyone whose permission
   covers the layer for `:lease` and `:session-closed` (L20, L28). On a yes:
   per value fact `locks/wrap` of its K under its wrap, placed by
   `locks/lock-for`; the row with `:sealed` as offered, `:lock-id`, `:lock`,
   `:digest` (`value-digest K plain`); for a lease the n lease rows; for a
   session close the delete; for a forget the deletion or excision and the
   ledger entry; for a person act the `$$persons` entry. Yes or no, the list
   of cited lease rows to delete. A `:forget` whose target has no lock id,
   or whose lock is already in the ledger, decides yes with no lock effect
   (`apply-control`'s nil branch; OP9 "the second is admitted and changes
   nothing"); a `:forget-person` of an already erased person decides yes
   with no change (the first date stays). The stamp is
   `rig.store.clock/next-stamp` over the task's clock, the wall and the
   carried and replaced stamps (P9's promise in R2's form).
9. Writes, only for a fresh decision, all sets and deletes as listed under
   "Writes", the lease consumption among them in the one group. The crash
   hook (P14) stays where stage 1 put it (`:seen`, `:recorded`,
   `:before-writes`, `:after-writes`); the two crash tests of this stage use
   those points ("Namespaces and tests").
10. `ack-return>` as stage 1; the answer of a value forget also carries
   `:how` (`:row-deleted`, `:excised`, or nil when nothing changed), the
   model's note made data; a lease's yes carries `:lock-ids`, computed
   from its name and count on the fresh and the record path alike.
11. **The person fan-out**, only when the act carries a `:person` or
   `:forget-person` fact and its answer is yes, fresh or recorded:
   `(local-select> [(keypath *p)] $$persons :> *entry)` on the home (the
   entry as the decision left it, or as later acts left it on a recorded
   replay), then `(|all)`, then `(local-transform> [(keypath *p) (termval
   *entry)] $$persons)`. The `|all` is the commit boundary for the home's
   writes (stream.md "When PState writes commit"), so the record, the fact
   and the home's own entry are durable before any other task copies it.
   The `termval` of the carried entry is idempotent and order-safe:
   children from one source task reach each task in order (stream.md
   "Partition ordering"), and a replayed make after a forget carries the
   erased entry, the latest truth. The ack returns when the whole tree
   completes, so once the offerer has the answer every task holds the
   destroyed lock (OP10 "once answered, no value ... opens anywhere").

**Idempotency of every write, traced.** Stage 1's trace stands for its
writes. New: the row's `:sealed`, `:lock-id`, `:lock`, `:digest` are part of
the row's one `termval`; a lease row a `termval`; a consumed lease row's
`NONE>` and a session close's `NONE>` (deleting twice is the same); `:locks[id]` a `termval` (a replay that reaches it committed
nothing before); `:by-stamp[stamp]` a `termval` of the name; `:erased[id]` a
`termval`; a row lock's `NONE>` (deleting twice is the same); an excision's
`termval nil` (nil twice is the same); `$$persons[p]` a `termval` on the
home and on every task. No increment, no append. The fresh random bytes
(a lease's locks, person locks, re-wrap nonces) differ between a lost
attempt and its replay, but a lost attempt committed nothing (RQ 1), so
exactly one encoding ever exists for a fact, and a lease's committed bytes
are the only ones any door can have taken (`lease-locks` reads committed
state).

**Same answer after a crash, extended.** A crash before the home's commit:
no record, replay decides on the same state (the same delivered locks,
because the attempt's consumption of the lease rows was discarded with its
other writes; fresh wrap nonces; the same facts). A crash between a lease
and the offer citing it (the lease committed and answered, the offer not yet
decided): the replay of the lease takes the record path and mints nothing,
so the rows the door took stand, and the offer then decides as it would
have.
A crash after the home's commit and before a fan-out child: the replay
finds the record, writes nothing on the home, re-reads its `$$persons`
entry and fans out again; a child that had already written gets the same
entry. A crash in the middle of the children: the same, at least once per
task. R4: tests assert at least once, never a count.

**No input can make topology code throw** (I-G1, RQ 3), extended: every
function of `rig.store.locks` catches `Throwable` and returns nil (`open` on
a tampered, foreign or truncated blob, which in AES-GCM throws
`AEADBadTagException`; `unwrap`, `unlease`, `deliver-lock>`), so a sealed
value from the depot, which the door made and the gate did not, can refuse
an offer and never kill a worker. The canonical decode of an opened value
and stage 1's value-domain and 32-level checks run inside
`locks/read-values`'s own catch, which returns `:malformed-value`; so
`grammar/subjects-of` walks one path into a value already bounded to 32
levels [F6]. `decide`'s outer guard (`:gate-error`, [F6])
stands. Every new written value has the schema's class (vectors rebuilt,
longs, keywords, strings).

**Cooperative multitasking.** The only new loops are over the act's facts,
its cited lock ids and its wrap persons (bounded by the offer and the 256
cap), a lease's n rows (at most 256) and,
under per-act excision, over the act's rows (`ALL` on the subindexed row
vector: f reads and f writes, bounded by the act; a forget is rare). No
loop over PState contents in the gate.

**PStates owned:** `$$layers`, `$$clock`, `$$persons`, schemas above.

## Query Topologies

Two (revised 26 September): `lease-locks`, the door's one path to a leased
lock's plaintext, and `read-as-of`, which now opens each row through
`open-value>`.

### `lease-locks` (revised 26 September)

- **name:** `lease-locks`, signature `[*layer *session :> *result]`,
  `*result` = `{:grain g :locks {lock-id K}}`.
- **Route:** `(|hash *layer)` first, as `read-as-of` below.
- **Reads, all local:** `(local-select> [(keypath *layer :settings)]
  $$layers :> *settings)` (the grain in force and the owner);
  `(local-select> [(keypath *owner)] $$persons :> *pe)`; `(local-select>
  [(keypath *layer :leases *session) ALL] $$layers {:allow-yield? true} :>
  [*lid *row])`, one emit per unconsumed row; per row the pure `unlease`
  over the owner's entry; `(|origin)` and a map aggregation into `:locks`,
  bound even when empty. A row that does not unlease is left out.
- **Input example 1:** a session with one fresh lease of 64 → 1 + 1 + 1
  range seek + 64 iterations; all meaningful.
- **Input example 2:** a session whose leases are all consumed → 1 + 1 + 1
  range seek to an empty map: the empty range is the answer, not avoidable
  without knowing it.
- **Input example 3:** a forgotten owner → the same reads; every row fails
  to unlease, `:locks` empty (the door's next offer would be refused
  anyway).
- **Fixed or variable:** variable in the rows; dynamic by emission, nothing
  padded. Cost: 3 seeks + rows, at the lease act's rate.
- **Why a query topology:** three PStates reads and an unwrap that needs a
  person lock, which must not leave the module; a client-side read would
  carry the person lock to the door.

### `read-as-of`

One, `read-as-of`, this stage's read as of a moment: the erasure half of
RD4 (I-L7), enough to check every A case both ways (R8) and to show "erased
on this date" and nothing else from after the moment. Stage 5 owns points,
patterns, visibility and read entries and may keep, extend or replace it;
its output shape is what this stage leaves.

- **name:** `read-as-of`, signature `[*layer *T :> *result]`.
- **Route:** `(|hash *layer)` as the first line, evaluated client-side, so
  the query lands on the layer's home (the same `f` as the depot's `hash-by
  :layer` and `$$layers`'s key partitioner, P2; the build's first check
  covers agreement, "What this plan could not settle").
- **Pre-agg, all local:** `(local-select> [(keypath *layer :by-stamp)
  (sorted-map-range-to (inc *T)) ALL] $$layers {:allow-yield? true} :>
  [*stamp *name])`, one emit per yes act at or before T; per act
  `(local-select> [(keypath *layer :log *name) INDEXED-VALS] $$layers :>
  [*idx *row])`, one emit per row; per row, `open-value>` over the row
  already read and the act's stamp (revised 26 September: the same
  function the read exit calls; given the row and the stamp it does not
  re-read them): a control fact's plaintext or a retract's nil as `{:id
  [name idx] :stamp s :value v}`; else the ledger entry, then, when none,
  the lock record (the row's `:lock`, else the lock row), then the wrap
  persons in a `loop<-` (a map, possibly empty), then `locks/erasure` →
  `{:id :stamp :erased-at e}` or, through `locks/unwrap` and `locks/open`,
  `{:id :stamp :value v}`; `{:unreadable :does-not-open}` cannot arise from
  this plan's writes and is reported for phase 8 to flag. Also
  `(local-select> [(keypath *layer :erased) ALL]
  $$layers {:allow-yield? true} :> [*lid *entry])`, the ledger whole, each
  tagged `[:erased lid entry]`; and each fact tagged `[:fact m]`.
- **Agg and post-agg:** `(|origin)`, `(aggs/+vec-agg *tagged :> *all)`,
  then a pure `split` into `{:as-of T :facts [...] :erased {lid entry}}`;
  `:facts` sorted by stamp then index in post-agg. The output is bound
  even when nothing matched (an empty vector, batch.md's zero-row rule).
- **Input example 1:** layer `:alice` with 3 yes acts of 2 value facts at
  stamps 5, 9, 14, T = 10 → 1 range read (2 entries) + 2 row reads (4
  rows) + per row: ledger 1, lock row 1 (personal layer), persons 1 (the
  owner, the same key four times, block-cached but counted) = 12, plus
  the ledger scan 1: 16 total, 16 meaningful (every read navigates to an
  entry that decides the output; the ledger read that finds no entry is
  the test "not value-forgotten", which is meaningful for the date shown).
- **Input example 2:** the same layer, T = 4 → 1 range read, empty; ledger
  scan 1: 2 total, 1 meaningful: the range read to an empty submap is the
  one read that answers "nothing as of T" and cannot be avoided without
  knowing the answer.
- **Input example 3:** an agent layer, 1 act of 3 facts, T = now, the act
  forgotten (excised) → 1 + 1 + 3 × (ledger 1) + 1 = 6, all meaningful; no
  lock row read, because the row's `:lock` is nil and the ledger decided.
- **Fixed or variable:** variable (the number of acts at or before T and
  their rows). Dynamic by construction: every read is emitted by the
  previous read's results (`local-select>` emits per navigated value; the
  persons loop is a `loop<-`); nothing is padded.
- **Cost:** 1 + acts + rows × (1 to 3) seeks on one task; `:allow-yield?`
  on the two range reads so a large layer does not hold the task thread.
  The gate's decision latency on that task is unaffected beyond the yield
  points.

No other query topology: a value's open-or-erased state alone (RD7) is
`read-as-of` at the value's stamp filtered by id in this stage's tests;
the read exit's point reads call `open-value>` and decide their own shape
(revised 26 September). Inside a gate event (stage 4's
read-out) the same reads are local `local-select>`s and the same two pure
functions.

## Partitioning efficiency

**Optimal placement, derived first.** The dominant read is unchanged from
stage 1: the gate's decision, once per offer, on the layer's home. This
stage adds to it one datum a layer does not own, the person locks, whose
ideal `f(person)` is "wherever a value about the person is decided or
opened", which is every task: `|all` (derived under "PState Design").
Everything else this stage adds (lock rows, ledger, by-stamp) is keyed by
the value and placed with it: `f(layer) = hash(layer) mod N`, stage 1's P2.
The alternative for person locks, one task per person, was costed there:
a hop inside every decision and every open, rejected on cost and on the
one-group decision. The alternative for lock rows, a separate PState with
the same key, was costed under "PState Design": more seeks, more memory,
rejected.

**Validation.** Seeks/op are totals across the cluster. Every read of the
decision touches one task, so the seek totals are flat in N. The person
fan-out is N local writes with no read; it is shown as its own column so
its growth with N is visible and weighed.

Data categories for the gate's decision (one offer), record-first [F4],
revised 26 September for the lease road. Counts are for the small acts
agent sessions write, one value fact each under per-value grain; an act of
f value facts adds f − 1 lease-row seeks to (a) and (b) and f − 1
iterations to (c). The owner's `$$persons` entry is read only for an act
that cites a lock or is a lease.

- (a) an ordinary act, no replace, one owner in every wrap: record +
  settings + the owner's entry + one lease row (`deliver-lock>`) + clock +
  permission = 6.
- (b) an act with a replace: 7.
- (c) a resend or replay of a decided offer: the record, then for a
  recorded yes the value check (R1's rider): the act's rows (1 seek + f
  iterations), the lock row (personal and hand; none for a record lock),
  the owner's entry = 4 seeks and 1 iteration (it was 1 seek).
- (d) an operator act with no value facts (make, grant, revoke, re-class; a
  making act adds `$$persons[owner]`): 4 to 5, counted 4.
- (e) a face refusal before the record (malformed, mis-tagged,
  `:not-sealed`): 0.
- (e') a missing lock (`:no-such-lock`): record + settings + the owner's
  entry + the lease row = 4.
- (f) a value forget: record + settings + clock + permission + target row
  + ledger = 6 (it cites no lock; the read exit's purge is that plan's
  cost).
- (g) a person act (make or forget): record + settings + clock +
  `$$persons[p]` = 4, then the fan-out: N task-writes, 0 seeks.
- (h) a lease act: record + settings + the owner's entry + clock +
  permission = 5, then n lease-row writes (n = 64 counted).

Frequencies: agent sessions write "many small acts" (a); replaces are
edits (b); resends per client error and replays after a crash (c);
operator acts, value forgets and person acts are rare, person acts rarest;
a lease per 64 value facts is about one per 80 offers (h); a missing lock
is a door's error or a race with a session close, rarer than any other
face refusal (e').

### N = 1 task (single-task baseline)
| Data category | Frequency proportion | Seeks/op | Iterator reads/op | Fan-out task-writes/op |
|---|---|---|---|---|
| (a) ordinary act | 0.568 | 6 | 0 | 0 |
| (b) act with a replace | 0.25 | 7 | 0 | 0 |
| (c) resend / replay, decided | 0.10 | 4 | 1 | 0 |
| (d) operator act | 0.04 | 4 | 0 | 0 |
| (e) face refusal | 0.01 | 0 | 0 | 0 |
| (e') missing lock | 0.002 | 4 | 0 | 0 |
| (f) value forget | 0.015 | 6 | 0 | 0 |
| (g) person act | 0.005 | 4 | 0 | 1 |
| (h) lease act | 0.01 | 5 | 0 | 0 |
Weighted seeks = 5.886   |   Weighted iterator reads = 0.10   |   Weighted fan-out writes = 0.005

### N = 16 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op | Fan-out task-writes/op |
|---|---|---|---|---|
| (a) ordinary act | 0.568 | 6 | 0 | 0 |
| (b) act with a replace | 0.25 | 7 | 0 | 0 |
| (c) resend / replay, decided | 0.10 | 4 | 1 | 0 |
| (d) operator act | 0.04 | 4 | 0 | 0 |
| (e) face refusal | 0.01 | 0 | 0 | 0 |
| (e') missing lock | 0.002 | 4 | 0 | 0 |
| (f) value forget | 0.015 | 6 | 0 | 0 |
| (g) person act | 0.005 | 4 | 0 | 16 |
| (h) lease act | 0.01 | 5 | 0 | 0 |
Weighted seeks = 5.886   |   Weighted iterator reads = 0.10   |   Weighted fan-out writes = 0.08

### N = 128 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op | Fan-out task-writes/op |
|---|---|---|---|---|
| (a) ordinary act | 0.568 | 6 | 0 | 0 |
| (b) act with a replace | 0.25 | 7 | 0 | 0 |
| (c) resend / replay, decided | 0.10 | 4 | 1 | 0 |
| (d) operator act | 0.04 | 4 | 0 | 0 |
| (e) face refusal | 0.01 | 0 | 0 | 0 |
| (e') missing lock | 0.002 | 4 | 0 | 0 |
| (f) value forget | 0.015 | 6 | 0 | 0 |
| (g) person act | 0.005 | 4 | 0 | 128 |
| (h) lease act | 0.01 | 5 | 0 | 0 |
Weighted seeks = 5.886   |   Weighted iterator reads = 0.10   |   Weighted fan-out writes = 0.64

Weighted seeks: 0.568×6 + 0.25×7 + 0.10×4 + 0.04×4 + 0.01×0 + 0.002×4 +
0.015×6 + 0.005×4 + 0.01×5 = 3.408 + 1.75 + 0.40 + 0.16 + 0 + 0.008 + 0.09
+ 0.02 + 0.05 = 5.886 at every N, flat (4.77 before the revision: the
lease road costs one local seek per value fact at decision, 0.818 per
offer, plus the lease acts and the value check on the record path). The
one quantity that grows with N is still the person fan-out, 0.005·N no-read
writes per offer (0.64 at N = 128). The alternative placement for person
locks now makes 0.93 reads per offer remote ((a), (b), (c), (e'), (h) read
the owner's entry) against 0.005·N local writes; the break-even is N =
186, above the two-year N this rig contemplates, and the alternative also
loses the one-group decision. The lease rows add 0.64 writes per offer on
average (64 per lease act), flat in N. The `lease-locks` query, at the
lease act's rate, is 3 seeks and 64 iterations on the layer's home, flat
in N.

Writes per offer, flat in N except the fan-out: stage 1's sets plus, per
value fact, one `NONE>` of its lease row, nothing extra for a record lock
(the row's one `termval` carries it), one `termval` for a row lock, plus
one `:by-stamp` set per act; a lease: n `termval`s; a session close: one
delete; a forget: one delete or f nils and one ledger set.

## Design Decisions

- **Real encryption, kept simple.** AES-256-GCM from `javax.crypto` (no
  dependency; authenticated, so a tampered slot fails closed), a fresh
  12-byte nonce per seal prepended to the output, 32-byte locks from
  `SecureRandom`. One primitive, `seal`/`open`, serves the value under its
  lock (at the door, revised 26 September), the lock under each person lock
  (the re-wrap) and a leased lock under its owner (the lease row);
  HMAC-SHA256 keyed by the value's lock is the value digest. No associated data, no key
  derivation, no rotation: nothing ruled asks for them, and each would be a
  second mechanism (L3, L4).
- **The wrap as key wrapping, not as policy.** "Required" is a chain of
  seals (every required lock is needed to peel it); "any of" is one copy of
  the required-sealed lock per any-of person (any one live lock peels its
  copy). So a wrap's rule is enforced by what can be decrypted, not by a
  check a reader could skip: after a person's lock is destroyed on every
  task, no copy of that person's key exists in the store, and the value
  locks that needed it are unrecoverable bytes. `wrap-closed` computes the
  *date* from the person locks' `:erased-at`; it never decides whether
  something opens, the ciphertext does (L3).
- **Subindexing.** `:locks`, `:erased`, `:by-stamp` subindexed (unbounded
  per layer), size tracking off. The lock record's `:any-blobs` and the
  wrap vectors are bounded by the 256 cap (L13). `$$persons` is a top-level
  map (one seek per person).
- **Colocation.** Lock rows, lease rows, the ledger and the by-stamp index
  sit under the layer's entry on the layer's home, where the value is
  (I-L2); a lease, the offers citing it and the session close ride the
  depot on the same partition, so they are ordered on that task. Person
  locks sit on every task. So the decision, a forget, a read-out (stage 4)
  and `read-as-of` each run on one task with local reads; the person
  fan-out is the one cross-task write, and it carries a 60-byte entry.
- **One event, one hop at most.** Ordinary acts and forgets: no partitioner,
  one atomic group (stage 1). Person acts: the decision's group, then
  `|all` (a commit boundary), then N single-write groups. The ack waits for
  all.
- **Erasure, two ledgers.** A value forget writes the date into `:erased`
  where the value is; a person forget writes the date into `$$persons`
  everywhere. `erasure` reads the first, then computes from the second: the
  model's order (O14: a value forget after a person forget shows the value
  forget's date, L16).
- **Time travel (I-L7).** `read-as-of` shows only acts stamped at or before
  T (the by-stamp range) and, for each of their facts, the value or the
  erasure date whatever the forget's stamp; nothing else from after T (no
  later fact, no forget fact's content, no replacement) reaches the output.
  The forget facts themselves are ordinary log rows with stamps after T, so
  the range excludes them.
- **The person forget as a fact (D3, O12).** An operator act in the store
  layer `:people` (L7), placed by layer on its home, stamped there
  (`stamp-for`, P9), the stamp being the erasure date. Ordered among person
  acts on one task, so `wrap-closed`'s "first" and "latest" are well
  defined (L8). The model's global stamp "later than everything so far" is
  the toy's kindness (README "Where the toy is kinder"); the rig's date is
  the fact's stamp on one scale (I-O3), at or after the wall clock at the
  forget, and tests under simulated time make it exact.
- **O2, no plaintext outlives its lock (revised 26 September).** Under
  tonight's default the store keeps a value's plaintext nowhere: the depot
  and the log hold one ciphertext under the value's lock, and the lock
  lives in exactly one place at a time (a lease row, then a lock row or a
  record lock). What holds anything about a value, place by place:
  1. **The depot `*offers`**: sealed bytes, lock ids and control values,
     for ever (no trimming). Replaced: the 25 September gap (plaintext
     values for ever, old L2) is gone, because the door seals before the
     append and the lock never travels to the gate: the gate minted it and
     keeps it in the lease row on its own task, and `lease-locks` carries
     it out to the door, never through the depot. A forgotten value's depot bytes open under nothing the store
     holds; P6 check 1 reads them raw and tries every held lock.
  2. **The digests**: the answer record's parts digest covers no value; the
     row's value digest is keyed by the value's lock and dies with it (R1's
     rider). The 25 September "Unchanged" digest over plaintext under a
     constant secret is gone. The rig secret stays in code for the parts
     digest only (P6).
  3. **The act's subject slot and the wrap's person lists**: person ids,
     "for finding" (ruling 8), kept in plaintext by design. A forgotten
     value still says whom it was about. Ruled, not a gap.
  4. **Unconsumed lease rows**: until a session closes or its owner is
     forgotten, a lease row can open the depot bytes of an offer refused on
     its face or never decided under it. Not values of the store; bounded
     by the session's life ("Lease rows at session close").
  5. **The substrate**: RocksDB keeps a deleted row's bytes in SST files
     until compaction; Rama's stream retry cache holds recent depot records
     in memory (now sealed); replication (factor 1 here) would copy every
     write. Named; nothing at the module level reaches them; a kept store
     would size compaction against its erasure promise.
  6. **The door**: holds the plaintext until answered and the leased locks
     until the session closes; it receives plaintext from `lease-locks` and
     `read-as-of`; operator code, outside the store, trusted (ruled).
- **The lease road against the holder road** (CONCLUSION R1 names both;
  the lease is the default). Lease: one local seek per value fact at
  decision (the lease row), one lease act and one `lease-locks` query per
  batch of up to 256 locks, lease rows durable and replicated with the
  layer, so a worker restart changes nothing. Holder: one query round trip
  per offer, a per-task TaskGlobal with a bound and a time-to-live, no
  seek at decision, and after a worker restart every undecided offer is
  refused `:no-such-lock` until the door hands its locks again. The
  delivery function is the one seam: its body is the only code that
  differs.
- **Grammar as data now, facts later (ruling 8, O20's half).** The
  `:mention` rule is a map entry, not a branch in code: `{:mention
  {:subjects-at [:persons]}}`. Stage 6 reads the same map from facts
  (`{:e key :k :grammar :v {:subjects-at [...]}}`) and `subjects-of` does
  not change. Shape checking and opacity (ruling 6) are stage 6's; this
  stage refuses only the one shape it must read (L12).
- **Per-act grain (I-L6, D12, O8).** The door seals every value of the act
  under one leased lock (it reads the grain from `lease-locks`); an act
  whose citations do not fit the grain in force at decision is refused
  `:grain-mismatch`, recorded, its leases destroyed (L30). One lock per act, one wrap over the
  act's union with marked? = any value fact marked; the row or record
  placement by the layer's kind or any `:own-row` mark; a forget of any
  value of the act erases the act (the lock id is the act's). Where an act
  mixes marked and unmarked values the rig's act lock dies with any subject
  of the union, which erases at least everything the model erases and
  possibly more; phase 8 reports the difference (L6). An act spanning
  tasks is impossible while placed by layer; O8's other half is stage 3's.
- **Refusals as data, extended (revised 26 September).** Two face
  refusals, unrecorded: `:not-sealed` (structural) and `:no-such-lock` (at
  the delivery). Nine recorded reasons after stage 1's list, in order:
  `:does-not-open`, `:malformed-value`, `:value-shape`, `:too-many-subjects`,
  `:grain-mismatch`, `:no-such-person`, `:person-forgotten`,
  `:person-already-made`, `:no-such-value`. `:value-shape` and
  `:too-many-subjects` were face refusals on 25 September; they need the
  plaintext, which exists only after the delivery, and every refusal after
  the delivery is recorded so that it consumes its leases (L27). A forget that changes nothing is a yes (the model
  admits it), so "a refused name stays refused" and "a second forget
  changes nothing" both hold as the sources say them.
- **What a forget does not touch.** `:heads` (ids and stamps), `:answers`,
  `:stood-on`, the stamp, the row's `:e :k :replaces :mark`, and its
`:sealed` bytes and `:digest`, which no held lock opens or reproduces once
the lock is gone (revised 26 September): the fact
  stays a fact with its position and date (I-L8: removal of meaning, never
  of the record's place). A forgotten value can still be replaced (E2 C1 ×
  value forget: "it can still be replaced").
- **What is not built.** No restore (O16, L17): the rig is not durable. No
  group rule (O17, OP12) and no grain on shared layers (O7): stage 3's
  layers do not exist here; a `:lock-grain` fact into a layer with no owner
  is refused for want of a permission as the model does (D11), which stage 1's
  checks already give. No re-class handling beyond stage 1's (E5 K1 ×
  re-class: the row stays where the value is; a later forget is nil-tagged
  and decided on the home, P16).

## State primitive selection

- `$$layers` (PState, extended): durable, partitioned by layer. Per
  admitted act the write volume grows by one `:by-stamp` set and, in
  personal and hand layers under per-value grain, f lock-row sets (one
  under per-act) and f lease-row deletes, all bounded by the act. Per lease
  at most 256 lease-row sets; per session close one delete. Per forget: one
  delete or up to f nils, one ledger set. `:leases` is the source of truth
  for unconsumed locks (revised 26 September). Source of truth for the lock store's rows and the
  erasure ledger; `:by-stamp` is a derived view rebuildable from
  `:answers`.
- `$$persons` (PState): durable, on every task. Per person act: N sets of a
  60-byte entry, once per act. Source of truth for person locks and their
  dates; every task's copy is the home's copy, carried.
- `$$clock` (PState): as stage 1.
- No TaskGlobal (see "What is not a PState"): the holder road's TaskGlobal
  is described, not built. The crypto primitives are stateless functions;
  `SecureRandom` is a JVM object created once per process in
  `rig.store.locks`, not per-task state.
- The door's leased locks: in the door's memory (the client), not the
  store's state; re-taken by `lease-locks` after a door restart.
- No external system. The fingerprint secret stays a constant in code (P6).

## Resource usage analysis

Sizes as stage 1 estimates them (ids about 10 bytes, a name about 60, a
fact id about 70, a stamp 8). A sealed blob is 12 (nonce) + 16 (tag) + the
plaintext length, stored as raw bytes (revised 26 September; the 25
September text inflated every blob by 4/3 as base64). A lock id `[name i]`
is about 70 bytes.

### Disk usage (PStates and the depot), per task

- A log row for a value fact: stage 1's about 60 bytes of framing and ids,
  plus `:sealed` at 28 + |EDN| (the toy's 40-character values: about 68),
  plus `:lock-id` about 70, plus `:digest` 32, plus, when kept in the
  record, the lock record: two short vectors (about 25), one 60-byte blob
  (a 32-byte lock sealed: 12 + 32 + 16) or per any-of person one such blob.
  About 230 bytes for a row lock's row, about 320 for a record lock's.
- `:locks`: key about 70 bytes, record about 90: the bench measured 169
  bytes a row as raw bytes, 189 as base64, whatever the value's size
  (runs/phase7-lock-growth.txt, with its own id form); one per value in
  personal and hand layers, one per act under per-act. This is M2's number.
- `:leases`: per unconsumed lock the session key (shared), the lock id
  about 70, `:under` about 10 and a 60-byte blob: about 140 bytes, for the
  life of a lease only.
- The depot record (revised 26 September): each value fact grows by 28
  bytes of seal and a lock id of about 70; its plaintext is no longer
  there.
- `:erased`: about 85 + 20 per forgotten lock. Grows with forgets only.
- `:by-stamp`: 8 + 60 per admitted act.
- `$$persons`: about 60 bytes per person, on every task.
- The depot: see above; control facts unchanged.

At 100,000 values on one hand layer on one task (M2's scale): `:locks`
about 17 MB (the bench's 169 bytes a row), the rows about 23 MB, `:by-stamp`
about 7 MB per 100,000 acts.

### Memory usage (TaskGlobals)

None. AES-GCM `Cipher` instances are created per call and discarded; the
JVM's provider caches the algorithm, and the cost is microseconds against
a seek's half millisecond.

### Minimization

- The lock record's two vectors repeat the person ids the act's subject
  slot already holds on the answer record; they are kept on the record
  because `crypto/open` and `wrap-closed` need the wrap without a second
  read, and the sealing order must be reproducible from the lock alone.
  About 25 bytes per value.
- Raw bytes are used (L4, revised); no text form is kept anywhere.
- The lock id repeats the lease name in every row of a lease; a lease-local
  index alone would save about 60 bytes a row at the cost of a second read
  to resolve it. Kept whole: the id must name the lock without context.
- Under per-act grain in an agent layer the act's lock record is repeated
  in every row of the act (L6) so a row opens on its own read; one act
  lock kept once (say in the first row) would save about 110 bytes per
  further row at the cost of a second row read on every open. Kept
  repeated: opens are the frequent operation.
- `:by-stamp` duplicates the stamp the answer record holds; it exists so a
  read as of T is a range, not a scan; 68 bytes per act.
- `$$persons` on every task duplicates N times what one task could hold;
  costed above as the price of local opens; 60 bytes per person per task.

## Rig choices proposed

Each is a pick where the rulings are silent, one sentence of what and one
of why. None changes PROGRESS.md. The build session copies the ones it
keeps into RIG.md with the next free numbers.

- **L1. Person locks live in one PState, `$$persons`, on every task,
  written by the stream gate through a `|all` fan-out inside the person
  act's event.** Why: every write about a person and every open needs the
  person's lock where the value is, and a lock on one task would put a hop
  and a second atomic group inside every decision (costed under "PState
  Design" and "Partitioning efficiency").
- **L2 (revised 26 September). The depot holds sealed values, lock ids and
  control values only: the door seals each value under a lock the gate
  leased, and no lock ever passes through the depot.** Why: tonight's
  default (CONCLUSION R1: "nothing that could open a value may ever sit in
  the depot"); Rama offers no per-record erase at runtime (trimming is a
  count cap, tombstones are a migration), so the depot's copy can only be
  made unopenable, which the lock's death does. The 25 September L2, the
  plaintext gap left open, is withdrawn.
- **L3. Encryption is AES-256-GCM from `javax.crypto`, 32-byte locks from
  `SecureRandom`, a fresh 12-byte nonce prepended to each sealed blob; a
  wrap is enforced by key wrapping: the value lock sealed under each
  required person's lock in sorted order, and one copy of that per any-of
  person sealed under theirs.** Why: one primitive, no dependency,
  authenticated so a corrupted slot fails closed, and the wrap's rule is
  what can be decrypted rather than a check a reader could skip. The
  sealed layout (nonce, ciphertext, tag) and the plaintext it seals (the
  UTF-8 bytes of the canonical EDN, P12) are first-record (revised 26
  September: they are now the depot's and the log's bytes).
- **L4 (revised 26 September; first-record). Ciphertext, wrapped locks,
  lease rows, value digests and person locks are raw bytes in `byte/1`
  slots.** Why: the lock-growth bench measured a lock row at 169 bytes raw
  against 189 as base64 (runs/phase7-lock-growth.txt); `byte/1` ran there
  as a schema class and round-tripped a byte array (BENCH_NOTES-locks.md);
  nothing needs text (tests compare with `java.util.Arrays/equals`, control
  values stay EDN text in `:v`). The 25 September pick, base64 in `String`,
  is withdrawn.
- **L5. The lock store's rows, the erasure ledger and the by-stamp index
  are fields of the layer's value in `$$layers` (`:locks`, `:erased`,
  `:by-stamp`; revised 26 September: and `:leases`); a lock id is
  `[lease-name i]` (L21; `[:value fid]` and `[:act name]` withdrawn); a row lock
  lives only in `:locks` and the log row carries `:lock-id` with `:lock`
  nil, a record lock lives in the row's `:lock`.** Why: the merge rule for
  data sharing the layer's key and partitioner, costed against separate
  PStates; the row's deletion and the record's excision stay two different
  writes as ruling 7 reads them.
- **L6. Under per-act grain the act gets one lock, wrapped once over the
  act's subject union with marked? true when any value fact is marked
  `:die-with-any`, kept as a row when the layer's kind is personal or hand
  or any fact is marked `:own-row`, else repeated in every row; a forget of
  any of its values erases the act.** Why: "one small lock shared by all
  values in an act, forgettable only as a whole, wrapped under the act's
  union" (ruling 7 and its sharpening); for an act mixing marks the rig's
  lock dies with any subject of the union where the model's per-fact wraps
  die more narrowly, an erase-more-never-less difference phase 8 reports
  (D12, O8).
- **L7. Persons are data in a store layer `:people` (kind `:store`, class
  by layer, no owner, operator-only): a person is made by an operator act
  with a `:person` fact, forgotten by one with a `:forget-person` fact;
  both keys are control keys (no lock) and placed keys (name class nil).**
  Why: every forget is a fact (D3) in some layer decided by some gate, Bob
  has no layer of his own in the model's world, and a store layer on the
  stream gate needs no mechanism the store lacks (O12).
- **L8. The person forget's date is its fact's stamp on `:people`'s home;
  `wrap-closed` orders person forgets by those stamps.** Why: all person
  forgets are decided on one task, so their stamps are a total order that
  stands in for the model's `:order`; the model's single global stamp is
  named in its README as the toy's kindness, and stamps are one comparable
  scale (I-O3).
- **L9. The person fan-out runs on the fresh and the recorded path alike,
  carries the home's `$$persons` entry as it stands, and writes it with an
  unconditional `termval` on every task.** Why: a crash between the home's
  commit and a child leaves a record whose fan-out must still complete
  (RQ 2), a carried entry is idempotent and order-safe under partition
  ordering, and reading each task's copy before writing would add a seek
  for nothing.
- **L10. A value forget is offered by the layer's owner under their
  own-layer permission or by the operator; R13's exception for owners grows
  from `:lock-grain` to `:forget`; a target that is not a row of this
  layer's log on this task is refused `:no-such-value`; a target with no
  lock or with a lock already in the ledger is admitted with no change;
  the effect is `:row-deleted` for a row lock and `:excised` for a record
  lock, whoever offered.** Why: `op-forget-value` has the owner or the
  operator order it and `apply-control` admits a no-op forget; the lookup
  by the target's name and index on the home is one seek and covers
  unknown, foreign-layer and not-yet-visible targets with one reason (O1).
- **L11. A value whose wrap names a person with no person lock, or a
  destroyed one, is refused (`:no-such-person`, `:person-forgotten`); a
  making act whose `:owner` has no person lock is refused
  `:no-such-person`; a forgotten person's control facts (a forget, a grain
  switch) are still admitted.** Why: a lock cannot be wrapped under a lock
  that does not exist (E6 Q1: "refused as data, never thrown"), and this is
  the simplest reading of O11 that keeps the gate total. Revised 26
  September: a lease act whose layer's owner has no person lock or a
  destroyed one is refused the same way, recorded; an offer citing a lease
  made before its owner's forget is refused `:no-such-lock` on its face,
  since the lease no longer opens.
- **L12. The `:mention` grammar reads `:persons` from a map value as a
  collection of keywords; any other shape under `:mention` is refused
  `:value-shape`, recorded, after the delivery (revised 26 September: the
  value is sealed until the gate opens it, and every refusal after the
  delivery consumes the leases); the grammar is a constant map keyed by
  fact key.** Why: the gate must read this one shape to wrap correctly,
  a malformed one cannot be admitted under a wrong wrap, and a map entry is
  what stage 6 turns into facts (O20's half).
- **L13. The act's subject union is capped at 256 persons; over the cap the
  act is refused `:too-many-subjects`, recorded, after the delivery (revised
  26 September, as L12).** Why: the answer
  record's `:subjects` and a lock record's `:any-blobs` must be bounded by
  an enforced mechanism, and [F3] already capped carried subjects the same
  way.
- **L14. A retract (`:v nil`, P13) and every control fact get no lock;
  their slot stays plaintext.** Why: a retract has nothing to seal and the
  model gives control facts none (D2).
- **L15. Size tracking stays off on `:locks`; M2 counts and sizes rows by
  one iteration per measurement point.** Why: tracking adds a read to every
  hand-layer write, the writes M2 times; one 100,000-entry iteration costs
  about half a second once.
- **L16. A value forget after a person forget writes the ledger and the
  value then shows the value forget's date.** Why: the model's `erasure`
  reads the ledger first (O14); the rig decides the same way.
- **L17. No restore in the rig (O16); a forget by a person whose permission
  is revoked is refused `:permission-revoked` as the model does (O13); the
  order of a person's forget against a read-out (O15) is the owner task's
  own order between the read-out event and the fan-out child, which stage 4
  shows with the crash hook holding a child.** Why: the rig is not durable;
  the model's refusal is the one the tests compare against; a task is
  single-threaded, so the order exists and can be forced.
- **L18. The read as of a moment is a query topology `read-as-of [layer T]`
  on the layer's home, returning `{:as-of :facts :erased}` with no
  visibility filter.** Why: it needs five reads on one task (a query
  topology, not five round trips), stage 5 owns visibility and may keep or
  replace it, and its output is the shape the A-case tests compare with the
  model's `read-as-of`.
- **L19. The offer's carried subjects (the tool's) join every value fact's
  own subjects, so a marked value's wrap and a per-act wrap include them.**
  Why: "the three sources applied to that fact" names the tool as a source
  of the value's subjects; the model's tool names none, so parity is
  unaffected.

Added 26 September, for the lease road (tonight's default, not a ruling):

- **L20 (first-record: the fact). A lease is an act into the layer, tagged
  with its class, one control fact `{:e s :k :lease :v {:count n}}`, n a
  long in 1..256, with a `:session`; any holder of a permission covering
  the layer may lease, and the operator; the yes carries `:lock-ids`.** Why:
  R1's lease is "an offer like any other, answered by name"; a bound per
  lease bounds each event's writes; the ids are computed, so a resent lease
  returns them with nothing stored.
- **L21 (first-record). A lock id is `[lease-name i]`.** Why: the lock
  exists before the value, and a function of a name decided once mints each
  id once, which is what makes "answered from the record, destroyed" hold
  by construction.
- **L22. Lease rows live at `$$layers [layer :leases session lock-id]` as
  `{:under p :sealed bytes}`, both levels subindexed, size tracking off.**
  Why: the merge rule (same key and partitioner as the layer); session
  first gives a session close one direct delete and `lease-locks` one range
  read.
- **L23. The session owner, whose person lock seals a lease row, is the
  layer's owner in a one-owner layer.** Why: every session writing a
  one-owner layer writes for its owner, and it is the one person lock the
  gate already reads for every act there; forgetting the owner then kills
  her unconsumed leases with no write.
- **L24 (first-record). A value fact is offered as `{:e :k :sealed bytes
  :lock-id id :replaces :mark}`; the log row keeps `:sealed` as offered
  beside `:v` (control text), with `:lock-id`, `:lock` and `:digest`.** Why:
  one ciphertext in the depot and the log, dying with one lock; two
  nullable fields keep one typed shape with no `Object`.
- **L25. One delivery function, `deliver-lock>` (layer, session, lock id,
  the persons read → the lock or nil), with the lease body built and the
  holder body described.** Why: R1 names both roads; one seam keeps the
  decision code the same under either.
- **L26 (first-record: what the record holds). The parts digest (the rig
  secret, over the offer minus its name and every `:sealed`, lock ids
  included) stays on the answer record; a value digest, HMAC-SHA256 keyed
  by the value's lock over its canonical EDN bytes, goes on each value
  fact's row, for a yes only.** Why: R1's rider; one act digest keyed by
  one lock would still confirm another forgotten value of the act to a
  holder of the first lock; lock ids in the parts digest make a resend's
  cited lock the recorded one.
- **L27 (first-record: the recorded reasons' names). The refusal order:
  stage 1's structural face refusals and `:not-sealed`; the record path;
  `:no-such-lock` at the delivery, on the face; then, recorded, stage 1's
  list, `:does-not-open`, `:malformed-value`, `:value-shape`,
  `:too-many-subjects`, `:grain-mismatch` and the 25 September four.** Why:
  R1: a missing lock is refused on its face so a resend is allowed; every
  refusal after the delivery consumes the leases, so no refused offer
  leaves openable bytes; stage 1's order stands in front, so the model's
  histories answer as before.
- **L28 (first-record: the fact). A session close is an act `{:e s :k
  :session-closed :v {:session s}}` by the session or the operator; on a yes
  it deletes `:leases[s]` whole.** Why: the default destroys unconsumed
  lease rows when their session closes, and every change is an act; when a
  session layer closes (item 82) stays Sid's.
- **L29. `lease-locks [layer session]` returns every unconsumed lock of the
  session in the layer, unwrapped, with the grain in force.** Why: one range
  read; a restarted door needs all of them; the person lock never leaves
  the module.
- **L30. An act's lock citations must fit the grain in force at decision
  (per value: a distinct id per value fact; per act: one id for all),
  else `:grain-mismatch`, recorded.** Why: the door seals before the gate
  decides and the gate never re-seals the depot's bytes; recorded, so a
  replay after a later grain switch cannot turn it into an admission.

## What later stages consume, and where it is

Stated as what this stage leaves, not as their design (revised 26
September; the interfaces tonight's parallel builds use are under "The door
and the lease road").

- **A value and its lock, on one task.** Row at `$$layers [layer :log name
  idx]` with `:sealed` (the offered ciphertext), `:lock-id`, `:lock` (record
  locks), `:digest`; row locks at `[layer :locks lock-id]`; lease rows at
  `[layer :leases session lock-id]`; the ledger at `[layer :erased
  lock-id]`; person locks at `$$persons [p]` on the same task. Opening is
  `rig.store.locks/open-value>` (or its pure steps `erasure`, `unwrap`,
  `open`, given those reads).
- **For the micro store (stage 3).** `$$persons` is readable as committed
  state on every task, so the micro gate wraps a shared-layer value with
  local reads; `locks/wrap` with owner nil gives 7b as written (any-of over
  the value's own subjects, required empty; marked: required all); the lock
  record shape is what "wrapped locks in the record" stores in their rows;
  `seal`, `open`, `wrap`, `unwrap`, `value-digest` and `unlease` are pure
  given their bytes (a microbatch retry regenerates fresh nonces and
  exactly-once replaces the attempt's writes, so one encoding survives).
  Under tonight's default a value offered into a shared layer is sealed at
  the door too, so the micro gate needs lease rows on the value's task and
  a delivery; where they live is stage 3's, and `unlease` serves whatever
  row it reads. `:lock-grain` into a shared layer is refused for want of a
  permission as the model does (D11, O7 left as is). The `:people` layer
  stays on the stream gate. Under per-act grain an act placed by entity
  spans tasks: one lease id `[lease-name i]` cited by facts on several
  tasks would need its lease row on each; that half of O8 is theirs.
- **For promotion (stage 4).** On the owner's task, inside the gate's
  event: the local reads above, then `erasure`, `unwrap` and `open`; a
  closed value is `:source-erased` and no crossing fact is written; an open
  one gives the plaintext the landing carries. Under tonight's default the
  landing offer into the micro depot must not carry that plaintext either:
  it has to be sealed under a lock leased for the target before the append,
  which is stage 4's to design (named under "What this plan could not
  settle"). The crossing fact is a control fact, no lock. A value forget
  and a read-out on the owner's task are ordered by that task; a person
  forget reaches the owner's task as a fan-out child, ordered against the
  read-out event by the task thread (L17). The copy's wrap is the target's,
  computed by stage 3's gate from the target's owner (none), the grammar
  and the tool.
- **For reads (the read exit, planned in parallel, and stage 5).**
  `open-value>` and `open-row>`; `read-as-of`'s output shape; `:by-stamp`
  per layer (stamp → name); the ledger keyed by lock id, which maps to fact
  ids through the rows that cite it (one row under per-value grain, the
  act's rows under per-act); every index they keep over values is purgeable
  by `[name idx]`, and a value forget calls that purge from its own event
  (PState ownership, stage 1 [F12]).
- **For tools and grammars (stage 6).** `rig.store.grammar/grammars`, the
  map, and `subjects-of`, the function over it; making the map a read of
  `:grammar` facts leaves `subjects-of` unchanged. The `:value-shape`
  refusal is the seed of ruling 6's shape check.
- **For the numbers (phase 7).** M2: iterate `[layer :locks]` for count and
  serialized bytes per measurement point, raw bytes (the bench's 169 bytes
  a row); lease rows are not counted, since they are consumed at decision.
  M1: agent layers write no lock rows; their per-act writes are the row
  (with the lock in it), `:by-stamp` and one lease-row delete per value
  fact, and a lease act per batch.
- **For the replay (phase 8).** Differences to report by construction: the
  `:people` layer and the two person acts (the model has neither; a model
  `:forget-person` maps to the operator's act); the person forget's date
  (a stamp on `:people`'s home, not a global stamp); per-act acts mixing
  marks (L6); the model's `[:act name p]` against `[lease-name i]`; and the
  lease road (the model has no lease act, no delivery and no
  `:no-such-lock`: a model offer replays as a lease, a `lease-locks`, and
  the sealed offer; CONCLUSION puts both roads into the model later).

## The door (`rig.store.client`, extended; revised 26 September)

- `(lease! store who layer session n)` → the lease act through
  `offer-until-answered!`; returns `:lock-ids`. `(lease-locks store layer
  session)` → `foreign-invoke-query` of `lease-locks`. `(close-session!
  store who layer session)` → the `:session-closed` act.
- `(offer! store offer)` and `offer-until-answered!` grow a sealing step:
  given an offer with plaintext value facts and the session's held locks,
  the door takes one unused held lock per value fact (per-value) or one for
  the act (per-act, by the grain from `lease-locks`), seals, and sends the
  sealed facts; it leases more when it holds too few. The door's held locks
  are an atom in the client's store handle, not store state. `lookup`
  compares the parts digest only.
- `(make-person! store p)`, `(forget-person! store p)` → the operator's
  acts in `:people`, through `offer-until-answered!`.
- `(forget-value! store who layer fid)` → the forget act by `who` (the
  owner citing `[who layer layer]`, or `:operator`), standing on the
  target with the stamp the client read.
- `(read-as-of store layer T)` → `foreign-invoke-query` of `read-as-of`;
  `(opens? store layer fid)` → `read-as-of` at the value's stamp, the fact
  by id: `{:value v}` or `{:erased-at s}`.
- `(lock-rows store layer)` → `foreign-select [(keypath layer :locks) ALL]`,
  the rows with their serialized sizes (for M2); `(lease-rows store layer
  session)`; `(ledger store layer)`; `(person store p)` →
  `foreign-select-one [(keypath p)] $$persons`; `(person-on-task store p
  task-key)` → the same with `{:pkey k}` for a key of
  `gen-hashing-index-keys` that lands on that task (testing.md).
- `(seed! store world)` grows: the `:people` layer first, then a `:person`
  act per `(:persons world)`, then stage 1's layers and grants; the default
  world adds `:persons [:alice :bob]`.
- `(depot-record store layer offset)` → `foreign-depot-read`, for P6 check
  1 (revised: the depot record now holds sealed bytes, not the gap).
- Stage 1's suite then sends its value acts through the sealing step; its
  cases that sent a malformed value (R17) now see `:malformed-value`,
  recorded, where they saw `:malformed` on the face, and its cases into an
  unknown layer with a value fact now see `:no-such-lock` on the face; the
  build updates those expectations and names each in its notes.

## Namespaces and tests

Revised 26 September: `rig.store.crypto` and `rig.store.lock` fold into
`rig.store.locks`, the namespace tonight's parallel stages build against.

- `src/rig/store/locks.clj` — public: `seal`, `open` (nil on any failure,
  `Throwable` caught), `wrap` (K, wrap, person entries → lock record),
  `unwrap` (lock record, person entries → K or nil), `value-digest`,
  `fresh` (n locks and nonces over `SecureRandom`), `lease-ids`, `unlease`,
  `canonical-bytes`; the model's four in executable form: `wrap-of` (the
  model's `wrap` under `:owner-required`), `wrap-closed` (stamps for
  `:order`), `lock-for` (placement: row or record, from settings, the mark
  and the grain), `erasure` (ledger entry, lock record presence, person
  entries, wrap → nil or `{:stamp :how}`); `read-values` (open, decode,
  value-domain check, subjects, citation-against-grain, one catch) and
  `check-resend` (the record path's value check); the dataflow ops
  `deliver-lock>`, `open-value>`, `open-row>`; the install functions
  `layer-fields`, `declare-pstates!`, `declare-queries!`. Pure except the
  dataflow ops and `fresh`.
- `src/rig/store/grammar.clj` — `grammars`, `subjects-of`.
- `src/rig/store/envelope.clj` — control keys grow (`:person`,
  `:forget-person`, `:lease`, `:session-closed`), placed keys grow
  (`:person`, `:forget-person`); fact parts grow by `:sealed` and
  `:lock-id`; `:not-sealed`; the control-value checks for `:forget`,
  `:person`, `:forget-person`, `:lease`, `:session-closed`; `digest`
  removes every `:sealed` before hashing; the value-domain checks move to
  a function `read-values` calls.
- `src/rig/store/gate.clj` — `decide` extended with the lock effects, the
  lease and session-close branches, the forget branches, the person
  branches, the recorded reasons in L27's order, the consumption list and
  `:how` and `:lock-ids` on the answer; still pure and total. The stamp
  through `rig.store.clock`.
- `src/rig/store/module.clj` — the install lines and the call sites inside
  the one event ("Interfaces tonight's parallel stages build against").
- `src/rig/store/client.clj` — the door, as above.
- `test/rig/store/lock_test.clj` — pure tests, no cluster:
  1. **The A cases, both directions, at the lock level** (R8): `wrap-of`
     and `wrap-closed` agree with `formal.model/wrap` (reading
     `:owner-required`) and `formal.model/wrap-closed` on every A case's
     inputs (A1 to A8: the owner, the subjects, the mark, then the forgets
     in the case's order) and on generated inputs (test.check: owners nil
     or a person, subject sets, marks, forget sequences), comparing
     open/closed and the date; then the enforcement direction: build each
     case's lock record with real locks, destroy the persons the case
     forgets (drop their lock bytes), and assert `unwrap` then `open` gives
     the value exactly when the model says `:open` and nil exactly when it
     says `:erased`, for the one-owner and the shared layer alike.
  2. Crypto round trips: seal/open, wrap/unwrap under required chains and
     any-of copies, a lease row's `unlease`, `value-digest` stable for equal
     plaintexts sealed twice (other bytes, same digest) and different for
     other plaintexts or other locks.
  3. `subjects-of` on well-formed and malformed `:mention` values.
  4. `lock-for` placement: personal and hand → row, agent → record,
     `:own-row` overrides, per-act citation and the union wrap.
  5. No throw: `open`, `unwrap`, `unlease`, `read-values` on garbage,
     truncated and swapped blobs and on a lock of the wrong length.
  6. `check-resend`: equal plaintext under the same lock ids answers;
     other plaintext refuses `:name-taken`; a lock that no longer opens
     skips its value.
- `test/rig/store/forget_test.clj` — one IPC, `{:tasks (rand-nth [2 4 8])
  :threads 2 :workers 1}`, seeded with the model's world (persons, the
  three layers, the grants), under `TopologyUtils/startSimTime` so stamps
  and dates are exact, every value act through the door; `testing` blocks:
  1. **A1 and A8 through the module, both directions** (the model's A
     cases; A2 to A7 need the group layer and run through the module in
     stage 3's suite, here at the lock level above): A1, a note and a
     mention of Bob in `:alice`, then `forget-person! :alice`: `read-as-of`
     shows both erased with the forget's stamp; `$$persons [:alice]` on
     every task has `:lock` nil and `:erased-at` the stamp, checked with a
     key per task. A8, a mention of Bob in `:alice`, `forget-person! :bob`:
     the value opens; Bob's lock nil on every task. Each asks the model for
     its answer to the same history and compares.
  2. **The missing lock, then the resend.** An offer citing an id never
     leased: `:no-such-lock`, no answer record under the name, no row, no
     lease row touched. The same name resent, sealed under a leased lock:
     decided fresh, yes. Also an id leased to another session: the same
     face refusal, and that session's row is still there afterwards; and
     an id consumed by an earlier offer: the same.
  3. **Lease consumption, three ways.** After an admission the cited lease
     row is gone and the value's lock row exists under the same id
     (personal) or the row's `:lock` holds it (agent). After a recorded
     refusal (the permission revoked between the lease and the offer) the
     cited lease row is gone and no lock row exists. After an answer from
     the record (the same offer resent) the lease row is still gone, the
     lock row unchanged, and nothing was written (the task's `$$clock`
     unchanged).
  4. **P6 widened, three places, after each forget.** For a value forget
     in `:alice` (row deleted), in `:alice-agent` (excised), and for A1's
     person forget: read the raw depot record with `foreign-depot-read`
     and try every lock the store still holds (every lease row, lock row
     and record lock of every layer, unwrapped with every live person lock,
     and the live person locks themselves) on its `:sealed` bytes and on
     the log row's: none opens; no held lock reproduces the row's value
     digest from the known plaintext; the depot record carries no plaintext
     value (its value facts have `:sealed` and no `:v`).
  5. **A resend with other content, before and after a forget.** Offer X
     admitted; X's name resent with another plaintext sealed under the same
     lock ids: `:name-taken` (the value digest differs). Forget the value;
     the same resend: the recorded yes, nothing written. X resent with the
     same plaintext sealed again (new bytes): the recorded yes, before the
     forget and after.
  6. **A lease resent.** The same lease name offered twice: the same
     `:lock-ids`, one set of lease rows, and `lease-locks` returns the same
     lock bytes both times.
  7. **Two crashes, each replaying to the same answer** (R4: at least once,
     never a count). Between the lease and the offer: the hook armed at
     `:seen` on the offer's name, so the worker dies on the offer after the
     lease completed; the lease replays from its record (same ids, the
     same lease rows, no new bytes) and the offer then decides yes, with
     the lock the door sealed under. During the gate event: the hook armed
     at `:before-writes` and, in another block, `:after-writes` of an
     offer; the event's writes are discarded, the lease rows are still
     there, the replay delivers the same locks and admits; the answer
     equals the answer a run without the crash gives, and the value opens.
  8. The 25 September cases, kept through the door: the ciphertext claim
     (the row's `:sealed` is not the value's EDN; `open-value>` gives it
     back); a value forget in `:alice` (row deleted, ledger `{:stamp :how
     :row-deleted}`, the value erased at every T at or after its stamp,
     the other values of the layer open, `:heads` unchanged, the fact still
     replaceable); in `:alice-agent` (excised: the row's `:lock` nil, ledger
     `:excised`, no lock row before or after); in `:alice-hand` (row); an
     `:own-row` value in `:alice-agent` gets a row; per-act grain (switch,
     an act of three values under one lease id, one lock row, a forget of
     one erases all three, a value written before the switch keeps its own
     row, and an act citing three ids after the switch refused
     `:grain-mismatch` with its leases gone); a second forget of the same
     value: yes, nothing changes, the first date; a forget of a value
     already closed by a person forget: the ledger's date shows (L16); a
     forget by Bob of Alice's value refused
     (`:permission-does-not-cover-this`); a forget naming an unknown or
     foreign-layer target refused `:no-such-value`; a forget of a control
     fact: yes, nothing changes; a marked (`:die-with-any`) mention of Bob
     in `:alice` dies with Bob; a write about a person with no lock refused
     `:no-such-person`; after Alice is forgotten a lease into `:alice`
     refused `:person-forgotten`, an offer citing a lease from before her
     forget refused `:no-such-lock` on its face, her grain switch still
     admitted; making a person twice refused, forgetting twice yes with the
     first date; the time-travel rule (a read as of the first act's stamp
     shows it and not the second act, and shows the erasure date of a
     value forgotten after that moment; a read before any stamp is empty);
     the crash mid-person-forget (the hook armed on the fan-out child: the
     worker restarts, the ack or the resend answers, and every task ends
     with the lock destroyed, at least once, R4); the crash mid-value-forget
     before the writes (no ledger, no deletion; the replay decides the
     same); the subject slot on the answer record (owner ∪ grammar ∪
     carried); the lock store count per layer (personal and hand grow by
     one per value, agent by none).
  9. **Session close.** A session leases 8 and uses 3; `:session-closed`:
     its 5 unconsumed rows are gone, another session's rows in the same
     layer stay, an offer citing a closed lease is refused `:no-such-lock`,
     a second close is a yes that changes nothing.
  Run with `clojure -M:test rig.store.lock-test rig.store.forget-test` from
  the rig folder, then the whole suite with stage 1's namespaces, whose
  value acts go through the door.

## Design difficulty log

Written while designing, first person.

- **Where the person locks live.** Three candidates: one task per person
  (`|hash`), task 0 (`:global?`), every task (`|all`). The first two put a
  hop inside the decision of every act that names the person, which is
  every act in a one-owner layer (the owner is always required), and inside
  every open; stage 1's whole correctness story rests on the decision
  being one atomic group on one task. Once I costed the hop per act
  against N writes per person act, `|all` was not close, and it is the
  case pstate-schema.md names for `|all`. The thing I went back and forth
  on was whether a `|all` fan-out inside a stream event is acceptable at
  all under retry; stream.md's partition ordering and the carried-entry
  `termval` settled it.
- **The depot's plaintext (O2).** The hardest part, because every honest
  road ends at "Rama's depot is an immutable log". I looked at trimming (a
  count cap), tombstoning (a migration, not a runtime effect), encrypting
  to a store key (the operator can still read it, which is not what "gone
  for everyone" means), and client-sealed values with the lock delivered
  out of band through a query topology into a TaskGlobal. The last one
  works and I nearly put it in; what stopped me was that it is a second
  protocol on every write for a record nobody keeps, against a phase named
  "kept simple". So the rig names the gap, shows it in a test, and writes
  the road down with its cost. Genuinely contested; I would build the road
  in a kept store.
- **The wrap as encryption.** "Required" and "any of" had to become key
  operations, not flags. A chain of seals for required and a copy per
  any-of person was the first shape that made the model's `wrap-closed`
  true by construction (nothing opens without the right locks) rather than
  by a check. The only alternative I weighed, a secret-sharing threshold,
  is more mechanism for the same two cases; not close.
- **Per-act grain with mixed marks (D12).** The model wraps each fact
  under its own mark with one shared id; the ruling says one lock, one
  wrap over the union. One lock cannot carry two wraps unless it is
  wrapped twice; that is a small mechanism, but "forgettable only as a
  whole" and "the coarser cut the person chose" read to me as one wrap.
  Erasing more, never less, was the tie-breaker; phase 8 reports it.
- **A ledger beside the rows, or rows with a date.** Keeping a deleted
  row's date in the row (never deleting) would be one map instead of two;
  the ruling says the row is deleted and M2 counts rows. Not close once
  read that way.
- **`:by-stamp` now or in stage 5.** Stage 5 owns reads; this stage owns
  the read that shows an erasure as its date. A scan of the layer's answers
  would keep every requirement of this stage and cost one seek plus every
  act; one no-read set per act turns it into a range. I put it in because
  the skill's rule is not to trade I/O for simplicity and stage 1 had
  already named the index as belonging in `$$layers` if kept; the validator
  may strike it as over-reach, and the read's output shape does not depend
  on it.
- **The person forget's layer (O12).** A per-person layer fails for Bob; the
  base is the micro gate's; a global "lock store" outside any layer would
  be the model's shape and the one thing the rig must not do (every forget
  is a fact). The store layer `:people` fell out once those were struck.
  Its kind, `:store`, is a new keyword the settings projection must accept
  (stage 1's kinds were three); small, but it touches stage 1's parser.
- **Bytes or base64.** Raw bytes are the right kept-store answer and a
  third smaller; a schema class question the build would have to check
  first. Text, with M2 stating the inflation, was the simplest thing that
  keeps every requirement. A real choice, not a hard one.
- **What "the record" means for a lock.** The row's `:lock` field versus a
  second map keyed by fact id: the first is one write and one read per
  value; the second would make the row lock and the record lock the same
  shape in different maps and blur the ruled distinction. Row field, quickly.

## Self-validation against `artifact-plan-validation.md`

Run against this plan before finishing; the Phase 2 artifact is not
written here.

- **Query topology `read-as-of`:** three input examples given; N and M
  stated for each; the one read that navigates to an empty submap (T
  before any act) is the read that answers the query and cannot be
  avoided; variable, dynamic by emission and a `loop<-`, nothing padded.
- **PState schemas:** `$$layers` and `$$persons` differ in partitioner
  (the layer's home versus every task), `$$clock` in key structure; no
  `Object`; the lock record, the ledger entry and the person entry are
  `fixed-keys-schema`; no polymorphic position (nullable `:blob` /
  `:any-blobs` on one shape; `:lock` nil for a row lock or after excision);
  the unbounded collections (`:locks`, `:erased`, `:by-stamp`) subindexed;
  the bounded ones (`:any-blobs`, the wrap vectors) bounded by the 256 cap
  (L13), an enforced mechanism.
- **Partitioning:** lock rows by the layer's hash (stage 1's justification
  stands); `$$persons` by `|all`, small and rarely written; the table
  filled for N = 1, 16, 128 with seven categories summing to 1.00, seeks
  as totals across tasks, weighted seeks flat at 4.77, the fan-out's
  growth shown as its own column and costed against the alternative with a
  break-even; no justification rests on a later stage's mechanism (each
  hand-off is stated as what is left); the stored-placement question does
  not arise (the placement is ruling 2's and stage 1's).
- **Topologies:** one stream topology, stage 1's, with both stream reasons
  restated for the new concerns; every new concern is part of the one
  decision whose answer the offerer takes from the ack; one query
  topology; no microbatch here; no test-synchronization argument was used
  (the crash tests use the crash hook, R3, P14).
- **Production readiness:** concurrent clients (two forgets of one value:
  the first erases, the second changes nothing; a forget racing a write
  about the person: ordered on the home; a write about a person racing the
  person's making on another task: refused `:no-such-person` until the
  fan-out lands, and the ack of the making act returns only after it, so a
  client that awaits the ack never races it); a client restart (stage 1's
  P6 road); a worker restart at every point of the person act (traced
  under "Topologies"); scale (every unbounded collection subindexed; the
  per-act excision loop bounded by the act); no non-idempotent write
  (every one a `termval` or `NONE>`; the fresh random bytes exist in one
  committed encoding only); the one multi-partition write (the fan-out)
  traced: a partial failure leaves some tasks written, the replay carries
  the same entry to all, nothing is left permanently unexecuted because
  the record's replay repeats the fan-out (L9).
- **Internal depots:** none. **Cross-topology:** none (the query topology
  reads committed state). **Stream `depot-partition-append!`:** none.
- **In-memory state:** none.
- **Minimality, the simplest sketch:** stage 1 plus a lock per value in
  the row, person locks somewhere, a forget that nulls the lock. The plan
  adds `:locks` as its own map (delete it: the ruled row/record distinction
  and M2's number are lost), `:erased` (delete it: an erased value's date
  needs a scan of the layer's forget facts per value read), `:by-stamp`
  (delete it: a read as of T scans every answer of the layer; kept with
  its cost, 68 bytes and one set per act, and named as the one the
  validator may strike), `$$persons` on every task (delete the replication:
  a hop in every decision), the required chain and any-of copies (delete
  them: the wrap becomes a flag a reader could ignore, and a destroyed
  person lock would not make the value unrecoverable), the `:people` layer
  (delete it: the person forget is not a fact, D3 broken), the grammar map
  (delete it: subjects are a branch in code stage 6 cannot move into facts),
  the query topology (delete it: five round trips per read, the anti-
  pattern). Each is required by a named ruling or check.
- **Throughput:** the decision pays one more seek per ordinary act (the
  owner's lock) and nothing more for a record lock; the alternative that
  avoids that seek is a cache (rejected by the CLAUDE.md rule and by the
  destroyed-lock-in-memory hazard). A value forget is 6 seeks, a person
  act 4 plus N writes. No cheaper design meeting every check was found.
- **Spec coverage:** I-L1 (the wrap table under "The shapes" and L3),
  I-L2 (L5), I-L3 (erasure by destroying a lock; the gate reads plaintext
  values it wraps), I-L4 and O2 (L2, with the gap), I-L5 (`:erased` and
  `:by-stamp` rebuildable; `:heads` untouched), I-L6 (L6), I-L7 (`read-as-
  of`), I-L8 (encodings change, meaning does not), D2 (control keys), D3
  and O12 (L7, L8), D9 (`:own-row` in every layer), D12 and O8 (L6); OP8
  (grain read at decision), OP9 (every invariant traced in "Writes" and the
  tests), OP10 (every invariant; the date; both directions), OP11 (L17),
  OP12 (stage 3); RD4 (erasure part), RD7, RD7s; E2 rows C1 × value forget,
  C2 × replace, C2 × value forget again, C1 × person forgets (owner, Bob
  unmarked, Bob marked), E3 L1 × grain switch, L2 × switch back, L2 × value
  forget, E5 K0 to K6 with every write, E6 Q0 and Q1 with every write, E9
  G0 and G1 (subjects) and G2 (a lock like any other): each is a test case
  above or a line in "Writes".

## What this plan could not settle

- Whether `INDEXED-VALS` navigates a subindexed vector with its indices in
  a `local-select>` (paths.md lists it for sequences; not shown for a
  subindexed one). The build checks it with `create-test-pstate` on the
  exact row-vector schema before the query topology; the fallback is
  `ALL` with a counter in a `loop<-`, or reading the act's size and
  `(keypath idx)` per row.
- Whether the query topology's leading `(|hash *layer)` lands on the task
  that `hash-by :layer` chose (the same hash by construction; assumed as
  stage 1 assumed `{:pkey layer}`). The build's first check, with the
  module up: `read-as-of` on a seeded layer returns its acts.
- Whether `ack-return>` before a `|all` behaves as stream.md says (the ack
  sent when the whole tree completes): read from the reference, not run;
  the crash-mid-person-forget test shows it.
- Whether `[B` is accepted as a schema class: not needed with L4; named for
  a kept store.
- The exact relation of a person forget's date to stamps given on other
  tasks by wall time (L8): under simulated time in tests it is exact; on a
  real cluster it holds up to clock skew, which is ruling 4's "unit"
  question and not this stage's to close.
- Whether stage 5 keeps `:by-stamp` and `read-as-of` or replaces them; the
  output shape is what is promised.
- The `:store` kind in stage 1's settings projection and the two new
  control keys in its parser: additive changes to stage 1's namespaces the
  build makes in place; no migration, the rig keeps no records.
