---
name: reference-recall-prompt-memory
description: Where Sid's prompt-recall tool (hybrid search over every prompt he ever typed, Claude Code + Codex) and his evidence-built persona live, and how a session should use them
metadata:
  type: reference
---

`/mnt/data/projects/recall` (git repo) — `recall search "…" [-p project] [--since]` · `recall show <uuid> [--session]`
· web http://127.0.0.1:7337 · MCP `recall mcp` (search_prompts · get_prompt · session_thread · recent_prompts · prompt_occurrences · list_projects).
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

For present decisions, establish the live conversation and current project guidance first. `docs/carry-on.md`
is the vision entrypoint and a reference synthesis; `vision/LOG.md` is the primary source. Recall can recover
the reasoning, corrections and unsettled questions behind them. Historical instructions, including BETS or
archived workflow rules, do not become current requirements because search returned them. Read the original
and its surrounding turns to establish status; a decision may be settled in a conversation too. Surface a
relevant contradiction with its sources instead of silently choosing the newest or most similar result.

Choose what to recall and along which dimensions for the actual question. `search_prompts` accepts up to
three queries with one shared result/character budget and identifies which query found each occurrence.
An empty query result is visible. Inspect matching excerpts and candidate/coverage limits; semantic search
covers indexed prompt chunks only, while keyword search also covers stored replies. Long replies may already have
their middle elided by ingestion: `reply_storage` makes that visible, and paging cannot recover it. Use transcript
file/line provenance when that missing passage matters. Use `get_prompt` and paged
`session_thread` around a selected UUID to read qualifications, short approvals, corrections and replies.
Identical prompts share presentation, with each occurrence retaining its own session/date/reply; expand
with `prompt_occurrences`. Truncated text has offsets for further reading. Use `recent_prompts` on demand
when resuming activity or when chronology matters without known keywords. Dates locate evidence; they do
not assign authority or expire older context. No automatic recall hook, decay rule or parallel truth store.

Evaluation uses a frozen corpus and checked source UUIDs. `RECALL_TRACE_FILE` optionally records a run's
queries, fetched UUIDs, response size and timing, with no transcript bodies; it is off otherwise. Keep traces
with that evaluation rather than starting a permanent collection or a manual classification backlog.
