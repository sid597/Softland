# Cross-Harness & Machine Dispatch Notes (`2026-11-22`)

Kept here because Antigravity and Codex do not auto-load `.claude/memory/`, and multi-session prompts regularly cross harnesses. Re-verify when tool or stack versions change.

## Repo, Locks & Classpaths
- **Shared working tree:** Run `git log -3` and `git status -sb` before staging; stage **only** owned paths by name (never `git add -A`). Retry after ~2s if `.git/index.lock` is held.
- **Same-inode `flock`:** Always pass the full absolute lock path (`/mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock`) — relative paths across git worktrees lock distinct inodes.
- **Frozen classpath override:** Top-level `-Sdeps '{:paths [...]}'` does **not** override `:replace-paths` in `deps.edn`; use `-Sdeps '{:aliases {:<alias> {:replace-paths [...]}}}'` against a clean `git archive` snapshot outside the repo.
- **Scratch paths:** Put temporary worktrees/snapshots under `/mnt/data/projects/research/softland/` (never `/tmp`, which wipes on reboot) and `git worktree remove` when done.

## Brief Mechanics by Harness
- **Claude Code (`Fable` / `Opus`):**
  - *Unattended builds:* `.claude/output-styles/build.md` pauses for Sid's go unless the prompt states *"This is a build (not exploration); this message is my go — do not wait for confirmation"*, runs under `/output-style Build`, and bypasses permission prompts.
  - *Safeguard stops (`reasoning_extraction`):* Never ask for first-person "thinking", "reasoning", "working", or a "design difficulty log"; for long plans, create the target file first and write section-by-section.
  - *Subagents & shell:* `CLAUDE_CODE_SUBAGENT_MODEL` defaults subagents to `opus` (set `model:` explicitly when needed); shell is `zsh` (wrap multi-word loops in `bash -c`).
- **Antigravity (`Neon` / `Carbon`):**
  - *Autonomous runs:* Prefix unattended builds with `/goal`. Prefer commit-and-stop over waiting for other windows; if a timer is required, use `schedule` (never shell `sleep`).
  - *Receipts:* Antigravity chat is not auto-indexed in `softland-memory`, so every receipt that must survive the session belongs in a committed file or git commit.
