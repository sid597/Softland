# Block layer — architecture orientation (NON-BINDING; CONTRACT.md governs)

2026-07-09 · Fable, spec room · reflects Sid's placement ruling (Option A,
adapter shape). Deeper codebase context: `docs/architecture/MAP.md`.

## 1 · The shape: a distiller, not a module

The land has ONE material kernel — `object-container-module` — and blocks
are its atomic text containers. The block layer is a **new distiller adapter
+ driver** over the two existing kernels (the git-spine package shape, one
grain finer). This package adds NO module, NO depot, NO PStates.

```
              ══ the material kernel: object-container-module ══
   ┌─────────────────────────────────────────────────────────────────────┐
   │ SourceArtifactRow    raw text, immutable, hashed         (:83)      │
   │ SourceAnchorRow      start/end-offset + block-path       (:122) SPAN│
   │ DerivedUnitRow       unit-kind + distiller-id@version    (:113) BLOCK│
   │ CompositionEdgeRow   parent/child slots + order          (:126) TREE│
   │ RevisionRow          parent-revision chains              (:109) SUPERSEDES│
   │ transcript projections: per-message containers, tool-call index …  │
   └──────────▲──────────────────────────────────▲────────────────────────┘
              │ ingest requests (payload CARRIES │ query API: read-unit ·
              │ units+anchors+edges — distilling │ read-common-material-for-
              │ is FOREIGN-SIDE, by design :620) │ source (:2443-2501)
   ┌──────────┴─────────────────────┐  ┌─────────┴──────────────────────┐
   │ distiller adapters:            │  │ readers: benchmark CLI ·       │
   │  markdown-block-v0  (existing) │  │ reconciliation UI · later the  │
   │  transcript adapter (existing, │  │ marker — query topologies ONLY │
   │   message/tool grain)          │  └────────────────────────────────┘
   │  sense-block-v0     (THIS      │
   │   package: paragraph grain,    │──── mechanical edges, driver-side,
   │   thinking, river/debris,      │     idempotency keys ──▶ relation-kernel
   │   role≠actor, prod-events)     │     (targets = unit ids, :block kind)
   └────────────────────────────────┘
```

One ontology at every grain: markdown blocks, transcript messages, and
sense-blocks are all derived units with anchors into immutable sources —
"one land at every zoom" in the schema.

## 2 · The flow

```
 sources already in the kernel (transcript ingest landed Jun 7-8)
      │  read via query API
      ▼
 DRIVER + sense-block distiller (plain Clojure, foreign side)
   classify river/debris → resolve actor (role ≠ actor)
   → FREE CUT (pure fns, markdown_adapter conventions)
   → mint deterministic ids (du:… convention; span identity at anchors)
      │
      ├── derived-unit import requests ──▶ container kernel depot
      │    :append-ack; deterministic     → its topology materializes
      │    ids ⇒ re-runs converge           units/anchors/edges
      │
      └── mechanical edges ──▶ relation-kernel depot
           produced · grounds · assembled-from
           idempotency key = sha256(unit ∥ kind ∥ target)
```

Retry axes: driver re-run → deterministic ids converge (G4) · kernel
topologies → their own exactly-once/idempotency machinery, untouched ·
edge re-appends → relation-kernel idempotency PState dedupes.

## 3 · Reuse decisions

| existing thing | role in this package |
|---|---|
| `object-container-module` | THE home: stores sources, units, anchors, edges; its import path and query API are the only doors |
| `object_container/markdown_adapter.clj` | the pattern to follow (emit-block, offsets, `du:` ids, distiller versioning) |
| `object_container/transcript_adapter.clj` | already minted the message/tool-grain containers these blocks nest under |
| `dogfood/transcript.clj` | jsonl parse/redaction library if raw re-reads are needed |
| `relation-kernel-module` | as-is via its depot; open target-kinds take `:block` |
| `git_spine.clj` | the package-shape precedent (adapters + driver, no module) |
| `kernel.clj` | the kernel-shape spec both kernels follow |

## 4 · Is the block layer common underlying functionality?

The common layer is the **container kernel** — it already is the land's
material substrate. This package adds the missing GRAIN (engagement-grade
blocks) and the missing PROVENANCE (production events, delegation chain,
context-parents), after which: marks target unit ids, episodes group them,
briefings assemble them (assemblies re-enter as new sources), the benchmark
reads them. It earns that role when it survives the dual benchmark; nothing
else builds against it before then (D-001).
