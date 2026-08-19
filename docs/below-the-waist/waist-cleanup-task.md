# Waist cleanup — the task (2026-08-19)

**What this is.** The full task as agreed in the 2026-08-19 review session
(Sid + Fable), written down to hand to a fresh execution session or a
falsification session. The direction is SID-RULED — reopened only by his
word. Everything marked derived or open-call is fair game for falsification.

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
  the same day as session prose, with no inline (Sid) stamp.
- From the census phase, same day, verbatim: "i do want to get to the
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
4. Only when the whole tree passes do we start building on top.
5. Typing in the app may go dark while this runs; that is accepted.
6. Hard rule: the data already written — blocks, turns, edits — sits in
   the kernels and is never lost by any of this.

## Step 0 — land the law (WAITS ON SID'S YES; not yet landed)

`docs/decisions.md` "The editing waist" still carries the superseded tail
("The next editor is built pristine — fresh beside the old ones … the old
editors go when the new one carries the job"). A fresh session must NOT
inherit that frame — it is session prose, burned by Sid 2026-08-19. On his
yes, replace that tail with:

> The clean-up comes first (Sid, 2026-08-19). There is no new editor to
> build, and nothing waits for a replacement before it can be deleted.
> First the codebase we already have gets cleaned until every file passes
> one test: it is one of the four floor pieces, the plumbing under them, a
> kernel, or material above the waist. Whatever fails the test is deleted
> or rewritten as material. Only when the whole tree passes do we start
> building new things on top. Typing in the app may go dark while this
> runs; that is accepted. Hard rule, unchanged: the data already written —
> blocks, turns, edits — sits in the kernels and is never lost by any of
> this.

Also fix the board (`docs/next-prompt.md`): the below-the-waist close line
says above-waist product work may fan out now; under this ruling the
cleanup comes first, and the electric-native "NEXT: road 1a" line queues
behind it.

## The test (every file, four doors)

kernel · plumbing under the floor · one of the four floor pieces (shaper,
edit verbs, validators, commit artery) · material above the waist.
Anything that is none of these: delete, or rewrite as material.

## Step 1 — dead cuts (derived: nothing live reaches these; falsify each)

Nothing on screen changes. Each cut still needs its own taken-path proof
at cut time — the census's own law: grep alone declares nothing dead.

Client:
- `src/app/client/workspace/trail_face/{wiring.cljs,sanitize.cljc}` — the
  retired trail-face's leftover pull lane. NOTE: these were explicit
  REFUSALS in census Row 1 (that row's scope was only the drawing island).
  Cutting them is a NEW row: fresh proof, and read Row 1's refusal reasons
  first.
- the trail pull lanes in `src/app/file_viewer.cljc` and the trail atoms
  in `src/app/client/workspace/runtime/state.cljs` (driving state never
  written; split atoms unread — census trace)
- the font-atlas boot fetch in `src/app/client/workspace/runtime.cljs`
  (feeds only the trail sanitizer)
- the face-list pull lane in `face_wiring.cljs` + its `:!face-list` mirror
  in `runtime/state.cljs` — ships a roster nothing displays
- the `/face` command parser in `face_wiring.cljs` — zero callers
- the unread epoch mirror in `face_wiring.cljs`
- dead keybinds in `events.cljs` — Ctrl+arrows parsed with no handler;
  shift flag carried, never read

Server:
- `/api/dev/replay-fixture` route in `src/app/server_jetty.clj` — no caller
- `/faces/*.edn` static serving — the client fetch was retired; the server
  reads disk directly
- never-deployed drafts, tree only (git keeps):
  `src/app/server/rama/text_kernel.clj` and
  `src/app/server/rama/dogfood/{space,transcript,transcript_ingest,compute}.clj`.
  `dogfood/llm.clj` STAYS — runs hot in-memory by design.

## Step 2 — monolith cuts (direction SID-RULED; boundaries are open calls)

Typing on the canvas goes dark here. Accepted (ruling, point 5).

- Ground's editor body: `ground.cljs` + `ground_edit.cljc` — keymaps, edit
  machine, caret, selections, paste, gestures, presentation policy.
- T2's editor body: `editing_runtime.cljs`, `text_editing.cljc`,
  `editing_segmentation.cljs`, `t2_block_wiring.cljs`.
- The semantic key tables: the parse in `events.cljs`, the char/caret
  table in `block_edit.cljc`, `runtime/keyboard.cljs`, Ground's dispatch.
  What stays of the input spine: raw listeners only — browser events in,
  nothing interpreted.
- The hand-wired faces: `live_edges.cljc`, `seam_demo.cljs` (open call
  below), the wired console faces in `face_wiring.cljs` (open call below).

Per-cut discipline (kept deliberately, even under pushback — this codebase
shipped broken cuts THIS WEEK when it was skipped; census Row 5's first
pass broke three clauses and needed hostile review to catch them):
1. prove nothing live reaches it (taken path, not grep)
2. move any duty it still carries
3. repair its consumers
4. delete — one receipt per cut, committed with the cut

## THE READING QUESTION (correction + open call — falsify this hardest)

Earlier chat drafts of this task said "reading stays lit." Too quick.
CHECKED: today the conversation's pixels are produced by `ground.cljs`'s
own reconcile (served truth → scene slots). Cut Ground whole and the
canvas draws nothing — reading goes dark too, even though the scene/GPU
floor and read wire beneath are intact and the data is safe.
The census's own floor-verb list already includes "compile faces-as-data
into scene entries" — meaning a serve→scene compile IS floor, not editor
body.
LEAN (Fable): during the Ground cut, extract/keep exactly that one job —
served material still draws, read-only, through floor code.
ALTERNATIVE (cheaper, darker): full dark until building on top; console/
inspector become the only read surfaces.
Sid's call; default = the lean.

## Untouched throughout (the fence)

- the five deployed kernels + all PStates — the data; the hard rule
- the floor: shaper (`text_shaper`, `text_layout`, `fonts`) · validators ·
  commit artery (envelope, transport, wiring, submit, truth echo). Artery
  grain (whole-text) is NOT changed in this phase.
- the read wire, including the whole-pull, whose DEMOTE-LATER duties are
  frozen (census Row 4) — this cleanup must not violate that freeze
- scene + GPU machine (its own 2026-08-19 ruling: keep the floor)
- material rows and the server-side material organs
- episode-lane routes (utterance, block-birth, geometry) — agents/CLI can
  still write while the canvas is dark

## Open calls (leans given; one-liners to Sid only where a call feels his)

- `face_primitives.cljc` / `face_assembly.cljc` — compile face data into
  drawables (enactor-shaped; serve the material rows). Lean: stay.
- `agent.cljs` (send-road client) — dark with typing anyway. Lean: goes
  with Ground; the HTTP route is the surviving send surface.
- halo / inspector / portal — working debug surface, but above-waist
  non-material; the gesture entry dies with Ground, console calls remain.
  Lean: keep console halves, cut gesture wiring.
- `seam_demo.cljs` — it is the PENDING acceptance surface for render
  Package 3 Atom B (board). Lean: hold until Sid's accept/reject drive,
  then cut.
- `t2_block_join.cljc` — the artery's real-block join proof (built this
  week); its floor half lives in artery/kernel files. Lean: the client
  bookkeeping goes with T2's body; the artery keeps the proven contract.
- the empty verbs slot on the floor — build it only when the first thing
  on top needs it; clean code with zero callers is dead code. Lean, not
  ruled.

## Done when (Sid's own words are the gate)

Every file left is kernel / substrate / floor / material — and the state
his census verbatim names is real: product work above the waist can fan
out in parallel without being able to break the kernel. Then, and only
then, building on top begins — a new act, not this task.

## Explicitly not doing

No contract session. No new editor. No verbs yet. No artery re-graining.
No kernel changes.

## For the falsification session

Attack, in order of value:
1. The reading question — is the serve→scene compile actually separable
   from `ground.cljs`, or does extracting it drag the monolith back in?
2. Every Step-1 "dead" claim — taken-path proof, runner/build boundary at
   cut time; the trail-wiring refusals re-checked against Row 1's reasons.
3. "Agents can still write while dark" — do the routes work with Ground
   gone (any client-side dependency in the request path)?
4. "Data never lost" — enumerate every write path this task touches and
   show none reaches a kernel/PState.
5. The fence — does any Step-2 cut drag a fenced file (shared namespaces,
   test suites, build config)?

Do NOT reopen: the ruling section, the hard rule, the four-way test —
those are Sid's. Findings against them file as flags for Sid, never as
edits.

Pointers: `docs/below-the-waist/census-and-moves.md` (rows, refusals,
frozen duties) · `docs/decisions.md` "The editing waist" (floor of four;
tail superseded — see Step 0) · the 2026-08-19 review session (map +
verdict table, in chat).
