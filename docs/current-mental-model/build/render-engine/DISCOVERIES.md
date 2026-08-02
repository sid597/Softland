# RENDER-ENGINE — DISCOVERIES (the derivation record)

**What this is:** the full record of the 2026-08-02 engine inquiry — the
reasoning chain, each discovery with the argument that produced it, the
self-corrections with their mechanisms, the beliefs that died and why,
and the portable methods. `ENGINE.md` (same dir) is the operational map:
what to act on. THIS doc is why the map says what it says. A future
session that wants to challenge, extend, or re-derive the map starts
here; a future session that wants to act starts there.

**How to read from a cold POV:** §1 is the spine — seven rounds, each
recorded as *pressure → move → what died → what was born → evidence*.
§2 holds each discovery's full argument (the spine references them as
D-nn). §3 is the corrections ledger — where the inquiry caught itself,
with the catch-mechanism named, because the catches are as reusable as
the finds. §4 is the superseded-beliefs table. §5 is the method
register. Sid's verbatim words that steered the turns live in
`vision/LOG.md` (2026-08-01 and 2026-08-02 entries); load-bearing
fragments are quoted inline where they turned the derivation.

---

## 0. The evidence the inquiry started from

- **G7, the arc's first lived gate, FAILED direction-class**
  (2026-08-01): "This is not my idea of a workshop." Contamination of
  the notes space · verbs-before-meaning · invisible subject. Record:
  `build/studio/G7.md`. The process finding that survived: *meaning is
  not falsifiable in written claims — every machine gate was green; the
  variable that moved the outcome was time between judge touches.*
- **The two-atom inventory** (2026-08-01): the belief that everything
  visible in Softland is SDF rounded-rects + MSDF glyphs, and that the
  gap to any authoring tool is unbuilt atoms, never architecture.
  Banked with three tiers (current / tldraw / figma), unpriced by rule.
- **Playground cut 1** (2026-08-01): first positive lived reaction of
  the whole arc ("much much useful … good iterative job"); the wall
  located precisely at structure verbs.
- **Playground cut 2** — the structure pencil (Codex, 2026-08-02):
  landed flat. Sid: *"now i can draw rectangles within rectangles and
  they are all bounded to the block and move as one not sure what else
  apart from it .... like is this all goineg somewhere???"* — the
  priced evidence that Tier-I fluency (structure verbs over two atoms)
  bottoms out in minutes. Meaning-per-stroke is capped by the atom
  vocabulary.
- **The ask** that opened the inquiry: *"I THINK THE ROOT CAUSE OF ALL
  PROBLEMS IS TRYING TO BUILD OUT stuff for which this is not made …
  guide me through the rendering engine that is need to provide
  meaning, a engine in which i can do both tldraw stuff, figma stuff
  and blender type of stuff as well so that what remains is composition
  on top and never the missing render."*

---

## 1. The derivation chain — seven rounds

### R1 — the ask and the first map
**Pressure:** the ask above, plus "don't talk out of this."
**Move:** verify the renderer at the current tree before mapping.
**Born:** the Slug discovery (D-01) — the renderer has FIVE pipelines,
not the believed three, and one is a live GPU quadratic-bezier
evaluator currently pointed only at glyphs. First map: one scene
substrate + ~six atom families + cross-cutting upgrades + an
adopt-vs-build graft rule + a felt-landing ladder (E1 ink&shape → E2
image → E3 vector kit → E4 mesh). Reading of cut-2: the boredom is the
vocabulary ceiling, so the engine completes the design rather than
pivoting it.
**Died:** "two atoms is the whole inventory."
**Evidence:** renderer.cljs read — pipelines at 606/787/846/1028/1146;
slug shader 375–514; no depth; no MSAA; analytic AA throughout.

### R2 — the completeness question and the seven classes
**Pressure:** Sid raised the altitude: *"What must ultimately exist
before Softland can honestly promise that no tldraw-, Figma-, or
Blender-class workshop will ever be blocked by a missing rendering
class?"*
**Move:** decompose the promise into requirements; enumerate a basis.
**Born:** the seven-class map (analytic primitives · path · glyph ·
image · mesh+3D · compositor · extension contract) + cross-cutting
preconditions; the group-opacity argument that forces the compositor
into existence (D-03's seed); the first closure sentence.
**Died:** nothing yet — this round's product would die next round.
**Evidence:** cross-checked against engine taxonomies [GK].

### R3 — the falsification round
**Pressure:** Sid's interrogation: examine the promise itself; is
extensibility equivalent to coverage?; does E1 follow from evidence or
from an attractive path through current code?
**Move:** audit own reasoning, trace provenance of each claim.
**Born:** the two-guarantee split (D-02) — inventory (A) vs cost-shape
(B), and the finding that the closure sentence was "B wearing A's
clothes"; B's own split into atom-tier vs frame-tier; the volumes tell
(D-04) — the escape hatch had hidden an envelope exclusion, so closure
is only honest relative to a DECLARED envelope; the three-kind taxonomy
(atom/frame/contract, D-03) replacing seven flat classes; the E1
attractor audit (C-1) — the recommendation had formed around the Slug
find, with the zoom argument produced post-hoc AND resting on a false
premise (C-2: the shipped zoom clamp is [0.1, 8.0], not infinite); the
ink discovery — pressure ink is a FILL problem (an outline polygon
regenerated every frame), hostile to band-encoding, friendly to
incremental triangulation — which cracked "SDF strokes + Slug fills"
into three hypotheses H1/H2/H3 and birthed lifecycle-tiering (D-07);
the E0 falsifier with pre-registered questions; the budget separation
(pointer-to-pixel vs the durable 52ms settle law) and the honest flag
that no playground cut had ever exercised the settle regime.
**Died:** the seven-class frame · E1-as-committed · Slug-as-committed ·
the first closure sentence · the infinite-zoom premise.
**Evidence:** the zoom clamp from P0's S6 bank; the shape of the
reasoning itself (provenance tracing).

### R4 — the continuation (mechanics verified, basis re-derived)
**Pressure:** Sid pasted the thinking tokens back: "continue the line."
**Move:** run the code verifications the line was about to make; finish
the re-derivation.
**Born:** the four guarantees (D-06: cover / extend / rhythm / verify —
rhythm provable only as a RATE of ≥2 dissimilar additions; verify as a
standing instrument cheaper than lived failure); the coverage×paint
factorization (D-05) — the right question is "given a pixel, what math
decides its coverage," six modes, paint orthogonal, forms are
combinations; the mechanical verdicts (D-10): slug curve/band textures
are COPY_DST + writeTexture-fed (possibility settled, cost open), the
**fp16 risk found only by reading formats** (curve texture is
rgba16float — half-float control points; glyphs survive in em-space,
arbitrary shapes may visibly quantize), `buffer_pool.cljs` found (a
slot-based differential pool — instance-tier heap exists; range
allocation does not), the storage-buffer hypothesis KILLED (C-3: no
storage buffers exist; containers ride a 1024-cap uniform of
[x y scale flag] — rotation absent at the byte level), a persistent
offscreen render target with COPY_SRC found (composited-mode seed), no
depth attachment confirmed, the 29-word rect instance = the real paint
axis, scale provisioning read (1M text / 50k rects), and the manifest
truth: Slug is selectable (`preferredBackend: "slug"` on DejaVu;
fonts.cljs:101-104) but the BOOT DEFAULT is MSDF. Also born: the third
regime (scene steady-state) completing D-09, and the data-model guard
(D-08): committing to Slug first would have silently defined shape =
closed contour — the renderer must never define the material.
**Died:** "storage-buffer precedent exists" · "Slug is the live
default" (precised) · the two-budget frame (became three).
**Evidence:** renderer.cljs 556–744, 1140–1214; buffer_pool.cljs;
fonts/manifest.json; runtime/fonts.cljs.

### R5 — the router (convergent vs divergent threads)
**Pressure:** "combining your previous line and this line — convergent
and divergent threads that may or may not need exploration."
**Move:** cross the two derivations; attach every conclusion to the
instrument that could kill it.
**Born:** seven convergences (direction; H3 twice-derived; compositor
design-early; envelope = Sid's; materiality; regime-tagging;
E0-before-basis + rate) and seven open threads with instruments (Slug
role/fp16/storage → E0 · shape contract, centerline-vs-outline → design
round · rotation scope → tripwire · glyph⊂path → guard line ·
meaning-above-engine → E1's felt gate proposed as **Sid's paper page
redrawn in the ink room** · verify-vs-throwaway → code disposable,
probes durable · binary-assets-as-material → note before the image
atom). The epistemic rule of the round (M-3): derivation-convergence is
not evidence — two frames predicting H3 is still zero measurements.
**Died:** nothing; this round organized.

### R6 — the deep inquiry (the promise itself re-derived)
**Pressure:** Sid: return to the original promise; interrogate "class,"
"workshop," "rendering capability," "blocked"; sort assumptions by
provenance (ambition / current renderer / desire for neatness); let the
map dissolve; carry everything as far as possible before naming
anything his.
**Move:** practice-eye rather than renderer-eye (M-1); walk the three
practices as a user; hunt counterexamples deliberately.
**Born:** practices-not-products (D-11) and the confession that
converting practices to product envelopes was finitization (C-5); the
FOURTH practice — self-hosted world-making, Smalltalk/HyperCard
ancestry — as the true referent, with the Smalltalk sentence: *the
world can render whatever it needs in order to redesign itself, at
every layer, without leaving itself*; the four grades of blocked
(D-12: absent / degraded / slow / illegible — grade 4 is what killed
G7 and no engine work touches it); the four-layer promise (D-13: atom
floor closes · tool-math adoptable-unbounded · composition open-ended ·
seams novel) with the confession that every earlier map let the
closable bottom carry the whole sentence's emotional weight (C-6);
tool-chrome as an unaudited demand class (D-16); **coexistence as the
novel promise** (D-14) — one world, no product ever shared a canvas;
regions + cross-anchoring + shared atoms; space-as-entity already
anticipates the seam; the surviving unknown = one identity across space
kinds; **motion caught as a pure omission** (D-15, C-7) — excluded by
nobody's decision, and arguably MORE native to Softland than to Figma
because live components actually transition; the practice convergences
and irreducible divergences (D-17), crowned by the strongest external
validation of the whole inquiry: all three products independently
evolved transient-preview-then-commit — Softland's own draft/settle —
and the irreducible split of occlusion-by-order vs
occlusion-by-distance (two picture ontologies; shared atoms, never one
space model); the exclusion audit by provenance; convincers (walkthrough
test · the rate · the seam demo) and breakers (a seventh in-envelope
mode — hunt survived; a seam failure at the identity layer — dissolves
into wear-vocabulary extension via space-as-entity, likely).
**Died:** the per-room framing's sufficiency · "engine closure = the
promise" (demoted to necessary fragment) · E0's centrality (still
first, no longer the center of gravity).
**Evidence:** in-thought walkthroughs; every code fact from R1/R4
re-used; no new reads (deliberately — this was a thinking round).

### R7 — settlement
**Pressure:** Sid's stop-word: *"strongest discovery artifact we have
now … Stop the inquiry loop. Preserve what this inquiry genuinely
discovered, mark the unproven claims as provisional rather than
canonical, and use the result to form the working engine map and choose
the next evidence-producing step."*
**Move:** distill into `ENGINE.md` with epistemic marks
([CODE]/[GK]/[HYP]/[SID]); advance custody (NOW.md · board · LOG);
choose E0. Commit f787a9b.
**Born:** the working map; this record.

---

## 2. The discovery catalog (arguments in full)

**D-01 · The Slug find.** The renderer ships a complete implementation
of banded-curve GPU glyph rendering: a curve texture holding quadratic
control points, a band texture as an acceleration structure, and a
fragment shader (renderer.cljs:375-514) that root-solves the curves per
pixel for analytic, resolution-independent coverage — nonzero winding
by construction (every rendered "o" is a multi-contour path with a
hole). Status precisely: shipping, end-to-end, runtime-switchable, boot
default MSDF. Why it matters: the hardest single piece of figma-class
2D — analytic filled curve shapes — has working physics in the file; to
this pipeline a user-drawn shape is a glyph with different provenance.
Why it is NOT a commitment: see C-1, D-08, and the fp16 risk in D-10.

**D-02 · Two guarantees, not one.** "Every needed rendering class
exists" (A: inventory, falsifiable only against a declared envelope)
and "whatever is missing can be added without rewrite" (B: cost-shape)
are different promises; an engine can satisfy either without the other.
The first closure sentence asserted A's comfort on B's grounds. B
itself is not yet a contract in this codebase — five pipelines were
each hand-wired; extension is precedent. And B has tiers: atom
additions (a sixth pipeline in the existing pass — cheap by precedent)
vs frame additions (compositor, depth passes — render-graph
restructuring, structural events).

**D-03 · Three kinds, not N classes.** Flat class-lists mix things
whose cost shapes differ by an order: **atom kind** (instanced
pipelines), **frame kind** (render-graph structure — where the
compositor lives, the largest single decision, to be *designed* early
and *built* later; forced into existence by group opacity: two
overlapping 50%-opaque siblings darken at their intersection, a
50%-opaque *group* must not, which is only achievable by compositing
the group offscreen), and **contract kind** (registration seam ·
geometry source-of-truth · draw-order · budget/lifetime — what makes
additions material rather than merely drawn).

**D-04 · The declared envelope.** The volumes case is the tell: filing
a known Blender form under "extension" hid an envelope exclusion inside
an escape hatch — the basis was only ever closed relative to a boundary
never stated. An honest promise carries its envelope explicitly, with
per-exclusion provenance: intent-discovered (path-tracing, multiplayer,
plugins) vs introduced-by-the-mapmaker (vector networks — tripwire
attached) vs mixed (volumes — plausibly outside the practice AND
conveniently expensive) vs omitted-by-accident (motion, D-15).

**D-05 · Coverage × paint.** "What forms exist" is unbounded — any
form-list feels closed until the next want. The closable question is:
*given a pixel, what math decides its coverage?* Six evaluation modes
span shipped raster graphics: implicit-analytic (SDF) ·
boundary-analytic (curve-solving) · tessellated · sampled · marched ·
composited. Paint (solid/gradient/image/procedural/lit) is orthogonal;
every visual form is a combination. Consequences: new paints are
fragment-local and cheap; new coverage modes are the structural events,
and there are ~six in the world. Softland already runs three (implicit,
boundary, sampled) — what's missing is the MUNDANE (tessellated,
composited), not the exotic. The deliberate counterexample hunt found
no in-envelope form needing a seventh mode; the strains it did find
(backdrop blur, silhouettes, caret parity, export, eyedropper) all land
in composited-mode or in layers above the atoms.

**D-06 · The four guarantees.** G-cover (envelope spanned as MATERIAL:
rendered + pickable + grammar-editable + versioned — rendering alone is
a screenshot) · G-extend (additions bounded/local through a contracted
seam) · G-rhythm (session-scale, provable only as a rate — one addition
is an anecdote; the second should be deliberately dissimilar, because
dissimilarity is what surfaces hidden single-pass assumptions) ·
G-verify (standing machine probes + short-cadence lived contact — a
promise verified only by discovering failure in Sid's hands is the G7
pattern again).

**D-07 · Lifecycle-tiered rendering (H3).** Derived independently
twice: from the ink workload (per-frame outline regrowth is hostile to
band re-encoding, friendly to incremental triangulation) and from the
regime split (an 8ms encoder is catastrophic at 120 pointer events/s,
irrelevant at 1 settle/s). Live geometry renders cheap; settled
geometry bakes analytic. External support, not proof: tldraw
(laser/ink), Figma (drag-preview/place), Blender (modal grab/confirm)
independently evolved transient-preview-then-commit — a decade of
product evolution converging on Softland's own draft/settle law.
Killer: E0. If H3 survives, the render basis is shaped like Softland
itself — physics following the material's life stage.

**D-08 · The renderer must never define the material.** The near-miss:
committing to Slug first would have silently defined "shape" = closed
contour in the material grammar, locking out open strokes and vector
networks by implementation accident — an implementation choice
dictating the permanent data model. Rule: the shape contract is chosen
at the material layer (day one: open polylines + closed contours +
holes) and render roads serve it per lifecycle stage. The deep open
sub-question (thread D2): is ink's material truth the
centerline+pressure (the gesture — outline derived at render; an agent
can re-thicken; provenance preserved) or the baked outline? This is
G7's two-body lesson pushed to the engine floor: the visible thing and
the true thing must be one thing, and the true thing is defined above
the pixels.

**D-09 · Three regimes.** the hand (pointer→pixel, ~16ms, no alloc
stalls, no GPU↔CPU syncs) · the settle (the durable 52ms echo law,
attested) · the scene (thousands at rest; culling/batching; the boot
capacities — 1M text, 50k rects — show the engine already assumes this
regime). Any performance verdict not tagged with its regime is
meaningless; the Slug-vs-tessellation fork dissolves per-regime, which
is exactly what predicts H3. Flagged honestly: no workshop cut has
exercised the settle regime for any new atom.

**D-10 · What reading the mechanics bought.** Possibility questions
became cost questions (COPY_DST + writeTexture settled updatability);
one NEW risk emerged that no amount of analysis would have found — the
fp16 curve texture (half-float control points; ~1/2048 relative
precision; glyphs survive in em-space, large arbitrary shapes may
visibly snap at zoom; E0 fork: accept / 32-bit variant / tessellated
fills); one hypothesis died cleanly (no storage buffers anywhere —
containers are a 1024-cap uniform, [x y scale flag], no rotation at the
byte level); two assets appeared (buffer_pool's slot-differential heap;
the persistent offscreen target with COPY_SRC — render-to-texture and
readback are seeds, not fantasies).

**D-11 · Practices, not products — and the fourth practice.** The
promise's referents are practices (thinking-by-drawing · interface
design · spatial composition); products are how Sid points at them.
Copying products replicates snapshots; serving practices means the
practice as it would naturally express itself in Softland's world.
Beneath all three: the fourth practice, self-hosted world-making —
designing Softland's components inside Softland — ancestor Smalltalk
morphic / HyperCard, which no modern product is. The Smalltalk
sentence is the promise's truest form; the three products are its
shadows on three walls.

**D-12 · Blocked is four-graded.** absent (existence proofs) · degraded
(golden receipts — fp16 snapping, unshaped type) · slow (regime
benchmarks) · illegible (lived contact — the grade that killed G7:
everything rendered, nothing could be understood or aimed). A promise
meaning only grade 1 while saying "never blocked" is the overpromise
shape wearing rigor.

**D-13 · The four-layer promise.** Atom floor — closes (six modes) ·
tool-math — adoptable-unbounded (shaping, booleans, tessellation:
never architectural, never "done") · composition/practice — open-ended
(meaning lives here; G7-class failure stays possible forever) · seams —
novel (D-14). Layer confusion was the root of every earlier
overpromise: the closable bottom kept borrowing the whole sentence's
emotional weight.

**D-14 · Coexistence — the novel promise.** In the products the three
practices never share a canvas; in Softland there is one world —
anything makeable anywhere stays renderable, pickable, annotatable
everywhere. Decomposition: regions (3D containers with own
camera+depth as first-class citizens, picking routed through — NOT an
iframe punt) · cross-anchoring (3D point → 2D label; 2D gesture → ray)
· shared atoms in-region (strokes/glyphs in 3D = the grease-pencil
convergence). space-as-entity anticipated the seam before this inquiry
existed — spaces as identities, membership as material. Surviving
unknown: one identity across space kinds (likely-yes via instances;
nothing 3D in code). The seam demo — text + ink + live component + 3D
region under one picking model — is the true "broad enough" bar, and
no per-room success implies it.

**D-15 · Motion — the caught omission.** Never excluded by anyone's
decision; simply never considered, across every map. Interface-design
practice includes motion as core literacy; Softland's live components
make motion MORE native than Figma's prototype fiction — states
actually transition. Render cost small (parameter interpolation over an
already-per-frame loop); the real cost is material-model: a transition
as material, a spring as a claim, time in the grammar. Declared into
the envelope by carried lean [SID], staged late.

**D-16 · Chrome — the unaudited class.** Hairlines at constant
screen-width across all zooms, marching selection, gizmos, pickers,
guides, measurement flags. Tool-chrome demands are sometimes harder
than artifact demands, and the map never audited them as a class — the
renderer-eye taxonomy could not see them. The screen-flag container
seam suggests coverage; unverified. The map's weakest plank, named.

**D-17 · Practice convergences and irreducible divergences.**
Converge: editors of persistent visual documents with identity (the
matter-room's home turf) · direct manipulation at fluency · typed
reusable parts with overrides (Figma's tokens/components =
master/instance/deviation — Figma converged on Softland's structure;
Softland's is live) · overlay-chrome vocabularies · camera-as-feel ·
transient-preview-then-commit (see D-07) · connectors-as-edges (the
arrow that survives movement is an edge with identity between
identities — discourse-graph shaped; rendering trivial, binding is
document-model). Diverge irreducibly: occlusion-by-order (2D) vs
occlusion-by-distance (3D) — two ontologies of picture; shared atoms,
never one space model · precision cultures (exact / loose / numeric) —
rooms share atoms, not tool-feel · typography deep in exactly one
practice · time deep in one (now declared in). Bonus dividend found on
the way: export-to-vector is a PROJECTION of material — material-first
architecture makes SVG/PDF export cheap, a place the thesis pays for
itself unexpectedly.

---

## 3. The corrections ledger (self-catches, with mechanism)

- **C-1 · Attractor-following on Slug.** The E1 recommendation formed
  around the most interesting existing code; the zoom argument was
  produced afterward as support. Caught by: provenance-tracing the
  recommendation under Sid's "does E1 follow from evidence?" prompt.
  (The named generator from memory: attractor-following — live fire,
  recovered.)
- **C-2 · The infinite-zoom premise.** "Analytic wins under infinite
  zoom" leaned on an idealized Softland; the shipped clamp is
  [0.1, 8.0] (~80×), within which tessellation at fine tolerance may
  never visibly facet. Caught by: checking the argument's premise
  against banked code truth (P0 S6).
- **C-3 · Storage-buffer precedent.** Hypothesized as existing;
  killed — no storage buffers anywhere in the renderer. Caught by:
  grep before claim.
- **C-4 · The closure sentence.** "Blocked-below becomes impossible"
  asserted inventory-comfort (A) on extensibility grounds (B). Caught
  by: Sid's equivalence question.
- **C-5 · Practices→products finitization.** Converting open practices
  into enumerable product envelopes because features close and
  practices don't. Confessed in R6; the promise re-aimed.
- **C-6 · The rendering=atoms boundary.** Drawing "rendering
  capability" at the GPU vocabulary because that's the part that
  closes — engineering-true, promise-false (unkerned text IS
  experienced as bad rendering). Confessed in R6; replaced by the
  four-layer promise.
- **C-7 · Motion.** Excluded by pure omission — no decision anywhere.
  Caught only when the exclusion audit asked *"was each exclusion
  discovered from intent or introduced to close the map?"* and one
  answered: neither.
- **C-8 · (inherited exemplar) The "teleporting resize."** Pre-session:
  a lived symptom (rectangles appearing at the drag site) was almost
  routed to builders as a code diagnosis; the durable resize writes
  only :w/:h — the symptom was invisible selection + a transient
  gesture ghost. Kept here as the standing exemplar: ship the symptom
  to builders, never an unverified diagnosis.

---

## 4. Superseded beliefs (was → is → why → what would reopen)

| Was believed | Is now | Why it changed | Reopens if |
|---|---|---|---|
| Two atoms are the whole inventory | Five pipelines; three of six coverage modes live | R1 code read (Slug find) | — (code fact) |
| Seven classes = the complete basis | coverage×paint modes + three addition-kinds | classes mixed kinds; form-lists never close | a seventh in-envelope mode appears |
| Closure is provable by enumeration | A-within-declared-envelope + B-beyond, B tiered | the volumes tell; C-4 | — (structural) |
| Slug is the committed fill road | H1/H2 retired as commitments; H3 leads, E0 prices | C-1, C-2, ink-is-a-fill-problem, fp16 | E0 results |
| "Infinite zoom" justifies analytic | zoom clamp [0.1, 8.0] is current truth | C-2 | Sid declares deeper zoom into the envelope |
| Storage-buffer precedent exists | none in file; containers = 1024-cap uniform | C-3 grep | — (code fact) |
| Slug is the live default text path | selectable; boot default MSDF | manifest + fonts.cljs read | manifest default changes |
| Engine closure = the promise | necessary fragment; four-layer promise above it | R6; C-6 | — (structural) |
| Per-room workshops frame the demand | one world; coexistence is the novel promise | R6 (the per-room frame hid it) | Sid rules coexistence out (unlikely — it's the differentiator) |
| E0 is the center of gravity | first machine act; prices one road of one atom | R6 demotion | — |
| Blocked = capability absent | four grades, four verifiers | G7+cut-2 evidence read honestly | — |

---

## 5. The method register (portable — how the discoveries were made)

- **M-1 · Practice-eye vs renderer-eye.** Renderer-outward taxonomies
  systematically hide what only practice-inward looking sees: chrome,
  seams, motion, typography's real weight. Walk the practice as a
  user; trace every visible thing INCLUDING the tools.
- **M-2 · Read the formats.** Mechanical code-reading finds risks
  analysis cannot (fp16), kills hypotheses cleanly (storage buffers),
  and finds assets nobody remembered (buffer_pool, the offscreen
  target, Slug itself). Possibility questions become cost questions.
- **M-3 · Derivation-convergence ≠ evidence.** Two frames predicting
  the same conclusion (H3) is zero measurements — and the moment a
  conclusion feels inevitable is peak coherence-preservation risk.
  Keep every conclusion attached to the instrument that could kill it
  (the router's whole function).
- **M-4 · Pre-register decision rules.** E0's verdict rules were
  written before results exist, so the results cannot be argued into
  the preferred road afterward.
- **M-5 · Code disposable, probes durable.** Resolves standing
  verification (G-verify) against the playground disposability ruling:
  harnesses die, receipts and pass/fail probes join the golden bank.
- **M-6 · The finitization tell.** When a boundary makes the problem
  enumerable, ask who drew it and why. Provenance-sort every exclusion:
  intent-discovered / introduced / omitted. The omitted ones (motion)
  are found ONLY by running this audit.
- **M-7 · Epistemic marks as immune system.** [CODE] facts carry
  file:line and re-verify triggers; [HYP] items carry named killers;
  [SID] leans record a position while making unmissable that the
  ruling isn't made. A map that can't say how it might be wrong
  becomes scripture (the seven-class map nearly did).
- **M-8 · (inherited, reconfirmed) Meaning is not falsifiable in
  written claims; the variable that moves outcomes is time between
  judge touches.** This inquiry's whole cadence — Sid's interrogation
  prompts between every round — is that law applied to thinking
  instead of building.

---

## 6. Verbatim anchors

The words that turned the inquiry, in order (full text: `vision/LOG.md`
2026-08-01 and 2026-08-02): the cut-2 flat landing ("is this all goineg
somewhere???") · the ask ("never the missing render … don't try to talk
out of this") · the altitude ("What must ultimately exist before
Softland can honestly promise…") · the interrogation discipline
("examine what exact promise the architecture is making" · "let the
present map dissolve if the inquiry requires it") · the stop-word
("strongest discovery artifact we have now … mark the unproven claims
as provisional rather than canonical").

---

## 7. The frontier at close

Three live threads, one per instrument-kind — the balance itself a
health sign: **D1** (Slug role · fp16 · storage road · H3) → machine,
E0, spec + pre-registered rules in `ENGINE.md` §12 · **D2** (the shape
material contract; centerline vs outline) → design round, parallel ·
**D5** (meaning above the engine) → lived, E1's felt gate = Sid's paper
page redrawn in the ink room. Plus one paragraph only Sid can write
(the envelope ratification) and the chrome walkthrough (the weakest
plank). Everything else: settled, parked with tripwires, or waiting on
evidence. The operational state, always: `ENGINE.md`.
