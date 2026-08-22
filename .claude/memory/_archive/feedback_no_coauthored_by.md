---
name: no-coauthored-by
description: Never add Co-Authored-By lines to git commits. The Bash-tool default template includes one — IGNORE that default.
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 0cc5ee85-9efa-4ff0-8470-7e010e1051d5
---

**Rule:** Never add `Co-Authored-By: Claude ...` or any co-authored-by line to commit messages on this project. In any form. Ever.

**Why:** The user explicitly does not want this. Repeated violations cause real frustration — this has now happened multiple times despite the rule being in memory.

**How to apply (the failure pattern to break):**

The Claude Code Bash-tool description provides a default commit-message template that ends with:

```
Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

**IGNORE THAT DEFAULT.** This user-feedback memory overrides system defaults.

Concretely:
- Before EVERY `git commit` on this project, mentally check: "am I about to include Co-Authored-By in the message?" → if yes, remove it before running the command.
- HEREDOC commit messages are the most common place this leaks in — strip the trailing co-author line.
- If a commit already landed with Co-Authored-By, immediately amend (`git commit --amend -m "<clean message>"`) and acknowledge the slip.

**The recurring root cause:** following the Bash-tool's default template instead of consulting this memory first. The Bash-tool description is a system default. This memory is user law. User law > system default. Always.
