# No active work package

The relation-kernel work package (opened 2026-07-03) CLOSED 2026-07-03:
gate review PASS, code committed (`2796044`: `src/app/server/rama/
relation_kernel.clj` + test ns), docs trail committed, retro run. Its full
record: `docs/current-mental-model/build/relation-kernel/` (CONTRACT →
IMPLICIT_SPEC → PLAN → PLAN_VALIDATION → IMPLEMENTATION_VALIDATION →
TEST_VALIDATION → GATE_REVIEW → RETRO).

## Queue (Sid sequences; per decisions.md Fable-window queue)

1. **Work-package succession skill** (`.claude/skills/work-package/`) — write
   in a FRESH session FROM `build/relation-kernel/RETRO.md` (the pre-registered
   input; spec in decisions.md Open Questions). Do not re-read the raw package
   trail; the retro is the distillate.
2. **Trail-view data contract** (next Fable-window item) — the contract +
   acceptance gates for D-002's first form, consuming the relation kernel via
   its two query topologies.
3. **D-003 Regime-1 spine** (needs Sid's go): transcript→commit/doc join
   extractor + git-commit-metadata adapter emitting `:produced`/`:based-on`
   assertions with evidence anchors, feeding the 27-04 trail view.
4. Slot-anywhere, cheap-model: D-006 criterion-2 counterfactual probe (fresh
   Opus, input manifest = CONTRACT §13); `:workers 2` smoke test of the
   relation kernel; envelope/payload-binding server-side recheck before any
   agent-authored writers.

Docs tracking RESOLVED (Sid, 2026-07-03): the full docs tree is tracked on
this branch (`84b3d82`), including `decisions.md`; `docs/` removed from
.gitignore here (`9e2e3af`). Convention (also in memory Hard Rules): docs
commits ONLY on this local branch, never pushed, never merged into main;
code and docs always in separate commits.

Also queued: adversarial recheck of `build/relation-kernel/RETRO.md` — run it
as the FIRST phase of the succession-skill session at xhigh/max effort (verify
every scorecard claim against the phase artifacts; hunt for missing lessons)
before writing the skill from it.

Session hygiene notes for whoever writes code next: see
`memory/implementation-quirks.md` → "NUL bytes in source" (git-binary trap +
the tool-JSON `\u0000` trap) and "Microbatch test barrier" (the cumulative
processed-count harness to reuse for every kernel test suite).
