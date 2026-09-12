# Testing
> Source: https://redplanetlabs.com/docs/~/testing.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Testing

Rama’s integrated approach pays huge dividends when it comes to testing. Instead of needing to juggle complex set ups, teardowns, and mocks of disparate databases, data processors, and other tools, full Rama clusters and modules can be simulated in a single process with very little code using [InProcessCluster](https://redplanetlabs.com/javadoc/com/rpl/rama/test/InProcessCluster.html). This lets you test every aspect of your modules end-to-end.

On this page, you’ll learn:

- Using `InProcessCluster`
- Testing stream topologies that are using mirror depots
- Testing modules with circular dependencies
- `MockOutputCollector` for unit testing custom operations

All examples on this page can be found in the [rama-examples](https://github.com/redplanetlabs/rama-examples) project.

## Using InProcessCluster

With `InProcessCluster` you can launch, update, and destroy modules just like you can on a real cluster. There are no differences in module capabilities or execution semantics between an `InProcessCluster` and a real cluster.

Here’s an example of using `InProcessCluster` along with JUnit to test a simple module:

```java
public class SimpleInProcessClusterExample {
  public static class SimpleModule implements RamaModule {
    @Override
    public void define(Setup setup, Topologies topologies) {
      setup.declareDepot("*depot", Depot.hashBy(Ops.IDENTITY));

      StreamTopology s = topologies.stream("counter");
      s.pstate("$$counts", PState.mapSchema(String.class, Long.class));
      s.source("*depot").out("*k")
       .compoundAgg("$$counts", CompoundAgg.map("*k", Agg.count()));
    }
  }

  @Test
  public void simpleTest() throws Exception {
    try(InProcessCluster cluster = InProcessCluster.create()) {
      cluster.launchModule(new SimpleModule(), new LaunchConfig(4, 2));

      String moduleName = SimpleModule.class.getName();
      Depot depot = cluster.clusterDepot(moduleName, "*depot");
      PState counts = cluster.clusterPState(moduleName, "$$counts");

      depot.append("cagney");
      depot.append("davis");
      depot.append("cagney");

      assertEquals(2, (Long) counts.selectOne(Path.key("cagney")));
      assertEquals(1, (Long) counts.selectOne(Path.key("davis")));
      assertNull(counts.selectOne(Path.key("garbo")));
    }
  }
}
```

An `InProcessCluster` launches many threads for a [Conductor](terminology.html#_conductor), a [Supervisor](terminology.html#_supervisor), the [Metastore](terminology.html#_metastore), and any workers launched by the module. So it’s important to call `close` on an `InProcessCluster` when the test finishes to clean up those resources. Generally it’s easiest to use a `try` block as demonstrated in this example to handle calling `close` for you.

An `InProcessCluster` is created using `InProcessCluster.create()`. There’s another version of that method for registering custom serializations which is explained more on [this page](serialization.html).

A module is launched on an `InProcessCluster` using the method `launchModule`. It takes as input an instance of a module and a `LaunchConfig` to specify the parallelism. This example specifies four tasks and two task threads. The replication factor cannot be configured for a module on an `InProcessCluster` since it doesn’t change the semantics of the module. So all modules on an `InProcessCluster` run with a replication factor of one.

You can also specify multiple workers using the `numWorkers` method on `LaunchConfig`, like so:

```java
cluster.launchModule(new SimpleModule(), new LaunchConfig(4, 2).numWorkers(2));
```

This would run two workers each with one task thread and two tasks. Running more workers doesn’t change the semantics of a module, but it does enable the module to exercise any custom serializations you have registered. Within a module Rama only performs serialization of data between workers and doesn’t serialize data going to a task thread located within the same worker.

The general flow of testing with `InProcessCluster` is to perform depot appends and then assert on the expected changes to downstream PStates. With a stream topology, like in this example, depot appends using [AckLevel.ACK](depots.html#_depot_client_api) don’t return until all downstream processing has finished. So the assertions can be made immediately on the PStates after the depot appends complete.

### Testing microbatch topologies

With a microbatch topology, processing is asynchronous to depot appends even with `AckLevel.ACK`. So `InProcessCluster` provides the helper method `waitForMicrobatchProcessedCount` to ease testing. Here’s an example:

```java
public class MicrobatchTestingExample {
  public static class MicrobatchModule implements RamaModule {
    @Override
    public void define(Setup setup, Topologies topologies) {
      setup.declareDepot("*depot", Depot.hashBy(Ops.IDENTITY));

      MicrobatchTopology mb = topologies.microbatch("counter");
      mb.pstate("$$counts", PState.mapSchema(String.class, Long.class));
      mb.source("*depot").out("*mb")
        .explodeMicrobatch("*mb").out("*k")
        .compoundAgg("$$counts", CompoundAgg.map("*k", Agg.count()));
    }
  }

  @Test
  public void microbatchTest() throws Exception {
    try(InProcessCluster cluster = InProcessCluster.create()) {
      cluster.launchModule(new MicrobatchModule(), new LaunchConfig(4, 2));

      String moduleName = MicrobatchModule.class.getName();
      Depot depot = cluster.clusterDepot(moduleName, "*depot");
      PState counts = cluster.clusterPState(moduleName, "$$counts");

      depot.append("cagney");
      depot.append("davis");
      depot.append("cagney");

      cluster.waitForMicrobatchProcessedCount(moduleName, "counter", 3);
      assertEquals(2, (Long) counts.selectOne(Path.key("cagney")));
      assertEquals(1, (Long) counts.selectOne(Path.key("davis")));
      assertNull(counts.selectOne(Path.key("garbo")));

      depot.append("cagney");

      cluster.waitForMicrobatchProcessedCount(moduleName, "counter", 4);
      assertEquals(3, (Long) counts.selectOne(Path.key("cagney")));
    }
  }
}
```

This example uses `waitForMicrobatchProcessedCount` to ensure the PStates reflect the processing of the appended depot records. `waitForMicrobatchProcessedCount` takes in as input a module name, topology name, and a total number of depot records. Critically, the number of depot records specified represents the total amount of records ever processed by the topology, not the number of depot records since the last time it was called. This is why the example first waits for three records and then waits for four records after appending one more record.

`InProcessCluster` also exposes facilities to pause and resume microbatch topologies. Since microbatching is always running asynchronously, you can’t control which depot records comprise an individual microbatch attempt. If you perform three depot appends, those could be processed in three separate microbatches, in two separate microbatches, or in one microbatch. If you care about the composition of a microbatch for the purposes of testing, like to test the behavior of [batch blocks](intermediate-dataflow.html#_batch_blocks), you can use `pauseMicrobatchTopology` and `resumeMicrobatchTopology`. For example:

```java
cluster.pauseMicrobatchTopology(moduleName, "counter");
depot.append("a");
depot.append("b");
depot.append("c");
depot.append("a");
cluster.resumeMicrobatchTopology(moduleName, "counter");
```

`pauseMicrobatchTopology` waits for the currently in-flight microbatch to complete before returning. Since a microbatch processes up to 1000 unprocessed records from each depot partition (by default), this code guarantees the next microbatch will contain those four records.

### Testing stream topologies that are using mirror depots

Depot appends using `AckLevel.ACK` don’t wait for stream topologies from other modules to finish processing them. So whereas with colocated stream topologies you can assert on PStates immediately after a depot append finishes, stream topologies consuming mirror depots may still be processing that depot record after the append finishes.

The technique to use in this case is to repeatedly poll the desired PState condition up to a timeout. Here’s an example:

```java
public class StreamTopologyWithMirrorTestingExample {
  public static class DepotModule implements RamaModule {
    @Override
    public void define(Setup setup, Topologies topologies) {
      setup.declareDepot("*depot", Depot.hashBy(Ops.IDENTITY));
    }
  }

  public static class CounterModule implements RamaModule {
    @Override
    public void define(Setup setup, Topologies topologies) {
      setup.clusterDepot("*mirror", DepotModule.class.getName(), "*depot");

      StreamTopology s = topologies.stream("counter");
      s.pstate("$$counts", PState.mapSchema(String.class, Long.class));
      s.source("*mirror").out("*k")
       .hashPartition("*k")
       .compoundAgg("$$counts", CompoundAgg.map("*k", Agg.count()));
    }
  }

  public void assertValueAttained(Object expected, RamaFunction0 f) throws Exception {
    long nanos = System.nanoTime();
    while(true) {
      Object val = f.invoke();
      if(expected.equals(val)) break;
      else if(System.nanoTime() - nanos > 1000000000L * 30) {
        throw new RuntimeException("Condition failed to attain " + expected + " != " + val);
      }
      Thread.sleep(50);
    }
  }

  @Test
  public void streamTopologyWithMirrorTest() throws Exception {
    try(InProcessCluster cluster = InProcessCluster.create()) {
      cluster.launchModule(new DepotModule(), new LaunchConfig(8, 2));
      cluster.launchModule(new CounterModule(), new LaunchConfig(4, 2));

      Depot depot = cluster.clusterDepot(DepotModule.class.getName(), "*depot");
      PState counts = cluster.clusterPState(CounterModule.class.getName(), "$$counts");

      depot.append("cagney");
      depot.append("davis");
      depot.append("cagney");

      assertValueAttained(2L, () -> counts.selectOne(Path.key("cagney")));
      assertValueAttained(1L, () -> counts.selectOne(Path.key("davis")));
    }
  }
}
```

Like the other examples, this code creates a PState that counts the depot records. The difference is the depot is kept in a separate module as the stream topology computing the counts.

To perform the assertions, this class defines a helper `assertValueAttained` that polls the provided function for up to 30 seconds until it equals the expected value. It’s important to use a generous timeout since garbage collection can easily cause a condition like this to fail incorrectly if the timeout is low.

### Module update

You can also perform module updates with `InProcessCluster`. To perform module updates the two module instances you deploy need to have the same module name, so you’ll need to override the `getModuleName` method of `RamaModule` to achieve this. Here’s an example:

```java
public class UpdateExample {
  public static class CounterModule_v1 implements RamaModule {
    @Override
    public void define(Setup setup, Topologies topologies) {
      setup.declareDepot("*depot", Depot.hashBy(Ops.IDENTITY));

      StreamTopology s = topologies.stream("counter");
      s.pstate("$$counts", PState.mapSchema(String.class, Long.class));
      s.source("*depot").out("*k")
       .compoundAgg("$$counts", CompoundAgg.map("*k", Agg.sum(2)));
    }

    @Override
    public String getModuleName() {
      return "CounterModule";
    }
  }

  public static class CounterModule_v2 implements RamaModule {
    @Override
    public void define(Setup setup, Topologies topologies) {
      setup.declareDepot("*depot", Depot.hashBy(Ops.IDENTITY));

      StreamTopology s = topologies.stream("counter");
      s.pstate("$$counts", PState.mapSchema(String.class, Long.class));
      s.source("*depot").out("*k")
       .compoundAgg("$$counts", CompoundAgg.map("*k", Agg.count()));
    }

    @Override
    public String getModuleName() {
      return "CounterModule";
    }
  }

  @Test
  public void updateTest() throws Exception {
    try(InProcessCluster cluster = InProcessCluster.create()) {
      cluster.launchModule(new CounterModule_v1(), new LaunchConfig(4, 2));
      Depot depot = cluster.clusterDepot("CounterModule", "*depot");
      PState counts = cluster.clusterPState("CounterModule", "$$counts");

      depot.append("cagney");
      assertEquals(2, (Long) counts.selectOne(Path.key("cagney")));

      cluster.updateModule(new CounterModule_v2());

      depot.append("cagney");
      assertEquals(3, (Long) counts.selectOne(Path.key("cagney")));
    }
  }
}
```

This example simulates fixing a bug in a running module. The first version of "CounterModule" increments by two instead of by one, and the second version fixes that. In the test code, you can see that `depot` and `counts` can be used after the module update, just like with clients for real clusters. Internally those clients automatically redirect their appends/queries to the updated module instance.

The `updateModule` call shown in this example can only be used when no PStates or depots are being removed from the existing module instance. Just like [with real clusters](operating-rama.html#_updating_modules), since removing PStates and depots is destructive Rama requires you to be explicit about their removals. With real clusters this is specified with an additional flag on the Rama CLI, but with `InProcessCluster` this is specified with an additional argument to `updateModule`. For example, if the new version of your module removes `"*depot"` and `"$$p"`, the update code would be:

```java
cluster.updateModule(new MyModule_v2(), UpdateOptions.objectsToDelete("*depot", "$$p"));
```

The deleted objects must be specified exactly or the update will not go through and you’ll get an exception.

### Module destroy

You can also destroy modules using `InProcessCluster`. Here’s an example:

```java
cluster.destroyModule("com.mycompany.MyModule");
```

Just like a real cluster, this requires no other modules to have a dependency on the module being destroyed.

## Testing modules with circular dependencies

Through module updates, you can deploy modules with mutual dependencies on each other. See [this section](module-dependencies.html#_circular_dependencies) for details on how this works and how to create circular dependencies with an `InProcessCluster`.

## Unit testing a custom RamaOperation with MockOutputCollector

You may want to unit test a `RamaOperation` implementation outside the context of a module. Rama provides [MockOutputCollector](https://redplanetlabs.com/javadoc/com/rpl/rama/test/MockOutputCollector.html) for this purpose.

Here’s an example of usage:

```java
public class MockOutputCollectorExample {
  public static class MyOperation implements RamaOperation1<Integer> {
    @Override
    public void invoke(Integer n, OutputCollector collector) {
      collector.emitStream("somestream", 1, 2);
      for(int i=0; i < n; i++) collector.emit(i);
      collector.emitStream("somestream", 3, 4);
    }
  }

  @Test
  public void mockOutputCollectorTest() {
    MockOutputCollector collector = new MockOutputCollector();
    new MyOperation().invoke(2, collector);

    List<CapturedEmit> emits = collector.getEmits();
    assertEquals(4, emits.size());

    assertEquals("somestream", emits.get(0).getStreamName());
    assertEquals(Arrays.asList(1, 2), emits.get(0).getValues());

    assertNull(emits.get(1).getStreamName());
    assertEquals(Arrays.asList(0), emits.get(1).getValues());

    assertNull(emits.get(2).getStreamName());
    assertEquals(Arrays.asList(1), emits.get(2).getValues());

    assertEquals("somestream", emits.get(3).getStreamName());
    assertEquals(Arrays.asList(3, 4), emits.get(3).getValues());

    Map expected = new HashMap();
    expected.put(null, Arrays.asList(Arrays.asList(0), Arrays.asList(1)));
    expected.put("somestream", Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
    assertEquals(expected, collector.getEmitsByStream());
  }
}
```

`MockOutputCollector` captures emits and lets you assert on them afterwards. It provides two ways to look at those emits: `getEmits`, which returns all emits in the order in which they happened, and `getEmitsByStream` which returns a map from stream name to emitted values.

This example demonstrates both `getEmits` and `getEmitsByStream`. This is redundant in this case and only done for illustration. In a real unit test you would only use one of the two methods. When you care about the order of emits across stream names, you should use `getEmits`. Otherwise, using `getEmitsByStream` is probably slightly less code.

When `emit` is used within the `RamaOperation`, the captured stream name will be `null`. Since `emit` or `emitStream` can emit any number of values, emitted values are always captured as a list.

## TestPState

Rama provides the class [TestPState](https://redplanetlabs.com/javadoc/com/rpl/rama/test/TestPState.html) that can be used to write tests using PStates outside the context of modules. A `TestPState` can be used with [localTransform](https://redplanetlabs.com/javadoc/com/rpl/rama/Block.html#localTransform-java.lang.String-com.rpl.rama.Path-) and [localSelect](https://redplanetlabs.com/javadoc/com/rpl/rama/Block.html#localSelect-java.lang.String-com.rpl.rama.Path-) in dataflow code, and they can also be manipulated directly using the `select` and `transform` methods directly on the object. The latter methods are also a great way to experiment with the capabilities of paths.

Here’s an example of using `TestPState` to unit test a [macro](intermediate-dataflow.html#_macros):

```java
public static Block fooMacro(String pstateVar) {
  return Block.localTransform(pstateVar, Path.key("a").term(Ops.INC));
}

@Test
public void testPStateExampleTest() throws Exception {
  try(TestPState tp = TestPState.create(PState.mapSchema(String.class, Integer.class))) {
    tp.transform(Path.key("a").termVal(10));
    assertEquals(10, (int) tp.selectOne(Path.key("a")));

    Block.each(Ops.IDENTITY, tp).out("$$p")
         .macro(fooMacro("$$p"))
         .execute();
    assertEquals(11, (int) tp.selectOne(Path.key("a")));
  }
}
```

## Summary

Unit testing is one of the pillars upon which you build robust applications. This is why the Rama team has spent so much time making the testing of Rama applications as seamless as possible.

While this page covered unit testing, that’s not the end of the story when it comes to testing. It’s also important to run modules on a development cluster to do performance analysis to determine how many resources they will need. Performance tests can leverage Rama’s [self-monitoring](operating-rama.html#_activating_self_monitoring) to determine max throughput and verify modules balance computation evenly across all workers. Profilers can also be useful for identifying micro-optimizations in module code to improve performance.