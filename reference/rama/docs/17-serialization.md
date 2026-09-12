# Custom Serialization
> Source: https://redplanetlabs.com/docs/~/serialization.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Custom serialization

One of the joys of using Rama is being able to use first-class types all the time, whether appending data to a depot, processing data in ETLs, querying PStates, or anything else. To use an object type within Rama, Rama must know how to serialize/deserialize that type to a stream of bytes. Rama does this when writing objects to disk or transferring objects to other processes.

Rama has built-in support for using basic types (int, long, float, double, string, etc.), commonly used types from [java.util](https://docs.oracle.com/javase/8/docs/api/java/util/package-summary.html), and the immutable data structures [from Clojure’s library](https://clojure.org/reference/data_structures).

On this page, you’ll learn:

- The conveniences and drawbacks of `RamaSerializable`
- Defining custom serializers
- Registering custom serializers in production and test environments
- Serialization requirements for custom types used as map keys or set values

## RamaSerializable interface

As was shown in the [RamaSpace example](tutorial6.html) from the tutorial, [RamaSerializable](https://redplanetlabs.com/javadoc/com/rpl/rama/RamaSerializable.html) is a convenient way to define Java types that integrate with Rama’s serialization needs. All you have to do is add the `RamaSerializable` interface to a type, and that type will be usable in all Rama contexts that require serialization. Here’s an example of defining a type with `RamaSerializable`:

```java
import com.rpl.rama.RamaSerializable;

public class LocationUpdate implements RamaSerializable {
  private long userId;
  private String location;

  public LocationUpdate(long userId, String location) {
    this.userId = userId;
    this.location = location;
  }

  public long getUserId() { return userId; }

  public String getLocation() { return location; }
}
```

Objects implementing `RamaSerializable` use [Java serialization](https://docs.oracle.com/javase/8/docs/technotes/guides/serialization/index.html). Though easy to use, there are some drawbacks to Java serialization:

- Performance is suboptimal since other serialization schemes can produce much lower numbers of bytes.
- Evolving types for future iterations of a module isn’t well supported by Java serialization. You may want to add a field to an existing type, for instance, and then you won’t be able to deserialize old versions of that type currently stored in your PStates.

For these reasons, we generally recommend limiting your use of `RamaSerializable` to experiments and tests. For production use it’s better to use a solution for custom types with first-class support for evolving types over time. Example projects that work well for this purpose are [Apache Thrift](https://thrift.apache.org/) and [Protocol Buffers](https://developers.google.com/protocol-buffers). In the next section, you’ll see how easy it is to integrate frameworks like these with Rama.

## RamaCustomSerialization interface

Serialization for custom types is implemented using the [RamaCustomSerialization](https://redplanetlabs.com/javadoc/com/rpl/rama/RamaCustomSerialization.html) interface. An implementation of this interface targets a particular type and provides methods to serialize and deserialize instances of that type. It is independent from the types themselves.

Let’s take a look at an example of using this interface to implement serialization for [Apache Thrift](https://thrift.apache.org/) types. This code comes from [our Twitter-scale Mastodon implementation](downloads-maven-local-dev.html):

```java
public abstract class ThriftSerialization implements RamaCustomSerialization<TBase> {
  private final Map<Short, Class> _idToType = new HashMap<>();
  private final Map<Class, Short> _typeToId = new HashMap<>();

  protected ThriftSerialization() {
    Map<Integer, Class> m = typeIds();
    for(Integer id: m.keySet()) _idToType.put(id.shortValue(), m.get(id));
    for(Short id: _idToType.keySet()) _typeToId.put(_idToType.get(id), id);
  }

  @Override
  public void serialize(TBase obj, DataOutput out) throws Exception {
    Short id = _typeToId.get(obj.getClass());
    if(id==null) throw new RuntimeException("Could not find type id for " + obj.getClass());
    out.writeShort(id);
    byte[] serialized = new TSerializer(new TCompactProtocol.Factory()).serialize(obj);
    out.writeInt(serialized.length);
    out.write(serialized);
  }

  @Override
  public TBase deserialize(DataInput in) throws Exception {
    TBase obj = (TBase) _idToType.get(in.readShort()).newInstance();
    byte[] arr = new byte[in.readInt()];
    in.readFully(arr);
    new TDeserializer(new TCompactProtocol.Factory()).deserialize(obj, arr);
    return obj;
  }

  @Override
  public Class targetType() {
    return TBase.class;
  }

  protected abstract Map<Integer, Class> typeIds();
}
```

There are three methods on the `RamaCustomSerialization` interface. `targetType` returns the type this object serializes. In this case it returns `TBase`, the base class for all Thrift objects. `serialize` writes an object of the target type to a `DataOutput`, and `deserialize` reads an object of the target type from `DataInput`.

`ThriftSerialization` implements serialization for all Thrift objects. In order to deserialize an object using Thrift, you need to first create an empty instance of the specific type being deserialized. So this code needs to distinguish different types in the serialized output. It does this using *type IDs*. An application using `ThriftSerialization` would extend the class with an implementation of `typeIds` to provide that information. Here’s an example of what that looks like:

```java
public class MyApplicationThriftSerialization extends ThriftSerialization {
  @Override
  protected Map<Integer, Class> typeIds() {
    Map<Integer, Class> ret = new HashMap<>();
    ret.put(0, ProfileEdit.class);
    ret.put(1, PageView.class);
    ret.put(2, Post.class);
    return ret;
  }
}
```

In this case `ProfileEdit`, `PageView`, and `Post` refer to the specific Thrift types used by the application.

Needing to distinguish types on the wire is a common problem when implementing serialization. Explicit type IDs are one solution you could use, but there are other ways you could approach it. You could generate type IDs using hashes of the classnames, with the size of the hash being a tradeoff between serialization compactness and collision resistance. If you used a two-byte hash, you’re only adding two bytes per serialized object, but the chance of multiple types hashing to the same value starts becoming high after only 50 types (see [Birthday problem](https://en.wikipedia.org/wiki/Birthday_problem)). If you used an eight-byte hash, the chance of collisions is effectively zero but serialization is correspondingly more expensive.

Custom serialization implementations are registered with Rama through the config provided for module launch or when creating a client-side [RamaClusterManager](https://redplanetlabs.com/javadoc/com/rpl/rama/RamaClusterManager.html). The key `"custom.serializations"` is given a list of class names of `RamaCustomSerialization` implementations you wish to have active. The classes provided must have a zero-argument constructor so that Rama can create instances of them. For example, to deploy a module using `MyApplicationThriftSerialization`, you could first specify the config in a file `overrides.yaml` like so:

```java
custom.serializations:
  - "com.rpl.myapp.serialization.MyApplicationThriftSerialization"
```

Then you would deploy the module with a command like the following:

```java
rama deploy \
--action launch \
--jar my-application-jar.jar \
--module com.rpl.myapp.MyModule \
--configOverrides overrides.yaml \
--tasks 128 \
--threads 32 \
--workers 8 \
--replicationFactor 3
```

To make a client that makes use of `MyApplicationThriftSerialization`, you would create the `RamaClusterManager` like so:

```java
Map config = new HashMap();
List serializations = new ArrayList();
serializations.add("com.rpl.myapp.serialization.MyApplicationThriftSerialization");
config.put("custom.serializations", serializations);
RamaClusterManager manager = RamaClusterManager.open(config);
```

It’s critical that all modules and clients that interact with those objects have this serialization registered. Since `"custom.serializations"` is a list, you can have as many custom serialization implementations registered as you need. See [Operating Rama clusters](operating-rama.html) for more details on deploying modules and using `RamaClusterManager`.

Configuring custom serializations in a [test context](testing.html) with `InProcessCluster` is slightly different. In this context custom serializations are configured as part of the `InProcessCluster` instead of as part of individual modules or clients. Here’s an example of using `MyApplicationThriftSerialization` with `InProcessCluster`:

```java
List<Class> serializations = new ArrayList<>();
serializations.add(MyApplicationThriftSerialization.class);
try(InProcessCluster ipc = InProcessCluster.create(serializations)) {
  RamaModule module = new MyModule();
  ipc.launchModule(module, new LaunchConfig(4, 4));
}
```

Finally, here are a few more properties a custom serialization implementation must have:

- Since every worker and client will have a separate instance, the serialization implementation must be deterministic.
- Must be thread-safe since the same instance can be used across multiple threads.
- Multiple `RamaCustomSerialization` implementations should not match on the same types, including parent types. Which one is used in that case is undefined.

## Serialization requirements for custom types used as map keys or set values

Top-level maps and subindexed structures in PStates are sorted. They also store their individual entries on disk in serialized form. If you want to use a custom type as a map key or set value, there are a few additional requirements.

First, equal values must serialize to the exact same sequence of bytes. Lookup of a key in an on-disk map or a value in an on-disk set is done using the serialized form, so if serialization isn’t consistent you may fail to look up an existing key. If you’re serializing something that has undefined order (like an unordered map), you need to assign a consistent order to the serialized form (like by sorting values before serializing). Rama implements consistent serialization for all of its built-in serialization, including types with undefined ordering like `HashMap` and `HashSet`.

Second, if you care about ordering in these contexts (like to be able to do [range queries](paths.html#_range_queries)), you need to make sure the serialized form of your custom types has the same ordering as the non-serialized form. The way serialized forms are compared on disk is with [lexicographic order](https://en.wikipedia.org/wiki/Lexicographic_order). Rama implements this property as well for built-in serialization for relevant types like numbers and strings.

## Summary

On this page you learned how to define and configure serialization for custom types. Once a serialization is configured, you’re able to use those types in a first-class way within modules and on clients without having to worry about serialization again. That the definition of serialization is disconnected from the types themselves, since the types don’t have to implement any additional interfaces, makes serialization completely non-intrusive to how you would normally define your data types. It also makes it easy to integrate serialization frameworks like Apache Thrift and Protocol Buffers with Rama.