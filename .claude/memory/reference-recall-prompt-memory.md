---
name: reference-recall-prompt-memory
description: Where Sid's prompt-recall tool (hybrid search over every prompt he ever typed, Claude Code + Codex) and his evidence-built persona live, and how a session should use them
metadata:
  type: reference
---

`/mnt/data/projects/recall` (git repo) — `recall search "…" [-p project] [--since]` · `recall show <uuid> [--session]`
· web http://127.0.0.1:7337 · MCP `recall mcp` (search_prompts · get_prompt · session_thread · list_projects).
A systemd user unit (`recall.service`) re-indexes new transcript bytes every 30 s and embeds with bge-m3 on the
local GPUs via Ollama, so it grows by itself. DB: `~/.local/share/recall/recall.db`.
Persona: `persona/SID.md` — every quote carries a uuid that `recall show` opens; reader digests in
`persona/digests/`; held-out test receipt `persona/holdout-results.md`; re-derive per `persona/REGENERATE.md`.
Skill: `/ask-as-sid` (user-level) — Sid-voiced pushbacks/questions on an artifact, with confidence tags.

**Why:** Sid asked (2026-08-23) for a persona he can use to "question the system in my language" at ~30–40 %
fidelity, and a local semantic search over all his prompts; both are built from the same corpus.
**How to apply:** before asserting "Sid said X" or writing a starter in his register, search his real words;
before handing him anything big, run /ask-as-sid; when the persona and his live words disagree, his live words
win — note the delta. Related: [[feedback-preserve-sids-vocabulary]], [[feedback-corpus-terms-never-back-at-sid]].
