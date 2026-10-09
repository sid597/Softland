# Test Validation

<!-- Phase 6. Fill in after tests are written in Phase 5. -->

Part: `door`, the one part of the door's build. Stage: the build, step 4 of the Rama skill's
`phase-build.md`, "validate the tests" (`phase-6-test-validate.md`), and what step 5, the runs, then
found. Written by: the build agent (Opus 5.5), 10 October 2026, in the same session that wrote the
proofs, as the build stage asks. It is for the build session to judge. The proofs that it validates:

- `test-store/softland/store/door_proof.clj`, the door's own proof, on Rama's in-process cluster: 21
  groups, 162 checks;
- the three groups that `test-store/softland/store/pure_proof.clj` gained, 14 to 16: 89 cases.

"Proof" is the tree's word for a test namespace; "group" for a `deftest`; "case" for one check of the
pure proof.

## How the skill's template is adapted

The template has four checks, written for the tests of a module. Each is answered for the door's two
proofs. None is left out.

| The template's section | What it holds here |
|---|---|
| Minimize IPC launches | How many clusters the door's proof starts, and why its groups are many `deftest`s in one fixed order. |
| Implicit spec coverage | Each call of the door, each invariant, each edge case and each cell of the matrix of `IMPLICIT_SPEC.md` that names the door, with the group or the case that holds it; and the plan's list of groups, with the check's fixes and rulings 11 to 13. |
| Synchronization | Each write of the proof that a read follows, and what the proof waits on. |
| Test namespaces compile | The loads, and what each namespace requires. |

Sections that the template does not have, and this paper needs: the fails that this validation found
in the proofs; which checks fail for a wrong door, shown by running wrong doors; the pure proof's 89
cases against the plan's list; what the proofs do not reach; and what the runs found.

## What was read, and what was run

Read for this step: `phase-5-tests.md`, `phase-6-test-validate.md`, `artifact-test-validation.md`,
`phase-7-finish.md`; the contract's part 7 again; the plan's "The door's own proof" and "The three
other proofs, after" again; `PLAN_VALIDATION.md`, "The door's proof" and F7, again; the implied spec's
operations and matrix again, against the two proofs line by line.

The order of the work, said plainly. The pure proof's three groups were written and run at once, since
a run of it is two seconds: it passed at its first run, 538. The door's proof was written, then read
against the plan and the implied spec (the first part of "The fails", below, T1 to T5), and then run.
It passed at its first run. Because a proof that passes at its first run has shown nothing about
itself, the validation then went on by another way: a list of wrong doors was written, each one change
of one source file, and both proofs were run against each (`build/mutants/`): 27 in all. Writing the
list found T6, before any wrong door was run. T7, T8 and T9 were found by reading again.

Run, each from the repository root, outputs under
`/mnt/data/projects/research/softland/door-build-2026-10-09/build/`:

- `run-pure-1.txt`: the pure proof's first run with its three new groups.
- `run-door-1.txt`, `run-door-2.txt`: the door's proof's first two runs.
- `mutants/run.py`, `mutants/results.txt`, and for each wrong door a folder with the changed file and
  the two outputs. A wrong door is a changed copy of one file under that folder, put first on the
  classpath by an alias of the command line; no file of the repository is changed by it.
- the later runs are listed under "What the runs found".

## The fails that this validation found, each fixed in the proofs

**T1. A check that fails for a right door.** In "things that are no act" the proof held "each of those
things is one record more in the log". One of those things has two values, so a door whose stock is
short sends a request for locks first, which is one record more. Fixed: the door's own requests are
counted and set aside.

**T2. The count of the locks that were made missed two.** The proof counted the locks of each request
"as the door sends one", a thing whose first fact is the request. In the group that holds the door
against the hand, the act itself carries a request fact as a store-key fact, at the third position,
and a gate makes its lock. Fixed: `locks-asked-in` reads a request fact at any position.

**T3. A threshold that the proof's own run cannot meet.** "over more than 900 records with sealed
values": by the proof's own acts there are about 730. Fixed: 600. The threshold is there so that the
check over every record is not passed by a log that holds few.

**T4. A check that is true by how it is built.** "each run of bytes from the rows is among every
lock's bytes", where the second set is made of the first. Fixed: the proof holds a sum: the locks
whose bytes the query gave, and the locks for which it gave nil, are as many as the lock rows.

**T5. Five things of the implied spec and of the plan had no case on the cluster.** Added before the
first run:

1. a fact under a store key that carries a value and a seal goes as given, and a gate refuses it
   `:store-key-sealed` (ruling 9; the pure proof had it, the cluster had not);
2. a value act into a layer that is not made: the door asks first and the request is refused
   `:layer-not-made` (IS matrix, A1);
3. an act-id given back with the other depot word, with no value and with one (IS O4, edge cases; the
   plan's reason for the depot in the stock's key): the group "an act-id with the other depot word";
4. a second maker that waits for the seat of a stock while the first maker's request is not answered,
   and does not wait past its own wait (IS O3, Concurrency; matrix E5);
5. a wait whose thread is interrupted (the plan's "What holds when something fails", the row on a
   thread that is interrupted while it waits).

**T6. Two wrong doors would have passed the door's proof.** Found when the list of wrong doors was
written, by asking of each which check would fail; no check answered for two of them (below, m10 and
m26). Neither was run against the proof as it then stood, so "would have passed" rests on reading and
on the two lines of evidence given here.

- A door that reads the answer of its own request in the base at once, and not after one interval:
  nothing held the plan's choice "In a microbatch layer the first read of the door's own request comes
  after one interval". The wrong door m10 now fails one check and no other, the one that was added.
  Fixed: the group of the base notes the moment of each call of
  Rama's client inside one send, by a stand-in that notes and then calls Rama, and holds the order of
  the calls and that the first read of the request's answer comes 195 ms or more after the request's
  append.
- A door that forgets a request in doubt at the first read that finds nothing: in the proof's one case
  of a request in doubt that went through, the answer was there at the first read. The first run's own
  line says so: "the next send read its answer and used its locks, with 1 append and 2 queries"
  (`run-door-1.txt`), the answer and the locks. The pure proof held the rule's function; nothing on the
  cluster held that the door keeps such a request. That is ruling 12's own case. Fixed: the group "a request in doubt in the base". The
  append of a request into the base is interrupted, as in the streaming case; the next send comes at
  once; its first read finds nothing, because the request's batch is not closed for most of a second;
  the door must keep the request, read again, and use its locks with one append. The check holds 3
  queries or more and 1 append.

**T7. The proofs named their cases by the rows of a table that is in a paper of the build.** "row 12"
is a row of the implied spec's table, and `src-store/CLAUDE.md` keeps a second tree of documents out
of the tree. Fixed: each case and each line of the proofs' map says what the thing is.

**T8. No case held a maker's thing with a part named `:could-not`** (the code's fail V3 in
`IMPLEMENTATION_VALIDATION.md`). Fixed: one thing more in a case of the pure proof, which stays one
case, and one thing more that goes in through the door, refused `:unknown-part`.

**T9. Two rows of the plan's own tables had no case**, found when "What the door's proof does not
reach" was written and two of its lines turned out to be cheap to reach. Added:

1. the refusal of a request that an earlier call left open is handed to nobody (the plan's life of a
   request, D4, second half): a request of a who with no permission is left open by a read of its
   answer that the proof makes throw; the next send of that who reads it, sends its own, and is handed
   the refusal of its own. The wrong door m37, which hands on the earlier refusal, fails that check
   alone;
2. a bound of zero, a bound below zero, and a bound that is no number: one read each (IS O5, edge
   cases). The wrong door m38 fails that check alone.

**Three printed lines did not say that they rest on a stand-in.** The plan: "Where it stands in for
one of Rama's client calls to make a read throw or to count round trips, its printed line says so".
Three `SHOWN` lines gave counts of appends and queries and did not say how they were counted. Fixed.

## Minimize IPC launches

Pass. The door's proof starts one cluster and launches the module once (`door_proof.clj`, `-main`), as
the module's proof does. The pure proof starts none.

Its 21 groups are 21 `deftest`s and not one, as in the three proofs beside it: `test-ns-hook` runs
them in one fixed order on the one cluster, and the count line then says how many groups ran. They
share state on purpose: the layers that earlier groups made, and the stocks of the first door. Where a
group needs to know what a stock holds, it uses a who of its own with a layer of its own (Carol for
the round trips, Dora for the size rule, Erin for the hand, Frank, Gina, Hana, Ivan, Judy and Kim for
one request each), so no group changes what another group's check counts. Three doors are opened on
the one module, which costs 15 fetches of a handle.

## Implicit spec coverage

### The calls of the door, each with a case that passes and a case that ends in a word

| Call | A result | A word | Where |
|---|---|---|---|
| `open` | a door, with and without numbers | `:find-the-module` | "the door is opened" |
| `act-id-for` | a layer that is not made; a made one; the base | `:make-an-act-id` (the pure proof and "things that are no act"), `:read-the-layers-depot` | "an act with no value", "the base through the door", "reads that throw" |
| `act-ids-for` | 20 ids with one query | `:make-an-act-id` at `:count` (pure), `:read-the-layers-depot` | "an act with no value", "reads that throw" |
| `send-act` | both depots; with and without a given act-id; with no value, one value, 10 values; with a request and without | each of twelve words | every group |
| `answer-of` | the four answers; with and without a wait | `:read-the-answer` | "the answer back", "round trips are counted", "reads that throw" |
| `stock-count` | an empty door; after takes; with an open request | none: it gives numbers | "the door is opened", "locks and the seal", "a request in doubt", "the door's memory starts again" |

### The invariants

| Invariant | The group or the case |
|---|---|
| O1.4, an opened door holds nothing | "the door is opened"; "the door's memory starts again" |
| O1.5, a name under which no module runs | "the door is opened" |
| O2.1 to O2.3, the act-id | "an act with no value" |
| O2.4, no id twice | 20 different of 20; over the run, no check: the door keeps none and the uuid is random |
| O2.5, each fact at its position | "the digest is the gate's"; pure, the thing that is built |
| O2.6, no depot remembered | "round trips are counted": a second send that makes its act-id makes one query again; the wrong door m21 |
| O3.1, the request's form | "locks and the seal"; pure, the request |
| O3.2, the count | "locks and the seal", "many makers at once"; pure, the size rule |
| O3.3, the request in the depot of the act's act-id | "the base through the door"; "an act-id with the other depot word" |
| O3.4, the answer before the locks | "the base through the door", the order of the calls; "reads that throw", 2 queries after a read of locks that threw |
| O3.5, a lock never crosses a who | "a lock never crosses a who"; the check over every record |
| O3.6, a lock for one value | "many makers at once"; "the base through the door" |
| O3.7, P18: no lock's bytes | the last group, three checks |
| O3.8, locks only of its own requests | "the door's memory starts again": a new door asks anew |
| O3.9, O3.11, whose refusal | "a refused request"; "reads that throw": the refusal of an earlier call's request is handed to nobody |
| O3.10, no lock destroyed | the count of the rows that seal nothing: each lock that was made is a row |
| O4.1, three additions and no other change | "an act with no value"; "the door and the hand"; pure |
| O4.2, O4.3, the value fact and its seal | "locks and the seal"; pure |
| O4.4, O4.5, the depot and the ack | "an act with no value", "the base through the door" |
| O4.6, O4.7, what the maker gets; the digest is the gate's | "the digest is the gate's"; "an append that threw" |
| O4.8, no plain value under a plain key | "things that are no act" |
| O4.10, O4.11, a word and nothing appended; no throw | "things that are no act"; pure |
| O4.12, the door keeps nothing | "the answer back": a second send is another thing |
| O4.13, the append is over when the call returns | "round trips are counted": an answer at the first read |
| O4.14, neighbours | "reads that throw": the 4 and then the first 6; "many makers at once": one request or two |
| O5.1 to O5.4, the four answers | "the answer back" |
| O5.6 to O5.8, the wait and its bound | "round trips are counted": 4 queries in 600 ms; "the base through the door" |
| O5.9, `answer-of` changes nothing | "round trips are counted": no append |
| O5.11, an act-id and a digest from before | "the module is started again"; "the door's memory starts again" reads through a new door |
| O6.1, the words | the last group: each of the fourteen was given |

### The edge cases

| Edge case | The group or the case |
|---|---|
| The eighteen things that are no act | pure, 23 cases; "things that are no act", 14 that end in a word and 12 that go in |
| A value over the limit of sealed bytes | pure, at its edge; "things that are no act" |
| An act-id given back: of another layer; not of the shape; with the other depot word | pure; "things that are no act"; "an act-id with the other depot word" |
| One faulty act, two outcomes | "a refused request" |
| The same act twice under one act-id; two acts under one act-id | "the answer back" |
| A who's locks under another permission | "a refused request", the second outcome: locks that were asked for under the who's standing permission are used for her act under another, and the gate judges that act's permission |
| The request's answer is not readable yet | "a wait for locks that passes" |
| The request is accepted and its locks cannot be read | "reads that throw" |
| The request's append threw | "a request in doubt", two cases; "a request in doubt in the base" |
| A lock with a way that the code does not know, or nil bytes | "reads that throw"; pure, what goes into a run |
| A bound of zero, none, below zero | "round trips are counted" |
| A digest that is nil | "the answer back"; pure |
| The read throws | "reads that throw" |
| The module is started again | "the module is started again" |
| A restart of the door's memory | "the door's memory starts again" |

### The cells of the matrix

A0: pure and "things that are no act". A1: "an act with no value", "a refused request". A2, A4, A5:
"the base through the door". A3: most groups. B1 to B5: "the answer back". B7: "an append that threw".
C1 to C5: "locks and the seal", "a lock never crosses a who", "the door's memory starts again". D0 to
D5: "reads that throw", "a refused request", "a request in doubt", "a request in doubt in the base",
"a wait for locks that passes". E1 to E6: "many makers at once", "the base through the door", "the
door's memory starts again". F0, F1: every group that counts the records of the logs.

### The plan's list of groups

| The plan's group | In the proof |
|---|---|
| 1. Opening | "the door is opened"; the printed text again, with locks held, in the last group |
| 2. An act with no value | "an act with no value" |
| 3. Round trips | "round trips are counted" |
| 4. Locks and the seal | "locks and the seal" |
| 5. The door and the hand | "the door and the hand send the same thing". The store-key fact of that act is a request fact on the act's own uuid: a permission fact can be written once for a place, so the same act twice could not both be accepted with one |
| 6. The digest | "the digest is the gate's" |
| 7. Things that are no act | "things that are no act" |
| 8. Each of the door's words | "a refused request", "a request in doubt", "reads that throw", "an append that threw", "a wait for locks that passes", and the tally in the last group |
| 8, F7 (i): `:append-cleanly` where the thing is on the depot | "an append that threw", the second case |
| 8, F7 (ii): a request in doubt that went in | "a request in doubt", the second case; "a request in doubt in the base" |
| 8, F7 (iii), ruling 11: the short read | "reads that throw": requests of 4 and 10, the act names both |
| 8, ruling 12: a request in doubt, after the bound | "a request in doubt", the first case, on the door whose wait is 400 ms |
| 8, ruling 13: the answer is read again | "reads that throw": 2 queries and 1 append |
| 9. The answer back | "the answer back" |
| 10. A lock never crosses a who | "a lock never crosses a who", and the check over every record in the last group |
| 11. Many makers | "many makers at once", with the requests held to the rule one by one (F7 (v)) |
| 12. The base | "the base through the door" |
| 13. No lock's bytes, F7 (iv) | the last group: the bytes of a lock of the base that seals come from its row |
| 14. A restart of the door's memory, and the count | "the door's memory starts again" |
| 15. The module is started again | "the module is started again" |

Groups beyond the plan's list: "an act-id with the other depot word" (T5), "a request in doubt in the
base" (T6). Checks beyond it inside the plan's groups: T5, 1, 2, 4 and 5; T6, the moments of the calls;
T9.

### The pure proof's 89 cases against the plan's list

The plan: group 14, 11; group 15, 55; group 16, 23. The proof: 11, 55 and 23; the count line is
`{:test 16, :pass 538, :fail 0, :error 0}`, which is the plan's 538.

- Group 14 (11), as the plan lists them: the table (1); no word is a gate's or a stray's (1); a word as
  a maker gets it (1); a word that is not in the table (1); the four answers and the two cases of a nil
  digest (6); the two acks (1).
- Group 15 (55): things that are no act, 23, two each for a fact that is a record, a fact that holds
  the door's part, and an act that holds the door's part, three for the limits, one for each other
  kind; the limit of sealed bytes (3); an act-id given back (4); the kinds of fact (5); the order of the
  steps (3); the thing that is built (3 and 6); the request (3); things that throw (2); a lock's bytes
  (1); the count of a series (2).
- Group 16 (23): an empty stock (2); taking (4); the size rule (8); the bound over 200 acts (1); what
  goes into a run (2); a lock that was taken (1); the open request (3); what was given out (1); the
  count (1).

One difference in the reading of a line, not in the count: the plan's "eight cases of the rule"
includes, here, one case of the door's five numbers and their defaults, since the rule reads three of
them.

## Which checks fail for a wrong door

A case that passes for a wrong door is a gap. Twenty-seven wrong doors were run against both proofs,
each one change of one source file (`build/mutants/run.py` has each change; `results.txt` the
outcomes). "Fails" is the count of failed checks and errors; the groups are those that failed. Each
of the 27 fails the door's proof; 11 fail the pure proof too.

| | The wrong door | The pure proof: fails | The door's proof: fails | Groups of the door's proof that fail |
|---|---|---|---|---|
| m01 | who dropped from the stocks triple | 0 | 9 | a-lock-never-crosses-a-who, a-refused-request, a-request-in-doubt-in-the-base, no-locks-bytes-anywhere, and 1 more |
| m02 | a take leaves the locks in the stock | 11 | 22 | a-lock-never-crosses-a-who, a-refused-request, locks-and-the-seal, many-makers-at-once, and 7 more |
| m03 | the digest of the makers act in the word of a thrown append | 0 | 2 | an-append-that-threw |
| m04 | a request whose append threw is forgotten at once | 0 | 9 | a-request-in-doubt, a-request-in-doubt-in-the-base, the-doors-memory-starts-again |
| m05 | a short read of an earlier request ends the call | 0 | 2 | reads-that-throw |
| m06 | a request always asks for the most at once | 6 | 8 | locks-and-the-seal, many-makers-at-once, reads-that-throw, the-base-through-the-door |
| m07 | answer of ignores the digest | 2 | 3 | the-answer-back |
| m08 | a value sealed to another place | 1 | 30 | a-lock-never-crosses-a-who, a-request-in-doubt, a-request-in-doubt-in-the-base, a-wait-for-locks-that-passes, and 11 more |
| m09 | the facts are not sent as a vector | 4 | 1 | the-digest-is-the-gates |
| m10 | the first read of an own request in the base comes at once | 0 | 1 | the-base-through-the-door |
| m11 | a wait reads twice as often | 0 | 2 | round-trips-are-counted, the-base-through-the-door |
| m13 | the threads mark is not set again | 0 | 3 | a-request-in-doubt, a-request-in-doubt-in-the-base, an-append-that-threw |
| m15 | append ack to the streaming depot | 0 | 40 | a-lock-never-crosses-a-who, a-refused-request, a-request-in-doubt, locks-and-the-seal, and 9 more |
| m16 | a store key fact is tested after the doors parts | 1 | 2 | things-that-are-no-act |
| m18 | depot dropped from the stocks triple | 0 | 3 | an-act-id-with-the-other-depot-word, no-locks-bytes-anywhere |
| m19 | a stock leaves the map whenever no call is counted in | 0 | 27 | a-lock-never-crosses-a-who, a-refused-request, a-request-in-doubt, a-request-in-doubt-in-the-base, and 8 more |
| m21 | act ids for reads once for each id | 0 | 2 | an-act-with-no-value, reads-that-throw |
| m23 | the door writes over a depot that the maker gave | 1 | 2 | things-that-are-no-act |
| m24 | a door prints its stocks | 0 | 2 | no-locks-bytes-anywhere, the-door-is-opened |
| m26 | a request in doubt is forgotten at the first read that finds nothing | 2 | 3 | a-request-in-doubt-in-the-base, the-doors-memory-starts-again |
| m28 | the call in the seat does not take again | 0 | 1 | the-base-through-the-door |
| m30 | no seat | 0 | 8 | many-makers-at-once, the-base-through-the-door, the-doors-memory-starts-again |
| m33 | the limit of sealed bytes is not tested before a lock | 1 | 1 | the-doors-memory-starts-again |
| m35 | the request does not carry the who | 2 | 69 | a-lock-never-crosses-a-who, a-refused-request, a-request-in-doubt, a-request-in-doubt-in-the-base, and 14 more |
| m36 | a read of locks leaves its request open | 4 | 30 | a-lock-never-crosses-a-who, a-request-in-doubt, a-request-in-doubt-in-the-base, a-wait-for-locks-that-passes, and 8 more |
| m37 | the refusal of an earlier calls request is handed to the next maker | 0 | 1 | reads-that-throw |
| m38 | a bound that is not above zero waits all the same | 0 | 1 | round-trips-are-counted |

What the table does not show. Each wrong door is one change that this session thought of; a wrong
door that it did not think of is not in it. The wrong doors m01 to m36 were run against the proof
before T7 to T9 were fixed; those fixes renamed cases and added checks, and took none away. m37 and
m38 were run after.

Wrong doors that were listed and not run, because no behaviour that a maker or the record can see
tells them from the right door: `:ack` in place of `:append-ack` to the microbatch depot (Rama's
documentation says that the two are the same there); a read of locks over the indexes 0 to 255 in
place of 0 to n - 1 (the same rows are read); a door that never lets a stock that holds nothing leave
its map (memory only: see "does not reach").

## Synchronization

Pass. Each write that a read follows, and what the proof waits on:

- **A streaming act.** The door appends with `:ack`, which returns when the gate has processed the
  thing; `decide!` then reads once. Where the proof reads the log for the act's record right after,
  the append has returned.
- **An act of the base.** `decide!` reads the answer through the door with a bound of 30 seconds. The
  door reads until an answer is there; no sleep of a fixed length stands for it.
- **A request in the base, round the door.** `await-round` reads the request's answer with the
  proof's own handle until one is there, 30 seconds at most, after Rama's
  `wait-for-microbatch-processed-count` on the count of the microbatch depot's log.
- **A paused gate.** `pause-microbatch-topology!`, the sends, `resume-microbatch-topology!`, and then
  waits of the first kind. One fixed sleep is in it, 600 ms before the gate goes on in the check of 4
  makers, so that the 4 calls are standing in the door when it does; the check does not rest on how
  many of them were: one request serves all four either way.
- **An append that threw and went through.** The proof reads the log each 50 ms until the record is
  there, 10 seconds at most, and then reads the answer with a bound of 10 seconds.
- **Threads.** Each `future` is read with a time limit, and a limit that passes gives a result that
  fails its check.

Three checks rest on a time and say so:

- "a wait of 600 ms that finds nothing: 4 queries" holds for reads that each take less than about 60
  ms; on the in-process cluster a read takes about 1 ms.
- "after longer than the door's wait [...] in less than the wait" holds for a send of 2 appends and 3
  queries inside 400 ms; it took 15 ms.
- "the first read comes 195 ms or more after the request's append": the door sleeps 200 ms, and the
  JVM's sleep can end a little early.

If one of these fails on a slow machine, it is a fault of the proof's number and not of the door.

## Test namespaces compile

Pass. `softland.store.pure-proof` loads under `:store`, with Clojure alone, and requires the door's
three pure namespaces; so the alias is still the guard that they need nothing else.
`softland.store.door-proof` loads under `:store-module` with reflection warnings on and none given. It
requires the door, Rama, Rama's path and test namespaces, and of the tree: the act, the declaration,
the digest, the seal, the text, the door's words (for the tally of the fourteen), the first act, the
module, and the record's rows. It requires no private var and nothing of the rig, the server or
Inland.

## What the door's proof does not reach

The proofs' map has the list (`test-store/softland/store/README.md`, "What the door's proof does not
reach"). Beside what the plan lists, this validation found these, and each is in that list or here:

- a request in doubt whose append truly failed, met before the door's wait has passed since the
  throw: the call would wait for nothing until then;
- that a stock which holds nothing leaves the door's memory, and the race of a call that leaves such
  a stock with a call that enters it (the check's F13): no call of the door shows its map of stocks.
  The wrong door m19, which lets a stock go whenever no call is counted in, fails the proof; a door
  that never lets one go passes it, and costs memory only;
- the door's five numbers at `open` beyond `:request-wait-ms`: the pure proof holds what `open` makes
  of them; the cluster is run with the defaults and with one other wait;
- two takes of one lock at the same instant, forced.

None of these is passed as covered.

## What the runs found

Every run of the door's proof and of the pure proof with its new groups, in their order. Outputs under
`/mnt/data/projects/research/softland/door-build-2026-10-09/build/`.

| Run | The proof as it stood | The count line | Seconds |
|---|---|---|---|
| `run-pure-1.txt` | the pure proof with groups 14 to 16 | `{:test 16, :pass 538, :fail 0, :error 0}` | 2 |
| `run-door-1.txt` | the door's proof after T1 to T5: 20 groups | `{:test 20, :pass 153, :fail 0, :error 0}` | 35 |
| `run-door-2.txt` | after T6: 21 groups | `{:test 21, :pass 159, :fail 0, :error 0}` | 38 |
| `mutants/` | 27 wrong doors, each against both proofs | each fails the door's proof | about 42 each |
| `run-pure-2.txt` | after T7 and T8 | `{:test 16, :pass 538, :fail 0, :error 0}` | 2 |
| `run-door-3.txt`, `-4`, `-5` | after T7 to T9, three runs one after the other | `{:test 21, :pass 162, :fail 0, :error 0}`, three times | 39 each |
| `run-act-1.txt` | the act's proof | `{:test 8, :pass 150, :fail 0, :error 0}` | under 1 |
| `run-module-1.txt` | the module's proof | `{:test 22, :pass 227, :fail 0, :error 0}` | 59 |
| `final-act.txt`, `final-pure.txt`, `final-module.txt`, `final-door.txt` | the four, once more, after the last change of the tree | 150; 538; 227; `{:test 21, :pass 162, :fail 0, :error 0}` | under 1; 2; 59; 39 |

**No run of a proof failed.** No check of the door's proof or of the pure proof's new groups has ever
failed against the door as built: every change of the proofs came from reading them and from the
wrong doors, not from a failed run. So nothing in the door was changed because of a run, and no run
failed once and then passed with no change. The door's proof was run against the right door 7 times:
two before T6, three after T9, and the final one twice, because three printed lines were changed after
its first (`run-door-6.txt` is that first). It gave no failed check in any.

What that does and does not say. Seven passes in a row, with 16 threads on one stock in each, show no
flake. They are seven runs on one machine, and the checks that rest on a time are named under
"Synchronization".

What the runs printed that the plan had not run:

- An append with `:append-ack` to the microbatch depot on a thread whose interrupt mark is set throws
  `InterruptedException` at once and goes through all the same, as the check of the plan found for
  `:ack` on the streaming depot: the request was accepted and the next send used its locks
  (`final-door.txt`, "a request in doubt in the base").
- A second maker that needs the same stock while the first maker's request is not answered came back
  at its own wait, 401 ms on a door whose wait is 400 ms, after it got the seat and read the first
  maker's request once (`run-door-1.txt`). On another run it could come back from the wait for the
  seat; the check takes both.
- In the base the door read its own request's answer 4 times, the first 205 ms after the request's
  append, and the send came back after about 0.8 s (`run-door-2.txt`).
- Sixteen threads for one who, 400 acts and 799 values: the requests in the log asked for 4, 4, 7, 14,
  29, 57 and then 64 eleven times; 2 acts named two requests (`run-door-1.txt`). The third request is
  7 and not 8 because an act of more than one value came short while the stock still held one lock:
  a request asks for what the stock has given out, not for what was asked before.
- The rows that seal nothing, counted in both records at the end of a run: the final run's line is in
  the report to the build session and in `final-door.txt`, "THE ROWS THAT SEAL NOTHING".

## The self-consistency check

- T1 to T8 are fails of this check, each fixed in the proofs by a few lines or by one group more in
  the existing namespace.
- "What the door's proof does not reach" lists what no case holds. Each is said in the proofs' map;
  none is in a table above as covered. Where a table above says "see 'does not reach'", the line is a
  fail of that edge case's coverage, and the verdict counts it.
- The three checks that rest on a time are named.

## Verdict

**minor-fail, fixed in this session**, but for what "What the door's proof does not reach" lists.
Eight fails, T1 to T8, each closed by lines in the existing proofs or by a group more in the existing
namespace; no namespace was added and no group was taken away or weakened. What stays open is listed,
with why no case of the in-process cluster holds it.

PHASE_VALIDATION:minor-fail
