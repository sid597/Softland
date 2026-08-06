# W4 FRAME-RUNTIME — one-pass contract (pass-graph · offscreen groups · group opacity · masks · blur · per-node clip · export seeds · animation clock / frame scheduler)

Cut 2026-08-07 by Fable — **cutter model `claude-fable-5`, effort `max`** (the
model-routing law, CLAUDE.md; Sid's two-second header check). One-pass law:
`.claude/skills/work-package/SKILL.md` + decisions.md "How we work". Two
Sid-touches: this cut · the accept. The cut closes through ONE bounded
fresh-eyes falsification round at Sid's hand (the armed A/B is available —
identical brief into a fresh Claude session and/or a Codex session; the chrome
round's datum: arm-2 overlap 5/6, pooling value falling as the author pass
improves — one arm is a lawful choice, Sid's line). Findings pooled,
evidence-cited, no verdict authority, no recut; author repairs in-session.
The four-lens author pass ran BEFORE the round (lens 1/4 Opus sweeps of the
finished draft; lens 2/3 author judgment). Structural precedents AT THE
SOURCE LEVEL: the chrome/path/connector atoms (`86918e9`/`10dd8e5`) and the
SEAM-STEP1 maintained arrangement + batch-oracle twin
(`renderer.cljs:3021-3078`). Do not imitate IMAGE-ATOM/SEAM-STEP1 docs; they
predate the law.

**The single-ownership law (ENGINE.md §0, the W4 row; DISCOVERIES
"compositor ∥ clock rejected as parallel work"):** ONE implementer, ONE lane,
SERIAL internal cuts — never two hands on the frame loop. The serial order is
this contract's build order below; no cut is parallelized against another.

## Scope — what this package is

Package 2's frame-kind package (ENGINE.md §0 W4 row): the W2-B tape's
executor learns **compiled multi-pass plans**, and the graph grows offscreen
groups · masks · group opacity · backdrop/layer blur · per-node scissor clip
(the road that retires the CPU clip degradations) · export seeds; THEN the
animation clock / frame scheduler. W0-C §5 is the settled design paper this
package makes code-real ("W2-B makes it code-real; W4 extends the same seam
with groups/effects/time" — W0-C §5, status line); its §5.9 receipts #4–#7
are reserved for exactly this package.

**Seven serial internal cuts, in build order (ENGINE's row order):**

1. **Pass-graph + plan compile + the intermediate road.** A pure plan
   compiler (`frame-graph`) derives the frame's pass list from (the
   maintained arrangement × declared container effects × enabled
   capabilities): which intermediates exist, which entry ranges render into
   which pass, the copy/producer edges, the one presentation terminal. The
   tape stays FLAT — a group is a contiguous stack-path span
   (`scene_store.cljc:264-273` `:order` carries `:stack-path`); the plan
   slices spans into passes; entries are never reordered (W1 Contract O
   §3.2.1: a frame graph may split entries across passes only if final
   composition is equivalent to forward tape order). The reserved
   pass-classes are already order DATA: `pass-rank {:frame-policy 0 :direct
   1 :intermediate 2 :region 3 :present 4}` (`scene_tape.cljc:584-588`) —
   `:intermediate`/`:region`/`:present` exist declared-but-never-minted;
   this cut mints them. Plan compile happens BEFORE any pass opens (today
   the arrangement update runs inside the open pass,
   `renderer.cljs:3206-3209` — that ordering inverts). Compile is
   change-driven, not per-frame (W0-C §5.5): the plan's STRUCTURE is a
   pure function of (the effect-bearing container TOPOLOGY — ids ·
   nesting · effect values, never concrete entry indices · enabled
   capabilities · viewport shape · color mode)
   and is reused while those inputs are value-equal; the batch recompute
   stays alive as its oracle (decisions.md fenced-incremental-view law; the
   `compile-frame-tape`/twin precedent, `renderer.cljs:3052-3078`). The
   reuse split, exact: the plan's PASS STRUCTURE (intermediates, pass
   kinds, copy edges, terminal) is the pure function reused on that
   4-tuple; the ENTRY-RANGE BINDING of passes to the current arrangement
   is resolved fresh each frame (a cheap ordered select) — a reused
   structure never carries stale ranges. W0-C §5.9 receipt #4 is
   discharged on the legacy road: an effectless plan may compile
   COPY-PRESENT (the dormant Phase-6E road made real — same pipelines,
   swap-format intermediate, `copyTextureToTexture`), and direct-present
   vs copy-present is BYTE-IDENTICAL (S1's verifier row).
   Validation fails closed on W0-C §5.5's ten laws — each gets a rejecting
   fixture in S1. The `:region` pass-kind is validated as a legal plan shape
   and NOT built (the W4-before-regions hard join, ENGINE §0: the island
   probe "becomes a compositor pass under the scheduler" in Package 3 — this
   contract only keeps that door open as data).

2. **Offscreen groups + group opacity.** Effect grammar lands at the
   CONTAINER/arrangement layer (session truth v1, flag lane, no durable
   writes): `scene-rt/set-effects!` beside `set-transform!`
   (`scene_runtime.cljs:143-158` is the registration shape), effects stored
   in the container registry beside transforms (`containers.cljc:65-105`),
   validated fail-closed in `frame-effects`. Effects NEVER enter store
   derivation: `derive-store-frame` stays a function of the store value
   alone (`scene_store.cljc:320` `[store]` — unchanged); the effect-span
   derivation happens at the PLAN layer — `frame-effects` reads (the
   container registry's effect map × the arrangement-order snapshot) and
   hands spans to the plan compiler, the same side-channel road effective
   transforms already ride. A group with `:opacity < 1` (or
   any mask/blur) compiles to an isolated offscreen pass: children render
   into a group target; ONE composite draw applies opacity to the group's
   composited premultiplied RGBA (W1 Contract C §8.2: "Group opacity is
   applied once to the composited group's premultiplied RGBA, not
   independently to every child. This is why group targets are semantic
   resources in W4."). The founding scenario is DISCOVERIES D-03: two
   overlapping 50%-opaque siblings darken at their intersection under
   per-child opacity and must not under group opacity — S2 freezes it.
   Nested groups compose inner-first (plan topological order).

3. **Masks.** v1 = alpha mask: a group declares `:mask <designated child
   slot>`; the mask source subtree renders its coverage·alpha into a mask
   texture (its own pass); the group composite multiplies by it. Mask
   sources are excluded from normal paint (they paint only into the mask
   pass). Vector/luminance mask variants are refusals (routed). Contract G
   §4.3: the mask relationship is a declared `:derived-effect` — the mask's
   blur/coverage never mints a second pick body.

4. **Backdrop blur + layer blur.** Backdrop: the group's composite samples
   a blurred snapshot of scene-color-so-far, **masked by the group's own
   composited alpha** (the frosted panel is glass exactly where the group
   has coverage — Figma's background-blur semantics); the snapshot region
   is the group's span AABB padded by the projected blur radius. The
   aliasing law is absolute (W0-C §5.2): no pass binds the same
   subresource as sampled read AND writable attachment — the backdrop
   input is a DECLARED snapshot copy (or ping-pong) with an explicit
   producer edge and lifetime; never undeclared read-write aliasing. Layer
   blur (blurring the group's own target before composite) is included as
   a ruled default — same separable kernel, simpler source. Blur =
   iterated downsample + separable kernel; the exact kernel/downsample
   ladder is the implementer's recorded default, carried as
   `algorithm-version` on the effect declaration (Contract G
   derived-effect law), because blur output participates in goldens.

5. **Per-node scissor clip — the retirement ROAD for the CPU clip
   degradations.** The degradations, named (ENGINE §4 / DISCOVERIES D-18),
   are exactly TWO: rects clamp with radii degrading at clip corners
   (`rect_tree.cljc:226-238`, the T-4 clamp); text clips at whole-character
   granularity (`text_layout.cljc:1285-1360` `clip-result`;
   `rect_tree.cljc:505-506`). Image quad+UV co-clipping
   (`image_material.cljc:178-189`) and path CPU triangle clipping
   (`path_gpu.cljs:200-226`) are EXACT — no degradation exists there; both
   stay untouched (their namespaces are MUST-NOT territory anyway), with
   the GPU road open to them LATER if a receipt ever demands it. So the
   v1 GPU road covers the RECT and TEXT lanes: the tree walks gain an
   opt-in mode that RETAINS the composed clip rect on the op instead of
   clamping/truncating geometry (`rect_tree.cljc:186-202` `intersect-clip`
   stays the one CPU composition; the already-carried-but-unread
   `:visibility :clip` tokens — `renderer.cljs:2615`, `path_gpu.cljs:371` —
   become real for opted-in entries). CLIP GRANULARITY IS PER-OP, never
   per-entry: two sibling nodes in ONE `(vi, family)` slot may sit under
   DIFFERENT clip ancestors, so the entry's ops are grouped into
   contiguous same-clip-rect sub-ranges and executed with a scissor per
   sub-range — the image atom's family-owned sub-draw walker is the
   exact precedent (`execute-image-batch!`, `renderer.cljs:2944-2954`);
   a single-scissor-per-entry build is a named wrong build (it passes a
   one-clip fixture and breaks mixed-clip siblings — S3(c) pins it). The
   one existing
   `setScissorRect` site (`renderer.cljs:2931-2937`) currently LEAKS — set
   and never reset within the pass; this cut fixes the semantics: scissor
   is explicit per-entry state (entry's clip rect, else full attachment),
   never inherited by tape neighbors. Entries opting into the GPU road skip
   CPU truncation (geometry uploads unclamped; text ops un-truncated) and
   keep CPU CULL (a fully-clipped-away entry is still dropped — cull is
   invisible-content only and byte-safe). Scissor requires an axis-aligned
   effective transform; production containers are translate+uniform-scale
   today (`scene_runtime.cljs:143-146` writes x/y/scale) so this always
   holds; a non-axis-aligned clip fails closed into the mask road (clip
   rendered as a mask — cut 3's machinery), never a silent wrong clip.
   ACTIVATION LAW: the GPU clip road is per-entry OPT-IN, exercised by
   flag-lane fixtures and new goldens; unflagged product content keeps the
   CPU road byte-identical. The product-wide flip (retiring the
   degradations from Sid's real land) is ONE staged activation with a
   scheduled golden re-bank — a Sid line, not this package's call
   (decisions.md two-dials law: build-ahead by reversibility, activation by
   contact).

6. **Export seeds.** A registered `:readback` road off the compiled plan —
   NOT a product export UI. `export-viewport!`: compile an EXPORT plan
   variant — **`:world` stratum only** (raster capture is a FRAME
   readback, not a per-family projection: the family `:export-projections`
   declarations — `:declared-by-future-exporter` /`:none-promised`/
   chrome's `:none-by-design`, `scene_tape.cljc:333-465` — stay untouched
   as the seam for future STRUCTURED exporters and do not gate the raster
   road). The stratum filter discharges both halves of W0-C P2.10:
   authoring chrome rides `:overlay` (`chrome_derive.cljc:166,219`) and
   does not export; annotations-as-material live in `:world` and DO. The
   export plan ALWAYS compiles the linear road (its own plan, its own
   single transfer — the per-frame no-mixing law holds per plan) —
   rendered through the Contract-C road to an export target,
   present-transferred to rgba8unorm, then `copyTextureToBuffer` +
   `mapAsync` (async only; the verifier's readback shape,
   `verifier.cljs:113-157`, is the source precedent), rows padded to
   WebGPU's 256-byte `bytesPerRow` alignment and stripped after mapping
   (an arbitrary-width viewport MUST work — a 256-aligned-only road is a
   named wrong build), alpha unpremultiplied to straight, PNG bytes via
   canvas `toBlob`/`convertToBlob` (zero new dependencies; the
   `live_atoms.cljs:53-54` precedent). Returns a Promise of bytes +
   records the C7 export receipt fields (output space sRGB, straight
   alpha, intentional losses — NAMED, per M9's spirit: a raster seed
   flattens identity/provenance and is not a family export projection;
   the C7 receipt says so in writing). Flag-only chord (installed by
   `frame-runtime`, its own listener — zero keyboard.cljs edits) downloads
   it for the felt pass.

7. **THEN the animation clock / frame scheduler.** A pure scheduler
   (`frame-scheduler`) generalizes draw-on-world-change into the W0-C §5.7
   cause policy: causes {`:world` `:viewport` `:resource` `:interaction`
   `:clock` `:readback` `:device-recovery`}, coalesced to ≤1 encode per rAF
   opportunity; sleep = today's skip (the world-identity gate,
   `runtime/render.cljs:285-291`, becomes cause derivation — idle-on-clean
   behavior preserved exactly; true rAF-unsubscribe is a routed refusal).
   The CLOCK is injected/monotonic and rebindable (replay + verifier pin
   it; W0-C §5.7.6: replay injects recorded logical time). Its default
   source is the rAF timestamp the reduce ALREADY receives and discards —
   `[world _frame-time]`, `runtime/render.cljs:282` — wrapped by
   `frame-scheduler`'s rebindable source; no new clock is minted. Clocked
   consumers register `{:next-deadline :cadence :stop-predicate}` in
   `frame-scheduler`'s module-level deadline registry (the
   `!frame-arrangement` defonce precedent, `renderer.cljs:3021`) via a
   public `register-deadline!`; the reduce's decide step reads the same
   registry — that is the written road from the flag lane's registration
   to the frame loop. A due deadline is an invalidation. THE COMPOSITION WITH SETTLED GROUND, pinned
   (decisions.md render seam): **no execution clock is ever an ancestor of
   derivation** — the clock value flows ONLY into (a) the scheduler's
   decide step and (b) sink-side paint uniforms; it never enters store or
   scene derivation. The first clocked consumer: the selection-outline
   **PULSE** (chrome's routed debt, flag lane) — outline alpha oscillates
   from a phase uniform computed at the sink; cadence 30Hz; stops (and the
   scheduler sleeps) when the selection empties. Marching ants are NOT the
   consumer: they need dash paint, which chrome's contract routed to the
   paint-axis expansion — that refusal is upheld, not overridden. The
   existing 530ms blink/shimmer timers (`events.cljs:192-203`) are lawful
   clock-as-writer precedents and are left untouched; migrating them onto
   the scheduler is a routed refusal.

**The color-mode law (this package's biggest pin — the pin-or-fork lens run
in writing).** Both scene-color configs already exist:
`scene-tape/legacy-direct-color` and `scene-tape/linear-premultiplied-color`
with the selector `(scene-tape/scene-color linear-premultiplied?)`
(`scene_tape.cljc:26-64`); every system constructor threads a `:scene-color`
kwarg defaulting to legacy (`renderer.cljs:800-803,1091-1093,1536-1539`),
shaders specialize by the `kSceneColorLinearPremultiplied` string replace
(`renderer.cljs:12-36`) and blend derives from the config
(`renderer.cljs:38-42`). W1 Contract C §8.2 is explicit that transparent
group/effect composition must NOT be built on the legacy convention. The
pin: **color mode is a PLAN-LEVEL fact, per frame, never mixed within a
frame.**

- A frame whose compiled plan contains NO effect pass runs the LEGACY road —
  the existing pipelines, direct-present (or byte-preserving copy-present) —
  so every existing golden and the unflagged product stay byte-identical.
- A frame containing ANY group/mask/blur pass compiles to the
  **scene-color/linear road end-to-end**: every pass (base scene,
  group targets, mask targets, composites) renders through the seam-ON
  pipeline set (linear-light premultiplied, `one / one-minus-src-alpha`,
  per W1 §8.1), intermediates are `rgba16float` (linear working space in
  8-bit banding is a named wrong build), transparent intermediates clear to
  `[0,0,0,0]`, and EXACTLY ONE presentation transfer encodes to the swap
  format at the present pass (C4 sentinels prove one-not-zero-not-two).
  No frame ever blends linear and legacy content into one target.
  Linear-mode plans always FULL-CLEAR (no dirty-rect/partial interaction —
  that machinery stays dormant); and the compiler accepts a FORCED color
  mode for verifier receipts only (C3 needs direct paint and the group
  road on the same linear footing).
- GRADIENT LAW (the round's catch): W1 §8.2 — "Linear gradients
  interpolate decoded linear-light stops by default." The legacy rect
  shader mixes ENCODED stops (`renderer.cljs:268-296`) and the seam
  boolean decodes only the mixed result — decode-AFTER-interpolation is
  the named wrong build, and it would pass a naive cross-road C4 because
  both roads err identically. The seam-ON variant therefore decodes each
  stop BEFORE `mix`, guarded by the SAME `kSceneColorLinearPremultiplied`
  const so the legacy path's bytes are untouched. This is a rect-fragment
  SOURCE change: the manifest's recorded shader digest shifts, riding a
  NEW one-shot `--amend-shader-digests` road whose preflight is ALL
  existing goldens byte-identical (a digest amendment is a recorded
  intended change, never a golden edit — the digest exists to catch
  UNINTENDED drift).
- The seam-ON pipeline set is constructed LAZILY on the first linear-mode
  plan (unflagged boots allocate nothing new) — and it is a VARIANT
  LAYER over the EXISTING family systems, never a second system: it
  shares textures, atlases, source registries, and bytes
  (`init-image-system` mints fresh empty registries,
  `renderer.cljs:1086-1168` — calling it again would placeholder every
  already-registered image), minting ONLY mode-specific views
  (the sRGB-view switch, `renderer.cljs:988-992`), bind groups, and
  pipelines, with zero re-decode/re-upload; if variant creation is
  asynchronous it rides the `:resource` cause, else it is a declared
  synchronous first-effect-frame cost (implementer's recorded default).
- Flipping the BASE product to the linear road for effectless frames too
  (with its golden re-bank) is the staged scene-color activation —
  `scene_tape.cljc:54-57` already models it — LATER, Sid's line.

**The export/present prerequisite fix.** The canvas is configured with no
`usage` field (`electric_flow.cljc:818-821`), so the swap texture cannot be
a copy destination and the dormant RT→swap copy road
(`renderer.cljs:3211-3220`) would fail if enabled — the boot log even
claims `"copyDst":true` without passing it (`electric_flow.cljc:812-817`).
One thin hook adds `usage: RENDER_ATTACHMENT | COPY_DST` at configure.

**Pick is untouched in this package — fully, declared, not silent.** The
v1 declared policy (W1 Contract O receipt O4's "according to their
declared policies" clause): paint gains exact GPU clip/mask/opacity; PICK
stays exactly today's road with ZERO changes — and "today's road" is
characterized honestly: `ss/pick` (`scene_store.cljc:368-391`) → reverse
tape → `rt/hit-test`, which GATES DESCENT through every ancestor's
BOUNDS without ever reading `:clip?` (`rect_tree.cljc:595-605`) — so
today is neither clip-aware nor purely clip-blind; it is
ancestor-bounds-gated, and ZERO changes means preserving exactly that
gate, effect-blind including mask and opacity-zero (the chrome
atom's clip-blind-v1 ruling is the precedent), census-counted (the
receipt counts pickable-but-invisible identities). ACKNOWLEDGED
DEVIATION, not silence: W1 §3.2.3 states clips/masks/group visibility
are shared references read by BOTH projections, and W0-C §5.4 says they
modify both — v1 deviates by declared policy exactly as chrome's
clip-blind v1 did (Sid-accepted), and the repayment is ONE named LATER
road: clip/mask/effect-aware pick + the marquee/snap population, taken
together (the chrome contract's together-law, upheld verbatim).

## Ruled at cut — defaults with Sid veto slots (one line reverses any)

1. **Effects are container/arrangement facts, session-only, flag-lane v1**
   (`set-effects!`); durable effect material/grammar is routed to the
   custody/material slice. Unflagged product byte-identical.
2. **Group opacity < 1 always isolates** (offscreen composite semantics —
   Figma's, and D-03's founding argument). Collapse-to-per-child-multiply
   when no internal overlap is a LATER optimization, tripwired by S2's
   overlap fixture.
3. **Blur radius is declared in WORLD units** (scales with zoom — an
   effect of the material, not the screen), projected through effective
   scale × camera zoom, clamped by a declared max-px budget (default 64px)
   with the clamp regime-tagged. Layer blur included beside backdrop blur.
4. **Linear-mode intermediates are `rgba16float`**; the seam-ON pipeline
   set is lazy-created on first need.
5. **Mask v1 = alpha mask from one designated child slot's rendered
   coverage·alpha**; luminance/vector-crop variants routed.
6. **Pick stays fully effect-blind v1** — clip, mask, AND opacity-zero
   (declared per O4; zero pick changes anywhere); the aware road is
   LATER, taken together with marquee/snap population.
7. **Pulse cadence 30Hz, stop-on-empty-selection**; waveform pinned as a
   sinusoid, outline alpha 1.0 ↔ 0.4; phase is sink-side only; the phase
   uniform is DISARMED by default (disarmed ⇒ outline bytes identical —
   chrome's existing goldens stay byte-identical even as its shader
   digest shifts).
8. **Export v1 = current viewport at physical canvas pixels, raster PNG,
   `:world` stratum, straight alpha sRGB**; world-rect export and SVG/PDF
   are routed.
9. **Non-axis-aligned clip fails closed into the mask road** — never a
   silently wrong scissor.
10. **Idle stays skip-based** (the rAF subscription persists; encodes stop —
    W0-C receipt #5 measures encodes); rAF-unsubscribe sleep is routed.
11. **The scheduler restructure runs UNFLAGGED but behavior-identical**
    (cause derivation reproduces today's exact skip/encode pattern;
    receipted in S4); every visible W4 behavior (effects, pulse, export
    chord, GPU-clip fixtures) rides `?live-atoms=1`.
12. **Group/effect targets are viewport-sized v1, drawn from the
    recycling pool** (nesting depth bounds pool depth; the M10 receipt
    proves steady-state); span-AABB-sized targets are a named LATER
    optimization.

## Refusals — deliberately not in this package (each routed, never a void)

- Blend modes (multiply/screen/…) → paint/composite expansion; the
  composite pass's operator seam is shaped for named operators (Contract C
  §8.1), none added here.
- Glass / noise / texture effect stacks + effect ORDERING grammar
  (multiple stacked effects per group) → effect-stack expansion; v1 fixes
  the order (mask → layer-blur → opacity → backdrop composite), a stack
  grammar arrives with the material slice.
- 3D regions, `scene-depth`/`object-id` resources, region cadence &
  multi-rate compositing → Package 3 (the `:region` pass-kind and
  `:enabled-when` resource shape stay validated data; the island probe is
  the ancestor).
- Clip/mask/effect-aware pick (opacity-zero included) + marquee/snap
  population → ONE LATER road, taken together (chrome's together-law).
- Stencil-buffer clipping — the ENGINE row's "scissor/stencil" is
  discharged v1 as scissor (axis-aligned, exact) + the mask road
  (general, covers non-axis-aligned); a true stencil road (depth/stencil
  attachments) → routed to the road that first demands it — Package 3
  regions bring depth attachments, or a perf receipt on mask cost.
- GPU-clip for the image/path lanes — their CPU clips are exact today
  (no degradation); extending the scissor road to them → LATER, on a
  receipt.
- Rotation authoring, rotated-bounds chrome, rotate handles → the
  authoring slice. The path/chrome routings that pointed rotation at W4
  are RE-ROUTED WITH CAUSE: W2-A already landed the 32-byte affine
  transport (`renderer.cljs:60-64,734-748`); what remains is authoring
  gestures + chrome, which are not frame-kind. W4's obligation to rotation
  is only: the mask road covers non-axis-aligned clips (ruled default 9).
- Moving sampled media (video/GIF) → the media atom (its seams here:
  the `:resource` cause + the clock).
- Eyedropper UI (CH-12) → chrome expansion; the scene-color readback seam
  it needs is cut 6's deliverable.
- Marching ants / dash paint → paint-axis expansion (chrome's own
  refusal, upheld; pulse is the v1 clocked consumer).
- Migrating caret blink / shimmer onto the scheduler → cleanup at the
  clock's second consumer.
- True rAF-unsubscribe idle (power) → perf road.
- Damage-rect / partial-present revival (the dormant dirty-present
  machinery stays dormant) → perf road at profile pressure.
- Product-wide CPU-clip retirement flip + base-scene linear-color
  activation (both = golden re-banks) → ONE staged activation act each,
  Sid's line.
- SVG/PDF/vector export, world-rect export, thumbnails/minimap → exporter
  package / readback-seam consumers, LATER.
- Durable effect grammar (material-level masks/opacity/blur with
  provenance/versioning; M1–M9 for an effect material) → the custody/
  material slice; v1 citizenship is the ephemeral-declaration pin
  (chrome's precedent).

## Laws — pointers, not restatements (each with its operationalizing scenario)

- **W0-C §5 (the frame-graph + scheduler contract)** — the data model
  (§5.2), the sampled-read aliasing law, family registration (§5.3),
  ordered truth (§5.4), the ten fail-closed validation laws (§5.5), the
  executor loop (§5.6), the seven causes + eight scheduler rules (§5.7),
  the migration map (§5.8), receipts #4–#7 (§5.9). This package makes it
  code-real for groups/effects/time. → S1, S3, S4.
- **W1 Contract C** (§8.1–8.3) — canonical meanings, the premultiplied
  source-over default, group opacity applied once, intermediate target
  declarations, exactly-one transfer, C1/C3/C4/C6/C7 receipts; the live
  pipelines are migration input, not precedent. → S2, S3.
- **W1 Contract O §3.2** — pass-splitting only under forward-order
  equivalence; transparent entries/masks/group targets/backdrop inputs
  carry explicit order dependencies; O4 visibility policies are DECLARED
  (this package's fully-effect-blind pick declaration lives under it,
  with its acknowledged §3.2.3 deviation named in Scope). → S1, S2.
- **W1 Contract G §4.2(4) + §4.3** — effects declare geometry: source
  authority, named operator, finite support/budget, `:pick :none` (§4.2
  item 4); `:boundary-relation :derived-effect` — a blur's contour is
  never a second mathematical boundary (§4.3). → S2, S3.
- **W1 Contract M** — M10 (target pool lifetime/budget owner; budget
  refusal mirrors the path system's policy; device loss rides the existing
  system-recreate road; asset-unavailable N/A by grammar) → S3. M11 —
  its composite/equivalence half → S2 (C1/C3/C4 rows); its export-color-
  metadata half → S4 (the export receipt records output profile + alpha
  association, the C7/M11 fields). The ephemeral-citizenship pin (chrome
  precedent) covers session-only effect declarations.
- **ENGINE.md §0** — the gate sentence (families/effects REGISTER; the
  negative-space law W0-C §5.1: adding mask/group-blur/region must never
  require the executor to name a family — S1's static fence) + the W4 row
  (single ownership, serial cuts) + the hard join "W4 before regions". →
  S1.
- **decisions.md "The render seam"** — recompute proportional to change
  (plan compile is change-driven with the batch oracle fenced; effect-span
  derivation is proportional to the touched container); no execution clock
  as derivation ancestor (the clock pin in cut 7); effects at mutation
  sites and the frame edge; the five declarations on every new derive. →
  S1, S4.
- **Dark-lane / flag / golden laws** (decisions.md; the chrome contract's
  SHARED-FILE LAW) — new namespaces load pure; flag absent →
  byte-identical; goldens append-only; shared-source digest shifts ride
  the input-amendment roads, never golden edits; the manifest's
  `shaderDigests` pin SIX renderer WGSL sources
  (`verifier.cljs:2770-2775`) — five hold via the existing
  string-replace reuse; the RECT-FRAGMENT source changes for the
  gradient law and rides the new `--amend-shader-digests` one-shot
  (preflight: all existing goldens byte-identical); chrome's WGSL lives
  in the chrome INPUT-digest set (`run_verifier.mjs:378`), so the pulse
  uniform rides the chrome input-amendment road. → S3, S5.

## Exact entry points

New namespaces (ALL new code lives here):

- `src/app/client/substrate/frame_graph.cljc` — the plan model + compiler:
  resources (kind/format/lifetime/budget-owner) · passes (kind,
  attachments, load/store, reads with declared producer edges, entry
  spans) · plan compile from (arrangement-order snapshot × effect spans ×
  capabilities × viewport × color mode) · the ten fail-closed validation
  laws · color-mode selection (+ the verifier-only forced mode) · the
  export plan variant · plan hash · span derivation from stack-path
  prefixes · the structure-vs-binding split (structure reused on its
  input tuple; entry-range binding resolved fresh per frame) · the
  batch-oracle twin of the reused-structure road (two code paths,
  fenced — the `chrome_derive`/`maintained_view` shape, never one fn
  against itself) · the render-seam five declarations (keyed inputs ·
  door · ownership · projections · oracle) written as this namespace's
  docstring contract, the chrome_derive precedent.
- `src/app/client/substrate/frame_effects.cljc` — effect grammar +
  fail-closed validation (`:opacity` `:mask` `:layer-blur`
  `:backdrop-blur` `:isolate?`) · Contract-G `:derived-effect`
  declaration data (operator, `algorithm-version`, support/budget, the
  radius→px projection law + max-px clamp) · effective-effect-chain
  derivation (container registry → per-entry/per-span references,
  value-diff proportional) · the CPU reference composite (linear premult
  source-over + group-opacity + mask multiply — S2's oracle, the
  `run-color-receipts!` shape).
- `src/app/client/substrate/webgpu/compositor_gpu.cljs` — the target pool
  (create/recycle `rgba16float` + swap-format targets, gpu-budget
  registered, destroy road) · the seam-ON VARIANT-LAYER constructor
  (lazy; shares each family system's textures/registries/bytes and mints
  only mode views/bind-groups/pipelines with
  `(scene-tape/scene-color true)` — never a second system) · the
  seam-ON rect-fragment gradient-decode variant (the same-const guard) ·
  composite pipeline (WGSL: sample
  group target, apply opacity/mask, premultiplied blend) · separable blur
  pipelines + downsample ladder · snapshot copy (the backdrop producer
  edge) · the present/transfer pipeline (linear → swap format, the ONE
  conversion) · scissor application helper (per-entry set/reset law) ·
  the readback road (`copyTextureToBuffer` + `mapAsync` + 256-byte
  `bytesPerRow` padding/strip + unpremultiply + PNG bytes).
- `src/app/client/substrate/frame_scheduler.cljc` — cause model +
  coalescing · the injected monotonic clock (rebindable source; the
  replay/verifier pin) · deadline registry ({:next-deadline :cadence
  :stop-predicate}) · the decide step (prev-state × now → {:encode?
  :causes :time}) · the cause/time/plan-hash ring receipt · pure,
  JVM-tested.
- `src/app/client/workspace/frame_runtime.cljs` — flag-boot wiring: fixture
  effects (a group of existing live-atoms fixtures with opacity · a masked
  group · a frosted backdrop-blur panel · a GPU-clip fixture pairing the
  two retired degradations) · pulse registration against the selection —
  the stop-predicate reads the PUBLISHED chrome census receipt
  (`globalThis.__softlandChromeReceipt`, chrome's declared public
  surface; zero chrome_runtime edits; a nil receipt reads as empty
  selection = pulse disarmed; frame_runtime boots AFTER chrome_runtime
  in the live_atoms boot order; phase is sink-side) · the export chord
  (its own flag-only listener; downloads the PNG) · provider installs +
  `globalThis.__softlandFrameRuntimeReceipt` {:color-mode :passes
  :export} (the established receipt pattern; encode/deadline counts live
  in the flag-independent scheduler receipt).

Thin hooks only (few lines each, enumerated):

- `renderer.cljs` (3222 lines — thin hooks ONLY): `draw-frame!` compiles
  the plan BEFORE opening passes and delegates multi-pass encoding to
  `compositor-gpu`/`frame-graph` when the plan is non-trivial (the
  legacy single-pass body remains the no-effect road, byte-identical);
  `execute-gpu-batch!` (`:2931-2937`) gains the per-entry scissor
  set/reset law; system constructors are reused, not modified (the
  seam-ON set calls them with the linear config, `:800-803` etc.);
  `frame-family-registry` untouched except any additive `:contract`
  fields the plan validator reads; the chrome prepare call site
  (`:3177-3178`) threads the sink-side phase value into
  `prepare-chrome-frame!` — the clock's ONLY road into paint.
- `scene_tape.cljc` — additive: plan/validation vocabulary the compiler
  shares (pass-kind/resource keywords already exist `:584-588`); the
  scene-color selector (`:26-64`) is consumed, not changed.
- `scene_store.cljc` — additive only: the flattened op lanes carry
  through whatever clip field the walks now emit (key-read; existing
  consumers unaffected). `derive-store-frame` (`:314-351`) is UNCHANGED —
  still `[store]`, still a function of the store value alone — and the
  pick road (`:368-391`) is UNTOUCHED.
- `rect_tree.cljc` — the rect/text walks gain the opt-in GPU-clip mode:
  clip rect RETAINED on the op (unclamped geometry, un-truncated text
  ops) when the node opts in; default path byte-identical
  (`intersect-clip` `:186-202` unchanged as the one composition).
- `scene_runtime.cljs` — `set-effects!` beside `set-transform!`; the
  registration opts pass `:effects` through (`:143-158`).
- `containers.cljc` (300 lines) — the registry rows carry `:effects`
  (additive field beside `:parent`/`:layer`, `:65-105`).
- `runtime/render.cljs` — the reduce's dirty gate (`:285-291`) becomes
  scheduler-cause derivation via `frame-scheduler/decide` (behavior-
  identical when only world/interaction causes exist; the discarded
  `_frame-time` at `:282` becomes the clock's default source); threads
  the clock + deadlines + new systems into `draw-frame!` at the one call
  site (`:717-772`); publishes the scheduler receipt UNCONDITIONALLY
  (`globalThis.__softlandFrameSchedulerReceipt` {:encodes :causes-ring
  :clock-mode} — flag-independent diagnostics, the receipt-global
  precedent; this is what S4's unflagged rows and S5's flag-off tripwire
  read).
- `electric_flow.cljc` (866 lines) — the canvas `configure` gains
  `usage: RENDER_ATTACHMENT | COPY_DST` (`:818-821`); one
  `frame_runtime` mount line beside the live-atoms mount.
- `live_atoms.cljs` — fixture effect declarations + `frame_runtime` boot
  hook (the named-hook exception, as chrome had).
- `chrome_material.cljc` / `chrome_gpu.cljs` — the pulse: an additive
  phase uniform + alpha modulation on the selection outline, DISARMED by
  default (disarmed ⇒ fragment output byte-identical, so chrome's
  existing goldens hold even as its shader digest shifts → chrome input
  amendment). These are W4's ONLY prior-atom namespace edits, named here;
  everything else in path/connector/image namespaces is untouched.
- `test/app/test_runner.clj` — register `frame-graph-test` ·
  `frame-effects-test` · `frame-scheduler-test` (pure tier, fail-closed
  inventory).
- Verifier lane (append-only, the established shape): `verifier.cljs`
  `run-w4-frame-runtime!` (the S2/S3/S4 machine receipts + 3 goldens +
  the clock pin + export digest) wired into the top-level `Promise.all`;
  `run_verifier.mjs` w4 rows + `w4FrameRuntimeInputs` digest set +
  one-shot `--append-w4-goldens` (preflight: prior banks byte-identical +
  no existing w4 cases) + `--append-w4-input-amendment` + the NEW
  `--amend-shader-digests` one-shot (preflight: ALL existing goldens
  byte-identical; records the intended rect-fragment change); `manifest.json`
  gains the w4 sets; `verify_scene_tape_fence.mjs` — the families array
  (`:63-73`) and the seeded-effect-family negative fence (S1's static
  fence) land here, the chrome precedent. SHARED-FILE LAW: prior
  families' input digests shift (renderer/scene_store/scene_tape/
  rect_tree/chrome edits) — run the existing path/connector/chrome
  `--append-*-input-amendment` roads AND ADD the missing image
  input-amendment road (no `--append-image-input-amendment` exists
  today, `run_verifier.mjs:36-44`, while `imageAtomInputs` digests four
  W4-edited files, `:282-287` — same one-shot preflight-guarded shape as
  the other three), never a golden edit.

CHECKED CLAIMS for the implementer (verify FIRST, minutes not hours):
`getCurrentTexture` copy-destination validity after adding `usage` at
configure (the boot-log/`copyDst` mismatch, `electric_flow.cljc:812-821`) ·
`rgba16float` render-attachment blendability on the verifier's SwiftShader
adapter (core WebGPU says yes; receipt it — GPU receipts state adapter
identity first) · the scissor-leak fact (a scissored entry followed by an
unscissored entry today inherits the scissor — S4's regression case
depends on it) · `queue.writeBuffer`-during-open-pass legality is NOT
relied on after the restructure (prepare moves before pass encoding —
today's prepare-inside-pass is `renderer.cljs:3168-3178`) · the
`:stratum`/stack-path span assumption: every container's entries are
tape-contiguous under `compare-order` (if an interleaving exists, the plan
compiler must handle split spans — a fork note in the baton, not a stop).

## Decisive scenarios (frozen as tripwires + goldens at close)

1. **Plan truth** [JVM + one verifier row]: the JVM half runs on
   SYNTHETIC arrangement values built from `scene_tape.cljc`'s own
   `.cljc` vocabulary (order tokens, `compare-order`, family contracts) —
   no `.cljs` require anywhere in the pure lane, and the plan
   structure-vs-recompile twin lives INSIDE `frame_graph.cljc` (the
   renderer's own entry-level twin is untouched); compile is
   deterministic (same inputs → same plan
   + hash; registration order shuffled → identical, the O1 shape); each of
   W0-C §5.5's TEN validation laws has a rejecting fixture (duplicate ids ·
   cycle · read-before-produce/alias · ≠1 presentation terminal · drawable
   without geometry/pick ownership · overlay without policy · clocked
   family without clock+stop · underivable pick order · undeclared
   hand-path readback · resource without lifetime/budget owner); color-mode
   law (no effects → `:legacy`; any effect → `:scene-color/linear`; never
   mixed — asserted on the plan value); group spans derive from
   stack-paths incl. nesting with inner-first topological pass order;
   non-axis-aligned clip compiles to the mask road, never a scissor; a
   FLAG-ON frame with ZERO effect declarations still compiles `:legacy`
   (a build keying color mode on the flag instead of the declarations is
   the named wrong build); the
   export plan is `:world`-stratum-only and always linear; the reused
   plan STRUCTURE + fresh range binding equals the full recompile after
   every mutation sequence, including entry insert/remove between
   reuses (two code paths); a `:region` pass-kind plan
   VALIDATES (door open) and is refused for execution (not built). The
   one verifier row: an effectless scene through the COPY-PRESENT plan is
   BYTE-IDENTICAL to direct-present (W0-C §5.9 receipt #4, discharged on
   the legacy road — also the receipt for the canvas `usage` configure
   fix). The
   static fence: the negative-space law — adding a seeded fake effect
   family touches registry/data only; a seeded central `case` naming it
   fails the fence (the `verify_scene_tape_fence` shape).
2. **Composite truth** [JVM + verifier]: the founding fixture — two
   overlapping 50%-opaque siblings inside a 50% group — matches the
   `frame-effects` CPU linear-premultiplied reference at the intersection
   within the declared quantization ε, where the per-child-multiply wrong
   build differs from the reference by a margin ≫ ε (both the ε and the
   wrong-build margin are computed and recorded in the receipt — the test
   is decisive by construction, not by hope); C3 group equivalence —
   direct paint == the same content through an
   isolated group at opacity 1, BOTH compiled under the verifier-forced
   linear mode (the force exists exactly for this receipt; within the
   declared quantization ε; recorded per-channel max delta); C1 reference
   source-over on the linear road incl. output alpha; C4 exactly-one
   transfer — tagged solids equivalent across direct-legacy,
   linear-intermediate, readback, and export within declared ε, with
   double/missing-conversion sentinels — and the GRADIENT DECISIVE
   fixture: a black→white linear gradient's midpoint through the linear
   road reads ≈0.735 encoded (decoded-stop interpolation, W1 §8.2);
   the decode-after-mix wrong build reads 0.5 and FAILS (gradients are
   deliberately excluded from naive cross-road equivalence — the two
   roads lawfully differ there until the base-scene activation);
   mask truth — inside/outside/
   soft-edge samples against the CPU reference, mask source absent from
   normal paint; an opacity-zero group absent from PAINT while its
   identities remain pickable-and-census-counted (the declared v1 pick
   policy, asserted as written — a build that silently drops them from
   pick violates the pick-untouched law); effect-chain proportionality — one
   container's effect change re-derives its span only (counter receipt,
   siblings 0, static frames 0).
3. **The composited scene** [verifier goldens]: 3 appended goldens — (a)
   nested groups (opacity in opacity) + alpha mask over mixed
   block/image/path/connector content at zoom 1; (b) a backdrop-blurred
   panel over the same content at zoom 1 and one legal-domain station
   (0.1), radius world-scaled, clamp receipt if hit; (c) the clip fixture:
   a rounded-corner rect + shaped text crossing a clip edge on the GPU
   road — radii intact at the clip corner, the boundary glyph PARTIALLY
   rendered. Because a first golden capture would bless whatever the
   build produces, (c) carries PROBE-PIXEL ASSERTIONS beside the golden:
   a pixel inside the clip at the corner-radius arc must read BACKGROUND
   (the CPU clamp road paints it square — the named wrong build), and a
   pixel inside a glyph fragment the CPU road drops whole must read
   NONZERO coverage — and (c) includes TWO SIBLINGS under DIFFERENT clip
   ancestors in one view instance, each clipped to its own rect (the
   per-op granularity probe; a single-scissor-per-entry build fails it);
   the mixed-content golden (a) registers its images into the LEGACY
   system BEFORE the linear variant set exists — the product's
   activation order — so a build whose "linear system" is a second
   empty system placeholders the image and fails (anti-preload law);
   determinism ×2; the
   aliasing receipt — the backdrop plan names its snapshot copy edge and
   no pass reads its own attachment (plan-level assertion, not prose);
   blur cost receipt at zoom stations (ms, regime-tagged, adapter
   identity stated first — SwiftShader numbers are SwiftShader receipts);
   M10 rows — target-pool budget registration, recycle bounds (steady
   allocation across 100 frames), destroy releases against the tracker,
   budget refusal by name; the C6 format receipt — every enabled
   intermediate/export target records format, transfer, alpha
   association, adapter fingerprint, and regime (the `rgba16float` rows
   land here; adapter identity stated first); ALL existing goldens
   byte-identical (legacy mode untouched — including chrome's, whose
   pulse uniform is disarmed by default); the MSDF counterexample stays
   RED; input amendments recorded for every prior family whose
   shared-source digests shift.
4. **Scheduler/clock/readback truth** [JVM + verifier + unflagged]:
   cause derivation reproduces today's behavior EXACTLY when no W4
   feature is active (clean scene → 0 encodes; a world change → 1; a
   camera move → 1 — the S4 unflagged receipt, also the flag-off
   tripwire); pulse — encodes tick at the declared 30Hz cadence ONLY
   while the selection is non-empty, then the deadline retires and
   encodes return to 0 (encode-count receipts read from the UNFLAGGED
   scheduler receipt global; W0-C §5.9.5 in full: "Clean scene produces
   no frames; interaction wakes one; a clocked region wakes only to its
   declared cadence and sleeps on stop" — all three clauses asserted);
   the clock is sink-only,
   EXECUTABLY: the JVM tripwire derives the store frame twice from the
   same store value while the injected clock advances between the two
   calls — byte-identical outputs, or the law is broken (the render-seam
   law as a test, not prose); the pulse golden pins a FIXED injected
   clock (deterministic phase bytes); a STATIC token check over the
   three pure namespaces + the frame_graph/frame_effects derive paths —
   no `js/Date`, `performance.now`, rAF, or interval tokens (the chrome
   no-text-check shape; the clock-hygiene MUST-NOT made fail-able);
   replay — a recorded cause/time sequence re-fed to the scheduler
   yields the same encode/skip decisions and plan hashes, AND the pulse
   fixture re-rendered under the replayed clock reproduces its pixels
   on the same fingerprinted environment (W0-C §5.9.6, both halves);
   export — async only (no `mapAsync` await in the encode path;
   receipt), byte-digest stable ×2, the C7/M11 metadata recorded
   (output profile sRGB, straight alpha, intentional losses named),
   chrome/overlay absent from the
   exported pixels (a fixture with visible selection chrome exports
   without it), the 256-byte row-padding case at a deliberately
   non-aligned viewport width renders/strips correctly; the scissor
   regression — a scissored entry followed by a full-viewport entry
   paints the full viewport (the leak, fixed, frozen).
5. **The felt receipt** [Sid's eyes, `?live-atoms=1`]: a group of real
   fixtures breathes as ONE SHEET at 50% opacity (the intersection does
   not darken); the frosted-glass panel sits over live content and stays
   frosted under pan/zoom; the masked group reveals only through its
   mask; the clip fixture shows rounded corners + smoothly-clipped text
   at its edge; shift-click a fixture — the selection outline PULSES,
   and stops costing frames the moment the selection clears (encodes
   visible in `__softlandFrameSchedulerReceipt`); the export chord
   downloads a PNG of the world with no chrome in it; flag absent →
   byte-identical product (extends the chrome flag-off tripwire: legacy
   color mode, zero effect passes, zero intermediates, no pulse, no
   export listener, encode pattern identical, all new namespaces load
   pure).

## MUST-NOTs

Never read `src/app/server/env.clj` · NO durable/Rama writes (effects,
fixtures, selection, exports are client-session values) · never edit
existing goldens/manifest rows or the MSDF RED case — append only;
shared-source drift rides the input-amendment roads and the ONE named
shader-digest amendment (rect-fragment, gradient law) rides its
byte-identity-preflighted one-shot · no hand-positioned
central family/effect branch (the negative-space law; the fence self-test
stays green) · no new JS/npm dependency (PNG via canvas API) · no
execution clock as a derivation ancestor — no rAF/interval/`js/Date`/
`performance.now` in any derive path; the injected clock reaches only the
scheduler decide and sink-side uniforms · no synchronous GPU readback in
any frame path — readback is async and `:readback`-caused only · no pass
binds one subresource as sampled read AND writable attachment · no second
pick road and ZERO pick changes — `ss/pick` (`scene_store.cljc:368-391`),
`rect_tree` hit-test, marquee, and snap populations are all UNTOUCHED
(the declared effect-blind v1 policy) ·
`renderer.cljs`/`electric_flow.cljc`/`ground.cljs`: thin
hooks only, and `ground.cljs` = ZERO edits in this package ·
path/connector/image namespaces untouched (chrome_material/chrome_gpu's
pulse uniform + live_atoms' named hooks are the enumerated exceptions) ·
the scheduler restructure may not change unflagged encode/skip behavior
(S4's unflagged receipt is the law) · commits only at Sid's word — code
and docs separate, both on `docs/current-mental-model-local`, exact-path
staged, never push.

## Close

Scenarios 1–4's JVM halves freeze as ~5 tripwires across the three pure
namespaces (`frame_graph_test` · `frame_effects_test` ·
`frame_scheduler_test`); scenario 3's goldens + scenario 2/4's verifier
receipts join the permanent bank via the one-shot scoped append road;
focused suite = the three namespaces GREEN + `npm run
verify:render-engine` whose ONLY red is the preserved MSDF counterexample
(W1's canonical exit — never "fixed" by this package). Foreign failures
are board debt, never stops. One NOW entry (≤15 lines, self-audit
included) in `W4-FRAME-RUNTIME-NOW.md` + the board line flip (NEXT after
W4: the T2 input-floor contract, then Package 2's seam courtroom over the
whole package set). Acceptance = Sid's word.

## Codex opening prompt (implementation — after the round closes)

```
You are implementing the W4 FRAME-RUNTIME package for Softland's render
engine — one pass, the whole package, one session, SERIAL internal cuts in
the contract's order (pass-graph → groups/opacity → masks → blur → clip →
export seeds → clock/scheduler). You are the single owner of the frame
loop; no cut is parallelized.

Read first, nothing else needed:
1. docs/render-engine/W4-FRAME-RUNTIME-CONTRACT.md  (this contract — the build law)
2. docs/render-engine/W0-C.md §5                    (the frame-graph/scheduler design paper W4 makes code-real)
3. docs/render-engine/W1.md                         (Contracts O/G/M/C — §8 Contract C governs every intermediate)
4. .claude/skills/work-package/SKILL.md             (the one-pass law: build, close, when to ask)

The chrome/path/connector atoms are your structural precedents AT THE
SOURCE LEVEL ONLY — scene_tape.cljc (order model, family registry, the two
scene-color configs), renderer.cljs draw-frame!/execute-gpu-batch!/the
maintained arrangement + batch-oracle twin, chrome_derive.cljc (the
two-code-path oracle fence shape), live_atoms.cljs (flag lane), the
verifier append roads. Consume them as precedents; edit only the thin
hooks the contract enumerates. Do not read IMAGE-ATOM-*/SEAM-STEP1 docs
(pre-law).

This contract has been through its four-lens author pass and its bounded
falsification round — build what is WRITTEN; the repairs are law.

Build the WHOLE package straight through. Keep your own falsification pass
and fix what it surfaces in-session. Ambiguity → strongest default + a
note in docs/render-engine/W4-FRAME-RUNTIME-NOW.md. A genuine fork (two
readings that cannot both hold) is ONE question in that file — route
around it and keep building; never stop. Foreign test failures are board
debt, never stops. The checked claims to verify FIRST (minutes, not
hours) are listed at the end of the contract's Entry points section — do
them before cut 1.

Close per SKILL.md: tripwires + goldens frozen from the contract's
scenarios; focused suite: the three pure JVM namespaces GREEN, and
verify:render-engine red ONLY on the preserved MSDF counterexample (its
canonical state — do not fix it). NOW entry ≤15 lines, board flip.
Commits only at Sid's word. Acceptance is Sid's word — you never wait on
a review round.
```
