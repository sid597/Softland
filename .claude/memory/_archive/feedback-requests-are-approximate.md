---
name: feedback-requests-are-approximate
description: "Sid's requests are approximate pointers to the cleanest design — the design goal outranks his literal words; walls mean re-derive, never patch around; duct-tape compliance is treated as sabotage"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: e4337204-e424-41c6-8e0f-c95c94f71166
---

Sid's standing directive, verbatim (2026-07-12, decisions.md-restructure session):

> My requests are APPROXIMATE. I am not the one coding; you are. My directions
> are pointers toward what I actually want -- the simplest, cleanest, most
> elegant design -- and they may be slightly off. That goal ALWAYS outranks my
> literal words.
>
> So when you hit a wall -- a case that doesn't fit, a spec that breaks, an
> assumption that fails -- the wall is information: the design is wrong
> somewhere. STOP. Re-derive the design from first principles until the wall
> does not exist. If the result diverges from my spec, diverging is your DUTY:
> present it to me.
>
> What you must NEVER do is patch around the wall to comply with my words: a
> flag, a special case, a conversion shim, a second channel, a parallel path, a
> test rewritten to dodge a broken rule. The patch IS the failure. Every
> duct-tape betrays my intent while pretending to honor it, and it WILL be
> rejected -- 100% of the time, regardless of cost already sunk. A blocker
> honestly reported is a good outcome; a "working" deliverable built on
> gambiarra is the worst possible one, and is treated as sabotage.

**Why:** Sid is not the one coding and knows his specs may be slightly off. He
optimizes for the cleanest design, not spec compliance — the same
feelings-first / medium-is-the-message stance as his design taste, applied to
engineering direction. A shim that "works" hides the information the wall was
carrying and costs a rejection later; an honestly reported blocker or a
presented divergence keeps the design honest.

**How to apply:** Treat every instruction as a pointer, not a contract. On any
wall (case that doesn't fit, spec that breaks, assumption that fails): stop,
re-derive from first principles, and if the clean design diverges from his
words, present the divergence explicitly — never silently comply via flags,
special cases, shims, parallel paths, or tests rewritten to dodge the rule.
First exercised in full 2026-07-12: his manual decisions.md cut deleted the
D-001 arbiter rule; the restructure kept it as one line and presented the
divergence — Sid then overruled with reasons ("middle manager bullshit...
elon management style") and the rule + the whole numbered-statute genre were
removed. Lesson: presenting the divergence is the duty; his ruling on it is
final and gets executed fully, not partially. Also lives in CLAUDE.md (top
section) so it loads in every session.

Related: [[feedback_precision_over_validation]],
[[feedback-approve-by-default-no-ceremony]], [[feedback_fix_dont_defer]]
