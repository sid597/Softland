# Waist fact base — session ask-max-2, 2026-09-03 (receipts for ceiling-and-waist.md §7)
Ground truth: src/app/client/ at HEAD 61b607e. Every line below is FACT (read this session, file:line) or a gatherer FACT (two read-only code-hunters, returns pasted verbatim in §3). Verdicts are in §4 and are mine.

## §1 Facts read directly (path/*, on_plane, harness, engine windows)

path/component.cljc
- 11-15 schema-version 2; legal-kinds #{:ink :shape}; legal-contour-roles #{:outer :hole}; legal-cap-join #{:round}
- 23-32 paint spec: keys #{:color :opacity :color-space :alpha-association}; :color-space only :srgb; :alpha-association only :straight
- 34-43 stroke-point: REQUIRED #{:stroke-point/id :position :width}, optional #{:pressure :gesture-time}
- 45-50 ink-geometry: stroke-points [:vector-of stroke-point {:min 2 :unique-by :stroke-point/id}], :cap and :join required, one legal value
- 52-57 contour: REQUIRED #{:contour/id :role :points}, points min 3
- 59-69 shape-geometry: contours min 1 unique-by :contour/id; form-validator holes-have-an-outer?
- 77-86 schema: REQUIRED #{:path/material-id :path/revision :path/kind :path/geometry :path/paint}
- 88-91 validate-component! = schema/check; 93-102 canonical-component (kind-neutral sort); 104-107 content hash = pr-str of canonical minus material-id/revision (so point/contour ids ARE hashed)
- 141-155 contour-classify ray cast; 177-210 classify tri-state with slop; 212 hit?; 215-225 boundary-distance; 227-230 paint-color → [r g b a*opacity]
- Readers of :stroke-point/id / :contour/id outside this file: harness/path.cljs 55,70,75,87 and tests only (grep, src+test). Nothing in tessellation or renderer reads them.

path/frame.cljc 7-14: frame-key [draw-items lod] → [[[material-id revision container] ...] lod]. Draw-item shape is undeclared; built by harness/path.cljs:98-99 as {:id :path/material component :container group}.

path/tessellation.cljc
- 12-21 three LOD buckets (0.01-0.1 fan 4, 0.1-8 fan 8, 8-1000 fan 16); 23-30 zoom-lod throws outside [0.01 1000]
- 35-42 component-cache-key = [canonical kind+geometry, algorithm, lod-id] (paint excluded)
- 50-60 shape-normalization: origin + scale from bbox (numeric robustness)
- 101-113 fan-triangles; 125-191 stroke-triangles-normalized: per-segment two triangles (139-146), start/end cap fans (148-160), join fans on the outer side (162-190). Caps/joins are separate fans that OVERLAP the segment quads.
- 270 bridge-holes (explicit CW holes into one CCW walk); 335 ear-clip (simple polygon)
- 410-424 tessellate → {:path.mesh/version 2 :algorithm-version :lod :cache-key :coverage :aliased-v1 :vertices [[x y]...] :triangle-count :vertex-count}; vertices are nested persistent vectors in component-local f64
- 426-440 derive-mesh-set [cache components zoom] → {:cache :meshes :derived-keys}, caller-owned value cache

path/renderer.cljs
- 17-18 vertex-words 7 / stride 28: pos vec2, color vec4, group u32
- 21-30 the WGSL Camera and GroupTransform structs are declared IN THIS FILE (also in text/renderer.cljs:18-20 and image/renderer.cljs:19-21); only scene_color is shared from engine/device.cljs:16-37
- 42-47 world = group affine applied, then * zoom + pan in f32
- 131-152 pack-vertices: paint-color of the draw-item's component written into EVERY vertex; group index via transform/buffer-index (:container)
- 168-211 prepare-path-frame!: one whole-frame key (173-174); on any change re-derives the set and REPACKS ALL vertices (181-202) into one buffer
- 213-219 draw-path-range! one .draw, non-indexed, non-instanced

region3d/on_plane.cljc
- 17 placement-zoom 1.0 (constant); 20-33 adapt-legacy-color: path's flat [r g b a] → {:rgba :color-space :srgb :alpha-association :straight}
- 35-58 layout-placed-text builds a text-layout/layout input from placement [:style :font-size] [:layout :constraints] [:style :max-inline-size], :source-id (:address placement), :source-revision (:content-revision placement), :zoom 1.0
- 88-100 pack-placed-ink: derive-mesh-set at zoom 1.0; pack {:object-id :address :cache-key :vertices :color}
region3d/on_plane_renderer.cljs:14 flat-vertex-stride 88 (a second vertex format for the same ink mesh; path's is 28)

engine/schema.cljc 136 check [spec form]; spec vocabulary :keys :optional :validators :form-validators, [:vector-of ..] [:map-of ..], sets, predicates
engine/transform.cljc 40-55 group spec (all optional; :affine xor legacy x/y/scale/rotation; :camera #{:world :screen})
engine/color.cljc ~35-40 tagged color spec {:rgba valid-rgba? :color-space #{:srgb} :alpha-association #{:straight}}; 42-52 legacy-direct-color (:enabled? false, blend src-alpha, clear opaque); 54- linear-premultiplied-color (:enabled? true)
text/layout.cljc 86-89 legal-zoom?; 467-469 zoom defaulted 1; 575 zoom enters the semantic (identity) hash; 815-819 zoom range throw; 832 :lod in the result
text/renderer.cljs 293 destroy-text-system!; 368 update-font-assets; 389 share-font-resources ("secondary text system at a primary's font resources"); 403 recreate-text-system; 422 clone-text-system (shares pipeline/bind-group/fonts, new instance buffer)

harness: core.cljs run-harness! is the only entry; loads dejavu-sans-mono + ubuntu-sans-variable. Tests: test/app/client/path/{component,frame,tessellation}_test.clj + fixtures.cljc. Harness/text.cljs:44-47 issues its own .draw (bypasses draw-text-system!).

## §2 Facts from the prompt's map that I re-verified: all matched, with the additions above (per-point ids; fan overlap; struct duplication; constant placement zoom; inline layout in the renderer; region vocabulary in engine services).

## §3 Gatherer returns (verbatim)

### 3a engine / image / region3d
INDEX (file · entry fn · input · output · validated where)
engine/color.cljc · scene-color :76 · one boolean · one of two literal color-resource maps · caller-trusted
engine/limits.cljc · adapter-limits :8, texture-bytes :32 · adapter/device; format w h mips samples · limits map; byte count · throws on unknown format :37
engine/rungs.cljc · grant :55 · request map w/ injected fns · admitted?/granted-key/rung-divisor map · caller-trusted
engine/schema.cljc · check :136 · spec + EDN form · unchanged form or throw · is the validator
engine/transform.cljc · add-group :94, world-transforms :194, screen-bounds :270 · registry, group-id, spec · registry; group-id→{affine flags buffer-index} · schema/check group at :108 :134 :186
engine/buffer_pool.cljs · create-pool :15, batch-update-pool! :47 · device+capacity+opts; item list · pool atom; write count · opts check :18
engine/compositor.cljs · create-compositor! :313, acquire-region-lease! :366, begin-target-pass! :526, draw-present! :534 · device+format; region id w h shadow? · compositor map; lease map; GPURenderPass; nil · caller-trusted
engine/device.cljs · write-groups! :73, update-camera :127 · device buffer world-transforms / pan zoom w h · stats map; nil · throws :80 :83 :88 :105
engine/leases.cljs · reconcile-desired! :51, attach-compositor! :102 · owner + desired rows; owner + compositor · desired map; owner mutated · caller-trusted
image/component.cljc · validate-component! :104 · image component map · same map · schema/check :107
image/frame.cljc · frame-key :7 · draw-items + residency rev · vector key · caller-trusted
image/renderer.cljs · prepare-image-frame! :646 · system draw-items world-transforms · {:changed? :writes :instances} · caller-trusted
region3d/component.cljc · validate-region! :594 · region map · canonical region · schema/check :597
region3d/frame.cljc · region-key :8 / frame-key :13 · draw-item zoom dpr session-rev · key vector · caller-trusted
region3d/scene.cljc · derive-scene :622, camera-matrices :557 · canonical region; view+viewport · derived-scene; matrices · canonical-view :558
region3d/on_plane.cljc · layout-placed-text :35, pack-placed-ink :88, project-region-anchor :125 · placement+font-assets / cache · layout; {:cache :derived-keys :pack} · caller-trusted
region3d/renderer.cljs · prepare-region3d-frame! :785, encode-region-pass! :1052, composite-region! :1106 · system {:regions} session opts · counts; pass; draws · caller-trusted
region3d/on_plane_renderer.cljs · prepare-placements! :238, draw-placements! :299 · system region-gpu placements maintained camera cache · path-cache; draws · limits enforced :176

transform.cljc — group spec :40-55 optional-only keys; world-transforms :194-212 returns per group exactly {:affine :flags :buffer-index}; :parent nesting resolved in compose-one :172-192 (camera inherits; cycle throws :175; unknown parent throws :110). screen-bounds :270 takes [world-transform bounds camera] → {:x :y :w :h}; anchored-screen-rect :288. Callers: world-transforms 5 sites (4 harness + test); screen-bounds NONE in src; anchored-screen-rect tests only; buffer-index :221 used by region3d/renderer :796, image/renderer :636, text/glyph_pack :140, path/renderer :140. fallback-buffer-indexes :164-170 "Old hand-built registries in tests/doc fixtures".
device.cljs — camera 24 bytes :121, update-camera :127 writes [pan-x pan-y zoom 0 w h]; write-groups! :73 entry 32 bytes [a b c d tx ty flags pad], throws nil index :80, dup :83, > max-transform-nodes 16384 :59/:88, affine≠6 :105, holes filled identity :98-103. scene-color-wgsl :16-37 with compile-time const :13-14; configure-scene-color-shader :38 string-replaces to true when (:enabled? color). write-groups! callers: 9, all harness. scene-color-wgsl callers: harness/shared :176, image/renderer :87, path/renderer :81, text/renderer :88.
compositor.cljs — lease :454-458 {:lease/version 1 :key [region-id qw qh] :region-id :size :target :resolve :shadow :bytes :rejected? false}; quantization 256 max 4096 :19-20; begin-target-pass! :526 [encoder target load-op]; draw-present! :534 [compositor encoder scene output-view output-format] full-screen triangle, scissor to scene size. Callers: begin/present harness only (:308 :311); create-compositor! region3d/renderer :1141 :1152 + harness; acquire-region-lease! harness/region :763 only.
leases.cljs — reconcile-desired! :51 [owner rows] rows keyed :region/id, retires buffer indexes of closed ids :63-71; attach-compositor! :102 bumps :device-epoch. Aliased region-bindings in region3d/renderer :18 and harness/region :9 (UNCERTAINTY: not every region-bindings/ call site enumerated).
rungs.cljc — admission-divisors [1 2 4 8] :10; grant :55 destructures {region-id :region/id [w h] :desired-size :keys [shadow? held-lease reserved-bytes budget-cap-bytes quantize lease-bytes]} :17-21. Callers: tests only.
limits.cljc — texture-bytes :32 fail-closed :36-38, format table :22-30. adapter-limits callers: on_plane_renderer :171, harness. texture-bytes: tests only.
color.cljc — scene-color :76 boolean → linear-premultiplied (:54, blend one/one-minus-src-alpha, clear 0 0 0 0) or legacy-direct (:42, src-alpha, clear 0 0 0 1); scene-color-boundary :67 :default-off? true. Callers: harness only (9).
buffer_pool.cljs — create-pool :15 requires :floats-per-item + :pack-fn :18-20; batch-update-pool! :47 diffs against :prev-items INDEX-WISE. Callers: image/renderer :382 :659 only.
schema.cljc — 13 call sites outside engine: path/component :73 :74 :91; image/component :43 :107; region3d/component :261 :306 :308 (+5).
image/component.cljc — ns "a verified source (digest, color tag, size, alpha association), its place in the atlas, and the 13 floats one image quad packs to". Source spec :28-37 (:image/digest sha256, :image/bytes-route). Component :89-102 REQUIRED #{:image/component-id :image/revision :image/source-digest :image/color-tag :image/intrinsic-size :image/provenance :image/rect :image/paint} optional :image/crop. Callers outside kind: harness/image only.
region3d/component.cljc — top keys :465-467 #{:region/id :region/revision :region3d/version :extent :scene :view :background :ambient :region/rect}; :scene [:map-of some? object]; form validators ids-match-keys? parents-exist? acyclic? :478-487; validate-region! :594-597.
region3d/scene.cljc — camera-matrices [view viewport] :557 (:perspective/:ortho); derive-scene :622 → :derive/version :region :world-transforms :instances-by-object :instances :triangles-by-object :bvh :stats :641-651.
region3d/on_plane.cljc — text-layout/layout :47-58 with {:text :provider (:layout-provider font-assets) :font-size :line-height :origin [0 0] :inline-size :wrap-policy :tab-stops :clip :source-id (:address placement) :source-revision (:content-revision placement) :zoom placement-zoom}; derive-mesh-set :92-93 at placement-zoom 1.0 (:17).
region3d/renderer.cljs — prepare-region3d-frame! :785 {:keys [regions]} + opts {zoom dpr world-transforms font-assets session-layout-snapshot path-system max-lease-size}; encode-region-pass! :1052 [system encoder region-id role lease] role :shadow/:interior; composite-region! :1106.
:container (22 hits): written by harness fixtures + text/layout.cljc:1025 (copied from template); read by image/frame :12, path/frame :12, region3d/frame :10, image/renderer :636, region3d/renderer :796, text/glyph_pack :140, path/renderer :140, text/renderer :535 :655, on_plane :131, harness/region :565 — every read except frame-keys goes straight to transform/buffer-index.

### 3b text
INDEX
text/layout.cljc · layout (859) · one options map, :provider + text/metrics keys · immutable layout-result map · provider presence only (861-865), :or defaults (467-468)
text/layout_planes.cljc · plane-builder/put-*/finish-planes! (535-601) + accessors · counts then line-specs · typed planes map · caller-trusted
text/shaped_line.cljc · make-line (62), from-maps (195), ->maps (163) · glyph-count run-count faces · scalars + 20 typed columns · shaped-line? (57-60) only
text/shaper.cljs · load-provider! (370) / create-provider (331) · font-source maps {:id :revision :url :variations} + opts · provider map whose :shape-line returns a shaped line · :or defaults; no source validation
text/fonts.cljs · load-font-assets (80) · one font-config from manifest · Promise {:id :name :backend :layout-provider :slug} · key presence only
text/glyph_pack.cljs · slug-table (25), count-instances (125), pack-lines! (188) · positioned draw-items + Slug glyph list + world transforms · 25 words per instance · caller-trusted
text/renderer.cljs · init-text-system (363), update-text-data (608), draw-text-system! (688) · device texts font-assets font-size · text-system map :instance-buffer :num-instances :line-offsets · :or only

layout.cljc — ns "wraps and measures text into lines of positioned glyphs, with caret stops, selection geometry, clipping, and hit testing, as one immutable result". layout 859-865 throws :text/layout-provider-required. shaped-layout destructure 464-468: [text source-lines provider font-size line-height origin baseline-offset inline-size wrap-policy wrap-col headers clip line-map source-id source-revision features variations language direction tab-stops zoom] :or {text "" font-size 14 line-height 14 origin [0 0] baseline-offset 0 wrap-policy :none zoom 1}. No schema/spec var in the file. Output 818-856 keys: :text-layout/version :layout/id :layout/planes :source :font :shaping :space :lod :constraints :metrics :lines :line-index :reference-advance :inline-size :clip-plan :stats.
line-paint-draw-items 1013-1030 [layout-result template] → per line (assoc template :text :from 0 :to :x :y :container :layout-result :layout-line-id :layout-anchor :paint-source-range).
:source-lines → 515 line records (126 fallback split on \n), 561 semantic-input, 853 stats. :source-id/:source-revision → 563 semantic-input, 608 :source map → planes/finish-planes! 807 → result :source 821. :line-map → 574 semantic-input, 838 :constraints, read once 1284 in shaped-hit-test-result.
Callers: layout — harness/text :328 :367 :386 :435; text/renderer.cljs:507; tests. line-paint-draw-items — harness/text :343 :443; tests. layout-cache-acquire, layout-key — tests only. caret/selection/clip/hit-test-result — harness/text :376-380; tests. pack-glyphs! 448-453 → glyph_pack :116. glyph-indexes-in-source-range 437, line-by-id 393, line-source-bounds 71, tagged-index 45 — renderer :512 :517 :519 :528-529.
layout_planes.cljc — ns "Columnar storage for layout results... Internal to text layout... Raw arrays never leave this namespace." compact-result 290 has no caller; plane-builder path is what layout uses (606, 807). pack-glyphs! 625-632 callback (f index glyph-id shaped? tab? font-id x y cluster-start cluster-end). Callers outside: layout.cljc; tests (layout_planes_test, on_plane_test line-glyphs).
shaped_line.cljc — make-line 62-92 → {:glyph-count :run-count :advance :base-direction :faces} + 16 per-glyph columns (:glyph-id :cluster-start :cluster-end :advance-x :advance-y :offset-x :offset-y :glyph-x :glyph-y :ink-x :ink-y :ink-w :ink-h :run-index) + 4 per-run (:run-source-start :run-source-end :run-flags :run-face). from-maps/->maps oracle converters; every provider result passes from-maps at layout 490/498. shaper :231 make-line.
shaper.cljs — create-provider 331-367 [hb font-sources {:keys [features language tab-columns] :or {features ["kern" "liga" "clig" "calt"] language "und" tab-columns 4}}] → {:face-id :face-revision :shaper-id :shaper-version :features :variations :axes :fallback-chain :metrics :upem :shape-line (fn [text opts])}; shape-line-flat 193-200 fills columns run by run, bidi visual order 158, one scratch per provider. Faces rescaled to primary upem 341-345. No validation on font-sources. Callers: load-provider! ← fonts.cljs:95; probe.
fonts.cljs — load-font-assets 80-128 → {:id :name :backend :slug :layout-provider :slug {:meta :curve-bytes :band-bytes}}; slug-ready? 114 partial → nil, no throw. Callers: harness/core :36 :47 :48 :115 :123; load-default-font-data-async, available-fonts, resolve-default-font-config: no caller outside.
glyph_pack.cljs — slug-table 25-70 WeakMap-cached; pack-draw-item! 132-186 words: 0-3 world x/y/w/h, 4-7 sample bounds, 8-11 inverse-size 2x2 [inv 0 0 -inv], 12-15 banding, 16-19 glyph loc/band meta (u32), 20-23 rgba from (:style draw-item), 24 group from transform/buffer-index (:container). instance-words 25 (:13) vs stride 100 bytes (renderer :229 ";; 24 words + group u32"). pack-lines! 188-197 lines = [{:draw-items :count}]. Draw-item destructure {:keys [style font-size]} 133, {:keys [line indexes dx dy]} 113. Callers: renderer :584 :589 :599 only.
renderer.cljs — ns "Slug. Glyph outlines evaluated per pixel from curve and band textures; no atlas, exact at any zoom". position-text-draw-item 493-540 reads {:text :x :y :size :layout-result :layout-line-id :paint-source-range :layout-anchor :container}; when :layout-result absent calls tl/layout inline 506-512 (counts a fallback); emits {:draw-item {:layout/id :style txt :font-size :container :line :indexes :dx :dy}}. draw-instances! 666-687 / draw-text-system! 688-697 (6 verts × num-instances): NO caller in src or test; harness/text.cljs:44-47 issues its own .draw. update-text-data 608-611 kwargs {line-height-factor line-height snap-step world-transforms}. pack-instances-flat 584 reads font-assets [:slug :meta :glyphs] unchecked. update-font-assets 368, share-font-resources 389, recreate-text-system 403, clone-text-system 422, destroy-text-system! 293: no caller in src or test.

## §4 Verdict tables (mine; anchors above)
(see the session reply; same tables)

### 4a path/*
component.cljc — IN: path map with own header (:path/material-id :path/revision), kind ink|shape, polyline w/ required per-point id, polygons w/ required per-contour id+role, own paint form. OUT: validated/canonical map, content hash, tri-state classify, hit?, boundary-distance, flat rgba. OWNER: this file; harness conforms. VERDICT: wrong form, goes. WHY: ink/shape split forbids fill+stroke on one geometry (12, 45-69); polylines/polygons are not zoom-independent exact geometry; per-point/per-contour ids required (35, 53), read by nothing below, hashed as content (104-107); header + color form are the engine's (color.cljc ~35 already declares tagged color; on_plane adapts 20-33); cap/join required with one legal value (14, 49-50). RE-HOMED: validate-at-ingress; canonical+hash (kind-neutral); tri-state query contract; ray-cast classify as the flattened-stage query.
frame.cljc — IN: undeclared draw-items + lod. OUT: one whole-frame key. OWNER: nobody declares; harness builds (98-99). VERDICT: wrong form, goes. WHY: whole-frame key ⇒ whole-frame repack (renderer 173-202); per-row id+revision diff is the shape (leases.cljs 51-71 has it for regions).
tessellation.cljc — IN: component + zoom bucketed to 3 lods (12-30). OUT: geometry-only local-f64 mesh as nested vectors + counts + coverage + cache key (410-424); caller-owned content cache (426-440). VERDICT: wrong input, right output contract in wrong representation; goes with component. WHY: lod buckets not a screen tolerance; joins/caps are fans overlapping the quads (139-190) ⇒ translucent ink double-blends; ear-clip needs simple polygons (335). RE-HOMED: mesh contract + cache shape as typed arrays; ear-clip terminal step.
renderer.cljs — IN: device+buffers; draw-items+zoom+world-transforms; pass+range. OUT: 7-word vertices with color in every vertex (131-152), one draw, stats. OWNER: vertex format here; engine's Camera/GroupTransform structs re-declared here (21-30). VERDICT: lifecycle right, packing wrong; goes with the rest, lifecycle kept.

### 4b text/*
shaped_line.cljc — RIGHT, stays (shaping return type; glyph-run raw material). shaper.cljs — RIGHT, stays (run shaper service). fonts.cljs — RIGHT, stays (:layout-provider misnamed). layout_planes.cljc — right representation, wrong owner (private to layout; becomes the glyph-run mark payload). layout.cljc — WRONG FORM, goes (string+one face+one size+document keys+zoom in, no schema 464-468; zoom in identity 575/832; draw-item = layout line by reference 1013-1030; owner of the mark). Re-homed as a service: line breaking over shaped lines; caret/selection/hit queries; content-keyed cache. glyph_pack.cljs — technique right, input contract wrong (reads layout planes 113/133; 2x2 inverse-size only 152-183 ⇒ no per-glyph rotation). renderer.cljs — Slug right; inline layout (506-512), layout knobs in update-text-data (608-611), and clone/share/recreate/update-font-assets (368-422, font textures lent between systems instead of engine assets) wrong; destroy + draw-text-system! uncalled and right-form, alive.

### 4c engine/*
schema RIGHT. transform RIGHT; fallback-buffer-indexes (164-170) goes; screen-bounds/anchored-screen-rect uncalled + right-form = alive. device RIGHT with two owner faults (structs in kinds; color const by string replace 13-14/38). color: tagged spec + linear-premultiplied RIGHT; legacy-direct (42) + boolean selector (76) + :default-off? true (67) go. limits RIGHT. rungs mechanism right, input speaks region3d (17-21). compositor mechanism right, region/shadow vocabulary (454-458) wrong owner, no clip/blend/filter. leases right shape (id-keyed reconcile 51-71), region vocabulary. buffer_pool RIGHT (image only; index-wise diff is its limit).

### 4d image / region3d
image: right as texture-rect mark; third header copy (89-102); whole-frame key. region3d: right as a portal (own scene tree/camera/target); fixed placement zoom (on_plane 17); own color adapter (20-33); second vertex format for the same ink (on_plane_renderer 14: 88 vs 28); fourth header.
