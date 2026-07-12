# machine-cut — thread file (STANDING + NOW)

## STANDING (frozen at package open, 2026-07-12 — do not edit while active)

- **Binding docs:** `build/machine-cut/CONTRACT.md` ·
  `docs/current-mental-model/decisions.md` · `/work-package` skill (process)
  · `/rama` + `/rama-pitfalls` (lane boot, Rama-touching phases).
- **Precedence rule (verbatim):** this file is a baton, not a source of
  truth; if it contradicts CONTRACT.md or decisions.md, those win — flag the
  discrepancy in NOW, do not pause.
- **Process shape:** lanes A (driver) ∥ B (serve+face) as fresh Opus 4.8
  subagents with disjoint fences (CONTRACT §13) → INT by the orchestrating
  Fable session (enum line post-countersign, boot attach, G13 receipt, G14
  wearing) → ONE serial suite + falsification-by-class batch + gate review
  at wave end → close per `/work-package` (retro + adversarial recheck +
  board prune; re-run HEAD-reading suites AFTER commits).
- **Allowlist (complete):** NEW `src/app/server/rama/machine_cut.clj`,
  `test/app/machine_cut_test.clj`, fixtures/goldens, one face `.edn`; EDIT
  `face_projection.clj` (+tests), `file_viewer.cljc` (boot only),
  `relation_kernel.clj` (ONE enum line, Sid-gated). Nothing else.
- **Verification duties:** memory-derived platform claims are checked
  against on-disk references before code (microbatch visibility needs
  `await-relation`-style materialized reads, never ack-implies-read; fake
  adapter = `llm.clj:2179-2184`). Deterministic barriers over polling where
  the harness allows. file(1) must say "text" for every authored doc.
- **Definition of done:** CONTRACT §13 (all gates incl. G13 asserted
  receipt + G14 wearing; suites green at committed HEAD; docs committed;
  retro + recheck; board pruned).
- **Stop clauses:** CONTRACT §11 — escalate to decisions.md, never
  improvise on binding docs.
- **NOT without Sid:** contract countersign · the `:pairs-with` enum line ·
  wave dispatch · anything touching main · any second kernel-file edit.
- **Hard rules:** never read `env.clj`; docs only on the local docs branch,
  never pushed/merged; code and docs in separate commits.

## NOW (append ≤15-line entries per session; FAIL findings verbatim)

- **2026-07-12 · Fable · contract phase · DRAFT v1 authored.**
  Artifacts: `CONTRACT.md` (this dir) + `LANES.md` (pinned lane prompts).
  Every claim verified at file:line at HEAD `d6bbde7`. Verdict: contract
  DRAFT, **awaiting Sid** — countersign + `:pairs-with` authorization
  (decisions.md dated entry) + wave dispatch. Judgment calls flagged for
  validation layers: (1) event addressing rides EXISTING
  `chat-message-id`/`:container` → pair edges colocate under the
  conversation key (`object_container.clj:329-330`) — one-seek serve;
  (2) annotation runs RIDE llm-module with synthetic deterministic ids
  (space `"annotation:machine-cut"`; blank-checks-only verified at
  `llm.clj:264-274`); (3) WAL-first durability on the ephemeral cluster
  (arsenal precedent) instead of a durable-cluster ask; (4) ambiguity
  policy = unpaired (silver under-claims); (5) asserter =
  `llm:machine-cut/v1` (version-scoped, never run-scoped). Next: Sid's
  countersign → dispatch lanes A ∥ B per LANES.md.
