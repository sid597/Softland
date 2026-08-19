# Waist cleanup — the task (v3, 2026-08-19)

**What this is.** The full task as ruled in the 2026-08-19 review sittings
(Sid + Fable + two hostile reviews: a sibling Fable session and two Codex
rounds), for a fresh execution or falsification session. v3 lands Sid's
final word — ALL LIGHTS OUT — and repairs two dependency claims v2 got
factually wrong (comment matches mistaken for requires; Codex caught it,
re-verified this session with ns-form greps). Direction is SID-RULED —
reopened only by his word.

## Sid's words (verbatim — the ground of the task)

- "the waist line is the holy grail now and we will first get the current
  codebase to that state and only then start building on top"
- "WE need a pristine code that is aligned with what softland is, what is
  it for, and the waist line"
- "The only hard constraint for me is that the data that i wrote down is
  not lost" · "the data should be saved as it is … what we do later is
  that it will get maybeee migrated to new whatever the format is"
- "There is nothing that we need to edit."
- "I am fine with removing both read and write not keeping anything that
  is not needed all lights go out i am fine we will build things correctly
  how is it even acceptable to keep the single atom survives"
- On "old editors go when the new one carries the job": "no fuck no they
  go now" (the clause was session prose, never his word — git blame
  `0d77cf8`, no (Sid) stamp; burned).
- On halo/inspector: "the form they are now currently also makes them
  useless the concept is what we need and we have that as smalltalk-vm"
- On the transcript pipeline: "we do want this its not plugged in but i
  think a core thing that will be used later and will also be modified
  accordingly imo but not be used for some time"
- From the census phase (verbatim, same day): "i do want to get to the
  state where large parallelisation and concurrent ui/ux and all other
  product work i can do above the waist … has to be done"

## The ruling in plain words (SID-RULED — do not reopen)

1. There is no new editor to build. Nothing waits for a replacement
   before it can be deleted.
2. Every file must pass one test: it is one of the four floor pieces, the
   plumbing under them, a kernel, or material above the waist. Whatever
   fails is deleted or rewritten as material.
3. **All lights out — writing AND reading.** The product client goes dark
   entirely. No thin read driver, no input pump, no console read surface.
   "we will build things correctly."
4. Only when the whole tree (outside the parked groups) passes do we
   start building on top.
5. Hard rule: the data already written is never lost — preserved AS IS
   (migration, if any, is a later act). Enumerated so an executor can't
   satisfy the words while losing a kind: block/object text · edit
   revisions and decisions · turns · placement cells · camera cells ·
   face wear · relations · imported material · transcript file-state
   offsets. Identities preserved; every kind reads back after a full
   app + cluster restart.

## What the app IS at the end of this task

- The server: jetty + the episode/matter routes + the five deployed
  kernels. Agents/CLI keep writing turns, blocks, geometry; everything
  acked durable. This is the whole running product during the dark.
- The floor + parked engine survive as guarded code, not as a running
  product surface: shaper (`text_shaper.cljs` · `text_layout.cljc` ·
  `runtime/fonts.cljs`, guarded by `npm run verify:text-layout`) and the
  parked scene+GPU machine + render families (guarded by
  `npm run verify:render-engine` after its repair, below).
- The product client (Electric boot, canvas loop, editors, faces, input,
  read wire) is deleted. The dev/prod entries reduce to server start;
  what remains of the client tree compiles only through the verifier
  build and test fences.

## Step 0 — land the law (DONE 2026-08-19, this sitting)

`docs/decisions.md` "The editing waist" tail replaced: the pristine-build
/ old-editors-go-when clause is burned (session prose); the cleanup-first
+ all-lights-out ruling stands in its place. The board's below-the-waist
close line ("product work may fan out now") is superseded — cleanup
first; electric-native road 1a queues behind it.

**Census supersession:** `census-and-moves.md` stays the historical
receipt of 2026-08-19. Its forward-looking conclusions (Row 4 whole-pull
demotion road, Row 5 T2-as-edit-floor, hoist/melt plans) are dated
testimony; where they collide with this task, this task wins.

## The test (every file, four doors) — and the parked exception

kernel · plumbing under the floor · one of the four floor pieces (shaper,
edit verbs, validators, commit artery) · material above the waist.
Anything that is none of these: delete, or rewrite as material.

**Parked, by Sid's word: the scene + GPU machine and the plug-in render
families** (scene_store/scene_runtime/rect_tree/containers/scene_tape/
frame_* substrate/webgpu pipelines · chrome/connector/path/image/region3d/
effects families incl. their runtimes, `selection.cljc`,
`region3d_pointer.cljc`, `snap.cljc`). Outside this task's certification:
not cut, not ruled floor, not walked through the doors. Their live door
(the product client) dies with this task; the verifier build is their
guard until their own later ruling.

## Step 1 — dead cuts (each needs its taken-path proof at cut time; grep
alone declares nothing dead — census law)

- `workspace/trail_face/{wiring.cljs,sanitize.cljc}` — Row 1 REFUSALS;
  cutting them is a NEW row: fresh proof, read Row 1's refusal reasons.
- the trail lanes + trail atoms (`file_viewer.cljc`, `runtime/state.cljs`)
- the font-atlas boot fetch (`runtime.cljs`)
- `/api/dev/replay-fixture` route · `/faces/*.edn` static serving
- drafts, tree only (git keeps): `rama/text_kernel.clj` ·
  `rama/dogfood/{space,compute}.clj` · **`rama/kernel.clj` + its
  kernel_shape test** — v2 wrongly ruled kernel.clj alive on comment
  matches; VERIFIED this session: zero executable requirers (llm.clj:32
  and dogfood/transcript.clj:35 cite it in prose only). Sid's original
  mark restored: goes.

## Step 2 — the monolith cut (one coherent pass; all lights out)

**Goes, with its callers** (zero-callers law):
- Ground's body: `ground.cljs` · `ground_edit.cljc` · `agent.cljs`
- T2's body: `editing_runtime.cljs` · `text_editing.cljc` ·
  `editing_segmentation.cljs` · `t2_block_wiring.cljs` ·
  `t2_block_join.cljc`
- `live_atoms.cljs` (T2 boot + flag composer) · `live_edges.cljc` ·
  `seam_demo.cljs` (see note below)
- the whole input tree: `events.cljs` · `runtime/{keyboard,mouse,touch,
  scroll,state}.cljs` · `runtime.cljs` and `runtime/render.cljs` boot glue
- the client artery implementation: `block_edit.cljc` ·
  `edit_transport.cljc` · `block_edit_wiring.cljs`
- **the read wire, whole** (Sid: "how is it even acceptable to keep the
  single atom survives"): `face_wiring.cljs` · `file_viewer.cljc` ·
  `electric_flow.cljc` (client main AND the server e/defn doors — zero
  callers once the client dies) · `global_flow.cljs`
- halo/inspector/portal console halves — "the concept is what we need and
  we have that as smalltalk-vm"; the concept lives in the organs and the
  law, not in this wiring.
- `runtime/fonts.cljs` STAYS (shaper floor). `design_tokens.cljc` stays
  (material data).

**Face split (VERIFIED requirers, ns-forms only):**
- `face_assembly.cljc` LIVES — required by the fenced
  `assembly_adapter.clj` and by `scene_store.cljc` (parked machine): a
  pure shared interpreter; passes the below-waist test.
- `face_primitives.cljc` SPLITS — `assembly_adapter.clj` requires it, but
  the file mixes that shared registry with Ground widgets (caret,
  selection wash, folds, refusal strip). At cut: keep what the adapter
  compiles against (named split, executor draws the line by requirer
  need), the product builders go with Ground. "Stays regardless" (v2) was
  wrong — material_portal.clj / anatomy_material.cljc matches are prose.

**The transcript question (Sid's word, answered):** two pipelines exist.
The REAL one is live and fenced: the four adapters + the transcript-ops
module (file offsets, defined in the object-container namespace) + the
helper fns in `dogfood/transcript.clj` that the episode lane and watchers
call today (episode.clj:46, ingest_watchers.clj:22 — executable requires).
The DUPLICATE is the never-deployed draft pair: `dogfood/transcript.clj`'s
own module def + `dogfood/transcript_ingest.clj`. Per Sid ("a core thing
that will be used later and will also be modified accordingly"), the
draft pair is **PARKED, not deleted**: `transcript.clj` is retained whole
and explicitly relabeled live-helpers + parked-draft-module;
`transcript_ingest.clj` parks beside it. Executable endpoint, no split
move needed.

**Method for live cuts:**
1. trace every live caller
2. name what goes dark (all of it: typing, reading, canvas)
3. preserve the ruled duties: the data, the routes, the kernels
4. repair compile + tests + **the verifier**: `verifier.cljs` requires
   editing-runtime, text-editing, live-atoms, live-edges (going) AND
   chrome-runtime, frame-runtime (parked). Strip the editor-dependent
   lanes; keep it guarding the parked machine and families.
5. name the runtime-context path for the routes with `file_viewer.cljc`
   gone (its boot delay holds oc-rt/machine-cut ctx + non-cluster WAL
   replays + first-pull distill; cluster mode has WALs off and the live
   distill is episode.clj's own post-turn road — each duty gets a keeper
   or a dead-ruling with receipt)
6. cut — one receipt per row/group
- `rama/trail_view.clj` goes with this pass (VERIFIED: mirror-pstates
  only, no depots, no unique data — the hard rule does not protect it;
  its only readers die here). Module undeploy is the named cluster op in
  the same row.

**Note on seam_demo** (Sid asked what it is): a fixture page behind three
URL flags that demos the render engine's seam package — it was the
pending acceptance surface for render Package 3 Atom B (board). All
lights out means no surface can drive it; it goes, and that acceptance
drive is formally abandoned — when the parked render world gets its later
ruling, acceptance evidence comes from the verifier or a rebuilt surface.

## Untouched throughout (the fence — named exactly)

- **The five deployed modules** and all PStates: object-container (+
  transcript-ops + transcript-identity + the four adapters) ·
  relation-kernel · face-arsenal · the object-container runtime adapter
  (`object_container/runtime.clj` — the admission road: depot → validate
  → decide → revisions → keyed truth; the artery's surviving floor).
  (trail-view leaves the fence by the no-data receipt above.)
- **Validators, named**: `object_container.clj` validate (~575-622) ·
  edit-effects (~1414) · decide (~2321-2483).
- **The shaper**: `text_shaper.cljs` · `text_layout.cljc` ·
  `runtime/fonts.cljs` (+ `text_layout_planes.cljc`, its internal).
- **Serve registry** (Sid: "i am fine with it"): `face_projection.clj` ·
  `block_distiller.clj` — goes dormant (no live caller during the dark),
  kept as the material system's server half.
- **Material**: the 18 `src/app/shared/*` files — walked through the
  doors at execution; expected material. No recutting (nothing to recut
  against until building).
- **The server material organs, named**: facet_master · material_truth ·
  material_circulation · machine_cut · verb_release · code_atoms ·
  cascade · material_portal.
- **The episode lane**: `server_jetty.clj` · `episode.clj` + the
  transcript helpers they require. The only write surface during the
  dark.
- **Parked**: scene + GPU machine · plug-in render families · the draft
  transcript pipeline.

## Quarry (what this section is: it deletes NOTHING and keeps NOTHING —
three git-history pointers so the future builder digs instead of
re-deriving)

`text_editing.cljc` (op kernel — already emits insert/delete/replace on
grapheme-safe offsets) · `block_edit.cljc` + `edit_transport.cljc`
(envelope id law · queue/sequence law) · `t2_block_join.cljc` (keyed-truth
join). The cut commits are the bookmarks.

## Done when — five close scenarios, exact receipts

Cluster mode for all receipts: the normal external Rama cluster
(LAND_CLUSTER unset — the default boot; NOT LAND_CLUSTER=0 in-memory).

1. Server compiles and boots clean: `clj -M:dev` starts jetty with no
   client bundle errors (dev entry reduced to server start).
2. `npm run verify:text-layout` green (shaper floor guard).
3. `npm run verify:render-engine` green after the verifier repair (parked
   machine + families still guarded).
4. An agent writes through the routes — one turn (`/api/episode/
   utterance`), one block (`/api/episode/block-birth`), one geometry
   settle (`/api/episode/geometry`) — each acked durable.
5. Full restart (app JVM + Rama cluster processes): every hard-rule kind
   reads back by point read through `object_container/runtime.clj` /
   kernel readers at the REPL — text, revisions, decisions, turns,
   placement cells, camera cells, wear, relations, offsets. One list
   names the door of every surviving non-parked file.

Then, and only then, building on top begins — a new act, not this task.

## Explicitly not doing

No contract session. No new editor. No verbs yet (empty slot; built when
the first thing on top needs it). No artery re-graining (the server
contract is untouched). No material-row recutting. No kernel changes. No
parked-group rulings. No data migration (preserve as-is).

## Open calls

None. The hard thinking is closed; everything above is ruled or is an
execution-time boundary with its receipt named.

## Review findings ledger (so nothing resurfaces)

Pulled from Codex round 2 (verified before landing):
- kernel.clj dependency claim was FALSE (comment matches) — kernel.clj +
  shape test now go. v2's "re-verified" header was too strong; v3's
  dependency claims are ns-form greps only.
- face_assembly lives / face_primitives splits (real requirers named).
- "Split alone is not an executable endpoint" → transcript.clj retained
  whole, relabeled; no split move.
- Open calls reduced to zero; pump/driver question dissolved by Sid's
  full-dark word; close scenarios now carry exact commands + cluster mode.

Refused (with reasons):
- "Re-grain the whole-pull now" — moot: Sid ruled the read wire GOES
  whole; further than re-graining.
- "Material rows recut now" — recutting is building; no grammar to recut
  against; no Sid word.
- "A fresh one-page pump occupies the input slot" — Sid ruled darker:
  no pump, nothing survives the input tree.
- (v2's own refusals of "face_wiring goes whole" and "reading is already
  full dark" are WITHDRAWN — Sid ruled both, this sitting.)

## For the falsification session

Attack, in order of value:
1. The file_viewer boot-duty walk (step 2.5) — every duty in its delay
   named keeper-or-dead with receipt; the routes' oc-rt path with it gone.
2. The face_primitives split line — does the adapter-needed part drag
   Ground widgets back in?
3. Step-1 taken-path proofs — trail-wiring vs Row 1 refusal reasons;
   kernel.clj's zero-requirers re-check at cut time.
4. The hard-rule enumeration — every write path this task touches reaches
   no kernel/PState.
5. The verifier repair boundary — exactly which lanes strip; parked
   machine still guarded.
6. The shared-18 walk — all pass as material?
7. Dev/prod entry reduction — what does `dev.cljc` become with no
   e/boot-client; does jetty still serve anything static it shouldn't.

Do NOT reopen: the ruling section, the hard rule, the four-door test,
the parked groups, all-lights-out. Findings against them file as flags
for Sid, never as edits.

Pointers: `census-and-moves.md` (historical receipt) · `docs/decisions.md`
"The editing waist" (floor of four + cleanup-first tail, landed this
sitting) · the 2026-08-19 review session (map, verdict table, dependency
receipts — in chat).
