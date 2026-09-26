# Review of step 6b, the micro gate's grammar (26 September)

The rama skill's review of step 6b's diff, done by reading. Reviewer: Claude
Opus 5.5, for builder C. Branch `rig-review-6b`, worktree
`/mnt/data/projects/Softland-rig-review-6b`, made at `rig-2026-09-25`'s
`dee0320a`, the merge of `rig-build-grammar-micro` at `1ca407cd`.
REVIEW-full-spec.md left 6b's path out of its scope. This review covers it.

**Verdict: minor-fail.** Under the skill's phase-4 rubric, no check passes
while it still has a gap, and each gap here can be fixed by editing named
lines or is a question for Sid. **Summary.** There are no high or medium
findings. 6b builds what STARTER-6b.md lists and what the plan's a4, b4, F4,
M1 and T-FR3 ask for. It keeps step R's clause and P3-5's guard. There are
five low findings:
- **L-1:** on every micro miss, the effective-row read goes on to
  `$$layers`. Keys that a re-classed layer used in its stream era never get
  a micro row, so for the life of the layer they cost two seeks per act and
  per read.
- **L-2:** the rebuild's new helpers dropped the try/catch the old ones had.
  They run inside the one microbatch that the whole micro gate runs on.
- **L-3:** a client with no permission in a layer can keep a key's first
  hint-setting grammar refused, and recorded as refused, by sending one small
  envelope per batch.
- **L-4:** more data reaches the leader than the fold needs.
- **L-5:** two of the plan's falsifiers hold by construction, but no test
  shows them.

The machinery count's arithmetic holds. Its unit boundaries follow no single
rule: under the notes' grouping the micro side is 4 (total 33), and if each
plan row's micro half counts as a unit, which is how the notes already count
b1's, it is 6 (total 35).

## What was reviewed, and how

- **The diff:** `git diff 749336e9 1ca407cd -- <rig>/src <rig>/test`. The
  branch forked at `13c8360b`, before phase 7's harnesses landed. So this
  two-dot range also shows six files under `test/rig/bench/` as deleted
  (3,072 lines). That comes from the fork point, not from 6b:
  - With `test/rig/bench` left out, the range is byte-identical to the
    three-dot diff `749336e9...1ca407cd` (same md5).
  - `749336e9..dee0320a` changes the same 14 files and leaves
    `test/rig/bench/` untouched, so the merge kept phase 7's harnesses.

  Across the whole tree, 6b adds 9 files, modifies 13 and deletes none. It
  touches neither `test/rig/store/gate_test.clj` nor anything under
  `src/app`. I checked that with `git diff --name-only` and opened neither.
  RIG.md is not edited.
- **Method.** I applied the rama skill's implementation validation: SKILL.md's
  rules, and `phase-4-impl-validate.md` with its check list. The checks were
  partition alignment of every new local read, retries and exactly-once
  behaviour, I/O per act and per read, cooperative yielding, subindexing of
  new state, totality, and plan conformance. After that, I checked
  STARTER-6b.md's list, the plan's a4, b4, F4, M1 and T-FR3, and RIG.md's
  unit. Everything was done by reading: no code was changed and no tests
  were run.
- **What I read:**
  - STARTER-6b.md, PLAN-grammar-micro.md, BUILD_NOTES-grammar-micro.md, and
    the builder's plan and implementation validations.
  - From BUILD_NOTES-tools-and-grammars.md: "Phase 6b" and the predicted
    count.
  - From RIG.md: "The machinery count so far" and For Sid 65.
  - PLAN-tools-and-grammars.md 3.2, 4.5, 4.7, 8, 10 and 11, and
    PLAN_VALIDATION-tools-and-grammars.md 2.2 (M1, F4).
  - Step R in BUILD_NOTES-review-fixes.md, P3-5 in BUILD_NOTES-replays.md,
    and W2-3 and the count in BUILD_NOTES-wave2.md.
  - PLAN-reads-rest.md 1418-1425, and REVIEW-full-spec.md's scope, severity,
    H-1 and M-1.
- **What I read beyond the diff.** Only what I needed to trace its changed
  lines:
  - The definitions those lines call, read by name at `dee0320a`:
    - micro: `skeleton-offer`, `intake`, `delivered-locks`, `lease-meta`,
      `skeleton`, `name-step`, `layer-rows`, `entity-rows`, `carry-rows`,
      `record-subjects` and `micro-record`, plus blocks 1b and 2a.
    - locks: `open`, `sealed?`, `decode-plain` and `read-values`.
    - gate: `refusal-with-rows`, `misplaced-setting?` and `store-key?`.
    - grammar: `rows-to-read`, `hint-change?`, `rebuild-refusal`,
      `key-row-writes` and `grammars-of`.
    - shared-reads: `entry-of`, `fact-writes`, `kv-digests`,
      `index-writes`, `implied-writes`, `implied-map`, `sweep-writes`,
      `guard-imp>`, `put-row-writes`, `opening-answer` and `delta>`.
    - replay_test: `seed-steps` and `run-seed!`.
  - A grep of `src/rig` for code that still reads the constants.
  - A grep of `test/rig/bench` for grammar seeds.
  - The summary lines of the two run receipts.

  Line numbers are at `dee0320a`. The source files are identical at
  `1ca407cd`.
- **Severity** uses REVIEW-full-spec.md's scale:
  - High: a ruled path stops working, or data is exposed or corrupted, on an
    ordinary path.
  - Medium: a spec clause or the model's decision is broken on a narrow
    path, or a pick contradicts a ruling and has not reached Sid.
  - Low: a gap with no wrong outcome today, or a reading that has not
    reached Sid.

  "Kept record" says whether the finding bears on what a kept record would
  carry.
- **Not repeated here.** REVIEW-full-spec.md's H-1 and M-1, which another
  builder is fixing. 6b does touch M-1's ground: block 2b still re-reads
  persons and lease rows, as 6b's plan says. See "For C".

## Findings: 0 high, 0 medium, 5 low

### L-1 (low; kept record: no): a micro miss always reads `$$layers`, and a re-classed layer's stream-era keys never get a micro row

- **Where.**
  - `shared_reads.clj` 113-131, `key-rows-of>`: it reads the `$$micro` row,
    and whenever that row is nil it reads `$$layers` (130).
  - Its callers:
    - block 1a (`micro.clj` 1437), where the layer's stream settings
      `*ssettings` are already in hand (1403-1407);
    - `micro-marks>` (`shared_reads.clj` 1410-1415), on every shared
      pattern, point and delta read (1508, 1592, 1651);
    - the kv refusals (1469, 1701);
    - the rebuild's put and sweep (1096, 1175).
  - `grammar.clj` 205: `key-row-writes` writes nothing for a use when the
    row it composes on already says `:used true`.
- **What happens.**
  - (a) Groups. A group has no stream era: its settings are micro's, so
    `*ssettings` is nil. Even so, every miss pays a second seek into
    `$$layers`, which holds nothing for it. For a group this is rare, because
    the first admitted use of a key writes its micro row.
  - (b) Re-classed layers. This is the case the fallback exists for, and
    here the cost does not go away.
    1. For a key the layer used in its stream era, and while no micro row
       exists, `key-rows-of>` returns the stream row, which says
       `:used true`.
    2. A micro-era yes composes on that row (`micro.clj` 1011), finds
       `:used` already true, and writes nothing (`grammar.clj` 205).
    3. So no micro row is written for the key unless a micro-era grammar for
       it is admitted.
    4. Until then:
       - every act that uses the key pays a micro miss plus a stream hit on
         hash(L);
       - every shown micro-era row of the key pays the same in
         `micro-marks>`;
       - a `[:kv k]` read pays three seeks, because W2-3's check reads the
         same `$$layers` row again (1474).
  - The plan's cost line is K(2-p), where p is the share of keys that have
    micro rows. It assumes p rises as micro rows get written. For a
    re-classed layer's inherited keys, p stays 0.
- **Why it matters.**
  - Re-class is the way out for a hot layer (IMPLICIT_SPEC OP7), and hash(L)
    is the one task where every act's layer visit lands. This change adds K
    seeks per act on that task, permanently. The skill says: "Never trade
    I/O efficiency for code simplicity."
  - Re-class already has the same shape for permissions: for a re-classed
    layer, block 1a reads the permission rows from both stores (M5). The plan
    chose "the same way it reads settings there" (PLAN-tools-and-grammars
    3.2). So this is a cost the plan chose and stated too low, not a slip.
- **What would fix it.**
  - Two line edits:
    - pass in whether the layer has a stream era (block 1a's `*ssettings`,
      the reads' `*ss`), and skip the fallback when it has none;
    - give W2-3's check the stream row that `key-rows-of>` already read.
  - For (b), the only cheap remedy is to write the stream row into `$$micro`
    to save the read. By the project's rule that is caching (CLAUDE.md,
    "Systemic repair and caching"), so it needs the adversarial examination
    first.
  - The structural question underneath is whether a re-classed layer's
    projections (key rows, settings, permissions) should move to the micro
    store at re-class, since the micro gate orders the layer from then on.
    That question belongs to the re-class design, not to 6b.
- **Phase 7.** Its workloads never re-class (REVIEW-full-spec.md, H-1's
  note), so its numbers do not see this.

### L-2 (low; kept record: no): the rebuild's new helpers lost the try/catch the old ones had, inside the microbatch the whole micro gate runs on

- **Where.**
  - The new helpers are `shared_reads.clj` 618-632 `rebuild-facts` and
    639-644 `rebuild-writes`. `put-page>` (1083, 1099) and `sweep-one>`
    (1170, 1178) call them.
  - The helpers they replace were total:
    - `put-row-writes` (602-614) catches and returns `{}`;
    - `implied-writes` (249-262) catches and returns `no-writes`.
  - `micro-ops>` runs in the micro microbatch topology itself, as its second
    source (`micro.clj` 1722-1728).
- **What happens.**
  - An exception in either new helper now fails the whole microbatch
    attempt. Rama retries the attempt, and the rebuild op is still in the
    batch. So a deterministic exception would stop every shared layer's
    gate, not just the rebuild.
  - I traced the inputs and found no path that throws today:
    - `entry-of` casts `:stamp` and `:batch` with `long`, and both callers
      pass only records answered `:yes` (1079, 1164), which `micro-record`
      always stamps and batches (`micro.clj` 953-967);
    - an erasure date from `open-rows>` is a long;
    - the values it encodes were either decoded as canonical EDN or admitted
      as control values.

  So nothing goes wrong today.
- **Why it matters.** The rig's rule is that every step is total: "a refusal
  is an answer ... never a throw" (PLAN-tools-and-grammars 4.7; phase 0
  finding 3). Here the cost of a throw is the whole micro gate. The old
  helpers kept that rule; the new ones don't.
- **What would fix it.** Wrap each body the way the old helpers were
  wrapped: return `{}` and `no-writes` on a throwable.

### L-3 (low; kept record: yes, the reason on a recorded no): a client with no permission in a layer can keep a key's first hint-setting grammar refused

- **Where.**
  - `micro.clj` 587-594, `grammar-rows`, emits `[:use L k]` for every
    non-store key of every parsed envelope. It does this in block 1a
    (1436-1438), before the name step and before any permission check.
  - Line 1577 joins those rows on every path.
  - Lines 979-981 mark them `:used` for the rebuild check.
  - PLAN_VALIDATION-grammar-micro.md chose this on purpose: the use must
    count "even when the use sorts later or fails permission".
- **What happens.**
  1. The smallest such envelope is one unsealed retract, `{:e x :k k :v
     nil}`, in layer L, sent through the micro door by a client with no
     lease and no permission there.
  2. It needs no lock, so it reaches the fresh path: `arrival-open` answers
     `:status :ok` with no cited ids (469-513). Stage 1's permission checks
     in the fold refuse it later.
  3. In the same batch, a grammar that sets a hint on k (`:by-value`,
     `:no-copy` or opaque) is refused `:grammar-change-needs-rebuild`, and
     that refusal is recorded. This applies while k is still unused in L,
     whether the grammar comes from the operator or from a re-classed
     layer's owner.
  4. One such envelope per batch keeps the grammar refused for as long as
     the sender keeps sending.

  The refused envelope writes no row and no index entry: block 2b writes
  rows only for an act decided yes with this envelope, and block 2d indexes
  only those. F4's reason is to stop values being indexed under hints the
  row no longer states, and that reason does not need this envelope counted.
- **Why it matters.** The admission of a grammar in L then depends on the
  traffic of someone with no permission in L. The record also says the key
  was in use when no admitted act used it. The notes list "the offered use
  set ... includes faces and recorded offers" as a rig choice, but they do
  not state this consequence. The finding is narrow: it only applies while
  k is unused in L, because after the first use every hint change is
  refused anyway.
- **What would fix it, if Sid wants it closed.** Keep "either order" exact,
  but count a use only from an envelope whose permission could pass: one
  that stage 1's permission check (`permit/refusal`, pure over the gathered
  rows) passes under the pre-batch rows, or whose pid another envelope in
  the same batch grants. The other option is to keep the pick and carry the
  consequence to Sid in RIG.md.

### L-4 (low; kept record: no): more reaches the leader than the fold needs

- **Where.**
  - Grammar rows ride every emission. At `micro.clj` 1577 the grammar rows
    join `*rows` after the paths rejoin, so they are sent:
    - once per entity on the fresh path (1511, 1524);
    - once per target entity on a value forget (1532, 1543);
    - once per value on a resend (1570).

    `entity-rows`' docstring (621-624) says each envelope's rows are emitted
    once, with the first entity, and the resend path keeps that rule for the
    name's rows (1570).
  - Per-value subjects are in the skeleton. At `micro.clj` 544 the skeleton
    now carries the per-value `:subjects`, and the skeleton goes to task 0
    (`[[:offer nm fp] sk]`). Only block 2b uses them, and 2b reads them from
    the materialized data on the arrival task (1667). The skeleton's
    docstring (527-532), which lists what may travel to the leader, doesn't
    name them. The plan listed "subject ids" beside the skeleton in the
    materialized data, not inside it.
- **What happens.**
  - An act that touches E entities sends its K key rows and its use rows E
    times to the global aggregation. `+map-agg` keys them, so the fold sees
    each row once and nothing is wrong.
  - The leader also receives which value names whom, and it doesn't use
    this: the record keeps only the union (`record-subjects`, 912-923).
- **Why it matters.** Task 0 is the fold's one task (M3), and the skill asks
  that what funnels there be the minimum. The per-value subjects are person
  ids, not values or locks, so M3's rule holds. This is data minimization,
  not exposure.
- **What would fix it.**
  - Put the grammar rows into `*first-rows` on the fresh path, and into the
    first emission of the other paths, as the name's rows already are.
  - Materialize `(:subjects *arr)` as a field of its own instead of inside
    the skeleton.

### L-5 (low; kept record: no): two of the plan's falsifiers hold by construction, but no test shows them

- **The micro branch of `delta>`.** The new micro branch (`shared_reads.clj`
  1700-1708) runs only on its passing road in `grammar_micro_test.clj`
  (267-272, a `:by-value` key). No test reaches its refusal. Here is the
  case that would: a group's standing `[:kv k]` read, where k's grammar is
  changed to stop indexing k before k's first use (allowed, since k is
  unused). Its next delta should be refused `:not-indexed`.
- **Recorded retries.** PLAN-grammar-micro's falsifier 3 lists "recorded
  retries do not mutate these rows". The cluster retry (211-214) checks the
  retry's answer, subjects and lock, but not the key row. By the code, a
  recorded retry never reaches `key-row-writes`; only a fresh yes does
  (1005-1011). So the evidence is missing, but the behaviour is right.

## Checked against STARTER-6b.md

- **`:key-rows` in `$$micro` on hash(L), written in 2a for a yes, once per
  batch.** Built.
  - Schema: `(grammar/layer-fields)` joins the per-layer projection
    (`micro.clj` 173). It is the typed, subindexed row that phase 6 probed.
  - Writes: a yes composes on the evolving rows and `put`s one whole row per
    `[L k]` (1005-1011). Block 2a's generic entity case sets it with
    `termval`. Refusals and recorded retries never reach it.
  - Test: the pure test `composed-rows-and-refusal-precedence` shows one
    write carrying `:used` and the last grammar.
- **The rows reach the open on the arrival task.** Built, in M1's order:
  1. Block 1a goes `(|hash *layer)`.
  2. It reads settings, permissions, stream-era heads and `key-rows-of>`
     (1401-1439).
  3. It returns with `(|direct *arrival)` (1440).
  4. It opens under `(grammar/grammars-of *krows)` (1460).
- **The fold's rebuild check counts the batch's own uses, in either order.**
  Built. See F4 below.
- **`:grammar-change-needs-rebuild` in `micro/reason-order`, after
  `:control-not-allowed`.** Built (73).
- **`:grammar` in `micro/control-keys`.** Built (50-53). Two effects:
  - the micro door's seal predicate reads this set (`micro_client.clj` 171-175, `value-fact?`),
    so the door now leaves grammar facts unsealed;
  - the parse takes grammar facts as control facts (`not-sealed?`,
    261-273).
- **For a re-classed layer, micro rows first and `$$layers` on a miss.**
  Built in `key-rows-of>`. A present micro row with a nil grammar wins,
  because the fallback runs only on a nil row (130). It is tested in
  "reclass falls back to stream rows, then micro rows take precedence". The
  cost is L-1.
- **Phase 5's shared-layer index writes take the rows' hints at hash(L).**
  Built.
  - Block 1a derives the act's hints on hash(L) (1439).
  - 2b's keyed digests and 2d's index writes take those hints (1680, 1696;
    `shared_reads.clj` 922).
  - The rebuild reads the effective rows on hash(L) (1096, 1175).
- **Phase 4's landing: its subjects come from the target's grammar.** Built,
  with no landing-specific code. A landing's offer layer is its target, so
  `arrival-open` opens it under the target's rows. The test "a landing names
  subjects under the target grammar" gets `#{:bob}` under the group's
  `:landed` grammar.
- **The road.** 6b took road 1 with M1, and the notes reason it through. It
  uses an attempt-local materialized handoff instead of a durable per-act
  row. I agree with the choice; see M1 and T-FR3 below.
- **The places For Sid 65 listed as still reading the constant.** Each one
  now takes rows, hints or saved subjects:
  - `control-keys` (above).
  - The two opens:
    - `arrival-open` (1460) takes the rows' grammars;
    - `row-wraps` (1667) takes block 1's per-value subjects and no grammar.
  - `kv-digests` (1680) and `index-block>` (1696).
  - `micro-ops>`: no constant left, since put and sweep read the rows at
    hash(L).
  - `shared-pattern>`'s parse: form-only `parse-hints`, then the effective
    row's `kv-refusal` (1463-1472).
  - The shared branch of `delta>` (1700-1708) and `opening-answer`:
    `stream-hints` is now form-only for both (1337-1344), and the opening
    wraps an answer the query already authorized.
  - `rig.bench.lock-slice` keeps its own copy. It is untouched and listed in
    the notes.

  A grep of `src/rig` at `dee0320a` finds the constants only in two kinds
  of place:
  - the retained legacy arities: the 3-arities of `locks/read-values`,
    `value-context` and `delivered-context`; the 3-arities of
    `micro/arrival-open` and `row-wraps`; the 2-arity of `kv-digests`; and
    the 3-arity of `reads/parse-pattern`;
  - `locks/empty-context`, which serves `gate/decide`'s pure 7-arity.

  No topology calls any of them. `reads/hints-of` still takes `seed-hints`'
  `:no-copy`, which is the store keys' floor, as phase 6 built it.
- **W2-3.** 6b builds on it. `key-rows-of>` is the micro-first half. W2-3's
  stream-era refusal (1477) and stream marks remain, and `micro-marks>`
  marks only micro-era rows (1404-1415).
- **Step R's clause.** Kept, and `:grammar` fits it:
  - `gate/misplaced-setting?` is unchanged and keys on `env/setting-keys`
    (`gate.clj` 63-72). A grammar's `:e` is its key, so a grammar is not a
    misplaced setting.
  - `refusal-with-rows`' `:malformed-control` clause is unchanged, and the
    micro fold reaches it through the 5-arity (`micro-decision`, 925).
  - The door's `refuse-misplaced!` keys on the same set.
  - A pure test shows that a misplaced `:kind` beside a grammar is
    `:malformed-control`.
- **P3-5 and the replay's seed.**
  - The seed writes the model's grammars through the micro gate into the
    group, and on the first case into the base too (`replay_test.clj`
    857-862).
  - `run-seed!` stops a case on any refused seed act (1685-1699).
  - The guard (1777-1786) now refuses the five mention cases only when the
    resolved shared seed lacks the `:mention` grammar. Its pure test covers
    both sides (2483-2494).
  - The receipt, `runs/grammar-micro-replays.txt` line 7, says "14 played
    (14 as said, 0 differ)".
- **The machinery count's micro side.** See below.

## Checked against the plan: a4, b4, F4, M1, T-FR3

- **a4** (PLAN-tools-and-grammars 8: "the micro gate reads and writes the
  rows in its layer visit"). Holds.
  - Reads happen in block 1a's visit to hash(L). They cover the act's
    distinct non-store keys and each grammar's governed key
    (`grammar/rows-to-read`, 1436): one seek each, or two on a miss (L-1).
  - Writes happen in block 2a: one whole row per changed `[L k]`, for a yes
    only. This matches the plan's 4.7: "for a yes only, each a whole-row
    set ... so a replay writes the same row".
- **b4** ("the micro gate's block 1 reads the rows before it opens values
  ... one hop per act"). Holds.
  - The order is M1's. As the notes say, it costs one transfer per fresh
    offer and one more global barrier, between blocks 1a and 1b.
  - A resend or a recorded retry now also pays the layer visit: two more
    transfers, plus its settings, permission and key-row seeks.
    PLAN-grammar-micro states this.
  - The part that was larger than planned (block 2b opening every value a
    second time) is answered by road 1: 2b reopens values only for digests
    and interprets no grammar (`row-wraps` 1168-1210).
  - 2b's `:plain` is unchanged for every admitted value. It is now the raw
    opened bytes, where before it was `read-values`' `:plain`. But
    `decode-plain` admits only bytes that are already the canonical text
    (`locks.clj` 177-195). So the row digest, the keyed kv digest and the
    rebuild's digest of `encode-value` still agree.
- **F4** (PLAN-tools-and-grammars 4.5: "any envelope of the same batch in L
  offering a fact under k counts as a use, in either order"). Holds, with
  L-3 as its cost.
  - How it is built:
    - uses are precomputed per layer from every parsed envelope (587-594);
    - they are overlaid as `:used` on an immutable snapshot of the pre-batch
      rows (1130, 979-981), for admission only;
    - yes projections compose on separate, evolving rows.
  - Cases I traced:
    - The grammar comes before or after the use in fold order: refused,
      because the use set is complete before the fold starts.
    - A shape-only or subject-only change beside a use: admitted, and it
      governs the next batch, because both the admission and the open use
      pre-batch rows.
    - A use in another layer: no effect.
    - A refused use: it blocks the grammar but writes no `:used`.
    - A re-classed layer whose stream era says `:used true`: every hint
      change is refused.
  - Tests:
    - the pure tests cover both orders and the in-act case;
    - the cluster test covers both orders, the shape change, and a
      missing-lock face counting as a use.
- **M1** (PLAN_VALIDATION-tools-and-grammars 2.2: "+1 hop per act; bare
  locks and plaintext never leave task a"). Holds:
  - The hop carries the parsed envelope, which is ciphertext; a landing's
    box travels with it, sealed. M1's cost statement said as much.
  - `carry-rows` keeps only rows sealed under a person lock (658-665), so an
    operator's bare row stays where it is.
  - `skeleton-offer` replaces sealed bytes and boxes with markers (364-373).
  - The materialized data stays on the arrival task (1479), and block 2b
    routes back there explicitly (1648).
  - The rebuild reduces opened values to keyed digests before hash(L)
    (618-632).
- **T-FR3** ("no grammar version is recorded on a decision; a value's
  grammar is the latest grammar fact for its key admitted in its layer
  before ... its batch"). Holds:
  - Block 1a reads the rows after the previous batch's block 2a and before
    this batch writes any. A retried attempt sees the pre-batch version
    again.
  - The record gains nothing: `micro-record` (953-967) is unchanged.
  - The projection keeps the last admitted grammar in fold order, which is
    stamp order. So "the latest before its batch" stays derivable from the
    layer's log and the records' `:batch`.
  - In a re-classed layer the derivation spans both eras' logs, since a
    micro row is composed from the stream row it replaces.
  - No first-record placeholder is added. T-FR1, T-FR2 and T-FR3 stand, as
    the notes say.

## The rama skill's checks

- **Partition alignment.** Pass. Every new local read runs on its own
  partition:
  - `key-rows-of>` runs after `(|hash *layer)` in block 1a, and after
    `(|hash *L)` in put (`shared_reads.clj` 1093-1099) and sweep (1174-1178);
  - in the shared reads, which run on the layer's task;
  - block 2b's lease and person reads run after `(|direct *arrival3)`
    (1648).
- **Retries and exactly-once.** Pass.
  - All of this runs in the microbatch.
  - Each new write is a whole-row `termval` at a computed location, so a
    retried attempt writes the same thing.
  - The handoff, `$$micro-arrivals`, is attempt-local. In the skill's words,
    materialized PStates are "temporary in-memory PStates for reuse within
    the same microbatch attempt" and are "cleared between microbatches". A
    retry rebuilds the handoff from the depot batch and the pre-batch
    PStates.
- **The caching rule.** The handoff is working data for one attempt, not a
  cache: nothing is reused across batches or requests, and nothing can go
  stale. It does not trigger CLAUDE.md's rule. L-1's second remedy would, so
  I have routed it there.
- **I/O.** Fail, on L-1 and L-4. The new seeks are:
  - K per act on hash(L), or 2K on misses;
  - per shared read: one per distinct shown key, plus one for a `[:kv]` key;
  - per rebuild page: one per distinct key per layer;
  - per swept entry: one.

  There is no new range scan, no new broadcast, and no new depot or durable
  PState.
- **Yielding.** Pass. `key-rows-of>`'s loop yields (`yield-if-overtime`,
  113-131).
- **Subindexing.** Pass. `:key-rows` is subindexed (`grammar.clj` 134-139).
- **Totality.** Fail on L-2. The rest of the new pure code holds:
  - `grammar-rows`;
  - `init-row`'s two new cases, inside `init-w`'s per-row catch;
  - `row-wraps`, inside its try;
  - `locks/open`, which returns nil on a nil lock and never throws.
- **Plan conformance.** Fail on one point: the build matches
  PLAN-grammar-micro.md except for where the per-value subjects sit (L-4).
  There is no other divergence.

## The machinery count's micro side

The ledger says the micro side is 4: 2 promised, 2 unanticipated, 0
capabilities. Adding the stream side's 26 (10, 6, 10) and wave 2's 3 (0, 3,
0) gives 33 (12, 11, 10).

- **Arithmetic.** Holds. 26 + 3 + 4 = 33. By column: promised 10 + 0 + 2 =
  12, unanticipated 6 + 3 + 2 = 11, capabilities 10 + 0 + 0 = 10.
- **Capabilities: 0.** Holds. The diff adds no vocabulary entry, and
  `recipe` and `runner` are untouched.
- **The four units are real.** Each one is new code, or an existing unit
  whose behaviour changed. The notes' inventory of 30 definitions makes the
  boundaries checkable, and I checked that list against the diff.
- **The boundaries follow no single rule.** RIG.md's unit is one named code
  unit added, or one existing unit whose behaviour changes. The stream side's
  26 applied it one plan row at a time; for example, a2 and a8 each span
  several files. The micro line applies it three different ways:
  1. It counts b1's micro half as a unit of its own, "micro F4 gather/fold".
     That half is the in-batch uses, `prepare`'s snapshot, and
     `:grammar-change-needs-rebuild` in `reason-order`.
  2. It folds a1's micro half, `:grammar` in `micro/control-keys`, into a4.
  3. It lumps the shared reads' two kinds of work into one unit, as W2-3
     lumped the stream half:
     - a8's kind, hints where hints are taken: 2b's digests, 2d's index
       writes, the rebuild's put and sweep, and the pattern parse and kv
       refusal;
     - a9's kind, "shown as opaque": `micro-marks>`.

     The 26 counts a8 and a9 as separate units.

  If one rule is used throughout, with each plan row's micro half counted as
  a unit (as the ledger already does for b1), the micro side is 6: a1, a4,
  a8 and a9 promised, b1 and b4 unanticipated. The total is then 35 (14, 11,
  10). If W2-3's namespace lumping is used throughout, the count is lower.
  O21 leaves the unit open, so neither number is wrong, but the ledger should
  state which rule it uses. I would count 6, so that the micro side can be
  read row for row against the stream side it mirrors.
- **The fourth unit's reason.** Its class, (b), is right, because b1 is
  class (b). But the reason the notes give ("b1's single-act rebuild guard
  is insufficient when another envelope in the batch uses the key") reads
  like a new discovery, and it was already planned:
  - the plan's b1 row says "a use in the same act or batch counts
    ([V-F4])";
  - the plan's 4.5 names `micro/prepare` as the place it lands;
  - D-P1 deferred it by name ("F4's in-batch rule").

  Name it "b1, micro half (planned under V-F4, deferred by D-P1)", the way
  a4 is named, so the comparison with the plan's prediction of 25 stays
  honest.
- **Promised or unanticipated, for the shared reads.**
  - The notes class the micro half as promised, because PLAN-reads-rest.md
    1424 says "Phase 6: `seed-hints` becomes the keys' grammar facts, for
    both stores' indexes at once".
  - That line covers W2-3's stream half too, which RIG.md classes as
    unanticipated ("none showed until the branches met").
  - Both halves were found at the same merge (W2-3 and For Sid 65), so one
    rule should class both.
  - I lean towards promised for both. That would move W2-3's unit from (b)
    to (a) in the "apart" line. The same argument reaches W2-1, through the
    promotion sharpening's "the target's grammar".

  That line belongs to RIG.md, so it is C's decision.

## For C (not findings)

- **The two-dot range.** Your range shows phase 7's six harness files as
  deleted. That comes from 6b's fork point (`13c8360b`), not from a
  deletion, and the merge kept the files (see above).
- **M-1's fix.** 6b rewrote two things on M-1's ground (`micro.clj`
  1168-1210, 1644-1667):
  - block 2b's source: it now reads `$$micro-arrivals`, not the depot batch;
  - `row-wraps`' inputs: block 1's per-value subjects, and values opened
    directly.

  A fix written against `749336e9` will need to be rebased onto `dee0320a`.
  The plan kept 2b's re-read of persons and lease rows, so M-1's race is the
  same kind of race as before.
- **Phase 7.** I checked this only by a grep of `test/rig/bench` at
  `dee0320a`. Its grammar writes go through the stream door with
  `:class :by-layer` (`numbers.clj` 395-402, used by `lock_growth.clj` 566),
  and no bench file makes a group. I did not trace whether any workload
  reaches a group in some other way. If none does, 6b's change to the
  group's world does not reach the three numbers.
- **Receipts, spot-checked.**
  - `runs/grammar-micro-suite.txt` ends "Ran 163 tests containing 8085
    assertions. 0 failures, 0 errors." across 26 namespaces.
  - `runs/grammar-micro-replays.txt` line 7 says "14 cases: 14 played (14 as
    said, 0 differ), 0 not practical".

PHASE_VALIDATION:minor-fail
