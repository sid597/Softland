# W2-B + T1 — landed receipts

**Landed:** W2-B 2026-08-02 · T1 2026-08-03

**Code commits:** `8ac1b04` (`render-engine: land W2-B ordered scene executor`,
11 files) → `52d123f` (`render-engine: land T1 proportional shaping`, 29
files; parent is the W2-B commit). Docs never mixed in; nothing pushed.

**Contracts:** `W1.md` O/G/M/T/C

**Gate:** `npm run verify:render-engine` — expected RED, with the pre-existing
MSDF counterexample preserved

This is the settlement record for the W2-B ordered scene executor and the T1
proportional-shaping package — the last two committed waves of Package 1. It
is not a campaign rescoping document. It carries implementer receipts AND an
independent full replay by the settling session (2026-08-03, at `52d123f`,
which contains both commits). No golden was updated; no scene-order or
transform truth was added beyond the contracts; no Studio custody was entered;
Slug-direct MSDF retirement was neither selected nor enacted.

## 1. W2-B — the one ordered scene executor

`src/app/client/substrate/scene_tape.cljc` (new, 513 lines) is the ordered
plan + registered draw batches — data and a loop, not a framework. The five
existing families (rect, shadow, MSDF, Slug, clip) migrate through the seam;
the central frame body carries **zero** hand-positioned family draw branches.
Paint runs the tape forward; pick runs it in exact reverse. The tagged
linear-premultiplied scene-color seam is landed **default-OFF**.

Its executable fence (`test/render_engine/verify_scene_tape_fence.mjs`,
replayed by this session) reports: 5 families registered, 0 central draw
branches, reverse-pick contract present, linear-premultiplied default-off,
and a seeded central-branch violation correctly rejected (self-test). Zero
production failures.

W2-B properties were re-verified intact at the T1 HEAD rather than by a
separate checkout replay of `8ac1b04`; every receipt below therefore covers
both packages at once.

## 2. T1 — real proportional shaping behind the T0 seam

- `text_shaper.cljs` (new) owns HarfBuzz WASM font-program interpretation and
  bidi/fallback run construction: glyph IDs, UTF-16 cluster ranges, advances,
  offsets, extents, directions, and font provenance in font-unit space.
  Requested `wght`/`wdth` are applied to the actual HarfBuzz font state
  (`.setVariations` at face creation and per-shape), not recorded as metadata.
- `text_layout.cljc` remains the sole material-space placement owner,
  producing the one immutable versioned layout result consumed by measure,
  wrap, glyph paint, caret, selection, clip, and hit testing.
- Live faces: Ubuntu Sans Variable (primary) + Noto Sans (fallback), declared
  in `resources/public/fonts/manifest.json`. Kerning, ligatures, combining
  clusters, bidi runs, fallback, tabs, explicit newlines, and variable axes
  flow through the same result. A cluster crossing a syntax-style boundary is
  assigned to exactly one paint range (no double-painted ligatures).
- `renderer.cljs` resolves positioned glyphs before backend selection;
  `paint-msdf-line` and `paint-slug-line` perform coverage-resource lookup and
  quad construction only — neither owns layout, advances, kerning, or private
  metrics (fence-enforced). Translated rect trees reuse carried shaped results
  through clipping.
- Multi-face MSDF and Slug resources cover shaped glyph indices for both
  faces; `src-build/build/slug_font.clj` compiles glyph-index outlines
  (including GSUB-produced glyphs) and packs faces without glyph-ID collision.
  MSDF remains the live default paint road; Slug remains an interchangeable
  consumer with complete resources.
- Pinned runtime deps (exact, no ranges): `harfbuzzjs 0.10.3` (HarfBuzz
  14.0.0 per its own receipt) · `bidi-js 1.0.3`.

### Shaping corpus (real HarfBuzz, replayed)

`office` → 4 shaped glyphs (ligature) · `AV` → 52 font-unit kerning delta ·
`é` → one cluster · RTL cluster order `[3, 2, 1, 0]` · fallback boundary
`U+0250` (absent in Ubuntu Sans, present in Noto Sans) · variable-width delta
648 font units · explicit tab + newline controls. Font digests: primary
SHA-256 `37e90bba…54fe40`, fallback `89c3c497…2909d` (full values in the
`[T1-SHAPER]` receipt line). Both MSDF and Slug resource sets contain every
shaped primary and fallback glyph used by the corpus.

## 3. Machine receipts — independent replay, 2026-08-03

Replayed whole by the settling session at `52d123f`
(`npm run verify:render-engine`), plus suite runs:

- focused Contract-T tests: **6 tests, 32 assertions, 0 failures/errors**;
- face/scene seam superset (12 namespaces: workspace block-edit/ground-edit/
  scene-store/text-layout/trail-face + face primitives/assembly/projection/
  arsenal/gate-fixes/integration/transcription): **135 tests, 1,911
  assertions, 0 failures/errors** — subsumes the implementer's 64t/695a set;
- T1 independent-metrics fence: **PASS** — 19 owners / 7 files; seeded
  `count × advance` consumer rejected; zero production failures;
- W2-B scene-tape fence: **PASS** — 5 families / 0 central branches; seeded
  central-branch violation rejected;
- compiler: **114 files, 0 warnings**;
- deterministic replay: **21/21**; prior golden comparison: **21/21
  byte-identical**;
- browser Contract-T: all seven readers report layout ID **`t1/654248430`**
  (RTL + fallback + tab exercised, 15 glyphs/15 clusters, production
  renderer, real WebGPU via SwiftShader);
- browser variable-axis receipt: `wdth=75` vs `wdth=125` changes material
  advance by **12.312**;
- SDF pick parity: **7/7** `decisive-parity`;
- Slug pick parity: **7/7** `decisive-parity-with-declared-boundary-ties`;
- MSDF: **7/7** `decisive-mismatch` — exactly **47 decisive mismatches + 2
  boundary ties per regime**, preserved;
- current-product bounds-pick divergence sentinels: **7/7**;
- Q5 affine/raster receipt: **PASS**; Q8 transport receipt: **PASS**;
- classification: `candidate-pick-parity-failure`, exit **1** — the required
  RED state; environment fingerprint `e79490f8882cd785f32b5bb82cadd425dc90f2d7616cc9f0debf8a0f1c476282`;
- receipt: `target/render-verifier/receipt.json` (generated, not committed).

The RED exit remains a campaign obligation (MSDF geometry vs outline truth),
not a W2-B/T1 regression and not authority to update goldens.

## 4. State after this settlement

Package 1's committed waves (W0 · W1 · W2-A · W2-B · T0 · T1) are all landed
in code. **T1's felt dividend — Sid working his real land in a proportional
font — is open at Sid's wear**; this receipt is machine truth only, not lived
approval. Per `ENGINE.md` §0 hard joins, the machine front moves to Package 2
(image atom first: decode · upload · color management · mipmaps · atlas/
bind-group strategy, registered through the W2-B seam — never a central
branch). Package-2 **felt** activations stay joined behind the durable
authoring artery — studio custody at the capability level, the one decision
reserved to Sid. Slug-direct MSDF retirement remains a separate
pre-registered road receipt (real-hardware dense-small-text fragment cost ·
tiny-text quality · implementation provenance).

## 5. Next-session starter — Package 2 opening

> Boot `ENGINE.md` §0 (Package 2 + hard joins) + `W1.md` Contracts O/G/M/C,
> `W2-A-T0.md`, and this receipt. Open the image atom: decode/upload/color-
> management/mipmap/atlas infrastructure, REGISTERED through the W2-B scene
> tape as pipelines/batches — adding it must not add a hand-positioned branch
> to the central frame body (the W2-B gate sentence). Unconditional geometry
> declarations per Contract G; straight → linear-premultiplied color per
> Contract C. No felt gate until the durable authoring artery is ambient
> (studio-custody capability ruling, reserved to Sid). Replay
> `npm run verify:render-engine`; Q5, SDF, and Slug may not regress and the
> MSDF 47-mismatch counterexample must remain visible. Do not update goldens,
> shape new text roads, retire MSDF for Slug-direct, push, or mix code and
> docs commits.
