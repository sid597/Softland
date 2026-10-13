# Part B as built: what changed in the store's source

Written from the finished source of part B, for the builders who adapt the proofs and
for whoever reads the build later. The source is `src-store/softland/store/`. The plan
of this part is `PLAN-the-bases-gate.md`; `PART-A.md` says what part A changed before
it. Where this page and `PART-A.md` differ, this page is the later one.

Sid's decisions that this part builds (13 October 2026): "There is local ordering: the
order Rama gives inside one partition." · "Every act is placed by its layer key, the
base too. A layer is one line." · "The base's rows: a copy on every task. One task
decides, in the line's order." · "The two depots that hold the answers go ... The depot
of batch clocks goes. Two depots stay." · "No gate reads a clock."

## 1. The module has two depots

- `softland.store.module/Store` declares `*streaming-depot` and `*microbatch-depot`,
  both `(hash-by :layer)`, and calls `add-gates-and-reads`, which takes `[topologies]`
  alone. A second module that mirrors the two depots calls it with its `topologies`.
- There is no `*batch-clock-depot`, no `*microbatch-answer-depot`, no `$$batch-clock`.
  No topology appends to any depot.
- A layer is one partition of its depot, so its acts have the depot's order, and the
  partition's task is the layer's task.

## 2. One walk for both gates

- `softland.store.module.walk/decide [$$answers $$facts $$facts-by-value $$task *gate
  *thing]` decides one thing on the task where it is called and writes what the
  decision writes. It gives back the rows of an accept that a gate may copy: each
  `[:fact key row]` and each `[:by-value key row]`; an empty vector for a refusal, a
  repeat and a stray. `copies-of` is its pure part.
- It holds no partitioner, no yield and no append, and none may be added: a walk that
  gives its task back is decided out of the depot's order.
- `softland.store.module.streaming-gate/add-to [topologies]` is one call of the walk.
- The walk reads its pure functions through their vars at each call when its namespace
  was loaded with the store's own functions in them. A proof that puts a function of
  its own into such a var loads the store's module first.

## 3. The microbatch gate, written again

`softland.store.module.microbatch-gate/add-to [topologies]`. For each thing of the
microbatch depot, in the depot's order, on the task of its partition: the walk; then,
for an accept, the copy of its rows to every task but the deciding one.

- **Where a row of a microbatch layer lives.** Its facts and its facts by value: the
  same rows on every task. Its answers and its last stamp: on the layer's task alone.
  A stray's digest: on the task of the partition where the stray arrived, which is the
  task of what the stray holds under `:layer`. `$$microbatch-task` has the schema of `$$streaming-task`
  (`softland.store.record.tables/task`): `[:last-stamp <text of the layer id>]` and
  `[:strays word]`.
- **The order.** The acts of the base are decided in the order of their appends. Two
  acts that one maker sends one after the other are decided in that order.
- **The stamp.** The greatest of the time on the act, the layer's last stamp plus one,
  and the greatest carried stamp plus one. The 10-second rule is held against the time
  on the act.
- **The marks.** An act is decided with the layer's marks as they were before it. A
  newly marked key gets the stamp of the act that marks it. A fact under that key in
  the next act is in the order by value.
- **A layer of the other gate.** An act that arrives on the microbatch depot for a layer
  that starts streaming is refused `:layer-belongs-to-the-other-gate`, whether or not
  that layer is made. No table of the streaming gate is read.
- **A second attempt of a batch** makes every row as the first made it. Nothing is
  closed by a later run, and nothing waits for one.
- **A throw in the walk** restarts the worker, as in the streaming gate. Nothing that
  arrives makes the walk throw.

## 4. What is gone

- The files `gate/batch.clj` and `module/batch_clock.clj`, taken out with `git rm`. With
  them: turns, questions, replies and parcels, the meeting task, the keeping task,
  `first-entity`, `on-every-task?`, the kept clock of a batch and the throw
  `clock-must-be-kept`.
- `:turns`, `:to-keep`, `:to-close` and `:closed-through`.
- In `record/rows.clj`: `answer-record`, `stray-record`, `log-record`, `closed?`,
  `by-value-route`. In `record/tables.clj`: `microbatch-task`, `batch-clock`.

## 5. The pure code that proofs call

- `softland.store.gate.decision/standing [gate-depot act permission-row depot-row]` and
  `makes-the-layer? [gate-depot act permission-row depot-row]`: four arguments. Whether
  the layer is the other gate's is worked out inside, from the gate's depot and the
  act's layer.
- `refused-early [time-millis act reason]` and `stamp-of [time-millis last-stamp act
  refused]`: the first argument is the time on the act, for both gates. In `writes`,
  `:level-from` is the stamp of the act, for both gates.
- `softland.store.record.rows/routing-id [act-id]` gives the act-id's layer, for both
  depots. `answer-read-by-routing-id [given act-id]` gives `[depot key]` or nil.
  `answer-key-parts`, `id-of-text` and `answer-read` stay.

## 6. The door's reads

- `answer-by-act-id [routing-id act-id]`: the routing id is the act-id's layer. One read
  of the answer's row on the layer's task.
- `depot-of-layer [layer]`: one read of the layer's depot place on the layer's task.
- Neither is held to a closed batch. A maker of the base reads her answer when its
  batch is committed on the base's task. The ack of the microbatch depot still says
  only that the depot holds the thing.

## 7. The read path

Every source of a microbatch layer is read under the routing id of the read's home: the
layer that the read starts from, `:from-layer` or the first of `:layers`. So the base is
read on the task where the asker's own layer lives. A source of a streaming layer is as
it was. The read path asks no task by number, and uses no class of Rama's for that.

## The smoke run of part B

`smoke/part_b.clj`, on the store itself, through the door and by hand, before any proof
was adapted. It showed: the base's making act, a give and the first act under it, sent
one after the other, are all accepted; a chain of 200 acts in one batch is accepted with
stamps rising in the depot's order; the facts and the facts by value are the same map on
all four tasks, and the answers and last stamps are on their layer's task; a mark holds
from the next act; the word for a layer of the other gate is the same before and after
that layer is made, on both depots; the module has its two depots and none of the three
that went; one read gives a base act's answer; and a second module over the same two
depots made all eight tables the same, 0 rows differing.
