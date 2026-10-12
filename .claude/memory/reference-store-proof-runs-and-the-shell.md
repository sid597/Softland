---
name: reference-store-proof-runs-and-the-shell
description: "Running the store's proofs from the Bash tool — the shell is zsh (an unquoted variable is not split into words), and a sequence of proofs loads source as each JVM starts, so no source edit while a sequence still has a proof to start"
metadata:
  node_type: memory
  type: reference
  originSessionId: c87e94e1-c1fe-4bc8-8491-5f2d66e3bfd6
  modified: 2026-10-12T12:07:43.703Z
---

Two things about running the store's proofs (`clojure -M:store ...`, `-M:store-module ...`) from this
environment. Both were learned the hard way on 12 October 2026, in the session that finished the read
build ([[feedback-parallel-sessions-share-the-working-tree]] is the neighbouring rule for commits).

- The Bash tool's shell is **zsh**. An unquoted `$var` is not split into words, so
  `for p in "a b" "c d"; do set -- $p; ...` runs with `$1` holding the whole string and `$2` empty.
  Six proofs "ran" with an empty namespace that way and each failed in a second. Write such a loop as
  `bash -c '...'`, or as explicit calls of a small function.
- A proof's JVM loads every namespace when it starts. In one command that runs several proofs one
  after the other, a later proof loads whatever the source is at that later moment. So an edit of
  `src-store` or `test-store` made while such a sequence is still running can land in its later
  proofs half-made. Either wait for the sequence, or take the baseline from a detached worktree of
  the head (`git worktree add --detach <scratch>/baseline HEAD`) so that the working tree is free.

Smaller: a foreground `sleep` is blocked here; a long run goes in the background and the harness
says when it ends. Never open a file under a build's `runs/` whose name begins `raw-`: one of them
in the read build is 1.0 GB.
