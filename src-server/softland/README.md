# Softland server namespaces

[Parent: server source](../README.md).

This namespace root contains [server/](server/README.md), the server's own
code: the Rama module of its sessions with the module's client, the seam, the
words, what sign-in writes, the check of Google's token, the face over HTTP,
and the program that runs them. It gives the tree its namespace prefix,
`softland.server`. It owns no state and no boot process: what lasts is in the
cluster, and what a running server holds of its open streams and sockets is the
memory of its process. Descend into server for its source.
