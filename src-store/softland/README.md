# Softland store namespaces

[Parent: store source](../README.md).

This namespace root contains [store/](store/README.md), the store's own code: the
act, and the store's Rama module with its two gates and their records. It gives
the tree its namespace prefix, `softland.store`. It owns no state and no boot
process; the module's durable state is in the cluster that runs it. Descend into
store for its source.
