# Review: the gates and the logs of answers

Reviewer: a fresh session, Opus 5.5, the night of 13 October 2026. One pass. Files of this area only.
After the last change: `clojure -M:store -m softland.store.pure-proof` gives
`{:test 22, :pass 715, :fail 0, :error 0}` (712 before; three checks added, row 12), and the module's
proof under the build's lock gives `{:test 38, :pass 398, :fail 0, :error 0}`
(`runs/try-review-module.txt`).

## Faults, each fixed

Rows 1 to 4 are this pass's own. Rows 5 to 12 came from the reviewer of the pages through the build
session; each was checked on disk first. No fault was found in the dataflow of either gate, and none in
what a log holds.

| # | Where | The words that it breaks | What was changed | How it is known to hold |
|---|---|---|---|---|
| 1 | `test-store/softland/store/module_proof.clj`, `a-closing-runs-a-second-time`, the last check | Its own name: "when the second run of the closing has committed, nothing is left to keep". The hand read `:to-keep` and `:to-close` as soon as the log showed the second run's records. An append from inside an attempt is visible before that attempt commits, and each task commits its part alone, so the read could come first and the check fail for no fault. Reasoned from the dataflow; not seen to fail | The hand reads the two fields until both are as a committed closing leaves them, for the hand's time limit at most, and the check holds what it read last. No check is weaker: a key that stays, or a `:to-close` that stays, fails it after the limit | Passed in three runs of the module's proof after the change |
| 2 | `src-store/softland/store/module.clj` and `module/batch_clock.clj`, the namespace docstrings | `src-store/CLAUDE.md`: "Does the explanation still explain the code?" Both said of the depot of batch clocks "nothing reads its old records". The contract's B3: a rebuild "reads the logs of acts, the log of answers and the log of the clocks of batches" | Both say that no topology reads the old records and that a rebuild is given the log whole, as a check of the clock | Read: `rebuild/reader.clj` reads `*batch-clock-depot`; `rebuild.clj`, `kept-clock`, walks it |
| 3 | `src-store/softland/store/module/microbatch_gate.clj`, the namespace docstring, "The keeping" | `src-store/CLAUDE.md`: "Replace old wording; do not add a history of corrections." The line said "as it did before the keeping" | It names the two fields that such a run reads | A search of the eleven source files for words that tell history finds none |
| 4 | `src-store/softland/store/gate/batch.clj` and `record/rows.clj`, "Callers" in the namespace docstrings | `src-store/CLAUDE.md`: "Trace the real callers". Neither named the rebuild, which calls `batch/routed`, `rows/answer-key` and `rows/answer-row` | Both name `softland.store.rebuild` and what it calls | A search of the rebuild's source for its calls into the two namespaces |
| 5 | `src-store/softland/store/module.clj`, the namespace docstring | It said "whether a log of the store is ever trimmed is Sid's". He decided it: entry 3 of 12 October, "The log is kept whole" | It says that the log is never trimmed and points to that entry, with no new words of his | Read against `src-store/README.md`, entry 3 |
| 6 | `module_proof.clj`, the namespace docstring | It said "no reader is built in this part", "no rebuild is built in this part", and "the one throw that the proof makes" | The hand plays a reader and a rebuild's reader and uses neither the read path nor the rebuild, which have proofs of their own; the throws are four | The run's output holds Rama's line "Will be treated as a fatal" four times, one for each stand-in that throws |
| 7 | `test-store/softland/store/pure_proof.clj`, the namespace docstring, `streaming-step`, and one SHOWN line | "steps 6b and 7 of the streaming gate": the gate's walk has no step 6b | "steps 6 and 7", three times | The gate's docstring: step 6 is the marked keys, step 7 the stamp and the writes |
| 8 | `pure_proof.clj`, `tools-own-keys`, the docstring | `src-store/CLAUDE.md`, no history: "the bare words that the store's keys were before they took the namespace" | Said in the present: which bare words are store keys in the store's namespace alone, and which in none. The key `:lock-request` stays in the list: the plan says so (`PLAN-the-locks-go.md`, B, the row of `the-declaration`: "`:lock-request` as a bare word stays a tool's own key") | The cases pass unchanged |
| 9 | `pure_proof.clj`, two cases of the declaration | "the three new keys": history | "the three keys of a second write" | — |
| 10 | `pure_proof.clj`, `wait-cases` and `the-doors-wait` | The contract, "When this build passes": "The checks of the locks and of the seal go with what they checked." The cases used `:request-wait-ms`, `:fewest-at-once` and `:most-at-once-streaming`, names of the stock, as "any other name" | `:no-number-of-the-door`, `:another-name`, `:read-every` | The cases pass: no other name is read |
| 11 | Names of checks that said more than their forms held. `module_proof.clj`: `a-permission-deeper-than-four` (first check), `what-a-permission-allows` (the stamps of the refusals), `an-act-on-several-tasks` (rule 18's stamp), `what-a-permission-allows-in-the-base` (three checks), `what-points-at-a-thing-in-the-base` (three checks) and its twin in `what-points-at-a-thing`. `pure_proof.clj`: `a-batch-is-decided-in-one-order` (things 25 and 26), `a-batch-under-marked-keys` (two checks), `a-batch-hands-what-it-decided-to-be-kept` (the strays) | The proofs' own rule, in the module proof's docstring: a check "says what must be read back, and reads it back" | The form now holds the name in six: the first permission is read on its holder; the six stamps of the refusals rise in the order sent, above the stamp before; "each row written before" also holds that there are such rows, where `every?` of none passed; her row on each of the four tasks is held whole, not its stamp alone; the word of thing 26 is held against the order of the two turns, by the proof's own count; the one task of a value's run is held to be the task of its digest. The name now says what the form holds in the others: no name tells where inside the gate an act was decided, or says "every run" of the runs that are read | Both proofs pass with the stronger forms |
| 12 | `pure_proof.clj`, `no-code-of-the-store-names-a-lock` | The contract: "The checks of the locks and of the seal go with what they checked." No check held it: the reading reached `src-store` alone | The same reading now also reads the code of each `.clj` file under `test-store`, and holds each name that names a lock against a written set of the names that are meant: a seal's part or a request for locks sent to be refused or to pass as a tool's own key, the entity of such a fact, the names of the checks that the store keeps no lock, and in this proof the examples of its own cases. Three checks more: none is not meant; the reading read the files; the test of what is meant can fail | Run: 10 files, 60,441 names, 44 that name a lock, each meant (2 in the act's proof, 4 in the door's, 8 in the module's, 30 in this one). It reaches no string, so not the text of a check |

## Traced, and found right

One check was tested by breaking the source for one run, under the lock: with the commit boundary of the
streaming gate taken out, `an-event-fails-behind-its-commit` failed in two checks (the count of
decisions, and the stamp against the throw) and the other 396 passed (`runs/try-review-module-break-1.txt`).
The source was put back and compared byte for byte.

By point of failure, the streaming gate, as written:

- Between an act's first read and its last write stands no partitioner, no yield and no append: the two
  loops of reads and the loop of writes do not yield (ruling 27).
- The entry is made and appended only behind `(|direct (ops/current-task-id))`, of what the event
  carried over it; behind it the event reads nothing and writes no row (S1, S2).
- An event that finds its act-id answered appends a copy made of the row that it read (S3, S5, S7;
  ruling 32, point 2). A stray appends its entry at each arrival, with the task of its set (ruling 8).
- A throw before the boundary leaves no row and no entry; a throw behind it leaves the row, and the next
  run of the record appends the copy; a throw or a lost ack behind the append leaves equal copies.
  Every entry under one act-id is made of the one committed row.
- The append is `:append-ack` (ruling 28); the depot is `:disallow`, with no option.

By point of failure, the microbatch gate, as written:

- What is to keep is written in the attempt that writes the answers' rows, into `:to-keep` of the gate's
  own table, by the send that carries the rows of a pass (M1, M2). Each answer is in one value: from a
  parcel on its home task, or from a pass on the meeting task.
- `:to-close` is read and written on the gate's task; the closing's range read, its appends and its
  removals are on the keeping task of the batch that is closed; a value is put on the keeping task of
  its own batch; the three routes are one long, the batch's id.
- 1, 256, 257 and 1,000 answers: 1, 1, 2 and 4 values for one task, and 1, 1, 2 and 4 records; with
  answers on several tasks the closing puts every entry into records of 256 at most, each once. Read,
  and run in the pure proof and, for 400 answers on several tasks, on Rama.
- A batch of strays alone leaves `{:batch}` and its entries, and writes no `:closed-through`. A batch of
  repeats alone leaves its clock, and its closing appends one record with the clock and the last stamp.
- A closing that fails after some appends: its writes are set back, so the next attempt reads the same
  values and appends the same records (M3). `:closed-through` is written behind the appends, in the
  same attempt.
- A module update between a batch and its closing: both fields are durable, and the entries are held as
  data, so the closing appends what the batch decided.
- A run with nothing to keep: two reads on the gate's task, as the plan counts; one read more of a
  temporary table at the head and one on each task in step 4, both in the plan's table; no write, no
  append.
- Read and not run on Rama: a run that closes a batch and also decides things of its own. In each proof
  of this area a quiet run closes. The first record's last stamp is the one that the head read before
  it wrote its own.

By contract item and ruling:

- A2, A3: a gate keeps a plain value as the text that the declaration's check made; no table, query,
  row, rule or word of a lock or of the seal is in the eleven source files.
- A6, C2, rulings 3 and 17: both bounds are the last step of the declaration's check, which each gate
  runs before any row of the act's standing; the text of a value is made once and carried in the act.
- B1: an entry holds the act-id, the stamp, the digest and the word of a refusal, and no value.
- B2, R3, ruling 11: no `source>` names a depot of answers; the door's two reads read the tables.
- B5, R7, ruling 12: a second attempt leaves the tables as one would; the log gains equal copies.
- B6, R2, ruling 15: no trimming option and no migration.
- R5: nothing is appended before its row is committed.
- R6, ruling 18: no new code chooses by a depot word; each gate hands its own word.
- R8: the words of this area are in the store's list of words for Sid.
- Entries 1, 2, 3 and 7 of 12 October: met as above; an act of a maker who may not act is in the log of
  acts, refused, with an entry.

## For Sid

Nothing.

## For another reviewer's file

- `test-store/softland/store/README.md`, group 22 and the last point of "what it does not reach": both
  say that the search reads the source alone and "reads no proof". It now reads the code of the proofs
  too (row 12), and the pure proof has three checks more.
- `src-store/softland/store/README.md`, "A thing for a streaming layer": the page numbers the walk 1 to
  7, the gate's docstring 0 to 9, and the proofs and their page say "steps 6 and 7" by the docstring's
  numbers. Not untrue, since each list is its own; said so that one of them can be chosen.
