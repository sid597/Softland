# SHAPER + VERIFIER — gathered digest (read-only)
DECISION SERVED (all blocks): explaining shaper + verifier plainly to a founder.

## INDEX
- A entry `tl/layout` in, one immutable versioned map out (lines/glyphs/clusters/runs + metrics + receipts).
- B one shape pass per source line chooses ALL cuts; whitespace runs are the only break opportunity; widths are HarfBuzz advances in font units, never Canvas2D.
- C planes = columnar typed arrays replacing retained glyph/cluster maps; layout-key = the cache identity that keeps one authority per address.
- D fonts.cljs fetches manifest + MSDF atlas bitmap + slug bundle + TTF bytes, and builds the HarfBuzz provider; GPU atlas texture is made in renderer.
- E text-layout fence = "no one measures privately"; shaping-correction test = the exact wrap semantic table.
- F start! -> setTimeout(0) -> adapter/device -> fonts -> build production systems -> Promise.all of 9 receipt lanes -> schema-v2 map on window.__renderVerifierResult.
- G ~30 run-*! receipts in 7 groups; goldens are render-twice-hash-compare, not stored-PNG diffs at the browser end.
- H calls tape/scheduler/graph/text-layout/family-GPU builders; does NOT call draw-frame!, draw-comparison-frame!, frame_semantic_state, scene_runtime.
- I Puppeteer intercepts every request and serves the build from a synthetic http://localhost; "152 files/0 warnings" is the shadow-cljs release compile line.
- J golden = sha256 of raw RGBA + sha256 of PNG bytes, compared to gpu-goldens/manifest.json AND to the on-disk PNG.
- K three surprises: derived-not-retained (~150MB), reference-space wrap column, verifier refuses lookalike WGSL.
- L generations: T0/T1/T2/T6-T8/T14 (Contract T), shaping-correction, SEAM-STEP1 T7, W0-A, IMAGE-ATOM, PATH ATOM, FRAME-RETENTION, W4, Region3D.

---

## A. Input -> output shapes
FACT. One entry: `(tl/layout input-map)`. Input keys: `:text :source-lines :provider :font-size
:line-height :origin :baseline-offset :inline-size :wrap-policy :wrap-col :headers :clip
:line-map :source-id :source-revision :features :variations :language :direction :tab-stops :zoom`.
`:provider` decides everything: a shaping provider (HarfBuzz) takes the T1 road, no provider takes
the legacy monospace road. Output is ONE immutable map, versioned `:text-layout/version 2`, with
`:lines` (each line: `:line/id :baseline [x y] :advance :logical-bounds :ink-bounds :glyphs :clusters
:runs :source-range :paint-source-range :consumed-range`), plus top-level `:metrics` (advance,
ascent/descent/leading, ink-bounds), `:source`, `:font`, `:shaping`, `:regime`, `:constraints`,
`:clip-plan`, `:receipts`. Each glyph carries `:glyph-id :glyph-id-kind :font-id :font-revision
:position [x y] :advance [ax ay] :offset :ink-bounds :cluster {:source-range [s e]} :character`.
Reader fns are the public surface: `measure-result wrap-result paint-result caret-result
selection-result clip-result hit-test-result copy-result line-paint-ops`; every one echoes
`:layout/id` so a consumer can prove which layout it read.
SOURCE src/app/client/workspace/text_layout.cljc:1001-1007 (entry), :919-935 (line shape), :810-833
(glyph shape), :946-995 (result map), :1121-1167 (readers).
EXTRACTION
```clojure
(defn layout
  "Produce the one immutable Contract-T result. A real provider selects T1;
   absence of a provider preserves the exact T0 compatibility road."
  [{:keys [provider] :as input}]
  (if (shaped-provider? provider) (shaped-layout input) (legacy-layout input)))
```
UNCERTAINTY glyph `:position` is baseline-relative in material-local space (`:space {:coordinates
:material-local}`); I did not trace the camera/zoom transform that maps material-local to screen.

## B. The algorithm (shaped block-greedy wrapping)
FACT. Plain words: for each source line the layout shapes the WHOLE line once, then walks the
resulting clusters left to right accumulating advance until the budget is exceeded, remembering the
last whitespace run it passed; that whitespace run is the cut. The whitespace itself is *consumed* —
it belongs to the line but paints nothing (`:paint-end` vs `:owned-end`), so copy still returns it
and the caret can still sit in it. "Break opportunity" is deliberately narrow: only U+0020 SPACE and
U+0009 TAB, never a platform `\s` class — so NBSP never breaks. If nothing fits, it makes a HARD cut
at one provider cluster (an emoji stays whole). Measurement is HarfBuzz advances in FONT UNITS —
never Canvas2D `measureText`, never a per-character width table on the shaped road. The pure
`.cljc` never measures: the provider is injected as `(:shape-line provider)`, a plain function
`text -> {:runs :glyphs :clusters :advance}`, so the same code runs in the JVM tests with a
synthetic provider. Browser measuring lives in `text_shaper.cljs` (HarfBuzz WASM + bidi-js);
`text_layout.cljc` owns all material-space placement. A wrap COLUMN is turned into a width by
shaping a single space once per layout and multiplying (`reference-advance`), which is why work
receipts count `:reference-shapes` separately.
SOURCE text_layout.cljc:399-406 (break class), :420-495 (`scan-wrap-cut`), :496-548
(`shaped-segments`), :707-729 (reference-advance / effective-inline-size); text_shaper.cljs:167-201.
EXTRACTION
```clojure
(defn- scan-wrap-cut
  "Choose one segment-relative cut from an already-shaped source line.
   A cluster is revisited at most once after the chosen whitespace boundary,
   keeping the complete cut walk linear with a <=2C visit bound."
```
UNCERTAINTY the legacy (no-provider) road still has `legacy-char-advance` = `font-size * char-width`
(0.56) — a monospace grid. Both roads live in the same file; which one production takes depends on
whether font assets resolved a `:layout-provider`.

## C. Planes and the layout key
FACT. Two different things sharing the word "layout". (1) A LAYOUT PLANE is storage: after a result
is built, `planes/compact-result` throws away the retained glyph and cluster MAPS and writes the
numbers into flat typed arrays (`position-x`, `position-y`, `advance-x`, `offset-*`, `ink-*`,
`span-source-start`, `span-glyph-start`, plus a packed `glyph-id+flags` u32 with id/kind/RTL bit
masks). Lines keep only integer ranges into those arrays; rich glyph/cluster maps are rebuilt at the
accessor boundary on demand. That is what makes a 228k-glyph document affordable. (2) The LAYOUT KEY
is identity: a three-part token — source (address + stamp + text), provider (face id/revision,
shaper id/version, features, variations, axes, fallback chain, upem, metrics), metrics (version,
font size, line height, wrap policy, wrap column, language, direction, tab stops, index space).
Paint, camera, origin, selection, hover, backend are deliberately EXCLUDED. The problem it solves:
before the correction, ground computed legacy geometry and emitted ops without the result, so the
renderer re-derived shaped layout per op every frame — the 28.7s text interval. With the key, one
address holds one current layout; `layout-cache-acquire` swaps atomically and counts
hits/misses/replacements, with an optional oracle mode that recomputes and compares.
SOURCE text_layout_planes.cljc:1-12, :225-303; text_layout.cljc:352-384 (layout-key), :1055-1084
(cache); docs/decisions.md:613-627 (the rationale).
EXTRACTION
```clojure
(defn layout-key
  "The shaping-correction keying source. Only full visible projection,
   stable address/revision, provider identity, and layout metrics enter.
   Paint, camera, origin, selection, hover, backend, and broad rebuild `sig`
   are deliberately absent (T6/T7/T8)."
```
UNCERTAINTY I did not verify which caller actually drives `layout-cache-acquire` in the live app
(the product client was cut in the below-the-waist round); the fence proves ownership statically.

## D. Fonts and how placements reach the GPU
FACT. `fonts.cljs` is a fetch orchestrator, not a font engine. It loads `/fonts/manifest.json`
(with a hardcoded fallback manifest if that fails), then per font config fires a `Promise.all` of:
the MSDF atlas PNG as an `ImageBitmap`, the atlas metrics JSON, the slug meta/curve/band binaries,
and `text-shaper/load-provider!` which fetches the primary + fallback TTF bytes and boots HarfBuzz
WASM. It returns one asset map with `:layout-provider`, `:bitmap`, `:atlas`, `:slug`, `:backend`
(`:slug` if the slug bundle is complete, else `:msdf`). No FontFace API, no opentype.js, no
glyph-advance cache. The glyph ATLAS is not built here and not built at runtime — it is a prebuilt
PNG + JSON under `resources/public/fonts/`; `renderer/init-text-system` uploads the bitmap into a
GPU texture. Placements reach the GPU through `renderer/position-text-op`, which calls `tl/layout`
(or reuses `:layout-result` already on the op), picks the line, and hands `(:glyphs selection)`
downstream; `paint-msdf-line` / `paint-slug-line` then look up ONLY coverage metadata per glyph via
`painted-glyph` and emit `{:rect :uv :color :layout/id :container}` instances.
SOURCE runtime/fonts.cljs:36-67, :155-227; text_shaper.cljs:291-324, :326-340;
renderer.cljs:1726+ (init-text-system), :2212-2259 (position-text-op), :2185-2192, :2270-2300.
EXTRACTION
```clojure
;; Placement/advance came from Contract T. MSDF selects coverage
;; metadata only; a missing glyph never changes placement.
(let [g (painted-glyph paint-map positioned-glyph)
      [x0 baseline-y] position]
```
UNCERTAINTY the slug (curve-based) text path exists alongside MSDF; I read only the MSDF paint
loop closely.

## E. The two shaper guards
FACT. `verify_text_layout_fence.mjs` protects "nobody else measures": it extracts 17 named
production functions by paren-matching, asserts each one DELEGATES to the pinned `tl/*` calls, and
FORBIDS private metric arithmetic inside them (`Math/round|ceil|floor`, dividing by `char-w`,
`count * char-advance`, `subs`, `str/split-lines`), plus a repo-wide walk of every `.clj[cs]` under
`src/app` forbidding raw plane token reads outside `text_layout_planes.cljc`. It seeds two
deliberate violations and fails if its own detectors do not reject them.
FACT. `shaping_correction_test.clj` protects the wrap SEMANTICS as an exact table: the worked
example `"abc   def"` must consume the whole internal whitespace run, leading whitespace paints,
NBSP never breaks, hard cuts advance by one cluster, and headers shape once and never wrap — with a
pinned shape-call count (5) proving the one-pass discipline.
SOURCE test/render_engine/verify_text_layout_fence.mjs:7-45, :92-156;
test/app/client/workspace/shaping_correction_test.clj:128-184.
EXTRACTION
```javascript
const privateMetricPatterns = [
  ["round/ceil/floor", /Math\/(?:round|ceil|floor)/],
  ["divide by private metric", /\(\s*\/[^\n)]{0,120}\b(?:char-w|char-advance|line-h)\b/],
  ["count-times-metric", /\(\s*\*[^\n)]{0,120}\((?:count|tl\/code-unit-count)\b/],
```
UNCERTAINTY none material.

## F. `start!` flow, step by step
FACT. (1) `start!` sets `window.__renderVerifierDone = false`, logs `[W0-A] start`, then defers the
whole run through `setTimeout 0` so the CDP console marker lands before any synchronous pipeline
compile. (2) It reads the URL query: `?region3d-floor-only` picks the narrow runner, otherwise
`run-verifier!`. (3) `run-verifier!` first runs a PURE in-process assertion (frame-retention payload
resolution with fake GPU objects), then refuses to continue unless the page is a secure context with
`navigator.gpu`. (4) `requestAdapter` -> `requestDevice` -> `fonts/load-font-manifest-async`, picks
TWO fonts by id: `dejavu-sans-mono` (MSDF/slug) and `ubuntu-sans-variable` (T1 shaping), and loads
both asset bundles. (5) It runs the T1 layout receipt on the CPU. (6) It builds REAL production
systems: `create-camera-buffer`, `create-containers-buffer`, `init-rect-system`, two
`init-text-system` instances (msdf + slug), a second isolated rect system for the affine boundary
probe, and decodes slug glyph curves. (7) `Promise.all` of nine lanes: zoom cases, shader digests,
Q5 affine boundary, image atom, path atom, connector atom, chrome atom, W4 frame runtime, Region3D
floor. (8) It assembles a `:schema-version 2` map (adapter info, device limits, canvas, font,
shader digests, every lane receipt) and `start!` publishes it as `window.__renderVerifierResult`
with `__renderVerifierDone = true`. Errors are caught and published as `{:fatal ... :stack ...}` —
the flag always flips, so the runner never hangs.
SOURCE verifier.cljs:5667-5696 (start!), :5465-5490 (guards + adapter), :5505-5560 (systems),
:5578-5620 (lanes + result map).
EXTRACTION
```clojure
(defn ^:export start! []
  (set! (.-__renderVerifierDone js/window) false)
  ;; Yield once so CDP can publish the boot marker before any browser/driver
  ;; implementation performs synchronous pipeline compilation.
  (js/setTimeout (fn [] ...) 0))
```
UNCERTAINTY none material.

## G. The receipts, grouped (verifier.cljs has 170 defns; ~30 are `run-*!`)
BASE — `run-case!` (:506) renders 7 zoom cases z0.01/0.1/1/8/10/100/1000 twice each through the real
rect + msdf + slug systems and hashes; `run-q8-transport!` (:206) container transport rows;
`run-q5-affine-boundary!` (:233) pinned-pixel coverage at an affine boundary; `shader-digests`
hashes the production WGSL sources; `t1-layout-receipt` (:2832) drives `tl/layout` + all seven
readers on a stress string with RTL, tab, ligature, combining mark, fallback glyph, and a variable
axis delta.
IMAGE — `run-image-golden-case!` (:747), `run-image-parity!` (:823, GPU vs CPU decode),
`run-color-receipts!` (:905, sRGB/linear/premultiplied), `run-arrangement-receipt!` (:1074, frame
input families), `run-lifecycle-receipt!` (:1169, create/destroy/rebuild), `run-image-atom!` (:1364).
PATH — `run-path-golden!` (:2013), `run-path-color!` (:2087), `run-path-arrangement!` (:2130),
`run-path-upload-gate!` (:2147, no redundant GPU write for an equal vector), `run-path-atom!` (:2162).
CONNECTOR — `run-connector-golden!` (:2464), `-color!` (:2558), `-arrangement!` (:2594),
`-upload-gate!` (:2615), `-label-road!` (:2636, the label goes through `connector-route` +
`tl/layout`), `run-connector-atom!` (:2698).
CHROME — `run-chrome-golden!` (:1631, 3 modes: selection z1, selection z8, marquee z0.1, with a
`max-white-run-px` handle metric and an anchor-error metric), `-upload-gate!` (:1655), `-color!`
(:1666), `-arrangement!` (:1725), `run-chrome-atom!` (:1758).
W4 — `run-w4-frame-runtime!` (:3635): dark-lane check (flag off, no receipt, no compositor before),
`frame-runtime/felt-fixture-receipt`, then a mixed nested case through `frame-graph/compile-frame-plan`,
`frame-scheduler/decide` + `replay` (replayed pixels must be byte-identical), and the compositor.
REGION3D — `run-region3d-floor!` (:5289) plus a seam-connector case (:4448) and a
`lower-resolution-pressure` case (:5024); drives `region3d-gpu/encode-region-passes!`,
`region3d-placement`, `scene-tape/paint-forward` and `pick-reverse`.
SOURCE verifier.cljs, line numbers as listed.
EXTRACTION
```clojure
(def ^:private zoom-cases
  [{:case-id "legal-min-z0p01" :zoom 0.01 :regime "legal-envelope-sentinel"}
   {:case-id "default-min-z0p1" :zoom 0.1 :regime "floor-default-clamp"}
   ... {:case-id "legal-max-z1000" :zoom 1000.0 :regime "legal-envelope-sentinel"}])
```
UNCERTAINTY the word "fence" appears zero times inside verifier.cljs — fences are the standalone
`.mjs` static checks; inside the browser they are called receipts/gates/goldens.

## H. Called vs NOT called (grep-confirmed over verifier.cljs)
- `draw-frame!` — NOT CALLED (defined renderer.cljs:3678; zero hits in verifier).
- `draw-comparison-frame!` — NOT CALLED (defined renderer.cljs:2637; zero hits).
- `scene-tape/paint-forward` — CALLED (3 sites, incl. :4733 region).
- `scene-tape/pick-reverse` — CALLED (4 sites, :4733).
- `scene-tape/compile-tape` — CALLED (8 sites: :1747 chrome, :2135 path, :2403 connector, …).
- `scene-tape/ordered-insert` — CALLED (:1742).
- `frame-scheduler/decide` — CALLED (:4986); also `replay` (:3964, :3967), `initial-state`,
  `pulse-alpha`, `receipt`.
- `frame-graph/compile-frame-plan` — CALLED (:3029, :3192, :3293). (No fn literally named
  `frame-graph/compile`.)
- `frame_semantic_state` apply-deltas — NOT CALLED (namespace exists at
  src/app/client/substrate/frame_semantic_state.cljc but is not in the verifier's `:require` list).
- `scene_runtime` anything — NOT CALLED (src/app/client/workspace/scene_runtime.cljs not required).
- `frame_runtime` — CALLED, two fns only: `flag-enabled-search?` (:3637) and `felt-fixture-receipt`
  (:3641).
- `text_layout` — CALLED: `tl/layout` (:2834, :2853, :4298) plus `measure-result wrap-result
  paint-result caret-result selection-result clip-result hit-test-result`.
- `chrome_gpu` builders — CALLED: `prepare-chrome-frame!` (:1534), `chrome-entries` (:1535),
  `execute-chrome-batch!` (:1547).
- `region3d_gpu` builders — CALLED: `region3d-entries` (:4362), `region-topology-rows` (:4376),
  `encode-region-passes!` (:4384).
- Also CALLED from renderer: `init-rect-system init-text-system init-image-system
  create-camera-buffer create-containers-buffer write-containers! update-camera update-rects
  update-text-data compile-frame-tape produce-frame-entries execute-frame-entry!
  execute-image-batch! update-frame-arrangement resolve-gpu-paint resolve-image-paint shape-text
  clone-text-system destroy-text-system! destroy-image-system!` and the four raw WGSL source vars.
SOURCE verifier.cljs:12-41 (`:require` list is the authority for the NOT-CALLED rows) + greps above.
EXTRACTION
```clojure
;; verifier ns requires — no frame-semantic-state, no scene-runtime, no frame-delta
[app.client.substrate.frame-graph :as frame-graph]
[app.client.substrate.frame-scheduler :as frame-scheduler]
[app.client.substrate.scene-tape :as scene-tape]
[app.client.workspace.frame-runtime :as frame-runtime]
```
UNCERTAINTY the verifier reaches the same GPU work by composing `compile-frame-tape` +
`execute-frame-entry!` itself rather than through `draw-frame!`; whether that is an exact behavioral
twin of `draw-frame!` is what `verify_scene_tape_fence.mjs` asserts statically, not something I ran.

## I. The Puppeteer runner and the static fences
FACT. `run_verifier.mjs` launches system Chrome (`--enable-unsafe-webgpu --use-angle=swiftshader`,
headless "chrome"), then intercepts EVERY request: `/` returns a 6-line inline HTML shell,
`/js/main.js` returns the shadow-cljs release build off disk, `/fonts/*` and `/images/*` are served
from the repo with path-escape guards, everything else 404s, and any non-`http://localhost` origin
is aborted. So there is no server process at all — the "origin" is synthetic, which is how the page
gets a secure context. It waits up to 600s for `window.__renderVerifierDone === true`, reads
`__renderVerifierResult`, camelizes the keys, evaluates seven `laneGuards` (base-renderer, image,
path, connector, chrome, frame-runtime, region3d), evaluates three representative goldens, writes
`target/render-verifier/receipt.json` with the guards, the goldens, sha256 of seven source inputs,
adapter identity, and the full browser console log, and exits 1 on any failure.
FACT. "152 files · 0 warnings" is NOT the runner — it is the shadow-cljs `release render-verifier`
compile line in the same npm script (`verify:render-engine` = text-layout suite -> proportional
shaper fence -> text-layout fence -> scene-tape fence -> shadow-cljs release -> runner). 152 is the
ClojureScript namespace count pulled into the verifier build.
FACT. `memory_receipt.mjs` is a separate harness: it serves `resources/public`, waits for 60 frames,
forces GC twice via CDP, samples three times, and gates on four numbers — release JS heap <= 100MB,
layout PLANE bytes <= 15MB (read from `window.__softlandLayoutRetention.planeCensus()`), >= 60 frames
with a non-blank canvas, and a stable corpus fingerprint across samples.
FACT. The `.mjs` static fences read SOURCE, not runtime. `verify_scene_tape_fence.mjs` extracts
`draw-frame!`, `execute-scene-tape!`, `execute-frame-entry!`, `frame-tape-twin-check!`,
`update-frame-arrangement`, `image-entries`, `execute-image-batch!`, `resolve-image-paint`,
`scene_store/pick`, `rect_tree/rt-node` and the family registry, then asserts every render family
goes through the registry and forbids a per-family `.draw pass` branch in the central executor — i.e.
"no family gets a private draw path". `verify_proportional_shaper.mjs` runs HarfBuzz in Node against
the shipped font files and asserts real typography actually happens: `office` produces a ligature
with fewer glyphs than clusters, `AV` kerns (advance differs with `kern` vs `kern=0`), a combining
accent stays one cluster, Arabic shapes RTL, a codepoint absent from the primary resolves in the
fallback face, and the `wdth` axis at 75 vs 125 changes the advance — plus atlas/slug coverage
cross-checks.
SOURCE run_verifier.mjs:22-28, :69-131, :146-215, :270-352; package.json:8-9;
memory_receipt.mjs:184-215, :309-346; verify_scene_tape_fence.mjs:45-145;
verify_proportional_shaper.mjs:45-96; docs/below-the-waist/editing-waist-map.html:691.
EXTRACTION
```javascript
// SEAM-STEP1 T7: ownership pins move to maintained names; no assertion or
// seeded self-test is deleted when the live path changes.
const requireToken = (label, source, token) => {
  if (!source.includes(token)) failures.push(`${label}: missing ${token}`);
```
UNCERTAINTY `memory_receipt.mjs` drives the PRODUCT build (`resources/public`), which the
below-the-waist cut removed the client from — whether it still runs today I did not test.

## J. How a "golden" is compared
FACT. Two layers, and neither is a per-pixel image diff. In the browser: `render-system-bytes!`
creates a 128x128 `rgba8unorm` texture, renders the real system into it, `copyTextureToBuffer` into
a mapped read buffer, and returns raw RGBA. `render-pair!` does this TWICE and sha256s both — that
is the determinism check (`:byte-identical?`). The bytes are also drawn into a 2D canvas and
`toDataURL("image/png")` gives `:png-data-url`. In Node: the runner base64-decodes that data URL and
sha256s the PNG bytes, then for three representative files requires a THREE-way agreement — current
raw hash == manifest raw hash, current PNG hash == manifest PNG hash, AND the on-disk golden PNG's
hash == the manifest PNG hash. So the manifest is the authority and the PNG on disk is the
human-lookable witness that must stay in sync with it. Goldens live in
`test/app/fixtures/render_engine/gpu-goldens/` with `manifest.json` beside them. Regeneration is not
automated in the runner (no write path for goldens, and `main()` refuses any argv) — the PNG and the
manifest row must be updated deliberately from a passing run's `receipt.json` / data URL.
SOURCE verifier.cljs:125-172 (readback), :173-190 (`render-pair!`), :105-124 (PNG data URL);
run_verifier.mjs:15-19, :132-146, :216-266, :300-310.
EXTRACTION
```javascript
const pass = Boolean(current && expected &&
  current.rawSha256 === expected.rawSha256 &&
  current.pngSha256 === expected.pngSha256 &&
  goldenPngSha256 === expected.pngSha256);
```
UNCERTAINTY I found no regeneration script; "how they are regenerated" is INFERRED from the absence
of a write path plus `if (process.argv.length !== 2) throw new Error("...one replay road...")`.

## K. Three most surprising design choices
1. NOTHING DERIVED IS KEPT — with a measured price tag in the comment. Caret stops and cluster ink
bounds are recomputed on every read rather than stored.
SOURCE text_layout.cljc:851-858
```clojure
;; Caret stops and cluster ink bounds are NOT retained:
;; both are pure functions of the retained fields
;; (logical-bounds + direction + source-range, glyphs)
;; and are derived at read time (cluster-caret-stops).
;; Measured 2026-08-08: retaining them cost ~150MB on a
;; 228k-glyph boot with zero readers outside this file.
```
2. THE WRAP COLUMN IS DEFINED BY SHAPING ONE SPACE. A column count is meaningless for a
proportional font, so the layout shapes `" "` once, calls its advance the reference unit, and
multiplies. If that reference comes back empty or zero, it records a `:provider-fault` rather than
guessing.
SOURCE text_layout.cljc:711-723
```clojure
reference-shaped (when positive-wrap-col?
                   (work+! !work :reference-shapes 1)
                   ((:shape-line provider) " " shape-opts))
effective-inline-size (cond positive-wrap-col? (when reference-units (* wrap-col reference-units)) ...)
```
3. THE TEST HARNESS IS FORBIDDEN FROM WRITING ITS OWN SHADERS. Most verification harnesses
reimplement a simplified version of the thing they check; this one bans that in its docstring, and
separately declares that its CPU point-in-path probe is a candidate, not the product picking path.
SOURCE verifier.cljs:2-11
```clojure
"Permanent W0-A browser half.
   This harness deliberately instantiates the production renderer's public
   pipeline/update functions and repository font assets. It owns only capture,
   comparison inputs, a candidate geometry-contract probe, and receipts. It is
   not a product renderer and it never substitutes lookalike WGSL.
   The CPU point-in-path probe is NOT today's product picking path."
```
(Runner-up, if a fourth is wanted: the fences seed deliberate violations into the real extracted
production source and fail if their own detectors miss them — `verify_text_layout_fence.mjs:126-142`.)

## L. Generations — which marker owns which code
- **Contract T / T0** — `legacy-provider`, `legacy-char-advance`, `wrap-line`, `block-wrap-lines`,
  `legacy-layout` (text_layout.cljc:29-311). The shipped MONOSPACE behavior, preserved byte-exact so
  the seam could land without changing pixels. `legacy-index-space` is its declared adapter boundary.
- **T1** — the shaping road: `text_shaper.cljs` whole (HarfBuzz + bidi), `shaped-layout`,
  `shaped-segments`, `shaped-provider?` (text_layout.cljc:312, :496-1000), the verifier's
  `t1-layout-receipt`, and the two guards branded `[T1-FENCE]` / `[T1-SHAPER]`.
- **T2** — the source-map/index-space replacement point (docstring at :17-19); `glyph-span-index`
  is marked "T1/T2".
- **T6/T7/T8** — what `layout-key` must EXCLUDE (paint, camera, origin, selection, hover, backend).
- **T14** — the break-class rule (SPACE/TAB only, never `\s`) at :400-401, and the scene-tape fence's
  `rt-node` ten-key shape pin.
- **shaping-correction** (the contracted correction, docs/shaping-correction/CONTRACT.md, banked in
  decisions.md:613-627) — `layout-key`, the layout cache, the work counters
  (`:glyph-visits :cluster-index-*` … `:wrap-candidate-visits`) and `within-work-bound?`, i.e.
  everything that makes construction proportional to G+C+R.
- **text-layout planes / layout-retention** — `text_layout_planes.cljc` whole, `compact-result`,
  `register-live-planes!` / `live-plane-census`, `memory_receipt.mjs`, and the fence's
  `rawPlaneTokens` list. Marked `:text-layout/version 2`.
- **SEAM-STEP1 T7** — the "pins MOVE, never delete" rule inside both static fences
  (verify_text_layout_fence.mjs:92-94, verify_scene_tape_fence.mjs:87-89).
- **W0-A** — the verifier itself: ns docstring, every `[W0-A]` console marker, the zoom cases,
  `q8-transport`, `q5-affine-boundary`, shader digests.
- **IMAGE-ATOM Package 2** (verifier.cljs:599) — image system lane, and IMAGE-ATOM T10/T12/T14 pins
  in the scene-tape fence.
- **PATH ATOM** (verifier.cljs:1833) — path lane; **connector atom** (:2212) is the round that
  produced the four-lens author pass in CLAUDE.md.
- **FRAME-RETENTION §5d** — `assert-frame-retention-payload-resolution!` and the moved producer pin.
- **W4** — `run-w4-frame-runtime!` (:3635): frame graph + scheduler + compositor, replay-identical.
- **Region3D** — :4190-5464: floor, seam-connector (:4448), lower-resolution-pressure (:5024), and
  the separate `run-region3d-floor-verifier!` entry reachable via `?region3d-floor-only`.
SOURCE as cited inline.
UNCERTAINTY the T-numbers are contract-item numbers from docs/shaping-correction/CONTRACT.md and
docs/render-engine/T2-INPUT-FLOOR-CONTRACT.md; I read the code's references to them, not the
contracts themselves, so the mapping above is from code comments only.
