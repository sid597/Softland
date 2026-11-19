# The store as a model

[Store source entry](README.md)

What the store keeps, what can be asked of it, what it promises, and what cannot
be got back from it. Each sentence points to the file that says it in full: read
the pointer when the sentence is not enough. The numbers are not copied here;
[the last section](#the-numbers-and-where-each-is-set) names each one and the
place that sets it. This page keeps the [rules for explanations](CLAUDE.md) of
this tree: it changes in the same commit as a change to what the store keeps,
what can be asked of it, or what it gives back.

## In one breath

The store is where makers write facts, and nothing written is ever erased. A
maker is a converter, the screen or an agent. A fact is one statement: an
entity, a key and a value, as in "chair-7, colour, green". Facts arrive in acts:
one or more facts that one maker sends into one layer under one act-id. A gate
takes or refuses each act whole. It judges the act's form and its maker's
standing, and reads nothing of what a tool's values say. Taken or refused, the
act stays in its layer's line of acts for ever. A reader reads through a stack
of layers that she chooses: at each place, the nearest layer that holds a fact
wins.

In an engineer's words: an append-only, permissioned entity–attribute–value
store, with one total order in each layer and overlay reads, built on Rama.

## The words

| The store's word | What it is | An engineer would say |
|---|---|---|
| fact | One statement: entity, key, value. It may name an earlier fact that it replaces. A fact with no value is a retract: it says "nothing here". | EAV triple; tombstone |
| key | What a fact says of its entity: a readable keyword. | attribute |
| place | One entity and one key. A layer holds one current fact at each place. | cell |
| act | One or more facts that one maker sends into one layer, taken or refused whole. | atomic batch, transaction |
| act-id | `[layer depot uuid]`. The door makes it on request, before the act is sent. | transaction id |
| fact id | `[act-id index]`: one fact of one act. | version id |
| layer | A home for facts, with its own line of acts in one order. | namespace |
| depot | Where a layer's acts arrive: `:streaming`, each act decided as it arrives, or `:microbatch`, acts decided in batches. The base is the one microbatch layer. | write mode; log |
| stack | The layers that one read looks through, nearest first. | overlay view |
| permission | The right to see a layer, or to see it and act in it: `[who layer granted-in]`, or a given one that holds the permission above it. | grant; capability |
| stamp | An act's number in its layer's order, with no gaps. A refused act gets one too. | per-layer sequence number |
| mark | A key that a layer indexes by value, so that a read can ask what points at a value. | secondary index |
| the base | The one layer that everyone subscribes to. | |
| the system | The id that makes the base, and each personal layer for its person. | |

An id (an entity, a who, a layer, a session, a subject) is a readable keyword or
a UUID, and a key is a readable keyword only ([act.clj](softland/store/act.clj),
`leaves`). A value is nil, a boolean, a string, a readable keyword, a UUID, an
integer of any width, a finite double, a big integer, a decimal, or a plain map,
vector or set of these ([act.clj](softland/store/act.clj), `value?`). "Act" and
"depot" are Sid's words. "Key" is the key of a fact; an encryption key would be
a "lock".

## The path of one act

```text
maker      a converter, the screen, an agent
  │ an act
  ▼
door       adds the act-id, the layer's depot and the time of its server's clock;
  │        checks nothing of the act
  ▼
depot      the layer's line: each act as it arrived, taken or refused, never trimmed
  │
  ▼
gate       decides each act once, in the layer's order, on its form and its standing
  │
  ▼
record     answers: under each act-id, the verdict, the reason, the stamp,
  │                 the time and the act's digest
  │        facts: the current fact at each place, from taken acts only
  │        facts by value: the same facts in the order of their values,
  │                 under the keys that a layer marks
  │
  ├──▶ door        reads an act's answer back by act-id
  └──▶ read path   reads facts through a stack, once or kept live
```

The parts, and how a thing passes through them, are in
[store/README.md](softland/store/README.md).

## One act, start to finish

An illustration of the rules as built; it was not run. The base says that
chair-7's colour is red and that its material is oak.

1. **Asha makes a layer, asha-draft.** A layer is made by its first act, which
   holds exactly two facts: the layer's depot, on the layer's own entity, and
   the layer's first permission, `[asha asha-draft asha-draft]`, on Asha
   ([gate/first_act.clj](softland/store/gate/first_act.clj), `making-act?`).
   The depot is not hers to choose: the base is microbatch and every other layer
   streaming (`depot-at-making`). The act's `:who` must be the holder or the
   system, but nothing checks the sender, so anyone can make any layer that is
   not made, for anyone. A new layer reads as open to its members only, building
   on nothing and marking nothing, until a fact says otherwise (`starts-with`).
2. **She says what it builds on.** A layer has three settings, each a fact on the
   layer's own entity: who may see it, the layers that it builds on in order, and
   the keys that it marks. A setting can be written again and never retracted;
   the depot and a permission are written once
   ([door/declaration.clj](softland/store/door/declaration.clj), `store-keys`).
3. **She writes a fact:** chair-7, colour, green. The act names her `:who`, her
   `:session` and the permission that it writes under. It can also carry
   `:based-on`, the facts that she read, each with that fact's stamp as her read
   gave it; `:because-of`, the one act that it answers; `:subjects`, the people
   that it is about, so that a person can be forgotten; and `:claimed-when`, her
   own clock ([act.clj](softland/store/act.clj), `shapes`). The depot keeps all
   of it, and no read gives those four back
   ([below](#what-cannot-be-got-back-said-plainly)).
4. **The store decides.** asha-draft is streaming, so the gate decides the act
   as it arrives, and her ack says that the gate has run and its rows are
   visible. The answer is taken, or refused with a reason; either way the act
   gets the next stamp. The answer that counts is the one that the door reads
   back by act-id. It gives the verdict, the reason of a refusal, the stamp and
   the time, and tells her act from another under the same act-id by the act's
   digest; it never gives the act itself
   ([door/words.clj](softland/store/door/words.clj), `answer` and `acks`). In
   the base the ack says only that the depot holds the act: the answer is there
   once the batch that decides it is committed, so the maker asks again.
5. **Ben tries to read it,** with no permission in asha-draft. A read names its
   layers as a list, nearest first, or names one layer and takes that layer's
   builds-on list ([read_path/query.clj](softland/store/read_path/query.clj)).
   Named as asha-draft then base, the read leaves asha-draft out, says so, and
   gives red from the base. Started from asha-draft, the read takes asha-draft
   alone, since its list is a fact of a layer that Ben may not see: he gets
   nothing, and the same note
   ([read_path/layers.clj](softland/store/read_path/layers.clj), `in-order`).
6. **Asha gives Ben a permission.** A give is an act. A holder who may act can
   give a permission straight beneath her own, in the same layer:
   `[ben asha-draft asha-draft [asha asha-draft asha-draft]]`. A holder who may
   only see cannot give, since a give is an act. A holder has one permission in
   a layer, written once, and nothing ends it
   ([gate/decision.clj](softland/store/gate/decision.clj), `standing`). Ben
   reads again from asha-draft and gets green, with the layer that won
   (asha-draft), the fact id and its stamp. The base still says red.
7. **Both change the same place.** Ben writes blue, replacing the green fact by
   its fact id: taken. Asha then writes yellow, replacing the same fact: refused,
   since that fact is no longer current. A replace works only on a fact of the
   act's own layer ([record/rows.clj](softland/store/record/rows.clj),
   `written-by?`), so a fact of the base can only be covered. A fact that names
   nothing to replace becomes current: the last in the layer's order wins.
8. **Asha's connection drops,** and she sends the act again under the same
   act-id. The first decision under an act-id is the only one, and she is given
   it. A different act under a used act-id is not decided again, and its sender
   is told "not mine" ([door/words.clj](softland/store/door/words.clj),
   `answers`).
9. **Ben builds on Asha.** ben-notes builds on asha-draft. A read from ben-notes
   looks at ben-notes, then asha-draft, and no further: the base is not read,
   though asha-draft builds on it. Ben names the base in his layer's list, or
   names the whole list in his read.

   ```text
                 colour    material
   ben-notes       ·          ·        nearest
   asha-draft     blue        ·
   - - - - - - - - - - - - - - - -     a read whose list is asha-draft alone stops here
   base            red       oak       furthest
   ```

   Each read falls down its column until it meets a fact. With the base in the
   list, the colour is blue and the material oak; without it, there is no
   material. How far a read walks is a working answer for Sid
   ([read_path/layers.clj](softland/store/read_path/layers.clj), `in-order`).
10. **Ben keeps a read live.** Each change to a place that he watches is pushed
    to him. Two acts on one place can come as one change, so a caller that must
    see every act cannot rest on a live read; a place never comes with an answer
    older than one that came before ([read_path.clj](softland/store/read_path.clj)).
11. **Asha retracts the colour** in asha-draft: chair-7, colour, no value. The
    retract is asha-draft's fact at that place and the nearest, so a read from
    asha-draft finds no colour, and the base's red is hidden too. A read of the
    base alone still gives red
    ([read_path/answer.clj](softland/store/read_path/answer.clj)).

## What the store keeps

Three kinds of thing.

- **The acts.** Each act as it arrived, with all that it carries, is in its
  layer's depot. A layer's acts are one line of one depot partition, and the
  depots are never trimmed ([module.clj](softland/store/module.clj);
  [store/README.md](softland/store/README.md)).
- **What the gate made of each act.** Its answer: the verdict, the reason of a
  refusal, the stamp, the time, and the act's digest, the SHA-256 of its one text
  without the time ([record/rows.clj](softland/store/record/rows.clj),
  `answer-row`; [door/digest.clj](softland/store/door/digest.clj)). The last
  stamp of each layer ([record/tables.clj](softland/store/record/tables.clj),
  `task`). And, under a marked key, each fact again in the order of its value's
  digest, the SHA-256 of the value's one text
  ([record/rows.clj](softland/store/record/rows.clj), `value-digest`).
- **The present.** The current fact at each place, with its value, its act, its
  index in the act, its stamp, its time, its writer and its permission
  ([record/tables.clj](softland/store/record/tables.clj), `facts`). A new fact at
  a place takes the place of the row: the tables are the present, and the history
  is in the depots.

The tables are made from the depots: a second run of the gates over the two
depots makes the same tables
([decisions of 13 October](README.md#decisions-of-13-october-2026--the-store-comes-out-the-same-when-it-is-run-again);
the proof is named in the [verification map](../test-store/README.md)).

## What can be asked

- **A read,** through the read path: a query written as data
  ([read_path/query.clj](softland/store/read_path/query.clj)). It asks for one
  place, one entity, a run of neighbouring places, or what points at a value
  under a marked key, once or kept live
  ([read_path.clj](softland/store/read_path.clj)).
- **Its layers,** as a list nearest first, or as one layer and its builds-on list
  ([read_path/layers.clj](softland/store/read_path/layers.clj)). A layer that the
  asker may not see is left out, and the answer says so. An asker who is not
  signed in sees only the layers open to everyone.
- **What every answer names.** For each place: the layer that won, the fact id,
  and the stamp of that fact. A writer names that fact id in `:replaces` and
  carries that stamp in `:based-on`. On request the answer also gives the
  writer, the permission and the time
  ([read_path/answer.clj](softland/store/read_path/answer.clj)).
- **An act's answer,** through the door, by act-id: accepted, refused, nothing
  yet, or not mine ([door.clj](softland/store/door.clj), `answer-of`;
  [door/words.clj](softland/store/door/words.clj)).

## What the gate judges

- **The form:** the act's declared shape and rules
  ([act.clj](softland/store/act.clj)), inside the door's declaration, which adds
  the store's keys and the limits
  ([door/declaration.clj](softland/store/door/declaration.clj)). Under a plain
  key a gate reads nothing of a tool's value: it keeps the value's text, counts
  its bytes and, under a marked key, hashes it (`with-texts`).
- **The standing:** the act's permission names the act's `:who`, the record
  holds that permission, and it allows acting
  ([gate/decision.clj](softland/store/gate/decision.clj), `standing`); or the
  act makes a layer that is not made
  ([gate/first_act.clj](softland/store/gate/first_act.clj)).
- **A replace:** the fact that an act replaces is current, in the act's own
  layer ([record/rows.clj](softland/store/record/rows.clj), `written-by?`).
- **The store's own keys:** five, each with its shape, its entity, and whether it
  takes a second write. A retract under any of them is refused
  ([door/declaration.clj](softland/store/door/declaration.clj), `store-keys`).

What a gate does not check is said plainly in
[store/README.md](softland/store/README.md#what-the-gate-does-not-check-said-plainly).

## What nobody can forge

Four things are made by a gate, from the act and its own rows: an act's stamp,
its verdict, its digest, and the order by value under a marked key, where a read
finds a place only when a gate wrote that value there. Everything else is what a
maker said: its `:who`, all that its act carries, and every fact of a tool.

## What the store promises

- An act is taken whole or refused whole.
- In a layer, acts have one order, the stamp, with no gaps and with the refusals
  in it. Between layers there is no order, except where one act names another.
- A depot is never changed and never trimmed. The tables hold the present.
- A second run of the gates over the depots makes the same tables.
- The time on an act is a label: no gate orders or decides by it
  ([decisions of 14 October](README.md#decisions-of-14-october-2026--order-and-time-apart);
  [gate/stamp.clj](softland/store/gate/stamp.clj)). A maker's own clock,
  `:claimed-when`, is kept in the depot only.

## What cannot be got back, said plainly

- **What an act carried besides its facts:** its `:session`, `:because-of`,
  `:based-on`, `:subjects`, `:claimed-when` and `:version`. The depot holds them
  and no table does ([record/tables.clj](softland/store/record/tables.clj),
  `facts`; [record/rows.clj](softland/store/record/rows.clj), `answer-row`), so
  no read gives them. Whether a read should reach "the whole act from a fact" is
  not decided
  ([decisions of 11 and 12 October](README.md#decisions-of-11-and-12-october-2026--reading-back-out),
  entry 7).
- **An act by its act-id.** The read by act-id gives the act's answer and not
  the act. The depot holds the act, and no row keeps where in its partition the
  act is ([gate/stamp.clj](softland/store/gate/stamp.clj);
  [record/rows.clj](softland/store/record/rows.clj), `answer-row`).
- **A fact that was replaced, and the facts that one act wrote.** A table holds
  the current fact at each place. A fact's row names its act, and no table is
  kept by act ([record/tables.clj](softland/store/record/tables.clj)).
- **A layer's refusals, as a list.** Each answer sits under the key
  `"<layer> <uuid>"`, so one layer's answers lie together on its task
  ([record/rows.clj](softland/store/record/rows.clj), `answer-key`), but no read
  is built for them.
- **Every fact under one key,** in one read.
- **Who sent an act.** `:who` is a claim. A gate holds it only against the
  permission that the act names, and a permission is made of public parts: its
  holder, its layer twice, and the permission above it. Until sign-in is built,
  anyone can write as anyone
  ([store/README.md](softland/store/README.md#what-the-gate-does-not-check-said-plainly)).
- **A table that only the read path can read.** Each program that can open
  Rama's ports is trusted with the whole store, in any person's name. The one
  check of who may see is in the read path, which is a client
  ([decisions of 11 and 12 October](README.md#decisions-of-11-and-12-october-2026--reading-back-out),
  entry 4).
- **A forget.** The store can forget nothing until a later build
  ([decisions of 12 October](README.md#decisions-of-12-october-2026--before-real-data-is-kept),
  entry 2).
- **A computation over facts.** The store keeps facts and reads them back; it
  computes nothing over what they say.

## Not decided, and not built

Each is said in full where its code is.

- How far a read walks from a layer: its own list and no further is a working
  answer ([read_path/layers.clj](softland/store/read_path/layers.clj),
  `in-order`).
- Who may write a layer's settings: today any holder who may act in it
  ([gate/decision.clj](softland/store/gate/decision.clj), `who-may-write-fault`).
- What a new layer reads as: its members for a layer, everyone for the base
  ([gate/first_act.clj](softland/store/gate/first_act.clj), `starts-with`).
- How deep a permission can be ([act.clj](softland/store/act.clj), `nesting`).
- The numbers of the build: the docstring of each says whether it is decided.
- A gate's check of a key's grammar. Sid decided that each layer holds a copy of
  each grammar that it uses, and that a gate reads it on its own task
  ([decisions of 7 October](README.md#decisions-of-7-october-2026--the-stores-module),
  entry 5). The check is not built
  ([door/declaration.clj](softland/store/door/declaration.clj)).
- Whether a key is an id or a word
  ([decisions of 6 October](README.md#decisions-of-6-october-2026), entry 9).
- Sign-in, and the end of a permission: not built.

## The numbers, and where each is set

| What is bounded | Where it is set |
|---|---|
| The facts of one act | `max-facts`, [door/declaration.clj](softland/store/door/declaration.clj) |
| The entries of one act's `:based-on` | `max-based-on`, the same |
| The bytes of one value under a plain key | `max-value-bytes`, the same |
| The bytes that one act can write into rows | `max-act-bytes`, the same |
| The characters of an id or a key | `max-id-text`, the same |
| The layers that one layer builds on | `max-builds-on`, the same |
| The keys that one layer marks | `max-marked-keys`, the same |
| The subjects of one act | `max-subjects`, [act.clj](softland/store/act.clj) |
| How deep a value nests, and a permission | `max-depth`, the same |
| The layers of one read | `max-layers-of-one-read`, [read_path/query.clj](softland/store/read_path/query.clj) |
| A read's runs, checks, pushes and wait | `default-numbers`, the same |
