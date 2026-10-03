# Bench notes: phase 7 on the stream store (running log)

Two of phase 7's three numbers, measured on the stream store as it stands
(`src/rig/store/`, unchanged). The harness is `test/rig/bench/stream_bench.clj`;
results are saved to `runs/phase7-agent-rate.txt` and
`runs/phase7-one-thread.txt`, raw per-run lines to `runs/phase7-*.log`.

Machine: AMD Ryzen 9 9900X (12 cores, 24 threads), 62 GB RAM,
Linux 7.0.0-31-generic, OpenJDK 21.0.12.1. The in-process cluster keeps its
files under `/tmp`, which is ext4 on an NVMe SSD (WD_BLACK SN850X), not tmpfs.
Load average before the first run: 0.43.

## Configuration chosen (25 September 2026)

- In-process cluster, `{:tasks 4 :threads 4 :workers 1}`: a thread per task.
  The layer measured lives on one of the four tasks; the others idle.
- JVM: the `:bench` alias (`-Xss16m -Xmx16g`, default G1).
- Each run is its own JVM and its own cluster; three runs per measurement.
- An act: one fact `{:e <one of 1,000 entities> :k :note :v <38-char string>}`,
  so the value's canonical text is 40 bytes; no replaces, nothing stood on,
  no control facts. Offers are built with `c/build`, sent with `c/offer!`
  (acked append), timed around `c/offer!` only.

## Log

### 10:55 — probe (not a measurement)

`clojure -M:bench rig.bench.stream-bench probe` (output `runs/phase7-probe.log`).
Checked what the harness relies on: the value's canonical text is 40 bytes;
the depot has 4 partitions; `gen-hashing-index-keys` gives a key per task
(`["d" "a" "b" "g"]`), so each task's `$$clock` can be read; the layer
`:bench-agent` lives on task 2 (partition 2 grew, only task 2's clock moved);
the sample check reads back record, row and head. The four task threads are
named `Worker ... DTT 2 0-task-group-exec-<n>`. A cold 3 s window at K = 4:
959 acts, 319/s, p50 11.9 ms, p99 22.0 ms, the busiest task thread at 52 % of
a core. Cold numbers; the measured runs warm first. Warmup raised from 10 s
to 20 s at K = 4 before the first measured run.

### 10:57 — agent-rate run 1

`clojure -M:bench rig.bench.stream-bench agent-rate 1 > runs/phase7-agent-rate-run1.log`.
Layer `:bench-agent` (kind agent, owner `:ada`) on task 2. Warmup K = 4 for
20 s: 509 acts/s. Measured 15 s windows (acts/s; p50 / p95 / p99 / max ms;
busiest task thread % of a core):

| K | acts/s | index writes/s | p50 | p95 | p99 | max | task thread |
|---|---|---|---|---|---|---|---|
| 1 | 301 | 1,204 | 3.22 | 3.71 | 4.84 | 24.87 | 19 % |
| 4 | 607 | 2,428 | 5.79 | 8.87 | 9.78 | 13.62 | 23 % |
| 16 | 2,234 | 8,936 | 6.84 | 8.79 | 11.75 | 37.10 | 37 % |

Every offer admitted, no errors; every window one-task (only partition 2
grew, by the offer count; only task 2's clock moved); the sample check read
back record, row and head for 91, 184 and 200 acts, all ok. The task thread
is not saturated at K = 16, so the rate is latency-bound (rate = K / mean
latency). At K = 16 the task's stamps ran 20.7 s ahead of the wall clock:
each decision stamps clock + 1 once the task decides more than 1,000 acts a
second.

IPC's files: `/tmp/ipc<random>/` — `$$layers` in one RocksDB per task, a
replication log per task thread (`task-threads/.../replog`), the depot as
Kafka-style log segments per partition. No RocksDB directory for `$$clock`
(a top-level Long, not a map; the docs say only top-level-map PStates use
RocksDB).

### 11:02 — agent-rate runs 2 and 3

Same command, run ids 2 and 3 (`runs/phase7-agent-rate-run{2,3}.log`).
Home task 2 again in both. acts/s at K = 1, 4, 16: run 2 302, 638, 2,260;
run 3 301, 651, 2,183. p99 ms: run 2 4.71, 9.73, 10.10; run 3 4.68, 9.61,
11.33. All admitted, no errors, every window one-task, every sampled act's
indexes read back. The three runs agree within 7 %.

Change before the one-thread runs: phase 7 says "K = 1, 2, 4, 8, 16, 32 until
throughput stops rising"; at K = 16 the agent layer's task thread was only
37 % busy, so 32 may not reach the plateau. The one-thread runs add K = 64
and 128 after 32, reported apart as past the asked range.

### 11:05 — one-thread run 1

`clojure -M:bench rig.bench.stream-bench one-thread 1 > runs/phase7-one-thread-run1.log`.
Layer `:bench-person` (kind personal, owner `:pat`) on task 1. Warmup K = 4
for 20 s: 452 acts/s.

(a) Sequential, 2,000 acts after 1,000 warm: 280 acts/s; p50 3.44, p95 4.23,
p99 5.23, max 20.69 ms.

(b) Concurrent, 10 s windows:

| K | acts/s | p50 | p95 | p99 | max | task thread |
|---|---|---|---|---|---|---|
| 1 | 303 | 3.21 | 3.72 | 4.71 | 11.14 | 19 % |
| 2 | 378 | 5.17 | 5.98 | 7.04 | 32.72 | 21 % |
| 4 | 694 | 5.53 | 6.79 | 9.30 | 35.16 | 25 % |
| 8 | 1,278 | 6.02 | 7.66 | 9.39 | 12.98 | 30 % |
| 16 | 2,288 | 6.74 | 8.34 | 9.52 | 14.34 | 36 % |
| 32 | 3,310 | 8.96 | 12.63 | 23.74 | 51.42 | 44 % |
| 64 (past the ask) | 3,726 | 15.88 | 23.38 | 46.72 | 83.97 | 53 % |
| 128 (past the ask) | 4,986 | 25.01 | 33.10 | 38.50 | 56.39 | 63 % |

All admitted, no errors, every window one-task (only partition 1 grew, by
the offer count; only task 1's clock moved), sampled indexes all read back.
The rate still rises at 128; the task thread never passes 63 % of a core,
so the ceiling is not the task thread's CPU. Something else serialises the
acked path; not yet identified.

### 11:12 — one-thread runs 2 and 3

Same command, run ids 2 and 3. Home task 1 in both. Sequential (a): 293 and
292 acts/s, p99 5.00 and 4.97 ms, max 22.5 and 19.2 ms. Concurrent (b), acts/s
for K = 1, 2, 4, 8, 16, 32, 64, 128: run 2 297, 371, 705, 1,286, 2,221,
3,375, 4,369, 5,213; run 3 295, 369, 701, 1,280, 2,213, 3,425, 4,200, 5,501.
p99 at K = 32: 16.0 and 13.4 ms (run 1 had 23.7). All admitted, no errors,
every window one-task, sampled indexes all read back. The rate still rises
at 128 in all three runs; the task thread tops out near 60 %.

### 11:20 — diagnostic under strace (not a measurement)

`strace -f --seccomp-bpf -ttt -T -y -e trace=fsync,fdatasync,sync_file_range,msync,sync,syncfs
clojure -M:bench rig.bench.stream-bench diag` (new function `diag`: 10 s warm,
then 5 s windows at K = 1 and K = 16 with their wall-clock bounds; output
`runs/phase7-diag-strace.log`, the trace in the session scratchpad). Tracing
slowed it: 232 and 1,427 acts/s against 301 and 2,234 untraced. Syncs inside
each window, by file:

- K = 1, 1,160 acts: 5.99 syncs per act, each about 0.4 ms — replog fsync
  1.99, `*offers` log segment fsync 0.99, `$$layers` RocksDB WAL fdatasync
  0.99, `$$clock` storage fsync 0.99, streaming-state checkpoint 0.01.
- K = 16, 7,147 acts: 0.95 syncs per act (replog 0.25, `$$clock` 0.19,
  WAL 0.19, depot 0.12): batching shares them.

So the 3.3 ms floor at one offerer is mostly serial disk flushes on this
machine; the task thread's own work is about 0.64 ms per act at K = 1 and
0.17 ms at K = 16 (derived from its CPU share and the rate).

### 11:25 — summary and results files

`clojure -M:bench rig.bench.stream-bench summarize runs/phase7-*-run*.log`
computed the medians and spreads. Written: `runs/phase7-agent-rate.txt`,
`runs/phase7-one-thread.txt` (method, configuration, machine, numbers,
placement, verdicts, the diagnostic, the stamp drift).

Medians: agent layer 301 / 638 / 2,234 acts/s at K = 1 / 4 / 16 (index
writes 1,204 / 2,552 / 8,936 per s at 4 per act), p99 4.71 / 9.73 / 11.33 ms.
Person's layer sequential 292 acts/s, p99 5.00 ms, max 20.7 ms; K sweep
297 → 3,375 acts/s from K = 1 to 32 (p99 4.77 → 16.0 ms), still rising at
128 (5,213/s, p99 37.6 ms). Verdicts with the 10x rule: agent rate near at
every K (0.30x, 0.64x, 2.2x); person's layer rate far at K ≥ 8 (12.8x, 22x)
and latency near (p99 at best 4x under 20 ms).

### 11:16 — a parallel session in the same worktree; runs 1–3 not verified quiet

Found `BENCH_NOTES-locks.md`: a parallel session measuring lock growth ran
in-process clusters here too. Its smoke runs were at about 10:59:30 (after
its slice file appeared at 10:59:26; about 14 s) and 11:04:30–11:05:05. By
my estimates from log end times, the first may have overlapped agent-rate
run 2's K = 16 window and the second came just after one-thread run 1's last
measured window; neither was monitored. Run 2's K = 16 numbers were the best
of the three, so no visible effect, but runs 1–3 are not verified quiet. The
in-process cluster binds the fixed port 2002, so two clusters cannot both
start; that session's runner now waits for 30 s with no other rig JVM.

So a monitored set, runs 4–6 of each measurement, started 11:17:43:
a scratch wrapper waits until no other JVM runs and port 2002 is free (30 s
before the first run, 3 s between mine, so the other runner keeps yielding),
runs one measurement, and logs every other JVM seen while it runs, every 2 s,
to `runs/phase7-stream-overlap.log`. A start that fails on the port is
retried. Result lines now also carry each window's wall-clock bounds
(`:wall-ms`), to line up with that log. Runs 4–6 are to be the reported set;
runs 1–3 stay as a consistency check.

### 11:18–11:30 — monitored runs 4–6 (the reported set)

The wrapper (scratch `run-guarded.sh`, self-tested: it does see another JVM
when one runs) started agent-rate runs 4, 5, 6 at 11:18:13, 11:19:41,
11:21:09 and one-thread runs 4, 5, 6 at 11:22:36, 11:25:04, 11:27:33, each
after its quiet check; every run exited 0; `runs/phase7-stream-overlap.log`
shows no other JVM during any of them.

Agent layer, medians (spread): 299 (298–299), 646 (590–672), 2,249
(2,209–2,276) acts/s at K = 1, 4, 16; index writes 1,196, 2,584, 8,996 per s;
p99 4.74, 9.79, 9.94 ms. Person's layer: (a) 299 (296–300) acts/s, p50 3.26,
p95 3.74, p99 4.72 (4.69–4.83), max 20.9 ms; (b) 300, 374, 711, 1,307,
2,228, 3,362 acts/s for K = 1 … 32 (p99 4.71 → 16.45 ms), past the ask 4,424
and 5,192 at 64 and 128 (p99 22.0, 43.9 ms). Every window one-task, every
sampled act's indexes read back, no errors. Runs 1–3 agree within 3 % on
every rate median except one-thread K = 64 (5 %).

`runs/phase7-agent-rate.txt` and `runs/phase7-one-thread.txt` rewritten on
runs 4–6, with runs 1–3 as the consistency check.

Verdicts (rule: far = at least 10x from the threshold; near = within 10x):
- Agent rate, 1,000 acts/s per task with every index written: 0.30x, 0.65x,
  2.2x at K = 1, 4, 16 → near, the in-process cluster cannot decide. One
  offerer waiting on acks is latency-bound at about 300/s; the task itself
  reached 5,192/s at K = 128, still rising, thread at 60 %.
- One person's layer, 100 acts/s at p99 ≤ 20 ms: rate far (13x at K = 8,
  22x at K = 16, p99 under 10 ms); latency near (best p99 4.7 ms, 4.2x under).

Nothing stopped a measurement. My strace diag run held port 2002 at 11:10:33
and made the lock session's first full-run start fail; it retried at 11:12.
