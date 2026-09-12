# Depots
> Source: https://redplanetlabs.com/docs/~/depots.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Depots

Depots are distributed logs of data that exist across one or more partitions on a module. All new data enters Rama via depots, and topologies source all incoming data from them. On this page you will learn:

- Declaring depots and options available
- Guidelines for what data should go in the same depot versus separate depots
- Tick depots, which emit based on the passage of time
- Depot client API
- Using ack levels to detect completion of processing
- Appending to depots from topologies

All examples on this page can be found in the [rama-examples](https://github.com/redplanetlabs/rama-examples) project.

## Declaring depots

Depots are declared at the top-level of a module definition with [declareDepot](https://redplanetlabs.com/javadoc/com/rpl/rama/RamaModule.Setup.html#declareDepot-java.lang.String-com.rpl.rama.impl.NativeDepotPartitioning-). Here are a few examples of depot declarations:

```java
public class BasicDepotExamplesModule implements RamaModule {
  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.declareDepot("*depot1", Depot.random());
    setup.declareDepot("*depot2", Depot.hashBy(Ops.FIRST));
    setup.declareDepot("*depot3", Depot.disallow());
  }
}
```

This module declares three depots each with a different *partitioning scheme*. Though this module has no ETLs or PStates, the depots could still be used as sources for topologies [in other modules](module-dependencies.html).

A depot’s partitioning scheme determines to which partition a client append goes. There are three built-in schemes available, and depots also support custom partitioning schemes. The three built-in schemes are shown in the above example and are:

- `Depot.random`: Each append goes to a random partition. The randomness ensures an even distribution, but ordering of processing cannot be guaranteed.
- `Depot.hashBy`: Each append goes to a partition determined by the hash of the value extracted by the provided function. This ensures data with the same extracted value always goes to the same partition. The use of hashing ensures an even distribution overall across all depot partitions (except for unusual scenarios, like the extraction function always extracting the same value).
- `Depot.disallow`: Appends from clients are not allowed, and attempting one will result in an exception on the client. This is used for depots which are meant to only be appended to by topologies. For example, a module may be publishing an event stream based on other depots meant for consumption by other modules.

Custom partitioning schemes are specified by implementing the interface [Depot.Partitioning](https://redplanetlabs.com/javadoc/com/rpl/rama/Depot.Partitioning.html) and providing the class reference when calling `declareDepot`. Here’s an example:

```java
public class CustomPartitioningModule implements RamaModule {
  public static class MyPartitioner implements Depot.Partitioning<Integer> {
    @Override
    public int choosePartitionIndex(Integer data, int numPartitions) {
      if(data==11) return numPartitions - 1;
      else return 0;
    }
  }

  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.declareDepot("*depot", MyPartitioner.class);
  }
}
```

This partitioner expects `Integer` types to be appended to this depot. The data value `11` gets appended to the last partition, and all other data gets appended to partition 0. All partitions in between will never have data appended (this isn’t a useful partitioner!).

There are two reasons why depot partitioning can be important. The first is so related events get processed in the order in which they happened. This is also known as maintaining *local ordering*. For example, suppose your application has "profile field set" data that’s generated from a user interacting with a web app. If those are processed in a different order than the user generated them, your PState mapping users to profile fields would end up with the wrong results. If you were to use `Depot.random()` for that depot, then they could be processed out of order since data on different partitions are processed in parallel and independently.

By using a depot partitioner to ensure any individual user’s "profile field set" data goes to the same depot partition, local ordering is maintained and ETLs can process that data in the correct order. The `Depot.hashBy` partitioner would be appropriate for this use case.

The second reason depot partitioning can be important is performance. For many use cases, depot data corresponds directly to PState updates. A colocated ETL topology for "profile field set" data could simply write that data into a corresponding PState mapping users to profile fields. If the depot is partitioned the same way as the PState, you can write the topology like so (in this example "profile field set" data is represented as tuples of `[user ID, field, value]`):

```java
public class ProfileFieldSetGoodModule implements RamaModule {
  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.declareDepot("*profileFieldsDepot", Depot.hashBy(Ops.FIRST));

    StreamTopology profiles = topologies.stream("profiles");
    profiles.pstate(
      "$$profiles",
      PState.mapSchema(
        String.class,
        PState.mapSchema(
          String.class,
          Object.class)));

    profiles.source("*profileFieldsDepot").out("*tuple")
            .each(Ops.EXPAND, "*tuple").out("*userId", "*field", "*value")
            .localTransform("$$profiles", Path.key("*userId", "*field").termVal("*value"));
  }
}
```

Since the depot partitions using the hash of the user ID, the data is already on the task needed to update the PState. For this reason the stream topology does not need to use any partitioners after reading data off the depot. It’s able to immediately write to the PState.

On the other hand, suppose the depot is partitioned differently than the PState:

```java
public class ProfileFieldSetBadModule implements RamaModule {
  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.declareDepot("*profileFieldsDepot", Depot.random());

    StreamTopology profiles = topologies.stream("profiles");
    profiles.pstate(
      "$$profiles",
      PState.mapSchema(
        String.class,
        PState.mapSchema(
          String.class,
          Object.class)));

    profiles.source("*profileFieldsDepot").out("*tuple")
            .each(Ops.EXPAND, "*tuple").out("*userId", "*field", "*value")
            .hashPartition("*userId")
            .localTransform("$$profiles", Path.key("*userId", "*field").termVal("*value"));
  }
}
```

Besides the local ordering problems mentioned above, this topology has the additional burden of needing to relocate the computation to the correct task by using `hashPartition`. This is an extra network hop which is a non-trivial amount of extra resource usage.

### Choosing number of depots

Data should be appended to the same depot when related and to different depots when unrelated. Data is related if local ordering is important or if they affect the same conceptual entities.

Topologies read all data appended to a depot. So if a topology doesn’t need certain data being appended, it must filter out that data at the beginning of processing. For example, keeping both "profile field sets" and "pageview" information on the same depot would probably require a lot of filtering by topologies. These totally unrelated datatypes would be better off in separate depots.

Local ordering can be relevant for data of different types. For example, if you have `Follow` and `Unfollow` data, both data types affect your social graph PStates. It’s important a user’s `Follow` and `Unfollow` events are processed in the order in which they happened, so they should go on the same depot. Rama provides the [subSource](intermediate-dataflow.html#_subsource) dataflow method to make it easy to process distinct datatypes off the same depot.

### Depot options

Depots only have one option available when declaring them, `global`. A global depot exists as a single partition. Here’s an example:

```java
public class GlobalDepotModule implements RamaModule {
  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.declareDepot("*myGlobalDepot", Depot.random()).global();
  }
}
```

Global depots should not be used for high throughput depots because of their inherent lack of scalability. Though the declaration still requires a depot partitioner, which one you use doesn’t matter since there’s only one partition.

## Tick depots

Rama provides a special kind of depot called a "tick depot" which emits based on the passage of time. Tick depots are configured with a frequency and cannot be appended to. They are useful for any time-based behavior needed in ETL topologies.

Here’s an example of declaring and using a tick depot:

```java
public class TickDepotModule implements RamaModule {
  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.declareTickDepot("*ticks", 3000);

    StreamTopology s = topologies.stream("s");
    s.source("*ticks")
     .each(Ops.PRINTLN, "Tick");
  }

  public static void main(String[] args) throws Exception {
    try(InProcessCluster cluster = InProcessCluster.create()) {
      cluster.launchModule(new TickDepotModule(), new LaunchConfig(4, 4));
      Thread.sleep(10000);
    }
  }
}
```

This creates a tick depot bound to the var `"*ticks"` that emits every three seconds. A stream topology subscribes to the depot and prints every time it emits. The main method lets the module run for ten seconds and prints:

```java
Tick
Tick
Tick
```

The stream topology doesn’t have a call to `out` to capture the value emitted by the tick depot because it doesn’t matter. If you were to capture it, you would see tick depots always emit the same constant value.

Ticks are emitted on task 0. If you want the tick to trigger action on all tasks, you would use `allPartition` after the tick.

Because stream topologies are [push-based](stream.html#_operation) and process depot data as soon as it’s emitted, they will always run at the frequency of the tick depot (except for slight variance due to things like GC). Microbatch topologies are a bit different because they’re [pull-based](microbatch.html#_operation_and_fault_tolerance). Every time a new microbatch attempt runs, it checks to see if enough time has elapsed since the last tick. If so, then the tick depot will emit exactly one time for that microbatch. If not, it won’t emit.

Since microbatches can take hundreds of milliseconds to many seconds to run, ticks for microbatching won’t run at exactly the frequency at which they’re configured – especially if the tick frequency is very low. For example, if the tick frequency is ten milliseconds and microbatches take 500 milliseconds, the ticks will only run once every 500 milliseconds. This is the frequency at which microbatches are checking the depot. And critically, even if many tick periods have passed since the last check, only one tick will be emitted for that microbatch.

## Depot client API

In the rest of the documentation you’ve seen many examples of using a depot client to do appends. Those examples only showed the most basic behavior of depot appends. Let’s now take a look at all the functionality available.

A depot client is retrieved from [RamaClusterManager](https://redplanetlabs.com/javadoc/com/rpl/rama/RamaClusterManager.html) on a real cluster, or from [InProcessCluster](https://redplanetlabs.com/javadoc/com/rpl/rama/test/InProcessCluster.html) in a test environment. Connecting to a Rama cluster to fetch depot clients is discussed more on [this page](operating-rama.html).

Depot clients are primarily used to append new data, but they can also be used to query ranges of data from depot partitions.

### Appends

A client append automatically makes use of the configured depot partitioner from the depot declaration. So you never have to worry about which partition to send data to – the depot client handles that for you. This is exactly as it should be, as ultimately the module author knows how the depot will be used and thereby how it should be partitioned.

Appends also store and index in the depot partition the time of the append. This is used for "start from" options for [stream](stream.html#_start_from_options) and [microbatch](microbatch.html#_start_from_options) topologies.

There are two signatures for the depot client `append` call. The first, which you’ve seen many times already, just takes in the data to append. The second takes in the data to append and an "ack level". The ack level tells the client what condition to wait for before returning success. Here are examples of all the ack levels available when calling `append`:

```java
depot.append("some data", AckLevel.APPEND_ACK);
depot.append("some data", AckLevel.ACK);
depot.append("some data", AckLevel.NONE);
```

`AckLevel.APPEND_ACK` returns success after the data has been appended to the depot partition and replicated successfully. `AckLevel.ACK` waits for the same condition plus waiting for all colocated stream topologies to process the data, including replication of any updated PStates. If there are no colocated stream topologies, `AckLevel.ACK` is equivalent to `AckLevel.APPEND_ACK`. `AckLevel.NONE` doesn’t wait for anything and returns success immediately.

An example of where `AckLevel.ACK` is useful is coordinating a user interface with the processing of data submitted by a user. For example, you may want a submit button for a profile update to be disabled until the profile update has been recorded in the associated PState.

Colocated stream topologies can also return arbitrary information during processing to clients of depot appends. This is called "streaming ack returns" and is documented in [the next section](#_streaming_ack_returns). One example use case for this is returning a user ID for a user registration depot append.

The depot client will throw an exception if anything goes wrong, such as a network partition or disk error on the target depot partition. An exception doesn’t mean the append did not go through – it just means it didn’t go through cleanly. For instance, if there were a network partition right before the server was about to send the success message back to the client, you would get an exception even though the append finished successfully.

For `AckLevel.ACK`, by default success of colocated streaming topologies is determined solely on the first attempt of the data. So if it fails the first time the stream topology tries to process it but succeeds when the topology retries it, the client append call will get an exception. This behavior can be changed with the dynamic option `depot.ack.failure.on.any.streaming.failure`. When that option is set to `false`, the depot client will wait until a timeout for the colocated stream topologies to succeed, even if they have to retry many times.

When you use the `append` variant without an explicit ack level, the ack level used is `AckLevel.ACK`.

The reason to use ack levels below `AckLevel.ACK` is for lower latencies. The less an `append` call has to wait for, the faster it can return success.

Just like the PState and query topology client APIs, depot clients have non-blocking variants of `append` called `appendAsync`. These return a [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) that is delivered success or an exception depending what happens with the append. Here are examples:

```java
CompletableFuture f = d.appendAsync("some data", AckLevel.APPEND_ACK);
f.get(2, TimeUnit.SECONDS);

CompletableFuture f2 = d.appendAsync("some data");
f2.join();
```

These take in the exact same arguments as `append` and just communicate success/failure in a non-blocking way.

### Querying ranges of data from partitions

Data can be fetched from depot partitions using a depot client. A record in a depot partition is identified by a "partition index" and an "offset". A partition index identifies on which depot partition it lives. Offsets start from zero and increase by one for each depot record appended.

To determine how many partitions a depot has and what offsets are available on each partition, a few methods are provided to return metadata about the depot. The first is [getObjectInfo](https://redplanetlabs.com/javadoc/com/rpl/rama/PartitionedObject.html#getObjectInfo--) (also available on PStates), which returns a [PartitionedObjectInfo](https://redplanetlabs.com/javadoc/com/rpl/rama/object/PartitionedObjectInfo.html) containing general information about the object, including the number of partitions it has.

The second method is [getPartitionInfo](https://redplanetlabs.com/javadoc/com/rpl/rama/Depot.html#getPartitionInfo-long-), which returns the offsets available on a particular depot partition. Since this does a remote call to fetch the info, there’s also an async version [getPartitionInfoAsync](https://redplanetlabs.com/javadoc/com/rpl/rama/Depot.html#getPartitionInfoAsync-long-) available. This returns a [DepotPartitionInfo](https://redplanetlabs.com/javadoc/com/rpl/rama/object/DepotPartitionInfo.html) that contains the offsets available, from start offset (inclusive) to end offset (exclusive).

The method for reading a range of data is [read](https://redplanetlabs.com/javadoc/com/rpl/rama/Depot.html#read-long-long-long-), which takes in a partition index, a start offset, and an end offset. The async version is [readAsync](https://redplanetlabs.com/javadoc/com/rpl/rama/Depot.html#readAsync-long-long-long-). It returns all depot records in that range as a list. If the range includes offsets that don’t exist, the method will throw an exception.

The fetch is done as a single synchronous event, so to avoid locking up the task thread for too long it’s recommended to limit the number of records fetched per call. As a rule of thumb you shouldn’t fetch more than 50kb worth of data at a time. If records are about 50 bytes, then the depot range should be at most 1000 offsets.

## Streaming ack returns

Stream topologies colocated with a depot in the same module can return arbitrary information back to the appender. The depot `append` and `appendAsync` methods return `Map<String, Object>` and `CompletableFuture<Map<String, Object>>` respectively. The returned `Map` is a map from topology name to "streaming ack return". Streaming ack returns are only returned for `AckLevel.ACK`, and the returned map will be empty for `AckLevel.APPEND_ACK` or `AckLevel.NONE`. The map only contains entries for non-null streaming ack returns.

How to specify streaming ack returns in a stream topology is documented [in this section](stream.html#_ack_return_agg_options).

## Appending to depots from topologies

You can also append to a depot from a topology, whether in the same module or a different module. Appends from topologies are done with the `Block` method [depotPartitionAppend](https://redplanetlabs.com/javadoc/com/rpl/rama/Block.html#depotPartitionAppend-java.lang.String-java.lang.Object-).

`depotPartitionAppend` works differently than depot client appends. Whereas depot client appends choose a partition to append to based on the data being appended, `depotPartitionAppend` always appends to the partition represented by the current task. So to control the partition an append goes you must use a [partitioner](partitioners.html). For example:

```java
public class DepotPartitionAppendModule implements RamaModule {
  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.declareDepot("*incomingDepot", Depot.hashBy(Ops.FIRST));
    setup.declareDepot("*outgoingDepot", Depot.disallow());

    StreamTopology s = topologies.stream("s");
    s.source("*incomingDepot").out("*tuple")
     .each(Ops.EXPAND, "*tuple").out("*k", "*k2", "*v")
     .hashPartition("*k2")
     .each(Ops.TUPLE, "*k2", new Expr(Ops.INC, "*v")).out("*newTuple")
     .depotPartitionAppend("*outgoingDepot", "*newTuple");
  }
}
```

This module publishes a new depot based on `"*incomingDepot"` partitioned by a different key and with a transformed value. `"*outgoingDepot"` is given the depot partitioner `Depot.disallow` to prevent depot clients from appending to that depot.

To control the partition for appends to [mirror depots](module-dependencies.html), you must use a partitioner on the depot object itself, like so:

```java
s.source("*depot").out("*tuple")
 .each(Ops.EXPAND, "*tuple").out("*k", "*v")
 .hashPartition("*mirrorDepot", "*k")
 .depotPartitionAppend("*mirrorDepot", "*v");
```

Since the mirror depot could have more or less tasks than the appending module, the explicit hash partition to it is necessary so Rama knows which partition to append to.

## Migrations

A depot’s records can be migrated to new values or excised as part of a [module update](operating-rama.html#_updating_modules). A migration is specified as a function of one argument that takes in a depot record and returns the new value to replace it with. If the function returns [Depot.TOMBSTONE](https://redplanetlabs.com/javadoc/com/rpl/rama/Depot.html#TOMBSTONE), the record is removed from the depot.

A depot migration takes effect instantly regardless of the size of the depot. Like [PState migrations](pstates.html#_migrations), it applies the migration function on read as necessary until the values are migrated on disk.

Here’s an example of specifying a depot migration:

```java
setup.declareDepot("*depot", Depot.random())
     .migration("myMigrationId", (Integer num) -> {
       if(num.equals(10)) return Depot.TOMBSTONE;
       else return "" + num;
     });
```

A migration takes in a migration ID and a migration function. The migration ID is used to determine if a migration should be restarted or continue where it left off when the module is updated mid-migration. If it remains the same, it continues where it left off. Otherwise, it restarts from the beginning of the depot. When a migration is restarted before it completes, the values on the depot will be as if the first migration never happened.

In this example, the original values on the depot were `Integer` objects. This migration function removes any values that were equal to `10` and converts all other values to strings.

Like PState migrations, the migration function for a depot migration must be idempotent (as of version 1.5.0). Depot migrations apply the migration function to new appends until the module is updated to remove the migration, which should be done after it’s complete. This allows for a graceful, zero-downtime transition of clients appending to the depot.

The disk space usage of the depot will be higher during the migration due to having these two logs existing simultaneously during the migration.

Depot migrations never change the offsets of records. If a record is excised, a small tombstone value is written in its place. Topologies and foreign depot reads will exclude those tombstones when reading from the depot. If there are tombstones, a foreign depot read for a range of offsets can thereby return less records than the size of the requested range.

Since the migration function is only run up to the original end offset, it’s required that clients append values of the new form of depot record once the module update has been done.

## Depot trimming

By default, depots permanently store all data appended to them. For many use cases this is desirable, but for others you may want to clean up the disk space for old depot entries that are no longer needed. You can do this by using a feature of depots called "depot trimming".

Depot trimming is controlled through [dynamic options](operating-rama.html#_worker_configurations_and_dynamic_options):

- `depot.max.entries.per.partition`: If set, causes affected depots to regularly delete entries older than this amount. Depot trimming is applied every 10 minutes.
- `depot.excess.proportion`: This determines the size of an extra buffer of depot entries beyond the above option. This is used to determine where topologies start processing data when they specify they wish to begin from the "start" of a depot.
- `depot.trim.coordinate.local.topologies`: This determines whether depot trimming will check if any colocated topologies need data before trimming.
- `depot.trim.coordinate.remote.topologies`: This determines whether depot trimming will check if any topologies in other modules need data before trimming.

Suppose `depot.max.entries.per.partition` is set to 1000 and `depot.excess.proportion` is set to 0.25. Then each depot partition will delete all entries other than the most recent 1250 entries. The excess buffer is there to guard against race conditions with new ETL topologies. If a new ETL wishes to begin from the start of that depot, it will begin at the 1000th oldest entry, not the 1250th. If it began at the very start and the depot happened to trim at that moment, then there could be a gap of data suddenly unavailable to the ETL. The excess buffer makes that scenario very unlikely.

By default, data will not be trimmed if it’s still needed by any topology in any module. So if an ETL topology had a bug in it that caused it to continuously fail, any depots it’s consuming will never trim. You can turn off this behavior for colocated or non-colocated topologies with the dynamic options listed above. When turned off, ETLs will skip ahead to the next available offset in the depot when the data they’re expecting has been trimmed.

## Tuning options

Depots have a number of dynamic options relevant specifically for [stream](stream.html) and [microbatch](microbatch.html) topologies. These are documented on those pages. Besides depot trimming, the only other dynamic option relevant for depots is:

- `replication.depot.append.timeout.millis`: Timeout for [replicating](replication.html) each depot append. If the timeout is exceeded depot client appends with `AckLevel.APPEND_ACK` or `AckLevel.ACK` will throw an exception.

The following configs can be used on foreign depot clients:

- `foreign.depot.flush.delay.millis`: Adds a delay on clients before flushing depot appends to the module. A higher number increases the amount of batching that can be done. This defaults to 0. An optimal number is usually between 0 and 50 milliseconds.
- `foreign.depot.operation.timeout.millis`: Timeout to use for foreign depot operations, including appends and partition queries

## Summary

Depots are simple to configure and use, with all the hard parts (replication, tracking downstream processing) happening automatically in the background. Depots being integrated on the same processes/threads as PStates enables great efficiency for simple topologies that don’t need any further partitioning.

The keys to using depots effectively are determining how many depots to have, what data should go on which depot, and how each depot should be partitioned. With a little experience, managing the tradeoffs at play becomes a straightforward part of the application design process.