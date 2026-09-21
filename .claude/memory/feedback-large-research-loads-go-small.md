---
name: feedback-large-research-loads-go-small
description: "For a big reading load (hundreds of thousands of tokens of research or docs), reading it whole at max effort had too long a time to value for Sid; what worked better was going small and breadth-first with Codex as a line-range finder (2026-09-21)."
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 372bfb78-5c86-4eec-b482-7ebfc04edfee
  modified: 2026-09-21T12:01:51.802Z
---

On 2026-09-21 a session read one 200 KB research file whole at max effort and wrote a long
application of it. With seven more files to go (about 310k tokens in all), Sid stopped it: "300k is
not going to work out we need some way to do it in quite small way fable-max is just going to use
all and this has a long ttv".

**Why:** the reading plus max-effort thinking plus a long write-up per file costs too much and
delivers too slowly. The pilot also showed the value of such a file sits in a small part of it (the
short version, the section where positions are pressed, the challenges above the table), while a
faithful trim of the whole saved only 6%.

**How to apply:** this is a shape that worked, not a rule; judge it per load. Look at headings and
section sizes before reading bodies. Read the decision layer of every file before going deep in
any, and go deep only where voices disagree. Work per question across files where that is what the
ask needs, since reading file by file anchors on whichever went first. Codex in a read-only sandbox
was reliable at "is this already said over there?" (18 of 18 fair) when it returns line ranges and a
script copies the text, so nothing is retyped; give it no size target. Keep write-ups short (about a
page per item) and say what was not read. Related: [[project-first-record-research-application]].
