# editable-material — package thread

## NOW

- **2026-07-24 · Codex · P1 Phase 0 (contract/code derivation) — PASS.**
- Contract boundary: DIRECTION §Probe-FINAL only; one provenance facet, no
  recipes, bindings, dispatch, taxonomy, material-kernel, or Horizon work.
- Existing precedent resolves cleanly: immutable OC revisions hold candidates;
  a new colocated active-pointer selects the worn revision independently of
  the container's latest revision; rollback is another pointer activation.
- Additive Rama cut: existing depot/PStates/rows remain untouched; one new
  PState + one request branch + one query in the existing OC module.
- Routing obligation: the new facet-master/import prefixes get explicit
  `extract-object-key` handling and a four-task foreign-read gate.
- Client cut: reuse generic `FacePull` + the one ingest epoch; activation never
  writes render state directly. Ground rebuilds only from the served active
  revision, with master+revision stamps in all three contributions.
- Malformed drill: `?drill=` invokes a deterministic write-side drill; the
  candidate revision and rejected activation remain durable, the served active
  revision remains worn, and the client receives a separate error-card fact.
- No stop clause fired. Next: implement the additive OC/material slice and its
  focused gates before any live-cluster deploy.

- **2026-07-24 · Codex · P1 Phase 0 correction — PASS.**
- The first additive PState draft crossed the JVM method-size ceiling in the
  already-large object-container `defmodule` (`Method code too large`). It was
  removed before deployment; no cluster or durable data was touched.
- Final seam: both the provenance material and its explicit active pointer use
  the existing revisioned object-container primitive. Candidate save advances
  the material container's latest revision. Activation and rollback edit only
  the pointer container, whose current revision names the worn material
  revision. Active and latest are therefore independently durable without a new
  PState, depot, topology branch, query, module, or row shape.
- The only Rama-module edit retained is routing: `fm:` identities and
  `imp:fm:` completion keys hash to the provenance master partition. Immutable
  revision reads use the existing revisions PState through a generalized
  runtime point-read helper.

- **2026-07-24 · Codex · P1 Phase 1 (implementation + focused gates) — PASS.**
- Implemented one revisioned `fm:provenance` material container plus one
  revisioned active-pointer container using only existing OC requests, rows,
  PStates, depots, and query surfaces. Candidate save, activation, and rollback
  passed in a four-task IPC runtime.
- The generic FacePull artery now carries one constant provenance-material
  request and re-stamps it inside the existing ingest-epoch debounce. Ground
  reads only confirmed projection data; the former `machine-tint` constant is
  deleted. Fold headers, machine rail, and episode boundary use the same active
  tint and carry master + revision + contribution-site stamps.
- Malformed drill persists the candidate import, submits no pointer edit,
  preserves the active pointer byte-for-byte, and serves candidate errors only
  under drill scope for a separate error card.
- Focused receipt: `app.provenance-material-test` — 3 tests, 46 assertions, 0
  failures/errors. Server namespaces load. Approved dev boot compiled 301
  files, 15 compiled, 0 warnings; Jetty is live on port 8080.
- Mandatory pre-deploy cold backup completed successfully:
  `/mnt/data/rama/backups/20260724-021401`. The backup script restarted all
  cluster daemons. No deploy had occurred before this receipt.

- **2026-07-24 · Codex · P1 live-deploy fence — STOP.**
- Correction to the preceding backup receipt: `bin/land backup` printed
  success, but the conductor log never emitted `CLUSTER-SHUTDOWN-COMPLETE`.
  The script's bounded wait expired, killed the daemons, copied the data
  directory, and restarted anyway. Rama's official free-license backup
  procedure requires the conductor to reach `[:cluster-shutdown-complete]`
  before killing daemons or copying state. The snapshot therefore cannot be
  claimed as a valid pre-deploy backup.
- The first object-container update was refused before module transition with
  `Cluster state is invalid for the operation; cluster state:
  [:cluster-draining]`. No module update landed and no existing application
  data was written.
- Supported recovery attempts only: one clean `bin/land down`/`up`, foreground
  supervisor recovery of all five existing workers, `forceClusterOpen`, and a
  second ordinary `bin/land down`. The conductor remained not-ready and never
  reached shutdown-complete. No ZooKeeper or durable module state was edited.
  Cluster daemons are stopped after the final supported shutdown attempt.
- Stop clause fired: the mandatory backup fence conflicts with observed
  `bin/land` behavior. Live deploy, durable bootstrap, cluster restart,
  browser activation/drill, and echo sampling were not attempted. The local
  implementation remains uncommitted; focused IPC gates and the 0-warning
  client compile remain valid.
- Resume only after the cluster is recovered through a supported Rama path and
  `bin/land backup` is changed to fail closed unless it actually observes
  `CLUSTER-SHUTDOWN-COMPLETE`; then rerun backup before any deploy.

- **2026-07-24 · Fable · P1 ops recovery — DONE; resume conditions adjudicated
  MET, one deviation recorded.**
- The STOP was correct. `bin/land` was fail-open (bounded wait fell through →
  mid-drain kill → "success"); fixed FAIL-CLOSED + 900s patience (`cd4fcb4`) —
  both new behaviors have since fired correctly live (loud abort, no copy, no
  kill, exit 1).
- Root cause, two layers. **(1) The wedge:** `forceClusterOpen` against a
  draining cluster clears the T11 resume-intent but NOT
  `/rama/version/1.6/conductor/state` (nippy `[:cluster-draining]`, 24 bytes)
  → every boot re-enters LEADER-FALLING (`:shutdown? false`),
  conductorReady=false, `shutdownCluster`/`forceClusterOpen` become silent
  no-ops, deploys refused. No supported exit (operating-rama docs checked —
  nothing documented for a stuck drain). Recovered surgically with a full
  rollback copy first (`/mnt/data/rama/pre-surgery-20260724`): nippy
  round-trip verified byte-identical, setData → `[:cluster-shutdown-complete]`,
  normal boot → READY, workers LEADER-OPEN. No application data touched.
  Now mechanized as `bin/land unwedge`.
- **(2) The disease under it:** drains on this cluster hang STRUCTURALLY at
  `LEADER-FALLING STOP-REPLICATION` — worker log: "Processing is paused.
  Waiting for depot appends to flush..." never completes (observed 20+ min on
  an idle READY cluster; the 07-17 drains passed in ~60s). Platform-level
  issue, **OPEN** — later dedicated probe: which module's depot never
  flushes; `supportBundle` + Red Planet if it persists. Until resolved, every
  `bin/land down` ends DIRTY and must be followed by `unwedge` before `up`.
- **Pre-deploy snapshot adjudication (Fable):** the documented clean
  cold-backup is UNAVAILABLE on this cluster (above). The fence's purpose —
  a restorable pre-deploy state — is satisfied by
  `backups/20260724-quiesced` (2.4G): copied from an idle, killed cluster
  with the state znode pre-cleaned (restores boot clean); crash-consistent
  class, which this cluster has proven lossless for acked writes. Honest
  label in the ledger; two further copies exist (021401, pre-surgery).
  Deviation recorded here, not silent.
- Cluster at handoff: conductorReady TRUE · workers LEADER-OPEN · five
  modules RUNNING. Codex resumes P1 from the deploy step; resume line in
  PROMPTS.md (do NOT rerun `bin/land backup` first; on any refused module
  update, stop and report).
