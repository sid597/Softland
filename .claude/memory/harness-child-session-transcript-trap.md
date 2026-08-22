---
name: harness-child-session-transcript-trap
description: "The 'Transcript saving is off — inherited CLAUDE_CODE_CHILD_SESSION marker' boot banner means the session jsonl will NEVER be written — act at boot, not at session end"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 1ccc1f2e-93ae-4ca1-a38c-2e93418ac72f
  modified: 2026-08-05T20:32:05.298Z
---

If the boot banner says "Transcript saving is off — inherited
CLAUDE_CODE_CHILD_SESSION marker", the harness will never write this
session's transcript jsonl (fired 2026-07-25, the P6 gate session — only
`tool-results/` existed on disk; the wire transcript was unrecoverable).

**Why:** the marker is inherited when `claude` is launched from a terminal
another Claude session spawned; the harness reads it once at boot and it
cannot be flipped mid-session.

**How to apply:** flag it to Sid in the FIRST reply, not when he notices.
If the session matters (gates, direction sittings), keep the durable
artifacts complete as you go (they should carry every number anyway), and
before session end write a reconstruction from live context into the
docs tree near the board (docs/ was reorganized 2026-08-06; precedent:
`OLD DOCS/sessions/transcripts/2026-07-25-p6-gate-session.md`,
commit b152161). Fix for the next launch: `unset
CLAUDE_CODE_CHILD_SESSION` in the launching shell / use a fresh terminal.
