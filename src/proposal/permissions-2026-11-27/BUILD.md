# Build: the permission fix, in four batches

For the session "permission-fix-implementer". From the permissions working
session, 27 November 2026. This is a session's text: Sid's own line in your
session is what starts a batch, and what says whether you go on from one
batch to the next without a new go.

Base: `PERMISSIONS.md` beside this file (P). Its section 6 is what Sid
decided, word for word. Its section 12 is what a session settled. Under
each batch, "decided" names what is his and "a session's" names the rest.

## What is settled before you start

What a give holds is decided: who holds it (item 1), what it allows
(item 2), what it covers (item 3), who gave it and what it is lent from
(item 5), and that an act names the give it acts under instead of carrying
it written out (the answer he said "5. yeah" to).

How that is stored is yours: the id, the keys, where a field sits. Your
record stands, as you amended it on 27 November: a give is a fact on an id
its giver makes, as a yes is today; its value holds who gave it and under
what from the start, so both stay readable after a change; and an act
carries the give's id. It replaces point 1 of P section 12, which was
written without the code.

Which goes first, this or the fixes of the review of 26 November, is Sid's
(P section 12, "The order"). Your position and this session's are the
same: this build need not wait, and the two should not run in the same
files at once.

## How a batch is given

Each batch says what must be true at its end and what to bring back. How
you get there is yours. A batch ends in a state the store can rest in: its
proofs run, and it is committed.

Back to Sid: a batch whose "must be true" cannot be built; a change to what
a give means, as against how it is stored; what `CLAUDE.md` puts with him.
Not to him: a point of P section 12 that the code makes wrong or needless.
Take the road that keeps "must be true", and report it.

In no batch: the fixes F1 to F34 of
`src/proposal/design-pitfalls-review-2026-11-26/DECISIONS-AND-FIXES.md`,
but for F29 in batch 4 (say where you touch another, and leave it);
`:session` leaving the act; groups beyond a lane's default group; runs as
parties; refer; two acts written from the same one; counts and time
limits; anything across lanes; the questions open in P section 12; pieces
5 to 8; deleting a file. The dated Decisions sections of
`src-store/README.md`: draft the entry in your report, do not write it.

Bring back for each batch: the proofs run and what each gave; from
`git diff --stat`, what changed and every change beyond the batch; what
the code showed that P has wrong.

## Batch 1: the give gets an id

Decided: a holder can have more than one permission (‹95754ddf›, item 1);
an act names the give it acts under (item 5); a holder may end her own
(item 18). From the bug list of P section 5, not a decision of his: a give
never given is answered apart from one that ended. A session's: that this
batch changes nothing else.

Must be true:

- A permission is a give with an id of its own. An act names the give it
  acts under. An end names the give it ends. The old form is refused for a
  new act.
- A holder can have any number of gives on one lane (bug 1).
- A give never given is answered apart from one that ended. Given again,
  it is a new give with a new id (bug 3).
- A holder can end her own give, a see-only holder too (bug 4).
- Everything else is as today. A give falls with the one above it. Public
  is the lane's setting. A lane's first give does not end. Who may end
  someone else's give is as today. A per-run permission is a give good for
  one login sitting, and is not renamed into a run.
- The record keeps apart what today's form fuses, with today's behaviour
  as the value in it: "lent from" is always the give above.

Bring back also: a proof for each of the first four lines, and the number
of pushes a live viewer now holds, measured.

## Batch 2: lent or standing, the right to give, who may end, your own agents

Decided: item 5 (lent or standing; an agent or a run holds only what is
lent; a person stays when the one who let her in loses hers); item 2 (give
only what you hold); item 18 (who may end a give); item 19 (lending to
one's own agents and runs comes with holding; another person needs the
right to give). A session's: points 3, 4 and 12 of P section 12, and the
reading of "her own agents" below.

Must be true:

- A give is lent from another or stands on its own. A lent give falls with
  the give it is lent from, and never allows or covers more than that give
  does at that moment. One that stands on its own does not fall when the
  one who gave it loses hers.
- A give to a person stands on its own unless its giver lends it. A give
  to an agent or a run is always lent, and its chain ends at a person. For
  this the gate tells a person from an agent or a run.
- What is made under a lent give is lent from it.
- A give never allows or covers more than the give it is made under.
  Passing to another person needs the right to give, and that right says
  what may be given on. Day one takes two values: "see", and "see, write
  and give all of it".
- Whoever holds see or write may lend it to an agent or a run, with no
  right to give. His words are "her own agents and runs". The store cannot
  tell whose an agent is, so a session reads it thus: an agent or a run
  that holds a give lent from hers acts for her under it (item 8). Say so
  if the code can tell more.
- These may end a give, and nobody else: its holder; whoever gave it,
  while she still holds what she gave it under; whoever holds a give it is
  lent from, directly or further up; whoever holds everything over what it
  covers.

Bring back also: a proof for each line, among them a person who stays when
her inviter's give ends, an agent that falls when its person's give ends,
and a read-only holder whose agent can read what she reads.

## Batch 3: several changes at one moment, change, owners and the hand-over

Decided: item 7 (several permission changes in one act pass or fail as
one); item 6 (the owner is a right apart from the maker); item 21 (owners
each act alone; a lane always has an owner; a whole lane can be handed
over in one act). A session's: points 2, 6, 7 and 8 of P section 12, and
what "owning" is made of below.

Must be true:

- Every change in one act is checked against what its author held just
  before the act. Changes may name what the same act makes. Then the
  result is checked as a whole: no give left lent from one the same act
  ends, no give changed twice, and the lane still has an owner. All of it
  passes, or none of it is kept.
- A change keeps a give's id and replaces what it allows or covers, never
  its holder or what it is lent from. Raising needs what giving needs;
  lowering needs what ending needs.
- Raising or lowering someone leaves her in throughout and ends nothing
  she lent. What she lent is capped by what she holds at that moment
  (bug 2).
- Owning is a give that holds everything over a lane: seeing, writing, its
  settings, giving all of it on, ending any give over it. It can be given.
  Any owner writes the lane's settings.
- An act that would leave a lane with no owner give that stands on its own
  is refused. A lane's first give can end in an act that leaves another
  owner. A whole lane is handed over in one act.

Bring back also: a proof for each line; an act that fails as a whole on
one bad change; one owner ending another's give while an owner remains.
Sid was not shown that last case (items 18 and 21 read together), so name
it in your report.

## Batch 4: public as a give to everyone, and the proof over the whole

Decided: item 20. A session's: the statement to prove as P section 12
words it, and point 16.

Must be true:

- "Seen by everyone" is a give whose holder is everyone, written in the
  lane's order when the lane is made or opened. Ending it closes the lane.
  There are two such holders: anyone at all, for reading, and anyone
  signed in, for writing. An agent or a run is never everyone on its own:
  it comes in through its person.
- No access comes from a setting of the lane or from a value in code,
  apart from `:softland.store/who-may-read-acts`, which this build leaves
  as it is. What a lane starts with is in the log (F29 of the review page).
- The statement to prove of P section 12 holds as one of the store's
  proofs, with its exceptions named.
- A transfer in one act never shows a live viewer a state in between.
- Who owns the base stays as it is today.

Bring back also: a proof for each line.

## Confirmed clarifications (27 November 2026, Loop 0c)

Confirmed by Sid after the cross-family contract audit:

- On a lane where `:anyone` or `:anyone-signed-in` holds a permission (Batch 4), a signed-in person may lend that permission (`:lent-from` naming that permission's id) to an agent or a run under Batch 2, so an agent comes in through its person (item 20, item 19).
- Under Batch 2 ("whoever holds a give it is lent from"), "holds" means the party named as `:holder` on that permission. Being covered through `:anyone` or `:anyone-signed-in` does not make a visitor the holder of the public permission, so a stranger cannot end another person's agent borrow `:lent-from` it (only the borrow's holder, whoever gave it while what it was given under still stands, or an owner of what it covers may end it).
- Neither the store nor `runtime/seam` adds an `Auth` `made-by` check on lends (Batch 2's reading of "her own agents" stands as written above).
- `:softland.store/who-may-read-acts` is left as it is in this build, except that in Batch 3 any owner of the lane writes it (not only the first holder).
- Granting or raising a permission whose `:holder` is `:anyone` or `:anyone-signed-in` requires `:everyone` inside the grantor's `:give` set, in addition to the right being passed (`:everyone` inside `:give` is passed on only when the grantor's `:give` contains both `:give` and `:everyone`; an owner's permission holds `:everyone` in `:give` by default). Inviting specific people (`:person.<id>`) uses `:give` without needing `:everyone` (`PERMISSIONS.md` §6 item 26, `Option B`).
- Ending or lowering another party's permission as `:given-by` (Batches 2 and 3) requires that the permission it was given under still stands **and** currently allows passing what the target permission holds to the target's holder (`may-pass?`), so a giver lowered in place cannot revoke writers or owners she could no longer grant (`PERMISSIONS.md` §6 item 26).
- Every grant and every in-place `change` of a `[:default-group lane]` permission writes `[:holder-permission key stamp]` with that decision's stamp (and `nil` on `end`), so live holder-run pushes advance on in-place changes (`Loop 0d` Invariant 1). Live views (Batch 4) hold standing from `lane-rights` only while the view's live pushes match the returned `read-set` stamps entry for entry; otherwise `:not-known-yet`.
