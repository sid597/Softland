# UI design pass — opening prompt (paste into a FRESH session)

Written 2026-07-06 in the product-side session, on Sid's ask. Paste the
block below verbatim into a new Claude session. Anywhere this prompt and
the repo disagree, the repo wins.

---

put on the principal-designer hat — this is the explicit manual invoke
(/principal-designer). this session is the UI DESIGN PASS for the trail
view, and ONLY that — no code, no src/ edits, no framework work.

**what i mean by UI work (this got misread once already, so be precise):**
NOT the legibility batch — titles-in-line, readable names, clamped labels
is surface repair and it rides the framework wave elsewhere. i mean design
from the TOP: how do nodes and relations LOOK — horizontal or vertical on
screen? how does the world look when all is closed, when a few are open,
when all are open? how does the container look — corners, color scheme,
font sizes, density? from imagining the top, down to the bottom. and only
then: does this design output work in our current framework?

**the honest baseline (established 2026-07-06; verify against the repo):**
the current on-screen look — vertical feed, bands, lanes, cards, kraft
connectors — was NEVER designed. it's engineering defaults out of the
R-1/R-2 contracts, loosely grounded in my wall panel (D-002). the design
canon (settlement thesis, merged laws, taste) sits at philosophy altitude.
the MISSING MIDDLE LAYER — the canon laws cashed out into a concrete
visual system — is what this session exists to build. vocabulary:
machinery-done ≠ form-done; this session is the form side.

**roles:** the top layer (what it should feel like) is MINE — i judge by
feel, over real material. your job is the middle layer: generate genuinely
different, coherent visual systems and render them so i can look and feel.
junior execution comes later, not in this session. you propose systems;
i pick. don't converge on one answer and sell it to me.

**read first (in order):**
1. the design canon: `docs/current-mental-model/design/claude/` —
   capstone `softland-at-scale-synthesis-2026-06-10.md` + taste + the
   design decision log (the principal-designer skill boots these)
2. what exists today: `vision/images/2026-07-06-r2-first-light.png` +
   `docs/current-mental-model/build/trail-room/FIRST_LIGHT_R2.md` —
   this is the DEFAULT you are designing over, not a design to extend
3. `docs/current-mental-model/decisions.md` — D-002 (first form = the
   trail view; my wall panel is the origin) and D-010 (three frontiers:
   rama · ui · framework; my plain map). closed decisions stay closed:
   you design the trail view's FACE, you don't re-litigate what it is.
4. `docs/current-mental-model/design/claude/sitting-3-queue-2026-07-05.md`
   — the band-2 semantic-text / commit-gesture question and the doors
   item are fragments of exactly this pass; fold them in
5. `vision/LOG.md` recent entries, if you need the why in my words

**the feasibility envelope (pre-verified 2026-07-06 against
`src/app/client/substrate/webgpu/renderer.cljs`; spot-check if you doubt,
don't re-derive from scratch):**
- **tier 1 — expressible TODAY:** SDF rounded boxes with per-corner
  radii, borders, gradients, a dedicated shadow pipeline, per-instance
  color, MSDF/slug text at any size. container aesthetics are
  essentially free.
- **tier 2 — lands as DATA once the face-2 framework wave ships:**
  layout paradigm = a named projection (horizontal vs vertical = two
  registry entries over the same data); open-state grammar = band
  policy + folds in a spec. design freely here — the framework is being
  built precisely so these become data edits, not rewrites.
- **tier 3 — needs renderer growth** (curved connectors, animation,
  anything else): DREAM FULLY and label it a dream. D-001 governs
  build, never dreaming. demand becomes lawful when a lived-in form
  breaks, not when a mockup wants it.
- tag every element of every proposal tier-1 / tier-2 / tier-3.

**deliverable:** 2–3 GENUINELY different visual systems for the trail
view — different answers to orientation and node/relation grammar, not
one system in three color schemes. each rendered (HTML artifact
side-by-sides are fine) at the three states — all closed / few open /
all open — over REAL material from the corpus (use the actual commits,
threads, and typed edges visible in the first-light screenshot; NEVER
lorem ipsum — i evaluate by feel and the medium is the message). each
system carries its rationale traced to canon laws, plus tier tags.
artifacts live under `docs/current-mental-model/design/claude/`.
decisions i make land in the design decision log marked
LAW/STANCE/DEFAULT/OPEN.

**guardrails:** no code, no src/ edits — the framework wave may run in
parallel in another session; zero collision by construction. docs only
on this local branch, never pushed. analyze failure modes of my ideas
before validating them. never read `src/app/server/env.clj`.

**start** by reading, then name the 3–5 real FORKS in the design space
(orientation, node grammar, time encoding, open-state mechanics —
whatever you actually find) BEFORE rendering anything. i want to feel
the shape of the space before we spend tokens on mockups.
