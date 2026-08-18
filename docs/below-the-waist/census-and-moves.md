# Below the waist — census and moves

The ground for the below-the-waist phase chat. Census taken 2026-08-18 by an
opus gatherer (static reading only: require graph + call-site grep, no runtime
probe) — single-pass and unfalsified: the phase chat runs Codex adversarially
over each row (try to prove it live/reachable) before any ruling (Sid,
2026-08-19: Codex is "better than opus for hunting and figuring and also
adversarial"). Adjudication verdicts by the derivation seat. A row remains
unruled unless explicitly marked **SID-RULED** or **CODEX-RULED** under Sid's
delegated removal authority below; rulings happen in the phase chat, row by
row, exact paths. Row 1 was ruled 2026-08-19 after its Codex falsification pass
found and repaired the test-only `cards.cljc` dependency; Row 2 closed after
its live build falsified and corrected the first dependency cut.
The underlying code always wins over this file.

## The goal (Sid, 2026-08-19, verbatim)

> "i do want to get to the state where large parallelisation and concurrent
> ui/ux and all other product work i can do above the waist — everything
> below it right now is sync and linear to this — has to be done"

The phase ends when the below-waist substrate is settled enough that product
work above it can fan out in parallel without being able to break the kernel.

## Naming gloss (two unrelated things share a word)

- **trails** — motion leaving traces. RULED yes 2026-08-18 (stamps never
  paths; thresholds are userland dials; a burst's settlement always
  persists). Recorded at `docs/ARCHITECTURE.md` conflict 2 and
  `docs/electric-native/PROBLEM-SPACE.md` forks-by-decider.
- **trail-face** — the old docs-reading surface (cards/lanes/threads over the
  ingested corpus). Dark client code, retire-candidate below. Its server-side
  ingestion is live artery and is NOT touched by retiring the drawing files.

## The four moves (and the two laws)

1. **Retire whole** — dead iterations nothing live mounts. True deletes,
   whole paths, tests included, builds green (the dead-path-census pattern).
2. **Hoist** — decisions buried as constants become facts with a code-held
   floor default (the facet-master pattern; wrap-columns is the precedent).
   Same behavior day one; the decision becomes visible and dial-able.
3. **Demote with duties, never delete** — wrong-shaped-but-live paths (the
   whole-page pull artery). Build the keyed path beside it, transfer the
   named duties, then the old path steps down to a checking role.
4. **Melt by replacement** — fused monoliths are never refactored in place;
   the treaty-shaped replacement is built small, and when it carries one real
   case the monolith retires whole.

Laws: **no duty-carrier is removed before its replacement carries the
duties**; and **the moves finish the waist** — every hoist and melt forces
"what fact-kind or verb does this actually need," so the waist member list is
discovered by migration, never guessed in the abstract.

## Headline census finding

**The old pre-render-engine Electric-DOM block editor no longer exists in
src/.** It died in the dead-path census (Sid's ruling 2026-08-15) — headers
say so at `src/app/client/workspace/runtime.cljs:5`,
`runtime/mouse.cljs:5`, `runtime/state.cljs:6`. localhost:8080 boots exactly
one UI element (a full-screen canvas, `electric_flow.cljc:331-335`) plus the
scene loop (`runtime.cljs start-loop!`). The typing Sid does there is the
live scene surface: `ground.cljs` minting edit envelopes through
`block_edit.cljc` — alive, keeps.

## Entry map (what actually boots)

`shadow-cljs.edn :dev` → single module `dev/start!` → `src-dev/dev.cljc:77`
`e/boot-client … app.electric-flow/main` → `electric_flow.cljc:201 main` →
canvas at `:331-335` → `loop/start-loop!` at `:357` →
`workspace/runtime.cljs`: trail wiring `:49-53`, face wiring `:56`,
block-edit wiring `:78`, `ground/install-ground!` `:92`, consumers
`:130-135`, render consumer `:138` (region3d self-gated `?region3d=1`).
Flag-gated second world `?live-atoms=1` (`live_atoms.cljs:254`): chrome,
frame, **T2** (`editing-runtime/boot!` `:361`), seam-demo. Separate build
`:render-verifier` (entry `substrate/webgpu/verifier.cljs`, ~6,000 L) is
exercised by `package.json verify:render-engine` — NOT dead code, and it
inflates rev-dep counts (requires editing_runtime, text_editing, live_atoms,
chrome_runtime, frame_runtime, live_edges).

## Pile 1 — retire whole (Sid's yes/no per row)

### Row 1 — trail-face drawing island — SID-RULED: RETIRE — CLOSED (2026-08-19)

**Retired** is the architectural ruling: prove the path dark, preserve or
transfer every surviving duty, repair its consumers, then remove it.
**Removed** is the resulting tree operation. The five old drawing files are
both retired and removed from the active tree; git keeps their history. Their
three harvested builders do not retire: `omission-line`, `omissions-block`,
and `hole-endpoint-card` already live in the generic face vocabulary as the
`:omissions` and `:hole-card` primitives.

Exact boundary — 16 affected files, no additions:

- **Delete (9):** `trail_face/{scene,cards,threads,lanes,text_face}.cljc` ·
  `test/app/client/workspace/trail_face_test.clj` ·
  `test/resources/trail_face/{bundle,feed,projection}.edn`.
- **Repair (6):** `face_primitives.cljc` loses only obsolete live-origin/G7
  wording · `scene_runtime.cljs` loses only the orphan
  `:trail-face/toggle-expand` registration · `face_primitives_test.clj`
  loses the filesystem source-form pin while retaining primitive coverage ·
  `face_integration_test.clj` loses or rebases only the legacy shipped-trail
  comparison while retaining its independent assembly tests ·
  `scene_store_test.clj` keeps the generic descriptor proof with neutral data
  and loses the trail-only handler proof · `test_runner.clj` loses the retired
  namespace's classification/cost entries and closes the pre-existing exact-
  inventory drift exposed by the removal receipt (`region3d-evaluation-test`
  and `region-rungs-test` are pure and green); its receipt floor changes only
  from an actual post-removal receipt, never guessed subtraction.
- **Converge (1):** this file records the ruling, corrected dependency
  boundary, retained duties, and close receipt.

Refusals: `trail_face/wiring.cljs`, `sanitize.cljc`, the pull atoms and
Electric artery, every server ingestion/trail path, the generic descriptor
router, all renderer/WebGPU/verifier code, and rendering-engine examples stay.
Historical documents keep their snapshot/prior-art references.

Close proof: no executable reference to a deleted namespace/path · retained
face primitives still compile and render · generic descriptor replay remains
true without trail vocabulary · the test inventory is exact · dev CLJS and
the row-local removal receipt are green. The repository-wide lanes were also
run and their foreign reds are recorded below; this row does not call them
green. The waist member-walk records only the pressure found here: omission
collections and hole endpoints are already generic enactable shapes; whether
either earns a primitive fact-kind name remains open until its real material
consumers are walked.

Close receipt — 2026-08-19:

- Tree: exactly 16 changed paths; 9 removals, 6 repairs, this census; diff-check green.
- Reachability: zero executable references to the five removed namespaces/paths or orphan action.
- Kept: wiring, sanitization, pull/ingestion artery, generic router, harvested primitives, renderer/verifier/examples.
- Row-local JVM: 39 tests / 310 assertions / 0 failures / 0 errors.
- Inventory: exact — 78 discovered = 43 pure + 8 shared + 27 isolated.
- Browser: dev CLJS green — 265 files; close rerun compiled 2; 4 existing inference warnings.
- Broad debt: fast 343 tests / 3,264 assertions, 7 fail + 1 error; full 612 / 7,615, 9 fail + 2 errors, outside this row's changed behavior.
- Floor: 612 / 7,615 remains above 397 / 5,416; no receipt-floor edit.

**The trail-face island** — one closed island, ~1,776 lines; `scene.cljc` is
the only door in and nothing in src/ opens it; render loop has zero 'trail'
hits (`grep 'trail' runtime/render.cljs` → none); its three atoms
(`!trail-text !trail-feed !trail-bundles`, created `runtime/state.cljs:138-140`,
reset by `trail_face/wiring.cljs:79,81,83`) are read by nothing:

| path | lines | mounted by | rev-deps in src |
|---|---|---|---|
| `src/app/client/workspace/trail_face/scene.cljc` | 799 | nothing | 0 (test only) |
| `src/app/client/workspace/trail_face/cards.cljc` | 415 | nothing | 1 (scene) |
| `src/app/client/workspace/trail_face/threads.cljc` | 305 | nothing | 1 (scene) |
| `src/app/client/workspace/trail_face/lanes.cljc` | 163 | nothing | 2 (scene, threads) |
| `src/app/client/workspace/trail_face/text_face.cljc` | 94 | nothing | 1 (scene) |

Their tests retire with them: `test/app/client/workspace/trail_face_test.clj`,
`test/app/face_integration_test.clj` (referencing). **STAYS:**
`trail_face/wiring.cljs` + `trail_face/sanitize.cljc` (installed at boot,
`runtime.cljs:50`). Follow-up ruling for the phase chat: whether wiring's
trail-pull arms demote once the island is gone (they currently feed the dark
atoms every epoch — lights on in an empty room).

### Row 2 — residue + legacy face editor — CODEX-RULED: STRIP — CLOSED (2026-08-19)

Sid delegated the remaining removal calls in this phase. The 2026-08-19
Codex falsification returned **READY**, with three corrections accepted before
the cut: `global-flow/await-promise` still needs Missionary; the old face
projection's orphan `declare` must leave with it; and
`probes/block_edit_probe.cljs` is retained explicitly as quarantined history,
not mistaken for an executable consumer. It is off every configured classpath
and would need historical checkout or adaptation before resurrection.

The first browser compile then falsified the npm sub-ruling: Shadow restored 14
CodeMirror/Lezer packages before compiling. The receipt led to the actual edge,
the otherwise unused `io.github.nextjournal/clojure-mode` dependency in
`deps.edn`; its own `deps.cljs` names exactly those 14 packages. A bounded Codex
follow-up found no project consumer and returned **READY** on removing that one
dependency too. The repeated compile, not this reading, is the kill test.

The stronger finding replaces two inaccurate census claims. `display-text`,
`refusal-notice`, and `overlay-face-context` were not individually dead: they
called one another and had JVM tests. But that whole face-buffer/projection
shell had no production entrance. The sole `:face-edit` writer was
`face-click!`; only the archived probe called it; the product mouse grammar
routes entirely to the ground. Tests proved the old island internally, not that
the product could reach it.

Exact boundary — 12 affected files, no file deletion:

- **Strip stale shelves and doors (4):** `global_flow.cljs` keeps only
  `await-promise`; `file_viewer.cljc` loses only `TrailConversation`;
  `ground.cljs` loses only the dead `?dev` predicate `ground-boot?`; and
  `events.cljs` loses only the unreachable `:face-edit` keyboard router.
- **Retire the legacy face-editor shell (4):** `block_edit_wiring.cljs` loses
  its face focus/buffer/view/key consumer while keeping submit/result
  continuations, the accepted-edit narrow pull, truth overlay, full-pull prune,
  and the ground's `edit-submit!`; `runtime.cljs` stops constructing and joining
  that unreachable consumer; `block_edit.cljc` keeps deterministic request
  identity, envelope minting, and the semantic key transition used by
  `ground_edit.cljc`, while its old face buffer/render projection retires;
  `block_edit_test.clj` keeps tests for that surviving vocabulary and removes
  tests whose only subject retired.
- **Remove dependency residue (3):** `deps.edn` drops only the unused
  `nextjournal/clojure-mode` root that made Shadow restore CodeMirror; then
  `package.json` and `package-lock.json` drop 22 direct packages: the
  CodeMirror/Lezer family, Radix, React/Recharts, `prop-types`,
  `web-tree-sitter`, and `w3c-keyname`. Electric itself, `bidi-js`,
  `harfbuzzjs`, `shadow-cljs`, Karma, and Puppeteer stay.
- **Converge (1):** this census records the falsification, correction, exact
  boundary, retained duties, member-walk, and close receipt.

Refusals: no archived probe or historical document is deleted; all
renderer/WebGPU/verifier code and examples stay; the real ground editor, T2,
whole face pull, narrow truth transport, trail wiring, and every server/Rama
path stay.

Waist member-walk: this removal exposes the current floor as deterministic edit
request identity plus outbox/result transport and accepted-edit truth
reconciliation. It also exposes one remaining above-waist weld:
`block_edit/apply-keydown` maps keys to edit/caret actions and stays only because
the ground editor calls it today. That is behavior/keymap material, not a newly
declared floor member; Pile 4 must transfer it rather than blessing it as waist.

Close receipt — 2026-08-19:

- Tree: exactly 12 changed paths; 106 additions / 2,901 removals before this close note; no file deletion; diff-check green.
- Reachability: zero executable references to the retired face-edit entrance/router/view symbols, `TrailConversation`, or `ground-boot?`.
- Dependencies: 22 direct npm roots gone; lock records 445 → 302; retained roots are only `bidi-js`, `harfbuzzjs`, and `shadow-cljs`; `npm ls` green.
- Clean install/build toggle: after pruning removed packages, dev CLJS built 264 files / 2 compiled / 4 existing inference warnings and restored nothing.
- Focused JVM: 11 tests / 99 assertions / 0 failures / 0 errors across the surviving edit vocabulary and real ground editor.
- Renderer fences: text layout 6/32 green; shaper, text-layout, and scene-tape fences green; release verifier built 184 files / 0 warnings.
- Foreign renderer red: W0-A fails at `default-unit-z1` because a legacy family paint lacks four required tape fields; pinned pre-Row-2 HEAD fails identically on the same machine.
- Kept: renderer/verifier/examples untouched, plus Electric, Rama/server paths, ground/T2, whole pulls, narrow truth, trail wiring, and archived probes.

## Pile 2 — strip the ghosts (surgery inside live files)

### Row 3 — invisible overlay plumbing — CODEX-RULED: STRIP — CLOSED (2026-08-19)

The 2026-08-19 Codex falsification returned **READY**. It tried to prove a
live writer, nonzero draw, or caller-selected visibility for every command,
settings, sidebar, and agent branch and found none. The command and settings
rect systems are allocated every boot with zero instances and never written;
the sidebar pools have no writer; the sole product draw call supplies false or
zero for every visibility/count door. `ground.cljs` still tries to hide a
command-panel atom that no runtime creates.

Three corrections are accepted into the cut. Public `frame-input-map` and
`draw-frame!` remain named and callable because renderer fences inspect them.
Diagnostics keeps its own visibility, line index, chrome readiness, offsets,
and bounds; only the permanently false command/settings blockers leave. One
tripwire owns the complete ten-key retired input surface so a panel noun cannot
quietly return as renderer state.

Exact candidate boundary — 8 affected files, no file deletion:

- **Stop paying at boot (2):** `runtime/state.cljs` drops the two zero-instance
  rect buffers and the two writerless sidebar pools; its settings atom keeps
  only values consumed by font layout/paint/diagnostics. `runtime/fonts.cljs`
  drops the unread theme and selected-font UI mirrors while keeping font
  defaults, manifest loading, active-font loading, and render settings.
- **Remove the false caller story (2):** `runtime/render.cljs` stops passing
  command/settings/sidebar/agent values that can never produce an entry;
  `ground.cljs` drops the nil-only command-panel hide.
- **Remove product nouns from the floor (2):** `frame_inputs.cljc` drops the ten
  absent-surface inputs from family declarations; `renderer.cljs` drops only
  their pool/system branches, tokens, arguments, and frame fields. Store
  rect/shadow entries, editor pools, diagnostics, every material family,
  retained-frame machinery, and the generic scene executor stay.
- **Tripwire and convergence (2):** `frame_inputs_test.clj` asserts none of the
  ten retired inputs is declared by any family; this census holds the ruling,
  member-walk, and eventual close receipt.

Refusals: no renderer example, verifier, material family, generic rect/shadow
route, store-frame route, editor pool, diagnostics path, or WebGPU execution
machinery is retired. This row removes dead product-specific plumbing inside a
live renderer, not the renderer or its examples.

Waist member-walk: the renderer floor consumes executable scene entries —
family, stable identity/order, paint source and count, plus live system tokens
that can actually change them. It does not own permanent command-panel,
settings-panel, sidebar, or agent nouns. If any of those products returns, its
visibility is above-waist material compiled into entries before the floor; it
does not reopen panel-specific booleans in `draw-frame!`.

Close receipt — 2026-08-19:

- Custody: exactly 8 changed paths; 87 additions / 181 removals before this
  close note; no file deletion; diff-check green before and after the bundle.
- Reachability: zero executable occurrences of the retired atom, system,
  visibility, count, or pool-info names. The only test occurrences are the ten
  exact keys in the negative declaration tripwire.
- Focused JVM: 10 tests / 24 assertions / 0 failures / 0 errors.
- Product compile: dev CLJS built 264 files / 2 compiled / 4 existing inference
  warnings. The scene-tape fence passed with 9 families, 5 effect declarations,
  zero renderer family branches, and no production failures.
- Renderer compile: release verifier built 184 files / 17 compiled / 0 Shadow
  warnings; public `frame-input-map` and `draw-frame!` and all examples remain.
- Full renderer chain: text layout 6/32, shaper fence, text-layout fence, and
  scene-tape fence green. Its sole terminal red remains W0-A
  `default-unit-z1`: the legacy paint value lacks `:paint/source`,
  `:paint/source-type`, `:op-offset`, and `:instance-count`, identical to the
  pinned pre-removal baseline. No new failure appeared.

## Pile 3 — demote later, never delete now (the artery; duty inventory)

### Row 4 — whole-pull artery — CODEX-RULED: DEMOTE-LATER — CLOSED (2026-08-19)

Current source proves this path live: it is the product's only browser-to-Rama
edit road and its only served-face road. Nothing in this row is removed now.
The old shape may step down only after a keyed path carries every duty below;
then the whole pull becomes recovery/oracle evidence, never a second truth
loop.

The artery is four different jobs, not one pile of mirror atoms:

- **Write and answer:** `block_edit/mint-envelope` carries object key, unit id,
  document-container id, deterministic request/idempotency identity, client id,
  and edit sequence into `SubmitBlockEdit`. The Object Container request depot
  is partitioned by object key. Its topology durably writes accepted or rejected
  decisions; an accepted edit writes event, container/revision/graduation, and
  edit-order truth before the stream `:ack` returns.
- **Refresh the changed unit:** the result is correlated by request id. A fresh
  accept arms `:block-truth` with a union map `{unit-id nonce}`; that value is
  deliberately lossless under Electric conflation. `read-unit` serves the
  materialized graduation/revision overlay, and the client overlays only those
  keyed texts.
- **Recover the whole view:** `FacePull` still serves the conversation as
  ordered turns containing blocks whose stable `:id` is the durable unit id.
  The next whole context clears the narrow overlay and remains the one recovery
  path until the keyed wire owns catch-up, membership/order, and tombstones.
- **Wake readers:** `!ingest-epoch-atom` is explicitly process-local and resets
  on restart. It carries no facts and orders nothing semantically; accepted
  writes and ingest continuations bump it only to re-arm current pulls. Face
  roster/wear, trail reads, material projections, and boot-static live edges
  share transport but are distinct duties and rates, not one truth family.

Transfer list for Electric-native 1c: preserve request correlation and durable
rejection; the ack-to-queryable-truth boundary; `unit-id` identity; object and
document scope; edit lineage/sequence; order and membership as data; the union
map's cross-unit conflation safety; full-pull catch-up; and overlay prune. The
global epoch may disappear only when scoped notification or the changelog
carries its wake-up duty. The whole comparer may demote only when catch-up uses
the same keyed apply path and committed-echo cross-check plus overflow recovery
have named owners.

Waist member-walk: an edit crosses the floor as a proposed action with stable
identity and target scope; it returns as a durable accepted/rejected decision;
accepted truth is a queryable unit revision. A projection fact carries entity
identity, occurrence membership, and order. Notification is not truth. Client
buffers, selection, cursor, visibility, and debounce timing remain above the
waist even when Electric transports their consequences.

Receipt — 2026-08-19: the focused real Object Container product path ran 2
tests / 53 assertions / 0 failures / 0 errors. It covers first graduation,
later revision, narrow `:block-truth`, whole river re-read, identical replay,
stale ordering rejection, unauthorized rejection, and edit-WAL replay across a
fresh runtime. The missing receipt is the client join itself: one T2 edit must
return by request id and repaint the same real `unit-id`; that is Pile 4.

## Pile 4 — the join (T2 eats real blocks)

The one real editor is an island: flag-gated `?live-atoms=1`, fed by two
hardcoded fixture documents (`editing_runtime.cljs:23-44`,
`install-fixtures!` `:272-285`, called from `boot!` `:739`), no server data
path (no FacePull, no fetch — checked). The move is not removal: feed it real
facts — the keyed wire's first customer ("prove it by making a block").
Landmines mapped: T2's mount rides `live_atoms.cljs`, which requires
`ground.cljs` (camera provider `:364`); `seam_demo.cljs` dies with T2;
T2-exclusive deps: `editing_segmentation.cljs`, `text_editing.cljc`
(+ verifier). Shared deps (deleting them takes T2 down): containers, events,
rect_tree, runtime/mouse, runtime/render, scene_runtime, scene_store,
text_layout.

## Scene-loop verdict (adjudicated, unruled)

Skeleton fits the settled shape (checked, static): keyed scene store with
incrementally-maintained index, flat keyed renderer frame, closed family
registry, identity-gated pools, camera outside the graph at frame clock,
faces-as-data with "no logic ever" guard. Three misfits, each already in a
pile: feeding (mirror atoms + whole pulls → pile 3), editing welded inline
into `ground.cljs` (~5k lines; melts later into primitive verbs +
cursor-entities + keymap-facts via the T2 join — pile 4), policies as
constants (caps, sizes, visibility flags → hoist).

## Unknowns / caveats (honest edges)

- Unruled rows remain static: require graph + call-site grep only.
- Each removal checks its actual runner/build boundary at cut time; a test file
  or dependency edge is not declared dead from source grep alone.
- The renderer verifier's baseline family-paint red above is carried as foreign
  debt into Pile 2; no renderer source is changed under Row 2.
- `resources/public/faces/` observed, not opened ("W1 HTTP fetch … RETIRED"
  per `face_wiring.cljs:15`); `style.css` is 0 bytes.

## Pointers (read before ruling; plain-words law applies to all writing)

- The recon: `docs/what-softland-is/build-softland-in-softland.md` (+
  `-threaded.md` for depth). The treaty, waist, four forces, fence
  instrument, clocks, the door.
- The wire seam: `docs/electric-native/DIRECTION.md` (road; 1c is the keyed
  wire), `PROBLEM-SPACE.md` (loads, forks), `RECON.md` (receipts).
- Principles: `docs/ARCHITECTURE.md` conflicts 2 (becoming/truth), 3
  (identity travels), 5 (cost follows attention); trails ruling recorded at
  conflict 2 and in PROBLEM-SPACE forks-by-decider.
- Register laws live in session memory (plain English to Sid; exact paths
  for anything he rules on; his deletions are rulings; no Co-Authored-By;
  commit per concern on `docs/current-mental-model-local`, never push).
- Delegation for this phase (Sid, 2026-08-19): **Codex does the code
  hunting/understanding and the adversarial checks** — package rounds with
  the ask-codex-for-feedback skill, ingest with ingest-codex-feedback;
  opus subagents only for mechanical in-harness collation; the main seat
  thinks and adjudicates.

## Boot prompt for the phase chat (copy from here)

> Read `docs/below-the-waist/census-and-moves.md` first — goal, census with
> receipts, the four moves, pointers. Then the recon faces it points at.
> This chat owns three things, Sid ruling row by row in plain English:
> (1) the removal rulings — retire / strip / demote-later / join, per the
> piles; (2) the waist member-walk — the floor's actual fact-kinds and
> verbs, discovered through the moves against what the repo already grew,
> never guessed in the abstract; (3) doc convergence — every ruling lands in
> the docs same turn, in Sid's register, so he ends the phase holding the
> full picture. Gathering and code-understanding run through Codex,
> adversarial by default: before Sid rules any retire/strip row, Codex gets
> a falsification pass on it (prove it live/reachable), packaged via the
> ask-codex-for-feedback skill and ingested via ingest-codex-feedback. The
> phase's finish line, his words: "large parallelisation and concurrent
> ui/ux and all other product work above the waist."
