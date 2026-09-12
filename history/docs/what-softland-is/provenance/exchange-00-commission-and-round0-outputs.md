# Product-Architecture Exchange — 00 · Commission and Round-0 Outputs

Part of the 2026-08-15 three-session exchange on the missing product
architecture. Working sessions: `softland-product-architecture-should-be-multi-turn`
[b8aad9] and `softland-product-architecture-should-be-one-shot` [d3c7d7], each
fenced from docs/bets.md, decision*.md, next-prompt.md, sense-line-model.md,
any DIRECTION.md, and vision/**. Meta session (router):
https://claude.ai/code/session_0166nBBfmfToLbUigW1DKN5w — same fence.

Fidelity: the commission and both outputs below are the text Sid pasted into
the meta session — the courier copy, the same text Sid read. Sid's separator
lines are kept. Plain-text box tables reflow under markdown renderers; the
file text is the record. The working sessions' own transcripts are the
ultimate primaries.

The multi-turn session's output is three turn-replies in sequence (it began
from a narrower opening question and grew by Sid's follow-ups); the one-shot
session's output is a single reply to the commission as stated.

---

## The commission (problem definition given to both working sessions)

Read:

    - docs/what-should-softland-be-session-1.md
    - docs/what-should-softland-be.md

    Then inspect whatever source is relevant. Do not read docs/bets.md, docs/
    decision*.md, docs/next-prompt.md, docs/sense-line-model.md, any
    DIRECTION.md, or vision/**.

    I want to understand the missing product architecture, not audit or inherit
    the current plan. Treat existing implementations as evidence and possible
    precedents—not as proof of what should be built. Existence falsifies
    absence; it does not establish inheritance.

    The deeper question is:

    How can Softland let a mind address any whole or subpart of its material,
    manipulate or question it at local or global scope, and move between
    multiple connected, bidirectional projections of it—such as text, spatial
    canvas, 3D, time, and an agent briefing?

    For example, I should be able to enter a 3D model, point at one submodule,
    ask about it, reach its connected textual or spatial representation, alter
    it through an appropriate view, and see the consequences across the others.

    Use text, 2D drawing, and 3D as deliberately dissimilar tests. Step one
    level above each instance and identify:

    - what genuinely recurs at the systems level;
    - what must remain specific to each medium;
    - what belongs in mutable data, stable code, or a tool;
    - what architecture must exist that does not exist today.

    Do not let the pencil, editor, or any current implementation define the
    answer, and do not force a generalization that fails under the dissimilar
    cases. Think fresh and make the missing product capability concrete enough
    that I can picture it.

## Sid's context to the meta session (verbatim)

there are 2 parallel session that ran on this same problem space (defined
above) but one was multi turn (started with a specific narrow statement and as
i read through i asked more) other one was one shot (basically the problem
definition that i pasted above and a sibling of multi turn in sense that i
gained better question asking capability) the session names are

- softland-product-architecture-should-be-multi-turn
- softland-product-architecture-should-be-one-shot

I will paste their outputs below, (for multi shot one it will 3 turn replies
in one but you are smart to understand it) then i want you to think through
what axis did they both worked on for this problem space, might have missed.
And what questions/claims/hypothesis we want to share to and fro between them,
why? because I want this problem space well understood and genuenly not meta
theorizing just for the sake of it. So consider yourself as the smartness
level but focusing your intent on meta, and we will treat the other 2 session
as same smartness but focused on their individual axis so we don't dilute them
trying to understand the other on questions that don't have much weightage.
You know you can communicate both to both theses session like you can send
messages to these individual session if you want to.

---

OUTPUTS:

-----START------

SESSION NAME:  softland-product-architecture-should-be-multi-turn


Recon complete — I read the two destination essays and walked the source tree itself (the material/facet system, the verb registry, face assemblies, the path substrate, the kernel taxonomy, the matter rooms, activation scopes). Nothing from DIRECTION.md, vision/, or the fenced docs was read, so whatever overlap exists with those I genuinely can't restate. Everything below is re-derived from the code and the two preserved essays. Here is the answer, thought fresh at the product-architecture layer.

Legend for load-bearing claims: [c] = checked in source this session · [d] = derived from checked facts · [a] = assumption, named.

---
The simple story

1. Softland today can render many kinds of things, but it can only make one kind of thing from inside: a block of text.
2. Everything else that exists in the land — its looks, its rules, its gestures — was made outside, in files and chat, and then carried in.
3. The system already measures this: it counts how often building Softland required leaving Softland. [c]
4. So the missing architecture is not another renderer and not another theory — it is the return trip: the path from a rendered thing back to the stuff that made it, and forward again as a change.
5. A thing is "native" when its circle is closed: you can make one with a gesture, change it and the change survives, see it rendered, point at it from elsewhere, and every step is on the record.
6. Text is native because its circle closed; nothing else's circle has closed yet.
7. An "editor" is not a component — it is the feeling of a closed circle; the same is true of a "pencil."
8. So the pencil is not a thing to hardcode: its strokes are data, its grip is a binding, its math is code — a tool is a loop threaded through all the lanes, exactly like text already is.
9. The line between hard code and data is not "logic vs. layout" — it is what happens when the author is wrong: data fails as a visible card you can roll back; code fails as a dead land.
10. Therefore code must keep exactly the things that make data safe to accept — the checkers, the renderer's math, the write funnel, the escape gesture — and everything else is destined to migrate into data, one grammar at a time.
11. An authoring runtime is a renderer that also ships its inverse: every surface can answer "what made you?", accept "be different" as a proposed revision rather than a mutation, and offer "make another."
12. Softland already contains this entire loop, working end to end, for exactly one narrow material — its own policy settings — and that proof is the most important fact in the codebase.
13. The crossing is therefore not invention; it is generalizing that one loop to the two families that matter: the faces of things, and drawn shapes.
14. Five organs are missing: a place where drafts live, hands for drawing, the reverse arrow from pixel to proposal, a distiller that turns a drawing into a component, and refusals that file the code-work they need instead of vanishing.
15. Build those five, and "build Softland from Softland" stops being a slogan and becomes a loop that runs — with the escape gauge as its progress meter.

---
A scene you can walk

The delta, felt rather than listed. You want machine blocks to carry a small staleness pill that weathers as their basis ages.

You press the meta-gesture on a machine block. The Halo opens at the point — identity and verbs. *(Exists today. [c])*

You step past the block into its face. The Halo now answers for pixels: this pill is a badge, from face so-and-so, this exact node of it, worn at this address since Tuesday, fed by this projection. The system already stamps every rendered node with the path back to the template node that made it — the wiring for this answer exists; the surface that speaks it does not. (Half-exists. [c] — provenance stamped per node, built explicitly for "designer edit-mode"; no edit-mode consumes it yet.)

You drag the badge's corner. Because you're inside the face, the drag does not move a thing in the world — it writes a candidate: the same face with one value changed, previewed for your eyes only. *(Missing — this is the reverse arrow. The preview membrane and the candidate machinery both exist for policy masters [c]; nothing turns a picked node plus a gesture into a face candidate.)*

You speak at the badge: "weather this with basis age." The block's resident reads the picked node, the face source, and the closed vocabulary of primitives, and answers with a candidate — or with the exact grammar refusal. *(Ingredients exist — residents summoned per block, grammars that refuse malformed material with error cards [c]; the seam from picked-node-plus-utterance to authored-candidate is missing.)*

The refusal has teeth. Basis age isn't in the data the face is fed, and faces are arrangement-only — they cannot compute it. That is a code-lane fact. Today the refusal would be a red card that vanishes. In the architecture to build, the refusal files itself: a commission — "this projection must carry basis-age" — lands as durable material an agent can pick up, and the release closes it. (Missing.)

Meanwhile you draw. The pill you actually want, sketched with a pencil, pressure and all. The strokes are real material born in your draft ground — pure form, meaning nothing yet. *(The ink atom is fully specced — geometry authority, zoom regimes, hit rules, provenance fields — and renders behind a dev flag. But no gesture creates one and no durable lane stores one: the pencil has no hand, and ink has no home. [c])*

You lasso the strokes: "this is the badge face." A distiller births a face candidate whose provenance says based on: your sketch. The sketch is never thrown away — it is the source artifact of the component, exactly the way a transcript is the source of its blocks. *(Missing — but the pattern is proven: the block distiller already turns raw source into addressable units, and the assembly adapter already links forks with based-on edges. [c])*

You activate — scoped to your workshop first. Blocks in your draft region wear the new face; the world doesn't. When it's earned, one more activation reaches everyone — signed, grounded, with rollback standing. *(Candidate → activate → rollback exists and runs [c]. But activation scope today admits exactly two values: the whole world, or one subject — the code even says "richer scopes wait for a need that names itself." [c] This is the need, naming itself.)*

A week later you press the badge again and its whole lineage opens: sketch, distillation, candidate, activation, author, grounds. Nothing about how the land looks is a mystery, and nothing got there without a signed act.

That scene is the product-architecture answer. Every sentence marked missing is the architecture that must be built; every sentence marked exists is why it's closer than it feels.

---
Part 1 — What a native is, derived from the one that exists

Your first question — "what are the primitives/natives of Softland now? one obvious answer is text, but does that mean editor?" — dissolves once you look at why text works. Text is not native because there's an editor. Text is native because six stations are all staffed [c]:

┌──────────┬───────────────────────────────────┬─────────────────────────────────────────────────────────────┐
│ Station  │           What it means           │                         Text has it                         │
├──────────┼───────────────────────────────────┼─────────────────────────────────────────────────────────────┤
│ Atom     │ a durable form with revisions, in │ blocks, revisioned, imported/edited through one funnel      │
│          │  the container                    │                                                             │
├──────────┼───────────────────────────────────┼─────────────────────────────────────────────────────────────┤
│ Birth    │ a gesture that creates one        │ click empty ground, type — the block is born on first       │
│          │                                   │ content                                                     │
├──────────┼───────────────────────────────────┼─────────────────────────────────────────────────────────────┤
│ Edit     │ changes that round-trip through   │ every keystroke rides the durable edit artery; glyphs       │
│ loop     │ truth                             │ render from the confirmed value                             │
├──────────┼───────────────────────────────────┼─────────────────────────────────────────────────────────────┤
│ Face     │ how it shows                      │ anatomy rows → assembly → primitives → glyph atlas          │
├──────────┼───────────────────────────────────┼─────────────────────────────────────────────────────────────┤
│ Address  │ how other things point at it      │ unit ids, relation edges, agent briefings narrowed to a     │
│          │                                   │ block                                                       │
├──────────┼───────────────────────────────────┼─────────────────────────────────────────────────────────────┤
│ Law      │ policy + provenance               │ wrap policy is served revisioned material; every act        │
│          │                                   │ carries an actor                                            │
└──────────┴───────────────────────────────────┴─────────────────────────────────────────────────────────────┘

"Editor" is the name for the experience of all six running at once. It is not a component and it doesn't live anywhere — which is the answer to "what is text without a way to lay it out": the layout mathematics (line breaking, shaping) is code; the layout choices (wrap columns) are served data with a code floor [c]. The editor-ness is distributed across the lanes, and asking "where does the editor live?" is a category error the codebase has already, quietly, stopped making.

Now run the same six stations across every family in the tree, and the state of the land is one honest table [c]:

┌──────────────────┬────────────────┬───────────┬──────────────────────┬──────────┬─────────────┬─────────┐
│      Family      │      Atom      │   Birth   │      Edit loop       │   Face   │   Address   │   Law   │
├──────────────────┼────────────────┼───────────┼──────────────────────┼──────────┼─────────────┼─────────┤
│ Text blocks      │ ✓              │ ✓         │ ✓                    │ ✓        │ ✓           │ ✓       │
├──────────────────┼────────────────┼───────────┼──────────────────────┼──────────┼─────────────┼─────────┤
│ Positions/camera │ ✓              │ ✓         │ ✓                    │ ✓        │ partial     │ ✓       │
│                  │ (settle-state) │           │                      │          │             │         │
├──────────────────┼────────────────┼───────────┼──────────────────────┼──────────┼─────────────┼─────────┤
│ Policy (facet    │ ✓              │ ✓         │ ✓                    │ ✓        │ ✓           │ ✓       │
│ material)        │                │ (deviate) │ (candidate→activate) │ (rooms)  │             │         │
├──────────────────┼────────────────┼───────────┼──────────────────────┼──────────┼─────────────┼─────────┤
│ Faces            │ ✓              │ ✗ from    │ ✗ from inside        │ ✓        │ ✓ (worn at  │ partial │
│ (assemblies)     │                │ inside    │                      │          │ addresses)  │         │
├──────────────────┼────────────────┼───────────┼──────────────────────┼──────────┼─────────────┼─────────┤
│ Ink / shapes     │ schema only    │ ✗         │ ✗                    │ ✓ behind │ ✗           │ schema  │
│                  │                │           │                      │  a flag  │             │ only    │
├──────────────────┼────────────────┼───────────┼──────────────────────┼──────────┼─────────────┼─────────┤
│ Images           │ fixture only   │ ✗         │ ✗                    │ ✓ behind │ ✗           │ partial │
│                  │                │           │                      │  a flag  │             │         │
├──────────────────┼────────────────┼───────────┼──────────────────────┼──────────┼─────────────┼─────────┤
│ Code (its own    │ ✓ (ingested as │ ✗ from    │ ✗ from inside        │ partial  │ ✓           │ via git │
│ source)          │  atoms)        │ inside    │                      │          │             │         │
└──────────────────┴────────────────┴───────────┴──────────────────────┴──────────┴─────────────┴─────────┘

Read the table as a shape and the answer to "what is the next native, where does it live" falls out: a native is not invented, it is completed — the next native is whichever family you finish staffing. The six stations are the same six every time; the kernel-shape file, the adapter pattern, and the facet lifecycle are the per-station templates, each proven at least once. That's the small half of the answer. The big half is that two of those rows — faces and ink — aren't just incomplete natives. They're the two whose completion is the authoring crossing.

★ Insight ─────────────────────────────────────
A tool is not a thing; it is the experiential name for a closed loop over a material family. "Editor" = the six stations closed over text. "Pencil" = the six stations closed over strokes. This is why the pencil cannot be found in the codebase and also cannot be added as a component: it will exist the moment ink's circle closes, and not before, and nowhere in particular.
─────────────────────────────────────────────────

---
Part 2 — Where the pencil lives (the dismemberment answer)

"Why is a pencil or any Figma-type tool live??" — here is the pencil, dismembered into lanes, each placement forced by the custody law in Part 3:

- Its strokes — ink atoms: durable data in the container, like blocks. The schema already exists and is good: centerline knots with pressure, provenance fields on the material itself, declared hit rules. [c]
- Its grip — a binding row in served material: "pointer press on ground, in draw mode → the ink verb." Bindings are already data, already revisioned, already refusable by grammar. [c] So which gesture is the pencil is a data question — rebindable, per-workspace, without touching code.
- Its meaning — a verb implementation in code: begin/move/end continuations writing knots. The verb registry's continuation machinery (begin/move/end) already models a stroke exactly; the registry even has a declared effect-class for everything a verb may do. [c] The pencil verb is a registry entry away.
- Its math — tessellation and hit-testing: permanent code. A wrong stroke is a card; a wrong tessellator corrupts every frame.
- Its feel — base width, smoothing, caps: policy material with a code floor, like wrap columns.
- Its face — the path render family, already in the frame executor's closed family registry. [c]

Notice what this means for the ECS question: the pencil never appears in the entity system as "a pencil." The entity system holds strokes (atoms), wears (which face at which address), and rows (bindings, policy). The tool is reconstituted at runtime by the loop. This is not evasive — it is exactly how text already works, and it is why tools in Softland will be forkable as data: your pencil and my pencil can differ in grip and feel while sharing math, because everything above the math line is served material.

---
Part 3 — The custody law: what goes in data, what stays code, and why

Your hardest phrasing — "what do we define in the ECS and data layer vs hard code and why???" — has an answer the codebase has been enacting without ever stating. Every custody decision already made in the tree draws the same line [c]: assemblies may hold arrangement but the guard scans out anything that could be a program; bindings may name verbs but never code; policy values are data but their validators are code; the camera is floor-reserved so that no material, valid or malformed, can capture it. One generator explains every case:

★ Insight ─────────────────────────────────────
The custody line is blast radius under a wrong author. Data is whatever the system can validate, refuse, preview, and roll back without trusting it — a wrong value fails as a visible error card. Code is whatever the system must trust to even run — a wrong value fails as a dead land. The line is not "logic vs. layout"; plenty of logic (policy, bindings, conditions) is data-lane, because it can fail safe. What can never migrate is the machinery that makes failing safe possible.
─────────────────────────────────────────────────

From that law, the permanent code floor — the full list, and it is short [d]:

1. The frame executor and the families' mathematics — rects, glyphs, tessellation, pick. A renderer bug can't error-card; there's nothing left to draw the card.
2. The checkers — grammars, validators, the assembly guard, the compilers. You cannot data-fy the thing that makes data safe; the validator of a lane can't ride the lane it validates. It changes through the slow lane: git, review, release.
3. The funnel — request → decision → event, actors, capabilities. The provenance clause of the destination essays is enforced here or nowhere.
4. The floor gestures — camera, and the meta-gesture that opens the Halo. These are the constitutional escape hatch: the one guarantee that no worn material, however wrong, can trap you inside itself. (This is the "renderer is kernel, skin is lease" clause of the destination essays, discovered independently by the tree — the code literally marks these verbs uncapturable. [c])

Everything else — faces, anatomies, bindings, tool-sets, policy, themes, arrangements, and eventually projections' selections if not their computation — is code only until its grammar exists. The tree's own history shows the ratchet running: interaction meaning was hardcoded in one giant pointer state machine and has been strangler-extracted verb by verb, each with a receipt naming the exact branch it was lifted from [c]; block rendering was hardcoded and is mid-migration into anatomy rows [c]. So the real answer to "what goes where" is not a static partition. It is: a ratchet with a fixed floor. Things are born in code, proven by use, given a grammar, and migrated to data over a code floor — and the direction of travel only ever goes one way.

The test, usable at every future decision: can a wrong revision of this fail as a card and roll back? Yes → it belongs in material, eventually. No → it is floor, forever.

---
Part 4 — What an authoring runtime actually is

The rendering runtime, which exists, is one arrow: durable material → projection → data context → face → render tree → tape → pixels. [c]

An authoring runtime is that renderer with its inverse also shipped. Three reverse arrows, and a definition you can hold:

- Inspect — every surface answers "what made you?" Pixel → picked node → template path → face revision → wear act → projection → material atoms → authors. The chain must be walkable by a person pressing, and readable by an agent as data.
- Alter — every surface accepts "be different" — never as mutation, always as a candidate: a proposed revision, grammar-checked, previewed inside a membrane, activated by a signed decision, rollback standing.
- Coin — every family offers "make another": a birth gesture for atoms, a promotion ladder for made things (scratch → named → worn → shared).

All three arrows travel the same lanes the forward arrow travels — the same container, the same funnel, the same provenance. That last clause is what "author its own things" means: the authoring acts are themselves material in the land, so the system's growth is part of the system's record.

And here is the fact I found that reframes the whole build — the most consequential thing in the tree:

★ Insight ─────────────────────────────────────
Softland already contains a complete, running authoring runtime — for exactly one material. Policy masters have the full circle: projected into rooms as ordinary blocks, deviated by utterance, candidates grammar-checked, previewed client-side, activated by signed event with grounds and scope, rolled back without deleting history — and agents can drive every step from inside. [c] The crossing to "authors its own things" is therefore not an invention problem. It is a generalization problem: widen that one proven loop from policy scalars to faces and strokes.
─────────────────────────────────────────────────

This also locates the destination essays' law in the product [d]: activation is the convergence event of the visual world — the plural resting state (candidates, deviations, forks of faces) converging only at a signed decision that deletes nothing. The essays' "convergence is an event, not a state" is not a metaphor here; it is literally the activation-event machinery, already built, waiting to govern more than zoom bounds.

---
Part 5 — What must exist that does not exist today

Five organs. Everything else in the crossing is wiring between things already proven.

1. The draft regime — a place for half-made things. (The biggest conceptual gap.) Today there are exactly three states: ephemeral preview (your eyes, this session), candidate (durable, worn by no one), and active (worn by the world) — because activation scope admits only "everyone" or "one subject." [c] Authoring needs the middle place: scoped wear — "this region of ground is my workshop; things here wear my drafts, and several drafts can interact." Without it every authoring act is a publish, and the darkness dial from the destination essays is pinned at zero — premature settlement forced by architecture. Concretely: a workshop scope in the activation vocabulary, plus ground regions that carry it. The scope field, the event grammar, and the region substrate all exist [c]; the scope value and its wear semantics do not.

2. Hands — birth verbs and the ink lane. The first verbs that create: stroke and shape birth riding the existing begin/move/end continuation machinery, and an ink adapter into the container in the pattern every other adapter follows (no new module — ink rides the same ontology blocks do). Notably, even text's birth predates the verb registry and lives in a bespoke lane [c] — so building ink birth as the first registry-native birth verb retroactively gives the system its template for all future births.

3. The reverse arrow — edit-mode that writes candidates. The one genuinely new interaction concept: a mode, entered through the Halo, in which gestures and utterances at a picked surface produce face candidates instead of world mutations. Drag a template's prop directly (mechanical grade); speak an intent and the block's resident authors the candidate against the closed primitive vocabulary (resident grade — the enzymes of the destination essays, given their first real job in the visual world). Every ingredient exists — per-node template provenance, the room pattern, grammar refusal, preview, activation [c] — the arrow connecting them does not.

4. The sketch distiller — drawing becomes component. "I want to draw and build components directly in Softland" decomposes as: draw (organ 2) → lasso and name → distill. Two grades, both real: stamp grade — the face simply is those paths (icons, marks, glyphs; mechanical, buildable immediately); semantic grade — a resident reads the drawn form plus a sentence and authors a primitive-based assembly that renders like the drawing. In both, the sketch is kept as the component's source artifact, linked by a based-on edge — sketch is to component as transcript is to blocks, and the distiller pattern is already proven at the block kernel. [c] Your drawing is never a picture of the component; it is the component's provenance.

5. Refusal → commission — the instrumented boundary with the code lane. When authoring hits the floor — needs a primitive, a projection field, a verb that doesn't exist — the grammar refusal must stop being a dead-end card and become filed work: a durable commission naming exactly what the code lane owes, addressable, pick-up-able by an agent, closed by a release. The code lane never disappears in this architecture — it becomes a visible queue with a meter. The escape gauge already measures the boundary [c]; commissions make the boundary workable. (This is the destination essays' "the machinery's exhaust lands as questions in the commons," arrived at from the mechanics instead of the philosophy.)

What deliberately does not appear on this list: a plugin system, a scripting language for faces, code generation into the substrate, or any general "user-defined behavior" mechanism. The arrangement-only guard is load-bearing [c], and the destination essays' refusals back it [d]: anything that wants to be a program gets to be a real one, in the code lane, through the funnel, with a release — because a program smuggled into data is a judge that exited the system.

---
Part 6 — Position: the order, and one fork that is yours

My position [d]: the next native is the face, and the pencil enters as the face's authoring gesture — not as a standalone ink feature. Reasoning: "build Softland from Softland" is bottlenecked on faces — the land's own surfaces are the material most worth authoring, the anatomy migration is already mid-flight toward exactly this, and organ 3 (the reverse arrow) is the single highest-leverage build because it converts the entire existing face system into an authoring surface at once. A pencil built first, without the distiller, produces decoration — one more renderable, which is precisely the trap named in your question. A pencil built inside the face loop produces components from day one, and the strokes mean something the moment they land.

So the build order that makes each piece the client of the previous — no museum natives: the reverse arrow on faces (3), inside the draft regime (1); then hands (2) feeding the distiller (4); commissions (5) throughout, because every step will generate them.

The genuine fork, which your word closes and mine cannot: whether the first drawing milestone is stamp grade (your strokes literally are the face — fast, honest, limited) or semantic grade (a resident distills strokes into primitive-based assemblies — slower, and the real Figma-crossing). I'd ship stamp grade inside the first draft-regime milestone and let semantic grade arrive as the residents' first commissioned craft — but which one feels like drawing-in-Softland to you is a thing only you can answer from inside the product, and it's the kind of decision the draft regime exists to make cheap.

---
Against your own criteria, so you can grade it: the missing product architecture is the five organs (Part 5); text/editor/pencil/ECS resolves as tools are loops, natives are six staffed stations, custody is blast radius (Parts 1–3); an authoring runtime is the renderer's inverse shipped on the same lanes (Part 4), and the load-bearing discovery is that it already runs at n=1 in the matter rooms; the direction — generalize the proven loop rather than invent an authoring system, with faces first and the pencil as the face's authoring gesture — is derived from the tree, not from any direction doc, all of which stayed unread. Whether you can now picture what must exist: the scene in the walkthrough is that picture, with each missing organ marked at the exact sentence where you'd feel its absence.
Correction taken at its full depth, not just its surface. What I did last turn: found the loop running for policy scalars, found text's loop closed, and slid from "an authoring loop exists" to "the authoring loop — widen it." Existence falsified the absence claim; the inheritance claim rode in unexamined. Worse, I had already read a counterexample and mis-filed it — text's committed-echo editing is not candidate-shaped, and I called it an "edit loop" without noticing my own contract couldn't hold it. So: one level up, contract stated explicitly, then run into region3d — which I name-dropped last turn but had not read. This turn I read the family in full: the material grammar, the evaluation layer, the rung admission system, the pointer court, the scene derivation, and the live runtime. [c] throughout below means checked in those reads.

---
The candidate contract, stated so it can fail

What last turn implicitly claimed was general — seven clauses, at the altitude where 3D can break them:

- K1 Authority/projection. Each family has one authoritative durable value; every derived form (mesh, tape, glyph run, GPU state) is disposable and re-derivable.
- K2 Settle. Gesture-time state is session-local projection; truth moves only through validated, provenance-stamped acts.
- K3 Alteration-as-candidacy. All change rides candidate → grammar check → preview membrane → signed scoped activation → rollback; never mutation.
- K4 Custody. Data is what can fail as a card and roll back; code is the checkers, executor, funnel, and floor; no logic in data; the camera is floor-reserved.
- K5 Reverse arrows. Inspect (to declared provenance), alter (via K3), coin (birth verbs + promotion).
- K6 Same lanes. Authoring acts are themselves material in the one container/funnel.
- K7 Stations-as-inheritance. Six stations, the same six, per family — staffed by the proven machinery.

---
What region3d actually is

The dissimilarity is real — eight axes at once: value structure (an object graph with parent hierarchy and cycle checks, not a flat form), size (meshes up to 65k vertices with hard caps), edit cadence (continuous manipulation, not deliberate rare deviation), sub-identity (per-object ids with per-object required provenance — :asserted-by is mandatory on every object), rendering cost (budgeted, refusable), cameras (constitutive, plural), validation (fail-closed canonicalization plus runtime equivalence oracles), and composition (regions contain other families' material — text and ink placed by {:address …} refs, with a declared placed-material pick route). [c]

Three findings that do the adjudicating work:

1. Region3D already designed its own alteration shape, and it is not candidacy. A closed vocabulary of nine edit operations; edit-diff mints invertible per-object diffs (before/after sub-maps; whole-scene replacement deliberately unrepresentable); apply-edit is pure replay, used by a maintained-view oracle and the GPU verifier; each diff carries :asserted-by and a :draft-settle field. [c] Today exactly one op is live — gizmo transform: drag writes a session preview-transform, release settles an invertible diff into a session vector, hardcoded actor. [c] The op log has no funnel yet — the diffs are queuing in a client atom, shaped for a durable lane that doesn't exist. Evidence of design intent, not of a working lane.

2. Region3D carries checker tiers policy never needed. Fail-closed canonical validation at admission (throws, not error-cards — a third refusal style beside compile-reports and cards), then a runtime tier: the maintained incremental view is fenced by equivalence against full re-derivation, and degradation is receipted — the rung system admits a region's offscreen target at divisor 1/2/4/8 against a byte budget and records exactly what was granted and what was denied. [c] Grammar admission alone cannot certify 3D correctness; the family had to add oracles and receipts.

3. The camera multiplied, and data captured some of it. Each region's view (pivot/distance/yaw/pitch/lens) is material — data. Camera objects exist in scenes — data. What stayed code is the court: wheel-route adjudicates custody per input sample (region first, ground fallback, dev receipts counting dual dispatches). [c] So "the camera is floor-reserved" was policy-era phrasing for something else.

---
The verdict, clause by clause

K1 survives, strengthened. 3D is the contract's best instance, not its breaker: primitive params are authority, meshes are versioned derivations with declared cache keys; the whole citizenship block declares authority/derivation/regime per family. [c] Attested now in path, region3d, faces, text — I'll treat this clause as load-bearing.

K2 survives, and 3D is its richest instance. Preview-transform overlay at gesture-time, settled transforms at release, truth (designed to) move only by validated replayable acts — structurally identical to text's intent-queue-then-confirmed-value and drag's projection-then-armed-settle. [c] Three independent families converged on it; I'd now call this the settle law and keep it.

K3 breaks as stated. The tree contains three settlement shapes, and two of them were already there before 3D:

┌─────────────────┬──────────────────┬────────────────────────┬─────────────────────────────────────────────┐
│     Profile     │      Family      │     Revision grain     │                  Protocol                   │
├─────────────────┼──────────────────┼────────────────────────┼─────────────────────────────────────────────┤
│ Form candidacy  │ policy masters   │ whole form             │ candidate → grammar → preview → activate →  │
│                 │                  │                        │ rollback                                    │
├─────────────────┼──────────────────┼────────────────────────┼─────────────────────────────────────────────┤
│ Committed echo  │ text             │ per confirmed edit     │ every act durable immediately through the   │
│                 │                  │                        │ edit artery                                 │
├─────────────────┼──────────────────┼────────────────────────┼─────────────────────────────────────────────┤
│ Invertible      │ region3d         │ per closed-vocabulary  │ diff → replay oracle → (no funnel yet)      │
│ op-log          │ (designed)       │ op                     │                                             │
└─────────────────┴──────────────────┴────────────────────────┴─────────────────────────────────────────────┘

What I called "the loop" is the first row. The general structure, once the conflation is undone, is two orthogonal axes I had fused: the settlement profile (set by the family's edit cadence and value structure — continuous manipulation cannot ride whole-form candidacy, rare policy deviation shouldn't ride an op log) and the publication protocol (set by audience: when a change crosses from one author's regime to other wearers, it takes the decision-event shape — signed, scoped, grounded, rollback standing). Policy uses candidacy for everything because policy is always published — every change immediately crosses the audience boundary, so its two axes coincide. I mistook that coincidence for the contract. The :draft-settle field on 3D's diffs is the tree independently anticipating the split [c]: settle finely and constantly below the boundary; publish coarsely and deliberately at it.

K4 bends in three places, each a real repair:

- "No logic in data" was already false and I repeated it. Assemblies contain :each — bounded iteration — inside a grammar whose own docstring says "no logic ever." [c] The line the tree actually draws, which 3D's anticipated "node-authoring extensions" [c] will test hardest: closed composition under declared budget is data; open terms are not. A closed op vocabulary, a bounded combinator, a terminating-by-construction node graph with admission caps — data side. Anything that can bind an arbitrary expression — code lane. The check is mechanical: is the interpreter total, and does admission bound cost?
- Camera restated. Cameras are plural and can be material all the way down. What is floor is the input-custody court and the ambient escape — the guarantee that adjudication of "who gets this sample" and the way out are never capturable by material. wheel-route is that clause, running. [c]
- "Fails as a card" is too narrow a custody test. 3D shows refusal has tiers: admission refusal (throw/card), budget refusal (rung denial, receipted), semantic divergence (oracle fence). The custody test restates as: data is what enters through a declared checker at a declared strength, with a bounded blast radius when wrong. (This is the destination essays' "verified how hard" arriving from the substrate side — convergent, not imported.)

K5 survives as questions, not machinery. Inspect: confirmed, with a new axis — provenance grain is declared per family (per-object :asserted-by, pick declaring its owner and result shape; path declared knot-level source events; text has character offsets). [c] Alter: must produce settlement acts in the family's declared shape — op-diffs for 3D, candidates for faces — not universally candidates. Coin: birth is family-shaped too; 3D birth is add-object into a container, placement-first, not a ground gesture — a genuinely different shape from stroke or typing.

K6 untested by this comparison — honestly. Region3D has no durable lane at all; the diffs waiting in a session atom are evidence the family was designed for the shared funnel, not evidence it survives one. This clause stays believed [a] and unproven for 3D.

K7 reframed — this is the systems-level result. The six stations survive as interrogatives, not inheritances. And the tree already invented the form the general architecture should take, twice, independently: path's geometry declaration and region3d's citizenship block are questionnaires answered with declarations — grammar, edit operations, pick ownership, provenance mapping, cost regimes, version policy, migration windows, receipts. [c] Two families needing overlapping-but-different questionnaires is exactly the evidence pattern Sid's instruction asks me to respect: the recurring structure is the question set; the machinery is the answers, per family, preserved as differences.

★ Insight ─────────────────────────────────────
The general Softland authoring architecture — as far as one dissimilar comparison can establish it — is not a shared engine any family inherits. It is a citizenship contract: a fixed set of questions every material family must answer with declarations (authority, settlement profile, publication protocol, checker tiers and strengths, provenance grain, cost regimes and degradation ladder, birth shape, version law, containment behavior), plus a very small set of reference-plane invariants that hold across answers — the settle law, refusal visibility, provenance completeness, decision events at audience boundaries, addressable containment. Matter rooms are one family's answers, not the contract.
─────────────────────────────────────────────────

Two things 3D forces into the contract that my version lacked entirely: cost citizenship (render cost is first-class and refusable, with receipted degradation — rungs for regions, zoom regimes for paths: same slot, different ladders [c]) and containment as recurring structure (families are not parallel rows in a table; they nest by address — ground places regions, regions place text and ink, assemblies place prims — so the six-stations table was the wrong picture and composition is the deeper invariant). [c]

---
What this comparison does not settle

Left open, deliberately, until further instances survive:

1. Publication for heavy material. Activation-as-pointer-swap plausibly transfers (it's reference-level), but preview and review of a geometric candidate is unsolved anywhere — the room pattern projects state as text; "see what this candidate changes" for a scene diff needs a family-specific review face nobody has designed. The publication law is confirmed only where diffs are readable.
2. Refusal visibility for 3D product surfaces. Assemblies error-card by construction; region validation throws behind a dev flag. Whether a bad region row lands as a visible card in product is unbuilt, so the refusal law is attested at two families, not three.
3. The next dissimilar instances, named. The questionnaire hypothesis has now survived exactly one hard comparison (policy ↔ 3D, with text as a third point I initially misread). The two tests that would move it from open toward established: code atoms — material whose nature is open terms, the custody line's limiting case (its checker tier is tests/review/release; its "activation" is deploy — does the question set even apply, or does code get a constitutionally different citizenship?); and node-authoring, when it arrives, which strikes the closed-composition line at its exact designed collision point. Until at least code survives, "the general Softland authoring architecture" stays a hypothesis with good posture, not a thing to build against.

The five organs from last turn survive this pass, restated where the contract moved: the draft regime is strengthened (3D's :draft-settle field is a second independent witness that the two-regime life is wanted [c]); hands survive with birth declared per family; the reverse arrow now reads "pick + intent → settlement act in the family's shape"; the distiller and commission seam are untouched by this comparison. And per your standing style note, which I'd extend to everything above: the citizenship blocks, the op vocabulary, the rung receipts — all of it is evidence of what one careful designer needed, precedent for the questionnaire's sections, and none of it is yet the answer to what should be built.
Yes — it makes sense, and it's not a loose extra wish: it names a requirement the contract from the last two turns needs and didn't yet state. Let me say it back sharply so you can check I caught it, then place it.

The restatement. Every render regime keeps its own acts (orbit in 3D, caret in text, pan on ground), but there is also a small universal tier of acts that work everywhere at any depth — point, ask, select, inspect. When you dive into a 3D model and point at a sub-module, that pointing resolves to a real address at the grain you pointed at; the ask you make there is not trapped inside the 3D view — it routes to wherever asks live (a text block beside the region, a room, the canvas), and the answer can point back. And underneath all of it: one piece of data, many simultaneous views, all of them live in both directions — you can manipulate the material from any view that renders it.

That's three architectural claims, and here is where each lands:

1. "Per render engine actions but also a global one" — a two-tier verb law, and the tree already has the seed. The floor-reserved verbs (camera, the Halo meta-gesture) are the embryo of the universal tier — acts no material may capture, working over every surface [c]. Your requirement extends that tier from "escape and camera" to "point, ask, select" and gives it a growth law worth stating: family verbs grow at material speed (a new binding row), but every universal verb multiplies across all families — each family must declare how it lands on them — so the universal tier grows at constitutional speed and must stay tiny. This adds a section to the citizenship questionnaire: how do the universal verbs land on you, at what grain? Region3D has already half-answered for "point": its pick declares owner, grain (object + surface point + normal), and a routing result [c].

2. "Point at a sub module and the ask gets navigated outside" — this needs the one genuinely missing primitive: a composed address. Today every family has a local path — blocks have part-paths, assemblies stamp src-paths, regions have object-ids, paths have knot-ids [c] — but nothing composes them into one land-wide name for "that bolt, on that assembly, inside that region, on this ground." Deixis, asks, relation edges, and cross-view navigation all need exactly this one grammar: an address that walks containment, at declared grain, ideally carrying its basis (which revision of the region you were pointing into). This is a real new organ — smaller than the five from the first turn, but under several of them: the reverse arrow, the ask, and cross-view navigation all consume it. The routing of the ask itself has a proven precedent: the matter rooms already do "conversation about X lives elsewhere, anchored to X by address, answers land as durable material" [c] — your sub-module ask is that pattern with a composed address instead of a master-id.

3. "Multi bidirectional views" — one new invariant, and a structural reason it's cheap here. The contract already had the forward half (one authority, many disposable projections — K1). Your requirement adds the reverse half as an invariant: every view that renders authority is also a settlement surface over it — there are no constitutionally read-only "viewer" views versus privileged "editor" views; the text view of a module's parameters and the 3D view of its body are peers, both minting acts against the same material. And the reason this doesn't explode the way multi-view editing usually does: views never synchronize with each other. They all talk to the one authority through the settle law, and re-derive — hub-and-spoke, not n². Bidirectional n-view editing is the payoff of the authority/projection and settle clauses we just stress-tested, not a new cost.

★ Insight ─────────────────────────────────────
Your scenario also names why containment courts must generalize. "Zoom, pan, go deeper" through nested regimes revives the classic nightmare (which thing gets my scroll?) — and the tree's answer-in-embryo is the court pattern: wheel custody adjudicated per input sample, inner regime first, ambient fallback, receipts counting dual dispatches [c]. Generalized: every containment boundary is a court; the universal tier is the one set of acts no court may swallow. Depth becomes navigable because custody is adjudicated with receipts, not guessed.
─────────────────────────────────────────────────

Traced through your exact moment, so the gaps are visible: zoom/pan into the region — courts, exists [c]. Go deeper to a sub-module — pick at object grain, exists in fixture [c]. Point at it as an addressable thing the rest of the land can name — composed address, missing. Ask it something — universal ask verb, missing (exists today only for text blocks via Ctrl+Enter). The ask's conversation landing as text beside the region, linked both ways — room/anchoring pattern, exists for masters, not yet generalized. The answer pointing back into the model and the model highlighting — shared ground truth with cross-view deixis, which is the essays' "ground shared, salience plural" clause meeting the composed address.

Two honest hard parts, named as the actual thinking work rather than smoothed over: composed addresses must survive change — re-parenting an object, revising a region, migrating a schema — so the grammar needs to be basis-aware from birth or the whole point-and-ask economy rots into dangling references (this is the "you cannot retrofit provenance" law wearing its addressing face); and custody arbitration is a felt design problem as much as a mechanical one — the courts can be correct and still feel wrong, which is exactly the kind of thing the draft regime exists to let you tune by living in it.

Net effect on the adjudication from last turn: nothing broken reopens. This strengthens the two clauses I'd promoted (containment as recurring structure; settle law as the bidirectionality enabler), adds one reference-plane invariant (views are peers over authority), and adds two questionnaire sections (universal-verb landing; address composition and grain). The general architecture stays open as agreed — but your requirement just told us what its connective tissue is: the composed address is the thing every other organ was quietly assuming.
The answer has a sharp shape, because the last three turns already forced it: an action never actually "crosses" — the gesture stays in one view, and the only thing that travels is an address. Everything else is each family doing its own declared job on either side of that address. Here is the whole thing: the trace, the ownership, and the minimal kernel.

The compressed answer

1. Input lands on the glass and walks the containment chain of courts, innermost first, until one regime takes custody.
2. That walk itself mints the composed address — each container it passes through contributes its segment.
3. The owning family's pick names what you touched, at the grain that family declared.
4. Binding data resolves gesture + site to a verb; universal verbs come from the uncapturable floor.
5. While the gesture runs, everything is session-local projection in the hosting view.
6. At settlement, the act is minted in the subject's family's shape, not the view's — and goes through the one funnel.
7. Anything the act creates in another family is born in that family's own lane and linked back by a relation edge to the address — never embedded, never copied.
8. Every view projecting the touched authority re-derives. Views never talk to each other.

The trace — your scenario, step by step

Point at a bolt inside a 3D model, ask about it, answer lands as text outside:

┌─────┬─────────────────────────────────────────────────────┬─────────────────────────┬──────────────────────┐
│  #  │                    What happens                     │       Who owns it       │        State         │
├─────┼─────────────────────────────────────────────────────┼─────────────────────────┼──────────────────────┤
│     │ Pointer sample → one typed packet                   │ platform edge (code,    │                      │
│ 1   │ (css/region-local/device derived together,          │ floor)                  │ exists [c]           │
│     │ un-cross-pairable)                                  │                         │                      │
├─────┼─────────────────────────────────────────────────────┼─────────────────────────┼──────────────────────┤
│     │ Court walk: region court asked first, ground is     │                         │ exists for wheel,    │
│ 2   │ fallback, decision receipted                        │ courts (code, floor)    │ one instance [c] —   │
│     │                                                     │                         │ needs generalizing   │
├─────┼─────────────────────────────────────────────────────┼─────────────────────────┼──────────────────────┤
│     │ Region's pick: ray → BVH → object-id + surface      │                         │ exists in fixture    │
│ 3   │ point + normal, 6px slop, per its citizenship       │ the 3D family           │ [c]                  │
│     │ declaration                                         │                         │                      │
├─────┼─────────────────────────────────────────────────────┼─────────────────────────┼──────────────────────┤
│     │ The walk's segments compose into one address:       │ the address grammar     │ missing — the one    │
│ 4   │ ground-slot / region-id @ rev / object-id /         │ (common)                │ new noun             │
│     │ surface-param, basis-stamped                        │                         │                      │
├─────┼─────────────────────────────────────────────────────┼─────────────────────────┼──────────────────────┤
│     │ Gesture + site → claim → verb. ask is floor-tier    │ binding material (data) │                      │
│ 5   │ (like Halo/camera today); family verbs come from    │  + verb registry +      │ dispatch exists [c]; │
│     │ worn binding rows, containment fallthrough already  │ phase machine (code)    │  ask verb missing    │
│     │ does the walking                                    │                         │                      │
├─────┼─────────────────────────────────────────────────────┼─────────────────────────┼──────────────────────┤
│     │ The ask's effect class is readable from the row     │ verb registry           │ exists as mechanism  │
│ 6   │ before anything runs — durable-via-request, so you  │ declaration             │ [c]                  │
│     │ know truth may move                                 │                         │                      │
├─────┼─────────────────────────────────────────────────────┼─────────────────────────┼──────────────────────┤
│     │ Settlement: your ask births a text unit through     │                         │ funnel exists [c];   │
│ 7   │ text's own birth lane, positioned by                │ the subject/target      │ generalized ask      │
│     │ positioned-policy beside the region — 3D never      │ family + the one funnel │ missing              │
│     │ gains a text lane                                   │                         │                      │
├─────┼─────────────────────────────────────────────────────┼─────────────────────────┼──────────────────────┤
│     │ A :references edge banks from the new unit to the   │                         │ exists for           │
│ 8   │ composed address; "success reported only after both │ relation kernel         │ block-grain subjects │
│     │  truths are queryable" — the matter-say precedent,  │                         │  [c]                 │
│     │ verbatim                                            │                         │                      │
├─────┼─────────────────────────────────────────────────────┼─────────────────────────┼──────────────────────┤
│     │ The resident is summoned with a portal briefing     │                         │                      │
│ 9   │ narrowed to the address (the narrowed-portal        │ invocation lane         │ exists at block      │
│     │ precedent); its answer lands as durable machine     │                         │ grain [c]            │
│     │ blocks                                              │                         │                      │
├─────┼─────────────────────────────────────────────────────┼─────────────────────────┼──────────────────────┤
│     │ The region re-derives and may render a mark at      │ each family's           │ per-family,          │
│ 10  │ addresses that carry edges; your view and mine may  │ projection; salience    │ correctly never      │
│     │ light it differently                                │ per mind                │ common               │
└─────┴─────────────────────────────────────────────────────┴─────────────────────────┴──────────────────────┘

And the reverse-direction crossing, three lines, because it proves the load-bearing rule: you edit placed text inside the 3D scene. The region holds only {:address …} — a placement ref [c]. Your keystrokes, gestured inside the 3D view, mint text acts through the text artery; the region's material never changes at all; the region simply re-derives its placement when text's authority moves. The view hosts the gesture; the subject's family owns the truth. That one rule is what makes "bidirectional multi-view" coherent instead of a sync nightmare.

The three crossing rules

These are the whole discipline; every step above is one of them applied:

- The view never owns the subject. The hosting regime supplies pick and gesture-time projection only; settlement is always in the subject family's declared shape, against the subject's authority.
- Crossings are births plus edges, never copies. Output belonging to another family is born in that family's lane and linked to the composed address. No family embeds another's material — it places references.
- The address is the only traveler. Families never call into each other's internals; everything between them is: resolve this address, or link to this address.

The smallest common machinery

Strictly minimal — what must be shared for any of this to work, and nothing more:

┌─────────────────┬─────────────────────────────────────────────────────────────────┬────────────────────────┐
│      Piece      │                           What it is                            │      Build state       │
├─────────────────┼─────────────────────────────────────────────────────────────────┼────────────────────────┤
│ One funnel      │ request → decision → event, actors, capabilities — all          │ built [c]              │
│                 │ settlement of every family rides it                             │                        │
├─────────────────┼─────────────────────────────────────────────────────────────────┼────────────────────────┤
│ One dispatch    │ sites → claims → verbs, begin/move/end, effect classes, floor   │ built [c]              │
│                 │ tier                                                            │                        │
├─────────────────┼─────────────────────────────────────────────────────────────────┼────────────────────────┤
│ Relation edges  │ asserted-by-carrying links — the only cross-family glue         │ built [c]              │
├─────────────────┼─────────────────────────────────────────────────────────────────┼────────────────────────┤
│ The court walk  │ custody adjudication at every containment boundary, receipted,  │ embryo: one instance   │
│                 │ floor uncapturable                                              │ (wheel) [c]            │
├─────────────────┼─────────────────────────────────────────────────────────────────┼────────────────────────┤
│ The composed    │ basis-stamped chain of family-opaque segments, minted by the    │ missing                │
│ address         │ same walk that adjudicates custody                              │                        │
└─────────────────┴─────────────────────────────────────────────────────────────────┴────────────────────────┘

Five pieces. Three are built and already family-agnostic. The court exists once and needs to become the shape of every containment boundary. The address is the only genuinely new invention — and note the economy: custody and addressing are two outputs of one traversal, so the missing piece isn't a new subsystem, it's making the walk that already happens emit the name of where it walked.

★ Insight ─────────────────────────────────────
Everything else that looks like it might need to be common — preview, re-derivation, settlement shape, pick geometry, refusal style, salience — is deliberately not in the kernel. Those are per-family answers to the citizenship questionnaire, held in place by laws (settle law, refusal visibility) rather than shared code. The kernel stays five pieces precisely because the contract pushed every family-shaped thing out of it; that is the "smallest" in your question, achieved by the adjudication we just did, not by hope.
─────────────────────────────────────────────────

So the direct answers: what happens is the ten-step trace — walk, name, claim, project, settle, link, re-derive. Who owns each step: floor owns the glass, the courts, and the universal verbs; the hosting family owns pick and gesture-time projection; the subject's family owns settlement shape and truth; the target family owns any birth; the relation kernel owns the linkage; every projecting family owns its own re-derivation. The smallest common machinery is the funnel, the dispatch, the edges, the generalized court walk, and the composed address — of which only the address is new, and one court needs to become many.


SESSION NAME:  softland-product-architecture-should-be-one-shot


All four mapping reports are in and cross-checked against my own reading. Here is the synthesis.

---
The simple story first

1. Softland today is two half-built machines: a machine of record and a machine of showing.
2. The machine of record — files cut into addressable blocks, edits as revisions, edges between things, marks on things — exists for text only.
3. The machine of showing — one store, one picker, one input law, one renderer — already serves all three media, and it is genuinely general: it has a closed, validated contract for adding a medium.
4. Drawing and 3D live only in the machine of showing: you can see them and (for 3D) touch them, but the land has no record that they exist.
5. So a stroke or a 3D sub-part cannot be linked, marked, cited, asked about, or found again — not because views are missing, but because addresses are.
6. The one place the two machines meet outside text — the seam demo — already proves the target capability in miniature: the 3D scene holds a reference to a text address, so typing in the 2D paragraph moves the 3D mirror live.
7. What genuinely recurs across the three media is not a shared document shape; it is a shared contract: everything has an address (what it is), an anchor (where it lives inside its parent material), and occurrences (where it currently appears) — and parts are minted when a mind points at them, not enumerated in advance.
8. What stays per-medium is the geometry under that contract: what a "part" is, how material is cut, laid out, edited, and packaged for a mind to read.
9. The missing product architecture is five pieces: the record-side medium contract (ink and 3D become substance), placements as durable substance so "where else does this live" is a query, ask as a verb over an address-set at a chosen scope, basis edges with served staleness for anything a mind derived, and time as a parameter of reads.
10. None of these require new kinds of machinery — each generalizes a mechanism that already runs for text.
11. Once they exist, your walkthrough is literal: point inside the model, ask, jump to the spec paragraph, edit it, and the model wears the staleness honestly instead of pretending to recompute.

Everything below expands these, in the structure you asked for. Provenance discipline: claims tagged [checked] were read directly by me or by a mapper with file:line receipts; [derived] is my systems-level inference from checked facts; [assumed] is marked where it appears. Per your fence I read neither the direction docs nor vision; the two essays were read as commissioned.

---
The three media, one level above

The dissimilar test worked — the three media disagree exactly where a lazy generalization would need them to agree.

┌──────────────────────────────────┬─────────────────────────────────────────────────────────────────────────────────┬──────────────────────────────────────────────────────────────────────────────────────────────────────────┬─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                  │                                      Text                                       │                                          2D drawing (path/ink)                                           │                                                     3D (region)                                                     │
├──────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Durable record                   │ Full spine: surface → distiller → units+anchors → revisions → relations/marks   │ None. Client rt-tree literal behind ?live-atoms=1 [checked]                                              │ None. Client rt-tree literal behind ?region3d=1 [checked]                                                           │
│                                  │ [checked]                                                                       │                                                                                                          │                                                                                                                     │
├──────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ What a "part" is                 │ An interval in an ordered stream, cut by a versioned distiller                  │ A stroke/knot/contour (authored), or an emergent region ("the face in the sketch") that exists in no     │ A named object in a hierarchy, or a volume/surface point in local space                                             │
│                                  │                                                                                 │ data structure                                                                                           │                                                                                                                     │
├──────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Sub-addressing declared          │ Tagged UTF-16 offsets, line ids, cluster spans, three block-path grammars       │ :knot/id, :contour/id + point index — stable, enforced unique [checked]                                  │ :object/id, parent graph, plus local [x y z] anchors [checked]                                                      │
│                                  │ [checked]                                                                       │                                                                                                          │                                                                                                                     │
├──────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Sub-addressing reachable at      │ Caret/selection spans work end to end [checked]                                 │ Pick collapses to a whole-node boolean; knot ids unreachable from a gesture [checked]                    │ The strongest: pick returns object + triangle index + placed-material routes [checked]                              │
│ runtime                          │                                                                                 │                                                                                                          │                                                                                                                     │
├──────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Edit vocabulary                  │ Live, three roads, one durable (block edit → revisions) [checked]               │ Five semantic ops implemented, zero live callers — "a fully specified medium with no author" [checked]   │ Nine ops implemented; gizmo settle mints invertible, provenance-carrying diffs — then discards them in a session    │
│                                  │                                                                                 │                                                                                                          │ atom [checked]                                                                                                      │
├──────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Mind-readability                 │ Native — quote the bytes                                                        │ Not symbolic; needs a render and/or a structural census                                                  │ Half-symbolic (kinds, names, params, transforms) + needs renders                                                    │
└──────────────────────────────────┴─────────────────────────────────────────────────────────────────────────────────┴──────────────────────────────────────────────────────────────────────────────────────────────────────────┴─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

Three observations fall out of the comparison, and they drive everything after:

First, the render substrate is already the proof that recurrence-by-contract works. scene_tape has a closed family registry with seven obligations (grammar, pick predicate, regimes that must tile the zoom range, color law, produce/execute, receipts), and text, paths, and 3D all satisfy it without sharing any representation [checked]. Nobody forced strokes to be blocks or meshes to be text to get them on screen; the contract recurred, the geometry stayed native. That is the exact shape the substance side is missing.

Second, the identity machinery is already split three ways, and the split is the load-bearing thing. Selection identity is {:vi, :address}; the store keeps an incrementally-maintained address → view-instances index plus targets-by-address (every on-screen occurrence with bounds); the container kernel keeps anchors (exact coordinates of a unit inside its parent surface) [checked]. Address, anchor, occurrence — three different lifetimes, three different owners.

★ Insight ─────────────────────────────────────
The recurring identity contract is a triple, and each leg has a different mutability: the address is what a thing is (durable, medium-blind, the thing edges and marks attach to); the anchor is where it lives inside its parent material (durable, medium-typed: a UTF-16 span, a knot range, a scene-path + local volume); the occurrence is where it currently appears (ephemeral, per view, what connectors and picks resolve against). Every cross-projection feature in the codebase that works — the 2D↔3D mirror, connectors, marks, selection — works precisely because it attaches to the right leg. Every gap — unlinkable strokes, unfindable 3D parts, page-reload navigation — is a leg missing for that medium.
─────────────────────────────────────────────────

Third, the one cross-medium composition that exists is by address reference, and it is bidirectional already: a 3D scene object of kind :text or :ink carries {:ref {:address …}} and re-renders through the same layout/tessellation kernels the 2D canvas uses, with in-flight edits mirrored; and 3D object positions project back into the 2D connector anchor space so an arrow can follow a gizmo drag [checked]. This settles a design question by existence proof: projections don't need to share geometry to stay connected — they need to share addresses and each redraw in their own idiom.

---
Q1 — What genuinely recurs at the systems level

Eight laws survive all three media plus the two non-content projections (time, briefing). All are [derived] from checked mechanisms; where a law is already implemented I say where.

1. The address/anchor/occurrence triple (above). Implemented fully for text, partially for 3D (occurrence + in-scene ids, no durable leg), grammar-only for ink.

2. Parts are minted on demand, idempotent by region-identity — never pre-enumerated. The container kernel already lives this law for text: refine! treats (surface, span) as the identity, minting a unit when a mind addresses a sub-span and resolving to the existing unit when the span coincides with one [checked]. This generalizes because it refuses the thing the dissimilar test forbids — a shared part structure. Each medium supplies its own region-identity type; the law (mint-on-address, idempotent, resolve-don't-duplicate) is the shared part.

★ Insight ─────────────────────────────────────
Demand-minting is what makes ink's emergent parts addressable at all. "The face in the sketch" exists in no data structure and never will; pre-cutting can't find it. But the moment a mind lassos it or an agent names it, (drawing, region) becomes a minted unit with an anchor — exactly as a text span becomes a refine: unit today, and exactly as the seam demo's edge already lands on (region, object, local [x y z]) by hand. Parts come into existence by being addressed. That single law covers authored parts (3D objects, strokes), cut parts (text blocks), and perceived parts (regions of a drawing) without forcing one ontology on them.
─────────────────────────────────────────────────

3. Relations live between addresses; every view resolves endpoints to its own occurrences, in its own idiom. Implemented: the relation kernel is target-kind agnostic (unknown kinds fall through to verbatim ids), edges carry asserter-in-identity, two clocks, evidence anchors; the connector router expands one durable edge over every visible from×to occurrence pair [checked]. The 3D idiom (a tether to a local anchor) already exists in the demo. Time's idiom (a line between trail cards) and the briefing's idiom (a citation) are the same law wearing different clothes.

4. One dispatch law; medium-owned verbs; declared effect classes. A pick yields a containment chain of claims, innermost first, always ending at the space; binding rows (instance / master / floor tiers) map gestures to named verbs; the verb registry declares effect class and required args; the four-station law has exactly one side-effecting site [checked]. This is medium-blind by construction — nothing in it knows about text. Today only blocks and the space stamp claims; region3d has parallel hand-wired handlers [checked] — evidence the law generalizes and evidence it hasn't been asked to yet.

5. Projection is a pure, honest read; arrangement is data; the record is never the view. Twelve projections behind one registry, all producing plain data-contexts that name their own lacks (:truncated?, :paging-lack, three distinct first-light states); faces and block anatomy are arrangement-only EDN over closed vocabularies, compiled totally with error cards [checked]. The map-must-not-lie discipline is mechanical, not aspirational — the portal literally computes which of its own questions went unanswered [checked].

6. Placement is wear on an occurrence, never content. Canvas geometry cells, the positioned facet, 3D placement transforms — in every medium the same split: content at the address, pose on the occurrence [checked]. This is what makes N simultaneous views of one thing coherent (two reader-face instances of one conversation already echo together live [checked]).

7. The judgment layer attaches to addresses and is already medium-blind. Marks in three strata (receipt = mechanical co-presence, silver = machine proposal, gold = Sid's assertion), edges with asserter identity, activation grounds with three-valued honesty, and the one genuine as-of in the system: receipts resolve what material was worn at capture time, never substituting current state [checked]. Nothing here would change to serve a stroke or a bolt — it only needs targets to exist.

8. Both species read the same projection. The resident's briefing is the canonical EDN of the same portal result the human renders, byte-equal, no model in the path, narrowing server-authoritative [checked]. That is the "agent briefing" projection already obeying the essays' law: the agent view is a projection contract, not a privileged API. The generalization (packagers per medium, below) must preserve this.

Time deserves its own sentence: the record is already event-sourced everywhere (append-only revisions, retraction-not-deletion, two clocks rendered honestly), so time recurs as a property of the record, but no read surface accepts a cut — trail-view's contract literally says "NO as-of," and the one scrub (until-ms) works within a single 64-block page [checked]. Time is a projection whose substance already exists and whose lens doesn't.

---
Q2 — What must remain specific to each medium

The bespoke list, verified by the media report, is short and principled — each item is bespoke because it is the medium:

- The surface form and the cut: what an immutable captured artifact is (char sequence, stroke log, scene description, mesh blob) and the versioned distiller that cuts it into units, including the per-medium path grammar. The codebase already runs three incompatible block-path grammars (positional for prose, name-stable for code, event-coordinates for transcripts) inside one id scheme [checked] — proof that path semantics must stay per-medium while the id grammar is shared.
- The anchor coordinate type and the re-anchor law under edit: spans shift under splice; knot indices shift under stroke-split; scene paths survive reparenting differently than volumes do. Each medium owes its own answer to "what happens to an anchor when the ground under it is edited," including the honest dangling state (the kernel already has the pattern: the deterministically-never-minted pending-result hole id [checked]).
- Layout/evaluation math, cache keys, GPU layout, narrow-phase pick — already cleanly per-family behind the substrate contract [checked].
- The edit-verb vocabulary and the gesture interior: splice is not transform is not draw. Verbs stay medium-owned; only dispatch, effect classes, and the request envelope are shared. (The camera stays floor-reserved per medium idiom — scroll, pan/zoom, orbit/enter — exactly as the registry already does for 2D [checked].)
- The packager for minds: how a unit becomes context an agent can read. Text quotes its bytes. A drawing must ship a render plus a structural census (strokes, bounds, the classifier's tri-state geometry). A 3D part must ship renders plus the scene slice (kind, params, transform, parents, placed refs) plus its relations. This is the medium-specific half of "ask"; the loop around it is shared.

And three generalizations the dissimilar cases refuse — worth stating because each is a natural next mistake:

- No universal part structure. "Everything is blocks" loses named 3D hierarchy; "everything is a scene graph" loses demand-cut spans; both lose emergent ink regions. The recurrence is the contract (triple + demand-minting), never the shape.
- No universal edit language. A CRDT-style one-op-set-fits-all collapses under transform-vs-splice. The envelope, idempotency, and revision accretion are shared; the op vocabulary is not.
- No special agent surface. The byte-equal briefing law already refuses a parallel machine-only API; packagers extend what agents see, not where they stand.

---
Q3 — What belongs in mutable data, stable code, or a tool

The codebase has already discovered its own custody pattern — closed grammar in code, revisions in data, a code floor no data can breach, teaching refusals at the boundary (anatomy, bindings, facet masters all follow it [checked]). Extending that pattern:

Stable code (the kernel): the container fold and request envelope; the id/address grammar and per-medium anchor resolvers; the dispatch law and effect classes; the render substrate (tape, frame graph, renderer, pick); the per-medium geometry kernels (shaper, tessellator, scene evaluator, BVH); camera verbs; the distiller interfaces.

Mutable data: content itself (surfaces, revisions); relations and marks; placements and views (this is the promotion — today they're runtime state); faces, anatomy, binding rows, skins (already data); briefings and every mind-made derivative (as substance with basis edges); which distiller cut a given surface (already data: distiller-id rides every unit [checked]).

Tools: importers (mesh, SVG, image-trace, transcript watchers — the watcher pattern already exists [checked]); heavyweight bakes; and the minds themselves — note the verb registry already reserves an effect class for exactly this, :external-via-derived-worker, currently worn by no verb [checked], and the one cascade in the system (ambient autotag after every durable turn) is its living precedent [checked].

---
Q4 — The missing architecture, concretely

Five pieces. Each is named with its nearest existing mechanism, because in every case the mechanism exists for one medium or one hop and the missing thing is its generalization — existence as evidence, not inheritance: what I'm claiming from the code is that these are buildable and consistent with the land's laws, not that the current implementations must be kept.

M1 — The substance-side medium contract. The mirror of the render-side family registry, for the record: a medium enters the land by declaring its surface form, its distiller, its anchor coordinate type + resolver + dangling policy, its demand-mint region-identity, and its edit ops as revision-producing requests. For ink and 3D the distance is startlingly short [checked]: both already have fail-closed grammars, stable internal ids, implemented edit operations, and (for 3D) settle-time diffs that carry :before/:after and provenance — dying today in a session atom one write-lane short of durability. The block-edit lane (deterministic request ids, idempotency keys, server-side hashing, WAL, ack/refuse/revert) is the template [checked]. When M1 exists, everything in the judgment layer — edges, marks, autotags, portals — serves strokes and bolts with zero changes, because it never knew about text in the first place.

M2 — Placements as durable substance, and the occurrence graph as a query. Today "where does X appear" is answerable only inside the current client frame; views are runtime spawns; moving between projections is a ?drill= page reload, named as deliberate MVP [checked]. The missing object is the placement row — (address × view/region → pose) — which already exists as a data shape in the 3D scene's placed-text objects and in the canvas geometry lane; it is just not a queryable, cross-view substance family. On top of it: the navigation verb family — reveal occurrences of this address; summon it here; open it in its medium-native view at this sub-address — riding the same dispatch law as every other verb. This is the piece that turns "reach its connected textual or spatial representation" from a demo wiring into a product act, and it is also what makes the land's spatial memory real: things live somewhere, findably, across sessions and across views.

M3 — Ask as a verb over (address-set × scope), with per-medium packagers. The loop exists end to end for exactly one subject shape — a conversation turn: deictic receipt captured, portal narrowed server-side, briefing composed, resident spawned, reply distilled into blocks, autotag cascade proposing silver [checked]. The generalization: the subject becomes any address-set (your current selection already is one — {:vi, :address} members [checked]), and scope becomes a first-class argument — roots × relation-kinds × containment depth, computed server-side over composition edges and the relation kernel, so "this bolt," "this bolt's assembly," and "everything that stands on this spec" are three values of one parameter rather than three features. The packager registry is the per-medium half (Q2); answers land as what answers already land as — blocks, edges, marks — anchored to the asked addresses, visible in every projection that shows them. Local-versus-global manipulation is the same generalization applied to verbs: a verb over a set the scope computed, with the effect-class law deciding what may run without asking.

M4 — Basis edges and served staleness for mind-made derivations. The land already distinguishes, without naming it, two kinds of cross-projection consequence:

★ Insight ─────────────────────────────────────
Consequences propagate two ways, and conflating them is the classic lie of "synced" tools. Reference consequences recompute: the 3D mirror follows the 2D paragraph because both dereference one address — code does the work, the result is correct by construction (live today at the seam [checked]). Derivation consequences cannot recompute: a briefing about the manifold, a machine-cut structure, a diagram an agent traced from the model — a mind did the work, so when the ground moves the honest system does not silently regenerate; it marks the derivative stale, visibly, with the diff that stales it, and repair is a new act by a mind, on the record. The relation kernel already has the edge kinds (:based-on, :assembled-from, :grounds) and circulation already proves as-of resolution [checked]; what does not exist is the law that every mind-made artifact is minted with basis edges at pinned revisions, and a served reverse-dependency staleness projection — "what moved under this." That projection is the mechanical half of the essays' basis-diff, scoped to product size.
─────────────────────────────────────────────────

This is what makes "see the consequences across the others" true and honest in your walkthrough: the spec edit doesn't remodel the manifold (no reference), it stales the two derivations that stood on the spec — and where no basis edge exists, the system claims nothing, which the current codebase's honesty discipline already demands of every other projection.

M5 — Time as a read parameter. The record supports it everywhere (append-only revisions with order keys, activation history, two clocks, receipts that resolve as-of); no read surface accepts it [checked]. The missing lens: address@cut resolution as an optional parameter of the standard reads, so the trail stops being a separate feed and becomes what it structurally already is — a projection of the same land along the one dimension every act already carries. Then scrubbing is navigation (other open projections re-resolve at the cut), a trail card is deixis (its addresses light up where they live), and "what did this look like before Tuesday's session" is a read, not an archaeology. The storage bill for this was already paid by the append-only decisions; only the lens is missing. [derived]; the one caveat: current-pointer PStates are overwritten in place, so at-cut reads for pointers need the revision history walk the activation machinery already does for materials [checked], generalized — a real cost, not a blocker. [assumed: no hidden compaction discards old revisions; I saw no evidence of any.]

---
The walkthrough, replayed against this

Your scenario, with each beat marked — because the commission asked for something you can picture:

You double-click into the engine model and orbit to the intake side (exists: region focus, orbit, dolly — session-only today). You click the manifold — the pick already answers with the object, the triangle you touched, and the route it took (exists, the strongest sub-addressing in the land). You press ask — the pick's containment chain becomes the address-set, scope defaults to the object and one ring of relations (M3); the packager ships two renders, the scene slice, and its edges; the resident's reply lands as blocks anchored to the manifold's address (the landing machinery exists; the address is M1). A tether appears from your question to the manifold's local anchor (the demo's edge, generalized). You ask where else does this live — the occurrence query answers: the spec paragraph in the design doc, a card on the north canvas, one briefing from last month (M2). You jump to the spec paragraph (M2's summon verb; today this is a page reload) and edit it (exists: the durable block-edit lane, acked, WAL'd). Back in the model, nothing silently remodels — instead the manifold wears a stale seam: ground moved under 2 derivations, and pressing it opens the diff between the spec revision the briefing stood on and the one you just made (M4). You drag the manifold's gizmo; on release the settle diff — the one the code already mints and discards — lands as a revision (M1), the arrow from the spec follows (exists), and the trail shows both acts, your name on each, claimed and arrival clocks both honest (exists). A week later, "what changed under what I stand on" is the morning read (M4 + M5).

Every beat is either running today or one of the five named pieces. That, I think, is the answer to "make the missing product capability concrete": the capability is not a new surface — it is the record catching up to the showing, at which point the projections you already have stop being windows onto three different worlds and become five lenses on one.

---
The compressed whole: the render side proved that dissimilar media can share a strict contract without sharing a shape; the substance side has that contract for text alone. Recurring: the address/anchor/occurrence triple, demand-minted parts, relations resolved per-view, one dispatch law, honest pure projections, placement-as-wear, an address-anchored judgment layer, one briefing for both species. Per-medium forever: surfaces, cuts, anchor types, edit vocabularies, packagers, cameras. Missing: the substance-side medium contract, durable placements with navigation verbs, ask over scoped address-sets, basis-staleness for mind-made derivations, and the cut as a read parameter. Nothing lands on disk from this session unless you say so.

-----END------
