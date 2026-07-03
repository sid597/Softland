# Tutorial 6: Tying It All Together
> Source: https://redplanetlabs.com/docs/~/tutorial6.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Tying it all together: build a social network

Now that we’ve covered the basics of working with Rama, let’s work on a real project: building a fully scalable social network with bidirectional relationships, profiles, posting on a user’s wall, and simple analytics. This will give you a better sense of how all the pieces fit together, and you’ll also be introduced to new tools.

The application will be called "RamaSpace" and will only be 180 lines of code. All the code on this page can be found in the [rama-examples](https://github.com/redplanetlabs/rama-examples) project.

## Designing your application

When you’re designing a Rama program, you start by figuring out what tasks you need to support, and from there what PStates will support those tasks and what depots and ETLs are needed to correctly maintain those PStates.

Our application will consist of *users*, *profiles*, *friendship requests*, and *friendships*. Here are the tasks we’ll support:

**Users**

- Register a new user with a unique user ID, email, and display name
- Update a profile field
- Fetch password hash for a user (for login)
- Post a comment on any user’s wall
- View posts on a user’s wall (paginated)
- View number of posts on a user’s wall

**Friendships**

- Request friendship with another user
- View friendship requests
- Cancel friendship request
- Accept friendship request
- Check if two users are currently friends
- View all friends for a user (paginated)
- View number of friends for a user
- Unfriend existing friend

**Analytics**

- Query for number of profile views for a user over a range of hours

Multiple PStates are needed to support all these tasks, although some tasks are supported by the same PState. As you gain experience using Rama, mapping the set of queries you need to support your application to a collection of PStates becomes second nature. To support the tasks for this application, we’ll build the following PStates:

**$$profiles**

```java
{userId<String>:
  {"displayName":      <String>,
   "email":            <String>,
   "profilePic":       <String>,
   "bio":              <String>,
   "location":         <String>,
   "pwdHash":          <Integer>,
   "joinedAtMillis":   <Long>,
   "registrationUUID": <String>
   }}
```

**$$outgoingFriendRequests**

```java
{userId<String>: Set<userId<String>>}
```

**$$incomingFriendRequests**

```java
{userId<String>: Set<userId<String>>}
```

**$$friends**

```java
{userId<String>: Set<userId<String>>}
```

**$$posts**

```java
{userId<String>: {postId<Long>: <Post>}}
```

**$$postId**

```java
<Long>
```

**$$profileViews**

```java
{userId<String>: {hourBucket<Long>: count<Long>}}
```

## Defining application queries

Let’s now take a look at all the queries on these PStates that will support our application. Since supporting a set of queries is a primary goal of any Rama application, this is a great place to start to make sure your PState structure is sound.

For these queries to be efficient, some of the PStates will take advantage of an important PState feature which hasn’t been discussed yet: subindexing. Subindexing is specified as a flag on an inner data structure in a PState schema and indicates the elements of that data structure should be indexed individually. Without subindexing, the entire data structure will be stored and retrieved as a single value. This will get expensive once there’s even just a few hundred elements in it. Subindexing enables inner data structures to efficiently contain huge numbers of elements, even more than could fit into memory. You can read more about subindexing on [this page](pstates.html). Enabling subindexing is extremely simple – here’s the declaration of the `"$$friends"` PState:

```java
friends.pstate(
  "$$friends",
  PState.mapSchema(
    String.class,
    PState.setSchema(String.class).subindexed()));
```

The queries for RamaSpace are defined in the class `RamaSpaceClient`. `RamaSpaceClient` wraps a cluster manager and maps application-level concepts into Rama queries. The constructor fetches all PStates for RamaSpace into fields on the class like so:

```java
String moduleName = RamaSpaceModule.class.getName();
_profiles = cluster.clusterPState(moduleName, "$$profiles");
_outgoingFriendRequests = cluster.clusterPState(moduleName, "$$outgoingFriendRequests");
_incomingFriendRequests = cluster.clusterPState(moduleName, "$$incomingFriendRequests");
_friends = cluster.clusterPState(moduleName, "$$friends");
_posts = cluster.clusterPState(moduleName, "$$posts");
_profileViews = cluster.clusterPState(moduleName, "$$profileViews");
```

Each RamaSpace query is defined as a `RamaSpaceClient` method. Let’s start with the query that fetches the password hash for a user. This is used to authenticate a user when logging in.

```java
public Integer getPwdHash(String userId) {
  return _profiles.selectOne(Path.key(userId, "pwdHash"));
}
```

The `"$$profiles"` PState has a slightly different structure than you’ve seen before. The inner map containing profile fields is a "fixed keys schema", which is similar to named columns on tables in a relational database. So those inner maps only contain the declared set of specified keys.

The password hash for a user is in the "pwdHash" field for their profile map. So this query is just a simple navigation into two maps: the outer map mapping user IDs to profile maps, and the inner map mapping profile fields to values.

Next, let’s take a look at the query for fetching all the profile information needed for a user’s profile page:

```java
public Profile getProfile(String userId) {
  Map profile = _profiles.selectOne(
                  Path.key(userId)
                      .subMap("displayName",
                              "location",
                              "bio",
                              "email",
                              "profilePic",
                              "joinedAtMillis"));
  if(profile.isEmpty()) return null;
  else return new Profile((String) profile.get("email"),
                          (String) profile.get("displayName"),
                          (String) profile.get("bio"),
                          (String) profile.get("location"),
                          (String) profile.get("profilePic"),
                          (long) profile.get("joinedAtMillis"));
}
```

This query specifies all the desired fields in the `subMap` navigator. The data in the map returned by `_profiles.selectOne` is then packaged into the first-class type `Profile`. `Profile` is a just a plain Java object.

Next, let’s take a look at getting the number of friends for a user:

```java
public long getFriendsCount(String userId) {
  return _friends.selectOne(Path.key(userId).view(Ops.SIZE));
}
```

This query navigates to the set of friends for a user ID and runs the `Ops.SIZE` function on it to get the friend count. Importantly, this path executes *entirely on the server*. So all that’s transferred from the client to the server is the path, and all that’s transferred back is the size. You can run any function that’s defined on both the client and module in query paths like this. These functions just have to implement the `RamaFunction1` interface.

As mentioned the inner set in the `"$$friends"` PState is subindexed, so each set element is indexed separately. This allows a user to have a large number of friends (even millions of friends) while still supporting fast queries. Internally, Rama automatically indexes the size of subindexed structures so that querying for their sizes is a fast, constant-time operation. You can read more about this on the [page about PStates](pstates.html).

Here’s the query to check if two users are friends:

```java
public boolean isFriends(String userId1, String userId2) {
  return !_friends.select(Path.key(userId1).setElem(userId2)).isEmpty();
}
```

Unlike `selectOne`, which must navigate to exactly one value, `select` returns a list of all values navigated to by the path. This path uses the `setElem` navigator which navigates to the specified value in the set only if it exists. So this path navigates to either one value or zero values, and the two users are currently friends if the returned list is not empty. Note that it doesn’t matter whether the friends of `userId1` or `userId2` are checked: the ETL that maintains the `"$$friends"` PState will ensure friendship relationships are always bidirectional.

Here is the query to retrieve a page of someone’s friends. Since someone could have many friends, the UI will display no more than twenty friends at a time:

```java
public Set<String> getFriendsPage(String userId, String start) {
  return _friends.selectOne(Path.key(userId).sortedSetRangeFrom(start, SortedRangeFromOptions.maxAmt(20).excludeStart()));
}
```

Subindexed sets and maps are *sorted*. The `sortedSetRangeFrom` navigator gets the subset of up to twenty friends starting from the given value. It works by iterating through the subindexed set starting from that value for that many elements. To get the first page of friends the `start` value would be set to the empty string, which is the smallest string value. To get the next page of friends, the start value would be the last value from the previous page. The `excludeStart` option is used here so as not to fetch the last value from the previous page again.

Note that this PState structure will paginate through friends in user ID order. Depending on your application, you may desire something different. For example, if you wanted to iterate through friends in the order in which the friendships were created, a linked set structure would be appropriate. The [rama-helpers](https://github.com/redplanetlabs/rama-helpers) repository contains a helper called `KeyToLinkedEntitySetPStateGroup` that provides this functionality.

Paginating through outgoing and incoming friendship requests is exactly the same, but on different PStates:

```java
public Set<String> getOutgoingFriendRequests(String userId, String start) {
  return _outgoingFriendRequests.selectOne(Path.key(userId).sortedSetRangeFrom(start, SortedRangeFromOptions.maxAmt(20).excludeStart()));
}

public Set<String> getIncomingFriendRequests(String userId, String start) {
  return _incomingFriendRequests.selectOne(Path.key(userId).sortedSetRangeFrom(start, SortedRangeFromOptions.maxAmt(20).excludeStart()));
}
```

Next, let’s take a look at the query to get the number of profile views for a user over a range of hours:

```java
public long getNumProfileViews(String userId, long startHourBucket, long endHourBucket) {
  return _profileViews.selectOne(
           Path.key(userId)
               .sortedMapRange(startHourBucket, endHourBucket)
               .subselect(Path.mapVals())
               .view(Ops.SUM));
}
```

This query is a great example of the power and flexibility of Rama’s path-based query API. The `"$$profileViews"` PState stores the number of profile views for a user ID for every hour of time. It’s a map from user ID to a subindexed map of hour bucket to view count. This query gets the sum of the counts for every hour in the requested range. Like the previous query invoking a function as part of the path, this entire query executes server-side. Let’s walk through how this path works:

- `Path.key(userId)`: This navigates to the subindexed map for `userId`
- `.sortedMapRange(startHourBucket, endHourBucket)`: This navigates to the submap containing keys only between that range of hours. Recall that subindexed maps and sets are sorted.
- `.subselect(Path.mapVals())`: This part is a little subtle. We wish to sum together all the values for all those buckets, but in order to do so we need all the values together in a single collection. The `mapVals` navigator navigates to each map value *separately*. So that navigator is wrapped in `subselect` which performs a full `select` at that point in the navigation. `subselect` here navigates to the sequence of values navigated by `Path.mapVals()`. The end result of the `subselect` is a list of all map values.
- `.view(Ops.SUM)`: At this point we are navigated to a list of all counts for every hour in that range. This last navigator finishes the query by summing together those values with `Ops.SUM`. `Ops.SUM` takes in as input a single list of values and returns the sum.

The computation cost of this query is linear with the number of requested hours. So for a very large range of time, such as five years, it will iterate and sum a huge number of values. If this is a use case you wish to optimize, there are more sophisticated PState structures aggregating additional time granularities that can accelerate queries like this. We’ll leave that as an exercise for the reader.

Next, here’s the query for the number of posts on a user’s wall. It’s the same as getting someone’s friend count.

```java
public long getPostsCount(String userId) {
  return _posts.selectOne(Path.key(userId).view(Ops.SIZE));
}
```

The last query is resolving a page of posts on a user’s wall. This query is more complex because there’s more information that needs to be retrieved besides what’s in the `"$$posts"` PState, such as the display name and profile pic of the posting user for each post on the page. This query is implemented as a query topology and will be covered later on this page.

## Defining application data

Let’s now define the types for all the data comprising this application. Most of these types define the raw data being appended to depots. While nothing is stopping you from using unstructured tools like JSON for this data, we recommend using first-class types with strong validation of fields. If you have a bug that’s creating invalid data, it’s much easier to debug when you get an exception creating the data than downstream when the data is being read.

Because these data types will be transferred across the network – for depot appends, between tasks during topology execution, or for PState queries – Rama needs to know how to serialize them. RamaSpace will make use of a built-in serialization mechanism provided by Rama. By simply implementing the `RamaSerializable` class, Rama will use Java serialization whenever that type needs to be serialized. Java serialiation isn’t the most efficient, so you can integrate other serialization mechanisms for greater efficiency. See [the page on serialization](serialization.html) for details.

To illustrate the structure of RamaSpace’s types, here are a few of them:

```java
public class UserRegistration implements RamaSerializable {
  public String userId;
  public String email;
  public String displayName;
  public int pwdHash;

  public UserRegistration(String userId, String email, String displayName, int pwdHash) {
    this.userId = userId;
    this.email = email;
    this.displayName = displayName;
    this.pwdHash = pwdHash;
  }
}

public class FriendRequest implements RamaSerializable {
  public String userId;
  public String toUserId;

  public FriendRequest(String userId, String toUserId) {
    this.userId = userId;
    this.toUserId = toUserId;
  }
}

public class Post implements RamaSerializable {
  public String userId;
  public String toUserId;
  public String content;

  public Post(String userId, String toUserId, String content) {
    this.userId = userId;
    this.toUserId = toUserId;
    this.content = content;
  }
}
```

These types use public fields to make use of a helper facility from [rama-helpers](https://github.com/redplanetlabs/rama-helpers) to make it easier to write topologies. This is not a requirement.

Here is the full list of datatypes and their fields:

- `UserRegistration[userId<String>, email<String>, displayName<String>, pwdHash<Integer>, registrationUUID<String>]`
- `FriendRequest[userId<String>, toUserId<String>]`
- `CancelFriendRequest[userId<String>, toUserId<String>]`
- `FriendshipAdd[userId1<String>, userId2<String>]`
- `FriendshipRemove[userId1<String>, userId2<String>]`
- `ProfileEdit[userId<String>, field<String>, value<Object>]`
- `Post[userId<String>, toUserId<String>, content<String>]`
- `Profile[email<String>, displayName<String>, bio<String>, location<String>, profilePic<String>, joinedAtMillis<Long>]`
- `ResolvedPost[userId<String>, content<String>, displayName<String>, profilePic<String>]`

`Profile` and `ResolvedPost` are used for query results, while the other types are used for depot appends.

## Defining `RamaSpaceModule`

Now let’s get to the fun part! All of the functionality described will only require 180 lines of code to implement as a module. It will be fully scalable and fault-tolerant in addition to being so easy to implement.

Let’s explore this module from the top-down, starting from the `define` method for the `RamaModule` interface. Remember, this is the only method that a module implements and defines all the depots, PStates, ETLs, and query topologies for the module.

```java
@Override
public void define(Setup setup, Topologies topologies) {
  setup.declareDepot("*userRegistrationsDepot", Depot.hashBy(UserIdExtract.class));
  setup.declareDepot("*profileEditsDepot", Depot.hashBy(UserIdExtract.class));
  setup.declareDepot("*profileViewsDepot", Depot.hashBy(ToUserIdExtract.class));
  setup.declareDepot("*friendRequestsDepot", Depot.hashBy(UserIdExtract.class));
  setup.declareDepot("*friendshipChangesDepot", Depot.hashBy(UserId1Extract.class));
  setup.declareDepot("*postsDepot", Depot.hashBy(ToUserIdExtract.class));

  declareUsersTopology(topologies);
  declareFriendsTopology(topologies);
  declarePostsTopology(topologies);
  declareProfileViewsTopology(topologies);

  topologies.query("resolvePosts", "*forUserId", "*startPostId").out("*resultMap")
            .hashPartition("*forUserId")
            .localSelect("$$posts", Path.key("*forUserId").sortedMapRangeFrom("*startPostId", 20)).out("*submap")
            .each(Ops.EXPLODE_MAP, "*submap").out("*i", "*post")
            .macro(extractJavaFields("*post", "*userId", "*content"))
            .hashPartition("*userId")
            .localSelect("$$profiles", Path.key("*userId", "displayName")).out("*displayName")
            .localSelect("$$profiles", Path.key("*userId", "profilePic")).out("*profilePic")
            .each(ResolvedPost::new, "*userId", "*content", "*displayName", "*profilePic").out("*resolvedPost")
            .originPartition()
            .compoundAgg(CompoundAgg.map("*i", Agg.last("*resolvedPost"))).out("*resultMap");
}
```

The depots and query topology for this module are defined in this method, while the four ETL topologies are delegated to helper functions. Let’s start with the depots.

### Depots

The depots correspond to the categories of events happening in the application. Not every data type has its own depot. Some of these depots receive multiple data types. For example, the `"*friendRequestsDepot"` receives both `FriendRequest` and `CancelFriendRequest` data. This is necessary so that different types of data that affect the same PStates are processed in the order in which they happened. As an illustration of this, suppose a user is spamming the "request friend" and "cancel friend request" buttons on the RamaSpace UI. This will generate many `FriendRequest` and `CancelFriendRequest` records. If those were kept on separate depots, there’s no guarantee as to the order in which they will be processed. The user could generate data in this order:

- `FriendRequest`
- `CancelFriendRequest`
- `FriendRequest`
- `CancelFriendRequest`

But they could be processed in this order:

- `FriendRequest`
- `CancelFriendRequest`
- `CancelFriendRequest`
- `FriendRequest`

Only by keeping these two datatypes on the same depot can the order of processing match the order in which the data was generated.

Likewise, how a depot is partitioned is critical for ensuring this ordering. The order in which data on different partitions of a depot is processed is undefined because they are being processed in parallel. If `"*friendRequestsDepot"` were defined with `Depot.random()` partitioning, you will run into the same ordering mismatch. So `"*friendRequestsDepot"` instead defines its partitioning as `Depot.hashBy(UserIdExtract.class)`. This class ensures `FriendRequest` and `CancelFriendRequest` from the same user always goes to the same partition. `UserIdExtract` is defined as follows:

```java
public static class UserIdExtract extends TopologyUtils.ExtractJavaField {
  public UserIdExtract() {
    super("userId");
  }
}
```

This uses the helper class `TopologyUtils.ExtractJavaField` from [rama-helpers](https://github.com/redplanetlabs/rama-helpers) to extract the `userId` field from the data being appended. Both `FriendRequest` and `CancelFriendRequest` have a field `userId`.

The definitions of the other depots all use the exact same logic. You’ll see specifically how the data from each depot is used in the definitions of the ETL topologies.

### Users topology

The users topology handles user registrations and profiles. Let’s start with the declaration of the topology:

```java
StreamTopology users = topologies.stream("users");
```

Streaming is used for this topology because low latency updates (a few milliseconds) are necessary for these use cases. Next is the declaration of the one PState for this topology:

```java
users.pstate(
  "$$profiles",
  PState.mapSchema(
    String.class,
    PState.fixedKeysSchema(
      "displayName", String.class,
      "email", String.class,
      "profilePic", String.class,
      "bio", String.class,
      "location", String.class,
      "pwdHash", Integer.class,
      "joinedAtMillis", Long.class,
      "registrationUUID", String.class
      )));
```

This is the "fixed keys schema" that was mentioned before. For each user ID, a map with a fixed set of fields is stored. These fields represent all profile information for that user, and each field has its own schema.

|  |  |
| --- | --- |
|  | User IDs in RamaSpace are the same as usernames. This structure makes it hard to implement the feature of a user being able to change their username. A more flexible design would track user IDs and usernames separately. |

Next is handling user registrations. The key task to handle is detecting whether the registration succeeded or whether the requested user ID already exists. An incorrect way to do this would be to first query the `"$$profiles"` PState to check if the user ID exists and then append a `UserRegistration` if the user ID is available. This doesn’t work because there’s a race condition between two clients trying to register the same user ID at the same time: they could both see the user ID doesn’t currently exist and then both append a `UserRegistration` record.

The correct way to handle this is within the ETL code that processes `UserRegistration` records. Each `UserRegistration` is an *attempt* to register that user ID – if the user ID currently exists, it doesn’t do anything. In order for a client to know whether their registration succeeded or not, the client provides a UUID with the registration that gets written into the `"$$profiles"` PState if and only if the user ID didn’t exist. Here’s the code:

```java
users.source("*userRegistrationsDepot").out("*registration")
     .macro(extractJavaFields("*registration", "*userId", "*email", "*displayName", "*pwdHash", "*registrationUUID"))
     .each(System::currentTimeMillis).out("*joinedAtMillis")
     .localTransform("$$profiles",
       Path.key("*userId")
           .filterPred(Ops.IS_NULL)
           .multiPath(Path.key("email").termVal("*email"),
                      Path.key("displayName").termVal("*displayName"),
                      Path.key("pwdHash").termVal("*pwdHash"),
                      Path.key("joinedAtMillis").termVal("*joinedAtMillis"),
                      Path.key("registrationUUID").termVal("*registrationUUID")));
```

This code takes advantage of fact that events for a single task run in sequence. So while this code is processing a `UserRegistration`, no other events for the same user ID can be running at the same time (but events for different user IDs on different tasks can be running in parallel). This is a property you can take advantage for other goals, such as updating multiple PStates on the same task and ensuring those changes become visible atomically.

This ETL code takes advantage of the depot partitioning for `"*userRegistrationsDepot"`. The depot is configured to partition by the user ID inside the `UserRegistration` objects. So when this ETL code begins processing a `UserRegistration`, it’s already on the task with the correct partition of the `"$$profiles"` PState for that user ID.

The next step is extracting the information needed from the `UserRegistration` to perform the work of the ETL. Here you can see a facility of Rama’s dataflow API that hasn’t been introduced yet: macros. Macros are a way to decompose and reuse chunks of dataflow code. `macro` is given a block of code and expands that block of code into that callsite. This `macro` call uses the `extractJavaFields` function defined in [rama-helpers](https://github.com/redplanetlabs/rama-helpers) and expands to:

```java
users.source("*userRegistrationsDepot").out("*registration")
     .each(new ExtractJavaField("userId"), "*registration").out("*userId")
     .each(new ExtractJavaField("email"), "*registration").out("*email")
     .each(new ExtractJavaField("displayName"), "*registration").out("*displayName")
     .each(new ExtractJavaField("pwdHash"), "*registration").out("*pwdHash")
     .each(new ExtractJavaField("registrationUUID"), "*registration").out("*registrationUUID")
     .each(System::currentTimeMillis).out("*joinedAtMillis")
     .localTransform("$$profiles",
       Path.key("*userId")
           .filterPred(Ops.IS_NULL)
           .multiPath(Path.key("email").termVal("*email"),
                      Path.key("displayName").termVal("*displayName"),
                      Path.key("pwdHash").termVal("*pwdHash"),
                      Path.key("joinedAtMillis").termVal("*joinedAtMillis"),
                      Path.key("registrationUUID").termVal("*registrationUUID")));
```

`ExtractJavaField` is also defined in [rama-helpers](https://github.com/redplanetlabs/rama-helpers) and extracts the public field from a Java object with that name. The `extractJavaFields` function produces code which maps the desired output variable names to field names. There’s much more to say about how to use macros, and you can read more about them on [this page](intermediate-dataflow.html).

The rest of the ETL code is straightforward. It writes the user registration information into the `$$profiles` PState only if that user ID doesn’t currently exist. The key code is the `filterPred(Ops.IS_NULL)` navigator, which only continues navigation if the user ID doesn’t exist.

With the ETL explained, we can now complete the story of how user registration works. A client does the following procedure to attempt to register a user ID:

- Append a `UserRegistration` object to `"*userRegistrationsDepot"`. Recall that a `UserRegistration` records contains a unique `registrationUUID`.
- This depot append uses `AckLevel.ACK` (which is the default if not specified). With this ack level the depot append call will only complete when all streaming topologies colocated with the depot have finished processing the data.
- The client queries the `"$$profiles"` PState to check if the `"registrationUUID"` field matches the `registrationUUID` included with the depot append. If so, the user registration was successful. If not, the user ID already existed.

Here’s the client code for this:

```java
public boolean appendUserRegistration(String userId, String email, String displayName, int pwdHash) {
  String registrationUUID = UUID.randomUUID().toString();
  _userRegistrationsDepot.append(new UserRegistration(userId, email, displayName, pwdHash, registrationUUID));
  String storedUUID = _profiles.selectOne(Path.key(userId, "registrationUUID"));
  return registrationUUID.equals(storedUUID);
}
```

Although only a small amount of code is required for handling user registrations, both in the module and client-side, it’s a great example of composing together Rama’s primitives to achieve application-level goals.

The last piece of the users topology is handling edits to existing profiles. This is accomplished by processing `ProfileEdit` data from the `"*profileEditsDepot"` like so:

```java
users.source("*profileEditsDepot").out("*edit")
     .macro(extractJavaFields("*edit", "*userId", "*field", "*value"))
     .localTransform("$$profiles", Path.key("*userId", "*field").termVal("*value"));
```

Like user registrations, this code takes advantage of the partitioning of `"*profileEditsDepot"`. It simply reads the field and value from the `ProfileEdit` and overwrites those in the `"$$profiles"` PState.

### Friends topology

The friends topology handles friend requests and maintaining a bidirectional social graph. A user can view both incoming and outgoing friend requests, can cancel pending friend requests, and can unfriend existing friends. Let’s start with the declaration of the topology and the PStates:

```java
StreamTopology friends = topologies.stream("friends");
friends.pstate(
  "$$outgoingFriendRequests",
  PState.mapSchema(
    String.class,
    PState.setSchema(String.class).subindexed()));
friends.pstate(
  "$$incomingFriendRequests",
  PState.mapSchema(
    String.class,
    PState.setSchema(String.class).subindexed()));
friends.pstate(
  "$$friends",
  PState.mapSchema(
    String.class,
    PState.setSchema(String.class).subindexed()));
```

This is a use case where either streaming or microbatching could be used depending on what exactly the application designer desires. Streaming ensures new friend requests and friendships are visible very quickly, while microbatching would be able to handle higher throughput while having higher latency (on order of a few hundred milliseconds).

There are three PStates for this topology all with the same structure: a map of user ID to a subindexed set of user IDs. These PStates track outgoing friend requests, incoming friend requests, and friendship relations. Any friendship will correspond to two pieces of data in the `"$$friends"` PState for each direction of the friendship. Each direction of a friendship could be stored on different partitions of the PState since the two user IDs are unrelated.

Let’s now take a look at the code for handling friend requests and cancellations of friend requests:

```java
friends.source("*friendRequestsDepot").out("*request")
       .macro(extractJavaFields("*request", "*userId", "*toUserId"))
       .subSource("*request",
         SubSource.create(FriendRequest.class)
                  .compoundAgg("$$outgoingFriendRequests", CompoundAgg.map("*userId", Agg.set("*toUserId")))
                  .hashPartition("*toUserId")
                  .compoundAgg("$$incomingFriendRequests", CompoundAgg.map("*toUserId", Agg.set("*userId"))),
         SubSource.create(CancelFriendRequest.class)
                  .compoundAgg("$$outgoingFriendRequests", CompoundAgg.map("*userId", Agg.setRemove("*toUserId")))
                  .hashPartition("*toUserId")
                  .compoundAgg("$$incomingFriendRequests", CompoundAgg.map("*toUserId", Agg.setRemove("*userId"))));
```

There’s a new operation here that hasn’t been introduced yet: `subSource`. `subSource` dispatches processing based on the type of the input data. While you could accomplish the same thing here using `ifTrue`, `subSource` is especially convenient when you have more than two data types. In this case there are two branches of computation: one for the `FriendRequest` type, and one for the `CancelFriendRequest` type.

Let’s start with the processing for `FriendRequest`. `"*friendRequestsDepot"` is partitioned by the `userId` field, so the processing is already on the right task to update `"$$outgoingFriendRequests"`. This is done here with `compoundAgg` by adding `"*toUserId"` to the set of outgoing friend requests for `"*userId"`. You could also perform this write with `localTransform`, but the code is slightly shorter with `compoundAgg`. Next, the computation is relocated to the partition representing data for `"*toUserId"` and a similar `compoundAgg` call is done to update incoming friend requests for `"*toUserId"`.

The code handling `CancelFriendRequest` is the same, except removing elements from those nested sets instead of adding them.

Next, let’s take a look for handling friendship additions and removals:

```java
friends.source("*friendshipChangesDepot").out("*change")
       .macro(extractJavaFields("*change", "*userId1", "*userId2"))
       .anchor("start")
       .compoundAgg("$$incomingFriendRequests", CompoundAgg.map("*userId1", Agg.setRemove("*userId2")))
       .compoundAgg("$$outgoingFriendRequests", CompoundAgg.map("*userId1", Agg.setRemove("*userId2")))
       .hashPartition("*userId2")
       .compoundAgg("$$incomingFriendRequests", CompoundAgg.map("*userId2", Agg.setRemove("*userId1")))
       .compoundAgg("$$outgoingFriendRequests", CompoundAgg.map("*userId2", Agg.setRemove("*userId1")))
       .hook("start")
       .subSource("*change",
         SubSource.create(FriendshipAdd.class)
                  .compoundAgg("$$friends", CompoundAgg.map("*userId1", Agg.set("*userId2")))
                  .hashPartition("*userId2")
                  .compoundAgg("$$friends", CompoundAgg.map("*userId2", Agg.set("*userId1"))),
         SubSource.create(FriendshipRemove.class)
                  .compoundAgg("$$friends", CompoundAgg.map("*userId1", Agg.setRemove("*userId2")))
                  .hashPartition("*userId2")
                  .compoundAgg("$$friends", CompoundAgg.map("*userId2", Agg.setRemove("*userId1"))));
```

The idea here is when a user accepts a friendship request, only a single `FriendshipAdd` record is appended to the `"*friendshipChangesDepot"`. So this code is a little more involved because friendship requests must be cleared in addition to updating the `"$$friends"` PState.

|  |  |
| --- | --- |
|  | An alternative design is to append a `CancelFriendRequest` record as well when accepting a friend request and have the ETL code above only update the `"$$friends"` PState. This can be prone to errors though – if the client process crashes after sending the `FriendshipAdd` but before sending the `CancelFriendRequest`, the friend request won’t be cleared. When you have multiple actions you want to take in response to an event, it’s best to encode that as a single depot append. That way the system can’t get into a state where only some of the desired actions are taken. |

This code uses ["anchor" and "hook"](tutorial4.html#_branching_dataflow_graphs) to create two branches of computation. The first branch clears all incoming and outgoing requests between those two user IDs, and the other branch updates the `"$$friends"` PState in both directions.

### Posts topology

For the posts topology, let’s once again start with the declaration of the topology and the PStates:

```java
MicrobatchTopology posts = topologies.microbatch("posts");
posts.pstate(
  "$$posts",
  PState.mapSchema(
    String.class,
    PState.mapSchema(Long.class, Post.class).subindexed()));

TaskUniqueIdPState id = new TaskUniqueIdPState("$$postId").descending();
id.declarePState(posts);
```

Because this uses a microbatch topology, it will be able to handle very high throughput at the tradeoff of moderately increased latency of processing. Microbatching is used here for the purposes of illustration; whether you use a stream topology or microbatch topology depends on the latency you desire of new posts being visible on the user’s wall.

The `"$$posts"` PState is a map from user ID to post ID to `Post`. The inner map is subindexed because users will have lots of posts on their wall. Post IDs are generated *in descending order*, which causes a user’s wall to be stored in reverse chronological order. This is exactly how we desire posts to be ordered on the frontend. Getting a page of posts is as simple as using the `sortedMapRangeFrom` navigator, as you’ll see later in the `resolvePosts` query topology.

`TaskUniqueIdPState` is a utility you haven’t seen before that comes from the [rama-helpers](https://github.com/redplanetlabs/rama-helpers) repository. It’s a simple tool that generates IDs unique to this task. It keeps the next post ID value in the local partition for the `"$$postId"` PState. So the `"$$postId"` PState is partitioned numbers – not a map like you’ve been seeing so far. In this case the `TaskUniqueIdPState` is configured to generate IDs in descending order starting from the largest `Long` value.

`TaskUniqueIdPState` generates IDs that are unique *on the task*. These IDs will not be unique across tasks. In this case that’s all that’s needed. [rama-helpers](https://github.com/redplanetlabs/rama-helpers) contains another utility called `ModuleUniqueIdPState` which generates IDs unique across the entire module. It does this by incorporating the task ID in the generated ID.

Now let’s look at the topology code:

```java
posts.source("*postsDepot").out("*microbatch")
     .explodeMicrobatch("*microbatch").out("*post")
     .macro(extractJavaFields("*post", "*toUserId"))
     .macro(id.genId("*id"))
     .localTransform("$$posts", Path.key("*toUserId", "*id").termVal("*post"));
```

This is only five lines of code! The only new bit is the `genId` macro from `TaskUniqueIdPState` which generates a new ID and binds it to the `"*id"` var. Helper utilities like `TaskUniqueIdPState` often expose fine-grained functionality like this through macros.

Also of note is the lack of any partitioner calls: the partitioning of depots is respected by microbatch topologies the same as stream topologies, so a post begins processing here on the same depot partition it was appended to (partitioned by `toUserId`).

### Profile views topology

The last topology in RamaSpace is the profile views analytics topology. Recall that this ETL produces a PState to support a query for getting the number of profile views over a range of hours. Once again, let’s start with the declaration of the topology and PStates:

```java
MicrobatchTopology profileViews = topologies.microbatch("profileViews");
profileViews.pstate(
  "$$profileViews",
  PState.mapSchema(
    String.class,
    PState.mapSchema(Long.class, Long.class).subindexed()));
```

Microbatching is perfect for this use case. It’s completely fine for the update latency of analytics like this to be a few hundred milliseconds. There’s no reason to need faster latency than that especially since the granularity of analytics is an hour. Microbatching gives high throughput and great fault-tolerance semantics while barely making the code more complicated.

The PState is a map from user ID to hour bucket to number of profile views in that bucket. There are about 8760 hours in a year, so it’s critical that inner map be subindexed. Subindexing enables inner data structures like this to be of arbitrary size, and you’re only limited by the amount of disk space on the machine.

Let’s now take a look at the topology code:

```java
profileViews.source("*profileViewsDepot").out("*microbatch")
            .explodeMicrobatch("*microbatch").out("*profileView")
            .macro(extractJavaFields("*profileView", "*toUserId", "*timestamp"))
            .each((Long timestamp) -> timestamp / (1000 * 60 * 60), "*timestamp").out("*bucket")
            .compoundAgg("$$profileViews",
              CompoundAgg.map(
                "*toUserId",
                CompoundAgg.map(
                  "*bucket",
                  Agg.count())));
```

Like all the other topologies, this code is extremely straightforward. It’s straightforward because RamaSpace is straightforward, and Rama can express applications close to how you describe them by cutting out all the usual complexity.

While the write to the PState could use a `localTransform`, using a `compoundAgg` here is more concise. Aggregators automate routine things like initializing non-existent subvalues. In this case, the `Agg.count()` aggregator initializes the value for a bucket to `0` the first time it appears. You can read more about aggregators and other features they have [here](aggregators.html).

### `resolvePosts` query topology

The last piece of the RamaSpace implementation is the `resolvePosts` query topology. Rendering a page of posts on a user’s wall requires not just the posts themselves but also information about the users who made those posts. While you could query all this user info client-side for every post, that would require over twenty round-trips with the module to get all the information needed to render one page of posts. It’s far more efficient to perform all that work in a single round-trip with a query topology.

With a query topology you specify a parallelized computation using the exact same API as used for making ETLs. This computation can query any or all of the module’s PStates and any or all of the module’s tasks. Here is the code for `resolvePosts`:

```java
topologies.query("resolvePosts", "*forUserId", "*startPostId").out("*resultMap")
          .hashPartition("*forUserId")
          .localSelect("$$posts", Path.key("*forUserId").sortedMapRangeFrom("*startPostId", 20)).out("*submap")
          .each(Ops.EXPLODE_MAP, "*submap").out("*i", "*post")
          .macro(extractJavaFields("*post", "*userId", "*content"))
          .hashPartition("*userId")
          .localSelect("$$profiles", Path.key("*userId", "displayName")).out("*displayName")
          .localSelect("$$profiles", Path.key("*userId", "profilePic")).out("*profilePic")
          .each(ResolvedPost::new, "*userId", "*content", "*displayName", "*profilePic").out("*resolvedPost")
          .originPartition()
          .compoundAgg(CompoundAgg.map("*i", Agg.last("*resolvedPost"))).out("*resultMap");
```

At a high-level, this code fetches up to the next 20 posts for `"*forUserId"` starting from `"*startPostId"`. The first page would use a starting post ID of 0, which is the lowest possible ID. For each of these posts it partitions by the user ID to fetch their display name and profile pic, and then it combines all this information into a map to return back to the caller. To get the next page of posts, the client would invoke the query topology again with a starting post ID of one more than the highest post ID from the last page.

Query topologies are *batched computations*, a concept that hasn’t been introduced yet. So you’ll have to do a little more reading to fully understand this code. See [this page](query.html) for all the details on making query topologies.

Lastly, here’s how this query topology is used client-side. First, here’s how a handle to the query topology is created in the constructor of `RamaSpaceClient`:

```java
_resolvePosts = cluster.clusterQuery(moduleName, "resolvePosts");
```

Then, here’s how it’s invoked:

```java
public TreeMap<Long, ResolvedPost> resolvePosts(String userId, long index) {
  return new TreeMap(_resolvePosts.invoke(userId, index));
}
```

As you can see query topologies are invoked just like normal functions, except they perform highly scalable distributed computations to do their work.

## Unit tests

The last piece to the RamaSpace project is testing. `InProcessCluster`, which you’ve seen already for many examples, is a great environment for unit testing the behavior of modules. Here is an example of using `InProcessCluster` along with JUnit to test `RamaSpaceModule`:

```java
@Test
public void basicTest() throws Exception {
  try(InProcessCluster ipc = InProcessCluster.create()) {
    RamaSpaceModule ramaspace = new RamaSpaceModule();
    String moduleName = ramaspace.getClass().getName();
    ipc.launchModule(ramaspace, new LaunchConfig(4, 4));
    RamaSpaceClient client = new RamaSpaceClient(ipc);

    assertTrue(client.appendUserRegistration("alice", "alice@gmail.com", "Alice Alice", 1));
    assertFalse(client.appendUserRegistration("alice", "alice2@gmail.com", "Alice2", 2));
    assertEquals(1, client.getPwdHash("alice"));
    client.appendBioEdit("alice", "in wonderland");
    Profile profile = client.getProfile("alice");
    assertEquals("Alice Alice", profile.displayName);
    assertEquals("alice@gmail.com", profile.email);
    assertEquals("in wonderland", profile.bio);
    assertTrue(profile.joinedAtMillis > 0);
    assertNull(profile.location);

    assertTrue(client.appendUserRegistration("bob", "bob@gmail.com", "Bobby", 2));
    assertTrue(client.appendUserRegistration("charlie", "charlie@gmail.com", "Charles", 2));

    for(int i=0; i<8; i++) {
      client.appendPost("alice", "alice", "x" + i);
      client.appendPost("charlie", "alice", "y" + i);
      client.appendPost("bob", "alice", "z" + i);
    }

    ipc.waitForMicrobatchProcessedCount(moduleName, "posts", 24);

    TreeMap<Long, ResolvedPost> page1 = client.resolvePosts("alice", 0);
    assertEquals(20, page1.size());
    TreeMap<Long, ResolvedPost> page2  = client.resolvePosts("alice", page1.lastKey() + 1);
    assertEquals(4, page2.size());
  }
}
```

The basic pattern of testing a module is to append some data and then validate the PStates change in the expected ways. How you coordinate those assertions depends on the type of topology updating the PStates in question. For example, in this code assertions on profile updates can happen right after the depot appends because the depot appends don’t complete until the "profiles" stream topology finishes processing those appends. On the other hand, the "posts" topology does not coordinate with depot appends because it’s a microbatch topology. So `InProcessCluster` provides the utility `waitForMicrobatchProcessedCount` to block until that topology has processed the expected number of records. Because 24 posts are appended in the test, the `waitForMicrobatchProcessedCount` call waits for 24 records to be processed. Then the `resolvePosts` query topology can be tested.

This unit test is only testing some of the functionality of `RamaSpaceModule`, as it’s only intended to illustrate how you would go about testing an application. Before deploying `RamaSpaceModule` to production you’d want to write more tests to validate all the different parts of the RamaSpace application.

As you can see, testing Rama modules is straightforward. This code also illustrates the ease of working with Rama from a client: you’re always working with application-level data types, and the details of where PStates, depots, and query topologies are located is managed for you. Using PStates, depots, and query topologies from a production client (like a web server) is exactly the same as in this unit test.

## Summary

You now have the core knowledge needed to build elegant, robust, scalable, high-performance backends end-to-end. There’s a world of possibilities even just with what you’ve learned so far. And yet there’s still many more capabilities of Rama to explore that will let you handle many more kinds of use cases. These capabilities include [dependencies between modules](module-dependencies.html), [fine-grained reactive queries](pstates.html#_reactive_queries), [subbatches](intermediate-dataflow.html), and more.

The rest of the documentation pages are deep dives into various aspects of Rama. You shouldn’t feel it necessary to learn every little detail of Rama’s capabilities. Instead, we recommend glancing over the rest of the docs to learn what’s available and coming back to them as you encounter those use cases. For running production clusters be sure to read [the page on operating Rama](operating-rama.html).

From here your learning journey can go many directions. You can try to build your own application, or you can explore more complex applications like our [Twitter-scale Mastodon implementation](downloads-maven-local-dev.html). You can also look at [rama-demo-gallery](https://github.com/redplanetlabs/rama-demo-gallery) which contains short, self-contained, thoroughly commented examples of applying Rama towards a variety of use cases. In all cases, `InProcessCluster` is a fantastic tool for playing with Rama with a fast feedback loop.