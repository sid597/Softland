---
name: Fix small cleanups immediately
description: When identifying small cleanup candidates during active work, fix them rather than deferring
type: feedback
---

When I identify a cleanup candidate that's small and I'm already in the relevant files, just fix it. Don't label things "not a blocker" and leave them for someone else.

**Why:** "Not a blocker" became an excuse to leave dead code in place. The legacy `update-shadows` call in electric_flow.cljc was a 1-line fix, identified during Phase 6C shadow pool work — but Claude deferred it and Codex had to clean it up. Small cleanups compound; leaving them signals sloppiness.

**How to apply:** If the fix is under ~5 lines and you're already working in the relevant subsystem, do it. Only defer if the fix genuinely touches a different subsystem or has risk you can't assess. "Not a blocker" is a reason to deprioritize, not to skip entirely.
