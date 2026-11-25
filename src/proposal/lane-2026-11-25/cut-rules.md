# The cut: rules for every file

Sid's word (25 November 2026): code says what happens, checks say what must
hold, and prose stays only where it is the one home of something. This pass
cuts the docstrings and comments of the source files. The pattern is
`src-store/softland/store/gate/first_act.clj` as it stands now: compare it
with `git show HEAD:src-store/softland/store/gate/first_act.clj`.

## What you may change

- Only docstrings and comments, and only in the files given to you.
- No code: no symbol, keyword, number, or string that is not a docstring. A
  string that a function returns or prints, a tool's text, a help text and a
  refusal's sentence are code. Leave them exactly.
- No tests, no pages (`.md`), no other files. No git command that writes
  (no add, commit, stash, checkout, restore). Other forks are editing other
  files in this tree at the same time.

## Each sentence goes one of four ways

1. **It retells the code: remove it.** What the name and the arguments
   already say; the steps of the body; "Inputs and results, by call" lists;
   lists of a namespace's functions; who calls it; "State and effects.
   None."; cross-references that restate another docstring; "Pure code. It
   needs no cluster" on a function (keep "Pure code." once, in the
   namespace's docstring, when true).
2. **It says what the code relies on and does not check: keep it, short.**
   What must hold before the call, the shape or unit of a value that the
   code does not show, a limit of what a result means ("True says only
   that the act has the form").
3. **It gives a why, a trap or a limit: keep it, in the fewest plain
   words.** Why this and not the obvious other; what Rama does here (an ack
   that can fail, a retry, an order); what a run found; a hazard; what is
   not built. Keep exact numbers, and the citation of a finding or a run.
4. **It says whose call a thing is: move it to data.** "Not decided",
   "kept as built", "a session's", "the builder's", "Sid's to say", "his to
   turn", "Sid's decision of ...", "a number of the build". Take the
   sentence out of the docstring and write one entry in your calls file
   (form below). If the same sentence also holds a why or a trap, that part
   stays in the docstring.

## Form

- A docstring that has nothing left is removed whole. A namespace keeps one
  or two sentences on what the file is for.
- Plain words, as the prose already is. Do not add vocabulary, and do not
  write history ("was", "used to", "now").
- Sid's quoted words stay exactly as typed, inside their quotes, wherever
  they end up. In text you keep, the store's unit is "lane"; inside his
  quotes "layer" stays.
- Do not rewrap or touch code lines. Keep the docstring's indentation of two
  spaces.

## The calls file

One EDN vector per fork, at the path given to you. One map per call:

    {:at    softland.store.gate.first-act/starts-with   ; the var it sits on
     :state :not-decided        ; or :a-sessions, or :decided
     :what  "A new lane reads as :members and the base as :everyone only so
             that the build can run. With :everyone a person who is not
             signed in sees the base. This function is the one place where
             the other answer would be made."
     :source "BUILD-reads.md, \"Not decided\""}        ; optional

- `:not-decided`: kept so that the build runs; nobody chose it.
- `:a-sessions`: a session or the builder chose it; Sid's to turn.
- `:decided`: Sid decided it; `:source` names where (a date, a page and
  entry, a line's id), copied as the docstring had it.
- `:what` keeps every fact the sentence held: numbers, dates, what the
  other answer would change. Sid's quoted words go in as typed.

## Before you report

Run, from the repository root, on your files:

    python3 -I src/proposal/lane-2026-11-25/same_code.py HEAD <your files>

Every file must print `same code`. A `DIFFERENT` line means a code token
changed: put it back. Then report: the files, prose lines before and after
if you counted, how many calls you wrote, and anything you were not sure
of, by file and function. Do not paste the docstrings.
