# ⚡⚡⚡ SPEC ROOM RAN 2026-07-08/09 — SPEC v0 EXISTS (read FIRST; supersedes the "next room: SPEC ROOM" pointer below)

The spec room (Sid + Fable-max, close iteration) produced, all committed on
this branch (`90ed435` → `e0ad1cc`):

1. **`build/sense-line-mvp/GROUNDS.md`** — the warrant: four block laws
   (Material/Boundary/Identity/Composition), break criteria (stance-flip seam
   + reaction test), parallel-reply harvest ledger, prior-art lens inventory
   (standoff/LAF-GrAF · PROV · panproto), open forks, Sid's in-flow
   ratifications, the agreed path (§11).
2. **`build/sense-line-mvp/SPEC.md`** — v0 DRAFT awaiting Sid's redline.
   Block layer NORMATIVE (surfaces/blocks/spans/forms, river-vs-debris,
   role≠actor law, free cut, demand refinement, identity-by-construction,
   context-parents, assembly/transclusion, holes first-class); marks/edges as
   interfaces; episode sketch. Real fixture from `7c80ce2a` head. AMENDED at
   contract contact: canonical-text-after-redaction (R1); UTF-16 offsets (R2).
3. **`build/sense-line-mvp/block-kernel/CONTRACT.md`** — implementation
   contract **v2** (placement re-ruled by Sid 2026-07-09, Option A): the
   block layer = `sense-block-v0` distiller adapter + driver over the
   EXISTING object-container + relation kernels (git-spine package shape;
   NO new module/depot/PStates). R1–R5 rulings; traps T1–T14; gates G1–G13;
   P0-verify (a)–(h). Codebase map (read-grounded, living):
   `docs/architecture/MAP.md`. QUEUED (Sid): code-size/verbosity audit of
   src/ (post-package). **CODE-ATOM ROUND RAN + CLOSED 2026-07-09**
   (session `code-atom-spec-round`): `code-atom/{GROUNDS,SPEC,CONTRACT}.md`
   authored + Sid-ruled in one sitting; /atomize amended to v1 (N=2);
   **work package #2 (code-atom) is OPEN — its STANDING/NOW block is
   below.** Step-3 UI fork (task UI vs baseline cards-wall) noted, Sid
   rules later.

**COUNTERSIGNED 2026-07-09 — Sid: "all agree on the specs."** SPEC v0 and
CONTRACT v1 are BINDING; the block-kernel work package is OPEN (STANDING/NOW
below). Agreed path after gates green: run the example chat through → chunks
in Rama → dual benchmark (Sid ∥ Fable, independent, then merge/reconcile —
the spec's falsifier) → dogfood reconciliation UI → then the marks/kinds
side. Kinds-round evidence pile so far: "sidetrackkkk" branch receipt;
serves/invokes family question; cross-scheme bridges (panproto lenses).
SPEC ≠ BENCHMARK stands. Non-binding architecture orientation for the
implementer: `build/sense-line-mvp/block-kernel/ARCHITECTURE.md`.

---

# Active work package #2: code-atom — clojure form atoms over the container kernel

## STANDING (frozen at package open 2026-07-09 — do not edit while active)

- **Binding docs:** `build/sense-line-mvp/code-atom/CONTRACT.md` (v1) +
  `code-atom/SPEC.md` (v0) + `build/sense-line-mvp/SPEC.md` (parent block
  laws) + `decisions.md`. This file is a baton, not a source of truth; if
  it contradicts CONTRACT.md or decisions.md, those win — flag the
  discrepancy in NOW.
- **Process:** /work-package shell + /rama phase mechanics (+ /rama-pitfalls
  before any topology-adjacent choice). ONE orchestrating session;
  validation/review layers as fresh Opus subagents; default-fail; never
  overwrite a FAIL artifact; NOW entries ≤15 lines. Artifacts in
  `build/sense-line-mvp/code-atom/`.
- **First action:** P0 parse+analysis spike per CONTRACT §12 (fresh
  subagent; artifact `P0_PARSE_SPIKE.md`; default-fail adjudication).
- **File allowlist:** NEW `src/app/server/rama/object_container/
  clojure_adapter.clj` · NEW `src/app/server/rama/code_atoms.clj` · NEW
  their two test nss · `deps.edn` ADDITIVE (P0-ruled deps) ·
  `relation_kernel.clj` `relation-kinds` ONE LINE (+`:requires :calls`,
  authorized CONTRACT §2) · package docs. Anything else = stop-clause.
  ⚠ COORDINATION: the block-kernel package holds the same registry
  authorization for 3 other kinds on the same set literal — land
  separately, rebase trivially, re-run both packages' gates after either.
- **Verification duties:** memory-derived Rama claims checked against
  `docs/reference/rama/`; deterministic microbatch barrier + physical
  PState readers for negatives (`memory/implementation-quirks.md`).
- **Definition of done:** CONTRACT §8 G1–G10 green as IPC tests in one
  suite + the documented REPL dogfood receipt (sync THIS repo; print the
  specimen census, `oc/fixed-width-order-key` callers, `relation-outcome`
  supersedes chain). G11 at gate review.
- **Stop clauses:** CONTRACT §9. **Hard rules:** env.clj never read (and
  deny-listed); code/docs separate commits; CODE commits on Sid's word;
  docs commits automatic, local branch only. Budget: local + subscription.
- Sid greenlit implementation at round close ("lets drive this home … so
  we can start the implementation") — phases proceed without further asks;
  code COMMITS still gate on his word.

## NOW (append ≤15 lines at session end)

- **2026-07-09, Fable (code-atom spec round) — ROUND CLOSED → PACKAGE
  OPENED.** Specimen hand-pass on `relation_kernel.clj` (READ full; census
  + F1–F5 in GROUNDS). Sid ruled in-flow: specimen · Fork 1 = store
  raw+atoms (transcript pattern) · comments = sense line at the code lens
  (two lanes; commentary deferred) · Fork 2 = relation kernel with NEW
  kinds `:requires`/`:calls` · "drive this home" (SPEC → CONTRACT →
  implementation). SPEC v0 + CONTRACT v1 authored same session;
  load-bearing shapes verified at source (md adapter + git_spine + rk FULL
  reads; MAP.md upgraded). Asserter correction caught at source:
  version-FREE deriver actors (git_spine:41-44; rk:441-448). T8 fired
  live: raw NULs decoded into CONTRACT.md from the Write payload; fixed to
  U+0000 prose, `file` verified text. NEXT: P0 spike (fresh Opus subagent)
  → adjudicate → P1.
- **2026-07-09 (P0) Opus subagent → Fable adjudication: PASS.** Artifact
  `code-atom/P0_PARSE_SPIKE.md` (committed fa75ba5). (a) rewrite-clj 1.1.47
  WINS — G1+G2+G3 on 96/96 files; tools.reader fails G2 on 2 `.cljc`
  (span-less top-level `#?`) + G3 structurally (drops comments). (b)
  clj-kondo 2025.06.05: G7 floor 4/4 usages + 7/7 ns deps at BASELINE;
  `:use`/`:require` indistinguishable in ns-usages (derive `:requires` from
  `:to`); **Rama 1.6.0 SHIPS clj-kondo hooks** — fabrication 193→6; P3 must
  STILL allowlist `:calls` to `app.*` (T5 concrete). (c) blob enum =
  `git log --all --full-history -m --raw --no-abbrev --no-renames -- src
  test` (895/895 == oracle; naive log silently drops 2 — flags are
  load-bearing); same pass feeds R3 lineage. Fable spot-checks all exact
  (kondo exports in jar; reassembly 51,848/32,009; 893/895 replicated).
  No stop-clauses. NEXT: P1 (adapter) dispatched, fresh Opus subagent.
- **2026-07-09 (P1) Opus subagent → Fable verification: GREEN, accepted.**
  Artifact `code-atom/PHASE_P1.md` (committed 8f32281). Built
  `clojure_adapter.clj` (cut + materialization + request builder +
  deny-list, traps cited) + test ns + pinned fixtures + deps.edn one line
  (rewrite-clj 1.1.47). Suite 7t/78a/0f — **re-run independently by the
  orchestrator, identical**; existing OC suite still green (149a). Census:
  specimen = 103 units {ns 1, def 9, fn 84, record 8, module 1}; byte-exact
  reassembly 51,848/32,009; U+0000 survives; G1–G4 all pass incl. physical
  no-dup PState scan. Hygiene grep clean (no wall clock; env.clj only as
  deny strings). One P0 prose typo corrected by measurement (electric_flow
  has 10 top-level reader-conds, not 3). Open doubts in artifact §Open
  (each with falsifier) — revisit at gate review. CODE UNCOMMITTED (gates
  on Sid). NEXT: P2 (driver + lineage lane) dispatched, fresh Opus
  subagent; :supersedes already registered — NO registry edit until P3.
- **2026-07-09 (P2) Opus subagent → Fable verification: GREEN, accepted.**
  Artifact `code-atom/PHASE_P2.md` (committed 278ff1b). Built
  `code_atoms.clj` (byte-safe git reader; B-full enum == ls-tree oracle
  895/895; deny-before-cat-file; batch ingest T9; pure `commit-lineage`;
  `code-sync!` stats-honest) + test ns. Suites 3t/78a/0f + P1 regression
  7t/78a/0f — **both re-run independently by orchestrator, identical**.
  G5: af0e0e2 = exactly 6 rk mech supersedes (pinned list), 284
  re-addressed minted ZERO (T2 held on real history). G6: the 119f3f8
  split yielded 45 hash-exact SILVER moves (34 md/5 transcript/6 identity)
  — richer than anticipated, zero false mech; within-file mech still fires.
  G9 byte-identical re-run. Hygiene grep clean (one stats-only wall clock,
  spine idiom). SPEC/CONTRACT G7 corrected 6→7 ns deps (P0-measured; my
  authoring error). CODE UNCOMMITTED. NEXT: P3 (analyzer lane + the ONE
  authorized registry edit) dispatched, fresh Opus subagent; then Fable
  gate review (full-code read + falsification) closes the package.
- **2026-07-09 (P3) Opus subagent → Fable verification: GREEN, accepted at
  implementation grade.** Artifact `code-atom/PHASE_P3.md` (committed
  d30dff1). Analyzer lane ADDITIVE in code_atoms.clj (P2 fns untouched,
  verified); registry edit minimal-diff `:requires :calls` + contract-cite
  comment (T11-clean, eyeballed by orchestrator); deps + clj-kondo
  2025.06.05. Suites independently re-run: 5t/164a/0f · 7t/78a/0f · rk
  2t/222a/0f. G7: 7 requires + 115 calls on specimen, evidence anchors
  resolve, continuant collapse (2 sites → 1 edge), T5 physical negative.
  G8 additive-proof. G10 retract-on-disappear deterministic via
  desired-override. Whole-tree desired set at HEAD: 458 requires + 5073
  calls, 3.8s, 279 skipped+counted. Deviations recorded in artifact (deps
  placement; test affordances) — gate review weighs them. CODE UNCOMMITTED.
  NEXT: adversarial falsification subagent (by class) → Fable gate review
  (full code read) → close + retro; commits on Sid's word.
- **2026-07-09 (falsification + SESSION CLOSE at Sid's budget call).**
  DIFF_FALSIFICATION.md committed (50bfb75): **6 CONFIRMED (none
  blocking) + 2 PLAUSIBLE; the dangerous classes DEFENDED** (human-edge
  custody via asserter-scoped ids + explicit filter; concurrency; clocks;
  deny-before-catfile; kernel-validation fit). All pinned gate numbers
  reproduced exact by the falsifier. Session cost check by Sid ($88;
  Fable week at 91%) → ruled: finish in a CHEAPER fresh session. **NEXT
  SESSION boots from `docs/sessions/
  code-atom-fixwave-opening-prompt-2026-07-09.md`** — line-cited fix list
  (F1 stale-edge-on-deletion reconcile basis · F2 `#N`→`~N` dedup · F3
  transition-unique reassert keys · F4-F6 stats/exit/deny honesty · P1/P2)
  → re-green all suites → gate Route A (fresh Fable, preferred
  post-reset) or Route B (without-Fable clause, doubts recorded). CODE
  still uncommitted (Sid's word); docs all committed through 50bfb75.

---

# Active work package: block-kernel — sense-line block grammar in Rama

## STANDING (frozen at package open 2026-07-09 — do not edit while active)

- **Binding docs:** `docs/current-mental-model/build/sense-line-mvp/
  block-kernel/CONTRACT.md` (**v2**, adapter shape — Sid's Option-A ruling
  2026-07-09) + `docs/current-mental-model/build/sense-line-mvp/SPEC.md`
  (v0, countersigned) + `docs/current-mental-model/decisions.md`. This file
  is a baton, not a source of truth; if it contradicts CONTRACT.md or
  decisions.md, those win — flag the discrepancy in NOW.
- **Process:** /work-package shell + /rama phase mechanics (load BOTH skills
  before anything; /rama-pitfalls before any topology-shape change). ONE
  orchestrating session; validation/review layers as fresh Opus subagents;
  default-fail verdicts; never overwrite a FAIL artifact (per-round files);
  NOW entries ≤15 lines. Phase artifacts live in
  `build/sense-line-mvp/block-kernel/`.
- **First actions:** Phase 0 requirements re-derivation (INHERITS CONTRACT §4
  mapping + §5 flow verbatim) INCLUDING ALL P0-verify items (a)–(h) of
  CONTRACT §12 — notably (e) second-distillation-over-existing-source import
  semantics (the R4 gate), (f) anchor granularity + determinism, (g)
  river-page realization, (h) delegation-chain capture point.
- **File allowlist:** NEW `src/app/server/rama/object_container/
  block_distiller.clj` (adapter + driver + pure cut fns; working name, Sid
  renames) · NEW `test/app/server/rama/object_container/
  block_distiller_test.clj` · test fixtures under `test/resources/
  block-distiller/` · package docs. `object_container.clj` touches ONLY as
  P0-ruled, ADDITIVE, citing the additive-field precedent (CONTRACT T13);
  relation_kernel.clj and everything else UNTOUCHED. Any other existing-file
  change = STOP-CLAUSE, never a silent edit.
- **Verification duties (before code):** check memory-derived Rama claims
  against `docs/reference/rama/`; test harness uses the deterministic
  microbatch-processed-count barrier (submit!==+1, routing key always
  present) + physical PState readers for negative invariants
  (`memory/implementation-quirks.md`).
- **Definition of done:** CONTRACT §8 gates G1–G11 green as IPC tests in one
  suite (G12/G13 at review) + the documented REPL/CLI invocation running the
  REAL example chat (`7c80ce2a`) end-to-end and pretty-printing the first
  river page.
- **Stop clauses:** CONTRACT §9. Classify implementer-fixable vs policy fork;
  escalate forks to decisions.md Open Questions as PROPOSED with verbatim
  citations. Never improvise policy.
- **Hard rules:** never read `src/app/server/env.clj`; code and docs in
  SEPARATE commits; CODE commits only on Sid's word; docs commits automatic
  on this local branch only, never pushed/merged.
- **Does NOT start without Sid:** mark-kind schemas · any UI · a second chat
  file · any relation-kernel amendment. Budget: local + subscription, no API
  dollars.

## NOW (append ≤15 lines at session end)

- **2026-07-09, Fable (spec room) — PACKAGE OPENED at countersign.** SPEC v0
  + CONTRACT v1 binding; pitfalls verdict applied (T11–T13). Next session:
  boot from this STANDING, load /rama + /work-package, run Phase 0 with
  P0-verify (a)–(d), then plan → validation → implement with gates-to-green.
  Nothing is built yet; the tree is clean of block-kernel code.
- **2026-07-09 (later), Fable — ⚠ PACKAGE ON HOLD at Sid's challenge. DO NOT
  BOOT the implementation session.** Sid: "block IS the atomic container of
  text" — upheld against source: object-container already runs the pattern
  (foreign-side distillers, span anchors, markdown-block-v0, unit-kinds,
  containment edges, query API). CONTRACT §2 was under-informed (anchored on
  the relation-kernel contract's characterization; OC unread — process miss,
  owned). Fork A/B/C in the CONTRACT.md banner; A recommended (block
  distiller adapter + driver over EXISTING OC + relation-kernel, git-spine
  shape). Sid rules → §2/R4/§4/§5 amended → package re-opens.
- **2026-07-09 (later still), Sid RULED: Option A ("this is what it was
  designed for"). PACKAGE RE-OPENED — BOOTABLE.** CONTRACT rewritten in
  place to v2 (sense-block-v0 distiller + driver; P0 grew to (a)–(h));
  STANDING refreshed to match (pre-phase-0, sanctioned by the ruling). Also
  ruled: no superseded-banners — working docs get direct replacement, git
  is the history (memory'd); code-size/verbosity audit QUEUED post-package;
  read-grounded codebase map started at `docs/architecture/MAP.md`.
- **2026-07-09 (Phase 0) Fable — PHASE 0 DONE + adjudicated; 2 §9 STOP-CLAUSES
  raised to Sid, both VERIFIED at source.** Artifact `block-kernel/PHASE_0.md`
  (committed 4e6986e, docs-only, clean; subagent held read-only). **SC1:**
  `relation-kinds` is a CLOSED enum (relation_kernel.clj:58-65); floor kinds
  `:grounds :assembled-from :refines` UNREGISTERED → reject at :327 → blocks
  G8/G10; fix = 3-value additive edit, Sid-gated (rk amendment). **SC2 (R4):**
  transcript sources store `source-raw-text=(pr-str payload)` per-message
  (transcript_adapter.clj:251-259), not clean per-part text → cutting breaks
  G6/§2.3; rec = driver mints per-part surfaces via `read-string` of the stored
  redacted payload (stretches R4 "no second store"). **VERIFIED GOOD (e):** import
  path accepts a 2nd distillation (empty containers + distinct import-key). Fable
  holds guardrails (routing-key=rel-id · additive delegation fields on an existing
  projection row · containment computed per SPEC §6.2›CONTRACT §4 · `:standard`@1);
  Sid micro-items (`image` §4.6 form · `attachment` class · SPEC §15 fixture stale →
  regen golden from live file). Details in PHASE_0.md §0/§4. NEXT: Sid rules
  SC1+SC2 → Plan phase.
- **2026-07-09 (Phase 0 rulings) Fable — Sid RULED both stop-clauses (recommended
  path).** SC1: register `:grounds :assembled-from :refines` in relation-kinds (the
  ONE authorized relation_kernel.clj edit). SC2: driver mints per-part
  `(event-id,part-path)` surfaces via `read-string` of the stored redacted payload
  (R4 now = no re-ingest / no second redaction). Executed as in-place CONTRACT edits
  (R4 · §4 · §9 · §12) + decisions.md ledger note. ⚠ Precedence: STANDING's
  "relation_kernel.clj UNTOUCHED" is superseded by CONTRACT §12 for exactly those 3
  kinds. NEXT: Plan phase — fresh Opus subagent authoring PLAN.md; Fable adjudicates
  + default-fail-validates on return.
- **2026-07-09 (Plan adjudicated) Fable — PLAN.md GATE-PASSED; implementation-ready.**
  Committed `dfaccbc` (subagent, docs-only). Load-bearing shapes verified at source (OC
  import validation :705-795; RK `->target-ref`/`envelope` :835-878 — N7 `:target-key`
  confirmed, routing-key auto-set). Adjudication in PLAN.md §8. **F2** (delegation home)
  resolved as Phase-1 spike + escalation trigger (spike-red → Sid authorizes an additive
  OC field + `transcript_adapter.clj` carve-out). **N3** (tool_result `:block` endpoint)
  → Phase-3b Sid-confirm; default whole-span-on-demand. 7-phase build wave in PLAN §5
  (P0 pure fns+fixture → P1 surfaces+import → P2 classify/actor → P3a kinds → P3b edges
  → P4 refine/assemble → P5 river-page+e2e). AWAITING Sid greenlight for the code wave
  (code commits gate on his word). ⓘ Sid committed a vision drawing mid-session (`08fc241`
  two-arm block/code braid) — parallel, noted, not yet engaged.
- **2026-07-09 (Phase 0 impl) Opus 4.8 — PHASE 0 GREEN (pure layer).**
  Built `block_distiller.clj` (§A ids · §B classify · §C actor · §D
  production-event · §E parts · §F free-cut with fence/quote/table atomicity ·
  §H edge specs · `distill-event`) + `block_distiller_test.clj` (10 tests /
  401 assertions / 0 fail) + `fixture.jsonl` + `golden.edn`. Fence stays atomic
  (the pre-called most-likely-red); astral U+1F600 UTF-16-honest; planted
  secret redacted at ingest → absent from every block (G6 core). **F1
  FALSIFIED by the spike:** 0/40 file-history-snapshot + 0/321 real river
  events throw on read-string — leading-slash path keys round-trip (ns→""),
  plan F1's mechanism was wrong; classify-first+try/catch kept as
  defense-in-depth (real guarded shape = whitespace keys, none in 7c80ce2a).
  Micro-deviation: image parts → surface-only (empty span would fail G11), not
  the plan's `material-part` form — SPEC §4.3-consistent. CODE UNCOMMITTED
  (gates on Sid). **NEXT: boot a FRESH session from
  `docs/sessions/block-p1-opening-prompt-2026-07-09.md`** — P1 (per-part
  surfaces + OC import); load /rama-pitfalls first; F2 delegation spike →
  possible stop-clause to Sid. P0 code should be committed for a clean
  baseline (awaiting Sid's word).
- **2026-07-09 (P1) Opus 4.8 — P1 GREEN: per-part surfaces + OC import + gates.**
  Extended `block_distiller.clj` with §G row builders + §I driver (foreign client:
  read stored sources → distill river events → OC import; no new module/PState).
  **F2 SPIKE GREEN → option A RULED IN (no stop-clause):** an extra `:production-event`
  key on a per-part `SourceArtifactRow` SURVIVES the `$$source-artifacts-by-id`
  round-trip ⇒ delegation rides the surface, ZERO existing-file edit (no
  `transcript_adapter.clj` carve-out). Gates green as IPC deftests: routing smoke
  (N1) · G3(phys) unit-kinds+spans+text==golden · G6 anchor honesty (subs==text
  ==derived-content-text, hash-eq, planted secret absent from every row) · G11(phys)
  surrogate-safe · G4 idempotence (re-run zero new units) · G5 strata (2nd
  distiller-id disjoint, v0 byte-unchanged). Suite: block_distiller_test **12/606**;
  +object_container_test **18/755**, 0 fail. Fixes at contact: import actor `:system`
  (core actor-types `#{:human :agent :system :bot}`; `:machine` is RK-only) · OC
  decision accessor `:status`/`:reason` (not `:decision/*`). Routing understood: OC
  PStates declare `{:key-partitioner partition-by-object-key}` so read+write both
  route via `extract-object-key` — id *shape* is the whole game (N1). CODE
  UNCOMMITTED (Sid's word). **NEXT: P2** — class→projection entry-kind + G1/G2
  physical gates (actor/created-by + delegation already STORED by P1's surfaces);
  prompt `docs/sessions/block-p2-opening-prompt-2026-07-09.md`.
- **2026-07-09 (P2) Opus 4.8 — P2 GREEN: classification ledger + delegation (G1/G2).**
  Stop-clause CLEARED at source: the `:transcript-conversation-projection` dispatch
  (object_container.clj:2160-2171) writes ANY entry-kind verbatim ⇒ NO `case>` edit,
  NO new `:projection-kind`. Driver now emits a class hint per river import — a
  `TranscriptConversationProjectionRow` (entry-kind `:river`, `sb:`-namespaced order-key
  so it co-tenants without colliding with the transcript `%020d:` :message rows; the F3
  read filters `:message` so it never re-ingests its own marks). **G1(phys):** 4 river
  marked; 10 retained :message rows − 4 = 6 debris (SPEC §3.2 retained-but-unmarked, a
  set-difference not a new row); actor law `created-by==resolve-actor`, tool_result→"tool".
  **G2(phys):** `:production-event` on the sidechain surface carries actor + on-behalf-of
  "a-1" (the F2 option-A home, now READ). Suite: block_distiller_test **12/625**;
  +object_container_test **18/772**, 0 fail. ADDITIVE-ONLY (no OC/RK edit). CODE
  UNCOMMITTED (Sid's word). **NEXT: P3a** — register `:grounds :assembled-from :refines`
  in `relation-kinds` (the ONE authorized rk edit, pre-approved CONTRACT §12) + accept
  micro-gate; then P3b edge floor (G8/G9, carries the N3 tool_result-endpoint Sid-confirm).
  Prompt `docs/sessions/block-p3a-opening-prompt-2026-07-09.md`.

---

# ⚡⚡ DIRECTION SHIFT 2026-07-08 — SENSE-LINE MVP (read BEFORE everything below)

The container-trail direction BROKE 2026-07-07 (Sid's granularity verdict) and
was rescoped IN PLACE the next day: **D-002 unit → sense-line units** (episodes
+ marks; container trail demotes to one evidence-lens) · **D-008 write boundary
→ machine marker writes lawfully** (worker→Rama observations). Both amendments
countersigned (Sid's blanket) — see decisions.md D-002/D-008 amendment notes +
the D-010 "REACH CLARIFIED" note (in-place operational amendments; NO
word-gating on commits; docs commits are AUTOMATIC per session — CLAUDE.md
§Docs Commits).

**New direction docs (boot order):** `docs/current-mental-model/
sense-line-model.md` (top-stratum model — ALSO the boot doc for direction
sessions, see CLAUDE.md §Session Registers) → `docs/current-mental-model/
build/sense-line-mvp/DIRECTION.md` (working map: rooms, dependency tree,
apparatus stance). **Next room: SPEC ROOM** (Sid+Fable close iteration, branch
of session `18d63935`) — the spec (block grammar · node kinds · relations;
SPEC ≠ BENCHMARK) gates the rama, benchmark, and design-fixture rooms.

The trunk-6 framework wave below is UNCHANGED as substrate work, but check
DIRECTION.md §1 (face-2 row) before dispatch. Everything through commit
`3eb76e8` is committed.

---

# ⚡ POST-TRUNK-5 RULINGS — D-010 + three frontiers (2026-07-06, Sid; recorded by the orientation session. READ FIRST)

**D-010 CLOSED (Sid verbatim in LOG):** approve-by-default — only
future-binding-cost decisions stop for Sid (operating test: "revert or
rewrite?"); everything pending at 2026-07-06 is countersigned. **BENCHMARK
PUSHED BACK** (paused; artifacts stay committed; resume = Sid's word only).
**THREE FRONTIERS ONLY: rama · ui · framework.** Sid's plain map (canonical —
prefer his words in product-facing docs): rama = internal data mapping · ui =
his sense-making surface over the raw data · framework = the glue both ways
(data→ui render; ui-interaction→data fetch).

**Applied under the blanket:** H1 **ARMED**, t0 = 2026-07-05 (BETS verdict
log; reversible on Sid's review; **KILL evaluation ~2026-07-19**).
Importer-provenance fork = builder rules with recorded alternative.
Durability fork (durable cluster vs spine-edge replay log) REMAINS SID'S —
future-binding, surface once.

**NEXT TRUNK (trunk-6): the framework wave** — dispatch from
`build/face-2/LANE_PROMPTS_DRAFT.md` (contract countersigned; 3 lanes,
duty results verified). Fold the UI legibility debt EARLY (chip noise ·
conversation display-names · fold "+N" counts · label clamping — evidence:
`build/trail-room/FIRST_LIGHT_R2.md` + Sid's screenshot); it is the ui
frontier's fastest win and the H1 window is NOW RUNNING. Slug switch-on
rides the advance sweep. Bench: do NOT resume without Sid's word.

**UI-FRONTIER SPLIT (2026-07-06, Sid in the product-side session):** the
ui frontier is TWO tracks, and "UI work" in Sid's mouth means the second.
(a) legibility REPAIR (names/folds/clamping, above) — still rides the
framework wave. (b) the TOP-DOWN DESIGN PASS — node/relation form ·
horizontal-vs-vertical orientation · open-state grammar (all-closed /
few-open / all-open) · container language (corners, color, type scale).
Honest audit on record: the current look is engineering defaults out of
R-1/R-2, never designed; the missing middle layer between canon laws and
pixels is the deliverable. Runs as its OWN session in the
principal-designer chair, zero src/ collision with the framework wave;
opening prompt: `docs/sessions/ui-design-pass-opening-prompt-2026-07-06.md`.
Feasibility envelope pre-verified (renderer has per-corner radii, borders,
gradients, shadows, per-instance color = tier-1 free; layout/open-states =
face-2 spec data = tier-2; beyond = tier-3 dream, D-001 governs build).
Framework-wave dispatch still awaits Sid's word.

---

# 🌲 TRUNK-5 CHECKPOINT — wave boundary EXECUTED + CLOSED 2026-07-06 (assertion-grade; stamped Trunk-5)

**FINAL STATE (updated at session close): the wave boundary ran END TO
END.** Commits LANDED on Sid's word (4d543a2 room · f6257a9 spine ·
8549a69 fonts · 39b93ec docs · countersign/first-light docs commit after
this edit). **R-2 FIRST LIGHT HAPPENED** — Sid booted, threads render
over the real corpus, evidence at
`vision/images/2026-07-06-r2-first-light.png` +
`build/trail-room/FIRST_LIGHT_R2.md` (2 design observations: chip noise;
raw conversation ids in reasons). **face-2 CONTRACT v1.1 COUNTERSIGNED**
(Sid's blanket, verbatim in decisions.md; Δ9 client-side stands; round-2
waived). **The face-2 BUILD WAVE is NOT STARTED** — Trunk-5 ran the §8
wave-global duties (results verified, see below) and drafted 3 builder-
lane prompts, but Sid HALTED dispatch: the wave belongs to a
product-side session. Drafts + verified duty results preserved at
`build/face-2/LANE_PROMPTS_DRAFT.md` (input, not binding). Bench resume
authorized but NOT started. Boot prompt for the next session:
`docs/sessions/where-we-at-2026-07-06.md` (orientation session's,
committed as found).

- **Suite (step 1): GREEN pre-fix** — 204 tests / 2500 assertions / 0 fail /
  0 err (all 26 nss, one JVM; baseline moved from 191/2210 with the wave's
  own gates). **Post-fix full receipt: 204 / 2505 / 1 flake** —
  `llm-adversary-probe-matrix-test` (dogfood-llm-probe, untouched by the
  wave, passed run-1, failed once in-suite WITH a Rama
  LeaderNotFoundException + once standalone, then GREEN standalone 67/67:
  a probe-await timing race, NEW instance of the documented contention
  family — known-flake registry now reads dogfood-space AND
  dogfood-llm-probe). All gate-touched nss green (39/973/0 standalone +
  in-suite).
- **Falsification (step 2): 3 fresh Opus subagents, by CLASS** (~460k
  tokens total). Artifacts: `build/trail-room/DIFF_FALSIFICATION_R2.md` ·
  `build/git-spine/DIFF_FALSIFICATION_SEAMS.md` ·
  `build/git-spine/DIFF_FALSIFICATION_CROSS.md`. Net: 1 BLOCKER
  (endpoint gap — live-probed threadless face; the trunk-ordered fix), 2
  latent logic breaks in R-2 pure code (phantom merge moves; same-ms tie
  order-dependence), 1 spine should-fix (mark-before-append), 2 honesty
  drifts (invented fixture asserters; transcript entry mis-mirror), all
  doubts carried with falsifiers.
- **Gates (step 3): ALL THREE PASS, fixes applied AT gate** —
  `build/trail-room/GATE_REVIEW_R2.md` (S1 incident-gated moves + carry
  `:edges` + merge regression test; S2 total tiebreak; fixture asserters
  nil'd + G2 live-true) · `build/git-spine/GATE_REVIEW_SEAMS.md` (endpoint
  projection in `relation-activity-entry` with dead-end nil-honest test;
  F1 mark-after-append; one branch-report healing claim corrected) ·
  `build/render-north/GATE_REVIEW_D7.md` (all claims script-verified;
  .bak gitignored; switch-on NOT taken). First-light expectation flipped:
  REAL threads over the live corpus now.
- **Retros + D-006 note (step 5): DONE** — addenda in
  `build/trail-room/RETRO.md` + `build/git-spine/RETRO.md`; wave note in
  decisions.md D-006 notes (incl. the STANDING PROPOSAL: mechanical
  fixture-key-diff gate for the face-2 contract — the drift class fired
  two waves running).
- **COMMITS PREPARED, AWAITING SID'S WORD (hard rule):** (1) room —
  threads.cljc(NEW) + cards/scene/editor_compute/combined_text/agent_flow/
  mouse + trail_face_test + feed.edn; (2) spine — git_spine + trail_view +
  ingest_watchers + object_container + markdown_adapter + both test nss;
  (3) substrate — 5 font assets + src-build/fonts/(NEW); (4) docs commit
  separate (gates, falsifications, retros, D-006 note, this baton).
- **SITTING AGENDA (step 6):** R-2 first light (threads expected now) +
  eyeball list (2 pre-answered: symlink dangler root-caused+self-heals;
  stats split+dedup) · H1 arming (HIS hand) · face-2 COUNTERSIGN (flag Δ9
  deviation + the §2.4 move-rule conjunct added at gate + band-line
  wording) · POLICY FORK incremental-jsonl (now also carrying the
  record-serialization durability doubt) · importer provenance on source
  rows (nil-honest today; fixture invention killed at gate) · bench
  rankings INCOMPLETE (sweep paused, batch 7+; fresh grading session to
  resume).
- Bench branch stays PAUSED per Sid's stop; Trunk-4's agent handles are
  dead.

---

# 🌲 TRUNK-4 MARATHON — all-fronts wave (updated 2026-07-06 mid-session by Trunk-4; supersedes the DO-list in the opening block below — its LAND STATE section remains the verified baseline)

**Authorization (Sid, in-session, Trunk-4 chat):** maximalist blanket — "i want
you to be unrealistic and we need to get done soooo much every fucking thing we
can think of"; all 4 fronts start now; **Codex 5.5 xhigh fast admitted to the
builder pool**; all-models bench sweep ordered ("run all the models ... that way
you will have a much better understanding of what to use underneath" — verbatim
in `vision/LOG.md` 2026-07-06). Strategy ruled with Sid: offense batched,
defense at TWO boundaries only — (a) wave-end falsification-by-CLASS + Fable
gate, (b) next-wave contract validation PIPELINED during the current wave's
code (text artifacts, zero collision). Gate tests are OFFENSE: builders write
each contract gate as a deftest in the same batch as its code.

**Structure:** ONE trunk (this lineage) owns baton · gates · the ONE serial
suite · falsification dispatch · commits · and authors the **FACE-2 contract**
(text, in-trunk, parallel to branches; countersign at Sid's sitting AFTER R-2
first light — the sitting evidence rides in). Branches = disjoint file fences,
ns-level tests only, NO commits, NO baton writes; each closes with an
assertion-grade report at its named path. Paste-ready prompts (TRUNK prompt
first, then the four branches): `docs/sessions/t4-branch-prompts.md`. Naming:
branches stamp `Trunk-4 / t4-<fence>`; sessions renamed to their role.
**Trunk handover:** Trunk-4 EXECUTION runs in a fresh session booted from the
trunk-4 prompt in that file; the orientation session (where-we-at-softland)
authored this block and stands down as trunk at that boot — one live trunk at
a time.

| Branch | Fence | Job | Report |
|---|---|---|---|
| **t4-room** | trail_face/* + CONTRACT_R2 allowlist §5 (rect_tree UNTOUCHED = stop-clause) | THE R-2 WAVE per CONTRACT_R2.md v1.1 (countersigned) | `build/trail-room/BRANCH_REPORT_R2.md` |
| **t4-spine** | server only: git_spine.clj · trail_view.clj feed assembly · ingest_watchers.clj + tests | claimed-ms (ADDITIVE) · dangling-doc-edge diagnosis · [GIT-SPINE] stats verify · incremental-jsonl stretch · :workers-2 smoke | `build/git-spine/BRANCH_REPORT_SEAMS.md` |
| **t4-substrate** | font/atlas/slug pipeline + renderer glyph tables ONLY | Δ7 slug glyph expansion (DELTA-B1; feasibility-with-exact-blocker is a valid deliverable) | `build/render-north/BRANCH_REPORT_D7.md` |
| **t4-bench** | runs/ · snapshots/ · tools/model-uxr · ccr config; NO product src | LM-1b autopsy + nemotron rerun · new arms at PINNED 127376b0 (sonnet-5 · haiku-4.5 · codex-5.5-xhigh-fast via ccr) — pre-register arms first; budget ruling stands: NO API dollars | `runs/<run>/BRANCH_REPORT_SWEEP.md` |

**Coordination flags:** claimed-ms is additive — t4-room renders the band-2
two-clock stamp with honest nil until t4-spine lands it; any file collision =
STOP + report (marathon collision protocol); zero-work-agent glitch fix =
resume with an explicit first action.

**Wave boundary (trunk only):** branch reports in → ONE serial suite (baseline
191/2210, 1 known dogfood-space contention flake) → ONE batched falsification
by CLASS → Fable gate → per-package CODE commits (docs separate) → light retro
+ D-006 note.

**Sid's sitting bundle (one sitting, mid-marathon):** R-2 first light + his
eyeball list (kraft labels · dangling edges · [GIT-SPINE] stats · idle
content-same? ratio) · H1 arming signature (HIS hand; candidate preserved) ·
face-2 contract countersign · intake rules (LM thresholds + pool routing, with
bench rankings in hand). Then: the face-2 wave — Δ1/Δ2 birth laws, Δ3 store,
camera; camera going live lawfully fires Δ13/Δ14; Δ3+RAF evidence fires Δ15.

**TRUNK-4 CHECKPOINT (2026-07-06 — SESSION CLOSED at ~350k, stood down on
Sid's call; the TRUNK-5 BOOT block below this checkpoint is the live
order. This checkpoint is Trunk-4's assertion-grade record. Stamped
Trunk-4):**
- **FACE-2 CONTRACT v1 AUTHORED** → `build/face-2/CONTRACT.md` @ 719a9d9
  (PROPOSED; birth laws B1–B5 head it; Δ1–Δ4/Δ6/Δ9–Δ11 in scope, Δ5/Δ8
  subsumed/fired; Δ12/Δ15/Δ16 pre-staged cascade §10; 20-trap ledger; gates
  G1–G19 + G-FL + G-Δ; countersign = Sid's sitting AFTER R-2 first light).
  file(1) text-verified. Authoring catch: PRIMITIVES' `!zoom-factor` cite
  (`global_flow.cljs:59` ⓘ) is STALE — no such atom at 719a9d9 (trap 19).
- **Branch-dispatch correction:** the four branches were never separate
  sessions — dispatched 2026-07-06 from THIS trunk as background subagents
  (verbatim prompts, /rename stripped; t4-room on Fable, rest Opus; ids held
  in-trunk). Zero-work glitch fired on spine/substrate/bench at launch;
  all three resumed with explicit first actions (the documented fix). t4-room
  never glitched.
- **CONTRACT validation R1 RAN: FAIL (2B/2S/3A) → ALL FOLDED into v1.1**
  (artifact kept verbatim). B1: Δ13's text-clip retirement lives in
  `rect_tree/tree->text-ops` → ONE lawful named rect_tree edit added to §6.
  B2: Δ9's server-side move killed on D-001 + R-2 §2.1 (binding form-break
  condition) + allowlist unreachability (trail-view mirrors OC only;
  relations client-composed) → Δ9 = client-side named projection registry
  this wave; Rama placement pre-staged §10. **Honest-ledger: this deviates
  from the standing order's "Δ9 in Rama" — Sid may override at
  countersign.** S1: chrome camera "separate buffer" was FALSE
  (clone-text-system shares buffer+bind-group; dead guard) → new buffer +
  bind-group + G8b + trap 21. Face-2 now touches ZERO server files.
  Round-2 = Sid's call at countersign. 4th consecutive Fable contract with
  real text errors killed by fresh-context validation (D-006 tally).
- **BRANCH REPORT IN — t4-substrate Δ7: BUILT, falsifier PASS**
  (`build/render-north/BRANCH_REPORT_D7.md`; fence verified clean). Slug
  95→591 glyphs via the in-repo generator (`src-build/build/slug_font.clj`
  — the GATE_REVIEW_B2 "missing toolchain" existed all along); zero
  renderer changes; switch-on = one manifest flip, Sid's call. ⚠ slug
  advance = 0.602051 (DejaVu) vs the ~30 hardcoded 0.56 (Ubuntu) sites —
  the flip must ride with an advance sweep (logged for G17/sitting). ⟳
  U+27F3 is a real DejaVu gap; ↻ covered.
- **BRANCH REPORT IN — t4-spine: ALL FOUR SEAMS CLOSED**
  (`build/git-spine/BRANCH_REPORT_SEAMS.md`; ns suites 7/167 + 13/295 +
  7/44 all green; no commits). Seam 1: claimed-ms via NEW explicit request
  key `:claimed/at-ms` + additive `claimed-at-ms` on
  SourceIngestCompletionRow (declared fence carve-out — kernel row
  additive-field; gate scrutinizes) — commits two-clock, md nil-honest.
  Seam 2: dangling doubt was REAL — `/home/sid/projects/Softland` is a
  SYMLINK to `/mnt/data/...`; 1 dishonest dangler live (1,089 exposure on
  symlink-cwd boot); `canonical-under-roots` rebase fix; live dangler
  self-heals next boot. Seam 3: TWO stats drifts fixed (converged re-runs
  printed as fresh → fresh/converged/rejected/unresolved split; per-file
  session dedup over-counted edges ~300× → run-level dedup). Seam 4:
  `:workers 2` smoke PASS (queue item closed); incremental-jsonl
  deliberately NOT built — instance-scoped cursor makes cross-boot skip
  impossible; honest routes (durable cluster OR durable spine-edge replay
  log) = POLICY-GRADE DECISION FOR THE SITTING. 6 doubts w/ falsifiers in
  the report. object_container.clj + markdown_adapter.clj touches =
  declared carve-outs, falsification hunts them.
- **t4-bench STOPPED on Sid's word (2026-07-06, mid-grading).** Preserved
  on disk, uncommitted: pre-registration (RUNBOOK/MANIFEST), raw
  completions for ALL four new arms (sonnet-5 · haiku-4.5 · codex-via-ccr
  · nemotron-16kdry rerun), partial grades for three arms. NO
  BRANCH_REPORT_SWEEP.md — the sweep is PAUSED, not closed; resumable
  (trunk holds the agent handle) or a fresh session can grade from the
  raw dirs. Intake-sitting rankings are incomplete until resumed.
  FULL STOP executed on Sid's word — in three moves, one lesson: (1)
  t4-bench agent killed; (2) Nemotron llama-server (8091) killed,
  GPU/RAM freed; (3) LESSON: killing a parent agent does NOT cascade to
  its child graders — a haiku grader survived and ran batches until Sid
  stopped it from the panel (~14:08 last grade write). Verified quiet:
  no fresh grade files, no benchmark OS processes. Restart cost when
  resumed: reload the gguf into llama-server (~a minute), then grade
  from the raw dirs (grades through ~batch-6/P7 already on disk).
- **BRANCH REPORT IN — t4-room: R-2 BUILT END TO END, no stop-clauses**
  (`build/trail-room/BRANCH_REPORT_R2.md`; 9 files in-fence; NEW
  `threads.cljc`; G1–G10 PASS as deftests; ns 28/680/0 incl. R-1 +
  view-mvp gates + live fixture-fidelity AFTER spine's changes; shadow
  :dev 0 warnings; gates caught 3 real bugs in-batch). The three
  unvalidated v1.1 additions HELD at implementation contact. 6 doubts
  in the report.
- **Trunk-4 STOOD DOWN at ~350k (Sid's call, 2026-07-06) — the wave
  boundary is TRUNK-5's. All four fences are CLOSED; the tree is the
  wave's uncommitted output.**

---

# 🌲 TRUNK-5 BOOT — run the wave boundary (authored by Trunk-4 at stand-down)

**You are Trunk-5.** One live trunk; you own the suite, falsification
dispatch, gates, commits (Sid's word), and the sitting. Branch reports on
disk: `build/trail-room/BRANCH_REPORT_R2.md` ·
`build/git-spine/BRANCH_REPORT_SEAMS.md` ·
`build/render-north/BRANCH_REPORT_D7.md` (bench = PAUSED, see above).
Standing: bypassPermissions is ON (project settings.local.json; deny
backstops on `git push` + `env.clj` read — never lift them). Docs only on
this branch, never pushed; code/docs separate commits.

**VERIFIED FACTS (Trunk-4 verified on disk — do not re-derive):**
- **Endpoint gap is REAL:** `trail_view.clj` `relation-activity-entry`
  emits `:entry/detail {:kind :status :relation-id}` — NO `:from`/`:to`
  (fixtures carry them; live doesn't). R-2 §7.4's "(it does today)" was a
  CONTRACT-TEXT ERROR → honest-ledger note (criterion-3 style) + additive
  gate-fix: detail gains from/to {:id :kind} from the activity row —
  FIRST verify the R3 activity row carries to-id/to-kind (grep
  relation_kernel.clj); then t4-room's lanes see real edges over the live
  corpus; re-run trail-face + trail-view nss. Without this fix R-2 first
  light is an honest all-band face (t4-room's top doubt).
- Suite invocation: `clojure -M:test -e "(require '[clojure.test :as t]
  '<ns…>) (t/run-tests '<ns…>)"` — ONE JVM, all test nss serial.
  Baseline at wave open 191/2210; it MOVES with the wave's new gates
  (never hardcode — R-2 G11). Known flake: dogfood-space contention
  under live dev-server load (standalone green).
- Slug advance: new slug set is uniform 0.602051 (DejaVu) vs ~30
  hardcoded 0.56 (Ubuntu) sites — switch-on flip must ride an advance
  sweep (face-2 wave, Sid's call; G17).

**DO, IN ORDER (wave boundary, delivery mode):**
1. **ONE serial suite** (command above; expect green given branch ns
   receipts; failures → diagnose before falsification).
2. **ONE batched falsification by CLASS** — 3 fresh Opus subagents
   (state count + token shape first): (a) t4-room diff — classes:
   unclipped text ops, phantom moves, per-band bounds-vs-paint,
   watched-input feedback (trap 13), order-as-data (G7), fold
   precedence edges, + the report's 6 doubts; (b) t4-spine diff —
   classes: fence carve-out custody (`object_container.clj` additive
   row field, `markdown_adapter.clj` canonicalization), claimed-at-ms
   replay determinism (no wall-clock in topology), fresh/converged
   split truth, run-level dedup (did it swing to UNDER-count?),
   `canonical-under-roots` edge cases (out-of-root, nested symlinks),
   + 6 doubts; (c) cross-package — the fixture-vs-live drift CLASS
   (endpoint gap = instance 1; hunt siblings), claimed-ms end-to-end
   (commit two-clock vs md nil-honest), band-2 stamp rendering.
3. **Fable gates, per package, code read IN FULL:**
   `GATE_REVIEW_R2.md` (trail-room) · `GATE_REVIEW_SEAMS.md`
   (git-spine) · `GATE_REVIEW_D7.md` (render-north, light — asset
   regen; check `.pre-d7.bak` hygiene + the 95-glyph byte-identity
   claim). Apply fixes AT gate (wave-1 precedent), incl. the endpoint
   additive fix; re-run touched nss; CLAUDE.md falsification protocol
   sections in each artifact.
4. **Per-package CODE commits on Sid's word** (room · spine · substrate
   assets; docs separate, this branch only).
5. **Light retros + ONE D-006 wave note**: face-2 validation R1 kill
   record (4th consecutive Fable contract with real text errors caught
   — B1 fence-collision, B2 imagined-demand+unreachable, S1 false GPU
   citation); delivery mode held for a 4-branch parallel wave; process
   lessons → quirks (already written: zero-work glitch ×3, grader
   no-cascade).
6. **SID'S SITTING**: R-2 first light over the real corpus (post
   endpoint-fix) + eyeball list — 2 items PRE-ANSWERED, verify live
   (dangling doc edge root-caused: /home symlink → canonical rebase,
   self-heals on boot; [GIT-SPINE] stats: fresh/converged split +
   run-level dedup) · kraft legibility + idle content-same? ratio
   (still open) · H1 arming signature (HIS hand;
   vision/images/2026-07-05-h1-arming-candidate.png) · **face-2
   COUNTERSIGN** (`build/face-2/CONTRACT.md` v1.1 — flag the Δ9
   deviation: client-side registry this wave, NOT "in Rama" as the
   standing order said, killed by validation on D-001 + R-2 §2.1 +
   allowlist unreachability; Sid may override; round-2 validation =
   his call) · POLICY FORK (from t4-spine): incremental-jsonl needs
   durable cluster OR durable spine-edge replay log — Sid rules ·
   intake rules: bench rankings INCOMPLETE (sweep paused; resume =
   llama-server reload + grade from raw dirs, batch 7+ outstanding;
   Trunk-4's agent handles are dead — fresh grading session if
   resumed).
7. Then: **the face-2 wave** per CONTRACT v1.1, on countersign.

---

# 🌲 TRUNK-4 OPENING (authored 2026-07-06 by the Trunk-1 checkup; supersedes nothing below — it CONSOLIDATES the block beneath it)

**Naming convention, adopted:** the orchestrating lineage = TRUNK sessions
(one live trunk at a time; the trunk owns the baton, gates, and commits);
tracks/windows = BRANCHES (disjoint file fences, decisions-as-text back to
the trunk). Trunk-1 = 07-05 marathon (spine · room · write path · Electric
skill · benchmark, across 4 branches). Trunk-2 = 07-05/06 delivery (gate +
6+1 fixes + code commits + Sid's first boot + R-2 countersign). Trunk-3 =
probe (LM-1 live run, nemotron vs opus). **You are Trunk-4.** Stamp it in
your baton entries.

**LAND STATE (verified against disk + history at c12b63c + checkup
commits):** five packages CLOSED (relation-kernel · trail-view WP1 ·
view-mvp WP-B2, ratified on MEASUREMENT_RAF [RAF] 7–12ms PASS · git-spine
WP2 · trail-room R-1). Suite baseline 191/2210 (dogfood-space contention
flake known; standalone green). App boots; real commit threads render with
kraft edges; write path live (`POST /api/relation/assert`; asserter-id +
keyword type required; `:git-commit` targets rejected). **D-009 RECORDED:
Fork 2 = one substrate** (Sid's verbatim in LOG; FORK-2.md full record).
DELTA-B1.md = the render sequencing spine (Δ1/Δ2 birth-laws at store
birth; Δ3 promotion at the FACE-2 contract; Δ7 slug before face 2 ships
design language). LM-1 ran: nemotron 17/32 vs opus-cc 26/32; dominant
local failure = never-committing (4096-token thinking budget,
config-repairable); KILL clause (a) formally met; interaction term
unmeasured until relations/ ablation differs from A0. H1 arming CANDIDATE
preserved (`vision/images/2026-07-05-h1-arming-candidate.png`) — **awaits
SID'S HAND in the BETS verdict log**; surface once, don't push.

**DO, IN ORDER (SUPERSEDED 2026-07-06 by the MARATHON block above — R-2 is now
branch t4-room's job under trunk gate; items 2–3 fold into the sitting bundle):**
1. **THE R-2 BUILD WAVE** — this session's job.
   `build/trail-room/CONTRACT_R2.md` v1.1 COUNTERSIGNED-BINDING (round-2
   waived — "implementation forward"). Delivery mode (D-006 e2): Fable
   codes directly. Batch ALL coding (bands · lanes-from-edges + the
   unthreaded BAND · move chips · fixture extensions per G3's note) → ONE
   serial test batch → ONE batched falsification (hunt by CLASS) + Fable
   gate (`GATE_REVIEW_R2.md`) → per-package code commits → light retro +
   D-006 note. Allowlist §5 validator-corrected; rect_tree UNTOUCHED =
   stop-clause. The three unvalidated v1.1 additions (fold-precedence ·
   thread-identity-member · geometry text) validate by implementation
   contact — wrong at build = STOP-CLAUSE → amend + honest-ledger note.
   Probe obligations bind: order rides ROWS AS DATA, never incseq
   permutations; C2-shaped scene-store consumer (PROBE-10K).
   **Do-not-preclude:** implement lane assignment as a PURE, address-keyed
   transform with rank fields as data — Δ9 moves layout projection into
   Rama (named, plural lenses) at face-2; keep that migration a lift, not
   a rewrite.
2. **R-2 first light with Sid**, folding his open eyeball list: kraft
   label legibility · dangling doc edges (dual working-dir doubt) ·
   `[GIT-SPINE]` server stats · idle `content-same?` skip ratio.
3. **Sid's parked items — surface, don't push:** H1 arming entry (his
   hand) · counterfactual-probe weighing (final D-006 eval) · intake
   sitting (local-models note + LM-1 observations + MEMORY-off-land
   boundary) · work-package skill amendment AFTER its adversarial
   retro-recheck.

**RULES:** never read `src/app/server/env.clj` · code and docs in SEPARATE
commits · docs only on this local branch, never pushed/merged · ns-level
tests while coding, ONE serial suite per wave · one builder per file if
subagents are dispatched · CLAUDE.md is slimmed by design (verified
Electric laws live in the electric-docs skill, guarded by
`test/app/missionary_claims_test.clj`).

---

# ⚡ BATON — closed 2026-07-06 (~500k session). NEXT SESSION = THE R-2 BUILD WAVE, CODE FIRST.

**START HERE, exact order (delivery mode, D-006 e2 — Fable codes directly):**
1. **R-2 BUILD WAVE, straight to code** per
   `build/trail-room/CONTRACT_R2.md` v1.1 (**COUNTERSIGNED 2026-07-06,
   binding; round-2 validation WAIVED by Sid — "implementation forward";
   the three unvalidated v1.1 additions are validated by implementation
   contact, so if fold-precedence / thread-identity-member / geometry text
   proves wrong at build, STOP-CLAUSE → amend, honest-ledger note**).
   Wave shape: coding batched (bands · lanes-from-edges + band · move
   chips · fixture extensions per G3's note) → ONE serial test batch → ONE
   batched falsification (hunt by CLASS) + Fable gate (`GATE_REVIEW_R2.md`)
   → per-package code commits → light retro + D-006 note. Allowlist §5 is
   validator-corrected (state.cljs/agent_flow.cljs own view-state;
   rect_tree UNTOUCHED = stop-clause). Suite baseline at HEAD: 191 tests /
   2210 assertions (1 known contention flake, dogfood-space under live
   dev-server load — ns-standalone green 26/208/0).
2. ~~Parallel: LM-1 grading~~ **DONE 2026-07-06 in the probe session —
   64 rows graded + aggregated → `runs/127376b0/RESULTS.md`** (nemotron
   17/32 vs opus-cc 26/32; details in the superseded baton's item 5 +
   intake §6.4). Remaining on that track: Sid's human audit (12 J + 3
   invented rows) · LM-1b failure autopsy · intake sitting reads/rules.
3. **Sid's own list**: H1 arming entry in BETS (HIS hand; candidate
   `vision/images/2026-07-05-h1-arming-candidate.png`) · 5-min eyeball on
   the fixed render (kraft label legibility · dangling-edge count ·
   `[GIT-SPINE]` server stats · idle `content-same?` skip ratio —
   MEASUREMENT_RAF open items, feed R-2 first light) · counterfactual-probe
   weighing (final D-006 eval) · intake sitting (local-models note).
   Queued process item: work-package skill amendment from wave-1 lessons,
   AFTER an adversarial retro-recheck (the skill's own rule).

**STATE AT CLOSE (2026-07-06):** git-spine WP2 + trail-room R-1 + view-mvp
WP-B2 all CLOSED (WP-B2 ratified on `build/view-mvp/MEASUREMENT_RAF.md`).
R-2 v1.1 countersigned-binding, round-2 waived. Cross-boot cursor bug fixed
(cfdb3d8); first-light defects fixed (637d293: sidebar-in-trail-face +
edge-direction "commit produced itself"). Criterion-2 probe RUN (leans
bet-holds, one ding — Sid weighs; artifacts in build/relation-kernel/).
All this session's work committed; the probe session's LM-1 material
(runner.clj / subjects.edn modified + runs/ + snapshots/ untracked) is
THAT session's to commit, on Sid's word. Hard rules stand: env.clj never
read; code/docs separate commits; docs branch never pushed.

---

# (superseded) BATON — 2026-07-05 delivery session

**DONE THIS SESSION (delivery mode, Fable coded directly):**
**git-spine WP2 + trail-room R-1 GATE-PASSED, FIXED, COMMITTED, CLOSED.**
- Batched gate review `build/git-spine/GATE_REVIEW.md`: all 6 falsification
  should-fixes CONFIRMED + FIXED at gate (route custody validation ·
  deterministic `assert:<relation-id>` idempotency with optional override ·
  `:git-commit` dropped from route allowlist (B1 non-join) · per-line replay
  + per-file extractor isolation · UTF-8 pins · reader rebuilds from ALL
  stored keys). PLUS one gate-found defect (kraft-label overflow, fixed) and
  one root cause (rect_tree `clip?` REPLACED the ancestor clip → now
  INTERSECTS; expansion is clip? again; behavior-identical at every nil-clip
  production call site).
- Suite: **191 tests / 2202 assertions / 0 fail / 0 err** (one JVM, serial).
- CODE COMMITS (this branch; cherry-pick to main later): eac6dd5 spine P1P2 ·
  ed2db8a route PW · a1fda21 names P3+G11 · dc743d6 render R-1+clip fix ·
  7827f6f missionary regression ns + H11/H12 docstrings · 57b0113 chore
  strays (+/data/ gitignored).
- Light retros: `build/git-spine/RETRO.md`, `build/trail-room/RETRO.md`;
  D-006 wave-close note appended to decisions.md.
- AMENDMENTS Tier-2 H1–H10+H13 APPLIED (insights.md ×8, progressive-summary
  ×2, quirks ×2 + core-reframes (memory), reactive_master paths). **A4
  APPLIED on Sid's word (same session)** — CLAUDE.md slimmed to
  skill-pointers; Electric-3-Limitations absorbed (L13); disk-only edit,
  reversal path in AMENDMENTS.md.
- Hygiene: `_map.md` STALE banner; codex_implementation →
  `docs/history/codex-implementation-session7-audit.md`; strays committed.

**NEXT (in order):**
1. **BOOT HAPPENED (2026-07-05 late).** Gate-15 evidence COLLECTED →
   `build/view-mvp/MEASUREMENT_RAF.md` ([RAF] 7–12ms at ~1.1k instances on
   near-4K = PASS; feel verdict recorded verbatim — the staircase
   illegibility indicts the family-key lane interim, i.e. R-2's cure, not
   the render machinery). **WP-B2 CLOSE proposed on that artifact — Sid
   ratifies.** **H1 ARMING CANDIDATE preserved:**
   `vision/images/2026-07-05-h1-arming-candidate.png` (expanded commit
   2232fbb showing `-> based-on … by import:git-spine` over the real repo —
   qualifies under the H1 ruling's "fact of a typed relation rendering";
   Sid ratifies in the BETS verdict log). TWO first-light defects FIXED at
   the sitting (637d293): sidebar-ambient-in-trail-face (W-1 closed early) +
   incoming-edges-printed-as-outgoing ("commit produced itself"). Write path
   live: `POST /api/relation/assert` (curl EDN body; asserter-id + keyword
   asserter-type REQUIRED; `:git-commit` targets rejected).
   STILL TO EYEBALL on the fixed render: dangling doc edges (dual
   working-dir, GATE_REVIEW doubt 1) · kraft label legibility · the
   `[GIT-SPINE]` server stat lines (replay/sync/extract counts) · idle
   `content-same?` skip ratio (MEASUREMENT_RAF open item).
2. **R-2 contract at v1.1 — PROPOSED, awaiting Sid's countersign**
   (`build/trail-room/CONTRACT_R2.md`). v1 was VALIDATED same session:
   fresh-context round returned **FAIL (2 blockers B1/B2 — real Fable text
   errors: impossible cross-band bounds gate; incoherent order-as-data
   gate — + S1–S7 + A1–A4)**, all folded into v1.1
   (`CONTRACT_R2_VALIDATION_R1.md` kept verbatim). Second validation round =
   Sid's call at countersign. Build does NOT start without countersign.
3. **Sid-AFK window also delivered:**
   - **Cross-boot cursor bug FOUND + FIXED** (would have hit Sid's SECOND
     boot: ephemeral IPC × durable cursor → transcript edges silently lost;
     cursor now cluster-instance-scoped via `:spine-run-id`; GATE_REVIEW.md
     ADDENDUM + quirks entry; new test block in g5-g6-g7).
   - **D-006 criterion-2 counterfactual probe RUN** (open since 07-03):
     blind Opus over the §13 manifest → PROPOSED scoring leans BET HOLDS
     with one honest ding (placement matched; probe reproduced the F1
     envelope trap uncorrected). `build/relation-kernel/
     COUNTERFACTUAL_PROBE.md` + rival contract verbatim; decisions.md D-006
     note appended. Sid weighs.
4. Sid's remaining picks: **countersign R-2 (± order validation R2)** ·
   **intake sitting** (`intake/2026-07-05-local-models.md` exists; Sid
   touched `vision/LOG.md` — committed e534f36) · ~~benchmark live runs
   (blocked on boot)~~ **RAN — see 5.**
5. **LM-1 SUBJECT PHASE RAN 2026-07-06 (probe-standup session; freeze gate
   closed in-session: bank v1 + sha 127376b0 pinned + Sid's budget ruling =
   NO API dollars, frontier on subscription).** Both arms 32/32, 0 errors:
   nemotron-q8-128k local (RAW: **8/32 empty visible completions** — thinking
   ate the 4096-token budget) vs `opus-4.8-cc-harness` subagents
   (`runs/127376b0/cc-raw/`). Deviations D1–D5 in the snapshot MANIFEST
   (A3 deferred ≡A0; qwen dropped — b8262 caps slot ctx at n_ctx_train).
   **GRADED + AGGREGATED same session → `runs/127376b0/RESULTS.md`:
   nemotron 17/32 vs opus-cc 26/32 correct; nemotron 9 never-committed +
   3 inventions vs 0/0; J-honesty 0/6 vs 1/6 (both flunked — audit queue).
   KILL clause (a) formally met; interaction term unmeasured (A3 deferred).
   NEXT: human audit (12 J + 3 invented + 15% sample) · LM-1b failure
   autopsy (intake §6.4; reasoning channels on disk) · sitting reads.**
   Queued behind it: ccr
   symmetric-harness instrument (installed, unconfigured; Sid floated it —
   needs own pre-registration). Endpoint census + LM-3 corpus census + all
   measurements: intake §6. runner.clj/subjects.edn modified (tracked,
   uncommitted); runs/ + snapshots/ untracked. Commits on Sid's word.

**Old baton (2026-07-05 HQ marathon) kept below for step-3 detail; its
statuses are superseded by the above.**

---

# (superseded) SESSION-CLOSE BATON — 2026-07-05 HQ marathon

**CLOSED/BUILT TODAY:** WP1 **CLOSED** (light retro `build/trail-view/RETRO.md`).
WP-B2 gate-PASSED — closes on gate-15 evidence (Sid's app boot). **H1 clock
RULED: NOT started at first light; arming event = first render showing ≥1
typed relation over real material** (BETS.md verdict log; reversible on Sid's
review). Trail-room **R-1 BUILT** (rim v0 · addresses off cards · kraft marks;
gates green ns-level, `build/trail-room/PHASE_R1.md`). **git-spine WP2 BUILT**:
P1+P2 adapter/parents/extractor/replay (5t/113a green, `PHASE_P1P2.md`), PW
/assert route (4t/30a green; POST `/api/relation/assert`), P3 display-names
(G9 green; view-mvp fixture gate STRENGTHENED, `PHASE_P3.md`). **Electric
skill REBUILT from verified evidence** (14 platform-real / 3 agent-error / 3
unverified; 9-test regression ns `test/app/missionary_claims_test.clj`;
skill signed by Fable; `build/electric-skill/{HACKS-LEDGER,VERDICTS,
PROBE-EVIDENCE,AMENDMENTS}.md`). **Model-UXR benchmark v0 BUILT**
(`build/model-uxr/` — 32 questions, ablations A0–A4, dry-run harness;
6 join questions spine-gated). **CLAUDE.md A1–A3 APPLIED** under Sid's
in-session blanket (m/ap ban re-scoped · atomic-settle fenced · RAF sunset;
AMENDMENTS.md tier-1 note; revert-to-reverse). Sitting-3 queue captured
(`design/claude/sitting-3-queue-2026-07-05.md`; Roam mirror pending —
`docs/sessions/roam-pending-2026-07-05-sitting-3-queue.md`, bridge was down).

**INTEGRATION STATE:** serial **G10 GREEN — 187 tests / 2168 assertions /
0 fail / 0 err** (one JVM; both concurrent-run flakes proven contention
artifacts, not isolation bugs). Reviewer gates AUTHORED per WP1-gate
precedent: **G8 pair** (`test/app/server/rama/git_spine_gate_test.clj` —
route-written log line replays into a FRESH cluster with identical
relation-id) + **G11** (appended to `trail_view_test.clj` —
commit+session in one View-3 context). **BOTH GREEN AT CLOSE: G8 pair
PASSED (the §3.C serialization-drift risk is dead, empirically). G11 first
ran RED — a REAL catch: View-3's `edge-line` dropped the FAR endpoint, so
the projection silently lost WHO produced a target (map-must-not-lie
violation). FIXED in `trail_view.clj` `edge-line` (full triple
from → kind → to; this is one more uncommitted change in that file);
trail-view namespace re-run 4 tests / 118 assertions / 0 failures
including all pre-existing gates.**
**FALSIFICATION: LANDED + COMMITTED** (`build/git-spine/
DIFF_FALSIFICATION_R1.md`, 17:59). **Verdict: NO BLOCKING findings — all
gated happy paths real and tested.** SHOULD-FIXES = next session's first
coding batch (delivery mode, Fable codes directly): (1) route: asserter-id
UNVALIDATED → garbage gets a lying 200 AND poisons the assert-log; (2)
route: curl RETRY after timeout mints fresh ids → duplicate event rows;
(3) route allowlist accepts `:git-commit` targets that don't join (dangling
by design now — either drop from allowlist or document); (4) spine: replay
per-line and extractor per-file lack try/catch — one bad line/file kills
the whole pass; (5) render: removing the outer expansion `clip?` stripped
card-width text clipping from info/relations/holes/omissions sections
(only material got the `:material-clip` replacement) — uncaught by tests;
(6) reader field-pinning + charset DOUBTs (see artifact). Fix all six →
re-run the touched namespaces → ONE serial suite → gate artifact → commits.

**UNCOMMITTED CODE in the working tree** (commit ONLY after gate review;
per-package, code-only commits, never mixed with .md):
git_spine.clj + git_spine_test.clj (P1P2) · server_jetty.clj +
relation_assert_route_test.clj (PW) · trail_view.clj + trail_view_test.clj
(P3 + reviewer G11) · trail_face/{cards,scene}.cljc, workspace_actions.cljs,
combined_text.cljs, trail_face_test.clj + feed.edn (R-1 + P3 fixture) ·
missionary_claims_test.clj (verify) · git_spine_gate_test.clj (reviewer) ·
ingest_watchers.clj + file_viewer.cljc cfg (P1; NOTE: file lives at
`src/app/file_viewer.cljc`, not client/workspace/). bench/ + tools/model-uxr
are new uncommitted dirs (code-tier).

**NEXT SESSION, exact order:**
0. **MODE (Sid's close-of-session ruling — decisions.md D-006 notes,
   verbatim there): delivery-first. Fable codes directly; coding tasks
   batch FIRST, then ONE serial test batch, then ONE batched
   falsification+gate per wave — never sprinkled per-phase. CLAUDE.md
   slimming to skill-pointers is drafted as AMENDMENTS A4 (apply after
   Sid glances at wording).**
1. Falsification artifact + gate-test result → **Fable GATE REVIEW**
   (artifact `build/git-spine/GATE_REVIEW.md`; CLAUDE.md falsification
   protocol) → apply should-fixes → per-package CODE commits (fold
   AMENDMENTS H11/H12 code-docstring fixes into the matching commits) →
   close WP2 + R-1 with light retros; D-006 notes entry.
2. Apply AMENDMENTS.md Tier-2 H1–H10 + H13 (exact old→new pairs; re-grep
   anchors — line numbers drift).
3. **SID'S APP BOOT — the finish line:** `clj -A:dev -X dev/-main` →
   `/trail timeline`. First boot runs replay→spine-sync→extract (ingests the
   repo's commits + parent edges; expect a long first sweep — watch server
   log). Collect gate-15: `[RAF]` lines + feel verdict + one
   screenshot→re-resolve → write `MEASUREMENT_RAF.md` → **WP-B2 CLOSED**.
   **Threads render → screenshot the first kraft connector = the H1 ARMING
   RECORD** (BETS verdict-log arming event). Write path is live:
   `POST /api/relation/assert`.
4. Hygiene: baton queue item 1 below is STALE (that package closed 07-05);
   `docs/_map.md` needs a STALE banner (model-uxr SPEC §7 finding); strays
   triage (codex_implementation, scripts/, src-build/, manifest, atlas .bak,
   bench/, tools/, data/ ignore-or-keep).
5. Sid's pick thereafter: **R-2 contract** (binding probe obligations
   pre-recorded: order-as-data + C2-shaped consumer — PROBE-10K:15,153-155)
   · **intake sitting** (candidates: local-model sovereignty — see
   `intake/2026-07-05-local-models.md` if Sid's window produced it;
   behind-the-scenes distillation; MEMORY-off-land boundary — model-uxr
   SPEC §7) · benchmark live runs (needs threads + load claude-api skill
   before enabling stubs) · D-006 criterion-2 counterfactual probe.

**SESSION LESSONS (quirks-grade):** zero-work agent glitch fired ×2 (fresh
background agent returns boilerplate with 0 tool calls; fix = resume with an
explicit first action). Electric SNAPSHOT resolved TWO different jars the
same day (-44 render probe vs -45 claims tests — PROBE-10K:24; Missionary is
transitive+unpinned). **Test policy RULED by Sid:** ns-level tests during
phases; ONE serial full suite at integration (encode in future contracts).
Sid's time-box blanket ("adopt recommended, note alternatives") was
SESSION-SCOPED — it does NOT carry into the next session.

---

# Active work package: trail-view WP1 (Track A) — data layer

## STANDING (frozen at package open, 2026-07-04 — do not edit while active)

- **Binding docs:** `docs/current-mental-model/build/trail-view/CONTRACT.md`
  (WP1 contract v1 — **countersigned by Sid 2026-07-04 in-session**; binding)
  and `docs/current-mental-model/decisions.md`. This file is
  a baton, not a source of truth; if it contradicts CONTRACT.md or
  decisions.md, those win — flag the discrepancy in NOW.
- **Process:** one phase per FRESH session per
  `.claude/skills/work-package/SKILL.md` + `/rama` mechanics. Phase order:
  A1 custody → A2 stance kinds → A3 activity+R3 (relation-kernel amendments,
  gates 9/10/15/16) → B trail-view module + wrappers + text projection
  (remaining gates). Fresh-context validation, default-fail verdicts, never
  overwrite a FAIL artifact (per-round files).
- **File allowlist:** NEW `src/app/server/rama/trail_view.clj`,
  `test/.../trail_view_test.clj`, fixture resources. AMENDED
  `src/app/server/rama/relation_kernel.clj` ONLY per CONTRACT §5 (registry
  set; envelope `:request/sent-at-ms`; RelationDecisionRow/RelationEventRow/
  RelationEdgeRow custody fields; accepted-branch activity write; R3 query)
  + its test ns (additive, plus arity-forced mechanical updates). NOTHING
  else; object-container module untouched.
- **Verification duties (before code):** mirror PStates/queries from query
  topologies (`docs/reference/rama/18-module-dependencies.md`,
  `13-query-topologies.md:203-207`; fallback architecture CONTRACT §7);
  activity-write replay determinism (no wall clock in topology — OC's
  `decided-at-ms` divergence is the anti-pattern); harness launch order for
  module dependencies.
- **Definition of done:** CONTRACT §11 gates 1–16 green in one suite run,
  incl. gate 16 (pre-existing 165 assertions green).
- **Stop clause:** contract unbuildable / wrong under platform semantics /
  binding-doc conflict → classify implementer-fixable vs policy fork;
  escalate forks to decisions.md Open Questions as PROPOSED with verbatim
  citations. Never improvise policy.
- **Hard rules:** never read `src/app/server/env.clj`; code and docs in
  separate commits; commits only on Sid's word; docs only on this local
  branch, never pushed/merged.
- **Does NOT start without Sid:** WP2 (D-003 spine); any commit.
  (The contract-countersign guard that stood here was satisfied 2026-07-04
  in-session, before any phase ran — see NOW.)

## NOW (append ≤15 lines at session end; history → build/trail-view/SESSIONS.md)

- **2026-07-05, Fable — baton COMPACTED, process AMENDED.** All prior NOW
  entries moved verbatim to `build/trail-view/SESSIONS.md`. Ruling with Sid
  (decisions.md D-006 notes 2026-07-05): fresh context ≠ fresh session — QC
  layers run as fresh Opus subagents inside ONE orchestrating session;
  routing tables are advisory on model/effort, never binding on session
  structure; NOW entries cap ~15 lines. STANDING's "one phase per FRESH
  session" line is superseded (precedence: decisions.md wins; STANDING left
  unedited per protocol).
- **State:** Phase A COMPLETE (A1 custody, A2 stance kinds, A3 activity+R3);
  gates 9, 10-kernel-half, 15, 16 green; suite 2 tests / 222 assertions / 0
  failures; code + docs UNCOMMITTED (commits on Sid's word). Phase B
  (trail_view.clj + test ns + fixtures; remaining gates 1–8, 11–14) IN
  FLIGHT this session (Opus subagent), then batched A1+A2+A3+B
  diff-falsification (fresh subagent), then Fable gate review — same session.
- **⚠ PHASE B OWNERSHIP (corrected 2026-07-05 ~01:10):** Phase B belongs to
  the LIVE parallel Track-A session (actively writing trail_view.clj + test,
  ~50s save cadence, verified by md5 at 01:03–01:05 — client-composition
  path, mirror routing + N3 pin verified correct in snapshot). The Fable
  session's builders STOOD DOWN with ZERO writes (two dispatch attempts,
  both halted by collision protocol). The Fable session owns what follows
  green: independent suite re-run → batched A1+A2+A3+B diff-falsification
  (fresh subagent) → Fable gate review. Gate question parked: module took
  client-composition while PLAN adopted single-roundtrip (F-8 spike WORKS)
  — §7 says shape/honesty gate, latency doesn't; Fable rules at gate.
- **Anchors (GREP — kernel line numbers shifted 3×, never trust PLAN cites):**
  R3 query `"relation-activity"` `[bucket-lo bucket-hi]` (fixed-width bucket
  strings); PState `$$relation-activity-by-bucket`; wrappers
  `rk/read-relation-activity` (public) + `rk/read-activity-rows` (V1);
  mirror-PState reads route ONLY via `(|hash$$ $$mirror *k)` — plain `|hash`
  silently mis-routes; advisory N3: pin the C7 container→source-ref join read
  path; gate 12 revises specs by RE-INGEST (new SourceVersionRow), never edit.

- **2026-07-05, Fable (marathon session, Track-B-origin) — PHASE B GREEN;
  ownership clarified; falsification CLAIMED.** The "live parallel session"
  writing trail_view.clj was THIS session's fresh-context Opus subagent —
  cross-track work Sid authorized in-session ("Everything, one marathon":
  B2-P5 is contract-gated on WP1 green, so this session built Phase B to
  unblock it). Receipt: 16/16 WP1 gates green; trail-view suite 445
  assertions; cross-package run 28 tests/909/0. Path = CONTRACT §7
  client-composition fallback (wrapper shapes identical; §7 says latency is
  not a gate). PLATFORM FINDING: Rama 1.6.0 mirror local-select> REJECTS
  `:allow-yield?` — §11's allow-yield rule cannot bind mirror reads (quirks
  updated). **CLAIM: the batched A1+A2+A3+B diff-falsification is RUNNING in
  THIS session** (fresh Opus, launched ~01:20) — do not double-run.
  PROPOSED SPLIT: the parallel Track-A Fable session takes the Track-A GATE
  REVIEW (fresh eyes on a build this session orchestrated — strictly better
  QC; its parked client-composition-vs-PLAN question rules there); this
  session gates Track B. D-006 note ec69e74 is canonical — no second note.
  Docs/code uncommitted (Sid's word).

- **2026-07-05, marathon — FALSIFICATION R1 COMPLETE; A1+A2+A3 review debt
  DISCHARGED (kernel verdict: SOUND); Phase B: NO blockers.** Artifact:
  `build/trail-view/DIFF_FALSIFICATION_R1.md` (probes-held ledger + 7
  first-light notes; the 1 should-fix was in a Track-B file, applied).
  Track-A GATE REVIEW remains with the parallel Fable session per the
  split above — its inputs are that artifact + the Phase-B receipt.

- **2026-07-05, Fable (Track-A session) — WP1 GATE REVIEW: PASS. Package
  gate-complete.** Artifact: `build/trail-view/GATE_REVIEW.md`. Code read in
  full; suite independently re-run twice (final **3 tests / 302 assertions /
  0 failures** = 222 A-phase + 74 Phase B + 6 reviewer-added). Gap found AT
  gate: gate 10's trail-view half (custody projection) had no executed
  assertion — closed by a reviewer-authored TEST-ONLY block (bundle
  `:written-by` + "via" text badge + convergent absence), green first run.
  Client-composition ruled CONTRACT-SANCTIONED (§7 fallback; letter-ding/
  spirit-held in the artifact). Traps 3/4b/10/12 spot-checked, HELD. 8
  non-blocking open doubts with falsifiers in the artifact (top: `:written-by`
  present-with-nil vs "omitted"; text-projection arrow direction; feed
  address round-trip untested). decisions.md D-006 note appended. **NEXT:
  commit + package close on Sid's word (code and docs separate); light retro
  can fold into close. WP-B2 P5+ is unblocked (WP1 gates green + gated).**

---

# Active work package: view-MVP WP-B2 (Track B) — the pixels

## STANDING (frozen at package open, 2026-07-04 — do not edit while active)

- **Binding docs:** `docs/current-mental-model/build/view-mvp/CONTRACT.md`
  (v1 — **countersigned by Sid 2026-07-04 in-session**; BINDING),
  `build/trail-view/CONTRACT.md` (WP1, binding — its §7 wrappers are this
  package's ONLY data surface), and `decisions.md`. This file is a baton,
  not a source of truth; if it contradicts CONTRACT.md or decisions.md,
  those win — flag the discrepancy in NOW.
- **Process:** one phase per FRESH session per
  `.claude/skills/work-package/SKILL.md`. Phase order: B2-P0 spec
  re-derivation → P1 plan → P2 plan validation (default-fail, per-round
  artifacts) → P3 pure core + fixtures + gates 1–13 (parallel-safe with WP1
  impl) → P4 gates green + substrate amendments → P5 integration (**waits
  for WP1 gates green**) → P6 watchers + first light (H1 clock) → P7/P8
  validations → Fable gate. NOT the /rama skill (no Rama modules touched);
  CLAUDE.md Missionary/Electric patterns + implementation-quirks ARE in
  force.
- **File allowlist:** CONTRACT §12 verbatim — NEW `src/app/client/
  workspace/trail_face/*` (cljc + wiring.cljs), `src/app/server/
  ingest_watchers.clj`, tests + fixtures, atlas regen; RENAMED
  `rect_tree.cljs → .cljc` (+T-4 clamp only); AMENDED (mode-branch/wiring
  additions only): combined_text, editor_compute, scroll, mouse,
  workspace_actions, render, runtime, state, agent_flow, file_viewer,
  electric_flow, util_fns (epoch atom only), renderer.cljs (two shape fns
  only). trail.cljs / dg_flow / sidebar / settings / cmd_panel untouched.
- **Verification duties before code (P1):** import entry points + their
  observable completion (read runtime.clj/adapters, not memory); client
  cljc compiles under `clojure -M:test` (spike one require); wheel-cascade
  order in scroll.cljs; shadow-cljs resolves the cljc rename.
- **Definition of done:** CONTRACT §11 gates 1–17 green + first-light
  artifact (gate 15 evidence) recorded.
- **Stop clause:** unbuildable / platform-wrong / binding-doc conflict →
  classify implementer-fixable vs policy fork; escalate forks to
  decisions.md Open Questions as PROPOSED with verbatim citations.
  Pre-flagged: WP1 shape amendments → fixtures re-derive
  (implementer-fixable); cljc-on-JVM classpath failure → policy fork
  (placement ruling 2.1 is load-bearing), escalate.
- **Hard rules:** never read `src/app/server/env.clj`; code and docs in
  separate commits; commits only on Sid's word; docs only on this local
  branch, never pushed/merged.
- **Does NOT start without Sid:** P5+ waits on WP1 gates green; any commit.
  (The contract-countersign guard that stood here was satisfied 2026-07-04
  in-session, before any phase ran — see NOW.)

## NOW (append ≤15 lines at session end; history → build/view-mvp/SESSIONS.md)

- **2026-07-05, Fable — baton COMPACTED** (same ruling as Track A; all prior
  NOW entries verbatim at `build/view-mvp/SESSIONS.md`).
- **State:** P0 (IMPLICIT_SPEC + CONTRACT v1.1) + P1 (PLAN.md) + P2
  (PLAN_VALIDATION_R1 = PASS; advisories A1–A8; open doubts D1–D2) COMPLETE.
  No code written yet. Docs uncommitted (Sid's word).
- **NEXT: B2-P3** — pure core + fixtures + gates 1–13, parallel-safe with WP1
  impl; runs under the amended process (fresh subagents inside one
  orchestrating session). Fold at write time: A1 (gate-1 round-trip is pure
  EDN, no live resolve-address), A5 (`:panes` cases are a HARD must — no
  default in the case), A6 (corrected paths), A2/A3/A4/A7/A8 at their named
  sites. P5+ WAITS on WP1 gates green. Cross-package: OI-1 (no production OC
  runtime handle — shared with WP1, Sid/WP1 coordination at P5); OI-2
  (manifest default font vs regenerated MSDF atlas — Sid decision at P4).

- **2026-07-05, Fable (marathon; Sid collapsed the shell in-session) —
  P3+P4+P5+P6-server COMPLETE; gates 1–14+16 green, 17 mechanically green.**
  Pure core (5 cljc + wiring.cljs) + ASCII fixtures (\uXXXX salts) +
  rect_tree.cljc clamp; gates 1–13 as 13 deftests. Atlas = MERGED
  Ubuntu+DejaVu MSDF (Ubuntu lacks ALL arrows/shapes/star/warn — 340 cps;
  uniform 0.56 advance; flat schema preserved); OP-27 shaper fix landed;
  manifest default → merged MSDF per **Sid's OI-2 ruling: MSDF is TEMPORARY
  verification, SLUG stays the destination** (slug glyph-set expansion =
  named follow-up). OP-35 two-part done; watchers + gate 14 via subagent
  (decision-latch, no polling, loop survives poisoned file); 7-step wiring +
  Electric bridge (Trail* e/defns; trail-rt = defonce-delay IPC boot — OI-1
  first-light risk, named). Gate-16 fidelity test CAUGHT real drift
  (verdicts = full edge rows; :omission/kind; no :display-name; anchors
  +:block-path) → fixtures + cards re-derived same session. Suite 28/909/0;
  shadow :dev compiles (prod build pre-broken: prod.cljc wants Electric v2).
  REMAINING: falsification verdict → fixes → Track-B gate HERE; first light
  with Sid (gate 15 artifact + H1 clock).

- **2026-07-05, marathon — TRACK-B GATE REVIEW: PASS, conditional on first
  light.** Artifact: `build/view-mvp/GATE_REVIEW_B2.md` (gate ledger, S1–S5,
  first-light risk register — read it before driving). Falsification
  should-fix applied (scene cache now value-equality, not hash) + shadow
  recompiled green; falsification artifact at
  `../trail-view/DIFF_FALSIFICATION_R1.md`. Gate 15 + visual halves DEFER to
  first light. **NEXT: first light with Sid** — `/trail timeline` over real
  material; expect a seconds-long stall on the FIRST pull (in-process IPC
  boot — risk register item 1); record FIRST_LIGHT.md + MEASUREMENT_RAF.md;
  H1 clock starts. Slug glyph-set expansion = named follow-up (OI-2).

---

# Cross-track (compressed 2026-07-05 — verbatim history at `docs/sessions/session-log.md`)

- **SESSION SCOPING LAW (Sid, 2026-07-04): one session = ONE track, never
  mix.** Declare the track at start; read its Roam page (graph `softland`) +
  this baton. Session end: baton entry (≤15 lines) + Roam track-log block.
- **Track A — Rama (serial):** WP1 live above → then WP2 D-003 spine (needs
  Sid's go; key adjudication queued: durable asserted edge vs
  projection-time query for transcript↔commit joins, + exactness-flag
  semantics).
- **Track B — render substrate:** WP-B2 live above. The D×B1 delta step
  waits on Track D's synthesis (inputs:
  `build/render-substrate-retro/{RETRO,PRIMITIVES}.md`).
- **Track C — read→write design (Sid live):** sitting 1 DONE 07-04/05 —
  face-test law (trail always on screen · every node reads at
  title/decision/2-liner · open-in-place while others hold), 27-item
  do-not-preclude ledger (the ONLY phase-1-binding output), horizon + dummy
  artifacts in `design/claude/`. Roam batch 3 pending re-proposal
  (`docs/sessions/roam-pending-2026-07-05-track-c.md`). Queue on its Roam
  page.
- **Track C sitting 2 DONE 2026-07-05 (Fable, designer lens).** Rulings
  R1–R7 in `design/claude/room-card-lane-2026-07-05.md` (all PROPOSED,
  awaiting Sid on sight); drivable sketch `trail-room-sketch-2026-07-05.html`
  (altitude toggle, open-in-place, kraft edges, the band — real repo
  history); 3 decision-log entries appended. Gist: room = ground + rim only
  (HQ's full-screen toggle CONFIRMED w/ constraints C1–C3); cards =
  typography closed / surface open, 4 bands, address off the card face
  (answers F-L5); lane = asserted thread over lineage kinds, unthreaded =
  self-declaring BAND, family-key demotes to fold rule (answers F-L2).
  **FOR HQ:** the handoff block "what the render contract must build next"
  (7 falsifiable items, rulings doc end) — rec order: rim+address first
  (kills worst F-L5 on sight), kraft marks before H1 arming screenshot,
  lanes-from-edges after tonight's git-spine edges land. Docs uncommitted —
  HQ coordinates commits. Roam: batch 3 landed+approved; HQ proposes the
  sitting-2 session-log block separately — Track C stacked no Roam writes.
- **Track D — render north: STUDY DONE 2026-07-05** (Fable + 9 Opus sweeps:
  3 code/artifact readers, 6 web researchers, all primary-source-verified).
  Deliverables: `build/render-north/INPUTS.md` (evidence manifest — never
  re-gather) + `NORTH.md` (the framework north) + `MECHANICS.md` (Sid-
  commissioned same session: the browser-services-rebuilt layer — hit-test/
  stacking/scale/clip/text/gestures/animation/wire/degradation, each with
  mechanism+today+north+traps; NEW wall-grade finding **N6: f32-absolute
  coords jitter at land scale — scene store must hold f64, GPU gets
  camera-relative f32**, verified as-built at renderer.cljs:7,41,1107)
  + `HARD-PROBLEMS.md` (after Sid's "Zed bled — is that it?" challenge: the
  bottomless five — diff-transducing spec COMPILER not interpreter (H1/H2),
  frame pacing + epoch rule, the text pit and where the mono moat ends,
  optimistic write overlay, transclusion identity `(view,addr)→slot` — with
  3 explicit corrections to NORTH/MECHANICS; birth-critical pair = store
  keying (H6) + coordinate discipline (N6)). **DECIDED (direction-grade,
  non-binding on build):** camera-over-addressed-world ACCEPTED w/ two
  amendments (islands lay out in local 2D then project — never zoom-aware
  flexbox; root noun = ADDRESS, not camera); **view-specs are ASSERTIONS in
  the land** (Rerun-Blueprint-on-Rama) — versioning/collab/on-the-fly inherit
  from the log; two code layers only (pipeline + render-type/action
  registries, D-001-paced); actions become data (closes the rect-tree closure
  hole); LWW for layout only, epistemic state accumulates (law N5).
  **VERIFIED:** Electric 3 diff = incseq 6-op protocol, diffs-only on wire;
  K1 proofs real (MapLibre expressions, rfw, deck.gl/json, Blueprint, MCP
  Apps); NO rebuild-grade divergence — NORTH §8 embryo table (every north
  noun has a living as-built seed, incl. dormant `gpu-mount` incseq bridge in
  `buffer_pool.cljs`). **DOUBTED:** Electric at 10⁴ deltas (Gap 3) — now a
  pre-registered evidence item (NORTH §9: synthetic 10⁴ incseq → scene store
  → `[RAF]` instrument, at face-gate time); either outcome keeps the model.
  Fork 2 left OPEN (founder's) with rec: one substrate, three projection
  families over one address space. **NEXT:** Sid reads NORTH.md; Fork-2
  sitting at his call; D×B1 delta instrument now has its Track-D input.
- **Track D — PROBE-10K MEASURED 2026-07-05** (render-bench session; NORTH §9
  pre-registered evidence item DISCHARGED). Artifact:
  `build/render-north/PROBE-10K.md` (setup, tables, knee, threats). Headline:
  the knee is diff SHAPE, not element count. Change/append/tail-shrink hold at
  10⁴ end to end (server mint 2–5ms, apply ≤4.2ms p95, clean 60fps, idle O(1))
  — Gap 3's conditional-RAF change signal is real on Electric's own protocol.
  Reorder/front-drop/ID-churn diffs knee at 10³ (mint ~0.1s) and die at 10⁴
  (12–74s) — inside `->seq-differ` (the e/diff-by path), upstream of wire AND
  consumer (a 10⁴ permutation applies in 6.5ms via order-indirection).
  Fallback (client re-diff) holds to 10³; flat 17–30ms/frame at 10⁴ (30fps).
  Obligations recorded: order/window = data on rows, never incseq permutations;
  store consumes the six ops directly — as-built gpu-mount bridge CORRUPTS
  under :permutation (demo: 16,750 slots for 100 entities). Model kept; no
  rebuild candidate. NEW `bench/src/render_probe/*` (4 files) uncommitted; no
  product code touched. Feeds the D×B1 delta instrument + face-gate 15.
- **Local-model sovereignty — intake + recon DONE 2026-07-05 (Fable; Sid
  engaged same-day in-chat).** Deliverable:
  `intake/2026-07-05-local-models.md` (Candidates-sitting pre-read;
  BETS.md/decisions.md untouched). Three D-007 drafts: **LM-1**
  structure-substitutes-for-scale (the H2 link — model-uxr's
  pre-registered small×ablation headline; probe = fill the existing
  `nemotron-local` subjects.edn slot); **LM-2** low-wrongness slots go
  local (multi-arm blind A/B on band-2/display-name enrichment); **LM-3**
  own-material QLoRA (gated: saturation probe-0 + `asserted-by=sid`
  census). Sid same-day (verbatim in-file; LOG landing due at sitting):
  the unit is a POOL routed model×slot (Gemma-class for low-reasoning
  slots); NO closed-provider distillation ever — open-legal only (DeepSeek
  MIT/Qwen3 Apache clean; Gemma ≤3 distills propagate its license; custody
  D-008 §5.1 = the clean-corpus filter). Recon: Nemotron fits 48GB only
  quantized; 16-bit LoRA 60GB → dense-32B tune path. 3 frontier Qs in-file.
- **Local-model PROBE STANDUP DONE 2026-07-05 (Fable delivery session;
  detail = intake §6 addendum).** Sid's two HQ follow-ups landed verbatim in
  vision/LOG.md. **Box census corrects the recon: 2× AMD RX 7900 XTX 24GB
  (gfx1100, ROCm 6.3.2) — NOT NVIDIA; no FP8; intake §4 Q4 CLOSED.** Endpoint
  = llama.cpp llama-server (ROCm b8262); models were ALREADY at
  ~/projects/models (221GB): Nemotron-3-Nano Q8 smoked **100 tok/s gen /
  4553 tok/s prefill**, Qwen3-32B Q8 smoked 21 tok/s — neutral prompts only,
  SPEC §6 held. subjects.edn WRITTEN (quant+context in ids; per-subject
  ports; runner dry-run green, bank-hash printable). **LM-3 gate-2 census:
  ~783k Sid-typed tokens all-projects / ~419k Softland-only (+13k LOG) —
  same order as the ~1M floor, gate does not kill; number in intake §1.**
  **LM-1 probe NOT run — freeze gate unmet (no bank-v1 declaration, no sha
  pin, no budget confirm).** Open: gemma3-27b pull (~17GB, Sid's word);
  QLoRA-on-2×24GB-ROCm feasibility UNVERIFIED (LM-3 path); reclaim: stale
  ollama store (99GB, root disk) + /mnt/data/models/gguf partials (3.7GB).
- **Standing cross-track pointers (from Track C, phase-1-binding):**
  Track A ← ledger 13 (altitude text forms are data: title·2-liner·full per
  bundle), 16 (mark/question counts survive folds), 17 (walk/attestation
  rows assertable). Track B ← design glyphs ⊢│├└• vs 95-ASCII atlases
  (extensible glyph pipeline before face 2) + wall W4 (face 2 =
  camera-model, never DOM-outliner). Electric-at-land-scale (Gap 3, 10⁴+
  elements) = THE powder question before face 2 scales.

## Queue (Sid sequences; per decisions.md Fable-window queue)

1. **Trail-view data contract** (next Fable-window item) — the contract +
   acceptance gates for D-002's first form, consuming the relation kernel via
   its two query topologies. Open it per the new `/work-package` skill, in a
   FRESH session. **INPUT MANIFEST: `build/trail-view/INPUTS.md`** (gathered
   2026-07-03/04: Sid-decided items incl. bundle=everything-layered,
   staleness-renders-differently, minimal-token agent legibility,
   view-specs-as-data; carried relation-kernel obligations; Fable-proposed
   verdict-capture + select/type/sign gesture). Prior trail-view/design docs
   are input NOT authority (Sid 2026-07-04: "the prev docs were written by
   lower reasoning fables") — ground truth is decisions.md + code + INPUTS.md.
2. **D-003 Regime-1 spine** (needs Sid's go): transcript→commit/doc join
   extractor + git-commit-metadata adapter emitting `:produced`/`:based-on`
   assertions with evidence anchors, feeding the 27-04 trail view.
3. Slot-anywhere, cheap-model: D-006 criterion-2 counterfactual probe (fresh
   Opus, input manifest = CONTRACT §13); `:workers 2` smoke test of the
   relation kernel; make the test-harness task count injectable (not only
   `rand-nth`-randomized) whenever the harness is next touched, so partition
   sweeps are reproducible.

Added 2026-07-03 (bet-foundry session, same day):

4. **D-007 awaits Sid's countersign** (decisions.md): the bet foundry —
   claims→bets intake via the BETS.md Candidates inbox + the Fable questioning
   practice (1–3 frontier questions per vision/bets-touching Fable session).
   Sid's verbatim proposal + HCI thesis landed in `vision/LOG.md` (2026-07-03
   entry).
5. **First BETS.md intake sitting** (Fable-grade, Sid live): seed the empty
   Candidates inbox. **PRE-READ: `docs/current-mental-model/intake/
   2026-07-04-painting.md`** — the full typed decomposition of the 07-03/04
   sessions (12 threads, parallelism map, carried questions). Sources:
   `vision/LOG.md` (HCI thesis, economy vision, April–June research notes)
   plus the wall panels. Status of the three posed frontier questions:
   Q1 (beachhead) ANSWERED — H3 re-sequenced in BETS.md 2026-07-04; Q3 (unit
   that leaves the chatbox) ANSWERED — "everything, raw + native + view-forward"
   (feeds the trail-view contract); Q2 (minimum viable discipline for a solo
   operator) still OPEN. New question posed 2026-07-04: what is the
   commit-gesture for thought (what mints a semantic version of an
   exploration)? — feeds the trail-view contract directly.
6. **Vision back-fill** — PROGRESSED 2026-07-04: April 28 / June 6 / June 11 /
   June 12 raw notes + the ~June 28 notebook pages landed in `vision/LOG.md`
   (photos preserved at `vision/images/2026-06-28-notebook-p{1,2}.png`).
   More may remain in Sid's notes apps; pull in under original dates when he
   supplies them.

Added 2026-07-04 (deep-thinking thread):

7. **Question-unit design** (Fable window; Sid: "lets make it a dedicated
   thing... have a solution for it"): the atomic unit of a question —
   hole-shaped relation rows (`? based-on B`), announcement-at-pattern-match,
   frontier = high-density holes. Sequencing: the trail-view contract only
   keeps the door open (INPUTS.md item 5); the dedicated design slot comes
   after/alongside that contract, build paced by D-005 (the view orders it).
   **D-001 evidence acquired 2026-07-04**: the decisions.md open-questions
   wall broke against Sid in daily use (verbatim in LOG; requirement recorded
   in INPUTS.md item 5) — this item now has a real form-break behind it, not
   an imagined demand. Interim: open questions go to Sid as Roam cards.
8. **Model-UXR benchmark design** ("user research for models", H3 machine
   half — BETS.md H3 2026-07-04 sharpening): frozen land snapshot +
   orientation-question bank (gold answers derivable only from the trail) +
   permission-scoped ablations (drop decisions.md / baton / relations, measure
   degradation). Metrics: tokens-to-orientation, wrong-authority rate,
   re-derivation ratio, join-question success, invented-structure rate.
   Cheap-model buildable once the trail view's queries exist; reruns as CI.
9. **Interim working protocol** (proposed by Fable 2026-07-04 after the
   form-break thread; zero-build relief, effective unless Sid objects):
   (a) thread-scoped sessions, not 400k monoliths — one painting-thread per
   session, Fable carries orientation via this baton + the intake pre-reads;
   (b) Sid engages decisions/questions ONLY via Roam cards — markdown stays
   the machine-facing record, maintained by models; (c) every session ends
   with an assertion-grade baton entry (the Q2 rule — **ADOPTED by Sid
   2026-07-04** via Roam card "yes adopt this"; also in decisions.md and
   Claude memory). Face order also RULED 2026-07-04 on Sid's delegation:
   View 3 → threaded/DAG timeline → canvas (decisions.md Open Questions).

Docs tracking (Sid, 2026-07-03): full docs tree tracked on this branch
(`84b3d82`), including `decisions.md`; docs commits ONLY on this local branch,
never pushed, never merged into main; code and docs always in separate
commits.

Session hygiene for whoever writes next: `memory/implementation-quirks.md` →
"NUL bytes in source" (the tool-JSON NUL-escape trap fired twice MORE in the
recheck session, both times while writing docs ABOUT the escape — file(1)-check
anything that mentions it) and "Microbatch test barrier" (reuse for every
kernel suite).
