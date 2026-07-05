# CONTRACT — trail-room R-2 (bands · lanes-from-edges · move chips) · v1 — PROPOSED

**Status: PROPOSED (Fable-authored 2026-07-05, delivery session, Sid AFK).
Binding only after Sid's countersign. The build does NOT start without it.**

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
  band 0 = glyph + staleness dot (folds render as chips with counts); band 1
  = ONE line `glyph name`; band 2 = title + ONE second line (compressed
  two-clock stamp + asserter; written-by only when it differs). Band 3 = the
  surface (the current open card/expansion, plus printed element address —
  already R-1). Hit target stays the full line region: **rt-tree node bounds
  unchanged, paint only** (R4).
- **Item 5 — lanes from edges, band for the rest (R7).** Thread assignment
  consumes lineage-kind edges (`based-on produced built-over new-direction`)
  as connected components over the feed's `:relation-transition` details.
  Cross-link kinds (`references elaborates`) NEVER merge lanes — they render
  as kraft connectors BETWEEN lanes. `family-key` demotes from lane-maker to
  FOLD rule (doc + its `du:` blocks fold vertically into one card). Entries
  with no lineage edge go to **the band**: one designated region at the
  bottom, self-declaring (`N unthreaded · no asserted relations yet`),
  wrap-packing INSIDE the band only, reading order = time order. Never
  auto-filed into fake lanes, never hidden.
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

**2.3 Thread identity is the lexicographically-smallest member target-id
(thread-scoped, deterministic, stable under growth).** When two components
MERGE via a new edge, the union keeps the smaller id — one side's entries
"moved" (true: they joined a thread) and fire chips; the other side's do not.
Grounds: ids derived from member SETS change on every growth → chips fire
for everything (trap 8). Alternative NOT taken: earliest-member-by-time id —
also stable, but time ties on same-ms imports make it nondeterministic
without a tiebreak that ends up being… the lexicographic id.

**2.4 Move detection state lives client-side, keyed by target-id, carried in
the trail view-state (previous pull's `{target-id → thread-id-or-band}`
map).** Computed pure (`moves prev-assign new-assign → {target-id reason}`),
stamped at scene build. Grounds: "since last pull" is a CLIENT notion (the
rim's delta slot already reads the arrival clock); no server state exists for
it and none is warranted pre-attestation-walk. Alternative NOT taken:
persisting move history — that is walk/attestation territory (D-008 §5).

**2.5 Ordering is DATA, never sequence order (probe obligation,
PROBE-10K:9-15).** Thread index, band membership, and in-thread position are
computed as per-entry ATTRIBUTES over the feed in its ARRIVAL order; no API
in this package may return a re-sorted entry sequence as its contract
surface (maps keyed by entry-key/target-id only). Grounds: order riding the
incseq is the measured 10³ knee (`:permutation` diffs); when the differential
feed arrives, thread moves must be change-shaped (attribute updates), which
this shape gives for free. This is the pre-recorded obligation from the
baton, now binding.

## §3 Traps ledger (naive → concrete failure → ruling)

1. **family-key stays a lane-maker** → when edges land, a doc sits in its
   family lane AND its edge thread — two homes, double spines. → family-key
   is ONLY the fold rule (§1 item 5); lanes come ONLY from lineage edges.
2. **Cross-link kinds in the component relation** → `references` chains merge
   unrelated threads into mush; the wall's lineage arrows lose meaning. →
   components over `#{:based-on :produced :built-over :new-direction}` ONLY
   (pin the set as a named def; cite this trap at its def site).
3. **Order rides the sequence** → a future differential feed ships
   `:permutation` diffs on every thread move → producer knees at 10³
   (PROBE-10K §3). → §2.5; assignment APIs return keyed maps; the feed seq is
   never re-sorted as an API surface.
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
7. **Changing hit bounds while changing paint** → clicks break at band
   boundaries (R-1 trap 6, still live). → rt-node bounds unchanged at every
   band; only ops differ.
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

## §4 Gates (executable; additions to `trail_face_test.clj` + fixtures)

- **G1** (item 1 check, R4): at band 1 a feed entry renders EXACTLY one text
  op and ZERO rect-fill ops; band 0 → glyph(+dot) only, no name text; band 2
  → exactly two line ops; band 3 (open) → the surface node with bg. The
  node's bounds are IDENTICAL across bands 0–3 (trap 7).
- **G2** (R5 data rule): the band-2 second line is built from entry DATA
  (two-clock stamp + asserter; written-by only on divergence) — assert it
  never contains material/body text (tripwire 4: not a truncation of
  content).
- **G3** (item 5 check): fixture feed with a 3-commit `:based-on` chain + 2
  unedged docs → the chain occupies ONE lane; both unedged docs sit in the
  band region (below all threaded content) with the self-declaring count
  line; NO unthreaded doc occupies its own lane; band reading order = arrival
  order.
- **G4** (cross-links): a `:references` edge between members of two threads
  does NOT merge them (assignments unchanged); it renders as a kraft
  connector between the lanes (R6 material, existing kraft constructors).
- **G5** (fold rule): a doc + its `du:` blocks render as ONE card at bands
  0–2 (family fold); at band 0 the fold renders as a chip with a count.
- **G6** (item 6 check, pure): `moves(prev, new)` over a fixture where one
  doc gains a `:produced` edge → exactly ONE move record, reason names the
  kind + far-end display-name; the moved card's ops carry the new-since chip;
  every other card carries none. Determinism: same inputs → identical output.
- **G7** (§2.5 probe obligation): assignment/move APIs return maps keyed by
  entry-key/target-id; the scene builder consumes `:feed/entries` in the
  order given (assert the builder output for a permuted-input copy of the
  fixture differs ONLY in y-stacking derived from the order — i.e. no API
  re-sorts as a side effect); thread/band membership arrives as node DATA
  (`:trail-face/thread`, `:trail-face/band?`) on every entry node.
- **G8** (determinism, standing): same fixture → identical scene twice.
- **G9** (constraints re-run): R-1's G4 constraint gates still green (palette
  parse, boot-flip one-liner, single rim address).
- **G10** `file(1)` text for every touched file.
- **G11** cross-package suite green in ONE serial run (191/2202/0 baseline +
  new), per the test-policy ruling (ns-level during phases, one suite at
  integration).
- **W-1** (wiring check, NOT a JVM gate — named honestly): in trail faces the
  layout consumes the derived world's sidebar flag (R-1 debt 1); evidence =
  code cite + `/trail timeline` screenshot with the sidebar atom forced true
  showing sb-w 0. Collected at first light, recorded in the phase artifact.

## §5 File allowlist

AMEND only: `src/app/client/workspace/trail_face/{cards,scene,lanes}.cljc`
(+ optionally ONE new pure cljc under `trail_face/` for components/moves —
builder names it in the phase artifact); the trail view-state owner for
`:band` + prev-assignment (builder locates — expected
`workspace_actions.cljs` / `editor_compute.cljs` wiring lines ONLY); the
layout consumer for W-1 (`combined_text.cljs` / `editor_compute.cljs` —
mode-branch lines only). Tests + fixtures in the existing
`trail_face_test.clj` + `test/resources/trail_face/`.
UNTOUCHED: `rect_tree.cljc` (the intersection fix is platform now — any need
to touch it again is a stop-clause), `trail_view.clj`, all server files,
kernel files, `renderer.cljs`, rim code paths. One builder per file
(the 2026-07-05 double-dispatch collision remains the grounds).

## §6 Process (delivery-mode, per D-006 notes 2026-07-05)

Coding batched FIRST (Fable may implement directly — fastest-path rule; or
fresh-context Opus subagents inside the orchestrating session), then ONE
serial test batch, then ONE batched falsification + Fable gate at the end of
the wave — never sprinkled per-phase. ONE fresh-context contract-validation
round (default-fail, artifact `CONTRACT_R2_VALIDATION_R1.md`) runs BEFORE
build unless Sid waives it — grounds: contract validation caught two
Fable-authored errors at text time in the git-spine cycle (R1/R2); the
delivery ruling consolidated per-phase falsification, not text-time
validation, and no time-box blanket carries into this session. Never
overwrite a FAIL artifact; per-round files.

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
5. Confirm where trail view-state lives and how `/trail` commands mutate it
   (R-1's G4 cites the exact forms); name the file before editing (§5).
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
