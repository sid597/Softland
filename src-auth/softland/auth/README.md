# src-auth/softland/auth

Parent: [softland](../README.md).

Owns both sides of `Auth`: the Rama module (`Auth`) that decides writes and serves reads inside the cluster, and the foreign client (`softland.auth.client`) that runtime copies call.

## Boundaries

- **Contract edge (`client.clj` + `fit.clj`):** Callers outside this tree depend only on `softland.auth.client` (its 18 public functions) and, when launching a cluster, on `softland.auth.module/Auth`. No public function throws; malformed inputs fail in `fit.clj` with `{:could-not :read-the-event}` before touching Rama. A record that reaches a depot from any other appender is held against the client's form in the topology (`schema.clj`) and refused the same way, so it never holds its partition.
- **No clock inside Auth:** Every timestamp comes from the caller's `:at` (or stored `:until` / `:ends`); neither `module.clj` nor `schema.clj` reads wall-clock time.
- **No external systems or caches:** Zero `TaskGlobal`s inside `Auth`, zero global depots or global PStates, and zero cross-module dependencies.

## State (kept apart)

- **Durable cluster state (owned by `Auth` in `module.clj`):** Four partitioned depots (`*root-events`, `*asks`, `*invites`, `*enters`) and six partitioned PStates (`$$root-data`, `$$live` launched with `pstate.proxy.limit = 20000`, `$$accounts`, `$$claims`, `$$asks`, `$$enter-ops`), all owned by the single stream topology `"auth"`. `$$live` is nested by root: under each root, a subindexed map of what its watchers are pushed (sign-ins and sessions that nobody ended and whose time had not passed at the root's latest write, though not every such one, and a count of the ends written under the root), so that a watch of one root is a proxy on that root's map alone. That someone ended a thing is written on its record in `$$root-data`; that its time has passed is written nowhere.
- **Borrowed cluster handles (held by each `Client` in `client.clj`):** Foreign handles to the four depots, `$$asks`, `$$live`, and the three query topologies (`"who-is"`, `"under"`, `"ping-task"`). The caller of `open` owns the underlying Rama cluster handle; `close!` does not close the cluster.
- **Process-local memory (owned by each `Client` in `client.clj`):** A closed flag, one shared `foreign-proxy` subscription on the root's own map in `$$live` and an incrementally projected live map per locally watched root, per-watcher turn locks, and a daemon scheduler running `"ping-task"` while watches are active. Holds no Auth truth and is discarded on `close!` or process exit.

## How the files relate

1. `fit.clj` — Pure form validation and fixed-width integer coercion (`Long`) for every write, read, and `watch-root` argument. Depends on nothing else in this folder.
2. `schema.clj` — Pure typed record definitions, 7-bit ASCII key encoders, read projections, and pure single-task transition/planning functions for every write (`invite`, `enter`, `own-lane-made`, root writes, the thinning of what is watched, 2-scan `end-actor`, `ask`, `answer-ask`). Depends on nothing else in this folder.
3. `module.clj` — Declares `defmodule Auth`, wires the four depots and six PStates in stream topology `"auth"` (each depot's dataflow in a function of its own, which first refuses a record of another form), invokes `schema.clj`'s pure step/plan functions across partitioner hops (`|hash`) and flat `<<atomic` + `ops/explode` writes, and declares `"who-is"`, `"under"`, and `"ping-task"`. Depends on `schema.clj`.
4. `client.clj` — Exposes the 18 contract calls. Validates arguments with `fit.clj`, encodes events and projects `ask-of` / `$$live` diffs with `schema.clj`, and communicates with a launched `Auth` module solely through Rama's foreign client APIs. Depends on `fit.clj` and `schema.clj` (never on `module.clj`).
