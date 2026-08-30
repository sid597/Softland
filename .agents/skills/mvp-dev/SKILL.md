---
name: mvp-dev
description: Sid's personal MVP-0 development philosophy and decision framework. Use when building an MVP, making build-vs-buy decisions, choosing between implementation options, scoping work, or when the user mentions MVP, prototype, or "just get it working." Guides architecture, security, code quality, and review tradeoffs. NOT the team's process.
user-invocable: true
disable-model-invocation: false
---

# MVP-0 Development Framework

## Context: Read This First

This is **sid's personal operating philosophy**. The team follows Shape Up / structured project management. Sid disagrees with that approach for experiments and MVPs at this stage. This is a deliberate, eyes-open choice to operate differently.

**The stakes are real.** If this goes wrong — a bug in prod, a bad merge, wasted time on a dead-end — there is no team process to point to. Sid bears 100% of the downside. The only protection is: sound architecture, real safety checks, and results that speak for themselves.

**Codex's role:** Counsel, not commander. Sid makes the calls. Codex gives honest strategic advice, flags risks that could blow up, and executes. If Codex sees something dangerous, say it once clearly — then follow sid's decision.

---

## Core Belief

Most projects never get implemented because they never rise in prioritization. Many are easy to build and would validate or kill a hypothesis cheaply. Experiments should live in the codebase — guarded by hidden settings, affecting no one who didn't opt in. Code maintenance cost is not the gold standard for whether to try something.

Enthusiasm and internal momentum matter. Process that blocks that momentum turns every experiment into "yet another project." One person should own the full vertical for an MVP. The process should empower the person doing it.

We are over-optimized on the first code that merges. Over-optimize on **architecture** instead. Cut aggressively on what's not needed now. Merge. Fix bugs as they come.

This is not "hey Codex implement this and push to main." We do all the sanity checks. LLMs still hallucinate at max effort, context windows aren't enough for 2-3 features stacked in one session. We know what safe code looks like and what not to do.

---

## Definitions

### FLO (Flow)
The candidate user flow for this MVP. "Candidate" because sometimes we can't know the real flow until users touch it.

### User Risk
Loading some version of the plugin modifies the user's data without them explicitly knowing. Examples:
- Corrupts data on a page
- Shares data with AI without permission
- Syncs data to a database the user didn't consent to
- New UI appears, functionality changes, keyboard shortcuts change
- Settings change silently

We have a sense of what's dangerous without exhaustively enumerating it. When in doubt: **don't code it, note it down, bring it up, be defensive.**

### Cost
Time to develop through to accept/reject:
- **If rejected:** cost = what was invested (keep it low)
- **If accepted:** Can we build on top of it? Is the architecture sound for foreseeable cycles? How much design cognitive load does integration add? How much time to fold into the design/UX system?

### Prototype
Codex codes it, sid gives directional feedback, it writes the code, security review, lives on a separate branch. No one gets affected. **We don't touch projects that would have user risk.**

### MVP
Merged to main. Only used by DG team members initially. Has potential for pilot and lab users.

The perceived risk is "we want it but do others?" — that's not the risk, it's the **potential**. Projects at this level get implemented because there's 75%+ confidence there's value. From an org perspective it's not only about existing users — low-cost experiments that are shared communicate direction and extend a hand to later adopters.

Architecture is the main benchmark. It'll be tested only by team members — no risk to other users. Not the stage for full Shape Up project management.

---

## What MVP-0 Means

- Works **end to end** — a user can complete the full flow
- **No optimization** on any axis
- **Reasonably sound architecture** — the base is solid, the glue is strong, what's glued together can be swapped later
- **Mergeable to main** — no branch maintenance, no rebasing pain, no blocked reviews
- Delivers on the **FLO**
- Clear boundaries, clear ways to build on top — the MVP will change but should be re-scoped and re-implemented, not hacked on
- From product POV: one specific initial user flow. Note branched flows for triage after MVP-0.
- From design POV: eyeball it, use existing libraries (Blueprint for Roam, Tailwind for web), let the FLO drive the UX

---

## Decision Framework: Choosing Between Options

When there are multiple ways to build something, pick in this order:

### 1. Already in the system
Use what's already implemented. Don't introduce a new pattern when one exists.
- **AI-related?** Prioritize embedding or prompt-based approaches first
- When prompts are the tool, write them broad but deep — capture the domain, not just the task

### 2. Widely-used library
If it's a big infra task and nothing exists in the system, pick the most popular library. Easiest path to fix bugs, most community knowledge, most LLM training data.

### 3. Custom implementation (last resort)
1. Note down 3 candidate solutions
2. Pick the one that **touches the least code and needs the least refactoring**
3. Document the other two as future options

### The Heuristic
> Will this help me deliver end-to-end so the user can try it and give feedback on the different axes?

Strong opinions, loosely held. Go with your best understanding, but don't marry the idea.

---

## Engineering Priorities (decreasing order of weight)

### 1. Architecture (most important — debate this the most)

- Identify individual parts of the current and larger project
- Make it **modular** — each part improvable independently later
- Figure out known unknowns and unknown unknowns with reasonable effort
- This should be debated and improved upon **much more than anything else**

### 2. Security (context-dependent)

**Inside Roam/Obsidian:** Host app provides the security envelope. Don't overthink it.

**Internal MVP (team-only):** Low risk. Focus elsewhere.

**External-facing:** Strategic decision (more than eng):
- Who sees this? What data can someone access? Are the data owners OK with that?
- **Time to action:** Worst case, how fast would we know, what can we do?
- Acceptable mitigations: cap spend on exposed keys, limit blast radius

### 3. Code Quality

LLMs do the coding, humans check **architecture and scope**:

**During implementation:**
- Use high-effort LLMs (Codex Opus high, Codex xhigh). Gemini Pro is fun but eager and shallow.
- Use the architecture ticket as system prompt
- Ask for initial plan from two AIs if possible — make them reach consensus. Save plan as `.md` (don't commit).
- After each session: update plan doc, write `next-prompt.md`
- After each milestone: have the other AI review it

**Before merge:**
- Run team review skills (`/review-like-our-team`, `/enforce-review-patterns`, or project equivalents)
- Security review
- Push to GitHub for automated reviews (Devin, CodeRabbit)

What passes through this filter gets fixed in later work — if that work ever arrives. Even if the original developer moves on, the MVP has good architecture and patterns to build on.

---

## Merge Philosophy

**Merge should not be blocked by long reviews.**

Code through LLM + human-check + skill-review is fine to merge. Production-level scrutiny is overkill for an MVP — we still get bugs after all that and fix them quickly, which is the important part.

**Critical safety check before merge (non-negotiable):**
- Only affects users who opted in
- Does NOT modify existing data in unexpected ways
- When in doubt: don't code it, note it down, bring it up

If there are multiple parts, create separate tickets based on right unit of work informed by architecture.

---

## After MVP-0: The Review Loop

### Product Feedback (before creating tickets)

1. **First pass (10 min):** Walk through the flow. Look for blockers preventing meaty feedback. "Works for me" bugs. Good defaults.
2. **Publish decision log:** What decisions were made? What were the options?
3. **For each axis** (design, AI strategy, library choices — described from user POV, not eng):
   - State baseline expectations
   - State what improves with further investment
   - Ask for **specific feedback** per axis
4. **Wait for feedback on ALL axes** before creating tickets
5. Consolidate, share, triage next milestones

---

## Introspection Loop

This skill is itself an MVP. It improves from use.

**After each MVP session**, Codex should prompt sid to capture observations in `field-notes.md` (in this skill directory):
- What decisions were made using this framework?
- What worked? What didn't?
- What surprised us?
- Any new pattern or anti-pattern discovered?
- Did we hit a safety boundary? Did the checks catch it?
- What did the team/lead actually say about the output?

**After each MVP project reaches accept/reject:**
- Was the architecture assessment correct?
- Was the cost/time prediction roughly right?
- What would we do differently?
- Fold durable patterns back into this SKILL.md

**What to watch for specifically (given the stakes):**
- Moments where the team process would have caught something we missed
- Moments where our speed caught something the team process would have been too slow for
- The actual reaction from the team to merged work
- Whether "fix bugs as they come" held up or compounded

Read `field-notes.md` before applying this framework to a new project. Accumulated learnings override generic advice in this file.
