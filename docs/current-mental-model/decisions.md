# Softland Decision Log

Seeded 2026-07-03, Fable session (wall-photo stress test). Revised same day after
Sid's corrections (Fable-ban context; 2026-06-30 code-ingestor consolidation).
**Countersigned by Sid in-session 2026-07-03** ("this is correct decision tree and
I want to commit to it") — D-001 through D-005 are CLOSED. Reopening requires
evidence of a used form breaking, per the operating rules below.

Companion file: `BETS.md` (same directory) — North (Sid's vision as commitment,
his authorship only) + the bet ladder (route hypotheses with pre-registered
kill/confirm evidence). This log governs closures; BETS.md governs direction.

Operating rules:
- A decision is **PROPOSED** until Sid countersigns it; countersigning flips it to **CLOSED**.
- A CLOSED decision may only be reopened by **new evidence from a used form breaking
  against it** — never by a new argument, research round, or model opinion.
- Every decision records the evidence it rests on, so reopening attempts can be
  checked against what was already known.

---

## D-001 — Arbiter rule (governance)
**STATUS: CLOSED** (countersigned 2026-07-03)

The runtime/substrate only grows when a form in actual daily use breaks against it.
"An imagined future form would need X" is not a valid reason to build X.

**Evidence:** Sid's own description of the recurring loop (runtime→too big→need a
view→runtime), four iterations, and the wall image-1 margin note: "my crisis is
based on me trying to build either end without the other."

**Correction 2026-07-03:** the original evidence cited the Jun 12–26 gap as a
confusion→research spiral. Wrong — Fable was banned mid-build; wrapping up was
forced, and the versioning research was a deliberate fallback that *converged*
(see D-003). The rule stands on the evidence above, not on June.

---

## D-002 — First form = the trail view (the 27-04-2026 panel)
**STATUS: CLOSED** (countersigned 2026-07-03)

The first form is the view Sid drew on 27-04-2026: timeline, what-changed-
concretely, product-DAG with dead-ends, answering "where are we / how did we get
here / what crossroads did we take" — rendered in Softland, over Softland's own
material (code, git history, docs, Claude/Codex chats). Panel 2's "View 3: AI
agent" is part of the same form: the material must be legible to agents, not only
to Sid.

Sid's 2026-07-03 message confirms this is the goal ("make the 27-04-2026 view come
true in softland itself"). The earlier framing of this decision as "overruling the
headless plan" was wrong — the real dispute was sequencing, now split out as D-005.

---

## D-003 — Code enters Softland per the two-regime split (Regime-1 spine first)
**STATUS: CLOSED** (countersigned 2026-07-03; adopts Sid's 2026-06-30 consolidation as the ruling)

"Code ingestor" is two problems wearing one name; the tell is the unit (flow vs
code). This log adopts Sid's consolidated understanding:

- **Regime 1 — external code as a VIEW.** git owns versioning; filesystem owns
  storage; Softland imports a derived view. Unit = the thread of divergence.
  Address the code (blob-sha + path anchors), don't copy it. Two boundaries:
  import (git→Rama, commit-grained) and commit (Rama→git); Rama is truth only for
  in-flight work between them. Drift discipline: on git change re-extract the
  changed set; on conflict git wins.
- **Regime 2 — recursive Softland (self-hosting substrate).** Unit = capability.
  Gated on the self-hosting test: building Softland's next capability must be a
  worked instance of "a user extending Softland." object-kernel-revision.md
  belongs here, not to the code ingestor.

**Ruling for the first form:** the 27-04 view needs only the **Regime-1 spine** —
commit metadata (sha, date, message, parents, files touched) + sha/path addresses
+ the joins (transcript↔commit, transcript↔doc, commit↔commit). The joins are
extractable from already-ingested transcripts (tool calls carry file paths and git
activity). Code *content* resolves live through pinned addresses. The atomic-block
semantic graph is built when the rendered view demands it; Regime 2 waits for its
gate.

**Settled inputs carried in from Sid's consolidation:** git-authority; commit-
boundary ingest; trail-to-code resolution as first consumer; the exactness rule
("the map must not lie"); file-granularity; fail-closed scope (env.clj can never
enter).

---

## D-004 — Ontology: core sound; missing noun = typed relation edge
**STATUS: CLOSED** (countersigned 2026-07-03)

The object-container ontology (identity / history / provenance / containment,
`object_container.clj:38-171`) is well-factored; the graduation pattern holds.
Missing: a typed, non-compositional relation edge between containers. Verified
2026-07-03: the only edge type in src is CompositionEdgeRow.

**Fill (from Sid's own artifacts, not a general theory):**
- Starter relation kinds = the arrows of the 27-04 panel: `based-on`, `produced`,
  `built-over` / `new-direction`, `dead-end`; plus `elaborates` (kraft overlay)
  and `references` (footnote-style panel refs).
- Home: the **space kernel** — Sid's dismantling names it as the connector layer
  that makes relations among artifact types.
- Every relation carries provenance: `asserted-by` (sid | llm | import) must be
  first-class, so LLM-proposed glue is visibly distinct from human-asserted
  structure (calibration: the map must not lie).

**Amendment 2026-07-03 (contract recon):** the evidence overstated absence. The
space kernel holds an untyped forward/reverse adjacency pair
(`$$artifact-graph` / `$$artifact-graph-in`, `space.clj:1484-1485`) populated
with derived workflow-plumbing edges (thread→turn→bundle→llm-run). The decision
stands: no *typed, asserted, provenance-carrying* relation exists. Home ruling
refined by the contract: relations live in a NEW `relation-kernel-module`
(implementing the connector role Sid assigned to the space kernel); the
plumbing graph stays separate as derived adjacency. Full reasoning:
`docs/current-mental-model/build/relation-kernel/CONTRACT.md` §2.

---

## D-005 — View-first sequencing (the one live disagreement)
**STATUS: CLOSED** (countersigned 2026-07-03)

The 27-04 view starts rendering **now**, on material already in the substrate
(transcripts + markdown, ingested Jun 7–8) plus the small commit-metadata adapter.
It will be ugly and gappy; each gap names the next ingestor slice. Ingestors are
built in the order the rendered view demands — the view orders the import work,
not the other way around.

This replaces "finish the headless import round, then show it in the UI."

**Evidence:** the image-1 margin note (data and view co-evolve; "the end product
is only produced when I keep track of both"); a completeness-gated import round
has no external signal for "done enough."

---

## D-006 — Fable-window allocation (bet + pre-registered evaluation)
**STATUS: CLOSED as a bet** (placed 2026-07-03; evaluation pending, criteria fixed below)

**The worry (Sid, recorded verbatim in spirit):** spending Fable-level intelligence
on MVP-path work may waste the limited window on tasks a cheaper model could do.

**The bet (Fable):** the window goes to (a) two load-bearing contracts — RelationEdge,
then the trail-view data contract + acceptance gates; (b) gate reviews of cheaper
models' work packages; (c) adjudication of forks; (d) a succession document.
NOT continuous orchestration, NOT implementation, NOT Regime-2 theory (gated by D-001).

**Why Fable for the RelationEdge contract specifically:**
1. Highest wrongness-cost artifact in the MVP path: the trail view, the space
   kernel, and the future DG protocol all build on this noun. A subtle identity or
   partitioning error is a rewrite of everything above it, discovered months later.
2. The hard parts are prior-failure-shaped: cross-partition writes / event
   boundaries (the Slice-A v1→v2.1 class of bug), retry semantics, and revisable
   judgments (a `dead-end` mark can change) riding on an immutable event log.
3. Contract errors are cheap now (text) and expensive later (kernel + consumers).

**Pre-registered evaluation — run after the contract exists:**
1. **Trap count.** The contract must name, for each load-bearing choice, the naive
   alternative and the concrete failure it causes. Fewer than 3 non-obvious traps
   caught ⇒ Fable added little over a careful Opus pass.
2. **Counterfactual probe.** Give Opus 4.8 (fresh session) the same input manifest
   (recorded with the contract) and ask for the same contract. Diff the
   load-bearing choices. If Opus independently makes the same calls, the bet loses.
3. ~~**Codex falsification round.**~~ **Waived by Sid 2026-07-03**, replaced by
   a stronger criterion: **implementation contact**. The contract must survive
   the implementer's full phased Rama process to green acceptance gates
   (CONTRACT.md §11) **without a contract amendment**. A contract-breaking flaw
   found during implementation ⇒ bet loses on quality, recorded here. The two
   memory-based claims flagged in-session (microbatch mid-batch replay fine
   print; query-topology fan-out idiom) are assigned to the implementer's
   Phase 1 verification against the on-disk Rama references.
4. **Token ledger.** Record roughly what the contract cost from the window.
   In-session estimate: contract session ≈ one long Fable conversation
   (recon + contract + log upkeep), no implementation tokens spent.

**Evaluation notes (running):**
- 2026-07-03, first stop-clause firing (corrected record): TWO
  `PHASE_VALIDATION:fail` rounds preceded escalation. Round-1 findings
  (duplicate-key conflict decision, reassertion no-ops, N>M query reads) were
  fixed by a Codex plan revision. Round-2 F1 (plan invented a
  `RelationRequestRow` depot record that couldn't route via
  `hash-by :relation/routing-key`) was fixed by an Opus session reverting to
  the contract's map-envelope idiom, verified against codebase precedent — no
  adjudication needed. Round-2 F2 (idempotency scope: CONTRACT's colocated
  journal vs IMPLICIT_SPEC's global-sounding entity) was correctly escalated
  per the stop clause and ruled by Fable: **relation-scoped** (see Open
  Questions ruling). Honest scoring against criterion 3: by the LETTER
  ("without a contract amendment") this is a ding — the contract left
  idempotency scope implicit where a validator had to catch it. By SPIRIT the
  design held: no schema/plan/topology change; the ruling made the contract's
  stated intent explicit. Both readings recorded; Sid weighs them at final
  evaluation. Positive signal for the QC layers: three defect classes were
  caught and fixed by fresh-context validation/revision cycles before a single
  implementation token was spent, and the one genuine policy fork was
  escalated rather than improvised.
- 2026-07-03, Fable gate review: **PASS** — full record in
  `build/relation-kernel/GATE_REVIEW.md`. Suite independently re-run (2 tests,
  165 assertions, 0 failures); traps 1/2/7 spot-checked in the diff (incl.
  byte-verifying the NUL id-separator); falsification pass produced 4
  non-blocking open doubts (envelope/payload binding is client trust — cheap
  server-side recheck when agent writers appear; dead
  `replayed-from-decision-id` field; O(n²) R1 dedup; single-worker tests).
  Criteria status: 1 (trap count) met; 3 (implementation contact) stands at
  the already-recorded letter-ding/spirit-held split — nothing after the F2
  ruling required amendment; 2 (counterfactual probe) NOT yet run; 4 (ledger)
  gate ≈ one read-heavy Fable session. Package is gate-complete; commit and
  close are Sid's.
- 2026-07-03, package CLOSED (Sid + Fable, same session): code committed
  (`2796044`), docs trail committed (`736708f`+), retro run —
  `build/relation-kernel/RETRO.md` (QC-layer scorecard, 7 next-contract rules,
  mechanisms to keep). The retro is the pre-registered input to the
  work-package succession skill (queued below); write that skill in a fresh
  session from RETRO.md.
- 2026-07-04, trail-view WP1 Phase 0 (fresh-context re-derivation, Opus —
  the succession skill's layer 1, first reuse after relation-kernel):
  verified ALL contract code citations (0 failures), derived 20 ops / 23
  invariants / 26 matrix rows / 16 edge cases, and caught **1 genuine
  contract conflict** (F-1: claimed-window feed retrieval vs arrival-keyed
  buckets vs O(window) promise — any two hold, never all three) plus 5
  implementer-fixable ambiguities. Fable ruled F-1 same day as contract
  author (arrival-only window selection; `:order` param; claimed-index as
  pre-named promotion path — D-001 grounds) and swept all six fixes into
  CONTRACT v1.1 before any plan/code tokens. Honest scoring in the
  criterion-3 style: by the LETTER an amendment happened during the process
  (ding); by SPIRIT the QC layer did exactly its job — conflict caught at
  text-time, zero downstream cost, no schema/topology change. Also logged:
  the NUL-escape tool-JSON trap fired a fourth time (in the contract
  itself), caught at Phase-0 close by file(1), repaired; file(1)-must-say-
  text is now a standing package gate.
- 2026-07-05, **process form-break, ruled with Sid in-session — fresh
  context ≠ fresh session.** Sid reported the same failure on BOTH live
  tracks: the remaining WP1 work rendered as "~5 more sessions" and he was
  disoriented by the package shell itself ("this track is feeling so big to
  me with no clarity if this is actually useful"). Evidence gathered before
  ruling: WP1 Phase A = 227 changed code lines vs ~300KB (~75k words) of
  process docs across the two packages; the baton at 939 lines with 1–2k-word
  NOW entries; cycle-2 marginal catch-rate falling (plan validation R1
  PASS-with-advisories only, A1/A2/A3 all first-run green, zero stop-clauses)
  while per-session orientation cost stayed constant. Root cause: the
  2026-07-04 token-economics routing ("no more Fable-priced wake-ups for
  mechanical phases") bound the fresh-CONTEXT QC requirement to fresh
  SESSIONS — a ledger that counted model tokens and priced Sid's attention at
  zero; and the scheduled Fable gate re-entry meant fragmentation no longer
  even saved Fable tokens (the re-entry boot was coming anyway). **Ruling
  (Sid's countersign = his in-session selection of the collapsed plan):** QC
  layers keep their kill record and stay; session boundaries go. Default
  shape: one orchestrating session (Fable when already booted) runs phases
  with fresh-context Opus subagents as the validation/review layers; routing
  tables are advisory on model/effort, never binding on session structure;
  baton NOW entries capped ~15 lines. Skill amended same session
  (work-package SKILL.md, four edits, provenance noted). D-006 itself is NOT
  reopened — Fable-does-contracts/gates/adjudication held; what broke was
  the second-order session cadence built around it. Scoring note for final
  evaluation: this is the pre-registered implementation-contact criterion
  doing its job at the PROCESS layer — the operator is also a user of the
  form, and the form broke against him.
- 2026-07-05, **WP1 gate review: PASS** — full record
  `build/trail-view/GATE_REVIEW.md`. Suite independently re-run twice this
  session (final: 3 tests / 302 assertions / 0 failures — 222 Phase A + 74
  Phase B + 6 reviewer-added). One gate gap found AT gate: gate 10's
  trail-view half (custody projection) had no executed assertion — closed by
  a reviewer-authored test-only block, green first run. The client-composition
  deviation from PLAN ruled contract-sanctioned (§7 pre-named fallback;
  letter-ding/spirit-held recorded in the artifact). Criterion 3 for Phase B:
  contract survived implementation with ZERO amendments. Process note:
  implementation, falsification, and gate ran as three distinct contexts
  across two Sid-authorized sessions after a double-dispatch collision (root
  cause + fix in the process ruling above); the collapsed cadence delivered
  contract→green→falsified→gated inside 24 hours. Eight non-blocking open
  doubts with named falsifiers in the artifact. Commit and package close are
  Sid's.
- 2026-07-05, WP1 package **CLOSED** (executed by Fable under Sid's in-session
  time-box blanket — "use the recommended option, note the others"; the
  alternative, holding for a full retro session, is noted in RETRO.md). Code
  was already committed (`af0e0e2`/`fd59b78`/`63202b0`/`67f75eb`); light retro
  at `build/trail-view/RETRO.md`; carried items routed — 8 open doubts stay
  live in GATE_REVIEW.md, display-name enrichment assigned to the WP2 window,
  the write-path pair (/assert affordance + OI-1 durability) scheduled same
  day. WP-B2 close waits only on gate-15 evidence (Sid, ~20 min).
- 2026-07-05 (session close), **delivery-mode ruling (Sid, verbatim): "lets
  do all the coding tasks first ... then a batch testing one ... we don't
  need to do the falsification one sprinkled all over because we will do the
  coding using fable now and its going to get it right the first time ...
  our goal is delivery not slowing down due to process and management
  things."** Effect on D-006 allocation: (e2) Fable MAY implement directly
  when it is the fastest path (supersedes "NOT implementation" as a hard
  bar); QC consolidates to ONE serial test batch + ONE batched
  falsification/gate at the END of a coding wave — never sprinkled
  per-phase. Honest ledger recorded for future evaluation, not argument:
  today's per-artifact validation rounds caught two Fable-authored contract
  errors (R1 invisible-edges, R2 impossible-reader) at text time; the
  consolidated end-gate must be sized to catch that class. CLOSED by Sid's
  words above; reversible on form-break like every process ruling.
- 2026-07-05 (delivery session), **first full delivery-mode wave CLOSED:
  git-spine WP2 + trail-room R-1 gate-passed and committed.** The (e2) shape
  ran end-to-end: one batched falsification (6 should-fixes, 0 blockers) →
  one Fable gate review (`build/git-spine/GATE_REVIEW.md`) with all fixes
  applied DIRECTLY at gate by Fable (route custody validation, deterministic
  idempotency, allowlist non-join drop, replay/extract per-item isolation,
  UTF-8 pins, reader field-preservation) → one serial suite (**191 tests /
  2202 assertions / 0 failures**) → per-package code commits
  (eac6dd5/ed2db8a/a1fda21/dc743d6/7827f6f/57b0113). Evaluation evidence for
  the honest ledger: the batched end-gate caught ONE new defect the
  falsification missed (kraft-label overflow — same class as a found one,
  instance-vs-class hunting) and ONE root cause beneath a found defect
  (rect_tree clip? replaced the ancestor clip instead of intersecting —
  the layer-below rule). Both fixed in-wave. Fable also self-caught one bug
  in its own fix before commit (truthy blank-string override). Delivery-mode
  cadence HELD for a 4-package wave; retros at `build/git-spine/RETRO.md` +
  `build/trail-room/RETRO.md`. Packages CLOSED.

---

## D-007 — The bet foundry: claims→bets intake + Fable questioning practice
**STATUS: CLOSED** (proposed by Fable 2026-07-03 from Sid's in-session idea;
**countersigned by Sid 2026-07-04 in-session** — "on the decisions i
countersign to d-007")

Sid's proposal (verbatim source: `vision/LOG.md` 2026-07-03 "the bet-foundry
idea + the HCI thesis"): BETS.md is where all his claims/hypotheses/bets land;
Fable's window includes making those bets real — sharpening, breaking down,
actually working toward them; and Fable proactively asks Sid questions about
the vision so bets are formed together, better informed.

What this closes if countersigned:

1. **Intake pipeline** (mostly already built at the 2026-07-03 sitting; this
   ratifies it): raw claims land verbatim in `vision/LOG.md` first (existing
   law); the BETS.md **Candidates** inbox holds only sharpened forms —
   falsifiable statement + pre-registered kill/confirm evidence +
   dependencies. Fable-grade sessions do the sharpening. Entry is cheap;
   activation is rationed.
2. **Fable-window allocation extension** (amends D-006's list): add
   (e) **bet formation** — sharpening candidates, and decomposing promoted
   bets into work-package-shaped probes, each probe then run per the
   `/work-package` skill; and (f) the **questioning practice** — a
   Fable-window session that touches vision/bets opens or closes with 1–3
   questions from the frontier: where a bet lacks a falsifier, where North is
   silent on something the ladder assumes, where two bets quietly conflict.
   No quota — a question must name its frontier source or it isn't asked
   (anti-ceremony guard). Sid's answers, his words → LOG; the sharpened
   consequences → BETS.md.
3. **Guardrails carried over unchanged**: one ACTIVE bet at a time; promotion
   only at review sittings, by Sid; the ladder never grows by accumulation;
   North stays Sid-authored; and all realization work remains evidence-paced —
   a bet's work package must be the cheapest discriminating probe, and
   form-break fixes outrank speculative probes (D-001 is not amended by this).

Evidence this rests on: the relation-kernel cycle proved the decomposition
machinery end to end (contract → phases → gate → retro → skill); the
Candidates inbox exists and is empty — what was missing is exactly the intake
motion and the questioning practice this entry names.

---

## D-008 — Read-only MVP: the first trail view writes nothing
**STATUS: CLOSED** (drafted by Fable 2026-07-04 in the track-A WP1 contract
session, from Sid's own 2026-07-04 ruling — verbatim in `vision/LOG.md`
"the read-only MVP ruling"; **countersigned by Sid 2026-07-04 in-session**,
same day: "Countersigned as yes")

What this closes:

1. **The first trail view is READ-ONLY**: zero kernel writes from the view.
   Local view-state (zoom, filter, selection) is allowed. Enforced
   structurally in WP1: the trail-view module declares no depots and no ETL
   topologies (contract gate 14) — it physically cannot write.
2. **The write surface is the existing Claude CLI.** The LLM is phase-1's
   writer: Sid instructs in CLI, the agent asserts (payload asserter = sid,
   envelope actor = the agent). Consequence, hard-gated BEFORE phase-1 daily
   use: the relation kernel must durably record BOTH actors (custody
   amendment, trail-view CONTRACT §5.1) — verified 2026-07-04 that today it
   persists only the payload asserter.
3. **Watchers are triggers over existing ingestors** (transcripts, md,
   commits once the spine lands) — never new ingestors; safe because the
   kernels were built for convergent re-import (deterministic ids,
   idempotency journals).
4. **Sid's loop:** work in CLI → view updates near-live → screenshot back
   into CLI when he wants action. Every face renders its own ADDRESS as text
   (the query + params that produced it), so a screenshot is a resolvable
   pointer (contract §3).
5. **read→write is the NAMED second milestone**, entered when observed
   read-only friction demands it — desire-paths sequencing (D-001) applied
   to the write surface itself. Walk-capture (`last-walked`) explicitly
   waits for that milestone; the field exists now, nullable, rendered as
   unknown.

Evidence this rests on: Sid's 2026-07-04 ruling (LOG verbatim: "for the very
first MVP we don't need the write surface... we would not be rethinking and
expanding scope... it will be testable by me"); the Fable falsification pass
the same day found no scope hole (the one real gap it surfaced — custody
recording — is item 2's hard gate).

---

## Open questions queued for ruling
- Fable-window queue (per D-006): ~~gate review of relation-kernel
  implementation~~ (done 2026-07-03, PASS) → trail-view data contract →
  hypothesis kill-conditions at first trail-view render → ~~succession
  document~~ (done 2026-07-03, pulled ahead per Sid's next-prompt sequencing:
  skill at `.claude/skills/work-package/SKILL.md`, written from RETRO.md after
  its adversarial recheck).
  D-006 criterion 2 (counterfactual probe, input manifest CONTRACT §13) is
  still unrun and can slot anywhere — it needs an Opus session, not the
  Fable window.
- Succession document spec (write AFTER the first full work-package cycle, as
  a skill at `.claude/skills/work-package/`, amended by what the cycle taught):
  (1) the five-layer QC model — contract coherence via fresh-context
  re-derivation / plan via validation sessions / code via executable gates /
  diff via adversarial falsification review / everything via daily use;
  (2) how to open a work package — contract with traps ledger + acceptance
  gates + input manifest, next-prompt.md STANDING/NOW baton, precedence rule
  (baton never outranks contract or log); (3) how to run a gate review without
  Fable — CLAUDE.md falsification protocol against the diff, verdict recorded
  in this log; (4) re-entry triggers — gates green or stop clause tripped;
  (5) the retro procedure — package trail (NOW log + phase artifacts) →
  lessons routed to implementation-quirks / operating-model / D-006 notes —
  first exercised at relation-kernel close; that first retro is INPUT to
  writing this skill. **EXECUTED 2026-07-03 (Fable)**: the queued adversarial
  recheck of RETRO.md ran first (addendum in RETRO.md — 4 corrections, 2 new
  lessons, 1 residue addition; suite independently re-run green, 165
  assertions), then the skill was written at
  `.claude/skills/work-package/SKILL.md` covering all five spec items.
- object-kernel-revision.md (Jun 26): now framed by D-003 as a **Regime 2**
  document. Rule on whether anything in it constrains the Regime-1 spine, or
  whether it is entirely gated behind the self-hosting test.
  **RULED 2026-07-04** (by Fable, in the track-A WP1 contract session, per the
  session duty Sid delegated on the track page): **entirely gated as
  authority.** None of its five encoding decisions (§6: merge operator,
  claim/disagreement encoding, credential algebra, non-monotone fold
  treatment, distortion-budget distribution) may be built ahead of the
  self-hosting gate, and no Regime-1 artifact may cite the doc as a
  requirement source. Nothing in it blocks the Regime-1 spine or the
  trail-view data contract. Two zero-cost *naming/semantics disciplines* that
  the doc's [HOLDS] items corroborate are adopted for new Regime-1 artifacts —
  on independent Regime-1 grounds, so they stand even if the doc is later
  revised: (1) **two-clock discipline** — every new row/feed that carries time
  distinguishes claimed-time (client-supplied `asserted-at-ms`, orders
  semantic history; the relation-kernel importer-timestamp residue) from
  ingest/arrival-time (when the land learned it); grounds: a July re-import of
  April notes must not render as "today" in the trail view. (2) **custody vs
  assertion recording** — where envelope actor and payload asserter
  legitimately differ (phase-1: agent writes on Sid's instruction), the write
  path must durably record BOTH; grounds: gate-review open doubt #1, and
  verified 2026-07-04 that the relation kernel currently persists only the
  payload asserter (envelope `:actor` is checked for retraction rights and
  dropped — `relation_kernel.clj` RelationDecisionRow/RelationEventRow carry
  no envelope-actor field). Everything else in the doc that touches Regime-1
  territory (omissions marking, disagreement-preserved-not-merged) is already
  Regime-1 law via D-003 settled inputs and D-004 — the doc adds no new
  obligations there.
- Which face of the trail view renders first (View 3 agent-legibility is
  cheapest; View 1 outline; View 2 canvas replaces the wall but costs most).
  Decide after D-002/D-005 countersign.
  **RULED 2026-07-04** (by Fable, on Sid's explicit delegation via Roam card —
  "I am not sure you decide"): **View 3 first** — it is nearly coextensive
  with the data contract itself (context bundles + the two query topologies;
  a minimal text projection, not a pixel investment); **the threaded/DAG
  timeline face second** (the 27-04 outline form) as Sid's first pixel
  surface for the read-only screenshot loop (INPUTS item 13/14); **canvas
  last**, parked until the wall-replacement ambition earns activation.
  Grounds: the first paying reader is the agent; the Sid-face rides the same
  queries View 3 defines, so the ordering costs nothing extra.
- **Solo-operator discipline rule — ADOPTED 2026-07-04** (Sid via Roam card:
  "yes adopt this"; origin: Q2 from the 2026-07-04 sessions): every session —
  build or thinking — ends with an assertion-grade baton entry in
  `docs/sessions/next-prompt.md`: what was decided / verified / doubted,
  findings verbatim on any FAIL, judgment calls flagged; everything else is
  recoverable. Cycle-1 evidence: the baton's verbatim entries were the only
  survivors of the overwritten PLAN_VALIDATION FAIL artifacts. Encoded in the
  interim protocol (baton item 9) and Claude memory.
- Regime 2 self-hosting test formulation (carried from Sid's consolidation).
- Confidence/credential algebra for the trail→code join; rename/move continuity
  (parked in the code-ingestor contract).
- **Transcript↔commit join representation — RULED 2026-07-05** (Fable, under
  Sid's in-session time-box blanket; reversible on his review): **durable
  import-asserted RelationEdges** — asserter-type `:import`, version-free
  actor-id `"import:git-spine"`, evidence refs to transcript session + entry,
  shas verified against the repo before asserting, exactness via a documented
  note-grammar v1 (promoted to a structured field only when a view demands
  rendering it — D-001). Alternative recorded, not taken: projection-time
  joins (no storage/staleness, but cannot carry asserted-by/exactness/
  retraction and need new query topologies per join type). Full grounds and
  traps: `build/git-spine/CONTRACT.md` §2. The exactness-flag semantics
  question queued alongside it is answered by the same ruling (note-grammar
  v1).
- **Relation-kernel idempotency scope (BLOCKS the relation-kernel work package;
  D-006 evaluation signal).** Surfaced at implementation contact after two
  `PHASE_VALIDATION:fail` rounds (PLAN_VALIDATION F2). The binding CONTRACT
  (§4-5: journal "copied from object-container's decisions-by-idempotency
  pattern", colocated on the relation-id task) and the binding IMPLICIT_SPEC
  (models `IdempotencyKey` as a standalone entity; "same idempotency key
  collapses to one decision") disagree on whether an idempotency key is unique
  **per relation** or **globally**. These cannot both hold in Rama: a colocated
  relation-id journal is physically relation-scoped; global uniqueness needs a
  `hash(idempotency-key)` index on a different task. **Recommendation (high
  confidence): confirm relation-scoped `(relation-id, idempotency-key)`** — it is
  a one-to-one copy of the object-container idiom the contract names (verified
  `object_container.clj:1693,1786`), and global collapse would silently drop a
  relation when two imports reuse a key (violates "the map must not lie"). Ruling
  options and the full trace are in
  `build/relation-kernel/PLAN.md` → "Idempotency scope (ESCALATED)". Option (A,
  recommended): a countersigned line here + reconcile IMPLICIT_SPEC wording, no
  plan change → re-run Phase 2. Option (B): amend CONTRACT §4-5 colocation, add a
  global index → re-run Phase 1. *Not decided by the implementing session, per
  the handoff stop clause (no improvising policy on binding docs).*
  **RULED 2026-07-03: (A) relation-scoped `(relation-id, idempotency-key)`** —
  by Fable on Sid's in-session referral of the fork (Sid: flag if you meant to
  hold this one personally). Reasoning: the key is a client retry token, not an
  operation identity (that is the deterministic relation-id); global collapse
  silently drops assertions (exactness rule); a global journal needs a
  pre-gate partition hop that un-colocates the gate; object-container precedent
  is partition-scoped. Executed: CONTRACT §5 amended (explicit scope), §11
  gained gate 11 (cross-relation key reuse), IMPLICIT_SPEC amendment banner
  added. CLOSED.
