# Two chairs — the preliminary workflow for building and attacking a kind below the waist

Preliminary, 2026-09-06, written at Sid's word after the path round's fourth attack. Distilled from sessions 8 to 12 of the path kind (`path-kind/`), with the 3D round (`3d/`) running the same shape. Every rule here is a default with its reason; a departure says its reason out loud. Sid redlines anytime.

## The two roles

**The composer** holds the picture: what code must exist below the waist, its input and its output, why; the contract; the cascade into the tree; the page; the bench. It folds what the other chair finds and carries the view forward.

**The definer** holds the definitions: whether each named thing means one thing; the hardest cases worked through the proposed machinery as concrete constructions; the work hiding behind a name. It writes what it finds as `attack-N.md` with records and numbers, and takes provisional positions saying what would change them.

Roles are points of attention, not limits: either may contribute a construction, an alternative or a clearer picture. When the definer pushes it pushes on correctness and is usually right about the detail; the composer takes every counterexample as a fix at full weight and never as a reframe unless the picture itself fails. Neither chair ranks the other; Sid carries paths between them. One model family per chair, and a ranking from one family counts as one vote.

**Sid** opens a round with its essence, pastes the paths as they land, rules the positions that are his (verbatim, with the time, on the page's ledger), and decides when boots go on the ground.

## The cycle, and its two phases

```
Sid opens the round (essence)
   │
   ▼
composer: picture + contract + page + bench      definer: attack-N.md = hard cases as records, run beside the bench, with numbers
   │                                              │
   └────────────── the directory ◄────────────────┘      Sid pastes each path into the other chair
   │
   ▼
composer folds: reproduce the attack's numbers on the definer's own route → fix the bench, code first → the attack's records become fixtures, its numbers receipts
   → the page: what changed in the picture or the contract; a table of where each finding landed
   → fact-base-N, HANDOFF-N, STARTER-N; republish once at the end; commit, exact paths; push is Sid's
```

The round has two phases, and the tell between them is what an attack adds.

**Phase A, attack the picture.** An attack can add a piece or move an arrow (the path round's attacks 1 and 2 added the executor and the compositor). The page is the deliverable and the bench is its evidence. A fold repaints the picture, rewrites the contract, and lands the bench that shows it.

**Phase B, attack the contract.** An attack adds an obligation and no piece (attacks 3 and 4: results keep their contents; a checkpoint is a continuation; a value a record reaches stays alive; a checkpoint as bytes). The bench is the deliverable and the page is the ledger. A fold is: reproduce, fix, fixture, receipt, and one line in the contract if the contract changed. No page repaint and no republish unless the picture moved. The attack's records land as files beside the prose; every fix keeps the earlier records passing, so the attacks are the test suite.

Guard for the switch: "name a piece by its output". When two attacks in a row add no output no other piece has, the round is in phase B. The round's stated question decides when it is done; for a kind that is "make a new tool, change its behaviour through data, reuse its results", all three running on the bench.

**After phase B, the implementation rounds** (Sid's ruling, 2026-09-06): the prototype is the spec, satisfied from both models; what remains is production. No contract is written first and no test suite before the code; the builder builds the tests as it builds; the documentation is the hierarchy from the READMEs down to the docstrings down to the code, carrying what is implemented and how, changed in the same commit as the code. The question held is how the best team in the world for this would do it. Functions follow the functional programming rule. The definer's records are what the implementation is measured by, and the definer keeps its chair against the implementation. The work-package law is not used here: "an old old thing ... it creates too many contracts".

**Two attackers are needed, and one has run.** The definer tests soundness, which Codex is good at. Whether a person or a cold agent can say a tool in the language is the claim the work rests on, and it is tested by Sid on the bench with his own change-it moves, and by a cold-mind attack: a fresh session given only the contract, asked to write a tool as a record; whatever it cannot say is a finding. Both are attacks like any other and fold the same way.

## What the guards say (each a default with its reason)

- **Code first, page second.** A construction runs before its picture is drawn; the definer's numbers are the receipt at every step. Reason: every definitional error of the round was a renderer quantity promoted to a definition; a bench catches it, a paragraph does not.
- **Reproduce on the definer's route before fixing.** The pure declarations run in Node; the attack's finding is reproduced there first, then fixed, then verified headless. Reason: a fix without the reproduction is a guess; the route is landed under `path-kind/bench-9/node-route/`.
- **Counterexamples at full weight, rankings at none.** Reason: each family ranks its own higher; a counterexample with numbers has no family.
- **Scratch copies measure, fixtures land.** A fixture is a record the definer wrote verbatim or a construction the page names; anything else is measured on a scratch copy and reported as such. Reason: no fixes nobody asked for; the bench is the definer's paste target and stays small enough to read.
- **A receipt that disagrees with the reference is a lead, not a tolerance.** Find one point where a single operation must give an exact answer and read there.
- **Page bytes live in the scribes.** The chair authors patches and text; a page-scribe maps, applies, checks, publishes and returns a receipt. A bench's script is a page too. A departure names the function it must patch inside and says so. Reason: reading a page costs the session for the rest of its life; Sid, 2026-09-06.
- **Write each thing once, and the dumps never enter the chair.** The chair's context is the fold's price: code goes to disk as a file the scribe embeds, never pasted twice; the page is written once, after the dumps; the scribe that made the dumps writes the fact base table and the handover's measured rows itself and returns only the lines the page will quote; the shader is the scribe's to write from the chair's definitions. Reason: session 3's fold on the 3D kind reached 330K, and about a third was the same functions and the same numbers passing through the chair two to four times. A fold larger than one construction splits at the zone boundary: the thinking session ends with the patches at the scribes and the handoff written; a second session at a lower effort lands from the handoff and the receipts on disk. The total is about the same; the expensive tail, every late request re-reading the whole prefix, is gone.
- **Republish once per session, at the end, both artifacts together.** The definer reads the committed files and their hashes, not the URLs.
- **Durable work lands under `docs/` and is committed**, exact paths, plain commits, one per milestone; push is Sid's. Never `/tmp`.
- **Positions asked once.** A fork Sid's word closes is asked once, marked POSITION with the condition under which it stops being the answer; it is not asked again unless he reopens it.

## How to write the prompts

Three kinds, and the law for all three is in memory: a prompt is fence, essence, terrain and the two riders, nothing else; no output shape, no method, no register block, no restated law. Prompts point at this document instead of restating it.

**1. Opening a round (a new kind, a new question).** Written after the exploration has moved in the chat, in Sid's first person. Two variants, one per chair, differing only in the role's terrain.

```
[the fence: what code may be read, what never]
[Sid's essence: what he is building and why, his adopted frames marked "working basis I want attacked", none of the session's answers as conclusions, what he wants to end up holding]
[terrain: what exists and where; this document; the directory the chairs share; the other chair, by name, and that Sid carries paths between them]
[rider a: work the hardest case through whatever is proposed and say where a named abstraction hides unfinished work, ending on the exact open question]
[rider b: where a position is taken, what supports it, when it becomes only a default, and what else changes with it]
```

The composer's terrain adds: hold the picture; bring forks only where Sid's word closes them. The definer's terrain adds its standing question for a kind: with the machinery proposed, how could someone create a new tool, change its behaviour through data, and reuse its results; and that it writes `attack-N.md` with records and numbers. That is all either adds.

**2. Continuing a chair (its context is full).** Two short paragraphs in Sid's first person: you hold this chair's view (handoff, page, bench, fact base, the live URLs); the work is done, do not redo it; the fence; I will paste you the feedback as it comes, naming what sits in the directory unfolded; the item that is Sid's and has been asked; the still-owed list is on the handoff. No new hardest case, no riders, no assignment. A closing paragraph names the session's zones, because the model cannot see its own context size or set its own effort and Sid can: the load turn replies in one line and waits for the effort switch; when the fold's patches are with the scribes it says so in one line, so the landing runs at a lower effort; if Sid names a context size, it answers with the cut point and the compact prompt it wants (the fold's thinking is on disk in the patch texts by then, the view in the handoff and the page, the maps and dumps in the scribes and the fact base, so the compact keeps the chair, the fence, the paths, the scribes by name with what each was asked and returned, and what is committed and published). Landed as `STARTER-N.md` beside `HANDOFF-N.md`.

**3. A phase B attack or fold.** No new prompt: the continuation starter carries the role, and this document carries the shape of a phase B fold. If a prompt is needed at all it says which phase the round is in and points here.

**The first implementation round is a comparison** (Sid, 2026-09-06; the definer's reply the same day folded in). Both model families get the builder starter unchanged, the same starting code and the same behavioural target, each in its own worktree on its own branch, and each owns all three responsibilities end to end, so that the question about production ability is answered rather than dodged:

| Responsibility | What it involves |
|---|---|
| Production design | find the data layers, the dependencies, the state owners and the function boundaries; decide where reactivity helps, if anywhere |
| Implementation | build the native client code, integrate its callers, replace the obsolete code, own resources, keep the hierarchical documentation with the code |
| Verification and repair | exercise the real behaviour, find failures, write the tests that earn their keep, fix the implementation |

The judge is the definer's records passing, the harness running and what Sid sees on screen, never one session ranking the other. The card below merges the composer's observable rows into the definer's frame (both 2026-09-06). It is written for a judge who reads no code and did not choose the approach: the burden of making the work judgeable is the builders'. Before scoring, each builder states plainly: this now runs in the client; this still exists only in the prototype; this remains unfinished. Marks per row: 0 fails (a demonstrated failure) · 1 partial (works only partly, or needed repeated intervention) · 2 works (demonstrated for the requested behaviour) · 3 holds up (also survives a meaningful variation or follow-up change) · ? unverified (not shown enough to judge; a valid answer). Rows stay visible, never summed; the first delivery is scored apart from the result after feedback. Lines of code, number of tests and confident prose earn nothing. Each run keeps its model, version, effort and tools attached.

| What Sid judges | What the builder must show, and what Sid looks at or asks | Codex | Claude |
|---|---|---|---|
| **1. Does the intended thing work in the client?** | The harness fixtures on screen beside the bench (the crossing painted once, the tapers swept, the border one pixel at every zoom, placed ink in 3D still drawing). The definer's records run through that branch's client implementation, every displayed number produced by the client and compared against the handover's tables, which supply only the expected values (re-running the prototype establishes nothing about the transfer): the crossing .62 as a union and .8556 as dabs, the pickup texel (0.013, 0, 0.987, 1), red after blue. Changes and repeated use shown; glitches and delays exposed. `git diff --stat` on the branch and the answer to "what did you delete?": the triangle shaders, mesh preparation, zoom bands and the tessellator's use by placed ink gone, not alive beside the new; a reviewer verifies that the old route's callers were migrated, since a diff summary alone cannot show the old route is unused. | | |
| **2. Can tools be created, changed and reused as data?** | Ask: "change how this tool behaves by changing its record, then use something it produced in another tool." A new recipe from capabilities already implemented; its behaviour changed through the record; a result fed into another operation (a returned outline as a clip is one). Shown which data changed and whether any engine code had to change. The client's test surface is enough; no editor UI is needed for this. | | |
| **3. Is the implementation sound?** | A fresh technical reviewer, one from each model family, reviews both builds, to reduce reliance on one reviewer's blind spots; where the two disagree, the specific code claim is resolved or the example reproduced, their disagreement being evidence to examine rather than a vote; every conclusion points to code or an executed example, and Sid receives the practical consequence in plain words ("changing a colour rebuilds every outline"; "painting again changes a result that was supposed to stay frozen"). The reviewer checks: the tools call the shared machinery; pure transformations and mutable resources have clear boundaries and owners; changing a colour, a geometry or the camera runs the appropriate work and no more; the replacement reaches the real callers and what was kept or removed fits it; the documented behaviour agrees with the code. Ask the builder: "draw me the data flow in ten lines, with who owns each state and where reactive code is and why", and "what did you carry forward from the old tree, and why each?" (under the ruling, each argued from the experience). Stays ? until a reviewer with receipts has checked; an unsupported score weighs nothing. | | |
| **4. Can someone else continue the work?** | A fresh session makes one small, concrete change using only the READMEs and docstrings: can it find the place, make the change and verify it without the builder explaining? Sid reads the new namespace's README for two minutes: does he know what is implemented and how, in plain words, matching the code's date? | | |
| **5. Was the builder a useful collaborator?** | Sid's own experience: sensible decisions, kept moving, consequential choices explained, meaningful verification, and effective repair when something failed ("what broke while you built, and how did you find it?"; finding bugs is not required for a good score), remaining uncertainties stated with what would resolve each ("which decisions are you least sure of?", no particular number expected), a test that fails when the crossing is painted twice, run in front of Sid. How often Sid had to repeat himself or rescue the task. | | |

| Cost of getting the demonstrated result | Codex | Claude |
|---|---|---|
| Elapsed time, including repairs | | |
| Reported usage and cost, subagents included | | |
| Times Sid had to repeat a requirement or redirect | | |
| Required work still unfinished | | |

Stage two, the exchange, is judged on the same card, scored apart from the first delivery: things landed on the other's branch (a bug with a receipt, a test, a repair), and whether the other's sound decisions were kept or rewritten for taste.

A second stage tests collaboration: once both implementations exist, each inspects the other's or makes one concrete improvement to it, and what the exchange adds is observed as things landed (bugs caught, abstractions clarified, tests, repairs) and as how well each preserves the other's sound decisions, never as opinions, since each family rates its own higher. The roles for the rounds after are assigned from both stages: one primary builder with the other checking named concerns, ownership alternating by task, or one model doing most of the work. Two parallel builders is the one departure from committing on `main`, said here.

**4. An implementation round.** The same fence, essence and terrain, one starter per lane, with Sid's process ruling carried in the essence (the prototype is the spec; tests built as you build; documentation in the hierarchy with the code; the best team in the world; functional; the data layers, the data flow and reactive programming worked out early). No contract, no method. `path-kind/STARTER-client.md` is the first pair.

What every prompt leaves out, because each has fired: an output shape, a method or reading order, epistemic coaching, a register block, enumerated freedoms, restated house law, example values, steering verbs inside the terrain. When Sid is present in the session, the prompt needs even less; his live redirection does the rest.

## What is preliminary here

This is one round's shape, written by the composer. The 3D round runs the same two chairs and its feedback file is not yet read against this. Phase B has run zero times in its smaller shape; the cost claim (an attack at half a session) is a prediction. The cold-mind attack has not run. The path round closed the same day this was written: Sid ruled Positions 7, 8 and 9 (the page's ledger), the definer drew the line in `path-kind/from-12-to-client.md`, and the implementation rounds start from the two starters in `path-kind/STARTER-client.md`, without contracts, as the paragraph above says. A stance from those rulings belongs in every future round: the best in the field is a datapoint, never the thing to copy, and nothing existing is carried forward because it exists.
