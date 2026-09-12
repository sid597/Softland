# Terminology
> Source: https://redplanetlabs.com/docs/~/terminology.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Terminology

This page summarizes the main terminology you’ll see throughout the documentation and when using Rama. This page is meant to be used as a quick reference, while other pages explain the concepts listed here in much greater detail.

## Cluster

A Rama cluster is a collection of machines which work together to execute Rama applications (called "modules"). A cluster consists of three different daemon processes: one "Conductor" process, any number of "Metastore" processes, and any number of "Supervisor" processes.

All these processes could be run on the same machine, or they could be spread across multiple machines.

Clusters can be simulated in a single process for the purposes of unit testing or experimentation using [InProcessCluster](testing.html). An `InProcessCluster` launches threads for each worker rather than separate processes.

## Conductor

The Conductor orchestrates launches of modules as well as other module operations like updates and scaling. The Conductor also serves a browser-based Cluster UI showing status of modules. When the [Monitoring module](operating-rama.html#_activating_self_monitoring) is deployed, the Cluster UI shows a wide variety of detailed time-series telemetry on modules and processes. The Conductor is not involved in any data processing or PState querying.

## Metastore

The Metastore stores cluster metadata such as which nodes should be running which module workers, replication coordination information (e.g. leadership election), and [dynamic options](operating-rama.html#_worker_configurations_and_dynamic_options). All Metastore state is stored on a [Zookeeper cluster](https://zookeeper.apache.org/).

## Supervisor

Every Worker node runs a process called the "Supervisor". A Supervisor watches the Metastore for changes to module assignments and starts/stops worker processes running on its machine as dictated by Conductor.

## Worker

A Worker, different than "Worker node", is a process launched by a Supervisor to run part of a module. A Worker runs many "tasks" across some number of "task threads".

## Module

A "module" is an application deployed on a Rama cluster. A module consists of any number of depots, PStates, ETL topologies, and query topologies. These all run colocated inside the same set of processes / threads across potentially many nodes. A module can be updated to provide new code as well as adding or removing depots, PStates, and topologies. A module can also be scaled up or down by issuing a command to the Conductor to orchestrate the process.

## Module instance

A "module instance" is a specific deployment of a module associated with a specific set of code. During module transitions (updates or scaling), multiple module instances will exist for the same module and Rama will orchestrate handoff of responsibilities between them (e.g. depot appends, PState queries, etc.).

## Task

A module is composed of some number of tasks, each of which runs the exact same module code. A task contains partitions for PStates and depots. A "task" is replicated across multiple "task instances". The number of tasks in a module must be a power of two. The "Task model" is described in more detail [here](tutorial3.html#task-model).

## Task instance

A "task instance" is one replica of a "task". An event is a discrete unit of computation that runs on a "task instance" and can access any or all of the PState and depot partitions on that task instance.

When the phrase "run on a task" is used, it more precisely means "run on a task instance". Usually this refers specifically to "running on a leader task instance".

## Task group / task thread

A "task instance" lives on a "task thread". Events run on a task instance execute on its corresponding task thread with the context set to access the depots/PStates for that particular task instance. A single "task thread" can run many task instances. The collection of tasks run on a "task thread" is called a "task group".

Replication happens at the "task group" level. So if a task group contains tasks 0, 12, and 24 and the replication factor is three, then there will be three task threads around the cluster each with that same task group.

## Category queue

A task thread pulls events to run off of a "category queue". The categories are "isr.replication", "client", "low.latency", "flex.latency", and "mirror.client", and a category queue contains a [FIFO](https://en.wikipedia.org/wiki/FIFO_(computing_and_electronics)) queue for each category. Task threads measure how much time has been spent running events of each category and choose which category to run next to balance the amount of time spent running each category. The desired proportions of time for each category can be configured with the dynamic option `worker.event.category.proportions`. This option is only relevant when there is CPU contention.

The "isr.replication" category runs critical replication-related events for communicating between leaders and followers in the [ISR](#_isr). The "client" category runs depot appends, PState client queries, query topologies, and other client-initiated tasks. The "low.latency" category runs stream topologies. The "flex.latency" category runs microbatch topologies. The "mirror.client" category is used for operations originating from other modules, like PState queries, depot appends, and query topology invokes.

## Depot

A [depot](depots.html) is a distributed, replicated, unindexed log of data. Depots are consumed by ETL topologies to produce PStates.

## PState

A [PState](pstates.html) is a distributed, durable, replicated datastore of arbitrary shape. Partitions of a PState can range from simple values (e.g. numbers, strings) to deeply nested combinations of maps, sets, and lists. The core API for querying and updating PStates is [Paths](paths.html).

## Topology

A topology is a [dataflow-based](tutorial4.html) distributed computation. ETL topologies source data from depots to produce PStates. Rama has two kinds of ETL topologies: [stream topologies](stream.html) and [microbatch topologies](microbatch.html).

The other kind of topology is called a [query topology](query.html). Query topologies are on-demand, realtime, distributed computations defined as part of a module. As part of aggregating their results, they can look at any or all PStates in a module and any or all of the partitions of those PStates.

## Partition

A partition is one part of a depot or PState living on a task. Depots and PStates can either be global, in which case they have a single partition on task 0, or non-global, in which case they have a partition on every task of their owning module. Events on tasks can access colocated depot and PState partitions.

## Leaders and followers

The leader replica for a task group manages and coordinates all computation for its constituent tasks. Things like ETL logic, PState queries, and depot appends always run on the leader. Leader and follower replicas communicate with each other with more events to keep the follower replicas in sync with the leader.

If a leader dies for any reason (e.g. machine loses power), a follower that’s in-sync will take over as the new leader. All clients and modules communicating with that task group will automatically detect the change and switch communication to the new leader.

## Replication factor

The replication factor is a config that can be set when deploying a module. It sets how many replicas will exist for each task group. A replication factor of three will mean each task group has one leader and two followers. The default replication factor is one, but the recommended replication factor for production deployments is three.

## Replog

Replog is short for "replication log". The replog is an internal data structure on each task thread in Rama at the center of how replication works. All changes to depots and PStates are appended to the replog as "replog entries". Leaders manage forwarding replog entries to followers and keeping track of which followers need which entries.

## ISR

ISR stands for "In-sync replicas". These are replicas that are up to date with the leader of a task group, and the ISR set is tracked at the task group level. A leader is always part of the ISR set. Changes to PStates are made visible and acks are sent back for depot appends only when all members of an ISR set have the corresponding replog entry for those changes. This ensures that any client-visible changes persist across leader switches.

## OSR

OSR stands for "Out-of-sync replicas". These are replicas that are not up to date with the leader of a task group. OSR members are not eligible to become leaders. Replicas in the OSR work to become in sync with the leader through a combination of fetching missing replog entries and transferring files from the leader.

## Min-ISR

Min-ISR can be set with the [dynamic option](operating-rama.html#_worker_configurations_and_dynamic_options) `replication.min.isr`. It defaults to one less than the replication factor for a module. Attempts to change depots or PStates get an exception if Rama is unable to replicate that change to at least min-ISR replicas (including the leader). See [the page on replication](replication.html) for more details.

## Mirror

A [mirror](module-dependencies.html) is a reference in a module to a depot, PState, or query topology from another module.

## Foreign context

When the term "foreign" is used, it refers to Rama work initiated from outside of modules. This includes depot appends, PState client queries, and query topology invokes.

## WORP

WORP stands for "Worker Rama Protocol". This is the internal messaging layer used within Rama to communicate with and between task threads. WORP is used by workers and foreign clients. Each worker runs a single WORP server, and each worker and foreign client has a single WORP client shared across all threads.

## WEFT

WEFT stands for "Worker Efficient File Transfer". This is an internal protocol for transferring files from one machine to another. Each Supervisor internally runs an HTTP-based WEFT server, and each worker has a multi-threaded WEFT client. WEFT is used when workers need to synchronize data from scratch, such as during scaling operations or far-behind followers catching up to their leader.