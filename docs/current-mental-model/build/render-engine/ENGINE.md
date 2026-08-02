# RENDER-ENGINE — the working map

**Status:** working map, formed 2026-08-02 by the engine inquiry (origin
session, standing orchestrator). It settles a multi-round falsification:
seven-class map → two guarantees → four guarantees → coverage×paint →
the practice re-read. The derivation lived in-session; this doc is the
distillation. Evidence prices everything downstream — nothing here is
canon unless marked so.

**Epistemic marks (load-bearing):**
- **[CODE]** — verified by reading source this arc (file:line where it
  matters; re-verify after any renderer change).
- **[GK]** — standard graphics knowledge, not repo-verified.
- **[HYP]** — provisional hypothesis; each carries its named killer.
- **[SID]** — a judgment about the world only Sid makes; a recorded
  lean is the orchestrator's carried position, NOT a ruling.

**Priced by lived evidence:** cut 2 (structure verbs over two atoms) →
"now i can draw rectangles within rectangles … is this all goineg
somewhere???" — the Tier-I ceiling, felt. G7's killer was grade-4
illegibility (§1). The engine answers the vocabulary wall; it does not
answer the meaning wall — both are real, only one lives in this doc.

## 1. The promise, reinterpreted

"No tldraw-, Figma-, or Blender-class workshop blocked by missing
rendering capability" — reread from practices, not products:

- The referents are PRACTICES: thinking-by-drawing (tldraw) · interface
  design (Figma) · spatial composition (Blender). Products are how Sid
  points at practices. Copy the feel, never the mechanics (Sid, 07-31).
- Beneath all three sits the FOURTH practice — the true referent:
  **designing and iterating Softland's components inside Softland** —
  ancestor Smalltalk/HyperCard, not any of the three products. The
  promise's truest form (the Smalltalk sentence): *the world can render
  whatever it needs in order to redesign itself, at every layer,
  without leaving itself.* The three products are reference shadows of
  this practice on three walls.
- The promise is FOUR-LAYERED; each layer carries a different
  guarantee-kind:
  1. **atom floor — closes** (finite evaluation modes, §5);
  2. **tool-math — adoptable-unbounded** (shaping, booleans,
     tessellation: never architectural, never "done");
  3. **composition/practice — open-ended** (meaning lives here;
     G7-class failures stay possible forever; no engine work addresses
     them);
  4. **the seams — novel** (coexistence, §8: no product ever promised
     one world; nothing anywhere to copy).
  Every earlier map let the closable bottom carry the emotional weight
  of the whole sentence. It must not.
- **"Blocked" is four-graded**, each grade with its own verifier:
  **absent** (existence proofs) · **degraded** (golden receipts) ·
  **slow** (regime benchmarks, §7) · **illegible** (lived contact —
  the grade that killed G7; engine work never touches it).

## 2. The four guarantees

- **G-cover** — the declared envelope (§3) is spanned as MATERIAL:
  rendered + pickable + grammar-editable + versioned. Rendering
  without the other three is a screenshot, not an unblocked workshop.
- **G-extend** — beyond the envelope, additions are bounded and local
  through a CONTRACTED seam. Today extension is precedent, not
  contract [CODE: five pipelines, each hand-wired into one pass].
- **G-rhythm** — additions land session-scale, not research-scale.
  Provable only as a RATE: ≥2 dissimilar atoms landed. One addition is
  an anecdote.
- **G-verify** — the promise stays cheaper to check than to break:
  standing machine probes for the machine-checkable half; short-cadence
  lived contact for the felt half.

## 3. Declared envelope [SID — proposed, provisional until ratified]

**IN:** tldraw-full · Figma-core — shapes, booleans, images, masks,
blends, shadows, blur, and text AT PRACTICE WEIGHT (typography is core
to interface design, not a garnish; today: one monospace font [CODE]) ·
Blender-as-scene-composition (grey-box → lights → PBR ladder · gizmos ·
grease-pencil-equivalent = our strokes in 3D).

**OUT, named, with provenance:**
- path-tracing / photorealism — intent-discovered (live worlds, not
  offline renders);
- multiplayer — intent (the agent-hand is a DIFFERENT capability, not
  its replacement — Sid's correction, 08-01);
- plugins — intent (the vm IS the extension story, deeper than any
  plugin API);
- volumes — MIXED provenance: plausibly outside the practice AND
  conveniently expensive; = the marched mode (§5); redrawable by one
  Sid sentence ("I want to sketch a foggy scene" breaks it);
- vector networks — introduced by the orchestrator; tripwire: the
  icon-drawing practice deepening reopens it.

**FLAGGED IN by carried lean [SID]: motion** — previously excluded by
PURE OMISSION (caught 08-02; neither intent-discovered nor decided).
Live components make motion MORE native here than in Figma — states
actually transition instead of being prototyped. Render-cheap [GK];
grammar cost (a transition as material, a spring as a claim, time in
the grammar) unknown [HYP]. Staged late, declared in.

**Carried leans awaiting Sid's word [SID]:** coexistence promised
STRUCTURALLY now, demoed late (§8) · character = product-grade
interaction fluency with Softland-native document semantics (mimic the
trained hands; diverge on draft/settle, provenance, liveness — the
soul).

## 4. Evidence spine [CODE]

What the renderer IS (renderer.cljs, verified 2026-08-02; untouched by
both pending uncommitted code layers):

- ONE render pass, painter's order, hand-sequenced draws; NO depth
  attachment anywhere; no MSAA — the AA philosophy is analytic
  throughout.
- FIVE pipelines: SDF rich-rect (per-corner radii, borders, 2-stop
  linear gradient — the 29-word instance in buffer_pool.cljs IS the
  current paint axis) · MSDF glyph (sampled) · **Slug glyph — a live
  GPU quadratic-bezier evaluator** (renderer.cljs:375-514: banded
  curve/band textures, per-pixel root-solving; nonzero winding by
  construction — every rendered "o" is a multi-contour path with a
  hole) · shadow · clear-quad.
- Slug status, precise: shipping, end-to-end, runtime-switchable
  (manifest `preferredBackend: "slug"` on DejaVu; fonts.cljs:101-104
  activates it when that font is selected) — but the BOOT-DEFAULT font
  is MSDF-backed, so Slug is less battle-tested than "live" implies.
- Slug curve/band textures are `COPY_DST`, fed by plain
  `queue.writeTexture` (renderer.cljs:700-713) — runtime re-upload is
  mechanically open; cost unmeasured. **fp16 risk:** the curve texture
  is `rgba16float` — half-float control points. Glyphs survive in
  em-space; arbitrary shapes need shape-local normalization and may
  visibly quantize (~1/2048 relative) at zoom → E0.
- Containers: a UNIFORM buffer (no storage buffers exist in the file),
  1024 × 16B `[x y scale flag]`, hard-capped throw; the screen flag is
  the chrome seam; **no rotation at the byte level** — the touch-set
  for adding it is exactly enumerable (formats + WGSL structs +
  write-containers! + CPU inverse picks).
- buffer_pool.cljs: slot-based differential pool, O(1) per-slot
  writes, per-type layouts — the instance-tier heap EXISTS. What does
  not: variable-length RANGE allocation (per-shape curve data).
- A persistent offscreen render target with `RENDER_ATTACHMENT |
  COPY_SRC` sits in the standard frame ("Phase 6E") — render-to-texture
  and readback seeds exist (eyedropper, raster export, thumbnails are
  one copy away).
- Scale provisioning: boot sizes 1,000,000 text + 50,000 rect
  instances — the scene regime is already in the engine's assumptions.
- Zoom clamp [0.1, 8.0] (fm:space resolved wear, P0 S6) — an ~80×
  range. "Infinite zoom" is NOT current truth; deeper zoom is an
  envelope parameter, never an assumption.

## 5. The basis: coverage × paint [GK, hunt-tested]

An atom answers two independent questions: what math decides a pixel's
COVERAGE, and what math decides its PAINT. Six coverage modes span
shipped raster graphics: **implicit-analytic** (SDF) ·
**boundary-analytic** (curve-solving) · **tessellated** · **sampled** ·
**marched** · **composited**. Paint is the orthogonal axis (solid ·
gradients · image · procedural · lit). New paints are
fragment-shader-local (cheap); new coverage modes are the structural
events.

- THREE modes run today [CODE]: implicit (rects), boundary (Slug),
  sampled (MSDF). Missing the MUNDANE: tessellated (Figma's workhorse)
  and composited. Marched = the named exclusion. The engine skipped
  ordinary and built advanced.
- Deliberate counterexample hunt (08-02): NO in-envelope form found
  needing a seventh mode. Strains, named: backdrop blur (samples what
  lies BEHIND — hardest everyday composited case) · selection
  silhouettes (edge-detect or geometric offset) · caret parity in
  shaped text (the two-body law extended to text) · export-to-vector —
  a PROJECTION of material, cheap precisely because Softland is
  material-first (a place the thesis pays a free dividend) ·
  eyedropper (readback; seed exists [CODE]).
- Additions come in three KINDS, not classes: **atom kind** (new
  instanced pipeline in the existing pass — stroke, path-fill,
  image-quad; cheap-by-precedent) · **frame kind** (render-graph
  restructuring — compositor, depth/3D passes, ID-buffers; the
  compositor is the LARGEST structural decision: design early, build
  later, seeded [CODE]) · **contract kind** (registration seam ·
  geometry source-of-truth · draw-order guarantees · budget/lifetime).

## 6. Rules discovered

- **The renderer must never define the material.** The shape contract
  is chosen at the material layer; render roads serve it per lifecycle
  stage. Committing to Slug first would have silently defined shape =
  closed contour (locking out open strokes and networks) by
  implementation accident. Day-one shape contract: open polylines +
  closed contours + holes. The deep sub-question — is ink's material
  truth the CENTERLINE+pressure (the gesture; outline derived at
  render; agent can re-thicken) or the outline polygon (baked)? — is
  thread D2, a design round, not a render measurement.
- **Adopt math, never renderers** [GK: clipper2/pathops, HarfBuzz,
  earcut, glTF loaders adoptable; CanvasKit-as-renderer would replace
  the material model — wrong graft].
- **Code disposable, probes durable** — resolves G-verify vs the
  playground disposability ruling: harnesses may die; receipts and
  pass/fail probes join the golden bank.
- **Chrome is a real demand class** — hairlines at constant
  screen-width, marching selection, gizmos, pickers, guides. The
  screen-flag seam suggests coverage; UNAUDITED as a class [HYP — the
  map's weakest plank].

## 7. Three regimes — verdicts must be regime-tagged

**the hand** (pointer→pixel, ~16ms; no alloc stalls, no GPU→CPU syncs)
· **the settle** (the durable 52ms echo law, receipts attested) · **the
scene** (thousands at rest; culling/batching; provisioned [CODE]). An
encoder costing 8ms is catastrophic at 120 events/s and irrelevant at
1 settle/s — the Slug-vs-tessellation fork DISSOLVES per-regime, which
is what predicts H3. No workshop cut has exercised the settle regime
for new atoms yet (cuts 1–2 were in-memory) — unproven, flagged.

## 8. Coexistence — the novel promise (the per-room frame hid it)

In the products, the three practices never share a canvas; in Softland
there is ONE WORLD — anything makeable anywhere stays renderable,
pickable, annotatable everywhere (ink on a component · a margin note
stuck to a 3D grey-box · an orbitable region sitting between text
blocks). Decomposition: **regions** (the 2D world hosts 3D containers
with their own camera+depth — first-class container kind, picking
routed through, NOT an iframe-punt) · **cross-anchoring** (3D point →
2D label; 2D gesture → ray into a region) · **shared atoms in-region**
(strokes/glyphs given 3D transforms = the grease-pencil convergence).
space-as-entity already anticipates the seam (spaces as identities,
membership as material) — the architecture was pointing here before
this inquiry. Open unknown: one identity across space kinds (a
component placed in the flat world AND staged in a 3D room) —
likely-yes via instances [HYP; nothing 3D exists in code]. The **seam
demo** — one scene: text + ink + live component + 3D region under one
picking and annotation model — is the true "broad enough" bar for
Softland's version of the promise. Unpriced, unprecedented.

## 9. Hypotheses and their killers [HYP]

- **H3 — lifecycle-tiered rendering:** live geometry renders cheap
  (immediate triangulation), settled geometry bakes analytic. Killer:
  E0. External support (support, not proof): all three products
  independently evolved transient-preview-then-commit — tldraw's
  laser/ink, Figma's drag-preview/place, Blender's modal
  grab/confirm — a decade of convergence onto Softland's own
  draft/settle mechanic.
- **Six-modes-suffice** for the declared envelope. Killer: one
  in-envelope counterexample (deliberate hunt survived, 08-02).
- **Region/seam model serves coexistence.** Killer: the seam demo.
- **Chrome covered by screen-flag + overlay patterns.** Killer: a
  chrome walkthrough finding a form with no provider.
- **Motion is render-cheap, grammar-heavy.** Unexplored.
- Retired as commitments: H1 Slug-primary · H2 tessellation-primary —
  both demoted to roads that E0 prices inside H3's frame.

## 10. Convergence notes (practice-derived, 08-02)

- All three practices are editors of persistent visual documents with
  identity — selection, transform, versions, typed reusable parts with
  overrides. That layer is the matter-room; Softland was born there.
- Figma's tokens/components/variants = master/instance/deviation —
  Figma converged on Softland's structure; Softland's version is LIVE
  (a focused state is feelable, not pictured).
- Connectors-as-edges: the arrow that survives movement is an edge
  with identity between identities — discourse-graph shaped; rendering
  trivial, binding is document-model.
- Divergences that STAY divergent: occlusion-by-order (2D) vs
  occlusion-by-distance (3D) — two picture ontologies; shared atoms,
  never one space model · precision cultures (exact / loose / numeric)
  — rooms share atoms, not tool-feel · typography deep in exactly one
  practice · time deep in one and now declared in (§3).

## 11. The thread router

| Thread | Status | Instrument | When |
|---|---|---|---|
| Direction: atoms not architecture | settled | — | — |
| D1 Slug role · fp16 · storage road · H3 | OPEN | machine — **E0** | now |
| D2 shape material contract (centerline vs outline; open+closed+holes) | OPEN | design round + E0 evidence | parallel to E0 |
| D5 meaning-above-engine | OPEN | lived — E1 felt gate = **Sid's paper page redrawn in the ink room** | at E1 |
| Envelope ratification (+ motion + coexistence-now + character leans) | OPEN | **Sid**, one paragraph | at will |
| Chrome class audit | OPEN — weakest plank | walkthrough | pre-E1 or with it |
| Compositor frame-graph | design-early | paper design | at the second atom |
| D3 container rotation · D4 glyph⊂path · D7 binary-assets-as-material · vector networks | parked | tripwires named above | on demand |

## 12. E0 — the falsifier spike (next machine act; chosen 2026-08-02)

Throwaway code · durable receipts (appended here) · one session ·
cheap lane (builder model EXPLICIT — Sonnet) · no product surface · no
server · no code commits. Pre-registered questions, every verdict
regime-tagged:

1. dynamic contour encode+upload cost @ 20 / 200 / 2000 points —
   hand-rate vs settle-rate;
2. fp16 quantization visibility: shape-local normalization, shape
   sizes × the real zoom clamp [0.1, 8.0];
3. the runtime curve/band `writeTexture` update path — works? costs?;
4. range-allocation sketch over buffer_pool patterns (slots→ranges:
   alloc/free/compact);
5. per-instance rotation on ONE pipeline + inverse-transform pick;
6. pick parity: CPU point-in-path vs GPU coverage at boundary pixels —
   one geometry truth, two readers;
7. ink stress: a per-frame-growing outline polygon (H3's sharpest
   test — hostile to band re-encode, friendly to incremental
   triangulation).

**Decision rules, registered BEFORE results:**
- band-encode can't hold hand-rate for a ~50-curve shape → Slug demotes
  to bake-on-settle inside H3; the live tier is triangulated.
- triangulated fills show no visible loss across the shipped zoom
  clamp → the live tier is confirmed cheap; Slug's value narrows to
  settle-tier quality — or retires to text-only if fp16 quantizes
  visibly with no cheap 32-bit road.
- pick-parity failure → NO road proceeds until the geometry
  source-of-truth contract exists.
- the rotation touch-set exceeds one session on one pipeline →
  rotation re-prices as frame-kind, not cross-cutting-cheap.

**What E0 is NOT:** the center of gravity. It prices one road of one
atom on one layer. E1 (the ink room, shaped by E0's data; felt gate =
the paper page redrawn) and the deliberately-dissimilar second atom
(image — which also brings Goal 2's HIG references into the world)
carry G-rhythm's rate evidence.

## 13. Walkthrough ledger (initial, in-thought 08-02; re-run as
receipts when rooms exist)

- **paper page** (thinking-by-drawing): closes at the atom floor
  pending E0; chrome thin.
- **app screen** (interface design): typography + chrome flagged thin;
  tokens/variants land on the matter-room natively.
- **grey-box room** (spatial composition): cleanly frame-kind;
  deferred; no strain found at declared scope.
- **the seam scene**: unpriced, unprecedented — the bar that matters.
