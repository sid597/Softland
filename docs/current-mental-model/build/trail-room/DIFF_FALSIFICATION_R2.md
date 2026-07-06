# DIFF_FALSIFICATION_R2 — t4-room R-2 diff falsification pass

**Reviewer: Fable falsification agent · 2026-07-06 · default-fail mindset.**
Scope: the t4-room part of the uncommitted working tree vs HEAD `9552aa4`
(threads.cljc NEW; cards/scene .cljc; editor_compute/combined_text/agent_flow/
mouse .cljs; trail_face_test.clj; feed.edn). Method: read + static analysis
first, then pure-JVM cljc probes (no Rama) executed against the actual
structures. Six pre-registered classes hunted by attempting a concrete break
on each; a class is HELD only where a break was attempted and shown impossible.

`git status` confirms rect_tree.cljc, trail_view.clj, all server/kernel files,
lanes.cljc UNTOUCHED (server files ARE modified — that is t4-spine, not this
branch).

---

## Bottom line

The client diff is **internally correct and its 28-ns suite is honestly
green**, BUT two independent facts break the review's default-fail gate:

1. **BLOCKER (trunk-level, not a client defect):** the LIVE feed emits no
   lineage endpoints, so every thread/lane/move-chip feature produces ZERO
   output on the real corpus. The gates pass only because the fixture was
   hand-extended with `:from`/`:to` shapes the live constructor does not emit.
2. **Class 2 (phantom moves) is BROKEN** by a thread-merge topology, and
   **Class 5 (order-as-data) is BROKEN** by a same-ms transition tie — both
   with concrete failing inputs below. Neither is reachable on today's live
   corpus (see #1), so they are latent, not live, defects.

Classes 1, 3, 4, 6 HELD under attempted breaks.

---

## Per-class verdict

### Class 1 — Unclipped text ops — **HELD**
Every text-bearing node R-2 introduces sits under a `:clip? true` ancestor:
`band-note` (scene.cljc, clip? node + text-in-child), `thread-overflow`
(scene.cljc, clip? node + text-in-child), and the fold/move chips
(cards.cljc `band-line-ops`) which ride inside the card's `card-text` child,
under the card node's `:clip? true`. Probe 3 confirmed `band-note`/
`thread-overflow` are `clip?=true`; probe 4 confirmed card text (incl. chips)
truncates/drops at card-w.
Strongest break attempted: a move-chip reason after a **long title** —
result: the chip op is *dropped entirely* by `tree->text-ops` `in-clip?`
(its x exceeds card-w), i.e. containment WORKS (nothing bleeds); the failure
mode is an *invisible* chip, filed as a DOUBT under Class 2, not a bleed.
(Pre-existing `omissions-block` paints text on a node with no clip? ancestor,
but it is full-width and untouched by R-2 — out of scope.)

### Class 2 — Phantom moves — **BROKEN** (concrete input below)
The carry-atom lifecycle itself is clean (probe 5): order flips, band flips,
and permuted re-pulls all persist `{}` — no phantom; face-off clears the
carry; feed canonicalization makes a permuted pull the same carry. The break
is in the thread-identity math, not the carry: **a thread merge where the
newcomer's root id is lexicographically smaller than the absorbing thread's
root re-roots the entire absorbed thread, firing a "joined thread" chip on
every member — including non-incident members whose reason names a
pre-existing intra-thread edge** (`threads/components` union-by-min at
threads.cljc:151 + `threads/moves` at threads.cljc:218-255). Probe 1 output:
`m2 => oc:doc:m1 -> oc:doc:a1 :: joined thread · based-on oc:doc:m1.md` — m2
never moved; m1 (its old threadmate) absorbed a smaller thread. This is
trap-8's symptom ("the whole thread lights up as moved") re-entering through
the merge door; §2.3's "stable under growth" holds only for growth adding
*larger* ids.

### Class 3 — Per-band bounds-vs-paint — **HELD**
Probe 4: card `:bounds :w` == the painted-line region width (clamped to
card-w, 2-advance floor) for cards WITH a move chip (282.56 == 282.56). The
lone-glyph floor makes bounds ≥ paint (over-clickable, sanctioned by contract
B1), never paint > bounds — when text-w > card-w the card's own `:clip?`
truncates the paint back to card-w == bounds. Band-3 is a separate sibling
with the bg; the title node keeps the click target and stays typography (G1,
verified in code + gate). No band/geometry combination found where the
hit-region and the visible paint diverge in the paint > bounds direction.

### Class 4 — Watched-input feedback (trap 13) — **HELD**
`!trail-prev-carry` has exactly four references, ALL in editor_compute.cljs
(def :367, face-off clear :375, read `:prev @…` :403, post-build write :410).
Nothing else in `src/` or `test/` touches it (grep). It is NOT one of the
`<trail-face` m/latest input flows, and it is NOT in the memo key
`struct [layout trail-state trail-text trail-feed trail-bundles coverage]`
(editor_compute.cljs:386). Reading it via `@` inside the fn does not
establish Missionary reactivity, so the write cannot re-fire the build — no
rebuild/re-pull loop. (Caveat, not a defect: the write sits INSIDE `m/latest`,
the CLAUDE.md side-effect-in-latest pattern — but it is the SAME pattern R-1
already uses for `!last-trail-struct`/`!trail-face-scene`, explicitly cited
in §2.4; consistent with accepted practice, and settles in ≤2 rebuilds
because a re-read carry equals the written carry.)

### Class 5 — Order-as-data (G7) — **BROKEN** (edge case, concrete input below)
Probe 2b: two transitions on ONE relation-id at the SAME `arrival-ms`
(asserted + retracted) → `current-lineage-rows` tiebreak is strict-`>`
(threads.cljc:117), so the FIRST row in `entries` order wins → a permutation
flips which survives → the edge is kept in one order (`X` threads with `Y`)
and dropped in the other (`X` → `:band`). This directly falsifies §2.5's
unconditional "a permuted input stream canonicalizes to the IDENTICAL scene."
The "latest transition wins" semantic fundamentally needs a total order on
`(relation-id, time)`; on a time tie it falls back to sequence position — the
exact thing §2.5 forbids. Also (2a): permutation-identity AND node-id
uniqueness both rest on `[target-id, arrival-ms]` uniqueness, which is never
enforced. The fixture avoids both ties, so G7a passes.

### Class 6 — Fold precedence edges — **HELD** (with a DOUBT)
Fold-first-then-component precedence is correct: du: members lift to the head
(`lift-id`), the head's edges thread the folded unit, and a du: mark whose
family head is itself banded stays banded (traced over the fixture's 9fdoc &
folddoc families; assignment probe confirms `du:9fdoc:000003 → oc:doc:9fdoc`,
`folddoc → :band`). Retracted relations drop (probe 2b). DOUBT: `fold-index`
`:count` = `(dec (count es))` (threads.cljc:76) counts relation-transition
MARKS as folded members — folddoc's "+2" counts two `confirms` marks
(du:folddoc:000001/2) that ALSO render independently as kraft lines (probe 6:
family kinds = `(:source-ingested :relation-transition :relation-transition)`).
The "+N folded" chip double-represents assertion activity that is also
visible.

---

## Ranked findings

### BLOCKER

**B1 — Live feed carries no lineage endpoints; the whole R-2 feature set is
dark on the real corpus.**
`src/app/server/rama/trail_view.clj:558-569` `relation-activity-entry` emits
`:entry/detail {:kind :status :relation-id}` — NO `:from`/`:to`. The client's
`threads/rt-lineage-row` (threads.cljc:94-104) REQUIRES both
(`(get-in d [:from :id])` AND `(get-in d [:to :id])`) or it returns nil. So on
live data `current-lineage-rows` = `[]` → zero components → every entry is
`:band` → no lanes, no spines, no move chips, no overflow. Every gate that
exercises threads (G3/G4/G5/G6/G7b) passes ONLY because feed.edn was
hand-extended with `:from`/`:to` the producer never emits. This is not a
defect *in the client diff* (trail_view.clj is off-fence and this is
pre-recorded as the report's doubt 1), but it is a **first-light / demo
blocker**: the gates prove the code against a shape the live path does not
produce. The trunk must land the server `:from`/`:to` addition on the
relation-activity detail (same seam class as t4-spine's claimed-ms work)
before R-2's headline features are observable. Falsifier run: grep confirmed
both ends.

### SHOULD-FIX

**S1 — Phantom move chips on thread merge (re-rooting).**
`src/app/client/workspace/trail_face/threads.cljc:135-157` (`components`,
union-by-min) + `:218-255` (`moves`). When two ≥2-node threads merge and the
absorbing side's root id is the *larger*, the whole absorbed side re-roots and
every member fires "joined thread · <kind> <far-end>", with non-incident
members naming a pre-existing intra-thread edge (probe 1: m2 → "based-on
m1.md"). Violates item-6 / G6's "non-movers carry none" in spirit and the
"map must not lie" soul. Not reachable on live data today (see B1). Fix
direction: only fire a chip for the target(s) directly incident to a NEW
edge, or hold thread identity stable across a merge (keep the pre-merge
identity of the larger component rather than re-rooting to the newcomer).

**S2 — Same-ms transition tie makes an edge order-dependent.**
`src/app/client/workspace/trail_face/threads.cljc:117` — `current-lineage-rows`
uses strict `>` on arrival, so two transitions on one relation-id at equal
arrival resolve by input order (probe 2b: edge kept vs dropped under
permutation). Falsifies §2.5's unconditional canonicalization claim. Narrow
trigger (same relation-id, same arrival-ms, differing status). Fix: make the
tiebreak total (e.g. on a time tie prefer `:retracted`, or tiebreak by
`(str status)`/`(str kind)`), so "latest wins" is deterministic without
sequence position.

### DOUBT

**D1 — Move chip after a long title is dropped (silent), not truncated.**
`src/app/client/workspace/trail_face/cards.cljc:357-360` — the move chip rides
`:x (+ 10 (first-row-end ops))` on the title row; when the title (glyph +
display-name-or-id) approaches card-w the chip's x exceeds card-w and
`tree->text-ops` `in-clip?` DROPS the whole op (probe 4: `chip visible after
clip?: false`). "Never a silent reshuffle" is nominally violated in that
corner, though the record rides `:trail-face/move` node data (consistent with
the R-1 truncation-vs-naming ruling). Low probability with short display-names
and the live ~672px card-w; reachable for a long-id/no-display-name terrain
card or a narrow viewport.

**D2 — Fold "+N" counts assertion marks, double-representing them.**
`threads.cljc:76` — `:count = (dec (count es))` includes relation-transition
marks (probe 6). The "+2" on folddoc counts two `confirms` marks that also
render as kraft lines. Extends the report's doubt 3.

**D3 — R-2 widened the combined_text sidebar↔chrome diamond (L8).**
`src/app/client/workspace/combined_text.cljs:43` — R-2 ADDED
`(m/watch !effective-local-world)` to `<layout>` (the diff's "4 flows → 5
flows"), so `sb-w` now co-varies with `<chrome-text>`'s `trail-mode?`
(:106) and other flows (:261), each a SEPARATE `m/latest` over the same
source. On a face flip the two can disagree for one frame (CLAUDE.md L8 raw
diamond). Benign (editor-strip fallback), matches report doubt 5, cure is the
out-of-scope Gap-3 single-latest derivation — but this diff is a NEW arm of
the diamond, not a pre-existing one.

**D4 — Permutation-identity & node-id uniqueness rest on unenforced
`[target-id, arrival-ms]` uniqueness.** Probe 2a: two distinct entries sharing
that key collide (same `entry-key` = same node id, and G7a's byte-identity
would break). Pre-existing system invariant, never validated at ingest.

*Nil-safety check (W-1): `local-world-trail-face?` → `local-world-mode` =
`(or (:mode local-world) :editor)` is nil-safe; the new mouse/combined_text
guards degrade to the old behavior on nil/boot. No finding.*

---

## Probes-attempted ledger (verbatim)

All probes: `clojure -M:test <file>`, pure cljc (threads/cards/scene/lanes/
sanitize/rect_tree), no Rama namespace loaded, no full test ns run.

**Probe 0 — load + assign over feed.edn (16 entries):**
```
ASSIGN {oc:doc:loose-2 :band, oc:doc:9fdoc oc:doc:9fdoc, oc:doc:cmt-a2 oc:doc:cmt-a1,
 oc:doc:cmt-a3 oc:doc:cmt-a1, oc:doc:deadend-doc :band, oc:doc:folddoc :band,
 oc:doc:loose-1 :band, du:folddoc:000001 :band, oc:doc:cmt-a1 oc:doc:cmt-a1,
 oc:chat-conversation:chat:ab12 :band, oc:doc:earlier oc:doc:9fdoc,
 du:folddoc:000002 :band, du:9fdoc:000003 oc:doc:9fdoc}
```

**Probe 1 — re-rooting phantom (Class 2, BROKEN):**
```
PREV-ASSIGN {m1 m1, m2 m1, a1 a1, a2 a1}
NEW-ASSIGN  {m1 a1, m2 a1, a1 a1, a2 a1}
MOVES:
  oc:doc:m1 => m1 -> a1 :: joined thread · based-on oc:doc:a1.md
  oc:doc:m2 => m1 -> a1 :: joined thread · based-on oc:doc:m1.md   ← PHANTOM
FIRED FOR: (oc:doc:m1 oc:doc:m2)
```

**Probe 2 — order-as-data ties (Class 5, BROKEN):**
```
2b same-ms relation-id tie (asserted vs retracted):
  order [a r] current rows: [{... :status :asserted ...}]     assign: {oc:doc:x oc:doc:x}
  order [r a] current rows: []                                 assign: {oc:doc:x :band}
2a entry-key collision same [tid,arrival]: ekey c1 == c2 ? true
```

**Probe 3 — full scene build inspection:**
```
band-count: 4 ; folddoc fold-count: 2
band-note clip?= true w= 784 ; band-note-text clip?= false w= 784
thread-overflow clip?= true ; overflow-label text-child clip?= (false)
overflow chips: 1 fold-count: (6) ; deterministic scene? true
```
(text lives in the child; the clip? PARENT clips it — HELD, not a bleed.)

**Probe 4 — chip clipping + bounds/paint (Class 1/3):**
```
card-w bound: 300 ; chip local x: 556.96  >= card-w? true  ; chip visible after clip?: false
normal-card bounds-w: 282.56  painted-w: 282.56  equal? true
pack-band wide cell: {:a {..:row 0} :b {..:row 1} :c {..:row 2}}  (wide cell → own row)
moves nil-prev: {} ; moves empty-prev: {}
```

**Probe 5 — carry lifecycle at scene level (Class 2, carry HELD):**
```
pull1 moves: {} ; order-flip moves: {} ; carry-feed order-independent? true
band-flip moves: {} ; permuted-repull moves: {} ; permuted carry-feed==prior? true
real-move moves: {oc:doc:loose-1 {:from :band :to oc:doc:cmt-a1
                                  :reason joined thread · produced aaa1111}}
```

**Probe 6 — glyphs + fold composition:**
```
★ + ▸ · ↓ ↔ ✎  all covered? true
folddoc family kinds: (:source-ingested :relation-transition :relation-transition)
folddoc fold count: 2 ; carrier: [oc:doc:folddoc 1782168800000]
```

---

## The six report doubts — attempted-falsifier results

1. **Live feed carries no lineage endpoints.** CONFIRMED (→ BLOCKER B1).
   `relation-activity-entry` (trail_view.clj:558-569) emits no `:from`/`:to`;
   client `rt-lineage-row` requires both. Live corpus = all-band, no threads.
2. **Band count line over-claims.** CONFIRMED. Band-count 4 includes folddoc,
   which HAS `confirms` relations, yet the line reads "no asserted relations
   yet". Contract-verbatim string → a wording amendment matter, not a code bug.
3. **Rank counts mark entries as first-appearance.** CONFIRMED as behavior
   (probe: thread B's rank is set by the du:9fdoc mark at 1782166000000,
   predating thread A's first commit → B holds lane 0, the commit chain
   overflows). Defensible; extends to D2 (fold-count also counts marks).
4. **Move chips clear only via feed change or face-off.** CONFIRMED (probe 5:
   identical re-pull persists chips; face-off clears). Intended.
5. **One-frame layout/chrome divergence on face flip.** CONFIRMED and
   WIDENED by this diff (→ D3). Two+ separate `m/latest` over
   `!effective-local-world`; R-2 added the `<layout>` arm.
6. **Open cards in an overflowed thread are absorbed into the chip.**
   CONFIRMED by code order: `(overflow? th)` (scene.cljc:579) is tested BEFORE
   the `(and (mark? e) exp?)` / terrain branches, so an expanded card in an
   overflowed thread never reaches its expansion. Edge case (max-lanes
   exceeded).

---

## Verdict (one paragraph)

The R-2 client diff is well-built and honestly green: the carry lifecycle is
sound (order/band flips and permuted re-pulls fire no phantom, face-off
clears, canonicalization defeats permuted pulls), the new text nodes are all
clip-contained, per-band bounds equal the visible paint, and the trap-13
feedback loop is structurally impossible (`!trail-prev-carry` is unwatched and
out of the memo key). But the review's default-fail gate is not passed on two
counts. First and highest: the live feed constructor emits no lineage
endpoints, so the entire lanes/threads/move-chip feature set renders nothing
on the real corpus — the gates pass only against a hand-extended fixture whose
`:from`/`:to` shape the producer does not emit, so R-2's headline value is
un-demonstrated on live data and blocked behind a server change (B1). Second,
two latent logic breaks exist with concrete failing inputs — a phantom
"joined thread" chip on every member of a thread that gets absorbed by a
smaller-rooted newcomer (S1), and an order-dependent edge on same-ms
transitions that falsifies the §2.5 canonicalization guarantee (S2) — both
currently dark because of B1 but real once endpoints land. Recommend: gate the
feature demo on the server `:from`/`:to` addition, and fix S1/S2 (small,
local) before first light so the map does not lie the moment threads appear.
