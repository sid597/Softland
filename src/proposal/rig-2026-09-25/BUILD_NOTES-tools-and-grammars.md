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
