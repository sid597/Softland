# durable-ground — thread file

## STANDING (frozen at open, 2026-07-17)

- Binding docs: `CONTRACT.md` (this package) + `decisions.md`. This file is a
  baton, not a source of truth; if it contradicts CONTRACT.md or decisions.md,
  those win — flag the discrepancy here, do not pause.
- Process: work-package skill (2026-07-13 amendment level) + /rama skill ops
  references. One phase per fresh CONTEXT; Fable orchestrates and implements
  directly (Sid 2026-07-17: "get into work mode... first light asap").
- Scope guard: NEW files only in `app.server.rama.cluster` ns + `bin/land` +
  `ops/`; edits to existing src limited to the boot-flag seam in
  `file_viewer.cljc` (T6) and WAL-path config removal at P4. No new Rama
  organs. IPC constructors untouched.
- Verification duties: every platform claim checks against
  `.claude/skills/rama/references/operate.md` / `docs/reference/rama/*` on
  disk before code; memory-derived claims are marked and verified.
- Definition of done: CONTRACT §7 G1–G6 green + gate review + retro; the dev
  boot defaults to the durable cluster; acked writes survive kill-9 and
  machine reboot with named-process receipts; ingest explicit; WAL writes
  off; backup recipe exercised.
- Stop clauses: CONTRACT §8. Hard rules: never read `env.clj`; code and docs
  in separate commits; docs only on the local docs branch (never push);
  no Co-Authored-By lines, ever.
- Must NOT start without Sid: machine-reboot drill (his act) · any
  system-package install · any spend.

## NOW (append per session; ~15 lines each)

- **2026-07-17 · Fable · P0 (inventory + environment) — DONE.**
  Facts: Java 21 ✓ (Rama supports 8/11/17/21) · 62GB RAM / 24 cores / 286GB
  free on /mnt/data · no existing rama install. Module inventory → CONTRACT
  §4 (five modules; dogfood excluded). Finding worth flagging: the product
  boot runs TWO IPC clusters with two disjoint object-container instances
  (trail_view.clj:779-788 vs object_container/runtime.clj:13-18) → CONTRACT
  §3 unification ruling (one cluster, one OC deployment, both handle bundles
  foreign). License question answered before open (free ≤2 supervisor
  nodes; RPL 2025-03-18). Backups honesty pinned: free tier = COLD backup
  procedure (22-backups.md:124-134), scripted at P6; crash/reboot recovery
  is native and continuous regardless. One TBD for P2: where file_viewer
  wires face-arsenal handles (attach point named in §4 table).
  Next: P1 — download release 1.6.x, `ops/rama.yaml`, daemons up, G1.

- **2026-07-17 · Fable · P1 + P2 — DONE, G1 + G2 PASS.**
  P1: release 1.6.0 (exact client match, T3) unpacked to /mnt/data/rama
  (zip unpacks FLAT — bin/land adjusted); `ops/rama.yaml` + `bin/land`
  (up/down/status/deploy/backup) + `ops/worker-overrides.yaml` (T2, worker
  -Xmx3072m). Daemons detached via setsid; identified by main class
  `rpl.rama.distributed.*` (pkill self-match trap hit once in-session —
  bracket regex; bin/land patterns fixed). **G1 receipt:** embedded free
  license `:active` (2 nodes, →2117) · UI :8888 HTTP 200 · kill -9 all
  daemons → 0 procs → restart clean (new PIDs 77743/77816/78011, ~2.5GB
  RSS daemon set). P2: `module-jar` task in src-build/build.clj — dep tree
  resolved WITHOUT com.rpl/rama + exclusion on rama-helpers (T4); 47MB;
  only com/rpl/rama/helpers/* inside ✓; `env.clj` excluded from the copy
  (secrets never ride artifacts) ✓. All five §4 modules deployed and
  RUNNING (module NAME = the var string — T7 simplified). Judgment call
  flagged: batch deploy failed on a launch race (deploy #2 while #1
  settling) — per-module settle-wait added to bin/land; same class as the
  G14 IPC collision note. **G2 receipt:** foreign append :ack → durable
  `:rejected/:target/not-found` decision → read back through the EXISTING
  `ocr/read-decision` over a cluster foreign-pstate handle (the P3
  shape-compatibility claim holds live). Cold first-append ack 481ms
  (connection+JIT — steady-state is P5's measurement, not this number).
  Code artifacts (bin/land, ops/, build.clj edit) UNCOMMITTED — code
  commits are Sid's call. Next: P3 — `app.server.rama.cluster` ns +
  boot-flag seam in `file_viewer.cljc` (T5, T6), then G3 full-app boot
  against the cluster.

- **2026-07-17 · Fable · P3 + P4 — DONE. G3 PASS (one item open), G4 PASS.**
  P3: `app.server.rama.cluster` (manager + shape-identical bundles,
  total-with-RETRY memos — a delay would cache a cluster-down nil, T6; no
  com.rpl.rama.test require = T5 by construction) + seam: `trail-rt`/`face-rt`
  branch on LAND_CLUSTER=1; jetty /assert routed via `trail-runtime-ref`
  (IDeref+IPending — `realized?` on a plain reify throws) + nil log-path in
  cluster mode; `record-wear!` explicit-nil = WAL off (fallback default would
  have silently defeated P4's WAL-off; flagged as sanctioned WAL-removal work).
  **G3:** app boots on cluster, WebGPU up, 0 client errors, ZERO ingest lines
  (T9 clean); reader face worn over migrated conversation; REAL keystroke
  round-trip: narrow truth-echo p50 30ms/p95 52ms/0 stalls/0 unechoed (n=10,
  cold — S3 verdict belongs to P5's 720-sample run); drill chars RESTORED by
  Backspace through the same organ, tail verified via cluster read. Open G3
  item: trail-face render check (drive `/trail` in browser) — next session.
  **P4/G4 receipt:** sweep 308/308 · spine 685 commits/712 edges · wear bridge
  57/0 · faces 4/4 · distill river 247 debris 399, object-key == the
  PRE-COMPUTED deterministic address (boot never re-distills) · block-edit
  bridge 10/0 (the G8 numbers) · machine-cut 34/0 · relation-assert WAL absent
  on disk → honest no-op (the "4th log" never existed) · same-unit truth +
  stale `{:rejected :edit/stale}` durable + §3 id-families disjoint
  (oc:doc: vs chat:/du:) + 4403 activity entries. Judgment calls: foreign
  client threads are NON-DAEMON → `-X` entries System/exit (observed hang);
  spine-run-id now STABLE "durable-ground-cluster" (delete cursor if cluster
  ever rebuilt empty); machine-cut live-annotate WAL left ON (doubles as its
  stale-reconcile — residue); util-fns text-kernel IPC delay is action-driven,
  outside §4 scope (residue); default-conversation literal duplicated in
  cluster ns until the IPC branch retires. bin/land grew ingest|migrate
  (clj -X one-shots). Code UNCOMMITTED. Next: P5 drills (kill-9 mid-typing,
  daemon restart, echo re-measure vs S3, footprint; REBOOT = SID) → P6
  backup/restore → close (incl. LAND_CLUSTER default flip decision).

- **2026-07-17 · Fable · G3-leftover + P5 + P6 + CLOSE — DONE. Gate PASS
  (G5c reboot = Sid's open slot).** G3: `/trail timeline` typed through the
  real panel → migrated corpus rendered from cluster PStates, 0 errors.
  P5a: OC worker 84500 kill-9 at t+10s mid-typing → replacement 213986 at
  t+11s; cluster read = pre-text + exactly the 50 pre-kill chars (acked=
  durable, zero loss); unit restored via the organ. FINDING: in-flight
  foreign failure kills the live Electric session (reload heals; 30s pong
  timeout) — availability residue → first-light. P5b/P6 minted **T11**:
  interrupted shutdownCluster RESUMES on next boot killing all workers; a
  COMPLETED shutdown self-exits every daemon; bin/land down/backup now wait
  on the terminal marker (set-e-safe if-forms). P5d: cluster echo c12 n=720
  p95 7.86 / p99 9.98 ms, 0 stalls — S3 PASS ~6x headroom (+0.2ms vs IPC).
  P5e: cluster ~8.1GB RSS total. P6/G6: cold backup 20260717-200611 (1.6G);
  scratch-dir restore booted + served identical reads. CLOSE: default-on
  flip (LAND_CLUSTER=0 opts out) verified by bare-env boot; suites 19t/314a
  + 21t/333a green; T4/T6 re-checked (T6 driven BOTH halves — down=honest
  empty, up=same-JVM heal). GATE.md + RETRO.md written. Probe-rig facts
  (recipe — /tmp scripts are disposable): headed Chrome DISPLAY=:0 + CDP
  :9222 + `--enable-unsafe-webgpu --enable-features=Vulkan`; the cmd panel
  BOOTS OPEN (Ctrl+K would close it — type directly); keystrokes ride
  window KeyboardEvents (m/relieve last-wins — pace ≥50ms/char); cljs
  interop via `window.cljs.core` + `__softland_atoms`. Leftovers for Sid:
  reboot drill card (close handoff) · `rm -rf /mnt/data/rama/
  data-restore-scratch` · vault rsync · code commit (then re-run git-spine
  suites).
