---
name: feedback-fresh-cut-no-prior-drafts
description: "when Sid starts a discussion on X, do NOT read prior-session draft artifacts about X — the independent-cut rule covers same-problem drafts, not just the Codex design track"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: f256c759-9e28-44b4-930d-f979de7b0b31
---

When Sid says "let's start discussing X" / "we are starting the discussion,"
do not read existing draft/proposal artifacts about X from prior sessions —
even ones Claude itself wrote, even when plan-mode diligence says "find prior
art first." Ask before reading.

**Why:** In the code-ingestor product discussion (2026-06-10), Claude read the
prior session's proposed `code-ingestor-contract.md` while "mapping prior art"
during plan mode. Sid: "did you read it???? you should not read it .... i want
you to do it seperately and not spoil your context". The point of a fresh
discussion is an independent perspective; reading the draft anchors every
subsequent position. This is the same principle as [[independent-design-cut]]
(don't read the Codex track), extended to Claude's own prior-session drafts on
the same problem.

**How to apply:** Before opening any doc that proposes answers to the problem
Sid wants to discuss, stop and ask whether it should be in context. Grounding
docs (contracts, specs, actual source code) are fine; answer-shaped artifacts
(draft contracts, proposals, prior cuts) are not. If contamination already
happened, disclose it immediately, never cite the draft, and flag points where
provenance of an idea is uncertain. Related: [[feedback-principal-designer-manual-only]].
