# Build: Auth (`src-auth/` + `softland.auth.client`)

Complete build record of the Rama module `Auth` (`softland.auth.module/Auth`) and its foreign client (`softland.auth.client`), implementing all 18 calls of [SPEC.md](SPEC.md) (including Sid's calls of 22 November) and the revised [PLAN.md](PLAN.md) (Revisions 1–6 and 8 of [PLAN_VALIDATION.md](PLAN_VALIDATION.md); Revision 7 closed for this build at depth 32).

---

## 1. What was built

### Step 1 & Step 2 — Revised Plan and Model-Checked Cross-Key Agreement (`7b45edc0`)
- **`src/proposal/runtime-2026-11-21/spine/auth/PLAN.md`**: Applied Revisions 1–6 and 8 of `PLAN_VALIDATION.md` in order (Revision 7 recorded as closed at 32 makers below root; live sign-in order kept in `$$live` so `under` never walks ended sign-ins).
- **`src/proposal/runtime-2026-11-21/spine/auth/agreement/`**:
  - `src/auth_concurrent/plan_steps.clj` — Revised single-task step transitions for `invite`, `enter`, `answer-ask`, 2-seek `who-is`, and 2-scan `end-actor` in the exact shape of carbon's `plan_steps.clj`.
  - `src/agreement/check.clj` — Runner driving carbon's unchanged `model_check.clj`, `spec_oracle.clj`, and `step1_run.clj` across all 25 carbon scenarios + 7 extra adversarial scenarios (`X-01..X-07`) × 4 fault/retry modes = **128 runs** (`88,261` states, `539` quiescent terminal histories, client re-runs checked in all 4 modes, **0 capped runs, 0 breaks across all 5 questions**).
  - `README.md`, `RESULTS.md`.

### Step 3 — Module and Client (`src-auth/`), Built Slice by Slice
- **Slice a (`374468ee`) — Module frame, `invite`, `enter`, `own-layer-made`:**
  - `src-auth/softland/auth/fit.clj` — Pure input-form validation for all 18 calls (Sid's Call 1: `contains?` on every `-or-nil` key, `:driven-by` alone optional, exact keys on top-level and sub-maps, `Locale/ROOT` lower-case emails, `fn?` on `watch-root`, fixed-width int coerced to `Long`, throwing-comparator safety).
  - `src-auth/softland/auth/schema.clj` — Typed Java marker interfaces (`IRootDatum`, `ILiveEntry`, `IAccountState`, `IClaimState`, `IAskState`, `IEnterOpState`, `IDrivenBy`, `IRootEvent`, `IAskEvent`), strictly typed `defrecord`s (zero `Object` positions, zero byte arrays), 7-bit ASCII key encoders (length-prefixed bijective `account-key`, prefix-disjoint `claim-key`, order-preserving `hex-ts`/`hex16`), read projections, and pure single-task transition functions.
  - `src-auth/softland/auth/module.clj` — `defmodule Auth`: 4 partitioned depots (`*root-events`, `*asks`, `*invites`, `*enters`), 6 partitioned PStates (`$$root-data`, `$$live` with `pstate.proxy.limit = 20000`, `$$accounts`, `$$claims`, `$$asks`, `$$enter-ops`), single stream topology `"auth"` (`{:start-from :beginning :retry-mode :all-after}`), and 3 query topologies (`"who-is"`, `"under"`, `"ping-task"`).
  - `src-auth/softland/auth/client.clj` — Public contract namespace `softland.auth.client`.
  - `test-auth/softland/auth/module_harness.clj` — `launch-module` (0-arg factory for `softland.auth.module-check`), direct `$$accounts` `:says` table check, `:not-reached` / callback-isolation check, and slice runner invoking carbon's unchanged `auth-concurrent.step3-run`.
- **Slice b (`25d3b5be`) — Root writes, take-out, 3-seek 2-scan `end-actor`, 2-seek `who-is`, live-only `under`:**
  - All 7 `:root` writes (`begin-sign-in`, `replace-secret`, `end-sign-in`, `make-actor`, `end-actor`, `open-session`, `close-session`) on `*root-events` (`0` hops, atomic single-task batches).
  - `take-out-on-root!` (1-seek `exp:` scan fast path when nothing expired; atomic `$$live` cascade when expired) and Revision 2's 3-seek 2-scan `end-actor` (`a:<A>` + forward `ao:` scan + `$$live` scan) applied via flat `<<atomic` + `ops/explode` (O(1) JVM stack depth, zero `ch:`/`ag:`/`sg:` indexes).
  - Revision 3's 2-seek `who-is` fast path (`g:<G>` read first, skipping `"<P>\0r"` when `:own-layer-made-at-begin?` is `true`).
  - *(Operational note on commit `25d3b5be`: another concurrent session had already staged files in `gate-histories/` and `store-piece-2/SPEC.md` in `.git/index` right before `git commit` ran, so `25d3b5be` picked up those already-staged files alongside our 4 files. Starting from Slice c onward, every commit used `git commit --only -- <our-files>` to stay completely isolated from `.git/index`.)*
- **Slice c (`23eb936d`) — `ask`, `answer-ask`, `ask-of`:**
  - `ask` (0 hops on `*asks`), Revision 4 `answer-ask` (`ok:<op>` checked BEFORE `take-out` in Step 2; Step 1b `:no-such-ask` replay guard; unconditional helper take-out on `(:root self-input)` at `(:at self-input)` in Step 3), and 1-seek direct `foreign-select-one-async` `ask-of`.
- **Slice d (`79f63722`) — `watch-root`, `open`, `close!`, `:softland.auth.client/not-reached`:**
  - Shared `foreign-proxy-async` per `(client, root)` on `$$live`, incremental `Diff$Processor` / `KeyDiff$Processor` / `KeyRemoveDiff$Processor` projection (omitting `:driven-by`), initial `ResyncDiff` and `DestroyedDiff` suppression, per-watcher turn locks + `ThreadLocal` re-entrancy guard (preventing deadlocks when a callback calls `stop` or `close!`), exception isolation per watcher, and background `"ping-task"` liveness monitor (2 s cadence, 5 s timeout + 3 s partial-outage probe = 8 s outage ceiling) delivering `:softland.auth.client/not-reached` exactly once within seconds of module/task loss.

### Step 4 & Step 5 — Full-Life Measurements and Self-Explaining Tree
- **`test-auth/softland/auth/full_life_measure.clj`** — All four STEP 4 full-life cluster measurements on live Rama 1.6.0 `InProcessCluster`s.
- **Folder READMEs & docstrings** — `src-auth/README.md`, `src-auth/softland/README.md`, `src-auth/softland/auth/README.md` (following `src-store/CLAUDE.md`), and verified `0` missing docstrings across all 6 source/test files (`ns`, `def`, `defn`, `defn-`, `deframaop`).

---

## 2. Forks taken (and why)

1. **Stream choose-at-account protocol for `invite` and `enter` (instead of switching to hybrid microbatch):**
   - Round 1 of our revised stream agreement passed 110/112 runs; the 2 failures (`2C-04 :all-after`, `3C-03 :all-after`) had one identical root cause (post-commit `:all-after` replay of a previously-refused `enter` depot record).
   - Round 2 added `:status` (`:probing | :chosen | :failing`) on `AccountLinking` and recorded PII-free `EnterOpFailed` in `$$enter-ops[op-id]` on refusal only (checking `H(op)` before E3 claim mutation and before returning/helping `:says` upgrade). Round 2 passed **128/128 runs (`88,261` states, `0` capped, `0` breaks)** — well within the 3-round stop rule, keeping single PState ownership, ~5 ms write latency, and zero task-0 microbatch coordination.
2. **String keys for `$$accounts` and `$$claims` (instead of `defrecord` keys):**
   - Used length-prefixed bijective strings (`"<len(p)>:<p>:<subject>"` for `$$accounts`; `"e:<email>"` / `"c:<hash>"` for `$$claims`) as both PState map keys and depot `(hash-by ...)` keys. Guarantees native Rama `String` serialization and RocksDB ordering with zero custom-key comparator risk, and matches `|hash` partition keys 100%.
3. **Carrying full fields on `LiveSignIn` and immutable `:driven-by` on `LiveSession` inside `$$live`:**
   - Lets 2-scan `end-actor`, `end-sign-in`, and `take-out` write ended `SignInRec` and closed `SessionRec` (preserving `:driven-by`) with **0 point seeks** on `g:`/`s:` and **0 `ch:`/`ag:`/`sg:` indexes**. `watch-root`'s client projection strictly omits `:driven-by` (and all extra fields), and because `:driven-by` is immutable once opened, carrying it on `LiveSession` never emits spurious proxy diffs.
4. **Flat `<<atomic` + `ops/explode` in `apply-root-plan!` (instead of recursive `loop<-`):**
   - Probe C in `PLAN_VALIDATION.md` showed non-yielding `loop<-` overflows the JVM call stack at 3,000/2,000/5,000. Replacing `loop<-` with `<<atomic` + `ops/explode` runs in O(1) JVM stack depth and completed the 3,000/2,000/5,000 `end-actor` cascade in **300.6 ms**.
5. **Checking `ok:<self-op>` on `answer-ask` Step 1b (`:no-such-ask`):**
   - Recorded `AskOutcomeRefused(:no-such-ask)` on `H(P)` during Step 1b (which already visits `H(P)` for take-out -> 0 extra hops), closing `:all-after` replay of `:no-such-ask` after a later `ask(U)` arrives (`X-05`).
6. **`ping-task` outage ceiling set to 5 s ping timeout + 3 s fallback probe = 8 s (cadence 2 s):**
   - Keeps `destroy-module!` detection fast (**1,842.8 ms** measured) while sitting 26× above worst-case 3,000/2,000/5,000 `end-actor` (**300.6 ms**), preventing false `:not-reached` storms across sibling watched roots.

---

## 3. Commands run and verbatim outputs

### A. Step 2 — Model Check of Revised Cross-Key Agreement (Round 2)
```bash
cd /mnt/data/projects/Softland/src/proposal/runtime-2026-11-21/spine/auth/agreement && \
clojure -Srepro -Sdeps '{:paths ["src" "../../../overnight-audit/auth-concurrent/src"] :deps {org.clojure/clojure {:mvn/version "1.12.4"}}}' -J-Xmx4g -M -m agreement.check
```
Output (tail summary; full 128-row table in `spine/auth/agreement/RESULTS.md`):
```text
Summary: runs=128 states=88,261 terminals=539 capped=0 violations=0 elapsed=3421ms
```

### B. Step 3 — Slice a Check (`:says` PState Table + Carbon `step3-run` Scenarios `1a..3`, 3 Rounds)
```bash
cd /mnt/data/projects/Softland && flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \
  clojure -Sdeps '{:aliases {:ac {:extra-paths ["src/proposal/runtime-2026-11-21/overnight-audit/auth-concurrent/src"]}}}' \
  -M:auth:ac -m softland.auth.module-harness slice-a 3
```
Output (summary lines):
```text
PASSED Refused enter returns :not-invited and leaves $$accounts[Acc] nil
PASSED Invite + first enter at t=200 stores :says={"name" "v200"} and :says-at=200
PASSED Equal :at=200 leaves kept :says unchanged (Sid's Call 2)
PASSED Earlier :at=100 leaves kept :says unchanged (Sid's Call 2)
PASSED Later :at=350 replaces kept :says and :says-at
PASSED Four concurrent returning enters (400, 700, 500, 600) keep :says of latest :at=700
  [round 1..3] PASSED 1a, 1b, 2a, 2b, 3 (2..5 threads)
PASSED carbon step3-run: 3 rounds, 15 multi-thread batches (0 failures).
```

### C. Step 3 — Slice b Check (Root Rules vs Reference + Carbon `step3-run` Scenarios `1a..3, 5, 6`, 2 Rounds)
```bash
cd /mnt/data/projects/Softland && flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \
  clojure -Sdeps '{:aliases {:ac {:extra-paths ["src/proposal/runtime-2026-11-21/overnight-audit/auth-concurrent/src"]}}}' \
  -M:auth:ac -m softland.auth.module-harness slice-b 2
```
Output (summary lines):
```text
PASSED Slice b root rules (32-deep chain, :too-deep, 2-scan end-actor, take-out cascade, :driven-by, 2-seek who-is, live-only under) match reference 100%.
  [round 1..2] PASSED 1a, 1b, 2a, 2b, 3, 5, 6 (2..7 threads)
PASSED carbon step3-run: 2 rounds, 14 multi-thread batches (0 failures).
```

### D. Step 3 — Slice c Check (`:says` + Carbon `step3-run` All 9 Scenarios up to 8 Threads, 2 Rounds)
```bash
cd /mnt/data/projects/Softland && flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \
  clojure -Sdeps '{:aliases {:ac {:extra-paths ["src/proposal/runtime-2026-11-21/overnight-audit/auth-concurrent/src"]}}}' \
  -M:auth:ac -m softland.auth.module-harness slice-c 2
```
Output (summary lines):
```text
PASSED (6/6 direct $$accounts :says checks)
  [round 1..2] PASSED 1a, 1b, 2a, 2b, 3, 4, 5, 6, 7 (2..8 threads)
PASSED carbon step3-run: 2 rounds, 18 multi-thread batches (0 failures).
```

### E. Step 3 — Slice d Checks (`module-check` 100 Histories + Full `step3` with Live Watches + `:not-reached` + `:says`)
```bash
cd /mnt/data/projects/Softland && flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \
  bash -c 'clojure -M:auth -m softland.auth.module-check 42 100 softland.auth.module-harness/launch-module && clojure -Sdeps "{:aliases {:ac {:extra-paths [\"src/proposal/runtime-2026-11-21/overnight-audit/auth-concurrent/src\"]}}}" -M:auth:ac -m softland.auth.module-harness step3 2'
```
Output (all `PASSED`/`FAILED` lines verbatim):
```text
Seed of the histories: 42
The module: Rama module softland.auth.module/Auth via softland.auth.client
PASSED The module, Rama module softland.auth.module/Auth via softland.auth.client, gave what the reference gives, through the contract alone, in 100 generated histories of 7100 steps: every result of 6280 writes (728 of them repeats, 375 whose input does not fit), every one of 47607 reads of who-is, ask-of and under, and what each of 232 watches came to after each of the 309 changes under its root; with 25 deep segments of 32 agents in a line, 26 times :too-deep, and 0 calls sent again after a :could-not that said nothing.
(The module's histories took 74.7 s.)
PASSED open of a name that the cluster does not hold, and of a thing that is no cluster, gives {:could-not :find-the-module}.
PASSED After close!, each of the sixteen calls on that client gives {:could-not :closed}, watch-root's in place of the function; close! gives nil, also again; no function that it watched with is called again; and the other client goes on.

3 passed, 0 failed.
PASSED Refused enter returns :not-invited and leaves $$accounts[Acc] nil
PASSED Invite + first enter at t=200 stores :says={"name" "v200"} and :says-at=200
PASSED Equal :at=200 leaves kept :says unchanged (Sid's Call 2)
PASSED Earlier :at=100 leaves kept :says unchanged (Sid's Call 2)
PASSED Later :at=350 replaces kept :says and :says-at
PASSED Four concurrent returning enters (400, 700, 500, 600) keep :says of latest :at=700
PASSED Throwing callback still gets later calls and sibling watcher receives live updates without :driven-by
PASSED Re-entrant stop inside callback does not deadlock and stops after 1 call
PASSED destroy-module! delivers :softland.auth.client/not-reached exactly once within seconds (1842.8 ms)
  [round 1] PASSED 1a. two-invites-same-claim (2 threads)                     (2 threads)
  [round 1] PASSED 1b. invites-one-person-two-claims-and-triangle (4 threads) (4 threads)
  [round 1] PASSED 2a. two-enters-one-account-first-time (3 threads)          (3 threads)
  [round 1] PASSED 2b. returning-enters-vs-own-layer-made (5 threads)         (5 threads)
  [round 1] PASSED 3. two-accounts-on-one-claim (4 threads)                   (4 threads)
  [round 1] PASSED 4. two-answers-to-one-ask (6 threads)                      (6 threads)
  [round 1] PASSED 5. replace-secret-vs-end-sign-in (6 threads)               (6 threads)
  [round 1] PASSED 6. end-actor-vs-open-session-and-make-actor (7 threads)    (7 threads)
  [round 1] PASSED 7. full-8-thread-mixed-contention (8 threads)              (8 threads)
  [round 2] PASSED 1a. two-invites-same-claim (2 threads)                     (2 threads)
  [round 2] PASSED 1b. invites-one-person-two-claims-and-triangle (4 threads) (4 threads)
  [round 2] PASSED 2a. two-enters-one-account-first-time (3 threads)          (3 threads)
  [round 2] PASSED 2b. returning-enters-vs-own-layer-made (5 threads)         (5 threads)
  [round 2] PASSED 3. two-accounts-on-one-claim (4 threads)                   (4 threads)
  [round 2] PASSED 4. two-answers-to-one-ask (6 threads)                      (6 threads)
  [round 2] PASSED 5. replace-secret-vs-end-sign-in (6 threads)               (6 threads)
  [round 2] PASSED 6. end-actor-vs-open-session-and-make-actor (7 threads)    (7 threads)
  [round 2] PASSED 7. full-8-thread-mixed-contention (8 threads)              (8 threads)
PASSED carbon step3-run: 2 rounds, 18 multi-thread batches (0 failures).
```

### F. Step 3 — Full 1,000-History `module-check` Against `reference.clj`
```bash
cd /mnt/data/projects/Softland && flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \
  clojure -M:auth -m softland.auth.module-check 20261122 1000 softland.auth.module-harness/launch-module
```
Output (verbatim):
```text
Seed of the histories: 20261122
The module: Rama module softland.auth.module/Auth via softland.auth.client
PASSED The module, Rama module softland.auth.module/Auth via softland.auth.client, gave what the reference gives, through the contract alone, in 1000 generated histories of 80177 steps: every result of 70954 writes (9027 of them repeats, 4054 whose input does not fit), every one of 539354 reads of who-is, ask-of and under, and what each of 2770 watches came to after each of the 3739 changes under its root; with 250 deep segments of 32 agents in a line, 287 times :too-deep, and 0 calls sent again after a :could-not that said nothing.
(The module's histories took 774.1 s.)
PASSED open of a name that the cluster does not hold, and of a thing that is no cluster, gives {:could-not :find-the-module}.
PASSED After close!, each of the sixteen calls on that client gives {:could-not :closed}, watch-root's in place of the function; close! gives nil, also again; no function that it watched with is called again; and the other client goes on.

3 passed, 0 failed.
```

### G. Step 4 — Full-Life Cluster Measurements
```bash
cd /mnt/data/projects/Softland && flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \
  clojure -M:auth -m softland.auth.full-life-measure
```
Output (verbatim):
```text
=== MEASURE 1: who-is (1 RT, 1 task, 2-seek fast path, flat across history) ===
  Static query check: leading (|hash *root), 0 other partitioners, 0 range scans = true
  Seeded 10,000 closed sessions + 500 ended sign-ins + 100 ended agents in 9025.1 ms
  Exact $$root-data seeks at 10 closed sessions: {:http [2 {:g 1, :r 0, :s 1, :a 0}], :conn [1 {:g 1, :r 0, :s 0, :a 0}], :ended-agent-with-s [3 {:g 1, :r 0, :s 1, :a 1}], :ended-agent-no-s [2 {:g 1, :r 0, :s 0, :a 1}], :unknown-root [2 {:g 1, :r 1, :s 0, :a 0}]}
  Exact $$root-data seeks at 10,000 closed sessions: {:http [2 {:g 1, :r 0, :s 1, :a 0}], :conn [1 {:g 1, :r 0, :s 0, :a 0}], :ended-agent-with-s [3 {:g 1, :r 0, :s 1, :a 1}], :ended-agent-no-s [2 {:g 1, :r 0, :s 0, :a 1}], :unknown-root [2 {:g 1, :r 1, :s 0, :a 0}]}
  who-is latency (200 calls): 10 sessions p50=0.829 ms p95=1.340 ms | 10,000 sessions p50=0.204 ms p95=0.389 ms
PASSED Measure 1: who-is 1 RT, 1 task, 2-seek fast path, seeks constant across 10 -> 10,000 history.

=== MEASURE 2: 20,000 roots watched simultaneously on one task ===
  Registered 20000/20000 watches on task 0 in 522.1 ms (not-reached=0)
  Live push while 20,000 active: root #0=true, root #19,999=true; close! took 42.1 ms
PASSED Measure 2: 20,000 roots watched simultaneously on one task.

=== MEASURE 3: end-actor under 3,000 actors, 2,000 sign-ins, 5,000 sessions ===
  Seeded 3,000 actors + 2,000 sign-ins + 5,000 sessions via contract in 7579.3 ms
  end-actor wall time = 300.6 ms (Revision 2 bound < 2,000 ms; ping-task ceiling = 8,000 ms)
  Concurrent sibling who-is(P-sib) = 290.6 ms (ok=true)
  Sibling watchers :not-reached = 0 / 20 | heavy watcher :not-reached = 0 | heavy cleared = true
  Post-check: under actors=3000 (all ended=true), live sign-ins=0; depth-32 who-is ended=true actor-ended=true session-closed=true driven-by={:kind :model, :model "worker"}
PASSED Measure 3: heavy end-actor within Revision 2 bound and 0 sibling :not-reached.

=== MEASURE 4: No write and no read of one person goes through a shared task ===
  Static audit: :global?/|global/|all present=false, |direct only in ping-task=true
  Single person depot partition deltas across 4 tasks: {:root [4 0 0 0], :inv [0 0 0 1], :ent [0 1 0 0], :ask [0 0 0 2]}
  40 distinct persons depot partition counts across 4 tasks: {:root [44 40 32 44], :inv [7 5 14 14], :ent [9 10 13 8], :ask [20 8 32 20]}
PASSED Measure 4: no write and no read of one person passes through a shared task.

STEP 4 Summary: m1=true m2=true m3=true m4=true (ALL_PASSED=true, elapsed=35.3 s)
```

### H. Step 5 — Docstring Audit
```bash
cd /mnt/data/projects/Softland && clojure -M:auth -e '...'
```
Output:
```text
Checked 6 files; missing docstrings: 0
```

---

## 4. What is open

1. **Multi-node real cluster / kill-`9` worker failover:** All tests ran on Rama 1.6.0 `InProcessCluster` (1 to 4 tasks, 2–8 client threads, `destroy-module!` for `:not-reached`). Real multi-JVM network partitions, worker kill-`9` mid-hop, and leader failover have not yet been exercised on a distributed cluster.
2. **PII erasure & depot thinning:** Per `SPEC.md`, no erasure API, `depot-migration`, or `depot.max.entries.per.partition` trim option is built in this pass; the layout keeps PII isolated (`*invites`, `*enters`, `$$accounts`, email keys of `$$claims`, and erasure pointer fields on `$$root-data["<P>\0r"]`) so future erasure needs no schema change.
3. **Depth 32 vs Store `act.clj`'s 31 delegated makers (`PLAN_VALIDATION.md` Revision 7):** Closed for this build per Sid's instruction (Auth depth stays at `32` makers below root, matching `SPEC.md` and `reference.clj`). If the runtime ever maps every Auth agent directly 1-to-1 to a nested Store `:permission-id`, depth-32 agents would hit `act.clj`'s 32-vertex cap unless one side is changed later.
4. **Isolated single-proxy failure on an otherwise healthy task (`PLAN.md` Wall 2):** `ping-task` + 5 s open timeout catch full module/worker/task outages within ~2–8 s; if a single `foreign-proxy` errors repeatedly on a healthy task, termination timing depends on the caller's `RamaClusterManager` config (`foreign.proxy.failure.window.seconds` / `threshold`).
5. **Findings from carbon's independent 2-fault check (`15a90b0f`, `PLAN_VALIDATION.md` Step 3):** Carbon confirmed 0 breaks across all 32 agreement scenarios + `TF-02..TF-04` at both 1 fault (`97,307` states) and 2 faults in one history (`> 1,157,000` states), and flagged three subtle uncheckpointed-replay edge cases as `minor-fail` (left open for Sid's call rather than adding per-event dedup tax on `*root-events` in this build):
   - Cross-depot `:all-after` replay of a refused `*root-events` write (e.g., refused `own-layer-made` replaying after a cross-depot `enter` commits `RootEntered` — `TF-01`);
   - Standalone linearizability corner when `own-layer-made` races first-time `enter` between Steps E4 and E5 while `enter` crashes before Step E5 (`TF-01`, unreachable in the runtime spine which calls `own-layer-made` only after `enter` returns `:own-layer-made? false`);
   - Transient backward mutation during `:all-after` replay of a `replace-secret` 2-cycle (`H2 -> H1 -> H2`) on `*root-events`;
   - Multi-year growth of `$$enter-ops` if uninvited `enter` floods reach Auth before runtime pacing.
   (Note: in `src-auth/`, `op-id` is minted on the client before `foreign-append!`, `watch-root` strips all hidden fields from `$$live` and suppresses value-equal post-open `ResyncDiff`s, and `end-actor` uses flat `<<atomic` + `ops/explode` verified at 3,000/2,000/5,000 in 300.6 ms — closing carbon's blind-spot items 1, 5, and 6.)

---

## 5. Choices made in this build (each Sid's to turn)

1. **Client blocking operation timeout (`10 s`)** in `softland.auth.client` (`default-timeout-ms = 10000`) for depot appends, query topologies, and `ask-of` before returning `{:could-not :reach-the-module}`.
2. **`watch-root` open timeout (`5 s`)** (`proxy-open-timeout-ms = 5000`): `watch-root` awaits server subscription registration up to 5 s before returning `stop` (closing the register-before-`who-is` blind window), delivering `:softland.auth.client/not-reached` asynchronously on failure/timeout.
3. **`ping-task` liveness monitor timings (`2 s` cadence, `5 s` ping timeout, `3 s` partial-outage probe = `8 s` ceiling)**: delivers `:not-reached` in `1.84 s` on `destroy-module!` while staying 26× above worst-case 3,000/2,000/5,000 `end-actor` (`300.6 ms`).
4. **Bijective String keys for `$$accounts` and `$$claims`** (`"<len(p)>:<p>:<subject>"`, `"e:<email>"`, `"c:<hash>"`) rather than `defrecord` map keys.
5. **Carrying immutable `:driven-by` on `LiveSession` and full `SignInRec` fields on `LiveSignIn` in `$$live`** (strictly omitted by `watch-root`'s client projection) to eliminate `ch:`, `ag:`, and `sg:` indexes and keep `end-actor` at 3 seeks (`300.6 ms` at 3,000/2,000/5,000).
6. **Recording `EnterOpFailed` in `$$enter-ops[op-id]` on refused enters only** (zero writes on successful enters, zero PII, `$$accounts[Acc]` left `nil` on refusal) and recording `AskOutcomeRefused(:no-such-ask)` in `ok:<op>` on Step 1b to close `:all-after` stream replay of refused writes.
7. **No per-event dedup ring buffer on `*root-events` (`PLAN.md` Wall 3):** Accepted the backward-clock-skew-exceeds-session-lifetime crash window under `SPEC.md`'s NTP clock-keeping rule rather than paying +1 seek / +1 write (+33% I/O) on every `open-session` and `replace-secret`.
8. **Born-over `ask` (`:ends <= :at`) persisted as `:born-over? true`** on `$$asks[U]` so any later `answer-ask` returns `:over` regardless of caller clock, while `ask-of` keeps returning `:state :waiting` with `:ends`.
9. **Internal Rama names:** Depots `*root-events`, `*asks`, `*invites`, `*enters`; PStates `$$root-data`, `$$live`, `$$accounts`, `$$claims`, `$$asks`, `$$enter-ops`; stream topology `"auth"`; query topologies `"who-is"`, `"under"`, `"ping-task"`. None cross `softland.auth.client`.
10. **Rama's `depot.ack.failure.on.any.streaming.failure` left at default (`true`)**: first-attempt stream failures surface `{:could-not :reach-the-module}` and rely on the contract's idempotent retry.
