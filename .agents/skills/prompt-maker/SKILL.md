---
name: prompt-maker
description: Turn a chat's raw exploration into a prompt for another session — the raw prompt in Sid's first person plus appendable scope flavorings; Sid decides what to append and to how many sessions. Use when Sid asks for a prompt for a new session, or /prompt-maker.
---

Output the raw prompt and the flavorings. Sid decides what to append and to
how many sessions.

The raw prompt is the essence of what Sid is asking, in his first person,
sliced and spliced from the chat. How to present it best is the writer's
judgment. Keep his meaning and his words, and fix the grammar, the pronouns
and the scope until every sentence reads one way. Cleaning adds nothing: no
shape for the answer, no question turned into an inventory.

Ask the other model for its view, its answer and the grounds: what holds,
what breaks on the case, what is still open. Never ask for its "thinking" or
"reasoning" (Sid, 30 Oct 2026), or for its "working": on Fable and Opus such
a request can be stopped by a safeguard. When Sid's own words in the chat
ask for one of these, carry that as a request for the view and its grounds.

Before writing to a session that already exists, look up what it has
already read: its texts in the record (memtool), never its transcript.

When the answer has to be independent, write two messages. The first asks
for its view on what it was handed. The second, sent only after it answers,
says what to read and compare.

The flavorings, each one line to append. Output all of them, the lines
together in one code block so a copy takes nothing else. Above the block,
one sentence per flavoring tied to this chat: what that scope might surface
that the chat is missing. Those sentences never go inside the prompt. A
flavoring that says "read nothing on disk" never goes on a message that has
to load or read files.

  widest (no disk):
  read nothing on disk, no code, never src/app/server/env.clj.
  tests the framing from the prompt alone; cannot verify disk state, so
  what it assumes exists is only what the raw prompt supplied.

  one mechanism deep:
  read nothing on disk; code only if a claim needs a receipt, scoped to
  one file, say why you went; never src/app/server/env.clj.
  traces one mechanism with at most one file checked for a concrete receipt.

  whole system (receipts only):
  read nothing on disk; code only if a claim needs a receipt; never
  src/app/server/env.clj.
  checks claims across files only as needed for receipts, without open-ended
  browsing.

  with attached facts:
  attached: <facts file>. i am asking ___ and what i want from it is ___.
  pairs an explicit facts bundle with the question and states the intended
  altitude and purpose directly in the question.

When attaching a facts bundle (or sending to a session without shared room
context): carry Sid's full question and the situation that gives it meaning.
Make the intended level and intellectual purpose explicit in the actual
question. A local request such as "agree or disagree" should serve that
larger purpose, rather than turn the conversation into isolated claim
checking. Attach the factual material needed for that question, preserving
what it establishes and what remains uncertain. Keep adopted positions
distinguishable from implemented behavior and useful experience. When
reading is prohibited, the supplied material is the grounding; otherwise,
any collection must stay within Sid's permitted scope.

Nothing about how to think, in the raw or in a flavoring.
