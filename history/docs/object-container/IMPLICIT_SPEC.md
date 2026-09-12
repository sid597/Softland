# Implicit Spec — Object Container kernel, Slice 1

<!-- Phase 0. Requirements analysis only — NO PState/depot/topology design (that is Phase 1). -->

Source spec: `docs/current-mental-model/architecture/object-container-spec.md`. This slice realizes the
import → derive → outline → graduate-on-edit spine for **one** source format (markdown), in the existing
kernel contract language (`core.clj`: `action-request` → decision → `event` → materialization →
projection; `target-kinds`).

## Slice 1 scope (committed)

**In:** ingest a markdown source; preserve raw immutably; derive a block tree; render an outline; edit a
derived block → graduate it to a durable, versioned container; edit an already-durable container → new
revision.

**Out (deferred, must not be designed in):** layers / base-active / promotion-granularity; situate /
relationship discovery; visibility promotion / global pool / semantic search; canvas + other interpreters
(outline only); non-markdown distillers; `RelationEdge` (only `CompositionEdge`); `ViewState`.

**Entities:** `SourceArtifact`, `DerivedUnit`, `ObjectContainer`, `Revision`, `CompositionEdge`, `SourceAnchor`.

## Operations

### W1. `source/ingest` — import a markdown file
- **Inputs:** raw text, logical ref (path), content hash, actor. Produces a `SourceArtifact` (immutable,
  content-addressed); a document-level `ObjectContainer`; one `DerivedUnit` per markdown block
  (heading/paragraph/list-item) via the markdown distiller `markdown-block-v0`; `CompositionEdge`s
  (document→block, block parent/child/order); a `SourceAnchor` per unit (→ source span).
- **Latency:** hundreds of ms acceptable. Not a hot path — a human imports a file/folder.
- **Throughput:** driven by number of files imported and blocks-per-file. Bursty (folder import =
  many files at once).
- **Consistency invariants:**
  - The raw bytes are preserved exactly and never mutated by any later operation.
  - Idempotent on `(source/ref, content/hash)`: re-ingesting identical content creates **no** new
    SourceArtifact, document, units, or edges (dedupe).
  - Every derived block is reachable from the document container via composition edges; sibling order is
    preserved exactly as in the source.
  - Each `DerivedUnit` carries a `SourceAnchor` (the span it came from) and the distiller id/version.
- **Data growth & scale:** **write volume scales with file size** — a large markdown file is many blocks,
  all produced by one logical import. This is the dominant scale risk: a single import must not assume a
  small, fixed number of derived units. (Bounded by the imported file, which the app controls, but a
  10 MB file is ~10^4–10^5 blocks.) Outline reads later dominate: **range access over a document's blocks
  in order**, not random point lookups.
- **Concurrency:** two ingests of the same `(ref, hash)` → exactly one result (dedupe wins, no
  duplicates). Two ingests of the same ref with **different** content → two distinct SourceArtifact
  versions (different hashes), grouped by logical ref; neither clobbers the other.
- **Edge cases:** empty file → SourceArtifact + document container + **zero** units, empty outline.
  Single-line file → one unit. File with only whitespace → preserved as raw; zero/one trivial unit.
  Re-import after external file change (same ref, new hash) → new SourceArtifact version; must **not**
  overwrite any block the user has already graduated (see drift, W2).

### W2. `object/edit` — edit a block (graduate-or-revise; the touch that promotes)
This single operation behaves differently by target state — the core of the slice.
- **Inputs:** target id (a `DerivedUnit` **or** an `ObjectContainer`), new content, actor, request id.
- **Behavior — target is a not-yet-graduated `DerivedUnit`:** **graduate** it — create a durable
  `ObjectContainer` (kind `:text-block`) with a first `Revision` holding the edited content; mark the
  unit graduated with a pointer to the new container; preserve the unit's `SourceAnchor` on the container.
- **Behavior — target is an already-durable `ObjectContainer`:** **revise** — append a new `Revision`,
  advance the container's current-revision pointer. No second container is created.
- **Behavior — target is a `DerivedUnit` already graduated:** treat as a revise on its container (do
  **not** create a second container).
- **Latency:** single-digit ms desired — this is interactive editing.
- **Throughput:** driven by user edit rate (low, human-paced).
- **Consistency invariants:**
  - A given `DerivedUnit` graduates **at most once** (idempotent graduation; ordering/dedupe must
    guarantee this under concurrency and retry).
  - Editing a block **never** mutates the `SourceArtifact` — `read-source` is unchanged after any edit.
  - After graduation, the outline shows the **container's current revision**, not the stale
    source-derived text.
  - The graduated container retains its `SourceAnchor` (provenance back to the source span survives).
  - Re-running the distiller (W1 re-ingest) over a span whose unit has graduated **must not overwrite**
    the container; it may record drift (deferred), never clobber authored content.
- **Data growth & scale:** revisions are append-only and grow with edit count per container (bounded by
  user activity). Access pattern: usually only the **current** revision is read (outline render); full
  revision history is a rarer, ranged read.
- **Concurrency:** two concurrent edits to the same not-yet-graduated unit → exactly one graduation; the
  later one must observe the graduated state and become a revise (no duplicate container, no lost edit).
  Ordering: an older edit callback must not overwrite a newer revision.
- **Edge cases:** edit to empty string → graduates to a container with empty content (an empty block is
  still a valid block). Edit a non-existent target → reject. Duplicate request id → idempotent (one
  graduation / one revision, not two). Edit a block whose document/source was deleted → out of scope
  (no deletion in slice 1).

## Reads (callable in ANY state, any time)

- **R1 `read-source`** — the immutable raw of a SourceArtifact (by id or by `(ref, hash)`).
- **R2 `read-outline`** — the ordered block tree of a document: for each node, its content (current
  revision if graduated, else source-derived text) + composition/order. Must be efficient over many
  blocks (range access).
- **R3 `read-container`** — a durable container + its current revision.
- **R4 `read-unit`** — a derived unit (content from its source span) + whether it has graduated (+ the
  container id if so).

## Entity State × Write Matrix

### SourceArtifact — states: `does-not-exist`, `ingested` (immutable)
- **`does-not-exist` × `source/ingest`** → becomes `ingested`.
  - R1 read-source: returns the raw bytes/text + ref + hash. (The preserved original.)
  - R2 read-outline: returns the document's block tree (derived content for all blocks).
  - R3 read-container: returns the document container (no text-block containers yet).
  - R4 read-unit: each block returns its source-derived content, `graduated? = false`.
- **`ingested` × `source/ingest` (same ref+hash)** → no change (dedupe).
  - R1: same raw as before. R2: identical tree (no duplicate blocks). R3/R4: unchanged.
- **`ingested` × `source/ingest` (same ref, new hash)** → a **new** SourceArtifact version exists.
  - R1: both versions are readable by id; by ref, the latest hash resolves (logical-ref grouping).
  - R2: the new version's outline; previously graduated blocks of the old version are untouched.
- **`ingested` × `object/edit`** → SourceArtifact unchanged (edits never touch source).
  - R1: returns the original raw, byte-identical. (Core invariant.)

### DerivedUnit — states: `does-not-exist`, `derived`, `graduated`
- **`does-not-exist` × `source/ingest`** → becomes `derived`.
  - R4 read-unit: source-derived content, `graduated? = false`, carries SourceAnchor.
  - R2 read-outline: shows this block with derived content.
- **`derived` × `object/edit`** → becomes `graduated`; a container + first revision appear.
  - R4 read-unit: `graduated? = true` + the container id; content now reflects the container's revision
    (read-unit redirects to the durable content so callers never see stale text).
  - R2 read-outline: shows the **edited** content (from the container), not the source-derived text.
  - R3 read-container: the new container + its first revision is returned.
  - R1 read-source: unchanged.
- **`graduated` × `object/edit`** → treated as a revise on the container (NOT a second graduation).
  - R4: still `graduated? = true`, same container id; content = container's new current revision.
  - R3: container's current revision advanced.
  - R2: shows the newest revision content.
- **`derived` × `source/ingest` (re-ingest same ref+hash)** → stays `derived` (dedupe, no duplicate unit).
- **`graduated` × `source/ingest` (re-ingest)** → stays `graduated`; the distiller must **not** overwrite
  the container.
  - R4: still graduated, container intact. R2: still shows authored content. (Drift recording deferred.)

### ObjectContainer — states: `does-not-exist`, `durable`
(document containers exist from ingest; text-block containers exist only after graduation.)
- **`does-not-exist` × `source/ingest`** → a `:document` container becomes `durable`.
  - R3 read-container: the document container (title/ref), current revision = the document-level handle.
  - R2 read-outline: the document is the root of the tree.
- **`does-not-exist` × `object/edit` (graduating a unit)** → a `:text-block` container becomes `durable`.
  - R3: the new container + first revision. R4: the source unit now points here. R2: edited content shown.
- **`durable` × `object/edit`** → new revision; current-revision pointer advances.
  - R3: returns the new current revision. R2: shows newest content. Prior revisions remain readable
    (append-only history; no deletion).
- **`durable` × `source/ingest`** → unchanged (re-distill never overwrites a durable container).

### Revision — states: `does-not-exist`, `exists` (append-only)
- **`does-not-exist` × `object/edit` (graduate)** → first revision `exists`.
  - R3 read-container: current-revision = this revision; content = edited text + hash + parent-rev = nil.
- **`exists` × `object/edit` (revise)** → a new revision `exists`; previous remains.
  - R3: current-revision = newest; `parent-revision` chains to the prior. History is range-readable.
- **`exists` × `source/ingest`** → unchanged (source ingest does not write container revisions).

### CompositionEdge — states: `does-not-exist`, `exists`
- **`does-not-exist` × `source/ingest`** → edges `exist` (document→block; block parent/child/order).
  - R2 read-outline: traverses these edges to build the ordered tree; order field preserved.
- **`exists` × `object/edit`** → composition unchanged on graduation: the block keeps its position; the
  edge now resolves to the container instead of the derived unit (identity is stable across graduation).
  - R2: same tree shape, same order; the edited node's content changed, not its place.
- **`exists` × `source/ingest` (re-ingest same)** → unchanged (dedupe; no duplicate edges).
