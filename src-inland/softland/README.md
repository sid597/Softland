# Softland product namespaces

[Parent: product source](../README.md).

This namespace root contains [inland/](inland/README.md), the isolated
Softland-in-Softland runtime. It groups the authored execution host and its
adapters without moving the existing `app.client` rendering families. It owns no
additional state or boot process; descend into inland for the ownership map and
its immediate source files.
