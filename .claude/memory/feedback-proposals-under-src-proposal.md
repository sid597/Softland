---
name: feedback-proposals-under-src-proposal
description: New handoffs, fact bases and proposals go under src/proposal/<work>*, never under history/ (history is out of bounds for new writes)
metadata:
  type: feedback
---

Session handoffs, fact bases, starters and proposals for a piece of work go
under `src/proposal/<work>*` (for example
`src/proposal/inland-integration-2026-09-14/`). `history/` is the archive of
earlier work and is out of bounds for new writes.

**Why:** Sid, 2026-09-14, after a fact base and starter were placed under
`history/docs/`: "nope not in history ... history is out of bounds add them to
src/proposal/<work>*". The archive keeps records of what happened; a proposal
is live material for the next session.

**How to apply:** When Sid asks to write down gathered context or a next-session
prompt, create or reuse a `src/proposal/<work>...` folder. Do not add to
`history/` unless Sid names it. Related: [[project-inland-integration-2026-09-14]].
