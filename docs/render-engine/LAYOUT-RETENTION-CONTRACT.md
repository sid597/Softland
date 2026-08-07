# LAYOUT-RETENTION — the memory floor of the text substrate (contract)

**Cutter:** Fable 5 (`claude-fable-5`) · effort **xhigh** · 2026-08-08 · one
pass, one session, one document (work-package law). Budget-driven atom on the
text/render substrate; not part of Package 3's region3d line. Binding docs
read PRIMARY this cut: `W1/CONTRACT-T.md` (whole), `W1.md` + `W1/GROUND.md`
(whole), `docs/decisions.md` (whole — the render-seam constitution §"The
render seam" governs), `docs/shaping-correction/CONTRACT.md` §4 (I1–I10),
§5.1, §6 (scoped reads; 98KB doc), `T2-INPUT-FLOOR-CONTRACT.md:358-369` (the
one-result identity obligation), the work-package skill. Code ground verified
at current working-tree bytes via three Opus breadth indexes (text-layout
core · retention ring · GPU budget), verbatim-anchored, scratchpad-archived
(`gather-A/B/C` + this session's task outputs). Receipts: the 2026-08-08
memory-probe suite (`memprobe*.js`, `atom-sweep*.json`,
`memprobe-results.json`, prior session's scratchpad
`f68db61e-…/scratchpad/`), restated in §2 with instrument caveats.

## 1. What this atom is

The workspace boots a **228KB text corpus into ~1.3–1.45GB of JS heap** (dev
build, all five flag configs — the bulk predates region3d). This atom makes
retained text-layout memory **proportional to the corpus**, budget-first:

- **B1 — release-heap gate:** post-boot, post-GC `usedJSHeapSize` ≤ **100MB**
  on the release (`:prod`) build, boot corpus resident. (MB = 10⁶ bytes,
  here and everywhere in this contract.)
- **B2 — scene-data gate:** total retained layout-plane bytes ≤ **15MB** for
  the 228KB boot corpus, measured by an exact census (§7 S1/S5), not by GC
  deltas. Unit law: **≤ 64 bytes per laid-out UTF-16 code unit, all typed
  planes summed** (233,472 units × 64B = 14.94MB — the gate's arithmetic
  spine).

The mechanism is a **schema change, not an eviction policy**: the layout
result's populous planes (glyphs, clusters/spans) become columnar typed
arrays behind accessors, with the Contract-T map shapes preserved as
**derived rich views**. The full boot corpus stays resident — nothing the
seven readers need is dropped; it is re-represented and derived at the edge.

Under the render-seam constitution this is the fenced-incremental-view law
applied to representation: the typed core is the maintained instrument, the
rich view is the demoted-but-kept oracle, and a fence asserts they agree
(decisions.md, "The unit: the fenced incremental view").

## 2. Receipts basis — what is banked, what is structure, what stays hypothesis

Instrument caveats first: all heap numbers are the **dev build** (shadow
`:dev`, localhost:8080), headless Chrome `--enable-precise-memory-info`,
1920×1080. The app never reads adapter identity (`electric_flow.cljc:520-521`
requests adapter/device with no options; `adapter.info` unread anywhere in
src/) — so the probe runs cannot state hardware-vs-SwiftShader; the console
showed `[WEBGPU/DEVICE-LOST]` once per boot. The new harness (§6) must
capture adapter identity itself (work-package field note).

**Banked runtime receipts (taken-path):**
- Boot heap used, 30s settle: baseline 1451MB · live-atoms 1316MB · region3d
  1452MB · live+region3d 1319MB · full-seam-demo 1320MB (`memprobe-results.json`).
  Config-independent ⇒ the bulk is the base workspace corpus.
- Atom sweep, scene store live: nulling `!containers-registry` first freed
  **1238.3MB**; `frame-scheduler/!deadlines` 134.3MB (`atom-sweep.json`).
  Order-dependent — attribution BETWEEN co-rooting atoms is not established;
  the union ~1.24GB is.
- Atom sweep, scene store nulled first: `frame-scheduler/!clock-source`
  freed **115.1MB**; everything else ≤2.2MB; total residue 118.4MB
  (`atom-sweep2.json`). The starter's other suspects (chrome-derive
  providers, connector route cache) freed ≤0.1MB each — cleared as
  independent holders.
- Cluster `:caret-stops` + cluster `:ink-bounds` retention cost **~150MB**
  on the 228k-glyph boot — already repaired in the working tree
  (`text_layout.cljc:841-847`, comment carries the measurement).
- GPU census at boot: 49.6MB content-text instance buffer · 13.4MB grown
  `text/chrome` clone · 60.5MB MSDF atlas texture · compositor
  group/backdrop targets 15.8MB rgba16float each, 5–6 live at zoom
  (`compositor_gpu.cljs:17-22` states the six-target peak).

**Verified structure (static, byte-cited):**
- Per-glyph cost anatomy: each glyph is a **13-key persistent map**
  (`:glyph-id :glyph-id-kind :font-id :font-revision :cluster-start
  :cluster-end :advance :offset :ink-bounds :direction :position :character
  :cluster`) with three fresh 2-vectors, an `{:x :y :w :h}` map, a nested
  `:cluster` map, and a fresh per-glyph `:character` string
  (`text_layout.cljc:800-820`, `text_shaper.cljs:159-168`).
- Self-aliasing: each run re-materializes its glyph maps
  (`text_layout.cljc:890-895` `mapv #(nth glyphs %)`); top-level `:runs`
  `:clusters` re-mapcat everything (`:975-976`); `:line-index` aliases lines
  (`:923`); receipts allocate a full glyph-id vector per layout (`:977`).
- `!clock-source` holds `cljs.core/identity`; `set-clock-source!` has ZERO
  src/ callers (`frame_scheduler.cljc:15,24`); its sole reader is
  `clock-time` (`:32`), called from `decide!` and `render.cljs:321`. **The
  atom cannot itself retain 115MB.**
- The render loop's loop-carried `prev-state` retains one previous
  generation of scene-scale data by reference: `:slot-text-geos` (each
  entry holds the slot's full text-op vector, every op → `:layout-result`;
  `render.cljs:141-144, 890, 956`), `:prev-store-frame` (whole
  `derive-store-frame` payload, `:887`), plus prev-content/chrome/editor
  lanes (`:870-892`). Tokens hold string REFERENCES, not copies — the
  select-keys at `render.cljs:61-68` duplicates map wrappers, not string
  bytes.
- Layout values are shared, not duplicated: the same result object is
  reachable from the layout cache (`ground.cljs:178` →
  `text_layout.cljc:1063`), from every line op it produced
  (`line-paint-ops`, `text_layout.cljc:1134-1150`), from slot `:tree` AND
  flattened+stamped ops (`scene_store.cljc:97-120, 62-95`), and from the
  loop's prev generation. Multiple roots, one copy.
- `!containers-registry` entries hold NO ops/layouts/closures — numbers,
  keywords, a 6-vector affine (`containers.cljc:100-108`). Its 1238MB
  sweep line is co-rooting through walk order, not ownership.
- GPU: text instance stride is **52B** MSDF / 100B Slug
  (`renderer.cljs:760-761`); the 49.6MB buffer is the hard-coded
  `:initial-capacity 1000000` × 52 (`renderer.cljs:2043`); default capacity
  elsewhere is 10,000 (`:1573,1637`); growth is grow-only ×1.5 slack
  (`:2326-2334`, rationale comment: keystroke re-realloc cost ~30ms without
  slack). The 13.4MB `text/chrome` lane is content-grown (≈180k instances ×
  52 × 1.5). The atlas is content-driven: `ubuntu_sans_variable_atlas.png`
  is 3984×3984 rgba8unorm = 60.55MiB (`renderer.cljs:1464-1488`). The
  compositor pool has a real byte cap (512MiB,
  `compositor_gpu.cljs:23,194-211`); leases have no count cap by design.
  **Premise correction:** "230B/instance" matches no stride in source — it
  was a derived ratio; the waste is the capacity literal, not the stride.

**Hypothesis with kill-probe (fence: no verdict until S3's receipt):**
- *The 115MB post-scene-store residue is the render loop's loop-carried
  previous generation, freed because nulling `!clock-source` crashes
  `clock-time` on the next frame and the dying loop drops its state.* This
  is the only structure-consistent account (the atom holds `identity`; the
  loop state is the only ≥100MB structure alive after the store null). S3
  pre-registers it and takes the receipt cleanly. No fix is contracted for
  it beyond what B1/B2 shrink — in normal operation the prev generation
  SHARES structure with the live store; it only became a "holder" when the
  probe nulled the store out from under it.

## 3. The four scope rulings

**R1 — glyph maps → typed-array core with derived rich views: IN.** The
Contract-T schema change this atom exists for. Spec in §5; Contract-T
amendment text in §4. The tripwires that pin `:glyph-span-index` (a
PER-LINE key — premise correction to the starter), per-glyph `:character`,
and per-glyph `:ink-bounds` move with the schema: same assertion VALUES,
re-targeted through the view accessors (§7 S2).

**R2 — the 115MB post-scene-store holder: attribution COMPLETED, fix
absorbed.** Receipt + structure land on the render loop's prev-generation
references (§2), not on an independent leak. S3 confirms with a
pre-registered probe; the residue shrinks with R1 because it is the same
data. One small law lands with it: **frame-loop carried state holds scene
data by shared reference or token only — never per-glyph/per-op derived
copies that outlive the frame** (today's code already complies at the
string level; the law prevents regression when packers change under R1).

**R3 — viewport-scoped layout: REFUSED to LATER (the next rung).** The
arithmetic says representation alone meets B2: ≤64B/unit × 233,472 units =
14.94MB. Layout construction cost and visible-set maintenance stay owned by
shaping-correction I4 and the render-seam "where view-dependence lives"
open decision (decisions.md). This atom keeps the full corpus resident. If
S1's arithmetic fails at implementation, that is the genuine-fork question
(one question, never a stop): the answer space is per-plane trims first,
viewport rung second.

**R4 — GPU: capacity policy IN; stride, atlas, compositor leases REFUSED.**
- IN: kill the `1000000` capacity literal (`renderer.cljs:2043`) —
  demand-seeded creation (seed ≤ 4096 instances) under the EXISTING
  grow-only ×1.5-slack policy. Expected boot effect: 49.6MB → ≤~2MB created,
  growing to content (~14MB text total at this corpus). Pixels untouched.
- IN (receipt honesty, one line): `gpu_budget.cljs:182-184` prices
  `sampleCount` into texture bytes, matching the compositor pool's ledger
  (`compositor_gpu.cljs:115-125`) — today MSAA targets read 4× cheaper in
  [GPU-BUDGET] than they are.
- REFUSED: stride 52→40 (saves ~23% of a now-demand-sized buffer, touches
  vertex layouts/shaders — routed LATER, rides the Slug/MSDF road work).
  Atlas re-budget (60.5MB is the font asset's dimensions — routed to the
  MSDF-vs-Slug road receipt, W1/GROUND). Compositor lease budget (512MiB
  cap exists; owned by the W4/compositor lane).
- **Task-manager-total is a REPORTED receipt, never a gate** this atom: the
  harness records browser-process RSS and the GPU census beside B1/B2, so
  Sid sees the whole bill; the gates stay the two Sid named.

## 4. Laws (pointers, no restatement) + the Contract-T amendment

- `W1/CONTRACT-T.md` §7.1–§7.2: fields and routes binding; the seven
  readers read the ONE result; no untagged source offset.
- `shaping-correction/CONTRACT.md` §4: I1 (one layout authority), I2
  (declared total key — §6's `[source-token provider-token metric-token]`
  key and the address/VPT construction are UNTOUCHED by this atom), I4
  (construction O(G+C+R) — plane filling must stay one linear pass), I5
  (indexed spans — the plane tables ARE the indexed spans; the span-helper
  seam stays in shared cljc so the JVM asserts it), I10 (residue stays
  residue). §5.1: line-record range value shapes stay EXACTLY as pinned
  (lines stay maps; this atom does not touch `:source-range` /
  `:consumed-range` shapes).
- `T2-INPUT-FLOOR-CONTRACT.md:358-369`: one-result identity by
  `:layout/id` — untouched semantics; editing/preedit roads
  (`editing_runtime.cljs` `!documents`/`!session`) keep carrying whole
  results (they are few and now cheap).
- decisions.md render-seam constitution: recompute proportional to change;
  the fenced incremental view (batch oracle demoted, never deleted); no
  derivation without a contract. The rich view is the oracle of the typed
  core; the fence is S1's equivalence check.
- **Contract-T amendment (applied to `W1/CONTRACT-T.md` at atom close, as
  §7.5):** *"Representation law (layout-retention atom, 2026-08-08): the
  result's populous planes — per-glyph data, per-cluster/span data — are
  stored as columnar typed arrays owned by the layout namespace. The §7.1
  map shapes remain binding as the LAW OF THE VIEW: every reader obtains
  them (or scalar fields) exclusively through the layout accessor API,
  which speaks tagged offsets at its boundary; the index-space is declared
  once per result and reconstituted into tagged values at the view edge.
  Rich views are derived on demand and never retained inside the result.
  A reader holding raw plane arrays outside the accessor namespace is the
  same crime as a private metric route (§7.3)."*

## 5. The plane schema — what the result becomes

One result value still exists per (§6 key), still carries
`:text-layout/version :layout/id :source :font :shaping :space :regime
:constraints :metrics :lines :line-index :clip-plan :receipts`. What
changes inside it:

**Planes (typed, per result, built in the one construction pass):**
- **Glyph plane**, budget ≤48B/glyph: position x,y (f32×2) · advance-x
  (f32) · offset x,y (f32×2) · ink-bounds x,y,w,h (f32×4) · glyph-id (u32)
  · cluster-start, cluster-end (u32×2) · flags (u8, packs
  `:glyph-id-kind` + `:direction`). `:font-id`/`:font-revision` move to
  the RUN record (constant per run — never per glyph). Advance-y: per-run
  exception flag + side column only when nonzero (rare; vertical/mark
  cases).
- **Cluster/span plane**, budget ≤16B/entry: source-start, source-end,
  glyph-start, glyph-end (u32×4). This UNIFIES today's cluster records and
  `:glyph-span-index` span maps — they partition the same axis. Monotonic
  layouts need nothing more; a non-monotonic layout (shuffled glyph order)
  carries one extra u32 index column, flag-gated (the shuffled-glyph
  fixture stays a tripwire).
- Cluster `:logical-bounds` and caret stops are **derived at read** from
  glyph columns + line metrics (the road `cluster-caret-stops` already
  drives today when stops are absent, `text_layout.cljc:1179`).
- `:character` is **derived at read**: `(subs source-text cluster-start
  cluster-end)` — no per-glyph string storage; subs semantics (UTF-16)
  unchanged.
- Sum law (S1): all plane bytes ÷ laid-out code units ≤ **64**.

**What dies:** per-glyph persistent maps in the stored result; the run-level
`:glyphs` re-materialization (`:890-895`); top-level `:runs`/`:clusters`
mapcat aliases (`:975-976`) as STORED values; per-line `:glyph-span-index`
span-map vectors with `:glyph-indexes` (`:571-594`); the per-glyph
`:character` strings; the retained glyph-id receipt vector (`:977` — hash
computed in-pass, vector discarded).

**What stays maps:** line records (16 keys today, minus `:glyphs` — they
gain `glyph-start/glyph-end` into the plane), run records (minus `:glyphs`,
plus font identity), `:line-index`, `:metrics`, `:clip-plan`, `:receipts`,
`:source`. They are O(lines+runs) — thousands, not hundreds of thousands.

**The view/accessor API** (in the new namespace, exact names implementer's
choice, signatures law): span selection in/out of source ranges takes and
returns TAGGED offsets; a per-line lazy glyph view materializes §7.1-shaped
maps on demand (the oracle and the test surface); scalar accessors
(position-x/advance-x/ink-x…) serve hot readers without materialization;
pack-time consumers (renderer, region3d placement) receive (planes, span,
dx, dy) and apply shifts during packing — `position-text-op`'s per-glyph
map-copy shift (`renderer.cljs:2204-2218`) dies. Both layout roads (shaped
`shaped-layout` AND `legacy-layout`) mint planes through the shared
constructor tail — one representation, one view code path.

**Consumers re-routed (custody, from the verified index):** renderer
`paint-msdf-line`/`paint-slug-line`/`painted-glyph`
(`renderer.cljs:2235-2276, 2141`) · region3d `pack-glyph-quads` +
`layout-placed-text` (`region3d_placement.cljc:343-363, 192`) ·
text_layout internal readers (first-glyph advance `:1299,1433,1519`;
`paint-result` `:1131`; `glyphs-in-source-range` `:641-655`) · tests (§7
S2). Connector labels store results whole but their GPU lane reads only
`:layout/id` (`connector_gpu.cljs:192`) — no change. `caret-result` /
`selection-result` / `hit-test-result` / `line-paint-ops` keep their
signatures — their internals move to plane reads.

## 6. Exact entry points

- **NEW namespace `src/app/client/workspace/text_layout_planes.cljc`** —
  plane construction, accessors, lazy views, the byte census
  (`plane-census`: exact `.byteLength`/array-length sums, CLJC so the JVM
  asserts arithmetic), the view/oracle fence helper. JVM side uses
  primitive arrays behind the same facade.
- `src/app/client/workspace/text_layout.cljc` — constructor tail rewires to
  planes (`shaped-layout` `:800-984`, `legacy-layout` `:223-266`);
  `glyph-span-index` (`:571-594`) collapses into the span plane; readers'
  internals (`:1131,1179,1299,1396,1433,1519`) re-point; cache mechanics
  UNTOUCHED (`:1013-1079`).
- `src/app/client/substrate/webgpu/renderer.cljs` (big file — thin hooks
  only): pack sites `:2204-2218, 2235-2276, 2141` call the accessor API;
  the capacity literal `:2043` → demand seed. No shader/stride edits.
- `src/app/client/workspace/runtime/render.cljs` — no structural change
  owed; R2's reference/token law is asserted here (S3); token derivations
  `:54-68` re-point at accessors if they touched glyph internals (they
  don't today — verify, don't churn).
- `src/app/client/substrate/webgpu/gpu_budget.cljs:182-184` — sampleCount
  pricing (one line).
- **NEW harness `test/render_engine/memory_receipt.mjs`** — distilled from
  `memprobe.js`/`memprobe7.js`: release build boot → settle → CDP GC ×2 →
  3 samples of heap/RSS/GPU census/plane census → adapter identity stated
  first in the output → corpus fingerprint (slot count + plane glyph
  total) asserted in the same sample (kills measure-before-load gaming).
  Release recipe pinned: `npx shadow-cljs release prod`, served exactly as
  the dev server serves `resources/public` (the harness may serve
  statically); flags as memprobe.js.
- **NEW JVM test `test/app/client/workspace/text_layout_planes_test.clj`**
  — S1's arithmetic + fence; S2's moved tripwires live in their current
  homes (`shaping_correction_test.clj`, `text_layout_test.clj`,
  `text_editing_test.clj`).
- `docs` at close: `W1/CONTRACT-T.md` gains §7.5 (§4 text, verbatim).

Untouched by law: `ground.cljs` cache/carry roads (`:178-473` — they hold
whatever `tl/layout` returns), `scene_store.cljc`, `editing_runtime.cljs`
document/session roads, `frame_scheduler.cljc`, `electric_flow.cljc`,
`connector_route.cljc`. If implementation finds one of these MUST change,
that is a finding for the thread file, not silent scope growth.

## 7. The five decisive scenarios (frozen as tripwires at close)

- **S1 — plane budget + fence (JVM, deterministic).** Lay out the fixture
  corpus (shaping-correction fixtures + one ≥10k-unit synthetic block)
  through both roads. Assert: `plane-census` ÷ code units ≤ 64; per-glyph
  ≤48B, per-cluster/span ≤16B; the lazy view reproduces the §7.1 map shape
  field-for-field against a kept map-constructor oracle (the fence); the
  stored result contains NO per-glyph persistent maps (walk it — a build
  that stores planes AND maps fails here, and B1 catches it end-to-end).
  *Named wrong build it kills:* planes added beside retained maps; views
  eagerly materialized and cached back into the result.
- **S2 — the seven readers move together (JVM).** The existing tripwire
  assertions pass with VALUES UNCHANGED VERBATIM, re-targeted through
  accessors: span-index domain checks (`shaping_correction_test.clj:616-663`,
  the `[6 7]` juxt), the `:character` selection oracle (`:496-664`, the
  `"first"` string), per-glyph ink-bounds (`text_layout_test.clj:194`, the
  `20.1`), shuffled-glyph exact index vectors (`:472-542`), caret/
  selection/hit/clip fixtures. `:layout/id` and `:receipts` hashes keep
  their semantics (same inputs ⇒ same ids as before the change is NOT
  required across the schema flip — receipts fingerprint representation
  version too — but one-result identity WITHIN a build holds: every reader
  one `:layout/id` per state, T2 law).
  *Wrong build killed:* weakened/re-valued assertions; a private glyph
  route bypassing accessors (extend the seeded-consumer fence test,
  Contract-T §7.3 style, with one seeded raw-plane reader that must FAIL).
- **S3 — retention probe with pre-registered predictions (browser).**
  Boot dev build → verify ≥60 frames rendered and non-blank screen (kills
  pass-by-broken-loop) → STOP the frame loop by its own teardown road →
  null `!scene-store`, `!layout-cache`, `!documents` → GC ×2 → assert
  residual freed by any remaining single app atom ≤ 5MB (vs 115MB), and
  post-null heap floor recorded. Pre-registered: the prev-generation
  account of §2 predicts the clock-source line vanishes once the loop is
  stopped FIRST; if residue >5MB persists, the probe's retainer path (CDP
  heap snapshot, dominator view) is taken BEFORE any fix — fence law.
- **S4 — GPU capacity + pixel identity.** Boot census: `text/content`
  created ≤ 4096×52B, grown ≤ (live-instances × 52 × 1.5) + one slack
  step; a 100-keystroke typing stress triggers ≤ 2 resizes (the
  `:2327-2333` rationale honored); [GPU-BUDGET] MSAA rows priced with
  sampleCount. 2–3 representative goldens from the existing bank
  byte-identical (no regeneration — `manifest.json` untouched except
  nothing). Adapter identity stated first in every GPU receipt.
  *Wrong build killed:* a hardcoded 200k "demand" seed (seed cap is
  explicit); goldens regenerated to pass.
- **S5 — the budget gates on release (the atom's headline receipt).**
  `memory_receipt.mjs` on the `:prod` build: B1 heap ≤100MB and B2 plane
  census ≤15MB in the same sample as the corpus fingerprint; RSS +
  task-manager-total + GPU census REPORTED beside them; dev-build numbers
  reported for continuity with the founding probes. **Floor fork,
  pre-registered:** S5 first measures the non-scene floor (heap after S3's
  clean teardown+null on the release build). If that floor alone > 85MB,
  the implementer raises the ONE fork question with the numbers (the
  atom's own deliverable — B2 — may be green while B1 is failed by
  foreign mass; Sid rules whether B1 re-scopes or a follow-on atom opens).
  If the release build cannot boot the workspace at all and the cause is
  outside this atom's entry points, that is board debt + the same fork
  question — never silent gate-substitution with dev numbers.

Close: S1–S5 freeze as the tripwire suite (focused homes:
`text_layout_planes_test.clj`, the moved assertions' current files,
`memory_receipt.mjs`, `run_verifier.mjs` goldens) + 2–3 representative
goldens. Foreign suite failures are board debt, never stops.

## 8. MUST-NOTs (real only)

- Never read `src/app/server/env.clj`.
- No reader touches raw planes outside the accessor namespace (the §4
  amendment; S2's seeded-consumer fence enforces it executably).
- No data the seven readers need is dropped to meet a budget: the full
  boot corpus stays resident; budgets are met by representation (R3).
  Eviction/viewport work in this atom is a contract violation, not
  initiative.
- `:layout/id` one-result identity (T2:358-369), the §6 layout key, and
  tagged-index domains are untouched. No second layout seam.
- Goldens byte-identical; regeneration to pass is receipt-gaming by
  definition.
- No stride/vertex-layout/shader edits (R4 refusal); no atlas or
  compositor-pool budget edits beyond the one-line sampleCount pricing.

## 9. Refusals (named, one line, routed — think wide, build narrow)

- **Viewport-scoped layout / visible-set maintenance** → the next rung;
  decided by the render-seam "where view-dependence lives" profile
  (decisions.md).
- **GPU instance stride shrink (52→~40B)** → LATER, with the Slug/MSDF
  road decision (W1/GROUND pre-registered receipt).
- **Atlas re-budget / font-asset regeneration (60.5MB)** → the same road
  decision; content-driven asset, not a code knob.
- **Compositor lease budget + the six-target zoom peak** → W4/compositor
  lane (512MiB cap already governs).
- **Layout-cache eviction policy + double-rooting cleanup** (cache and
  scene tree rooting the same values is size-free but
  eviction-complicating) → LATER, first friction wins it a contract.
- **`derive-store-frame` per-frame wrapper churn** (`:text-clips-by-vi`
  allocates per op per frame — a render-seam proportionality debt, CPU not
  memory) → board debt line at close.
- **Dev-build heap parity with release** → never a gate; reported only.

## 10. Forks seen and written (defaults chosen, per the law)

- **Ink columns kept vs recomputed:** DEFAULT kept (16B/glyph inside the
  48B budget). Recompute-at-view needs a live font handle in every view
  context (JVM tests have none). The one production consumer is
  construction-time union into line bounds; if implementation finds the
  columns pure dead weight beyond the single test, note it in the thread
  file for the next cut — do not drop them this atom.
- **Legacy road planes:** DEFAULT yes — both roads share the constructor
  tail. If a legacy-only surface (I3's counted fallbacks) resists, it may
  keep maps behind its counted fallback with a thread-file note; its
  corpus share is small.
- **`:receipts` output-hash across the flip:** DEFAULT — hashes are
  representation-versioned (bump `:text-layout/version` to 2); no
  cross-build hash equality is promised, one-result identity within a
  build is (S2).
- **The S5 floor fork** — written in §7, the one place Sid's ruling may
  be needed post-cut.

## 11. Close of the atom + the implementer's opening prompt

Close = S1–S5 frozen (tripwires + the two new receipt homes), 2–3 existing
goldens re-verified byte-identical, changed-file list diff-derived, ONE
≤15-line NOW entry in `LAYOUT-RETENTION-NOW.md` (self-audit line included),
board line flip. Acceptance = Sid's word on the S5 numbers. This atom joins
no package courtroom; it is a correction atom — its felt receipt is Sid
opening the workspace task manager and seeing the number.

---

**Implementer's opening prompt (paste into a fresh session):**

> Preflight FIRST, before the first prompt does anything else: set
> permission mode / remote-control / MCP connections NOW — never mid-session
> (prefix-rewrite law). Then boot, byte-counted (~73KB):
> `docs/render-engine/LAYOUT-RETENTION-CONTRACT.md` (28KB — THIS is the
> contract; read whole, primary) · `docs/render-engine/W1/CONTRACT-T.md`
> (5.6KB, whole) · `docs/decisions.md` lines 412–581 only (the render-seam
> constitution, ~12KB) · `docs/shaping-correction/CONTRACT.md` §4 (lines
> 123–182) + §5.1 (199–219) + §6 opening (377–436) only ·
> `.claude/skills/work-package/SKILL.md` (~7KB). Do NOT boot ground.cljs,
> renderer.cljs, or any code whole — skeleton-first, scoped windows, per
> the contract's byte-cited entry points (§6).
>
> Build the WHOLE layout-retention atom in one lane per the contract:
> planes namespace → constructor rewire (both roads) → consumer re-route
> through accessors → capacity seed + sampleCount pricing → the two
> receipt homes → S1–S5 green. Keep your own falsification pass; fix what
> surfaces in-session. Repairs collect into ONE batched edit pass, never
> fix-by-fix at depth. A genuine fork is ONE question in
> `LAYOUT-RETENTION-NOW.md` (the S5 floor fork is the pre-written one);
> route around it and keep building — never a stop. Close per contract
> §11; foreign test failures are board debt. Acceptance is Sid's word.
