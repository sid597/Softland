# LM-1 run 127376b0 — raw results (graded 2026-07-06)

Instrument: closed-book A0 (73,279-token bundle), 32 questions, deviations
D1–D5 per snapshot MANIFEST. Grader: Opus subagents, frozen rubric
(RUNBOOK). Thresholds NOT applied — SITTING-SETS.

| Metric | nemotron-30b-a3b-q8-128k | opus-4.8-cc-harness |
|---|---|---|
| Correct | **17/32 (53%)** | **26/32 (81%)** |
| Partial | 1 | 3 |
| Wrong | 5 | 3 |
| Never-committed | **9** (8 empty: thinking ate 4096-tok budget) | 0 |
| Wrong-authority | 1/32 | 0/32 |
| Rows with invented structure | 3/32 | 0/32 |
| Spine-gated J-honesty (gold=refusal) | 0/6 | 1/6 |
| Committed rows / mean offset | 18/32, ~6 words | 27/32, ~6 words |
| Re-derivation mean | 0.21 | 0.30 |

## Observations for the sitting (not verdicts)
1. **Head-to-head is decisively frontier** — pre-registered KILL clause (a)
   (local invented-structure ≥2× frontier) is formally met (3 vs 0), though
   the absolute local invention rate is low (9%).
2. **Nemotron's dominant failure is never-committing (9/32), not lying.**
   Likely serving-config-repairable (max_tokens 4096 too small for its
   thinking; see LM-1b autopsy — reasoning channels are on disk).
3. **The interaction term (LM-1 clause b, A0→ablated delta) is UNMEASURED**
   — A3 deferred (byte-identical to A0 at this sha). LM-1 cannot fully
   resolve until relations/ exists.
4. **Both subjects failed the honesty questions** (0/6 and 1/6): even the
   frontier mostly attempted join answers instead of refusing. Partly a
   land ambiguity — docs mention commits, so J-questions feel derivable.
   ALL J rows are in the human-audit queue.
5. Human-audit queue (RUNBOOK §3): all 12 J rows + nemotron's 3 invented
   rows + 15% sample (every 7th row, seed 0).

Grades: `grades/<subject>/<qid>.edn` · answers: `nemotron-raw/`, `cc-raw/`
· nemotron reasoning channels: in the .ednl rows (`:reasoning`).
