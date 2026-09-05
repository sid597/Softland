# 3D kind — fact base, session 1 (Claude lane, 2026-09-06)

The raw facts under `3d-kind.html`. Three hunters read disjoint slices of `src/app/client/` at HEAD `eeaf4f9` (region3d was unmodified in the working tree at the time: `git status --short src/app/client/` empty). Fence: `src/app/client/` only; never `src/app/server/env.clj`. Docs read: `docs/below-the-waist/path-kind/` (path-kind.md, HANDOFF-8.md, attack-1.md, fact-base-9.md, bench handovers) and `docs/below-the-waist/3d-ceilings-starter.md`. Every line below is CHECKED unless marked. Hunter reports are reproduced as delivered, lightly trimmed of preamble; nothing was rewritten.

The page derives from these and from the field; where the page says CHECKED, the anchor is here.

---

## Part 1 — the region contract and the derived scene (hunter A: `region3d/component.cljc`, `region3d/scene.cljc`)

### 1. THE REGION ROW — `region3d/component.cljc`

Schema at `component.cljc:593-615`. All nine keys are **required** (`:keys`), there is no `:optional` set, and any extra key is rejected `:schema/unknown-key` (`engine/schema.cljc:182-184`).

```
{:region/id        any non-nil        ; some?        component.cljc:597
 :region/revision  any non-nil        ; some?        :598
 :region3d/version 2 exactly          ; #{schema-version}, schema-version=2 :15,:599
 :extent  {:width :height :depth}     ; each positive-number? and <= 1.0e4  :222-233
 :scene   {any-non-nil-key -> object} ; [:map-of some? object] :601, walked at schema.cljc:110-131
 :view    {...}                       ; :213-220
 :background {:kind :color}           ; :235-238
 :ambient {:color :intensity}         ; :240-245
 :region/rect {:x :y :w :h}}          ; x,y finite; w,h positive; no maximum :518-524
```

`extent-max` is `1.0e4` (`:18`). Region-level form validators, each with an `:explain` fn: `ids-match-keys?` (`:526`, error `:region/object-id-mismatch`), `parents-exist?` (`:546`, `:region/parent-missing`), `acyclic?` (`:586`, `:region/parent-cycle`) — `:606-615`.

`canonical-region` (`:747-765`) fills `:scene`/`:view`/`:background`/`:ambient` only. It never supplies `:extent`, `:region/rect`, `:region/id`, `:region/revision`; `migrate-region` (`:728-745`) rewrites version 1→2 and folds `:view-default` into `:view`. `validate-region!` (`:767-773`) = `schema/check` after canonicalization; docstring `:770-771`: "Downstream APIs do not all call this automatically."

**:extent vs :region/rect.** `:extent` is a 3-number `{:width :height :depth}` box; grep across `src/app/client` finds it written once (`harness/region.cljs:105`) and read by **nothing** — `renderer.cljs` has zero matches for "extent". `:region/rect` is 2D and is the pixel/layout rect: `renderer.cljs:1051-1064` reads `{:keys [w h]} (:region/rect raw-region)` to build `pixel-size` and `encode-pixel-size` (× zoom × dpr / encode-scale), and `scene.cljc:1080` explicitly drops `:region/rect` from the evaluation key. Camera aspect derives from that rect-derived viewport, never from `:extent` (`scene.cljc:747`, `renderer.cljs:1094`).

### 2. THE OBJECT — `component.cljc:501-516`

```
required: :object/id  any non-nil (:505)
          :object/kind ∈ #{:mesh :light :empty :text :ink} (:24, :506)
          :transform  TRS map (:508 -> :172)
          :provenance {:asserted-by non-nil, :act optional} (:480-484)
optional: :parent (validator (constantly true), :507)  ; any value; existence only via parents-exist?
          :mesh :component :light :text :ink
```

`body-matches-kind?` (`:486-499`) makes bodies exclusive: `:mesh` ⇒ exactly `#{:mesh :component}`; `:light` ⇒ `#{:light}`; `:text` ⇒ `#{:text}`; `:ink` ⇒ `#{:ink}`; `:empty` ⇒ none.

**Transform** is TRS, never a matrix (`:172-177`): `:translation` finite vec3, `:rotation` unit quaternion `[x y z w]` within 1e-9 (`:149-155`), `:scale` finite vec3 — negative and zero scale pass (`vec3?` only; docstring `:621-622`: "Zero scale is allowed by schema although inverse/normal computations may be singular"). Composition order is **T×R×S** (`scene.cljc:135-142`).

**Geometry** (`mesh`, `:397-408`), kind ∈ six primitives + `:indexed-triangles`, dispatched by `geometry-matches-kind?` (`:386-395`):
- `:indexed-triangles` (`:373-384`), exact keys `#{:kind :positions :normals :indices}` — `positions` flat vector, `count % 3 = 0`, ≤ 65536 verts (`mesh-vertex-max :22`), all finite; `normals` same rules and `count` must equal positions (`:358-363`); `indices` flat, `%3=0`, ≤ 131072 tris (`:23`), non-negative ints, each `< vertex-count` (`:365-371`). Empty vectors accepted (`:341`, `:351`).
- Primitives (`:330-336`) with per-kind params: box `{:size pos-vec3}` `:257`; plane `{:size pos-vec2}` `:262`; sphere `{:radius pos, :width-segments 3..4096, :height-segments 2..4096}` `:267`; cylinder/cone `{:radius :height pos, :radial-segments 3..4096}` `:278`; torus `{:radius :tube pos, :radial-segments/:tubular-segments 3..4096, tube<radius}` `:287-306`.
- **No uvs, no per-vertex colours, no tangents** — grep for `uv|texcoord` in both files: not found. A geometry value holds positions, normals, indices only.
- Mismatch worth noting side by side: `mesh` declares `:optional #{:params :positions :normals :indices}` (`:399`) with loose `vector?`/`map?` validators (`:403-406`), but the form validator re-checks with exact-key specs (`:392-394`), so e.g. a `:box` mesh carrying `:positions` is rejected as an unknown key by `primitive` (`:331`).

**Material** = `component-spec` (`:247-255`): `:base-color`, `:metallic` 0..1, `:roughness` 0..1, `:emissive`. Colours are `color/tagged` = `{:rgba valid-rgba?, :color-space #{:srgb}, :alpha-association #{:straight}}` (`engine/color.cljc:54-58`). No textures, no maps, no per-object shadow flags.

**Visibility: not found.** No `:visible`/`:hidden`/`:enabled` field anywhere in these files. The only related derived value is `:transparent?` (`scene.cljc:820-823`), true for `:text`/`:ink` or when base-color alpha < 1.0.

**Non-mesh objects exist**: `:light`, `:empty` (transform only), `:text` = `placed-text` `{:ref {:address non-nil} :params {:color? :max-inline-size?}}` (`:460-474`), `:ink` = `placed-ink` `{:ref …}` (`:476-478`).

### 3. LIGHTS, CAMERA, VIEW

**Light** (`:444-458`): required `:kind` ∈ `#{:directional :point :spot}` (`:26`), `:color` tagged, `:intensity` non-negative, `:range` positive — **required even for directional** (`light-matches-kind?` `:418-420`), `:cast-shadow` boolean; `:cone` optional and legal only for spot (`:416-417`), shape `{:inner-deg :outer-deg}` each 0..89 with inner ≤ outer (`:423-434`). `cast-shadow` true is allowed only on directional (`shadow-only-directional?` `:436-442`; its docstring: "Does not limit how many directional lights request shadows"). No per-light bias/resolution — shadow settings are global constants at `:77-84`.

**View** (`:213-220`) is orbit-shaped, not eye/target/up: `:pivot` vec3, `:distance` positive, `:yaw` finite, `:pitch` finite (radians — `scene.cljc:711-715`), `:lens`. There is **no `:aspect` and no `:up` field**: aspect is derived in `camera-matrices` as `(/ width (max 1.0 height))` from the passed viewport (`scene.cljc:747`), and up is hardcoded `[0.0 1.0 0.0]` (`scene.cljc:749`).

**Lens** (`:198-211`): `:kind` ∈ `#{:perspective :ortho}`, `:near`/`:far` positive with `near < far` (`:189-196`); `:fov-y-deg` in `(0,170)` exclusive (`:207`) required for perspective, `:ortho-scale` positive required for ortho — enforced by exact key-set match per kind (`:184-186`). Defaults `:40-44`.

**Ambient** (`:240-245`): `{:color tagged, :intensity non-negative}`, default intensity 0.1 (`:38`). **Background** (`:235-238`): `{:kind ∈ #{:opaque :transparent}, :color tagged}`, default at `:28-32`.

### 4. WHAT SCENE.CLJC DERIVES

| thing | fn:line | in → out |
|---|---|---|
| world transforms | `compose-hierarchy:209` | canonical region → `{object-id mat4}`, memo atom local, ids walked `sort-by pr-str` (`:226`) |
| instance rows | `derive-instance-row:804` | object + matrix → `{:object-id :kind :matrix :component :light :placement :transparent?}` |
| CPU mesh | `object-mesh:449` / `primitive-mesh:424` | object → `{:positions :normals :indices}`; primitives regenerated per call, no cache in these files |
| world triangles | `transformed-triangles:466` | object + matrix → `[{:object-id :triangle-index :a :b :c :normal}]`; `:normal` is the **geometric** normal `cross(b-a, c-a)` (`:486`), vertex normals are dropped |
| BVH | `build-bvh:526` | triangles → `{:bvh/version :kind :bounds {:min :max} :object-ids #{} (:triangles \| :left :right)}`, median split on largest axis, leaves ≤ 8 (`bvh-leaf-size :23`) |
| BVH refit | `refit-bvh:560` | preserves `identical?` untouched subtrees (`:595-597`) |
| full scene | `derive-scene:825-859` | region → `{:derive/version :region :world-transforms :instances-by-object :instances :triangles-by-object :bvh :stats}` |
| shadow space | `shadow-light-space:984` | maintained → `{:algorithm-version :light-id :view :bounds :texel-world :constants}` or nil; first directional cast-shadow light by sorted id, scans **all** triangles (`:1001`) |
| camera | `camera-matrices:739` | view + `[w h]` → `{:camera/version :eye :view :projection :view-projection :inverse-view-projection :viewport :lens}` |

**Gating.** `evaluation-key:1071-1086` splits `:static-component` (region minus `:region/id :region/revision :region/rect :view :background`, and each object minus `:transform`) from `:transforms`. `evaluate-scene:1088-1125`: `:full` → `derive-scene`; `:none` when transforms equal; else `:transform` → `maintain-transforms:926` → `maintain-affected:884` (`compose-affected:861` + `refit-bvh`), affected set from `affected-descendants:230`. `:region/revision` does **not** gate anything inside scene.cljc; it gates the outer key at `frame.cljc:16-18` used by `renderer.cljs:1047-1049`.

**Retention: none here** (ns docstring `:6-8`). The `maintained` map is held by the renderer's `:!prepared` rows (`renderer.cljs:1037`, `1048`, `1145`).

### 5. QUERIES

- `pick-region:965-982` — in `{:maintained, :camera, :region-point [x y]}`; out either `{:route :object :object-id :point3 :normal :t :triangle-index :boundary?}` (`:978-980`) or `{:route :region-background :region-id …}`. Throws `:region3d/missing-camera` when camera is nil (`:972-974`). **Callers: harness only** — `harness/region.cljs:600, 618, 624, 1023, 1027`. None in `renderer.cljs`.
- `ray-from-region-point:764-777` — camera + `[x y]` → `{:origin :direction :ndc}`; divides by `(:viewport camera)`, so the point is in **camera-viewport pixels**, which the renderer sets to `encode-pixel-size` = rect w/h × encode-scale (`renderer.cljs:1062-1064, 1094`) — not page coordinates and not CSS px unless the scale is 1. Docstring `:768`: "Coordinates must match the camera viewport." Caller in `src/`: `pick-region:976` only.
- `query-bvh:670-690` — bvh + `{:origin :direction}` → nearest hit or nil. Callers: `pick-region:977`, `harness/region_oracle.cljc:170`.
- `ray-triangle:626-655` — ray + triangle → the triangle map plus `:t`, `:point3` (world), `:normal`, `:barycentric [1-u-v u v]`, `:boundary?`. Two-sided (docstring `:629-630`). Caller: `query-bvh` only.
- `project-point:779-802` — camera + world point → `{:screen [x y] :depth}` in the same viewport pixels, nil when `w ≤ 1e-7`. Callers: `on_plane.cljc:166`, `harness/region.cljs:613, 622`.

**Hover / nearest-without-click: not found.** Only exact-point ray casting. No rect query, no frustum query, no k-nearest, no distance-to-nearest, no proximity radius.

**Face: not found.** Answers carry `:triangle-index` only (`:484`, `:979-980`); nothing groups triangles into faces or names them, even though `box-mesh:254-282` builds six flat-shaded quads.

### 6. COORDINATE CHAIN

object-local → scene-world: `trs-matrix:135` + `compose-hierarchy:209` / `compose-affected:861`, applied by `transform-point:144`, `transform-direction:163`. world → view: `look-at:692` with eye from `orbit-eye:706`, up fixed `[0 1 0]` (`:749`). view → clip/NDC: `perspective-matrix:717` (row 3 `[0 0 -1 0]`, depth `far/(near-far)`, `:726-727`) or `ortho-matrix:729` (`:736`). NDC → viewport pixels: `project-point:800-801`. Viewport pixels → NDC → world ray: `ray-from-region-point:770-777` (unprojects depth 0 and 1 through `:inverse-view-projection`). world → object-local: `inverse-mat4:172`, used by `on_plane.cljc:79-81`, not by picking. Region-pixel → page: **absent from both files** — done in `renderer.cljs:1051-1064` from `:region/rect` w/h with zoom/dpr, and the rect x/y offset is applied by hand in the harness (`harness/region.cljs:617-629`).

Numbers: ordinary Clojure/CLJS numerics throughout — f64 in ClojureScript, doubles on JVM; explicit `(double …)` only inside `Math/abs` calls (`:190, :615, :636, :652-654, :665, :1006`) and the aspect divide (`:747`). f32 exists only at the GPU boundary, outside these files (`renderer.cljs:615-620`, `js/Float32Array.`).

Rebasing around camera or region: **not found.** Coordinates stay in scene-world; the only snapping is shadow texel quantization (`:1018-1028`).

### 7. NESTING

Objects nest only through `:parent` (`component.cljc:503, 507`), an id inside the **same** `:scene` map, checked by `parents-exist?:546` and `acyclic?:586`.

Region-in-region: **not found.** The region schema (`:593-595`) has no field pointing at another region id; `:region/id` appears as identity (`:597`) and in the pick miss route (`scene.cljc:981-982`). `:scene` values are validated against `object` only (`:601`) — never a nested region or scene. Object bodies are mesh/component/light/text/ink (`:503`); no `:scene` and no `:region` key exists on an object. The closest to a reference is `placed-ref` `{:address some?}` (`:460-462`) inside `:text`/`:ink`, an opaque address these files never resolve (`canonical-placed-text` docstring `:691`: "Does not resolve content references.").

### 8. IDENTITY

Object identity is `:object/id`, any non-nil value (`:505`), required to equal its scene map key (`ids-match-keys?:526-533`). All ordering and tie-breaking is by printed form: `sort-by pr-str` at `:226, :832, :911, :1000` and `component.cljc:543, 566, 584`, and `hit-before?:667` breaks distance ties by `(compare (pr-str object-id) …)`.

Triangle identity in an answer is the pair `{:object-id, :triangle-index}` (`:483-484`); `refit-bvh` keys replacements by exactly that pair (`:580-585`). `:triangle-index` is positional into the object's own index array, so changing `:indices` or primitive `:params` renumbers it silently. No face identifier exists.

Revision: only the region carries `:region/revision` (`:598`); **no object-level revision field exists** in the object schema (`:501-516`). And `evaluation-key` drops `:region/revision` (`scene.cljc:1080`), so derivation keys off value equality, not the revision; the revision only gates the outer frame key (`frame.cljc:16-18`).

### 9. LIMITS AND NUMBERS

`extent-max 1.0e4` (`:18`) enforced at `:225-233`. `mesh-vertex-max 65536` (`:22`) via `flat-vec3-data?:338-346` at `:377-380`. `mesh-triangle-max 131072` (`:23`) via `triangle-index-data?:348-356` at `:381`. Segment counts 3..4096 (sphere height 2..4096) at `:271-276, :283-285, :299-304`; the comment at `:19-21` states primitive segment products can generate more vertices/triangles than the indexed limits. **No max object count and no max light count** in either file; the eight-light cap is referenced only in harness prose (`harness/region.cljs:574`).

Precision: `quaternion-tolerance 1.0e-3` for migration (`:17`) versus the strict 1e-9 unit check (`:155`). `ray-epsilon 1.0e-7` (`scene.cljc:22`) does sextuple duty: singular-pivot threshold (`:190`), slab near-parallel test (`:615`), Möller–Trumbore determinant / barycentric slack / `t > eps` (`:636-646`), `:boundary?` classification (`:652-654`), hit tie-break window (`:664-665`), degenerate `normalize` fallback (`:68`), `project-point` w cutoff (`:797`), shadow extent test (`:1013`). Near/far get only positivity and ordering (`:189-196`) — no ratio clamp. `camera-matrices` clamps the height denominator only, `(max 1.0 (double height))` (`:747`); its docstring says "Clamps height denominator but not zero width or degenerate orbit poles" (`:742-743`). Shadow constants at `component.cljc:77-84`: size 2048, bounds-padding 0.05, PCF `[3 3]`, depth bias 2 / slope 2.0, shader comparison offset 0.0015. `bvh-leaf-size 8` (`:23`).

### 10. DOCSTRING CLAIMS (verbatim)

`component.cljc:2-9`:
> "Canonicalize and validate region data. / Input: region/object/view values. Output: canonical values, schema acceptance or named errors. No retained state. Defaults and schemas define version 2; object kinds mesh/light/empty/text/ink; six primitive kinds; perspective/orthographic lenses; directional/point/spot lights; source references and provenance. :empty is a legal transform-bearing object kind in this source."

`scene.cljc:2-12`:
> "Derive spatial state and maintain transform changes. / … No namespace-owned scene state survives a call; local memo atoms are scratch. Purely returned scene state belongs to its caller. / Full derivation builds transforms, instances, world triangles and a BVH. Transform-only changes update dependents and refit retained topology; static component changes rebuild it."

Scope / negation claims:
- `scene.cljc:969-970` (pick-region): "Intended for mesh picking; text/ink placements are not in this BVH."
- `scene.cljc:629-630`: "Two-sided CPU query differs from backface-culling GPU pipelines."
- `scene.cljc:674`: "Does not order children by ray-near distance."
- `scene.cljc:427-428`: "Despite "validated" wording it does not perform complete schema validation itself."
- `scene.cljc:989-990`: "one shadow space serves the renderer even if several lights request shadows."
- `scene.cljc:1093-1094`: "Excluded background/view changes must be handled by renderer, as they are."
- `scene.cljc:931`: "Canonicalization is not full transform schema validation."
- `scene.cljc:287-288` (plane-mesh): "This plane differs from placement-local XY planes."
- `component.cljc:770-771`: "Downstream APIs do not all call this automatically."
- `component.cljc:438-440`: "Does not limit how many directional lights request shadows."
- `component.cljc:161-162`: "Does not silently repair arbitrary rotations."
- `component.cljc:361`: "Does not normalize normals."
- `component.cljc:691`: "Does not resolve content references."
- `component.cljc:621-622`: "Zero scale is allowed by schema although inverse/normal computations may be singular."

---

## Part 2 — the renderer and the engine seam (hunter B: `region3d/renderer.cljs`, `region3d/frame.cljc`, `engine/*`, plus the only caller `harness/region.cljs`)

### 1. What enters per prepare / per frame

- `prepare-region3d-frame!` — `region3d/renderer.cljs:1023`, args `renderer.cljs:1032-1035`: `[system {:keys [regions]} session {:keys [zoom dpr world-transforms font-assets session-layout-snapshot path-system max-lease-size] :or {zoom 1.0 dpr 1.0}}]`. Returns `{:regions <per-id counters> :composite-uploads n :held-passes 0}` (`1210-1212`).
- `font-assets` is destructured and never used in the body (`1033`; docstring admits it, `1031`: "Font-assets is destructured but unused.").
- Session revision comes only from `(:revision session-layout-snapshot)` (`1038`); raw session contents never enter the key (`1029-1031`).
- Group index per region: `(transform/buffer-index world-transforms (:container %))` (`1040-1041`), which throws `:transform/unknown-group` (`engine/transform.cljc:283`).
- Dependency key — `region3d/frame.cljc:15-18`: `[(:region/id region) (:region/revision region) (:container draw-item) zoom dpr session-revision]`. `frame-key` is `mapv` of that (`frame.cljc:25-26`).
- Callers: prepare — `harness/region.cljs:429` only; `encode-region-pass!` — `harness/region.cljs:383`; `composite-region!` — `harness/region.cljs:349`. **No production (non-harness) caller in `src/`.** `frame/region-key`/`frame-key`: renderer + `test/app/client/region3d/frame_test.clj` only.
- Per-encode input: `encode-region-pass! [system encoder region-id role lease]` (`1332`). Per-composite: `composite-region! [pass region-system region-id]` (`1396`) — it re-reads the lease from the binding owner itself (`1397-1399`).

### 2. Pixel size

- `renderer.cljs:1053-1054`: `pixel-size [(max 1 (ceil (* w zoom dpr))) (max 1 (ceil (* h zoom dpr)))]` from `:region/rect`'s `w`/`h` (`1051`).
- `1055-1059`: clamp each axis to `max-lease-size` **only if supplied**, then `compositor/quantize-region-size`.
- `quantize-region-size` — `engine/compositor.cljs:398-407`: `ceil(v/256)*256`, then `(min 4096)`. Constants `region-lease-quant 256`, `region-lease-max 4096` (`compositor.cljs:25-26`).
- `max-lease-size` = `(min region-lease-max max-texture-dimension-2d)` at `compositor.cljs:389`; harness passes it in at `harness/region.cljs:411`. Docstring `compositor.cljs:383`: "Stores device-bounded :max-lease-size, but quantize-region-size itself uses the fixed 4096 cap."
- Bucket ladder is a **separate** quantity: `encode-scale-step 1.12` (`995`), `encode-scale-bucket` = `floor(log(scale)/log(1.12))` (`997-1006`), `quantize-encode-scale` = `1.12^bucket` (`1008-1013`), applied to `(* zoom dpr)` (`1060-1061`). `encode-pixel-size = ceil(w*encode-scale)` (`1062-1064`) is fed **only** to `scene/camera-matrices` as the viewport (`1094`) — it does not size any texture.
- Rungs `[1 2 4 8]` — `engine/rungs.cljc:11`; each divides the desired size before quantizing (`rungs.cljc:32-33`). Admission is bytes-only: `:admitted? (<= projected-reserved-bytes budget-cap-bytes)` (`rungs.cljc:63`). No size test, no screen-size test anywhere.
- Page zoom larger than screen: nothing clips. The only ceilings are 4096 / `maxTextureDimension2D` and the byte budget. Past them the region keeps the same page rect and samples a smaller texture.
- Rejection fill: `acquire-region-lease!` catches any error into `{:rejected? true :rejection …}` (`compositor.cljs:548-560`, and the shadow-add path `484-492`), which originates from `ensure-target-capacity!` throwing `:frame-target-budget-exceeded` (`compositor.cljs:142-155`). `composite-region!` then picks the `:rejection` pipeline (`renderer.cljs:1400-1404`) → `rejection-fragment-shader` (`renderer.cljs:293-300`): 12×12 dark-red checker with an orange top-right corner, alpha 1. When `rung-divisor > 1` it picks `:worn` (`1403`) → cyan diagonal stripes in the top-right corner (`280-289`).

### 3. Passes, in order

Per region per frame the harness loops roles `[:shadow :interior]` when `shadow?`, else `[:interior]` (`harness/region.cljs:379-385`).

- **Shadow** — `encode-shadow!` `renderer.cljs:1268-1293`. `:colorAttachments []`, depth = `lease :shadow :view`, clear 1.0, store. Draws `opaque ++ transparent` (`1285-1286`), one `.draw` per object. Pipeline `renderer.cljs:395-405`: `depth32float`, write true, compare `"less"`, `:depthBias 2 :depthBiasSlopeScale 2.0`, no `:multisample` key (defaults to 1). One map only, 2048² (`compositor.cljs:474`, `535`).
- **Interior** — `encode-interior!` `1295-1322`. Color = `lease :color-msaa :view` with `:resolveTarget (lease :resolve :view)`, `clearValue` from `clear-color` (`1242-1252`; transparent background forces alpha 0), depth = `lease :depth :view` clear 1.0. Draw order: opaque meshes → transparent meshes → on-plane placements (`1315-1321`). No `setViewport`/`setScissorRect` in this file (grep: none).
- Mesh pipelines `380-394`: target `rgba16float`, **blend only on the transparent variant** (`389`) using premultiplied `one / one-minus-src-alpha` (`309-315`); `triangle-list`, `frontFace ccw`, `cullMode back`; `depthStencil {:format "depth24plus" :depthWriteEnabled (not transparent?) :depthCompare "less"}`; `:multisample {:count 4}`.
- Placement pipeline `on_plane_renderer.cljs:86-113`: `depth24plus`, write **false**, `less-equal`, `depthBias -1`, slope `-1.0`, premultiplied blend, `cullMode none`, `multisample 4`.
- Sorting — `draw-order` `883-913`: opaque near-first `(juxt :depth (pr-str id))`, transparent far-first `(juxt (comp - :depth) …)`; `:depth` is eye-to-**object-origin** distance (`883-892`, docstring: "Object origin is a sorting proxy, not surface depth"). A transparent *background* pushes every mesh into the transparent list (`901-908`).
- **Composite** — `composite-region!` `1390-1411`: sets pipeline, bind group, the shared composite instance buffer, then `.draw pass 6 1 0 composite-buffer-index` — six vertices, one instance, first-instance selects the region's row. It draws into a pass the *caller* opened; the harness opens one `rgba16float` scene pass and paints path-below → region → path-above (`harness/region.cljs:341-354`, `387-392`).
- Whole-canvas present is separate: `compositor/draw-present!` `compositor.cljs:644-671`, one full-screen triangle (`33-44`), unpremultiply → linear→sRGB → repremultiply (`46-66`).

### 4. The composite seam

- The rect is the region's own `:region/rect` `{x y w h}` taken from the draw item (`renderer.cljs:1180-1188`), packed as 4 f32 + 1 u32 group index (`composite-row-bytes` `946-957`, stride 20 = `composite-instance-stride`, `33`).
- The instance slot index is the **logical lease buffer index** from `engine/leases.cljs` (`renderer.cljs:1193`, `upload-composites!` `959-993`), not the group index; the group index rides inside the row.
- `composite-vertex-shader` `245-266`: `local = rect.xy + uv*rect.zw`; `world = c.translation + c.axis_x*local.x + c.axis_y*local.y` — so `:container`'s composed affine **does** transform the rect (including shear/rotation of the quad's corners). Then `pixel = world*zoom + pan`, with `zoom→1, pan→0` when `flags & 1u` (screen-fixed group).
- Nothing from the page camera reaches the region's interior. `camera-matrices` is called with `(view, encode-pixel-size)` only (`1094`); `view` is `(or (:view session-row) (:view raw-region))` (`1087-1088`). Page zoom/dpr affect (a) lease pixel size, (b) `encode-pixel-size` → the 3D **aspect ratio** and pick-space resolution, (c) the `encode-bucket` term in `view-key` (`1089-1092`) that decides whether the camera uniform is re-uploaded. The 3D projection is otherwise unchanged; between buckets the interior is **resampled**, not re-projected.
- Sampling filter: `(.createSampler … {:minFilter "linear" :magFilter "linear"})` (`renderer.cljs:527-528`) — no address mode, no mipmap filter, no mips anywhere (`limits/texture-bytes` is always called with mip count 1, `compositor.cljs:444-448`). Bound at `composite-bind-group` `1366-1377` together with the shared camera buffer and groups storage buffer.

### 5. Engine compositor

- A **lease** = one bundle keyed `[region-id qw qh]` (`compositor.cljs:460`): `color-msaa` rgba16float ×4 `RENDER_ATTACHMENT`; `depth` depth24plus ×4; `resolve` rgba16float ×1 `RENDER_ATTACHMENT|TEXTURE_BINDING`; optional `shadow` depth32float 2048² ×1 (`510-545`). Priced by `region-lease-bytes` `439-449`.
- Lifetime: held in `:!region-leases` across frames; shadow can be added (`466-492`) or dropped (`494-501`) in place; retired when the id leaves the desired set (`723-726`) or when the granted key changes (`746-753`); actual destruction deferred to `release-after-submit!` with an identity guard (`686-697`).
- Rungs gate on **bytes only** (`rungs.cljc:63`), crediting a replaced lease (`40`) and pricing a same-key shadow addition as the delta (`44-48`).
- Targets: pooled by `[format w h samples]` (`86-91`); free targets reclaimed at frame boundaries (`93-140`, `204-212`); budget default 512 MiB (`27`, `74`).
- Live regions: **no count limit found** anywhere — only bytes, plus the 16384-entry affine table (`device.cljs:73`).
- Readback in this file: `strip-padded-rows` (`801`), `unpremultiply!` (`816`), `png-bytes!` (`835`) — all private, and grep over `src/` + `test/` finds **no callers at all**. The harness uses its own `w4-read-texture!` (`harness/shared.cljs:259`, called `harness/region.cljs:397`).

### 6. Transform and device

- Registry: `{:groups {0 root-group} :next-buffer-index 1 :free-buffer-indexes …}` (`transform.cljc:105-112`); group 0 reserved, unmutable, unremovable (`133`, `165`, `178`).
- Per group it derives `{:affine [a b c d tx ty] :flags (if screen 1 0) :buffer-index n}` (`world-transforms` `242-261`); `:camera` inherits from parent when unset (`237`); flag semantics: `:screen` ⇒ 1 ⇒ shader ignores camera zoom/pan (`renderer.cljs:258-260`; CPU twin `transform.cljc:344-347`).
- Numbers: CPU side doubles (`spec->affine` `81-97` `mapv double`); upload converts to f32 (`device.cljs:110-133`, `Float32Array` view over a 32-byte row: 6 floats + `flags:u32` + pad).
- Camera buffer: 24 bytes (`device.cljs:139-147`), six f32 `[pan-x pan-y zoom 0.0 w h]` (`149-162`).
- No rebasing around the camera: the composite shader applies `world*zoom + pan` in one step (`renderer.cljs:262`); `screen-bounds` does the same on CPU (`transform.cljc:348-351`).

### 7. Limits (numeric, with lines)

`limits.cljc`: selected adapter fields `29-36`; byte table for 8 formats `38-46`, unknown format throws `57-58`.
`compositor.cljs`: `region-lease-quant 256` `25`; `region-lease-max 4096` `26`; `default-pool-budget-bytes 512 MiB` `27`; `max-lease-size (min 4096 maxTextureDimension2D)` `389`.
`device.cljs`: `affine-entry-bytes 32` `71`; `max-transform-nodes 16384` `73`, enforced `107-109`.
`rungs.cljc`: divisors `[1 2 4 8]` `11`.
`renderer.cljs`: `max-lights 8` `29`; strides 24 / 112 / 80 / 20 `30-33`; `region-uniform-bytes 112` `34`; `shadow-uniform-bytes 64` `35`; initial light buffer 640 bytes `818`; buffer growth `max(required, 1.5×)` `474-476`; min buffer 4 bytes `461`; shadow map 2048 (`component.cljc:79`).
`on_plane_renderer.cljs`: ink vertex limit `maxBufferSize / 88` `237-246`.
No max-zoom and no max-region-count constant exists in any of these files.

### 8. Lights and shadows

- Eight, hard: `max-lights 8` (`29`), shader loop `min(u32(settings.x), 8u)` (`211-212`), `light-rows` takes the first 8 sorted by printed key (`698-707`; docstring `701`: "Extra lights are silently omitted from rendering here").
- Kinds encoded `directional 0.0 / point 1.0 / spot 2.0` (`729`); falloff and spot cone in `light_radiance` (`169-186`).
- **One** shadow map per region, from the first sorted **directional** light with `:cast-shadow true` (`scene.cljc:993-1000`). Every light whose `casts_shadow > 0.5` samples that same map (`renderer.cljs:215-217`; the shader comment at `190-191` states it: "All shadow-casting lights use the same selected shadow map").
- Technique: orthographic light-space fit over all mesh triangles, 5% padded, texel-snapped in x/y (`scene.cljc:1001-1036`; `shadow-projection` `renderer.cljs:752-764`); 3×3 PCF via `textureSampleCompareLevel` averaged /9 (`158-167`), comparison sampler `"less-equal"` with linear min/mag (`529-532`), constant bias `SHADOW_OFFSET 0.0015` (`113`). Constants mirrored in `component.cljc:77-84`.
- Objects shadow each other: the shadow pass draws every object into one map (`1285-1292`) and all lit fragments sample it. Alpha-transparent meshes cast solid shadows (docstring `1271-1272`; the shadow vertex shader has no fragment stage, `229-241`).
- Ambient is one constant term: `base.rgb * ambient.rgb * ambient.a + emissive` (`209-210`), packed from `:region/ambient` colour and intensity (`789-794`); default intensity 0.1 (`component.cljc:38`).
- Shadowing is switched off wholesale when `settings.z < 0.5` (`152`), and `settings.z` is `1.0` only if a shadow space exists (`796`). Fallback binding is a 1×1 `depth32float` texture (`497-510`, chosen at `1221-1222`).

### 9. Depth and precision

- Interior `depth24plus`, 4 samples, clear 1.0, `"less"`, write on for opaque / off for transparent (`392-393`, `1310-1313`). Placements `less-equal`, write off (`on_plane_renderer.cljs:86-88`).
- Shadow `depth32float`, write on, `"less"`, `depthBias 2` / slope `2.0` (`403-405`). Placement bias is negative: `-1` / `-1.0` (`on_plane_renderer.cljs:20-21`).
- **Not reversed-z**: `perspective-matrix` (`scene.cljc:717-727`) maps to a `[0,1]` range via `far/(near-far)` with `-1` in the w row, paired with clear 1.0 and `"less"`. `ortho-matrix` `729-737` matches. Shadow ortho `renderer.cljs:752-764` likewise `1/dz`.
- Near/far come from the lens: default perspective `fov-y 50°, near 0.1, far 1.0e4` (`component.cljc:41`); ortho `scale 10.0`, same near/far (`44`). No dynamic near/far fitting.
- Precision: all CPU math in Clojure doubles; every GPU upload goes through `typed-f32` → `Float32Array` (`renderer.cljs:614-620`), with `column-major` reordering (`607-612`). Normals use `1/dot(bx, cross(by,bz))` on the instance basis (`65`), which the comment at `37-40` calls singular for zero-scale inputs.

### 10. Docstring / comment claims, verbatim

- `renderer.cljs:9-14`: "Texture lease ownership remains in engine/compositor.cljs. // Preparation compares scene, view, background and placements. Dirty roles encode to offscreen leases; clean content is retained. Composition selects content, reduced-resolution marking or rejection fill."
- `compositor.cljs:12-15`: "A target is one texture; a region lease bundles color, depth, resolve and optional shadow targets. leases.cljs owns logical region associations; this file owns physical allocation and destruction. GPU texture readback is driven by the harness." (readback helpers here have zero callers — §5.)
- `frame.cljc:4-6`: "Input: region draw rows and engine/session stamps. Output: pure keys. No retained state."
- `rungs.cljc:6-8`: "No GPU work or retained state. It tries divisors [1 2 4 8] in order. This file chooses; the compositor allocates."
- `leases.cljs:7-8`: "It does not allocate textures." / `device.cljs:7-8`: "It does not acquire the browser device or project clips." / `limits.cljc:6-8`: "There is no state. format-bytes is a deliberately finite pricing table; unknown formats throw rather than being assigned an invented cost."
- Marked limitations: `rungs.cljc:16-18` ("Current limitation: physical compositor leases carry :shadow instead."); `compositor.cljs:414` ("The size arity accepts shadow? But does not use it in lookup."); `compositor.cljs:617-619` (negative-origin scissor not intersected exactly); `compositor.cljs:323-324` ("Owner label mentions frame-runtime, which is not a file in this client tree."); `renderer.cljs:1328-1331` ("unknown roles reach a no-op branch yet return :encoded? True"); `renderer.cljs:1435-1436` ("no call to this helper was found within this file" — grep across `src/`+`test/` confirms `ensure-frame-compositor!` has **no callers**; `replace-frame-compositor!` also has none); `renderer.cljs:786-787` ("also constructing unused packed light values"); `renderer.cljs:897-899` ("Per-object sorting cannot resolve intersecting transparency exactly"); `renderer.cljs:1298-1300` ("Mesh and ink transparency are separate ordered groups rather than one combined depth ordering"); `renderer.cljs:564-565` ("Replacement shared buffers on the same device do not replace the cached system"); `renderer.cljs:468-469` ("no explicit device-limit check here"); `renderer.cljs:1394-1395` ("missing logical index is not separately guarded"); `transform.cljc:272-274` (scale proxy, "not the maximum singular value under shear"); `on_plane_renderer.cljs:274-276` ("Maintained parameter is unused"). No `TODO`, `FIXME`, "not yet", or "unsupported" strings exist in any of the scoped files.

---

## Part 3 — placed content, the harness driver, the oracle (hunter C: `region3d/on_plane.cljc`, `region3d/on_plane_renderer.cljs`, `harness/region.cljs`, `harness/core.cljs`, `harness/region_oracle.cljc`)

### 1. PLACEMENT RECORD

**Two different shapes exist. The validated one is the *scene object*; the one the renderer consumes is an unvalidated map supplied by the caller.**

**1a. The validated scene-object body (`region3d/component.cljc`)**

- `component.cljc:460-462` — `placed-ref`: required keys `#{:address}`, validator `some?` (any non-nil value passes; no address format check).
- `component.cljc:472-474` — `placed-text`: required `#{:ref :params}`.
- `component.cljc:464-470` — `placed-text-params`: required keys `#{}` (empty), optional `#{:color :max-inline-size}`; `:color` → `color/tagged`, `:max-inline-size` → positive number.
- `component.cljc:476-478` — `placed-ink`: required `#{:ref}` only. **No params, no color, no width on ink.**
- `component.cljc:501-516` — `object`: required `#{:object/id :object/kind :transform :provenance}`, optional `#{:parent :mesh :component :light :text :ink}`. The **object id** is `:object/id`; the **local transform** is `:transform` (TRS, `component.cljc:172-177`); the **path/text reference** is `[:text :ref :address]` / `[:ink :ref :address]`.
- `component.cljc:486-499` — `body-matches-kind?`: `:text` ⇒ exactly `#{:text}` present; `:ink` ⇒ exactly `#{:ink}`. An ink object may not carry `:component`.
- `component.cljc:24` — `legal-object-kinds #{:mesh :light :empty :text :ink}`.
- Validation entry point: `component.cljc:767-773` `validate-region!` → `schema/check schema (canonical-region region)`. Its own docstring: *"Downstream APIs do not all call this automatically."*
- Canonicalization for text only: `component.cljc:688-698` `canonical-placed-text` strips nil params. **There is no `canonical-placed-ink`** (`canonical-object`, `component.cljc:700-714`, has no `:ink` branch).

**There is no plane definition and no anchor field in the schema.** The "plane" is implicit: local z=0 of the object's own transform (see §2). No `:anchor`, no `:plane`, no `:uv` key exists anywhere in the object schema.

**1b. The "resolved placement" the GPU lane consumes**

Not defined by any schema or constructor in `src/`. It is read key-by-key. The full observed key set, with the reading line:

| key | read at | used for |
|---|---|---|
| `:object-id` | `on_plane_renderer.cljs:208, 217, 301, 342, 354`; `on_plane.cljc:112` | matrix lookup into `[:world-transforms …]`, cache key, sort key |
| `:kind` | `on_plane_renderer.cljs:208, 226, 234` | `:ink` is the only drawable branch |
| `:status` | `on_plane_renderer.cljs:208, 221, 261` | must `= :resolved` to pack |
| `:content-revision` | `on_plane_renderer.cljs:209`; `on_plane.cljc:63` | cache-invalidation token |
| `:component` | `on_plane.cljc:109, 117` | the actual path component tessellated and painted |
| `:address` | `on_plane.cljc:62, 113` | carried into the pack and into layout `:source-id` |
| `:cache-key` | never read by `src/` — set by the harness at `harness/region.cljs:185` | — |
| `:text`, `:style`, `:layout` | `on_plane.cljc:46-63` | text-only, never reaches GPU |
| `:owner` | never read in `src/` | set at `harness/region.cljs:187` |
| `:matrix` | *written* by `on_plane_renderer.cljs:223, 229, 233` (from maintained), read at `:283` | model matrix |

The only place a resolved placement is *constructed* in the tree is the harness: `harness/region.cljs:181-187`.

**Who resolves a placement into a resolved placement: nobody in `src/`.** Grep for `resolved-placements` returns exactly six sites — one consumer (`renderer.cljs:1119`) and five harness sites. The key `:region3d/resolved-placements` arrives on the draw item already populated (`renderer.cljs:1119`, `harness/region.cljs:189`). `on_plane.cljc:6-8` states it plainly: *"it does not resolve external content addresses."*

The closest thing to a resolver is `on_plane_renderer.cljs:211-235` `pack-one` — cache, old row, placement, maintained scene → `{:cache :row :packed?}` — which turns a resolved placement into a `:packed` payload with one of four statuses: reused, carried-unresolved, `:ink` packed, or `:unsupported-kind`.

### 2. THE COORDINATE CHAIN FOR PLACED CONTENT

Page pixel → region rect → interior texture → ray → object local → path local, with the function at each hop.

1. **Page pixel ← region rect.** `renderer.cljs:245-266` `composite-vertex-shader`, line 261-263: `world = c.translation + c.axis_x * local.x + c.axis_y * local.y; pixel = world * zoom + pan;` `local` is the region's `:region/rect` `[x y w h]` (`renderer.cljs:257`). `zoom`/`pan` come from the camera uniform, **selected off** for screen-flagged groups (`renderer.cljs:259-260`). This is the only place page pan/zoom touches a region.
2. **Region interior pixel size.** `renderer.cljs:1060-1064`: `encode-scale (quantize-encode-scale (* zoom dpr))`, `encode-pixel-size = ceil(w*encode-scale), ceil(h*encode-scale)`. `encode-scale-bucket` = `floor(log(scale)/log(encode-scale-step))` (`renderer.cljs:997-1006`); `quantize-encode-scale` re-exponentiates (`renderer.cljs:1008-1013`). So the interior camera viewport is a *quantized* function of page zoom, not page zoom itself.
3. **Camera.** `scene.cljc:739-762` `camera-matrices` — view + viewport → `{:eye :view :projection :view-projection :inverse-view-projection :viewport :lens}`. Called at `renderer.cljs:1094` with `encode-pixel-size`.
4. **Region pixel → world ray.** `scene.cljc:764-777` `ray-from-region-point` — camera + `[local-x local-y]` → `{:origin (near-plane point) :direction (normalized) :ndc}`. Docstring: *"Coordinates must match the camera viewport."*
5. **Ray → the placement plane.** `on_plane.cljc:72-99` `ray->placement-plane` — `{:keys [origin direction]}` + `world-transform-matrix` → `{:t :point3 :object-local :component-local}` or nil. `:79` inverts the matrix, `:80-81` brings origin/direction into object local space, `:83` rejects |dz| ≤ `plane-epsilon` (`1.0e-9`, `on_plane.cljc:21`), `:84` solves `t = -oz/dz` — **the plane is object-local z = 0, hard-coded**, `:85` rejects `t ≤ 0`, `:89-94` re-derives world `t` by projecting back. **Callers: none in `src/`, none in `test/`.** Grep returns only the definition line.
6. **Object local XYZ → path-local 2D.** `on_plane.cljc:66-70` `object->component-local` — `[x y _z]` → `[x (- y)]`. Z is discarded, Y is negated. Called once, from `ray->placement-plane:99`. This is the inverse of the shader's flip at `on_plane_renderer.cljs:44` (`vec4(input.point.x, -input.point.y, 0.0, 1.0)`).
7. **Back out: path-local → page.** No function does this. The forward direction for anchors is `on_plane.cljc:151-187` `project-region-anchor`: object local `(:local binding)` → world via the object matrix (`:169`) → screen via `scene.cljc:779-802` `project-point` (`:170`) → depth-gated `0.0 ≤ depth ≤ 1.0` (`:172`) → `clamp-projection` to the region rect (`:171`, defined `on_plane.cljc:132-149`) → offset by the region's `:x/:y` (`:173-174`) → `transform/forward-point` region group then `transform/inverse-point` anchor group (`transform.cljc:293-317`). Output at `:179-187`: `{:status :resolved :kind :point :center :camera :clip :region :object :point3 :anchor-clamped}`. **Callers: tests only** (`test/app/client/region3d/on_plane_test.clj:54`).

**"placement zoom fixed at 1.0."** `on_plane.cljc:20` — `(def placement-zoom 1.0)`. It is used at exactly three places: `on_plane.cljc:64` (`:zoom` into `text-layout/layout`) and `on_plane.cljc:109` (the zoom argument to `derive-mesh-set`). What zoom does in tessellation, and only this: `tessellation.cljc:596-607` `derive-mesh-set` passes it to `component-cache-key` (`:599`) and `tessellate` (`:602`). `tessellate:577-579` passes it to `stroke-triangles`/`shape-triangles`, where it reaches `(:fan-resolution (zoom-lod zoom))` (`tessellation.cljc:209`). `zoom-lod` (`tessellation.cljc:25-37`) has three bands; `1.0` lands in `:engine-default` (`0.1 < z ≤ 8.0`), **fan-resolution 8** (`tessellation.cljc:18-20`). So: placed ink is always tessellated with 8-segment round caps/joins, and its cache key always carries LOD `:engine-default` (`tessellation.cljc:42-55`). Page zoom, region encode-scale and the object's own scale never re-tessellate it. In the harness fixture the ink object scale is `0.018` (`harness/region.cljs:167`) on a component whose points span 0→222 units (`harness/region.cljs:177-179`) — the LOD band never sees either number.

### 3. WHAT THE PLACED-INK LANE DRAWS

`region3d/on_plane_renderer.cljs`.

- **Vertex layout.** `:19` `flat-vertex-stride 88`. Attributes at `:98-109`: loc 0 `float32x2` @0 (local XY), loc 1-4 `float32x4` @8/24/40/56 (the four rows of a 4×4 model matrix, column-major via `:182-187` `column-major`), loc 5 `float32x4` @72 (linear premultiplied RGBA). 22 floats. `stepMode "vertex"` — **the matrix and color are repeated on every vertex**, acknowledged at `:276-277`: *"Repeats matrix/color per vertex and records origin distance. Maintained parameter is unused; large repeated matrices are a bandwidth/storage choice."*
- **Program.** One: `:26-50` `placed-flat-shader`. Vertex applies `region.view_proj * model * vec4(x, -y, 0, 1)`; fragment returns the interpolated color unchanged (`:48-50`). No lighting, no texture, no MSAA-aware coverage.
- **MSAA.** `:113` `:multisample {:count 4}`. It draws into the same MSAA color attachment as the meshes (`renderer.cljs:1305-1309`).
- **Depth.** `:86-88`: `depth24plus`, write false, compare `less-equal`, `depthBias -1`, `depthBiasSlopeScale -1.0` (`:20-21`). Occluded by other objects: yes — `less-equal` against the depth buffer written by the mesh passes in the same interior pass (`renderer.cljs:1315-1319` before `:1320`). Z-fights with the face it sits on: no, by construction — the negative bias pulls the ink toward the eye. `:24-25` comment: *"Placement passes test depth without writing it and use a small surface bias."* Writes no depth, so placed ink never occludes anything, including other placed ink.
- **Blend.** `:59-65` `blend-state` → src `"one"`, dst `"one-minus-src-alpha"` on both color and alpha — premultiplied over. Color target `:89` `{:format "rgba16float"}`.
- **Primitive.** `:112` `{:topology "triangle-list" :cullMode "none"}` — the ink is visible from behind the plane.
- **Kinds drawable.** `:ink` only. `:226-230` is the sole packing branch; `:231-235` produces `{:status :unsupported-kind …}`; `:407-416` `draw-placements!` `case`s on `:ink` and falls to `nil` otherwise. Namespace docstring `:10-11`: *"Resolved ink is the supported GPU placement kind. Resolved text and other kinds receive :unsupported-kind."*
- **Accepted but not drawn.** Any `:kind` other than `:ink` with `:status :resolved` → `:unsupported-kind`, retained and counted in `:coverage-check` (`:370`), zero vertices. Any placement whose `:status` ≠ `:resolved` → carried through unchanged (`:221-225`). Ink over the device buffer cap → `:over-limit`, `:vertices` cleared but `:vertex-count` kept (`:248-269`). Cap is `maxBufferSize / 88` (`:237-246`). Docstring `:240`: *"It is a buffer limit, not frame-time admission."* `:407` `:when (pos? count)` skips zero-count draws.

**Draw ordering (approximate, and stale under camera-only motion).** `:310-326` `sort-draws` sorts back-to-front by the Euclidean distance from `:eye` to the object's *origin* `[0 0 0]` transformed by its matrix, tie-broken on `pr-str` of the id. Docstring `:314-315`: *"Approximate transparency order for intersecting/extended surfaces."* And `prepare-placements!` `:333-335`: *"Current limitation: a camera-only change does not change pack key and retains old draw order. A camera-crossing transparency fixture would determine the visual consequence."* — confirmed in code at `:375-377`, where `sort-draws` runs only when `changed?`.

Coverage is not anti-aliased at the geometry level: the mesh carries `:coverage :aliased-v1` (`tessellation.cljc:585`); only the 4× MSAA gives edge smoothing.

Color path: `on_plane.cljc:101-117` `pack-placed-ink` → `path-component/paint-color` (`path/component.cljc:317-324`, straight RGBA with opacity folded into alpha) → `adapt-legacy-color` (`on_plane.cljc:23-38`) → `on_plane.cljc:119-130` `linear-premultiplied` called at `on_plane_renderer.cljs:285-287` **with coverage 1.0 and opacity 1.0 hard-coded**.

### 4. PICK/HOVER THROUGH PLACED CONTENT

**No function anywhere under `src/app/client/` answers "you hit this placed ink."** Exact absence, four ways:

1. `scene.cljc:965-982` `pick-region` returns `{:route :object :object-id :point3 :normal :t :triangle-index :boundary?}` or `{:route :region-background :region-id}`. Docstring `:969-970`: *"Intended for mesh picking; text/ink placements are not in this BVH."*
2. Structurally true: `scene.cljc:839-846` builds `triangles-by-object` with `(when (= :mesh (:object/kind object)) …)`, and `scene.cljc:449-457` `object-mesh` returns nil for anything but `:mesh`. `:text` and `:ink` objects contribute zero triangles to `build-bvh` (`scene.cljc:854`).
3. The one function that *would* be the ink-plane hit test, `on_plane.cljc:72-99` `ray->placement-plane`, has **no caller in `src/` or `test/`**. Even if called it stops at `:component-local` — it does not consult the path's own geometry.
4. The CPU path hit-test exists and is unwired to region3d: `path/component.cljc:254-292` `classify` (component + point + slop → `:inside`/`:boundary`/`:outside`), `:294-297` `hit?`, `:299-315` `boundary-distance`. Callers: `harness/path.cljs:354, 357, 375` and `test/app/client/path/component_test.clj:74, 87` — harness and tests only. Nothing in `region3d/` requires `path.component` except `on_plane.cljc:12` for `paint-color`.

**Hover: not found.** `grep -rni "hover" src/app/client/` returns exactly one line, `text/layout.cljc:130`, and it is a docstring listing hover among things that are *not* keys of that map. No nearest-without-click function exists for any kind. `nearest-by` (`text/layout.cljc:1268`) is text caret-stop selection.

Callers of `pick-region`: `harness/region.cljs:600, 618, 624, 1023, 1027` — **harness only**.

### 5. HARNESS: HOW REGIONS ARE MADE AND DRIVEN

All in `harness/region.cljs`.

- **Fixture constructors.** `:40-46` `region3d-tagged` (color), `:48-55` `region3d-transform` (TRS), `:57-69` `region3d-mesh`, `:71-82` `region3d-light`, `:84-92` `region3d-remint` (validates + rehashes `:region/revision` from content), `:94-136` `region3d-fixture-region`, `:138-148` `region3d-draw-item`, `:156-192` `region3d-boundary-fixture`.
- **Geometry source: a primitive generator, not triangle lists and not files.** `:66-67` sets `:mesh {:kind primitive :params (get region3d-component/primitive-defaults primitive)}`; `scene.cljc:449-457` routes non-`:indexed-triangles` meshes to `primitive-mesh` (`scene.cljc:424`), which builds box/plane/sphere/radial/torus meshes in code (`scene.cljc:254-424`). The `:indexed-triangles` branch exists (`component.cljc:373-384`) but **no harness fixture uses it**.
- **Objects and lights.** `:114-135` — five meshes (`:near` box, `:far` box, `:sphere` sphere, `:glass` box at alpha 0.5, `:floor` plane) and three lights (`:sun` directional with `:cast-shadow true`, `:point` with range 18, `:spot` with an 18°/32° cone). Eight objects. The boundary fixture adds a ninth, `:boundary/ink` (`:164-173`).
- **Nesting.** No fixture nests a region inside a region. The only nesting is a 2D transform group: `:1137-1139` registers group 17 with affine `[0.5 0.0 0.0 0.5 40.0 20.0]` under parent 0, and `:188` puts the boundary draw item in `:group 17`. Region rect is fixed `{:x 24 :y 20 :w 80 :h 88}` (`:136`, `:146-147`).
- **Region on a plane: yes, one — ink only.** `:156-192` `region3d-boundary-fixture`. Docstring `:157-162`: *"No arguments → region/draw item with a resolved transformed ink placement in child group 17. … Intended for the implemented ink-on-plane boundary; does not exercise text placement rendering."* The ink object sits at translation `[-2.7 -0.95 0.4]`, scale `0.018`, quaternion `[0.0 -0.21644 0.0 0.976296]` (`:166-168`); its component is a five-sample pressure stroke built by `harness/path.cljs:74-92` `path-ink-component`.
- **Does the harness click a point and compare to an expected hit? Yes — CPU pick vs. CPU expectation, plus a coarse pixel sanity check.** `:588-662` `region3d-lit-oracle`: `:600-601` picks region-point `[40.0 44.0]` (the rect center) with a camera built at `[80.0 88.0]`, and `:658` asserts `(= :near (:object-id hit))` and `:659` `|t − 6.9| ≤ 1e-6` — a hard-coded expected object and expected ray parameter. `:617-621` picks `[1.0 1.0]` and expects `:route :region-background` (`:643-645`), and checks the GPU pixel at `(25,21)` has alpha 0 (`:645`). `:622-629` projects world `[1 1 1]`, picks at that screen point, expects `:route :object`, `:object-id :near`, `:boundary? true` (`:646-648`), and the GPU pixel there to have alpha > 0 (`:649`). `:1017-1030` (in `region3d-lower-resolution!`) re-picks at `[320.0 352.0]` against a `[640.0 704.0]` camera expecting `:near`, and `[5.0 5.0]` expecting `:region-background` (`:1070-1072`). **Every one of these is a mesh pick.** No harness call asks whether a point hit the placed ink.
- **Evidence collected.** Readback: `:397` `w4-read-texture!` on a 128×128 `rgba8unorm` texture (`harness/shared.cljs:19-20`, `region.cljs:369-373`). Pixel checks: `pixel-rgba` at `(64,64)` compared to the oracle shade with `:epsilon-bytes 2` (`:609-612, 652-653, 660`); glass sample strictly between 0 and 255 (`:661`); outside-alpha zero and boundary-alpha positive (`:645, 649`); wear marker at `(100,24)` required to be green-and-blue-dominant (`:1016, 1080-1081`); rejection center pixel nonzero (`:893`). Hashes: `sha256-bytes` on the raw readback, twice per case, `:byte-identical?` (`:465-473`); floor-identical comparison across shadow/no-shadow rejection legs (`:1069`). `byte-delta > 2` to prove content actually changed (`:1040`). **Placed-ink evidence is two integers, not pixels.** `:1214-1220` `:boundary-evidence` = `{:resolved <count of :status :resolved> :ink-vertices <recomputed count>}`, and `:1258-1259` `boundary-pass?` = `(and (= 1 resolved) (pos? ink-vertices))`. `:ink-vertices` comes from `:208-223` `placement-ink-vertices`, which re-derives the mesh on the CPU with a fresh empty cache; its own docstring `:211-212`: *"Proves derivable geometry size, not the exact vertices uploaded by the placement renderer."* No pixel anywhere is sampled on the ink.
- **Page zoom varied across a region: yes, and nothing about the ink is checked at any of them.** `region3d-r3!` `:513-525` drives zoom 1.0 ×4 then 4.0, checking rebuild/upload/refit/encode counters (`:534-548`). `region3d-lower-resolution!` `:949-979` drives 2.0 → 8.0 → 8.0 (mutated) → held → 10.0 → 8.0, checking lease rung divisor, lease size `[512 512]`, activity counters, recovery to `[768 768]` rung 1 (`:1031-1087`). `region3d-rejection-leg!` `:885` captures at zoom 8.0 under a byte cap. The boundary (ink) fixture is captured **only** at zoom 1.0 (`:1194-1196` `specs`, consumed at `:1205` `region3d-capture-pair!`, which uses no zoom override; `:1208` records `:zoom 1.0`). `harness/shared.cljs:25-32` `zoom-cases` (0.01 … 1000) is a path/text sweep; `region.cljs` never refers to it.

### 6. HARNESS: THE FRAME CALLER

**There is no function that composes one frame across text, image, path and region.** `harness/core.cljs:87-92` runs the four drivers as independent promises joined by `js/Promise.all` and assembled into one result map at `:95-117`. Each driver owns its own textures, encoders and readbacks. `run-harness!`'s docstring says so at `:35-36`: *"joins independent drivers with Promise.all … no common final aggregate."*

The nearest thing to a cross-kind frame is inside the region driver, and it mixes **2D path and region only**:

- `region.cljs:336-354` `region3d-renderers` — harness + `sides` → an **ordered vector of paint closures**, not a sort. `:sandwich` → `[(surround 0) region (surround 1)]`; `:region` → `[region]`; `:empty` → `[(surround 0)]`. Order is positional and hard-coded; **there is no sort key**. Docstring `:339-340`: *"Selects below-path, region composite and above-path order. Intended for explicit overlap evidence."*
- The 2D items are two `path-draw-item`s in group 0: `:region3d/below`, a 8→120 square (`:1151-1157`), and `:region3d/above`, a thin bar at y 58-70 with alpha 0.88 (`:1158-1164`). Both prepared once at `:1165-1166` at zoom 1.0.
- `region.cljs:356-400` `region3d-direct-frame!` — the actual frame: `:367-373` create a 128×128 `rgba8unorm` target texture; `:374-376` `active-region-leases!` reconcile; `:379-385` for each desired row, for role `[:shadow :interior]` (or `[:interior]` when no shadow), call `region3d-renderer/encode-region-pass!` — separate render passes, encoded before the page pass opens; `:387-390` acquire an `rgba16float` scene target and `begin-target-pass!` with `"clear"`; `:391` `(doseq [paint! renderers] (paint! pass))` — the vector's order *is* the draw order; `:393-396` `draw-present!` into the readback texture, submit, release; `:397-400` `w4-read-texture!` → `{:bytes :passes}`.
- **Page camera write.** `region.cljs:1141-1142`: `device/update-camera device camera (js/Float32Array. 6) 0.0 0.0 1.0 canvas-size canvas-size` — pan `(0,0)`, **zoom 1.0**, viewport 128×128. Written **once, at harness construction**, never again. `device.cljs:149-162` packs `[pan-x pan-y zoom 0.0 w h]`.
- **Groups write.** `region.cljs:1143-1144` `device/write-groups! device groups-buffer world-transforms`, from the registry built at `:1137-1140`.
- **Where the region reads the page camera.** Only in the composite quad: `renderer.cljs:1366-1377` `composite-bind-group` binds `(:camera-buffer system)` at binding 2 and `(:groups-buffer system)` at binding 3; the shader consumes them at `renderer.cljs:250-251, 259-263`. The **interior** pass never sees them — its uniform is the region's own `view_proj` (`on_plane_renderer.cljs:27-31`, `renderer.cljs:1314`).
- Consequence, from the code: the `{:zoom 2.0 … 10.0}` values passed at `region.cljs:929, 950, 954, 967, 972` reach `prepare-region3d-frame!`'s `:zoom` (`renderer.cljs:1033`) and change lease size and encode-scale — but the *page* camera zoom stays 1.0 for the whole run, so the composited quad's page footprint never changes.

### 7. REGION ORACLE

`harness/region_oracle.cljc` — under `harness/`, not under `region3d/`, though `region3d/README.md` does not mention it and `harness/README.md:26` does.

Namespace docstring `:4-7`: *"Input: maintained scenes, rays, material/light parameters and sample geometry. Output: equivalence predicates, shading values and occlusion decisions. No retained state. This reference shares the production scene/vector/color helpers, while evaluating lighting on the CPU."*

**It is not a rasteriser.** It shades single points that the caller has already picked. Public surface is two functions:

- `:28-45` `scene-equivalent?` — maintained scene map → boolean. Re-derives with `scene/derive-scene` and compares `:world-transforms`, `:instances`, `:triangles-by-object`, the BVH root `:bounds`, and the flattened sorted leaf triangles (`:13-26` `bvh-triangle-stats`). Docstring `:32-33`: *"Does not verify every internal BVH bound or split."*
- `:176-215` `shade-reference` — `{:component :normal :point :eye :lights :ambient :bvh :object-id}` (`:183-184`, `lights` defaulting to `[]`) → a 4-vector: tone-mapped linear RGB premultiplied by alpha, then alpha (`:215`).

**Lighting model, exactly:** metallic-roughness PBR. `:70-103` `pbr-brdf` — GGX normal distribution (`:89`), height-correlated Smith visibility (`:90-94`), Schlick Fresnel with `f0 = mix(0.04, base, metallic)` (`:59-68, :95-96`), Lambert diffuse scaled by `(1-metallic)(1-F)` (`:98-100`). `:105-134` `punctual-radiance` — directional = 1.0; point/spot = inverse-square × `(clamp(1-(d/range)^4))^2` window (`:116-123`); spot cone linear ramp between `cos(inner)` and `cos(outer)` (`:124-133`). `:136-159` `khronos-neutral-tone-map`, matching `scene.cljc:21` `tone-map-algorithm-version :khronos-pbr-neutral-v1`; its docstring `:139-140` says *"Intended for code parity; the name alone is not external conformance evidence."* Ambient is `base ⊙ (ambient-color × intensity)` (`:209-211`), emissive added flat (`:212`).

**Shadows: yes, one hard ray.** `:161-174` `shadowed?` — offsets the origin along the normal by `1e-4`, queries the BVH once, and reports occluded if the nearest hit is a different object within the light distance. Docstring `:165-167`: *"Ignoring a nearest same-object hit does not search onward for a different occluder; one ray differs from GPU filtered shadows."* Gated on `(:cast-shadow light)` at `:196`.

**Transparency: alpha is carried, not composited.** `:185` unpacks alpha from the base color; `:215` premultiplies the tone-mapped RGB by it. There is no over-blend against anything behind. The harness handles the glass case by only asserting the sampled pixel is strictly between 0 and 255 (`region.cljs:661`).

**Placed content: not covered.** `shade-reference` takes `:component` — the *mesh* component (base-color/metallic/roughness/emissive). No branch reads `:text`, `:ink`, or a placement. The ink object contributes no triangles to the BVH it queries (`scene.cljc:842`).

**What the tests compare it to.** GPU readback, at one pixel: `region.cljs:603-612` builds `expected` by `linear->srgb-byte` on the first three channels and `round(255·a)` on the fourth, reads `actual` = `pixel-rgba bytes 64 64`, and `:660` requires `max |expected−actual| ≤ 2` (`:epsilon-bytes 2` at `:653`). Light set: `region.cljs:571-586` `region3d-lights` passes **all** scene lights; docstring `:574-575`: *"Does not apply the GPU's sorted eight-light cap."* Pure-CPU: `test/app/client/region3d/scene_test.clj:87`. `scene-equivalent?` is **not called from `src/`**.

### 8. TEXT PLACEMENT

`on_plane.cljc:40-64` `layout-placed-text` — `[placement font-assets]` → the return of `text-layout/layout`, a flat text layout at local origin `[0.0 0.0]` (`:57`). Inputs: `[:style :font-size]` defaulting to `14.0` (`:46`), `[:layout :constraints]` (`:47`), `[:style :max-inline-size]` (`:48-49`), line-height or font-size × 1.2 (`:50-51`), `(:text placement)`, wrap default `:none`, tab-stops, clip, `(:address placement)` as `:source-id`, `(:content-revision placement)` as `:source-revision`, and `:zoom placement-zoom` (`:52-64`). Note the schema mismatch: the validated object body puts color and inline-size under `[:text :params]` (`component.cljc:464-474`), while `layout-placed-text` reads `[:style :max-inline-size]` and `[:layout :constraints]`. Nothing in `src/` bridges the two.

Where it stops: it returns a text layout, never vertices, a pack, or a color. **Callers: none in `src/`** (`test/app/client/region3d/on_plane_test.clj:36` and the static fence `test/render_engine/verify_text_layout_fence.mjs:8`). `pack-one` (`on_plane_renderer.cljs:211-235`) gives a resolved `:text` placement `:unsupported-kind` (`:231-235`); `build-uploads` (`:271-308`) emits vertices only for `(and (= :resolved status) (= :ink kind))` (`:294`). No harness fixture supplies a text placement. `scene.cljc:804-823` `derive-instance-row` builds `:placement (:text object)` for `:text` kind and marks it `:transparent? true`; nothing consumes that `:placement`.

### 9. DOCSTRING CLAIMS — verbatim

**`region3d/on_plane.cljc`**: `:6-9` *"It calls the existing text/path computations; it does not resolve external content addresses or allocate GPU resources. placement-zoom is fixed at 1."* · `:43-44` *"Serves as reusable calculation; this does not prove a text GPU placement path exists."* · `:69` *"Explicit coordinate convention."* · `:76-77` *"Inverts matrix, intersects local z=0, converts t back. Singular inverse throws and zero-direction ray is not independently checked."* · `:104-105` *"Reuses path tessellation at zoom 1 and adapts paint. Shares geometry computation."* · `:135-136` *"Intersects center-to-point direction with rectangle edge. Intended for directional edge anchoring, not independent-axis clamping."* · `:155-156` *"Serves as geometry utility; its connector wording does not establish a current connector system."*

**`region3d/on_plane_renderer.cljs`**: `:6-8` *"It owns one flat vertex buffer per region; each vertex repeats local XY, a model matrix and linear premultiplied color (88 bytes)."* · `:10-11` *"Resolved ink is the supported GPU placement kind. Resolved text and other kinds receive :unsupported-kind."* · `:24-25` *"Placement passes test depth without writing it and use a small surface bias."* · `:77-78` *"Fixed RGBA16F, 4× MSAA, depth-tested transparent geometry. Intended for region interior target."* · `:240` *"Hardware-cap derivation. It is a buffer limit, not frame-time admission."* · `:276-277` *"Repeats matrix/color per vertex and records origin distance. Maintained parameter is unused; large repeated matrices are a bandwidth/storage choice."* · `:314-315` *"Sorts by object-origin Euclidean distance. Approximate transparency order for intersecting/extended surfaces."* · `:333-335` *"Current limitation: a camera-only change does not change pack key and retains old draw order. A camera-crossing transparency fixture would determine the visual consequence."*

**`harness/region_oracle.cljc`**: `:6-7` *"No retained state. This reference shares the production scene/vector/color helpers, while evaluating lighting on the CPU."* · `:139-140` *"Mirrors the renderer's neutral-tone-map calculation. Intended for code parity; the name alone is not external conformance evidence."* · `:164-167` *"Offsets the origin and tests a nearest hit within light distance, excluding the same object. Ignoring a nearest same-object hit does not search onward for a different occluder; one ray differs from GPU filtered shadows."* · `:181-182` *"Intended for the selected oracle sample; does not reproduce all GPU light/shadow resource limits."*

**`harness/region.cljs`**: `:161-162` *"Intended for the implemented ink-on-plane boundary; does not exercise text placement rendering."* · `:211-212` *"Proves derivable geometry size, not the exact vertices uploaded by the placement renderer."* · `:339-340` *"Selects below-path, region composite and above-path order. Intended for explicit overlap evidence."* · `:574-575` *"Collects all scene lights and transforms position/direction. Does not apply the GPU's sorted eight-light cap."* · `:592-594` *"CPU ray pick/reference shade at the center, plus glass/outside/boundary samples and two-byte color tolerance. Intended for a few explicit points; not image-wide equivalence."*

**`region3d/renderer.cljs`**: `:1029-1031` *"Key omits raw session contents, placement contents, max lease size and group index; callers must carry changes via revisions or a different keyed stamp. Font-assets is destructured but unused."* · `:1298-1300` *"Opaque meshes, transparent meshes, then placed ink, then resolve. Mesh and ink transparency are separate ordered groups rather than one combined depth ordering."*

**`region3d/scene.cljc`**: `:969-970` *"Camera ray plus BVH. Intended for mesh picking; text/ink placements are not in this BVH."* · `:808-809` *"Projects kind-specific data. Text/ink always enter transparent classification."*

**`region3d/frame.cljc`**: `:13-14` *"Outer invalidation contract. Session contents and resolved placement contents must change the supplied revision to get past this key."*

**Markdown claims vs. code:** `region3d/README.md:5` says the region takes *"optional session overlays and resolved placements."* No code in `src/` produces a resolved placement. `region3d/README.md:29` — *"The placement GPU lane currently supports ink; accepted placement data and available CPU calculations do not by themselves imply GPU support for every kind."* Matches `on_plane_renderer.cljs:226-235`. `harness/README.md:31` — *"A named check, an available fixture and a passing run are separate facts."*

TODO/FIXME: not found in these files.

---

## Part 4 — what session 1 read itself

- `src/app/client/README.md` (whole): four families, one engine; "placed text calculations" and "placed ink geometry" arrows from text and path into region3d; "Component values describe input. Derived representations can be reconstructed from that input and may be retained for reuse. GPU ownership adds allocation, invalidation and teardown responsibilities."
- `src/app/client/region3d/README.md` (whole): "Input: region objects, transforms, meshes, shading/lights, camera, background and 2D extent; optional session overlays and resolved placements. Output: CPU spatial answers and an offscreen scene image composited into that region." And: "The placement GPU lane currently supports ink; accepted placement data and available CPU calculations do not by themselves imply GPU support for every kind."
- `docs/below-the-waist/path-kind/path-kind.md` at working tree (session 9's regeneration; a sibling session later folded attack 1 and committed `d202c90`, `4b8e986` while this session ran), `HANDOFF-8.md`, `attack-1.md`, `fact-base-9.md`, `bench-4/HANDOVER.md` and `bench-9/HANDOVER.md` headers, `3d-ceilings-starter.md`, `3d/STARTER-0.md`.
- Working tree at write time: `docs/below-the-waist/3d/3d-kind-working-model.md` is an untracked file written by the other lane during this session; not read beyond its first paragraph, not ground, left as found.

## Part 5 — derivations made on this page (DERIVED, from the facts above)

- The seam today is a picture, not a chain: the interior's projection never receives the page camera (Part 2 §4), so page zoom is a resample between 1.12-power buckets and a re-render at 256-pixel lease quanta up to 4096; beyond that the same page rect samples a smaller texture. There is no execution in which a page zoom re-projects a region's content at true pixel scale.
- The pick answer today ends at a triangle in a mesh; the chain the hardest case needs (region → object → face → chart point → path classify) has every link present as an uncalled function or a harness-only function except the face and the chart: `ray->placement-plane` (no caller), `path/classify` (harness/tests only), `pick-region` (harness only), and no face identity anywhere.
- Placed ink is a decal drawn as a separate transparent mesh with a negative depth bias, not part of the face's material; it therefore cannot be lit or shadowed with its face, and its tessellation is fixed at one zoom band regardless of the object's scale, the region's encode scale or the page zoom.
- One revision gates the whole region at the frame key, and inside the scene the only fine grain is transform-only refit; every other edit re-derives the whole scene.
- The shadow map is rendered from the light into a depth surface and then read by shading: the tree already contains one instance of "render the space from another camera into a surface that another stage reads", the same primitive that a portal or a region composite is.
- Nothing rebases coordinates around the camera in either dimension; f64 CPU values narrow to f32 at upload and are used as absolute world coordinates in both the 2D composite shader and the 3D interior.
