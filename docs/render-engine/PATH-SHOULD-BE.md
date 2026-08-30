# `path/*` — how things should be

Handoff from the path/tessellation session, 2026-08-30. Purpose: a fresh
session verifies each claim against source (claim → source, evidence-cited,
no recut) and reports which hold; build sessions implement the ruled items.
Labels (A1, B2, …) are anchors for cross-reference only.

**Sid's rulings, 2026-08-30 (verbatim answers to the seven questions):**
`1. once · 2. table · 3. both · 4. yes · 5. no · 6. ????? why do you say code
is it not supposed to be ecs??? · 7. yes` — see §E for what each maps to;
question 6 is OPEN with the primitive/composite split proposed.

**Ground.** `src/app/client/path/` is one story in three stages:
`material.cljc` (17.8KB) says what counts as a valid path and answers
questions about it without drawing; `tessellation.cljc` (18.9KB) turns a path
into triangles; `painter.cljs` (15.2KB) hands triangles to WebGPU.
Dependencies point one way: painter → tessellation → material, never back.
Tests: `test/app/client/path/material_test.clj` (4.3KB),
`tessellation_test.clj` (3.1KB). Law: `docs/render-engine/PATH-ATOM-CONTRACT.md`
(12KB) and `docs/render-engine/W0-C.md` §6 (lines 605–700, 4.3KB).

**Status legend.** *verified* = checked by grep/read in the session ·
*judgment* = reasoned, not measured · *ruled* = Sid's word above.

---

## A. REMOVE (the important list)

### A1. Test census out of the engine floor — *verified · ruled: both kinds*
**Claim.** `claimed-corpus-pressures` and `assert-corpus-coverage!`
(`material.cljc` ~lines 409–420) belong in
`test/app/client/path/material_test.clj`, not in the production namespace.
The identical pattern in `src/app/client/image/material.cljc` moves the same
way, same atom (ruling 3 = both).
**Reasoning.** They assert that the *test fixtures* exercise every frozen
scenario pressure (pressure-width, holes, translucent self-crossing, zoom
extremes). That is a property of the test corpus, not of a path; no
production caller uses them. The engine floor should contain only what the
three tests in B1 justify (per-frame · determinism promise · shared meaning);
a test census is none of those. Every def in the floor is a def a future
reader must understand before trusting the file; this one is noise.
**Verify.** `grep -rn "assert-corpus-coverage!\|claimed-corpus-pressures" src test`
— callers only in `test/` plus the image twin.

### A2. Test-only receipt helpers out of `tessellation.cljc` — *verified*
**Claim.** `point-in-mesh?` and `mesh-bytes` are called only by
`tessellation_test.clj`. Move them to the test namespace — or wire the
verifier's determinism probe to `mesh-bytes` so it earns its place.
**Reasoning.** `mesh-bytes`' docstring says "used by JVM and verifier
determinism receipts"; the verifier (`src/app/client/verifier/core.cljs`)
never calls it. A docstring that overclaims is a small lie in the floor;
make it true or move the function. `point-in-mesh?` is a parity check
(triangles vs. truth) with no production use. Same principle as A1.
**Verify.** `grep -rn "point-in-mesh?\|mesh-bytes" src test` — `src` hits only
inside `tessellation.cljc`. Contrast `quantization-receipt`, which the
verifier does call — that one stays.

### A3. Validate once at the doors, trust inside — *judgment · ruled: once*
**Claim.** `validate-material!` currently opens nearly every public function in
`material.cljc` (`classify`, `boundary-distance`, `material-points`,
`material-cache-key`, `paint-color`, …) and every public entry in
`tessellation.cljc`. It should run once, where a material *enters the world*
(mint, archive load, wire ingress); internals trust the value.
**Reasoning.**
- *Why it is shaped this way today:* with no type system, each public function
  can only promise "I never compute on garbage" by checking for itself. It is
  what "every function is a door" costs — paranoia, not sloppiness — and it has
  one virtue: impossible to hold wrong.
- *Why it is the wrong shape:* the dataflow principle — work belongs where a
  value enters the graph, once per change; the graph's structure then carries
  trust. Re-validating inside every node re-does edge work on every read, and
  in a reactive setting multiplies (a node re-run because an *unrelated* input
  changed re-walks the same unchanged material). "One place of entry" is what
  both the reactive lens and plain engineering say.
- *Honesty about cost:* nobody has profiled this; at fixture scale it is
  probably noise, and "probably" is a magnitude claim with no receipt. **The
  ruling is on shape, not on measured cost.**
- *The refactor:* add `admit-material` — runs `validate-material!`, returns the
  map stamped with metadata `{:material/validated true}`; add `admitted?`;
  internals replace their preamble with an `admitted?` assertion that throws a
  named `ex-info` on an unstamped map. Metadata does not affect equality, so
  content keys and caches are untouched.
- *Adversarial check before freeze — metadata survival:* `assoc`/`update-in`
  preserve metadata; `into`, `mapv`-rebuilt maps, and `canonical-material`'s
  sorted-map rebuild do not. Every place a material is reconstructed
  (`normalized-ink`, `normalized-shape`, `canonical-material`,
  `open-contour-material`) must work from the stamped original or re-stamp.
  A silently lost stamp turns the guarantee into a false alarm.
- *What must survive untouched:* full validation at mint, at archive load, at
  any wire ingress; the fail-closed throw-with-names behavior. Trust applies
  only inside the engine, never where an outside author hands in data.
**Verify.** `grep -n "validate-material!" src/app/client/path/*.clj*` — count
the call sites; confirm each is a preamble; confirm no single ingress function
exists today. Callers to route through the door: test fixtures, the verifier
corpus in `verifier/core.cljs`, `region3d/on_plane.cljc`, and wherever the
painter's path ops originate.

### A4. `hit-slop-screen-px` folds into the regime table — *verified · ruled: table*
**Claim.** It is hardcoded `0.0` and read nowhere outside `material.cljc`;
inside, `classify` computes `slop-local = 0.0 / zoom = 0`, so the slop path is
dead by value. Ruling: move it into `legal-zoom-regimes` as a declared
per-regime limit (`:hit-slop-screen-px 0.0`), and have `classify` read it from
the regime.
**Reasoning.** The contract calls slop a Contract-G declaration ("ink =
distance-to-centerline ≤ half-width + declared slop"). A declaration is
legitimate data; a def that looks like a tuning knob and is a constant zero is
ambiguous. In the table it reads as what it is: a declared limit, zero for
now, next to the other declared limits.
**Verify.** `grep -rn "hit-slop-screen-px" src test` — only its def and its one
use in `classify` today.

---

## B. KEEP as-is (affirmed under falsification)

### B1. The three-stage split, all three at this layer — *judgment, grounded in reads*
**Claim.** `material`, `tessellation`, `painter` must all be compiled engine
code; nothing above them needs to be.
**Reasoning — the litmus test.** A piece of path logic lives at this layer if
it **(a)** runs per frame, **(b)** carries a determinism promise across
runtimes or time, or **(c)** defines meaning that multiple independent
components rely on.
- *painter* — (a)+(c). Its WGSL shader source (`path-vertex-shader`,
  `path-fragment-shader`) and its byte packer (`pack-vertices`, offsets
  0/8/24, 7 floats per vertex) are two compiled halves of one contract that
  must agree byte-for-byte — inexpressible as runtime data; a mismatch does
  not error, it corrupts. It holds a slot in the shared frame
  (`prepare-path-frame!`, `draw-path-range!`). Hottest code in the app.
- *tessellation* — (b)+(c). Its output is promised: same material + same
  `algorithm-version` + same regime → byte-identical mesh, which is what lets
  untouched strokes return their old mesh by identity. The promise dies if the
  algorithm can vary. The same `.cljc` compiles to JVM (where the suite proves
  determinism, ear-clip termination via the fuel bound, quantization bounds)
  and to the browser; the proof transfers because the code is identical.
  Picking uses the same triangles — pluggable tessellation would be pluggable
  click-truth.
- *material* — (c) in its strongest form: the fence that makes everything above
  safe to be data (B2), the shared vocabulary every component reads, and half
  of geometric truth (`classify`, `hit?`).
**The seam.** Above `validate-material!` (after A3: `admit-material`) anyone may
say anything; below it, only this layer says what anything means. Each file
has its lawful way to change: painter edits are engine work, tessellation
changes bump `algorithm-version`, grammar changes bump `schema-version`.
**Verify.** `grep -n "^(def" src/app/client/path/*.clj*`;
`grep -n "shaderLocation\|vertex-words" src/app/client/path/*`;
`grep -rn "app.client.path" src/app/client --include='*.clj*'` — no kind→kind
dependency except `region3d/on_plane`.

### B2. The fail-closed, hand-rolled, code-as-schema validator — *verified pattern · Sid: "the current shape is good"*
**Claim.** Keep the validator's shape: closed maps (unknown keys rejected by
name), `:path/extensions` as the single deliberate open door, no schema
library, data constants + imperative leaf rules, versioned by `schema-version`.
**Reasoning — why this boundary earns strictness** (none depends on language
idiom or anyone's authority):
1. *Agents mint materials.* An unreliable author's only feedback loop is a
   named rejection (`"Ink knot pressure must be finite"`,
   `{:unknown [:sparkles] :policy :reject}`).
2. *The archive means data outlives every implementation.* The grammar is the
   future reader's only contract; strictly specified data survives language
   pivots, code never does.
3. *`material-content-key` fingerprints the whole canonical map.* An unknown key
   riding through would silently fork a path's identity and split its caches.
   Closed maps keep the content key honest.
4. *The GPU never throws.* One NaN renders as invisible garbage;
   `finite-number?` on every coordinate is the only fence.
**Why no schema library.** `clojure.spec`: removes no code, poor errors
without wrappers, global macro registry, loses the exact error shapes.
`malli`: schemas-as-data is closer in spirit, but a dependency on the engine
floor, whose discipline is dependency-free `.cljc` for the determinism lane
(same reason ear clipping is hand-rolled); the interesting rules (unique ids,
holes-require-outer, cross-field conditions) become custom predicates anyway.
The house pattern is uniform: `envelope.clj`'s `required-request-keys` /
`required-event-keys` are the same style; no spec/malli anywhere.
**The tower-of-schemas worry, answered.** Schemas count with *kinds of
persisted truth*, never with layers of composition. Every tower of generators
flattens through the one material fence; a new schema appears only when a
genuinely new noun enters the archive (a vocabulary entry, a relation row).
**Verify.** `grep -rn "malli\|clojure.spec" deps.edn src` → nothing.
`sed -n '10,54p;201,230p' src/app/client/path/material.cljc`.
`grep -n "required-.*-keys" src/app/server/rama/envelope.clj`.

### B3. The grammar's content — knots-as-truth, explicit holes, straight segments, round caps, aliased v1 — *verified provenance*
**Claim.** Each is a recorded decision or a routed refusal, not a gap.
**Reasoning — the provenance chain.** (1) *Envelope* from corpus: W0-C §§1–3
read tldraw source, Figma docs, Blender practice to derive that a day-one
family must express open polylines, closed contours, explicit holes — §6.1,
ratified 2026-08-02. (2) *Truth* by ruling: W0-C §6.2 put the fork to Sid —
A (centerline + pressure is truth; outline is a deterministic, versioned
derivation) vs. B (outline is truth) — with a forbidden non-answer (two
coequal truths). Sid: "Decision 2: yes A" (verbatim, `vision/LOG.md`). That is
why ink is `:knots` with `:pressure` and why tessellation carries
`algorithm-version`. (3) *Common anatomy* from engine law: W0-C §6.3 — id,
revision, provenance, paint, regime-keyed caches — shared with the image atom.
(4) *v1 narrowing* by contract: `PATH-ATOM-CONTRACT.md` "Refusals" routes
curves → Package 2 vector-network slice, gradients/dashes → paint-axis
expansion, winding rules → Figma-core floor, AA fringe → LATER, translucent
self-overlap → stencil-cover road.
**Genuinely untested.** Felt use (the atom's felt gate waits behind studio
custody); v1 tessellates raw knots as-is — no smoothing/resampling pass,
though D2 option A anticipated one; interop with curve+winding formats pays a
conversion tax; branching vector networks are not expressible in `:contours`
(not precluded — `schema-version` + `:path/extensions` — but not native).
**Verify.** `sed -n '605,700p' docs/render-engine/W0-C.md`;
`sed -n '54,72p' docs/render-engine/PATH-ATOM-CONTRACT.md`;
`grep -n "smooth\|resampl" src/app/client/path/tessellation.cljc` → nothing.

### B4. The regime table's declaration fields stay, unread — *verified*
`:coverage-precision`, `:lifecycle`, `:backend`, `:verdict`, `:extent` in
`legal-zoom-regimes` are read by no code; only `:fan-resolution` and
`:regime/id` are consumed. Correct, not dead: Contract G's declared limits,
documentation-as-data. A4 adds `:hit-slop-screen-px` beside them.
**Verify.** `grep -rn ":coverage-precision\|:lifecycle\|:verdict" src/app/client/path` → only the table.

### B5. `derive-mesh-set`, `quantization-receipt`, `boundary-distance` — *verified in use*
`derive-mesh-set` is called by the painter and `region3d/on_plane.cljc`;
`quantization-receipt` and `boundary-distance` by the verifier. All stay.

---

## C. ADD / FIX while here

### C1. Live examples in `material.cljc` — *ruled: yes*
**Claim.** Add `example-ink-material` and `example-shape-material` (concave
outer + one hole + one open contour) as defs beside the schema constants, and
assert in `material_test.clj` that both are admitted.
**Reasoning.** The one real gap in the current shape: the schema can be run but
not read — an agent about to mint, a future importer, the vocabulary library
all have to read Clojure source. An example is the highest-value cover, and it
is complete when paired with the fail-closed errors: copy the example, mutate
it, and every mistake comes back with its name. Example teaches the shape;
rejections teach the edges. In-file, not a new file, because `material.cljc`
*is* the schema. The test assertion turns documentation into schema: an
example validated on every suite run cannot drift. A declarative constraint
table stays unwritten unless real agents demonstrably keep failing in ways the
errors do not explain.

### C2. `mesh-bytes` — make the docstring true or move the function (see A2).

---

## D. HOW THINGS SHOULD BE — above the waist (settled stances, no code now)

### D1. The vocabulary limits, and what goes below vs. above — *judgment*

**What the grammar can say.** Two sentences, and everything is phrased as one
of them. `:ink` — "a band of varying width swept along these points": the
hand-following case, but nothing requires a hand; two knots make a clean line,
a generator can emit perfectly spaced knots along any curve it computes.
`:shape` — "the region enclosed by these boundaries": from a 4-point rectangle
to a 3,000-point coastline with five lakes as holes, plus `:open` polylines
mixed in. Complexity is unlimited in point count; the ear clipper does not
care whether it is a triangle or a floor plan.

**The real limits are on vocabulary, not complexity:**
- *No true curves* — every edge is straight. A smooth circle is always an
  approximation by many short segments; you can make the error invisible, but
  the material records segments, not "circle of radius r." (v1 refusal, routed
  to Package 2.)
- *One flat paint per material* — one color, one opacity. No gradients,
  dashes, textures.
- *One material = one mark, not a picture.* A drawn diagram — box, arrow,
  scribbled annotation — is several materials, each with its own id and
  revision, layered by scene order. No grouping inside the grammar;
  composition lives above it.
- *No semantics* — the map says where ink is, never what it means. "This is a
  rectangle," "this is an arrow pointing at that claim" — that knowledge
  belongs to whoever minted the map. The material is deliberately dumb:
  geometry and paint, nothing else.

**The decision rule:**

> Does the feature change what a pixel's color is, or what geometry *means*?
> Then it is below the waist. Does it only arrange, relate, or parameterize
> marks that already exist? Then it is data on top.

(An ECS framing lands in the same place: a component is only real if some
system interprets it, and the systems here are tessellation and the painter —
"add a component" renames the codify question, it does not dodge it.)

**Each missing item run through the rule:**

- **Curves — must go below, and the reason is zoom.** You can fake a curve
  above the waist today: a generator polygonizes a bézier into 200 segments
  and mints a normal material. It renders fine — at the zoom you chose the
  density for. But the material is zoom-independent while tessellation is
  per-regime: the engine re-derives meshes when you cross a zoom band, and it
  can only add density if the material still carries the curve. A polygonized
  curve has already thrown that information away — zoom in 100× and the facets
  are frozen in the truth itself, unfixable downstream. Editing (dragging
  control points vs. 200 points) and picking (distance-to-curve ≠
  distance-to-segments) both want the curve as truth. So curves are a grammar
  widening — new segment type in `material.cljc`, new expansion in
  `tessellation.cljc`, `schema-version` bump — which is exactly why the
  contract routed them rather than calling them an easy-layer item.
- **Rich paint — below, with one funny exception.** Gradients change the color
  per pixel — fragment-shader work, painter change, below by the rule.
  (Stacking a hundred solid slivers to fake a gradient is technically legal
  data and genuinely awful.) The exception is **dashes**: a dashed line is
  many short ink materials, expressible above the waist right now — clunky (id
  explosion, painful editing) but real. The rule working as intended: dashes
  change *where* ink is, not what a pixel's color computation is, so they ride
  on top until someone wants them done properly.
- **Grouping — purely above, nothing to build below.** A group is scene
  structure: marks as entities, a parent reference as a component, the
  container tree (`engine/placement.cljc`, which already exists) supplying the
  shared transform. Move-the-group = change one affine; the materials never
  know. Scene order's layer/stack key handles group render order. The most
  ECS-shaped item on the list; the engine needs zero new code. Waits on the
  scene-order runtime (W0-C §5.4 is design, not code).
- **Semantics — above, forever, and it is the product.** "This scribble is an
  arrow supporting that claim" is a relation row keyed by
  `:path/material-id` — Softland's discourse-graph territory. The render
  engine is deliberately meaning-free; meaning attaching to mark ids from
  above is not a workaround, it is the architecture. Never sinks below the
  waist. Needs durable mark ids — the "where client shapes get stored"
  decision on Sid's list.
- **The middle case: geometry-to-geometry operations** (booleans, smoothing,
  offset, donut-from-parameters). Pure functions from contours to contours —
  no GPU, no per-frame — so by the rule they can live above the waist as
  libraries whose output is minted as ordinary materials. The catch is the
  tessellator's lesson: if such an operation's output becomes durable truth,
  it needs the same discipline the engine gives itself — pinned, versioned,
  deterministic — or you get silently drifting geometry. They live above, but
  as versioned easy-layer code, never ad-hoc. They sink below only if the
  engine ever had to re-run them per regime or per frame.

**Three piles.** *Grammar widenings* (curves, gradients, winding rules —
below, each a versioned decision, on demand) · *pure composition* (grouping,
semantics, dashes-as-marks, shape generators — above, buildable when their
prerequisites exist) · *versioned derivation libraries* (booleans, smoothers —
above, held to below-the-waist discipline). The floor grows only when a
feature needs the tessellator's zoom-awareness or the painter's shader;
everything else is arrangement of what the floor already says.

**Verify.** `sed -n '1,14p' src/app/client/engine/placement.cljc`;
`grep -n "fan-resolution\|zoom-regime" src/app/client/path/tessellation.cljc`
(density is chosen per regime from the material — the curves argument); the
contract's refusals list.

### D2. Complexity shifts to data — and that is the good trade — *judgment*

**The worry.** Layers of generators composing into layers: does this not just
move complexity to the data side and make the system un-navigable?

**The correction: it is not a tower, it is a fan with a flattening fence.**
Whatever height of composition produced a shape, what crosses the fence is
always a flat, self-contained map. A flowchart generator may call a box
generator that calls a rectangle generator — three layers of scaffolding — but
the system receives N materials, each complete in itself: points, paint, id.
The runtime truth is never layered — a flat list of self-describing values
plus a scene order. Navigating *what exists* never requires navigating *how it
was made*. That is the structural difference from a deep component hierarchy,
where the layers are alive at runtime.

**Complexity is conserved; its species changes.** The shift is from
*heterogeneous code complexity* (many interacting abstractions — understanding
one requires understanding five others) to *homogeneous data complexity* (many
instances of one simple schema). A hundred thousand materials is bulk, but
bulk of one shape — every element inspectable alone, diffable, deletable,
archivable — and Softland's substrate (ids, revisions, content keys, Rama, the
EDN archive) is purpose-built for exactly that kind. Uniform bulk scales with
tooling; interlocking abstraction does not.

**Two bounded residuals.** *Bulk cost* — a generated diagram with 400 marks is
400 materials; fine for the engine, but the tooling above needs bulk verbs
(select-by-origin, delete-the-batch, re-run-the-recipe). *Orphaned intent* — a
recipe whose generator's meaning drifted; hence versioned generators (D3).
Neither pushes anything below the waist.

### D3. Two persistences: instances flatten, the vocabulary persists at every level — *judgment; corrected by Sid's pushback*

**First, the mechanical bit.** Nothing gets redone on load. Materials are
durable data — on load you read the flat maps back and render them;
generators never re-run at render or load time. They re-run only on edit
("make the hole bigger") or on stamping a new instance.

**Per-instance construction** — "this flowchart was made by flowchart-gen
calling box-gen calling rect-gen, in that order, with these intermediate
values." This should evaporate: it is replay history, and persisting it per
mark is the spelunking-tower that makes systems un-navigable. Nobody builds on
the shoulders of a call stack.

**The vocabulary itself** — the definitions: rectangle-gen, box-gen,
flowchart-gen, each a named, versioned, durable thing. **This must persist, at
every level of the tower.** "Persist top and bottom, never the middle" was
about what an *instance* records; the *library* keeps every rung. The
box-generator does not survive because some flowchart remembers calling it —
it survives because it is an entry in the durable vocabulary, referenceable by
name, with its own version and history.

This is the move code and mathematics make: a program's output does not record
every call that produced it, but the function definitions persist in source —
and that is what people build on. A theorem's proof is written once; everyone
after cites the theorem. **Shoulders are made of named definitions, not
preserved executions.**

**The emergent loop, concretely.** Someone (Sid, an agent) explores and
composes something that works → making it permanent is *naming it into the
library* — the composite becomes a vocabulary entry, `flowchart-gen@v1`,
itself defined in terms of other named entries → from then on anything can
mint instances by calling it, and further composites can be defined on top.
Each instance stays cheap: flat points plus one thin pointer —
`{:generator :flowchart-gen :version 1 :params {...}}` — into the library.
The tower persists once, as definitions; instances carry a name, never the
tower.

**Two consequences of taking this seriously.** (1) The library becomes real
infrastructure, not a convenience: named, versioned, deprecable, with the
tessellator's discipline — a vocabulary entry whose meaning drifts silently
corrupts every instance pointing at it. Governance work; the fair price of
shoulders. (2) It is Softland-shaped all the way down: a vocabulary entry is
itself material — identity, revision, provenance, relations to the entries it
composes. Discovery → naming → settlement into durable, buildable-upon
material is the settlement thesis applied to the drawing vocabulary itself.
The emergent system is not a side effect of the shape library; it *is* the
shape library, governed like everything else Softland keeps.

### D4. Where recipes live — *OPEN (Sid's question 6)*

The ECS split: *components* (data on an entity — "this mark is a rectangle,
200×100") are data; *systems* (the code that turns a component into points)
are code, in every ECS. Recipes come in two kinds and the answer differs:

- **Primitive recipes** (few, stable, need actual math: rectangle, ellipse,
  arrow head, donut) → **code** in the repo — versioned `.cljc` functions with
  the registry entry as data — and ingested through the existing
  `clojure_adapter` so each is also visible inside Softland as material.
- **Composite recipes** (many, emergent, discovered by Sid or an agent:
  "flowchart = box ×3 here, arrow ×2 there") → **data** inside Softland — a
  named list of which primitives, which parameters, placed where; interpreted
  by one generic compose system. Mintable at runtime, no deploy, versioned
  like any other Softland material. Softland never executes stored code.

Proposed as the answer to question 6; awaiting Sid's word.

### D5. Parameters are to points what centerline is to outline — *judgment*
The D2 ruling generalized one level up: the parametric description
(`{:generator … :params …}`) is the material truth for a generated shape; the
contours are the derivation. Re-edit = re-run the generator, mint a new
revision. The schema already leaves room (`:path/provenance`
required-but-unchecked, `:path/extensions` optional); nothing implements it
yet, and no generators, library, or template store exist in the repo today —
the only minters are test fixtures and the verifier's corpus.

---

## E. Sid's rulings mapped (2026-08-30)

| Q | Plain question | Answer | Maps to |
|---|---|---|---|
| 1 | Check validity once at entry, or leave as-is until measured? | **once** | A3 |
| 2 | Click-tolerance setting: into the limits table, or delete? | **table** | A4 |
| 3 | Same cleanup in the image code, same batch? | **both** | A1/A2 scope |
| 4 | Add the two example shapes? | **yes** | C1 |
| 5 | Save the "discovery … permanent" sentence to `vision/LOG.md`? | **no** | — |
| 6 | Where do shape recipes live? | **open** | D4 |
| 7 | Save this document into the repo? | **yes** | this file |

Still on Sid's list from the path-atom close, needed before the library (D3)
can be built: **drawing input** (studio custody) and **where client shapes are
stored durably** (the artery).

## F. The work, sized

| # | What | Size (est.) | Prerequisites |
|---|---|---|---|
| 1 | Hygiene atom — A1, A2, A4, C1, C2, both kinds | ~100 lines, half a session | none |
| 2 | Ingress atom — A3 | ~1 session | none (ruled) |
| 3 | Vocabulary library package — D3/D4/D5 | 1 contract + 3–5 atoms | question 6 · drawing input · durable storage |
| 4 | Grouping | package | scene-order runtime |
| 5 | Semantics | mostly exists | durable mark ids |
| — | Curves / gradients / winding | on demand | each its own contract |
