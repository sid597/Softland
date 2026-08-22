---
name: parallel-sessions-shared-branch-git
description: "Parallel Claude sessions share one working tree and branch — git discipline that prevents clobbering a sibling's work"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: ce94a3e4-9370-4ba1-9890-2527793d8501
  modified: 2026-07-21T10:31:10.294Z
---

Parallel sessions are normal in this repo (the board is built for them). They
share ONE working tree, ONE index, ONE branch — every git write races them.

**Why:** A session ran `git add docs/` and swept a sibling's half-written
phase record into its commit; then a `reset --soft HEAD~1` planned minutes
earlier landed AFTER the sibling had committed — and reset away the
sibling's commit instead of its own. Content survived only because soft
resets don't touch files and the reflog holds everything.

**How to apply:**
- Stage exact file paths only — never `git add docs/`, `-A`, or a directory.
- Before ANY git write: `git status --short` + `git log -1` — a commit or
  staged file you don't recognize means a sibling is active; coordinate,
  don't fix.
- Never reset/amend/rebase when the tip commit isn't yours.
- Guard destructive chains on the expected HEAD:
  `[ "$(git rev-parse HEAD)" = "<hash>" ] && …`.
- If a sibling's commit gets damaged: soft-reset + re-mint it with THEIR
  exact message and content (diff against the orphaned hash must be empty);
  the reflog keeps the original recoverable.
- **Exact-path staging does NOT protect against foreign content INSIDE a
  file you both touch** (fired 2026-07-21: a `git commit -- ground.cljs`
  swept a sibling's uncommitted 66-line WIP into this session's commit).
  The Edit tool's "file had been modified on disk since you last read it"
  note IS the sibling-activity alarm — STOP on it: `git diff` the file and
  recognize every hunk as yours before any commit that includes it.
  Repair that worked: save the full file aside → soft-reset (tip was mine)
  → restore HEAD's version → re-apply ONLY own edits → commit → copy the
  saved file back, leaving the sibling's hunks uncommitted as found.
