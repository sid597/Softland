# RETRO — trail-room R-1 (rim + address + kraft marks) · light, at close 2026-07-05

Package: CONTRACT v1 (handoff items 2/3/4/7 from
`design/claude/room-card-lane-2026-07-05.md`); built in the 07-05 HQ marathon;
falsified in the batched `build/git-spine/DIFF_FALSIFICATION_R1.md` (render
section); gated in `build/git-spine/GATE_REVIEW.md` with fixes applied
in-review. Closed under the delivery-mode ruling.

## What the package delivered
- **Item 2**: raw EDN addresses off the card faces; the face address lives
  once in rim chrome; open cards print the element's addressable STRING;
  address stays attached as node DATA (ledger 1 held — paint moved, data
  didn't).
- **Item 3**: rim v0 — four slots (scope · delta · address · palette) in the
  existing 24px strip, trail faces only; editor strip byte-identical (concat
  associativity argument verified in falsification).
- **Item 4**: `:relation-transition` entries render as kraft marks — labeled
  connector when both ends on screen, standalone kraft line NAMING the far
  end when not; asserter badge at band ≥2; open marks get a typographic
  handle, never a box-card.
- **Item 7 / C1–C3**: palette wiring untouched; boot flip still one line;
  sidebar guard added (report-level).

## What worked
- Op-level gates in the pure core (G1–G4 as executable tests) made the
  falsification pass cheap and precise — the one real regression it found was
  exactly at the seam the tests didn't cover (production nil-clip walk vs the
  test's explicit-clip scroll model).
- The kraft-mark form survived contact with real edges unchanged: the design
  handoff's R6 rule ("an edge may never silently vanish") translated directly
  into the two-branch mark constructor and its gate.

## The one real finding, and its lesson
The builder removed the expansion's `clip?` to fix a true bug (gate-6
negative-y bg rects) and thereby silently un-clipped four text sections —
because `rect_tree`'s child-clip REPLACED the ancestor clip rather than
intersecting. **Lesson: when a fix forces a trade between two correctness
properties, suspect the layer below.** The gate fixed the walk (intersect),
restored the expansion clip, and clipped kraft labels the same way; both
properties now hold simultaneously and the fix is behavior-identical for
every other caller. A second instance of the same class (kraft label
overflow) was found AT gate — falsification by class, not by instance, would
have caught it first time (added to the falsification checklist).

## Debts recorded
- Sidebar guard is report-only: `<layout>` reads the sidebar ATOM directly;
  the derived-world guard hides it from mode consumers but not from the
  layout reader (falsification table R-1 #1). R-2 should move the read.
- Rim glitch frame on mode flip (two-watch co-variance under one `m/latest`);
  benign, falls back to editor strip for one frame.
- Kraft far-end labels truncate at card-w at paint; full id lives in node
  data (`:trail-face/off-screen`) — R-2's hover/copy is the consumer, and the
  R-2 contract should state the truncation-vs-naming rule explicitly.
- `⊢` (U+22A2) absent from the atlas; `├` substituted per trap 5. OI-2 slug
  expansion remains the real fix.

## Not in scope, still queued (R-2 tranche)
Band-aware card builders (item 1); lanes-from-edges + move chips (items 5–6,
now unblocked — the git-spine edges they consume exist); binding probe
obligations pre-recorded for R-2: order-as-data + C2-shaped consumer
(PROBE-10K:15,153-155).
