# Instance cut — the remaining old-page instances come off the render floor

*below the waist · execution starter · opened by Sid 2026-08-26 · one session, straight through*

**Ground.** HEAD `8647fef` (the rect stack is gone; 37 engine files, ~23.6k code lines, eight families in the key). The tree is the ground; this page is the list. Read code, not docs — `engine-waist-map.md` only if a why is missing.

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
| `src/app/client/substrate/frame_scheduler.cljc` | the selection pulse timer; only a test and the verifier call it |

Lift list (git, when arrows return above): `elbow-points` `clip-from-center` `trim-polyline-start/end` `head-triangle` `layout-label` `route-centerline`.

## 2. Delete — named defs inside kept files

- `src/app/client/substrate/region3d_scene.cljc` — `orbit` `orbit-eye` `gizmo-world-scale` `line-samples` `sampled-handles` `axis-handles` `translate-plane-handles` `ring-handles` `gizmo-handles` `projected-handle-distance2` `pick-screen-handles` `translate-delta` `rotate-delta` `scale-ratio`; the constants `gizmo-hit-radius-px` `gizmo-size-factor` `gizmo-ring-sample-count` `orbit-radians-per-pixel` `orbit-pitch-limit` `glyph-hit-radius-px`. `pick-region` keeps only: ray → BVH surface hit, else region background (`:route :region-background`) — the glyph-handle and gizmo branches go.
- `src/app/client/substrate/region3d_material.cljc` — `edit-operation-ids` `edit-diff` `apply-edit`. The `legal-*-kinds` menus **stay**: they enumerate what the kernel can compute (which primitives `primitive-mesh` can build, which lights the shader lights, which cameras exist) — floor capability, not product vocabulary.
- `src/app/client/substrate/webgpu/region3d_gpu.cljs` — `gizmo-shader` `grid-shader` `gizmo-hover-number` `gizmo-uniform-bytes`, the global `!pick-state` (`:706`) and its reads/resets, and the session-row `:selection` / `:gizmo-mode` / `:gizmo-hover` plumbing.
- `src/app/client/substrate/path_material.cljc` — `move-knot` `set-knot-pressure` `move-contour-point` `set-paint` `replace-contours` `revisioned-edit`.
- `src/app/client/workspace/text_layout.cljc` — `ground-op-roles` (`:23`, sixteen Softland words) `ground-op-role` (`:320`) `ground-text-address` (`:346`). `layout-key` (`:352`) takes an opaque `:address` from the caller instead of `subject-id / op-role / occurrence`.
- `src/app/client/workspace/containers.cljc` — `update-legacy-transform` (`:139`) and the `:x/:y/:scale` arm of `set-transform`; `:affine` is the only input.
- `src/app/client/substrate/webgpu/renderer.cljs` — the second text system for overlay lines (`chrome-text-sys` `chrome-base-line-count` `diagnostics-visible` `diagnostics-line-index`, everywhere they appear — the old debug HUD); `pulse-alpha`; `connector-system` and `connector-label-entry`; the connector row of `frame-family-registry`; `:connectors` in `store-input-keys` and `frame-input-map`.
- `src/app/client/substrate/scene_tape.cljc` — `connector-registration`, `:render.family/connector` in `family-ids` and `family-contracts`.
- `src/app/client/substrate/frame_inputs.cljc` — the connector row; the `chrome-text-sys` / `diagnostics` keys in the msdf, slug and chrome rows.

## 3. Marks — delete the vocabulary, keep the mechanism neutral

The px-anchored quad (anchor in world, size in screen px, crisp at any zoom with no re-upload) is a real floor regime — Codex and Claude converged on this. The five forms are not.

- `src/app/client/substrate/chrome_material.cljc` — delete `legal-forms` `form-declarations` `geometry-declaration-for` `legal-corners` `colors` `solid-rect-quad` `outline-quads` `line-quad` `gap-tick-quad` `alignment-axis` `material-quads` `claimed-corpus-forms` `assert-corpus-coverage!` and the px constants (`snap-threshold-…` `handle-size-…` `handle-slop-…` `chrome-width-…` `gap-tick-length-…`). Keep `vertex` `quad` `camera-project` `chrome-screen-rect` `vertex-words` `vertex-stride` `vertex-values`; `material-vertices` takes a vector of neutral quads `{:anchor [wx wy] :offset-px [dx dy] :size-px [w h] :color [r g b a]}` and nothing else.
- `src/app/client/substrate/webgpu/chrome_gpu.cljs` — stays; its input is the neutral quads; its pick is `:none` (marks are display; what a mark means is decided above). Remove the `handle-slop` pick (`:304`).
- `scene_tape` chrome registration — keep the family row (the key changes at build stage, not here); its grammar names no form.
- **Escape hatch:** if neutralizing costs more than ~100 lines, cut the marks family whole instead (both files + row) and note it in the receipt; the mechanism is lifted from git when the mark road gets its contract.

## 4. Guard and tests

- `verifier.cljs` — delete `run-connector-*` and their fixtures; rewrite `run-chrome-*` as one neutral-quad golden (two quads, two zooms) or delete with the escape hatch; in `run-region3d-floor!` drop the `gizmo-overlay` / `gizmo-hover-x` cases and the gizmo pick probe near `:3307`; drop the `frame-scheduler` receipt near `:3640`.
- goldens `test/app/fixtures/render_engine/gpu-goldens/gpu-connector-*.png` go; `gpu-chrome-*.png` go (one neutral golden replaces them); the 3D placed goldens (`region3dPlacedFlat` / `region3dPlacedMsdf`) re-bake only if a gizmo or grid was in frame.
- tests: delete `connector_material_test` `connector_route_test` `frame_scheduler_test` `region3d_pointer_test`; trim `path_material_test` (edit verbs) `region3d_scene_test` (orbit / gizmo / deltas) `region3d_material_test` (edit ops) `text_layout_test` (ground roles) `chrome_material_test` (forms) `scene_tape_test` `frame_inputs_test` (connector rows); `test_runner.clj` and `test/render_engine/*.mjs` family lists follow.

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

1. `clojure -M:test` green; render-verifier compiles with zero new warnings; every remaining golden passes (image · path · msdf · slug · t2 · 3D placed · the neutral mark).
2. Sizes before/after (code lines, comments off) per file touched.
3. **The fence** — add `test/render_engine/verify_no_product_words.mjs` (or a clj test): fails if any file under `src/app/client` contains `selection|marquee|gizmo|orbit|elbow|arrowhead|connector|handle-hit|snap-threshold` outside a comment. This is the generator fix: "composition on top" as a test, not a doc line. It must pass at close.
4. The receipt is this file with a **Closed** section appended: what was deleted (files, defs, lines), what was neutralized, what the escape hatch decided, what the fence found. Board pointer on `docs/next-prompt.md` (one line).

**Commit discipline:** memory laws — parallel-sessions git (foreign-change check, exact paths, plain `git commit`, never `git commit -- path`), never Co-Authored-By in any form, commit on `main` grouped by concern, push is Sid's alone. Sid's dirty files (`CLAUDE.md` deleted, `CLAUDE-1.md`, the memory note) are his — never stage them.

**Sequence after this:** the packet contract (§6) as one hard-thinking document, then one execution that re-keys. Done means `scene_tape:597` requires `:road :resource-key :pick-token` and nothing named material, instance, or family.
