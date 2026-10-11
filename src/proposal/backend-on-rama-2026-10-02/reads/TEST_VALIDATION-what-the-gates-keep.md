# Test validation: what the gates keep

Part: `what-the-gates-keep` of the read build. Stage: the build, step "validate the tests" (the Rama
skill's `phase-6-test-validate.md`, by its template `artifact-test-validation.md`), and the last
step, "finish" (`phase-7-finish.md`). Written by the build stage's own session (Opus 5.5), 12
October 2026, for the build session, which runs the proofs itself and judges this paper.

The tests of this tree are its four proofs, under `test-store/softland/store/`. Ruling 1: the four
files stay; each check that a decision of this build makes untrue is written again for the new
truth, never only removed; the plan's new checks are added. "Entry N" is entry N of "Decisions of 11
and 12 October 2026" in `src-store/README.md`. "The implied spec" is `IMPLICIT_SPEC.md`: its VA1 to
VA6, its matrix 1 to 4, its A1 to A6.

**Verdict of this step: pass.** The four proofs pass with exit 0 on the last run (section 7).

Three kinds of statement are kept apart: *run* (a proof printed it), *traced* (read in the proof's
source), *not reached*.

## 1. The counts, before and after

| Proof | Before | After | Checks written again | Assertions more |
|---|---|---|---|---|
| act | `{:test 8, :pass 150, :fail 0, :error 0}` | `{:test 8, :pass 160, :fail 0, :error 0}` | 1 | 10 |
| pure | `{:test 16, :pass 540, :fail 0, :error 0}` | `{:test 17, :pass 671, :fail 0, :error 0}` | 29 | 131 |
| module | `{:test 22, :pass 227, :fail 0, :error 0}` | `{:test 31, :pass 333, :fail 0, :error 0}` | 13 | 106 |
| door | `{:test 26, :pass 193, :fail 0, :error 0}` | `{:test 27, :pass 199, :fail 0, :error 0}` | 0 | 6 |

"Before" is the build session's run on the tree before this build (`PROGRESS.md`). "After" is this
session's last run, each with exit 0. A check that was written again keeps its one assertion, so
"assertions more" is the count of what is new: 253 in all. No check was removed.

The plan's table named 27 rows of checks that the build makes untrue. Six further ones were found
by reading the groups that changed; they are marked **(further)** in section 2.

## 2. Each check that was written again

"Old" is the check's sense at the last commit; "new" is its sense now.

### The act's proof

| Check | Old | New | Made untrue by |
|---|---|---|---|
| "the declared shape of an act and of each thing that an act holds" | `:permission-id` written out a second time with two forms | with three forms; the third is `[:tuple :id :id :id :permission-id [:enum :see :see-and-act]]` | ruling 2; entry 1 |

### The pure proof

| Check | Old | New | Made untrue by |
|---|---|---|---|
| "the five shapes that the door adds" | five | "the eight shapes that the door adds", with `:who-may-see-word`, `:layer-list`, `:key-set` | entries 1, 2, 6 |
| "the twenty-one rules, the maker's first, each with its reason" | twenty-one | "the twenty-seven rules", the six new ones after the rules of a request for locks | entries 1, 2, 6; ruling 40 |
| "the words with which the check refuses, read from the two declarations" | 28 words | the same set and the six new words | the same |
| "the five limits" **(further)** | five numbers | "the seven limits", with 64 layers and 32 marked keys | the build's two numbers |
| "the three store keys, the shape of the value of each, and which is a setting" | three keys; one setting | "the six store keys, the shape of the value of each, which is a setting, and which is written again"; four settings; three that take a second write | entries 1, 2, 6; R7 |
| "the store's namespace, and each store key is in it" **(further)** | three namespaces listed | each of six keys is in the one namespace | entries 1, 2, 6 |
| "each of the three store keys is known" | a fact under each of three passes | "each of the six store keys is known: a fact under each, with a right plain value, passes" | the same |
| "each word of the declaration comes from a thing that earns it" | 28 words earned | the same check; a thing that earns each of the six new words is in its rows | the same |
| "the row of a sealed fact: the sealed bytes, its lock, the fact that wrote it, the stamp" | `:value` sealed, `:lock-act`, `:lock-index` | "the plain bytes that its lock opened, the fact that wrote it, the stamp, who wrote it and the permission that the act named; it names no lock", with the class of each field | entries 5, 7 |
| "the row of a fact under a store key: ... and no lock" | four fields | the same and `:who`, `:permission`, "and no other field" | entry 7 |
| "the row of a retract: no value and no lock" | three fields | no value; "who retracted and under which permission, and no other field" | entry 7 |
| "a row holds a plain value: ... not a sealed row" | `holds-plain-value?` false for a sealed row | `holds-value?`: true for a tool's row that holds the text of that value | entry 5 |
| "the plain value of a row, read back: ... nothing for a sealed row or a retract" | `plain-value-of` nil for a sealed row | `value-of`: a tool's row gives the tool's value | entry 5 |
| "an accept of one sealed value writes four rows: the fact, its lock again, the answer, the last stamp" | the fact's row with sealed bytes and its lock | the row with the plain bytes that `opened` gave, who and the permission, and no lock | entries 5, 7 |
| "an accept of a request for 3 locks writes ..." | the row with no who | the row with who and the permission | entry 7 |
| "an accept of a making act writes ..." | the same | the same | entry 7 |
| "an accept of a retract and a permission given writes a row for each" | the same | the same | entry 7 |
| "a question about a place says whether the row is wanted and whether the place is the act's own" | five fields and no more | and `:marks-place`, `:k`; also for the place of a request for locks | ruling 45 |
| "the reads of a task: one for each key asked for ..." | `reads-of` takes the questions alone | it takes the rows of the marked keys too; this case is the one of a key that is not marked | rulings 18, 45 |
| "check (a) in a batch: ... the reply carries the clock and the base" | `:says`, `:clock`, `:base` | and `:batch-end` | ruling 41 |
| "a place named by one act is not shared; the row goes to an asker that wants it and does not own it" | said of every place | said "under a key that is not marked" | entry 6; ruling 45 |
| "a place named by two acts is shared, unless each of them has it as its own" | said of every place | said "under a key that is not marked" | the same |
| "the items of a task: each entry with what its replies say ..." | six fields of an item | and `:batch-end`, `:since-by-place` | rulings 41, 45 |
| "rule 1: an act with a fact under :softland.store/permission, or under :softland.store/depot, goes to the meeting task" **(further)** | two keys | "one of the five store keys whose row every task holds" | ruling 18 |
| "the home of each row: a fact under :softland.store/depot or :softland.store/permission on every task ..." | two keys | each of the five | ruling 18 |
| "the rows that the meeting task reads from its own copy: ..." | a permission fact's place, a depot fact's that names something to replace | and a setting's or a list's that names something to replace, a marks fact's always, none that the pass wrote | ruling 18 |
| "an act that meets is sent whole to its layer's task, with its digest, the clock, the base and its rows" **(further)** | six things | and the last stamp that the batch can hold, and the stamp of each of its places whose key is marked | rulings 41, 45 |
| "a batch into a base that is made: in each order of arrival the record is the record of deciding one at a time" | the model puts a row "on every task" for two keys | for five; the model and the comparison hold the facts by value | ruling 18 |
| "a fact under each of the three store keys keeps its plain value, and takes no lock" | three | six | entries 1, 2, 6 |

Three `SHOWN` lines of the pure proof say another thing now: the door's shapes "and N more" is read
from the declaration; the record's rows line says that every value is kept plain; the decision's
line says what the fact's row holds.

Twelve checks call `opened` where they called `seals`, with the same sense: the eleven words of the
seal, and "a second value under a lock whose home's row holds no bytes". Their expected word did not
change. Each other call of a function whose arguments changed (`fact-row`, `writes`, `arrive`,
`replies`, `reads-of`) is a fixture.

### The module's proof

| Check (deftest) | Old | New | Made untrue by |
|---|---|---|---|
| "the place holds the sealed bytes as sent, the fact that wrote it, its lock, and the stamp of the answer" (`an-act-lands`) | `:value` is the sealed bytes; `:lock-act`, `:lock-index` | "the plain bytes of the value, the fact that wrote it, who wrote it and the permission that the act named, the stamp of the answer, and no lock" | entries 5, 7 |
| "the lock opens the place's bytes at its place" (`an-act-lands`) | the lock opens the row's `:value` | "the lock opens the sealed bytes that the hand sent, at their place, and what it opens is the row's value" | entry 5 |
| "a retract is recorded as the current fact of its place: a row with no value and no lock" (`refusals-and-repeats`) | no `:lock-act` | no value; `[:nothing-here]`; who retracted and the permission | entries 5, 7 |
| "a permission that stands is not given again: a store key takes one write for a place" (`settings-and-permissions`) **(further)** | said of every store key | "a permission's place takes one write" | R7 |
| "each sits at its own place, sealed, ..." (`settings-and-permissions`) | "sealed" told by `(some? (:lock-act row))` | each row's value reads back as the tool's value | entry 5 |
| "a sealed value of 65,537 bytes is refused; one of 65,536 is accepted" (`strays-and-limits`) | the row holds 65,536 bytes | the row holds the 65,508 plain bytes | entry 5 |
| "the place holds the sealed bytes as sent ..." (`an-act-lands-in-the-base`) | as the first row of this table | as the first row | entries 5, 7 |
| "a sealed value in the base is opened by its lock: the bytes beside the value open the place's bytes" (`an-act-lands-in-the-base`) | opens the row's `:value` | opens the sent bytes to the row's value | entry 5 |
| "in the base a store key takes one write for a place: ..." (`a-person-acts-in-the-base`) **(further)** | said of every store key | "a permission's place and the layer's depot take one write" | R7 |
| "a tool's own fact ... its row is on one task, sealed, ..." (`a-person-acts-in-the-base`) | `(some? (:lock-act row))` | its row holds the tool's value | entry 5 |
| "each sealed value is opened by the bytes beside it" (`an-act-on-several-tasks`) | opens each row's `:value` | opens the sent bytes to each row's value | entry 5 |
| "and its bytes open the value" (`the-path-of-a-lock-in-the-base`) | the same | the same | entry 5 |
| "each sealed value of the act on several tasks is as it was sent, and the bytes beside it still open it" (`the-base-after-the-module-is-started-again`) | the row holds the sealed bytes | the row holds the plain bytes of what was sent, and the bytes beside it open the sent bytes to them | entry 5 |

Five `SHOWN` lines changed with them. The calls of `record/plain-value-of` are calls of
`record/value-of` now: fixtures.

### The door's proof

None. *Run*: it passed, 193, before one check of it was added.

## 3. Each new check, what a pass shows, and what it does not reach

"N" is the plan's number.

| N | Where | What it does | A pass shows | It does not reach |
|---|---|---|---|---|
| N1 | act: two passing rows, five refusals, two limits, one case of shapes | Parses an act under each form; a word in the fourth slot; a third word, a permission and a sixth item after the fourth slot; a slot after a first permission; the depth with the slot | The shape takes the slot and nothing else there; the slot counts for no depth | A gate |
| N2 | pure, `the-decision`: "what a permission allows" | `act/allows` of each form | Without the slot it reads as see and act | — |
| N3 | pure, `the-declaration`, `refusals-of-the-declaration`, `the-order-of-the-reasons` | Each new key with right values, each wrong value with its word, sealed, 65 layers, 33 keys, a key of the store's namespace, a long id inside each value, each limit at its edge, six things with two faults | The six rules and their order | A gate's check of the same things, which N11 runs |
| N4 | pure, `the-decision`: eight rows of the standing | A holder of see names her permission as the row has it, without the slot, with see and act; the kept forms; a making act with the slot | The new word, and that naming more does not stand | The dataflow |
| N5 | pure, `the-decision` | `opened` for a right seal, for three facts of which one is sealed, for an act whose second seal is wrong; `writes` puts those bytes in the row, which reads back as the value | A1 in the pure code | A cluster |
| N6 | pure, `the-records-rows` | The digest, a key, the two ends of a run; for six kinds of value, the keys of another value, layer, key and stamp are outside the run; the two texts of a UUID; a key back to its parts | Ruling 10's sentence, as far as texts go | A collision of the digest; Rama's order of keys, which N13 runs |
| N7 | pure, `the-decision`: fifteen cases over `streaming-step` | The plan's S5 and S6, each act decided as steps 6b and 7 decide it, on a record in memory | Ruling 20 in the pure code | The gate's own reads: the function is the proof's. A seal: the plain bytes are handed over |
| N8 | pure, `a-batch-under-marked-keys`, and the cases of `the-batch` | A batch of 21 things under marked keys, run in memory in 200 orders of arrival, against a model that decides one at a time with the marks as the batch began | Two acts on one place meet and leave the later value alone; a mark holds from the batch after; one stamp for a key that two writes hold | Rama: the proof delivers the messages itself |
| N9 | module, `what-a-fact-keeps`, `what-a-fact-keeps-in-the-base` | After an accepted value of each kind: the row, the log's record, the lock's row. A refused act. An unsealed value. And in `strays-and-limits`, bytes that are no value | A1, each gate | A failure in the middle of an event |
| N10 | module, the same two | Who and the permission of five rows; the system's making of a personal layer | A2 | A who that Rama serialised as a UUID: the pure proof has it |
| N11 | module, `the-keys-that-take-a-second-write`, and `-in-the-base` | Each key: written, again, again naming the fact, stale, a retract, another entity, sealed, each wrong value; a permission a second time; a depot after the making; a setting into a layer that is not made, and one for the base on the other depot | A3, A5, R7, each refusal recorded with its word; in the base each row on every task | A layer that is made on the microbatch depot and is not the base: none exists |
| N12 | module, `what-a-permission-allows`, and `-in-the-base` | A holder of see acts, names less, names more, gives, asks for locks; a second give to her, by her giver and by another; a holder of see and act acts and gives; a making act with the slot | A4. In the base both roads: the pass, in the batch of the give, and the refusal on arrival, in a later batch | How many rows such an act asks for in the base: no check counts questions |
| N13 | module, `what-points-at-a-thing`, and `-in-the-base` | The plan's S5 walk on the cluster, with a select of the run of each value after each act, and of the key of each entry that went out; one accepted act appended a second time | The run of each value after each act. That a row which went out is gone in the store's own module. That a second thing under a used act-id leaves the run as it was. In the base: that two places on two tasks with one value have their rows on one task | A push; a moment between a fact and its row of the other order |
| N14 | module, `one-batch-under-a-marked-key` | In one batch: two acts on one place under a marked key; a mark and a fact under it; two writes of who may see from two tasks | What the walk says they leave | A second attempt of that batch |
| N16 | door, `a-holder-of-see` | A holder of see sends a value, and an act with no value | `:have-its-request-accepted` with the gate's new word in the request's answer; nothing appended for her act; the act with no value refused under its own act-id | — |

**N15 was not written, by the task and ruling 53.** That each row of this part is the same on a
second attempt of a real batch is the check of the build of `base-for-a-reader`, which comes after
this one and sees every row.

## 4. The template's checks

- **Minimize IPC launches.** *Traced.* The module's proof starts one cluster for its 31 deftests,
  and the door's proof one for its 27: `-main` of each calls `create-ipc` and `launch-module!`
  once, and the checks run in one order on that cluster. No new launch was added. The nine new
  deftests of the module's proof share the cluster and build on what earlier ones made, as the
  proof's own approach says.
- **Implied spec coverage.** Section 5.
- **Synchronization.** *Traced.* A streaming act is appended with `:ack`, which waits for the gate,
  before its rows are selected (`decide!`). An act of the base goes through `decide-in-base!` or
  `decide-in-one-batch!`, each of which calls `wait-until-processed!`, Rama's
  `wait-for-microbatch-processed-count` over every thing that the hand appended, before any select.
  The one append that is not a decision, the second sending of an accepted act in
  `what-points-at-a-thing-in-the-base`, is followed by `wait-until-processed!` before the run is
  selected again. No new check selects before its wait.
- **The test namespaces compile.** *Run.* Each proof loaded and ran. `clj-kondo` over the four
  files: 0 errors, 0 warnings.

## 5. The implied spec, checked off

**VA1 to VA6**, each for each gate: N9, N10, N11, N12, N11, N13, in the table above. VA4's "a making
act whose own permission says see, by the ruling of P3": ruling 3 has it refused `:layer-not-made`,
*run* for a permission with the slot (N12). VA5's "a read from the layer uses the newest": the row
holds the newest list (N11); the read is the next part's.

**The matrix.**

| Block | State and write | Where |
|---|---|---|
| 1. A layer | Not made, any other act: a setting; a making act with a third fact | module N11, pure `the-first-act` |
| | Made, each setting written, written with the other word, the same word again by another writer | module N11 |
| | A setting with another word, sealed, on another entity | module N11, pure N3 |
| | A list, written again longer, shorter, in another order, empty; naming itself, a layer twice, a layer not made | module N11, pure N3 |
| | A retract of the setting, the list or a mark | module N11, pure `the-decision` |
| 2. A permission | None, a give that says see: her act, her request, her value act at the door | module N12, door N16 |
| | None, a give of see and act, or without the part: she acts and gives | module N12, N9 |
| | See, a give of see and act: by her giver, by another giver | module N12 |
| | None, a give by a holder of see | module N12 |
| 3. A place | Empty, a fact; a fact over a fact with and without `:replaces`; the same value again; a retract; a fact onto a retract; a stale fact | module N13, pure N7 |
| 4. A marked key | Not marked, a fact; the mark; a fact with an id, a UUID, a string; replaced; retracted; the mark off; the mark again | module N13, pure N7 |
| | A value that is no thing's id: a number, a map, 65,508 bytes | pure N6, N7 |
| | In the base: a mark and a fact under its key in one batch | module N14, pure N8 |

**G1 to G6 of half A.** G1: each new check gives a word, and the rows were written on a cluster
whose schemas would throw on another class: *run*. `read-value`, `who-of`, `permission-of`,
`marked-keys-of`, `id-of-text` and `place-of-key` are each given things that are none: *run*, pure.
G2: an act refused at its second seal leaves no row: *run*. G3: N13's second sending. G5: six things
with two faults for the declaration; a holder of see with a stale fact for the gate. G4 and G6 are
of the code and the store's page.

## 6. Do the checks catch a wrong build? Six breaks, each run

This is a thing that the skill's steps do not ask for, done once, so that "a pass shows" above is
not only read. One line of the pure code was changed, the pure proof run, and the line put back; six
times. Each break failed the proof.

| The break | Failing checks |
|---|---|
| Two acts that each own a marked place do not meet (`place-replies`) | 2: the case of the reply, and the batch in memory |
| An entry is taken out whatever the stamp of the row before (`by-value-writes`) | 1: "a fact written after the second mark" |
| `standing` does not ask what the permission allows | 4 |
| A key that stays marked gets a new stamp (`level-since-after`) | 2 |
| The microbatch gate gives a newly marked key the act's own stamp (`decided`) | 2, the batch in memory among them |
| The row before is not sent to an asker that does not own a marked place (`place-replies`) | 1 at first: only the case of the reply |

The last showed a hole: the batch in memory held no marked place on another task than its act's
that had a value to take out. The fixture gained one, and the same break then failed 3 checks. The
source was put back after each run and compared with its copy.

## 7. The finish: the last run of the four proofs

Each from the repository root, on the working tree as this session left it, with nothing committed.
The cut outputs are `runs/after-what-the-gates-keep-<proof>.txt`: the `SHOWN` lines once, the count
line, the exit status.

| Command | Count line | Exit |
|---|---|---|
| `clojure -M:store -m softland.store.act-proof` | `{:test 8, :pass 160, :fail 0, :error 0}` | 0 |
| `clojure -M:store -m softland.store.pure-proof` | `{:test 17, :pass 671, :fail 0, :error 0}` | 0 |
| `clojure -M:store-module -m softland.store.module-proof` | `{:test 31, :pass 333, :fail 0, :error 0}` | 0 |
| `clojure -M:store-module -m softland.store.door-proof` | `{:test 27, :pass 199, :fail 0, :error 0}` | 0 |

The first two ran on the alias `:store`, which holds Clojure alone: each pure namespace still loads
with no Rama on the classpath. No run failed on a port or on memory.

## 8. What no check reaches

- That a streaming layer's fact and its rows of the other order have no moment between. It rests on
  one event with no partitioner: read in the code.
- An event that Rama discards and runs again; a failure in the middle of an event or of a batch.
- A second attempt of a batch, for any row of this part: the other part's build (ruling 53).
- A push. That a removal of a key that holds no row reaches an open push on its run was run by the
  check of the plan, on a stand-in table and not on the store's module.
- A real cluster, a second node, a move of a task's leader.
- What the one read more costs a gate, a run of very many places, one value that very many places
  hold on one task of the base: the measures of the contract's section C.
- A collision of the value digest.
- A reader that asks through layers, and whether an asker may see a layer: the next part.

## 9. Points for the build session

1. The count of the plan's table was 27 rows; six further checks were written again (section 2).
2. The six breaks of section 6 are a check beyond the skill's steps. They changed no source that is
   left changed; one of them led to one more row of a fixture.
3. The last run of the module's proof took about a minute and a half on this machine, with its 106
   assertions more. I did not time that proof before this build, so I cannot say how much of that
   is new.
