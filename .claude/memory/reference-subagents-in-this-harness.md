---
name: reference-subagents-in-this-harness
description: "Before you orchestrate background subagents from one session: what was seen of them on 28 November 2026 (stopping one, where its report lives, briefs as files, what each notification costs, worktrees)"
metadata:
  node_type: memory
  type: reference
  originSessionId: 26482499-eeca-42a6-8599-d2fddfc4d456
  modified: 2026-11-28T08:15:06.001Z
---

Seen on 28 November 2026, in a Claude Code session that ran seven background subagents through the
Agent tool. Facts of that day's harness; check one before you lean on it.

- **A stopped subagent gives no report.** `TaskStop` kills it mid-step: its last sentence comes
  back and nothing else, and its uncommitted edits stay in its worktree. To end one and keep its
  work, send it a message first (finish the part in hand, commit what is whole, commit the rest as
  "WIP:", report) and wait for the report. A message reaches it only at its next tool round.
- **A subagent's report is its last message, and that can be the only copy.** One reviewer was
  refused when it tried to write its findings to a file in the scratch folder. Save a report that
  matters yourself, as soon as it arrives.
- **A brief can be a file.** "Your whole brief is this file: read it from top to bottom" worked,
  and the orchestrator does not write the brief out a second time. The session's scratch folder is
  under `/tmp` and is lost at a restart, so a brief that a later session needs is copied into
  `src/proposal/<work>/` before the handoff.
- **Every notification wakes the orchestrator over its whole context.** A subagent that waits on
  its own background run notifies each time it stops to wait: a runner of eight proof runs woke
  the orchestrator seven times before it reported. With a long context that is the larger cost of
  a cheap runner. Fewer, longer subagents, and an orchestrator that keeps its own context short,
  cost less.
- **Model and effort are set on the Agent call** (`model`, `effort`). A subagent loads the root
  `CLAUDE.md` and no memory, so a brief carries the rules it needs: the cluster lock, zsh, no
  attribution line, no push, never `env.clj`.
- **Each builder gets a git worktree outside the shared checkout**
  (`git worktree add -b <branch> /mnt/data/projects/Softland-<name> main`), since other sessions
  work in `/mnt/data/projects/Softland`. Cluster proofs from every worktree go behind the one lock,
  `src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock`, with `flock`;
  `read-path-proof` took about 63 seconds once it held the lock.
- **Another session can write to this one** (a cross-session message arrived from a session named
  softland-99, asking where this one ran). It is a teammate's word, not Sid's.

Related: [[feedback-fable-thinks-opus-builds]], [[feedback-parallel-sessions-share-the-working-tree]],
[[reference-store-proof-runs-and-the-shell]].
