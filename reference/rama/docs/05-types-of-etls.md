# Tutorial 5: Types of ETLs
> Source: https://redplanetlabs.com/docs/~/tutorial5.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Types of ETLs

So far you’ve explored the core concepts of using Rama, but all the examples shown have only made use of stream topologies. Rama has another kind of ETL called microbatch topologies which is more appropriate for many cases. In this section you’ll see more detail on both stream and microbatch topologies and when you would prefer one to the other.

## Stream topologies

Stream topologies process data as it comes in. Data is processed from a depot partition in the order in which they were appended, but otherwise the processing of data is independent. Let’s take a look at our `SimpleWordCountModule` definition again:

```java
public void define(Setup setup, Topologies topologies) {
  setup.declareDepot("*depot", Depot.random());
  StreamTopology s = topologies.stream("s");
  s.pstate("$$wordCounts", PState.mapSchema(Object.class, Object.class));

  s.source("*depot").out("*token")
   .hashPartition("*token")
   .compoundAgg("$$wordCounts", CompoundAgg.map("*token", Agg.count()));
}
```

Suppose this module is running as two tasks, and suppose the depot partitions receive the following appends:

- **Task 0:** "a", "b"
- **Task 1:** "c", "d"

In this case, "a" will definitely begin processing before "b", but the relationship between the data processed by each task is independent. So "c" could be processed before "a" or it could be processed after "b".

On the other hand, there’s no guarantee that the processing for "a" will finish before "b" because of the partitioner in this topology. If "a" and "b" partition to different tasks, it’s possible "b" will finish first even though it was sent after "a". Once they go to different tasks, the processing is independent and concurrent.

Rama keeps track of which records have successfully finished processing for each stream topology. Even though the processing of a record can trigger a dynamic amount of downstream computation on many other tasks, Rama efficiently detects when processing has succeeded, failed (e.g. due to an exception), or timed out. Rama will retry failed records according to the *retry policy* configured for the topology. For more details on how retries work and other features of streaming, see the [page on stream topologies](stream.html).

Stream topologies integrate with depot appends, allowing you to coordinate client-side code with the completion of processing associated with depot appends. When you do a depot append like the following code, the append blocks until processing of all stream topologies colocated with the depot finish processing the data.

```java
depot.append("a");
```

There’s also a non-blocking version of `append` that returns a [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) which asynchronously notifies you of completion:

```java
depot.appendAsync("a");
```

The implementation of stream topologies uses its tracking of downstream computation to efficiently notify waiting depot clients about success or failure of processing.

Note that depot appends don’t have to wait for colocated stream topologies to process. There are other variants of `append` and `appendAsync` that let you specify different behavior. This is discussed in more detail in the [page about depots](depots.html).

## Microbatch topologies

Let’s take a look at how our word count module looks when implemented with a microbatch topology instead of a stream topology:

```java
public void define(Setup setup, Topologies topologies) {
  setup.declareDepot("*depot", Depot.random());
  MicrobatchTopology mb = topologies.microbatch("mb");
  mb.pstate("$$wordCounts", PState.mapSchema(Object.class, Object.class));

  mb.source("*depot").out("*microbatch")
    .explodeMicrobatch("*microbatch").out("*token")
    .hashPartition("*token")
    .compoundAgg("$$wordCounts", CompoundAgg.map("*token", Agg.count()));
}
```

This looks almost the same as the stream version. The main difference is the addition of the `explodeMicrobatch("*microbatch")` call at the beginning of processing.

Whereas stream topologies process data immediately as it comes in, with each depot partition independently emitting records for processing, microbatch topologies process accumulated data across all partitions at the same time in a single coordinated batch computation. Internally, Rama runs a loop for each microbatch topology that triggers processing of all unprocessed data across all partitions. This means each microbatch iteration processes all data accumulated during the previous iteration.

At the start of a microbatch topology, processing is always on task 0. `explodeMicrobatch` emits every piece of data in this microbatch across all tasks, and the code after `explodeMicrobatch` runs in parallel across all partitions (that emitted data for the microbatch). From there, the code can be written exactly the same as a stream topology.

There are many use cases where you would call `explodeMicrobatch` multiple times for a single `source`, usually in conjunction with additional features of microbatching that provide increased computational expressiveness. These features include subbatch processing, two-phase aggregation, temporary PStates, and more. See [the page on microbatch topologies](microbatch.html) for more details.

A microbatch iteration can take anywhere from a couple hundred milliseconds to many seconds, depending on the complexity of processing and the amount of incoming data. But because there’s so much less overhead per record of data, due to everything being batched together, microbatch topologies can handle much higher throughput than stream topologies.

Another advantage of microbatch topologies is its simple fault-tolerance semantics: microbatch topologies guarantee exactly-once semantics for the results of processing into PStates regardless of failures and retries. You can read more about this and other details of microbatching on [this page](microbatch.html).

Lastly, unlike stream topologies microbatch topologies do not integrate with depot appends. It wouldn’t be desirable anyway due to the much higher latency of processing with microbatching.

## Summary of tradeoffs

Here is a table summarizing the tradeoffs between stream and microbatch topologies:

| Type | Latency | Throughput | Fault-tolerance | Depot append integration |
| --- | --- | --- | --- | --- |
| Stream | Few millis | Medium | At-least once or at-most once | Yes |
| Microbatch | Hundreds of millis | High | Exactly-once | No |

## Summary

Rama’s ETL types give you the computational primitives to tackle any realtime use case with scalability, performance, and fault-tolerance built-in. In the [next section](tutorial6.html), you’ll combine your newfound knowledge with the information from previous sections to build a real project end-to-end.