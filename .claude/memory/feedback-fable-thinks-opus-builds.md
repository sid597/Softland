---
name: feedback-fable-thinks-opus-builds
description: "Before you hand building or proof runs to a Fable subagent, or run every proof in each subagent: what Sid said on 28 November 2026 when an orchestrated build used 8% of his Fable limit"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 26482499-eeca-42a6-8599-d2fddfc4d456
  modified: 2026-11-28T08:15:05.963Z
---

On 28 November 2026 an orchestrating session (Fable) ran a twelve-change build over subagents.
The prompt, which a session had written and Sid pasted, said "each piece of work below goes to a
subagent on Fable at high effort". The session did that: a review (about 345k subagent tokens) and
two builders (about 580k and 680k), each of which also ran the long list of proofs. When Sid
looked, the session had used 8% of his Fable limit with most of the work still to do, and the
orchestrator's estimate for the rest was 30 to 45%. His lines, typed in that session:

- "be aware of.not wasting tokens on random tests that can be run by opus and all that and one
  trst for all the changes instead of one for eaxch in each subagent and i hope the subagents are
  fable high not xhigh and not max"
- "why do you need fable then why dont we only use fable for tinking and then opus is for
  implementation???"
- "since we know the bugs now we can fix rhem directly or no? Like each requores first thinking
  what is we need to do and only then do"
- "I dont get the reason for you saying 30-40% its toooo much"

**Why:** once a design is settled, building to it does not need Fable, and a proof run needs no
judgment at all. The cost came from Fable doing both, and from each subagent reading the tree anew
and running every proof.

**How it was applied that day** (the session's reading of his lines, not a rule of his; weigh it
against the work in front of you):

- Fable settles the design and does the reviews that try to break a build. The settled design goes
  into a file, and a builder on Opus is told to read that file.
- A builder runs only the proofs that hold what it changed. All the proofs are run once, at the
  end, on the branches stacked, by an Opus subagent that only runs and reports counts. A reviewer
  does not rerun proofs to see them pass.
- No subagent at xhigh or max unless he asks.
- What the session did not do and should have: say what a step would cost in Fable before starting
  it. He first learned the cost from his own meter.

A line of a pasted prompt that fixes the model of every subagent is a session's choice like any
other in it: [[feedback-a-pasted-prompt-is-not-sids-word]]. How subagents behave in this harness:
[[reference-subagents-in-this-harness]].
