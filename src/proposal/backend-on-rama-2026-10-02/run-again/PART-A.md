# Part A as built: what changed in the store's source

Written from the finished source of part A, for the builders who adapt the proofs and
for whoever reads the build later. The source is `src-store/softland/store/`. A name in
backticks is in the code as written here.

Sid's decisions that this part builds (13 October 2026): "The clock comes before the
depot. The door writes the time on the act. No gate reads a clock." · "The two depots
that hold the answers go, and the rebuild with them." · "A number counted per layer.
The clock the door carries is the clock of the server that runs the door."

Part A does this for the door and the streaming gate. **The microbatch gate is not
changed in part A**: it still reads its batch's clock, keeps its depot of answers,
closes its batches, and reads the streaming gate's table for a layer of the other gate.
Part B writes that gate again.

## 1. The door writes the time on the act

- The door adds a third part to a maker's thing, beside `:act-id` and `:depot`:
  `:door-time`, the clock of the server that runs the door, in milliseconds since 1970.
  The name is made in one place, `softland.store.door.declaration/time-part`.
- `softland.store.door.additions/thing-to-append` takes `[reading act-id door-time]` and
  adds the three parts. It reads no clock.
- `softland.store.door/send-act` reads the clock once in each call (a private
  `server-clock`, `System/currentTimeMillis`) and hands it on.
- A maker's thing that already holds `:door-time`, also with nil, gets the door's word
  `:add-its-parts` at `:door-time`, and nothing is appended. The `:says` text of that
  word in `door/words.clj` names three parts.
- A second send of one act under one act-id carries the clock of the second send.

## 2. The digest leaves the time out

`softland.store.door.digest`: `digest`, `hex-of` and `readable-hex-of` take the digest
of a plain map without its top-level `:door-time`. Two things that differ only in that
part have one digest. A map from which the part cannot be taken (a sorted map whose
keys cannot be compared with a keyword) is digested as it is, so it keeps a digest of
its own. So a second send has the digest of the first, and both read the one answer.

## 3. The declaration knows the time

`softland.store.door.declaration`:

- `time-part` (`:door-time`), `max-time` (2^46), and `usable-time`: a thing as it
  arrived -> its time when it is a fixed-width integer, 0 or more and below 2^46; else
  nil. It never throws.
- The shape `:arrived-act` holds the time's part after `:depot`. A thing whose time is
  absent, nil, no fixed-width integer, below zero, or 2^46 or more is refused with the
  new word `:no-door-time`, which is in `reasons`.
- The order of the words: a part that the form does not know, and a `:depot` that is no
  depot word, come before it; each of the maker's own parts comes after it.
- `stray-reason` is as it was: the time plays no part in whether a thing is a stray.
- **Every thing on either depot needs the part.** Both gates call the one `check`, so a
  thing that a proof's hand appends with no `:door-time` is refused `:no-door-time` by
  the streaming gate and by the microbatch gate alike.

## 4. The stamp

- `softland.store.gate.stamp/next-stamp [time-millis last-stamp greatest-carried]`: the
  greatest of the time with a counter of zero, the last stamp plus one, and the
  greatest carried stamp plus one; each only when it is there; 0 when none is.
  `too-far-ahead? [time-millis carried-stamp]` is false for a nil time.
- `softland.store.gate.decision/time-on [thing]`: the usable time of a thing as it
  arrived, or nil. A thing that the declaration refuses for another fault keeps its
  time, so the stamp of its refusal holds it. A thing with no usable time has a stamp
  of the layer's last plus one, or 0 in a layer with no stamp yet.
- `refused-early [time-millis act reason]` and `stamp-of [time-millis last-stamp act
  refused]` keep their arities. The first argument is the time on the act in the
  streaming gate, and the batch's kept clock in the microbatch gate until part B.
- In the streaming gate the last stamp is the LAYER's: two layers on one task count
  apart, and two layers can hold the same stamp.

## 5. A layer of the other gate

- `softland.store.gate.first-act/starts-with-the-other-gates-depot? [gate-depot layer]`.
- The streaming gate hands its result to `decision/standing` and
  `decision/makes-the-layer?` as their fifth argument, `layer-is-the-other-gates?`.
  So an act that arrives on the streaming depot for a layer that starts with the other
  depot, the base, is refused `:layer-belongs-to-the-other-gate` whether or not the
  base is made. It was `:layer-not-made` before the base was made.
- The microbatch gate still hands those two functions what its own read of the
  streaming gate's table gave.

## 6. The tables

`softland.store.record.tables/task`, the schema of `$$streaming-task`: `:last-stamp`
holds a map from the text of a layer id to that layer's last stamp. The path of one
layer's number is `(keypath :last-stamp (softland.store.door.declaration/id-text layer))`.
`:strays` is as it was. `microbatch-task` is as it was.

## 7. The streaming gate

`softland.store.module.streaming-gate`: no clock, no depot of answers, no partitioner,
no append, no wait, and no read of another gate's table. `add-to` takes `[topologies]`.
The module has no depot named `*streaming-answer-depot`. The walk of one event is in
the namespace docstring. An accepted act with one value under a plain key that is not
marked reads 4 rows and writes 3.

## 8. The module, and what is gone

- `softland.store.module/add-gates-and-reads [setup topologies]` adds everything of the
  module but its two depots of acts; `Store` is those two depots and that call. The
  run-again proof's second module mirrors the two depots and calls it.
- The module has four depots: the two of acts, the depot of batch clocks, and the
  microbatch gate's depot of answers.
- Gone, taken out with `git rm`: `rebuild.clj`, `rebuild/placing.clj`,
  `rebuild/reader.clj`, `rebuild/README.md`, and the proofs `rebuild_proof.clj` and
  `rebuild_pure_proof.clj`.

## The smoke run of part A

`smoke/part_a.clj`, on the store itself in Rama's in-process cluster, before any proof
was adapted. It showed: the time in an accepted act's stamp is the time that the door
wrote; two layers on one task each count their own number; a thing with no time, a nil
time and a time of 2^46 are each refused `:no-door-time`, and 2^46 less one is taken;
an act for the base on the streaming depot is refused `:layer-belongs-to-the-other-gate`
while the base is not made; a second send has the digest of the first and another time,
and both read the one answer; and the module has no `*streaming-answer-depot`.
