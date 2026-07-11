# W2-E — the plurality (lane artifact, gates G22–G23)

2026-07-11 · lane W2-E (Opus subagent, fresh context; host-restart mid-lane —
re-verified clean tree, nothing stale built on). Binding: CONTRACT v2 §§4, 5,
6, 16, 17, 19. Design inputs: `build/framework/design-round/` (INTENT.md +
BlockExplorer.dc.html + data.js). Sid's picks transcribed: **1e Boxes** +
**1f Minimap + Reader**.

## Gate results (RUN in-context, not claimed)

- Lane suite `app.face-transcription-test`: **9 tests · 234 assertions · 0
  failures · 0 errors** (G22 + G23 + the extension regression).
- Full face regression (W1 suites + lane E): `face-assembly` +
  `face-primitives` + `face-projection` + `face-integration` +
  `face-transcription` = **32 tests · 529 assertions · 0 failures · 0
  errors** — the interpreter extension and vocabulary additions regress
  nothing (W1 goldens unchanged, G7 pin green over the grown file).
- **G22 PASS**: both faces compile CLEAN against the real registry (V1–V7;
  the W2 §17 envelope keys ride V1's namespaced tolerance); golden rt-trees
  committed (`test/app/fixtures/faces/{boxes-face,minimap-reader-face}.golden.edn`,
  regen = explicit `regen-goldens!` act); apply-report **zero/zero** on both;
  positive `content-h` = root height; both flatten through
  `tree->rects`/`tree->text-ops`; all four new prims carry measured goldens
  (G8 discipline: wrap-once via `wrap-line`, `:text-layout` nowhere — the
  whole-file source scan in `face_primitives_test` covers the additions —
  non-zero non-overlapping bounds after `resolve-layout`, the ONE
  `fallback-char-width` constant).
- **G23 PASS**: named anatomy checklists (below) asserted against the golden
  trees — presence + relative geometry, each item citing its design source
  line; every inexpressible element logged as a named lack (D-005), none
  improvised around.

## Files touched (fence honored — nothing else)

- `src/app/client/workspace/face_primitives.cljc` — additions only (G7-pinned
  copies untouched; pin re-verified green).
- `src/app/client/workspace/face_assembly.cljc` — the ONE named interpreter
  extension (below).
- `resources/public/faces/boxes-face.edn` · `minimap-reader-face.edn` (new).
- `test/app/face_transcription_test.clj` (new).
- `test/app/fixtures/faces/plurality-conversation.edn` + the two goldens (new).

## New primitives (each exists for a named design element)

| prim | design element (BlockExplorer.dc.html) |
|---|---|
| `:box` | the russian-doll containment frame — turn frame :277, user frame :285, chunk card :296, response frame :309, block card :319; strip group card :358, reader frames :385/:412. `:card` cannot express it (the G7-pinned `ui-card` copy has no `:layout`, children would not stack). Column layout + padding/gap, measures bottom-up (T7). |
| `:header-band` | the header strip every frame opens with — :278-283, :286-293, :310-316, :320-329, :355, :374-383. Leading role dot (:287), bold label (+ `:label-prefix` for the design's "T"/"turn " literals :360/:377), muted meta RIGHT-ALIGNED (the `flex:1` spacer :281); `:kind` derives dot+label color from the palette (b.color, :321-322); `:words-of`/`:count-of` compute counts INSIDE the primitive (INTENT.md rule). |
| `:text-clip` | "long blocks clip at ~6 lines" (1e intent) — the showClip branch :333-338 + the measured "▸ N …" stub :488. One wrap, first `:max-lines` ops + one honest "▸ N more lines" op; `:data {:text/clipped? :lines-total :lines-shown}` so the tree never lies about the cut. |
| `:sliver` | the minimap density bar — "navigate by density"; h = `max(2, min(26, round(√words·0.75)))` verbatim from :795 (the strip header names the law, :355); kind-coded color :814; words computed in-primitive; `:data {:sliver/words}` so tests assert the LAW, not a magic height. |

Support defs: `words-in` (private), `kind-color` (private), `sliver-palette`
(public — hexes ported from the design TYPE map :448-456 at the design's 0.6
sliver alpha; unknown kinds → neutral default).

## The named interpreter extension (TAKEN — flag for INT)

**Child `content-w` narrowing** (W1-INT Lack 2; pre-authorized in CONTRACT
§16 as the ONE `face_assembly.cljc` edit, "if a transcription genuinely
needs it"). Both faces genuinely need it: Minimap's strip/reader split means
every descendant wraps at a different width than the pane; Boxes' nested
frames wrap prose at frame width (the W1 worn-face symptom, mitigated then
by a global 32px inset). Shape: a prim node's resolved props may carry
`:child-w <n>` (absolute px — the design's fixed 152px strip) or
`:child-inset <n>` (relative subtract — padding/border-relative frames,
responsive). The SUBTREE builds against the narrowed geom; the node itself
builds at the parent's width; numbers only, the arithmetic lives in the
engine (§4 guard intact — zero grammar change, V2/V5 untouched); follows the
§5 `:props {:id ..}` interpreter-read-prop precedent. Regression in
`g22-child-content-w-narrowing` (no-props = W1 behavior byte-identical;
relative; absolute; propagation through `:each`). Two forms instead of one
is the minimal honest cut — each is used by a shipped face; collapsing to
one would force either a non-responsive reader or hand-computed inner
widths in EDN.

## G23 anatomy checklists (as asserted; cites = BlockExplorer.dc.html lines)

**Boxes (1e)** — B1 one bordered radius-10 turn frame per turn, id travels
with the turn (:277) · B2 every frame opens with a tinted, hairlined header
band: "turn N" + speaker + right-aligned "N blocks" (:278-283) · B3
containment made literal: every block card's absolute bounds nest strictly
inside its turn frame (intent; :285-343) · B4 block-card header: kind-colored
dot + kind label + computed "Nw" meta chip (:320-329, :486) · B5 the monster
prose block and the 10-line code block clip at 6 lines with a visible "▸ N
more lines" stub; one-liners stay whole (intent; :333-338, :488) · B6 16px
frame rhythm, non-overlapping (:277) · B7 the view ENDS with the explicit
paging answer — `:conversation/paging-lack` rendered (frame-page verbatim;
:347) · B8 every filled rect inside the pane width.

**Minimap + Reader (1f)** — M1 two panes: fixed 152px strip (right hairline)
at the left edge, reader at x=160 w=600, sum = pane (:353-354) · M2 strip
header names the density law "map · h ∝ √words" (:355) · M3 one group card
per turn: "T{n}" + computed per-turn "Nw" + one sliver per block (:357-368) ·
M4 every sliver height = the :795 formula from its own computed words; the
monster block visibly taller than the heading (density IS shape) · M5
kind-coded sliver colors (:814) · M6 strip ends with the page-2 honesty box
(:370) · M7 34px reader top bar naming the focused turn + speaker + words
(:374-378) · M8 the reader shows exactly ONE turn — card count = focused
turn's block count; other turns' prose provably absent from the reader ·
M9 full fidelity: the 800-word-class block renders EVERY wrapped line
(`:text-run`, never `:text-clip`) (:422-424 + the 1f judge line) · M10
wrap-width honesty at face grain: slivers never leak from the strip; every
text op ends inside the pane.

## Named lacks (G23/D-005 — ordered work, never improvised around)

1. **Pair structure (data — THE G25 evidence item)**: the design's
   `turn ⊃ user-message | response` layer is inexpressible — v0 `:turns` are
   single-speaker with flat `:blocks`, so Boxes' 4-layer russian doll
   flattens to 3 and the reader loses its user/response split. Both picked
   faces WANT block-role/pair structure → this is exactly the evidence G25
   says orders the machine-cut package.
2. **`:reader-turn` focus selection (data, Minimap)**: the `:conversation`
   projection serves no focus turn; candidate = a request `:params` key
   served as `:reader-turn` in the data-context. The fixture models the
   post-lack shape (documented in-file); worn over the real projection today
   the reader renders an honest gap with `binds-missing` counted — the strip
   works immediately.
3. **Interaction verbs (`:actions`-class, v0-out — INTENT pre-named)**:
   click-to-pin + cross-pane scroll sync, expand/collapse toggles, ‹›
   turn nav (CONTRACT §4 reserved shape).
4. **Event ranges (data)**: "events 12–47" chips — no event numbering in the
   data-context.
5. **Noise-class taxonomy (data)**: toolish dimming (rowOp 0.62 / sliver
   alpha 0.3) and hatch fills ride a kind-class the data lacks (`:kind`
   only); everything renders at one honest alpha.
6. **Conditional display (grammar-adjacent, v0-out by design)**:
   expandable-only-when, clip-only-tool-results>20-lines, the at-end "next
   turn is on page 2" chip — conditional show/hide is logic; handled
   honestly (uniform clip rule in Boxes, full text in reader, page box
   always present).
7. **Style vocabulary (cosmetic)**: dashed borders, hatched "page 2" fill,
   clip fade gradient — solid muted equivalents used; the ▸ stub line is the
   fade's honest replacement.
8. **Provenance**: NO `:assembly/birthed-by` — the design-round conversation
   is not in the OC; a fake edge would make the map lie (per lane
   instructions, the named provenance lack).
9. **Word counts (data, MITIGATED)**: computed inside primitives per
   INTENT.md; projection-served authoritative counts stay a candidate.
- Pre-named `:pad-after` (W1 Lack 3): **not needed** — between-gaps + bottom
  padding covered both faces.

## Judgment calls

- Handles/names: `boxes-face` / `minimap-reader-face`; file basenames match
  the handles (§17: identity keys on the NAME; also keeps the W1
  `/face <name>` HTTP fetch working pre-arsenal).
- `:assembly/author "claude:w2-lane-e"` — machine-transcribed from Sid's
  by-feel pick; honest actor, not Sid's.
- Envelope carries §17 `:assembly/status :candidate` + `:assembly/author`;
  W1 interpreter tolerance proven by compile-clean.
- Turn-frame header meta = "N blocks" (single meta slot; per-turn words live
  in the minimap strip where the design puts them).
- Double division in `:header-band` centering so goldens stay
  `clojure.edn`-readable (no ratio literals).
- 1px inner padding on framed boxes so children sit inside the border
  stroke.
- Page-end honesty binds `:conversation/paging-lack` directly (a nil lack
  renders an empty line — conditional hide is v0-out, lack 6).

## INT flags

1. `face_assembly.cljc` carries the named extension — include it in the
   G26 falsification classes (narrowing × `:each` × error paths).
2. Minimap worn over the REAL projection shows an empty reader +
   `binds-missing > 0` until lack 2 lands — expected honest degradation, not
   a defect; decide at INT whether the first wear waits on the `:params`
   focus key (small projection addition, lane-D-owned file post-wave).
3. cljs parity eyeballed only (new code is interop-free: `Math/sqrt`,
   `subvec`, `re-seq` — all cljc-safe); browser confirmation rides W2-INT
   per the W1 precedent (the `guard-reason` lesson).
4. Lane D's parallel edits were present in the tree during my full-suite
   run (green, 32t/529a); the wave-end ONE serial suite at committed HEAD
   remains the binding re-check (G26).
5. G24 wear path: `/face boxes-face` and `/face minimap-reader-face` are the
   handles.
