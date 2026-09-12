# W2-A + T0 — landed receipts

**Landed:** 2026-08-02

**Code commit:** `7651c8d` (`render-engine: land W2-A affine transport and T0 layout seam`)

**Contracts:** `W1.md` O/G/M/T/C

**Gate:** `npm run verify:render-engine` — expected RED, with the pre-existing
MSDF counterexample preserved

This is the settlement record for the W2-A GPU-structural spine and the T0
CPU text-layout lane. It is not a campaign rescoping document. W2-B remains
the next package; T1 remains the shaping package after it. Slug-direct MSDF
retirement remains a separate pre-registered road receipt.

## 1. W2-A — affine and transport migrated once

The existing container family now has one canonical affine representation,
`[a b c d tx ty]`, with legacy translation/scale accepted only at the input
boundary. Nested transforms compose canonically; bounds project all four
corners; picking uses the inverse transform and fails closed for singular
matrices.

The same transform crosses the complete W2-A path:

- registration assigns a compact stable transport slot independently of the
  sparse semantic container ID;
- nested stack-path context and affine screen bounds enter scene ordering and
  culling;
- text clipping remains local to the atom before the same affine projection;
- paint reads the affine from one read-only storage buffer for rect, shadow,
  MSDF, and Slug paths;
- picking inverts the same affine used by paint.

The old 1,024-entry uniform-array ceiling and hard throw are gone. The single
replacement transport is a 32-byte storage-buffer record with a declared
16,384-slot capacity. Freed slots are reused without coupling capacity to
semantic ID magnitude.

This package carries the canonical nested `stack-path` that Contract O needs,
but does **not** claim that W2-B's ordered scene tape is already live. The
current family-specific frame assembly and layer sort are still the migration
tail. W2-B must replace them with the one forward-paint/reverse-pick tape and
make registration, geometry, and color citizenship code-real for every
existing family before a new atom is admitted.

### Exact geometry obligations

- Q5's two rotated-rect pixels are now analytically inside and visibly
  covered: `(61,43)` maps to
  `[-14.202831971806303,-14.992650332100617]`; `(66,84)` maps to
  `[14.202831971806303,14.992650332100617]`; both render byte `131`.
- The conservative half-pixel hull repairs the raster-boundary omission while
  preserving the prior fragment-AA rule and every existing golden byte.
- Q6 remains the settled byte-128 tie and is not promoted into an inside vote.
- The MSDF counterexample remains 47 decisive mismatches per regime. No
  threshold, classification change, or golden update hides it.

### Q8 transport receipt

| Entries | Bytes | Highest slot | Highest sparse semantic ID |
|---:|---:|---:|---:|
| 1,024 | 32,768 | 1,023 | 17,391 |
| 4,096 | 131,072 | 4,095 | 69,615 |
| 16,384 | 524,288 | 16,383 | 278,511 |

The sparse-ID attack uses stride 17, proving that semantic identity no longer
indexes or prices the GPU transport.

## 2. T0 — one legacy-compatible layout result

`text_layout.cljc` is now the only owner of the existing text metrics. It
produces one immutable, versioned layout result containing the source, font,
shaping/space tags, constraints, metrics, lines, runs, clusters, clip plan,
and receipts. Indices are explicitly tagged as UTF-16 code units.

Measure, wrap, paint, caret, selection, clip, and hit-test are readers of that
one result. The cited owners in `rect_tree.cljc`, `face_primitives.cljc`,
`combined_text.cljs`, and `runtime/mouse.cljs` route through the seam; the
renderer text paths and bracket hit path do too. The legacy advance and snap
rules are centralized rather than independently recomputed.

An executable static fence inventories 16 production owners across five
files, verifies their delegation to the layout seam, rejects private metric
math, and proves itself with a seeded `count × advance` private consumer. The
fence runs first in `verify:render-engine`.

T0 deliberately contains no shaper. Its provider is tagged `legacy-v0`; T1
replaces positioned-glyph production behind this already-live contract.

## 3. Machine receipts

Focused Clojure receipts:

- layout + face primitives + scene store: **42 tests, 358 assertions, 0
  failures, 0 errors**;
- anatomy/assembly integration: **24 tests, 288 assertions, 0 failures, 0
  errors**.

Mandatory verifier replay:

- T0 independent-metrics fence: **PASS** — 16 owners / 5 files; seeded private
  consumer rejected; zero production failures;
- compiler: **106 files, 2 compiled, 0 warnings**;
- deterministic replay: **21/21**;
- prior golden comparison: **21/21 byte-identical**, zero mismatches;
- SDF: **7/7 pass**, zero decisive mismatches;
- Slug: **7/7 pass**, zero decisive mismatches;
- MSDF: **7/7 fail**, 47 decisive mismatches and 2 ties per regime;
- divergence sentinels: **7/7**;
- Q5 affine/raster receipt: **PASS**;
- Q8 capacity/transport receipt: **PASS**;
- classification: `candidate-pick-parity-failure` — the required RED state;
- environment fingerprint:
  `9213da9a74738c3397e99ef62b08c3833cbcaab9b6124d8255c1a4616e58a03c`;
- receipt: `target/render-verifier/receipt.json` (generated, not committed).

The verifier exits non-zero by design because MSDF parity is still falsified.
That RED is a campaign obligation, not a W2-A/T0 regression and not authority
to update the goldens.

## 4. Next-session starter — W2-B

> Boot `ENGINE.md` §0 + §12 Q2/Q5/Q6/Q8, `W1.md` Contracts O/G/M/C,
> and this receipt. Implement W2-B only: make the one ordered scene tape
> code-real for the existing rect/shadow/MSDF/Slug/clip families; paint it
> forward and pick it in exact reverse; move family registration into the
> declared executor; require unconditional geometry declarations; and land
> the tagged linear-premultiplied scene-color seam default-off/zero-diff.
> Consume W2-A's compact affine slots and canonical nested stack paths—do not
> create a second transport or family-specific order truth. Admit no new atom.
> Replay `npm run verify:render-engine`; Q5, SDF, and Slug may not regress, and
> the MSDF 47-mismatch counterexample must remain visible. Do not update
> goldens, enter Studio custody, shape text, retire MSDF for Slug-direct, push,
> or mix code and docs commits. T1 follows W2-B.
