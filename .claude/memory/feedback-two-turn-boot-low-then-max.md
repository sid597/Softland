---
name: feedback-two-turn-boot-low-then-max
description: A boot shape worth reaching for when a session must hold a large load before thinking: turn one at low effort loads the ground and answers in one line, Sid types /effort max, turn two thinks; the cache survives the switch (checked 2026-09-06 in the seam session's transcript)
metadata:
  type: feedback
---

When a session has to carry a big load (handoffs, fact bases, pasted reviews,
a scribe's page maps) before it can think, split the boot in two. Turn one,
at low effort, loads names and skeletons rather than bodies and replies with
one line: what was read and its byte total. Sid types `/effort max`. Turn two
gets the thinking and the question. Order matters more than effort: the read
enters before any judgment exists, so the judgment cannot steer what got
read. Fence the load turn to one line, or a low-effort turn still writes a
summary.

This is a shape, not a requirement. A short session, or one where the
thinking has to start mid-read, runs on one effort and that is fine.

**Why:** Sid, 2026-09-02: "gather/read the stuff keep them in context BUT
NOT REASON OR POLLUTE BY GIVEN PRE DIRECTIONS then in the second prompt ...
ask for its decision". Reading costs tool-result tokens once; what the model
writes while reading rides the prefix for the rest of the session. Receipt:
the seam session (8a905b50) held the load turn to one line; at both effort
switches the first request read 168k and 344k from cache and wrote only the
new prompt.

**How to apply:** starters that carry a large load get two pastes, the first
ending "reply with one line". Lives as pastes in
`docs/below-the-waist/EXPLORE-STARTER.md`. Kin:
[[feedback-page-bytes-live-in-the-scribe]] (the page half),
[[feedback-waist-sort-hunters-code-only]].
