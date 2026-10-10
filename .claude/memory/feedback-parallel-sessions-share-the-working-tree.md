---
name: parallel-sessions-share-the-working-tree
description: Other Claude sessions commit in the same Softland working tree while a build runs; stage only your own paths by name and read git log before each commit
metadata:
  node_type: memory
  type: feedback
  originSessionId: c7d59789-b4bd-4c5d-b7ee-7d84727f9704
  modified: 2026-10-09T20:43:44.776Z
---

Several sessions of Sid's work in `/mnt/data/projects/Softland` at once, on `main`, in one working tree. A commit of one session can take another session's uncommitted edits when both touched the same file: staging is by file, not by author.

Seen on 10 October 2026, during the door's build: a session that was restoring Sid's lines to the decisions committed the whole of `src-store/README.md` (`67ad8d25`) while the door build's three sentences in that file were uncommitted, so those sentences went into that commit and not the door's. Nothing was lost; the door's commit message says so.

**Why:** a commit that silently holds another session's work reads wrongly in the log, and a `git add` of a folder could take another session's half-written change into mine.

**How to apply:** (my reading, not a line of Sid's) before each commit run `git log --oneline -3` and `git status --short`; stage named paths only, never a folder that another session may be writing in; tell a subagent the same, and to leave files outside its own paths alone. If a file of mine went into another session's commit, say so in my commit message and on the build's page rather than rewriting history.
