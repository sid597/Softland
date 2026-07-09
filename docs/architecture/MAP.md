# Softland Codebase Map — living, read-grounded

Started 2026-07-09 (Fable, spec-room session) on Sid's ask: a full
architecture map "for me and you … and future readers." Rules of this doc:

- **Entries are grounded in actual reads, never greps.** Every entry carries
  a depth marker and date. A grep may locate; only a read may describe.
- Depths: **READ** (lines read in full) · **PARTIAL** (named line ranges) ·
  **DECLS** (declarations/fn-lists only — locate-grade, not describe-grade) ·
  **UNREAD** (listed for completeness; do not trust anything said about it).
- Update by direct replacement; git carries history (no banners). Grow it
  whenever a session genuinely reads something new.

## Coverage ledger

| file | depth | lines read | date |
|---|---|---|---|
| `src/app/server/rama/object_container.clj` (2,655 ln) | PARTIAL | 1–180 (docstring + all 30 row records); DECLS 1692–1760 (module, depots, PStates); DECLS 2443–2501 (query topologies) | 2026-07-09 |
| `object_container/markdown_adapter.clj` | PARTIAL | 1–40 (distiller id/version, patterns, offset walker) + emit-block sites | 2026-07-09 |
| `object_container/transcript_adapter.clj` | DECLS | public fn inventory only | 2026-07-09 |
| `object_container/{runtime,transcript_identity}.clj` | UNREAD | — | |
| `relation_kernel.clj` | DECLS | 616–656 (module/depot/PStates) + its CONTRACT.md §1–3 (READ) | 2026-07-09 |
| `git_spine.clj` | PARTIAL | 1–8 (ns docstring — self-describing) | 2026-07-09 |
| `trail_view.clj` | PARTIAL | 1–8 (header) | 2026-07-09 |
| `kernel.clj` (826 ln) | PARTIAL | 1–8 (header) | 2026-07-09 |
| `text_kernel.clj` (733 ln) | PARTIAL | ns form only | 2026-07-09 |
| `dogfood/transcript.clj` (1,522 ln) | DECLS | fn inventory + observation field accessors (206–219) | 2026-07-09 |
| `dogfood/space.clj` | PARTIAL | 1640–1685 (event-materialization region: objects/turns/bundles/llm-runs + `$$artifact-graph` writes) | 2026-07-09 |
| `dogfood/{compute,llm}.clj` | UNREAD | ns forms only | 2026-07-09 |
| `dogfood/transcript_ingest.clj` | DECLS | id-helper fn list (27–70) | 2026-07-09 |
| `roam_ns.clj`, `objects.cljc`, `core.clj`, `util_fns.cljc` | UNREAD | (roam_ns: attribute list glanced) | |
| client layer (`electric_flow.cljc`, `client/substrate/webgpu/renderer.cljs`, `client/workspace/*`, `client/workflows/*`) | UNREAD | — | |

## The server shape (grounded in the reads above)

```
                     ══ WORLD-LINE material ══                ══ SENSE-LINE assertions ══
  ┌──────────────────────────────────────────────────┐   ┌───────────────────────────────┐
  │        object-container-module                   │   │   relation-kernel-module      │
  │        THE material kernel                       │   │   typed RelationEdges with    │
  │  sources · derived units · anchors ·             │   │   asserted-by provenance,     │
  │  containers · revisions · composition edges ·    │   │   idempotency PStates, OPEN   │
  │  outline & transcript projections                │   │   target-kinds                │
  └───────▲──────────────────────────▲───────────────┘   └──────────────▲────────────────┘
          │ ingest requests           │ query API                        │ /assert route +
          │ (payload carries the      │ (read-unit, read-common-         │ driver appends
          │  distilled units/anchors/ │  material-for-source, …)         │
          │  edges — distillation is  │                                  │
          │  FOREIGN-SIDE)            │                                  │
  ┌───────┴───────────────────┐  ┌────┴──────────────────────────────────┴───────────┐
  │ adapters + drivers        │  │ projections / views                               │
  │  markdown_adapter         │  │  trail_view.clj (View-3 data layer over both     │
  │  transcript_adapter       │  │  kernels)                                        │
  │  git_spine.clj (commits → │  │  …future: river-page, morning answer            │
  │   canonical text + edges) │  └──────────────────────────────────────────────────┘
  │  sense-block distiller    │
  │  (block layer — planned,  │      kernel.clj = the written SPEC of the kernel
  │   CONTRACT v2)            │      shape these modules follow (5 instances)
  └───────────────────────────┘
```

## Module inventory (detail strictly ∝ read depth)

### object-container-module — the material kernel [PARTIAL read]

Its own docstring (:16-21): *"intentionally a new storage shape rather than a
copy of the old text kernel. Immutable source artifacts, derived units,
authored object containers, revisions, anchors, composition edges, and the
outline projection are separate rows with deterministic ids."*

What the rows say (all read, :38-176):

- **Sources are immutable and raw.** `SourceArtifactRow` holds
  `source-raw-text` + hash + format (:82-84); `SourceVersionRow` versions by
  ref (:86-88); transcript ingest tracks per-line status with byte offsets
  and line hashes (`TranscriptSourceLineStatusRow` :156-159) and per-file
  watch offsets (`TranscriptFileOffsetRow` :165-168).
- **Units are distilled, not authored.** `DerivedUnitRow` (:113-116):
  unit-kind, block-path, parent slot, anchor ref, derived text + hash, and
  the distiller's id + version. Distillation happens FOREIGN-SIDE — the
  request payload carries the units (`payload-derived-units` :620); the
  module materializes generically.
- **Addresses are spans.** `SourceAnchorRow` (:122-124): target, source,
  `start-offset`/`end-offset`, block-path.
- **Containment is edges.** `CompositionEdgeRow` (:126-129): parent/child
  slots with order keys — trees are computed, not nested storage.
- **Authorship is revisions.** `ObjectContainerRow` + `RevisionRow` with
  parent-revision chains (:104-111); `UnitGraduationRow` (:118-120) is a
  derived unit *graduating* into an authored, editable container.
- **Identity/idempotency:** `NativeIdentityClaimRow` + material fingerprints
  + import-completion rows (:99-102, :134-137) make re-imports idempotent.
- **Transcript projections:** per-conversation ordered entries with role +
  preview, tool-call index, audit entries, last-message rows (:139-154).
- **Redaction is a first-class policy** (`transcript-redaction-policies
  #{:standard}` :29-32) — stored text is post-redaction canonical.
- **Query API** (DECLS :2443-2501): `read-latest-source-by-ref`,
  `read-source-by-ref-version`, `read-unit`, `read-current-revision`,
  `read-common-material-for-source` → `CommonMaterialBundle
  {containers derived-units anchors edges}` (:67).
- Header warning (:1-5): re-read `build/object-container/PLAN.md` +
  `build/object-container-common-infra/PLAN.md` before modifying. [Those
  PLANs: UNREAD — next reads for this map.]

### object_container/markdown_adapter.clj — the distiller pattern [PARTIAL]

`markdown-distiller-id "markdown-block-v0"`, version 1 (:6-7). Pure fns:
line walker with running offsets (:24-40), heading/list grammars (:9-11),
`emit-block [state unit-kind …]` (:82), deterministic unit ids
`du:<object-key>:<distiller>:<block-path>` (:13-15). **This is the proof
that block-grain distillation is what the kernel was designed for** — the
sense-block distiller (CONTRACT v2) follows this file's shape.

### object_container/transcript_adapter.clj [DECLS only]

Fn inventory shows: content-blocks / tool-use-blocks / tool-result-blocks
extraction, role + preview, per-message container + revision + anchor +
composition-edge row builders, previous-message chaining, observation→import
request assembly. Grain today: message/tool-call containers. [Bodies unread
— P0 of the block package verifies source/anchor granularity here.]

### relation-kernel-module [DECLS + its contract §1-3]

D-004's edge kernel: one request depot (hash-by routing key), microbatch
topology, PStates for decisions-by-idempotency / edges-by-id /
edges-by-target / target descriptors / activity buckets (:616-656).
`RelationTargetRef.target-kind` is an OPEN keyword — new target kinds (e.g.
`:block`) need no schema change. Placement rationale and envelope style-gate
precedent live in `build/relation-kernel/CONTRACT.md`.

### git_spine.clj [PARTIAL: docstring + :24-61]

*"NOT a Rama module — no depots, topologies, or PStates. Pure adapter fns +
a sync driver + a transcript extractor + a replay fn, all over the EXISTING
object-container and relation-kernel public APIs."* The package-shape
precedent the block layer now follows. Verified 2026-07-09: requires
`relation-kernel :as rk` (:31); deterministic spine idempotency
`"spine:"+relation-id+":"+basis` (:24); relation-id includes the asserter
(:43); durable `data/relation-assert-log.ednl` replay log (:51). Content it
adds: commits as canonical-text material (→ OC) + parent lineage and
session↔commit joins as typed edges (→ RK).

### kernel.clj [header only]

The kernel-shape SPEC: prose theory (CT framing) + a Clojure-readable
`defkernel KERNEL-SHAPE` describing the pattern across "the 5 current
instances." [Body unread — high-value next read for this map.]

### trail_view.clj [header only]

Trail-view data layer (WP1 Phase B): View-3 projections over
object-container + relation-kernel. Binding contract in
`build/trail-view/CONTRACT.md`.

### text_kernel.clj [ns form only]

The OLD text kernel — object-container's docstring positions itself as its
intentional replacement shape. [Unverified beyond that; body unread.]

### dogfood/transcript.clj [DECLS + accessors]

Library (not a module): jsonl walking/parsing, redaction
(`redact-raw-string`, `redacted-preview-with-redactions` :411-452),
line-hash/file-id helpers, observation records with conv-key / message-uuid /
event-type / redacted-payload / byte-offset fields (:206-219). Reads
`:parentUuid` (:336); sidechain/promptId not captured (grep-verified
absence, 2026-07-09).

### dogfood/space.clj [PARTIAL: 1640-1685] + compute/llm [UNREAD]

The workspace RUNTIME family (2026-05 dogfood chapter): turns, context
bundles, llm runs, executors. Verified 2026-07-09: `$$artifact-graph` /
`$$artifact-graph-in` hold system-written WIRING between runtime objects
(thread→turn→bundle→llm-run edges, written inline as the topology
materializes space events, :1655-1674) — plumbing truth: no kinds beyond
from/to, no asserter, no lifecycle. This is why relations got their own
kernel (its CONTRACT §2 examined exactly this store); the plumbing graph is
projectable into relation-kernel later as `asserted-by :system` if a
one-graph view is ever wanted. compute/llm: memory-grade only until read.

## The truth-kind test (the goose-chase vaccine, 2026-07-09)

Every store holds a KIND of truth: **material** (what exists/was said — the
container kernel) · **asserted** (what someone claims — the relation kernel)
· **derived** (recomputable projections — trail_view, outline) · **plumbing**
(system wiring — space's artifact-graph). Redundancy = two stores holding
the SAME kind. Before any new package: name the truth-kind it adds, then ask
"does a store for that kind already exist?" — block-kernel v1 failed this
question (material store already existed); every other package in the record
passes it. Kernel packages add a missing truth-kind; content packages
(git-spine, the block layer) add material/assertions THROUGH existing
kernels via stateless adapters.

## Client layer [UNREAD — stubs from CLAUDE.md structure]

`electric_flow.cljc` (Electric reactive UI: layout, text, DOM) ·
`client/substrate/webgpu/renderer.cljs` (render loop, GPU pipeline) ·
`client/workspace/*` (runtime, editor, sidebar, shell, events, themes) ·
`client/workflows/*` (dg_flow, jit). Fill per real reads; the render-path
paradigm notes live in CLAUDE.md §Electric's Programming Paradigm.

## Next reads that would grow this map most

1. `kernel.clj` body — the written shape of every kernel.
2. `object_container/transcript_adapter.clj` bodies + `runtime.clj` — the
   import path end-to-end (also P0 of the block package).
3. `build/object-container/PLAN.md` — the kernel's own binding intent.
4. One client pass: `electric_flow.cljc` + renderer entry points.
