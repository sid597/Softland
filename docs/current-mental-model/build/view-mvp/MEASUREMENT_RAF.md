# MEASUREMENT_RAF — WP-B2 gate 15 evidence (Sid's first boot, 2026-07-05)

Collected at Sid's app boot (`clj -A:dev -X dev/-main` → `/trail timeline`),
the first boot to run the full git-spine sweep (replay → spine-sync →
extract) over the real repo + transcript corpus. WP-B2 was gate-PASSED
2026-07-05 pending exactly this evidence; **CLOSE proposed below, Sid
ratifies.**

## The [RAF] numbers (browser console, verbatim)

Viewport 3668×1371 @ dpr 1.046875, msdf backend, merged atlas
(`ubuntu-dejavu-merged`), snap on. Two consecutive frames during active use
of the trail timeline over the FULL ingested corpus (~180+ commit cards +
md/doc/transcript entries):

```
[RAF] prep: 0.0 ms | text-gpu: 11.3 ms | rects-gpu: 0.9 ms | draw: 0.2 ms | TOTAL: 12.4 ms  (frame 90: 126 rects, 1168 content instances, 53 content lines)
[RAF] prep: 0.0 ms | text-gpu:  6.1 ms | rects-gpu: 0.8 ms | draw: 0.2 ms | TOTAL:  7.1 ms  (frame 91: 141 rects, 1089 content instances, 51 content lines)
[EDITOR-POOL] ordered-diff: 0.50 ms | added: 1  updated: 122 freed: 0 writes: 123
[EDITOR-POOL] ordered-diff: 0.40 ms | added: 15 updated: 126 freed: 0 writes: 141
```

**Verdict on the numbers: PASS.** 7–12ms total per frame at ~1,100 glyph
instances on a near-4K viewport — comfortably inside the 16.6ms frame
budget; the pool diff is sub-millisecond; text-gpu dominates as expected for
the msdf path.

## Feel verdict (Sid, verbatim)

> "idk how this ui works does not make sense to me"

Recorded honestly. Diagnosis (agreed at the sitting): the ILLEGIBILITY is
the lane interim degenerating at corpus scale — family-key gives every
commit its own lane → staircase cascades with wrap resets — EXACTLY the
degeneration the designer pre-named (room-card-lane R7: family-key-as-lane
"was an honest interim; it retires as lane semantics when edges land").
The R-2 contract (lanes-from-edges + the band) is the standing cure and this
boot is its form-break evidence. The verdict indicts the interim, not the
thread design — the render machinery itself (this gate's subject) held.

## First-light defects found at the boot (fixed same sitting, separate commits)

1. **Sidebar ambient in the trail face** (home-directory tree, no less) —
   R-1's guard was report-only; `<layout>` read the raw atom (the known W-1
   debt, confirmed live). FIXED: layout consumes the derived judgment
   (trail face → sb-w 0); the raw atom is untouched so leaving the face
   restores the sidebar as it was.
2. **Incoming edges printed as outgoing** in the card expansion — a commit
   showed `-> produced <its own id>` ("produced itself"). G11's sibling at
   the card level. FIXED: direction is relative to the expanded card
   (`->` outgoing naming :to; `<-` incoming naming :from); test added.
3. (Retracted) two blank-white screenshots were a screenshot-tool artifact,
   not a render crash — clicks during the still-running first sweep simply
   showed partial content, which is the designed honest behavior.

## Open items (non-blocking, tracked)

- **`content-same?: false` on consecutive frames** — the `identical?` idle
  skip did not fire during the captured window. Frames 90/91 were during
  active interaction (content lines 53→51), so an idle steady-state skip is
  UNVERIFIED at corpus scale, not disproven. Falsifier: idle the trail face
  10s, read the skip ratio from the [RAF] cadence. Matters for battery, not
  for the 60fps budget (12ms worst observed).
- **"claimed unknown" on every commit card** — commits HAVE a claimed clock
  (committed-at rides the import request's :time-ms), but the feed's
  source-ingested rows carry `claimed-ms nil`. Data seam for the WP2
  follow-up / R-2's band-2 line (when·who needs it).
- **Kraft mark visibility at corpus scale** — the marks render (thin
  segments visible at some rows' right edges) but amber labels were not
  confirmed legible in the screenshot; R-2's thread/band re-layout changes
  where marks sit anyway. Re-check at R-2 first light.
- Screenshot→re-resolve half of gate 15: the expanded card PRINTS its
  element address (visible in the H1 candidate screenshot); a live
  re-resolve of that address is deferred to the R-2 window (the address
  affordances tranche).

## H1 arming candidate (separate ruling, Sid's)

`vision/images/2026-07-05-h1-arming-candidate.png` — the expanded commit
`2232fbb` showing `-> based-on … by import:git-spine` (×2, a merge commit)
over the real repo. Under the H1 ruling ("arms on the FACT of a typed
relation rendering over real material; the kill text does not require the
kraft pixel form"), this render QUALIFIES as the arming event. Sid ratifies
in the BETS verdict log; the kraft-connector form remains the preferred
pixel for the record shot once R-2 lands.

## Disposition

Gate-15 evidence collected: [RAF] lines PASS + feel verdict recorded + the
H1-candidate screenshot preserved (re-resolve deferred, named above).
**WP-B2 CLOSE proposed on this artifact; Sid's ratification closes it.**
