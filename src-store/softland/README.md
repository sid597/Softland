# Softland store namespaces

[Parent: store source](../README.md).

This namespace root contains [store/](store/README.md), the store's own code: the
act, the store's Rama module with its two gates, their records and their logs
of answers, the door, the read path, and the rebuild.
It gives the tree its namespace prefix, `softland.store`. It owns no state and no
boot process; the module's durable state is in the cluster that runs it, and
what the read path holds of its live reads is the memory of the process that
opens it. The door holds nothing between two calls, and the rebuild holds
nothing after a call. Descend into store for
its source.
