---
name: design-pitfalls
description: Use as an adversarial review of a concrete design, plan, stored shape or page of decisions BEFORE it is fixed or put to Sid. Looks for one id, key, word or record doing two jobs; for things fixed at write that could be drawn at read; for sizes built as kinds where a value would do; for a session's reading written as Sid's decision; and for a fix that grew into new building. Complements rama-pitfalls (Rama's own foot-guns). Not for inventing a design, and not for code style.
allowed-tools: Read, Grep, Glob, Bash
---

# design-pitfalls

Run this as an adversarial review of something concrete: a plan, a stored
shape, a diff, a page of decisions. Its job is to break the thing, not to
design it. The rule it checks is in `CLAUDE.md` ("Defaults, not conflations"
and the paragraph after it). This page is how to hold a concrete thing
against that rule.

A session wrote this on Sid's word of 26 November 2026. The questions are a
session's, each drawn from a case that happened in this repository. They are
cases to check against, not laws, and each is his to turn. A question is
added when a real mistake teaches one, with its case.

## When to run

- Before a stored shape, an id, a key or a word of the product is fixed.
- Before a plan or a list of decisions goes to Sid.
- Before something is written down as his decision.
- When one of the signs on the root page shows.

## First, the inventory

Before any question, list from the thing under review:

```text
each id, key, word and record
  -> every job it does: what reads it, and to decide what
  -> for each job: decided when (at write, at read), can it change later,
     one or many, who changes it
what the change fixes for good
  -> stored in an act, shown to someone, or said to Sid as decided
what Sid was shown, in which words, and what he answered
```

A job that cannot be said in one plain sentence is a finding by itself.

## The questions

1. **Does one id, key, word or record do more than one job?** Name each job.
   Case: one id of the store is the order of its acts, and by the store's
   pages it also decides who may write, who may see, what a read looks
   through and who sets its settings. A session's reading: only the first has
   to be that id.
2. **Do the jobs on one thing differ in when they are decided, in whether
   they change, or in how many there are?** Then they are two things, and
   "the same for now" is a default value. Case: `:see-and-act` is one word of
   a permission for two rights. Who may see is asked at each read; who may
   act is decided once by a gate, and its answer is kept.
3. **Is a size, a kind or a limit built as a form where a value would do?**
   Case: a permission covers a lane, one run or one exact act, each as a form
   of its own (`:permission-id` in `src-store/softland/store/act.clj`), so
   each further size is another build. Sid: "providing spectrums of
   possibilites and choosing a single defualt to start with" (‹45c7127b›, a
   long prompt).
4. **What does this fix for good, and does it have to be fixed now?** His
   test for what an act's order has to hold: "what does an act need to decide
   while it passes at "one present moment" through the system" (‹68b2b592›).
   What that moment does not need can be drawn later from acts that are kept
   whole: "why do we need to answer and discuss this at this stage? we have
   the stamps for each act right?" (‹23f4a742›).
5. **Is something decided at write that could be drawn at read?** Case: what
   an act belonged to was fixed by the unit it was born in: "we cannot
   provide granularity after the fact that it is committed" (‹9e06f4fe›,
   dictated).
6. **Is the finest grain kept?** "we can combine these to make a bigger one
   but can't decompose it to atomic units later on" (‹e36008e1›).
7. **Does a choice of design reach Sid as an either/or?** Say the range it is
   a point of, and the default. Case: a second permission for one holder was
   put to him as a yes or no: "why is it being presented as a binary
   decision? we are a provider of spectrum" (‹6db69c3b›).
8. **Does a word of his name something built?** Say in an example what the
   built thing is. Is it the whole of what he means, a part of it, or
   something that only matches it today? Case: "layer" (`src-store/README.md`,
   the Decisions of 25 November 2026).
9. **What was he shown when he said yes, and what is being drawn from it?**
   Open the line he typed (`memtool line <id>`); where the record cannot be
   reached, say so. Case: the rename of the store's unit was committed as his
   word for "lane" before any line of his held the word.
10. **Is this the fix, or building on top of the fix?** Case: the plan for
    that rename took in the four pieces that come after it: "this is only
    related to what we already did and fixing that mistake anything we need
    to build on top of it is different" (‹f07c97d1›).

## The brake

A split has a cost: one more name, one more thing to hold in mind. The root
page's test stands: split only what differs in lifecycle or in count. A
finding says what the two jobs are and how they differ. "These could be
separate" is not a finding.

## How to answer

- Each finding: the thing, its jobs, how they differ, what it costs to
  separate them now, and what it costs later (what will have been stored,
  shown or built on by then).
- Quote the line or the code, and say what you read and what you did not.
- Name no fix beyond the split. The design is its author's and Sid's.
- When nothing is found, say which questions were asked, and of what.
- Rama's own traps are `rama-pitfalls`; what is written beside the store's
  code is `src-store/CLAUDE.md`. This page repeats neither.
