# smalltalk-ui-vm — the noun becomes material: anatomy, interpreter, Workshop

Cut 2026-07-30 in the workshop-realization session (Sid: "lets write down
what all we discussed … goooo write out"), from the same-day DIRECTION
(SETTLED, this directory) and `vision/LOG.md` 2026-07-30. Every manifest
locator below was machine-verified on disk this session by four fresh
terrain sweeps against HEAD `25ce803` (code tree `db8e21d`); the terrain
answered the DIRECTION's contract-time opens — see §0.

**TWO implementation phases, by Sid's standing phases-low ruling**
(2026-07-29: phases FEW and LARGE; the implementer lane takes a
well-fenced package whole). This package is CUTOVER-CLASS (a hand-written
composer dies), so it takes two large phases, not one: P1 the engine
cutover (FULL gate tier), P2 the Workshop surface + the remaining
pressures (slim tier). The contract carries plan-grade specificity;
there is no PLAN.md. Cadence per phase: fresh implementer context builds
the phase whole → one fresh falsifier in-phase → fresh gate → Sid's
commit ruling. Before P1: ONE fresh default-fail validation round over
this document (V1–V6, §11).

## §0 Terrain rulings — the DIRECTION's opens, closed at cut time

- **The interpreter already exists.** `face_assembly/compile-assembly`
  (`face_assembly.cljc:330` — wear-time, pure, registry-validated, total
  via error-card) + `apply-assembly` (`:526`) + the primitives registry
  (`face_primitives.cljc:934`). The ground block was never cut over to
  it. This package makes the block the assembly machinery's next WEARER
  (the parent DIRECTION's second-wearer falsifier, applied): additive
  reuse of compile/apply/registry conventions, extended with the block's
  missing primitives — never a second parallel interpreter.
- **Anatomy has a home; no new module.** The OC facet-master adapter
  (`object_container/facet_master.clj`) is generic over the spec; the
  only non-generic surfaces are FOUR (validation F3):
  `facet_masters.cljc:14` (specs), `ground.cljs:206`
  (`resolve-material-wears`, hand-written), `cluster.clj:330`
  (bootstrap), and `ground.cljs:347` (`block-wear-census`, the halo's
  hardcoded six-facet vector — gains `:anatomy`; its pinned test
  literal `material_portal_test.clj:1191-1193` re-cut in the same
  change). The parked material-kernel platform
  check is answered by the P6 precedent (CONTRACT_P6.md:39-52 — every
  homeless-truth candidate resolved to OC; the horizon stays open).
- **The loop needs ZERO new HTTP endpoints and ZERO new verbs.** Edit =
  a master-candidate import through the existing
  `/api/matter-room/deviate` master branch (`server_jetty.clj:1357` →
  `facet-master/import-candidate!`); preview = the existing client
  membrane (`ground.cljs:3254` previews exactly shared-master
  candidates); activate/rollback = the existing routes. The disclosed
  act-endpoint set stays FOUR (`material_portal_test.clj:1123-1137`
  unchanged); the registry↔ground equality set is unchanged.
- **The Workshop's door is the halo.** A new master in `specs`
  automatically gets a room (`matter_room.cljc:57` derives
  `registered-master-ids` from `facet-masters/master-ids`) and joins the
  serve (`face_projection.clj:995` iterates specs). Once blocks wear
  `:anatomy`, the halo census lists it and **enter** lands in the
  anatomy room.
- **Preview scope (DIRECTION's open) ruled:** type-preview IS
  master-candidate preview on the live ground through the membrane —
  plus a specimen block in the room rendered by the same interpreter.
  One mechanism, no second renderer.

## §1 Purpose

The block type's policy surface is material; its STRUCTURE exists only
as `block-tree` (`ground.cljs:599`) — a hand-written composer. This
package makes the noun material: the block's part composition becomes a
revisioned facet-master (`fm:anatomy`) interpreted by the existing
assembly machinery, `block-tree` dies, and the anatomy room becomes the
Workshop — the place where Sid (or an agent) stands in front of how the
block is made and evolves it through the six-arrow loop: anatomy exists
as material → open it → see its composition → edit
parts/nesting/bindings/defaults → render the candidate through the real
interpreter → activate or reverse. Acceptance is the lived pressure
triple: thread structure (part birth) · paste-clamp (policy birth) ·
visible ctrl+enter invocation settings (master birth). After this
package, a structural change to the block is a material diff, never a
code change — the boundary map shows honestly what remains code.

## §2 Consumers, in order

1. **Sid's hand** — the Workshop unblocks the wall he named ("the thing
   i am missing is a general workshop"); his three concrete changes are
   the acceptance triple.
2. **Agents** — the same edit lane through `__portal` (deviate hand,
   same request shape); anatomy legibility is a gate, not a hope (G8).
3. **The next component type** — the first recurrence generalizes the
   family; this package's conventions are what it will inherit or recut
   (second-wearer law).
4. **C1 in BETS.md** — the escape gauge's structure-stratum reading; the
   armed matter-room §8 falsifier remains C1's instrument, untouched.

## §3 The laws this package builds (W1–W9)

- **W1 — THE NOUN.** `fm:anatomy` is a facet-master (facet `:anatomy`),
  worn by every block at shared tier; per-instance deviation is legal
  (a block wearing deviant structure — cheap, scoped, visible,
  reversible, per the aliveness laws). Its form is FLAT PART ROWS with
  closed vocabularies: each part = `{:part/id kw, :part/prim kw (a
  face_primitives registry keyword), :part/when kw (closed presence
  vocab: :always :machine :user :header :focused :hover :has-selection
  :has-refusal :has-notice :boundary :has-group-sel :has-gold-marks
  :has-silver-marks :has-conflicts — the full census is block-tree's
  own, enumerated in VALIDATION.md; evaluated at APPLY time, since
  presence is instance data and compile stays per (master, revision)),
  :part/props {slot → literal | [:wear facet key] | [:view key]},
  :part/order n, :part/stamp {facet site role slot}}` plus
  `:anatomy/defs {name → [part…]}` for named sub-compositions and parts
  of prim `:sub-anatomy` referencing a def by name (one level of
  reference; a def may not reference itself or another def — depth is
  bounded by construction). DATA-RESOLUTION is fixed in the same
  breath, for BOTH bind forms (validation F1): `[:wear facet key]`
  resolves against the subject's `wears-for` at apply time; `[:view
  key]` resolves against the block's derived view-model (the
  rebuild-block! census: text/lines · caret · selection · msel ·
  machine? · hover? · notice · refusal · headers · wrap-col · boundary?
  · gsel? · gold/silver marks · metrics) at apply time, with the CLOSED
  view-key vocabulary owned by `anatomy_material.cljc` beside the
  grammar — never a static code map beside the schema, and never an
  open bind path (the assembly grammar's `{:bind [path]}` stays an
  engine internal the anatomy compiler targets; anatomy rows carry only
  the two closed forms). The grammar validator lives in a new
  `shared/anatomy_material.cljc` following `facet_material.cljc`
  compile-form conventions (accumulated namespaced errors, offending
  value + legal vocabulary in every refusal — refusals TEACH). No new
  module, depot, PState, topology, or import family: rows ride
  `imp:fm:` through the existing adapter; the eight-owner import census
  is unchanged.
- **W2 — THE INTERPRETER, second-wearer.** The block renders through
  `compile-assembly` → `apply-assembly` conventions: compile at
  wear/activation time (per (master, revision) — cached), apply per
  instance data change. The block's missing leaves land as NEW
  REGISTERED PRIMITIVES cut from `block-tree`'s own code (the
  char-grid body with wrap/headers/per-line color policy · selection
  wash · caret · provenance rail · attention box · fold-header
  hit-areas · refusal/notice/boundary/mark lines · conflict lint ·
  contribution stamps). Anatomy references primitives by KEYWORD only —
  material never names code. Totality: a malformed or missing anatomy
  falls to the CODE FLOOR, which is the seed form held as a code
  constant fed to the SAME interpreter — one truth, floored form (the
  resolved-wear law, `facet_material.cljc:175`, applied to structure).
  The interpreter never branches on component identity — content varies
  by data, never by dispatch into per-component code.
- **W3 — SEED, BYTE-IDENTITY, HARD CUTOVER.** The seed anatomy is the
  transcription of `block-tree`. The cutover gate ENUMERATES the real
  corpus (every block the live land renders, plus the committed fixture
  set) and RENDERS-AND-COMPARES each: resolved tree from the
  interpreter == resolved tree from `block-tree`, structurally exact
  (ops, bounds, styles, node ids, claims, stamps, and every
  block-tree-authored data key). The EQUALITY RELATION is named
  (validation F2): the assembly engine stamps its OWN provenance onto
  every tree it applies — `:assembly/src-path` per node; the root's
  `:view-instance` `:address` `:assembly/content-h`
  `:assembly/apply-report` (`face_assembly.cljc:395-412,511-524`) —
  that exact key set is the EXPECTED delta, normalized out by the
  comparator (and asserted present separately); everything else is
  exact, and primitives emit block-tree's exact node ids (builders
  choose their own ids — the standing convention). Removing those
  stamps to make a naive compare pass is a T2 fail, never a fix. Then
  `block-tree` and its private helpers ARE DELETED in the same phase —
  no flag, no fallback, no second truth (deletion scope: the executable
  criterion is the grep-negative below; helpers with other callers —
  `wrap-lines`, `text-op`, `run-view`, `lines-offset` — survive where
  still used, and the body primitive's wrap truth is block-tree's OWN
  `wrap-lines` moved to `.cljc`, shared with the copy path — never
  `rt/wrap-line`, a different algorithm). ONE-SHOT law: the both-paths-exist precondition dies
  at the package's own close, so the COMMITTED harness asserts the
  post-invariant — interpreter output vs committed golden trees
  (fixtures generated from `block-tree` before deletion, banked in the
  phase receipt) — while the live-corpus comparison receipt is banked
  at phase time. KEYING (named source): the worn anatomy identity rides
  `wears` into the existing 16-element `:render-sig`
  (`ground.cljs:1208-1210`) — activation flips the served master, the
  epoch push refreshes `!facet-materials`, the existing watcher
  rebuilds; the compile cache keys on (master-id, revision-id), a
  slow-changing identity minted only by activation. No new trigger
  machinery.
- **W4 — THE LOOP ON EXISTING LANES.** Deviate (master-candidate
  branch) · membrane preview · activate · rollback — the four existing
  endpoints, unchanged; zero new registry verbs; zero new gesture rows;
  zero new interaction-table rows. The halo lists `:anatomy` like any
  worn master (census = the hardcoded `block-wear-census` vector,
  `ground.cljs:347`, which gains `:anatomy` — validation F3; the wear
  itself then resolves per subject through `wears-for`); **enter** is
  the Workshop door. Grammar, kernel, and dispatch stay byte-frozen at their pinned
  surfaces.
- **W5 — THE COMPOSITION PROJECTION (see-composition).** A new portal
  SECTION on the anatomy room riding the existing `open`/sections
  machinery (`material_portal.clj:671`, sections `:741-860`) and the
  existing pure composition/recipe helpers
  (`material_portal.cljc:371-401`): the part rows, each labeled with
  its STRATUM — green (material: the row itself, its props, worn
  values) · amber (Softland code: the primitive it names, with
  src-path) · red (floor) — the standing boundary surface, seeded from
  the halo's "code-owned · floored" vocabulary. Client-side, the room
  renders the projection beside a live SPECIMEN block drawn by the same
  interpreter; picking a part row highlights its pixels on the specimen
  and vice versa (why-this-pixel inverse), riding scene-descriptor
  actions (`register-action!`, the halo-card pattern) — no new verbs.
  The 17-question floor is untouched (the section is a section, not a
  question).
- **W6 — EDIT-TO-CANDIDATE.** Part edits (add · remove · reorder ·
  retune props · rebind a slot · attach/detach a sub-anatomy def)
  compose ONE candidate form client-side; the candidate flows: preview
  (membrane) → deviate (master-candidate import, revision-id knowable
  pre-append) → activate (scoped). Human hands: room widgets (the
  block_edit machinery for text-valued edits where warranted). Agent
  hands: the SAME composition through `__portal` — agent parity is the
  same lane, not a parallel one. Every malformed candidate is refused
  with a teaching card (W1's validator) before any append.
- **W7 — THE PRESSURE TRIPLE.** Each is a material diff + an acceptance
  test + AT MOST one named code consult (blue born once):
  - **threaded first pixels (P1, part birth, ZERO consults):** a
    candidate revision adds thread-edge parts (edge rail · indent)
    bound to new `fm:threaded` keys (grammar migration via the
    existing `ensure-active-source!` lane, `facet_master.clj:374`);
    driven through the console lane (deviate → preview → activate)
    BEFORE the Workshop surface exists — P1's proof that a part birth
    is a material diff.
  - **paste-clamp (P2, policy birth, ONE consult):** paste from outside
    over the policy threshold arrives clamped to the policy's
    max-share with an expandable fold header carrying the source mark;
    policy keys land on `fm:foldable` (grammar migration); the ONE
    consult is born at the paste path (`handle-content-key!` `:paste`,
    `ground.cljs:2129`; `ground_edit.cljc:67-71` inserts verbatim
    today) — thereafter clamp fraction, threshold, and header copy are
    material forever.
  - **fm:invocation (P2, master birth, consults at the send seam):** a
    NEW master carrying `{:invocation/model :invocation/effort
    :invocation/precontext}`; visible-defaults parts on the block
    anatomy bound to the worn values (per-block instance deviation =
    the sticky per-block setting, already durable machinery); the
    consult reads the SOURCE block's worn invocation server-side at
    `run-episode-turn` (`server_jetty.clj:876`) and threads model into
    `summon-argv` (`episode.clj:871-884` — carries NO model flag
    today) and precontext into the existing narrowing
    (`reply_to_block.cljc:16` `narrowed-portal-open`). Effort's wire
    mechanism is VERIFIED against the installed CLI before wiring
    (V6); if the CLI carries no effort control, the value stays
    material + visible and the gap is disclosed in the receipt, never
    faked. (Validation receipt 2026-07-30: `claude` 2.1.220 on this
    box carries BOTH `--model <model>` and `--effort <level>` — `low,
    medium, high, xhigh, max` — so the honest subset is model + effort,
    both wireable; re-verify at wiring time, the duty stands.) Touching `episode.clj` re-cuts its standing SHA-256
    zero-diff pin in the same change
    (`material_portal_test.clj:1203-1208` — the pin is a standing
    suite assertion, re-cut when legitimately touched, per its own
    gate law).
- **W8 — MIGRATION + CONTINUITY.** Structural change is an explicit
  recorded migration: the activation event carries its grounds; recipes
  never silently migrate instance state. Ephemeral continuity at
  type-flip: caret/selection/focus key on unit-id and SURVIVE a
  rebuild under a new anatomy; an in-flight drag completes under the
  old revision — activation applies at the next rebuild, never
  mid-gesture; an active preview of the flipping master ends before
  the flip is fed to render.
- **W9 — THE ECHO BAR.** p95 < 52ms (the standing instrument,
  `ground.cljs:1819-1872`) with the interpreter live and the Workshop
  open during sampling; environment attested first. Pre-authorized
  escape hatch: compile-to-cached-template invalidated on activation —
  a compile step, never a law change.

## §4 Non-goals — each a named extension point, not a void

- **In-place setting picker widgets on the block** (click the model
  name to cycle, etc.) — v0 makes the values VISIBLE via anatomy parts
  and CHANGEABLE via the deviate lane (room/console/agent); a dedicated
  picker needs new binding rows + verbs and is exactly the kind of
  change the Workshop makes cheap — a later material diff + one small
  package if a verb is needed.
- **The space anatomy cutover** — the space stays policy-only (zero
  pixels today); the second component type is the family's
  generalization moment, governed by the second-wearer law.
- **Other hand-written composers** (halo card, provisional slot,
  marquee, anchor, lint cards, editor overlay, trail faces) stay code —
  pre-named walls; the block is v0's whole claim. Each is a future
  wearer at its own recurrence.
- **face_assembly.cljc migration to `shared/`** — permitted only if
  the JVM path genuinely compels it (it is `.cljc` and JVM-loadable
  where it sits); otherwise it stays grandfathered per CLAUDE.md.
  Disclose either way in the receipt.
- **Figma-grade drag-manipulation of parts** — v0 edit is row-level
  acts with live preview; direct-manipulation skin rides the same
  candidate lane later.
- **The Workshop's own face as material** (the browser-rewrites-the-
  browser flip) — the far gauge, deliberately not v0.
- **Meta-schema evolution** (the anatomy FORMAT itself) — floor;
  changes are explicit grammar-version migrations, the
  `ensure-active-source!` class.
- **Gauge stratum-split instrumentation** — proposed in DIRECTION as a
  C1-instrument refinement; an evidence-review item, not build. BETS
  untouched by this package.
- **Recursive/deep anatomy nesting** — one def-reference level, by
  construction; depth genuinely demanded = a grammar-version migration
  with its own contract.

## §5 Placement ruling

New shared namespaces: `src/app/shared/anatomy_material.cljc` (master
spec, part grammar, validators, the seed form + floor constant) and
`src/app/shared/invocation_material.cljc` (P2). Everything else lands in
the files that own their stations today: primitives in
`face_primitives.cljc`; the rebuild swap, wear resolution, projection
render, and edit composition in `ground.cljs` (+ `face_wiring.cljs` for
room/portal hands); serve/portal sections in `face_projection.clj` /
`material_portal.clj[c]`; consults at their named seams
(`server_jetty.clj`, `episode.clj`, `reply_to_block.cljc`,
`ground_edit.cljc`); bootstrap in `cluster.clj`. NO new Rama module
(adapter precedent; `bin/land`'s five-module set unchanged), NO new
HTTP route, NO new import family. One new test namespace
`test/app/anatomy_test.clj` is warranted (the family's own gates —
grammar, interpreter totality, byte-identity harness, continuity) and
MUST get its tier entry in `test_runner.clj`'s inventory
(fail-closed enumeration, `test_runner.clj:226-250`). Alternatives
rejected: a new module (nothing homeless — §0); extending
`matter_room.cljc` with anatomy knowledge (rooms are master-generic —
keep them so); a new endpoint (the deviate master branch already
carries candidates).

## §6 The phases

### P1 — the engine cutover (FULL gate tier)

**Files (allowlist; substance-bound per the manifest law; docs under
`build/smalltalk-ui-vm/` free):**
- `src/app/shared/anatomy_material.cljc` — NEW: master id `fm:anatomy`,
  facet `:anatomy`, part-row grammar + closed vocabularies, validators
  (compile-form conventions), the seed form, the floor constant.
- `src/app/shared/facet_masters.cljc` — the `fm:anatomy` spec entry;
  nothing else.
- `src/app/shared/threaded_material.cljc` — the SECOND grammar version
  (the next key after the existing `0` — "v2" names the count, not the
  key) carrying the thread-edge part keys (rail style/width, indent) +
  validator widening; the existing keys byte-stable.
- `src/app/client/workspace/face_primitives.cljc` — the block's new
  primitives, cut from `block-tree`'s own code; registry entries only —
  no changes to existing primitives' semantics.
- `src/app/client/workspace/face_assembly.cljc` — ONLY if the block
  context genuinely compels a widening (e.g. apply-context census);
  additive, disclosed; core compile/apply semantics byte-stable.
- `src/app/client/workspace/ground.cljs` — `resolve-material-wears`
  gains `:anatomy`; `block-wear-census` (`:347`) gains `:anatomy`
  (validation F3 — the fourth non-generic surface; the pinned test
  literal re-cut below); `rebuild-block!` renders via compile/apply +
  the compile cache; `block-tree` and its private helpers DELETED
  (scope per W3 — sole caller `rebuild-block!:1214`);
  continuity rules (W8) at the rebuild/activation seam; no dispatch,
  grammar, or verb-registration changes.
- `src/app/server/rama/cluster.clj` — bootstrap entry for `fm:anatomy`
  + the `fm:threaded` grammar migration, the existing `:378/:408`
  pattern.
- `test/app/anatomy_test.clj` — NEW (tier entry in
  `test_runner.clj`); grammar refusals teach, interpreter totality
  (malformed → error-card, floor renders), golden byte-identity vs
  committed fixtures, continuity, compile-cache keying.
- `test/app/test_runner.clj` — the inventory entry + receipt floor
  growth only.
- Focused re-cuts in existing test namespaces the diff disturbs
  (`material_portal_test` wear-census vector `:1159-1201`;
  `material_truth_test` deterministic-time census `:612-628` if new
  time literals; `binding_dispatch_test` one-place censuses ONLY if a
  pinned count moves — moving them needs the pin re-cut in the same
  change, disclosed).
- ZERO-DIFF asserted: `src/app/server/episode.clj` ·
  `src/app/server/rama/relation_kernel.clj` (both SHA-pinned) ·
  `src/app/server/rama/object_container.clj` ·
  `src/app/shared/binding_material.cljc` ·
  `src/app/shared/verb_registry.cljc` · `src/app/server_jetty.clj`.
- **FORBIDDEN:** any new module/depot/PState/topology/import composer ·
  any new HTTP route · any `register-verb!` change · `env.clj` (NEVER
  read) · `src/app/server/cascade.clj`.

**Behavior at P1 close:** every block renders through the interpreter,
byte-identical at the seed; `block-tree` is gone; the threaded-pixels
candidate revision has been driven console-lane through
deviate → preview → activate → visible rail/indent → rollback →
restored, live; echo holds; G1–G5 green.

### P2 — the Workshop + the remaining pressures (slim gate tier)

**Files (allowlist):**
- `src/app/shared/invocation_material.cljc` — NEW: `fm:invocation`
  master (model/effort/precontext), grammar + validators + floor.
- `src/app/shared/facet_masters.cljc` — the `fm:invocation` entry.
- `src/app/shared/foldable_material.cljc` — paste-clamp policy keys,
  grammar migration.
- `src/app/shared/anatomy_material.cljc` — seed revision additions only
  as pressures demand (visible-defaults parts land as a candidate
  REVISION, not a seed edit, wherever the live lane suffices).
- `src/app/shared/material_portal.cljc` — the pure composition-section
  rows + stratum labels; the 17 questions untouched. The `worn-five`
  recipe set (`:397-403`) is DISTURBED by anatomy joining block
  compositions (validation F6 — anatomy-stamped contributions widen
  the observed composition, so the named "block" recipe stops
  matching): rule its widening explicitly in the section work (the
  named recipe follows the worn composition), disclosed in the
  receipt.
- `src/app/server/rama/material_portal.clj` — the composition section
  in `open`; read-only laws hold (the no-write-verb scan
  `material_portal_test.clj:2147-2162` stays green).
- `src/app/server/rama/face_projection.clj` — only if the section
  serve compels a feed; NO new `ocr/` or `face-arsenal/` call (the
  G21 exact read-surface sets stay green).
- `src/app/client/workspace/ground.cljs` — the projection render +
  specimen + part↔pixel highlight (scene-descriptor actions) +
  edit-to-candidate composition + paste consult + `:invocation` in
  `resolve-material-wears`.
- `src/app/client/workspace/ground_edit.cljc` — the paste branch
  consults the worn policy (pure; the clamp decision testable on JVM).
- `src/app/client/workspace/face_wiring.cljs` — workshop hands on
  `__portal` (composition read, candidate submit via the existing
  deviate/preview/activate hands); no new endpoint calls.
- `src/app/client/workspace/runtime/mouse.cljs` — only if the paste
  route needs source metadata (clipboard kinds); minimal, disclosed.
- `src/app/server_jetty.clj` — the invocation consult at
  `run-episode-turn` (read worn invocation for the source subject;
  thread into argv + narrowing); no new route.
- `src/app/server/episode.clj` — `summon-argv` gains the model (and
  verified-effort) threading; **its SHA pin re-cut in the same
  change**.
- `src/app/shared/reply_to_block.cljc` — precontext knob into
  `narrowed-portal-open`/`compose-resident-prompt`; versioned seam
  scan (`reply_to_block_test.clj:208-221`) re-cut as needed.
- `src/app/server/rama/cluster.clj` — bootstrap entries/migrations for
  `fm:invocation` + `fm:foldable`.
- `test/app/anatomy_test.clj` + focused re-cuts (the episode SHA pin;
  wear-census vector gains `:invocation`; briefing/pin sets ONLY if
  genuinely disturbed — disclosed).
- ZERO-DIFF asserted: `src/app/server/rama/relation_kernel.clj` ·
  `src/app/server/rama/object_container.clj` ·
  `src/app/shared/binding_material.cljc` ·
  `src/app/shared/verb_registry.cljc`.
- **FORBIDDEN:** as P1, plus: no anatomy-specific branch in
  `matter_room.cljc` (rooms stay master-generic); no new question in
  the 17-card floor.

**Behavior at P2 close:** the six arrows run headed end-to-end from a
right-click on a real block; the three pressures are live as material
with their named consults; the boundary strata render on every
composition row; an agent edit lands through `__portal` first try;
G6–G10 green.

## §7 Traps ledger — cite by number in code comments

- **T1** — a second structural truth: `block-tree` (or any helper of
  it) surviving P1, or any per-component composer path reachable for a
  block = FAIL.
- **T2** — the interpreter branching on component identity, or
  condensing content by dispatch into per-component code = FAIL
  (derived-only; the halo H3 class).
- **T3** — anatomy rows naming code: any non-keyword primitive
  reference, fn value, or string resolved to code in material = FAIL
  (closed vocabulary; the binding-grammar law).
- **T4** — byte-identity gated on synthetic fixtures only = FAIL: the
  comparison ENUMERATES the live corpus and names its verb
  (render-and-compare each; counts asserted) — the code-atom G-F1
  class.
- **T5** — preview leaking: any durable write on the preview path,
  `endPreview` not restoring by identity, or a preview surviving the
  activation it previewed = FAIL.
- **T6** — activation applying mid-gesture, or caret/selection/focus
  lost across a type-flip rebuild = FAIL (W8).
- **T7** — a pressure landing as code where material could say it
  (thread pixels hardcoded; clamp constant in code; model literal in
  argv) = FAIL — the escape gauge is the standing detector;
  structure-escapes must read zero post-package.
- **T8** — a refusal that does not teach (missing offending value or
  legal vocabulary), or the agent-edit receipt failing = FAIL (R10's
  gate, G8).
- **T9** — the fence pierced: a fifth act endpoint, a registry verb, a
  new import composer (the eight-owner census), a sixth module, or a
  gesture-grammar change = FAIL.
- **T10** — a disturbed exact pin not re-cut in the same change
  (episode SHA at P2; wear-census vector; inventory floor; time
  census) = FAIL — the pinned-scan law.
- **T11** — the anatomy identity missing from the rebuild keying, or
  keyed on a fast-flipping identity while rebuilding from a
  slow-changing source = FAIL (the scene-substrate G-F1 keying class;
  W3 names the sources).

## §8 Acceptance gates

**P1 — FULL tier (cutover-class):**
- **G1 — suite + compile.** Focused namespaces green (selection
  diff-derived); the new namespace in the runner inventory; every
  disturbed pin re-cut; CLJS `clj -M:dev -m shadow.cljs.devtools.cli
  compile dev` → 0 warnings.
- **G2 — byte-identity + deletion.** The enumerated live corpus +
  committed fixtures render-and-compare exact per W3's named equality
  relation (interpreter-provenance keys normalized; everything else
  exact). The live-corpus ENUMERATION SURFACE is named (validation
  F5): `(:blocks @!world)` after reconcile — the POST-MERGE rendered
  set — read on the rig as `window.__ground.blocks()`
  (`ground.cljs:3701-3705`), count asserted; verb =
  render-and-compare each in one browser context while both paths
  exist. Golden fixtures = committed EDN generated from `block-tree`
  BEFORE deletion (view + wears + metrics + headers + marks as data);
  the receipt banked in the phase artifact; `block-tree`
  grep-negative in `src/`; the committed harness asserts the
  post-invariant against golden fixtures JVM-side (the interpreter is
  `.cljc`; `assembly_adapter.clj:145` is the standing server-side
  proof).
- **G3 — the loop's durable half, live** (isolated rig, the worktree
  convention — separate worktree, `LAND_CLUSTER=0 LAND_PINNED=1`, own
  port per the :8093–:8098 ladder, `env.clj` symlinked blind and
  removed at teardown): the threaded-pixels candidate driven
  console-lane — deviate (candidate import, revision-id verified) →
  membrane preview (server bytes immobile) → activate → rail/indent
  visible on real blocks → rollback → byte-restored; restart restores
  the active pointer.
- **G4 — echo.** Environment attested first; warm p95 < 52ms with the
  interpreter live and a preview open during sampling; cold recorded
  separately, never the receipt.
- **G5 — one fresh falsifier**, in-phase, aimed at T1/T2/T3/T4/T11 +
  W8 continuity. First-FAIL → smallest in-fence repair →
  counterexample re-run; counterexamples become fixtures.

**P2 — slim tier:**
- **G6 — suite + compile + pins.** As G1; the episode SHA pin re-cut;
  wear-census vector re-cut; endpoint-set test still asserts exactly
  four; registry equality unchanged.
- **G7 — the six arrows, headed, live** (isolated rig): right-click a
  real block → `:anatomy` listed honestly → enter → the room with
  composition rows (strata colored) + live specimen → pick a part ↔
  pixels highlight → edit a part → candidate → preview on live ground
  → activate scoped → announced → reverse. Then the pressures: paste
  over threshold arrives clamped + source-marked + expandable;
  invocation defaults visible on the block, a per-block deviation
  sticks durably, and the spawned resident's argv carries the worn
  model (receipt from the spawn log).
- **G8 — the agent-edit receipt.** A scripted `__portal` session
  composes a valid part edit and lands it first try through the
  existing hands; a malformed edit is refused with a teaching card
  naming the offending key and the legal vocabulary.
- **G9 — echo.** Warm p95 < 52ms with the room + Workshop projection
  open.
- **G10 — one fresh falsifier**, aimed at T5/T6/T7/T8/T9/T10.
- **Sid's headed Workshop wear is NOT a gate** — his first
  unscaffolded open and his first REAL structural change routed
  in-land are the C1/C2-class instruments; scaffolding them in a gate
  would poison the measurement. The gate record says so.

## §9 Stop clauses

Verbatim from the halo package, in force here: the implementing session
NEVER improvises policy on binding docs. A genuine conflict between
this contract and disk (or inside it) → STOP, write the finding to
`build/smalltalk-ui-vm/NOW.md`, Sid/Fable rules, the ruling lands
before code resumes. Manifests bind on SUBSTANCE — locator drift
re-locates + logs, never stops. Named foreseeable stops: (a) an
assembly convention that genuinely cannot hold the block (a W2
FAIL-tell) — escalate, never parallel-build; (b) byte-identity
divergence rooted in a `block-tree` behavior the schema cannot express
— that is a schema-wrongness finding (pressure-cut law), escalate with
the divergent specimen; (c) the CLI carrying no effort control (V6) —
disclose and land the honest subset, no fake wire. The fresh falsifier
is never skipped. Commit decisions are Sid's; code and docs ride
separate commits; docs only on the local docs branch, never pushed;
never Co-Authored-By.

## §10 Residue intake — discharged at cut

The halo GATE_P1 five findings reviewed: the SHA-pin standing rule is
EXECUTED by this contract (P2 re-cuts the episode pin, the first
legitimate touch); the other four (`:relation-appended?` honesty,
`:replay?` under-reporting on exact HTTP replay, 400-vs-422 envelope
shape, ask-parse array trust) are say-lane tenants — re-banked in
GATE_P1 where they live. The matter-room re-banked set (GATE_P3 ×3,
GATE_P4 ×5) stays room-side, untouched. The armed matter-room §8
package falsifier (Sid's first real material-policy change routes
in-land) stays ARMED and untouched — this package's own activations
are package-built drives, not its subject; its stratum-split refinement
is DIRECTION's proposal for the next evidence review.

## §11 Input manifest — substance binding; line numbers are hints
(machine-verified 2026-07-30 by four fresh terrain sweeps at HEAD 25ce803)

- `ground.cljs` — `block-tree :599-899` (the transcription source; body
  ops `:658-672` · sel wash `:697` · rail `:713` · attention box `:748`
  · fold hits `:759` · gsel `:777` · caret `:785` · refusal `:791` ·
  notice `:798` · boundary `:806` · gold/silver `:820/:837` · lint
  `:534` · root `:851`; w/h law `:654-656`; colors `:179-183`; fold
  grammar `:185-197`) · `resolve-material-wears :206` (hand-written
  map) · `current-material-wears :263` · `wears-for :289` ·
  `served-material-value :248` · preview `:3254` `end-preview! :3312`
  `preview-state :3323` · `rebuild-block! :1142` (`:render-sig`
  `:1205-1211`) · `upsert-block-slot! :904` (`block-vi :902`) ·
  triggers: truth watch `:3652-3657`, material watch `:3663-3673`,
  `rebuild-material-sites! :1007`, `reconcile! :1487` · halo census
  `:344-384`, rows `:386-420`, action registration `:532` · paste
  `handle-paste! :2289` → `handle-content-key! :2129` ·
  `reply-to-block! :2044` (thread pick `:2069`, wearers `:2098`) ·
  echo `:1819-1872`, report `:3572-3621` · deviate lanes
  `set-instance-bindings! :2450` / `served-instance-binding-rows
  :2500`.
- `rect_tree.cljc` — `rt-node :40` · walks `:225/:286/:353` ·
  `hit-test :382` · `resolve-layout :194`.
- `face_assembly.cljc` — `compile-assembly :330` · `apply-assembly
  :526` (view-ctx census in its docstring) · `error-card-node :266` ·
  `build-node :414` · `expand-slot :472` · `stamp-root :511`.
- `face_primitives.cljc` — `registry :934` · `stack-prim :451` ·
  `text-run-prim :476` · `indent-rail-prim :519` · `box-prim :588` ·
  `header-band-prim :625` · `text-clip-prim :701` · `sliver-prim
  :758`.
- `facet_masters.cljc` — `specs :14` (order-bearing) · `by-facet :29` ·
  `floor-master-id :56`.
- `facet_material.cljc` — `compile-form :58` (error minting `:94-126`)
  · `compile-source :132` · `valid-material? :159` · `resolved-wear
  :175` · `contribution-stamp :194` · `instance-marker :235` · pins
  `:251-273` · `wear-for-subject :370` (tier + malformed-fall laws
  `:388-391`) · `compose :495`.
- `object_container/facet_master.clj` — `import-key :38` ·
  `materialization :69` · `append-and-read! :206` · `import-candidate!
  :213` · `read-master :227` · `activate! :252` (pre-validation
  `:258-273`) · `ensure-master! :344` · `ensure-active-source! :374`
  (the grammar-migration lane) · `write-instance-revision! :517` ·
  `deviate!/release/pin/unpin :641-677` · `activation-trail :695` ·
  `worn-at :733`.
- `object_container.clj` — `imp:fm:` routing `:328` · conflict
  predicates `:537-557` · module `:1746` (five-module set:
  `bin/land:21-29`).
- `cluster.clj` — `facet-materials-ingest! :330` · spec iteration
  `:362` · migrations `:378/:408`.
- `face_projection.clj` — `facet-master-projection :876` ·
  `facet-materials-projection :995` (spec iteration `:1007-1010`) ·
  `interaction-table-tiers :1018` · registry `:1825-1839` · `serve
  :1888`.
- `material_portal.cljc` — `questions :149` (17, pinned) ·
  `composition/composition-id/recipe :371-401` · `why-this-pixel :514`
  · `render-model :799` · `briefing :848`.
- `material_portal.clj` — `open :671` (sections `:741-860`) · `render
  :1052` · read-only laws (no-write scan, no-model scan — test pins
  below).
- `matter_room.cljc` — `registered-master-ids :57` (derives from
  specs) · `room-id-by-master :74` · act builders `:159-291`.
- `server_jetty.clj` — deviate `:1329` (master branch `:1357`) ·
  activate `:1360` · rollback `:1384` · routes `:2016-2086` ·
  `run-episode-turn :876` (prompt composition `:1036-1045`).
- `episode.clj` — `summon-argv :871-884` (no model flag today) ·
  `utterance-import-request :654-690` · **SHA pin
  e1f1836f… stands until P2's legitimate touch re-cuts it**.
- `reply_to_block.cljc` — `narrowed-portal-open :16` · `request :39`
  (body fields `:48-61` — no model/effort today) ·
  `compose-resident-prompt :63`.
- `threaded_material.cljc` — `:15` (`column-adoption-reach-lines`, the
  ONLY key; zero pixels today) · validator `:31-33`.
- `positioned_material.cljc` — `:29-34` (reply-gap · fallback ·
  anchor-order).
- `runtime/mouse.cljs` — `install-paste-handler! :39` (ground branch
  `:54`); `ground_edit.cljc` — `:paste :67-71` (verbatim insert).
- `block_edit.cljc` — the pure edit machine (`init-state :98` …
  `block-view :307`); `block_edit_wiring.cljs` —
  `install-block-edit-wiring! :168`.
- `face_wiring.cljs` — `material-request! :632` (mirror `:676-680`) ·
  `__portal :423` (`say :502` · `openMaster :428` · deviate/act hands
  `:386-393`) · `invoke-halo-handle! :78-113`.
- Test pins the phases will disturb or must keep green:
  `material_portal_test.clj:1123-1137` (four endpoints exact) ·
  `:1203-1208` (the two SHA pins) · `:1159-1201` (halo kernel-scope +
  exact wear-census vector) · `:483-502` (eight import owners) ·
  `:263,1278-1282` (17 questions) · `:2147-2162` (portal no-write
  scan) · `binding_dispatch_test.clj:997-1001` (registry↔ground) ·
  `:915-929` (frozen grammar) · `:149-161` (floor-reserved + durable
  exact sets) · `:632-672` (16 probe receipts) · `:975-995` +
  `:933-938` (one-place censuses + branch budget) ·
  `material_truth_test.clj:461-485` (21 rows) · `:612-628` (time
  census) · `space_material_test.clj:134-193` (reservation censuses) ·
  `face_arsenal_test.clj:194-213` (exact read-surface sets) ·
  `test_runner.clj:226-250` (fail-closed inventory) + `:156-160`
  (receipt floor) · HEAD-dynamic re-run after commits:
  `git_spine_gate_test` + `code_atoms_test`.
- Verification duties BEFORE code (the validation round's checklist):
  - **V1 — assembly capacity:** walk `block-tree`'s full behavior
    census (§11 render pins) against compile/apply/registry on disk;
    every behavior maps to a primitive + part row or is named as a
    new primitive; any convention that cannot hold the block = a §9(a)
    stop finding, pre-code.
  - **V2 — corpus + verb:** pin the byte-identity corpus enumeration
    (live blocks + fixtures), its verb (render-and-compare each), and
    the golden-fixture generation step BEFORE deletion; confirm the
    ONE-SHOT law's committed-harness form.
  - **V3 — wear generality:** confirm on disk that `wears-for` /
    `resolve-material-wears` / `:render-sig` / the halo census carry
    a new facet with only the FOUR named touches (§0; validation F3
    added `block-wear-census`); enumerate every site that hardcodes
    the six-wear assumption (the halo census vector pin included —
    census executed, VALIDATION.md §V3).
  - **V4 — re-cut every affected exact pin, same commit** — the full
    list above, per phase; P2 explicitly owns the episode SHA re-cut.
  - **V5 — standing:** every memory-derived platform claim checked
    against on-disk references.
  - **V6 — the CLI seam:** verify the installed `claude` CLI's actual
    model/effort/context flags before wiring the invocation consult;
    the honest subset lands; gaps disclosed, never faked.

## §12 Handoff

- Thread file: `build/smalltalk-ui-vm/NOW.md` (STANDING frozen at
  open + newest-first entries). Phase receipts: `P1.md` / `P2.md`
  (diff-derived file lists, falsifier verdicts with counterexamples,
  gate evidence). Gate records: `GATE_P1.md` / `GATE_P2.md`.
- Next session after the cut: ONE fresh default-fail validation round
  over this contract (V1–V6 + coherence). Minor-fail law applies; a
  substantive FAIL amends the contract in place and re-runs the round.
- Then: a fresh implementer context builds P1 whole under §6; Fable
  orchestrates + gates (FULL tier). P2 opens only after P1's gate
  PASS + Sid's commit ruling; a fresh implementer builds P2 whole;
  slim gate.
- After each closing code commit: re-run the HEAD-dynamic suites at
  committed HEAD (`git_spine_gate_test`, `code_atoms_test`).
- The package closes at: both gates PASS + Sid's commit rulings +
  board flip. Retro joins the stratum batch. Sid's first unscaffolded
  Workshop open and his first real structural change routed in-land
  are the C1/C2-class instruments — deliberately never gates.
