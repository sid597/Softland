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
  paste for the claude.ai design session). First paste tripped Fable 5's
  safeguards (session fell back to Opus 4.8): the river fixture's block [63]
  quotes a LOG.md aside that names biology + Claude-flagging in one breath.
  That aside is REDACTED in the paste copy only (marked in place); the
  source fixture stays verbatim. If the paste is ever regenerated from the
  fixture, re-apply the redaction.
