# durable-ground — CONTRACT

2026-07-17 · Fable · pre-first-light durability slice per the 2026-07-15 ruling
(`build/first-light/DEPLOY.md`, reaffirmed verbatim 2026-07-17, LOG entry).
Written under the /rama skill (ops references: `operate.md`,
`docs/reference/rama/22-backups.md`, `32-downloads-maven-local-dev.md`).
Binding with `decisions.md`; this contract governs over any thread file.

## 1. Purpose

Native-born material (first-light conversations, wishes, face revisions,
block edits) must survive process death and machine reboot with zero loss of
acked writes. Mechanism: the land's product runtimes ride a REAL single-node
Rama cluster on Sid's PC instead of the in-memory test cluster
(`create-ipc`). Depots and PStates become the durable log natively; no
hand-rolled journals (the recorded dead branch — DEPLOY.md
correction-of-record).

Consumers, in order: **first-light A** (the genesis package — its restart
gate is unpassable on IPC) · every existing runtime (trail view, faces,
block-write) · the dogfood Space/LLM kernels when first-light wires them.

**Non-goals (each an extension point, not a void):** no new Rama organs
(zero new depots/PStates/topologies) · no dogfood-module deploy (first-light
A's work — but the cluster makes it a one-line deploy later) · no multi-node,
no isolation scheduler, no monitoring module (single personal node; revisit
at the server trigger) · no first-class incremental backups (paid feature —
see §5 Backups) · no phone/remote access (deferred per DEPLOY.md stances).

## 2. Placement ruling

New code lives in ONE namespace: `app.server.rama.cluster` — the cluster
connection (manager construction from a plain config map: localhost
conductor, no secrets) plus cluster-backed variants of the runtime
constructors' handle maps. The existing `start-*-runtime!` IPC constructors
stay untouched for tests. Ops tooling lives OUTSIDE src: `bin/land` (shell:
up / down / status / deploy / backup / restore / ingest) + `ops/rama.yaml`.
Why not editing each runtime ns in place: the IPC constructors are the test
harness for 40+ suites; the seam must be additive. Reversal cost: delete one
ns + one script; the IPC path never stopped working.

## 3. The unification ruling (discovered at P0)

Today's boot runs TWO in-memory clusters: the trail cluster
(object-container + transcript-ops + relation-kernel + trail-view;
`trail_view.clj:779-788`) and the face cluster (a SECOND object-container +
transcript-ops instance; `object_container/runtime.clj:13-18`), i.e. two
disjoint object-container truths that already collided once in the shared
simulated worker registry (board, G14 note). A real cluster deploys a module
name ONCE. **Ruling: one cluster, one object-container deployment; both the
trail-view handle bundle and the face handle bundle open foreign handles to
the SAME modules.** This heals an accidental split — it is the "ONE edge
truth, never a second store" ruling applied to the object store. Reversal
cost if a real conflict appears (e.g. import keyspaces collide): deploy the
face OC under an explicit second module name via `get-module-name` override —
one line, no data migration (both stores rebuild from durable sources).
Object-keys are content-addressed and distinct across the two current
populations (doc imports vs chat objects); P4's receipt asserts no key
collision during migration.

## 4. Module set (deploy at P2)

| Module var | Why |
|---|---|
| `app.server.rama.object-container/object-container-module` | all object/block/source truth |
| `app.server.rama.object-container/object-container-transcript-ops-module` | transcript operational control |
| `app.server.rama.relation-kernel/relation-kernel-module` | typed edges |
| `app.server.rama.trail-view/trail-view-module` | trail projections |
| `app.server.rama.face-arsenal/face-arsenal-module` | face roster (attach point verified at P2 — P0 left one TBD: where file_viewer wires arsenal handles) |

Launch parallelism: `--tasks 8 --threads 4 --workers 1` (tasks power of 2;
tasks ≥ threads ≥ workers; single worker JVM on one node). Worker heap via
`--configOverrides`: `worker.child.opts: "-Xmx3072m"` (default 4096m; box has
62GB — headroom is fine, but pin it so footprint is a measured number, not a
default). Dogfood modules (space/llm/compute/transcript/transcript-ingest)
are NOT deployed by this package.

## 5. Backups (honest durability levels — the block-write RETRO lesson applied)

- **Crash/reboot recovery — native, continuous:** PStates persist to RocksDB
  at write time; topologies resume from persisted offsets; worker restart
  does NOT need depot replay (/rama skill, verified doctrine). This is the
  layer that makes kill-9 and machine reboot non-events.
- **Disaster recovery (disk death, fat-fingered destroy) — COLD backups:**
  first-class incremental backup requires a paid license
  (`22-backups.md:124`). Free-tier procedure, scripted as `bin/land backup`:
  `rama shutdownCluster` → wait for `[:cluster-shutdown-complete]` → stop
  daemons → snapshot ZK data + copy `local.dir` → restart daemons → rsync
  the snapshot to the Mac vault. RPO = since-last-cold-backup, for
  disk-death only. Cadence: manual/nightly, Sid's habit call; snapshot size
  at current scale is MBs.
- Vault copies inherit never-pushed privacy; encrypt if they ever leave the
  two machines (standing DEPLOY.md rule).

## 6. Traps ledger (cite by number in code/scripts)

- **T1 — devZookeeper persistence is unverified.** `rama devZookeeper` is
  documented "not for production." If its metadata does not survive reboot,
  modules would need redeploy after every boot — silently breaking the
  restart gate. P1's drill includes daemon kill + restart AND the G5 reboot
  drill includes ZK state; if metadata is lost, switch to a real Zookeeper
  (single-node) and record the swap here.
- **T2 — worker heap defaults to 4GB.** Pin via configOverrides (§4);
  footprint is measured at G5, never assumed.
- **T3 — client/cluster versions must match (major.minor).** deps.edn pins
  `com.rpl/rama 1.6.0`; the downloaded release must be 1.6.x; upgrades move
  both together (atomic-upgrade procedure in operate.md).
- **T4 — the uberjar must EXCLUDE rama** (provided-scope equivalent for
  deps.edn: build the jar from a basis that omits the rama dep) — else 100x
  bloat and version-clash risk. Gate: jar size sanity (< 150MB) + no
  `com/rpl/rama` classes inside.
- **T5 — the app must NEVER `launch-module!` against the real cluster.**
  Modules deploy via CLI; the app only opens foreign handles. The cluster
  constructors must not share the IPC constructors' launch lines.
- **T6 — delay-boot totality (standing gate class):** the cluster-connection
  delays feeding the render path yield poisoned-but-TOTAL values on failure,
  never cached throws — a cluster-down boot renders an honest empty land,
  not a permanent exception.
- **T7 — Clojure module names:** `--module` takes the namespace-qualified
  VAR; CLI status/destroy take the module NAME (`get-module-name`). The two
  spellings live in `bin/land` once, nowhere else.
- **T8 — bridge replay ordering (block-write RETRO lesson):** EDN-WAL bridge
  replay is meaningful only after each target's imported base exists. P4
  order: explicit ingest first, then bridge replay, then WAL paths OFF.
- **T9 — boot-ingest off startup ≠ watchers dead.** The 07-15 ruling removes
  sweep/watchers/git-spine from the STARTUP path; `bin/land ingest` (or a
  REPL call) runs them explicitly. Live watching resumes whenever that
  command is run; first-light may later make it a wish.
- **T10 — echo probes are UNCOMMITTED tree files** (`write_echo_probe.clj`,
  `stream_echo_probe.clj`, board note). G5 re-uses them against the cluster;
  they stay uncommitted.

## 7. Phases and gates (Fable implements directly — Sid's ASAP; validation
layers stay fresh-context where they exist)

- **P0 — inventory + environment (DONE 2026-07-17, recorded in NOW):**
  Java 21 ✓ · 62GB/24c/286GB free ✓ · no existing install · module inventory
  (§4) · two-cluster wrinkle found (§3) · license verified free ≤2 nodes.
- **P1 — cluster up.** Download release 1.6.x matching deps.edn; unpack to
  `/mnt/data/rama/`; `ops/rama.yaml` (localhost, `local.dir:
  /mnt/data/rama/data`); start devZookeeper + Conductor + Supervisor
  (detached; systemd user units recorded as a later nicety); `bin/land
  up|down|status`. **G1:** `licenseInfo` prints the free license; Cluster UI
  answers on :8888; kill all daemons + restart → cluster returns with no
  errors (empty-state recovery).
- **P2 — uberjar + deploy.** `build.clj` uberjar task (T4); deploy the §4
  module set (T5, T7). **G2:** `moduleStatus` [:running] for all five; REPL
  foreign smoke over the cluster manager: one depot append with :ack + one
  PState read, against object-container.
- **P3 — the runtime seam.** `app.server.rama.cluster` ns: manager +
  cluster-backed handle bundles shaped IDENTICALLY to the IPC runtime maps
  (the `foreign-*` API is the same); boot flag (`LAND_CLUSTER=1` env or
  config) selects cluster vs IPC in `file_viewer.cljc`'s delays (T6).
  **G3:** full app boots against the cluster; trail face renders; a block
  edit round-trips (pending-input → ack → truth echo) end-to-end in the
  browser.
- **P4 — migration day.** Run `bin/land ingest` (sweep + watchers +
  git-spine, now explicit — T9); bridge-replay the four EDN WALs once
  (block-edit via existing `replay-block-edit-log!`; face-wear / machine-cut
  / relation-assert via their existing replay fns) — T8 order; then remove
  WAL paths from the cluster runtime config (files kept on disk as history).
  **G4:** replay receipt with exact counts per log (replayed/failed), a
  no-key-collision assertion for §3, and reads showing the material (the
  G8 conversation's block text present via cluster read).
- **P5 — drills + measures.** (a) kill -9 the worker JVM mid-typing →
  recovery receipt naming old/new PIDs + same-unit text (restart claims name
  the replaced process — block-write RETRO rule); (b) full daemon-set
  restart; (c) **machine reboot drill — SID'S MOVE** (he reboots; the
  receipt is the land showing the same conversation + revisions + trail
  after login); (d) echo re-measure with the T10 probes against the cluster:
  **budget = the standing S3 ruling, p95 ≤ 50ms AND p99 ≤ 100ms** (stream
  basis 7.66ms on IPC; localhost hop expected small — measured, not
  assumed); (e) footprint: RSS per daemon + worker under normal use.
  **G5 = all five receipts.** Environment attestation opens the perf
  receipt (host, localhost transport, cluster version) per the standing
  gate rule.
- **P6 — backup + restore.** `bin/land backup` per §5; then a restore drill
  against a SCRATCH `local.dir` copy proving the recipe (never the live
  dir). **G6:** restored scratch cluster serves the same reads.
- **Close:** retro per the work-package skill; board flip; the decisions.md
  ruling already stands (no new law needed unless a stop clause fires).

## 8. Stop clauses

- Echo re-measure blows the S3 budget on the real cluster → STOP; escalate
  with numbers (recorded fallback: stay on IPC + the four EXISTING WALs
  only — no new shims — and re-derive; D-014 pre-registered this
  re-measure).
- T1 fires (devZookeeper loses metadata across reboot) AND a real single-node
  Zookeeper cannot be run from a plain tarball without system-level installs
  Sid must approve → STOP, escalate the install choice.
- Java/version incompatibility at deploy (T3) → STOP with options.
- Any genuine two-readings conflict between this contract and decisions.md →
  STOP per the work-package skill; no silent picks.

## 9. Input manifest

`build/first-light/DEPLOY.md` (the ruling + correction-of-record) ·
`decisions.md` §Durable ground + §Only-Sid · board Active-blocks entry ·
`.claude/skills/rama/references/operate.md` · `docs/reference/rama/22-backups.md`
+ `32-downloads-maven-local-dev.md` · `src/app/file_viewer.cljc:155-270` (the
two delays + boot ingest) · `src/app/server/rama/trail_view.clj:779-798` +
`object_container/runtime.clj:11-120` (IPC constructors + WAL seam) ·
`data/*.ednl` (the four bridge logs) · deps.edn:16 (rama 1.6.0) ·
`src/app/probe/{write,stream}_echo_probe.clj` (T10).

## 10. Handoff

Implementer: Fable directly (this session onward), fresh-context subagents
for any validation layer that gets one. After G6 green: gate review per the
work-package skill (re-run suites incl. every pinned-enumeration scan over
touched files), then retro + board prune. What must NOT start without Sid:
the machine-reboot drill (his act), any system-package install (stop clause
2), spending money (nothing here should).
