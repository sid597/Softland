# Inland JVM suite

[Parent: JVM checks](../README.md) · [Verification map](../../README.md).

[scenes_test.clj](scenes_test.clj) owns one fresh two-task in-process cluster with
the rig's store module and drives two pages through the adapter as the browser
drives them (gestures with the page's local cells, and watched reads): scene one,
the pointer's rule changed while in use; scene two, the pair made (the base
switching to shared), a member accepted after it was made, a rule promoted into the
pair and used by the other person; scene three, a note naming a person, the person
forgotten, the note erased on its date on both pages. Every value it checks reached
the page through the rig's one read exit and a push. It is what `bin/inland check`
runs.

[test_runner.clj](test_runner.clj) is the suite of Inland's own module, which the
rig's store replaced as the authority: it calls the old adapter's `submit!` and
PState paths, so it no longer compiles against `store` and is not run. Kept until
Sid rules whether to port or remove it (the pointer screen's PROGRESS.md lists it).
