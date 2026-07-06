# BRANCH REPORT — Δ7 slug glyph-set expansion

**Stamp: Trunk-4 / t4-substrate · 2026-07-06 · NO commits (working tree only)**
**Verdict: Δ7 BUILT. Falsifier PASS.** Slug backend now carries 591 glyphs
(was 95 ASCII); the full design-language set — including ⊢ and ⚑, which even
the merged MSDF atlas lacks — renders from slug data with the uniform mono
advance. Missing-glyph is now *drawn* tofu-with-advance (U+FFFD added to the
slug meta), strictly stronger than the pre-Δ7 honest-gap-with-advance.

---

## 1. What the landing actually required (found, not built)

The "slug meta/curve/band regeneration toolchain, unnamed by the contract"
(GATE_REVIEW_B2 risk 3) **already exists in-repo**:
`src-build/build/slug_font.clj` — a pure-JVM generator (Java AWT outline
extraction → quadratic curves → band layout → curve/band texture packing).
It is glyph-list-driven: it reads whatever `glyphs[]` the DejaVu *metrics*
JSON carries. So the whole expansion reduces to:

1. regenerate `dejavu_sans_mono_atlas.json` with a wider charset
   (`msdf-atlas-gen`, present at `/usr/local/bin`), then
2. re-run the slug generator over it.

Zero renderer changes were needed (and none were made): the glyph table
(`renderer.cljs:1139 glyph-map`) is data-driven; slug texture upload
(`renderer.cljs:610,630`) takes width/height from the meta; the V3-5
fallback (`renderer.cljs:1215-1219`) advances unconditionally and draws
`(get glyphs 0xFFFD)` when a codepoint is missing. Fence held.

## 2. The required glyph set (collected, with sources)

- **Code, rendering today** (`src/app/client/workspace/trail_face/`):
  `● ◐ ○` (staleness, cards.cljc:98-100), `◌` (hole, :235),
  `↔ ✎ ↓` (feed kinds, :257-259), `├` (kraft-tack, scene.cljc:197 — the
  documented *substitute* for ⊢, absent from the merged atlas), plus
  `· — … • └ │ §`.
- **Design language** (`design/claude/room-card-lane-2026-07-05.md`): the
  blessed connector set `⊢ │ ├ └ •` (R6), dead-end `✗`, `✓`, the ⚑ flag
  class (⚑ ⚐ included).
- **Corpus** (CHARSET_AUDIT.md): box-drawing (U+2500 block — 1.13M × `─`
  alone), arrows (U+2190 block — 371k × `→`), dashes/quotes/ellipsis,
  `★ ⚠ ✔ ✗ ✓`, Latin-1.
- **Base**: the B2-blessed merged-MSDF charset (588 cps in
  `ubuntu_dejavu_merged_atlas.json`) — the slug set must not regress it.

**Target = merged-588 ∪ {⊢ U+22A2, ⚑ U+2691, ⚐ U+2690} = 591 codepoints**
(all BMP). Charset recorded at `src-build/fonts/dejavu_slug_charset.txt`
(msdf-atlas-gen format, 14 range entries).

## 3. Feasibility facts established

- **DejaVu Sans Mono covers all 591** (fontTools cmap check) — including
  ⊢ ⚑ ⚐ and U+FFFD. The single design-adjacent gap: **⟳ U+27F3**
  (sidebar in-progress dot) is NOT in DejaVu; `↻ U+21BB` is covered and is
  already in the set as the natural substitute. ⟳ renders drawn-tofu-with-
  advance until a font-level fix.
- **All 591 have the identical hmtx advance 0.60205 em** — the mono moat
  (Δ18) holds exactly; no shaping, no per-glyph advance, box-drawing fills
  the cell by design.
- **DejaVu is TrueType `glyf` (quadratic-only)** — the generator's
  cubic-outline hard-stop (`slug_font.clj:87-89`) can never fire on it.
  This constraint is the real wall for *future* fonts: any CFF/OTF
  (cubic) font cannot pass this generator without a cubic→quadratic
  approximation step that does not exist today.

## 4. What was done (repro commands)

```
# backups: resources/public/fonts/*.pre-d7.bak (5 files)
cd resources/public/fonts
msdf-atlas-gen -font dejavu_sans_mono.ttf \
  -charset ../../../src-build/fonts/dejavu_slug_charset.txt \
  -type msdf -size 64 -pxrange 16 -pots -format png \
  -imageout dejavu_sans_mono_atlas.png -json dejavu_sans_mono_atlas.json
# (matches the original DejaVu params: msdf/size 64/distanceRange 16)
cd ../../..
clojure -X:build build.slug-font/write-font-assets!
```

Outputs (all in `resources/public/fonts/`, modified in working tree,
NOT committed):

| file | before | after |
|---|---|---|
| `dejavu_sans_mono_atlas.json` | 95 glyphs, 1024² | 591 glyphs, 2048² |
| `dejavu_sans_mono_atlas.png` | 1024×1024 | 2048×2048 |
| `dejavu_sans_mono_slug_meta.json` | 95 glyphs | 591 glyphs |
| `dejavu_sans_mono_slug_curve.bin` | 4096×1 rgba16float | 4096×5 (163,840 B) |
| `dejavu_sans_mono_slug_band.bin` | 4096×2 rg16uint | 4096×9 (147,456 B) |

New file: `src-build/fonts/dejavu_slug_charset.txt`.
`manifest.json` NOT touched (see §6).

## 5. Falsifier evidence (glyph-table test — PASS)

Scripted verification over the generated assets (bin bytes decoded and
cross-checked against meta):

- **Design set renders in slug**: every one of
  `⊢ │ ├ └ • ✓ ✗ ⚑ ⚐ ● ◐ ○ ◌ ✎ ↔ ↓ — · … § → ─ ═ ★ ⚠ ✔ ↻ �` present in
  the slug meta, advance exactly **0.602051** each, curve/band locations
  in-bounds, first curve texel non-degenerate (real outline data decoded
  from the bin for each — e.g. ⊢ texel (0.125, 0.625, 0.125, 0.4922),
  band grid 1×2).
- **Tofu-WITH-advance (V3-5)**: ⟳ U+27F3 absent from meta (font gap) →
  shaper path `renderer.cljs:1217-1219`: advance is unconditional
  (`swap! !x + advance` before `when g`), then `(or (get glyphs code)
  (get glyphs 0xFFFD))` — and U+FFFD is now present WITH ink
  (`sampleBounds` non-nil), so missing glyphs draw visible tofu at full
  advance. Never dropped, never zero-advance.
- **No narrowing**: all 95 pre-Δ7 codepoints present; their `advance`,
  `planeBounds`, `sampleBounds`, `banding`, `bandMax` fields are
  **identical** to the S47-proven `.pre-d7.bak` generation (packing
  positions differ; geometry does not) — the generator run is a
  regression-clean superset.
- **Uniform advances**: exactly one advance value (0.602051) across all
  591 glyphs.
- **Bin/meta consistency**: curve bin = 4096·5·8 bytes, band bin =
  4096·9·4 bytes, matching the declared texture dims; all band headers
  within row bounds.

(No screenshot: the assets are wired but not *selected* — the manifest
default is the merged MSDF per Sid's OI-2 interim. A live render of slug
requires the switch-on in §6, which is not this branch's call.)

## 6. Switch-on = one manifest edit (Sid's call, not taken here)

The app renders slug the moment `manifest.json` `default:true` moves from
`ubuntu-dejavu-merged` back to `dejavu-sans-mono` (its `preferredBackend:
"slug"` + slug asset triple are already wired; `fonts.cljs:101-104` picks
slug when the assets load). Two consequences to price at that moment,
both pre-existing and known:

1. **charWidth 0.56 → 0.60**: the DejaVu manifest entry carries 0.60 and
   this flowed correctly when DejaVu/slug WAS the default (pre-B2, OI-2
   finding). The ~30 hardcoded 0.56 sites are Δ18 LEAVE territory —
   unchanged by this branch.
2. **Glyph style changes** Ubuntu→DejaVu everywhere (the OI-2 destination
   as ruled; slug was Sid's S47 preference).

## 7. Named leftovers (for the face-2 contract's glyph gate)

- **`scene.cljc` kraft-tack**: real ⊢ is now in slug data (and NOT in the
  merged MSDF atlas the app currently renders). Swapping `├`→`⊢` in
  `kraft-tack` is face-2 territory — it should land *with or after* the
  slug switch-on, else it renders tofu on the MSDF interim.
- **⟳ U+27F3**: font-level gap. Options: accept drawn tofu; switch the
  status-dot vocabulary to `↻` (covered, in-set); or patch the glyph into
  a derived font (heavier, needs no new toolchain decision today).
- **Astral/UTF-16**: the shapers iterate `.charCodeAt` (UTF-16 units) —
  astral codepoints (emoji 🔴🟢) would double-advance if they ever reached
  the shaper raw; the cljc sanitizer substitutes upstream today. Both
  target sets here are BMP-only. Becomes real only if emoji enter the
  design language.
- **Future fonts**: slug generation for any *cubic-outline* (CFF/OTF)
  font is blocked on a quadratic-conversion step the generator lacks —
  the named wall if the design ever leaves DejaVu for slug.
- **Merged MSDF atlas still lacks ⊢ ⚑ ⚐**: not regenerated here (it is
  B2's gate-4/5 artifact and the TEMPORARY path; expanding it is a
  10-minute rerun of the B2 recipe with this same charset file if the
  interim needs ⊢ before switch-on).
