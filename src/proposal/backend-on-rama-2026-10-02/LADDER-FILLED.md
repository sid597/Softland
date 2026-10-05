# The ladder, filled in

Drawn in chat on 3 October 2026. Written here on 5 October, on Sid's word.
Read `PROGRESS.md` first. This page is the detail under its ladder.

Nothing here is a ruling. It holds three things:

1. who writes each row of the ladder;
2. what each source would be on the ladder;
3. each row with what it holds, and a mark for where each line comes from.

Parts 1 and 2 are one session's reading. Part 3 marks each line separately.

## 1. Who writes each row

| Row | Code or data | Who writes it | Where that comes from |
|---|---|---|---|
| Data sources | Neither | Nobody. They exist | Sid's list of 3 October |
| Inlets | Code | Us | Sid's word. That this code moves a thing and does not say what it means is the proposal, `frame-2026-09-15/PARTS-4-8.md:272` |
| Adapters | Data | Whoever brings the source | Sid: "an adapter that the user would define". That it is data is his position of 2 October, not ruled |
| Converters | Data | Whoever makes the tool. Sid writes the first ones | Sid's sentence and his position of 2 October, not ruled. Found by matching is the proposal |
| Act | Code checks the form | Us | Ruled: the envelope belongs to the store |
| Depots | Code | Us | Two gates are ruled. One depot for each gate is the 2 October drawing, derived |

Each row has one author. The test, from one of the sessions that judged the four
replies: if one author cannot write all of a box, it is two boxes.

Not drawn: the code that runs an adapter or a converter, and the small steps
their bodies are written in. Part 3 has them under RUN BY.

## 2. What each source would be on the ladder

One row shape. This is what each row would be under the ladder. It is not what
is built.

| Source | Inlet | Adapter | Converter | Acts |
|---|---|---|---|---|
| A hand on a screen | Inside. Input and the hit test | None | The pointer's rules say what the signals mean, then a tool gives the act | A selection, an edit |
| A fact that lands | Inside. A bell, then a read | None | The tools that wait for that kind of fact | Further facts |
| The host's files | Outside. Files and git | The shape of each kind of file: forms, sections, lines | Each form becomes facts: its name, text and place | One fact for each form |
| A mind's reply | Outside. A process or a socket | The shape of a reply: text, tool calls | Each turn becomes an act on its thread | One act for each turn |
| The host's work | Outside. A process, the clock | The shape of an outcome: result, revision | "Check passed at this revision" | The outcome as a fact |
| Google sign-in | Outside. Bytes and a signature check | The shape of a token: person, issuer, expiry | A person the first time, a session each time | The session and its permissions |
| A new source | Outside. No new code if an inlet reaches it | Written by whoever brings it | Theirs, or a canonical one | Theirs |

What is built today differs in four places:

- The pointer's rules are already records: `resources/inland/seed.edn:14`.
- A click comes from the browser's own event: `src-inland/softland/inland/render.cljs:200`.
  So the browser decides what a click is, and drag is not built.
- The files row is code: the rig's `:material/reading` step,
  `src/proposal/rig-2026-09-25/src/rig/store/recipe.clj:138`. The server's
  adapters are code too.
- Sign-in is a password login in code: `src-inland/softland/inland/people.clj:114`.

Not drawn: other instances. Sid's ruling of 18 September says there is one store
(`frame-2026-09-15/LEDGER.md:201`). The screen sits on the inside inlet by Sid's
words of 2 October. The proposal's part six sends a pointer's return through the
outside door (`PARTS-4-8.md:279`). Nobody decided between them.

## 3. Each row, with a mark on every line

The rows are the ladder's. RUN BY is beside the ladder and is not one of Sid's
five. Below ACT the picture goes on with THE MODULE band of
`ARCHITECTURE-STE100.md` section 2, unchanged. It is not copied here, so that
there is one copy of it.

```
 marks at the right:  you = Sid's words · you* = Sid's 2 October position, not ruled · ruled · prop = Sid's proposal · file = the 2 October drawing and its rechecks ·
                      built = the rig or Inland · mine = a session's position, not ruled · open
 ─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
 DATA SOURCES           outside         not Softland's · what it gives has no id, no address in the store, and no basis                                              prop
 nobody writes them     inside          Softland's own · a fact that landed                                                                                          prop
                                        or a signal on Softland's own screen · it is never a fact before a rule tells what it is                                     you · prop
                              │
                              ▼ a thing
 INLETS                 outside inlet   the primitives: bytes (files, sockets) · processes on the host · the clock                                                   you*
 CODE · us                              small and general · a primitive moves a thing, and it does not know what the thing is                                        you* · prop
 a row of its own: mine                 each primitive runs under a grant, and it records what it read                                                               you*
                                        it moves a thing in only for an ask: an act that a gate accepted                                                             mine
                                        what it records: the kind, the bytes or their hash, the moment · no adapter or converter can make this record                mine
                                        what arrived is handed on as a value, or recorded as a fact first                                                            open
                                        the store keeps the bytes, or only their hash                                                                                open
                        inside inlet    a bell rings when a layer changed · a ring is not a read: the runtime reads again through resolve                            file · built
                                        the screen: input and the hit-test · the hit-test gives the address that was painted                                         prop
                                        ?2 a feed, if match runs in the module                                                                                       file
                              │
                              ▼ what arrived (outside) · what was noticed (inside)
 ADAPTERS                               for an outside thing only · an inside thing has a shape already                                                              mine: a guess
 DATA · whoever brings                  gives what arrived a shape                                                                                                   mine: a guess
 the source (you*)                      a shape is a grammar, and a grammar is a fact                                                                                prop
                                        ?8 a grammar for the bytes of a format, which one leaf reads                                                                 file
                        inward return   makes the identity, the address and the basis of an outside thing · a match by name only is not exact, and the basis says so prop
                                        an adapter makes these, or a converter does: where an adapter ends                                                           open
                              │
                              ▼ a thing with a shape
 CONVERTERS                             takes a shape, runs a function, gives an act or a series of acts                                                             you
 DATA · whoever makes                   a pattern and a body · found by matching what came in against patterns, which are facts                                      prop
 the tool · you write                   the function reads the store only through resolve · what it read becomes the basis of each act                               prop · ruled
 the first (you*)                       each act goes into one layer · a series is named in full before its first act is sent                                        ruled
                                        ① what the act says · ② which layer it goes into: the two things a converter gives                                           you, agreed
                        base functions  a body with a name, which other bodies call · you write the first ones                                                       you*
                                        the canonical ones are in the base · a changed copy in a nearer layer wins · there is one store                              mine

 RUN BY                 match           finds the adapters and converters whose patterns match · ?2 where it runs                                                    prop · file
 CODE · us              the evaluator   runs a body, under a budget                                                                                                  you* · settled
 not one of your five                   records each read: the reads become the basis · finds each name again at each use, through the layers of the actor           ruled · prop
                                        a budget that stops a body while it runs is not built: Inland examines sizes after the run (total.cljc:6)                    built
                        leaves          the small steps that a body is written in · a new leaf is a rebuild · the rate of new leaves is the gauge                    prop
                                        no leaf reads a clock, a random number, or a thing outside that can change                                                   prop
                                        formulas · a parse by a grammar · a hash · the check of a signature                                                          mine
                                        a body never touches the outside · to get or to do a thing outside, it gives an act that asks                                mine
                              │
                              ▼ an act, or a series of acts
 ACT                    the door        for each act: makes the name, puts on the envelope, reads the class ③ of the layer again,                                    file: ruled, built
 CODE checks the form                   appends to the acts depot of that class
 · us                   the act         name · layer · who · permission · based-on · subjects · facts                                                                ruled
                                        session · because-of · claimed-when · expected · version: ?7 where they sit                                                  ruled · open
                        every read      queries resolve · appends a read entry (an act in the hand layer of the reader) ·                                            file: ruled, built
                                        shows the result only after that act is accepted
                        an ask          an enactment has an act that asks and an act that tells the outcome                                                          prop
                                        the same for a prompt, a send and a fetch · a gate decides the ask like each act · then the outside inlet does it,           mine
                                        and what comes back arrives at the outside inlet
                                        paint is not an ask: its act is the read entry                                                                               ruled
                       │ append                          │ query                    ▲ proxy
                       │ (acts, read entries)            │ (resolve)                │ (bells)
 ─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
 THE MODULE     as in ARCHITECTURE-STE100.md section 2, unchanged: the acts depots, the gates, the PStates, the bell PStates, a feed, resolve                         file
                one line is added under resolve:
                an accepted act is now a fact · a bell rings · the inside inlet notices it: the top of the picture again                                             prop · file
```

Eight lines are marked mine and four are marked open. Every other line names
where it comes from. "You" in the marks is Sid.

What this changes against the runtime band of `ARCHITECTURE-STE100.md`. That
file is not changed:

- Its row "act converters" is two rows here, ADAPTERS and CONVERTERS. Its phrase
  "the clock and random numbers: for outside tools only" is not here. The line
  "a body never touches the outside" stands in its place, marked mine.
- Its row "sign-in" is not here. Sign-in is a row of part 2. Who the person is
  in the store stays open as ?4.
- Its row "crossings out" is the lines under "an ask".
- Its rows "inward return" and "bells" are under ADAPTERS and the inside inlet.

Not drawn: the instances (part 2 and `mapping.json`), the session as what sits
above every converter, grants and policy, the body language, and the maintain
step.
