# Instance cut — the remaining old-page instances come off the render floor

*below the waist · execution starter · opened by Sid 2026-08-26 · one session, straight through*

**Ground.** Source baseline `8647fef` (the rect stack is gone; 37 engine files, ~23.6k code lines, eight families in the key); this page sits at `fd0f544`+. The tree is the ground; this page is the list. Read code, not docs — `engine-waist-map.md` only if a why is missing. Codex's final review (2026-08-26) is folded in: scheduler kept, `orbit-eye` kept, the 3D editor icons added, the mark format made exact, the escape hatch removed, the fence narrowed to painters, the close receipt matched to the work-package law.

**The criterion (Sid's).** Below the waist = description in, geometry out — or geometry in, pixels out — *without knowing what the thing is or what a gesture means*, plus the custody of GPU memory and order. Everything else is an old-page **instance** riding on that floor. Instances belong above, in ECS / Smalltalk land, which does not exist yet — so today they are deleted and git keeps them.

**Sid's two touches:** this page (open) · his word on the close receipt (accept). No gates in between; keep going.

---

## 1. Delete — whole files

| file | why |
|---|---|
| `src/app/client/substrate/connector_material.cljc` | arrow kinds, heads, policies, provenance colours — an arrow is a rule above (A's edge → B's edge, elbows, heads, a label) producing one stroke + one text |
| `src/app/client/substrate/connector_route.cljc` | the elbow router, a live route cache, a provider atom set by mutation |
| `src/app/client/substrate/webgpu/connector_gpu.cljs` | its own buffer + a cloned text system for the label |
| `src/app/client/workspace/region3d_pointer.cljc` | dolly / glued pan / gizmo hover — the 3D window's pointer policy; `region3d_scene` uses it only for handle picking (five call sites, all cut below) |

Lift list (git, when arrows return above): `elbow-points` `clip-from-center` `trim-polyline-start/end` `head-triangle` `layout-label` `route-centerline`.

## 2. Delete — named defs inside kept files

- `src/app/client/substrate/frame_scheduler.cljc` — `pulse-alpha` (`:179`) only, plus its assertions in `frame_scheduler_test`. The rest of the file stays: causes, deadlines, due-deadlines, encode-or-skip, monotonic time, replay, receipt — facts + time → should the GPU encode a frame. That it is verifier-only today is true of the whole dark engine.
- `src/app/client/substrate/region3d_scene.cljc` — `orbit` (pointer motion → an edit) `gizmo-world-scale` `line-samples` `sampled-handles` `axis-handles` `translate-plane-handles` `ring-handles` `gizmo-handles` `projected-handle-distance2` `pick-screen-handles` `translate-delta` `rotate-delta` `scale-ratio`; the constants `gizmo-hit-radius-px` `gizmo-size-factor` `gizmo-ring-sample-count` `orbit-radians-per-pixel` `orbit-pitch-limit` `glyph-hit-radius-px`. **Keep `orbit-eye`** (`:544`) — camera description → eye position, called by `camera-matrices` (`:570`); it is description → geometry. `pick-region` keeps only: ray → BVH surface hit, else region background (`:route :region-background`) — the glyph-handle and gizmo branches go.
- `src/app/client/substrate/region3d_material.cljc` — `edit-operation-ids` `edit-diff` `apply-edit`. The `legal-*-kinds` menus **stay**: they enumerate what the kernel can compute (which primitives `primitive-mesh` can build, which lights the shader lights, which cameras exist) — floor capability, not product vocabulary.
- `src/app/client/substrate/webgpu/region3d_gpu.cljs` — `gizmo-shader` `grid-shader` `gizmo-hover-number` `gizmo-uniform-bytes`, the global `!pick-state` (`:706`) and its reads/resets, the session-row `:selection` / `:gizmo-mode` / `:gizmo-hover` plumbing, **and the whole editor-icon closure** (Blender-style viewport icons for `:light` `:camera` `:empty` objects — checked in `glyph-rows`): `overlay-glyph-shader` (`:233`) `glyph-instance-stride`, the glyph shader module + pipeline, `glyph-rows` `glyph-index` `glyph-row-values` `glyph-values`, the editor-glyph buffer's allocation / upload / update / destruction, `:glyph-count`, the glyph draw call, glyph upload counters and receipt fields. **Do not touch `region3d_placement_gpu`** — that is the retained road for real text and ink on 3D planes.
- `src/app/client/substrate/path_material.cljc` — `move-knot` `set-knot-pressure` `move-contour-point` `set-paint` `replace-contours` `revisioned-edit`.
- `src/app/client/workspace/text_layout.cljc` — `ground-op-roles` (`:23`, sixteen Softland words) `ground-op-role` (`:320`) `ground-text-address` (`:346`). `layout-key` (`:352`) takes an opaque `:address` from the caller instead of `subject-id / op-role / occurrence`.
- `src/app/client/workspace/containers.cljc` — `update-legacy-transform` (`:139`) and the `:x/:y/:scale` arm of `set-transform`; `:affine` is the only input.
- `src/app/client/substrate/webgpu/renderer.cljs` — the second text system for overlay lines (`chrome-text-sys` `chrome-base-line-count` `diagnostics-visible` `diagnostics-line-index`, everywhere they appear — the old debug HUD); `pulse-alpha`; `connector-system` and `connector-label-entry`; the connector row of `frame-family-registry`; `:connectors` in `store-input-keys` and `frame-input-map`.
- `src/app/client/substrate/scene_tape.cljc` — `connector-registration`, `:render.family/connector` in `family-ids` and `family-contracts`.
- `src/app/client/substrate/frame_inputs.cljc` — the connector row; the `chrome-text-sys` / `diagnostics` keys in the msdf, slug and chrome rows.

## 3. Marks — delete the vocabulary, keep the mechanism neutral

The px-anchored quad (anchor in world, size in screen px, crisp at any zoom with no re-upload) is a real floor regime — Codex and Claude converged on this. The five forms are not.

- `src/app/client/substrate/chrome_material.cljc` — delete `legal-forms` `form-declarations` `geometry-declaration-for` `legal-corners` `colors` `solid-rect-quad` `outline-quads` `line-quad` `gap-tick-quad` `alignment-axis` `material-quads` `claimed-corpus-forms` `assert-corpus-coverage!` and the px constants (`snap-threshold-…` `handle-size-…` `handle-slop-…` `chrome-width-…` `gap-tick-length-…`). Keep `vertex` `quad` `camera-project` `chrome-screen-rect` `vertex-words` `vertex-stride` `vertex-values`; `material-vertices` takes a vector of neutral quads and nothing else. The neutral quad is **per-vertex** (the mechanism already is), so it covers both a fixed-size handle (four equal anchors, offsets make the square) and a line between two world points with constant px thickness (anchors are the endpoints, offsets are the thickness):

  ```
  {:anchors    [[wx0 wy0] [wx1 wy1] [wx2 wy2] [wx3 wy3]]
   :offsets-px [[dx0 dy0] [dx1 dy1] [dx2 dy2] [dx3 dy3]]
   :color      [r g b a]}          → two triangles
  ```

- `src/app/client/substrate/webgpu/chrome_gpu.cljs` — stays; its input is the neutral quads. Remove: the pulse vertex field, the pulse fragment uniform + binding, `pulse-buffer`, `!last-pulse-alpha`, the pulse counters and the prepare parameter, `:geometry :chrome-form`, the `handle-slop` pick (`:304`). The scene entry carries literal `:pick :none` — marks are display; what a mark means is decided above.
- `scene_tape` chrome registration — keep the family row (the key changes at build stage, not here); its grammar names no form and no selection field enters the neutral material.
- No escape hatch. The decision is made: mark meanings go, the world-anchor / screen-px painter stays. A line count is not a contradiction; a genuine one (two binding laws colliding) gets recorded in the receipt and the work continues.

## 4. Guard and tests

- `verifier.cljs` — delete `run-connector-*` and their fixtures; rewrite `run-chrome-*` as one neutral-quad golden (a handle and a line, two zooms); in `run-region3d-floor!` drop the `gizmo-overlay` / `gizmo-hover-x` cases and the gizmo pick probe near `:3307`; keep the `frame-scheduler` receipt / replay near `:3640`, minus pulse.
- goldens `test/app/fixtures/render_engine/gpu-goldens/gpu-connector-*.png` go; `gpu-chrome-*.png` go (one neutral golden replaces them); the 3D placed goldens (`region3dPlacedFlat` / `region3dPlacedMsdf`) re-bake only if a gizmo or grid was in frame.
- tests: delete `connector_material_test` `connector_route_test` `region3d_pointer_test`; trim `frame_scheduler_test` (pulse assertions only) `path_material_test` (edit verbs) `region3d_scene_test` (orbit / gizmo / deltas) `region3d_material_test` (edit ops) `text_layout_test` (ground roles) `chrome_material_test` (forms) `scene_tape_test` `frame_inputs_test` (connector rows); `test_runner.clj` and `test/render_engine/*.mjs` family lists follow.

## 5. Do NOT delete (and why — both reviews checked these)

- `image_material` — has no edit ops; the class *declared* them in `scene_tape`, the code never had them.
- `containers` add / remove / set / slot allocation — the floor's transform table; a table needs add/remove; the slot is GPU custody.
- the `__softland*` globals in `frame_inputs` / `renderer` — the guard's channel; the puppeteer runner reads receipts off `window`.
- `make-snapper` in `renderer` — snaps glyph origins to the pixel grid (text rendering), not editor snapping.
- `legacy-layout` in `text_layout` — the JVM's stand-in shaper until a JVM shaper exists.
- the 3D defaults (`default-view` `primitive-defaults` …) — a build-stage question (defaults vs required fields), not an instance.
- **the 3D kernel** (`region3d_scene` minus §2, `region3d_material`, `region3d_placement`, `region3d_evaluation`, `region_rungs`, `region3d_gpu` minus §2, `region3d_placement_gpu`) — stays on the standing position that a thing may someday wear a mesh facet. Only Sid flips this; if he does, the whole `region3d_*` set joins §1.

## 6. Not this session — build stage, needs its own contract

- family key → facet / road key, with multi-facet entities (`scene_tape:14`, `frame_inputs` table, `renderer` registry, `draw-frame!`'s per-family prepare)
- the render packet: `:road` + `:resource-key` + opaque `:pick-token` replacing `:material/id :material/revision :instance/id :family/id` on every entry (`scene_tape:597`); no live system object in `:paint/source` (`path_gpu:367`)
- the mark road's contract (neutral input is enough for now)
- msdf / slug as two backends of one text road

## 7. Close receipts

1. The focused affected JVM namespaces green (not the whole suite — unrelated failures are recorded as foreign debt, per the work-package law); CLJS compiles with zero new warnings; the render verifier passes: every remaining golden (image · path · msdf · slug · t2 · 3D placed), the neutral-mark golden at two zooms, and one retained 3D mesh / depth / shadow representative golden.
2. Sizes before/after (code lines, comments off) per file touched.
3. **Exact-absence tripwires** (a clj test or `test/render_engine/verify_instance_cut.mjs`): the connector files and registration absent; the five chrome forms absent from `chrome_material` `scene_tape` `frame_inputs` `renderer`; the gizmo / grid / editor-glyph symbols absent from `region3d_gpu` and `region3d_scene`; the path edit fns absent; `pulse-alpha` absent; no selection field in the neutral mark material.
4. **The painter fence** — same test, narrower than a client-wide word ban (text layout legitimately owns caret / selection geometry; `orbit-eye` is camera math): `renderer.cljs`, `compositor_gpu.cljs` and every `*_gpu.cljs` contain none of `selection|marquee|gizmo|orbit|elbow|arrowhead|connector` outside a comment. The rule is *no product meaning inside the painter*. This is the generator fix — "composition on top" as a test, not a doc line — and it must pass at close.
5. `git diff --stat` proof that nothing under `src/app/server`, the Rama modules, durable data, or the preservation archive changed.
6. The receipt is this file with a **Closed** section appended: what was deleted (files, defs, lines), what was neutralized, any contradiction met, what the tripwires and the fence found. Board pointer on `docs/next-prompt.md` (one line).

**Commit discipline:** memory laws — parallel-sessions git (foreign-change check, exact paths, plain `git commit`, never `git commit -- path`), never Co-Authored-By in any form, commit on `main` grouped by concern, push is Sid's alone. Sid's dirty files (`CLAUDE.md` deleted, `CLAUDE-1.md`, the memory note) are his — never stage them.

**Sequence after this:** the packet contract (§6) as one hard-thinking document, then one execution that re-keys. Done means `scene_tape:597` requires `:road :resource-key :pick-token` and nothing named material, instance, or family.

## Closed — 2026-08-26

- Cut: `connector_material.cljc` (route material), `connector_route.cljc` (router), `connector_gpu.cljs` (executor), and `region3d_pointer.cljc` (input adapter), plus their three focused test namespaces; 1,583 source code lines → 0.
- Neutralized: chrome accepts only opaque world anchors, screen-pixel offsets, and RGBA quads; form, selection, pick, pulse, HUD, grid, gizmo, object-glyph, and edit-delta machinery no longer enters the painter.
- Trimmed: path/Region3D edit operations, ground text vocabulary, the legacy container transform arm, connector verifier/input/registration lanes, and coupled assertions/receipts.
- Goldens: six chrome/connector PNGs removed; `gpu-chrome-neutral-point-line-z1.png` and `gpu-chrome-neutral-point-line-z4.png` baked and manifest-pinned.
- Tripwires: 5/5 scenarios pass, including four-file and registration absence, old-form/selection/pulse/edit/3D-symbol absence, and the six-file painter fence.
- Close receipt: 61 JVM tests / 543 assertions / 0 failures / 0 errors; CLJS 0 warnings; base, image, path, chrome, and Region3D guards pass; msdf, neutral-chrome, and path representative goldens pass.
- Source lines A (comments off): chrome_material 254→118; frame_inputs 199→186; frame_scheduler 178→169; path_material 522→417; region3d_material 658→587; region3d_scene 1201→976; scene_tape 635→599; chrome_gpu 320→275; region3d_gpu 1535→1194.
- Source lines B: renderer 2967→2854; verifier 4111→3178; containers 278→246; text_layout 1439→1404; connector_material 399→0; connector_route 656→0; connector_gpu 422→0; region3d_pointer 106→0. Total source 15,880→12,203.
- Test/tool lines A: chrome_material_test 70→47; connector_material_test 120→0; connector_route_test 286→0; frame_inputs_test 113→96; frame_scheduler_test 61→59; frame_view_region_binding_test 200→197; path_material_test 97→92; path_tessellation_test 55→57; region3d_material_test 162→118.
- Test/tool lines B: region3d_scene_test 257→145; region3d_pointer_test 170→0; shaping_correction_test 610→588; test_runner 574→571; run_verifier 329→341; scene_tape_fence 158→157; text_layout_fence 126→125; instance_cut tripwire 0→68. Total 3,388→2,661.
- Custody: no task write under `src/app/server`, `/mnt/data/rama`, or the completed archive; §5 roads and Sid's foreign dirty files remain untouched.
- Contradiction/debt: none; the close runner found one stale deleted connector-route owner in the text-layout fence, that row was removed, and the failed npm receipt replayed green.
- Acceptance: Sid accepts at this Closed append; this atom stops here.
