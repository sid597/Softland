# Next session: implement relation-kernel-module

Written 2026-07-03 (Fable session). This is the active handoff. The implementer
may be any model (Opus 4.8 / Codex / other) in a fresh session.

## Context in 6 lines

- The decision log `docs/current-mental-model/decisions.md` is **binding**
  (D-001..D-006, all CLOSED and countersigned by Sid). Read it first. Do not
  reopen closed decisions; reopening requires evidence of a used form breaking.
- First form being built: the **trail view** (D-002). Its prerequisite noun is
  the typed RelationEdge (D-004).
- The full design is already decided:
  `docs/current-mental-model/build/relation-kernel/CONTRACT.md`. It is the
  Phase-0 input. Implement it; do not redesign it.
- Sequencing rule (D-005): view-first; this kernel is the current work package.

## The task

Implement `relation-kernel-module` exactly per CONTRACT.md:

1. **Invoke the `/rama` skill FIRST** and follow its full phased process
   (Phase 0–4, one phase per session, phase artifacts required). The contract
   is the Phase-0/spec input.
2. New files only: `src/app/server/rama/relation_kernel.clj` + a test
   namespace. Do NOT edit existing kernels — the only permitted dependency is
   requiring plain helper fns from `app.server.rama.object-container`
   (`extract-object-key`, `fixed-width-order-key`, `actor-row`).
3. Scope guard: CONTRACT.md §9 lists explicit refusals (no FK validation, no
   confidence scores, no relation-as-container, no deletion). Do not add them.

## Phase-1 verification duties (flagged by contract author)

Two contract claims were written from model memory, not verified against the
Rama references — verify BOTH against `.claude/skills/rama/references/` (and
linked official docs) before writing code, and note findings in the phase
artifact:

1. §5's fine print on microbatch semantics: mid-batch crash → batch replay;
   PState writes transactional per batch across tasks.
2. §7's query-topology idiom: per-key `|hash` fan-out, aggregate at `|origin`.

## Definition of done

- All 10 acceptance gates in CONTRACT.md §11 pass as IPC tests.
- Style gates (§11 tail): typed defrecords, imported partition helpers,
  `{:allow-yield? true}` on unbounded range reads, consumers only via the two
  query topologies.
- Commit only code files (`.clj`); never commit `.md` files or .gitignore
  changes (hard rule). Never read `src/app/server/env.clj`.

## If the contract breaks

If implementation surfaces a contract-breaking problem (something in
CONTRACT.md that cannot be built as specified or is wrong under Rama
semantics): **STOP that thread. Do not improvise around it.** Record the
problem precisely under "Open questions" in
`docs/current-mental-model/decisions.md` and in the phase artifact; it goes
back to Sid + a Fable session for amendment. Per D-006, a contract-breaking
flaw is also an evaluation result and must be recorded there.

## After green gates (next work package, do NOT start without Sid)

Transcript→commit/doc join extractor: walk already-ingested transcript
material (tool calls carry file paths + git activity) and emit `:produced` /
`:based-on` relation assertions with evidence anchors, plus the small
git-commit-metadata adapter (D-003 Regime-1 spine). This feeds the 27-04 trail
view. Design notes live in D-003 and CONTRACT.md §12.
