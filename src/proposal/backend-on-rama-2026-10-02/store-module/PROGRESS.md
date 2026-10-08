# The store's module: where the build stands

For a session that takes this build over, and for the receipt to Sid. Kept by the build session
"softland-11" (Opus 5.5, effort max). Sid, 8 October 2026: "you decide ... pay more now is better than
future"; no test loop after every step; carry on without check-ins; a parallel session
"gate-implementation" may take over if this one's context gets heavy.

## What to read, in this order

1. `../BUILD-store-module.md` — the contract, amended to Sid's answers of 8 October, with the builder's
   rules 1 to 21. It is the full spec.
2. `src-store/README.md` — Sid's decisions of 6, 7 and 8 October. It wins over the contract.
3. This folder's papers, by stage: `IMPLICIT_SPEC.md`, `DECOMPOSITION.json`, then for each part
   `PLAN-<part>.md`, `PLAN_VALIDATION-<part>.md`, and after its build `IMPLEMENTATION_VALIDATION-<part>.md`
   and `TEST_VALIDATION-<part>.md`.

## The stages

The Rama skill's stages, each design stage in a fresh session (the Agent tool, model opus), judged here.

| stage | state | commit |
|---|---|---|
| Sid's decisions written down; contract amended | done | `5c86b4ea` |
| implied spec; rulings on its 12 open points (rules 10–21) | done | `39b58161` |
| split into two parts | done | `0d43c31a` |
| part 1 `streaming-gate-and-record`: plan | done | `90ef0ab0` |
| part 1: check of the plan (minor-fail, fixed; two rulings) | done | `4241b95e` |
| part 1: build (code under `src-store/`, proof under `test-store/`, alias `:store-module`) | done; proofs run here: act 150, pure 343, module 117 passes, no failure | `ee138e28`, `66f22ca3` |
| part 2 `microbatch-gate`: plan (cut off at 05:36 on 8 October by a scheduled power-off; resumed 08:30) | done | `825cf4cf` |
| part 2: check of the plan (minor-fail, fixed; ruling: one copy of a lock's bytes) | done | `e9647a45` |
| part 2: build (the microbatch gate; small changes to part 1's files; the proofs extended) | running since 09:35 on 8 October, as a subagent of softland-11; it reports there and does not commit | — |
| one fresh review of the whole against the contract (`phase-full-spec-review.md`) | to do | — |
| regroup `src-store/softland/store/` into folders (see below) | to do, only on Sid's "go" | — |
| the receipt to Sid | to do | — |

How a stage is run: a fresh agent reads the skill's doc for its stage and the papers above, writes one
paper (or the code), and does not commit. This session reads its report and the parts of its paper that
need a ruling, rules, has the agent apply the rulings, and commits with a message that says what and
why. No `Co-Authored-By`, no session link. Never push.

For part 2's plan, give the planner the check's list of what part 2 will find hard (the end of
`PLAN_VALIDATION-streaming-gate-and-record.md`, and the plan's "For the second part").

## What is left, for the session that takes over

1. When part 2's build has reported: run the three proofs once
   (`clojure -M:store -m softland.store.act-proof`, `clojure -M:store -m softland.store.pure-proof`,
   `clojure -M:store-module -m softland.store.module-proof`), read the build's two papers, and commit
   part 2 as one commit with a message that says what and why.
2. Run the skill's last stage, `phase-full-spec-review.md`, in one fresh session against the contract.
   Fix what it finds, prove again, commit.
3. The folders. Sid asked why the store's files are flat (8 October). Proposed to him, waiting for his
   "go": `door/` (declaration, seal, text, digest), `gate/` (decision, first_act, stamp), `record/`
   (rows, tables), `module/` (module, streaming_gate, microbatch_gate, door_reads), with `act.clj` at
   the top; the proofs follow the same folders. One mechanical commit, proofs passing before and after.
4. The receipt to Sid: the commits, the proofs' count lines, the list below, and every place where the
   contract could not be met as written.
5. Next work, as told to Sid on 8 October: the door is the next build (decision C1 of 6 October); reads
   are the next big deciding (group 6 of the queue; no sheet yet). He asked for the reading and the
   questions. Ask in the simple form that worked on 8 October: one small story, one stop for each
   decision, each with a pick and the reason. He answers in chat.

Sid's printed sheet "The record, and who acts", row by row, as of 8 October: answered — E1·H8, E3·H7 (a
session's reading of his answer to E4, which he let stand), E4, E2, H2; put off — H1 ("do we need to answer
now???"); not gone through — ?4, E5, H10, H11. This build touches none of the last four: ?4 and E5 are
for the door, H10 and H11 for forget.

## For the receipt: what Sid should know, as found so far

1. H1 is open; the module makes a lock for each value. Found: with a lock of about 110 bytes for each
   value, F9's third limit (locks at most 2 times the bytes of the values) is missed for values under
   about 50 bytes. The grain is what moves it.
2. When the system makes a personal layer, the making act's who is `:system` and the permission that it
   names is the person's. His decision does not say under which permission the system acts there; this
   is the builder's reading.
3. A permission fact sits on its holder, so a holder has one permission in a layer.
4. Until sign-in is built, any sender can make any layer that is not made, for any holder, and `:who` is
   not checked.
5. What F2 A ("the record is recomputable from the acts depot") reaches as built: the decisions and the
   current facts of a layer on one task. Not the locks (kept, never made again). Not the stamps (a stamp
   holds the gate's clock time, so a rebuild gives new ones). A refusal for a stamp too far ahead can
   flip at a rebuild. After a failure Rama can decide an act of a layer after a later act of that layer;
   the setting `topology.stream.max.executing.per.task` at 1 closes that window at a cost in throughput
   and stays at its default: his to choose. Rule 9 (the winner between two microbatch acts on different
   entities) waits for part 2.
6. F9's first limit: by the skill's cost of a cold read the plan gives 480 to 770 acts a second on one
   task against the aim of 1,000. A measure will tell; none is taken yet.
7. `act.clj` has one change: its engine takes the map of shapes, so that the door's declaration is data
   checked by the same engine. Its proof must still show 150 passes.
8. The gate opens each sealed value once to check that its lock opens it (rule 16). The build session
   ruled it; the rulings file's ruling 6 points the same way.
9. Numbers of the build, kept and not decided: 10 seconds for a carried stamp ahead of the clock; 256
   facts, 1,024 entries of `:based-on`, 65,536 sealed bytes, 256 locks in a request, 256 characters in an
   id's text.
10. The words that the build made are listed in the module's README for him to strike.
11. The printed sheet of 7 October (job 35) left the print queue; four jobs of 1 October are still in it,
    untouched.
12. Found by part 1's build: a query topology of the module does read a private PState. So a record that
    only the module's gates and queries can read is possible (open item ?11). The record's PStates are
    left readable in this build; the proof looks into them to show the facts.
13. Not reached by any proof of part 1: a retry of an event by Rama, a failed batch (the order window of
    item 5), and F9's three targets. No speed is measured yet.
14. The contract asked for three commits by concern for the module; part 1 is one commit beside the one
    for `act.clj`, because the folder's map changes with its files.
15. From part 2's plan and its check. An answer of a microbatch act can be read only after the next
    iteration has closed its batch, because Rama can run a batch again after one task has shown it. A
    direct read of the tables sees a batch that is not closed (?11). A second attempt of a batch reads
    a later clock, so a refusal for a stamp too far ahead, and with it a winner, can differ between
    attempts; closing keeps a reader from seeing the first. A rebuild from the microbatch depot would
    not pick the same winner between two acts on different entities: the depot holds no order between
    partitions; keeping each batch's cut and clock would make it so (rule 9; not built).
16. One copy of a lock's bytes in a microbatch layer, beside the value. A lock still costs about 245
    bytes for each sealed value there, so F9's third limit is missed below about 90 to 120 bytes for a
    value.
17. The night's run stopped at 05:36 on 8 October because the machine powered off on a shutdown that was
    scheduled at 01:36 for four hours later. Nothing was lost.
