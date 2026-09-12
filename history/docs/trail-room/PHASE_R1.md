# PHASE_R1 — trail-room R-1 (rim + address + kraft marks)

Builder: Opus 4.8 (1M), 2026-07-05. Scope: CONTRACT §1 items **2, 3, 4, 7**.
No commits (HQ commits). Files touched are ONLY the contract allowlist.

**Mid-phase policy amendment (Sid throughput ruling, via HQ coordinator,
2026-07-05, received during this phase):** builders do NOT run the full
cross-package suite — G5's "cross-package suite green in one run" moves to
HQ's final integration phase (one JVM, serial). Builder responsibility =
ns-level runs of the trail-face test namespaces being edited + the shadow
`:dev` compile check. G1–G4 + G6 stand exactly as written, at namespace
level. The G5 row below is scoped accordingly; one 6-ns cluster run that
happened BEFORE the ruling arrived is retained as informational history only.

---

## §5 Builder-duty verification (grep-verified, file:line)

All verified against the working tree before code, re-confirmed after.

1. **Mode derivation** — `runtime/workspace_actions.cljs`
   - `derive-effective-local-world` :125; `trail-face (:face trail-face-state)`
     :147; `/trail timeline` wins → `(= :timeline trail-face) :trail-timeline`
     :150 (checked BEFORE flow/file/editor in the `mode` cond); `/trail off`
     clears `:face` (in `wiring.cljs`, not mine) so `mode` falls through to
     `:editor`.
   - **Boot-default one-line flip (C3/G4):** `local-world-mode` :233-235 —
     `(or (:mode local-world) :editor)` at **:235**. Untouched.
2. **Card passport paint** — `trail_face/cards.cljc`
   - `feed-entry-card` :266; the removed 4th line was
     `addr-ln (pr-str (:entry/address entry))` (raw EDN address on the closed
     card face — item-2 target). `expanded-address-op` :313.
3. **Status-strip render site** — `combined_text.cljs` `<chrome-text>`
   :175-199 (`status-left-text` / `status-right-text`); the strip **bg** rect
   is `cmd_panel.cljs`:186-188; `status-bar-h 24` = `sidebar.cljs`:18. This is
   "the single file that renders the status strip" (allowlist); named here
   before editing.
4. **Palette-over-face wiring (C1)** — Ctrl+K → `:toggle-command-panel` at
   `events.cljs`:89; handled `keyboard.cljs`:31-40 with **no mode guard**, so
   the cmd panel (the palette) opens over ANY mode incl. the trail faces. C1
   holds untouched.
5. **Glyph coverage (trap 5)** — `resources/public/font_atlas.json` (588
   glyphs). Verified by codepoint scan: `│`U+2502 `├`U+251C `└`U+2514 `•`U+2022
   PRESENT; also `·`U+00B7 `↔` `✎` `↓` `─` `→` `●◐○◌` PRESENT. **`⊢` U+22A2
   RIGHT TACK is ABSENT** → substitution recorded below.
6. **`:relation-transition` feed shape** — `test/resources/trail_face/feed.edn`
   entries 0/3/4: `:entry/kind :relation-transition`,
   `:entry/detail {:kind :status :relation-id :from{:kind :id} :to{:kind :id}}`,
   `:entry/actor {:asserted-by :written-by}`. Consumed at `scene.cljc` as the
   `rt-edges` keep (was `entry-edges`); NOT invented.

---

## What changed, per file (all in allowlist)

### `trail_face/cards.cljc` (item 2)
- **NEW** `address->addressable-string` :51 — total over the WP1 `->address`
  shapes; `(trail/context-bundle {:targets [id …]}) → id`,
  `(trail/recent-activity {… :order o}) → "recent-activity · o"`; never a
  `pr-str` map dump.
- `feed-entry-card` :266 — **removed** the raw EDN address line; card is now
  name + stamp (+ badges). Address kept as node DATA
  `:trail-face/address (:entry/address entry)` :299 (ledger 1 — paint moved,
  data stays). Hit bounds shrink WITH the paint (trap 6: paint/bounds stay
  consistent, click still lands).
- `expanded-address-op` :313 — now takes the element address and renders the
  **addressable string** (the id), tagged `:trail-face/element-address?`
  (distinct from the FACE address, so G1 counts them apart).

### `trail_face/scene.cljc` (items 2, 3, 4)
- **NEW** `rim-slots` :80 (item 3) — pure, EXACTLY four slots
  `[:scope :delta :address :palette]`; total over both faces; stored in the
  scene root `:data :trail-face/rim-slots` (:141 text, :521 timeline).
- **NEW** kraft machinery (item 4): `kraft-tack` :195, `kraft-rgba`,
  `kraft-label-text`, `kraft-mark-nodes` :216, `kraft-handle-node` :265.
- `expansion-node` — the OPEN card address now uses `(:address tb)` (the
  element's own address) → addressable string; **removed the OUTER `:clip?`**
  and wrapped the material preview in a `:material-clip` (clip?, card-w)
  container (:342 note) — see gate 6 below. A node's own text clips to its
  INCOMING clip, so removing the outer clip alone would have let long preview
  lines overflow to the viewport; the wrapper restores card-width text
  clipping while the bg-bearing holes/omissions clamp to the viewport (no
  negative-y bg rects when scrolled above).
- `build-timeline-scene` — **removed the scene-header FACE-address op** (moves
  to the rim, C2); split entries: terrain → `feed-entry-card`;
  `:relation-transition` CLOSED → kraft marks (no box-card), OPEN → kraft
  handle + expansion surface; `rt-edges` route to kraft marks, `bundle-edges`
  stay connectors; rim stored in `:data`.
- `build-text-face-scene` — KEEPS its View-3 header address (that face law is
  unchanged, out of item-2 "closed cards" scope) and ADDS the rim.

### `runtime/workspace_actions.cljs` (item 7, guard only)
- `trail-face?` binding :158 and `:sidebar-visible (boolean (and
  sidebar-visible (not trail-face?)))` :166 — the R1 guard: the effective
  world never reports the sidebar visible in a trail face. Boot-default flip
  (:235) untouched.

### `combined_text.cljs` (item 3, status-strip render only)
- `<chrome-text>`: `trail-mode?`/`rim-slots` from the CACHED scene :98-99; the
  status-strip ops branch on `trail-mode?` :199 — trail → the four rim slots;
  else → the EXACT original `status-left/right` ops (editor byte-identical,
  G2). Added `(m/watch !trail-face-scene)` (11 args / 11 flows; `m/latest`, no
  `m/ap` — CLAUDE.md).

### `test/app/client/workspace/trail_face_test.clj` (gates)
- Updated gate 1 (`address-law-test`) timeline branch + gate 8
  (`two-clocks-test`) for the item-2/item-4 behavior changes; added 4 new
  deftests G1–G4. Text-face gate-1 branch UNCHANGED (View-3 law intact).

---

## Gate evidence (op-level; test names + counts)

Run (per the throughput ruling): `clojure -M:test` over
`app.client.workspace.trail-face-test` ONLY (the one test ns this package
edits; 18 deftests) + the shadow `:dev` compile check.

| Gate | Verdict | Evidence |
|---|---|---|
| **G1** address off card face | **PASS** | `g1-address-off-card-face-test` + updated `address-law-test`: closed card has no `context-bundle`/`{:targets` text, address kept as `:data`; scene has ZERO face-address ops; rim has exactly one (`recent-activity · arrival`); open card prints `oc:doc:9fdoc`. |
| **G2** rim v0 (four slots) | **PASS** | `g2-rim-v0-test`: exactly 4 slots `[:scope :delta :address :palette]` on BOTH faces; scope="softland… in view", delta="since last pull … arrived", palette hint present; non-trail scene carries no rim slots (editor strip = original ops, verified in the combined_text else-branch which reproduces the two original ops byte-for-byte). |
| **G3** kraft marks | **PASS** | `g3-kraft-marks-test`: both endpoints → `:kraft-connector` label + connector rects, NO `:feed-card`, label carries kind+asserter+tack; one off-screen → `:kraft-line` naming the far end + "off screen"; scene-level — the 3 `:relation-transition` entry-keys yield NO feed-cards but DO yield kraft `based-on` labels. |
| **G4** C1–C3 | **PASS** | `g4-constraints-test`: `parse-trail-command` still parses `/trail timeline|off|order` (C1 palette entry untouched); source assert `(or (:mode local-world) :editor)` still present + the `not trail-face?` guard present (C3); rim has exactly one address (C2). |
| **G5** (re-scoped by the throughput ruling: builder = ns-level tests + shadow compile; cross-package = HQ final phase) | **PASS at builder scope** | ns-level: `trail-face-test` **18 tests / 429 assertions, 17 green / 1 red** — the 1 red is `fixture-fidelity-test` (view-mvp gate 16), NOT an R-1 gate: its fix is already recorded with the P3 builder (three lines, `trail_face_test.clj` ~611/615/635 fixture-pairing), landing AFTER this phase. `shadow :dev` compile GREEN both runs: "Build completed (248 files, 15 compiled, 0 warnings)" and, after the material-clip fix, "(248 files, 11 compiled, 0 warnings)". Cross-package green-in-one-run: **HQ final integration phase**. *Informational history (pre-ruling run):* 6-ns cluster = 37 tests / 991 assertions, same single gate-16 red, zero failures introduced by this package. |
| **G6** `file(1)` = text | **PASS** | all 5 touched files → "Clojure module source, Unicode text, UTF-8 text" (output below). |

Baseline (pre-change): `trail-face-test` 14 tests / 460 assertions, 0 fail.
After: `trail-face-test` = 18 tests (my +4: G1-G4), **17 green / 1 red** — the
single red is `fixture-fidelity-test` (gate 16), whose three-line
fixture-pairing fix is recorded with the P3 builder and lands after this
phase (see G5 + handoff note). Every gate this package OWNS (G1-G4) and
every gate my changes touched (1, 6, 8) is green.

---

## Glyph substitution (trap 5)

- **`⊢` U+22A2 RIGHT TACK → `├` U+251C** (BOX DRAWINGS LIGHT VERTICAL AND
  RIGHT). `⊢` is ABSENT from the merged `font_atlas.json`; `├` is present, is
  in the design's own blessed connector set (`⊢ │ ├ └ •`), and reads as a
  branch junction. Applied in `scene.cljc` `kraft-tack` :195; used by every
  kraft mark/handle label. **OI-2 slug expansion is the real fix; the atlas is
  NOT regen'd in this package** (per trap 5). Every kraft label still passes
  `sanitize-tree`, so any future uncovered glyph degrades to U+FFFD honestly.

---

## Recorded debt (stop-clause §6)

1. **Sidebar force-hide vs auto-appear (item 7).** The design end-state is
   "no sidebar" in a trail face; the phase-1 requirement (and my guard) is
   "never AUTO-appears", which holds — there is NO code path that auto-shows
   the sidebar on entering a trail face; it appears only on explicit Ctrl+B.
   My guard makes `derive-effective-local-world` REPORT the sidebar hidden in
   trail faces. However, the load-bearing renderers (`editor_compute.cljs` /
   `combined_text.cljs` `<layout>`) read the `!sidebar-visible` ATOM directly,
   so a sidebar the user opened BEFORE entering a trail face is not
   force-hidden. Fully enforcing "no sidebar" would gate those `<layout>`
   readers on mode — `editor_compute.cljs` is OUTSIDE this package's
   allowlist. Deferred (matches the contract's "guard only" scoping + the
   §6 status-strip-entanglement pre-flag).
2. **Relation-transition entries stay expandable.** The design stance is
   "relations are pure connectors". R-1 keeps a `:relation-transition` entry
   expandable (OPEN → a kraft handle + detail surface) because the hand
   `bundle.edn` only carries target-bundles for `oc:doc:9fdoc` /
   `oc:doc:deadend-doc` (both reached ONLY via relation-transition entries),
   and the fixtures are OUTSIDE my allowlist — so the expansion gates (1, 11,
   12, 13) can only be exercised through a relation entry. The CLOSED form is
   a pure mark (G3 satisfied). Whether an open relation should surface at all
   is an R-2/design question.
3. **Rim horizontal overflow.** The four rim slots lay out left-to-right by
   `char-advance`; a very long address + long delta could exceed the viewport
   width at small windows (the renderer clips at the right edge). A right-
   anchored address (as the old status-right was) or slot elision is a v1
   refinement — not gated in R-1.

---

## Handoff note (for HQ + the P3 builder)

`fixture-fidelity-test` (view-mvp gate 16) is the single ns-level red. Cause
chain, verified: the git-spine builder's `trail_view.clj` now emits
`:display-name` on source/transcript `:entry/target`s (unstaged; HEAD had
none) and `feed.edn` was re-derived to match — but the gate pairs the FIRST
hand entry (a `:relation-transition`, correctly display-name-less) against
the FIRST live entry (a source/transcript row that carries it). Per the
coordinator, the **P3 builder has the three-line fixture-pairing fix
recorded** for `trail_face_test.clj` lines ~611/615/635, applying AFTER this
phase.

**No-collision confirmation (this phase's duty):** my diff hunks in
`trail_face_test.clj` are `@@ -63 @@`, `@@ -77 @@`, `@@ -287 @@`,
`@@ -438,+460 @@` — the last (G1–G4 insertion) closes before the gate-16 body;
lines ~611 (`live-entry (first …)`), ~615 (`hand-entry (first …)`), ~635 (the
`"entry target"` shape-compare) are UNTOUCHED by me, and none of my gates
required touching them. `trail_view.clj` and `feed.edn` left exactly as
found, per instruction. My `feed-entry-card` already renders `:display-name`
when present (`(or (:display-name target) (:id target))`), so R-1 is
forward-compatible with the P3 fix either way.

---

## `file(1)` output (G6)

```
src/app/client/workspace/trail_face/cards.cljc:          Clojure module source, Unicode text, UTF-8 text
src/app/client/workspace/trail_face/scene.cljc:          Clojure module source, Unicode text, UTF-8 text
src/app/client/workspace/runtime/workspace_actions.cljs: Clojure module source, Unicode text, UTF-8 text
src/app/client/workspace/combined_text.cljs:             Clojure module source, Unicode text, UTF-8 text
test/app/client/workspace/trail_face_test.clj:           Clojure module source, Unicode text, UTF-8 text
```
