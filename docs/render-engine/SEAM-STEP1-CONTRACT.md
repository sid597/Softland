# SEAM-STEP1 — CONTRACT (one phase · implementer: Codex · gate: Fable, FULL tier)

Cut 2026-08-03 from `SEAM-STEP1.md` (the starter) + `SEAM-STEP1-CONTRACT-HANDOFF.md`
(the banked reconnaissance) under the work-package skill's few-and-large rule:
ONE implementation phase, no PLAN.md, plan-grade specificity carried here.
One fresh default-fail validation round ran over this contract; it returned
FAIL (four defects, nine minor) — preserved verbatim in
`SEAM-STEP1-CONTRACT-VALIDATION-R1.md` — and every finding is folded in
below, cited by number where it reshaped a section. RULING R1
(pick-follows-paint, §4a) resolves the round's one policy fork.

**Binding docs + precedence:** decisions.md "The render seam" (settled + amended
2026-08-03) and THIS contract bind. `SEAM-STEP1-NOW.md` is the baton — if it
contradicts either, they win; flag the discrepancy in NOW, do not pause. The
handoff file is reconnaissance, not authority; where it and this contract
differ, this contract governs (re-verification + the validation round
corrected several of its bullets — the stamped≡refreshed premise, the pick
caller count, the memo key, the fence-pin inventory).

**Manifest law:** locators below are navigation hints machine-verified with
`grep -n` on 2026-08-03; SUBSTANCE (named symbols, forms, semantics) binds.
Hint drift with substance intact → re-locate, log in the phase artifact, never
stop. Substance missing → stop clause §S5.

---

## 1. Purpose — in both vocabularies

First code act of the render-seam constitution: **the maintained tape**. In
Sid's words, the Electric-native arc starts landing here — the store that
"electric native and equivalent" stands on. Three acts, one phase:

- **Act 1** — the scene store's order becomes a maintained sorted view patched
  at write sites; the batch scene compiler is DEMOTED to oracle behind a new
  equivalence fence (the growth law's first crossing). The per-event pick
  recompile and the frame-counter-as-derivation-ancestor die.
- **Act 2** — the editor's stable layout flows split from the 530ms ticker
  overlays (the caret blink stops re-shaping a document it never touched);
  forked flow copies unify at their real sharing points; fenced-view
  instance #2 BEGINS (rescan consumers upgraded to keyed ones — begun, not
  finished).
- **Act 3** — the `!camera` watch leaves scene derivation (quarantine to the
  frame sink), then the honest fix: camera-aware pick; the camera→registry
  edge loses its reason to exist.

**The diff touches no Electric file.** Transport stays untouched;
`electric_flow.cljc` is NOT edited (the stamps Act 2 consumes —
`:source-revision` :441, `:layout-line-id` :465 — already exist there).

**Consumers, in order:** the workspace frame path today (renderer + editor +
pick) · the engine floors as they land (the store contract they stand on) ·
later: undo, the wire, collaborators (why minted diffs stay values — out of
scope here, named so no one deletes the door).

## 2. Non-goals — each a named extension point, not a void

- NO store-contract slice (per-key read subscriptions, write-site dispatch,
  m/signal fleet) — that package opens at composition pressure.
- NO host probe (Electric vs Missionary face host) — open register, probe
  decides later.
- NO furniture migration to screen-space containers (studio birth-handle,
  workshop door, error card, binding lint) — the pick CAPABILITY lands; the
  furniture is foreign/uncommitted sibling code. Flag sites in the artifact,
  touch none of them.
- NO patch-driven frame executor — today's walk stays; profiles summon it or
  they don't.
- NO finishing of fenced-view instance #2 — Act 2 upgrades the named
  renderer rescan only; keyed shaping is a later slice, and the THIRD
  `:line/id` scan (`text_layout.cljc` :669 `clip-result`) is named-deferred
  to it (§5b).
- NO new watch anywhere in the seam; NO clock-typed derivation input; NO
  derived state materialized through watch-mirror atoms (constitution law).

## 3. Tree state — foreign work + the adopt ruling (T11)

Sibling (Package 2 / studio / smalltalk-ui-vm) work sits UNCOMMITTED in the
tree: `ground.cljs` (+819), `facet_master.clj`, `server_jetty.clj`,
`anatomy_test.clj`, `material_truth_test.clj`, untracked `studio.cljc`,
`workshop_playground.cljs`, `image_material.cljc`, `shared/studio.cljc`,
`shared/workshop_playground.cljc`, `build/studio/PLAYGROUND.md`,
`build/smalltalk-ui-vm/PODCAST_SCRIPT.md`.

**Ruling (Sid, at contract landing): ADOPT-OR-STOP on exactly two files whose
foreign hunks sit on Act 2's surface** — `editor_compute.cljs` (+10: the
mode-guard `(not (contains? #{:editor :file-workspace} mode)) →
empty-face-content` at :699–700, the DEGENERATE form of Act 2's split) and
`test/render_engine/verify_text_layout_fence.mjs` (+36:
`auditDormantEditorBranch` pinning `<editor-content` anchors + seeded
self-test). RULING RECORDED: **ADOPT** — this package absorbs those two
files' hunks; Act 2 subsumes the guard and moves the fence's pins. If this
line does not read ADOPT, stop clause §S1 fires at Act 2 staging.

**Every other foreign file: never modified, never staged.** One exception with
a scalpel: `ground.cljs` carries Act 3's `pick-at` call site (:3270). Verified
2026-08-03: no foreign hunk overlaps it (foreign insertions end at :2850 and
resume at :4004). The package edits ONLY the `pick-at` region in that file.
**Codex makes NO git commits** (§10) — hunk-level staging is the close
session's job; this clause exists so the close session can stage cleanly.

## 4. Act 1 — maintained ordered view (store + frame edge)

### 4a. Ground facts (verified; the design stands on them)

- Store value `{:slots {vi→slot} :index {address→#{vi}}}`
  (`scene_store.cljc`). Writers: `upsert-slot` :135, `remove-slot` :161;
  `update-nodes-by-address` :177 REBUILDS THROUGH `upsert-slot` (:194) — so
  all writes funnel through exactly two functions. The `:ordered` patch lives
  in those two; any future writer that bypasses them is what the equivalence
  fence catches.
- `slot-entry` :213 builds the tape entry: `:entry/id [:scene-slot vi]`,
  `:order {:stratum … :pass-class :direct :stack-path <stamped-or-refreshed>
  :part-rank 0 :stable-tie vi}`, `:runtime/slot` = the whole slot. 1-arg
  `scene-tape` :240 passes `{}` → STAMPED paths (paint projection); 2-arg
  refreshes from the effective map (pick). `pick` :265 compiles the full tape
  EVERY pick — the named disease.
- **Total order:** `:stable-tie` = vi and `:entry/id` = `[:scene-slot vi]` →
  no two distinct entries tie → a sorted map keyed `[order-key entry-id]` is
  deterministic AND agrees with the batch stable sort over the SAME entries.
  Equivalence against the 1-arg batch is exact.
- **Registry immutability:** `containers.cljc` `set-transform` :143 mutates
  ONLY `:affine`; `:layer`/`:sibling-rank`/`:parent` are fixed at
  `add-container` :86 (re-add throws; `remove-container` :158 refuses
  parented cids) → a stamped stack-path cannot go stale for any
  registry-registered slot. Every committed production writer stamps the
  real composed path at registration (`scene_runtime.cljs` :153–159 reads it
  from `ctn/effective` at upsert).
- **The stampless-slot fact (validation R1, finding 1 — proven by probe):**
  `build-slot` defaults `:stack-path` to `[]` (:87), which is truthy, so a
  slot upserted WITHOUT a stack-path orders as root in the 1-arg (stamped)
  paint projection while the 2-arg (refreshed) form composes its real path —
  paint and pick can DISAGREE on order for stampless slots in the committed
  tree today. Stampless writers: every fixture in `scene_store_test.clj`,
  and the foreign `workshop_playground.cljs` :143.
- **RULING R1 (Fable, at contract landing — pick-follows-paint):** the
  maintained (stamped) order is the ONE order truth for BOTH projections.
  `pick`'s own docstring promises "the exact reverse of the paint tape"; the
  refreshed path silently broke that for stampless slots. Consequences: the
  batch ORACLE is the 1-arg `scene-tape` (stamped — equivalence exact); the
  2-arg refreshed form is RETIRED from the pick path (it remains for
  explicit-transform callers/tests); for registry-stamped slots (all
  committed production) pick behavior is unchanged; for stampless slots pick
  order changes to agree with paint — G1 documents this with a stampless
  probe, and the phase artifact records it.
- **RULING R1's one committed casualty + its pinned repair (validation R2,
  N1 — proven by probe):** `scene_store_test.clj` :280 `pick-layer-and-miss`
  is stampless AND overlapping; its "higher `:layer` wins" pick expectation
  encodes the refreshed order, so the ruling flips its result. The repair is
  fixed HERE, exactly: the two upserts at :290–291 gain
  `:stack-path (:stack-path (get effs 10))` / `(get effs 20)` respectively —
  the fixture becomes registry-stamped, which is what every production
  writer already is; **assertions unchanged — the fixture is re-stamped,
  never re-expected.** This is the single test-file edit the package makes
  (§12 exception); any OTHER committed test flipping under the ruling is
  §S4 with the case verbatim, never a quiet edit.
- Order machinery (`scene_tape.cljc`): `compare-scalar` :393, `compare-seq`
  :399, `compare-order` :427 — all currently PRIVATE (`defn-`), as is
  `validate-entry!` :439. `compile-tape` :469 validates + sorts.

### 4b. The build

**In `scene_tape.cljc`** (placement ruling §8): export the compare fns
(public), add the ordered-view kit —
- `entry-key-compare`: comparator over `[order-token entry-id]`. It reuses
  `compare-order`, **which takes ENTRIES, not bare tokens** — call it as
  `(compare-order {:order lt} {:order rt})` or export a token-level
  `compare-order-token`; a bare token handed to `compare-order` compares
  nils and returns 0 for every pair, collapsing the sorted map to one entry
  (validation R1, finding 5). Tie-break with `compare-seq`/`compare-scalar`
  over the id vector. **Never default `compare`** (T1: Clojure vector
  compare is LENGTH-FIRST — `(compare [2] [1 1])` < 0 — it mis-orders
  stack-paths).
- `ordered-insert` / `ordered-remove` over a persistent sorted map with that
  comparator; `validate-entry!` becomes PUBLIC and runs at insert — its
  signature is `[registry entry]`, and the store passes
  `default-family-registry` (`scene_tape.cljc` :384, the same registry the
  2-arity `compile-tape` defaults to — validation R2, F2 residual).
  `compile-tape` keeps its own validation BYTE-IDENTICAL (T6: the oracle is
  demoted, never weakened — ADD validation at the edge, remove nothing).

**In `scene_store.cljc`**: the store value gains `:ordered` (sorted map
`[order-key entry-id] → entry`, entries via `slot-entry` with `{}` = stamped
paths). `upsert-slot` removes the OLD slot's key (derived from the old
slot's entry — no extra index) and inserts the new; `remove-slot` removes.
`empty-store` seeds it. One legal rebuild door `rebuild-ordered` (from
`:slots`) for any rehydration path (T3: EDN pr-str/read-string silently
degrades a sorted map to a hash map — the door is the ONLY sanctioned
reconstruction). **The walk and the oracle are two DISTINCT code paths
(validation R1, finding 2 — a fence over one expression proves nothing):**
NEW fn `maintained-entries` returns `(into [] (map val) (:ordered store))`
(the sorted map yields MapEntry pairs — take `val`, finding 13a);
`scene-tape` — BOTH arities — stays the batch compile, byte-untouched: the
1-arg form IS the named oracle (RULING R1). `<store-frame` and `pick`
consume `maintained-entries`, never `scene-tape`. `pick` walks the
maintained entries in reverse with `pick-reverse`'s exact filters
(`:visibility :visible?`, `:pick ≠ :none`); the per-entry hit fn (inverse
transform via the CURRENT effective map) is unchanged — the maintained view
replaces ORDER derivation, not transform lookup. `ordered-slots` :257 has
zero callers (grep-verified) — leave it on the batch path, unspecified.
`store-fns-free?` :527 is unaffected (comparator is collection-internal;
verify, don't assume).

**T10 (ops identity):** untouched slots keep the SAME ops arrays through the
`:ordered` walk — `identical?` skips downstream (`content-same?`, pool diffs,
slot-geo skip) must survive. `<store-frame` (`scene_runtime.cljs` :384–416)
walks the maintained view and derives the SAME keys with the same ops
identity: `:rects :shadows :text-by-vi :ordered-vis :ops-count-by-vi
:order-by-vi`. The `:scene-tape` key (:415) is DROPPED — grep-verified no
consumer outside `<store-frame`. Renderer consumers
(`store-pool-entries` :1948, `render.cljs` :677–685) see identical shapes.

**Frame edge (`renderer.cljs`)**: the per-RAF `compile-frame-tape` :2185
(world-revision `[:frame (:frame-idx frame)]` — the named crime) leaves the
per-frame path. A renderer-owned frame-arrangement atom (T8) holds **a
sorted map `[order-token entry-id] → entry` built with the same
`scene_tape.cljc` kit** (one shape, named — validation R1, finding 13c):
producers (`frame-order` :1904 static ranks, `system-entry` :1973,
`clip-entries` :1988, `shadow-entries` :2007, `rect-entries` :2021 over
`frame-family-registry` :2151) still run per frame (bounded, ~dozens of
entries); per produced entry — existing `[order-token entry-id]` key →
`assoc` the value (payload rebind, cheap); new or token-changed key →
`validate-entry!` + sorted insert; keys absent from this frame's production
→ removed. **The diff of produced `[entry-id order-token]` pairs against
the arrangement's current keys is the keying source** for order
re-derivation — never `:frame-idx`, never a revision stamp. `draw-frame!`
:2207 + `execute-scene-tape!` :2194 walk the arrangement's vals.
**The batch stage stays ALIVE (validation R1, finding 4 — an oracle nothing
runs is a deletion wearing a name), with NAMED homes (validation R2, gap
4b):** the arrangement-update fn is named `update-frame-arrangement`; the
dev-flagged door is named `frame-tape-twin-check!` — when the flag is on it
compiles the batch tape via `compile-frame-tape` each frame and asserts
entry-sequence equality with the arrangement, logging any divergence.
G10's sitting runs ≥1 minute with the flag on (NOT inheritable — gap 4a);
that receipt is the demotion made executable. JVM G2 covers the shared
cljc ops.
**T12:** `frame-idx` stays legal at the sink (≤5-frame boot logs, loop
control) and INSIDE the oracle's own compile — only its ancestry into the
maintained ORDER DERIVATION is illegal, and that is what fence G3's new
assertion pins.

## 5. Act 2 — editor flow split + sharing points + keyed rescan (begun)

### 5a. Ground facts (verified)

`editor_compute.cljs` `<editor-rects+sidebar` :414: `<layout` :432 (5
watches), `<mode` :463, `<sidebar` :479 (SIDE-EFFECTS in combine: resets
`!sidebar-scene`, cache atom), `<trail-face` :530 (same shape), `<face-main`
:604, `<intake-content` :626, `<run-content` :645 (watches
`!shimmer-phase`), `<editor-content` :665 — ONE 18-input `m/latest`
including `!caret-visible` + `!shimmer-phase`: every 530ms tick re-runs
`compute-editor-rects` (FULL proportional layout since T1). Ticker source:
`events.cljs` `make-blink-timer` :192, wired `runtime.cljs` :399–400 —
NEITHER file is touched (tickers stay; their subscription point moves).
`<layout` is consumed by ~7 sibling combines — per Missionary semantics each
`m/latest` use is a SEPARATE subscription/process: the hand-rolled sharing
points the constitution names.

Verified laws (electric-docs skill, regression-tested build −45; cite in
code comments by number): **L6** (coarse invalidation grounds the split) ·
**L8** (raw m/latest diamonds GLITCH deterministically) · **L4** (m/latest
is demand-driven; no side effects in combines) · **L12** (genuine
shared-process/multicast belongs to `m/signal`/`m/stream` — and Missionary
is b.46, TRANSITIVE AND UNPINNED, with NO claims test for m/signal yet).

### 5b. The build

- **The split is a CHAIN, never a diamond (T4/L8):** a stable stage
  (doc-derived inputs only — the current 18 minus the two ticker atoms)
  computes the full layout INCLUDING caret geometry; an overlay stage
  consumes the stable stage's OUTPUT plus `(m/watch !caret-visible)` /
  `(m/watch !shimmer-phase)` and only toggles caret presence/alpha and
  shimmer paint. The overlay never re-derives from doc atoms — two
  doc-derived branches merged downstream is the L8 glitch. `<run-content`
  gets the same treatment for its shimmer watch. The adopted mode-guard
  (§3) lands in the stable stage (cheap skip before layout).
- **G4 ORDERING GATE (non-negotiable):** new deftests in
  `test/app/missionary_claims_test.clj` proving m/signal semantics on THIS
  build land green BEFORE any unification relies on m/signal: (1) ONE
  upstream process shared across N subscribers (side-effect counter
  increments once per upstream change, not N times); (2) late subscriber
  sees latest immediately; (3) zero-subscriber lifecycle — what terminates,
  what restarts on resubscribe (assert OBSERVED behavior, document it);
  (4) a side-effecting combine (the `<sidebar` shape) under signal vs raw
  m/latest — when and how often the effect runs. **Pre-ruled fallback (§S2,
  not a stop):** if (1) or (2) falsifies on b.46, split the tickers WITHOUT
  signal — the split does not depend on it; unification defers, record in
  the artifact.
- **Unification at real sharing points:** after G4 green, `<layout` is
  wrapped in `m/signal` at its sharing point inside `<editor-rects+sidebar`;
  further forks (`<mode`, local-world) unify only if the identical pattern
  applies trivially — this is BEGUN-not-finished register, do not chase
  every fork. `<sidebar`/`<trail-face` (side-effecting combines, T5): if
  claims-test (4) shows lifecycle change under signal, leave those two
  UNSIGNALED and flag in the artifact.
- **Keyed rescan upgrade (fenced-view instance #2 BEGINS):**
  `renderer.cljs` `position-text-op` :1452, rescan at :1476 —
  `(first (filter #(= (:layout-line-id txt) (:line/id %)) (:lines
  layout-result)))` is a LINEAR SCAN per text op. Replace with a
  renderer-side index `:line/id → line`, **memoized on the layout-result
  object's IDENTITY** (a JS WeakMap keyed by the map that arrives as the
  op's layout-result; no entry → build once, GC does the lifecycle). The
  revision, when present, lives at `[:source :revision]` in the
  layout-result (NOT top-level `:source-revision` — that is the stamp's
  input at `electric_flow.cljc` :441) and is a HASH: log/assert it as a
  receipt, never use it as the sole cache key — some `position-text-op`
  layouts are computed inline with no revision at all (validation R1,
  finding 7). The lookup becomes a map `get`. `electric_flow.cljc` is NOT
  edited. **Named-deferred (finding 11):** a THIRD identical `:line/id`
  scan lives in `text_layout.cljc` :669 (`clip-result`, reached from
  `combined_text.cljs` and `rect_tree.cljc`) — it is instance #2's next
  surface and is explicitly OUT of this package (§2); the artifact lists
  it so the keyed-shaping slice finds it named.
- **Fence pins move, never die (T7):** the split changes `<editor-content>`'s
  arity — the `;; 18 fn args` anchor WILL move; the mode-guard string may
  relocate into the stable stage. Move `verify_text_layout_fence.mjs`'s
  pins (including the adopted `auditDormantEditorBranch` anchors) to the new
  shapes; every forbid/assertion stays; keep the seeded self-test pattern.

## 6. Act 3 — camera quarantine, then screen-space pick

### 6a. Ground facts (verified)

`ground.cljs` `!camera` :87 (shader law: screen = world·zoom + pan),
`screen->world` :169, `pick-at` :3270 → `scene-rt/pick-world` :304 →
`ss/pick`. Second pick site: `runtime/mouse.cljs` :460. `render.cljs` :187 —
`(m/watch ground/!camera)` INSIDE `<world-snapshot`'s m/latest (line carries
a `;; first-light P2b` comment — relocate the comment's story to the sink,
don't silently drop it): every pan tick rebuilds the snapshot and re-fires
the combine. RAF `m/reduce` :220 with `identical?` world skip;
`camera-moved?` full-clear logic :556 exists and stays. `containers.cljc`
`effective` :221 already emits `:flags` 1 for `:camera :screen` (:236) and
`screen-bounds` :284 branches on it — but `ss/pick` does NOT: callers
pre-convert screen→world for ALL containers, so screen containers mis-pick
under pan/zoom.

### 6b. The build

- **Quarantine:** the camera watch leaves `<world-snapshot`'s inputs and
  becomes a deref at the RAF sink with its own prev-compare. **CARE (named
  in the handoff, binding here):** with camera out of the snapshot, a pan
  tick leaves world `identical?` — the RAF body must still draw on camera
  change. The redraw condition becomes: world changed OR camera changed
  (sink-local prev-camera compare — the `camera-moved?` compare moves from
  the world map to the sink) OR dirty-rect pending. Pan must still redraw
  (G6 live half).
- **The honest fix — camera-aware pick, BACKWARD-COMPATIBLE (validation R1,
  finding 3 — the point-map-only form breaks nine committed test call sites
  in files §12 forbids editing):** `ss/pick`'s point argument accepts BOTH
  forms — a bare `[x y]` vector means "this point in both spaces" (exactly
  today's semantics; every committed test call site stays untouched and
  green), and `{:world [wx wy] :screen [sx sy]}` supplies the two spaces.
  Normalize at entry. The per-entry hit fn selects by the entry's effective
  SCREEN FLAG — `(= 1 (:flags eff))`, the same predicate `screen-bounds`
  uses (`containers.cljc` :287); there is no `:camera` key on the effective
  map (finding 6), and `containers.cljc` is read-only. **T9 —
  direct-vs-transitive, re-enumerated:** the DIRECT `ss/pick` callers in
  `src/` are `scene_runtime.cljs` `pick-world` (:312, inside :304) and
  `scene_store.cljc` `context-bundle` (:459, inside :445). The map form is
  threaded from the two cursor sites — `runtime/mouse.cljs` :460 and
  `ground.cljs` `pick-at` :3270 (the ONLY `ground.cljs` edit — §3) —
  through `pick-world`. `bundle-for-viewport` :328 / `bundle-at-world-point`
  :341 reach pick transitively and stay world-only (they resolve
  world-space material context; if that proves wrong it is an artifact
  flag, not a silent change). A NEW direct caller found = artifact flag.
- The camera→registry edge (screen-pinned furniture re-placed per camera
  tick) loses its reason to exist. MIGRATION IS OUT OF SCOPE (§2): list the
  sites in the artifact (`ground.cljs` error card, binding lint, studio
  birth-handle — inside the foreign hunk; `workshop_playground.cljs` :1416
  `::wsp-door` camera watch), touch none.

## 7. Standing guards (the open register stays open)

Diffs minted as canonical facts, as values, in one namespace — destination
deltas derived behind their doors · blob assets by id+revision, bytes never
in the scene value · store internals private behind the two interfaces
(write ops in / projections out) · no new watch, no clock-typed derivation
input · playgrounds and probes stay free until they feed frames (the toll is
at the frame door).

## 8. Placement rulings (with reversal costs)

- **Order machinery → `scene_tape.cljc`.** It owns the order vocabulary
  (compare fns, validation); a new namespace would fork it. Rejected:
  `scene_store.cljc` (would couple frame-edge reuse to the store), new ns
  (vocabulary fork). Reversal: move fns, callers chase — cheap, no state.
- **`:ordered` → `scene_store.cljc` store value.** The projection lives
  beside what it projects; writers patch it where they already write.
  Reversal: delete the key + walk call sites back to compile — the oracle
  path never left.
- **Frame arrangement → `renderer.cljs` atom (T8).** Frame entries are
  renderer-private (producers, registry, executors all live there); the
  store must not learn frame vocabulary. Reversal: same as above.
- **Line index → renderer-side memo.** Keeps `electric_flow.cljc` untouched
  (§1's "no Electric file" sentence); the consumer owns its own lookup
  shape. Reversal: move the index into the layout-result later (a later
  slice may, when instance #2 finishes).

## 9. Traps ledger — cite by number in code comments

- **T1** Default `compare` on a composite key: vector compare is
  LENGTH-FIRST → mis-orders stack-paths vs `compare-seq` semantics. Reuse
  scene-tape's own compare fns; never default `compare`.
- **T2** Order truth is STAMPED, per RULING R1 (pick-follows-paint): do not
  "fix" the stampless divergence by refreshing paths in the maintained walk
  — that reintroduces the per-pick scan AND re-breaks pick's
  reverse-of-paint promise. The fence catches any future re-layer API.
- **T3** EDN round-trip silently degrades sorted→hash map: `rebuild-ordered`
  is the ONE legal rebuild door; no other reconstruction.
- **T4** Split-as-diamond: L8 glitches deterministically → CHAIN only
  (overlay consumes stable OUTPUT).
- **T5** m/signal assumed: claims test FIRST (G4); side-effecting combines'
  lifecycle under signal asserted before any signal wrap touches them.
- **T6** Moving (not copying) validation out of `compile-tape` weakens the
  oracle: ADD `validate-entry!` at the insert edge; `compile-tape` stays
  byte-identical.
- **T7** Fence-deletion instinct: pins MOVE, assertions stay, seeded
  self-tests stay — both .mjs fences.
- **T8** Frame-view state placement: renderer-owned frame-edge atom; the
  store never learns frame vocabulary.
- **T9** Pick signature: the point argument is backward-compatible (bare
  vector = both spaces); two DIRECT callers (`pick-world`,
  `context-bundle`), two cursor sites threading the map form (§6b). A new
  direct caller found = artifact flag, not silent update. Committed test
  call sites are never edited.
- **T10** Ops-array identity: untouched slots keep the SAME ops arrays
  through the `:ordered` walk; `identical?` skips downstream must survive.
- **T11** Foreign-tree discipline: §3 verbatim — two adopted files, one
  scalpel edit in `ground.cljs`, nothing else touched, NO commits.
- **T12** `frame-idx` stays legal at the sink; only its ancestry into order
  derivation is illegal.

## 10. Definition of done + handoff

Acts 1→2→3 in order, gates interleaved (G4 strictly before Act 2's
unification; G6's static half after Act 3). Then:

1. Phase artifact `SEAM-STEP1-P1.md` in this directory: what was built per
   act, gate receipts, judgment calls, flagged sites (§2/§6b), the
   **diff-derived changed-file list** — `git diff --name-only` + status at
   artifact-writing time, every path classified allowlist / new-per-contract
   / DRIFT-flagged, never reconstructed from memory — sum-checked against
   §12.
2. NOW entry in `SEAM-STEP1-NOW.md` (≤15 lines; FAIL findings verbatim).
3. **NO git commits — the commit decision is Sid's, at gate review.** The
   tree is left clean-running with the package's edits in place.
4. **Prediction check (pre-registered, decisions.md render seam, last
   bullet):** did the plumbing dialects start converging toward
   two-plus-transport? One paragraph of observation in the artifact — cheap
   to falsify, report what IS.
5. Gate review: **Fable, FULL tier** (the frame path is cutover-class, not
   additive-and-dark): independent suite re-run, trap spot-checks in the
   diff, falsification pass, live receipts verified. Then Sid's commit
   decision; the close session stages hunks per §3.

**First-run-green expectation:** if any gate does NOT go green first run,
that is signal about this contract, and it belongs verbatim in the NOW
entry.

## 11. Acceptance gates — G1–G10, owners named, partition sum-checked

In-phase (Codex): G1 G2 G3 G4 G5s G6s G7 G8 G9 — G8's owner FLIPS to Sid's
sitting if the phase environment lacks Chrome (G8's clause). Sid's headed
browser (one sitting, at/before gate review): G5L G6L G10 (+ G8 iff
flipped). Sum: G1✓ G2✓ G3✓ G4✓ G5(s+L)✓ G6(s+L)✓ G7✓ G8✓ G9✓ G10✓ — every
gate on this list is owned; this line IS the partition sum-check (skill
rule, space-as-entity lesson).

- **G1 — store equivalence fence (JVM).** New ns
  `test/app/client/substrate/maintained_view_test.clj` (+ explicit
  `pure-namespaces` tier entry in `test_runner.clj` — classification is
  FAIL-CLOSED). Scripted keyed mutations driving ALL public write surfaces
  (`upsert-slot`, `remove-slot`, `update-nodes-by-address`, plus
  `set-transform` on the registry): after EVERY step assert
  `(ss/maintained-entries store)` ≡ `(:entries (ss/scene-tape store))` —
  **two distinct code paths: the maintained walk vs the 1-arg batch
  compile, the named oracle per RULING R1** (validation R1, finding 2: the
  first cut fenced one expression against itself); pick-maintained ≡
  pick-over-oracle-entries at probe points; validation THROWS at the
  insert edge on a malformed entry. PLUS the two R1-ruling probes:
  (a) registry-stamped fixtures — stamped ≡ refreshed ≡ maintained (the
  production equivalence); (b) ONE stampless fixture — maintained agrees
  with 1-arg PAINT order and diverges from the 2-arg refreshed form
  (documents the ruled pick-order change; this probe FAILING means the
  ruling's premise moved → §S4, with the case verbatim).
- **G2 — frame-arrangement equivalence (JVM).** Same ns: the cljc
  arrangement ops driven over synthetic frame-shaped entries (including
  visibility toggles, entry enter/exit, order-token change, payload-only
  rebind) vs the `compile-tape` oracle with a synthetic family registry —
  equivalence over entry SEQUENCES (the oracle's revision argument is a
  value, not compared). The renderer's atom glue is covered by the
  dev-flag twin-run door (§4b) + G3 pins + G10's flag-on minute.
- **G3 — both .mjs fences green with MOVED pins.**
  `verify_scene_tape_fence.mjs`: the required tokens re-pin by NAME
  (validation R2, gap 4b) — in `draw-frame!`, :70 re-pins to
  `frame-tape-twin-check!` and a NEW requireToken pins `compile-frame-tape`
  INSIDE `frame-tape-twin-check!` (the demoted oracle cannot be orphaned by
  a later edit); `execute-scene-tape!` (:71) stays; in `pick`,
  `scene-tape/pick-reverse` AND `containers/inverse-point` (:82–:83) re-pin
  to the maintained-walk shape; every other `requireToken` and every
  `forbid` stays — hand-positioned draw branches, `case` family dispatch,
  `sort-by` in pick, AND the legacy-layer forbid (:85); `(defn-
  compile-frame-tape` is also the END ANCHOR of the registry slice (:51) —
  if it moves, `registryEnd` moves with it (validation R1, finding 8). NEW
  assertion: no `frame-idx` token inside
  `findForm(renderer, "update-frame-arrangement")`; sink and oracle usage
  stay legal (T12). `verify_text_layout_fence.mjs`: pins
  moved per §5b; NEW assertion: the stable stage's source region contains
  neither `!caret-visible` nor `!shimmer-phase` (G5's static half lives
  here). Seeded self-tests kept in both.
- **G4 — m/signal claims test (ordering gate, end-state invariant).** §5b's
  four assertions green in `missionary_claims_test.clj` (already tiered —
  no runner edit needed for it), recorded in the artifact BEFORE the
  unification decision. **Reviewable invariant (validation R1, finding 10 —
  no commits exist to prove ordering):** the diff contains ZERO `m/signal`
  unless assertions (1) and (2) are recorded green; if either falsified,
  §S2's fallback shape is the only legal diff.
- **G5 — blink-no-relayout.** Static half (G5s): the fence assertion named
  in G3. Live half (G5L, OWNER: Sid): idle editor, caret visibly blinking,
  a dev counter on stable-stage recomputes reads ZERO over ≥10s while the
  overlay ticks. Codex ships the counter probe (dev-only, behind a flag or
  console counter) so the receipt is one browser sitting.
- **G6 — camera quarantine.** Static half (G6s): `<world-snapshot`'s
  m/latest inputs contain no `ground/!camera` watch (fence or grep-form
  assertion in the scene-tape fence, named in G3's file); JVM/world paths
  unaffected. Live half (G6L, OWNER: Sid, at the wearing sitting): pan
  still redraws (functional) + before/after pan measurement. **The receipt's
  FIRST field is the WebGPU adapter attestation** (`isFallbackAdapter` +
  description — the SwiftShader lesson). The "before" side's precondition
  is destroyed by this package (ONE-SHOT): capture it from a READ-ONLY
  WORKTREE of the pre-package commit (`git worktree add <tmp> HEAD` —
  never `checkout`/`stash` in the shared tree, which would destroy §3's
  foreign work; validation R1, finding 9) — Sid runs it at the sitting, or
  waives with the known baseline; name which in the receipt.
- **G7 — screen-space pick (JVM).** Same test ns: a `:camera :screen`
  container under non-identity pan/zoom picks correctly from the screen
  point; world containers' pick results IDENTICAL to the oracle path at
  identity camera and under pan/zoom (world semantics unchanged).
- **G8 — render goldens.** `run_verifier.mjs`: 21/21 byte-identical + the
  MSDF 47-mismatch counterexample stays RED (both are close conditions —
  RED is correct, do not "fix" it). **Environment, named (validation R1,
  finding 12):** needs `puppeteer`, Chrome at `/usr/bin/google-chrome` (or
  `RENDER_VERIFIER_CHROME`), and the built verifier bundle at
  `target/render-verifier/js/main.js` (build it first if stale). If the
  phase environment lacks Chrome, G8's OWNER FLIPS to Sid's sitting —
  record the flip in the artifact, never report the gate as green-by-skip.
- **G9 — full JVM suite.** `clj -X:test` green (`app.test-runner/full`).
- **G10 — the wearing (OWNER: Sid).** Hand-feel pan/wheel/inspector on the
  real ground. May be INHERITED from Sid's real use per the skill's
  inherited-wearing rule — harvest receipts instead of staging a duplicate
  drive; the bar is unchanged — **EXCEPT the twin-run receipt (§4b), which
  is NOT inheritable (validation R2, gap 4a — real use runs flag-off): a
  ≥1-minute flag-on run with zero logged divergences is a separate,
  mandatory line in the G10 receipt.**

## 12. Allowlist — the complete edit surface

Modified: `src/app/client/substrate/scene_tape.cljc` ·
`src/app/client/workspace/scene_store.cljc` ·
`src/app/client/workspace/scene_runtime.cljs` ·
`src/app/client/substrate/webgpu/renderer.cljs` ·
`src/app/client/workspace/runtime/render.cljs` ·
`src/app/client/workspace/runtime/mouse.cljs` ·
`src/app/client/workspace/editor_compute.cljs` (foreign +10 ADOPTED, §3) ·
`src/app/client/workspace/ground.cljs` (ONLY the `pick-at` region, §3) ·
`test/render_engine/verify_scene_tape_fence.mjs` ·
`test/render_engine/verify_text_layout_fence.mjs` (foreign +36 ADOPTED, §3) ·
`test/app/missionary_claims_test.clj` · `test/app/test_runner.clj` (tier
entry only) · `test/app/client/workspace/scene_store_test.clj` (**the
single test-file exception — EXACTLY the §4a pinned re-stamp of
`pick-layer-and-miss` :290–291, assertions untouched; validation R2, N1**).
New: `test/app/client/substrate/maintained_view_test.clj` ·
`docs/…/render-engine/SEAM-STEP1-P1.md` (artifact).
NOT touched, ever: `electric_flow.cljc` · `events.cljs` · `runtime.cljs`
(timer wiring) · `containers.cljc` · `workshop_playground.cljs` · every
other foreign file in §3. An edit needed outside this list → §S6.

## 13. Input manifest (substance binds; hints grep-verified 2026-08-03)

- `scene_store.cljc`: `build-slot` :69 (`:stack-path (or stack-path [])`
  :87 — the stampless fact) · `upsert-slot` :135 · `remove-slot` :161 ·
  `update-nodes-by-address` :177 (funnels through upsert :194) ·
  `slot-entry` :213 · `scene-tape` :240 · `ordered-slots` :257 (dead —
  zero callers) · `pick` :265 (direct `ss/pick` call :271–273; second
  direct caller inside `context-bundle` :459) · `context-bundle` :445 ·
  `store-fns-free?` :527.
- `scene_tape.cljc`: `compare-scalar` :393 · `compare-seq` :399 ·
  `compare-order` :427 · `validate-entry!` :439 (all `defn-` today) ·
  `compile-tape` :469 · `paint-forward` :493 · `pick-reverse` :502.
- `scene_runtime.cljs`: `!scene-store` :33 · registration stamps the real
  path from `ctn/effective` :153–159 · `pick-world` :304 (direct `ss/pick`
  call :312) · `bundle-for-viewport` :328 · `bundle-at-world-point` :341 ·
  `<store-frame` :384–416.
- `renderer.cljs`: `position-text-op` :1452 (rescan :1476) · `frame-order`
  :1904 · `frame-entry` :1916 · `store-pool-entries` :1948 · `system-entry`
  :1973 · `clip-entries` :1988 · `shadow-entries` :2007 · `rect-entries`
  :2021 · `frame-family-registry` :2151 · `compile-frame-tape` :2185 ·
  `execute-scene-tape!` :2194 · `draw-frame!` :2207.
- `render.cljs`: camera watch :187 (`;; first-light P2b`) · RAF `m/reduce`
  :220 · `camera-moved?` :556.
- `editor_compute.cljs`: `<editor-rects+sidebar` :414 · flows §5a ·
  mode-guard :699–700 (foreign, adopted).
- `containers.cljc`: `add-container` :86 · `set-transform` :143 ·
  `effective` :221 (`:flags` :236) · `screen-bounds` :284 — READ-ONLY.
- `ground.cljs`: `!camera` :87 · `screen->world` :169 · `pick-at` :3270.
- `runtime/mouse.cljs`: pick site :460.
- `electric_flow.cljc`: `:source-revision` :441 · `:layout-line-id` :465 —
  READ-ONLY. `text_layout.cljc`: revision stored at `[:source :revision]`
  (:222, :470) · `clip-result` scan :669 (named-deferred) — READ-ONLY.
- Committed pick call sites that must STAY GREEN UNTOUCHED (the
  backward-compat law's ground): `scene_store_test.clj`
  :59/:60/:297/:310/:327/:416/:417 · `scene_tape_test.clj` :126 (stamped
  fixture — stays green under the ruling, R2-traced). NOT on this list:
  :293–295 — the one stampless-and-overlapping fixture RULING R1 flips; it
  is re-stamped per §4a's pinned repair, not re-expected.
- Fences: `verify_scene_tape_fence.mjs` (`registryEnd` anchor :51 ·
  required tokens :70–:71 and :82–:83 · forbids incl. legacy-layer :84–:85)
  · `verify_text_layout_fence.mjs` (`auditDormantEditorBranch` :84–:87,
  seeded branch :130) · `run_verifier.mjs` (puppeteer/Chrome/bundle
  preconditions :12, :29–:35).
- Harness: `test_runner.clj` `pure-namespaces` :31 ·
  `app.missionary-claims-test` already tiered :43. Idiom sources:
  `scene_store_test.clj`, `scene_tape_test.clj`.
- Reference: `docs/reference/missionary-reference.txt` :899 (m/signal) ·
  electric-docs skill (laws L4/L6/L8/L12).

## 14. Stop clauses — classify first (implementer-fixable vs policy fork)

- **§S1** Act-2 foreign-hunk staging with §3 not reading ADOPT → STOP.
- **§S2** m/signal claims test falsifies sharing on b.46 → NOT a stop:
  pre-ruled fallback (split without signal, defer unification, record).
- **§S3** G1's maintained ≡ 1-arg-batch equivalence CANNOT go green on a
  real case → the total-order fact or the write-funnel fact is broken →
  STOP (contract wrong under platform semantics); bring the failing case
  verbatim. (The stampless stamped-vs-refreshed divergence is NOT this — it
  is pre-ruled by RULING R1; only G1 probe (b) failing escalates, via §S4.)
- **§S4** Genuine binding-doc conflict (two readings, both physically
  implementable) → decisions.md Open Questions with citations + options +
  recommendation → STOP. Never improvise policy.
- **§S5** Manifest SUBSTANCE missing (named symbol/form/semantics absent) →
  STOP. Hint drift with substance intact → re-locate + log, continue.
- **§S6** Any edit needed outside §12's allowlist → STOP, name the file and
  why.

On any STOP: write the finding verbatim in the NOW entry and the phase
artifact, leave the tree runnable, end the session.

## 15. Codex opening prompt (Sid pastes this into a fresh Codex session)

    You are the implementer under contract for Softland work package
    SEAM-STEP1. Repo: this working tree. Your BINDING documents, read in
    this order before any code:
    1. docs/current-mental-model/build/render-engine/SEAM-STEP1-CONTRACT.md
       (the contract — binding, plan-grade; traps T1–T12 cited by number
       in code comments; gates G1–G10; allowlist §12; stop clauses §14)
    2. docs/current-mental-model/build/render-engine/SEAM-STEP1-NOW.md
       (STANDING — frozen; append your NOW entry at session end)
    3. docs/current-mental-model/decisions.md, section "The render seam"
       only (the constitution this package enacts)
    Implement the ONE phase: Act 1 → Act 2 → Act 3 in order, gates
    interleaved — G4 (m/signal claims test) green BEFORE any unification
    relies on m/signal. Run your in-phase gates to green in this session:
    G1 G2 G3 G4 G5s G6s G7 G8 G9 (G8: if this environment lacks Chrome,
    record the owner-flip, never green-by-skip).
    Hard rules: edit ONLY §12's allowlist (one pinned test-fixture
    exception, §4a) · foreign uncommitted files per §3 — two adopted, one
    scalpel (`ground.cljs` pick-at only), rest untouchable · NO git
    commits, NO pushes · never read src/app/server/env.clj · fences: pins
    move, never deleted; seeded self-tests stay.
    On any §14 stop: record verbatim in NOW + the phase artifact, leave
    the tree runnable, end the session.
    Close: phase artifact SEAM-STEP1-P1.md (per §10 — including the
    diff-derived changed-file list sum-checked against §12, the flagged
    sites, the prediction-check paragraph) + a ≤15-line NOW entry. If a
    gate does not go green first run, that is contract signal — record it
    verbatim.
