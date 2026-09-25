# Plan — phase 7, the three numbers, on the finished store

*Cut by Opus 5.5 at max effort, 26 September 2026 (IST, before dawn),
following the rama skill's plan step (`references/phase-1-plan.md`),
adapted to a measurement plan: this phase builds harnesses and runs them,
not a module. Worktree `Softland-rig-plan-numbers`, branch
`rig-plan-numbers`, made off `rig-2026-09-25` at `703b8e26`.*

**Status.** A plan. Nothing is built and nothing was run. One method
question (can a harness tell a PState's lock rows apart from its other
entries inside RocksDB?) was taken as far as reading takes it, from the
Rama 1.6.0 jar's class listing (section 6.4); no cluster was started and
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
  the task's clock. The slices' act made 4. Every 64 value acts also cost
  one lease act of 72 writes. A test checks the list against the merged
  store's own growth before any number is run (section 2, test T1).
- **Number 1, the agent rate.** One agent session layer on one task,
  written under a session permission, K closed-loop writers sharing one
  door: K = 1, 4, 16, then 32, 64, 128 and on while the rate still rises.
  Value acts a second and index writes a second. Beside it, the same act
  under a by-value grammar (11 writes), and S sessions on one task, each
  with its own door, each writing at an assumed agent speed of 100 acts a
  second: how many agent sessions one task carries at that speed, the
  divisor the README's yardstick asks for. Third, the agent reading before
  it writes, each read recorded through the exit.
- **Number 2, lock store growth under hand layers.** 100,000 values into a
  hand layer through the door, for 40-byte and 200-byte values, a point
  every 10,000. The finished store's real lock rows read back and sized in
  Rama's own serializers (logical bytes), and on disk after a forced
  compaction with the lock rows isolated from everything else the layer
  keeps. Each extra subject from values marked to die with one, two and
  four more people. Then one hand layer at per-act grain, four values an
  act: the README's fallback, computed on 25 September, run.
- **Number 3, one person's layer on one thread.** A personal layer: one
  sequential writer (6,400 measured acts, so the lease cadence is well
  sampled), then K = 1 to 128 closed-loop writers. Acts a second; latency
  typical (p50), slowest 1 in 100 (p99) and worst (max), with the offers
  that paid for a lease shown apart.
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
  minimum set is about 50 minutes of machine time, the full set about 75.
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
into a threshold." Variant B of number 1 (section 4.4) measures the task's
side of that division directly, at an agent speed that stays an
assumption.

### 1.6 What the stage plans left for phase 7

| Plan | What it leaves | Taken in |
|---|---|---|
| PLAN-locks-and-forgetting.md, "What later stages consume" (lines 1898-1902) | M2: iterate `[layer :locks]` for count and serialized bytes at each point, raw bytes; lease rows not counted, they are consumed at decision. M1: agent layers write no lock rows; their per-act writes are the row with the lock in it, `:by-stamp`, one lease-row delete per value fact, and a lease act per batch | 2.3, 6 |
| PLAN-reads-rest.md, "The mark at write" (1153-1185) | every read entry is marked `:own-row`, so agent layers now write one lock row per read-entry value, and a hand layer's `:locks` count includes entries | 2.3; 4.5; 6.1 (number 2's workload makes no entries, so its count is values alone) |
| PLAN-reads-rest.md, "The delta" (816-895) and WS11 | a fifth index, `:ix-s`, written in the admitting event for every fact | 2.3 |
| PLAN-read-exit.md, "Disk usage" (1179) | an entry line with an exact list is "still the number to watch" at a model's call rate | 4.5, the entry bytes (optional) |
| PLAN-read-exit.md, "Minimization" (1534) | the exit's two round trips are "the first thing to measure if phase 7 finds the exit slow" | 4.5, read latency |
| PLAN-micro-store.md, permission walk (598) | "phase 7's numbers say whether the 1.5 seeks show" | 4.6, diagnostic D2 |
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
carries one, section 4.5), deletes of `:heads` (one per replaced fact),
`:settings` (setting facts), `:permissions` (grants and revokes),
`:key-rows` (phase 6 writes a row only when a grammar lands or the first
fact under a key is admitted in a layer: *plan*, tools-and-grammars 4.7),
`:erased` and lock-row deletes (forgets), `$$persons` (person acts and
their fan-out to every task).

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
