# The store's line, piece 2: a permission for one run, and a yes for one act

[The runtime's page](../PROPOSAL.md), entry 6 and "The order of the build".
[Piece 1](../BUILD-store-piece-1.md) is built; this piece stands on its rule of
when a permission stands (`src-store/softland/store/gate/chain.clj`).

**21 November 2026.** Written by the Claude session that built piece 1, before
any code of this piece: the form of the record, and the statements that the
build must hold, in plain words, so that the generated histories
([gate-histories/](../gate-histories/README.md)) can hold them before the rule
is built. The piece adds no table and no topology, so it is not built by the
Rama skill's phases: it changes the gates' pure code, the declaration and the
read path, as piece 1 did.

**22 November 2026: a yes for one act gets an id of its own.** The form of a
permission for one act changes, and with it R2, R5, R6 and R9; one answer for
an act-id is said as a statement; R5's words on a holder's own end now say
what a yes and a permission that says see can do. The form is the one in the
prompt that Sid pasted on 22 November (‹526c4049›). A session wrote that prompt
(«a2cc0b7a»). The form in it is the one that a long prompt sent at 07:09 that
day calls "Carbon's form" (‹b438ef46›; the record cannot say whether he typed
that prompt or pasted it). By the record he never typed the form: it is a
session's form, and his to turn. Why it changed: the
generated histories and the overnight review found that the first form, a
permission that sat on the uuid of the act's act-id, stops the yes's act for
good when anything is decided first under that act-id: the holder's own early
or wrong try, or a thing from anyone, one who may only see or holds nothing
among them; that anyone who may act can take that place first; and that its
text could not be made by a maker of what she hands the door
([gate-histories/README.md](../gate-histories/README.md);
[REVIEW-store-piece-2.md](../overnight-audit/REVIEW-store-piece-2.md), 1A, 1B
and 1C). The first form is in this file at `8b11300c`. Piece 2 as committed at
`3544d026` keeps the first form; the store is brought to this one by its own
builders.

## What Sid asked for

- "each act is the smallest atomic unit right and we can have permission over
  that" (‹45c7127b›).
- "permissions is also like someone is asking for read write access and that
  has the spectrum of everytime to always allow and that can be per run of
  some task/query/agent" (‹45c7127b›).
- "I think yes it can have more than one permission because bio-agent is not a
  single thing we can run 10k instances of the same thing unloke a human so
  each instance can have different permisiion based on different uses it has
  or work so yes" (‹95754ddf›).
- Asked whether a yes for one act covers the act's id or the exact act, he
  said that this is a question of the product: "It is abasically asking what
  all permisiions am i agreeing to by giving a heigher level permission"
  (‹4eaf571e›). So how a yes is shown, and what a person is asked,
  is the product's. The store's part is that each kind of yes on his spectrum
  can be said, and says exactly what it covers. The forms below are a
  session's, and his to turn.

## The form

A permission says what it covers, in a sixth slot of its id. Without the slot
it covers the layer, until it is ended: every permission that exists keeps
its meaning.

```text
[who layer granted-in above]                       the layer, until ended
[who layer granted-in above allows]                the same, and what it allows
[who layer granted-in above allows [:run session]] one run: the acts and the reads
                                                   of that session, until ended
[who layer granted-in above :see-and-act [:act yes digest]]
                                                   one act, once: the act whose text is
                                                   that digest, under any act-id
```

- **`[:run session]`.** `session` is the id that an act carries as its
  `:session`: one sitting of a person, or one run of an agent (the runtime's
  page, entry 7). Ten thousand instances of one agent are one who and ten
  thousand runs, each with a permission of its own or with none.
- **`[:act yes digest]`, a yes for one act.** `yes` is a UUID that the yes's
  giver makes: the yes's own id, and the place where it sits. `digest` is the
  SHA-256, as 64 hex characters, of the one text
  (`softland.store.door.text/text`) of the act as its maker made it, in the
  one form that `softland.store.act/parse` gives, without its `:permission`.
  So it holds the act's who, its session, its facts and every other part of
  the maker's act, and no act-id, no depot and no time: a maker's act carries
  none of them. A maker makes it of the act that she hands the door, before
  the yes is given; `parse` wants a permission, and the one form of every
  other part is the same whichever permission is put in for it. Two acts with
  one form are one act: a list of facts and a vector of them, a part left out
  and the same part given empty. The act names the yes, so the yes cannot
  hold a digest that covers the yes itself. A yes says `:see-and-act`: one
  that says `:see` would allow nothing.
- **Under any act-id.** The act is sent under any act-id of its maker's: the
  act-id plays no part in a yes, and a yes names none. So nothing that is
  sent under an act-id touches a yes, and a maker who learns that her
  act-id was taken first sends again under another.
- **Used up.** The gate uses a yes up when it takes the act under it. From
  then on no act is taken under it, the same act under another act-id
  neither; the yes has nothing left to end; and nothing is given at its
  place again. A try that the gate refuses uses nothing: the yes stands as it
  stood, the try's act-id keeps its refusal (one answer, below), and the
  holder sends the act again under a new act-id. The same act sent again
  under the act-id under which it was taken is answered, accepted, and uses
  nothing more. Where the gate writes the use is the store's to make; the
  walk hands `decision/writes` the row at the yes's place, among the rows
  that it read for the act (`:place-rows`).
- **Two yeses for one act.** An act can have any number of yeses that stand
  at once, each with an id of its own and each used once, from one giver or
  from several: so the act can be taken once under each. A yes sits on an id
  that its giver made, and its place takes one give, so nobody can stand in a
  giver's place: nothing that another sends writes at a yes's place or uses
  it up.
- **Where each sits.** A fact under a key of the store's, in the layer:

  | What it covers | The fact sits on | Under |
  |---|---|---|
  | The layer | its holder | `:softland.store/permission`, as today |
  | One run | the session's id | `:softland.store/run-permission` |
  | One act | the yes's own id | `:softland.store/act-permission` |

  Three keys and not one, because a holder, a session and a yes are each
  named by an id of the same kind, a keyword or a UUID: under one key, a give
  for a run whose session has the id of another holder would land on that
  holder's place. The two words are a session's.
- **Above.** Each is given straight beneath the giver's own permission, in the
  giver's layer, as today. The giver's own covers the layer: a permission for
  a run or a yes for one act gives nothing (R10).

## The statements

What must hold, said without rows and stamps. S1 to S10 are those of
`gate-histories/src/gate_histories/reference.clj`; these change or add:

- **S6 becomes:** in a layer a holder has at most one permission that covers
  the layer and stands. Beside it she can have a permission for each of any
  number of runs, and any number of yeses for one act, for one act or for
  many, each standing, ended or used by itself.
- **R1.** An act is taken under a run's permission only when the act's `:who`
  is the permission's holder, the act's `:session` is that run, the
  permission stands, and it allows acting.
- **R2.** An act is taken under a yes for one act only when the act's `:who`
  is the yes's holder, the act is the act that the yes names, part for part
  (its text is the yes's digest), the yes stands, and it is not used. The
  act-id plays no part. The act that is taken under a yes uses it up, so no
  act is ever taken under it again: one yes, one act, once.
- **R3.** A permission for a run or a yes for one act stands as any
  permission does: it was given by a holder whose own permission stood,
  straight beneath that one; it was not ended since; and no permission above
  it was ended since it was given. So the end of a holder's permission for
  the layer ends every permission that she gave beneath it, for a layer, a
  run or an act.
- **R4.** The end of one of a holder's permissions ends no other permission of
  hers, unless that other is beneath it.
- **R5.** A permission for a run or a yes for one act is ended by its giver,
  by anyone above the giver, and by its holder. Its holder ends it by an act
  under her permission for the layer, when she holds one that stands and
  allows acting; or under the permission itself, where that permission takes
  the end. A permission for a run that allows acting takes an end of itself,
  in an act of that run. A yes takes no act but its one act, so it ends
  itself only when that act is its own end, made so by its giver. A permission
  that says see takes no act at all, so its holder ends it only under her
  permission for the layer. Never under her permission for another run or
  another yes: a run ends nothing of hers but its own. A yes that is used
  has nothing left to end: an end of it is refused. After an end a run can be
  given a permission again, and an act another yes, under a new id; the yes
  that was ended stays ended. The same holds for her permission for the
  layer: she ends it under itself, and no run of hers ends it.
- **R6.** A run has at most one permission that stands. An act has any number
  of yeses, each with its own id: a yes's place takes one give, and nothing
  is given there again once it holds a yes, its end or its use. "An act has
  at most one permission that stands" no longer holds for acts.
- **R7.** A setting, a list and a mark are written under a layer's first
  permission alone: never under a permission for a run or a yes for one act
  (S8, unchanged in its words).
- **R8.** An asker may see a layer that is open to its members when her
  permission for the layer stands; or when she asks in a run, and that run's
  permission stands and names her as its holder. A yes for one act lets
  nobody see.
- **R9.** A thing that names a permission for another run than its own is
  refused, whatever else is right in it; and so is a thing that names a yes
  for another act, an act whose text is not the yes's digest, whatever its
  act-id.
- **R10.** A permission for a run or a yes for one act gives nothing: a give
  in an act under one is refused. Giving takes a permission that covers the
  layer. Else a holder for one run could give herself, or anyone, a
  permission for the layer beneath the run's, and a yes for one run would be
  a yes for every run: the session that writes the generated histories found
  that road in these statements before any of them was built. What is given
  beneath a permission must cover no more than it does, and neither a second
  run nor a layer fits inside one run. It is the side that can be widened
  later: a run that may give would be one more word in the sixth slot.
- **One answer for an act-id.** An act-id names one act and gets one answer,
  whoever sent first: the answer of the first thing that the gate decides
  under it, a refusal too. The same thing sent again under it is not decided
  again and is told that answer; another thing under it is not decided, and
  its sender is told that it is not hers (`:not-mine`). It is Sid's decision
  of 7 October 2026, "a repeat is answered, not decided twice" (the store's
  decisions of that day, entry 6), and the gate's check (a); said here as a
  statement on 22 November. So a refused try is sent again under a new
  act-id.

The generated histories hold two more beside these, which the overnight
review showed missing: **T1**, an act that the statements take is taken by
the gate, and its sender is told so; **T2**, nothing that an outsider of a
yes sends writes at the yes's place or uses it up, where an outsider is one
who is not the yes's holder, its giver or one above the giver.

## What changes in the store

Piece 2 as built (`3544d026`) changed these, for the first form:

- `act.clj`: the sixth slot of a permission id, and `allows` for six slots.
- `door/declaration.clj`: the two keys; each permission fact has the form of
  its key and sits on what it covers.
- `door/digest.clj`: the digest that a permission for an act holds.
- `gate/chain.clj`: the place of a permission is read from its form.
- `gate/decision.clj`: the standing of an act under each of the three; gives
  and ends under the two keys.
- `module/walk.clj`: the digest for a permission of an act, only when the
  act's permission is one.
- `read_path/`: a query can name the run that it is asked in, and "may see"
  then reads the run's permission and the rows above it, beside the asker's
  own.
- The pages of the tree and the proofs, with the code.

What the form of 22 November reaches, read from the code and not built here;
how is the store's builders' to make:

- the text of a yes, of the act in its one form, and a way for a maker to
  make it of the act that she hands the door (`door/digest.clj`, and the
  door), and the gate's own (`decision/hex-for-its-permission`);
- the act-id's uuid no longer held against the yes (`decision/standing`);
- the use of a yes when an act is taken under it (`decision/writes`, which
  the walk hands the row at the yes's place), and a used yes read as one
  that takes no act;
- a give at a yes's place that holds a row refused (`decision`, the give at
  a place that holds a row);
- the words of the tree that say that an act's uuid is a permission's place.

The module's walk already hands `writes` every row that it read for an act,
the row at the place of the act's permission among them
(`module/walk.clj`, steps 4 to 7). The generated histories call
`decision/hex-for-its-permission` with the thing and the act, and
`decision/writes` with seven arguments; the builders are asked to keep those
two (gate-histories/README.md lists every function that its copy of the walk
calls).

What an act costs more, read from the plan and not measured: nothing under a
permission for a run, whose row is read where the holder's was; one SHA-256
over the act under a yes, and the row of its use. A read that names its run
holds one row more in each layer that is open to its members, and the rows
above it.

## Not in this piece

- A permission to see that is sized by a query; a yes for one read. A read has
  no id until the trail is built (piece 5).
- A permission that stands for a count of acts, or until a time: a gate reads
  no clock. A yes is used once; a yes for some number of acts is not here.
- Two permissions for the layer that stand at once for one holder, "see" from
  one giver and "see and act" from another. S6 keeps that at one.
- How a person is asked, and what a yes is shown as: the product's.

## Proofs

As the store's rules have them, and as piece 1 ran them: the pure proofs, the
module and the read path on the in-process cluster in both gates, and the
run-again proof with a line of this piece's acts in its hand; the fixture is
made again on purpose, and the receipt says so. The generated histories hold
the statements above before the piece is committed: `clojure -M:run` breaks
nowhere, `clojure -M:run mutants` catches every broken copy, those that touch
a yes among them, and `clojure -M:cluster` gives on a cluster the answers of
the walk in memory ([gate-histories/README.md](../gate-histories/README.md),
"What waits for the store's fix").
