# Shaper border — the flat road (step-2 contract)

**Cutter:** Claude Fable 5 (`claude-fable-5`), effort high, 2026-08-30.
**Lane:** shaping-correction · step 2 of the shaper-border lane (step 1 = the
banked receipt, `evidence/2026-08-30-shaper-border/README.md`).
**Sid's word on the headline:** the starter's `<paste verbatim + time>`
placeholder arrived unfilled and the paste is taken as the opening act. His
word on the RECEIPT is on record, captured by a parallel session (NOW.md,
commit `e24fb19`, 2026-08-30 14:10, "in chat, after the receipt review"),
verbatim: **"12seconds is too much where did that no. come from??? it should
be as small as possible under 500ms i would say"** — a redline on the bar,
not a close of the lane; §2(4) carries it.
**Touches:** the word (opens) · atom acceptance (closes). Nothing else waits;
the cut executes straight through in the cutting session (starter, verbatim:
"Cut ONE document … Then execute straight through").
**Law read primary:** decisions.md "The render seam" · CONTRACT.md §4 I1–I2,
§12 · work-package skill · the receipt README. Code read by seam: `shaper.cljs`
whole · `layout.cljc` :384–645, :646–960, :1085–1132 · `layout_planes.cljc`
:132–306, :307–497 · `painter.cljs` :455–606, :637–720 · the fence script ·
`hbjs.js` :1–70, :987–1030, :1136–1215, :594–612, :1330–1347.

## 1. Scope — what the atom is

Between HarfBuzz and the GPU the text organ today builds an object per glyph
four times over:

1. hbjs builds a JS object per glyph out of the WASM heap
   (`getGlyphInfosAndPositions`, `hbjs.js:1186`; `glyphExtents`, `:594`).
2. the shaper `js->clj`s both, then builds an 11-key glyph map and a
   12-key positioned map (`shaper.cljs:178–194`, `:221–242`), plus a cluster
   record per cluster (`:244–260`).
3. layout builds two more maps per glyph (positioned+scaled, then +ink,
   `layout.cljc:773–797`), a span-index map per cluster (`:538–567`), cluster
   and run maps (`:799–878`), a `subs` per glyph — and then
   `planes/compact-result` walks ALL of it again to write the typed planes
   (`layout_planes.cljc:132–306`).
4. the painter derives a glyph map per glyph back out of the planes
   (`glyph-view*`, `layout_planes.cljc:328`) and an instance map per glyph
   (`paint-slug-line`, `painter.cljs:556`) before `aset`ing 25 words
   (`pack-slug-instances!`, `:637`).

Measured (the receipt): HarfBuzz itself is 48 ms of a 1,984 ms shaping pass
over 330,254 glyphs; the WASM crossings are 96 ms; ~1.5 s is "an object per
glyph". Layout adds 3,771 ms on top (11.4 µs/glyph), structure-read as the
same disease, magnitude unsplit.

**The atom replaces 1–4 with columns.** The shaper reads HarfBuzz's structs
straight out of the WASM heap into typed arrays per line (one crossing in —
`addText`; one crossing out — the two `subarray` views); layout scales those
columns into its retained planes with no map in between; the painter iterates
the planes into the instance buffer's words. Every per-glyph map road that
exists today stays alive as the **oracle** of the flat road, and three fences
assert the two agree. The retained plane vocabulary, the accessor seam, every
Contract-T read result, the Slug instance format, the layout key and cache,
and the shaping semantics are unchanged — the atom changes the SHAPE of the
data on the road, nothing the reader of a layout result can see.

## 2. The four statements the starter asked for

**(1) Order against union_bounds / I1–I2.** Finding, checked in git and in
the receipt: the union_bounds correction already landed — `52d123f` (T1
proportional shaping) and `e577311` (ownership + multiline spans), lived
acceptance PASS/PASS 2026-08-05 (`docs/packages/shaping-correction.md`); the
receipt's L6 growth law is linear at 16.7–17.7 µs/char across 100-char and
96,727-char blocks. There is no second fix left to order. What the pass
carries from I1–I2 is the DOORS: the `:layout-result` op door and
`line-paint-ops` (`layout.cljc:1115`), the total `layout-key`, the
`layout-cache-acquire` oracle mode, the counted fallback in
`position-text-op`. They have had no production carrier since the 2026-08-20
waist cut removed ground (`tl/layout`'s only live callers today: the painter's
own counted fallback and the verifier; `on-plane/layout-placed-text` has no
caller). Position: ONE pass over the organ; "both fixes" = the flat data
shape + the I1/I2 doors kept live and re-tested on the flat road (S4). No new
carrier is wired here (refusal R2).

**(2) Does the layout-side 3.77 s need its own ladder first?** No. The
structure read (§1 item 3) shows at least six map allocations and a `subs`
per glyph on the layout side, every one of them removed by the re-key; a
ladder would rank sites we are deleting wholesale. The instrument that earns
its keep is the SAME bracket AFTER: pre-registered predictions (§3) + one
replay at close (§9). If the replay leaves layout ≥ 3 µs/glyph, THEN a ladder
over `layout.cljc` is the next receipt (refusal R6 → LATER).

**(3) The probe's retirement.** One replay at close of the existing probe,
unchanged except the two lines that count glyphs on the flat shape (L5 reads
`:glyph-count`; L6 is untouched — it counts through `tl/layout`'s result).
Its before/after table is banked in a new evidence directory; then the probe
namespace, the headful runner, and the `:shaper-border-probe` build are
deleted in the close commit. Retired, never extended. The permanent
replacement is ONE bracket in the verifier's receipt — µs/glyph for
{shape, layout, pack} over a fixed fixture — a recorded number, never a gate
(timing gates flake; CONTRACT §8's bounded-observability spirit).

**(4) The founding-scale numbers are derived.** Rate × count at the
human-block rate; the 36 machine blocks are absent from the archive as blocks
and would push shaping UP (block-greedy reshape ≤2×). The **12.0 s cold bar**
was CONTRACT §10's — the contract author's choice for the old workspace's
cold visual settle, a surface no longer in the tree — and Sid struck it
(2026-08-30, verbatim above): the bar is **whole-system, under 500 ms**. Read
against this atom: whole-corpus text CPU at founding scale (≈579k glyphs)
was 17.4 µs/glyph ≈ 10.1 s before the cut; at the replay's 2.26 µs/glyph it
is ≈1.3 s — still over the bar on its own, so text meets Sid's bar only per
VISIBLE glyph (shape what you look at: the viewport-residency thread, which
this atom does not open — R1/R2). What this atom delivers against the bar is
the rate: under 500 ms the text organ now affords ≈220k glyphs (≈ the whole
144-block human corpus at 330k, minus a third) where it afforded ≈29k.
Whole-corpus shaping never on the cold path is the residency thread's law
to write, not this document's.

## 3. Pre-registered predictions (hypotheses — the close replay decides)

Same corpus (sha256 `edfed32c…bd63`, 144 blocks / 3,511 lines / 330,254
glyphs), same machine, same headful road, physical adapter attested first,
medians of 3.

- **P1** `shape-line` whole-corpus pass: 1,984 ms → **≤ 400 ms** (≤ 1.2
  µs/glyph). Floor: 48 ms HarfBuzz + ~96 ms crossings + the run split.
- **P2** `tl/layout` per block, wrap `:none`: 5,755 ms → **≤ 1,500 ms** (≤ 4.5
  µs/glyph). Wide band: layout's non-map work is unmeasured.
- **P3** planes bit-identical to the oracle road on every fenced input;
  instance bytes identical; every GPU golden byte-unchanged.

A miss is a finding to name in the close NOW, never a stop; acceptance is
Sid's word on the banked before/after.

## 4. Refusals — what the atom deliberately does not do

- **R1 — not a cache.** No memo of shaping or layout keyed by text, glyphs,
  or font, anywhere in shaper/layout/pack. The layout key + cache (I2) already
  exist and are untouched. → any memo is the residency thread's question.
- **R2 — not a new carrier.** No production surface is wired to carry
  `:layout-result`; the doors stay and are tested. → the block face rebuilt as
  material (electric-native DIRECTION) / viewport-residency carry it.
- **R3 — not a change to the retained vocabulary.** Plane names, widths, the
  48 B/glyph + 16 B/span + 64 B/code-unit budgets, `plane-ref`, every accessor
  and every Contract-T read result (paint/caret/selection/hit/clip/copy/
  measure/wrap) are identical. → residency owns plane changes.
- **R4 — not incremental shaping.** Batch stays batch; the keyed-diff shaping
  view is the render seam's later moment (decisions.md "Where this stands").
- **R5 — not a GPU format change.** Slug's 25-word instance, the WGSL, the
  buffer growth policy, `line-offsets`/clip runs are untouched; only WHO writes
  the words changes.
- **R6 — not a layout ladder.** See §2(2). → LATER if P2 misses.
- **R7 — not the legacy road.** `legacy-layout` (T0, no provider) and
  `legacy-provider` untouched.
- **R8 — no shaping-semantics change.** Features, bidi levels, fallback face
  selection, tab stops, cluster level 1, per-line variations, the two-face
  fallback check — unchanged; the oracle fence F1 is the proof.
- **R9 — no region3d/image/path work.** Their painters are not on this road.

## 5. Laws (pointers; each names the scenario that fails under its violation)

- decisions.md **"The render seam"** — *the fenced incremental view* ("a
  batch stage is never deleted when its sibling arrives — it is demoted to
  that sibling's oracle") and *no derivation without a contract*. The flat
  road's five: keyed inputs = text × provider identity × layout params (the
  unchanged `semantic-input` → `:layout/id`); door = the `tl/layout` call, no
  clock ancestor; ownership = the one layout-result value; projections = draw
  (the pack iterator), pick/caret/selection (accessor views), inspector
  (`rich-lines`), oracle (the map roads); oracle + fence = F1/F2/F3. Fails
  under: S1, S2, S3.
- CONTRACT.md **§4 I1–I2** (carried, §2(1)); **I5** provider-fault totality —
  a non-empty text shaped to zero glyphs is a fault on the flat road exactly
  as `(empty? (:clusters shaped))` was. Fails under: S2 (provider-fault
  table), S4.
- CONTRACT.md **§8 / the proportionality receipts** — `:receipts
  :proportionality` keeps its meaning on the flat road: `glyph-visits` = G,
  `cluster-index-writes` = S, `run-index-*` = R, `wrap-candidate-visits` ≤ 2C,
  `shape-calls`, `reference-shapes`, `provider-fault`. Fails under: S2
  (pathological linearity, growth row).
- Contract-T **§7.5 the accessor seam** (`build/render-engine/W1.md`), enforced
  by `test/render_engine/verify_text_layout_fence.mjs`: raw plane reads live
  only in `layout_planes.cljc`; painters never own layout. The pack door is a
  NEW accessor inside that file; the fence's raw-token walk stays green with
  zero exemption changes. Fails under: S3 (fence run).
- CLAUDE.md **Token economy / work-package** — compose before the first
  write; repairs in one batch; freeze → close receipt → terminal NOW.

## 6. The vocabulary — pinned exactly

### 6.1 The shaped line (`app.client.text.shaped-line`, `.cljc`)

The provider's `:shape-line` returns ONE record-free map of typed columns and
scalars. Cross-platform arrays as `layout_planes.cljc` already does
(`#?(:clj (int-array n) :cljs (js/Int32Array. n))`, same for u32/f32).

Scalars: `:glyph-count` · `:run-count` · `:advance` (int, font units, the pen
after the last run) · `:base-direction` (`:ltr`/`:rtl`) ·
`:faces` (vector `[{:id :revision :upem}]`, index space of `:run-face`).

Glyph columns (length `glyph-count`, **visual order**):
`:glyph-id` u32 (0 for a virtual tab) · `:cluster-start` u32 ·
`:cluster-end` u32 (line-local UTF-16 offsets; cluster-end = the next
distinct cluster start inside the run, else the run's source end — today's
`cluster-end-map`) · `:advance-x` `:advance-y` `:offset-x` `:offset-y` i32 ·
`:glyph-x` `:glyph-y` i32 (pen-walked exactly as `position-runs`: x =
pen + x_offset, y = y_offset, pen += x_advance; tab runs advance to the next
stop with `tab-width = max(1, tab-columns) × primary-upem × 0.5`; named
`glyph-x`, not `position-x`, because the fence's raw-plane token walk is
textual over every `src/app` file — the retained plane keeps its name) ·
`:ink-x` `:ink-y` `:ink-w` `:ink-h` i32 (HarfBuzz extents xBearing, yBearing,
width, height; **`:ink-w = -1` means no ink** — virtual tabs and glyphs whose
`hb_font_get_glyph_extents` returns false) · `:run-index` u32.

Run columns (length `run-count`, visual order): `:run-source-start`
`:run-source-end` u32 (line-local) · `:run-flags` u32 (bit 0 = rtl, bit 1 =
tab run) · `:run-face` u32 (index into `:faces`).

Pinned decisions: **per-glyph `run-index`, not run glyph-ranges** — so any
visual order is expressible (the pathological shuffled provider stays a
lawful input); **no cluster table on the shaped line** — layout derives the
span table in one pass from the cluster columns (that pass IS today's
`glyph-span-index`, re-keyed), and wrap's `cluster-width` is that span's
`max(x+advance) − min(x)` over its glyphs in font units.

Two converters, oracle/test only, in the same namespace: `->maps` rebuilds
EXACTLY today's `{:runs [{… :glyphs […]}] :glyphs [11-key + :position]
:clusters [records sorted by :left] :advance :base-direction}` so the old
roads consume it unchanged; `from-maps` builds a shaped line from that shape
(visual order = the `:glyphs` vector; runs from `:runs`' source ranges; a glyph's
run = the run whose `:glyphs` contains it). `(->maps (from-maps m)) = m` is a
tripwire (S2).

### 6.2 Layout on columns (`app.client.text.layout`, the flat `shaped-layout`)

Two passes, no per-glyph map: (a) shape every visual record (headers, body
lines, wrapped segments — `shaped-segments`/`scan-wrap-cut` re-keyed to the
span table) and count; (b) allocate the result-wide planes ONCE and fill them
line by line. The line-spec maps keep exactly today's keys
(`:line/id` `:line/index` `:logical-line` `:text` `:source-range`
`:paint-source-range` `:consumed-range` `:baseline` `:advance`
`:logical-bounds` `:ink-bounds` `:run-range` `:glyph-start` `:glyph-end`
`:cluster-start` `:cluster-end` `:runs` `::planes` `::shaped?`
`::run-source-index`); `rich-lines`, `plane-census`, `retained-rich-map?`
read them unchanged.

The scale/origin expressions are pinned to today's, in today's order, in
doubles, stored f32 at the end — F2 demands bit equality:
`px = ox + gx·scale` · `py = baseline-y − gy·scale` · `ax = ax·scale` ·
`ay = ay·scale` · `off = off·scale` · ink: `x1 = ox + gx·scale + xBearing·scale`,
`x2 = x1 + width·scale`, `y1 = baseline-y − (gy + yBearing)·scale`,
`y2 = y1 − height·scale`, `{x (min x1 x2) y (min y1 y2) w |x2−x1| h |y2−y1|}`;
no ink → `ink-x = NaN`, others 0. Line `:ink-bounds` = union over glyphs with
ink; `:metrics :ink-bounds` = union over lines (both linear, as today).

The three plane-shape decisions keep their rules: `:advance-y` column present
iff any glyph has a non-zero y-advance; `:glyph-order` present iff any span is
non-contiguous in visual order; `::shaped?` per line iff the line has ≥ 1
glyph from a shaping provider. `run-records` semantics (`:glyph-span` per run
= [min, max+1] of its glyphs' result-wide indexes, `:font-id`/`:font-revision`
from the face, `:advance-y-column?`) unchanged.

### 6.3 The pack door (`layout_planes.cljc`, new accessor)

`(pack-glyphs! line glyph-indexes dx dy f)` — walks the given result-wide
glyph indexes and calls `f` with primitives only:
`(f index packed-id font-id x y cluster-start cluster-end)`; `x`/`y` are
the plane values plus `dx`/`dy` (the same `view-number` road as
`glyph-view*`); `font-id` is resolved inside the door by the same
source-containment rule `run-for-source` uses (the retained planes carry no
per-glyph run column — R3). Raw arrays never leave the file. `result=` (new, public):
element-wise equality over `:layout/planes` (NaN = NaN) plus structural
equality of the rest with `::planes` stripped and `:receipts :proportionality`
excluded — it is F2's comparator and it REPLACES `oracle-match?`'s `=`
(found defect: `=` on two results holding distinct typed arrays is identity in
both hosts, so I2's oracle mode could never report a match on a real layout).

### 6.4 The pack (`app.client.text.glyph-pack`, `.cljs`, verifier road)

Takes a layout line, the op's glyph indexes, the op's style, and a **slug
table** derived once per `font-assets` identity (the same WeakMap-on-assets
pattern as today's `glyph-map`; a derived table, not R1's cache): per fontId an
`Int32Array` glyph-index → row, rows as one `Float32Array` (sampleBounds
l t r b · banding sx sy ox oy) + one `Uint32Array` (glyphLoc x y · bandMax x ·
packedBandMeta); the six-step resolution of `painted-glyph`
(`[font-id :index gid]` → `[:index gid]` → FFFD by font → FFFD → index 0 by
font → index 0) reproduced in the SAME reduce order — the `[:index gid]`
entry is the LAST glyph with that index, as the current reduce leaves it.
Gives: words written into the instance `Float32Array`/`Uint32Array` views at
`base = instance × 25`, the identical layout `pack-slug-instances!` writes.
Skips exactly what `paint-slug-line` skips: a glyph whose cluster text is the
single character `" "`, and virtual tabs. **Two passes** — count, then write —
so `actual-instances`, `line-offsets`, `required-size`, and the `:!shape-rev`
bump are byte-for-byte today's (F3). Holds nothing.

### 6.5 The shaper on the heap (`app.client.text.shaper`, flat `shape-line`)

- `load-harfbuzz!` keeps the Emscripten `Module` beside the hbjs wrapper
  (`Module.HEAP32`, `Module.HEAPU32`, `Module.wasmExports`,
  `Module.stackAlloc/stackSave/stackRestore` — `hbjs.js:1–10`).
- Per run: hbjs `createBuffer`/`addText` (UTF-16 → clusters are UTF-16 code
  units, `hbjs.js:991`)/props/`hb.shape`/`destroy` stay. The read-out is
  direct: `n = wasmExports.hb_buffer_get_length(buf.ptr)`;
  `infos = Module.HEAPU32.subarray(p/4, p/4 + 5n)` with
  `p = hb_buffer_get_glyph_infos(buf.ptr, 0)` (stride 5: codepoint, mask,
  cluster, var1, var2); positions likewise from
  `hb_buffer_get_glyph_positions` on `HEAP32` (stride 5: x_advance,
  y_advance, x_offset, y_offset, var). Copy into the line's columns in one
  loop; cluster += run start.
- Extents: `hb_font_get_glyph_extents(font.ptr, gid, scratch)` per glyph into
  ONE 16-byte scratch `malloc`ed at provider creation (provider lifetime, never
  freed), read as four `HEAP32` ints — N calls, zero objects (the calls were
  18 ms at 330k glyphs; the objects were 270 ms).
- **Heap views are re-read from `Module` per run, never held across a WASM
  call** — memory growth replaces the `HEAP*` arrays.
- The run split (`codepoint-spans`, `faces-by-offset`, `logical-runs`,
  `visually-order-runs`) becomes array loops over a per-line `Uint8Array` of
  face indexes and bidi-js's `levels`; identical semantics (a run breaks on
  level, face, or tab-ness; visual order by min reordered index).
- The old road moves verbatim into `app.client.text.shaper-oracle` (`.cljs`),
  taking `{:hb :bidi :faces}` explicitly; `create-provider` exposes it as
  `:shape-line-oracle` (one-line hook). The old layout road moves verbatim into
  `app.client.text.layout-oracle` (`.cljc`; its tiny private helpers are copied,
  public ones required) exposing `(layout input)` that wraps the provider with
  `->maps` and ends in `planes/compact-result`. Frozen copies: the oracles are
  never "improved".

## 7. Exact entry points

MAY EDIT — `src/app/client/text/shaper.cljs` (the flat road; the oracle hook) ·
`src/app/client/text/layout.cljc` (flat `shaped-layout` + re-keyed wrap
helpers; `oracle-match?` → `planes/result=`; `glyph-indexes-in-source-range`
public accessor) · `src/app/client/text/layout_planes.cljc` (ONLY: the pack
door `pack-glyphs!`, `result=`, and a from-columns constructor beside
`allocate-planes`; retained vocabulary untouched) ·
`src/app/client/text/painter.cljs` (thin hooks in `position-text-op` and
`update-text-data`; `shape-text`/`paint-slug-line`/`pack-slug-instances!` stay
as the oracle road) · `src/app/client/verifier/core.cljs` (F1, F3, the
µs/glyph bracket; result keys `textFlatRoad`) ·
`test/render_engine/verify_text_layout_fence.mjs` (owners rows for
`position-text-op` and the packer; `paintConsumers` += the packer; nothing
else) · `test/render_engine/run_verifier.mjs` (one `laneGuards` row
`text-flat-road`) · `test/app/client/text/*` (untouched: a map-shaped
provider return is coerced to a shaped line by `from-maps` at ONE seam —
`shaped-layout`'s `shape!` — so every existing corpus drives the flat layout
road unchanged) · `shadow-cljs.edn` (delete the probe build at close) ·
`docs/shaping-correction/NOW.md` · `docs/next-prompt.md` (the pointer line).

MAY CREATE — `src/app/client/text/shaped_line.cljc` ·
`src/app/client/text/shaper_oracle.cljs` ·
`src/app/client/text/layout_oracle.cljc` ·
`src/app/client/text/glyph_pack.cljs` ·
`test/app/client/text/flat_road_test.clj` ·
`docs/shaping-correction/evidence/<date>-flat-road/` (README + receipt.json +
SHA256SUMS).

MUST DELETE at close, after the one replay —
`src/app/client/verifier/shaper_border_probe.cljs` ·
`test/render_engine/profile_shaper_border.mjs` · the `:shaper-border-probe`
build entry.

MUST NOT TOUCH — `src/app/client/region3d/**` (dead `layout-placed-text`
keeps calling `tl/layout`; on_plane_test's legacy road passes untouched) ·
`image/**` · `path/**` · `engine/**` · server · `resources/public/fonts/**` ·
the Slug WGSL and `slug-text-instance-stride` · `docs/decisions.md` (no ruling
is needed; a finding that forces one is proposed as one line in NOW, never
written) · the historic CONTRACT.md / VALIDATION_R* / RECUT_LEDGER_R* files ·
`src/app/server/env.clj` (never read).

Every namespace docstring reads: what it is · Takes · Gives · Holds.

## 8. Decisive scenarios → tripwires at close

**S1 — the mixed line.** `"AV office é\tسلام ɐ\n漢字"`-class input: four
run kinds (LTR, tab, RTL, fallback face), a ligature, a combining mark, a
non-BMP-free line and a two-face line. F1 (verifier, real HarfBuzz):
`(= (->maps (shape-line t o)) (shape-line-oracle t o))` exact. F2 (JVM
synthetic + verifier real): `result=` of flat `tl/layout` vs `layout-oracle`.
*Wrong build that passes:* a shaper that forgets `cluster += run start` passes
every single-run line — S1 has four runs; strengthened: the RTL run's
cluster-starts strictly decrease in visual order, the tab glyph's advance
lands on a stop, the ɐ glyph's run-face is the fallback.

**S2 — the frozen pathological corpora on the flat road.** Every existing
test in `shaping_correction_test`, `layout_test`, `layout_planes_test`,
`on_plane_test` runs green unchanged, their map-shaped providers coerced by
`from-maps` at the seam; F2 holds on
each layout they build; `(->maps (from-maps m)) = m` on the shuffled line;
the plane budget test (48/16/64) unchanged. *Wrong build that passes:* a
`from-maps` that re-sorts glyphs monotonic — caught by the round-trip and by
the shuffled test's exact index vectors + `:glyph-order` presence.

**S3 — GPU bytes.** In the verifier, for every slug case and the ubuntu
mixed-face case: flat pack `Uint8Array` ≡ oracle pack bytes, `line-offsets` ≡,
`num-instances` ≡ (F3); every golden PNG/raw sha256 unchanged
(`gpu-slug-*`, `gpu-slug-ubuntu-mixed-face.png` among the representatives);
the fence script green. *Wrong build that passes:* correct words, wrong
per-line counts — F3 compares `line-offsets` on a two-line case whose first
line holds a space and a tab.

**S4 — I1/I2 doors carried.** `line-paint-ops` → ops with `:layout-result` →
flat `update-text-data`: the fallback counter for that surface stays 0; ops
without the result count exactly once each. `layout-cache-acquire` in oracle
mode with `build-result` = flat `tl/layout` reports `oracle-match? true` on a
hit (JVM). Two `tl/layout` calls on the same text through a counting provider
give `:shape-calls` = 2 × lines — layout never memoizes (R1's runtime
tripwire; the diff review is its static one).

**S5 — the replay.** §9's command on the banked corpus; P1/P2 judged;
attestation first; the before/after table banked. *Wrong build that passes:*
a text-keyed memo aces three timed passes — S4's shape-call count and the R1
diff review close it; a build that defers extents to paint shows a fast L5 —
L6 brackets the whole layout and S3 covers paint.

Goldens (2–3 representative): the existing `gpu-slug-legal-min-z0p01.png`
and `gpu-slug-ubuntu-mixed-face.png` stay the goldens — byte-unchanged is the
claim. The verifier's new receipt block `textFlatRoad` (F1 rows, F3 rows, the
µs/glyph bracket) is recorded, F1/F3 gated, the bracket never.

## 9. Close bundle (pre-registered; one fresh runner after freeze)

1. JVM focused: `clj -M:test -e "(require 'app.client.text.layout-test
   'app.client.text.layout-planes-test 'app.client.text.shaping-correction-test
   'app.client.text.flat-road-test 'app.client.region3d.on-plane-test)
   (clojure.test/run-all-tests #\"app\\.client\\.(text|region3d)\\..*\")"` green.
   Full suite `clj -X:test` — foreign failures are board debt, named.
2. `npm run verify:text-layout` green (layout-test · proportional-shaper node
   receipt · the fence).
3. `npm run verify:render-engine` green — every golden byte-identical, the
   `text-flat-road` guard row true, the bracket recorded in the receipt.
4. The replay: `clj -M:dev -m shadow.cljs.devtools.cli release
   shaper-border-probe` then `DISPLAY=:0 node
   test/render_engine/profile_shaper_border.mjs --corpus=<corpus.json,
   sha256 edfed32c…bd63> --out=docs/shaping-correction/evidence/<date>-flat-road`
   on the proven headful road (Puppeteer + system Chrome, `--use-angle=vulkan`,
   physical adapter, `isFallbackAdapter false` serialized first). README:
   headline, attestation, the before/after table (L5, L6, µs/glyph, P1/P2
   verdicts), SHA256SUMS.
5. Delete the probe (§7) · NOW.md ≤15-line close entry · `docs/next-prompt.md`
   pointer flip. Commits on `main`, grouped by concern: (a) shaped-line
   vocabulary + flat shaper + shaper oracle · (b) flat layout + layout oracle +
   pack door + tests · (c) glyph pack + painter hooks + verifier fences +
   fence rows · (d) replay receipt + probe retirement + docs. No push.

After the NOW close entry, no source/test/tooling edit; a later invalidating
finding gets one REPAIR/RECEIPT PENDING line and resumes in a fresh context.

## 10. MUST-NOTs (real ones only)

- Never read `src/app/server/env.clj`.
- No cache keyed by text/glyphs/font in shaper, layout, planes, or pack (R1).
- No change to retained plane names, widths, budgets, or the accessor seam's
  read results (R3) — the plane-budget test and F2 are the tripwires.
- No change to font assets, WGSL, or the instance stride (R5) — goldens.
- No extension of the probe; one replay, then deletion (§2(3)).
- No push, no merge (Sid's alone); never a Co-Authored-By line.

## 11. Forks seen, written down

- **Oracle placement.** Frozen copies in their own namespaces (chosen) vs.
  keeping the old fns inside `shaper.cljs`/`layout.cljc` behind a flag. Chosen
  because the old code must not be "improved" by proximity and the fence's
  owner audits stay readable; cost: ~40 lines of tiny helpers duplicated.
- **Pack door shape.** A per-glyph callback with primitive args (chosen) vs.
  handing the painter the typed columns under new names. Chosen because the
  residency thread wants `plane-ref` to stay sovereign over storage, and the
  fence's raw-token law stays intact without exemptions; cost: one function
  call per glyph (~ns).
- **Run representation.** Per-glyph `run-index` (chosen) vs. run glyph-ranges.
  Chosen so the shuffled pathological provider stays a lawful input and the
  JVM corpora need no rewrite; cost: 4 B/glyph transient.
- **Extents.** N direct calls into a provider-lifetime scratch (chosen) vs. a
  per-line distinct-gid pass. Chosen because the calls were 18 ms and a
  distinct pass is a cache in disguise (R1).
- **The 12.0 s bar.** Reported as §10's bar for a dead surface (chosen) vs.
  carried as this atom's bar. The current organ's bar is per-glyph (§2(4)).

## 12. The starter for the executing context (this one)

Preflight: toggles set. Boot: this document (whole) · the receipt README ·
`shaper.cljs` whole · `layout.cljc` :384–960 · `layout_planes.cljc` whole ·
`painter.cljs` :455–720 · the fence script. Build by namespace/compile
milestone: `shaped_line.cljc` → `shaper.cljs` + `shaper_oracle.cljs` →
`layout_oracle.cljc` → `layout.cljc` → `layout_planes.cljc` doors →
`glyph_pack.cljs` + `painter.cljs` hooks → tests → verifier → fence rows →
freeze → the close bundle §9. Keep filling; a genuine fork is ONE line in
NOW, never a stop.
