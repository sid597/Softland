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

## The queue of open questions

`OPEN-QUESTIONS.md` beside this page holds what is not yet decided, once each:
its id, its group, its title, where its source lines are, and which session has
taken the group. A session writes its name on a group there before it writes the
group's sheet, so two sessions never write one group. **Answers never live in
the queue.** An answer, with Sid's reason, goes to the README of the code it
shapes — today `src-store/README.md` — on the day he gives it; the queue's row
then only points there.

## The sheet files and the build

One text file per group, in `groups/`, in the mini-format that
`make_sheet.py` reads: `=== id | title` opens a question; `## heading` opens a
part; `- ` a word; `[ ] **A** — ...` a choice; a fenced block is a thing shown
as code; `=ANSWER ...` is the answer line. The script lays the group out as A4
portrait pages and writes the PDF to the research depot
(`/mnt/data/projects/research/softland/`), never into git. Sid answers on paper:
print the PDF with
`lp -d Canon_G600_Direct -o media=A4 -o print-quality=5 -o cupsPrintQuality=High <the pdf>`
(the queue's own default is Normal; the printer is queued when off and prints when
switched on), and attach the PDF in the chat too.

A sheet is pure black on white. Sid's printer has a grey ink beside its black, so
grey text and grey rules print pale. `make_sheet.py` uses no other colour. A thing
is set apart by weight, an underline, a box or an arrow, never by a shade.

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
- **Contract first, then the questions it cannot answer.** The store's sheet was
  written before the build contract; two questions (the store's keys, the
  bootstrap) surfaced only when the contract made "what the gates need"
  concrete. Write the build's contract first; the sheet asks what it cannot
  settle.
- **Answers never live in the queue.** `OPEN-QUESTIONS.md` holds only what is
  not decided and who is writing which sheet. An answer, with its reason, goes
  to the README of the code it shapes — `src-store/README.md` today.

## What the sheets of 8 October taught

The first is Sid's. The other three are what the second reader found in the first
draft of the reads sheet; they are a session's, not his.

- **Black only, at the highest quality.** Sid, 8 October: "use more darker color
  not grey ... use icons or smth else to do typography not color because my
  printer makes them less black and harder to read".
- **Each sheet defines the words of the way in itself.** Maker, act, fact, layer,
  door, depot, gate, seal, lock and stamp were named on the draft and defined only
  on another sheet. A reader holds one sheet.
- **A new sheet takes a letter that no paper uses.** The draft's R1 to R7 met the
  rulings R1 to R8 on his printed sheet and R1 to R5 of the build's review. Search
  the folder for the letter first.
- **The rulings file is a session's writing.** Where a sheet cites it, say "the
  rulings file lists it as ruled, in a session's words", and give Sid's own line
  of that day when the record has one. His line is often a lean, not a ruling.

## What the check of 10 October taught

On 10 October six sessions read the store's papers and code for what nobody
had examined. Sid said yes on 12 October to what follows (`src-store/README.md`,
"Decisions of 12 October 2026 — before real data is kept", entry 6). The
wording is the sheet's ("Before real data is kept", Z5), which a session wrote.

- **No paper gives "frozen at the first kept record" as a reason.** A paper
  that says a thing is due now says what breaks later. Sessions wrote that
  phrase on the sheets of 6 to 10 October, as the measure of how firm a
  question is. Sid typed it in no line, and this page never had it. Those
  sheets are kept as they were printed and answered; no later paper takes the
  phrase from them.

  What holds of Rama, read in its docs and not run. It can write the records
  of a log again, or take one out, at a module update
  (`reference/rama/docs/14-depots.md`, line 288). It can write the values of
  a table again (`15-pstates.md`, line 770). Three things it cannot change,
  and a paper names the one that bears on its question:
  - the number of tasks of a module (`19-operating-rama.md`, line 483, "Task
    scaling": a way past it is a new module that is filled from the logs);
  - the name of a depot or of a table (`19-operating-rama.md`, line 424);
  - the keys of a table: a migration is of values (`15-pstates.md`, lines 772
    to 776 and 897).

  In the store the key of a fact's row is made of the printed texts of its
  layer's id, its entity's id and its key (`place-key` in
  `src-store/softland/store/record/rows.clj`). So what an id is and what a
  key is bear on the third. The two words for a depot do not: no key holds
  them (`answer-key`, the same file).
- **The second reader of a sheet first tries to make each question fall
  away.** It does this before it reads the sheet against this page ("The
  sheet files and the build", above). What is left goes to Sid.
- **A builder's "is Sid's" line goes to the queue the same day.** A build's
  page may say that a choice is his. Until the queue has a row for it, nobody
  asks him. The check found such lines on the store's page that no row held,
  among them "The choice is Sid's", of the order of a layer's acts after a
  failure.
- **A question whose answer is a fact is not asked.** A session checks it and
  states it, with where it was checked. On 7 October "Can two nodes run
  here?" was put to him as a box, and his line was "wtf is "here" which 2
  nodes ???" (‹27882354›). The answer is a line of Rama's docs
  (`reference/rama/docs/19-operating-rama.md`, line 110).
