# Comparison — This Retro vs. the Prior Retro (`build/rama-retro-review/`)

Written 2026-06-11, AFTER all five tracks' defect-finding completed and findings were locked on disk (independence preserved; the prior retro was read only for this comparison). Answers: "did the prior retro miss things we are finding?" — **yes, substantially — and the inverse is also true.** The two methods are strongly complementary.

## The two methods

| | Prior retro (`build/rama-retro-review/`) | This retro (`docs/retros/rama/`) |
|---|---|---|
| Phase artifacts | PHASE_RECONSTRUCTION from committed docs | Blind IMPLICIT_SPEC + re-derived PLAN (validated) per track |
| Review engine | Review-lens checklist + Phase-4 style matrix | Official skill phase-4/6 docs run verbatim, adversarial default-fail |
| **Execution** | **Runtime probes against IPC** (adversarial appends, load checks) | Static code-tracing only |
| Findings/track | ~5–6 | ~10–21 |
| Output | RAMA_REVIEW per block | SPEC/PLAN/validations/FINDINGS/FIX_PLAN per track |

## What the prior retro MISSED (found only by this retro)

The entire **cross-partitioner partial-commit class** is absent from the prior reviews (their "Partial Failure" checks either passed narrowly or said "unproven"):

- compute C-02: grant/inbox partial commit → permanent stale inbox + infinite 50ms claim-append loop
- space SP-01: compose retry after the mid-tree idempotency write → catalog + relations + LLM dispatch **permanently lost**
- space SP-04: turn-order read→hop→write race drops turns permanently
- kernel K-01: 5 independent `|hash` hops per request commit → racing status-set wrongly rejected; head regression on retry
- transcript TR-01: ledger-write-then-retry loses conversation/tool/counter writes

Plus: compute C-03 (nil-token authorization bypass — their compute review never tried a tokenless observation against a `:pending` run), llm terminal-status regressions (4 sites) + undeliverable mid-turn cancel + executor restart impossibility, compute C-13 (close leaks processes / fabricates exit-127 truth), the whole **boundedness family** (admission size caps, per-line byte caps, store-if-absent buffered duplicates — produced by our R2 adversarial plan loop), quantified write amplification (×2 PStates per output line), and per-track **validated reference PLANs** + the C1–C19 contract-enforcement scorecard that fix sessions can build against.

## What THIS retro missed (found only by the prior retro — all runtime-proven)

1. **`app.server.rama.kernel` does not load** (their 06/F1, critical). Nobody in our pipeline ran `require`. The file we treated as the contract spec artifact doesn't compile.
2. **compute unknown-run observation = fatal NPE poison record**, not a ghost row (their 02/F1, critical). Our R4/R7 and the direct review all statically mis-traced `fold-observation` — the eager `(long (:last-seq nil))` let-binding throws before any guard. **Corrected in `01-compute/FINDINGS.md` C-05 (MEDIUM → HIGH).**
3. **llm observations carry no claim proof at all** (their 04/F1, critical, probe: a claimless observation flipped a `:pending` run to `:running`). Our T2 artifact covered adjacent surfaces (token wiped by replay; executor-side verification) but not the naked topology-side auth hole itself.
4. **space non-patch observations become pending patch proposals** (their 03/F1, high — `append-llm-observation!` unconditionally appends proposal-create; `patch-proposal-observation?` exists but is unused). We caught the same helper's two-append atomicity flaw (SP-05) but not its semantic leak.
5. **Idempotency is conflict-blind** (their 03/F3 — same key, different material → silently aliased to the first request's decision). We caught the check/write race (SP-03) but not the aliasing semantics.
6. **UTF-8 transcript rows corrupt byte identity** (their 05/F2, high). We flagged multi-byte coverage only as a test gap (TT-04); they proved the corruption.
7. Transcript: harvest accumulates whole-file observations instead of streaming appends (05/F3); obs depot partitioned by request rather than source file (05/F4) — we had only the adjacent "single-task funnel" note.

## Where both converged (independent confirmation — highest-confidence findings)

- Duplicate/replayed request resets terminal state → second physical side effect (every module: compute C-01≡02/F2, llm L-01≡04/F2, kernel K-02/03≡01/F1, transcript TR-02)
- Approval resolution invents missing approvals (L-07 ≡ 04/F3 + 03/F2)
- `{String Object}` schemas, non-subindexed unbounded collections, whole-map reads (all tracks)
- `util_fns` atom mirrors as unrebuildable UI truth (K-07 ≡ 01/F2)
- Redaction not real in transcript (TR-03 ≡ 05/F1, their probe proved persisted raw bytes)
- Tests prove the happy spine only (every track)

## Method verdict

- **Static falsification finds more classes; runtime probing finds harder truths.** Our pipeline's count advantage (~3–4×) is real — the partial-commit family alone justifies it. But every one of the prior retro's six unique findings was something static tracing got *wrong or couldn't see*, including one mechanism error in our own HIGH-confidence artifact.
- **Lesson codified:** a future retro (or fix-validation) should add an explicit probe phase — convert every HIGH finding into an adversarial IPC probe (their technique) before and after the fix, and `require` every namespace under review as step zero.
- The fix sessions inherit BOTH repair queues: ours (FIX_PLAN.md per track, plan-anchored) and theirs (Recommended Repair Queue per block, probe-anchored). They agree on priorities where they overlap.
