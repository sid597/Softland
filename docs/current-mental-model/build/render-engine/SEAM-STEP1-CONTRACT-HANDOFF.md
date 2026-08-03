# SEAM-STEP1 — contract-session handoff (reconnaissance banked, register corrected)

Written 2026-08-03 at ~220k context by the session that booted
`SEAM-STEP1.md`. Everything below was READ AND VERIFIED against the live
tree this day; line numbers are navigation hints (substance binds —
re-verify any locator you quote with `grep -n` at staging, never
hand-count).

## Register — the mistake this session made, do not repeat it

**The deliverable is the CONTRACT, not the implementation.** Sid's ruling,
verbatim intent: "the goal of this session is to only write up the
contract work or fable class work no implementation that is to be done by
codex." The SEAM-STEP1.md starter's "Recommended: Fable implements
directly" is OVERRIDDEN by Sid routing to an implementer-under-contract —
the starter itself reserved that option, and it requires a contract first
(work-package skill). This session drifted into implementation mode
(created impl tasks, read toward writing code) until Sid caught it.
Fresh session: load the work-package skill, author the contract, write
NOTHING under `src/` or `test/`.

## What the next session produces

1. `docs/current-mental-model/build/render-engine/SEAM-STEP1-CONTRACT.md`
   — one-phase package (2026-07-29 few-and-large ruling; no PLAN.md),
   implementer = Codex, plan-grade specificity carried by the contract.
2. STANDING/NOW baton + one board line (engine block, `next-prompt.md`).
3. ONE fresh-context default-fail validation round over the contract.
4. Paste-able Codex opening prompt for Sid.
5. Write-set preview in one message before landing; docs commit on
   `docs/current-mental-model-local` at settlement; NEVER push. Consult
   memories `feedback-parallel-sessions-shared-branch-git` +
   `feedback-commits-one-branch-docs-local` before any git write.

Boot list (small, exact): CLAUDE.md · decisions.md "The render seam"
(~:340–497) · `SEAM-STEP1.md` · THIS file · work-package skill ·
electric-docs skill. Do NOT re-run the wide source sweep — the pins below
are the yield of it.

## Tree state — foreign uncommitted work (the #1 hazard)

Sibling (Package 2 / studio) work sits UNCOMMITTED in the tree. Never
sweep into any commit: `ground.cljs` (+819 — studio/workshop/anatomy),
`facet_master.clj`, `server_jetty.clj`, `anatomy_test.clj`,
`material_truth_test.clj`, untracked `studio.cljc`,
`workshop_playground.cljs`, `image_material.cljc`,
`shared/studio.cljc`, `shared/workshop_playground.cljc`.

**COLLISION:** two foreign-modified files sit EXACTLY on Act 2's surface:
- `editor_compute.cljs` (+10): a mode-guard added to `<editor-content` —
  `(not (contains? #{:editor :file-workspace} mode)) → empty-face-content`
  — i.e. the DEGENERATE form of Act 2's split, already in-tree.
- `test/render_engine/verify_text_layout_fence.mjs` (+36):
  `auditDormantEditorBranch` pinning literal anchors
  `"        <editor-content"`, `"        ;; 18 fn args"`, and the guard
  string, plus a seeded self-test.
The contract MUST carry an adopt-or-stop staging clause. Recommendation
(flag for Sid, one-line veto): this package ADOPTS those two files' hunks
(same arc — Act 2 subsumes the guard), never any other foreign hunk.

## Act 1 pins — maintained ordered view

Store (`src/app/client/workspace/scene_store.cljc`): value
`{:slots {vi→slot} :index {address→#{vi}}}`. `upsert-slot` :135 (inherits
`:container-slot`/`:stack-path` from old slot when omitted),
`remove-slot` :161, `slot-entry` :213 — entry shape: `:entry/id
[:scene-slot vi]`, family `:render.family/rect`, `:order {:stratum
:pass-class :direct :stack-path <stamped> :part-rank 0 :stable-tie vi}`,
`:runtime/slot` = the whole slot. `scene-tape` :240 (1-arg = paint
projection over STAMPED paths; 2-arg = pick form, refreshes paths from
current effective map). `pick` :265 — compiles the full tape EVERY pick
(the named disease). `ordered-slots` :257. `store-fns-free?` :527.

Tape (`src/app/client/substrate/scene_tape.cljc`): `compare-scalar` :393
(pr-str fallback), `compare-seq` :399 (element-wise, missing = less),
`compare-order` :427 (stratum→pass-class→stack-path→part-rank→stable-tie),
`validate-entry!` :439 (PRIVATE), `compile-tape` :469 (validates + sorts;
world-revision baked into the tape value), `paint-forward` :493,
`pick-reverse` :502 (filters `:visibility :visible?` + `:pick ≠ :none`).

**Total-order fact:** `:stable-tie` = vi and `:entry/id` = `[:scene-slot
vi]` → no two distinct entries tie → a sorted map keyed
`[order-key entry-id]` is deterministic AND agrees with the batch stable
sort. Equivalence is exact, not approximate.

**Comparator trap (would have cost the implementer a day):** Clojure's
default `compare` on vectors is LENGTH-FIRST — `(compare [2] [1 1])` < 0.
That mis-orders stack-paths vs `compare-seq` semantics. The sorted-map
comparator MUST reuse scene-tape's own compare fns (export them); never
default `compare` on a composite key.

**Registry-immutability fact (settles the keying question):**
`containers.cljc` `set-transform` :143 mutates ONLY `:affine`;
`:layer`/`:sibling-rank`/`:parent` are fixed at `add-container` :86 and no
API mutates them → stamped stack-paths can NEVER go stale today → keying
the maintained view on STAMPED order tokens is exactly equivalent to the
2-arg refreshed batch. The equivalence fence is what catches any future
re-layer API.

Runtime (`src/app/client/workspace/scene_runtime.cljs`): `!scene-store`
:33. `<store-frame` :384–416 — ONE m/latest over `(m/watch !scene-store)`,
calls `ss/scene-tape` per store change, derives `{:rects :shadows
:text-by-vi :ordered-vis :ops-count-by-vi :order-by-vi :scene-tape}`.
Consumers of those keys: renderer `store-pool-entries` :1948–1971 (walks
`:ordered-vis`/`:ops-count-by-vi`/`:order-by-vi`), render.cljs :677–685
(`:extra-text-geos`). The `:scene-tape` key has NO consumer outside
`<store-frame` (grep-verified) — free to drop or repoint at the oracle.

Renderer frame path (`src/app/client/substrate/webgpu/renderer.cljs`):
`frame-order` :1904 (static rank constants under `[[:frame/root rank
rank]]`), `frame-entry` :1916, producers `clip-entries` :1988,
`shadow-entries` :2007, `rect-entries` :2021, `system-entry` :1973,
`frame-family-registry` :2151, `compile-frame-tape` :2185 — compiles +
validates + sorts EVERY RAF with world-revision `[:frame (:frame-idx
frame)]` (the frame-counter-as-ancestor crime), `execute-scene-tape!`
:2194, `draw-frame!` :2207 (frame-idx also drives ≤5-frame boot logs —
that is a legal sink read and STAYS).

Render consumer (`src/app/client/workspace/runtime/render.cljs`):
`render-consumer` :66, `<world-snapshot` :134–187, RAF `m/reduce` :220
with `identical?` world skip, draw call :638.

Design already derived (lift into the contract):
- Store gains `:ordered` (persistent sorted map `[order-key entry-id]` →
  entry) beside `:slots`, patched in `upsert-slot`/`remove-slot`; old key
  derived from the old slot's entry (no extra index needed store-side).
- `validate-entry!` becomes public and runs at the upsert edge;
  `compile-tape` keeps its own validation BYTE-IDENTICAL (growth law: the
  oracle is demoted, never weakened).
- `<store-frame` walks `(vals (:ordered store))` forward — no compile.
  `ss/pick` walks `(rseq (:ordered store))` — no compile. Same
  visible/pickable filters as `pick-reverse`.
- Frame side: the per-RAF `compile-frame-tape` leaves `draw-frame!`; a
  maintained frame arrangement (entries diffed by entry-id + order token,
  validated on insert) lives as renderer-owned frame-edge state; paint
  payloads rebind per frame, ORDER re-derives only when the entry set
  changes. `compile-frame-tape` survives as the oracle behind the fence.
- Placement ruling (with rejected alternative): the maintained-order
  structure (comparator export + ordered-view insert/remove +
  validate-at-insert) lives in `scene_tape.cljc` — it owns the order
  vocabulary; a new namespace would fork it. Store `:ordered` in
  `scene_store.cljc`; frame arrangement atom in `renderer.cljs`.
- EDN round-trip trap: pr-str/read-string silently degrades a sorted map
  to a hash map. `:ordered` is a maintained projection — name the ONE
  legal rebuild door for any rehydration path.
- `store-fns-free?` walk is unaffected (comparator is
  collection-internal, not in keys/vals/meta).

## Act 2 pins — editor flow split

`src/app/client/workspace/editor_compute.cljs`, fn
`<editor-rects+sidebar` :422: `<layout` :432 (5 watches), `<mode` :463,
`<sidebar` :479 (SIDE-EFFECTS in combine: resets `!sidebar-scene`, cache
atom `!last-sidebar-struct`), `<trail-face` :530 (resets
`!trail-face-scene`, `!trail-prev-carry`), `<face-main` :604,
`<intake-content` :626, `<run-content` :645 (watches `!shimmer-phase`),
`<editor-content` :665 — ONE 18-input m/latest including `!caret-visible`
+ `!shimmer-phase`; in `:editor`/`:file-workspace` modes every tick
re-runs `compute-editor-rects` (FULL proportional layout since T1) — the
constitution's "caret blink re-shaping a document it never touched".
Return combine :753–772. Ticker: `events.cljs` `make-blink-timer` :192
(530ms), wired `runtime.cljs` :399–400 (`>blink-timer`,
`>shimmer-timer`).

Forked copies: `<layout` is consumed by ~7 sibling combines; per
Missionary semantics each `m/latest` use is a SEPARATE subscription = a
separate process — these are the hand-rolled sharing points the
constitution names.

Rescan consumers (fenced-view instance #2 — BEGUN here, not finished):
`electric_flow.cljc` :441 stamps `:source-revision
(hash [source-lines folded-lines])` into the layout-result; :465 stamps
`:layout-line-id (:line/id line)`. `renderer.cljs` `position-text-op`
:1452, rescan at :1474 — `(first (filter #(= (:layout-line-id txt)
(:line/id %)) (:lines layout-result)))` — a LINEAR SCAN per text op.
Upgrade = keyed lookup consuming `:line/id` + `:source-revision`.

Verified laws to cite (electric-docs skill, regression-tested build -45):
- **L6** coarse invalidation — grounds the split.
- **L8** raw m/latest diamonds GLITCH deterministically — the split MUST
  be a CHAIN (stable doc-derived flow → overlay combine adding ticker
  atoms), never two doc-derived branches merged downstream. Stable stage
  computes caret geometry; the overlay stage only toggles its
  presence/alpha.
- **L4** m/latest is demand-driven — no side effects in combines (note
  `<sidebar`/`<trail-face` already violate in spirit; signal-wrapping
  them changes lifecycle — see the m/signal gate).
- **L12** the genuine shared-process/multicast property belongs to
  `m/signal`/`m/stream` — and there is NO claims test for m/signal on the
  pinned build. Missionary is b.46, TRANSITIVE AND UNPINNED.
- m/signal reference: `docs/reference/missionary-reference.txt` :899 —
  publisher, latest-value, default sg `{}` discards all but latest.

**CONTRACT GATE (non-negotiable ordering):** a new claims test in
`test/app/missionary_claims_test.clj` proving m/signal semantics (ONE
upstream process shared across N subscribers; late-subscribe sees latest;
zero-subscriber lifecycle → what restarts; behavior of side-effecting
combines under signal) lands and goes green BEFORE any unification relies
on m/signal. Fallback if falsified on this build: split the tickers
WITHOUT signal — the split does not depend on it.

Fence-pin duty: the split changes `<editor-content`'s arity (the
`;; 18 fn args` anchor WILL move) and may relocate the mode guard — move
`verify_text_layout_fence.mjs` pins with it, never delete the fence.

## Act 3 pins — camera quarantine + :screen pick

- `ground.cljs` `!camera` :87 (shader law: screen = world·zoom + pan),
  `screen->world` :169, `pick-at` :3270 → `scene-rt/pick-world` :304 →
  `ss/pick` (compiles per pick). Second pick site: `mouse.cljs` :460.
- `render.cljs` :187 — `(m/watch ground/!camera)` INSIDE
  `<world-snapshot`'s m/latest: every pan tick rebuilds the snapshot map
  and re-fires the combine. Quarantine = camera leaves the m/latest
  inputs, becomes a deref at the RAF sink with its own prev-compare
  (`camera-moved?` full-clear logic :556–557 already exists and stays).
  CARE: with camera out of the snapshot, a pan tick leaves world
  `identical?` — the RAF body must still draw on camera change (sink
  compare, not world identity).
- The camera→registry edge (what must lose its reason to exist):
  screen-pinned furniture placed by `screen->world` at one camera moment
  + `set-transform!` — error card :1093/:1110, binding lint :3558/:3574,
  studio birth-handle :2304–2308 (1/zoom scale-cancel hack; its comment
  states the pick limitation verbatim); `workshop_playground.cljs` :1416
  `add-watch !camera ::wsp-door` re-places the door per camera tick.
- The honest fix: `containers.cljc` `effective` :221 already emits
  `:flags` 1 for `:camera :screen` (:236) and `screen-bounds` :284
  branches on it — but `ss/pick` does NOT: callers pre-convert
  screen→world for ALL containers, so screen containers mis-pick under
  pan/zoom. Fix: camera-aware pick (per-entry: world containers get the
  world point, screen containers get the screen point). Call sites to
  update, enumerated: `scene_runtime` `pick-world` :304,
  `bundle-for-viewport` :328, `bundle-at-world-point` :341,
  `ss/context-bundle` :445, `mouse.cljs` :460, `ground.cljs` `pick-at`
  :3270.
- SCOPE: the pick CAPABILITY lands in this package; MIGRATING the foreign
  furniture (studio handle, workshop door — uncommitted sibling code) does
  NOT. Flag, never commit.
- Pan measurement gate: any perf receipt opens with the WebGPU adapter
  attestation (skill rule — the SwiftShader lesson).

## Fences — pins move, never deleted (both committed-fence edits are ours to make)

- `verify_scene_tape_fence.mjs` (COMMITTED, clean): currently REQUIRES
  the literal token `compile-frame-tape` inside `draw-frame!` and
  `scene-tape/pick-reverse` inside `ss/pick`; forbids hand-positioned
  draw branches, `case` family dispatch, `sort-by` in pick. Act 1
  relocates both required tokens → the fence's pins move to the new
  maintained-walk names, every forbid assertion stays, and it grows a
  new assertion: no `:frame-idx` reaching tape derivation. Keep the
  seeded self-test pattern.
- `verify_text_layout_fence.mjs` (FOREIGN-MODIFIED — see collision
  clause above).
- The NEW equivalence fence (maintained ≡ batch) is a NEW instrument —
  JVM test, not an extension of either static fence: drive scripted keyed
  mutations (upsert/remove/update-nodes-by-address/set-transform),
  assert `(vec (vals (:ordered store)))` ≡ `(:entries (ss/scene-tape
  store))` after every step, pick-maintained ≡ pick-oracle at probe
  points, and validation-throws at the upsert edge. Frame-side twin over
  synthetic frame entries incl. visibility toggles vs `compile-frame-tape`.

## Test-harness facts

- `clj -X:test` → `app.test-runner/full`; classification is FAIL-CLOSED —
  a new test namespace REQUIRES an explicit tier entry in
  `test_runner.clj` (`pure-namespaces` for JVM-pure; the new fence test
  and the claims-test addition both qualify).
- Idiom sources: `test/app/client/workspace/scene_store_test.clj`,
  `test/app/client/substrate/scene_tape_test.clj`.
- Render fences run via node: `test/render_engine/*.mjs`;
  `run_verifier.mjs` = goldens 21/21 byte-identical + MSDF 47
  counterexample stays RED (both are close conditions).

## Traps ledger — drafted, ready to lift

T1 default-compare-on-composite-key (length-first) → reuse scene-tape
compare fns. T2 keying on refreshed-not-stamped paths → stamped is exact
(registry immutability); fence catches future drift. T3 EDN round-trip
silently drops sortedness → one legal rebuild door. T4 split-as-diamond →
L8 glitch → CHAIN shape only. T5 m/signal assumed → claims test first;
side-effecting combines under signal lifecycle asserted. T6 moving (not
copying) validation out of compile-tape → oracle weakened → ADD at edge,
oracle byte-identical. T7 fence deletion instinct → pins move, assertions
stay. T8 frame-view state placement → renderer-owned frame-edge atom.
T9 pick signature ripple → all six call sites enumerated above.
T10 ops-array identity (`identical?` skips downstream: `content-same?`,
pool diffs, G8 slot-geo skip) must survive the `:ordered` walk — untouched
slots keep the SAME ops arrays. T11 foreign-tree commit discipline +
adopt-or-stop clause. T12 frame-idx stays legal at the sink (boot logs,
loop control) — only its ancestry into derivation is illegal.

## Gates — sketched, renumber in the contract

G1 store equivalence fence (JVM, new ns, tiered into test_runner).
G2 frame-arrangement equivalence vs compile-frame-tape oracle.
G3 both .mjs fences green with MOVED pins (+ new frame-idx assertion).
G4 m/signal claims test green BEFORE unification (ordering gate).
G5 blink-no-relayout: static pin (stable stage must not watch the ticker
atoms) + live receipt (idle editor, caret blinking, zero stable-layout
rebuilds over N seconds).
G6 pan measurement before/after with adapter attestation; camera absent
from `<world-snapshot` inputs (static pin) + pan still redraws (live).
G7 :screen-space pick JVM test (screen container under pan/zoom picks
correctly; world containers byte-identical at identity camera).
G8 goldens 21/21 + MSDF 47 RED (run_verifier.mjs).
G9 full JVM suite green (`clj -X:test`).
G10 wearing: hand-feel pan/wheel/inspector on the real ground — OWNER:
Sid's headed browser (named per the gate-partition rule).
Sum-check the partition against this list when the phase prompt is cut.

## Shape of the package

ONE phase (Codex, fresh context) in the starter's act order — Act 1 →
Act 2 → Act 3, gates interleaved (G4 before Act 2's unification). ONE
falsifier aimed at the genuinely-new machinery (maintained view +
signal split). FULL gate review (Fable) — the frame path is
cutover-class, not additive-and-dark. Stop clauses: (a) the Act-2
foreign-hunk collision if Sid hasn't ruled adopt; (b) m/signal claims
test falsifies sharing on this build → fall back to ticker split without
signal, record; (c) genuine binding-doc conflict → decisions.md Open
Questions with options + recommendation.

At close, check the pre-registered prediction (decisions.md render seam,
last bullet): the plumbing dialects converge toward two-plus-transport —
report what was observed, cheap to falsify at the store's first version.

## Session-record note

This session made NO commits and wrote nothing to disk except this file.
The board/Vision line was not touched. No LOG entries were captured
(nothing vision-verbatim occurred beyond the register correction quoted
above).
