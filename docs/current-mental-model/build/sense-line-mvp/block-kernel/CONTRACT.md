# Block Kernel Contract — block-kernel-module

Status: DRAFT v1 (Fable, 2026-07-09, spec-room session). Realizes `../SPEC.md`
(block layer, v0) in Rama. Serves the agreed path (`../GROUNDS.md` §11):
implement → run ONE example chat → chunks readable in Rama → dual benchmark.
Pending: rama-pitfalls falsification (this session), Sid's redline, then
handoff as a work package per the work-package skill.

Binding order: `decisions.md` › `SPEC.md` › this contract › derived artifacts.
Where this contract pins something SPEC left OPEN, the pin is recorded back
into SPEC's OPEN list.

---

## 1 · Purpose and scope

Ingest ONE example session jsonl (default `7c80ce2a`, Sid may redirect) into:
**surfaces** (immutable canonical text), **production events** (actor chain +
context-parents), **blocks** (address+span+form, no copies), **river/debris
classification**, and the **mechanical edge floor** — then serve them back in
session order for the dual benchmark.

Consumers, in order: (1) the dual-benchmark step (Sid ∥ Fable independently
marking the chunk output); (2) the dogfood reconciliation UI; (3) the marks/
kinds side of the grammar (later — its targets are these blocks); (4) design-
room fixtures (real chunked material).

**Non-goals (each an extension point, §10):** mark-kind vocabularies; semantic
segmentation (stance-flip models); episodes/consolidator; benchmark machinery;
any UI; full-past ingestion (A1: ONE chat; the log appreciates later);
morning-answer projection; return-path assembly beyond gate-level occurrence
tests.

A1/A2 compliance: forward-capture-first, one example chat only (A1); this
module owns its writes — the jsonl driver streams observations INTO Rama via
depot appends; UI reads Rama (back-arrow rule; A2's not-read-only stance).

## 2 · Ruling: a new module, `block-kernel-module`

Own namespace `src/app/server/rama/block_kernel.clj`, own depot, one
microbatch topology, query topologies. Precedent: D-004's relation kernel.

- **Why not extend transcript ingest** (`dogfood/transcript*.clj`): it is a
  hardened import path at container grain (conversations, messages,
  tool-calls). Blocks are a different layer with different identity law
  (deterministic span addressing) and different pace (re-runnable strata).
  Blast radius for a worker model inside a hardened path is the dominant
  risk — same argument that kept relations out of object-container. Its plain
  fns (jsonl walking/parsing/redaction, `line-hash`, `file-id`, conv-keys)
  ARE reused as library imports.
- **Why not relation-kernel**: relations are edges; blocks are their targets.
  The relation kernel already accepts new target kinds (`RelationTargetRef`
  `target-kind` is an open keyword) — blocks arrive as `:block` targets with
  zero changes there.
- **Why not text_kernel/object-container**: editor-document machinery and
  doc-scoped partition discipline respectively; neither owns chat-stream
  addressing.

Reversal cost if wrong: PState migration is manual and disruptive. Seam that
keeps it low: ALL consumer access goes through the query topologies (§6);
nobody hard-codes PState paths; a future merge moves the module behind a
mirror without touching consumers.

## 3 · Rulings that pin SPEC OPEN items (record back into SPEC)

**R1 · Canonical text = post-redaction (SPEC §2.1 amendment).** The producer's
raw text passes the declared, versioned redaction transform (reusing
`transcript.clj`'s redaction fns) exactly ONCE, at ingest; the result is the
surface's canonical text, immutable thereafter. Redaction events are recorded
(rule-id@version, count, span positions in canonical text) and redacted spans
MUST render as visible redaction marks — the map must not lie. Raw secrets
never enter any depot or PState (env.clj hard rule generalized). The
reconstruction guarantee (SPEC §2.3) holds against canonical text.

**R2 · Offsets = UTF-16 code units** over the canonical text (SPEC §2.2 pin).
Rationale: JVM and JS strings are both natively UTF-16 — zero-conversion at
both the splitter and the renderer; codepoint conversion is mechanical if ever
needed. Constraint (MUST): no span boundary may split a surrogate pair (gate
G12). Reversal: a span re-mapping migration — mechanical because surfaces are
immutable.

**R3 · Deterministic ids — identity by construction.**
`surface-id = "surf:" sha256(session-id ∥ event-uuid ∥ part-path)` (truncated
hex); `block-id = "blk:" sha256(surface-id ∥ start ∥ end)`. SPEC §6.1's
resolve-don't-duplicate becomes structural: re-runs regenerate identical ids;
no id-minting state exists; idempotency needs no read-check. Ids embed nothing
positional beyond their key — identity is never path-dependent (SPEC §6.3).
Production-event id = `"pev:" sha256(session-id ∥ event-uuid)`.

**R4 · Own surface store** (not an adapter over transcript-ingest PStates),
because P0 must otherwise prove the existing store preserves full canonical
payload text with stable sub-entry addressing — and even if it does, coupling
block addressing to another module's storage layout couples two pace layers.
Cost: duplicate text for ONE chat ≈ trivially small. Extension point: an
address-mapping adapter later, if full-corpus ingest ever demands it.

## 4 · Data model (contract-grade; Phase-0/plan refine field lists)

Plain map envelopes with namespaced top-level keys; typed records may ride
inside fields the partitioner never reads (relation-kernel style-gate
precedent). All maps `{String (map-schema Keyword Object)}`-convention unless
the plan justifies tighter schemas.

- **SurfaceRow** `{:surface/id :surface/session-id :surface/event-uuid
  :surface/part-path :surface/text :surface/content-hash :surface/class
  (:river | :debris-<sub>) :surface/actor :surface/prod-event-id
  :surface/seq :surface/redactions [...]}` — `part-path` examples:
  `"message.content"` (human), `"content[0].thinking"`, `"content[2].text"`,
  `"toolUseResult"`.
- **ProductionEventRow** `{:pev/id :pev/session-id :pev/event-uuid
  :pev/actor :pev/actor-chain [...] :pev/on-behalf-of :pev/sidechain?
  :pev/prompt-id :pev/parents {:degenerate-prefix <prev-event-uuid>}
  :pev/plan nil :pev/t}` — parents recorded AS degenerate at chat ingest
  (SPEC §7.2), never dressed up.
- **BlockRow** `{:block/id :block/surface-id :block/session-id :block/span
  [start end) :block/form <SPEC §4.6 enum> :block/seg-id :block/minted-by
  :block/t}` — NO text field, ever (Trap T6).
- **SegmentationRow** `{:seg/id :seg/rule-id :seg/rule-version :seg/producer
  :seg/session-id :seg/t}`.
- **OccurrenceRow** `{:occ/id :occ/block-id :occ/assembly-surface-id
  :occ/position}`.
- **Classification** is stored ON the surface (`:surface/class`), plus a
  per-session event ledger for debris (events that produce NO surface still
  get a classified ledger row — conformance G1 needs to see them).
- **Mechanical edges** → relation-kernel requests: target-kind `:block` /
  `:git-commit` / `:doc-file`, kinds `produced · grounds · assembled-from ·
  refines · supersedes`, `asserted-by "block-kernel/mechanical@<version>"`,
  silver tier. **Appended by the DRIVER (foreign side), never from topology
  code** — a cross-module depot append inside the topology is a world effect
  under retry (pitfalls §2). Each request carries a **deterministic
  idempotency key** `sha256(block-id ∥ kind ∥ target-id)` conforming to the
  relation kernel's relation-scoped idempotency, so driver re-runs dedupe
  inside the relation kernel. P0 verifies (a) the kind vocabulary is open
  (strings/keywords, not enum-gated) and (b) the idempotency-key scope
  semantics; if kind-gated, STOP-CLAUSE (§9) — options: amend relation
  kernel (small) vs local edge PState (defers unification).

## 5 · Depot, topology, partitioning

- ONE depot `*block-events` partitioned `(hash-by :block/session-id)` — every
  event type (surface-recorded, block-minted, refinement-requested,
  assembly-recorded) carries the session-id routing key present-always (the
  submit!==+1 barrier invariant needs no ingress drops).
- ONE microbatch topology materializes all PStates (exactly-once, no
  low-latency need; stream buys nothing here).
- Driver (plain Clojure, OUTSIDE topologies — Trap T3): walks the ONE jsonl
  file with `transcript.clj` reader fns, applies redaction, classifies,
  resolves actors, cuts blocks (pure fns, unit-testable without IPC), appends
  envelope events via foreign append — **`:append-ack`** (bulk; no per-append
  materialization wait; tests use the deterministic
  microbatch-processed-count barrier, never polling). The driver ALSO appends
  the mechanical-edge requests to the relation-kernel depot (`:append-ack`,
  deterministic idempotency keys — §4). Re-running the driver re-appends;
  determinism (R3) + idempotency keys make the whole run idempotent at the
  PState layer (gate G4 proves zero drift on re-run).
- **PStates** (all partitioned by session-id; per-session inner maps
  subindexed — sessions hold 10²–10⁴ entries):
  - `$$session->surfaces` `{session-id (map-schema seq-key SurfaceRow
    {:subindex? true})}` — seq-key = `fixed-width-order-key` of event seq ∥
    part index (ordered river read = 1 seek + iteration).
  - `$$surfaces-by-id` `{surface-id SurfaceRow-pointer}` — pointer row
    {session-id, seq-key}, NOT a second text copy (Trap T6a: one text copy
    total). Routing: surface-id is hash-derived, so this PState partitions by
    surface-id — an extra partitioner hop on point reads, accepted (rare
    path; river reads dominate).
  - `$$session->blocks` `{session-id (map-schema block-order-key BlockRow
    {:subindex? true})}` — block-order-key = surface seq-key ∥ span-start
    fixed-width (river page join = seeks: 1 surfaces + 1 blocks, then
    iterate).
  - `$$blocks-by-id` `{block-id {:session-id ... :order-key ...}}` pointer,
    partitioned by block-id (mark-layer point lookups later).
  - `$$session->events-ledger` `{session-id (map-schema seq-key
    {:class ... :event-uuid ...} {:subindex? true})}` — every jsonl event,
    debris included.
  - `$$session->prod-events` `{session-id (map-schema pev-id
    ProductionEventRow {:subindex? true})}`.
- **Query topologies** (consumers use ONLY these; queries are READ-ONLY):
  `river-page` (session-id, from-key, limit → ordered surfaces with their
  blocks; text sliced on read from the one stored copy); `block-resolve`
  (surface-id, span → block-id or nil — never writes; R3 determinism means a
  client helper can COMPUTE the id locally and append a mint event
  fire-and-forget, no read-modify-write roundtrip); `block-text` (block-id →
  sliced text + form + provenance).

Read plans for the promises: river page = 2 seeks (surfaces + blocks
subindex ranges) + sequential iteration; block-text = 2 seeks (pointer +
surface row); block-resolve hit-path = 1 seek. Worst-case estimates per the
skill's cost model; Phase 4 audits divergence.

## 6 · The free cut (pure-fn layer)

Implements SPEC §4 exactly; the cut functions are PURE (`(cut surface-text
part-kind) → [{:span [s e) :form ...}]`) and live apart from topology code so
G3/G12 run as plain unit tests. MUSTs it inherits: provider content parts as
given; markdown units within text (paragraph/list-item/header/fence/quote/
table); fences-tables-quotes atomic; tool_use one block; tool_result surface
only, NO pre-chunk; human whole-message block + silver structural sub-blocks
(default ON, Sid's OPEN); every segmentation declares (rule-id@version).
Form vocabulary = SPEC §4.6 closed list.

## 7 · Traps ledger (implementers cite trap numbers in code comments)

- **T1 · role ≠ actor.** user-role events include tool_results, meta caveats,
  command wrappers. Actor resolution per SPEC §3.4 BEFORE anything else;
  classifying a tool_result as the human is the canonical failure (gate G1
  physical-reader check).
- **T2 · offsets.** UTF-16 code units (R2); `.substring`/`subs` semantics —
  never codepoint APIs mixed in; no span through a surrogate pair (G12).
- **T3 · retry vs side effects.** Topologies write PStates only. File I/O
  lives in the driver (foreign side). A retried microbatch must redo ONLY
  PState writes (`feedback_rama_side_effects`).
- **T4 · event boundary = partitioner.** Ids minted BEFORE append (R3:
  deterministic, so mintable anywhere); ONE routing key (session-id) per
  event; no multi-key "atomic" illusions (`feedback_rama_event_boundaries`).
- **T5 · identity scope.** All dedup/idempotency is **session-scoped** (the
  partition); ids are globally unique by construction (hash) but uniqueness
  is only ENFORCED within the session partition — cross-session collision =
  hash collision (accepted; 128-bit truncation).
- **T6 · no text copies.** Block rows never store text (SPEC Material law);
  `$$surfaces-by-id` stores a pointer, not the row. ONE text copy per
  surface, total. Slicing happens at read.
- **T7 · subindex the per-session maps** (10²–10⁴ entries); do NOT subindex
  the tiny per-surface redaction lists.
- **T8 · control bytes.** tool_result text may contain anything; tests spell
  NUL as ` ` escapes (git-binary + tool-JSON traps, quirks memory).
- **T9 · debris is retained.** Classification never drops events; the ledger
  row exists even when no surface does (SPEC §3.2; G1 counts both sides).
- **T10 · cooperative yield.** River-page iteration over subindexed maps uses
  `{:allow-yield? true}` on large `local-select>`s.
- **T11 · cross-module appends live in the driver only.** Topology code never
  appends to another module's depot (retry = duplicate world effect); all
  relation-kernel requests are driver-side with deterministic idempotency
  keys (§4).
- **T12 · ack + barrier.** Driver appends `:append-ack`; the test suite's
  done-signal is the microbatch-processed-count barrier with the
  submit!==+1 invariant (routing key always present) — never polling.
- **T13 · pointer PStates are a separate event.** `$$surfaces-by-id` /
  `$$blocks-by-id` writes sit behind a partitioner hop from the session-row
  writes — NOT atomic with them. Readers nil-tolerate a transient
  pointer/row skew; at-rest consistency is the microbatch's exactly-once
  guarantee.

**Pitfalls verdict (2026-07-09, this session):** protocol run against this
contract — §1 minor-fail and §2/§6 fails found and fixed as T11–T13 + §4/§5
amendments above; §3/§4/§5-ids/§7/§9/§10 PASS; §8/§11 N/A. OVERALL after
amendment: READY-FOR-PHASE-0. Residual: relation-kernel kind-openness and
idempotency-scope are P0-verify items; pointer-skew tolerance must appear in
query-topology code review (Phase 4 checks it).

## 8 · Acceptance gates (numbered; IPC tests unless marked style/review)

Fixture: the example jsonl (committed test copy of a SMALL real session or a
constructed fixture reproducing the SPEC §15 head shapes + one fenced code
block + one emoji/astral char + one tool_use/tool_result pair + one NUL in a
tool_result).

- **G1 classification & actor.** Every fixture event has a ledger row; class
  counts match the hand-counted golden; ZERO surfaces with actor=human that
  carry tool_result/meta markers — asserted by a validation-only PState
  reader, not the query API.
- **G2 delegation chain.** Assistant surfaces carry model actor +
  on-behalf-of; sidechain events flagged; prompt-id grouping present.
- **G3 free cut golden.** Forms and spans over the fixture match the golden
  file exactly (fences/tables/quotes atomic; human whole+subs; headers own
  blocks per SPEC OPEN default).
- **G4 idempotence.** Full re-run of driver+topology on the same file: PState
  row counts and ids identical (physical count reader; zero new rows).
- **G5 stratum addition.** A second segmentation (test rule) adds blocks with
  its own seg-id; first stratum's ids/rows untouched byte-for-byte.
- **G6 reconstruction + redaction honesty.** Every surface's stored text ==
  canonical post-redaction text (char-for-char vs independently computed
  golden); redaction records present where the fixture planted a secret;
  planted secret string absent from EVERY PState (grep-the-store check).
- **G7 refinement.** Demand-mint of a sub-span: new block with engagement
  provenance; coarse block persists; both resolvable.
- **G8 mechanical edge floor.** tool_use write-call → `produced` edge;
  read-call → `grounds`; both in relation kernel with asserted-by
  block-kernel/mechanical + silver tier, target-kind `:block`/`:doc-file`.
- **G9 holes.** An edge request with an absent endpoint persists and is
  queryable (relation kernel descriptor row) — SPEC §9 RATIFIED requirement.
- **G10 assembly & transclusion.** Assemble 2 blocks → new surface (form
  `assembled`) + `assembled-from` edges + occurrences addressable; the
  transcluded blocks keep their ids (no copy).
- **G11 span discipline.** Property test: all spans in-bounds, non-empty,
  no surrogate-pair splits, sub-block spans within parent (G12 merged here).
- **G12 (style, review-time — names its boundary).** Partitioner-read keys
  are top-level namespaced keys on plain map envelopes; typed records only
  inside fields partitioners never read; records allowed in PStates. Stops
  at: no requirement to defrecord anything.
- **G13 (style, review-time).** No PState path hard-coded outside the module
  + its query topologies; consumers compile against query names only. Stops
  at: tests' validation-only readers are exempt (they MUST read raw).

Definition of done: gates green + a REPL/CLI invocation documented in the
package README that runs the real example chat end-to-end and pretty-prints
the first river page — the artifact Sid and Fable each take into the dual
benchmark. Break QUALITY is explicitly NOT gated here (SPEC ≠ BENCHMARK).

## 9 · Stop clauses (escalate per work-package skill; never improvise)

- Relation-kernel kind vocabulary turns out enum-gated (§4 last bullet).
- Existing redaction fns prove non-deterministic or version-unstable
  (breaks R1's exactly-once canonicalization).
- The jsonl format shows event shapes the SPEC classification cannot place
  (new debris classes are implementer-fixable; a RIVER-shaped ambiguity is a
  policy fork).
- Any SPEC MUST unimplementable under Rama semantics as written.

## 10 · Extension points (refusals, kept warm)

Marks layer (kinds round) targets `:block`/occurrences via relation kernel —
nothing here changes. Semantic segmentations = new seg-ids, same depot.
Episode grouping = new module reading closed spans. Full-corpus ingest =
driver loop + (maybe) the R4 adapter. Benchmark apparatus consumes
`river-page` as-is. Reconciliation UI reads only query topologies (G13).

## 11 · Input manifest (what a fresh model needs to reproduce this contract)

`../SPEC.md` (all) · `../GROUNDS.md` §2 §4 §11 · this file ·
`docs/current-mental-model/build/relation-kernel/CONTRACT.md` §1–3 (target
refs, envelope style-gate precedent) · `src/app/server/rama/relation_kernel.clj`
:616–656 (declarations) · `src/app/server/rama/dogfood/transcript.clj` reader/
redaction fns (:290–610) + `transcript_ingest.clj` id helpers (:27–70) ·
`memory/implementation-quirks.md` · `.claude/skills/rama/` (phases) · example
transcript `~/.claude/projects/-mnt-data-projects-Softland/7c80ce2a-*.jsonl`
(read spans only) · CLAUDE.md hard rules (env.clj; commit discipline).

## 12 · Handoff

Implementer: worker models (Opus-class) under the /rama phased process —
Phase 0 (requirements re-derivation; INHERITS §5's partition context
verbatim — the F2 lesson) with **P0-verify items**: (a) relation-kernel kind
openness, (b) redaction fn determinism + versioning, (c) exact jsonl event-
shape inventory of the example file vs SPEC §3 classes, (d) relation-kernel
idempotency-key scope semantics (driver keys must conform — §4). Then plan →
validation (default-fail, per-round artifacts kept) → implementation with
gates-to-green in-context → adversarial diff review (fresh context) → Fable
gate review (re-runs suite; falsification pass per CLAUDE.md).

Orchestration: ONE orchestrating session; validation/review layers as fresh
subagents (work-package amendment 2026-07-05); baton entries ≤15 lines in
`docs/sessions/next-prompt.md` NOW; STANDING frozen at package open. Budget:
local + subscription, no API dollars. What must NOT start without Sid: any
schema for mark KINDS; any UI; any second chat file; any push/merge of docs.
