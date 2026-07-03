# Downloads, Maven, and Local Development
> Source: https://redplanetlabs.com/docs/~/downloads-maven-local-dev.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Downloads, Maven, and local development

This page lists all downloads and Maven dependencies available. You’ll also learn how to set up your local development environment.

## Downloads

Rama can be downloaded [on this page](https://redplanetlabs.com/download).

We also have a number of open-source projects available on [our Github](https://github.com/redplanetlabs). These include:

- [rama-demo-gallery](https://github.com/redplanetlabs/rama-demo-gallery), containing short, self-contained, thoroughly commented demos of applying Rama towards a variety of use cases. This is a great resource for learning the basics of Rama usage. The project is set up to run Java examples with Maven and Clojure examples with Leiningen.
- [rama-helpers source code](https://github.com/redplanetlabs/rama-helpers). This library implements helpful utilities for implementing modules. For application development this library is available as a [Maven dependency](#_maven).
- [rama-examples source code](https://github.com/redplanetlabs/rama-examples), containing all examples in this documentation
- [rama-kafka source code](https://github.com/redplanetlabs/rama-kafka). This library enables a remote Kafka cluster to be used as a source for Rama topologies and makes it easy to publish records to a remote Kafka cluster from topologies. This library is also available as a [Maven dependency](#_maven).
- [twitter-scale-mastodon](https://github.com/redplanetlabs/twitter-scale-mastodon), our Twitter-scale Mastodon implementation in only 10k lines of code.

## Maven

`rama`, `rama-helpers`, and `rama-kafka` are available via a Maven repository managed by Red Planet Labs. Other libraries used by Rama are hosted in the [Clojars](https://clojars.org/) public repository. Here is the repository information you can add to your `pom.xml` file:

```java
<repositories>
  <repository>
    <id>nexus-releases</id>
    <url>https://nexus.redplanetlabs.com/repository/maven-public-releases</url>
  </repository>
  <repository>
    <id>clojars</id>
    <url>https://repo.clojars.org/</url>
  </repository>
</repositories>
```

To add `rama` as a dependency to your project, use the following dependency declaration:

```java
<dependency>
    <groupId>com.rpl</groupId>
    <artifactId>rama</artifactId>
    <version>1.5.0</version>
    <scope>provided</scope>
</dependency>
```

`rama-helpers` is available via a similar dependency declaration:

```java
<dependency>
    <groupId>com.rpl</groupId>
    <artifactId>rama-helpers</artifactId>
    <version>0.10.0</version>
</dependency>
```

Lastly, `rama-kafka` is available via this dependency declaration:

```java
<dependency>
    <groupId>com.rpl</groupId>
    <artifactId>rama-kafka</artifactId>
    <version>0.14.0</version>
</dependency>
```

## Setting up local development environment

Rama modules can be developed using whatever build tooling you’re comfortable with for JVM applications. The workflow of development is to test your modules using [InProcessCluster](testing.html) and then deploy to a real cluster using the [Rama CLI](#_using_the_rama_cli).

When running locally on `InProcessCluster`, you want to make sure to have a `log4j2.properties` file on the classpath so that you can see any logs emitted during your tests. Here’s a minimal `log4j2.properties` you can use:

```java
appender.console.name = console
appender.console.type = Console
appender.console.Target = SYSTEM_ERR
appender.console.layout.type = PatternLayout
appender.console.layout.pattern = %d{HH:mm:ss.SSS} %-5p [%t] %.20c - %m%n%throwable
appender.console.immediateFlush=true

rootLogger.level=ERROR
rootLogger.appenderRefs = console
rootLogger.appenderRef.console.ref = console

logger.rama.name=rpl.rama
logger.rama.level=WARN
```

The Rama CLI is in the same Rama release you use to set up Rama clusters. All you need to do is unpack the release somewhere on your machine and configure it to point to the cluster to which you want to deploy. See [Operating Rama](operating-rama.html) for the full details.