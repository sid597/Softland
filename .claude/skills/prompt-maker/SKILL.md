---
name: prompt-maker
description: Turn a chat's raw thinking into a prompt for another session — the raw prompt in Sid's first person plus appendable flavorings per model family; Sid decides what to append and to how many sessions. Use when Sid asks for a prompt for a new session, or /prompt-maker.
---

Codex does what the words say. Claude becomes what is in the room, and the
biggest thing in the room wins. The room supplies the candidate ideas, the
question decides which count, and the idea a session finds first organizes
everything after it. For Claude, handed material becomes the world and
fetched material stays evidence, and Sid's "load this" is the act that hands
it. For Claude, the level of the question and the size of the room must
match, or the room pulls the answer down to its own level; for Codex only the
question sets the level.

Output the raw prompt and the flavorings. Sid decides what to append and to
how many sessions.

The raw prompt is the essence of what Sid is asking, in his first person,
sliced and spliced from the chat. How to present it best is the writer's
judgment.

The flavorings, each one line to append. Output all of them, the lines
together in one code block so a copy takes nothing else. Above the block,
one sentence per flavoring tied to this chat: what that room might find
that the chat is missing. Those sentences never go inside the prompt.

  claude, widest:
  read nothing on disk, no code, never src/app/server/env.clj.
  gives the frame, in his register, hands a question back; cannot check
  anything, so what it says exists is what the raw said exists.

  claude, one mechanism deep:
  read nothing on disk; code only if a claim needs a receipt, scoped to
  one file, say why you went; never src/app/server/env.clj.
  gives one mechanism worked through, one side of the wire; its first
  answer follows the shape of the question, its second turn is where it
  folds.

  claude, whole system:
  read nothing on disk; code only if a claim needs a receipt; never
  src/app/server/env.clj.
  gives both sides with receipts and the whole picture; comes back as a
  report and may pose a choice that is not one.

  codex:
  attached: <facts file>. i am asking ___ and what i want from it is ___.
  gives a position that says what the direction would let him do, which
  distinctions matter, and whether it serves the need; the purpose rides
  in the question, at whatever altitude the question sits, not in a level
  label.

For a Codex session: carry Sid's full question and the situation that gives
it meaning. Make the intended level and intellectual purpose explicit in the
actual question. A local request such as "agree or disagree" should serve
that larger purpose, rather than turn the conversation into isolated claim
checking. Attach the factual material needed for that question, preserving
what it establishes and what remains uncertain. Keep adopted positions
distinguishable from implemented behavior and useful experience. When
reading is prohibited, the supplied material is the grounding; otherwise,
any collection must stay within Sid's permitted scope.

Nothing about how to think, in the raw or in a flavoring.
