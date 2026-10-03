# Plan: the fixes for H-1 and M-1 of the full-spec review

Builder for builder C, Claude Opus 5.5, 26 September 2026. Branch
`rig-build-spec-fixes`, worktree `/mnt/data/projects/Softland-rig-build-spec-fixes`,
made at `ad49bced` and fast-forwarded to `dee0320a` (step 6b landed) before
this plan was written. The findings are `REVIEW-full-spec.md`'s H-1 and M-1.
The rama skill was loaded (`SKILL.md`, `phases.md`, `phase-build.md`,
`phase-2-plan-validate.md`, `phase-full-spec-review.md`, `batch.md`), and
this plan is validated by reading, in its last section.

## Prep

Read: `REVIEW-full-spec.md` whole; `STARTER-6b.md`;
`BUILD_NOTES-tools-and-grammars.md` "Phase 6b"; `BUILD_NOTES-grammar-micro.md`
and 6b's diff of `micro.clj`; SPEC.md; RIG.md's Overnight state; the model's
`lock-for`, `erasure`, `wrap-closed`, `micro-commit`, `op-forget-value` and
scenario A3. The code: `read_exit.clj`, `standing.clj`, `client.clj`,
`micro_client.clj`, `runner.clj` whole; in `micro.clj` the intake, block 1a,
1b, 2a, 2b, 2c, `arrival-open`, `persons-to-check`, `skeleton`,
`micro-extras`, `decide-envelope`, `row-wraps`, `fact-rows`, `wrap-live`;
in `locks.clj` the primitives, `lease-under`, `persons-refusal`,
`value-writes`, `open-with`; in `shared_reads.clj` the addresses, the index
entry, `fact-writes`, `kv-digests`, `index-block>`, `open-entry>`, the
person purge and the maintenance reads; in `gate.clj` `refusal`,
`control-value-ok?`, `stamp-for`; `reads.clj` `entry-facts`,
`entry-moments`, `shared-layer?`, the read queries; `recipe.clj`
`run-name`. Nothing was run. 6b landed while this was read; the plan is
written against the merged tree, so "the places the fixes meet 6b" below
are places in landed code, not guesses.

Four things the review did not say, found while tracing its two findings
through the code; each is needed for the fix to do what the finding asks:

1. **The micro gate never applies W1-1** (R20: a lease by a writer who is no
   person is sealed under the layer's person owner). `persons-to-check`
   checks a lease act's `:who` itself, so an agent's or a tool's lease at
   the micro gate is refused `:no-such-person`, and its act then
   `:no-such-lock` on its face. H-1's reader is an agent; routing its entry
   to the micro gate is not enough while the gate refuses its lease.
2. **The micro leader does not count a read entry's moment in its stamp**
   (F1: an entry is stamped after what it read). The stream gate reads the
   moment from the value it opens (`gate/stamp-for`'s 5-arity); the micro
   leader holds no value (M3), so an entry decided there could be stamped
   before its moment.
3. **The micro gate refuses a session close's `:reads` part** (FRR5):
   `close-ok?` takes only `{:session s}` (the review says this too).
4. **The maintenance reads see only the stream era.** `entry-ids`,
   `standing-open`, `standing-close` and `rx/session-closes` read
   `$$layers [W ...]`. After a re-class, W's new entries, lines and closes
   are in the micro store, so the drop, the crash close, the closing
   values and `resume-drops!` would miss them (the review: "the close and
   the drop would need the same repair").

And one the fix must not decide: at the micro gate a value forget is the
operator's (M14), so a layer's owner cannot forget a micro-era value in a
re-classed layer. That is For Sid 41, already asked. The drop of a
re-classed working layer's micro-era entries therefore runs as the
operator, which `drop-reads!` already allows ("as the layer's owner or the
operator"); the owner's own drop of them waits for Sid's answer.

## M-1: the micro gate locks from the one read it decides by

**What the finding is.** Block 1a opens and checks each value on its arrival
task under the lease rows and person entries it reads there, and the fold
decides from that. Block 2b reads the lease rows and `$$persons` again, on
the same task, after the fold, and wraps from the second read. A person
forget's fan-out child can run on the task between the two reads (the
stream topology shares the task thread). If the lease writer was forgotten
in between, 2b cannot deliver the lock and writes the row with `:lock nil`;
if a required wrap person, or every any-of one, was forgotten, `locks/wrap`
gives nil and the row has no record. The answer stays yes, no ledger entry
exists, and every read shows `:does-not-open` with no date (`locks/open-with`
512-513 says no write produces that). 6b kept this: its 2b still re-reads
the leases and the live persons and wraps with the carried subjects.

**How the stream gate and the model decide.** The stream gate reads the
persons, decides and writes in one event; if a lock cannot be made at
decision it throws `locks/fail!`, which its guard turns into the unrecorded
face `:gate-error` ("so a value is never admitted half locked"). A person
forgotten before the event: the lease writer makes the delivery fail
(`:no-such-lock` on its face), a wrap person is recorded
`:person-forgotten`. Forgotten after: the value was wrapped under the live
lock and reads "erased on" the forget's date by `wrap-closed`. The model
locks at commit with the wrap as data (`lock-for`), so a value about a
person forgotten between prepare and commit reads `:erased-at` the
forget's stamp; A3's group note about no one survives both forgets.

**The fix.** The lock work moves into block 1a, right after the decision's
reads, and uses exactly the entries the decision used:

- In block 1a's arrival part, after `person-reason`: `row-wraps` (6b's
  4-arity, with `(:subjects arr)`, over `*lrows` and `*upersons`, the
  delivery block 1a already made), `fresh-nonces`, `fact-rows` over the
  persons block 1a read (`*upersons` merged with `*cpersons`, the union the
  person check read), and `shared-reads/kv-digests` with 6b's `*hints`.
  The plaintext and the delivered locks stay in the step (`*ww` is never
  materialized). The rows and keyed digests join 6b's handoff:
  `(materialize> *arrival *in *sk *carry *lstep *hints *frows *kvd :>
  $$micro-arrivals)`.
- A new pure `lock-failed?` over the act and its rows: a sealed act whose
  checks passed (status ok, no value reason, no person reason) but some
  value row has no lock record (in `:lock` or as its lock row) or no
  digest. It can only come from an internal failure now, since every wrap
  person is in the union the person check found alive in the same read.
  The skeleton carries `:lock-failed`.
- The fold (`decide-envelope`): an act that would be answered yes with
  `:lock-failed` gets the unrecorded face `:gate-error` instead, written
  before anything else of the decision (no record, nothing consumed), as
  the stream gate's `fail!` does. A refusal is unaffected.
- Block 2b reads `*frows3 *kvd3` from the handoff and no longer routes back
  to the arrival task, reads no lease row, reads no `$$persons` entry and
  opens nothing: `($$micro-arrivals :> ... *frows3 *kvd3)`, the name task's
  check, 2d and the row writes as before.

So a person forgotten before block 1a's read is refused as the stream gate
refuses; forgotten after it, the value was wrapped under the lock that was
live at the read and reads "erased on" the forget's date (`wrap-closed`), or
opens when the person is not in its wrap (the lease writer of A3's note).
`:does-not-open` again means only what `open-with` says it means. The
person purge finds the row, because its record names the person
(`live-for?`).

**What else changes and why it is the right shape.** The work a refused act
does moves from 2b to 1a; PV-F6's plan already accepted wrapping before the
verdict ("a few microseconds of work thrown away"). 2b's I/O falls: per
sealed act it no longer seeks its lease rows (one per cited lock) or the
persons (the unders and the wrap persons). 6b's handoff is attempt-local
memory, so the carried rows cost no disk; they hold the sealed bytes a
second time in memory for the batch (the parsed offer already holds them),
and, for a value about no one, the empty wrap's bare K that PV-F2 already
stores in the record and already carried to the entity task. The
skeleton's `:subjects`, which 6b added only for 2b's wrap, is no longer
read after the fold; it is removed from the skeleton so it stops travelling
to the leader (block 1a uses `(:subjects arr)` directly).

First-record: none new. The row a yes writes is the same row; only the
moment its wrap is made changes. The review's question 2 for Sid is
answered as "follow the stream gate" for the race, and its sub-question
stays open: a value leased under a person forgotten before the gate's read
is refused at both gates (the lease road's price), where the model, which
has no leases, would admit it.

## H-1: an act into a working layer goes to the gate that orders it, named by its class

**The rule the fix follows.** FR3 and default 4: a read entry is an
ordinary act in the reader's working layer. An ordinary act names its
layer's class and goes to the gate that orders the layer (SPEC phase 1:
"the class checked against the layer's class fact"; M13's dispatch by the
name's tag). After a re-class the working layer's class fact is
`:by-entity`, so its entries, lines, closes, drops and runs are
`:by-entity` acts at the micro gate. This is the simplest placeholder for
what an entry then holds, and it is first-record (below).

**H-1a, the door (rig.store.micro-client).**
- `layer-class [store L]`: the class an offerer names, read per act (not
  held: a re-class can happen during a session, and the door must not act
  on a stale class): `(:class (c/settings store L))`, the stream settings
  every one-owner layer has from its making act and a re-class changes;
  else this store's settings (a group). One point read for a working
  layer.
- `offer-into! [store offer]`: offers a built act through the gate its
  name's tag names (`gate-for`: `:by-layer` the stream door,
  `offer-until-answered!`; `:by-entity` this door's `write!`, which leases,
  seals, offers and waits; a store-placed act by its layer and target, as
  `gate-for` already does). Returns the stream door's shape `{:answer
  :reason :stamp :name}` (plus `:batch` from this gate) and throws when
  this gate gives no answer, as the stream door throws after its tries.
- `lookup-many [store names]`: this gate's records by name, visible at
  each name task's frontier, one query (new query topology
  `micro-lookup-many` in `micro/declare!`: explode, `|hash` each valid
  name, `visible-at?`, `|origin`, `+map-agg`), for the runner.
- `connect` adds `:lookup-many-q`.
- `rig.store.read-exit/connect` builds on `mc/connect` (it built on
  `c/connect`), and adds the two new maintenance queries' handles.

**H-1b, the micro gate (rig.store.micro).**
- W1-1 at this gate. Block 1a, on the arrival task, for a lease act by a
  writer who is not exempt: read `$$persons` for the writer and the
  layer's person owner (settings in force from 6b's layer visit, `(or
  *msettings *ssettings)`), and take `locks/lease-under` (the stream gate's
  own function). `persons-to-check` gains a 3-arity taking that person: a
  lease act checks it (`:no-such-person` when it has no entry,
  `:person-forgotten` when its lock is gone, the stream gate's reasons).
  The skeleton carries `:lease-under`; the fold mints the lease rows under
  it (a landing lease stays bare). The 2-arity `persons-to-check` and the
  4-arity `skeleton` keep the old rule for the pure fixtures that call
  them. The block 1a person read becomes `locks/read-persons>` seeded with
  the entries already read, so no entry is read twice.
- F1 at this gate. `arrival-open` adds `:entry-moments`, for each read
  entry fact (`reads/read-keys`) the `{:moment :max-stamp}` part of the
  value it opened, and nothing else of a value; the skeleton carries it;
  `micro-decision` gains an 8-arity taking it as `gate/stamp-for`'s
  `opened` map, so `reads/entry-moments` runs on the same parts at both
  gates. The leader sees an entry's moment stamps, never its read content
  (a rig choice beside M3: stamps, like ids, travel).
- FRR5 at this gate. `close-ok?` accepts `{:session s}` and `{:session s
  :reads :keep|:drop}`, the stream gate's form (`gate/control-value-ok?`).

**H-1c, the exit's acts (read_exit.clj, standing.clj).**
- `read!`: check the call, then read the working layer's class, then make
  the entry's name `(env/make-name W class)` (a given `:entry-name` is
  used as it is), then query, then offer the entry by `mc/offer-into!`.
  `entry-offer` takes the class from the name (`(nth nm 1)`), so a name and
  its act cannot disagree.
- Standing reads: each line's name is made with the class read for that
  line; `line-offer` takes the class from the name; `offer-line!` offers by
  `mc/offer-into!`.
- `close-session!` (the exit's, with `:reads`): the layer's class, offered
  by `mc/offer-into!`.
- `forget-entry-offer` takes the layer's class in force (a forget is placed
  by the store, so its name carries no class, but its `:class` is checked
  against the layer's class fact at either gate); `drop-reads!` offers each
  by `mc/offer-into!`, whose `gate-for` sends a stream-era target to the
  stream gate (M25) and a micro-era target to the micro gate.

**H-1d, the maintenance reads' micro era (shared_reads.clj, read_exit.clj, standing.clj).**
- The drop's cut-off in the micro era is the close's batch, not its stamp:
  the micro store's stamps come from per-task clocks and a moment can push
  an entry's stamp past a later close's, so "admitted at or before the
  close" is "decided in a batch at or below the close's". A close made in
  the stream era (its name `:by-layer`) precedes the re-class, so it drops
  stream-era entries by stamp as now and no micro-era entry; a close made
  in the micro era drops every stream-era entry of the session (all
  precede the re-class) and the micro-era ones by batch. A rig choice.
- New query `micro-entry-ids [*layer *session *before-batch *after *n]` on
  hash(W): one `$$micro [W :ix-ke]` page of the `read/` range as
  `entry-ids` pages the stream era (live `:read/*` entries, batch at or
  below the bound), then each candidate's act session from its name row
  (`|hash` the name, `$$micro-names [nm :answer :session]`), `|origin`,
  `+vec-agg`, the page's ids of that session in page order with the same
  `:next` and `:done?`. The session is not in a micro index entry (the
  entry is the row plus its own fields); a hop per candidate, on a
  maintenance read, is cheaper than widening every index entry and its
  rebuild.
- New query `micro-standing-open [*layer *session]` on hash(W): the micro
  era's `:read/standing` and `:read/closed` ranges; `{:open [ent ...]
  :closed [ent ...]}`, the open ones filtered by their opening act's
  session with the same hop. `standing/close-session!` closes `(stream
  open minus micro closed) plus micro open`, when W's class is by entity.
- `standing-close` (the existing query): when W's stream settings say by
  entity, it also tail-reads the micro era's delivery and opening lines on
  `$$micro [W :ix-ek]`, and when either is there hops to hash(ent), opens
  the line's row there (the row, its lock row, its ledger entry, the
  persons; `locks/open-with`), and computes `closing` in the module from
  the later line of each kind. No change for a layer never re-classed
  beyond one settings read.
- `rx/session-closes` also reads the micro era's `session-closed` range of
  `$$micro [W :ix-ke]`, at or below the settled frontier.
- `drop-reads!` pages the stream era (bound: the close's stamp, or none for
  a micro-era close) and then, for a micro-era close, the micro era by
  `micro-entry-ids` (bound: the close's batch, from its record).

**H-1e, the runner (recipe.clj, runner.clj).**
- `recipe/run-name` gains a 4-arity `[layer class tool-fid matched-fid]`;
  the id part is unchanged, so a match's name under either class has the
  same id. The 3-arity stays `:by-layer`.
- `run-pass!` names runs with the layer's class (its settings, as now),
  offers by `mc/offer-into!`, and counts a match as run when its name under
  either tag has a record: `c/lookup-many` for the `:by-layer` names, and,
  when the layer is by entity, `mc/lookup-many` for the `:by-entity` ones.
  So a match run before the re-class does not run again after it.

**First-record placeholders and questions for Sid.**
- FR3, FR4 (For Sid 3): an entry into a re-classed working layer is an
  ordinary act at the micro gate: name `[W :by-entity :offer uuid7]`, a
  record with `:batch`, stamped by the micro gate after its moment, its
  lock rows on the entry entity's task. The review's question 1: should
  entries follow the working layer's class and gate?
- T-FR5: a run's name follows its layer's class, `[L class :offer id]`, id
  unchanged; a match counts as run under either tag.
- FRR5 at the micro gate: the close's `:reads` part, the same form at both
  gates.
- The drop's micro-era cut-off by batch (a rig choice, not a record form).
- For Sid 41 stays open: the owner's forget of a micro-era value.

**Costs.** Every act into a working layer reads its class (one point read,
a roundtrip); the exit's read goes from two roundtrips to three. An act
into a re-classed working layer goes through the micro door: a lease act
and the act, two microbatch cycles (about 0.5 to 1 s with the 250 ms
tick), where the stream door's pooled leases take one roundtrip; a read
after a re-class waits that long before it shows anything. At the micro
gate an act cites one lease of at most 256 locks (M24), so a point read of
more than 256 fact ids into a re-classed working layer is refused at its
lease and shows nothing. None of this is fixed here; each goes to the
notes.

## The places the fixes meet 6b (landed code)

1. Block 1a's arrival part (6b's M1 reorder): the lock work, the lease-under
   read and the moments go after its person reads.
2. `$$micro-arrivals` (6b's handoff): gains `*frows *kvd`; blocks 1b and 2b
   bind eight fields.
3. Block 2b (6b's `|direct` back to the arrival task, the lease and person
   re-reads, `row-wraps` with carried subjects, `kv-digests` with carried
   hints): replaced by the carried rows and digests.
4. `skeleton` (6b added `:subjects`): gains `:lease-under`, `:lock-failed`,
   `:entry-moments`; loses `:subjects`.
5. `arrival-open` (6b's 4-arity with grammars): gains `:entry-moments`.
6. `micro-decision` (6b's 7-arity with key rows): gains an 8-arity.
7. `decide-envelope` (6b's key-row composition): the `:gate-error` face
   before any write, and the mint's person.
8. `row-wraps`' 4-arity (6b's): called in block 1a, not 2b.
Not touched: 6b's grammar rows, uses, F4 check, key-row projection,
shared-read grammar inputs and rebuild paths.

## Build order

1. micro.clj: `close-ok?`; `arrival-open`'s moments; `persons-to-check`
   3-arity; `lock-failed?`; `skeleton` 6-arity; `micro-decision` 8-arity;
   `decide-envelope`; block 1a, 1b, 2b; `micro-lookup-many`. Commit.
2. micro_client.clj: `layer-class`, `offer-into!`, `lookup-many`,
   `connect`. read_exit.clj, standing.clj, recipe.clj, runner.clj. Commit.
3. shared_reads.clj: the maintenance reads' micro era. Commit.
4. Load check (compile the changed namespaces, no cluster), validation by
   reading, the tests, one run of the touched namespaces under the cluster
   lock.

## Tests

A new namespace, `rig.store.spec-fixes-test`, pure tests first, then one
cluster:
- pure: `close-ok?` with `:reads`; `persons-to-check` for an agent's lease
  gives the owner; the skeleton carries no bytes; the fold refuses a yes
  with `:lock-failed` as the face `:gate-error`, writing no record and
  consuming nothing; the fold stamps an entry after a moment ahead of its
  wall and clocks; the mint takes `:lease-under`; M-1's two outcomes over
  the pure functions (a row wrapped with the person live reads `{:erased-at
  s}` once the person's entry is forgotten; a group note about no one,
  leased by Bob, opens after Bob's forget); `run-name`'s two tags share an
  id.
- cluster (the model's world, `:alice-agent` re-classed after stream-era
  entries): an agent's point and pattern reads through the exit with
  working `:alice-agent` (rows shown; the entry `:by-entity`, admitted at
  the micro gate, stamped after its moment); a standing read opened before
  the re-class and delivered after, closed (`:deliveries` counts both eras)
  and a crashed one closed by `close-session!`; a session close with
  `:reads :drop` at the micro gate and the operator's drop of both eras;
  a runner pass in the re-classed layer, run once across two passes and
  across the re-class.
Existing namespaces whose functions change: `micro_prepare_test`,
`grammar_micro_test`, `read_exit_test`, `reads_rest_test`, `tools_test`,
`recipe_test`, `micro_test`, `review_fixes_test`. None of their assertions
should change; they are the "touched" namespaces for the one run.

## Validation by reading (phase 2's checks, scenario-traced)

Default verdict fail; each check below was traced on the merged code.

**M-1, the race, lease writer case.** Bob leases in the group at the micro
door (rows sealed under Bob's lock), seals a note about no one, offers it.
Block 1a on the arrival task reads the lease rows and `$$persons [:bob]`
(alive), delivers K, opens, checks; the union is empty (no owner in a
group), `persons-to-check` gives [] and `person-reason` nil; the lock work
wraps K with the empty wrap (`:blob` K, PV-F2) and computes the digest.
Bob's forget fan-out runs on the task now. The fold answers yes; 2b writes
the carried row. A read: `erasure` nil (no ledger, the empty wrap never
closes), `unwrap` gives K, the note opens: A3's outcome. Before the fix:
2b's `unlease` failed, the row was `:lock nil`, `:does-not-open`.
Pass.

**M-1, the race, wrap person case.** Alice writes in the group a mention of
Bob. Block 1a reads Bob alive (the union holds Bob), the wrap is any-of
[:bob] under Bob's live lock. Bob's forget runs; 2b writes the carried
record. A read: `wrap-closed` gives Bob's `:erased-at`, `{:erased-at s}`.
The person purge's `live-for?` finds Bob in the record. Pass.

**M-1, a forget before block 1a's read.** Bob's entry is gone at the read:
lease writer case, `delivered-locks` misses, `:missing`, face
`:no-such-lock` (the stream gate's). Wrap person case, `person-reason`
`:person-forgotten`, recorded (the stream gate's). Pass.

**M-1, internal failure.** A wrap that does not seal: `fact-rows` gives a
row with no record, `lock-failed?` true; on a would-be yes the fold writes
only the face `:gate-error` (unrecorded; the door may resend), on a
refusal the refusal. Never a yes without a lock. Pass.

**M-1, retries.** A failed batch attempt discards its writes and the
materialized handoff; the retry reruns block 1a with fresh nonces and the
then-current persons, so the committed rows are the committed attempt's.
Two byte-identical records of one envelope in a batch each carry their
own rows; the fold decides the name once; both pass 2b's filter and write
rows of one K under one wrap (as before). Pass.

**H-1, an agent's read after the re-class.** `:alice-agent` re-classed.
`:agent-a` reads `:alice` with working `:alice-agent`, permission
`[:agent-a :alice-agent :alice-agent]` (granted in the stream era).
`check-call` passes; `layer-class` reads `:by-entity`; the name is
`[:alice-agent :by-entity :offer u]`; the query runs on `:alice`'s home and
answers; `entry-offer` builds `:class :by-entity`; `offer-into!` sends it
through `write!`: the lease act (`:who :agent-a`) reaches block 1a, its
permission is read from both stores (M5), `lease-under` gives `:alice`
(`:agent-a` has no person entry, the layer's owner is Alice), Alice is
alive, the fold mints rows under Alice's lock; `take-locks` unleases them
over Alice's entry; the entry is sealed and offered; block 1a delivers,
opens, checks (`:read/pattern` has no grammar row, permissive), computes
the moment parts and the lock work (the `:own-row` mark makes each lock a
lock row on the entry entity's task); the fold stamps after the moment;
yes; 2b writes the rows and lock rows; `read!` shows the rows. Pass.

**H-1, the drop.** A session `s` of `:agent-a` in `:alice-agent` has one
entry before the re-class (`E1`, stream) and one after (`E2`, micro).
Alice closes `s` with `:reads :drop` after the re-class:
`close-session!` reads the class, builds `:class :by-entity`, the micro
gate's `close-ok?` takes `{:session s :reads :drop}`, its author check
takes `(:session o) = s`; yes, batch b. `drop-reads!` as the operator:
the close's name is `:by-entity`, so the stream-era page has no stamp
bound and finds `E1` (its record's session `s`), whose forget
(`:class :by-entity`, target `:by-layer`) `gate-for` sends to the stream
gate, where the class matches the layer's fact and the operator may
forget; the micro-era page, bound b, finds `E2` (batch below b), its
session read at its name task, whose forget goes to the micro gate (target
`:by-entity`), the operator's (M14). A rerun finds both erased
(tombstones are not candidates). Pass. As Alice: `E1` is dropped, `E2`'s
forget is `:control-not-allowed` (For Sid 41), and `drop-reads!` counts
one; stated in the notes, not hidden.

**H-1, the runner.** A tool fact and a matched fact in `:alice-agent`
before the re-class; pass 1 (stream era) runs the match under
`[:alice-agent :by-layer :offer id]`. Re-class. Pass 2: the class is by
entity; the run's name is `[:alice-agent :by-entity :offer id]`; the
stream lookup of the `:by-layer` twin finds pass 1's record, so the match
is reported recorded and not offered. A new match after the re-class runs
at the micro gate (the tool's lease under Alice by W1-1); pass 3 finds it
by `mc/lookup-many`. Pass.

**Partition alignment.** New reads: `micro-entry-ids` and
`micro-standing-open` start with `(|hash *layer)` (the index on hash(W)),
then `(|hash *nm)` for `$$micro-names [nm]`; `standing-close` hops
`(|hash *ent)` before reading `$$micro [ent ...]`; `micro-lookup-many`
hashes each name. Block 1a's new person reads are local (`$$persons` is on
every task). Pass.

**Topologies and state.** No new depot, PState or topology; one new field
set in the attempt-local handoff; three query topologies. Pass.

**Minimality.** The simplest design that keeps the properties: the gate
locks from its one read (no second read to disagree with), the door names
the class the layer has, and the maintenance reads read both eras. Each
mechanism above is one of those three or a place where the micro gate did
not yet decide as the stream gate does (W1-1, F1, FRR5). The per-candidate
hop in the micro-era maintenance reads is the one cost chosen over a
schema change; it is on the drop and crash-close paths only. Pass.

Verdict: pass, with the costs and the open question (For Sid 41) carried to
the notes.
