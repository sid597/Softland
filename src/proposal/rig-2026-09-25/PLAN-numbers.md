# Plan — phase 7, the three numbers, on the finished store

*Cut by Opus 5.5 at max effort, 26 September 2026 (IST, before dawn),
following the rama skill's plan step (`references/phase-1-plan.md`),
adapted to a measurement plan: this phase builds harnesses and runs them,
not a module. Worktree `Softland-rig-plan-numbers`, branch
`rig-plan-numbers`, made off `rig-2026-09-25` at `703b8e26`.*

**Status.** A plan. Nothing is built and nothing was run. One method
question (can a harness tell a PState's lock rows apart from its other
entries inside RocksDB?) was taken as far as reading takes it, from the
Rama 1.6.0 jar's class listing (section 6.5); no cluster was started and
no probe was run.

**Marks.** Each load-bearing claim says where it comes from: *code* (a file
and line on a named branch and commit), *plan* (a validated plan's
section), *slice* (a run file of 25 September), *derived* (reasoned from
those, not run), *assumed* (a default or a guess, named as one). Anything
read off a branch that tonight's merges may rename is marked **to confirm
at build**; section 12 gathers them.

Paths are relative to the rig folder, `src/proposal/rig-2026-09-25/`,
unless absolute. Vocabulary as SPEC.md fixes it: "key" is a fact's key,
"lock" an encryption key.

## 0. The plan on one screen

- **Three numbers, each on the finished store through its own road**:
  offers built and sealed by the door (`rig.store.client`), locks leased
  from the gate, the gate's one decision event, and the read exit where
  reads are part of the workload. No measurement changes `src/rig/store/`
  and none uses a slice module.
- **"Index writes"** keeps the slices' meaning: every PState write the
  gate's decision event makes for an admitted act, one per path written,
  sets and deletes, named from the code. For the small agent act (one
  fact, a 40-byte value, a key with no grammar) the built branches and the
  validated plans give **9**: the answer record, the row, the head, three
  id-index entries, the consumed lease row, the stamp-to-name entry and
  the task's clock (**[F1]** 10 on the first merge, `d83e5ff8`, whose
  constant hints index `:note` by value until phase 6 replaces them: 2.1,
  4.2). The slices' act made 4. Every 64 value acts also cost
  one lease act of 72 writes. A test checks the list against the merged
  store's own growth before any number is run (section 2, test T1).
- **Number 1, the agent rate.** One agent session layer on one task,
  written under a session permission, K closed-loop writers sharing one
  door: K = 1, 4, 16, then 32, 64, 128 and on while the rate still rises.
  Value acts a second and index writes a second. Beside it, the same act
  under a by-value grammar (11 writes). In the full set: S sessions on one
  task, each with its own door, each writing at an assumed agent speed of
  100 acts a second, which measures how many agent sessions one task
  carries at that speed, the divisor the README's yardstick asks for; and
  the agent reading before it writes, each read recorded through the exit.
- **Number 2, lock store growth under hand layers.** 100,000 values into a
  hand layer through the door, for 40-byte and 200-byte values, a point
  every 10,000. The finished store's real lock rows read back and sized in
  Rama's own serializers (logical bytes), and on disk after a forced
  compaction with the lock rows isolated from everything else the layer
  keeps. Each extra subject from values marked to die with one, two and
  four more people. In the full set, one hand layer at per-act grain, four
  values an act: the README's fallback, computed on 25 September, run.
- **Number 3, one person's layer on one thread.** A personal layer: one
  sequential writer (6,400 measured acts, so the lease cadence is well
  sampled), then K = 1 to 128 closed-loop writers. Acts a second; latency
  typical (p50), slowest 1 in 100 (p99) and worst (max), with the offers
  that paid for a lease shown apart. **[F5]** Then an open arrival at 100
  and 1,000 acts a second, latency from the schedule, on which the latency
  threshold is judged; the closed-loop latencies are labeled service times.
- **The machine** is recorded by the harness at every run, not copied from
  a note, and every result file says the in-process cluster gives orders
  of magnitude only.
- **Thresholds**: RIG.md default 7's, assumed and not Sid's, judged with
  the slices' rule (ten times on one side decides; within ten times the
  in-process cluster cannot decide). Bytes are judged directly.
- **Runs**: one JVM and one cluster per run, each under `flock` on the
  relay's cluster lock, driven in the background by one script, progress
  one line every 5 s under `runs/`, results in `runs/phase7-final-*.txt`
  with method headers and raw points in `runs/phase7-final-*.edn`. The
  minimum set is about 45 minutes of machine time and the full set about
  65 (with **[F4]** and **[F5]**); **[F11]** a shortest set, one run of each
  timing step and the two main lock variants, about 20; plus any wait for
  the lock (section 8.6).
- **Build**: five new namespaces under `test/rig/bench/` (a shared
  harness, one per number, a test namespace) and a driver script. Nothing
  under `src/`. The slice benches stay as they are: they are the record of
  how the slice numbers were made.

## 1. What binds this plan

### 1.1 Sid's words (SPEC.md, phase 7, verbatim)

> 7. The three numbers, each with its method and the machine it ran on:
>    index writes per second with an agent session layer writing small acts
>    continuously; lock store growth under hand layers, bytes per value and
>    the curve over a hundred thousand values; one person's layer on one
>    thread, acts per second and latency.

### 1.2 SPEC.md, "Tests and evidence"

- Tests are `clojure.test` namespaces under the rig folder's `test/`, run
  with `clojure -M:test <ns> ...` from the rig folder.
- Every claim that a capability works says what was run and what it
  showed; a claim that was only reasoned says so.
- Measurements state the method, the machine and the run. The in-process
  cluster is not a proxy for production latency or throughput; only orders
  of magnitude mean anything, and the numbers say so.

### 1.3 The brief for this plan, and where each part is answered

| The brief asks | Section |
|---|---|
| each number's method on the finished store: what writes, through which road (door, leases, sealing; the read exit where reads are part of the workload); what counts, named from the code; run length, warm-up, task count, writers, value sizes | 2, 4, 5, 6 |
| agent rate: acts and index writes a second on one task at 1, 4 and 16 writers at least, and how far past | 4 |
| lock growth: bytes per value and the curve over 100,000 values, 40 and 200 bytes, logical and on disk after compaction, each extra subject, the finished store's real lock rows | 6 |
| one person's layer: acts a second and latency (typical, 1 in 100, worst) at 1 writer and rising | 5 |
| the machine, with the in-process caveat | 3 |
| the thresholds, judged against default 7 and said to be assumed | 7 |
| how the runs execute: the cluster lock, the background, progress under `runs/`, `runs/phase7-final-*.txt` with method headers | 8 |
| what the build writes: harness files and namespaces as named pieces, and a test that each harness measures what it claims | 9 |
| how each final number compares with its slice number, and what a change would mean | 10 |

### 1.4 The thresholds as written (RIG.md, "Defaults taken overnight, not ruled", 7)

> The numbers are judged against the thresholds assumed in this file and
> README.md (at least 1,000 admitted acts a second a task with every index
> written; at least 100 acts a second at 20 ms or less for the slowest 1
> in 100; a lock store at most twice the value bytes, above four times
> change the default), which are assumed, not Sid's. Lock growth is
> reported for 40- and 200-byte values.

### 1.5 The yardstick (README.md, "The yardstick for the numbers")

The agent rate is a divisor: how many agent sessions one task carries,
which feeds N, fixed at launch for two years. The README's arithmetic
(5,000 small acts a second a task, in-process, over an agent writing
about 100 a second at full speed: about 50 agents a task) is *derived*,
not measured, and the README names the missing inputs: "How fast an agent
really writes, and how many run at once, are the inputs that turn this
into a threshold." Variant B of number 1 (section 4.5) measures the task's
side of that division directly, at an agent speed that stays an
assumption.

### 1.6 What the stage plans left for phase 7

| Plan | What it leaves | Taken in |
|---|---|---|
| PLAN-locks-and-forgetting.md, "What later stages consume" (lines 1898-1902) | M2: iterate `[layer :locks]` for count and serialized bytes at each point, raw bytes; lease rows not counted, they are consumed at decision. M1: agent layers write no lock rows; their per-act writes are the row with the lock in it, `:by-stamp`, one lease-row delete per value fact, and a lease act per batch | 2.3, 6 |
| PLAN-reads-rest.md, "The mark at write" (1153-1185) | every read entry is marked `:own-row`, so agent layers now write one lock row per read-entry value, and a hand layer's `:locks` count includes entries | 2.3; 4.6; 6.1 (number 2's workload makes no entries, so its count is values alone) |
| PLAN-reads-rest.md, "The delta" (816-895) and WS11 | a fifth index, `:ix-s`, written in the admitting event for every fact | 2.3 |
| PLAN-read-exit.md, "Disk usage" (1179) | an entry line with an exact list is "still the number to watch" at a model's call rate | 4.6, the entry bytes (optional) |
| PLAN-read-exit.md, "Minimization" (1534) | the exit's two round trips are "the first thing to measure if phase 7 finds the exit slow" | 4.6 and 5.5, read latency |
| PLAN-micro-store.md, permission walk (598) | "phase 7's numbers say whether the 1.5 seeks show" | 4.7, diagnostic D2 |
| PLAN-tools-and-grammars.md, section 10 (1418-1422) | the two-person `:mention` variant needs the toy grammar written, or Bob is not in its wraps | 6.1 |
| PLAN-promotion.md (1133) | no new number | nothing |

### 1.7 What does not bind

- The slices' harness choices of 25 September (four tasks, a 40-byte
  value, closed-loop writers, 15-second windows, three runs). They were
  made for slices. This plan keeps each one where comparability is worth
  its cost and says so where it departs.
- RIG.md's parenthetical "the harnesses in `src/rig/bench` and
  `test/rig/bench`" (Overnight state, Unfinished, item 4) is a placement
  note. The finished store's harnesses drive the store's own module, so
  nothing needs a module of its own under `src/` (section 9).

## 2. The finished store, per act: what is counted

### 2.1 Which code this is

"The finished store" is `rig-2026-09-25` after wave 1 (phase 2's locks and
phase 3's micro store merged with the read exit, in `Softland-rig-wave1`),
wave 2 (phase 4, the rest of phase 5, phase 6) and phase 8's replays. At
the time of writing no merge has committed (`rig-wave1` stands at
`703b8e26`). The tables below are therefore read off:

- phase 1, the hybrid clock and the read exit: this branch at `703b8e26`
  (*code*);
- phase 2: `rig-build-locks` at `fd41f6d2`, built green on its own
  (*code*);
- phase 3's permission walk and session opening: `rig-build-micro` at
  `e60c8ee1` (*code*);
- the rest of phase 5 (`:ix-s`; the `:own-row` mark on entries) and
  phase 6 (grammar rows and hints): their validated plans (*plan*).

Every row is **to confirm at build**. Test T1 (section 9.3) counts the
merged store's own growth, field by field, over a hundred acts and fails
if the harness's list disagrees, so a list that the merge made wrong
fails before a single number is run.

**[F1] What the first merge shows.** At validation (26 September, 05:00
IST) `rig-wave1` stands at `d83e5ff8`: phase 3 merged (`f29f792d`), then
phase 2, with the seams of RIG.md "Unfinished" item 1 still to wire. Its
decision event (module.clj L127-209; gate.clj `decide*` L335-390;
locks.clj `write-decision>` L1197-1241 and `lock-effects` L784-816;
reads.clj `index-writes` L181-200) writes, for an admitted agent `:note`
act, rows 1 to 5, 7, 8, 10, 12 and 13 of the table below: **10 writes**.
Rows 7 and 8 are there because `reads/seed-hints` (reads.clj L34-41) is
`{:by-value #{:note} ...}`, a constant `decide*` applies in every layer
(gate.clj L356). Phase 6's plan replaces it with the store's own keys only
(PLAN-tools-and-grammars.md L1381-1385), after which a `:note` with no
grammar gets no value index (its 4.6); row 6, `:ix-s`, is the rest of
phase 5's. So the agent column's 9 is the store after wave 2; with the
constant still in place and `:ix-s` built it is 11. Section 4.2 says which
key the primary act uses in each case.

### 2.2 The acts the workloads make

- **Value act.** One fact `{:e <one of 1,000 entities in rotation> :k
  :note :v <38-character string>}`, whose canonical EDN is 40 bytes: the
  slices' act. The door seals the value under a leased lock, so the depot
  record carries 68 sealed bytes (40 + 12-byte nonce + 16-byte tag) and
  the lock id, never the plaintext (*code*: `rig-build-locks` client.clj
  `seal-with` :188). No replace, nothing stood on, no control fact.
- **Lease act.** The door's `{:e session :k :lease :v {:count 64}}`, sent
  whenever its pool for [layer, session] holds too few locks (*code*:
  client.clj `lease-size` :27, `assign!` :155, `lease-for!` :136): one
  per 64 value acts at steady state. The door leases one thread at a time
  (its `:lease-mutex`, BUILD_NOTES-locks-and-forgetting.md "Rig choices"),
  and a lease act cites the same permission and session as the offer that
  needed it.
- **Read entry act** (number 1's variant C and number 3's reads only).
  The exit's entry: one `:read/point` fact per fact read (*code*: reads.clj
  `entry-facts` :759), an ordinary offer into the reader's working layer,
  sealed at the door like any value (RIG.md default 3), marked `:own-row`
  (*plan*: reads-rest, "The mark at write").
- **Setup acts** (the `:people` store layer, persons, layers, grants,
  session permissions, grammars): outside every measured window.

### 2.3 The writes of the gate's decision event, per admitted act

"Index writes" are the `local-transform>` path writes (a set by `termval`
or a delete by `NONE>`) of the gate's decision event for an admitted act,
on any PState, named from the code. That is the slices' own count
(runs/phase7-agent-rate.txt, "INDEX WRITES PER ADMITTED ACT", which
counted `$$clock`), kept so the numbers compare. Two subsets are reported
beside it: the read indexes (`:ix-*`) alone, and the deletes. A whole row
vector written into its subindexed vector is one path write, whatever
RocksDB entries it becomes.

`L` is the layer, `s` the session, `name` the act's name, `fid` a fact id.

| # | Write | Path | Kind | Agent value act, key without grammar | Same, by-value grammar | Personal or hand value act | Lease act, 64 locks | Read entry act | Site, to confirm at build |
|---|---|---|---|---|---|---|---|---|---|
| 1 | answer record | `$$layers [L :answers name]` | set | 1 | 1 | 1 | 1 | 1 | module.clj decision block, `703b8e26`:135, `fd41f6d2`:156 |
| 2 | the act's rows | `$$layers [L :log name]` | set | 1 | 1 | 1 | 1 | 1 | :138 / :159 |
| 3 | the fact's head | `$$layers [L :heads [e k fid]]` | set | 1 | 1 | 1 | 1 | 1 | :148 / :169 |
| 4 | id index by entity | `$$layers [L :ix-ek address]` | set | 1 | 1 | 1 | 1 | 1, no copy | reads.clj `index-writes` :181, applied at module.clj :153 |
| 5 | id index by key | `$$layers [L :ix-ke address]` | set | 1 | 1 | 1 | 1 | 1, no copy | same |
| 6 | stamp index | `$$layers [L :ix-s address]` | set | 1 | 1 | 1 | 1 | 1 | PLAN-reads-rest.md "The delta" (not built yet) |
| 7 | value index | `$$layers [L :ix-kv address]` | set | 0 | 1 | 0 | 0 | 0 | reads.clj `fact-writes` :152 |
| 8 | value id to its value addresses | `$$layers [L :ix-of fid]` | set | 0 | 1 | 0 | 0 | 0 | module.clj :156 |
| 9 | lock row | `$$layers [L :locks lock-id]` | set | 0 | 0 | 1 | 0 | 1, `:own-row` | locks.clj `write-decision>` :1197 |
| 10 | the cited lease row, consumed | `$$layers [L :leases s lock-id]` | delete | 1 | 1 | 1 | 0 | 1 | locks.clj `consume-locks>` :1050 |
| 11 | the lease's rows | `$$layers [L :leases s lock-id]` | set | 0 | 0 | 0 | 64 | 0 | locks.clj `lease-writes` :743, written by `write-decision>` |
| 12 | stamp to name | `$$layers [L :by-stamp stamp]` | set | 1 | 1 | 1 | 1 | 1 | locks.clj `write-decision>`; whether the merge keeps it (BUILD_NOTES-locks-and-forgetting.md, "Seams and stubs") |
| 13 | the task's last stamp | `$$clock` | set | 1 | 1 | 1 | 1 | 1 | :168 / :180 |
| | **total** | | | **9** | **11** | **10** | **72** | **10** | |
| | of which read indexes (rows 4 to 8) | | | 3 | 5 | 3 | 3 | 3 | |
| | of which deletes | | | 1 | 1 | 1 | 0 | 1 | |

The gate computes index writes for every admitted act, control facts
included (*code*: gate.clj :281 on `703b8e26`), which is why the lease act
has its three.

Written by none of the workloads' acts, listed so a reader sees each was
considered: `:stood-on` (one per carried entry; variant C's value act
carries one, section 4.6), deletes of `:heads` (one per replaced fact),
`:settings` (setting facts), `:permissions` (grants and revokes),
`:key-rows` (phase 6 writes a row only when a grammar lands or the first
fact under a key is admitted in a layer: *plan*, tools-and-grammars 4.7;
**[F2]** so each layer's first value act under a key that is not a store
key writes one row, in setup or the warm-up and never inside a measured
window, and T1 to T3 count it), `:erased` and lock-row deletes (forgets),
`$$persons` (person acts and their fan-out to every task).

With its share of lease acts, a value act costs 9 + 72/64 = 10.1 writes in
an agent layer and 10 + 72/64 = 11.1 in a personal or hand layer
(*derived*). The slices' act cost 4 (*slice*).

### 2.4 The reads of the decision event, per value act (reported, not a number)

| Read | Path | Agent value act | Personal value act | Site, to confirm at build |
|---|---|---|---|---|
| the name's record | `[L :answers name]` | 1 | 1 | module.clj :103 / :114 |
| the layer's settings | `[L :settings]` | 1 | 1 | :110 / :125 |
| the owner's and the writer's person entries (once each) | `$$persons [p]` | 1 | 1 | locks.clj `deliver-all>` :1063, `read-persons>` :1007 |
| the cited lease row | `[L :leases s lock-id]` | 1 | 1 | locks.clj `deliver-lock>` :1026 |
| the task's last stamp | `$$clock` | 1 | 1 | :111 / :131 |
| permission rows, one per chain element | `[L :permissions pid]` | 2 (the session's, beneath the owner's root) | 1 (the owner's root) | :119 / :139; phase 3's walk, `rig-build-micro` permit.clj `chain` :29 |
| heads of replaced facts | `[L :heads hk]` | 0 | 0 | :127 / :147 |
| the key's row | `[L :key-rows k]` | 1 | 1 | *plan*: tools-and-grammars 4.7, step 2 |
| **total** | | **8** | **7** | |

The slices' act read 4 (*slice*). New work on the task thread per value
act (*code*, locks.clj on `fd41f6d2`): opening the lease row under the
person's lock, opening the value under the delivered lock, re-wrapping
that lock under the wrap's persons (one AES-256-GCM seal each), and the
value digest (HMAC-SHA256).

### 2.5 What each number reports, named once

- **Value acts a second**: admitted value acts over the measured window,
  the writers' own acts. The thresholds are judged on this.
- **All acts a second**: value, lease and entry acts admitted.
- **Index writes a second**: the sum over act kinds of (admitted acts of
  that kind in the window) × (that kind's total in 2.3), over the window's
  seconds; beside it the read-index subset and the deletes, each a
  second. The counts come from the run; the per-act totals from the code,
  checked by T1.
- **Lease acts** are counted twice, and the two must agree: from Rama's own
  count (the home depot partition's growth, less the value and entry
  offers the harness sent) and from the door (the leases it came to know
  in the window).

## 3. The machine and the configuration

### 3.1 The machine, recorded by the harness at every run

The harness writes a `META` line at the start of every run and copies it
into the head of each result file. Nothing in it is typed by hand.

| Field | Source |
|---|---|
| CPU model | `/proc/cpuinfo`, `model name` |
| cores and threads | `lscpu` (cores per socket × sockets), `Runtime.availableProcessors` |
| RAM | `/proc/meminfo`, `MemTotal` |
| OS and kernel | `/etc/os-release` `PRETTY_NAME`; `uname -r` |
| JDK | system properties `java.vm.name`, `java.runtime.version`, `java.vendor` |
| JVM flags and GC | `RuntimeMXBean.getInputArguments`; the garbage collector beans' names |
| Rama and Clojure | the Rama jar on the classpath (its versioned file name), `(clojure-version)` |
| where the cluster's files live | the in-process cluster's temp dir; `df -T` of it (filesystem); `lsblk -no MODEL` of its device |
| load before the run | `/proc/loadavg` |
| what ran | the rig folder's `git rev-parse HEAD` and `git status --short` (clean or not), the namespace, function and arguments |

For comparison, the slices recorded an AMD Ryzen 9 9900X (12 cores, 24
threads), 62 GB RAM, Linux 7.0.0-31-generic, OpenJDK 21.0.12.1, the
cluster's files on ext4 on an NVMe SSD (WD_BLACK SN850X), load 0.43
(*slice*). If any field differs at run time, the comparison in section 10
says so first.

### 3.2 Configuration common to the three numbers

- Rama 1.6.0, rama-helpers 0.10.0, Clojure 1.12.4, as deps.edn pins them.
- The JVM from deps.edn's `:bench` alias: `-Xss16m -Xmx16g`, default G1.
- `rtest/create-ipc`, then `rtest/launch-module!` of the finished
  `rig.store.module/Store` (**to confirm at build**: the module's var
  after the merges), with Rama's default stream and microbatch options.
- Numbers 1 and 3: `{:tasks 4 :threads 4 :workers 1}`, a thread per task,
  as the slices ran. The layer measured lives on one task; the other three
  idle. Number 2: `{:tasks 1 :threads 1 :workers 1}`, as its slice ran:
  bytes do not depend on the task count, and one task puts the hand
  layer's whole store in one RocksDB directory.
- The finished module carries the micro store's microbatch topology,
  idle through every workload here. Each run of numbers 1 and 3 opens with
  a 10-second idle window (no offers) that records every thread's CPU, so
  whatever the idle topology costs the task threads is on record.
- One JVM and one cluster per run; three runs per timing measurement,
  reported as the median and the spread (minimum to maximum).

### 3.3 The caveat every result file carries, verbatim

> In-process cluster: every task, the RocksDB stores and the clients run
> in one JVM on one machine, with replication factor 1 and this machine's
> disk. The timings mean orders of magnitude only (the rama skill's
> testing.md, axiom 1; SPEC.md, "Tests and evidence"): a result ten times
> or more from its threshold decides the question; one within ten times
> needs a real cluster. Logical bytes are exact for Rama 1.6.0's
> serializers; bytes on disk depend on the RocksDB options Rama sets here.

## 4. Number 1: index writes a second, an agent session layer writing small acts continuously

### 4.1 What it answers

README.md, "Each rig question checks a ruling": ruling 2 places one-owner
layers by layer, with N, the task count, fixed at launch for two years, and
"a stream is many small acts". A bad answer changes "how many agent
sessions one task can carry, and so how large N must be". So the number is
a task's capacity for small acts with every index written, and the divisor
that capacity gives.

### 4.2 Setup, per run, before any window

Every setup act must be answered `:yes`; anything else stops the run with
the answer printed, as the slices' `make-layer!` did.

1. The store layer `:people` (`c/make-layer-offer :people {:kind :store}`)
   and the person `:ada` (`c/make-person!`): a layer's owner needs a person
   lock before its making act (*code*: client.clj `seed!`, L11).
2. The agent session layer `:bench-agent`, `{:kind :agent :owner :ada}`
   (`c/make-layer-offer`, which also sets per-value grain), and the owner's
   root permission `[:ada :bench-agent :bench-agent]` (`c/grant-offer`).
3. A session `:bench-s1` opened on it: the grant
   `[:bench-s1 :bench-agent :bench-agent [:ada :bench-agent :bench-agent]]`
   beneath the owner's, by the operator through the layer's gate (*code*:
   `rig-build-micro` micro_client.clj `open-session!` :482; **to confirm
   at build**, its namespace after the merge). Value acts are built with
   `c/build {:who :ada :session :bench-s1 :permission <that pid> :layer
   :bench-agent :class :by-layer :facts [f]}`: the permission's holder is
   the offer's session, which phase 3's check accepts (M21).
4. The layer's home task: the depot partition that grew by one when the
   layer was made.
5. For A' (4.4): a second layer `:bench-agent-v`, made, granted and opened
   the same way, and a `:note` grammar with `:index #{:by-value}` written
   in it as a fact by the operator (*plan*: tools-and-grammars 4.1 and 4.7;
   **to confirm at build**, the fact's form). Its home task may differ from
   the first layer's; placement is checked per window.

The layer holds no grammar in variant A: "session layers start with none"
(*plan*: tools-and-grammars, section 0), and a key with no grammar gets no
index hints (4.6 there), so the act writes the three id indexes and no
value index. That is the realistic default and the primary number; A'
gives the heavier end.

**[F1] Which key the primary act uses.** Before writing the harness the
build reads the merged store's hints. If a key with no grammar gets no
hints (phase 6 merged), the primary act's key is `:note`, as above. If
any constant still hints `:note` by value (today's `reads/seed-hints`,
2.1), `:note` is the by-value act: the primary act then uses a key that no
constant names and no grammar covers (the build picks one, `:memo` say,
and names it in every result header), and A' keeps `:note` (with its
grammar fact where grammar facts exist, by the constant otherwise).
Number 3's act and number 2's unmarked values use the primary key too;
the `:mention` variants keep `:mention`. T1 settles which list holds, the
harness's list follows the code (13.1, item 1), and each result file says
which case ran.

### 4.3 Variant A: one agent session layer, K writers sharing one door

This is the number as asked, in the slices' shape.

- **Writers.** Each is a thread in a closed loop with no pause: build a
  value act (a fresh name each time; the value string distinct per writer
  and act, as the slices' `value-for`), send it with `c/offer!` (the door
  seals it, then an acked append that returns once the gate's event is
  complete and its writes are visible), take the answer, send the next.
  All K writers share one store handle, so one door, one pool of leased
  locks and one lease at a time. Latency is `System/nanoTime` around
  `c/offer!` only.
- **Warm-up.** The 10-second idle window (3.2), then 4 writers for 20
  seconds, as the slices warmed.
- **Levels.** K = 1, 4, 16, 32, 64, 128. At each: 3 seconds at K
  unmeasured, then a 15-second measured window. Past 128, K = 256 and then
  512, each only while the rate gained at least 10% over the level before,
  p99 is at most 200 ms and nothing errs; the run applies this rule to its
  own windows.
- **Recorded per window** (a `RESULT` line): value offers sent and every
  answer (yes, no with its reason, error); lease acts, both counts (2.5);
  the rate, admitted value acts over the time from the window's start to
  the last writer's last ack; latency p50, p95, p99, max and mean by
  nearest rank over every offer in the window (**[F5]** closed-loop
  service times: reported, not judged, and labeled so); placement (each depot
  partition's growth, each task's `$$clock` movement); every thread's CPU
  over the window (`ThreadMXBean`), with the home task thread's share of a
  core; GC count and time; the home clock's lead over the wall clock in
  ms (the hybrid clock should hold it near zero, where the slices'
  millisecond stamps ran 21 s ahead); whether another JVM ran.
- **Read back after each window, never during it.** Up to 200 admitted
  acts, sampled one in 50 as they were answered, checked against every
  write in 2.3's agent column: the answer record says yes with the act's
  stamp; the log holds one row, with the sealed bytes, the lock id, and the
  lock record inside the row (an agent layer keeps it there); the head at
  that stamp; the `:ix-ek`, `:ix-ke` and `:ix-s` entries at their
  addresses (`reads/address`, **to confirm at build** for `:ix-s`); the
  cited lease row gone; the stamp-to-name entry; no lock row. This is what
  "with every index written" is checked by.
- **Derived per window**: index writes a second, all, read indexes and
  deletes, by 2.5's sum.

### 4.4 Variant A': the same act under a by-value grammar

In the same JVM after A's levels: K = 1, 4, 16 on `:bench-agent-v`, 3 s
unmeasured and 15 s measured each, recorded and checked as in 4.3, the
read-back adding the `:ix-kv` entry and the `:ix-of` set. It shows the
range of the index-write count for one small act (9 to 11) and whether two
more writes an act move the rate.

### 4.5 Variant B: S agent sessions on one task, each at an assumed agent speed

The divisor, measured instead of divided out.

- **Setup.** The person `:ada`; S session layers `:bench-a<i>`, each
  `{:kind :agent :owner :ada}`, all homed on one task. Placement is by
  layer (`hash-by :layer`), so the harness makes candidate layers one at a
  time and keeps those whose making grew the target task's partition
  (about one candidate in four). Each kept layer gets its root grant and
  its own session. Each session has its own store handle (`c/connect`),
  so its own door and pool: no session waits on another's lease, as no
  real agent process would.
- **Writers, open loop.** Each session has one writer that sends a value
  act every 10 ms, 100 acts a second (*assumed*: the README's "an agent at
  full speed writes about 100 a second"), on a fixed schedule
  t(n) = t(0) + n × 10 ms with a random phase per session. A send that
  falls behind its schedule goes at once, and its latency is measured from
  its scheduled time, not from when it was sent, so a queue at the task
  shows as latency instead of the writers quietly slowing down.
- **Levels.** After the idle window and a 20-second warm-up at S = 10:
  S = 10, 20, 40, 80, 160, which offer 1,000 to 16,000 value acts a second.
  Each: 3 s unmeasured, 15 s measured. The run stops after the
  first level where fewer than 90% of the offered acts were admitted in
  the window, or p99 passed 100 ms, or anything erred.
- **Recorded per level**: offered and admitted value acts a second, lease
  acts a second, index writes a second, latency from schedule (p50, p99,
  max), the home task thread's CPU share, and placement (every decision on
  the one task).
- **The result**: the largest S at which at least 95% of the offered acts
  were admitted and p99 stayed at or under 20 ms (number 3's latency line,
  reused, *assumed*). It reads "this many agent sessions at 100 acts a
  second each, on one task, on this machine". It does not say how fast
  real agents write or how many run at once, the README's two missing
  inputs; they stay Sid's (section 13).

### 4.6 Variant C: the agent reads before it writes, each read recorded

- **Loop.** One session layer, K = 1, 4, 16 writers. Each iteration: a
  point read of the fact that writer had admitted last (the first
  iteration reads a fact written at setup), through
  `rig.store.read-exit/read!` with `:reader :ada`, `:reader-kind :model`,
  `:for :ada`, the session layer as both the layer read and the working
  layer, **[F3]** the owner's root permission `[:ada L L]`, role
  `:stood-on`; then a value act standing on that fact at its stamp
  (`:stood-on {fid stamp}`), as an agent's act stands on what it read.
- **[F3] Why the root permission.** The exit builds the entry with the
  reader as `:who` and no session (read_exit.clj `entry-offer` :78 on
  `703b8e26`), so the door writes it in `:door/ada`. The session's
  permission `[:bench-s1 L L [:ada L L]]` covers only an offer whose `:who`
  or `:session` is `:bench-s1` (permit.clj `refusal` :65-69 on
  `e60c8ee1`), and a reader that is not a person cannot lease: its lease
  is refused `:no-such-person` (locks.clj `persons-refusal` :682) and its
  entry `:no-such-lock`. So the session's permission refuses every read
  either way. If the merged exit carries a session into its entry, the
  session's permission instead. The entries lease in `:door/ada` and the
  value acts in `:bench-s1`, one lease per 64 of each: still one lease per
  32 iterations.
- **Per iteration**: one entry act and one value act, each sealed at the
  door under its own lock, so a lease act every 32 iterations. Writes (2.3):
  the entry act 10, the value act 9 plus one `:stood-on` row, so about
  20 + 72/32 = 22.3 an iteration (*derived*).
- **Reported per level**: iterations a second; value, entry and lease acts
  a second; index writes a second; the lock rows the entries wrote (the
  `:locks` growth, one per entry, from the `:own-row` mark in an agent
  layer); latency of `read!` (its query and its entry's acked offer) and
  of the write, each p50, p99 and max.
- **Entry bytes (optional).** The read exit's plan calls an entry line
  with an exact list "the number to watch" at a model's call rate. After
  C's windows: a model's pattern reads `[:k :note]` with exact lists of
  10, 100 and 1,000 matched facts, and the logical bytes (Rama's
  serializers, as in 6.4) of each entry act's row, its three id-index
  entries (no copy) and its lock row.
- **What it informs.** RIG.md default 4 records agent session reads
  through the exit, which "R5 partly reopens" against ruling 3's "agent
  session layers may default to none". C prices that default in acts and
  writes a second. It decides nothing (section 13).

### 4.7 Diagnostics: run and reported, never numbers

- **D1, the door without its lease stalls.** Only when A's rate has
  flattened (under 10% from K = 64 to K = 128) while the home task thread
  stayed under 60% of a core: one more K = 128 window with the pool
  stocked ahead. It gives the gate's rate when no writer waits on a lease.
  The stocking lease acts run before the window and are reported apart.
  **[F4]** Stocked by `c/lease!` of 256, repeated until the leases hold
  twice the window's expected acts (A's K = 128 rate × 18 s), then one
  `c/stock!`, whose single `lease-locks` call takes every unconsumed lock
  of the session into the pool. Not `c/stock!` each time: its `refresh!`
  reads and unleases every standing lease row of the session on the home
  task (locks.clj `lease-locks>` :1378, `leased-locks` :896), so n
  stockings read about 256 × n² / 2 rows, some 16 million for 90,000
  locks (*derived*), on the task being measured. The window counts as D1
  only if the door's known leases did not grow in it; otherwise it is
  marked "the pool ran dry". The stocking's own time is reported. Expect
  D1 to run: a lease is 64 locks and one thread leases at a time
  (client.clj `assign!` :155), so one door keeps at most about 64 acts in
  flight between leases, each lease act queued behind them (*derived*).
- **D2, the permission walk.** One K = 16 window in A's layer with acts
  citing the owner's root permission (a chain of 1) instead of the
  session's (a chain of 2), beside A's own K = 16 window: whether one more
  permission read per act shows at all (PLAN-micro-store.md, "the 1.5
  seeks").
- **D3, disk syncs per act.** Not run unless a result needs it: the
  slices' strace road (runs/phase7-agent-rate.txt, "WHAT THE ACK LATENCY
  IS MADE OF"), for the case where K = 1 latency moves far from the
  slices' 3.3 ms.

### 4.8 Checked in every window

A window that fails a check is kept, marked, and shown as such by the
summary.

- Every answer counted; no error; no refusal (a refusal is printed with
  its reason; the workloads make none by design).
- One task: only the home partition grew, by exactly the value, entry and
  lease offers sent; only the home task's clock moved.
- The two lease counts agree.
- The sample read-back of 4.3 (and 4.4, 4.6) is complete.
- No other JVM ran during the window (the overlap monitor, section 8.4).

### 4.9 Run length (derived from the levels, JVM start assumed)

Per run: JVM start, compile and module launch about 40 s (*assumed*, the
slices did not record it); setup about 10 s; idle 10 s; warm-up 20 s; A
six levels of 18 s (108 s) and up to two more (36 s); A' three levels
(54 s); D2 18 s. About 4.5 minutes, 5 with D1; **[F4]** about 5.5 with
D1 and its stocking, which should be expected to run. Variant B: about 1 minute
of setup (some 640 candidate layers made one at a time, 160 grants and 160
sessions), the idle window and warm-up, and five levels of 18 s, about 3.5
minutes with the JVM. Variant C: setup, warm-up and three levels, about 2
minutes. Three runs of each.

## 5. Number 3: one person's layer on one thread, acts a second and latency

### 5.1 What it answers

README.md: ruling 2's placement by layer, with re-class as the way out for
a hot layer; a bad answer says whether "one thread can carry a person, or
a person's layer needs splitting". Every offer into a one-owner layer is
decided on that layer's one task (*code*: the gate's `<<sources` block has
no partitioner; *slice*: every window one-task), and that task has one
thread under `{:tasks 4 :threads 4}`.

### 5.2 Setup, per run

The store layer `:people`; the person `:pat`; the personal layer
`:bench-person`, `{:kind :personal :owner :pat}`, per-value grain; the
owner's root permission `[:pat :bench-person :bench-person]`. The person
writes as `:who :pat`, citing that permission, in the door's default
session `:door/pat` (*code*: client.clj `default-session` and `build`).
No grammar, so the value act writes 10 (2.3): a personal layer keeps a
lock row per value. The home task is recorded as in 4.2.

### 5.3 (a) One writer, one act at a time

After the idle window and the 20-second warm-up at 4 writers: 1,000 acts
unmeasured, then **6,400 measured**, each `c/offer!` timed. One offer in
64 carries a lease, which is more than one in 100, so the slowest 1 in 100
will be a leased offer at any sample size (*derived*). The slices' 2,000
would hold 31 leased offers, too few to describe the leased offers' own
spread; 6,400 holds 100. Each offer is tagged "leased" when the door came
to know a new lease during its call, which is exact with one writer.
Reported: acts a second; p50, p95, p99, max and mean over all offers; the
same for leased and unleased offers apart.

### 5.4 (b) K writers sharing one door

K = 1, 2, 4, 8, 16, 32, 64, 128, the slices' levels (64 and 128 were past
the asked range then and stay so). At each: 3 seconds unmeasured and a
10-second measured window. Recorded, read back and checked as in 4.3 and
4.8; the sample read-back adds the lock row, which a personal layer
writes. Reported per level: value acts a second, all acts a second,
latency (typical p50, slowest 1 in 100 p99, worst max, with p95 and mean),
the task thread's CPU share, lease acts, index writes a second, and
placement ("every offer decided on the layer's one task").

**[F5]** (a) and (b) are closed loops. A stall (a disk flush, a GC pause,
a lease round trip) delays only the offers in flight, and the offers an
arrival at the same rate would have made during it are never sent, so
none of them records it: their p99 is a service time at a fixed number in
flight, and it understates what a person writing at a rate would see
(coordinated omission). The result file says so beside them, and the
latency threshold, itself a statement about a rate, is judged on (b').

### 5.4b (b') Open arrival at the threshold's rate **[F5]**

After (b): value acts at 100 a second (the threshold's rate) and at 1,000
a second (ten times it), each on a fixed schedule t(n) = t(0) + n / rate
with a random phase, sent by a pool of threads (4 at 100 a second, 32 at
1,000) that each take the next slot, so a late ack delays no other send; a
send that falls behind its slot goes at once, and its latency is measured
from the slot, not from when it was sent (variant B's accounting, 4.5).
Each level: 3 s unmeasured, 15 s measured. Recorded, read back and checked
as (b); reported: offered and admitted value acts a second, and latency
from the schedule (p50, p95, p99, max, mean). The latency threshold (100
acts a second at 20 ms or less for the slowest 1 in 100) is judged on the
100-a-second window, by 7.2's rule; the 1,000-a-second window says whether
the margin holds at ten times the rate. In the minimum set.

### 5.5 (c) The person's own reads (optional)

2,000 sequential point reads of the person's own facts through the exit
(reader kind `:person`, the personal layer as the layer read and as the
working layer), each `read!` timed: p50, p99 and max. A person's read waits
on a query and on an acked entry, the exit's two round trips
(PLAN-read-exit.md, "Minimization").

### 5.6 Run length

JVM about 40 s; setup and idle 20 s; warm-up 20 s; (a) about 7,400 offers
at roughly 3.5 ms, 26 s; (b) eight levels of 13 s, 104 s; **[F5]** (b')
two levels of 18 s, 36 s. About 4 minutes a run, three runs. (c) runs once in a JVM of its own in the full
set (8.2): setup and warm-up, then 2,000 reads at an estimated 7 ms each
(a query and an acked entry), about 1.2 minutes.

## 6. Number 2: lock store growth under hand layers, bytes per value and the curve over 100,000 values

### 6.1 What it answers

README.md: ruling 7 gives every value its own lock, a row of its own in
personal and hand layers, and forgetting destroys the lock. A bad answer
says "whether a lock row per value is an affordable default, or hand
layers lock per act or keep locks in the record".

### 6.2 The finished store's lock row (*code*, `rig-build-locks` locks.clj at `fd41f6d2`)

- **Where**: `$$layers [L :locks lock-id]`, written in personal and hand
  layers (`row-kinds` :364, `row-lock?` :366), one per value under
  per-value grain and one per act under per-act (`lock-plan` :608).
- **Key**: the lock id `[lease-name i]` (L21), the lease act's name and
  the lock's index in it.
- **Record**: `{:scheme :aes-gcm-1 :required [..] :any-of [..] :blob
  <bytes> :any-blobs nil}` (`lock-record-schema` :953), raw bytes in
  `byte/1` slots (L4). The slice measured both base64 text (its plan's
  choice then) and raw bytes; the finished store keeps raw bytes, so the
  slice's raw variants are the ones to compare (10.4).
- **Wrap in a hand layer** (`wrap-of` :253): an unmarked value is wrapped
  under the owner alone; a value marked `:die-with-any` under every
  subject, the owner among them, as a chain of seals in sorted order. Each
  seal adds 28 bytes, a 12-byte nonce and a 16-byte tag (`wrap` :290).
- **Expected** (*derived*, to be measured): about 180 bytes logical per
  row: a key near the slice's 55-byte per-act id `[:act name]`, and a
  record of the slice's 110 raw bytes plus `:scheme :aes-gcm-1`. The slice
  measured 169 bytes raw (runs/phase7-lock-growth.txt).

### 6.3 The workload

- **Cluster and setup, per variant.** A fresh cluster,
  `{:tasks 1 :threads 1 :workers 1}`. The store layer `:people`; the
  persons `:alice` (the owner) and, for the subject variants, `:bob`,
  `:carol`, `:dave` and `:erin`; the hand layer `:alice-hand`, `{:kind
  :hand :owner :alice}`, at per-value grain from its making act; the root
  permission `[:alice :alice-hand :alice-hand]`. For the `:mention`
  variants, the `:mention` grammar `{:subjects-at [:persons]}` written in
  the hand layer as a fact first, as the tools plan says the bench needs
  (**to confirm at build**; if the merged gate has no grammar facts yet,
  the offer carries the persons in `:subjects` with the mark, which gives
  the same wrap: *plan*, tools-and-grammars 4.6 [V-F3]). For the per-act
  variant, the owner's grain switch `{:e :alice-hand :k :lock-grain :v
  :per-act}` first.
- **Values.** The slice's seeded generator (`rig.bench.lock-bench/value-spec`,
  seed 20260925), so `h40`, `h200` and `h40-p2` get the same values in the
  same order at the same sizes as the slice: `{:text "..."}` whose
  canonical EDN is exactly 40 or 200 bytes under key `:note`, and
  `{:persons #{:bob} :text "..."}` of 40 bytes under `:mention`, marked
  `:die-with-any`. The slice's generator names Bob only, and four names do
  not fit in 40 bytes (`{:persons #{:bob :carol :dave :erin} :text ""}` is
  already longer), so `rig.bench.lock-growth` carries its own generator,
  the slice's extended to a set of persons and checked to reproduce the
  slice's first 100 values for the three variants above; `h40-p3` and
  `h40-p5` use 64-byte values. The lock row does not depend on the value's
  size (the slice's finding, 169 bytes at 40 and at 200), so the slope per
  subject compares rows across these variants. The harness asserts, for
  the first 100 values of every variant, that the door's plaintext
  (`locks/canonical-bytes`, **to confirm at build**) has exactly the
  intended size.
- **Writers.** 64 threads sharing one door, each sending one act at a
  time with `c/offer!` as `:who :alice` under the root permission in the
  door's default session, taking values in order from one shared counter.
  At every point all writers finish their act and wait; the point is
  measured; they go on. The slice kept 64 asynchronous appends in flight;
  the finished store's appends go through the door, whose `c/offer!`
  waits for its answer, so 64 threads keep 64 in flight.

| Variant | Values | Value | Key and mark | Wrap, required | Grain | Lock rows at the end | Set |
|---|---|---|---|---|---|---|---|
| `h40` | 100,000 | 40 B | `:note`, none | `[:alice]` | per value | 100,000 | minimum |
| `h200` | 100,000 | 200 B | `:note`, none | `[:alice]` | per value | 100,000 | minimum |
| `h40-p2` | 100,000 | 40 B | `:mention` `#{:bob}`, `:die-with-any` | `[:alice :bob]` | per value | 100,000 | minimum |
| `h40-p3` | 10,000, a point every 2,000 | 64 B | `:mention` `#{:bob :carol}`, marked | 3 persons | per value | 10,000 | minimum |
| `h40-p5` | 10,000, a point every 2,000 | 64 B | `:mention` of four, marked | 5 persons | per value | 10,000 | minimum |
| `h40-again` | 100,000 | 40 B | as `h40` | | | 100,000 | full |
| `h40-act4` | 100,000 in 25,000 acts of 4 | 40 B | `:note`, none | `[:alice]` | per act | 25,000 | full |

The hand layer of every variant holds only that variant's values, so its
lock rows are the lock store and nothing else. That an unmarked `:mention`
of Bob costs what `h40` costs (wrapped under the owner alone) is checked
by the test namespace (T8), not by a variant.

### 6.4 Measured at every point

1. **Logical.** Every lock row of `[:alice-hand :locks]` read back in pages
   of 10,000 (`sorted-map-range` paging, the slice's `lock-rows-logical`
   generalized to any layer), each key sized by Rama's key encoder
   (`rpl.rama.api.durable.rocksdb.key-encoding/k-ser`) and each record by
   its PState value serializer (`rpl.rama.util.nippy-serialization/freeze`),
   the slice's sizing. Count, total bytes, smallest and largest row.
2. **Lease rows standing**: the count under `[:alice-hand :leases
   <session>]`, at most 64 plus those in flight, and not part of the lock
   store (phase 2's M2 note, 1.6).
3. **Compacted, the whole layer store.** The home task's `$$layers` RocksDB
   directory copied to scratch (without its `LOCK` and info `LOG`),
   opened with the options Rama wrote into it, every column family
   flushed and then compacted with `BottommostLevelCompaction/kForce`: the
   slice's forced rewrite, without which a lone flushed file is moved and
   not rewritten (runs/phase7-lock-growth.txt, "Runs"). Its SST bytes, and
   every key and value counted, per column family.
4. **Compacted, the lock store alone**: the lock rows picked out of that
   compacted copy and re-packed alone (6.5). Its SST bytes are the lock
   store's bytes on disk.
5. **The rest**: (3) less (4), everything else the store keeps for these
   values: rows, answers, heads, indexes, stamp-to-name entries. This is
   the denominator of the README's closing reading ("measured against the
   rows the store already writes for each value").
6. **The live directory** as it stands (write-ahead log, SST, manifest,
   options), for its shape only; the slice found it jumps at memtable
   flushes (runs/phase7-lock-growth.txt, "The curve").
7. **The replication log's** segment bytes, for reference, as the slice.

At the end of each variant, on 100 sampled values: the stored row's
`:sealed` is not the plaintext; the value opens through its lock row and
the person entries read from `$$persons` (`locks/unwrap`, then
`locks/open`; **to confirm at build**) to its canonical text; it does not
open without `:alice`'s entry, nor, in the `p` variants, without each named
person's; the lock-row count equals the values (per value) or the acts
(per act); nothing was refused and nothing erred.

### 6.5 Picking the lock rows out on disk, and why the pick can be trusted

The slice measured the lock store on disk as a difference: the same
values written with lock rows and without. The finished store cannot write
a hand layer without lock rows, and every other field of the layer lives
in the same PState, so the rows are picked out of the compacted copy
instead:

- **Pick by value.** Iterate the copy's `subindexed` column family and
  thaw each value with Rama's `rpl.rama.util.nippy-serialization/thaw`
  (the Rama 1.6.0 jar has `nippy_serialization$thaw`; its signature is
  **to confirm at build**). In a hand layer a lock record is the only value
  that is a map carrying `:scheme` and `:required`: a lease row is `{:under
  :sealed}`, rows and index entries carry `:e` and `:k`, and a hand layer's
  rows keep `:lock` nil. After the first point the scan seeks straight to
  the range the first point found. **[F9]** A value `thaw` does not
  accept (the references of nested subindexed structures such as `:log`'s
  vectors and `:leases`' session maps, the size entries of `:log`'s
  vectors, module.clj L65-67) is not picked and is counted as unthawed,
  never dropped silently; the count check below says whether a lock row
  was among them. The full scan, with both checks, runs at the first and
  at the last point; the points between seek to P's range.
- **Check that the pick is exactly one structure.** Every picked key must
  start with one common prefix P, and every key that starts with P must
  have been picked. **[F9]** P is the longest common prefix of the picked
  keys. The check assumes nothing about where Rama puts a structure's
  reference id: if it leads the key, only lock rows start with P and the
  check holds; if not, keys of other structures (names are `[layer class
  :offer uuid]`, lock ids `[name i]`) can share P, and the check fails into
  the fallback below, never into a wrong number. Rama addresses a subindexed structure's elements under
  a reference id (the jar has `key_encoding$freeze_SubReferenceID`), and
  the slice found each lock row's RocksDB key 3 bytes longer than its lock
  id's own encoding ("the subindex key prefix"), which is what one prefix
  per structure predicts. If the check holds, the pick is `[:alice-hand
  :locks]` and nothing else.
- **Check the count and the size.** The picked entries number exactly the
  logical pass's rows, and their key and value bytes are within a few
  bytes a row of the logical sizes (the slice: within 3).
- **Re-pack.** A fresh RocksDB in scratch, with the DB options and the
  `subindexed` family's options loaded from the copy; the picked entries
  put in key order; flushed; force-compacted; its
  `rocksdb.total-sst-files-size` taken.
- **If a check fails** (a merge changed the record's shape, or the rows do
  not sit under one prefix): the lock store's compacted bytes are
  estimated as the whole store's compacted bytes times the lock rows'
  share of its raw key-value bytes, the result says "estimate" and names
  the check that failed. No other road is taken silently. **[F9]** What
  the estimate cannot show, and the result says: it assumes the lock rows
  compress as the store's average does. A lock row carries 60 bytes of
  random ciphertext; the rest of the store mixes random bytes (sealed
  values, digests) with repeated keywords and addresses; so the
  estimate's error has no known sign. Two bounds hold whatever the layout,
  and are printed beside it: at least 60 bytes a row (the sealed lock does
  not compress) and at most the rows' raw key and value bytes plus block
  overhead (the slice's raw rows: 172.2 B raw, 86.9 B compacted).

Alternatives weighed: RocksDB's approximate size of the key range [P, P+1)
needs the same P and is an estimate over it, so it is kept as a
cross-check printed beside the re-pack; the slice's difference needs a
second store with the same rows and no lock rows, which the finished
store cannot make without changing its code.

### 6.6 Reported

- **Bytes per value of the lock store** at 100,000 values: logical, raw
  key-value, compacted; the key and the record apart, and the record's
  parts (the 60-byte sealed lock, the wrap vectors, `:scheme`, the nil
  `:any-blobs`) sized with the serializer, as the slice's breakdown.
- **The curve**: the ten points of each variant, each step's increment,
  and the largest departure of a step from the mean step (the slice:
  exactly linear in logical bytes, within 0.4% to 1.3% on disk).
- **Each extra subject**: bytes per lock row at 1, 2, 3 and 5 required
  persons (`h40`, `h40-p2`, `h40-p3`, `h40-p5`), logical and compacted, and
  the least-squares slope per extra person. From the code each extra
  required person adds one 28-byte seal and one keyword in `:required`
  (*derived*); the run measures it.
- **Ratios**: lock store over the plaintext value (40 and 200 bytes),
  logical and compacted; lock store over the rest (6.4, item 5).
- **Per-act grain**, if run: 25,000 lock rows for 100,000 values, and the
  bytes per value.
- **Outside the lock store but part of the lock design**, as the slice
  reported it: the row's `:sealed` (28 bytes over the plaintext),
  `:lock-id` and `:digest` (32 bytes), sized per row; and the lease rows
  standing at each point.
- **The verdict** (section 7).

### 6.7 Run length

Each 100,000-value variant: the JVM about 40 s; setup a few seconds;
writing at an *assumed* 2,000 to 5,000 acts a second through the door, 20
to 50 s; ten points, each copying and force-compacting a layer store that
grows to an estimated 150 to 250 MB by the last point (*derived* from 2.3:
about ten entries per value, most carrying the sealed row), a few seconds
each. About 2.5 minutes a variant at 3,000 acts a second, 3.5 at 2,000. `h40-p3` and `h40-p5` about a minute
each. Minimum set about 9 to 12 minutes; the full set adds about 5 to 7 (8.6).

## 7. The thresholds and the verdicts

### 7.1 Default 7's thresholds, assumed and not Sid's

| Number | Assumed threshold | Judged on |
|---|---|---|
| Agent rate | at least 1,000 admitted acts a second on one task, with every index written | variant A's value acts a second at each K, with the read-back complete; variant B's largest S reported against the same line (S × 100 acts a second) |
| One person's layer | at least 100 acts a second, at 20 ms or less for the slowest 1 in 100 | value acts a second at each K and at one sequential writer; **[F5]** p99 on (b')'s open arrival at 100 acts a second, with the closed-loop p99 of (a) and (b) beside it, labeled |
| Lock store | at most twice the value bytes is fine; above four times, change the default | lock-store bytes per value over the plaintext value's bytes, at 40 and 200 bytes, logical and compacted |

Every result file prints the line "thresholds assumed (RIG.md default 7),
not Sid's".

### 7.2 The rule for timings

The slices' rule, kept so the verdicts compare: **far**, at least ten
times on one side of the threshold, decides; **near**, within ten times,
means the in-process cluster cannot decide. Each K is judged on its own;
the verdict names the side and the factor.

### 7.3 The rule for bytes

Logical bytes are exact for Rama 1.6.0's serializers and are judged
directly against 2× and 4×. Compacted bytes depend on the RocksDB options
Rama sets on the in-process cluster (Snappy, 4 KB blocks, a 10-bit bloom
filter, runs/phase7-lock-growth.txt); they are judged the same way and
carry that caveat. The denominator is the plaintext value, the slice's
reading; the ratio to the rest (6.4, item 5) is printed beside it,
because the threshold does not say which bytes it means
(runs/phase7-lock-growth.txt, "What is uncertain").

## 8. How the runs execute

### 8.1 Under the cluster lock

Every JVM that starts an in-process cluster, each measured run and the
harness test alike, runs from the rig folder as

    flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock clojure -M:bench <ns> <fn> <args>

(`clojure -M:test rig.bench.numbers-test` for the test). `flock` waits for
the lock with no timeout, holds it for that one JVM and lets it go when the
JVM exits, so a run waits behind any other session's cluster, and no two
clusters try to bind port 2002 at once (the slices' collision at 11:10:33,
BENCH_NOTES-locks.md). The lock is taken per run, not for a whole set, so
another session can use the machine between runs; the overlap monitor
(8.4) says whether anything else ran during a window. Before each step the
driver tries the lock without waiting (`flock -n`); if it is held, it
writes "waiting for the cluster lock" to the progress file and then waits,
so a watcher can tell a wait from a hang.

### 8.2 The driver, in the background

`test/rig/bench/phase7-final.sh <set>`, where `<set>` is `test`, `min`,
`full`, **[F11]** `short` (8.6), or one step's name, started once from the
rig folder:

    nohup test/rig/bench/phase7-final.sh full > runs/phase7-final-driver.log 2>&1 &

For each step it appends a start line to `runs/phase7-final-progress.log`
(the time, the step, the load average, any other JVM running), runs the
step's JVM under the lock with its output to `runs/phase7-final-<step>.log`,
and appends an end line (exit code, duration). The steps, in order:

| # | Step | Command (`clojure -M:bench ...` unless said) | Set |
|---|---|---|---|
| 1 | `test` | `clojure -M:test rig.bench.numbers-test`; the driver stops if it fails, because a harness that does not measure what it claims measures nothing | both |
| 2 | `agent-rate-1` to `-3` | `rig.bench.agent-rate run <n>`: A, A', D2, and D1 when its condition holds | both |
| 3 | `one-thread-1` to `-3` | `rig.bench.one-thread run <n>`: (a), (b) and **[F5]** (b') | both |
| 4 | `lock-growth-<v>` | `rig.bench.lock-growth run <v>` for `h40`, `h200`, `h40-p2`, `h40-p3`, `h40-p5`, a JVM each | both |
| 5 | `agent-sessions-1` to `-3` | `rig.bench.agent-rate sessions <n>`: B | full |
| 6 | `agent-reads-1` to `-3` | `rig.bench.agent-rate reads <n>`: C and the entry bytes | full |
| 7 | `lock-growth-<v>` | `h40-again`, `h40-act4` | full |
| 8 | `one-thread-reads` | `rig.bench.one-thread reads 1`: 5.5 | full |
| 9 | `report` | `rig.bench.numbers report`: no cluster and no lock; writes the three `.txt` files from the `.edn` files | both |

The timing runs go first, on the quietest machine the night will give;
bytes do not care about noise, so the lock store follows. Variant B leads
the full set because it turns number 1 into the yardstick's divisor; C
prices a default; the rest repeat or extend.

### 8.3 Progress a watcher can see

While writers run, every harness prints a `PROGRESS` line every 5 seconds
and one at each window's start and end: `<time> <number> run=<n>
<variant> K=<k> t=<s> acts=<n> rate=<r>/s errors=<e>`. The lock store
prints one at every point and every 5 seconds of writing. These go to the
step's `.log`; the driver's progress file has one line per step start and
end. A watcher runs `tail -f runs/phase7-final-progress.log
runs/phase7-final-*.log`. The `.log` files are ignored by git
(`runs/*.log`, the rig folder's `.gitignore`), as the slices' console
logs were; nothing a result needs lives only in them.

### 8.4 Quiet, and a busy port

- **The overlap monitor**: a daemon thread in the harness lists every
  other Java process on the machine (`ProcessHandle/allProcesses`) every 2
  seconds while a window runs. A window during which one ran is marked
  `:overlap` with the processes' command names. The summary uses the clean
  runs when at least two of three are clean, and otherwise reports the
  marked runs with their mark.
- **A cluster that cannot bind port 2002** ("Address already in use") is
  retried every 30 seconds, up to five times, and each retry is logged.
  Under the lock this should not happen; the slices met it without one.

### 8.5 Where results land

| File | In git | What it holds |
|---|---|---|
| `runs/phase7-final-agent-rate.edn`, `runs/phase7-final-one-thread.edn`, `runs/phase7-final-lock-growth.edn` | yes | every `META` and `RESULT` map of every run, appended by the harness as it goes, one EDN map a line |
| `runs/phase7-final-agent-rate.txt`, `runs/phase7-final-one-thread.txt`, `runs/phase7-final-lock-growth.txt` | yes | the method header: the machine (from `META`), the configuration, the method (**[F5]** which latencies are closed-loop service times and which are from an open schedule; **[F1]** the primary act's key), the per-act write list with its code sites, and the caveat of 3.3; then the tables (median and spread across runs), the checks, the verdicts against default 7, and the slice's numbers beside the final ones. `report` writes all of that; the session that runs the numbers adds the reading in words |
| `runs/phase7-final-<step>.log`, `runs/phase7-final-progress.log`, `runs/phase7-final-driver.log` | no | console output and progress |

### 8.6 Time

| Step | Per run (*derived* from the windows; JVM start about 40 s, *assumed*) | Runs | Total |
|---|---|---|---|
| test | about 3 min | 1 | 3 min |
| agent rate (A, A', D2, and D1, which **[F4]** expects to run) | about 5.5 min | 3 | 16.5 min |
| one person's layer ((a), (b), **[F5]** (b')) | about 4 min | 3 | 12 min |
| lock growth, 100,000 values | about 2.5 min at 3,000 acts a second through the door (*assumed*), longer if the door is slower | 3 | 7 min |
| lock growth, 10,000 values | about 1 min | 2 | 2 min |
| report | under 1 min | 1 | 1 min |
| **minimum set** | | | **about 42 min** |
| agent sessions (B) | about 3.5 min | 3 | 10.5 min |
| agent reads (C) | about 2.1 min | 3 | 6.5 min |
| lock growth, `h40-again` and `h40-act4` | about 2.5 min | 2 | 5 min |
| one person's reads | about 1.2 min | 1 | 1 min |
| **full set** | | | **about 65 min** |

Say about 45 minutes for the minimum and about 65 for the full set,
plus any wait for the lock. If the door proves slower than assumed, the
lock-growth steps stretch first: 100,000 values at 1,000 acts a second
instead of 3,000 adds about a minute a variant.

**[F11] The shortest set**, for when less than 45 minutes remain:
`test`, `agent-rate-1`, `one-thread-1`, `lock-growth-h40`,
`lock-growth-h200` and `report`, about 19 minutes (3 + 5.5 + 4 + 2.5 +
2.5 + 1). It answers each of Sid's three numbers once. Its timings are
single runs, so the spread across runs is unknown, and the report says so
beside every timing; the lock variants run once in every set, so `h40`
and `h200` lose nothing but the subject slope (`h40-p2` to `h40-p5`). The
driver takes it as `<set>` = `short`.

### 8.7 When something goes wrong

A run that exits non-zero is logged, and the driver goes on to the next
step: no step depends on an earlier one's output, except that nothing runs
after a failed `test`. A window whose check failed is kept and marked, not
dropped. Nothing is rerun by the script; the session that reads the results
decides, and the `.txt` says what it decided and why.

## 9. What the build step writes

### 9.1 The files

All new, all in the rig folder. Nothing that exists changes.

| File | Namespace | What it is |
|---|---|---|
| `test/rig/bench/numbers.clj` | `rig.bench.numbers` | the shared harness |
| `test/rig/bench/agent_rate.clj` | `rig.bench.agent-rate` | number 1 |
| `test/rig/bench/one_thread.clj` | `rig.bench.one-thread` | number 3 |
| `test/rig/bench/lock_growth.clj` | `rig.bench.lock-growth` | number 2 |
| `test/rig/bench/numbers_test.clj` | `rig.bench.numbers-test` | the tests that each harness measures what it claims |
| `test/rig/bench/phase7-final.sh` | | the driver of 8.2 |

**Why `test/` and nothing under `src/`.** The harnesses run on the
`:bench` and `:test` aliases' classpath, drive the store's own module and
declare none. The slice's `src/rig/bench/lock_slice.clj` sat under `src/`
because it was a module; the final lock store needs none.

**Left as they are.** Everything under `src/rig/store/`: a measurement
never changes what it measures. The slice benches
(`src/rig/bench/lock_slice.clj`, `test/rig/bench/stream_bench.clj`,
`test/rig/bench/lock_bench.clj`) and their run files, the record of how the
slice numbers were made. `rig.bench.lock-growth` requires
`rig.bench.lock-bench` for six public pure helpers (`value-bytes`,
`key-bytes`, `rocks-dirs`, `live-bytes`, `ipc-root`, `replog-bytes`), and
for its `value-spec` only as the reference its own generator is checked
against (6.3). If the merged store stops that namespace from loading, the
build copies those seven into `rig.bench.lock-growth` unchanged and says
so in its notes.

### 9.2 The named pieces

`rig.bench.numbers`, the shared harness:

- `machine`: the `META` map of 3.1.
- `with-store`: a fresh in-process cluster with the finished store
  launched under a given launch config, retrying a busy port (8.4); yields
  the store handles (`c/connect`, and the read exit's `connect` where a
  workload reads).
- `people!`, `person!`, `layer!` (make, grant, optionally open a session
  and write a grammar; returns the layer spec with its home task), and
  `layers-on-task!` (variant B's candidate search).
- `writes`: 2.3's per-act write lists as data. For each act kind, each
  write's name, path head, set or delete, and code site; and
  `per-act-writes`, kind to total, read-index subset and deletes; **[F2]**
  and the one-time write a layer's first fact under a key that is not a
  store key makes (one `:key-rows` row, where the merged store has key
  rows). This is the harness's claim, and T1 to T4 test it.
- `field-counts`: for a layer, the entry count of every field of its
  `$$layers` value, the fields enumerated from the store itself (**to
  confirm at build**: `MAP-KEYS` on the layer's value, else the merged
  module's field map), and `$$clock` per task. Nested fields are counted
  at their leaves: `:leases` by session then lock id, `:stood-on` by act
  then fact; `:log` by act, with its rows counted beside; a field that is
  one value (`:settings`) is compared, not counted.
- `offer-loop` and `window` (closed loop: the slice's
  `stream-bench/window`, extended with lease counting, a lease tag per
  offer, entry acts and the overlap mark), `open-loop-window` (variant B's
  schedule, latency from the scheduled time; **[F5]** with a pool of
  sender threads for number 3's (b')), `read-loop` (variant C and
  5.5). Each writer keeps its latencies in a growable primitive `long`
  array, not a list of boxed longs, so the harness's own garbage stays out
  of the latencies it measures (the client threads share the JVM with the
  cluster).
- `lease-counts`: the door's known leases for [layer, session], and the
  home depot partition's growth.
- `check-sample`: every write of an act's kind read back for sampled acts.
- `thread-cpu`, `busiest`, `gc-totals`, `percentiles`, `rate`,
  `partition-ends`, `task-clocks`: the slice's, taken over and generalized.
- `overlap-monitor`, `progress!`, `emit!` (`META`, `PROGRESS` and `RESULT`
  to stdout; `META` and `RESULT` appended to the number's `.edn` as well).
- `verdict`: the rules of 7.2 and 7.3 over given values and thresholds.
- `summarize` and `report`: the median and spread across runs per variant
  and level, and the three `.txt` files.

`rig.bench.agent-rate`: `run` (A, A', D1, D2), `sessions` (B), `reads`
(C, with the entry bytes).

`rig.bench.one-thread`: `run` ((a) and (b)), `reads` ((c)).

`rig.bench.lock-growth`: `variants` (6.3's table), `value-spec` (the
slice's generator extended to a set of persons, 6.3), `run <variant>`,
`lock-rows-logical` (for any layer), `compacted` (6.4, item 3),
`pick-lock-rows` and `repack` (6.5), `check-values` (the end-of-variant
checks of 6.4), `report`.

### 9.3 The tests: `rig.bench.numbers-test`

Run under the lock as `clojure -M:test rig.bench.numbers-test`: one
cluster for the namespace at `{:tasks 4 :threads 4 :workers 1}`, and one at
`{:tasks 1 :threads 1 :workers 1}` for T7. **[F6]** Never both at once:
each is opened in its own `with-open`, and the four-task one is closed
before T7's opens. Both would bind port 2002 in one JVM, which `flock`
cannot order.

- **T1, the write list is the store's own.** In an agent layer with no
  grammar, 100 value acts from one writer through the door (so exactly
  two lease acts: 64 locks, then 64 more at the 65th act). Take
  `field-counts` before and after. Every field must grow by what `writes`
  claims, summed over the acts admitted by kind: `:answers`, `:log`,
  `:heads`, `:ix-ek`, `:ix-ke`, `:ix-s` and `:by-stamp` by 102; `:leases` by
  2 × 64 − 100; `:locks`, `:ix-kv` and `:ix-of` by 0; **[F2]** `:key-rows`
  by 1, the first `:note` act's row (0 on a store with no key rows);
  every other field by 0; `$$clock` moved on the home task only. A field the store has and the
  list does not name fails the test, and so does a named write that did
  not happen. This is the brief's "count of index writes per act checked
  against the code's own list": the list is the harness's claim, the
  growth is what the code did.
- **T2 and T3**, the same for the by-value grammar (`:ix-kv` and `:ix-of`
  by 100) and for a personal and a hand layer (`:locks` by 100).
  **[F2]** In T2 `:key-rows` grows by 0, the grammar fact having made the
  row before the snapshot, and the row is read back with `:used` true,
  since a count cannot see the first `:note` act setting it; in T3 each
  fresh layer's `:key-rows` grows by 1, as in T1.
- **T4, the read entry.** Twenty point reads through the exit in an agent
  layer, with 4.6's read spec (**[F3]**: the reader a person, citing a
  permission that person holds): `:locks` grows by 20 (the `:own-row`
  mark), the entries' id-index
  entries carry no value fields (`:no-copy`), and each entry act's growth
  matches the list's entry column.
- **T5, the counts the numbers are made of.** In a short window of four
  writers: the harness's value count equals the answers the writers
  received; its two lease counts agree with each other and with the
  growth of `:answers` less the value acts. On a known synthetic latency
  list, `rate` and `percentiles` give the known values (pure). **[F5]**
  With a synthetic send that stalls once for 50 ms, `open-loop-window`
  measures each slot scheduled during the stall from its slot (each
  latency at least its lateness), which a closed loop would not record.
- **T6, placement.** Every offer of a window was decided on its layer's
  home task (partition and clock deltas), and `layers-on-task!` returns
  only layers whose making grew the target partition. **[F7]** Checked by
  an observation the search did not make: one more act into each layer it
  returns grows only the target partition and moves only the target
  task's `$$clock`.
- **T7, the lock-store measure.** In a hand layer on one task, 2,000
  values of 40 bytes: the logical pass counts 2,000 rows and sizes each as
  its key's bytes plus its record's, matching a row read by keypath;
  `pick-lock-rows` on a compacted copy picks exactly 2,000 entries under
  one prefix, with raw bytes within 4 a row of the logical ones; `repack`
  gives a positive size no larger than the whole store's. **[F8]** And the
  re-packed store, scanned back, holds exactly the picked entries (their
  count and their key and value bytes), and its SST bytes are at least 60
  a row: each row holds a 60-byte sealed lock, random bytes no
  compression shrinks, so a re-pack below that lost entries. Then a second
  hand layer's values in the same store: the pick must now report two
  prefixes and a failed check, which shows the one-structure check is not
  empty.
- **T8, the wrap.** In a hand layer: an unmarked `:mention` of Bob makes a
  lock row whose `:required` is `[:alice]` and whose record is the size of
  an unmarked `:note`'s; a marked one makes `[:alice :bob]` and a blob 28
  bytes longer.
- **T9, the machine.** Every field of `machine` is present and not empty.
- **T10, the verdict rule**, on synthetic values: 10,000 against 1,000 is
  far above; 2,249 against 1,000 is near; bytes at 4.7 times are over four
  times.
- **T11, the values** (pure): number 2's generator gives, for `h40`,
  `h200` and `h40-p2`, the same first 100 values as the slice's
  `value-spec`, and every variant's values have exactly their intended
  canonical size.

The driver runs no measurement unless this namespace passes.

## 10. The final numbers against the slice numbers

### 10.1 What changed in the thing measured

The slices measured the stream store of 25 September; the final runs
measure the finished store. The differences the numbers should show, each
from 2.3 and 2.4:

| | Slices | Finished store |
|---|---|---|
| writes per small agent act | 4 | 9 (10 in a personal or hand layer), and a 72-write lease act per 64 value acts |
| reads per small act | 4 | 8 for the agent (7 for the person) |
| the value in the depot | plaintext | sealed at the door, 28 bytes more, and a lock id |
| crypto on the task thread | none | unlease, open, re-wrap, value digest |
| lock rows | none (stream); a slice module for number 2 | written by the store in personal and hand layers, one per value |
| stamps | milliseconds, 21 s ahead of the wall at 2,250 acts a second | the hybrid clock (default 2) |
| other topologies in the module | none | the micro store's, idle |

So a change is expected everywhere. What each comparison looks for is
whether a verdict moves, and why.

### 10.2 Number 1

Slice (runs/phase7-agent-rate.txt, runs 4 to 6): 299, 646 and 2,249
admitted acts a second at K = 1, 4 and 16; 1,196, 2,584 and 8,996 index
writes a second at 4 an act; p99 4.74, 9.79 and 9.94 ms; the home task
thread 20%, 24% and 37% of a core. `report` prints the final value beside
each and their ratio. Readings:

- **Value acts a second within about a quarter of the slice's at every K,
  and index writes a second up by about the ratio of write counts (about
  2.5 times).** The finished rules cost the task little at these loads;
  the writers in flight still set the rate; the verdict stays "near" at
  every K.
- **Value acts a second well under the slice's at high K, while the home
  thread stays under 60% of a core.** The door is the limit (one lease at
  a time, and a lease only when the pool is empty). D1 shows the gate's
  own rate. The lease size and when to lease are rig choices that change
  no record.
- **The home thread near a full core.** The gate's per-act work is the
  ceiling, and that rate is the task capacity the divisor needs. Variant
  B's largest S should agree with it divided by 100; if it does not, the
  report says which one to believe and why.
- **K = 1 p99 above the slice's 4.7 ms.** The lease tier: one offer in 64
  carries an acked lease act and a query. Number 3's (a) shows it apart.
- **The clock's lead over the wall**: the slices' 20 to 21 seconds at
  2,250 acts a second should be about zero. A check that default 2 did
  what it was for, not a number.

### 10.3 Number 3

Slice (runs/phase7-one-thread.txt, runs 4 to 6): one writer 299 acts a
second, p50 3.26, p99 4.72, max 20.9 ms; K = 1 to 128 from 300 to 5,192
acts a second, p99 from 4.71 to 43.94 ms, the thread never above 60%.
Readings:

- **The rate verdict** was far above (13 times at K = 8). It stays far as
  long as K = 8 still gives at least 1,000 value acts a second.
- **The latency verdict** was near (the best p99 4.7 ms, 4.2 times under
  20 ms). If the finished p99 at one writer is set by the lease tier, the
  margin falls to about two and a half times (*derived*: a leased offer
  waits for two acked round trips of about 3.3 ms and a query, about 8 ms
  in all). If it passes 20 ms at one writer, the
  assumed threshold fails at the smallest load, and the cause is the
  door's cadence, not the task: a door that leases ahead would remove it.
  (a)'s split of leased and unleased offers says which.

### 10.4 Number 2

Slice (runs/phase7-lock-growth.txt): raw bytes, one person: 169 B a value
logical, 86.9 B compacted; two persons (base64) 233 B and 148.0 B; each
extra person computed at 32 to 35 B raw; ratios at 40 B 4.23 times
logical and 2.17 on disk, at 200 B 0.85 and 0.43. The finished store keeps
raw bytes, so the slice's raw variants are the ones compared. Readings:

- **Within about 10% of 169 B** (about 180 B expected, 6.2): the slice's
  verdict stands. Fine at 200 bytes; over four times logically at 40
  bytes and between two and four times on disk. It still turns on how big
  hand-layer values are, which is Sid's.
- **The slope per extra subject near 28 bytes plus a keyword**: the wrap is
  what 6.2 says. Far from it, the report says where the bytes went.
- **The ratio to the rest far under the slice's 0.38 to 0.75**: the
  finished store keeps the sealed row four times (the log and three id
  indexes), so the lock store weighs less among what a value costs than
  the slice showed; a threshold read against stored bytes passes
  everywhere.
- **Per-act grain at four values an act**, if run: about a quarter of a
  row a value (*derived*: about 45 B, 1.1 times a 40-byte value). It turns
  the README's fallback, stated "by calculation rather than a run", into a
  run.
- **A curve that is not linear**: something per value is not constant
  (lease rows standing, say); the report names the step where it bends.

### 10.5 What a comparison cannot say

The slices and the final runs ran on one machine (if the `META` lines
agree) at different times. The slices' own runs agreed within 2% to 5%
(runs 1 to 3 against 4 to 6), so smaller differences here are noise, and
timing differences read as orders of magnitude only, by 7.2.

## 11. The plan template's sections, read for a measurement

The rama skill's plan template (`references/artifact-plan.md`) is written
for a module. This phase adds no depot, PState or topology, so each section
below says what the harness does to the store it measures, which should be
nothing but offers through the store's own road.

### Reads

| Read | How | When | What it costs the measured task |
|---|---|---|---|
| sample read-back | `foreign-select-one` by keypath, about eight paths for each of up to 200 acts | after each window | nothing inside a window; about 1,600 point reads between windows |
| placement | `foreign-depot-partition-info` per partition, `$$clock` per task | at each window's start and end | eight reads per window edge |
| field counts | `foreign-select-one [(keypath L f) (view count)]` per field | in the tests only (T1 to T4) | none in a measured run |
| the logical lock rows | `foreign-select`, pages of 10,000 | at each lock-growth point, writers paused | one seek and up to 10,000 iterations a page |
| `lease-locks` | by the door, once a lease | inside windows | part of the workload |
| `read-point` | by the exit's `read!` | inside C's and 5.5's windows | part of the workload |
| `$$persons` entries | `foreign-select-one` per person | at the end of each lock-growth variant | a handful |

Every harness read that is not the workload runs between windows or at a
point with the writers paused.

### Writes

Only through the store's road: `c/offer!` (the door, the `*offers` depot,
the gate) for the workloads, `c/offer-until-answered!` for setup, the
door's own lease acts, and the exit's entry acts. The harness appends to no
depot directly and writes no PState. The act shapes are 2.2 and 6.3.

### PState Design

No new PState. The ones counted, by name: `$$layers` (the fields of 2.3,
and `[L :locks]` for number 2), `$$clock`, and `$$persons` (read). Their
schemas are the merged module's, measured as they are (**to confirm at
build**).

### Depots

`*offers`, `(hash-by :layer)`, carries every workload offer. Placement by
layer is what puts all of a layer's offers on one task, and each window
checks it. `*index-ops` is not touched. No new depot.

### Topologies and PStates

The `gate` stream topology is what is measured. The micro store's
microbatch topology is present and idle, and its idle cost is recorded
(3.2). The query topologies `lease-locks` (the door) and `read-point` (the
exit) run as parts of the workloads, and `read-pattern` in C's optional
entry bytes. None is added.

### Query Topologies

None is added. The two the workloads call are the store's own:
`lease-locks`, one call a lease, reading a session's unconsumed lease rows
(one seek, and up to 64 rows plus any in flight: fixed per call); and
`read-point`, one fact id a read (the name's record and the row). They are
measured as built.

### Partitioning efficiency

For a module this table costs the dominant read. Here the dominant
operation is the value act's decision event, measured on one task by
design. From 2.4: every read of the event is local to the layer's home
task, since the event has no partitioner. Variant A's mix at steady state
is 64 value acts to 1 lease act. **[F10]** The lease act's event reads
six (the name's record, the settings, its writer's person entry, the
clock, two permission rows; seven if the merged gate reads a key row for a
store key), and its `lease-locks` query three (the settings, one range
read of the session's lease rows, up to 64 rows plus those in flight, and
the person entry): nine.

| N | Category | Proportion | Seeks/op, all tasks | Iterator reads/op |
|---|---|---|---|---|
| 1 | value act | 64/65 | 8 | 0 |
| 1 | lease act with its query | 1/65 | 9 | 64 |
| 16 | value act | 64/65 | 8 | 0 |
| 16 | lease act with its query | 1/65 | 9 | 64 |
| 128 | value act | 64/65 | 8 | 0 |
| 128 | lease act with its query | 1/65 | 9 | 64 |

Weighted seeks 8.02 and weighted iterator reads 0.98 at N = 1, 16 and 128:
flat, because placement by layer keeps every read of an act on one task
whatever N is. Some of these point reads will hit RocksDB's block cache
rather than the disk; the counts are of reads, not of disk seeks. Variant B
puts S layers on one task on purpose, to find one task's capacity; normal
placement spreads layers by hash.

### Design Decisions

The method choices, each argued in the difficulty log (14): one layer with
K writers as asked, and S sessions with their own doors for the divisor;
closed loops where the slices had them, an open loop at an assumed agent
speed for B; a key with no grammar as the primary act, a by-value grammar
beside it; the lock rows picked out of a compacted copy and re-packed; one
task for the bytes, four for the timings; 6,400 sequential acts for (a);
64 door threads for the lock store's writes; the slice's sizing reused.

### State primitive selection

- **Harness state**: in the JVM only (atoms, growable primitive `long`
  arrays of latencies), gone with the JVM. That is right: a run's result is
  its `RESULT` lines, written as it goes.
- **Results**: the `.edn` files, appended, durable, in git.
- **The store's state**: the merged module's own. The harness adds none.

### Resource usage analysis

- **Disk.** A run of number 1 or 3 grows its cluster directory by about 2 KB an
  admitted act (*derived*: the row and its three id-index copies, the
  other entries, the write-ahead log, the replication log and the depot),
  so about 1 GB for an agent-rate run of some 450,000 acts; the cluster
  deletes it when it closes. The lock store's layer reaches an estimated
  150 to 250 MB by 100,000 values, plus its log, plus a scratch copy at
  each point, removed after the point. The driver checks free space under
  the JVM's temp directory before each step and stops below 10 GB.
- **Memory.** `-Xmx16g`. Latency buffers are a few MB at most (A's largest
  window: about 150,000 offers). RocksDB memtables are native memory.
- **Threads.** Up to 512 writer threads (A's cap), 160 in B.
- **Time.** 8.6.

## 12. Binding points to confirm at build

Each is a name this plan uses that tonight's merges may move or rename.
The build confirms each against the merged code before writing the
harness, and T1 to T11 fail if any is wrong in a way that matters.

| # | Binding point | Where it is today | What the harness does with it |
|---|---|---|---|
| B1 | the store module and its launch | `rig.store.module/Store` (`703b8e26`; `fd41f6d2`) | `rtest/launch-module!` |
| B2 | `$$layers` fields of a one-owner layer, and `$$clock`, `$$persons` | module.clj `layers-schema`; locks.clj `layer-fields` :971, `persons-schema`; reads.clj `layer-fields` :125; `:ix-s` (reads-rest, planned); `:key-rows` (tools, planned), **[F2]** with the row a layer's first fact under a key that is not a store key writes | `field-counts`; the read-back; T1 to T3 |
| B3 | the decision event's writes | module.clj decision block (:135 to :168 on `703b8e26`); locks.clj `write-decision>` :1197, `consume-locks>` :1050; reads.clj `index-writes` :181 | `writes` (2.3); T1 to T4 |
| B4 | whether `:by-stamp` is still written | locks.clj `write-decision>`; BUILD_NOTES-locks-and-forgetting.md, "Seams and stubs" | row 12 of 2.3 |
| B5 | the door | client.clj (`fd41f6d2`): `offer!` :236, `build` :71, `default-session`, `lease-size` :27, `:lease-mutex`, the door atom's `:known` and `:pool`, `stock!` :322, `lease-locks` :296; **[F12]** `connect` :42, `offer-until-answered!` :275, **[F4]** `lease!` :305 | the workloads; lease counting; D1; `with-store` and B's doors; setup |
| B6 | setup acts | client.clj `make-layer-offer` :507, `make-person!` :348, `grant-offer` :517, the `:people` store layer | `people!`, `person!`, `layer!` |
| B7 | opening a session | `rig-build-micro` micro_client.clj `open-session!` :482, `person-permission` :473 | 4.2, step 3 |
| B8 | the permission walk | `rig-build-micro` permit.clj `chain` :29 | 2.4; D2 |
| B9 | grammar facts and hints | *plan*: tools-and-grammars 4.1 to 4.7 (`{:e <key id> :k :grammar :v {...}}`, `[L :key-rows k]`, `grammar/hints`); today `reads/seed-hints` :34, **[F1]** which hints `:note` by value in every layer (:41) until phase 6 replaces it | the primary act's key (4.2); A' and the `:mention` variants; whether keys stay keywords ("keys are ids") |
| B10 | the read exit | read_exit.clj `read!` :88 (its spec keys), `connect` :28, **[F3]** `entry-offer` :78 (the reader as `:who`, `:session nil`); reads.clj `entry-facts` :759 with `:own-row` (reads-rest, planned); `reads/address` :96 | C; 5.5; the read-back's addresses |
| B11 | the lock row and the wrap | locks.clj `lock-record-schema` :953, lock ids `[lease-name i]` (L21), `row-kinds` :364, `wrap-of` :253, `wrap` :290, `unwrap`, `open`, `canonical-bytes` | number 2's sizing, pick and checks; T7, T8 |
| B12 | Rama internals the sizing uses | `rpl.rama.util.nippy-serialization/freeze` and `thaw`; `rpl.rama.api.durable.rocksdb.key-encoding/k-ser` (in the Rama 1.6.0 jar; `freeze` and `k-ser` used by the slice); **[F12]** the RocksDB column families `default` (top-level entries) and `subindexed` (every subindexed element), and `OptionsUtil/loadLatestOptions` over the options Rama writes (the slice's finding, runs/phase7-lock-growth.txt "Method") | 6.4, 6.5 |
| B13 | query and depot names | `"*offers"`, `"lease-locks"`, `"read-point"`, `"read-pattern"` | `with-store` handles |
| B14 | **[F12]** the slice bench's helpers and what loading them needs | `rig.bench.lock-bench` (`value-spec`, `value-bytes`, `key-bytes`, `rocks-dirs`, `live-bytes`, `ipc-root`, `replog-bytes`), which requires `rig.bench.lock-slice` and uses `rig.store.envelope/canonical` (lock_bench.clj L30-38, L82-94) | number 2's sizing and generator; the copy fallback of 9.1 |

## 13. What this plan could not settle, and questions for Sid

### 13.1 Not settled here

1. **The merged store's exact per-act lists.** 2.3 and 2.4 are read off
   three branches and two plans, before any merge. T1 to T4 settle them at
   build; if the merge changed them, the harness's list follows the code
   and the test says so. **[F1]** The first merge (`d83e5ff8`) already
   gives 10 for the agent act, because of the constant hints (2.1).
2. **The on-disk pick of the lock rows** rests on Rama's key layout, which
   reading supports (6.5) but did not prove. T7 settles it at build, and
   the fallback is named.
3. **The agent's permission chain**: two deep through phase 3's session
   opening, if the merged store keeps that road (B7).
4. **Keys as ids** (phase 6): whether the bench's `:note` and `:mention`
   stay keyword keys (B9).
5. **Whether the door, not the task, limits variant A** is a result, not a
   method question; D1 is there for it.
6. **The time estimates** rest on an assumed door rate of 2,000 to 5,000
   acts a second (8.6).
7. **The agent speed** in B, 100 acts a second, is the README's
   assumption.

### 13.2 Questions for Sid

None of this plan's own picks touches a record: the benches write nothing
that is kept, and no record shape is made here. The questions are the
numbers' inputs, most carried unchanged from README.md "What only Sid can
settle".

- **Q1. The three thresholds.** Default 7's are the main session's
  guesses; the verdicts are only as good as them.
- **Q2. How big hand-layer values are.** Number 2's verdict turns on it,
  as it did for the slice.
- **Q3. How fast an agent writes, and how many run at once.** The README's
  two inputs that turn variant B's result into a threshold on N.
- **Q4 (first-record). Whether agent session reads are recorded by
  default.** RIG.md default 4 records them through the exit, which R5
  partly reopens against ruling 3's "agent session layers may default to
  none"; where an entry lives is first-record (default 3). Variant C
  prices the default; it does not decide it.
- **Q5 (first-record). Whether hand layers should default to per-act
  grain if hand values turn out small.** Ruling 7 gives every value its
  own lock row; `h40-act4` prices the alternative. A layer's grain is a
  fact in its making act, so the first hand layers' records carry the
  answer.

## 14. Design difficulty log

Written as the plan was cut, first person.

1. **What "index writes" counts.** I weighed three readings: every PState
   write of the decision event, sets and deletes, lease rows included; only
   the read indexes (`:ix-*`), the narrow sense of "index"; and RocksDB
   entries. The slices counted every PState write, the clock among them,
   and the README says so in words ("its answer record, its log row, its
   chain head, the task's clock"), so the first keeps the final number
   comparable; the second is printed beside it; the third cannot be
   counted without Rama's internals and appears only as the net entry
   counts the tests use. The first two were close in meaning; printing
   both settled it.
2. **Writers sharing one door, or sessions with doors of their own.** The
   brief says writers into "an agent session layer", which is variant A.
   But A's writers wait on one door that leases one thread at a time, so A
   may measure the door and not the task, while the question the number
   answers is how many sessions a task carries. I kept A as the number as
   asked and added B for the divisor. The close call was whether B belongs
   in the minimum set. I left it at the head of the full set because A
   answers Sid's words and B's agent speed is an assumption; if only the
   minimum runs, the divisor stays derived and the report says so.
3. **Closed loop or open.** Closed for A and number 3, the slices' shape.
   Open for B: a closed loop at an assumed agent speed hides a queue at the
   task, because a slow ack only slows the writer. Not close once B's
   purpose was clear.
4. **The primary act's key.** A key with no grammar is the realistic
   default, since session layers start with none; a by-value grammar makes
   the heaviest small act. The threshold's "every index written" could
   argue for the heavy one. I read it as every index the store keeps for
   that act, checked by the read-back, and put A' in the minimum set at
   three minutes, so the range 9 to 11 is on the page either way.
5. **The lock store on disk.** The slice's difference method has no
   counterpart in the finished store. I weighed picking the rows by value
   and re-packing them, RocksDB's approximate range sizes, and a
   proportional estimate. The pick is exact if it can be trusted, and the
   hard part was trusting it without Rama's source; two checks that can
   fail (one prefix; the exact count) and a test that makes the first fail
   on purpose (T7) settled it. The range size stays as a cross-check, the
   proportion as the named fallback.
6. **How many sequential offers for number 3's (a).** My first reason for
   6,400 was that 2,000 could not place the slowest 1 in 100 among leased
   offers. That was wrong: at one lease in 64 offers, p99 is a leased offer
   at any size. The reason that holds is describing the leased offers'
   own spread, which 31 of them cannot do and 100 can. Corrected in 5.3.
7. **Reusing the slice's helpers or copying them.** Reuse, because number
   2's sizing must be the slice's for the two to compare; copying stays
   the fallback if the merged store stops the slice's namespace loading.
   Close, since reuse ties the final harness to a file kept as a record.
8. **One task for number 2; the harness under `test/`.** Neither was
   contested once the harness declared no module and bytes proved not to
   depend on the task count.

## 15. Self-validation (`references/artifact-plan-validation.md`, read for a measurement plan)

- **Query topologies.** None added; the two the workloads call are the
  store's, measured as built. Not applicable, with that reason.
- **PState schemas.** None added. Not applicable.
- **Partitioning.** The harness writes nothing of its own; the workloads'
  placement is the store's placement by layer. The table in 11 is flat at
  N = 1, 16 and 128 (8.02 seeks after [F10], 0.98 iterator reads). Pass.
- **Topologies.** None added. Not applicable.
- **Production readiness, as it applies to a harness.** Concurrent
  clients: K writers share one door, whose pool locking is phase 2's own
  (fixed in `de86fd23` so a lease's locks enter the pool once); the
  harness's counters are per writer, merged after the window. A client
  that dies: a run is one JVM; a killed run leaves `RESULT` lines under its
  run id and no end line, and the summary uses complete runs only; `flock`
  lets the lock go when the JVM exits. A worker restart mid-run is not
  simulated: the numbers are not about failover, and a run that errs is
  marked. Pass.
- **Internal depots, stream correctness.** None added. Not applicable.
- **In-memory efficiency.** Latencies in growable primitive `long` arrays
  per writer (9.2), not boxed lists, so the harness's garbage does not show
  up as the store's latency. Pass after the fix made while validating.
- **Minimality, adversarially.** The simplest plan: rerun the slices'
  benches on the merged store. It fails the brief twice: the lock slice is
  not the finished store's real lock rows, and the stream bench neither
  counts leases nor names or reads back the finished store's writes, so it
  cannot say "every index written" or name the writes from the code. The
  three harnesses and the test are what remains. Beyond Sid's words: A'
  (kept in the minimum; its cost is three minutes and without it the
  number holds only for keys without grammar), D2 (kept in the minimum;
  18 s a run, and PLAN-micro-store.md asks phase 7 for exactly this), and
  B, C, the per-act grain, the entry bytes, the repeat and 5.5 (all in the
  full set, each naming what it decides). Deleting any of them loses no
  property SPEC.md requires. Pass, with each extra named and priced.
- **Throughput, adversarially: does the harness load the task it
  measures?** Read-backs after windows; placement reads at window edges;
  the overlap monitor is a client thread polling the process table every
  2 s; progress every 5 s; CPU snapshots at edges. The writers share the
  machine's 24 hardware threads with the cluster, which the caveat of 3.3
  covers. Pass.
- **Spec coverage.** Each clause, traced:
  - "index writes per second": 2.3 and 2.5 say what is counted and how it
    becomes a rate; 4.3 measures it; T1 ties the count to the code.
  - "an agent session layer": 4.2, a layer of kind `:agent` written
    under an opened session.
  - "writing small acts continuously": closed-loop writers with no pause,
    one fact of 40 bytes an act (4.3).
  - "lock store growth under hand layers": 6, a layer of kind `:hand`,
    the store's own lock rows.
  - "bytes per value": 6.4 and 6.6, logical and on disk after compaction.
  - "the curve over a hundred thousand values": ten points to 100,000.
  - "one person's layer on one thread": 5, a personal layer, every offer
    checked to be decided on the layer's one task.
  - "acts per second and latency": 5.3 and 5.4, typical, 1 in 100, worst.
  - "each with its method and the machine it ran on": 3.1, and the result
    files' headers (8.5).
  - SPEC.md, "Tests and evidence": `rig.bench.numbers-test` (9.3); each
    result says what ran and what it showed, and marks what was reasoned
    (8.5); every file carries the caveat (3.3).
  - The brief's parts: 1.3 maps each to its section; 40- and 200-byte
    values (6.3), each extra subject (6.3, 6.6), 1, 4, 16 writers and past
    (4.3), the thresholds (7), the lock and the background (8), the build
    and its test (9), the comparison (10).

  A run killed mid-way leaves partial lines that the summary ignores, and
  two sessions' clusters cannot overlap under `flock`: the failure and
  race checks as they apply to a harness.

- **Flaws found by this validation and the read-through after it, each
  fixed in place:** 5.3's reason for 6,400 sequential acts (the slowest 1
  in 100 is a leased offer at any sample size; the reason that holds is the
  leased offers' own spread); latencies in primitive buffers (9.2); the
  three- and five-person variants, whose persons do not fit in a 40-byte
  value and whose persons the slice's generator cannot name (6.3: 64-byte
  values, a generator of its own checked against the slice's by T11);
  `field-counts` counting nested fields at their leaves, which T1's lease
  arithmetic needs (9.2); variant B's missing warm-up (4.5); run lengths
  made to agree between 4.9, 5.6, 6.7 and 8.6; section 0 marking which
  variants belong to the full set. None found after those.

- **The plan validation** (`PLAN_VALIDATION-numbers.md`, a fresh session,
  26 September from 05:00 IST): **minor-fail**, twelve fixes applied in
  place and marked `[F1]` to `[F12]`: the primary act's key under the
  constant hints, and the first merge's trace (F1, 2.1, 4.2); the first-use
  key row in the write list and in T1 to T3 (F2); variant C's reader and
  permission, and T4 (F3); D1's stocking made linear, and D1 expected to
  run (F4); open-arrival latency for number 3, and closed-loop latencies
  labeled (F5); the test namespace's two clusters never open at once (F6);
  T6's search checked by an observation of its own (F7); T7's re-pack
  checked (F8); the pick's thaw handling, P defined, the fallback's bounds
  (F9); the lease act's reads in section 11 (F10); a shortest set (F11);
  the binding points B2, B5, B9, B10, B12 and B14 (F12).
