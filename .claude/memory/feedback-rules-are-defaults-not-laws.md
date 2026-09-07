---
name: feedback-rules-are-defaults-not-laws
description: Consult when writing memory, agent definition, skill or starter text: state the default and its reason, and name what a departure looks like; "never", "ANY", "every", "must" are for the few hard rules only, because judgment in a session is sometimes dynamic
metadata:
  type: feedback
---

A memory, agent file, skill or starter says what the default is and why, and
what a reasonable departure looks like with its cost. It does not say the
default is the only way. Absolute words ("never", "ANY", "every", "must")
stay reserved for the handful of hard rules that really are absolute
(`env.clj`, no Co-Authored-By, subscriptions not API keys).

**Why:** Sid, 2026-09-06, on the scribe and two-turn-boot texts as first
written: "don't write in language that would make it seem like everything is
fixed and HAS TO DONE like the way it is described in memory or skill or
whatever the place is we don't want that .. the judgement is like sometimes
(note sometimes) dynamic and requires to do smth while in the process." A
rule written as law removes the judgment call it was meant to inform; a
session that hits the case the rule did not foresee either breaks the rule
in silence or does the wrong thing to honor it.

**How to apply:** three parts per rule — the default, the reason, the
departure and its cost. Read old memory in the same spirit: an existing
"never" outside the hard rules is a strong default with its reason, not a
wall. Kin: [[feedback-exploration-starters-direction-not-shape]] (the same
correction for starters), [[feedback-fix-the-generator-not-the-instance]].
