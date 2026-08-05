# BRANCH_REPORT_R2 — trail-room R-2 (bands · lanes-from-edges · move chips)

**Stamp: Trunk-4 / t4-room · 2026-07-06 · builder: Fable-direct (CONTRACT_R2
§6 fastest-path). NO commits (trunk commits). Gates written IN THE SAME BATCH
as the code, as transcription.**

Contract: `CONTRACT_R2.md` v1.1 COUNTERSIGNED-BINDING (round-2 waived).
Builder verification duties §7: ALL executed before code (results in §Duties
below). rect_tree: **UNTOUCHED** (verified by `git status` — not in the diff).

---

## What built

- **Item 1 — band-aware card builders** (`cards.cljc`): `feed-entry-card` is
  band-parameterized (0/1/2 via `geom :band`, default 2). Bands 0–2 emit LINE
  ops only — the card node carries **no `:style :bg`**, so `tree->rects` over
  a closed card is `[]` at every band (167-boxes class dead at op level).
  Band 0 = glyph (+ fold chip `+N`), no name, no staleness dot (S5 deferred
  with the attestation walk); band 1 = ONE op `glyph name`; band 2 = title +
  reading line (`compressed-two-clock-stamp` + asserter; written-by only on
  divergence). Bounds rule v1.1/B1 per-band: node bounds = that band's
  painted line region (widest painted line clamped to card-w, a 2-advance
  floor keeps a lone glyph clickable); band 0 provably denser than band 2.
  Band 3 = the open surface (`expansion-node`, has bg) as a SEPARATE sibling;
  the title node keeps the closed-card click target.
- **Item 5 — lanes from edges** (NEW `threads.cljc` + `scene.cljc` rewrite):
  fold FIRST (`fold-index`: family = the demoted `entry-thread-key` rule;
  head = smallest non-du member id; carrier = newest-arrival terrain entry),
  then connected components (union-by-min union-find) over **fold-lifted**
  lineage edges (`lineage-kinds` = `#{:based-on :produced :built-over
  :new-direction}`, pinned def with trap-2 citation; latest transition per
  relation-id wins, retracted final state drops the edge). Thread identity =
  the component root = smallest member id (§2.3). Cross-links never enter
  components; they render as kraft connectors between lanes (G4). Unthreaded
  material renders as THE BAND: bottom region, self-declaring count line
  (`N unthreaded · no asserted relations yet`, contract-verbatim), pure
  wrap-packer (`pack-band`) INSIDE the band only. Thread overflow (S4):
  ranks ≥ max-lanes fold to ONE `:thread-overflow` chip with a count; no
  mod-wrap across threaded lanes anywhere (scene no longer calls
  `assign-lanes`; the fns remain in `lanes.cljc` as the fold-rule/test
  surface). Lane spines group by THREAD id.
- **Item 6 — move chips**: pure `threads/moves` (prev-assign × new-assign ×
  entries → target-id-keyed map). Only targets present in BOTH maps with
  changed values move (arrival ≠ move); reason = `joined thread · <kind>
  <far-end display-name-or-raw-id>` (S6 honest fallback). The moved card's
  ops carry a `★` new-since chip; full record rides node data
  (`:trail-face/move`). State per §2.4: carry (`{:assignment :moves :feed}`)
  read pre-build / written post-build into an UNWATCHED cache atom
  (`!trail-prev-carry`, `editor_compute.cljs`); the carry's feed is
  CANONICALIZED (arrival-sorted) so a permuted pull is the same carry; carry
  cleared on `/trail off` (no phantom moves on re-entry); chips persist
  across unrelated rebuilds within one pull, recompute on feed change.
- **§2.2 — `/trail band <0|1|2>`**: `parse-trail-command` gains the `band`
  word (out-of-range → nil, never a silent clamp); `agent_flow.cljs` `:band`
  op (`swap! assoc :band` — the `:order` precedent); `editor_compute.cljs`
  passes `:band` in view-state (default 2).
- **R-1 debt 1 / W-1**: `combined_text.cljs` `<layout>` now derives
  `sb-vis?` from the derived world (trail face → sidebar hidden → sb-w 0),
  matching `editor_compute.cljs`'s already-landed guard; `mouse.cljs`
  `handle-mousedown!` gates `sb-vis?` the same way so click space matches
  render space (raw atom untouched — leaving the face restores the sidebar).
- **R-1 debt 2**: chip/label truncation follows the ruling — paint truncates
  under the card's `clip?`; full reason/ids ride node data.
- **G7b data**: every entry node — cards, kraft marks (connector + label
  nodes), handles, overflow chips — carries `:trail-face/thread` +
  `:trail-face/band?`; threaded cards add `:trail-face/thread-rank` +
  `:trail-face/thread-index` (rank/index as DATA — Δ9: the later Rama move
  is a lift). Root data adds `:band`, `:band-count`, `:trail-face/carry`.
- **Fixture extension** (G3 note, duty §7.4 — extend, never invent; every
  shape mirrors the live constructors read from `trail_view.clj`): 3-commit
  `:based-on` chain (source-ingested commit artifacts, `git-commit:<sha>`
  source-refs, sha7 display-names, one back-dated commit for the gate-8
  order flip) + 2 chain marks + 1 `:references` cross-link (G4) + fold
  family `folddoc` (doc + 2 du:-block confirms marks, G5) + 2 unedged docs
  (G3 band). Original 5 entries byte-identical; new entries appended so
  existing index-based test references hold.

## Per-gate status (all executable, `trail_face_test.clj`; ns run green)

| Gate | Test | Verdict |
|---|---|---|
| G1 | `r2-g1-band-card-builders-test` | **PASS** — band 1 = exactly 1 text op + zero rect-fills; band 0 = glyph only, no name, no staleness dot; band 2 = exactly 2 ops; band 3 surface = bg-bearing SEPARATE sibling, title keeps the click; per-band bounds == painted line region; band-0 h < band-2 h; never cross-band identity |
| G2 | `r2-g2-reading-line-test` | **PASS** — reading line == compressed stamp + asserter composed from entry DATA; nil claimed renders `claimed unknown` (t4-spine field consumed as data); written-by only on divergence; never contains material body text |
| G3 | `r2-g3-lanes-from-edges-test` | **PASS** — chain occupies ONE lane under one thread id; unedged docs in the band below ALL threaded content; no unthreaded doc in a lane; self-declaring `4 unthreaded` line; band reading order = arrival order; overflow half: max-lanes 1 → ONE fold-chip (count 6), zero mod-wrapped cards |
| G4 | `r2-g4-cross-links-test` | **PASS** — `:references` between two threads: assignments unchanged; renders as a labeled kraft connector (existing constructors) |
| G5 | `r2-g5-fold-test` | **PASS** — doc + du: blocks = ONE card at bands 0/1/2 (carrier = the doc's terrain entry, fold-count 2); band 0 renders the chip with the count |
| G6 | `r2-g6-move-chips-test` | **PASS** — one `:produced` edge → exactly ONE move record; reason `joined thread · produced aaa1111` (display-name); raw-id fallback verified; moved card carries the chip, every other card carries none; deterministic; chips persist across same-feed rebuilds |
| G7 | `r2-g7-order-as-data-test` | **PASS** — (a) two permutations → value- AND pr-str-identical scenes; (b) thread/band data on every entry node incl. kraft nodes, rank/index as data; (c) assignment/moves/rank/index are keyed MAPS |
| G8 | `r2-g8-determinism-test` (+ standing determinism in gate 7) | **PASS** — identical scene twice at bands 0/1/2 |
| G9 | existing `g4-constraints-test` + `r2-g9-band-command-test` | **PASS** — palette parse (all R-1 forms + the new band form), boot-flip one-liner + sidebar guard source-asserted, single rim address |
| G10 | `file(1)` | **PASS** — all 9 touched files report Clojure/Unicode text (output below) |
| G11 | cross-package one-serial-run | **TRUNK'S at wave boundary** (per the throughput ruling + this branch's brief). Builder scope: full `trail-face-test` ns = **28 tests / 680 assertions / 0 fail / 0 error** (includes all view-mvp gates 1–13, R-1 G1–G4, gate-16 fixture-fidelity against the LIVE runtime — run on the shared tree AFTER t4-spine's server changes landed, so the hand fixture paired green against the spine's current shapes) + shadow `:dev` compile: "Build completed. (249 files, 13 compiled, 0 warnings)" |
| W-1 | wiring check (NOT a JVM gate) | **CODE HALF DONE** — cites: `combined_text.cljs` `<layout>` (derived-world guard, ~:31–48), `editor_compute.cljs` `<layout>` (:291–297, pre-existing), `mouse.cljs` `handle-mousedown!` (~:417–428). The `/trail timeline` screenshot with the sidebar atom forced true (sb-w 0) is **pending first light** (Sid's sitting) — record it there |

Legitimate updates to pre-existing tests (R-1 precedent — gates updated where
behavior legitimately changed, 2 sites):
1. `two-clocks-test` `:order` flip: the old pair (threaded April doc vs chat
   row) now spans two REGIONS (chat is band material, always below threaded
   content), so the claimed-vs-arrival flip is asserted over two commit-chain
   cards of the same thread (c3 back-dated); the arrival-side cross-region
   assertion retained. Keys-set equality across orders retained.
2. Thread-B identity expectations in new gates corrected to `"oc:doc:9fdoc"`
   (`"9" < "e"` lexicographically — the §2.3 rule, not my first guess).

## Bugs found AT BUILD by the gates (fixed in-batch)

1. `threads/components` returned roots only for LOSING union sides — a
   component's own root id mapped to `:band` (a thread's identity member
   silently banded). Fixed: every edge endpoint maps through `find-root`.
2. `threads/assign` bound the components map to `comp`, shadowing
   `clojure.core/comp` — `(comp str key)` became a map lookup returning the
   `key` fn, sorting raw group keys (String vs Keyword CCE). Renamed `comps`.
3. Carry embedded the RAW feed (permuted pull → different carry → G7a
   byte-identity broke). Fixed: carry feed canonicalized on the arrival
   attribute.

## Deviations (implementer-fixable class, §8)

1. **Prev-carry atom lives let-bound in `editor_compute.cljs`, not
   `state.cljs`** (contract §5/§7.5 expected state.cljs). Grounds: the build
   flow receives atoms POSITIONALLY from `render.cljs` (off the allowlist);
   threading a state.cljs atom would touch it. §2.4's own named pattern
   (`!last-trail-struct`) IS a let-bound editor_compute atom; the new atom
   sits beside it, watched by nothing, cleared on face-off. Location named
   per §8 ("view-state location differing from expectation").
2. **`state.cljs` and `wiring.cljs` untouched** (allowlisted but no change
   needed — `:band` rides inside the existing `!trail-face-state` value; the
   pull loop is band-agnostic). `lanes.cljc` untouched in content (its fns
   remain the fold rule + tested surface; the scene stopped consuming
   `assign-lanes`).
3. **Glyph substitutions (trap 10, atlas-verified 2026-07-06)**: fold chip =
   ASCII `+N` (U+2295 ⊕ ABSENT from the merged atlas); move chip = `★`
   U+2605 (present); overflow chip = `▸` U+25B8 (present). ⟲/✕/≡ also absent
   — avoided. Everything still passes `sanitize-tree`.
4. **Kraft mark band compression NOT built** (design R6: band 1 = bare tack,
   band 0 = absorbed into fold counts). Contract §1 item 1 scopes CARD
   builders; no gate demands mark paint compression. Marks render their R-1
   form at every band (more honest than vanishing). Queued as an R-3/design
   question.
5. **Band "fog tint" not painted** — the contract's band demands are the
   region + self-declaring line + wrap-packing; a region tint is design
   R7 flavor, left for the design track (no rect-fill invented).

## Doubts, with falsifiers

1. **The LIVE feed carries no lineage endpoints.** `trail_view.clj`
   `relation-activity-entry` emits `:entry/detail {:kind :status
   :relation-id}` — no `:from`/`:to` (t4-spine added claimed-ms only;
   verified in the shared tree's diff). The client component pass therefore
   sees ZERO lineage edges over the real corpus: first light will show an
   ALL-BAND face (honest — `N unthreaded` — but no threads until the
   endpoints ride the detail). The contract's duty §7.4 sentence "it does
   today" is true of the FIXTURE, not the live path. Falsifier: grep
   `relation-activity-entry` in `trail_view.clj`; run `/trail timeline` over
   tonight's corpus and count threads. **Trunk decision needed**: endpoints
   on the rt detail are a server-side additive change (off THIS branch's
   fence) — same seam class as the spine's claimed-ms work.
2. **The band count line's wording can over-claim.** `folddoc` HAS asserted
   (stance) relations — just no lineage — yet sits under "no asserted
   relations yet" (contract-verbatim string, §1 item 5). Candidate honest
   amendment: "no asserted lineage yet". Not improvised — recorded for a
   contract amendment ruling. Falsifier: assert a `confirms` on a band doc
   and read the line.
3. **Thread rank counts mark entries as "first appearance".** Rank = min
   arrival over ALL entries assigned to the thread (a du:-block mark's
   arrival can out-rank a terrain card, as the overflow gate now documents).
   Design R7 says "first appearance of their earliest member" — defensible
   either way; the attribute-derived reading was kept. Falsifier: if lane
   order feels wrong at first light, restrict `firsts` to terrain carriers
   (one-line change in `threads/assign`).
4. **Move chips clear only via feed change or face-off.** A move chip
   survives until the next pull alters the canonicalized feed. If a pull
   returns IDENTICAL data (no new edges), chips persist — intended ("since
   last pull"), but if the rim delta and chips ever disagree at first light,
   the carry's clear condition is the place to look
   (`editor_compute.cljs` `!trail-prev-carry`).
5. **One-frame layout/chrome divergence on face flip** (pre-recorded R-1
   debt class): `combined_text`'s `<layout>` and `<chrome-text>` both derive
   from `!effective-local-world` through separate `m/latest` chains — on a
   mode flip sb-w and trail-mode? can disagree for one frame (benign,
   editor-strip fallback). Same class as the recorded rim glitch frame; the
   cure is the Gap-3 single-latest derivation, out of scope here.
6. **Open cards inside an overflowed thread are absorbed into the chip**
   (expansion unreachable while the thread is folded). Edge case; reopening
   = raise max-lanes or unfold (R-3 interaction question).

## Exact files touched (9 — all inside CONTRACT_R2 §5)

```
src/app/client/workspace/trail_face/threads.cljc      NEW (the ONE new pure cljc: components/moves/band packer)
src/app/client/workspace/trail_face/cards.cljc        band-aware builders + compressed stamp + chips
src/app/client/workspace/trail_face/scene.cljc        /trail band parse; timeline scene: threads/band/overflow/moves/carry
src/app/client/workspace/editor_compute.cljs          :band in view-state; !trail-prev-carry read/write/clear
src/app/client/workspace/combined_text.cljs           <layout> derived-world sidebar guard (W-1)
src/app/client/workspace/runtime/agent_flow.cljs      :band op in the /trail case block
src/app/client/workspace/runtime/mouse.cljs           sb-vis? trail-face guard (click space == render space)
test/app/client/workspace/trail_face_test.clj         R-2 gates G1-G9 + two legitimate updates
test/resources/trail_face/feed.edn                    fixture extension per G3's note
```

UNTOUCHED per fence: `rect_tree.cljc`, `trail_view.clj`, all server/kernel
files, `renderer.cljs`, rim code paths, `workspace_actions.cljs`,
`text_face.cljc`, `sanitize.cljc`, `lanes.cljc`, `state.cljs`, `wiring.cljs`.
(Server files ARE modified in the shared tree — that is t4-spine's diff, not
this branch's.)

## `file(1)` output (G10)

```
src/app/client/workspace/trail_face/threads.cljc: Clojure module source, Unicode text, UTF-8 text
src/app/client/workspace/trail_face/cards.cljc:   Clojure module source, Unicode text, UTF-8 text
src/app/client/workspace/trail_face/scene.cljc:   Clojure module source, Unicode text, UTF-8 text
src/app/client/workspace/editor_compute.cljs:     Clojure module source, Unicode text, UTF-8 text, with very long lines (316) [pre-existing]
src/app/client/workspace/combined_text.cljs:      Clojure module source, Unicode text, UTF-8 text
src/app/client/workspace/runtime/agent_flow.cljs: Clojure module source, ASCII text
src/app/client/workspace/runtime/mouse.cljs:      Clojure module source, Unicode text, UTF-8 text
test/resources/trail_face/feed.edn:               Unicode text, UTF-8 text
test/app/client/workspace/trail_face_test.clj:    Clojure module source, Unicode text, UTF-8 text
```

## Duties record (§7, executed before code)

1. `room-card-lane-2026-07-05.md` §§2–3 + items 1/5/6 re-read in full.
2. Cites re-grepped at build tree: `rt-edges` scene.cljc:503 (pre-rewrite);
   `family-key`/`assign-lanes`/`lane-spines` lanes.cljc:23/47/138;
   `feed-entry-card` cards.cljc:266 (pre-rewrite); `parse-trail-command`
   scene.cljc:19.
3. Glyph coverage vs the merged `font_atlas.json` (588 glyphs): ★ U+2605,
   × U+00D7, ▸ U+25B8, ✗ U+2717, ↺ U+21BA PRESENT; ⊕ U+2295, ⟲ U+27F2,
   ✕ U+2715, ≡ U+2261 ABSENT. Chips use +/★/▸ only.
4. Fixture rt detail carries `:from`/`:to`/`:kind` — confirmed; extension
   mirrors, never invents (live divergence recorded as doubt 1).
5. View-state locations confirmed: `!trail-face-state` state.cljs:198;
   `/trail` case agent_flow.cljs:310–318; expansion toggle mouse.cljs:393–409;
   pull loop wiring.cljs:24–84; scene watch editor_compute.cljs:348–397.
   Prev-carry deviation recorded above.
6. `:display-name` live: source/transcript targets carry it,
   relation-transition targets carry none — G6's raw-id fallback exercised.

## Stop-clauses

None fired. fold-precedence (S3), thread-identity-member (§2.3), and the
geometry text (S7) all held at implementation contact — the three v1.1
Fable-authored additions survived their first build hour. No rect_tree need,
no incremental-consumer temptation, no second y-axis.
