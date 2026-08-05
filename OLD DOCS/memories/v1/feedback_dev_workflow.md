---
name: Dev workflow command
description: Never run standalone shadow-cljs compile — use clj -A:dev -X dev/-main which starts shadow watch + Jetty together
type: feedback
---

Use `clj -A:dev -X dev/-main` to start the dev environment. This runs shadow-server, `shadow/watch :dev`, and the Jetty server in one process. Incremental hot-reload handles recompilation automatically.

**Why:** Standalone `npx shadow-cljs compile dev` or `clj -A:dev -M -m shadow.cljs.devtools.cli compile dev` does a full cold JVM startup + compile (10+ minutes). The dev server's watch process does the same thing incrementally in ~1 second.

**How to apply:** Never run a standalone compile for verification. If the user's dev server is running, file saves trigger hot-reload. If it's not running, don't start a cold compile — verify at the source level or suggest the user start their dev server.
