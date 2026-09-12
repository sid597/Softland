# Design brief — the river & reconciliation room (PART 1 of 2)

2026-07-10 · opening brief for this design session. Two-message delivery:
this message carries the brief plus a data-shapes fixture; my NEXT message
carries the other fixture — 64 real river blocks. Repo docs the fixtures
mention (PROTOCOL.md, SPEC.md) are not attached and not needed — everything
binding from them is already reflected here. No engineering constraint
gates in here — explore, render alternatives; the human picks by feel.
Dreams welcome, labeled as dreams.

## SCENE

A conversation from the night of 2026-07-06 — the one where the old
container model died and the sense-line was born — has been broken by the
machine into addressable **river blocks** (Sid's messages, the agent's
thinking, the agent's replies), with **debris** (harness noise, tool
plumbing) classed out. Two readers — Sid and his agent — have each marked,
independently, where the SENSE of that night turns. Their seams disagree in
places. This room is where a human sits with the material, sees both
readings honestly, and settles them one by one — or declines to.

## CAST

- **the river** — the conversation as ordered, addressable material. Real
  data exists: 51 river events in the read conversation; a sibling
  conversation arrives as the next message (64 real blocks, machine cut
  visible).
- **seams** — two authors' marks on the same material; provenance always
  visible. Whose mark is whose must be legible at a glance — the map must
  not lie.
- **the diff** — agreements · missed · extra · moved · same-place-different-why.
- **debris** — present in truth, quiet in the room (this conversation has 73
  debris events). How quiet is quiet?
- **the settle gesture** — adopt the other's seam · merge · keep-both-with-why
  · defer · walk away mid-settle and return.

## EXPLORE (Sid's own questions)

1. **Entrypoint** — how does one ARRIVE? A list of conversations? Straight
   into THE conversation? What does the first second of this place look like?
2. **The data dump** — how does fifty-blocks-of-a-lived-night first show
   itself? This carries the standing fork: **task-queue face vs cards-wall
   face** — render BOTH as first-class alternatives; the pick happens by
   feel, not argument.
3. **Actions** — what can be DONE here, and what does doing it feel like:
   mark a seam, take the other reader's seam, disagree-and-keep-both,
   annotate why, leave half-settled?
4. **Feel-checks the spec is waiting on**: Sid's messages sub-chunked by
   default — rendered, does it feel right? Headers as their own blocks —
   same question.
5. **Provenance texture** — sid-seams vs agent-seams vs the machine's cut:
   distinguishable at a glance, without a legend?

## MOOD

A reading room, not a dashboard. The material is a lived night and should be
re-livable while being worked on. Disagreement is content, not an
error-state. Nothing auto-resolves. Softland, not app: a place you inhabit,
history as terrain.

## MATERIAL

**Below in this message** — data shapes: the shape of every cast member
beyond the river; readings/seams and the diff (SYNTHETIC — SHAPE ONLY; no
real seams exist until the reads land), real debris samples,
mechanical-edge shape. **Next message** — 64 real river blocks from the
sibling conversation, machine cut and block ids visible, honestly flagged
as the first page of a longer river. The read conversation itself
(`18d63935`) is deliberately withheld until both readings land —
anti-anchoring; don't ask for it, design against the sibling.

## BOUNDS (the only two)

Provenance visible, always. The view never mutates truth — human
write-gestures sketched here are design material, spec'd later.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
fixture: data shapes (verbatim; ids elided to first 8 chars)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

# Data shapes for the design session — what actually resides in the room

2026-07-10 · companion to `a535650e-river-blocks.md` (real blocks). The
designer should know the shape of EVERY cast member, not just the river.
Provenance of each shape below is marked: REAL (pulled from the store/files)
or SYNTHETIC — SHAPE ONLY (the structure is binding, the values are
invented; no real seams exist yet — readings are pending by design).

## 1 · Block (REAL — see the 64 in `a535650e-river-blocks.md`)

Every block: a durable id + actor + form + exact text (address+offsets into
raw stored material, never a copy). Id anatomy:

    du:chat:4c9f171e…:sense-block-v0:000002:00:000001
    └unit┘└conversation object┘ └scheme┘ └event┘└part┘└block┘

Same conversation re-cut by a future scheme = new ids, old ones undisturbed
(strata law). Blocks arrive via an honestly-bounded page: `blocks-returned`,
`river-events-total`, `truncated?`, `page-complete?` — a partial page SAYS
it is partial; the room must have an answer for "there is more river."

## 2 · A reading = seam list (SYNTHETIC — SHAPE ONLY; format is PROTOCOL.md's)

    reader: sid | fable-fresh-read          (provenance, first-class)
    read-at: 2026-07-1X
    seams:
      - n: 1
        quote: "…exact short quote from block text…"
        anchor: du:chat:4c9f171e…:sense-block-v0:000012:00:000000  (resolved from quote)
        flipped: "container granularity stops being the unit here"
      - n: 2
        quote: "…"
        anchor: du:chat:4c9f171e…:sense-block-v0:000002:00:000004
        flipped: "…"
    runs (optional): a word per stretch between seams

Readings land as strata pointing AT block addresses — they never edit blocks.

## 3 · The diff (SYNTHETIC — SHAPE ONLY; classes are PROTOCOL.md's)

| # | sid anchor | fable anchor | class | note |
|---|---|---|---|---|
| 1 | …:000012:00:000000 | …:000012:00:000000 | agree | same event |
| 2 | …:000002:00:000004 | — | missed (fable) | |
| 3 | — | …:000002:00:000001 | extra (fable) | |
| 4 | …:000031:00:000000 | …:000033:00:000000 | moved | 2 events apart |
| 5 | …:000040:00:000000 | …:000040:00:000000 | same-place-different-why | the "why" texts disagree |

Every row is a settle-gesture candidate: adopt · merge · keep-both-with-why
· defer. Settling produces NEW provenance-carrying assertions; the two
original readings stay forever (oxbows, not erasures).

## 4 · Debris (REAL — three events from this same conversation)

Debris is retained truth, unmarkable by default, never in the river stream:

    {type: file-history-snapshot, peek: "{\"messageId\":\"d989503b-…\",\"trackedFileBackups\":{}…"}
    {type: file-history-snapshot, peek: "{\"messageId\":\"9eff02f7-…\",\"trackedFileBackups\":{}…"}
    {type: user, isMeta: true,   peek: "<local-command-caveat>Caveat: The messages below were generated by the user while running…"}

This conversation: 68 debris vs 34 river events — debris usually OUTNUMBERS
river. "Quiet but present" has to survive a 2:1 ratio.

## 5 · Mechanical edges (shape from the relation kernel; 5 exist for this
conversation in the store, not rendered in the block dump)

    {kind: produced | grounds | assembled-from | refines,
     from: <address>, to: <address>,
     asserted-by: <machinery id>, at: <time>}

Same noun a reading-seam assertion uses — everything that claims anything
carries `asserted-by`. An edge endpoint may be a HOLE (referenced, unminted)
— storable, queryable, renderable.
