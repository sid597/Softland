# The Trail View's Face — Fork Map · UI design pass, sitting 3 · 2026-07-06

**Standing:** design-track artifact (Fable, principal-designer, explicit manual
invoke). This pass builds the missing middle layer: canon laws cashed out into a
concrete visual system. Forks named BEFORE any rendering, per Sid's sequencing.
Machinery-done ≠ form-done; this is the form side. No code, no src/ edits.

**Grounding read:** decision-log + taste (boot), capstone synthesis,
interface-derivation, room-card-lane sitting-2 rulings, sitting-3 queue,
FIRST_LIGHT_R2 + screenshot, D-002/D-010, vision/LOG recent, and a spot-check
of `trail_face/lanes.cljc` (as-built axis verified, not inferred).

---

## 0 · The honest baseline, verified

The shipped face (first light, 2026-07-06): **y = time** (a vertical scroll
feed, ordinal spacing by row), lanes as **mod-wrapped x-offset spines**
(`lanes.cljc assign-lanes`), Manhattan elbow connectors, every card a filled
multi-line box, terminal-dark, one type size. Two clocks and kraft edge lines
work — the *honesty* landed; the *form* is what a feed renderer emits by
default.

One finding worth stating plainly: sitting-2's R7 carries the parenthetical
"LAW: x is time everywhere," and the built face runs time down **y**. The
standing design language and the shipped face already disagree about the most
basic spatial fact of the view. Nobody decided this. That is the cleanest
possible evidence that orientation is undesigned — and it is Fork 1.

## 0.5 · The walls (not forks — ruled, and this pass designs inside them)

- **The face test [LAW, Sid-ratified 2026-07-04]:** trail always on screen;
  every node reads at title/decision/2-liner; any node opens in place while
  others hold; detail costs local space, **never the shared axis**; layout
  deterministic from the log, never force-directed.
- **Time is primary** (D-002; capstone §4.4: time is the one given geometry).
  WHICH screen axis it owns is open; THAT it organizes the face is not.
- **The band exists** (R7): unthreaded material renders as one self-declaring
  fog region, never auto-filed, never hidden. Every system must render it.
- **Two materials** (R6): terrain ink vs kraft assertion marks; a relation is
  never a box-card. Laws 8/12: question marks survive every altitude;
  truth-states never color alone.
- **Closed = typography, open = surface** (R4) — standing designer STANCE.
  Systems may vary *within* it (see Fork 2); a system that breaks it must say
  so out loud.

Sitting-3 queue items folded in: every proposed system must answer **item 3**
(where the band-2 semantic line comes from and lives — the render half of the
commit-gesture question) and **item 4** (doors: the gesture by which a card
opens a room at its address).

---

## The five forks

### Fork 1 — The axis of time: who pays for text width?

The physics underneath "horizontal or vertical": text is wide and short;
screens are wide; a node's minimum legible form is a ~20–60-char title. Time
and titles compete for the same dimension somewhere. The fork is where.

**Branch H — x = time (the wall's orientation).** Threads are horizontal
rows; the trail reads left→right; branching and dead-ends LOOK like a DAG —
the crossroads question (D-002) renders as literal geometry. Opens spend y,
which is lane-local, so the outliner disease is *structurally impossible*,
not just forbidden. Costs: clustered events (15 commits in a day) stack
titles at one x — needs fold chips, label laddering, or local ordinal
compression; reading order is not scroll-natural; text density per screen is
low. **Tier 2** (layout = a named projection).

**Branch V — y = time (the feed's orientation, designed instead of
defaulted).** Scrolling is reading; every event gets full line width — title
+ reading line always legible; the corpus reads as a chronicle. Opens must
spend x (the vast empty right half of the first-light screenshot) — lawful
under the face test if and only if opens grow *sideways*, never by inserting
rows. Costs: parallelism reads weakly — simultaneous threads are interleaved
rows, branches don't look like branches; this is the genre Sid's face-test
kill was aimed at, and it must answer that kill explicitly. **Tier 1–2.**

**Sub-fork 1a — metric vs ordinal spacing.** Metric position (∝ timestamp)
is honest about gaps and rhythm but kills text at burst density; ordinal
(one slot per event) reads beautifully but compresses gaps — and the map
must not lie, so ordinal requires *declared* gaps (gap glyphs / "4 months
pass" rules). Likely zoom-regime split: metric at survey altitude, ordinal
at reading altitude, the crossing labeled (lawful kind-shift, Law 7). The
two-clock discipline rides here: **claimed** time owns position (semantic
history), **arrived** renders as chips/marks — already the standing lean.

*Pull:* the wall panel (D-002's origin) and "sense of space" pull H; every
readable chronicle ever made pulls V. This is the fork most worth feeling
with eyes, not arguing. *Forecloses:* H forecloses casual scroll-reading; V
forecloses geometry-as-orientation. *Reversal:* cheap by construction —
tier-2 makes orientation a registry entry, so the losing branch stays
drivable.

### Fork 2 — Node grammar: mark-first or line-first?

What IS a closed node, visually? R4 killed the box; two grammars survive:

**Mark-first (cartographic).** The primitive is a positioned glyph — kind
mark + status dot; the name is a *label attached to the mark*. Zoom out and
labels recede but marks persist: the world degrades like a map, and band-0
density is graceful by construction. The glyph vocabulary must carry
kind/staleness/dead-end/calibration redundantly (Law 12). Feels like terrain
with settlements. **Tier 1** (glyphs are text; dots are SDF).

**Line-first (typographic).** The primitive is a text line; position is
where the line starts. The world reads as a ledger/manuscript; density is
handled by folding lines into count-chips, not by shrinking labels. Zoom-out
becomes a table of contents, not a map. Feels like a chronicle. **Tier 1.**

*Pull:* taste #1 (text before texture) pulls line-first; Sid's "at least
ZUI-based… sense of space" and the settlement thesis pull mark-first. The
deep question: should zooming the trail feel like *rising over land* or like
*closing a book to its contents page*? *Forecloses:* mark-first forecloses
nothing textual (labels are text); line-first forecloses map-feel at survey
altitude. *Reversal:* band-0/1 render style is data once face-2 lands
(tier 2).

### Fork 3 — Edge grammar: is a relation a line in space or a mark in time?

A typed edge (`based-on`, `produced`…) has two honest renders, and they
correspond to the two things an edge IS:

**Geometry — a drawn connector** between endpoint positions, kraft-colored,
labeled at nearer bands (R6's connector). The DAG appears as *shape*;
dead-ends visibly terminate; structure is seen, not read. Costs: crossings
at density; requires both endpoints placed; off-screen endpoints need R6's
standalone-line rule. **Tier 1** straight/elbow; curved connectors
**tier 3 — dream, labeled**.

**Event — an assertion row**: the edge renders at its *own time position*
as a kraft mark ("⊢ 2232fbb based-on d6432f7 · import:git-spine · attested
07-06"). This is Law 1 taken literally: an edge is an event with an asserter
and two clocks, and it happened *at a time*. Provenance renders natively; no
crossings ever. Costs: the DAG never appears as shape; structure must be
read. **Tier 1.**

*Pull:* the wall has literal kraft connectors — geometry has provenance in
Sid's own artifact; the log-is-truth law argues for event-primary. Likely
band-split synthesis (events at reading altitude, geometry at survey), but
the fork is *which is primary in the all-closed state* — what the world
looks like before you touch it. *Forecloses:* event-primary forecloses
glance-legible structure; geometry-primary forecloses glance-legible
provenance. *Reversal:* per-kind render rule in the face spec (tier 2).

### Fork 4 — Open mechanics: inflate, lens, or margin?

The face test rules the constraint (detail costs local space, in place,
others hold); three mechanics live inside or at its edge:

**Inflate.** The box appears and grows at the node's position; neighbors
hold; the non-time axis pays. The law's most literal reading; R4's "open
cards are surfaces" assumed it. Multiple opens = multiple surfaces standing
in the terrain. **Tier 1–2.**

**Lens — open IS zoom, locally.** A node's open state is a *pinned local
band override*: opening = zooming that one node to band 3 while the world
holds its band. Open and zoom collapse into one mechanic — arguably the
verbatim of Sid's face-test text ("zoom in and out granularly while having
the sense of space"). All-open and all-closed become altitudes of one
surface, not modes. **Tier 2** (band policy per node in the spec);
smooth animated transition **tier 3 — dream**.

**Margin (apparatus).** Opens project into a dedicated strip anchored by
leader lines — the critical-edition grammar; the trail stays pristine.
**Named honestly: this is in tension with the ratified LAW's "in place,"**
and the margin is a shared resource N opens compete for (queue smell, taste
#2's cousin). Included because it is genuinely different, carried only as a
variant a system may *show* — adopting it would need Sid to knowingly amend
his own law, which is his call alone. **Tier 2.**

*Pull:* lens, strongly — it is the only branch where the mechanic and the
face-test law are the same sentence. *Forecloses:* lens couples open-grammar
to the zoom model (a cost if zoom regimes change). *Reversal:* open-state =
band policy data (tier 2), so mechanics can be swapped per face.

### Fork 5 — Material and atmosphere: terminal, wall, or atlas?

The container language — ground color, corner language, type scale, density,
what kraft looks like against what:

**Terminal land.** Dark ground, mono, hairline ink, square-shouldered
boxes, uniform scale. Native to the editor-at-zoom-100 bedrock; zero
warmth; currently the default by inertia, not choice.

**The wall.** The origin panel's own material: warm paper ground, ink
terrain, kraft-tape relation marks, a real type hierarchy (titles carry
size, not weight alone), soft corner radii. "A land, not an app" taken to
pixels; R6's two materials become literal materials. Risk: kitsch if it
performs paper instead of behaving like it.

**The atlas.** Cartographic instrument: muted terrain tones, fog for the
band/frontier (Law 8 rendered as weather), contour-fine hairlines, small
labels in big space, calibration as map conventions (dashed = proposed,
solid = ratified). The map-that-must-not-lie wearing a mapmaker's craft.

All three are **tier 1** (SDF corners/gradients/shadows + MSDF/slug text are
free today). Law 12 binds all: truth-states redundant, never color alone.
Taste #1 governs: material may never outrank text legibility.

---

## Non-forks (look open, aren't)

- Whether the band renders — ruled (R7). Only its *material* varies (fog vs
  margin-note vs unshaded strip).
- Whether provenance is visible at reading altitude — ruled (R5 band table;
  honesty-as-verbosity is what we're repairing, not the honesty).
- Color scheme as differentiation — three palettes on one skeleton is fake
  diversity; Fork 5 only counts riding a structural stance.
- x-of-time vs "no time axis at all" — not a fork; D-002's form is temporal.

## Failure modes of this pass itself (pre-registered)

1. **Fake diversity** — three skins on one skeleton. Guard: each system must
   take a different branch on at least two of Forks 1–4.
2. **Strawman diversity** — one real candidate plus two decoys. Guard: I must
   be able to argue each system as the winner for five minutes.
3. **Dazzle over law** — a beautiful board that violates the face test or
   Law 8. Guard: falsification pass per system before delivery.
4. **Lorem-ipsum leak** — mockups over invented material. Guard: every render
   uses the first-light corpus verbatim (real shas, real conversation ids,
   real two-clock stamps, the real `based-on`/`produced` edges).

---

## The proposed slate — three systems as fork coordinates

**AMENDED 2026-07-06 (Sid override, logged in decision-log):** the wall panel
is provenance of the QUESTIONS the view answers, never an aesthetic target —
"the goal of code is not to make a reality emulator." Fork 5's "wall
material" branch is DEAD. S1 renamed THE WALL → THE CROSSROADS; its material
is now chosen purely for shape legibility. Briefs (v3) lead with the
trail-view problem: history fully captured, completely illegible.

| | Fork 1 | Fork 2 | Fork 3 | Fork 4 | Fork 5 |
|---|---|---|---|---|---|
| **S1 · THE CROSSROADS** | x = time, metric-at-survey/ordinal-at-read | mark-first | geometry-primary | lens | calm ground + reserved amber; material serves shape |
| **S2 · THE CHRONICLE** | y = time, ordinal + declared gaps | line-first | event-primary (geometry at hover/near) | inflate (sideways, into the right half) | refined ink (terminal, designed) |
| **S3 · THE SURVEY** | x = time, metric; zoom is the only mechanic | mark-first | geometry-primary | lens taken to its limit: continuous altitude field | atlas + fog frontier |

- **S1 — The Crossroads.** The shape-first bet: horizontal trail, threads
  as rows, connectors as geometry — fork/pivot/dead-end answered before a
  word is read. Discrete zoom regimes, opens as pinned local lenses.
  Mostly tier 2; curved connectors tier-3 dream, labeled.
- **S2 — The Chronicle.** The vertical genre kept but *designed*: the feed
  becomes a ledger — line-first typography with a real scale hierarchy,
  edges as kraft assertion rows in the flow, opens inflate rightward into
  the empty half, gaps declared. The honest evolution of what exists; must
  explicitly answer the face-test kill (it does, if opens never insert
  rows). Mostly tier 1 — cheapest distance from today.
- **S3 — The Survey.** The lawful ZUI: one continuous surface where camera
  altitude sets the base band, focus pins local altitude, fog renders the
  frontier, the band is weather at the map's edge. All-closed / few-open /
  all-open are *altitudes*, not states. Tier 2 core; focus+context
  distortion and animated altitude transitions tier 3 — dreamed, labeled.

Distinctness check against failure mode 1: S1/S2 differ on Forks 1,2,3,4,5;
S2/S3 differ on 1,2,3,4,5; S1/S3 share mark-first + geometry but differ on
time spacing, zoom/open model (discrete regimes + pinned lens vs one
continuous altitude field), and material. Passes.

Each system's render must additionally answer, on its own terms:
- **The band-2 semantic line** (sitting-3 item 3): what the second line says
  before authored decision-text exists (contract data: when·who), and where
  the authored 2-liner lands when the commit-gesture arrives.
- **Doors** (sitting-3 item 4): what the gesture and the affordance look
  like when a card opens a room at its address — drawn, not just named.
- **The three states**: all closed / few open / all open, over the
  first-light corpus verbatim.

## What this pass forecloses / reversal

Choosing a system forecloses nothing structural: tier-2 framing means
orientation, band policy, and open mechanics land as data (named projections
+ face specs), so the losing systems remain buildable as lenses later —
which is also Sid's own world-and-lenses stance (LOG 2026-07-05: "we have a
world and the lenses to view that world … different workzones might have
different default world"). What IS spent: the default. The default face is
what daily use wears, and D-001 means daily use is what earns runtime growth
— so the default quietly steers which demands become lawful. That is the
real stake of the pick.

**Next:** Sid reacts to the fork map (kill branches on sight, or bless the
slate); then the three systems render as HTML side-by-sides over the
first-light corpus, three states each, tier-tagged, rationale traced to
laws.
