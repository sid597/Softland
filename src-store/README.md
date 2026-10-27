# The store — source entry

This tree holds the code of the store that is built to stay. The implementation
lives beneath [softland/](softland/README.md). It holds the act: the one form of
what a maker gives to the store, and the check of that form. And it holds the
store's Rama module: the two depots of acts, the streaming gate and the
microbatch gate, the one walk by which both decide a thing, the record that each
gate keeps, and the reads by act-id. The module has those two depots and no
other. And it holds the door: the client code that takes a maker's act into a
depot, with the time written on it, and reads its answer back. And it holds the
read path: the client code by which a fact comes back out to a caller, once or
kept live.
The pure code owns no state. The module's state, the two depots and the records,
is durable in the cluster that runs it. The door owns no state: it borrows four
handles of Rama's client and holds one number, fixed at its opening. The read
path's state is the memory of one process: who looks at what, and the pushes
that they share.

The store comes out the same when its gates are run again over its two depots:
a gate reads no clock and no table of the other gate, and the acts of a layer
are one line in one depot partition. The
[decisions of 13 October 2026](#decisions-of-13-october-2026--the-store-comes-out-the-same-when-it-is-run-again)
say why, and the [verification map](../test-store/README.md) names the proof.

The rig (`src/proposal/rig-2026-09-25/`) is an example build, not a source: no
namespace of this tree requires it.

The store as a model, what it keeps, what can be asked of it, what it promises
and what cannot be got back from it, is in [MODEL.md](MODEL.md). Read it before
the parts.

Start with the question you are carrying:

| Question | Enter here |
|---|---|
| What does the store keep, what can I ask of it, and what does it promise? | [MODEL.md](MODEL.md) |
| What can I read back of an act, and what can I not get back? | [MODEL.md](MODEL.md), "What cannot be got back, said plainly" |
| Which of the store's answers can nobody forge? | [MODEL.md](MODEL.md), "What nobody can forge" |
| What is in this tree, and what state does each part own? | [softland/](softland/README.md), then [store/](softland/store/README.md) |
| What is an act, and why is a thing refused as one? | The namespace docstring of [act.clj](softland/store/act.clj), then its two declarations, the shape and the rules |
| What happens to a thing that arrives on the streaming depot, and why is it refused? | [store/](softland/store/README.md), "How the parts relate"; then the namespace docstring of [streaming_gate.clj](softland/store/module/streaming_gate.clj) |
| What happens to a thing that arrives on the microbatch depot, and when is its answer readable? | [store/](softland/store/README.md), "How the parts relate"; then the namespace docstrings of [microbatch_gate.clj](softland/store/module/microbatch_gate.clj) and of the walk that both gates call, [walk.clj](softland/store/module/walk.clj) |
| How does a maker's act get into the store, and how is its answer read? | The namespace docstring of [door.clj](softland/store/door.clj), then [door/](softland/store/door/README.md) for its choices and its words |
| How does a fact come back out to a tool, once or kept live, and what is it told of a layer that it may not see? | The namespace docstring of [read_path.clj](softland/store/read_path.clj), then [read_path/](softland/store/read_path/README.md) for its choices and its words |
| How are the tables made again, and how do I know that a second run gives the same? | A second module that mirrors the two depots and calls `add-gates-and-reads` of [module.clj](softland/store/module.clj) runs the store's own gates over them from their beginning. The [verification map](../test-store/README.md) names the proof that does it and compares every table |
| What does the gate not check? Which choices are the builder's, and which words did a session make? | [store/](softland/store/README.md): "What the gate does not check", "Engineering choices", "Words a session made" |
| What did Sid decide about the form of an act? | [Decisions of 6 October 2026](#decisions-of-6-october-2026), below |
| What is a stamp, and where is the time on an act? | The namespace docstring of [stamp.clj](softland/store/gate/stamp.clj); [Decisions of 14 October 2026](#decisions-of-14-october-2026--order-and-time-apart), below |
| Where can I challenge a behavior with an existing check? | [Verification map](../test-store/README.md) |
| What does the store rest on in Rama and not know? | [Open questions about Rama](OPEN-QUESTIONS-rama.md) |

"Act" is the word in this tree. The rig's code says "offer" for the same thing.

This tree has its own classpath, in two aliases of the root `deps.edn`. Both hold
`src-store` and `test-store` as `:replace-paths`. So the root `src` and
`resources`, the rig and the root's other dependencies are on neither, and a
require of any of them from here fails at load.

- `:store` holds Clojure alone as `:replace-deps`. The pure code and its three
  proofs run on it, the read path's five pure namespaces with their proof among
  them. It is the guard that the pure code
  needs nothing else: a pure namespace that came to require Rama would fail to
  load here.
- `:store-module` holds Clojure and Rama (`com.rpl/rama` 1.6.0), and the same JVM
  options. The module and its proof run on it, and so do the door and its proof,
  the read path with its proof and its measures, the proof that runs the gates
  again over the depots, and the measures of the gates. It is a whole alias and not an
  addition to `:store`: with Rama in `:store`, the guard above would be gone.
  Rama's helper library is left out; the module uses none of it.

Follow folder map → namespace docstring → function docstring, and read only the
branch that the question needs. A folder map explains its immediate children.
What a function takes, gives and does not handle is in its docstring. The
[rules for explanations](CLAUDE.md) of this tree govern this hierarchy: update a
function's docstring and the affected parent map in the same change as the code.
A source description is not a run; what a check observed belongs to the
verification tree.

## Decisions of 6 October 2026

Sid made these decisions on 6 October 2026, on a printed sheet. Where an entry
says "Sid decided", the answer is his tick. Three boxes were blank (6b, 6c and
11 C/D). For 6b and 11C a session proposed an answer and Sid agreed in chat the
same day; those entries say so. 6c is open only if he strikes 6b.
Some values are kept from today's code and were not decided; those entries say
so too. In Sid's words, these are high level, and he needs them here at least
until everything is built out. The words that they use are in the namespace
docstring of [act.clj](softland/store/act.clj).

Where each answer is in the record. He typed 1 A, 2 A, 3 A, 7 A, "8b-yes",
10 A and 11 A in one line (‹d4fd70ba›). The same line holds his question on 9,
"I think i need more explanation around 4,5,6", "for 12 a vs b???", and where
these go: "write in src-store/readme.md because these are high level and need
them there atlest we build everything out". His ticks are on three photographs
of the sheet, on his next line (‹61dbaec7›). The record keeps no image. A
session read the photographs and wrote down a mark for each question
(«3d4ee664»); for 4, 5, 6a and 12 that reading is all that the record holds.

1. **The act is the one form and the check of it; nothing else.** Sid decided.
   `act.clj` makes no act-id, and has no stand-in sealed form and no digest.
2. **The one form is the maker's act.** Sid decided. It is what a converter
   gives, before the door touches it. The door's additions (the act-id, the
   class, the seal) are the door's own small declaration, which contains the
   maker's; a gate checks what arrives against it. Since then the class is
   called the depot (9 October), the door also writes the time, and the seal
   was set aside on 12 October (entry 2 of that day): the door adds the
   act-id, the depot and the time.
3. **The seal is below the form.** Sid decided. The act holds plain values. The
   door seals, and a gate refuses an unsealed value by its own rule. So the act
   has no sealed bytes, no lock id, no store keys, no placed keys, no mark, and
   one kind of fact. Its half on sealing was set aside on 12 October (entry 2
   of that day): the door seals nothing, and the store keeps no locks.
4. **Parts: `:version` and `:session` required; `:because-of` and
   `:claimed-when` optional.** Sid decided. The version is the edition of this
   list of parts, and the code knows 1. `:because-of` is an act-id.
   `:claimed-when` is an integer, the maker's own clock, a claim. Expected
   versions are not a part: `:based-on` and `:replaces` carry them.
5. **The class and the act-id are not parts of the act.** Sid decided. The
   door reads the layer and adds the class; an act that the store places
   itself always carries the class. The door makes act-ids. An act-id is
   `[layer class uuid]`: three slots, no scheme word, the last a random UUID.
   Sid asked on 7 October why it was called a name when "act-uuid" would be
   understood (‹9b8f461f›); it holds more than the UUID, so a session wrote
   act-id — his to change. The act still refers to
   act-ids, so the shape of an act-id stays declared. Derived from two of his
   rulings (the door makes the act-id; a series gets all its act-ids before its
   first act is sent), not his tick: act-ids are given on request, ahead of
   sending. The two class words are `:streaming` and `:microbatch`. Sid
   decided: they are his words for the two kinds of depot, "my streaming and
   microbatch" (‹12c9bd22›). What a later change of the two words would cost,
   read and not run: the word is in every act-id, and an act-id is in the
   log, with its own act and in each act that names it. Rama can write the
   records of a log again at a module update
   (`reference/rama/docs/14-depots.md`, line 288). No key of a table holds
   the word (`answer-key` in `softland/store/record/rows.clj`).
6. **A fact is the named slots `{:e :k :v :replaces}`.** Sid decided:
   `:replaces` is optional, a fact id; `:v` is optional; a fact has no mark.
   Proposed by a session for a blank box (6b) and agreed by Sid in chat on
   6 October: a fact has no layer of its own. His words to the session's
   picks: "I think we agree on this" (‹61dbaec7›). When a session then wrote
   the box down as pending, he asked the same day "how did you come to the
   conclusion that this is pending?" (‹1aa6cc3b›). Also blank (6c): where a fact that
   names another layer is refused, on the face of the act or in the gate's
   decision; moot while 6b stands. Kept from today's code, not decided:
   a fact with no value is a retract.
7. **The rig is an example.** Sid decided. Its findings are evidence. No line of
   the form, no name and no call shape comes from its code.
8. **A permission is structured, and required.** Sid decided. It is
   `[who layer granted-in]`, or `[who layer granted-in parent]` where the parent
   is a permission. An act without one is refused `:no-permission`. One rule
   stays in the form: the permission's layer is the act's layer, else
   `:permission-not-in-layer`. Whether the permission stands is the gate's.
   Kept from today's code, not decided: a permission is 4 levels deep at most.
   A fact, checked on 12 October: the rig's code holds the same limit of four,
   and counts it as "root, session, agent, tool"
   (`src/proposal/rig-2026-09-25/src/rig/store/permit.clj`, line 22). On
   12 October he said yes to lifting it (entry 6 of that day). That build is
   done: a permission is now as deep as a value can be
   (`softland/store/act.clj`, `nesting`), and how deep stays his to say.
9. **An id is a readable keyword or a UUID; a key is a readable keyword only.**
   Sid decided the half on an id. The ids are an entity, who, a layer, a
   session, a subject and the layer slot of an act-id. A key is a fact's `:k`.
   The half on a key is a session's (found on 9 October). The question put to
   him was what an id is, and his line was "how can it be a keyword????? we
   can have10^x no of ids where will we find the keywords for them???"
   (‹d4fd70ba›). The session that answered wrote "Keywords were the rig's
   choice" and proposed the split itself, "a key is a keyword" («a78296bc»);
   no line of his takes up that half. On the photographed sheet a session
   read his tick at A, and beside it a note in his hand, "Keyword + uuid ???"
   («3d4ee664»).
   The rulings file of September says, in a session's words, "Keys are ids;
   word and grammar are facts about them", and lists "Keys as ids: confirmed"
   among the nine of 24 September. On that day he picked "5 (keys as ids):
   confirm" on a form of ten questions that a session built; it was the
   option that the form marked as the session's own lean (the rulings file,
   its last section). He has not ruled between the two. Until he
   does, the code holds a key to a readable keyword (the queue, K5).
10. **The microbatch hash entity is no part.** Sid decided. The depot derives
    it. On 13 October he decided that every act is placed by its layer key,
    the base too (entry 3 of that day): the microbatch depot derives no entity,
    and it places a thing by its `:layer` as the streaming depot does.
11. **`:based-on` is a map from a fact id to a carried stamp.** Sid decided.
    Proposed by a session for a blank box (11C) and agreed by Sid in chat on
    6 October: an outside thing is recorded as a fact first, so its basis is
    that fact. It rests on the same two lines of his as 6b (entry 6). Kept
    from today's code, not decided: the part is optional, and empty when absent.
12. **A carried stamp is an integer.** Sid decided. Kept from today's code, not
    decided: a carried stamp is below 2^62, an act names 256 subjects at most,
    and a value is inside 32 collections at most. A fact, checked on
    12 October: the rig's code holds the same three numbers
    (`src/proposal/rig-2026-09-25/src/rig/store/envelope.clj`, lines 74 to
    87). He asked on 6 October "is 2^62 big enough" (‹61dbaec7›); the record
    holds no answer to it. Closed by a session on
    6 October, waiting for his word: a carried stamp is not below zero, because
    no gate gives one. On 14 October the bound of 2^62 went: no gate reads a
    carried stamp, so its form alone holds it (entry 2 of that day).

## Decisions of 7 October 2026 — the store's module

Sid answered these in chat on 7 October 2026, one by one, on the sheet "The
store — the questions the gates need". The record holds his answers as three
lines of that morning: ‹27882354›, ‹32f65085› and ‹9b8f461f›. Words in
quotation marks are his, as he typed them. A reason that a session gave him
before he answered is marked as the session's. They shape the module built
next: the
depots, the gates and the record. As each part is built, its folder's README
carries the decision that shapes it and this section points there. What is not
yet decided is queued in
`src/proposal/backend-on-rama-2026-10-02/decisions/OPEN-QUESTIONS.md`; answers
never live there.

1. **Two depots, one for each gate: the streaming depot and the microbatch
   depot.** Sid decided (F6 A, Q7 a; ‹27882354›). The builds of 8 to 13
   October added three more: a depot of the clocks of batches, and a depot of
   answers for each gate. On 13 October he took the three out (entry 5 of that
   day). The module has these two.
2. **The record is in the gates' PStates.** Sid decided (F2 A). His line
   (‹27882354›):
   "f2 - A, note to self- it is recomputable given the version a act was
   proposed at it will be rejected every time so storing it a new
   depotdoesn't make sense". The build found that a second run of a gate
   over the acts depots did not give the same record. On 12 October he
   decided that what a gate decided also goes into a log (entry 1 of that
   day). On 13 October he took that log out again and had the store built so
   that a second run does give the same record (the aim and entry 5 of that
   day), with the version of the act as what a rule belongs to (entry 6). So
   his note of 7 October holds as he wrote it. The gates' PStates stay the
   record that the door and every read use.
3. **The members of a layer set its class, as a fact on the layer.** Sid
   decided (F1 A; ‹27882354›). The "third switch" of the 2 October drawing was never his
   agreement; this is.
4. **The base is a microbatch layer; every other layer starts streaming.** Sid
   decided (F4 C). Of the other layers: "members choose yes, default is
   streaming yes" (‹27882354›). Of the base: "Base:: Microbatch I think ...
   why should it be streaming??? instant vs a few ms", and his heuristic, in
   his words (‹32f65085›):
   "i think one heuristic is when I am doing personal work somethign I am
   engaging with directly and i know it is supposed to be fast like editor,
   canvas, 3d roaming around, live collaboration .. people expect it to be
   interactive .. if they are subscribed to something some other layer the data
   is coming from somewhere else that is not marked as live collaboration is
   fine to come a little bit delayed wdyt??"
   A session's, from that: the base, which everyone subscribes to, is the
   extreme of the second kind. His "a few ms" is not the build's figure; see
   entry 8 of 11 and 12 October, "The base's second is fine". What he means
   by the base is said on 13 October, under "What the base is"; that its acts
   are one line is entry 3 of that day.
5. **Each layer holds a copy of each grammar it uses; a gate reads on its own
   task.** Sid decided (F5 A). He asked "what is the cost of holding a copy vs
   fetching it from another task??" (‹27882354›), and met a session's answer
   with "copy .. i agree with your logic here" (‹32f65085›). The logic is the
   session's: a copy is bytes and a rare write; a fetch is a cross-task hop on
   every act. On 13 October he took the same side for the base's rows: a
   copy on every task (entry 4 of that day).
6. **The door reads a streaming act's answer from the record by act-id; the
   ack is a signal.** Sid decided (Q22 A): "so yeah A it is", after "ahh i
   understand why", where he says a session's reason back to it, "Because the
   ack can fail to carry it" (‹9b8f461f›). The session's reason in full: an
   ack can fail to carry the answer — an error for an act the gate accepted —
   and a microbatch act never has one.
7. **A rule for all code: what leaves a decision — an ack, a bell, a
   promotion's second act — is a signal; the truth is the record; read it by
   act-id.** Sid decided (F7 A), by the same line as Q22. He had asked of the
   two as one, "q22 & f7:::" (‹32f65085›); a session answered the two under
   one heading and gave its letters, "Q22 A, F7 A" («a02f36a4»); his "so yeah
   A it is" (‹9b8f461f›) is one answer to that. A gate reads its own record by
   act-id before it decides, so a repeat is answered, not decided twice.
8. **The gate refuses a setting key on an entity that is not the act's layer,
   as a recorded decision.** Sid decided (F8 C): "f8 C ok" (‹9b8f461f›), after
   "f8::: wdyt???" (‹27882354›) and "f8 :: still don't understand"
   (‹32f65085›). The reason is a session's, which it gave him before he
   answered («a02f36a4»): the door may warn early; the gate is the authority,
   because anything can append to a depot without the door, and the one form
   knows no store keys (decision 3 of 6 October). What can reach a depot is
   his decision of 12 October (entry 4 of "reading back out").
9. **The three limits are targets to optimise toward; the rig's measurements
   are the starting point, not a ceiling.** Sid decided (F9 b, c): "The limits
   are what we should be optimising for what we measured is starting point
   maybe a better optimised sytem will perform better", and "idk lets go by
   whats measured and we will know better when we build more and optimise"
   (‹27882354›). Two nodes on one machine is a question for production (F9 a):
   "it is fine to punt to later when i go to production" (‹32f65085›).
10. **The act's id is called act-id.** Sid asked why it was called a name when
    "act-uuid" would be understood (‹9b8f461f›); it holds more than the UUID —
    `[layer class uuid]`, the layer and the class route the act and find its
    answer — so a session wrote act-id; the word is his to change. **The word
    "class"** (streaming or microbatch) is borrowed and not accepted; it stays
    until Sid gives one. He gave it on 9 October: "depot" (entry 4 of that
    day). The code says "depot" since the rename of that day.

## Decisions of 8 October 2026 — who acts, and what the store reads

Sid answered these in chat on 8 October 2026, against a list of eight that the
build session put to him: the two questions of its own sheet (K1, K2) and six
rows of the sheet "The record, and who acts". Words in quotation marks are his,
of that day unless a date is given. The record holds his answers as two lines,
‹f3f530b8› and ‹751c2af7›; an earlier line, ‹d05a12e6›, holds the first five
of the eight. Their day in the record is 7 October, by its clock; by his it
was 8 October. Where a session read his answer onto a row, the entry says so.
He struck none of those readings that day; for one of them his own words on
the row were that he did not understand the question (entry 3). They shape
the gates' check of a permission, the making of a layer, the stamp and the
seal.

1. **The system stands at the top of the base's chain.** Sid decided (E1·H8
   A): "system" (‹f3f530b8›). His line of 6 October (‹ddf13d17›) names the
   three that act: a user, a
   group, or "the system basically software"; "if it is a system level thing
   like we as the architect of it are trying to do then it comes under that
   part".
2. **A signed-in user has a personal layer and a permission in it, without
   asking.** Sid decided (K2): "the user already is logged in as a user so
   they automatically have their personal layer and permissions" (‹f3f530b8›).
   A user who is not signed in is "just roaming around on what is open by the
   system or world group". He agreed, with "2. ok" (‹751c2af7›), to this form
   of it, which a session wrote: the system makes the layer and
   the permission at the first sign-in, and the person who creates a group
   makes the group's layer. The builder's part, not his: each layer is made by
   one act, its first, so that the making of a layer is in the acts depot
   (decision 2 of 7 October). When the system makes a personal layer, that
   act's who is the system and the permission that it names is the person's;
   his decision does not say under which permission the system acts there.
   Sign-in is not built.
3. **Permission is a hierarchy that ties back to one person.** Sid decided: a
   permission "ties back to a single individual", however deep the groups and
   agents go; to walk back along it comes down to who created the group,
   "invited who, who gave the permission to do xyz in their group". An act in
   a personal layer comes under the person's own permission (‹f3f530b8›). A
   session read three rows from this: each actor acts under
   its own permission (E4 A); whoever holds a permission can give one beneath
   it (E2 A; Sid: "I answered this in 4 imo"); the top of a chain is a person,
   or the system for the base, and never a session (E3·H7 B). The third is a
   session's reading and not his answer. His words on that row were "what and
   why is this question??? no me understand". A session replied that his
   fourth answer settles it («c738ecc3»), and his next line answered rows 2
   and 8 only (‹751c2af7›). The rulings file of September has, in a session's
   words, "The session is the root". The row waits for sign-in (the sheet
   "Before real data is kept", last page). On 9 October he
   decided that the system can hold many layers (entry 3 of that day), so
   "for the base" in this reading is no limit.
4. **A stamp holds the clock time and a counter.** Sid decided (H2 A):
   "clock-time+counter" (‹f3f530b8›). Whose clock, and what the counter
   counts, are his decisions of 13 October: the clock of the server that runs
   the door, written on the act before the depot, and a number counted per
   layer (entries 1 and 8 of that day). No gate reads a clock. Replaced on 14
   October: order and time are apart, and the stamp holds no time (entry 1 of
   that day).
5. **The store's keys in this build are three: the class, a permission, and
   the door's request for locks.** A session proposed the list (K1 A). Sid:
   "idk what else is missing or could be missing, so lets go" (‹751c2af7›). No
   layer names an owner as a fact. The words are his to change: "class" is borrowed
   (decision 10 of 7 October), and the request for locks is written
   `:lock-request`, a session's plain word, because "lease" is the rig's.
   Since then the class is the depot (9 October), the request for locks left
   the store's keys on 12 October (entry 2 of that day), and the read build
   added three settings (11 and 12 October, entries 1, 2 and 6): the store's
   keys are five (`softland/store/door/declaration.clj`, `store-keys`).

## Decisions of 9 October 2026 — the door's four questions

Sid answered these in chat on 9 October 2026, on the sheet "The door — four
questions before it is built": "4B and all else A" (‹15af6243›). Words in
quotation marks are his, of that day unless a date is given; some are
dictated, and are given as the dictation has them. The record holds his lines
of that morning as ‹941a29a5›, ‹85a64cfb›, ‹5ea945a8›, ‹a3dfb1b8›, ‹6d5e8c3c›
and ‹15af6243›, and two later ones as ‹ccc88e16› and ‹1a5ec25a›. It holds
‹6d5e8c3c› as a long prompt, where it cannot tell his typing from a paste.
What a session drew from an answer is marked as a session's. No code changes with them: for three the gates already do what he
chose, and the fourth waits for two words.

1. **A person's id says nothing: it is a UUID. Her sign-in accounts are joined
   to it by links that the store can erase.** Sid decided (?4 A). He asked
   first: "a person can sign in with multiple accounta so how will that work",
   as when "i want to replace my google account with a new one or disable my
   old google and use githib"; "if we go with uuid that would still be
   possible????" It is. The id is the person's and not an account's, so an
   account is added, replaced or removed by its link, and no act changes.
   A session's, from that: there is a link for each account, where the sheet
   said "one link"; the id is the store's own random id, never the number
   that Google gives an account; what a person is called is a fact about her,
   and not the link; and nothing that is written plain in an act is made from
   the person, her own layer's id and a session's id too, or the record names
   her there. Acts about her carry the same id in `:subjects`, so forgetting
   a person meets this at H11. No code makes a person yet. Open, for sign-in:
   the queue, K7 to K10.
2. **One lock seals one value.** Sid decided (H1 A): "I think my stance is A.
   Because given A, any tool user layer group can decide by themselves what
   uh, a log delete means for different kind of values for their tool or
   layer" (‹5ea945a8›, dictated; "log" is the lock). His frame: "this is only about what
   gets deleted", and "how much uh, control do we have over what gets
   deleted". The gates already make a lock for each value, and refuse a second
   value under a used lock. A session's, from that:
   - A lock does one thing: it is destroyed when a value must be forgotten.
     Who may read is not its work.
   - Each write of a value is a new value, so this is a lock for each version.
   - H1 is three things. The floor is the least that a lock can seal, one
     value: it is code, and each written value keeps the lock that sealed it.
     The default is this answer; it can change for what is written after. The
     switch that moves a layer to one lock for each act (ruling 7 of the
     rulings file) is not built; it is built when a bulk layer asks for it.
     Values that share a lock can still be forgotten one by one: the others
     are written again under new locks and the shared lock is destroyed, and
     the others then lose their earlier versions.
   - His two cases, which the choice turned on (‹5ea945a8›). Against a lock for a sitting: "if it's
     like everything was written in one session, and then we want to forget something, but keep like
     ninety nine percent of it, that that might not be possible because uh, we have a single log for
     everything that was done". And the worry he raised against the fine grain: "if we delete a
     block. We might also want to delete uh, some thing more related to it. For example, some context
     around it that was added. Some relation link, etc., etc. And we might forget that. Uh, even
     though the intent would be to delete." A
     session's, from that: what a forget takes with it is chosen when forgetting, not when writing;
     the things around a forgotten value were written in other acts, sittings and layers, so a lock
     for each act or for each sitting would not have taken them either. The store owes the one who
     forgets the means to find them (the queue, K11; W8 of the reads sheet
     names "a forget" as an asker).
   - Set aside on 12 October (entry 2 of that day): the store keeps no locks,
     and no gate makes one. This entry says how a sealed end would be; the
     store has none.
3. **The system can hold many layers.** Sid decided (K3 A): "my preference is
   a uh, system can have uh, many layers" (‹6d5e8c3c›; and "all else A",
   ‹15af6243›). His picture: "what I will be
   providing in Softland is like code and then like a few layers that are
   built on top of it, kind of libraries", the "converters or importers,
   ingesters", "the default things that I will be making"; "any other person
   can make their own"; "other people can use either theirs or the system
   ones, or maybe it turns out like the other people's becomes the system one
   on some point". As with Linux, "we can like uh, basically draw a line of
   what we mean by what is being shipped by Linux and what is not being
   shipped by it". This replaces the reading "or the system for the base" in
   entry 3 of 8 October, which was a session's. A gate accepts such a layer
   today. A session's, from that: a layer is the system's when the system
   holds its first permission; the layer's first act writes that, so the line
   can be read from the record. A session's reason, beside his: the base is a
   microbatch layer and every other layer starts streaming (entry 4 of
   7 October), so with the base alone the system could hold nothing that
   answers one act at once. Open: how layers build on layers (W2 of the reads
   sheet), how a person's tool becomes the system's (K13), who may act as the
   system (K9), and whether the "world group" of his line of 8 October is the
   base (K14). His line of 2 October: "world is the open group that every
   group if make open their data publishes to".
4. **The store's own keys carry a namespace; they are not bare words.** Sid
   decided (K4 B). He asked what it is for, from the side of those who build
   on Softland: "if someone is building like 50 layers of uh, different tools
   on top of each other then they would have to answer it each time right",
   and "what are we trying to do here" (‹a3dfb1b8›). Between those two the
   same line has "or like you are saying the that uh, I have to decide it
   right now because then we would be exporting the thinking process and
   going more of the same question every time for like literally say like
   billions of times". That is a session's argument, which he says back to it
   inside his question. It is not a reason of his, and the record holds none
   of his for B: his answer is "4B and all else A". A session's answer, which
   he heard before he decided: B asks a builder nothing. It gives the bare
   words back
   to the tools; it keeps a tool's own fact from being read as the store's;
   and it leaves the store room for more keys, where a bare word taken later
   would change how old acts are read when the record is built again from the
   log. He gave the two words the same day: "softland.store" before the
   slash, and "yes to depot" for "class" (K6). They are the last two lines of
   ‹ccc88e16›, under a paste of a session's text; the record holds the whole
   line as a paste, and those two lines are in no session's text. Asked on
   13 October whether he typed them: "oh ok ok yes i did approve that my
   wording s". So the store's keys are
   `:softland.store/depot`, `:softland.store/permission` and
   `:softland.store/lock-request`, and a layer's depot is `:streaming` or
   `:microbatch` (the request for locks left the store's keys on 12 October,
   entry 2 of that day). Two sessions had put options to him; one preferred `store`
   and `gate`, the other `softland.store` and `depot`. A session's reasons for
   the pair that he chose: the store owns its whole namespace, so the word
   must be one that no tool would reach for; and "depot" says what the slot
   holds, in a word that the decisions already have (entry 5 of 6 October
   records the two words as "his words for the two kinds of depot"). On
   7 October he had met "gate" with "we are also using the gate order as of
   now would it not conflict with that" (‹27882354›), and "pace" with
   "Naming::: bro you total shit at it .." (‹32f65085›). The rename was made
   the same day. The code says
   "depot", the three keys of facts carry the namespace, and the part of a
   thing as it arrives is `:depot`. The act's own part `:permission` keeps
   its word: only the key of a fact takes the namespace. Asked "Should a gate
   refuse a key in softland.store that the store does not know?", he said
   "yes" (‹1a5ec25a›). So a gate refuses such a fact with
   `:store-key-not-known`, a
   session's word, and a tool's own fact under the bare word `:class` or
   `:permission` is taken like any other. A session's reading, not his: the
   namespace is taken exactly, so a key in `softland.store.x` is not the
   store's (K15). Open, found that day: what a key is (entry 9 of 6 October;
   K5).

Open, and built to a working answer until Sid decides:

- **What a key is (K5).** The code holds a key to a readable keyword, with a
  namespace or without one. The rulings file of September says that a key is
  an id, and that its word and its grammar are facts about it.

5. **Beside the four: what Sid said on 9 October that no box asked.** Positions in his
   words, kept here because the reads sheet asks about each; none is a tick on a row.
   The first three are of one dictated line (‹85a64cfb›).
   - How often a tool writes: "for an editor we do write things into it so each uh, word type or
     each letter that you type don't how often do those get synced these are also the decision that
     the tool has to make like how collaborative it is that that is a spectrum and that is a choice
     that uh, layers and tools and uh, groups can make". Bears on W4 and W5 of the reads sheet, and
     on K12.
   - Undo and redo: "what does undo and redo mean if it is like per value how much are we undoing
     and because like if we take that as an act then it becomes a little bit easier to think about it
     but maybe all this could be uh, tool choice because we cannot uh, make that decision right now".
   - The grain of a value and of an act: "the tool is deciding how much of their different actions
     are put together inside a single value or inside a single act".
   - The word for whatever makes an act: "maker is fine general word for what we are doing imo"
     (‹22f90886›).

## Decisions of 11 and 12 October 2026 — reading back out

Sid answered these in chat on 11 and 12 October 2026, on the sheet "Reading back
out, second sheet — what the read build needs from you"
(`src/proposal/backend-on-rama-2026-10-02/decisions/groups/g-reads.txt`). Words
in quotation marks are his, of those two days unless a date is given, as he
typed them. The record holds his lines as ‹601c6793› and ‹cad7d6b5› (11
October) and ‹191d9442› (12 October), and one "yes" after them. Where a session
read his answer onto a box, the entry says so. What a session drew from an
answer is marked as a session's, and what stays not decided is named. No code
changes with them. They shape the read build, whose starter is
`src/proposal/backend-on-rama-2026-10-02/BUILD-reads.md`.

1. **Who may see a layer: a setting on the layer, and a permission that says
   what it allows.** Sid decided (W1 C). On 10 October: "The owner of the layer
   decides they can say on the group members or everyone, so everyone's data in
   that layer is avaailble if the owner says so and people are joing it". Asked
   whether a members-only layer ever has someone who may see and not write:
   "yes ofc and I am not sure why is this a question as i have said many times
   the softland system is providing the full spectrum so". On 12 October he
   confirmed it with "yes", to these words of a session: "a layer has a
   setting, members or everyone, and a permission says see, or see and act. The
   three extras stay undecided, and the build treats them as temporary."
   A session's reading, which he did not strike: "members" is whoever holds a
   permission in the layer, since the store holds nothing else that the word
   could mean; "everyone" takes in a person who is not signed in (entry 2 of 8
   October). Not decided, and kept only so that the build can run:
   - A holder has one permission in a layer, as built. She cannot hold "see"
     from one giver and "act" from another.
   - The setting takes a second write. Who may write it is not said: his word
     is "the owner", and no layer names an owner as a fact (entry 5 of 8
     October).
   - A new layer starts as "members" and the base as "everyone". With the
     reading above, a person who is not signed in then sees the base; that is
     half of the queue's K14.
   - A permission that says "see" cannot later be made "see and act" in that
     layer: a permission's place takes one write, and nothing ends a
     permission. A session read this in `gate/decision.clj`; it was not run.
2. **Which layers a read looks through: the layer says, and a view may ask for
   another list.** Sid decided (W2 C): "yes", and "ok" after a session showed
   him two of its follow-ons. On 10 October: "Agrreee". A layer says which
   layers it builds on, in order, as a fact on the layer. A read gets that list
   when the view names none. A view may give another list, of layers that the
   asker may see. The two follow-ons that he was shown and let stand: a layer
   of the list that the asker may not see is left out, and the read says that
   it left one out; the nearest layer that holds a fact at a place wins, also
   when that fact says "nothing here". Three more are on the sheet and were not
   put to him one by one; they are a session's: the list is one more store key,
   and where a layer builds on two, the one who writes the fact states the
   order; a read is given a list, or a layer to walk the list from, and where a
   read starts and where a person's acts go are two choices of the view; the
   store key takes a second write.
3. **A read is done in the runtime, straight from the tables, with Rama's
   push.** Sid decided (W3): "I think yes", and then "ok". On 10 October: "the
   electric missionary client should read from the pstates directly
   correct??". His ruling on the sheet's last follow-on: "yeah i also don't
   consider it a cache". That follow-on: Rama's push keeps the current value in
   the runtime's memory, and Rama keeps it level; the read path shares one live
   read among those who look at the same thing, and ends it when the last one
   leaves. Rama's own page says "the current cached value" of it
   (`reference/rama/docs/28-clj-defining-modules.md`); his ruling is on what it
   is. The other follow-ons are a session's, and he struck none: the read path
   is one piece of code in the runtime, and a tool asks it; "trusted and
   verified for their layer" is checked when a live read opens, and the read
   path keeps a live read on the permission too; a screen is shown the newest
   state; one push watches one value. On the last, see entry 6: one value can
   be a run of places.
4. **Only programs on machines that Sid runs may reach Rama's ports.** Sid
   decided (W9 A): "A", and then "yes". On 12 October: "this is a solved
   problem correct you can say the same for google.com ... even if someone hits
   to the server ip we can reject because of no permisisons or other
   techinques nginx etc.". What A carries, from the sheet: each program that
   can open the ports is trusted with the whole store, in any person's name; a
   program that is not trusted that far talks to the runtime; the rule is a
   setting of the network, set and checked when a cluster is first run for real
   data. A session's note to his line: Rama asks no name and rejects nothing,
   so the rejecting is done before Rama, by the network. Read in Rama's docs,
   not run.
5. **The tables keep a value plain, when a layer has not chosen otherwise.**
   Sid decided (W10 A): "A", and then "we are keeing everything plain see Z
   sections". The gate keeps the plain bytes that it already has when it
   checks a value. The copy in the log is the other sheet's Z2, under entry 9:
   that phase came after the read build, and entry 2 of 12 October then made
   the log plain too: the door seals nothing. A layer whose members hold their own locks keeps its values
   sealed in the tables too; that kind of layer is not built.
6. **What can be asked: a query written as data, made of a few words; and one
   more order for the keys that a layer marks.** His position, 11 October:
   "can this not be set for each user, tool, layer, group from data?? because
   we don't know how different tools,users or groups might want right?? so how
   do we decide on what we want is the query engine generalised enough that we
   can make query from data side??? which is what we will need.What we need
   For starters is defined by what is possible". On 12 October: "there can be
   thousands of tools that get build then like how are we deciding right
   now???" A session then ran what the sheet had left unrun, put the result and
   the following to him, and he answered "yes" (W11; it is box B with his
   change):
   - The first read build serves one place; one entity, or any run of
     neighbouring keys; and what points at a thing. Each can be asked once, or
     kept live.
   - A tool hands the read path a query written as data. Nobody decides what a
     tool asks. What is decided is the few words that a query is made of. A
     word that does not exist yet, such as finding by text or looking back, is
     added to the store once, in code.
   - "What points at a thing" is switched on by a fact on the layer that marks
     a key. For a marked key the gates keep the facts in one more order, by
     value.
   The run: one push can watch a run of neighbouring places in a table of the
   facts table's shape, and it sends only the changed, new or removed place.
   It cannot watch a whole table. In-process, one machine, Rama 1.6.0:
   `src/proposal/backend-on-rama-2026-10-02/reads/push-probe/RESULTS.md`.
   Not designed, and the builder's to propose: the form of the fact that marks
   a key, and who may write it; the facts of a key that were written before it
   was marked; the form of the new table; what it costs each write.
7. **From a fact, a read can get who wrote it, and under which permission.**
   Sid decided (W12 B), by a session's reading of "agree with you". What he
   agreed with, in the session's words to him: "keep the writer within reach
   for every fact, and let each tool choose whether to ask". The session told
   him that it read this as box B, and that the further parts of box C stay
   not decided; he did not object, and his "yes" of 12 October was to a summary
   that held "the writer with each fact". His first line on it, 11 October:
   "maybe it is needed for some groups or tools for some reason but this does
   not make it default to all that is going to be build on sofland". The answer
   to that line: a read that does not ask for the writer is sent nothing of it;
   the row keeps it within reach. Not decided: the whole act from a fact (box
   C): its session, because of which act, and what it stood on.
8. **The base's second is fine.** Sid decided (W13 Yes): "hmmm yes". The base
   stays a microbatch layer (decision 4 of 7 October). The sheet names two
   things that the read build does whichever he ticked; they are a session's,
   and he struck neither: it shows no fact of a batch that could still be tried
   again, and it never gives nothing for a place that holds a fact. A session's
   note, which he did not answer: with no optimistic update, whoever writes
   into the base waits that second to see her own fact. Since 13 October the
   base's gate closes no batch (entry 5 of that day), so nothing waits for a
   run that closes one: a maker reads her answer when the batch of her act is
   committed. A read still shows no fact that a second try could change,
   because a second try of a batch makes every row as the first made it.
9. **The other sheet comes after the read build.** Sid, 12 October: "all the Z
   phase can be done after this one imo". The sheet is "Before real data is
   kept" (`decisions/groups/g-check.txt`). His answers to it, with his lines
   of 11 October on its questions, are the section of 12 October below.

## Decisions of 12 October 2026 — before real data is kept

Sid answered these in chat on 11 and 12 October 2026, on the sheet "Before
real data is kept — what the check of 10 October leaves to settle"
(`src/proposal/backend-on-rama-2026-10-02/decisions/groups/g-check.txt`),
questions Z1 to Z6. Words in quotation marks are his, with their day; some
are dictated, and are given as the dictation has them. The record holds his
lines as ‹727f622d› and ‹cad7d6b5› (11 October) and ‹149ad161›, ‹9ba38b70›,
‹55b56bf3› and ‹071290dc› (12 October); his other lines of that evening are
in the same chat. Entry 7 answers a question that the sheet did not ask: the
read build's session found it. Where a session read his
answer onto a box, the entry says so. What a session drew from an answer is
marked as a session's, and what stays not decided is named. No code changes
with them. They shape the build whose starter is
`src/proposal/backend-on-rama-2026-10-02/BUILD-before-real-data.md`.

1. **Every table can be made again from the logs, and a lost machine is met
   by copies and backups.** Sid decided (Z1). **On 13 October he set aside
   the box that a session had proposed under this entry**, the log of what a
   gate decided and the rebuild that applies it (entry 5 of that day). The
   entry's own line stands and is met another way: the tables are made again
   by running the gates again over the two logs of acts, and a second run
   gives the same tables (the aim of that day). What follows is the record of
   12 October. On 11 October: "can't we just
   replay the log?? why it says A ?? when its deployed there will be a backup
   that will run and even if the main gets blown up we should be able to
   replay log and get to latest ...". A session answered that his sentence
   holds two losses. A lost machine takes the log with the tables, since both
   are on its disk; a copy on a second machine or a backup meets it. A table
   that is wrong, or that needs another form, while the log is whole, is what
   a replay meets. The session proposed a box that the sheet did not print: a
   gate decides once, and what it decided also goes into a log, so that a
   rebuild applies the answers and decides nothing again. On 12 October:
   "yes I think both are answerign different parts and I covered both and that
   is the correct answer". A session's reading: his "yes" is to that box. What
   comes with it:
   - Decision 2 of 7 October stands: the gates' PStates are the record that
     the door and every read use. Its reason does not; that entry says so.
   - The sheet's box B, a gate that reads no clock and makes no lock, is not
     taken. The stamp of 8 October (entry 4) stays.
   - A session's, which he was shown and did not strike: a replay that
     decides again gives another answer on the day that a rule of a gate
     changes, so an answer is kept and is not worked out again.
   - With entry 2 the store keeps no locks, so no table is outside the logs
     on their account. In the base the last stamp of a batch is in no act and
     in no answer, as the read build's session found; it goes into a log too.
     He was told so and did not object.
   - "Two depots, one for each gate" (decision 1 of 7 October) counted the
     depots of acts. The module already has a third depot, of the clocks of
     batches, from which no gate decides (the read build's receipt, section
     10, row 10), and this decision adds the log of answers. A session's
     note; nobody put it to him as a question.
   - Not designed: the form of that log, and of the rebuild. Not run: that
     the tables made again are the same.
2. **Every value is plain in the log, the store keeps no locks, and the
   shared logs are never cut.** Sid decided (Z2: plain, the sheet's box A,
   without the purge that the box held, and with no sealed end beside it). On
   11 October: "plain we will not
   store any locks and such by default it is up to the layer, tool, group etc
   if they want to for their they can and should but not by default and we
   are providing the spectrum." Asked on 12 October whether a forget may cut
   a value out of the log: "lets go with simple policy answer by default
   since this is spectrum. there is cost to be paid for replay .. I think the
   answer is we will not do the trim, say there is a sensitive organisation
   we might have a seperate depot for them specifically if that works not
   sure but i think one group or tool cannot mandate what we do for everyone.
   They can lock their values if they want add encryption and manage their
   (or we manage and host but we charge and also give them their local key)".
   A session's reading, which he was shown and did not strike: nothing is cut
   out of a shared log, by a trim or for a forget; and "there is cost to be
   paid for replay" says that a log which can be replayed stays whole.
   A session first read "not by default" as a store that keeps its locks for
   whoever chooses them, and cut the next build's contract so. That evening
   he asked: "we are removing all the locks right??" (‹9ba38b70›). The
   session laid out the two builds, and what keeping the locks would cost: a
   look for each act at the door and at a gate, in layers that lock nothing
   too; and code that is not in the form of the ends that he had named. He
   asked "Is there a harm in keeping it?", named one himself, "whenever we
   will build some code, it will always come up", and decided: "yeah lets
   remove it" (‹55b56bf3›). What comes with it:
   - The door seals nothing, a gate makes and opens no lock, and the store
     has no table of locks and no request for locks.
   - Decision 3 of 6 October, in its half "the door seals, and a gate refuses
     an unsealed value", and entry 2 of 9 October (one lock seals one value)
     are set aside. They are not reversed: they say how a sealed end is, and
     the store has none now. The request for locks leaves the store's own
     keys (entry 5 of 8 October).
   - The store can forget nothing until a later build. A delete is a
     retract: the thing is gone from now and stays in the past.
   - Whoever wants a value that only they can open seals it before it
     reaches the door, and keeps the lock. A session's, reasoned and not run:
     the store keeps what they give as any other value, and can check, order
     and show nothing inside it.
   - The code of the locks, with its proofs, stays in the repository at
     commit `13a1d65b`, the end of the read build.
   - The sheet's "time", how long a forgotten thing may stay in a plain log,
     is asked no more: nothing is cut.
   - His other ends, named and not built: a depot of its own for an
     organisation; and locks that we host for a charge and also hand to
     their owners. For the first, read in Rama's docs and not
     run: a depot can be added and destroyed at a module update, trimming can
     be set for one depot, and a cut is declared on one depot
     (`reference/rama/docs/19-operating-rama.md`, lines 356, 422 and 424;
     `14-depots.md`, "Migrations"). It takes a deploy by whoever runs the
     store, and cannot be made from data. A gatherer's list of 12 October
     names about twenty places of the store's code that take the depots of
     acts to be two; no session checked them one by one.
   - The sheet quotes his line of 23 September as "gets deleted in some sense
     like it is not recoverable". The whole line (‹6304b683›) begins "what
     happens on delete or something it is dependent on the groups", asks
     whether it is "by carrying two different logs or uh, maintaining a
     cryptographic key", and puts the quoted part as "maybe is is there a
     choice that we have". In the line the quoted part is "gets uh, deleted in
     some sense like uh, it is not recoverable". It is no position of his for
     a cut of the log.
3. **The log is kept whole.** Sid decided (Z3 A), on 11 October: "forever
   never trim". Rama's trim setting stays off. A session's: with entries 1
   and 2 this holds for each log of the module, the two of acts, the clocks
   of batches and the answers; the read build's receipt asked it of the
   clocks of batches (section 10, row 11). Nobody has priced the disk. Since
   13 October the module's logs are the two of acts (entry 5 of that day).
4. **Whose reads are kept: a model's, always; a person's, until she turns it
   off.** Sid decided (Z4 B). On 11 October, of a model's reads: "yeah we
   should for them store it". Asked on 12 October what holds for a person
   when nobody has chosen: "yes by default if its not set then we take the
   side of spectrum that captures more because we are not choosing the
   destructive side by defualt that can happen even after but the other side
   cannot." A session had suggested the other box, C; this answer is his. A
   session's reading: his reason is a rule for a default wherever one end
   keeps and the other drops. A session's note: with entry 2, what was kept
   before she turned it off stays in the log. Not built, and not in the next
   build: nothing in the store says who is a model and who is a person until
   sign-in, and where such a line lands is not decided. The read path keeps
   nothing of a read today.
5. **Three machines, three copies and Rama's own backups, when real data is
   kept.** Sid decided (Z6 C). On 11 October: "idk but we will do it right as
   rama team will suggest and  i think that will include backups and all".
   On 12 October, told what Rama's pricing page shows: "yeah i am fine with
   $100 and the other hosting costs". What the page showed, fetched that day
   through a tool that gives a summary of a page, and read in full by no
   session: the free licence, two nodes, without Rama's own backups;
   "Bootstrap", three nodes with backups, 100 dollars a month. Not decided:
   which machines; and the number of tasks, which a session reckons and
   brings to him as one number before a cluster is launched. A session's,
   read and not run: with entry 1 the number of tasks can be changed later by
   a second run from the logs, with downtime
   (`reference/rama/docs/19-operating-rama.md`, "Task scaling").
6. **The eight of Z5: yes to each.** Sid, 12 October: "yeah agree", to a
   session's "I would say yes to all eight". They are: backups to storage
   that is not this machine, and one restore, before real data is kept; one
   pass over this page and over the rulings file of September, which marks
   each entry as his, as a fact, or as a session's; "freezes" goes from the
   papers; the limit of four on the depth of a permission is lifted, and a
   bound on the size of an act takes its place; three lines for the method;
   the check's six reports, and a row in the queue for each thing on the
   sheet's last page, go into the repository; Rama's docs for the version
   that the store runs; one page that lists what must be true before real
   data is kept. The fourth is in the next build. Each of the others is a
   session's work of its own.
7. **An act from a maker who may not act is written to the log, and then
   refused. The door checks nothing first.** Sid decided, 12 October. The
   read build's session found that the request for locks had been a stop: a
   maker with no standing got no lock, so the door appended nothing of her
   act. With no locks (entry 2), either the door looks first and appends
   nothing for her, or it appends and a gate refuses. A session suggested the
   first. He asked how she could send at all with no access. A session
   answered that a polite tool sends nothing, and that a careless tool, a
   script, or a holder of "see" can: an act names its layer by id and needs no
   sight of it. His answer, dictated (‹071290dc›): "why don't we store both?
   Like the data is useful." Of a maker in good faith: "That can be raised to
   the place that they were trying to write." Of one in bad faith: "we will
   see like what they are trying to do", and "we have the data to make our
   systems better, but we also block them." Of what she sent: "this data
   would not be written down to our actual data, because it is denied." Of an
   author whose act is refused: "if they have it in the log, we can send them
   back, hey, this was rejected." And: "we are not choosing the destructive
   choice here." Then: "Yes, B". The session changed its suggestion to his:
   a check of a permission keeps neither bad content nor a flood out of a
   log, since a maker who may act can send both. What comes with it:
   - What is handed to the door is not lost when it is refused: the act is in
     the log, its answer says refused, and no fact of it is in the tables.
   - Not built, his three uses: to raise a refused attempt to the place that
     it was for; to see what a sender in bad faith does; to give an author
     back what was refused. Nothing reads a refused act today. A session's
     question, not yet asked of him: whether the owner of a place sees what a
     stranger sent, or only that she tried.
   - Not built, and due before a stranger can reach the door: a bound on how
     much one sender can put into the log, and a way to block a sender. A
     session's: a layer is decided on one thread, so a flood of refused acts
     at a layer slows its own makers; and what is written before a block
     stays.

## Decisions of 13 October 2026 — the store comes out the same when it is run again

Sid made these on 13 October 2026, in chat, after he went through the receipt
of the build before real data
(`src/proposal/backend-on-rama-2026-10-02/before-real-data/RECEIPT.md`). That
night he sent them as eight numbered decisions in the prompt of the build "run
again" (`src/proposal/backend-on-rama-2026-10-02/run-again/`). A session wrote
the prompt's text at his word, from his lines of the day, and he sent it; where
an entry gives a sentence as "the prompt's", it is in those words. Words in
quotation marks with an id of the record are lines that he typed or dictated;
dictated lines are given as the dictation has them. What a session drew from a
decision is marked as a session's, and what stays not decided is named.

The eight entries below have his eight numbers. Two things that the prompt
says before its list stand before them here: the aim, and what the base is.

The build "run again" built entries 1 to 5 and 8 in the night of 13 to 14
October. Its page and its receipt are in its folder.

**The aim: the store comes out the same when it is run again from its
depots.** The prompt's first lines: "The store has to come out the same when
it is run again from its depots. Today it does not, and it has five depots
where I decided two." This is his note of 7 October (decision 2 of that day):
"it is recomputable given the version a act was proposed at". What comes with
it:

- A proof that runs the store's gates a second time: a second module that
  mirrors the two depots and decides every act again from their beginning,
  with every row of every table compared. The prompt's: "Write first the
  proof that runs the store's gates again over its depots and compares every
  table ... It must fail on today's store." It failed on the store as it was,
  in 3,064 rows, and passes on the store as it is.
- A kept set of acts with the tables that they gave. The prompt's: "Keep the
  acts of both depots and the tables they gave as a fixture. Every later build
  must reproduce those tables from those acts, also with another number of
  tasks."
- A session's, which the build found and he has not seen: for a second run to
  give the same, a gate can read nothing whose order no depot holds. So a gate
  reads no clock (entry 1), no table of the other gate, and no count that it
  shares with another layer (entry 8).

**What the base is.** He said it on 13 October because sessions had built on
another meaning. His lines (‹4123e23c›, dictated): "The base there means like
the code that we are writing"; "any user who gets onboarded to Softland, they
have this uh, basic admin provided things that they can do. Maybe like
ingestors or some other stuff"; and of who changes it: "at that layer things
are only going to change by either me making the changes or a few set of
people or agents making the changes to the core of uh, system." The prompt's:
"It is not partitioned by an entity. I said the base is microbatch because it
does not need to be instant. I never decided that its acts are spread over
partitions." Decision 4 of 7 October stands: the base is a microbatch layer.
Not decided, the prompt's: "whether the layer that many people write into is a
separate layer from the base". Nothing is built for a microbatch layer with
many writers.

His line for the first three entries (‹f8a673a6›, dictated): "From my point of
view, it is the clock comes before the depot. There is local ordering and then
everything is partitioned by the layer key."

1. **The clock comes before the depot.** The prompt's: "The clock comes before
   the depot. The door writes the time on the act. No gate reads a clock."
   What comes with it:
   - The door adds the time to a maker's thing, beside the act-id and the
     depot. A stamp is made of that time, and the stamp of 8 October stands
     (entry 4 of that day). Changed on 14 October: no stamp is made of the
     time, which is a label beside it (entry 1 of that day).
   - Not decided, his list in the prompt: "what a second send of the same act
     carries as its time"; "what happens to a record that arrives with no
     time"; "what the time on the act is called"; "where a time that is far
     ahead is stopped". Each is built to a small working answer that the code
     marks "Not decided: kept for now"; the store's page lists them.
   - A session's, for him to weigh with the last of those four: the rule of 10
     seconds on a carried stamp was held against the gate's own clock. It is
     held against the time on the act. Through the door that is the clock of
     his server. A thing that is appended without the door carries a time that
     its sender wrote.
2. **Order is local.** The prompt's: "There is local ordering: the order Rama
   gives inside one partition." So the store has no order between two layers,
   and none between its two depots; what a gate decides rests on the order of
   one layer's acts alone.
3. **Every act is placed by its layer key, the base too.** The prompt's: "Every
   act is placed by its layer key, the base too. A layer is one line." Earlier
   that day he had asked of the base (‹7b869b91›, dictated): "why is it a case
   for us that we are having two partitions? And we will not be following the
   rule of uh, everything must stay in order, goes into one partition." Both
   depots place a thing by its `:layer`. A layer's acts are one partition, and
   their order is the depot's. Decision 10 of 6 October is met another way:
   the depot derives no entity.
4. **The base's rows: a copy on every task. One task decides, in the line's
   order.** The prompt's words. A session had put three ways to him: the
   base's rows on its one task, a copy on every task, or spread by entity as
   built. His answer (‹f5283354›): "ok B it is". The reasons are a session's,
   which he was shown: everyone reads the base, and with its rows on one task
   every read of it in Softland would run on one thread and share Rama's bound
   on live reads of one table on one task; and a copy is his own logic of 7
   October (decision 5 of that day). What a copy costs, a session's: the base
   is stored once on each task, a write to it is a write on each task, and a
   reader on another task sees a change a moment later than a reader on the
   base's own task. The build found one cost more: askers share a live read of
   the base only when their reads start from the same layer.
5. **Two depots. The depots of answers, the rebuild and the depot of batch
   clocks go.** The prompt's: "The two depots that hold the answers go, and
   the rebuild with them. The depot of batch clocks goes. Two depots stay."
   Of the depots of answers, after a session had argued to keep them
   (‹6f6300a5›): "no i will remove them this is weak logic imo". The session's
   three reasons for keeping them, which he read and decided against: a rule
   of a gate will change one day, and a second run would then decide an old
   act under the new rule; a table can be changed by the code that owns it,
   and a depot can only grow; and whatever is built later in Rama can be fed
   only by a depot. Entry 6 is his answer to the first. What comes with it:
   - Decision 1 of 7 October stands as he made it. The box under entry 1 of
     12 October, a log of what a gate decided and a rebuild that applies it,
     is set aside.
   - A gate closes no batch, and the door's reads wait for none.
   - Not designed: how a store with real data moves to another number of
     tasks. A session's, read in Rama's docs and partly run: Rama adds tasks
     by a new module that reads the old depots from their beginning, which is
     the second run of the aim.
6. **A change in what a gate accepts is a new version of the act. The old
   version's rules stay in the code.** The prompt's words, with: "This starts
   with the first real record. Until then the fixture is made again only when
   a rule is changed on purpose, and the receipt says so. In this build it
   means the rule on the page and the fixture; the code still knows one
   version." A session proposed the rule to him that day, as the answer to
   what a second run does with an old act when a rule of a gate has changed
   since, and he took it into his prompt as his decision. What comes with it,
   a session's: a second run decides an act under the rules of the act's own
   version, so a fault in an old rule stays the answer of the acts that it
   answered; the correction is a new version, from then on.
7. **He is against the Rama setting that orders a layer's acts after a
   failure.** His line (‹278f6238›): "yeah i am against the setting as well
   ... so that is sorted what next?". The setting is
   `topology.stream.max.executing.per.task` at 1; the measure that he read is
   in the receipt of the build before real data, section 5. The prompt's:
   "Nobody has shown whether a failure in a fast layer can make acts that were
   in flight come out in another order when the store is run again. Run it
   and find out. If it can, look for a cheap way to close it. If there is
   none, say it on the store's page, and the proof names such acts." What the
   run found is on the store's page, under the order of a layer's acts after a
   failure.
8. **A number counted per layer. The door's clock is its server's. What the
   door cannot label stays as built.** The prompt's: "A number counted per
   layer. The clock the door carries is the clock of the server that runs the
   door. What the door does with a thing it cannot label stays as built." A
   session had put the three to him as things it would add or leave unless he
   said no, and his answer (‹febe1bee›) was: "to me the remaining ones and
   what to do with them seems right to me". What comes with it:
   - A gate keeps one last stamp for each layer. Two layers on one task count
     apart, so a layer's stamps do not depend on which layers share its task
     or on how many tasks the module has.
   - The door tells the maker of a thing that it cannot give an act-id to,
     and appends nothing for it, as it did.

How the build went, in the prompt's words: "Not the Rama skill's seven phases
on every piece." "Work in parts. Commit a part only when every proof passes, so
the store is whole wherever the night ends." And of the first proof, a
sentence that a session wrote and that he sent on as a line of its own
(‹932247ec›): "The first proof is the one exception: commit it while it still
fails, say which part makes it pass, and until that part is in it may go on
failing."

## Decisions of 14 October 2026 — order and time apart

Sid made these on 14 October 2026, in chat, from a page of his refinements
that a session wrote at his word
(`src/proposal/backend-on-rama-2026-10-02/order-and-time/REFINEMENTS.md`), and
from a session's questions on it. Words in quotation marks with an id of the
record are lines that he typed. Where he answered with a session's wording,
it says so.

1. **Order and time are apart, and order never uses the time.** His lines
   (‹aa212e6b›): "so we also make the order and time different is that what
   you saying??? btw my intent was the same"; (‹2f80019d›): "so from what i
   am understanding: order and time are different and we don't use the time
   for ordering. every thing that goes in depot has a time attached to it"; and, asked whether that was his
   direction, (‹296a2a7b›): "my intent was the same yes". What comes with it:
   - A stamp is the layer's count of decisions: the last stamp plus one, or 0
     for the layer's first, for an accept and a refusal alike.
   - The time on the act is a label. A gate writes it beside the stamp and no
     rule reads it. The rule of 10 seconds on a carried stamp went with it,
     and so did the bound of 2^45 on the time, whose one reason was the
     time's place in the stamp. Of the four things that 13 October left not
     decided, "where a time that is far ahead is stopped" fell away: a time
     far ahead is a wrong label and nothing else.
   - It replaces entry 4 of 8 October, his answer "clock-time+counter"
     (‹f3f530b8›) to the sheet's H2 A, "the wall time and a counter, in one
     integer". What it gives up: one number for "as of 15:00" in every
     layer. A session's view, which he did not rule on: that can be built
     later from the depots, which keep every act with its time and what it
     is based on.
2. **An act's stamp is not raised above the stamps that it carries, and no
   check holds a carried stamp against the layer's last stamp.** He answered
   a session's two questions with that session's wording, "Drop the jump
   above carried stamps? Yes." and "Keep the own-layer check? Leave it out:
   one rule fewer, and it protects nothing" (‹296a2a7b›; the words are those
   of «6c16b53a»), and with his own (‹f6a39dde›): "makes sense both of them".
   That an act came after what it is based on is read from its `:based-on`,
   by fact id. No gate reads a carried stamp, so its bound of 2^62 went.
3. **The time is on the rows and on the answer.** Asked whether the time goes
   on each fact's row and on the answer, he answered "1) yes" (‹296a2a7b›). A
   session's, for him to review: a read hands it when the query asks for it,
   as it hands who wrote a fact; and a refused thing keeps its time in its
   answer when it held a usable one.
4. **A thing that arrives with no time is refused.** "2) refuse"
   (‹296a2a7b›). It was built so on 13 October and kept for his word.
5. **Time travel is designed later.** "3) time travel will do later"
   (‹296a2a7b›). The refinements page holds what a session understood of it.
