# The store's line, piece 2: a permission for one run, and for one act

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
[who layer granted-in above :see-and-act [:act uuid digest]]
                                                   one act: the act with that uuid in
                                                   its act-id and that digest
```

- **`[:run session]`.** `session` is the id that an act carries as its
  `:session`: one sitting of a person, or one run of an agent (the runtime's
  page, entry 7). Ten thousand instances of one agent are one who and ten
  thousand runs, each with a permission of its own or with none.
- **`[:act uuid digest]`.** `uuid` is the uuid of the act's act-id, which its
  maker holds before the act is sent. `digest` is the SHA-256, as 64 hex
  characters, of the thing as it arrives, without the time on it and without
  its `:permission`: so it holds the act's who, its session, its facts and
  every other part, and it can be made before the permission is given. The
  act names the permission, so the permission cannot hold a digest that
  covers the permission itself. A permission for an act says `:see-and-act`:
  one that says `:see` would allow nothing.
- **Where each sits.** A fact under a key of the store's, in the layer:

  | What it covers | The fact sits on | Under |
  |---|---|---|
  | The layer | its holder | `:softland.store/permission`, as today |
  | One run | the session's id | `:softland.store/run-permission` |
  | One act | the act's uuid | `:softland.store/act-permission` |

  Three keys and not one, because a holder, a session and an act are each
  named by an id of the same kind, a keyword or a UUID: under one key, a give
  for a run whose session has the id of another holder would land on that
  holder's place. The two words are a session's.
- **Above.** Each is given straight beneath the giver's own permission, in the
  giver's layer, as today. The giver's own covers the layer: a permission for
  a run or for an act gives nothing (R10).

## The statements

What must hold, said without rows and stamps. S1 to S10 are those of
`gate-histories/src/gate_histories/reference.clj`; these change or add:

- **S6 becomes:** in a layer a holder has at most one permission that covers
  the layer and stands. Beside it she can have a permission for each of any
  number of runs and of acts, each standing or ended by itself.
- **R1.** An act is taken under a run's permission only when the act's `:who`
  is the permission's holder, the act's `:session` is that run, the
  permission stands, and it allows acting.
- **R2.** An act is taken under an act's permission only when the act's `:who`
  is its holder, the uuid of the act's act-id is the one that the permission
  names, the act is the act that the permission names, part for part, and the
  permission stands. No other act is ever taken under it.
- **R3.** A permission for a run or for an act stands as any permission does:
  it was given by a holder whose own permission stood, straight beneath that
  one; it was not ended since; and no permission above it was ended since it
  was given. So the end of a holder's permission for the layer ends every
  permission that she gave beneath it, for a layer, a run or an act.
- **R4.** The end of one of a holder's permissions ends no other permission of
  hers, unless that other is beneath it.
- **R5.** A permission for a run or for an act is ended by its giver, by
  anyone above the giver, and by its holder. Its holder ends it by an act
  under that permission itself, or under her permission for the layer: never
  under her permission for another run or act, so a run ends nothing of hers
  but its own. After an end, the run or the act can be given a permission
  again. The same holds for her permission for the layer: she ends it under
  itself, and no run of hers ends it.
- **R6.** A run or an act has at most one permission that stands.
- **R7.** A setting, a list and a mark are written under a layer's first
  permission alone: never under a permission for a run or for an act (S8,
  unchanged in its words).
- **R8.** An asker may see a layer that is open to its members when her
  permission for the layer stands; or when she asks in a run, and that run's
  permission stands and names her as its holder. A permission for an act lets
  nobody see.
- **R9.** A thing that names a permission for another run than its own, or for
  another act, is refused, whatever else is right in it.
- **R10.** A permission for a run or for an act gives nothing: a give in an
  act under one is refused. Giving takes a permission that covers the layer.
  Else a holder for one run could give herself, or anyone, a permission for
  the layer beneath the run's, and a yes for one run would be a yes for every
  run: the session that writes the generated histories found that road in
  these statements before any of them was built. What is given beneath a
  permission must cover no more than it does, and neither a second run nor a
  layer fits inside one run. It is the side that can be widened later: a run
  that may give would be one more word in the sixth slot.

## What changes in the store

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

What an act costs more, read from the plan and not measured: nothing under a
permission for a run, whose row is read where the holder's was; one SHA-256
over the act under a permission for an act. A read that names its run holds
one row more in each layer that is open to its members, and the rows above
it.

## Not in this piece

- A permission to see that is sized by a query; a yes for one read. A read has
  no id until the trail is built (piece 5).
- A permission that stands for a count of acts, or until a time: a gate reads
  no clock.
- Two permissions for the layer that stand at once for one holder, "see" from
  one giver and "see and act" from another. S6 keeps that at one.
- How a person is asked, and what a yes is shown as: the product's.

## Proofs

As the store's rules have them, and as piece 1 ran them: the pure proofs, the
module and the read path on the in-process cluster in both gates, and the
run-again proof with a line of this piece's acts in its hand; the fixture is
made again on purpose, and the receipt says so. The generated histories hold
the statements above before the piece is committed.
