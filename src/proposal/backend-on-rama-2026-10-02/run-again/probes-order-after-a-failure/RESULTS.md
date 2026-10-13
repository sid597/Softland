# Order after a failure in a layer that a stream topology decides — runs of 13 and 14 October 2026

Sid's question: "I am against the Rama setting that orders a layer's acts after
a failure. Nobody has shown whether a failure in a fast layer can make acts
that were in flight come out in another order when the store is run again. Run
it and find out. If it can, look for a cheap way to close it."

"The setting" is `topology.stream.max.executing.per.task` at 1. It was not
used in any run here.

**Blocked:** nothing. One kind of failure could not be produced in process. It
is the one that matters most, and it is named under "Not settled".

- Rama 1.6.0, its in-process cluster, one machine, one JVM for the modules and
  for the watchers. 4 tasks, 4 threads, 1 worker, except the five runs named
  `w2-…`: 4 tasks, 2 threads, 2 workers.
- A probe, not a design. Its gate (`src/probe/gate.clj`) stands in for the
  shape that the store's streaming gate has after this build: no partitioner,
  no append and no wait inside an event. The source is
  `(source> *acts {:start-from :beginning :retry-mode :all-after} :> *act)`,
  the depot is declared `(hash-by :layer)`, makers append with `:ack`. One
  event reads `$$answers` at the act's id and does nothing when an answer is
  there; else it reads the row at [layer key], accepts when `:replaces` equals
  the id that the row holds, takes the number max(`:time` × 65536, the layer's
  last + 1), and writes `$$last`, the answer, and for an accept the row. No
  table, name or form here is a proposal for the store.
- "Run again" in every run: module `First` owns the depot and decides the acts
  as they arrive, with the failure put in. Then module `Again` is launched. It
  mirrors First's depot and runs the same gate over it from its beginning,
  with no failure. Then the answers, rows and last numbers of the two modules
  are held against each other, over every act that the depot holds. One line
  says SAME or DIFFERENT.
- The acts. A and B are of one layer and one key and both say `:replaces nil`:
  the one decided first is accepted, the other refused. C is of another layer
  on the same task. E is of A's layer, decided before A is sent. D is of A's
  layer and key, sent when all is quiet.
- The failures are put in from outside the gate's rule, by a plan in an atom
  that only the FIRST attempt of an act in module First reads: a throw, a
  `Thread/sleep` on the task's thread, or an answer that Rama cannot write.
- A number is printed as `<ms since the run's clock began>:<the low 16 bits>`.
- Run again, from this folder: `./run.sh <log name> <scenario> [key value …]`,
  `./run.sh suite <name>`, `./run.sh <log name> cost [key value …]`. The
  scenarios and their options are in the docstring of `src/probe/order.clj`;
  the suites that were run are in `suites/`. `run.sh` takes the lock that
  every in-process cluster on this machine waits on.
- Logs: `logs/<name>.log` holds the probe's own lines of one scenario, with the
  wall clock and the run's clock. `logs/<name>-stdout.txt` or
  `logs/suite-<name>-stdout.txt` holds the JVM's whole output.
  `logs/<name>-rama-lines.log` holds Rama's own ERROR and WARN lines of the
  scenario, each distinct line once with its count.
- 110 runs in all, each on a fresh cluster. `s1-try` and `s2-t3-while-try` are
  the first try of each kind, one run each, kept and counted.
- Every line below says whether it was **run** (with the log) or **read**
  (with file and line), or **derived** (from a run and a rule, not run).
  `reference/rama/docs/` is an extract of March 2026 whose pages name Rama
  1.5.0; a line from it is read, not run.
- Who ran it: one fresh session for this probe alone (Opus 5.5), in the night
  of 13 to 14 October 2026. Nothing was written outside this folder.
  `.cpcache/` here is what `clojure` made when it ran.

## The short of it

1. **With the gate as it is given (`:all-after`), no failure that could be
   produced in process made a run again come out in another order: 87 of 87
   runs SAME.** 23 with a throw, 6 with an answer that Rama could not write,
   38 with a stall on the task's thread past the stream timeout, 6 with a
   stall in the write of the batch, 7 with the timeout set very high, 3 with a
   module update under load, 4 with no failure. In 13 of them between 1,190
   and 8,014 acts were in flight, four on each key.
2. **A throw starts the worker again, and then the records are decided in the
   depot's order.** The rows of the failed batch are gone. An act behind the
   failed one is decided once, after it. 23 of 23 SAME.
3. **An answer that Rama cannot write is a throw too.** The event's code ends;
   the failure comes at the batch's end; Rama treats it as fatal and starts
   the worker again. 6 of 6 SAME.
4. **A stall on the task's thread past the stream timeout starts no worker
   again and drops nothing.** Rama writes no line at all. When the thread is
   free again the batch is written: A's first attempt is accepted and its rows
   stay. Then Rama fails A's `:ack` append ("Callback failure") and runs A a
   second time; the second attempt finds its answer and does nothing. 38 of
   38 SAME, in every window.
5. **The order that the probe of 12 October saw does come back in one
   window.** When B was in the depot before A's event began, B's first attempt
   began before A's second, 16 of 16. It did no harm, since A's rows were
   there: B was refused, as in the run again. In the other three windows A's
   second attempt began before B's first, 28 of 28.
6. **While the thread is held, nothing is appended to that partition**
   (derived from the runs: B's call was made 4 s before A woke, longer than
   the timeout of 3 s, and B's record did not time out; under load 1,613 of
   1,614 calls were answered `{}`). A maker's append that is called during
   the stall waits, and lands when the thread is free. So an act can be "in
   flight behind A" in the depot only when it was appended before A's event
   began.
7. **What is left is one failure that the in-process cluster cannot make with
   this gate: a batch that Rama forces to fail with no restart, its rows
   dropped.** The probe of 12 October made it with a wait inside the event,
   the thread free ("Detected stalled streaming batch. Forcing failure and
   moving on to next batch."). The new gate has no wait. That line did not
   come once in these 110 runs. If such a batch failed with B's first attempt
   behind it, B would be decided before A: computed with the gate's own rule
   over the records of 60 runs, First would hold A refused and B accepted,
   and the run again A accepted and B refused. This was not run.
8. **What a maker is told is a separate matter, and it did go wrong.**
   "Callback failure" was given for an act that was accepted and whose rows
   stayed (A in every stall), for acts that were decided after a restart, and
   for acts that never reached the depot. A maker cannot tell these apart.
9. **The retry mode `:none` loses acts after a throw: 8 of 8 DIFFERENT.** The
   acts of the failed batch were never decided in First, and D was accepted
   where the run again refused it. Rama also refuses `:start-from` together
   with `:none`. `:individual` gave the same as `:all-after`, 9 of 9 SAME.
10. **Rama gives a stream event no offset of its source record, and no op to
    fail or queue a record again.** So an event cannot see that it runs ahead
    of an earlier record (K1 cannot be built).
11. **The stream timeout set very high turned the stall into a delay.** No
    retry, no "Callback failure" up to 8 s; the makers were answered when the
    stall ended. It cost nothing that the cost run could measure: 7.1 and 7.5
    ms for one act at 100 a second, and every append answered at 200, 400, 800
    and 1,600 a second, with and without it. A stall of 40 s still failed the
    makers' calls, after 10.0 s, by another limit.
12. **A proof can name the acts.** An act with no answer, or with a number
    that is not in the order of its layer's places in the partition, is out of
    its line. Folding the gate's pure rule over the partition in its order
    gave exactly the run again's answers (60 of 60), and named exactly the
    acts that differed (21 of 21, with the 2 runs of them that differed).

## Found

| # | Question | Finding | How |
|---|---|---|---|
| S1 | A throws once; B and C behind it | SAME 23 of 23. The worker is started again each time; from the throw to A's next begin 233 to 403 ms in the nine runs measured. First decided A, then B | run, `s1-together-end`, `s1-during-end`, `s1-behind-end`, `s1-together-start`, `s1-behind-start`, `load-throw`, `w2-s1-together`, `w2-load-throw`, `s1-try` |
| S1 | does B's append land | called while an earlier event held the thread: yes, 10 of 10. Called while A's own event ran: no, 0 of 3; the maker got "Callback failure" and the act is not in the depot. Sent right behind A: 3 of 6 | run, the same logs, the line "the partition of layer … in order" |
| S2 | A sleeps 5 s on the thread, timeout 3 s; B called 1 s in | SAME 3 of 3 (+3 with the sleep after the writes, +3 again). No line from Rama. After the stall A's second attempt begins, then B's first | run, `s2-t3-while`, `s2-t3-while-end`, `s2-t3-while-rule` |
| S2 | the same with Rama's own timeout, A sleeps 8 s | SAME 3 of 3 and 1 of 1. A's append failed after 8.0 s | run, `s2-own-while`, `s2-own-before` |
| S3 | a stall with the thread free | not run: the new gate has no wait | — |
| S4 | an answer that Rama cannot write | fatal, the worker is started again; SAME 6 of 6 | run, `s4-together`, `s4-behind` |
| S4 | the write of A's batch made slow by Rama's own test hook | the thread is held, as in S2; SAME 6 of 6; no line from Rama | run, `s4slow-commit-while`, `s4slow-commit-before`, `hooks-look` |
| S4 | a module update while 200 acts a second arrive | all 1,612 appends answered, none ran twice; SAME 3 of 3 | run, `load-update` |
| S4 | a failed replication | not reachable in process | read, `27-testing.md:63` |
| S5 | B before A's event begins | SAME 10 of 10; B's first attempt before A's second, each time | run, `s2-t3-before`, `s2-t3-before-rule`, `s2-own-before`, `w2-s2-before` |
| S5 | B while A sleeps | SAME, see S2 | run |
| S5 | B after the timeout has passed, A still asleep | SAME 3 of 3 | run, `s2-t3-after-deadline` |
| S5 | B when A has woken | SAME 3 of 3 | run, `s2-t3-after-wake` |
| — | many acts in flight behind the stall | SAME 3 of 3 at 200 a second (1,614 acts), 2 of 2 at 1,000 a second (8,014 acts), 1 of 1 with two workers | run, `load-stall`, `load-stall-1000`, `w2-load-stall` |
| K1 | the offset of the source record inside the event | cannot be built: no op, no source option, no dynamic var | read, the jar's names (`logs/names-stdout.txt`); run, `look` |
| K2 | the stream timeout very high | the stall is a delay; SAME 7 of 7; no cost measured | run, `k2-high-while`, `k2-high-before`, `k2-high-long`, `cost-plain`, `cost-timeout-high` |
| K3 | `:individual` | as `:all-after`; SAME 9 of 9 | run, `k3-individual-while`, `-before`, `-throw` |
| K3 | `:none` | cannot stand with `:start-from`. Stall: SAME 6 of 6. Throw: DIFFERENT 8 of 8, acts never decided | run, `k3-none-while`, `-before`, `-throw`, `-throw-behind`, `-throw-rule` |
| K4 | Mastodon | `retryAllAfter` on the sources whose order matters; no option set; no offset used | read, see below |
| rule | names the acts out of their line | nothing in 102 runs with nothing differing; in the 8 that differed, the acts with no answer, and D stood behind them | run, the lines RULE and FOLD of every log |

## S1: a throw (`logs/s1-*.log`, `logs/load-throw.log`)

A's event throws once, on its first attempt. The flag is the plan in the atom
`probe.gate/plan`. The throw stands at the end of the event, after its three
writes (`:throw-at :end`), or at its start, before its reads (`:start`).

Three ways of having B and C behind A:

- `:together`: an earlier event H holds the thread for 400 ms. A, B and C are
  called meanwhile, 60 ms apart. All three are in the depot before A's event
  begins.
- `:during`: A's event sleeps 300 ms and then throws. B and C are called while
  it sleeps.
- `:behind`: A, B and C are sent one behind the other by the client's call
  that does not wait.

Run, `s1-together-end.log`, the first of three (the lines of H and the
"ends" lines left out):

```
     2894.4  A attempt 1 begins on task 1
     2894.5  A attempt 1 decides: accepted, number 117435066838745088
     2894.6  A attempt 1 THROWS on purpose at the end of the event
     3253.5  the task global of task 1 is made ready again (time 2): the worker was started again
     3297.6  E attempt 2 begins on task 1
     3297.6  E attempt 2 finds its answer there and does nothing
     3297.7  A attempt 2 begins on task 1
     3297.8  A attempt 2 decides: accepted, number 117435066838745088
     3298.1  B attempt 1 begins on task 1
     3298.2  B attempt 1 decides: refused, number 117435066842677248
     3298.4  C attempt 1 begins on task 1
     3298.5  C attempt 1 decides: accepted, number 117435066846609408
```

- The worker is started again, all four task globals, every time. Rama's
  lines (run, `s1-together-end-rama-lines.log`): "Unexpected throwable! Will
  be treated as a fatal! {:event-tags [:streaming :gate :topology-event
  :execute-batch]}" and "One or more monitored components have become
  unhealthy! Triggering system shutdown. {:unhealthy-component-keys
  [:task-thread-systems]}". With two workers only the two tasks of the one
  worker were made ready again (run, `w2-s1-together.log`).
- A's first attempt had decided "accepted" and written. Its rows were gone:
  the second attempt decided again, with the same number, since the number
  comes from `:time`.
- E, decided and answered before, ran again and found its answer. That is the
  finding of 12 October: a restart runs again every record since the progress
  was last written.
- B's first attempt did not begin before the throw in any run. After the
  restart: A, then B, then C.
- First decided A before B in 23 of 23, the same as Again. Verdict lines, for
  example: `VERDICT s1-together-end run 1 of 3 · SAME · First decided ["E" "A"
  "B" "D"] · Again decided ["E" "A" "B" "D"] · workers of First started again:
  yes`.

**What became of B's append.** All three makers were told "Callback failure"
in every run. Whether the act was in the depot differed:

| mode | B and C in the depot | run |
|---|---|---|
| `:together`, throw at the end | 3 of 3, and 3 of 3 with two workers, 1 of 1 in the first try | `s1-together-end.log`, `w2-s1-together.log`, `s1-try.log` |
| `:together`, throw at the start | 3 of 3 | `s1-together-start.log` |
| `:during`, throw at the end | 0 of 3 | `s1-during-end.log` |
| `:behind`, throw at the end | 1 of 3 | `s1-behind-end.log` |
| `:behind`, throw at the start | 2 of 3 | `s1-behind-start.log` |

An append that was called while A's own event held the thread never landed:
the restart took it. So after a throw "Callback failure" means one of two
things, and the maker cannot tell which: the act is in the depot and will be
decided, or it is nowhere.

**Under load** (run, `load-throw.log`, three runs): four makers send 200 acts
a second for 6 s, four acts on each key, all `:replaces nil`. One second in, A
is sent and throws. 1,214 appends were called; 22 to 26 gave "Callback
failure"; the depot held 1,190 to 1,194 acts; 17 acts ran more than once.
Every answer, row and last number was the same in First and in Again.

## S2: a stall with the thread held (`logs/s2-*.log`)

A's event calls `Thread/sleep` on the task's own thread, on its first attempt
only. `topology.stream.timeout.seconds` is set to 3 for the module, by
`set-launch-topology-dynamic-option!` in the module's definition, as the probe
of 12 October set it. A sleeps 5 s. B is appended with `:ack` from a thread of
its own, 1 s after A began to sleep. This is the main run.

Run, `s2-t3-while-rule.log`, the first of three:

```
     2509.3  A attempt 1 begins on task 1
     2509.3  A attempt 1 sleeps 5000 ms on the task's thread, at the start of the event
     7509.4  A attempt 1 wakes
     7509.6  A attempt 1 decides: accepted, number 117435166514741248
     7509.9  A attempt 1 ends
     7529.5  A attempt 2 begins on task 1
     7529.5  A attempt 2 finds its answer there and does nothing
     7529.6  A attempt 2 ends
     7529.7  B attempt 1 begins on task 1
     7529.8  B attempt 1 decides: refused, number 117435166580539392
     7529.9  B attempt 1 ends
  the :ack append of A   called at    2505.9, came back after   5021.8 ms with CallbackException: Callback failure
  the :ack append of B   called at    3510.5, came back after   4023.7 ms with {}
  A    First: accepted  number 2506:0     attempt 1   | Again: accepted  number 2506:0     | same
  B    First: refused   number 3510:0     attempt 1   | Again: refused   number 3510:0     | same
  the order of the decisions in layer L00 by number · First: ["E" "A" "B" "D"] · Again: ["E" "A" "B" "D"]
```

- **What Rama logs: nothing.** The JVM's output of a stall run holds the
  probe's own lines and no line of Rama's, at any level (run, the
  `-stdout.txt` of the seven scenarios `s2-t3-*` and `s2-own-*` of the first
  batch; every `s2-*-rama-lines.log` is empty). The line of 12 October,
  "Detected stalled streaming batch", did not come, in any of the 110 runs.
  The only mark of the failure is what the maker's call gave.
- **Which begins first after the stall: A's second attempt**, 20 ms after A's
  first attempt ended, and B's first right behind it. So in 28 of 28 runs of
  the windows "while", "after the timeout" and "when A has woken" in which A
  ran twice (22 of them with `:all-after` and the sleep in the event, 3 with
  `:individual`, 3 with the sleep in the write of the batch).
- **A's first attempt is not lost.** It decides when it wakes, and its rows
  stay: the second attempt finds the answer, and the answer that First holds
  at the end carries attempt 1. Rama had failed the record, since its time
  was up, but the batch was written.
- **The tables are the same in First and in Again**, 3 of 3. With the sleep
  after the writes (`s2-t3-while-end.log`) the same, 3 of 3.
- **What the makers were told.** A: "Callback failure" when it woke, after
  5.0 s, in 32 of 32 runs of S2 and S5, though A was accepted and holds the
  row. B: `{}` after 4.0 s, the rest of A's sleep.
- **B was not in the depot while A slept.** Its call waited for the thread.
  That is why its own time was not up when A woke, and why it ran once.
- **With Rama's own timeout** (no option set; 5 s as Rama's line of 12 October
  gave it) and a sleep of 8 s: the same, 3 of 3. A's call failed after 8.0 s
  (run, `s2-own-while.log`).

**Under load** (run, `load-stall.log`, three runs): 200 acts a second for 8 s;
one second in, A is sent and sleeps 5 s. 1,614 appends: one "Callback
failure", A's; 1,613 `{}`. The calls took 1.1 s in the middle and 5.0 s at
most. One act ran twice, A. Every answer the same in First and Again. At 1,000
a second (run, `load-stall-1000.log`, two runs): 8,014 acts, 5 and 10
"Callback failure", 1 and 8 acts ran twice, the longest call 10.0 s, every
answer the same.

## S3: a stall with the thread free — not run

The new gate has no wait inside an event, so this cannot happen in it, and the
task said not to build it. It is the failure in which the probe of 12 October
saw "N+1 attempt 1 begins, N attempt 2 begins, N+1 attempt 2 begins"
(`before-real-data/stream-probe/RESULTS.md`, question 3 (d)), and the only one
known in which Rama drops a batch's rows without starting the worker again.
See "Not settled".

## S4: a failure that is neither

**An answer that Rama cannot write** (run, `s4-together.log`, `s4-behind.log`).
On A's first attempt the answer carries a plain `java.lang.Object`. The event's
code ends without a throw. The failure comes when the batch is written:
"Serializer not defined for type class java.lang.Object", and then the same
two lines as for a throw, with the same tags `[:streaming :gate
:topology-event :execute-batch]`. The worker is started again.

The notes of `s4-together.log`, first run, put on fewer lines:

```
A attempt 1 begins · A attempt 1 decides: accepted · A attempt 1 ends
B attempt 1 begins · B attempt 1 decides: refused · B attempt 1 ends
the task global of task 0 is made ready again (time 2): the worker was started again
A attempt 2 begins · A attempt 2 decides: accepted
B attempt 2 begins · B attempt 2 decides: refused
C attempt 1 begins · C attempt 1 decides: accepted
```

Here B's first attempt ran in A's batch, read A's row before it was written
out, and was refused. The whole batch was dropped. SAME 6 of 6. So a failed
write is not a failure "with no restart": it is a throw.

**The write of the batch made slow** (run, `s4slow-commit-while.log`,
`s4slow-commit-before.log`, `hooks-look.log`). Rama's jar holds functions
named `hook:…` that do nothing; Rama's own code calls them at set points. They
are not a public API. Eight were replaced by a function that counts its calls
(`src/probe/hooks.clj`). One of them is called through its name:
`hook:replog-append-all`, twice for the one act that was watched, 0.4 ms and
2.1 ms after the maker's call, on the task's own thread (the event's thread
and the hook's thread have one name, "… DTT 1 0-task-group-exec-0"). The
other seven were never called. The scenario makes
the call of that hook that follows A's event sleep 5 s, with the timeout at 3
s. This puts the stall into the write of A's batch, not into A's event. The
thread is held, as in S2. No line from Rama. A and B were written when the
sleep ended; their second attempts found their answers. SAME 6 of 6.

So in process the end of a batch runs on the task's thread and waits for
nothing. No hook was found that lets it wait with the thread free.

**A module update** (run, `load-update.log`, three runs). `update-module!`
with the same module, one second into 8 s of 200 acts a second. The update
took 3.5 to 3.7 s. All four task globals were made ready again. All 1,612
`:ack` appends gave `{}`; no act ran twice; every answer the same in First and
Again. Rama wrote 12 ERROR lines of `rpl.shaded.kafka.log.Log` ("Could not
find offset index file corresponding to log file …") at the updates; they
were not looked into.

**A failed replication: not reachable in process.** Read, `27-testing.md:63`:
"The replication factor cannot be configured for a module on an
`InProcessCluster` … So all modules on an `InProcessCluster` run with a
replication factor of one." Read, `21-replication.md:41`: a leader "replicates
that entry to followers before making the changes from that entry visible",
"e.g. at the end of a stream topology batch"; `:79`: when too few followers
are in sync, "Stream processing that goes through that task group will fail."
With one copy there is no follower to wait for.

## S5: the window (`logs/s2-t3-*.log`)

The shape of S2, timeout 3 s, A sleeps 5 s. Only the moment of B's append
changes.

| B is called | what begins first after the stall | First and Again | run |
|---|---|---|---|
| before A's event begins (an earlier event holds the thread 400 ms; A and B are both in the depot when A begins) | B's first attempt, then A's second, then B's second | SAME 3 of 3, and 3 of 3 again, 1 of 1 with Rama's own timeout, 3 of 3 with two workers | `s2-t3-before`, `s2-t3-before-rule`, `s2-own-before`, `w2-s2-before` |
| 1 s after A began to sleep | A's second attempt, then B's first | SAME 3 + 3 + 3, 3 with Rama's own timeout, 3 with two workers | `s2-t3-while`, `-while-end`, `-while-rule`, `s2-own-while`, `w2-s2-while` |
| 4 s after A began to sleep, 1 s after the timeout had passed | A's second, then B's first | SAME 3 of 3 | `s2-t3-after-deadline` |
| when A has woken | A's second, then B's first | SAME 3 of 3 | `s2-t3-after-wake` |

No window gave DIFFERENT.

The first window is the one to look at. Run, `s2-t3-before-rule.log`:

```
      752.0  A attempt 1 begins on task 1
      752.0  A attempt 1 sleeps 5000 ms on the task's thread, at the start of the event
     5752.1  A attempt 1 wakes
     5752.3  A attempt 1 decides: accepted, number 117435169239728128
     5752.5  A attempt 1 ends
     5755.6  B attempt 1 begins on task 1
     5755.7  B attempt 1 decides: refused, number 117435169243660288
     5755.9  B attempt 1 ends
     5760.6  A attempt 2 begins on task 1
     5760.7  A attempt 2 finds its answer there and does nothing
     5760.7  B attempt 2 begins on task 1
     5760.7  B attempt 2 finds its answer there and does nothing
  the :ack append of A   called at     346.6, came back after   5410.7 ms with CallbackException: Callback failure
  the :ack append of B   called at     406.7, came back after   5350.5 ms with CallbackException: Callback failure
```

This is the order of 12 October: N+1's first attempt begins before N's second.
It is harmless here for one reason only: A's first attempt had been written.
B read A's row and was refused. Both makers were told "Callback failure";
both acts had been decided, in the depot's order.

That B was in the depot before A's event began is derived, not read off the
depot: B's own time ran out too (its maker got "Callback failure" and Rama
ran it twice), so its clock had begun before the sleep. In the other windows
B's time did not run out.

## The candidates

No scenario with `:all-after` gave DIFFERENT, so there was nothing to close in
process. Each candidate was still examined for what it does.

**K1, the offset of the source record: cannot be built.**

- Read, the names of the loaded jar (`logs/names-stdout.txt`, made by
  `clojure -M:names`): `com.rpl.rama.ops` has 14 public names; the ones that
  tell an event where it is are `current-task-id`, `current-microbatch-id`
  (microbatch only), `current-topology-type` and `current-random-source`.
  None gives an offset.
- Read, `javap` of `com.rpl.rama.module.StreamSourceOptions`: a stream source
  takes a source id, a retry mode, a start, and an aggregator for
  `ack-return>`. Nothing that passes the offset on.
- Run, `look.log`: on the thread of a stream event no dynamic var is bound
  (0).
- Read, the same names: `com.rpl.rama` has no op that fails a record or queues
  it again. A throw is the only way to fail it from inside, and a throw
  starts the worker again.
- From outside, a client reads positions by two calls,
  `foreign-depot-partition-info` and `foreign-depot-read` (run, in every
  scenario here; found on 12 October).

**K2, `topology.stream.timeout.seconds` very high (3,600): the stall becomes a
delay.**

- Run, `k2-high-while.log` (A sleeps 5 s) and `k2-high-before.log` (A sleeps 8
  s, B in the depot before A begins): each act ran once. No second attempt.
  The `:ack` appends of A, B and C gave `{}`, after 5.0 s and 4.0 s, and after
  8.4 s and 8.3 s. SAME 6 of 6.
- Run, `k2-high-long.log`, A sleeps 40 s: the three `:ack` appends gave
  "Callback failure" after 10,000.8, 10,000.7 and 10,000.5 ms. The gate still
  ran each act once, in order, when the sleep ended. SAME 1 of 1. So another
  limit of 10 s stands between a maker and its answer. Which option it is was
  not found; read, `26-all-configs.md:71` names
  `foreign.depot.operation.timeout.millis`, "Timeout to use for foreign depot
  operations, including appends".
- What it changes in S2: not the order, which was the same already. It takes
  away Rama's second run of the record and the wrong "Callback failure" for
  stalls under 10 s.
- What other failure with no restart is then left: none that could be made in
  process. Derived, from 12 October: the limit of the forced failure of a
  stalled batch is this same option ("{:time-limit-nanos 3000000000}" with the
  option at 3), so a very high value would also keep Rama from forcing a slow
  batch to fail. Not run.
- Its cost (run, `cost-plain.log`, `cost-timeout-high.log`; one layer, every
  act accepted, four makers, the client's call that does not wait):

| acts a second | plain: answered · middle · 99 of 100 under | timeout 3,600: answered · middle · 99 of 100 under |
|---|---|---|
| 100 | 1,000 of 1,000 · 7.12 ms · 12.70 ms | 1,000 of 1,000 · 7.51 ms · 12.88 ms |
| 200 | 2,000 of 2,000 · 5.89 ms · 8.84 ms | 2,000 of 2,000 · 6.19 ms · 11.16 ms |
| 400 | 4,000 of 4,000 · 5.27 ms · 8.42 ms | 4,000 of 4,000 · 5.29 ms · 8.39 ms |
| 800 | 8,000 of 8,000 · 4.94 ms · 10.25 ms | 8,000 of 8,000 · 4.99 ms · 9.71 ms |
| 1,600 | 16,000 of 16,000 · 7.69 ms · 13.89 ms | 16,000 of 16,000 · 7.49 ms · 13.68 ms |

  No ceiling was met: every append was answered at the highest rate asked.
  The two columns do not differ beyond what two runs differ.

**K3, another retry mode.**

- `:individual` (run, `k3-individual-while.log`, `-before.log`, `-throw.log`):
  the same notes as `:all-after`, the same order after the stall, SAME 9 of 9.
  Read, `11-stream-topologies.md:158`: "Rama will try its best to retry
  records in the order in which they were appended, but … it can't guarantee
  this."
- `:none` cannot be written as the gate's source is: "IllegalConfigException:
  start-from option disallowed for streaming with retry-mode = none" (run,
  `logs/suite-k3-stdout.txt`, nine launches). The runs used `{:retry-mode
  :none}`; First is launched on an empty depot, so its end is its beginning.
- `:none` in the stall (run, `k3-none-while.log`, `-before.log`): A is not run
  again; its first attempt had been written; SAME 6 of 6. A's maker still got
  "Callback failure".
- `:none` after a throw (run, `k3-none-throw.log`, `-throw-behind.log`,
  `-throw-rule.log`): **DIFFERENT 8 of 8.** After the restart A, B and C were
  never decided. D, sent later, was accepted in First; in Again A is accepted
  and B and D refused. `the row at ["L00" "k"] · First holds the act "D" ·
  Again holds the act "A"`. What is lost: every act of the failed batch.

**K4, the docs and Mastodon's source (read).**

- Mastodon (`/mnt/data/projects/research/softland/rama-mastodon-time-2026-10-13/raw/`)
  uses `StreamSourceOptions.retryAllAfter()` on eight sources:
  `backend/src/main/java/com/rpl/mastodon/modules/Relationships.java:141`,
  `:205`, `:320`, `:476`, `:499`, `:508`, and `TrendsAndHashtags.java:176`,
  `:384`. One source has `retryNone()`: `Core.java:775`. Its other stream
  sources take Rama's own mode.
- It sets no dynamic option in the modules: no hit for `topology.stream`,
  `max.executing`, `timeout` or a dynamic-option call under `backend/` or
  `api/`.
- It uses no depot offset: the only hits for "offset" are a list helper
  (`MastodonHelpers.java:170`–`178`).
- Against a client that appends the same thing twice it keeps a table from
  the append's UUID to the id it made (`Relationships.java:136`–`139`). That
  is the same means as the gate's read of `$$answers`.
- So Mastodon's whole answer to order after a failure is the retry mode.
  Read, `11-stream-topologies.md:162`: with "all after" "a failed record will
  cause all records after it on the same depot partition to also be retried
  (even if they already succeeded). If the order of processing is critical …
  this mode is appropriate."
- Read, `11-stream-topologies.md:164`: "Rama also fails a record if its event
  tree doesn't succeed within a configurable timeout." `:168`: timeouts "are
  tracked on depot partitions at the root of event trees." `:170`: "if an
  exception is thrown or the process crashes during execution of a stream
  topology event, all PState writes during that batch of execution will be
  discarded."
- Read, `11-stream-topologies.md:322`:
  `depot.ack.failure.on.any.streaming.failure` decides whether an `:ack`
  append fails when the topology fails on the first attempt, "or if it should
  give stream topologies a chance to succeed on retries". It bears on what a
  maker is told, not on the order. Not run.

## How a proof could name such acts

From a client: read the layer's partition in order, each record with its
position (`foreign-depot-partition-info`, `foreign-depot-read`), and read each
act's number from the answers. The code is `named-by-the-rule` and `fold-gate`
in `src/probe/order.clj`; the lines RULE, FOLD, PURE and IF of each log are its
output.

**The rule.** Within one layer, numbers rise with the places in the
partition. An act is out of its line when

1. the depot holds it and it has no answer, when the layer is quiet; or
2. an act of its layer stands before it with a higher number, or stands
   behind it with a lower number.

Both acts of a swapped pair are named, since both answers are other than a
run again gives. Every act of the layer that stands behind the first named
one may also differ; the rule does not say which of them do.

**The fold says which.** The gate's rule is a pure function. Folded over the
partition in its order, from a client and with no cluster, it gives what a run
again holds. Each answer of the first module that is not the fold's answer is
named.

What the runs show:

- **No failure, and every failure with `:all-after`:** the rule named nothing
  and nothing differed, 102 of 102 (run, the line RULE of each log; for
  example `none.log`, 412 acts, and `load-none.log`, 2,012 acts).
- **The fold gives the run again's answers:** 60 of 60 runs in which it was
  checked (run, the line PURE: "the gate's rule folded over the partition in
  its order gives exactly the answers that Again holds: true").
- **A run that differed** (run, `k3-none-throw-rule.log`, retry mode `:none`,
  a throw): the rule named A, B and C, by its first part. D stands behind A
  in the layer and differed too. `RULE names: 3 ["A" "B" "C"] (with no
  number: 3 · with a number out of its line: 0) · acts that stand behind the
  first of them in their layer: 2 · the acts whose answer differs: 4 · the
  rule names exactly those: false · every one of those is named or stands
  behind a named one: true`. The fold named exactly the four: `FOLD … 4 ["A"
  "B" "C" "D"] · exactly the acts whose answer differs from Again: true`. In
  all 8 runs that differed, every differing act was named or stood behind a
  named one; in the 2 of them that had the line FOLD, the fold named exactly
  the differing acts. The fold was exact in all 21 runs that had the line.
- **The swap of S2 did not happen, so the second part of the rule was shown
  only by computing** (derived; the line IF of 60 runs). The records of A's
  partition were folded with B moved in front of A, which is the order of 12
  October with A's rows dropped: `IF B had been decided before A in First
  (computed with the gate's rule, not run): A refused number 3510:1 · B
  accepted number 3510:0 · answers that would differ from Again: ["A" "B"] ·
  the rule would name: ["A" "B"] · exactly those: true`. 60 of 60. D's number
  would not move, since `:time` × 65536 is far above the layer's last + 1.

## Not settled

- **Whether a batch of the new gate can be forced to fail with no restart, its
  rows dropped, while a later act's first attempt stands behind it.** This is
  the one case in which the order would differ (the line IF above), and it
  could not be made in process. In process the end of a batch runs on the
  task's thread and waits for nothing. In a real cluster a batch waits for its
  followers at its end (read, `21-replication.md:41`). Whether that wait can
  be long enough, or can come after a stall of the thread, so that Rama's
  check of 12 October ("Detected stalled streaming batch. Forcing failure and
  moving on to next batch.") fires on a batch of this gate, was not run. It
  needs a cluster with more than one copy, or Rama's own word.
- Whether a held thread followed by that wait counts as a stalled batch. In
  process A's batch was written at once when the thread was free, 44 of 44
  (38 stalls in the event, 6 in the write).
- What a leader that changes does to acts in flight. Not reachable in process.
- Which limit failed the makers' calls after 10.0 s (`k2-high-long.log`,
  `load-stall-1000.log`).
- What a maker should do with "Callback failure". Three different states gave
  it. This probe only counted them.
- `depot.ack.failure.on.any.streaming.failure` was read, not run.
- Rama's seven other `hook:` functions were replaced and never called; why,
  and whether another build of Rama calls them, was not looked into. The hook
  that was used is no public API; the two scenarios that use it show what
  Rama 1.6.0 did here and promise nothing.
- Every run used one partition's acts on one task. Two tasks on one thread
  were run only in the five `w2-…` scenarios (11 runs).
- The 12 ERROR lines at a module update were not looked into.
- One thing that was thought of and not run, and that is in no doc: the
  failure of 12 October drops A's rows but not what the task keeps in memory.
  An event could note there which acts began, and an act that finds an
  earlier one of its layer begun and unanswered could decide that one first.
  It could not be shown in process without the wait that S3 leaves out.
