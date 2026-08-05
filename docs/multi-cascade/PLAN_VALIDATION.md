# Multi-cascade R2 — Phase 2 plan validation

Status: fresh-context adversarial validation on 2026-07-27. The binding inputs
were `CONTRACT_R2.md`, the current `IMPLICIT_SPEC.md`, `PLAN.md`, and
`NOW.md` STANDING-R2 plus its latest manifest ruling. The Rama Phase-2
template, the pinned Rama 1.6.0 API, the local Rama reference extraction, and
the current manifest source were checked independently. Default verdict:
**FAIL**.

## Verdict

`PLAN.md` is not safe to implement. It contains one direct durable-identity
violation, one genuine contract/platform policy fork, one binding §6 manifest
substance mismatch in the required deploy path, and several plan-completeness
failures. The topology's narrow state-transition core is mostly sound, but that
does not validate the package.

### Blocking finding B1 — identity bytes discard keyword namespaces

- **Binding source:** CONTRACT_R2.md:146-155 and 268-273 require
  `"casc-em:episode-turn-closed:" + sha256(turn-id NUL terminal-status)` and
  `"casc-run:" + sha256(cascade-id NUL emission-id)`; T1 says these exact
  bytes are durable truth. CONTRACT_R2.md:235-237 separately and explicitly
  says `(name terminal-status)` for the defunct import key.
- **Plan:** PLAN.md:142-146 instead hashes `name(terminal-status)` and
  `name(cascade-id)`.
- **Concrete falsifier:** `(name :cascade/episode-retry)` and
  `(name :other/episode-retry)` are both `"episode-retry"`. With emission
  `"casc-em:episode-turn-closed:E"`, the plan produces the same run id for two
  distinct namespaced cascade rows. Likewise `(name :episode/failed)` and
  `(name :worker/failed)` collide. Even the current unqualified `:failed`
  hashes different bytes under `name` (`"failed"`) than under the full keyword
  token (`":failed"`).
- **Fault/race consequence:** two clients dispatching those distinct rows
  concurrently route to one `:run/id`; the second obligation overwrites the
  first while obligated. Retry and restart preserve the collision forever.
- **Required plan change:** pin the exact canonical UTF-8 byte encoder for the
  *full* terminal-status and cascade-id tokens, including keyword namespace,
  and use it in constructors, topology revalidation, and two-independent-
  runtime byte tests. Do not use `name` in either R2 id. Keep `name` only where
  CONTRACT §3f explicitly requires it for the defunct import key.
- **Classification:** plan violation, not a policy fork.
- **Verdict:** **FAIL**.

### Blocking finding B2 — pending read plan is physically inconsistent

- **Binding source:** CONTRACT_R2.md:126-127 fixes
  `$$cascade-pending` as `{String run-id → Long obligated-at-ms}`.
  CONTRACT_R2.md:156-160 fixes pending enumeration as one seek plus sequential
  iteration per partition. IMPLICIT_SPEC.md:177-185 requires the result to
  contain enough obligation data to resume.
- **Plan:** PLAN.md:51-66 scans pending once per partition and then performs
  one point seek into `$$cascade-runs` for every pending run: `P + K` seeks.
- **Concrete falsifier:** with `P=8` and `K=50,000` pending obligations after
  a prolonged handler outage, the contract read is 8 seeks plus sequential
  iteration. The plan performs 50,008 seeks, approximately 25 seconds of seek
  time before network overhead using the Rama skill's 0.5ms estimate.
  "K is small by construction" has no enforcing mechanism; the contract says
  only pending-normality, and a module/handler outage makes K unbounded.
- **Race consequence:** after the pending scan returns `(R, 1000)`, a normal
  observation can terminalize `R` before the K-th run read. PLAN.md:59-60
  checks only timestamp equality. A terminal row preserves
  `:obligated-at-ms=1000`, so the plan can return it as resumable even though
  O7 requires every and only nonterminal obligated runs.
- **Required ruling:** the fixed `Long` pending value cannot both carry enough
  data to resume and satisfy the fixed no-K-read plan. A revision must choose
  one physically buildable policy: permit and cost the K reads with a
  status-and-timestamp recheck; change the pending value to carry the bounded
  resumable obligation; or change the fixed read/topology shape. Phase 1 may
  not choose among these because each changes binding contract substance.
- **Classification:** **genuine CONTRACT §6 policy fork / pre-code stop**.
- **Verdict:** **FAIL**.

### Blocking finding B3 — G12's mandated `bin/land deploy` path does not do
what the contract and plan claim

- **Binding source:** CONTRACT_R2.md:461-470 requires `bin/land deploy` to
  deploy the sixth module, preserve the five running modules, restart a worker,
  and prove native offset/state preservation. CONTRACT_R2.md:474-481 makes a
  substantive mismatch in `bin/land` deploy mechanics a §6 stop.
- **Disk:** bin/land:24-30 lists the five already-deployed module vars.
  bin/land:136-153 loops from the first entry under `set -e` and invokes
  `rama deploy --action launch` for *every* entry. The pinned CLI help says
  `launch` starts a new module and `update` redeploys an existing module.
  A read-only cluster query confirmed that the exact five modules are already
  deployed. Adding one sixth `MODULE_VARS` line cannot make this command reach
  the sixth entry cleanly.
- **Plan:** PLAN.md:518-524 says to run that command and assumes all six settle.
  PLAN.md:539-542 further claims a clean redeploy path, although the same
  command still uses `--action launch`.
- **Checkpoint API falsifier:** PLAN.md:525-534 requires a
  “server-readable consumed positions/checkpoint” but names no callable API.
  In pinned Rama 1.6.0, public `com.rpl.rama/get-module-status` and the
  `moduleStatus` CLI expose module state/target ids, not consumed offsets.
  `moduleInstanceStatus` exposes placement/config, and `taskGroupsStatus`
  exposes task-group state; none is a documented stream-offset reader.
  Rama documents internal `$$__streaming-state-<topology>` and Cluster-UI
  telemetry, but the plan names no supported path/schema for executable proof.
- **Worker-target falsifier:** PLAN.md:528 says “TERM only the identified
  cascade-log worker” but gives no command that derives the exact worker PID
  from module instance/replica placement and no restart completion predicate.
- **Required plan/contract action:** stop under §6. Repair the binding deploy
  mechanics or rule a dedicated first-launch/update command; name an actually
  available, executable offset/checkpoint observation or change the required
  evidence; and provide a fail-closed worker-identification/replacement
  procedure. “If no evidence can be obtained, stop” is not a validated plan.
- **Classification:** **CONTRACT §6 manifest-substance stop**, with an
  additional unresolved G12 proof-policy question.
- **Verdict:** **FAIL**.

### Blocking finding B4 — receipt normalization erases the handler contract

- **Binding source:** CONTRACT_R2.md:120, 132-135, 166-171, and 220-226
  require a bounded handler receipt and explicitly call the decline a
  `:skipped` receipt. The existing handler precedent is the plain map
  `{:status :skipped}` at server_jetty.clj:815-816.
- **Plan:** PLAN.md:166-179 fixes a different record vocabulary whose key is
  `:outcome`, then converts completed returns to `:outcome :skipped`.
- **Concrete falsifier:** handler result `{:status :skipped}` becomes
  `{:outcome :skipped :reason nil ...}`. A live `cascade/run` read no longer
  preserves the bounded handler receipt it claims to record.
- **Required plan change:** preserve the contract-visible `:status` key and
  specify exact bounded receipt variants for skipped, repaired/rejected, and
  thrown outcomes. Validators may cap strings and select keys; they must not
  silently rename the handler's status vocabulary.
- **Classification:** plan violation, not a policy fork.
- **Verdict:** **FAIL**.

### Blocking finding B5 — PState “typed records” do not satisfy the Phase-2
schema rules

- **Template source:** artifact-plan-validation.md requires uniform record-like
  values to use `fixed-keys-schema`; when lifecycle instances have different
  fields, it requires an interface plus concrete records and explicitly rejects
  one optional/nil-field shape.
- **Plan:** PLAN.md:197-242 declares one `CascadeRunRow` with six nullable
  lifecycle fields and PLAN.md:246-250 stores it as a class leaf.
- **Concrete falsifier:** an obligated row has terminal fields nil; an
  observation-before-obligation row has obligation fields nil; a terminal
  after obligation row has both. These are lifecycle variants encoded by
  optional/nil fields. `{String CascadeRunRow}` validates only the outer record
  class; Clojure `defrecord` field declarations do not enforce the asserted
  nested `String-or-nil`, `Long-or-nil`, or receipt domains. The proposed
  validators are program logic, not the declared Rama PState schema.
- **What does pass:** Rama 1.6.0 serializes `defrecord`; the wire envelopes are
  plain maps; explicit reader conversion can return plain maps; no literal
  `Object` or `IPersistentMap` appears in the proposed PState schema.
- **Required plan change:** choose the template-compliant schema shape. For
  real lifecycle variants, define a common interface with concrete records
  whose fields match each variant, validate before construction, and convert
  to/from the contract's plain map wire/read forms. If the author instead
  claims one uniform map shape, use `fixed-keys-schema` and show how every
  field's class and absence semantics are enforced without `Object`.
- **Classification:** plan violation, not a policy fork.
- **Verdict:** **FAIL**.

### Blocking finding B6 — the plan does not design the P2 operations it claims
to validate

- **Plan:** PLAN.md:458-474 assigns files; PLAN.md:546-556 lists gate labels.
  It does not give implementable algorithms for row grammar, the exact
  best-effort branch preservation, handler resolution/normalization, sweep
  selection and counts, repair request construction, compare-and-remove,
  defunct reads, adoption filtering, the second-failure monotonicity path, or
  boot placement.
- **Concrete consequence:** the operation traces O1, O2, and O9-O13 below
  cannot cite a complete mechanism. A later test label is not a plan.
- **Required plan change:** add exact P2 algorithms with input validation,
  state/read calls, ordering, total failure behavior, and each fault/race
  outcome, while staying inside the fixed file fence.
- **Classification:** plan incompleteness, not a policy fork.
- **Verdict:** **FAIL**.

## Platform/API verification

### API-1 — generated hashing keys

- Pinned jar inspection confirms
  `com.rpl.rama.Helpers/genHashingIndexKeys(int)` and
  `genHashingIndexKeys(String,int)` exist and return `List<String>`.
- PLAN.md:52-53 says only `Helpers/genHashingIndexKeys` and leaves import,
  overload, and argument narrowing unspecified; PLAN.md:630 incorrectly
  defers proof of the API to P1.
- Required plan spelling:
  `(:import [com.rpl.rama Helpers])` and
  `(Helpers/genHashingIndexKeys (int num-partitions))`, or the fully qualified
  class call.
- **Verdict:** **FAIL as written; API itself exists**.

### API-2 — foreign object information on a PState

- Pinned `com.rpl.rama/foreign-object-info` has arity `[pobject]` and returns
  `:name`, `:module-name`, and `:num-partitions`. The official local reference
  at docs/reference/rama/28-clj-defining-modules.md:412-420 explicitly says it
  also works on PStates.
- **Verdict:** **PASS**.

### API-3 — root `ALL` with explicit `:pkey`

- Pinned `com.rpl.rama/foreign-select` accepts `[path pstate options]`.
  docs/reference/rama/28-clj-defining-modules.md:443-461 confirms foreign
  paths query one partition and `:pkey` overrides first-key routing. Thus
  `(foreign-select [ALL] pstate {:pkey generated-key})` is a valid production
  shape for one partition.
- The plan must cite this exact call, not only prose.
- **Verdict:** **PASS in substance; citation/call-shape correction required**.

### API-4 — server-read module inventory/status

- Pinned `com.rpl.rama/deployed-module-names` and
  `com.rpl.rama/get-module-status` exist. The CLI `moduleStatus` reports
  server state and target ids.
- PLAN.md does not name these calls even though G12 requires executable
  attestation.
- **Verdict:** **FAIL as a plan; APIs exist**.

### API-5 — server-read consumed offsets/checkpoints

- No public Clojure var or documented CLI command in pinned Rama 1.6.0 was
  found that returns a stream topology's consumed offsets. `moduleStatus`
  does not contain them. The internal progress PState is documented as an
  implementation detail and its readable schema/call is not supplied.
- **Verdict:** **FAIL; PLAN.md:525-534 names evidence, not an API**.

## Phase-2 template checks

### Query topologies

- Query topology: none (PLAN.md:367-369).
- Example `cascade/run`, run `"casc-run:R1"` present: `N=1`, `M=1`.
- Example malformed run id `""`: `N=0`, `M=0`.
- Pending example `P=8,K=3`: plan reads `N=11`; all 11 contribute data, but
  the binding read budget is 8 range seeks, so this is a contract failure,
  not a wasted-query-topology read.
- Pending example `P=8,K=50,000`: `N=50,008`; M varies with K and the plan
  names the dynamic loop, but the read contract and production cost fail.
- Failed example `P=8,R=1,000,000,F=10`: all one million rows are transferred
  or inspected to return ten results. PLAN.md:82-85 acknowledges unbounded
  history and explicitly refuses pagination.
- **Verdict:** no query-topology item is applicable, but the direct read plan
  **FAILS** the contract and large-scale readiness checks.

### PState schemas

- Any literal `Object`: no.
- Any `IPersistentMap`: no.
- Uniform record-like values use `fixed-keys-schema`: no.
- Different lifecycle instances use interface plus distinct records: no.
- Inner collection that can exceed 100 without subindex: none; each stored
  value is fixed-cardinality.
- Top-level run and pending maps index each key separately; no nested growing
  collection is hidden.
- **Verdict:** **FAIL** because the required typed variant/fixed-key rule is
  not met.

### Topologies

- Microbatch unless justified: stream is justified by the `:ack`
  materialize-before-call latency barrier (PLAN.md:298-305).
- Low-latency W1 is stream: yes.
- One topology owns both PStates and contains two source branches:
  PLAN.md:298-300.
- **Verdict:** **PASS**.

### Production readiness

- Multiple clients on the same run: topology events serialize on the
  hash(run-id) task, but identity collisions, stale enumeration, and
  conflicting obligated payloads are not closed. **FAIL**.
- Client restart: durable PStates survive, but P2 resumption is not designed.
  **FAIL**.
- Worker restart: Rama durability is valid, but G12 has no executable proof
  and `bin/land deploy` is substantively wrong. **FAIL**.
- Millions/unbounded growth: unpaged failed scan and unbounded K pending seeks
  are not production-ready. **FAIL**.
- Stream non-idempotent writes: none. All planned writes are absolute
  `termval` or `NONE>` operations. **PASS**.
- Stream multi-partition write: none. Both PState writes are before any
  partitioner and on one task. **PASS**.
- **Overall verdict:** **FAIL**.

### Cross-topology correctness

- No internal depot append exists inside the topology.
- Handler-side observation append is an external second ingress, not a
  topology-to-topology append.
- Duplicate W2 appends are handled by same-status no-op or conflict value
  replacement.
- **Verdict:** **PASS**.

### Stream `depot-partition-append!` rule

- The topology contains no `depot-partition-append!`.
- **Verdict:** **N/A**.

## Spec coverage — every operation

### O1 — enumerate and validate cascade rows

- **Source:** IMPLICIT_SPEC.md:103-113: “R2 returns row #1 unchanged followed
  by `:cascade/episode-retry`” and a durable row needs a nonblank full
  idempotency story.
- **Concrete trace:** call `(cascade/rows)` with the two-row table; an invalid
  third durable row has `"   "` as its story. PLAN.md:462 mentions grammar and
  PLAN.md:450-455 mentions a later G10 test, but no validator or exact
  refusal point is specified.
- **Restart/retry:** code data reloads identically; no durable write.
- **Multi-client/out-of-order:** callers only read immutable code data.
- **Flaw:** uniqueness, closed runner values, closed effect vocabulary, and
  story-content validation are not planned.
- **Required change:** specify the pure validator, when it runs, exact
  rejection, and tests for duplicate ids, unknown runner/effect, blank story,
  and unmatched trigger.
- **Verdict:** **FAIL**.

### O2 — dispatch a best-effort row

- **Source:** IMPLICIT_SPEC.md:115-126 requires R1's exact future-per-row path,
  receipt, logging, resolution-in-future, and literal false/nil guard behavior.
- **Concrete trace:** for row #1, `gold-receipt=false` and `rk-rt=false`, the
  current handler returns `{:status :skipped}` while `react!` immediately
  returns `{:cascade/id :cascade/material-autotag :dispatched? true}`.
  PLAN.md:548 names G1 but gives no branch pseudocode showing the current
  lines remain the exact best-effort path.
- **Restart/retry:** this lane remains intentionally best effort.
- **Multi-client/out-of-order:** independent futures preserve R1 behavior.
- **Flaw:** file assignment and a future test do not prove an implementable
  byte-behavior-identical branch.
- **Required change:** quote the exact existing branch and show the new
  runner dispatch wraps around it without changing row #1 or its code path.
- **Verdict:** **FAIL**.

### O3 — mint close-emission and run identities

- **Source:** IMPLICIT_SPEC.md:128-138 and CONTRACT §3b/T1.
- **Concrete trace:** turn `"T-7"`, status `:failed`, cascade
  `:cascade/episode-retry`; exact replay must match, `:timeout` must differ.
  PLAN.md:142-146 drops keyword token bytes via `name`; namespaced collisions
  are demonstrated in B1.
- **Restart/retry:** bad bytes are deterministic but permanently wrong.
- **Multi-client/out-of-order:** collision merges distinct clients' runs.
- **Required change:** B1, plus topology-side relational validation that
  supplied run/emission ids equal the canonical derivation.
- **Verdict:** **FAIL**.

### O4 — record and acknowledge a durable obligation

- **Source:** IMPLICIT_SPEC.md:140-152.
- **Concrete trace:** append run `R`, payload P, time 1000 with `:ack`.
  PLAN.md:307-325 reads absent, writes run+pending atomically, and an exact
  retry remains one obligated row. A terminal replay is a no-op.
- **Restart/retry:** absolute same-task writes recover safely; `:all-after`
  replays from the source branch.
- **Multi-client/out-of-order:** same task serializes writes, but the plan does
  not recompute/validate the identity relationships, so two well-typed maps can
  target the same `R` with different cascade/emission data.
- **Flaw:** durable identity integrity is only promised by the constructor;
  defensive topology validation checks types, not the pinned equations.
- **Required change:** validate canonical emission/run identities before any
  mutation and specify conflict behavior for a well-typed mismatched envelope.
- **Verdict:** **FAIL**.

### O5 — execute a durable handler and record observation

- **Source:** IMPLICIT_SPEC.md:154-165.
- **Concrete trace:** JVM A and JVM B both execute run R. A returns
  `{:status :skipped}` at t=2000; B throws at t=2001. PLAN.md:327-346 makes A
  completed first-terminal and B the failed late conflict, atomically removing
  pending.
- **Restart/retry:** topology transition is safe. If A's external effect lands
  but W2 append fails, R stays obligated and sweep may execute again, which the
  row's idempotency law must tolerate.
- **Multi-client/out-of-order:** observation before obligation is handled; two
  W2 records serialize.
- **Flaws:** P2 common runner is not designed, and receipt normalization erases
  `:status`.
- **Required change:** specify the common runner, bounded receipt variants,
  append failure behavior, and exact return/throw normalization.
- **Verdict:** **FAIL**.

### O6 — read one run

- **Source:** IMPLICIT_SPEC.md:167-175.
- **Concrete trace:** one read for `R`; absent `R2` returns nil.
  PLAN.md:28-38 gives the exact point read and plain-map conversion.
- **Restart/retry:** PState is authoritative and durable.
- **Multi-client/out-of-order:** each read sees one replicated partition
  state; first-terminal semantics remain intact.
- **Flaw:** dependent PState schema is invalid under the template, but the
  read algorithm itself is sound.
- **Required change:** repair the schema and retain this one-seek read.
- **Verdict:** **PASS for read algorithm; package check remains FAIL**.

### O7 — enumerate pending runs

- **Source:** IMPLICIT_SPEC.md:177-185 and CONTRACT_R2.md:156-160.
- **Concrete trace:** `P=8,K=50,000`; see B2. A terminalization between pending
  scan and row enrichment passes the plan's timestamp-only check.
- **Restart/retry:** persisted pending survives, but client-side scans are not
  one snapshot.
- **Multi-client/out-of-order:** concurrent W2 can make a scanned entry stale;
  new W1 can arrive after its partition was scanned.
- **Flaws:** contract read-plan fork, unbounded seeks, no status recheck, and
  no stated consistency boundary.
- **Required change:** ruling B2, then specify status+timestamp validation and
  the sweep's last pre-execution recheck.
- **Verdict:** **FAIL / §6 policy fork**.

### O8 — enumerate failed runs

- **Source:** IMPLICIT_SPEC.md:187-196 says history grows without bound.
- **Concrete trace:** 1,000,000 lifetime rows with 10 first-terminal failures.
  PLAN.md:76-85 root-scans and filters all million in unbounded responses.
- **Restart/retry:** durable rows survive; repeating the scan is logically
  stable but expensive.
- **Multi-client/out-of-order:** concurrent terminalizations make a
  cross-partition result interval-consistent, not a named snapshot.
- **Flaw:** no page/range bound or consistency semantics; one huge foreign
  `ALL` can occupy a task and client memory.
- **Required change:** design bounded per-partition range paging/yielding and
  state the result consistency. If one eager all-history collection is truly
  binding, obtain a performance ruling.
- **Verdict:** **FAIL**.

### O9 — resume old obligated runs

- **Source:** IMPLICIT_SPEC.md:198-210 requires nonnegative grace, strict
  “older than”, exact-boundary exclusion, one log with three counts, same
  runner, no failed/completed execution.
- **Concrete trace:** invocation `now=61,000, grace=60,000`; timestamp 999 is
  old, timestamp 1000 is exactly boundary and must not run, timestamp 1001 is
  young. PLAN.md:383-385 only says grace is evaluated outside topology;
  PLAN.md:552 and 555 only name later gates.
- **Restart/retry:** two JVM boot sweeps may overlap; no P2 algorithm says
  how each rechecks terminality or normalizes observation failure.
- **Multi-client/out-of-order:** normal completion can race between scan and
  handler call.
- **Flaws:** no `grace-ms >= 0` validation, strict inequality, captured
  invocation time, failed count read, runner call, status recheck, or one-line
  log algorithm.
- **Required change:** specify all of those, including exact
  `obligated-at-ms < now-ms - grace-ms`.
- **Verdict:** **FAIL**.

### O10 — emit `:episode/turn-closed`

- **Source:** IMPLICIT_SPEC.md:212-223.
- **Concrete trace:** failed fresh turn T at time 1000, no jsonl. The emission
  must contain the exact 12 scalar keys and finish its total attempt before
  `record-turn!` overwrites `:open` with `:failed`. PLAN.md:465-466 names the
  location and W1 lists keys, but no exact waiter transformation or error path.
- **Restart/retry:** JVM death after acknowledged emission but before cell
  overwrite is repairable; before ack may lose emission without killing turn.
- **Multi-client/out-of-order:** identical HTTP replay derives same identity.
- **Flaw:** no implementable site algorithm, jsonl observation timing, or
  total wrapper trace.
- **Required change:** show the exact insertion before current
  server_jetty.clj:1069-1083 and build the scalar payload from waiter values.
- **Verdict:** **FAIL**.

### O11 — repair a stranded lane

- **Source:** IMPLICIT_SPEC.md:225-243.
- **Concrete trace:** failed fresh episode E, turn T, absent file at close and
  execution. It must append one system-authored transition-scoped import, await
  accepted truth, then compare-remove only local entry E. PLAN.md:467-469 and
  550-553 provide file/gate labels only.
- **Restart/retry:** duplicate JVM executions must converge on one import;
  file appearance before a replay must skip.
- **Multi-client/out-of-order:** an old repair must not remove local entry E2.
- **Flaws:** no guard, execution-time recheck, exact request/actor, accepted-vs-
  rejected handling, replay-stable value, or compare-remove algorithm.
- **Required change:** fully design the handler and preserve
  `{:status :skipped}`.
- **Verdict:** **FAIL**.

### O12 — adopt current episode

- **Source:** IMPLICIT_SPEC.md:245-259.
- **Concrete trace:** marker E at 1000, turns E@900, E@1000, E@1001, X@950.
  Only E@900/E@1000 are filtered; newest survivor E@1001 revalidates E.
  PLAN.md:467-469 says “fallback filtering” but provides no read/merge logic.
- **Restart/retry:** empty runtime atom uses durable reads; marker-read throw
  must use the exact pre-R2 fallback.
- **Multi-client/out-of-order:** warm runtime path must do no added durable
  read; different episode marker cannot filter X.
- **Required change:** specify marker read shape, per-episode/time predicate,
  read-failure fallback, and unchanged `decide-episode` call.
- **Verdict:** **FAIL**.

### O13 — second eligible failure of the same episode id

- **Source:** IMPLICIT_SPEC.md:261-274.
- **Concrete trace:** A at 1000 lands import IA; B at 2000 lands distinct IB
  into the same order key; old IA replay must be journal no-op and never
  regress marker B. PLAN.md:550-551 names the test but no mechanism.
- **Restart/retry:** old and new repairs can be invoked by different JVMs; the
  existing import journal must make IA replay a no-op.
- **Multi-client/out-of-order:** B is causally later because A's visible marker
  enabled revalidation; the plan must preserve that reasoning in request ids.
- **Required change:** specify exact imp-key/order-key/value/request flow and
  authoritative reads after A, B, replay A, and replay B.
- **Verdict:** **FAIL**.

### O14 — boot and deploy

- **Source:** IMPLICIT_SPEC.md:276-287.
- **Concrete trace:** exact five running, sixth absent, launch sixth, kill only
  its worker, reopen handles, prove state/progress, then terminalize.
- **Restart/retry:** native PState durability is valid Rama behavior, but the
  executable path fails B3.
- **Multi-client/out-of-order:** each server boot may sweep; duplicate handlers
  must remain safe.
- **Required change:** resolve B3 and specify exactly one post-binding boot
  invocation site.
- **Verdict:** **FAIL / §6 stop**.

## Non-operation invariant checks

### C1 — one run id forever

PLAN.md:142-146 is deterministic but not namespace-preserving. **FAIL; B1**.

### C2 — obligation replay cannot create a second row

Absolute writes under the same top-level key satisfy this after identity is
fixed. Retry and restart reproduce one key. **PASS conditionally**.

### C3 — obligation after terminal cannot resurrect or re-pend

PLAN.md:321-322 guards both terminal states and writes nothing. **PASS**.

### C4 — first terminal and same-status replay

PLAN.md:333-341 fixes the base terminal and makes same-status replay a no-op.
One task serializes concurrent clients. **PASS**.

### C5 — last contradictory evidence, never a counter

PLAN.md:339-346 replaces one value and uses `:all-after` within the observation
depot partition. Reprocessing the ordered suffix converges. **PASS**.

### C6 — failed terminal enumerable but never swept

Enumeration classification is planned, but sweep selection/recheck is not.
**FAIL; O9**.

### C7 — duplicate handler execution is independently safe

The plan assigns this to P2 tests but does not design the repair's durable and
runtime convergence. **FAIL; O11/O13**.

### C8 — obligation ack before handler

PLAN.md:149-151, 181-182, and 357-358 establish the order and owned-stream
`:ack` has the required read-after-write semantics. **PASS for W1**.

### C9 — durable immediate receipt exact keys and after ack

PLAN.md:549 names the test but never gives the P2 construction. The key set is
not restated in the P2 algorithm. **FAIL; O2/O5 plan gap**.

### C10 — bounded terminal receipt

Size caps and fixed keys are intended, but `:status` is renamed and schema
variants are invalid. **FAIL; B4/B5**.

### C11 — bounded EDN payload; no runtime/fn/stream/`:lines`

PLAN.md:100-105 and 122-137 give an exact scalar key set and defensive
validation. A restart sees only data. **PASS**, subject to implementation tests.

### C12 — durable emission failure is total

PLAN.md has no P2 try/catch/log/receipt algorithm; a gate label is not enough.
**FAIL; B6**.

### C13 — namespace loads are inert

PLAN.md:406-420 makes constructors callable-only and PLAN.md:498-500 gives a
fresh-require proof. **PASS for P1**; P2 edited namespaces remain only a gate
label, so full-package **FAIL**.

### C14 — existing projection readers ignore the new kind

PLAN.md:554 names G7/G8 but does not enumerate or trace the actual positive
entry-kind filters. **FAIL as a plan**.

### C15 — no deletion of history

Only pending membership is deleted; run rows are never deleted.
**PASS**.

### C16 — no clock/scheduler/backoff/failed retry/auto-respawn

The module declares no scheduler/TaskGlobal and P2 ownership does not name
respawn. The missing P2 algorithm prevents accidental-code proof, but the
planned architecture respects the refusal. **PASS at design intent**.

### C17 — `:system` actor authorization

PLAN.md:11-13 says it was checked; current core.clj:343-350 confirms system
actors bypass capability checks. The request itself is not designed.
**PASS for authorization claim; O11 remains FAIL**.

## Trap ledger audit

### T1 — durable identity bytes

Namespaced bytes are discarded. **FAIL; B1**.

### T2 — pure loads and data payloads

P1 constructors are inert and W1 payload is data-only. P2 purity is not
designed. **FAIL overall**.

### T3 — converge before call

Owned-stream `:ack` precedes the external runner. **PASS**.

### T4 — replayed repair bytes stable

The plan assigns a test but does not design `marked-at-ms` or the request.
**FAIL**.

### T5 — execution-time file recheck

Only a later gate label exists. **FAIL**.

### T6 — no terminal resurrection

The topology guard is explicit and same-task. **PASS**.

### T7 — transition-scoped defunct identity

Only a test label exists; no P2 constructor is planned. **FAIL**.

### T8 — emission before status overwrite

The location is stated but no code-level transformation/fault trace exists.
**FAIL**.

### T9 — turn lane never hostage to cascade

No total P2 wrapper algorithm or poisoned-runtime behavior is specified.
**FAIL**.

### T10 — best-effort lane byte-identical

Claim and gate exist; exact preserved branch is not designed. **FAIL**.

### T11 — two JVMs

Topology state converges, but handler/adoption convergence is unplanned and
pending scans are stale under races. **FAIL**.

### T12 — trigger names in-process; run truth local

The module owns both run PStates, no OC/RK vocabulary is added, and the table
stays code. **PASS**.

## File-fence and phase ownership

### P1 fence

CONTRACT_R2.md:593-598 is the controlling detailed fence: P1 implementation
belongs in `cascade_log.clj` plus its test, exact test-runner classification,
one `bin/land` module line, and narrowly required runtime plumbing.
PLAN.md:435-456 stays within those named files and does not prebuild
`react!`, sweep, repair, or activation. The latest NOW phrase
“cascade_log.clj + IPC constructors ONLY” is consistent when read as the
module/source-content boundary, not as a ban on the contract's explicitly
permitted operational/test edits.

The fence itself **PASSES**, but B3 proves the permitted one-line `bin/land`
edit is insufficient to execute G12. That is a §6 substance stop, not
authorization to widen P1.

### P2 fence

PLAN.md:458-479 names only allowlisted P2 files and keeps
`decide-episode`, material circulation, verb registry, and OC/RK/LLM module
files read-only. **PASS**.

### Runtime-plumbing scope

PLAN.md:422-431 limits cluster/file-viewer work to cascade handles and the
existing IPC/cluster boot seam. **PASS in scope**, subject to T9 totality.

## Acceptance-gate audit

### G1

Named but not mechanically planned beyond “byte-behavior identity”.
**FAIL**.

### G2

Module ack/read proof is concrete; emitter half lacks its exact P2 receipt
construction. Identity bytes are wrong. **FAIL**.

### G3

Module transition matrix is concrete and includes observation-before-
obligation, both conflicts, and replay. Repair half is only a label.
**FAIL overall**.

### G4

IPC fresh-handle substrate drill is concrete. Sweep half is only a label and
depends on the B2 fork. **FAIL overall**.

### G5

The plan names the scenario but supplies no repair/adoption mechanisms.
**FAIL**.

### G6

The live drive is named without exact setup, authoritative read calls, or
expected server receipts. **FAIL**.

### G7

The plan restates the fence but does not supply the complete edited-path and
positive-reader scan. **FAIL**.

### G8

The exact W1 payload is bounded and excludes `:lines`; the production-site
scan/assert is only a label. **FAIL overall**.

### G9

P1 classload proof is concrete. Grace, failed poison, boot placement, and P2
classload are not designed. **FAIL overall**.

### G10

The declaration/read gate is named, but grammar and P2 read wrappers are not
designed; pending reads also hit B2. **FAIL**.

### G11

Focused/full commands are partly named, but edited-path inventory and lint
commands are not complete. **FAIL**.

### G12

The executable deployment/restart/checkpoint proof fails B3 and triggers the
binding stop. **FAIL / §6 stop**.

## Non-goal audit

### NG1 — no scheduler or clock

No module timer/tick/scheduler is planned. **PASS**.

### NG2 — no retry/backoff policy

No backoff queue or failed-run retry is planned. **PASS**.

### NG3 — no auto-respawn

No resident spawn is assigned to repair. **PASS at intent; O11 algorithm
missing**.

### NG4 — no autotag migration

Row #1 remains assigned to the old best-effort branch. **PASS at intent; G1
plan incomplete**.

### NG5 — no rows-as-material

The declaration table remains code data. **PASS**.

### NG6 — no cross-boundary resume

`decide-episode` remains unedited and no new resume path is planned. **PASS**.

### NG7 — no new relation kind

No relation vocabulary edit is planned. **PASS**.

### NG8 — no existing durable-shape edit

The only planned module durables are new. Episode repair would add one
projection entry kind through the existing import lane. **PASS at intent**.

### NG9 — no OC/RK/LLM module edit

Those files are explicitly read-only. **PASS**.

### NG10 — no durable declaration table

Rows remain JVM code. **PASS**.

## Required Phase-1 revision list

1. Stop for the B2 contract ruling before redesigning pending enumeration.
2. Stop for the B3 §6 manifest/deploy-mechanics mismatch; do not widen the
   `bin/land` fence without a ruling.
3. Replace both `name` calls in R2 identity derivation with the contract's
   explicit canonical full-token byte encoding and add namespaced collision
   counterexamples.
4. Make topology validators recompute and compare emission/run identities.
5. Replace the optional/nil all-in-one PState record design with the
   template-compliant fixed-key or interface-plus-record variant design while
   retaining plain map wire/read semantics.
6. Preserve handler `:status`, especially `{:status :skipped}`, in bounded
   observation receipts.
7. Spell the exact production calls and namespaces for
   `foreign-object-info`, `Helpers/genHashingIndexKeys`, root `[ALL]` with
   `:pkey`, deployed module inventory, and module status.
8. Add bounded/paged failed enumeration and explicit consistency semantics.
9. Fully design P2 O1/O2/O9-O13, including grace validation and strict
   boundary, one captured sweep time, status recheck, counts, total wrapper,
   execution-time file recheck, system import, compare-remove, adoption
   filtering, second-failure non-regression, and exact boot placement.
10. Replace every “prove in P1/P2” residual-risk deferral with the executable
    mechanism or command the implementation phase is supposed to carry out.

PHASE_VALIDATION:fail
