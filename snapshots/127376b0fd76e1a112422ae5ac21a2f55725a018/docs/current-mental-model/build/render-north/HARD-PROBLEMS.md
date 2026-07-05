# Render North — THE HARD PROBLEMS: where the framework will actually bleed

**Status: DIRECTION (thinking layer), non-binding on build.** Written
2026-07-05, after Sid's challenge: *"Zed made theirs and I see them saying
how this was hard, that was hard… is that what a UI framework amounts to? I
am honestly disappointed."* The challenge is correct. `MECHANICS.md` is the
perimeter — each browser service answered once. A framework is not a list of
answered questions; it is **the small set of bottomless problems handled at
the bottom, plus a programming model that stays coherent when fifty answers
compose.** This document names those problems for Softland specifically,
takes each to where the difficulty actually lives, and — where the earlier
documents were glib or wrong — says so explicitly.

## 0 · What the people who did this actually bled on

Before our list, theirs — because Sid cited it and it calibrates everything:

- **Zed / GPUI:** frame pacing and input latency as an obsession (their whole
  pitch is "120fps editor"); they wrote their **own layout engine and own
  text system** after nothing off-the-shelf survived; their contrarian
  choice: **no damage tracking — redraw the entire window every frame** and
  win by being fast enough; years of Rust element-architecture iteration to
  make composition ergonomic. GPUI was built FOR Zed and generalized later —
  the framework grew against one demanding form.
- **Figma:** text was famously their hardest system (their own layout engine,
  their own text engine, years of edge cases); multiplayer took a bespoke
  protocol and a server rewrite; and they accepted "build a browser inside
  the browser" as the cost of correctness.
- **Mozilla / WebRender:** the retained pipeline's real difficulty was not
  drawing — it was **consistency and invalidation**: scene vs frame epochs,
  interning, per-tile damage, years to ship.

Mapped onto Softland: some wounds we dodge (Rust ergonomics — we're in CLJS;
platform text stacks — we already left them, same as Figma). Some we inherit
in full (**text**, **frame pacing**, and — worse for us than for Rust shops —
**GC pressure**). And three are NEW, ours alone, with no one to copy from:
the **diff-transducing interpreter** (H1), **transclusion identity** (H6),
and **dual projection** (K2 — no shipping system does it). That is the honest
shape of the mountain.

---

## H1 · The interpreter must be a diff TRANSDUCER, not a function
### (the technical heart; NORTH.md under-stated it)

**Where the difficulty lives.** NORTH.md said: "the interpreter folds
`(spec, query-results, camera)` into scene-store updates." Stated like that,
it's a function — and a function must re-run when inputs change. At land
scale the inputs change every keystroke, every ingest, every presence tick.
A re-run that touches O(scene) per change is death; the whole architecture
stands or falls on the interpreter having the signature

```
interpret : spec × Δdata → Δscene        (never: spec × data → scene, per change)
```

This is **incremental view maintenance** — the problem Jane Street built
Incremental for, the problem that makes UI frameworks hard rather than
tedious. Nobody hands us this: React reconciles by re-running components and
diffing output (acceptable because DOM subtrees are small); deck.gl re-runs
accessors over the whole data array on a trigger; MapLibre re-evaluates per
tile. None of them face "one keystroke inside one card among 10⁴ on screen."

**The tractable v1 (entity-scoped re-interpretation).** Electric's `diff-by`
keys give us the entity grain for free: a `:change` at key K means *only
entity K's* spec projection re-runs; its old instance set and new instance
set are structurally diffed (bounded — an entity emits 1–50 instances); the
delta writes through to slots. Cost per change = O(one entity), not O(scene).
Global effects (a lane re-flow after an insert) are bounded by the design's
own laws — see H3.

**The cliff this defers, honestly:** cross-entity derived visuals —
aggregation chips, lane assignment, label collision — are *joins over many
entities*, and entity-scoped re-runs don't maintain joins incrementally.
Naiad/differential-dataflow would; Electric explicitly is NOT that (verified,
INPUTS §6.6). v1 answer: those joins are **server-side projections in Rama**
(fold counts, lane indexes as PStates — Rama is literally an incremental
materialized-view engine; use it as one) or coarse client recomputes gated by
the `[RAF]` instrument. The framework line: **client interpreters maintain
per-entity projections; cross-entity structure is Rama's job.** If that line
ever breaks in practice, THAT is the form-break that would justify real
incremental-join machinery — pre-registered here so nobody sleepwalks into
building differential dataflow because one chip re-count got slow.

## H2 · Compile the spec, don't interpret it
### (correction: NORTH.md's framing was naive and would not survive 10⁴)

**Where the difficulty lives.** "View-specs are data interpreted live" reads
as: walk EDN per instance per frame. Walking `[:match [:get :kind] …]` in
CLJS for 10⁴ instances at 60Hz is dead on arrival. **The systems that made
data-driven styling fast all compile:** MapLibre compiles expressions into
closures evaluated **once per feature at data-load time**, results stored in
GPU attribute buffers, with zoom interpolation done **in the shader** from
precomputed stops; deck.gl runs accessors at update time into typed arrays.
The spec stays data (K1 intact); the *runtime* partitions and compiles it.

**The partition, which is the actual compiler design:** every style/policy
rule in a spec is classified by what it depends on —

| depends on | compiled into | re-evaluated when |
|---|---|---|
| nothing (constant) | buffer constant | spec edit |
| entity data (`[:get …]`) | per-instance attribute, computed at diff time (H1's per-entity run) | that entity's diff |
| camera/zoom (`[:zoom]`-outer) | shader-side interpolation params (stops as attributes/uniforms) | never on CPU — free per frame |
| interaction (hover/selection) | overlay stratum patch | pointer events only |

The camera-vs-data expression separation (MapLibre's rule, INPUTS §6.4) turns
out not to be a stylistic nicety — **it is the compiler's partition
criterion.** A spec edit recompiles (cheap, one view); data changes touch
attributes for changed entities; zoom costs zero CPU. That is how "agent
edits a view mid-conversation" and "60fps at 10⁴ instances" coexist.

**What this changes upstream:** NORTH.md's interpreter is really THREE
artifacts: a **spec compiler** (EDN → attribute-gen plans + shader params), a
**per-entity projector** (H1's diff transducer, running compiled plans), and
a thin **frame encoder**. The spec language must be designed to BE
compilable this way — total, statically partitionable, no dynamic dependence
that straddles the partition (e.g. a rule mixing `[:get …]` and `[:zoom]`
compiles to attribute × shader-interp composition, which is why MapLibre
restricts `[:zoom]` to the outermost position — adopt that restriction, it
is load-bearing, not pedantry).

## H3 · Layout incrementality — and the design law that saves us

**Where the difficulty lives.** Text wrap depends on width; height depends on
wrap; siblings depend on height; a one-character edit can reflow a whole
column — this cascade is why UI layout is a bottomless pit (and why Zed and
Figma both wrote their own engines). At world scale it would be fatal: one
card expanding cannot re-lay 10³ cards.

**The gift already in the constitution:** Sid's open-in-place law (D6 — "no
layout dependency between siblings; detail costs local space, never the
shared axis") is not just an aesthetic ruling. **It is the technical firewall
that makes world layout O(1) per change:** world positions are log-derived
and fixed (x=time, y=lane); opening a card overlaps rather than shoves; ergo
no world-scale reflow exists *by law*. The cascade is confined INSIDE one
island, where it's bounded (a panel's interior: tens-to-hundreds of nodes,
constraints-down/sizes-up, dirty-bit cutoffs — the Flutter discipline at a
size where it always wins).

**The remaining real work:** (a) island interiors need honest incremental
layout (dirty subtree only — rect-tree resolves whole trees today; fine at
current sizes, instrument-gated later); (b) **lane assignment on insert** is
the one legitimate world-scale layout event (a new thread claims a lane) —
it's a fold over the log, so it belongs to Rama (H1's line), arrives as
ordinary row changes, and animates as presentation (§9 of MECHANICS);
(c) text measure stays the island's opaque leaf (the Taffy seam). The trap
to never allow: any world position computed from another entity's *rendered
size* — that single dependency would re-couple the cascade the law severed.

## H4 · Frame pacing, the latency budget, and the epoch rule

**Where the difficulty lives.** Zed's whole identity is this problem. Ours:
RAF hands us ~1 frame of scheduling latency; a scene-build or encode that
exceeds ~8ms visibly hitches a 120Hz pointer; a keystroke's echo must beat
~30ms input→photon or typing feels wrong. The as-built path is honest here
(the `[RAF]`>5ms instrument, the identical?-skip making idle frames one
pointer compare) — but the framework question is not "are we fast today," it
is **what is the architecture when a frame's work exceeds its budget?**
Answers that must be designed in, not bolted on:

- **Priority encode, center-out:** when the dirty set is large (band cross,
  big diff batch), encode viewport-center instances first, fringe next frame.
  The scene store's spatial index (H6/pick grid) already knows what's central.
- **Time-sliced scene work with the EPOCH RULE:** scene updates may span
  frames, but a frame renders exactly one consistent epoch — never half-old,
  half-new (WebRender's hard-won discipline; the failure mode is a frame
  where a moved panel's shadow and body disagree). Concretely: double-buffer
  the dirty span or gate the frame on batch completion; never let the encoder
  read a store mid-transaction.
- **Input bypass lane:** caret echo and camera pan may not queue behind scene
  work — they are overlay-stratum writes (H7/MECHANICS §8) with a fast path
  to the next frame. Typing latency is a *product* property of the framework,
  not an optimization.
- **Zed's contrarian datum, weighed:** GPUI redraws everything every frame —
  no damage machinery at all — and wins because one editor window at 120fps
  fits the budget. That is a real warning against retained-machinery
  overhead… and it does not transfer: our scene is a *world* (10⁴–10⁵
  addressable, most of it cold), and our K2 face NEEDS the retained store
  anyway. But adopt the spirit: the skip path stays O(1), the retained
  machinery must never tax the idle frame, and if profiling ever shows the
  diff plumbing costing more than brute re-encode at our sizes, brute wins
  that round (measure, don't worship architecture).

## H5 · Allocation: two data models, and the GC dragon

**Where the difficulty lives.** Zed chose Rust substantially to kill GC
pauses. We chose CLJS, so the dragon is ours to manage: persistent-map
allocation inside the frame path = minor-GC stutter exactly when the camera
moves. The framework rule that keeps CLJS viable: **two data models, with a
compiler between them.** The programmer/agent sees EDN (specs, entities,
actions — allocation-rich, GC-friendly because it changes at *edit* rate).
The frame sees **structure-of-arrays typed arrays** (positions, colors,
bands, clip indexes — allocated once, mutated in place, zero per-frame
allocation beyond scratch). The boundary is the per-entity projector (H1):
it runs at *diff* rate, reads EDN, writes SoA. Nothing downstream of it
allocates. The as-built buffer pools half-learned this already (slot pools,
in-place writes); the framework makes it a law rather than a habit. And the
instrument exists: a GC-pause column belongs next to the `[RAF]` log the
first time the camera goes live.

## H6 · One address, many appearances — transclusion identity
### (correction: MECHANICS.md's scene-store diagram is WRONG as drawn)

MECHANICS.md's central diagram says `address → slot`. **That's wrong, and
the error matters.** Transclusion is core Softland: the same claim visible
simultaneously on the ground, in an open room, and in a search overlay. Same
address — three appearances, three positions, three LOD bands.

**The correct keying:** `(view-instance-id, address) → slot`, with a fan-out
index `address → #{appearances}`. Consequences, each load-bearing:
- **Picking** returns the pair: *what* you hit (address — for the action) and
  *where* you hit it (appearance — for local UI like which panel's ✕).
  Sid's box-in-box question, asked again at transclusion depth: the ✕ closes
  an APPEARANCE (a view mutation); retracting a CLAIM is an action on the
  ADDRESS (a log assertion). The pair keeps the two forever distinct — a
  framework that conflates them will eventually delete truth when someone
  meant to close a window.
- **Marks and presence fan out:** sign a claim in a room and its ground
  appearance glows in the same frame — one address write, N slot patches via
  the fan-out index. This is the magic moment of the whole product, and it
  costs one index lookup *if the keying is right from birth*.
- **The diff pipeline** (H1) is per (view, entity): one entity's data change
  fans to N appearance projections, each through its own view's compiled
  plans (different bands, different styles). N is small (2–5); the fan-out is
  the cheap direction.

## H7 · The optimistic write overlay — the center loop's client half

**Where the difficulty lives.** Click → ActionRequest → Rama → decision →
event → diff → render is a round trip. Typing cannot wait for it; dragging
cannot wait for it; at multiplayer distance (50–200ms) *nothing interactive*
can wait for it. So every write-capable framework needs a formal
**pending layer**: local echo applied instantly, keyed to the request,
reconciled when truth returns (confirm → drop echo; deny/divergent → visible
correction, never silent). The as-built sidebar overlay and the CLAUDE.md
golden rule (trace write → render → truth reconciliation → clear) are this
pattern ad hoc; the framework promotes it to a citizen: pending entries are
scene-store overlay patches with request-ids, rendered in the "proposed"
visual channel (the map may show my unconfirmed stroke — but *as* mine and
unconfirmed; honesty channels already have the vocabulary). Text input needs
the tight version (per-keystroke echo against Rama-ordered confirmation
within one island — the existing editor's measured 7.5ms round trip says
solo-local is a non-problem; the multiplayer case rides LWW-for-layout /
island-serialized text ops and is deferred with its milestone). D-008 defers
ALL of this — but the overlay stratum must be shaped for it now, because it
is the same stratum hover and drag-ghosts already need (MECHANICS §8).

## H8 · Commands, keymaps, focus — dispatch as data (the boring 40%)

Zed's dispatch tree, VS Code's `when`-clause keybindings — every serious
tool converges here: **keys → context-predicated bindings → named commands**.
We get the pieces free if we choose to: commands ARE action descriptors;
contexts ARE the focus path (which island, which mode — the hit/focus
machinery already yields it); so a keymap is pure data
`{context-pattern → {chord → descriptor}}` — spec-shaped, agent-editable,
D-010-consistent (a user's keymap is material in the land, versioned like
any view). The as-built per-pane keyboard consumers work at 4 panes and will
metastasize at N views; the dispatch table replaces them the same way the
registry replaces mode-`case`s. Also in this bucket, named so nobody
believes the framework is done without them: focus trapping in modals,
tab-order inside islands, tooltip/popup/portal lifecycles (overlay stratum +
anchor address + dismissal rules), drag-and-drop across islands (source
address, target address, payload = descriptor), clipboard (serialize
selection's addressed spans; paste = an action). None deep; all mandatory;
each is a week nobody budgets — Zed's blogs are full of exactly these.

## H9 · Text: the pit, and exactly where our moat ends

Figma's hardest system; Zed wrote their own. Ours is survivable ONLY because
of two standing choices, and the framework must know where their protection
ends: **monospace grid** (kills shaping, kerning, measure — cursor math is
column math) and **MSDF/slug atlases** (kills the per-size raster pyramid;
one atlas, every zoom). What the moat does NOT cover, with the cliff marked:
- **Unicode segmentation** — transcripts already carry emoji, CJK, combining
  marks. Cursor/selection must move by *grapheme cluster*, counting by
  codepoint (the B2 sanitizer got counting right; segmentation is the next
  rung and is needed the moment the editor hosts ingested material, not at
  some far horizon).
- **Line breaking** — greedy word-wrap suffices for mono cards; UAX-14
  correctness (CJK, no-break rules) arrives with CJK *content*, before CJK
  *input*.
- **IME** — composition (underline, candidate window anchoring) through the
  hidden-input trick; required for "others can visit," untestable except by
  hand, notoriously the last 5% that takes 25%.
- **Font fallback** — one merged atlas today; a fallback CHAIN (per-codepoint
  atlas election at shaping) is the real shape once material is multilingual;
  tofu-with-advance stays the honest floor.
- **The cliff:** the first proportional or shaped text (cartographic labels,
  CJK) opens the pit for real — shaping, kerning, measure caches, baseline
  grids. NORTH's ruling stands (a NEW text render-type beside the mono path,
  never a mutation of it), and this section is the cost tag on that registry
  event: it is the single most expensive primitive the registry will ever
  admit. Enter it eyes-open or not at all.

## H10 · Live spec evolution — hot-swap without losing the user's place

An agent patches a view you are *inside*. What survives? Scroll, selection,
collapse-state, camera — each keyed how? The rule that falls out of the
address discipline: **local state keyed by address survives (selection,
collapse, camera-target); state keyed by position dies (scroll-as-offset —
unless re-anchored to the topmost visible address, which is the correct
scroll representation anyway).** Spec migrations (tldraw's up/down) apply to
*stored* specs; the live-swap adds the Flutter GlobalKey lesson — identity
continuity across re-projection, which our addresses give structurally. The
renovation-shows-its-diff demand (D10) then costs only: keep the outgoing
projection one transition long and cross-fade — the same two-epoch machinery
H4 already requires. Cheap *because* of three other decisions; this is what
"framework coherence" means in practice.

---

## Corrections this document makes to the earlier two

1. **MECHANICS.md's `address → slot` is wrong** → `(view-instance, address)
   → slot` + fan-out index (H6). Correction note added in MECHANICS.md.
2. **NORTH.md's "interpreter" is really compiler + projector + encoder**, and
   the spec language must be designed for partitioned compilation (H2) —
   `[:zoom]`-outermost is load-bearing, adopt it.
3. **"Interpreted live" must mean "recompiled on edit, incrementally
   projected on data change"** — never per-frame EDN walking (H1+H2). K1 is
   about the spec's *material* (data), not its *execution strategy*.

## The honest effort statement

The bottomless five for us: **H1/H2 (the diff-transducing compiler — the
genuinely novel core), H4 (pacing), H9 (text), H7 (optimistic writes when
the write surface opens), H6 (transclusion identity — cheap only if born
right).** Everything else is bounded engineering. Zed spent years on GPUI
*against one form* (the editor) and generalized later — that is exactly the
D-001 shape: each hard problem here gets built against a real face demand,
not as framework-in-the-void. The two things that must be right at BIRTH
regardless (because retrofits touch everything): the scene-store keying
(H6) and the coordinate discipline (MECHANICS N6). The rest can — and per
D-001 must — earn their way in.
