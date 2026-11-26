# Build, batch 1: the give gets an id

From the permissions working session, 27 November 2026, on Sid's word that
the implementation prompt goes to the session "permission-fix-implementer".
This is a session's text. Sid's go in your own session is what starts the
work.

Base: `src/proposal/permissions-2026-11-27/PERMISSIONS.md` (P). Section 6
is what Sid decided, word for word. Section 12 is what a session settled
for the slice, made without reading code: it is yours to hold against the
code. Sections 4 and 5 say what is built and what is wrong with it, as of
26 November.

## What must be true when you are done

A permission is a give: a record with an id of its own, made from the act
that makes it and its place in that act. An act no longer carries its
permission written out whole. It names the give it acts under. An end
names the give it ends. There is one form: the old form is refused for a
new act.

Three things then behave differently, and they are three of the four bugs
of P section 5:

- A holder can have any number of gives on one lane.
- A give that was never given is answered apart from one that ended. Given
  again, it is a new give with a new id.
- A holder can end her own give, a see-only holder too.

Everything else behaves as today. A give still falls with the one above
it. Raising or lowering is still an end and a give. Public is still the
lane's setting. A lane's first give still does not end. Who may end
someone else's give is as today.

The record keeps apart from the start what today's form fuses, and today's
behaviour is the value in it: holder, what it allows, what it covers (the
default group of its lane, or one exact act for today's yes), who gave it,
the give it was made under, the give it is lent from (in this batch,
always the one above it). P section 12 has the record, and its points 1, 5,
9, 10, 13, 14 and 15 are the ones this batch uses. Today's per-run
permission is carried as what it enforces, a give good for one login
sitting. It is not renamed into a run.

## What is not in this batch

Lent or standing. A right to give that says what may be given on. Who may
end (item 18). Lending to one's own agents (item 19). One act as one
moment, and the change that keeps a give's id. The owner guard and handing
over a lane (item 21). Public as a give to everyone (item 20). `:session`.
Groups, runs as parties, refer, two acts written from the same one. Those
are batches 2 to 4 and later work (P section 12).

Also not here: the fixes of
`src/proposal/design-pitfalls-review-2026-11-26/DECISIONS-AND-FIXES.md`
(F1 to F34). Where your change touches one, say so and leave it. Pieces 5
to 8. The dated Decisions sections of `src-store/README.md`: draft the
entry in your report and do not write it. Deleting any file.

## What is Sid's, and what to do when the page is wrong

The form of a stored record is Sid's call. The record of P section 12 is a
session's; his go on it has to be his own line in your session.

By his answer of the same night (P section 12, "The order") the fixes of
the review of 26 November come first and this design after, as its own
build. He then asked for this prompt to be sent to you. Which goes first is
his: if his line in your session does not say, ask him that one thing.

No code was read for P section 12. Where the code makes a point wrong or
needless, do not force it and do not build around it. Take the road that
keeps "what must be true" above, and say in your report what you found and
what you did. If "what must be true" itself cannot be built, stop there
and say why.

## What to bring back

- The store's proofs, run on the new form, with what each gave.
- New proofs for: two gives for one holder on one lane, each ended on its
  own; an act that names a give never given, answered apart from one that
  names an ended give; a give ended and given again; a see-only holder
  ending her own; the old form refused.
- From `git diff --stat`: what changed, and every change beyond this
  batch, the small ones too.
- What you read in the code that P gets wrong. P's sections 4 and 9 and
  the review's corrections (P section 12, "The review") list what nobody
  has checked.

How you get there is yours: size the process to the work and say it in
your first reply (`.claude/output-styles/build.md`). The review, with its
file and line receipts, is in the record as a paste of Sid's of
26 November; it opens "Part 1 — Misses That Change What Is Stored".
