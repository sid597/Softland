# Auth — Revised Cross-Key Agreement (Model-Checked)

This directory holds the revised multi-task stream step specification for `Auth`'s three cross-key writes (`invite`, `enter`, and `answer-ask`) together with `who-is` (2-seek fast path) and `end-actor` (2-scan cascade), encoded in the exact shape of `overnight-audit/auth-concurrent`'s `auth-concurrent.plan-steps` so carbon's unchanged model checker (`model_check.clj`), sequential oracle (`spec_oracle.clj`), and 25 scenario suite (`step1_run.clj`) run over it without modification.

## What changed from the Phase-1 plan

1. **`invite` (Revision 1):**
   - **Cleanup on Step-1 `:claim-in-use` (Witness 6):** When `$$claims[C]` already names another person (`!= P`), `invite` hops `|hash P` to clear any orphaned `RootReserving(C)` (if still `C`) before returning `{:refused :claim-in-use}`.
   - **Recheck `C` before `:person-in-use` (Witness 9):** Before returning `{:refused :person-in-use}` from `H(P)` (either on `RootInvited`/`RootEntered` with another claim, or after helping a competing `RootReserving(C0)` commit to `RootInvited`), `invite` hops `|hash C` to re-check `$$claims[C]`. If `C` now names another person, it returns `{:refused :claim-in-use}`; if `C` names `P`, it hops to Step 4; only if `C` is still `nil` does it return `{:refused :person-in-use}`.

2. **`enter` (Revisions 1 & 5):**
   - **Uniform choose-at-account protocol (zero `ClaimClaiming`, zero uncoordinated 1-claim fast path):**
     - `AccountLinking` on `$$accounts[Acc]` carries `:status` (`:probing` | `:chosen` | `:failing`) and `:chosen` (`ClaimKey` or `nil`).
     - Step E2 (`H(C)`) is strictly **read-only** (never writes `ClaimClaiming` or mutates `$$claims`). It probes `:code` then `:email`; if `:code` was `nil` and `:email` was `ClaimUsed`, it re-checks `:code` once so `:not-invited` always has a single-instant linearization point (`X-01`).
     - Step E2' (`H(Acc)`) is the single-threaded serialization point between choosing a claim (`:probing -> :chosen(C)`) and failing (`:probing -> :failing`).
     - Step E2c (`H(op)`) uses PII-free `$$enter-ops[op-id]` strictly to record `EnterOpFailed` on refusal (never `EnterOpWon` or `EnterOpTryEmail`). Every `:chosen(C)` path checks `H(op)` before mutating `$$claims[C]` in Step E3, and every `:failing` path writes `EnterOpFailed` on `H(op)` *before* deleting `AccountLinking(op)` on `H(Acc)` in Step E2d. Because `:failing` can never transition to `:chosen` and `:chosen` can only return to `:probing` after observing permanent `ClaimUsed(*, other-op)`, no `op-id` can both fail and claim a claim — sealing `:all-after` stream replay of refused enters (`2C-04`, `3C-03`, `X-04`, `X-06`).
     - Step E3 (`H(chosen)`) transitions `ClaimWaiting(P)` directly to `ClaimUsed(P, op)`. If lost to `ClaimUsed(*, other-op)`, Step E3b strictly CAS-clears `:chosen` from `failed-claim` back to `:probing`.
   - **Monotonic `:says` retention (Sid's Call 2 — Witnesses 7, 8, `2C-16`):** `$$accounts[Acc]` updates `:says` and `:says-at` **only** when `(> event.at account.says-at)`. Any returning or helping enter with `(> at says-at)` first checks `$$enter-ops[self-op]` on `H(self-op)` so a replayed refused high-`:at` enter never overwrites `:says` (`X-04`, `X-06`).
   - *(Legacy compatibility for carbon's pre-seeded scenarios `3C-06` and `2C-15`: `eval-probe-on-claim` and `eval-use-on-claim` still resolve any pre-seeded `ClaimClaiming` via strict CAS against `$$enter-ops[other-op]` into `ClaimUsed` or `ClaimWaiting`, even though the revised protocol itself never writes `ClaimClaiming`.)*

3. **`answer-ask` (Revision 4):**
   - **Check `ok:<op>` before take-out:** Step 2 on `H(P)` checks `$$root-data["<P>\0ok:<op>"]` *before* running `take-out(P, ans.at)` so replays and helpers never re-run stale take-outs. Step 1b on `{:refused :no-such-ask}` likewise records `AskOutcomeRefused(:no-such-ask)` on `H(P)` so `:all-after` replay after a later `ask(U)` never flips `:no-such-ask` to accepted (`X-05`).
   - **Always run caller's take-out after helping (Witness 10):** In Step 3 on `H(U)`, a helper (`op0 != self-op`) on `AskOutcomeYes` or `AskOutcomeNo` always hops `|hash (:root self-input)` to run take-out at `(:at self-input)` before emitting `(ack-return> ...)`, even when `(:root self-input) == (:root target)`.

## How to run carbon's unchanged checker over this agreement

Placing `src/` ahead of `../../../overnight-audit/auth-concurrent/src` on the classpath loads our revised `auth-concurrent.plan-steps` directly under carbon's unchanged `model_check.clj`, `spec_oracle.clj`, and `step1_run.clj`:

```bash
cd /mnt/data/projects/Softland/src/proposal/runtime-2026-11-21/spine/auth/agreement && \
clojure -Srepro -Sdeps '{:paths ["src" "../../../overnight-audit/auth-concurrent/src"] :deps {org.clojure/clojure {:mvn/version "1.12.4"}}}' -J-Xmx4g -M -m agreement.check
```

## Summary of results (`RESULTS.md`)

- **Scenarios × modes:** **32 scenarios** (all 25 of carbon's scenarios + 7 extra adversarial scenarios `X-01..X-07`) × **4 fault/retry modes** (`:pure`, `:individual`, `:all-after`, `:timeout-overlap`) = **128 runs** (`3,421 ms`).
- **Client re-runs checked:** in **all 4 modes** (`check-reruns? true`).
- **States explored:** **88,261** deduplicated `(state, queues, fault-budget)` configurations (**0 capped runs**).
- **Quiescent terminal histories checked:** **539**.
- **Breaks across all 5 questions:** **0** (Q1 = 0, Q2 = 0, Q3 = 0, Q4 = 0, Q5 = 0).

Full per-scenario table is in [RESULTS.md](RESULTS.md).
