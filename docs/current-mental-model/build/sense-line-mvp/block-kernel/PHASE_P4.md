# Phase 4 — refinement (`refine!`, G7) + assembly (`assemble!`, G10)

Orchestrating session (Fable, max effort), 2026-07-09. Implementation phase of the
block-kernel work package. P0–P3 inherited GREEN; this phase adds the two composition
operations that ride the relation kinds P3a registered (`:refines`, `:assembled-from`).

**Result: G7 + G10 GREEN as IPC deftests; `mechanical-edge-gates` (P3) stays green.**
No stop-clause; no registry/OC-kernel edit; the occurrence question resolved additively
(edge `:note`), so no T14 escalation to Sid.

**Final receipt (all independently re-run this session):**
- `block_distiller_test` **17t / 828a / 0f** (P3 was 15t/770a; +2 gates G7/G10, +58a incl. idempotence).
- `relation_kernel_test` **2t / 222a / 0f** — the T11 check: `relation-kinds` green with BOTH
  packages' kinds coexisting (`:grounds :assembled-from :refines` + code-atom's `:requires :calls`).
- `object_container_test` **6t / 149a / 0f** (exercises the code-atom `imp:clj:` extract-object-key branch).
- ⚠ The kernel run had a **1-error FLAKE on the first pass** (a Rama `watchable_promise`
  background-thread async error — the documented contention/probe-await family); it did NOT
  reproduce on re-run (371a clean). NOT a P4 regression (P4 touched neither kernel file).

## Inherited-state correction (recorded — the baton was stale)

The opening prompt + STANDING said "P3 green 15t/740a" and treated the F1/F2 P1/P2
fixpass as *queued*. **The working tree had already advanced past that**: the F1+F2
fixpass is applied (`import-key` = `imp:tr:<ok>:sb:<hash>`; `free-cut-part` drops the
whole-message-coincident human sub; fixture gained event 10 `u-solo`). Baseline re-run
this session: **block_distiller_test 15t/770a, 0f** (not 740a). Followed the
work-package precedence rule — code + green suite are ground truth; the baton is a
pointer. My first read of `block_distiller.clj`/`_test.clj` was stale and was
re-fetched before editing.

## What was built (`block_distiller.clj`, §K + §L — additive, no existing fn touched)

- **§K `refine!`** (SPEC §5, gate G7). Demand-mints a FINER `DerivedUnitRow` inside a
  coarse block, over the SAME surface, at `:sub-span` = [start end) ABSOLUTE UTF-16
  offsets. Helpers: `refine-block-path` (id = pure fn of (surface, span) ⇒ §6.1
  identity-by-construction), `composition-edge-request` (shared with §L; carries the
  engagement/occurrence in the RelationEdge's declared `:note` — no new row).
  - Reads (foreign-client substrate, read-conversation-inputs class — NOT a G13 output
    path): `ocr/read-unit` (coarse form + source-id), `ocr/read-source-anchors` (the
    coarse span), `ocr/read-source` (surface raw-text + the row to re-declare).
  - Writes: ONE OC import (surface re-declared BYTE-IDENTICALLY — read-back-and-pass-
    through, because the OC source write is a `termval` overwrite keyed by source-id,
    object_container.clj:1950; + the finer unit + its anchor; DISTINCT import-key so the
    OC journal does not dedup it as a P1 replay), then ONE `:refines` edge finer→coarse
    (RK microbatch; test barriers on processed-count).
  - Guards (throw): coarse unit/anchor/surface absent; sub-span not a non-empty proper
    child (coarse.start ≤ start < end ≤ coarse.end, G11); sub-span splits a surrogate.
- **§L `assemble!`** (SPEC §8, gate G10). Mints a NEW `assembled` surface (source-format
  `:assembled`, non-`:markdown` ⇒ skips the OC hash-check; unit-kind `:assembled` is the
  SPEC §4.6 form) + a whole-span assembled block, with one `:assembled-from` edge per
  source block (assembled→source, position in `:note`). Transclude-by-default (§8.2): the
  source blocks are re-addressed via edges, never copied; their rows are untouched.
  Default assembled text = source `derived-content-text`s joined; `:text` overrides
  (the "new words written" case).

## Gates (physical PState readers, T7 validation-only; RK microbatch barrier)

- **G7** (`g7-refinement`): refines the first prose-para of a-1's text part (its surface
  ALSO holds a code-fence + 2nd prose-para) → finer unit present with the sub-span text
  + span + inherited form; the coarse unit, **a sibling unit on the same re-declared
  surface**, and **the surface itself** are byte-identical (the termval-overwrite drift
  risk, asserted dead); the `:refines` edge is in `$$relations-by-id` with the engagement
  in `:note`; a pre-existing `:references` mark on the coarse block persists (§5.2);
  **idempotent** — a re-refine resolves to the same finer-unit-id and re-writes nothing.
- **G10** (`g10-assembly`): assembles a thinking block + a sidechain prose block → new
  `assembled` surface + block; two `:assembled-from` edges with position in `:note`;
  source blocks byte-identical (re-addressed, not copied); **idempotent** — a re-assemble
  resolves to the same surface/unit id and re-writes nothing.

## Falsification self-review (CLAUDE.md protocol — author pass; the fresh-context layer rides the package gate)

- **Architecture.** Two driver fns; zero new topology/depot/PState; foreign appends into
  the EXISTING OC (stream) + RK (microbatch). `/rama-pitfalls` walked before coding:
  READY-TO-CODE (event-boundary N/A; retry-idempotent ids; ack levels correct; du:/src:tr:
  routing verified at object_container.clj:331/288).
- **Failure modes attempted.** (a) surface drift on re-declare → asserted byte-identical
  (G7). (b) sibling units disturbed by re-declaring their surface → asserted byte-identical
  (G7). (c) finer-id collision with a P1 id → `refine:` segment is disjoint from the
  `%06d:…` and `pending-result:` shapes. (d) re-run duplication → idempotence gates (both).
  (e) surrogate-splitting sub-span → refine! throws (guard). (f) empty assembly → assemble!
  throws. (g) OC mint not landed before the edge → `await-object-container-decision` before
  the RK append.
- **Writers/readers/clearers.** refine!/assemble! WRITE nothing directly (T7): OC rows via
  the import topology (owner), RK edges via the assert topology (owner). Refinement/assembly
  only ADD (SPEC §5.2/§14) — nothing clears; coarse/source rows are never in the payload.
- **Async ordering.** OC import awaited (decision `:accepted`) → the finer/assembled block
  is committed + readable → THEN the RK edge is appended, so its endpoint is a real block
  (G7 "both real"; G10 assembled→source both real). Older-callback-overwrites-newer: N/A —
  deterministic keys, `termval` writes are idempotent.
- **Error-path cleanup.** No locks/in-flight flags to release; a mid-op throw leaves only a
  committed OC import (idempotent) with no edge — a benign partial (a real block with no
  refines/assembled-from edge), re-runnable to completion. `close-distiller-runtime!` in the
  test `finally`.
- **Open doubts (non-blocking, with falsifiers).**
  1. **Assembly home = first block's object-key.** v0 co-tenants the assembled surface under
     `(extract-object-key (first block-ids))`. Cross-object assembly still works (RK target
     copies hop per endpoint); the surface just lives on one chat's task. Falsifier: assemble
     blocks from two object-keys and read the `:assembled-from` edges from both `to` sides.
  2. **Occurrence = edge `:note` position, not a first-class row.** Satisfies G10 "re-addressed
     not copied" for v0 (opening-prompt recommendation; a row = T14 stop-clause, deferred to
     the marks-layer, CONTRACT §10). Falsifier: a consumer that needs to enumerate occurrences
     by `(block, assembly, position)` would need the marks-layer extension.
  3. **Assembled surface `document-container-id` is synthetic** (`chat-message-id` over the
     assembled-source-id hash) and references no existing container — unvalidated by the import
     (empty `:object-containers`), unread in v0. Falsifier: a reader that traverses surface →
     container would find a dangling ref; a real container mint is a P5+/marks concern.
  4. **`refine!`/`assemble!` return maps, not the bare id** the PLAN §2 signature names
     (`→ finer-unit-id` / `→ assembled-source-id`). The map is a superset (`:finer-unit-id` /
     `:assembled-source-id` are keys) — chosen so callers/tests get the relation-id + edge-count
     for the microbatch barrier. A letter-deviation from PLAN §2, spirit-held.

## Definition of P4 done (opening prompt)

G7 + G10 green as IPC deftests (RK microbatch barrier + physical OC/RK readers): **MET.**
P3 `mechanical-edge-gates` stays green: **MET** (in the same suite). CODE UNCOMMITTED
(Sid's word). The package's batched fresh-context falsification + gate review (with
G12/G13) rides the package close, after P5 (delivery-mode ruling: falsification batched at
wave end, not sprinkled per-phase).
