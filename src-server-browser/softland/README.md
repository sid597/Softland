# Softland browser namespaces

[Parent: the browser's face, source entry](../README.md).

This namespace root contains [browser/](browser/README.md): the Electric
program of the browser's face, the entry that boots its client half, the host
that serves it in front of the face over HTTP, and the program that runs them.
It gives the tree its namespace prefix, `softland.browser`. It owns no state:
what a socket's program holds is Electric's and ends with the socket, and what
a page holds is the browser's. Descend into browser for its source.
