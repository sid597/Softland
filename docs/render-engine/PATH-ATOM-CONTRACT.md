# PATH ATOM — one-pass contract (path / tessellation / ink)

Cut 2026-08-06 by Fable under the one-pass law (`.claude/skills/work-package/SKILL.md`;
decisions.md "How we work"). Two Sid-touches: this cut · the accept. First post-law
contract — do not imitate IMAGE-ATOM/SEAM-STEP1 docs; they predate the law.
**Sid's ruling at cut (boot, 2026-08-06): studio custody stays OPEN; a minimal
on-canvas join ships behind a dev flag so path AND image render live, not dark.**

## Scope — what this atom is

The **tessellated coverage mode made real** (the missing mundane mode, ENGINE.md §5)
as one new tape family `:render.family/path`, on D2=A's material contract:

- **Ink**: centerline + per-point pressure is the authoritative material; the outline
  is a deterministic, algorithm-versioned derivation (W0-C §6.2). Expansion goes
  DIRECTLY to triangles (segment quads + round join/cap fans, fan resolution per
  regime) — never through polygon triangulation. Width law: monotone in pressure
  (base-width × clamped pressure); the exact curve is the implementer's recorded default.
- **Shapes**: the §6 day-one contract exactly — open polylines + closed contours +
  explicit holes. `:line` segments only. Fills triangulate by ear clipping with hole
  bridging, hand-rolled in pure `.cljc` — no JS dependency (keeps the tessellator on
  the JVM tripwire lane that `image_material.cljc` proved).
- **Paint**: solid color + opacity. Coverage is declared ALIASED v1 (raw triangle
  edges) — an honest Contract-G declaration, not a gap to hide.
- Tessellation is an event-driven derivation cached per **(material identity ·
  algorithm-version · zoom regime)** — image-material's cache-key shape. Continuous
  pan/zoom rides in-shader container transforms (vertices are container-local f32;
  motion is a transform value, never a re-tessellation); only regime-band changes
  re-tessellate. Uploads happen on mesh-set change, never unconditionally per frame.
- Full citizenship (W1 M / G-cover): rendered + pickable + grammar-editable +
  versioned. CPU classification is THE geometry truth (point-in-polygon-with-holes;
  ink = distance-to-centerline ≤ half-width + declared slop); GPU coverage is its
  projection, parity-probed outside a declared boundary band (Q6 law; byte-128 tie
  precedent). Declaration data (interior · coverage relation · tie semantics · slop ·
  precision/backend regimes) lives in `path-material`, mirroring W1 Contract G.
- **The live join** (Sid's cut ruling): URL flag `?live-atoms=1` boots the product
  workspace with image-system + path-system constructed and a small fixture corpus —
  a procedural image (bytes generated client-side, registered through
  `register-image-source!`, never a bypass), two ink strokes, one concave shape with
  a hole — injected through the REAL artery: rt-nodes → tree → store → tape →
  pipelines. Flag absent → nil systems, no fixture load, byte-identical product.
  The join decides NOTHING about custody: no authoring, no durable writes; the
  atom's felt GATE (real use, one live variable) still waits behind the
  durable-artery/studio-custody ruling reserved to Sid. This discharges the
  no-third-consecutive-dark-atom corner at zero custody cost.

## Refusals — deliberately not in this atom (each routed, never a void)

- Curve segments (quadratic/cubic) — ink centerlines are pointer-sampled polylines
  already → vector-network slice, Package 2.
- Vector networks · boolean ops · winding fill rules (holes stay explicit) →
  Figma-core floor.
- Path paint beyond solid+opacity (gradients · dashes · image/procedural) →
  paint-axis expansion, Package 2.
- Rotation → Q5 halted road; re-priced frame-kind → W4.
- Settle-tier analytic bake / H3 lifecycle tiering (Q7 left it UNRESOLVED) → E0/H3;
  this atom is the live-tier triangulated road only.
- Scene-regime range allocator (Q4: eager-free is the named dead road) → scene-regime
  work at population pressure; v1 repacks the path lane on change (fixture-scale).
- Analytic AA fringe on tessellated edges → AA road (fringe or settle-bake), LATER.
- Exact self-overlap alpha for translucent ink (direct triangles double-blend at
  self-crossings — declared, falsifier-pinned) → stencil-cover road, LATER.
- Authoring/drawing input + unflagged product activation + the felt GATE → studio
  custody + durable artery, reserved to Sid.

## Laws — pointers, not restatements

- `docs/render-engine/W1.md` — contracts O/G/M/T/C bind the family; G's declaration
  is unconditional; a regime tag records a limit, never waives legal material.
- ENGINE.md §0 gate sentence — the family REGISTERS through the W2-B seam
  (`scene-tape/family-ids` + `default-family-registry` + `frame-family-registry`;
  the load-time equality fence forces the pair); never a hand-positioned central
  branch; the seeded-branch fence self-test stays green.
- decisions.md "The render seam" — proportionality; no execution clock is an
  ancestor of derivation; effects at mutation sites and the frame edge only.
- decisions.md settled architecture — store coords f64 world, GPU buffers
  container-relative f32; f32-visible quantization at extreme extents gets a
  declared regime boundary (Q2's law-shape).
- Contract C color — straight material → tagged linear-premultiplied seam
  (default-OFF) → present; path shaders join `configure-*-color-shader` like image.
- Dark-when-off — the join namespace loads PURE and routes nowhere without the flag
  (dark-lane laws, decisions.md); zoom envelope legal [0.01, 1000], floor-default
  [0.1, 8.0] its own regime; verifier stations 0.01/0.1/1/8/10/100/1000.

## Exact entry points

New namespaces (ALL new code lives here):
- `src/app/client/substrate/path_material.cljc` — grammar + fail-closed validation ·
  Contract-G declaration data · CPU hit truth · cache keys · packing law ·
  corpus-pressure census (mirror `assert-corpus-coverage!`).
- `src/app/client/substrate/path_tessellation.cljc` — stroke expansion · fill
  triangulation · `algorithm-version` · determinism.
- `src/app/client/substrate/webgpu/path_gpu.cljs` — inline WGSL · `init-path-system` ·
  `prepare-path-frame!` · `path-entries` · `execute-path-batch!` ·
  `destroy-path-system!` · gpu-budget registration.
- `src/app/client/workspace/live_atoms.cljs` — flag read · fixture corpus ·
  real-artery injection · system construction (`augment-pipelines`-shaped, so big
  files stay thin).

Thin hooks only (few lines each): `scene_tape.cljc` family entry ·
`renderer.cljs` (require + registry entry + `:path-system` kwarg + one `prepare`
call beside the image one, `renderer.cljs:3131`) · `rect_tree.cljc` `tree->paths`
delegation beside `tree->images` · `scene_store.cljc` `:paths` lane beside
`:images` · `runtime/render.cljs` passes `:image-system`/`:path-system` at the one
`draw-frame!` call site (`:717`) · `electric_flow.cljc` one construction line ·
`test/app/test_runner.clj` registers the two new test namespaces (fail-closed
inventory).

## Decisive scenarios (frozen as tripwires + goldens at close)

1. **Fail-closed + deterministic**: malformed material refuses by name; identical
   (material, version, regime) → byte-identical mesh; version bump changes the
   cache key, never silently the bytes. [JVM]
2. **Ink law**: a pressure-varying centerline yields monotone width with declared
   caps/joins; the outline is a pure function of (centerline, version, regime). [JVM]
3. **Interior truth**: concave contour + hole — CPU classification agrees with
   tessellated coverage outside the declared boundary band; tie semantics named.
   [JVM + verifier pick-parity rows]
4. **Tape citizenship**: path entries paint forward / pick reverse interleaved with
   rect/text/image; 2–3 representative goldens appended (ink · holed concave fill ·
   translucent self-crossing ink) spanning ≥3 zoom stations, one outside the
   floor-default band; determinism 2×; ALL existing goldens byte-identical; the MSDF
   counterexample stays RED. [verifier]
5. **The live join**: `?live-atoms=1` → image + ink + holed shape move together
   under pan/zoom on the real canvas; flag absent → nil systems, no injection —
   the existing golden bank is the byte-identity receipt. [felt receipt, Sid's eyes]

## MUST-NOTs

Never read `src/app/server/env.clj` · no durable/Rama writes (fixtures are
client-session values) · never edit existing goldens/manifest rows or the MSDF RED
case — append only · no hand-positioned central draw branch · no new JS/npm
dependency · `ground.cljs` untouched; `renderer.cljs`/`electric_flow.cljc` thin
hooks only · commits only at Sid's word — code and docs separate, both on
`docs/current-mental-model-local`, exact-path staged, never push.

## Close

Scenarios 1–3 freeze as ~5 tripwires across the two JVM namespaces; scenario 4's
goldens + parity rows join the permanent bank; focused suite = the two namespaces +
`npm run verify:render-engine`. Foreign failures are board debt, never stops. One
NOW entry (≤15 lines, self-audit included) in `PATH-ATOM-NOW.md` + the board line
flip. Acceptance = Sid's word.

## Codex opening prompt

```
You are implementing the PATH ATOM for Softland's render engine — one pass,
whole atom, one session.

Read first, nothing else needed:
1. docs/render-engine/PATH-ATOM-CONTRACT.md   (this contract)
2. docs/render-engine/W1.md                   (contracts O/G/M/T/C your family satisfies)
3. .claude/skills/work-package/SKILL.md       (the one-pass law: build, close, when to ask)

Build the WHOLE atom straight through. Keep your own falsification pass and fix
what it surfaces in-session. Ambiguity → strongest default + a note in
docs/render-engine/PATH-ATOM-NOW.md. A genuine fork (two readings that cannot
both hold) is ONE question in that file — route around it and keep building;
never stop. Foreign test failures are board debt, never stops.

The image atom is your structural precedent AT THE SOURCE LEVEL ONLY —
image_material.cljc, the image half of renderer.cljs, verifier.cljs. Do NOT read
IMAGE-ATOM-CONTRACT.md or any IMAGE-ATOM-* doc: they predate the one-pass law.

Close per SKILL.md: tripwires + goldens frozen from the contract's scenarios,
focused suite green (two JVM namespaces + npm run verify:render-engine), NOW
entry ≤15 lines, board flip. Commits only at Sid's word. Acceptance is Sid's
word — you never wait on a review round.
```
