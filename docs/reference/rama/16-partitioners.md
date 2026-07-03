# Partitioners
> Source: https://redplanetlabs.com/docs/~/partitioners.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Partitioners

At a fundamental level Rama’s ETL topologies let you run whatever computation you need wherever you need it. Processing incoming data may require you to fetch and update data from many tasks, and partitioners are how you control where your computation runs at any given point.

In this section you will learn:

- How partitioners work
- Built-in partitioners available
- How to define custom partitioners
- Partitioning to PStates or depots from other modules
- Implicit partitioner inserted by `select`

All examples on this page can be found in the [rama-examples](https://github.com/redplanetlabs/rama-examples) project.

## How partitioners work

In [this section](tutorial3.html#task-model) from the tutorial, you learned how a Rama module is deployed across some number of *tasks*. Each task runs the exact same code and stores different partitions of PStates and depots. When ETL code is running on a task it can read and write to colocated partitions.

A partitioner moves dataflow computation to one or more target tasks. Let’s take a look at an example:

```java
public class BasicPartitionerExampleModule implements RamaModule {
  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.declareDepot("*depot", Depot.random());

    StreamTopology s = topologies.stream("s");
    s.source("*depot").out("*k")
     .each(Ops.PRINTLN, "Start task", "*k", new Expr(Ops.CURRENT_TASK_ID))
     .hashPartition("*k")
     .each(Ops.PRINTLN, "End task", "*k", new Expr(Ops.CURRENT_TASK_ID));
  }

  public static void main(String[] args) throws Exception {
    try(InProcessCluster cluster = InProcessCluster.create()) {
      RamaModule module = new BasicPartitionerExampleModule();
      cluster.launchModule(module, new LaunchConfig(8, 4));
      String moduleName = module.getClass().getName();

      Depot depot = cluster.clusterDepot(moduleName, "*depot");

      depot.append("cagney");
      depot.append("cagney");
      depot.append("lemmon");
      depot.append("lemmon");
      depot.append("bergman");
      depot.append("bergman");
    }
  }
}
```

This module is launched with 8 tasks and prints the difference in task ID before and after a `hashPartition` call. Since the depot partitioner is `Depot.random`, the start task is random. `hashPartition` chooses its target task based on the hash of the input. The same input will always go to the same task, but different inputs can go to different tasks. Running the `main` method prints:

```java
Start task cagney 3
End task cagney 4
Start task cagney 1
End task cagney 4
Start task lemmon 5
End task lemmon 1
Start task lemmon 6
End task lemmon 1
Start task bergman 5
End task bergman 5
Start task bergman 1
End task bergman 5
```

Two depot appends are done for each input string, and you can see that though they start off on different tasks they always end up on the same task. You can also see that a partitioner doesn’t necessarily have to change tasks – it just ensures the task after the partitioner call is the correct one.

Here’s an example using a different partitioner:

```java
public class AllPartitionerExampleModule implements RamaModule {
  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.declareDepot("*depot", Depot.random());

    StreamTopology s = topologies.stream("s");
    s.source("*depot").out("*k")
     .each(Ops.PRINTLN, "Start task", "*k", new Expr(Ops.CURRENT_TASK_ID))
     .allPartition()
     .each(Ops.PRINTLN, "End task", "*k", new Expr(Ops.CURRENT_TASK_ID));
  }

  public static void main(String[] args) throws Exception {
    try(InProcessCluster cluster = InProcessCluster.create()) {
      RamaModule module = new AllPartitionerExampleModule();
      cluster.launchModule(module, new LaunchConfig(4, 4));
      String moduleName = module.getClass().getName();

      Depot depot = cluster.clusterDepot(moduleName, "*depot");

      depot.append("groucho");
      depot.append("harpo");
      depot.append("chico");
    }
  }
}
```

Running this prints:

```java
Start task groucho 2
End task groucho 3
End task groucho 1
End task groucho 0
End task groucho 2
Start task harpo 1
End task harpo 0
End task harpo 1
End task harpo 2
End task harpo 3
Start task chico 3
End task chico 0
End task chico 1
End task chico 2
End task chico 3
```

Just like how a [RamaOperation](https://redplanetlabs.com/javadoc/com/rpl/rama/ops/RamaOperation.html) implementation can emit many times, `allPartition` emits one time on every task in the module. From the output of this example you can see the processing of each depot record starts on a random task, but then ends on each of the four tasks in the module. `allPartition` should be used carefully since it becomes more expensive the larger your module. There are valid use cases for it though, like performing work on every task on a fixed interval via a [tick depot](depots.html#_tick_depots).

Partitioners ensure ordering is maintained between any two tasks. If task A sends events 1, 2, and 3 to task B across a partitioner, task B will receive and process those events in exactly that order. This is called "local ordering" and is a property you can leverage when implementing topologies. The same local ordering property exists between depot clients and individual depot partitions as well.

Partitioners take care of all the details of transferring your computation to another task in the most efficient way possible. The target task could be on another thread in the same process, on another process on the same machine, or on another machine altogether. Regardless of where the target task resides, partitioners will transfer the computation efficiently. Here’s a slightly more complicated topology to explain more what’s happening behind the scenes:

```java
StreamTopology s = topologies.stream("s");
s.pstate("$$p", PState.mapSchema(String.class, Integer.class));

s.source("*depot").out("*tuple")
 .each(Ops.EXPAND, "*tuple").out("*k", "*k2", "*v")
 .hashPartition("*k")
 .compoundAgg("$$p", CompoundAgg.map("*k", Agg.sum("*v")))
 .hashPartition("*k2")
 .compoundAgg("$$p", CompoundAgg.map("*k2", Agg.sum("*v")));
```

The first thing to note is every var reaches a point in the topology where it is no longer needed. `"*tuple"`, for example, is not used after the second line of the topology. Each var transferred across a partitioner increases the size of network packets and the cost of serialization/deserialization. So there’s no point in transferring `"*tuple"` across `hashPartition("*k")`. Rama analyzes the topology code to determine the exact set of vars needed after every point and only transfers needed vars across partitioners. Likewise, `"*k"` is no longer needed after the first `compoundAgg` call, so it’s not transferred with the second partitioner.

Partitioners also batch the transfer of events across partitioners as much as possible. This works a little bit differently in stream topologies versus microbatch topologies because stream topologies have much more stringent latency constraints. So microbatch topologies will tend to get better batching for partitioners, and this is one reason why microbatch topologies have higher throughput than stream topologies.

## Built-in partitioners

The partitioners available in Rama’s dataflow API are:

- [hashPartition](https://redplanetlabs.com/javadoc/com/rpl/rama/Block.html#hashPartition-java.lang.Object-): Chooses target task based on hash of input modded by number of tasks in module. Always emits exactly once.
- [allPartition](https://redplanetlabs.com/javadoc/com/rpl/rama/Block.html#allPartition--): Emits on every task in the module.
- [globalPartition](https://redplanetlabs.com/javadoc/com/rpl/rama/Block.html#globalPartition--): Transfers computation to task 0.
- [shufflePartition](https://redplanetlabs.com/javadoc/com/rpl/rama/Block.html#shufflePartition--): Chooses target task using random round-robin algorithm. The list of tasks is randomly ordered and the next task on the list is chosen each time the partitioner is called. When the list is exhausted the list of tasks is randomly ordered again. This ensures an even distribution of work across all tasks. Always emits exactly once.
- [directPartition](https://redplanetlabs.com/javadoc/com/rpl/rama/Block.html#directPartition-java.lang.Object-): Transfers computation to the task ID given as input.
- [pathPartition](https://redplanetlabs.com/javadoc/com/rpl/rama/Block.html#pathPartition-java.lang.String-com.rpl.rama.Path-): Takes as input a PState and a path and partitions according to the configured [key partitioner](pstates.html#_pstate_options) on the PState. This is inserted implicitly [when using select](#_implicit_partitioner_inserted_by_select) in a topology.
- [originPartition](https://redplanetlabs.com/javadoc/com/rpl/rama/Block.html#originPartition--): Only usable in [query topologies](query.html), this transfers computation back to the start task of a query topology invoke.

Most of these partitioners also have versions taking in a PState or depot as a first argument. These will restrict the range of tasks considered for partitioning to the tasks on which the PState or depot lives. PStates and depots from the same module exist on either every task or just on task 0 (if declared `global`). So you don’t need these versions of partitioners for colocated PStates and depots. As you’ll [see below](#_partitioning_to_pstates_or_depots_from_other_modules), they’re needed when interacting with PStates or depots from other modules.

## Custom partitioners

A custom partitioner function can be used with [customPartition](https://redplanetlabs.com/javadoc/com/rpl/rama/Block.html#customPartition-com.rpl.rama.ops.RamaFunction1-). Here’s an example of usage:

```java
public class CustomPartitionerModule implements RamaModule {
  public static class TaskOnePartitioner implements RamaFunction1<Integer, Integer> {
    @Override
    public Integer invoke(Integer numPartitions) {
      return 1;
    }
  }

  public static class MyPartitioner implements RamaFunction3<Integer, Integer, Integer, Integer> {
    @Override
    public Integer invoke(Integer numPartitions, Integer n1, Integer n2) {
      if(n2 > n1) return 0;
      else return numPartitions - 1;
    }
  }

  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.declareDepot("*depot", Depot.random());

    StreamTopology s = topologies.stream("s");
    s.source("*depot").out("*tuple")
     .each(Ops.PRINTLN, "Start task", "*tuple", new Expr(Ops.CURRENT_TASK_ID))
     .customPartition(new TaskOnePartitioner())
     .each(Ops.PRINTLN, "Next task", "*tuple", new Expr(Ops.CURRENT_TASK_ID))
     .each(Ops.EXPAND, "*tuple").out("*n1", "*n2")
     .customPartition(new MyPartitioner(), "*n1", "*n2")
     .each(Ops.PRINTLN, "Final task", "*tuple", new Expr(Ops.CURRENT_TASK_ID));
  }

  public static void main(String[] args) throws Exception {
    try(InProcessCluster cluster = InProcessCluster.create()) {
      RamaModule module = new CustomPartitionerModule();
      cluster.launchModule(module, new LaunchConfig(4, 4));
      String moduleName = module.getClass().getName();

      Depot depot = cluster.clusterDepot(moduleName, "*depot");

      depot.append(Arrays.asList(0, 1));
      depot.append(Arrays.asList(5, 2));
    }
  }
}
```

A partitioner function is a regular function implementation that takes in as arguments the number of partitions followed by any arguments passed to `customPartition`. In this example you can see one partitioner taking in no additional arguments and one partitioner taking in two arguments. A partitioner returns the partition number to which to transfer computation.

Running this prints:

```java
Start task #object[java.util.Arrays$ArrayList 0x3060082d [0, 1]] 1
Next task #object[java.util.Arrays$ArrayList 0x3060082d [0, 1]] 1
Final task #object[java.util.Arrays$ArrayList 0x3060082d [0, 1]] 0
Start task #object[java.util.Arrays$ArrayList 0x222759ce [5, 2]] 2
Next task #object[java.util.Arrays$ArrayList 0x222759ce [5, 2]] 1
Final task #object[java.util.Arrays$ArrayList 0x222759ce [5, 2]] 3
```

`TaskOnePartitioner` always partitions to task 1. This partitioner function would cause a runtime exception if used in a module with only one task (since it only has task 0). `MyPartitioner` partitions to the first task if the second argument is greater than the first, and it partitions to the last task otherwise. There are four tasks in this module, which accounts for the output.

When the `customPartition` variant that doesn’t take in a PState or depot as a first argument is called, `numPartitions` will be equal to the number of tasks in the module. If called with the variants that takes a PState or depot as a first argument, `numPartitions` will be the number of partitions for that PState or depot. When used with a PState or depot from another module, the returned partition number will be mapped by Rama to the appropriate task in the current module (even if the other module has more tasks). This is explored further in the next section.

## Partitioning to PStates or depots from other modules

In topology code, [localSelect](https://redplanetlabs.com/javadoc/com/rpl/rama/Block.html#localSelect-java.lang.String-com.rpl.rama.Path-) on PStates and [depotPartitionAppend](https://redplanetlabs.com/javadoc/com/rpl/rama/Block.html#depotPartitionAppend-java.lang.String-java.lang.Object-) on depots interact with the "local" partition for that PState or depot. For PStates and depots from the same module, the local partition is the one on the current task. But because PStates and depots from other modules (also known as "mirror PStates" and "mirror depots") could have more or less tasks than the current module, what "local" means is slightly different.

When a mirror PState or depot is declared on a module, Rama assigns each task of the current module to zero or more partitions of that mirrored object. If the mirrored object has more partitions than tasks on the current module, each task on the current module will be assigned to multiple partitions of that object. Because the number of tasks in a module must be a power of two, the distribution will be even. If the mirrored object has less partitions than tasks on the current module, then only some tasks will be assigned a partition for that object.

When multiple partitions for a mirrored object are assigned to the same task, Rama needs more information to know which partition to interact with when calling `localSelect` or `depotPartitionAppend`. This information is provided by using a partitioner on the mirrored object. For example:

```java
public class MirrorPStateExample {
  public static class Module1 implements RamaModule {
    @Override
    public void define(Setup setup, Topologies topologies) {
      setup.declareDepot("*depot", Depot.hashBy(Ops.IDENTITY));

      StreamTopology s = topologies.stream("s");
      s.pstate("$$p", PState.mapSchema(String.class, Long.class));

      s.source("*depot").out("*k")
       .compoundAgg("$$p", CompoundAgg.map("*k", Agg.count()));
    }
  }

  public static class Module2 implements RamaModule {
    @Override
    public void define(Setup setup, Topologies topologies) {
      setup.declareDepot("*depot", Depot.random());
      setup.clusterPState("$$other", Module1.class.getName(), "$$p");

      StreamTopology s = topologies.stream("s");
      s.pstate("$$p", PState.mapSchema(String.class, Long.class));

      s.source("*depot").out("*tuple")
       .each(Ops.EXPAND, "*tuple").out("*k", "*v")
       .hashPartition("$$other", "*k")
       .localSelect("$$other", Path.key("*k").nullToVal(0L)).out("*n")
       .each(Ops.PLUS_LONG, "*v", "*n").out("*newv")
       .hashPartition("*k")
       .localTransform("$$p", Path.key("*k").termVal("*newv"));
    }
  }

  public static void main(String[] args) throws Exception {
    try(InProcessCluster cluster = InProcessCluster.create()) {
      RamaModule module1 = new Module1();
      cluster.launchModule(module1, new LaunchConfig(8, 4));
      RamaModule module2 = new Module2();
      cluster.launchModule(module2, new LaunchConfig(4, 4));
      String module1Name = module1.getClass().getName();
      String module2Name = module2.getClass().getName();

      Depot depot1 = cluster.clusterDepot(module1Name, "*depot");
      Depot depot2 = cluster.clusterDepot(module2Name, "*depot");
      PState pstate = cluster.clusterPState(module2Name, "$$p");

      depot1.append("a");
      depot1.append("a");

      depot2.append(Arrays.asList("a", 10));
      System.out.println("Val: " + pstate.selectOne(Path.key("a")));
    }
  }
}
```

This example launches two modules. The first creates a simple PState recording the number of times each key was appended to its depot. The second uses `hashPartition` to query the PState from the first module and integrate that information into its own PState. Notice that the second module has four tasks while the first module has eight tasks. The `hashPartition` on `"$$other"` transfers computation to the correct task and sets the "partition index" in the context of the computation so it knows which partition to query.

Running `main` on this example prints:

```java
Val: 12
```

As you’ll see in the next section, a more concise way to query a mirror PState is with `select` rather than a partitioner plus a `localSelect` call. A full discussion of using mirrored PStates and depots can be found on the [Module dependencies page](module-dependencies.html).

## Implicit partitioner inserted by `select`

`select` in a topology partitions the computation first before doing a `localSelect`. It’s equivalent to using [pathPartition](https://redplanetlabs.com/javadoc/com/rpl/rama/Block.html#pathPartition-java.lang.String-com.rpl.rama.Path-) followed by a `localSelect`.

`select` follows the same rules as `pathPartition` or PState clients when it comes to choosing a target task. If the target PState has more than one partition, the path must begin with `key` and the target task will be chosen using the PState’s [configured key partitioner](pstates.html#_pstate_options) along with that key. If the target PState is global or is on a module with only one task, any path can be used.

Using `select` in a topology works for either colocated or mirror PStates. It’s generally the preferred way of interacting with mirror PStates because it’s more concise.

## Summary

Partitioners seamlessly move computation around a cluster without you having to worry about low-level routines like serialization or networking. They enable you to always have full control over where computation runs, and they do so while automatically batching and optimizing transfer between tasks.

Many topologies can get away without using partitioners at all. These topologies takes advantage of depot partitioning to start computation colocated with the PState partition they need to update. More sophisticated topologies which maintain multiple PStates partitioned by different values make use of partitioners heavily.