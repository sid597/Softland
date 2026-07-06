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

---

## PRE-REGISTRATION — t4-bench all-models sweep + LM-1b rerun (2026-07-06)

Registered BEFORE any new subject call (freeze-gate discipline). Bank v1
(hash df5feb86…), snapshot sha 127376b0, ablation A0 amended base — the
SAME 32 prompt files at `prompts/A0/<QID>.txt` every prior arm saw. Grader
= frozen rubric above, Opus subagents, one grader call per row (run-1
precedent), grades to `grades/<subject>/<QID>.edn`. Thresholds stay
SITTING-SETS. Budget ruling stands: NO API dollars — subscription +
local endpoints only. Deviations D6–D9 appended to snapshot MANIFEST.edn.

### LM-1b autopsy result (evidence, from the .ednl :reasoning channels)
9 never-committed rows split into THREE classes, not one:
- **(a1) temp-0 repetition-loop degeneration — 5 rows** (A6, J2, N2, N3,
  P7): a single n-gram repeats 79–196× until the 4096 budget exhausts
  (A6/J2 loop on `trail-view/CONTRACT.md, RETRO.md, GATE_REVIEW.md` — a
  file that does not exist in the bundle; N2 loops on "the active work
  handoff is next-prompt.md"; N3 on `git-spine/RETRO.md (the same)`).
  finish=length, completion empty. More budget would NOT fix; the loop is
  stable under greedy decoding.
- **(a2) genuine long-thinking truncation — 3 rows** (O7, P6, J4):
  coherent, progressing reasoning cut at exactly 4096 tokens (top 8-gram
  count ≤9 = no loop); each tail is within sight of a correct commitment.
  More budget plausibly fixes.
- **(a3) refusal-protocol miss — 1 row** (J1, finish=stop): the model
  correctly detected the commit sha is absent ("The commit hash might not
  be given"), even generated-and-rejected a fake sha in thinking, then
  emitted a vague non-answer instead of the mandated refusal string.
  Neither budget nor sampling fixes this; it is prompt-adherence.
Hypothesis on file ("4096-token thinking budget") is therefore PARTIAL:
proximate cause for 8/9, root cause for only 3/9.

### New arms (4)
1. **`nemotron-30b-a3b-q8-128k-16kdry`** — the LM-1b evidenced-fix rerun.
   Same GGUF, port 8091, `-c 131072`, temp 0, SAME frozen prompts. Changes:
   `max_tokens` 4096→16384 (fix for a2) + llama.cpp DRY sampler
   `dry_multiplier` 0.8 (fix for a1; targets long-n-gram loops without
   distorting legit path reuse). Pre-registered fallback: if this
   llama-server build rejects DRY params (neutral smoke check), use
   `repeat_penalty` 1.15 instead and record which was used. Harness:
   `runner.clj --run` with an `:extra-params` passthrough added to
   `call-openai-compatible` (code change in tools/model-uxr/, working
   tree, uncommitted per no-commit fence).
   **Pre-registered predictions:** a2 rows (O7, P6, J4) recover with
   budget alone; a1 rows (A6, J2, N2, N3, P7) recover only if DRY breaks
   the loop; a3 (J1) does NOT recover (prompt unchanged — the prompt is
   frozen with the bank, so the commit-first/refusal-emphasis fix is a
   FUTURE bank-version lever, not available at bank v1).
2. **`sonnet-5-cc-harness`** — D2 path exactly (fresh Claude Code
   subagents, subscription), Agent-tool model alias `sonnet`. Answers →
   `cc-raw-sonnet-5/<QID>.md`. Same D2 audit rule (only Reads of the one
   prompt file + one Write, else row void).
3. **`haiku-4.5-cc-harness`** — same path, model alias `haiku`. Answers →
   `cc-raw-haiku-4.5/<QID>.md`.
4. **`codex-5.5-xhigh-fast-codex-cli`** — via `codex exec` headless on
   Sid's ChatGPT-subscription OAuth (`~/.codex/auth.json` present;
   config.toml already = gpt-5.5 / xhigh / service_tier fast — the exact
   arm named). **ccr blocker (exact):** ccr 1.0.73 has no
   ChatGPT-subscription/OAuth provider type — its providers take
   `api_base_url` + `api_key`; reaching gpt-5.5-codex through it requires
   an OpenAI API key = API dollars, forbidden by the budget ruling. No
   Sid-side action can fix ccr itself short of paying; codex CLI is
   already authed, so the arm runs there instead (deviation D8: different
   agentic harness — codex-cli scaffolding, not Claude Code; rows are
   cross-harness comparable only with that caveat, same as D2 flavored
   opus rows). Contamination control: each exec runs in an isolated
   scratch dir containing ONLY that QID's prompt file copy, sandboxed;
   post-hoc audit of the codex session log for any file access beyond the
   prompt file voids the row. Answers → `codex-raw/<QID>.md`.

### Model-id provenance caveat (D7)
`sonnet` / `haiku` are harness aliases (Agent tool); branch prompt names
them sonnet-5 / haiku-4.5. Exact model ids are not independently
verifiable from inside the run; recorded as alias+presumption.

### Not run (unchanged)
A3 ≡ A0 at this sha (D1 stands) — the LM-1 interaction term stays
UNMEASURED until a post-R-2 snapshot with relations/ exists.
