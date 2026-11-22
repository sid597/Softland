# Concurrent Checks for `Auth`

[Parent: Overnight audit](../MORNING.md) · [Auth spec](../../spine/auth/SPEC.md) · [Auth plan](../../spine/auth/PLAN.md) · [Plan validation](../../spine/auth/PLAN_VALIDATION.md)

Checks that hold `Auth` under concurrent callers, multi-partition interleavings, retries, helpers, and differing clocks, without touching `src-auth/`, `test-auth/`, or `PLAN.md`.

## Files

- [`deps.edn`](deps.edn) — standalone Clojure classpath (`:step1` runs the multi-task plan model; `:step3` runs the multi-thread linearizability check over `test-auth`).
- [`src/auth_concurrent/plan_steps.clj`](src/auth_concurrent/plan_steps.clj) — `PLAN.md`'s single-task steps for `invite` (§1A), `enter` with 1 claim and with 2 claims (§1B), `answer-ask` (§1C), `:root` take-outs (§2), and the four reads (`who-is`, `ask-of`, `under`, `watch-root`), each tagged with its `PLAN.md` lines.
- [`src/auth_concurrent/spec_oracle.clj`](src/auth_concurrent/spec_oracle.clj) — pure sequential state machine of `SPEC.md` with Sid's two calls of 22 November, plus causal linearizability search.
- [`src/auth_concurrent/model_check.clj`](src/auth_concurrent/model_check.clj) — exhaustive multi-task scheduler with per-`(src-task, dst-task)` FIFO channels, failure injection after every step, `:individual` retry, `:all-after` partition replay (mid-flight and post-commit), `:timeout-overlap` retry, and client re-runs.
- [`src/auth_concurrent/step1_run.clj`](src/auth_concurrent/step1_run.clj) — runs the 2-caller and 3-caller suites across all four modes and writes [`STEP1_RESULTS.md`](STEP1_RESULTS.md).
- [`STEP1_RESULTS.md`](STEP1_RESULTS.md) — answers to the five Step-1 questions, whole shortest witness histories with `PLAN.md` line citations for every step, and scope/limits of the search.

## Running Step 1

```bash
cd src/proposal/runtime-2026-11-21/overnight-audit/auth-concurrent
clojure -J-XX:TieredStopAtLevel=1 -M:step1
```
