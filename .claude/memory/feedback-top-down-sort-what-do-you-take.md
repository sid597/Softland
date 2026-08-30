---
name: feedback-top-down-sort-what-do-you-take
description: "For any 'what is this code / what goes' walk: the five-question reading card (what do you take · give · who calls you · what do you hold · who makes the thing you take), the role × kind grid read by column, two diagrams (what's there / struck-through ✗ ~ ? ◐); verdicts are Sid's."
metadata:
  type: feedback
---

When Sid walks a codebase top-down ("let's go layer by layer", "what does this whole client take?"), the instrument that worked (2026-08-28, the client sort: 32 files → the render engine folded by kind, 25 files) is:

1. Ask every namespace five questions, in order: **what do you take** (arg vectors of the 1–3 fns called from outside) · **what do you give** · **who calls you** (only the harness ⇒ a library waiting for a host) · **what do you hold** (defonce/atoms) · **who makes the thing you take, and what does a person DO to make one** — this maps code to feature; "nobody in src/" = a feature with no author.
2. Sort into a grid: rows = role (what it is · preparer · painter · conductor · driver), columns = kind (what it's about). A file lives in one cell; a column is one feature end to end. Read by column, never by row; folder names lie.
3. Deliver two diagrams at one grain: everything that's there, then the same struck through — ✗ ruled · ~ position (his word) · ? open fork (his) · ◐ part goes. Verdicts wait; positions are marked.
4. The waist test differs per area: engine = "does this need the GPU?"; server = "is this the land's truth, or old-page behavior that should be data made in-land?"

**Why:** Sid, 2026-08-28: "this is my whole goal for this phase … what does this whole client take? and then, like, going down different files." Jumping from the five boxes straight into one file lost him ("now I am disconnected as to what these path materials refer to") — the grid was the missing step. Namespace docstrings written in contract names tell him nothing ("it tells nothing"); the card replaces them until headers are plain.

**How to apply:** Opus hunters gather per disjoint slice with the five questions as the brief ([[feedback-waist-sort-hunters-code-only]]); the session sorts; show the grid before any single file; one grain per map ([[feedback-code-maps-one-notch-in-out-why]]); plain words ([[feedback-corpus-terms-never-back-at-sid]]).
