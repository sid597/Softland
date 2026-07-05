# Trail-view WP1 (Track A) — per-session log, verbatim archive

Moved verbatim from `docs/sessions/next-prompt.md` NOW on 2026-07-05
(baton compaction per the D-006 process ruling of the same date: the baton
holds "now"; this file holds session history. If a future phase genuinely
needs more than the baton's ~15-line cap, the overflow goes HERE.)

- **2026-07-04, Fable — Track A session 1: WP1 contract OPENED.** Read
  INPUTS.md (14 items), decisions.md, relation-kernel CONTRACT + full
  `relation_kernel.clj`, object-container records/helpers/PStates/queries +
  ops module, Rama mirror/query references, BETS H1–H3, LOG 2026-07-04
  verbatims; one Opus Explore agent inventoried the ingest substrate.
  Artifacts: **`build/trail-view/CONTRACT.md` v1** (bundle/feed/trail/address
  shapes; verdicts = 3 stance kinds in the relation registry; activity
  projection + R3 in the relation kernel; read-only trail-view-module with
  mirrors only; 13-trap ledger; 16 gates; phased handoff);
  **object-kernel-revision RULED** (decisions.md: entirely gated as
  authority; two zero-cost disciplines adopted on Regime-1 grounds —
  two-clock naming, custody-vs-assertion recording); **D-008 PROPOSED**
  (decisions.md entry + Roam card). Verified facts the next session needs:
  envelope actor NOT persisted today (custody amendment is real, lands
  FIRST); md anchors CHAR offsets vs transcript BYTE offsets; OC decision
  rows stamp wall clock (`object_container.clj:428,448,467`) — activity
  write must not copy that; no cross-land recency index exists (feed =
  registry scans + R3). Answer to Track-B RETRO §6.5 doubt: WP1 demands NO
  sub-paragraph per-glyph provenance — anchors are span-level (char/byte
  ranges), text projection is plain text; T-1 does not grow. Judgment calls
  for countersign: CONTRACT §14 (9 items). NEXT: on countersign → Phase A1
  (custody) in a fresh implementer session; on objection → amend contract
  first. Track-B B2 is now unblocked on the contract side (needs Sid's
  countersign to be binding).

- **2026-07-04, Fable — same session, later: COUNTERSIGNED, package LIVE.**
  Sid countersigned in-session ("Countersigned as yes"): **D-008 CLOSED** in
  decisions.md; **CONTRACT.md v1 now BINDING** (header updated). All four
  Roam batches were approved (cards, track log, daily pointer live in the
  graph); a countersign-record block proposed under the track page.
  STANDING's pre-countersign guard line edited to reflect satisfaction —
  done as package-open completion, before any phase ran (trace note left in
  STANDING). **Phase 0 launched** (fresh-context spec re-derivation, Opus
  subagent, per Sid's standing lower-model instruction): deliverable
  `build/trail-view/IMPLICIT_SPEC.md` + findings classified
  implementer-fixable vs policy-fork; subagent barred from baton writes
  (one writer — parent session logs on its completion). Next after Phase 0:
  plan phase (fresh session/subagent), then default-fail plan validation.

- **2026-07-04, Fable — same session: Phase 0 COMPLETE, F-1 RULED, CONTRACT
  v1.1, plan phase LAUNCHED.** Phase-0 receipt (Opus, artifact
  `build/trail-view/IMPLICIT_SPEC.md`): 20 ops / 23 invariants / 26 matrix
  rows / 16 edge cases; ALL contract code citations verified, 0 failures;
  8 findings — 1 policy fork (F-1), 5 implementer-fixable, 2 platform notes.
  **F-1 ruled by Fable as contract author** (the fork was internal to the
  contract's own §6/gate-4/O(window) texts): feed window selection is
  ARRIVAL-ONLY; `:order :arrival|:claimed` orders within the window;
  claimed-window selection refused with a pre-named promotion path
  (claimed-keyed twin bucket index) — D-001 grounds, no consumer demands
  claimed windows. Sweep executed (§3 example, §6, gate 4 rewritten, trap 4,
  new §9.10) + F-2..F-6 fixes (§11 PState-access scope; §3 `:rev` =
  source-version key; §5.2 uniform 3-kind fold + max-status-changed
  dominance, tiebreak relation-id; §4 sibling-row rule; gate 7 supersedes).
  Recorded in IMPLICIT_SPEC §7 addendum + D-006 evaluation notes
  (letter-ding / spirit-held). **Incident:** raw NUL byte in CONTRACT §11
  (tool-JSON escape decode, 4th firing); file(1) said "data"; perl re-spell
  applied; now "UTF-8 text". STANDING PACKAGE GATE from here:
  file(1)-must-say-text on every doc/code write. **Plan phase (P1)
  launched** (Opus): deliverable `build/trail-view/PLAN.md` against
  CONTRACT v1.1 + IMPLICIT_SPEC; duties include the F-8 query→mirror-query
  scratchpad SPIKE (settle now, not in Phase B) and a baseline
  `clojure -M:test` run (stop on red). Next on its return: plan-validation
  phase, fresh context, default-fail.

- **2026-07-04, Fable — session CLOSE-OUT: PLAN COMPLETE, handoff to fresh
  sessions.** Plan receipt (Opus, artifact `build/trail-view/PLAN.md`):
  **F-8 spike VERDICT = WORKS** — query topology → mirror query via
  `invoke-query` proven live on create-ipc; single-roundtrip Phase-B design
  ADOPTED, client-composition fallback not needed. Spike also proved the
  load-bearing mirror rule: mirror-PState reads route ONLY via the mirror
  partitioner `(|hash$$ $$mirror *k)` — plain `|hash` silently mis-routes
  (recorded in PLAN as a hard rule + in implementation-quirks); one `|hash$$`
  sets the mirror-partition index for sibling same-module PStates (whole OC
  family in one hop); custom key-partitioner routing needs the EXTRACTED
  object-key. **Baseline suite GREEN** (2 tests, 165 assertions, 0
  failures). Plan covers A1/A2/A3/B with cited constructor call-sites,
  per-gate deftest map, fixture via the existing OC ingest-request builders,
  read plans with partition scopes inline. **No stop-clause escalations.**
  One recorded open item (PLAN §10): F-3 `:latest` follows CONTRACT §3
  (`read-latest-source-by-ref`) over IMPLICIT_SPEC C7 (contract > derived
  spec), with a CONDITIONAL escalation trigger — if re-ingest cannot yield a
  stable pinnable source-version-key for a spec doc, escalate per stop
  clause. NUL trap fired AGAIN in PLAN.md (5th repo firing) and was caught
  by the standing file(1) gate — the gate works; keep it. **HANDOFF: this
  Fable session ends here (token-economics ruling with Sid — no more
  Fable-priced wake-ups for mechanical phases). NEXT SESSION (fresh, cheaper
  model, one line: "Trail-view WP1 — run the next phase per this baton"):
  PLAN VALIDATION — fresh context, default-fail, scenario-trace PLAN.md
  against CONTRACT v1.1 + IMPLICIT_SPEC (incl. §7 resolutions) + the Rama
  refs; artifact `PLAN_VALIDATION_R1.md` (per-round files, never overwrite a
  FAIL). Then: A1 → A2 → A3 → B, one phase per fresh session. Fable
  re-enters ONLY on: stop-clause fork, twice-failed validation, or gates
  green (gate review).**

- **2026-07-04, Fable — SESSION ROUTING (standing for the rest of this
  package; Sid's ask: he opens every session conservatively on Opus; the
  session itself must catch a mis-route).**

  **Self-check rule — run this FIRST, before any phase work:** (1) find the
  next undone phase in this NOW log; (2) look it up in the table below;
  (3) your own system prompt says which model you are ("You are powered
  by..."). If the required model is NOT the one you're running on, your
  FIRST message must say: *"This session is `<phase>` — it needs
  `<model, effort>` because `<the why column>`. Please restart me there."*
  — then STOP; do no phase work. If the model is right but the effort
  isn't, say so and continue only if Sid says go (`/effort` is
  session-scoped). If a Fable re-entry trigger fires MID-session (genuine
  policy fork, second validation FAIL, gates green), do NOT attempt that
  work on Opus: write the escalation into this baton (and decisions.md
  Open Questions if it's a fork) and tell Sid to open a Fable session.

  | Phase (in order) | Model | Effort | Why |
  |---|---|---|---|
  | Plan validation (NEXT) | Opus 4.8 | xhigh | default-fail scenario tracing — the layer that caught both cycle-1 plan bugs; a miss here costs whole code rounds |
  | A1 custody impl | Opus 4.8 | high | careful code against cited call-sites |
  | A2 stance kinds | Opus 4.8 | medium | one registry line + grep + gates |
  | A3 activity + R3 | Opus 4.8 | high | 4th-hop write + replay determinism — discipline work, plan already rules it |
  | B trail-view module | Opus 4.8 | high | largest phase, but spike-proven mechanics + cited plan |
  | Diff falsification reviews (impl + tests, per skill layer 4) | Opus 4.8 | xhigh | fresh-context adversarial pass; caught the cycle-1 read-path divergence |
  | Stop-clause ruling | **Fable** | high | adjudication on binding docs — never improvised below Fable |
  | Gate review (gates green) | **Fable** | xhigh | the judgment layer; boots fresh from this baton (~40k context) |
  | Retro at close | Opus 4.8 | high | written from the trail |
  | Retro adversarial recheck | **Fable** (or strongest available) | xhigh | per skill: retros are written by the process they judge |

- **2026-07-04, Opus 4.8 (effort max) — Track A: PLAN VALIDATION R1 = PASS.**
  Fresh-context, default-fail scenario-trace of PLAN.md vs CONTRACT v1.1 +
  IMPLICIT_SPEC (+ its §7 resolutions) + code + Rama refs. Artifact:
  **`build/trail-view/PLAN_VALIDATION_R1.md`** (file(1)=UTF-8 text, byte-clean).
  Self-routing check: this is Plan validation → Opus 4.8/xhigh; ran on Opus 4.8
  at max (≥ xhigh, Sid set it explicitly) — no mis-route.
  **VERIFIED (affirmatively, with citations — default-fail earned):** read
  `relation_kernel.clj` IN FULL — every Phase-A edit-site citation EXACT (records
  :178-190; relation-outcome :314-395; actor bind :325/326; rejected-row helper +
  3 sites :309-312/:345/:351/:358; transition-row :271-286; endpoint-copy +
  copy-hops :288-297/:610/:618; envelope :705-718 + actor-default :714; journal
  dedup :562; accepted branch :579; to-side :617-621). Gate-9 replay determinism
  HOLDS — the kernel is entirely now-ms-free (even `decided-at-ms`=`ts` at :384,
  unlike OC's :428/:448/:467); duplicates dropped at :562 BEFORE the outcome;
  rejects skip the `<<if`; activity write is `termval` (write-only); vars carry
  across ≥2 partition hops (proven by existing `*row` at :587→:610→:618).
  `RelationActivityRow` 16-arg order correct; R3 out-of-`let` placement follows
  the working R1/R2 precedent; A2 one-line registry edit safe (grep: NO second
  kind enumeration in src/+test/). **Custody (A1) PROVABLY cannot regress the 165
  assertions:** no `->Relation*Row` literals in the test ns; T10 (:168) compares
  three copies of the same `*row` → custody-invariant for any actor; the existing
  distinct-`:actor` tests (llm :230, mallory :359/:400, empty :513) are all
  reject cases asserting on status/reason, not full rows. Two Opus subagents
  verified ALL object_container.clj + adapter + test citations (11 mirror PStates
  :1685-1770 all object-key-partitioned; ops module :2547-:2556;
  extract-object-key routing incl. `chat:`-segment kept; query arg vectors; join
  fields source-id :99-102 / source-ref :82-84; NO arbitrary-container-revision
  query exists) and ALL Rama-doc citations (mirrors.md skill tree + 13-/18- docs
  tree — every line MATCH; `|hash$$`→source-module task; sibling-index reuse;
  async-boundary snapshot honesty; launch-time dependency enforcement;
  leading-partitioner = latency-only). **NO correctness defect, NO citation
  failure, NO silently-resolved binding-doc conflict** (F-3 is contract-governed
  + §10-tracked; `:clock`→`:order` is spec-stale-flagged; plan follows contract).
  **ADVISORIES (non-blocking; fold into the phase):** N1 — §4.4 omits the
  `activity-order-key` row accessor its own §4.5 code calls (add it in A3;
  self-correcting at compile); N2 — A3 extract-placement style (extract before
  the hops per :591-592, optional); N3 — C7's container→source-ref join *read
  path* is the plan's thinnest spot (pin in Phase B: read the spec via
  `read-context-bundle` first, or hold an extra handle); N4 — cosmetic
  extract/leading-object-key label swap (both in :262-334); N6 — keep the bucket
  `"%08d"` width identical across §4.4 write / §4.6 R3 / §5.3 C2.
  **OPEN DOUBTS (falsifiers named; non-blocking):** (A) the F-8 spike is
  un-reverifiable here (throwaway modules discarded) — the docs make
  query→mirror-query inferable-but-not-exemplified for the query-topology case
  (18:68 is a stream topology); it rests on the baton attestation + the
  PRE-SPECIFIED client-composition fallback (unchanged wrapper signatures, §7
  "latency is not a gate"), so even if wrong Phase B has a defined path → the
  Phase-B smoke test is the falsifier; do NOT conflate invoke-of-mirror-query
  with a *leading* mirror-state partitioner (13:196 forbids only the latter,
  optimization-only — plan handles this at §10 F-7). (B) gate-12 must revise the
  spec by RE-INGEST (new SourceVersionRow), not an edit — plan §10 carries the
  conditional escalation trigger. Did NOT re-run the 165-assertion IPC suite
  (that is code-phase gate 16; kernel ns loads clean this session; baton attests
  it green twice today). **NO stop-clause escalation; NO Fable re-entry trigger
  fired.** **NEXT:** **A1 custody** — fresh session, Opus 4.8 / high, per §12 +
  PLAN §11; phase order A1→A2→A3→B stands. Roam track-A session-log block ready
  to post on Sid's word.

- **2026-07-04, Opus 4.8 (effort max) — Track A: A1 CUSTODY IMPLEMENTED; gates
  10 (kernel half) + 16 GREEN.** Self-routing check ran FIRST: next undone phase
  = A1 custody → table = Opus 4.8/high; ran on Opus 4.8 at max (≥ high, Sid set
  it explicitly) — no mis-route. Loaded `/rama` + `/work-package`; read CONTRACT
  v1.1 §5.1 + PLAN §2 + PLAN_VALIDATION_R1 + `relation_kernel.clj` IN FULL + the
  test ns IN FULL (code = ground truth over the plan's absolute cites).
  **IMPLEMENTED (`src/app/server/rama/relation_kernel.clj`; authorized by
  CONTRACT §2 "Exception, ruled here" + baton allowlist):** appended
  `envelope-actor-id` + `envelope-actor-type` to the END of `RelationDecisionRow`
  (12→14 fields), `RelationEventRow` (16→18), `RelationEdgeRow` (14→16); bound
  `actor-type (:actor/type actor)` beside the existing `actor-id` in
  `relation-outcome`; threaded `actor-id actor-type` into ALL SIX row
  constructors inside it — the 3 `rejected-decision-row` sites (helper arity
  9→11), the accepted `->RelationDecisionRow`, the `->RelationEventRow`, and the
  edge via `transition-row` (arity 14→16 — whose EXISTING-ROW `assoc` branch ALSO
  overwrites `:envelope-actor-id/-type`, so the edge always reflects the LATEST
  transition's writer). `endpoint-copy` + the copy hops UNCHANGED — the two
  target copies inherit custody via the existing `(termval *row)` writes (the
  pre-existing T10 full-row-equality assertion proves all three copies identical,
  still green). `RelationStatusLogRow`/`…DescriptorRow` untouched (§2.1).
  **TEST (`relation_kernel_test.clj`, additive, NO 3rd IPC launch — rode dt1 on
  disjoint keys per the minimize-launches rule):** two `testing` blocks — (a)
  divergent custody (payload asserter `sid`, envelope `:actor` =
  `agent:claude-code/session-x`/`:llm`) asserting writer≠asserter on the edge row
  AND the writer on decision + event rows AND identity NOT forked (trap 11); (b)
  convergent (no `:actor` → default writer == asserter, so Phase-B `:written-by`
  omits it). **VERIFIED:** ns macroexpands clean (`clojure -M:test` require);
  gate 16 run BEFORE adding the test = 2 tests / 165 / 0 fail (no regression);
  full suite after = **2 tests / 180 assertions / 0 failures** (165 + 15 new).
  Every new-arity constructor is exercised by a green assertion (accept, all 3
  reject reasons, both `transition-row` branches) → arities proven at RUNTIME,
  not just compile. Falsification done-gate: named "target copies lack custody"
  → refuted by the green T10 identity. **NO stop-clause, NO binding-doc conflict,
  NO Fable re-entry trigger.** Gate-10's TRAIL-VIEW half (bundle `:written-by`
  projection) is Phase B, correctly deferred. **⚠ LINE NUMBERS SHIFTED — A2/A3/B
  must GREP, not trust PLAN absolute cites:** now `(def relation-kinds` = **:52**
  (A2's one-line edit), `actor-type` binding = :351, to-side copy hop =
  **:646-650** (A3 inserts the 4th-hop activity write AFTER :650, inside the
  `<<if (outcome-accepted?` branch). **A1's diff STILL OWES the fresh-context
  diff-falsification review** (routing table: Opus 4.8/xhigh, skill layer 4) —
  not run this session (author self-review ≠ a fresh layer). **NEXT: A2 stance
  kinds** — fresh session, Opus 4.8 / medium; add `:confirms :refutes
  :supersedes` to `relation-kinds` (:52); gate 15 (unregistered still rejected,
  3 new accepted, descriptor bound ≤ 2×10 = 20 rows); validation R1 §3.4 already
  confirmed NO second kind enumeration exists in src/+test/. Then A3 (activity +
  R3, gates 9/16), then B. Docs NOT committed (Sid's word); code NOT committed.
  Roam track-A session-log block ready to post on Sid's word.

- **2026-07-04, Opus 4.8 (effort max) — Track A: A2 STANCE KINDS IMPLEMENTED;
  gates 15 + 16 GREEN.** Self-routing check ran FIRST: next undone phase = A2
  stance kinds → table = Opus 4.8/medium; ran on Opus 4.8 at max (≥ medium, Sid
  set it explicitly) — no mis-route. Resolved the A2-vs-owed-A1-review ambiguity
  from the work-package QC model: layer-4 diff-falsification is a BATCHED
  fresh-context pass listed AFTER B in the routing table, so A1's owed review is a
  standing debt subsumed there — and A2 (registry line) is ORTHOGONAL to A1
  (custody fields), so proceeding builds nothing on unverified custody logic.
  Loaded /rama + /work-package; read CONTRACT v1.1 §5.2 + gate 15 + PLAN §3/§7.3 +
  the kernel registry/descriptor/reader regions + the test ns IN FULL.
  **INDEPENDENTLY re-verified (grep, not trusting PLAN — promises don't
  self-enforce):** `relation-kinds` is consumed by EXACTLY ONE fn,
  `registered-kind?` (`contains? relation-kinds`); NO second enumeration / switch /
  client allowlist anywhere in src/+test/; the kind keywords elsewhere are
  PRODUCERS emitting one edge (`transcript_ingest.clj:408/411`,
  `transcript_adapter.clj:319/354` hard-code `:produced`) or test data; the only
  `case` near kinds is `direction-code` (switches on :outgoing/:incoming, NOT
  kind); R1's kind filter is descriptor-driven (`relation-read-ranges`), not a
  literal list → the one-line add is PROVABLY sufficient for acceptance.
  **IMPLEMENTED (`relation_kernel.clj`, registry-only per allowlist):** appended
  `:confirms :refutes :supersedes` to `relation-kinds` (7→10) with a 6-line
  rationale comment (binary directed; :supersedes = belief displacement, distinct
  from :built-over/:new-direction lineage). **TEST (`relation_kernel_test.clj`,
  additive, rode dt1 on disjoint `g15-` keys — minimize-launches, no 3rd IPC):**
  new "WP1 gate 15" block — (a) all three stance kinds ACCEPTED end-to-end
  (`read-relation-row` carries the kind + `:asserted`; decision `:accepted`);
  (b) the common judged `to` target carries EXACTLY the three incoming stance
  descriptors (`i:confirms/i:refutes/i:supersedes`) and per-target descriptor count
  `≤ (* 2 (count relation-kinds))` read LIVE off the registry (keeps the
  NON-subindexed `$$relation-target-descriptors` PState safe); (c) an unregistered
  `:relates-to` STILL rejected (`:relation/kind-unregistered`, no row / no target
  copy / no descriptor). **VERIFIED:** full suite = **2 tests / 203 assertions / 0
  failures** (180 prior + 23 new gate-15 — the exact +23 match proves every
  gate-15 assertion executed, no vacuous/skipped block). Gate 16 (180 prior) GREEN
  — the registry change regressed nothing (grep confirmed no test counts/enumerates
  the kind set). file(1)=UTF-8 text on both files, zero raw NUL (standing package
  gate). Falsification done-gate: named "new kind's descriptor written under a
  colliding/mangled key" → refuted by the exact-3 + per-kind `contains?` assertions
  (green) and the SHARED `target-descriptor-key` helper (write/read can't drift,
  kernel :143-145). **NO stop-clause, NO binding-doc conflict, NO Fable re-entry
  trigger.** **⚠ LINE NUMBERS SHIFTED AGAIN — my +6-line kernel comment moved
  everything below :53 down by 6; A3/B MUST GREP, not trust PLAN §4 absolute
  cites.** Post-edit kernel anchors A3 needs: `relation-kinds` = :52-59,
  `registered-kind?` = :232, `relation-outcome` = :344, `outcome-accepted?` = :436,
  `$$relation-target-descriptors` PState = :576, the accepted branch
  `(<<if (outcome-accepted? *outcome)` = **:616** (A3 inserts the 4th-hop activity
  write inside this branch, after the to-side copy hop). The test ns's lower half
  shifted down ~69 lines. **A2's diff (impl + test) OWES the fresh-context
  diff-falsification review** (routing table: Opus 4.8/xhigh, skill layer 4) —
  batched with A1's after B; author self-review ≠ a fresh layer, NOT run this
  session. **NEXT: A3 activity + R3** — fresh session, Opus 4.8 / high, per CONTRACT
  §5.3 + PLAN §4: `RelationActivityRow` + `$$relation-activity-by-bucket` PState +
  `:request/sent-at-ms` envelope key (+ `relreq-sent-at-ms` accessor,
  `assert/retract-request` pass-through) + the 4th-hop `(|hash bucket)` write in the
  accepted branch (:616) + R3 `relation-activity` query topology + a V1
  `read-activity-rows` reader for gate-9 negatives; gates 9, 16. THE discipline:
  NEVER read the wall clock on the accepted path — bucket/order-key/arrival derive
  from client stamps so a replayed microbatch re-derives byte-identical rows
  (contrast `object_container.clj:428/448/467`, trap 4b). Then B. Docs NOT
  committed; code NOT committed (Sid's word). Roam track-A session-log block ready
  to post on Sid's word.

- **2026-07-05, Opus 4.8 (effort max) — Track A: A3 ACTIVITY + R3 IMPLEMENTED;
  gates 9 + 16 GREEN. PHASE A COMPLETE.** Self-routing check ran FIRST: next undone
  phase = A3 → table = Opus 4.8/high; ran on Opus 4.8 at max (≥ high, Sid set it) —
  no mis-route. Loaded `/rama` + `/work-package`; read CONTRACT v1.1 §5.3 + PLAN §4 +
  PLAN_VALIDATION_R1 advisories (N1/N2/N6) + `relation_kernel.clj` IN FULL + the test
  ns IN FULL + `13-query-topologies.md` + `object_container.clj` `fixed-width-order-key`
  (= `"%020d:%s"`). Code = ground truth; line numbers had shifted twice — GREP'd, did
  NOT trust PLAN absolute cites. **IMPLEMENTED (`relation_kernel.clj`, allowlist §5
  only):** (a) `RelationActivityRow` (16 fields — carries `envelope-actor-id` but NOT
  `-type`, §5.3 authoritative); (b) `$$relation-activity-by-bucket`
  `{String (map-schema String RelationActivityRow {:subindex? true})}` on `hash(bucket)`;
  (c) `:request/sent-at-ms` envelope key `(or sent-at-ms asserted-at-ms)` + accessor
  `relreq-sent-at-ms` + assert/retract docstrings (builders already spread opts);
  (d) two-clock derivation in `relation-outcome`'s accepted branch — `claimed = ts`,
  `arrival = (or sent-at asserted-at 0)`, `bucket = (bucket-key arrival)`,
  `activity-ok = (fixed-width-order-key arrival event-id)`; **bucket width centralized
  in ONE `%08d` const** (advisory N6 by construction, not convention); (e) accessors
  `outcome-activity-row`/`outcome-bucket` + `activity-order-key` row accessor (resolves
  advisory N1); (f) extract-before-hops (advisory N2 idiom, `*outcome` never crosses a
  hop) + the **4th `(|hash *bucket)` write** in the accepted branch after the to-side
  copy; (g) **R3 `relation-activity`** query topology (enumerate+fan buckets →
  subindexed `{:allow-yield? true}` read → `|origin` → `+vec-agg` → `sort-activity-rows`);
  (h) runtime handles `:activity-by-bucket`/`:relation-activity-query` + `read-relation-activity`
  (public R3) + `read-activity-rows` (V1, `[(keypath k) ALL]`→`mapv second`, the proven
  `read-target-index` idiom). **THE OPEN TECHNICAL QUESTION SETTLED** (default-fail):
  does the R3 terminal agg fire on an EMPTY bucket range? `13-query-topologies.md` —
  query topologies are "implicitly batch blocks… emitted exactly one time"; a terminal
  global aggregation fires exactly once regardless of input count. **Empirically
  confirmed:** the green test asserts R3 over an empty range returns `[]` (not nil, not
  error). No nil-seed needed — unlike R1, whose seed exists only for result-MAP
  completeness, not agg-firing. **REPLAY DETERMINISM (gate 9 core): zero wall-clock
  reads on the accepted path** — bucket/order-key/arrival/claimed all pure fns of client
  stamps; trap 4b cited in a code comment. **TEST (additive, rode dt1 on disjoint
  `a3act-*` keys, no 3rd IPC launch):** gate-9 block — 3 accepted transitions
  (assert/reassert/retract) → EXACTLY 3 rows in bucket b1; journal-replay (same idem
  key) + a rejected `:bogus-kind` whose arrival ALSO maps to b1 add ZERO (b1 stays 3);
  the assert row's `arrival == client :sent-at-ms`, `claimed == :asserted-at-ms`,
  `bucket` derived from arrival — asserted with **FIXED year-2001 sent-at values** so a
  stray `now-ms` would land 2026 buckets and FAIL; order-key arrival-prefixed; R3
  single-bucket (3), multi-bucket `[b1 b2]` (4), empty range (`[]`). Added
  `clojure.string` require. **VERIFIED:** kernel macroexpands clean (topology graph
  compiles — R3, 4th hop, record arities all parse); full suite = **2 tests / 222
  assertions / 0 failures** (203 prior + 19 new gate-9 — the exact +19 proves every
  gate-9 assertion executed, no vacuous block). **Gate 16 (203 prior) GREEN** — the
  additive change (new PState, new hop, new envelope key, R3) regressed nothing (activity
  adds no depot records, so the deterministic barrier count is unchanged; existing tests
  don't read activity). file(1)=UTF-8 text on both files; 0 raw NUL (perl byte-count),
  only the 2 intentional `\u0000` escapes (standing package gate). **Falsification
  done-gate:** named "unary `:none` activity row (to-id nil) chokes construction/read" →
  refuted by green Gate-8 (writes a nil-`to-id` activity row without error); named
  "replay double-buckets" → refuted by no-now-ms + green `arrival == sent-at`. **NO
  stop-clause, NO binding-doc conflict, NO Fable re-entry trigger.** **PHASE A (A1+A2+A3)
  is COMPLETE — gates 9, 10-kernel-half, 15, 16 green.** Gate-10's TRAIL-VIEW half
  (bundle `:written-by` projection) and gate-9's trail-view CONSUMPTION are Phase B,
  correctly deferred. **A3's diff (impl + test) OWES the fresh-context
  diff-falsification review** (routing table: Opus 4.8/xhigh, skill layer 4) — batched
  with A1's + A2's after Phase B; author self-review ≠ a fresh layer, NOT run this
  session. **⚠ LINE NUMBERS SHIFTED AGAIN (~+55 lines: helpers + record + R3); Phase B
  must GREP, not trust PLAN cites. STABLE NAMES for Phase B:** R3 query = `"relation-activity"`
  `[bucket-lo bucket-hi]` (fixed-width bucket strings); PState = `$$relation-activity-by-bucket`;
  wrappers = `rk/read-relation-activity [rt lo hi]` (public) + `rk/read-activity-rows` (V1);
  the trail-view module mirrors this R3 per PLAN §5.1 (mirror-query, `invoke-query`).
  **NEXT: Phase B (trail-view module + wrappers + text projection + fixtures)** — fresh
  session, Opus 4.8 / high, per CONTRACT §7/§8 + PLAN §5–§7; NEW files
  `src/app/server/rama/trail_view.clj` + `test/.../trail_view_test.clj` (+ fixtures);
  remaining gates 1–8, 11–14. Then run-to-green, then the BATCHED layer-4
  diff-falsification (A1+A2+A3+B, Opus 4.8/xhigh), then the Fable gate review. Docs NOT
  committed; code NOT committed (Sid's word). Roam track-A session-log block ready to
  post on Sid's word.
