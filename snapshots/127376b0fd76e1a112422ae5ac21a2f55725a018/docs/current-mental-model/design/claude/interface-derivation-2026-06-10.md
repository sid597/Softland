# What Kind of Interface Does This World Need?

A clean-room derivation. Claude, 2026-06-10. Independent track — produced without
reading `design/` siblings, `*DESIGN_RESEARCH*` docs, or `next-prompt.md`.

---

## 0. Method and independence

**Read:** `docs/current-mental-model/README.md`; `architecture/object-container-spec.md`;
a code survey of `src/app/server/rama/object_container/` (markdown adapter 520 lines,
transcript adapter 623 lines, core module ~2,650 lines, runtime queries 329 lines,
~3,500 lines of tests), the client workspace code, and targeted greps.

**Did not read:** anything under `design/`, the two `*DESIGN_RESEARCH*` docs, or
`next-prompt.md`. Two disclosure notes for honesty:
1. The README's read-order contains one-line summaries of the quarantined docs
   (titles only: "imported-topology-view" and "knowledge-earth-zui"). I know those
   docs exist and roughly what genre they are. I did not read them. Where my
   conclusions contradict the *title* of one ("knowledge-earth-zui"), that is a
   genuine contradiction of an active track, made blind.
2. My session auto-loads the project memory index, which contains vocabulary from
   prior syntheses ("lawful compression," "attention is content"). Where an idea
   here overlaps that vocabulary, I re-derive it from the spec/code and say so.

**Inference flags:** claims about HCI history (Pad++, desert fog, Figma, Lynch) are
from general knowledge, not from reading sources during this session — marked
*(inference)*. Claims about your code are from reading it this session — marked by
file path.

---

## 1. What the world must DO for its inhabitant

From the fixed goals (place, public form, papers-as-logs, replayable history,
preserved disagreement) plus the two test corpora plus the substrate, the jobs are:

| | Job | The question it answers |
|---|---|---|
| J1 | **Orient** | "What is here?" — survey a corpus you didn't arrange (the silo census) |
| J2 | **Connect** | "How do these relate?" — cross-silo identity and relation |
| J3 | **Trace** | "How did this come to be?" — provenance, causality, replay |
| J4 | **Adjudicate** | "What do I believe?" — accept/reject candidates, hold disagreement, calibrate |
| J5 | **Compose** | Arrange, relate, annotate; build new understanding from old |
| J6 | **Hand off** | Package a region of understanding so another mind can enter it |
| J7 | **Return** | Come back after a week, re-find where you were, marks intact |

J7 is the operational meaning of "place." A tool is something you pick up; a place
is somewhere your stuff stays where you left it.

The two test corpora are **differently shaped**, and that is a constraint on the
interface *kind*, not just on views:

- **Softland-itself** is *history-shaped*. "Real understanding of how this project
  got here" is a question about causality through time: conversations → decisions →
  docs → code. The corpus is literally the project's own construction log
  (transcripts, doc revisions, commits).
- **The DG project** is *state-shaped*. Claims, people, support/contradiction —
  "what is the current standing of this question" — mostly indifferent to the order
  in which it was learned.

Any interface kind that privileges one shape structurally fails one test case.

---

## 2. What the substrate already decides about the interface

The spec is not neutral input — it forecloses interface kinds. Five binding
consequences:

**S1 — Touch is a write.** DerivedUnits graduate on touch (spec §1.8). In this
world, *reading has side effects*: attention materializes structure. So the
interface cannot be a pure viewer, and — critically — it must make visible what
your attention has done, or the world silently accretes structure its inhabitant
doesn't know they created. (One correction to the spec at interface level, argued
in §8: *selection* must not count as touch.)

**S2 — Candidates-until-accepted.** Nothing inferred becomes truth without
judgment (spec §1.9). So *adjudication is a core interface verb*, and "candidate"
is a core interface element — visually distinct, provenance-carrying, judgeable.
Calibration is structural, not decorative.

**S3 — Projection ≠ Situate.** Views may invent presentation, never relationships
(spec §1.6). Consequence: every relation drawn on any screen is exactly one of
(a) authored, (b) source-native, (c) a labeled candidate. There is no fourth
category. This is an honesty property almost no existing software has, and the
interface should wear it openly.

**S4 — ViewState is keyed by placement, not object** (spec §1.11). The same object
sits on many boards at once. Objects have N placements and **no canonical
location**. This single fact forecloses any interface whose foundation is "every
thing has a place in one geography."

**S5 — The log is primary.** Every mutation is Request → Decision → Event
(README, core module). Replay is not a feature to build; it is a projection of
what is already stored.

---

## 3. Five kinds this interface could be — and why each pure form loses

These differ in *which structure they make primary*. That's the honest way to get
width: not five skins, five ontologies of "where you are."

### A. The Atlas (continuous spatial ZUI — your disclosed leaning)
One world, one space, everything has a location; movement is pan/zoom; the camera
dolly from knowledge-landscape to code. Place-ness from geography.

*Nails:* continuity; the dream's literal image; the WebGPU substrate renders it.
*Loses because:*
1. **Knowledge has no given geometry.** Google Earth's zoom works because Earth's
   geometry is given (unique location per thing), stable (continents don't
   reshuffle), and shared (your map is my map). Knowledge violates all three:
   every layout is a choice; algorithmic layouts reshuffle on re-ingest — *a place
   whose geography reshuffles under you is the opposite of a place*; and S4 says
   objects have N placements, so unique location is false in your own spec.
2. **Zoom-out changes kind, not just scale.** Optically zooming terrain keeps it
   terrain. Zooming knowledge turns messages into sessions into arcs — a
   re-projection wearing zoom's clothes. Disguising a kind-shift as a camera move
   makes the map lie.
3. *(inference)* The HCI record agrees: ZUIs (Pad++, Raskin's zoomworld) hit
   "desert fog" (Jul & Furnas) and never won for information work. Where zooming
   *did* win: maps (geometry **given**) and design canvases like Figma (geometry
   **authored**). It loses where geometry is *inferred*. That pattern is the key
   that unlocks §4.3 and §4.4.

### B. The Observatory (instruments pointed at a corpus)
You don't travel; you summon views. Query lenses, projections, filters — astronomy,
Bloomberg, a debugger.

*Nails:* honest about projection (S3); matches the substrate (everything is a
PState query); no fake geometry; strong for J1/J2.
*Loses because:* it is a tool, not a place. No persistence, no terrain, nothing
accumulates, nowhere to return to (J7 fails by construction). The vision's central
rejection is of exactly this.

### C. The Trail-world (time-primary, replayable log)
The causal log is the primary structure; the interface is a walker/player over
history. Reading = replaying. Memex trails; papers-as-logs taken literally.

*Nails:* J3 natively; S5 says the data is already there; test case (a) is
literally this question.
*Loses because:* time is one axis. The DG corpus is state-shaped; "what claims
exist about X, who disputes it" should not require replay. Pure trails are paths,
not places — you can walk them but not live in them (J5, J7 weak).

### D. The Workshop (material-and-arrangement primary)
Knowledge objects as material on persistent, *authored* surfaces — a desk, a wall,
a corkboard. Local space, not global. Place-ness from your arrangements persisting.

*Nails:* J5 and J7 natively; geometry is authored, so spatial memory is real (the
Figma lesson from A); transclusion is natural — ViewState-keyed-by-placement *is*
the workshop model, already in your spec.
*Loses because:* a desk holds the fifty things you're working with and is useless
for the fifty thousand you imported and never touched (J1/J2 fail). The imported-
silo problem is precisely material no one will hand-arrange.

### E. The Court (adjudication-primary)
The interface as the surface where candidates come before the inhabitant for
judgment: a docket of proposed relations, a ledger of accepted facts. Code review
generalized to all understanding.

*Nails:* J4 natively; S2 is its constitution; the theory-of-change's narrowest
bet (review) lives here.
*Loses because:* a queue is not a world. As the whole interface it makes Softland
feel like email — reactive, extractive, the least inhabitable genre of software.

### The meta-observation

Each candidate nails the jobs it makes primary and structurally fails the others.
That is the tell that "pick the primary structure" is the wrong move — and your
substrate already says so: *one ontology, many projections; no interpreter is
canonical* (spec §1.5). The storage answer and the interface answer have the same
shape. **The unity cannot come from a master view. It must come from the
continuity of the inhabitant.**

---

## 4. The thesis: a settlement, not a globe

### 4.1 Where place-ness actually comes from

Compare pairs. Wikipedia has no geometry at all and is unmistakably a place —
people "go to" pages, return, find their watchlists and edit histories intact.
A force-directed graph view has geometry and is *not* a place — its geography
reshuffles. Minecraft (geometry, place) vs a BI dashboard (layout, not a place).
The discriminating variables are not spatial:

> **Place-ness = stable addresses + persistence of your marks + dense paths
> onward + memory of what happened there.**
>
> Geometry is neither necessary nor sufficient.

*(inference)* Kevin Lynch's "imageability" elements for legible cities — paths,
landmarks, districts, nodes — require route knowledge, not survey knowledge.
People mostly navigate cities without ever seeing the globe view. The elements
map cleanly: landmarks ↔ stable object handles, paths ↔ trails, districts ↔ rooms.

### 4.2 The kind, stated

> **Softland's interface is a settlement: a growing fabric of persistent rooms
> over one address space, with the trail as its memory and judgment as its
> citizenship act.**

Unpacked:

- **One address space.** Every ObjectContainer has one identity and appears, with
  the *same recognizable handle*, in every room that places it. Identity, not
  location, is what survives movement. (This is what makes many rooms one world
  instead of many apps.)
- **Rooms** are persistent projections-with-memory: a *frame* (the query/
  projection that determines what's in the room) + an *arrangement* (authored
  placements; ViewState) + a *memory* (the room's own history: what was judged,
  touched, said there). Rooms are themselves objects — addressable, linkable,
  transcludable, hand-offable. A "paper" is a curated room over a trail.
- **The trail is the settlement's memory** — both yours (where you went, what you
  touched) and the world's (the event log). It is a first-class element, always
  one gesture away, and it is what makes every past state re-enterable.
- **Judgment is the citizenship act.** Candidates flow *into rooms* as visually
  distinct proposals and are judged in context, in the course of reading — never
  routed to an inbox. Accepting is how structure becomes real; "settled" is what
  candidates become.
- **No master view.** The five candidates above survive as *organs*: the census
  (B's instrument) is a room-kind; the braid (C's trail) is a room-kind; boards
  (D's workshop) are a room-kind; judgment (E's court) is a verb available in all
  of them; and geography (A's atlas) — see next.

The name "settlement" is doing real work, twice: settlements *grow by
habitation* — nobody computes a city's layout — and candidates *settle* into
facts. Both senses are the thesis.

### 4.3 Geography is grown, not given

This is the move that rescues the Atlas dream lawfully instead of killing it.

The lesson from §3-A: zoomable space works where geometry is **given** or
**authored**, fails where it is **inferred**. Knowledge geometry can't be given.
But it can become authored — *gradually, by use*:

> **A layout is a candidate, like any other inferred structure.** The system may
> propose arrangement (cluster these sources, lay out this domain). The proposal
> is rendered as what it is — a candidate. The inhabitant ratifies it the same
> way derived units graduate: by touch — arranging, naming a region, returning to
> it. Ratified layout is authored layout; authored layout is stable; stable
> layout is the only kind spatial memory can bind to.

This is the DerivedUnit lifecycle applied to *space itself*, and the substrate
already has the machinery (distiller → candidate → graduate-on-touch). Paths
become roads by walking. Over months you get a knowledge landscape that is
stable *and* meaningful — because it is the sediment of habitation, not a force
simulation. The Google-Earth feeling arrives late, as an achievement, not early,
as a rendering trick.

### 4.4 Time is the only given geometry — so the first continuous zoom is temporal

Of every dimension available to Softland, exactly one has Google-Earth-grade
geometry *today*: **time**. It is one-dimensional, totally ordered, given (the
log is timestamped), stable (append-only — the past never reshuffles), and shared
between minds. If you want a continuous zoomable surface that can never lie, the
time axis is the one place it is structurally guaranteed.

And the first test case — "how did this project get here" — is a time-zoom
question. So the camera dolly you've been reaching for ships first as: zoom from
the project's whole arc, through months, into one session, into one exchange,
into one diff — with each level honestly labeled as the kind-shift it is
(arcs → sessions → exchanges → events). Causal structure (the true DAG) layers on
as edges across the time axis; linear time is its first lawful compression.

---

## 5. The grammars

### Elements (the nouns the inhabitant sees)

| Element | What it is | Substrate backing |
|---|---|---|
| **Object handle** | The on-screen presence of a container: same identity affordance everywhere — kind glyph, name, source badge, calibration/judgment state. One object, many appearances, recognizably one. | ObjectContainer, Revision |
| **Room** | Frame (query/projection) + arrangement (authored placements) + memory (its own history). Itself an object: linkable, hand-offable. | Projection contract, ViewState, ContextMembership |
| **Candidate** | Any inferred unit/relation: distinct presentational class, provenance (which distiller/situator, when, from what), judgment affordance, in place. | DerivedUnit, Situate, LayerOverlay |
| **Trail** | The recorded path — yours and the world's — rendered, time-zoomable, re-enterable. | Event log (S5), KernelEvent causality |
| **Source thread** | Every projected unit can open its anchor back to the raw bytes of its SourceArtifact span. | SourceAnchor, SourceArtifact |

### Movement (how you go)

| Verb | Meaning |
|---|---|
| **Follow** | Traverse an edge/anchor/reference from a handle to its other contexts. The link-step. |
| **Summon** | Bring material to your room by query or name — material moves to you. |
| **Reproject** | Same focus, different frame: this set as outline / timeline / claim-graph. Always labeled with what the new frame preserves and drops. |
| **Zoom** | Lawful compression *within* a frame (incl. time-zoom). Continuous within a frame; between frames it is Reproject and shown as such. |
| **Return** | Re-enter any room or trail-point. Rooms persist; the trail makes the past addressable. |

Continuity across all five comes from three invariants, not from a shared camera:
the focus object travels with you (what you followed stays in hand), the trail
records the step, and handles are identical everywhere.

### Action (how you change the world)

| Verb | Meaning | Note |
|---|---|---|
| **Touch** | Edit / quote / reply / relate — graduates derived units, visibly. | Selection and reading must NOT graduate (§8). |
| **Judge** | Accept / reject / defer a candidate, in context. | Never an inbox. |
| **Relate** | Author a typed RelationEdge (supports / contradicts / refines / derives-from). | |
| **Arrange** | Place, move, group within a room. A meaningful event (attention as content), and the ratification gesture for proposed layout (§4.3). | ViewState write |
| **Mark** | Annotate; set calibration (fact / hypothesis / guess); name. | |
| **Package** | Promote a room/trail-region to public-material for handoff. | Visibility promotion (spec §1.7) |

All actions are ActionRequests; projection stays read-only; truth stays in Rama.
The grammar is the substrate's loop worn on the outside.

---

## 6. Audit of the disclosed thinking

### Category theory

**Need beneath it:** identity surviving transformation; many projections of one
thing staying recognizably one thing; tools that compose across domains.

**Verdict: right need, wrong altitude.** As the user-facing skeleton, CT is the
wrong material — inhabitants should never need to see a functor. As an
*engineering discipline* it is exactly right, and it cashes out concretely:

> **Every view-kind ships a declared preservation contract** — what structure it
> preserves (identity? composition? order? time?) and what it forgets.
> Re-projection between view-kinds is permitted exactly along declared preserved
> structure. Identity handles are uniform everywhere. Tests enforce the contracts.

That *is* functor discipline — "a projection is a structure-preserving map, and
you must say which structure" — without the notation. The honest version of the
claim: most of the coherence you want comes from address stability plus these
contracts; the formalism itself starts earning rent later, when authoring
*situators and tools that transport across domains* becomes daily work — that's
the moment to reach for Spivak-style ologs as the schema language. Until then,
naming the preserved structure in plain words keeps everyone honest and survives
Codex review; claiming "this is precisely a Kan extension" does not.

### Zoomable UI / continuous semantic zoom

**Need beneath it** (decoded from "I keep coming back to one world at every
scale"): *continuity of self.* Never losing where you are; no app-switch context
death; the assurance that code, claims, and landscape are one world. The camera
dolly is a mechanism for never experiencing a discontinuity.

**Verdict: right need, mechanism half-wrong.** Optical zoom over inferred global
geometry fails (§3-A) — it manufactures the very instability that destroys
place-feel, and it disguises kind-shifts. But zoom *survives in three lawful
forms*, and the need is served by four mechanisms:

1. **Time-zoom** (geometry given) — the first continuous zoom surface, §4.4.
2. **Zoom within authored rooms** (geometry authored) — boards zoom like Figma
   canvases because the inhabitant put things where they are.
3. **Compression control on any projection** — every frame has levels; crossing
   a level is a labeled kind-shift, so semantic zoom is real but honest.
4. **Grown geography** (§4.3) — the global landscape arrives by graduation, and
   once grown, it too zooms lawfully, because it is authored.

Plus the continuity invariants (focus-carrying, trail, uniform handles), which
serve "never lose where I am" better than a camera does — orientation in real
places is mostly route memory and landmarks, not aerial views *(inference)*.

---

## 7. The path from today: month one

**Today's reality** (from code, this session): two live ingestors (markdown,
transcript JSONL) feeding a real substrate — containers, revisions, anchors,
composition edges, outline and conversation projections, ~40 PStates, full
request/decision/event audit — and **zero client views rendering any of it**.
No RelationEdge in code (spec-only). No graduation writes wired. No git/commit
ingestion. Test fixtures are toy-sized; the real corpus (your actual session
transcripts, this repo's docs and history) is sitting on disk uningested.

**The slice: "How Softland Got Here" — the braided trail room.** One room-kind,
on the history-shaped corpus, proving the thesis where it is sharpest.

1. **Ingest the real corpus.** Run the existing transcript adapter over actual
   session JSONL and the markdown adapter over `docs/` revisions. Add a **thin
   commit-lane adapter** — commit metadata only (hash, message, author, timestamp,
   files touched) as containers, following `markdown_adapter.clj`'s pattern. This
   is a day-scale adapter, *not* the code ingestor; it unlocks the third lane.
   *Proves:* the adapters survive real data volume.
2. **The braid.** First Rama→Electric→client view of object-container data:
   three lanes on one shared time axis — conversations, doc revisions, commits —
   at session-level compression. *Proves:* the missing arrow (substrate → screen)
   on real data; this is currently the biggest hole in the whole system.
3. **Census strip.** A header over the braid: per-source counts, kinds, recency
   (from `read-common-material-for-source`). *Proves:* J1 cheaply — "what does
   each silo contain" — without building a census room yet.
4. **Time-zoom, three levels.** Project arc → session → exchange. Each level a
   labeled kind-shift on the same axis. *Proves:* whether honest semantic zoom
   can still *feel* continuous — the felt test of §4.4.
5. **Source thread end-to-end.** Click any message/doc/commit → SourceAnchor →
   raw span of the SourceArtifact. *Proves:* the honesty spine; exercises
   `read-unit` / anchors / revision queries that exist but have never been
   consumed.
6. **Cross-lane candidates + judgment.** High-precision linkers only: commit
   hashes and file paths mentioned in transcript text → candidate relation edges
   drawn across lanes, judged in place via ActionRequest. Requires the slice's
   **one substrate addition**: a small RelationEdge PState with candidate status,
   straight from the spec. *Proves:* J2 + J4, the candidates loop closed for the
   first time, and S3's three-kinds-of-line honesty on screen.

**Success criterion** is your own felt test: scrub the braid and ask whether you
understand how this project got here better than you did from memory. If yes, the
core primitive (structured exploration logs over static prose) is validated on
the narrowest case — which is the theory of change.

**Deliberately deferred:** rooms-as-boards and arrangement; grown geography;
the DG corpus (state-shaped; needs the Roam import; its first room is a census/
claim-graph, and it becomes slice two); graduation writes (the braid is
read-mostly); any global landscape.

---

## 8. Falsification pass — strongest attacks on my own thesis

**A1. "A settlement of rooms is just the desktop rebranded — windows, files,
links."** The differences are load-bearing: one address space (handles are the
same object everywhere, with provenance — not copies); rooms re-derive from
shared truth when truth changes (not files); a global replayable trail (desktops
have no memory); candidates/judgment native. *But the attack partially lands:*
if rooms proliferate without connective tissue, you get window-manager sprawl.
Mitigation: the trail is the connective tissue, and follow-edges link rooms. I
concede the place-*feel* depends on transition execution (focus-carrying, trail
visibility) that cannot be proven on paper. This is the thesis's real risk.

**A2. "You killed the global view, but J1 demands an overview."** The overview is
a census room (counts × kinds × recency × connectivity), not a geography — plus
the time-zoomed trail for "what happened." Where a spatial overview is genuinely
wanted, §4.3 provides it lawfully: proposed layout as candidate, ratified by use.
This attack improved the slice — the census strip (§7.3) exists because of it.

**A3. "The braid will be noise — causality in vibe-coded projects lives in messy
conversations."** If the braid is noise, compression failed, not the axis:
session-level units, graduated decisions as landmarks, kind-shifting levels. The
real risk is cross-lane linker precision; mitigated by shipping only
high-precision linkers (hashes, paths) first. Conceded as the slice's main
quality risk.

**A4. "You designed a cathedral — five elements, five movements, six actions."**
The grammar is the destination; the month-one slice is one room-kind, one zoom
axis, one candidate type, one judgment verb. The grammar's job is to ensure the
slice is a *down payment* on a kind, not a one-off view.

**A5. "Your thesis is suspiciously congruent with the spec you were told to read
— echo, not derivation."** The spec constrains hard; convergence with it is
coherence, not contamination. The independent content is checkable: the
place-ness-without-geometry argument, time-as-the-only-given-geometry, geography-
as-graduating-candidate, judgment-in-context-never-inbox, preservation-contract
CT, and a direct contradiction of an active project track (knowledge-earth ZUI)
made without reading it. Also one correction *to* the spec at interface level:
§1.8 lists "select" among graduating touches. **Selection must not graduate** —
it is the cheapest gesture in the interface, used constantly for pure reading;
graduating on select turns every sweep of attention into silent structure
accretion, violating S1's own visibility requirement and silting the world with
accidental durables. Graduate on edit / quote / reply / relate — deliberate acts
with content. (Mere *reading* writing the world is exactly the kind of lie the
map must not tell.)

---

## 9. Three honest sections

### (a) What surprised me — where I diverge from what you might expect

1. **I reject the global ZUI as the foundation** — and recommend *against*
   leading with a knowledge-earth surface, the direction one of your active
   tracks is named for. Zoom survives, but time-first and authored-space-first;
   global geography is grown by graduation, late, or it is fake.
2. **CT gets demoted to invisible discipline** — preservation contracts and
   uniform identity, no user-facing categorical skeleton, ologs deferred until
   tool-authoring across domains is the daily bottleneck.
3. **The first view is a history braid**, not a file-explorer upgrade, not a
   graph view, not a canvas — because your sharpest test corpus is history-shaped
   and the substrate's deepest asset is the log.
4. **The best new idea came from your own spec**: layout as a candidate that
   graduates on touch. The settlement grows the way derived units do. I'd guess
   you expected zoom to be defended or killed; instead it gets a lifecycle.
5. **I push back on one settled-spec detail** (select-graduates) — from the
   interface side, where its cost shows up.

### (b) Vision assumptions that deserve challenge

1. **"Explorable like Google Earth."** The metaphor smuggles in given, unique,
   stable geometry — three properties knowledge lacks and your own ViewState
   model contradicts. Keep the aspiration (a landscape you can survey and dive
   into); re-ground it as *grown* geography.
2. **"The code editor IS Softland at zoom 100."** Keep the real content — one
   address space down to source spans (SourceAnchors already do this); drop the
   optical-dolly claim. The descent from landscape to code is a chain of honest
   kind-shifts, and the interface should show them, or the map lies.
3. **Preserved disagreement has a cost surface.** Candidates accumulate without
   bound; preservation without pacing silts the world. The vision needs a
   triage/decay story — my answer is judgment embedded in reading flow (never an
   inbox) plus candidate aging made visible, but *some* answer is required.
4. **"Papers should be logs."** Sharpened: the log is the provenance spine; the
   paper is a curated *room over* the log — replay available at every claim, not
   the primary reading surface. Pure replay is re-living, not reading; handoff
   (J6) needs the curated form.

### (c) Decisions only you can make

1. **The name of the room primitive** (room / place / board / desk / view).
   Vocabulary will shape every doc, function name, and instinct after this.
   *Recommendation:* an inhabitation word — "room" or "place" — over instrument
   words like "view"/"board." *Cost of deciding late:* low today, compounding
   steadily as code and docs accrete the wrong noun.
2. **The graduation consent boundary** — which touches write the world.
   *Recommendation:* edit / quote / reply / relate graduate; select / hover /
   read never do. *Cost of deciding late:* your first real corpora (the ones the
   felt test runs on) get polluted with accidental durables that must be
   demoted by hand, and the world's first lesson to its inhabitant is that
   looking at things changes them.
3. **First-corpus priority** — Softland-itself vs the DG project.
   *Recommendation:* Softland-itself (ingestors are live today; the felt test is
   sharpest on your own history; DG needs the Roam import regardless, and its
   state-shaped room is a cleaner slice two). *Cost of deciding late:* a month of
   view-building against the wrong corpus shape — history rooms and state rooms
   share elements but not frames.

(The spec's own open decision — cherry-pick vs all-or-nothing promotion — stays
rightly deferred; its interface implication is that judgment affordances are
per-change, which the grammar already assumes.)
