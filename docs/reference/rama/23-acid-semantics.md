# ACID Semantics
> Source: https://redplanetlabs.com/docs/~/acid.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# ACID semantics

Atomicity, consistency, isolation, and durability are critical traits of database systems. Rama does considerably more than database systems by integrating computation and storage, so these traits manifest differently in Rama. Instead of requiring clients to specify low-level details such as "uncommitted read" or "repeatable read", Rama automatically provides the ideal semantics for each context. This page explores how Rama provides these traits.

## External and internal views of PStates

PStates are interacted with either from a topology or from a client outside the cluster. When within a topology, you could either be interacting with a PState owned by the same topology, owned by a different topology in the same module, or owned by a topology in a different module. The precise ACID semantics depend on which of these contexts your code is running.

PStates maintain an "internal" and "external" view. How this view is maintained is different between streaming and microbatching, but in both cases the internal view reflects in-progress, unreplicated changes and the external view reflects replicated, durable information. The process of getting changes from the internal view to the external view is called "making changes visible" and is part of how Rama implements stream and microbatch topologies.

A topology specifies how one or more PStates change according to new events. They serve the same purpose that transactions do in databases, except expressed via a general-purpose Turing-complete API rather than a limited DSL. Stream topologies replicate and make visible changes to PStates on an event by event basis, while microbatch topologies make all changes across all partitions visible together. Put another way, stream topologies are transactions for changes on a single partition, while microbatch topologies are cross-partition transactions for every change across all partitions.

The internal view of PStates is only seen by topologies accessing their own PStates. This is intuitive since a topology is an in-progress series of changes, so you should be able to read your own writes before they replicate. If the internal view fails to become the external view, like if the process with the unreplicated changes crashes, it’s completely fine for the internal view to regress back to the previous external view. This is because the only reader of the internal view was the topology responsible for making those changes, not an external process responsible for serving the end consumers of the application.

The external view is always seen by clients outside the cluster or topologies querying PStates they don’t own. This is also intuitive since external consumers of PStates should only ever be seeing replicated information that is guaranteed to never regress to some prior version of the PState.

How all this relates to ACID properties will be explained throughout the rest of this page.

## Concurrency

A traditional database handles many write requests concurrently. There are many coordination mechanisms inside databases to make this safe.

Rama works completely differently with all writes to a PState happening within its owning topology. Rama gets parallelism by partitioning a module and its PStates across many tasks. Tasks live across many threads across many machines, and each task is single-threaded. This means all actions on a task happen in serial.

For both stream and microbatch topologies, the efficiency of applying and replicating writes to PStates comes from batching. As described [in this section](stream.html#_operation), stream topologies batch and replicate writes across many streaming events on the same task at the same time. The topology runner on that task does not move on to the next batch of events until the previous batch has finished replicating its changes.

For microbatch topologies, as described [in this section](microbatch.html#_operation_and_fault_tolerance), all PState changes on a partition for an entire microbatch are made visible at once in the commit phase. This includes applying the changes to disk in batch and replicating those changes in batch.

## Atomicity

Atomicity refers to all the operations on a datastore succeeding or failing together. If any operation fails, none of the changes should be visible. If the operation succeeds, all changes should become visible at the same time. So it should be impossible to see a datastore in an inconsistent state.

Microbatch topologies provide atomicity for the entire microbatch. Since none of the changes become visible until the commit phase, all the computation in a microbatch to all PStates across all partitions become visible together. This property holds no matter how complex and dynamic the computation of a microbatch, such as containing branches, conditionals, loops, and subbatches. As mentioned earlier, this means every microbatch topology is a cross-partition transaction.

Microbatch topologies take this even further by providing an exactly-once guarantee on the results of processing. If there’s a failure during a microbatch, like a machine losing power, the microbatch will retry and the results in PStates will be as if there were no failures at all. This works because a microbatch always resets the internal views of PStates to the result of the previous successful microbatch before beginning computation. This is further described [in this section](microbatch.html#_operation_and_fault_tolerance) on the microbatch topology page.

Stream topologies provide atomicity at the event level. All updates to all PStates within the same event become visible together, and any failure in processing will result in any pending changes being dropped. Depending on how the stream topology is configured, those updates may be retried by processing the depot record again from the start. For example, consider the following stream topology code:

```java
stream.source("*depot").out("*tuple")
      .each(Ops.EXPAND, "*tuple").out("*key", "*value")
      .compoundAgg("$$counts", CompoundAgg.map("*key", Agg.count()))
      .compoundAgg("$$values", CompoundAgg.map("*key", Agg.set("*value")))
      .hashPartition("*value")
      .compoundAgg("$$valueCounts", CompoundAgg.map("*value", Agg.count()));
```

A stream topology event is all code between partitioner calls. There are two events in this code, the first updating `"$$counts"` and `"$$values"` and the second updating `"$$valueCounts"`. Both updates to `"$$counts"` and `"$$values"` are atomic, and it’s impossible for a reader to see those PStates out of sync. Their updates will also become visible before the next event after the partitioner is sent for execution.

Now, take a look at this example with more complicated logic:

```java
stream.source("*depot").out("*data")
      .each(Ops.EXPAND, "*data").out("*k", "*amt")
      .compoundAgg("$$p", CompoundAgg.map("*k", Agg.count()))
      .loopWithVars(LoopVars.var("*i", 0),
        Block.ifTrue(new Expr(Ops.LESS_THAN, "*i", "*amt"),
          Block.compoundAgg("$$p2", CompoundAgg.map("*k", CompoundAgg.map("*i", Agg.count())))
               .hashPartition("*i")
               .compoundAgg("$$p3", CompoundAgg.map("*i", Agg.count()))
               .continueLoop(new Expr(Ops.INC, "*i"))
        ));
```

This example is contrived for the purpose of illustrating how PState updates map to events. The loop in this code has a dynamic number of iterations depending on the value of `"*amt"` in the incoming data. The partitioner inside the loop causes the code to execute as multiple events.

Since there’s no partitioner in between the update to `"$$p"` and the first update to `"$$p2"`, the first event atomically applies those two updates on that partition. The `hashPartition` call then causes another event to begin after those updates finish replicating. Since there’s no partitioner in between the update to `"$$p3"` and the subsequent update to `"$$p2"`, the next event atomically applies the updates to those two PStates on that partition. That those two updates are in different loop iterations is irrelevant, as the end of the first loop iteration and the beginning of the next loop iteration are part of the same event. Every event after that has the same structure with one update to `"$$p3"` and one update to `"$$p2"`.

These examples are only showing two PState updates happening atomically, but it should be clear that any number of updates to any number of PStates within the same event will be atomic.

## Consistency

Consistency refers to only valid data being written into a datastore. Data written should not violate any constraints defined in the definition of the datastore.

Rama provides very strong consistency guarantees:

- Any writes violating a [PState’s schema](pstates.html#_schema_validation) are rejected. Since a PState is an arbitrary combination of data structures containing arbitrary objects, these schemas can be very detailed.
- The atomicity of PState writes, as described in the previous section, ensures that application-level constraints between PStates are never violated. For stream topologies, you can ensure this for colocated PStates on the same task, and for microbatch topologies you can ensure this for all PStates across all tasks.
- Replication ensures reads on PStates will never regress, even if failures cause a new replica to take leadership for a task. You’ll never read an earlier version of data from a PState than you’ve already read.

## Isolation

Isolation refers to the degree transactions are isolated from each other and the consistency of data they access. With a database, you often have different "isolation levels" you can select to provide different guarantees, such as:

- **Read uncommitted**: Transactions can read data that has been modified but not committed by other transactions, which can lead to dirty reads, non-repeatable reads, and phantom reads.
- **Read committed**: Transactions only read data that has been committed by other transactions. This prevents dirty reads but allows non-repeatable reads and phantom reads.
- **Repeatable read**: Any data read in a transaction is guaranteed to remain unchanged throughout the transaction. This prevents non-repeatable reads but still allows phantom reads, as new data can be inserted by other transactions during the duration.
- **Serializable**: Transactions are executed serially, preventing dirty, non-repeatable, and phantom reads.

Isolation levels in a database are a tradeoff between semantics and performance, with stronger isolation levels requiring more coordination and thus limiting concurrency and hurting performance.

The need for isolation levels are an artifact of how databases work by concurrently executing many independent transactions at the same time. As described in the [section on concurrency](#_concurrency) above, Rama works completely differently due to how it colocates computation with storage and how it achieves parallelism through partitioning. Because of this, Rama provides the ideal semantics for each context and you never need to specify isolation levels.

In stream topologies, a batch of streaming events is executed on a task. In microbatch topologies, a batch of data across all depot partitions is executed at the same time. In both streaming and microbatching, Rama does not move on to the next batch until the previous batch has finished executing and changes to all PStates have replicated and been made visible. You can think of each batch as being a set of transactions happening together, with the size of batches dynamically adjusting according to incoming load.

When reading PStates owned by the same topology as that which is executing, the semantics are similar to "read uncommitted". This is desirable since the topology code is the transaction that’s executing, so it just means you can read your own writes. No other batches are happening concurrently.

When reading PStates owned by a different topology in the same module, you have similar semantics to "serializable" for reads in the same event. This is due to all computation and storage for a module being colocated.

When reading PStates owned by different modules, you get similar semantics to "read committed". This is because those PStates exist in different processes that could be on different nodes. Part of designing Rama modules is determining which PStates should be colocated, and read semantics can be a consideration for those design decisions.

The way Rama batches events serves the same purpose as concurrent transactions in a database. Rama’s performance for PState writes is as good as any database while eliminating the need to tune isolation levels. This is a major simplification since isolation levels can be complex to reason about.

## Durability

Rama provides an extremely strong durability guarantee for all PState and depot updates.

Writes to PStates and depots are not made visible until they’re durable on disk on the leader and durable on disk on all ISR ("in-sync replica") followers. This is what guarantees that reads on a PState never regress. See [the page on replication](replication.html) for more details on how that works.

## Summary

Rama provides strong ACID guarantees that are as good as any database while also requiring less tuning. A "transaction" is implicit in Rama code, with each event being a transaction for a stream topology and the entire microbatch being a transaction for a microbatch topology. That these "transactions" are programmed with a general-purpose Turing-complete dataflow API that can utilize arbitrary Java or Clojure code at any point is another major difference with databases that has profound implications beyond ACID semantics.