# Clojure: Custom Serialization
> Source: https://redplanetlabs.com/docs/~/clj-serialization.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Custom serialization

One of the joys of using Rama is being able to use first-class types all the time, whether appending data to a depot, processing data in ETLs, querying PStates, or anything else. To use an object type within Rama, Rama must know how to serialize/deserialize that type to a stream of bytes. Rama does this when writing objects to disk or transferring objects to other processes.

Rama has built-in support for using basic types (int, long, float, double, string, etc.), commonly used types from [java.util](https://docs.oracle.com/javase/8/docs/api/java/util/package-summary.html), [Clojure data structures](https://clojure.org/reference/data_structures), and types defined with `defrecord`.

For other types you need to tell Rama how to serialize/deserialize them. One option is to use the serialization mechanisms described on the main [serialization page](serialization.html), such as implementing the [RamaCustomSerialization](https://redplanetlabs.com/javadoc/com/rpl/rama/RamaCustomSerialization.html) interface.

Under the hood, Rama uses [Nippy](https://github.com/taoensso/nippy) for serialization. So you can also register serializations with Rama by extending Nippy’s protocols directly.

Something to watch out for with Nippy is it uses a 2 byte hash to encode type IDs when using [extend-freeze](https://taoensso.github.io/nippy/taoensso.nippy.html#var-extend-freeze). This makes collisions of type IDs occur with greater than 1% probability after only 50 extensions ([birthday problem](https://en.wikipedia.org/wiki/Birthday_problem)). Rama’s `RamaCustomSerialization` interface implements Nippy extensions in a special way to internally use an 8 byte hash of those types to make collisions effectively impossible.