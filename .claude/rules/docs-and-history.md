---
paths:
  - "docs/**"
  - "history/**"
  - "reference/**"
---

# Reading docs, reference and history

`docs/README.md` distinguishes the vision, intended decisions, current build
guides, and the account of how we got here. Current code explanations belong
with their source. `reference/README.md` indexes dependency documentation and
source snapshots; check their provenance and version before applying them.
`history/` holds earlier plans,
implementation logs, reviews, experiments, and research; consult it when a
question needs their rationale or evidence, rather than loading it by default.
Labels such as "current", "binding", or "next" inside historical records
describe their original context. They do not establish present requirements
or authorize unfinished work. Check implementation claims against current
source and surface relevant discrepancies using the vision guidance in `CLAUDE.md` under "Where the vision is".
The former `meta/` workflow is archived as an experiment, not a standing
procedure or a required read before making a prompt. Reuse observations when
they help the question without making the entire method compulsory.

`BETS.md` is retired; mentions in historical records, quoted prompts, and
frozen benchmark material refer to that earlier document, not current guidance.
