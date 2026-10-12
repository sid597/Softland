# Module — what runs in Rama

[Parent: store](../README.md) · [Store source entry](../../../README.md).

This folder holds the topologies of the store's module: the two gates, each
with its depot of answers, the
topology that keeps the clock of each batch of the microbatch gate, and the
two reads by which the door reads the module. Each file requires Rama.
[module.clj](../module.clj), one folder up, declares the module `Store`, its
two depots of acts and its depot of batch clocks, and calls each of these files
to add itself. The module has five depots: those three, and the depot of
answers that each gate declares for itself.

| File | Responsibility | State and lifetime |
|---|---|---|
| [streaming_gate.clj](streaming_gate.clj) | The stream topology `streaming-gate`, the dataflow of one event, and the gate's depot of answers. | It owns the four PStates of the streaming record, durable in the cluster: the answers, the facts, the facts by value, and what it keeps for a task. It declares the depot `*streaming-answer-depot` and alone appends to it: one record at the end of each event. It borrows two PStates of the microbatch gate to read. Nothing in the memory of a process. |
| [microbatch_gate.clj](microbatch_gate.clj) | The microbatch topology `microbatch-gate`, the dataflow of one batch, in four steps, and the gate's depot of answers. | It owns the four PStates of the microbatch record, durable in the cluster: the answers, the facts, the facts by value, and what it keeps for a task, which holds what a batch decided until the next run appends it. It declares the depot `*microbatch-answer-depot` and alone appends to it: the run that closes a batch appends what that batch decided. It borrows one PState of the streaming gate to read, and the kept clock of [batch_clock.clj](batch_clock.clj) to read on the gate's task. It appends to one depot more, the depot of batch clocks: one record for a batch with things to decide. Three temporary PStates of Rama live for one attempt of one batch. Nothing in the memory of a process. |
| [batch_clock.clj](batch_clock.clj) | The stream topology `batch-clock` and the event of one record of the depot of batch clocks: it keeps the clock of the newest batch of the microbatch gate, first write wins, so that a second attempt of a batch takes the clock of the first. | It owns one PState, durable in the cluster: one row, the newest batch's clock. Nothing in the memory of a process. |
| [door_reads.clj](door_reads.clj) | The two query topologies, for layers of both depots: the answer by act-id, and the depot of a layer. | None. It borrows the two gates' PStates to read. |

## How the files relate

Each gate's dataflow reads rows, calls pure functions, writes rows, and appends
to its depot of answers what it decided. The streaming gate appends the entry
of an event at the event's end, behind the commit of its writes. The
microbatch gate puts each entry into a table of its own in the attempt that
writes the answer's row, and the next run of the gate, which closes that batch,
appends it. The microbatch gate's dataflow appends in one place more, for the
clock of a batch: when
none is kept for the batch it reads the clock, appends one record to the depot
of batch clocks, waits until [batch_clock.clj](batch_clock.clj) has kept it, and
throws in the one case where the clock is then not kept. So each attempt of a
batch takes one clock, and no stamp is made from a clock that is not kept. The
checks are [gate/](../gate/README.md)'s, the form of what arrived is
[door/](../door/README.md)'s, and each row, each entry of a depot of answers
and each schema is [record/](../record/README.md)'s.

Each gate reads one thing of the other gate's record: whether it holds a layer
of which its own record holds nothing. The reads borrow both gates' PStates, and
they write nothing. No file here reads a depot of answers: no topology has a
source on one, and no query opens one.

The walk of a thing through each gate, what the gates do not check, and the
builders' choices are in [the store's map](../README.md).
