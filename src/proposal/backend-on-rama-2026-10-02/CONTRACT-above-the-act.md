# Contract: can adapters and converters be data? A test

**NOT IN USE.** Sid declined this test on 3 October 2026: "I don't want to do this test. This
is not a priority to test out." Do not act on this page. Nothing on it is a ruling, and the
seam of section 2 is not confirmed.

## What this is, and why

This page is the instructions for one test. A new session gets this page and nothing else.

Sid wants adapters and converters to be data, not code, so that anyone can bring a source.
Sessions have drawn this. None has written one out, as far as the cutter found. The new session
tries it: it writes the adapter and the converter for two sources as data, with Sid's five names.

What comes back: the adapter and the converter as data, a list of all that had to stay code,
and the places where the chain did not fit. That list answers Sid's question, "what is below
the waist". It is also the list of code to build above the act.

A new session does this, and not one that drew the chain, so that the test can fail. Nothing
runs. Thus the list is a prediction. It is not a build.

Cutter: Claude Fable 5.1 (`claude-fable-5-1`), effort max. 3 October 2026.
Standing: declined, see the top of the page. The cutter read Sid's "just do it" of 3 October
as a yes to the seam of section 2. Sid did not confirm that reading.
Marks: as in `ARCHITECTURE-STE100.md` section 0.2. [SID 3 OCT] is Sid's words of today.
[DERIVED] is the cutter's conclusion: examine those first.
Paths: `PICTURE.md`, `PARTS-4-8.md` and `PROGRESS.md` are in `src/proposal/frame-2026-09-15/`.
The rig is `src/proposal/rig-2026-09-25/`. Inland is `src-inland/softland/inland/`.

## 1. If you have only this path

You are the fresh session. This is the count that Sid decided on 25 September: "the count is
taken on a held-back tool written by a fresh session from the round's one-page contract, so it
can fail." [RULED: `PROGRESS.md:201` on `main`]

Write two data sources through the chain of section 2, as records. Do not run the records. Do
not write to disk. Reply in the chat, with the report of section 6.

## 2. The chain

Sid's five names, in his order. [SID 3 OCT] The two lines in brackets are code that is not one
of the five. [DERIVED]

    data sources   a fact that a person writes: which outside thing, which adapter reads it,
                   which layer its acts go into, with which permission
      (the crossing: it does the ask, brings back what arrived, and gives the basis)
    adapters       given: what arrived. Give: its parts. Each part has a shape, a position,
                   and the name that the source gives it. An adapter can ask for more
    converters     given: the parts. They read the store. They tell which thing each part
                   is, and if it is the same thing as before. They give acts
      (the door: it names each act and puts it into the depot for the class of its layer)
    act            one form, from all sources
    depots         streaming · microbatch

An inside thing has a shape already. It goes directly to a converter. [DERIVED]

## 3. The rules of the cut

Each rule is a test. If a record cannot obey a rule, do not repair the rule. Report it.

1. An adapter knows the source. It reads nothing from the store. Test: its recorded reads hold
   only what arrived and the base functions that it calls. [DERIVED]
2. A converter knows the store. It does not know the form of the source. Test: a different
   source that gives the same parts needs no change in the converter. [DERIVED]
3. The parts go from the adapter to the converter as a value. The store does not keep them. A
   part becomes a thing only when something must refer to it. Do not keep parts to get a
   repeat. Count the repeat (section 6). [PROPOSAL: `PICTURE.md:82` + DERIVED]
4. A function does not touch the outside. It asks. An ask is an act. The crossing does the ask.
   What arrives comes back as an act. [DERIVED: a default, see section 7]
5. The crossing gives the basis. An adapter does not tell where a thing came from.
   [PROPOSAL: `PARTS-4-8.md:273, 276–278` + DERIVED]
6. Which thing a part is, and if it is the same as before, are claims of the converter. A
   person can replace them in the person's layer. [PROPOSAL: `PICTURE.md:82` + DERIVED]
7. A base function is a named function that other functions call by name. People import base
   functions. An adapter is made of them. A converter calls none that knows a source.
   [SID + DERIVED]

## 4. Defaults

- **Language:** Inland's, `total.cljc`: seven kinds of step and a fixed table of leaves.
  [RIG: `total.cljc:14`] Start at `src-inland/README.md`. Example records:
  `resources/inland/seed.edn`.
- **Only ask:** rule 4.
- **Permission:** a data source uses a permission narrower than that of the person who wrote
  its fact. [DERIVED]

If a default does not hold, write down the record that you could not write. Report it.

## 5. The work

For each source, write the data-source fact, the adapter, the converter, and each base function
that they call.

**Source 1: the repo.** A file at a git revision. The request is `{:repo :line :rev :path}`.
Match the one act that `rig.material/reading-facts` gives for the same request
[RIG: `src/rig/material.clj:402–460`]:

- `:material/file` on the thing of the file (the reading), and `:material/text` on that thing;
- one `:material/form` for each named form, on the thing of the form;
- one gone `:material/form` for each name that the previous reading held and this one does not.

Each fact replaces the previous fact of its thing and key. Match also the outputs for a missing
file and for a file that cannot be read. The rig's reader shows the parts: units with content
and position and no identity. [RIG: `src/rig/revision.clj:3–13`] Do not use the rig's
`:material/reading` step. It knows the source. [RIG: `src/rig/store/recipe.clj:148`]

**Source 2: Sid gives it** in his first message to you. This page does not name it, so that the
count can fail. If Sid names none, use sign-in with Google, and say that it was not held back.
Count apart the code for its one exception: sign-in decides who the actor is. [DERIVED]

## 6. The count and the report

The unit and the classes are the rig's. [RIG: `BUILD_NOTES-citation.md:44–50`] A unit is one
named code unit added to the fixed side or to the vocabulary, or one existing unit whose
behaviour changes. The classes are promised, unanticipated, and capabilities. Earlier counts:
citation tools 22 (4, 7, 11); micro side 4 (2, 2, 0); pointer build 42 (9, 29, 4), provisional.

For this count, promised means "on this list". All other units are unanticipated.

- the crossing for files and git, and the crossing for the network [PROPOSAL: `PARTS-4-8.md:273`]
- the evaluator, which records each read [RIG: `gesture.clj:1–14`]
- match, the door, the gates, resolve [PROPOSAL: `PARTS-4-8.md:324–326`]
- a reader of grammars [PROPOSAL: `PICTURE.md:30`]
- a repeat over a finite list; Inland's language has none [DERIVED + RIG: `total.cljc:14`]
- a limit that stops a run; Inland examines size after the run [DERIVED + RIG: `total.cljc:6–8`]

For each code unit give its name, its class, the rule that needs it, and one mark: does it know
a source? A "yes" is a failure of the cut. Name the source.

The report:

1. The records.
2. The count: one row for each source. Columns: Promised, Unanticipated, Capabilities, Total.
3. What this contract could not say.
4. Each place where rule 1 or rule 2 failed. Each default that did not hold.
5. The cost of "only ask" for one commit of one file: the asks, the arrivals, what the store keeps.
6. What you did not examine.

This count is a prediction. It becomes a measurement when the records run and the output for
source 1 is equal to the rig's. [DERIVED]

## 7. Open, and Sid's

- **The screen.** Here, "inside" means "has a shape already". Sid, 2 October: a click "is part
  of Softland itself so it is inside inlet". [SID] The proposal sends the return of the screen
  through the outside door. [PROPOSAL: `PARTS-4-8.md:279, 324–335`] Nobody decided.
- **Can a function touch the outside?** The default is no. Sid decides before the first kept
  record. The opus session of 3 October named this D3.
- **The permission of a data source.** Section 4 gives only a default.
- **Identity, address, basis.** `ARCHITECTURE.md:158` gives all three to the adapters.
  `ARCHITECTURE-STE100.md:310` gives all three to the fixed part. Rules 5 and 6 divide them.
  This page does not change those files, or D1 and D2.
