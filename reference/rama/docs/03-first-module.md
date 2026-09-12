# Tutorial 1: First Module
> Source: https://redplanetlabs.com/docs/~/tutorial1.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Building your first module: Hello, World

Distributed web applications can resemble a creature from the imagination of [Mary Shelley](https://en.wikipedia.org/wiki/Mary_Shelley). "You do *what* with Postgres?" "Kafka’s doing *that* in your cluster?" And the appeal is understandable; no one
ever accused Dr. Frankenstein of living a dull life.

Still, not everyone is cut out for the mad genius lifestyle. Perhaps you
yourself have wondered what it would be like to live a more predictable life, to
use a single tool built from the ground up to handle your distributed needs
without requiring two tons of duct tape and shell scripts.

Whether you’re building the next Twitter or bootstrapping a micro-niche SaaS,
Rama is a distributed-first scalable programming platform that you can use to build the
entire data layer of your application.

As you learn to build Rama applications, you might find your instincts fighting
the design decisions you encounter. To ease your transition, this tutorial takes
pains to identify differences with standard development practice and to show the
rationale for the choices. Some of the differences won’t fully
make sense until later in the tutorial. But like astronauts adjusting to zero-gravity,
you’ll soon find yourself adjusting to your new environment and feeling
more powerful than ever.

Let’s start learning. To begin, you’ll learn the basics of writing and running a Rama application. Just as Rama is new and exciting, your application will do something new and exciting: print *hello world*.

## Setting up your sandbox

To get started, you’ll need to clone the [rama-examples](https://github.com/redplanetlabs/rama-examples) project on Github.

The `rama-examples` project uses Maven to manage dependencies. Starting a process and loading all the Rama classes takes a little while (30 seconds to a minute), so the easiest way to try out the examples is via an interactive shell. `rama-examples` is configured so you can easily launch a [Groovy](https://groovy-lang.org) shell like so:

```java
mvn compile
mvn gplus:shell
```

You must run `mvn compile` first so the examples are available inside the shell. From this shell you’ll be able to easily run the code examples in this tutorial. Note the the Groovy shell only works with Java 17 or below.

## Running a module on a cluster

Now that you’re all set up, let’s work with some code! Here’s how to run an application that prints *hello world*:

```java
package rama.examples.tutorial;

import com.rpl.rama.*;
import com.rpl.rama.module.*;
import com.rpl.rama.ops.Ops;
import com.rpl.rama.test.*;

public class HelloWorldModule implements RamaModule {
  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.declareDepot("*depot", Depot.random());
    StreamTopology s = topologies.stream("s");
    s.source("*depot").out("*data")
     .each(Ops.PRINTLN, "*data");
  }

  public static void main(String[] args) throws Exception {
    try (InProcessCluster cluster = InProcessCluster.create()) {
      cluster.launchModule(new HelloWorldModule(), new LaunchConfig(1, 1));

      String moduleName = HelloWorldModule.class.getName();
      Depot depot = cluster.clusterDepot(moduleName, "*depot");
      depot.append("Hello, world!!");
    }
  }
}
```

To run the example, try this command from the Groovy shell:

```java
groovy:000> rama.examples.tutorial.HelloWorldModule.main(null)
```

The first time you run an `InProcessCluster`, the JVM will take 30 seconds to a minute loading all the Rama classes. Once everything has finished loading, you should see `Hello, world!!` printed. Subsequent examples you run in the same shell will run very quickly.

At a macro level, here is what the code does:

1. It creates an *in-process cluster*. Clusters are the execution environment.
2. It defines a Rama *module*, `HelloWorldModule`. Modules are executables.
3. It *launches* the module on the cluster.
4. It *appends* data to the module’s *depot*.
5. The appended data is *sourced* from the depot in a *streaming topology*.
6. The topology performs an *operation* on the data, which results in the string
   being printed.

By the end of this tutorial you’ll have a precise idea of what all this means.
The first step to getting there is explaining the larger context in which Rama
applications live.

## The big picture

Rama *integrates* and *generalizes* data ingestion, processing, indexing, and querying. The "integrates" aspect means it encompasses a suite of responsibilities that aren’t normally packaged in a single tool. The "generalizes" part means Rama is unrestricted in the kinds of applications it can build, including applications that require huge scale. This means Rama can build complex application backends on its own in a small amount of code that previously required dozens of tools and a lot of code. In many cases the code difference is over 100x.

Rama’s programming model is based on *event sourcing* and *materialized views*. Rather than manipulate the current state of the world, as you do with databases, with event sourcing you append new data to an unindexed log. From that log you materialize views, which are indexed datastores, to serve the queries needed by your application.

Most databases are fundamentally flawed because being a source of truth and being an indexed store that answers queries are fundamentally in conflict. It’s impossible for one datastore to do both of these well. A source of truth demands complete data normalization to achieve data consistency. Performance requirements are non-negotiable and eventually force [denormalization](https://en.wikipedia.org/wiki/Denormalization), which throws data consistency out the window.

Event sourcing solves this by separating the source of truth from the indexed datastores that serve queries. Since the source of truth is an unindexed log, data can be normalized completely. Then you’re free to make as many materialized views, denormalized as much as necessary, to handle application queries.

There are huge additional advantages that come from event sourcing. If you deploy a bug that corrupts your views, you can always recompute those views from scratch from the raw data in the logs. This is a capability you don’t have with databases where bugs trample over the state of the database. Event sourcing also provides a natural audit trail for your application, which is invaluable for debugging or future analysis.

While traditionally event sourcing is only for asynchronous use cases, due to the consumption of a queue system being uncoordinated with a client doing appends, this is not the case with Rama. Because Rama is an integrated system, clients doing appends can optionally coordinate with downstream processing so that an append only returns to the client when all downstream processing and updates to materialized views have completed. This makes Rama suitable for highly interactive use cases as well as asynchronous use cases.

As you make your way through the tutorial, you’ll learn the details of how applications are built with this programming model. Let’s start by looking at the Rama execution environment: the *cluster*.

## Clusters

The example code’s `main` method starts an *in-process cluster* with
`InProcessCluster cluster = InProcessCluster.create()`. There are a couple
things to unpack here: what’s a cluster, and what’s an in-process cluster?

Let’s start by exploring what we mean by *cluster*, both conceptually and
concretely. The term is used with slightly different meanings in different
contexts, so let’s define it precisely.

Conceptually, a *cluster* is Rama’s execution environment. Similar to how you
start a program by telling an operating system to run it, you start a Rama
module by telling a cluster to *launch* it (launching modules is covered in the
next section).

Concretely, a cluster consists of a group of cooperating, networked machines
(*nodes*); this is the general usage of the term "cluster" you’ve probably
encountered. A Rama cluster refines this notion by specifying three different
roles for the processes running on the machines in a cluster:

- Conductor
- Supervisor
- Worker

*Worker* processes are responsible for, well, doing work – they perform the
compute operations you specify. There can be multiple workers per machine.
Workers are managed by Supervisors.

*Supervisor* processes monitor worker health and start or stop worker processes
according to the specification you’ve given for how work should be distributed.
There’s one supervisor per machine.

The *Conductor* orchestrates the steps involved in launching and updating modules.

Here’s what a Rama cluster could look like with two modules deployed, "SocialGraphModule" and "TimelineModule":

For small/medium scale applications, a Rama cluster could be deployed onto a single node running both the Conductor and Supervisor. Larger-scale deployments typically run the Conductor on its own node with a Supervisor on each other node. Each module is partitioned across one or more workers in order to parallelize its work. Everything a module does, such as data ingestion via ["depots"](depots.html), data processing via ["topologies"](tutorial5.html), and indexing via ["PStates"](pstates.html), is colocated within the same set of worker processes.

Rama [gives you tools](operating-rama.html) for creating clusters in a data center, on the cloud, or on your local machine. You can also create in-process clusters.

An *in-process cluster* (IPC) is a virtual cluster that runs within a single JVM
process. Starting and stopping a fleet of machines and their conductor,
supervisors, and worker processes takes time. By using an in-process cluster
instead of a real cluster, you get faster feedback, which is better for
learning. In-process clusters are also used for unit testing the behavior of modules.

In the example above, we create an in-process cluster and launch a module on it
with:

```java
InProcessCluster cluster = InProcessCluster.create()
cluster.launchModule(new HelloWorldModule(), new LaunchConfig(1, 1));
```

You’ll continue to see this as we work through examples. Just keep in mind that IPCs are a development tool, and that you’re unlikely to use them outside of tests and tutorials. In a real project, you wouldn’t have `main` methods on your modules and your use of IPCs would be in tests.

To sum up: a Rama cluster is a group of cooperating machines and processes
responsible for performing the work defined in Rama modules. Modules run on
clusters. We use in-process clusters for learning and development because they
provide tight feedback loops.

All of these topics (conductors, workers, supervisors, modules) will be explained fully throughout this tutorial, but hopefully this gives you a decent idea of what’s going on.

## Modules

Modules are where we access data and manipulate it. If a cluster is the
execution environment, a *module* is the executable. You create modules to do
work. We can understand modules in terms of their *definitions* and their
*runtime instantiation*.

To define a module, you create a class that implements the `RamaModule`
interface, which consists of the `define` method. `define` takes two arguments, `setup` and
`topologies`, which you’ll learn more about in the next section. In the Hello
World example, we define a module named `HelloWorldModule`.

Data enters Rama modules through *depots*. [Depots](depots.html) exist at the boundary between the external world and your system: clients
*append* data to depots, and module *ETLs* read the data, perform work with it,
and index it. (*ETL* stands for *extract-transform-load*, which refers to the
general process of reading data from data stores, performing computations on it,
and storing the result.) Queries read and transform stored data for consumption
by user interfaces, APIs, and other systems that rely on your data.

Depots are defined within and belong to modules. `HelloWorldModule` defines a depot with `setup.declareDepot("*depot", Depot.random());`. The first argument `"*depot"` is the depot’s name. The `*` at the beginning of the name is important: strings beginning with `*` are *variables* that can be referred to later on in topology code. A depot’s name always begins with a `*`. The second argument, `Depot.random()`, specifies the depot’s partitioning scheme; more on that later.

The next few lines define a topology:

```java
StreamTopology s = topologies.stream("s");
s.source("*depot").out("*data")
 .each(Ops.PRINTLN, "*data");
```

This creates a [stream topology](stream.html). Topologies are a world unto themselves that
we’ll dedicate time and space to, but for now you can think of this topology as
a kind of process with streaming performance and consistency characteristics: it
operates on data as the data is received, rather than periodically performing
batch operations. The overall effect is that, as data is appended to `"*depot"`,
it’s processed by the topology such that every append is printed.

There are other kinds of ETLs with different performance and consistency
characteristics, and we’ll learn about those in a later section.

So that’s what’s involved in defining a module. Now let’s look at their runtime
instantiations.

## Launching a module

|  |  |
| --- | --- |
|  | This section explains how to launch modules on an in-process cluster. Launching modules on a real cluster is done through Rama’s [command-line client](operating-rama.html). The concepts are the same, though there are more options for launch on the command-line like replication factor and config overrides. |

After crafting your beautiful new module, you’ll want to run it. To do that, you
*launch* it on a cluster. When you launch a module, you register it with a
cluster and you give the cluster some specifications around resource allocation.
In the example we do that with this code:

```java
cluster.launchModule(new HelloWorldModule(), new LaunchConfig(1, 1));
```

`cluster` is an instance of an `InProcessCluster` and its `launchModule` method
takes two arguments, a module instance and a `LaunchConfig` instance. The launch
config specifies how many partitions a module should have, as well as the number
of workers and threads that should be dedicated to those partitions. In upcoming
sections you’ll learn more about what it means to specify a launch config and
how the choices you make in a launch config affect the runtime characteristics
of your module.

It’s worth pointing out that the `LaunchConfig` strains the "execution environment / executable" metaphor a little bit, but you can consider it to be similar to how you can specify the max heap size and other
resource constraints when starting a Java process. Runtime environments can
provide knobs for you to tune an executable’s resource consumption, it’s just
that in Rama you’re required to specify these settings.

Launching a module on a real cluster results in Java processes getting created on your cluster’s machines – these are the *worker processes* mentioned earlier. These worker processes run the module’s code, and they’ll start an event loop that listens for depot appends to kick off ETLs to process those appends. (In an IPC these worker processes are simulated as threads – otherwise IPC would be incredibly slow!)

Most programming languages specify some kind of entry point for a program, some
variation of `main` that tells the runtime environment what sequence of steps
to take. Your module has no such entry point; instead, you create a kind of
event-driven dataflow pipeline that processes data as added.

## Appending data to depots

After you’ve launched a module you can start appending data to its depots.
Appending data to depots is the main means of conveying information from the
outside world into your system. Modules can also perform further depot appends,
both to their own depots and to depots belonging to other modules.

In the example, we appended data to the depot thusly:

```java
String moduleName = HelloWorldModule.class.getName();
Depot depot = cluster.clusterDepot(moduleName, "*depot");
depot.append("Hello, world!!");
```

Here, we’re retrieving a depot object by looking it up with `cluster.clusterDepot`, and then appending to it directly with `depot.append`. After you append data to a depot, the ETLs sourcing that depot will receive the new data and process it.

## Processing data with ETLs

When you define an ETL, you specify which depots it should *source* – you can
see this in the example with `s.source("*depot")`. When an ETL sources a depot,
it effectively listens for new depot appends and reactively performs
*operations* on that data. In the example, we only perform the `Ops.PRINTLN`
operation.

In real ETLs, you usually produce indexes that can satisfy application queries. Indexes are called ["partitioned states"](pstates.html) (PStates) in Rama and they can be fine-tuned to precisely the shapes needed for handling all your querying needs. You’ll learn a lot more about this later.

We’re going to spend a lot of time covering exactly how to define ETLs. They embody a programming style that may be new to you, so they’ll take some time getting used to. This can be daunting, but it’s also exciting: ETLs provide immense expressive power, allowing you to concisely describe distributed operations at a level of abstraction that isn’t possible with any other tool. By learning to write them, you’ll not only become proficient at Rama, you’ll expand your mental programming toolkit. In the same way that learning about declarative programming is useful if you’ve only ever been exposed to imperative programming, learning about how Rama ETLs work will give you a new way to think about how to solve problems in your work, even outside of using Rama.

## Querying data

While processing and indexing data can be a life-enriching activity in and of itself, most of the time you’ll need to then retrieve the data so you can use it. Ultimately, you’re building a system that takes source data and processes it for consumption. ETLs perform some of that processing, indexing the results, and queries might perform further computation.

As in other systems, you make tradeoffs between precomputing some results and deriving some results at query time. Rama has two ways of doing queries: [point queries](pstates.html#_using_pstate_clients), where you ask for one piece of information from one index on one module, and predefined queries, where your query is itself a full distributed computation that can talk to many indexes on many modules. Predefined queries are defined using Rama’s [query topology](query.html) API. Rama differs from other systems in that you’ll use the same computation API for both ETLs and query topologies. Being able to use the same API and concepts in both contexts is another way that simplifies the process of developing distributed applications, as you’ll see. We’ll look at querying PStates more on [the next page](tutorial2.html).

## Summary

In this section, you learned about Rama basics:

- Rama applications run on *clusters*
- *Modules* are Rama’s executables
- You *launch* a module on a cluster
- Modules contain *depots*, *ETLs*, *PStates*, and *query topologies*
- You *append* data to depots
- ETLs *source* data from depots and perform operations on that data