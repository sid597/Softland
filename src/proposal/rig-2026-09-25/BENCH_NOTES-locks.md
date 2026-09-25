# Bench notes: lock store growth under hand layers (phase 7, second number)

Running log of the lock-growth measurement, 25 September 2026. What ran, with
what configuration, what came out. Results with method: `runs/phase7-lock-growth.txt`;
raw per-point data: `runs/phase7-lock-growth.edn`.

Machine: AMD Ryzen 9 9900X (12 cores, 24 threads), 62 GB RAM, Linux
7.0.0-31-generic, OpenJDK 21.0.12.1. Rama 1.6.0, in-process cluster.

## Setup

- Slice: `src/rig/bench/lock_slice.clj`, module `rig.bench.LockSlice`. Per value,
  in the stream topology: a fresh 32-byte lock, the value's canonical EDN sealed
  with AES-256-GCM (12-byte nonce prepended, 16-byte tag), the lock wrapped under
  the person locks of the model's `:owner-required` wrap (fixed bench person
  locks stand in for `$$persons`). Writes the log row (`:v` sealed, `:lock-id`
  `[:value fid]`, `:lock` nil) and the lock row under `[layer :locks lock-id]`
  as the phase 2 plan draws them. Two PStates of one schema: `$$layers`
  (base64 in String slots, L4) and `$$layers-raw` (raw bytes in `byte/1` slots).
- Bench: `test/rig/bench/lock_bench.clj`, `clojure -M:bench rig.bench.lock-bench run <variants|all> [values] [every]`.
- Probes before the build (scratch, not kept):
  - The in-process cluster keeps each PState's RocksDB in its own directory
    under its temp dir (`objects/<module>/<instance>/local/%24%24<pstate>/<task>/0`);
    the bench finds it through the cluster's `temp_dir` field, so a parallel
    session's cluster cannot be mistaken for it.
  - Rama's RocksDB options there: write buffer 64 MB, Snappy, 4 KB blocks,
    bloom filter 10 bits, WAL on and preallocated (about 70 MB of allocated
    blocks per WAL, apparent length = bytes written). So below 64 MB of
    memtable a PState's directory is almost all WAL; the bench also measures
    a flushed and compacted copy.
  - `byte/1` is accepted as a schema class and round-trips a byte array (the
    plan's open question on `[B`); a String slot refuses bytes.
  - A nil field in a fixed-keys record is stored: the plan's lock record with
    `:any-blobs nil` is 12 bytes larger than without it.

## Runs

### Smoke 1 (about 10:59): 4 variants, 2,000 values, a point every 500

Found two bench bugs, fixed: the key-value scan read only RocksDB's default
column family (Rama keeps every subindexed element in a second column family,
`subindexed`; the SST total already counted both), and the row check assumed a
lock row in the no-lock variant. Results discarded (moved to scratch).

### Smoke 2 (11:04:30 to 11:05:05): all 10 variants, 2,000 values, a point every 500

Output to scratch, not kept. Every variant ran with 0 append errors; 100 of 100
sampled values opened through their lock rows and none opened without the
owner's lock; in the two-person variant none opened without Bob's. Lock rows
per value: logical 189 B (base64) / 169 B (raw) / 233 B (two people, base64);
the raw RocksDB key-value difference agrees within 2 to 4 B; compacted SST
about 111 / 90 / 153 B. Shape checked, not the result.

Overlap: a parallel session's timing benches (`phase7-agent-rate-run*`,
`phase7-one-thread-run*`) were running in the same worktree. Both smoke runs
(about 14 s and 35 s) may have overlapped its runs 2 and one-thread run 1.
From here each variant starts only after 30 s with no other rig bench or test
JVM, one JVM per variant, and any overlap is logged with times in
`runs/phase7-lock-growth-overlap.log`.

### Full run, first attempt (11:10:33): failed to start, runner replaced

`small-b64-lock` at 100,000 values could not start its cluster: "Failed to
bind to 0.0.0.0:2002: Address already in use". The in-process cluster binds a
fixed port, 2002 (its admin web server), and the parallel session's
`rig.bench.stream-bench diag` had started seconds earlier and held it. So two
in-process clusters cannot run at once on this machine: the port is a mutex,
and a collision fails loudly at start rather than slowing the other's timings
unnoticed. A cluster of mine holding the port would equally make a start of
theirs fail; that is the cost of running the two sessions side by side.
Nothing was written to the results. The runner (session scratch, not a rig
file) now waits for 30 s with no other rig bench or test JVM and port 2002
free, retries a failed start up to five times, logs overlaps on change only,
and leaves 45 s between variants for the other session. Restarted 11:12:08.

### Full run, 100,000 values, a point every 10,000, 1 task / 1 thread / 1 worker, 64 appends in flight

- `small-b64-lock` 11:12:46 to 11:13:24, no other rig JVM seen. 100,000 lock
  rows; logical 18,900,000 B (189 B per row at every point); `$$layers`
  directory live 50,528,084 B (write-ahead log only, no memtable flush yet);
  compacted copy 27,276,300 B of SST. 100 of 100 sampled values opened
  through their lock rows, none without the owner's lock. 0 append errors.
- `small-b64-none` 11:14:40 to 11:15:12. Another rig JVM (pid 371802) lived
  from about 11:14:56 to 11:15:02 while this cluster held port 2002; what it
  was is not known, and the other session's logs show no failure. Its own
  numbers are bytes, so they are unaffected. Live 30,695,631 B, compacted
  16,191,474 B; no lock rows; the 100 sampled rows hold ciphertext.
- `small-raw-lock` 11:16:27 to 11:17:03, no overlap. Logical 16,900,000 B
  (169 B per row); live 46,028,074 B; compacted 22,638,542 B; 100 of 100 open.
- From 11:18 the other session ran its runner back to back (3 s gaps); this
  runner waited for a 30 s quiet window, as intended.
- The other session's timing set (agent-rate 4 to 6, one-thread 4 to 6) ran
  11:18:13 to 11:29:57 by its own overlap log, with no overlap and no bind
  error; this runner waited through it. The two 6-second JVMs seen during my
  runs (11:14:56, 11:30:42) line up with rewrites of its
  `phase7-one-thread.txt` (11:14:48, 11:32:09): report runs, no cluster. No
  cluster of mine ever ran alongside one of theirs; the fixed port forbids
  it. What could have overlapped is only JVM start-up CPU: my smoke runs at
  about 10:59 and 11:04:30 fell near the ends of its agent-rate run 2 and
  one-thread run 1.
- `small-raw-none` 11:30:26 to 11:30:58. Live 28,297,726 B, compacted
  13,645,800 B; no lock rows.
- `large-b64-lock` 11:32:13 to 11:32:51, `large-b64-none` 11:34:07 to 11:34:41,
  `large-raw-lock` 11:35:56 to 11:36:34, `large-raw-none` 11:37:49 to 11:38:24,
  `two-b64-lock` 11:39:39 to 11:40:15, `two-b64-none` 11:41:30 to 11:42:03, all
  done 11:42:48; every variant on its first attempt, 0 append errors, every
  check as expected, no overlap after 11:30:48.

### What the first full run showed, and why it is rerun (11:45)

Logical lock rows: 189 B (base64), 169 B (raw), 233 B (two people), the same at
every point. The raw RocksDB key-value difference is 3 B more per row (the
subindex key prefix); exactly 100,000 more entries, all in the `subindexed`
column family. The live directory is write-ahead log until the memtable fills
(64 MB); in both 200-byte lock variants it flushed near 85,000 values, and the
WAL stayed on disk beside the new SST (the default column family's one entry
keeps it pinned), so the live difference jumps to about 620 B per value there:
flush timing, not lock cost.

The compacted measure was inconsistent: after that live flush the compacted
lock-store cost per value fell from about 110.7 to 103.7 B (base64), with
key-value counts and bytes still exact. Cause, reasoned from RocksDB and
consistent with the rerun below: `compactRange` with default options moves a
lone flushed file rather than rewriting it, so most points measured flush
output with sequence numbers intact, while after a live flush two files
overlap and are really rewritten, which zeroes sequence numbers at the bottom
level. Fix: the copy is now compacted with `BottommostLevelCompaction/kForce`,
and the flush output is recorded too (`:sst-flushed`). Checked at 2,000 values:
forced 1,002,828 B against 1,018,718 flush-only. All ten variants rerun; the
report takes the latest result per variant, and the first run's lines stay in
the EDN.

### Rerun with forced compaction, 11:46:38 to 12:04:42

All ten variants, first attempt each, no overlap, 0 append errors, every check
as expected. Logical bytes repeated the first run exactly; raw KV within 60 B
in 19 MB. Forced compaction makes the lock store's disk cost the same at both
value sizes (107.9 B per value in base64, 86.9 raw; 148.0 for two people), and
the dip after the live flush is gone, which confirms the cause above. Per
value: logical 189 / 169 / 233 B; compacted 107.9 / 86.9 / 148.0 B. Ratio to
plaintext: 40 B 4.73 / 4.23 logical, 2.70 / 2.17 disk; 200 B 0.95 / 0.85,
0.54 / 0.43; two people 5.83 / 3.70. Linear at every point. Results, reading,
verdict and uncertainties: `runs/phase7-lock-growth.txt`.
