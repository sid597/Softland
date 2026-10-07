# Deciding: the method

How a question reaches Sid, and how an answer reaches disk. One page. The sheet
"The act — twelve questions" of 6 October 2026 is the model; Sid answered it in
one sitting, on paper.

## The form of a question

Each question has these parts, in this order, and nothing else.

1. **Words.** Only the words that are new to this question, one line each. A
   word means one thing on the whole sheet. "Key" is a fact's key; "lock" is an
   encryption key; never one word for both. A word that every question uses
   goes on the sheet's first page once.
2. **The thing.** A concrete example in front of the question, when it helps:
   an act written out, two forms side by side, a name with its slots. Marks on
   its lines (ruled · in the file · open) only where the standing of a line is
   what the question asks; otherwise none, because marks argue.
3. **The question.** One sentence.
4. **The choices.** Lettered. A choice says only its own answer. If a choice
   would settle another question, it names that question. Each choice has its
   "if": what follows, in one or two lines, the same length on each side.
5. **Today.** What the code or the map does now, in one line, as a fact. Where
   the rig did a thing because Rama forced it, say the finding (refusals are
   data; a batch is visible only when every task is done; plain milliseconds
   ran ahead under load; the log keeps every appended value). Never "the rig
   has it so" as a reason to choose, and never a choice marked as the one that
   stands if Sid says nothing.
6. **A box to tick, or a line to write on.**

Rules. No sentence argues for a side. No default comes from existing code. The
reader needs nothing from another page to answer. A question fits on half a
page. No history on a sheet ("was", "used to be", "the night of"); history is
in the commits. A session may add "a session suggests: X" only when Sid has
asked for suggestions, and then marked as a session's.

## The questions file

`DECISIONS.md` beside this page holds every question once: its id, its group,
its title, its state (open · answered · superseded · moot), where its source
lines are, and, when answered, the answer, by whom, and the date. A session
writes its name on a group there before it writes the group's sheet, so two
sessions never write one group. An answer is written there the day Sid gives
it. The act's twelve answers of 6 October live in `src-store/README.md`; the
file points to them and does not copy them.

## The sheet files and the build

One text file per group, in `groups/`, in the mini-format that
`make_sheet.py` reads: `=== id | title` opens a question; `## heading` opens a
part; `- ` a word; `[ ] **A** — ...` a choice; a fenced block is a thing shown
as code; `=ANSWER ...` is the answer line. The script lays the group out as A4
portrait pages and writes the PDF to the research depot
(`/mnt/data/projects/research/softland/`), never into git.

Before a sheet goes to Sid, a second session reads it against this page and
against Sid's rulings and words, and reports only what does not hold.

## What the first two sheets taught (Sid, 6–7 October 2026)

- **The picture first.** Sid could not answer a question about "the record" or "the
  gate" without the whole flow in front of him: "I need a system architecture and
  data flow to make sense of the different parts." Page 1 of every sheet is the
  data-flow of one act — maker → door → depot → gate → record → answer and reads —
  with each word defined where it first appears, and the part the sheet asks about
  marked on it. The words list goes to the back.
- **Say its kind.** Each question's first line says which of Sid's two categories
  it is: *system sets* (one choice, ours) · *group chooses — the default is the
  question* · *target*. His words: "softland provides a spectrum of choices that
  the entities make; so two categories: something that has only one choice that
  the system has to set, or a spectrum, and what are the defaults."
- **Say what it is to a person**, not only what Rama does: "work I engage with
  directly must be interactive; data I am subscribed to may come a little later."
  A Today line is still a fact; the "if" lines may say what a person would feel.
- **No word that hides the thing.** "name" hid that an act's id carries its layer
  and class; it is now *act-id*. "class" is a borrowed word Sid has not accepted;
  it stays only until he gives one. Never a word from the rig's code.
- **No premise a session cannot source.** A drawing or a booklet that says Sid
  agreed is "the drawing says", never "you agreed". His words only with their date.
- **Half a page, and no bookkeeping.** Questions about the sheets themselves go
  to group 9, never onto a sheet about the store.
- **Already answered is not asked.** It is listed at the end as "answered, see
  the ledger".
