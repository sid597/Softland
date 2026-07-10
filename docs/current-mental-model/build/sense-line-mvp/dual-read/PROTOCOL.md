# Dual-read falsifier — protocol

Round opened 2026-07-10 (board thread 1) · status: DRAFT for Sid's redline ·
This round is the SPEC's falsifier — break *quality* — NOT a benchmark
(SPEC ≠ BENCHMARK, DIRECTION §0; SPEC.md §16 tail). Machinery conformance
already holds (block-kernel CLOSED 2026-07-09/10).

## Question under test

Does SPEC v0's block grammar carve where sense actually turns? Two
independent readers mark seams in the same material; the diff falsifies or
refines the grammar. No scores, no gold minting, no marker models in this
round. Each disagreement resolves to exactly one of: spec underspecification
(amend), taste divergence (Sid's ruling), or a cut the grammar cannot express
(form-break evidence).

## Material — Sid's pick 2026-07-10: the dissolution chain

- **Read window W1 (default): conversation `18d63935`** — the 2026-07-06/07
  night session where container granularity died and the sense-line direction
  was born. **51 river / 73 debris / 12 mechanical edges** (receipt: NOW.md).
  Why W1: natural boundaries (a whole session — no imposed cut pre-judging a
  seam), both beats in-material ("this is useless" in the opener; "i start
  feeling stupid" mid-arc), human-read size, and out-of-sample for the
  grammar — SPEC v0 was developed against `7c80ce2a` + the spec-sense-line
  chat; `18d63935` discusses schemas as *content* but was never grammar
  material.
- Alternates if W1 disappoints on contact: `a535650e` (34 river) ·
  `fdf0079d` (51 river). **`651956e0` is excluded** (88 river, Jul 8 —
  spec-adjacent in time; contamination risk).
- **Read material:** `read-material/18d63935-raw.md` — event-attributed
  render (SID / claude·thinking / claude·reply), NO block boundaries.
  Declared reductions: tool activity omitted from the render (river per SPEC
  §3.3, but discussion-sparse in this conversation); images rendered as
  placeholders. If a reader's seam wants omitted activity or an image, flag
  it — that is itself a finding about the render, not a reader error.

## Readers & anti-anchoring

- **Sid** reads W1 raw — his medium, his pace.
- **Fable** reads in a FRESH session (not the round-opening session, which
  has already discussed the material's beats). That session boots from
  SPEC.md + this protocol + the raw render ONLY. Known residual exposure, to
  be disclosed in its read record: MEMORY.md auto-loads and names the
  dissolution *event* (not its seams).
- Neither reader sees the other's seams, the machine cut of W1, or the design
  fixture before both readings land. The design fixture
  (`fixtures/a535650e-river-blocks.md`) deliberately uses a *sibling*
  conversation so the UI design session can proceed in parallel without
  anchoring either reader on W1's machine cut.

## What each reader produces — a READING

An ordered list, one line per seam:

    SEAM n · "exact short quote where the turn happens" · what flipped (one line)

Optional: a word naming each run between seams. Grain guidance: mark where
YOUR sense of the conversation turns — not paragraph edges, not topic
changes. Quotes resolve to addresses mechanically afterwards (blocks are
address+offsets; each reading lands as a stratum with reader provenance —
SPEC §14; nothing mutates the material).

## Diff & reconciliation — feeds the UI

Align seams by resolved position: same river event (or adjacent) =
agreement; classify the rest **missed / extra / moved /
same-place-different-why**. The diff table is the reconciliation UI's seed
dataset and the round's primary output. Only the RECONCILED reading may
later mint seed gold for bench-1 — explicitly not this round.

## Done when

Both readings land as files in this folder → diff computed → reconciliation
happens ON the UI (or on paper if the UI isn't ready — the diff then seeds
the UI with real data either way). Round output: the diff + the list of spec
amendments / rulings / form-breaks it forced.
