VERDICT: major-fail

## Revisions that the plan needs, in order of weight

1. **Redesign the multi-partition `enter` and `invite` protocols (`PLAN.md:472–575`) to close nine concurrency and retry safety bugs proven by Step 1.**
   - **`enter` (`PLAN.md:516–546`):**
     1. *Eliminate the uncoordinated 1-claim fast path (`PLAN.md:521–526`):* Skipping `$$enter-ops` allows a helper `c2` (dispatched for `op1`) to claim `C1 -> P1` after `c1` already missed `C1` and cleared `AccountLinking(op1)`, and on retry before Step 3b lets `c2` claim `C2 -> P2`, binding **one account to two persons** (`#{P1 P2}`) and burning two invitations (`Witness 1`, `Witness 4`, `Witness 11`). Route **every** first-time `enter` (1-claim and 2-claim alike) through one durable per-attempt commit record `$$enter-ops[op-id]` (`EnterOpPending -> EnterOpTryEmail -> EnterOpWon(P, C) | EnterOpFailed`), so once `op1` transitions to `EnterOpFailed`, no delayed helper can ever acquire `C1` on `op1`'s behalf, and once `op1` transitions to `EnterOpWon(P, C)`, `$$accounts[Acc]` cannot clear `AccountLinking(op1)` until `AccountLinked(P)` is written.
     2. *Add strict CAS ownership on `$$claims[C]` when evicting a stale `ClaimClaiming` (`PLAN.md:525, 537`):* When two accounts (`Acc2`, `Acc3`) observe `ClaimClaiming(P1, stale-op, :code)`, hop to `H(stale-op)`, see `stale-op` moved on, and hop back to `H(C)`, `PLAN.md:525, 537` overwrites `$$claims[C]` unconditionally — letting **both accounts win the same claim and person** with zero crashes (`Witness 2`). On `H(C)`, overwrite **only if** `$$claims[C]` is *still* `ClaimClaiming(P1, stale-op, stale-phase)`; otherwise re-evaluate the updated `$$claims[C]`.
     3. *Clean up `ClaimClaiming` before short-circuiting a retry on `AccountLinked` (`PLAN.md:513, 530`):* If a helper writes `ClaimClaiming(P1, op1, :code)` after `op1` already linked `Acc1 -> P2` and crashes before `H(op1)`, its retry hits `AccountLinked` in Step 1 (`PLAN.md:513`) and returns without restoring `C_code` to `ClaimWaiting(P1)`, leaving `C_code` stuck in `ClaimClaiming` (`Witness 3`). Note that routing 1-claim/2-claim through `$$enter-ops[op1]` and checking `$$enter-ops[op1]` (or having any future reader of `ClaimClaiming(op1)` see `EnterOpWon(P2, :email)` on `H(op1)` and restore `ClaimWaiting(P1)` via CAS) resolves it only if `$$claims[C_code]` is restored to `ClaimWaiting(P1)` rather than `ClaimUsed` when a subsequent `invite` or `enter` inspects it, AND Step 1 retries carry/clean any tentative claim or check `op1` first.
   - **`invite` (`PLAN.md:472–486, 503`):**
     1. *Enforce `:claim-in-use` ahead of `:person-in-use` across concurrent causal chains (`PLAN.md:474, 479–480`):* When `invite(P1, C1)` passes Step 1 (`$$claims[C1] == nil`), and before Step 2 runs on `H(P1)`, `invite(P2, C1)` returns `{:invited P2}` and `invite(P1, C2)` returns `{:invited P1}`, Step 2 sees `RootInvited(C2)` on `H(P1)` and returns `{:refused :person-in-use}` without re-checking `$$claims[C1]` — violating `SPEC.md:104–105, 153–154` with zero crashes (`Witness 9`). Before returning `{:refused :person-in-use}` from `H(P)` (both in Step 2 on `RootInvited`/`RootEntered` and after helping `C0`), hop `|hash C` to verify whether `$$claims[C]` now names another person (`!= P`); if so, return `{:refused :claim-in-use}`.
     2. *Clean up orphaned `RootReserving(C)` on `:claim-in-use` retry (`PLAN.md:472, 503`):* If `invite(P2, C)` writes `RootReserving(C)` in Step 2 and crashes before Step 3 while `invite(P1, C)` wins `C`, the retry of `invite(P2, C)` sees `$$claims[C].person == P1 != P2` in Step 1 and returns in 0 hops, leaving `$$root-data["<P2>\0r"]` in `RootReserving(C)` forever with uncollected email PII (`Witness 6`). On Step 1 `:claim-in-use`, hop `|hash P` and `NONE>` `"<P>\0r"` if it is `RootReserving(C)` before emitting `(ack-return> {:refused :claim-in-use})`.

2. **Replace `end-actor`'s `18,000`-seek recursive-`loop<-` BFS (`PLAN.md:78, 82, 86, 404, 658–668`) with a 2-scan (`ao:` + `$$live`) cascade, and raise `ping-task`'s outage ceiling above worst-case single-task write latency (`PLAN.md:745–748`).**
   - `PLAN.md`'s BFS issues `3·A + 2·G + 1·S` RocksDB seeks (`18,000` seeks + `34,000` transforms at `A = 3,000, G = 2,000, S = 5,000`). On Rama 1.6.0 `InProcessCluster`, even 1/5th scale (`600` actors, `400` sign-ins, `1,000` sessions = `3,600` seeks) stalls the task thread for **`2,590.78 ms`** (`who-is` on a sibling root stalls for **`2,552.89 ms`**, `ping-task` stalls for **`2,561.22 ms`**), and full scale (`3,000 / 2,000 / 5,000`) fails with `CallbackException` (non-yielding nested `loop<-` stack overflow / >30 s synchronous timeout), crossing the `2 s + 1 s = 3 s` `ping-task` threshold and firing false `:softland.auth.client/not-reached` across up to **20,000 watched roots** on that task.
   - Store `:initial-hash` on `LiveSignIn` (or keep `g:` read free by carrying the 12 `SignInRec` fields on `LiveSignIn`) and `:driven-by` on a non-proxied live-session summary (or `sg:`/flat map), scan `ao:` forward once from `target.seq` (`seq(maker) < seq(child)` always holds -> 1 range seek finds all live descendants in one linear pass), scan `$$live` once for `P` (1 range seek finds all affected live sign-ins and open sessions), delete `ch:`, `ag:`, and `sg:` from `$$root-data` entirely, apply writes via top-level `ops/explode` (not recursive `loop<-`), and set `ping-task` + fallback probe timeout to `>= 5 s + 3 s` so single-task cascades never trigger false `:not-reached`. At `(600, 400, 1,000)`, this cuts `end-actor` from **`2,590.78 ms` (`3,600` seeks)** to **`65.41 ms` (`3` seeks — 39.6× faster)**.

3. **Cut dominant `who-is` cost from 3 seeks to 2 seeks (`2.89` -> `1.93` weighted seeks/op, `-33.2%` cluster disk I/O) (`PLAN.md:9–11, 80, 325–359, 683–710`).**
   - `PLAN.md:710` claims 3 seeks cannot be reduced. Probe E disproved this on Rama 1.6.0: store immutable `:own-layer` (`UUID`, 16 B) and `:own-layer-made-at-begin?` (`Boolean`, 1 B) on `SignInRec` (`g:<G>`) at `begin-sign-in` / `answer-ask` (when `r` is already in hand -> 0 extra write seeks). In `"who-is"`, read `g:<G>` **first**; when `g:<G>` exists and `:own-layer-made-at-begin?` is `true` (>99.99% of requests, since `SPEC.md:305, 425` states no sign-in begins before own-layer is made), **skip reading `"<P>\0r"`**, falling back to `"<P>\0r"` via `<<if` only when `g:<G>` is absent or had `:own-layer-made-at-begin? == false`. Dominant HTTP `who-is` drops from 3 seeks to 2 seeks; `S = nil` drops from 2 seeks to 1 seek.

4. **Fix `answer-ask` post-return missed take-out on `P == P0` (`PLAN.md:589, 592–593`) and replay take-out ordering.**
   - In `PLAN.md:589`, when Copy 2 (`root = P1, at = 2000`) helps Copy 1 (`root = P1, at = 500`), Step 2 runs take-out at `500`, and Step 3 (`"If P != P0, hop |hash P to run take-out under P by Copy 2's :at"`) skips Copy 2's take-out because `P == P0`! Immediately after Copy 2 returns, `who-is`, `under`, and `watch-root` still report sign-ins/sessions with `500 < until <= 2000` as live (`Witness 10`). Always hop `|hash P` to run take-out at Copy 2's `:at` whenever `(or (not= P P0) (> at2 at0))` before Copy 2's `ack-return>`.
   - In Step 2 (`PLAN.md:592–593`), check `ok:<op>` **before** running `take-out(P, ans.at)` so that a stream retry of an already-evaluated `op` never re-executes `ans.at` take-out after a slower-clock write has committed on `H(P)`.

5. **Fix `:says` retention on `enter` to match Sid's Call 2 (`SPEC.md:282–284`, `PLAN.md:38, 513, 519, 545, 803`).**
   - `PLAN.md` overwrites `:says` and `:says-at` unconditionally on every returning/helping `enter`, so an older-clock enter (`at = 100`), an equal-`:at` enter (`at = 200` after `at = 200`), or a client re-run clobbers `:says` from `at = 300` (`Witnesses 7 & 8`, `2C-16`). Update `:says` and `:says-at` on `AccountLinked` **only when `(> event.at account.says-at)`** (*"an enter whose `:at` is not later than the kept one's leaves it"*).

6. **Bring `softland.auth.client` input validation and lifecycle edge cases into exact compliance with Sid's Call 1, `SPEC.md:121–141`, and `reference.clj` (`PLAN.md:30–31, 160, 383–401, 727, 858`).**
   - Delete Choice 2 (`PLAN.md:858`): `answer-ask` with `:yes? false` must require `:actor` (UUID), `:sign-in` (UUID), `:until` (fixed-width int), and `:new-agent` (`nil` or `{:label text}`).
   - Require every `-or-nil` key (`:email` and `:code-hash` on `enter`, `:label` on `begin-sign-in`, `:new-agent` on `answer-ask`, `:session` on `who-is`) to be present in the map via `contains?` (`:driven-by` on `open-session` alone may be omitted).
   - Reject non-lowercase emails (`(not= s (.toLowerCase s Locale/ROOT))`) with `{:could-not :read-the-event}` on both `invite` email claims and `enter` `:email` (`SPEC.md:130–132`, `reference.clj:132`).
   - Validate `watch-root` callback `f` with `(fn? f)` rather than `(ifn? f)` so keywords, maps, sets, symbols, and vars are rejected with `{:could-not :read-the-event}` (`QUESTIONS.md` Q27, `reference.clj:1202`).
   - Handle non-client/non-cluster arguments per `SPEC.md:134–140` and `reference.clj:989, 1037, 1067, 1075, 1195`: `(open non-cluster module-name)` -> `{:could-not :find-the-module}`, `(close! non-client)` -> `nil`, and any write/read/`watch-root` given a non-client -> `{:could-not :read-the-event}`.

7. **Align or explicitly settle the off-by-one depth bound (`32` vs `31` makers below root) between Auth (`SPEC.md:184, 334`, `PLAN.md:655, 808`) and `src-store/softland/store/act.clj:227–232`.**
   - Auth allows `32` makers below `P` (`33` actors total: `P -> A1 -> ... -> A32`), whereas `act.clj` caps `:permission-id` nesting at `32` counting the root permission as `1` (`31` delegated agent permissions below `P`; `32` makers produces `{:refuse :malformed}`). Flagged below for Sid's turn.

8. **Repair inaccurate justifications in `PLAN.md:329–332, 821, 833, 847–850`.**
   - Include the missing `S = nil`, ended-agent `G` category in the `Partitioning efficiency` table; remove the false statement in `PLAN.md:821` that microbatch cannot resolve same-batch cross-key races and remove `ack-return>` convenience as a ground for rejecting microbatch; replace the false claim in `PLAN.md:833, 849` that preventing Wall 3 requires "unbounded storage" (a bounded ring window of `topology.stream.checkpoint.progress.threshold` event IDs per partition costs ~16 KB/task, though still +1 seek/+1 write per root event).

---

## Focused validation checks (`a` through `g`)

### a. What Step 1 found

In [`overnight-audit/auth-concurrent/`](../../overnight-audit/auth-concurrent/), `PLAN.md`'s single-task steps for `invite` (`§1A`), `enter` with 1 claim and with 2 claims (`§1B`), `answer-ask` (`§1C`), `:root` take-outs (`§2`), `own-layer-made`, `make-actor`, `end-actor`, and the four reads (`who-is`, `ask-of`, `under`, `watch-root`) were encoded faithfully as plain Clojure step transitions in [`plan_steps.clj`](../../overnight-audit/auth-concurrent/src/auth_concurrent/plan_steps.clj) and explored exhaustively in [`model_check.clj`](../../overnight-audit/auth-concurrent/src/auth_concurrent/model_check.clj) across **25 scenarios (2-caller and 3-caller) × 4 fault/retry modes = 100 runs** (`1,641 ms`, **51,280** deduplicated `(state, queues, fault-budget)` configurations, **613** quiescent terminal histories, **0 capped runs**). Full shortest counterexample histories with `PLAN.md` line numbers on every step are in [`STEP1_RESULTS.md`](../../overnight-audit/auth-concurrent/STEP1_RESULTS.md).

| # | Step-1 Question | Broken? | Distinct witness classes found | Scenario × mode hits |
|---|---|---|---|---|
| 1 | Does one account ever get two persons, or one claim two accounts, or one person two claims? | **YES — FAIL** | `one-account-two-persons` (`3C-03`), `one-claim-two-accounts` (`3C-06`, `2C-15`) *(one person two claims: 0 breaks)* | 11 |
| 2 | Does one ask ever get two answers that stand? | **NO — PASS** | None (`AskAnswering(op)` + `ok:<op>` serialized `$$asks[U]` across all runs) | 0 |
| 3 | Is anything left forever in a state that is neither done nor refused? | **YES — FAIL** | `root-reserving-forever` (`2C-01`, `3C-01`, `3C-02`), `claim-claiming-forever` (`3C-04`), `claim-used-without-entered-root` (`3C-03`), `entered-root-without-matching-account-link` (`3C-06`) | 12 |
| 4 | Does a call (or re-run) give a result/state that `SPEC.md` + Sid's 2 calls disallow? | **YES — FAIL** | `no-valid-causal-linearization-of-results` (`3C-02`, `3C-03`), `final-state-or-says-violates-spec` (`2C-04`, `2C-05`, `2C-06`, `2C-16`, `3C-03`, `3C-04`), `client-rerun-violates-spec` (`3C-03`, `3C-04`) | 28 |
| 5 | After a write returns, can any read still miss its effect? | **YES — FAIL** | `answer-ask-takeout-missed-on-return` (`2C-12`), `enter-account-link-misses-effect` (`3C-03`) | 6 |

**Summary of the 11 distinct witness mechanisms (each proven by whole step trace in [`STEP1_RESULTS.md`](../../overnight-audit/auth-concurrent/STEP1_RESULTS.md)):**

1. **`one-account-two-persons` (`Witness 1`, `3C-03`, `:individual` / `:all-after` / `:timeout-overlap`, `PLAN.md:516, 519, 523, 526, 544–546`):** `c1` (`enter(Acc1, C1)`) writes `AccountLinking(op1)` (`:516`); `c2` (`enter(Acc1, C2)`) sees `AccountLinking(op1)` and hops to `H(C1)` to help `op1` (`:519`). `c1` sees `$$claims[C1] == nil`, hops back to `H(Acc1)`, clears `AccountLinking(op1)`, and returns `{:refused :not-invited}` (`:526`). Next, `c3` (`invite(P1, C1)`) commits `ClaimWaiting(P1)` (`:483`). Now `c2`'s queued helper hop arrives at `H(C1)`, takes the **1-claim fast path** (`:523`, which skips `$$enter-ops`), marks `C1 = ClaimUsed(P1, op1)`, and enters `P1` (`RootEntered(linked=Acc1)` on `H(P1)`, `:544`). When `c2` crashes before Step 3b (`:545`) and Rama retries `c2` from Step 1 on `H(Acc1)`, `$$accounts[Acc1]` is `nil`! So `c2` writes `AccountLinking(op2)`, claims `C2 -> P2`, enters `P2`, and links `Acc1 -> P2` — leaving `Acc1` recorded as the linked account of **two entered persons `#{:P1 :P2}`** and burning both claims! (Even with **0 crashes** in `:pure`, `c2` returns `{:person :P1}` after `c1` returned `:not-invited` even though `c2` only offered `C2 -> P2`.)
2. **`one-claim-two-accounts` (`Witness 2`, `3C-06` & `2C-15`, **`:pure` — 0 crashes**, `PLAN.md:525, 537`):** After a 2-claim enter (`op-stale`) falls through from `:code` to `:email` while a delayed helper leaves a stale `ClaimClaiming(P1, op-stale, :code)` on `$$claims[C_code]` (`PLAN.md:564` Trace E-3), two competing callers `Acc2` and `Acc3` (either 1-claim at `:525` or 2-claim at `:537`) both see `ClaimClaiming(P1, op-stale, :code)`, both hop to `H(op-stale)`, both see `EnterOpWon(*, :email)`, and both hop back to `H(C_code)` and **overwrite `$$claims[C_code]` without checking that `$$claims[C_code]` is still `ClaimClaiming(P1, op-stale, :code)`**! Both enter `P1` and both return `{:person :P1}`!
3. **`claim-claiming-forever` (`Witness 3`, `3C-04`, `:individual` / `:all-after`, `PLAN.md:513, 519, 530`):** Helper `c2` for 2-claim `enter` `c1` writes `ClaimClaiming(P1, op1, :code)` on `H(C_code)` (`:530`) after `c1` already won via `:email` and linked `Acc1 -> P2` (`:545`), then crashes before reaching `H(op1)`. Retry of `c2` hits `AccountLinked(P2)` in Step 1 (`:513`) and returns in 1 hop, leaving `$$claims[C_code]` stuck in `ClaimClaiming` forever.
4. **`claim-used-without-entered-root` (`Witness 4`, `3C-03`, `:individual` / `:all-after`, `PLAN.md:523`):** Same uncoordinated 1-claim helper as Witness 1, with the crash occurring right after `c2` writes `ClaimUsed(P1, op1)` (`:523`) and before `H(P1)`. Retry of `c2` takes `C2 -> P2`, leaving `C1` permanently `ClaimUsed(P1)` while `P1` stays `RootInvited` forever (invitation permanently burned, nobody can ever enter as `P1`).
5. **`entered-root-without-matching-account-link` (`Witness 5`, `3C-06`, `:individual` / `:all-after`, `PLAN.md:525, 544`):** In the Witness 2 race, `Acc3` sets `RootEntered(P1, linked=Acc3)` (`:544`), crashes before Step 3b, and on retry sees `C_code = ClaimUsed(op_Acc2)` and clears `AccountLinking(Acc3)`, leaving `P1.linked-account = Acc3` while `$$accounts[Acc3] = nil`.
6. **`root-reserving-forever` (`Witness 6`, `2C-01`, `:individual` / `:all-after`, `PLAN.md:472, 475, 503`):** `invite(P2, C)` writes `RootReserving(C)` on `H(P2)` (`:475`) and crashes before Step 3; `invite(P1, C)` wins `C`; retry of `invite(P2, C)` sees `$$claims[C].person == P1 != P2` in Step 1 (`:472`) and immediately returns `{:refused :claim-in-use}` in 0 hops, leaving `P2` in `RootReserving(C)` forever (including uncollected email PII on a refused invite).
7. **`client-rerun-violates-spec` (`Witness 7`, `3C-03` & `3C-04`, **`:pure` — 0 crashes**, `PLAN.md:513`):** After `c1` (`at = 100`) and `c2` (`at = 200`) settle with `:says` at `200`, re-sending `c1` (`at = 100`) unconditionally overwrites `:says` and `:says-at` back to `100` (`:513`), violating Sid's Call 2 (`SPEC.md:282–284`).
8. **`final-state-or-says-violates-spec` (`Witness 8`, `2C-05`, `2C-06`, `2C-16`, **`:pure` — 0 crashes**, `PLAN.md:513, 519, 545`):** Two concurrent or sequential enters of a linked account with `at = 300` then `at = 100` (or `at = 200` with `"first"` then `at = 200` with `"second"` in `2C-16`) leave `:says` from `at = 100` (or `"second"` on tie), violating `SPEC.md:282–284` (*"an enter whose `:at` is not later than the kept one's leaves it"*).
9. **`no-valid-causal-linearization-of-results` (`Witness 9`, `3C-02`, **`:pure` — 0 crashes**, `PLAN.md:472, 474`):** `c1 = invite(P1, C1)` checks `$$claims[C1] == nil` on `H(C1)` (`:472`). While `c1`'s hop to `H(P1)` is queued, `c2 = invite(P2, C1)` completes (`{:invited :P2}`), and then `c3 = invite(P1, C2)` (invoked *after* `c2` returned!) completes (`{:invited :P1}`). When `c1`'s hop runs on `H(P1)`, Step 2 (`:474`) sees `RootInvited(C2)` and returns `{:refused :person-in-use}`. Yet in **every** total order preserving `c2 < c3`, either `c1` precedes `c2` (must return `{:invited :P1}`), `c1` sits between `c2` and `c3` (must return `:claim-in-use`), or `c1` follows `c3` (where **both** `C1` and `P1` are in use, so `SPEC.md:104–105, 153–154` mandates `:claim-in-use` before `:person-in-use`!). `:person-in-use` is impossible in every causal order.
10. **`answer-ask-takeout-missed-on-return` (`Witness 10`, `2C-12`, **`:pure` — 0 crashes**, `PLAN.md:589, 592`):** Under `P1`, `G0` and `S0` have `:until = 1500`. Copy 1 calls `answer-ask(U, root=P1, at=500)` and Copy 2 calls `answer-ask(U, root=P1, at=2000)`. Copy 2 helps Copy 1 on `H(P1)` at `c1.at = 500` (`:592`), then in Step 3 (`:589`: `"If P != P0, hop |hash P to run take-out under P by Copy 2's :at"`) skips Copy 2's take-out because `P == P0 == :P1`! Copy 2 returns `{:yes ...}`, and subsequent `who-is`, `under`, and `watch-root` reads still see `G0` and `S0` (`until = 1500 <= 2000`) as live, violating `SPEC.md:106, 398–400`.
11. **`enter-account-link-misses-effect` (`Witness 11`, `3C-03`, `:individual` / `:timeout-overlap`, `PLAN.md:516, 545`):** In the Witness 1 overlap, `c1` returns `{:person :P1}` while `$$accounts[Acc1]` holds `AccountLinking(op2, :L2)` (and then flips to `:P2`), so a read/enter immediately after `c1` returns misses `c1`'s link.

---

### b. One stream topology vs microbatch for `invite`, first `enter`, and `answer-ask`

We tested `PLAN.md`'s grounds (`PLAN.md:218–221, 821`) both analytically and by building and running a hybrid module (`HybridAuthProbeMod` + `IllegalDualWriterMod` in [`step2_probes.clj`](../../overnight-audit/auth-concurrent/src/auth_concurrent/step2_probes.clj)) on Rama 1.6.0 `InProcessCluster` (`{:tasks 2 :threads 2}`).

#### 1. Three claims in `PLAN.md:218–221, 821` that break under scrutiny

1. **False claim on cross-key races (`PLAN.md:821`):** *"Moreover, microbatch does not eliminate conflicting same-batch cross-key races (`invite(P, C1)` vs `invite(P, C2)`)."*
   - **Broken (empirically disproven by Probe B):** In a microbatch topology, sequential `<<batch` blocks (`microbatch.md:38–67`) act as **global cross-task barriers**, and any worker crash rolls back the entire multi-partition microbatch attempt atomically (`microbatch.md:118`). In Probe B (`HybridAuthProbeMod`), 4 sequential `<<batch` blocks resolved the exact Witness-9 triangle (`invite(P2, C1)`, `invite(P1, C2)`, `invite(P1, C1)` in one batch) with **zero** `RootReserving` states, **zero** `$$enter-ops`, **zero** helping protocol, and **exact** `:claim-in-use` priority (`:op1-P1-C1 -> {:kind :refused, :reason :claim-in-use}`). Microbatch eliminates **9 of the 11 Step-1 bug classes** by construction (only the local `:says` tie-breaker and `P == P0` take-out logic are independent of stream vs microbatch).
2. **Irrelevant latency objection (`PLAN.md:821`):** *"adds >= 300 ms latency (missing tens-of-ms targets on `open-session`/`replace-secret`)."*
   - **Broken against the hybrid split:** Leaving the 9 single-root/ask writes (`open-session`, `close-session`, `replace-secret`, `begin-sign-in`, `end-sign-in`, `make-actor`, `end-actor`, `own-layer-made`, `ask`) in stream `"auth"` keeps all high-frequency writes at single-digit/tens-of-ms (`19.04 ms` in Probe B), while `invite`, first-time `enter`, and `answer-ask` run at human speed where `IMPLICIT_SPEC.md:233, 284, 725` explicitly allows a few hundred milliseconds to a second (Probe B measured **`286.27–390.94 ms`** idle-poll latency and **`359.92–394.49 ms`** active batch latency).
3. **Invalid `ack-return>` synchronization ground (`PLAN.md:218, 821` vs `artifact-plan-validation.md:39`):** Rejecting microbatch because it cannot emit `ack-return>` is invalid if client waiting can be implemented cleanly via a PII-free `$$mb-decisions` `{UUID -> DecisionRec}` PState (`foreign-proxy-async` or short backoff poll keyed by random `op-id` UUID, leaving 0 PII on `:not-invited` and erasable later via `depot.max.entries.per.partition` or ring cleanup).

#### 2. Constructed hybrid (`mb-onboarding` + `stream-roots`) vs fixed single-stream — where the hybrid genuinely hits Rama's PState ownership wall

Probe B empirically verified two Rama 1.6.0 invariants:
- **Cross-topology `local-select>` WORKS:** Stream topology `"stream-roots"` executed `(local-select> [(keypath *root)] $$mb-persons :> *mb-p)` on colocated task `H(*root)` and returned `{"stream-roots" {:ok true, :claim "bob@example.com"}}` with zero errors.
- **Cross-topology `local-transform>` FAILS HARD:** When `"st"` executed `local-transform>` on `$$shared` declared by `"mb"`, Rama 1.6.0 threw `rpl.rama.distributed.exceptions.IllegalWriteException {:name $$shared, :topology-id :mb, :curr-topology-id :st}` (`topology.clj:873`, `check_valid_writer!`) and shut down the worker watchdog.

Now trace what single-writer PState ownership forces on the hybrid for **`answer-ask`**, **`enter`**, and **`SPEC.md:446`**:

| Dimension | Constructed Hybrid (`mb` for `{invite, 1st enter, answer-ask}` + `stream` for the rest) | Fixed Single-Stream (`stream` `"auth"` with corrected state machines) |
|---|---|---|
| **`answer-ask` (`:yes` & take-out on `P`)** | `stream` **must** own `$$root-data` and `$$live` (written by `open-session`, `replace-secret`, `end-actor`, etc.). Therefore `mb` **cannot** run take-out on `P` or create `a:<A>` / `g:<G>` in `$$root-data` and `$$live`! Either:<br>• `mb` locks `$$asks[U]`, appends to an internal depot `*mb-to-root`, `stream` evaluates `P` (still needing `ok:<op-id>` dedup because `depot-partition-append!` inside `mb` re-appends on retry, `microbatch.md:127`!), and appends back to `*root-to-mb` for a **second** microbatch cycle (`~500–800 ms`, +2 internal depots, **restoring the exact cross-topology state machine microbatch was meant to avoid!**); OR<br>• `answer-ask` stays in `stream` (which then requires `stream` to own `$$asks` too). | `stream` owns `$$asks`, `$$root-data`, and `$$live`; `answer-ask` executes in 2 hops (`~3–5 ms`) with 0 internal depots and 1-seek `foreign-select-one` on `ask-of`. |
| **`enter` & `who-is` (`$$root-data["<P>\0r"]`)** | `own-layer-made` (in `stream`) writes `:own-layer-made?` while `invite` and first `enter` (in `mb`) write `RootInvited` / `RootEntered` (`:own-layer`, `:making`). Because both cannot write `"<P>\0r"`, either:<br>• `mb` owns `$$mb-persons[P]` and `stream` owns `$$root-data[P]`: every returning `enter` and fallback `who-is` must read **both** PStates (+1 seek forever on cold/unmade paths, +1 RocksDB instance per task); OR<br>• `mb` appends `PersonEntered` to `*root-events` and the client must wait for `mb` (`~300–400 ms`) + `stream` handoff before returning so `who-is` sees `:own-layer` on return (`SPEC.md:106`). | `stream` owns `$$root-data`, `$$accounts`, `$$claims`, and `$$enter-ops`; returning `enter` is 1 hop (`~1–2 ms`), first `enter` is 4–5 hops (`~5 ms`), 1 PState for root data. |
| **`SPEC.md:446` ("No one task that everything passes through")** | Every `<<batch` block in a microbatch topology coordinates its barrier from **task 0** (`microbatch.md:65`: *"microbatch processing always starts [on task 0]"*). While each cycle coordinates once per batch rather than per person, task 0 is still a global coordinator for every `mb` cycle. | Zero task-0 coordination; every write touches only `H(C)`, `H(P)`, `H(Acc)`, `H(op)`, or `H(U)`. |
| **Client decision state (`$$mb-decisions`)** | Requires an extra PState `{UUID -> DecisionRec}` + cleanup/TTL mechanism so `op-id` outcomes do not accumulate forever. | Zero decision-state storage (`ack-return>` is ephemeral in memory). |

**Ruling on `b`:**
- **For `answer-ask`:** Keeping `answer-ask` in `stream` holds unconditionally — moving it to microbatch hits Rama's `IllegalWriteException` wall against `$$root-data`/`$$live` and forces a 2-cycle ping-pong across two internal depots that is *more* complex than `stream`'s 3-step protocol.
- **For `invite` and first `enter`:** A hybrid where microbatch owns ONLY `($$claims, $$accounts, $$mb-persons, $$mb-decisions)` and appends a `RootInit` event to `*root-events` is feasible (~350–400 ms latency) and eliminates cross-task reservation bugs, **at the cost of** +2 PStates, an internal handoff before `enter` returns, and task-0 microbatch barrier coordination. Conversely, fixing `PLAN.md`'s single-stream state machines (Revision 1: routing 1-claim `enter` through `$$enter-ops`, adding CAS on `ClaimClaiming` eviction, re-checking `C` before `:person-in-use`, and cleaning `RootReserving` on Step-1 `:claim-in-use` — ~25 lines of state-machine logic, matching Opus's 4-invariant proof in [`second-view/PLAN-invite-and-enter.opus.md`](second-view/PLAN-invite-and-enter.opus.md)) keeps zero task-0 coordination, 5 ms latency, and single PState ownership. **However, `PLAN.md` FAILS Phase 2** both because its stream protocols for `invite`/`enter`/`answer-ask` are currently buggy (Step 1) and because its stated grounds against microbatch in `PLAN.md:821` contain a false technical claim about `<<batch` cross-key races.

---

### c. Sizing `end-actor` across thousands of actors, sign-ins, and sessions on a shared task

`PLAN.md:404` asserts: *"Worst-case `end-actor` (thousands of actors) completes locally in sub-second time."* Let us size `PLAN.md:658–668` accurately on a root `P1` with **`A = 3,000` live agents**, **`G = 2,000` live sign-ins**, and **`S = 5,000` open sessions** sharing task `t_P` with `~15,625` other roots and `20,000` `watch-root` proxies.

#### 1. Exact seek and write count in `PLAN.md:658–668`

Look closely at the schemas in `PLAN.md:80–87`:
- `ActorSignInEdge` (`ag:<A>:<G>`, `:82`) stores **only** `[sign-in<UUID> until<Long>]` — it omits `actor`, `kind`, `initial-hash`, `hash`, `prev-hash`, `prev-until`, `begun`, `label`, `seq`! Because `$$root-data` is a `{String -> IRootDatum}` map of whole serialized records, setting `:ended? true` on `g:<G>` (`:663`) requires **reading `g:<G>` first** (`1` point seek per live sign-in) or else `termval` wipes the `SignInRec`!
- `SignInSessionEdge` (`sg:<G>:<S>`, `:86`) stores **only** `[session<UUID> until<Long>]` — it omits `:driven-by`! Setting `:closed? true` on `s:<S>` (`:666`) while keeping `:driven-by` (`SPEC.md:343–344`) requires **reading `s:<S>` first** (`1` point seek per open session)!

Therefore, for `(A = 3,000, G = 2,000, S = 5,000)`, `PLAN.md`'s non-yielding BFS executes:
- **Per live actor `X` (`3,000`):** 1 point seek `a:<X>` + 1 range seek `ag:<X>:` + 1 range seek `ch:<X>:` = **`9,000` seeks** (`+ 8,000` iter-reads) + `6,000` writes (`a:`, `ao:`).
- **Per live sign-in `G` (`2,000`):** 1 point seek `g:<G>` + 1 range seek `sg:<G>:` = **`4,000` seeks** + `8,000` writes (`g:`, `$$live`, `exp:`, `ag:`).
- **Per open session `S` (`5,000`):** 1 point seek `s:<S>` = **`5,000` seeks** + `20,000` writes (`s:`, `$$live`, `exp:`, `sg:`).
- **Total in ONE non-yielding event on `t_P`:** **`18,000` RocksDB seeks + `34,000` PState writes** (`27,000` on `$$root-data` + `7,000` `NONE>` on `$$live`, evaluated against `20,000` range proxies and replicated before `:ack`).
- **Analytical cost (Rama skill model `0.5 ms`/cold seek, `0.15 ms`/warm seek):** **`2.7 s` warm to `9.0 s+` cold** just in RocksDB seeks, plus replication and proxy diffing — **not sub-second**.

#### 2. Empirical measurement on Rama 1.6.0 `InProcessCluster` (Probe C, [`step2_probes.clj`](../../overnight-audit/auth-concurrent/src/auth_concurrent/step2_probes.clj))

| Workload on shared task `0` (with active `$$live` proxies + sibling root `P_sib`) | Seeks | `end-actor` wall time | Sibling `who-is(P_sib)` stall | `ping-task(0)` stall | Outcome |
|---|---|---|---|---|---|
| Idle baseline (`P_sib`, no concurrent `end-actor`) | 3 | — | **`4.25 ms`** | **`3.09 ms`** | Healthy |
| **`PLAN.md` BFS — 1/5th scale** (`600` actors, `400` sign-ins, `1,000` sessions) | `3,600` | **`2,590.78 ms`** | **`2,552.89 ms`** (**600× spike**) | **`2,561.22 ms`** | Already **2.6 s** at 1/5th scale! |
| **`PLAN.md` BFS — full scale** (`3,000` actors, `2,000` sign-ins, `5,000` sessions) | `18,000` | **Failed (`> 12.9 s` / crash)** | **Task thread killed/blocked** | **Exceeds `3 s` ceiling** | **`CallbackException` (non-yielding `loop<-` stack overflow / >30 s sync timeout)** |
| **Constructed 2-scan — 1/5th scale** (`600` actors, `400` sign-ins, `1,000` sessions) | **`3`** | **`65.41 ms`** (**39.6× faster**) | **`59.55 ms`** | **`64.81 ms`** | Sub-100 ms, identical post-state |

#### 3. Cascading collateral damage on the shared task

1. **False `:softland.auth.client/not-reached` storm (`PLAN.md:745–748`):** `PLAN.md`'s watch monitor runs `"ping-task"` on `(|direct task-id)` with a **2 s timeout** followed by a **1 s `foreign-select-async` probe** on `$$live` — both execute on the **exact same blocked task thread `t_P`**! Any `end-actor` stall `> 3.0 s` (reached at `~700` actors under `PLAN.md`'s BFS!) causes **every connected runtime copy** to declare `t_P` unreachable and fire `:softland.auth.client/not-reached` across **all 20,000 watched roots** sharing `t_P`, tearing down and re-opening 20,000 `foreign-proxy` range scans at once!
2. **Head-of-line blocking of `who-is` and writes:** Every HTTP request (`who-is`) and `open-session`/`replace-secret` for all `~15,625` roots on `t_P` queues behind `end-actor` for multiple seconds (or fails client timeout at `10 s`, `PLAN.md:399`).
3. **JVM stack overflow on non-yielding `loop<-`:** Without yielding, deep continuation loops in Rama execute on the JVM call stack and overflow `-Xss16m` at thousands of entities.

#### 4. Constructed 2-scan fix (`3` seeks total, `39.6×` faster, `-3` index prefixes)

Because `make-actor` assigns strictly increasing `seq` (`seq(maker) < seq(child)` always holds):
1. Point-read `a:<A>` (`1` seek) to obtain `A.seq`.
2. Range-scan `ao:` from `"<P>\0ao:<hex-(:seq A)>"` to `"<P>\0ao;"` (`1` seek + `<= A` sequential `Next`s): accumulate `ended-actors = #{A ...}` in a single forward Clojure pass (`contains? ended-actors (:made-by ao)`), since every parent appears before every child!
3. Range-scan `$$live` on `["<P>\0", "<P>\1")` (`1` seek + `(L + O)` sequential `Next`s; `'g' < 's'` in ASCII, so all `LiveSignIn`s precede all `LiveSession`s!). Carry `initial-hash` on `LiveSignIn` and `driven-by` on a non-proxied field (or keep `driven-by` on `s:` via an `exp:`/`ls:` record while `$$live` stays `driven-by`-free), compute all `rd-upserts`, `rd-deletes`, and `live-deletes` in pure Clojure, and apply them via top-level `ops/explode` + `local-transform>` (zero recursive `loop<-` stack frames!).
4. Delete `ch:`, `ag:`, and `sg:` from `$$root-data` (`end-sign-in` scans `$$live`'s bounded `s:` range — tens to hundreds of open sessions = `< 1 ms` — instead of maintaining `sg:` on every `open-session`!).

---

### d. The four walls: Rama's limit or the plan's

| Wall in `PLAN.md:839–854` | Rama's limit or the plan's? | Verdict & Grounding |
|---|---|---|
| **Wall 1: Cross-partition mid-flight read visibility** (`PLAN.md:839–841`) | **Rama's limit** *(for mid-flight reads)*;<br>**The plan's bug** *(post-return)* | **Mid-flight:** Genuinely **Rama's limit**. Both stream (`stream.md:41`) and microbatch (`microbatch.md:128`) commit per-task partitions independently with no cross-partition snapshot read isolation, and routing all reads/writes through one global task is forbidden by `SPEC.md:446`. Note also that `SPEC.md:106` only requires *"A write returns when its effect can be read by any copy of the runtime"* — `"No read shows part of one write's effects"` (`IMPLICIT_SPEC.md:115–116`) was an over-strong Phase-0 derivation.<br>**Post-return:** `PLAN.md` broke `SPEC.md:106` *after* write return as well (Step-1 Witnesses 10 & 11) — those two are **the plan's bugs**, fixed in Revisions 1 & 4. |
| **Wall 2: Isolated single-proxy errors on a healthy task under caller-supplied `cluster`** (`PLAN.md:843–845`) | **Rama's client-API boundary**, compounded by **the plan's 3 s `ping-task` bug** | **Client config boundary:** Genuinely **Rama's boundary** given `(open cluster module-name)` (`SPEC.md:144`): `foreign.proxy.failure.window.seconds` (`120`) and `threshold` (`10`) live on `RamaClusterManager` construction, not per-proxy.<br>**Compounding plan bug:** `PLAN.md`'s `2 s + 1 s = 3 s` `ping-task` monitor (`PLAN.md:745–748`) introduces the *opposite* failure — false `:not-reached` storms whenever `end-actor` stalls a task `> 3 s` (Item `c`). |
| **Wall 3: Uncheckpointed stream replay under backward clock skew** (`PLAN.md:847–850`) | **The plan's choice, NOT Rama's limit** | **1. On `*asks` (`answer-ask`):** Purely a **plan bug** (`PLAN.md:592–593` runs `take-out` *before* checking `ok:<op>`; flipping lines 592 and 593 eliminates replay take-outs on `answer-ask` at **0 extra seeks**!).<br>**2. On `*root-events`:** `PLAN.md:833, 849` falsely claims deduplicating uncheckpointed stream replays requires *"unbounded storage"*. In Rama (`stream.md:166`), stream checkpoints progress every `topology.stream.checkpoint.progress.threshold` events (default ~100–1,000), so a fixed-capacity ring buffer of the last `W` event IDs per root/task (~16 KB/task, **bounded**) completely eliminates Wall 3 inside stream (and microbatch checkpoints offsets atomically with PStates). Choosing not to pay `+1 seek / +1 write` on every `open-session` under `SPEC.md:413–415`'s clock-keeping rule is a **cost tradeoff of the plan**, not a limit of Rama. |
| **Wall 4: `pstate.proxy.limit = 20000` counts `(root, runtime-copy)` subscriptions** (`PLAN.md:851–854`) | **Rama's mechanism, matching `SPEC.md:459–464` verbatim** *(not a true wall)* | `SPEC.md:459–463` explicitly states: *"The watched roots of one task, **over all copies of the runtime together**, must not be held to that default. The module sets what it needs at launch: it lets 20,000 roots be watched on one task..."* Rama's per-`(module, PState, task)` proxy count matches `SPEC.md`'s exact words, and `pstate.proxy.limit` is dynamically tunable via `rama setOption`. |

---

### e. `who-is`: the count of seeks, traced

#### 1. Exact seek trace of `PLAN.md`'s `"who-is"` (`PLAN.md:263–278, 683–710`)

Query topology `"who-is"` `[*root *sign-in *session :> *result]` begins with `(|hash *root)` (`1` network round trip directly to task `H(*root)`, `0` intermediate hops) and executes four sequential `<<if`-guarded point `local-select>` calls on `$$root-data`:

1. **Seek 1 (always):** `(local-select> [(keypath "<P>\0r")] $$root-data :> *r)`.
   - If `*r` is `nil`, `RootReserving`, or `RootInvited`: **short-circuits immediately** -> emits `{:sign-in nil :session nil :own-layer nil :own-layer-made? false}` across `(|origin)`. **Total = 1 seek, 0 iter-reads.**
2. **Seek 2 (iff `*r` is `RootEntered`):** `(local-select> [(keypath "<P>\0g:<G>")] $$root-data :> *sig)`.
3. **Seek 3 (iff `*r` is `RootEntered` AND `(some? *session)`):** `(local-select> [(keypath "<P>\0s:<S>")] $$root-data :> *sess)`. (Skipped with `0` seeks when `*session` is `nil`.)
4. **Seek 4 (iff `*r` is `RootEntered` AND `(some? *sig)` AND `(:ended? *sig)` AND `(not= (:actor *sig) *root)`):** `(local-select> [(keypath "<P>\0a:<A>")] $$root-data :> *actor)`. (Skipped with `0` seeks when `*sig` is `nil`, when `*sig` is live — since atomic `end-actor` guarantees its actor is live — or when `(:actor *sig) == *root` — since a person never ends.)

Complete input-class breakdown for `PLAN.md`:
- Unknown or waiting root `P` (any `G`, `S = nil` or `S != nil`): **1 seek** (`r`).
- Entered root `P`, live or human or unknown `G`, `S = nil`: **2 seeks** (`r`, `g:<G>`).
- Entered root `P`, ended agent `G` (`A != P`), `S = nil`: **3 seeks** (`r`, `g:<G>`, `a:<A>`) — *(omitted from `PLAN.md:329–332, 704–707` tables!)*.
- Entered root `P`, live or human or unknown `G`, `S != nil` *(dominant HTTP request)*: **3 seeks** (`r`, `g:<G>`, `s:<S>`).
- Entered root `P`, ended agent `G` (`A != P`), `S != nil`: **4 seeks** (`r`, `g:<G>`, `s:<S>`, `a:<A>`).

#### 2. Adversarial throughput reduction: breaking `PLAN.md:710`'s 3-seek lower bound (verified by Probe E)

`PLAN.md:710` claims: *"Why 3 seeks cannot be reduced further without breaking the spec: `r` (`:own-layer-made?` flips via `own-layer-made` without naming `G`), `g:<G>`, and `s:<S>` are three distinct entities..."*

**Refutation (constructed and verified on Rama 1.6.0 in Probe E, [`step2_probes.clj`](../../overnight-audit/auth-concurrent/src/auth_concurrent/step2_probes.clj)):**
- `:own-layer` is **immutable forever** once `P` enters (`SPEC.md:280–281`), and `:own-layer-made?` is **monotonic `false -> true`** (`SPEC.md:301–304`), with `SPEC.md:305, 425` stating *"no sign-in of hers begins before her layer is made."*
- When `begin-sign-in` or `answer-ask (:yes)` writes `SignInRec` (`g:<G>`), it **already holds `"<P>\0r"` in memory** on `H(P)`. Storing `own-layer<UUID>` (`+16 B`) and `own-layer-made-at-begin?<Boolean>` (`+1 B`) on `SignInRec` costs **0 extra write seeks** (`own-layer-made` still writes `"<P>\0r"` alone in O(1)).
- In `"who-is"`, read `g:<G>` **first** (`1` seek):
  - **Fast path (>99.99% of entered requests — `g:<G>` exists AND `(:own-layer-made-at-begin? g)` is `true`):** `P` is guaranteed `RootEntered` with `:own-layer = (:own-layer g)` and `:own-layer-made? = true` — **skip reading `"<P>\0r"` completely (`0` seeks on `r`)!**
  - **Fallback (`g:<G>` is `nil` OR `(:own-layer-made-at-begin? g)` is `false`):** read `"<P>\0r"` (`1` seek) to distinguish unknown/waiting `P` from entered `P` and read current `(:own-layer-made? r)`.

**Side-by-side seek comparison (verified 100% output-identical across all 8 cases in Probe E):**

| Workload category (`PLAN.md:329–332` weights) | Weight | `PLAN.md` seeks | Constructed 2-seek | Change |
|---|---|---|---|---|
| Entered root (`made?=true`), live `G`, `S != nil` (HTTP request) | `0.85` | `3` | **`2`** | **`-33.3%`** |
| Entered root (`made?=true`), live `G`, `S = nil` (conn open) | `0.10` | `2` | **`1`** | **`-50.0%`** |
| Entered root (`made?=true`), ended agent `G`, `S != nil` | `0.03` | `4` | **`3`** | **`-25.0%`** |
| Unknown or waiting root `P` (`g:<G>` miss -> `r` miss) | `0.02` | `1` | `2` | `+1` on `2%` invalid traffic |
| *(Edge: `G` begun before `own-layer-made`, before or after `own-layer-made`)* | `~0.00` | `2–3` | `2–3` | `0` (exact fallback via `r`) |
| **Weighted cluster RocksDB seeks / `who-is`** | **`1.00`** | **`2.89`** | **`1.93`** | **`-0.96` seeks/op (`-33.2%` disk I/O)** |

Under `artifact-plan-validation.md:95–96` (*"If the sketch does less total work per operation while staying within latency, FAIL — adopt the cheaper design"*), `PLAN.md`'s 3-seek `who-is` is a **FAIL**.

---

### f. Whether Auth's 32 and the store's 32 (`src-store/softland/store/act.clj`) count the same thing

**No — they are off by 1.**

1. **What the store counts (`src-store/softland/store/act.clj:129–133, 209–214, 227–232, 444–446`):**
   - `act.clj:227–231`: *"The one entry is the depth of a permission: how many permission ids one permission id can hold, **itself counted**. A first permission has depth 1. The engine adds one each time it enters the shape and refuses a thing whose count is over the number, so the number is the greatest depth: 32. A deeper permission is `:malformed`."*
   - In `conform` (`act.clj:444–446`), entering `:permission-id` sets `times = (inc (get seen :permission-id 0))` and checks `(<= times 32)`.
   - Thus a root permission `[P layer granted-in]` has `times = 1` (`0` makers below `P`), `[A1 layer granted-in perm_P]` has `times = 2` (`1` maker below `P`), ..., `[A31 layer granted-in perm_A30]` has `times = 32` (**`31` makers below `P`**), and `[A32 layer granted-in perm_A31]` has `times = 33 > 32` -> **refused as `:malformed`**!
2. **What Auth counts (`SPEC.md:184, 334`, `PLAN.md:655, 808`, `test-auth/softland/auth/reference.clj:96–99, 574`):**
   - `SPEC.md:184`: `{:refused :too-deep}  B is 32 makers below its root` (`SPEC.md:334`: *"Who made whom goes 32 deep at most, the store's own bound on how deep a permission can be"*).
   - `PLAN.md:655, 808` and `reference.clj:96–99, 574` assign `P` `depth = 0`, `A1` (`made-by P`) `depth = 1`, ..., `A31` `depth = 31`, and **`A32` (`made-by A31`) `depth = 32` (`{:made A32}` — ALLOWED!)**, refusing only `A33` (`made-by A32`, `(:depth B) >= 32`).
3. **Empirical verification (Probe F in [`step2_probes.clj`](../../overnight-audit/auth-concurrent/src/auth_concurrent/step2_probes.clj)):**
   - `makers-31` (`auth-actor-depth = 31`, `store-permission-nesting = 32`): `(act/has-shape? :permission-id perm-31)` => **`true`**, `(act/parse act-31)` => **`:ok`**.
   - `makers-32` (`auth-actor-depth = 32`, allowed by Auth, `store-permission-nesting = 33`): `(act/has-shape? :permission-id perm-32)` => **`false`**, `(act/parse act-32)` => **`{:refuse :malformed}`**.
4. **Consequence:** Auth counts **edges** (`32` `made-by` steps = `33` actors), whereas `act.clj` counts **vertices** (`32` nested `:permission-id` tuples = `31` delegation steps). An agent `A32` created legally in Auth at depth `32` **cannot** hold a 1-per-maker delegated store permission chain `[A32 ... [A1 ... [P ...]]]` without `softland.store.act/parse` rejecting every act it sends as `:malformed`.

---

### g. Every place where `PLAN.md`, `reference.clj`, and Sid's two calls of 22 November differ

Checked against `SPEC.md` (`234960bd`), `test-auth/softland/auth/reference.clj` (`7cebbac2`), `test-auth/softland/auth/QUESTIONS.md`, and Sid's two calls + two additions of 22 November:

| # | Contract point | `PLAN.md` | `reference.clj` (`7cebbac2`) | Sid's 22 Nov calls & `SPEC.md` (`234960bd`) | Status |
|---|---|---|---|---|---|
| 1 | **`answer-ask` with `:yes? false` (`no`) unjudged parts** | Choice 2 (`:160, 396, 858`) allows `:actor`, `:sign-in`, `:until`, `:new-agent` to be `nil` or omitted on `:yes? false` | Requires all 8 keys present (`:232–233`), `:actor` & `:sign-in` UUIDs, `:until` int, `:new-agent` `nil` or `{:label text}` (`:260` `:no-takes-any-parts` is a broken copy!) | **Call 1** (`SPEC.md:121–127, 384–385`): *"The parts of a write that it does not judge, as those of a no, must fit all the same."* | **`PLAN.md` FAILS** |
| 2 | **Omission of `-or-nil` keys** (`:email`, `:code-hash`, `:label` on `begin-sign-in`, `:new-agent`, `who-is` `:session`) | `:384` says "required keys present" without requiring `-or-nil` keys to be `contains?` when `nil` | `shaped?` (`:161, 218–238, 880`) requires `(contains? m k)` for every key except `:driven-by` (`:826` `:loose-form` is a broken copy!) | **Call 1** (`SPEC.md:121–124`): *"every part that the contract names is there, a part written `-or-nil` as nil when it holds nothing ... `:driven-by` alone may be left out."* | **`PLAN.md` FAILS** |
| 3 | **Email lowercase as form** (`invite` claim & `enter` `:email`) | `:37, 389, 392` checks `String <= 256 chars` only; uppercase email fits as a distinct string | `email?` (`:132`) checks `(= x (.toLowerCase x Locale/ROOT))` (`:830` `:capital-email-fits` is a broken copy!) | `SPEC.md:130–132` (`QUESTIONS.md` Q3): *"an email that Java's `toLowerCase`, in the root locale, would change does not fit, in a claim or in an enter, whatever else the enter holds."* | **`PLAN.md` FAILS** |
| 4 | **`:says` kept on linked `enter`** | `:38, 513, 519, 545, 803` overwrites `:says` and `:says-at` unconditionally on every returning/rerun enter | `:448–450` updates `:says` **only** when `(> at (:said-at link))` (`:836` `:says-of-every-enter` is a broken copy!) | **Call 2** (`SPEC.md:282–284`): *"`:says` is kept with the link from the enter with the latest `:at`, with that `:at`: an enter whose `:at` is not later than the kept one's leaves it."* | **`PLAN.md` FAILS** |
| 5 | **Returning/repeated `enter` `:own-layer-made?`** | `:513, 573, 803` keeps no memory of calls; reads live `:own-layer-made?` from `r` on every enter | `:420–422, 447–453` keeps no `:decided` memory; returns present `(:own-layer-made? p)` | **Call 2** (`SPEC.md:268–272`): *"gives its person ... with the own layer and the `:making` that were fixed, and `:own-layer-made?` as it stands at that enter."* | **Agrees (PASS)** |
| 6 | **Standing `answer-ask` vs refusals / `:over`** | `:48, 581–582, 813` checks `AskYes`/`AskNo` right after `:no-such-ask` (before `:over` and actor/id refusals), keeping no caller memory *(modulo `P == P0` take-out bug in #10)* | `:734–740` checks `(answered? k)` right after `(nil? k)`, returning `(what-stands k code)` before `over?` and `answer-refusal` | **Call 2** (`SPEC.md:369–372`): *"Once an ask has an answer, every answer that fits gives what stands, before any refusal, and changes nothing but what its time takes out."* | **Order agrees; `P==P0` take-out FAILS** |
| 7 | **`replace-secret` two-hash rule (Sid's Addition 2)** | `SignInRec` (`:80`) keeps **only** `hash` (present) and `prev-hash` (the one before it) — zero replacement history (`initial-hash` is read solely by `begin-sign-in`). `:806` judges repeat by `(and (= hash H') (= prev-hash H))`, then `:not-live`, then `(!= hash H) -> :wrong-secret`. Thus `H1->H2->H1` followed by `H1->H2` again is **taken**, and `H1->H2` arriving after `H2->H3` gets `:wrong-secret`. *(Note: on first `H1->H1` when `prev-hash == nil`, both `PLAN.md:806` and `reference.clj:526` take it and set `prev-hash = H1`, overriding `IMPLICIT_SPEC.md:455`.)* | `:31–33, 525–537` keeps `:hash` and `:previous` (plus `:first-hash` for `begin-sign-in`), no `:replaced` history (`:840` `:replacements-remembered` is a broken copy!); identical `(and (= new-hash (:hash g)) (= old-hash (get-in g [:previous :hash])))` rule and `until <= at` cascade (`:536` / `PLAN.md:652`). | **Call 2 & Addition 2** (`SPEC.md:315–325`): *"a sign-in keeps no history of its replacements ... judged by the present hash and the one before it alone: it is refused with `:wrong-secret` when its old hash is no longer the hash now, and a hash that has come back is taken when the replacement's old hash is the hash now."* | **Agrees (PASS)** |
| 8 | **`watch-root` callback `f` check** | `:727` checks `(ifn? f)` (accepts keywords, maps, sets, symbols, vars!) | `:1202` checks `(fn? f)` (`QUESTIONS.md` Q27) | `SPEC.md:237`: *"a function of one argument"* (`QUESTIONS.md` Q27 settled on `fn?`) | **`PLAN.md` FAILS** |
| 9 | **Non-client / non-cluster arguments & `close!`** | `:30–31, 399` omits non-client argument handling; maps bad/unreachable cluster on `open` to `:reach-the-module` | `(open non-cluster _)` -> `{:could-not :find-the-module}` (`:1067`); `(close! non-client)` -> `nil` (`:1075`); write/read/`watch-root` on `non-client` -> `{:could-not :read-the-event}` (`:989, 1037, 1195`) | `SPEC.md:134–140`: *"when ... what [`open`] is given stands for no cluster [`-> :find-the-module`] ... A call whose client is no client at all gives `{:could-not :read-the-event}`, and `close!` of it gives nil."* | **`PLAN.md` FAILS** |
| 10 | **Concurrent multi-key races** (`invite`, `enter`, `answer-ask`) | Exhibits Step-1 Witnesses 1–6, 9–11 under cross-task concurrency/retries | Single-atom `(locking place ...)` is strictly linearizable (`Step 3` passed 360/360 multi-thread batches) | `SPEC.md:104–106, 288–293, 369–377, 398–400` | **`PLAN.md` FAILS** |

---

## Phase 2 Template Checklist (`artifact-plan-validation.md`)

### Query topology: `"who-is"`
- Input examples present: **yes** (`PLAN.md:273–276`)
- Example 1 (unknown/waiting `P`, `S = S1`): N=1, M=1. N == M? **yes**
- Example 2 (entered `P`, live `G`, `S = nil`): N=2, M=2 *(under `PLAN.md`'s schema; reducible to M=1 via Revision 3)*. N == M? **no — FAIL** (`r` read is avoidable on >99.99% of entered sign-ins once `SignInRec` carries `own-layer` + `own-layer-made-at-begin?`)
- Example 3 (entered `P`, live `G`, `S = S1`): N=3, M=2 *(reducible from 3 seeks to 2 seeks via Revision 3)*. N == M? **no — FAIL** (issues 1 wasted seek on `"<P>\0r"` on every HTTP request)
- Example 4 (entered `P`, ended agent `G`, `S = S1`): N=4, M=3 *(reducible to 3 seeks)*. N == M? **no — FAIL**
- M values across examples: `[1, 2, 3, 4]` in `PLAN.md` (`[1, 2, 3]` with Revision 3). All same? **no**
- If M differs: must be marked variable with dynamic approach. Is it? **yes** (`PLAN.md:277–278`, sequential `<<if` guards)

### Query topology: `"under"`
- Input examples present: **yes** (`PLAN.md:289–292`)
- Example 1 (unknown/waiting `P`): N=1, M=1. N == M? **yes**
- Example 2 (newly entered `P`, 0 agents, 0 sign-ins): N=1, M=1. N == M? **yes**
- Example 3 (entered `P`, 0 agents, 3 live sign-ins): N=2, M=2. N == M? **yes**
- Example 4 (entered `P`, 250 agents, 8 live sign-ins): N=3, M=3 (`r` + `ao:` range with `{:allow-yield? true}` + `$$live` `g:` range). N == M? **yes**
- M values across examples: `[1, 1, 2, 3]`. All same? **no**
- If M differs: must be marked variable with dynamic approach. Is it? **yes** (`PLAN.md:293–294`, `<<if` on `RootEntered`, `(pos? next-actor-seq)`, `(pos? next-sign-in-seq)`) — **PASS**

### Query topology: `"ping-task"`
- Input examples present: **yes** (`PLAN.md:301–302`)
- Example 1 (`*task-id = 0`): N=0, M=0. N == M? **yes**
- Example 2 (`*task-id = 15`): N=0, M=0. N == M? **yes**
- M values across examples: `[0, 0]`. All same? **yes** (`Fixed`, `M = 0`). *(Note: topology read efficiency is PASS, while its 2 s + 1 s client timeout interacts fatally with `end-actor` stalls — see Item `c`.)*

### Direct foreign read: `ask-of`
- Uses direct `(foreign-select-one [(keypath U)] $$asks {:pkey U})` (`PLAN.md:12, 261`): N=1, M=1 (`Fixed`, 1 point seek on `|hash U`) — **PASS**.

### PState schemas
- **Grouped by `(key type, partitioner)`:**
  - `(String, |hash P)` -> `$$root-data`, `$$live` (`PLAN.md:188`): justified by key-space / proxy-dispatch separation (`$$live` has `pstate.proxy.limit = 20000` with active range-proxy diff matching on every `local-transform>` and churns `NONE>` deletions, whereas `$$root-data` holds permanent history `a:`, `ao:`, `g:`, `s:`, `ok:` + `exp:` with 0 proxies; merging multiplies range-proxy subscriber matching CPU ~4× on every write and mixes tombstones into permanent history).
  - `(AccountKey, |hash AccountKey)` -> `$$accounts` (1 PState).
  - `(ClaimKey, |hash ClaimKey)` -> `$$claims` (1 PState).
  - `(String, |hash code)` -> `$$asks` (1 PState; distinct partitioner `|hash code` vs `|hash P`).
  - `(UUID, |hash op-id)` -> `$$enter-ops` (1 PState).
- **Any `Object` type?** **no** (`PLAN.md:56, 250–257`).
- **Uniform record-like values use `fixed-keys-schema` (or typed `defrecord`)?** **yes** — polymorphic PState values use `definterface + defrecord` (`IRootDatum`, `ILiveEntry`, `IAccountState`, `IClaimState`, `IAskState`, `IEnterOpState`, `IDrivenBy`).
- **Different instances at same PState position use `definterface + defrecord`?** **yes** (`PLAN.md:68–90, 112–116, 134–173`).
- **Inner collections that can exceed 100 elements subindexed?** **yes** — the sole non-subindexed inner map is `:says` (`IPersistentMap` inside `AccountLinking`/`AccountLinked`, `PLAN.md:136–138`), explicitly bounded to `<= 32` entries by client input validation before depot append (`SPEC.md:119–120`). All other collections are top-level RocksDB-indexed keys.
- **Schema flaw found during operational trace:** `ActorSignInEdge` (`PLAN.md:82`), `SignInSessionEdge` (`PLAN.md:86`), and `ChildEdge` (`PLAN.md:78`) omit fields needed by `end-actor` (`PLAN.md:663–666`), forcing `18,000` hidden point/range seeks; under Revision 2 (`ao:` + `$$live` 2-scan), `ch:`, `ag:`, and `sg:` are deleted altogether and `SignInRec` gains `:own-layer` + `:own-layer-made-at-begin?` (`+17 B`) — **FAIL** (requires Revision 2 & Revision 3 schema adjustments).

### Partitioning
- **`|hash` check across all 5 keyspaces (`P`, `Acc`, `C`, `U`, `op-id`):** Millions of UUIDs/accounts/claims/codes (`>= 10^4` keys/task at `N = 128`, `< 3%` hash variance), no global singleton key, hot per-root live state ~20 KB. *(Exception: on `end-actor` for a power-user root with thousands of live actors/sessions, `PLAN.md`'s `18,000`-seek BFS creates a multi-second single-task hot stall — FAIL, fixed by Revision 2.)*
- **`|all` check:** None used (`0` `|all` PStates or partitioners).
- **`## Partitioning efficiency` table check (`PLAN.md:325–359`):**
  - Filled in for `N = 1, 16, 128`? **yes**.
  - Proportions sum to 1? `0.85 + 0.10 + 0.03 + 0.02 = 1.00`, **HOWEVER** the category *"Entered root, ended agent sign-in (`A != P`), `S = nil`"* (`3` seeks in `PLAN.md`, `2` seeks in Revision 3) is **missing** from the table — **FAIL** (`artifact-plan-validation.md:29`: *"if all inputs aren't represented ... FAIL"*).
  - Weighted sums computed as totals across all tasks? **yes** (`2.89` seeks/op, `1.00` task/op, flat across `N = 1, 16, 128`, zero growth with `N`).
  - Stored-placement alternatives costed on total I/O? **yes** (`PLAN.md:316`: splitting `G`/`S` across tasks via stored placement adds `+1` placement seek -> `3.89+` seeks and violates `SPEC.md:451` *"one round trip to one task"*).

### Topologies
- **Microbatch unless justified?** High-frequency root writes (`open-session`, `replace-secret`, etc.) require stream for tens-of-ms latency + `:ack` visibility (`SPEC.md:106`), and `answer-ask` requires stream because it must atomically mutate `$$root-data` and `$$live` owned by `"auth"` (`IllegalWriteException` proven in Probe B).
- **Invalid justifications present (`artifact-plan-validation.md:39`)?** **yes — FAIL:** `PLAN.md:821` falsely claims microbatch cannot handle conflicting same-batch cross-key races and leans on `ack-return>` convenience rather than pure spec latency/ownership arithmetic (see Item `b`).

### Production readiness
- **Multiple concurrent clients?** **no — FAIL** (Step 1 proved Witnesses 1, 2, 7, 8, 9, 10 with **0 worker crashes** under 2–3 concurrent callers).
- **Client process restarts?** **no — FAIL** (client re-run after restart overwrites newer `:says` via Witness 7; `open-session`/`begin-sign-in`/`make-actor`/`replace-secret`/`ask` repeats are safe).
- **Worker process restarts at any point during topology execution?** **no — FAIL** (Step 1 `:individual` and `:all-after` modes proved Witnesses 1, 3, 4, 5, 6, 11 leave permanent half-states or double-bind accounts).
- **Large scale (millions of entities, thousands of actors/root)?** **no — FAIL** (Probe C proved `end-actor` stalls `2.59 s` at `600` actors and fails with `CallbackException` at `3,000` actors, triggering false `:not-reached` across `20,000` watches).
- **Non-idempotent stream writes audit (`PLAN.md:237–248`):** Single-task writes (`make-actor` via `a:<A>`, `begin-sign-in` via `g:<G>` + `:initial-hash`, `replace-secret` via `(hash, prev-hash)`, `open-session` via `max` on `:until`) are properly deduplicated; multi-partition writes (`invite`, `enter`, `answer-ask`) **FAIL** partial-failure + retry safety (Witnesses 1, 3, 4, 5, 6, 11, plus `answer-ask` `take-out` before `ok:<op>` check on `:592–593`).

### Internal depot usage & Cross-topology correctness & Stream topology correctness
- Zero `:disallow` internal depots and zero `depot-partition-append!` calls (`PLAN.md:196, 222`) — **PASS** on commit-boundary and cross-topology duplicate rules.

### In-memory state efficiency
- Zero module `TaskGlobal`s (`PLAN.md:418, 444`) — **PASS** on module task-thread GC/heap isolation. (Client-side JVM watch registry holds one shared `ProxyState` + flat Clojure map per watched root `P`, ~15–20 KB/root.)

---

## Minimality — adversarial simplification

**Simplest plausible baseline sketch (3 sentences):** One stream topology `"auth"` consuming 3 depots (`*root-events` by `:root-id`, `*asks` by `:code`, `*onboarding` or `*invites`+`*enters` for PII isolation), owning 5 PStates (`$$root-data` with `r`, `a:`, `ao:`, `g:`, `s:`, `exp:`, `ok:`; `$$live` with `g:`, `s:`; `$$accounts`; `$$claims`; `$$asks`) plus `$$enter-ops` `{UUID -> IEnterOpState}` used uniformly by all first-time `enter`s. Query topologies `"who-is"` (2-seek fast path) and `"under"` read `$$root-data`/`$$live` directly on `|hash P`; `ask-of` uses `foreign-select-one` on `$$asks`; `watch-root` uses `foreign-proxy-async` on `$$live` with `"ping-task"` (`>= 8 s` ceiling). All cascades (`end-actor`, `end-sign-in`, take-outs) scan `ao:`, `exp:`, and `$$live` directly with zero auxiliary `ch:`, `ag:`, or `sg:` edge indexes.

### Depot `*root-events`
- **Delete it**: No client entry point for the 8 root writes (`SPEC.md:163–200`).
- **Merge or bypass it**: Merging `*invites`/`*enters` into `*root-events` mixes PII (`account`, `email`, `:says`) into the high-volume session/secret log (violating clean future PII erasure, `SPEC.md:437–440`) and adds an extra initial hop since `invite`/`enter` do not know `P` before resolving `C`/`Acc`. **Verdict: PASS (keep).**

### Depot `*asks`
- **Delete it**: No entry point for `ask` / `answer-ask` (`SPEC.md:201–212`).
- **Merge or bypass it**: Merging `*asks` into `*root-events` forces `ask` (which has no `:root`, `SPEC.md:201`) to hash by `:code` on a mixed depot and exposes high-volume `*root-events` partitions to cross-task `:all-after` replays whenever `answer-ask` hops `H(U) <-> H(P)`. **Verdict: PASS (keep).**

### Depots `*invites` and `*enters`
- **Delete them**: No entry point for `invite` / `enter` (`SPEC.md:149–161`).
- **Merge or bypass them**: `*invites` (`hash-by :claim`, email PII only) and `*enters` (`hash-by :account`, account + email + `:says` PII) start on distinct partition keys (`C` vs `Acc`), saving 1 hop on every returning `enter` (`99.9%` of enters) and every `:claim-in-use` refusal while keeping both PII logs small and separately migratable (`SPEC.md:437–440`). **Verdict: PASS (keep).**

### Stream topology `"auth"`
- **Delete it**: Nothing processes any depot or owns any PState.
- **Merge or bypass it**: Already a single ETL topology (`1` topology total). **Verdict: PASS (keep, fixing protocols per Revisions 1–5).**

### Query topologies `"who-is"`, `"under"`, `"ping-task"`
- **Delete `"who-is"`**: Client-side sequential `foreign-select-one` calls would require **2–3 network round trips** instead of 1, violating `SPEC.md:451` (*"It is one round trip to one task"*). **Keep (with 2-seek Revision 3).**
- **Delete `"under"`**: Reading `r`, `ao:`, and `$$live` from client would take 2–3 round trips and lose `{:allow-yield? true}` server-side assembly (`SPEC.md:226–231`). **Keep.**
- **Delete `"ping-task"`**: Probe `auth_rama_probe.clj` proved Rama 1.6.0 `ProxyState` takes ~100 s under default `RamaClusterManager` config to fire `UngracefulTerminationDiff`, violating `SPEC.md:245–246` (*"within seconds of the loss"*). Replacing `"ping-task"` (`N` in-memory 0-seek RPCs / 2 s) with per-watch `foreign-select` polling across `20,000` watches would cost `10,000` RocksDB seeks/sec/task! **Keep (raising timeout per Revision 2 so `end-actor` never trips it).**

### PStates `$$root-data`, `$$live`, `$$accounts`, `$$claims`, `$$asks`, `$$enter-ops`
- **`$$root-data`**: Primary per-root durable store (`SPEC.md:465–467`). Cannot merge with `$$accounts`/`$$claims`/`$$asks` (different partition keys). **Keep.**
- **`$$live`**: Deleting it forces `watch-root` to proxy `$$root-data` (bounded-value violation over millions of closed sessions, `SPEC.md:453–456`, plus 4–5× range-proxy CPU overhead on every history write). **Keep.**
- **`$$accounts`, `$$claims`, `$$asks`**: Each is the unique `|hash`-partitioned index for `[provider subject]`, `ClaimKey`, and `code` (`SPEC.md:156, 149, 201, 223`). **Keep.**
- **`$$enter-ops`**: Deleting it and putting `AccountKey` in `$$claims` leaks `[provider subject]` PII into code claims (`SPEC.md:437–440`); omitting a commit coordinator causes double-person binding (`Witness 1`). **Keep — and extend to 1-claim `enter`s too (`FAIL` on `PLAN.md`'s 1-claim fast-path split: deleting the separate 1-claim fast path simplifies the code AND fixes Witnesses 1, 4, 11!).**

### Index key prefixes inside `$$root-data` (`ao:`, `exp:`, `ok:`, vs `ch:`, `ag:`, `sg:`)
- **`ao:<hex-seq>`**: Deleting it forces `under(P)` (`SPEC.md:226–230`: *"in the order in which they were made"*) to scan `a:` in UUID order and sort thousands of actors in memory without sequence index (acceptable if `a:` is scanned + sorted by `:seq`, saving 1 write on rare `make-actor`/`end-actor`, OR kept at ~140 B/actor so yielded `ao:` scan streams directly in order). Keeping `ao:` also powers our 1-scan `end-actor`! **Keep.**
- **`exp:<hex-until>:...`**: Deleting it forces every `open-session` and `replace-secret` to range-scan all of `$$live` (`200–2,000` open sessions = `1.5–10.5 ms` vs `0.5 ms` empty range probe). **Keep.**
- **`ok:<op-id>`**: Deleting it causes `answer-ask` retries/helpers on `H(P)` to see `:in-use` for `A`/`G` they themselves just created and falsely revert `AskAnswering` to `:refused :in-use`! **Keep.**
- **`ch:<B>:<A>`, `ag:<A>:<G>`, `sg:<G>:<S>` (`PLAN.md:78, 82, 86`)**:
  - **Delete all three!** Constructed replacement:
    - For `end-actor`: 1 forward scan of `ao:` from `target.seq` + 1 scan of `$$live` (`65.41 ms` / `3` seeks vs `2,590.78 ms` / `3,600` seeks at 600 actors — **39.6× faster**!).
    - For `end-sign-in` (rare: human sign-out / program exit): 1 range scan of `$$live` `"<P>\0s:" .. "<P>\0s;"` (`1` seek + `20–200` open sessions × `5 µs` = `0.6–1.5 ms`), saving `1` write on **every** high-frequency `open-session` and `close-session`!
    - For `take-out` cascading an expired sign-in `G` to its open sessions: the same `$$live` `"<P>\0s:"` scan runs at most once per `:root` write when `>= 1` sign-in expired (`0` seeks when only sessions or nothing expired!).
  - Keeping `ch:`, `ag:`, and `sg:` adds **+2 writes on every `open-session`/`close-session` and +2 writes on every `begin-sign-in`/`end-sign-in`**, wastes ~100 MB/task of RocksDB entries, AND caused the `18,000`-seek `end-actor` failure! **Verdict: FAIL — delete `ch:`, `ag:`, and `sg:` from `PLAN.md`.**

---

## Throughput — adversarial

1. **`who-is` (highest throughput — every HTTP request & connection open, `SPEC.md:450–452`):**
   - **Constructed lower-cost design:** Revision 3 / Probe E (`SignInRec` carries immutable `:own-layer` + `:own-layer-made-at-begin?`; `"who-is"` reads `g:<G>` first and skips `"<P>\0r"` whenever `g:<G>` exists with `:own-layer-made-at-begin? == true`).
   - **Side-by-side numbers:** Cuts dominant HTTP `who-is` from **3 seeks to 2 seeks (`-33.3%`)**, `S = nil` from **2 seeks to 1 seek (`-50.0%`)**, and weighted seeks from **`2.89` to `1.93` (`-33.2%` cluster disk I/O)** at **0 extra write seeks** (`+17 B` per `SignInRec`).
   - **Verdict: FAIL** — adopt the 2-seek `"who-is"` design.

2. **`open-session` & `close-session` (highest write throughput — thousands/day/agent + half-span refresh, `SPEC.md:429–433, 455–456`):**
   - **Constructed lower-cost design:** Deleting `sg:<G>:<S>` (Minimality section above) removes `1` RocksDB key write from **every** new `open-session` and `1` RocksDB `NONE>` tombstone from **every** `close-session` and session take-out (`4` writes -> `3` writes per `open-session`, **`-25%` write batch entries** on the hottest write!), trading it for scanning `$$live`'s bounded `s:` range (`~20–200` open sessions = `0.6–1.5 ms`) only on rare `end-sign-in` or sign-in expiry.
   - **Verdict: FAIL** — drop `sg:` (and `ag:`, `ch:`).

3. **`replace-secret` (high write throughput, `IMPLICIT_SPEC.md:423–424`):**
   - `PLAN.md`: `1` `exp:` range seek (`0.5 ms` when nothing expired) + `1` `g:<G>` seek (`0.5 ms`) = **2 seeks** + `4` writes (`g:`, `$$live`, old `exp:` `NONE>`, new `exp:`). Cannot read fewer than `exp:` (mandatory take-out, `SPEC.md:398`) + `g:<G>` (mandatory hash/liveness check). **Verdict: PASS.**

4. **`end-actor` (burst resource spike on shared task, `SPEC.md:337–338, 469`):**
   - **Constructed lower-cost design:** Revision 2 / Probe C (`1` seek `a:<A>` + `1` `ao:` scan + `1` `$$live` scan = **`3` seeks** vs `PLAN.md`'s **`3A + 2G + 1S = 18,000` seeks**; **`65.41 ms` vs `2,590.78 ms` at 600 actors, 39.6× faster**).
   - **Verdict: FAIL** — adopt the 2-scan `end-actor` cascade.

---

## Spec coverage — trace every operation and constraint

### Global Constraint 1: What fits & input validation (`:read-the-event`)
- **Source** (`SPEC.md:121–132, 139–140`): *"What fits (Sid's call of 22 November): every part that the contract names is there, a part written `-or-nil` as nil when it holds nothing, each in its stated form, and nothing that the contract does not name. `:driven-by` alone may be left out... The parts of a write that it does not judge, as those of a no, must fit all the same. A write or a read whose input does not fit gives `{:could-not :read-the-event}`, and a write then writes nothing... an email that Java's `toLowerCase`, in the root locale, would change does not fit, in a claim or in an enter, whatever else the enter holds... A call whose client is no client at all gives `{:could-not :read-the-event}`, and `close!` of it gives nil."*
- **Trace through the plan**: Caller calls `(answer-ask client {:code "ABCD1234" :yes? false :root P1 :at 1000})` (omitting `:actor`, `:sign-in`, `:new-agent`, `:until`), or `(enter client {:account [:google "u1"] :says {} :email "Alice@Example.com" :code-hash nil :own-layer L1 :making M1 :at 1000})`, or `(who-is "not-a-client" {:root P1 :sign-in G1 :session nil})`. Under `PLAN.md:384–396, 858`, all three fail to return `{:could-not :read-the-event}` as required: the `no` passes via Choice 2 (`:858`), `"Alice@Example.com"` passes `:389` (`String <= 256`), and non-client handling is unspecified (`:399`).
- **Fault-tolerance check**: Pure client-side check before depot append; zero module state affected on worker restart, retry, or partial write.
- **Race-condition check**: Pure stateless check per call; concurrent callers do not interfere.
- **Flaws found**: (1) Choice 2 (`PLAN.md:160, 396, 858`) violates Sid's Call 1 on `answer-ask` `:yes? false`; (2) `-or-nil` keys are not explicitly required to be `contains?` (`PLAN.md:384`); (3) uppercase emails pass `PLAN.md:389` instead of failing `:read-the-event` (`SPEC.md:130–132`); (4) `watch-root` uses `ifn?` (`PLAN.md:727`) instead of `fn?` (`reference.clj:1202`); (5) non-client first arguments on reads/writes/`watch-root` are not guarded (`SPEC.md:139–140`).
- **Verdict**: FAIL
- **If FAIL**: Apply Revision 6 across all 18 functions in `softland.auth.client`.

### Global Constraint 2: Repeats, standing state, and refusal order
- **Source** (`SPEC.md:93–105, 270–271, 318–322, 369–372`): *"A write that arrives twice leaves the same state as once... Where two refusals apply to one write, a repeat is recognised first, then the refusals in the order in which the contract lists them."* + Sid's Call 2: *"A repeat's result comes from what stands, never from a memory of calls."*
- **Trace through the plan**:
  - Single-root writes (`PLAN.md:804–811`): `begin-sign-in` (`G` + `A, K, initial-hash`), `make-actor` (`A` + `B`), `open-session` (`S` + `G`), `replace-secret` (`hash == H'` and `prev-hash == H`), `end-sign-in`, `end-actor`, `close-session`, `own-layer-made`, `ask` all judge from standing state alone in contract refusal order after take-out.
  - Multi-key `invite` (`PLAN.md:472, 474`): `c1 = invite(P1, C1)` checks `C1 == nil` at `t = 100`; `c2 = invite(P2, C1)` commits `C1 -> P2` at `t = 110`; `c3 = invite(P1, C2)` commits `P1 -> C2` at `t = 120`; `c1` reaches `H(P1)` and returns `:person-in-use` instead of `:claim-in-use` (`Witness 9`).
- **Fault-tolerance check**: Worker restart/retry keeps standing PState records; however, `enter` client re-run (`Witness 7`) mutates `:says` backward when `at` is older (`PLAN.md:513`).
- **Race-condition check**: Concurrent `invite` triangle (`3C-02`) violates refusal order `:claim-in-use` before `:person-in-use`.
- **Flaws found**: (1) `invite` Step 2 (`PLAN.md:474, 479–480`) returns `:person-in-use` without re-verifying `$$claims[C]`; (2) `enter` (`PLAN.md:513`) mutates `:says` on older-`:at` repeat.
- **Verdict**: FAIL
- **If FAIL**: Apply Revision 1 (`invite` re-checks `$$claims[C]` before returning `:person-in-use`) and Revision 5 (`(> at says-at)` guard on `:says`).

### Global Constraint 3: Read-after-write visibility across copies
- **Source** (`SPEC.md:106`): *"A write returns when its effect can be read by any copy of the runtime."*
- **Trace through the plan**: Copy 1 calls `answer-ask(U, root=P1, at=500)` while Copy 2 calls `answer-ask(U, root=P1, at=2000)` on a root with `G0, S0` expiring at `1500`. Copy 2 helps Copy 1 at `500`, skips Step-3 take-out because `P == P0` (`PLAN.md:589`), and returns `{:yes ...}`. Copy 3 immediately calls `(who-is client {:root P1 :sign-in G0 :session S0})` and `(under client P1)` and sees `G0` `:ended? false` and `S0` `:closed? false` (`Witness 10`).
- **Fault-tolerance check**: Under `:individual` / `:timeout-overlap` retry on `3C-03`, `enter` also violates post-return account-link visibility (`Witness 11`).
- **Race-condition check**: Broken in 0-fault `:pure` mode on `2C-12` (`Witness 10`) and retry mode on `3C-03` (`Witness 11`).
- **Flaws found**: `PLAN.md:589` skips helper take-out when `P == P0` and `at2 > at0`; `PLAN.md:521–526` 1-claim `enter` fast path races helper cleanup.
- **Verdict**: FAIL
- **If FAIL**: Apply Revisions 1 and 4.

### Global Constraint 4: No clock & `:root` take-outs (including time's edge)
- **Source** (`SPEC.md:107–109, 398–421`): *"No write reads a clock... Each write that names a `:root` takes out of what is live under that root every sign-in and every session whose time is at or before the write's `:at`, before the write itself is judged, and whether the write is then taken, is a repeat, or is refused... `invite`, `enter`, `own-layer-made` and `ask` name no root and take nothing out... A sign-in, a session or an ask whose end is at or before the `:at` of the event that makes it is taken and is never live."*
- **Trace through the plan**: On `H(P)`, `exp:<hex-until>:<g|s>:<id>` with `(bit-xor t Long/MIN_VALUE)` (`PLAN.md:62, 88, 642–652`) scans `["<P>\0exp:", "<P>\0exp:<hex-at>;")` (`0.5 ms` when empty), closing expired sessions (`S1` at `900 <= 1000`, `S3` at `1000 <= 1000`) and ending expired sign-ins (`G1` at `1000 <= 1000`) along with cascading open session `S2` (`until = 2000`). Born-expired items (`until <= at`, `:652`) write `:ended? true` / `:closed? true` directly and skip `$$live`/`exp:`. However, on `answer-ask` (`PLAN.md:589, 592–593`), `P == P0` helper skips `at2` take-out, and Step 2 runs `take-out` before checking `ok:<op>`.
- **Fault-tolerance check**: Worker restart preserves `exp:`; uncheckpointed stream replay on `answer-ask` re-runs `take-out(ans.at)` because line `592` precedes line `593` (`ok:<op>`).
- **Race-condition check**: Single-root writes serialize on `H(P)`; `answer-ask` helper with `P == P0` and `at2 > at0` fails (`Witness 10`).
- **Flaws found**: (1) `PLAN.md:589` skips take-out when `P == P0`; (2) `PLAN.md:592–593` runs `take-out` before `ok:<op>` dedup check.
- **Verdict**: FAIL
- **If FAIL**: Apply Revision 4.

### Global Constraint 5: Erasability of human PII & nothing deleted for non-PII facts
- **Source** (`SPEC.md:437–440, 465–467`): *"What names a human being is her account, her email, and what her provider said. All of it must be erasable later from everything that Auth keeps, its events too: the plan keeps that possible from the start, and builds none of it. An enter that is refused keeps its event like any other... Nothing is deleted. An ended sign-in, a closed session, an ended actor and an answered ask can still be read: the facts of them stay."*
- **Trace through the plan**: `PLAN.md:758–779` isolates `[provider subject]`, `email`, and `:says` to `$$accounts`, `$$claims` (`:email` keys), `$$root-data["<P>\0r"]` pointer fields, `*enters`, and `*invites`, using random `op-id` UUIDs in `ClaimUsed`/`$$enter-ops` so `*root-events`, `*asks`, `$$live`, `$$asks`, `a:`, `g:`, `s:` hold zero PII while keeping all historical actors/sign-ins/sessions/asks readable. However, in `Witness 6` (`PLAN.md:472, 475, 503`), a crashed + retried refused `invite(P2, {:by :email :email "alice@example.com"})` leaves `RootReserving` with `"alice@example.com"` stranded on `"<P2>\0r"` for a person `P2` who was refused and never invited!
- **Fault-tolerance check**: Crash between `invite` Step 2 and Step 3 + Step-1 `:claim-in-use` retry leaks `RootReserving` email PII on uninvited `P2` (`Witness 6`).
- **Race-condition check**: Triggered when two invites contend on one email claim (`2C-01`).
- **Flaws found**: Orphaned `RootReserving` retains email PII on a refused `invite` (`Witness 6`, `PLAN.md:472, 503`).
- **Verdict**: FAIL
- **If FAIL**: Apply Revision 1 (`invite` Step-1 `:claim-in-use` cleans up `RootReserving(C)` on `H(P)`).

### Global Constraint 6: Full-life scale — no bottleneck task, 20,000 watched roots/task, history kept apart
- **Source** (`SPEC.md:446–474`): *"No one task that everything passes through... `who-is` is the read of every request... It is one round trip to one task, and it reads a bounded amount however long the person's history is... What a runtime watches for one root stays small... it lets 20,000 roots be watched on one task... Persons: millions. Actors under one person: from one to thousands... Order. Events of one person are decided in one order. An end is never taken back."*
- **Trace through the plan**: Zero global/`|all` depots or PStates (`PLAN.md:784`); `$$live` launches with `pstate.proxy.limit = 20000` (`PLAN.md:111, 722`) and removes ended sign-ins/sessions via `NONE>`. Yet for a root with `3,000` actors, `2,000` sign-ins, and `5,000` sessions, `end-actor` (`PLAN.md:658–668`) issues `18,000` seeks in one non-yielding event, stalling the shared task thread and causing `ping-task` (`2 s + 1 s`) to fire false `:not-reached` across all `20,000` watched roots on that task (Probe C).
- **Fault-tolerance check**: Worker failure on `end-actor` at `3,000` actors throws `CallbackException` (`:retry-mode :all-after` then replays the crashing event repeatedly, wedging the partition!).
- **Race-condition check**: Unrelated roots sharing task `t_P` suffer multi-second `who-is` stalls (`2,552.89 ms` at 600 actors) and false watch drops.
- **Flaws found**: `end-actor` `18,000`-seek recursive `loop<-` + `3 s` `ping-task` false-positive interaction (Item `c`).
- **Verdict**: FAIL
- **If FAIL**: Apply Revision 2.

---

### Operation 1: `open`
- **Source** (`SPEC.md:133–136, 144`): `"(open cluster module-name) -> a client, or {:could-not :find-the-module ...}"` — *"`:find-the-module`, when the cluster that `open` is given holds no module of that name, or what it is given stands for no cluster; `:reach-the-module`, when Rama threw or gave no answer in time, with what it threw."*
- **Trace through the plan**: `(open ipc "Auth")` resolves 4 depots, 2 PStates, 3 query topologies, and `num-partitions` from `(foreign-object-info $$live)` (`PLAN.md:30, 399`). However, if called as `(open nil "Auth")` or `(open {:not :a-cluster} "Auth")` or `(open cluster 123)`, `PLAN.md:30, 399` catches the exception as `:reach-the-module` (or does not check `cluster` type / `string? module-name`), whereas `SPEC.md:134–135` and `reference.clj:1061–1067` require `{:could-not :find-the-module}`!
- **Fault-tolerance check**: Stateless handle lookup; if cluster is unreachable over network, bounded timeout returns `{:could-not :reach-the-module}`.
- **Race-condition check**: Concurrent `open` calls create independent client records sharing zero mutable state.
- **Flaws found**: Non-cluster first argument or non-string `module-name` is not mapped to `{:could-not :find-the-module}` (`PLAN.md:30, 399` vs `SPEC.md:134–135`).
- **Verdict**: FAIL
- **If FAIL**: Check `(and (instance? RamaClusterManager cluster) (string? module-name))` (or ForeignCluster check) up front in `open` and return `{:could-not :find-the-module}` when false (Revision 6).

### Operation 2: `close!`
- **Source** (`SPEC.md:136–140, 145`): `"(close! client) -> nil"` — *"`:closed`, for a call on a client that was closed. `watch-root` on a closed client gives `{:could-not :closed}` in place of the function, and `close!` calls no `f` again. A call whose client is no client at all gives `{:could-not :read-the-event}`, and `close!` of it gives nil."*
- **Trace through the plan**: `(close! client)` (`PLAN.md:31, 752`) marks client closed, cancels `ping-task`, transitions watchers to `:stopped` under per-watcher turn locks (with `ThreadLocal` re-entrancy guard if called inside `f`), closes `foreign-proxy` handles, and returns `nil`. However, `(close! "not-a-client")` or `(close! nil)` is not specified in `PLAN.md:31` to return `nil` without throwing.
- **Fault-tolerance check**: Closing a client whose cluster already died catches proxy close errors and returns `nil`.
- **Race-condition check**: Turn lock + `:stopped` transition prevents callbacks after `close!` returns; re-entrant `ThreadLocal` avoids self-deadlock inside `f`.
- **Flaws found**: `PLAN.md:31` omits `(when-not (client? x) nil)` guard required by `SPEC.md:140`.
- **Verdict**: FAIL
- **If FAIL**: Guard `close!` with `(when (client? client) ...) nil` (Revision 6).

### Operation 3: `invite`
- **Source** (`SPEC.md:149–155, 255–262`): `"invite {:person P :claim C :invited-by B :at t} -> {:invited P} | {:refused :claim-in-use} | {:refused :person-in-use}"` — *"Makes the person `P` wait under the claim `C`. A claim names one person for good... A person has one claim... The same `P` under the same `C` again changes nothing, also after she has entered."*
- **Trace through the plan**: `invite(P1, {:by :email :email "alice@example.com"}, :system, 100)` (`PLAN.md:472–486`) checks `$$claims[C]` on `H(C)`, writes `RootReserving(C)` on `H(P1)`, commits `ClaimWaiting(P1)` on `H(C)`, and promotes `RootInvited(C)` on `H(P1)`.
- **Fault-tolerance check**: Crash after Step 2 (`RootReserving` on `P2`) + concurrent winner `P1` on `C` + retry of `P2` returns `:claim-in-use` in Step 1 (`:472`) without clearing `RootReserving(C)` on `P2` (`Witness 6`).
- **Race-condition check**: Causal chain `invite(P2, C1)` -> `invite(P1, C2)` while `invite(P1, C1)` is in flight between Step 1 and Step 2 returns `:person-in-use` instead of `:claim-in-use` (`Witness 9`). Also uppercase email claim `"Alice@b"` passes form check (`:389`).
- **Flaws found**: (1) `Witness 9` refusal-order violation (`PLAN.md:474, 479–480`); (2) `Witness 6` `RootReserving` leak (`PLAN.md:472, 503`); (3) uppercase email accepted (`PLAN.md:389`).
- **Verdict**: FAIL
- **If FAIL**: Apply Revisions 1 and 6.

### Operation 4: `enter`
- **Source** (`SPEC.md:156–161, 264–293`): `"enter {:account [provider subject] :says {...} :email text-or-nil :code-hash H-or-nil :own-layer L :making {:uuid U :session M} :at t} -> {:person P :own-layer L' :making {:uuid U' :session M'} :own-layer-made? bool} | {:refused :not-invited}"` + Sid's Call 2 (`:says` kept from enter with latest `:at`; present `:own-layer-made?` returned).
- **Trace through the plan**: `PLAN.md:512–546` handles returning accounts in 1 hop (`H(Acc) -> H(P)`), 1-claim unlinked accounts via a fast path skipping `$$enter-ops` (`:521–526`), and 2-claim unlinked accounts via `$$enter-ops` (`:527–542`).
- **Fault-tolerance check**: Crash before Step 3b on 1-claim helper causes `Acc1` to bind two persons `#{:P1 :P2}` (`Witness 1`) or burn `C1` with unentered `P1` (`Witness 4`) or leave `P1` entered with `$$accounts[Acc3] = nil` (`Witness 5`); crash on 2-claim helper before `H(op1)` after `Acc1` linked leaves `C_code` in `ClaimClaiming` forever (`Witness 3`).
- **Race-condition check**: Two accounts meeting stale `ClaimClaiming` on `C_code` both win `P1` with 0 crashes (`Witness 2`, `PLAN.md:525, 537`); older or equal `:at` overwrites `:says` (`Witnesses 7 & 8`, `2C-16`, `PLAN.md:513`).
- **Flaws found**: Witnesses 1, 2, 3, 4, 5, 7, 8, 11 (`PLAN.md:513–546`) + uppercase `:email` form check (`PLAN.md:389`).
- **Verdict**: FAIL
- **If FAIL**: Apply Revisions 1, 5, and 6.

### Operation 5: `own-layer-made`
- **Source** (`SPEC.md:163–164, 301–307`): `"own-layer-made {:person P :at t} -> {:made P} | {:refused :no-such-person}"` — *"From that call on, every enter says true, one that is sent again among them... `own-layer-made` of a person who waits is refused with `:no-such-person`."*
- **Trace through the plan**: Appends `OwnLayerMadeEvent` to `*root-events` (`hash-by :root-id` = `P`, 0 hops, `PLAN.md:39, 238, 804`). Reads `$$root-data["<P>\0r"]` on `H(P)`: if absent, `RootReserving`, or `RootInvited`, returns `{:refused :no-such-person}`; if `RootEntered`, sets `:own-layer-made? true` (no take-out run) and returns `{:made P}`.
- **Fault-tolerance check**: Zero in-memory state; retry on `RootEntered` sees `:own-layer-made? true` and returns `{:made P}`; 0 cross-partition hops.
- **Race-condition check**: Serialized on single task `H(P)` with returning `enter`'s flag read (`2C-10` passed all 4 modes in Step 1).
- **Flaws found**: Non-client first argument handling (`Revision 6` client edge); module topology logic itself has none.
- **Verdict**: FAIL *(client non-client guard in Revision 6; topology logic PASS)*
- **If FAIL**: Add `(client? client)` check in `softland.auth.client/own-layer-made` (Revision 6).

### Operation 6: `begin-sign-in`
- **Source** (`SPEC.md:166–171, 309–312`): `"begin-sign-in {:root P :actor A :sign-in G :kind K :hash H :until t1 :label text-or-nil :at t} -> {:begun G} | {:refused :no-such-actor} | {:refused :ended} | {:refused :in-use}"`
- **Trace through the plan**: On `H(P)` (0 hops, `PLAN.md:40, 240, 652, 805`): runs `take-out(P, t)`; reads `g:<G>` — if present with `(actor A, kind K, initial-hash H)`, returns `{:begun G}` (dropping `t1, label, t`); else checks `r` is `RootEntered` and (`A == P` or `a:<A>` exists) -> `:no-such-actor`; checks `A == P` or `(not (:ended? a))` -> `:ended`; checks `g:<G>` absent -> `:in-use`; then increments `next-sign-in-seq`, writes `SignInRec` (`ended? = (<= t1 t)`), and if `(> t1 t)` writes `LiveSignIn` to `$$live` and `exp:` to `$$root-data`.
- **Fault-tolerance check**: Single-task atomic commit; retry after commit matches `(A, K, initial-hash)` even if `replace-secret` or `end-sign-in` subsequently ran (`PLAN.md:832`).
- **Race-condition check**: Same-key events serialize on `H(P)` without yielding (`end-actor` vs `begin-sign-in` never leaves live sign-in under ended actor).
- **Flaws found**: (1) `:label` (`text-or-nil`) must be required via `contains?` even when `nil` (Revision 6); (2) writes redundant `ag:<A>:<G>` index (Revision 2); (3) omits `:own-layer` + `:own-layer-made-at-begin?` needed for 2-seek `who-is` (Revision 3).
- **Verdict**: FAIL
- **If FAIL**: Apply Revisions 2, 3, and 6.

### Operation 7: `replace-secret`
- **Source** (`SPEC.md:172–175, 314–328` + Sid's Call 2 / Addition 2): `"replace-secret {:root P :sign-in G :old-hash H :new-hash H' :until t1 :previous-until t2 :at t} -> {:replaced G} | {:refused :not-live} | {:refused :wrong-secret}"` — *"a sign-in keeps no history of its replacements... judged by the present hash and the one before it alone: it is refused with `:wrong-secret` when its old hash is no longer the hash now, and a hash that has come back is taken when the replacement's old hash is the hash now... A replacement whose new `:until` is at or before its own `:at` is taken, and its event takes the sign-in out at once, ended as by `end-sign-in`, with its sessions."*
- **Trace through the plan**: On `H(P)` (`PLAN.md:41, 80, 241, 652, 806`): Sign-in `G1` starts with `hash = H1, prev-hash = nil, until = 5000`.
  1. `replace-secret(G1, old=H1, new=H2, until=6000, prev-until=4500, at=100)`: `take-out(P, 100)`; repeat check `(and (= H1 H2) (= nil H1))` false; live; `hash == H1` -> sets `hash = H2, prev-hash = H1, until = 6000, prev-until = 4500`, updates `$$live` and `exp:` -> `{:replaced G1}`.
  2. `replace-secret(G1, old=H2, new=H1, until=7000, prev-until=5500, at=200)` (`H1` comes back): sets `hash = H1, prev-hash = H2` -> `{:replaced G1}`.
  3. `replace-secret(G1, old=H1, new=H2, until=8000, prev-until=6500, at=300)` (`H1 -> H2` a second time, Sid's Addition 2): repeat check `(and (= H1 H2) (= H2 H1))` false; live; `hash == H1` -> **taken!** Sets `hash = H2, prev-hash = H1, until = 8000, prev-until = 6500` -> `{:replaced G1}` (zero replacement history kept, exact match with `reference.clj:526–536`).
  4. Late repeat `replace-secret(G1, old=H2, new=H1, at=310)` arriving after step 3: `hash` is `H2 != H1` -> `:wrong-secret`.
  5. Born-expired `until = 350 <= at = 350` (`PLAN.md:652`): marks `g:<G1>` `:ended? true`, closes open sessions of `G1`, removes `G1` and its sessions from `$$live` and `exp:` -> `{:replaced G1}`.
- **Fault-tolerance check**: Single-task atomic commit; immediate retry sees `hash == H'` and `prev-hash == H` and returns `{:replaced G1}` without modifying `until` or `prev-until`.
- **Race-condition check**: Serialized on `H(P)` against `end-sign-in` and competing `replace-secret` calls.
- **Flaws found**: Non-client first argument guard in client (`Revision 6`); when `until <= at` cascades to open sessions (`PLAN.md:652`), drop `sg:` and scan `$$live` `"<P>\0s:"` per Revision 2. Core two-hash state machine satisfies Sid's Call 2 / Addition 2 completely.
- **Verdict**: FAIL *(client non-client guard + `sg:` removal; core 2-hash logic PASS)*
- **If FAIL**: Apply Revisions 2 and 6.

### Operation 8: `end-sign-in`
- **Source** (`SPEC.md:177–178, 330–331`): `"end-sign-in {:root P :sign-in G :at t} -> {:ended G} | {:refused :no-such-sign-in}"` — *"The sign-in ends, and every session under it is closed. Ending it again changes nothing."*
- **Trace through the plan**: On `H(P)` (`PLAN.md:42, 807`): runs `take-out(P, t)`; reads `g:<G>` — if absent, returns `{:refused :no-such-sign-in}`; if `:ended? true`, returns `{:ended G}`; else marks `g:<G>` `:ended? true`, removes `G` from `$$live` and `exp:`, and closes every open session of `G` (removing each from `$$live` and `exp:`).
- **Fault-tolerance check**: Single-task atomic commit; retry sees `:ended? true` -> `{:ended G}`.
- **Race-condition check**: Serialized on `H(P)` against `replace-secret` and `open-session`.
- **Flaws found**: In `PLAN.md:86`, scanning `sg:<G>:` lacks `:driven-by` so closing `s:<S>` requires an unaccounted point seek on `s:<S>` per open session (plus `ag:`/`sg:` write overhead) + client non-client guard.
- **Verdict**: FAIL
- **If FAIL**: Apply Revisions 2 and 6.

### Operation 9: `make-actor`
- **Source** (`SPEC.md:180–184, 333–335`): `"make-actor {:root P :actor A :made-by B :label text :at t} -> {:made A} | {:refused :no-such-maker} | {:refused :ended} | {:refused :in-use} | {:refused :too-deep}"` — *"Who made whom goes 32 deep at most, the store's own bound on how deep a permission can be; no read costs more with the depth."*
- **Trace through the plan**: On `H(P)` (`PLAN.md:43, 239, 808`): runs `take-out(P, t)`; checks repeat `a:<A>` with `(:made-by a) == B` -> `{:made A}`; checks `r` `RootEntered` and (`B == P` or `a:<B>` exists) -> `:no-such-maker`; checks `B == P` or `(not (:ended? b))` -> `:ended`; checks `A != P` and `a:<A>` absent -> `:in-use`; checks `(:depth B) < 32` -> `:too-deep`; writes `a:<A>` (`depth = inc depth_B`, `seq = next-actor-seq`) and `ao:<hex-seq>`.
- **Fault-tolerance check**: Deduplicated by `a:<A>` with `:made-by B` before touching `next-actor-seq`.
- **Race-condition check**: Serialized on `H(P)` against `end-actor` and `answer-ask`.
- **Flaws found**: (1) Writes unneeded `ch:<B>:<A>` (`Revision 2`); (2) off-by-one depth mismatch against `src-store/softland/store/act.clj` (`32` makers in Auth = `33` permissions -> `:malformed` in store, Item `f`); (3) client non-client guard (`Revision 6`).
- **Verdict**: FAIL
- **If FAIL**: Apply Revisions 2, 6, and 7.

### Operation 10: `end-actor`
- **Source** (`SPEC.md:186–188, 337–338, 469`): `"end-actor {:root P :actor A :at t} -> {:ended A} | {:refused :no-such-actor} | {:refused :is-a-person}"` — *"The agent ends, with every actor that it made, in any depth, and every sign-in and session of each. A person is not ended by this call... Actors under one person: from one to thousands."*
- **Trace through the plan**: On `H(P)` (`PLAN.md:44, 655–668, 809`): for `(3,000 actors, 2,000 sign-ins, 5,000 sessions)`, `PLAN.md`'s non-yielding BFS issues `18,000` seeks and `34,000` writes inside nested `loop<-`, failing with `CallbackException` at `3,000` actors and stalling `2,590.78 ms` even at `600` actors (Probe C).
- **Fault-tolerance check**: At `3,000` actors, worker throws fatal `CallbackException` and `:retry-mode :all-after` replays the crashing event endlessly.
- **Race-condition check**: Blocks sibling `who-is` for seconds and triggers false `ping-task` `:not-reached` across `20,000` watches on the shared task.
- **Flaws found**: `18,000`-seek BFS + nested non-yielding `loop<-` stack growth + `3 s` `ping-task` false-positive cascade (Item `c`).
- **Verdict**: FAIL
- **If FAIL**: Apply Revisions 2 and 6.

### Operation 11: `open-session`
- **Source** (`SPEC.md:190–197, 340–358`): `"open-session {:root P :sign-in G :session S :until t1 :at t :driven-by D} -> {:open S} | {:refused :not-live} | {:refused :in-use}"`
- **Trace through the plan**: On `H(P)` (`PLAN.md:45, 243, 642–652, 810`): runs `take-out(P, t)` first; reads `s:<S>` — if present with `(:sign-in s) == G`, leaves `:driven-by` unchanged, extends `:until` to `(max until t1)` only if `(not (:closed? s))` (updating `$$live` and replacing old `exp:` key with new `exp:` key), and returns `{:open S}`; else checks `g:<G>` exists and `(not (:ended? g))` -> `:not-live`; checks `s:<S>` absent -> `:in-use`; writes `s:<S>` (`closed? = (<= t1 t)`) and if `(> t1 t)` writes `LiveSession` (without `:driven-by`) to `$$live` and `exp:`.
- **Fault-tolerance check**: Single-task atomic commit; retry sees `s:<S>` with `t1 <= until` (or `:closed? true`) and performs 0 mutations.
- **Race-condition check**: Serialized on `H(P)` against `close-session`, `end-sign-in`, and `end-actor`.
- **Flaws found**: Writes redundant `sg:<G>:<S>` on every session open (`+25%` writes on highest-volume write, Revision 2) + client non-client guard (Revision 6).
- **Verdict**: FAIL
- **If FAIL**: Apply Revisions 2 and 6.

### Operation 12: `close-session`
- **Source** (`SPEC.md:198–199, 340–345`): `"close-session {:root P :session S :at t} -> {:closed S} | {:refused :no-such-session}"`
- **Trace through the plan**: On `H(P)` (`PLAN.md:46, 242, 811`): runs `take-out(P, t)`; reads `s:<S>` — if absent -> `:no-such-session`; if `:closed? true` -> `{:closed S}`; else marks `s:<S>` `:closed? true` (preserving `:driven-by`), removes `S` from `$$live` and `exp:`.
- **Fault-tolerance check**: Monotonic idempotent flag + `NONE>`.
- **Race-condition check**: Serialized on `H(P)`.
- **Flaws found**: Redundant `sg:` deletion (Revision 2) + client non-client guard (Revision 6).
- **Verdict**: FAIL
- **If FAIL**: Apply Revisions 2 and 6.

### Operation 13: `ask`
- **Source** (`SPEC.md:201–202, 360, 388–389, 418–419`): `"ask {:code U :hash H :asks text :ends t1 :at t} -> {:asked U} | {:refused :in-use}"` — *"A code is bound for good... An ask whose end is at or before the `:at` of the event that makes it is taken and is never live."*
- **Trace through the plan**: On `H(U)` (0 hops on `*asks`, `PLAN.md:47, 159, 244, 812, 862`): reads `$$asks[U]` — if present with `(:hash k) == H`, returns `{:asked U}`; if present with different hash, returns `{:refused :in-use}`; else writes `AskWaiting(H, asks, ends, (<= ends at))` and returns `{:asked U}`.
- **Fault-tolerance check**: Single-task atomic commit; retry matches `(:hash k) == H` -> `{:asked U}`.
- **Race-condition check**: Serialized on `H(U)` against competing `ask` and `answer-ask`.
- **Flaws found**: Client non-client guard only (`Revision 6`; topology logic PASS).
- **Verdict**: FAIL *(client non-client guard in Revision 6; topology logic PASS)*
- **If FAIL**: Add `(client? client)` guard in `softland.auth.client/ask` (Revision 6).

### Operation 14: `answer-ask`
- **Source** (`SPEC.md:204–212, 360–389, 398–400` + Sid's Calls 1 & 2): `"answer-ask {:code U :yes? bool :root P :actor A :new-agent {:label text}-or-nil :sign-in G :until t1 :at t} -> {:yes {:root P :actor A :sign-in G}} | {:no U} | {:refused :no-such-ask | :over | :no-such-actor | :ended | :in-use}"`
- **Trace through the plan**: `PLAN.md:579–610` acquires `AskAnswering(op)` on `H(U)`, evaluates on `H(P)` with `ok:<op>`, and commits `AskYes`/`AskNo` on `H(U)`.
- **Fault-tolerance check**: Line `592` runs `take-out(P, ans.at)` *before* line `593` checks `ok:<op>`, so uncheckpointed replay re-runs `ans.at` take-out after newer events.
- **Race-condition check**: When Copy 2 (`root=P1, at=2000`) helps Copy 1 (`root=P1, at=500`), `PLAN.md:589` (`If P != P0`) skips Copy 2's take-out, leaving `until = 1500` items live after Copy 2 returns (`Witness 10`). Also Choice 2 (`:858`) violates Sid's Call 1 on `:yes? false`.
- **Flaws found**: (1) `Witness 10` missed take-out when `P == P0` (`PLAN.md:589`); (2) `take-out` before `ok:<op>` (`PLAN.md:592–593`); (3) Choice 2 allows omitted/nil `:actor, :sign-in, :until` on `:yes? false` (`PLAN.md:160, 396, 858`); (4) `ag:`/`ch:` writes & missing `SignInRec` `:own-layer` fields (Revisions 2 & 3).
- **Verdict**: FAIL
- **If FAIL**: Apply Revisions 2, 3, 4, and 6.

### Operation 15: `who-is`
- **Source** (`SPEC.md:215–222, 450–452`): `"(who-is client {:root P :sign-in G :session S-or-nil}) -> {:sign-in ... :session ... :own-layer L-or-nil :own-layer-made? bool}"` — *"It is one round trip to one task, and it reads a bounded amount however long the person's history is."*
- **Trace through the plan**: `PLAN.md:263–278, 683–710` executes 3 seeks (`r`, `g:<G>`, `s:<S>`) on every HTTP request, whereas Probe E proved 2 seeks (`g:<G>`, `s:<S>`) produces identical results across all 8 input classes (`-33.2%` weighted seeks). Also client must require `:session` key via `contains?` even when `nil`, and check `(client? client)`.
- **Fault-tolerance check**: Read-only query topology over committed RocksDB state; survives worker restart cleanly.
- **Race-condition check**: Single-task snapshot read on `H(P)`; however, stalls for seconds during `PLAN.md`'s BFS `end-actor` (`2,552.89 ms` at 600 actors in Probe C).
- **Flaws found**: (1) Issues 3 seeks instead of 2 on dominant HTTP requests (`PLAN.md:710`); (2) omits ended-agent `S = nil` row in partitioning table (`PLAN.md:329–332`); (3) stalls behind BFS `end-actor`; (4) client `:session` `contains?` & `client?` checks.
- **Verdict**: FAIL
- **If FAIL**: Apply Revisions 2, 3, 6, and 8.

### Operation 16: `ask-of`
- **Source** (`SPEC.md:223–224, 385–389`): `"(ask-of client U) -> {:hash H :asks text :ends t1 :state :waiting|:yes|:no :answer {:root P :actor A :sign-in G}-or-nil}, or nil"`
- **Trace through the plan**: Direct `(foreign-select-one [(keypath U)] $$asks {:pkey U})` (`PLAN.md:12, 20`), projecting both `AskWaiting` and transient `AskAnswering` to `:state :waiting :answer nil`, `AskYes` to `:yes` (only committed after `A` and `G` are durable on `H(P)`, `:604`), and `AskNo` to `:no`.
- **Fault-tolerance check**: Durable PState point read; unaffected by worker restart.
- **Race-condition check**: Never exposes uncommitted `AskAnswering` or `:yes` before `H(P)` commits (`0` breaks in Step 1).
- **Flaws found**: Client `(client? client)` guard (`Revision 6`; PState read & projection PASS).
- **Verdict**: FAIL *(client non-client guard in Revision 6; PState read PASS)*
- **If FAIL**: Add `(client? client)` check in `softland.auth.client/ask-of` (Revision 6).

### Operation 17: `under`
- **Source** (`SPEC.md:226–231, 410–412`): `"(under client P) -> {:actors [{:actor A :made-by B :label text :ended? bool} ...] :sign-ins [{:sign-in G :actor A :kind K :label text-or-nil :begun t :until t1} ...]}"`
- **Trace through the plan**: Query topology `"under"` (`PLAN.md:280–294`) reads `r`, conditionally scans `ao:` with `{:allow-yield? true}` in creation order, and conditionally scans `$$live` `g:` sorting in memory by `:seq`.
- **Fault-tolerance check**: Yielded range select runs over a consistent RocksDB snapshot (`dataflow.md:195`); survives worker restart.
- **Race-condition check**: Snapshot read on `H(P)`; misses expired items after `answer-ask` `P == P0` helper bug (`Witness 10`).
- **Flaws found**: Affected by `answer-ask` `Witness 10` (`Revision 4`) and client `(client? client)` guard (`Revision 6`); `"under"` topology itself is PASS.
- **Verdict**: FAIL *(via Revisions 4 & 6; `"under"` query topology PASS)*
- **If FAIL**: Apply Revisions 4 and 6.

### Operation 18: `watch-root`
- **Source** (`SPEC.md:233–252, 453–464`): `"(watch-root client P f) -> a function of no argument that stops the watching"` — delivers `{:sign-ins ... :sessions ...}` (no `:driven-by`) on each live change, or `:softland.auth.client/not-reached` within seconds of loss once, no call at registration, `f` must be `fn?`.
- **Trace through the plan**: Shared `foreign-proxy-async` per `(client, P)` on `$$live` `["<P>\0", "<P>\1")` (`pstate.proxy.limit = 20000`), awaiting initial `ResyncDiff` (up to 5 s) before returning `stop`, suppressing registration callback (`PLAN.md:726–752`).
- **Fault-tolerance check**: Worker/cluster loss detected in 2–5 s via `"ping-task"`, firing `:not-reached` once. **HOWEVER**, during a single-task `end-actor` stall `> 3 s`, `"ping-task"` (`2 s`) + fallback `foreign-select-async` (`1 s`) falsely fires `:not-reached` across all `20,000` watches on that healthy task!
- **Race-condition check**: Register-before-`who-is` race closed by awaiting proxy open (`:730–732`). Callback `f` validated with `ifn?` (`:727`) instead of `fn?` (`reference.clj:1202`).
- **Flaws found**: (1) `3 s` `ping-task` false `:not-reached` storm under `end-actor` (Revision 2); (2) `ifn?` instead of `fn?` (`PLAN.md:727`, Revision 6); (3) non-client first argument guard (Revision 6); (4) missed `answer-ask` `P == P0` take-out push (`Witness 10`, Revision 4).
- **Verdict**: FAIL
- **If FAIL**: Apply Revisions 2, 4, and 6.

---

## What was run, what was only read, what is open, and choices for Sid to turn

### 1. What was run (commands and verbatim outputs)

#### Run 1 — Step 1: Exhaustive multi-task model of `PLAN.md` (`invite`, `enter` 1-claim & 2-claim, `answer-ask`)
- **Command:** `cd src/proposal/runtime-2026-11-21/overnight-audit/auth-concurrent && clojure -M:step1`
- **Output summary (full 100-row table and 11 whole witness histories in [`STEP1_RESULTS.md`](../../overnight-audit/auth-concurrent/STEP1_RESULTS.md)):**
  ```text
  Scenarios × modes run: 25 scenarios × 4 modes = 100 runs (1641 ms)
  Distinct (state, queues, fault-budget) configurations explored: 51,280 (0 capped runs)
  Quiescent terminal histories checked: 613
  Q1 (uniqueness): FAIL — one-account-two-persons, one-claim-two-accounts (11 hits)
  Q2 (two answers stand on one ask): PASS — 0 breaks
  Q3 (left forever unsettled): FAIL — root-reserving-forever, claim-claiming-forever,
     claim-used-without-entered-root, entered-root-without-matching-account-link (12 hits)
  Q4 (disallowed result / rerun): FAIL — no-valid-causal-linearization-of-results,
     final-state-or-says-violates-spec, client-rerun-violates-spec (28 hits)
  Q5 (read misses effect after write return): FAIL — answer-ask-takeout-missed-on-return,
     enter-account-link-misses-effect (6 hits)
  Wrote STEP1_RESULTS.md with 11 distinct witness classes in 1641 ms.
  ```

#### Run 2 — Step 2: Empirical Rama 1.6.0 `InProcessCluster` & `softland.store.act` probes (`Probe F`, `Probe B`, `Probe E`, `Probe C`)
- **Command:** `cd src/proposal/runtime-2026-11-21/overnight-audit/auth-concurrent && flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock clojure -M:probes`
- **Verbatim output:**
  ```clojure
  === PROBE F: Auth's 32 vs Store's 32 (softland.store.act) ===
  {:makers-31
   {:auth-actor-depth 31,
    :store-permission-nesting 32,
    :has-shape? true,
    :parse-verdict :ok},
   :makers-32
   {:auth-actor-depth 32,
    :auth-allows-make-actor? true,
    :store-permission-nesting 33,
    :has-shape? false,
    :parse-result {:refuse :malformed}}}

  === PROBE B: Hybrid Microbatch + Stream & PState Ownership ===
  ;; Worker threw: rpl.rama.distributed.exceptions.IllegalWriteException
  ;; {:name $$shared, :module user/IllegalDualWriterMod, :topology-id :mb, :curr-topology-id :st}
  {:dual-writer-rejection
   {:threw rpl.rama.distributed.exceptions.CallbackException,
    :message "Callback failure"},
   :triangle-results
   {:op2-P2-C1 {:kind :invited, :person #uuid "78a1daa0-02d3-40cd-bb65-28a018e5596b", :reason :none},
    :op3-P1-C2 {:kind :invited, :person #uuid "b56dc2e1-629a-4922-af81-e1f04c308922", :reason :none},
    :op1-P1-C1 {:kind :refused, :person #uuid "b56dc2e1-629a-4922-af81-e1f04c308922", :reason :claim-in-use}},
   :mb-batch-latency-ms 394.49,
   :mb-idle-poll-latency-ms 390.94,
   :stream-read-mb-pstate-ack {"stream-roots" {:ok true, :claim "bob@example.com"}},
   :stream-latency-ms 19.04}

  === PROBE E: who-is 2-Seek Construction vs PLAN.md 3-Seek ===
  {:all-equal? true,
   :cases
   [{:case "1. unknown root (S=S1)", :equal? true, :plan-seeks 1, :two-seek-seeks 2}
    {:case "2. waiting root (S=S1)", :equal? true, :plan-seeks 1, :two-seek-seeks 2}
    {:case "3. entered (made?=false), missing G", :equal? true, :plan-seeks 2, :two-seek-seeks 2}
    {:case "4. entered (made?=false), G begun before made", :equal? true, :plan-seeks 2, :two-seek-seeks 2}
    {:case "5. G begun before made, AFTER own-layer-made", :equal? true, :plan-seeks 2, :two-seek-seeks 2}
    {:case "6. dominant S=nil (entered made?=true, live G)", :equal? true, :plan-seeks 2, :two-seek-seeks 1}
    {:case "7. dominant HTTP S=S1 (entered made?=true, live G)", :equal? true, :plan-seeks 3, :two-seek-seeks 2}
    {:case "8. ended agent G (A!=P), S=S1", :equal? true, :plan-seeks 4, :two-seek-seeks 3}]}

  === PROBE C: end-actor Non-Yielding Sizing & Shared-Task Stall ===
  {:idle-baseline {:who-is-ms 4.25, :ping-task-ms 3.09},
   :plan-bfs-600a-400g-1000s
   {:proxy-callbacks 2, :seeks 3600, :end-actor-ms 2590.78,
    :ping-task-stall-ms 2561.22, :sibling-who-is-stall-ms 2552.89,
    :exceeds-3s-ping-ceiling? false, :exceeds-10s-client-timeout? false,
    :end-actor-ack {"auth" {:actors 600, :sessions 1000, :sign-ins 400}},
    :post-who-is {:own-layer #uuid "709a229b-b4fd-460a-b6fc-1bee0dfacd64", :closed? true, :ended? true}},
   :constructed-2scan-600a-400g-1000s
   {:proxy-callbacks 2, :seeks 3, :end-actor-ms 65.41,
    :ping-task-stall-ms 64.81, :sibling-who-is-stall-ms 59.55,
    :exceeds-3s-ping-ceiling? false, :exceeds-10s-client-timeout? false,
    :end-actor-ack {"auth" {:actors 600, :sign-ins 400, :sessions 1000}},
    :post-who-is {:own-layer #uuid "709a229b-b4fd-460a-b6fc-1bee0dfacd64", :closed? true, :ended? true}},
   :plan-bfs-3000a-2000g-5000s
   {:failed true, :label :plan-bfs-3k,
    :exception "class rpl.rama.distributed.exceptions.CallbackException",
    :message "Callback failure", :seeks 18000}}
  ```

#### Run 3 — Step 3: Multi-thread concurrent linearizability check (2 to 8 threads) against `test-auth/softland/auth/reference.clj`
- **Command:** `cd src/proposal/runtime-2026-11-21/overnight-audit/auth-concurrent && clojure -M:step3`
- **Verbatim output:**
  ```text
  === STEP 3: Multi-Thread Concurrent Linearizability Runner (2..8 Threads) ===

  1. Reference under explicit external lock (15 rounds × 8 scenarios = 120 batches):
     {:passed? true, :rounds 15, :batches-checked 135}

  2. Reference (`test-auth/softland/auth/reference.clj` internal lock, 25 rounds × 8 scenarios = 200 batches):
     {:passed? true, :rounds 25, :batches-checked 225}

  3. `{:could-not ...}` write ambiguity check (both taken and not-taken allowed):
     {:taken-accepted? true, :dropped-accepted? true}

  4. Purposefully racing copies caught (6/6):
     - racy-invite-split-locks              locked-pass=true  caught=true  scenario=1a. two-invites-same-claim (2 threads) (threads=2, batch=0) reason=:no-causal-order-matches-call-results
     - racy-enter-check-then-act            locked-pass=true  caught=true  scenario=2a. two-enters-one-account-first-time (3 threads) (threads=3, batch=2) reason=:no-causal-order-matches-call-results
     - racy-answer-ask-double-take          locked-pass=true  caught=true  scenario=4. two-answers-to-one-ask (6 threads) (threads=6, batch=5) reason=:no-causal-order-matches-call-results
     - racy-replace-vs-end-sign-in          locked-pass=true  caught=true  scenario=5. replace-secret-vs-end-sign-in (6 threads) (threads=6, batch=6) reason=:no-causal-order-matches-call-results
     - racy-end-actor-non-atomic-subtree    locked-pass=true  caught=true  scenario=6. end-actor-vs-open-session-and-make-actor (7 threads) (threads=7, batch=7) reason=:post-read-mismatch-on-all-valid-orders
     - racy-unlocked-read-modify-write      locked-pass=true  caught=true  scenario=1a. two-invites-same-claim (2 threads) (threads=2, batch=0) reason=:no-causal-order-matches-call-results

  Completed Step 3 verification in 431.0 ms. ALL_CHECKS_PASSED=true
  ```
  *(Note: 9 scenarios × 15 rounds = 135 batches; 9 scenarios × 25 rounds = 225 batches.)*

---

### 2. What was only read (not run in this session)

1. **`depot-migration` two-log physical disk erasure and `depot.max.entries.per.partition` trimming** (`PLAN.md:769–772`, read from `.agents/skills/rama/references/depot-reference.md` and Rama 1.6.0 docs; `SPEC.md:439` specifies building none of erasure in this pass).
2. **`pstate.proxy.limit = 20000` full 20,000-proxy instantiation and `destroy-module!` 15 s `UngracefulTerminationDiff` behavior** (`PLAN.md:720–724, 740`: read from prior empirical probes `/mnt/data/projects/research/softland/runtime-spine-2026-11-21/plan-probes/auth_rama_probe.clj` and `WORKFLOW-AND-MODEL.md` §2.1; in Probe C we verified `(set-launch-pstate-dynamic-option! setup "$$live" "pstate.proxy.limit" 20000)` launches cleanly with active range proxies).
3. **Multi-JVM network socket partitions & OS worker `kill -9` leader failover** (modeled exhaustively at the Rama task/channel/retry-mode level in Step 1 across `:individual`, `:all-after`, and `:timeout-overlap`, and run in-process on Rama 1.6.0 `InProcessCluster` in Step 2, rather than on a multi-node ZooKeeper/Conductor deployment).
4. **Custom `foreign.proxy.failure.window.seconds` / `threshold` client options on `RamaClusterManager`** (`PLAN.md:740, 844`: read from Rama 1.6.0 client docs).

---

### 3. What is open

1. **Auth's `32` vs Store's `32` off-by-one (`Item f`):** `SPEC.md:184, 334` and `reference.clj:96–99` allow `32` makers below root (`33` actors: `P -> A1 -> ... -> A32`), whereas `src-store/softland/store/act.clj:227–232` caps `:permission-id` nesting at `32` counting `P`'s root permission as `1` (`31` makers below root). Whether Auth's `deepest` should become `31` (so every Auth agent can receive a 1-per-maker delegated store permission) or `act.clj`'s `:permission-id` limit should become `33` (or stay decoupled) is Sid's call.
2. **Three standing readings in `SPEC.md` (`QUESTIONS.md` Q10, Q17, Q24):**
   - **Q10 (`SPEC.md:323–328`):** `replace-secret` with `:until <= :at` ends the sign-in and closes its sessions at once (`PLAN.md:652` and `reference.clj:536` agree).
   - **Q17 (`SPEC.md:353–357`):** Repeat of `open-session` on a closed/taken-out session returns `{:open S}` and leaves it closed (`PLAN.md:810` and `reference.clj:626–630` agree).
   - **Q24 (`SPEC.md:379–382`):** `answer-ask` `:yes` with `:new-agent nil` accepts any live agent of `P` at any depth `1..32` (`PLAN.md:598` and `reference.clj:716–717` agree).

---

### 4. Choices made by this session, each Sid's to turn

1. **Single-stream vs hybrid recommendation (`Item b`):** Kept the single-stream topology direction (once Revisions 1–5 fix its state machines and seek counts) rather than mandating a microbatch + stream split, on the ground that Rama's `IllegalWriteException` single-writer rule prevents microbatch from mutating `$$root-data`/`$$live` in `answer-ask` and `own-layer-made`. *Turn:* Move `invite` + first `enter` alone into a microbatch topology owning `($$claims, $$accounts, $$mb-persons, $$mb-decisions)` (~350–400 ms latency) with a `*root-events` handoff to stream.
2. **Step-1 fault bound (`overnight-audit/auth-concurrent/model_check.clj`):** Explored 2-caller and 3-caller scenarios exhaustively (`0` capped runs) with `1` injected crash/replay/timeout-overlap fault per run plus post-quiescence client re-runs (sufficient to expose 11 distinct bug classes). *Turn:* Increase fault budget to `>= 2` cascading faults per trace on a reduced state space after Revision 1 is applied.
3. **Step-3 `:could-not` write ambiguity rule (`overnight-audit/auth-concurrent/linearizability.clj`):** Allowed any write returning `{:could-not :reach-the-module}` or `{:could-not :closed}` to be linearized either as **not taken** or as **taken at or after `invoked-ns`** (even after `returned-ns`, modeling in-flight timeouts), while requiring `{:could-not :read-the-event}` on an open valid client to match `reference/step`'s `:read-the-event` form check so a broken implementation cannot pass by rejecting valid inputs as `:read-the-event`. *Turn:* Allow `{:could-not :read-the-event}` to be treated identically to `:reach-the-module` if testing a transport wrapper that conflates them.
4. **Step-3 `watch-root` assertion scope (`overnight-audit/auth-concurrent/step3_run.clj`):** Checked that after a concurrent write batch settles, every active `watch-root` listener's latest delivered value matches `(reference/live-under final-state P)` (or received `:softland.auth.client/not-reached`, or received no callback iff `live-under` ended equal to its initial value), without requiring intermediate per-step callback count equality across coalesced `MultiDiff` pushes. *Turn:* Also assert monotonic intermediate callback sequence against every prefix of the chosen linearization.

---

## Step 3 — Independent check of the revised agreement (`7b45edc0`, `spine/auth/agreement/`), including two faults in one history

### Verdict for the revision: `minor-fail`

- **What holds (all 11 Phase-1 cross-key bugs closed under both 1 fault and 2 faults in one history):** Across the full **32-scenario suite** in [`spine/auth/agreement/`](agreement/) (`25` original scenarios + `7` adversarial scenarios `X-01..X-07`), the revised `invite`, uniform choose-at-account `enter`, and `answer-ask` protocols pass **100%** under **0/1 fault (`128` runs, `97,307` states, `539` terminal histories, `0` capped runs, `0` violations)** AND under **2 faults in one history (`120` runs on those 32 scenarios + `12` runs on `TF-02..TF-04` across `:individual`, `:all-after`, `:timeout-overlap`, and heterogeneous `:mixed`, `> 1,157,000` states, `0` violations)**. Every one of the 11 Phase-1 witness classes (`one-account-two-persons`, `one-claim-two-accounts`, `claim-claiming-forever`, `claim-used-without-entered-root`, `entered-root-without-matching-account-link`, `root-reserving-forever`, `client-rerun-violates-spec` on `:says`, `final-state-or-says-violates-spec`, `no-valid-causal-linearization-of-results` on `invite`, `answer-ask-takeout-missed-on-return`, and `enter-account-link-misses-effect`) is genuinely closed.
- **Why the verdict is `minor-fail` rather than `pass`:** Adding scenario **`TF-01`** (*first-time `enter(Acc1, C1)` racing `own-layer-made(P1)` + sequential `enter(Acc1)`* — absent from both the original 25 scenarios and `X-01..X-07`) exposed **two cross-depot retry/replay flaws** at the seam between `*enters` and `*root-events` (already hit at 1 fault and confirmed at 2 faults), and code inspection against our blind-spot checklist exposed a **third** `:all-after` transient regression on `replace-secret` 2-cycles (`H2 -> H1 -> H2`):
  1. **Refused `*root-events` writes flip to accepted on `:all-after` replay after a cross-depot hop (`TF-01` `:all-after` / `:mixed`, `Witnesses 2, 3, 5, 6` in [`STEP3_TWO_FAULT_RESULTS.md`](../../overnight-audit/auth-concurrent/STEP3_TWO_FAULT_RESULTS.md)):**
     - *Trace:* `c2 = own-layer-made(:P1)` arrives on `*root-events[H(:P1)]` while `:P1` is `RootInvited`, and returns **`{:refused :no-such-person}`**. Next, `c1 = enter(Acc1, C1)` hops from `*enters` onto `H(:P1)`, writes `RootEntered(:P1, own-layer-made? = false)`, and returns `:own-layer-made? false`. Next, `H(:P1)` experiences an uncheckpointed `:all-after` replay of `*root-events`. Because `PLAN.md:200, 231` treats `*root-events` as single-task without recording refused `op-id`s, the replayed `OwnLayerMadeEvent(:P1)` executes against RocksDB's post-`enter` `RootEntered(:P1)` and **silently flips `:own-layer-made?` to `true`** — even though the only `own-layer-made` call ever sent was refused and `:P1`'s own layer was never made in the store!
     - *Same root cause across `*root-events`:* Any refused `end-actor` (`:no-such-actor`), `end-sign-in` (`:no-such-sign-in`), `begin-sign-in` (`:no-such-actor`), `make-actor` (`:no-such-maker`), or `open-session` (`:not-live`) on `*root-events` that precedes a cross-depot `enter` or `answer-ask (:yes)` creating `P`, `A`, or `G` on `H(P)` will likewise execute as accepted if `*root-events` replays from before that offset prior to stream checkpointing (`topology.stream.checkpoint.progress.threshold`).
     - *Repair:* Carry a client-minted `op-id` on `*root-events` records and record `ok:<op>` on refusal (or when refused for missing/not-live target) so `:all-after` replay replays the refusal with 0 mutations.
  2. **First-time `enter` retry after mid-flight `own-layer-made` leaves all calls reporting `made = true` (`TF-01` `:individual` / `:timeout-overlap`, `Witnesses 1 & 4`):**
     - *Trace:* `c1 = enter(Acc1, C1)` commits `RootEntered(:P1, own-layer-made? = false)` in Step E4 (`H(:P1)`), `c2 = own-layer-made(:P1)` then commits `{:made :P1}` on `H(:P1)`, `c3 = enter(Acc1, C1, at=50)` (invoked after `c2` returned) helps `c1` finish `AccountLinked` in Step E5 (`H(Acc1)`), and `c1` crashes/times out before its own Step E5. When `c1` retries from Step E1, it sees `AccountLinked`, reads `:own-layer-made?` on `H(:P1)`, and returns **`:own-layer-made? true`**. Because `c2` (`{:made :P1}`) preceded `c3` in real time, `c1` is the only call that could precede `c2` in a causal order, yet `c1` returned `:own-layer-made? true` instead of `false`. *(Low impact in the runtime spine because the runtime only calls `own-layer-made` after an `enter` has already returned `:own-layer-made? false`, but breaks standalone `SPEC.md` linearizability if `own-layer-made` races first-time `enter`.)*
  3. **`replace-secret` 2-cycle (`H2 -> H1 -> H2`) transiently regresses `$$root-data` and `$$live` during `:all-after` replay (falsifies `PLAN.md:234` with zero clock skew; invisible to `agreement/`'s step model):**
     - `PLAN.md:234` claims that on `replace-secret` retry after a newer rotation ran, `"returns :wrong-secret with no mutation"`. Under Sid's Addition 2 (`H1` can come back), if checkpointed state is `(hash = H2, prev = H1)`, Event A rotates `H2 -> H1`, and Event B rotates `H1 -> H2`, an uncheckpointed `:all-after` replay of `[Event A, Event B]` runs Event A (`old = H2, new = H1`) against `(hash = H2, prev = H1)`: the repeat check `(and (= hash H1) (= prev H2))` is false while `(= hash old-hash)` (`H2 == H2`) is **true**, so Event A **mutates `G` backward to `H1`** in both `$$root-data` and `$$live` (emitting a backward `watch-root` push and causing any concurrent `who-is` read between Replay A and Replay B to reject current secret `H2`) before Event B replays `H1 -> H2`.

### Commands and results run for Step 3

1. **Unchanged 1-fault check in `spine/auth/agreement/`:**
   - **Command:**
     ```bash
     cd src/proposal/runtime-2026-11-21/spine/auth/agreement && \
     clojure -Srepro -Sdeps '{:paths ["src" "../../../overnight-audit/auth-concurrent/src"] :deps {org.clojure/clojure {:mvn/version "1.12.4"}}}' -J-Xmx4g -M -m agreement.check
     ```
   - **Output:** `Summary: runs=128 states=88,261 terminals=539 capped=0 violations=0 elapsed=3360ms`

2. **Extended 0/1-fault + 2-faults-in-one-history check (`:fault-budget 2`, including `:mixed` cross-mechanism fault pairs and `TF-01..TF-04`):**
   - Enhanced [`overnight-audit/auth-concurrent/src/auth_concurrent/model_check.clj`](../../overnight-audit/auth-concurrent/src/auth_concurrent/model_check.clj) to distinguish `:active?` attempts on queued channel hops in `world-signature`, accept configurable `:fault-budget`, and support `:mixed` mode (injecting any combination of `:individual`, `:all-after`, and `:timeout-overlap` within one history), and added [`step1_two_faults.clj`](../../overnight-audit/auth-concurrent/src/auth_concurrent/step1_two_faults.clj) (full output and witness tables in [`STEP3_TWO_FAULT_RESULTS.md`](../../overnight-audit/auth-concurrent/STEP3_TWO_FAULT_RESULTS.md)).
   - **Command:**
     ```bash
     cd src/proposal/runtime-2026-11-21/overnight-audit/auth-concurrent && \
     clojure -Srepro -Sdeps '{:paths ["../../spine/auth/agreement/src" "src"] :deps {org.clojure/clojure {:mvn/version "1.12.4"}}}' -J-Xmx6g -M -m auth-concurrent.step1-two-faults 250000
     ```
   - **Output summary:**
     ```text
     === SUITE 1 (0/1-fault over 32 agreement scenarios × 4 modes = 128 runs) ===
     states=97,307 terminals=539 capped=0 violations=0 elapsed=3588ms

     === SUITE 2 SUMMARY (2 faults in one history over 36 scenarios, 136 runs) ===
     runs=136 states=1,188,828 terminals=1,910 capped=1 violations=6 elapsed=50553ms
     - Original 25 + X-01..X-07 + TF-02..TF-04 (132 runs): 0 violations across all Q1..Q5!
       (Sole capped cell: 3C-05 :timeout-overlap 2-fault at 250,000 states, 0 violations.)
     - TF-01 (4 runs): 6 hits across 2 distinct mechanisms (Witnesses 1–6 in STEP3_TWO_FAULT_RESULTS.md;
       also reproduced at fault-budget=1 in 323 / 248 / 892 states).
     ```

### What `spine/auth/agreement/`'s steps (and `PLAN.md`) do that our checker cannot see

1. **Where `op-id` is minted (`client` envelope vs inside stream topology):** `model_check.clj` attaches a fixed `:op-id` per depot record and preserves it across `:individual`, `:all-after`, and `:timeout-overlap` retries (`model_check.clj:477, 505, 548`). The checker cannot see whether the real Rama code mints `op-id` in `softland.auth.client` before `foreign-append!` (`PLAN.md:521, 574`) rather than calling `(UUID/randomUUID)` inside `"auth"` — which would mint a fresh `op-id` on every retry and completely defeat `$$enter-ops[op]` and `ok:<op>`.
2. **Dead `ClaimClaiming` / `EnterOpWon` / `EnterOpTryEmail` branches in `agreement/plan_steps.clj` (`:727–755, :807–819, :895–932`) that do not exist in `PLAN.md`:** `agreement/plan_steps.clj` keeps 75 lines of CAS resolution for `ClaimClaiming` (`:enter/step-2-resolve-claiming` and `:enter/step-2-after-resolve`) solely so pre-seeded states in scenarios `3C-06` and `2C-15` execute cleanly, whereas `PLAN.md:145, 166, 522` deletes `ClaimClaiming`, `EnterOpWon`, and `EnterOpTryEmail` from the schema completely. Our checker sees `3C-06`/`2C-15` pass via helper code that is absent from `PLAN.md`.
3. **Five `*root-events` writes omitted from `execute-step` (`begin-sign-in`, `replace-secret`, `end-sign-in`, `open-session`, `close-session`):** `agreement/plan_steps.clj:1484–1502` implements `seed-*` helpers for three of these and omits `replace-secret`, `end-sign-in`, and `close-session` completely from step execution. Consequently, the checker cannot see:
   - The **`replace-secret` 2-cycle (`H2 -> H1 -> H2`) `:all-after` replay regression** derived above (nor intermediate `who-is` / `watch-root` snapshots between replayed events);
   - **Wall 3** backward-clock `:all-after` take-out replay on `*root-events` (`PLAN.md:811–813`);
   - Cross-depot `:all-after` replay of refused `begin-sign-in`, `open-session`, and `end-sign-in` after `answer-ask (:yes)` creates `G` or `A`.
4. **Mid-flight cross-partition reads before `(ack-return> ...)` (`Wall 1`, `PLAN.md:803–805`):** `model_check.clj` runs `Q5` read checks only when an operation emits `ack-return` (`:427–437`) and at quiescence (`:625–637`), never between hops. Thus it cannot see that between `enter` Step E4 (`H(P)`) and Step E5 (`H(Acc)`), `who-is(P)` already returns `:own-layer` while `$$accounts[Acc]` is still `AccountLinking`, or that between `answer-ask (:yes)` Step 2 (`H(P)`) and Step 3 (`H(U)`), `who-is`, `under`, and `watch-root` already expose the new agent `A` and live sign-in `G` while `ask-of(U)` still reports `:state :waiting`.
5. **Hidden fields inside `$$live` vs `watch-root` wire transport and diff suppression (`agreement/plan_steps.clj:202–217, 315–320, 352–353`):** To avoid point seeks on `g:`/`s:` during 2-scan `end-actor` and take-outs, `LiveSignIn` and `LiveSession` in `$$live` store `:driven-by`, `:initial-hash`, `:begun`, `:label`, `:seq`, `:own-layer`, and `:own-layer-made-at-begin?`, which `SPEC.md:237–240` forbids `watch-root` from delivering. `plan-live-under` strips those keys in pure Clojure, so the checker cannot see that `foreign-proxy-async` transmits them over the wire or whether `softland.auth.client` strips every hidden key and suppresses value-equal post-open `ResyncDiff`s (`PLAN.md:699`).
6. **Unbounded `N`-caller helping chains (`:rounds > 8` in `model_check.clj:402`) and Rama `loop<-` / `ops/explode` execution:** `agreement/plan_steps.clj` increments `:rounds` on helper retries (`:543, :941, :1023, :1040, :1392`) and our checker explores 2–3 callers (`rounds <= 2`), flagging `:infinite-loop?` at `rounds > 8`. It cannot verify stack safety under `> 8` simultaneous contending callers on one key or physical Rama dataflow compilation of flat `ops/explode` in 2-scan `end-actor`.
7. **Unbounded growth of `$$enter-ops` under refused-`enter` floods (`PLAN.md:166–168`):** Every refused `enter` (including unauthenticated/uninvited no-claim attempts if any reach Auth) writes a permanent `EnterOpFailed` entry to `$$enter-ops[op-id]` that is never deleted. Our finite-trace checker checks Q1–Q5 correctness and cannot see multi-year `$$enter-ops` key growth.
8. **Client-edge validation, `ping-task` 8 s liveness monitor, `close!` callback locking, and Rama schema/key serialization:** Input form checks (`:read-the-event`, lowercase `Locale/ROOT` email, `fn?` on `watch-root`, non-client guards), client timeouts, `ProxyState` callback threading, and 7-bit ASCII key-range ordering in RocksDB live outside `agreement/plan_steps.clj`.
