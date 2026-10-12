# Softland store namespaces

[Parent: store source](../README.md).

This namespace root contains [store/](store/README.md), the store's own code: the
act, the store's Rama module with its two gates and their records, the door, and
the read path.
It gives the tree its namespace prefix, `softland.store`. It owns no state and no
boot process; the module's durable state is in the cluster that runs it, and the
door's stocks, and what the read path holds of its live reads, are the memory of
the process that opens them. Descend into store for
its source.
