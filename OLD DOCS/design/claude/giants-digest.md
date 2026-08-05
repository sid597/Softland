# Giants Digest — Standing on Shoulders for Softland's Design

> **Status:** independent cut. NOT yet reconciled with `charter.md`, `orientation.md`,
> the `malleable-software/` subfolder, or the parallel `codex/` track. Reconciliation is
> a deliberate next step (the menu option that was deferred). Read this as "what the field
> already knows that Softland should not rediscover," then merge.
>
> **How to read each entry:** ONE load-bearing idea → how it lands in Softland → the
> failure mode it pre-pays (stated as an imperative *Directive*). This is meant to replace
> reading ~30 primary sources. Confidence is flagged at the bottom; verify the small
> "verify-before-public" set before citing anything in a published/shared artifact.
>
> **Boundary (so this doesn't become insight porn — your word):** the giants give you the
> *parts bin, the failure modes, and the evaluation criteria*. They do NOT give you the
> novel core (one continuous world from knowledge-landscape to source code) — that is still
> yours to invent. This doc only earns its keep if **each lane below terminates in a sketch
> tested against the real "how did Softland get here" task.** See the sketch obligations at
> the end.

---

## If you read nothing else — the 11 directives that fall out

1. **Abstraction before idiom.** Never pick "graph vs timeline vs 3D" before you've written the *task + data abstraction*. A beautiful view on a wrong abstraction is wasted work. (Munzner)
2. **Position must be honest.** If X/Y on screen don't *mean* something, you're building a hairball. Resist force-directed layout; make location = semantic coordinate. (Shneiderman semantic substrates; anti-tSNE)
3. **Never teleport — animate every transition.** Instant view changes destroy the user's mental map. (Bederson ZUI)
4. **Every altitude needs landmarks.** Name Softland's landmarks/edges/districts *per zoom level* before drawing anything. (Lynch)
5. **Compress by folding, not deleting.** Low-relevance content folds and stays recoverable; it is never severed. (Furnas DOI; cartographic generalization)
6. **The traversal is a first-class artifact.** The *path through* knowledge is authorable, saveable, shareable — not ephemeral scroll history. (Bush Memex trails)
7. **Transclude, don't copy.** One source, many views; edits propagate; provenance survives. (Nelson)
8. **Design the gutters.** Causality is inferred in the *cut between frames*, not inside a frame. (McCloud; Kuleshov)
9. **Arrange before formalize; arrangement is data.** Let users place things spatially before labeling them; capture the placement as meaning. (spatial hypertext)
10. **Renovation must show its own diff.** After the place changes itself, the inhabitant must *see* what changed, or the evaluation gulf swallows them. (Norman)
11. **Pace = challenge matched to skill.** Reveal complexity on a curve; never drop a newcomer into the full 10k-node world. (Csikszentmihalyi; game onboarding)

---

## 0. The spine — methodology (read these first; they govern everything below)

**Munzner — Nested Model for Visualization Design (2009) + *Visualization Analysis & Design* (2014)**
- *Idea:* four nested levels — (1) domain situation, (2) data/task **abstraction**, (3) visual/interaction **idiom**, (4) algorithm. Errors cascade *downward*; you can't patch a wrong abstraction with a clever encoding.
- *Softland:* your recurring instinct is to jump to level 3 ("what should the view look like"). The real unknown is level 1–2: what *is* the task "sensemaking / synthesizing / building on top"? Decompose it (trace-causality, assess-coverage, assess-confidence, re-find, hand-off).
- *Directive:* before any view, write the abstraction. If you can't state the task in one sentence, you're not ready to draw.

**Bertin — *Semiology of Graphics* (1967) + Cleveland & McGill (1984) + marks/channels**
- *Idea:* there is a *ground truth* to visual encoding. Bertin's visual variables (position, size, value, hue, shape, orientation, texture); Cleveland–McGill's perceptual ranking (position > length > angle > area > volume > color).
- *Softland:* spend your most precious channel — **position** — on your most important variable (semantic coordinate / altitude), not on aesthetics. Color and size are weak; don't make meaning depend on them alone.
- *Directive:* don't encode something critical (truth-status, recency) in a channel humans read poorly (area, hue). Reserve position for what matters most.

**Card, Mackinlay & Shneiderman — *Readings in Information Visualization* reference model (1999)**
- *Idea:* the pipeline raw data → data tables → visual structures → views, with **human interaction adjusting every stage**. Interaction is not a layer on top; it's woven through.
- *Softland:* maps almost 1:1 onto your back-arrow architecture (event log → materialized state → projection → user acts → back into the log). Your Rama substrate already *is* this reference model.
- *Directive:* treat interaction as a transform on the pipeline, not chrome bolted to a finished picture.

---

## 1. Lineage — the vision's grandparents (orient your radical claims in a tradition)

**Vannevar Bush — "As We May Think" / Memex (1945)**
- *Idea:* knowledge work is building and sharing **associative trails** through documents — the trail itself is the artifact, more valuable than any single doc.
- *Softland:* this *is* "papers should be logs." Your "how did we get here" view is a Memex trail through code+DG+tickets.
- *Directive:* make the traversal a saveable, shareable, forkable object. If trails are ephemeral, you've built a browser, not a Memex.

**Engelbart — "Augmenting Human Intellect" (1962) + NLS / Mother of All Demos**
- *Idea:* tools should *augment* (not automate) thought; the system, the user, and their methods co-evolve ("bootstrapping" — using the tool to improve the tool).
- *Softland:* "the refactor is self-modification" and "renovation" are pure Engelbart bootstrapping. Softland improving Softland is the thesis, not a side effect.
- *Directive:* measure features by *augmentation* (does the inhabitant think better?), not automation (did the machine do it for them). You explicitly want "do the work, not know it" — that's augmentation over automation.

**Ted Nelson — hypertext / Xanadu / transclusion / "intertwingularity"**
- *Idea:* **transclusion** (include by reference, not copy), visible bidirectional links, and the claim that knowledge is irreducibly interconnected ("everything is deeply intertwingled").
- *Softland:* your "one ontology, many projections" is transclusion. A claim shown in DG-view, code-comment-view, and ticket-view should be *one* object transcluded, not three copies.
- *Directive:* never duplicate content across projections; transclude so edits propagate and provenance survives. Copying is how silos are reborn.

---

## 2. TERRAIN — *you know where you are*

**Kevin Lynch — *The Image of the City* (1960)**
- *Idea:* people build a mental map of any inhabited space from five elements: **paths, edges, districts, nodes, landmarks.** "Imageability" = how easily a place yields a clear mental map.
- *Softland:* this is your single most useful import for "terrain must be navigable." A zoom level with no landmark and no district boundary is unnavigable by construction.
- *Directive:* enumerate Softland's five elements *at each altitude* (what's a landmark at zoom 0.01 vs 1.0 vs 100?) before designing any view. Low imageability = users lost.

**Bederson — Zoomable UIs: Pad++, Jazz, Piccolo (1994–2004) + Jul & Furnas "Desert Fog" (1998)**
- *Idea:* the *direct academic ancestor of semantic zoom.* ZUIs work — but the documented killers are **disorientation** and **"desert fog"** (zooming into empty/uniform space with no cue where to go or how to get back).
- *Softland:* your semantic zoom is a ZUI at planetary scale. The field already paid for these mistakes; don't re-pay them.
- *Directive:* (a) animate every zoom/pan — *never teleport*; (b) never allow a zoom target with no visible landmark (no desert fog); (c) always keep a "you are here + how to get back" cue. Bederson + Shneiderman's IEEE-VIS Test-of-Time was for *ordered* treemaps precisely because layout stability across updates preserves the mental map — instability is disorientation.

**Furnas — Generalized Fisheye Views (1986) + "Effective View Navigation" (1997)**
- *Idea:* **Degree of Interest = a-priori importance − distance from focus.** Show high-DOI in detail, low-DOI compressed-but-present (focus+context, no mode switch). The 1997 paper proves a space is only navigable if every target has a *findable scent trail* to it.
- *Softland:* DOI is the math behind "recoverable compression / lawful semantic zoom." Focus+context = your "camera dolly, not mode switch."
- *Directive:* compress low-DOI by *folding*, never by removing — and guarantee every node has a navigable scent path, or parts of the world become unreachable (not just hidden).

**Cartographic generalization / LOD / Tobler's First Law**
- *Idea:* maps don't *delete* detail as you zoom out — they *generalize* (simplify a coastline, merge towns) while preserving identity and topology. Tobler: "near things are more related than distant things."
- *Softland:* the formal model for "semantic zoom must preserve identity, provenance, and relations." Google Earth never mode-switches globe→street; that's your north star's actual mechanism.
- *Directive:* when an altitude drops detail, the *shape and relations* must stay true (generalization), not get scrambled (distortion). A simplified view that lies about structure is worse than no view.

---

## 3. WEATHER — *pacing; not overwhelmed*

**Pirolli & Card — Information Foraging (1995) + The Sensemaking Loop (2005)**
- *Idea:* analysis is two coupled loops — **foraging** (search/filter/read/extract) ↔ **sensemaking** (schematize → build case → tell story), driven by **information scent** (cues that predict value of going somewhere). The closest existing theory to what Softland *is*.
- *Softland:* your "ingest → inhabit → synthesize → build on top" is exactly this. The schematize step (turning foraged scraps into structure) is where understanding forms — and where most tools abandon the user.
- *Directive:* don't over-invest in foraging (search) and starve schematizing. The hardest, highest-value design surface is the *structure-building* step, not the search box.

**Csikszentmihalyi — Flow (challenge/skill balance)**
- *Idea:* engagement lives in the channel where challenge matches skill; too hard → anxiety, too easy → boredom. Skill rises, so challenge must rise with it.
- *Softland:* this *is* "weather must be survivable." A newcomer and a power-user need different challenge levels in the same world.
- *Directive:* design a difficulty *ramp*, not a flat surface. Never drop a first-time visitor into the full 10k-node world; reveal complexity as competence grows.

**Shneiderman — "The Eyes Have It" mantra (1996): overview first, zoom & filter, details on demand**
- *Idea:* the canonical interaction sequence for any data type.
- *Softland:* your semantic zoom is a radical generalization of it. **Its known limit (use this):** "overview first" breaks when the data has *no natural overview* (high-dimensional, no inherent 2D layout) — which is exactly why your links reach for tSNE/UMAP and parallel coordinates. The mantra assumes an overview *exists*; for knowledge it must be *manufactured*, and manufacturing it honestly is the hard part (see Calibration).
- *Directive:* don't assume an overview exists — design how you *construct* one, and make that construction honest.

**Game design — difficulty curves, MDA (Hunicke/LeBlanc/Zubek), Sid Meier ("a series of interesting decisions"), Swink's *Game Feel***
- *Idea:* games are the only medium that solved "drop someone into a complex system they don't understand and have them learn it by *doing*, joyfully." MDA: designers build Mechanics, players experience Aesthetics — you can't author the feeling directly, only the rules that produce it. **Diegetic UI** (info lives *in* the world — Dead Space) removes chrome. "Game feel" = responsiveness/juice = your feelings-first bar.
- *Softland:* this is your #2 load-bearing domain (after architecture). Camera-as-first-class-verb, spatial memory, onboarding-by-doing, diegetic info-in-the-world all transfer directly to a *place*.
- *Directive:* prefer **diegetic** information (in the world) over chrome panels; treat the camera as a designed verb, not a viewport; you author rules, the *feel* emerges — playtest for it.

---

## 4. HISTORY — *trails, provenance, time (your killer-demo lane)*

**Scott McCloud — *Understanding Comics* (1993): closure & "the gutter"**
- *Idea:* readers construct narrative and **causality in the gap between panels** (the gutter). Juxtaposing two frames *creates* the "this led to that" — the meaning isn't in either frame.
- *Softland:* your causal-braid ("how we got here") is a sequence of frames (commit, claim, ticket). The *cut between them* is where the user infers causation. Under-used theory; punches above its weight.
- *Directive:* design the **transitions/cuts** in the history view as carefully as the frames. The story lives in the gutters.

**Film montage — Eisenstein / the Kuleshov effect**
- *Idea:* meaning emerges from *juxtaposition and ordering* of shots; continuity editing keeps the viewer oriented across cuts (don't break spatial continuity arbitrarily).
- *Softland:* "papers as logs" is montage of a thinking process. Continuity editing = your zoom/time transitions must not disorient (ties back to Bederson "never teleport").
- *Directive:* order and adjacency are authoring choices that *create* meaning — the trail's sequence is content, not just a list. Preserve continuity across every cut.

**Provenance & time-travel (synthesis — capture is the hard part)**
- *Idea:* "show how we got here" is a provenance/causality problem, and the bottleneck is **capture, not display.** You can only render the braid (commit ↔ ticket ↔ claim ↔ reasoning) if the joins were recorded.
- *Softland:* your back-arrow architecture already solves capture — the append-only event log *is* the provenance substrate; the object-container kernel is the join layer. The design job is to *project* the existing log as a navigable causal narrative.
- *Directive:* design requirements flow **backward into the ingestors** — code ingestor keeps commit messages/timestamps, Linear ingestor keeps ticket↔commit links, DG ingestor keeps claim provenance. No capture → no demo. Specify the joins *now*, while ingestors are being built next door.

---

## 5. CALIBRATION — *the map must not lie*

**Tufte — graphical integrity, the *lie factor*, data-ink ratio, small multiples**
- *Idea:* "Lie factor = size of effect shown ÷ size of effect in data" (should be ~1). Maximize data-ink, minimize chartjunk. **Small multiples** (same chart repeated across a variable) let the eye compare honestly.
- *Softland:* directly governs "lawful compression" — when zoom drops detail, the *visible change must equal the real change*. Small multiples are a clean idiom for "the same local world across versions/agents."
- *Directive:* audit every semantic-zoom transition for lie factor. If folding detail exaggerates or hides a real change, the map is lying — that breaks the core habitability criterion.

**Uncertainty visualization (the HCI subfield)**
- *Idea:* showing data is easy; showing *how much to trust it* is a distinct, hard problem (error bars lie too; people read them as ranges-of-possibility wrongly). Techniques: hypothetical outcome plots, value-suppressing palettes, explicit confidence encodings.
- *Softland:* this is the literature for "fact vs hypothesis vs guess" — your calibration criterion is an uncertainty-visualization problem with a name and a body of results.
- *Directive:* truth-status (fact/hypothesis/guess) needs a *first-class, legible* encoding — and pick the channel carefully (don't bury it in hue; humans read hue poorly, per Bertin). Untrustworthy-but-pretty is the most dangerous failure mode for an epistemic interface.

**tSNE / UMAP + Inselberg parallel coordinates — high-dimensional honesty**
- *Idea:* tSNE/UMAP project high-dim data to 2D, but **between-cluster distances are not meaningful and cluster sizes lie**; layouts are hyperparameter-sensitive (perplexity) and can manufacture illusory clusters. Parallel coordinates (Inselberg) are an *honest* high-dim idiom but clutter badly and depend on axis ordering.
- *Softland:* your knowledge-landscape "overview" will be tempted to use these. They make beautiful, *dishonest* maps if misread — the exact opposite of "the map must not lie."
- *Directive:* never let a dimensionality-reduction layout imply a distance/size it can't back up. If you use embeddings for the landscape, *annotate what the geometry does and does not mean* (ties to "position must be honest").

---

## 6. VISITORS — *public form, transferable, plural*

**Baldonado, Woodruff & Kuchinsky — Guidelines for Using Multiple Views (2000)**
- *Idea:* rules for when to use coordinated multiple views vs. a single one — diversity, complementarity, **parsimony** (don't add a view without clear gain), decomposition, and the cost of forcing users to mentally integrate across views.
- *Softland:* the antidote to the "all-in-one view" trap. Either *one continuous space* (your semantic zoom) OR *coordinated views* — but every extra view has a context-switching cost.
- *Directive:* justify each pane. The 3-pane (material/process/artifact) recurs because it's parsimonious; don't multiply panes without a Baldonado-style reason.

**Bret Victor + Nicky Case — Explorable Explanations (2011) / ncase.me / LOOPY**
- *Idea:* replace static prose with **models you can poke, break, and play** — understanding is built by interaction, not reading. Victor: "Up and Down the Ladder of Abstraction" (slide a parameter, watch the whole space). Case: LOOPY (causal-loop sims), "Parable of the Polygons."
- *Softland:* this is *literally* "REPL for knowledge," "3D C. elegans you can poke," "papers as logs you build piece by piece." The artifact pane is an explorable, not a display.
- *Directive:* the preview/artifact pane must be *manipulable* (a REPL for the current local world), not a rendered output. If the reader can't break it, it's a picture, not an explorable.

**Spatial hypertext — Marshall & Shipman (VIKI/VKB): incremental formalization**
- *Idea:* people express structure **spatially before they can formalize it** — they pile, cluster, and align objects, and that arrangement carries meaning the system shouldn't force them to label prematurely ("information triage").
- *Softland:* this is the deep cut behind "attention/arrangement is content" and "commands are larval workflows." Formalization should be *earned*, not demanded up front.
- *Directive:* let inhabitants arrange before they label; **treat spatial arrangement as first-class data** (an event in the log). Forcing premature formalization kills the exploratory mode where understanding actually starts.

**CSCW — beyond "cursors of all the people"**
- *Idea:* your README's gripe is real and named: most "collaboration" is *synchronous presence* (cursors). The richer literature is **asynchronous, artifact-mediated** collaboration — handoff, annotation, divergence/merge, and the social construction of shared meaning.
- *Softland:* "conflicts are good for progress" + "preserved disagreement until synthesis" is a CSCW stance: collaboration as *composable artifacts and preserved plurality*, not co-located cursors.
- *Directive:* design for **handoff and divergence**, not just co-presence. A local world must be enterable by another mind without you in the room (your "composable handoff").

---

## 7. RENOVATION — *the place grows with its inhabitants*

**Ink & Switch — "Malleable Software" essay + "Local-First Software" (Kleppmann et al. 2019)**
- *Idea:* software should be *moldable by its users* from the inside; local-first = you own your data and can keep working/forking without the cloud as gatekeeper (7 ideals: fast, multi-device, offline, collaborative, long-lived, private, user-controlled).
- *Softland:* your "renovation" criterion and "the editor IS Softland at zoom 100" are the malleable-software thesis. *(Note: a `malleable-software/` subfolder already exists in this track — flag for reconciliation; this entry likely overlaps prior work.)*
- *Directive:* renovation must be reachable *from inside the place by the same camera* — if changing Softland requires dropping into a separate dev mode/tool, it isn't malleable, it's just configurable.

**Geoffrey Litt — end-user programming (Wildcard, Riffle, Potluck, browser-customization work)**
- *Idea:* close the gap between *using* and *programming* — let non-programmers reshape their tools via direct manipulation over a live data substrate; "small-scale" personal software.
- *Softland:* the bridge between zoom-1 (using) and zoom-100 (code). Your "requests flow down, malleable software flows up" collaboration model is Litt's end-user-programming loop at organization scale.
- *Directive:* the path from "I wish it did X" to "it now does X" should traverse zoom levels *continuously*, not teleport a user into an IDE. Design the gradient between inhabit and modify.

**Christopher Alexander — *A Pattern Language* / *The Timeless Way of Building* / QWAN**
- *Idea:* recurring spatial problems have reusable *patterns* (each = problem + context + solution); good places have "the Quality Without A Name" (QWAN) — a felt aliveness. Directly seeded software patterns (GoF) and the wiki (Ward Cunningham).
- *Softland:* your "feelings first" evaluation *is* QWAN — you're judging aliveness, which Alexander argued is real and namable-by-pattern. Your 3-pane / split / JIT-component instincts are an emerging *pattern language*.
- *Directive:* catalog Softland's recurring spatial solutions as **named patterns with the context they solve for** (your JIT/component thread). A pattern without its context is cargo-culting.

**Don Norman — *The Design of Everyday Things*: affordances, signifiers, the two gulfs**
- *Idea:* usability = bridging the **gulf of execution** (can I tell how to act?) and the **gulf of evaluation** (can I tell whether it worked?). Affordances + signifiers + feedback + mapping.
- *Softland:* for a *self-modifying* place, the **gulf of evaluation is brutal** — after the world renovates itself, can the inhabitant *see* what changed and whether it's what they wanted?
- *Directive:* every renovation must **show its own diff** and confirm intent. A place that silently rewrites itself maximizes the evaluation gulf — the inhabitant loses trust and their mental map at once.

---

## Confidence & what to verify before public use

- **Canonical-solid (cite freely):** Munzner nested model; Lynch's five elements; Shneiderman's mantra; Pirolli & Card foraging/sensemaking; Furnas fisheye/DOI; Bertin visual variables; Cleveland–McGill ranking; Tufte lie-factor/data-ink; Bush Memex; Engelbart augmentation; Nelson transclusion; McCloud closure; Norman gulfs; Csikszentmihalyi flow; tSNE/UMAP distance-distortion caveat.
- **Confirmed by your own links (so: solid):** Shneiderman & Bederson IEEE-VIS Test-of-Time for ordered treemaps; Ink & Switch "Malleable Software" essay; Inselberg parallel coordinates; tSNE-vs-UMAP framing.
- **Verify exact attribution before publishing** (I'm reconstructing specifics from trained knowledge, not a fresh read): Jul & Furnas "Critical Zones in Desert Fog" (1998, year/title); Baldonado et al. AVI 2000 exact rule names; Kleppmann et al. "Local-First Software" (2019) seven-ideals wording; MDA framework authorship (Hunicke, LeBlanc, Zubek 2004); specific Litt project scopes. These are *directionally* correct; check the citation strings if they go into a shared/published doc.
- **Method note:** this digest is synthesized from my knowledge of the field, not from fetching each source this session. If you want primary-source rigor on any entry, I can web-fetch and tighten it.

---

## Sketch obligations — what each lane owes (so this isn't insight porn)

Each lane must produce ONE concrete sketch tested against **"how did vibe-coded Softland get here?"** (the chef's-kiss case). Minimum viable set:

| Lane | The sketch it owes |
|---|---|
| Terrain | Name Softland's landmarks/edges/districts at zoom 0.01 / 1.0 / 100 — one labeled frame each. |
| Weather | The onboarding ramp: what does a *first* visit to the Softland world reveal vs. the 100th? |
| History | The causal-braid storyboard for one real question (e.g. "why did object-container split source adapters?") — design the gutters. |
| Calibration | One frame showing fact vs. hypothesis vs. guess across the same content, with the channel chosen deliberately. |
| Visitors | What a *handoffable* Softland local world exposes (material/process/artifact/provenance/tensions) — one packet spec. |
| Renovation | The continuous path from "I wish it showed X" → zoom-100 edit → new version flows up — no mode switch, with a visible diff. |

**Recommended first:** the **History** sketch — it's your stated success metric, it forces the capture requirements onto the ingestors *while they're being built*, and it's the one you can *feel*. Everything else can follow it.
