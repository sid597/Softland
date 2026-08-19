# Waist cleanup — the task (v2, 2026-08-19)

**What this is.** The full task as agreed in the 2026-08-19 review session
(Sid + Fable), for a fresh execution or falsification session. v2 integrates
two reviews (a sibling Fable session and Codex) — every accepted finding was
re-verified against the tree this session (dependency greps); refused
findings are recorded at the bottom so they don't resurface. Direction is
SID-RULED — reopened only by his word.

## Sid's words (verbatim — the ground of the task)

- "the waist line is the holy grail now and we will first get the current
  codebase to that state and only then start building on top"
- "WE need a pristine code that is aligned with what softland is, what is
  it for, and the waist line"
- "The only hard constraint for me is that the data that i wrote down is
  not lost"
- "There is nothing that we need to edit."
- "fuck the previous laws" — said when a session-written clause ("the old
  editors go when the new one carries the job") was quoted back at him as
  his own. It never was his word: git blame shows it landed in `0d77cf8`
  same day as session prose, no inline (Sid) stamp.
- From the census phase (verbatim, same day): "i do want to get to the
  state where large parallelisation and concurrent ui/ux and all other
  product work i can do above the waist — everything below it right now
  is sync and linear to this — has to be done"

## The ruling in plain words (SID-RULED — do not reopen)

1. There is no new editor to build. Nothing waits for a replacement
   before it can be deleted.
2. First, the codebase we already have gets cleaned until every file
   passes one test: it is one of the four floor pieces, the plumbing
   under them, a kernel, or material above the waist.
3. Whatever fails the test is deleted or rewritten as material.
4. Only when the whole tree (outside the parked groups, named below)
   passes do we start building on top.
5. Typing in the app may go dark while this runs; that is accepted.
   (Reading is a separate, still-open call — see the reading question.)
6. Hard rule: the data already written is never lost. Enumerated, so an
   executor can't satisfy the words while losing the thing:
   block/object text · edit revisions and decisions · turns · placement
   cells · camera cells · face wear · relations · imported material ·
   transcript file-state offsets. Identities preserved; every kind reads
   back after a full app + cluster restart.

## Step 0 — land the law (WAITS ON SID'S YES; not yet landed)

`docs/decisions.md` "The editing waist" still carries the superseded tail
("The next editor is built pristine — fresh beside the old ones … the old
editors go when the new one carries the job"). A fresh session must NOT
inherit that frame — session prose, burned by Sid 2026-08-19. On his yes,
replace that tail with:

> The clean-up comes first (Sid, 2026-08-19). There is no new editor to
> build, and nothing waits for a replacement before it can be deleted.
> First the codebase we already have gets cleaned until every file passes
> one test: it is one of the four floor pieces, the plumbing under them, a
> kernel, or material above the waist. Whatever fails the test is deleted
> or rewritten as material. Only when the whole tree passes do we start
> building new things on top. Typing in the app may go dark while this
> runs; that is accepted. Hard rule, unchanged: the data already written —
> blocks, turns, edits, placement, camera, wear, relations — sits in the
> kernels and is never lost by any of this.

Also fix the board (`docs/next-prompt.md`): the below-the-waist close line
says above-waist product work may fan out now; under this ruling the
cleanup comes first, and the electric-native "NEXT: road 1a" line queues
behind it.

**Census supersession (part of step 0's honesty):** the census
(`census-and-moves.md`) stays the historical receipt of what existed and
ran on 2026-08-19. Its forward-looking conclusions — Row 4's whole-pull
demotion road, Row 5's T2-as-the-edit-floor, the scene-loop "hoist/melt
above" plan — are dated testimony now; where they collide with this
cleanup, this task wins. (The whole-pull still stays this phase — on its
own merits below, not because the freeze binds.)

## The test (every file, four doors) — and the parked exception

kernel · plumbing under the floor · one of the four floor pieces (shaper,
edit verbs, validators, commit artery) · material above the waist.
Anything that is none of these: delete, or rewrite as material.

**Parked, by Sid's word ("I will come to it later"): the scene + GPU
machine and the plug-in render families** (chrome/connector/path/image/
region3d/effects/frame runtimes — the accepted build-ahead packages).
They are outside this task's certification: not cut, not ruled floor,
not walked through the doors. They keep their own later ruling. This
task therefore certifies the tree MINUS the parked groups — saying
otherwise would silently resolve a decision Sid parked.

## Step 1 — dead cuts (each still needs its taken-path proof at cut time;
grep alone declares nothing dead — census law)

Client:
- `src/app/client/workspace/trail_face/{wiring.cljs,sanitize.cljc}` — the
  retired trail-face's leftover pull lane. NOTE: explicit REFUSALS in
  census Row 1 (its scope was only the drawing island). Cutting them is a
  NEW row: fresh proof, read Row 1's refusal reasons first.
- the trail pull lanes in `src/app/file_viewer.cljc` and the trail atoms
  in `src/app/client/workspace/runtime/state.cljs`
- the font-atlas boot fetch in `src/app/client/workspace/runtime.cljs`
  (feeds only the trail sanitizer)
- the face-list pull lane in `face_wiring.cljs` + its `:!face-list` mirror
  (ships a roster nothing displays)
- the `/face` command parser and the unread epoch mirror in
  `face_wiring.cljs`
- dead keybinds in `events.cljs` — Ctrl+arrows parsed with no handler;
  shift flag carried, never read

Server:
- `/api/dev/replay-fixture` route in `src/app/server_jetty.clj`
- `/faces/*.edn` static serving (client fetch retired)
- drafts, tree only, git keeps: `src/app/server/rama/text_kernel.clj` ·
  `src/app/server/rama/dogfood/{space,compute}.clj` ·
  `dogfood/transcript_ingest.clj` (check its requirers at cut)

Doors verified this session (receipts in the 2026-08-19 chat):
- **`dogfood/transcript.clj` — SPLIT, not a whole-file delete.** Its
  never-deployed draft module goes; its helper fns STAY — the live
  episode lane and read wire call them (`episode.clj`, `file_viewer.cljc`,
  `cluster.clj`, `git_spine.clj`, `ingest_watchers.clj`: harvest, paths,
  request-id, import-observations…, append-and-await…). The deployed
  transcript-ops module is a DIFFERENT thing — defined in the
  object-container namespace, holds the file-offset data, fenced.
- **`rama/kernel.clj` — LIVES.** Required by `dogfood/llm.clj` (stays,
  runs hot) and by the transcript helpers above. A dependency, not
  product. (One review said cut it, the other said llm needs it — grep
  says llm needs it.)
- **`dogfood/llm.clj` — stays** (hot in-memory by design; durable outputs
  land in deployed kernels).

## Step 2 — the monolith cut (one coherent pass, not per-file ceremony)

Typing on the canvas goes dark here. Accepted (ruling, point 5).

**Goes, one cut, with its callers** (zero-callers law: once the editor
bodies go, their client plumbing has no caller and keeping it violates
the test):
- Ground's body: `ground.cljs` · `ground_edit.cljc` · `agent.cljs` (send
  client; the HTTP routes stay)
- T2's body: `editing_runtime.cljs` · `text_editing.cljc` ·
  `editing_segmentation.cljs` · `t2_block_wiring.cljs` ·
  `t2_block_join.cljc`
- `live_atoms.cljs` — T2's boot and the flag composer (verified: requires
  editing-runtime, calls its boot). The parked render families lose their
  live door with it; that door returns with their own later ruling.
- `live_edges.cljc`
- the semantic key tables: the parse in `events.cljs` ·
  `runtime/keyboard.cljs` — plus the boot glue in `runtime.cljs` that
  arms all of the above
- **the client artery implementation**: `block_edit.cljc` ·
  `edit_transport.cljc` · `block_edit_wiring.cljs` — verified: their only
  requirers are the editor bodies and boot glue. The artery FLOOR that
  remains is the server side: the electric_flow server door, the
  object-container admission road (depot → validate → decide → revisions
  → keyed truth). No artery re-graining; the server contract is
  untouched. (This corrects v1, which froze these client files as floor.)

**Splits, not whole files:**
- `face_wiring.cljs` — its pull/wear arming STAYS (it is the read road's
  client arm); its face-list lane, /face parser, epoch mirror go (step
  1); console-face arming: see open calls.

**Method for live cuts** (Ground/T2 are live paths — "prove nothing
reaches it" is impossible and is the dead-cut test, not this one):
1. trace every live caller
2. name what intentionally goes dark (typing, block birth by keystroke,
   moves, send-from-canvas)
3. preserve the ruled duties (data, routes, the read road)
4. repair compile + tests + **the verifier** — verified:
   `substrate/webgpu/verifier.cljs` requires editing-runtime,
   text-editing, live-atoms, live-edges, chrome-runtime, frame-runtime;
   strip its editor-dependent lanes, keep it guarding the parked machine
5. cut

Receipts: one per row/group (not per file). Close proof is the five
scenarios at the bottom — no validation ladder.

## The reading question (Sid's call; default = the lean)

Ruled: typing dark. NOT ruled: reading. Verified this session: the
face-compile road already lives mostly OUTSIDE Ground —
`face_assembly.cljc` and `face_primitives.cljc` are required by FENCED
server organs (`assembly_adapter.clj`, `material_portal.clj`,
`anatomy_material.cljc`) and by the scene store, so they stay regardless
and the "extraction" is smaller than v1 feared. What `ground.cljs`
uniquely holds: the reconcile driver, the placement + camera restore
(~ground.cljs 2223-2258 — part of the job, NOT part of the body being
burned), and camera gestures.

- **LEAN (default):** read-lit. A thin fresh driver (pristine, never
  grown from Ground): served truth → compile road → scene slots, with
  placement + camera restored from their cells, plus a one-page raw
  input pump (wheel/drag → camera) so Sid can look around.
- **ALTERNATIVE:** full dark. Then `events.cljs`/`mouse`/`touch`/
  `scroll` go whole too, and the console/inspector (riding face_wiring's
  surviving half) is the only read surface.

One word from Sid picks.

## Untouched throughout (the fence — named exactly)

- **The five deployed modules** and all their PStates: object-container
  (+ its transcript-ops module — defined in the object-container
  namespace, holds the file offsets — + transcript-identity + the four
  adapters: transcript/markdown/clojure/assembly) · relation-kernel ·
  face-arsenal · trail-view · the object-container runtime adapter.
  NOTE on trail-view: verified mirror-pstates only — no depots, no
  unique data; its only readers die in step 1. It stays deployed this
  phase (Sid's blanket kernel fence); undeploy is a later one-word call.
- **Validators, named**: `object_container.clj` validate (~575-622),
  edit-effects (~1414), decide (~2321-2483); admission checks inside
  material grammars stay with their rows.
- **The shaper**: `text_shaper.cljs` · `text_layout.cljc` · `fonts.cljs`.
- **The read wire this phase**: `electric_flow.cljc` main ·
  `file_viewer.cljc` (minus its trail lanes) · `global_flow.cljs` ·
  `util_fns.cljc` epoch. The whole-pull STAYS — not because the census
  freeze binds (superseded above) but because it is the only reading
  road; re-graining it is building, not cleaning.
- **Serve registry**: `face_projection.clj` · `block_distiller.clj`.
- **Material**: the 11 `src/app/shared/*_material.cljc` rows and the
  other 7 shared files (18 total — walk all 18 through the doors at
  execution; expected: material). No recutting of rows in this task —
  that is building.
- **The server material organs, named**: facet_master · material_truth ·
  material_circulation · machine_cut · verb_release · code_atoms ·
  cascade · material_portal · the face entries in face_projection.
- **The episode lane**: `server_jetty.clj` · `episode.clj` + the
  dogfood/transcript helpers they call (see step 1's split). Agents/CLI
  keep writing turns, blocks, geometry while the canvas is dark.
- **Parked (outside certification, untouched)**: scene + GPU machine ·
  the plug-in render families.

## Quarry (git history pointers for the future verbs/editor builder)

The cut commits are the bookmarks. Dig here later: `text_editing.cljc`
(op kernel — already emits insert/delete/replace on tagged, grapheme-safe
offsets) · `editing_segmentation.cljs` (grapheme boundaries) ·
`block_edit.cljc` (envelope + id law) · `edit_transport.cljc`
(queue/sequence law) · `t2_block_join.cljc` (keyed-truth join) ·
`ground_edit.cljc` (splice + rejection-rewind behavior).

## Done when

Every file outside the parked groups passes one of the four doors, and
five close scenarios pass — no receipt ceremony beyond these:

1. Client + server compile clean; the verifier is green after its repair
   and still guards the parked machine.
2. Boot serves the land per the reading ruling (lit read-only, or ruled
   dark).
3. An agent writes a turn + a block + geometry through the routes, acked
   durable.
4. Full restart (app + cluster): every data kind in the hard rule reads
   back — text, revisions, decisions, turns, placement, camera, wear,
   relations, offsets.
5. One list names the door of every surviving non-parked file.

Then, and only then, building on top begins — a new act, not this task.

## Explicitly not doing

No contract session. No new editor. No verbs yet (empty slot; built when
the first thing on top needs it). No artery re-graining. No read-wire
re-graining. No material-row recutting. No kernel changes. No parked-group
rulings.

## Open calls (one word each, Sid's)

- **The reading question** — lean read-lit (above); your word picks.
- **`seam_demo.cljs`** — it is the PENDING acceptance surface for render
  Package 3 Atom B (board). Cutting it = abandoning that acceptance
  drive. Cut on your word, or drive it first.
- **trail-view undeploy** — no unique data, readers gone after step 1;
  later one-word call.
- **`src/components/design_tokens.cljc`** — token data; lean: keep as
  material candidate, classify at the doors.
- **halo/inspector/portal console halves** — they ride face_wiring's
  surviving half and are the only read surface under full-dark. Lean:
  keep console halves, gesture entry dies with Ground.

## For the falsification session

Attack, in order of value:
1. The reading extraction boundary — what does `ground.cljs` uniquely
   hold beyond driver + restore + gestures? Anything that drags the
   monolith back in?
2. Step-1 taken-path proofs — especially the transcript SPLIT line
   (which helpers are live) and `transcript_ingest.clj`'s requirers.
3. Routes-with-Ground-gone — walk the episode lane's request path and
   show no cut file sits in it; name the `runtime.cljs` boot glue that
   must be repaired.
4. The hard-rule enumeration — every write path this task touches,
   shown to reach no kernel/PState.
5. The verifier repair boundary — exactly which lanes strip, what still
   guards the parked machine.
6. The shared-18 walk — do all 18 really pass as material?

Do NOT reopen: the ruling section, the hard rule, the four-door test,
the parked status of scene+GPU/render families. Findings against them
file as flags for Sid, never as edits.

## Review findings REFUSED (recorded so they don't resurface)

- "face_primitives/face_assembly go" — refused: fenced server organs and
  the scene store require them (verified requirers listed above).
- "kernel.clj goes" — refused: llm.clj (stays) and the live transcript
  helpers require it.
- "Re-grain the whole-pull / read wire now" — refused: only reading road;
  re-graining is building.
- "Material rows recut for the new grammar" — refused: no Sid word in
  this session's record; recutting is building. If another session holds
  his word on this, it files as its own row with the receipt.
- "face_wiring.cljs goes whole" — refused: it carries the read road's
  client arm; it splits (named above).
- "The reading question is already resolved to full dark" — refused: Sid
  ruled typing dark; reading is his open call.
- "One receipt + one commit per small removal" (v1's own ceremony) —
  replaced by per-row receipts + the five close scenarios (one-pass law).

Pointers: `docs/below-the-waist/census-and-moves.md` (historical receipt;
supersession note in step 0) · `docs/decisions.md` "The editing waist"
(floor of four; tail superseded — step 0) · the 2026-08-19 review session
(map, verdict table, dependency receipts — in chat).
