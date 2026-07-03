# Integrating With Other Tools
> Source: https://redplanetlabs.com/docs/~/integrating.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Integrating with other tools

Rama makes it easy to efficiently integrate with other tools, whether queues, databases, or other systems. If the system has a Java API, you’ll be able to interact with it from Rama modules with very little code.

The open-source library [rama-kafka](https://github.com/redplanetlabs/rama-kafka) integrates Rama with external [Apache Kafka](https://kafka.apache.org/) clusters and is a good example of an integration implemented using the information on this page.

On this page you will learn:

- Using Rama’s "task global" API to manage the lifecycle of creating and closing connections to external systems
- Using "ticks" to execute code periodically for an integration
- Using `eachAsync` to efficiently issue remote calls within a topology
- Making an "external depot" to use an external system as a source of data for a topology
- How Rama implements backpressure for external depots
- Using `Ops.CURRENT_MICROBATCH_ID` to achieve exactly-once semantics for updates to external systems

## Task globals

The first step to integrating with an external system is creating a client to those systems that’s accessible to Rama topology code. If the client for an external system is thread-safe, then you may only need one client for each worker process. If it’s not thread-safe, then you’ll need a client for every task thread. Additionally, because you’ll need to unit test your modules against these external systems, you need lifecycle hooks to shut those clients down when your modules running on [InProcessCluster](testing.html) are shut down.

Rama’s "task global" API satisfies all these requirements in a simple way. A task global is an object associated with a var that’s accessible on every task of a module. Additionally, they can optionally specialize their contents for each task. Task globals are created using the `declareObject` method when declaring a module. Let’s take a look at an example:

```java
public class BasicTaskGlobalModule implements RamaModule {
  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.declareObject("*globalValue", 7);
    setup.declareDepot("*depot", Depot.random());

    StreamTopology s = topologies.stream("s");
    s.source("*depot")
     .each(Ops.PRINTLN, "Task", new Expr(Ops.CURRENT_TASK_ID), "->", "*globalValue")
     .shufflePartition()
     .each(Ops.PRINTLN, "Task", new Expr(Ops.CURRENT_TASK_ID), "->", "*globalValue");
  }

  public static void main(String[] args) throws Exception {
    try(InProcessCluster cluster = InProcessCluster.create()) {
      RamaModule module = new BasicTaskGlobalModule();
      cluster.launchModule(module, new LaunchConfig(4, 4));
      String moduleName = module.getClass().getName();

      Depot depot = cluster.clusterDepot(moduleName, "*depot");
      depot.append(null);
    }
  }
}
```

This module declares the var `"*globalValue"` with the value `7`. The topology prints the value of that object on the initial task, partitions to a random task using `shufflePartition()`, and then prints the value on the new task. Running the main method performs one depot append and prints:

```java
Task 0 -> 7
Task 2 -> 7
```

The critical aspect of task globals, such as `"*globalValue"` in this example, is a copy exists on every task. It is a value accessible to each task without having to transfer it from any other task or node.

Rama allows you to take things a step further by specializing the value of a task global for each task. For integrating with external systems, like databases, this is necessary since tasks can live across multiple worker processes and each process, thread, or task would need its own connection.

You can specialize a task global’s value by implementing the [TaskGlobalObject](https://redplanetlabs.com/javadoc/com/rpl/rama/integration/TaskGlobalObject.html) interface. Let’s take a look at an example to see how this interface works:

```java
public class SpecializedTaskGlobalModule implements RamaModule {
  public static class MyTaskGlobal implements TaskGlobalObject {
    int _v;
    public int special;

    public MyTaskGlobal(int v) {
      _v = v;
    }

    @Override
    public void prepareForTask(int taskId, TaskGlobalContext context) {
      this.special = taskId * _v;
    }

    @Override
    public void close() throws IOException {

    }
  }

  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.declareObject("*tg", new MyTaskGlobal(10));
    setup.declareDepot("*depot", Depot.random());

    StreamTopology s = topologies.stream("s");
    s.source("*depot")
     .each(Ops.CURRENT_TASK_ID).out("*taskId1")
     .each((MyTaskGlobal mtg) -> mtg.special, "*tg").out("*special1")
     .shufflePartition()
     .each(Ops.CURRENT_TASK_ID).out("*taskId2")
     .each((MyTaskGlobal mtg) -> mtg.special, "*tg").out("*special2")
     .each(Ops.PRINTLN, "Results:", "*taskId1", "->", "*special1", ",", "*taskId2", "->", "*special2");
  }

  public static void main(String[] args) throws Exception {
    try(InProcessCluster cluster = InProcessCluster.create()) {
      RamaModule module = new SpecializedTaskGlobalModule();
      cluster.launchModule(module, new LaunchConfig(8, 4));
      String moduleName = module.getClass().getName();

      Depot depot = cluster.clusterDepot(moduleName, "*depot");
      depot.append(null);
    }
  }
}
```

When `declareObject` is called with an object implementing `TaskGlobalObject`, Rama doesn’t just send a copy of that object to each task. It additionally calls the `prepareForTask` method on each instance of that object on each task during worker startup. `prepareForTask` acts like a second constructor for the object.

In this example, `MyTaskGlobal` has two fields: `_v` and `special`. `_v` is set in the constructor when the object is initially constructed during declaration of the module. When the module is deployed, Rama uses Java serialization to send that object to every task on every worker. The object is deserialized for every task so that each task has a unique instance of the object. After deserialization the field `_v` is set and the field `special` is `null`.

Rama then calls `prepareForTask` on the object to allow it to specialize its value. `prepareForTask` receives as arguments information about the context for which the object is being prepared: the task ID and a [TaskGlobalContext](https://redplanetlabs.com/javadoc/com/rpl/rama/integration/TaskGlobalContext.html) object containing information about the [task thread](terminology.html#_task_group_task_thread) and module. In this example the field `special` is set to be a combination of the parameterized value `_v` and the task ID.

The module collects the value of `special` on two different tasks and prints them for each depot append. Running the `main` method prints:

```java
Results: 4 -> 40 , 2 -> 20
```

You can also see from the definition of `MyTaskGlobal` that the `close` method is required. This is used to clean up any resources opened by the task global when the worker is being shut down, such as database connections. This is most important in unit test scenarios where you’ll be launching and tearing down many `InProcessCluster` instances and don’t want resources leaking between tests.

### Ticks on task global objects

In some cases, you may want your task global object to execute code on a regular basis. For example, you may have a task global that collects monitoring data for an external system and you want to flush accumulated monitoring data every 60 seconds. You can accomplish this with task globals by implementing the `TaskGlobalObjectWithTick` interface. This interface extends `TaskGlobalObject` with two additional methods. Here’s an example:

```java
public class TickedTaskGlobalExample implements TaskGlobalObjectWithTick {
  Integer _taskId;
  int _tick = 0;

  @Override
  public void prepareForTask(int taskId, TaskGlobalContext context) {
    _taskId = taskId;
  }

  @Override
  public long getFrequencyMillis() {
    return 30000;
  }

  @Override
  public void tick() {
    _tick++;
    System.out.println("Tick " + _tick + " on " + _taskId);
  }

  @Override
  public void close() throws IOException {

  }
}
```

`getFrequencyMillis()` returns the tick frequency and `tick()` implements the code to run on each tick. Every 30 seconds this task global will print out how many times it ticked and on which task it lives.

### Sharing resources between task global instances

You may want to share resources between multiple instances of a task global across multiple tasks. For example, you may be integrating with a database that provides a thread-safe client and only want one client created per process. Or the client is not thread-safe but you only want one client for each [task thread](terminology.html#_task_group_task_thread). The `TaskGlobalContext` argument to `prepareForTask` provides the information needed to accomplish this.

The basic technique to share resources is to store those shared resources in a static map on the task global class. During `prepareForTask`, the task global checks to see if the desired resource already exists and creates it if not. Whatever task creates the resource remembers that so it can be responsible for closing the resource and removing it from the static map in the `close()` method. Let’s look at an example of this technique in action to share a resource on a per-thread basis:

```java
public class TaskThreadSharedResourceTaskGlobal implements TaskGlobalObject {
  static ConcurrentHashMap<List, Closeable> sharedResources = new ConcurrentHashMap<>();

  String _connectionString;
  List _resourceId;
  Closeable _resource;
  boolean _owner = false;

  private Closeable makeResource() {
    // stubbed out for illustration purposes
    return () -> { };
  }


  public TaskThreadSharedResourceTaskGlobal(String connectionString) {
    _connectionString = connectionString;
  }

  @Override
  public void prepareForTask(int taskId, TaskGlobalContext context) {
    int taskThreadId = Collections.min(context.getTaskGroup());
    _resourceId = Arrays.asList(context.getModuleInstanceInfo().getModuleInstanceId(),
                                taskThreadId,
                                _connectionString);
    if(sharedResources.containsKey(_resourceId)) {
      _resource = sharedResources.get(_resourceId);
    } else {
      _resource = makeResource();
      sharedResources.put(_resourceId, _resource);
      _owner = true;
    }
  }

  @Override
  public void close() throws IOException {
    if(_owner) {
      _resource.close();
      sharedResources.remove(_resourceId);
    }
  }
}
```

The `List` keys in the static map uniquely identify each resource. In this case the list contains: the [module instance ID](terminology.html#_module_instance), the lowest task ID on the task thread, and the connection string. Module instance IDs are unique across all modules, and the reason they’re needed here are so this code can be run in a test environment using `InProcessCluster` where you may be running many modules all in the same process. Using the lowest task ID identifies each task as being on the same task thread, and the connection string identifies this particular resource. So if you wanted a task global representing a SQL database, the connection string can serve as the identifier so you can have multiple task globals of the same type targeting different SQL databases.

The code keeps track of which task creates the resource and makes it responsible for closing it in the `close()` method. Because all the tasks in a task thread share the thread, any code they run is serial to one another. So no synchronization is needed between them. If you wanted to have one shared resource for all tasks in the process, those different task threads would be intializing concurrently and you would need additional synchronization to make sure the resource is only created by a single task (e.g. with a lock).

[rama-kafka](https://github.com/redplanetlabs/rama-kafka) is an example of an integration that has a per-thread shared resource as well as a per-process shared resource (because [KafkaConsumer](https://javadoc.io/static/org.apache.kafka/kafka-clients/3.2.3/org/apache/kafka/clients/consumer/KafkaConsumer.html) is not thread-safe and [KafkaProducer](https://javadoc.io/static/org.apache.kafka/kafka-clients/3.2.3/org/apache/kafka/clients/producer/KafkaProducer.html) is thread-safe).

## Issuing remote calls within a topology with eachAsync

When writing a topology, it’s critical you never block a task thread by waiting on another thread or process. A task thread is used for depot appends, other topologies, PState queries, and internal system tasks. Blocking the task thread would block all those other functions from being carried out. So when you need to interact with a system running somewhere else, Rama provides the method [eachAsync](https://redplanetlabs.com/javadoc/com/rpl/rama/Block.html#eachAsync-com.rpl.rama.ops.RamaFunction0-) to do so in an efficient, non-blocking way.

`eachAsync` works just like [each](https://redplanetlabs.com/javadoc/com/rpl/rama/Block.html#each-com.rpl.rama.ops.RamaFunction0-) except the provided function returns a [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) instead of a value. When the `CompletableFuture` is delivered, it will emit the delivered value asynchronusly to `out` on the `eachAsync` call within the topology context. Here’s an example:

```java
public class EachAsyncModule implements RamaModule {
  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.declareDepot("*depot", Depot.random());

    StreamTopology s = topologies.stream("s");
    s.source("*depot")
     .eachAsync(() -> {
       CompletableFuture ret = new CompletableFuture();
       ExecutorService es = Executors.newSingleThreadExecutor();
       es.submit(() -> {
         ret.complete("ABCDE");
         es.shutdown();
       });
       return ret;
     }).out("*v")
     .each(Ops.PRINTLN, "Result:", "*v");
  }

  public static void main(String[] args) throws Exception {
    try(InProcessCluster cluster = InProcessCluster.create()) {
      RamaModule module = new EachAsyncModule();
      cluster.launchModule(module, new LaunchConfig(4, 4));
      String moduleName = module.getClass().getName();

      Depot depot = cluster.clusterDepot(moduleName, "*depot");
      depot.append(null);
    }
  }
}
```

Running this prints:

```java
Result: ABCDE
```

In this code the `eachAsync` call returns a `CompletableFuture` that is delivered on another thread. As you can see the output var `"*v"` is bound to the value delivered to the `CompletableFuture` and not to the `CompletableFuture` itself. `eachAsync` efficiently ties the success/failure of the topology to the success/failure of the asynchronous call (which may be doing a remote call to an external system). If the call succeeds, the value is delivered and the topology proceeds with whatever is after the `eachAsync` call. If the call fails by delivering an exception to the `CompletableFuture`, the topology will detect that failure and retry if appropriate.

`eachAsync` isn’t specifically tied to task globals. It’s a general facility for performing async computation in a topology. When used with a task global, like one representing a database, you would pass that task global in as an argument and your `eachAsync` function can grab whatever client interface it needs from the task global.

Ideally, the client library for the external system you are integrating with already has a non-blocking interface. In this case it’s easy to call that non-blocking API within an `eachAsync` call and return the result as a `CompletableFuture`. If the system only has a blocking API, then you have to set up a thread to perform those calls in the background. That thread can be set up and managed as part of a task global. [rama-kafka](https://github.com/redplanetlabs/rama-kafka) utilizes this technique for calls to [KafkaConsumer](https://javadoc.io/static/org.apache.kafka/kafka-clients/3.2.3/org/apache/kafka/clients/consumer/KafkaConsumer.html) since that only has a blocking API.

## External depots

Rama provides the interface [ExternalDepot](https://redplanetlabs.com/javadoc/com/rpl/rama/integration/ExternalDepot.html) for making task globals which can be used as sources for topologies. External depots work exactly like regular depots. All depot options are available and retries in case of downstream processing failure work the same. Additionally, for stream topologies Rama implements backpressure and will stop polling for new data if consuming topologies aren’t keeping up.

Let’s take a look at the `ExternalDepot` interface:

```java
public interface ExternalDepot extends TaskGlobalObject {
  CompletableFuture<Integer> getNumPartitions();
  CompletableFuture<Long> startOffset(int partitionIndex);
  CompletableFuture<Long> endOffset(int partitionIndex);
  CompletableFuture<Long> offsetAfterTimestampMillis(int partitionIndex, long millis);
  CompletableFuture<List> fetchFrom(int partitionIndex, long startOffset, long endOffset);
  CompletableFuture<List> fetchFrom(int partitionIndex, long startOffset);
}
```

Before diving into the interface, let’s take a look at an example of usage. This example makes use of the `rama-kafka` library which implements `ExternalDepot` for [Apache Kafka](https://kafka.apache.org/).

```java
public class KafkaExternalDepotExampleModule implements RamaModule {
  @Override
  public void define(Setup setup, Topologies topologies) {
    Map<String, Object> kafkaConfig = new HashMap<>();
    kafkaConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1.mycompany.com:9092,kafka2.mycompany.com:9092");
    setup.declareObject("*kafka", new KafkaExternalDepot(kafkaConfig, "myTopic"));

    StreamTopology s = topologies.stream("s");
    s.source("*kafka").out("*record")
     .each(Ops.PRINTLN, "Kafka record:", "*record");
  }
}
```

Just like every other task global, external depots are declared with `declareObject`. They can then be used as sources for topologies by referencing that var in a `.source` declaration. Just like regular depots, any number of topologies can consume from the same external depot.

The core concepts for external depots are "partition indexes" and "offsets". Just like how regular depots can be distributed across many partitions across many nodes, external depots can be distributed as well. Rama refers to an individual partition it wants to query by "partition index". An external depot that has five partitions would have partition indexes `0`, `1`, `2`, `3`, and `4`. An external depot that isn’t distributed would have a single partition that’s always referred to as partition index `0`.

An "offset" identifies a specific record on a specific partition. Offsets monotonically increase by one with each record on a partition. Rama always requests data from an external depot by offset.

All the `ExternalDepot` methods execute on task threads so they must be non-blocking. So just like `eachAsync`, each of these methods returns a [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html). If the client library for the external system you wish to implement does not have a non-blocking interface, you’ll need to set up and manage a background thread in the `ExternalDepot` implementation for issuing those blocking calls.

Let’s now take a look at the requirements for each of the `ExternalDepot` interface methods. The first, `getNumPartitions()`, returns the number of partitions for the external depot. Rama assigns partitions of the external depot to tasks in the module. For example, if an external depot has ten partitions and the module has eight tasks, then six tasks will be assigned to one partition and two tasks will be assigned to two partitions. Rama calls `getNumPartitions()` regularly (every 30 seconds by default) to detect any upscaling or downscaling of the external depot. Rama internally stores progress state for consuming topologies by partition index, so it’s critical that the external depot implementation ensures the same partition index always refers to the same partition of data. This progress state is used by Rama to determine what to process next or what data to retry in case of a failure.

The `startOffset(int partitionIndex)` and `offsetAfterTimestampMillis(int partitionIndex, long millis)` methods exist solely to support "start from" options for [stream](stream.html#_start_from_options) and [microbatch](microbatch.html#_start_from_options) topologies. If you don’t ever plan to use those options, it’s ok for these methods to throw an [UnsupportedOperationException](https://docs.oracle.com/javase/7/docs/api/java/lang/UnsupportedOperationException.html).

`startOffset(int partitionIndex)` should return the first available offset on that partition. `offsetAfterTimestampMillis(int partitionIndex, long millis)` should return the first offset appended to that partition after that timestamp. If there is no offset after that timestamp, it should deliver `null` to the returned `CompletableFuture`. This method requires the external depot source to maintain a time index for appended records.

The `endOffset(int partitionIndex)` method returns the next offset that will be appended to on the partition. It’s one more than the last available offset on the partition. This must be implemented.

The `fetchFrom(int partitionIndex, long startOffset, long endOffset)` method must return a list of all records between that range of offsets on that partition. `startOffset` is inclusive and `endOffset` is exclusive. This method is primarily used for fetching microbatches. This method must return exactly that range, no more and no less.

The `fetchFrom(int partitionIndex, long startOffset)` method must return a "reasonable" amount of data starting from `startOffset`. This method is used for streaming to get new data to push to stream topologies. Rama only calls this method if consuming stream topologies have available capacity to process new records. This provides backpressure to gracefully lower the load on external depot sources if stream topologies are behind (e.g. due to bursts in traffic or underallocation of resources). The implementation of `ExternalDepot` is free to determine what is a "reasonable" amount of data to poll at a time. As a rule of thumb, you should return no more than a few hundred records per call of this method.

For a complete example of an `ExternalDepot` implemention, you can reference the [rama-kafka](https://github.com/redplanetlabs/rama-kafka) implementation.

### Dynamic options for external depots

Since external depots aren’t actual depots, depot-level [dynamic options](operating-rama.html#_worker_configurations_and_dynamic_options) don’t apply to them. Instead, they can be configured by setting those options at the module level.

## Achieving exactly-once update semantics on external systems

One of the great benefits of using [microbatch topologies](microbatch.html) are their strong exactly-once update semantics for PStates. No matter how many failures occur and how many times processing needs to be retried, the results into PStates will be as if processing was done exactly one time. The logic powering that happens completely behind the scenes and as a user you just need to worry about your computation.

You can get the same exactly-once update semantics when updating external systems, like external databases, with a microbatch topology. It’s not as elegant or efficient as how Rama implements PStates, but the necessary information is exposed so you can implement it yourself.

The core to how microbatch topologies achieve exactly-once semantics is by tracking a value called the "microbatch ID". The microbatch ID is a 64 bit value that increments by one with each successful microbatch. Critically, each microbatch ID is associated with a specific range of data on each depot partition (either built-in depots or external depots) being consumed by the microbatch topology. So if a microbatch attempt fails, the microbatch is retried with the same microbatch ID and the exact same data from each depot.

The microbatch ID is stored as part of each PState partition updated in a microbatch. PState partitions are versioned by microbatch ID. So when a microbatch retries, all PState partitions are reverted internally to the version for the previous microbatch ID. Because a microbatch must succeed before moving on to the next microbatch, this guarantees exactly-once update semantics.

You can achieve the same thing with external systems by versioning your data per-record. This isn’t nearly as efficient as versioning per partition like Rama PStates, but it achieves the same purpose. Rama provides the function [Ops.CURRENT\_MICROBATCH\_ID](https://redplanetlabs.com/javadoc/com/rpl/rama/ops/Ops.html#CURRENT_MICROBATCH_ID) to get the ID for the current microbatch attempt. This function can only be called from within a microbatch topology.

Here’s some pseudocode showing how you could use `Ops.CURRENT_MICROBATCH_ID` to achieve exactly-once update semantics to a key/value database system:

```java
.eachAsync((MyExternalSystem s, String key) -> s.getAsync(key), "*mySystem", "*key").out("*record")
.each(Ops.CURRENT_MICROBATCH_ID).out("*microbatchId")
.each((MyRecord record, Long microbatchId, Integer toAdd) -> {
  int curr;
  if(microbatchId.equals(record.microbatchId)) {
    curr = record.prevVal;
  } else {
    curr = record.currVal;
  }
  return new MyRecord(curr, curr + toAdd, microbatchId);
}, "*record", "*microbatchId", "*toAdd").out("*newRecord")
.eachAsync((MyExternalSystem s, String key, MyRecord record) -> s.putAsync(key, record), "*mySystem", "*key", "*newRecord");
```

In this example, each value for the K/V database stores the last microbatch ID to update it, the value associated with that microbatch ID, and the value associated for the previous microbatch ID. This example increments the stored value for the key `"*key"` by the value of `"*toAdd"`. Before updating the record, it checks if a previous attempt for this microbatch ID updated the value. If so, it uses the value associated with the previous microbatch ID to perform the increment. This ensures that no matter how many times the microbatch fails and retries, the values represented in the external K/V database reflect the processing of depot records exactly one time.

## Summary

Rama’s facilities for integrating with external systems are simple and general purpose. Task globals make it easy to manage any resources you need for integrations, `eachAsync` enables you to interact with external systems in efficient and arbitrary ways, and the external depot API lets you use external systems as data sources for topologies with support for Rama’s full feature set. Because everything is just Java code, and your integrations are just Java libraries, you’re able to manage your integrations just like you would any dependency for any Java application. Additionally, you’re able to test your integrations just like you would test any Java system.