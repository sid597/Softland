# CONTRACT — trail-room R-1 (rim + address + kraft marks) · v1

Fable-authored 2026-07-05 (HQ session, time-box blanket). Authority chain:
`design/claude/room-card-lane-2026-07-05.md` (sitting-2 rulings + the 7-item
handoff — the demands and CHECKS are binding here), the do-not-preclude
ledger, `decisions.md`, WP-B2 as-built code. Process deviation recorded: no
separate contract-validation round — the demands passed the designer's
falsification pass same-day and every gate below is one of its executable
checks; the diff-falsification round after build stands.

## §1 Scope — handoff items 2, 3, 4, 7 ONLY (the designer's "first" tranche)

- **Item 2 — address off the card face.** Closed cards stop printing the raw
  EDN address; the FACE address renders once, in constant rim chrome; an open
  card prints its element address as the addressable string (never a `pr-str`
  map dump). Address stays attached as DATA on every node (ledger 1) — only
  paint moves.
- **Item 3 — rim v0 in the status strip.** The existing 24px status-bar strip
  becomes, in trail faces only: scope (land + source counts) · delta
  (new-since-last-pull, arrival clock, labeled as such) · address (live) ·
  palette hint. Nothing else joins the strip. Editor mode's strip is
  UNCHANGED.
- **Item 4 — kraft mark rendering.** `:relation-transition` feed entries and
  asserted edges render as the assertion material, never as box-cards: both
  endpoints on screen → labeled connector (`⊢ based-on · import:git-spine` at
  band ≥2); endpoint off screen → a standalone kraft line at its time
  position naming the far end (an edge may never silently vanish).
- **Item 7 — constraints C1–C3 hold**: palette reachable over the face
  always; face address in rim (not floating card text); boot default stays a
  one-line flip — nothing added may require editor mode mounted before the
  trail renders.

NOT in scope (R-2, a later tranche): item 1 band-aware card builders; items
5–6 lanes-from-edges + move chips (they consume tonight's git-spine edges).
No server files, no trail_view.clj (a parallel builder owns it), no kernel
work, no new faces.

## §2 Traps (naive → failure)

1. Rim rendered in shared chrome unconditionally → editor mode wears trail
   furniture. Rim slots render only when a trail face is the mode.
2. Removing the printed EDN by deleting the address from node data → ledger 1
   (address at one gesture everywhere) breaks and R-2's hover/copy has no
   data. Paint-only change.
3. Kraft connector that skips when an endpoint is off screen → the map lies.
   Standalone kraft line rule is mandatory, with the far end named.
4. New text measured with anything but the 0.56 advance → drift (CLAUDE.md
   invariant, all files listed there).
5. Glyph assumptions: verify `⊢ │ ├ └ •` exist in the CURRENT merged
   `font_atlas.json` BEFORE using them; if any is missing, use the nearest
   covered glyph and RECORD the substitution in the phase artifact (OI-2 slug
   expansion is the real fix; do not regen the atlas in this package).
6. Changing hit bounds while changing paint → clicks break at band
   boundaries. R4 rule: rt-tree node bounds unchanged; paint only.
7. Electric/Missionary: no side effects in `m/latest`; follow CLAUDE.md
   patterns for any wiring touch.

## §3 Gates (each is the handoff's own check, made executable)

- **G1** (item 2): pure-core render ops for `/trail timeline` contain exactly
  ONE face-address text op, attributed to the rim, at any scroll offset;
  closed-card op lists contain no EDN-address text op; an open card's ops
  contain the addressable string.
- **G2** (item 3): rim ops show exactly four slots on both trail faces;
  editor-mode strip ops unchanged (snapshot compare).
- **G3** (item 4): a `:relation-transition` fixture entry yields connector
  ops (no rect-fill card ops) when both endpoints are in view, and a kraft
  line op naming the far end when one endpoint is outside the viewport;
  asserter badge present at band ≥2.
- **G4** (item 7): palette-open wiring over the trail face untouched (test or
  snapshot); the boot-default flip remains a single line (assert the exact
  form still exists where the builder found it, cite in artifact).
- **G5**: cross-package suite green in one run (28 tests / 909 assertions +
  new); shadow :dev compiles.
- **G6**: `file(1)` says text for every touched file.

## §4 File allowlist

AMEND only: `cards.cljc`, `lanes.cljc` (connector emission if it lives
there), the trail-face wiring/render cljc/cljs under
`src/app/client/workspace/trail_face/`, `workspace_actions.cljs` (sidebar
never auto-appears in trail faces — guard only), and the single file that
renders the status strip (builder locates it, names it in the phase
artifact before editing). NEW: test additions in the existing trail-face
test namespaces. NOTHING else — `trail_view.clj`, server files, kernel
files, `renderer.cljs` are other builders' or out of scope.

## §5 Builder duties before code

1. Re-read `room-card-lane-2026-07-05.md` §§1–2 and the handoff items 2/3/4/7
   in full — the doc is the authority on intent; this contract only scopes.
2. Verify the as-built cites: mode derivation in `workspace_actions.cljs`
   (`local-world-mode`, `/trail timeline` wins / `/trail off` clears), the
   four-line card passport paint sites in `cards.cljc`, the status strip's
   render site, the palette-over-face wiring.
3. Verify glyph coverage (trap 5) against `resources/public/font_atlas.json`.
4. Confirm which feed entry shapes exist for relations TODAY
   (`:relation-transition` rows) so G3's fixture matches reality — grep the
   trail-face fixtures, do not invent shapes.

## §6 Stop clause

Standard: unbuildable / conflicts with a binding doc → stop, classify,
escalate to HQ; never improvise policy. Pre-flagged implementer-fixable: the
status strip may be entangled with editor state — if isolating it needs more
than a mode branch, do the minimal mode branch and record the debt. Commits
are HQ's; report gate evidence (op-level) in the phase artifact
`build/trail-room/PHASE_R1.md`.
