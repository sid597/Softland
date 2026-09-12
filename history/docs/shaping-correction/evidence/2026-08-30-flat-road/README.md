# Flat road receipt — 2026-08-30 (the shaper-border step-2 replay)

**Headline.** Same corpus, same machine, same road, same instrument as the
step-1 receipt (`../2026-08-30-shaper-border/`), one replay after the flat
road landed: the real `shape-line` pass over 330,254 glyphs went from
**1,983.8 ms → 187.8 ms** (10.6×; 6.01 → 0.57 µs/glyph) and the real
`tl/layout` per block from **5,755.0 ms → 557.4 ms** (10.3×; 17.43 → 1.69
µs/glyph). HarfBuzz's own `shape()` is still 48.9 ms — it is now **26% of
shaping** instead of 2.4%. The layout-side residue (L6 − L5) went
3,771 → 370 ms. The nine replica rungs (the old code, copied line for line
into the probe) reproduce the step-1 numbers within noise, so the machine
and the clock are the same; only the real roads moved. Both pre-registered
predictions of `../../SHAPER-BORDER.md` §3 hit: P1 (≤ 400 ms) and P2
(≤ 1,500 ms); P3 (bit-identical planes and bytes, goldens unchanged) held in
the verifier and the JVM suites before this replay ran.

## Sid's bar, carried

Sid, 2026-08-30, in chat after the step-1 receipt review, verbatim (recorded
by a parallel session, NOW.md commit `e24fb19`): **"12seconds is too much
where did that no. come from??? it should be as small as possible under 500ms
i would say"**. The 12.0 s was the old contract author's bar for a surface
that no longer exists; Sid's bar is whole-system, under 500 ms. Against it:
the text organ's whole-corpus CPU at founding scale (≈579k glyphs) derives
to ≈1.3 s now (was ≈10.1 s) — still over the bar on its own. Text meets a
500 ms whole-system bar only per visible glyph (the viewport-residency
thread); what this atom changed is the rate, 17.4 → 2.26 µs/glyph, so under
500 ms the organ now affords ≈220k glyphs where it afforded ≈29k.

## Capture conditions — attestation first

- **Adapter (serialized first):** `isFallbackAdapter: false`
  (`GPUAdapterInfo.isFallbackAdapter`) · vendor `amd` · architecture
  `rdna-3` · 23 features. Same physical Radeon, same headful road: Puppeteer
  15.2.0 + system Chrome **150.0.7871.46**, `DISPLAY :0`, headless false,
  `--use-angle=vulkan --enable-features=Vulkan,WebGPU,UnsafeWebGPU
  --ignore-gpu-blocklist`. The measurement is CPU-side; the attestation
  classifies the receipt, it does not enter the numbers.
- **Machine:** `sid-System-Product-Name` · AMD Ryzen 9 9900X (24 threads) ·
  Linux 7.0.0-30-generic · Node v20.20.2 · viewport 800×602 @ DPR 1.046875.
- **Clock:** `performance.now()` resolution 0.1 ms; `window.gc()` exposed
  and called before every pass; `performance.memory` on. One bracket per
  whole-corpus pass; one warm-up, three timed passes, **median** reported.
- **Build:** shadow `:shaper-border-probe`, `:optimizations :simple`, built
  from the working tree at `204cc34` + the flat road (uncommitted at capture;
  `receipt.json → capture.gitDirty` lists the 14 paths; sha256 of every
  source input in `capture.sources`). The probe itself changed by two lines:
  its L5 rung reads `:glyph-count` off the shaped line instead of counting
  a `:glyphs` vector.
- **Provider:** `:harfbuzz/wasm` 14.0.0 · `ubuntu-sans-variable-0.869-wght400-wdth100`
  + fallback `noto-sans-regular-2.011` · features kern/liga/clig/calt ·
  variations wght 400 / wdth 100 · upem 1000.
- **Corpus:** the step-1 corpus exactly — sha256
  `edfed32c1a0bbf9d4f73e00c87f945d7b001bf622cae1564086890fc530ebd63`
  (`../2026-08-30-shaper-border/corpus-manifest.json`): 144 graduated
  blocks, 334,537 chars (UTF-16), 3,511 lines, 330,254 glyphs shaped; text
  not banked (data custody stays with the archive).
- **First attempt aborted, not banked:** a wrapper timeout of 300 s killed
  the runner during rung L4d while another process was still winding down;
  the banked run below is the immediate re-run alone on a quiet machine
  (whole ladder 31 s wall).

## The ladder — before (step 1) and after (this replay)

Medians of 3; 330,254 glyphs · 3,511 lines. Rungs L0–L4e are the probe's
private replica of the OLD shaper, unchanged — they are the control.

| rung | what | before ms | after ms | after passes |
|---|---|---:|---:|---|
| L0 | `hb.shape()` + buffer create/addText/props/destroy | 48.4 | 48.9 | 48.9 · 48.4 · 49.0 |
| L1 | + `getGlyphInfosAndPositions` raw JS objects | 126.1 | 119.7 | 119.1 · 120.0 · 119.7 |
| L2 | + `js->clj` of that array | 409.2 | 407.0 | 407.0 · 411.3 · 406.1 |
| L3 | + `glyphExtents` raw | 427.5 | 427.1 | 442.7 · 427.0 · 427.1 |
| L4 | + `js->clj` of each extents object | 697.4 | 702.5 | 702.5 · 700.0 · 702.9 |
| L4b | + the 11-key glyph map per glyph | 1,052.5 | 1,062.5 | 1,061.5 · 1,062.5 · 1,067.1 |
| L4c | + `setVariations` per line | 1,060.8 | 1,057.2 | 1,072.6 · 1,056.5 · 1,057.2 |
| L4d | + bidi · run split | 1,266.6 | 1,280.6 | 1,297.6 · 1,280.6 · 1,272.7 |
| L4e | + `position-runs` · `cluster-records` · mapcat | 1,877.9 | 1,910.1 | 1,964.6 · 1,892.5 · 1,910.1 |
| **L5** | **the real `shape-line`** | **1,983.8** | **187.8** | 200.8 · 187.8 · 184.4 |
| **L6** | **the real `tl/layout` per block, wrap `:none`** | **5,755.0** | **557.4** | 568.8 · 557.4 · 547.9 |

Reading it:

- **Shaping:** 6.01 → **0.57 µs/glyph**. The floor named in the contract
  (48 ms HarfBuzz + ~96 ms crossings) was 144 ms; the flat road lands at
  188 ms, i.e. ~44 ms for everything else — bidi, the run split over arrays,
  the pen walk, cluster ends, the per-glyph `hb_font_get_glyph_extents` calls
  (N calls, zero objects). Crossings are no longer a separate line item:
  there is one `subarray` view read per run.
- **Layout:** 17.43 → **1.69 µs/glyph**; the layout-side residue (L6 − L5)
  3,771 → 370 ms. The whole class — positioned maps, ink maps, span-index
  maps, cluster/run maps, `subs` per glyph, and `compact-result`'s second
  walk — is gone; what remains is the span sort, the wrap scan, the run
  records, and the plane writes. No ladder over layout was needed (contract
  §2(2)); the class was removed whole and the same bracket measured it.
- **Growth law (L6 per-block rows, `receipt.json → ladder[L6].per-item`):**
  1.69 µs/char mean over the 16 blocks ≥ 5,000 chars vs 2.18 µs/char over
  the 50 blocks of 100–999 chars; the 96,727-char block runs at 1.52 µs/char.
  Linear, with per-line fixed cost amortizing at scale (before: 16.65 /
  17.27 / 17.59).
- **The control held:** every replica rung is within 0.4–1.7% of step 1
  (L4e +1.7%, the noisiest); L0 48.4 → 48.9. Same machine, same clock, same
  corpus — the deltas on L5/L6 are the road, not the day.

## What the fences said before this number was taken

- JVM (`clj -M:test`, the text namespaces + `flat-road-test`): 27 tests,
  344 assertions green — F2 (`tl/result=` flat vs the frozen map road,
  bit-identical planes on every fenced corpus incl. wrap, headers, faults,
  the shuffled pathological order), the converter round trip, I2's oracle
  mode seeing through planes, layout never memoizing.
- Verifier (`npm run verify:render-engine`, physical adapter): all six lane
  guards pass incl. `text-flat-road`; F1 (flat shaper ≡ oracle shaper on
  real HarfBuzz, 7 lines incl. RTL/tab/fallback/ligature/combining, RTL
  clusters descending, tab on a stop, ɐ through the fallback face), F2 (real
  HarfBuzz layouts incl. word wrap and block-greedy with headers), F3 (flat
  pack bytes ≡ oracle pack bytes on three cases; carried ops fall back 0
  times, uncarried ones exactly once each); **every golden byte-identical**
  (7 DejaVu Slug cases, the Ubuntu mixed-face case, the path representative).
- The ownership fence (`verify_text_layout_fence.mjs`): green; raw plane
  reads still live only in `layout_planes.cljc`; the packer is a registered
  paint consumer that never owns layout.
- The verifier's permanent bracket (receipt `textFlatRoad.bracket`, a
  55-glyph fixture × 40 reps, never gated): shape 4.5 µs/glyph, layout 11.5
  µs/glyph, pack 3.9 µs/instance — per-line fixed costs dominate at that
  size (setVariations, bidi, buffer create); it is a drift alarm, not a
  rate. The corpus number above is the rate.

## Files

- `receipt.json` — the replay (attestation first, capture incl. `gitDirty`
  and source sha256s, environment, provider, corpus stats, the ladder with
  all passes and the L6 per-block rows, console).
- `README.md` — this file. `SHA256SUMS` — over both; verify with
  `sha256sum -c`.
- Replay road: the probe (`src/app/client/verifier/shaper_border_probe.cljs`
  · `test/render_engine/profile_shaper_border.mjs` · shadow build
  `:shaper-border-probe`) is retired at close per the contract §2(3); to
  replay, check out the commit that banked this receipt, build
  `shaper-border-probe`, and run the runner on the manifest's corpus.
