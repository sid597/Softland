# Build notes — phase 7, the three numbers, on the finished store

*Builder: Claude Opus 5.5, reporting to builder C. Worktree
`Softland-rig-build-numbers`, branch `rig-build-numbers`, made off
`rig-2026-09-25` at `c9684356` (26 September, 07:40 IST). Built by
`PLAN-numbers.md` as validated (`PLAN_VALIDATION-numbers.md`, F1 to F12
applied in place).*

**Status.** Prep. The binding check below is read from wave 2's three
branches, before their merge; no cluster has been started on this branch.
The harnesses are built in two steps: now, the parts that do not depend on
wave 2's entry points; after wave 2 lands here, the rest, each tested with
one short run. The minimum set runs only when builder C says the store is
finished.

Marks as in the plan: *code* (branch, file and line), *plan*, *derived*,
*assumed*. Paths are relative to the rig folder.

## Binding check (26 September, 07:15 to 07:50 IST)

**What was read, and how.** Wave 2's three branches, read-only, with `git
show <branch>:<path>` from this worktree and the Read tool in their own
worktrees, each diffed against `c9684356` (this branch's base, wave 1 and
the review merged):

- `rig-build-promotion` at `5303418f` (phase 4),
- `rig-build-reads-rest` at `33817364` (the rest of phase 5),
- `rig-build-tools` at `2d2f666a` (phase 6, stream side).

Their merge, `rig-wave2`, had no commit of its own at the time of reading.
So each row says what the branch that touches the point has; where two
branches touch the same point, what the merge must reconcile is named, and
the harness confirms it on the merged tree (tests T1 to T4 count the store's
own growth, so a merge that changed a count fails before any number runs).

| # | Binding point | Plan | Wave 2's code | What the harness does |
|---|---|---|---|---|
| B1 | the store module | `rig.store.module/Store` | unchanged name on all three (`defmodule Store`: promotion module.clj L107, reads-rest L106, tools L106); `*offers` still `(hash-by :layer)` | `rtest/launch-module!` of `m/Store` |
| B2 | `$$layers` fields, `$$clock`, `$$persons` | stage 1, 2 and 5a fields; `:ix-s` and `:key-rows` planned | **built**: `:ix-s` (reads-rest reads.clj `layer-fields` L136-148), `:key-rows` (tools grammar.clj `layer-fields` L135, row `{:used :grammar}` L119), and one field the plan did not name, `:forwards` (promotion promote.clj `layer-fields` L233, written only in a crossing's yes). `$$clock` and `$$persons` unchanged | `field-counts` enumerates the fields from the store's own field functions, so `:forwards` is counted and must grow by 0 |
| B3 | the decision event's writes | module.clj's decision block | **moved** into `rig.store.gate-event` (`write-decided>`, promotion and tools gate_event.clj L34; `record-or-decide>` L93/L97), verbatim, plus tools' key-row block (L84); `locks/write-decision>` unchanged; `reads/fact-writes` writes `:ix-s` for every fact (reads-rest reads.clj L165-184) | the write list's sites name `gate_event.clj`; counts unchanged (below) |
| B4 | `:by-stamp` still written | whether the merge keeps it | yes: `lock-effects` sets `:by-stamp [stamp name]` for every yes, unchanged on all three (promotion locks.clj L887) | row 12 of 2.3 stays |
| B5 | the door | client.clj `offer!`, `build`, `default-session`, `lease-size`, `:lease-mutex`, `:known`, `:pool`, `stock!`, `lease-locks`, `connect`, `offer-until-answered!`, `lease!` | unchanged; tools adds `lookup-many` (client.clj L268) and nothing else. Promotion: `lease-locks`' answer gains `:landings` (locks.clj `leased-locks`), which the door does not read | as planned |
| B6 | setup acts | `make-layer-offer`, `make-person!`, `grant-offer`, `:people` | unchanged | as planned |
| B7 | opening a session | `micro_client.clj` `open-session!` | unchanged, `rig.store.micro-client/open-session!` (L513 on `c9684356`): for a layer placed by layer it sends `c/grant-offer` of `[S L L [p L L]]` through `c/offer-until-answered!`; it needs `mc/connect`'s handles (its `settings-of` reads `$$micro`) | setup opens `:bench-s1` with `mc/open-session!` on an `mc/connect` handle |
| B8 | the permission walk | permit.clj `chain` | unchanged; `[:bench-s1 L L [:ada L L]]` walks 2 rows, `[:ada L L]` 1 (permit.clj L29-38, L44-78) | D2 as planned |
| B9 | grammar facts and hints | a fact per key; `seed-hints` hints `:note` by value until phase 6 | **built by tools**: `{:e <key> :k :grammar :v {:shape s :subjects-at p :opaque b :index #{h}}}` with exactly those four parts (grammar.clj L57, `refusal` L86); `:grammar` is a control key, so the door leaves it plaintext (envelope.clj L57); the operator or the layer's owner may write it (gate.clj L133); the stream gate takes hints from the layer's key rows only, `(reads/hints-of key-rows)` (gate.clj L410; reads.clj `hints-of` L54), and `seed-hints`' `:by-value #{:note}` is no longer read there. Keys stay keywords (`:e :note`). The first fact under a key that is not a store key writes that key's row (`key-row-writes`, grammar.clj L187; store keys: control keys, `:grammar` and the read keys, gate.clj `store-key?` L60) | **F1's first case**: the primary act's key is `:note`, 9 writes, provided the merged `decide*` keeps tools' `hints-of` (reads-rest's `decide*` still passes `(reads/current-hints)`, which the merge must replace). A' writes the `:note` grammar `{:shape [:any] :subjects-at nil :opaque false :index #{:by-value}}` (tools' toy grammar, test/rig/store/toy_grammars.clj) |
| B10 | the read exit | `read!`, `connect`, `entry-offer` (reader as `:who`, `:session nil`), `entry-facts` with `:own-row`, `reads/address` | reads-rest: **the entry now carries the reader's session** (read_exit.clj `entry-offer` L91-103, FRR10, first-record; `check-call` accepts `:session`, L64-76); every entry fact is marked `#{:own-row}` (reads.clj `entry-facts` L986-1014); `(reads/address :ix-s {:stamp s :fid f})` (L104-116); `read-exit/connect` also opens reads-rest's six new queries and two depots (L29-47) | **F3's second case**: variant C and T4 read as `:reader :ada`, `:reader-kind :model`, `:for :ada`, `:session :bench-s1`, citing the session's permission `[:bench-s1 L L [:ada L L]]`. Entries and value acts then lease from one pool, `[L :bench-s1]`: two locks an iteration, one lease per 32 iterations, as planned |
| B11 | the lock row and the wrap | locks.clj `lock-record-schema`, lock ids, `row-kinds`, `wrap-of`, `wrap`, `unwrap`, `open`, `canonical-bytes` | unchanged; promotion's `unlease` answers nil for a landing lease's row only, and its lease rows gain two fields, `:public` and `:for`, nil for every lease here (promotion locks.clj L1063) | as planned |
| B12 | Rama internals for sizing | `freeze`, `thaw`, `k-ser`; the families `default` and `subindexed`; `OptionsUtil/loadLatestOptions` | **confirmed** for the functions by a JVM with no cluster (07:48): `rpl.rama.util.nippy-serialization/freeze [bb v]`, `thaw [bb]`, `rpl.rama.api.durable.rocksdb.key-encoding/k-ser [bb v]`. The families and the options load are the slice's finding; T7 checks them on the merged store | `pick-lock-rows` thaws each value from a `ByteBuffer` |
| B13 | query and depot names | `"*offers"`, `"lease-locks"`, `"read-point"`, `"read-pattern"` | unchanged; wave 2 adds queries (`read-delta`, `standing-close`, `standing-open`, `entry-ids`, `micro-index-progress`, `task-layers`, `promotion-status`) | as planned |
| B14 | the slice bench's helpers | `rig.bench.lock-bench`, which requires `rig.bench.lock-slice` and `rig.store.envelope/canonical` | loads: lock_slice.clj requires only Rama and `rig.store.envelope` (L33-35), and `env/canonical` is unchanged on all three | reused as planned; the copy fallback is not needed |

**The per-act writes on wave 2's code** (*derived* from the three branches
read together; T1 to T4 count them on the merged store):

| Act | Writes | Plan |
|---|---|---|
| agent value act, key with no grammar | 9: answer, rows, head, `:ix-ek`, `:ix-ke`, `:ix-s`, the cited lease row consumed, `:by-stamp`, clock | 9 |
| the same under a by-value grammar | 11: the 9 and `:ix-kv`, `:ix-of` | 11 |
| personal or hand value act | 10: the 9 and the lock row | 10 |
| lease act of 64 | 72: answer, rows, head, three id indexes, 64 lease rows, `:by-stamp`, clock (`:lease` is a store key: no key row) | 72 |
| read entry act (`:read/point`, `:own-row`, no copy) | 10: answer, rows, head, three id indexes, the lock row, the consumed lease row, `:by-stamp`, clock (`:read/point` is a store key: no key row) | 10 |
| first act under a key that is not a store key, per layer | one more, the `:key-rows` row (in setup or warm-up, never in a window) | F2 |

Promotion adds no write to these acts: `write-decided>` is the same body as
tools' without the key-row block, and `promote-flow/continue>` (promote_flow.clj
L138-160) emits once and does nothing for an act that is not a promotion
request. Its reads per act are 2.4's (the key row read is tools' `:keys`,
gate.clj L341).

**What changes in the harness from the plan's text:**

1. Code sites: the decision's writes are in `rig.store.gate-event`, not
   module.clj; the write list names them there (B3).
2. The primary act's key is `:note` (F1's first case), to be confirmed on the
   merged tree by reading its `decide*` and by T1 (B9). If the merge left
   `current-hints` in the stream gate, the primary key becomes `:memo` and
   A' keeps `:note`, as F1 says.
3. Variant C and T4 cite the session's permission, with the session named in
   the read spec (F3's second case, B10).
4. Grammar facts carry exactly `:shape`, `:subjects-at`, `:opaque` and
   `:index`. Number 2's `:mention` grammar must admit four persons for
   `h40-p5`, so the harness writes its own: `{:shape [:map {:persons [:set-of
   [:keyword] 1 8]} {:open? true}] :subjects-at [:persons] :opaque false
   :index #{}}` (tools' toy `:mention` admits one or two, toy_grammars.clj
   L18-25). With the grammar a fact of the layer, the plan's fallback (the
   persons carried in `:subjects`) is not needed.
5. `field-counts` counts `:forwards` too, which must grow by 0 (B2).
6. Nothing else: the door, setup, session opening, permission walk, lock
   row, wrap and depot and query names hold as the plan has them.

**Still to confirm on the merged tree**, because it is the merge's choice,
not any one branch's: that `decide*` takes tools' `(reads/hints-of
key-rows)` and reads-rest's `:ix-s` together; that `write-decided>` keeps the
key-row block; and that step R and step 6b, which follow the merge, leave
the counts as they are (step R refuses control-key facts the gate does not
act on and changes pattern entries, neither of which these workloads make;
6b is the micro gate's). T1 to T4 are the check.

**Confirmed on the landed tree** (08:05 IST, read with `git show
rig-2026-09-25:<path>` at `42619066`, wave 2 at `27543fd7`; not merged
here, see "The merge" below): the stream gate's `decide*` takes
`(reads/hints-of key-rows)` (gate.clj L440), so the primary key is `:note`
(F1's first case); `write-decided>` keeps the key-row block
(gate_event.clj L84-85) and writes the clock at L93; `fact-writes` writes
`:ix-s` for every fact (reads.clj L259-264); the read entry carries its
session (read_exit.clj L102) and is marked `#{:own-row}` (reads.clj
L1089); `lock-effects` still sets `:by-stamp` (locks.clj L897); the lease
row gains `:public` and `:for` (L1073); `module.clj` merges promotion's and
grammar's fields (L92-98); the door, `open-session!` (micro_client.clj
L513), `permit/chain` and `:grammar` as a control key (envelope.clj L57)
are as the branches had them. The bench files the harness reuses
(`test/rig/bench`, `src/rig/bench`, deps.edn) are unchanged. So the
branch-by-branch check above holds on the landed tree, and the harness's
write lists are the plan's: 9, 11, 10, 72, 10 and the first-use key row.

## The build (26 September, 07:55 to 08:30 IST)

**What was built**, all new files under the rig folder, nothing that exists
changed (9.1):

| File | Namespace | What it is |
|---|---|---|
| `test/rig/bench/numbers.clj` | `rig.bench.numbers` | the shared harness: META, one cluster a run with the busy-port retry, the overlap monitor, setup through the store's own acts, 2.3's write lists as data with their landed sites, the store's field counts, closed and open windows, the counts, the read-back, the verdict rules, the report |
| `test/rig/bench/agent_rate.clj` | `rig.bench.agent-rate` | number 1: `run` (A, A', D2, D1), `sessions` (B), `reads` (C and the entry bytes) |
| `test/rig/bench/one_thread.clj` | `rig.bench.one-thread` | number 3: `run` ((a), (b), (b')), `reads` ((c)) |
| `test/rig/bench/lock_growth.clj` | `rig.bench.lock-growth` | number 2: `run <variant>` for the seven variants of 6.3 |
| `test/rig/bench/numbers_test.clj` | `rig.bench.numbers-test` | T1 to T11 |
| `test/rig/bench/phase7-final.sh` | | the driver of 8.2 |

**What ran: nothing on a cluster.** Sid's rule of 26 September (relayed by
builder C: optimize for the system's throughput, run tests only when they
are needed) dropped the one small run each harness was to get. The only
things run were JVMs with no cluster: the serializer signatures (B12), and
two loads of the five namespaces, the second clean. The first load found
that Clojure does not import `java.lang.ProcessHandle` by itself, and
running the META's commands by hand found that `df` refuses `-T` with
`--output`; both fixed. So the harness is written and compiled, not run:
the minimum set is its first run. Its first step is `test`, which loads
every harness (the test namespace requires `rig.bench.one-thread` for that
alone) and runs T1 to T11 on the finished store; the driver measures
nothing unless it passes.

**The run order** (Sid's rule: a broken harness must fail near the start
of the set, not late in it): `test`; then the first run of each harness,
the shortest first; then the repeats. `min`: test, lock-growth-h40-p3
(about a minute: the lock pipeline with a grammar and three persons),
agent-rate-1, one-thread-1, lock-growth h40, h200, h40-p2, h40-p5,
agent-rate-2, one-thread-2, agent-rate-3, one-thread-3, report. `short` and
`full` are ordered the same way. The plan put the timing runs first "on the
quietest machine the night will give"; my own steps never overlap, so the
order does not change the noise, and failing early is worth more. From the
merged tree's rig folder:

    nohup test/rig/bench/phase7-final.sh min > runs/phase7-final-driver.log 2>&1 &

Progress: `tail -f runs/phase7-final-progress.log runs/phase7-final-*.log`.

**Where the build departs from the plan, and why:**

1. **Number 2 scans the whole `subindexed` family at every point** (6.5
   says the points between the first and the last seek to the first
   point's P). P is the longest common prefix of the picked keys, so it can
   reach into the lock ids' own bytes: their lease names are
   `[layer class :offer uuid7]`, and a uuid7 begins with the millisecond,
   so rows written a minute later need not start with the first point's P
   and a seek would miss them (*derived* from the envelope's `make-name`
   and `uuid7`). The full scan costs seconds a point and runs both checks,
   one structure and the exact count, at every point.
2. **Variant C and T4 cite the session's permission**, the session named in
   the read spec: the landed exit carries it into the entry (F3's second
   case). Entries and value acts share one pool, two locks an iteration.
3. **T4 writes the facts it reads in the owner's default session.** A door
   takes a session's unconsumed locks from any earlier lease (the door's
   `refresh!` takes every lease it has not seen), so facts written in the
   read session would leave the reader's fresh door no lease to make, and
   the entry's lease path would go untested.
4. **The harness writes its own `:mention` grammar**, one to eight persons;
   tools' toy grammar admits two, and `h40-p5` names four (binding check,
   change 4).
5. **The home task's thread** is the busiest thread that is not the
   harness's own (it names its threads `phase7-*`), as the slices found the
   home task's thread the busiest in every window; its name is in every
   result, so a reader can check it.
6. **The overlap monitor also records the CPU** each other JVM used over
   its sightings, so an idle JVM can be told from a busy one; a window is
   still marked `:overlap` whenever one was seen (8.4).
7. **B's and (b')'s index writes a second** take the doors' lease count
   over the whole window (warm-up included) and pro-rate it to the measured
   part by the admitted value acts; marked `:derived` in the result.
8. **T7's "two prefixes"** is shown as how many separate stretches of key
   order the picked rows make (`:runs`), printed, not asserted, since it
   depends on Rama's key layout; what is asserted is the failed
   one-structure check and the named estimate.
9. B's target task is task 0.

Each number's result file and each RESULT line carry the primary key, the
latency kind (`:closed-loop-service-time` or `:from-schedule`), the caveat
and the thresholds line, as 8.5 asks.

**First-record placeholders**: none. The benches keep no record; every
shape they write is the store's own (acts through the door, reads through
the exit).

**Nothing was deleted.** The harness removes, at run time, only the scratch
copies it makes of a RocksDB directory under the JVM's temp dir, as the
slice did.

## The merge

Builder C asked twice for `rig-2026-09-25` to be merged into this branch.
The first `git merge --no-edit rig-2026-09-25` was refused by the session's
permission check (the auto mode classifier, with no reason given), and it
has not been retried by any other road. The merge is needed before the set
runs: this branch sits on `c9684356`, before wave 2, and the harness is
written for the landed store (`:ix-s`, key rows, the session in the read
entry). This branch adds new files only (the six above and these notes)
and changes no file `rig-2026-09-25` has, so the merge should be
conflict-free; the harness was checked against the landed tree by reading
it (above), and it compiles against this branch's tree, whose namespaces
and signatures the harness uses are unchanged by wave 2.

## The run: the minimum set on the finished store (26 September, 09:24 to 10:11 IST)

*Run on branch `rig-run-numbers` (worktree `Softland-rig-run-numbers`),
made by builder C at `dee0320a` (step 6b landed; the harness landed before
it at `ad49bced`), plus `cc011a22`, the harness's two fixes below. Results:
`runs/phase7-final-{agent-rate,one-thread,lock-growth}.{edn,txt}`. The
machine, recorded by the harness at every run: AMD Ryzen 9 9900X (12 cores,
24 hardware threads), 62 GiB RAM, Ubuntu 24.04.4, kernel 7.0.0-31, OpenJDK
21.0.12.1, Rama 1.6.0, the cluster's files on ext4 on the WD_BLACK SN850X,
the slices' machine. Every timing is the median of three runs, spread in
the result files; the in-process cluster gives orders of magnitude only,
and every verdict below is against RIG.md default 7's thresholds, which are
assumed and not Sid's.*

**The bindings on 6b held** (read, 09:15): 6b changed the micro gate and
shared reads only (`gate.clj`, `grammar.clj` and `reads.clj` in docstrings);
step R's door check (`refuse-misplaced!`) passes every setting fact the
harness sends, each about its own layer; R-2 changes pattern entries only,
which the minimum set never makes.

**The first attempt** (09:24:36) measured nothing: its `test` step failed
in 44 s and the driver stopped, as it is built to. T1 to T6 and T8 passed
on it: every field of the finished store grew by exactly what the write
list claims (T1: `:answers`, `:heads`, the three id indexes and `:by-stamp`
by 102, `:leases` by 28, `:key-rows` by 1, every other field by 0, the home
task's clock alone moved). Two harness faults, fixed in `cc011a22`: Rama's
`thaw` accepts the references to nested subindexed structures and returns
a `ReferenceID`, whose lookup of an undeclared key throws, which ended T7's
pick (F9 had expected `thaw` to refuse them); and on this kernel Java's
`FileInputStream.available` throws on /proc files, so the machine record
had no CPU or RAM (T9). **The second attempt** (09:27:47) ran every step to
exit 0: `test` 33 s, then 12 measured steps, then the report; 43 minutes.
Every window's checks held (sample read-back complete, every offer decided
on its layer's one task, the two lease counts equal, no error), except that
one run in three at K = 1 and K = 4 of number 1 saw another session's JVM
(26 to 28 s of CPU in the window); the tables use the clean runs there.

### Number 1: index writes a second, an agent session layer writing small acts

One agent session layer on one task, K writers sharing one door, the act
of 2.2 (9 writes; with its share of lease acts 10.1, which the run
confirms: index writes a second over value acts a second is 10.1 at every
K). Latencies here are closed-loop service times, reported, not judged
(F5).

| K | value acts a second | index writes a second | read-index writes a second | p50 / p99 ms | home task's thread | against 1,000 (assumed) |
|---|---|---|---|---|---|---|
| 1 | 227 | 2,305 | 693 | 3.96 / 16.2 | 26% | near, 4.4 times under |
| 4 | 413 | 4,179 | 1,257 | 8.95 / 25.8 | 34% | near, 2.4 times under |
| 16 | 907 | 9,186 | 2,764 | 16.0 / 43.2 | 48% | near, 1.1 times under |
| 32 | 1,103 | 11,166 | 3,360 | 29.2 / 59.7 | 56% | near, 1.1 times over |
| 64 | 1,222 | 12,378 | 3,725 | 49.8 / 91.6 | 61% | near, 1.2 times over |
| 128 | 1,186 | 12,010 | 3,614 | 98.9 / 249 | 58% | near, 1.2 times over |

**Verdict** (assumed threshold): within ten times of 1,000 admitted acts a
second at every K, with every index written (the read-back complete at
every K), so the in-process cluster cannot decide it; one task carries
about 1,200 small agent acts a second here, 12,400 index writes, and the
home task's thread never passed 61% of a core. Against the slices (0.76,
0.64 and 0.40 of their 299, 646 and 2,249 acts a second at K = 1, 4 and
16): the finished rules cost the rate, most at K = 16, and the thread works
harder per act (48% at K = 16 where the slice's was 37%). A' (a by-value
grammar, 11 writes): 210, 321 and 803 acts a second at K = 1, 4 and 16.
D2 (the owner's root permission, a chain of one): 883 at K = 16 beside A's
907, so the second permission read does not show. Variant B, the divisor
at an assumed agent speed, is in the full set and did not run, so the
number of agent sessions a task carries stays derived (about 1,200 / 100 =
12 at 100 acts a second each, *derived*, not measured).

### Number 3: one person's layer on one thread, acts a second and latency

A personal layer on one task of four, each task on its own thread; every
offer of every window was decided on the layer's one task.

- **(a) One writer**, 6,400 measured acts: 226 acts a second (225 to 227);
  closed-loop latency p50 3.95 ms, p99 15.7, max 45.3. The offers that paid
  for a lease (one in 64, 100 a run): p50 12.8, p99 29.6. The others: p50
  3.94, p99 15.3. The slice: 299 acts a second, p50 3.26, p99 4.72.
- **(b) K writers**: 228, 257, 420, 626, 842, 1,062, 1,183 and 1,198 acts a
  second at K = 1, 2, 4, 8, 16, 32, 64 and 128; closed-loop p99 15.5 ms at
  K = 1 rising to 236 ms at K = 128; the thread at most 62% of a core. The
  slice reached 5,192 at K = 128.
- **(b') Open arrival**, latency from the schedule: at 100 acts a second
  every act admitted, p50 4.19 ms, **p99 46.1 ms** (the three runs 22.7,
  46.1 and 67.0); at 1,000 a second every act admitted, p50 37.1, p99 254
  (114 to 270).

**Verdict** (assumed thresholds): the rate, at least 100 acts a second, is
far on the passing side from K = 32 (10.6 to 12 times) and near below it
(2.3 times at one writer). The latency, 20 ms or less for the slowest 1 in
100, judged on the open arrival at 100 a second (F5), is **near on the
failing side: 2.3 times over**, and every one of the three runs was over
20 ms; the in-process cluster cannot decide it, and on this machine one
person writing at 100 acts a second sees a p99 near 46 ms. The closed-loop
p99 at one writer, 15.7 ms, is a service time and understates it.

**H-1 qualifies this verdict** (REVIEW-full-spec.md, H-1): the way out the
README gives for a person's layer that one thread cannot carry is re-class
to the micro gate, and until H-1's fix lands a re-classed layer that is also
someone's working layer refuses every read entry into it, so its owner
reads nothing through the one exit. The workloads here never re-class, so
the numbers stand; the way out they point to does not, yet, for a working
layer.

### Number 2: lock store growth under hand layers

A hand layer on one task, 100,000 values through the door, a point every
10,000; the store's own lock rows read back (logical, Rama's serializers)
and, after a forced compaction, picked out by value and re-packed alone.
The pick held at every point of every variant (one structure, the exact
count, the bytes within 4 a row, the re-pack read back exactly), and every
end check passed (100 sampled values open through their lock rows and stay
shut without each named person).

| Variant | Bytes a value, logical | On disk, compacted | Over the plaintext, logical / on disk | Verdict (assumed: 2 times fine, over 4 change the default) |
|---|---|---|---|---|
| **40-byte values** (`h40`) | 181.0 | 81.0 | 4.52 / 2.02 | over four times logically; between two and four on disk |
| **200-byte values** (`h200`) | 181.0 | 81.0 | 0.90 / 0.41 | fine both ways |
| 40 B, two persons (`h40-p2`) | 213.0 | 111.1 | 5.32 / 2.78 | |
| 64 B, three persons (`h40-p3`) | 248.0 | 143.4 | 3.87 / 2.24 | |
| 64 B, five persons (`h40-p5`) | 318.0 | 204.2 | 4.97 / 3.19 | |

The curve is a straight line: 181.0 bytes a value logical at every point
and 81.0 to 81.1 on disk, from 10,000 to 100,000 values, for both sizes.
The lock row does not depend on the value's size: a 52-byte key (the lock
id) and a 129-byte record, the sealed lock 60 of it. Each extra person in
the wrap adds 34.4 bytes logical (least squares; one 28-byte seal and a
keyword in `:required`, as the code says), about 30 on disk. Against what
the store already keeps for each value (the row and three id-index copies,
925 bytes on disk at 40 B, 1,622 at 200 B), the lock store is 9% and 5%.
The slice's raw variant: 169 bytes logical and 86.9 on disk; the finished
row is 7% bigger logically (the scheme tag, a longer lock id) and 7%
smaller on disk. So the verdict turns, as the slice's did, on how big
hand-layer values are, which is Sid's (Q2): at 40 bytes the lock store is
over the assumed four times logically and just over two on disk; at 200
bytes it is fine.

### What the run showed that the plan did not expect

1. **D1 measured the door, not the gate.** In all three runs A's rate
   flattened from K = 64 to 128 with the home thread under 60%, so D1 ran:
   the pool stocked with about 43,000 locks (165 to 171 lease acts of 256,
   2 s), then K = 128 admitted 1.1 acts a second, every act yes, no lease in
   the window, while the home task's thread idled at 1.5%. The door's
   `take!` (client.clj) sorts its whole pool by the lock ids' text on every
   offer, under the door's lock, so with a pool that large each offer waits
   about a second on the sort (*derived* from the code and the rate; the
   sort was not timed). The gate's own ceiling is therefore not measured:
   at every K the home thread stayed at or under 62% of a core, so what
   limits A and (b) at high K is on the client's side of the depot, the
   door's one-lease-at-a-time and its locking (*derived*). A door that
   takes a lock without sorting the pool, then D1 again, would measure the
   gate; both are Sid's or builder C's to authorize.
2. **The one-writer tail is not the lease tier alone.** The unleased
   offers' p99 is 15.3 ms where the slice's p99 was 4.72; one offer in 100
   waits about 15 ms with no lease in it. The cause was not measured (D3's
   strace road did not run); the disk flushes of the replication log and
   RocksDB are a hypothesis, not a finding.
3. **The finished store is slower per act than the slices** at every K
   (0.4 to 0.76 of their rate) with more work per act on the task thread,
   as the plan's section 10 allowed.
4. **The hybrid clock holds stamps at the wall**: the home clock led the
   wall by -3 to -4 ms in every window, where the slices' millisecond
   stamps ran 21 s ahead (default 2 did what it was for).
5. **The idle micro topology** keeps each task thread at 2 to 4% of a core
   with no offers at all (the 10 s idle windows).
6. **Rama's `thaw` accepts subindexed references** and returns a
   `ReferenceID` (the first attempt's T7); F9 expected a refusal. Such
   values are classified total and never picked; none was a lock row.

**Not run**: the full set's steps (variant B, the divisor at an assumed
agent speed; variant C and the entry bytes; `h40-again`; the per-act grain,
`h40-act4`; the person's own reads), by Sid's test rule. Nothing else ran.
