# The client's intended design — the render boundary

What the client is meant to become, where that runs ahead of the code in this
tree: Sid's rulings of 3 August and 2 September 2026 on how frames get
computed, and the Electric question as it stood on 12 September. The text was
kept in `docs/decisions.md` until 18 November 2026 and is here as it stood
there, with its dates. The citations that had been cut out of it are restored,
pointing to where their targets now are under `history/`.

The source maps beside this file say what exists; this page says what is
intended. Where the two differ, state the difference: it is a fact about the
code or about the design, and neither is a bug by itself
([upkeep rules](CLAUDE.md)). What this page says does or does not exist was
true on its dates, so check it against the code. A ruling has no authority of
its own (root `CLAUDE.md`, "What we are committed to"): when the problem
gives a better design, make the case to Sid.

## The render boundary — how frames get computed (settled 2026-08-03, amended 2026-09-02)

Every render-path disease we have had was the read side paying to rediscover
what the write side already knew: a compiler re-deriving order every frame
from keyed mutations that knew their own diffs, a pick re-compiling an
unchanged world, a caret blink re-shaping a document it never touched, a
frame counter sitting as an ancestor of scene derivation. This page is
the refusal to forget (Sid's push: solve it systemically, never patch). It
governs any path that feeds frames and extends the one-render-substrate
ruling. The 2026-09-02 amendment carries the waist, round two: two
maps drawn from code alone, merged in chat, settled by Sid's word
(`land it · A: data · B: GPU`; the contract that holds the cut is
`history/docs/seam-cuts/CONTRACT.md`).

**Where a thing lives — the two axes.** Two questions decide residency:
its **granularity** (per pixel is the engine; per vertex or per entity is the ECS
layer above the waist) and **whether the camera is an input** (a derivation
that takes the camera is engine-internal, never a row).

**The engine** is compiled code that is physical or holds uncommitted state: filling any
contours under a fill rule with per-pixel coverage · the paint kinds the
shader evaluates · composite operations (clip by mask, group opacity, blend) · the
camera · the pointer · the clock · the wire · culling · the uncommitted store
(what is not yet a row) · the derivation cache (engine-internal derivations of
settled rows) · the hit builtin the runtime calls with screen-pixel slop
converted through the world-transform scale · the runtime and its builtins. Sid's
older criterion still names it: description in, geometry out, or geometry
in, pixels out, without knowing what the thing is or what a gesture means,
plus ownership of GPU memory and order
(`history/docs/below-the-waist/INSTANCE-CUT-NOW.md`). The pointer, uncommitted store,
wire, clock and culling are engine code that does not exist yet; they land in
that order, the order a canvas needs them.

**Geometry generators are data** (Sid: `A: data`), run by a client runtime over compiled
builtins. The stroker, the brush, layout, every shape and every composite are
geometry-generator rows. The stroker emits camera-free offset contours or it is not done.
The layer above the waist is the ECS layer, in Sid's words (2026-09-02):
"we leave other heigher level layers for ecs and call that above the waist.
Anything above the waist is stored as data, is collaborative editable,
createable, agents and humans can build freely over it without getting into
git merge deadlocks, anything in ecs layer should be creatable and then
saved for reuse or build higher order things from it".

**Rows** are truth and derived rows. A derivation is a row only if something
above the vertex-packing step reads it: layout passes (caret, selection, hit and
on-plane read it), scene evaluation passes (pick and shadow space read it),
world transforms pass (hit and clip read them). Meshes and atlas placements
fail the test and are never rows.

**Fill moves to the GPU** (Sid: `B: GPU`): contours under a rule with
per-pixel coverage, curves evaluated per pixel the way the Slug text shader
already does for glyphs. Ear clipping, hole bridging and the LOD
table go with that step. The legal zoom range lives in the server's space
component; what remains on the engine is one flattening tolerance in screen
pixels.

**The boundary is a row boundary, not a folder boundary.** A layer's vocabulary
never appears in another layer's rows: no buffer index on a draw item, no process-local
stamp on a component, no shader concern in a schema. The engine translates at
the entry point. Draw items name group ids and the transform hierarchy runs. The dirty check
is the row's revision. The schema of a kind is declared data checked by one
engine. The color lives once.

**Dead means wrong form for the waist, called or not.** Right form for a
valid future case is alive, called or not. Sid's criterion, verbatim
(2026-09-02): "Things that should be dead are not explicitly... that are not
being called from anywhere, but like that exists for some future valid case
and are implemented in the form that they should be". And the order of work:
"first make the existing code how it should be and only then fold the other
things in … we are going to remove now". Restated 2026-09-03: "any code that
survies should be there only if its in the form as it should be". The path
boundary is the template (`history/docs/seam-cuts/`); the same removals repeat on image, text and region.

**One vocabulary (Sid, 2026-09-02).** His rule, verbatim: "The standard
graphics or ECS term is canonical in every doc. A Softland word survives
only if BOTH hold: Sid coined it (verbatim hit in vision/LOG.md) AND no
standard term covers the concept. A rename of a standard concept never
survives." And the test every word must pass: "I'm at a stage where I want
to have less vocabulary and whatever that is makes direct sense in
connection to me. So I don't have to dig through things". So the most
specific standard term wins and a genus word never stands alone (a two-word
name that says which thing, "draw item", beats "instance"); a word that
means something else in graphics is a collision, never a keep. Code
identifiers, folder names and namespaces rename in the step that touches
each file, never a sweep; until then a doc names the identifier in
backticks and the concept in this vocabulary.

**Electric and the Softland target.** Softland's presentation target is its own
rendering system. Electric v3's supplied UI integration targets the DOM, while
its reactive language offers functions, tracked dependencies, keyed occurrences,
conditional lifetime, and client/server placement. The place to examine their
meeting is the UI/client work that turns changing values and interaction into
presentation. Softland must define the meaning of its scene, spatial input,
resource ownership, draw order, and frame scheduling.

How much of Electric to use, and where to connect it, remain implementation
decisions. Learning from its source while using Missionary, using actual
Electric computations to feed Softland rendering, and building Softland target
bindings are possibilities to investigate. `e/defn` and `e/diff` are candidates
whose compiler/runtime requirements and behaviour need checking; mentioning
them chooses neither the integration nor the architecture. A DOM target does
not make every Electric affordance irrelevant, and an existing integration
does not establish the system we should build.

The authored form of entity templates remains data that humans and agents can
create and change inside the medium. Which host executes that material is an
implementation choice; choosing a reactive host does not itself satisfy the
authorship requirement.

**Implementation observed 2026-09-12:** `deps.edn` pins Missionary directly and
adds actual Electric in the `:inland` alias. `src-inland/README.md` maps the
current product caller of Softland's renderers. This describes the checkout;
it does not settle how far to adopt that implementation. Use `electric-docs`
for source lookup and the language/target distinctions, checking the version
and source path relevant to the work.
