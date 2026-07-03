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

---

## D-007 — The bet foundry: claims→bets intake + Fable questioning practice
**STATUS: PROPOSED** (Fable, 2026-07-03, from Sid's in-session proposal; awaiting countersign)

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
- Which face of the trail view renders first (View 3 agent-legibility is
  cheapest; View 1 outline; View 2 canvas replaces the wall but costs most).
  Decide after D-002/D-005 countersign.
- Regime 2 self-hosting test formulation (carried from Sid's consolidation).
- Confidence/credential algebra for the trail→code join; rename/move continuity
  (parked in the code-ingestor contract).
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
