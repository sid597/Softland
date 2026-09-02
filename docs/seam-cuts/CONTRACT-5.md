# Delete what no kind owned

*the third contract in `docs/seam-cuts/` · cut 2026-09-03 · cutter Fable 5.1, effort xhigh · two steps, each one session, each one accept*

*Vocabulary: `docs/decisions.md` "The render boundary" glossary. Code identifiers keep their names until the step that touches each file; a doc names the identifier in backticks and the concept in this vocabulary.*

**Ground.** Source baseline `7fe2a7b`. The path cleanup (`CONTRACT.md`) and the image, region and text cleanups (`CONTRACT-2-4.md`) are closed and accepted; the rename is closed. Those were cut per kind: each session owned one kind's files. What sits under the kinds was in no session's list, so nobody judged it: the buffer pool, the compositor, the device setup, the limits reader, the font loader, the frozen copies of old implementations, the measurement probe, the memory receipt. This contract judges it by the criterion the kinds used. It was cut from the code at `7fe2a7b` read by window (this session's anchors, `scratchpad/anchors.txt`, 171KB, every line the 2026-09-02 code-only map cited) and from `CONTRACT-2-4.md` §0–§2, §9, §10 and `NOW-2-4.md` read primary. Read code, not docs.

**Sid's rulings, verbatim (2026-09-03).**
- The criterion, restated: "we delete in this phase we going through how softland should be and any code that survies should be there only if its in the form as it should be"
- The growth law replaced: "ok" to this wording, now in `docs/decisions.md` "The render boundary": *a check survives only in the form a check should have: an independent implementation written as one, or recorded outputs. A frozen copy of the previous implementation is neither.*
- Clipping: "yeah agree only the first column" (the sort below), and "there is no concept of rectangle ... i think this is something that should be discussed at what are we missing below the waist and iff this one cuts below"
- Draw order: "undecided"; "i think this requires first figuring out how do we even handle the paint/render order given the 2d, 3d that all can exist"
- The grid text route: "yes if this is not used and not how should be the goes"
- The ask: "so do we have a contract to do further deletion?"

**The sort.** Sid's word: only the first column.

|  | changes no picture | changes a picture |
|---|---|---|
| known wrong form | delete now · **step A** | delete and re-record · **step B** |
| undecided form | delete now · **step A** | its design talk decides · untouched here |

Step A is byte-identical: every recorded picture, every byte hash, every receipt count unchanged. Step B is the grid text route alone: seven DejaVu pictures are re-recorded; the Ubuntu pictures are the fence that nothing else moved.

**Sid's touches:** this page (open) · accept of step A on its NOW lines · accept of step B on the seven before-and-after pairs.

---

## 0. Why these survived the kinds

`CONTRACT-2-4.md` §4 assigned custody per kind; §2 named what it would not touch. Everything below is either a file no heading owned (`engine/buffer_pool.cljs`, `engine/device.cljs` beyond its first 45 lines, `engine/compositor.cljs`, `engine/limits.cljs`, `engine/rungs.cljc`, `text/fonts.cljs`, `text/shaper.cljs`, the two text oracles, `harness/shaper_border_probe.cljs`, `test/render_engine/memory_receipt.mjs`) or a §2 deferral whose reason is false at HEAD (the retention census was kept for "a live reader"; the reader builds a shadow target that no longer exists). Two items are the kinds' own leftovers: the page-surface names inside text's fallback count, and the three copies of the `:!shape-rev` cargo removed from the renderers but not from the pool.

---

## 1. Scope

Each piece below is judged by form: dead means wrong form, called or not; right form for a valid future case is alive, called or not. Step A changes no picture. Step B re-records seven.

### 1a. Engine (step A)

| piece | verdict | why |
|---|---|---|
| `engine/transform.cljc`: `:effects` in the group schema (`:optional`, `:validators :effects any?`, line 52), on `root-group` (:83), in `add-group` (:126-136); `set-effects` (:138-144) | delete | a slot with no declared shape and no reader; its docstring cites a namespace that does not exist. Clipping's home is undecided (header); an undecided slot nothing reads is the first column. |
| `engine/transform.cljc`: `:layer`, `:sibling-rank` in the schema, `root-group`, `add-group`; `:stack-path` and `:layer` on the composed world transform (`compose-one` :207-212, the `world-transforms` docstring) | delete | draw order is undecided (header); no reader outside the file; `harness/shared.cljs:96` writes a 2-tuple where the tree makes 3-tuples. All three fields go, not one. |
| `engine/compositor.cljs:566` `world-transform-scale` | move to `engine/transform.cljc` | settled ground names its job (the hit builtin's slop, converted through the world-transform scale); a transform fact, not a compositor one. `Math/sqrt` compiles on both hosts. |
| `engine/device.cljs:228-271` `project-clip-rect`, `clip-execution-mode` | delete | no caller; clipping's level is undecided. |
| `engine/device.cljs:123-206` `clear-quad-shader`, `configure-clear-quad-shader`, `clear-value`, `init-clear-quad`, `create-render-target`, `destroy-render-target!`, `scene-color-resource` | delete | no caller; the compositor's target pool owns targets; "Phase 6E" code of a deleted frame path. `max-transform-nodes` stays (`create-groups-buffer` reads it). |
| the sRGB curve as numeric literals in three places: `engine/color.cljc:14-24` (CPU, both directions), `engine/device.cljs` `scene-color-wgsl` (decode), `engine/compositor.cljs` `present-fragment-shader` (encode) | replace | `color.cljc` exports the six curve constants once; both WGSL strings are built from them. Pixels identical; `shaderDigests` re-pinned once (F5). |
| `engine/compositor.cljs:715` `copy-present!`, `:499` `retire-absent-region-leases!` | delete | self-labelled legacy, no caller. |
| three byte tables: `engine/compositor.cljs:51-61` `texture-bytes` (throws on unknown, no mips), `engine/limits.cljs:17-43` `texture-bytes-per-pixel` (unknown → 4) and `texture-reserved-bytes` (mips, samples), `image/component.cljc:200-204` `texture-bytes` (mips, no format) | replace | one table (F1): format, width, height, mip levels, sample count; unknown format throws. The compositor's lease pricing and image's mip pricing call it. |
| four adapter-limit readers: `engine/limits.cljs:9-15` `snapshot-adapter-limits` (no requirer), `harness/shared.cljs:150-156` `selected-limits`, `region3d/on_plane_renderer.cljs:168-171` `device-ink-vertex-limit` (direct read, hardcoded 268435456 fallback), `engine/compositor.cljs:19` `region-lease-max` 4096 beside `region3d/renderer.cljs:790` `max-lease-size` (destructured, never passed) | replace | one reader (F1). The harness and the on-plane renderer call it. The lease ceiling is the smaller of `region-lease-max` and the adapter's max 2D texture dimension, read once at compositor creation and passed as `max-lease-size`. On this machine the adapter's number exceeds 4096, so no picture changes; S1 is the fence and NOW records the number. |
| `engine/buffer_pool.cljs`: the raw index API (`allocate-buffer-index!`, `free-buffer-index!`, `update-buffer-index!`, :69-110), `keyed-diff-update-pool!` (:112-167), `ordered-diff-update-pool!` (:169-219), `pool-draw-info` (:259-276), the handle API (`allocate-handle!`, `validate-handle`, `update-handle!`, `free-handle!`, :278-326), `bump-shape-on-count-boundary!`, and the state only they use (`:generations`, `:free-buffer-indexes`, `:active-buffer-indexes`, `:id->buffer-index`, `:ordered-ids`, `:prev-keyed-items`, `:frame-input/identity`, `:!shape-rev`) | delete | five write paths for one buffer; one caller, `batch-update-pool!` from `image/renderer.cljs:659` (F2). The `:!shape-rev` cargo was removed from three renderers by CONTRACT-2-4; this is the fourth copy. |
| `engine/rungs.cljc:12-16` `lease-shadow?` | replace | reads `:shadow?` then falls back to `:shadow`; no lease carries a boolean `:shadow` (the compositor writes `:shadow?`, `compositor.cljs:645-652`; the `:shadow` at `region3d/renderer.cljs:490` is a role name in `:dirty-by-role`). Read `:shadow?`. |

### 1b. Text (step A)

| piece | verdict | why |
|---|---|---|
| `text/layout_oracle.cljc` (600 lines), `text/shaper_oracle.cljs` (209) | delete | frozen copies of the previous implementation; the replaced growth law. The layout copy refers `break-whitespace-at?`, a break rule, from the file it checks (`layout_oracle.cljc:14-16`). |
| `text/shaper.cljs:19` the require, `:373-375` `:shape-line-oracle` on the provider, `:8, :336` the docstring lines | delete | the oracle's only production road. |
| `text/renderer.cljs`: `pack-instances-oracle` (:696-720), `shape-text` (:608-618), `paint-slug-line` (:447-604), `pack-slug-instances!`; `line-offsets-for` only if it becomes uncalled | delete | the map-route packer kept beside the flat one for the F3 fence; the same class as the oracles. `tl/glyph-views` stays while a reader remains. |
| `harness/text.cljs`: the F1, F2, F3 fences (`:textFlatRoute`, :404-540), the `shape-text` call (:278), every read of `:shape-line-oracle`, `pack-instances-oracle`, `layout-oracle/layout`; `test/render_engine/run_verifier.mjs:193-201` the `textFlatRoute` gate and `:464` its echo | delete | the recorded goldens and the T1 tree hash are the check. The closer appends one line to `NOW-2-4.md`: "T2/T3's F1–F3 text fences retired by CONTRACT-5; the recorded goldens are the check." |
| `test/app/client/text/flat_route_test.clj`: the oracle comparison (`oracle/layout`, :57 and its assertions) and the require | delete | same. `i2-oracle-mode-sees-through-planes` stays: it calls `tl/layout` against itself; the layout cache is not this contract's (§2). |
| `text/renderer.cljs`: `no-layout-fallbacks` (:494-495), `sum-fallbacks`'s per-surface merge (:497-500), the `surface` parameter through `position-text-draw-item` (:503), `position-text` (:555), `pack-instances-flat` (:727), `update-text-data` (:752), the `:layout/surface` read (:511) | replace | page names inside the engine; one integer under `:fallbacks` (F3). `harness/text.cljs:545-547, 652` read the integer. |
| `text/fonts.cljs`: `install-font-watch!` (:218-241), `manifest-defaults->settings` (:15-23), `font-defaults->settings` (:25-35), the "TEMP mobile-boot probe" `probe` (:77-88) and its three wraps (:128, :131, :158), the hardcoded fallback manifest in the manifest loader's catch (:52-70), the `:settings-keys` log (:45) | delete | the settings page is gone; a second copy of the font manifest is two truths; a timing probe is an instrument. `with-retry` stays. A manifest that fails to load fails the boot, as a failed default font already does. |
| `resources/public/fonts/manifest.json`: every `defaults` block and the top-level `settings` block | delete | the settings page's vocabulary in a font manifest; no reader after the line above. (`charWidth` goes in step B.) |
| `text/layout_planes.cljc:735-772`: `!live-plane-refs`, `register-live-planes!` with its `declare` (:16) and call site, `live-plane-coverage-check`, the `__softlandLayoutRetention` install; the docstring's "Holds" line; `test/render_engine/memory_receipt.mjs` whole | delete | the receipt builds `release prod`, a target absent from `shadow-cljs.edn`, and reads `window.sceneFaces` and `__softlandFrameSchedulerReceipt`, which exist only in stale bundles of the deleted app. `plane-coverage-check` (:492) stays if a reader remains. |

### 1c. Region and image (step A)

| piece | verdict | why |
|---|---|---|
| `region3d/on_plane.cljc:17-18` `placed-ink-version`, `placement-pack-version`; `:37-45` `provider-identity`, `session-layout-key` | delete | no reader; the identity list is a verbatim copy of layout's. `placed-color-adapter-version` stays with its one use in `adapt-legacy-color` (CONTRACT-2-4 F3). |
| `region3d/on_plane_renderer.cljs:238` `_options`; `region3d/renderer.cljs:876-877` the `{:font-assets :session-layout-snapshot}` map passed into it | delete | an argument that promises text support and drops it. The `:unsupported-kind` branch stays with CONTRACT-2-4 §2's deferred question; its status is returned in `:coverage-check`, so it is counted, not hidden. |
| `region3d/on_plane_renderer.cljs:168-171` `device-ink-vertex-limit` | replace | calls the one adapter-limit reader (1a); the hardcoded fallback goes. |
| `region3d/renderer.cljs:769-776` `region-encode-step`, `region-encode-rung`, `quantize-region-encode-scale` | rename | "rung" is the budget ladder in `engine/rungs.cljc` (÷1 2 4 8); this is a zoom bucket of step 1.12. `encode-scale-step`, `encode-scale-bucket`, `quantize-encode-scale`. Names only, in the file this step touches. |
| `region3d/scene.cljc:748-753` `pick-region`'s synthesized camera | replace | the camera is an input (the render boundary's second axis). The default assumes the composite rect's aspect equals the extent's; a pick mis-aims when they differ. `pick-region` requires `:camera`; every caller passes the camera it rendered with. |
| `test/app/client/region3d/oracle.cljc` → `src/app/client/harness/region_oracle.cljc`, ns `app.client.harness.region-oracle` | move | not a frozen copy. `scene-equivalent?` is a check in the right form: the incrementally maintained scene against the full rebuild, calling the production full rebuild. `shade-reference` is an independent CPU implementation of the lighting law. `src/` stops requiring `test/` (`harness/region.cljs:15`, which compiles only because `deps.edn` `:dev` adds `test`). `scene_test.clj` and `harness/region.cljs` require the new namespace (F4). |
| `image/component.cljc:69-71` `source-cache-key`, `:117-122` `component-cache-key`; `test/app/client/image/component_test.clj:81` | delete | a `lod` argument copied from path's zoom regime (`path/tessellation.cljc:35-37`); nothing produces an image lod; the frame gate is `frame/frame-key`. |
| `image/component.cljc:113-115` `canonical-component` | replace | sorts one level; nested `:image/rect`, `:image/crop`, `:image/paint` stay insertion-ordered, so a printed row is not canonical. Sort every nested map, matching path's canonicalizer (the implementer confirms path's recurses; if it does not, both do after this). |

### 1d. Harness, build, files (step A)

| piece | verdict | why |
|---|---|---|
| `harness/shared.cljs:96` `q8-world-transforms` writes `:stack-path`, `:layer`; `test/app/client/engine/transform_test.clj:76-82` expects them | delete the keys | with 1a. |
| `harness/shared.cljs:150-156` `selected-limits` | replace | calls the one reader (1a); the harness report keeps its six keys. |
| `harness/shaper_border_probe.cljs` → `test/app/client/harness/shaper_border_probe.cljs`, ns unchanged | move | an instrument; "never a product path" by its own docstring. `shadow-cljs.edn` is unchanged: its build names the ns and `:dev` puts `test` on the classpath. `test/render_engine/profile_shaper_border.mjs` is checked to still serve it. |
| `resources/electric-manifest.edn`; `resources/public/font_atlas.json.bak`, `resources/public/font_atlas.png.bak` (tracked) | delete | the deleted app's. |
| untracked in the working tree: `resources/public/font_atlas.json.pre-b2-regen.bak`, `resources/public/fonts/*.pre-d7.bak` (four), `resources/public/js/` (stale `main.*.js` bundles and `cljs-runtime/`) | remove | never in git; NOW lists what was removed. |
| comment lines across `src/app/client` citing deleted contracts: `Contract-[A-Z]`, `Contract [A-Z]\b`, `W[0-9]-[A-Z]`, `Phase [0-9]`, `Q8`, `frame-effects`, `plan compile`, `SHAPER-BORDER.md`, and `\bT[0-9]{1,2}\b` where it is a contract tag (about 94 lines on 2026-09-02) | rewrite in plain words or delete | the reader has no such contract to open; every one is a wrong pointer. Docstrings keep their nouns: "Contract-T result" becomes "the layout result", "Contract O's third slot" is deleted with the field. |

### 1e. The grid text route (step B, after step A's accept)

What it is, plain: the shaper reads a font file to know each letter's real width. DejaVu's manifest entry lists its Slug curve data but no font file, so the shaper skips it and its letters are placed on a grid of font size times 0.60, a made-up number; two other files say 0.56 and 0.52. DejaVu is monospace, so the grid looked fine. Ubuntu Sans lists its file and goes through the shaper. `dejavu_sans_mono.ttf` is on disk in `resources/public/fonts/`.

| piece | verdict | why |
|---|---|---|
| `resources/public/fonts/manifest.json`: the DejaVu entry gains `"font": "dejavu_sans_mono.ttf"`; `charWidth` leaves both entries | replace | the shaper reads DejaVu's real advances; the made-up width goes. |
| `text/layout.cljc`: `legacy-provider` (:26), `legacy-char-advance` (:84), `legacy-char-advance-step` (:95), `wrap-line` (:103), `block-wrap-lines` (:129), `visual-lines` (:148), `input-id` (:166), `legacy-layout` (:171-307), the route switch in `layout` (:1094-1100), `shaped-result?` (:1291) and the four grid branches in `caret-result` (:1406), `selection-result` (:1457), `clip-result` (:1483), `hit-test-result` (:1618) | delete | two layouts for one definition; the second uses a width no font declares. `legacy-index-space` and `index-space-token` are the UTF-16 offset domain both routes declare; they stay, renamed `index-space` and `index-space-token`. |
| `text/renderer.cljs`: the `char-width` parameter and its 0.56 defaults (:503, :555, :612, :701, :727, :752), `tl/legacy-char-advance-step` (:518), `:char-advance` in the layout input | delete | inputs the shaped route never destructures. |
| `text/fonts.cljs:57, 76` the `:charWidth` fallbacks; `harness/text.cljs:280, 287, 518` `:char-width 0.60` | delete | with it. |
| `region3d/on_plane.cljc:56-60` `carried-advance` and its `:char-advance`; `text-layout/first-glyph-advance-x` if uncalled after | delete | feeds the deleted input. |
| `test/app/client/text/layout_test.clj`: `t0-legacy-provider-zero-diff-corpus` (:119), `t0-ground-wrapper-is-behavior-identical` (:153); any other test that calls `layout` without a provider | delete | tests of the deleted route; the file already carries a fake shaped provider (:51) for the rest. |
| the seven DejaVu goldens in `test/app/fixtures/render_engine/gpu-goldens/manifest.json` | re-record once, one commit | positions move from the grid to real advances. The before and after PNGs sit side by side under `test/app/fixtures/render_engine/gpu-goldens/step-b-before/` for Sid's accept, then the before set is deleted at accept. The Ubuntu goldens are the fence: byte-equal. |

---

## 2. Refusals (each routed, none a void)

- **Does not decide clipping.** The GPU cut call (`compositor/apply-scissor!`, called at `text/renderer.cljs:819-822` and `compositor.cljs:591`), text's clip input and `clip-result`'s shaped branch run today and stay. Sid: "only the first column". The level clipping lives at is the below-the-waist talk.
- **Does not decide draw order.** List order runs. The tree's three order fields go because nothing reads them; the 2D-with-3D question is its own talk (Sid: "undecided").
- **Does not touch the layout key, the layout cache, or its `:oracle?` mode.** Shaping-correction's; the mode is the cache's self-check calling the same function, which the layout row boundary step judges.
- **Does not change what `:layout/id` hashes** (zoom, clip, origin ride in it). The layout row boundary step, LATER (CONTRACT-2-4 §2).
- **Does not remove `:!source-bytes`** (needs the wire), **nor move ids and revisions off content rows** (needs the store), **nor unify the client schema engine with the server's** (needs the dependency ruling). All LATER by CONTRACT-2-4 §2 and §1.
- **Does not unify the shadow constants.** `SHADOW_OFFSET` 0.0015 is a depth bias in NDC (`region3d/renderer.cljs:85, :136`); the oracle's 1.0e-4 is a world-space ray-origin offset (`oracle.cljc:122`). Two quantities, one word.
- **Does not unify the no-ink sentinels.** `shaped_line.cljc` packs ink width into an integer column (-1); the planes into a float column (NaN). Two storages, one conversion between them; the conversion is the seam, judged with the layout row boundary.
- **Does not change the budget constant** (`default-pool-budget-bytes`, 512 MiB). WebGPU exposes no total-memory limit; a policy row later.
- **Does not touch `test/resources/code-atom/*`** (server import fixtures) **nor `rungs_test.clj`'s injected `lease-bytes`** (a fixture for a pure function that takes its pricing as an argument).
- **Does not rename beyond 1c's three and 1e's two.** The glossary rule: identifiers rename in the step that touches each file, never a sweep.
- **Does not run a felt pass.** Sid's standing word: no driver but the test code; step B's seven pairs are the felt receipt.

---

## 3. Laws (pointers, never restated)

- `docs/decisions.md` "The render boundary": the dead criterion (fails S2 when violated) · the replaced growth law (fails S4) · "the color lives once" (fails S3) · the two axes, camera as input (fails S5's pick line) · the ownership of order and GPU memory (1a's reader and ceiling).
- `CLAUDE.md` Source Structure: kind → engine, never engine → kind; `src/` never requires `test/` (fails S4).
- `CLAUDE.md` Token Economy; memory `feedback-parallel-sessions-shared-branch-git` (exact paths, foreign dirt preserved); memory `feedback-commits-one-branch-docs-local` (one commit per milestone, big enough to read as one change).

---

## 4. Entry points and custody

**Step A, one session, three milestones, one commit each.** Every file in §1a–§1d is this session's. Shared one-line files: `docs/seam-cuts/NOW-5.md` (its own), one appended line in `docs/seam-cuts/NOW-2-4.md`, the board pointer in `docs/next-prompt.md` at close.

- **A1 engine:** `engine/{transform.cljc, device.cljs, compositor.cljs, buffer_pool.cljs, limits.cljs → limits.cljc, rungs.cljc, color.cljc}`, `image/component.cljc` (the byte table call), `region3d/on_plane_renderer.cljs` (the limit call), `region3d/renderer.cljs` (`max-lease-size` wiring), `harness/shared.cljs` (`q8-world-transforms`, `selected-limits`), `test/app/client/engine/transform_test.clj`, the goldens manifest's `shaderDigests` (re-pin, F5).
- **A2 text:** `text/{layout_oracle.cljc, shaper_oracle.cljs}` (deleted), `text/{shaper.cljs, renderer.cljs, fonts.cljs, layout_planes.cljc}`, `harness/text.cljs`, `test/render_engine/{run_verifier.mjs, memory_receipt.mjs}`, `test/app/client/text/flat_route_test.clj`, `resources/public/fonts/manifest.json` (blocks only).
- **A3 region, image, files:** `region3d/{on_plane.cljc, on_plane_renderer.cljs, renderer.cljs, scene.cljc}`, `test/app/client/region3d/oracle.cljc` → `src/app/client/harness/region_oracle.cljc`, `harness/region.cljs`, `test/app/client/region3d/scene_test.clj`, `image/component.cljc`, `test/app/client/image/component_test.clj`, `harness/shaper_border_probe.cljs` → `test/app/client/harness/`, the tracked and untracked files of 1d, the comment scrub across `src/app/client`.

**Step B, one fresh session after step A's accept.** `text/{layout.cljc, renderer.cljs, fonts.cljs}`, `region3d/on_plane.cljc`, `harness/text.cljs`, `resources/public/fonts/manifest.json`, `test/app/client/text/layout_test.clj`, the seven DejaVu golden PNGs and their manifest rows.

Never, either step: `CLAUDE.md`, `CLAUDE-1.md`, `.claude/memory/*`, `docs/decisions.md`, `shadow-cljs.edn`, `deps.edn`, any file under `src/app/server`, `test/resources/`. The `.claude/memory/` dirt in the working tree is a sibling's; preserve it, never stage it.

---

## 5. Decisive scenarios (frozen as tripwires at close)

**S1 — no picture changed.** `npm run verify:render-engine` PASS on the same adapter as `NOW-2-4.md` (Google SwiftShader, named first): `6/6` guards, every representative golden and all `7/7` DejaVu goldens byte-equal to their PNGs, the T1 tree hash `6160b2a7…be30` unchanged, the image and region receipt counts equal to `NOW-2-4.md`'s; `shaderDigests` re-pinned exactly once, in the F5 commit, with the pixel equality in the same receipt. Wrong build that passes a weaker test: the F1–F3 fences deleted and "fences gone" reported as evidence; the golden replay is the evidence, the fences were never it. Second wrong build: `max-lease-size` wired on a machine whose adapter reports under 4096, so lease sizes shrink and region pixels soften; NOW carries the adapter's number and S1's byte-equality catches the shrink.

**S2 — nothing dead by name remains.** This grep over `src/` and `test/` returns nothing: `set-effects|:effects|:stack-path|:sibling-rank|project-clip-rect|clip-execution-mode|init-clear-quad|create-render-target|scene-color-resource|copy-present!|retire-absent-region-leases!|keyed-diff-update-pool!|ordered-diff-update-pool!|allocate-handle!|pool-draw-info|:!shape-rev|frame-input/identity|bump-shape-on-count-boundary!|texture-bytes-per-pixel|texture-reserved-bytes|snapshot-adapter-limits|selected-limits|install-font-watch!|manifest-defaults->settings|font-defaults->settings|FONT/PROBE|layout-oracle|shaper-oracle|shape-line-oracle|pack-instances-oracle|paint-slug-line|pack-slug-instances!|__softlandLayoutRetention|register-live-planes!|live-plane-coverage-check|session-layout-key|placed-ink-version|placement-pack-version|placement-pack|settings-panel-text|combined-text-draw-items|layout/surface|source-cache-key|component-cache-key.*lod|region-encode-rung|memory_receipt`, plus the 1d comment regex over `src/app/client`. `git ls-files` shows none of the deleted files. Wrong build that passes a weaker test: a rename in place of a deletion (`legacy-` prefixed, `-old` suffixed, commented out); the falsification round reads the diff for moved bodies, and the grep includes the bodies' distinctive strings above, not only names.

**S3 — one truth each.** Over `src/`: `defn texture-bytes` has one definition, in `engine/limits.cljc`; the literals `0.04045`, `0.0031308`, `12.92`, `1.055`, `0.055`, `2.4` each appear once, in `engine/color.cljc`; `maxBufferSize` and `maxTextureDimension2D` are read in one place; `lease-shadow?` reads one key. JVM: the image mip pricing assertions in `component_test.clj` pass against the one table; a new `limits_test.clj` pins the table's four formats, the mip sum for 512×512 with 10 levels, and the throw on an unknown format. Wrong build that passes a weaker test: the compositor keeps its own table under a new name; the `defn` count and the literal count catch it.

**S4 — the one surviving check is in the right form, and `src/` requires no `test/`.** `scene_test.clj`'s three `scene-equivalent?` assertions and `harness/region.cljs`'s `shade-reference` call run from `app.client.harness.region-oracle` under `src/`; `grep -rn "app.client.region3d.oracle" src test` is empty; the region JVM shard is green; the region lit receipt equals `NOW-2-4.md`'s. Wrong build that passes a weaker test: the region oracle deleted with the other two, and `scene_test`'s three assertions deleted with it; the assertion count is pinned here.

**S5 — the harness's own dead is gone and its receipts stand.** `q8-world-transforms` writes only `:affine :flags :buffer-index`; the Q8 transport rows in the receipt are unchanged; `pick-region` throws a named rejection without `:camera` (one JVM assertion) and the harness's pick receipt is unchanged with the camera passed; `:fallbacks` is an integer in every pack return and the harness fence reads it (carried draw items 0, uncarried 1 per draw item, as T2). Wrong build that passes a weaker test: `pick-region` given a default camera again under a new name; the assertion demands the throw.

**SB1 (step B) — DejaVu goes through the shaper and only DejaVu moves.** The harness report names DejaVu's provider by face from the font file, not `legacy/monospace`; the pack reports zero unresolved glyphs for every DejaVu case (if no such count exists, the pack returns one, call-local, beside `:fallbacks`); the seven DejaVu goldens are re-recorded in one commit with the before set kept for Sid; every Ubuntu golden and the T1 tree hash are byte-equal. Wrong build that passes a weaker test: all goldens re-recorded together, hiding an Ubuntu regression; the Ubuntu byte-equality is the fence. Second: DejaVu's Slug glyph ids and the font file's HarfBuzz ids disagree, so the right advances place the wrong outlines; the unresolved-glyph count and Sid's eye on the pairs catch it.

**SB2 (step B) — one route.** `grep -rn "legacy\|char-width\|char-advance\|charWidth" src resources/public/fonts test/app/client/text` returns nothing but `index-space`'s renamed lines; the text JVM shard is green with the two grid tests deleted; `layout` without a shaped provider throws a named rejection (one assertion). Wrong build that passes a weaker test: the grid route kept as a fallback "for fonts without a file"; the throw is the fence.

---

## 6. MUST-NOTs (real only)

- Never read `src/app/server/env.clj`.
- No server, Rama, `/mnt/data/rama`, archive, or durable-data mutation.
- Never a Co-Authored-By line; exact paths staged; never `CLAUDE.md`, `CLAUDE-1.md`, `.claude/memory/*`, `docs/decisions.md`, `shadow-cljs.edn`, `deps.edn`.
- Step A re-records no golden and edits no PNG. Step B re-records DejaVu's seven and nothing else.
- Neither step edits the layout key, the layout cache, the shaped layout algorithm, or the four reading functions beyond deleting their grid branch (step B).

---

## 7. Forks written, with defaults

- **F1, the one byte table and the one limit reader.** Default: `engine/limits.cljs` becomes `engine/limits.cljc`, holding `texture-bytes` (format, width, height, mip levels, sample count; unknown format throws) and `adapter-limits` (cljs, reader conditional; the four keys today plus max 2D texture dimension). The alternative, `device.cljs`, loses image's JVM mip tests; the alternative of deleting `limits` and leaving the compositor's table loses mips.
- **F2, which pool write path survives.** Default: `batch-update-pool!`, the called one; the image receipt pins per-instance write counts (`NOW-2-4.md`, I3). The alternative, the ordered keyed diff, was the 2026-09-02 map's pick for its shape; it changes count semantics and compares with `=` either way. The revision compare replaces `=` when the pool takes rows: LATER.
- **F3, the fallback count.** Default: one integer under `:fallbacks`. The alternative, keep the map with one key, keeps a page word.
- **F4, the region check's home.** Default: `src/app/client/harness/region_oracle.cljc`, both functions. The alternative, leave it in `test/` and move the harness's lit receipt into a JVM test, loses the receipt: it needs the GPU.
- **F5, the sRGB fold.** Default: `color.cljc` exports the six constants; both WGSL strings interpolate them; `shaderDigests` re-pinned once in that commit with S1's pixel equality beside it. The alternative, leave the two shader strings, kept the digest pin as its only reason.
- **F6, DejaVu's shaping options (step B).** Default: the manifest entry gains only `"font"`; features and language default as Ubuntu's do. The Slug curve data stays.
- **F7, `pick-region` without a camera.** Default: a named rejection. The alternative, a default from the extent, is the assumption being removed.

---

## 8. Close

**Step A** freezes S1–S5 as tripwires (the S2/S3/SB greps as one JVM test that shells them or as `bin/` lines the receipt runs; the adapter named first), keeps its goldens, writes ≤10 lines at the top of `docs/seam-cuts/NOW-5.md` (baseline, the three closing commits, the receipt line with the adapter's max texture dimension, the untracked files removed), appends the one retirement line to `NOW-2-4.md`, flips the board pointer, commits; after that no edit to source, tests or tooling. Custody check: `git diff --name-only 7fe2a7b..HEAD` shows only §4's step-A files plus the three shared lines. The accept session amends one CLAUDE.md line (Source Structure: the probe is a test-side build under `test/`). One accept, Sid's word.

**Step B** boots fresh after that accept, closes on SB1–SB2, appends its lines to `NOW-5.md` under "Step B" with the path to the seven pairs, commits; Sid's accept on the pairs; the before set is deleted at accept.

Foreign suite failures are debt (known: four server namespaces red since `81f8c67`, `NOW-2-4.md`).

---

## 9. Basis

**Alternatives considered, and why each lost.**
- Delete the region oracle with the other two. Lost: `scene-equivalent?` calls production code to compare the incremental scene against the full rebuild; that is the check form the new law names. Only the frozen copies fall.
- Keep `engine/limits.cljs` dead, fold pricing into the compositor. Lost: image's mip pricing is JVM-tested and needs cljc; the namespace's job ("what the device allows, what a texture costs") is coherent. Wrong form only in having no reader.
- Keep the ordered keyed pool path, the 2026-09-02 map's pick. Lost: write counts are pinned by an accepted receipt; the shape gain is nil while the compare is `=`.
- Leave the two WGSL sRGB strings for the digest pin. Lost to "the color lives once"; a digest pin is a recording, re-pinned with its reason.
- Three parallel sessions. Lost: every milestone touches the harness files; two sessions in one file is the corpse in memory. One session, three commits.
- Put the grid route in step A. Lost: it re-records seven pictures; Sid's word was the first column.
- Delete text's clip path and `apply-scissor!` under the criterion. Lost: it runs and draws today's pictures; the level is undecided; the second column waits for its talk.
- Delete `pick-region`. Lost: a pick is a valid future case; only its default camera is wrong form.
- Unify the shadow epsilons and the no-ink sentinels as the map proposed. Lost on fact: different quantities, different storages (§2).

**Holes left on purpose.** Clipping (undecided, the below-the-waist talk). Draw order across 2D and 3D (undecided, its own talk). What `:layout/id` hashes and the layout cache's self-check mode (the layout row boundary step). The image byte cache (the wire). Ids and revisions on content rows (the store). One schema engine (the dependency ruling). The budget constant (a policy row). The sentinel conversion (the layout row boundary).

**What the cutter would push back on.** Keeping any pool path "because it is better" (F2 is pinned). Re-recording any golden in step A for any reason. Replacing the F1–F3 fences with a new fence rather than the goldens. A rename in place of a deletion (`legacy-` stays legacy). Scrubbing a docstring to nothing instead of to plain words. Wiring `max-lease-size` on a machine under 4096 and reporting PASS on softened pixels.

**Least sure.** (a) The `max-lease-size` wiring depends on the adapter's max texture dimension exceeding 4096 on the build machine; if it does not, the implementer leaves the wiring out with one NOW line and S1 stands. (b) Whether `plane-coverage-check` has a reader beyond the census; if not it goes too. (c) Step B: DejaVu's Slug meta may have been generated from a different DejaVu build than the ttf, so glyph ids could disagree; SB1's unresolved count and Sid's eye are the only fences. (d) Deleting the fallback manifest assumes every road serves `manifest.json`; the render test harness does; any other road fails loudly, which is the intended form. (e) `first-glyph-advance-x` and `line-offsets-for` may keep a reader; each stays if so. (f) The comment scrub's regexes will hit a few true positives that are not contract tags (`T1` as a scenario name in a harness string); the implementer reads each hit.

---

## 10. The starters (paste-able, Codex lane)

### 10a. Step A

```
Preflight, before the first prompt: permission mode, remote-control and MCP set now; never mid-session.

Three milestones in one session, booted one at a time; the artifact under repair in each is read
whole, the rest by skeleton (`grep -n "^(def"`) then the windows named. Contract first, PRIMARY:
  docs/seam-cuts/CONTRACT-5.md   header, §0, §1 for the milestone, §2, §4, §5, §6, §7, §8, §9

  Milestone A1, engine (about 70KB):
  src/app/client/engine/transform.cljc                        13KB   whole
  src/app/client/engine/device.cljs                           12KB   whole
  src/app/client/engine/buffer_pool.cljs                      15KB   whole
  src/app/client/engine/limits.cljs                            2KB   whole
  src/app/client/engine/color.cljc                             3KB   whole
  src/app/client/engine/rungs.cljc                             4KB   lines 1-20
  src/app/client/engine/compositor.cljs                       35KB   lines 15-65, 355-370, 495-510, 560-570,
                                                                     640-660, 710-760
  src/app/client/image/component.cljc                         13KB   lines 195-215
  src/app/client/region3d/on_plane_renderer.cljs              14KB   lines 160-190
  src/app/client/region3d/renderer.cljs                       56KB   lines 785-800
  src/app/client/harness/shared.cljs                          10KB   lines 85-100, 145-160
  test/app/client/engine/transform_test.clj                    4KB   whole

  Milestone A2, text (about 75KB):
  src/app/client/text/renderer.cljs                           42KB   skeleton; then 440-460, 485-520, 540-560,
                                                                     600-720, 745-760
  src/app/client/text/shaper.cljs                             18KB   lines 1-25, 330-380
  src/app/client/text/fonts.cljs                              11KB   whole
  src/app/client/text/layout_planes.cljc                      33KB   lines 1-20, 485-495, 730-772
  src/app/client/harness/text.cljs                            35KB   skeleton; then 270-290, 400-560, 640-670
  test/render_engine/run_verifier.mjs                         16KB   lines 185-205, 455-470
  test/app/client/text/flat_route_test.clj                     8KB   whole
  resources/public/fonts/manifest.json                         2KB   whole

  Milestone A3, region, image, files (about 45KB):
  src/app/client/region3d/on_plane.cljc                        8KB   lines 1-80
  src/app/client/region3d/on_plane_renderer.cljs              14KB   lines 230-270
  src/app/client/region3d/renderer.cljs                       56KB   lines 760-800, 870-880
  src/app/client/region3d/scene.cljc                          39KB   lines 740-760
  test/app/client/region3d/oracle.cljc                         7KB   whole (it moves)
  src/app/client/harness/region.cljs                          56KB   lines 1-40, 465-480
  test/app/client/region3d/scene_test.clj                     10KB   lines 80-90, 135-170
  src/app/client/image/component.cljc                         13KB   lines 60-125
  test/app/client/image/component_test.clj                     8KB   lines 75-90

You own the whole step: build, surface bugs, fix before source freeze. It changes no picture: S1 is
byte-identical and is the evidence. Sid's word: "any code that survives should be there only if its
in the form as it should be"; §1 is the list; call count is never the criterion; a rename is not a
deletion. Plan: OWNER you · SOURCE this contract · DONE WHEN S1-S5 are tripwires and NOW-5.md's top
lines are committed. Ambiguity takes §7's default plus a note; a genuine fork is one question in
NOW-5.md, routed around while unblocked work continues. Commits: three, one per milestone, each big
enough to read as one change, the message saying what went and why; never a fix-by-fix tail (Sid,
2026-09-02: "too small to know anything"). On main, exact paths, no Co-Authored-By. Do not stage
CLAUDE.md, CLAUDE-1.md, docs/decisions.md, shadow-cljs.edn, deps.edn, or anything under
.claude/memory/; the memory dirt in the tree is a sibling's, preserve it.
```

### 10b. Step B (paste after Sid's accept of step A; one fresh session)

```
Preflight, before the first prompt: permission mode, remote-control and MCP set now; never mid-session.

Boot, by seam, sizes first (about 70KB; layout.cljc by window, never whole):
  docs/seam-cuts/CONTRACT-5.md   header, §1e, §2, §4 step B, §5 SB1-SB2, §6, §7 F6, §8, §9   read PRIMARY
  docs/seam-cuts/NOW-5.md                                      2KB   whole
  src/app/client/text/layout.cljc                             79KB   skeleton; then 15-35, 80-175, 300-310,
                                                                     1090-1100, 1285-1295, 1400-1410,
                                                                     1455-1490, 1615-1625
  src/app/client/text/renderer.cljs                           40KB   skeleton; then 500-520, 550-560, 605-620,
                                                                     695-705, 720-730, 745-760
  src/app/client/text/fonts.cljs                               9KB   lines 50-80
  src/app/client/region3d/on_plane.cljc                        8KB   lines 45-80
  src/app/client/harness/text.cljs                            30KB   lines 270-290, 510-525
  resources/public/fonts/manifest.json                         2KB   whole
  test/app/client/text/layout_test.clj                        11KB   whole
  test/app/fixtures/render_engine/gpu-goldens/manifest.json    4KB   the DejaVu rows

You own the whole step: build, surface bugs, fix before source freeze. Seven DejaVu pictures move and
nothing else does: the Ubuntu goldens and the T1 tree hash byte-equal are the fence. Keep the before
set beside the after set for Sid. Sid's word: "yes if this is not used and not how should be the
goes". Plan: OWNER you · SOURCE this contract · DONE WHEN SB1-SB2 are tripwires and your lines are
appended to NOW-5.md under "Step B". Ambiguity takes §7's default plus a note; a genuine fork is one
question in NOW-5.md, routed around while unblocked work continues. Commits: two, the route and the
re-record, each big enough to read as one change. On main, exact paths, no Co-Authored-By. Do not
stage CLAUDE.md, CLAUDE-1.md, docs/decisions.md, or anything under .claude/memory/.
```

### 10c. The falsification round (paste-able, one fresh Codex-class session, at Sid's hand)

```
Read docs/seam-cuts/CONTRACT-5.md primary and the files it names, by the windows it names. Falsify
it: claim → source, evidence cited, no verdict authority, no recut. For each of §5's scenarios name
one more wrong build that passes it. For each deletion in §1, confirm the "no reader" or "no caller"
claim by grep at HEAD and quote the hit if there is one; a reader the contract missed is the finding
that matters most. For each "replace", quote the written road from the new producer to every consumer
the old one had (the byte table to the compositor's lease pricing and image's mip pricing; the limit
reader to the harness report, the on-plane limit and the lease ceiling; the fallback integer to the
harness fence). Check §4's custody against §1. Expand every "the/its" on a load-bearing noun to its
referent; where two readings exist, say so. Return ≤3 decision-changing findings with anchors;
everything else is a list of lines. The author repairs in-session.
```
