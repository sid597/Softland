# durable-ground — GATE review

2026-07-17 · Fable (orchestrating session, per the 2026-07-05 amendment — no
re-entry cost) · verdict at bottom. Inputs: the code diff read in full
(cluster.clj 407 lines + the four seam edits + bin/land + ops/), the phase
receipts in NOW.md as prior-pass records (not authority), suites re-run THIS
session, and live drills driven this session.

## 1. Suites re-run this session (post default-flip)

Pinned-scan sweep first (the machine-cut close rule): grepped the test tree
for every touched file's path. Eight test namespaces reference touched files
(`relation_assert_route_test` `face_arsenal_test` `face_gate_fixes_test`
`machine_cut_test` `git_spine_test` `git_spine_gate_test` `parser_test`
`review_pack_test`); nothing pins the NEW files (cluster.clj, bin/land,
build.clj module-jar) — expected, they are new surfaces.

- Batch 1 (assert-route + arsenal + face-gate-fixes): **19t / 314a, 0 fail 0 err.**
- Batch 2 (machine-cut + git-spine ×2 + parser + review-pack): **21t / 333a, 0 fail 0 err.**
- Flip-hazard check: `cluster-boot?` has ZERO test-tree callers; the assert
  route suite drives the inner handler with its own {:runtime :log-path} —
  flip-immune by construction.

## 2. Trap spot-checks (byte-level where relevant)

- **T4** (jar): 47MB (<150) · `com/rpl/rama/**` = helpers classes only ·
  the three `env.clj`-pattern hits are third-party library files
  (`cljs/env.cljc`, `process/env.cljs`, `tools.analyzer/env.clj`) — the
  app's `app/server/env.clj` is NOT in the jar.
- **T5**: cluster.clj has no `com.rpl.rama.test` require — cannot launch
  modules, by construction.
- **T6**: DRIVEN both halves this session (not just read): cluster down +
  fresh page → workspace mounts, canvas renders, 0 errors (honest empty
  land); cluster back up → the SAME server JVM serves trail feed + scene
  again with no restart (the memo-total retry heals; a `delay` would have
  poisoned).
- **T7**: both spellings live only in bin/land. Correction learned live:
  `rama destroy` takes the module name POSITIONALLY + stdin confirmation.
- **T9**: both dev-server boots this session logged ZERO ingest/sweep lines.
- **T11 (new, minted at P5b/P6)**: an interrupted `shutdownCluster` persists
  its intent in ZK; the next conductor boot RESUMES it and kills every
  worker into terminal CLUSTER-SHUTDOWN-COMPLETE ("Awaiting exit"), and a
  COMPLETED shutdown makes every daemon EXIT ITSELF (so the CLI's own
  completion-poll dies with a thrift connect error). bin/land `down` and
  `backup` now wait on the terminal log marker (or all-daemons-gone) before
  pkill, with `set -e`-safe if-forms. Worn: the fixed paths ran the P6
  backup, the T6 down/up, and the final up.

## 3. Gate receipts (all driven live this session; numbers verbatim in NOW)

- **G3 leftover**: `/trail timeline` typed through the REAL command panel in
  headed Chrome → trail face renders the migrated corpus (spine commits
  claimed at commit dates, arrived 07-17) from cluster PStates; server env
  receipt `LAND_CLUSTER=1`; 0 errors.
- **G5(a) kill -9 mid-typing**: worker 84500 (object-container) killed at
  t+10.0s of a 40s/5-keys-s run; supervisor replacement pid 213986 at
  t+11.0s (~1s). Cluster read after drain: unit = pre-text + EXACTLY the 50
  pre-kill keystrokes — zero acked-write loss, zero corruption. Unit
  restored through the real organ afterward (4955 chars, original tail,
  cluster-read verified).
- **G5(b) daemon-set restart**: full down/up with data; all five modules
  RUNNING; same-unit read identical. (T11 was discovered here.)
- **G5(c) machine reboot — OPEN, Sid's move.** The one outstanding receipt.
- **G5(d) echo re-measure**: same T10 probe, module CLI-deployed to the
  cluster (destroyed after). c12 n=720: echo p50 5.85 / p95 7.86 / p99 9.98
  / max 12.53 ms, 0 stalls, 720/720 materialized. S3 budget (p95≤50,
  p99≤100): **PASS with ~6x headroom**; localhost hop over the 7.66ms IPC
  basis ≈ 0.2ms at p95. `:ack` cross-check p95 4.30ms agrees. Environment
  attestation opened the receipt.
- **G5(e) footprint**: daemons 859/881/798 MB, workers 1573/1004/1243/941/996
  MB — cluster total ~8.1GB on the 62GB box; dev-server JVM 1.9GB beside it.
- **G6 backup + restore**: cold snapshot 1.6GB (quiesced; live dir 3.0GB —
  the contract's "MBs" guess corrected), ledger entry `20260717-200611`;
  scratch restore drill booted a cluster FROM the copy (worker tmpdir
  receipt), five modules RUNNING, same-unit read identical → recipe proven.
  Live cluster returned and re-verified after.
- **Default-on flip**: `cluster-boot?` now true unless LAND_CLUSTER=0; a
  bare `clj -M:dev:test -m dev` boot (no env var) drove `/trail` to a
  1080-node cluster-fed scene, 0 errors, 0 ingest lines.

## 4. Falsification pass (attempted breaks, what held)

- Nil-runtime totality under the flip: every `trail-rt`/`face-rt` caller
  rides the honest-degrade vocabulary (G20/G21/MC-T12 classes); driven live
  by the T6 drill. HELD.
- `record-wear!` contains-vs-nil: absent key → legacy default path (IPC
  unchanged); explicit nil → WAL off (cluster). Arsenal suite green. HELD.
- `/assert` in cluster mode writes no WAL; IPC mode (LAND_CLUSTER=0) still
  does; `trail-runtime-ref` reify satisfies the route's `realized?` probe in
  both modes. Suite green. HELD.
- Worker-replacement healing of cached foreign handles: receipted live at
  P5(a) — the same dev-server JVM served the restore drill through the
  REPLACED worker after the foreign client's ~30s pong timeout. HELD.
- default-address race: two threads double-compute the same deterministic
  sha — benign. HELD.

## 5. Open doubts (non-blocking; cheap falsifier named per the skill)

1. **Electric session dies on in-flight foreign failure** (P5a finding): an
   in-flight foreign call against a dying worker throws inside the websocket
   handler and takes the live session; the client needs one reload; the
   foreign channel takes a 30s pong timeout before reconnecting. Durability
   is unaffected (acked=durable held). Falsifier when it matters:
   catch/absorb `CallbackException` in the serve paths + re-run the P5a
   drill expecting live-session survival. Routed to first-light as an
   availability residue, not a durability defect.
2. **`clj -X` error-path exit with non-daemon foreign threads**: success
   paths System/exit (observed hang fixed at P3); whether an EXCEPTION exits
   or hangs is unverified. Falsifier: `bin/land ingest` with the cluster
   down — expect exit-1-not-hang.
3. **`bin/land up` foreground-pipe hang**: observed once under a piped
   foreground invocation (daemons started fine; the invoking pipe never
   closed). Workaround in practice: run backgrounded. Falsifier: reproduce
   with `bin/land up | tail`, chase the held fd.
4. **machine-cut live-annotate WAL still ON** (sanctioned P4 residue — it
   doubles as that organ's stale-reconcile until its own slice).
5. **module-jar dep-tree honesty is build-time-manual**: a future dep that
   transitively reintroduces com.rpl/rama would only be caught by re-running
   the T4 grep. Falsifier: the T4 check itself, re-run at any jar rebuild.

## 6. Verdict

**PASS — package close authorized, with G5(c) machine-reboot held OPEN as
Sid's receipt slot** (his act by contract §7/§10; the drill card is in the
close handoff). Everything machine-verifiable is green: G1–G4 (prior
receipts), G5 a/b/d/e, G6, the default flip, suites, traps. D-006 notes: the
six-layer model ran with Fable orchestrating + live drills replacing a
separate wearing (the cluster IS the land's substrate — daily use starts
immediately); no stop clause fired; two genuine platform traps (T11 class)
were found by DRILLS, not by any static layer — the drills earned their keep.
