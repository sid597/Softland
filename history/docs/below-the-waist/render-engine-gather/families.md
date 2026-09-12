# Render-family layer — gather digest (2026-08-23)

Read-only gatherer (Opus-class), skeleton-first, scoped windows. Paths relative to
`/mnt/data/projects/Softland`. `scene_tape.cljc` and `frame_inputs.cljc` live under
`src/app/client/substrate/`, not `workspace/`. DECISION SERVED (all blocks): explaining
the family layer. No verdicts on whether the design is good.

# INDEX
- **A. Chrome** = selection furniture (outline, 4 corner handles, marquee, guide line, gap tick). Three pure halves + one GPU half; registers as top-level DATA in three maps plus one hit-predicate map.
- **B. Contract** = 4 registries, all load-time top-level data, no explicit `register!` call. Two real leaks in "without touching another family."
- **C. Connector** = durable `:references` edge; straight or Z-elbow polyline, no obstacle avoidance; drawn by minting a synthetic ink path.
- **D. Path** = ink + vector shape; segment quads + round cap/join fans / hole-bridging + ear clipping, hand-written `.cljc`, no JS dep; hit-tests the material, not the mesh.
- **E. Image** = material half in `substrate/`, GPU half inside `renderer.cljs`; bytes → sha256 verify → ImageBitmap → 512² atlas (≤128px) or dedicated texture.
- **F. Region3d** = a whole 3D scene as ONE outer 2D tape citizen; interior objects never enter the tape, depth owns their order.
- **G. State**: image/chrome/path/connector each have 3 golden cases, region3d-floor 8; no central golden registry; the six `*-gpu` namespaces have zero JVM-test callers.
- **H. Surprises**: million-unit handle bounds · connector-as-fake-ink-path · the region's hidden interior.
- **I. Generations**: only `IMAGE-ATOM` (20 hits) and `SEAM-STEP1` (8) survive as real markers; the rest is banner prose — the generation record lives in the registration `:receipts` vectors.

---

# A. CHROME END-TO-END

**FACT — what it is.** Chrome is selection/manipulation furniture, not block borders and not hover styling. Five legal forms, one color each: a selection outline, four corner drag handles, a marquee rectangle, a snap guide line, and a gap tick. It is session-only truth — the registration declares `:serialization :none-session-truth` and `:export-projections :none-by-design`, and its provenance says `:durable-rows :none-by-design`.
SOURCE: `src/app/client/substrate/chrome_material.cljc:16-28`; `src/app/client/substrate/scene_tape.cljc:466-475`
EXTRACTION:
```clojure
(def legal-forms
  #{:selection-outline :handle :marquee :guide-line :gap-tick})
(def legal-corners #{:nw :ne :sw :se})
```

**FACT — chrome has THREE pure halves, not two.** `chrome_material.cljc` is the grammar + geometry (what a chrome material may contain, and what quads it becomes). `chrome_derive.cljc` is the pure state-transition layer (selection revision → slot set; gesture → slot; follow-transforms). `chrome_runtime.cljs` (under `workspace/`) is the impure third piece that owns the atoms and mutates the store. Then `chrome_gpu.cljs` is the GPU half.
SOURCE: `src/app/client/substrate/chrome_derive.cljc:1-5`; `src/app/client/workspace/chrome_runtime.cljs:1-4`
EXTRACTION:
```clojure
"Pure maintained chrome slots, independent full-recompute oracle, and live
 handle-pick providers. Runtime owns atoms and store mutation; this namespace
 owns the data transition laws and proportionality receipts."
```
UNCERTAINTY: chrome and region3d are the only families with a `workspace/*_runtime` namespace; path/connector/image drive through `scene_runtime`/`scene_store` instead. Not read to confirm why.

**FACT — material half, in→out shape.** IN: a target identity `{:vi :address}`, a store row `{:bounds :container :family}`, and a selection revision. OUT: a *slot* — a rect-tree subtree of chrome nodes, each carrying a validated `:chrome/material`. The material is a flat map: `{:chrome/form :chrome/anchor-bounds :chrome/derived-from :chrome/selection-rev :chrome/pick :chrome/container}` plus optional `:chrome/corner :chrome/gesture-id :chrome/from :chrome/to :chrome/alignment`. `chrome_material` then turns one material into quads → triangles → a 10-word interleaved vertex `[anchor.xy, offset_px.xy, rgba, container_idx, pulse]`.
SOURCE: `src/app/client/substrate/chrome_derive.cljc:96-108, 159-176`; `src/app/client/substrate/chrome_material.cljc:241-273`
EXTRACTION:
```clojure
(def vertex-words 10)
(def vertex-stride (* vertex-words 4))
(defn vertex-values [{:keys [anchor offset-px color pulse?]} container-idx]
  (into (into (vec anchor) offset-px)
        (conj (vec color) (or container-idx 0) (if pulse? 1 0))))
```

**FACT — the hybrid trick that defines chrome.** Every vertex has *two* positions: a container-local **anchor** (moves with the block, scales with zoom) and a screen-pixel **offset** applied *after* the camera. So a handle is always 10 screen px and an outline always 1 px, at any zoom, without recomputing geometry.
SOURCE: `src/app/client/substrate/webgpu/chrome_gpu.cljs:38-42`
EXTRACTION:
```wgsl
let world_anchor = c.translation + c.axis_x * input.anchor.x +
                   c.axis_y * input.anchor.y;
let screen_pos = world_anchor * zm + pn + input.offset_px;
```

**FACT — GPU half.** One pipeline, `triangle-list`, no index buffer, three bindings: camera uniform, a read-only storage array of container transforms, and a 16-byte pulse uniform. Fragment shader is trivial — pass the color, multiply alpha by pulse if flagged, optionally sRGB→linear-premultiply behind a compile-time constant string that gets `str/replace`d at pipeline creation. One private interleaved vertex buffer, repacked only when the *mesh set key* changes (`[material container-idx owner-vi]` per op) — camera motion never triggers an upload.
SOURCE: `src/app/client/substrate/webgpu/chrome_gpu.cljs:118-142, 239-246`
EXTRACTION:
```clojure
(if (= mesh-set-key @(:!last-mesh-set-key chrome-system))
  {:mesh-set-changed? false :writes 0 ...}
  (let [prepared (prepare-ops chromes)
        packed (pack-vertices prepared) ...
```

**FACT — batches from tape entries.** `chrome-entries` walks `(:ordered-vis store-frame)` and mints **one entry per visible container** that has chrome ops, summing vertex counts over that container's prepared rows into `{:vertex-count :first-vertex}`. Order is `{:stratum :overlay :pass-class :direct}` with a `[:frame/root -1 -1]` prefix on the stack path.
SOURCE: `src/app/client/substrate/webgpu/chrome_gpu.cljs:278-306`
EXTRACTION:
```clojure
:family/id :render.family/chrome
:order (frame-order source-order entry-id)
:paint {:paint/source chrome-system :paint/source-type :chrome-system
        :vertex-count vertex-count :first-vertex first-vertex}
:pick {:geometry :chrome-form :owner vi :boundary :hit
       :hit-slop chrome-material/handle-slop-screen-px}
```

**FACT — the four registrations, with exact keys.**

(i) **Scene tape family registry** — `src/app/client/substrate/scene_tape.cljc:449-478`, a top-level `def chrome-registration` folded into `family-contracts` (`:480-495`) and reduced into `default-family-registry` (`:584`). Keys supplied: `:family/id :family/version :grammar :pick :provenance :versioning :render :receipts`, where `:grammar` carries `material-fields · instance-fields · validation · defaults · edit-operations · serialization · export-projections · entry-paint-required-keys [:vertex-count]`. No paint fn, no hit fn — this registry is pure declaration.

(ii) **Frame input declarations** — `src/app/client/substrate/frame_inputs.cljc:62-65`:
```clojure
:render.family/chrome
#{:chromes :ordered-vis :ops-count-by-vi :order-by-vi
  :chrome-system :chrome-system-token :diagnostics-visible}
```
This set is the family's entire allowed view of frame state; `declared-inputs` `select-keys` it and throws if undeclared, and `changed-families` re-produces a family only when one of these values changed.

(iii) **Renderer frame-family registry** — `src/app/client/substrate/webgpu/renderer.cljs:3352-3356`. Exactly four keys per family: `:contract` (pulled from the scene-tape registry), `:inputs` (pulled from frame-inputs), `:produce`, `:execute!`:
```clojure
:render.family/chrome
(family :render.family/chrome
        (store-producer chrome-gpu/chrome-entries
                        [:chromes :ordered-vis :ops-count-by-vi :order-by-vi])
        chrome-gpu/execute-chrome-batch!)
```

(iv) **Narrow-phase hit predicate** — `src/app/client/workspace/rect_tree.cljc:639-654`:
```clojure
(def family-hit-predicates
  "Per-family narrow-phase seam. Bounds remain the universal broad phase;
   registered families may replace only their own mathematical predicate."
  {... :render.family/chrome
   (fn [node local-point] (chrome-derive/handle-hit? node local-point))})
```

**FACT — verifier / the chrome golden.** Three cases, driven by mode-keys `[:selection-z1 :selection-z8 :gesture-z0p1]`: `selection-outline-handles-default-unit-z1`, `…-default-max-z8`, `marquee-guide-gap-default-min-z0p1`. The verifier builds `chrome_material` maps directly (not via derive), synthesizes a store-frame, runs the real `chrome-gpu` system, and pixel-compares. Pass/fail is a `:pass?` boolean per row, aggregated by `(every? :pass? rows)`.
SOURCE: `src/app/client/substrate/webgpu/verifier.cljs:1584-1600, 1758 (run-chrome-atom!), 1767`
EXTRACTION:
```clojure
:selection-z8
{:case-id "selection-outline-handles-default-max-z8"
 :mode "selection-outline-handles" :zoom 8.0
 :pan [-24.0 -16.0]
 :ops (chrome-selection-ops 8.0)}
```
UNCERTAINTY: `chrome-runtime` is `:require`d by verifier.cljs:37 but no call to `chrome-runtime/boot!` was found anywhere in `src/` — a dead require (confirmed by the scene gatherer: the verifier calls nothing in chrome/region3d runtimes).

---

# B. THE PLUG-IN CONTRACT

**FACT — what a family must supply, and where.** Four places, all pure data at namespace load:

1. **A family contract row** in `scene_tape/family-contracts` (`:480-495`) — declares grammar, pick policy, provenance, versioning, geometry (`:render/geometry` with authority/classify/coverage/pick/bounds/derivations/regimes), the color seam, and `:receipts`. Validated fail-closed by `validate-family!` (`:542-568`), which rejects any id outside the hardcoded `family-ids` vector (`:14-26`) and any family missing the linear-premultiplied scene-color tag.
2. **An input declaration set** in `frame_inputs/family-input-declarations` (`:28-68`).
3. **A `{:contract :inputs :produce :execute!}` row** in `renderer/frame-family-registry` (`:3297-3361`).
4. **Optionally** a narrow-phase hit fn in `rect_tree/family-hit-predicates` (`:639-654`) — absent means "bounds hit is the answer".

Per-entry shape is enforced generically: `validate-entry!` requires `[:entry/id :material/id :material/revision :instance/id :family/id :order :paint :pick :visibility]` and then applies the family's *declared* `:entry-paint-required-keys` with no branch of its own.
SOURCE: `src/app/client/substrate/scene_tape.cljc:653-686`
EXTRACTION:
```clojure
;; IMAGE-ATOM T1: family-specific entry shape is registration data.  The
;; validator applies declared keys generically and gains no image branch.
(when (seq required-paint-keys)
  (require-keys! "family paint" (:paint entry) required-paint-keys))
```

**FACT — registration is load-time, not explicit.** All four registries are top-level `def`s. There is a load-time exhaustiveness check that throws if the two main sets disagree:
SOURCE: `src/app/client/substrate/webgpu/renderer.cljs:3364-3372`
EXTRACTION:
```clojure
(def ^:private frame-contract-registry
  (let [executor-families (set (keys frame-family-registry))
        admitted-families (set scene-tape/family-ids)]
    (when-not (= admitted-families executor-families)
      (throw (ex-info "Frame executor registrations must exactly cover admitted families" ...
```
GPU *systems* (buffers, pipelines) are created explicitly at device init (`renderer.cljs:3198-3230`), separately from registration.

**FACT — the "no branch" claim holds in the core walkers.** `scene_tape` has zero family branches; `paint-forward` and `pick-reverse` take the executor as an argument. `execute-frame-entry!` looks the executor up by `:family/id` and throws if absent.
SOURCE: `src/app/client/substrate/scene_tape.cljc:748-752`
EXTRACTION:
```clojure
(defn paint-forward
  "Execute the paint projection in exact tape order.  `execute!` is the declared
   family executor dispatch; this function contains no family branch."
  [tape execute!] (mapv execute! (:entries tape)))
```

**FACT — but two real leaks.** (1) The **prepare/upload phase** in `draw-frame!` is a hand-written sequence of named per-family calls, each guarded by a named local that also appears in the function's arglist — adding a family means editing this block:
SOURCE: `src/app/client/substrate/webgpu/renderer.cljs:3717-3730, 3162-3163`
EXTRACTION:
```clojure
(when image-system (prepare-image-frame! image-system (:images store-frame)))
(mark-draw! "img")
(when path-system (path-gpu/prepare-path-frame! path-system (:paths store-frame) zoom))
(mark-draw! "path")
(when chrome-system (chrome-gpu/prepare-chrome-frame! chrome-system (:chromes store-frame) ...))
```
(2) Six of the ten families (`rect shadow msdf slug clip image`) have their entry-producers, pipelines and executors **inside renderer.cljs itself** (`:887, :1637, :1706, :1894, :2016, :2858`), not in their own namespace. Only `path`, `connector`, `chrome`, `region-3d` are fully out-of-tree. Also `scene_tape.cljc`'s `registration` helper is `defn-` (private), so `region3d_material` writes its own parallel copy of the contract map (`region3d_material.cljc:645-700`).
UNCERTAINTY: whether the six in-renderer families are *older* code or a deliberate split was not checked. The `IMAGE-ATOM` markers suggest image was migrated in place.

---

# C. CONNECTOR

**FACT — what it is.** A durable "references" relation between blocks — one legal kind (`:references`), two statuses (`:asserted`/`:retracted`), colored by *who asserted it* (human vs llm/agent/import/machine tint). Its material is "the fail-closed merge of a relation-row projection and client-session dress"; resolved routes are explicitly disposable.
SOURCE: `src/app/client/substrate/connector_material.cljc:1-6, 14-19`
EXTRACTION:
```clojure
"A connector material is the fail-closed merge of a relation-row projection
 and client-session dress. Resolved routes and meshes are disposable readers;
 none of their coordinates become relation truth."
```

**FACT — routing, one paragraph.** Two policies only: `:straight` and `:elbow/v1`. Elbow is a plain Z: pick the dominant axis, split at the midpoint, emit 4 points. Author waypoints, if present, replace the policy entirely. There is **no obstacle avoidance and no anchor ports** — endpoints start at each shape's *center* and are then clipped outward to the boundary by ray-polygon intersection (`clip-from-center`), so the attachment point is wherever the line exits the shape. Cross-camera or unresolved endpoints yield a status (`:mixed-camera`, `:unresolved`, `:region-anchor-absent`, `:degenerate`) instead of geometry. Optional triangle heads are sized `size-k × stroke-width` and the polyline is trimmed back by head length plus a cap radius so the round cap doesn't bleed into the head.
SOURCE: `src/app/client/substrate/connector_route.cljc:185-196, 235-300`
EXTRACTION:
```clojure
(defn elbow-points
  "Pinned :elbow/v1 Z route over unclipped endpoint centers."
  [[ax ay :as from] [bx by :as to]] ...
      (>= dx dy) (let [mid (/ (+ ax bx) 2.0)] [from [mid ay] [mid by] to])
```

**FACT — what the GPU half draws.** Filled triangles, not line strips and not an SDF stroke. `connector_gpu` has **no vertex shader of its own** — it loads `path-gpu/path-vertex-shader` and supplies only a fragment shader. The mesh comes from `derive-mesh`, which tessellates through the path library and appends ear-clipped head triangles.
SOURCE: `src/app/client/substrate/webgpu/connector_gpu.cljs:63-64`; `src/app/client/substrate/connector_route.cljc:360-373`
EXTRACTION:
```clojure
(let [stroke-mesh (path-tessellation/tessellate (route-path-material resolved) zoom)
      triangles (into (:triangles stroke-mesh) (head-triangles resolved))
```

**FACT — hit testing.** `live-hit?` looks the current route up in a bounded in-memory route cache and asks `connector-material/hit?`. Registered as the family's narrow-phase predicate in `rect_tree`.
SOURCE: `src/app/client/substrate/connector_route.cljc:718-720`; `src/app/client/workspace/rect_tree.cljc:646-649`
UNCERTAINTY: `connector-material/hit?`'s tolerance rule not read.

---

# D. PATH

**FACT — what it is.** Both: `legal-kinds #{:ink :shape}`. Ink is pressure-varying freehand (knots carry `:pressure`, `:gesture-time`, `:source-event-ids`); shape is filled contours with `:outer`/`:hole`/`:open` roles.
SOURCE: `src/app/client/substrate/path_material.cljc:8-10`

**FACT — tessellation.** Ink: each knot pair becomes two triangles offset by `pressure-width/2` along the segment normal, plus round cap and join fans whose resolution comes from the zoom regime. Shape: holes are bridged into the outer contour by a visibility test, then the whole polygon is ear-clipped. Both are written by hand in `.cljc` — the docstring names the refusal explicitly.
SOURCE: `src/app/client/substrate/path_tessellation.cljc:1-6`
EXTRACTION:
```clojure
"Ink expands directly to segment quads plus round cap/join fans. Shapes use
 explicit hole bridging followed by ear clipping. Neither road promotes its
 mesh to material truth or routes through a JS triangulation dependency."
```

**FACT — GPU half.** Same shape as chrome: one `triangle-list` pipeline, camera uniform + container-transform storage array, one repacked vertex buffer. Uploads happen only when the **mesh-set identity or the zoom regime** changes; camera and container motion stay shader values. `path_gpu` also carries its own Sutherland-Hodgman triangle clipper (`clip-triangle`, `:202`).
SOURCE: `src/app/client/substrate/webgpu/path_gpu.cljs:1-5`

**FACT — hit testing runs on the material, not the mesh.** `path-material/hit?` classifies against centerline + pressure width (ink) or contour containment (shape), so picking never depends on the tessellated triangles. `path_tessellation/point-in-mesh?` exists but is only referenced from tests.
SOURCE: `src/app/client/substrate/path_material.cljc:515-516`; `src/app/client/workspace/rect_tree.cljc:643-645`

---

# E. IMAGE

**FACT — the split is asymmetric.** `image_material.cljc` is the only file in `substrate/` for this family; its GPU half lives inside `renderer.cljs`: `init-image-system:1139`, `image-entries:2858`, `execute-image-batch!:3102`, `image-ingress-receipt:1376`, `destroy-image-system!:1432`.
SOURCE: `src/app/client/substrate/image_material.cljc:1-6`
EXTRACTION:
```clojure
"This namespace owns no GPU objects and emits no scene-tape entries.  Source
 bytes resolve only through the digest-keyed registry below; renderer code is
 a consumer of these values, never a second material authority."
```

**FACT — load/upload path.** bytes → `validate-source!` (refuses untagged color: `source-refusal-policy :reject-untagged`) → sha256, compared against the declared digest and thrown on mismatch → byte-budget check → `Blob` → `createImageBitmap` with `premultiplyAlpha "none"` → alpha normalize → dimension re-check against declared → `placement-plan`. Small images (≤128px each side) land in a shared **512×512, 2-mip, 2px-padded atlas**; anything larger gets a dedicated texture (`:overflow :dedicated`). Upload is `copyExternalImageToTexture`.
SOURCE: `src/app/client/substrate/webgpu/renderer.cljs:1280-1349`; `src/app/client/substrate/image_material.cljc:229-249`
EXTRACTION:
```clojure
(def atlas-config
  {:atlas/version 1 :width 512 :height 512 :padding 2 :max-side 128
   :format :rgba8unorm
   ;; Two levels are the declared atlas road; level 1 is the deepest sampled
   ;; mip and the 2px gutter therefore still supplies a full texel (T5).
   :mip-level-count 2 :lod-max 1.0 :overflow :dedicated})
```

**FACT — instancing, not vertices.** Image is the one family that draws instances: a 13-word instance (`rect[4] + uv[4] + tint/opacity[4] + container u32`), with **no per-node transform**, and one tape entry per container whose sub-draws are `contiguous-binding-runs` — adjacent equal bindings merged, order never regrouped by texture.
SOURCE: `src/app/client/substrate/image_material.cljc:322-345`

---

# F. REGION3D

**FACT — the idea in code.** A region is a whole nested 3D scene that appears as exactly **one** outer 2D tape citizen; its interior objects never enter the tape at all.
SOURCE: `src/app/client/substrate/region3d_scene.cljc:1235-1250`
EXTRACTION:
```clojure
(defn tape-entry
  "Produce the one outer 2D tape citizen. Interior objects never enter the
   tape; depth owns their order."
  ...
     :paint {:region-id region-id :resolve-view resolve-view :rect rect}
     :pick {:geometry :region-router :owner region-id :boundary :hit}
```

**FACT — `region3d_scene.cljc` (56K) is the region's own world.** A region row carries its own `:scene` map (object-id → object with parent/transform/mesh/light/camera/text/ink) and its own authored camera `:view-default` (orbit rig: pivot/distance/yaw/pitch/lens). Live session camera is joined separately as a distinct generation. Load-bearing fns: `compose-hierarchy:140`, `derive-scene:733`, `maintain-camera:905`, `pick-region:947`, `tape-entry:1235`, `region-pass-fragment:1251`, plus BVH build/refit, camera matrices, gizmo handles, and a CPU reference PBR shader.
SOURCE: `src/app/client/substrate/region3d_scene.cljc:1-11` (ns docstring: "No clock exists here.")

**FACT — `region3d_placement.cljc` is NOT the region-in-parent transform.** It places text/ink *inside* a region and owns `project-region-anchor:409`, which projects an interior object out through the region camera onto the parent surface so a connector can attach to it. The region's own transform-in-parent is `compose-hierarchy` in scene.cljc.
SOURCE: `src/app/client/substrate/region3d_placement.cljc:1-11, 409`

**FACT — evaluation, rungs, pointer, bindings.**
- `region3d_evaluation.cljc` decides full rebuild vs. transform-only patch vs. no-op, splitting an evaluation key into `:static-material` and `:transforms` (`:2-4, 42-46`).
- `region_rungs.cljc` — a "rung" is a **texture-budget admission divisor** `[1 2 4 8]`, not a visual LOD: shrink the render target until it fits the physical pool (`:3, 50-53`).
- `region3d_pointer.cljc` is pure math (pointer packet, ray-plane, orbit/dolly, wheel routing `:region3d`/`:ground`/`:scroll`); the actual hit comes from `scene/pick-region`, called by `region3d_runtime.cljs:225` — region3d has **no** entry in `rect_tree/family-hit-predicates`.
- `region_bindings.cljs` binds a region-id to a compositor **slot index + texture lease** under a device epoch — not a `GPUBindGroup` (those are built in region3d_gpu).

**FACT — `region3d_gpu.cljs` (81K) is a real 3D renderer.** 8 `createRenderPipeline` calls from 10 shader defs: `mesh-vertex/fragment`, `shadow-depth`, `grid` (the floor), `overlay-glyph`, `gizmo`, `composite-vertex/fragment`, `worn-fragment`, `refusal-fragment`. Render-to-texture with MSAA color + depth + resolve; `execute-region3d-batch!` then draws the resolved texture as a quad in the outer 2D tape.
SOURCE: `src/app/client/substrate/region3d_scene.cljc:1251-1283`
EXTRACTION:
```clojure
{[:region3d/color-msaa region-id]
 {:kind :color :format "rgba16float" :sample-count 4
  :lifetime :held :budget-owner :compositor/region-leases}
 [:region3d/depth region-id]
 {:kind :depth :format "depth24plus" :sample-count 4 ...}
```
`region3d_placement_gpu.cljs` is the GPU half for 2D text/ink *placed inside* a region (flat + MSDF pipelines), separate from mesh geometry.

**FACT — region3d declares a SECOND family layer.** Objects inside a region get their own citizenship row, `:material.family/object-3d`, ordered `:via-router` rather than by scene order.
SOURCE: `src/app/client/substrate/region3d_material.cljc:692-707`
UNCERTAINTY: `object-family-citizenship` is not in `scene_tape/family-contracts` — its consumer was not found.

---

# G. STATE

**FACT — goldens per family** (all in `src/app/client/substrate/webgpu/verifier.cljs`):
- image (`run-image-atom!:1364`): `:golden/opaque:754`, `:golden/alpha:757`, `:golden/clipped:762`
- chrome (`run-chrome-atom!:1758`): 3 cases, `:1587/:1592/:1597`
- path (`run-path-atom!:2162`): `pressure-ink-default-min-z0p1:1990`, `holed-concave-default-unit-z1:1999`, `translucent-self-crossing-legal-z10:2005`
- connector (`run-connector-atom!:2698`): `straight-arrow-label-default-unit-z1:2262`, `elbow-waypoints-heads-default-min-z0p1:2277`, `provenance-pair-overlap-legal-z10:2294`
- region3d (`run-region3d-floor!:5289`): 8 cases — `sandwich`, `lit-depth-shadow`, `gizmo-overlay`, `gizmo-hover-x`, `placed-depth-interleave`, `anchored-edge-over-region`, `seam-demo`, plus a synthesized `worn`
There is **no central golden registry**; `run-verifier!` (`^:export`, `:5465`) fans out to per-family runners via one `Promise.all` (`:5577-5591`), each holding its own inlined mode-key vector. Pass/fail is a per-row `:pass?` aggregated by `(every? :pass? rows)`.
UNCERTAINTY: these run in a browser; current green/red status was not confirmed by this gatherer — nothing in the source records last-run results (NOW.md records base/chrome/Region3D green at the waist close).

**FACT — callers.** Every material/route namespace has at least one non-test production caller; none are test-only. The load-bearing negative: **all six GPU-half namespaces** (`chrome-gpu`, `connector-gpu`, `path-gpu`, `region3d-gpu`, `region3d-placement-gpu`, `region-bindings`) have **zero** `.clj` test files referencing them. Their only callers are `renderer.cljs`, sibling GPU namespaces, and `verifier.cljs`. GPU coverage exists only through the browser verifier, not the JVM suite.
UNCERTAINTY: search was by namespace-qualified symbol; a test using a fully-qualified `require` alias not grepped could be missed.

---

# H. THREE MOST SURPRISING DESIGN CHOICES

**1. A drag handle gets bounds two million units wide.** So the shared rect-tree broad phase can never miss a handle, each handle node is given a `2R × 2R` box with the true anchor sitting at local `[R,R]` — the real hit is then done by `handle-hit?` reading the recorded center.
SOURCE: `src/app/client/substrate/chrome_derive.cljc:21, 145-155`
EXTRACTION:
```clojure
(def broad-phase-radius 1000000.0)
;; Root origin is [-R,-R]. A child at [x,y] therefore has
;; absolute broad-phase bounds [x-R,y-R,2R,2R], with its
;; true handle anchor exactly at local [R,R].
node-bounds {:x x :y y :w (* 2.0 broad-phase-radius) :h (* 2.0 broad-phase-radius)}
```

**2. Connectors are drawn by pretending to be a freehand ink stroke.** `route-path-material` synthesizes a `:path/kind :ink` material whose knots are the route's polyline points, all at pressure 1.0, then runs the path tessellator. The connector family owns no stroke geometry of its own.
SOURCE: `src/app/client/substrate/connector_route.cljc:334-352`
EXTRACTION:
```clojure
:path/kind :ink
:path/geometry
{:knots (mapv (fn [index point]
                {:knot/id [id index] :position point :pressure 1.0
                 :gesture-time :explicitly-absent :source-event-ids []})
              (range) (:stroke-points resolved))
 :base-width (:stroke-width resolved) :cap :round :join :round}
```

**3. An entire 3D world is one row on the tape.** Everything inside a region is invisible to the outer ordering system; depth buffer replaces scene order for the interior. (Verbatim in F above: *"Interior objects never enter the tape; depth owns their order."*)
SOURCE: `src/app/client/substrate/region3d_scene.cljc:1236-1238`

*Runner-up:* a connector emits an entry into **another family** (the text family) and reads that family's id from the live text system rather than naming it:
SOURCE: `src/app/client/substrate/webgpu/renderer.cljs:2982-2985`
```clojure
"The connector label paint door. Family identity is read from the live
 cloned text geo; connector code never hardcodes an MSDF or Slug family."
```
*Runner-up 2:* the tape's `:order-hash` is not a hash — it's a printed preimage, deliberately, so CLJ and CLJS agree byte-for-byte (`scene_tape.cljc:743-746`).

---

# I. GENERATIONS

**FACT — the marker vocabulary is thinner than assumed.** Across all of `src/`, only two systematic uppercase in-comment markers exist: `IMAGE-ATOM` (20 hits, `T1`–`T15` / `G1`,`G4`,`G8`, in `renderer.cljs`, `scene_tape.cljc`, `scene_store.cljc`) and `SEAM-STEP1` (8 hits, `T1`/`T6`/`T8`/`T12`, in `scene_tape.cljc` and `renderer.cljs`). There is **no** `PATH-ATOM`, `CONNECTOR-ATOM`, `CHROME-ATOM`, or `REGION3D-SEAM/FLOOR/POINTER` comment marker anywhere.

**FACT — the generations live in the registration data instead, as `:receipts` vectors.** This is the real generation record:
SOURCE: `src/app/client/substrate/scene_tape.cljc:480-495`, `region3d_material.cljc:688-690`
EXTRACTION:
```
rect      [:w2-a/q5 :w0-a/sdf :w2-b/ordered-executor]
shadow    [:w2-b/unconditional-effect-geometry]
msdf      [:w0-a/msdf-47-mismatch-counterexample]
slug      [:w0-a/slug :w2-a/q8]        clip [:w2-b/shared-visibility]
image     [:image-atom/admission :geometry :color :resources :store-lane]
path      [:path-atom/… + :product-pick]
connector [:connector-atom/… + :product-pick :durable-r1-read]
chrome    [:chrome-atom/… + :session-only-absence]
region-3d [:region3d/s1-order :s2-depth :s3-edit :s4-color :s5-resources]
```
Reading order of arrival: W0-A (text/sdf) → W2-A/W2-B (rect, shadow, clip, ordered executor) → IMAGE-ATOM → PATH-ATOM → CONNECTOR-ATOM → CHROME-ATOM → REGION3D s1–s5, with SEAM-STEP1 and W4 as the frame-graph rework that runs across all of them.
UNCERTAINTY: that ordering is inferred from the receipt naming and the `IMAGE-ATOM G1/Q5` comment at `scene_tape.cljc:330` ("Keep the historical three-argument result byte-for-value identical for the five admitted W2-B families") — the code does not state a timeline; git history was not consulted by this gatherer.

**FACT — the verifier mirrors the generations in ASCII banners:** `CHROME ATOM:1439`, `PATH ATOM:1833`, `CONNECTOR ATOM:2210`, `REGION3D FLOOR:4191`. There is no `IMAGE ATOM` banner even though `run-image-atom!` exists at `:1364`.
