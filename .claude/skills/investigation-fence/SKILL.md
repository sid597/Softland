---
name: investigation-fence
description: >-
  Use when investigating a live failure, performance problem, regression,
  intermittent bug, or any DISPUTED factual claim about system behavior —
  what dominates cost, what caused X, whether change Y produced symptom Z —
  BEFORE writing attribution, architecture verdicts, or a correction
  contract. Encodes the receipts-before-verdicts law ("measurement first"):
  what static reading may assert, when a verdict is allowed, how disputes
  between analyses adjudicate, and the staged prompt templates
  (probe → ruling → bounded review). Also fires on: profiling, "what's the
  bottleneck", debugging a stall/freeze, or adjudicating between competing
  causal stories from different models/sessions.
---

# Investigation Fence — receipts before verdicts

Provenance (2026-08, the 28s-frame incident): a 28.7s frame / 35.7s settle.
Three static analyses across two lanes produced three DIFFERENT dominant
mechanisms. A true finding — the cluster-bounds quadratic, later measured
at 24.39s ≈ 68% of wall — was killed for a full cycle by a refutation that
inspected the wrong file, and the refutation was inherited by every later
document. The claimed dominant cost (HarfBuzz shaping) measured at 1.2s of
28.6s. One 30-minute read-only profile settled what three multi-thousand-
word documents could not — and the refutation's fix direction would have
closed the correction with ~24s of cold cost intact. This project fences
code (`verify_*_fence.mjs`); this skill fences investigation.

## The standing interrupts (the whole law — everything else is elaboration)

1. **Structure vs magnitude.** Reading source may assert STRUCTURE: this
   path exists, this field is/isn't carried, this loop is O(C×G). Any
   MAGNITUDE or attribution claim from reading alone — "X dominates",
   "Y caused it", "Z is expensive" — is a hypothesis: tag it and name its
   kill-probe. (Every consequential error in the incident was a magnitude
   claim made by reading; every durable pre-receipt find was a structure
   claim. Same instrument — different reliability per claim type.)
2. **Receipt gate.** No attribution verdict, architecture ruling, or
   correction contract until its receipt exists. Pre-receipt analysis is
   legal at ANY depth — the fence is on the deliverable type: it must end
   in a probe choice + pre-registered predictions, never a verdict. (A
   naive "no analysis before measurement" would have suppressed the
   incident's two best finds — the quadratic hypothesis and the confirmed
   hover prediction. Depth found them; the deliverable type is what keeps
   depth honest.)
3. **Modality escalation.** Disputes between analyses escalate MODALITY —
   reading → runtime value → profile/trace → bisect — never head-count,
   effort, or model size. A second reader is correlated noise with
   confident affect; a bigger reader is still a reader. Model identity is
   ZERO evidence (identity-prestige is exactly how the wrong refutation
   outranked the right claim).
4. **A refutation is a claim.** It meets the same taken-path standard as
   the claim it kills. "I found no evidence of X" is a gap report, not a
   refutation; "not proven" is never "disproven".
5. **Review timing.** Verdict-review before a receipt: none (probe-choice
   sanity only). After a receipt: exactly ONE bounded pass, ≤3
   decision-changing findings, preserve the rest. (Review value flipped
   sign at the receipt in the incident: pre-receipt cross-review multiplied
   wrong narratives; the single post-ruling review caught two real
   contract holes.)
6. **One author per question.** Two parallel full derivations of the same
   verdict → stop and merge. Plurality belongs in hypothesis GENERATION
   (cheap pre-registered predictions), never in verdicts.
7. **Deliverables are decision-shaped.** Every deliverable opens with the
   decision it serves; if there is no decision for Sid, it is a ledger
   line, not a message. Sid's attention is the binding resource.
8. **Breadth queues, never shrinks.** Forest asks (history, architecture,
   contract consequence) are legitimate — the incident's forest asks paid
   off. They run AFTER the receipt re-orders them, not concurrently with
   attribution. Never answer seven verdicts at once; queue six behind the
   probe.

## Choosing the receipt (by failure class)

A receipt = the cheapest observation that would change what we do next.
It certifies its CAPTURE CONDITIONS, not the world — pin corpus, adapter,
environment in the same breath (kin: the work-package env-attestation law).

- **Performance / stall / freeze** → CPU profile or frame trace + one
  runtime value inspection.
- **Wrong answer (semantic)** → minimal failing repro, reduced until every
  remaining element is load-bearing.
- **"Did change X cause it?"** → bisect, or toggle at the seam.
- **Intermittent / non-reproducible** → standing tripwire counter or a
  trap that captures state at occurrence. Never fake a repro; wrong
  measurement mints false certainty.
- **Distributed / ordering** → lifecycle transcript, ordered event log.
- **Design / meaning ("is this right for humans?")** → no instrument
  exists: the lived gate (Sid's felt walkthrough) IS the receipt, and this
  is the one domain where genuinely plural independent derivation earns
  its cost. Plurality scales with irreversibility + instrument-absence,
  never with complexity.

## Stage templates

**Probe stage (pre-receipt).** Give the investigating session:

> Investigate this empirically before interpreting it architecturally.
> Deliverable, one screen:
> 1. up to 3 observed facts (each names HOW it was observed);
> 2. your falsifiable predictions, pre-registered — each states the
>    observation that would kill it;
> 3. the ONE cheapest decisive probe (profile / repro / bisect / runtime
>    value) and why it adjudicates the most hypothesis-space;
> 4. what stays unknown either way.
> Forbidden at this stage: attribution verdicts, architecture judgments,
> fix directions, contracts. Magnitude words ("dominates", "hot",
> "expensive") only inside a tagged prediction.

**Ruling stage (post-receipt).** Give the receipt-holding session:

> Given the attached receipt(s), write the measured ruling, one screen:
> 1. where the time/failure actually went — buckets with numbers;
> 2. the taken path, producer → consumer, with file:line;
> 3. every claim marked observed / derived / still-assumed;
> 4. capture conditions the ruling is valid under (corpus, environment,
>    adapter);
> 5. local defect or violated/missing invariant — at most 3 governing
>    decisions, each with the evidence that would falsify it.
> Do not reopen attribution. Do not prescribe implementation beyond what
> the invariant requires.

**Bounded review (the one pass on a ruling or ruling-born contract).**

> One pass. Find at most 3 decision-changing omissions or contradictions;
> preserve everything else. A refutation must trace the taken path to the
> same standard as the claim it kills — "I find no evidence of X" is a gap
> report, not a refutation. Verdict per finding: CONFIRMED-DEFECT /
> PLAUSIBLE-CHECK-THIS / COVERED. No rewrites, no new scope, no second
> round unless a finding is a genuine blocker.

## Permanent receipts (so the next incident starts at a log line)

Instrument INVARIANTS, not metrics. A standing tripwire: fires on
violation of a stated law, names its owner when it fires
(attribution-carrying), costs zero reading when green. No invariant → no
tripwire — that is the whole defense against observability bureaucracy.
Correction contracts ship their tripwires as DELIVERABLES, not
scaffolding. The 28s frame was self-noticing; what was missing was
attribution — build receipts that answer "whose is this" the moment they
fire.

## Routing note (lanes and prompts)

Prompt shape predicted output quality better than lane choice:
verdict-inviting breadth prompts produced confident wrongness from every
lane; receipt-anchored prompts produced the incident's strongest artifacts
from every lane. So: route receipt-anchored factual work to the cheapest
competent lane; magnitude attribution to an instrument, never a lane;
direction/meaning by REGISTER (CLAUDE.md), not routing. Lane-capability
beliefs ("X is better at source", "Y at coverage") are hypotheses — retros
note lane surprises in one line; priors move on accumulation, never on one
incident, and never adjudicate a dispute.

## What this skill does NOT govern

Direction/meaning exploration (Session Registers in CLAUDE.md) — no
receipt exists for "where should we go", and demanding one would kill the
register. And it never bans deep reading: depth is legal at every stage;
only the pre-receipt deliverable type is fenced.
