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
- **F-L4 (bug, FIXED same session):** multi-line material previews
  overprinted the staleness/address lines below them — the renderer breaks
  embedded newlines AFTER the tree walk, so one multi-line op escaped both
  the height math and the clip. Previews now emit one op per line (cap 6)
  + a visible "… N more lines" notice.
- **F-L5 (bug + form observation, hygiene FIXED same session; Sid's "wtf,
  this looks garbage" — verbatim, the ledger keeps it):** card text bled
  past card boxes (no self-clip — 120-char addresses ran across the
  canvas), and the lane grid degenerated into diagonal confetti (every
  unthreaded doc = its own lane). Hygiene applied: cards clip their own
  text via a clipped child node; the feed collapsed to a full-width
  single column with threads as a bounded 14px left indent. **The actual
  card design language and the lane/DAG form for threaded material are
  DESIGN-TRACK questions** — first light's job was to surface them, not
  answer them.
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

## Addendum 2026-07-05 — clock ruling

Line 3 of this file called it "the H1 clock-start record". Ruled the same day
(BETS.md verdict log, under Sid's time-box blanket, reversible on his review):
the clock did NOT start at first light — zero typed relations rendered over
real material (F-L2), and the pre-registered KILL text conditions the window
on "relations + timeline working". Arming event, pre-registered: **the first
render showing ≥1 typed relation over real material** (import-derived or
sid-via-agent both count). This file remains the first-light record; the
arming-event entry in BETS.md will be the clock-start record.
