# Implementation Validation — stage 4, "promotion"

<!-- The rama skill's build step 2 (phase-4-impl-validate.md, template
artifact-impl-validation.md), 26 September 2026, 06:10 IST, by the build
session (Claude Opus 5.5, effort max), branch rig-build-promotion.
Default verdict major-fail; each check below was code-traced. Reviewed:
rig.store.box, promote-shape, promote, promote-flow, promote-client,
gate-event, and the stage's lines in envelope, gate, locks, micro,
inject and module (commits 1b1d416c to f5e38f6b). Marks: *traced* =
followed through the code named; *run* = seen on a cluster (the smoke
run, runs/promotion-smoke.log); *assumed* = neither. -->

**Verdict: minor-fail, fixed in place.** Three findings, each a
localized edit, made and committed before this artifact was closed:
V-1 (plan conformance: the crossing fact's entity), V-2 (spec
conformance: the read-out's class, where the plan's F8 contradicts the
model) and V-3 (minimization: the status read carried sealed bytes).
After them every check below passes by trace. One divergence from the
plan stands with its reason (V-2), and is for Sid and the orchestrator.

## Findings and fixes

| # | Finding | Fix | Commit |
|---|---|---|---|
| V-1 | The build put the crossing fact on the request fact's entity and refused `:malformed-control` when it was not the source's, so that a replay could rebuild the crossing's digest without reading the source row. The plan puts it on the source's entity, and the extra refusal is not the plan's. | The crossing is built on the read-out's fresh path from the source's row (promote.clj:57); its record answers by name with no digest (`recorded-ack`, promote.clj:89): a crossing's name is under the store's reserved scheme, which no depot record may carry (envelope.clj parse), so no other writer can have used it. | 1a3a11e8 |
| V-2 | The plan's F8 refuses a read-out `:class-mismatch` when the source layer was re-classed after the request. The model does not: `refusal` exempts a read-out from the class check, "a read-out is the store's own step, placed where the source's lock is; it claims no class" (model.clj 411-419), and IMPLICIT_SPEC OP7 says a promotion pending at a re-class is read out where the source's lock is, in the stream store. P16 keeps a re-classed layer's store-placed acts with the stream gate. SPEC.md makes the model the gates' executable spec. | The crossing carries the class in force on its act (nil in its name), so `gate/refusal`'s class check never refuses it, and the read-out opens the source whatever the layer's class (promote_flow.clj:74-82). PR2's recorded refusals are the model's two plus F2's `:malformed-control`. | 3f9a928e |
| V-3 | `status-home>` returned the whole stored forward, so the sealed copy and its box travelled with the query to the landing's task and back. They open nothing without the landing lease's private key, but the status needs none of it. | `forward-summary` (promote.clj:341): the landing's road, name and target only. | f5e38f6b |

## Redundant conditionals

- `read-out>` (promote_flow.clj:45): its `<<if` branches do different
  things (the record path returns the recorded answer and forward; the
  fresh path decides). The inner `(<<if (= :yes (get *srec :answer))
  (local-select> row) (else>) (identity nil))` differs by operation. PASS.
- `forward>` (108): micro road (hash, commit boundary, append) against
  stream road (hop, decide). PASS.
- `continue>` (137): continuation against pass-through. PASS.
- `status-home>` (163): each branch emits a different map (the reads made
  differ). PASS.
- The query (204): the micro branch reads the name row and frontier; the
  stream branch the target's answer. PASS.

## Consecutive keypath

Every navigation is one `keypath` with all its keys: `(keypath *layer
:answers *cname)`, `(keypath *layer :log *sname *sidx)`, `(keypath
*layer :forwards *req)`, `(keypath *lname :answer)`. PASS.

## Select-compute-transform

The one write this stage adds outside the gate's shared ops is the
forward (promote_flow.clj:98): a `termval` of a value computed from the
event's decision, with no read of the key it writes. No select-then-
transform of the same location. PASS.

## Unnecessary nil->val

None used. PASS.

## :allow-yield?

Every read this stage adds is a point read (an answer, a row, a forward,
a name row, a frontier). The lease queries' range reads over one
session's rows (`lease-locks>`, `micro-lease`) are phase 2's and 3's,
unchanged, and already yield. PASS.

## Non-subindexed collections without size limits

- `:forwards` (promote.clj:233) is subindexed.
- Inside a forward: `:facts` holds exactly one fact (`landing-offer`
  builds one; `landing-face` refuses any other count at the micro gate,
  promote_shape.clj:160); `:stood-on` exactly two entries; `:subjects` at
  most 256 (`request-value-ok?`, promote_shape.clj:71, checked before the
  request is admitted). All bounded. PASS.
- Lease rows gain two scalar fields. PASS.

## Stream topology idempotency

Traced for every write and side effect of the continuation, on a retry
at each point (`:retry-mode :all-after` replays the request's record from
its start):
- The request: decided once; a replay answers from its record
  (gate_event.clj:93, unchanged path).
- The crossing (read-out>, promote_flow.clj:45): its record is looked up
  first by its derived name; a replay after its commit answers from it
  and re-reads the stored forward. Its writes are `termval`s by name
  (answer, log, stood-on, heads, index entries, by-stamp, clock) through
  `write-decided>` and the forward's `termval` by request name: the same
  keys on any attempt. PASS.
- Randomness in the topology (`fresh-read-out`, promote.clj:97): K,
  nonces, the ephemeral pair. A retry before the crossing's commit draws
  again, and nothing of the first attempt was visible or sent (the send
  follows the commit, promote_flow.clj:155). No id is generated: the
  crossing's and landing's names derive from the request's. PASS.
- The landing lease's key pair: on the stream gate drawn in
  `decision-reads>` before the decision (locks `fresh-for`), keyed by the
  lease's name; on the micro gate drawn in block 2a, where "only the
  committed attempt's exist". The door takes the public key only from the
  committed row. PASS.
- The internal append (promote_flow.clj:121-123): `(|hash *route)`, then
  `(|direct (ops/current-task-id))` immediately before
  `depot-partition-append! ... :append-ack` ([F6]). On a retry after the
  append, a second record goes to `*micro-offers`; the micro gate answers
  it by name: a record from an earlier batch takes the record path (name
  step, then the resend check opens it with the recorded lock and finds
  the same digest); two in one batch with one fingerprint collapse in
  `envelopes`; two with different fingerprints (the first send in memory,
  the replay read back from the PState, if they ever printed apart) give
  the second the face `:name-taken`, harmless since nobody waits on it.
  Traced in micro.clj's fold; T7 a runs it. PASS.
- The stream landing (promote_flow.clj:125-133): decided on T's home by
  `record-or-decide>`, whose record path answers a replay (phase 2's
  resend check, R = O since the replay cites the recorded lock id). PASS.

## Partial failure in stream topologies

The continuation writes on L's home (the request, then the crossing and
its forward, each group committed at a `|direct` boundary), then either
appends on hash(lease-name) or writes on T's home. A failure after any
committed group replays the record; every step before it answers from
its record, and the stored forward is sent again whatever happened to
the source meanwhile (PR8). No write can stay unexecuted while the
record's tree is incomplete, and the tree completes only after the send.
The one case left to the door is a hold (`inject/held?`), a test device
that stops the continuation until the door's resend. PASS.

## Single depot append per client operation

`promote!` (promote_client.clj:120) makes two acts, each one append and
each answered by name: the landing lease in T, then the request in L.
They are two operations of the lease road (default 1: a lease act before
a write), not one operation split in two. The request's gate cannot see
the lease (another layer, often another store), so the order is the
door's rule, "no request without a landing lease": `promote!` sends no
request when the lease gave no public key (promote_client.clj:137). A
door crash between the two leaves an unused
landing lease, a bare key pair row that sealed nothing, reclaimed when
its session closes, the state phase 2 accepts for every unconsumed lease.
No inconsistent state. PASS.

## Application-state caches survive restart

No TaskGlobal and no cache. K, the ephemeral private key and the
plaintext are locals of one event; the landing's opener waits in a
PState row (the landing lease), and the landing itself in `:forwards`.
The `inject` atoms are test hooks. PASS.

## No reimplementation of built-in operations

The box uses the JDK's X25519, HMAC-SHA256 and AES-GCM; Rama has no
equivalent. Partitioners, appends and paths are Rama's. PASS.

## Plan conformance

Every divergence from PLAN-promotion.md found, with the template's test:
a divergence passes only when the plan was wrong (correctness) or much
slower.

| # | Divergence | Why | Verdict |
|---|---|---|---|
| D1 | The crossing is written, and a stream-gate landing decided, by `rig.store.gate-event`'s ops (the gate's path moved there, the orchestrator's decision (a)). | The plan's own words: the landing decided "through the gate's ordinary decision path"; the crossing's writes "one atomic group". One path, not a copy. | conforms |
| D2 | The request's ack is the request's answer plus `:crossing` (and `:landing` for a stream target), not `{:request .. :crossing ..}`. | Correctness of the door's contract: `client/offer-until-answered!` returns the ack, or after a send error a looked-up answer `{:answer :reason :stamp :name}`. With the plan's shape one call would return two shapes depending on whether an error happened, and a caller (phase 8's replays among them) would misread one. | PASS |
| D3 | The lease queries give a landing row's `{:public :for}` under `:landings`, beside `:locks` (locks.clj `leased-locks`, micro.clj `lease-result`). | Correctness: the stream door pools every `:locks` entry as a sealing lock (client.clj `refresh!`), so a landing row there would be handed to the door's next act as a lock. | PASS |
| D4 | The read-out reads the source's answer and row and calls `locks/open-row>`, not `open-value>`. | Correctness: `open-value>` answers `{:value ..}` for a control fact and a retract, which the read-out must refuse `:source-has-no-value`, and it does not hand back the row whose e and k the landing needs. Cost: 8 seeks for a fresh read-out, where the plan counted 7; the eighth is phase 2's erasure-ledger read inside its open step, which the plan's count of `open-value>` left out. | PASS |
| D5 | The read-out claims no class (V-2), where the plan's F8 recorded `:class-mismatch`. | The plan contradicts the spec: the model's `refusal` exempts the read-out, IMPLICIT_SPEC OP7 reads it out where the lock is, P16 keeps store-placed acts of a re-classed layer with the stream gate. | PASS (plan wrong) |
| D6 | A stream-gate landing whose bound row is missing under the landing's session is recorded `:landing-lock-gone`, also when a row exists under another session (the plan's F3 makes that case a face refusal). | On the stream gate lease rows are keyed by session, so a row under another session cannot be seen without scanning every session of T; and F3's reason for the face refusal, a forger citing someone's lease, cannot arise here, since the depot refuses the `:landing` scheme and only the store's hop brings landings. For a door that broke the one-session rule the promotion ends refused instead of staying crossed. | PASS |
| D7 | The request's check also requires a session on the act, the source among its stood-on, and a source index a row vector can hold. | The plan's step 1 has the request "standing on the source with its stamp carried" and the landing in the request's session; these make its premises checked, so the crossing and the landing can stand on the carried stamp and a sessionless request cannot cross and then never land. | PASS |
| D8 | `promote!` sends no request when the landing lease gave no public key; `status-of` returns the keyword, `promotion-status` the map. | The plan's door rule; phase 8's table asks for the keyword. | PASS |
| D9 | `gate/intake-offer` split out of `gate/intake`, unchanged in behavior. | The stream landing's intake is the depot record's, so a later stage's change to intake reaches both. | PASS |
| D10 | A `:before-read-out` crash point beside the plan's `:before-forward` and `:after-forward`; the hold keyed by point and name. | Test devices (R3, PR15). | PASS |

## Spec coverage: SPEC.md phase 4 and the sharpening, traced

- **"A request act in the owner's layer"** (PR1). A door's act
  `[:alice :by-layer :offer u]` with one `:promote-request` fact reaches
  L's home (`*offers`, hash-by :layer). `gate/control-fact?` now names
  it; `control-value-ok?` calls `request-value-ok?`
  (promote_shape.clj:71): keys, source in L and stood on, target ≠ L,
  class, lease `[[T C :offer u] 0]` with u the act's own ([F1]), public
  key decoded as X25519 ([F2]), permission for T, replaces in T, ≤ 256
  subjects, a session. `control-allowed?` lets anyone whose permission
  covers L request. A second fact beside it is `:malformed-control`
  (gate.clj, the one-fact rule). Decided by `record-or-decide>`, answered
  by the ack. *Traced; run (smoke: yes).*
- **"the read-out opens the value through its lock on the owner's task
  and writes the crossing fact there"** (PR2). `continue>`
  (promote_flow.clj:137) after the answer: a commit boundary, then
  `read-out>` on the same task: the crossing name's record; else the
  source's answer and row and `locks/open-row>` (the lock row, the
  ledger, the wrap's persons, all local); the draw; `read-out-verdict`
  (promote.clj:112): `:source-erased`, `:source-has-no-value`,
  `:malformed-control` when no box can be made ([F2]), else the copy
  sealed under a fresh K and K boxed; `gate/decide` over the crossing
  with the verdict's reason as the lock context's value reason, then
  `write-decided>` and the forward's `termval`, one group. *Traced; run.*
- **"a landing offer named from the request under a scheme reserved to
  the store"** (PR3, PR10). `env/landing-name req T C` = `[T C :landing
  u]`. The stream parse refuses `:landing` and `:crossing` names from
  `*offers`; the micro parse takes `:landing` only as a landing: its box
  only there (`:unknown-part` elsewhere), exactly one sealed fact with a
  well-formed box, citing exactly `[[T C :offer u] 0]`
  (`landing-face`, promote_shape.clj:160), else a face refusal; an
  `:offer` citing a landing row gets no lock (`unlease` answers nil for
  it) and is `:no-such-lock`. *Traced.*
- **"pending with two states"**. `promote/status` (promote.clj:253):
  `:pending` until a crossing answer at or before T, `:crossed` after a
  yes, `:done` or `:refused` only for a settled landing (a micro answer in
  a batch ≤ the frontier of its name row's task, R5), `:none` for a
  request admitted after T ([F5]). The query reads one task, or two only
  when crossed, and the frontier only when a landing answer is there.
  *Traced; run (crossed at return, done after the batch).*
- **"a forget before the read-out refuses"** (B1, B2). A value forget of
  the source deletes its lock row on L's home (a personal or hand value)
  or excises it (an agent layer's record lock), writing the ledger; a
  person forget of the owner destroys her person lock on every task, and
  in a one-owner layer the owner is always required. Either is decided
  on L's home, ordered by its one thread against the read-out, which then
  gets `{:erased-at d}` from `open-row>`: `:source-erased`, recorded, no
  box made, nothing forwarded. *Traced.*
- **"after it does not recall"** (B3, B4). After the crossing's commit K
  exists only in two boxes (the forward in `:forwards`, and the landing
  once appended), opened only by the landing lease's private key. The
  landing row is bare (`landing-row`, `:under nil`) whoever leased it: a
  person forget of the leaser writes `$$persons` and purges dying values
  (locks `fan-out>`), touching no lease row, closing no session and
  revoking no permission ([F9]). The landing's `:who` is never checked
  against `$$persons` on either gate: the micro gate checks the subject
  union (`persons-to-check`), the stream gate the wrap persons, a lease's
  writer, a maker and a forget target (PR13). So the landing is decided
  as it would have been. *Traced.*
- **"a retried request landing once"**. Every resend of the request is
  answered from its record, re-enters `continue>`, finds the crossing's
  record by name and re-sends the stored forward; the target's gate
  decides the first and answers the rest by name (above, idempotency).
  *Traced.*
- **"a failover between forward and landing"**. A crash after the
  crossing's commit and before the append (`:before-forward`): the replay
  re-sends the stored forward, whatever happened to the source. After the
  append (`:after-forward`): a second record, answered by name. In the
  landing's batch: the batch retries whole, deciding once and consuming
  the lease once (phase 3). *Traced.*
- **"The copy is about whoever the target's grammar and tool name, not
  its former owner"**. The landing carries the request's `:subjects`,
  never the source act's subject slot; T's gate computes the copy's
  subjects from its owner (none in a group; the base's root actor is no
  person), the key's grammar over the copy, and the carried subjects, and
  wraps K under them into the record (group layers and the base keep
  record locks, ruling 7). *Traced.*
- **"Crossed does not promise done; a landing can still be refused"**.
  Every value-level refusal at T is recorded, and the lease row consumed
  with it: a revoked permission, a stale replace, a forgotten subject
  ([F7] closes its window at the landing's decision), a lock that did not
  come out of its box (`:landing-lock-gone`, PR5). The status then reads
  refused. *Traced.*
- **"Say all of this at the point of promotion"**. `promote!` returns the
  crossing's answer and the status with its statement; the query gives
  the statement for the state it found (`promote/statement`). *Run.*
- **Sid's rule on order between the stores.** Nothing reads the other
  store: the read-out does not look at T; the landing's gate does not
  look at the source or the crossing. The landing stands on both with
  their stamps, so its stamp is after the crossing's (the micro gate's
  largest carried stamp plus one). *Traced.*
- **Default 1** (nothing that could open a value sits in the depot). The
  request's record holds ids, a layer, a permission, a public key and
  subject ids. The landing's record holds the copy under K and K in a box
  that only the landing lease's private key opens; the private key sits
  in a PState row, consumed at the landing's decision whatever it is, or
  at its session's close. After a copy's forget its bytes follow its
  record lock. *Traced; T8, T11 check it.*
- **"A gate never throws; every refusal is data"**. The box is total
  (box.clj, every function catches); `request-value-ok?`, `landing-face`,
  `open-landing`, `read-out-verdict` catch; `gate/decide` is total; a
  failed draw is the unrecorded `:gate-error`, never a throw
  (promote_flow.clj, `fresh-ok?`). *Traced.*

## Self-consistency

No check above passes over a gap it names. V-1 to V-3 were fixed before
the verdict. D5 and D6 are divergences with reasons, not gaps: D5 builds
the spec where the plan contradicts it, D6 builds what the stream gate
can decide where the plan's rule needs a scan. Both are listed for the
orchestrator and Sid.

## Verdict

**minor-fail**: three findings (V-1 to V-3), each fixed by editing named
lines of the stage's own namespaces, committed; no restructuring was
needed.

PHASE_VALIDATION:minor-fail
