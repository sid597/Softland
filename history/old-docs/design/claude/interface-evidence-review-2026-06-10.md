# Evidence Review — Testing the Settlement Thesis Against the Literature

Companion to `interface-derivation-2026-06-10.md`. 2026-06-10.

**Method note.** A deep-research harness ran search + fetch over ~20 primary
sources; the adversarial verification pass was deliberately skipped (user call,
token economy) and synthesis was done by hand from the extracted claims. So:
claims below are sourced but not independently re-verified. Confidence flags are
mine. Things we did NOT fetch are listed at the end — notably the modern
embedding-map papers (Nomic Atlas, WizMap) and Archambault & Purchase's
mental-map experiments.

---

## Scoreboard

| Claim under test | Verdict |
|---|---|
| Q1 "Zoom works where geometry is given or authored, fails where inferred" | **Nuanced — survives with an upgrade: legible ≠ inhabitable** |
| Q2 "A braided, time-zoomable construction history can transfer real understanding" | **Supported for fact-finding; corrected on mechanism; cross-lane is the hard part** |
| Q3a "Judge candidates in reading flow, not an inbox" | **Supported** |
| Q3b "Candidate piles need aging/decay" | **REFUTED — wrong lever; it's volume + redundancy, not time** |
| Q4 "Place = addresses + persistence + paths + memory, not geometry" | **Supported — but missing one load-bearing ingredient: co-presence** |

---

## Q1 — Spatial and zoomable UIs

**The given/authored/inferred trichotomy survives, but the evidence splits
"inferred" into two capabilities I had conflated:**

- **Inferred layouts are LEGIBLE.** Users correctly decode proximity as
  similarity in point-display spatializations (Fabrikant et al., the "first law
  of cognitive geography"), and they can complete search tasks inside inferred
  2D document maps (Westerman & Cribbin, IJHCS 2000). So an embedding map is a
  readable *instrument*.
- **Inferred layouts are NOT INHABITABLE.** Domain experts using force-directed
  layouts reported that session-to-session layout variation broke recall, and
  >50% of their navigation was *revisitation* (Ghani & Elmqvist) — return is the
  dominant act, and it's exactly what instability destroys. The spatial-memory
  survey (Scarr, Cockburn & Gutwin 2013) is blunt: interfaces that move or
  rearrange items break users' spatial memory; stable arrangement is a
  performance resource.
- **The killer detail:** Ghani & Elmqvist tested system-computed, data-derived
  regions (Voronoi from node clustering) against a meaningless-but-stable 3×3
  grid — **the arbitrary grid won significantly**; users said the data-derived
  boundaries confused them. Machine-meaningful geography lost to stable-dumb
  geography. (F(1,15)=33.35 time, p≤.001 for the substrate-grid effect.)
- **Authored geometry is durable.** Data Mountain (Robertson et al., UIST '98):
  user-placed layouts beat the IE4 Favorites hierarchy with statistical support;
  layouts were idiosyncratic, personal, evolved under user control; the authors
  *explicitly* contrast manual placement (long-term memory) with automatic
  layout (short-term use). Follow-up (Czerwinski et al., INTERACT '99): months
  later, retrieval speed held; with thumbnails blanked, location memory alone
  sufficed after brief re-adaptation.
- **Desert fog is real and not even confined to inferred layouts**: Jul & Furnas
  (UIST '98) documented it as inherent to multiscale worlds (Pad++); Hornbæk,
  Bederson & Plaisant (TOCHI 2002) observed it on *real geographic maps* (6/32
  subjects). Jul & Furnas also tried inferring structure from spatial layout via
  cluster analysis and **abandoned it** — semantics and layout were confounded —
  switching to view-based grouping. (View-based grouping ≈ rooms.)
- **Overview panes are overrated**: removing the overview did not hurt accuracy
  and made users 22% *faster* on a multi-level (semantically structured) map;
  separate-overview use cost ~20% time (split attention). But 80% *preferred*
  having the overview — a satisfaction/performance dissociation. Multi-level
  structure embedded in the view substitutes for a separate overview.
- **Caution from Data Mountain:** performance preceded preference — the first
  user group preferred the worse-performing familiar tool until interaction
  polish landed. Spatial interfaces need polish before users accept them.

**Design consequence (refined claim):** inferred maps are permissible as
*reading instruments* — one-shot census/projection views, frozen per edition and
landmarked if shown at all — and impermissible as *home terrain*. When the
system proposes spatial scaffolding for the grown geography, propose **simple,
stable, named scaffolding** (grid/landmark-like), not data-derived blobs; let
meaning come from the inhabitant's arrangement. The grown-geography lifecycle
gets direct support from an unexpected source: the Juggler MOO implemented
literally it — exits accumulated usage counts and well-trodden paths surfaced in
room descriptions (Dieberger & Frank, 1998), and the authors argue an
information city "cannot be laid out in one step but must develop over time out
of inhabitants' interactions."

---

## Q2 — Trails, replay, provenance

**Supported where it matters, with sharp design numbers and one demotion.**

- **Direct support:** 15 first-time users navigating *someone else's* notebook
  history (300+ versions) via Verdant answered a median **80%** of realistic
  history-foraging questions correctly (Kery et al., CHI 2019). Construction
  history can transfer project understanding to newcomers — for concrete
  fact-finding.
- **Process-only insights exist:** History Flow (Viégas et al., CHI 2004)
  surfaced dynamics invisible in the artifact — first-mover advantage (earliest
  text persists longest), vandalism repair medians of 1.7–2.8 minutes. Some
  understanding is *only* accessible through process history.
- **The honest demotion:** rigorous evidence that provenance tools transfer
  *deep* understanding is thin — Ragan et al. (2015) found few convincing
  evaluations; Heer et al. (2008) never experimentally validated transfer, and
  proposed semantic zooming of histories only as future work. The braid's felt
  test is genuinely an experiment, not a confirmation.
- **Raw logs are unusable; compression is the product.** Exploratory work
  generates ~300 versions/hour (Kery); usable history required chunking
  (60-second merges) and foraging cues — without scent, users degraded to
  brute-force linear replay, lost confidence, and abandoned (8/15 hit "how do I
  get back?" failures). Heer: undo was ~12.5× more common than redo → cull
  abandoned branches by default; behavior-derived chunking culled 61.7% of
  states; **user-authored landmarks exempt a state from culling**.
- **Compression has human-calibrated numbers:** people compress a ~90-minute
  analysis session into **~7 segments** (median 7.5, mode 7; 78% used 4–12),
  with boundaries at *semantic events* — key findings (23%), strategy changes
  (27%) — not raw interaction events; no consensus granularity exists, so
  compression must be user-adjustable (arXiv 2410.11011). ~66% of summary
  content is machine-capturable from logs; the reasoning layer is not — machine
  drafts the trail, human annotation supplies the why, concentrated at
  anomalies (Ragan).
- **Spacing mode determines visibility:** equally-spaced revisions reveal
  fast-repaired perturbations that date-spaced views hide (History Flow) — the
  braid needs both spacing modes, plus tight braid↔source crosshair linking,
  which History Flow found *necessary*, not nice-to-have.
- **The braid's weakest link is its main bet:** cross-artifact relation tasks
  had the *lowest* success rate (66%) in Verdant. Cross-lane links must be few,
  high-precision, and visually privileged — which converges with the plan to
  make them judged candidates.
- **Curated beats raw for handoff:** Tableau users communicated findings via
  manually curated, exported subsets of history, not raw replay (Heer). Walden's
  Paths (the memex-trail descendant) found the bottleneck was *authoring and
  maintenance* — gradual authoring, author/source visual distinction, link rot —
  not reader comprehension. "Papers are curated rooms over trails" is the right
  formulation; raw replay is the provenance spine, not the deliverable.

---

## Q3 — Judging machine-proposed structure

**In-flow judgment: supported. Aging/decay: refuted — wrong lever.**

- **In-flow is the studied, working paradigm:** Copilot-style in-editor
  suggestion judgment at deployment scale (Mozannar et al. 2023); Horvitz's
  mixed-initiative principles (CHI '99) define a *dialog band* between act and
  stay-silent — surface a candidate in context exactly when confidence is
  intermediate; rejection must be a cheap, natural gesture; a wrong guess should
  leave the user in an approximately-correct position to refine (graceful
  degradation: propose coarser scope when unsure).
- **The decay correction.** Clinical alert-fatigue data (BMC Med Inform 2017,
  large-N): each additional alert per encounter dropped acceptance ~30%; after a
  user overrode an alert once, they overrode its repeats 87.9–99.9% of the time;
  and there was **no evidence of time-based desensitization** — fatigue is
  driven by volume and redundancy, not elapsed time. So the silting risk is
  real, but the remedy is **suppression, dedup, and withholding**: cap
  candidates per view; never re-present a judged candidate unchanged; withhold
  candidates predicted to be rejected (CDHF showed this works on real Copilot
  telemetry). Decay timers solve a problem the evidence says doesn't exist.
- **Don't tune situators on acceptance rate** — acceptance is a flawed proxy
  for quality (Mozannar). An accepted candidate is not necessarily a good one.
- **Over-acceptance is the deep risk:** explanations next to AI suggestions do
  NOT reduce overreliance and can increase it; people form global accept/reject
  heuristics instead of judging per-item; cognitive forcing functions reduce
  overreliance but are disliked and benefit high-Need-for-Cognition users most
  (Buçinca et al. 2021). **Placement rule:** friction only at consequential
  boundaries — graduation to durable, acceptance into truth, promotion to
  public-material — never per-candidate. This also independently confirms the
  derivation's select-must-not-graduate correction: cheap gestures get
  heuristic, not deliberate, judgment.

---

## Q4 — Place-ness without geometry

**Supported about as strongly as a theory claim can be — with one amendment.**

- **The canonical frame:** Harrison & Dourish (CSCW '96): *space is the
  opportunity; place is the understood reality.* Place-ness cannot be designed
  or computed, only grown through appropriation — designers design FOR place.
  Dourish (2006) goes further: place comes *first* in experience; geometry is a
  derived abstraction; top-down spatial imposition fails (and geometry-first
  CVEs did historically fail as collaboration media).
- **Geometry unnecessary:** text-only environments are robustly experienced as
  places — LambdaMOO (Curtis); 69% of 207 MUD users reported presence, and
  **not one** spontaneously credited the spatial metaphor (Towell & Towell);
  USENET groups develop distinct norms/place-ness on identical technology
  (Harrison & Dourish). Geometry insufficient too: MUD geography is routinely
  undermined by teleports without destroying place-ness.
- **The amendment — co-presence is load-bearing.** What the presence studies
  actually found is that *other reacting people* drive place-feel; persistence
  of marks matters because someone left them and someone will find them. My
  formula (addresses + persistence + paths + memory) needs a fifth element:
  **others' presence, visible**. For solo-Softland today, the co-present others
  are the agents — Claude/Codex sessions are already other minds leaving
  trails. Their marks should read as inhabitants' marks, not system output.
- **Read-wear precedent, with a twist:** Juggler MOO surfaced usage counts on
  exits — grown geography working in 1998. The twist: the same read-wear data
  was understood by everyone when rendered as *text*, and confused users when
  rendered *graphically*. Presence and wear should ship as marks and words
  before glows and heatmaps — a direct check on WebGPU-spectacle instincts.
- **Appropriability beats fidelity:** Xerox's cheap, movable, user-adjustable
  video link fostered place; Bellcore's high-fidelity, un-appropriable
  VideoWindow failed. Rooms must be cheaply rearrangeable and markable from day
  one; polish is optional, appropriability is not.
- One weak spot flagged honestly: virtual-world *place attachment* literature
  (2011) is theoretical extrapolation, not replicated measurement.

---

## Design deltas (what changes in the thesis and the slice)

1. **Kill candidate decay; install suppression.** Per-view candidate cap;
   judged-rejected candidates never reappear unchanged; low-confidence
   candidates withheld entirely; situators never tuned on acceptance rate.
2. **Add co-presence to the settlement formula.** Rooms and trails show who has
   been here (human and agent), as textual marks/wear first. The braid's lanes
   are already this — name it and design for it.
3. **Split "legible" from "inhabitable" geometry.** Embedding-map census views
   are fine as frozen, landmarked, clearly-instrumental editions. Home terrain
   only from authored/ratified arrangement. System-proposed scaffolding =
   simple, stable, named — never data-derived regions.
4. **Braid numbers from evidence:** default session compression to ~5–9
   segments; segment boundaries at semantic events (ActionDecisions, doc
   commits, named findings — we have these in the substrate); user-adjustable
   granularity; both equal-spaced and time-spaced modes; cull abandoned
   branches by default with landmark-exemption; tight braid↔source crosshairs;
   an auto-drafted narrative overlay whose reasoning layer is human-correctable
   (machine drafts ~66%, human supplies the why at anomalies).
5. **Cross-lane links get the highest design care** — fewest, highest-precision,
   visually privileged, judged in place. The literature says this is where users
   fail (66% success); it is also the braid's core value claim.
6. **Friction placement:** consequential boundaries only (graduate / accept /
   publish). Per-candidate judgment stays one cheap gesture. Confirms
   select-must-not-graduate.
7. **No separate overview pane as crutch:** build multi-level structure into
   the braid itself (Hornbæk); the census strip is metadata, not navigation.
8. **Ship rooms rough.** Appropriability (rearrange, mark, name) before polish —
   with Data Mountain's caveat that *acceptance* (preference) may lag until
   polish arrives; don't read early "feels rough" as thesis failure.

---

## Vision-language fixes (ready to paste; say the word and I'll edit the glossary)

- **Replace** "explorable like Google Earth" →
  *"a settled landscape: geography grown by inhabitation — zoomable where
  geometry is given (time) or authored (arrangement); machine-drawn maps are
  instruments you consult, not terrain you live in."*
- **Keep & sharpen** "semantic zoom IS pacing" →
  *"zoom is dosage control: continuous within a frame; crossing a kind-shift
  (messages → sessions → arcs) is a labeled re-projection, never a disguised
  camera move."*
- **Operationalize "local world"** (glossary currently says "use carefully"):
  *"room — a persistent projection with memory: frame (what query fills it) +
  arrangement (authored placement; ViewState) + memory (what was judged,
  touched, said here). Rooms are objects: addressable, transcludable,
  hand-offable. A split is a preserved-tension co-presence INSIDE a room, not a
  layout."*
- **Add: read-wear** —
  *"traces of use rendered in place (visits, touches, judgments, agent passes).
  Render as marks and words before glows; wear is how geography grows."*
- **Add: candidate** —
  *"machine-inferred structure awaiting judgment. Judged in reading flow, one
  gesture, in place. A rejected candidate never reappears unchanged. Volume is
  capped; suppression — not decay — prevents silting."*
- **Replace** "papers should be logs" →
  *"a paper is a curated room over a replayable trail: every claim one gesture
  from its provenance; the trail available, the curation doing the talking."*
- **Re-ground** "the editor IS Softland at zoom 100" →
  *"the code is in the world: one address space reaches from landscape to
  source span (SourceAnchors). The descent is a chain of labeled projections —
  continuity of identity, not of camera."*

---

## What we did not research (honest gaps)

- **Nomic Atlas / WizMap** (modern embedding maps) surfaced in search, not
  fetched — the legible-instrument stance rests on older spatialization work.
- **Archambault & Purchase (2013)** mental-map experiments surfaced, not
  fetched; my layout-stability claims rest on Ghani & Elmqvist + the Scarr
  survey instead. *(From general knowledge, flagged as such: A&P's results on
  mental-map preservation are more mixed than the revisitation work — worth a
  look before building the geography-ratification mechanics.)*
- **Prezi-vs-slides studies** — never fetched; no claims rest on them.
- **Uncertainty visualization** for calibration display — search hit only;
  calibration rendering is undesigned and unresearched. Fine for the braid
  slice; needed before calibration UI.
- **Category theory / ologs** — deliberately out of scope; the preservation-
  contract stance from the derivation doc stands unexamined by this review.
