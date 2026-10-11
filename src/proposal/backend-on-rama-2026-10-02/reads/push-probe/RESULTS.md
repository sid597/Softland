# What Rama's push can watch — two runs, 11 October 2026

The read sheet's W11 says: "One run on Rama settles what is possible. It is not
run." The other sheet's last page lists "What Rama's push can watch" as waiting
for a run. These are those runs.

- Rama 1.6.0, an in-process cluster on one machine, 4 tasks, 2 threads.
- A probe, not a design. Its tables stand in for the questions. One of them has
  the shape of the store's facts table: a top-level map from a place key,
  `"<layer> <entity> <key>"`, to a row.
- One watcher. Small values. Each case ran once. Times are rough: they include
  the append call, and the JVM was warming up.
- Run again: `clojure -M:run` (first run), `clojure -M:run2` (second run), from
  this folder. `run-2.log` is the second run's output of 11 October. The first
  run's output is 578 KB, most of it the server's errors for the three refusals,
  so it is kept outside the repository:
  `/mnt/data/projects/research/softland/rama-runs-2026-10-11/push-probe/run-1.log`.
- Who ran it: the session that went through the read sheet with Sid on 11 and
  12 October 2026 (Opus 5.5, effort max).

## Found

| # | What the push watches | Opens | What a change sends |
|---|---|---|---|
| F1 | one place | yes | that change only; a change inside the row comes as a change of that field |
| F2 | a run of neighbouring places (one entity), top-level map | yes | only the changed, new or removed place; a place outside the run sends nothing |
| F3 | a run that is empty when the push opens | yes | the first place, when it arrives |
| F4 | the values of a run, gathered into one list | yes | the whole list again on each change |
| F5 | how many places a run holds | yes | the new number |
| F6 | the whole table | no | — |
| N1 | one place under a map that keeps its elements apart | yes | that change only |
| N2 | the whole map that keeps its elements apart | no | — |
| N3 | a run of that map | yes | as F2 |
| E1 | an entity held as one value | yes | the changed or new key only |
| P1 | a set held as one value | yes | the one element added or removed |
| S1 | a whole set that keeps its elements apart | no | — |
| S2 | a run of that set, from "" to "~" | yes | the one element added or removed |
| S3 | how many elements that set holds | yes | the new number |
| Z1 | a run of 5,000 places | yes, in 59 ms | one small diff for one changed place |
| Z2 | a run of 20,000 places | yes, in 32 ms | one small diff for one changed place |
| Z3 | a run of 3 places in a table of 20,003 | yes, in 2 ms | a change among the 20,000 others sends nothing |
| Z4 | the first 3 places of a run, as a window | yes | a new place that sorts into the window comes in and the last one goes out, in one diff |
| Z5 | a range from "" to "~", all of one task's keys | yes | only the changed or new key |
| Z6 | a run on a table that a microbatch topology writes | yes | as F2, 114 and 285 ms after the append call |

On a table that a stream topology writes, a push came 4 to 34 ms after the
append call.

## The three refusals

F6, N2 and S1 did not refuse at once. Each call hung for about 100 seconds. The
server logged "Serializer not defined for type class
rpl.rama.durable.RocksDBWrapper" every 10 seconds, and the client then got
"CallbackException: Callback failure". So whatever opens pushes must never ask
for a whole table or a whole collection that keeps its elements apart. Rama's
docs say the same of what cannot be watched (`15-pstates.md`, line 654); the
run shows how it fails.

## Not settled by these runs

- A real cluster, and two nodes. A network between the runtime and Rama.
- Many watchers at once. The docs name a limit after which watchers are left
  out of a change and sent the value anew (`15-pstates.md`, line 1230).
- Large values, and the memory that the watcher holds for a big run: a push
  over 20,000 places keeps 20,000 rows in the watcher's memory.
- What one more kept order costs each write. No such table is built.
- Anything about the store's own module. The probe does not load it.
