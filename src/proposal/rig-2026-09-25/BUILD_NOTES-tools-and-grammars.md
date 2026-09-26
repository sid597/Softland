# Build notes — tools and grammars from inside (rig phase 6)

Builder: Claude Opus 5.5 (`claude-opus-5-5`), effort max, a fresh session,
26 September 2026 from 05:00 IST, as the rama skill's build step
(`references/phase-build.md`) for `PLAN-tools-and-grammars.md` (validated
minor-fail, thirteen fixes in place, `PLAN_VALIDATION-tools-and-grammars.md`;
put through the caching examination, `EXAMINATION-copies.md`). Branch
`rig-build-tools`, worktree `/mnt/data/projects/Softland-rig-build-tools`,
made off `rig-2026-09-25` at `9730010c`. Wave 2, beside phase 4 (promotion,
`Softland-rig-build-promotion`) and the rest of phase 5
(`Softland-rig-build-reads-rest`).

Tags on load-bearing claims: *checked* (read in the named source, or run),
*derived* (reasoned from checked facts), *assumed*.

## Prep (before wave 1 lands)

Written 05:00 to 05:40 IST. No code written, no cluster run. Wave 1 (phases
2 and 3 merged, and their seams) was read in
`/mnt/data/projects/Softland-rig-wave1` at `57c46159`, read only, with its
merge's own test file `test/rig/store/wave1_test.clj` still untracked; it can
change before it lands, so every line number below is as of `57c46159` and
is re-checked after the merge.

### What was loaded

- The rama skill: `SKILL.md`, `references/phases.md`,
  `references/phase-build.md` (the step docs are read in part 2, at their
  steps).
- `SPEC.md` whole; `RIG.md` "Overnight state" (with "Stages tonight"),
  "Found tonight", "For Sid" items 7, 23, 26, 27, 31, 32, 33, "Defaults taken
  overnight, not ruled", "Rig choices so far".
- `PLAN-tools-and-grammars.md` whole; `PLAN_VALIDATION-tools-and-grammars.md`
  whole; `EXAMINATION-copies.md` whole; `BUILD_NOTES-revision-reader.md`.
- The rulings: PROGRESS.md "Now" on main (lines 9-230), read only; and
  `docs/decisions.md` 159-184 ("Tools are records over a vocabulary"; the
  leaves' "designed total language, never runtime code, asked of Sid then",
  line 176-178).
- For merge planning only: the siblings' plans where they touch the same
  seams (`PLAN-reads-rest.md` on `seed-hints` and "Phase 6";
  `PLAN-promotion.md` on the landing's subjects), and Sid's build
  instructions in the relay folder (`build-instructions`).

### Binding points, as wave 1 stands (`57c46159`)

Each row: the plan's name (its section 1 table), what exists, and what
differs. *Checked* by reading the file at the line given.

| Binding point | In wave 1 | Differs from the plan's table |
|---|---|---|
| The gate's control facts | `gate/control-fact?` (gate.clj:59-66: settings, `:permission`, `:revoke`, `locks/lock-control-keys`); `control-value-ok?` (80-102, private, a `case` with **no default**); `control-allowed?` (104-121, private) | Same names. New to the plan: `control-value-ok?`'s `case` throws on a control key it has no clause for, so `:grammar` needs its clause in the same change as `control-fact?`, or every grammar act is an unrecorded `:gate-error`. |
| The refusal order | `gate/refusal [offer settings rows heads]` (165-217): fact-outside, no-such-layer, class-mismatch, the chain's four permission reasons (`permit/refusal`), `:malformed-control`, `:control-not-allowed`, `:stale-replaces`, `:stale-revoke`, `:layer-already-made`, `:unsupported-reclass`; then `locks/lock-refusal` (locks.clj:720-736, L27) | As the validation traced. New: the micro fold calls this 4-arity too (`micro/micro-decision`, micro.clj:842-867), so `:grammar-change-needs-rebuild`, which needs the key rows, goes in a 5-arity with the 4-arity kept. |
| The decision event | module.clj:105-220: intake, the name's record, settings, `locks/deliver-all>`, clock, wall, the permission-rows `loop<-` (150-156), the heads `loop<-` (158-164), `locks/decision-reads>` (165), `gate/decide` 8-arity (166), the writes (171-213) | The key rows' `loop<-` goes after the heads loop and before `decision-reads>`, because phase 2's value checks (and so the wraps, the persons read and the nonces drawn) run inside `decision-reads>`, not in `decide`. |
| The decision | `gate/decide [offer settings rows heads clock wall digest lx]` (392-405) over `decide*` (335-390); hints at gate.clj:356, `(reads/index-writes (reads/current-hints) layer nm log stamp (plain-texts log opened))` | `index-writes` is 6-arity now (the opened plaintexts, wave 1). The rows can reach `decide` inside `lx` with no signature change. |
| `$$layers` schema | module.clj:80-87, `(merge layer-fields (locks/layer-fields) (reads/layer-fields row-fields))` | `:key-rows` joins as a fourth merge argument. |
| Phase 2's grammar | `rig.store.grammar` (grammar.clj, 35 lines): `grammars` = `{:mention {:subjects-at [:persons]}}`, `(subjects-of grammars k v)` total | As planned, `:mention` is data today. |
| Subjects and the value checks | `locks/read-values` (locks.clj:422-458): 3-arity defaults to `grammar/grammars`, 4-arity takes a grammar map; `locks/value-context [offer settings delivered]` (623-630) calls the 3-arity; `decision-reads> [*layer *offer *settings *lk]` (1111-1137) calls `value-context`; `wrap-of` (257-272); `person-owner` (251-255); `lock-plan` (632-657) takes per-value subjects from `read-values` | Differs: the grammar enters through `value-context` inside `decision-reads>`, so both take one more argument (the rows' grammar map). The constant also reaches the micro store's two opens (`micro/arrival-open`, micro.clj:413-438; `micro/row-wraps`, 1063-1095) and phase 2's pure tests (`lock_test.clj:104, 281, 299, 313, 324`, `envelope_test.clj:788`) through the 3-arity. |
| The door | `client/build` (client.clj:71-93; `:claimed-when` defaults to the wall, `:session` to `default-session` when the act has a value fact); `lookup` (251-266, one name); `offer-until-answered!` (275-292); `lease!` (305-320); `stock!` (322-332, **committed**); `default-session` (65-69); `settings` (434); `seed!` (537-550); `grant-offer`, `revoke-offer` (522-535) | `lookup-many` does not exist (b6, new). `assign!` throws after 100 lease tries (client.clj:184), and `offer-until-answered!` rethrows its last error: the runner catches both to stay total. |
| The lease road for a tool (F6) | **W1-1** (commit `ee239a04`, `locks/lease-under`, locks.clj:591-608): a lease by a writer with no person entry (an agent, a tool) is sealed under the layer's person owner; with no person owner the writer is still refused `:no-such-person` | Differs, and simplifies: the plan's runner step 4.0 (lease as the operator with `client/stock!` into the tool's door session) is not needed in a person-owned layer. The door leases as the tool through its own `lease-for!`, rows sealed under Alice. b5 stays "no step", for this reason; the probe in part 2 confirms it. |
| The read exit | `read-exit/read!` (read_exit.clj:88-124) with the planned spec keys; `check-call` (48-67) takes `:permission nil`; `entry-offer` (78-86) builds with `:session nil`, so the door gives the reader's default session; `reads/reader-kinds` has `:tool` (reads.clj:76); `reads/roles`, `exact?`, `entry-facts` (74, 825-854) | As planned; zero steps there. |
| The hint seam | `reads/seed-hints` (reads.clj:38-45), `reads/current-hints` (47-51). Callers: gate.clj:356 (`decide*`); reads.clj:498 (`parse-pattern`'s 3-arity, which the `read-pattern` query calls at 1227); reads.clj:1053 (the `*index-ops` source: rebuild pages and sweeps) | The third caller is the rebuild (index-ops source), not a query topology as the table said; the query reaches the constant through `parse-pattern`'s default arity. `seed-hints` is also edited by phase 5-rest (`:no-copy` gains its standing keys), so I leave that literal alone. |
| `open-row>` | `reads/open-row>` (410-427) is now a pass-through to `locks/open-row>`; the queries call `reads/open-row-with>` (428-437) over `locks/open-row-with>` | The opaque mark (a9) goes on the answer rows (`point-row`, `shown-row`), not in the open. |
| The query topologies | `read-point`, `read-pattern` in `reads/declare-queries!` (1162-), each opening on `(|hash *layer)` before any read | As planned: the pattern key's row is read on the home before `parse-pattern`. |
| Read-entry keys | `reads/read-keys` `#{:read/point :read/pattern}` (72) | As planned. |
| The permission walk | `rig.store.permit` (78 lines, R19): `chain`, `refusal` over the chain's rows; `gate/pids-to-read` (142-153) reads the chain on the home task; a pid is `[who layer in]` or `[who layer in parent]`, at most 4 deep | As planned. A tool's grant under a session's permission (default 5, "narrower still") is now expressible as a 4-element pid; tonight's grant stays a plain operator grant (T-RC7). |
| The envelope's unsealed list | `env/control-keys` (envelope.clj:43-52), which `env/value-fact?` (448-452) and the parse's `value-slot` (316-337) read | `:grammar` must join it, or the door seals a grammar fact and the gate parses it as a value. |
| The micro gate | `micro/control-keys` (micro.clj:45-49), a second copy of the unsealed list; `reason-order` (58-70, 22 reasons); `arrival-open` (413-438); `layer-rows` (518-533) on the `(|hash *layer)` visit after `(|hash *name)`; the fold `prepare` (1019) over `decide-envelope` (885-982), which takes stage 1's reasons from `gate/refusal`; block 2b's `row-wraps` (1063-1095) | Differs, beyond the plan: block 2b opens every value **again** on the arrival task to make its wraps, after block 2a has written the batch's writes, so the grammar must reach two opens in two blocks, and 2a runs between them. The plan's b4 (one hop, block 1 only) does not cover it. See D-P1. |
| The clock | `clock/ms-of` (clock.clj:33-36) | As planned. |
| The revision reader | `rig.revision/read-units`, `read-span` on the rig branch (built, merged) | As planned (validation 4.1). |

### Decisions taken at prep (defaults; the orchestrator can override)

- **D-P1. The micro store's grammar (a4, b4, and F4's in-batch rule) is
  deferred to after wave 2's merge; the micro store keeps the constant
  tonight.** Why: (1) it reorders block 1 of the micro topology and changes
  its fold and block 2a, which phase 4's landing (the landing's subjects
  "from the target's rules", PLAN-promotion.md 105-116) and phase 5-rest's
  shared-layer indexes (written in the deciding batch, taking `seed-hints`,
  PLAN-reads-rest.md 228-238, 1380-1392) change in parallel, so a merge would
  not be mechanical; (2) the built micro store opens each value twice, in
  block 1 and again in block 2b after 2a's writes (`row-wraps`), which the
  plan's b4 did not see: giving 2b the grammar in force before the batch is
  a design question (a second hop pair per act in 2b, or subjects carried
  from block 1 in batch state, or F4's rule widened so a batch never holds a
  grammar for k beside a value under k), not a build detail; (3) time.
  Meanwhile a grammar fact offered to a shared layer is refused as data, not
  ignored (either `:not-sealed` on its face, since `micro/control-keys` lacks
  `:grammar`, or `:control-not-allowed` if I add `:grammar` there and to
  `foreign-control-keys`, two set literals). The count then has a4 and b4 as
  "planned, deferred", and the receipt says the micro gate still checks the
  compiled constant.
- **D-P2. The runner leases through the door as the tool (W1-1), not as the
  operator.** F6's operator `stock!` step is dropped: in a person-owned
  layer a tool's own lease is sealed under the layer's owner. Nothing in
  store code; the probe confirms it before the runner is written.
- **D-P3. The constants stay in source for what still reads them.** The
  stream gate and the one-owner exit stop consulting `grammar/grammars` and
  `reads/seed-hints`' `:by-value #{:note}` (a6, a8); they read the layer's
  rows only. `grammar/grammars` stays as the 3-arity default the micro
  store's two opens and phase 2's pure tests use, until a4. `seed-hints` is
  not edited (phase 5-rest edits the same literal); a new `reads/hints-of`
  takes the store keys' `:no-copy` from it and everything else from the
  rows. At the merge, phase 5-rest's shared-layer callers of the constant
  are named as the place a4's rows plug in.
- **D-P4. Earlier suites get the toy grammars as facts from a test-side
  helper**, a new test namespace holding the two toy grammar facts and a
  function that writes them into named layers (the plan's "test helper
  beside `client/seed!`"), called by each affected fixture right after
  seeding and before that layer's first `:note` or `:mention`. Not in
  `client/seed!` or `model-world`: extra facts in every seeded layer would
  change `[:all]`-type answers and the model comparison, and would put the
  toy's grammar back in `src/`.

### Shared-file changes (expected; the final list is at the end)

Source (each kept to the lines named; new work goes in new namespaces):

- `src/rig/store/envelope.clj`: `:grammar` joins `control-keys` (one token):
  unsealed at the door, a plaintext control value in the log (a1, T-FR2).
- `src/rig/store/gate.clj`: `control-fact?` knows `:grammar`;
  `control-value-ok?` gets its `:grammar` clause through `grammar/parse`;
  `control-allowed?` lets the layer's owner write one (T-RC4); `refusal`
  gains a 5-arity with the key rows (a store key's grammar and two grammar
  facts for one key inside `:malformed-control`; `:grammar-change-needs-rebuild`
  after `:control-not-allowed`, counting a use in the same act, F4), the
  4-arity kept for the micro fold; `intake` names the keys whose rows the
  event reads; `decide*` hands `reads/index-writes` the rows' hints and
  returns the changed rows (a1, a2, a8, b1).
- `src/rig/store/module.clj`: `:key-rows` as a fourth argument of the
  `layers-schema` merge; one `loop<-` reading the rows after the heads loop;
  `decision-reads>` given them; one write block for the changed rows in the
  yes branch (a2, a3).
- `src/rig/store/locks.clj`: `value-context` and `decision-reads>` take the
  rows' grammar map (one parameter each, 3-arity defaults kept);
  `read-values`' naming step calls one grammar function that checks the
  shape, skips an opaque key and names subjects (a6, a7).
- `src/rig/store/grammar.clj`: phase 2's namespace, the one phase 6 grows:
  `parse`, `row`, the row schema, `grammars-of`, `hints`, the naming call;
  `grammars` and `subjects-of` unchanged (a5 to a7).
- `src/rig/store/reads.clj`: `hints-of` beside `current-hints` (D-P3); the
  `read-pattern` query reads the pattern key's row before `parse-pattern`;
  both queries read the rows of the keys they show and mark opaque rows;
  the `*index-ops` source takes the page's rows' hints (a8, a9).
- `src/rig/store/client.clj`: `lookup-many`, appended (b6).
- `src/rig/store/read_exit.clj`: nothing expected.
- `src/rig/store/micro.clj`: nothing under D-P1, or the two set literals.
- New: `src/rig/store/shape.clj` (a5), `recipe.clj` (a10, b2, b3, the
  vocabulary), `runner.clj` (a11).

Tests: new `rig.store.shape-test`, `rig.store.grammar-test`,
`rig.store.recipe-test`, `rig.store.tools-test`, and the D-P4 helper. Earlier
suites expected to change, each expectation listed with its reason after the
runs: `stream_gate_test` (E1's "the grammar's Bob"), `forget_test` and
`lock_test`'s cluster cases (mentions of Bob), `read_exit_test` and
`reads_test` (`[:kv :note ...]`), `read_model_test` (V-F12), and whatever
`wave1_test` relies on. Never `gate_test.clj`.

### Order of work (part 2)

0. Merge `rig-2026-09-25` at the commit the orchestrator names; take its
   merged-suite log as the baseline, or run the named suite under the lock
   in the background into `runs/`.
1. Re-check this table against the merge. Probes under the lock, output in
   `runs/`: (a) the exit with the operator as reader and `:permission nil`;
   (b) a tool with only a grant in `:alice` reading through the exit and
   offering an output under its pid, the door leasing as the tool (D-P2).
2. Pure namespaces and their tests, no cluster: `shape`, the `grammar`
   additions, `recipe` (`parse-tool`, `run`, `loop-free`, `run-name`, the
   vocabulary with `:revision/*` bound to the reader behind the operator's
   `{:repos {id path}}`, empty by default).
3. The stream gate: the control key, the rows' read and write, the value
   checks from the rows, the refusals, the hints in `decide*`. Then phase
   1's, 2's and 5a's suites with the D-P4 helper where they relied on the
   constants.
4. The exit: the pattern key's row before parsing, the opaque marks, the
   rebuild's hints from rows.
5. `client/lookup-many`, then `rig.store.runner/run-pass!`.
6. `rig.store.tools-test`: G1 to G7, R1 to R5.
7. `IMPLEMENTATION_VALIDATION-tools-and-grammars.md` (default FAIL until
   scenario-traced), fixes; `TEST_VALIDATION-tools-and-grammars.md`, fixes.
8. The whole suite, every namespace named, in the background, logged in
   `runs/`.
9. The machinery count on the build; the rest of these notes, with "For
   RIG.md".

If time runs short, the order holds and the stream store's proof is
finished and committed whole before anything after it.

### The machinery count, predicted (the plan's section 8, to compare)

25 built tonight: 11 + 5 + 9; b7 deferred; b5 resolved to none.

| # | Class | Step | Expectation after reading wave 1 |
|---|---|---|---|
| a1 | promised | `:grammar` a control key (`control-fact?`, `control-value-ok?` via `grammar/parse`, `control-allowed?` for the owner, the unsealed list) | as planned, over two files (gate, envelope); the micro copy of the list under D-P1 |
| a2 | promised | the `:key-rows` rows written in the decision event | as planned |
| a3 | promised | the stream gate reads the act's key rows in its one event | as planned, before `decision-reads>` |
| a4 | promised | the micro gate reads and writes the rows | deferred (D-P1) |
| a5 | promised | the shape language and its checker | as planned |
| a6 | promised | `read-values` checks the shape and takes subjects from the rows | through `value-context` and `decision-reads>` |
| a7 | promised | opaque: no shape, no value subjects, `:opaque` in the hints | as planned |
| a8 | promised | index hints from the rows at the three places | the three are `decide*`, `parse-pattern` via `read-pattern`, and the rebuild |
| a9 | promised | "shown as opaque" | on the answer rows |
| a10 | promised | the recipe executor | as planned |
| a11 | promised | the minimal runner | as planned, without F6's lease step (D-P2) |
| b1 | unanticipated | `:grammar-change-needs-rebuild` and `:used` | as planned, stream only |
| b2 | unanticipated | the run's derived name and derived content | as planned |
| b3 | unanticipated | the loop check | as planned |
| b4 | unanticipated | the micro gate's block 1 reads the rows before it opens | deferred (D-P1); larger than planned (block 2b) |
| b5 | resolved | a lease road for a tool actor | none, now by W1-1 |
| b6 | unanticipated | `client/lookup-many` | as planned |
| b7 | deferred | a "start after" bound on the pattern read | deferred, as planned |
| c1-c3 | capability | `:emit`, `:revision/read-units`, `:revision/read-span` | as planned |
| c4-c9 | capability | `:lit`, `:in`, `:got`, `:count`, `:str`, `:map` | as planned |

Expected on the build if D-P1 holds: 23 built (10 + 4 + 9), with a4 and b4
deferred beside b7. Any step the build needs that is not in this table is
reported as found, with its class and why.

### What could block

Nothing blocks the prep. Part 2 waits on wave 1's merge. Two things would
change the plan of work: the orchestrator overriding D-P1 (then a4 and b4
need a short design call on block 2b first), and wave 1's final form of
`decision-reads>`, `lease-under` or the hint seam differing from
`57c46159`.

## Phase 6b: the micro gate's grammar, and the question block 2b needs answered

D-P1 was approved by the orchestrator (26 September, part 2's go) on two
conditions: the micro gate's grammar (a4, b4, and F4's in-batch rule)
becomes a named step, "phase 6b", right after wave 2's merge; and the
machinery count is reported in two parts, the stream side now and the
micro side after 6b, so the deferral hides no compiled step. Until 6b the
micro store keeps the compiled constant (`grammar/grammars`, through
`locks/read-values`' 3-arity in `micro/arrival-open` and `micro/row-wraps`)
and refuses grammar facts as data: `micro/control-keys` lacks `:grammar`,
so the micro client seals a grammar fact like a value, its value never
reaches the fold, and `gate/refusal` answers it `:malformed-control`
(tested in `rig.store.grammar-test`, the 4-arity case).

**What 6b must build** (from the plan and the built micro store):
`:key-rows` in `$$micro`'s per-layer state on `hash(L)`, written in block
2a for a yes (a grammar fact, a first use), exactly once per batch; the
rows reaching the open on the arrival task; the fold's rebuild check
counting the batch's own uses in either order (V-F4, `micro/prepare`);
`:grammar-change-needs-rebuild` in `micro/reason-order` after
`:control-not-allowed`; `:grammar` in `micro/control-keys`; for a
re-classed layer the micro rows first and `$$layers`' on a miss; and the
two places the siblings built against the constants: phase 5's shared-layer
index writes (they take `seed-hints`; they need the rows' hints at
`hash(L)`) and phase 4's landing (its copy's subjects are "the target's
grammar", computed by the micro gate's open).

**The question block 2b needs answered first.** The plan's b4 assumed one
open per act, in block 1 on the arrival task. The built micro store opens
each sealed value twice on the arrival task: in block 1 (`arrival-open`,
the value checks and the act's subject union, which travel to the leader
in the skeleton) and again in block 2b (`row-wraps`), after block 2a has
written the batch's writes, to compute each lock's wrap from the value's
own subjects. With grammars from facts, both opens need the grammar in
force before the batch (T-FR3: "the latest grammar fact admitted before
its batch"), and the rows live on `hash(L)`, not on the arrival task. So:
how does block 2b get the pre-batch grammar, or the subjects block 1
computed under it? The roads I see, for 6b's design session to choose:

1. **Carry the subjects, not the grammar.** Block 1 computes each value's
   own subjects once, under the rows it fetched, and they reach block 2b
   on the arrival task through batch state (a row written in the gather,
   or in 2a by the leader from the skeleton, which would then carry
   per-value subjects, person ids only, no plaintext). 2b applies no
   grammar at all. Costs one row write and read per act, no hop in 2b.
2. **Fetch the rows again in 2b.** A hop pair to `hash(L)` and back per act
   in 2b, on top of block 1's. After 2a the rows hold this batch's grammar
   writes, so 2b would need either versioned rows ("the grammar before
   batch b") or road 3's rule.
3. **Never mix a grammar change with a use in one batch.** Widen F4: the
   fold refuses any grammar fact for k in L (not only a hint-changing one)
   when an envelope of the same batch in L offers a fact under k. Then the
   rows 2b reads after 2a equal the pre-batch rows for every key a value in
   the batch uses. It still needs road 2's hops, and it makes a grammar
   write in a busy shared layer retry across batches.

My lean, for 6b to test rather than take: road 1 with the plan's M1
reorder in block 1 (the rows reach the arrival task before the open, one
hop per act), so the grammar is applied once, where the plaintext is, and
block 2b derives nothing from a grammar. It keeps the micro plan's rule
that neither a plaintext value nor a bare lock leaves its task.

## Build (part 2, from 05:45 IST)

### Log

- 05:45. Merged `rig-2026-09-25` at `2034cce5` (wave 1 landed), clean. The
  binding table above held; one change since `57c46159`: `gate.clj` gained
  `micro-control-keys` and `stream-refusal` (W1-7), outside my edits.
- Pure parts, `c38bcbf5`: `rig.store.shape`, the grammar fact in
  `rig.store.grammar`, `rig.store.recipe`. Loaded with reflection warnings
  on: none.
- Merged phase 4's extraction `1b1d416c` (`rig.store.gate-event`) as the
  orchestrator asked, then made the key-row read and write inside its ops.
- 06:0x. The probe (`runs/phase6-tools-lease-probe.txt`), first run, all
  as expected: a grammar fact admitted and projected into its row, a value
  refused `:value-shape` by it, `:used` set on first use, the operator
  reading through the exit with permission nil, a tool with only a grant
  reading and writing in Alice's layer with its lease rows sealed under
  Alice (62 unconsumed of 64). D-P2 confirmed: no runner lease step.
- Commits `74fdfcc3` (the stream gate), `fdedd710` (the exit), `9511ffa4`
  (the runner and `lookup-many`). A `git reset` in my first commit command
  waited for approval and was stopped by the orchestrator; nothing had run;
  the three commits were then made with `git commit -- <paths>`.
- The pure tests, `c747acd8`: 12 tests, 217 checks, 0 failures, no
  cluster. The first run found the plan's own test tool unparseable: its
  `" named"` is a bare string where the plan's formula table allows only
  vectors; a plain scalar now stands for itself (D5 in the implementation
  validation).
- `IMPLEMENTATION_VALIDATION-tools-and-grammars.md`, `b9e92982`: minor-fail,
  one fix (the queries' key-row loops yield).
- `TEST_VALIDATION-tools-and-grammars.md`: minor-fail, four cases added to
  `tools-test` (R6 a replaced tool, two runners at once, G3's opaque forget,
  G2's kept wrap).
- 06:47 to 06:49. The suites the change touches, with the proof: 45 tests,
  2,344 checks, one failure (R3 counted three mentions; the two-runners
  case added a fifth). Fixed in the test (`32905440`); the proof rerun
  alone, 06:52: 160 checks, 0 failures.
- 06:53 to 07:00. The full suite, once: 106 tests, 5,907 checks, 0
  failures, 0 errors.

### What was built

- **Grammar facts checked at the stream gate.** `:grammar` is a control
  key (unsealed, plaintext, never forgotten by a value forget), admitted
  for the operator or the layer's owner, one per key per act, never on a
  store key (`gate/store-key?`: the control keys, `:grammar`, the read
  entries' keys). The layer's gate projects it into `[L :key-rows k]`,
  with `:used` set by the first fact under k, and reads the rows of the
  act's keys in the decision's one event, before phase 2's value checks,
  which take their grammars: the shape check (`rig.store.shape`, a total
  language of plain data with bounds and a visit budget), no shape and no
  value subjects for an opaque key, subjects from `:subjects-at`. A grammar
  changing a used key's hints or opacity (or one the act writes) is
  refused `:grammar-change-needs-rebuild`.
- **Hints and marks at the read exit.** Index writes, a `[:kv]` read's
  refusals (applied after the visibility check), the rebuild's pages and
  the "shown as opaque" marks all read the layer's rows.
- **Tool facts and the recipe executor** (`rig.store.recipe`): the tool
  fact's parse, a run over one matched row, the vocabulary (`:emit`, the
  revision reader's two steps behind the operator's repository ids;
  `:lit`, `:in`, `:got`, `:count`, `:str`, `:map`, a bare scalar), the loop
  check, the run's derived name.
- **The minimal runner** (`rig.store.runner/run-pass!`) and the door's
  `client/lookup-many`.
- **Not built, by decision (D-P1):** the micro gate's grammar, phase 6b.

### Divergences

The implementation validation lists D1 to D8 with their reasons: the micro
grammar deferred (D1); no runner lease step (D2, W1-1); the constants kept
in source for what still reads them (D3); a `[:kv]` read's grammar applied
after the visibility check (D4); a bare scalar as a formula literal (D5);
an `:emit` of nil fails its step (D6); a replaced tool is not run (D7); the
proof's G cases in their own layers, Gus forgotten instead of Bob (D8).

### The machinery count (the stream side; the micro side comes with 6b)

Taken on the build, in the plan's unit: one named code unit added to the
fixed side or to the vocabulary, or one existing unit whose behaviour
changes (the plan's assumed definition, IMPLICIT_SPEC O21 leaving it open).
It is the machinery count, not the thesis count.

**(a) Fixed-side steps the frame already promised: 10 built, 1 in 6b.**

| # | Step | Where | Why it was needed |
|---|---|---|---|
| a1 | `:grammar` a control key | `envelope.clj` (`control-keys`), `gate.clj` (`store-key?`, `control-fact?`, `control-value-ok?`'s clause, `control-allowed?` for the owner) | a grammar is a fact the gate reads on every later decision, so it cannot sit behind a lock a forget destroys (T-FR2) |
| a2 | the key rows written in the decision's event | `grammar.clj` (`row`, `row-schema`, `layer-fields`, `key-row-writes`), `module.clj` (the schema), `gate.clj` (`decide*`'s `:key-rows`), `gate_event.clj` (`write-decided>`) | the projection the gate reads without opening the log, as settings and permission rows are |
| a3 | the stream gate reads the act's key rows in its one event | `grammar.clj` (`rows-to-read`), `gate.clj` (`intake`), `gate_event.clj` (`record-or-decide>`'s loop), `locks.clj` (`decision-reads>` takes them) | every input of a stream decision is local to its event (the rig's invariant) |
| a4 | the micro gate reads and writes the rows | phase 6b | ruling 1's second gate |
| a5 | the shape language and its checker | `shape.clj`, `grammar.clj` (`refusal`) | ruling 6's shape check |
| a6 | the value checks take shape and subjects from the rows | `grammar.clj` (`grammars-of`, `named`), `locks.clj` (`value-context`'s grammars, `read-values`' naming call) | ruling 8's second source of subjects, from facts |
| a7 | opaque: no shape, no value subjects, no index | `grammar.clj` (`named`, `hints`) | ruling 6 |
| a8 | index hints from the rows where hints are taken | `reads.clj` (`hints-of`; the gate's index writes; `read-pattern`'s `[:kv]` refusal; the rebuild's pages through `key-rows-of>`, `page-keys`, `found-keys`) | the read exit's promise that phase 6 replaces its constant |
| a9 | "shown as opaque" | `reads.clj` (`shown-keys`, `mark-opaque`, the two queries' key-row loops) | ruling 6 |
| a10 | the recipe executor | `recipe.clj` (`parse-tool`, `run`, `eval-formula`, the formula and step checks) | decisions.md: one executor with several runners |
| a11 | the minimal runner | `runner.clj` (`run-pass!`, `output-act`) | SPEC phase 6 |

**(b) Fixed-side steps nobody anticipated: 6 built, 1 in 6b, 1 deferred.**

| # | Step | Where | Why it was needed |
|---|---|---|---|
| b1 | `:grammar-change-needs-rebuild` and `:used` | `grammar.clj` (`hint-change?`, `rebuild-refusal`), `gate.clj` (`refusal`'s 5-arity) | a changed hint on a used key leaves its indexes wrong without a rebuild; the act's own use counts (V-F4) |
| b2 | the run's derived name and content | `recipe.clj` (`run-name`), `runner.clj` (`output-act`: `:claimed-when`, session) | once per match with no runner state; two runners must offer one map (V-F5) |
| b3 | the loop check | `recipe.clj` (`loop-free`, `matched-key`) | nothing in the frame stops a tool feeding itself, the runner's own writes included (V-F7) |
| b4 | the micro gate's open gets the rows | phase 6b | the open is on the arrival task, the rows on `hash(L)`; and a second open in block 2b |
| b6 | `client/lookup-many` | `client.clj` | one roundtrip per tool for which matches ran, not one per match |
| b7 | a "start after" on the pattern read | deferred, as planned | a tool past `limit` matches reads the same first page |
| b8 | the exit applies a key's grammar after its visibility check | `reads.clj` (`parse-hints`, `kv-key`, `kv-refusal`) | found in the build: a refusal before it would tell a reader who cannot see a layer what its grammar says (the read exit's F4) |
| b9 | a replaced tool is not run | `runner.clj` (`tools-in`) | found in the build: `[:k :tool]` shows every version, and running both would double every output for ever |
| b5 | a lease road for a tool actor | none | wave 1's W1-1 seals a tool's lease under the layer's owner |

**(c) Capabilities: 10 vocabulary entries.** `:emit`;
`:revision/read-units` and `:revision/read-span` (one capability, the
revision reader, as two steps); the formulas `:lit`, `:in`, `:got`,
`:count`, `:str`, `:map`; and a bare scalar standing for itself (found:
the plan's own test tool writes `" named"`).

**Against the prediction.** Predicted 25 built tonight (11 + 5 + 9); with
D-P1, 23 expected. Built on the stream side: 10 + 6 + 10 = 26. The three
more than expected are b8, b9 and the bare scalar, each found by the build
and none by the plan; a4 and b4 move to 6b, and b4 is larger than planned
(block 2b). **The zero:** after these, the proof's test grammar (`:mention`)
and test tool (`:mention-count`) were added by writing facts only
(`rig.store.tools-test`, EDN in the test); nothing under `src/` names
`:mention-count`, and the stream gate and the exit read `:mention`'s and
`:note`'s grammars only from facts. The micro store still compiles
`:mention` into `grammar/grammars` (and a bench, `lock_slice.clj`, keeps
its own copy for phase 7): that is the micro side's count, after 6b.

## For RIG.md

### Rig choices (the orchestrator assigns R numbers)

- **P6-1. The micro gate's grammar is phase 6b** (D-P1, the
  orchestrator's). Until then the micro store checks the compiled
  constant and refuses a grammar fact `:malformed-control`; the question
  block 2b needs answered is above.
- **P6-2. A tool leases as itself through the door** (D-P2): wave 1's W1-1
  seals its lease rows under the layer's person owner, so the owner's
  forget reaches them; no runner lease step.
- **P6-3. A replaced tool is not run**: the runner runs a tool fact only
  while no other tool fact in the page replaces it; its outputs stay.
- **P6-4. A `[:kv]` read applies its key's grammar after the visibility
  check**, so a reader who cannot see a layer learns nothing of its
  grammars.
- **P6-5. In a recipe, a bare scalar stands for itself, and an `:emit`
  whose value is nil fails its step** (a nil would be a retract).
- **P6-6. The constants stay in source for what still reads them** (D-P3):
  `grammar/grammars` for the micro store's opens and phase 2's pure tests,
  `seed-hints` unedited (phase 5's rest edits it); the stream gate and the
  exit read only rows.
- **P6-7. Earlier suites write the toy grammars as test data**
  (`rig.store.toy-grammars`, D-P4), never from `src/`.
- **The plan's T-RC1 to T-RC12 stand as built**: rows per key read per
  distinct key, `:key-rows` subindexed; a key with no grammar permissive;
  the rebuild refusal and `:used`; grammar writers the operator and the
  owner; the shape bounds (8 deep, 256 nodes, 8 branches, 64 values) and
  budget (65,536); the run name's derivation (SHA-256, version 8 UUID, no
  secret); a tool's grant a plain operator grant; opaque means never
  interpreted (the gate still unseals); a shown opaque value marked; the
  key-level loop graph in stamp order; the runner as operator code by
  passes; a mark with no grammar admitted.

### Questions for Sid

The plan's Q1 to Q10 stand (PLAN-tools-and-grammars.md section 12), none
answered by the build: where a key's grammar has authority (per layer, or
store-wide); one key with two grammars; opaque and the lock; a key with no
grammar, and a mark on one; changing a used key's index or opacity; a
run's derived name within "names: random"; the tool as a source of
subjects; who writes grammars and tools, and grants a tool; what a new
tool owes history (with P6-3: is replacing the tool fact how a tool is
changed, and does the old version stop?); what a capability may reach.
The build adds one: the leaves' language (For Sid 31): tonight's formulas
now include a bare scalar, still not "a designed total language".

### First-record placeholders (as built)

T-FR1 the grammar fact `{:e <key id> :k :grammar :v {:shape :subjects-at
:opaque :index}}` with the shape language's vector forms; T-FR2 `:grammar`
a control key; T-FR3 no grammar version on a decision (a value's grammar
is the latest grammar fact for its key admitted in its layer before its
act); T-FR4 the tool fact `{:e <tool id> :k :tool :v {:matches :signature
:permission :recipe}}`, acting as its own id; T-FR5 a run's output act
(`:who` the tool, its pid, `:stood-on` the matched fact and the tool fact
with their stamps, `:because-of` the matched act, `:subjects #{}`,
`:claimed-when` the later stamp's millisecond, the tool's door session,
and a name derived from the layer, the tool fact and the match); T-FR6 the
runner's read entries (the operator's `[:k :tool]` line with role
`:stood-on`, each tool's match read with role `:matched`); T-FR7 a
grammar lives in the layer it governs.

## Shared-file changes (the final list, for the merge)

My commits on `rig-build-tools` after `9730010c`, excluding the two merges
(`rig-2026-09-25` at `2034cce5`; phase 4's extraction `1b1d416c`, which
brought `gate_event.clj` and phase 4's prep notes). Lines added/removed
against the state I merged. Every edit is local to the lines named; new
work is in new namespaces.

| File | +/- | What changed, and why |
|---|---|---|
| `src/rig/store/envelope.clj` | +7/-2 | `:grammar` joins `control-keys` (unsealed at the door, plaintext in the log; T-FR2), and its docstring |
| `src/rig/store/gate.clj` | +55/-16 | requires `rig.store.grammar`; new `store-key?`; `control-fact?` and `control-value-ok?` (a `:grammar` clause) and `control-allowed?` (the owner) know grammars; `refusal` becomes a 4-arity (the micro fold's, unchanged behaviour) and a 5-arity over the private `refusal-with-rows`, which adds two grammars for one key to `:malformed-control` and `:grammar-change-needs-rebuild` after `:control-not-allowed`; `intake` adds `:keys`; `decide*` reads `:key-rows` from `lx`, takes hints from `reads/hints-of`, returns `:key-rows` |
| `src/rig/store/gate_event.clj` (phase 4's) | +16/-2 | inside `record-or-decide>`: the key-row loop after the heads loop, and `decision-reads>` given the rows; inside `write-decided>`: one write block for `:key-rows`, in the yes branch |
| `src/rig/store/locks.clj` | +19/-11 | `read-values`' naming step calls `grammar/named`; `value-context` gets a 4-arity with grammars (the 3-arity keeps the constant); `decision-reads>` takes `*key-rows`, turns them into grammars, and returns `lx` with `:key-rows` |
| `src/rig/store/module.clj` | +5/-2 | requires `rig.store.grammar`; `(grammar/layer-fields)` as a fourth argument of `layers-schema`'s merge |
| `src/rig/store/grammar.clj` | +241/-8 | phase 2's namespace, grown: header; the grammar fact's admission, row, schema, rows to read, rebuild check, row writes, grammar map, `named`, `hints`; `grammars` and `subjects-of` unchanged but for a docstring |
| `src/rig/store/reads.clj` | +125/-4 | requires `rig.store.grammar`; new `hints-of`, `parse-hints`, `kv-key`, `kv-refusal`, `shown-keys`, `mark-opaque`, `page-keys`, `found-keys`, `key-rows-of>`; the `*index-ops` source's `current-hints` line replaced by per-page hints in the put and sweep branches; `read-point` and `read-pattern` mark opaque rows (yielding key-row loops); `read-pattern` parses with `parse-hints` and answers `kv-refusal` in its `<<cond` after the visibility cases. `seed-hints` and `current-hints` untouched |
| `src/rig/store/client.clj` | +11/-0 | `lookup-many`, beside `lookup` |
| New | | `src/rig/store/shape.clj`, `recipe.clj`, `runner.clj`; `test/rig/store/shape_test.clj`, `grammar_test.clj`, `recipe_test.clj`, `tools_test.clj`, `toy_grammars.clj`; `runs/probes/rig/probe/tools_lease_probe.clj` |
| `test/rig/store/stream_gate_test.clj` | +3/-0 | the toy grammars in Alice's three layers after the seed (E1's Bob) |
| `test/rig/store/forget_test.clj` | +6/-1 | the toy grammars in each layer its `layer!` makes, and after the seed and the base |
| `test/rig/store/wave1_test.clj` | +6/-1 | the same, in its `layer!` and after the seed and the base |
| `test/rig/store/read_exit_test.clj` | +5/-0 | the same, in `make-layer!` and after the seed |
| `test/rig/store/reads_test.clj` | +7/-3 | `lx-for` decides with the toy rows (`gate-integration`); requires ordered |
| `test/rig/store/read_model_test.clj` | +8/-2 | the toy grammars before each history; `:grammar` joins `door-keys`, left out of the comparison |

What the merge with the siblings must wire (not mechanical, named here so
it is done on purpose): phase 5's rest built its shared-layer index writes
and reads against `seed-hints` and the micro store's constant; they read
facts only once phase 6b gives the micro store its rows. `micro.clj` is
untouched by this branch.

## The suite

One full run at the end, every namespace named: the 14 wave 1 namespaces
and phase 6's four (`rig.store.shape-test`, `rig.store.grammar-test`,
`rig.store.recipe-test`, `rig.store.tools-test`). **106 tests, 5,907
assertions, 0 failures, 0 errors**, lock 06:53:00 to 07:00:32 IST (7 min
32 s), at `32905440`; record `runs/phase6-suite.txt` (the runner's lines;
Rama's log left out). Before it, under Sid's rule, only what each change
touched: the pure namespaces (12 tests, 217 checks, no cluster); the
suites the gate change touches with the proof (45 tests, 2,344 checks, 1
failure, R3's count, `runs/phase6-stream-suites.txt`); the proof alone
after its fix (1 test, 160 checks, `runs/phase6-tools-test-rerun.txt`).
Every earlier suite passed with the toy grammars written as facts, and
the micro suites, which still read the constant, passed unchanged.
