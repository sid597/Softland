# Softland — The Architecture, In Principle

Status: **LIVING** · born 2026-08-13 (three parallel derivations → adversarial
court → Sid's commission; receipts banked in `docs/electric-native/RECON.md`
§11). Approved by default; Sid redlines anytime. Names are working
scaffolding — they finalize by recurrence, and the naming is Sid's.

What this file is: the principle register — each principle stated as a
CONFLICT resolved by a SEPARATION, with what it refuses and where it already
lives. What it is not: settled law (`docs/decisions.md` stays binding and is
pointed at, never restated) · the road (`docs/electric-native/DIRECTION.md`)
· the open problem (`docs/electric-native/PROBLEM-SPACE.md`).

Every line carries one of three marks: **SETTLED** (already ruled; pointer
only) · **PROPOSED** (default-approved, redline anytime) · **FORK** (only
Sid's word closes it).

## The big picture

Softland integrates and generalizes what normally ships as a dozen
products: a durable knowledge store, a rendering engine, a collaboration
layer, and the working memory of machine minds — one circulation, one set
of laws. The compass is Sid's sentence (LOG 2026-08-12, verbatim):

> "the reactive composition between rama→electric/missionary querying data
> that needs to be rendered ↔ rendering engine taking it and also asking
> for more ↔ user interacting and based on the interaction we add or
> remove the data needed"

Three arrows: truth flows down · demand flows up · interaction writes in.
Every architecture question in this repo is one of these arrows asking for
its invariants.

The foundation is Rama's founding principle, inherited whole: being a
source of truth and being an indexed store that serves queries are in
fundamental conflict — so separate them. Acts append to an unindexed log;
views materialize from it, denormalized freely, disposable and
recomputable; the log stays normalized and permanent, and it *appreciates*
(every better reader re-reads the entire past). Softland's claim on top:
**this conflict recurs at every altitude of an inhabited land, and the
same separation resolves it each time.**

One pattern at four altitudes — none was designed to mirror the others;
the correspondence was found, not imposed:

```
 altitude                the log              its views            the return path
 ──────────────────────  ───────────────────  ───────────────────  ─────────────────
 knowledge earth (North) claims·evidence·srcs the map at each zoom each visitor's
                                                                   assistant briefing
 sense-line (product)    episodes + marks,    standing views =     the morning
                         log-primary          answers to recurring answer; briefings
                                              questions
 the circulation (here)  acts in depots       store→frames for     catch-up from any
                                              humans · briefings   watermark
                                              for minds
 render seam (frames)    minted diffs         scene store → GPU    the oracle fence
```

An appreciating log · views per reader · a return path. The six conflicts
below are that pattern, stated with teeth.

## The six conflicts

```
 conflict                       separation                          mark
 1 truth vs views               acts apart from projections         SETTLED
 2 being truth vs being felt    two planes; settlement the door     PROPOSED
 3 identity at rest vs motion   identity travels every crossing     PROPOSED
 4 merge vs decision            truth is decided, not assumed       SETTLED+
 5 existence vs attention       cost follows attention (leases)     SETTLED+
 6 one truth vs many eyes       budgeted projections, one door      SETTLED+
```

### 1 · Truth apart from views — SETTLED

**Conflict** (Rama's own): a source of truth demands normalization;
serving demands denormalization; one store cannot do both.

**Law:** acts append to depots; every reader reads a materialized view;
views are disposable, the log is not. And the view ladder does not stop at
the server: the scene store is a view of served facts, the GPU pools are
views of the store, a briefing is a view for a token-budgeted reader, undo
and the wire's deltas are views of minted change. Recompute proportional
to change, at every layer — change minted once, at the site that knows it
(binding: decisions.md "The render seam"; its minted-diffs paragraph — one
consumer today, undo and the wire waiting — is this principle's sharpest
statement).

**Refuses:** truth outside the log · a view acting as truth. The mirror
quarantine is the fossil: ephemeral *buffers* are architecture, ephemeral
*truth* was the sin (INV-14; quarantine test-enforced,
`util_fns.cljc:104-112`).

**Receipt:** `event-id` rides every row defrecord in the store
(`object_container.clj:90-129`) — act linkage is already the resting
convention of all truth. Only motion strips it (conflict 3).

### 2 · Becoming apart from truth — PROPOSED

**Conflict:** being durable-decided-replayable and being felt-now at 60fps
are incompatible duties for one channel or one store.

**Law:** two planes with different physics. TRUTH moves as acts — ordered,
acked, replayable, every one matters. BECOMING moves as declared-lossy
flows — gesture, caret, token streams, presence — where conflation is
*correct* and each lane declares its merge algebra (latest-wins · concat ·
compose · sequence · acked). SETTLEMENT is the only door between the
planes: gesture-end mints one act (`arm-settle!`/`fire-settle!`,
`ground.cljs:2078,2122` — the codebase named this itself). Nothing
continuous touches a depot; nothing discrete waits on a frame. The record
wants punctuation, never video.

**Refuses:** events through conflating channels (the cap-64 and cap-8
scars are the fossils — RECON §2/§11) · continuous data in depots ·
frame-rate geometry sync (in-flight gestures stay client-side at 60Hz;
their settlements cross — settled, scene-substrate ruling).

**FORKS it feeds (Sid's):** trails — whether sampled motion becomes
material (as dwell/mark punctuation, never poses). The presence ring is
really TWO rings with two algebras — latest-wins poses need ~3 samples,
concat streams need the disconnection window — so ring depth *derives
from* the trails ruling; 120 is a placeholder, not a law.

### 3 · Identity travels — PROPOSED (the wire principle)

**Conflict:** the land is keyed and act-linked at rest, anonymous in
motion. Every crossing that strips key + cause forces the reader to
re-derive what the writer knew — N-shaped work for O(1) change — and
makes conflation lossy (the scars) and echo a stomp.

**Law:** every boundary crossing carries key and causing act. **Down:**
keyed batches stamped generation = the accepted act (the epoch bump is
already accept-gated: "not a replay, not a rejection",
`electric_flow.cljc:100-104`); order is row data, never position (the
measured permutation cliff, laws register L16). **Up:** demand is
membership at attention rate — grow/shrink against container indexes,
never position windows. Conflation becomes safe exactly when payloads are
keyed — the union-map echo is the working existence proof ("a union map
makes conflation lossless", `face_projection.clj:1691-95`). **Recovery is
one path:** boot = reconnect = catch-up = the live apply path replayed
from a watermark, never a second reconcile; the batch pull demotes to
oracle — never deleted — WITH its named duties transferred (the
committed-echo cross-check INV-19, `server_jetty.clj:2301-04`; the
cap-overflow reconcile, `block_edit_wiring.cljs:187-89`).

**Refuses:** anonymous whole-value crossings at truth rate · order as
position · a second apply path for recovery · deleting the oracle.

**Where decided:** the ledger-form open already in settled ground
("ephemeral notifications vs a reified op-log: undo and multiplayer
decide" — render seam, open register). Evidence banked: the revision log
half-exists — RevisionRow with time-prefixed order-keys and cursor-range
reads (RECON §11). The staged road is DIRECTION 1c.

### 4 · Truth is decided — SETTLED core · PROPOSED riders

**Conflict:** silent convergence vs reviewable trust — decisive in a land
where most acts will be machine acts.

**Law (settled, pointers):** propose → decide → apply. Provenance rides
everything (`asserted-by`; the map must not lie); two clocks on everything
that carries time; deterministic op-ids make replays converge. Rejection
is free before acceptance — a rejected act changed nothing. Decision rows
are the review surface: the morning-after answer ("what happened here
overnight — show me — let me reject #47") is a projection over rows that
already exist.

**Riders (PROPOSED):** the **seen stamp** — an act carries the watermark
of the view it was made against; one compare at apply buys conflict
detection without locks, honest undo (refuse when the ground moved), and
epistemic honesty (whether an agent contradicted the counter-evidence or
never saw it — arrived at W+4). **QoS hand-first** — interactive acts
preempt bulk, so an agent's thousand-act import never queues Sid's
keystroke.

**Refuses:** silent merge · machine output indistinguishable from Sid's
hand.

### 5 · Cost follows attention — SETTLED spirit · PROPOSED lens

**Conflict:** the log appreciates forever; attention and device budgets do
not. (Sid's thousand-editors example is this conflict lived: per-block
editors each fast, all live = dog slow on load and movement.)

**Law:** liveness follows attention; existence follows truth. Millions
exist, hundreds are resident, a handful are interactive, ONE is edited
(T2 visits the focus — settled). Demand is a **lease** — a reader's live
claim on a slice of truth: scope (membership) · pacing (frame clock for
humans, turn clock for minds) · budget. Zoom is the master epistemic axis
and representation degrades honestly by rung (settled: the spatial model).
Briefings budget tokens the way frames budget pixels. Machine attention is
budgeted by actor class: turn-scoped leases evaporate between turns;
standing watches ride coarse clocks — otherwise the agent era silently
converts cost-follows-attention into cost-follows-fleet.

**Refuses:** load-all boots (the 512-cap and 100k-cliff scars, RECON §5)
· unbudgeted warmth · per-frame subscription churn.

### 6 · Many eyes, one door — SETTLED spirit · PROPOSED symmetry

**Conflict:** pixels at frame rate, tokens at turn rate, semantic trees
for other eyes — one truth cannot wear one shape.

**Law:** every reader receives a budgeted projection through the same
serve door. **brief! = draw-frame! for minds:** a briefing is a render
pass — priority-ordered, honestly truncating, "composed from durable truth
through the same projection that renders the screen" (settled, verbatim —
the episodes ruling). Faces are data for every inhabitant (settled,
2026-08-12). Shared facts derive above the boundary, viewer-relative facts
below it, never across. Other-eyes (find-in-page, screen readers) arrive
as one more store consumer when lived want pulls them.

**Refuses:** transcripts as briefings · per-reader bespoke arteries ("the
Electric surface never grows per face" — the one-artery law, kept).

## The circulation — the shape the six force

```
            ┌────────────────────────────────────┐
            │  RAMA — decided truth        1 · 4 │  acts in depots ·
            │  (5 modules deployed)              │  views per need ·
            └───────┬────────────────────▲───────┘  event-id on every row
              keyed │                    │ acts through ONE grammar
        act-stamped │                    │ provenance · two clocks ·
            batches │                    │ op-ids · [seen] · hand-first
            ┌───────▼────────────────────┴───────┐
            │  THE LENS — one per reader   3 · 5 │  demand UP: membership
            │  scope · pacing · budget           │  at attention rate ·
            │  oracle beside, duties named       │  delivery DOWN: keyed,
            └───────┬────────────────────▲───────┘  per-lane algebra
            ┌───────▼────────────────────┴───────┐
            │  PROJECTIONS PER EYE           6   │  store → GPU frames for
            │  scene store · briefings · (a11y)  │  humans · briefings for
            └───────┬────────────────────▲───────┘  minds — same door
              60Hz  │                    │ input · attention
            ┌───────▼────────────────────┴───────┐
            │  BECOMING — beside everything  2   │  gesture · caret ·
            │  settlement is the only door up    │  streams · presence —
            └────────────────────────────────────┘  lossy by declaration
```

## What the architecture refuses — consolidated

- No truth outside the log (mirror lesson, INV-14).
- No view acting as truth; views are disposable, recomputable.
- No anonymous crossing at truth rate; no order as position (L16).
- No events through conflating channels (cap-64 / cap-8 fossils).
- No continuous data in depots; no discrete act waiting on a frame.
- No execution clock as an ancestor of derivation (render seam, settled).
- No silent merge; no machine output wearing Sid's hand.
- No ad-hoc live queries: live things are NAMED projections; a hot query
  gets promoted, request-shaped reads stay request-shaped (the
  REST-beat-Electric lesson, kept with honor).
- No load-all; no unbudgeted warmth; no per-frame subscription churn.
- No second apply path for recovery; no deleting a demoted stage.

## Staging discipline

Demote, never delete (the growth law, settled): a batch stage retires into
the oracle seat WITH a duties-transfer list — the page-pull's duties are
already named in code (committed-echo cross-check · cap-overflow
reconcile). Staged ≠ patchy: a stage names its replacement in the contract
(DIRECTION). The test a stage must pass to be called *stable* is one-path
recovery.

## The protocol instinct — why this document exists

Sid, 2026-08-13, verbatim: "I think maybe intuitively i am building the
architecture of softland to how it should be ideally like maybe my
intellectual goal is like protocol or architecture book equivalent idk … i
like rama for their implementation, product but also the principles they
build on."

The register above is that instinct practiced: conflicts → separations →
refusals → receipts — the genre Rama's own big-picture opens with. The
boundary contracts this register produces — acts up, keyed act-stamped
deltas down, briefings out, settlement between planes — are
protocol-shaped: they can outlive any host, renderer, or transport. The
book, if it comes, grows from this register the way the native discourse
protocol is ruled to grow — by emergence, names earned by recurrence.
