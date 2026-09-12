# Heterogenous Clusters
> Source: https://redplanetlabs.com/docs/~/heterogenous-clusters.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Heterogenous clusters

Modules can have wildly different resource needs between them. One module may need a large amount of memory, and another module might need access to GPUs. Rama provides a "label" system that allows a cluster with different kinds of hardware to be efficiently used by scheduling certain modules to certain kinds of nodes.

A supervisor can have any number of labels defined in its `rama.yaml` file under the `supervisor.labels` key. Here’s an example `rama.yaml` using this:

```java
zookeeper.servers:
  - "1.2.3.4"
  - "5.6.7.8"
conductor.host: "9.10.11.12"
local.dir: "/data/rama"
supervisor.port.range:
  - 2500
  - 3500
supervisor.labels:
  - "gpu"
  - "xlarge-cpu"
```

Modules can then target a particular label when being deployed. If the conductor is using the dev scheduling mode (the default), the target label is passed as part of the deploy command like this:

```java
rama deploy \
--action launch \
--jar target/my-application.jar \
--module com.mycompany.MyModule \
--tasks 64 \
--threads 16 \
--workers 8 \
--replicationFactor 3 \
--targetLabel xlarge-cpu
```

The module will only be scheduled onto nodes with that label, and if not enough nodes exist with that label to satisfy the assignment the deploy will fail. `--targetLabel` is optional.

The target label must also be passed on module update, similar to config overrides. This allows a label to be unset by simply not passing it. The target label does not have to be passed when scaling a module, however. This is also the same as config overrides.

If the Conductor is using the [isolation scheduler](operating-rama.html#_isolation_scheduler), the target label is specified as part of the module’s config in the Conductor’s `rama.yaml`. For example, this could be the isolation config:

```java
conductor.assignment.mode:
  type: isolation
  modules:
    com.mycompany.Module1:
      numSupervisors: 4
      targetLabel: xlarge-cpu
    com.mycompany.AnotherModule: 8
    com.mycompany.Module2:
      numSupervisors: 12
      targetLabel: gpu
    com.mycompany.Module3:
      numSupervisors: 10
      targetLabel: xlarge-cpu
```

Each module must have `numSupervisors` specified, but `targetLabel` is optional. If the config for a module is just a number, then that is interpreted as the `numSupervisors` value.

When deploying a module in isolation mode, the scheduler takes into account all the other unscheduled modules when choosing nodes. It makes a best effort to choose an assignment that also ensures the other modules will be able to be scheduled as well.