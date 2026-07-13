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

- **2026-07-12 · Fable (same session) · wave RUN + gate + close-on-Sid's-word.**
  Lanes A∥B GREEN first-run (LANE_A/B.md). INT: enum pre-landed (`f864c74`);
  boot attach REWORKED to reuse the TRAIL cluster's rk (the first wearing
  caught the cluster collision live) + `machine-cut-ctx`; constant re-homed;
  model plumbed. G13 receipt PASS (real corpus, real run, $0.37, agreement
  100% on a structurally-trivial window — RECEIPT_G13.edn). Falsification:
  A=4 HIGH CONFIRMED (journal-key/barriers/noop/replay-scope) — ALL fixed at
  gate with `gate-fix-*` regressions; B/C=0 HIGH. CONTRACT §4.3/§3/§5.5
  amended in place (dated). **OPEN: G14 wearing (driver interrupted at Sid's
  close) + quiet-box serial re-run** (post-fix run: 93t/1458a, 6 fails = llm
  timing flake under dev-server boot, green isolated 12/114). Verdict + full
  dispositions: INT.md. Sid flagged wave cost (~990k subagent tokens);
  retro rule proposed: falsification defaults to ONE finder on new machinery.
  Next: CLOSE session (suite at committed HEAD · G14 · retro; recheck = Sid's
  call on cost).

- **2026-07-12 · Fable (same session) · countersign + dispatch.**
  Sid countersigned all three items in-session ("i say do the
  countersign") — recorded in decisions.md. `:pairs-with` enum line landed
  + verified (ns loads, kind registered) — isolated code commit `f864c74`.
  Lanes A ∥ B dispatched as parallel Opus 4.8 subagents (LANES.md prompts;
  kind-indirection caveat void — enum in tree). Next: lanes land
  `LANE_A.md`/`LANE_B.md` → INT checklist (boot attach, WAL wiring, serial
  suite, G13 receipt, G14 wearing) → falsification batch → gate.

- **2026-07-13 · Fable · CLOSE session — package CLOSED.**
  G14 CLOSED: `boxes-paired-face` worn in the wild during scene-substrate
  G11 (Sid drove it 3 rounds over the annotated 64-block window); boot-log
  receipt verified in the G11 session transcript: `[FACE] machine-cut WAL
  replay: 1 lines, 34 asserted, 0 retracted, 0 failed` (evidence split +
  salted-re-annotate residue: INT.md §G14). Quiet-box serial suite at
  committed HEAD: first run 94t/1476a/1f — the one fail = framework's
  g21 read-surface scan stale vs block-write's committed `ocr/read-unit`
  (`ad19b96`; literal-vs-intent, scan updated with citation) — re-run
  **94t/1476a/0f/0e GREEN**. RETRO.md written; lessons routed (skill: ONE-
  finder falsification, transition-keyed identity, cross-package scans,
  inherited wearing; quirks: unread barriers, one-cluster-per-land).
  Adversarial recheck NOT run — Sid's call on cost. Board pruned to a
  one-line done-pointer. T11 resolved on the board (edits committed).
