# Inland integration — starter for the session after 14 September 2026

The raw prompt, in Sid's first person, to paste into the next session.
`FACTS.md` beside this file is the gathered context it refers to. Flavorings
are one-line appends; Sid decides which, and to how many sessions.

## Raw prompt

```
load src/proposal/inland-integration-2026-09-14/FACTS.md. it is the context
gathered last session (14 sept): what was read, the checked facts with file
and line for src/*, src-inland and the client, the product side as settled and
in my words, the discrepancies between intended and implemented, one session's
derivations marked as such, and the questions we carried forward. nothing was
run.

the work is integrating src-inland into src/*. we have the existing
architecture, where we want to go from the product pov, and how that
implementation looks like. the base is the current architecture; inland
contributes.

last session we worked through the current and the proposed architecture
through the product lens, following my line of questioning: something to
point at in the client, where it exists (rama or code), what happens when we
change the code, where the data is and how it is addressed and who is asking,
who composes the shape, who decides the view and the actions, pointing "this
thing" and "do X to" it, how the update reaches the view, and how deep
tool-updating-tool goes and why not further. the facts file ends with the
questions that came out of that, split into this instance and the bigger
buildups.

this session i want to go from there: ___

my plan was: ask what are the minimal things to add to our current
architecture and why, and go from there. keep the split 80-20 current instance
vs future, and when we make a choice say whether it is future forward. keep
what is checked, derived and assumed distinguishable.
```

## Flavorings

```
read nothing on disk, no code, never src/app/server/env.clj.
read nothing on disk; code only if a claim needs a receipt, scoped to one file, say why you went; never src/app/server/env.clj.
read nothing on disk; code only if a claim needs a receipt; never src/app/server/env.clj.
attached: src/proposal/inland-integration-2026-09-14/FACTS.md. i am asking ___ and what i want from it is ___.
```
