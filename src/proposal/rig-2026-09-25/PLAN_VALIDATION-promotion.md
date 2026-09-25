# Plan validation — stage 4, "promotion"

<!-- The rama skill's phase 2 (phase-2-plan-validate.md, template
artifact-plan-validation.md), run on 26 September 2026 from 03:11 IST by a
fresh-context session (Claude Opus 5.5, effort max) on branch
rig-plan-promotion, worktree /mnt/data/projects/Softland-rig-plan-promotion.
Validates PLAN-promotion.md as committed at d6712941. Default verdict FAIL.

Read for this: SPEC.md (Sid's phase 4 verbatim, "What Rama showed");
PROGRESS.md "Now" on main (lines 44-56 the envelope, 114-118 the two
consequences, 120-165 the sharpenings, 172-186 what stays open); the main
rig worktree's RIG.md "For Sid" and "Defaults taken overnight" (defaults 1,
2, 5, 6); CONCLUSION.md R1 (lines 47-60), R8 (100-104), P6 widened (107);
model.clj exempt? (76), send-read-out, forward, continue-promotion
(592-648), the stream continuation (672-685), commit-decision (711-714),
refusal's read-out branches (444-452), promotion-status (981-1000);
scenarios.clj b-cases (56-77); the probe runs/phase4-probe-sealed-box.clj
and .txt; PLAN-locks-and-forgetting.md (lease rows by session, 408-418,
590-596, person forget on leases 2062-2065); the micro build's
`route-key` (Softland-rig-build-micro, micro.clj:290, at 5afe3c7d);
stream.md (commit boundaries 39-57, ack-return> 233-252); microbatch.md 15.
Nothing under src/app was read. gate_test.clj was not opened. No cluster
was run. The plan's probe was read and checked, not re-run; one probe of
this validation's own ran with no cluster: runs/Phase4ValidateX25519.java,
output runs/phase4-validate-x25519.txt (JDK 21.0.12.1, this machine).
Marks: *checked* = read in a source named here; *derived* = reasoned from
those; *assumed* = not checked. -->

**Verdict: minor-fail.** The architecture holds under tracing: the landing
sealed to a bare, count-1 landing lease minted by the target's gate; the
read-out as a continuation of the request's record past a commit boundary;
the stored forward re-sent on every replay; the landing decided as an
ordinary sealed act with the lease consumed whatever the decision. Nine
failures, each fixable in named lines of the plan, were found and fixed in
place (F1 to F9, marked `[F<n>]` in PLAN-promotion.md). The largest, F1, is
not the self-forgery the plan names in open question 1: a *third* person
with write permission in the target can capture someone else's promotion,
because nothing makes a landing lease the only lease for its landing name.

## Failures found and the fixes applied

| # | Failure | Where fixed |
|---|---|---|
| F1 | A landing lease is not bound one-to-one to its landing. Any writer in T who sees the lease act's plaintext `:landing L*` (a recorded control fact in T) can make her own landing lease `:for L*`, seal her own content to its public key and append a landing named L* to `*micro-offers`. If hers is decided first, L*'s record is hers: the real landing is then refused by digest on its face, and `promotion-status` reads **done** with her content. | Names; step 0; step 1's check; step 4; PR4, PR10; U4, T12; open question 1 |
| F2 | The read-out is not total over the public key. The request's check is "44 bytes" only; a 44-byte blob that is not an X25519 key, or a small-order point, makes `box` return nil, and the read-out has no branch for it (a yes with no box, or a schema throw writing `:box nil`, a poison record replayed for ever). Also the box binds nothing to the landing: no AEAD associated data. | step 1's check; step 2; PR6; U1, U4 |
| F3 | "No promotion stays crossed for ever" holds only for a missing lease row. Any other refusal of a landing on its face (inside one batch, a face-failing envelope under the same name taking the name from the store's; a lease row "leased to another session" read as missing and recorded, which re-opens F1 as a denial) either leaves the promotion crossed with no further send, or lets a forger record a refusal under someone else's landing name. | step 4; micro changes; PR5 |
| F4 | `:forwards` stores a flattened form, not the landing as sent: no `:version`, fields rebuilt into an envelope by code. "Byte for byte" and "holds across a code change between the sends" are then false. | PState Design (`:forwards`); Minimization |
| F5 | The status read issues a read the state already decided (the frontier when the name row is nil: "crossed, not landed" is 4 seeks, not 5), and shows a request admitted after `*as-of` (the time-travel sharpening: "a read as of a moment shows nothing admitted after it"). Weighted seeks 4.10 → 4.05. | Query Topologies; the three tables |
| F6 | The forward's `depot-partition-append!` is preceded by `(|hash lease-name)`, not by `(|direct (ops/current-task-id))` immediately before it (the template's rule; stream.md 49). | step 3; Topologies |
| F7 | The forgets walked omit a person forget of someone the copy is about *between* the read-out and the landing: the bare lease row then opens a copy of a value about a forgotten person until the landing's decision. The same window every in-flight sealed offer has under phase 2; unnamed here. | "The forgets against it, walked"; For Sid |
| F8 | A re-class of the *source* layer between the request and the read-out is not handled: the continuation would read out and write a crossing on the stream gate for a layer it no longer orders. | step 2 |
| F9 | B case 4 needs more than PR13: the requester's person forget must also leave her session in T and her bare landing lease row alone. Phase 2 cuts her *wrapped* lease rows (checked, PLAN-locks-and-forgetting.md 2062-2065); a bare row is not wrapped, but nothing in the plan says a forget-person fan-out or session rule does not delete it. | PR13; T4 |

## Query topology: `promotion-status`

- Input examples present: yes (plan "Query Topologies", input examples).
- Example, pending: N = 2 (request answer, crossing answer nil), M = 2. N == M: yes.
- Example, refused at the read-out: N = 2, M = 2. Yes.
- Example, done on the micro gate: N = 5 (request, crossing, forward on L's home; name row, frontier on hash(landing-name)), M = 5. Yes.
- Example, crossed not landed on the micro gate: the plan's table counts 5; the name row is nil, which already decides "crossed", so the frontier read is not meaningful: N = 5, M = 4. **FAIL as written** → F5: the frontier is read only when the name row holds an answer. After F5, N = M = 4.
- Example, done on the stream gate: N = 4, M = 4. Yes.
- Example, not a request: N = 1, M = 1. Yes.
- M differs across examples (1, 2, 4, 5): marked variable, handled by `<<if` on each answer. Yes.
- Time travel: a request stamped after `*as-of` was not tested by the plan's step 1 (only nil or a no). The model's `promotion-status` returns `:pending` there (it never looks at the request's stamp, *checked*, model.clj 981-1000); the sharpening "a read as of a moment shows nothing admitted after it" (PROGRESS.md "Forget, time travel") says the request is not shown. **FAIL** → F5 (`:none` when the request is stamped after `*as-of`; reported as a difference from the model in phase 8).

## PState schemas

- Groups by (key type, partitioner): no new PState. `:forwards` is a field of `$$layers` (layer key, hash on the layer), the landing lease's `:public` and `:for` are fields of existing lease rows (`$$layers [base :leases s id]`, `$$micro-names [lease-name :leases 0]`). No group of two PStates is added. Pass.
- Object type: none. Pass.
- Fixed-keys-schema for uniform records: `:forwards` values are fixed-keys; lease rows keep their fixed-keys schema with two nullable fields. Pass, with F4: the value's shape changes to the landing envelope as sent (still fixed-keys, one fixed-keys fact in a vector of length 1), and gains `:version`.
- Different instances with different fields at one position: lease rows carry `:public`/`:for` nil except on a landing lease. Nullable fields on otherwise identical rows; not the definterface case. Pass.
- Subindexing: `:forwards` subindexed (a layer promotes without bound). Lease rows keep phases 2's and 3's subindexing. The forward's `:facts` vector has exactly one element, enforced by the store building it (`rig.store.promote`'s `landing-offer`, pure) and by PR10's "exactly one value fact" at the micro gate. `:subjects` is bounded by the request's check ("at most 256", step 1) — above the 100 of the template, but a set-schema inside a fixed-keys value read whole with the forward, never navigated; the bound is enforced by a named rule. Pass.

## Partitioning

- `hash-by :layer` on `*offers` (request): phase 1's P2, one-owner layers placed by layer; the keyspace is layers, one layer's rate is one person's. Pass (phase 1's argument stands).
- `|hash lease-name` before the forward's append: lease names are random (uuids), a large sparse-free keyspace, no hot key. Pass.
- `|hash T` for a stream target (the base while one-owner): the base is one layer on one task until its re-class; that is default 6's placement (R8), not this plan's pick. Pass.
- No `|all`. Pass.

**Partitioning efficiency, recomputed from the plan's rows as totals.**
0.60·5 + 0.05·4 + 0.05·5 + 0.05·5 + 0.05·2 + 0.10·2 + 0.10·1 = 3.00 +
0.20 + 0.25 + 0.25 + 0.10 + 0.20 + 0.10 = **4.10**, proportions summing to
1.00, iterator reads 0, the same at N = 1, 16 and 128 (every read touches
one task or two, never a number that grows with N; at N = 1 the second task
is the same task and the seeks are the same). The plan's number is right
for its rows. With F5's row (crossed, not landed: 4), 3.00 + 0.20 + 0.20 +
0.25 + 0.10 + 0.20 + 0.10 = **4.05**, flat in N.

- All three N present: yes. Every category a row, proportions sum to 1: yes (the mix is assumed, not measured, and the plan says so). Weighted sums computed: yes. Totals across tasks, not per task: yes (a done promotion counts both tasks' reads). Growth with N: none. Pass.
- Justifications from the spec only: placement is derived from where the rulings put the state (the source's lock row on the owner's home, phase 2; the lease row where the target's gate does value work, phase 3's §A). One justification leans on another part's mechanism: the forward's hop depends on phase 3 routing a sealed act by its first cited lock's lease name. Checked against the micro build, not only the plan: `route-key` in Softland-rig-build-micro micro.clj:290 is "the lease name of the first lock any fact cites, else the first fact's entity" (§A). So the micro build follows §A, not the stale `:session` note, and the landing path depends on it: were it by session, the forward's `|hash` and the lease rows would both have to follow (the plan's "one change in `route`" is right). Pass.
- Placement state: none needed; every `f` is a pure function of what the step holds. Pass.
- Write path per promotion (prose in the plan, not a table): read-out 7 seeks on L's home, 1 hop, 1 append, the landing an ordinary sealed act plus 0.08 ms unbox; every term independent of N (derived). Pass.

## Topologies

- Microbatch unless justified: no new topology. The read-out and forward run in the stream `gate` because only it writes `$$layers` and the request's appender waits on the ack (phase 1's reasons). The landing on the micro gate runs in phase 3's microbatch `micro`. Pass.
- Low-latency writes: the request and the read-out, on the stream gate (the door's ack carries the crossing's answer). Pass.
- Stream concerns: the request's decision (ack), the read-out (the crossing must be decided on the owner's task, in the owner's order against forgets: the model's B cases need it ordered on the task where forgets are decided), the forward (an append after the crossing's commit), the stream-gate landing for the base while one-owner (the base is ordered by the stream gate, default 6). Each needs the stream gate's task order or its ack. Pass.
- No topology choice made on test-synchronization grounds; the hold (PR15) is a test device over a design chosen on the spec. Pass.

## Production readiness

- Concurrent clients: two doors promoting the same source into the same T: two requests, two read-outs, two landings on the same head; T's gate refuses the second `:stale-replaces` (recorded), both promotions end (derived). A third party in T racing a forged landing under the same name: **FAIL** → F1.
- Client restart: the door's resend under the request's name re-enters the continuation by name; a lost door leaves its lease until its session closes (open question 2). Pass.
- Worker restart at any point: before the request's commit (nothing visible, replays fresh); after it and before the crossing's commit (the request from its record, the read-out decided fresh on the replay, after anything the task ordered in between, a forget included: the model's order); after the crossing's commit (crossing and forward from the record, the forward re-sent); after the append (a duplicate record, answered by name); in the landing's batch (retried whole, exactly once). Pass (derived; tests T7 a to c).
- Scale: `:forwards` subindexed; lease rows per session bounded by leases taken. Pass.
- Non-idempotent writes in the stream topology: the depot append — "Deduplicated by: the landing's name at the target (the record path; phase 3's F3 inside one batch)". Every PState write is a `termval` by name or a `NONE>`. Pass, subject to F3 (the dedup must not let a face-failing envelope take the name).
- Multi-partition stream writes: the request and read-out on L's home, the append on hash(lease-name), the stream landing on T's home. A failure after any of them replays the record from its start (What Rama showed 2); every step answers from its record by name, and the stored forward is re-sent, so no write stays unexecuted. Pass.

## Internal depot usage

- No new depot. `*micro-offers` is a client depot that also takes the stream gate's appends (phase 3 declared it). The `:disallow` alternative was rejected because each `source>` in a microbatch `<<sources` block is its own dataflow section (checked, microbatch.md 15), so landings on a second depot could not join phase 3's one fold. Pass.

## Cross-topology correctness

- The one flow is stream `gate` → `*micro-offers` → microbatch `micro`. Duplicates: every send is the same stored landing (F4 makes that literal); the micro gate answers a second send by name, and two in one batch by phase 3's F3. With F3 here: an envelope that fails its face checks never takes a name from one that passes. Pass after F3.

## Stream topology correctness

- The forward's `depot-partition-append!` must have `(|direct (ops/current-task-id))` immediately before it (stream.md 49, "ALWAYS"). The plan: `(|direct current)` after the crossing, then `(|hash lease-name)`, then the append. **FAIL** → F6: `(|hash lease-name) (|direct (ops/current-task-id)) (depot-partition-append! *micro-offers *landing :append-ack)`.
- `ack-return>`: called once at the end; multiple calls are last-write-wins unless `:ack-return-agg` is set, and the ack is sent only when the whole event tree completes (checked, stream.md 250-252). The plan's open question 7 is answered by the reference; the build confirms it runs as read. Pass.

## In-memory state efficiency

- No TaskGlobal. `K`, the ephemeral private key and the plaintext are locals of one event. Pass.

## Minimality — adversarial simplification

Simplest design I can construct: the request on L's home; the read-out as
its continuation opening the source and sealing the copy to a key only the
target's deciding task holds; the sealed landing appended once to the
target's depot; the target's gate lands it and destroys that key. That is
the plan. Each mechanism:

### The landing lease (key pair in a lease row)
- **Delete it**: nothing on the target's deciding task can open the landing, and a lock would have to sit in the depot or travel bare between tasks; default 1 ("Nothing that could open a value may ever sit in the depot", CONCLUSION.md R1) fails. Stays.
- **Merge or bypass**: merged already into phase 2's and 3's lease rows (two fields). Pass.

### The sealed box (X25519, KDF, AES-GCM)
- **Delete it**: the alternative built and costed in the plan is a symmetric lease fetched by a hop before the read-out: 2 more hops per promotion and a read of the other store in the promotion's path. The box costs 0.18 ms and 0.08 ms of CPU (probed on this machine, JDK 21, *checked* in the probe's text; not re-run). Stays.
- **Merge**: none cheaper constructed. Pass.

### The read-out as a continuation (PR7)
- **Delete it** (a depot record per read-out): adds a depot and a source; buys nothing the task order does not already give. The continuation is the simpler one. Pass.

### The stored forward `:forwards` (PR8)
- **Delete it**: constructed: re-send by re-opening the source on a replay (the model's `forward`). A crash after the crossing's commit and before the append, then a forget of the source, then the replay: nothing to send, the promotion crossed for ever, the copy lost; "after it does not recall" (Sid's phase 4) fails. Stays.
- **Merge into the crossing's answer record**: saves one seek on the record path, costs a promotion's bytes on phase 1's answer lookup for every offer on that answer. The plan's pick stands.

### `:landing-lock-gone` (PR5)
- **Delete it**: a landing whose lease is gone is refused on its face, nothing recorded, and a promotion stays crossed for ever: "Crossed does not promise done; a landing can still be refused" needs the refusal to be seen. Stays; widened by F3.

### `promotion-status` (query topology)
- **Delete it**: "Pending has two states" (PROGRESS.md) cannot be shown by a read. A client-side composition costs one roundtrip per step (up to 3) against one query invoke. Stays.

### PR10 (the reservation on the micro depot)
- **Delete it**: the micro gate either refuses the store's landings or takes any landing name. Stays; F1 binds it to one lease.

### The class in the landing name
- **Delete it**: every promotion into the base before its re-class reaches the micro gate and is refused on its face; default 6 fails. Stays.

## Throughput — adversarial

- The promotion's write path does 7 seeks on the owner's home; I constructed a read-out that skips the crossing name's record lookup on a fresh decision (the request's own record path already tells a replay from a fresh one): it saves 1 seek only when the request itself was fresh, but a replay after a crash between the request's commit and the crossing's commit would then decide the crossing twice (not idempotent). Rejected on correctness, not cost.
- The status read: F5 removes one seek from the crossed row. No cheaper design found beyond it: the landing's name and road need one read of the forward (T and C are not in the query's inputs). Note (not a fail under the template's seek metric): that read pulls the forward's whole value, the copy's sealed bytes included; the `:name`/`:route` pair in the crossing's answer record would avoid the bytes at the cost the plan weighed. Left to the build's numbers.

## Spec coverage — every operation and constraint

### C1. "A request act in the owner's layer"
- **Source**: Sid's phase 4, verbatim: "A request act in the owner's layer".
- **Trace**: Alice (`:alice`, home task 3 of 16), source fact `[a1 0]` = `{:e :e0 :k :note}` stamped s1. Door leases in `:group` (F1: lease act `[:group :by-entity :offer u]`, `{:count 1 :landing [:group :by-entity :landing u]}`), takes the public key P, then appends `[:alice :by-layer :offer u]` with `:promote-request {:source [a1 0] :target :group :class :by-entity :lease [[:group :by-entity :offer u] 0] :public P ...}`, stood-on `{[a1 0] s1}`, to `*offers` → task 3; decided one event; admitted with stamp r > s1 (plan step 1, lines 232-256).
- **Fault tolerance**: restart before commit: replay decides fresh (nothing visible); retry after commit: answered from the record by name; multi-partition: one task only.
- **Races**: two doors, two names, two requests; each its own promotion. Out of order: one task.
- **Flaws**: F2 (the key's check is length only); F1 (the lease name is not tied to the request's uuid). Fixed.
- **Verdict**: PASS after F1, F2.

### C2. "the read-out opens the value through its lock on the owner's task and writes the crossing fact there"
- **Source**: Sid's phase 4; PROGRESS.md "At the read-out the stream gate opens the value through its lock on the owner's partition and writes a crossing fact there."
- **Trace**: after the request's yes, `(|direct 3)`: the request commits; the continuation is a new event on task 3's queue (a forget queued before it runs first). Event: crossing name `[:alice nil :crossing u]` has no record (1 seek); `open-value>` of `[a1 0]` (4 seeks) → `{:value "hello" :stamp s1}`; K, nonces and the ephemeral pair drawn; the copy sealed under K; K boxed to P; the crossing's yes, log row, stood-on `{[a1 0] s1, [req 0] r}`, the forward, the clock, one atomic group, stamp c > r (plan lines 258-280).
- **How it is done in a Rama stream topology**: `(|direct (ops/current-task-id))` is a partitioner, so the request's writes commit and the continuation is re-queued on the same task; the read-out's own work has no partitioner inside (the lock row is local, phase 2; `$$persons` is on every task; P came in the request), so it is one event under phase 1's rule; the request's own decision event is unchanged and keeps the one-event rule. *Derived from* stream.md 39-57 and SPEC "What Rama showed" 1.
- **Fault tolerance**: crash between the request's commit and the read-out's commit: the record replays from its start; the request answers from its record; the read-out decides fresh, after whatever the task ordered meanwhile (a forget → `:source-erased`). The first attempt's K and box were never committed, so nothing of them exists. Retried after the crossing's commit: answered from its record. Multi-partition: one task.
- **Races**: a forget on task 3 is ordered against the read-out by task 3's thread: one side or the other of the line, never both. A re-class of `:alice` between the two events: **flaw** → F8.
- **Flaws**: F2 (`box` nil unhandled), F8. Fixed.
- **Verdict**: PASS after F2, F8.

### C3. "a landing offer named from the request under a scheme reserved to the store"
- **Source**: Sid's phase 4; the names sharpening: "a promotion's landing is named from the request's name under a scheme reserved to the store. A name carries the layer and class it was made for and reaches one gate only."
- **Trace**: `[:group :by-entity :landing u]`, u the request's; the class carried (plan Names, 362-370). The stream gate refuses `:landing` and `:crossing` names from `*offers` on their face; the store's own steps there never pass a depot. The micro depot cannot tell a topology's append from a door's (plan 373-386).
- **Forged landing, how far it gets (asked)**: (a) *Alice's own door*, knowing u and P: seals her own K and content to P, appends `[:group :by-entity :landing u]` citing her landing lease. Passes every micro check (one value fact, lease `:for` this name, her session, her permission in `:group`). Reach: content she could have written directly, with a false provenance (`:because-of` her request, standing on a crossing that may not have happened or opened other content); if it wins the name, the real landing is refused by digest and the status reads done. If her read-out was a no, the status reads refused (step 2 decides before the landing is looked at). Bounded to her own promotion and her own permission: the plan's open question 1, "no signing; a later edition can add the part". (b) *Mallory, another writer in `:group`*: reads Alice's lease act in the group (a recorded act; its control value `{:count 1 :landing L*}` is plaintext, *derived*: phase 1 logs every act's facts), makes her own lease `:for L*`, appends a landing named L* under her own session. Before F1 it lands, and Alice's real landing is refused by digest: Mallory captures Alice's promotion. After F1: a lease `:for L*` must be named `[:group :by-entity :offer u]`, a name Alice's lease act already holds, so Mallory's lease is a reused name (refused by digest); a landing named L* must cite exactly `[[:group :by-entity :offer u] 0]`; citing it under Mallory's session finds a row leased to another session, refused on its face and left alone (F3); only a door that claims Alice's session gets further, which breaks every act, not promotion only.
- **Fault tolerance**: the name is fixed before the first gate; every send is the same.
- **Races**: a forged and a real landing under L* in one batch: **flaw** → F3 (the passing one takes the name).
- **Flaws**: F1, F3. Fixed.
- **Verdict**: PASS after F1, F3; the self-forgery (a) stays open for Sid.

### C4. "pending with two states"
- **Source**: Sid's phase 4; PROGRESS.md "Pending has two states: not yet read out, still forgettable; read out and not landed, crossed."
- **Trace**: T3's history: status as of r is pending (no crossing at or before r), as of c crossed, as of the landing's stamp in a batch ≤ F done; the model's precedence done, refused, crossed, pending (model.clj 996). Plan step 5, lines 322-336; query 543-566.
- **Fault tolerance**: the status is read from the records; nothing to recover.
- **Races**: a landing decided in a batch above the frontier reads crossed on every task until F passes it (R5).
- **Flaws**: F5 (an extra read; a request after `*as-of`). Fixed.
- **Verdict**: PASS after F5.

### C5. "a forget before the read-out refuses"
- **Source**: Sid's phase 4; PROGRESS.md "A forget ordered before the read-out refuses the promotion".
- **Trace, value forget (B case 1)**: hold at `:before-read-out`; the forget deletes the source's lock row on task 3; release and resend: the read-out's `open-value>` gives `{:erased-at d}` → recorded no `:source-erased`; shown `[:pending :pending :refused]` (*checked* against scenarios.clj 64). No box, no forward, no append (T11 checks the depots).
- **Trace, person forget on the owner's home (B case 2)**: phase 2's fan-out destroys Alice's person lock on every task; on task 3 it is one event ordered against the read-out by task 3's thread; in a one-owner layer the owner's lock is required, so the source's wrap is closed and the read-out records `:source-erased`; shown `[:pending :refused]` (*checked*, scenarios.clj 68). The fan-out's order on other tasks never reaches the read-out, which reads only task 3. The rig's answer to PROGRESS.md's open "the lock store's order of a person's forget against a read-out".
- **Fault tolerance**: a restart after the forget and before the read-out: the replayed read-out sees the forget. Retried read-out after its no: answered from its record.
- **Races**: the forget and the read-out on one task: ordered, never concurrent.
- **Flaws**: none found, with reasoning: both forgets are decided on the task the read-out reads, and the read-out's refusal is recorded.
- **Verdict**: PASS.

### C6. "after it does not recall"
- **Source**: Sid's phase 4; PROGRESS.md "after it the copy is not recalled, because the read-out is a read that happened."
- **Trace (B cases 3 and 4)**: after the crossing's commit, K exists only in two boxes (the forward in `$$layers`, the landing in `*micro-offers`), both opened only by the landing lease's private key, which is bare (no person lock). Alice's value forget deletes the source's lock row; her person forget destroys her person lock; neither reaches K. The landing is decided as it would have been: shown `[:crossed :crossed :done :done]` and `[:crossed :done :done]` (*checked*, scenarios.clj 73, 77). PR13 keeps `:who` Alice from refusing it; F9 adds that the fan-out must not delete her bare lease row or close her session in T (phase 2 cuts her wrapped rows, *checked* 2062-2065).
- **Fault tolerance**: a crash between the crossing's commit and the append, then the forget, then the replay: the stored forward is re-sent (T7 b). The model's `forward` would send nothing then (it re-sends "while the source can still be opened", *checked*, model.clj 624-627); in the model the first send cannot be lost, so the outcomes agree; phase 8 reports the difference.
- **Races**: none across stores: the landing's gate never looks at the source (`:landing-checks-source false`).
- **Flaws**: F9. Fixed.
- **Verdict**: PASS after F9.

### C7. The sealed box: what could open the copy, at every moment
- **Source**: default 1 (RIG.md); CONCLUSION.md P6 widened: "nothing retained opens or confirms a forgotten value. The depot, the lease rows or holder, and the lock store become separate places the model checks for retained data."
- **Trace, moment by moment** (derived):
  - lease decided → request decided: the lease row holds the private key bare; nothing is sealed to it; the request's depot record holds ids, a layer, a permission id, a public key and subject ids (plaintext; For Sid 5's class of leak, not an opener).
  - crossing committed → forward appended: openers of the copy = the lease row's private key only (the box needs it; K, the ephemeral private key and the plaintext were event locals). A source forget does not reach it, by the ruling (C6).
  - landing decided, yes: the lease row deleted in the deciding group; K re-wrapped as the copy's record lock under the target's wrap. The forward's and the depot's bytes then open only with the copy's record lock, and follow it.
  - landing decided, refused (any recorded reason): the lease row deleted; the forward's and the depot's bytes open under nothing the store holds.
  - landing face-refused and not recorded: before F3, the row stays until its session closes, and the promotion stays crossed. After F3, only a malformed envelope, a lease id its name does not bind, or a row leased to another session are face refusals, and none of them is the store's own forward from an honest door.
  - a person forget of someone the copy is about, between the read-out and the landing: the bare row still opens a copy about a forgotten person until the landing's decision refuses it (`:person-forgotten`) and deletes the row. **Unwalked** → F7. The same window phase 2 leaves for every in-flight sealed offer (its lease rows are wrapped under the writer, not the subjects).
- **Construction** (the probe, *checked* by reading the code and its output, not re-run): X25519 by the JDK (`XDH`), an ephemeral key per box, the wrapping lock HMAC-SHA256 keyed by the raw shared secret over the label `softland/landing-box/v1` ‖ ephemeral public ‖ lease public (a one-block HKDF-style derivation; acceptable for PR6, which can change without touching a record), AES-GCM with a fresh 12-byte nonce over the 32-byte K, **no associated data**. The probe shows: opens with the lease's private key, not with another; 44 + 12 + 48 bytes; 0.09/0.18/0.08 ms. It does not show: a non-key or small-order public key; what is bound to the landing name (nothing in the box; only the lease row's `:for`). **Probed here** (runs/phase4-validate-x25519.txt): 44 random bytes fail to decode (`InvalidKeySpecException`); a well-formed 44-byte key with u = 0 or u = 1 *decodes*, and the agreement then throws `InvalidKeyException: Point has small order`; a real key agrees. The plan's probe `box` has no `try` (its `unbox` has), so the plan's "44 bytes" check admits a request whose read-out would throw in topology code: fatal to the worker, and replayed from the record's start, a poison record. A decode check at the request alone would not catch the small-order points; the read-out needs its own nil-box branch. → F2: the request's check decodes the key; the read-out records a no if `box` still returns nil; GCM's associated data binds the lock id and the landing name.
- **Verdict**: PASS after F2, F3, F7.

### C8. "a retried request landing once"
- **Source**: Sid's phase 4 tests: "a retried request landing once".
- **Trace (T6)**: five resends of `[:alice :by-layer :offer u]`: two while held (the request from its record; the read-out once released), one between crossing and landing (crossing from its record, the stored forward re-sent, a second record under L* in `*micro-offers`, answered by name or collapsed in one batch by phase 3's F3), two after the landing (answered from the record; the lease already consumed, F2 of phase 3's validation: the recorded answer returns). One crossing, one forward, one landing answer, one copy.
- **Fault tolerance**: covered in Production readiness.
- **Races**: a forged same-name envelope in the same batch → F3.
- **Flaws**: F3. Fixed.
- **Verdict**: PASS after F3.

### C9. "a failover between forward and landing"
- **Source**: Sid's phase 4 tests.
- **Trace (T7)**: (a) a throw after the append: the worker restarts, the record replays, the forward is re-sent (F4: the same stored envelope), the micro gate decides once; (b) a throw after the crossing's commit and before the append, a forget of the source before the replay: the stored forward is re-sent and lands; (c) the landing's batch fails once: retried whole. For the base on the stream gate: a throw on T's home during the landing event replays the record; the landing answers from its record if it committed.
- **Flaws**: F4 (the "same bytes" claim) and F6 (commit boundary before the append). Fixed.
- **Verdict**: PASS after F4, F6.

### C10. The landing: decided like any sealed act; the lease row deleted whatever the decision
- **Source**: default 1, builder A: "a lease row is consumed at decision whatever the decision"; phase 2's V-F3 (one call site).
- **Trace**: the landing arrives on hash(lease-name) (the micro build's `route-key`, *checked*); block 1 gathers the lease row, unboxes K, opens the copy, computes the copy's subjects from T's rules (the group's none, the key's grammar, the carried `:subjects`: the ruling "The copy is about whoever the target's grammar and tool name, not its former owner"), re-wraps K into the record, digests under K; `consume-locks>` in the deciding group. A missing row → `:landing-lock-gone`, recorded (PR5).
- **Flaws**: F3 (which refusals are recorded). Fixed.
- **Verdict**: PASS after F3.

### C11. A requester's session closing before the landing
- **Source**: default 1, builder A: "unconsumed lease rows go when their session closes".
- **Trace**: Alice's session in `:group` closes after the crossing: the landing lease row is deleted (phase 3's F4); the landing arrives: no row at its bound id → `:landing-lock-gone`, recorded; status crossed, then refused; the forward's and depot's bytes open under nothing. PR9's door rule keeps an honest door's session open; a crashed door's (open item 82) may not: open question 2.
- **Verdict**: PASS (open for Sid).

### C12. The target re-classed between the request and the landing
- **Source**: ruling 2's sharpening: "re-classing a one-owner layer to by-entity moves it to the microbatch gate"; the names sharpening: "reaches one gate only".
- **Trace (T10 c)**: the request carries `:by-layer` for the base; the base re-classes; the landing `[base :by-layer :landing u]` hops to the base's home on the stream gate, which refuses `:class-mismatch` (recorded) and consumes the lease (phase 2: whatever the decision). Status refused. The consequence is named for Sid (open question 5).
- **Verdict**: PASS.

### C13. The envelope's `:box` and the landing name's class
- **Source**: PROGRESS.md "named parts the store owns (...), a version marker, and the gate refusing parts it does not know. Tools add facts, never envelope parts."
- **Trace**: `:box` is a fact-level part the store owns, like phase 2's `:sealed` and `:lock-id`; no tool can add it; the micro gate accepts it on a `:landing` name only and refuses it elsewhere as an unknown part. It extends the store's list of named parts, so it is first-record (PR3 says so) and for Sid; the rig's records are not kept, so it joins version 1 rather than bumping the marker (derived; stated in PR3 by F4's edit). The class in the landing name is the names sharpening's "carries the layer and class it was made for".
- **Verdict**: PASS (first-record, for Sid).

### C14. Sid's rule on order between the two stores
- **Source**: PROGRESS.md "order between the two stores exists only through stood-on."
- **Trace**: the landing stands on the source and the crossing with their stamps; the micro gate takes the largest carried stamp plus one (phase 3's M6); no check against the other store anywhere (the read-out does not look at T, the landing does not look at L).
- **Verdict**: PASS.

### C15. Defaults 2, 5, 6
- **Default 2** (hybrid clock): every stamp from `rig.store.clock/next-stamp` over the stood-on. PASS.
- **Default 5** (permissions): the landing's permission is in T, checked by T's gate; the read-out is exempt (model.clj 76, `exempt?`), so a revoke in L between the request and the read-out does not stop it (PR12); a revoke in T before the landing refuses it (T9 a). PASS.
- **Default 6** (the base): one-owner on the stream gate until its re-class, lock rules shared from day one; T10 a to c. PASS.

### C16. "What Rama showed" 3: a gate never throws on an offer
- **Trace**: the plan says the box functions are total, but its probe's `box` is not (no `try`), and the read-out had no branch for a nil box. Probed: a small-order key passes a length check and a decode check, then the agreement throws (runs/phase4-validate-x25519.txt). Unfixed, the read-out throws, the worker dies, the record replays and throws again. **FAIL** → F2 (decode at the request, `box` total, a recorded no when it is nil).
- **Verdict**: PASS after F2.

## Self-consistency

Every check above that found a gap names its fix; no PASS stands on a gap
left open. The self-forgery of C3 (a) is not a gap in the plan's rules but
the ruling's deferred signing, and is listed for Sid, not passed as solved.
The F7 window is phase 2's accepted window applied here, named, not solved.

## For Sid (from this validation)

1. **First-record picks** this plan adds, with F1's change: PR1 the request
   (control fact `:promote-request`, now citing its lease by the request's
   own uuid); PR2 the crossing; PR3 the landing and the envelope's new
   store-owned fact part `:box`; PR4 the landing lease, **named from the
   request's uuid** `[T C :offer u]` (F1); PR5 `:landing-lock-gone`.
2. **A door forging its own landing** (open question 1) stays: bounded to
   its own promotion and permission, a false provenance. Signing closes it.
3. **The window F7 names**: between a read-out and its landing, the store
   holds a bare opener of a copy about someone who may be forgotten
   meanwhile, until the landing is decided. The same as any sealed offer in
   flight; inside "gone for everyone including the past"?
4. **A request's control value names the copy's subjects in plain text**
   (the For Sid 5 class), and a copy about someone forgotten is refused at
   the landing (open item "writes about someone already forgotten").
</content>
</invoke>

## Probes and commits

- runs/Phase4ValidateX25519.java → runs/phase4-validate-x25519.txt, no
  cluster, JDK 21.0.12.1: decode and agreement for a non-key, two
  small-order points, a non-canonical u and a real key (F2's evidence).
- Commits on rig-plan-promotion: 24c6aaf7 (this file, first version);
  2942e888 (the plan, F1 to F9 applied and marked `[F<n>]`); the commit
  after it (this file with the probe's evidence, and the probe).
