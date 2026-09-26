# Build notes — stage 5b, "reads, the rest" (PLAN-reads-rest.md)

<!-- Build session for the rest of Sid's phase 5, 26 September 2026, Claude
Opus 5.5, worktree /mnt/data/projects/Softland-rig-build-reads-rest, branch
rig-build-reads-rest, made off rig-2026-09-25 at 9730010c. Part 1 (this
section) prepared against wave 1's code as it stood in
/mnt/data/projects/Softland-rig-wave1 at 57c46159 (branch rig-wave1, with an
untracked test/rig/store/wave1_test.clj), read only. Part 2 builds after
wave 1 lands on rig-2026-09-25. Vocabulary: "key" is a fact's key, "lock"
an encryption key. -->

## Prep

**Loaded.** The rama skill (SKILL.md, phases.md, phase-build.md); SPEC.md;
RIG.md "Overnight state", "For Sid" (3, 4, 6, 13, 19, 28 to 30 and the
rest), "Defaults taken overnight" (1 to 8), "Rig choices so far";
PLAN-reads-rest.md whole; PLAN_VALIDATION-reads-rest.md whole;
PLAN-read-exit.md ("The shapes", "Later stages", L1043-1049); on main,
PROGRESS.md "Now" and CONCLUSION.md "Eight rulings" (read only);
IMPLICIT_SPEC RD3 to RD7; PLAN-replays.md's API table; the other wave 2
plans' lists of files they touch. Wave 1's code read in full where this
stage binds: reads.clj, read_exit.clj, module.clj, gate.clj, micro.clj,
micro_client.clj, client.clj; locks.clj's pure helpers and every dataflow
op; BUILD_NOTES-locks-and-forgetting.md "Seams and stubs".

### Binding points, as wave 1 has them at 57c46159

Status words: **as planned**, **differs** (the plan's call follows wave 1,
reason given), **absent** (this stage builds it, or builds around it).
Line numbers are wave 1's files at 57c46159; they may move before it lands.

**The read exit (`rig.store.reads`, `rig.store.read-exit`)**

1. `reads/open-row>` `[*layer *fid *row *stamp *T :> *o]`, body
   `locks/open-row>`, three shapes (reads.clj 410-426). **As planned.**
2. The forget's purge seam: `reads/purge> [*layer *erased *date]`, items
   `{:fid :row :stamp}` (reads.clj 285-341); module.clj calls it in a value
   forget's decision group (207-211, dated by the forget's stamp), on a
   recorded person forget's resend (136-139), and in each task's person
   fan-out child once per dying value (215-220, `locks/dying>`). **Differs
   in form** (the plan had phase 2 call `purge-writes` itself); the plan's
   path 1 takes `purge>` as its call site.
3. `purge-writes`, `index-writes` (plain texts from the gate's
   `plain-texts`), `put-page-writes`, `sweep-page-writes`, `implied`,
   `all-fields`, `rebuild!`'s field list (reads.clj 141-264, 367-406;
   read_exit.clj 142-164). **As planned**; each gains `:ix-s`.
4. **Differs, the purge rule.** Wave 1's rebuild put page tombstones every
   row the open step reports erased, whether by the ledger or by a wrap a
   person forget closed (`row-writes`, reads.clj 236-241), and its person
   forget tombstones every dying value dated by `wrap-closed`, skipping a
   value the ledger already erased (`locks/dying>`, locks.clj 1253-1284).
   The plan's invariant has the other rule (item 9: a tombstone only for a
   ledger erasure; a person purge deletes only `:ix-kv` and `:ix-of`).
   Both rules keep "a purge writes what a rebuild would". Intended: keep
   wave 1's rule (forget_test and wave1_test assert it; it retains less),
   drop the plan's item 9, and apply the same rule in the micro store (its
   person purge tombstones every dying entry, found by a sweep of `:ix-s`,
   where the plan swept `:ix-kv`). RT7 and RT8 assert this rule.
5. `read-point [layer for fids as-of]`, `read-pattern [layer for pattern
   as-of limit]` (reads.clj 1168-1295): a re-classed layer is refused
   `:re-classed`, a group layer (no stream settings) `:not-visible`. The
   shared branch takes both cases' place. `as-of` is accepted only as nil
   or a non-negative integer, in `parse-point`, `parse-pattern`
   (`as-of-ok?`, 439) and `read-exit/check-call` (63): **must widen** to
   `{:stamp s}` and `{:frontier F}`. `read_exit_test.clj` 494-498 pins the
   `:re-classed` refusal; that expectation changes.
6. `entry-facts`, `entry-moments`, `seed-hints`, `read-keys`, `index-op`,
   `fingerprint`, `address`, the page loop's pure steps, `tail-state`,
   `pattern-answer`, `point-answer`, `entry-entity`. **As planned.** The
   fingerprint secret is private (`fp-secret`, 756): the kv digest and the
   standing chain need one public HMAC under it.
7. `read-exit/read!`, `check-call`, `connect`, `rebuild!`, `index-op!`
   (read_exit.clj, unchanged by wave 1). **As planned.**
8. `gate/stamp-for` counts `reads/entry-moments` over the values the gate
   opened (`with-opened`, gate.clj 221-257). **As planned**; the standing
   keys and a frontier line's `:max-stamp` reach it through
   `entry-moments`.

**Phase 2 (`rig.store.locks`, and its calls in gate.clj and client.clj)**

9. `locks/open-row>`, `open-value>`, pure `open-with [row stamp T ledger
   record persons]`, `unwrap`, `wrap-closed`, `erasure` (locks.clj 322-364,
   495-522, 1312-1367). **As planned**; `open-entry>` is `open-with` over
   the entry's `:lock` and `$$persons` read on the layer's task.
10. OP9, the value forget: `{:e e :k :forget :v {:target fid}}` by the
    layer's owner (citing `[owner L L]`) or the operator; a second forget
    of an erased value is admitted and changes nothing (locks.clj 779-793).
    **Differs: a lock control fact must be its act's one fact** (gate.clj
    194-197, `:malformed-control` otherwise), so the plan's drop acts of 64
    targets (F4, RR14, FRR9) cannot be offered. Intended: one forget act
    per entry fact, taken in pages of 64 ids from `entry-ids`; each event is
    then about five seeks, well inside any bound. FRR9 becomes "one forget
    act per dropped fact, because-of the close act".
11. The session close, `{:e s :k :session-closed :v {:session s}}`: the
    gate's value check is `(= #{:session} (set (keys v)))` (gate.clj
    101-102), so a `:reads` part is refused `:malformed-control`. **Needs
    one clause in gate.clj** (FRR5). The micro gate's `close-ok?` has the
    same check and stays: read entries never live in shared layers.
12. The paged enumeration `dying-with>`: **absent.** Wave 1 built
    `locks/dying>`, unpaged, inside the fan-out child (flagged over any
    per-event budget at scale by phase 2's own notes). Intended: keep it as
    the forget's immediate reach (tests assert it); this stage's one-owner
    `:person-purge` page (for the restore's replay and for recovery) does
    the same enumeration paged, in this stage's namespace, over locks' pure
    helpers (`names-person?`, `locked-rows`, `closes-with?`, `erased-item`,
    `wrap-closed`, `read-persons>`), so `locks.clj` is untouched.
13. A replay seam for forget effects: **absent** as a seam. In the rig no
    lock is restored (L17), so `:replay-forget` reads the ledger (or the
    closed wrap) and purges at that date, as the plan says for the rig.
14. `client/forget-person! [store p]` (client.clj 355-364), the fan-out on
    every task before the answer. **As planned**; the plan's F17 wrapper
    calls it, then `purge-person!` for the micro store.
15. `$$persons` on every task; `:own-row` gives a lock row in any layer
    (`row-lock?`, 368-375); an agent's lease sealed under the layer's
    person owner (`lease-under`, 591-607). **As planned.**
16. `read-as-of` internal; `:by-stamp` kept (ids only). **As planned**;
    nothing here needs either.

**Phase 3 (`rig.store.micro`, `rig.store.micro-client`)**

17. `$$micro-task :frontier`, written by block 0 as the previous batch's id
    (-1 before the first); `frontier-of`, `visible-at?` (micro.clj 180-206,
    1262-1266). **As planned.**
18. `$$micro` is the literal `micro-schema` (111-157): entity -> fixed
    keys; a row `[e :log name idx]` is `{:layer :k :v :sealed :replaces
    :mark :lock-id :lock :digest}` (no `:e`). **Needs** its row fields named
    once and the entity value merged with this stage's fields
    (`shared-reads/layer-fields`).
19. `:settings` versions keyed by batch, `{:kind :owner :class :grain
    :batch}`. **As planned** (settings at F: a tail read below `F + 1`).
20. `:members` is `(map-schema Keyword Long)`, person -> the batch that
    named them (148), not a subindexed set. **Differs, for the better:**
    membership is one keypath seek, and membership as of F is readable
    (batch <= F), so F7's `subselect (set-elem)` is moot and RR2 becomes
    "members as of F".
21. `$$micro-task` is `(fixed-keys-schema {:clock Long :frontier Long})`.
    **Needs** `:rebuild` and `:layers` (the docs allow a subindexed set
    nested anywhere below the top level; checked at the build).
22. Block 2b re-reads the offers in its own `<<batch` (1501-1552): on the
    arrival task `row-wraps` (plaintext as `:plain {i bytes}`, the UTF-8 of
    the value's canonical text, the same text the one-owner `:ix-kv` holds),
    `fact-rows`; then `(|hash name)`, the record with `:stamp` and `:batch`,
    `rows-written?`; then `(|hash e)` per row. **As planned** for block 2d:
    the kv digests are taken on the arrival task before the name hop, and
    block 2d is a branch (`anchor>` / `<<branch`) after `rows-written?`,
    routed to `(|hash L)`. A row's lock record is its `:lock` or, for a
    row lock, the fourth element of `fact-rows`' tuple.
23. The value forget at the micro gate (wave 1): the operator's; the fold's
    `forget-effect` (785-827) excises the record lock or deletes the lock
    row on every entity sharing the target's lock, ledger dated by the
    forget's stamp, `{:how nil}` when already erased. **Differs: no row
    seam emits the erased fact ids and the date**; the fold holds them
    (`sharing`). Intended: `forget-effect` also returns the erased fids,
    `decide-envelope` puts `:purge` writes, block 2a routes them to
    `(|hash L)` (block 2a runs before 2b, and a forget's target was
    admitted in an earlier batch, so nothing in the same batch can rewrite
    what the purge wrote).
24. M25: a stream-era forget target is refused `:no-such-value` here and
    `micro-client/gate-for` sends it to the stream gate. **As planned**
    (the plan's F16 path 1).
25. Sources: `*micro-tick`, `*micro-offers` (1255-1260). **Needs** a third,
    `*micro-index-ops`, and one `<<batch` block after block 2c.
26. Re-class: the base re-classed by a stream act `{:e L :k :class :v
    :by-entity}` (`micro-client/reclass-offer`, `make-group!`); its
    settings stay in `$$layers`. **As planned.**
27. `micro-lookup`, `micro-act`, `micro-lease`; `micro-client/frontier`,
    `write!`, `offer!`, `make-group!`, `make-base!`, `open-session!`,
    `forget-value! [store L fid e]`, `open-act`. **As planned**; tests use
    them.
28. PState read visibility: reads inside the owning topology see its own
    uncommitted writes (the skill's pstate-schema.md), and a batch's
    blocks run in order. So the plan's put-page guard (read `:ix-ek` before
    each put, one seek a fact) is not needed when the ops block runs after
    blocks 2a to 2c; kept only if the build shows otherwise.

**Phase 8's name for "the exit's shared-layer read"**:
`rig.store.read-exit/read!` itself, `:layer` a by-entity layer (a group, or
the re-classed base), `:read [:point fids]` or `[:pattern p]`, `:as-of` nil
or `{:frontier F}`; the answer's `:moment` is `{:frontier F}`, rows carry
`:batch`, and `:max-stamp` rides in the answer. The plan keeps one exit, so
there is no second function.

### Shared files this stage expects to change, and why

- `reads.clj`: the fifth field `:ix-s` (fields, entry writes, purge
  tombstones, `implied`, the field sets); one `<<cond` case in each of
  `read-point` and `read-pattern` sending a by-entity layer to
  `rig.store.shared-reads`, and a frontier moment on a one-owner layer
  refused `:moment-kind`; moment maps in `parse-point`, `parse-pattern`,
  `moment`; `entry-facts` (`:mark #{:own-row}`, a shared line's
  `:max-stamp`); `entry-moments` (the standing keys, `:max-stamp`);
  `seed-hints :no-copy` and `read-keys` gain `:read/standing`,
  `:read/delivery`, `:read/closed` (phase 6 renames `seed-hints` to
  `store-hints`: the merge carries the five keys there); `index-op` and
  the index-ops source gain `:person-purge` (routed by `:task`) and
  `:replay-forget`; one public HMAC under the fingerprint secret.
- `read_exit.clj`: `check-call` takes moment maps; `connect` takes the new
  queries and depot; `rebuild!` sweeps `:ix-s`; new at the end:
  `drop-reads!`, `resume-drops!`, `purge-person!`, `forget-person!` (the
  F17 wrapper), `restore!`.
- `gate.clj`: the `:session-closed` value check accepts `:reads :keep |
  :drop` (FRR5). One clause.
- `module.clj`: one line, `(shared-reads/declare-queries! topologies)`.
- `micro.clj`: the row fields named once and `$$micro` merged with
  `shared-reads/layer-fields`; `task-schema` gains `:rebuild`, `:layers`;
  block 2b's kv digests and the block 2d branch; `forget-effect` returns
  the erased fids, `decide-envelope` puts `:purge` writes, block 2a routes
  them; the `*micro-index-ops` depot (in `declare!`), its source and its
  block.
- `locks.clj`, `micro_client.clj`, `client.clj`: no change intended.

New: `src/rig/store/shared_reads.clj` (module side: the shared read path,
`open-entry>`, the micro index fields and writes, the micro purge and pages,
the delta query, the three maintenance queries), `src/rig/store/standing.clj`
(client side), `test/rig/store/shared_reads_test.clj` (pure),
`test/rig/store/reads_rest_test.clj` (cluster, 4 tasks). Existing tests
whose expectations move: `reads_test.clj` (write lists gain `:ix-s`),
`read_exit_test.clj` (the `:re-classed` refusal becomes a shared read).

One divergence of form, for the merge: the delta is a new query
`read-delta` beside `read-pattern`, sharing its pure steps, not a new
argument of `read-pattern`, whose signature phase 6's runner and phase 8's
replays call; the plan's behaviour is unchanged. It is a fourth caller of
`reads/current-hints`, which phase 6 replaces.

### Order of work (part 2)

1. Merge `rig-2026-09-25` at the commit the orchestrator names; run the
   landed suite once, as the baseline.
2. The read exit's changes: `:ix-s`, moment maps, `entry-facts` marks,
   `entry-moments`, the hints, the public HMAC; update `reads_test`
   expectations. Pure tests first, then the suite's read exit namespaces.
3. `shared_reads.clj`, pure half: moment, visibility, addresses, the micro
   entry and write functions, purge writes, the era merge, the delta plan
   and `matches?`, the standing chain.
4. The micro store: schema merge, `task-schema`, block 2d, the forget's
   `:purge` writes, the `*micro-index-ops` source and pages.
5. The shared branch in the two read queries, `open-entry>`, the
   re-classed layer's two eras.
6. `read-delta`, `standing-close`, `standing-open`, `entry-ids`;
   `standing.clj`.
7. The close act: the gate clause, `drop-reads!`, `resume-drops!`.
8. The person purge and the restore: the `:person-purge` and
   `:replay-forget` ops in both stores, `purge-person!`, `forget-person!`,
   `restore!`.
9. Implementation validation, tests (RT1 to RT14), test validation, the
   whole suite green, the notes' last sections. Commits along the way, one
   change each.

### Blocks

None for part 1. Part 2 waits on wave 1's landing. Two decisions above
(the purge rule, item 4; one forget per act, item 10) follow wave 1's code
rather than the plan; both change no record beyond FRR9's placeholder.

## Built (part 2)

Merged `rig-2026-09-25` at 2034cce5 (wave 1) into this branch (63f6f795), then
built PLAN-reads-rest.md through the rama skill's build step. Phase 4's
extraction (1b1d416c) was not merged: this stage's module.clj line and gate.clj
clause sit outside the moved path.

- **Shared-layer reads through the one exit** (`rig.store.shared-reads`
  `shared-pattern>`, `shared-point>`, `open-entry>`): the read exit's
  `read-point` and `read-pattern` hand a layer of the shared kind (by-entity
  class, or no stream settings) to the queries `shared-read-point` and
  `shared-read-pattern` with one `invoke-query` on the same task. F =
  min(asked, the task's frontier); a stamp moment refused `:moment-kind` (and
  a frontier on a one-owner layer); settings and membership as of F; the micro
  index on the layer's task, visibility by batch; a re-classed layer's stream
  era merged by the entries' fields (F1); the answer `{:frontier F}`,
  `:max-stamp`, rows with `:batch`, `:partial` while the layer has an index
  gap (F9).
- **The micro store's indexes** (`$$micro [L]`: `:ix-ek :ix-ke :ix-kv :ix-s
  :ix-of :ix-id :ix-error :ix-place`): written by block 2d, a fork of block
  2b after its name-task check, in the batch that decides the act; the value
  index keyed by an HMAC of the value's text taken on the arrival task (F12);
  a value forget's purge in its own batch through block 2a (`purge-fact>`);
  the `*micro-index-ops` source (its own section of the batch): put, sweep,
  person purge and forget replay pages on their task, with a progress row per
  task and a `micro-index-progress` query; a tombstone guard on puts and
  sweeps; a rebuild ends with the replay of the forgets decided since it
  began.
- **The one-owner `:ix-s`** (every fact in stamp order) in the entry writes,
  purges, rebuilds and field sets.
- **Standing reads** (`rig.store.standing`): `subscribe!`, `deliver!`,
  `unsubscribe!`, `close-session!`; the `read-delta` query (the opening read
  through `read-pattern`, then deltas over `:ix-s` or the pattern's own
  index); lines FRR1 to FRR3 with the running closing value (F3);
  `standing-close` reads one line; `standing-open` finds a crashed door's open
  entries by session; a handle held across a re-class closes `:reclass` and
  reopens (F18); a budget-cut delta resumes past its last scanned address,
  sealed (below, D6).
- **The close act and the drop**: the gate accepts `:reads :keep | :drop` on
  the session close (FRR5); `read-exit/close-session!`, `drop-reads!` (one
  forget act per entry fact, pages of 64 ids from `entry-ids`, by the act
  record's session), `resume-drops!` from the record (F11). Every read entry
  fact is marked `:own-row` (FRR6); the exit's entry names its session
  (FRR10, the coordinator's finding from phase 7's validation).
- **Indexes and forgets whole**: the replayed forget and the paged person
  purge on the read exit's `*index-ops` (`dying-rows>`, `person-page>`);
  `purge-person!`, `forget-person!` (F17's wrapper), `rebuild-micro!`,
  `restore!` (the fact, both rebuilds, the forget replay in stamp order).

## Divergences from the plan (each in IMPLEMENTATION_VALIDATION, "Plan conformance")

- D1. The purge rule is wave 1's: a tombstone for whatever the open step
  reports erased (the ledger's date, else the wrap's close), in both stores;
  the plan's item 9 is not built.
- D2. A drop is one forget act per dropped entry fact (phase 2's gate: a lock
  control fact is its act's one fact). FRR9 changes (first-record, For Sid).
- D3. The read entry names its session (FRR10, new, first-record).
- D4. The shared read is `invoke-query`d from the exit's two queries (no
  namespace cycle, no shared path inside reads.clj, which phase 6 rewrites).
- D5. The delta is its own query, `read-delta`; `read-pattern` keeps its
  signature.
- D6. F2 as built: a budget-cut delta resumes from a sealed cursor, not a time
  part, which could not pass one act larger than the scan budget (found by
  RT9, runs/reads-rest-try2.log).
- D7. The micro index ops run in their own batch section, beside the offers'
  blocks: guards on puts and sweeps, and the replay after every micro
  rebuild.
- D8. The micro person purge sweeps `:ix-s` (every dying entry, D1's rule).
- D9. Membership is read as of F (the built `:members` is person -> batch).
- D10. The progress row carries a request id and a count; two queries added
  (`micro-index-progress`, `task-layers`).
- D11. An opening read cut by its limit is not caught up later (its line and
  the closing mark say partial).
- D12. Block 2d is an explode fork: a `<<branch` inside a `<<batch` block
  does not build in Rama 1.6.0.
- D13. The micro put page's row cap is 2,048.

## Shared-file changes

- `reads.clj`: hints and read keys (the three standing keys); `:ix-s` in
  `address`, `layer-fields`, `index-fields`, `all-fields`, `fact-writes`,
  `purge-writes`, `implied`; public `kw-text`, `fid-text`, `prefix-end`,
  `fid-ok?`, `norm-fid`; `moment`, `frontier-moment?`, `as-of-ok?`,
  `norm-as-of`, `shared-layer?`; `page-init`'s `:by`, `stamp-ok?`,
  `tail-state`, `page-step` (`:match`, `:last`, `:cut`), `entry-matches?`;
  `entry-facts` (`:mark #{:own-row}`, `:max-stamp`), `entry-moments`; after
  `fingerprint`: `kv-digest`, `seal-cursor`, `open-cursor`, `standing-seed`,
  `standing-link`; `index-op` gains `:replay-forget`, `:person-purge`;
  `max-person-page`, `replay-writes`, `task-count`, `task-ok?`,
  `person-start`, `person-next`; `dying-rows>`, `person-page>` before the
  depot section; two `case>` in the index-ops source; one `case>` each in
  `read-point` and `read-pattern` (the `invoke-query`, the `:moment-kind`
  refusal; the `:re-classed` refusal removed). Phase 6 renames `seed-hints`
  to `store-hints`: carry the five read keys there; `read-delta` is a fourth
  caller of `current-hints`.
- `read_exit.clj`: `connect` (eight handles), `check-call` (moment maps,
  `:session`), `entry-offer` (`:session`), `read!` (`:max-stamp`), `rebuild!`
  (`:ix-s`); new at the end: `close-session!`, `entry-ids`,
  `forget-entry-offer`, `drop-reads!`, `session-closes`, `resume-drops!`,
  `task-count`, `wait-frontier!`, `micro-op!`, `rebuild-micro!`,
  `purge-person!`, `forget-person!`, `forget-facts`, `restore!`.
- `gate.clj`: `control-value-ok?`'s `:session-closed` clause (`:reads`).
- `module.clj`: the require and `(shared-reads/declare-queries! topologies)`.
- `micro.clj`: the require; `micro-row-fields` named and `micro-schema`
  merged with `shared-reads/layer-fields`; `task-schema` merged with
  `shared-reads/task-fields`; `forget-effect`'s `:erased`; `decide-envelope`'s
  `:purge` writes; block 2a's `:purge` case; block 2b's `kv-digests`,
  `*layer3` and the `[:rows :index]` fork; `declare!`'s depot line; the
  `*micro-index-ops` section at the end of `<<sources`.
- `locks.clj`, `micro_client.clj`, `client.clj`: unchanged.
- Tests: `reads_test.clj` (four counts), `read_exit_test.clj` (D7's block).

## For RIG.md

**Rig choices** (each changes without touching a record):

- P5-1. Every index of a shared layer on the layer's own task (Option B, RR5);
  the bucket count `:ix-place` is maintenance state, unwritten (absent is 1).
  Accepted for the rig, not for the store core (For Sid 29).
- P5-2. A shared read is as of F = min(asked, the frontier on the layer's
  task); a stamp moment on a shared layer, or a frontier on a one-owner
  layer, is refused `:moment-kind` (RR1).
- P5-3. Group membership is read as of F (the batch that named the member).
- P5-4. The exit's two queries hand a shared layer to `shared-read-pattern`
  / `shared-read-point` by `invoke-query` on the same task.
- P5-5. Standing reads poll at the delivery rate (RR9; the doorbell is the
  named upgrade).
- P5-6. A delta cut by the limit resumes after the last shown row; cut by the
  scan budget, past the last address it scanned, sealed under a module lock
  so the client cannot read it (the build's repair of F2).
- P5-7. An opening read cut by its limit is not caught up by later
  deliveries; its line and the closing mark say `:partial`.
- P5-8. The micro index pages run in their own section of the batch: no put
  or sweep writes a live entry over a tombstone, and every micro rebuild ends
  with the replay of the forgets decided since it began.
- P5-9. The micro person purge sweeps `:ix-s` of the task's shared layers
  (RR17: a subject index by person is the named upgrade).
- P5-10. Person purges run in the operator's pages on every task: for the
  one-owner store beside wave 1's purge in the forget's own fan-out (the pages
  find the same values: recovery and a restore's replay), for the micro store
  the only road.
- P5-11. A drop takes pages of 64 ids, one forget act per entry fact.
- P5-12. `standing-close`, `standing-open` and `entry-ids` are maintenance
  reads, ids and stamps only, not recorded; their callers are trusted (RR11).
- P5-13. An index gap in block 2d writes nothing for the act and flags its
  layer; every read of the layer is `:partial` until a rebuild clears the
  flag (RR6, F9).
- P5-14. Micro put pages of at most 64 entities and 2,048 rows; sweep pages
  of 512 entries; person pages of 256 (both stores).
- P5-15. The one-owner `:ix-s` index (one more put per fact, every layer) so
  a delta costs one seek and the new facts' iterations.

**Questions for Sid** (each touches a record or a ruling's intent):

1. **FRR9 as built: one forget act per dropped entry.** Phase 2's gate
   refuses an act holding a lock control fact beside another fact, so a
   dropped session's entries are forgotten one act each, `:because-of` the
   close act, not in acts of 64 targets. Every forget is a fact either way;
   a drop of n entries records n acts. Keep one per act, or allow a forget
   act of many targets?
2. **FRR10, new: the read entry names its session.** The exit's entry act
   carries the session the read was taken in (its `:session` part), so the
   session's close finds its entries by the acts' records. Before this, the
   entry had no session and fell into the door's default session.
3. **The rest of phase 5's forms** (FRR1 to FRR8, as the plan): a standing
   read's opening, delivery and closing lines and their keys; the close act's
   `:reads :keep | :drop`; `:own-row` on every entry; the restore fact in
   `:people`; the shared moment `{:frontier F}` and a line's `:max-stamp`;
   the closing fingerprint as a chain over the lines' moments and
   fingerprints (difficulty 5's reading of "a fingerprint over everything
   delivered").
4. **A drop is effective per entry** as each forget is decided, not at the
   close act (RR15; For Sid 13): until the drop's forgets finish, the
   remaining entries still open.
5. **The purge rule for a person forget** (wave 1's, both stores): the index
   entries of a value that dies with a person are tombstoned (their sealed
   and lock copies dropped), dated by the wrap's close; the plan had them
   kept, only their value index deleted. Both keep "a purge writes what a
   rebuild would".
6. **A standing read does not deliver a forget** of a fact it already
   delivered; the next full read shows it erased.
7. **Index placement at the yardstick** (For Sid 29, unchanged).
8. **The two-store moment** (carried, F10, F16): a re-classed layer's stream
   era is bounded by no frontier.

**First-record placeholders built tonight:** FRR1 to FRR4 (the standing
read's three keys `:read/standing`, `:read/delivery`, `:read/closed`, their
values, the running value `:so-far {:fp :n :partial?}` on each delivery line,
the chain's prefix `"softland.standing-fp/1\n"`); FRR5 (the close act's
`:reads`); FRR6 (`:own-row` on every read entry fact); FRR7 (`{:e :store :k
:restore :v {:indexes :rebuilt}}` in `:people`); FRR8 (`{:frontier F}`,
`:max-stamp`, a shared row's `:batch`, a shared `[:kv]` answer ordered by
batch then stamp, a re-classed layer's eras merged by pattern part, stamp,
fact id, `[:kv]` stream era first); FRR9 as built (one forget act per dropped
entry, `:because-of` the close act); FRR10 (the entry's `:session`). Secrets
derived by label, never stored: the kv index `"softland.kv-index/1"`, the
scan cursor `"softland.scan-cursor/1"`.

## The suite

Full suite, every namespace named (wave 1's fourteen and this stage's two),
once, under the cluster lock: `clojure -M:test rig.smoke-test rig.claims-test
rig.revision-test rig.store.clock-test rig.store.envelope-test
rig.store.stream-gate-test rig.store.reads-test rig.store.read-exit-test
rig.store.read-model-test rig.store.lock-test rig.store.forget-test
rig.store.micro-prepare-test rig.store.micro-test rig.store.wave1-test
rig.store.shared-reads-test rig.store.reads-rest-test`, lock taken 06:28:10
IST: 103 tests, 6,132 assertions, 3 failures, 0 errors, 587 s
(runs/reads-rest-suite.log). The three failures were read_exit_test's T10 and
T12, comparing four index fields with `reads/implied`, which now implies five
(`:ix-s`): a test fix (ce1b0441). Along the way, runs of this stage's
cluster test: try 1 (every shared read failed: the page loop emitted nothing),
try 2 (7 failures: F2's time part, the lines' order, a forget fact in `[:e]`,
the restore's own acts), try 3 green (runs/reads-rest-try3.log: 1 test, 511
assertions). Per Sid's rule the fixes after the full run were rerun alone:
see "The rerun".

**Wave 1's review, R-2** (the coordinator): a recorded `[:kv]` pattern keeps
its value's text, which no forget reaches. This stage's new lines never
record it: `reads/recorded-pattern` keys the value under the fingerprint
secret in the module, every pattern answer carries it as
`:recorded-pattern` (the seam R-2's fix of `entry-facts` takes), and a
standing read's opening line records it. The one-owner entries of
`entry-facts` are R-2's fix, after wave 2's merge.

## The rerun

After the full run, the two fixes (ce1b0441, the read exit's tests over five
fields; 255c5eac, R-2's keyed pattern) were rerun alone, the namespaces they
touch, under the lock taken 06:49:17 IST: `rig.store.reads-test
rig.store.shared-reads-test rig.store.read-exit-test rig.store.reads-rest-test`:
28 tests, 1,120 assertions, 0 failures, 0 errors, 183 s
(runs/reads-rest-rerun.log). The other twelve namespaces were green in the
full run (runs/reads-rest-suite.log) and nothing they load changed since.
With both runs: 103 tests, every one green on its last run; the build's
verdict is pass.

PHASE_VALIDATION:pass
