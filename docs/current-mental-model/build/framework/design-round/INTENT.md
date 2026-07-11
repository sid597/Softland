# Design-round pull record — Block Views (2026-07-11, W2 orchestrating session)

Pulled via DesignSync from the claude.ai design project `4f144e22-a652-412b-8ca7-4f189e83d49f`
(the project Sid's 2026-07-10 vision-log entry points at). Files landed here
verbatim, untruncated: `BlockExplorer.dc.html` (63,092 chars — the six modes'
actual anatomy, one component, `mode` param) · `data.js` (48,868 chars — the
stand-in dataset whose shapes are the template contract). The frame page
(`Block Views.dc.html`) was pulled in-context; its per-candidate intent +
judge lines are quoted VERBATIM below (that page adds no other anatomy).
`support.js` (dc runtime) and `Reading Room.dc.html` (project `1d15ae7c…`,
not picked) were not pulled.

Frame-page header, verbatim: "Every frame below is the same two-pane,
read-only view: left, the raw session as a chat log reads; right, the block
structure — block → response → turn → conversation. Same stand-in dataset
everywhere (30 turns, 149 events, one 1,400-word thinking block, one
139-line test log, a 5-grep spree, all-caps shouting). Only the right pane's
representation changes. Click any block or any message to pin it and scroll
its counterpart into view. Tool/system noise is dimmed, never deleted; every
view ends with an explicit 'page 2 not loaded' answer."

## The six candidates, verbatim (intent line · judge line)

- **1a Outline** — "indentation is the structure; text stays primary; long
  blocks collapse to measured stubs (▸ 812 words) you expand in place." ·
  judge: "scanability at density · whether four nesting levels read without
  labels · how collapsed thinking/logs feel next to one-liners." **(worn, W1)**
- **1b Canvas** — "turns are frames on a spatial field, threaded in sequence;
  user column beside response column; card size ∝ text length." · judge:
  "whether turn shape (tool-heavy vs prose-heavy) is legible at a glance ·
  the cost of clipped text · 2-axis scrolling."
- **1c Tree** — "the pipeline's-eye view: every parent→child edge drawn, one
  node per block, text demoted to one-line previews." · judge: "structural
  honesty vs readability — is a topology with preview-only text useful for
  this data, or too far from it?"
- **1d Score** — "the conversation as strata: height ∝ words^0.62, columns =
  nesting depth, labels only where they fit; full text in the inspector
  below." · judge: "rhythm — monster blocks and tool sprees should be
  visible as shape · whether inspector-on-click is enough text access."
- **1e Boxes** — "containment made literal — turn ⊃ user-message | response
  ⊃ blocks, russian-doll frames with headers; long blocks clip at ~6 lines."
  · judge: "whether explicit frames beat indentation for parent/child ·
  frame overhead vs reading flow at 30 turns." **(PICKED for W2, Sid
  2026-07-11)**
- **1f Minimap + Reader** — "navigate by density, read at full fidelity:
  proportional strip of all 30 turns on the left, one turn fully expanded on
  the right." · judge: "the only view where an 800-word thinking block is
  just… readable · is one-turn-at-a-time too narrow a window?" **(PICKED for
  W2, Sid 2026-07-11)**

## Transcription notes (contract §16)

- The `.dc.html` interaction verbs (click-to-pin, inspector-on-click,
  expand-in-place) are `:actions`-class — OUT of v0 assemblies (CONTRACT §4
  reserved shape). Transcribe the READ anatomy; log the interaction verbs as
  named lacks per G23.
- The two-pane chat-log-left frame is the EXPLORATION harness, not the face:
  the land's face is the right pane worn full-screen (the W1 outline
  precedent). Minimap+Reader's own two panes (strip + reader) ARE its
  anatomy and stay.
- v0 data lacks vs the stand-in dataset: single ≤64-block page (honest
  `:paging-lack`), no per-block word counts unless computed by a primitive
  (measure rule), no dimmed-noise classes beyond `:kind`. Each mismatch the
  transcription hits is a G23 named lack, not an improvisation.
