# Plan Validation

<!-- Phase 2 of the Rama skill: the check of the plan. -->

**What this paper is.** The check of `door/PLAN.md`, the design of the door, against its contract
(`src/proposal/backend-on-rama-2026-10-02/BUILD-door.md`), the implied spec with its rulings
(`door/IMPLICIT_SPEC.md`), the eight rulings on the plan, and the module as it is built.
**Who wrote it.** One fresh session (Opus 5.5), 9 October 2026, with no stake in the plan, for the
build session of the door to judge. It wrote this file, fixed `door/PLAN.md` where a fail was local,
and wrote three probe files and their outputs outside the repository. It changed nothing else and
committed nothing.
**The verdict: minor-fail.** Fifteen fails, each fixed in `PLAN.md` by an edit of a few lines; the
edits are listed under "The fails" and each is marked in the plan with its number, "F1" to "F15".
One more fail is not fixed, because its fix needs a ruling: point 1. Ten points go to the build
session. No fail asks for another design: the stock, the seat, the life of a request and the steps of
a send hold under each trace below.

Line numbers of `PLAN.md` are those of the file as this check leaves it. `C` is the contract, `IS` the
implied spec, `P7` the ruling on that point, "ruling 6" one of the eight rulings on the plan.

## How the skill's template is adapted

The template is written for a module. The door declares no PState, no depot and no topology. Each
category is walked, or says in a line why it does not apply.

| The template's category | Here |
|---|---|
| Query topology: reads total and meaningful | The door declares none. Walked for the three queries that it calls, for each thing that the plan passes them: "The template's checks", 1. |
| PState schemas | The door declares none. The analogue is the stock, the door's one memory: what it holds, its key, its bound. Walked as hard: 2. The checks of schema types (Object, fixed keys, records) have no analogue: the stock is a value in the memory of one process and is never serialised. |
| Partitioning | The door places no data. The one choice of a place that it makes is the depot of an append, which the act-id names. Walked: 3. |
| Partitioning efficiency | What each call costs the cluster in round trips, seeks and rows, at 1, 16 and 128 tasks. Every number recounted: 4, and "The cost, recounted". |
| Topologies | Which depot and which ack level. Walked: 5. The checks of what a stream topology handles are the module's and were made in its build. |
| Production readiness | Many makers at once; a restart of the process; an append or a query that throws; a thread that is interrupted. Walked with interleavings written out: "Concurrency" and "Failure". |
| Internal depot usage; cross-topology correctness; stream topology correctness | Do not apply: the door declares no depot and no topology, and appends only from client code. |
| In-memory state efficiency | The stocks, in the runtime's process. Walked: 6. |
| Minimality | Each mechanism of the door: delete it, merge it. Walked: 7. |
| Throughput | Each frequent call against a cheaper one. Walked: 8. |
| Spec coverage | Each clause of the contract, each ruling, each row and each cell that the brief names. Walked: "Spec coverage". |

## What was read, and what was run

**Read whole.** The skill's `SKILL.md`, `phase-2-plan-validate.md`, `artifact-plan-validation.md`; the
contract; `src-store/CLAUDE.md`; `src-store/README.md`; the implied spec with its rulings;
`DECOMPOSITION.json`; `PLAN.md` with its rulings; `act.clj`; `door/declaration.clj`, `seal.clj`,
`text.clj`, `digest.clj`; `module.clj`; `module/door_reads.clj`; `record/rows.clj`;
`gate/first_act.clj`; `gate/decision.clj`; `door/README.md`, `module/README.md`,
`src-store/softland/README.md`, `test-store/README.md`, `test-store/softland/README.md`.

**Read in part, for the lines that a check needed.** The docstrings of the two gates
(`streaming_gate.clj:1-125`, `microbatch_gate.clj:1-130`); `gate/batch.clj:1-130` and `405-564`, for
what the microbatch gate does with the locks that an act names; the hand,
`module_proof.clj:1-30` and `96-385`; the store's map, `README.md:1-36`, `158-210`, `288-300`,
`370-412`, `488-506`, `548-564`; the proofs' README, `1-12`, `52-70`, `141-151`, `276-312`;
`pure_proof.clj:1-8`, `30-36`, `286-311`; rules 5, 11, 15, 16, 20 and 21 of `BUILD-store-module.md`;
Rama's documentation, `14-depots.md:204-216`, `11-stream-topologies.md:295-299`,
`13-query-topologies.md:338-346`; the skill's `foreign-client.md:236-250`,
`query-topologies.md:116-128`, `testing.md:34-40`; the plan's probes, `src/probe/cluster.clj:60-228`
and `slow.clj`, and each `FOUND` line of their outputs.

**Searched.** `src-store/` and `test-store/` for "not built", "door proper", "by hand", "the guard",
"three proofs", "two proofs", "plays", "as a door", "comparison", and each line that holds "door"
outside a namespace's name. Rama's documentation and the skill's client pages for "thread-safe" and
the like. The code of the rig, the server and Inland, by name only and never read, for each name that
the plan makes: none is there but `sealed-length` as a key of a bench's map, and `stock!` (point 7).

**Not read.** `gate/stamp.clj`, `record/tables.clj`, the dataflow of the two gates below their
docstrings, `gate/batch.clj:130-405` and `564-860`, the checks of the module's proof below line 385,
`act_proof.clj`, `decisions/OPEN-QUESTIONS.md`, `decisions/groups/g-door.txt` but for its lines on
the stock, the papers of the module's build. No `env.clj` and no `.env` file.

**Run.** Three things, each under
`/mnt/data/projects/research/softland/door-build-2026-10-09/plan-check-probes/`.

1. `python3 -I sums.py > out-sums.txt`: the plan's arithmetic, summed again. The lines are quoted
   under "The cost, recounted".
2. `clojure -Sdeps '{:aliases {:probe {:extra-paths ["<that directory>/src"]}}}' -M:store-module:probe -m check.interrupt`,
   from the repository root, twice (`out-interrupt.txt`, `out-interrupt-2.txt`): what a call of
   Rama's client does on a thread that is interrupted. The second run printed:
   - "append :ack on a thread whose interrupt mark is set: [0 :threw \"java.lang.InterruptedException: \" false] | answer read after, on a clean thread: nil"
   - "the same act-id, read 3 s later: {:verdict :accepted, :reason nil} | records of the streaming depot's log that hold this act-id: 1"
   - "append :append-ack to the microbatch depot on a thread whose mark is set: [0 :threw \"java.lang.InterruptedException: \" false]"
   - "answer-by-act-id on a thread whose mark is set: [0 :threw \"java.lang.InterruptedException: \" false]"
   - "a query that sleeps 3,000 ms, its caller interrupted after 400 ms: [400 :threw \"java.lang.InterruptedException: \" false]"
   - "an append on the same handle after those, on a clean thread: [7 :ok {} false] | answer: {:verdict :accepted, :reason nil}"

   The four parts of each result are: milliseconds, `:ok` or `:threw`, the value or the throw, and
   the thread's interrupt mark after the call. One run on the in-process cluster, Rama 1.6.0. What
   it shows: a call of Rama's client on an interrupted thread throws at once and clears the mark;
   and an append that threw so **went through all the same**. F7 and F8 rest on it.
3. `git status --short` after the runs: only `src/app/server/README.md` modified and the two
   untracked paths of `docs/`, as before, until this paper and the fixes of the plan.

The three existing proofs were not run by this session.

## The fails

Each has the clause, word for word; one scenario with values; the lines of `PLAN.md`; whether the
mechanism holds; and the fix that this check made. The most serious first.

### F7. The door's proof: two branches have no case, one case passes for a wrong door, and two are not as worded

**The clauses.** C part 7: "Its own cases: each of the door's own words; the not-mine answer; that a
lock never crosses a who; that no lock's bytes are ever appended". P13: "A real throw of Rama's
client is wanted for the word of P1". Ruling 6: "Rama's own words make 'it went in' the common case
of a thrown `:ack` append". The brief of this check: "A case that would pass for a wrong door is a
gap."

**(i) `:append-cleanly`.** The plan's case (1416-1419) sends an act that holds a plain
Java object; Rama's client throws before anything leaves the process; "`answer-of` with that pair
gives 'nothing yet'". Take a door that puts a wrong digest in the word, the digest of the maker's
act before the seal. The case reads "nothing yet" with any digest, because no answer is there. It
passes. The plan also said (1464-1466, before the fix) that the proof does not reach "an append that
threw and went through all the same". *Ran*, this check's probe: an append on a thread whose
interrupt mark is set throws `InterruptedException` in 0 ms, and 3 s later the log holds the record
and the answer is accepted. So the case that P1 is for can be made on the in-process cluster with
no fault of the cluster.
**(ii) A request in doubt that went in.** The plan's one case of a request in doubt is a request
that never left the process (a `:session` that is a plain Java object): the next call waits and
ends in a word. The branch that ruling 6 keeps the request for, "If the request went in, its locks
are used, and no second request is sent" (PLAN 525-526), has no case. A door that never read the
answer of a request in doubt would pass.
**(iii) The short read of ruling 8.** The word `:read-its-requests-locks` has a case for a read
that threw. The other branch, "the locks that can seal go into the stock, the request is forgotten,
and the call ends in the word; the next call takes or asks anew" (ruling 8), has none on the
cluster. It is reached with no fault of a lock: an earlier call's request for 4, and a later act
that needs 10.
**(iv) The bytes of a lock of the base that seals a value.** PLAN 1340-1341 has the proof read
"the locks of a request, with its own handle of `locks-of-request` [...] to hold every lock's bytes
against every record of both logs". *Source*: "In a microbatch layer a lock that seals a value is
given with its way and with nil for its bytes" (`door_reads.clj:20-23`). So for each lock of the
base that an accepted act used, that read gives no bytes, and group 13 would hold nothing against
the log for exactly the locks that sealed.
**(v) The count of requests under many threads.** PLAN group 11 said that the requests in the log
"are as many as the size rule gives for the values written". With 16 threads and acts of 1 to 3
values there is no such number: a request's count is `max(4, given out)`, and what the stock has
given out when a call comes short differs by 0 to 2 from run to run (third request: 6, 7 or 8). A
check for one number fails for a right door.

**The fix** (PLAN 1394-1400, 1412-1416, 1419-1425, 1437-1441, 1449-1452, 1464-1467). A second case
of `:append-cleanly`, on a thread that the proof interrupts, where the log holds the record, its
digest is the word's, and `answer-of` gives accepted. A case of a request in doubt that went
through, by a stand-in that sets the interrupt mark at the request's append and then calls Rama.
A case of the short read. The bytes of a base lock that seals, from the lock's whole row. The
requests under many threads held to the rule one by one. The sentence on what the proof does not
reach now says which thrown append it reaches.

### F1. How far the door's bound reaches

**The clauses.** P8: "The door's wait for its own request has a bound of the door's own, a number
with its reason. When it passes: a word of the door's". IS O3, Concurrency: "a maker never waits
without a bound on a request that is not its own". `src-store/CLAUDE.md`: "When you claim a retry,
an ack or a transaction, say how far the claim reaches."
**The scenario.** The base, a stock that is empty, the bound 10,000 ms. Maker M1 comes short at
t = 0 and sits in the seat. It appends its request (5 ms), and reads the answer at 200, 400, and so
on. At t = 9,800 it starts a read that the cluster does not answer. *Ran*, the plan's probe 2:
"slow query, 120 s: returned after 70002 ms :threw [...] QueryTopologyInvokeFailed". M1's call comes
back at about t = 79,800, with `:read-its-requests-answer`. Maker M2 came short at t = 100 and
stands for the seat; its wait ends at t = 10,100 with `:get-locks-within-its-wait`.
**The plan.** "Waiting for the seat, reading an earlier call's open request, and waiting for its own
request's answer all end at that one moment" (467-471); "So the door holds a maker for locks no
longer than one append can hold it anyway" (489-490, before the fix); "A maker never waits past the
door's bound on a request that is not its own" (1241, before the fix). True of M2. Not true of M1,
and not true of a call in the seat that reads an earlier call's request. IS O3 still holds: the
wait has a bound, Rama's.
**The fix.** A paragraph, "How far the door's bound reaches" (473-480), as the plan has one for the
maker's bound; and the three sentences now say which call can outlast the bound, and by what.

### F8. A thread that is interrupted inside a call of Rama's client

**The clause.** The build session, at the end of its rulings: "that a seat is given up on every way
out of a call". PLAN 1242: "A thread is interrupted while it waits: The wait ends as if its time had
passed, and the thread's mark is set again."
**The scenario.** A maker's thread is interrupted while its call is in the seat and inside the
append of its request. *Ran*: the call of Rama's client throws `InterruptedException` at once, the
thread's mark is cleared, and the append goes through. The plan said nothing of this way out. As
the steps are written the stock is right: the request was written as in doubt before the append
(499), the seat is given up, the word is `:append-its-request-cleanly`, and the next call reads the
request's answer and uses its locks. What was not said: that the mark is gone, where the plan's own
rule for a wait is that it is set again.
**The fix.** One sentence in that row (1242), with the probe's line. It has the door set the mark
again, by the plan's own rule for a wait; that is point 10, for the build session to strike.

### F13. The taking of an empty stock out of the map

**The clause.** The build session: "what holds between a take, a call that counts itself into a stock
and out of it, and the taking of an empty stock out of the map". IS O3.6 and P7: a lock is lost only
by a restart, a refused act or a thrown append.
**The interleaving.** Stock S of `[L :streaming W]`, no run, nothing given out. Call X's request was
refused; X has forgotten it. (1) X reads "S holds nothing": true. (2) Call Y counts itself in; the
count is 2. (3) Y comes short, sits, asks for 4, reads them, takes 1: S holds 3 locks, 1 given out.
(4) Y counts itself out: the count is 1; S stays. (5) X counts itself out: the count is 0. If X now
acts on what it read at (1), it takes S out of the map. Three locks are rows that seal nothing, no
word says so, and `stock-count` no longer counts them. If "holds nothing" is read inside the step
that brings the count to 0, it is read after (3) and is false; and when the count is 0 no call can
change the stock, so the reading cannot be overtaken.
**The plan.** "A call counts itself into its stock when it gets it and out when it leaves, each in
one atomic step of the map, and a stock goes only when no call is counted in" (1152-1154). It did
not say where "holds nothing" is read.
**The fix.** A clause (1154-1158): it is read inside that step, after the count, with the reason.

### F2. "One more whole wait" for a request in doubt

**The clause.** Ruling 6: "A request whose append threw is kept as in doubt and waited for once
more". IS O3, Data growth: the plan "says what bounds the others".
**The scenario.** A streaming layer, the bound 10,000 ms. M1 sits at t = 0; its request's append
hangs and throws at t = 10,000 (the plan's probe 2: an append to a module that was gone threw after
10,000 ms). M2 came short at t = 100 and stood for the seat; its wait ends at 10,100, and it gets
the seat at 10,000 with 100 ms left. It reads the answer once, perhaps twice, finds nothing, and
forgets the request (504: "until its own T [...] No answer by then: the request is forgotten"). If
the request went in, its locks are rows that seal nothing, and M3 sends a second request.
**The plan.** The steps (504) are as ruled. The claim was wider than the steps: the only request
given up is one "that had no answer through one more whole wait" (1197-1199, before the fix).
**The fix.** The sentence now says what the wait is: what is left of the next call's bound after it
stood for the seat, one read at least (1197-1201). What would make it a whole wait is point 4.

### F3. The bound on the unused locks of a stock

**The clause.** C part 3: "the plan says how small a stock keeps that cost".
**(i)** PLAN 389, before the fix: "what a restart loses for a who is less than what that who
wrote through this door". A who that wrote 1 value: the request asked for 4, 3 are left, and 3 is
more than 1. The plan's own table says "1 request; 3 left" (377), and its later section says
"fewer than 4 for a who that wrote up to 4 values" (1190).
**(ii)** "The stock holds fewer unused locks than its last request asked for" (387, in bold). After
the short read of ruling 8 it does not: the stock holds 3, an earlier call's request for 4 is read,
the act needs 10; the stock now holds 7, and the last request asked for 4. *Ran*, `out-sums.txt`:
"ruling 8, short branch: stock 3 + earlier request 4 = 7 < need 10 -> unused 7 >= last request 4 :
True". In every other state the sentence holds: 2,000 runs of 200 acts of 1 to 5 values gave no
run where it failed. What does hold in each state: fewer than the act that came short needed, so
255 at most.
**The fix.** Both sentences (389-394, 1192-1193).

### F4. Four wrong numbers

**The clause.** The brief: "A wrong number is a fail of that check."
1. **The seeks at the border of two runs** (562-566). The plan: "Over three runs (a) costs the gate
   3 seeks more and (b) costs it 2." Acts of 3 values, requests of 64: act 22 takes locks 64 to 66
   and act 43 takes 127 to 129, each across a border; act 64 ends at 192, on the border. So (a)
   costs 2, and (b) costs 2 in its one act of three requests. *Ran*: "(a) oldest first: acts that
   name two requests in 64 acts: 2". With 3 and 2 the plan's own next sentence, "the gate would not
   read less" (574), and its point 4, "no fewer seeks over time", were against its numbers. With 2
   and 2 they hold, and so does ruling 4.
2. **The queries a second of the wait** (868-873, 1066). The plan's column was 50,000, 5,000, 2,000
   and "about 4,000". By the plan's own first column, at 1,000 acts a second each answer costs 46,
   6, 3 or 6 reads: 46,000, 6,000, 3,000 and 6,000 queries a second. The plan held 1,000 waits at
   once for each schedule and left out the read at the start; under the schedule that doubles a
   wait lasts 1,550 ms, so 1,550 run at once. The pick of 200 ms stands: it reads a seventh of what
   the hand reads, and the doubling schedule reads the same 6.
3. **The base's requests on the in-process cluster** (421-422, 483-484, 1723-1724). The plan: "752
   to 1,034 ms [...] six requests". The probe's output has five requests, at 752, 914, 979, 911 and
   929 ms, and one making act at 1,034 ms. Ruling 1's own text has it right, "752 to 979 ms".
4. **"About 0.1 read an act"** (1039-1040). 2 reads for each 64 values are 0.03 read an act.

Every other number was summed again and holds: "The cost, recounted".

### F6. Three sentences that the door makes untrue are not in the table, and one line of the proofs' README

**The clause.** P15: "Each sentence that the door makes untrue is changed in the commit that makes it
untrue, in `src-store/` and in `test-store/`."
1. `src-store/softland/store/README.md:1` and `:5-9`: "# Store — the act, and the store's module";
   "This folder holds two things. The act [...] And the store's Rama module". With `door.clj` in
   the folder it holds three. The table had the lines 17-18 and 20-34 of that file and not these.
2. `test-store/softland/store/README.md:59`: "the use of the act by a gate: the two proofs below
   show it." With the door's proof there are three below.
3. `test-store/softland/store/pure_proof.clj:290` and `:308`: "the door's comparison", of
   `is-digest-of?`. Row 12 of the plan's table makes `digest.clj` say that this is "the comparison
   of a sender that still holds its thing", since the door compares two hex texts and holds no
   thing. The proof's two lines then say the old thing.
4. PLAN 1545-1548 named the door's proof as the second exception to D8 for what it reads. It also
   appends: for "what the door sends for an act is what the hand sends", the hand's steps make a
   request and an act (1331-1334). `src-store/CLAUDE.md`: the map of a check says "what state it
   changes outside the code that it checks".
**The fix.** Rows 23, 24 and 25 of the table (1577-1579); the line of the README (1545-1549); the
count in the table of rulings (25 rows).

The 22 rows that the table had were each opened: "The explanations".

### F9. The contract's clause on commits is nowhere in the plan

**The clause.** C "Commits and the receipt": "Commit on `main`, grouped by concern (the door; its
proof; the maps), each message saying what and why. No push. No `Co-Authored-By`, no
`Claude-Session`, no attribution line of any kind."
**The plan.** "What the build hands to the receipt" listed the receipt and named the commits only as
"the commit hashes". No line carried the rule, and none met its meeting with P15 (point 6).
**The fix.** Item 0 of that section (1620-1624), which carries the clause and names the point.

### F10. Where the test of a plain map comes from

**The clause.** P6: "No rule is written a second time." P2: "It tests what its own addition needs:
the thing is a plain map".
**The plan.** "They are the two functions of which `declaration/stray-reason` is made" (725, before
the fix), of `usable-id?` and `act-id-in-layer?`. *Source*: `stray-reason` calls neither. It calls a
private `plain-map?`, `act/as-shape`, `id-within-the-limit?` and two comparisons
(`declaration.clj:295-298`, `477-487`). The two public functions are made of the same tests but
for the first. The plan's table of what each namespace requires (139) names no home for the test of
a plain map, which step 1 and the walk of the facts both need, and 147 says "No rule is written a
second time."
**The fix.** The sentence is made true to the source (725-729). The choice is not this check's:
point 2.

### F11. A stock that holds an open request stays

**The clause.** IS O1, Data growth: "what bounds it is the plan's to say".
**The scenario.** A maker sends value acts under a new who each time, and each request's append
throws, or is acked in the base and not answered within the bound. Each stock holds an open request,
so it is not "a stock that holds nothing at all", and no later call of that who comes to read or
forget it. Each is about 550 bytes for the life of the process. A `:who` that is a Java object is
equal to no other key, so that is one stock for each call.
**The plan.** "So a maker that sends value acts under whos that do not stand grows no memory in the
door" (1150-1151) is true of a refused request. The plan did not say the other case, and its line
for the build session says the memory is "about 300 bytes for each who that wrote a value" (1772).
**The fix.** Two sentences (1158-1162).

### F12. What does not come back, beside facts with no end

**The clause.** P9: "the docstring says what it can cost: facts with no end".
**The scenario.** An act whose fact has `:e (range)`, or a part `:note (range)` that the form does
not know. Step 7 seals to the place and takes the digest. *Source*: "A sequence with no end does not
come back from it" (`text.clj:61-62`). The call does not come back, and in the first case after the
locks were taken. And `act-ids-for` with a count of 10 thousand million: the door makes that many
ids in one vector, and the JVM ends the call, where the plan says "No call throws for what a maker
gave" (154).
**The fix.** Two sentences (780-783).

### F5. One state of the answer's query has no example

**The clause.** The template: "Example N total reads, M meaningful".
**The plan.** For a microbatch act-id it gave "on its depot and not decided: 1 read [...] This is
each read of a wait but the last" (679-680). *Source*: when the row is there and its batch is not
closed, the query reads the row and `:closed-through` and gives nil (`door_reads.clj:97-107`). So a
read of a wait that falls in that state costs 2. The state is short: "a batch is closed one short
run after it" (`microbatch_gate.clj:92-93`).
**The fix.** The example (681-684), with what it does to the 9 seeks of kind D.

### F14. A word that a ruling made is not listed for Sid

**The clause.** C D7: "Every word the door makes is plain, says what the thing is, and is listed for
Sid in the door's README under 'Words a session made, Sid's to change'."
**The plan.** Ruling 5 gives `seal.clj` the function `sealed-length`. The list (1652-1715) did not
have it. **The fix.** One row (1695).

### F15. The table of costs has no row for an act-id that the maker gave

**The clause.** The template: "Does it include every category of input as its own data-category row".
**The plan.** Kinds A to F each count the read of the layer's depot. An act whose act-id the maker
gave, or took from `act-ids-for`, is another kind, and the plan itself names it as the cheaper way
(1049-1051). **The fix.** One sentence (987-989): the table takes the dearer case for each act, so
its sums are the most that the mix costs.

## Points for the build session

Each is a ruling that is wrong on a fact or does not hold in one case, two requirements that meet, or
a thing that this check may not settle. None is fixed against a ruling.

**1. The ruling on P3, and rule A, for a fact under a store key that carries a value and a seal.**
P3: "By A: [...] a fact that carries a value beside `:sealed` or `:lock-id`, are things to which the
door could not add its part." Rule A: "under rule A the door tests nothing that a gate can see and
judge." C part 4: "a fact under a store key keeps its plain value". Take
`{:e :l :k :softland.store/depot :v :streaming :sealed <bytes>}`. The door adds no part to a fact
under a store key, so it writes over nothing; and a gate sees this fact as given and refuses it
`:store-key-sealed` (`declaration.clj:377-380`). By P3's letter the door stops it with
`:add-its-parts`; by rule A it goes as given. The plan's table of the walk has both rows and no
order between them (749, 750). **This is the one fail that is not fixed.** The choices: (a) the row
of a store key comes first, and the fact goes as given; (b) P3's letter. The fix is one clause in
row 749 or in row 750.

**2. The ruling on P6, "No rule is written a second time", and the test of a plain map.** The test
is `(and (map? x) (not (record? x)))`. It is private in `declaration.clj:295-298`, private in
`gate/batch.clj:80-83`, and written in line in `act.clj:332`. The door needs it for the thing (P2)
and for each fact (rule B: "a fact that is a map"; P3: "a map and no plain map"). No public
function gives it for a fact. The choices: (a) the door writes the one line a fourth time, and the
pure proof's case "`stray-reason` gives nil for each thing that passes" holds it for the thing; the
plan then says so where it says that no rule is written twice (147); (b) `act.clj`, which is none of
the three closed files, gains a public `plain-map?`, as `seal.clj` gains `sealed-length` by ruling
5; (c) the door asks `declaration/stray-reason` for the thing, and still needs (a) or (b) for a fact.

**3. Ruling 8 sends a sound act away where the row beside it goes on.** Ruling 8: "the call ends in
the word; the next call takes or asks anew." The ruling's own case is "a who whose acts all need
more than that request gave". Walk it: a first call (1 value) asks for 4 and ends in
`:read-its-requests-answer`; a second call (10 values) reads that request, finds 4 locks that can
all seal, and ends in `:read-its-requests-locks`, with 9.9 s of its bound left and nothing wrong
with the cluster; a third call asks for 10. In D4 the same second call, meeting an earlier call's
request that was *refused*, "goes to D0 with its own" (503). The two rows differ and the plan gives
no reason. C D1: the door's words "name only what the door could not do"; here it could. The
choices: (a) as ruled; (b) when the request was an earlier call's and each lock that it gave can
seal, the call goes on to D0 inside its own wait, and the word is kept for locks that cannot seal.
The proof's new case for this branch (F7) is written to (a).

**4. Ruling 6: what "once more" is.** As F2 shows, the next call gives a request in doubt what is
left of its own bound, which can be one read. A rule by the request's age would give the whole
bound: the stock keeps the moment of the thrown append, and a call forgets the request only when a
read finds nothing at or after that moment plus the door's bound; a call whose own bound passes
sooner ends in `:get-locks-within-its-wait` and leaves the request. It reads a clock, which the
wait reads already. One finding for the ruling, *ran*: an append that threw on an interrupted
thread went through; so "it went in" has a case that needs no fault of the cluster, and keeping the
request saved its locks.

**5. The state *accepted* of an open request is a remembered answer.** The project's rule: a
remembered copy of what the cluster holds is not designed in. IS O1.3 makes "the act-id of a request
that it sent and whose locks it has not read" part of the stock's memory. The plan keeps one thing
more: that the door read "accepted" for it (298, 501), so that after a thrown read of locks "the
next call in the seat reads the locks with one round trip". It cannot go stale: "an answer [...]
that a read gave never changes" (`door_reads.clj:44-45`). This check passes the rule on caching,
because the open request is inside the stock, and names the state because it is the one thing in
the stock that is a read's result and no lock. The choices: (a) as planned; (b) two states, and the
next call reads the answer again and then the locks: one round trip more, on a path that follows a
thrown read, and C part 3's order in each call.

**6. The contract's three groups of commits, and P15.** C: "grouped by concern (the door; its proof;
the maps)". P15: each sentence "is changed in the commit that makes it untrue". `src-store/CLAUDE.md`:
"An explanation changes in the same commit as the code that it explains." The commit that adds
`door.clj` makes sentences untrue in the fifteen files of the plan's table: maps, docstrings and
proofs. If those go
in that commit, a third commit of "the maps" has little left; if they wait for it, P15 is not met
for one commit. The choices: (a) two commits, the door with every sentence that it makes untrue and
its proof with the proofs' maps, and the receipt says that the contract's third group fell into
them; (b) the contract's three, and P15 read of the build as a whole.

**7. "Stock" is a name in the rig's code.** C D7: "No word comes from the rig's code." *Source*:
`src/proposal/rig-2026-09-25/src/rig/store/client.clj:358`, `(defn stock!`, found by a search for
the word; the file was not read. The word is the contract's (part 1: "a stock of locks") and the
door's sheet's (`decisions/groups/g-door.txt:101`), so the plan did not take it from the rig. The
plan makes two names of it: `softland.store.door.stock` and `stock-count`. For the list of words
that are Sid's to change, with that line. One more meeting of names, of no weight: `:sealed-length`
is a key of a map in one bench of the rig (`test/rig/bench/lock_growth.clj:535`); ruling 5's
`sealed-length` is a function, of another thing.

**8. One send in 64 asks, and the aim for one person is the slowest 1 in 100.** Rule 21 of
`BUILD-store-module.md`: "at least 100 acts each second for one person, with the slowest 1 in 100
at 20 ms". With the most at once at 64, 1 send in 64 of a who that goes on writing meets a request:
1.56%, three round trips more (14.9, 1.9 and 12.8 ms in the plan's probe). So the slowest 1 in 100
of that who's sends is a send that asks. At 128 it is 0.78%. The plan weighs 64 against 256 by the
gate's events and the rows that a restart leaves (414-418), not by this. No fail: the implied spec
says "No requirement here turns an aim into a number that the door must meet", and the store's map
measures the aim "from the door's append", after the request. For the receipt's line on the three
numbers, or a ruling.

**9. The first bound of P9 is read from two limits.** P9: "no more locks than the declaration lets
one act hold facts". The plan: "the smaller of the declaration's `max-facts` and
`max-locks-in-request`, both 256 today" (426-427). They are the same number today. If
`max-locks-in-request` were made less, the plan's door would stop an act that a gate would accept
with the locks of two requests. The plan's reason is that one request can then always cover one
act, which the step "this call's own locks are taken" rests on.

**10. The thread's mark after a throw of Rama's client.** F8's sentence has the door set the mark
again when what Rama's client threw is an `InterruptedException`. The plan's rule for a wait says
so; for a call of the client it said nothing. To leave the mark cleared is the other answer: the
word still names the class.

**Nothing that is Sid's is decided in the plan.** Each line of C "Not in this build, by decision"
was held against it: "Spec coverage", 4.

## The template's checks, as adapted

### 1. The three queries that the door calls

- **`answer-by-act-id`.** Input examples present: yes (PLAN 675-687).
  - A streaming act-id after its acked append: N = 1, M = 1 (`door_reads.clj:92-95`).
  - A microbatch act-id whose batch is closed: N = 2, M = 2 (`:97-103`).
  - A microbatch act-id that is not decided: N = 1, M = 0. By the template's letter a read that
    finds nothing is a fail. Here it is each read of a wait but the last. C D5 allows "No
    subscription, no bell", and the three queries are the whole surface, so no design of the door
    takes these reads away; the plan makes them few, 6 for an answer at 0.9 s against the hand's 46.
    Pass, on that reasoning; the interval is accepted in the rulings.
  - A microbatch act-id that is decided in a batch that is not closed: N = 2, M = 2, and nil. The
    plan did not have it: F5.
  - A thing that fails the head's test: no read, and through the door no call (PLAN 238-239;
    `rows.clj:323-332`).
  - M is 1 or 2 by the depot slot, and the plan says so (686-687).
- **`locks-of-request`.** A streaming request for 64: 1 seek and 64 rows, each a lock that the door
  keeps (`door_reads.clj:118-124`). A microbatch request for 256: the request's row,
  `:closed-through`, then 1 seek and 256 rows (`:126-139`). The door calls it only after it read
  "accepted" (PLAN 501), and asks for the indexes 0 to n - 1 and no more (242-245). N = M.
- **`depot-of-layer`.** A made streaming layer: 1 and 1. The made base: 2 and 2 (`:156-165`). A layer
  that is not made: 1 and 0; C part 2 names this read, and it is what tells "not made".

### 2. The stock, in the place of the PState schemas

- **One structure.** One map from `[layer depot who]` to a stock (PLAN 285). No second memory beside
  it: no act-id, no digest of a maker's thing, no layer's depot (303-307). Pass.
- **What it holds** (288-301): runs, how many locks it gave out, at most one open request, a seat.
  The count of calls that F13 is about is the map's, not the stock's (1152).
- **Its key.** Scenario: who W holds 5 locks in `[L :streaming W]`. A maker gives W's act with one
  value and the act-id `[L :microbatch U]`, which has the shape and is of the layer (P2). The plan's
  key is `[L :microbatch W]`: empty, so the request goes to the microbatch depot as
  `[L :microbatch U2]`, and that gate refuses it, `:layer-belongs-to-the-other-gate`
  (`decision.clj:146-147`); the word is `:have-its-request-accepted`, and the 5 locks are where they
  were. With the key `[L W]` one of the 5 would be taken, the act refused, the lock lost. The plan's
  key holds (313-344), and it is the one thing that a gate holds a lock to
  (`decision.clj:266-272`).
- **Collections that can pass 100.** A run holds up to 256 locks. It is taken from its front in the
  order of the indexes; nothing walks it.
- **Its bound.** F3.

### 3. Partitioning

The door places nothing. An append goes to the depot that the second slot of the act-id names (PLAN
601-609; `module.clj:73-74`), and the two partitioners run on the client (`module.clj:57-58`). The
streaming depot's reads `:layer`, which step 1 of a send has read before it (PLAN 615-617). Pass.

### 4. What each call costs the cluster, in the place of partitioning efficiency

- The table is there for 1, 16 and 128 tasks (996-1030). The proportions sum to 1; *ran*:
  "proportions sum: 1.0 | weighted seeks: 2.9195 | weighted iterations: 0.928". The plan's 2.92 and
  0.93 hold.
- A row for each kind of act: but for an act-id that the maker gave. F15.
- The seeks are totals over all tasks: each query begins with the hash partitioner on its first
  argument and goes to one task (`door_reads.clj:54-58`; skill `query-topologies.md:120-126`), so no
  read fans out and the sums are flat from 1 task to 128. Pass.
- No number of the door's grows with the cluster.

### 5. The depot and the ack level, in the place of the choice of topology

`:ack` to the streaming depot is the contract's (C part 4); `:append-ack` to the microbatch depot is
P17. *Rama's documentation*: "If there are no colocated stream topologies, `AckLevel.ACK` is
equivalent to `AckLevel.APPEND_ACK`" (`14-depots.md:206`), read at the line. The two levels that the
plan weighed and dropped (643-648) are dropped on reads and on a lost act, not on what a proof can
wait for. Pass.

### 6. The memory of the door's process

- Per lock: the array of 32 bytes as the read gave it. The plan built the flat answer, one array for
  a run, with its numbers: 16 bytes less for each lock, and one copy of a lock's bytes at each seal
  (1216-1225). The door's code runs on a maker's thread in the runtime's process and on no task of
  the module, so the template's reason, a pause of the collector on a task's thread, does not reach
  it. Pass.
- One fact beside it, which changes no check: `seal.clj:114` makes a `SecretKeySpec` of the lock's
  bytes, and the JVM's class copies them. "The door makes no copy of a lock's bytes" (PLAN 587-588)
  is true of the door's code.
- Could the stock be fetched at need? Built and dropped on one round trip for each act with a value
  (1120-1126). The loss of a stock is the contract's: "a row that seals nothing, for good" (C part
  3). Pass.
- The sums: *ran*: "100,000 whos x (300 + 20 x 60) bytes = 150.0 MB"; "63 x 60 + 300 = 4080 bytes;
  255 x 60 + 300 = 15600 bytes". The plan's 150 MB, 4 KB and 16 KB hold. What stays in the map: F11.

### 7. Minimality

**The simplest door that could meet the contract.** One namespace. One stock for each layer and who,
under one mutual exclusion. A call that finds it short sends a request for a fixed 64, reads the
answer and the locks, and forgets the request when anything fails. `answer-of` reads in a loop with
a sleep. One word for each thing that failed.

Each thing that the plan has and this sketch has not:

| The mechanism | Delete it: what is lost | Merge it |
|---|---|---|
| Three pure namespaces under `door/` | The pure proof cannot hold the door's rules; each of the 18 rows is then shown only on a cluster. The contract leaves it: "may be split into a pure namespace so that the pure proof can hold them; builder's choice." Kept. | Into `door.clj`: the same loss. |
| `act-ids-for` | A series of 20 ids costs 20 reads of one row on the layer's task, where one call costs 1 (IS O2, Throughput: "What a series costs the layer's task in reads is the plan's to say"). Kept: I/O. | Into `act-id-for` with a count: the same call by another name. |
| `stock-count` | The count that group 14 of the proof holds against the rows. The proof could derive it from the log, so no required property is lost. P22 says "The door may say how many locks it holds [...] that is the plan's", and the rulings accept it. Not re-opened. | |
| The depot in the stock's key | A lock of one depot's record is taken for an act that goes to the other, and lost (check 2 above). Kept. | |
| The size rule by what was given out | With a fixed 64, a who that wrote 1 value leaves 63 rows at a restart; C part 3: "the plan says how small a stock keeps that cost". *Ran*: the table's nine cells. Kept. | |
| The seat | In the base a request takes about a second; each maker that comes short would send its own, 100 requests for 100 acts in that second, and the stock has no bound (PLAN 440-444). Kept. | Into one mutual exclusion round every use: a take then waits behind a call that asks (449-451). |
| The open request, *sent* | P8: "The door remembers the request and reads its locks at a later need." Required. | |
| The open request, *in doubt* | Ruling 6. *Ran*: a thrown append that went through. Kept. | |
| The open request, *accepted* | One round trip, after a thrown read of locks. Point 5. | Into *sent*. |
| The count of calls, and the taking out of an empty stock | The map grows by one stock for each who whose request was refused, about 300 bytes, with no end, for a maker that sends under whos that do not stand (IS O1, Data growth). Kept; F13 and F11 are about it. | |
| The first read of an own request in the base, after one interval | One round trip that finds nothing, for each request of the base. Kept: I/O. | |
| The limit of sealed bytes tested before a lock | m locks lost for each act with one value over the limit (P7). Ruling 5. Kept. | |
| The door as a type that prints nothing | P18: "in nothing that the door prints". Required. | |
| The two words of the ack | IS O4.6: the ack "says which ack it is". Required. | |
| Fourteen words | IS O6.1: each word says what became of the act and of a request. Accepted in the rulings. | |

No mechanism is in the plan that the contract, a ruling, or a cost in reads or rows does not ask
for, but for the state *accepted*, which is point 5.

### 8. Throughput

- **A send in a streaming layer.** 3 round trips with the maker's `answer-of`. The answer's read is
  the contract's (C D4). The read of the layer's depot goes when the maker gives the act-id; to
  take it away for every act is a remembered depot, which ruling 2 sends to its own examination. No
  cheaper door inside the rulings.
- **A request.** 3 round trips; 2 are possible, and ruling 3 keeps the contract's order.
- **A wait.** 6 reads for an answer at 0.9 s. A wait that began with a sleep would save the read at
  the start, as the door does for its own request (PLAN 508-510); `answer-of` cannot know when the
  act was sent, since the door keeps nothing (C part 5). No cheaper wait inside D5.
- **The aim for one person.** Point 8.

## Spec coverage

Each clause is quoted, walked on one scenario, and held against the lines of the plan. "Holds" means
that the lines cited do what the clause asks in that scenario. Where a block says nothing of a
restart, a second run or two makers at once, the call that it traces keeps no state and reads the
record alone; the stock's faults and races are traced once, under "Concurrency" and "Failure".

### 1. The contract's "What to build", sentence by sentence

**Part 1.**

| The sentence | The scenario | PLAN | Holds |
|---|---|---|---|
| "The door is opened on a cluster handle and the module's name, and holds the two depots' handles and the three queries' handles." | `open` on the in-process cluster and the name that `get-module-name` gives. The plan's probe: "open the five handles: 3 ms :ok". On the name `no.such/Module`: "2 ms :threw org.apache.thrift.TException: Module not alive", so the word `:find-the-module` and no door. | 160, 282-283, 919 | Yes |
| "It owns no durable state." | The process ends with 12 unused locks in one stock. A door opened later holds none, and reads none of them: it reads locks "only of requests that it made itself in this process". | 272, 285-286, 1108-1114 | Yes |
| "Its one memory is a stock of locks (part 3), the memory of one process." | After a send returns, what the door holds of it: nothing of the act, its act-id or its digest; of its own request, an act-id, a digest, a count and a state, until its locks are read. | 288-307 | Yes; the state *accepted* is point 5 |
| "In this build the door is the module's only client in the runtime [...] and nothing else in the runtime touches a depot or a query." | Nothing in the plan opens a second client in `src-store/`. The two proofs do, and the proofs' README says so. | 143-147, 1545-1549 | Yes |
| "Where a read's steps run (W3) is open and not decided here." | The wait runs on the maker's thread and is "one question about one act-id". The docstring leaves the room by the row's id. | 654-657, 1529-1532 | Yes |

**Part 2.**

| The sentence | The scenario | PLAN | Holds |
|---|---|---|---|
| "`act-id-for` a layer gives `[layer depot uuid]`: the layer's depot as the module gives it (`depot-of-layer`), or the depot that the layer starts with when it is not made (`first_act.clj`), and a random UUID." | `:notes`, not made: the read gives nil, `depot-at-making` gives `:streaming` (`first_act.clj:49-54`): `[:notes :streaming u]`. `:base`, not made: `[:base :microbatch u]`. `:notes`, made: `:streaming` from the read (`door_reads.clj:167-169`). The read threw: `:read-the-layers-depot` and no id (P21). A layer that is a string: `depot-read` gives nil, the word `:make-an-act-id`, no round trip. | 174-182 | Yes |
| "A maker may ask for the ids of a series before it sends the first, as `act.clj` allows; the door keeps none of them." | 20 ids for `:notes`: one read, 20 uuids. The third names the first in `:because-of` before anything is sent. | 183-191, 303 | Yes |
| "An act-id is made once and never reused." | The uuid is the JVM's random one. A maker that sends twice under one id is not stopped, and reads "not mine" for one digest (P11). | 179-180, 845-849 | Yes, as P11 reads it |

**Part 3.**

| The sentence | The scenario | PLAN | Holds |
|---|---|---|---|
| "it sends a lock-request act whose `:who` and `:permission` are the maker's, whose `:layer` is the act's, and whose one fact is `{:e <the uuid of the request's own act-id> :k :softland.store/lock-request :v n}`" | Alice's act in `:notes`, one value, no lock: the request is `{:act-id [:notes :streaming r] :depot :streaming :version 1 :who :alice :layer :notes :permission [:alice :notes :notes] :session <hers> :facts [{:e r :k :softland.store/lock-request :v 4}]}`. The declaration takes it: the count fits `:lock-count`, is 256 at most, and the fact sits on the uuid of its act (`declaration.clj:395-409`). It is the hand's request part for part (`module_proof.clj:162-171`, `205-208`). | 258, 499 | Yes |
| "it reads the request's answer by act-id and then its locks with `locks-of-request`." | After the ack: `answer-by-act-id [:notes [:notes :streaming r]]` gives accepted with the request's digest; then `locks-of-request [:notes [...] 0 3]` gives 4 locks. | 500-501; ruling 3 | Yes |
| "A lock-id is `[request-act-id index]`." | The run keeps `[:notes :streaming r]` and the next index, 0; the fact gets `:lock-id [[:notes :streaming r] 0]`. | 290-292 | Yes |
| "a lock is used only for values of acts that carry the same `:who` and `:layer` as the request that made it." | Bob, a second holder of `:notes`, sends a value while Alice's stock holds 3: his key is `[:notes :streaming :bob]`, empty; he asks for his own 4. Alice in `:plans`: another key. | 313-344, 592; proof group 10 | Yes |
| "The stock — per layer and who, how far ahead it asks, how many at once (the declaration's limit is the ceiling) — is engineering, with its reason in the docstring." | The rule gives 4, 4, 8, 16, 32, 64, 64 for a who of one-value acts (*ran*). It never gives more than 256. | 402-430, 1536-1537 | Yes |
| "A lock's bytes never go into a depot" | The request holds a count. The thing holds `:sealed` and `:lock-id`. No word holds a lock. | 258, 785-788, 934-938; group 13 | Yes, with F7 (iv) for the proof |
| "A refused request is handed to the maker as the gate's refusal; the door retries nothing on its own." | Carol holds no permission in `:notes` and sends a value: her request is refused `:permission-does-not-stand`; the word `:have-its-request-accepted` carries the request's act-id and that answer, and nothing is appended for her act. No second request is sent in that call. | 503, 926, 531-532 | Yes, as P5 reads the two clauses |
| "A lock that a stock loses at a restart is a row that seals nothing, for good; the door never destroys one (forget is not its work); the plan says how small a stock keeps that cost, and the receipt counts the rows it left." | A who that wrote 9 values holds 7 (requests 4, 4, 8). The restart leaves 7 rows. Group 14 of the proof counts them from the record. | 1187-1215, 599; group 14 | Yes, with F3 |

**Part 4.**

| The sentence | The scenario | PLAN | Holds |
|---|---|---|---|
| "Given a maker's act in its one form, the door reads `:layer` and `:facts` and no more." | The door reads six parts. | 713 | No, by P4; for the receipt (1631) |
| "For each fact that carries a value and is not under a store key, it takes a lock from the stock and seals the value to its place with `seal.clj`, giving `:sealed` bytes and a `:lock-id`" | `{:e :n1 :k :text :v "hello"}` in `:notes`: the plain bytes are the UTF-8 of `"hello"` in its one text, 7 bytes; the sealed bytes are 35; the place is `[:notes :n1 :text]`. | 751, 719, 785-791 | Yes |
| "a fact under a store key keeps its plain value; a retract has nothing to seal." | A making act: two facts under store keys, no lock, the stock not read. `{:e :n1 :k :text}`: as given. | 748, 750, 718 | Yes; one fact has two rows: point 1 |
| "It adds `:act-id` and `:depot`, appends the thing to the depot the act-id names — the streaming depot with the ack level `:ack`, the microbatch depot with the ack the module's docstring describes" | `[:notes :streaming u]`: `*streaming-depot`, `:ack`. `[:base :microbatch u]`: `*microbatch-depot`, `:append-ack` (P17). | 720, 632-635 | Yes |
| "and gives the maker the act-id, the digest of what it sent (`digest.clj`), and the ack as a signal, never as the answer (F7 A)." | `{:act-id [...] :digest "64 hex" :ack :gate-ran}`. The digest is `hex-of` the thing that is appended, sealed bytes and all; a gate takes the digest of the thing as it arrived (`streaming_gate.clj:42`), and the one text is the same after Rama carried it (`text.clj:46-54`). | 721, 793-795, 650-652 | Yes |
| "The door checks nothing of the act's form; a malformed act goes in and comes back as the gate's refusal." | An act with no `:session` and no value: it goes in and is refused `:no-session`. The rows where this cannot hold are ruled (P2, P3): section 6 below. | 735-755 | As ruled; for the receipt |
| "The door never fills a missing part, never reorders facts, never names a permission for the maker, and changes the act only by the three additions its declaration names." | An act with no `:permission` and three facts, the second with a value: the thing has no `:permission`; the facts are a vector of three, the second sealed, each at its place. The request for it has no `:permission` either, and is refused `:no-permission`. | 785-791, 1498 | Yes |
| "When the door cannot do a step — no `:layer`, a layer not made and no depot to start with, a stock empty and the request refused, an append that threw — it tells the maker in the door's own words, which are not a gate's refusal and are kept in no record, and it appends nothing for that act." | The four: `:make-an-act-id`; no such case, by P14; `:have-its-request-accepted`; `:append-cleanly`, of which "appends nothing" cannot be said (P1). A word holds `:could-not` and no `:answer`. | 940-946, 909-913 | As ruled |
| "The words are the builder's, plain, listed for Sid (D7)." | Fourteen, each after "the door could not". | 1652-1715 | Yes, with F14 |

**Part 5.**

| The sentence | The scenario | PLAN | Holds |
|---|---|---|---|
| "`answer-of` an act-id and the digest the door gave on sending reads `answer-by-act-id` and gives one of four: accepted, refused with the gate's reason, nothing yet, or not mine — an answer under this act-id whose digest is not of what was sent" | Alice's act: accepted with a stamp. Sent again under the same act-id: another lock, another random part, so another digest `d2`; the gate ends at check (a) (`streaming_gate.clj:37-41`); `d2` reads "not mine", `d1` reads accepted. A nil digest with an answer: "not mine". | 824-834, 845-849 | Yes |
| "For a microbatch act, the door waits as the maker asks, in a bound the maker gives, reading by act-id until an answer is there." | A bound of 600 ms on an act-id that was never sent: reads at 0, 200, 400 and 600, then "nothing yet". A bound of 0, none, or -5: one read. | 858-862 | Yes |
| "That wait is one question about one act-id. It offers no subscription and no stream of changes, and it decides nothing of W4" | The wait holds one act-id and one digest and gives one result. | 853-856, 896-899 | Yes |
| "The door keeps nothing of what it sent once the call returns; the maker holds the act-id and the digest." | A door opened after a restart reads the answer for a pair that a maker kept: the call reads the stock not at all. | 303-307, 832-834 | Yes |

**Part 6.** "The door takes `:who`, `:session` and `:permission` as the maker gives them. It cannot
check that the sender is the who it names, and does not [...] a maker that names `:system` is treated
as any who." Scenario: an act with `:who :system` and one value in `:base`: the stock is
`[:base :microbatch :system]`, as for any who, and the request copies the three parts. PLAN 341-344,
814-817, 1526-1527. Holds.

**Part 7.** "A fourth proof [...] through the door for everything the door covers: acts to both
depots, lock requests, sealing, answers, the base's batches. Its own cases [...]". Each named case
is held against a wrong door under "The door's proof". Holds, with F7. "The module proof stays as
it is": its docstring changes and no check (P15; PLAN 1476, row 21). "The counts before are act 150,
pure 449, module 227": PLAN 1475-1477 gives 150, 538 and 227 after; *ran*: "11 + 55 + 23 = 89 ;
449 + 89 = 538".

### 2. The builder's rules

| The rule | The scenario | PLAN | Holds |
|---|---|---|---|
| D1. "The door checks nothing of the act; the gate does [...] The door's own words name only what the door could not do, never a judgment of the act." | An act with two facts on one place, each with a value: two locks, sent; the gate refuses `:place-named-twice`. The door's tests are those of rules A and B. | 703-755 | As ruled. Point 3 is one place where a word names a thing that the door could do |
| D2. "An act-id is made once and never reused; the door gives ids on request and keeps none" | Part 2 above. | 179-180, 303 | Yes |
| D3. "A lock is requested under the maker's who and permission, and used only for that who in that layer (part 3). The stock is the memory of one process; a restart asks again." | Part 3 above; group 14 of the proof. | 313-344, 285-286 | Yes |
| D4. "An ack is never the answer (F7 A). The door reads the record by act-id, and compares digests." | For a maker's act and for the door's own request: D1 of the life of a request takes "Accepted, with the request's digest". | 500, 650-652, 832 | Yes |
| D5. "A wait for an answer is bounded by the maker's word and is one question about one act-id. No subscription, no bell; W4 is open." | Part 5 above. No bound means one read. | 851-862 | Yes |
| D6. "The door never opens a sealed value, never reads a fact of the record for a maker (group 6 is open), and reads locks only of the requests it made itself." | No step of a send calls `seal/open`. `depot-of-layer` gives a depot word, which goes into an act-id; `answer-of` gives an answer and its stamp. The read of locks is called with the act-id of an open request of this stock and no other. | 247-249, 598, 836-838 | Yes |
| D7. "Every word the door makes is plain, says what the thing is, and is listed for Sid [...] No word comes from the rig's code." | The list, and a search of the rig's, the server's and Inland's code for each name. | 1652-1715 | Yes, with F14; "stock" is point 7 |
| D8. "In this build the door is the module's only client in the runtime [...] The module proof's hand is the one exception, and the proofs' README says so." | Part 1 above. P13 adds the door's proof. | 1545-1549 | Yes, with F6 (4) |

### 3. The store's rules that touch the door

| The rule | Where it touches | PLAN | Holds |
|---|---|---|---|
| 5. "A lock's bytes are made inside the module and are never appended to a depot [...] Neither a retry nor a replay makes a lock again" | The request carries a count. The door sends no request twice; a request in doubt is read, not sent again. | 258, 531-532 | Yes |
| 11. "The answer under an act-id keeps the digest of its act [...] The sender compares it with the digest of what it sent." | `answer-of` compares two hex texts; "not mine" for another. | 832-834 | Yes |
| 15. "A lock that a refused act named is free [...] The door takes a fresh random part for each seal." | The lock is free in the record and never comes back to the stock (P7). `seal` takes a fresh random part at each call (`seal.clj:128`). | 593-596, 719 | Yes |
| 16. "The seal binds a value to its place" | The place is `[layer entity key]`, each as the maker gave it. | 719 | Yes |
| 20. "256 facts in one act [...] 65,536 bytes in one sealed value, 256 locks in one request, and 256 characters in the printed text of an id" | The three limits are read from the declaration; a request asks for 256 at most; a value of 65,509 plain bytes ends in `:seal-a-value` before a lock (*ran*: "65,536 - 28 = 65508"); a layer of 257 characters ends in `:make-an-act-id`. | 139, 426-429, 757-772, 715 | Yes |

### 4. "Not in this build, by decision", line by line

| The line | What the plan does | Decided? |
|---|---|---|
| "A first maker: no converter, no screen, no agent. The proof is the only maker." | The door's proof is the only caller. | No |
| "Who may act as the system (K9)." | `:system` is a who like any other (343-344). | No |
| "Sign-in: persons, their UUIDs, the links to their accounts [...] sessions and their ids" | The door makes no id for a person and none for a session; the request copies `:session` as given (1596-1599). | No |
| "Reads of facts for a screen, resolve, context, the tower of layers [...] what tells a screen that a fact changed (W4); bells and the feed." | Two reads for a maker: a depot word inside an act-id, and an answer with its stamp. The stamp is P10's. | No |
| "The switch of a layer to one lock for each act; forget's destroy step" | One lock for one value; the stock never destroys a lock (599). | No |
| "A network edge." | In-process Clojure. | No |
| "Re-class (?9), promotion (?10), what a key is (K5)." | The read of the layer's depot is made at each act-id, "what leaves room for a layer whose depot can change"; the depot in the stock's key leaves the same room and decides nothing. A store key is told by its being one of the three. | No |
| "Any change to the gates, the record or the declaration." | Three docstrings change in those files (rows 14 to 17 of the plan's table), by P15. `seal.clj` gains one function, by ruling 5; it is none of the three. | No |
| "Where the door's code must leave room for one, say so in the docstring." | The namespace's docstring names each room by its row (1528-1532). | Yes, it says so |

### 5. "Explanations, as the tree's rules say", and "Commits and the receipt"

| The sentence | PLAN | Holds |
|---|---|---|
| "Each namespace and each function has a docstring; each folder a README with a parent link; no function lists in READMEs; no history of corrections." | 1523-1544 | Yes |
| "The door's namespace enters the tree's README map." | Rows 4 and 9 | Yes |
| "`door/README.md` no longer says the door is not built: it says where the door sits and what the folder gives it." | Row 11; 1540-1544; ruling 7 | Yes |
| "The door's own explanation holds 'Decisions this build stands on' [...], 'Engineering choices' [...], and 'Words a session made, Sid's to change'." | 1540-1544. The plan puts the three in `door/README.md`, which D7 calls "the door's README" and ruling 7 is about. | Yes |
| "`module/README.md`'s line that any caller can read a lock's bytes stays true and now names the door as the caller it meant." | That file has no such line; read whole. The line is in rows 5 and 16 (P12). | As ruled; for the receipt (1586-1587) |
| "The proofs' README gains a row for the door's proof and leaves the module proof's row as it is." | Row 20; 1545-1549 | Yes |
| "Sid's answers are in `src-store/README.md` as his, never in these files as new text." | "the four of 9 October by row, the rest by pointer" (1542-1543) | Yes |
| "Commit on `main`, grouped by concern (the door; its proof; the maps) [...] No push. No `Co-Authored-By`, no `Claude-Session`, no attribution line of any kind." | 1620-1624, added by F9 | With F9; point 6 |
| "The receipt to Sid: the line 'proven by a hand-played maker, used by no inhabitant yet' [...]; the commit hashes; the four proofs' outputs in full [...]; every provisional choice made [...]; every place this contract could not be met as written, and why; what the build found that is not decided [...]; `git status --short`" | 1625-1651: items 1, 2, 3, 3a, 3b, 4, 5 and 6 | Yes |

### 6. The rulings on P1 to P22, and rules A and B

| The ruling | One scenario | PLAN | Holds |
|---|---|---|---|
| Rule A: "The door makes each of its own additions right, or it says that it could not and appends nothing." | Alice's act with one value: the thing has an act-id of its layer, `:depot` equal to the act-id's second slot, `:sealed` of 35 bytes, a `:lock-id` of the shape, and no `:v` on that fact. `declaration/check` takes each (`declaration.clj:189-196`, `361-362`, `411-446`). | 713-721 | Yes |
| Rule B: "A value that the door finds under `:v` is sealed so that it reads back, or the act is not appended." | `:v 1/3`: `act/value?` gives false (`act.clj:269`); the word `:seal-a-value` at `[:facts 0 :v]`, no lock taken. | 751, 924 | Yes |
| P1 (a), with the request's append | The act's append throws: `:append-cleanly`, with the act-id, the digest and `:appended :not-known`. The request's append throws: `:append-its-request-cleanly`, nothing appended for the act. | 720, 927, 931 | Yes; the proof of it is F7 (i) |
| P2 (a) | A record with a `:layer`: `:read-the-act`. `:layer "notes"`: `:make-an-act-id`. A given act-id `[:plans :streaming u]` with an act of `:notes`: `:make-an-act-id` at `:act-id`. A given `[:notes :microbatch u]` for a streaming layer: it goes to the microbatch depot, and that gate answers it. | 713-715, 723-733 | Yes; F10 for one sentence |
| P3 | `:act-id` on the act: `:add-its-parts`. A fact with `:v` and `:sealed` under a plain key: `:add-its-parts` at `[:facts i :sealed]`. A record with a value as a fact: `:seal-a-value`. A fact with `:sealed`, `:lock-id` and no `:v`: as given. | 714, 746-749 | Yes, but for a fact under a store key: point 1 |
| P4 (a) | Step 1 reads six parts; the request copies four as given. | 713, 258 | Yes |
| P5 (a) | Carol, above. The word says that nothing is recorded under the act's act-id. The docstring says the two outcomes. | 926, 809-812 | Yes |
| P6 (a) | The table of what each namespace requires: `depot-at-making`, `routing-id` and the three tests of a read. No PState, no key of a row. | 136-147 | Yes, but for the test of a plain map: F10, point 2 |
| P7 (a) | An act of 3 values is refused `:place-named-twice`: the 3 locks are free in the record (`decision.clj:388-391`) and gone from the stock. | 593-596, 1202-1207 | Yes |
| P8 (a) | The base, the gate paused: the call ends at 10,000 ms in `:get-locks-within-its-wait`; the request stays *sent*; the next call reads it. | 467-491, 500, 534-536 | Yes; F1 for how far the bound reaches |
| P9, the two bounds | 257 values: the walk stops at the 257th, `:seal-that-many-values`, no lock. A string of 65,509 bytes: `:seal-a-value` with the two numbers, no lock. 300 retracts with no end: the call does not come back, and the plan says so. | 426-429, 757-783 | Yes; F12; point 9 |
| P10 | The read throws: `:read-the-answer`. Accepted and refused carry the stamp. "Not mine" carries nothing. One wait for any act-id. | 824-838, 853-854 | Yes |
| P11 (a) | Part 5 above. | 845-849 | Yes |
| P12 (a) | Rows 5 and 16 of the table: opened below. | 1559, 1570 | Yes |
| P13 (a) | The proof reads the logs, the locks and the lock rows round the door, and appends through it but for one check. | 1329-1346 | Yes, with F6 (4) and F7 |
| P14 (b) | `:make-an-act-id` has the case. | 942-944 | Yes |
| P15 (a) | The table. | 1551-1587 | With F6 |
| P16 (a) | A request is sent only in step 6 of a send, by a call that came short. The door has no thread. | 353-355, 597, 654-657 | Yes |
| P17 | `:append-ack`. | 635 | Yes |
| P18 | The door prints as "door" and the module's name. `:said` is Rama's message; the word about the read of locks carries the class alone. The door's own calls throw nothing. | 209-213, 934-938 | Yes |
| P19 (a) | `:facts {:e :n1 :k :text :v "x"}`, one map and no sequence: the door finds no fact, the thing goes as given, the value is plain in the log, and the gate refuses `:malformed`. | 738-740 | Yes; for the receipt (1634) |
| P20 (a) | Facts given as `(map identity [...])`: the thing holds a vector of the same facts in order; the digest is taken of that. | 785-787; group 6 | Yes |
| P21 (a) | The read of the layer's depot throws: the word and no id. | 717, 921 | Yes |
| P22 | No closing; one door for a process; the proof counts; `stock-count` gives three numbers. | 193-196, 283, 309-311, 1212-1214 | Yes |
| On threads | *Searched*: Rama's documentation says nothing of a client handle and threads. The plan's probe: 16 threads, "errors 0". The proof's group 11. | 659-668 | Yes; one run is not Rama's word, and the plan says so |
| On what Rama's client cannot turn into bytes | The plan's probe: a plain Java object, "14 ms :threw [...] CallbackException [...] answer after: nil"; a function and a ratio went in and were refused `:unknown-part`. Read in the output. | 1243, 1281-1287 | Yes |
| On a query's time limit | The setting is named, `topology.query.timeout.millis` (`13-query-topologies.md:342`); one run gave 70,002 ms. | 886-894 | Yes |

### 7. The eight rulings on the plan

| The ruling | Held against the plan |
|---|---|
| 1. One request at a time; (a) for the base | PLAN 453-460, 1723-1738. Holds. The number in its text is corrected by F4 (3). |
| 2. No copy of a layer's depot | No remembered depot is in any step: `act-id-for` and step 5 read each time. Holds. |
| 3. The answer and then the locks | D1 before D2 (500-501). Holds. |
| 4. The oldest run first | 542-574. Holds. Its price, "one seek more for one act of each request", is right at most: 2 for 3 requests in the plan's example (F4, 1). |
| 5. `sealed-length` | 757-772; row 13 of the table; group 15 of the pure proof holds it against `seal` for three sizes. Holds. The word is listed by F14. |
| 6. A request in doubt is kept | 515-532, 504. Holds as steps. F2 for what "once more" is; point 4; F7 (ii) for its proof. |
| 7. `door/README.md` keeps two groups of files apart | 1540-1544 says where the door sits and holds a row for each of seven files. The plan's line does not name the two groups. The ruling stands beside the plan as a requirement, and the build writes the file; no line of the plan is against it. Holds. |
| 8. A read of locks that came back ends the request | 502, 576-584, 930. Holds as steps. Point 3 for what it does to a sound act; F3 (ii) for the bound; F7 (iii) for its proof. |

The build session's last lines, "for the check of the plan to trace": F13, and "Concurrency", 6
and 7.

### 8. The twelve things of the split

| # | The thing | Where the plan has it | Whole? |
|---|---|---|---|
| 1 | The door's memory of one process | 278-311, 1103-1126 | Yes |
| 2 | The stock: (a) makers at once, (b) the life of a request, (c) the taking | 431-491, 492-541, 542-589 | Yes, with F1, F2, F3, F13 |
| 3 | What the calls leave in the cluster | 1175-1215 | Yes, with F2, F3 |
| 4 | The way of one act in | 703-818 | Yes; point 1 |
| 5 | The answer back, and the wait | 819-900 | Yes, with F4 (2) |
| 6 | The door's own words | 901-960 | Yes |
| 7 | What the plan must find out or say | 961-1069, 1250-1317 | Yes, with F4, F5, F15 |
| 8 | Where the code sits | 105-150 | Yes; point 2 |
| 9 | The door's own proof | 1318-1510 | With F7 |
| 10 | The explanations | 1511-1588 | With F6 |
| 11 | What is not built and not decided | 1589-1614 | Yes |
| 12 | What goes to the receipt | 1615-1651 | With F9 |

### 9. The eighteen things that are no act, in the plan's order of steps

| Row | What is given | The step that ends it, or what is appended | What comes back | As the rulings ask? |
|---|---|---|---|---|
| 1 | nil, 7, "x", `[1 2]` | Step 1: no plain map. `:read-the-act`. | Nothing appended | Yes |
| 2 | `{:facts []}`; `{:layer nil}` | Step 1 reads it. Step 3: `usable-id?` of nil is false. `:make-an-act-id` at `:layer`. | Nothing | Yes |
| 3 | A sorted map with string keys | Step 1: it is a plain map, and the read of `:layer` throws. `:read-the-act`. | Nothing | Yes |
| 4 | A record with a `:layer` | Step 1: no plain map. `:read-the-act`. | Nothing; no stray | Yes (P2) |
| 5 | `:layer "notes"`; `:layer 7` | Step 3. `:make-an-act-id`. | Nothing; no stray | Yes (P2) |
| 6 | A layer of 257 characters | Step 3: `usable-id?` holds the limit (`declaration.clj:276-282`). `:make-an-act-id`. | Nothing; no stray | Yes (P2) |
| 7 | No `:facts`, or nil | Step 4 finds no fact; m = 0; the thing with `:act-id` and `:depot`, and no `:facts` made. | `:malformed`, recorded (`act.clj:343-345`) | Yes |
| 8 | `:facts` is one map, a set, a string | The same. A value inside goes plain. | `:malformed`; the log keeps the value | Yes (P19) |
| 9 | Facts with no end | With a value in each: the walk stops at the 257th value, `:seal-that-many-values`. With 256 values or fewer in all: the call does not come back. | Nothing | Yes (P9); the plan says both |
| 10 | A fact that is "x", 7, `[1]` | As given, at its position. | `:malformed` | Yes (P19) |
| 11 | A fact that is a record with a value | `:seal-a-value` at `[:facts i]`. Without a value: as given, and the gate refuses it. | Nothing; or `:malformed` | Yes (P3, rule B) |
| 12 | A fact with `:sealed` and `:lock-id` and no `:v` | As given; no lock taken for it. | The gate's judgment of the maker's seal | Yes (P3) |
| 13 | A fact with `:v` and `:sealed` | `:add-its-parts` at `[:facts i :sealed]`. | Nothing | Yes for a plain key; point 1 for a store key |
| 14 | An act with `:act-id` or `:depot` | Step 2. `:add-its-parts`. | Nothing | Yes (P3) |
| 15 | `:v 1/3`; a list; a record; `##NaN`; a value inside 33 collections | Step 4: `act/value?` is false for each (`act.clj:262-269`). `:seal-a-value`. | Nothing | Yes (rule B) |
| 16 | A value fact with `:e "n1"` | Sealed to the place `[layer "n1" key]`; one lock taken; sent. | `:malformed`; the lock is free in the record and out of the stock | Yes (P7) |
| 17 | A part that the form does not know | As given. | `:unknown-part`; the log keeps what the part held | Yes (P19) |
| 18 | 300 facts, 200 with values; 1,025 entries of `:based-on`; an entity of 300 characters | 200 locks taken, the thing sent. With 300 values: `:seal-that-many-values`. | `:too-many-facts`, `:too-many-based-on`, `:id-too-long` | Yes (P9) |

The order holds what the plan says of it: steps 1 to 4 make no round trip, step 5 makes one and
takes no lock, and a lock is taken only when nothing that the door can know beforehand is left
(PLAN 706-709). One thing more that it does not say and that is right as it stands: the plain bytes
of each value are made in step 4, before the read of step 5, so a thing that ends in step 5 has
cost the door's process the bytes and the cluster nothing.

### 10. The cells of the matrix that name the door

Each cell of IS matrix A to F was read against the steps. The cells whose outcome the rulings
changed are marked.

- **A0** (a layer that is no usable id). `act-id-for`: the word, not an id (P2; PLAN 174-176). A
  request and a send: step 3 ends them; no stray is ever made. Changed by P2.
- **A1** (not made, starts streaming). `act-id-for`: `[layer :streaming u]`. A right making act: two
  store-key facts, no lock, 1 or 2 round trips, `:gate-ran`, accepted. A value act: the stock is
  empty, the request is refused `:layer-not-made`, the word carries it, nothing is appended; the
  stock held nothing and gave nothing, so it leaves the map. As the cell.
- **A2** (the base, not made). The making act: `:depot-holds`, then accepted by a wait. A value
  act: the request waits for its batch and is refused, or the bound passes first. As the cell.
- **A3** (made, streaming). A request under a permission that stands: 4 or more locks, indexes from
  0. A send with locks in the stock: one append; the stock is fewer by one for each value whatever
  the answer. As the cell.
- **A4, A5** (the base, made, and in the making's batch). The request's answer is readable when its
  batch is closed; the door reads the locks after that, so each comes with its bytes. A lock of
  the base that later seals a value comes from the read with nil, and the door does not read a
  request twice. As the cells.
- **B0 to B7** (an act-id). B1: a send gives the act-id, a digest and the ack. B2 and B3: a second
  send under the id is another thing, takes other locks, and reads "not mine". B4: a corrected act
  under a refused id reads "not mine". B6 is not reached through the door (P2). B7: the word of
  the thrown append carries the digest, so the cell's "the maker has d1 only if the door's word
  for the thrown append carries it" is met (PLAN 931).
- **C0 to C5** (a lock). C2 with another who or layer: another stock. C3 with a refusal, a second
  arrival or a thrown append: the lock does not come back (P7). C5: never destroyed, counted by
  the proof. As the cells.
- **D0 to D5** (a request). The plan's table of the life of a request is the matrix's states with
  one more, *accepted* with its locks not read. Each is traced under "Failure", 3.
- **E0 to E6** (the door's memory). E1 with `act-id-for` or `answer-of`: the stock is not touched.
  E3: the door asks for more, and the act names the locks of two requests. E5: the second call
  stands for the seat inside its bound. E6: a new request; no lock of a request from before.
- **F0, F1** (a depot's log). A send that ends in a word leaves nothing of the act, but for
  `:append-cleanly`; a request that the send caused may be there.

## Concurrency, with the interleavings written out

The mechanism, as the plan has it. A stock's value holds its runs, how many locks it gave out and
at most one open request; it changes by compare-and-set (PLAN 455-457, 502). A take of m gives the
next m locks of the queue or changes nothing. A call that comes short stands for the seat, which is
fair and can be left at a time limit (299-301, 458-459). Only the call in the seat reads or changes
the open request. A call counts itself into its stock and out (1152-1158). T is the end of a call's
wait: 10,000 ms after its first short take (467-471).

The five questions, each answered at the end: can a lock go to two facts or two acts; can a maker
wait past the door's bound; can a maker be handed a refusal that its own call did not send; can a
stock be left where it never asks again; can a seat be left taken.

**1. Two makers, the stock empty, a streaming layer.** A and B each need 1.
(1) A takes: short. B takes: short. (2) A gets the seat; B stands, until its T. (3) A takes again:
short. No open request. A makes request R, n = 4, writes it into the stock as in doubt, appends
with `:ack`, marks it sent. (4) A reads R's answer: accepted, R's digest. A reads the locks 0 to 3.
(5) A, in one compare-and-set: the run `R: 0 1 2 3` in, the open request out, lock 0 taken. A
leaves the seat. (6) B gets the seat and takes again: lock 1. B leaves the seat. B sent no request.
A's fact names `[R 0]`, B's `[R 1]`. B's lock was asked for under A's permission; IS O4, edge
cases, allows it, and both acts carry one who and one layer.

**2. Three makers, the stock short.** The stock holds 1 of run Q. A needs 2, B needs 1, C needs 3;
7 are given out.
(1) B takes 1: the stock is empty. B never stood for the seat. (2) A takes: short; A sits. C takes:
short; C stands. (3) A asks for n = max(2, min(64, max(4, 7))) = 7, reads 7, and in one step puts
the run in and takes 2: 5 are left. (4) C sits, takes again: 3 of the 5. No second request. Had B
come after (3) it would have taken one of the 7 before C; C would then find 4 and still take 3.
A call in the seat that asks is always served by its own request: n is never less than m (PLAN
402-409), and the run goes in and its own m come out in one step, so no take of another call can
come between.

**3. The stock full.** Each of 16 calls takes by compare-and-set and none stands for the seat. Two
takes that read one value: one compare-and-set wins, the other reads the new value and takes the
locks after it. No lock is in two takes.

**4. A take racing the call that puts a run in.** The stock holds 0; X is in the seat with 4 locks
read; Y takes 1. Y's take before X's step: short, and Y stands for the seat; X puts the run in and
leaves; Y sits, takes again, and has its lock. Y waited for the seat a moment although the locks
came: inside its bound, and right. Y's take after X's step: lock 1 at once.

**5. Leaving the seat.** By a word: A's request is refused; A forgets it in one step, the `finally`
gives the seat up, A is handed the word with R's answer; B sits, finds no open request, and asks
under its own act's who and permission. B is not handed A's refusal (PLAN 461-465, 503). By a throw
of Rama's client: A's append of R throws; R stays in doubt (it was written before the append, 499);
the seat is given up; B sits and reads R's answer until B's T. By an interrupt in a wait: the wait
for the seat or the sleep between two reads ends as if T had passed; the word is
`:get-locks-within-its-wait`; a request that was acked stays sent. By an interrupt inside a call of
Rama's client: *ran*, the call throws at once; the same as a throw (F8).

**6. An empty stock taken out of the map, racing a call that enters it.** F13 has the interleaving
that loses three locks when "holds nothing" is read before the count, and why it cannot happen when
it is read inside the step that brings the count to none. With the clause that F13 added: X's
request is refused; X counts itself out; in that step the count is 0 and the stock holds nothing,
and it leaves the map. Y enters before that step: the count was 2, X's step leaves 1, the stock
stays, Y uses it. Y enters after: Y gets a new stock. No call holds a stock that the map let go.

**7. A request left open by one call and met by the next.** Under "Failure", 3.

**The answers.**

- *A lock to two facts or two acts.* No. A lock leaves the stock by one compare-and-set and never
  comes back (P7). The locks of a request go in by one step that also forgets the request, so they
  go in once. A request in doubt that is forgotten is never read. Each request has a new uuid.
- *A wait past the door's bound.* A call that stands for the seat: no. The call in the seat: by one
  append or one query of Rama's client that is on its way (F1).
- *A refusal that its own call did not send.* No: a call hands on only the refusal of the request
  that it sent itself in this call (PLAN 461-465).
- *A stock that never asks again.* A stock asks again unless it holds an open request. *Sent*: its
  append was acked, so a depot holds it, and it is no stray: the door built it as a plain map, with
  the act's layer, which passed `usable-id?`, an act-id of that layer, and the depot that it was
  appended to (`declaration.clj:477-487`, each of the five tests). So a gate answers it, and it
  stays open only as long as the gate does not run. *In doubt*: forgotten at the end of the next
  call's wait. *Accepted*: read at the next call; it stays only while the read of locks throws.
  After a read that came back, ruling 8 ends it. So no: but for a gate that never runs or a read
  that never works.
- *A seat left taken.* Not by any way out that the plan names: each gives it up in the `finally`
  (PLAN 1242). A thread that is stuck inside a call of Rama's client holds it until that call
  ends: up to the client's own limit (F1); the calls that stand for it end at their bounds.

## Failure

**1. Each place where an append or a query can throw, and what the stock holds after.**

| The step | It throws | The stock after | The maker gets |
|---|---|---|---|
| Step 5, the read of the layer's depot | Before any lock | Untouched | `:read-the-layers-depot` |
| Step 6, the append of the request | After the request was written as in doubt | The open request, in doubt | `:append-its-request-cleanly` |
| Step 6, a read of the request's answer | The request is sent or in doubt | The open request, as it was | `:read-its-requests-answer` |
| Step 6, the read of the locks | The request is accepted | The open request, accepted | `:read-its-requests-locks` |
| Step 8, the append of the act | The m locks are taken | m fewer; they do not come back | `:append-cleanly`, with the act-id and the digest |
| `answer-of`, a read | No lock is in play | Untouched | `:read-the-answer` |
| `act-id-for`, the read | | Untouched | `:read-the-layers-depot` |
| `open`, a fetch of a handle | | No door | `:find-the-module` |

In each row the seat is given up and the call counts itself out. No row leaves a lock both in the
stock and on a fact.

**2. A process that ends at each step of a send.** Before step 6: nothing is anywhere. In step 6
after the request's append: the request is decided; its locks are rows that seal nothing, n at
most 256. After step 6 and before step 8: the m locks are rows that seal nothing, with what the
stock held. In step 8: the depot holds the thing or not; if it does, a gate decides it. The digest
is given only when the call returns, so no maker holds it for a call that never came back, and the
answer of such an act can be read by nobody as its own. After step 8: the act is decided; the stock's unused locks are lost. Each is inside
the bounds of PLAN 1187-1211 as F2 and F3 leave them.

**3. An open request in each of its three states, met by the next call B.**

- *Sent* (the call before ended at its T, or its read threw). B sits and reads the answer at once.
  Accepted: B reads the locks; when they serve B's act, B takes its own and goes on; one request
  is in the log. Refused: B forgets it, tells nobody, and asks under its own act (503). Nothing at
  B's T: B ends in `:get-locks-within-its-wait` and the request stays.
- *In doubt* (the append threw). The same reads. An answer is used as any. None by B's T: forgotten
  (504). F2 says how long that is.
- *Accepted* (the read of locks threw). B reads the locks with one query. They come: as above. The
  read throws again: `:read-its-requests-locks`, and it stays.
- In each: when the locks came and the stock is still short of B's need, ruling 8 ends B's call in
  `:read-its-requests-locks`, and the call after it takes or asks. Point 3.

**4. The module's own second runs.** A streaming request is read only after its ack: its rows are
committed and "a second run after a commit ends at check (a) and writes nothing"
(`streaming_gate.clj:75-79`). A request in doubt is read only by `answer-by-act-id`, which "sees
only committed state" (`door_reads.clj:38`). A request of the base is read only when its batch is
closed, and "a closed batch cannot run again" (`door_reads.clj:44`). So the door never holds a
lock's bytes that a second run could change.

## The cost, recounted

Every figure of the plan was summed again; the lines in quotes are from `out-sums.txt`.

- **The size rule's table** (PLAN 373-377). "(a) fixed 64 values 1: requests 1 left 63", "values 20:
  requests 1 left 44", "values 2000: requests 32 left 48"; "(a') fixed 8 [...] 1 left 7", "3 left
  4", "250 left 0"; "(b) [...] values 1: requests 1 left 3", "values 20: requests 4 left 12 first
  sizes [4, 4, 8, 16]", "values 2000: requests 36 left 48". All nine cells hold. "rule (b), first
  128 values: requests 6 [4, 4, 8, 16, 32, 64] | one request in 64 = 1.56 % ; in 256 = 0.39 %":
  the plan's 6, 1.6% and 0.4% hold. "a small batch of 8 at 1,000 acts a second: 125 requests":
  holds.
- **One request against one unused lock.** "a request: 300 + 140 + 100 = 540 bytes; / 100 = 5.4
  unused locks": the plan's "about 550" and "about five" hold.
- **The round trips of each call**, from the steps as written. `open`: 5. `act-id-for`: 1, or 0 for
  a thing that is no id. `send-act` with a given act-id and no value, or with its locks in the
  stock: step 8 alone, 1. With no act-id: steps 5 and 8, 2. Short in a streaming layer: the
  request's append, its answer, its locks, 3 more. Short in the base: the append, the reads of the
  answer from 200 ms on ("reads at [200, 400, 600, 800, 1000] -> 5 reads, 4 that find nothing"),
  the locks: 7 more, which the plan writes as "3 more, and the reads of a wait". `answer-of`: 1, or
  0, and one more for each 200 ms. Each is what PLAN 198-207 and 797-804 say.
- **The six kinds of act** (980-985). A: 3 round trips, 1 + 1 = 2 seeks. B: 6, and 2 + 1 + 1 = 4
  seeks with 64 rows. C: 3 and 2. D: 1 + 1 + 6 = 8 round trips, 2 + 5 + 2 = 9 seeks. E: 8 + 1 + 5 +
  1 = 15, and 9 + 4 + 2 + 3 = 18 seeks with 256 rows. F: none. Each holds, with F5 for D and E.
  "B/(A+B) = 0.015625 = 1/64"; "E/(D+E) = 0.003846", against 1/256 = 0.003906: the plan's 0.0005
  is that rate rounded.
- **Against the streaming gate's aim.** "4 cold reads at 0.5 and 0.3 ms: 500 to 833 acts a second";
  "6 cold reads [...] 333 to 556": the plan's 500 to 830 and 330 to 550 hold. The 0.1 read an act
  does not: F4 (4).
- **The wait.** "every 20 ms reads 46"; "every 200 ms reads 6"; "every 500 ms reads 3"; "50 ms
  doubling reads 6, the answer is read at 1550 ms". The reads hold. The queries a second did not:
  F4 (2).
- **The memory.** Under check 6 of the template.
- **The pure proof's count.** "group 15: 55 | group 16: 23 | group 14: 11"; "rows of the table of
  things that are no act: 23"; "449 + 89 = 538". Holds.
- **The timings of the probes**, each read in its output file: 3 ms for the five handles; 66.6 ms
  for the making act; 0.57 ms, the median of 30 reads of a depot; 14.9, 1.9 and 12.8 ms for a
  request of 64; 7.09 and 1.07 ms, the medians of 40 acts; 400 results in 1,044 ms with "errors 0";
  14 ms to a `CallbackException`; 70,002 ms to `QueryTopologyInvokeFailed`; 10,000 ms for each
  append after the module was destroyed; `:default 10000` for the two foreign limits and `:default
  60000` for `topology.query.sync.timeout.frequency.millis`; "OPS-UUID ()"; and probe 4's one
  line, "TYPE rpl.rama.util.rooted_dag.RootedDAG count 3", which is the plan's "no plain map". The
  one that the plan had wrong: F4 (3).

## The module as built

- **The door's thing, in `stray-reason` and `check`.** Under "Concurrency", the answer on a stock
  that never asks again, for a request; the same five tests hold for an act that passed steps 1 to
  3. In `check`: `:arrived-act` takes `:act-id` and `:depot` first and then the maker's ten parts
  (`declaration.clj:183-196`); a sealed fact is `:e :k :v :replaces :lock-id` in the shape, with no `:v`, and
  `:sealed` beside it (`:491-512`); the rules that touch the door's additions are
  `:depot-is-the-act-ids-depot`, `:plain-value-is-sealed`, `:sealed-fact-has-no-plain-value`,
  `:seal-is-whole`, `:sealed-is-bytes`, `:sealed-within-the-limit`, `:lock-named-once` and
  `:ids-within-the-limit`, and the thing of PLAN 785-791 keeps each.
- **The door's request.** Part 3 above. A gate that accepts it writes n keys of new locks at
  `<layer> <request uuid> <index>` (`decision.clj:366-368`).
- **`locks-of-request`.** Streaming: the range of rows, each with its bytes, in each state
  (`door_reads.clj:118-124`). Microbatch: `{}` unless the request's answer is there, is no refusal
  and is of a closed batch; then the range, and a lock that seals with nil for its bytes
  (`:126-143`; `rows.clj:258-269`). The door reads a request once, before any of its locks is used.
- **An act that names the locks of two requests.** Streaming: `lock-ranges` gives "one range for
  each request whose locks the act names, from the least to the greatest index that the act uses"
  (`decision.clj:274-290`); the door's neighbours make each range exactly the locks named.
  Microbatch: a question goes "to the task of a request's uuid, once for each request whose locks
  the act names" (`batch.clj:229-231`, `254-258`). A lock is "shared", and its act sent to the
  meeting task, only when another act of the batch names the same index, or the request that makes
  it is in the same batch (`batch.clj:417-447`). The door gives an index once, and reads a
  request's locks only after its batch is closed, so neither comes about. Both gates take such an
  act.
- **`answer-by-act-id`.** For `[layer [layer :streaming u]]` and `[u [layer :microbatch u]]`: the
  head's test gives the depot and the key (`rows.clj:323-332`). For a thing that is no act-id the
  door makes no call. The plan passes nothing else.
- **`depot-of-layer`.** For a layer that passed `depot-read`.

## The door's proof

Each named case of C part 7, and each ruling that asks the proof for something, against a door
that is wrong in that one thing.

| What is asked | The plan's case | A wrong door | Does the case fail for it? |
|---|---|---|---|
| "acts to both depots" | Groups 2, 4, 12 | Sends a base act to the streaming depot | Yes: refused `:layer-belongs-to-the-other-gate`; group 12 wants accepted |
| "lock requests" | Group 4: the requests in the log ask for 4, 4, 8, 16, 32, 64, 64 | Asks for a fixed 64 | Yes |
| "sealing" | Group 4: the lock opens the sealed bytes at the place to the UTF-8 of the one text | Seals to another place, or other bytes | Yes: the gate refuses, and the proof's own opening fails |
| "answers" | Group 9 | Gives accepted for another digest | Yes: "not mine" is wanted |
| "the base's batches" | Group 12: 8 acts on 8 entities in one batch | | Yes |
| "each of the door's own words" | Group 8 | A word is never given | Yes, for each of the fourteen |
| "the not-mine answer" | Group 9: a second, different act; and a maker's own second send | Compares nothing | Yes |
| "that a lock never crosses a who" | Group 10: two holders of one layer, one who in two layers; every lock id of the run held to its request's who, layer and depot | A stock for each layer only: Bob is given Alice's unused lock | Yes: Alice's stock holds 3 when Bob sends |
| "that no lock's bytes are ever appended" | Group 13 | Appends a lock's bytes | Yes in a streaming layer; in the base only with F7 (iv) |
| "what the door sends for an act is what the hand sends" | Group 5: one act with a value, a retract and a store-key fact; the two records equal but for the uuids and the sealed bytes | Drops a part, reorders, or adds a part | Yes |
| P13: a real throw for the word of P1 | A plain Java object | Gives a wrong digest in the word | No, before F7 (i) |
| P20: the digest is the gate's, for a vector, a list and a lazy sequence | Group 6 | Sends the lazy sequence as it is | The two digests can agree, since a list and a lazy sequence have one text. The case's second half fails for it: `:facts` in the log must be a vector |
| P22: the rows left are counted | Group 14: the count is held against its sources | Loses a stock (F13) | Yes, if the count is held as a sum that must be met: `stock-count`, the locks of acts that were not accepted, the locks of requests that were given up |
| Many threads | Group 11 | Gives one lock twice | Yes: "No lock id is named twice" |
| Ruling 6 | The request that never went in | Never reads a request in doubt | No, before F7 (ii) |
| Ruling 8 | None on the cluster | Leaves the request open | No, before F7 (iii) |

**The fourteen words, and how each is reached.**

| The word | Reached by | Honest? |
|---|---|---|
| `:find-the-module` | A real throw: a wrong name | Yes |
| `:make-an-act-id` | Rows 2, 5, 6; a count of 0 | No throw is needed |
| `:read-the-act` | Rows 1, 3, 4 | No throw of Rama |
| `:add-its-parts` | Rows 13, 14 | |
| `:seal-a-value` | Rows 11, 15; the limit | |
| `:seal-that-many-values` | Rows 9, 18 | |
| `:have-its-request-accepted` | A real refusal of a gate | Yes |
| `:append-its-request-cleanly` | A real throw of Rama's client: a `:session` that is a plain Java object. With F7: a second real throw, on a thread that the proof interrupts | Yes; the line says which |
| `:get-locks-within-its-wait` | The request in doubt; and the gate paused with Rama's test tool | Yes |
| `:read-its-requests-answer` | **The proof makes the read throw itself** | The plan says so (1405-1410) |
| `:read-its-requests-locks` | **The proof makes the read throw itself.** With F7: the short read, with no throw | The plan says so |
| `:read-the-layers-depot` | **The proof makes the read throw itself** | The plan says so |
| `:append-cleanly` | A real throw. With F7: a second, where the thing is on the depot | Yes |
| `:read-the-answer` | **The proof makes the read throw itself** | The plan says so |

Four of the fourteen are reached by a throw that the proof makes, and the plan says which four
(1768-1771). The stand-in works as the plan says: its probe printed "with-redefs on
foreign-invoke-query, a call of four arguments made to throw: [\"clojure.lang.ExceptionInfo: made
to throw\" nil]", the nil being the answer of the call of two arguments that went through.

## The explanations

**The 22 places of the plan's table, each opened at its lines.**

| Row | Where | The words there today | Right? |
|---|---|---|---|
| 1 | `act.clj:59` | "The door is not built; the gate is in its own" | Yes |
| 2 | `act.clj:41-46` | "The proof's hand, which plays the door, asks `normal-value`." (45-46) | Yes |
| 3 | the store's map, `:17-18` | "The door is not built. The [module's proof] plays it by hand." | Yes |
| 4 | the store's map, `:20-34` | The row of `door/` ends "The door itself is not built." (25); the sentence on what loads with Clojure alone (31-34) | Yes |
| 5 | the store's map, `:162-164` | "Any caller of the module can read a lock's bytes. The door proper is the guard, later." | Yes |
| 6 | the store's map, `:192-196` | "The door proper is the guard, later." (196) | Yes |
| 7 | the store's map, `:197-207` | "Nothing is built against it [...] The door proper is the guard, later." (206-207) | Yes |
| 8 | the store's map, `:552-561` | "Two are at the top with no folder: `act` and `module`."; "The two proofs are `pure-proof` and `module-proof`." | Yes |
| 9 | `src-store/README.md:3-9` | "It holds the act [...] And it holds the store's Rama module" | Yes |
| 10 | `src-store/softland/README.md:5-6` | "the act, and the store's Rama module with its two gates and their records" | Yes |
| 11 | `door/README.md:5-9` | "The door itself is not built; the [module's proof] plays it by hand." (7-9) | Yes |
| 12 | `door/digest.clj:5-10`, `:62-68` | "The sender compares it with the digest of what it sent"; "The door's comparison." | Yes |
| 13 | `door/seal.clj:51-52` | "Which plain bytes stand for a value: the door's. The proof's hand seals the UTF-8 of the one text of the value." | Yes |
| 14 | `door/declaration.clj:50-52` | "Callers. `softland.store.gate.decision`, `softland.store.record.rows` and the two gates" | Yes |
| 15 | `module/door_reads.clj:13-15` | "The sender compares the digest with the digest of what it sent (`[...]is-digest-of?`)" | Yes |
| 16 | `module/door_reads.clj:73-76` | "Any caller of the module can read a lock's bytes in this build; the door proper is the guard, later." (73-74) | Yes |
| 17 | `record/rows.clj:83-85` | "The proof's hand calls `routing-id` and the `-read` functions" (84-85) | Yes |
| 18 | `test-store/README.md:10`, `:12-18`, `:20-27`, `:33-37` | Three commands; "The third needs Rama"; "No door is built; the third proof plays it by hand." (37) | Yes |
| 19 | `test-store/softland/README.md:5-7` | "the proof of the act, the proof of the gates' pure namespaces, and the proof of the store's module" | Yes |
| 20 | `test-store/softland/store/README.md:5-9` and the pure proof's section | The pure proof's row (8); 13 groups (72-127) | Yes |
| 21 | `module_proof.clj:12` | "The door is not built. The proof plays it by hand" | Yes |
| 22 | `pure_proof.clj:2-5`, `:33-34` | "The proof of the gates' pure namespaces" | Yes |

Each line number and each quoted word is as the plan has it.

**The three sentences that the plan says stay true** (1581-1584) were read and stay true.
`module/README.md` was read whole and holds no line on who reads a lock, as the plan says
(1586-1587).

**The search for more.** Three sentences that the table did not have: F6. Read and found to stay
true: "The one pair of the door's reads on two tasks" (the store's map, 183); "The door reads all
the locks of a request with one call of `locks-of-request`" (298-299); "three reads by which the
door reads the module" (`module/README.md:6`); "An act as the door sends it" (`module_proof.clj:163`),
said of the hand's thing; "a door has none, and reads" (`:316`); "as a door does" (`:1053`); "It
keeps each thing that it sent" (`:18-20`), said of the hand; "The pure code and its two proofs run
on it" (`src-store/README.md:33-34`), of the alias `:store`; "an append whose ack fails for an act
that the gate accepted" among what the module's proof does not reach (the proofs' README, 304),
which stays true of that proof.

## The self-consistency check

Each place where this paper says "gap", "not said", "did not have", "wrong" or "does not hold" was
read again against its verdict.

- F1 to F15: each is a fail of its check, and each is fixed in `PLAN.md` at the lines given.
- Point 1 is a fail that is not fixed: the plan's two rows for one fact need a ruling between P3's
  letter and rule A. The lines to change are known (749 or 750).
- Points 2 to 10: none is passed in silence, and none is fixed against a ruling. Point 2's one
  wrong sentence is F10. Point 5 is passed on the rule on caching with its reason, and named.
  Points 3, 4, 6, 8 and 9 are rulings or requirements that hold as steps and meet something; the
  plan follows each as ruled.
- Template check 1, the reads of a wait that find nothing: passed, with the reason that D5 closes
  every other way. Not a gap of the plan.
- Template check 7, `stock-count`: no required property is lost without it; P22 leaves it to the
  plan and the rulings accept it. Not re-opened, and said.

No check is passed here whose text says that it does not hold.

## The verdict

**minor-fail.** Fifteen fails, each closed by an edit of the lines that it names; `git diff
door/PLAN.md` shows each, marked F1 to F15. One fail waits for a ruling (point 1) and is one clause.
Nothing asks for the design to be made again:

- the stock's key, its size rule, the seat, the three states of an open request and the nine steps
  of a send were each walked on values, with two and three makers and with each throw, and hold;
- the module takes what the door appends and gives what the door reads, at the source lines given;
- every number but four was summed again and holds.

What the build session rules on: the ten points.

PHASE_VALIDATION:minor-fail
