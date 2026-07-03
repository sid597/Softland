# Module Dependencies
> Source: https://redplanetlabs.com/docs/~/module-dependencies.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Dependencies between modules

Modules can use depots, PStates, and query topologies from other modules just as seamlessly as from their own. There are many reasons you may want to split up functionality across multiple modules. One reason is just basic decomposition: rather than have one module responsible for all functionality, you split the functionality across multiple modules. This also allows you to [update](operating-rama.html#_updating_modules) your application in finer-grained ways. Another reason could be because there are multiple teams in your company, so each team would work on independent modules. The depots, PStates, and query topologies exposed by a team are the "service interface" provided to other teams.

A depot, PState, or query topology used by another module is called a "mirror". Mirrors in Rama don’t store data locally – operations done on them always go through to the original.

On this page you will learn:

- How to declare and use mirror depots, PStates, and query topologies in a module
- Tradeoffs to consider when using mirrors
- How Rama enforces that dependencies between modules aren’t violated on launch/update/destroy
- How modules can have circular dependencies

All examples on this page can be found in the [rama-examples](https://github.com/redplanetlabs/rama-examples) project.

## Declaring and using mirrors

Mirrors are simple to declare using the methods `clusterDepot`, `clusterPState`, and `clusterQuery` in the definition of a module. Here’s an example:

```java
public class MirrorDeclarationsModule implements RamaModule {
  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.clusterPState("$$mirror", "com.mycompany.FooModule", "$$p");
    setup.clusterDepot("*mirrorDepot", "com.mycompany.FooModule", "*depot");
    setup.clusterQuery("*mirrorQuery", "com.mycompany.BarModule", "someQueryTopologyName");
  }
}
```

The first argument to these methods is the var to use within this module to refer to those mirrors in topology code. The second argument is the name of the owning module, and the third argument is the name of the object or query topology in the owning module. This example creates a mirror PState and mirror Depot from `"com.mycompany.FooModule"` and a mirror query topology from `"com.mycompany.BarModule"`. There’s no limit to the number of mirrors you can declare.

Using mirrors is similarly straightforward. You use them just as you would use colocated depots, PStates, and query topologies. Here’s an example:

```java
public class UsingMirrorsExample {
  public static class Module1 implements RamaModule {
    @Override
    public void define(Setup setup, Topologies topologies) {
      setup.declareDepot("*depot1", Depot.hashBy(Ops.IDENTITY));
      setup.declareDepot("*depot2", Depot.hashBy(Ops.IDENTITY));

      StreamTopology s = topologies.stream("s");
      s.pstate("$$p", PState.mapSchema(String.class, Long.class));
      s.source("*depot1").out("*k")
       .compoundAgg("$$p", CompoundAgg.map("*k", Agg.count()));

      topologies.query("qq", "*v1", "*v2").out("*res")
                .each(Ops.TIMES, new Expr(Ops.INC, "*v1"), "*v2").out("*res")
                .originPartition();
    }
  }

  public static class Module2 implements RamaModule {
    @Override
    public void define(Setup setup, Topologies topologies) {
      setup.clusterDepot("*depot2", Module1.class.getName(), "*depot2");
      setup.clusterPState("$$mirror", Module1.class.getName(), "$$p");
      setup.clusterQuery("*mirrorQuery", Module1.class.getName(), "qq");

      StreamTopology s = topologies.stream("s");
      s.source("*depot2", StreamSourceOptions.startFromBeginning()).out("*k")
       .select("$$mirror", Path.key("*k")).out("*count")
       .invokeQuery("*mirrorQuery", 3, 7).out("*queryResult")
       .each(Ops.PRINTLN, "Results:", "*count", "*queryResult");
    }
  }

  public static void main(String[] args) throws Exception {
    try(InProcessCluster cluster = InProcessCluster.create()) {
      RamaModule module1 = new Module1();
      cluster.launchModule(module1, new LaunchConfig(4, 4));
      String module1Name = module1.getClass().getName();
      cluster.launchModule(new Module2(), new LaunchConfig(2, 2));

      Depot depot1 = cluster.clusterDepot(module1Name, "*depot1");
      Depot depot2 = cluster.clusterDepot(module1Name, "*depot2");

      depot1.append("a");
      depot1.append("a");
      depot1.append("b");
      depot2.append("a");
      depot2.append("b");
      depot2.append("c");

      Thread.sleep(2000);
    }
  }
}
```

The `main` method starts an [InProcessCluster](https://redplanetlabs.com/javadoc/com/rpl/rama/test/InProcessCluster.html) and then launches two modules on it. `Module1` contains a depot, PState, and query topology that are mirrored by `Module2`.

The depot is used identically to a depot defined in the same module. There’s no limit to the number of subscribers to a depot, whether colocated subscribers or mirror subscribers.

`Module2` uses `StreamSourceOptions.startFromBeginning()` when sourcing the depot for an important reason. After module launch, it can take a little bit of time (generally less than 100 milliseconds) for the two modules to synchronize in order to resolve where the mirroring module will start processing a depot. That process is asynchronous and can still be resolving during the subsequent depot appends in this `main` method. So to ensure it picks up any data appended to `"*depot2"`, that start option is used. In general, production stream topologies that want to process just new data appended to a depot don’t care if they start processing appends 100 milliseconds after launch.

Unlike colocated stream topology subscriptions, depot appends do not coordinate on stream topologies from other modules. So the depot appends on `"*depot2"` in this example can complete while `Module2` is still processing them. For that reason the call to `Thread.sleep` is inserted to give `Module2` time to finish processing before shutting down the cluster.

Invoking the mirror query topology is also about the same as [invoking a colocated query topology](query.html#_invoking_colocated_query_topologies). The only difference is using a var to refer to the query topology rather than the query topology name.

The last mirror used in this example is the mirror PState `"$$mirror"`. A `select` call [on a colocated PState](pstates.html#_using_pstates_in_topologies) in a topology works by first partitioning to the task containing the relevant data and then doing a `localSelect`. A `select` call on the mirror does the same thing but works slightly differently behind the scenes.

Rama assigns each task of the current module to zero or more partitions of each mirrored depot or PState. If the mirrored object has more partitions than tasks on the current module, each task on the current module will be assigned to multiple partitions of that object. Because the number of tasks in a module must be a power of two, the distribution will be even. If the mirrored object has less partitions than tasks on the current module, then only some tasks will be assigned a partition for that object.

In this example, the source PState for `"$$mirror"` is deployed on four tasks, but `Module2` only has two tasks. So each task of `Module2` represents two tasks from `Module1`. When the `select` call partitions to the task in its module representing the relevant data, it also sets up context behind the scenes called the "mirror partition index" indicating which of the two tasks on `Module1` to query for `localSelect` calls.

An alternative to using `select` is to use an object partitioner followed by explicit `localSelect` calls, like this:

```java
.hashPartition("$$mirror", "*k")
.localSelect("$$mirror", Path.key("*k")).out("*count")
```

All the object partitioners like this will set the "mirror partition index" in the background context. If you want to query multiple mirror PStates from the same module, and those PStates are partitioned the same way, you can partition once using an object partitioner and then query each one in turn with `localSelect`.

[depotPartitionAppend](depots.html#_appending_to_depots_from_topologies) on a mirror depot likewise requires use of an object partitioner to know to which partition to send the append. [This section from Partitioners](partitioners.html#mirror-partitioners) goes into more detail about using object partitioners.

## Tradeoffs to consider when using mirrors

Using mirrors is seamless, but there are a few tradeoffs to consider when splitting an application across multiple modules.

The first is the loss of colocation. All queries to PStates and appends to depots will require network calls instead of being thread-local operations as they are with colocated PStates and depots. Though Rama will batch these `localSelect` and `depotPartitionAppend` appends as much as possible, the additional cost of network transfer will reduce performance compared to being colocated.

Another tradeoff is stream topologies on mirror depots don’t coordinate their processing with appends to that depot. An append on that depot with [AckLevel.ACK](depots.html#depot_client_api) can complete before a stream topology on another module finishes processing the append. Whether this coordination is needed depends on how your application will be interacting with that depot. There are plenty of cases where you don’t need that coordination.

Lastly, interacting with mirrors is a little different than interacting with colocated PStates in terms of event execution. In [this section](intermediate-dataflow.html#_yieldifovertime), we discussed how topologies break down into discrete events around the concept of "async boundaries". Unlike colocated `localSelect` calls, mirror `localSelect` calls are async boundaries. This can have subtle differences in semantics, such as in this topology code snippet:

```java
.localSelect("$$p1", Path.key("*k")).out("*v1")
.localSelect("$$mirror", Path.key("*k")).out("*v2")
.localSelect("$$p2", Path.key("*k")).out("*v1")
```

Because the `localSelect` call to `"$$mirror"` is an async boundary, the `localSelect` calls to the other two PStates happen in separate events. This means the values fetched from `"$$p1"` and `"$$p2"` won’t be synchronized to the same moment in time. If `"$$mirror"` were a colocated PState they would be in the same event and would be synchronized. This could be important for cases where your ETL updates multiple PStates in the same event and you want a consistent view of both of those PStates when querying.

It’s uncommon for this to matter, and it’s not something that influences how you decide if or how to split an application into multiple modules. And of course, all you have to do in this case to synchronize them is reorder the calls. This is a subtle semantic difference that’s good to be aware of in order to fully understand what’s happening in your topologies.

As a general rule, it’s good to separate non-intersecting functionality into separate modules. Features like "user profiles" and "pageview analytics" probably don’t interact very much, so the benefits of decomposition makes putting those in separate modules attractive.

## Enforcement of module dependencies on launch/update/destroy

Rama enforces module dependencies when performing module operations. If you try to [launch](operating-rama.html#_launching_modules) a module with a dependency that doesn’t exist, such as a specified PState not existing, the launch will fail. Likewise, if you try to add a non-existing dependency when [updating](operating-rama.html#_updating_modules) a module, the update will fail.

If you try to [destroy](operating-rama.html#_using_the_rama_cli) a module that is depended on by other modules, the destroy will fail. In order to destroy that module you will first need to either destroy the dependent modules or update them to no longer have the dependency. An update will also fail if you try to remove a depot, PState, or query topology depended on by another module.

## Circular dependencies

You can have circular dependencies betwen modules on a Rama cluster. Two modules could be deployed using mirrors on each other, or you could have more complex arrangements of more than two modules with circular dependencies. Although we don’t recommend designing applications from the start to have circular dependencies, you may find the need for them as an application evolves with more features over time.

Because new module launches enforce module dependencies, it’s not possible to create circular dependencies just with module launches. However, by updating an existing module you can create circular dependencies without ever violating any dependencies between modules. Here’s one way you can do it:

- Deploy Module A
- Deploy Module B with a dependency on something in Module A
- Update Module A to add a dependency on something in Module B

Let’s take a look at an example using this exact procedure:

```java
public class CircularDependenciesExample {
  public static class ModuleA_v1 implements RamaModule {
    @Override
    public void define(Setup setup, Topologies topologies) {
      setup.declareDepot("*depot", Depot.random());
    }

    @Override
    public String getModuleName() {
      return "ModuleA";
    }
  }

  public static class ModuleB implements RamaModule {
    @Override
    public void define(Setup setup, Topologies topologies) {
      setup.clusterDepot("*depot", "ModuleA", "*depot");

      StreamTopology s = topologies.stream("s");
      s.pstate("$$p", PState.mapSchema(String.class, Long.class));
      s.source("*depot").out("*k")
        .hashPartition("*k")
        .compoundAgg("$$p", CompoundAgg.map("*k", Agg.count()));
    }
  }

  public static class ModuleA_v2 implements RamaModule {
    @Override
    public void define(Setup setup, Topologies topologies) {
      setup.declareDepot("*depot", Depot.random());
      setup.declareDepot("*depot2", Depot.random());
      setup.clusterPState("$$mirror", ModuleB.class.getName(), "$$p");

      StreamTopology s = topologies.stream("s");
      s.source("*depot2").out("*k")
        .select("$$mirror", Path.key("*k")).out("*count")
        .each(Ops.PRINTLN, "Mirror count:", "*k", "*count");
    }

    @Override
    public String getModuleName() {
      return "ModuleA";
    }
  }

  public static void main(String[] args) throws Exception {
    try(InProcessCluster cluster = InProcessCluster.create()) {
      cluster.launchModule(new ModuleA_v1(), new LaunchConfig(2, 2));

      RamaModule moduleB = new ModuleB();
      cluster.launchModule(moduleB, new LaunchConfig(4, 4));
      String moduleBName = moduleB.getClass().getName();

      cluster.updateModule(new ModuleA_v2());

      Depot depot = cluster.clusterDepot("ModuleA", "*depot");
      Depot depot2 = cluster.clusterDepot("ModuleA", "*depot2");

      depot.append("a");
      depot.append("a");
      depot.append("b");

      Thread.sleep(2000);

      depot2.append("a");
      depot2.append("b");
    }
  }
}
```

`RamaModule` has an optional method `getModuleName` that can be used to explicitly set the module name to something other than the class name. In this example it’s used to create two versions of `"ModuleA"`. The first version is launched containing one depot, and `"ModuleB"` is then launched with a dependency on that depot. `"ModuleA"` is then updated with a new version that adds a dependency on a PState from `"ModuleB"`. After the circular dependencies are set up, the `main` method does some depot appends to show that everything works. Running `main` prints:

```java
Mirror count: a 2
Mirror count: b 1
```

On a real cluster, you wouldn’t necessarily need to override `getModuleName` in order to create circular dependencies. The new version of a module could just be the same class from a later commit of the repository with updated code. But in the context of testing with `InProcessCluster`, where everything is running in the same process with the same classpath, you’ll need to use the technique shown in this example to create circular dependencies.

## Summary

Mirrors are seamless to use and allow you to decompose an application into multiple modules to separate independent functionality and/or mimic the organizational structure of your company. Rama is designed from the ground up to support any kind of module layout efficiently. Though there are tradeoffs to consider when using mirrors, as you gain experience with Rama you’ll find reasoning about how to split up functionality across multiple modules to generally be a straightforward process.