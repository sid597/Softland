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

- **2026-07-24 · Codex · P1 Phase 2 (deploy + Probe-FINAL live drills) —
  PASS; Phase 3 full-suite gate — STOP.**
- Honored the adjudicated backup deviation: did not rerun `bin/land backup`.
  Issued exactly one update for the existing object-container module; it was
  accepted and returned to RUNNING. No module, depot, PState, topology branch,
  query, or durable row shape was added.
- Explicit `provenance-material-ingest!` installed the default material
  revision
  `rev:fm:provenance:3cd873dd99ebc288c8ca8f6fef031e36ff9a5b163610df65074ed25896f4db09:dc85777d27451037b12a90ee2905dc6030c2c51f52fba4f7e5616d52b91bc120`
  and pointer revision
  `rev:fm:provenance:aa8fadced2e8e1dee4aca996b2ed0db571cf58a468d2051699354c29e0193638:f68ece31ff6c393770677961a3e80eb28d6b86cfdfda7d270bb733cc30131a98`.
  The application startup path is read-only; the deploy-time ingest remains
  the sole initializer.
- Restart drill: `bin/land down` reached its clean terminal marker after about
  900s; the sanctioned `bin/land unwedge` was still run and was idempotent
  (`[:cluster-shutdown-complete]` before and after), followed by
  `bin/land up`. All five modules returned RUNNING. A read-only material query
  returned the same active, latest, and pointer revision IDs.
- Live browser activation imported and activated revision
  `rev:fm:provenance:3cd873dd99ebc288c8ca8f6fef031e36ff9a5b163610df65074ed25896f4db09:4c950d3a53aa358a2148e96a972c49db1bbca410c0297db4385a7ad2a4e7b3f5`
  with tint `[0.18 0.84 0.32 0.95]`. Confirmed scene-store contributions were
  68 fold headers, 34 machine rails, and 1 episode boundary; all 103 carried
  master `fm:provenance`, the same activated revision, and their contribution
  site. A page born after activation wore that revision.
- The `?drill=p1-final-1784884925150` malformed candidate was retained as
  latest revision
  `rev:fm:provenance:3cd873dd99ebc288c8ca8f6fef031e36ff9a5b163610df65074ed25896f4db09:26c3dfd7ac4ccf9ec44d4af9ed2a18e036bc3ac8dfb57d93c1b77b1ab5db5043`.
  Activation closed on `EOF while reading`; the separate error card named the
  candidate and error while all 103 worn contributions remained on the green
  active revision. The rejected candidate trace remains durable.
- Same-page rollback repointed active to the original default revision while
  latest remained the malformed candidate. Without reload, all 68/34/1
  contributions changed together to the default revision and tint; counts and
  layout remained stable. This proves active is independent of latest and
  rollback is a pointer edit.
- Byte proof: untouched `HEAD` (`a334a79`) constant-backed render, the new
  master-backed default render, and the same-page rollback render produced
  identical 1440x1000 PNG bytes: SHA-256
  `f9a5f3eeed43587d0f7944fdcbc4e27ba83a0a76860dcb03ae1f048477356946`.
  ImageMagick reported 0 changed pixels.
- Echo machinery was not edited. Isolated browser samples were
  `[26.3, 41.6, 55.0]` ms; `__ground.report()` returned
  `n=3 p50=41.6 p95=55.0 p99=55.0 max=55.0` against the unchanged 52ms bar,
  with the one 55.0ms idle-run outlier retained honestly.
- Focused gate is green: `app.provenance-material-test` plus
  `app.face-projection-test` ran 12 tests / 132 assertions, 0 failures, 0
  errors. The current client compiled 301 files with 0 CLJS warnings.
- Full repository suite was attempted by loading and running every one of the
  44 `*_test.clj` namespaces: 383 tests / 5,183 assertions, 8 failures, 0
  errors. Six order-sensitive failures came from
  `stale-approval-on-executor-death-test`; that namespace passes alone (12
  tests / 114 assertions). Two deterministic failures remain in
  `kernel_shape_test`: it requires exactly five kernel examples, while
  committed `src/app/server/rama/kernel.clj` includes the pre-existing
  `:face-arsenal` sixth example. Both failing test/implementation surfaces are
  unchanged by P1; the mismatch was introduced before this package by
  `1725f55`.
- Standing stop fired: the hard `tests green` fence conflicts with observed
  committed code. Repairing the unrelated kernel-shape contract test would
  expand this one-package change beyond P1. Per the stop clause, no code was
  committed and nothing was pushed. P1 code and this private phase note remain
  local; cluster is up, active is the default revision, and latest is the
  retained malformed drill trace.

- **2026-07-24 · Fable · P1 GATE — PASS. Code committed `83aa5dd`; P2 open.**
- Full record: `GATE_P1.md`. Every receipt independently re-run: full suite
  383t/5,183a with the ONLY red being kernel-shape (pre-P1); the six
  stale-approval failures did NOT reproduce in-suite (flake class clinched —
  two standalone greens + one in-suite green; registry amended in
  memory/implementation-quirks.md). CLJS 0 warnings. Falsification pass held
  on all load-bearing claims (routing byte-verified, totality chain closed,
  no optimistic render, startup read-only).
- Adjudications: (a) tests-green SATISFIED — kernel-shape drift was
  framework-W2 debt (`1725f55`), repaired this session as its own commit
  `b6f3d75` (KERNEL-SHAPE completed + test re-pins six kernels; 10/10
  standalone); suite now honestly green for P2–P8 fences. (b)
  `src/app/shared/` ACCEPTED as the home for both-sides .cljc (CLAUDE.md
  updated; face_assembly migrates at a natural P3 touchpoint). (c) commit
  rode Sid's standing 07-24 word; stray `.wtest` deleted, not committed.
- Open doubts (non-blocking, falsifiers in GATE_P1.md): cross-JVM activation
  epoch staleness (check at P6 review) · wear in every render-sig (echo-gate
  data point) · drill accretion is by-design retained trace.
- Next: Sid sends the P2 prompt (PROMPTS.md §P2) to a fresh Codex session.

- **2026-07-24 · Codex · P2 (material inspector) — BUILT, stopped honestly at
  the dead cluster (host reboot, not Codex); live replay NOT PROVEN their
  turn.** Full proof list in their report; adjudicated at the gate below.

- **2026-07-24 · Fable · P2 GATE — PASS. Code committed `c6bb5d8`; P3 ∥ P4
  open.**
- Full record: `GATE_P2.md`. Receipts re-run here: full suite 385t/5,220a
  FULLY green (45 nss; kernel-shape repair holds, no flake fired) · cljs 302
  files 0 warnings · falsification held (read-only fence physical-proven,
  fixed read cost, no wall clock, g21 scan widened minimally).
- The gate ran the wearing Codex was blocked on: recovered the cluster
  (`bin/land up`, no unwedge — host-reboot kill recovers natively), booted
  the dev app, drove `window.__material` headless: 34 wearers on v0,
  active ≠ latest LIVE (latest = P1's retained malformed candidate,
  byte-identical to the NOW record, across the reboot), trail 3/2/1
  complete, `edn()` twice byte-equal (22,197B). Sid's one headed line:
  click a block → `__material.picked()`.
- Adjudications: scene-derived wearers = CORRECT fence reading (durable
  index is §Horizon/P6; honest `:derived` labels) · no face = correct ·
  open doubts in GATE_P2.md (roster-shadowing nit; trail classification
  switches to event truth at P6).
- Ops: land left UP (five modules RUNNING, dev app on 8080).
- Next: Sid sends P3 (PROMPTS.md §P3) and P4 (§P4 — ∥ ONLY in a separate
  git worktree) to fresh Codex sessions.

- **2026-07-24 · Codex · P3 FACET 2 (attention) — IMPLEMENTED; mandatory
  second-wearer halt. Awaiting Fable adjudication before facet 3.**
- **Facet 1 / provenance proof carried forward and made collision-explicit.**
  Durable grammar 0 keeps its original source bytes and tint meaning and
  remains rewearable. A separate immutable grammar-1 revision declares only
  the composition law demanded by the first real collision:
  `:facet-master/merge :append`, priority `10`; it is explicitly imported and
  activated, never backfilled into v0. The old provenance-only OC adapter is
  deleted.
- **Facet 2 / attention proof.** `fm:attention` now owns the exact worn box
  policy: hit padding `8.0`, border width `1.0`, border color
  `[0.45 0.52 0.66 0.55]`, transparent background, merge `:append`, priority
  `20`. The old `block-pad` and `attention-border` constants are deleted from
  the renderer. Hover/focus remain ephemeral mechanism. Attention rides the
  same deterministic OC identity, immutable revision, active pointer,
  activation, rollback, one ingest epoch, one batched `:facet-materials`
  FacePull, causal master+revision stamps, and code-floor/error-card totality
  as provenance.
- **First collision proof.** Provenance's machine rail and attention's box
  both contribute to `:block/decorations`. The masters declare append with
  priorities `10` then `20`; the minimum composer orders them deterministically.
  Missing/incompatible merge, missing priority, or a priority tie returns
  structured conflict data which the actual block tree renders as visible
  `:error-card` lint. There is no silent winner and no recipe.
- **Current-value render proof.** Compiler assertions pin every extracted
  value to the deleted literals and pin the code floor to the same policy.
  A headed before/after wear at the same camera preserved the box at
  x≈92..576, y≈92..188 and the sampled left/right/bottom border pixels
  exactly. Whole-PNG bytes were not used as the receipt: separate Chrome/WebGPU
  processes varied glyph/top-edge antialiasing while geometry and policy
  remained unchanged. No later-facet literal moved.
- **Lifecycle/totality proof.** Candidate save advanced latest without moving
  active; accepted activation moved the attention pointer; rollback repointed
  only that pointer. The live malformed attention candidate
  `rev:fm:attention:57c9d6b23b767b2be42f80ab0044533bd0c6685c85834a627d05076bc799c6f0:a1f13e6e8e0112b048e6c28b4e16f083a130a3a20bc7cc0845e8b1fdce61b33c`
  remains latest and queryable, its parse error renders beside the land, and
  active remains the valid default revision
  `rev:fm:attention:57c9d6b23b767b2be42f80ab0044533bd0c6685c85834a627d05076bc799c6f0:7cdb19ad7515dd939abe0e821a153e6e93b22097e39cf7ff1cc31c6fd1509733`.
  A post-restart cluster-backed inspector read returned boolean
  `:attachment/present? true` for both attention and provenance, with their
  independent active/latest identities.
- **Echo and executable receipts.** The isolated attention drill recorded
  `n=4 p50=23.7 p95=29.2 p99=29.2 max=29.2` ms against the unchanged 52ms
  bar. Focused lifecycle/composition/projection tests: 15 tests / 195
  assertions, all green. Fresh-context full-suite rerun: 45 namespaces, 386
  tests / 5,246 assertions, all green. Two earlier all-namespace runs each
  exposed only the registered `dogfood-llm-probe` suite-order flake; its exact
  namespace reran standalone green (1 test / 67 assertions), and the
  fresh-context full run did not reproduce it. CLJS dev build: 305 files, 0
  warnings. `git diff --check`: clean.
- **SECOND-WEARER REUSE TEST — PASS.** Explicit FAIL-tell matrix:
  1. no parallel artery — one registry batch, FacePull, mirror, and epoch;
  2. no source-specific client transport branch — facet-specific code only
     consumes its policy at the honest render boundary;
  3. no compatibility adapter — the provenance-only adapter is deleted;
  4. no v0 reinterpretation — provenance v0 and v1 are separate grammars and
     immutable revisions;
  5. no divergent activation — both use the same generic pointer edit;
  6. no duplicated identity machinery — document/import/revision/pointer
     identities are derived once from each spec;
  7. no hidden compositor case — the real shared slot declares merge+priority,
     and invalid collisions visibly lint;
  8. no weakened totality or performance — every master independently floors,
     malformed latest never wears, the inspector performs fixed reads per
     selected registered master with no wearer N+1, and the live echo stays
     below the bar.
- **REASONING-COST CHECK — PASS.** The two-facet path has one uniform trace:
  facet spec → immutable OC revision → explicit active pointer → one batched
  serve → total resolved wear → causal stamp → declared slot/merge/priority →
  node or visible lint. That single invariant surface replaces embedded
  literals, the provenance-only lifecycle, and implicit child ordering. Facet
  policy stays local to two small masters; transport, identity, activation,
  totality, inspection, and composition do not fork per facet. The added
  composer makes the one actual collision easier to inspect than the monolith,
  not harder.
- **Fences and halt.** No recipe, bindings, dispatch/verb migration, bulk
  extraction, new facet, Rama module/PState/depot, durable attachment owner,
  wear index, or §Horizon machinery was added. `fold-default`, fold header
  text, `reply-gap 34.0`, placement/birth rules, thread-column adoption, and
  wrap floor/fallback remain in code untouched. Both stop checks PASS and echo
  did not regress. **END P3 RUN HERE: facet 3 (foldable) waits for Fable.**

- **2026-07-24 · Fable · P3 FACET-2 HALT — ADJUDICATED: second-wearer PASS ·
  reasoning-cost PASS. Facet 3 AUTHORIZED. Flag A pre-ruled; P4 sendable.**
- Independent verification, not report-trust: full diff read (19 files); all
  8 FAIL-tells checked against source — one artery (one `:facet-materials`
  batch/request/mirror/epoch); provenance adapter DELETED with its v0
  request-ids byte-preserved in the generic `ensure-master!` (live durable
  rows replay as no-ops); the per-version grammar map makes v0
  reinterpretation structurally impossible; one lifecycle parameterized by
  spec; the real `:block/decorations` collision composed with structured
  conflicts rendered as visible lint; per-facet try/catch floors that still
  compose (the floor's move to v1 carries merge/priority — load-bearing).
  `fm:attention` rides P1's generic `fm:` routing — ZERO Rama edits for
  wearer 2, the reuse thesis proven at the kernel layer.
- Suite re-run here: 386t/5,246a; only red = the registered
  `dogfood-llm-probe` flake (standalone green 1t/67a — protocol satisfied;
  Codex's fresh-context run was 0-fail). The re-cut is the sanctioned
  two-wearer generalization (DIRECTION: "or re-cut while there are only two
  wearers").
- Deviation recorded: the whole-PNG byte-proof degraded to sampled border
  pixels + geometry + compiler value-pins (cross-process WebGPU antialias
  variance, honestly disclosed). Non-blocking; the P3-END gate re-attempts a
  controlled proof; Sid's headed eyes at return are the standing falsifier.
- **FLAG A RULING (first-light §9 redline 1): `:references` STAYS the wish
  edge — no Wish kind is minted.** Wish-ness lives in the wish unit's
  material + projection per first-light §4, never the edge; kind-naming is
  recurrence-driven and Sid-only (DIRECTION: precedent, not statute); P4's
  kernel budget stays the two enumerated reviewed lines. Revisit trigger:
  recurring wish-shaped queries filtering `:references` by wish material →
  Sid names the kind at the marks/kinds round; `based-on` chains survive.
  Ruling filled into PROMPTS.md §P4; Sid may veto in one line.
- Next: Codex resumes P3 from facet 3 (resume line at PROMPTS.md bottom);
  P4 is sendable (∥ ONLY in a separate git worktree).
