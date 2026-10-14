# Module — what runs in Rama

[Parent: store](../README.md) · [Store source entry](../../../README.md).

This folder holds the topologies of the store's module: the two gates, the
walk of one thing that both gates call, and the two reads by which the door
reads the module. Each file requires Rama. [module.clj](../module.clj), one
folder up, declares the module `Store` and its two depots of acts, and calls
the files here to add the gates and the reads. The module has those two depots
and no other, and no topology appends to a depot.

| File | Responsibility | State and lifetime |
|---|---|---|
| [walk.clj](walk.clj) | The walk of one thing: the decision of one thing that arrived on a gate's depot, as one Rama operation that is handed the calling gate's four tables and its depot word. It reads rows, calls pure checks and writes rows, all on the task where it is called, and gives back the rows of an accept that a gate may copy. | None of its own. It borrows the calling gate's four PStates for one call. Nothing in the memory of a process. |
| [streaming_gate.clj](streaming_gate.clj) | The stream topology `streaming-gate`: one event is one call of the walk. | It owns the four PStates of the streaming record, durable in the cluster: the answers, the facts, the facts by value, and what it keeps for a task, which holds the last stamp of each layer and the strays. Every row of a streaming layer is on the layer's task. It borrows no PState, appends to no depot and reads no clock. Nothing in the memory of a process. |
| [microbatch_gate.clj](microbatch_gate.clj) | The microbatch topology `microbatch-gate`: for one record, the walk on the layer's task, and then the copy of an accept's rows of facts to every other task. | It owns the four PStates of the microbatch record, durable in the cluster, with the same four schemas. A fact's row and a row of the facts by value are on every task; an answer and a layer's last stamp are on the layer's task alone. It borrows no PState, appends to no depot and reads no clock. Nothing in the memory of a process. |
| [door_reads.clj](door_reads.clj) | The two query topologies, for layers of both depots: the answer by act-id, and the depot of a layer. Each is one read of one row on the layer's task. | None. It borrows the two gates' PStates to read. |

## How the files relate

Both gates decide with one walk. [walk.clj](walk.clj) holds the dataflow of a
decision once: it reads rows, calls pure functions and writes rows. The checks
are [gate/](../gate/README.md)'s, the form of what arrived is
[door/](../door/README.md)'s, and each row and each schema is
[record/](../record/README.md)'s. A decision is made of the thing and of rows
of the calling gate's own record on the task of the call. The stamp is counted
in the act's layer, and the time on the act, which the door wrote, is a label
beside it that no check reads. Neither gate reads a clock or a table of the other gate: for
a layer that starts with the other depot a gate answers from the layer id alone.

Each depot partitions by the thing's layer, so a layer is one partition and its
things have the depot's order. A gate hands the walk each thing of a partition
on the partition's task, which is the layer's task. The walk never gives its
task back between its first read and its last write, so the next thing of the
layer is decided against the rows that this one wrote.

The two gates differ in the kind of topology and in where a fact's row lives.

The streaming gate is a stream topology. An event is the walk and nothing else:
no partitioner, no append and no wait. It does not use what the walk gives back,
so every row of a streaming layer stays on the layer's task. An append to its
depot can wait for the event.

The microbatch gate is a microbatch topology. After the walk it goes to every
task and writes the rows of an accept's facts, and of its facts by value, on
each task but the deciding one. So a read of a microbatch layer's facts needs
one task, whichever it is, and a maker still reads her answer on the layer's
task. A batch is committed on every task or on none, and a second attempt of a
batch makes the same rows. An append to its depot does not wait for the gate.

The reads borrow both gates' PStates, and they write nothing. A query by act-id
or by layer goes straight to the layer's task and reads one row there.

What the gates do not check, and the builders' choices, are in
[the store's map](../README.md).
