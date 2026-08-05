# P6 gate — the truth loop whole — PASS (with two gate-authored fixes)

2026-07-25 · fresh Fable session. Gate object: `be247c5` (code) + `a6a44fb`
(docs), local branch, unpushed — 21 files, +3,143/−118 (the handoff's
+1,682 undercounts; git's summary line is authoritative). Spec:
CONTRACT_P6.md under DIRECTION + CAMPAIGN §P6 + first-light CONTRACT §7
(P4/P5). Diff read in full. Gate fixes landed as `cff3a14` (this session;
author-verifier overlap disclosed — the fresh check on the fixes is the
full-suite re-run + the pinned regression, not this session's judgment).

## Receipts (this session, independently re-derived)

- **Full suite ×2**: gate object as committed — **427t / 5,929a / 0 / 0**,
  exact match with the banked numbers; over the fixed tree —
  **428t / 5,940a / 0 / 0** (+1t/+11a = the F1/F2 regression). Three
  registered flakes clean on attempt 1 both runs; registry unchanged.
- **cljs cold compile** (`:build-options {:cache-level :off}`): 270 files,
  198 compiled, **0 warnings**.
- **G11 recount** (independent grep): 7 deterministic `:time-ms` literal
  sites across 4 files — byte-match with the pinned baseline; all
  grandfathered (P1 v0 bootstrap ×2 in `ensure-master!`, migration stamps
  0/1/2, first-light). No fourth in any production write path.

## R2 id-shape adjudication — ACCEPTED (the stop-clause note resolved)

Verified empirically, not from the implementer's word: `extract-object-key`
funnels `rev:`/`src:`/`oc:block:`/`imp:fm:` through `leading-object-key`,
which collapses an `fm:`-prefixed key to its first TWO colon segments, while
`oc:doc:` returns the whole remainder (object_container.clj:267–389). Probe:
the literal `fm:attention:i:abc12345` puts its document container at
partition key `fm:attention:i:abc12345` and its own pointer/src at
`fm:attention` — **straddles**, the four-times-fired foreign-read mis-route
class. The shipped `fm:<facet>~i~<sha8>` (two segments) routes every derived
id of all six masters to ONE partition key (probed per-spec). R2's five
intent properties (distinct, short, subject-scoped, one per (facet,subject),
full uid inside the form, never in specs) all hold; the alternative was an
unauthorized routing-kernel edit against R1. Contract amended in place;
NOW's stop-clause note stands as history; the regression test pins both the
one-partition law and the straddle.

## Falsification (the genuinely-new machinery)

- **F1 — CONFIRMED BUG, fixed (`cff3a14`)**: the second byte-identical
  non-bootstrap transition silently no-oped with `:accepted? true`.
  pin → unpin → re-pin → **unpin2**: worn stayed PINNED (activation
  `replay? true`); deviate → release → re-deviate → **release2**: stayed
  deviating. Cycle 1 survived only because the first write is a bootstrap
  (pointer inline, its activation id never spent). Root cause:
  content-derived activation request-ids (the machine-cut A-F2/F3
  stable-per-transition class — the contract never named which transitions
  mint a NEW key, and the implementer keyed per-content). Fix:
  transition-keyed activation ids (content + pre-state pointer revision;
  true retries still replay) + `:accepted?` requires the activation on
  non-bootstrap writes. Pinned as a regression.
- **F2 — contract-letter gap, fixed (`cff3a14`)**: a durable instance
  revision carrying `:space/ground` bindings rows was ACCEPTED as valid
  material and served; the only fence was the client consumption filter.
  G10's "the durable instance lane refuses" now fires at write time in
  `write-instance-revision!`. (The camera was never reachable either way:
  `:camera/*` verbs are floor-reserved and unbindable, and the client filter
  held — the gap was refuse-vs-accept-then-ignore.)
- **F3 — dead client lane (residue, non-blocking)**: `announce!` in
  ground.cljs is defined and never called — the client breath/weather
  accumulation never fires and `__bindings.weather()` always returns `[]`
  (live-confirmed). The three scales ARE served (`:truth/announcements`
  projection, JVM-gated G5); the local breath happens via epoch-driven
  reconcile rather than per-change announce. Cheap falsifier for the fix:
  wire `announce!` to served activation arrivals at the P7 portal
  touchpoint, or delete the lane.
- **F4 — escape-detector coverage skew (residue, live-measured)**:
  `default-material-policy-paths` grew 1 → 6 files but
  `terminal-escape-report` still consults ONLY the provenance master's
  activation log, and on the durable cluster it reads
  `:ambiguous-activation-history` **forever** (provenance's grandfathered
  0/1 stamps sit under P1's live drill wall-clocks — permanent regressions,
  honestly exposed, count unmeasurable). The implementer's "escape detector
  measured 0" was true on the ephemeral run only. Fix direction: per-file
  master mapping + causal-tip comparison; owner P7-or-next-touchpoint.
- **R4 third-reader hunt: none found.** The two fixed readers
  (circulation as-of, inspector trail) verified; `analyze-activation-history`
  is row-graph based (never reads content-text); `compiled-pin-target` and
  `instance-state` read REVISIONS, not pointers; src swept.
- **T9 membrane**: the one-door serve (`served-material-value`) + fresh
  overlay identity forces full re-derive (wears cache keys on served
  identity; `:wrap-col` recomputed inside `reconcile!` from the same door);
  no cache keyed off served-identity found that could carry prior-revision
  derived state. Live byte proof below.
- **T7 registry**: G13 JVM-green; the index rides an upsert-only hint lane
  (rows physically undeletable — "drop" is simulated as absence via direct
  subject probes, which is the strongest available form there).
- **Scope note (honest, non-blocking)**: instance deviations are FELT
  per-subject at the block builder (attention/foldable/threaded ride
  `wears-for`, in the render signature); the reconcile pass reads the SHARED
  tier for positioned (correct — settle cells are the positioned instance
  truth, R2 grandfather) and text-body (a text-body instance deviation would
  serve + resolve but not change a reply's wrap; noted for the first real
  need).

## Live gates (cluster was DOWN at handoff; this session's acts)

- `bin/land up`: five modules RUNNING from durable state — **no deploy**,
  confirmed structurally (no deployed-module source in the diff; the
  projections live in the app JVM).
- **v2 grammar ingest** (`cluster/facet-materials-ingest!`) — a WRITE:
  attention/foldable/positioned now wear grammar-2 bytes; pointer sources
  are R4 event forms (`:v0? false`, wall-clock times, `:grounds/ungrounded`
  said honestly). Pre-existing P3-era candidates on text-body/threaded
  (wrap 120 / reach 0.0) remain latest ≠ active, worn by nobody — the law
  standing on durable state.
- **G6 echo bar CLOSED** (adapter attestation FIRST: fallback adapter,
  vendor google — CPU raster; the real GPU is strictly faster): with the
  live deviant + pin IN THE SERVE (`instances()` = 2 attention subjects,
  holds deviation/pin), typing through the real edit lane on a `?drill=`
  world: **120 samples, max 24.3 ms, p50 14.2, p95 17.5 — 0 of 120 over the
  52 ms bar**. Disclosed separately: the block-BIRTH keystroke on a cold
  drill world measured 118.2 ms once (the mint lane, not the per-keystroke
  echo; recorded, not gated).
- **G12 LIVE**: `drillAll()` PASS ×3 with the instance tier in the serve;
  the malformed drill re-ran on the durable cluster through the land's own
  `?drill=` lane (P6 kernel): rejected candidate RETAINED as latest,
  latest ≠ active, latest compiles invalid, active byte-untouched. The P1
  wound trace stands as durable immune memory; re-wear-from-inside is
  JVM-proven (g12) — the in-land re-wear surface is P7's.
- **G3 LIVE byte proof, camera-pinned, same-process**: cluster reads
  before/during/after preview of the wrap-64 candidate on `fm:text-body`
  are byte-identical (`active-sha b0910e1af590f869`, same pointer revision)
  while `previewing? true, applied? true`; `endPreview` restored to the
  same revision; camera exactly `{0,0,1}` throughout; world blocks
  byte-restored; zero blocks moved. Note: zero machine-block heights
  changed under THIS page's preview — the fallback-columns knob only
  touches replies with no source width (an honest property of the
  candidate, for the Gate-3 sitting to see).
- **T12/G7 live**: deviation + pin written through the app JVM moved the
  client-visible epoch 0 → 2; the drill page's serve carried them without
  a reload.
- **The truth loop's reversal ran live on durable state** (fixed code):
  release = `:rollback` accepted, unpin accepted, both subjects back to
  `:shared`; the deviant instance master's trail reads
  `[:rollback :activate]` — recorded, reversible, land as found.

## Verdict

**PASS.** R1 held (zero Rama edits — verified against the kernel diff);
R2 holds under the adjudicated id shape; R3/R4 hold with both pointer
generations read through one door; the preview membrane is proven at the
byte level on the live cluster; the instance tier serves, resolves,
reverses, and stays under the echo bar with deviants in the serve. The one
real defect the machinery had (F1) was caught by this gate's falsification
probe before any daily use, fixed minimally, and pinned.

**Gate 4 (immunity): CLOSED.** Wound retained as latest ≠ active on durable
state, drill re-run under the changed kernel, re-wear proven, case report
claims only declared grounds.

**Gate 3 (metabolism): AT THE STOP, awaiting Sid's word** — the
constitutional touchpoint, never self-activated. Friction: reply width;
candidate: `fm:text-body` `wrap-fallback-columns` 80 → **64** (the
implementer's proposed magnitude — the recorded number was not locatable in
the corpus). The preview receipt above is attached to the paste-able in the
board HANDOFF. His one line decides value + activation; the headed sitting
folds in `__material.picked()` + the ⌁/≈ looks (6096e85).

## Campaign state

P6 gated PASS (`be247c5` + `cff3a14`); Gate 4 closed; Gate 3 at the STOP.
P7 (the portal) becomes sendable at Sid's Gate-3 word. Ops: cluster UP
(five modules RUNNING) — left up for the headed sitting; dev app shut down
after use. Residue carried: F3 (dead client weather lane), F4 (escape
detector: per-master mapping + causal-tip comparison; durable-cluster
status is permanently `:ambiguous` until then), the text-body instance-felt
scope note, and two drill conversations minted by this gate's drives
(`p6-gate-echo-1784974904597` + one earlier, both `?drill=`-scoped).
