# Build notes — read exit (phase 5a build session)

Running log for the rama skill's `build` phase of the one-owner read exit
(PLAN-read-exit.md, validated minor-fail, fixes F1 to F12 in place). A fresh
session picks up from "Next". Newest entries at the bottom of "Log".

Worktree `/mnt/data/projects/Softland-rig-build-reads`, branch
`rig-build-reads`, off `rig-2026-09-25` at `b31ad57e`. Every cluster run
under `flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock`.

## State

- Step: 1 of 5 (implement).

## Next

- Probe the plan's [build checks] in one small cluster run, then write
  `rig.store.reads`, `rig.store.read-exit` and the shared-file edits.

## Brief overrides of the plan (the orchestrator's, binding)

- No `src/rig/store/locks.clj`: the open step is one seam in
  `rig.store.reads` (`open-row>`), phase 2's planned signature and return
  shapes, passing values through tonight. The merge replaces its body with a
  call to phase 2's `rig.store.locks/open-row>`.
- `reads/purge-writes` is built to be called by phase 2's forget, once per
  value, for a value forget and for every value a person forget kills.
- Entries go through `client/offer-until-answered!` as it is.
- Shared files (module.clj, gate.clj, client.clj, envelope.clj) changed only
  as [F10] names.

## Log
