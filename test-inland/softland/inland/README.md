# Inland JVM suite

[Parent: JVM checks](../README.md) · [Verification map](../../README.md).

[test_runner.clj](test_runner.clj) owns one fresh two-task Rama IPC for five named
scenarios. It installs foreign handles for admission/proxy tests and closes the
IPC after execution. Its namespace and helper docstrings distinguish actual Rama
from the small pure reference interpreter. Scenario assertions are the local
executable contracts; browser behavior and external execution belong to the
sibling browser driver, linked from the verification map.
