# All Configs
> Source: https://redplanetlabs.com/docs/~/all-configs.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# All configs

This page lists configs available for the [Conductor](terminology.html#_conductor), [Supervisors](terminology.html#_supervisor), [Workers](terminology.html#_worker), and [foreign](terminology.html#_foreign_context) clients. These configs are set at the start of process launch and can only be changed by restarting the process. For Conductors and Supervisors this involves killing and restarting the process. For Workers, this involves performing a module update.

Configs are set either through the `rama.yaml` file, through the `--configOverrides` flag for [module launch](operating-rama.html#_launching_modules) or [module update](operating-rama.html#_updating_modules), or programatically when creating a [RamaClusterManager](https://redplanetlabs.com/javadoc/com/rpl/rama/RamaClusterManager.html).

[Dynamic options](operating-rama.html#_worker_configurations_and_dynamic_options) are separate from the configs listed on this page and can be changed at any time. Dynamic options are documented on the [Cluster UI](operating-rama.html#_cluster_ui).

## Shared Conductor/Supervisor configs

These are specified for the Conductor and Supervisor through their `rama.yaml` files.

- `conductor.port`: port exposed by Conductor for cluster operation, defaults to 1973
- `local.dir`: path to directory for local state for Supervisor and module workers, defaults to "local-rama-data" (where the release is unpacked)
- `zookeeper.servers`: list of hostnames for all Zookeeper nodes, must be specified
- `zookeeper.port`: configured port for Zookeeper nodes, defaults to 2000
- `zookeeper.root`: root Zookeeper node for Rama to keep cluster metadata, defaults to "rama"
- `zookeeper.connection.timeout.millis`: connection timeout for Zookeeper connection, defaults to 5000
- `zookeeper.session.timeout.millis`: desired session timeout for Zookeeper client, defaults to 5000. See [the Zookeeper documentation](https://zookeeper.apache.org/doc/r3.3.3/zookeeperProgrammers.html).
- `zookeeper.retry.interval.millis`: base wait time to retry a failed Zookeeper request, defaults to 1000
- `zookeeper.retry.times`: number of times to retry failed Zookeeper request, defaults to 2
- `zookeeper.retry.interval.millis.ceiling`: maximum wait time to retry a failed Zookeeper request, defaults to 5000

## Conductor configs

These are specified through the Conductor’s `rama.yaml` file.

- `backup.provider`: external store configuration for [module backups](backups.html)
- `conductor.child.opts`: options string to JVM process for Conductor, defaults to `"-Xmx1024m"`
- `conductor.assignment.mode`: configures algorithm to assign modules to nodes, see [this section](operating-rama.html#_isolation_scheduler)
- `cluster.ui.port`: port serving the Cluster UI, defaults to 8888

## Supervisor configs

These are specified through the Supervisor’s `rama.yaml` file.

- `conductor.host`: hostname of Conductor, must be specified
- `zookeeper.servers`: list of hostnames for all Zookeeper nodes, must be specified
- `supervisor.host`: hostname reported by this Supervisor used by other nodes for network communication, defaults to result of calling [InetAddress.getLocalHost](https://docs.oracle.com/javase/7/docs/api/java/net/InetAddress.html#getLocalHost())
- `supervisor.child.opts`: options string to JVM process for Supervisor, defaults to `"-Xmx256m"`
- `supervisor.port.range`: pair of numbers for range of ports to assign to launched workers, defaults to `3000, 4000`. Should be a large range to ensure Supervisor never runs out of ports to assign
- `supervisor.heartbeat.frequency.millis`: frequency at which Supervisor heartbeats node state to Zookeeper, defaults to 5000
- `supervisor.worker.heartbeat.timeout.millis`: timeout for receiving a heartbeat from a worker, defaults to 10000. A Supervisor will kill and restart a timed out worker.
- `supervisor.worker.launch.timeout.millis`: timeout to receive first heartbeat from a launched worker, defaults to 90000. A Supervisor will kill and restart a timed out worker.
- `supervisor.labels`: list of labels for the node, used for [assigning modules to nodes with particular attributes](heterogenous-clusters.html)

## Worker configs

These are specified through the `--configOverrides` flag on the Rama CLI for [module launch](operating-rama.html#_launching_modules) or [module update](operating-rama.html#_updating_modules).

- `worker.child.opts`: options string to JVM process for Worker processes for this module instance, commonly used to configure memory and GC. Defaults to `"-Xmx4096m"`.
- `worker.max.direct.memory.size`: amount of direct memory to allocate to each worker process. Defaults to `"500m"`.
- `worker.heartbeat.frequency.millis`: frequency at which worker heartbeats to its Supervisor, defaults to 1000
- `worker.weft.client.max.threads`: number of download threads for [WEFT client](terminology.html#_weft), defaults to 10
- `worker.worp.server.threads`: number of threads to process incoming [WORP](terminology.html#_worp) messages, defaults to 10
- `replication.replog.recency.cache.size`: number of [replog entries](replication.html) to keep cached per [task thread](terminology.html#_task_group_task_thread), defaults to 1000
- `pstate.maximal.schema.validations`: Setting this to `false` disables potentially expensive schema validations for nested, non-subindexed data structures. This should always be set to `false` for production, as running tests with [IPC](testing.html) should give sufficient coverage that there are no schema validation problems with the topology code.
- `pstate.validate.subindexed.structure.locations`: Setting this to `false` turns off validation that subindexed structures aren’t moved or duplicated to a different location (e.g. under a different key). This should also be set to `false` for production since tests should give sufficient coverage of this.
- `pstate.rocksdb.options.builder`: PStates with a top-level map in the schema use [RocksDB](https://rocksdb.org/) as the underlying durable storage. This config lets you provide the full name of a class implementing [com.rpl.rama.RocksDBOptionsBuilder](https://redplanetlabs.com/javadoc/com/rpl/rama/RocksDBOptionsBuilder.html) to configure the RocksDB instances. By default, RocksDB is configured to use two-level indexing and have a 256MB block cache.

## Foreign configs

These are specified through either the `rama.yaml` file on the classpath of the client process or programatically through the constructor of [RamaClusterManager](https://redplanetlabs.com/javadoc/com/rpl/rama/RamaClusterManager.html).

- `conductor.host`: hostname of Conductor, must be specified
- `foreign.pstate.operation.timeout.millis`: Timeout to use for foreign PState queries
- `foreign.depot.operation.timeout.millis`: Timeout to use for foreign depot operations, including appends and partition queries
- `foreign.proxy.thread.pool.size`: size of the thread pool on PState clients to handle `ProxyState` callbacks, defaults to 2
- `foreign.proxy.failure.window.seconds`: Length of window for which to count proxy failures to determine if it should be forcibly terminated
- `foreign.proxy.failure.window.threshold`: Number of failures within the failure window which will cause proxy to be forcibly terminated

## Shared Worker/Foreign configs

- `custom.serializations`: Registers [custom serializations](serialization.html)
- `worp.client.threads`: number of [WORP](terminology.html#_worp) threads for sending outgoing messages, defaults to 10
- `worp.max.send.buffer.size`: maximum amount of pending outgoing messages for [WORP](terminology.html#_worp) connections, defaults to 120000