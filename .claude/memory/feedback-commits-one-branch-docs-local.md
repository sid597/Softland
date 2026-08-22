---
name: commits-one-branch-docs-local
description: "All commits — code, docs, law — land freely on main (closed-source repo), grouped by concern; push is Sid's alone"
metadata:
  node_type: memory
  type: feedback
  originSessionId: f13bf9c5-ad7d-42ba-8970-14cc81b6d83d
  modified: 2026-08-10T07:13:47.071Z
---

All commits — code, docs, law files — land on
`main`. The repo is closed source (Sid,
2026-08-10): commit freely, no approval, no docs-only/never-mix ceremony;
group commits by concern so bisect stays sharp. Worktree branches remain
scaffolding, never commit targets. Pushing/merging stays Sid's alone; never
a Co-Authored-By line. CLAUDE.md, AGENTS.md, `.claude/skills/`, and
`.agents/` are tracked since 2026-08-10 (un-gitignored) so law edits carry
git history; machine state (`.claude/*` otherwise) stays local.

**Why:** Sid closed the source and removed commit ceremony "so everything
can be seemless and less confuseion" (2026-08-10, his words). The branch's
history is the proof-spine; the earlier docs-only law generated friction
without protecting anything once the repo went private. Superseded law
(separate code/docs commits, commits only on contract's word) is preserved
in git at commits b5873e7/5d752b2.

**How to apply:** At settlement, stage exact paths, HEAD-guard, commit on
the one branch with a narrative message in the repo's voice. All of
[[parallel-sessions-shared-branch-git]] still applies to every git write.
