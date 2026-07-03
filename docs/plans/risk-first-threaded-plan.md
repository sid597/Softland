# Session Plan - 2026-02-13 (Risk-First Threaded Review System)

## 0) Why this plan exists

We are not optimizing for "build more features."  
We are optimizing for:

1. Finding the right thing to work on in optimal time.
2. Hitting the highest-risk assumptions early.
3. Failing fast if the thesis is wrong.
4. Preserving the long-term vision without premature overbuild.

---

## 1) Working thesis (explicit)

### Thesis
Code review and handoff quality improves when work is represented as a **review artifact**:
- code changes
- decision trail
- evidence links
- unresolved risks

instead of only a git diff + PR comments.

### Recursive scope (zoom levels)
Same pattern at each level:
1. agent system -> developer reviewer
2. developer -> tech lead reviewer
3. tech lead -> product/project reviewer

What changes is abstraction level, not review grammar.

### Review grammar (stable across levels)
1. What was intended?
2. What changed?
3. Why this option?
4. What was rejected?
5. What evidence supports it?
6. What is still unknown/risky?

---

## 2) Highest-risk assumptions (ranked)

### Risk A (highest): Reviewer utility
Reviewers (human) must get faster confidence from artifact vs standard PR.

Failure signal:
- reviewer still asks "what changed and why?" repeatedly
- review latency unchanged
- back-and-forth unchanged

### Risk B: Artifact size/noise
Captured context may become too large to review.

Failure signal:
- reviewers ignore artifact
- only diff is used
- author spends too much time curating

### Risk C: Evidence quality
Claim -> evidence links may be shallow, stale, or misleading.

Failure signal:
- anchors don't prove claims
- links drift after rebase
- reviewers distrust references

### Risk D: Author overhead
Producing artifact may slow implementation too much.

Failure signal:
- author avoids using system
- manual burden outweighs review gains

### Risk E: Multi-context integration value (Roam + Linear + org context)
Could be useful long-term but low immediate proof value.

Failure signal:
- heavy ingestion work without measurable review improvement

---

## 3) Strategy: prove/disprove in this order

Order is based on maximum information gain per unit time:

1. Prove reviewer utility (A) first.
2. Prove low-noise bounded artifact (B) second.
3. Prove evidence integrity and staleness handling (C) third.
4. Prove acceptable author overhead (D) fourth.
5. Integrate broader org context (E) only after A-D hold.

If A fails, stop broad platform work.

---

## 4) What NOT to build yet (hard constraints)

Do not build in first cycle:
1. Full Roam sync pipelines.
2. Full Linear sync pipelines.
3. New ontology with many node/edge types.
4. Large graph UX beyond reviewer needs.
5. Cross-org dashboarding.
6. Model-training pipelines.

Reason: these increase complexity but do not answer top risks A/B/C.

---

## 5) Minimal product object for fast validation

## 5.1 Review Pack v0 (single artifact)

A bounded artifact generated per issue/PR with exactly six sections:

1. Intent (problem + acceptance target)
2. Scope (files/symbols touched + why)
3. Change summary (diff clusters)
4. Decisions (accepted + rejected options)
5. Evidence (claim -> code span/test output/command result)
6. Risks & Unknowns (explicit unresolved items)

### Constraints
1. Fixed length budget per section.
2. Every key claim must reference at least one evidence anchor.
3. Every changed file must have a rationale tag.
4. All anchors pin to commit hash.

This is the shortest path to test reviewer utility.

---

## 6) Implementation depth model (how deep to build)

We use depth levels to avoid overbuilding:

### Depth 0: Manual/concierge
- Capture structure manually for real tasks.
- No heavy automation.
- Purpose: validate usefulness of the artifact shape.

### Depth 1: Assisted generation
- Auto-propose sections from agent session + git diff.
- Human edits before publish.
- Purpose: reduce author burden while preserving quality.

### Depth 2: Reactive integrated flow
- Artifact auto-updates with commit/rebase/session changes.
- Staleness checks and delta review support.
- Purpose: operationalize at team scale.

Stop at Depth 1 until risks A/B/C are validated.

---

## 7) Phase plan with fail-fast gates

## Phase 0 (2-3 days): Research contract + baseline

### Build
1. Finalize Review Pack v0 schema and constraints.
2. Define baseline metrics on current workflow.
3. Select 3-5 real issues for pilot.

### Measure baseline
1. PR open -> first actionable review feedback time.
2. Total review cycle time to approval.
3. Clarification comment count.
4. Rework after review rounds.
5. Author prep time.

### Gate G0
Proceed only if team agrees Review Pack v0 fields are sufficient for review.

---

## Phase 1 (4-5 days): Concierge pilot (Depth 0)

### Build
1. For each pilot issue, produce Review Pack v0 manually.
2. Reviewer consumes pack before reviewing code.
3. Capture qualitative feedback with strict prompts:
   - What was missing?
   - What was noisy?
   - What accelerated review?

### Goal
Validate utility before engineering automation.

### Gate G1 (critical)
Continue only if at least 2 of 3 are true:
1. Review latency improves materially (target: >=20%).
2. Clarification loops reduce (target: >=25%).
3. Reviewer confidence improves subjectively (target: +1 point on 5-point scale).

If G1 fails: revise pack shape or abandon thesis.

---

## Phase 2 (1-2 weeks): Assisted pack generation (Depth 1)

### Build
1. Generate draft Intent/Scope/Change sections from:
   - issue text
   - branch diff
   - agent session events
2. Generate draft Decisions/Evidence/Risks from session traces.
3. Add author edit pass and publish action.
4. Render reviewer UI with strict progressive disclosure:
   - summary first
   - expand details on demand

### Keep explicit human checkpoint
No auto-publish to reviewer.

### Gate G2
Proceed only if:
1. Author prep overhead is acceptable (target <=15 minutes median per issue).
2. Reviewer still gets gains from G1.
3. Evidence precision acceptable (target >=80% "useful anchor" rating).

If G2 fails: trim automation, keep manual curation.

---

## Phase 3 (1 week): Integrity + staleness (Depth 1.5)

### Build
1. Commit-pinned evidence anchors.
2. Rebase detection + stale marker.
3. "What changed since last review request" view.
4. Diff-of-decisions (new accepted/rejected since previous pass).

### Gate G3
Proceed only if stale/conflict confusion drops in pilot reviews.

---

## Phase 4 (2+ weeks): Reactive integration (Depth 2)

Only after A/B/C/D validated.

### Build
1. Rama-native event streams for artifact state.
2. Agent/session context linked by issue/branch/work-session.
3. Reviewer subscriptions for live updates.
4. Optional initial Linear linkage (metadata only).

### Still defer
Full Roam ingestion and broad org knowledge graph integration.

---

## 8) Data model v0 (minimal and sufficient)

### Entities
1. `work-session`
2. `decision`
3. `evidence-anchor`
4. `change-cluster`
5. `review-pack`

### Minimal fields

`work-session`
- id, issue-id, branch, author, started-at, ended-at

`decision`
- id, session-id, status (accepted/rejected/parked), rationale, timestamp

`evidence-anchor`
- id, decision-id/claim-id, kind (code/test/command/link), file-path, commit, span, snippet

`change-cluster`
- id, session-id, files, summary, risk-tag

`review-pack`
- id, issue-id, pr-id, version, sections, created-by, created-at

No extra ontology in v0.

---

## 9) Integration strategy with existing Softland codebase

Leverage current assets:
1. Existing agent execution and session persistence flow.
2. Existing WebGPU rendering pipeline.
3. Existing Rama event/depot foundation.

First implementation slice should be additive:
1. New review-pack store + retrieval path.
2. New UI panel/view for pack summary + evidence jumps.
3. No replacement of current editor rendering model.

Do not block on file-window virtualization in first risk cycle.
That is a separate performance architecture track.

---

## 10) Metrics and stop criteria (non-negotiable)

### Primary outcome metrics
1. Review cycle time reduction.
2. Clarification comment reduction.
3. Post-review rework reduction.

### Secondary metrics
1. Author artifact prep time.
2. Reviewer pack engagement rate.
3. Evidence click-through/usefulness.

### Stop criteria
Stop or pivot if after two pilot rounds:
1. <10% review cycle improvement, and
2. author overhead >20 minutes median, or
3. reviewers skip artifact in most cases.

---

## 11) Pivot options if thesis partially fails

If full Review Pack underperforms:
1. Pivot to "Decision + Evidence brief" only (smaller artifact).
2. Keep automation as assistant for PR description quality.
3. Use packs only for complex/high-risk issues.

If reviewer value exists but author overhead too high:
1. Increase auto-draft coverage.
2. Reduce required fields.
3. Allow staged pack completeness (quick vs full modes).

---

## 12) Concrete near-term execution recommendation

For the next cycle:
1. Execute Phase 0 + Phase 1 only.
2. Timebox to 2 weeks max.
3. Make go/no-go decision at G1 using measured data.

This hits the riskiest assumptions fastest with minimum engineering spend.

---

## 13) Final strategic stance

The vision is worth pursuing, but only with strict risk-first discipline.

Build order should follow this rule:
1. Prove review value.
2. Then reduce capture cost.
3. Then harden integrity.
4. Then scale context integration.

Anything else is likely premature architecture.
