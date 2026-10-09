# Module — what runs in Rama

[Parent: store](../README.md) · [Store source entry](../../../README.md).

This folder holds the topologies of the store's module: the two gates, and the
three reads by which the door reads the module. Each file requires Rama.
[module.clj](../module.clj), one folder up, declares the module `Store` and its
two depots, and calls each of these files to add itself.

| File | Responsibility | State and lifetime |
|---|---|---|
| [streaming_gate.clj](streaming_gate.clj) | The stream topology `streaming-gate` and the dataflow of one event. | It owns the four PStates of the streaming record, durable in the cluster. It borrows two PStates of the microbatch gate to read. Nothing in the memory of a process. |
| [microbatch_gate.clj](microbatch_gate.clj) | The microbatch topology `microbatch-gate` and the dataflow of one batch, in four steps. | It owns the four PStates of the microbatch record, durable in the cluster. It borrows one PState of the streaming gate to read. Three temporary PStates of Rama live for one attempt of one batch. Nothing in the memory of a process. |
| [door_reads.clj](door_reads.clj) | The three query topologies, for layers of both depots: the answer by act-id, the locks of a request, the depot of a layer. | None. It borrows the two gates' PStates to read. |

## How the files relate

Each gate's dataflow only reads rows, calls pure functions and writes rows. The
checks are [gate/](../gate/README.md)'s, the form of what arrived is
[door/](../door/README.md)'s, and each row and each schema is
[record/](../record/README.md)'s.

Each gate reads one thing of the other gate's record: whether it holds a layer
of which its own record holds nothing. The reads borrow both gates' PStates, and
they write nothing.

The walk of a thing through each gate, what the gates do not check, and the
builders' choices are in [the store's map](../README.md).
