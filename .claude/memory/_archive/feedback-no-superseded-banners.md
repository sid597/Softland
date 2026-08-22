---
name: feedback-no-superseded-banners
description: "Working docs get direct content replacement — no SUPERSEDED banners, strikethroughs, or was-clauses; git carries the history"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: be3a2bc9-c95c-4a38-a664-5c5dcccf3875
---

2026-07-09, spec room. Sid, on seeing a "⚠ SUPERSEDED IN PART" banner: "since we have git commiting docs now why do we need this just replace the content directly why are you doing the bookkeeping it will make you and other agents trip on it and more tokens to consume to think through."

**Why:** Docs are committed per-session on the docs branch — git IS the history. In-file bookkeeping (banners, strikethroughs, "AMENDED (was X)" clauses) makes every future reader — human or agent — parse dead state to find live state, and burns tokens on archaeology.

**How to apply:** When a working doc's content changes (contracts, specs, architecture docs, opening prompts): rewrite the section to say the current truth, commit with a message that names what changed. EXCEPTIONS — docs whose GENRE is a dated log keep appending entries: GROUNDS ratification ledgers, baton NOW entries, decisions.md (amend-in-place with dated note is D-010's own rule), vision/LOG.md. The test: is the date part of the content (a ledger) or bookkeeping about the edit (a banner)? Banners never; ledgers always. Related: [[feedback-approve-by-default-no-ceremony]].
