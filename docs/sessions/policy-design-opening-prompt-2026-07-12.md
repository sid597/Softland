# Opening prompt — component-policy design session (paste whole file)

You are opening a DESIGN session on Softland's policy model — the two
questions Sid separated on 2026-07-12:

1. **Component-local policy.** What can THIS component do; which of its
   fields/bindings are writable, by whom, in which mode. Authored in the
   design phase, as data, living with the component. (Example from Sid: id
   and timestamps read-only at product layer — but the declaration that makes
   them read-only is itself editable one layer up, in design mode.)
2. **Policy visibility/granting.** What policies can a component even KNOW
   exist or have access to — the vocabulary, its namespace, who grants which
   policies to which contexts. A different, higher question than (1). Do not
   fuse them.

**Register:** design/direction — think WITH Sid; models, frames, failure
modes; no work-queues. This is UX design in Sid's sense: "designing means not
only UI, it is UX, and what the policy is for a component falls under that."
The horizon is "land as its own design medium" (vision/LOG.md 2026-07-10
and 2026-07-11 entries: the four sides, design-conversation-in-land, edit
mode for designers).

**Boot set (read, in order):**
- vision/LOG.md — the 2026-07-10 four-sides entry + the 2026-07-11 entries
  (editor lane, per-object control, citizenship criterion, edit mode).
- docs/current-mental-model/decisions.md — settled ground, whole file (short).
- src/app/client/workspace/face_assembly.cljc — header + validator shape
  (what assembly data looks like; where a :writable-style prop would live).
- src/app/server/rama/core.clj:343-350 — the existing authority check:
  actor capability sets, checked at the decision step. Policy is ALREADY
  data at this one point.
- docs/current-mental-model/build/block-write/CONTRACT.md §6 — the seam this
  design must plug into: ONE server-side decision point; policy inputs are
  data; client affordances never substitute for the server check.

**Hard constraints from settled ground (not yours to reopen):**
- Rama is truth; the UI reads materialized state; refusals are durable and
  visible (the map must not lie).
- Furniture is data: keywords/addresses persist, fn values never. A policy
  vocabulary must be expressible as data a face/assembly can carry.
- Anything that wants to be a program becomes real code in the code lane —
  if a "policy" needs turing-complete evaluation, that's a sign it's not
  policy data anymore; name it and stop there.

**Deliverable:** a policy-model proposal — the data shapes for (1) and (2),
where each is authored (design layer / component definition / grant table),
how the decision step consults them, and how affordances project from the
same data — written so it lands on block-write's §6 seam without re-plumbing.
Mark every commitment LAW/STANCE/DEFAULT/OPEN. Where (2) is genuinely open,
frame the option space instead of forcing a pick.

**Non-goals:** implementing anything; amending block-write's contract
(additive follow-ons only); visual design of the policy UI (that's a later
design-harness brief).

**No upstream blocker:** this runs in PARALLEL with block-write. The one
coordination point is CONTRACT §6 (the seam), already written.
