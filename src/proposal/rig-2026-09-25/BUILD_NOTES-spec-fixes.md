# Build notes: the fixes for H-1 and M-1

Builder for builder C, Claude Opus 5.5, 26 September 2026. Branch
`rig-build-spec-fixes`, worktree `/mnt/data/projects/Softland-rig-build-spec-fixes`,
fast-forwarded to `dee0320a` (step 6b landed) before the plan, so every
change sits on 6b's code. Plan: `PLAN-spec-fixes.md` (validated by reading,
its last section). The rama skill's build steps were followed in one
session: implement, validate the implementation by reading, write the
tests, validate them by reading, one run.

## Receipt

RECEIPT-PENDING

## M-1: the micro gate locks from the one read it decides by

**Built.** Block 1a's arrival part now does the lock work right after the
reads its checks are made from: `row-wraps` (6b's 4-arity with the
subjects `arrival-open` computed), fresh nonces, `fact-rows` over the
person entries the person check read, and the keyed digests with 6b's
hints. The finished rows and keyed digests join 6b's attempt-local
handoff (`$$micro-arrivals`, now eight fields). Block 2b reads them there:
it no longer routes back to the arrival task, reads no lease row and no
`$$persons` entry, and opens nothing. A new pure `lock-failed?` marks a
sealed act whose checks passed but whose rows lack a lock record or a
digest; the skeleton carries it, and the fold answers such a would-be yes
with the unrecorded face `:gate-error`, before any other write of the
decision (no record, nothing consumed), as the stream gate's `locks/fail!`
does.

**What it changes.** A person forgotten before block 1a's read is refused
as the stream gate refuses (the lease writer: the face `:no-such-lock`; a
wrap person: `:person-forgotten`). Forgotten after it, the value was
wrapped under the lock that was live at the read, so it reads "erased on"
the forget's date through `wrap-closed`, or opens when the person is not in
its wrap: scenario A3's group note about no one, leased by Bob, survives
Bob's forget, where the old 2b wrote it `:lock nil` and lost it to
everyone. `:does-not-open` means again only what `locks/open-with` says. The
person purge finds such a row, since its record names the person.

**Why this shape.** It is road 1 of 6b carried one step further: 6b carried
the subjects so block 2b applies no grammar; M-1 needs 2b to read no
person either, so block 1a carries the finished rows, and 2b derives
nothing. The handoff is 6b's (memory, one attempt), so the rows cost no
disk; 2b saves its seeks of the lease rows and persons. A refused act's
wrap is work thrown away, which PV-F6's plan had already accepted.

## H-1: an act into a working layer goes to the gate that orders it

**The door** (`micro_client.clj`). `layer-class` reads the class an
offerer names, per act (the stream settings a one-owner layer has; a
re-class changes them); `offer-into!` offers a built act through the gate
its name's tag names (`gate-for`) and answers in the stream door's shape,
throwing when the micro gate gives no answer; `lookup-many` reads this
gate's records under many names in one query (`micro-lookup-many`, new in
`micro/declare!`). `rx/connect` now builds on `mc/connect`.

**The micro gate** (`micro.clj`), three places it did not yet decide as the
stream gate does, each needed for an agent's entry to be admitted there:
- W1-1 (R20): a lease by a writer who is no person is checked and sealed
  under the layer's person owner (`lease-persons`, `lease-under-of` using
  the stream gate's own `locks/lease-under`; `persons-to-check` 3-arity; the
  skeleton's `:lease-under`; the mint). Before, an agent's or a tool's
  lease there was refused `:no-such-person`.
- F1: a read entry is stamped after its moment. `arrival-open` hands on
  each read entry fact's `{:moment :max-stamp}` parts
  (`entry-moment-parts`), the skeleton carries them, and `micro-decision`'s
  8-arity passes them to `gate/stamp-for` as its `opened` map, so both
  gates stamp by one `reads/entry-moments`.
- FRR5: `close-ok?` takes the close's `:reads :keep | :drop` part.

**The exit and its neighbours.** `read!` checks the call, reads the
working layer's class, names the entry with it, queries, and offers the
entry by `offer-into!`; `entry-offer` takes its class from its name.
Standing reads name every line with the class read for it and offer by
`offer-into!`. The exit's `close-session!` names its class and offers the
same way. `forget-entry-offer` takes the layer's class in force;
`drop-reads!` offers each forget by `offer-into!`, whose `gate-for` sends a
stream-era target to the stream gate (M25) and a micro-era one to the
micro gate.

**The maintenance reads' micro era** (`shared_reads.clj`,
`read_exit.clj`, `standing.clj`). `standing-close` also reads the micro
era's last delivery and opening lines of the entry (a tail read on the
layer's task, a hop to the entry entity's task to open them, the micro-era
line taken over the stream era's); new queries `micro-entry-ids` and
`micro-standing-open` page `$$micro [W :ix-ke]` on the layer's task and
check each candidate's session where its record is (a hop per candidate);
`session-closes` also lists the micro era's closes at or below the settled
frontier; `drop-reads!` drops a micro-era close's stream-era entries whole
and its micro-era ones by the close's batch; `standing/close-session!`
closes `(stream open minus micro closed) plus micro open`.

**The runner** (`recipe.clj`, `runner.clj`). `run-name`'s 4-arity tags the
run with the layer's class (the id unchanged); the runner offers by
`offer-into!` and counts a match as run when its name under either tag has
a record (`answered-runs`: `c/lookup-many` for the `:by-layer` tags and,
in a layer by entity, `mc/lookup-many` for the `:by-entity` ones;
`run-record`).

## Divergences from the plan

1. **The micro-era maintenance reads do not bound by the layer task's
   frontier.** The plan read only entries "at or below the frontier on the
   layer's task". Tracing a close right after a delivery showed that bound
   is wrong: a door sees a line answered at its name task's frontier, and
   the layer task's frontier can still trail that batch, so the closing
   values would miss the line just shown. Each read now takes visibility
   from where the row or record lives: a line whose row its entity task
   does not show yet gives way to the one before it (`standing-close` reads
   the last two lines of a kind), an opening or an entry whose record its
   name task does not show yet is not matched to a session (a later close
   finds it), and the drop's bound is the close's batch, which every task
   has committed once the close was answered. `session-closes`, which feeds
   `resume-drops!`, keeps the settled frontier.
2. **The skeleton's `:subjects` is removed.** 6b added it for block 2b's
   wrap; block 1a now wraps with `arrival-open`'s subjects on the same
   task, so nothing after the fold read it. It no longer travels to the
   leader.
3. The plan named `micro-lookup-many` visibility loosely; as built each
   name's record is taken at its own name task's frontier, as
   `micro-lookup` does with no F given.

## Validation by reading (the build's phase 4)

Every change was traced on the merged code; nothing was run before the one
run below.
- **Block 1a's dataflow.** The face branch binds the two new handoff
  fields nil; the offer branch binds them in both arms of the `<<if *due`
  (rows or nil, keyed digests or {}); `*ww` and `*nonces`, which hold the
  plaintext and the delivered locks, are bound in one arm only and never
  reach the handoff. The lease persons read feeds `locks/read-persons>`,
  seeded into the person check's read so no entry is read twice. Blocks 1b
  and 2b bind eight fields.
- **The fold.** `failed?` is computed after the reason, so a refused act is
  refused for its own reason; on `failed?` W is left as it came in and only
  the face is put. The mint's person is the skeleton's `:lease-under`, nil
  for an exempt writer and for a landing lease (bare), as before.
- **Legacy arities.** The 2-arity `persons-to-check` and the 4-arity
  `skeleton` keep the older lease rule, and `micro-decision`'s 6- and
  7-arity pass nil moments, so the pure fixtures of `micro_prepare_test`,
  `grammar_micro_test` and `review_fixes_test` see what they saw.
- **The door.** `write!` rebuilds a built map unchanged (`client/build` is
  idempotent on its own output), so an entry keeps its name, claimed-when
  and digest; a `:by-layer` name takes the stream door with no extra read;
  a store-placed forget costs one `settings-versions` read in `gate-for`.
- **Partitioning.** The new queries start with `(|hash *layer)` (the
  index), then `(|hash nm)` for `$$micro-names`; `standing-close` hops to
  `(|hash *ent)` before reading `$$micro [ent ...]`; `micro-lookup-many`
  hashes each name; block 1a's new reads are local (`$$persons` is on
  every task).
- **Totality.** Every new function a topology or query calls is pure and
  total or a guarded read; a malformed query argument is `{:refused
  :bad-read}` as data (`micro-ids-args`, `upkeep-args`, `valid-names`).

## Tests, and their validation by reading (phase 6)

`test/rig/store/spec_fixes_test.clj`, new. Pure:
`micro-close-takes-the-reads-part`;
`micro-lease-by-no-person-is-sealed-under-the-layer-owner` (the persons,
the check, and the fold minting under `:alice` for `:agent-a`);
`micro-yes-without-a-lock-is-the-face-gate-error` (no record, the face,
nothing consumed; and the same act locked is a yes that consumes its
lease); `micro-entry-is-stamped-after-its-moment`;
`micro-lock-work-comes-from-the-reads-the-decision-used` (M-1's two
outcomes over the steps block 1a runs: a mention of Bob reads erased at
Bob's forget date; A3's note opens after Bob's forget; the second read the
old 2b made gives rows `lock-failed?` refuses); `micro-lookup-many-takes-only-names`;
`a-run-name-follows-the-class-with-the-same-id`;
`micro-era-maintenance-reads` (the drop's candidates by batch, the page's
session filter and order, the crash close's opens and closes).
Cluster, `a-re-classed-working-layer`: the model's one-owner world, the
toy grammars, the bot's and a tool's permissions in `:alice-agent`; in the
stream era an entry, a standing read and a tool run; the re-class; then
the bot's pattern and point reads through the exit (rows shown, the entry
`:by-entity` and admitted at the micro gate, stamped after its moment,
its lock a lock row wrapped under Alice by W1-1, its value as the stream
gate records it); the standing read delivered and closed across the
re-class (`:deliveries` 2); a crash close of a micro-era opening, and no
crash close of the stream-era opening the micro era closed; the close
`:reads :drop` at the micro gate and the operator's drop of both eras (3
forgets; each erased with its date); `session-closes` and a rerun finding
nothing; Alice's own drop leaving a micro-era entry (For Sid 41 as built);
and the runner: the pre-re-class match not run again, a new one run at the
micro gate once. `micro_prepare_test`'s two PV-F6 pure cases now say what
the gate does with such rows, and one asserts `lock-failed?`.

Not tested, and why: M-1's race itself on a cluster (a person forget's
fan-out child between block 1a and 2b cannot be timed; its outcomes are
the pure cases above, and every micro write in the cluster test runs the
moved lock work); a point read of more than 256 ids into a re-classed
working layer (refused at its lease, M24; noted below).

## Costs

- Every act into a working layer reads the layer's class: one point read,
  one roundtrip; the exit's read goes from two roundtrips to three.
- An act into a re-classed working layer goes through the micro door: a
  lease act and the act, about two microbatch cycles (0.5 to 1 s with the
  250 ms tick), where the stream door pools 64 locks a lease; a read after
  a re-class shows nothing for that long. A pooled micro door would halve
  it; not built.
- At the micro gate an act cites one lease of at most 256 locks (M24), so
  a point read of more than 256 fact ids into a re-classed working layer is
  refused at its lease and shows nothing.
- The micro-era maintenance reads hop once per candidate to its record
  (the drop's pages and the crash close only); `standing-close` adds one
  settings read, and for a re-classed layer two tail reads and a hop.
- Block 1a wraps for acts later refused or answered from their record
  (nonces and seals, no I/O); block 2b's lease-row and person seeks are
  gone.

## Shared-file changes (for the merge)

- `src/rig/store/micro.clj`: `close-ok?`; `entry-moment-parts`,
  `arrival-open`; `lease-persons`, `lease-under-of`, `persons-to-check`;
  `skeleton` (6-arity; `:subjects` removed); `layer-settings`,
  `lock-work-due?`, `lock-failed?`; `micro-decision` (8-arity);
  `decide-envelope`; block 1a, 1b, 2b; `valid-names`; the query
  `micro-lookup-many`; docstrings of `delivered-locks`, `row-wraps`,
  `wrap-live`, `fact-rows` and the namespace.
- `src/rig/store/micro_client.clj`: `connect`; `layer-class`,
  `offer-into!`, `lookup-many`.
- `src/rig/store/read_exit.clj`: `connect`, `entry-offer`, `read!`,
  `close-session!`, `micro-entry-ids`, `forget-entry-offer`,
  `page-forgets!`, `drop-reads!`, `settled-frontier`, `session-closes`.
- `src/rig/store/standing.clj`: `fresh-line-name`, `line-offer`,
  `offer-line!`, `subscribe!`, `deliver!`, `close-entry!`,
  `close-session!`.
- `src/rig/store/shared_reads.clj`: the aggs require; `line-or`,
  `last-two-under`, `micro-line-entries>`, `micro-line-value>`,
  `standing-close>`, `micro-id-candidates`, `micro-ids-args`,
  `micro-id-rows`, `micro-ids-answer`, `micro-id-rows>`,
  `micro-prefix-entries>`, `micro-open-rows`, `micro-open-answer`,
  `micro-open-rows>`; the queries `micro-entry-ids`,
  `micro-standing-open`.
- `src/rig/store/recipe.clj`: `run-name` (4-arity).
- `src/rig/store/runner.clj`: `offer-run`, `answered-runs`, `run-record`,
  `output-act`, `run-tool`.
- Tests: `test/rig/store/spec_fixes_test.clj` (new);
  `test/rig/store/micro_prepare_test.clj` (two testing strings, one
  assertion). Run: `runs/spec-fixes-cmd.sh`, `runs/spec-fixes-run.txt`,
  `runs/spec-fixes-replays.txt`.
- Removed code, listed as asked (edits, no file deleted): block 2b's
  `|direct` back to the arrival task, its lease-row, unders and wrap-person
  reads, and its `row-wraps`, `fresh-nonces`, `fact-rows` and
  `kv-digests` steps (moved to block 1a); the skeleton's `:subjects`.

## For RIG.md

**Rig choices** (C assigns R numbers):
- The micro gate does the lock work in block 1a from the reads its checks
  use, carried to 2b in 6b's handoff; a yes it could not lock is the face
  `:gate-error` (M-1).
- An act into a working layer names the layer's class, read per act, and
  goes to the gate that orders the layer (`micro-client/offer-into!`);
  never a class the door holds (H-1).
- W1-1 at the micro gate: a lease by a writer who is no person is sealed
  under the layer's person owner, as at the stream gate.
- F1 at the micro gate: the leader sees a read entry's moment stamps, from
  the arrival task, and stamps after them; stamps travel as ids do (M3).
- A drop's micro-era cut-off is the close's batch; a close made in the
  micro era drops the session's whole stream era.
- The micro-era maintenance reads take visibility from where each row or
  record lives, not from the layer task's frontier.

**First-record placeholders:**
- FR3, FR4 (For Sid 3): an entry into a re-classed working layer is an
  ordinary act at the micro gate: name `[W :by-entity :offer uuid7]`, a
  record with `:batch`, stamped by the micro gate after its moment, its
  lock rows on the entry entity's task.
- T-FR5: a run's name follows its layer's class, `[L class :offer id]`,
  the id unchanged; a match counts as run under either tag.
- FRR5 at the micro gate: the close's `:reads` part, the same form at both
  gates.

**Questions for Sid:**
1. (The review's 1.) Should a read entry, a standing-read line, a drop's
   forget and a tool run follow the working layer's class and gate once it
   is re-classed? Built: yes. The alternative keeps entries on a
   stream-ordered layer, which changes default 4 ("recorded there").
2. (The review's 2.) When the micro gate cannot lock a value it would
   admit: built, refuse as the stream gate does (the face `:gate-error`),
   and lock from the decision's own read, so a forget after that read reads
   "erased on" its date. Left open: a value whose lease was sealed under a
   person forgotten before the gate's read is refused at both gates (the
   lease road's price), where the model, which has no leases, admits it.
3. For Sid 41, now live: the owner's forget of a micro-era value is refused
   (M14), so Alice cannot drop her own agent session's micro-era entries;
   the operator can. Should the owner's forget follow the layer (the model
   and the stream gate say the owner forgets in her layer)?
4. A read after its working layer's re-class waits about two microbatch
   cycles before it shows anything, and a point read of more than 256 ids
   there is refused. Acceptable for the re-class's hot layer, or should the
   micro door pool leases?
