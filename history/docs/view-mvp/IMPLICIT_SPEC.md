# View-MVP IMPLICIT_SPEC — WP-B2 Phase B2-P0 (fresh-context re-derivation)

Status: derived artifact (NOT binding). On any conflict with
`build/view-mvp/CONTRACT.md` (v1 BINDING) the contract governs; genuine
two-readings-can't-both-hold conflicts are stop-clause events recorded in
§5 Findings, never silently resolved here.

Provenance: re-derived cold from `build/view-mvp/CONTRACT.md` (entire),
cross-checked against `build/trail-view/CONTRACT.md` (WP1, BINDING — every
"WP1 §n" citation verified against the WP1 text), the render-substrate
`RETRO.md` + `PRIMITIVES.md`, `CHARSET_AUDIT.md`, `decisions.md`
(D-001/002/004/008 + the 2026-07-04 face-order & object-kernel-revision
rulings), CLAUDE.md Missionary/Electric law, and
`memory/implementation-quirks.md`. Every source-fact the contract cites was
checked against the on-disk source under `src/app/`; MATCH/MISMATCH tally in
§6. This is QC layer 1 (contract coherence). Per the F2 lesson: **every
operation and invariant below carries its placement** (which §2.1 cljc file /
`wiring.cljs` / `file_viewer.cljc` / `ingest_watchers.clj` / amended shared
file it lives in) and the data-surface rule (WP1 §7 wrappers ONLY, plus the
one carve-out named in INV-S1a). Placement-free prose is the failure mode
this layer exists to avoid.

Note on notation: codepoints are written `U+XXXX`, never as raw glyphs, so
this file stays plain and no decode surprises can inject bytes. No raw
control byte appears anywhere; control chars are spelled `\uXXXX` in
backticks (S4 discipline applied to this doc too).

---

## 1. Operations (every behaviour the package must implement)

Grouped by placement seam. `OP-n` ids are referenced by the §3 matrix.
"Data surface" on each op names the ONLY inputs it is allowed to read
(WP1 §7 wrapper results / geometry / local view-state), enforcing INV-S1.

### 1.1 `trail_face/sanitize.cljc` — codepoint sanitizer (pure cljc; V3-5 client half)

- **OP-1 Coverage-set load.** Read the loaded font's coverage set from the
  glyph table of `resources/public/font_atlas.json` (the `glyphs[].unicode`
  field, per CHARSET_AUDIT §8) into a set of codepoints. Data surface:
  atlas JSON (asset), passed IN as data — the pure fn takes the coverage set
  as an argument; the *loading* is done by `wiring.cljs` (INV-S2). Contract:
  §2.1 `sanitize.cljc` "coverage-set lookup"; gate 4 "Coverage set loaded
  from the REAL `resources/public/font_atlas.json`".
- **OP-2 Codepoint-wise sanitize of an op.** Given a text op and the coverage
  set, walk the op text by Unicode **codepoint** (surrogate-pair-safe, NOT
  UTF-16 code unit — the as-built shaper's `charCodeAt`-on-`(seq text)` bug is
  what this fixes): each covered codepoint passes through; each uncovered
  codepoint is substituted with the fallback glyph U+FFFD; the codepoint
  **count is preserved** and positions preserved (never dropped, never
  zero-width). Contract: §3.9 "sanitized codepoint-wise … uncovered render
  the fallback glyph … never vanish. Advance counting is codepoint-correct
  (surrogate pairs = one advance)"; trap 13; gate 4.
- **OP-3 Codepoint advance count.** Provide the codepoint count of a
  sanitized op (== rendered advance count) so downstream clip/wrap/hit math
  in `rect_tree.cljc` and `scene.cljc` uses codepoint counts, not UTF-16
  `count`. Contract: §3.9; trap 13 "count vs codepoint"; gate 4 "advance
  count == codepoint count".

### 1.2 `trail_face/text_face.cljc` — View-3 face (pure cljc)

- **OP-4 Projection → line ops.** Take WP1 `render-bundle-text` output (a
  string, WP1 §8; NOTE: text is generated SERVER-side per WP1 §8 — the face
  does NOT re-derive it, RETRO §6 RESOLVED) and split it into per-line text
  ops, **line-for-line**: same line count, same per-line codepoint sequence,
  no re-wrap (horizontal overflow scrolls). Data surface: the string result of
  the `render-bundle-text` WP1 §7 wrapper. Contract: §3.2 verbatim law;
  trap 3; gate 2.
- **OP-5 Marker → style table.** One marker→style table keyed to the
  `;; trail-text v0` version header (WP1 §8). Each recognised line-shape marker
  (`==`, `--`, `->`, `<-`, verdict `via`, `(current)`, `(disagrees)`,
  `walked unknown`, `attested`, `omissions`, address header) maps to a style;
  unknown line shapes map to `:normal` (rendered, never dropped). Contract:
  §3.3; trap 12; gate 3.
- **OP-6 Version-header guard.** If the first line's version header does not
  match the table's key (`;; trail-text v0`), render the WHOLE projection
  `:normal`/plain and emit exactly ONE visible notice line. Contract: §3.3
  "version-header mismatch renders the whole projection plain + one visible
  notice line"; trap 12; gate 3.
- **OP-7 Address header op.** Render the face's own address (the WP1 §8
  header `;; address: (trail/context-bundle {..})`) as one text line in the
  header. Data surface: `:bundle/address` on the WP1 wrapper result. Contract:
  §3.1 address law; gate 1.
- **OP-8 Marker coloring is total.** Apply style→per-run rgba on every line
  (reuse renderer per-run color; RETRO V3-3 shrank to marker-based display
  coloring). Contract: §3.3; gate 3.

### 1.3 `trail_face/cards.cljc` — kernel-material card builders (pure cljc; separate builder set)

- **OP-9 Feed-entry card.** Build an rt-node card per WP1 §6 feed entry:
  kind glyph + display name + two-clock stamps + actor badges + address line.
  Data surface: WP1 §6 `<entry>` shape (`recent-activity` wrapper result).
  Card kinds are a SEPARATE set from trail.cljs agent-run kinds
  (`:reasoning`/`:tool-call` NOT reused). Contract: §4 Nodes; §2.1
  `cards.cljc`; trap 10; T-1.
- **OP-10 Two-clock stamp rendering.** Render BOTH `:time/claimed-ms` and
  `:time/arrival-ms` on a card when they diverge (`claimed Apr 28 · arrived
  today`); a nil claimed-ms renders honestly (WP1 §6 "nil is honest").
  Default active clock `:arrival`. Contract: §3.4; WP1 §6; gate 8.
- **OP-11 Staleness triad rendering.** Three visually distinct treatments
  keyed off WP1 L5: attested-recent, attested-stale, never-attested
  (`last-attested-ms` nil → `walked unknown`, must NOT read as fresh). Data
  surface: WP1 L5 `:last-attested-ms`, `:last-walked-ms` (always nil in WP1).
  Contract: §3.5 (I-2); gate 9.
- **OP-12 Provenance badges.** `asserted-by` always rendered on
  relation/verdict cards; `written-by` badge only when it differs from
  asserted-by (WP1 §5.1 projection rule); two disagreeing asserters BOTH
  rendered, badged, never merged. Data surface: WP1 `<edge>` /
  `<entry>.:entry/actor` / L4 verdicts. Contract: §3.6 (D-004, I-10); gate 10.
- **OP-13 Verdict-fold badges.** Render L4 `:verdicts.:current` per-asserter,
  badged, with `(current)` / `(disagrees)` markers. Data surface: WP1 L4
  (fold is projection; rows are truth). Contract: §4 Expansion "verdict fold
  with badges"; §3.6; gate 10.
- **OP-14 Omissions block.** Every `:omissions` (bundle caps) and
  `:feed/uncovered` (standing feed declaration) entry in consumed data
  renders as a visible line/chip with its count; a capped layer is never
  rendered as complete. Data surface: WP1 `:bundle/omissions`,
  `:omissions`, `:feed/omissions`. Contract: §3.7 exactness rule; gate 11.
- **OP-15 Hole-endpoint rendering.** A hole-shaped fixture row (question-unit
  door, WP1 §9.3 refusal kept open) renders with an explicit hole style; no
  exception, no silent skip. Contract: §9.3; gate 12.
- **OP-16 Material preview (expansion).** On card expansion, render the pulled
  bundle's material preview as mechanical truncation only (WP1 §9.6 already
  guarantees no paraphrase — the face adds no summarization). Data surface:
  expanded `context-bundle` wrapper result L1 `:material`. Contract: §4
  Expansion; gate 1 (expanded-card address).
- **OP-17 Expanded-card address line.** Card expansion renders the expanded
  target's own address (`:bundle/address` of the pulled bundle). Contract:
  §3.1 "Card expansions render the expanded target's address"; gate 1.

### 1.4 `trail_face/lanes.cljc` — thread/lane layout + connector geometry (pure cljc)

- **OP-18 Lane assignment.** Group feed entries into threads by
  conversation / target family; a thread is a lane; assignment is
  **deterministic** (same fixture → identical output twice; git-graph-style).
  Data surface: WP1 §6 entries + their `:entry/target`. Contract: §4 Nodes;
  T-2; gate 7.
- **OP-19 Manhattan connectors.** Draw relation rows (bundle L3 on expanded
  targets + `:relation-transition` feed entries) as vertical lane spines +
  elbow joints + junction dots — **thin rects only** (no line/bezier
  pipeline; RETRO T-3, O-1 gated). Every connector endpoint touches its
  card's bounds. Edge color/alpha carries kind + status (`:retracted` →
  struck/dim, still visible). Data surface: WP1 `<edge>` rows. Contract:
  §4 Edges; trap 6; gate 7.
- **OP-20 Dead-end terminality.** `:dead-end`-marked targets get terminal
  treatment; lanes emit NO forward segment past the terminal card. Data
  surface: WP1 relation kind `:dead-end`. Contract: §3.8 (D-002); gate 7.

### 1.5 `trail_face/scene.cljc` — face scene assembly (pure cljc)

- **OP-21 Single scene build.** Assemble the face rt-node tree ONCE per
  data/viewport change (cached-scene contract). Render flattens from it;
  clicks hit-test against the SAME tree object. Data surface: outputs of
  OP-4..OP-20 + viewport geometry. Contract: §4 "Scene built ONCE per
  data/viewport change into a cached scene atom"; T-6; trap 1; gate 13.
- **OP-22 Scroll clamp math.** Compute content height (codepoint/line based,
  NOT UTF-16 count) and clamp scroll offset against it (existing chat-pane
  idiom). Contract: §2.1 `scene.cljc` "scroll clamp math"; RETRO §3.8 async
  note (clamp on next wheel event).
- **OP-23 Windowing (NOT built in v0).** Windowing is deferred; scene builds
  ALL cards in v0. The measurement hook is the existing `[RAF]`>5ms
  instrument. Contract: §4 Windowing; trap 7; gate 15 (evidence only).
- **OP-24 Collapse-id namespacing.** Every interactive/collapse id in the
  scene is `:trail-face/*`-namespaced (no collision with trail.cljs/pane
  collapse-ids). Contract: §2.1; trap 10; S5.

### 1.6 `workspace/rect_tree.cljs → rect_tree.cljc` (RENAMED; promotion + T-4 clamp)

- **OP-25 Rename to `.cljc`.** Promote `rect_tree.cljs` to `rect_tree.cljc`
  (file already pure — VERIFIED zero js interop, §6). Same ns name;
  shadow-cljs + JVM both resolve. Puts `resolve-layout`/`tree->rects`/
  `tree->text-ops`/`hit-test` on the JVM where gates run. Contract: §2.1.
- **OP-26 Clip clamp (T-4).** In `tree->rects`, with `:clip?`, clamp
  partially-visible bg rects to the axis-aligned intersection with clip
  bounds (radii degrade at clamped corners — accepted). Fixes the suspected
  chat-pane bleed. Currently bg rects emit at full `w`/`h` with no clamp
  (VERIFIED §6). Contract: §5.2; T-4; gate 6.

### 1.7 `substrate/webgpu/renderer.cljs` — shape-fn amendment (bounded, §5.1)

- **OP-27 Missing-glyph advance + fallback draw.** In `shape-msdf-line` AND
  `shape-slug-line` ONLY: a missing glyph must still ADVANCE x and draw the
  atlas fallback glyph if present — never the current zero-advance skip
  (VERIFIED: advance sits inside the glyph `when-let`, §6). Defense-in-depth
  behind the cljc sanitizer (OP-2); the sanitizer is the gate-tested layer,
  this is diff-review + first-light verified. Contract: §5.1; V3-5; gate 4
  "does not execute the GPU shaper — the renderer.cljs fix (§5.1) is verified
  by diff review + first light".

### 1.8 `resources/public/font_atlas.{json,png}` — atlas regen (§5.3)

- **OP-28 Charset widen + regen.** Widen the msdf-atlas-gen charset per
  CHARSET_AUDIT missing set (box-drawing `U+2500` class, arrows, curly
  punctuation, `U+2713`/`U+2605`/`U+26A0` class) **AND the fallback glyph
  U+FFFD itself** (without it the sanitizer's substitution renders nothing);
  re-run the audit green afterward. Contract: §5.3; gate 5 (falsifier: regen
  silently narrows, or ships without the fallback).

### 1.9 `trail_face/wiring.cljs` — the ONLY cljs file (atoms/flows/glue)

- **OP-29 Local view-state atoms.** Declare view-state atoms (scroll, clock/
  `:order` param, collapse set, selection) — atoms ONLY; zero kernel writes.
  Contract: §3.10 read-only; §2.1 seam; gate 13/first-light.
- **OP-30 Flow wiring for the two modes.** Register the two faces' m/latest
  flow branches (build/compute fns) per the 7-step recipe; use `m/latest`
  for combining watches, NOT `m/ap`+`m/?<` (CLAUDE.md law, §12). Contract:
  §2.2; PRIMITIVES §recipe.
- **OP-31 Coverage-set load call.** Load the atlas coverage set (OP-1 input)
  here (js/asset side), pass as data into the pure sanitizer. Contract:
  §2.1 seam ("wiring.cljs … the only places allowed to touch atoms/flows/
  DOM").

### 1.10 `file_viewer.cljc` — Electric bridge e/defns (the ×5 precedent)

- **OP-32 Pull e/defns.** New e/defns wrapping WP1 §7 pull wrappers inside
  `e/server`, address-shaped args: over `read-context-bundle`,
  `read-recent-activity`, `read-conversation-trail`, and `render-bundle-text`
  (View-3 text). Client side: results land in atoms threaded through
  `start-loop!` like the existing five. NO `foreign-select`, no PState names
  (INV-S1). Contract: §2.3; S1; verified precedent = 5 `Watch*` e/defns +
  `FileContent` pull (§6). [See Finding F-3 on the wrapper NAMES.]
- **OP-33 `WatchIngestEpoch` push e/defn.** New push e/defn:
  `(e/server (e/watch util-fns/!ingest-epoch-atom))` — mirrors the epoch
  counter to the client. This reads the package's OWN epoch atom, NOT a WP1
  wrapper (carve-out INV-S1a). Contract: §2.3, §2.4, §6.

### 1.11 `electric_flow.cljc` — thread new atoms

- **OP-34 Thread epoch/face atoms.** Thread the new client atoms (epoch,
  face results) into `start-loop!` alongside the existing
  `(reset! !sidebar-truth (fv/WatchSidebarTruth))` … block (VERIFIED lines
  479–483, §6). Contract: §12 allowlist (`electric_flow.cljc` thread new
  atoms).

### 1.12 `util_fns.cljc` — ingest-epoch mirror atom ONLY

- **OP-35 Epoch mirror atom.** Add a server-side monotonic counter atom
  `!ingest-epoch-atom` (the util-fns mirror-atom pattern — VERIFIED the
  `!sidebar-truth-atom` etc. pattern lives here, §6). Bumped by the watcher
  loop; pushed by OP-33. Contract: §2.4, §6, allowlist. [File is
  `util_fns.cljc`, not `util_fns.clj` as the contract writes it — Finding
  F-1.]

### 1.13 `server/ingest_watchers.clj` — NEW ns, outside every Rama module

- **OP-36 Root watch + debounce.** Watch configured roots (Claude transcript
  dir for this repo; `docs/`; `vision/`) with debounce ≥ 500ms per path;
  serialized import per file. Config, not code. Contract: §2.4, §6.
- **OP-37 Call EXISTING import fns only.** On a completed change, call the
  EXISTING import entry points (no new ingestors, no new truth path).
  Convergent re-import is the safety (deterministic ids + idempotency
  journals, D-008.3). Contract: §2.4, §6; trap 2; verification duty (a) —
  implementer confirms import entry points + their observable completion in
  Phase B2-P1 (`object_container/runtime.clj` + adapters).
- **OP-38 Epoch bump on batch completion.** After each completed import batch
  (latched on import-fn return / completion PState — implementer verifies
  which is observable), bump `!ingest-epoch-atom` (+1). Contract: §6; trap 11;
  gate 14.
- **OP-39 Failure resilience.** Import failures are logged and retried on the
  next change event; the watcher loop NEVER crashes. No polling loops
  anywhere. Contract: §6; quirks no-polling law.

### 1.14 The 7-step mode-registration wiring (additive to shared files)

- **OP-40 Mode branch + predicate.** Add a mode branch in
  `derive-effective-local-world` (`runtime/workspace_actions.cljs`,
  VERIFIED at line 125, §6) + a `local-world-<face>?` predicate; register in
  the add-watch recompute set (`runtime.cljs` ~301–322, VERIFIED §6).
  Contract: §2.2; PRIMITIVES §recipe steps 1–2; gate 17 regression.
- **OP-41 Compute-fn branches.** Add branches in the `<content-text` mode
  case (`combined_text.cljs` ~253, VERIFIED §6) and the `<editor-content`
  mode case (`editor_compute.cljs` ~457–464, VERIFIED §6); pass compute fns
  into `render-consumer` (`runtime/render.cljs` ~49/62, VERIFIED §6).
  Contract: PRIMITIVES §recipe steps 3–5; gate 17 (existing-mode output
  byte-identical).
- **OP-42 Scroll zone.** Insert the new wheel zone into the `scroll.cljs`
  cond cascade at the CORRECT position relative to the existing order
  (sidebar → flow → chat → editor; VERIFIED order-sensitive, flow before
  chat, §6). Contract: trap 9; §12 Phase-C duty; verification duty (c).
- **OP-43 Mouse/hit-test zone.** Add mouse routing + click handler
  (`runtime/mouse.cljs`); hit-test against the cached scene tree (OP-21), not
  a rebuilt tree. Contract: PRIMITIVES §recipe step 6; T-6; gate 13.
- **OP-44 Entry command.** Add an entry command dispatched via
  `runtime/agent_flow.cljs` command dispatch (VERIFIED dispatch site, §6);
  register any new atoms in `runtime/state.cljs`. Contract: PRIMITIVES
  §recipe step 7; allowlist.

### 1.15 Fixtures (test resources)

- **OP-45 Hand fixtures.** `test/resources/trail_face/` EDN fixtures shaped
  EXACTLY per WP1 §4/§6/§8: a bundle with all six layers incl. `:in-family`
  verdicts + omissions; a feed window with a back-dated entry, an
  uncovered-declaration, a dead-end, two disagreeing asserters, a
  `:written-by`-differs row; a §8 v0 text projection incl. `walked unknown`;
  a hole-endpoint row; strings salted with CHARSET_AUDIT top-missing
  codepoints. Contract: §8. Re-derive in the same session if WP1 amends
  §4/§6/§8 shapes (implementer-fixable, §8). [Salt-class subtlety →
  Finding F-4.]
- **OP-46 Integration fixture (post-WP1-green).** After WP1 gates green,
  capture ONE live fixture from the WP1 wrappers and diff shape-equal against
  the hand fixtures. Contract: §8; gate 16.

---

## 2. Invariants (what must hold, with citations)

### 2.1 The 10 face laws (§3)

- **INV-1 Address law (I-14, WP1 §3).** Every face renders its own address as
  one header text line that round-trips through WP1 `resolve-address`; card
  expansions render the expanded target's address. Placement: OP-7/OP-17 in
  `text_face.cljc`/`cards.cljc`; address value from WP1 §7 wrapper results.
  Gate 1.
- **INV-2 View-3 verbatim law.** `render-bundle-text` output rendered
  line-for-line: same line count, same per-line codepoint sequence; soft
  horizontal overflow scrolls, NEVER re-wraps. Placement: OP-4
  `text_face.cljc`. §3.2, trap 3, gate 2. **Precise reading (reconciles with
  INV-9):** "same character sequence" means no drop/reorder/re-wrap; an
  uncovered codepoint MAY be substituted to U+FFFD by the sanitizer with
  position + count preserved (gate 2 "byte-for-byte after sanitize, fallback
  substitutions counted"). Falsifier is a dropped/reordered/re-wrapped char,
  NOT a fallback substitution.
- **INV-3 Marker coloring total + versioned.** One marker→style table keyed
  to `;; trail-text v0`; unknown line shapes → `:normal` (never dropped);
  version-header mismatch → whole projection plain + exactly one notice line.
  Placement: OP-5/OP-6 `text_face.cljc`. §3.3, trap 12, gate 3.
- **INV-4 Two clocks at the pixel layer (WP1 §6, trap 4).** Default clock
  `:arrival`; the active clock is part of the rendered address params (== the
  `:order` param of the `recent-activity` address); entries render BOTH
  stamps when they diverge. Placement: OP-10 `cards.cljc` + address param in
  `wiring.cljs`. §3.4, gate 8. **Scope (WP1 §6/§9.10):** the face has NO
  claimed-window *selection* — window selection is always arrival; the clock
  param only orders/labels within the arrival window. [See Finding F-5.]
- **INV-5 Staleness renders differently (I-2).** Three pairwise-distinct
  states: attested-recent, attested-stale, never-attested
  (`last-attested-ms` nil). nil must NOT read as fresh; `walked unknown`
  rendered verbatim. Placement: OP-11 `cards.cljc`. §3.5, gate 9.
- **INV-6 Provenance badges (D-004, I-10).** `asserted-by` always visible on
  relation/verdict cards; `written-by` only when it differs (WP1 §5.1);
  disagreeing asserters both render, badged, never merged. Placement:
  OP-12/OP-13 `cards.cljc`. §3.6, gate 10.
- **INV-7 Omissions are pixels (exactness rule).** Every `:omissions` /
  `:feed/uncovered` entry renders as a visible line/chip with its count; a
  capped layer is never rendered as complete. Placement: OP-14 `cards.cljc`.
  §3.7, gate 11.
- **INV-8 Dead-ends terminal (D-002).** `:dead-end`-marked targets get
  terminal treatment; lanes never continue forward out of them. Placement:
  OP-20 `lanes.cljc`. §3.8, gate 7.
- **INV-9 No content past the shaper the atlas cannot draw (V3-5).** Ops
  sanitized codepoint-wise against the coverage set; uncovered → fallback
  glyph (visible tofu, honest), never vanish; advance counting
  codepoint-correct (surrogate pair = one advance). Placement: OP-2/OP-3
  `sanitize.cljc` (gate layer) + OP-27 `renderer.cljs` (defense-in-depth).
  §3.9, trap 5/13, gates 4/5.
- **INV-10 Read-only (D-008.1).** Zero kernel writes from any face code path;
  local view-state (scroll, clock param, collapse, selection) is atoms only.
  Placement: whole `trail_face/` + `wiring.cljs`; enforced structurally by
  the seam. §3.10, gate 13/first-light, WP1 gate 14 (module has no depots).

### 2.2 Style gates S1–S5 (§10) — each names where it stops

- **INV-S1 Seam (S1).** Electric bridge + faces call ONLY WP1 §7 wrappers —
  no `foreign-select`, no PState names, no kernel write fns anywhere under
  `trail_face/` or the new e/defns. **Stops at source scan**; runtime paths
  are gate 14's integration test.
- **INV-S1a Epoch carve-out.** The ingest-epoch mirror atom
  (`util_fns.cljc` OP-35, pushed by OP-33) is the ONE data surface outside
  WP1 §7 the bridge may read — justified because it is a monotonic counter,
  carries no data, orders nothing semantically, resets on restart (INV-14).
  It is NOT a PState and NOT a `foreign-select`, so it does not breach S1's
  actual prohibition. §2.4, §6.
- **INV-S2 Purity (S2).** `trail_face/*.cljc` contains no atoms, no js
  interop, no reader conditionals except an unavoidable platform shim (then
  1 named fn). **Stops at the file boundary**; `wiring.cljs` exempt by design.
- **INV-S3 Char-advance (S3).** Face code takes `char-advance` as an
  argument; the literal `0.56` appears NOWHERE in `trail_face/`. **Stops at
  the new dir**; the ~30 legacy sites (incl. `rect_tree.cljc` line 283, which
  keeps its `0.56` — VERIFIED §6) are out of scope (CLAUDE.md law).
- **INV-S4 Control bytes (S4).** NUL and control chars only as `\uXXXX`
  escapes in source AND fixtures. **Stops at source/fixture files.** (The
  tool-JSON `\u0000`-decode trap has fired 4× in this project — quirks law.)
- **INV-S5 Collapse-id namespace (S5).** Every interactive id under
  `trail_face/` is `:trail-face/*`-namespaced. **Stops at the new dir.**
  Placement: OP-24 `scene.cljc`/`cards.cljc`.

### 2.3 Cross-cutting invariants the contract imposes

- **INV-11 Cached-scene law.** Exactly ONE scene tree per data/viewport
  change; render flatten and click hit-test share the SAME tree object
  (`identical?`-stable). Placement: OP-21 `scene.cljc` + OP-43 hit-test.
  §4, T-6, trap 1, gate 13.
- **INV-12 Verbatim/no-paraphrase law (material).** All previews are
  mechanical truncation; the face adds no summarization (WP1 §9.6 guarantees
  the data layer already does). Placement: OP-16 `cards.cljc`. §4.
- **INV-13 Watchers-are-triggers law.** Watchers call EXISTING import fns
  only; no new ingestors, no second truth path; convergent re-import is the
  safety. Placement: OP-37 `ingest_watchers.clj`, outside every Rama module.
  §2.4, §6, trap 2, D-008.3.
- **INV-14 Epoch-is-a-counter-not-truth.** The ingest epoch is a monotonic
  per-server counter: no data, orders nothing semantically, resets on
  restart. Every rendered timestamp comes from WP1's TWO clocks, never the
  epoch. Placement: OP-35 `util_fns.cljc` + OP-38 bump. §2.4 identity-law
  scope note, §6.
- **INV-15 Substrate-untouched law.** Nothing in the substrate changes beyond
  OP-26 (rect_tree clamp) + OP-27 (two shape fns) + OP-28 (atlas regen).
  Explicitly untouched: `draw-frame!` order, camera/zoom, dirty-present flag,
  buffer pools, font backends. §5.
- **INV-16 Additive-wiring/regression law.** The 7-step wiring touches shared
  files additively (mode branches) only; existing modes' output stays
  byte-identical for a non-trail fixture world. No edits to trail.cljs,
  dg_flow.cljs, sidebar.cljs, settings, cmd_panel. §9.7, allowlist, gate 17.
- **INV-17 Manhattan-only-v0 law.** Edges are axis-aligned thin rects only;
  no line/bezier pipeline (curves are a Sid-gated follow-up, O-1). Placement:
  OP-19 `lanes.cljc`. §4, trap 6.
- **INV-18 Windowing-is-evidence-gated law.** v0 builds all cards; windowing
  (T-5) enters only if gate-15 `[RAF]` p95 > 5ms evidence demands it (D-001).
  Placement: OP-23 `scene.cljc`. §4, trap 7, gate 15.
- **INV-19 No-polling law.** No polling loops anywhere (watcher, tests, client
  re-pull ≥ 1s debounce). Tests latch on import-fn return / completion read.
  §6, trap 11, quirks discipline, gate 14.

---

## 3. Gate × law × fixture matrix (§12 P0 deliverable shape)

For each of the 17 §11 gates: law(s)/invariant(s) it tests, the §8 fixture
element that exercises it, its falsifier, and the phase it lands in
(P3=written/compile; P4=green + substrate; P5=integration post-WP1-green;
P6=watchers + first light). Operations that produce the tested behaviour in
parentheses.

| Gate | Law(s)/Invariant(s) | Fixture element (§8) | Falsifier | Phase |
|---|---|---|---|---|
| 1 Address law | INV-1 (OP-7/OP-17) | any bundle/feed/expanded-card fixture w/ `:*/address` | a face w/o an address, or an address that doesn't parse/round-trip through `resolve-address` | P3→P4 |
| 2 View-3 verbatim | INV-2, INV-9 (OP-4/OP-2) | §8 v0 text projection sample (incl. `walked unknown` + salted strings) | any dropped / reordered / re-wrapped char (a counted fallback substitution is NOT a falsifier) | P3→P4 |
| 3 Marker totality | INV-3 (OP-5/OP-6) | projection sample; mutated unknown-marker line; mutated version header | unknown line dropped (≠ `:normal`); version mismatch not → plain + exactly one notice | P3→P4 |
| 4 Sanitize correctness (V3-5) | INV-9 (OP-2/OP-3) | strings salted w/ audit top-missing codepoints **+ surrogate pairs**; coverage from REAL `font_atlas.json` | codepoint count not preserved; uncovered not → fallback; advance ≠ codepoint count | P3→P4 (cljc only; GPU shaper OP-27 = diff-review + first light) |
| 5 Atlas coverage regression | INV-9 (OP-28) | must-have set (box `U+2500` class, arrows, curly punct, `U+2713`/`U+2605`/`U+26A0` class, **+ U+FFFD itself**) | regen silently narrows, or ships without the fallback glyph | P4 (regen lands here) |
| 6 Clip containment (T-4) | INV-11-adjacent, T-4 (OP-26) | cards straddling a `:clip?` boundary; timeline face @ 3 scroll offsets | any bg rect outside clip bounds (the as-built bleed) | P4 (rect_tree clamp) |
| 7 Lane/connector geometry | INV-8, INV-17 (OP-18/OP-19/OP-20) | feed window w/ dead-end; relation rows | nondeterminism; floating connector; forward segment past a dead-end (ghost lane) | P3→P4 |
| 8 Two clocks | INV-4 (OP-10 + address param) | back-dated entry (claimed 60d ago, arrived now) | back-dated entry not in arrival-today; both stamps not rendered; clock param doesn't change the address line | P3→P4 |
| 9 Staleness triad | INV-5 (OP-11) | attested-recent / attested-stale / never-attested(nil) fixtures | any two of the three not pairwise-distinct; nil reads as fresh | P3→P4 |
| 10 Badges | INV-6 (OP-12/OP-13) | two disagreeing asserters; a `:written-by`-differs row | `asserted-by` missing; `written-by` shown when equal / hidden when differs; disagreeing verdicts merged | P3→P4 |
| 11 Omissions are pixels | INV-7 (OP-14) | bundle caps + feed `:feed/uncovered` | a forced cap silently narrows; an omission without its count | P3→P4 |
| 12 Hole endpoint | INV-15-adjacent (OP-15) | hole-endpoint row | exception or silent skip on the hole row | P3→P4 |
| 13 Cached scene identity | INV-11 (OP-21/OP-43) | any scene; a fixture click | two flattens not `identical?`-stable; hit-test resolves against a different tree object | P3→P4 (structural; real mouse routing = first-light) |
| 14 Watcher loop (JVM integ) | INV-13, INV-14, INV-19 (OP-36..OP-39) | temp watch root + fixture md file | import not latched (polling); epoch ≠ +1; identical rewrite changes feed entry count | **P6** (watchers) [phase-conflict, Finding F-2] |
| 15 Windowing evidence | INV-18 (OP-23) | a real day's material (first-light) | `MEASUREMENT_RAF.md` absent / no stated ruling either way | P6 (first light) |
| 16 Fixture fidelity | OP-45/OP-46 (hand vs live shape-equal) | one live WP1-wrapper capture vs hand fixtures | hand fixtures drifted from real shapes (keys/nesting) | P5 (post-WP1-green) |
| 17 Regression | INV-16 (OP-40..OP-44) | non-trail fixture world (editor/flow modes) | relation-kernel/WP1 suites not green; shadow build fails; existing-mode ops not byte-identical w/ trail mode absent | P5 |

Notes: gates 1–13 are the P3-written / P4-green block (contract §12 B2-P3/P4).
Gate 5 needs OP-28 (regen) which lands in P4, so gate 5 is a P4 gate even
though its assertion is over the atlas JSON. Gate 4's *fallback* assertion
must use permanently-uncovered codepoints (see edge cases E-8 / Finding F-4).

---

## 4. Edge cases the gates/fixtures must cover

- **E-1 Surrogate pairs / astral codepoints.** Emoji (`U+1F534`, `U+1F7E2`)
  and any astral char = one codepoint = one advance; the as-built shaper
  does two UTF-16 lookups → double-miss + double-desync. OP-2/OP-3 must count
  by codepoint. Gate 4 salt. (Charset audit: emoji rare but present.)
- **E-2 U+FFFD itself.** The fallback glyph must be IN the atlas (gate 5) AND
  a fixture where the SOURCE already contains a literal U+FFFD (e.g. the
  audit's own decode artifacts) must not be double-substituted or crash —
  U+FFFD is covered-after-regen, so it passes through as itself.
- **E-3 Version-header mismatch.** A projection whose header ≠ `;; trail-text
  v0` → whole projection plain + exactly ONE notice line (OP-6, gate 3). Also:
  a projection with NO header line at all.
- **E-4 nil `last-attested-ms`.** never-attested target → `walked unknown`,
  visually distinct from attested-stale; nil ≠ fresh (OP-11, gate 9). Also
  WP1 L5 `:last-walked-ms` is ALWAYS nil in WP1 → always `walked unknown`.
- **E-5 Disagreeing asserters.** Two `:current` verdicts on one target, both
  badged, never merged; and a `:written-by`≠`:asserted-by` row where
  `written-by` shows, vs a matching row where it's omitted (OP-12, gate 10).
- **E-6 Dead-end lanes.** A `:dead-end`-marked target: lane terminates, no
  forward connector segment (OP-20, gate 7).
- **E-7 Back-dated entry under both clocks.** claimed 60d ago / arrived now:
  appears in arrival-today; ordered by its April stamp under `:order
  :claimed`; card shows both stamps; changing clock param changes address
  line — but NEVER re-selects to a claimed window (WP1 §9.10; E-11).
- **E-8 Salt-class split (covered-after-regen vs permanently-uncovered).**
  The audit's TOP-missing codepoints (`U+2500`, `U+2192`, `U+2014`) become
  COVERED after OP-28 regen, so post-regen they must NOT map to fallback in
  gate 4. Gate 4's fallback assertion needs permanently-uncovered salt
  (emoji/rare astral); gate 5 needs the must-have set present. Same fixture,
  two salt classes. [Finding F-4.]
- **E-9 Hole endpoints.** A hole-shaped row (question-unit door) renders with
  hole style; no crash (OP-15, gate 12; §9.3 door kept open).
- **E-10 Clip-straddling cards.** A card straddling a `:clip?` boundary emits
  a clamped bg rect; same over the timeline at 3 scroll offsets (OP-26,
  gate 6). Text is already clipped; bg is the bleed.
- **E-11 Claimed-window selection attempt.** The face must NOT offer
  claimed-window *selection* (WP1 §9.10 refuses it at the data layer). The
  clock param is in-window ordering/labeling only. A naive face that tries to
  select a claimed window is a defect (Finding F-5).
- **E-12 Identical-rewrite convergent re-import.** Watcher sees an identical
  md rewrite → convergent re-import → feed entry COUNT unchanged, epoch still
  +1 (OP-38, gate 14). (Deterministic ids + idempotency journals, D-008.3.)
- **E-13 Import failure / watcher resilience.** An import that throws is
  logged + retried on next change; the loop does not crash; no polling
  (OP-39, gate 14, INV-19).
- **E-14 Unknown / unresolved target ids.** WP1 returns unknown ids under
  `:bundle/omissions` (`:target/unrecognized`) and git-sha targets as
  `:unresolved` (dangling) — the face renders them as omission chips /
  unresolved cards, never crashes (OP-14; WP1 §4).
- **E-15 Omissions with cursors.** A capped layer with a resumable cursor
  renders the count AND the resume affordance (OP-14; WP1 omissions law).
- **E-16 Rapid resize / RAF starvation.** Per CLAUDE.md, `m/latest` must not
  hold side effects and RAF stays unconditional; the new flows (OP-30) must
  not introduce `m/ap`+`m/?<` fork-cancel or in-`m/latest` request scheduling.
- **E-17 Scroll clamp on shrink.** A stream update that shrinks content while
  scrolled deep clamps on the NEXT wheel event, not on data change (OP-22;
  RETRO §3.8 accepted behaviour) — the acceptance gate should not assume
  clamp-on-data-change.

---

## 5. Findings

Classification: **implementer-fixable** = the contract (or a cited binding
doc) already answers it, cite where; **policy fork** = two readings that
cannot both hold, both stated verbatim, options + recommendation, NOT
resolved here. No policy forks were found; no stop-clause-grade cross-contract
conflict was found (see §7 return note).

- **F-1 (implementer-fixable) — `util_fns` extension is wrong in the
  contract.** CONTRACT §2.4/§6 and the §12 allowlist name
  `src/app/server/rama/util_fns.clj`; the actual file is
  `src/app/server/rama/util_fns.cljc` (VERIFIED: it holds the exact
  mirror-atom pattern — `!sidebar-truth-atom` etc., read by the 5 `Watch*`
  e/defns). Fix: add `!ingest-epoch-atom` to `util_fns.cljc`. The `.cljc`
  extension is load-bearing only in that the atom is defined in a cross-target
  ns; the `Watch*` bridge reads it server-side via `util-fns/!*-atom`, so the
  package's OP-33/OP-35 wiring is unaffected. Cite: §6 MISMATCH row.

- **F-2 (implementer-fixable) — gate 14 phase placement is internally
  contradictory.** §12 lists gate 14 under **B2-P5** ("integration … gates
  14, 16, 17") AND under **B2-P6** ("watchers + epoch push (gate 14 full)").
  Gate 14's own text (§11) is entirely the SERVER watcher loop ("temp watch
  root … import fires … epoch bumped by 1 … Stops at the server loop; Electric
  push is first-light"), and the watcher ns (`ingest_watchers.clj`) + the §6
  loop are P6 work. Reading: gate 14 lands in **P6** with the watchers; P5's
  "14" is at most the Electric-push *wiring* (`WatchIngestEpoch`) that P5's
  bridge provides while the loop-level assertion is P6. Recommend the matrix
  assign gate 14 → P6, P5 → gates 16, 17. Cite: §11 gate 14 stop-boundary +
  §12 allowlist (watchers are P6). Implementer-fixable — contract §6 places
  the loop in P6.

- **F-3 (implementer-fixable) — §2.3 conflates the package's own e/defn names
  with "WP1 §7 wrappers".** §2.3 says the new e/defns call "**only WP1 §7
  wrappers** … : `TrailBundle`, `TrailFeed`, `TrailConversation`, `TrailText`
  (pull …) and `WatchIngestEpoch` (push …)". But WP1 §7's wrapper set is
  `read-context-bundle` / `read-recent-activity` / `read-conversation-trail` /
  `read-relation-detail` / `->address` / `resolve-address` /
  `current-verdicts` / `render-bundle-text` — none named `Trail*`, and
  `WatchIngestEpoch` is NOT a WP1 wrapper at all (WP1 §9.8 refuses watchers;
  the epoch atom is this package's own, INV-S1a). Reading: `TrailBundle`
  etc. are the NAMES of the package's own `e/defn` bridges (like the existing
  `WatchSidebarTruth`), each wrapping a WP1 §7 read fn inside `e/server`;
  `TrailText` wraps `render-bundle-text`; `WatchIngestEpoch` reads the epoch
  atom (carve-out). S1's actual prohibition (§10: "no `foreign-select`, no
  PState names, no kernel write fns") is not breached by either. Cite: WP1 §7
  wrapper list; §2.4/§6; §10 S1 text. Implementer-fixable — intent is clear
  from §2.4 + WP1 §7.

- **F-4 (implementer-fixable) — gate-4 salt vs §5.3 regen (fixture-design
  subtlety).** §8 says fixtures are "salted with CHARSET_AUDIT's top-missing
  codepoints"; gate 4 asserts "uncovered codepoints map to the fallback".
  After OP-28 regen the top-missing codepoints (`U+2500`, `U+2192`, `U+2014`)
  become COVERED, so a fixture salted only with them would exercise NO
  fallback path post-regen (they'd pass through as themselves) and gate 4's
  "uncovered → fallback" assertion would be vacuous or, if it asserts fallback
  ON those chars, would FAIL post-regen. Fix: gate 4's fallback + surrogate
  assertion uses permanently-uncovered salt (emoji `U+1F534`/`U+1F7E2`, or any
  codepoint outside the must-have set); the must-have codepoints go to gate 5
  (coverage-present) and to gate 2's positive path. Cite: §8, gate 4 ("+
  surrogate pairs"), gate 5 (must-have set), CHARSET_AUDIT §7. Also implies
  gate 4 is order-sensitive to regen (E-8); the matrix marks it P3→P4 and the
  salt choice makes it regen-independent. Implementer-fixable.

- **F-5 (implementer-fixable) — gate-8 wording vs WP1 §9.10 (no claimed-window
  selection).** Gate 8 (§11) reads: "the back-dated fixture entry appears
  under `:arrival` today **and not under `:claimed` today**". Taken literally
  that describes two *window selections* (arrival-window and claimed-window),
  but WP1 §6 rules window selection is ALWAYS arrival and WP1 §9.10 REFUSES
  claimed-window cross-land selection. Reading consistent with both contracts:
  the face's clock param is the `:order` param (in-window ordering) + which
  stamp heads the card; "not under `:claimed` today" means when ordering by
  `:claimed` the entry sorts by its 60-day-old claimed stamp (away from
  today's position), never that a claimed window is selected. This matches WP1
  gate 4 exactly. Risk if misread: an implementer builds claimed-window
  selection into the face and violates WP1 §9.10 (E-11). Cite: WP1 §6 F-1
  ruling, WP1 §9.10, WP1 gate 4; view-mvp §3.4. Implementer-fixable —
  precedence resolves it (WP1 binding).

- **F-6 (implementer-fixable, cosmetic) — §5.1 header label garbled.** §5.1
  titles the shape-fn fix "**`renderer.cljs` shape fns (V3-5 server... client
  half)**". V3-5 (RETRO) is a single client-side shaper item; the
  "server/client half" language belongs to the WP1 §8 text-generation split,
  not to the glyph fix. No build ambiguity — the body ("missing glyph →
  advance anyway + draw fallback … bounded to `shape-msdf-line`/
  `shape-slug-line`") is precise and VERIFIED (§6). Cite: RETRO V3-5, §5.1
  body. Implementer-fixable / ignorable.

- **F-7 (implementer-fixable) — `file_viewer.cljc` path.** The contract cites
  `file_viewer.cljc` bare; the file is at `src/app/file_viewer.cljc` (NOT
  under `client/workspace/`). The ×5 `Watch*` precedent VERIFIED there. No
  action beyond knowing the path. Cite: §6.

---

## 6. Citation verification tally (source-fact claims vs on-disk `src/app/`)

20 distinct source-fact citations checked; 19 MATCH, 1 MISMATCH (extension).

| # | Contract claim | Source check | Verdict |
|---|---|---|---|
| 1 | `rect_tree.cljs` pure, zero js interop | grep for js interop in `rect_tree.cljs` = 0 hits | MATCH |
| 2 | `resolve-layout`/`tree->rects`/`tree->text-ops`/`hit-test` present | `rect_tree.cljs:194,207,255,351` (+`tree->shadows:322`, `dispatch-event:376`, `wrap-line:7`) | MATCH |
| 3 | partially-visible bg rects emitted full-size (T-4) | `rect_tree.cljs:229-245`: bg emitted at full `w`/`h`, `visible?` only culls fully-offscreen; no clamp | MATCH |
| 4 | `shape-msdf-line`/`shape-slug-line` are the shape fns | `renderer.cljs:1147,1187` (`defn-`) | MATCH |
| 5 | missing glyph = no advance (advance inside glyph `when-let`) | both fns: `(swap! !x + advance)` sits INSIDE `(when-let [g (get glyphs code)] …)` | MATCH |
| 6 | surrogate pairs double-count (UTF-16 lookup) | `(doseq [ch (seq text)] (let [code (.charCodeAt ch 0)] …))` — per code unit | MATCH |
| 7 | `file_viewer.cljc` ×5 e/defn precedent | `src/app/file_viewer.cljc`: 5 `Watch*` e/defns (`WatchSidebarTruth`/`UserSettings`/`AgentTrail`/`FlowSession`/`WorkspaceTruth`) + `HomeDirs`/`DirContents`/`FileContent` | MATCH |
| 8 | sidebar `!sidebar-scene` cached-scene precedent | `editor_compute.cljs:307-328` builds once → `(reset! !sidebar-scene tree)`, hit-test reuse; `state.cljs:192` declares it | MATCH |
| 9 | `scroll.cljs` wheel cascade order-sensitive (flow before chat) | `scroll.cljs:48` `cond`: sidebar → flow (`;; must check BEFORE chat`, line ~70) → chat → editor | MATCH |
| 10 | util_fns mirror-atom pattern (server atom + Watch) | pattern present in `util_fns.cljc`; `Watch*` e/defns `(e/watch util-fns/!*-atom)` | MATCH (pattern) |
| 10b | file is `util_fns.clj` (contract spelling) | actual file is `util_fns.cljc` | **MISMATCH** → F-1 |
| 11 | `combined_text.cljs` `<content-text` mode branch | `combined_text.cljs:~253` flow-mode branch (intake/run) | MATCH |
| 12 | `editor_compute.cljs` `<editor-content` mode case | `editor_compute.cljs:457-464` `(case mode :flow-intake … :flow-run … editor-content)` | MATCH |
| 13 | `render.cljs` render-consumer args (7-step step 5) | `render.cljs:~49/62` compute fns threaded into `render-consumer` | MATCH |
| 14 | `runtime.cljs` derive + add-watch (~301-322) | `runtime.cljs:301-322` `recompute-local-world!` + add-watch over atoms | MATCH |
| 15 | `derive-effective-local-world` + predicates | `workspace_actions.cljs:125` + `local-world-flow?:214` + `:flow-intake/:flow-run` | MATCH |
| 16 | `agent_flow.cljs` entry command dispatch (step 7) | `agent_flow.cljs:214-307` command dispatch (`parse-agent-command`, `handle-dg-command!`) | MATCH |
| 17 | trail.cljs agent-run card builders (separate from kernel) | `trail.cljs:501 trail->chat-nodes`, `1235 trail->display-lines`, `210 parse-md-blocks`, `95 wrap-md-spans` | MATCH |
| 18 | dg_flow 7-step precedent exists | `src/app/client/workflows/dg_flow.cljs` present | MATCH |
| 19 | `electric_flow.cljc` threads Watch* into atoms | `electric_flow.cljc:479-483` `(reset! !sidebar-truth (fv/WatchSidebarTruth))` … | MATCH |
| 20 | `rect_tree` keeps literal `0.56` (S3 out-of-scope legacy) | `rect_tree.cljs:283 (* fs 0.56)` | MATCH |

WP1 `§n` citation cross-checks (view-mvp → WP1 CONTRACT text): §3.1↔WP1 §3
address law; §3.4↔WP1 §6 two clocks; §3.5↔WP1 L5 `last-attested-ms`;
§3.6↔WP1 §5.1 `:written-by` projection rule; §3.7↔WP1 §4 omissions +
§6 `:feed/uncovered`; §4 preview↔WP1 §9.6 no-paraphrase; §4 conversation
cursor↔WP1 §7 `read-conversation-trail`; §4 edges↔WP1 §6 `:relation-transition`
+ §4 L3 relations. ALL MATCH. NOTE: WP1 §7 wrapper NAMES are `read-*`, not
`Trail*` — the view-mvp §2.3 `Trail*` names are the package's own e/defns,
not WP1 wrappers (F-3). WP1 module `trail_view.clj` and its wrappers do NOT
yet exist in `src/` (WP1 impl runs in parallel; B2-P3 is contract-fixture-
based; P5 waits for WP1 green) — this is EXPECTED per §12, not a mismatch.

---

## 7. Summary (for the parent session)

- Operations: **46** (OP-1..OP-46). Invariants: **19 face/cross-cutting +
  5 style (S1..S5) + 1 seam carve-out (S1a) = 25**. Matrix rows: **17**
  (one per acceptance gate). Edge cases: **17** (E-1..E-17).
- Citation verification: **20 source-fact claims checked → 19 MATCH,
  1 MISMATCH** (`util_fns.clj` should be `util_fns.cljc`, F-1). All WP1 `§n`
  cross-citations MATCH the WP1 CONTRACT text.
- Findings: **7, all implementer-fixable, 0 policy forks.** F-1 wrong file
  extension; F-2 gate-14 phase contradiction (→ P6); F-3 `Trail*` names ≠ WP1
  §7 wrappers; F-4 gate-4 salt must be permanently-uncovered (regen makes
  top-missing chars covered); F-5 gate-8 wording vs WP1 §9.10 no-claimed-
  window; F-6 §5.1 label cosmetic; F-7 `file_viewer.cljc` path.
- **No stop-clause-grade conflict** between view-mvp CONTRACT and WP1 CONTRACT
  or decisions.md. The seam holds: WP1 §7 wrappers are the only truth surface;
  the one out-of-§7 read (the ingest-epoch atom) is explicitly a counter, not
  truth (INV-14), and does not breach S1's actual prohibition. Read-only
  (INV-10) is structurally consistent with WP1 gate 14 (no-depot module).
  Watchers (view-mvp) fill exactly the slot WP1 §9.8 defers. All 2026-07-04
  rulings (face order, D-008 read-only, two-clock discipline) are honoured.

## 8. Addendum — contract-author sweep (Fable, 2026-07-04, same session)

All seven findings were ruled **implementer-fixable contract-text defects**
and swept into **CONTRACT.md v1.1** by Fable as contract author (Track-A
P0 precedent). No §14 judgment call touched; no policy changed. Resolutions:

- **F-1** allowlist now `src/app/server/rama/util_fns.cljc` (verified on disk).
- **F-2** gate 14 is **P6-only**; P5's gate list is now "gates 16, 17".
- **F-3** §2.3 rewritten: `Trail*` e/defns are THIS package's names, each
  calling the named WP1 §7 `read-*` wrappers; `WatchIngestEpoch` reads the
  ingest-epoch atom — codified as **S1a carve-out** in §10 (the spec's S1a
  reading is now contract text, not just derivation).
- **F-4** §8 fixtures + gate 4 split into two salt classes: audit top-missing
  (covered post-regen → verbatim/advance assertions) vs permanently-uncovered
  emoji/astral + surrogate pairs (→ fallback assertions). Must-have coverage
  stays gate 5.
- **F-5** gate 8 + face law 4 + trap 8 reworded to WP1 §9.10 semantics:
  window selection is arrival-only; `:order :arrival|:claimed` sorts WITHIN
  the window; a claimed-side window must not be built. Face law 10's stray
  "clock param" → "order param".
- **F-6** §5.1 label fixed: "(V3-5 shaper half)".
- **F-7** `src/app/file_viewer.cljc` path spelled in §2.3, §12 allowlist, §13.
