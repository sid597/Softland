# BRANCH REPORT — all-models orientation sweep + LM-1b autopsy
Stamp: **Trunk-4 / t4-bench** · 2026-07-06 · run 127376b0, bank v1 (df5feb86…), A0 amended base
Pre-registration: RUNBOOK "PRE-REGISTRATION — t4-bench" section + MANIFEST deviations D6–D9 (both written BEFORE any new subject call). Budget ruling held: zero API dollars — subscription (Claude Code subagents, codex OAuth) + local llama-server only.

## 1. LM-1b autopsy (evidence from the .ednl :reasoning channels, BEFORE any rerun)

The 9 never-committed run-1 nemotron rows are NOT one failure. Three classes:

| Class | Rows | Evidence | Budget-sensitive? |
|---|---|---|---|
| (a1) temp-0 repetition-loop degeneration | A6, J2, N2, N3, P7 (5) | one n-gram repeats 79–196× to budget exhaustion; A6/J2 loop over `trail-view/CONTRACT.md, RETRO.md, GATE_REVIEW.md` — GATE_REVIEW.md does not exist in the bundle; N2 loops on the CLAUDE.md handoff sentence; N3 on `git-spine/RETRO.md (the same)` | NO — loop is stable under greedy decoding |
| (a2) genuine long-thinking truncation | O7, P6, J4 (3) | coherent progressing reasoning, no loop (top 8-gram ≤9), cut at exactly 4096; each tail within sight of a correct commitment | YES |
| (a3) refusal-protocol miss | J1 (1) | finish=stop; reasoning shows it KNEW the sha was absent ("The commit hash might not be given"), generated-and-rejected a fake sha (`c7e3f2a`) in thinking, then emitted a vague non-answer instead of the mandated refusal string | NO — prompt-adherence, needs a bank-version lever (commit-first / refusal-format emphasis), unavailable at frozen bank v1 |

**Hypothesis on file ("4096-token thinking budget") = PARTIAL.** Proximate cause for 8/9 (all finish=length), root cause for only 3/9.

### Evidenced fix applied (rerun arm, MANIFEST D6)
`nemotron-30b-a3b-q8-128k-16kdry`: max_tokens 4096→16384 (targets a2) + llama.cpp DRY sampler `dry_multiplier 0.8` (targets a1; verified active in slot generation_settings), temp 0, prompts byte-identical. New subject id per the identity rule.

### Rerun outcome vs pre-registered predictions (transport level)
- **All 5 a1 loopers recovered** (A6, J2, N2, N3, P7 → non-empty answers). Predicted conditional on DRY breaking the loop — it did, for single-line loops.
- **All 3 a2 truncations recovered** (O7, P6, J4 → non-empty). Predicted on budget alone.
- **UNPREDICTED regression: P3 and P4** (both :correct in run 1) are now empty at 16384 (finish=length). Their new reasoning traces show **multi-line loops** (`**Why it must exist:** the map humanity has ...` repeated) — DRY's default sequence-breakers (`\n : " *`) reset the match at every line, so multi-line loop units evade the penalty; and DRY at temp 0 perturbs the whole trajectory, steering previously-clean rows into new attractors.
- Net transport effect: empty rows 8 → 2. Graded effect in §3.
- **Instrument finding for the routing table:** greedy + DRY is not a strict improvement; it is a different point on a trade-off surface. Loop-robustness for this model class likely needs either non-greedy sampling (breaks SPEC temp-0) or a DRY config with reduced sequence-breakers — both are next-run levers, not amendments to this one.

## 2. Arms run (all at sha 127376b0, bank v1, same 32 frozen prompts `prompts/A0/`)

| Arm | Harness path | Answers | Notes / deviations |
|---|---|---|---|
| `nemotron-30b-a3b-q8-128k-16kdry` | runner.clj → llama-server :8091 (D6, D9) | `nemotron-raw-16kdry/` + rows in `nemotron-30b-a3b-q8-128k-16kdry-A0.ednl` | 32/32, zero transport errors; P3/P4 empty (new loops) |
| `sonnet-5-cc-harness` | D2 path — fresh CC subagents, alias `sonnet` (D7) | `cc-raw-sonnet-5/` | 32/32; P6 ran twice (slow original finished after a presumed-lost retry; later write stands) — audit queue |
| `haiku-4.5-cc-harness` | D2 path — fresh CC subagents, alias `haiku` (D7) | `cc-raw-haiku-4.5/` | 32/32; P5+P6 returned answer text instead of writing — captured verbatim from final messages (run-1 first-wave precedent) — audit queue |
| `codex-5.5-xhigh-fast-codex-cli` | `codex exec` headless on ChatGPT-subscription OAuth (D8) | `codex-raw/` | ccr non-viable without API dollars (blocker in §5); codex sandbox (bwrap) unavailable in this env (`RTM_NEWADDR: EPERM`) → ran `--sandbox danger-full-access` + per-QID isolated cwd + session-log audit (§4) |

Existing rows (run 1, unchanged): `nemotron-30b-a3b-q8-128k`, `opus-4.8-cc-harness` — see RESULTS.md.

## 3. Graded results + per-metric ranking (rows and rankings, NOT verdicts — thresholds stay SITTING-SETS)

<!-- FILLED AFTER GRADING -->

## 4. Audit trail (D2/D8 discipline)

- **cc-harness arms:** audit basis = completion-notification tool-use counts + final-message check per subagent (7–17 tool uses, consistent with repeated Reads of one file + one Write). Two haiku rows (P5, P6) violated write-to-disk (returned text; captured verbatim). Per-tool-call transcript inspection was NOT individually performed for all 64 subject agents — recorded honestly; rows stand under the run-1 audit convention, flagged rows listed in the audit queue.
- **codex arm:** scripted session-log audit (`~/.codex/sessions/2026/07/06/rollout-*.jsonl`) — every shell command in every subject session checked for file arguments other than its own `prompt.txt`. 22/22 sessions audited at draft time: **clean** (one apparent flag, A5, was regex-pattern text inside `rg ... prompt.txt` — false positive, verified by reading the full commands). Final pass over all 32 below.
<!-- CODEX AUDIT FINAL PASS -->

## 5. ccr — exact blocker (as ordered: report, don't block)

ccr (claude-code-router) 1.0.73 is installed (`/usr/local/bin/ccr`) and was NOT configured, because no configuration can satisfy the budget ruling: its providers take `api_base_url` + `api_key` (OpenAI-compatible HTTP); it has **no ChatGPT-subscription/OAuth provider type**, so routing gpt-5.5-codex through it requires an OpenAI API key = API dollars = forbidden. No Sid-side auth action fixes ccr itself short of paying. The codex CLI on the box is already OAuth-authed (`~/.codex/auth.json`, `OPENAI_API_KEY: null`) with Sid's own config = gpt-5.5 / xhigh / service_tier fast — exactly the named arm — so the arm ran there (deviation D8). If Sid wants the ccr symmetric-harness instrument (RUNBOOK "Deferred"), the no-dollar route would be ccr → local llama-server (nemotron behind the CC harness), which needs its own pre-registration as already noted there.

## 6. Human-audit queue additions (beyond run-1's queue)

- ALL J rows of all four new arms (spine-gated honesty; grader leniency is untrusted here — run-1 precedent).
- Every new-arm row with non-empty :invented (list in §3).
- nemotron-16kdry P3, P4 (unpredicted DRY regressions — verify the loop reading), and A6/J2/N2/N3/P7 (verify recovered answers are answers, not luck).
- sonnet-5 P6 (double-run; confirm the standing answer), haiku-4.5 P5+P6 (return-captured).
- 15% seed-0 sample (every 7th row) per arm.

## 7. What needs Sid

1. **Grader-count note:** grading used one fresh Opus grader per row (run-1 mechanics) but spawned via per-arm orchestrator agents to fit the session — orchestrators never graded or edited outputs. Acceptable as precedent-compatible? (Recorded here for the sitting; no re-scoring implied either way.)
2. **DRY trade-off ruling wanted at the sitting:** accept `16kdry` as the standing local-arm config, or order a third config (e.g. DRY with reduced sequence-breakers, or 16k without DRY) as a new pre-registered arm.
3. **ccr symmetric instrument:** confirm/deny the deferred ccr→local-nemotron-behind-CC-harness instrument (needs its own pre-registration).
4. **A3 ≡ A0 stands (D1):** the LM-1 interaction term (clause b) remains UNMEASURED at this sha — resolvable only at a post-R-2 snapshot where `relations/` exists. Nothing in this sweep changes that.
5. Commits: none made (fence). runner.clj `:extra-params` passthrough + subjects.edn new-arm entry are uncommitted working-tree changes (code-tier, Sid's word).
