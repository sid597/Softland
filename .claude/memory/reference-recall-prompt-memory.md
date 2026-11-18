---
name: reference-recall-prompt-memory
description: Recall capabilities and evidence boundaries; where the retired portrait of Sid is kept
metadata:
  type: reference
---

Recall makes indexed Claude Code and Codex conversations available as evidence. It can recover why a choice
was made, how a question developed, what later qualified an earlier position, or what was happening near a
particular time. A decision may be settled in a conversation even when it never reached a decisions file.
Whether recall is useful, which dimensions to explore, how far to follow them, and when there is enough
evidence are the session's judgment.

- `search_prompts`: scoped keyword, semantic or hybrid retrieval; up to three query dimensions within a shared
  budget. Each occurrence identifies the queries and passages that found it, including matches in replies.
- `get_prompt` and `session_thread`: expandable stored passages, replies and surrounding turns with provenance.
  `recent_prompts` offers activity without requiring search keywords.
- `prompt_occurrences`: every exact-text occurrence is accessible with its own date, session and reply.
  Shared prompt text does not make the conversations interchangeable.

Results expose candidate limits, text clipping and embedding coverage. Semantic search covers stored prompt
chunks; keyword search also covers stored replies. Long replies may already have their middle elided during
indexing. Offsets recover display-clipped text; transcript file/line provenance provides the route to material
absent from the index. These boundaries describe what was searched and shown, not how much relevant history
exists beyond it.

The live conversation and current project guidance establish present constraints. `vision/LOG.md` is the
primary source for the vision. Historical passages can explain,
qualify or conflict with that understanding. Relevance, recency and confident wording do not establish a
passage's authority. A consequential contradiction belongs in the conversation with its sources; an old
instruction's appearance in search does not reinstate it.

Repo: `/mnt/data/projects/recall`. MCP: `recall mcp`. CLI: `recall search "…"`, `recall show <uuid> [--session]`.
Web: http://127.0.0.1:7337. The daemon continuously indexes new transcript material. Operational details and
evaluation receipts live in Recall's README.

A portrait of how Sid responded, built on 23 August 2026 from his prompts, is at `persona/SID.md` in the
Recall repo, with its supporting material and regeneration instructions. Its quoted UUIDs open through Recall.
The `ask-as-sid` skill that ran it was retired on 11 November 2026: Sid no longer felt the need for it, and the
portrait had drifted from him. The skill file is kept beside the portrait as `persona/ask-as-sid-skill.md`.
Sid's live words take precedence over the portrait.
Related: [[feedback-preserve-sids-vocabulary]], [[feedback-corpus-terms-never-back-at-sid]].
