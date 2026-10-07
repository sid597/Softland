# The store — source entry

This tree holds the code of the store that is built to stay. The implementation
lives beneath [softland/](softland/README.md). It holds the act: the one form of
what a maker gives to the store, and the check of that form. It owns no state.

The rig (`src/proposal/rig-2026-09-25/`) is an example build, not a source: no
namespace of this tree requires it.

Start with the question you are carrying:

| Question | Enter here |
|---|---|
| What is in this tree, and what state does each part own? | [softland/](softland/README.md), then [store/](softland/store/README.md) |
| What is an act, and why is a thing refused as one? | The namespace docstring of [act.clj](softland/store/act.clj), then its two declarations, the shape and the rules |
| What did Sid decide about the form of an act? | [Decisions of 6 October 2026](#decisions-of-6-october-2026), below |
| Where can I challenge a behavior with an existing check? | [Verification map](../test-store/README.md) |

"Act" is the word in this tree. The rig's code says "offer" for the same thing.

This tree has its own classpath. The alias `:store` in the root `deps.edn` holds
`src-store` and `test-store` as `:replace-paths`, and Clojure alone as
`:replace-deps`. So the root `src` and `resources`, the rig and the root's
dependencies are not on it, and a require of any of them from here fails at load.

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

1. **The act is the one form and the check of it; nothing else.** Sid decided.
   `act.clj` makes no act-id, and has no stand-in sealed form and no digest.
2. **The one form is the maker's act.** Sid decided. It is what a converter
   gives, before the door touches it. The door's additions (the act-id, the
   class, the seal) are the door's own small declaration, which contains the
   maker's; a gate checks what arrives against it.
3. **The seal is below the form.** Sid decided. The act holds plain values. The
   door seals, and a gate refuses an unsealed value by its own rule. So the act
   has no sealed bytes, no lock id, no store keys, no placed keys, no mark, and
   one kind of fact.
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
   understood; it holds more than the UUID, so a session wrote act-id — his to
   change. The act still refers to
   act-ids, so the shape of an act-id stays declared. Derived from two of his
   rulings (the door makes the act-id; a series gets all its act-ids before its
   first act is sent), not his tick: act-ids are given on request, ahead of
   sending. The two class words are `:streaming` and `:microbatch`. Sid
   decided: they are his words for the two kinds of depot, and the class words
   freeze into every act-id at the first kept record.
6. **A fact is the named slots `{:e :k :v :replaces}`.** Sid decided:
   `:replaces` is optional, a fact id; `:v` is optional; a fact has no mark.
   Proposed by a session for a blank box (6b) and agreed by Sid in chat on
   6 October: a fact has no layer of its own. Also blank (6c): where a fact that
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
9. **An id is a readable keyword or a UUID; a key is a readable keyword only.**
   Sid decided. The ids are an entity, who, a layer, a session, a subject and
   the layer slot of an act-id. A key is a fact's `:k`.
10. **The microbatch hash entity is no part.** Sid decided. The depot derives
    it.
11. **`:based-on` is a map from a fact id to a carried stamp.** Sid decided.
    Proposed by a session for a blank box (11C) and agreed by Sid in chat on
    6 October: an outside thing is recorded as a fact first, so its basis is
    that fact. Kept
    from today's code, not decided: the part is optional, and empty when absent.
12. **A carried stamp is an integer.** Sid decided. Kept from today's code, not
    decided: a carried stamp is below 2^62, an act names 256 subjects at most,
    and a value is inside 32 collections at most. Closed by a session on
    6 October, waiting for his word: a carried stamp is not below zero, because
    no gate gives one.

## Decisions of 7 October 2026 — the store's module

Sid answered these in chat on 7 October 2026, one by one, on the sheet "The
store — the questions the gates need". They shape the module built next: the
depots, the gates and the record. As each part is built, its folder's README
carries the decision that shapes it and this section points there. What is not
yet decided is queued in
`src/proposal/backend-on-rama-2026-10-02/decisions/OPEN-QUESTIONS.md`; answers
never live there.

1. **Two depots, one for each gate: the streaming depot and the microbatch
   depot.** Sid decided (F6 A, Q7 a).
2. **The record is in the gates' PStates.** Sid decided (F2 A). His reason:
   the record is recomputable from the acts depot — an act proposed at a
   version is refused the same way every time — so a second depot of
   decisions adds nothing. The acts depots are the replay log.
3. **The members of a layer set its class, as a fact on the layer.** Sid
   decided (F1 A). The "third switch" of the 2 October drawing was never his
   agreement; this is.
4. **The base is a microbatch layer; every other layer starts streaming.** Sid
   decided (F4 C). His heuristic: work a person engages with directly — an
   editor, a canvas, 3D, live collaboration — must be interactive, so
   streaming; data subscribed to from another layer may arrive a batch later,
   so microbatch. The base, which everyone subscribes to, is the extreme of
   the second kind.
5. **Each layer holds a copy of each grammar it uses; a gate reads on its own
   task.** Sid decided (F5 A), agreeing with the cost: a copy is bytes and a
   rare write; a fetch is a cross-task hop on every act.
6. **The door reads a streaming act's answer from the record by act-id; the
   ack is a signal.** Sid decided (Q22 A): an ack can fail to carry the answer
   — an error for an act the gate accepted — and a microbatch act never has
   one.
7. **A rule for all code: what leaves a decision — an ack, a bell, a
   promotion's second act — is a signal; the truth is the record; read it by
   act-id.** Sid decided (F7 A). A gate reads its own record by act-id before
   it decides, so a repeat is answered, not decided twice.
8. **The gate refuses a setting key on an entity that is not the act's layer,
   as a recorded decision.** Sid decided (F8 C). The door may warn early; the
   gate is the authority, because anything can append to a depot without the
   door, and the one form knows no store keys (decision 3 of 6 October).
9. **The three limits are targets to optimise toward; the rig's measurements
   are the starting point, not a ceiling.** Sid decided (F9 b, c). Two nodes
   on one machine is a question for production (F9 a).
10. **The act's id is called act-id.** Sid asked why it was called a name when
    "act-uuid" would be understood; it holds more than the UUID —
    `[layer class uuid]`, the layer and the class route the act and find its
    answer — so a session wrote act-id; the word is his to change. **The word
    "class"** (streaming or microbatch) is borrowed and not accepted; it stays
    until Sid gives one.

## Decisions of 8 October 2026 — who acts, and what the store reads

Sid answered these in chat on 8 October 2026, against a list of eight that the
build session put to him: the two questions of its own sheet (K1, K2) and six
rows of the sheet "The record, and who acts". Words in quotation marks are his,
of that day unless a date is given. Where a session read his answer onto a
row, the entry says so; he let those readings stand the same day. They shape
the gates' check of a permission, the making of a layer, the stamp and the
seal.

1. **The system stands at the top of the base's chain.** Sid decided (E1·H8
   A): "system". His line of 6 October names the three that act: a user, a
   group, or "the system basically software"; "if it is a system level thing
   like we as the architect of it are trying to do then it comes under that
   part".
2. **A signed-in user has a personal layer and a permission in it, without
   asking.** Sid decided (K2): "the user already is logged in as a user so
   they automatically have their personal layer and permissions". A user who
   is not signed in is "just roaming around on what is open by the system or
   world group". He agreed to this form of it: the system makes the layer and
   the permission at the first sign-in, and the person who creates a group
   makes the group's layer. The builder's part, not his: each layer is made by
   one act, its first, so that the making of a layer is in the acts depot
   (decision 2 of 7 October). Sign-in is not built.
3. **Permission is a hierarchy that ties back to one person.** Sid decided: a
   permission "ties back to a single individual", however deep the groups and
   agents go; to walk back along it comes down to who created the group,
   "invited who, who gave the permission to do xyz in their group". An act in
   a personal layer comes under the person's own permission. A session read
   three rows from this, and he let the reading stand: each actor acts under
   its own permission (E4 A); whoever holds a permission can give one beneath
   it (E2 A; Sid: "I answered this in 4 imo"); the top of a chain is a person,
   or the system for the base, and never a session (E3·H7 B).
4. **A stamp holds the clock time and a counter.** Sid decided (H2 A):
   "clock-time+counter".
5. **The store's keys in this build are three: the class, a permission, and
   the door's request for locks.** A session proposed the list (K1 A). Sid:
   "idk what else is missing or could be missing, so lets go". No layer names
   an owner as a fact. The words are his to change: "class" is borrowed
   (decision 10 of 7 October), and the request for locks is written
   `:lock-request`, a session's plain word, because "lease" is the rig's.

Open, and built to a working answer until Sid decides:

- **A lock for each value, or one for each session and layer (H1).** Sid:
  "do we need to answer now???" The module makes a lock for each value, as
  ruling 7 of the rulings file says. This build keeps no record, so his answer
  can still change it.
