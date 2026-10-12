# Review: the rebuild

Reviewer: a fresh session, 13 October 2026 (Opus 5.5). Files: `src-store/softland/store/rebuild.clj`,
`rebuild/reader.clj`, `rebuild/placing.clj`; `test-store/softland/store/rebuild_pure_proof.clj`,
`rebuild_proof.clj`. After the last change: the pure proof `{:test 6, :pass 37, :fail 0, :error 0}`
(`runs/try-review-rebuild-pure.txt`), the comparison `{:test 4, :pass 29, :fail 0, :error 0}`
(`runs/try-review-rebuild.txt`). Before: 5/24 and 4/22. No fault was found in the code that makes
the rows: every change of the source is a docstring; the proofs gained checks.

## Faults

| # | Where | The words that it breaks | What was changed | How it holds now |
|---|---|---|---|---|
| 1 | `rebuild_proof.clj`: the namespace docstring ("the throw that the proof makes is before the commit"), the docstring of `failing-answer-record` ("behind the event's writes and before its commit"), the SHOWN line of `after-a-failed-attempt-of-each-gate` | `src-store/CLAUDE.md`: "Does the code do what its explanation says?"; B5: "what the log of answers holds after a failed attempt of each gate, and how the rebuild comes to the answer that the tables hold". The streaming gate calls `answer-record` in step 9, behind the commit (`module/streaming_gate.clj:417-427`); the module's proof uses the same function for its failure "behind its commit" | The three texts say where the throw stands. The stand-in keeps the stamp of the row that it was handed; two checks: that stamp is the stamp of the act's answer in the table, and the log holds an entry of the act | Run: stamp at the throw 117429816658427904, stamp in the table 117429816658427904, 1 entry. A throw before the commit would give the second run a new clock and another stamp |
| 2 | `rebuild.clj`, namespace docstring, "A store that is not at rest": "A thing with no entry gives no row, as the tables hold none for it" | Ruling 13: "The rebuild's page says what it gives of a store that is not at rest, as what falls out". The tables hold rows with no entry between a commit and the append behind it | The paragraph says that the tables can hold rows of such a thing, and when for each kind of decision | Read against `streaming_gate.clj` steps 8 and 9 and `microbatch_gate.clj` step 2 (the closing by the next run) |
| 3 | `rebuild.clj`, namespace docstring: "it runs no check of a gate" | R1: "A rebuild decides nothing again"; ruling 14: "Giving a thing its form is not deciding; refusing it is." `writes-of` calls `declaration/check`, which both gates judge with | The docstring names the one function, what it is called for, and that its refusal changes no answer | The calls into the gates' code, by search: `declaration/check`, `decision/marked-keys-place`, `decision/since-by-place`, `decision/writes`, `batch/routed`, `rows/answer-key`, `rows/answer-row`, `digest/hex-of`. No test for a stray, no `standing`, no `places`, no clock. Pure proof, `the-answer-of-the-log-is-applied-and-not-the-gate-of-today` |
| 4 | `rebuild.clj`, `routes-asked`: "asks for all of them in few calls"; no docstring said what the placing costs a large store | `src-store/CLAUDE.md`: "what must hold before the call, and its limits". `tasks-of-routes` makes one call of the query for each route | `routes-asked` says "before the rebuild"; the namespace docstring of `rebuild.clj` ("What a large store needs instead") and a new "Limits" of `placing.clj` say that the calls grow with the store and that a query of many routes is not built | Read against `placing.clj`, `tasks-of-routes`; the run asked 42 routes in 42 calls |
| 5 | `rebuild_proof.clj`, two SHOWN lines: "made again from the logs alone"; the parts left out printed with no reason | "When this build passes": "with each exception named and its reason given". The task of a row of a batch is asked of Rama; no log holds it | The line names the placing as asked of Rama, with the count of routes; the other prints each part with its reason from `rebuild/left-out` | The recap of `runs/try-review-rebuild.txt` |
| 6 | `rebuild_pure_proof.clj`, `an-act-of-a-gate-that-decides-in-batches`: `(is (= rebuild/left-out (:left-out result)))` | A check that can only pass: `from-logs` gives that same var. (Sent by the build session from the reviewer of the pages; true) | Held against the three parts written out, each with a reason, and against the rows: no row of `:task` that was made holds one of them | The pure proof passes; the list written out fails when `left-out` changes |
| 7 | `rebuild_pure_proof.clj`, `copies-and-the-order-of-a-log-change-nothing`: "every partition of every log turned back to front" | The log of the clocks of batches was not turned. (Sent as above; true) | It is turned too | The pure proof passes |
| 8 | `rebuild_pure_proof.clj`: the things of the two logs, 27 defs with no docstring | `src-store/CLAUDE.md`: "A small function needs a small docstring." (Sent as above; true: 25 and the 2 of this review) | Each has one line that says what the thing is in the logs | Read |
| 9 | `rebuild_proof.clj`, namespace docstring: "on each of two clusters ... through the door and through no door" | The second cluster sends by the hand alone. (Sent as above; true) | The docstring says which cluster sends how | Read against `after-a-failed-attempt-of-each-gate` |
| 10 | `rebuild_proof.clj`, `after-a-failed-attempt-of-each-gate`: "a second run leaves copies, and no entry of an attempt that did not count", shown and not checked | B5: "An entry of an attempt that did not count does no harm." (Sent as above; true) | `entries-with-no-row`, which does not go through the rebuild: each entry of each log of answers is the row that the table holds under its act-id, on both clusters; and the streaming log holds more entries than the table holds answers | Run: none apart; streaming 9 entries for 6 answers, microbatch 6 for 6 |
| 11 | `rebuild_proof.clj`, `the-store-is-made-again-from-its-logs`: `:answered?` of the rest | A check that can only pass on the first cluster: no act-id was read for there. (Sent as above; true) | `by-door!` keeps the act-id of each act that the door appended, and the rest waits for each answer | Run: "each of the 18 acts that went through the door has its answer" |
| 12 | The proofs left three things with no check: an act with no entry; three of the six words that `from-logs` names (`:records-of-a-batch-differ`, `:no-last-stamp-of-its-batch`, `:clock-not-in-the-log-of-clocks`); a record of the base that holds an answer and a stray | Ruling 31: "a wrong rebuild fails its own proof"; implied spec 5.2, edge 1 | Pure proof: `a-not-yet` and `b-not-yet`, each on a place that an act with an entry wrote, and the check `what-the-rebuild-names-of-a-batch`. Comparison: a stray and an act in one batch, and a check that one record holds both with the batch's clock and last stamp | Both proofs pass |

## The edges of point 4

| The edge | Where it is held |
|---|---|
| A repeat with copies of its entry | In the proof: pure `an-act-of-a-gate-that-decides-alone` (three copies of the entry of `a-first`) and `copies-and-the-order-of-a-log-change-nothing`; comparison, both clusters (20 entries for 18 answers; 9 for 6) |
| A second and different thing under a used act-id | In the proof: pure (`a-other`); comparison, both gates, in two batches and in one |
| A stray of each gate | In the proof: pure (`a-stray`, `b-stray`); comparison (no act-id; an act-id of the other gate, twice) |
| A stray of the base that took a turn | In the proof `the-store-is-made-again-from-its-logs`: a stray alone in its batch, and a stray in one batch with an act. That a stray moves the stamps behind it: traced by hand, the rebuild makes no turn and takes each stamp from its entry |
| A retract | In the proof: pure (`a-retract`); comparison, under a plain key and under a marked key |
| A fact under a marked key, replaced | In the proof: pure (`a-replaced`, `b-later`); comparison, both gates |
| A mark taken off and put on again; the rows of the earlier marking stay | In the proof: pure, the streaming layer (keys with 100 and with 170); comparison, both gates (the base's two markings are shown). The pure proof takes the base's mark off and does not put it on again |
| A mark and a fact under its key in one batch of the base | In the proof: pure (`b-in-batch` has no row); comparison (`:thing-1`, first value) |
| A fact in the batch that takes a mark off | In the proof: pure (`b-z`); comparison (`:thing-5`) |
| Two acts of one batch on one place | In the proof: pure (`b-first`, `b-second`; `b-p`, `b-q`); comparison, under a plain key and under the marked key |
| A fact of the base under one of the keys that are on every task | In the proof: pure (the marks row on both tasks); comparison (a permission on 4 tasks) |
| The first act of a layer | In the proof: comparison, both gates. Not in the pure proof: its layers are never made, and every accept there is one that the gate of today would refuse |
| An act into a layer of the other gate | In the proof: comparison (the streaming gate before and after the base is made, two words; the microbatch gate). Traced by hand: the word is the entry's, and no row of one gate is made from a row of the other |
| An entry with no act | In the proof: pure `what-the-rebuild-names` |
| Two entries under one act-id that differ | In the proof: pure `what-the-rebuild-names` |
| An act in a log of acts with no answer yet | In the proof: pure `what-the-rebuild-names` (added by this review) |
| The gate's last stamp made again, not below any stamp of a marked key | In the proof as an equality: pure (220 and 170; 3003 and 1007), comparison (the row of each task). Traced by hand that it cannot be below: it is the greatest stamp of a partition's entries, or the last stamp of the newest batch |
| The kept clock of a batch, made from the gate's own log | In the proof: pure (batch 9, with two records of batch 8 in the log of clocks; and a clock that no record of that log holds); comparison (the row, on the gate's task) |

Points 3 and 5, read: the rebuild and its reader read no table (no select, no PState handle in the
three source files). `from-logs` takes `:acts` as a list of `{:gate :log}`, and the reader's `depots`
is data. `partition-read` asks the two offsets once and reads from the start to the end in ranges of
`:at-once` records. `Placing` partitions with `|hash` on the route itself, as the gate does for each
home; the comparison holds a UUID (an answer), a keyword (an entity, the gate's word) and a hex text
(a value digest). It declares no depot and no PState. No way with no module was found: Rama 1.6.0's
client classes (`Helpers`, `Depot`, `PartitionedObject`, by `javap`) give a key for a task and never
the task of a value. One part is in the logs already: the partition of a thing on
`*microbatch-depot` is the task of its first entity. It gives no task for a uuid or a value digest.
Not built.

## The breaks of point 1

Each was one change of `rebuild.clj` for one run of the first cluster, then put back by hand;
`git diff` showed the file as committed before the fixes above. Each run ended with
`{:test 2, :pass 15, :fail 1, :error 0}` and exit 1. Outputs: `runs/try-review-rebuild-break-1.txt`
to `-6.txt`.

| # | The break | What the comparison printed |
|---|---|---|
| 1 | The stamp of an answer off by one, for one word of refusal | 2 lines: `DIFFERENT :microbatch :answers task 0 key ":base 6ce9760e-..." \| made: {:stamp ...652 ... :refused :permission-does-not-allow-acting} \| held: {:stamp ...651 ...}`, and the same for `:streaming :answers task 0` |
| 2 | One fact row dropped | 1 line: `DIFFERENT :streaming :facts task 0 key ":alice :note-3 :text" \| made: nil \| held: {... :value "\"while the mark is off\""}` |
| 3 | A retract kept as the value before | 3 lines: `:microbatch :facts task 1` for `":base :thing-2 :name"` and `":base :thing-3 :kind"`, `:streaming :facts task 0` for `":alice :note-2 :text"`; made holds `:value`, held holds none |
| 4 | A stray's word changed | 2 lines: `DIFFERENT :microbatch :task task 3 key :strays \| made: {:act-id-of-another-gate #{"18ae..."}} \| held: {:act-id-of-the-other-gate #{"18ae..."}}`, and `:streaming :task task 0` |
| 5 | A row of the facts by value not taken out (a row that only the rebuild made) | 5 lines in `:facts-by-value`, 3 of the base and 2 of the streaming layer, each `made: {...} \| held: nil` |
| 6 | The kept clock off by one, and `:closed-through` of one task off by one | 2 lines: `DIFFERENT :microbatch :task task 2 key :closed-through \| made: ...682 \| held: ...683`, `DIFFERENT :the-kept-clock :batch-clock task 0 key :microbatch \| made: {:batch 48, :clock ...256} \| held: {:batch 48, :clock ...255}` |

What the comparison reads: the answers, the facts and the facts by value of each gate, whole, on
each of the four tasks; of what a gate keeps for a task `:last-stamp`, `:closed-through` and each set
of `:strays`; the row of `$$batch-clock` on each task. It unites the keys of both sides, so a row of
one side alone is printed (breaks 2 and 5). Left out: `:turns`, `:to-keep`, `:to-close`, each named
with its reason in `rebuild/left-out` and printed by the proof.

A fault of a gate's log: none found. Every row of both clusters was made from the logs.

## For Sid

- `Placing` is a module beside the store. One query more in the store's own module would do the same
  with no second module. `placing.clj` says it under "Not decided"; it is the builder's choice.
- A provisional choice for the receipt, by R11 ("the default keeps it, and the choice is listed as
  provisional"): for a batch whose records hold no last stamp, the rebuild makes the answer's row and
  no fact row for every accepted act of the batch, also for an act that marks no new key and needs no
  last stamp. The gates cannot make such a log. Not changed.

## For another reviewer's file

- `test-store/softland/store/README.md`, "The rebuild's proof": line 1427, "behind its writes and
  before its commit", and lines 1476 to 1478, "no function of the store runs there, so the throw that
  this proof makes is before the commit". Both are untrue: the throw is behind the commit and before
  the append (fault 1). What the proof does not reach is a failure before the commit.
- The same page: the groups of both rebuild proofs gained checks (faults 1, 6, 10, 11, 12), and the
  second cluster sends by the hand alone (fault 9).
- `src-store/softland/store/rebuild/README.md`, line 6, "from the logs alone": the task of a row of a
  batch is asked of Rama (fault 5). Its row for `left-out` and for the six words stays true.
