# Softland runtime namespaces

[Parent: runtime source](../README.md).

This namespace root contains [runtime/](runtime/README.md), the runtime's own
code. It gives the tree its namespace prefix, `softland.runtime`. It owns no
state and no boot process: what lasts is in the cluster, and what a running
runtime holds of its open connections is the memory of its process.
