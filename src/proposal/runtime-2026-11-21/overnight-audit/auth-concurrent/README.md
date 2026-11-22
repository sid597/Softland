# Concurrent Checks for `Auth`

[Parent: Overnight audit](../MORNING.md) · [Auth spec](../../spine/auth/SPEC.md) · [Auth plan](../../spine/auth/PLAN.md) · [Plan validation](../../spine/auth/PLAN_VALIDATION.md)

Checks that hold `Auth` under concurrent callers, multi-partition interleavings, retries, helpers, differing clocks, and 2–8 OS threads at once, without touching `src-auth/`, `test-auth/`, or `PLAN.md`.

## Files

- [`deps.edn`](deps.edn) — standalone Clojure classpath (`:step1` runs the multi-task plan model; `:probes` runs the Rama 1.6.0 `InProcessCluster` and `softland.store.act` verification probes; `:step3` runs the 2–8 thread linearizability runner against `test-auth/softland/auth/reference.clj`).
- **Step 1 — Executable multi-task model of `PLAN.md`:**
  - [`src/auth_concurrent/plan_steps.clj`](src/auth_concurrent/plan_steps.clj) — `PLAN.md`'s single-task steps for `invite` (§1A), `enter` with 1 claim and with 2 claims (§1B), `answer-ask` (§1C), `:root` take-outs and `end-actor` (§2), and the four reads (`who-is`, `ask-of`, `under`, `watch-root`), each tagged with its `PLAN.md` line range.
  - [`src/auth_concurrent/spec_oracle.clj`](src/auth_concurrent/spec_oracle.clj) — pure sequential state machine of `SPEC.md` with Sid's two calls of 22 November, plus causal linearizability search.
  - [`src/auth_concurrent/model_check.clj`](src/auth_concurrent/model_check.clj) — exhaustive multi-task scheduler with per-`(src-task, dst-task)` FIFO channels, failure injection after every step, `:individual` retry, `:all-after` partition replay (mid-flight and post-commit), `:timeout-overlap` retry, and client re-runs.
  - [`src/auth_concurrent/step1_run.clj`](src/auth_concurrent/step1_run.clj) — runs the 25 2-caller and 3-caller scenarios across all four modes (100 runs) and writes [`STEP1_RESULTS.md`](STEP1_RESULTS.md).
  - [`STEP1_RESULTS.md`](STEP1_RESULTS.md) — answers to the five Step-1 questions, whole shortest witness histories with `PLAN.md` line citations for every step, and scope/limits of the search.
- **Step 2 — Empirical Rama 1.6.0 & store boundary probes:**
  - [`src/auth_concurrent/step2_probes.clj`](src/auth_concurrent/step2_probes.clj) — verifies (F) Auth's 32 vs `softland.store.act`'s 32, (B) single-writer PState ownership + hybrid microbatch `<<batch` barriers + stream `local-select>`, (E) 2-seek vs 3-seek `who-is` equivalence and seek counts, and (C) `end-actor` non-yielding sizing (18,000-seek BFS vs 2-scan `ao:` + `$$live`) and sibling-root `who-is` / `ping-task` stalls on a shared task.
- **Step 3 — Multi-thread concurrent linearizability runner (2 to 8 threads):**
  - [`src/auth_concurrent/linearizability.clj`](src/auth_concurrent/linearizability.clj) — real-time partial-order (`returned-ns < invoked-ns`) linearizability and post-batch read checker (`who-is`, `ask-of`, `under`, `watch-root`) held against `test-auth/softland/auth/reference.clj`, allowing both taken and not-taken outcomes when a write returns `{:could-not ...}`, plus `api-from-ns` to wrap any namespace providing the 18 calls of `softland.auth.client`.
  - [`src/auth_concurrent/racy_copies.clj`](src/auth_concurrent/racy_copies.clj) — six purposefully racing implementations of the 18 client calls that pass when serialized one call at a time and race under concurrent threads.
  - [`src/auth_concurrent/step3_run.clj`](src/auth_concurrent/step3_run.clj) — drives any 18-call target from 2 to 8 threads across all required contention patterns (`invite`, `enter` 1-account, `enter` 1-claim, `answer-ask`, `replace-secret` vs `end-sign-in`, `end-actor` vs `open-session` and `make-actor`, and an 8-thread mix), proving `reference.clj` passes when locked and all six racy copies are caught.

## Commands

```bash
cd src/proposal/runtime-2026-11-21/overnight-audit/auth-concurrent

# Step 1: exhaustive multi-task model of PLAN.md
clojure -M:step1

# Step 2: Rama 1.6.0 InProcessCluster & store act.clj probes (must hold cluster.lock)
flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \
  clojure -M:probes

# Step 3: 2..8 thread linearizability check on reference.clj + 6 racy copies
clojure -M:step3
```

To run Step 3 against a live Rama `softland.auth.client` module once `src-auth` is built:

```clojure
(require '[auth-concurrent.linearizability :as lin]
         '[auth-concurrent.step3-run :as s3])

(s3/check-suite!
  {:api (lin/api-from-ns 'softland.auth.client)
   :make-cluster (fn [mod-name] ...)} ; launches Auth under mod-name and returns cluster
  {:rounds 20})
```
