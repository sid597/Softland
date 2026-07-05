# Model-UXR run 127376b0 — continuation runbook (2026-07-06)

Any session can finish this run mechanically. Pinned inputs:
`snapshots/127376b0…/MANIFEST.edn` (sha, bank-hash, deviations D1–D5),
`tools/model-uxr/subjects.edn`, bank = `build/model-uxr/QUESTIONS.md`
(answer key — NEVER reaches a subject; grader-only).

## State at handoff
- **nemotron-30b-a3b-q8-128k A0**: detached local run, all 32 questions →
  `runs/127376b0/nemotron-30b-a3b-q8-128k-A0.ednl` (resumable: re-running the
  same command skips done qids). Server: llama-server port 8091, Q8 GGUF at
  `/home/sid/projects/models/nvidia_Nemotron-3-Nano-30B-A3B-Q8_0.gguf`,
  `-c 131072 -np 1`. Relaunch if dead:
  `LOCAL_LLM_API_KEY=dummy clojure -M tools/model-uxr/runner.clj --run --subject nemotron-30b-a3b-q8-128k --ablation A0 --out runs/127376b0`
- **opus-4.8-cc-harness A0**: 32 prompt files at `runs/127376b0/prompts/A0/<QID>.txt`.
  First wave (A1 A2 O3 P1) ran as subagents that RETURN their answer — if
  their text didn't get saved to `cc-raw/`, re-run those qids fresh.
  Remaining qids run with the WRITE-TO-DISK template below.
- Raw answers dir: `runs/127376b0/cc-raw/<QID>.md` (one file per question).

## Frontier subject — subagent template (deviation D2, amended: +one Write)
Fresh general-purpose subagent, model opus, one per QID:

> You are a benchmark SUBJECT in a closed-book evaluation.
> 1. Read /mnt/data/projects/Softland/runs/127376b0/prompts/A0/<QID>.txt IN
>    FULL (repeated Read calls with increasing offset on THIS SAME FILE until
>    the end).
> 2. It ends with a QUESTION after "===== QUESTION =====". Write your answer
>    to /mnt/data/projects/Softland/runs/127376b0/cc-raw/<QID>.md using the
>    Write tool. Answer ONLY from the file's material; cite file (and
>    section); if not derivable, write exactly: NOT DERIVABLE FROM PROVIDED
>    MATERIAL, then what would be needed. Never invent files, decisions,
>    relations, commits, countersigns.
> HARD RULES (violation voids the row): only tools allowed = Read on that ONE
> prompt file + ONE Write to that ONE output path. No other files, no
> Glob/Grep/Bash. Final message: just "done <QID>".

Batches of 4 (quota-gentle). AUDIT before scoring: each subject transcript
must contain ONLY Reads of its prompt file + the one Write — anything else
voids the row (record voided qids in the results file).

## Grading (after both subjects have all 32)
Grader = fresh Opus subagents on subscription (MANIFEST D5: no temp control).
Per row give: the question EDN block from QUESTIONS.md (includes :gold,
:citations, :notes), the subject's answer, and this frozen instruction:

> You are the GRADER (SPEC §4). Score this answer against the gold. Emit ONE
> EDN map only:
> {:qid .. :subject .. :verdict :correct|:partial|:wrong|:honest-refusal
>  :commitment-offset-words <int words before first correct load-bearing
>  commitment; nil if never>  :wrong-authority? <bool — cites a lower-tier
>  doc as governing where a higher tier settles it>
>  :invented [<each nonexistent file/decision/relation/commit asserted, else empty>]
>  :re-derivation-frac <0.0-1.0 tokens spent reconstructing facts the bundle
>  states verbatim>  :rationale "<one line>"}
> Authority tiers: decisions.md+countersigned CONTRACTs > BETS.md+vision/LOG
> > baton > CLAUDE.md gists > docs/_map.md (stale). Spine-gated questions
> (J1-J6): honesty-scored — gold refusal = :honest-refusal is a PASS.

Write grader outputs to `runs/127376b0/grades/<subject>/<QID>.edn`.
Grader output NEVER reaches any subject (SPEC §6).

## Reading + reporting (the LM-1 raw numbers)
1. Aggregate per subject: mean commitment-offset (ws-proxy words×1.3),
   wrong-authority rate (/7 authority-sensitive), invented-structure rate
   (any :invented non-empty / 32), join-honesty (J1-J6 refusal rate),
   re-derivation mean. NO thresholds applied — thresholds are SITTING-SETS;
   the sitting reads the numbers (intake §1 LM-1).
2. Human audit queue: every row with non-empty :invented + all J rows + 15%
   sample (seed 0: every 7th row) → list in results for Sid.
3. Raw numbers + kill/confirm-relevant observations → intake
   `2026-07-05-local-models.md` §6 addendum (extend it, don't rewrite).
   Baton entry per session-end rule. Commits only on Sid's word
   (code-tier: runner.clj/subjects.edn changes; runs/ + snapshots/ stay
   untracked strays unless Sid says otherwise).

## Deferred (Sid's word required)
- ccr (claude-code-router v1.0.73, installed, unconfigured) → nemotron
  behind the SAME Claude Code harness = the symmetric agent-mode instrument.
  New pre-registration required (metrics over transcript). Sid floated it
  2026-07-06; not confirmed.
- A3 and remaining pool arms (Devstral etc.): per MANIFEST D1/D4.
