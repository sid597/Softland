# REST API
> Source: https://redplanetlabs.com/docs/~/rest.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# REST API

Rama has a built-in REST API for doing depot appends, PState queries, and query topology invokes with HTTP requests. This allow Rama modules to be read and written to from any programming language, as well as allowing ad-hoc queries to be done with tools like `curl`.

The REST API uses JSON to represent arguments and responses. All requests are done using POST requests so that JSON can be provided in the body.

Here are a few examples of using `curl` to invoke the REST API:

```java
>> curl -X POST -H "Content-Type: text/plain" 'http://1.2.3.4:2000/rest/com.mycompany.MyModule/depot/*registerDepot/append' -d '{"data": {"userid":"alice"}}'
{"profileTopology":"success"}

>> curl -X POST -H "Content-Type: text/plain" 'http://1.2.3.4:2000/rest/com.mycompany.MyModule/pstate/$$profiles/selectOne' -d '["alice", "location"]'
"New York City, NY"

>> curl -X POST -H "Content-Type: text/plain" 'http://1.2.3.4:2000/rest/com.mycompany.MyModule/query/usersWithAge/invoke' -d '[31]'
["cagney", "davis", "tracy", "lemmon"]
```

REST API requests are handled by [Supervisor](terminology.html#_supervisor) daemons. The [Conductor](terminology.html#_conductor) also implements the same REST API on the Cluster UI port (default 8888), and it will return a 308 redirect to the appropriate Supervisor to handle the request. So a REST request could be done directly on the Conductor using `curl` like this:

```java
>> curl -L -X POST -H "Content-Type: text/plain" 'http://1.1.1.1:8888/rest/com.mycompany.MyModule/pstate/$$profiles/selectOne' -d '["alice", "location"]'
"New York City, NY"
```

The additional `-L` flag tells `curl` to follow redirects.

The Conductor should not be used for lots of requests since it’s a central point of contention and computing the redirect puts load on the [metastore](terminology.html#_metastore). It’s fine to use for low-throughput, ad-hoc usage like this, but in general it should only be used for the initial request to find the Supervisors on which to send all future requests. This is detailed in the next section.

## Structure of REST API

In order to reduce the number of cross-cluster connections, a REST request for a module is handled only by a Supervisor that is currently running a worker of that module. REST requests made to the Conductor or to a Supervisor not running that module will return a 308 redirect.

A 308 redirect includes the following headers:

```java
"Location": "http://1.2.3.4:2000/rest/com.mycompany.MyModule/depot/*registerDepot/append"
"Supervisor-Locations": '["1.2.3.4:2000", "1.2.3.5:2000", "1.2.3.6:3000"]'
```

`"Location"` is a standard header in redirects that can be automatically followed by programs like `curl`. `"Supervisor-Locations"` is a header custom to Rama that a programmatic HTTP client should use for all future HTTP requests for this module. It’s a JSON-encoded list of host/port strings for all Supervisors running this module. Each hostname returned is the internal host configured for that Supervisor. To evenly distribute the load of handling REST requests, REST requests should be evenly distributed across all those Supervisors (e.g. by selecting a random one for each request).

If a Supervisor is no longer running that module, like if the Conductor reassigned it during a scaling or reassign operation, the Supervisor will return a redirect of this form. If a Supervisor is down and a request returns a 500 status code, an HTTP client should try another Supervisor or make the request to the Conductor to get an updated list of Supervisors to try.

Any `/` characters in module names in URLs, like for modules defined with the Clojure API, should be encoded with `%2F`.

## JSON-encoding arguments

JSON can only represent a limited number of types. Since it’s common to use precise numeric types in Rama modules, like ints, longs, and shorts, and since other types like functions need to be referenced, Rama has a special way to represent those non-standard types. By default, numbers are interpreted as 32-bit integers, and floating-point values are interpreted as 64-bit doubles.

A non-standard type is represented with a string of the form `"#__<type character><value>"`. Here’s a way to JSON encode a list containing a long (64-bit integer), a character, and a byte (8-bit integer):

```java
'["#__L123456789", "#__Ca", "#__B19"]'
```

If the value-string cannot be parsed into that type, such as if the type doesn’t have enough bits to represent that value, Rama will throw an exception while parsing the JSON and the request will fail.

Rama parses these strings regardless of how they’re nested. So even if a special string is specified in a data structure six levels deep, it will be converted properly.

Here are all the special types that can be represented:

|  |  |  |
| --- | --- | --- |
| Type character | Parsed type | Example |
| B | Byte (8-bit integer) | "#\_\_B19" |
| S | Short (16-bit integer) | "#\_\_S5190" |
| L | Long (64-bit integer) | "#\_\_L123456789" |
| F | Float (32-bit floating-point) | "#\_\_F123.456" |
| C | Character | "#\_\_CQ" |
| K | Clojure keyword (e.g. `:a`) | "#\_\_Ka" |
| f | Function | "#\_\_fcom.mycompany.ops.MyRamaFunction" |

Function references can be to three kinds of functions:

- [RamaFunction](https://redplanetlabs.com/javadoc/com/rpl/rama/ops/RamaFunction.html) implementation: the referenced class must have a zero-arg constructor.
- Clojure function: the value string must be a fully-qualified reference to a Clojure function, e.g. `clojure.core/even?`
- Built-in function from [Ops](https://redplanetlabs.com/javadoc/com/rpl/rama/ops/Ops.html) class: in this case, the value string is of the form `Ops.IS_EVEN`

Note that the special encodings are only used for requests, and they’re not used for responses. Responses for all REST API methods are JSON encoded without any special handling for non-standard types.

## Depot appends

A [depot](depots.html) append is posted to a URL of the form `http://<host>:<port>/rest/<module name>/depot/<depot name>/append`. The body of the request is a JSON-encoded map containing `"data"` and optionally `"ackLevel"`.

The accepted ack levels are `"ack"`, `"appendAck"`, and `"none"`. When unspecified `"ackLevel"` defaults to `"ack"`, the same default as used for native depot clients. The meaning of the ack levels are described in [this section](depots.html#_appends).

The response to a depot append are JSON-encoded [streaming ack returns](depots.html#_streaming_ack_returns). Just like native clients, this will be an empty map for ack levels other than `"ack"`. The returned object maps topology names to their ack return value.

Here are some examples of doing depot appends with `curl`:

```java
>> curl -X POST -H "Content-Type: text/plain" 'http://1.2.3.4:2000/rest/com.mycompany.MyModule/depot/*registerDepot/append' -d '{"data": {"userid":"alice"}}'
{"profileTopology":"success"}

>> curl -X POST -H "Content-Type: text/plain" 'http://1.2.3.4:2000/rest/com.mycompany.MyModule/depot/*registerDepot/append' -d '{"data": {"userid":"alice"}, "ackLevel": "appendAck"}'
{}

>> curl -X POST -H "Content-Type: text/plain" 'http://1.2.3.4:2000/rest/com.mycompany.Module2/depot/*anotherDepot/append' -d '{"data": [1,2,"#__L1234567"], "ackLevel": "none"}'
{}
```

## PState queries

[PStates](pstates.html) can be queried with the REST API with the full richness of paths. Paths are represented using a JSON format. To learn paths, we recommend reading the [Paths](paths.html) page and playing with them using Rama’s `TestPState` with either the [Java API version](testing.html#_testpstate) or [Clojure API version](clj-testing.html).

There are two REST API methods available for querying PStates, `select` and `selectOne`. Like their counterparts in the native API, `select` returns a list of navigated values and `selectOne` returns a single navigated value. `selectOne` errors if zero or more than one value is selected.

Here are examples of invoking these methods using `curl`:

```java
>> curl -X POST -H "Content-Type: text/plain" 'http://1.2.3.4:2000/rest/com.mycompany.MyModule/pstate/$$p/select' -d '["alice", ["all"], "#__fOps.IS_EVEN"]'
[4, 2, 18, 2]

>> curl -X POST -H "Content-Type: text/plain" 'http://1.2.3.4:2000/rest/com.mycompany.MyModule/pstate/$$profiles/selectOne' -d '["alice", "location"]'
"New York City, NY"
```

Like Rama’s native PState APIs, paths are represented in JSON as a list of navigators. A navigator can be either *implicit* or *explicit*.

An implicit navigator can either be a string, a Clojure keyword, or a function. Strings and keywords get wrapped in the [key](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#key-java.lang.Object...-) navigator, and functions get wrapped in the [filterPred](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#filterPred-com.rpl.rama.ops.RamaFunction1-) navigator. For example, here is a JSON path comprised solely of implicit navigators:

```java
["a", "b", "#__fOps.IS_EVEN"]
```

This is equivalent to the Java path:

```java
Path.key("a").key("b").filterPred(Ops.IS_EVEN)
```

This is also equivalent to the Clojure path:

```java
["a" "b" even?]
```

Explicit navigators are represented as a list of the form `[<operation string> <args>…​]`. The operation string determines how each argument is interpreted, whether as a value or another path. For example, the ["must"](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#must-java.lang.Object…​-) navigator accepts one or more keys as arguments. Here are valid ways to use `"must"`:

```java
["must", "a", "b"]
["must", "k"]
["must", "key1", "key2", "key3"]
```

The ["filterSelected"](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#filterSelected-com.rpl.rama.Path-) navigator interprets all of its arguments as a single path. For example:

```java
["filterSelected", "a", ["all"], #__fOps.IS_EVEN"]
```

This is equivalent to the Java path:

```java
Path.filterSelected(Path.key("a").all().filterPred(Ops.IS_EVEN))
```

This is also equivalent to the Clojure path:

```java
(selected? "a" ALL even?)
```

["multiPath"](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#multiPath-com.rpl.rama.Path...-), on the other hand, interprets each argument as a separate path. For example:

```java
["multiPath", "a", "b", "c"]
```

This is equivalent to the Java path:

```java
Path.multiPath(Path.key("a"), Path.key("b"), Path.key("c"))
```

This is also equivalent to the Clojure path:

```java
(multi-path "a" "b" "c")
```

Like with PState queries done with a Java or Clojure client, a query path requires a leading `key` navigator and will route to the PState partition according to that leading key. Like how Java and Clojure clients allow you to specify an explicit partitioning key instead, you can specify an explicit partitioning key with a JSON path like this:

```java
[["pkey", "some-pkey"], "mapKeys"]
```

If the path begins with a list starting with `"pkey"`, the second element of that list will be the partitioning key used to route the query to a PState partition.

The following is a complete listing of all navigators available, categorized by type. All navigators can be referred to by their name in either the Java API or Clojure API. The "Arguments" column specifies how each argument is interpreted:

- `None`: the navigator takes no arguments
- `Value…​`: any number of values can be provided as arguments
- `Path…​`: all arguments are interpreted as a single path
- `Paths…​`: each argument is interpreted as its own path and there can be a variadic number of them
- Otherwise, the arguments are specified individually as either a `Value` or a `Path`. Some navigators have an optional final argument specified as `?Value` or `?Path`. For example, `sortedMapRange` below accepts either two or three arguments.

The meaning of the arguments is identical to how they’re described in the corresponding Javadoc / Clojuredoc.

Some navigators appear in multiple sections because they can be used on multiple data types.

### Map navigators

|  |  |  |  |
| --- | --- | --- | --- |
| Names | Arguments | Example | Note |
| [all / ALL](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#all--) | None | ["all"] |  |
| [key / keypath](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#key-java.lang.Object...-) | Value…​ | ["key", "a", "b"] |  |
| [mapKey / map-key](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#mapKey-java.lang.Object-) | Value | ["mapKey", "k1"] |  |
| [mapKeys / MAP-KEYS](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#mapKeys--) | None | ["mapKeys"] |  |
| [mapVals / MAP-VALS](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#mapVals--) | None | ["mapVals"] |  |
| [must](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#must-java.lang.Object...-) | Value…​ | ["must", "k1", "k2", "k3"] |  |
| [sortedMapRange / sorted-map-range](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#sortedMapRange-java.lang.Object-java.lang.Object-) | Value, Value, ?Value | ["sortedMapRange", 123, 987, {"inclusive-start?": false}] | Final argument is options map accepting `"inclusive-start?"` and `"inclusive-end?"` |
| [sortedMapRangeFrom / sorted-map-range-from](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#sortedMapRangeFrom-java.lang.Object-) | Value, ?Value | ["sortedMapRangeFrom", "k", 100] | Final argument is either value for max amount or options map accepting `"max-amt"` and `"inclusive?"` |
| [sortedMapRangeFromStart / sorted-map-range-from-start](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#sortedMapRangeFromStart-java.lang.Object-) | Value | ["sortedMapRangeFromStart", 100] |  |
| [sortedMapRangeTo / sorted-map-range-to](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#sortedMapRangeTo-java.lang.Object-) | Value, ?Value | ["sortedMapRangeTo", "s", {"max-amt": 10, "inclusive?": true}] | Final argument is either value for max amount or options map accepting `"max-amt"` and `"inclusive?"` |
| [subMap / submap](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#subMap-java.lang.Object...-) | Value…​ | ["submap", "k1", "k2"] |  |

### List navigators

|  |  |  |  |
| --- | --- | --- | --- |
| Names | Arguments | Example | Note |
| [all / ALL](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#all--) | None | ["all"] |  |
| [afterElem / AFTER-ELEM](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#afterElem--) | None | ["afterElem"] |  |
| [beforeElem / BEFORE-ELEM](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#beforeElem--) | None | ["beforeElem"] |  |
| [beforeIndex / before-index](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#beforeIndex-java.lang.Object-) | Value | ["beforeIndex", 3] |  |
| [beginning / BEGINNING](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#beginning--) | None | ["beginning"] |  |
| [end / END](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#end--) | None | ["end"] |  |
| [filteredList / filterer](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#filteredList-com.rpl.rama.Path-) | Path…​ | ["filteredList", ["must", "a"], "#\_\_fOps.IS\_ODD"] |  |
| [first / FIRST](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#first--) | None | ["first"] |  |
| [index / index-nav](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#index-java.lang.Object-) | Value | ["index", 7] |  |
| [indexedVals / INDEXED-VALS](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#indexedVals--) | None | ["indexedVals"] |  |
| [last / LAST](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#last--) | None | ["last"] |  |
| [nth / nthpath](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#nth-java.lang.Object-) | Value…​ | ["nth", 4, 0, 2] |  |
| [sublist / srange](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#sublist-java.lang.Object-java.lang.Object-) | Value, Value | ["sublist", 1, 8] |  |
| [sublistDynamic / srange-dynamic](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#sublistDynamic-com.rpl.rama.ops.RamaFunction1-com.rpl.rama.ops.RamaFunction2-) | Value, Value | ["sublistDynamic", "#\_\_fcom.mycompany.MyFunction1", "#\_\_fcom.mycompany.MyFunction2"] |  |

### Set navigators

|  |  |  |  |
| --- | --- | --- | --- |
| Names | Arguments | Example | Note |
| [all / ALL](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#all--) | None | ["all"] |  |
| [setElem / set-elem](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#setElem-java.lang.Object-) | Value | ["setElem", "e1"] |  |
| [sortedSetRange / sorted-set-range](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#sortedSetRange-java.lang.Object-java.lang.Object-) | Value, Value, ?Value | ["sortedSetRange", "e1", "hh", {"inclusive-end?": true}] | Final argument is options map accepting `"inclusive-start?"` and `"inclusive-end?"` |
| [sortedSetRangeFrom / sorted-set-range-from](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#sortedSetRangeFrom-java.lang.Object-) | Value, ?Value | ["sortedSetRangeFrom", "e", 10] | Final argument is either value for max amount or options map accepting `"max-amt"` and `"inclusive?"` |
| [sortedSetRangeFromStart / sorted-set-range-from-start](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#sortedSetRangeFromStart-java.lang.Object-) | Value | ["sortedSetRangeFromStart", "e", 10] |  |
| [sortedSetRangeTo / sorted-set-range-to](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#sortedSetRangeTo-java.lang.Object-) | Value, ?Value | ["sortedSetRangeTo", "q", {"max-amt": 10, "inclusive?": true}] | Final argument is either value for max amount or options map accepting `"max-amt"` and `"inclusive?"` |
| [subset](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#subset-java.lang.Object...-) | Value…​ | ["subset", "e1", "e3"] |  |
| [voidSetElem / NONE-ELEM](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#voidSetElem--) | None | ["voidSetElem"] |  |

### Filter navigators

|  |  |  |  |
| --- | --- | --- | --- |
| Names | Arguments | Example | Note |
| [filterEqual / pred=](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#filterEqual-java.lang.Object-) | Value | ["filterEqual", 9] |  |
| [filterGreaterThan / pred>](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#filterGreaterThan-java.lang.Object-) | Value | ["filterGreaterThan", 9] |  |
| [filterGreaterThanOrEqual / pred>=](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#filterGreaterThanOrEqual-java.lang.Object-) | Value | ["filterGreaterThanOrEqual", 9] |  |
| [filterLessThan / pred<](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#filterLessThan-java.lang.Object-) | Value | ["filterLessThan", 9] |  |
| [filterLessThanOrEqual / pred<=](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#filterLessThanOrEqual-java.lang.Object-) | Value | ["filterLessThanOrEqual", 9] |  |
| [filterNotEqual / prednot=](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#filterNotEqual-java.lang.Object-) | Value | ["filterNotEqual", 9] |  |
| [filterPred / pred](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#filterNotEqual-java.lang.Object-) | Value | ["filterPred", "#\_\_fOps.IS\_ODD"] |  |
| [filterSelected / selected?](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#filterSelected-com.rpl.rama.Path-) | Path…​ | ["filterSelected", ["all"], ["filterEqual" 2]] |  |
| [filterNotSelected / not-selected?](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#filterNotSelected-com.rpl.rama.Path-) | Path…​ | ["filterSelected", ["all"], ["filterEqual" 2]] |  |

### View navigators

|  |  |  |  |
| --- | --- | --- | --- |
| Names | Arguments | Example | Note |
| [nullToList / NIL->VECTOR](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#nullToList--) | None | ["nullToList"] |  |
| [nullToSet / NIL->SET](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#nullToSet--) | None | ["nullToSet"] |  |
| [nullToVal / nil->val](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#nullToVal-java.lang.Object-) | Value | ["nullToVal", 0] |  |
| [transformed / multi-transformed](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#transformed-com.rpl.rama.Path-) | Path…​ | ["transformed", ["all"], "a", ["termVal", "v"]] |  |
| [view](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#view-com.rpl.rama.ops.RamaFunction1-) | Value…​ | ["view", "#\_\_fOps.PLUS", 2] |  |

These navigators are used in paths passed to `"transformed"`:

|  |  |  |  |
| --- | --- | --- | --- |
| Names | Arguments | Example | Note |
| [term](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#term-com.rpl.rama.ops.RamaFunction1-) | Value | ["term", "#\_\_fOps.INC"] |  |
| [termVal / termval](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#termVal-java.lang.Object-) | Value | ["termVal", 100] |  |
| [termVoid, NONE>](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#termVoid--) | None | ["termVoid"] |  |

### Control navigators

|  |  |  |  |
| --- | --- | --- | --- |
| Names | Arguments | Example | Note |
| [ifPath / if-path](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#ifPath-com.rpl.rama.Path-com.rpl.rama.Path-) | Path, Path, ?Path | ["ifPath", ["a", "#\_\_fOps.IS\_EVEN"], "b", "c"] | Last argument is optional "else" path |
| [multiPath / multi-path](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#multiPath-com.rpl.rama.Path...-) | Paths…​ | ["multiPath", ["a", ["ALL"]], "b"] |  |
| [stay, STAY](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#stay--) | None | ["stay"] |  |
| [stop, STOP](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#stop--) | None | ["stop"] |  |
| [subselect](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#subselect-com.rpl.rama.Path-) | Path…​ | ["subselect", ["ALL"], "a"] |  |
| [withPageSize / with-page-size](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#withPageSize-java.lang.Object-com.rpl.rama.Path-) | Value, Path | ["withPageSize", 3, [["mapVals"]]] |  |

### Value collection navigators

|  |  |  |  |
| --- | --- | --- | --- |
| Names | Arguments | Example | Note |
| [collect](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#collect-com.rpl.rama.Path-) | Path…​ | ["collect", ["ALL"], "a"] |  |
| [collectOne / collect-one](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#collectOne-com.rpl.rama.Path-) | Path…​ | ["collectOne", "k"] |  |
| [dispenseCollected, DISPENSE](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#dispenseCollected--) | None | ["dispenseCollected"] |  |
| [isCollected](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#isCollected-com.rpl.rama.ops.RamaFunction1-) | Value | ["isCollected", "#\_\_fcom.mycompany.MyPredicate"] |  |
| [putCollected / putVal](https://redplanetlabs.com/javadoc/com/rpl/rama/Path.html#putCollected-java.lang.Object-) | Value | ["putCollected", "foo"] |  |

## Query topology invokes

A [query topology](query.html) invoke is posted to a URL of the form `http://<host>:<port>/rest/<module name>/query/<query topology name>/invoke`. The body of the request is a JSON-encoded list of arguments to that query topology. The response is the JSON-encoded result of the topology invoke.

Here’s an example of invoking query topology "usersWithAge" with one argument:

```java
>> curl -X POST -H "Content-Type: text/plain" 'http://1.2.3.4:2000/rest/com.mycompany.MyModule/query/usersWithAge/invoke' -d '[31]'
["cagney", "davis", "tracy", "lemmon"]
```

Here’s an example of invoking query topology "urlReach" with three arguments:

```java
>> curl -X POST -H "Content-Type: text/plain" 'http://1.2.3.4:2000/rest/com.mycompany.MyModule/query/urlReach/invoke' -d '["foo.com", "7/1/2024", "8/1/2024"]'
9849019
```

## Limitations of REST API

The REST API has a few limitations compared to the native Java and Clojure clients:

- Can’t use types outside of what can be represented in Rama’s JSON format. For PState queries, you can work around this by using the `"view"` navigator to convert custom types into a string representation that your client can interpret.
- Custom navigators cannot be referenced by JSON paths yet.
- PState reactivity is not exposed yet.

## Summary

On this page you saw how depot appends could be done with any ack level, how PState queries could be done with the full compositionality and expressiveness of paths, and how query topologies could be invoked by just passing a list of arguments.

Rama’s REST API exposes a huge amount of Rama’s functionality with very few limitations. It enables frontends to be written in any language, such as Python, Ruby, or JavaScript. This also makes it easier for an organization to adopt Rama, as the frontend team can continue using existing tooling.