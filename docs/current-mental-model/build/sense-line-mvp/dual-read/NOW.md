# dual-read round — NOW

## STANDING (frozen at open, 2026-07-10)

Round: **dual-read falsifier** (board thread 1) — SPEC v0 break-quality
falsifier; NOT a benchmark (DIRECTION §0). Material: Sid picked the
dissolution chain (round-opening session, 2026-07-10); read window W1 =
conversation `18d63935`. Docs here: `PROTOCOL.md` (reading rules — DRAFT for
Sid's redline) · `DESIGN_BRIEF.md` (one-paste opener for Sid's UI design
session) · `read-material/18d63935-raw.md` (boundary-free render, both
readers) · `fixtures/a535650e-river-blocks.md` (design fixture, sibling
conversation, machine cut visible).

## Receipt — chain ingest 2026-07-10 (product path, IPC harness)

4 files · 587 observations · 0 parse errors · 1286 containers · status
`:complete`. Per conversation (river/debris = EVENTS; blocks are finer —
`a535650e`'s 34 river events free-cut into 64+ blocks):

| conversation | river | debris | mech. edges |
|---|---|---|---|
| `18d63935` (W1, read window) | 51 | 73 | 12 |
| `a535650e` (design fixture) | 34 | 68 | 5 |
| `fdf0079d` | 51 | 60 | 3 |
| `99f1d713` (own-id events only) | 15 | 105 | 0 |
| `651956e0` (via replays in 99f1d713's file; EXCLUDED from falsifier) | 88 | 10 | 9 |

Braid notes (for the episode/kinds rounds, recorded not fixed — D-001):
(1) fork/compaction files carry events tagged with ORIGIN session ids —
conversation ≠ file; idempotent ids deduped the overlap. (2) summary events
(no sessionId) fall back to PATH-keyed debris-only conversations (river 0) —
adapter wart. (3) chain chronology: `18d63935` is the ORIGIN (Jul 6
20:01–21:41 UTC, both beats live in it), the others are continuations/forks —
openers are retyped variants, not literal replays. (4) IPC-harness data is
ephemeral — the UI arm re-ingests into its own runtime.

## NOW

- 2026-07-10 · Round opened in the fable-max session. Ingest receipt above.
  PROTOCOL.md drafted — awaiting Sid's redline (esp. §what-each-reader-
  produces). DESIGN_BRIEF.md ready for Sid to open the UI design session.
  Next: Sid's read (his pace) ∥ Fable's read (fresh session per PROTOCOL
  §anti-anchoring, after protocol settles) ∥ design session (brief). Board
  thread 1/2 lines updated this session.
- 2026-07-10 · `DESIGN_BRIEF_PASTE.md` built (brief + both fixtures, one
  paste for the claude.ai design session). Tripped Fable 5's safeguards
  TWICE (v1, and v2 with the biology+flagging LOG aside redacted — that
  aside stays redacted in every paste variant; source fixture verbatim).
  Suspected remaining trigger: the paste's agent-transcript SHAPE (labeled
  thinking/tool-use blocks, all-caps imperatives, 65 SHA-256-like ids).
  Split into a bisection pair: `DESIGN_BRIEF_PASTE_A.md` (brief + shapes,
  ~8KB, no transcript structure; message 1) + `DESIGN_BRIEF_PASTE_B.md`
  (river fixture de-transcriptified: labels → `agent`, hex elided, shouts
  lowercased with transport note; message 2). BISECTION RESULT (Sid, same
  day): A passes on Fable 5; B flags. CAVEAT — B's test was CONTAMINATED:
  regenerating from the verbatim source fixture reintroduced the unredacted
  biology+flagging LOG aside (exactly the failure this note warned about),
  so B's flag doesn't cleanly separate that line from the other suspect,
  the material's biology-lab METAPHOR register (24× specimen, 12×
  dissolution, 8× harvest, 7× mutat-, anatomy/cell/strain over 98KB).
  `DESIGN_BRIEF_PASTE_B2.md` carries BOTH fixes: aside redacted + register
  domain-swapped (specimen→exhibit, dissolution→unravelling,
  harvest→gather, mutation→drift, anatomy→layout, ~75 swaps; transport
  note in header; Sid's message text otherwise verbatim, lengths/structure
  untouched). If B2 also flags → structured-synthetic fixture (matched
  lengths/actors/forms, swapped-domain content, "imagine 64 of these").
  Claude Code file-reads don't trip any of this.
- 2026-07-10 · DESIGN ROUND RE-SCOPED BY SID (level ruling, after seeing
  the first render): the reconciliation room (seams/diff/settle) was the
  round's PROCESS apparatus wrongly promoted to design object — deferred to
  a later chapter, after real readings exist. Design round 1 = the
  CONTAINERIZATION VIEW only: raw chat ↔ the machine's block cut, side by
  side; the challenge is the block as visual unit + the nesting grammar
  (blocks top-down in a response · user↔agent loop · turn topology) —
  render multiple faces (outline/roam, canvas/miro, topological tree, …),
  pick by feel. `DESIGN_BRIEF.md` rewritten to v3 accordingly (git has v2).
  Session protocol: fresh design chat → paste DESIGN_BRIEF.md → then
  DESIGN_BRIEF_PASTE_B2.md as message 2. PASTE_A/PASTE (v2-scope) are
  superseded pointers, kept for the safeguard record.
