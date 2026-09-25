# Rama check, 25 September 2026

Throwaway. Checks on a real in-process cluster the two Rama claims the formal
model (`../formal-model-2026-09-24/README.md`, "What was assumed") takes on
trust, plus the stamp and failover questions around them. Nothing here is a
design; the module exists to be run once and read.

Plan, in place of the rama skill's phased artifacts (a probe, not a module
meant to run in production; the skill's "use judgment for simpler tasks"
clause applies):

- `src/check/faults.clj`: an in-JVM side channel. The runner arms a throw or a
  block at a named point in an event; topology code hits the point; the runner
  reads attempt counts and notes back. Works only because the in-process
  cluster shares the runner's JVM.
- `src/check/module.clj`: `StampModule` (a stream topology with `$$stamps`) and
  `CheckModule` (stream topology `s` with a retrying and a non-retrying source,
  microbatch topology `mb`, and a mirror of `$$stamps`). Every event writes on
  partition A (clock tick, append, count), hops, writes on partition B.
- `src/check/run.clj`: the experiments, one `record!` per claim, and a summary.
  Writes `results.edn` here. `src/check/run3.clj`: the exception-class check
  (which thrown classes kill the worker), writes `results3.edn`.

Run: `clojure -M:run > run2.log 2>&1` from this folder (4 tasks, 4 threads,
1 worker; keys chosen with `gen-hashing-index-keys` so A is task 1 and B is
task 2, away from task 0 where the microbatch runner lives).
`clojure -M:probe -m check.run3 [runtime|intentional|ex-info ...]` for the
exception-class check.

What it showed: RESULTS.md. Logs kept: `run.log` (first run, died at its
first injected failure), `run2.log` (full run), `run3.log` (three exception
classes), `run4.log` (Rama's own failure class alone).
