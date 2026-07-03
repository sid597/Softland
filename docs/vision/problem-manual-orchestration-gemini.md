# Problem: Manual Orchestration & Context Thrashing

> "I do this all manually... pull issues from Linear, create worktrees, start background terminals..."

## The Current Workflow (The Pain)
Currently, managing parallel streams of work requires the developer to act as a manual "Process Manager" for the AI:

1.  **Context Switching:** Manually switch to Linear/Jira to find tickets.
2.  **Environment Setup:** Manually type shell commands to create `git worktrees` (`git worktree add ...`).
3.  **Context Loading:** Manually copy-paste ticket descriptions into the AI's prompt.
4.  **Process Management:** Manually open new terminal tabs for builds/servers for each stream.
5.  **Review Assembly:** Manually run `git diff`, copy the output, and paste it into a PR description.

## The Cost
- **High Cognitive Load:** The developer spends energy managing *files and processes*, not solving problems.
- **Serialization:** Because setup is hard, developers tend to work serially (one task at a time) rather than letting the AI handle 3 tasks in parallel.
- **Context Loss:** "What was I waiting for on Branch B?" is forgotten while working on Branch A.

## The Gap
We have a powerful "Unit of Work" engine (the Editor + Agent).
We lack the **"Manager of Work"** — the layer that automates the setup, teardown, and context switching between these units.
