# The store's module: where the build stands

For a session that takes this build over, and for the receipt to Sid. Kept by the build session
"softland-11" (Opus 5.5, effort max). Sid, 8 October 2026: "you decide ... pay more now is better than
future"; no test loop after every step; carry on without check-ins; a parallel session
"gate-implementation" may take over if this one's context gets heavy. It did, on 8 October, after both
parts were built: the session "gate-implementation" (Opus 5.5, effort max) ran the review of the whole,
ruled on it, and gave the receipt. The folders wait for Sid's "go" there.

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
| part 2: build (the microbatch gate; small changes to part 1's files; the proofs extended) | done; proofs run by softland-11: act 150, pure 429, module 218 passes, no failure | `3420abcf` |
| one fresh review of the whole against the contract (`phase-full-spec-review.md`) | done: no fault in the module's code; twelve repairs to maps and proofs; five rulings by "gate-implementation"; proofs run there: act 150, pure 433, module 222 passes, no failure | `b6646187` |
| regroup `src-store/softland/store/` into folders (see below) | to do, only on Sid's "go" | — |
| the receipt to Sid | given in chat by "gate-implementation", 8 October | — |

How a stage is run: a fresh agent reads the skill's doc for its stage and the papers above, writes one
paper (or the code), and does not commit. This session reads its report and the parts of its paper that
need a ruling, rules, has the agent apply the rulings, and commits with a message that says what and
why. No `Co-Authored-By`, no session link. Never push.

For part 2's plan, give the planner the check's list of what part 2 will find hard (the end of
`PLAN_VALIDATION-streaming-gate-and-record.md`, and the plan's "For the second part").

## What is left, for the session that takes over

1. Done by softland-11 on 8 October: part 2 is built, proven here and committed (`3420abcf`). The three
   proofs are `clojure -M:store -m softland.store.act-proof`, `clojure -M:store -m softland.store.pure-proof`
   and `clojure -M:store-module -m softland.store.module-proof`; they showed 150, 429 and 218 passes.
2. Done by "gate-implementation" on 8 October: the skill's last stage, `phase-full-spec-review.md`, in
   one fresh session against the contract, with the rulings on it (`b6646187`). Its paper is
   `FULL_SPEC_REVIEW.md`; "Rulings after the review" in it is the build session's. The three proofs
   showed 150, 433 and 222 passes.
3. The folders. Sid asked why the store's files are flat (8 October). Proposed to him, waiting for his
   "go": `door/` (declaration, seal, text, digest), `gate/` (decision, first_act, stamp, batch),
   `record/` (rows, tables), `module/` (module, streaming_gate, microbatch_gate, door_reads), with
   `act.clj` at the top; the proofs follow the same folders. One mechanical commit, proofs passing before
   and after. "gate-implementation" put two changes of it to him, and waits for his word on them too:
   `module.clj` stays at the top beside `act.clj`, so that the module keeps its namespace and its name
   in the cluster, and `module/` holds the two gates and the reads; and the source moves first with the
   content of the proofs unchanged, so that the count lines are the same before and after. A split of the
   pure proof by folder would be a second commit.
4. Done: the receipt to Sid, given in chat on 8 October: the commits, the proofs' count lines, the list
   below, and every place where the contract could not be met as written. It asks him three things: the
   folders, and the two questions of item 23 below.
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
   flip at a rebuild. Which of two refusal words an act on the wrong depot gets rests on the timing of
   the other gate, so a rebuild can give the other word. After a failure Rama can decide an act of a
   layer after a later act of that layer; the setting `topology.stream.max.executing.per.task` at 1
   closes that window at a cost in throughput and stays at its default: his to choose. The review could
   not check that setting. Rule 9 (the winner between two microbatch acts on different entities) is
   answered in item 15.
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
11. The printed sheet of 7 October (job 35) left the print queue. As `lpstat -o` showed the queue on
    8 October, nine older jobs are in it, none of this build: two of 30 April, six of 1 October, one of
    6 October. Untouched.
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
    value. The read of locks gives no bytes for a lock of a microbatch layer that seals a value, and no
    read of this build gives them: they are beside the value, for the reads that are not built.
17. The night's run stopped at 05:36 on 8 October because the machine powered off on a shutdown that was
    scheduled at 01:36 for four hours later. Nothing was lost.
18. From part 2's build. In the proof an answer of a microbatch act was readable 873 ms after its append,
    with nothing sent after it. Nine behaviours of Rama that the plan leaned on were checked on a probe
    module with four tasks and none was false. No proof reaches: a second attempt of a real batch, a
    commit that fails after one task has shown its part, a rebuild, or a measure of speed or size
    against F9's targets. No linter ran. In the review's runs the same answer was readable after 885 and
    893 ms.
19. From the review of the whole (`FULL_SPEC_REVIEW.md`, `b6646187`). No fault was found in the module's
    code. Twelve repairs, all in explanations and proofs. One check of the module's proof could not fail:
    it tested at the end of the run that each answer read was of a closed batch, when every batch is
    closed. It now looks right after each read, and it fails against a copy of the read that ignores
    closing. 21,000 random batches through the pure proof's own run of the microbatch gate gave the
    decisions of deciding one at a time.
20. Places where the contract is not met as written, beside those above. Each is in the module's README.
    - Item 4, "decisions of one batch are visible together": nothing of a batch is readable before every
      task holds it, but then two tasks open some milliseconds apart.
    - Item 3 (d), "each permission above it in its chain stands": not read. It holds because a permission
      is written only beneath one that stood, and nothing ends a permission. The day that one can end,
      the gate must read the chain: up to 3 reads more for each act.
    - Item 3 (c), "the layer's class is this gate's": no read for a layer that is made here.
    - Item 8, "act-id → the recorded answer": the read takes two arguments, the slot of the act-id that
      routes it and the act-id.
    - Item 6, "strictly increasing on its task": the microbatch gate has one sequence of stamps for the
      whole gate, not one for a task.
    - Rule 15, "a refusal writes the answer and nothing else": also the gate's own counts. The task's
      last stamp (streaming); the count of turns and the gate's last stamp (microbatch).
    - Rule 20, "a gate refuses a thing over one": a thing whose act-id names a layer of more than 256
      characters gets no answer. It is kept as a stray, because the answer's key would hold that layer.
    - Rule 13, "the word that says it is not built": `:store-key-takes-one-write` does not say it.
      Ruled: the word stands, because "not built" is untrue of two of the three cases that earn it. The
      word is his to change, with the others.
21. An act with no permission in a made microbatch layer costs up to 256 reads of places and 256 ranges
    of locks, where the streaming gate reads 4 rows at most.
22. One act can hold a task of the streaming gate for all its reads, because that gate's event does not
    give the task back: up to 256 places, 256 sealed values of 65,536 bytes each, and 32,768 lock rows
    for an act that names the two ends of 128 requests. It needs standing in the layer. Ruled: no change
    of the code; reading each lock by its key halves that worst case and no more, and nothing is
    measured. It goes with F9's measure.
23. Two questions for Sid from the review.
    - May the system hold the top of a layer that is not the base? As built it can. "The system for the
      base" (E3·H7 B) is a session's reading that he let stand; his line of 8 October speaks of "what is
      open by the system". The build session's pick: yes.
    - Do the three store keys stay bare words? As built `:class`, `:permission` and `:lock-request` are
      taken from every tool in every layer: a tool cannot have a key of its own with one of the three
      words. The build session's pick: a namespace on the three, at the rename that follows his word for
      "class", before the first kept record.
24. Found by the session that judged the review. Read in the code, not run.
    - The read of locks shows that a lock is used one run before the batch that uses it is closed. The
      bytes that a read gave never change.
    - For the reads, which are next: a place holds one row. After a batch replaces a fact of a microbatch
      layer, its place holds no row of a closed batch until that batch is closed, about one run. A place
      that is replaced in every batch never holds one. So a read of facts cannot be held to closing as
      the three reads are, without more: the row before, kept beside the new one; or attempts of a batch
      made the same, by keeping each batch's clock (rule 9).
25. What the 10 seconds of item 9 let through. A decision's stamp is later than each stamp that its act
    carries, a refusal's too. So any sender whose thing passes the declaration can hold a gate's stamps
    up to 10 seconds ahead of the clock. No order is broken. Read in the code, not run.
