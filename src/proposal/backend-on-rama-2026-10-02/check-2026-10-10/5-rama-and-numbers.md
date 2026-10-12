# Part 5 — what Rama is assumed to do, and every number

Assumption check of 10 October 2026. One of six independent sessions.
This file is appended to as each finding gets its receipt. A run that is cut
off leaves what it had found. Nothing in the Softland repository was changed.
No cluster was started and no proof was run: this part reads what was written
about runs.

Marks used below:
- DOC = traced to a line of the local Rama docs (reference/rama/docs/, an
  extract of the public docs made 22 March 2026) or to the Rama skill's
  reference pages (.claude/skills/rama/references/).
- RUN = traced to a written result of a run, with the place it is written.
- NOBODY = no doc line and no run found by this session.
- SID = a line the memory tool shows he typed, with its id.
- SESSION = a session's text or figure.

Status: FINISHED. Findings follow in the order they were receipted (F-A to
F-L). The list ordered by bite, what nobody asked, and what was read and not
read are at the end.


---

## What I read first (the full list is at the end)

- `.claude/skills/rama/SKILL.md` whole (the repo's rule before reading Rama).
- `reference/rama/docs/README.md` whole and `reference/README.md` (Rama row).
  The local docs are an extract "Generated: 2026-03-22"; the index says "An
  exact Rama release for the extraction is not recorded there". `deps.edn`
  pins `com.rpl/rama` 1.6.0. So a doc line is evidence for "what Rama's public
  docs said in March", not for 1.6.0 itself.
- `src-store/README.md` whole (355 lines).
- `ARCHITECTURE-STE100.md` whole (1257 lines).

---

## F-A. "The runtime is the only Rama client" is nobody's fact about Rama. Rama ships an HTTP door of its own.

**What it is.** ARCHITECTURE-STE100.md §1.3 "The runtime ... is the only Rama
client. [DERIVED]" and §3.2.2 "The runtime is the only Rama client. No other
program connects to the module. [DERIVED]" (the lines Sid quoted, 386). §3.3.3
then leans on it: "a value in an acts depot is safe only if the door seals it
(?3), or because the runtime is the only client." §8 correction 18 and §3.3.14
lean on it too: private PStates are "the choice that lets Rama keep the one
exit".

**Whose it is.** A session's (its own mark is [DERIVED]). No Rama doc line says
it and no run tested it.

**The check (DOC).** Rama's docs describe a second way in that none of the
papers names. I searched `src-store/README.md`, every `.md` in
`backend-on-rama-2026-10-02/`, `decisions/` and both build PROGRESS pages for
`REST`, `curl`, `8888`, `supervisor`, `conductor`: no hit about Rama.
- `reference/rama/docs/24-rest-api.md:7`: "Rama has a built-in REST API for
  doing depot appends, PState queries, and query topology invokes with HTTP
  requests. This allow Rama modules to be read and written to from any
  programming language, as well as allowing ad-hoc queries to be done with
  tools like `curl`."
- `24-rest-api.md:24`: "REST API requests are handled by Supervisor daemons.
  The Conductor also implements the same REST API on the Cluster UI port
  (default 8888)".
- `19-operating-rama.md:213`: "The minimal information `RamaClusterManager`
  needs to know is the host and port of the Conductor node". No name, no
  password.
- I searched all 33 local doc pages for authenticat / authoriz / security /
  TLS / SSL / firewall / credential / access control. The only hits are the
  tutorial's "password hash" example. The docs describe no way for Rama to ask
  who is connecting. (This agrees with what the session found today: "Rama
  asks no name of whoever connects". It is checked by absence in the March
  extract, not against 1.6.0's jar.)
- `24-rest-api.md:341`: "PState reactivity is not exposed yet" over REST, so
  the HTTP door reads and appends but cannot subscribe.

**So in plain words.** "Who can connect to Rama" is: anything that can open a
socket to the Conductor (port 1973, or the UI port 8888) or to a Supervisor.
Rama does not make the runtime the only client. A network rule does, and
nobody has written that rule down as something the store depends on. With the
default setup, any program that reaches the Conductor's port can open a client
with no name, append to either acts depot and read any PState that is not
private. `curl` to port 8888 can post an append and a select too (whether
Rama's JSON can carry this store's keywords and bytes, I did not check).

**What Sid loses if it is wrong, and when.**
- Now, on one machine: nothing.
- At the first deployment where the cluster's ports are reachable by anything
  but the runtime: every statement of the form "the gate is the authority
  because the door did X first" and "the check is in the runtime" is void for
  that caller. Decision 8 of 7 October already says "anything can append to a
  depot without the door", so the write side was built for it (the gates
  check). The read side was not: the reads sheet's choice between "the store
  checks" and "the runtime checks" is the same question, and the PStates built
  so far are, as far as the papers say, readable by a client (§9.2 ?11: "The
  door and Inland read the PStates directly now").
- It lands when reads are built, and it is cheap to change until then.

**What would settle it.** One line from Sid on what is allowed to reach the
cluster's ports (only our runtime processes on a private network, or more),
written as a requirement of the deployment. Then one check on a real cluster:
does 1.6.0 let the REST API be turned off or bound to a private interface
(`26-all-configs.md` has no REST option in the local extract; I found none).
This is a fact to check, not a decision to put to him.


---

## F-B. "The acts depot is the replay log; replaying the gate over it rebuilds the record" is a session's sentence, and the gates as built cannot be replayed.

**What it is.** `src-store/README.md`, decisions of 7 October, entry 2: "The
record is in the gates' PStates. Sid decided (F2 A). His reason: the record is
recomputable from the acts depot — an act proposed at a version is refused the
same way every time — so a second depot of decisions adds nothing. The acts
depots are the replay log." The same idea is used again as a reason in entry 4
of 9 October (K4 B: a bare word taken later "would change how old acts are
read when the record is built again from the log") and in entry 2 of 8 October
("each layer is made by one act, its first, so that the making of a layer is
in the acts depot (decision 2 of 7 October)").

**Whose it is.**
- Sid's own line, typed (‹27882354›, 7 Oct 07:26 UTC): "f2 - A, note to self-
  it is recomputable given the version a act was proposed at it will be
  rejected every time so storing it a new depotdoesn't make sense". His claim
  is narrow: a refusal is a function of the act and the version it was
  proposed at, so a second depot of decisions adds nothing.
- The wide claim is the session's, and it was inside the question he was
  answering (session text «802316c6», 7 Oct 06:35): "the acts depot *is* the
  replay log (every act, as sent), and replaying the gate over it rebuilds the
  record in order" and "the replay you want already exists in the acts depot".
  The memory tool shows "replay log" in no line he typed.
- No run tested a replay. No paper I read names one.

**The check.** Three receipts, one from Rama's docs and two from the built
code.
1. DOC, `reference/rama/docs/19-operating-rama.md:487-489`, on recomputing
   every PState from the depots in a new module: "This approach only works if
   your processing is deterministic, which may not be the case if your
   processing makes use of any random numbers (such as UUIDs)."
2. CODE, both gates read the wall clock inside the decision:
   `src-store/softland/store/module/streaming_gate.clj:175` and
   `microbatch_gate.clj:285`, `(System/currentTimeMillis :> *now)`. The stamp
   of every decision is made from it (`gate/stamp.clj`, `next-stamp`), and
   the 10-second rule refuses on it (`gate/decision.clj:84`). A replay gives
   every act a different stamp, and an act once refused
   `:carried-stamp-too-far-ahead` is accepted the second time.
3. CODE, both gates make each lock from fresh random bytes inside the
   decision: `streaming_gate.clj:126-132`, docstring "The bytes are made here,
   in the gate's event, at the write, and go to the locks PState only; they
   are never on a depot. Each call gives other bytes." (`microbatch_gate.clj:143-151`
   is the same.) The gate then accepts a sealed value only if that lock opens
   it: `gate/decision.clj:292-309`, `:lock-does-not-open-the-value`. A replay
   of a lock request makes other bytes, so every sealed value that follows in
   the depot is refused. With the seal on before the depot (Sid's decision 3
   of 6 October) that is every value.

**So in plain words.** As built, the acts depots plus the locks tables are the
store. The record cannot be rebuilt from the acts depots alone. This is not a
bug in the gates: a lock that could be rebuilt from the log would undo a
forget. The two things Sid decided (the record rebuilds from the log; a forget
destroys a lock for good) cannot both hold for the same value, and nobody put
that to him. Rama's docs name this exact trap.

**What Sid loses if it is wrong, and when.**
- At the first kept record. From then on any plan that says "fix the gate and
  replay" or "rebuild the tables from the log" loses every sealed value and
  changes every stamp. The stamps are carried by later acts in `:based-on`
  (decision 11 of 6 October), so the stamps in the depot no longer match the
  stamps in the rebuilt record.
- At scale: Rama's first road past N tasks (recompute in a new module) is
  closed to this store. Only the second road is open (`19-operating-rama.md:489`,
  "repartition the PStates directly"), which STE100 §3.3.8 already names.
- For backups: the locks tables and the record tables are primary data. A
  backup that keeps "the log, because everything rebuilds from it" keeps
  nothing readable.

**What would settle it.** A line from Sid on which of these he wants: (a) the
tables are primary and the depot is only the door's inbox and an audit of what
was sent; (b) the record must rebuild from the log, in which case the stamp
and the lock both have to come from the act or from a log of the gate's own
(and a forget must then be replayed too); or (c) both, per layer or per value.
Before he is asked: one probe on the in-process cluster that launches the
module a second time over the same depot contents and compares the two
records. That is a fact to check first.


**F-B, further receipts (added after reading the build's own papers).** The
build session found most of this on 7–8 October and wrote it down, in places
Sid is not asked to read:
- `BUILD-store-module.md` rule 19: "A rebuild of the record from the depots is
  not built and not tested here ... locks are kept through a rebuild and never
  made again; stamps would be new".
- `src-store/softland/store/README.md` rule 19: "What a rebuild would not give
  again: a lock's bytes, which are in no depot; a stamp, which holds the
  gate's clock at the decision; a refusal by rule 17; the order across a
  failed batch", and rule 9: between two depot partitions "it holds none ...
  A rebuild that reads the depot from its start cuts other batches, so the
  other act can have the earlier turn ... whether it is wanted is Sid's."
- `store-module/PROGRESS.md` item 5 says the same "for the receipt".
- How it reached Sid: as one bullet of five in a status reply («40cdb514»,
  8 Oct 03:13; his answer ‹8e510528› was "why do we have a flat hierarchy of
  the project files????"), as one line in a 13,303-character reply
  («380c05e8»; his answer ‹8573450f›: "so what next??? which one should i be
  printign???"), and in a file sent while he slept («c758a2ad»). It was never
  put to him as a question, and it is not a row of `OPEN-QUESTIONS.md`.
- The sheets written after the finding still lean on the rebuild as a fact:
  `g-door.txt:40` "The builders never trim it: your decision 2 of 7 October
  rests on that"; `g-reads.txt:369` "the depots are never trimmed ... So a
  list over all layers can be built from the two on a later day";
  `g-reads.txt` "Looking back" and "Finding things by a pattern" ("Its tables
  can be built later, from the depots and the record"); and the K4 reason
  recorded on 9 October. `src-store/softland/store/module.clj:34` still calls
  each depot "the replay log of those layers".
So this one is half caught: the builders know, the decisions page and the
sheets do not say it, and Sid has not been asked.

---

## F-C. "A batch later" was decided as "a few ms". In this build it is near a second. Part of that is Rama's; the extra batch is the gate's clock and random locks.

**What it is.** `src-store/README.md`, 7 October entry 4: "The base is a
microbatch layer; every other layer starts streaming. Sid decided (F4 C) ...
data subscribed to from another layer may arrive a batch later".

**Whose it is.** Sid's, with his number in it: ‹32f65085› (7 Oct 07:50 UTC)
"f4 -- Base:: Microbatch I think ... why should it be streaming??? instant vs
a few ms ... is fine to come a little bit delayed wdyt??". The question he
answered gave no size for "a batch later" (session text «802316c6»/«6631e4be»:
"instant on one thread vs room on many, a batch later").

**The numbers, and what each is.**
- 873 ms: RUN. `store-module/PROGRESS.md` item 18, "In the proof an answer of
  a microbatch act was readable 873 ms after its append, with nothing sent
  after it"; the review saw 885 and 893 ms (`FULL_SPEC_REVIEW.md:244,273`).
  In-process cluster, one machine, 4 tasks, an idle store, 3 readings.
- 887 ms: on the reads sheet (`g-reads.txt:242`, "Measured in this build").
  I found no paper that holds 887. The written runs are 873, 885 and 893. It
  is a session's figure until its run is named.
- "near a second" (`g-reads.txt`, last list): the same three readings, rounded.

**What Rama does here (DOC).**
- `12-microbatch-topologies.md:124`: a batch's changes show only after the
  commit on each task, and "changes on different tasks may become visible at
  slightly different times".
- `12-microbatch-topologies.md:128`: "A microbatch topology is not necessarily
  deterministic ... If you use `shufflePartition`, random numbers ... the
  results after a retry could be different than the previous version for the
  same microbatch ID ... the new version replaces the old version in the
  external view of the PState."
- `.claude/skills/rama/references/microbatch.md:30`: "a record that is
  appended to a depot immediately after a microbatch starts will not have its
  changes visible until two microbatches have run and finished."
- `microbatch.md:34` and `12-microbatch-topologies.md:159`: with no new data
  the runner sleeps (`topology.microbatch.empty.sleep.time.millis`, "one
  minute by default" in the skill's page) and polls for new data every 200 ms
  by default.

**What the build added (a session's ruling, `store-module/PROGRESS.md` item
15).** "An answer of a microbatch act can be read only after the next
iteration has closed its batch, because Rama can run a batch again after one
task has shown it ... A second attempt of a batch reads a later clock, so a
refusal for a stamp too far ahead, and with it a winner, can differ between
attempts; closing keeps a reader from seeing the first." So every answer in
the base waits one more batch than Rama asks, and the reason is that the
gate's decision is not the same on a second attempt (the clock and the random
lock bytes of F-B). Item 24 then says what this costs the reads: "After a
batch replaces a fact of a microbatch layer, its place holds no row of a
closed batch until that batch is closed, about one run. A place that is
replaced in every batch never holds one."

**What Sid loses if it is wrong, and when.** When reads are built. He chose
microbatch for the base on "a few ms"; what is built gives the base's answer
after about 0.9 s on an idle one-machine store. And if reads of facts are held
to closed batches, as the three built reads are, a place of the base that is
replaced in every batch never has a readable row (item 24; reads are not
built, so this is what the next build meets). The base is what every layer builds on, and by
entry 3 of 9 October the system's own tool layers are its neighbours. Under
load the wait is two batches, and a batch's length under load is not
measured. On a real cluster each batch also waits for replication to the
followers (`12-microbatch-topologies.md:124`: "The commit phase also
replicates all changes"), which no run here had.

**What would settle it.** Two things, in this order. A fact first: measure
the base's append-to-readable time under a steady load and with the two
pacing options set low (they are dynamic options, changed from the Cluster
UI), so the number is the store's and not a default's. Then a line from Sid
on what "a little bit delayed" may be for the base, in his number. If the
number is small, the cheaper road is to make a second attempt of a batch give
the same decision (keep each batch's clock, as rule 9 already says), which
removes the extra batch and the read hole together.

---

## F-D. No number of the store that is built has been measured. Every target and every measure on the sheets is the rig's, on one machine, with one copy of the data.

**What it is, and where.**
- The three limits: `src-store/README.md` 7 October entry 9; `g-store.txt`
  F9; `BUILD-store-module.md` rule 21; `g-reads.txt` W5 and W7 ("The store's
  target (F9): at least 100 acts each second for one person, each answered
  within 20 ms of the door's append, for the slowest 1 in 100").
- The throughput table: `ARCHITECTURE-STE100.md` §6.1.

**Whose each number is.**
| Number | Where | What it is |
|---|---|---|
| 1,000 acts/s on one task; 100 acts/s for one person at 20 ms for the slowest 1 in 100; locks at most 2x the bytes of the values | F9 | A session's proposal of 25 September ("assumed thresholds" in the rig's own notes, `BUILD_NOTES-numbers.md:309`; `g-store.txt`: "the three limits are a proposal of 25 September; none is ruled"). Sid's line ‹27882354›: "The limits are what we should be optimising for what we measured is starting point". He took them as aims. He gave none of the three numbers. |
| 300 / 2,228 / 3,362 / 5,192 acts/s; 4.7 to 43.9 ms | STE100 §6.1 row 1 | Measured on the rig, copied correctly (`BENCH_NOTES-stream.md:180-185`). |
| 226 / 842 / 1,062 / 1,198 acts/s; 15.5 to 236 ms | §6.1 row 2 | Measured on the rig, copied correctly (`BUILD_NOTES-numbers.md:296-303`). |
| 46 ms at 100 acts/s | §6.1, F9, BUILD rule 21 | Measured on the rig. It is the middle of three runs that gave 22.7, 46.1 and 67.0 ms (`BUILD_NOTES-numbers.md:304-306`). The papers give 46 with no spread. All three were over 20 ms (the nearest 22.7); the spread is 3x. |
| 254 ms at 1,000 acts/s | same | Measured on the rig, three runs 114 to 270 ms. Given with no spread. |
| "2.3 times the assumed limit" | §6.1 | Arithmetic, 46.1 / 20. |
| "about 60% of a core" | §6.1 | Measured on the rig ("at most 62%"). |
| 169 bytes of lock for each value | F9 (c), BUILD rule 21 | Measured on the rig. |
| "holds for values of 200 bytes, and not for values of 40 bytes" | `g-store.txt` F9 (c) | Arithmetic from 169 and 2x; the line where it turns is 85 bytes. |
| 480 to 770 acts/s on one task | `store-module/PROGRESS.md` item 6 | Arithmetic for the built store, from the Rama skill's rule of thumb for a disk read (0.3 to 0.5 ms). Below the aim of 1,000. "A measure will tell; none is taken yet." |
| 140 / 102 / 110 bytes of lock (streaming), 245 / 180 (base) | `store-module/PROGRESS.md` items 1 and 16 | Arithmetic "by the plan's arithmetic, and not by a measure"; 102 and 180 rest on RocksDB sharing key fronts, "(not verified)"; 110 is the plan's aim, a session's. |
| "missed for values under 55 bytes", "below about 90 to 120 bytes" | same | Arithmetic from the row above. |
| "3 disk reads, 4 row writes, 3 hops" for one accepted act in the base | «40cdb514», the plan | A count read from the plan, not from a run. |
| 50 ms · 100 ms · 2 s | `g-reads.txt` W5 option A | The sheet says it itself: "The three numbers are a session's, from nothing measured." |
| 26.6 ms key press to confirmed text; 7.6 ms | `g-reads.txt` W5 | July figures from the earlier app, "not measured again"; the 7.6 is Sid's "iirc". |

**The conditions all the measures share (RUN, as written).** In-process
cluster, one JVM, one machine (AMD Ryzen 9 9900X), 4 tasks, one worker,
replication factor 1 (STE100 §6.1 and §7; `rama-check-2026-09-25/RESULTS.md:3,
113`: "No replication (factor fixed at 1) and no way to kill a worker from the
test API"). No measure had: a network between the door and the cluster; a
second copy of the data; a proxy subscribed to anything; the seal and the
gate's opening of each value as built in `src-store`; a read on the way out.

**What Rama's docs say those missing things cost (DOC).**
- `11-stream-topologies.md:136`: a stream event's writes are seen "until after
  the event has finished and those writes have finished replicating".
  `21-replication.md:41`: the leader "replicates that entry to followers before
  making the changes from that entry visible". With more than one copy, every
  answer waits for a round trip to another machine and its disk.
- `14-depots.md:206`: an append with the full ack "waits for ... all colocated
  stream topologies to process the data, including replication of any updated
  PStates".
- `15-pstates.md:1230`: "`localTransform` calls process subscribers
  synchronously with the transform up to a time limit". A proxy's work is done
  on the task's one thread, inside the gate's write. Every watching screen is
  load on the same thread that decides acts. No paper I read says this, and
  no measure had a subscriber.

**What Sid loses if it is wrong, and when.** When reads are built, and again
at the first real cluster. The sheets ask him to set waits (W5) and to pick
where reads are checked (W3) and what tells the screen (W4) against "the
store's target ... 20 ms". That target is a September guess; the only measure
near it missed it by 2.3x with a 3x spread, on a rig that is an example, with
none of the three costs above in it. The estimate for the store that is built
is below its own aim of 1,000 before any measure. A choice among W3 and W4
changes the per-act work on the gate's thread, and nothing on the sheet says
by how much.

**What would settle it.** A measure of the built store (not the rig) under
the same three shapes the rig used, with the door in the path; then the same
with replication 2 or 3, which needs the answer to "can two nodes run here".
Sid's line on that was "since we are in dev phase it is fine to punt to later
when i go to production" (‹32f65085›). That punt is his; what he was not told
is that every latency number he is shown has replication left out. Before any
number is put to him as a target, say whose it is and from what.

---

## F-E. After a failure the streaming gate can decide a layer's later act before an earlier one, and the answer check then keeps the wrong order for good.

**What it is.** `src-store/softland/store/README.md:155-164`: "When something
throws while a batch of events runs, that batch is discarded, and a later act
of the layer can be decided before the failed one is retried ... Rama's
setting `topology.stream.max.executing.per.task`, at 1, closes the window ...
The cost is one act in each batch of a task, so each act waits for the
replication of the act before it; that is paid against the target of 1,000
acts a second on one task. The module leaves the setting at Rama's default.
The choice is Sid's."

**Whose it is.** A session's finding, derived from the docs, not run:
`PLAN_VALIDATION-streaming-gate-and-record.md:616` marks the fix "(*derived*)";
`FULL_SPEC_REVIEW.md:220`: "is said to close it ... I could not check the
setting"; `store-module/PROGRESS.md` item 13: "Not reached by any proof of
part 1: a retry of an event by Rama". The choice is named as Sid's and is in
no sheet and no row of the queue.

**What Rama says (DOC).** `11-stream-topologies.md:162`: in the "all after"
mode "a failed record will cause all records after it on the same depot
partition to also be retried (even if they already succeeded). If the order
of processing is critical ... this mode is appropriate." The gate uses that
mode (`streaming_gate.clj:154`). `11-stream-topologies.md:168`: "if any one of
those events throws an exception all the events will fail."

**Where two decisions meet.** Rama's "all after" puts order right by deciding
the later records again. Decision 7 of 7 October makes a gate "read its own
record by act-id before it decides, so a repeat is answered, not decided
twice". So the second pass finds the later act's first answer and keeps it.
The repair that Rama's mode offers is switched off by the rule that makes a
repeat safe. STE100 §6.1 still says of a stream layer "The layer has one
order. [RAMA DOCS]". That holds without a failure only.

**What Sid loses, and when.** At the first kept record, on any fault of a
machine or of Rama. A maker that sends A then B into its own layer, B standing
on A, can get B refused as stale and A accepted, and the refusal is kept. The
maker must send B again under a new act-id. No test shows it and no test
shows that the setting at 1 closes it, or what it costs.

**What would settle it.** A fact first: one run on the in-process cluster
that throws inside a batch holding two acts of one layer and reads both
answers, with the setting at its default and at 1, and the throughput at 1.
Then the choice is Sid's, as the builders wrote: one order for a layer always,
at that price, or the order of the depot only while nothing fails.

---

## F-F. The two runs of 2 October that moved the record into the tables have no raw files left. The only receipt is the table in the document being checked.

**What it is.** STE100 §7.5: "The files of the two runs are in temporary
folders. A restart removes them." Both paths are under `/tmp/claude-1000/...`.

**The check.** Neither folder exists today (`ls` on both: "No such file or
directory"; the machine came up at 2026-10-09 21:34). No copy of
`depot-append-probe` or `append-check` is in the repository or under
`/mnt/data/projects/research/softland/`. The 25 September check is kept
(`src/proposal/rama-check-2026-09-25/`, code, logs and `RESULTS.md`). The
door's probes are kept (`/mnt/data/projects/research/softland/door-build-2026-10-09/plan-probes`).

**What rests on the two lost runs.** Correction 1 and D2 (a failed attempt
keeps its depot append, so the record is in PStates: decision 2 of 7 October);
rule 9 of §5 "a gate commits first, and appends after"; "When an attempt
fails, Rama removes its clock tick. The next act can then get the same stamp";
the private-PState line ("a query topology in the same module read it. A
client read failed"); the 3 seconds of Q5. For the microbatch half the docs
agree by themselves (`12-microbatch-topologies.md:130`: depot appends from a
microbatch "currently do not have exactly-once semantics"). For the stream
half I found no doc line; it is the runs alone. The build's part 1 says it saw
the private-PState read again (`store-module/PROGRESS.md` item 12), with no
file named.

**What Sid loses, and when.** Nothing while the findings are true. But a
finding that cannot be run again cannot be checked against the next Rama
release, and the docs say of the microbatch case "this is on our roadmap".
The whole reason for F2 A is a behaviour that Rama's makers call temporary
for one gate.

**What would settle it.** Write the two probes again, into the repository
beside the 25 September check, with their logs. Not a decision.


**F-B, where the sentence came from (added).** `ARCHITECTURE.md:278-280`, the
drawing session's position of 2 October: "a depot of decided acts, because it
is Rama's source of truth, Rama never rewrites it, and replaying it gives the
same state, including past N tasks." That was said of a depot of *decided*
acts, which would carry each stamp and each answer, so a replay of it would
give the same state. The recheck corrected "never rewrites" (correction 9) and
moved the record into PStates (correction 1). Nobody then asked whether
"replaying it gives the same state" still held for the depot that was left,
the depot of acts *as sent*, where every decision has to be made again. On
7 October the property was handed to that depot in the question put to Sid.
`g-store.txt:28` on the same sheet says the opposite in its word list: a
PState "is kept on disk when it is written; it is not made again from a
depot". And `g-store.txt:102` gives as B's nature "The log is append-only by
its nature", which Rama's docs deny (`14-depots.md:288`: "A depot's records
can be migrated to new values or excised as part of a module update").

---

## F-G. "Forget destroys a lock and the value can never be unsealed" assumes that a delete in Rama removes the bytes. Rama keeps copies in four kinds of place; the papers name two in one line, and never backups.

**What it is.** `g-reads.txt` word list: "forget — the store destroys a lock;
what it sealed can never be unsealed." Decision 2 of 9 October (one lock seals
one value) and the whole seal rest on it. STE100 §3.3.12 has the one caveat:
"The bytes of a destroyed lock stay on the disk until compaction, on each task
and on each replica. [RIG: RIG.md:1577–1581]".

**Whose it is.** The two deletes are Sid's (the rulings file). That a destroyed
lock is gone is a session's reading of what a PState delete does. No run.

**What Rama does (DOC).**
- Replicas: `21-replication.md:24`, each piece of data is stored
  "replication factor" times; every copy holds the lock until its own disk
  compacts.
- Microbatch checkpoints: `12-microbatch-topologies.md:124`, "The latest
  versions for the last two microbatch IDs are always kept", by hard links on
  disk. The base's lock rows live in those.
- Backups: `22-backups.md:7,30`, "Rama can be configured to back up all module
  state to an external storage service ... Restoring a module to a previous
  state is a simple one-line command"; a backup "includes log segments for
  depots, SSTs and write-ahead logs for PStates". A backup taken before a
  forget holds the lock, and a restore brings it back beside the sealed value
  that the depot never dropped.
- The word "backup" appears in no paper of this build (searched `src-store`,
  the proposal folder, the sheets, both PROGRESS pages and the rulings file).

**Where it meets F-B.** Because the locks tables cannot be rebuilt from the
log, they must be backed up or the store is one disk fault from losing every
value. Every backup is a place where a forgotten lock still is. The stronger
the store's need to keep locks, the weaker "forgotten for good".

**What Sid loses, and when.** At the first forget after the first backup.
What he has said forget is for ("it is not recoverable", 23 September,
‹6304b683›) would be untrue for as long as any backup or uncompacted file
holds the lock, and nobody can tell a person how long that is.

**What would settle it.** A line from Sid on what "forgotten" must mean in
time (at once, within N days, or "as soon as the last backup ages out").
Before that, two facts: how Rama's backups age out, and whether a PState
delete can be forced through compaction (neither is in the local docs).

---

## F-H. "Frozen at the first kept record" is a session's rule, used about twenty times to say a thing must be decided now. Rama can rewrite kept records; what pins the bytes is the builders' digest.

**What it is.** The sheets mark questions "Freezes" and define it
(`g-reads.txt`): "a thing freezes on the day the first record is kept for
good: after that day a change of it splits the store into before and after."
Counts: g-reads 5, g-record 6, g-door 5, g-store 4, `src-store/README.md` 1.

**Whose it is.** A session's. The memory tool shows "first kept record" in no
line Sid typed; it first appears in session text of 23 September («5a5ac596»)
and reached him only as pastes.

**What Rama does (DOC).** `15-pstates.md:768-812`: "A PState can be migrated to
a new schema as part of a module update ... PState migrations take effect
immediately". `14-depots.md:288-290`: "A depot's records can be migrated to new
values ... A depot migration takes effect instantly regardless of the size of
the depot." So Rama itself does not freeze a form at the first record. The
papers use depot migration once, for excision (STE100 §3.3.12), and never for
changing a form.

**What does pin the bytes (CODE, a session's choice).** Each answer keeps the
SHA-256 of the thing as appended (`src-store/softland/store/README.md` rule 1;
`BUILD-store-module.md` rules 1 and 11), and each seal binds a value to its
place (rule 16). A migration that changes a kept act changes its digest and
breaks the seal's binding. So how hard a thing freezes was set by two builder's
rules, not by Rama and not by Sid.

**What Sid loses, and when.** Now. "It freezes" is the reason given for asking
him to settle things before he has used them (W1 "Freezes", H1, the class
words, the keys' namespace). Some of those may cost a migration, not a split.
He is paying decisions early against a cost nobody priced.

**What would settle it.** For each thing marked "freezes": say what would have
to be rewritten (the depot record, a table row, a digest, a sealed value) and
whether a Rama migration can do it. That is a fact to work out per item, not a
question for him. The one question that is his: whether an answer's digest
must match the bytes as first appended for ever, or the bytes as the store now
holds them.

---

## F-I. Three things about pushing changes that no sheet states, and that W3 and W4 turn on.

All three are DOC, none is run, and none is on the reads sheet.

1. **A proxy's work runs on the gate's thread, inside the write.**
   `15-pstates.md:1230`: "`localTransform` calls process subscribers
   synchronously with the transform up to a time limit, and any remaining
   subscribers are processed asynchronously". `15-pstates.md:724`: "An active
   `ProxyState` uses resources on the process of the subscribed PState
   partition ... memory to track the subscription and ... CPU to process
   diffs". W4 A ("a bell that names the place", one row and so one watch for
   each place shown; the sheet's own case is 5,000 places on one screen) and
   W4 C (the fact itself) both put per-watcher work on the one thread that
   decides acts for that task. The sheet prices A as "Alice's screen reads one
   place". It does not price the watches.
2. **A proxy can miss a change and not be told until the next one.**
   `15-pstates.md:1230`: "When the limit is reached, those subscribers are
   dropped from processing that diff and will resync the next time they
   receive a diff." `15-pstates.md:710`: "if something goes wrong – like a
   network error or overloaded cluster – a `ProxyState` can be forced to
   resync from scratch." STE100 §1.4 defines a proxy as "A client object that
   receives each change to one path". The docs do not promise each change. A
   bell row that changes once and is dropped leaves a screen stale until
   something else moves. This holds for a pushed value too.
3. **"Private" in the docs means the owning topology only.**
   `15-pstates.md:222`: "`makePrivate` makes the PState inaccessible for reads
   for anything other than the owning topology." STE100 §1.4 says "The
   topologies of the same module can read it", on the lost check of 2 October
   and on the build's note that "a query topology of the module does read a
   private PState" (`store-module/PROGRESS.md` item 12). Nobody ran the other
   case that the built code needs: each gate reads the *other* gate's tables
   (`streaming_gate.clj`, step 4: `local-select>` on `$$microbatch-facts` and
   `$$microbatch-task`). If the docs' sentence is exact for a stream or
   microbatch topology, W3 A (make the record private) breaks the check that
   tells which gate a layer belongs to.

**Whose.** The docs' lines are facts. That nobody put them beside W3 and W4 is
the sheet's.

**What Sid loses, and when.** When reads are built. He would pick among W3 A/B
and W4 A/B/C without the three costs that tell them apart on Rama: watchers
tax the deciding thread; a push is not a promise; private may not mean what
the option says.

**What would settle it.** Three small runs on the in-process cluster before
the sheet goes to him: (a) time of one gate event with 0, 100 and 5,000
proxies on the rows it writes; (b) a proxy under a low
`pstate.reactivity.queue.limit`, to see a missed change; (c) a stream
topology reading a private PState of another topology in the same module.

---

## F-J. "One task, one thread" and the per-task numbers.

**What it is.** `g-store.txt:27` "task — ... Each task has one thread";
`g-record.txt:22` "one of N slices of the store, each one thread"; STE100 §6.1
"A stream layer uses one thread only"; the aim "1,000 acts each second on one
task".

**The check (DOC).** `19-operating-rama.md:483-485`: "the number of threads
must be less than or equal to the number of tasks ... Having something like 16
tasks per thread is reasonable". So a thread can serve many tasks. A streaming
layer's ceiling is its thread, and its neighbours are every layer on every
task of that thread. The rig measured with 4 tasks on 4 threads. With N
"chosen for the next two years" and fewer threads than tasks, "per task" on
the sheets means "per thread, shared". `19-operating-rama.md:483`: "Currently
Rama does not support changing the number of tasks for a module" (STE100 has
this right). `12-microbatch-topologies.md:112`: every microbatch topology's
"runner" is "on the task 0 leader" and "shares the task 0 thread", so task 0
is special for any microbatch gate, not only in the rig (STE100 ?15 asks it of
the rig alone).

**Whose.** Rama's rule is a fact. "Each task has one thread" is a session's
shortening. "Choose N for the next two years" is marked [RULED] in STE100
§3.3.1; the sentence is Rama's advice from the same doc page.

**What Sid loses, and when.** At scale. N is fixed at the first launch that
keeps records (changing it means copying the tables into a new module, since
F-B closes the replay road). Nobody has put N, or tasks per thread, to him or
derived it.

**What would settle it.** A derivation of N from his own picture (how many
layers, how many live at once), then one line from him. The measure of F-D
should state tasks and threads.


---

## F-K. The lead: can a depot record be cut, and are the depots "never trimmed"?

**Checked (DOC).**
- "a module update can cut depot records" (session text «640badaf», 2 October)
  is true: `14-depots.md:288`, "A depot's records can be migrated to new values
  or excised as part of a module update ... If the function returns
  `Depot.TOMBSTONE`, the record is removed from the depot."; `:310`, "Depot
  migrations never change the offsets of records."
- "the depots are never trimmed" (`g-reads.txt:369`; `g-door.txt:40,186`;
  `BUILD-store-module.md` rule 4; `src-store/softland/store/module.clj:34`) is
  Rama's default and not a guarantee: `14-depots.md:316`, "By default, depots
  permanently store all data appended to them"; `:318-320`, trimming "is
  controlled through dynamic options" (`depot.max.entries.per.partition`),
  "applied every 10 minutes". Dynamic options "can be edited from the Cluster
  UI and take effect immediately" (`15-pstates.md:1227`). So the module's code
  cannot hold this promise. Whoever reaches the Cluster UI can end it, and by
  F-A that page asks for no name.
- The papers disagree with each other: g-store, g-record and STE100 say "until
  a trim"; g-door, g-reads, the contract and the code say "never trimmed".
- Sid's own picture on 23 September (‹abb12971›): "anything that goes into
  Rama Depot we cannot rewrite it the log is append only so what am I missing".
  A session told him then that a migration can cut a record and a depot can be
  trimmed. "Never trimmed" is in no line he typed (memory tool).

**Whose.** The two Rama facts are facts. "Never trimmed" as a rule of the
store is a session's, written as the footing of his decision 2.

**What he loses, and when.** With F-B the rebuild is already gone, so what
"never trimmed" still carries is: history ("as of a day"), the list of what
stood on what (W8), and any table "built later from the depots". All three
are promised on the reads sheet from a log that an operator's one setting or
one excision changes. And the cost side was never put: a log that is never
trimmed and holds every act, sealed bytes included (and every read, if W6
keeps reads as acts), grows without end on every replica. My arithmetic, not
a measure: at the aim of 1,000 acts a second on one task, about 31.5 thousand
million acts a year for that task.

**What would settle it.** A line from Sid: is the log kept for ever, and for
which layers (the spectrum again: a tool that writes each letter and a tool
that writes a paper need not keep the same). And one fact: the bytes of one
act in the depot as built, to turn the rate into disk.

---

## The trace: each statement about Rama, and what stands under it

DOC lines are `reference/rama/docs/` unless a path is given. "IPC" = Rama's
in-process cluster, one machine, 4 tasks, one worker, one copy of the data.

| # | Statement (where) | Trace |
|---|---|---|
| 1 | The runtime is the only Rama client (STE100 §1.3, §3.2.2; ARCHITECTURE.md:115; BUILD-door.md:113) | NOBODY. See F-A. |
| 2 | A client can read and append to a depot; Rama cannot make a depot private (STE100 §3.3.3; g-store:16) | DOC 14:238-240 (`getPartitionInfo`, `read`), 24-rest-api:7. "Cannot make private": by absence in the docs, plus the lost check. |
| 3 | A PState can be made private; topologies of the same module read it (STE100 §1.4, §3.3.8, corr. 7, 18; g-reads W3 A) | DOC 15:222 says "anything other than the owning topology". RUN for a query topology only (lost check of 2 Oct; build note, no file). The other gate reading it: NOBODY. See F-I. |
| 4 | Only one topology writes a PState (STE100 §1.4, §3.3.2) | DOC 15:313. |
| 5 | A query topology cannot write the record (STE100 §3.2.6; BUILD-store rule 7; g-keys:74) | DOC by 15:313 (a query topology owns no PState). No doc line says it outright. |
| 6 | A proxy is Rama's only push; a query topology cannot have a proxy (STE100 §3.2.7, citing 15:436 and 13:175) | By absence. 15:436 describes `proxy`; 13:175 is about `invokeAsync` and says nothing about proxies. 24-rest-api:341: no reactivity over REST. True as far as the docs go; the cited lines do not say it. |
| 7 | A proxy "receives each change" (STE100 §1.4) | Too strong. DOC 15:710, 15:1230. See F-I. |
| 8 | Rama pushes a change only from a table that outside programs can read (g-reads W3, W4) | DOC by 15:222 with 15:436. Derived, sound. |
| 9 | The ack waits only for the stream topologies that read that depot (STE100 §3.2.5; g-store:62) | DOC 14:206. |
| 10 | The ack can be an error for an act the gate accepted (STE100 §3.3.4, §5 rule 5; decision 6 of 7 Oct) | DOC 14:212-214, 11:322; RUN 25 Sep (kept). |
| 11 | A microbatch act gets no answer on the ack (decision 6 of 7 Oct; g-store:193) | DOC 14:206 ("colocated stream topologies"). |
| 12 | A stream event is atomic on one task only (STE100 §3.3.7) | DOC 23:41, 23:52, 23:82; RUN 25 Sep. |
| 13 | Rama can run an event more than once (STE100 §3.3.4) | DOC 11:156 ("at least once"); RUN 25 Sep and lost probe. |
| 14 | An exception stops the worker; all records after the checkpoint run again (STE100 §3.3.4, §7.1; BUILD-store "Read first" 6) | RUN 25 Sep, IPC only (`RESULTS.md:32-35`). The docs say something else: 11:146, a failed record is retried by the retry mode. Whether a real cluster kills the worker: NOBODY. The rule drawn from it (a refusal is data) is safe either way. |
| 15 | A slow event can run twice at once (STE100 §3.3.4, §7.1) | RUN 25 Sep, IPC. |
| 16 | A stream layer has one order (STE100 §6.1 "[RAMA DOCS]") | DOC 11:162 for "all after", without a failure. With one: see F-E. |
| 17 | A failed attempt's PState writes are removed; its depot appends mostly stay (STE100 §3.3.8; g-store:104) | Stream: RUN only, both runs lost (F-F); DOC 11:170 for the PState half. Microbatch: DOC 12:130. |
| 18 | A batch's PState writes happen exactly once; nothing shows before the commit (STE100 §3.3.5) | DOC 12:124-126; RUN 25 Sep. |
| 19 | A batch shows only when the slowest task is done; tasks open at slightly different times (STE100 §3.3.5; README F-4) | DOC 12:124; RUN lost probe Q5 (3 seconds) and 25 Sep. |
| 20 | A batch can be run again after one task has shown it, and can then differ (store-module/PROGRESS 15) | DOC 12:116, 12:128. Not run ("No proof reaches: a second attempt of a real batch"). |
| 21 | Depot appends from a batch are not exactly-once (STE100 §6.2) | DOC 12:130 ("on our roadmap"). |
| 22 | N does not change after the start; past N by copying PStates (STE100 §3.3.1, §3.3.8) | DOC 19:483, 19:487-489. |
| 23 | Two depots or one with a custom partitioner (STE100 §3.3.3; decision 1 of 7 Oct) | DOC 14:121-125 and the partitioner section. |
| 24 | Depots keep each act until a trim by count (STE100 §3.3.3; g-store:17) | DOC 14:316-320. See F-K. |
| 25 | A module update can change or excise depot and PState records (STE100 §3.3.3, §3.3.12) | DOC 14:288-312, 15:768. |
| 26 | The record rebuilds from the acts depot (README 7 Oct entry 2 and after) | NOBODY, and contradicted. See F-B. |
| 27 | A cross-task fetch is a hop on every act; a copy is a rare write (decision 5 of 7 Oct) | DOC in kind (23:52, an event ends at a partitioner). The sizes of the hop and of the copy: not measured. |
| 28 | Replication: what is visible is replicated (RESULTS.md:123-133) | DOC 21:41 only. "a docs promise here, not a run" in the check's own words. |
| 29 | The base's answer about 0.9 s after the append (g-reads:242) | RUN, IPC idle, 3 readings. See F-C. |
| 30 | `topology.stream.max.executing.per.task` at 1 closes the order window (src-store store README) | DOC 11:317 for what the option is. That it closes the window: derived, not run. See F-E. |
| 31 | A query that the cluster does not answer throws after about 70 s (door/PLAN.md) | RUN, IPC, one reading of 70,002 ms; probes kept outside the repo. Default not found in the jar. |

---

## The other numbers in the papers

| Number | Where | What it is |
|---|---|---|
| stamp = ms x 65,536 + counter; below 2^62; "the year 4199" | `gate/stamp.clj`; README 6 Oct entry 12 | Arithmetic, and it checks: 2^46 ms is about 2,230 years after 1970. The packing is the builder's. 2^62 is "kept from today's code, not decided". |
| "65,536 stamps ... far more than a task decides in a millisecond" | `gate/stamp.clj` | A session's judgement; no measure of the built store exists (F-D). |
| 10 seconds ahead of the clock | BUILD-store rule 17; `stamp.clj` | A session's figure, "kept and not decided". Item 25 of the build page: any sender can hold a gate's stamps 10 s ahead. |
| 256 facts, 1,024 `:based-on` entries, 65,536 sealed bytes, 256 locks a request, 256 characters an id | BUILD-store rule 20 | A session's figures, "kept, not decided". 65,536 bytes caps one sealed value: an image, or a text past about ten thousand words, does not fit in one value. |
| 256 subjects, 32 collections deep, permission 4 deep | README 6 Oct entries 8 and 12 | "Kept from today's code, not decided." |
| up to 256 places, 256 values of 65,536 bytes, 32,768 lock rows held by one act on one task | store-module/PROGRESS 22 | Arithmetic from the limits (128 requests x 256). "nothing is measured". |
| "3 reads more for each act" when a permission can end | store-module/PROGRESS 20 | Arithmetic from "4 deep". |
| 64 locks in a streaming stock; a request "once in each 64 values" | door/PROGRESS | A session's figure (ruling 16 of the door's plan). |
| "The hand reads every 20 ms; at a thousand waits at once that rate is a thousand times fifty queries a second" | door/PROGRESS, list for a session that takes over | Arithmetic: 50,000 queries a second, each one work on a task thread. The door as built reads every 200 ms by default (`door/stock.clj:103`, "none is measured, and none is a decision of Sid's"), so a base act costs about 5 reads over its 0.9 s and is seen up to 200 ms later than it is readable. Decision 6 of 7 October was put to Sid as "each act costs one more read" (`g-store.txt:189`). |
| `:request-wait-ms` 10,000 | `door/stock.clj:95` | A session's figure: "about ten times the time in which a request's answer was readable in the base on the in-process cluster", and equal to a Rama client default read from the jar (`foreign.depot.operation.timeout.millis`, door/PLAN.md probe 3). It is built on the idle 0.9 s of F-C. |
| 21,000 random batches; proof counts 150, 449/538/540, 227, 162/193 | both PROGRESS pages | Counts of runs, as written. Not checked by me. |
| 3 seconds (Q5), about 80 ms to relaunch a worker | STE100 §7.2; RESULTS.md:35 | RUN, IPC; the first is from a lost run. |
| 4 tasks, replication 1, Ryzen 9 9900X | STE100 §6.1, §7 | The conditions of every run. |


---

## F-L. "Can two nodes run here?" was put to Sid as a box. It is a line of Rama's docs.

**What it is.** `g-store.txt` F9 (a): "Can two nodes run here? A test of a
failed node needs two. [ ] yes [ ] no [ ] not known". Recorded in
`src-store/README.md`, 7 October entry 9: "Two nodes on one machine is a
question for production (F9 a)."

**Whose.** The rulings file lists it as "Still Sid's". His lines: ‹27882354›
"wtf is 'here' which 2 nodes ???" and ‹32f65085› "currently idk about the
license part and how many nodes can run once but since we are in dev phase it
is fine to punt to later when i go to production".

**The check (DOC).** `19-operating-rama.md:110`: "Rama has an embedded free
license that allows clusters to be run up to two nodes. The node limit is for
Supervisors only, and Conductor/Zookeeper do not count towards that limit. A
paid license is required for larger clusters". `:165-167`: a single-node
cluster is Zookeeper, the Conductor and one Supervisor on one node. Whether
two Supervisors run on one machine is not in the local docs; it is a ten
minute try, not a question for him.

**What he loses, and when.** At the first real cluster. Every run so far has
one copy of the data, so nothing about a failed node, a moved leader or the
cost of replication is known (F-D). And the free licence stops at two
Supervisor nodes, so three copies of the data is a paid licence: a cost fact
that bears on any promise about losing no record. The punt is his. What was
not his to answer was "can it".

**What would settle it.** Try two Supervisors on the one machine. Then run
the 25 September check and the measure of F-D on it with replication 2.

---

## What nobody asked at all

1. **What can reach the cluster's ports.** No paper lists the ways into Rama
   (the Clojure/Java client through the Conductor, the REST API on every
   Supervisor and on the Cluster UI port, the Cluster UI's dynamic options,
   the `rama` CLI). "The only Rama client" was a sentence, never a list. (F-A)
2. **Whether the record must be rebuildable from the log, or the tables are
   the store.** It was assumed in the question, found false by the build, and
   never asked. With it: whether a stamp and a lock must be the same on a
   second attempt. (F-B, F-C)
3. **How long "a batch later" may be**, in Sid's own number, before the base
   was made a microbatch layer; and what the base's readers see while a place
   has no closed row. (F-C)
4. **What a watching screen costs the gate.** Sid's line is "write directly to
   rama and then stream from there". No run has one proxy in it, and no paper
   says that Rama does a proxy's work on the task's thread inside the write.
   (F-D, F-I)
5. **Whether a push is a promise.** Rama's docs say a proxy can be dropped
   from a change and catch up only at the next one. Every design here that
   says "a bell rings, then read" or "Rama pushes the value" assumes the ring
   always comes. (F-I)
6. **What "private" covers** for a gate that reads the other gate's tables.
   (F-I)
7. **Where a forgotten lock still is, and for how long**: replicas,
   checkpoints, backups, files not yet compacted. And whether the store is
   backed up at all, given that its locks cannot be rebuilt. (F-G)
8. **What "frozen" costs on Rama**, item by item, given that Rama can migrate
   depots and PStates. Whether an answer's digest may ever change. (F-H)
9. **Whether the log is kept for ever, for every layer**, and what that costs
   in disk; who may change the trim setting. (F-K)
10. **Whether a layer's acts must keep the depot's order through a failure.**
    Named as Sid's choice in a README; in no sheet and no queue row. (F-E)
11. **N, and tasks per thread.** Fixed at the first launch that keeps records.
    (F-J)
12. **What every number would be with a second copy of the data and a network
    between the door and the cluster.** (F-D, F-L)
13. **Whether the clocks of two machines agree within 10 seconds.** The
    10-second rule refuses an honest act that stands on a stamp from a machine
    whose clock is that far ahead. One machine so far; never run on two. (Read
    in `gate/stamp.clj`; not run.)
14. **Which Rama the docs describe.** The local docs are an extract of March
    with no release named; the jar is 1.6.0. Two sessions leaned on behaviours
    the docs call temporary ("on our roadmap": exactly-once depot appends from
    a batch; "high priority": changing the number of tasks). Nobody wrote down
    which findings would turn if those land.

---

## For another part (one line each)

- Flows: with W3 B, what Rama pushes from the facts table is the sealed row;
  its lock is a row of another table (`$$streaming-locks`), and a proxy path
  "must navigate to exactly one value" (`15-pstates.md:438`), so each pushed
  value needs a second read or a second watch. Not checked further here.
- Decisions page: "a permission is 4 levels deep at most" (kept from the code)
  sits beside Sid's "however deep the groups and agents go" (8 October) and
  "infinitely compositional" (17 September, ‹af5a942e›).
- Decisions page: entry 2 of 7 October gives "The acts depots are the replay
  log" inside the paragraph of his reason; the phrase is a session's.
- Build rulings: the order after a failure, the 10 seconds, the five limits of
  rule 20 and the door's five numbers are each written as "his to choose" or
  "kept, not decided", and none is a row of `OPEN-QUESTIONS.md`.
- Build rulings: the microbatch gate keeps one sequence of stamps for the
  whole gate, not one for a task (`store-module/PROGRESS.md` item 20), against
  the contract's "strictly increasing on its task".
- Architecture files: STE100 marks "Choose N for the next two years" as
  [RULED]; the sentence is Rama's advice (`19-operating-rama.md:485`).
- Architecture files: STE100 §8 correction 9 fixed "Rama never rewrites a
  depot" and left "replaying it gives the same state" (ARCHITECTURE.md:279)
  uncorrected; it is not in the table of 28.

---

## What I read, and what I did not

**Read whole:** `.claude/skills/rama/SKILL.md`; `reference/rama/docs/README.md`;
`src-store/README.md`; `ARCHITECTURE-STE100.md`; `BUILD-store-module.md`.

**Read in part, by search and then the passages:** `g-reads.txt` (the picture,
W1, W3, W4, W5, W8 and the last lists; W2, W6, W7 by search only);
`g-store.txt`, `g-record.txt`, `g-door.txt`, `g-keys.txt` (by search for Rama's
words and for numbers; F2 and F9 of g-store as passages); `ARCHITECTURE.md`
(by search; lines 112-117, 274-284, 326-338); `BUILD-door.md` (by search;
lines 108-116); `store-module/PROGRESS.md` (lines 104-232);
`door/PROGRESS.md` (by search for numbers; lines 262-290, 336-346);
Rama docs 11, 12, 13, 14, 15, 19, 21, 22, 23, 24 (the cited lines and their
neighbours); the skill's `references/microbatch.md:30-38`; of the built code,
`module/streaming_gate.clj:118-200`, `gate/stamp.clj`, `gate/decision.clj`
(the seal check), `door/stock.clj:90-104`, `softland/store/README.md` (lines
150-166, 425-505), and searches of `module.clj` and `microbatch_gate.clj`;
`rama-check-2026-09-25/RESULTS.md` (by search); the rig's two bench notes at
the cited lines; `store-module/PLAN-microbatch-gate.md:1548-1575` and one row
each of `PLAN_VALIDATION-streaming-gate-and-record.md` and `FULL_SPEC_REVIEW.md`.

**Sid's lines opened with the memory tool:** ‹27882354›, ‹32f65085›,
‹8e510528›, ‹8573450f›; and by search, ‹abb12971›. Session texts opened:
«802316c6», «40cdb514», «640badaf» (by search). `who` on "replay log", "never
trimmed", "first kept record", "recomputable from the acts depot".

**Not read:** the other files in this folder; `src/proposal/frame-2026-09-15/PROGRESS.md`
(one line by search); `vision/LOG.md` and `docs/carry-on.md` (only as the
sheets quote them); `decisions/METHOD.md` and `OPEN-QUESTIONS.md` (searched,
not read); `door/PLAN.md` beyond two passages; the door's source beyond its
numbers; the rig's code; Rama docs 01-10, 16-18, 20, 25-32 beyond searches;
the Rama jar itself. So "the docs do not say X" means the March extract does
not, under the words I searched.

**Not run:** anything. Every "what would settle it" that is a run is still to
do. Line numbers in the Rama docs were read from the files today; the few I
gave as a range from memory of a longer print are within three lines.

---

## Order by bite (how badly, and how hard to undo)

1. **F-B** — the record cannot be rebuilt from the log; the locks tables are
   primary data. Lands at the first kept record; cannot be undone for what is
   kept. Half caught by the builders, not asked of Sid.
2. **F-C** — the base was made microbatch on "a few ms"; built, its answer
   comes after about 0.9 s, and reads held to closed batches leave a busy
   place with no readable row. The depot
   word is in every act-id, so it sets at the first kept record in the base.
3. **F-G** — a forgotten lock lives on in replicas, checkpoints and backups,
   and the store must be backed up because of F-B. Lands at the first forget.
4. **F-A** — "the only Rama client" is a network rule nobody wrote; Rama has
   an HTTP door and asks no name. Lands when reads are built; cheap until
   then, and it decides W3.
5. **F-E** — after a failure a layer's later act can be decided first and the
   answer check keeps it. Sid's choice by the builders' own words; never put
   to him. Lands at the first kept record, on a fault.
6. **F-I** — watchers run on the gate's thread; a push can be missed;
   "private" may shut out the other gate. Lands when reads are built.
7. **F-D** — no number of the built store is measured; the aims are a
   September proposal; every measure leaves out replication, the network and
   watchers. Steers the reads sheet now; lands at the first real cluster.
8. **F-H** — "freezes" is a session's rule that makes decisions look due now;
   Rama can migrate; the digest is what pins. Bites now, as pressure.
9. **F-K** — "never trimmed" is a default that one operator setting ends, and
   the reads sheet promises history from it. Lands at scale.
10. **F-J** — a task is not a thread; N is fixed at launch and underived.
    Lands at scale.
11. **F-L** — "can two nodes run" was a fact in the docs. Small by itself; it
    is why nothing here was ever run with two copies.
12. **F-F** — the two runs under decision 2 of 7 October have no files left.
    Small, and an hour to mend.
