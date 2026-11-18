---
name: feedback-test-when-a-part-is-whole
description: "In a build, Sid wants no test or review loop after every small step; write a part whole, then prove it, and carry on without check-ins when he says he trusts the work"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 1c711d3c-e90b-4c42-a21f-a4ced72f7f97
  modified: 2026-10-07T19:23:10.455Z
---

Sid, 8 October 2026, at the start of the store-module build, before he went to sleep: "please don't get into testing circle jerk after every fuckign line ... ok i believe you and going to sleep now".

**Why:** he did not say. My reading: on 7 October he waited through many passes of checking before any code was written (a question sheet read three times by a second reader, a contract rewritten twice), and he reads repeated checking as not moving. It fits [[feedback-large-research-loads-go-small]]: a long time to value costs him more than a small error that is fixed later.

**How to apply:** in a build, write a part whole and then run its proof. Do not run the suite or add a review pass after each small edit. Keep the checks that a method or a skill already sets at its stage boundaries (for Rama: the check of the plan, the build's own proof, the final review) and add none of my own on top. When he says he trusts the work and leaves, carry on without check-ins and give one receipt at the end. This does not loosen the rule to confirm before a destructive or outward-facing action.

**13 October 2026, the build "before real data is kept" (session z-build).** I had run the Rama skill's whole staged process (requirements paper, split, plan, adversarial check of the plan, build, review, each in a fresh session) on every part of the build alike, because his prompt said "Run it by the Rama skill's stages". Two hours in, no line of the store had changed, and a third of the build was the removal of code. His lines: "but this is not a fresh work right??? it is about removing code that is now not needed???"; "why do we need to go through 7 steps to remove the code?? and same for the tiny one??"; "so why the fuck did we did that and doing that??? this is such a bad design"; "fuck the testing for every fucking word that is written focus on throughtput think about the process of working itself not blindly following instructiosn". When I kept one adversarial check of a plan running "on the side", he quoted its opening line back ("adversarial, default-fail mindset ... PLAN_VALIDATION") with "These are the fucking things I am saying", and I stopped it.

**How to apply, corrected by that day** (my reading, not a line of his): the earlier line here, "keep the checks that a method already sets at its stage boundaries", was too weak. A named method in his prompt is a pointer to how the work should go, not an order to run every stage on every piece.
- Before starting, size the process to the work and say so in the first reply: what is new design, what is removal or a small change, and which stages each gets. If the method he named is heavier than the work, tell him then, not after two hours.
- Removal, renames and small changes: one short list of what goes, then the edit, then the proofs. No requirements paper, no plan check, no validation paper.
- New design with a hidden failure mode: runs on the real system first (those found the real faults that day), one plan, then build. A separate adversarial check of a plan is not something he wants, even beside the critical path.
- Throughput: split a big edit by file ownership among parallel builders working from one plan, run the proofs once when all are done, and send each failure back to its owner.
- A check that I add "because it costs no wall time" still costs him attention and reads as ceremony. Do not add it.

**14 October 2026, the change "order and time apart".** Four builders split the proofs, and each was told to run its own proof as it worked. Sid, asked about it: "it depends on the changs if each individual is big then it can do its proof run itself but if all are smallish we can do all one at the eng".

**How to apply:** when builders split a change, size each builder's runs to its own change. A builder whose change is big runs its own proof while it works; when the changes are smallish, no builder runs a proof, and every proof runs once at the end. Make anything a proof rests on, such as a fixture, at the end too, just before that run, so it is made from the final source.
