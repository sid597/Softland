# Tutorial 2: Depots, ETLs, and PStates
> Source: https://redplanetlabs.com/docs/~/tutorial2.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Doing work in Rama: Depots, ETLs, and PStates

In the last section you got your first taste of how to do work with Rama, but it
wasn’t very meaningful work. Unless you’re employed by Big Hello World, it’s
likely that you need to write more substantial applications. Applications need
to gather data, process it, and store it for efficient retrieval, and this
section will introduce you to the tools you’ll need to do that. (Those of you
working for Big Word Count: rejoice, for we are going to start with a word count
app as an example.)

It isn’t sufficient, however, to merely show you what the tools are and how to
use them if you’re to become productive with them. In the same way that Marie
Kondo’s advice to fold your socks and thank your trash makes more sense within a
larger context of full-life transformation, Rama’s tools make more sense within
a larger context of application development. We’ll therefore look at the *why*
of Rama’s tools in addition to the *how*.

## Storing results in PStates

In the last example, you looked at how to gather input data by appending to
depots. You had a glimpse of how to process data by creating an ETL that simply
printed the customary greeting between programmers. Most applications, however,
need to store processed data somewhere so that it can be retrieved and used by
clients or other parts of the system. Rama’s construct for such data storage is
the [partitioned state](pstates.html) (abbreviated as *PState*), and this page will introduce
you to PState fundamentals.

So what exactly is a PState? PStates are a little similar to tables in an RDBMS
in that both are named, discrete data containers within a larger data management
system (unlike in, say, Redis, which is a key-value store where all data lives in
the same container).

That surface characteristic is where their similarities end. PStates differ from
tables in that they are *arbitrarily compound data structures of arbitrary size*, meaning
that you can use them to store as much data as you need,
organized however you need. It’s possible, for example, to structure a PState as
a map where the values are lists, and the lists contain sets, and so on. You
store data in the shape you want to use it, without having to deal with the
impedance mismatches present when working with many databases.

To illustrate this idea, let’s look at some examples. Let’s start with a
stripped-down word count, one that doesn’t even include a tokenizing step, so we
can focus on PStates. Here’s the code:

```java
package rama.examples.tutorial;

import com.rpl.rama.*;
import com.rpl.rama.module.*;
import com.rpl.rama.test.*;

public class SimpleWordCountModule implements RamaModule {
  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.declareDepot("*wordDepot", Depot.random());

    StreamTopology s = topologies.stream("wordCountStream");
    s.pstate("$$wordCounts", PState.mapSchema(String.class, Long.class));

    s.source("*wordDepot").out("*token")
     .hashPartition("*token")
     .compoundAgg("$$wordCounts", CompoundAgg.map("*token", Agg.count()));
  }

  public static void main(String[] args) throws Exception {
    try (InProcessCluster cluster = InProcessCluster.create()) {
      cluster.launchModule(new SimpleWordCountModule(), new LaunchConfig(1, 1));
      String moduleName = SimpleWordCountModule.class.getName();
      Depot depot = cluster.clusterDepot(moduleName, "*wordDepot");
      depot.append("one");
      depot.append("two");
      depot.append("two");
      depot.append("three");
      depot.append("three");
      depot.append("three");

      PState wc = cluster.clusterPState(moduleName, "$$wordCounts");
      System.out.println("one: " + wc.selectOne(Path.key("one")));
      System.out.println("two: " + wc.selectOne(Path.key("two")));
      System.out.println("three: " + wc.selectOne(Path.key("three")));
    }
  }
}
```

This definition includes parts you’ve seen before: in `define`, you create a
[depot](depots.html) (named `"*wordDepot"`) and a [streaming topology](stream.html) (assigned to `wordCountStream`). In `main`,
you follow the same basic pattern of launching a module and appending to
a depot.

### Creating a partitioned state

The module definition also includes a new bit:

```java
s.pstate("$$wordCounts", PState.mapSchema(String.class, Long.class));
```

You create a PState by calling the `pstate` method on a topology instance. In
this case, you’re creating a PState named `"$$wordCounts"` with a schema of
`PState.mapSchema(String.class, Long.class)`. The `"$$wordCounts"`
identifier is used to reference the PState both from within a topology and from
outside it, and you’ll see examples of how PStates are referenced throughout the
tutorial.

As for the schema, that defines the data structures used to store your data. The
schema `PState.mapSchema(String.class, Long.class)` specifies that the PState
is a map, storing data as key-value pairs – perfect for associating every word
with its count. As you go through the tutorial you’ll encounter more kinds of
schemas.

PStates belong to topologies. When you call `s.pstate`, the PState you create is
associated with the `s` topology. Even though PStates belong to topologies, you
must give them names that are unique within a module.

PStates are essentially materialized views of depots, with depots being the
source of truth. In this world, it doesn’t make sense for anything but the
owning topology to write to them.

### Updating PStates

We now have a PState to store some data, but how do you actually modify the data
in it? How do you perform CUD (create, update, delete; we’ll save read for later)
operations?

This is actually a deep topic, and it would be overwhelming to cover it all at
this point, so we’ll start with just the parts that will help build the
foundations of your mental model for how to do work in Rama.

Let’s start with the core concept governing how you update PStates: you write
ETLs that set up a relationship between depots and PStates, creating a pipeline
that updates PStates as new entries are added to the depot. For example, this
abstract timeline shows how the `"$$wordCounts"` PState changes as words are
appended to the depot:

| Time | Event | `"$$wordCounts"` |
| --- | --- | --- |
| 1 | `append("one")` | {"one": 1} |
| 2 | `append("two")` | {"one": 1, "two": 1} |
| 3 | `append("two")` | {"one": 1, "two": 2} |
| 4 | `append("three")` | {"one": 1, "two": 2, "three": 1} |
| 5 | `append("three")` | {"one": 1, "two": 2, "three": 2} |
| 6 | `append("three")` | {"one": 1, "two": 2, "three": 3} |

Let’s examine the code which achieves this result to see how the core idea of
creating data pipelines is implemented:

```java
s.source("*wordDepot").out("*token")
 .hashPartition("*token")
 .compoundAgg("$$wordCounts", CompoundAgg.map("*token", Agg.count()));
```

There are three distinct steps in this ETL:

1. `.source("*wordDepot").out("*token")`: This establishes how data added to the
   depot can be referenced in the ETL. Its meaning is, on an ongoing basis,
   read entries from `"*wordDepot"` and name each entry `"*token"`. Each call to
   `depot.append` produces a new depot entry.
2. `.hashPartition("*token")`: This *relocates* the rest of the
   computation using a [partitioner](partitioners.html) – perhaps to another thread, perhaps to another machine. We’ll
   cover this more when we discuss distributed programming.
3. `.compoundAgg("$$wordCounts", CompoundAgg.map("*token", Agg.count()))`: This
   is what updates the PState. PStates are data structures, and aggregators are
   a high-level way of specifying where and how to write to specific locations
   in the data structure. They let you write your update in the shape of the
   data you’re trying to produce. `CompoundAgg.map("*token", Agg.count())`
   means, "traverse this data structure to the location indicated by `"*token"`
   and perform `Agg.count()`." `Agg.count()` increments whatever value is found
   there. If the location doesn’t yet have a value, it sets the value to 1.

You can probably feel a breeze from all the hand waving in that last
step. For an explanation of the details of what’s going on here, you can
jump ahead to [this guide to aggregators](aggregators.html).

You can also update a PState’s values by using *path transforms*. You’ll learn
about [those](paths.html) later, too. As a preview: paths can navigate by key or index, but
also all elements, filter, transformed views, subsequences, and more.

### Querying PStates

At the end of the code example is this:

```java
PState wc = cluster.clusterPState(moduleName, "$$wordCounts");
System.out.println("one: " + wc.selectOne(Path.key("one")));
System.out.println("two: " + wc.selectOne(Path.key("two")));
System.out.println("three: " + wc.selectOne(Path.key("three")));
```

The first line, `PState wc = cluster.clusterPState(moduleName,
"$$wordCounts");`, assigns a reference to the PState to `wc`. We then use
`wc.selectOne` to retrieve the count of each word we’ve appended to the depot,
and then print it.

It’s not obvious from this example, but queries retrieve data from arbitrary
points within the PState’s structure using *paths*. If you’re familiar with
XPath paths or CSS selectors, Rama paths serve a similar function: they let you
specify a node or collection of nodes from an arbitrary graph.

For example, if you constructed a PState that mapped book titles to word counts
then you could use a path to retrieve the count for a word within a particular
book; if your PState included `{"Frankenstein; or, The Modern Prometheus":
{"rejoiced": 3}}`, then the following query would return `3`:

```java
wc.selectOne(Path.key("Frankenstein; or, The Modern Prometheus", "rejoiced"))
```

Rama’s PStates allow you to capture data in arbitrary structures. Being able to
shape the data in your data store however you want is powerful, but to make it
useful you need to be able to navigate the structures in a way that’s easily
comprehensible. Paths let you do that. Here’s an example of a slightly more
complex path:

```java
wc.selectOne(Path.key("Lord of the Rings").subselect(Path.sortedMapRange("Frodo", "Gandalf").mapVals()).view(Ops.SUM))
```

Suppose this query ran on a PState containing these contents:

```java
{"Lord of the Rings":
   {"Elf": 1,
    "Frodo": 2,
    "Frodo2": 3,
    "Gandalf": 4
   }}
```

|  |  |
| --- | --- |
|  | Because Java doesn’t have an object literal notation, we’ll be using JSON inspired syntax to concisely convey the shapes that data structures might take. Note that Rama does not actually store data as JSON. |

This gets the sum for all words alphabetically between `"Frodo"` and `"Gandalf"` for the book `"Lord of the Rings"`. In this case, it returns `5`. Don’t worry if the specifics of how this path works feel hazy right now. You can get a complete understanding of paths later from [this page](paths.html).

### More PState capabilities

There are other powerful capabilities of PStates important for many applications. These include "subindexing", which allows inner data structures in PStates to be of huge size, and "fine-grained reactive queries", which allows for continuous queries that are pushed "diffs" about changes to the result of the query. These capabilities and more are described on [this page](pstates.html).

### Rama programming

The word count example, simple as it is, demonstrates the fundamentals of how
you do meaningful work in Rama:

1. You gather data by appending to depots.
2. You process data by writing ETLs that read from one or more depots and perform
   computations…​
3. …​and you store the results in PStates for later retrieval.

Figure 1. The basic process for doing work in Rama: 1) Append records to depot 2) ETLs read records from depot 3) Transform data and store in PStates

Note that modules can have multiple depots and ETLs, and ETLs can read from
multiple depots. The image below depicts a module with two depots and two ETLs.
The left ETL reads from one depot and writes to two PStates, while the one on
the right reads from both depots and writes to one PState:

This is the basic form that Rama development takes. Whether you’re building the
next Twitter or mining terabytes of sales data or, well, counting words, you’ll
be appending data to depots, processing that data, and putting the results in
PStates.

This deserves elaboration: Rama is designed so that you can meet *all* of your
data handling needs *within a single system*. Not just within a single data
store — within a single system, meaning that storage and computation
participate in the same design and are part of a single coherent model; you
don’t have to glue disparate systems together. You don’t have to figure out how
to manage some unholy combination of Kafka, Cassandra, and Redis; you get to
just use one tool. If you do need to integrate with other systems, for legacy reasons
or otherwise, Rama provides [a simple and powerful integration API](integrating.html)
for that purpose.

Understanding why Rama was designed this way and the implications this design
has for how you write applications will help you become proficient at building
your own systems. It will also help you understand future topics like
partitioning because you’ll be able to put them into context. Therefore, the
next section will expand on Rama’s design and how it fits into the broader
context of application development.

## What is a data system, really?

Rama is designed to make it substantially easier for you to create data systems. At the most basic level, a data system enables *questions* to be asked on your data in a *reasonable* amount of time. There are many properties you care about for each question you want to ask:

- **Timeliness**: how long it takes for data to be included in the results of a question
- **Latency**: how long it takes to receive an answer to a question
- **Accuracy**: how accurate an answer is – approximations are sometimes needed due to the fundamental limits of computation. Sometimes an answer can be more accurate by increasing the latency of the answer.
- **Throughput**: how many questions can be answered per second
- **Scalability**: how throughput is increased by adding resources to the system

The kinds of questions you ask can vary tremendously in both form and function. Here’s a tiny sample of the kinds of questions a data system might answer:

- What is Alice’s birthday?
- What is the bank account balance of James?
- How many pageviews did "http://foo.com" receive between January 3rd, 2012 and March 16th, 2021?
- What URLs best match the keywords "how to write scalable applications"?
- Who are the last 20 people to follow Samantha?
- What five books should be recommended to Joe to maximize sales?

Some of these questions are simple retrievals of information previously seen, while others can be complex aggregations of many pieces of data seen over a long period of time. Ultimately, every aspect of how a data system functions comes down to two things: capturing new data, and answering questions on data previously captured.

For most questions you want to receive an answer as fast as possible. So some sort of pre-built index is needed so you can get at the information you need quickly without having to sift through irrelevant data. So at a high level, every data system (existing systems as well as Rama) looks like this:

There is a ton of variety in how each of these pieces can work: how you ingest new data, how you process data to compute and maintain indexes, how indexes are structured, and how computation is performed to utilize indexes to quickly answer questions.

One common architecture for a data system is:

- The primary data store is a relational database (RDBMS)
- A key-value store like Redis is used as a cache or queue
- Custom programs coordinate everything: processing data, sending CRUD requests to the data stores, interacting with external clients, etc.

With an RDBMS, the way you organize your data for storage frequently does not match the way you organize your data for consumption, so you must reorganize it into useful data structures on retrieval, whether through your SQL queries or through further processing in your program. If these operations become expensive, then you might denormalize your tables or introduce caches in your system (materialized views, Memcached, Redis) to improve performance. Your coordinating program then becomes responsible for ensuring consistency across these different views. The complexity of this can be daunting, and corruption involving inconsistent views is often the result.

It can also be difficult to scale an RDBMS, so NoSQL databases are frequently used instead. In this model only the index is different: custom programs are still built and operated for processing data and interacting with clients. Each NoSQL database provides a "data model" which is a specific way to organize data for consumption, like "key/value", "graph", "document", or "column-family based". Each of these data models can only handle some use cases, and multiple NoSQL databases are often needed to be able to be able to support all an application’s use cases. The cost and complexity of operating multiple distributed databases is significant, and future use cases may require new distributed databases to be adopted or built from the ground up.

Traditional data systems require developers to tetris many different systems together – frequently dozens – in order to produce an application that can answer questions with the appropriate timeliness, accuracy, latency, and throughput. And it works, sort of. As you combine systems though, it becomes more complicated and difficult to coordinate them. Each system has unique abstractions and interfaces. Getting all the pieces to interact correctly, such that your data is consistent and an outage in one system doesn’t bring everything else down (to name a couple issues) is hard.

The key breakthrough of Rama is generalizing and integrating every aspect of data systems to such an extent that you can build entire data systems end-to-end with just Rama:

- Data is stored in [PStates](pstates.html), organized into whatever data structures are useful for consumers. Whereas a database (either RDBMS and NoSQL) offers one particular data model, each PState can be *any data model*. Ultimately a data model is just a particular data structure: "key/value" is a map, "column-family based" is a map of sorted maps, and "document" is a map of maps. Each PState you create can be whatever arbitrary combination of data structures you want, and you can have as many PStates as you need.
- Rama’s [stream](stream.html) and [microbatch](microbatch.html) topologies enable PStates to be built using arbitrary, fully scalable distributed computation. Rama’s topology API is Turing-complete and extremely flexible.
- New data enters Rama through [depots](depots.html), and depots can be consumed by as many topologies as you need to materialize as many PStates as you need.
- Rama’s query API for retrieving information from PStates is fast and flexible. A query can be as simple as retrieving one piece of data from one partition of one PState, or it can be an [on-demand distributed computation](query.html) aggregating data across many partitions of many PStates.
- Every piece of Rama is fully scalable.
- Because there’s only one system, Rama, deployment and monitoring is all built-in and massively simplified compared to having to manually coordinate many systems.

So that’s an overview of Rama’s design philosophy, and how its constructs – depots, ETLs, and PStates – work together to meet your needs. The examples you’ve seen so far, however, don’t fully demonstrate how it’s possible that Rama can meet all of your data needs. How would you work with user profiles? How would you create a simple forum? What about a recommendation engine?

## Theory in practice: PStates are views

Let’s direct our attention to some concrete examples to deepen our understanding
of Rama’s constructs and how to use them to develop an application.

Central to the Rama development workflow is identifying useful views of your data, and then creating PStates for those views. By "useful views of your data", we refer to the data structures that make the most sense for how the data is actually consumed by clients. If clients are best served by a simple key/value pair mapping strings to numbers like you saw in the word count example, you would specify your PState be structured as a map of `String` to `Long`. If you needed a richer, more complex data structure, Rama can handle that too.

For example, if you are building a web app analytics application, you might have
a depot that receives navigation events that look like this:

```java
// event 1
{"path":      "/product/1",
 "duration":  3500,
 "sessionId": "abc"}

// event 2
{"path":      "/about-us",
 "duration":  1800,
 "sessionId": "def"}

// event 3
{"path":      "/cart",
 "duration":  2100,
 "sessionId": "abc"}
```

Two useful views of this raw event data are a Session History and a Page Hit
Count. Your application’s clients would want views that look like this:

```java
// Page Hit Count
{"/product/1": 1,
 "/cart":      1,
 "/about-us":  1}

// Session History
// "abc" is the session id, and the value is a sequence of paths
{"abc": [{"path":     "/product/1",
          "duration": 3500},
         {"path":     "/cart",
          "duration": 2100}]
 "def": [{"path":     "/about-us",
          "duration": 1800}]}
```

The first view gives you an idea of how popular a given page is, and the second
allows you to precisely review individuals' histories of interactions with your
site. You can create a PState for each of these views, storing these data
structures directly.

### Analytics app example

Let’s look at how these ideas play out in a slightly more sophisticated example.
The module below implements the hypothetical analytics application we’ve been
discussing:

```java
package rama.examples.tutorial;

import com.rpl.rama.*;
import com.rpl.rama.module.*;
import com.rpl.rama.test.*;
import java.util.*;

public class PageAnalyticsModule implements RamaModule {
  @Override
  public void define(Setup setup, Topologies topologies) {
    setup.declareDepot("*depot", Depot.random());

    StreamTopology s = topologies.stream("s");
    s.pstate("$$pageViewCount", PState.mapSchema(String.class, Long.class));
    s.pstate("$$sessionHistory",
             PState.mapSchema(
               String.class,
               PState.listSchema(PState.mapSchema(String.class, Object.class))));

    s.source("*depot").out("*pageVisit")
     .each((Map<String, Object> visit) -> visit.get("sessionId"), "*pageVisit").out("*sessionId")
     .each((Map<String, Object> visit) -> visit.get("path"), "*pageVisit").out("*path")
     .each((Map<String, Object> visit) -> {
       Map ret = new HashMap(visit);
       ret.remove("sessionId");
       return ret;
     }, "*pageVisit").out("*thinPageVisit")
     .compoundAgg("$$pageViewCount", CompoundAgg.map("*path", Agg.count()))
     .compoundAgg("$$sessionHistory", CompoundAgg.map("*sessionId", Agg.list("*thinPageVisit")));
  }

  public static void main(String[] args) throws Exception {
    try (InProcessCluster cluster = InProcessCluster.create()) {
      cluster.launchModule(new PageAnalyticsModule(), new LaunchConfig(1, 1));
      String moduleName = PageAnalyticsModule.class.getName();
      Depot depot = cluster.clusterDepot(moduleName, "*depot");

      Map<String, Object> pageVisit = new HashMap<String, Object>();
      pageVisit.put("sessionId", "abc123");
      pageVisit.put("path", "/posts");
      pageVisit.put("duration", 4200);

      depot.append(pageVisit);
    }
  }
}
```

In this code regular Java hash maps are used to represent page visits – in a production module we would recommend using first-class types instead. But for the purposes of illustration, hash maps will serve nicely.

The first non-boilerplate block of code defines PStates:

```java
s.pstate("$$pageViewCount", PState.mapSchema(String.class, Long.class));
s.pstate("$$sessionHistory",
         PState.mapSchema(String.class,
                          PState.listSchema(PState.mapSchema(String.class, Object.class))));
```

This defines `$$pageViewCount` as a map where the keys are strings and the
values are numbers. It then defines `$$sessionHistory` as a nested structure.
The top level is a map where keys are strings and values are lists. The lists
can contain maps where the keys are strings and the values can be of any type.

After this, we create the ETL:

```java
s.source("*depot").out("*pageVisit")
 .each((Map visit) -> visit.get("sessionId"), "*pageVisit").out("*sessionId")
 .each((Map visit) -> visit.get("path"), "*pageVisit").out("*path")
 .compoundAgg("$$pageViewCount", CompoundAgg.map("*path", Agg.count()))
 .compoundAgg("$$sessionHistory", CompoundAgg.map("*sessionId", Agg.list("*pageVisit")));
```

This defines how to populate the PStates. As page visits come in, we update the
`$$pageViewCount` that correspond’s to the visit’s `path`, and we append the
page visit to the `$$sessionHistory` for the visit’s `*sessionId`.

Let’s look a little closer at both the schemas and the ETL.

### Schemas define PState structure

You specify a PState’s structure with its *schema*. PStates can be *maps*,
*lists* or *sets*. (A map is a key-value data structure like a dictionary.)
Their contents can be of any type — strings, numbers, or even nested data
structures.

We’ve seen a schema getting passed in when creating a PState:

```java
s.pstate("$$wordCounts", PState.mapSchema(Object.class, Object.class));
```

This specifies that `"$$wordCounts"` is a map PState where the keys and values
can be of any type. More examples of schemas include:

`PState.mapSchema(String.class, Long.class)`
:   specifies a map where keys
    must be Strings and values must be Longs. This is a better schema for
    `"$$wordCounts"`. It is also a good schema for our Page Hit Count
    PState.

`PState.listSchema(Object.class)`
:   specifies a list where each value can be
    of any type

`PState.listSchema(Long.class)`
:   specifies a list where each value must be
    a Long

`PState.setSchema(Object.class)`
:   specifies a set where each value can be
    of any type

`PState.setSchema(String.class)`
:   specifies a set where each value must be
    a String

When specifying what types are allowed in a PState, you can use classes like
`String.class` as seen above, or you can use another schema. For example:

`PState.listSchema(PState.mapSchema(String.class, Object.class))`
:   specifies
    a list where each value must be a map. The maps' keys must be strings, and
    values must be any type. This would be a good schema for the Session History
    PState in our analytics application.

`PState.mapSchema(Object.class, PState.setSchema(String.class))`
:   specifies a
    map where each key can be any type and each value must be a set of strings.

The simplest kind of schema is `Long.class`, and an appropriate use case for
that would be a global count.

The reason Rama supports storing rich data structures like this is that it
furthers the design goal of allowing you to build your entire application using
a single system. "Single system" doesn’t just mean that your data store lives in
the same process as the rest of your application; it also means that you don’t
have to contort your data into whatever limited data structures your data store
requires. You can take regular day-in day-out programming concepts and bring
them into backend programming, which frees you up to focus more on the actual
business logic of your application rather than the arcana of integrating with
external systems.

### ETLs update PStates

While PState schemas define the shapes of the views you want to store, ETLs
populate those views. Let’s revisit the ETL:

```java
s.source("*depot").out("*pageVisit")
 .each((Map visit) -> visit.get("sessionId"), "*pageVisit").out("*sessionId")
 .each((Map visit) -> visit.get("path"), "*pageVisit").out("*path")
 .compoundAgg("$$pageViewCount", CompoundAgg.map("*path", Agg.count()))
 .compoundAgg("$$sessionHistory", CompoundAgg.map("*sessionId", Agg.list("*pageVisit")));
```

For you to understand this code to the point that you feel fluent with (it’s
obvious what’s happening just from reading it, and you could write it yourself
if you needed to) requires a thorough explanation of the programming model
behind ETLs, along with guidance on using the full suite of tools available when
constructing ETLs. And we’ll get to that [soon](tutorial3.html#Section1)!

Right now, though, let’s continue constructing the outlines of how to do work in Rama. Let’s first go through each line above and then consider the bigger picture on how ETLs fit into Rama programming.

#### The ETL, translated

The ETL begins with this:

```java
s.source("*depot").out("*pageVisit")
```

The `source` method means "read records from the depot named `"*depot"` on an
ongoing basis. When a record is read, *bind* it to the name `"*pageVisit"`."
Binding a value is like assigning a variable: it’s associating a value with a
name within some scope. In this case, the name is the string `"*pageVisit"` and
the scope is everything that follows in the ETL definition. Let’s call these bindings *vars*.
They’re *variables* in the sense that their value will change each time this ETL
runs.

The next line two lines are:

```java
.each((Map visit) -> visit.get("sessionId"), "*pageVisit").out("*sessionId")
.each((Map visit) -> visit.get("path"), "*pageVisit").out("*path")
```

Both of these lines have the same form: they call `each` with two arguments, an
operator and a var. `each` works by performing the supplied computation on
the supplied var. After each call to `each`, we call `out` and provide a
var name. Just like in the first step, this binds the result of `each`, making it
available to the rest of the ETL.

Lastly, we have:

```java
.compoundAgg("$$pageViewCount", CompoundAgg.map("*path", Agg.count()))
.compoundAgg("$$sessionHistory", CompoundAgg.map("*sessionId", Agg.list("*pageVisit")));
```

These calls to `compoundAgg` update our PStates. The strings `"*path"` and
`"*sessionId"` that we pass in to `CompoundAgg.map` are the vars we bound in
the previous steps.

If this notion of vars binding values within some scope still seems a little
hazy, you can think of the ETL as being semantically equivalent to the following
Java pseudocode with regard to variables and scopes:

```java
public static void runETL() {
    var pageVisit = Depot.read("*depot");
    var sessionId = pageVisit.get("sessionId");
    var path = pageVisit.get("path");
    PState.update("$$pageViewCount", CompoundAgg.map(path, Agg.count()));
    PState.update("$$sessionHistory", CompoundAgg.map(sessionId, Agg.list(pageVisit)));
}
```

In the same way that `sessionId` is assigned a value using the current
`pageVisit` in this pseudocode, the var `"*sessionId"` is assigned a value
for the current `"*pageVisit"` in the web analytics ETL.

#### ETLs specify distributed computations

A reasonable question to ask is, why not just use vanilla Java here? Why add
this extra layer?

In the same way that the JVM provides resource abstractions on top of the
underlying operating system, Rama provides resource abstractions on top of a
cluster of JVM processes. ETLs are a computation abstraction that Rama provides
for executing a distributed data pipeline across multiple JVMs. ETLs allow you
to specify the source for a data pipeline (with the `source` method), place
values in a data pipeline (`out`), and access the values in the
pipeline (`each`). They also allow you to store values in PStates
(`compoundAgg`). In upcoming sections, you’ll also see how ETLs allow you to
transparently relocate parts of the pipeline to other JVMs in your cluster.

The need for a special API isn’t evident from the examples so far because they’ve been
artificially constrained so that the work is performed on only a single
thread of a single machine. This was so to focus on the idea of how to
use depots, ETLs, and PStates to produce useful views of data. But the broader
conception of performing work in Rama is that you’re *coordinating distributed
tasks across an arbitrary number of processes and machines to produce useful,
distributed views of data.*

One **very important** detail here is that the ETL API is implemented *in Java*,
and that means that there are ways in which we get to leverage the full power of
Java. Specifically, the values that flow through the pipeline are Java objects,
and you transform them using Java methods. That’s what’s happening with this
line:

```java
.each((Map visit) -> visit.get("sessionId"), "*pageVisit").out("*sessionId")
```

The `each` method is accessing the `"*pageVisit"` value in the pipeline, which
is a Java HashMap. It’s accessing it with the lambda expression
`(Map visit) → visit.get("sessionId")` and placing the value in
the pipeline, bound to `"*sessionId"`. While this is a very simple example,
it’s possible to write an arbitrarily complex Java method to transform values.

Just as PStates allow you to store data in arbitrarily nested structures, ETLs
allow you to write pipelines that transform data using arbitrary Java.

### Queries can be distributed computations, too

Being able to use arbitrary Java in your ETLs affords you great power as you
write the parts of your program that read from depots, transform those records,
and store the results in PStates. But what about when you’re reading that data?
Do queries get the same kind of love?

If you look at how we queried word count data, it might seem like the answer is
no:

```java
PState wc = cluster.clusterPState(moduleName, "$$wordCounts");
System.out.println("one: " + wc.selectOne(Path.key("one")));
System.out.println("two: " + wc.selectOne(Path.key("two")));
System.out.println("three: " + wc.selectOne(Path.key("three")));
```

Do not let these simple examples fool you! In fact, you can use the same API you
use for ETLs to construct queries that perform arbitrary computations. Such
queries are called [query topologies](query.html). Here’s an example query topology:

```java
topologies.query("followersYouKnow", "*you", "*otherUser").out("*userSet")
          .hashPartition("$$following", "*you")
          .localSelect("$$followingById", Path.key("*you").sortedMapRangeFrom(0, 300).mapVals()).out("*following")
          .hashPartition("$$followers", "*otherUser")
          .localSelect("$$followers", Path.must("*otherUser", "*following"))
          .originPartition()
          .agg(Agg.set("*following")).out("*userSet");
```

This query topology will return a list of "followers you know" – people you follow who are following a given user.

Don’t worry about figuring out precisely how this query works yet. The point of
the example is to show you that this query topology takes the same form as an
ETL and uses the same elements, like `.out` and `.hashPartition`.

As it turns out, ETLs are also topologies, and you can see that in a part of the
word count source that we’ve glossed over until now:

```java
public void define(Setup setup, Topologies topologies) {
  setup.declareDepot("*wordDepot", Depot.random());
  StreamTopology s = topologies.stream("wordCountStream");
  s.pstate("$$wordCounts", PState.mapSchema(String.class, Long.class));

  s.source("*wordDepot").out("*token")
   .hashPartition("*token")
   .compoundAgg("$$wordCounts", CompoundAgg.map("*token", Agg.count()));
}
```

This is the `define` method for a module, and the second argument is
`topologies`. You use `topologies.stream` to define a *streaming ETL topology*,
and you use `topologies.query` to define a query topology.

You can think of "topology" as short for "distributed computation topology", a
computation graph where each node defines some operation. These distributed
computations have the general form of *reading data from somewhere and
performing operations on it*. With ETLs, you’re reading data from depots, and
the operations include transformations and writes to PStates. With queries,
you’re reading data from PStates, and you can perform the same kinds of
transformations as you can in ETLs, including transforming the data with
arbitrary Java.

Writing topologies is the core part of Rama development, and the next couple
pages will cover this topic in detail.

## Arbitrary Java

You might be wondering, why is this document so obsessed with
"arbitrary Java"? Well here’s why: Rama’s design closes the gap between "normal
programming" and "database programming". It removes the boundary between
"application system" and "data system", colocating application-level
computations with the same resources responsible for retrieving and storing
data.

An earlier section discussed how your system is all just Rama, and this is an
extension of that. Rama’s model is simple in that it’s comprised of fewer
pieces, and it’s powerful in that you can use all of Java pretty much
everywhere. It’s not a half-measure like a stored procedure, which has to be
written in the database’s language, lives outside of version control,
and is likely managed by another team in your organization. You and your team
have full control of how to interact with your data layer.

## Summary

In this section, you learned the flow of how data moves through a Rama module. All data arrives into depots, topologies process that data to create PStates, and clients query those PStates to power applications. What makes Rama so powerful is how general-purpose all these facilities are: the expressive topology API gives you full control over where and how code executes, PStates can be shaped optimally for each use case, and the path-based query API can take full advantage of your PStates no matter what shape they’re in. In subsequent pages you’ll dive deeper into all these topics.