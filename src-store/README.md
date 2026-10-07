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
   Sid renamed it on 7 October: act-id, not name. The act still refers to
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
