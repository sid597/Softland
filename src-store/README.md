# The store — source entry

This tree holds the code of the store that is built to stay. The implementation
lives beneath [softland/](softland/README.md). It holds the act: the one form of
what a maker gives to the store, and the check of that form. And it holds the
store's Rama module: the two depots, the streaming gate and the microbatch gate,
the record that each gate keeps, and the reads by act-id. And it holds the door:
the client code that takes a maker's act into a depot and reads its answer back.
The pure code owns no state. The module's state, the depots and the records, is
durable in the cluster that runs it. The door's state, its stocks of unused
locks, is the memory of one process.

The rig (`src/proposal/rig-2026-09-25/`) is an example build, not a source: no
namespace of this tree requires it.

Start with the question you are carrying:

| Question | Enter here |
|---|---|
| What is in this tree, and what state does each part own? | [softland/](softland/README.md), then [store/](softland/store/README.md) |
| What is an act, and why is a thing refused as one? | The namespace docstring of [act.clj](softland/store/act.clj), then its two declarations, the shape and the rules |
| What happens to a thing that arrives on the streaming depot, and why is it refused? | [store/](softland/store/README.md), "How the parts relate"; then the namespace docstring of [streaming_gate.clj](softland/store/module/streaming_gate.clj) |
| What happens to a thing that arrives on the microbatch depot, and when is its answer readable? | [store/](softland/store/README.md), "How the parts relate"; then the namespace docstrings of [microbatch_gate.clj](softland/store/module/microbatch_gate.clj) and [batch.clj](softland/store/gate/batch.clj) |
| How does a maker's act get into the store, and how is its answer read? | The namespace docstring of [door.clj](softland/store/door.clj), then [door/](softland/store/door/README.md) for its choices and its words |
| What does the gate not check? Which choices are the builder's, and which words did a session make? | [store/](softland/store/README.md): "What the gate does not check", "Engineering choices", "Words a session made" |
| What did Sid decide about the form of an act? | [Decisions of 6 October 2026](#decisions-of-6-october-2026), below |
| Where can I challenge a behavior with an existing check? | [Verification map](../test-store/README.md) |

"Act" is the word in this tree. The rig's code says "offer" for the same thing.

This tree has its own classpath, in two aliases of the root `deps.edn`. Both hold
`src-store` and `test-store` as `:replace-paths`. So the root `src` and
`resources`, the rig and the root's other dependencies are on neither, and a
require of any of them from here fails at load.

- `:store` holds Clojure alone as `:replace-deps`. The pure code and its two
  proofs run on it. It is the guard that the pure code needs nothing else: a pure
  namespace that came to require Rama would fail to load here.
- `:store-module` holds Clojure and Rama (`com.rpl/rama` 1.6.0), and the same JVM
  options. The module and its proof run on it, and so do the door and its proof. It is a whole alias and not an
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
   Sid decided the half on an id. The ids are an entity, who, a layer, a
   session, a subject and the layer slot of an act-id. A key is a fact's `:k`.
   The half on a key is a session's (found on 9 October). The question put to
   him was what an id is, and his line was "how can it be a keyword????? we
   can have10^x no of ids where will we find the keywords for them???". The
   session that answered wrote "Keywords were the rig's choice" and proposed
   the split itself, "a key is a keyword"; no line of his takes up that half.
   The rulings file of September says, in a session's words, "Keys are ids;
   word and grammar are facts about them", and lists "Keys as ids: confirmed"
   among the nine of 24 September. He has not ruled between the two. Until he
   does, the code holds a key to a readable keyword (the queue, K5).
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
    until Sid gives one. He gave it on 9 October: "depot" (entry 4 of that
    day). The code says "depot" since the rename of that day.

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
   (decision 2 of 7 October). When the system makes a personal layer, that
   act's who is the system and the permission that it names is the person's;
   his decision does not say under which permission the system acts there.
   Sign-in is not built.
3. **Permission is a hierarchy that ties back to one person.** Sid decided: a
   permission "ties back to a single individual", however deep the groups and
   agents go; to walk back along it comes down to who created the group,
   "invited who, who gave the permission to do xyz in their group". An act in
   a personal layer comes under the person's own permission. A session read
   three rows from this, and he let the reading stand: each actor acts under
   its own permission (E4 A); whoever holds a permission can give one beneath
   it (E2 A; Sid: "I answered this in 4 imo"); the top of a chain is a person,
   or the system for the base, and never a session (E3·H7 B). On 9 October he
   decided that the system can hold many layers (entry 3 of that day), so
   "for the base" in this reading is no limit.
4. **A stamp holds the clock time and a counter.** Sid decided (H2 A):
   "clock-time+counter".
5. **The store's keys in this build are three: the class, a permission, and
   the door's request for locks.** A session proposed the list (K1 A). Sid:
   "idk what else is missing or could be missing, so lets go". No layer names
   an owner as a fact. The words are his to change: "class" is borrowed
   (decision 10 of 7 October), and the request for locks is written
   `:lock-request`, a session's plain word, because "lease" is the rig's.

## Decisions of 9 October 2026 — the door's four questions

Sid answered these in chat on 9 October 2026, on the sheet "The door — four
questions before it is built": "4B and all else A". Words in quotation marks
are his, of that day unless a date is given; some are dictated, and are given
as the dictation has them. What a session drew from an answer is marked as a
session's. No code changes with them: for three the gates already do what he
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
   layer" (dictated; "log" is the lock). His frame: "this is only about what
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
   - His two cases, which the choice turned on. Against a lock for a sitting: "if everything was
     written in one session, and then we want to forget something, but keep like ninety nine percent
     of it, that might not be possible because we have a single log for everything that was done".
     And the worry he raised against the fine grain: "if we delete a block, we might also want to
     delete some thing more related to it. For example, some context around it that was added, some
     relation link, etc.", and "we might forget that, even though the intent would be to delete". A
     session's, from that: what a forget takes with it is chosen when forgetting, not when writing;
     the things around a forgotten value were written in other acts, sittings and layers, so a lock
     for each act or for each sitting would not have taken them either. The store owes the one who
     forgets the means to find them (the queue, K11; W8 of the reads sheet
     names "a forget" as an asker).
3. **The system can hold many layers.** Sid decided (K3 A): "my preference is
   a uh, system can have uh, many layers". His picture: "what I will be
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
   and "what are we trying to do here". His own reason for deciding it now, in his words: "I have to
   decide it right now because then we would be exporting the thinking process and going more of the
   same question every time for like literally say like billions of times". A session's answer, which he heard
   before he decided: B asks a builder nothing. It gives the bare words back
   to the tools; it keeps a tool's own fact from being read as the store's;
   and it leaves the store room for more keys, where a bare word taken later
   would change how old acts are read when the record is built again from the
   log. He gave the two words the same day: "softland.store" before the
   slash, and "yes to depot" for "class" (K6). So the store's keys are
   `:softland.store/depot`, `:softland.store/permission` and
   `:softland.store/lock-request`, and a layer's depot is `:streaming` or
   `:microbatch`. Two sessions had put options to him; one preferred `store`
   and `gate`, the other `softland.store` and `depot`. A session's reasons for
   the pair that he chose: the store owns its whole namespace, so the word
   must be one that no tool would reach for; and "depot" says what the slot
   holds, in a word that the decisions already have (entry 5 of 6 October
   records the two words as "his words for the two kinds of depot"). On
   7 October he had met "gate" with "we are also using the gate order as of
   now would it not conflict with that", and "pace" with "Naming::: bro you
   total shit at it ..". The rename was made the same day. The code says
   "depot", the three keys of facts carry the namespace, and the part of a
   thing as it arrives is `:depot`. The act's own part `:permission` keeps
   its word: only the key of a fact takes the namespace. Asked "Should a gate
   refuse a key in softland.store that the store does not know?", he said
   "yes". So a gate refuses such a fact with `:store-key-not-known`, a
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
   - How often a tool writes: "for an editor we do write things into it, so each word typed or each
     letter that you type, how often do those get synced, these are also the decision that the tool
     has to make, like how collaborative it is, that is a spectrum, and that is a choice that layers
     and tools and groups can make". Bears on W4 and W5 of the reads sheet, and on K12.
   - Undo and redo: "what does undo and redo mean, if it is like per value how much are we undoing,
     and if we take that as an act then it becomes a little bit easier to think about it, but maybe all
     this could be tool choice because we cannot make that decision right now".
   - The grain of a value and of an act: "the tool is deciding how much of their different actions
     are put together inside a single value or inside a single act".
   - The word for whatever makes an act: "maker is fine general word for what we are doing imo".

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
   that phase comes after the read build, so until then the door seals for the
   log as built. A layer whose members hold their own locks keeps its values
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
   into the base waits that second to see her own fact.
9. **The other sheet comes after the read build.** Sid, 12 October: "all the Z
   phase can be done after this one imo". The sheet is "Before real data is
   kept" (`decisions/groups/g-check.txt`). His lines of 11 October on its
   questions are given here as he typed them. None is recorded as a decision:
   each wants one more word from him, or waits for a run.
   - Z2, the copy in the log: "plain we will not store any locks and such by
     default it is up to the layer, tool, group etc if they want to for their
     they can and should but not by default and we are providing the
     spectrum." That is box A. It reopens decision 3 of 6 October and entry 2
     of 9 October for the layers that do not seal. Box A asks for a time, which
     he did not give: how long a forgotten thing may stay in the log before it
     is cut out. The purge is not run.
   - Z3, the log kept whole: "forever never trim". That is box A. Whether
     "never" also holds for the purge of a forgotten thing is the same missing
     word as in Z2.
   - Z1, what brings the tables back: "can't we just replay the log?? why it
     says A ?? when its deployed there will be a backup that will run and even
     if the main gets blown up we should be able to replay log and get to
     latest". A question. He ticked neither box.
   - Z4, whose reads are kept: "model as in ai model??? and we have this
     question because we are saying that models on differnt runs can ask for
     different reads ?? yeah we should for them store it". His line gives the
     half that boxes A, B and C share, a model's reads. He did not say for a
     person's. Until that phase the read build keeps no read.
   - Z6, copies and machines: "idk but we will do it right as rama team will
     suggest and  i think that will include backups and all".
   - Z5: not answered.
