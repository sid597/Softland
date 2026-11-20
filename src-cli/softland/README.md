# Softland command-line namespaces

[Parent: the command-line program, source entry](../README.md).

This namespace root contains [cli/](cli/README.md): the program `softland`,
which speaks the words to the server over HTTP and is also the MCP server on
stdio. It gives the tree its namespace prefix, `softland.cli`. It owns no
state: the one thing that the program keeps between two runs is the
credentials file, outside the repository. Descend into cli for its source.
