# Shaper border receipt — 2026-08-30

**Headline.** At the shaper, HarfBuzz's own `shape()` is **48 ms** of a
**1,984 ms** `shape-line` pass over 330,254 glyphs (2.4%); the two named
WASM-border sites are **649 ms** (33% — 13× HarfBuzz), and the WASM crossings
themselves are only 96 ms of that: the cost is `js->clj` and the per-glyph
CLJS maps built on the JS side of the border. Derived to the founding corpus
(586,927 chars) the border is **≈1.1 s**, ≈29% of the ≈3.95 s residue outside
`union_bounds`; the whole provider derives to ≈3.5 s ≈ that residue's size.
**Verdict: material.**

## The decision this serves

Starter, step 1 (2026-08-30): "how many ms of shaping go to the WASM border —
(a) `getGlyphInfosAndPositions` → `js->clj` per-glyph maps, (b) the per-glyph
`glyphExtents` round trip — versus HarfBuzz's own `shape()` call?" Sid's word
on this headline opens step 2 (the flat-typed-array re-cut) or closes the lane.

## Capture conditions — attestation first (CONTRACT §3/§9/§10 standing rule)

- **Adapter (serialized first):** `isFallbackAdapter: false` (source
  `GPUAdapterInfo.isFallbackAdapter`) · vendor `amd` · architecture `rdna-3`
  · 23 features. The physical Radeon adapter on the proven headful road
  (Puppeteer 15.2.0 + system Chrome **150.0.7871.46**, `DISPLAY :0`,
  `--use-angle=vulkan --enable-features=Vulkan,WebGPU,UnsafeWebGPU
  --ignore-gpu-blocklist`). The measurement is CPU-side; the attestation
  classifies the receipt, it does not enter the numbers.
- **Machine:** `sid-System-Product-Name` · AMD Ryzen 9 9900X (24 threads) ·
  Linux 7.0.0-30-generic · Node v20.20.2 · viewport 800×601 @ DPR 1.046875.
- **Clock:** `performance.now()` resolution measured **0.1 ms**
  (`crossOriginIsolated: false`) — why per-call counters were ruled out
  (a 1 µs WASM crossing is invisible to a 100 µs clock) and the instrument
  is one bracket per whole-corpus pass. `window.gc()` exposed and called
  before every pass; `performance.memory` on.
- **Build:** shadow `:shaper-border-probe`, `:optimizations :simple` (the
  verifier's road). The founding capture ran the `:dev` build of the old
  layout; the shaper code (`shaper.cljs`) is unchanged in substance since
  (git: only the fold and docstring commits touch it).
- **Source HEAD:** `3872ff7` + the three uncommitted probe files (listed in
  `receipt.json → capture.gitDirty`); sha256 of every source input in
  `capture.sources`.
- **Provider:** `:harfbuzz/wasm` 14.0.0 · `ubuntu-sans-variable-0.869-wght400-wdth100`
  + fallback `noto-sans-regular-2.011` · features kern/liga/clig/calt ·
  variations wght 400 / wdth 100 · upem 1000.
- **Corpus:** the durable archive's 144 graduated blocks
  (`unit-graduations-by-id`, key `:current-content-text`) — **334,537 chars
  (UTF-16) · 3,511 lines · 330,254 glyphs shaped**, largest block 96,727
  chars, 15 empty. Both founding check blocks match exactly: `…3c6512e2`
  = 24,891 chars, `…3405ac6d` = 66 chars. The founding corpus's 36 machine
  blocks are NOT in the archive as blocks (extraction record:
  `corpus-extraction.md`); the corpus text is not banked here (data custody
  stays with the archive) — `corpus-manifest.json` carries its sha256 and
  every uid with its char/line count, so the run is reproducible from the
  archive.

## Method — an ablation ladder anchored at both ends

Nine replica rungs plus the two real ones, each = one `performance.now()`
bracket around one pass over all 3,511 lines (or 144 blocks); one warm-up
pass, then 3 timed passes; **median** reported, all three passes in
`receipt.json`. Rungs L0–L4e replicate `shaper.cljs` line for line on a
private font (same bytes, scale, variations; single face, LTR) so each site's
cost is a delta; L5 is the real `(:shape-line provider)`; L6 the real
`tl/layout` per block (wrap-policy `:none`). Every rung returns a glyph count
so the work cannot be dead-code-eliminated — all rungs count 330,254.

Replay: `clj -M:dev -m shadow.cljs.devtools.cli release shaper-border-probe`
then `DISPLAY=:0 node test/render_engine/profile_shaper_border.mjs
--corpus=<corpus.json> --out=<dir>` (corpus = the manifest's 144 blocks
re-extracted from the archive).

## The ladder (measured, medians of 3; 330,254 glyphs · 3,511 lines)

| rung | adds | ms | Δ ms | Δ µs/glyph | share of L5 |
|---|---|---:|---:|---:|---:|
| L0 | `hb.shape()` + buffer create/addText/props/destroy | 48.4 | 48.4 | 0.15 | 2.4% |
| L1 | + `buffer.getGlyphInfosAndPositions()` — raw JS objects (`hbjs.js:1186`; 4× `Object.defineProperty` per glyph) | 126.1 | 77.7 | 0.24 | 3.9% |
| L2 | + `js->clj :keywordize-keys` of that array (`shaper.cljs:178`) | 409.2 | 283.1 | 0.86 | 14.3% |
| L3 | + `font.glyphExtents(gid)` per glyph, raw (`hbjs.js:594`) | 427.5 | 18.3 | 0.06 | 0.9% |
| L4 | + `js->clj` of each extents object (`shaper.cljs:156–158`) | 697.4 | 269.9 | 0.82 | 13.6% |
| L4b | + `update :cluster` · `cluster-end-map` · the 11-key glyph map per glyph (`shaper.cljs:180–194`) | 1,052.5 | 355.1 | 1.08 | 17.9% |
| L4c | + `font.setVariations` per line (`apply-variations!`, `:262–267`) | 1,060.8 | 8.3 | 0.03 | 0.4% |
| L4d | + bidi `getEmbeddingLevels` · `codepoint-spans` · `faces-by-offset` · `logical-runs` · `getReorderedIndices`/`visually-order-runs` (`:102–154`, `:274–277`) | 1,266.6 | 205.8 | 0.62 | 10.4% |
| L4e | + `position-runs` (assoc `:position` per glyph) · `cluster-records` (group-by juxt per glyph, sort) · `(vec (mapcat :glyphs))` (`:221–260`, `:283–286`) | 1,877.9 | 611.3 | 1.85 | 30.8% |
| **L5** | **the real `shape-line`** (residual = two-face fallback check, tab runs, opts merge) | **1,983.8** | 105.9 | 0.32 | 5.3% |
| **L6** | **the real `tl/layout` per block** (layout-side work beyond shaping) | **5,755.0** | 3,771.2 | 11.42 | — |

Reading the two named sites:

- **(a) gip → `js->clj`** = L2 − L0 = **360.8 ms** (18.2% of `shape-line`;
  7.5× HarfBuzz). Of it, the crossing + hbjs object build is 77.7 ms; the
  `js->clj` walk is 283.1 ms.
- **(b) per-glyph `glyphExtents` round trip** = L4 − L2 = **288.2 ms**
  (14.5%; 6.0× HarfBuzz). The WASM call + hbjs object is 18.3 ms; the
  `js->clj` of the 4-field object is 269.9 ms.
- **Border sites (a)+(b) = 649.0 ms = 32.7% of `shape-line` = 13.4×
  `hb.shape()`.** Crossings alone 96 ms (2× HarfBuzz); conversions 553 ms
  (11.4×).
- Everything in `shape-line` that is not HarfBuzz — the JS/CLJS side of the
  border in full: conversions, per-glyph maps, run split, positioning,
  cluster records — is **1,935 ms = 97.6%**. The per-line `setVariations`
  hypothesis (coord-serial bump dropping HarfBuzz's advance cache) is dead:
  +8 ms.
- **Growth law (L6 per-block rows in `receipt.json`):** 16.65 µs/char mean
  over the 16 blocks ≥ 5,000 chars vs 17.27 µs/char over the 50 blocks of
  100–999 chars; the 96,727-char block runs at 17.7 µs/char. Linear — no
  hidden quadratic in the current shaped layout at this corpus.

## Derived to the founding corpus (586,927 chars) — derived, not measured

Scale = 586,927 / 334,537 = 1.7545 on the measured per-glyph rates (glyph/char
here 0.987 → ≈579k glyphs). Assumes the founding line-length distribution
resembles this corpus's; the 36 machine blocks (~252k chars) are absent, so
this is the human-block rate applied to the whole.

| | measured (334,537 chars) | derived at 586,927 chars |
|---|---:|---:|
| `hb.shape()` | 48 ms | ≈ 85 ms |
| border site (a) | 361 ms | ≈ 0.63 s |
| border site (b) | 288 ms | ≈ 0.51 s |
| **border (a)+(b)** | **649 ms** | **≈ 1.14 s** |
| `shape-line` (the provider) | 1,984 ms | ≈ 3.48 s |
| `tl/layout`, wrap `:none` | 5,755 ms | ≈ 10.1 s |

Against the founding capture (FOUNDING-EVIDENCE §4: `shaped_layout` 28.343 s
− `union_bounds` 24.390 s = **≈3.95 s residue**): the border derives to
≈29% of it; the whole provider derives to ≈88% — an upper-bound reading,
since the founding residue also held that era's other layout work. The
founding capture's own `text-gpu` bucket was CPU shape/pack/upload; nothing
here reopens that attribution.

## What stays unknown (queued, not answered here)

1. **The layout side is the larger pile and is unattributed by this
   receipt:** L6 − L5 = 3.77 s (65% of layout) — the per-glyph work in
   `layout.cljc` (`glyph-span-index`, the positioned/scaled glyph maps,
   `material-ink-bounds`, `union-bounds`, the plane transform). Structure
   checked in source; magnitude split NOT measured. The flat-typed-array cut
   re-keys layout to the new shape, so this pile is inside step 2's scope
   either way; a ladder over `layout.cljc` is the next receipt if the cut
   needs the split.
2. Derived-at-founding numbers are rate × count; wrapped machine blocks
   (block-greedy, I4's ≤2× reshape) would push shaping **up**, not down.
3. `:simple` vs the founding `:dev` build; same shaper source in substance.
4. Chrome's clock clamp means sub-rung splits (e.g. `defineProperty` vs the
   `for..of` inside hbjs) are not resolvable by this instrument; the rung
   deltas are the finest grain the receipt claims.

## Files

- `receipt.json` — the run (attestation first, capture, environment,
  provider, corpus stats, ladder with all passes, L6 per-block rows,
  console). sha256 `3761102b8f7ba9aacf41298690e77efd7dd007820f88405feea3d63f637ef3e0`.
- `corpus-manifest.json` — corpus sha256 + per-uid char/line counts (no
  text).
- `corpus-extraction.md` — the gatherer's extraction record (PState, keys,
  check blocks, the machine-block search).
- `SHA256SUMS` — over the files above; verify with `sha256sum -c`.
- Harness: `src/app/client/verifier/shaper_border_probe.cljs` ·
  `test/render_engine/profile_shaper_border.mjs` · shadow build
  `:shaper-border-probe`.
- Development runs before the banked one (not banked; same build, same
  corpus): `shape-line` 2,029.7 / 1,984.8 ms, `layout` 5,820.9 / 5,700.9 ms —
  within 3% of the banked medians.
