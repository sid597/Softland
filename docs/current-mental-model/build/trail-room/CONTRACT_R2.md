# CONTRACT — trail-room R-2 (bands · lanes-from-edges · move chips) · v1.1 — PROPOSED

**Status: PROPOSED (Fable-authored 2026-07-05, delivery session, Sid AFK).
Binding only after Sid's countersign. The build does NOT start without it.**

**v1.1 amends v1 after `CONTRACT_R2_VALIDATION_R1.md` returned FAIL** (B1:
cross-band bounds identity was geometrically impossible and contradicted
R-1's shipped bounds-track-paint ruling; B2: the order-as-data gate was
incoherent against the arrival-sorting builder). Both blockers' smallest
fixes adopted verbatim; S1–S7 should-fixes and A1–A4 advisories folded. The
FAIL artifact is kept verbatim per process. A second validation round is
Sid's call at countersign (no time-box blanket stands today; every finding
was addressed mechanically per the validator's own fix text).

Authority chain: `design/claude/room-card-lane-2026-07-05.md` (sitting-2
rulings R4–R7 + handoff items 1, 5, 6 — the demands and CHECKS are binding
here), the do-not-preclude ledger, `decisions.md` (incl. the 2026-07-05
delivery-mode ruling in D-006 notes), trail-room R-1
CONTRACT/PHASE_R1/RETRO, `build/git-spine/GATE_REVIEW.md` (clip idiom + open
doubts), and `build/render-north/PROBE-10K.md` (pre-recorded probe
obligations, §Traps 3 and 11 below). WP1 wrappers (feed + bundles) remain the
ONLY data surface.

## §1 Scope — handoff items 1, 5, 6 (+ two R-1 debts)

- **Item 1 — band-aware card builders (R4/R5).** `cards.cljc` gains a band
  parameter. Bands 0–2 emit LINE ops (typography — no box node, no fill):
  band 0 = glyph + fold-count chip ONLY (v1.1/S5: the staleness dot needs
  `:last-attested-ms`, which lives on the per-target bundle, not the feed —
  it arrives with the D-008 §5 attestation walk, deferred); band 1 = ONE
  line `glyph name`; band 2 = title + ONE second line (compressed two-clock
  stamp + asserter; written-by only when it differs). Band 3 = the surface
  (the current open card/expansion, plus printed element address — already
  R-1), a SEPARATE sibling node; the title/handle node stays the closed-card
  click target. **Bounds rule (v1.1, B1 — per-band, never cross-band): at
  each band the hit-region node bounds equal that band's painted line
  region, so the click target matches the paint (R-1 trap 6 "bounds shrink
  WITH the paint"); bounds MAY differ across bands — band 0 is the densest,
  that compression is the point.**
- **Item 5 — lanes from edges, band for the rest (R7).** Thread assignment
  consumes lineage-kind edges (`based-on produced built-over new-direction`)
  as connected components over the feed's `:relation-transition` details.
  Cross-link kinds (`references elaborates`) NEVER merge lanes — they render
  as kraft connectors BETWEEN lanes. `family-key` demotes from lane-maker to
  FOLD rule (doc + its `du:` blocks fold vertically into one card).
  **Fold-vs-component precedence (v1.1, S3): fold FIRST, then the folded
  unit is assigned by the DOC's (fold head's) edges; a folded member's own
  lineage edge counts as the head's for component purposes.** Entries with
  no lineage edge go to **the band**: one designated region at the bottom,
  self-declaring (`N unthreaded · no asserted relations yet`), wrap-packing
  INSIDE the band only, reading order = time order. Never auto-filed into
  fake lanes, never hidden. **Thread overflow (v1.1, S4 — design R7's own
  DEFAULT): more threads than viewport lanes → the overflow threads render
  as fold-chips with counts; mod-wrap NEVER applies across threaded lanes**
  (band-internal only — the F-L2 staircase must not return by the other
  door).
  **Geometry note (v1.1, S7 — honest scoping):** R-2 builds on the as-built
  F-L5 vertical stack (y = time, x = lane indent). The design's "x = time
  everywhere" LAW and the ledger's "x = time (given), y = lane" are NOT
  satisfied by this geometry — R-2 does not change that; it substitutes
  "reading order = time order" as the interim honest reading and records the
  axis question as a deferred do-not-preclude item for the design track.
  Nothing R-2 builds may make the axis swap harder (positions stay derived,
  never hand-placed).
- **Item 6 — move announcement (R7 arrival choreography).** A card whose
  thread assignment changed since the last pull carries a new-since chip with
  the sayable reason (`joined thread · <kind> <far-end display-name>`), never
  a silent reshuffle.
- **R-1 debt 1 — sidebar read.** In trail faces the LAYOUT must consume the
  derived world's `:sidebar-visible` (or equivalent), not the raw sidebar
  atom — R-1's guard was report-only (`GATE_REVIEW.md` writers table).
- **R-1 debt 2 — kraft label truncation-vs-naming, made explicit.** Paint
  truncates kraft labels at card-w; the FULL far-end id rides node data
  (`:trail-face/off-screen`); hover/copy (this package's chip/label hit
  affordances may consume it, full hover UI may land later) is the
  full-name affordance. R6's "names the far end" is satisfied at data + best
  paint; unbounded label bleed is the worse map-lie (gate-review ruling).

NOT in scope: kernel/server work of any kind; `trail_view.clj`; View-3 text
face changes; new faces; zoom/altitude → band mapping (band is a view-state
value this tranche, see §2.2); persistent hover/tooltip UI; curves/beziers
(INV-17 stands — thin rects only); attestation delta in the rim (D-008 §5
milestone); rim changes of any kind (R-1 closed them).

## §2 Adjudications (Fable, recorded; alternatives preserved)

**2.1 Threads are computed client-side over the feed's own edges — no new
data surface.** The scene already collects `:relation-transition` details
(`rt-edges` in `scene.cljc`); connected components run over those. Grounds:
D-005 (the view renders NOW on ingested material), WP1 §7 (wrappers are the
only surface), and the band rule makes missing edges an HONEST state, not a
data gap. Alternative NOT taken: a server-side thread projection/query —
zero new query machinery is the standing win; promote only if the client
component pass shows up in frame profiles (form-break evidence, D-001).

**2.2 Band (altitude) is a view-state value with a palette command
[DEFAULT].** `view-state` gains `:band` ∈ {0,1,2}, default **2**;
`parse-trail-command` gains `/trail band <0|1|2>`. An OPEN card is band 3
for that card regardless of the global band (open = the user paid for local
space). Grounds: R5's bands are zoom-driven eventually, but no zoom exists on
this face; a palette command makes bands drivable in daily use TODAY (D-001
fuel) and precludes nothing — the zoom mapping later replaces the command's
source, not the builders' parameter. Alternative NOT taken: fixed band-2
only — cheaper, but then R5's grammar ships untestable in use.

**2.3 Thread identity is the lexicographically-smallest MEMBER target-id
(thread-scoped, deterministic, stable under growth), where "member" is
defined (v1.1, S3) as the set of VISIBLE-card / edge-endpoint target-ids —
folded members (`du:` blocks and other fold children) are EXCLUDED from the
identity min** (they'd win the sort — `d` < `o` — and pin a thread's
identity to an invisible row). When two components MERGE via a new edge, the
union keeps the smaller id — one side's entries "moved" (true: they joined a
thread) and fire chips; the other side's do not (validator trace B confirms:
chips fire only for the changed side, including when the surviving id comes
from the OTHER component). Grounds: ids derived from member SETS change on
every growth → chips fire for everything (trap 8). Alternative NOT taken:
earliest-member-by-time id — also stable, but time ties on same-ms imports
make it nondeterministic without a tiebreak that ends up being… the
lexicographic id.

**2.4 Move detection state lives client-side, keyed by target-id, in a
SEPARATE post-build cache atom (the `!last-trail-struct` pattern,
editor_compute.cljs) — NEVER inside `!trail-face-state` (v1.1, S2).**
Grounds for the never: `!trail-face-state` is a WATCHED INPUT — wiring's
`add-watch` re-pulls and editor_compute's `m/watch` rebuilds on every write,
so storing per-build output there is a rebuild/re-pull feedback loop, and
the write would sit inside `m/latest` (the CLAUDE.md side-effect ban). The
prev-assignment `{target-id → thread-id-or-band}` map is written AFTER scene
build as a cache, read at the next build; `moves prev-assign new-assign →
{target-id reason}` stays pure. "Since last pull" is a CLIENT notion (the
rim's delta slot already reads the arrival clock); no server state exists
for it and none is warranted pre-attestation-walk. Alternative NOT taken:
persisting move history — that is walk/attestation territory (D-008 §5).

**2.5 Ordering is DATA, never sequence position (probe obligation,
PROBE-10K:9-15; reworded v1.1 per B2 — the validator's fix adopted
verbatim).** The builder derives every position from the ARRIVAL-MS
ATTRIBUTE (canonicalizing by sort on that field); raw `:feed/entries`
sequence position is never load-bearing. Thread index, band membership, and
in-thread position are per-entry attributes; assignment/move APIs return
entry-key/target-id-keyed MAPS, never a re-sorted entry sequence as a
contract surface. Grounds: order riding the incseq is the measured 10³ knee
(`:permutation` diffs); when the differential feed arrives, thread moves
must be change-shaped (attribute updates), which attribute-derived
positioning gives for free — a permuted input stream canonicalizes to the
IDENTICAL scene instead of demanding a reorder diff. This is the
pre-recorded obligation from the baton, now binding.

## §3 Traps ledger (naive → concrete failure → ruling)

1. **family-key stays a lane-maker** → when edges land, a doc sits in its
   family lane AND its edge thread — two homes, double spines. → family-key
   is ONLY the fold rule (§1 item 5); lanes come ONLY from lineage edges.
2. **Cross-link kinds in the component relation** → `references` chains merge
   unrelated threads into mush; the wall's lineage arrows lose meaning. →
   components over `#{:based-on :produced :built-over :new-direction}` ONLY
   (pin the set as a named def; cite this trap at its def site).
3. **Order rides the sequence** → a future differential feed ships
   `:permutation` diffs on every thread move → the PRODUCER knees at 10³
   (PROBE-10K §2 + §6; §3 shows the consumer side is fine — citation fixed
   v1.1/A1). → §2.5; positions derive from the arrival-ms attribute;
   assignment APIs return keyed maps; the feed seq is never re-sorted as an
   API surface.
4. **Band wrap leaks into threaded lanes** → mod-wrap across threads chains
   unrelated spines (the F-L2 staircase fix's own footgun, already fenced in
   `lanes.cljc` 1-arity/2-arity split) → wrap-packing INSIDE the band region
   only; threaded lanes never wrap.
5. **Silent reshuffle on edge arrival** → a card teleports between pulls with
   no account — the "nothing hand-placed" law reads as layout lying. → every
   assignment change carries the chip + sayable reason (item 6); G6 asserts
   non-movers carry none.
6. **Box-cards at bands 0–2** → R4 regression; 167 filled boxes again
   (F-L5). → bands 0–2 emit zero rect-fill ops for closed cards; G1 asserts
   it op-level.
7. **Hit bounds diverging from paint at a band** → clicks land on empty
   space or miss painted lines (R-1 trap 6, still live). → PER-BAND
   consistency (v1.1, B1): at each band the node bounds equal that band's
   painted line region; bounds legitimately DIFFER across bands (band 0 is
   densest); the open surface is a separate sibling node.
8. **Thread-id from member sets** → id changes on every growth → chips fire
   for the whole thread every pull. → §2.3 stable id.
9. **New text ops without a clip? ancestor** → the R-1 overflow class
   (gate-review fix): band lines, chips, and band-count labels bleed past
   card-w/viewport. → text-in-child + `clip?` idiom for every new
   text-bearing painted node; `rect_tree`'s intersection semantics (commit
   dc743d6) is the platform this relies on.
10. **Glyph assumptions** → `↓ ● ✗` etc. missing from the merged atlas render
    as tofu (R-1 trap 5 fired: ⊢ was absent). → verify every NEW glyph
    against `resources/public/font_atlas.json` BEFORE use; substitute nearest
    covered glyph and RECORD it in the phase artifact.
11. **DOM-shaped incremental bridges** → `gpu-mount`-style insert-before
    corrupts slot pools on `:permutation` (PROBE-10K §4, 16,750 slots for 100
    entities). → this package introduces NO incremental consumer; if any
    stage is tempted, stop-clause — the C2 six-ops-direct shape is a
    render-north decision, not a trail-room one.
12. **NUL bytes in fixtures/docs** (fired 4× historically) → `file(1)` must
    say text for every touched file; gate G10.
13. **Per-build output written into a WATCHED input** (v1.1, S2) →
    prev-assignment stored in `!trail-face-state` re-fires wiring's
    `add-watch` (re-pull) and editor_compute's `m/watch` (rebuild) — a
    feedback loop, with the write sitting inside `m/latest` (the CLAUDE.md
    side-effect ban). → §2.4: a separate post-build cache atom, never the
    view-state atom.

## §4 Gates (executable; additions to `trail_face_test.clj` + fixtures)

- **G1** (item 1 check, R4; re-scoped v1.1 per B1): at band 1 a feed entry
  renders EXACTLY one text op and ZERO rect-fill ops; band 0 →
  glyph(+fold-count) only, no name text, no staleness dot (S5 — deferred
  with the attestation walk); band 2 → exactly two line ops; band 3 (open) →
  the surface node with bg as a SEPARATE sibling. WITHIN each band, the
  hit-node bounds equal that band's painted line region (paint/hit
  consistency — the click still lands); bounds MAY differ across bands, and
  the gate asserts band-0 height < band-2 height (the compression is the
  point). Never asserts cross-band identity.
- **G2** (R5 data rule): the band-2 second line is built from entry DATA
  (two-clock stamp + asserter; written-by only on divergence) — assert it
  never contains material/body text (tripwire 4: not a truncation of
  content).
- **G3** (item 5 check): fixture feed with a 3-commit `:based-on` chain + 2
  unedged docs → the chain occupies ONE lane; both unedged docs sit in the
  band region (below all threaded content) with the self-declaring count
  line; NO unthreaded doc occupies its own lane; band reading order = arrival
  order. Overflow half (v1.1, S4): with max-lanes 1 and two distinct
  threads, the overflow thread renders as a fold-chip with a count — never a
  mod-wrapped lane. Fixture note (v1.1, validator trace A): the current
  feed.edn carries only `based-on/dead-end/confirms` marks and no terrain
  chain — G3/G4/G5/G6 REQUIRE fixture extension per duty §7.4 (extend, never
  invent shapes; mirror the live constructors).
- **G4** (cross-links): a `:references` edge between members of two threads
  does NOT merge them (assignments unchanged); it renders as a kraft
  connector between the lanes (R6 material, existing kraft constructors).
- **G5** (fold rule): a doc + its `du:` blocks render as ONE card at bands
  0–2 (family fold); at band 0 the fold renders as a chip with a count.
- **G6** (item 6 check, pure): `moves(prev, new)` over a fixture where one
  doc gains a `:produced` edge → exactly ONE move record, reason names the
  kind + the far end's display-name WHEN PRESENT, else its raw id (v1.1, S6
  — matching duty §7.6's honest fallback); the moved card's ops carry the
  new-since chip; every other card carries none. Determinism: same inputs →
  identical output.
- **G7** (§2.5 probe obligation; reworded v1.1 per B2 — the validator's fix
  verbatim): (a) a permuted-input copy of the fixture yields a
  BYTE-IDENTICAL scene (positions derive from the arrival-ms attribute, so
  raw sequence position is provably not load-bearing); (b) thread/band
  membership + in-thread index arrive as node DATA (`:trail-face/thread`,
  `:trail-face/band?`) on every entry node — INCLUDING kraft mark nodes
  (v1.1, A3: an edge belongs to the thread its endpoints define); (c)
  assignment/move APIs return entry-key/target-id-keyed maps, never a
  re-sorted entry seq.
- **G8** (determinism, standing): same fixture → identical scene twice.
- **G9** (constraints re-run): R-1's G4 constraint gates still green (palette
  parse, boot-flip one-liner, single rim address).
- **G10** `file(1)` text for every touched file.
- **G11** cross-package suite green in ONE serial run at the then-current
  HEAD baseline + the new gates (v1.1, A2: never a hardcoded count — the
  baseline moves with every landed wave), per the test-policy ruling
  (ns-level during phases, one suite at integration).
- **W-1** (wiring check, NOT a JVM gate — named honestly): in trail faces the
  layout consumes the derived world's sidebar flag (R-1 debt 1); evidence =
  code cite + `/trail timeline` screenshot with the sidebar atom forced true
  showing sb-w 0. Collected at first light, recorded in the phase artifact.

## §5 File allowlist

AMEND only: `src/app/client/workspace/trail_face/{cards,scene,lanes}.cljc`
(+ ONE new pure cljc under `trail_face/` for components/moves AND the band
packer — v1.1/A4: rect_tree's layout engine has no wrap-flow and stays
untouched, so band packing is new pure code here); the ACTUAL view-state
owners, corrected v1.1 per S1 (the validator located them): `state.cljs`
(`!trail-face-state` + the NEW prev-assignment cache atom),
`agent_flow.cljs` (`/trail` command case block — gains the `band` op),
`wiring.cljs` / `mouse.cljs` / `editor_compute.cljs` (watch/read wiring
lines only); the layout consumer for W-1 (`combined_text.cljs` /
`editor_compute.cljs` — mode-branch lines only). Tests + fixtures in the
existing `trail_face_test.clj` + `test/resources/trail_face/` (extended per
G3's fixture note). `workspace_actions.cljs` is NOT a view-state owner (it
only reads `:face`) — off the allowlist.
UNTOUCHED: `rect_tree.cljc` (the intersection fix is platform now — any need
to touch it again is a stop-clause), `trail_view.clj`, all server files,
kernel files, `renderer.cljs`, rim code paths. One builder per file
(the 2026-07-05 double-dispatch collision remains the grounds).

## §6 Process (delivery-mode, per D-006 notes 2026-07-05)

Coding batched FIRST (Fable may implement directly — fastest-path rule; or
fresh-context Opus subagents inside the orchestrating session), then ONE
serial test batch, then ONE batched falsification + Fable gate at the end of
the wave — never sprinkled per-phase. The contract-validation round RAN
2026-07-05 (fresh-context Opus, default-fail): **FAIL — 2 blockers, 7
should-fixes, 4 advisories, all folded into this v1.1**
(`CONTRACT_R2_VALIDATION_R1.md`, kept verbatim; the round's kill record —
B1/B2 were real Fable-authored text errors — goes to the D-006 honest
ledger). A SECOND round is Sid's call at countersign. Never overwrite a FAIL
artifact; per-round files.

Falsification note for the end gate (instance-vs-class lesson, wave 1): hunt
by CLASS — "unclipped text ops" and "phantom moves" are classes, not
one-liners.

## §7 Builder verification duties (before code)

1. Re-read `room-card-lane-2026-07-05.md` §§2–3 + handoff items 1/5/6 in
   full; this contract scopes, the doc holds intent.
2. Re-grep every code cite here (line numbers drift): `rt-edges` collection
   in `scene.cljc`; `family-key`/`assign-lanes`/`lane-spines` in
   `lanes.cljc`; `feed-entry-card` + badge/stamp builders in `cards.cljc`;
   `parse-trail-command`.
3. Verify glyph coverage for every NEW glyph (trap 10) against the CURRENT
   merged `font_atlas.json`.
4. Confirm the feed fixture's `:relation-transition` detail shape carries
   `:from`/`:to`/`:kind` for lineage kinds (it does today — G3's fixture
   must extend it, not invent shapes).
5. View-state locations are KNOWN (v1.1, S1 — validator-located, re-grep to
   confirm drift): `!trail-face-state` in `state.cljs:~198`; `/trail`
   command mutation in `agent_flow.cljs:~311-318`; expansion toggle in
   `mouse.cljs:~404`; pull loop `wiring.cljs:~39-60`; scene watch
   `editor_compute.cljs:~388`. The prev-assignment cache atom is NEW in
   `state.cljs`, written post-build (trap 13).
6. Check `:display-name` availability on feed entries for G6's sayable
   reason (git-spine P3 landed it); fall back to the raw id honestly when
   absent.

## §8 Stop clause

Standard: unbuildable / conflicts with a binding doc / genuine policy fork →
stop, classify (implementer-fixable vs fork), escalate forks to decisions.md
Open Questions as PROPOSED with verbatim citations; never improvise policy.
Pre-flagged: rect_tree needs changing → STOP (platform seam, §5); any
temptation toward an incremental/diff consumer → STOP (trap 11); band
geometry demands a second y-axis or non-thin-rect primitives → STOP (INV-17).
Implementer-fixable examples: glyph substitutions (record them); view-state
location differing from expectation (locate + name it).

## §9 Input manifest (D-006 counterfactual probe feed)

`design/claude/room-card-lane-2026-07-05.md` (all; esp. R4–R7 + handoff);
`build/trail-room/CONTRACT.md` + `PHASE_R1.md` + `RETRO.md`;
`build/git-spine/GATE_REVIEW.md` (clip idiom, kraft truncation ruling, open
doubts); `build/render-north/PROBE-10K.md:9-15,105-155`;
`src/app/client/workspace/trail_face/{lanes,cards,scene}.cljc` @ dc743d6;
`test/app/client/workspace/trail_face_test.clj` + `test/resources/trail_face/
feed.edn` @ dc743d6; `docs/current-mental-model/decisions.md` (D-001, D-005,
D-006 notes incl. delivery ruling); FIRST_LIGHT.md F-L2/F-L5.

## §10 Handoff

Implementer: per §6 (Fable-direct or fresh-context subagents — Sid's call at
wave open). Reviewer gate: batched falsification + Fable gate per §6, receipts
in `GATE_REVIEW_R2.md`. After green: first light over the REAL corpus (Sid) —
the band count line over tonight's ingested material is the D-001 gauge this
tranche exists to expose; H1's verdict-log window is already armed by then
(or arms at the same sitting). Commits per standing hard rules (code/docs
separate; Sid's word).
