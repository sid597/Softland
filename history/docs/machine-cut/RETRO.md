# machine-cut — RETRO (close session, 2026-07-13)

Written from the full trail: NOW.md · CONTRACT.md (as amended at gate) ·
LANES.md + LANE_A/B.md · INT.md · FALSIFY_A/B/C.md · RECEIPT_G13.edn ·
the source at committed HEAD (`ceb84da` + gate fixes) · the scene-substrate
G11 session transcript (the wearing's host). Adversarial recheck of this
retro: **NOT RUN — Sid's call on cost** (deferred at gate; the one
skill-protocol step this package skipped at close).

## Shape of the package

Opened and built in ONE day (2026-07-12): Fable contract (every claim
file:line-verified) → Sid's in-session countersign → lanes A ∥ B as fresh
Opus 4.8 subagents with disjoint fences, **both GREEN first-run** → INT by
the orchestrating session (enum pre-landed `f864c74`, boot attach, constant
re-home, observed-model plumb) → serial suite 90t/1431a → G13 receipt over
the REAL corpus with a REAL run ($0.37, 10 assertions against durable
state) → falsification-by-class batch (3 finders) → 4-HIGH cluster fixed at
gate with biting regressions → gate PASS with two open items. Close session
(2026-07-13): G14 recorded (worn in the wild, see below), quiet-box serial
suite at committed HEAD, this retro. No plan-validation layers ran this
package — the lanes went straight from contract to code.

## QC-layer scorecard

- **Contract (file:line verification, traps ledger).** Caught platform
  reality before code (microbatch visibility ≠ ack, the fake-adapter seam,
  synthetic-id validation path) — zero platform surprises at implementation
  contact. Its own §4.3 idempotency text was the wave's ONE contract-text
  error (flat per-edge-forever key; the pre-registered ≥1-error expectation
  confirmed for the third package running). The gates built FROM the
  contract could not catch the contract's own error — falsification did.
- **Plan-validation layers: SKIPPED, and the wave survived it.** Both lanes
  first-run green says the contract + pinned lane prompts carried the load
  for a package this bounded (2 new files + 2 edited, one closed enum).
  Not evidence the layers are dead weight at kernel scale — evidence they
  scale DOWN to zero when the contract is small and file:line-verified.
- **Executable gates (G1–G12, fake adapter).** Held both lanes honest;
  green first-run. MISSED the entire 4-HIGH cluster — all four live in
  driver lifecycle semantics the gate fixtures never stressed
  (assert→retract→re-assert flips, barrier results, crash-between-WAL-and-
  edges). Gates test what the contract imagined; the falsifier tests what
  it didn't.
- **Falsification batch (3 fresh Opus finders, by class).** The wave's
  decisive layer — and its cost lesson. **A (driver lifecycle): all 4 HIGH**
  (transition-keyed idempotency root + barrier-discard + noop-trust +
  replay-scope) **+ 3 MED**, every one CONFIRMED with a scenario. **B
  (serve/fixture drift): 0 HIGH** — 1 cosmetic MED + 4 LOW, the serve core
  survived deliberate attack. **C (boot seams): 0 HIGH**, but C-MC-C2 (no
  lawful live rk handle → second-cluster risk) was real — the SAME class
  fired live at the first wearing attempt (worker-registry collision), so
  layer 5 covered what C found. Sid's verbatim cost flag: ~990k subagent
  tokens for the wave; the HIGH yield was 100% concentrated in the finder
  aimed at the genuinely-new machinery.
- **The wearing (layer 5).** Earned its keep TWICE. First attempt
  (2026-07-12, interrupted): caught the concurrent-cluster worker-registry
  collision live — invisible to every JVM/IPC layer because no suite boots
  the face OC and a fresh rk concurrently. Then the wearing itself arrived
  UNSCHEDULED: Sid drove `boxes-paired-face` through all three
  scene-substrate G11 rounds (2026-07-13) over the annotated 64-block
  window, and the post-reboot boot log carries the receipt —
  `[FACE] machine-cut WAL replay: 1 lines, 34 asserted, 0 retracted,
  0 failed` — the "replays at any dev boot" claim observed at a boot this
  package never orchestrated. G14 closed on Sid's wearer's word + that
  receipt (evidence split recorded in INT.md §G14, salted-re-annotate
  residue named there).
- **Serial-suite discipline + committed-HEAD re-run.** The 07-12 post-fix
  run's 6 fails (llm timing flakes while the dev server booted on the same
  box) re-confirmed the quiet-box rule. The close session's quiet-box run at
  committed HEAD then earned ITS keep: 94t/1476a with ONE failure —
  `g21-read-only-scan` (framework's style gate pinning face_projection.clj's
  exact OC read surface) went stale-red against block-write's committed,
  gate-reviewed `ocr/read-unit` addition (`ad19b96`), which block-write's
  own suite selection never scanned. Classified literal-vs-intent (the
  addition is a read-only OC query API — exactly the class the scan's intent
  blesses), scan enumeration updated with the citation, full suite re-run
  green (final line in NOW.md's close entry). A cross-PACKAGE instance of
  the committed-HEAD staleness class the skill already knew intra-package.

## What the next contract should do differently

1. **Falsification defaults to ONE finder, aimed at the genuinely-new
   machinery.** Grounds: B+C consumed ~2/3 of the batch and found 0 HIGH;
   A found all 4. Widen only when the package touches a prior kill class
   (fixture-vs-live drift, boot seams) that layer 5 will NOT independently
   exercise — the C-MC-C2 caveat: the boot-seam class DID hold a real
   defect, but the wearing caught it independently, which is exactly why
   C's marginal value was low. Already adopted in practice (scene-substrate
   ran ONE finder citing this rule); lands in the skill at this close.
2. **Idempotency/journal keys name their TRANSITION story, not just their
   scope.** "Stable per identity" and "stable per transition" are different
   laws; the contract said the first, the lifecycle needed the second
   (A-F2/F3, the wave's root HIGH — a CONTRACT error, found only by
   falsification). Extends the skill's existing "scope on every uniqueness
   claim" rule with the transition dimension.
3. **A barrier that returns-without-throwing is not a barrier until its
   result is asserted.** `await-relation` returns the last value on
   deadline; every call site discarded it, so counts and the epoch decision
   ran on the PLAN (A-F4). Any await/retry helper in a contract names what
   the caller must CHECK. → also routed to `implementation-quirks.md`.
4. **A wearing can be inherited from real use.** G14 was satisfied inside
   another package's wearing rounds, receipts harvested afterward from the
   boot log + transcript — strictly better evidence than a staged drive
   (nobody was performing for the gate). The gate's bar stays the same; the
   close session's job becomes verifying receipts, not scheduling a drive.
5. **One in-process Rama cluster per land.** Two clusters booting
   concurrently collide in the worker registry (the first wearing's live
   catch); the fix — reuse the trail cluster's rk, export ONE lawful
   accessor (`machine-cut-ctx`) — is the precedent the next boot-attaching
   package cites. → also routed to `implementation-quirks.md`.
6. **A package that edits a file runs every pinned-enumeration scan over
   that file** (grep the test tree for the file's path). Grounds: the g21
   stale-scan catch above — the scan's enumeration is a shared surface;
   whoever moves the surface updates the scan in the same change. → routed
   to the skill's test-harness invariants.

## Mechanisms that earned their keep

- **Traps ledger cited by MC-T number in code** — the transition-key fix,
  replay scoping, and noop-converge all landed AS amendments to named
  traps; reviewers navigated by number.
- **Pinned lane prompts + disjoint fences (LANES.md)** — two parallel
  subagents, zero fence violations (G15 clean), both first-run green.
- **The fake-adapter seam** (`llm.clj` canned `:lines`) — all 12 suite
  gates run with zero LLM spend; the ONE paid run is the receipt.
- **WAL-first durability on the ephemeral cluster** — proven by the
  unorchestrated boot's replay receipt, not by its own tests.
- **G13 as an ASSERTING receipt** (10 assertions against durable state,
  honest coverage verbs: window ≠ conversation) — and its honest-ledger
  note that 100% mechanical agreement on a structurally-trivial window
  exercised the loop, not the annotator's judgment.
- **Countersign gating on the enum line** — the one kernel edit landed as
  an isolated, Sid-authorized commit (`f864c74`) before any lane touched it.

## Residue (non-blocking, carried with falsifiers named)

- **G14's salted re-annotate half** — live falsifier: one paid salted run
  (~$0.40) whenever a real re-annotation is next wanted (mechanics pinned
  green at G8/G9).
- Open doubts from INT (falsifiers named there): A-F8 retry-with-salt ·
  A-F10 replay×live-annotate race · C-MC-C3 Error-scoped replay catch ·
  C-MC-C5 constant-only-require scan blindness · B-F4 hand-seeded chain
  edge.
- **B-F1/F2 fixture vocab drift** (block `:kind`/`:speaker` vs live) —
  inherited W1 class, cosmetic, routed to the harmonization/marks round.
- **For the kinds round**: G13's ledger — the discriminating material
  (multi-prompt windows, interruptions, the "sidetrackkkk" class) sits
  beyond page 1 and waits on the block-kernel §10 paging extension.
- **For thread 2 (reconciliation UI)**: the gold overlay lands against
  these edges (Sid's assertion = a new identity beside `llm:machine-cut/v1`).
- Cost ledger (D-006 criterion 4): contract+INT+gate ≈ one Fable session;
  lanes+falsifiers ≈ 990k subagent tokens (Sid's flag, verbatim); receipts
  $0.37; close ≈ this session.
