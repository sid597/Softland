# Implementation Validation

This paper is the build stage's, of the tree at the commits `f33177cb` and `1626bab5`. The review of
the whole changed the door after it was written (`FULL_SPEC_REVIEW.md`, "Items found and fixed");
where the two differ, the review and the code hold. Its sixth difference from the plan, "no step
starts after the call's wait has ended", was not true of one way then, and is true now.

<!-- Phase 4. Fill in after Phase 3 produces the module source. -->

Part: `door`, the one part of the door's build. Stage: the build, step 2 of the Rama skill's
`phase-build.md`, "validate the implementation against the plan and spec" (`phase-4-impl-validate.md`).
Written by: the build agent (Opus 5.5), 10 October 2026, in the same session that wrote the code, as
the build stage asks. It is for the build session to judge. The code that it validates:

- `src-store/softland/store/door.clj` (885 lines), `door/additions.clj` (413), `door/stock.clj` (380),
  `door/words.clj` (202), and the one function that `door/seal.clj` gained, `sealed-length`;
- the explanations that changed with them, in `src-store/` and `test-store/`.

Line numbers below are of those files as they stand at the end of the build.

## How the skill's template is adapted

The template is written for a Rama module: dataflow, paths, PStates, topologies. The door is client
code over a module that exists. It has no dataflow, no path, no PState and no topology. No section of
the template is left out. Each is answered for the door, or says in a line why it does not apply.

| The template's section | What it holds here |
|---|---|
| Redundant conditionals | The door's own branches, read for two branches that do one thing. |
| Consecutive keypath | Does not apply: the door writes no path. One line. |
| Select-compute-transform | Does not apply: the door writes no PState. Its analogue, a read of the stock followed by a write of it, is answered. |
| Unnecessary nil->val | Does not apply. One line. |
| :allow-yield? | Does not apply to a client. What the door's one long read costs a task is named. |
| Non-subindexed collections without size limits | The collections in the door's memory, each with what bounds it. |
| Stream topology idempotency | What holds when a call of the door is made twice, and where the door makes ids. |
| Partial failure in stream topologies | What holds when a send ends between two of its steps. |
| Single depot append per client operation | The one check that the door fails by the template's letter, and why the contract asks for it. |
| Application-state caches survive restart | The stock: what it is rebuilt from (nothing), and why the contract tolerates its loss. |
| No reimplementation of built-in operations | Rama's built-ins and the tree's own rules, each written once. |
| Plan conformance | Every place where the code is not what the plan says, and every passage of the plan that still differs from a ruling. |

Sections that the template does not have, and this paper needs: the fails that this validation found
and fixed; the traces of the plan's check, walked on the code; the implied spec's invariants against
the code; the contract's closed list; the explanations; points for the build session; and what the
runs found after this paper was first written.

## What was read, and what was run

Read whole before any code was written: the skill's `SKILL.md`, `phase-build.md`, `phase-3-implement.md`,
`foreign-client.md`, `testing.md`; `src-store/CLAUDE.md`; the contract; `PLAN.md`; `IMPLICIT_SPEC.md`;
of `PLAN_VALIDATION.md` the fails, the points, the template's checks, the cells of the matrix,
"Concurrency", "Failure", "The cost, recounted", "The module as built", "The door's proof" and "The
explanations"; `act.clj`; the four files of `door/` and its README; `module.clj`;
`module/door_reads.clj`; `record/rows.clj`; `gate/first_act.clj`; `record/tables.clj`; the store's map;
the two README files above it; the three READMEs under `test-store/`; `module_proof.clj:1-660` and
`1440-1630`; `pure_proof.clj:1-320`, `744-790`, `2030-2247`; the two probes' sources and outputs.

Read for this step: `phase-4-impl-validate.md`, `artifact-impl-validation.md`, and the four source
files of the door, line by line, against the plan's sections in their order.

Not read: the rig's code, the server's, Inland's; any `env.clj` or `.env` file; `gate/batch.clj` and
`gate/decision.clj` beyond a search for how a gate makes locks (`decision.clj:355-369`).

Run, each from the repository root, outputs under
`/mnt/data/projects/research/softland/door-build-2026-10-09/build/`:

- the load of the three pure namespaces under `:store` and of `softland.store.door` under
  `:store-module`, with reflection warnings on: none (`load.sh`);
- `clj-kondo` on the five source files: one warning, an unused value, fixed;
- a smoke run of the pure parts on values (`smoke.clj`) and of the door on the in-process cluster
  (`src/buildprobe/smoke.clj`, `out-smoke.txt`), before the proofs existed;
- a comparison of the forms of sixteen files at `HEAD` and in the working tree with every string set
  to nothing (`closed/forms.clj`, `closed/out-forms.txt`), for "The contract's closed list" below.

## The fails that this validation found, each fixed in the code

Each was found by reading the code against the plan or the implied spec, before the door's proof was
run. None asks for the design to be made again.

**V1. The door did not make the test of what it passes to the read of a request's locks.** Plan,
"Reads": "Before each read the door tests what it passes, with the function of `record/rows.clj` that
the head of that query uses"; its table of requires names three tests. The first code called
`depot-read` and `answer-read-by-routing-id` and never `lock-range-read-by-routing-id`. Fixed:
`read-locks` (`door.clj:225-240`) makes the test and gives an empty map for what fails it, as the query
would; `read-answer` (`:207-223`) makes its test in one place, so the reads of a request's answer make
it too.

**V2. A wait could throw for nothing that a maker gave.** The first code added a span to
`System/nanoTime` with Clojure's `+`, which throws on overflow, and the JVM gives no promise about
where the numbers of that clock begin. Plan, "The calls of the door": "No call throws for what a
maker gave". Fixed: `after`, `left` and `passed?` (`door.clj:265-284`) add and compare moments without
the overflow check, as differences; `stock/longest-wait-ms` (`stock.clj:84-88`) cuts a wait to about 73
years so that each difference stays in range; `stock/kept-long-enough?` compares by a difference too.
The cut is a number that the plan does not have: see "Plan conformance", 3.

**V3. A maker's thing that holds a part named `:could-not` would have been taken for one of the
door's words.** The first `thing-to-append` gave the thing itself, and `send-act` told a word from a
thing by the key `:could-not`. A maker's act `{:layer :alice ... :could-not 1}` is a thing that the
plan sends as given, for a gate to refuse `:unknown-part` (IS table, row 17); `send-act` would have
given it back to the maker as a result, with nothing appended. Fixed before the first run:
`thing-to-append` gives `{:thing the-thing}` (`additions.clj:361-386`), and `send-act` reads `:thing`
(`door.clj:795-798`). The pure proof and the door's proof each hold a thing with that part.

**V4. A door's printed text had no home with a docstring.** `src-store/CLAUDE.md`: "The docstring of
each function, a private function too". A `deftype` and its print methods take no docstring. Fixed:
`printed` (`door.clj:125-133`) is the one function that makes the text, and the constructor's
docstring is set on its var (`:152-159`).

## Redundant conditionals

Pass. `append!` (`door.clj:184-205`) has two branches that differ in the depot's handle and in the
ack level, which is two things, so it is not one call with a variable. `settle` (`:416-489`) has one
branch for each of four outcomes of the wait and each does another thing. `ask` (`:491-534`) maps two
of `settle`'s outcomes to two words and passes the rest on. `entry-of` (`additions.clj:151-230`) has
three branches that give `{:fact fact}`: no map, a record with no value, and a plain map with no value
or under a store key. They are kept apart because the table of the walk has a row for each and the
order between them is the rule (a store key is tested before the door's own parts).

## Consecutive keypath

Does not apply: the door writes no path. It calls three queries and holds no PState handle
(`door.clj:135-142`, the eight fields of a door).

## Select-compute-transform

Does not apply as written: the door has no `local-select>` and no `local-transform>`. Its analogue is
"read the stock, compute, write the stock", and each such place is one compare-and-set on the value
that was read, never a read and then a blind write:

- `take!` (`door.clj:390-403`): reads the cell, computes `stock/take-locks`, and writes with
  `compare-and-set!`; on a lost race it reads again.
- the step after a read of locks (`settle`, `:484-489`): reads the cell, computes
  `stock/with-locks-read`, writes with `compare-and-set!`, and loops.
- the open request (`ask`, `:517-524`; `settle`, `:466`, `:473`): `swap!`, which is the same loop.

Pass.

## Unnecessary nil->val

Does not apply: no navigator.

## :allow-yield?

Does not apply to a client: the door runs on a maker's thread and on no task. The one long read that
it causes on a task is the range of a request's locks, 256 rows at most; the module's query has
`{:allow-yield? true}` on that range in a microbatch layer (`module/door_reads.clj:135-139`), and
Rama's client reads yield on the task by themselves (skill `foreign-client.md:118`). The door changes
nothing of it.

## Non-subindexed collections without size limits

The template asks it of a PState. For the door: each collection in its memory, and what bounds it.

| Collection | Where | What bounds it |
|---|---|---|
| The door's map of stocks | `door.clj:343-362` | Nothing but the life of the process: one entry for each layer, depot and who that wrote a value through the door, or that left an open request. The plan says so ("The memory of the door's process") and the namespace docstring says so (`:51-58`). A stock that holds nothing leaves the map (`leave!`, `:364-388`). |
| The runs of one stock | `stock.clj:214-252` | Two between requests, by the taking order; one more for each earlier request that a call read while the stock was short. A run that gave its last lock is dropped (`:250`). |
| The locks of one run | `stock.clj:324-340` | The count that the request asked for, 256 at most (`ask-for`, `:151-187`). A list, so that a take drops the head and nothing keeps the locks that were taken. |
| The entries of one reading | `additions.clj:232-270` | The facts of the maker's act. The walk has no bound on facts as such, which the plan leaves to a gate; it stops at the 257th value fact. Dead when the call returns. |
| The vector of `act-ids-for` | `door.clj:681-703` | The count that the maker gave; the docstring says that it has no bound but memory. |

Pass, with the first row as the plan has it: no bound, said.

## Stream topology idempotency

The template asks what holds when an event runs twice. The door has no event. Its analogue: what holds
when a maker calls twice, and where ids are made.

- **Ids are made in the client**, as the template advises: an act-id's uuid by `UUID/randomUUID` in
  `act-id-for`, `act-ids-for` and `send-act` (`door.clj:679`, `:703`, `:787`), and a request's in `ask`
  (`:514`). The door makes no id inside a topology and keeps none.
- **The door sends nothing twice.** `send-act` has one `append!` of the act (`:800`) on one path, and
  `ask` one `append!` of a request (`:520`). No loop holds either. A request whose append threw is kept
  as in doubt (`:522-523`) and read later; it is never appended again.
- **A maker's second send of one act** is another thing when the act has a value: `locks-for` takes
  other locks and `seal` another random part. The gate decides one thing under an act-id (the module's
  check (a)). The docstring of `answer-of` says how a maker reads that (`door.clj:836-841`).
- **`answer-of`, `act-id-for`, `act-ids-for` and `stock-count`** write nothing, so a second call
  changes nothing.

Pass.

## Partial failure in stream topologies

The template asks what holds when an event fails after some of its writes. For the door: a send that
ends between two of its steps. Traced on `send-act` (`door.clj:776-805`) and `locks-for` (`:567-624`).

| The send ends | What is in the cluster | What is in the door's memory | Bounded by |
|---|---|---|---|
| Before `locks-for` | Nothing | Nothing | |
| In `ask`, after the request's append | The request; a gate decides it | The open request, in doubt or sent, when the process lives. The next call of that stock reads it (`in-the-seat`, `:555-556`) | One request for each stock |
| After `locks-for`, before the act's append (a seal gave nothing, or the process ended) | The request's locks, free | The stock without the act's locks: they do not come back | The act's count of values, 256 at most |
| In the act's `append!`, by a throw | The thing, or not | Nothing of the act. The maker has the act-id and the digest in the word (`:802`) | |

No row leaves a lock both in a stock and on a fact: a lock leaves the stock by `take!` or by the
compare-and-set of `settle` before it is sealed with. Pass.

## Single depot append per client operation

**By the template's letter this fails, and it is not a fault of the code.** The template: "each client
write operation must call foreign-append! exactly once". `send-act` makes two appends when the stock is
short: the door's request for locks (`ask`, `door.clj:520`) and then the act (`:800`).

The contract asks for exactly that (C part 3: "it sends a lock-request act [...] it reads the request's
answer by act-id and then its locks"), because in Rama a query cannot write
(`door/declaration.clj:132-136`), and the plan and its check passed it. What the template protects,
"a client crash between them leaves inconsistent state", was traced above: the request is a whole act
of its own; what a crash between the two leaves is locks that seal nothing, counted and bounded. No
record is wrong for it.

So: one append for each act that the door sends, the request being one act and the maker's act
another. Recorded here as the one check that does not pass as written, and not passed in silence.

## Application-state caches survive restart

The stock is the one application state in the process (`door.clj:343-362`; `stock.clj`).

- **Is there a durable source to rebuild it from?** The lock rows are in the record. The door could
  not rebuild a stock from them: it reads locks only of requests that it made itself in this process
  (C D6), and it does not know the requests of a process before.
- **Is there a rebuild path?** None, by the contract: "It owns no durable state. Its one memory is a
  stock of locks (part 3), the memory of one process" (C part 1); "a restart asks again" (C D3). `open`
  starts with an empty map (`door.clj:653`).

By the template's letter that is a fail; the plan's "State primitive selection" has the same answer
and its check passed it on the contract's words. What a restart costs is bounded by the size rule and
counted by the door's proof.

Three things were checked so that nothing else is a kept copy of what the cluster holds (the
project's rule on caching):

- no layer's depot is kept: `depot-of` (`:242-261`) is called inside a call and its result is a local;
- no answer is kept: an open request holds `:act-id :digest :count :state :since` and nothing that a
  read gave (`stock.clj:256-265`); after a read of locks that threw the next call reads the answer
  again (`settle` is entered with the same open request, `door.clj:555-556`);
- no act-id, thing or digest of a maker's act is kept: they are locals of `send-act`.

## No reimplementation of built-in operations

Pass, with one line that a ruling names.

- Rama: the door uses four client calls and no other var of Rama (`door.clj:112`, the alias `rama`, and
  its uses at `:203-204`, `:222`, `:239`, `:258`, `:647-651`). It makes ids with the JVM's random
  UUID because Rama 1.6.0 has no uuid function in `com.rpl.rama.ops` (the plan's probe 1), and the
  act's decision asks for a random one.
- The tree's own rules, each asked of its one home: what a value is (`act/value?`,
  `additions.clj:218`); the tests of an id and of an act-id (`declaration/usable-id?`,
  `declaration/act-id-in-layer?`, `:145-148`); a store key (`declaration/store-key?`, `:206`); the
  three limits (`additions.clj:85`, `:224`, `:350`; `stock.clj:186`); the length of a seal
  (`seal/sealed-length`, `additions.clj:223`); the routing id and the three tests of what a read is
  passed (`door.clj:220-221`, `:237-238`, `:674`, `:697`, `:861`); the depot that a layer starts with
  (`:261`); the digest (`:518`, `:799`); the one text (`additions.clj:222`).
- The test of a plain map is written once more, privately (`additions.clj:87-93`), by ruling 10.
- The key `:softland.store/lock-request` is written as a literal in `additions/request`
  (`additions.clj:413`), as `gate/decision.clj:366` and the declaration's table write it; the
  declaration gives no name for the key alone.

## Plan conformance

Each sentence of the plan's design sections was held against the code. What follows is every place
where the code is not what the plan says, and then every passage of the plan that still differs from a
ruling. Everything else matches; the three tables after it show where.

### Where the code is not what the plan says

1. **`stock.clj` does not read `declaration/max-facts`.** The plan's table of requires gives the
   stock both limits. The first bound of P9, the smaller of the two, has one home,
   `additions/most-values-in-one-act` (`additions.clj:74-85`), because the walk needs it and the plan's
   table does not let `additions` require `stock`. The stock reads the ceiling of one request only
   (`stock.clj:186`). Reason: one home for one bound (P6, P9: "each read from the declaration in one
   place"). Not a correctness issue of the plan; the two readings give the same door.
2. **The door's five numbers are declared in `softland.store.door.stock`** (`stock.clj:90-149`), with
   the function that reads what the opener gave. The plan says that the opener gives them to `open`
   and does not say which namespace holds the defaults. They are in a pure namespace so that the pure
   proof holds the defaults and the cuts as cases.
3. **A wait is cut to about 73 years** (`stock/longest-wait-ms`), for `:request-wait-ms`,
   `:read-every-ms` and the maker's bound of `answer-of`. A number that the plan does not have. It is
   what V2 needed: without a cut, the arithmetic of the clock could leave the range of a long. Evidence:
   Clojure's `(+ Long/MAX_VALUE 1)` throws `ArithmeticException`.
4. **`send-act` takes a nil act-id as none given** (`door.clj:776`, `:781`). The plan: "when the maker
   asked for one ahead, its act-id". It does not say what nil is. The other reading, nil as a thing
   that is no act-id, would give `:make-an-act-id` at `:act-id` for a maker that passes its own "none".
5. **`answer-of` comes back at once for a thing that is no act-id, also with a bound**
   (`door.clj:861-862`). The plan: "`answer-of` gives 'nothing yet' [...] No round trip", and says
   nothing of the bound. The door makes no read for such a thing, so a wait would be a sleep for
   nothing. The docstring says it (`:833-834`).
6. **No step of a request is started after the call's wait has ended, the read of the locks too**
   (`settle`, `door.clj:476-477`). The plan's row D2 says "Reads the locks" with no condition, and its
   general sentence says "A step is started only while the call's own T has not passed"; its paragraph
   on how far the bound reaches says that the call in the seat "can outlast its bound by one append and
   by one query". The code follows the general sentence, so that the second holds: a call whose read of
   the answer began at T does not go on to a second query. The request then stays open, and the next
   call reads the answer again and then the locks.
7. **The table of words has two fields more than the plan names**, `:from` and what a word carries
   (`words.clj:43-138`, `carried`). The plan: "each with what it says and with its `:appended`". Data
   only; no call reads them but the proofs.
8. **`:read-the-act` is also the word for a `:who` that cannot be hashed** (`door.clj:607-608`), at
   `:who`, and for a thing that throws while the door builds on it (`additions.clj:386`). The plan's
   table has the word for "it threw while the door read it"; it names no case of a throw after the
   reading. Both are a thing that throws when the door handles it, and nothing is appended.
9. **`stock-count` gives its three numbers under `:unused-locks`, `:stocks` and `:open-requests`**
   (`stock.clj:366-380`). The plan names the three numbers and no names for them. Listed for Sid in the
   door's README.
10. **A run keeps the way of its locks once, always**: where the locks of one request are of more than
    one known way, they are split into runs of one way each (`stock.clj:324-340`). The plan: "The way
    of a run's locks is kept once for the run when they are all of one way", and says nothing of the
    other case. No lock of this build is of a second way.

### Passages of the plan that still differ from a ruling

1. **"What holds when something fails", the row "The request's append throws"** is as ruling 12 has
   it. **"Concurrency", the answer "A stock that never asks again" in `PLAN_VALIDATION.md`** still says
   of a request in doubt "forgotten at the end of the next call's wait", which is ruling 6 before
   ruling 12. That is the check's paper and not the plan; the code follows ruling 12
   (`stock/kept-long-enough?`, `stock.clj:292-310`).
2. **`PLAN_VALIDATION.md`, "Failure", 1 and 3** still name a third state, "the open request,
   accepted". Ruling 13 took it away; the plan's own table of the life of a request has two states,
   and so has the code (`stock.clj:256-290`).
3. **The plan's "Design difficulty log", "The limit of sealed bytes without the number"** still tells
   the first answer, a length measured with a lock made for nothing else. Ruling 5 says that the entry
   "stays as the record of the first answer"; the design passage is as the ruling has it, and the code
   asks `seal/sealed-length` (`additions.clj:223`).
4. **The plan's section "The door's own proof", group 8, `:read-its-requests-locks`**: "After
   `:read-its-requests-locks`, the next send reads the locks of the same request". By ruling 13 the
   next send reads the answer again and then the locks; the plan's table of the life of a request says
   so, and this sentence of the proof's list does not. The door's proof counts two queries there.

No passage of the plan's design sections was found that the code had to choose against a ruling for.

### Where each call of the plan is in the code

| The plan | The code |
|---|---|
| `open`: five fetches, the numbers, a door or `:find-the-module` | `door.clj:628-655` |
| `act-id-for`: the test of the layer, one read, the starting depot for nil, a random uuid | `:657-679`, `depot-of` `:242-261` |
| `act-ids-for`: the same with one read; a count that is no integer of 1 or more | `:681-703`, `additions/series-word` |
| `send-act`, steps 1 to 4, no round trip | `additions/reading` (`additions.clj:272-318`) |
| step 5, the act-id | `door.clj:781-787` |
| step 6, the locks | `locks-for` (`:567-624`) |
| step 7, the seal, the thing, the digest | `additions/thing-to-append`; `door.clj:795-799` |
| step 8, the append, with `:ack` or `:append-ack` | `append!` (`:184-205`), `:800` |
| step 9, `{:act-id :digest :ack}` | `:803-805` |
| `answer-of`: the test, the wait, the four answers, the word | `:807-871`, `read-answer-until` (`:298-339`), `words/answer` |
| `stock-count` | `:873-885`, `stock/count-of` |
| The door as a value that prints no stock | `:125-159` |

### Where the stock of the plan is in the code

| The plan | The code |
|---|---|
| One stock for `[layer depot who]` | `locks-for`, `door.clj:606` |
| The size rule | `stock/ask-for`, `stock.clj:151-187` |
| Taking is one compare-and-set and never waits | `take!`, `door.clj:390-403` |
| Asking needs the seat; the seat is fair and can be left at a time limit | a semaphore of one permit, fair (`enter!`, `:361`); `tryAcquire` with the time that is left (`:616-619`) |
| One time limit for all of a call's getting of locks, from the first short take | `began` and `ends`, `:614-615`, passed to every step |
| D0: written in doubt before the append; sent after | `ask`, `:517-524` |
| D1: reads at once, or after one interval for an own request of the base | `settle`, `:455-457`; `read-answer-until`, `:328` |
| D2, D3: the locks, and one step for the run, the forgetting and the take | `:480-489`; `stock/with-locks-read` |
| D4: refused or another digest; whose refusal | `settle`, `:472-474`; `ask`, `:530-532`; `in-the-seat`, `:555-565` |
| D5: in doubt from the throw; kept by its age | `ask`, `:522-523`; `stock/kept-long-enough?`; `read-answer-until`, `:336` |
| One call sends one request and reads one earlier request at most | `in-the-seat`, `:553-565`: `settle` once, then `ask` once |
| A call counts itself in and out; an empty stock leaves; "holds nothing" read inside the step | `enter!`, `leave!`, `:343-388` |
| A thread interrupted in a wait, and in a call of Rama's client | `:617-619`, `sleep-for` `:286-296`, `what-threw` `:163-173` |

### Where each ruling is in the code

| Ruling | The code |
|---|---|
| 1, 16 | No request ahead of need; one request at a time; 64 and 256: `stock/default-numbers` |
| 2 | No copy of a layer's depot: `depot-of` gives a local |
| 3 | The answer, then the locks: `settle` |
| 4 | Oldest run first, across the border: `stock/take-locks` |
| 5 | `seal/sealed-length`, `seal.clj:107-116`; asked before a lock, `additions.clj:223` |
| 7 | Two groups of files in `door/README.md` |
| 8, 11 | A read that came back ends its request (`with-locks-read`); a short read of an earlier call's request goes on to the call's own (`in-the-seat`); of its own, the word (`ask`, `:527-528`) |
| 9 | The store-key row is tested before the door's parts: `additions.clj:206-213` |
| 10 | `plain-map?` private in `additions.clj` |
| 12 | Kept by its age, by the clock of the waits |
| 13 | Two states; no answer kept |
| 15 | "stock" listed for Sid with what was found, `door/README.md` |
| 17 | `most-values-in-one-act`, with its docstring |
| 18 | `what-threw` sets the mark again |
| 6, 14 | 6 is amended by 12. 14 is the build session's, on commits |

## The traces of the plan's check, walked on the code

`PLAN_VALIDATION.md`, "Concurrency" and "Failure", each walked on the lines.

**1. Two makers, the stock empty, a streaming layer.** A and B each call `locks-for`. Each `enter!`
counts in (`:609`); each `take!` gives nil (`:612`). A's `tryAcquire` succeeds; B's waits, for the time
left to its own `ends` (`:616`). A in `in-the-seat`: `take!` nil (`:553`); no open request (`:555`);
`ask`: n is 4, the request is written in doubt (`:517`), appended (`:520`), made sent (`:524`);
`settle` reads the answer at once (`sleep-first?` is false in a streaming layer), reads the locks
(`:480`), and in one compare-and-set puts the run in, forgets the request and takes lock 0
(`:484-489`). A leaves the seat in the `finally` (`:623`) and counts out (`:624`). B sits, and its
first step is `take!` (`:553`): lock 1. B sent nothing.

**2. Three makers, the stock short.** The same lines; n is `ask-for` of the stock's `:given-out` at
`:515`. The call that asks is served by its own request because n is never less than m and the run
goes in and the m come out in one compare-and-set.

**3 and 4. Takes that race.** `take!` loops on `compare-and-set!` of the whole stock value, so two
takes that read one value cannot both write. A take that lost to the step of `settle` reads the new
value. A take before that step is short, and its call stands for the seat; when it sits, `:553` takes.

**5. Leaving the seat.** By a word: every word of `settle`, `ask` and `in-the-seat` is a returned
value, so the two `finally`s run. By a throw of Rama's client: `attempt` (`:175-182`) turns it into a
value. By an interrupt in the wait for the seat: caught at `:617`, the mark is set, and the seat was not
taken, so it is not released (`:622` is entered only when `seated?`). By an interrupt in a sleep:
`sleep-for` gives false and `read-answer-until` gives `[:nothing]` (`:338-339`).

**6. An empty stock taken out of the map (F13).** `leave!` (`:381-387`) computes the count after this
call and reads `holds-nothing?` of the cell inside the function that `swap!` applies, only when that
count is zero. If another call entered meanwhile, the map changed, the swap runs again on the new
map, and the count is not zero. If the swap succeeds with a count of zero, no call entered since the
map was read, and only a call that is counted in changes a cell, so the reading cannot be overtaken.

**7. An open request met by the next call.** Sent: `settle` with `own?` false reads at once. Accepted
and the locks serve: `{:locks}`. Refused or another digest: forgotten (`:473`), `{:ended :no-accept}`,
and `in-the-seat` goes on to `ask`; the answer is handed to nobody. In doubt: `read-answer-until`
gets the open request as `in-doubt` and gives `[:given-up]` only for a read that began the door's wait
or more after `:since` (`:336`). Too few locks: `{:ended :short}`, and `in-the-seat` goes on to `ask`
when the call's wait has not ended (`:561-565`).

**Failure, each place where a call of Rama's client can throw.** The read of the layer's depot:
`depot-of` gives a word before `locks-for` is entered (`door.clj:781-789`), so no stock is touched. The
request's append: `:522-523`. A read of the request's answer: `:459-460`, and the request is not
changed. The read of the locks: `:481-483`, the same. The act's append: `:801-802`, after the locks
left the stock. `answer-of`: `:869`. `open`: `:654-655`.

**F8, a thread interrupted inside a call of Rama's client.** `what-threw` sets the mark again for an
`InterruptedException` (`:171-172`). The request was written before its append, so the stock knows it.

**F1, how far the door's wait reaches.** `passed?` is asked before each step (`:476`, `:561`) and
inside the wait (`:337`); a call of the client that has begun is not cut. The docstring of `locks-for`
says which call can outlast the wait and by what (`:590-602`).

## The implied spec's invariants against the code

Each of O1 to O6 was read against the code. The ones whose place is not obvious from the tables above:

| Invariant | The code |
|---|---|
| O1.1, no PState handle | Five handles (`door.clj:647-651`) |
| O2.3, a uuid made from nothing | `UUID/randomUUID` |
| O2.5, each fact at its position | `additions/facts-to-append` builds the vector in the order of the entries (`additions.clj:322-359`) |
| O3.2, a count of 1 to 256 | `ask-for` gives `min(256, max(m, ...))`, m is 1 or more (`door.clj:792`) |
| O3.3, the request in the depot of the act's act-id | `ask`, `:514` |
| O3.7, P18: no lock's bytes in a word, in what is printed or thrown, or in what a maker gets | Words carry what `words/carried` lists (`words.clj:123-138`); the word of the read of locks carries the class alone (`door.clj:482-483`); a door prints as `printed`; the door throws nothing |
| O3.11, whose refusal | Only `ask` makes `:have-its-request-accepted` |
| O4.1, three additions and no other change | `thing-to-append`: `assoc` of `:act-id`, `:depot` and, when the facts were a sequence, `:facts` |
| O4.7, the digest of the very thing | `digest/hex-of thing` and `append! ... thing` take the same object (`door.clj:798-800`) |
| O4.11, no throw for what a maker gave | `reading` and `thing-to-append` catch every throw; each call of Rama goes through `attempt`; the hash of the stock's triple is guarded (`:607`); `stock/numbers` never throws |
| O4.14, neighbours | `take-locks` |
| O5.7, no read starts after the bound | `read-answer-until`: `passed?` before each sleep |
| O5.9, `answer-of` changes nothing | It touches no stock |

## The contract's closed list

"Any change to the gates, the record or the declaration" is outside this build. *Ran*
(`closed/out-forms.txt`): for each of sixteen files, every form of the file at `HEAD` and in the
working tree was read, every string set to nothing, and the two compared.

- Byte for byte as at `HEAD`: `door/text.clj`, `module.clj`, `module/streaming_gate.clj`,
  `module/microbatch_gate.clj`, `record/tables.clj`, `gate/first_act.clj`, `gate/decision.clj`,
  `gate/batch.clj`, `gate/stamp.clj`, and `act_proof.clj`.
- Equal with every string set to nothing, so changed inside strings only: `act.clj` (1 string, its
  namespace docstring), `door/declaration.clj` (1, its namespace docstring), `door/digest.clj` (2, its
  namespace docstring and the docstring of `is-digest-of?`), `module/door_reads.clj` (1),
  `record/rows.clj` (1), `module_proof.clj` (1, its namespace docstring).
- `door/seal.clj` gained `sealed-length` and two passages of its namespace docstring; no other line of
  it changed (`git diff`). `deps.edn` did not change.

## The explanations

- A docstring on the namespace and on every function of the four new files, private ones too. A
  `deftype` and a `defmethod` take none; see V4.
- No docstring and no README of the tree cites this build's papers by their marks. Searched for
  "ruling", "the plan", "F1" to "F15", "P1" to "P22", "C part", "IS O", "rule A", "rule B": none in
  the files that this build wrote or changed. The queue's rows (`?9`, `K9`, `W3`) are cited where the
  door leaves room for one, as the contract asks. The contract's own rule numbers 1 to 8 are cited in
  `door/README.md`, as the store's map cites the store's 1 to 21.
- "Key" is used of a fact's key only; the three things that a stock is for are "a layer, a depot and a
  who", and in code `triple`. "Lock" is used of an encryption key only; what lets one call at a time
  ask is the seat, a semaphore, so no name of the JVM says "lock" for it either.
- Each number that is a choice has its reason where it is declared: the five numbers
  (`stock/default-numbers`), the size rule (`stock/ask-for`), the bound of one act
  (`additions/most-values-in-one-act`), the longest wait (`stock/longest-wait-ms`).
- Each claim of a wait, a bound, an ack or a retry says how far it reaches: `append!`,
  `read-answer-until`, `locks-for`, `send-act`, `answer-of`.

The sentences of the tree that the door makes untrue are in the report to the build session, by the
rows of the plan's table and beyond it.

## Points for the build session

1. **"Single depot append per client operation" and "caches survive restart" do not pass by the
   template's letter.** Both are the contract's own design. Nothing to fix; recorded.
2. **The first bound of P9 has one home, in `additions`, and the stock does not read `max-facts`.**
   Divergence 1.
3. **A cut of about 73 years on every wait.** Divergence 3. A number that no paper has.
4. **Nil as "no act-id given".** Divergence 4.
5. **No read of the locks after the call's wait has ended.** Divergence 6. The other reading saves one
   request's round for a call at the edge of its wait and lets a call in the seat outlast its wait by
   two queries.
6. **A thing with a part `:could-not`.** V3. The plan's sentence "A *word* is a map that holds
   `:could-not`; no other result of the door holds that key" is true of results; a builder that tells
   a word by that key inside the door must not ask it of a maker's thing.

## What the runs found, after this paper was first written

`TEST_VALIDATION.md`, "What the runs found", has each run. For the code:

- The door's proof passed at its first run and at each of the six after it; the pure proof's three
  groups passed at their first run. No line of the four source files was changed because of a run. The
  faults that the proofs' validation found were in the proofs.
- Twenty-seven wrong doors, each one change of one of the four source files, each fail the door's
  proof (`build/mutants/results.txt`). So the lines that those changes touch are held by a check: the
  stock's triple, the take, the size rule, the open request and its age, the order of the walk, the
  seat, the count of calls, the first read in the base, the interval of the wait, the thread's mark,
  the ack level of the streaming depot, the digest in the word of a thrown append, and the printed
  text.
- The final four count lines: act `{:test 8, :pass 150, :fail 0, :error 0}`; pure
  `{:test 16, :pass 538, :fail 0, :error 0}`; module `{:test 22, :pass 227, :fail 0, :error 0}`; door
  `{:test 21, :pass 162, :fail 0, :error 0}`.

What no run reached in the code, so that it rests on the traces above alone: the step of `leave!` that
lets an empty stock go, when it races a call that enters; a request in doubt whose append truly
failed, met before the door's wait has passed since the throw; the cut of a wait to
`longest-wait-ms`, and a clock whose numbers wrap; a `:who` that cannot be hashed.

## The self-consistency check

- "Single depot append" and "caches survive restart" are written as not passing by the template's
  letter, and the verdict below says so.
- Each of the ten divergences says why the code is as it is; none is "easier another way". 1, 2, 7, 9
  and 10 are places where the plan does not say, or where two of its sentences name one thing in two
  homes. 3 is a correctness fix (V2). 4, 5, 6 and 8 are readings where the plan is silent; each is a
  point above.
- No check is passed here whose text says that it does not hold.

## Verdict

**minor-fail, fixed in this session.** Four fails (V1 to V4), each a few lines of the existing files
and each fixed before the door's proof was first run; two of the template's checks do not pass by
their letter and are the contract's design. Nothing asked for the code to be built again.

PHASE_VALIDATION:minor-fail
