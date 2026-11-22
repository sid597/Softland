# Build: Auth (`src-auth/` + `softland.auth.client`)

Complete build record of the Rama module `Auth` (`softland.auth.module/Auth`) and its foreign client (`softland.auth.client`), implementing all 18 calls of [SPEC.md](SPEC.md) (including Sid's calls of 22 November) and the revised [PLAN.md](PLAN.md) (Revisions 1–6 and 8 of [PLAN_VALIDATION.md](PLAN_VALIDATION.md); Revision 7 closed for this build at depth 32).

---

## 1. What was built

### Step 1 & Step 2 — Revised Plan and Model-Checked Cross-Key Agreement (`7b45edc0`)
*(What was model-checked is this writing of the agreement, `plan_steps.clj`, and not `src-auth/softland/auth/schema.clj`, which is a second writing of the same steps: see §6.)*
- **`src/proposal/runtime-2026-11-21/spine/auth/PLAN.md`**: Applied Revisions 1–6 and 8 of `PLAN_VALIDATION.md` in order (Revision 7 recorded as closed at 32 makers below root; live sign-in order kept in `$$live` so `under` never walks ended sign-ins).
- **`src/proposal/runtime-2026-11-21/spine/auth/agreement/`**:
  - `src/auth_concurrent/plan_steps.clj` — Revised single-task step transitions for `invite`, `enter`, `answer-ask`, 2-seek `who-is`, and 2-scan `end-actor` in the exact shape of carbon's `plan_steps.clj`.
  - `src/agreement/check.clj` — Runner driving carbon's unchanged `model_check.clj`, `spec_oracle.clj`, and `step1_run.clj` across all 25 carbon scenarios + 7 extra adversarial scenarios (`X-01..X-07`) × 4 fault/retry modes = **128 runs** (`88,261` states, `539` quiescent terminal histories, client re-runs checked in all 4 modes, **0 capped runs, 0 breaks across all 5 questions**).
  - `README.md`, `RESULTS.md`.

### Step 3 — Module and Client (`src-auth/`), Built Slice by Slice
- **Slice a (`374468ee`) — Module frame, `invite`, `enter`, `own-layer-made`:**
  - `src-auth/softland/auth/fit.clj` — Pure input-form validation for all 18 calls (Sid's Call 1: `contains?` on every `-or-nil` key, `:driven-by` alone optional, exact keys on top-level and sub-maps, `Locale/ROOT` lower-case emails, `fn?` on `watch-root`, fixed-width int coerced to `Long`, throwing-comparator safety).
  - `src-auth/softland/auth/schema.clj` — Typed Java marker interfaces (`IRootDatum`, `ILiveEntry`, `IAccountState`, `IClaimState`, `IAskState`, `IEnterOpState`, `IDrivenBy`, `IRootEvent`, `IAskEvent`), strictly typed `defrecord`s (zero `Object` positions, zero byte arrays), 7-bit ASCII key encoders (length-prefixed bijective `account-key`, prefix-disjoint `claim-key`, order-preserving `hex-ts`/`hex16`), read projections, and pure single-task transition functions.
  - `src-auth/softland/auth/module.clj` — `defmodule Auth`: 4 partitioned depots (`*root-events`, `*asks`, `*invites`, `*enters`), 6 partitioned PStates (`$$root-data`, `$$live` with `pstate.proxy.limit = 20000` — nested by root since §6: under each root a subindexed map keyed `"g:<G>"` / `"s:<S>"` — `$$accounts`, `$$claims`, `$$asks`, `$$enter-ops`), single stream topology `"auth"` (`{:start-from :beginning :retry-mode :all-after}`; since §6 each depot's dataflow is a function of its own that first refuses a record of another form), and 3 query topologies (`"who-is"`, `"under"`, `"ping-task"`).
  - `src-auth/softland/auth/client.clj` — Public contract namespace `softland.auth.client`.
  - `test-auth/softland/auth/module_harness.clj` — `launch-module` (0-arg factory for `softland.auth.module-check`), direct `$$accounts` `:says` table check, `:not-reached` / callback-isolation check, and slice runner invoking carbon's unchanged `auth-concurrent.step3-run`.
- **Slice b (`25d3b5be`) — Root writes, take-out, 3-seek 2-scan `end-actor`, 2-seek `who-is`, live-only `under`:**
  - All 7 `:root` writes (`begin-sign-in`, `replace-secret`, `end-sign-in`, `make-actor`, `end-actor`, `open-session`, `close-session`) on `*root-events` (`0` hops, atomic single-task batches).
  - `take-out-on-root!` (1-seek `exp:` scan fast path when nothing expired; atomic `$$live` cascade when expired) and Revision 2's 3-seek 2-scan `end-actor` (`a:<A>` + forward `ao:` scan + `$$live` scan; since §6 `ao:` holds live agents alone, and an ended agent's order record moves to `ae:`) applied via flat `<<atomic` + `ops/explode` (O(1) JVM stack depth, zero `ch:`/`ag:`/`sg:` indexes).
  - Revision 3's 2-seek `who-is` fast path (`g:<G>` read first, skipping `"<P>\0r"` when `:own-layer-made-at-begin?` is `true`).
  - *(Operational note on commit `25d3b5be`: another concurrent session had already staged files in `gate-histories/` and `store-piece-2/SPEC.md` in `.git/index` right before `git commit` ran, so `25d3b5be` picked up those already-staged files alongside our 4 files. Starting from Slice c onward, every commit used `git commit --only -- <our-files>` to stay completely isolated from `.git/index`.)*
- **Slice c (`23eb936d`) — `ask`, `answer-ask`, `ask-of`:**
  - `ask` (0 hops on `*asks`), Revision 4 `answer-ask` (`ok:<op>` checked BEFORE `take-out` in Step 2; Step 1b `:no-such-ask` replay guard; unconditional helper take-out on `(:root self-input)` at `(:at self-input)` in Step 3), and 1-seek direct `foreign-select-one-async` `ask-of`.
- **Slice d (`79f63722`) — `watch-root`, `open`, `close!`, `:softland.auth.client/not-reached`:**
  - Shared `foreign-proxy-async` per `(client, root)` on that root's own map in `$$live` (since §6; before it, a range proxy on the flat map), incremental `Diff$Processor` / `KeyDiff$Processor` / `KeyRemoveDiff$Processor` projection (omitting `:driven-by`), initial `ResyncDiff` and `DestroyedDiff` suppression, per-watcher turn locks + `ThreadLocal` re-entrancy guard (preventing deadlocks when a callback calls `stop` or `close!`), exception isolation per watcher, and background `"ping-task"` liveness monitor (2 s cadence, 5 s timeout + 3 s partial-outage probe = 8 s outage ceiling; since §6 the probe of every watched root runs at once against one 3 s deadline, and a proxy that opens after `watch-root`'s wait is closed when it comes) delivering `:softland.auth.client/not-reached` exactly once within seconds of module/task loss.

### Step 4 & Step 5 — Full-Life Measurements and Self-Explaining Tree
- **`test-auth/softland/auth/full_life_measure.clj`** — The STEP 4 full-life cluster measurements on live Rama 1.6.0 `InProcessCluster`s (four at this step; §6 times the writes in Measure 2 and adds Measure 3b).
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
6. **After the review pass of 23 November ([§6](#6-the-review-pass-of-23-november-2026)):** review points 2, 5, 6 and 8 and three of its Low items are repaired there; what it leaves open (the model check of the steps that run, the rows kept for good, the replay dedup that Sid set aside) is listed at its end.

---

## 5. Choices made in this build (each Sid's to turn)

1. **Client blocking operation timeout (`10 s`)** in `softland.auth.client` (`default-timeout-ms = 10000`) for depot appends, query topologies, and `ask-of` before returning `{:could-not :reach-the-module}`.
2. **`watch-root` open timeout (`5 s`)** (`proxy-open-timeout-ms = 5000`): `watch-root` awaits server subscription registration up to 5 s before returning `stop` (closing the register-before-`who-is` blind window), delivering `:softland.auth.client/not-reached` asynchronously on failure/timeout (since §6 on a virtual thread of its own, and closing the proxy that Rama finishes opening after the wait).
3. **`ping-task` liveness monitor timings (`2 s` cadence, `5 s` ping timeout, `3 s` partial-outage probe = `8 s` ceiling; since §6 the 3 s is one deadline for the probes of every watched root at once)**: delivers `:not-reached` in `1.84 s` on `destroy-module!` while staying 26× above worst-case 3,000/2,000/5,000 `end-actor` (`300.6 ms`).
4. **Bijective String keys for `$$accounts` and `$$claims`** (`"<len(p)>:<p>:<subject>"`, `"e:<email>"`, `"c:<hash>"`) rather than `defrecord` map keys.
5. **Carrying immutable `:driven-by` on `LiveSession` and full `SignInRec` fields on `LiveSignIn` in `$$live`** (strictly omitted by `watch-root`'s client projection) to eliminate `ch:`, `ag:`, and `sg:` indexes and keep `end-actor` at 3 seeks (`300.6 ms` at 3,000/2,000/5,000).
6. **Recording `EnterOpFailed` in `$$enter-ops[op-id]` on refused enters only** (zero writes on successful enters, zero PII, `$$accounts[Acc]` left `nil` on refusal) and recording `AskOutcomeRefused(:no-such-ask)` in `ok:<op>` on Step 1b to close `:all-after` stream replay of refused writes.
7. **No per-event dedup ring buffer on `*root-events` (`PLAN.md` Wall 3):** Accepted the backward-clock-skew-exceeds-session-lifetime crash window under `SPEC.md`'s NTP clock-keeping rule rather than paying +1 seek / +1 write (+33% I/O) on every `open-session` and `replace-secret`.
8. **Born-over `ask` (`:ends <= :at`) persisted as `:born-over? true`** on `$$asks[U]` so any later `answer-ask` returns `:over` regardless of caller clock, while `ask-of` keeps returning `:state :waiting` with `:ends`.
9. **Internal Rama names:** Depots `*root-events`, `*asks`, `*invites`, `*enters`; PStates `$$root-data`, `$$live`, `$$accounts`, `$$claims`, `$$asks`, `$$enter-ops`; stream topology `"auth"`; query topologies `"who-is"`, `"under"`, `"ping-task"`. None cross `softland.auth.client`.
10. **Rama's `depot.ack.failure.on.any.streaming.failure` left at default (`true`)**: first-attempt stream failures surface `{:could-not :reach-the-module}` and rely on the contract's idempotent retry.

---

## 6. The review pass of 23 November 2026

The review of 22 November (its points 2, 5, 6, 8 and 9, and its Low items on Auth) found these in `src-auth/`; the runtime's and the command line's points are on [the spine's page](../BUILD.md). Sid's go was for the whole job, with three things left out of this pass: the `.cljc` forms, the firewalling of 8a's ports, and the dedup of replays on `*root-events`. The contract of `softland.auth.client` does not change, but for one rule of form (`a3941d66` below). Every run was behind `flock …/before-real-data/runs/cluster.lock`.

### What changed, by commit

| Commit | Point | What changed |
|---|---|---|
| `42e4e786` | 2 | `$$live` is nested by root: `{UUID (map-schema String ILiveEntry {:subindex-options {:track-size? false}})}`, inner keys `"g:<G>"` / `"s:<S>"` (`schema.clj` `lk-signin`, `lk-session`, `live-lo`…`live-session-hi`). A watch is a proxy at `[(keypath root) (sorted-map-range …)]`, on that root's map alone, so a write is matched against its own root's watches and no other; it still gets `KeyDiff`s. `apply-root-plan!` takes the root; every plan writes inner keys; take-out, `end-actor`, `replace-secret`, `end-sign-in` and `under` read the root's map. Measure 2 now times `open-session`, `close-session` and a `who-is` right behind them, with 0 and with 20,000 roots watched on one task. |
| `444e391c` | 5, 6 | A proxy that Rama finishes opening after `watch-root`'s 5 s wait is closed when it comes (`whenCompleteAsync` on the pending open, `client.clj`), so a stall with reconnecting clients cannot fill `pstate.proxy.limit`. In a partial outage every watched root is probed at once (`probe-root`) against one 3 s deadline (`unreached-roots`), not one root after another; a failed opening is told `:not-reached` on a virtual thread of its own, not on the sweep's thread. `module_harness` gains `late-open` and `partial-outage`. |
| `c1b5554f` | Low: no form check in the topology | Each depot's dataflow first holds the record against the client's form (`schema.clj` `invite-record-fits?`, `enter-record-fits?`, `ask-record-fits?`, `root-record-fits?`: the type for that depot, each field of the type the client puts there); a record that does not fit is acked `{:could-not :read-the-event}` and changes nothing, so it cannot throw in a step and hold its partition under `:all-after`. Each depot's dataflow is a function of its own (`invites-source!` … `asks-source!`): with the four gates in one `defmodule` body the module was more than one JVM method can hold ("Method code too large"). `module_harness` gains `unfit-record`. |
| `a3941d66` | 8 (Sid's call) | **The build's default, for Sid to turn:** a string that UTF-8 cannot carry (a surrogate `char` without its pair) does not fit wherever the contract takes a string — a text, a code, a provider's name, a key or value of `:says` — and gives `{:could-not :read-the-event}` with nothing appended (`fit.clj` `carried?`). The reference holds the same rule (`reference.clj`), so the two agree; `SPEC.md` says it in one bullet; `histories.clj` draws such inputs for every write that takes a string (209 in 200 histories of seed 42, each refused by the reference); `reference_proof` gains one case. |
| `68ba4e07` | Low: `end-actor` | `ao:` holds live agents alone; `end-actor` moves an ended agent's order record to `ae:` (`schema.clj` `k-ae`), so its scan reads the live agents made after the one it ends and none that ended before. Every live descendant is still reached: a live agent's makers are all live. `under` reads both ranges (one more seek) and merges them by `:seq`; its result does not change. Ending an agent costs one more write (the delete of its `ao:` record). `full_life_measure` gains Measure 3b. |
| `dc9142ff` | 9 | `module.clj` and `schema.clj` say what holds: the protocol was model-checked as `spine/auth/agreement/src/auth_concurrent/plan_steps.clj`; `schema.clj` is a second writing of the same steps, held to the reference by `module-check` on a healthy module, where no hop has failed and no record has been replayed. |

### Each check against the code before it (red, then green)

Each "before" ran against a snapshot of `src-auth` taken from git (`git archive` of the commit named, into scratch outside the repository): the two measures with this pass's `full_life_measure.clj`, and the three harness checks as a scratch copy of their steps (their harness names functions that the older client does not have). Each "after" is the committed code. The numbers of Measures 2, 3 and 3b here are from those paired runs; the final suite's own are in its output below.

| Check | Before the fix | After |
|---|---|---|
| Measure 2, 20,000 roots watched on one task, writes under another root (p50, ms) | HEAD `4c096eab`: open 3.420 → **8.745**, close 3.345 → **13.246**, who-is right after 0.454 → **4.785**; FAILED | open 3.380 → 3.292, close 3.375 → 3.292, who-is 0.433 → 0.329; PASSED |
| `late-open`: 21,000 watches with the wait at 0 ms, then a normal watch | HEAD: the normal watch is told `:not-reached` and never of the new session; Rama logs `Number of proxies on PStates exceeds pstate.proxy.limit` (1,001 lines) | told of the new session, never `:not-reached`; no such line |
| `partial-outage`: tasks 0 and 1 unreached, 20 of 40 roots unanswered | HEAD (its `probe-root-alive?` stubbed to wait its 3 s): swept in **60,040 ms** | swept in 3,034 ms; the 20 told once each, the other 20 nothing |
| `unfit-record`: a begin on `*root-events` whose `:until` is a text, then an `open-session` of the same root | HEAD: the record throws in the topology (ExecutionException at the ack, 32 ClassCastException traces as it is tried again), and the `open-session` behind it gives `{:could-not :reach-the-module}` | the record gives `{:could-not :read-the-event}`, the `open-session` is taken; the same on `*invites`, `*enters`, `*asks` |
| Measure 3b, mean `end-actor` of 100 agents (ms) | `a3941d66`: no ended history 2.446, 3,000 ended agents after them **6.396**; FAILED | 2.498 and 2.638; PASSED |
| Measure 3, `end-actor` of 3,000 live agents, 2,000 sign-ins, 5,000 sessions | `a3941d66`: 314.4 ms, sibling's `who-is` during it 309.1 ms | 310.4 ms, 311.1 ms (unchanged: every agent there is live, so all of them are the work) |

### Commands run and their outputs

Every run from `/mnt/data/projects/Softland`, after the last commit of code above (`dc9142ff`), one after another in one script behind the lock:

```bash
LOCK=/mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock
AC='{:aliases {:ac {:extra-paths ["src/proposal/runtime-2026-11-21/overnight-audit/auth-concurrent/src"]}}}'
flock "$LOCK" bash -c "
  clojure -M:auth -m softland.auth.reference-proof 42
  clojure -M:auth -m softland.auth.module-check 42 200 softland.auth.module-harness/launch-module
  clojure -M:auth -m softland.auth.module-harness late-open
  clojure -M:auth -m softland.auth.module-harness partial-outage
  clojure -M:auth -m softland.auth.module-harness unfit-record
  clojure -Sdeps \"$AC\" -M:auth:ac -m softland.auth.module-harness slice-a 3
  clojure -Sdeps \"$AC\" -M:auth:ac -m softland.auth.module-harness slice-b 2
  clojure -Sdeps \"$AC\" -M:auth:ac -m softland.auth.module-harness slice-c 2
  clojure -Sdeps \"$AC\" -M:auth:ac -m softland.auth.module-harness step3 2
  clojure -M:auth -m softland.auth.full-life-measure"
```

Then `reference-proof` once more, after the citation of question 29 was added to the reference's docstring and to its proof's sentence (no rule changed): **191 passed, 0 failed**, the same.

**`reference-proof`** — exit 0 (9 s):
```text
191 passed, 0 failed.
(191 PASSED lines, each a sentence of reference_proof.clj; the one added in this pass, as the second run printed it:)
PASSED A string that UTF-8 cannot carry, one that holds a surrogate character without its pair, does not fit wherever the contract takes a string (QUESTIONS.md, 29: the build's default of 23 November, for Sid to turn): a subject, a provider's name, an email, a key and a value of :says, a label, an ask's :asks and code, and a :model; a pair that stands together fits.
```

**`module-check-200`** — exit 0 (182 s):
```text
Seed of the histories: 42
The module: Rama module softland.auth.module/Auth via softland.auth.client
PASSED The module, Rama module softland.auth.module/Auth via softland.auth.client, gave what the reference gives, through the contract alone, in 200 generated histories of 16234 steps: every result of 14295 writes (1717 of them repeats, 862 whose input does not fit), every one of 108697 reads of who-is, ask-of and under, and what each of 564 watches came to after each of the 777 changes under its root; with 50 deep segments of 32 agents in a line, 52 times :too-deep, and 0 calls sent again after a :could-not that said nothing.
(The module's histories took 170.7 s.)
PASSED open of a name that the cluster does not hold, and of a thing that is no cluster, gives {:could-not :find-the-module}.
PASSED After close!, each of the sixteen calls on that client gives {:could-not :closed}, watch-root's in place of the function; close! gives nil, also again; no function that it watched with is called again; and the other client goes on.
3 passed, 0 failed.
```

**`late-open`** — exit 0 (20 s):
```text
PASSED Before: a normal watch of a root is told of a new session and never :not-reached.
PASSED With watch-root's wait forced to 0 ms, 21,000 watches on one task (more than pstate.proxy.limit, 20,000), each told :not-reached or given a watch, are stopped by their callers.
PASSED After them, with the wait back: a normal watch of a root is still told of a new session and never :not-reached, so the proxies that opened after their wait were closed and none is left counting against the limit.
```

**`partial-outage`** — exit 0 (13 s):
```text
PASSED A partial outage (tasks 0 and 1 unreached, 20 of 40 watched roots unanswered) is swept in 3039.3 ms: within 6 s, one probe deadline and a margin, however many roots are watched.
PASSED Each watcher of an unreached root is told :not-reached once, and the watchers of the roots that answered are told nothing.
```

**`unfit-record`** — exit 0 (11 s):
```text
PASSED A record on *root-events of Auth's own type with a field of another type, a map of no type of Auth's, and a thing that is no map are each refused with {:could-not :read-the-event}; and after each, a write of the same root on the same partition is taken.
PASSED A record on *invites, on *enters and on *asks with a field of another type is refused with {:could-not :read-the-event}; and after each, a write of the same claim, account or code is taken.
```

**`slice-a`** — exit 0 (104 s):
```text
PASSED Refused enter returns :not-invited and leaves $$accounts[Acc] nil
PASSED Invite + first enter at t=200 stores :says={"name" "v200"} and :says-at=200
PASSED Equal :at=200 leaves kept :says unchanged (Sid's Call 2)
PASSED Earlier :at=100 leaves kept :says unchanged (Sid's Call 2)
PASSED Later :at=350 replaces kept :says and :says-at
PASSED Four concurrent returning enters (400, 700, 500, 600) keep :says of latest :at=700
  [round 1] PASSED 1a. two-invites-same-claim (2 threads)                     (2 threads)
  [round 1] PASSED 1b. invites-one-person-two-claims-and-triangle (4 threads) (4 threads)
  [round 1] PASSED 2a. two-enters-one-account-first-time (3 threads)          (3 threads)
  [round 1] PASSED 2b. returning-enters-vs-own-layer-made (5 threads)         (5 threads)
  [round 1] PASSED 3. two-accounts-on-one-claim (4 threads)                   (4 threads)
  [round 2] PASSED 1a. two-invites-same-claim (2 threads)                     (2 threads)
  [round 2] PASSED 1b. invites-one-person-two-claims-and-triangle (4 threads) (4 threads)
  [round 2] PASSED 2a. two-enters-one-account-first-time (3 threads)          (3 threads)
  [round 2] PASSED 2b. returning-enters-vs-own-layer-made (5 threads)         (5 threads)
  [round 2] PASSED 3. two-accounts-on-one-claim (4 threads)                   (4 threads)
  [round 3] PASSED 1a. two-invites-same-claim (2 threads)                     (2 threads)
  [round 3] PASSED 1b. invites-one-person-two-claims-and-triangle (4 threads) (4 threads)
  [round 3] PASSED 2a. two-enters-one-account-first-time (3 threads)          (3 threads)
  [round 3] PASSED 2b. returning-enters-vs-own-layer-made (5 threads)         (5 threads)
  [round 3] PASSED 3. two-accounts-on-one-claim (4 threads)                   (4 threads)
PASSED carbon step3-run: 3 rounds, 15 multi-thread batches (0 failures).
```

**`slice-b`** — exit 0 (95 s):
```text
PASSED Slice b root rules (32-deep chain, :too-deep, 2-scan end-actor, take-out cascade, :driven-by, 2-seek who-is, live-only under) match reference 100%.
  [round 1] PASSED 1a. two-invites-same-claim (2 threads)                     (2 threads)
  [round 1] PASSED 1b. invites-one-person-two-claims-and-triangle (4 threads) (4 threads)
  [round 1] PASSED 2a. two-enters-one-account-first-time (3 threads)          (3 threads)
  [round 1] PASSED 2b. returning-enters-vs-own-layer-made (5 threads)         (5 threads)
  [round 1] PASSED 3. two-accounts-on-one-claim (4 threads)                   (4 threads)
  [round 1] PASSED 5. replace-secret-vs-end-sign-in (6 threads)               (6 threads)
  [round 1] PASSED 6. end-actor-vs-open-session-and-make-actor (7 threads)    (7 threads)
  [round 2] PASSED 1a. two-invites-same-claim (2 threads)                     (2 threads)
  [round 2] PASSED 1b. invites-one-person-two-claims-and-triangle (4 threads) (4 threads)
  [round 2] PASSED 2a. two-enters-one-account-first-time (3 threads)          (3 threads)
  [round 2] PASSED 2b. returning-enters-vs-own-layer-made (5 threads)         (5 threads)
  [round 2] PASSED 3. two-accounts-on-one-claim (4 threads)                   (4 threads)
  [round 2] PASSED 5. replace-secret-vs-end-sign-in (6 threads)               (6 threads)
  [round 2] PASSED 6. end-actor-vs-open-session-and-make-actor (7 threads)    (7 threads)
PASSED carbon step3-run: 2 rounds, 14 multi-thread batches (0 failures).
```

**`slice-c`** — exit 0 (162 s):
```text
PASSED Refused enter returns :not-invited and leaves $$accounts[Acc] nil
PASSED Invite + first enter at t=200 stores :says={"name" "v200"} and :says-at=200
PASSED Equal :at=200 leaves kept :says unchanged (Sid's Call 2)
PASSED Earlier :at=100 leaves kept :says unchanged (Sid's Call 2)
PASSED Later :at=350 replaces kept :says and :says-at
PASSED Four concurrent returning enters (400, 700, 500, 600) keep :says of latest :at=700
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

**`step3`** — exit 0 (212 s):
```text
PASSED Refused enter returns :not-invited and leaves $$accounts[Acc] nil
PASSED Invite + first enter at t=200 stores :says={"name" "v200"} and :says-at=200
PASSED Equal :at=200 leaves kept :says unchanged (Sid's Call 2)
PASSED Earlier :at=100 leaves kept :says unchanged (Sid's Call 2)
PASSED Later :at=350 replaces kept :says and :says-at
PASSED Four concurrent returning enters (400, 700, 500, 600) keep :says of latest :at=700
PASSED Throwing callback still gets later calls and sibling watcher receives live updates without :driven-by
PASSED Re-entrant stop inside callback does not deadlock and stops after 1 call
PASSED destroy-module! delivers :softland.auth.client/not-reached exactly once within seconds (1833.8 ms)
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

**`full-life-measure`** — exit 0 (64 s):
```text
=== MEASURE 1: who-is (1 RT, 1 task, 2-seek fast path, flat across history) ===
  Static query check: leading (|hash *root), 0 other partitioners, 0 range scans = true
  Seeded 10,000 closed sessions + 500 ended sign-ins + 100 ended agents in 8812.9 ms
  Exact $$root-data seeks at 10 closed sessions: {:http [2 {:g 1, :r 0, :s 1, :a 0}], :conn [1 {:g 1, :r 0, :s 0, :a 0}], :ended-agent-with-s [3 {:g 1, :r 0, :s 1, :a 1}], :ended-agent-no-s [2 {:g 1, :r 0, :s 0, :a 1}], :unknown-root [2 {:g 1, :r 1, :s 0, :a 0}]}
  Exact $$root-data seeks at 10,000 closed sessions: {:http [2 {:g 1, :r 0, :s 1, :a 0}], :conn [1 {:g 1, :r 0, :s 0, :a 0}], :ended-agent-with-s [3 {:g 1, :r 0, :s 1, :a 1}], :ended-agent-no-s [2 {:g 1, :r 0, :s 0, :a 1}], :unknown-root [2 {:g 1, :r 1, :s 0, :a 0}]}
  who-is latency (200 calls): 10 sessions p50=0.788 ms p95=1.371 ms | 10,000 sessions p50=0.184 ms p95=0.392 ms
PASSED Measure 1: who-is 1 RT, 1 task, 2-seek fast path, seeks constant across 10 -> 10,000 history.
=== MEASURE 2: 20,000 roots watched simultaneously on one task ===
  0 roots watched:         open-session p50=2.900 p95=3.386 | close-session p50=2.880 p95=3.302 | who-is right after p50=0.271 p95=0.470 ms
  20,000 roots watched:    open-session p50=2.853 p95=3.334 | close-session p50=2.824 p95=3.293 | who-is right after p50=0.228 p95=0.427 ms
  Registered 20000/20000 watches on task 0 in 407.7 ms (not-reached=0)
  Live push while 20,000 active: root #0=true, root #19,999=true; close! took 34.9 ms
  Writes under another root with 20,000 watched, each p50 within 2x + 2 ms of none watched: true
PASSED Measure 2: 20,000 roots watched simultaneously on one task, and a write under another root and a who-is right behind it cost what they cost with none watched.
=== MEASURE 3: end-actor under 3,000 actors, 2,000 sign-ins, 5,000 sessions ===
  Seeded 3,000 actors + 2,000 sign-ins + 5,000 sessions via contract in 6954.5 ms
  end-actor wall time = 287.2 ms (Revision 2 bound < 2,000 ms; ping-task ceiling = 8,000 ms)
  Concurrent sibling who-is(P-sib) = 277.1 ms (ok=true)
  Sibling watchers :not-reached = 0 / 20 | heavy watcher :not-reached = 0 | heavy cleared = true
  Post-check: under actors=3000 (all ended=true), live sign-ins=0; depth-32 who-is ended=true actor-ended=true session-closed=true driven-by={:kind :model, :model "worker"}
PASSED Measure 3: heavy end-actor within Revision 2 bound and 0 sibling :not-reached.
=== MEASURE 3b: end-actor over 3,000 ended agents made after it ===
  Ended the 3,000 later agents of P-hist in 7698.9 ms
  end-actor of each of 100 agents: no ended history mean=2.455 p95=2.875 ms | 3,000 ended agents after them mean=2.590 p95=2.934 ms
  under P-hist afterwards: 3100 agents, all ended=true, in the order they were made=true
PASSED Measure 3b: end-actor reads no agent that ended before, and under still gives every agent in the order it was made.
=== MEASURE 4: No write and no read of one person goes through a shared task ===
  Static audit: :global?/|global/|all present=false, |direct only in ping-task=true
  Single person depot partition deltas across 4 tasks: {:root [0 0 0 4], :inv [0 0 0 1], :ent [0 1 0 0], :ask [0 0 0 2]}
  40 distinct persons depot partition counts across 4 tasks: {:root [28 52 52 28], :inv [7 5 14 14], :ent [9 10 13 8], :ask [20 8 32 20]}
PASSED Measure 4: no write and no read of one person passes through a shared task.
STEP 4 Summary: m1=true m2=true m3=true m3b=true m4=true (ALL_PASSED=true, elapsed=58.1 s)
```

### What is open after this pass

1. **No model check runs the steps that run (review point 9).** The docstrings now say so; the gap stays. Position: close it with an adapter namespace that implements the interface of `auth-concurrent.plan-steps` (`empty-pstates`, `task-for`, `depot-info`, `make-initial-msg`, `execute-step`, the four reads and the seeds) by calling `schema.clj`'s own step functions over in-memory PStates shaped as `module.clj`'s, so that carbon's unchanged checker (`model_check.clj`, `spec_oracle.clj`, `step1_run.clj`) runs the steps that run, in all four fault and retry modes; and kill Auth's worker on the proof cluster while cross-key writes (`invite`, `enter`, `answer-ask`) are in flight, so that the real module replays records and fails hops at least once. The spine's cluster proof kills Auth's worker while a connection is held, which is neither.
2. **Rows kept for good (review, Low).** One `EnterOpFailed` row in `$$enter-ops` for each refused enter, and one `ok:<op>` row in `$$root-data` for each answer, stay for ever. Not changed in this pass: they are what makes an `:all-after` replay of a refused or answered write give its first result, and giving them an end changes that replay protocol, which belongs with the replay dedup that Sid set aside. Position: give each an end tied to take-out, past a horizon well beyond any replay (indexed in `exp:` for `ok:` rows; a time-ordered index on `H(op)` for `$$enter-ops`), model-checked first against a long outage with cross-depot interleaving; until then, the runtime's sign-in pace is what bounds their growth.
3. **Replays of refused writes on `*root-events` (review, Low; §4.5).** Left out of this pass by Sid ("`*root-events` replay dedup"). §4 item 5 and §5 choice 7 stand as they were.
4. The items of §4 not named here stand as they were.

### What the runtime sees differently

- A string that UTF-8 cannot carry now gives `{:could-not :read-the-event}` from every write that takes it (`a3941d66`). Nothing the runtime sends over the line can hold one (the wire is UTF-8); a claim of an ID token could, through a JSON escape, and its sign-in would then be refused instead of kept changed.
- Everything else of the contract is as it was: `watch-root` hands the same values (`module-check` holds them against the reference), and a failed opening is still told `:not-reached` asynchronously, now on a virtual thread.

### Choices of this pass (each Sid's to turn)

1. **The UTF-8 rule (`a3941d66`)** is a default, marked in `SPEC.md`, `fit.clj` and `reference.clj`. The other way would keep such strings, encoded losslessly for every key and value that Auth stores.
2. **`$$live`'s inner map tracks no size** (`{:track-size? false}`): nothing reads its count, and tracking costs a read on each write.
3. **Ended agents' order records under `ae:`**, beside the live ones under `ao:`, rather than a children index (`ch:`), which the build had chosen against: `end-actor`'s scan is bounded by the live agents of the root, not by its subtree.
4. **A record of another form is acked `{:could-not :read-the-event}`**, not dropped: an appender that is not the client learns what happened to it.
5. **The bounds of the new measures:** Measure 2 holds each p50 under 20,000 watched roots to twice its p50 with none, plus 2 ms; Measure 3b holds the mean `end-actor` over ended history to 1.5 ms above the mean without; `partial-outage` holds the sweep to 6 s. Each is generous to the new code and fails the old.
6. **`late-open` forces the wait with `with-redefs`** of the private `proxy-open-timeout-ms`, and `partial-outage` stubs the private `probe-root` and Rama's `foreign-invoke-query-async`, rather than adding a test hook to the client.
