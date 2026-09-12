# GATE REVIEW — trail-room R-2 (bands · lanes-from-edges · move chips)

**Stamp: Trunk-5 · 2026-07-06 · Fable gate. Code read IN FULL: threads.cljc
(new ns, whole file), cards.cljc + scene.cljc diffs hunk by hunk, all four
wiring diffs, gate tests skimmed against their claims.**
**Verdict: PASS — with three fixes applied AT gate (S1 incident-gated
moves, S2 total tiebreak, S1-cross fixture honesty) plus the server-side
endpoint projection; touched nss re-run green (39/973/0).**

Inputs: `BRANCH_REPORT_R2.md` · CONTRACT_R2.md v1.1 (countersigned-binding)
· serial suite (Trunk-5: **204 tests / 2500 assertions / 0 fail / 0 err**) ·
`DIFF_FALSIFICATION_R2.md` + `DIFF_FALSIFICATION_CROSS.md` (fresh-context
subagents).

## Architecture

The R-2 shape is clean and matches the contract's decomposition:

- **threads.cljc (NEW, pure)** owns fold-first → union-find components over
  fold-lifted lineage edges → assignment/rank/index as KEYED MAPS → move
  detection → band wrap-packer. Every output derives from entry ATTRIBUTES
  (arrival-ms, ids), never sequence position — the PROBE-10K obligation
  (order rides rows as data) is structurally honored, not just tested.
- **cards.cljc** band-parameterizes the card builder: bands 0–2 emit LINE
  ops only, no `:style :bg` on the card node (the 167-boxes class is dead
  at op level, and G1 asserts it as `tree->rects = []`); bounds derive FROM
  the ops (hit region == painted region, per band); chips compose on the
  first row with running `first-row-end` so fold + move chips never
  overlap.
- **scene.cljc** rewires the timeline: canonical total sort → assign →
  moves-vs-carry → per-entry flow with overflow chips, carrier-only fold
  paint, and the band as a bottom region packed by the pure packer. The
  carry rides ROOT DATA out of the build; the caller caches it.
- **Wiring**: band rides `!trail-face-state` (existing watched value, no
  new watch); the carry lives in a NEW unwatched let-bound cache atom
  beside the `!last-trail-struct` precedent; W-1 closes by deriving
  sidebar visibility from the derived world in BOTH `<layout>` (paint) and
  `handle-mousedown!` (hit-test) — click space equals render space, raw
  atom untouched so leaving the face restores the sidebar.

## Writers / readers / clearers — `!trail-prev-carry` (the one new state)

- **Writer**: the `<trail-face` `m/latest` fn, post-build (from root data)
  — one writer.
- **Reader**: the same fn at build time (`:prev @!trail-prev-carry`) —
  deref, never watch.
- **Clearer**: the nil-face branch (`/trail off` → `!tfs` nil → face nil →
  carry reset nil). Re-entry starts with arrivals, no phantom moves.
- **Stuck-state check**: on the struct-skip path (identical struct → cached
  scene returned) the carry is NOT rewritten — correct, same feed means
  same carry. A feed change always passes the struct gate (feed is in the
  struct), so the carry can never mask a newer feed. HELD.
- **Trap 13 (watched-input feedback)**: the atom is watched by NOTHING
  (grep: one reset-nil, one reset-carry, one deref, all inside the same
  serialized flow). No feedback edge. HELD.
- **Pattern tension, named honestly**: writing cache atoms inside
  `m/latest` technically brushes the R3 "no side effects in m/latest"
  law. This diff did not introduce the pattern — `!trail-face-scene` and
  `!last-trail-struct` live in the SAME fn pre-R-2 — and the escape class
  (unwatched write-only render caches) is exactly why trap 13's
  "watched-by-nothing" condition exists. Recorded so the Gap-3 rework
  sweeps all three atoms together, not as a new defect.

## Failure modes attempted (Fable pass)

- **Phantom move on band flip**: `/trail band` swaps `!tfs` → rebuild with
  the SAME feed → `(= (:feed prev) carry-feed)` → moves carried, not
  recomputed. No phantom. CANNOT HAPPEN via band/order/expand changes.
- **Move reason citing a foreign thread**: `incident` edges all have both
  endpoints inside tid's component by union-find construction — any
  incident lineage edge IS in the destination thread. CANNOT HAPPEN.
- **Chip overlap**: fold chip appended first, move chip's `first-row-end`
  then includes it. CANNOT HAPPEN.
- **Root-identity banding** (the build-time bug class): `components` maps
  EVERY node through `find-root`, including roots (the in-batch bug 1 fix,
  re-verified in source).
- **`comp` shadowing** (in-batch bug 2): renamed `comps`, re-verified.
- **Band cards leaking into spines**: `spine-cards` iterates `nodes` only;
  band cards ride `band-nodes`. CANNOT HAPPEN.
- **Overflowed thread double-chip**: `seen-overflow` set guards. CANNOT
  HAPPEN.
- **`/trail band 1` with no face active**: `swap! assoc` on nil `!tfs`
  yields `{:band 1}`, face nil → scene nil (harmless); the next `/trail
  timeline` RESETS state (band back to default 2). No stale-band leak.

## Async ordering risks

The build runs inside one `m/latest` recomputation — serialized by
construction; carry read-before/write-after within one invocation. The
one-frame layout/chrome divergence on face flip (report doubt 5) is a
DISCLOSED pre-existing class (two separate `m/latest` chains off
`!effective-local-world`), benign, cured only by the Gap-3 single-latest
derivation — out of scope, carried.

## Error-path cleanup

A throw inside the scene build crashes the `m/latest` chain (pre-existing
class, unchanged by R-2); no locks/pending sets to leak. The carry atom
would hold the LAST GOOD carry across such a crash — acceptable: the next
successful build recomputes moves against it.

## Fable doubts added at gate (beyond the report's six)

1. **Canonical sort tie**: two distinct rt-entries on the same target in
   the same arrival-ms share the full sort key AND the entry-key —
   stable-sort hides it, but a permuted pull could flip their paint order.
   Their entry-keys colliding means the maps already treat them as one
   entity; if two same-ms transitions on one relation ever matter, the
   sort key needs the relation-id. DOUBT, low.
2. **Compact stamp year-blindness**: `ms->compact-date-str` references the
   entry's own arrival year, so a wholly-last-year card prints no year at
   band 2 (cross-CLOCK divergence shows; cross-year-vs-TODAY doesn't).
   Design-track question, not a lie (the full stamp lives on the open
   surface).
3. **`display-names` last-writer-wins** is entry-order-dependent in
   isolation; at the call site the canonicalized carry-feed kills the
   order-dependence for moves, and live display-names derive
   deterministically per id. HELD at consumers; noted for anyone reusing
   the fn on raw feeds.
4. **Fold count includes marks**: `:count` = family size minus one, and rt
   entries share the family key — so "+N" counts marks, not just folded
   terrain. G5's fixture pins this semantic deliberately (doc + 2 marks →
   count 2). Named so nobody reads "+N" as "N hidden documents".

## The report's 6 doubts — gate disposition

1. **Live feed carries no lineage endpoints** — REAL, verified at
   trail_view.clj:568 vs RelationActivityRow (relation_kernel.clj:271).
   This is the trunk-ordered ENDPOINT ADDITIVE FIX, applied at this gate
   (below). Closes the doubt.
2. Band wording over-claims ("no asserted relations yet" over stance-
   related material) — contract-verbatim string; candidate amendment
   ("no asserted lineage yet") surfaced for Sid at the sitting. Not
   improvised at gate.
3. Thread rank counts marks as first appearance — attribute-derived
   reading kept; one-line falsifier stands if lane order feels wrong at
   first light.
4. Move chips persist across identical pulls — intended ("since last
   pull"); clear condition named.
5. One-frame chrome divergence — pre-existing class, carried (above).
6. Open cards absorbed into overflow chips — edge case, R-3 interaction
   question.

## Falsification results (fresh-context subagents) — FOLDED

**R2 falsifier (`DIFF_FALSIFICATION_R2.md`): Classes 1/3/4/6 HELD**
(carry lifecycle, clip containment, bounds==paint, trap-13 structural
impossibility all survived attempted breaks). **Classes 2 and 5 BROKEN**
with concrete probe inputs — both dark until the endpoint fix, both LIVE
the moment it lands, both FIXED at gate:

- **S1 — phantom move chips on thread merge (Class 2, BROKEN → FIXED).**
  Union-by-min re-roots the absorbed side when the newcomer's root id is
  smaller; every re-rooted member's assignment VALUE changes, so the old
  rule ("present in both maps with changed value") fired "joined thread"
  on bystanders, citing pre-existing intra-thread edges (probe: `m2 →
  "based-on m1.md"`). Violates G6's "non-movers carry none" — and the
  validator-trace intent stated in threads.cljc itself ("chips fire only
  for the changed side"). **Fix:** a chip now requires the target's fold
  head to be an endpoint of a CHANGED edge (symmetric difference of the
  lifted edge sets); the carry gains `:edges`; reasons prefer the ADDED
  incident edge (so they cite the causing edge, not an old one). New G6
  regression block: a 2×2-thread merge fires EXACTLY one chip, on the
  changed incident endpoint, with the added edge in the reason.
  **Honest-ledger:** this adds a conjunct to the contract's §2.4 letter
  ("only targets present in both maps with changed values") in service of
  its own stated intent — flag at countersign.
- **S2 — same-ms transition tie order-dependent (Class 5, BROKEN →
  FIXED).** Strict `>` on arrival left equal-arrival transitions on one
  relation-id to input order, falsifying §2.5's canonicalization
  guarantee. **Fix:** total tiebreak `[arrival (str status) (str kind)]`
  — deterministic, and a same-ms assert+retract pair resolves RETRACTED
  (when in doubt, don't thread).

R2 falsifier doubts carried: D1 (a move chip placed past a long title is
clipped invisible — data survives, rim delta still announces; chip
placement = R-3 design question), D2 (fold "+N" counts marks — same as
Fable doubt 4 above), D3 (the widened `<layout>` diamond — same class as
report doubt 5, Gap-3 cure), D4 (`[target-id arrival-ms]` uniqueness
unenforced — same as Fable doubt 1).

**CROSS falsifier (`DIFF_FALSIFICATION_CROSS.md`): B1 + S1 + S2 + D1/D2.**
- **B1 endpoint gap → FIXED server-side** (see below).
- **S1-cross — fixture-invented asserters → FIXED at gate on the fixture
  side:** live source/transcript rows carry `:asserted-by nil`
  (trail_view hard-codes it); the fixture's `"import:md"` /
  `"import:git-spine"` / `"import:transcript"` on its 8 source/transcript
  entries were INVENTIONS (violating the fixture's own mirror-never-invent
  law) — all 8 nil'd; the 2 relation-transition asserters kept (those ARE
  live-real: git-spine edges assert as `import:git-spine`). G2's first
  assertion updated to the live-true form (reading line = stamp alone on
  source rows); asserter-presence rendering stays covered by G2's
  constructed entries + the rt fixtures. **The open question — should the
  live builders stamp importer provenance instead? — is a SITTING ruling,
  not a gate improvisation.**
- **S2-cross — transcript-file entry mis-mirrored in four keys**
  (pre-existing, byte-frozen original entry, fold-key consumer): NOT fixed
  at gate — it ripples through frozen fixture entries and several gate
  expectations for a cosmetic-fidelity gain on the lowest-traffic entry
  kind. NAMED FOLLOW-UP (R-3 / fixture hygiene): align entry 2 to
  `file-activity-entry`'s real shape, or record why the conversation form
  is deliberately modeled. The durable cure for the whole CLASS is
  mechanical: a fidelity gate that diffs fixture keys against live
  builder output keys (proposed in the D-006 wave note).
- D1-cross (nil-arrival NPE in the stamp fns — unreachable through the
  feed window filter; latent for unwindowed callers) and D2-cross
  (fixture under-specs `:derived-unit-count`) carried.

## Endpoint additive fix (applied AT gate — the trunk-ordered fix)

`trail_view.clj relation-activity-entry` now projects
`:from {:id (:from-id row) :kind (:from-kind row)}` and
`:to {:id (:to-id row) :kind (:to-kind row)}` verbatim from the activity
row (which has carried all four fields since R3 landed —
relation_kernel.clj:271). A `:dead-end` row projects
`:to {:id nil :kind :none}` honestly — the new trail_view_test assertion
caught exactly that case on its first run (initially written too strict;
the row's truth won). With this, `threads/rt-lineage-row` sees real
endpoints over the live corpus: lanes, spines, move chips, overflow, and
`lanes/dead-end-ids` all go LIVE at first light instead of rendering an
honest-but-empty all-band face.

## Re-run receipts (post-fix)

- trail-face + trail-view + git-spine, one JVM: **39 tests / 973
  assertions / 0 failures / 0 errors.**
- Full serial suite re-running at close for the commit receipt (baton
  carries the number).
- Shadow note: no dev server was running at gate; the standalone compile
  is barred by the dev-workflow rule. My client edits are portable-subset
  cljc (no interop, no JVM-only forms), JVM-exercised by the gates above;
  the branch's `:dev 0 warnings` receipt covers the R-2 bulk; Sid's boot
  verifies the bundle at first light.

## Gate ruling

**trail-room R-2 PASSES.** The build honored the contract's structural
laws (pure, address-keyed, order-as-data, fold-first) and its three
in-batch gate catches were real; the two falsification breaks were latent
logic errors in exactly the class the wave pre-registered (order/carry),
fixed and regression-pinned at gate. Commit scope: threads.cljc (NEW) ·
cards.cljc · scene.cljc · editor_compute.cljs · combined_text.cljs ·
agent_flow.cljs · mouse.cljs · trail_face_test.clj · feed.edn (one code
commit, room package). Countersign flags: the §2.4 move-rule conjunct
(above) + the band count line wording (report doubt 2, "no asserted
lineage yet" candidate). First-light expectation: REAL threads over the
live corpus now — if the face still renders all-band, the fault is
upstream of the feed, not in this package.
