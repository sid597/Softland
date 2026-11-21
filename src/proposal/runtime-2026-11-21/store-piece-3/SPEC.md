# The store's line, piece 3: a layer's acts and refusals, read by stamp

[The runtime's page](../PROPOSAL.md), entry 9 and "The order of the build".
[Piece 1](../BUILD-store-piece-1.md) is built; this piece stands on it.

**21 November 2026.** Written by the Claude session that built piece 1, as the
spec that the Rama skill's phases start from
(`.claude/skills/rama/references/phases.md`). Phase 0 has run:
[IMPLICIT_SPEC.md](IMPLICIT_SPEC.md) holds what this page implies and does not
list, where its words were looser than the code, and nine open questions, each
marked Sid's or Phase 1's. This page was corrected from it on the same day:
the asker's own things in a layer that she may not see, the who of a refused
thing, the bound in bytes, and the base. **The plan, Phase 1, starts when piece
2 has landed**: the two pieces change the same files of the store, and a plan
made before would be made against code that piece 2 changes. It says what is asked, what the
store is today, and what must hold. It designs no table and no topology: that
is Phase 1's. Three kinds of statement are kept apart: **Sid's**, **a
session's**, and **a fact of the code**, with the file that says it.

## What is asked

**Sid's.** The runtime's page, entry 9: "The store gains a read of a layer's
acts by stamp, each whole and with its answer. A refused act can be read by its
own who and by whoever holds a permission in the layer. A visitor of a layer
that everyone may see cannot, unless the layer turns it on." He typed "5.2 B I
think" (‹93a88844›) and then agreed to that narrowing on the phone. The reason
for the narrowing: anyone can send an act that will be refused, and without it
every visitor of an open layer could read what a stranger sent, for ever.

**Sid's**, 12 October 2026 (`src-store/README.md`, "before real data is kept",
entry 7), the three uses of a refused act that nothing serves today: to raise a
refused attempt to the place that it was for; to see what a sender in bad faith
does; to give an author back what was refused. "Why don't we store both? Like
the data is useful."

**A fact of the code.** The store already keeps every act, taken or refused,
whole, in its layer's depot, and gives every decision a stamp with no gaps,
refusals in it (`src-store/MODEL.md`, "What the store keeps"). What is missing
is the way back out (`MODEL.md`, "What cannot be got back, said plainly"):

- an act by its act-id gives its answer and not the act: "no row keeps where in
  its partition the act is";
- a layer's refusals as a list: "no read is built for them";
- what an act carried besides its facts (`:session`, `:because-of`,
  `:based-on`, `:subjects`, `:claimed-when`, `:version`): "the depot holds them
  and no table does".

## The read

One new thing that can be asked of the store, through the read path, which
holds the one check of who may see (`src-store/softland/store/read_path.clj`):

- **For one layer, the decisions from a stamp on, in the layer's order.** For
  each: its stamp; its act-id; its answer as the module's read by act-id gives
  it today (`softland/store/record/rows.clj`, `answer-of`: the verdict, the
  reason of a refusal, the stamp, the time, the digest); and the thing whole,
  as it arrived on the depot: the arrival that was decided, and not another
  arrival with the same digest, since the digest leaves the time out.
- **Bounded, by count and by bytes.** A caller names where it starts and how
  many it takes at most. One read gives a bounded count, and says where the
  next begins. A count bounds nothing by itself: the bound on an act
  (`door/declaration.clj`, `max-act-bytes`) counts the text that is written
  into rows, not the thing whole, and a refused thing has no bound. So the
  read has a bound in bytes of its own, on what is fetched and on what is
  handed over. A thing larger than the bound does not stop the line: its
  stamp, its act-id and its answer are given, it is told apart from one that
  is withheld, and the next read begins behind it. The bounds are numbers of
  the build beside the read path's others (`read_path/query.clj`,
  `default-numbers`).
- **It says what it left out.** For an asker who may see the layer, a decision
  that she may not read whole is given as its stamp and nothing else of it, so
  that the stamps that she gets have no gap that she cannot tell from a fault.
  A session's choice. The other way gives nothing for it, and the caller sees
  a gap. An asker who may not see the layer is told less: the next section.
- **The layer's last stamp** can be asked with it, so a caller knows whether it
  has reached the end.

Whether the read can also be kept live, so that a caller is told each new
decision of a layer as it is made, is Phase 1's to weigh and to say. The trail
and an importer will want it (the runtime's page, pieces 5 and 8). A read
asked once is the floor of this piece.

A stray is not in it. A stray has no answer and no stamp: a gate keeps its
digest alone (`softland/store/module/walk.clj`, step 0).

## Who may read what

The asker is what the caller says, as for every read today: sign-in is not
built, and the runtime will hold the asker against the caller.

| The decision | Who is given the thing whole |
|---|---|
| A taken act | Whoever holds a permission that stands in the layer; and the act's own `:who` |
| A refused thing | The same: whoever holds a permission that stands in the layer; and the `:who` that the thing names, when it names a usable one |
| Either, in a layer that everyone may see, for an asker with no permission there | Nobody, unless the layer's setting below says `:everyone` |

- "A permission that stands" is piece 1's rule, the gates' own
  (`softland/store/gate/chain.clj`, `stands?`), which the read path already
  reads for whether an asker may see a layer
  (`read_path/layers.clj`, `permission-state`). `:see` is enough: this is a
  read.
- **A session's reading, to be shown to Sid: a taken act, whole, is held to the
  same rule as a refused one.** His entry names the refused act. A taken act
  whole gives back what no read gives today: its `:session`, what it was based
  on, the one act that it answers, and `:subjects`, the people that it is
  about. Whether a read reaches "the whole act from a fact" is not decided
  (`src-store/README.md`, 11 and 12 October, entry 7, box C). So a visitor of
  an open layer reads its facts as today and is given no act whole until the
  layer says so. The other way: a taken act whole to whoever may see the
  layer.
- **The who of a refused thing is read from the raw thing.** The declaration
  refuses many things that name a usable `:who`: one with no time, one with a
  value over the bound. Such a thing is given to the who that it names. A
  thing that names none is read by the layer's holders alone. No row holds the
  who of a refused thing today, and the walk has no act for a thing that the
  declaration refused (`softland/store/module/walk.clj`, steps 2 and 4): so
  it comes from the thing as it arrived, by code that cannot throw.
- **An asker is given her own things in any layer.** A thing that names her as
  its `:who`, taken or refused, is given to her whole, with its stamp and its
  answer, also in a layer that she may not see, and after her permission
  there ended. It is his third use of 12 October, "to give an author back
  what was refused"; the door's answer already tells her the stamp. Of any
  other decision of a layer that she may not see she is given nothing, not
  its stamp either: the stamps that she gets then have gaps, and the answer
  says that the layer is otherwise left out. A session's resolution: Phase 0
  found that this page said this and its opposite.
- **In a layer that everyone may see, this read reads the asker's permission**,
  which a read of facts does not there
  (`softland/store/read_path/sources.clj`, `reading`): holder or not decides
  whole or withheld.

## The setting

**Sid's**: "unless the layer turns it on". **A session's**: its form. A sixth
key of the store on the layer's own entity, beside who may see it, what it
builds on and the keys it marks (`softland/store/door/declaration.clj`,
`store-keys`): who may read the layer's acts whole, `:members` or `:everyone`,
reading as `:members` while no fact says it, for the base too
(`softland/store/gate/first_act.clj`, `starts-with`, which gives the base
`:everyone` for who may see it): the base is open to everyone, and its acts
are read whole by its holders alone until its first holder says otherwise. It takes a second write,
and only the layer's first holder writes it, as piece 1 has it for the other
three (`softland/store/gate/decision.clj`, `who-may-write-fault`). With
`:everyone`, whoever may see the layer reads its acts whole. The word of the
key is a session's and Sid's to change; `:softland.store/who-may-read-acts` is
a working word.

## What must hold

1. **The store's promises stay** (`MODEL.md`, "What the store promises"). A
   depot is never changed and never trimmed. A gate reads no clock and no table
   of the other gate. Whatever a gate comes to keep for this read is made of
   the thing, the rows read and the stamp, and of nothing else.
2. **A second run of the gates over the depots makes the same tables**, on a
   store with another number of tasks too (`src-store/README.md`, decisions of
   13 October; `test-store/softland/store/run_again_proof.clj`, "the form with
   no task"). So nothing that a gate keeps may hold a position in a depot
   partition or the number of a task, unless the proof's form with no task can
   say why it is left out. A position differs between a store of 4 tasks and
   one of 8.
3. **The walk never gives its task back** between its first read and its last
   write, and none of the four that would let it may be added
   (`softland/store/module/walk.clj`, the namespace docstring). What this piece
   adds to a decision is bounded by the thing.
4. **A thing can be anything.** The door is not the only sender, and a refused
   thing is kept as it arrived. What a read gives back of it must not throw,
   and must be bounded: the bound on an act holds only for an act that passed
   the declaration (`door/declaration.clj`, `max-act-bytes`).
5. **Both gates.** A streaming layer has every row on its own task. The base
   has its facts on every task and its answers on the base's task alone, and
   is read on the task of the read's home
   (`softland/store/read_path/sources.clj`, "Where a layer is read"). The read
   of the base's acts must say on which task it reads, and why that task holds
   what it needs.
6. **The answer that a caller gets is the one that the door gives.** One act-id
   has one answer, that of the first thing decided under it. A second thing
   under a used act-id is in the depot and has no stamp of its own: it is not
   in this read, as a stray is not.
7. **Nothing is shown that the asker may not read**, also in the moment in
   which a permission ends: a read that is asked once reads her permission
   with the rows, as a read of facts does; a read that is kept live leaves the
   layer when her permission ends, or one above it (piece 1).
8. **Cost is said.** What each decision costs more, in reads and writes, for
   both gates; what one read of n decisions costs, in seeks and in round
   trips; what the store keeps twice, if anything, and how much. Counted from
   the dataflow where it is not measured, and said so.

## What Phase 1 has to weigh, and is not told

- Where the thing whole comes from at a read. Two roads are known. A gate
  keeps a row for each decision, by layer and stamp, that holds the thing
  again: the depot's content is then kept twice. Or a gate keeps, by layer and
  stamp, only what finds the thing, and the read takes the thing from the
  depot: Rama's client reads a depot partition by position
  (`.claude/skills/rama/references/depot-reference.md`,
  `foreign-depot-read`). The second meets line 2 above head-on, and needs a
  gate to know the position of what it decides; whether a topology is given
  it is not known here. There may be a third road.
- Whether the answers' table, which is keyed by layer and act's uuid
  (`softland/store/record/rows.clj`, `answer-key`), can serve the order by
  stamp, or a table of its own is needed.
- Whether the read is one more word of a query
  (`read_path/query.clj`, `words`), or a call of its own beside the read
  path's five. The spine fixes the words of `read` and `watch` with the trail
  in mind (the runtime's page, "The spine, to start from"): a layer's acts by
  stamp and a session's trail are the same shape of read, a run of one line in
  its order.

## Proofs

As the store's rules have them (`src-store/CLAUDE.md`, "The checks explain
themselves"; `test-store/softland/store/README.md`):

- pure ones, with no cluster, for whatever is pure: who may read what, on rows
  that the proof builds;
- the module on the in-process cluster, both gates: taken and refused things
  of each kind, read back by stamp, whole and with their answers;
- the read path on the cluster: each row of the table "Who may read what",
  the setting turned on and off, and an asker whose permission ends;
- the run-again proof: what the gates keep for this read is the same when
  they are run again, and from kept acts on 4 tasks and on 8. A rule of the
  store changes on purpose, so the fixture is made again, and the receipt
  says so (`test-store/softland/store/run_again_fixture/README.md`).

Each cluster run goes behind the machine's lock:
`flock src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock`.
Another session builds the runtime in this tree at the same time
(`src-runtime/`, `src-wire/`, `src-command/`, `deps.edn`): this piece stages
its own paths by name and leaves those alone.

## Pages that change with it

`src-store/MODEL.md` ("What can be asked", "What cannot be got back"), the
maps of the folders that change, and the proofs' map. The store's "Decisions
of ..." sections are Sid's pages and are not changed without his word.
