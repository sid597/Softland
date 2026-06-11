# Next Session: Code Ingestor — Ratify Contract, Run Codex Gate

Status: active handoff, 2026-06-10.

## What Happened In The Prior Session(s)

The object-container adapter split (the previous handoff task) was completed
and committed:

```text
119f3f8 rama: split object-container source adapters
```

The architecture is now real: markdown + transcript ingestors both emit the
common `:object-container/import-material` contract into the
Object-Container Kernel.

The session then started the CODE INGESTOR product discussion. Sid asked for
a fresh, independent cut ("do it separately and not spoil your context") and
then put the discussion in autopilot ("do all i don't have inputs"). Result:

```text
docs/current-mental-model/build/code-ingestor/PRODUCT.md
```

A later session synthesized the contract into a single argued write-up:
`docs/current-mental-model/build/code-ingestor/index.html` — open this first
(it ends in the ratification checklist). The 8-run process record (rankings,
all eight answers verbatim) is at `build/code-ingestor/comparison.html`.
A1/A2 factual corrections are already applied to PRODUCT.md; the two open
amendments (slice-2 anchor unit; degradation-ladder naming) await
ratification on the write-up's checklist.

Six clusters decided (ALL PROPOSED, NOT RATIFIED):

```text
1. first consumer  = trail-to-code resolution; corpus = Softland repo itself
2. identity        = stable over time (transcript precedent, not markdown);
                     repo declared, file = (repo-id, relpath), renames parked
3. time/versioning = git stays authority; commit-boundary revisions on one
                     branch; first-parent; exactness flag on trail resolution
                     (tool calls touch uncommitted state — approximation must
                     be visible)
4. acquisition     = harvest + watch (commit-triggered); fail-closed scope:
                     git-tracked AND extension allow-list (.clj .cljc .cljs
                     .json; .md OUT, .edn off-by-default) AND deny-list
                     naming env.clj
5. source/anchors  = SourceArtifact per (path, content-version); source-hash
                     = kernel sha-256 of raw bytes (NOT git object id);
                     imp:code: idempotency family
6. slice 1         = repo baseline + commit watch, file-level; 12 acceptance
                     criteria; explicit parking lot
```

## Important Process Context

- A PRIOR draft contract exists at
  `docs/current-mental-model/architecture/code-ingestor-contract.md`. This
  session accidentally read it before Sid stopped that; Sid wants independent
  cuts. The new PRODUCT.md was derived only from the ingester-contract gates,
  the spec, the chat-ingester template, and adapter/kernel source, with a
  provenance disclosure at its top. DO NOT read the old draft in future
  sessions unless Sid explicitly routes there (e.g., for a clean diff by a
  separate agent).
- Memory saved: `feedback-fresh-cut-no-prior-drafts.md` — when Sid starts a
  discussion on X, do not read prior-session draft artifacts about X.

## Next Task

1. Sid ratifies / amends the six cluster decisions in
   `build/code-ingestor/PRODUCT.md`.
2. Run the Codex falsification gate on the ratified doc (a paste-ready
   prompt was prepared via $ask-codex-for-feedback at the end of the prior
   session — see that session's final message; regenerate with the skill if
   lost).
3. Only after both gates: Rama Phase 0 (IMPLICIT_SPEC.md) for the code
   adapter under $rama / $think-in-rama / $rama-pitfalls / $rama-retro-lens,
   following the markdown/transcript adapter conventions in
   `src/app/server/rama/object_container/`.

## Rules

Never read `src/app/server/env.clj`. Docs are never committed. Only commit
code files.

---

# Parallel Thread: Rama Retro — COMPLETE (fix sessions are next)

Status: ALL FIVE TRACKS COMPLETE, 2026-06-11. Every pre-skill module
validated; every R4 verdict = major-fail. Cross-retro comparison done.

**Read `docs/retros/rama/UNIFIED-RETRO.md` first** — both retros merged: one
verdict, seven weakness groups, master fix queue (Batch 0 cross-cutting →
compute → llm → space → kernel-contract → transcript), fix-session protocol,
and a paste-ready handoff prompt. Method codified as the `rama-retro` skill
(`.claude/skills/rama-retro/SKILL.md`) for all future retros. Supporting
detail: `README.md` (how this retro ran), per-track `0N-*/FINDINGS.md`,
`COMPARISON-prior-retro.md` (what each method caught/missed).

Next steps (in order of value):
1. Fix sessions per track — resume the standard /rama skill process at
   Phase 3 with the track folder as impl-root (spec + plan + findings in
   place). Start with compute Batch 1 (microbatch conversion + submit dedup
   guard) — it is the pattern-setter the other kernels copied.
2. Every fix batch MUST add runtime probes (the prior retro's technique):
   convert each HIGH finding into an adversarial IPC probe; `require` every
   touched namespace as step zero. Re-run Phase 4 (+6) after changes.
3. Cross-cutting fix worth doing once, everywhere: shared idempotent-fold
   helpers (guarded writes, dedup anchors, sticky terminals) in core.clj so
   all five kernels stop re-implementing the same broken pattern.

Cost note: tracks 2–4 plans are UNVALIDATED (R2/R3/R6/R7 skipped to save
tokens after Track 1 calibrated the method). Memory:
`feedback-agent-wave-cost-visibility.md` — announce agent counts + token
estimates before waves; offer manual runbook.
