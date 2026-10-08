# Full-Spec Review

Verdict: fail

Summary: One fresh session (Opus 5.5, called at effort xhigh, 8 October 2026) read the whole module at f6029dea
(15 files of `src-store/softland/store/`, its README, the maps above it), the three proofs and their maps,
against `src-store/README.md`, the contract `BUILD-store-module.md`, `src-store/CLAUDE.md`, `IMPLICIT_SPEC.md`
and `DECOMPOSITION.json`. It hunted each item 1–9, each rule 1–21, each SHOWN line, each decision of 7 and
8 October, the six invariants, the seam between the two gates, and what holds when Rama runs work again.
It found no defect in the code of the module: no check missing or out of order, no path that throws on
what arrives, no write that is wrong on a second run. 21,000 random batches through the pure proof's own
run of the microbatch gate gave the decisions of deciding one at a time in the order of the turns, each
time. It found and repaired twelve plain defects, all in explanations and proofs: six places where a map or
a docstring said what the code does not do, one check of the module's proof that passed without showing
what it claimed, four gaps of coverage, and wording that told the build's history. Five things need a
ruling and are not changed. They are the only outstanding items. The three proofs pass, with no failure,
before and after. By the stage doc's letter an outstanding item is a fail, so the verdict is fail; nothing
is outstanding but the five that wait for a ruling.

## The proofs' count lines

Before any change (ran, at f6029dea; outputs kept under the scratch path as `before-*.txt`):

| Proof | Count line | Exit | SHOWN lines |
|---|---|---|---|
| `clojure -M:store -m softland.store.act-proof` | `{:test 8, :pass 150, :fail 0, :error 0}` | 0 | 82 |
| `clojure -M:store -m softland.store.pure-proof` | `{:test 13, :pass 429, :fail 0, :error 0}` | 0 | 72 |
| `clojure -M:store-module -m softland.store.module-proof` | `{:test 22, :pass 218, :fail 0, :error 0}` | 0 | 134 |

They are the build session's 150, 429 and 218.

At the end (ran, after the last change to a proof; `final-*.txt`):

| Proof | Count line | Exit | SHOWN lines |
|---|---|---|---|
| act-proof | `{:test 8, :pass 150, :fail 0, :error 0}` | 0 | 82 |
| pure-proof | `{:test 13, :pass 433, :fail 0, :error 0}` | 0 | 72 |
| module-proof | `{:test 22, :pass 222, :fail 0, :error 0}` | 0 | 138 |

The pure proof has four cases more (F9, F10). The module's proof has four checks and two SHOWN lines more
(F8, F11, F12); each SHOWN line prints twice, so 134 became 138. The ERROR lines in the module proof's output
are Rama's own log while the module is started again (`rpl.shaded.kafka.log.Log`, "Could not find offset
index file"; `ModuleAssignmentInfoNotFoundException from resolve-leader`); they are there before and after,
and are no failed check.

## Items found and fixed

"New" means not among the 18 items of `PROGRESS.md`, "For the receipt". Every one of these is new.

| # | Location | Why it violated the spec | Fix applied | Evidence it holds | How found |
|---|---|---|---|---|---|
| F1 | `src-store/softland/store/README.md`, "The reason words of the gate", row `:layer-belongs-to-the-other-gate` | It said "No act of this build can earn it." Untrue: an act sent to the wrong depot earns it at each gate. `src-store/CLAUDE.md`: "Does the explanation still explain the code?" | The row now says "the act was sent to the wrong depot". | `module_proof.clj`, `each-gate-tells-the-other-gates-layers`, SHOWN "rule 7 in both directions" holds the word from both gates, before and after. | ran |
| F2 | `src-store/softland/store/declaration.clj`, namespace docstring, "Callers" | It named "the gate (`softland.store.streaming-gate`)" only. `softland.store.microbatch-gate` calls `stray-reason` and `check` in step 1. CLAUDE.md: "Trace the real callers". | Names both gates. | `microbatch_gate.clj`, `add-to`, step 1 requires and calls both. | read |
| F3 | README, "Engineering choices", "One accepted act with one sealed value on one entity costs the microbatch gate 3 reads and 4 row writes ... Each batch adds a few reads that do not grow with its acts." | The gate also reads and writes `:turns` of `$$microbatch-task` once for each thing that arrives (`microbatch_gate.clj`, `add-to`, step 1), a stray and a repeat too. That grows with the acts and was in neither count; the streaming count beside it does count the task's value. Contract: each choice "with its reason"; CLAUDE.md keeps "a claim about speed" apart from what a source says. | One sentence added: each thing that arrives costs one read and one write of its task's count of turns. Nothing in code. | The two forms of step 1 are there for every `*thing`, before the test for a stray. | read |
| F4 | README, "How the parts relate", first paragraph | It said that `declaration.clj`, `digest.clj`, `decision.clj`, `first_act.clj`, `stamp.clj`, `seal.clj` and `record.clj` "take the class of the gate that calls them". No function of `digest.clj`, `stamp.clj` or `seal.clj` takes a class. | Says that both gates decide with them, and that a function takes the gate's class where what it gives depends on the gate. | The signatures: `digest [x]`, `next-stamp [now last carried]`, `opens? [lock place sealed]`. | read |
| F5 | README, "How each of the builder's rules is met", 19 | It listed "the two words for a layer of the other gate" under "For the microbatch record also". The streaming record has the same: an act for the base on the streaming depot is refused `:layer-not-made` before the base's making is closed and `:layer-belongs-to-the-other-gate` after (`streaming_gate.clj`, `other-gates-record-holds-layer?`). Rule 19: "What F2 A reaches ... goes to Sid in the receipt." | Moved into the list for either record. | `module_proof.clj`: `the-base-is-made` holds `:layer-not-made` for that act, `each-gate-tells-the-other-gates-layers` holds the other word for the same kind of act later. | ran |
| F6 | README, "Engineering choices", "Where each row of a microbatch layer lives" | It said "The contract gives the depot: it places an act by the entity of its first fact." Contract item 1: "*Engineering choice, the builder's, reason in the docstring:* the entity of the first fact"; "Explanations": the section "lists each choice the builder made (the partition entity, ...) with its reason". | Says that the entity of the first fact is a choice that the contract names as the builder's, with its reason. | The reason is the one in `module.clj`'s namespace docstring. | read |
| F7 | `test-store/softland/store/README.md` (group 13 of the pure proof; "The checks of the streaming gate come first, as they were"); `pure_proof.clj` (namespace docstring, `one-at-a-time`, one case of `routing-cases`); `module_proof.clj`, `test-ns-hook` | "the first part's checks in the first part's order", "what the first part's head gives", "as they were": they tell the build's two parts. Contract "Explanations": "no history of corrections"; CLAUDE.md "What lasts". A reader of the tree does not know the build's parts. | Each now names the code: "the checks of `softland.store.decision` in their order", "what `answer-read` gives"; "as they were" is gone. | The proofs pass with the same cases. | read |
| F8 | `module_proof.clj`, `the-stamps-and-the-depot-of-the-base`, check "each answer that a read gave is of a closed batch" | It selected `:closed-through` at the end of the run. By then every batch but the last is closed whatever the reads did, so it passed without showing what it claimed. Task: "a check that passes without showing what it claims". | `base-result` now selects `:closed-through` on the answer's task right after each read that gave an answer and keeps the pair (`kept`, `:base-reads`); the last check holds each stamp to that. `:closed-through` never goes back, so a stamp above it then is a row read before its batch was closed. The old assertion stays, with the claim that it does show. | Ran a scratch mutant of `door_reads.clj` whose read by act-id ignores closing (`mutant-module.txt`): the new check fails and lists the reads; the old one fails for one answer only, of the last batch. On the real module: 465 reads, none above. | ran |
| F9 | `pure_proof.clj`, `stray-rows` | The class "an act-id of the wrong shape" left out four slots. IMPLICIT_SPEC X2: "two slots, four slots, a class word that is not one of the two ...". | One row: an act-id of four slots, `:act-id-malformed`. | pure-proof passes with it. | ran |
| F10 | `pure_proof.clj`, `doors-rows` | A request's count left out "a count below zero" (IMPLICIT_SPEC X90) and a number that is no whole number; a lock index left out its lower edge (256 and 255 were there). | Three rows: `-1` and `2.5` give `:lock-request-not-a-count`; a lock index of `-1` gives `:lock-id-malformed`. | pure-proof passes with them: 433. | ran |
| F11 | `module_proof.clj`, `an-act-on-several-tasks` | No act through the microbatch gate was accepted on a `:replaces` whose place is on another task than the one where the act is decided; that row comes in a reply of step 2 (`batch.clj`, `place-replies`, `with-replies`). The path was shown refused only. Rule 6: "an act that touches several tasks is accepted whole or refused whole"; implied spec E2, "Holds a value × a value, :replaces F1". | Three acts and two checks: a fact is planted on one task; an act that arrives on another task replaces it and is accepted, and the place holds its fact and stamp; the same `replaces` again is stale and writes nothing on either task. | SHOWN "an act that arrived on task 2 replaces the current fact of a place on task 1 and is accepted ... the same replaces again: :replaced-fact-not-current". | ran |
| F12 | `module_proof.clj`, `the-path-of-a-lock-in-the-base` | "A refused request makes no lock" (IMPLICIT_SPEC X92) was shown for the streaming gate only. `locks-of-request` has a branch of its own for a refused request of a microbatch layer (`door_reads.clj`, `(nil? (get *request-row :refused))`). | One check: a request by a who that is not the holder is refused; the read of locks gives `{}`; no task holds a row of a lock of it. | SHOWN "a refused request for locks in the base makes no lock: :who-not-the-permissions-holder {}". | ran |

Files changed: `src-store/softland/store/README.md`, `src-store/softland/store/declaration.clj` (docstring
only), `test-store/softland/store/README.md`, `test-store/softland/store/module_proof.clj`,
`test-store/softland/store/pure_proof.clj`, and this paper. No word was made. No file was moved or renamed.

## Items outstanding

Each waits for a ruling; the section after this gives the choices. Nothing else is outstanding.

| # | Location | Why it violates the spec | Required fix | Why it is unresolved |
|---|---|---|---|---|
| R1 | `src-store/softland/store/README.md`, "Words a session made": "Functions added to `record.clj` for the microbatch gate ... `routing-id`, `answer-read-by-routing-id`, ..." and "In `act.clj`, the one new name is `as-shape`" | Contract "Explanations": "no function lists in READMEs; no history of corrections". CLAUDE.md: "A README holds no list of functions". | Take the six names and "added ... for the microbatch gate" out; they are in `record.clj`'s namespace docstring. | Rule 8 says the README lists "each word that the build makes"; the builders read function names as such words. Two clauses, one paragraph. |
| R2 | `src-store/softland/store/decision.clj`, `store-key-fault`, word `:store-key-takes-one-write` | Rule 13: "Each is refused with the word that says it is not built." The word says the rule and not that it is a limit of this build. | A word that says so, or a ruling that this word does. | A reason word is Sid's to change (rule 8), and the plan listed this one. |
| R3 | `src-store/softland/store/first_act.clj`, `making-act?` | Decision 3 of 8 October (E3·H7 B): "the top of a chain is a person, or the system for the base, and never a session". The gate accepts a making act of a layer that is not the base whose first holder is `:system`, and it can tell `:system`. | Refuse it, or rule that it stands. | Rule 14: "The gate checks nothing more about who holds the top of a layer". A ruled choice against a decision. |
| R4 | `src-store/softland/store/declaration.clj`, `store-keys` | Nothing is violated. IMPLICIT_SPEC X14, "for Sid to know": the three bare words `:class`, `:permission`, `:lock-request` are taken out of every tool's keys in every layer. No map of the tree says it and it is not among the 18. | Tell Sid; a namespace on the three keys if he wants the words back. | A store key is his (K1: "idk what else is missing or could be missing, so lets go"). |
| R5 | `src-store/softland/store/decision.clj`, `lock-ranges`; `streaming_gate.clj`, `add-to`, step 6 | SKILL.md: "Long-running synchronous work on a task blocks everything else ... Risk factors: large `local-select>` calls". One range for each request, from the least to the greatest index named, in an event that does not yield: an act of 256 sealed facts that names indexes 0 and 255 of 128 requests reads 32,768 lock rows. | Read a lock by its key where the range holds more locks than the act names; or a limit. | The remedy is a choice, and one way of it is a limit of the build. |

## Needs a ruling

**R1. Function names in the README's list of words.**
Clause: contract, "Explanations": "no function lists in READMEs; no history of corrections". Against it,
rule 8: "Each word that the build makes — a reason that a gate records, a key, an id — says in plain words
what the thing is ... The module's README lists them".
Choices: (a) take out the paragraph "Functions added to `record.clj` for the microbatch gate" and reword
"the one new name is `as-shape`"; (b) keep both as words that a session made.
My pick: (a). Rule 8 names reasons, keys and ids, not functions; the other clause names function lists
outright; the six are already in `record.clj`'s docstring, which is their home by CLAUDE.md.

**R2. The word `:store-key-takes-one-write`.**
Clause: rule 13, "Each is refused with the word that says it is not built."
It is recorded for a permission given again, a second permission fact on a holder's place, and a `:class`
fact with the layer's own class after the making. The three words beside it say "not decided" or "not
built"; this one reads as a law of the store.
Choices: (a) keep; the README says "in this build"; (b) a word that says it, such as
`:second-write-on-a-store-key-not-built`.
My pick: (a) for now, and put the word to Sid with the others. It is plain, the plan listed it, and no kept
record holds it yet.

**R3. The system at the top of a layer that is not the base.**
Clause: `src-store/README.md`, 8 October, decision 3: "the top of a chain is a person, or the system for the
base, and never a session (E3·H7 B)". Against it, rule 14: "The gate checks nothing more about who holds
the top of a layer: it cannot tell a person from a session or an agent".
`making-act?` takes `(making :tools :system :system)` (pure proof, `first-act-rows`, "a layer whose first
holder is the system ... true"). The gate cannot tell a person from a session, but it can tell `:system`.
Choices: (a) keep; (b) refuse a making act of a layer other than the base whose first holder is `:system`
(one line in `making-act?`, one row of the pure proof turned).
My pick: (a), and ask Sid. His line of 8 October has a signed-out user "roaming around on what is open by
the system or world group", which may be a layer of the system's that is not the base; and until sign-in is
built any sender can claim any holder, so (b) guards nothing yet. PROGRESS item 4 covers "for any holder"
but not this decision.

**R4. The three store keys are bare words.**
Clause: contract item 2, "The store keys are three (K1 A) ... `:class`, `:permission`, `:lock-request`";
IMPLICIT_SPEC X14.
A tool's fact `{:e some-entity :k :class ...}` is refused `:setting-not-on-the-layer`, and one under
`:permission` or `:lock-request` is read as the store's, whatever the tool meant.
Choices: (a) keep, and say so to Sid; (b) give the three a namespace, which changes a store key.
My pick: (a). The words are in his decision of 8 October and in the contract. A sentence in the README is
then enough; I did not add it, because whether the words stay is his.

**R5. The lock rows that one streaming event can read.**
Clause: SKILL.md, cooperative multitasking; `declaration.clj`, `max-facts`: "it bounds the rows that one
event writes" (nothing bounds the rows that it reads).
A door that uses a request's locks in order reads exactly the locks that it names. The worst case needs a
holder with standing and 128 requests of 256 locks. The microbatch gate reads the same ranges with a yield.
Choices: (a) keep; (b) in `lock-ranges`, give a range for each lock when a request's span holds more than,
say, twice the locks named; (c) a limit on the span.
My pick: (b), in a later change, with the door proper. It changes no shape and no limit, but which reads
the gate makes is a choice with a cost on the common path, so it is not mine to make here.

## Rulings after the review

By the build session "gate-implementation" (Opus 5.5, effort max), 8 October 2026, which took the build
over from "softland-11" and judged this paper. It read the paper and the whole diff. Before the paper came
it had read the module's map, both gates, `door_reads.clj`, `decision.clj`, `batch.clj`, `stamp.clj`,
`seal.clj`, `first_act.clj` and the four papers of the build's own checks. It checked five of the paper's
facts in the code and in the kept runs: the pure proof's row for R3; that no code of the module appends to
a depot; that both gates earn `:layer-belongs-to-the-other-gate` (F1); the one changed line of the mutant of
F8 and its run, in which the new check and the check of closing both fail; the final count lines. None was
false.

Each repair F1 to F12 stands as the reviewer made it.

**R1: taken out.** The six function names are gone from the README's list of words, and the sentence on
`as-shape` no longer says "new". The tree's rule names function lists outright ("A README holds no list of
functions"); rule 8 names reasons, keys and ids. The six are in `record.clj`'s namespace docstring.

**R2: the word stands.** `:store-key-takes-one-write` is true of each of the three cases that earn it. A
word that says "not built" would be untrue of two of them: a permission that is given again, and a class
fact with the layer's own class, are refused under any later answer; only the second permission of one
holder is a limit of this build. The contract's sentence in rule 13 is left as it was written. The
difference goes to Sid in the receipt, with the word, which is his to change.

**R3: stands, and is a question for Sid.** "The system for the base" is a session's reading of his answer,
which he let stand; his own line of 8 October speaks of "what is open by the system". Until sign-in is
built a refusal there guards nothing. The question, for the receipt: may the system hold the top of a layer
that is not the base? The build session's pick: yes, as built.

**R4: the keys stand; the README now says what follows; a question for Sid.** That the three bare words
are the store's in every layer is what the code does, so the map says it ("Words a session made", second
paragraph). Whether they get a namespace is his. The build session's pick: a namespace on the three, at the
rename that follows his word for "class" and before the first kept record. `:class` is a word that tools
will want, and a store key is in each act of the depot for good.

**R5: no change of the code; the README now says it.** Read by its key, each lock is one seek, so the same
act still costs 256 seeks: the remedy halves a worst case that needs standing and 128 accepted requests. It
is a choice of reads without a measure, and F9 puts the measure first. The limit is said under "What the
gate does not check": "How long one act can hold a task of the streaming gate".

**Found by the build session while it judged, and said in the tree.** Each is read in the code, and none is
run.

- The read of locks is held to the closing of the request's batch, and not of a later batch in which an act
  uses one of the locks: a lock is given without its bytes one run before that batch is closed
  (`door_reads.clj`, `locks-of-request`; `record.clj`, `locks-by-index-used-without-bytes`). The maps said
  that the three reads give a row only when its batch is closed. It is now said in `door_reads.clj`'s
  namespace docstring, in `closed?`'s docstring, and in the README ("That a lock seals a value, one run
  early").
- A place holds one row. After a batch replaces a fact, its place holds no row of a closed batch until that
  batch is closed, and a place that is replaced in every batch never holds one. No read of this build meets
  it. It bears on the reads, which are not built. Said in the README, "Closing".
- The check of F8 holds one way: a stamp above `:closed-through` right after a read shows a row that was
  read too early; a row whose batch closed between the read and the select would pass. Said in
  `test-store/softland/store/README.md`, "What the module's proof does not reach", with the point before.

One suspicion was examined and dropped: that `:closed-through` could step back for a reader while Rama runs
a batch again. Rama's doc says that the reset touches the internal view only; "Not reached by any proof",
below, quotes the same passage.

**The proofs after the rulings**, run by the build session on the tree that is committed: act-proof
`{:test 8, :pass 150, :fail 0, :error 0}`; pure-proof `{:test 13, :pass 433, :fail 0, :error 0}`;
module-proof `{:test 22, :pass 222, :fail 0, :error 0}`, in 59 seconds. Each exit 0. The sixteen lines that
the contract asks for are each on a SHOWN line of that run.

**The verdict.** The reviewer's verdict stands as it wrote it: fail by the stage doc's letter, for five
items that waited for a ruling. Each is now ruled, and none asks for a change of the module's code. After
the rulings nothing is outstanding in this build. R3, R4 and the word of R2 wait for Sid's word; they are in
the receipt.

## Where the module does not meet the contract as written

For the owner's receipt. "README" is `src-store/softland/store/README.md`.

| Clause | What the module does | State |
|---|---|---|
| Item 4: "Decisions of one batch are visible together" | Nothing of a batch is readable before every task has committed it. Then each task opens when it commits the next run, so two tasks open some milliseconds apart. The one pair of reads on two tasks: the answer of the base's making act, and the class of the base. | Known in the README ("One instant for two tasks"). PROGRESS 15 has the closing, not this. |
| Item 3 (d): "each permission above it in its chain stands" | Not read. A permission's row is written only by a right making act or by a give whose own permission stood and is exactly the one above; nothing ends one. I traced it and it holds. | Known in the README ("Engineering choices"). Not among the 18. |
| Item 3 (c): "the layer's class is this gate's" | No read for a layer that is made here. The other record is asked only for a layer that starts with the other class: the base at the streaming gate, any other layer at the microbatch gate. | Known in the README. Not among the 18. |
| Item 7: "The door reads a lock's bytes by a query" | For a lock of a microbatch layer that seals a value, `locks-of-request` gives the way and no bytes; no read of this build gives them. A streaming lock is given with its bytes in each state. | Known in the README ("Sid should know two things"). PROGRESS 16 has the one copy, not the read. |
| Item 8: "act-id → the recorded answer" | The query takes two arguments: the routing id, which is a slot of the act-id, and the act-id. | Known in the README. |
| Item 6: "strictly increasing on its task" | The microbatch gate has one sequence of stamps for the gate, not one for a task. | Known in the README (rule 18). |
| Rule 13: "the word that says it is not built" | `:store-key-takes-one-write`. | Brought back, R2. |
| Rule 14 against E3·H7 B | The system can hold the top of a layer that is not the base. | Brought back, R3. |
| Rule 15: "a refusal writes the answer and nothing else" | Also the task's last stamp (streaming); the count of turns and the gate's last stamp (microbatch). No fact, lock or permission. | README says the first. The second was not said; F3 now says the count of turns. |
| Rule 20: "a gate refuses a thing over one" | A thing whose act-id's layer slot is over 256 characters is a stray with the word `:id-too-long`: it gets no answer, because an answer's key would hold that layer. | Known in the README. Not among the 18. |
| F2 A, rules 9 and 19 | A rebuild does not give again: lock bytes, stamps, a refusal by rule 17, the order across a failed batch, the winner between two microbatch acts of two partitions, and which of two words an act on the wrong depot got. | Known: PROGRESS 5 and 15. The last is said for both records since F5. |
| IMPLICIT_SPEC X29: the acts of a layer in the depot's order | After a failure of a streaming batch a later act of a layer can be decided before an earlier one. `topology.stream.max.executing.per.task` at 1 is said to close it. | Known: PROGRESS 5; his to choose. I could not check the setting. |
| F5 A: "a gate reads on its own task" | The streaming gate does. A microbatch act reads its standing where it arrives, and sends a question to the task of its act-id's uuid for every act, and one for each row on another task. | Known in the README (6 messages for a common act). |
| "Explanations": "no function lists in READMEs" | One list of six function names. | Brought back, R1. |
| "Explanations": "lists each choice the builder made (the partition entity ...)" | Was given as the contract's. | Repaired, F6. |
| "The proof ... run as `clojure -M:store -m ...`" | Runs under `:store-module`, as "Classpath" allows; `src-store/README.md` says which and why. | Met. |
| "Commits ... grouped by concern" | Two commits for the module. | Known: PROGRESS 14. |

## Not reached by any proof

What only reading supports, and what a run would need.

- A stream event that Rama runs again after its writes were committed. Reading: each write of a decision is
  behind the read of the answer (`streaming_gate.clj`, step 1), and a stray's write is an add to a set. A
  run needs a fault between the commit and Rama's progress mark.
- A streaming batch that fails, and the order of a layer's acts after it. A run needs a throw inside a
  batch, which the module's code does not give.
- A second attempt of a batch of the microbatch gate; a commit that fails after one task showed its part.
  Reading: the turn is from `:turns`, which is set back with the attempt; the closing gives no row of the
  batch. Rama's doc (`reference/rama/docs/12-microbatch-topologies.md`, the priming phase) says that the
  reset "only affects the internal view" and that a new version "replaces the old version in the external
  view": so `:closed-through` does not step back for a reader during a second attempt. Inferred from the
  doc; a run needs a fault at a commit.
- That the run after a batch with things starts at once in production. Rama's doc: the runner sleeps only
  when "a microbatch does not process any records". In the proof the answer of an act alone in its batch was
  readable 885 to 893 ms after its append.
- A worker that stops in the middle of an event; a move of a task's leader; replication; more than one
  machine. The proof starts the module again by Rama's update and reads both records.
- A loop that gives the task back: 400 acts in one batch is the longest pass.
- Rule 4 of who goes to the meeting task, inside a whole batch on the cluster. It is held in memory: one
  case of the pure proof, and the scratch probe below.
- A thing that a topology appends to another partition; an append whose ack fails for an accepted act.
- A read whose first argument Rama's client cannot hash. The hand tests what it passes and never calls so.
- A map whose keys cannot be compared with a keyword (a sorted map with string keys), on a depot. Reading:
  `stray-reason`, `check`, `first-entity` and `digest` each catch what is thrown; the streaming depot's
  partitioner would throw on the appending client, not in a gate.
- Facts given as a lazy sequence, through Rama: whether the door's digest is then the gate's. A list is
  shown (`what-rama-carries`, `an-act-lands`).
- F9's three targets. No speed or size is measured.

## The 18 known items against the code

None is false. Two are overstated or stale, and three limits that the README holds are not among them.

- Item 5. "What F2 A ... reaches as built: the decisions and the current facts of a layer on one task."
  Overstated by one word: which of two refusal words an act for the base on the streaming depot gets
  depends on when the microbatch gate closed the base's making (F5). Its last sentence, "Rule 9 ... waits
  for part 2", is stale: item 15 and README rule 9 give the answer.
- Item 16 has the one copy of a lock's bytes, and not that the read then gives no bytes for a used lock.
- Not among the 18, in the README: two tasks do not open at one instant; the chain above a permission is
  not read; an act with no permission in a made microbatch layer costs up to 256 reads of places and 256
  ranges of locks.
- Items 12 and 18 (a query reads a private PState; nine behaviours of Rama checked on a probe module) and
  the 245 bytes of item 16 I could not check.
- Item 18's 873 ms: I saw 893 and 885 ms.

## The hunt

### The invariants, and where each is held

| Invariant | Held by | When Rama runs the work again | Shown |
|---|---|---|---|
| One act-id never gets two answers | A stray gets none (`declaration.clj`, `stray-reason`: the class slot is the gate's, the layer slot is the thing's layer). Streaming: the answer is read first and the event ends (`streaming_gate.clj`, step 1). Microbatch: the answer's home task tells the least turn `:yours` and each other `:taken-in-this-batch`, or all `:used` (`batch.clj`, `answer-replies`). | Streaming: a second run after a commit ends at step 1. Microbatch: an attempt starts from the PStates of the batch before, and the turns are the same. | `refusals-and-repeats`, `refusals-and-repeats-in-the-base` (one act-id under two things in one batch, on two tasks: one answer), `each-gate-tells-the-other-gates-layers` (one act-id on both depots). |
| A refusal writes the answer and nothing else | `decision.clj`, `writes`: for a refusal the answer and the last stamp. `batch.clj`, `routed` drops the last stamp. | The same rows at the same keys. | `refusals-and-repeats` (nothing at the place), `an-act-on-several-tasks` (no row on any task, three locks free). |
| A microbatch act is accepted whole or refused whole on every task | An act is decided once, on one task, with each row in hand (`batch.clj`, `decided`); its rows go out inside one attempt. | Rama keeps or drops an attempt on all tasks. A reader sees none of it before closing. | `an-act-on-several-tasks`; the pure proof's batches in 200 orders; my 21,000 runs. |
| A lock seals one accepted value | `declaration.clj`, `:lock-named-once`; `decision.clj`, `seal-fault` (`seals-a-fact?`); two uses in one batch meet (`batch.clj`, `lock-replies`), and the pass lays the first over (`lay-over`). | The lock's row is written whole at a fixed key. | `the-path-of-a-lock`, `the-path-of-a-lock-in-the-base`, `one-batch-one-order` (two acts under one lock). |
| A stamp is strictly increasing on its task | `stamp.clj`, `next-stamp`: the greatest of the clock, the last stamp plus one, the carried stamp plus one. The last stamp is a row, read at each decision or once for a batch. `batch.clj`, `base`, `stamp-of-turn`, `last-stamp-of-batch`. | A stamp of a run that was discarded was never visible; one of a batch that is not closed is not given. | `the-stamps-and-the-depot`, `the-stamps-and-the-depot-of-the-base`, both checks after the module is started again. |
| A lock's bytes never reach a depot | No code of the module appends to a depot (I searched each file). A request holds a count; the bytes are made at the write (`new-lock-row` of each gate). | Streaming: behind the answer. Microbatch: other bytes on a second attempt; none was given. | Both depots' logs read back whole against the hex of each lock that the door read. |

### What to build, 1–9

| Clause, verbatim | Module | Proof |
|---|---|---|
| 1. "The streaming depot partitions by the act's `:layer`. The microbatch depot partitions by one entity derived from the act's facts ... the entity of the first fact." | `module.clj`, `Store`: `(hash-by :layer)`, `(hash-by batch/first-entity)`; `batch.clj`, `first-entity` | `the-stamps-and-the-depot` (layers by partition); `an-act-lands-in-the-base` (eight entities, more than one task); pure `the-batch` (what the depot derives, never throwing) |
| 2. "what arrives on a depot is the maker's act plus `:act-id`, `:class`, and each value fact's `:v` replaced by `:sealed` bytes and a `:lock-id`. Facts under a **store key** keep a plain value. The store keys are three" | `declaration.clj`: `shapes`, `rules`, `store-keys` (each with `:why`), `check`; `act.clj`, `as-shape` | pure `the-declaration`, `refusals-of-the-declaration` (61 things, each for one fault), `the-maker's-check-inside-the-door's`; act-proof unchanged at 150 |
| 3. "(a) the record already holds an answer under this act-id → return it, decide nothing ... (b) the door's declaration ... (c) the layer's class is this gate's; (d) the act's permission stands ... (e) each `:replaces` is the current fact ... (f) no setting key on an entity other than the layer (F8); (g) a fact under `:permission` writes a permission beneath the one that the act itself acts under" | `streaming_gate.clj`, `add-to`, steps 1 to 7 in that order; `decision.clj`: `refused-early`, `standing`, `places`, `seals`, `writes` | `refusals-and-repeats` (a), `strays-and-limits` and pure (b), `each-gate-tells-the-other-gates-layers` (c), `settings-and-permissions` (d, f, g), `refusals-and-repeats` (e) |
| 3. "Not decided, so refused in this build with a word that says so: a retract of a permission ... and a change of a layer's class" | `decision.clj`, `store-key-fault`: `:ending-a-permission-not-decided`, `:changing-a-class-not-decided` | `settings-and-permissions`, `a-person-acts-in-the-base` |
| 4. "a microbatch topology with the same checks, over a batch, across tasks. Decisions of one batch are visible together (the finding). Same record shape." | `microbatch_gate.clj`, `add-to`, four batch blocks; `batch.clj`, `decided` calls `decision.clj`'s four checks; `record_pstates.clj` | the eleven checks of the base; pure `a-batch-is-decided-in-one-order` |
| 5. "the layer's current facts ...; answers by act-id ... with the digest of builder's rule 1; the layer's settings as facts on its own entity (the class); permissions as facts; the locks" | `record_pstates.clj`: `answers`, `facts`, `locks`, `task`, `microbatch-task`; `record.clj`: the rows | `an-act-lands`, `layers-are-made` (class and permission as rows), pure `the-records-rows` |
| 6. "an integer the gate assigns, strictly increasing on its task, later than each stamp that the act carries, below 2^62. It holds the clock time and a counter (H2 A)." | `stamp.clj`: `pack`, `next-stamp`; `decision.clj`, `stamp-of`; `batch.clj`, `base` | pure `the-stamp`; `strays-and-limits` ("5 seconds ahead ... with a stamp later than the one it carries"); the two checks of the stamps |
| 7. "every value gets its own small lock at write ... lock rows co-located with their values ... The module makes locks on request ... The door reads a lock's bytes by a query ... The gate refuses a plain value under a non-store key (`:not-sealed`), a sealed value whose lock the store did not make for this layer, and a second value under a lock that already seals one." | `seal.clj`; `decision.clj`, `seal-fault`, `accept-writes`; `door_reads.clj`, `locks-of-request`; `batch.clj`, `routed` (one copy beside the value) | `the-path-of-a-lock`, `the-path-of-a-lock-in-the-base`, `an-act-lands-in-the-base`, pure `the-seal` |
| 8. "a query topology: act-id → the recorded answer, or nothing yet" | `door_reads.clj`, `answer-by-act-id`; `record.clj`, `answer-read-by-routing-id`, `answer-of` | `an-act-lands` (never sent: nothing), `the-reads-take-anything`, `an-answer-is-readable-when-its-batch-is-closed` |
| 9. "A gate accepts an act into a layer for which the record holds nothing when it is a making act ... The base, `:base`: class microbatch (F4 C); the first permission is the system's ... Any other act into a layer that is not made is refused." | `first_act.clj`, `making-act?`, `class-at-making`; `decision.clj`, `standing`, `makes-the-layer?` | `layers-are-made`, `the-base-is-made`, pure `the-first-act` (19 rows) |

### The builder's rules, 1–21

| Rule, verbatim (the part that binds) | Module | Proof |
|---|---|---|
| 1. "A second, different act under a used act-id must not read back the first act's answer as its own." | `record.clj`, `answer-row` (`:digest`); `digest.clj` | `refusals-and-repeats`, `refusals-and-repeats-in-the-base`: `:mine?` false |
| 2. "A fact that names nothing to replace is never refused as stale" | `decision.clj`, `places-needed`, `replaced-fact-fault` | `refusals-and-repeats`; `one-batch-one-order` (two that write one place) |
| 3. "A retract is recorded as the current fact of its place" | `record.clj`, `fact-row` | `refusals-and-repeats` (a row with no value; a later fact replaces it) |
| 4. "The acts depots are never trimmed." | `module.clj`: no option | both depots hold each thing sent (104 and 473) |
| 5. "A lock's bytes are made inside the module and are never appended to a depot ... Neither a retry nor a replay makes a lock again" | `new-lock-row` of each gate; `decision.clj`, `writes` (`[:new-lock key nil]`) | a request sent twice gives the same bytes, in both gates; no lock's hex on either depot |
| 6. "two acts that replace the same fact get the same winner on every attempt, and an act that touches several tasks is accepted whole or refused whole. Do not send all acts through one task" | `batch.clj`: `turn`, `who-meets`, `decide-or-send`, `decide-next`; `microbatch_gate.clj`, step 1 (`:turns`) | `one-batch-one-order`, `an-act-on-several-tasks`; pure, 200 orders; an attempt run twice: not reached |
| 7. "Either gate can tell which gate a layer belongs to. A microbatch act's answer is found from its act-id alone" | `streaming_gate.clj`, step 4; `batch.clj`, `reads-other-record?`; `record.clj`, `routing-id` | `each-gate-tells-the-other-gates-layers` |
| 8. "Each word that the build makes ... The module's README lists them" | README, "Words a session made": 5 words of a stray, 20 of the declaration, 18 of the gate, each of which I matched to the code | pure: "each of the 27 reason words comes from a thing that earns it" |
| 9. "whether a rebuild from the acts depots picks the same winner" | README, rule 9: found, no | said, not run |
| 10. "is a stray: the gate keeps its digest under a reason word, on its task, once however often it comes, and gives it no answer" | `declaration.clj`, `stray-reason`; step 0 of each gate (`NONE-ELEM` into a set) | `strays-and-limits`, `each-gate-tells-the-other-gates-layers` |
| 11. "the read by act-id gives the digest with the answer ... The gate writes nothing for a second, different act." | `record.clj`, `answer-of`; `digest.clj`, `is-digest-of?` | `refusals-and-repeats` ("nothing of the second act is written") |
| 12. "The declaration refuses an act with two facts on one place, and a fact that names a fact of its own act" | `declaration.clj`, `:place-named-once`, `:no-fact-replaces-a-fact-of-its-act` | `refusals-and-repeats`; pure `doors-rows` |
| 13. "a store key takes one write for a place ... a request for locks holds a count" | `decision.clj`, `store-key-fault`; `declaration.clj`, `:lock-request-holds-a-count` | `settings-and-permissions`, `a-person-acts-in-the-base`; R2 |
| 14. "`granted-in` is the act's layer ... the gate refuses another value" | `first_act.clj`, `making-act?`; `decision.clj`, `permission-fact-fault` | `settings-and-permissions`; pure `first-act-rows` ("a first permission granted in another layer") |
| 15. "A lock that a refused act named is free" | `decision.clj`, `writes` | `the-path-of-a-lock`, `an-act-on-several-tasks` |
| 16. "A gate opens each sealed value once ... The seal binds a value to its place ... A lock's row says which way" | `decision.clj`, `seal-fault`; `seal.clj`, `cipher` (the place as associated data), `ways` | `the-path-of-a-lock` (another entity; any bytes); pure `the-seal` (entity, key, layer) |
| 17. "refuses an act that carries a stamp more than 10 seconds ahead of the gate's wall clock" | `stamp.clj`, `too-far-ahead?`; `decision.clj`, `refused-early`, `stamp-of` | `strays-and-limits`, `a-person-acts-in-the-base`; pure: 10,000 ms is the edge |
| 18. "A microbatch act has one stamp, later than the last stamp of each task on which it writes." | `batch.clj`, `stamp-of-turn`, `base`; one last stamp for the gate | `an-act-on-several-tasks` (SHOWN "the one stamp of builder's rule 18") |
| 19. "A rebuild of the record from the depots is not built" | README, rule 19 | none, by the rule |
| 20. "256 facts in one act, 1,024 entries of `:based-on`, 65,536 bytes in one sealed value, 256 locks in one request, and 256 characters" | `declaration.clj`: the five numbers, four rules, `stray-reason` | `strays-and-limits`; pure `limit-rows`, `id-rows` (12 places) |
| 21. "The plan says what it aims at for each." | README, rule 21 | none measured |

### The lines that the contract says must be SHOWN

All sixteen are on SHOWN lines of the module's proof, in the contract's words, before and after:
"the base made by the system, with the class microbatch" (`the-base-is-made`); "a person's layer made by the
system for that person, with the class streaming", "a group's layer made by the person who creates it", "an
act into a layer that is not made refused" (`layers-are-made`); "one act landing through the streaming gate
and read back by act-id" (`an-act-lands`); "one act landing through the microbatch gate and read back by
act-id" (`an-act-lands-in-the-base`); "a refusal recorded with its reason", "a repeat (same act-id sent
twice) answered from the record, not decided twice", "a second, different act under a used act-id told so",
"an unsealed value refused", "a stale replaces refused" (`refusals-and-repeats`, and each again for the
base); "a setting key on another layer refused", "an act with a permission for another layer refused", "an
act under a permission that does not stand refused", "a permission given beneath another by its holder ...
and an act under it accepted", "a permission given by one who does not hold the one above refused"
(`settings-and-permissions`). I read each check behind each line: each compares what was read back through
the module's own queries with the word that must be, and none passes on a weaker thing than its line says.

### The decisions of 7 and 8 October that the module stands on

| Decision | In the module | Shown |
|---|---|---|
| 7 Oct 1: "Two depots, one for each gate" | `module.clj` | each proof of a gate |
| 7 Oct 2: "The record is in the gates' PStates ... The acts depots are the replay log." | each gate declares its four PStates; no second depot | the limits are in the table above |
| 7 Oct 3: "The members of a layer set its class, as a fact on the layer." | the row at the layer's own entity under `:class`; no member sets it after the making in this build | `layers-are-made` |
| 7 Oct 4: "The base is a microbatch layer; every other layer starts streaming." | `first_act.clj`, `class-at-making` | `layers-are-made`, `the-base-is-made` |
| 7 Oct 5: "a gate reads on its own task" | see the table of what is not met | — |
| 7 Oct 6: "The door reads a streaming act's answer from the record by act-id; the ack is a signal." | no `ack-return>`; `answer-by-act-id` | `an-act-lands` |
| 7 Oct 7: "A gate reads its own record by act-id before it decides, so a repeat is answered, not decided twice." | step 1 of the streaming gate; `answer-replies` | the repeats, in both gates |
| 7 Oct 8: "The gate refuses a setting key on an entity that is not the act's layer, as a recorded decision." | `decision.clj`, `setting-fault` | `settings-and-permissions`, `a-person-acts-in-the-base` |
| 8 Oct 1: "The system stands at the top of the base's chain." | `first_act.clj`: for the base the holder is `system` | `the-base-is-made`; pure: "the base by a who that is not the system ... false" |
| 8 Oct 2: "the system makes the layer and the permission at the first sign-in, and the person who creates a group makes the group's layer" | `making-act?`, point 3 | `layers-are-made` |
| 8 Oct 3: "each actor acts under its own permission (E4 A); whoever holds a permission can give one beneath it (E2 A)" | `standing` (`:who-not-the-permissions-holder`); `permission-fact-fault` | `settings-and-permissions` (a chain four deep); R3 |
| 8 Oct 4: "A stamp holds the clock time and a counter." | `stamp.clj`, `pack` | pure `the-stamp` |
| 8 Oct 5: "The store's keys in this build are three ... No layer names an owner as a fact." | `declaration.clj`, `store-keys`; `:owner` is `:unknown-part` | pure `makers-rows`; R4 |
| Open, H1: "The module makes a lock for each value" | `seal.clj`; a lock's row | the paths of a lock |

### The whole lives that I traced

- A lock: asked for by a count (`:lock-request-holds-a-count`) → made at the write of an accepted request,
  at `<layer> <request uuid> <index>` → read by `locks-of-request` (microbatch: only when the request's
  answer is accepted and closed) → named by a fact → the row read, `seals-a-fact?` false, the way known,
  the bytes open the value at its place → written again whole with the fact that it seals (microbatch: the
  home's row without bytes, then the whole row beside the value). After a refusal the row is not written:
  free (rule 15). I found no state from which a second accepted value can come under it.
- An answer: absent → written once, with the stamp and the 32 bytes of the digest → read with the digest as
  hex. A repeat and a different thing both end at the read of the answer.
- A stray: its digest into the set of its word, on the task where the depot put it; the same thing goes to
  the same task (the partitioner is a function of the thing), so the set holds it once.
- A layer: nothing → class and first permission by a right making act, on the layer's task (streaming) or
  on every task (the base) → no act changes either. Two making acts in one batch: both go to the layer's
  task, and the earlier turn makes it.
- A permission: given beneath the giver's own, in the layer, on an empty place of its holder → stands for
  good. The chain is in the id, so the bytes at the holder's place are the whole chain.
- A microbatch act: appended → a turn → standing from this task's copy → questions → replies → decided
  where it arrived or on the layer's task → rows on their homes inside one attempt → closed by the next
  run → readable.
- A stamp: later than the last by one at least; later than each carried stamp that the 10-second rule lets
  pass; a refusal by that rule gets an ordinary stamp; below 2^62 until the year 4199.

### The seam

- An act for the base on the streaming depot, with a streaming act-id: `:layer-not-made` before the base's
  making is closed, `:layer-belongs-to-the-other-gate` after; never accepted, because `making-act?` is false
  for the base at the streaming gate. Ran.
- An act for a streaming layer on the microbatch depot, with a microbatch act-id: it goes to the layer's
  task, where the gate reads `$$streaming-facts`; that task is the streaming layer's own, since both
  partitioners hash the layer id. `:layer-belongs-to-the-other-gate`, or `:layer-not-made`. Ran.
- An act-id of the other class on a depot is a stray. So one act-id has an answer in one record at most.
- The three reads: by the class slot for an answer and a request's locks, by `class-at-making` for a class.
  They differ in one thing, the bytes of a used lock (the table above).

### The scratch probes

Under the scratch path, none left in the tree.

- `fuzz_batch.clj`, `fuzz_batch2.clj`: 7,000 random batches of 3 to 19 things (plain and sealed facts on 4
  to 8 entities of 2 to 4 tasks, `:replaces` of facts before the batch and of facts of the batch, shared
  locks, requests and uses of their locks, gives and acts under them, class facts, one act-id under two
  things, carried stamps, a base that is made and one that is not), each in 3 orders of arrival, through
  the pure proof's `run-batch` against its `one-at-a-time`. 21,000 runs: no difference, nothing thrown;
  38,106 accepted answers among them.
- `mut/softland/store/door_reads.clj`: the mutant of F8.
- `docs_check.clj`: each namespace and each function of both trees has a docstring. The eight atoms of the
  proofs have none; they are no functions, and each namespace docstring names them.
- Each relative link of the six READMEs resolves.

PHASE_VALIDATION:fail
