# FIRST_LIGHT.md — view-MVP WP-B2 (2026-07-05, Sid live)

The H1 clock-start record (CONTRACT §11, post-gate). D-001 form-break
evidence for the next slice accumulates HERE.

## What happened

Sid ran `clj -A:dev -X dev/-main`, opened the app, invoked
`/trail timeline`. Server log: **initial sweep 167 of 167 imported**
(docs/current-mental-model + vision), live watchers running on both roots.
The timeline face RENDERED over real material — screenshot in session
record. First light is REAL: cards with two-clock stamps
(`arrived 2026-07-04 · claimed unknown` — the honest nil on md ingest),
per-card resolvable addresses, expansions with mechanically-truncated
material previews (`… 7327 chars truncated`), staleness line
(`attested never · walked unknown`), the merged-atlas glyphs drawing
(↓ kind glyph, middots). In-process cluster boot + 167-doc sweep completed
in seconds.

## What jarred (findings; the ledger this file exists for)

- **F-L1 (bug, FIXED same session):** newlines in material previews
  rendered as U+FFFD pairs (`••`) — the sanitizer checked control
  whitespace against atlas coverage and tofu'd it. Fix: codepoints < U+0020
  pass through untouched; the renderer owns line breaks. Regression
  assertion added to gate 4.
- **F-L2 (form finding, MITIGATED same session):** every md doc is its own
  family → one lane per card → the timeline staircased off-screen right
  after ~6 cards. Mitigation: lanes wrap at viewport capacity (mod
  max-lanes); spines still group by UNWRAPPED thread so unrelated docs
  never chain visually. The deeper form question — what IS the right lane
  semantics for a corpus of unthreaded docs — is genuine D-001 material
  for the next slice, not something to invent now.
- **F-L3 (expectation, no action):** card titles are full content-hash ids
  (live entries carry no display-name — gate-16 finding). Readable names
  need a display-name projection upstream (WP1-side enrichment; goes to
  the importer-enrichment gap already declared in the feed's standing
  omissions).

## Gate 15 status

`MEASUREMENT_RAF.md` PENDING: needs the browser-console `[RAF]` numbers
from Sid's live session over the real corpus (167 cards, all built —
windowing is evidence-gated on p95 dirty-frame > 5ms). Also pending from
Sid: whether scroll/expand FEELS instant, and the screenshot →
re-resolve check (paste a rendered address back through the CLI and
compare).

## Named follow-ups (recorded, not silently absorbed)

1. Slug glyph-set expansion (OI-2 ruling: merged MSDF is TEMPORARY; slug
   is the destination).
2. Transcript live-watch needs offset-incremental jsonl re-read (1.2 GB
   dir; whole-file re-read per change event is the current cost).
3. Production OC runtime ownership (OI-1): the in-process defonce-delay
   cluster is non-durable — every server restart re-boots + re-sweeps.
4. Display-name enrichment for md objects (F-L3).
