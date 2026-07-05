# Render-Substrate Retro — Track B, step 1 (2026-07-04, Fable)

**Mandate** (baton + Track-B Roam page): as-built adversarial review of the
WebGPU + Electric render layer against ONE question — *can this render the
trail view's two faces (View-3 text + threaded/DAG timeline), and what is
the minimal delta?* Output: verdict + trail-face-ordered fix list +
primitive inventory (→ `PRIMITIVES.md`, same directory). Guardrails
honored: the output is a fix list, not a framework design; ran BLIND to
Track D's ideal-framework docs (`gpu-component-library.md`,
`component-library-jit.md`, and Sid's framework notes were not opened).

**Provenance**: judgment from Fable direct reads (~7k lines: renderer,
electric_flow, trail, rect_tree, runtime + render/state/scroll,
combined_text, shell, file_viewer); two Opus Explore inventory passes for
breadth (substrate internals; sidebar/settings/dg_flow/jit/events/mouse
detail). Every load-bearing claim below traces to a Fable-read line unless
marked ⓘ (agent-sourced, spot-checked). Static review only — the app was
not run; runtime-observable claims are flagged as such.

---

## 1. Verdict

**YES to both faces. No rebuild. The delta is small and mostly above the
substrate.**

- **View-3 text face: renderable today** modulo wiring. Every capability
  it needs — wrapped styled text from data, per-line color/alpha for
  provenance and staleness, scrolling, collapse, a Rama→client data path —
  already exists and is exercised daily by the agent panel, chat pane, and
  editor. The delta is a new mode wired along the exact 7-step path
  `:flow-run` already walked, plus a projection function and one glyph-
  robustness fix (V3-5) that the face's material makes newly load-bearing.
- **Threaded/DAG timeline face: renderable with S/M-sized additions, none
  of them in the GPU substrate.** The card stack, markdown, collapse,
  click-nav, clipped scroll, hover, and drag patterns all exist. What does
  not exist: a lane/thread layout algorithm (pure CPU, new module), edge
  connectors (Manhattan routing from existing thin rects — no new
  primitive needed for the first form), and windowing for long card lists
  (pattern exists in the editor; card variant is new).
- **Rebuild test** (per the guardrail, a rebuild verdict must name the
  trail-face requirement current code cannot meet without disproportionate
  cost): no requirement qualifies. The three closest candidates fail the
  test — *curved/diagonal edges* (avoidable via Manhattan routing for the
  27-04 outline form; a line pipeline is an S/M add-on if Sid wants curves,
  not a rebuild); *proportional/multi-weight fonts* (not demanded by either
  face; bold-as-color is the established idiom); *continuous zoom* (canvas
  face — parked by the face-order ruling; and the camera uniform already
  has zoom in every shader, so even that face starts from "wire it," not
  "rebuild").

Two genuinely surprising as-built facts shrink the imagined delta:
(1) `trail.cljs` (1,337 lines) is a working trail→typed-card projection
with markdown, wrapping, collapse, pending-shimmer, and click-to-navigate
— the chat pane IS a card timeline already, minus threading. (2) The
substrate is a zoomable/pannable world-space canvas in its shaders (pan +
zoom uniforms everywhere) currently driven at zoom=1 with pan=scroll.

## 2. Face demands traced to capability (evidence)

Face demands taken from decisions.md (D-002, face-order ruling) and
`build/trail-view/INPUTS.md` items 2, 3, 5, 13, 14. WP1 data shapes are
Track A's; this trace holds for any row/bundle shape that projects to
text + typed cards + edges.

| Demand | As-built answer | Evidence |
|---|---|---|
| Render a text projection on screen (View 3 visible to Sid) | Wrapped colored lines from data: exists twice (agent overlay `trail->display-lines`; chat pane) | `trail.cljs:1235-1292`, `combined_text.cljs:137-174` |
| Staleness renders differently (INPUTS 2) | Per-glyph rgba + per-card bg/border variants; dim/ghost idioms in use | `renderer.cljs:1132` (per-run color), `trail.cljs:375` (dimmed thinking), ui-list ghost ⓘ |
| Provenance visibly distinct (D-004 asserted-by; both-asserters-badged, INPUTS 10) | Colored chips = rect+text; per-kind color maps already the idiom | `trail.cljs:370-387`, `shell.cljs` header accents |
| Typed cards for decisions/questions/annotations in ONE surface (INPUTS 13) | Typed card builders exist for agent-run kinds; markdown bodies, collapse, nav | `trail.cljs:501-1233` |
| Threaded/forked session DAG (INPUTS 13) | Vertical card stack exists; **threading absent**: no lane layout, no connectors | fix T-2, T-3 |
| Long-material scrolling | Clamped per-region scroll in clipped containers, proven in chat pane | `shell.cljs:144-181`, `scroll.cljs` |
| Interaction: expand/collapse, click-through to source | `:collapse-id` toggle + `:nav` dispatch through rect-tree hit-test, live today | `mouse.cljs:330-360` ⓘ, `rect_tree.cljs:351-394` |
| Every face renders its own address as text (INPUTS 14a) | One more text run; nothing missing | `combined_text.cljs` chrome pattern |
| TODAY's material, near-live (INPUTS 13) | Server→client push path proven ×5 (`Watch*` mirror atoms); pull path proven (`FileContent`) | `file_viewer.cljc:96-133`, `runtime.cljs:122-127` |
| Agent-legible minimal-token View 3 (INPUTS 3) | Not a render-layer demand (it's the projection/query contract, Track A); render side only displays it | — |
| Scale: a session's material on screen | Editor virtualizes; **card stacks don't** (build-all-then-cull) | `combined_text.cljs:263-318` vs agent-2 table ⓘ; fix T-5 |

## 3. Falsification pass — failure modes attempted

Per the review protocol: each mode was pushed until it either broke the
verdict or resolved into a fix-list item.

1. **"Streaming updates re-shape all text every event → jank at trail
   scale."** Traced: any content change → full CPU re-shape + full buffer
   rewrite of ALL content text (`render.cljs:218-226`); sidebar hover or
   one trail token has the same cost. Today's tolerance is empirical (the
   editor at zoom 100 is daily-driven; `[RAF]`>5ms instrumentation exists
   and stays quiet enough to live with). The trail face at full-screen
   density is the same order as the editor screen — but only if visible
   ops are bounded ⇒ windowing (T-5) is the guard, measurement-gated. Also
   found: while a tool card is pending, the shimmer timer forces the full
   re-shape at ~2 Hz (`!shimmer-phase` is watched by the content flow,
   `combined_text.cljs:402`) — current-tolerable, recorded as O-2.
   *Verdict survives; produces T-5, O-2.*
2. **"Card backgrounds bleed outside clipped scroll panels."** Traced:
   `tree->rects` culls only fully-invisible nodes; a card straddling the
   clip edge emits its full bg rect (`rect_tree.cljs:221-228`), and within
   one pool the body draws after the header (tree order) → a half-scrolled
   card should paint over the chat header. Text is clipped (per-op band
   filter) so the bleed is bg-only. **Suspected live defect in today's
   chat pane; not runtime-verified this session** (repro: open a file
   workspace with a long trail, scroll the chat pane, watch the header).
   *Verdict survives (S-sized fix); produces T-4.*
3. **"Ingested material breaks the text pipeline (unicode)."** Traced:
   shaping looks up glyphs by UTF-16 code unit and **advances x only when
   the glyph exists** (`renderer.cljs:1169-1184`) — a missing glyph
   (emoji, curly quotes, box-drawing, any astral-plane char = two
   surrogate lookups) silently vanishes AND collapses the line, desyncing
   every count-based width/wrap/clip/hit computation on that line. The
   editor never hits this (ASCII source); transcripts and markdown WILL.
   This is a map-must-not-lie violation at the pixel level: the face
   would render a claim with characters missing, invisibly.
   *Verdict survives (S fix + audit); produces V3-5 — the only substrate-
   file change in the F0 set.*
4. **"The render loop can't host a face that isn't the editor."** False:
   two full-screen non-editor modes exist (`:flow-intake`, `:flow-run`)
   with their own scroll/click routing; the mode derivation is data-driven
   (`derive-effective-local-world`). The 7-step registration recipe is
   recorded in PRIMITIVES.md. *Produces V3-1 (wiring, precedented).*
5. **"DAG needs primitives the GPU doesn't have."** Edges: axis-aligned
   thin rects are already the separator idiom (hr/table rules/accent
   bars); a threaded timeline's spine+elbow connectors are axis-aligned by
   construction. Diagonals/curves genuinely absent (no rotation, no line
   pipeline) — optional add-on (O-1), Sid's call after seeing Manhattan on
   real material. Junction dots: rects with radius=half. Lane layout is
   CPU work regardless of substrate. *Verdict survives; produces T-2, T-3,
   O-1.*
6. **"Hit-testing/scroll won't survive a new scrolling card surface."**
   The chat pane already composes clip + scroll + collapse + nav through
   the shared walks. Two real wrinkles: (a) chat/flow rebuild their tree
   per click instead of reusing the rendered tree — the sidebar's cached
   `!sidebar-scene` pattern is the as-built correct form; (b) the same
   tree is built twice per change (text walk in `combined_text.cljs:357`,
   rect walk in `editor_compute.cljs:423`). Both are cost/correctness
   hygiene, not blockers. *Produces T-6.*
7. **"Rama→view data path is unproven for query-shaped reads."** Push
   mirror-atom path proven ×5; pull path (`e/server` fn call) proven for
   file content. The trail face's watcher-triggered re-query composes the
   two. Render-side integration is not the risky part; the queries are
   Track A's contract. *Produces V3-2 (thin).*
8. **Async ordering risks for the face** (protocol requires naming them):
   scroll clamps read content heights computed from the same tree being
   rebuilt — a stream update that shrinks content while scrolled deep
   clamps on next wheel event, not on data change (existing behavior in
   chat pane; acceptable, noted for the contract's acceptance gates).
   Trail restore-on-load (`runtime.cljs:191-204`) only fires when
   `!agent-output` is nil — no clobber path found. No new writers were
   introduced by this retro (review only; writers/readers/clearers table
   n/a).

## 4. Fix list (trail-face-ordered; the ONLY build items this retro asserts)

Sizes: S ≤ ~50 lines, M ~50–300, L > 300. Every item names the face
requirement it serves. F0 = needed for the face to exist; F1 = needed for
the face to be honest/usable on real material; O = optional, explicitly
gated.

**Face 1 — View 3 (text projection):**
- **V3-1 (M)** Register the trail-view mode: the 7-step recipe
  (PRIMITIVES.md §recipe) — local-world branch + predicate + build/compute
  fns + two flow branches + render-consumer args + scroll/mouse zones +
  entry command. Requirement: the face exists as a screen (D-002).
- **V3-2 (S)** Data bridge: one `Watch*`/query e/defn + client atom +
  add-watch reconciler, per the ×5 precedent. Requirement: renders TODAY's
  material (INPUTS 13); query shapes are WP1's.
- **V3-3 (M)** Projection fn: WP1 bundle/rows → wrapped colored lines
  (reuse `wrap-line` + per-run colors), staleness→alpha/color mapping,
  provenance→color mapping, per-asserter badges. Requirements: INPUTS 2,
  D-004, map-must-not-lie.
- **V3-4 (S)** Address-as-text: render the face's own query+params line
  (INPUTS 14a). Depends only on WP1's address format.
- **V3-5 (S + asset audit)** Glyph robustness — the one substrate-file F0
  change: missing glyph must still advance and render a visible fallback
  (tofu/`�`), surrogate pairs must not double-count; regenerate the atlas
  charset from the actual ingested material's character histogram.
  Requirement: the face renders foreign material without silent loss
  (map-must-not-lie). Files: `renderer.cljs` shape fns; atlas regen per
  CLAUDE.md font recipe.

**Face 2 — threaded/DAG timeline (adds to the above):**
- **T-1 (M)** Kernel-material card builders: today's `trail->chat-nodes`
  is agent-run-typed (:reasoning/:tool-call/…); the timeline needs card
  types for WP1 rows (session, commit, decision, question, relation,
  dead-end) reusing the same markdown/card machinery. Requirement:
  INPUTS 13 "decisions, questions, annotations, artifacts engage in ONE
  place."
- **T-2 (M)** Lane/thread layout: sessions/forks → lanes, time-ordered,
  fork/merge points marked, dead-ends visually terminal (git-graph-style
  lane assignment; pure fn over WP1 rows). Requirement: D-002
  product-DAG-with-dead-ends; INPUTS 13 threaded session DAG.
- **T-3 (S)** Manhattan connectors: vertical spines + elbow joints +
  junction dots from existing thin rects; per-edge color/alpha carries
  relation kind + staleness. Requirement: the DAG reads as a DAG.
- **T-4 (S)** Clip-clamp partially-visible bg rects in `tree->rects`
  (intersect rect with clip bounds — axis-aligned, cheap). Fixes the
  suspected chat-pane bleed and any card panel the face adds. Requirement:
  honest panel boundaries when scrolling cards (both faces).
- **T-5 (M, measurement-gated)** Height-indexed windowing for card
  stacks: prefix-sum card heights, build only the visible range ±
  overscan (editor windowing is the precedent; variable heights are the
  new part). Gate: build the face, load a real day's material, read the
  existing `[RAF]`>5ms log — implement only if it fires (D-001: measured
  break, not imagined). Requirement: a session's material at daily scale.
- **T-6 (S)** Single-build cached scene for the face: build the rt-node
  tree once per change into an atom (`!sidebar-scene` precedent), flatten
  text+rects+shadows from it, hit-test against the SAME tree. Avoids the
  chat/flow double-build and click-time rebuild. Requirement: clicks
  resolve against exactly what is rendered.

**Optional / explicitly gated (not scheduled):**
- **O-1 (M)** SDF line-segment (and possibly quadratic-bezier) pipeline,
  cloned structurally from the rect system — ONLY if Manhattan connectors
  read poorly on real material (Sid's call at the face gate).
- **O-2 (S)** Isolate the pending-pulse from the content flow (chrome-side
  overlay or dedicated small buffer) — gated on the same `[RAF]` evidence;
  do not pre-build.
- **O-3** Zoom activation (one call-site constant + zoom-aware hit-test/
  scroll math) — belongs to the canvas face, which is parked; recorded
  here only so nobody "discovers" zoom later: the shaders already do it.
- **O-4** Re-enable dirty-present (flag at `render.cljs:16`) — perf lever,
  currently unnecessary; revisit with T-5 evidence.

Deliberately absent from this list: any component library, view-spec
interpreter, retained-scene diffing framework, or layout-engine
generalization. Those are Track-D questions; building them now would
invert D-001.

## 5. Friction observed but NOT fixed (data for the D×B1 delta step)

Recorded as observations, not proposals, per the fresh-cut rule:
- Adding a view touches ~7 hand-wired sites with manually curated
  `m/latest` watch sets (comments in `combined_text.cljs` document "NOT
  watching: X" by hand). Linear cost per face; error mode is a missed
  watch (stale pane) or an extra watch (recompute storm).
- Three parallel clip implementations (rect-tree walk; combined_text
  `clip-sub`/`clip-bottom`; agent-panel band filter) with subtly different
  behavior (glyph-truncate vs op-drop vs band-filter).
- The 0.56 advance constant appears at ~30 sites ⓘ (CLAUDE.md already
  legislates its consistency); any face code must take `char-advance`
  reactively, never the literal.
- Fixed chrome counter-compensating the global camera (`+ scroll-y`)
  works but means every fixed element must know scroll-y; a second
  independently-scrolled region bakes its own offset at build time.
- Six dead ui-primitives components (badge/button/progress/scrollbar/
  tabs/tooltip ⓘ) — built ahead of a consuming form; the face should not
  add to this pile.

## 6. Open doubts

1. **Bleed (T-4) is statically derived, not runtime-verified** — repro in
   §3.2; confirm before/while fixing.
2. **Shaping throughput is unmeasured by me**: the >5ms RAF log is the
   instrument; the T-5 gate depends on reading it against a real day's
   material, not on this retro's estimate.
3. **Atlas charset reality**: V3-5's audit needs the character histogram
   of actually-ingested transcripts/markdown — unknown until run.
4. **Line-granularity text clipping** (ops pop at scroll edges rather
   than pixel-clip): cosmetic on current panes; acceptability on the
   timeline face is a taste call for Sid at first render.
5. **WP1 shape risk**: this retro assumed rows/bundles project to
   text + typed cards + edges. If WP1's contract surfaces a demand outside
   that projection class (e.g., per-glyph provenance *within* a wrapped
   paragraph at arbitrary granularity), the View-3 trace holds but T-1
   sizing grows.
   **RESOLVED 2026-07-04, same day (Track-A WP1 contract session, recorded
   in the baton):** WP1 demands NO sub-paragraph per-glyph provenance —
   anchors are span-level (char/byte ranges), the View-3 text projection is
   plain text (WP1 CONTRACT §8). T-1 does not grow. Additionally, WP1 §8
   took the View-3 text *generation* server-side (`render-bundle-text`), so
   this retro's V3-3 shrinks to marker-based display coloring; and WP1
   §9.8/§12 assigned watcher triggers to the view-MVP package. Both carried
   into the B2 contract (`build/view-mvp/CONTRACT.md`).
