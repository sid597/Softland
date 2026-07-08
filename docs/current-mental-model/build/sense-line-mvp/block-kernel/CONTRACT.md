# Block Distiller Contract — the block layer over the container kernel

Status: **v2 BINDING** — placement re-ruled by Sid 2026-07-09 (Option A,
adapter shape: *"this is what it was designed for"*), replacing v1's
new-module shape (v1 in git history, `e0ad1cc`). Realizes `../SPEC.md`
(block layer, v0 — countersigned) as **a new distiller adapter + driver over
the EXISTING `object-container-module` and `relation-kernel-module`** — the
`git_spine.clj` shape ("NOT a Rama module — pure adapter fns + a driver …
over the EXISTING object-container and relation-kernel public APIs"), one
grain finer. Package dir keeps the working name `block-kernel/`; every noun
is Sid's to rename.

Binding order: `decisions.md` › `SPEC.md` › this contract › derived
artifacts. Where this contract pins something SPEC left OPEN, the pin is
recorded back into SPEC.

---

## 1 · Purpose and scope

Distill ONE example session (default `7c80ce2a`, Sid may redirect) into
**blocks** — SPEC-grade units at engagement grain — living in the container
kernel's existing ontology: derived units + span anchors + containment edges
over immutable sources, plus **production events** (actor chain, delegation,
context-parents) and the **mechanical edge floor** into the relation kernel —
then serve the session back in order for the dual benchmark.

Consumers, in order: (1) the dual benchmark (Sid ∥ Fable independently
marking the chunk output); (2) the dogfood reconciliation UI; (3) the
marks/kinds side (its targets are these units); (4) design-room fixtures.

**Non-goals (each an extension point, §10):** mark-kind vocabularies;
semantic segmentation (stance-flip models); episodes/consolidator; benchmark
machinery; any UI; full-past ingestion (A1: ONE chat); morning-answer
projection; assembly beyond gate-level tests.

A1/A2 compliance: forward-capture-first, one chat (A1); all writes ride the
lawful request path into the container kernel's depot — the driver is a
foreign client; UI reads Rama (back-arrow; A2).

## 2 · Placement ruling — adapter over the container kernel (Sid, 2026-07-09)

**A block IS the atomic container of text** (Sid's formulation, upheld
against source). The container kernel was built for exactly this pattern:

- Distillation is **foreign-side by design**: the ingest request payload
  CARRIES derived units + anchors + edges (`payload-derived-units`,
  object_container.clj:620); the module materializes generically. A new
  distiller = a new adapter file, zero module surgery.
- The pattern already runs: `markdown-block-v0` (markdown_adapter.clj:6)
  cuts markdown into block-grain units with offsets, unit-kinds, containment
  edges, deterministic `du:` ids (:14).
- The rows are the SPEC's nouns: `SourceArtifactRow.source-raw-text` (:83) =
  surface material; `SourceAnchorRow.start-offset/end-offset` (:122) = span
  address; `DerivedUnitRow.unit-kind + distiller-id@version` (:113) = form +
  segmentation provenance; `CompositionEdgeRow` (:126) = containment as
  edges; `RevisionRow` (:109) = supersedes chains.
- A public read API exists: `read-unit` (:2473), `read-current-revision`
  (:2485), `read-common-material-for-source` → `{containers derived-units
  anchors edges}` (:2501).
- Precedent for the package shape: git-spine WP2 — adapters + driver over
  the two kernels, no new module, gate-passed and closed.

Why not a sibling module (v1's shape): it would duplicate an existing
derived-unit ontology 2,600 lines away — the "two truths drift" failure the
land already ruled against. Why not a mirror-module (option B): it keeps
worker isolation but splits the ontology, which is the objection that
opened this fork.

Reversal cost if the adapter shape is wrong: re-point the driver + migrate
unit/anchor rows to wherever blocks move — medium, symmetric with any
alternative. The seam that keeps consumers stable: **readers use query
topologies only** (G13); nobody compiles against PState paths.

## 3 · Rulings

**R1 · Canonical text = post-redaction** (SPEC §2.1). The container kernel
already carries redaction policies (`transcript-redaction-policies
#{:standard}`, :32) — the block distiller inherits the SAME canonical text
its sources were stored with; it never re-redacts. P0 verifies the policy is
deterministic + versioned.

**R2 · Offsets = UTF-16 code units** (SPEC §2.2). Consistent with the
existing adapter convention — markdown_adapter counts offsets with
`(count line)` on JVM strings (:26-40), which IS UTF-16 units. No span may
split a surrogate pair (gate G11).

**R3 · Identity follows container-kernel conventions.** Unit ids embed the
distiller (`du:<object-key>:<distiller-id>:<block-path>` per
markdown_adapter:14) — deterministic, so re-runs converge and strata coexist
by id design (two distillers over one source = disjoint id spaces). **Span
identity lives at the anchor layer**: SPEC §6.1's "same (surface, span) ⇒
same identity" maps to anchor rows; P0 pins anchor-id determinism. The
distiller id for this work: `sense-block-v0`, version 1.

**R4 · Sources are the container kernel's — no second store.** The distiller
runs over ALREADY-INGESTED transcript sources (read via the query API), cuts,
and submits derived-unit import requests for those same sources. P0 GATE:
verify the import path accepts a second distillation over an existing source
(the strata question — identity claims / material fingerprints must not
reject it). Fallback if blocked: a fenced, additive import-request variant
(stop-clause first — §9).

**R5 · Block text is anchor-first.** SPEC's no-copies law meets
`DerivedUnitRow.derived-content-text` (a text field by OC design): the
distiller stores the field ONLY if the import path requires it, and then it
MUST equal the anchored span exactly (content-hash equality, gate G6) — a
verified cache, never a second truth. Lean: empty/nil derived text, anchor
is the address. P0 verifies OC tolerates unit rows without derived text.

## 4 · Data model — SPEC nouns mapped onto container-kernel rows

| SPEC noun | realization | notes |
|---|---|---|
| surface | existing source rows (`SourceArtifactRow` / source-line identity) + message containers the transcript adapter already mints | P0 pins the exact source/anchor granularity the transcript adapter uses today (per-file vs per-line) |
| block | `DerivedUnitRow` (distiller `sense-block-v0`) + `SourceAnchorRow` | anchor carries the span; unit carries form + provenance |
| form | `unit-kind` values = SPEC §4.6 vocabulary | vocabulary addition, never new row types (T14) |
| segmentation provenance | `distiller-id` + `distiller-version` on every unit | already first-class |
| containment | `CompositionEdgeRow` parent/child slots + order keys | computed tree, SPEC §6.2 |
| production event | actor + delegation chain (`parentUuid` is read at transcript.clj:336; sidechain/promptId are NOT captured) | P0 decides: additive projection-row fields (additive-field precedent: claimed-at-ms carve-out) vs relation edges; lean = additive fields |
| classification (river/debris) | driver-side classifier; result rides the import request; ledger visibility per conversation projection entry-kinds | P0 pins the row; requirement is SPEC §3 + gate G1, not a specific row |
| occurrence / assembly | assembly = a NEW source ingest whose units carry `assembled-from` relation edges to source blocks | gate G10 exercises the minimal path |
| mechanical edges | relation-kernel requests, target-kind `:block` (unit ids), kinds `produced · grounds · assembled-from · refines · supersedes`, `asserted-by "sense-block/mechanical@1"`, silver tier | driver-side ONLY (T11), deterministic idempotency keys `sha256(unit-id ∥ kind ∥ target-id)` conforming to relation-scoped idempotency |

## 5 · Flow

```
 container kernel (sources already ingested)          relation kernel
        ▲ read: query API                                    ▲
        │                                                    │ foreign-append
        │ submit: derived-unit import requests               │ :append-ack,
        │ (:append-ack; test barrier = deterministic         │ idempotency keys
        │  processed-count invariant, never polling)         │
   ┌────┴────────────────────────────────────────────────────┴───┐
   │ DRIVER + block distiller (plain Clojure, foreign side):     │
   │  read sources → classify river/debris → resolve actor       │
   │  (role ≠ actor) → FREE CUT (pure fns, markdown_adapter      │
   │  conventions) → mint deterministic ids → submit             │
   └──────────────────────────────────────────────────────────────┘
```

Topologies are the kernels' own; this package adds NONE. All side effects
(file reads if any, cross-module appends) live in the driver (T3/T11).

## 6 · The free cut (pure-fn layer — unchanged from SPEC §4)

Adapter fns following markdown_adapter's shape (emit-block pattern, offset
convention): provider content parts as given (`thinking`/`text`/`tool_use`);
markdown units inside text parts (paragraph · list-item · header · fence ·
quote · table; fences/tables/quotes atomic); tool_result = material, NO
pre-chunk (spans mint lazily on engagement); human messages = whole-message
block + silver structural sub-blocks (default ON, SPEC OPEN-1); every
segmentation declares rule-id@version. Unit-testable without IPC.

## 7 · Traps ledger (implementers cite trap numbers in code comments)

- **T1 · role ≠ actor.** tool_results and harness injections arrive as
  user-role events; attributing them to the human is the canonical failure
  (G1 physical-reader check).
- **T2 · offsets.** UTF-16 units; `subs`/`(count …)` semantics; no surrogate
  splits (G11).
- **T3 · side effects live in the driver.** Kernel topologies stay pure;
  this package never adds in-topology world effects.
- **T4 · routing.** Import requests carry the container kernel's partition
  key exactly as the transcript adapter builds it — never invent a second
  routing convention.
- **T5 · idempotency scope = the kernel's import semantics** (material
  fingerprints, identity claims — P0 verifies for second distillations);
  relation requests dedupe relation-scoped (T11 keys).
- **T6 · one text truth.** Anchor-first (R5); any stored derived text must
  hash-equal its span (G6).
- **T7 · never write kernel PStates directly.** Requests through depots
  only; validation-only readers in tests are the sanctioned exception.
- **T8 · control bytes.** tool_result text contains anything; tests spell
  NUL as escapes.
- **T9 · debris is retained** — classified, visible, unmarkable by default
  (G1 counts both sides).
- **T10 · big reads yield.** Any large query-API iteration uses the kernels'
  existing pagination; never slurp a whole conversation into one select.
- **T11 · cross-module appends: driver only, deterministic idempotency
  keys** (§4 last row).
- **T12 · ack + barrier.** `:append-ack` everywhere; deterministic
  processed-count barrier in tests, never polling.
- **T13 · additive-only touches.** Any object_container.clj edit is named in
  the allowlist, additive, and cites the additive-field precedent
  (claimed-at-ms carve-out, SourceIngestCompletionRow:90-97). Anything else
  = stop-clause.
- **T14 · vocabulary, not schema.** New unit-kinds / entry-kinds are VALUES.
  New row types require a stop-clause, never improvisation.

## 8 · Acceptance gates (IPC tests unless marked review-time)

Fixture: committed small real-shaped jsonl reproducing SPEC §15's head
shapes + a fenced code block + an astral-plane char + a tool_use/tool_result
pair + one NUL in a tool_result + one planted fake secret.

- **G1 classification & actor.** Every fixture event classed; counts match
  golden; ZERO human-attributed tool_result/meta events — physical
  PState-level reader, not the query API.
- **G2 delegation chain.** Model actor + on-behalf-of chain present;
  sidechain/promptId captured per the P0-ruled realization.
- **G3 free cut golden.** Forms and spans match the golden file exactly.
- **G4 idempotence.** Full re-run: zero new rows, identical ids (physical
  count reader).
- **G5 strata.** A second distiller (test rule) adds its units; the first
  stratum untouched byte-for-byte.
- **G6 anchor honesty.** Every anchor's span slices the stored source to
  exactly the expected text; any derived-content-text hash-equals its span;
  planted secret absent from EVERY row (redaction inherited correctly).
- **G7 refinement.** Demand-mint of a sub-span unit with engagement
  provenance; coarse unit + its marks persist.
- **G8 mechanical edge floor.** Write-tool → `produced`, read-tool →
  `grounds` in the relation kernel, asserted-by sense-block/mechanical,
  silver, target-kind `:block`.
- **G9 holes.** An edge request with an absent endpoint persists and is
  queryable (SPEC §9 RATIFIED).
- **G10 assembly & transclusion.** Two blocks assembled → new source +
  `assembled-from` edges + re-addressed (not copied) units.
- **G11 span discipline.** Property test: in-bounds, non-empty, no surrogate
  splits, child spans within parents.
- **G12 (review-time).** River-page read plan audited: bounded seeks +
  sequential iteration; no N-point-read fan-out per page.
- **G13 (review-time).** Consumers compile against query topologies only;
  test validation readers exempt.

Definition of done: gates green + a documented REPL/CLI invocation running
the REAL example chat end-to-end and pretty-printing the first river page —
the artifact Sid and Fable each take into the dual benchmark. Break QUALITY
is not gated here (SPEC ≠ BENCHMARK).

## 9 · Stop clauses (escalate per work-package skill; never improvise)

- Second distillation over an existing source is rejected by the import
  path (R4 gate) → options: fenced additive request variant vs re-ingest
  under a distinct source-ref; Sid rules.
- Anchor ids prove non-deterministic (R3).
- Redaction policy proves non-deterministic/unversioned (R1).
- Relation-kernel kind vocabulary is enum-gated (§4).
- OC requires derived-content-text and cannot hold span-equal text (R5).
- Any SPEC MUST unimplementable as specified.

## 10 · Extension points (refusals, kept warm)

Marks layer targets unit ids/occurrences via the relation kernel — nothing
here changes. Semantic segmenters = new distiller ids, same import path.
Episodes = a consolidator reading closed spans. Full-corpus = driver loop.
river-page hardening (dedicated query topology vs CommonMaterialBundle
composition) = P0's call, revisable at UI time.

## 11 · Input manifest

`../SPEC.md` (all) · `../GROUNDS.md` §2 §4 §11 · this file ·
`src/app/server/rama/object_container.clj` :1-180 (records) + :1692-1760
(module decl) + :2443-2520 (query API) · `object_container/
markdown_adapter.clj` (the distiller pattern) + `transcript_adapter.clj`
(fn inventory) · `dogfood/transcript.clj` :290-610 (reader/redaction fns) ·
`relation_kernel.clj` :616-656 + `build/relation-kernel/CONTRACT.md` §1-3 ·
`git_spine.clj` :1-40 (the package-shape precedent) ·
`memory/implementation-quirks.md` · `.claude/skills/rama/` + `/rama-pitfalls`
· example transcript `~/.claude/projects/-mnt-data-projects-Softland/
7c80ce2a-*.jsonl` (spans only) · CLAUDE.md hard rules.

## 12 · Handoff

Implementer: worker models under /rama phases inside the /work-package
shell; ONE orchestrating session; QC layers as fresh Opus subagents.

**Phase 0 P0-verify items (all before plan):**
(a) relation-kernel kind-vocabulary openness;
(b) redaction determinism + versioning (R1);
(c) jsonl event-shape inventory vs SPEC §3 classes;
(d) relation idempotency-key scope (driver keys conform);
(e) second-distillation-over-existing-source import semantics (R4 gate);
(f) transcript source/anchor granularity + anchor-id determinism (R3/R4);
(g) river-page realization: existing `read-common-material-for-source`
composition vs one additive query topology (lean: composition first);
(h) delegation-chain capture point (additive projection fields vs edges).

Budget: local + subscription, no API dollars. What must NOT start without
Sid: mark-kind schemas · any UI · a second chat file · any non-additive
object_container.clj change · any relation-kernel amendment.
