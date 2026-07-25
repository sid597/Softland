# CONTRACT_P6 — the truth loop whole (Gates 3 + 4)

Fable-authored 2026-07-25 (the CAMPAIGN §P6 mandate: the owner-condensation
is adjudicated HERE; PROMPTS.md §P6 is the implementation half only and is
subordinate to this contract). Binding with DIRECTION.md + CAMPAIGN.md +
the first-light CONTRACT §7 (P4/P5 — this package fulfills them, no fork).
Precedence: decisions.md and this contract over every derived artifact.

## Purpose

Close the Truth loop and the Perception loop on real material: deviation →
candidate → preview → scoped activation → announced change → reversal —
with every step recorded, reversible, and standable in history. Gate 3
(metabolism) and Gate 4 (immunity) close here.

Consumers, in order: Sid's return wear (the first full breath) · the P7
portal (reads every projection this package serves) · P8's verb-from-inside
(activates through this package's machinery) · the first-light CONTRACT
(P4 preview-membrane + P5 accept/reject/reverse/explain are fulfilled by
these deliverables).

## R1 — Owner condensation: REFUSED. No new Rama module. (adjudicated)

The platform-check ran against OC and the relation kernel (sources: the
input manifest below; P1–P5 as the proof-of-use record):

- **OC already owns every needed convention**, proven across P1/P3/P5: one
  spec-generic adapter (`object-container.facet-master`) gives immutable
  revisions, idempotent imports, candidate-vs-active (latest ≠ active), an
  explicit revisioned active-pointer whose revision chain IS the causal
  activation history, grammar validation on both sides, the malformed
  drill, and the single batched serve. Nothing in P6's truth list needs a
  storage semantics OC lacks.
- **The relation kernel is REFUSED as a policy store**: attachments, pins,
  and deviations are mechanism (masters), not epistemic claims; storing
  them as relation edges would fuse the strata DIRECTION forbids fusing
  (masters ≠ kinds; epistemic status never fuses with meaning). RK stays
  what it is: kinds/edges over the record.
- **Every candidate "homeless truth" resolves to an existing owner** (R2
  below): instance deviations, pins, and durable instance-binding rows ride
  instance-scoped facet-masters (the same adapter); wear/blast-radius
  indexes are DERIVED (plus one rebuildable registry hint row — an index,
  never a truth owner); activation scope/actor/grounds ride the activation
  pointer's own source form; announcements, case reports, and standable
  history are projections of the event trail. The §Horizon "material kernel
  module" stays un-condensed; the horizon line stays open.
- **Reversal cost** if this ruling is wrong in practice (serve cost at N
  instance masters, import latency inside the preview membrane, an OC
  validation limit): STOP CLAUSE — record in NOW + decisions.md Open
  Questions and END. The implementer never mints a module, PState, depot,
  or topology under this contract.

## R2 — Instance-level truth: instance-scoped facet-masters (adjudicated)

P5 made the instance tier legal with no durable owner. It lands as
follows:

- **At the FIRST durable deviation of (facet, subject)**, mint an
  instance-scoped master through the SAME adapter, spec synthesized from
  the parent facet's spec: distinct master-id (shape
  `fm:<facet>:i:<sha8(subject-uid)>`, full subject uid carried INSIDE the
  form — ids stay short, identity stays exact; instance-master ids are
  subject-scoped, one per (facet, subject), and never enter
  `facet-masters/specs`), same grammars, same floor. Everything is
  inherited, not rebuilt: revisions, candidates, preview, activation
  events, rollback, drill totality, latest ≠ active. This is the
  second-wearer law applied to our own machinery — additive reuse, no
  parallel serve artery, no compat adapter.
- **Resolution law (kernel, pure)**: wear(subject, facet) = valid active
  instance revision → else shared active → floor (always present). This is
  P5's tier order made durable; `resolve-binding` already consumes the
  instance tier — P6 feeds `:instance-rows` from served instance masters
  instead of only the console atom. The console seam
  (`__bindings.instance`) stays as the ephemeral experiment lane and
  additionally REFUSES `:space/ground` (P5 gate finding 1 — the space has
  no instance tier, ever, until space-as-outermost-entity is built).
- **Pins ride the same lane**: a pin is an instance-master form whose
  material carries `:facet-master/pin {:pinned-revision-id …}` (grammar
  key, validated; pinned revision resolved under ITS OWN grammar,
  malformed → floor). Pinning and unpinning are activations of the
  instance master — recorded, reversible, plural. No serve-time special
  case in code.
- **Deviation visibility**: a deviation is diffable vs inherited — the
  serve carries, per instance master, the parent master-id + the revision
  the subject would wear if the deviation were gone; the diff is a
  projection, never stored.
- **The registry index**: one upsert hint row per instance master on the
  world container ({facet → {subject → instance-master-id}}), REBUILDABLE
  from container existence (T7 asserts this) — it exists so the serve and
  blast-radius never enumerate blind. Settled geometry cells are
  GRANDFATHERED as-is (positions are settle-state truth, P3's ruling);
  they do not migrate into instance masters.

## R3 — Honest event times (adjudicated; the P4 causal-time finding)

- Every production activation/candidate request stamps `:time-ms` with the
  wall-clock of the REQUESTING act (client `Date.now()` through the write
  path, or server `now-ms` at request creation). Deterministic time
  constants are legal ONLY in tests via explicit opts — never in a shipped
  write path. (P1/P3/P5 shipped `time-ms` 0/1/2; idempotency never needed
  it — import identity is content-hash-keyed — so honesty costs nothing.)
- As-of/history reconstruction stays CAUSAL (the pointer revisions' parent
  chain — quirks-registry law); timestamps are display and ordering hints
  only; clock regressions stay exposed (`:ambiguous-activation-history`),
  never fabricated over.

## R4 — Activation events are declared forms (adjudicated)

The active-pointer revision's SOURCE grows from a bare revision-id string
to a closed edn form: `{:activation/revision-id … :activation/kind
:activate|:rollback|:pin|:unpin :activation/scope … :activation/actor …
:activation/time-ms … :activation/grounds […]}`. Scope in P6 =
`:scope/all-unpinned` (all unpinned wearers incl. future, by reference) or
`:scope/subject <uid>` (instance masters); richer scopes wait for need.
Grounds may ONLY name declared things (grounded-in experience/deviation,
responds-to conflict); ungrounded is legal and renders as ungrounded. The
old bare-string pointer sources remain valid v0 history — never
reinterpreted, never rewritten; readers treat them as
`{:activation/kind :activate}` with unknown grounds, labeled as such.

## Deliverables

1. Instance deviations per R2 (mint, serve, resolve, diff-vs-inherited).
2. Pins per R2.
3. Candidate revisions + the preview MEMBRANE (fulfills first-light P4):
   a candidate renders against REAL served material with the active face
   untouched — the P3 reconcile-before-stamp law applies to the preview
   path verbatim (labeled-derived state re-derives under the candidate;
   settled cells sovereign; nothing cached from the previous revision).
4. Scoped activation as accepted events per R3/R4; rollback = activation
   of a prior revision; blast-radius shown before/alongside (derived:
   wearers in scope minus pinned/deviant, "including future wearers" said
   honestly — by reference, NEVER fan-out writes).
5. Change announces itself at three scales: a local breath at affected
   appearances · a recoverable trace (revision, scope, actor, reversal
   path) · ambient material-weather (RecentChanges projection). Preview /
   deviation / scoped activation / canonical activation / rerender /
   rollback / recovery stay distinguishable.
6. Case reports DERIVED from the event trail, claiming only declared
   grounds (fulfills first-light P5's explain).
7. Standable history: project the material world at a historical
   cut/revision (causal, per R3).
8. The P5 carried closures: registry `:verb/required-args` + row-refusal
   (T10) · `:space/ground` instance refusal · floor-row label parity
   client/server · the served instance tier feeding `resolve-binding`.

## Traps ledger (cite by number in code comments)

- **T1 fan-out activation.** Naive: copy the new revision to every wearer.
  Failure: O(N) rollback, plurality broken, "future wearers" a lie.
  Ruling: one pointer flip; wearers re-resolve by reference.
- **T2 timestamp-ordered history.** Naive: sort activations by time-ms.
  Failure: P3/P5's deterministic stamps make clock order lie (proven in
  P4 — non-monotone history). Ruling: causal parent chain; times are
  display hints; regressions exposed.
- **T3 deviation as hint-row upsert.** Naive: geometry-cells shape for
  deviations. Failure: no history, no candidate/preview, no reversal
  record — breaks the package's own laws. Ruling: instance masters (R2);
  geometry cells stay settle-state only.
- **T4 pin as a code special case.** Naive: `if pinned` in the serve.
  Failure: invisible, unreversible, unrecorded. Ruling: pin is material
  (R2), activation-evented.
- **T5 per-keystroke instance resolution.** Naive: resolve instance
  masters per block per render. Failure: the P3 reconcile-storm doubt
  comes true; echo bar dies. Ruling: extend P5's identity-keyed wears
  cache — shared six resolved once per served identity, instance masters
  once per served identity per deviant subject; G6 re-measures the bar.
- **T6 relation kernel as policy store.** Naive: pins/deviations as RK
  edges. Failure: strata fusion (masters ≠ kinds). Ruling: refused (R1).
- **T7 registry row as truth.** Naive: trust the index row. Failure: a
  missed upsert silently unhosts a deviation. Ruling: the truth is
  container existence; the index is rebuildable and a test rebuilds it.
- **T8 open-ended event form.** Naive: free-form activation maps. Failure:
  case reports can claim undeclared grounds. Ruling: closed form (R4);
  ungrounded legal + labeled.
- **T9 preview against fake material.** Naive: render the candidate in a
  sandbox copy. Failure: the membrane lies (the P3 stale-derived-state
  class). Ruling: candidate against REAL served material, active face
  untouched, reconcile-before-stamp.
- **T10 arg-starved verb rebind** (P5 gate probe). Naive: any bindable
  verb at any site. Failure: a valid revision rebinds tap to a total
  no-op, lint-silent. Ruling: registry declares `:verb/required-args`;
  sites declare their claim-arg keys in ONE kernel place; `valid-row?`
  refuses a row whose site cannot feed its verb (grammar version bump on
  the three bindings grammars — additive, v1 never reinterpreted).
- **T11 tombstone confusion.** Naive: "deactivate" an instance master via
  the geometry tombstone lane. Failure: a tombstone cell DELETES THE UNIT
  from the page (Task 18 semantics — proven in the P5 gate's cell
  forensics). Ruling: removal of a deviation = activation of the inherited
  state (kind `:rollback` to parent), never a tombstone, never deletion.
- **T12 cross-JVM epoch staleness** (GATE_P1 carried). Naive: activate
  from any JVM. Failure: clients stale until an unrelated epoch move.
  Ruling: every activation write path routes through the app server's
  epoch bump; G7 asserts it.

## Acceptance gates (numbered; executable; run under `clj -X:test full`
lanes + the live cluster where marked LIVE)

- **G1 instance deviation end-to-end** (JVM): mint deviation → wearer
  resolves instance tier; other subjects untouched; diff-vs-inherited
  served; rollback restores inheritance; the whole trail readable as
  events. Negative reader: PState-level check that NO shared-master row
  was rewritten by an instance activation.
- **G2 pin**: pin survives a shared activation (pinned subject unchanged,
  unpinned subjects move); unpin re-joins; both recorded as events;
  malformed pinned-revision resolves to floor (totality).
- **G3 preview membrane**: candidate renders against real material;
  active face byte-untouched during preview (the P3 controlled-receipt
  method — same-process, camera-pinned); no derived state from the
  previous revision survives into the preview (T9).
- **G4 scoped activation + blast radius**: activation event carries the
  R4 form; blast-radius projection = exact wearer set (minus pinned,
  minus overriding deviations) BEFORE the flip; a subject born after
  activation wears the new revision without copying (future-wearers by
  reference).
- **G5 announcements**: one activation produces the breath, the trace
  (with reversal path), and the weather row; preview and rollback remain
  distinguishable in all three.
- **G6 echo bar re-measured LIVE** with ≥1 deviant subject + ≥1 pin in
  the serve: per-keystroke wear resolution stays cached (T5);
  0 samples > 52 ms; receipt carries adapter attestation first.
- **G7 epoch bump** (T12): an activation issued through the write path
  moves the client-visible epoch; a foreign-JVM activation is either
  routed or refused — never silently stale.
- **G8 event-truth trail** (GATE_P2 carried): the material inspector's
  trail classification reads activation EVENT kinds (not pointer-shape
  heuristics) for post-P6 history; v0 bare-string history still renders,
  labeled unknown-grounds.
- **G9 required-args** (T10): a row binding an arg-requiring verb at a
  site that cannot feed it fails `valid-row?`; the P5 gate's probe row
  (`:fold/toggle-section` at `:block/user-hit-area`) is the regression
  fixture; all 20 shipped rows still validate.
- **G10 space instance refusal**: `set-instance-bindings!` (and the
  durable instance lane) refuse `:space/ground`; the P5 gate's shadow
  probe becomes the regression test.
- **G11 honest times** (R3): no new deterministic `time-ms` constant in
  any production write path (grep-gate over src, test-opts exempted);
  activation events on the live cluster carry wall-clock times; causal
  as-of unchanged (its P4 tests stay green).
- **G12 Gate 4 immunity**: the P1 wound trace
  (`?drill=p1-final-1784884925150`, rejected revision retained as latest)
  lands as the record's first immune-memory entry; `__bindings.drillAll()`
  + the P1 malformed drill RE-RUN because the kernel changed (LIVE);
  a previous revision reworn from INSIDE the land (not the console).
- **G13 registry rebuild** (T7): drop the index row in a test world,
  rebuild from container existence, byte-equal.
- **G14 floor-label parity** (P5 finding 3): client tiers and server
  projection emit identical master-id/revision-id for floor rows.

Style gates stop at: naming/shape of NEW namespaces is the implementer's
(within the placement below); test tiering per the fail-closed runner law
(every new ns explicitly tiered); no gate constrains the face/visual
design of announcements beyond distinguishability (G5).

## Gate 3 — metabolism (verbatim stop; Sid's constitutional touchpoint)

Pick ONE banked friction from Sid's corpus (reply width or muted-gray
legibility — both recorded in his notes). Run it: pressure → record →
candidate → preview → **STOP AND REQUEST SID'S ONE-LINE ACTIVATION WORD —
never self-activate the first repair** → announced change → zero terminal
anywhere (the escape detector stays armed and must read 0 for the run).
The stop is delivered as a paste-able line for Sid with the preview
receipt attached.

## Non-goals / refusals (each an extension point)

No portal self-editing (P7) · no recipes · no new verbs (P8 births one) ·
no space-as-outermost-entity · no narrow invalidation unless G6 complains
(standing board item) · no attach/detach truth (first real need names its
owner) · no conformance lenses · **no new Rama module / PState / depot /
topology** (R1; stop clause on conflict) · no migration of existing
geometry cells (R2 grandfather) · no rewriting v0 pointer history (R4).

## Perf promise + read plan

The serve stays ONE batched facet-materials read, extended by: the
registry row (1 read) + the instance masters for served deviant subjects
(batched in the same projection call). No per-gesture reads — dispatch
consumes the served tiers (P5 law). Echo bar: 52 ms, gated live (G6).

## Deferred alongside (named, not P6's to close)

GATE_P4's four: live model turn for silver · headed ⌁/≈ look (Sid) ·
starter-culture corpus location · in-app LLM runtime first-invocation
seam. P5's dead-wheel-row class (finding 4) — fold into T10's site/args
declaration if cheap, else record as documented deadness.

## Input manifest

DIRECTION.md · CAMPAIGN.md §P6 + §Standing-stops · PROMPTS.md §P6 ·
GATE_P1..P5.md (carried doubts) · first-light CONTRACT §7 ·
`object_container/facet_master.clj` (the adapter — R2's base) ·
`shared/facet_material.cljc` · `shared/binding_material.cljc` +
`shared/verb_registry.cljc` (P5's law + registry) · `ground.cljs` stations
+ wears cache · `server/episode.clj` geometry lane (T3/T11 grandfather) ·
`memory/implementation-quirks.md` (causal-history law, flake registry,
shutdown state machine) · test lanes (`test_runner.clj`, fail-closed).

## Placement

Server: extend `object-container.facet-master` (spec synthesis for
instance masters) + `face_projection.clj` (serve/announce/blast/history
projections). Shared: `facet_material.cljc` resolution + the R4 event
form + T10 grammar bump. Client: workspace (preview membrane, breath,
weather, trace UI). New namespaces allowed under those homes; no new
top-level architecture. Code commits ride Sid's standing 07-24 word.

## Handoff

Implementer: Codex (PROMPTS.md §P6 — subordinate to this contract; on any
conflict THIS file wins and the conflict is a stop-clause note). Phases +
validation per the work-package skill (fresh-context layers; batched
falsification sized ONE finder at the genuinely-new machinery: the
instance-master serve/resolve path). Gates green + Gate-3's Sid word =
gate review (fresh Fable session). Stop clauses: R1 reversal · genuine
grammar fork · anything requiring existing-data migration → NOW.md + END.
