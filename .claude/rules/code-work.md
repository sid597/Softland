---
paths:
  - "src/**"
  - "src-store/**"
  - "src-inland/**"
  - "src-build/**"
  - "src-dev/**"
  - "src-prod/**"
  - "test/**"
  - "test-store/**"
  - "test-inland/**"
---

# Working in code

Before gathering, reviewing, explaining, or changing client or server code,
read that source root's guidance and map: `src/app/client/CLAUDE.md` and
`src/app/client/README.md`, or `src/app/server/CLAUDE.md` and
`src/app/server/README.md`, respectively. Each source root's `AGENTS.md`
points to its `CLAUDE.md`, so both agents use the same upkeep instructions.
Follow the relevant
folder maps, namespace docstrings, and function docstrings into scoped source
reads. Keep affected explanations and their immediate parent maps aligned
with code changes, as those instructions describe.

## Code Review Protocol — Falsification Pass

After the architectural pass, do a second pass whose job is to **break** the change. Review for falsification, not coherence.

**Golden rule:** Never approve an optimistic-state change without tracing the full write → render → truth reconciliation → clear lifecycle.
Use the Staff-level product architect perspective for the falsification pass: read `.claude/skills/staff-product-architect.md` (a guide to read; it is not a registered skill).
