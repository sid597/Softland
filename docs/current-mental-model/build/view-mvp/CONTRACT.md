# View-MVP Contract — WP-B2 (the pixels; read-only screenshot loop)

Status: v1 **BINDING** (Fable, 2026-07-04, Track-B session; **countersigned
by Sid 2026-07-04 in-session** — "yes to all 3", covering all 8 §14
judgment calls. The countersigned v1 includes the same-day pre-countersign
gate-5 amendment: U+FFFD fallback in the atlas must-have set, from the
charset audit).

Implements the pixel side of D-002 (first form = trail view) under D-008
(read-only MVP, CLOSED) and the face-order ruling (View 3 → threaded/DAG
timeline → canvas). Consumes exactly two upstream artifacts: the
render-substrate retro (`build/render-substrate-retro/{RETRO,PRIMITIVES}.md`
— as-built verdict + fix list) and the trail-view WP1 data contract
(`build/trail-view/CONTRACT.md`, BINDING — cited as "WP1 §n"; its §7
wrappers are this package's ONLY data surface). Prior trail-view/design docs
are input, not authority. Track D's ideal-framework study is **non-binding
on this package**: its delta instrument applies at the face gates when it
exists (slot named in §12); nothing here waits for it.

---

## 1. Purpose and scope

Make the read-only loop of D-008.4 real: **Sid works in the CLI → watchers
re-ingest → the view updates near-live → a screenshot is a resolvable
pointer back into the land.** Four deliverables:

1. **View-3 face** — WP1 §8's text projection rendered in the land
   (verbatim, marker-colored), addressed.
2. **Threaded/DAG timeline face** — the 27-04 outline form as pixels:
   recent-activity feed entries as typed cards in session/target threads,
   relation edges as connectors, dead-ends terminal, verdicts badged,
   staleness visible, addressed.
3. **Watcher triggers** — OS-side loop over the EXISTING ingestors
   (transcripts, md), per WP1 §9.8/§12 and D-008.3, driving near-live
   updates.
4. **Substrate honesty fixes** the faces newly make load-bearing (retro
   V3-5 glyph robustness, T-4 clip containment).

Consumers, in order:
1. **Sid's daily loop** (H1's kill/confirm clock starts at first render
   over real material).
2. **Agents, indirectly** — a screenshot of any face resolves through its
   rendered address to the same WP1 query an agent can run (I-14).
3. **The H3 benchmark**, later — reads WP1 wrappers directly; this package
   only guarantees the addresses it would screenshot are honest.

Non-goals in §9. Phases and the WP1 dependency boundary in §12.

## 2. Placement rulings

### 2.1 Pure-cljc face core (the load-bearing placement)

All face logic that turns WP1 data into scene trees and text ops is
**pure `.cljc`** in a new directory `src/app/client/workspace/trail_face/`:

- `sanitize.cljc` — codepoint-level op sanitizer (V3-5 client half):
  coverage-set lookup, fallback substitution, surrogate-safe counting.
- `text_face.cljc` — View-3 face: WP1 §8 text → line ops, marker→style
  table keyed to the `;; trail-text v0` version header.
- `cards.cljc` — kernel-material card builders: feed entries, bundle
  layers, verdict badges, omission blocks, hole-endpoint rendering.
- `lanes.cljc` — thread/lane assignment + Manhattan connector geometry.
- `scene.cljc` — face scene assembly (rt-node trees), windowing (if/when
  gated in), scroll clamp math.
- `wiring.cljs` — the ONLY cljs file: atoms, flows, mode registration glue.

Why not the alternatives:
- **Face logic in `.cljs` interleaved with atoms (the trail.cljs/dg_flow
  precedent):** untestable without a browser — the falsifiable-gates
  requirement of this contract dies first (trap 4). The as-built codebase
  already shows the cost: zero executable gates exist for any current view.
- **A new framework layer:** forbidden — Track D territory; D-001 says the
  runtime grows only on form-break, and no face exists yet.

Reversal cost: pure fns with data in/data out — relocating or renaming
costs consumers a require line. The seam: `wiring.cljs` and the Electric
bridge are the only places allowed to touch atoms/flows/DOM; everything
below them is a function of (data, viewport-geometry) → (tree/ops).

`rect_tree.cljs` is **promoted to `rect_tree.cljc`** (rename; the file is
already pure — zero js interop, verified in the retro read). This puts
`resolve-layout`/`tree->rects`/`tree->text-ops`/`hit-test` on the JVM where
the gates run. Consumers keep the same ns name; shadow-cljs and JVM both
resolve cljc.

### 2.2 Mode registration (not a new shell)

The faces register as local-world modes via the as-built 7-step recipe
(PRIMITIVES.md §recipe; dg_flow precedent) — mode branch + predicate +
build/compute fns + two flow branches + render-consumer args + scroll/mouse
zones + entry command. **No router is built.** Friction with this recipe is
Track-D data (retro §5), not this package's problem to solve.

### 2.3 Electric bridge

New e/defns in `file_viewer.cljc` (the ×5 precedent), calling **only WP1 §7
wrappers** inside `e/server`: `TrailBundle`, `TrailFeed`,
`TrailConversation`, `TrailText` (pull, address-shaped args) and
`WatchIngestEpoch` (push; §6). Client side: results land in atoms threaded
through `start-loop!` like the existing five.

### 2.4 Watchers

New ns `src/app/server/ingest_watchers.clj` — **outside every Rama module**
(placing it in trail-view-module would put a writer inside the
read-only-by-construction module and break WP1 gate 14; placing it in a
kernel module couples OS I/O to module deploy). It watches the configured
roots (Claude transcript dir; docs/vision md trees), debounces, and calls
the EXISTING import entry points only (convergent re-import is the safety:
deterministic ids + idempotency journals, D-008.3). After each completed
import batch it bumps an **ingest-epoch mirror atom** (server side, the
util-fns pattern) that Electric pushes; the client re-pulls the feed on
epoch change. Scope note (identity law): the epoch is a **monotonic
per-server counter, not truth** — it carries no data, orders nothing
semantically, and resets on restart; every rendered timestamp still comes
from WP1's two clocks.

## 3. Face laws (what "rendered" means, both faces)

1. **Address law (I-14, WP1 §3):** every face renders its own address as
   one text line in its header; the line round-trips through WP1
   `resolve-address`. Card expansions render the expanded target's address.
2. **View-3 verbatim law:** the View-3 face renders `render-bundle-text`
   output **line-for-line** — same line count, same per-line character
   sequence (soft horizontal overflow is scrolled, never re-wrapped;
   re-wrapping would make the screenshot diverge from what an agent reads
   at the same address: trap 3).
3. **Marker coloring is total and versioned:** one marker→style table,
   keyed to the `;; trail-text v0` header. Unknown line shapes render
   `:normal`, never dropped. A version-header mismatch renders the whole
   projection plain + one visible notice line (honesty over prettiness;
   trap 12).
4. **Two clocks at the pixel layer (WP1 §6, trap 4):** timeline default
   clock `:arrival`; the active clock is part of the face's rendered
   address params; entries render BOTH stamps when they diverge
   (`claimed Apr 28 · arrived today`).
5. **Staleness renders differently (I-2):** three visually distinct
   states — attested-recent, attested-stale, and **never-attested/unknown**
   (`last-attested-ms` nil). Unknown must not read as fresh; `walked
   unknown` renders verbatim from the projection.
6. **Provenance badges (D-004, I-10):** `asserted-by` always visible on
   relation/verdict cards; `written-by` badge only when it differs (WP1
   §5.1 projection rule); disagreeing asserters both render, badged, never
   merged.
7. **Omissions are pixels (exactness rule):** every `:omissions` /
   `:feed/uncovered` entry in consumed data renders as a visible line/chip
   with its count. A face never renders a capped layer as if complete.
8. **Dead-ends are terminal (D-002):** `:dead-end`-marked targets get the
   terminal treatment; lanes never continue forward out of them.
9. **No content past the shaper that the atlas cannot draw (V3-5):** ops
   are sanitized codepoint-wise against the loaded font's coverage set;
   uncovered codepoints render the fallback glyph (visible tofu — honest),
   never vanish. Advance counting is codepoint-correct (surrogate pairs =
   one advance).
10. **Read-only (D-008.1):** zero kernel writes from any face code path.
    Local view-state (scroll, clock param, collapse, selection) is atoms
    only.

## 4. The timeline face, concretely (v0 composition)

- **Nodes:** feed entries (WP1 §6 shapes) grouped into threads by
  conversation / target family; a thread is a lane. Cards carry kind
  glyph + display name + two-clock stamps + actor badges + address.
- **Edges:** relation rows (from bundle L3 on expanded targets and from
  `:relation-transition` feed entries) drawn as Manhattan connectors
  (vertical lane spines, elbow joints, junction dots — thin rects only,
  retro T-3). Edge color/alpha carries kind + status (`:retracted` renders
  struck/dim, still visible with history affordance).
- **Expansion:** clicking a card pulls its context bundle (Electric pull by
  address) and expands in place: material preview (mechanical truncation
  only — WP1 §9.6 already guarantees no paraphrase), relations (`:this` +
  `:in-family`), verdict fold with badges, omissions block. Conversation
  cards page through `conversation-trail` (cursor from WP1 §7).
- **Windowing:** NOT built in v0. The measurement hook is the existing
  `[RAF]`>5ms instrument; gate 15 makes the decision evidence a package
  artifact (retro T-5 discipline: measured break, not imagined).
- Scene built ONCE per data/viewport change into a cached scene atom
  (sidebar `!sidebar-scene` precedent); render flattens from it, clicks
  hit-test against the SAME tree (retro T-6; trap 1).

## 5. Substrate amendments (bounded, named)

1. **`renderer.cljs` shape fns (V3-5 server... client half):** missing
   glyph → advance anyway + draw fallback glyph if the atlas has one; never
   zero-advance skip. Bounded to `shape-msdf-line`/`shape-slug-line`.
   (Defense-in-depth behind the cljc sanitizer; the sanitizer is the
   gate-tested layer, this is the belt-and-suspenders the diff review
   checks.)
2. **`rect_tree.cljc` clip clamp (T-4):** with `:clip?`, partially-visible
   bg rects are clamped to the intersection with clip bounds (axis-aligned
   intersection; radii degrade at clamped corners — acceptable, noted).
   Fixes the suspected chat-pane bleed as a side effect.
3. **Atlas regen** (`resources/public/font_atlas.{json,png}`): charset
   widened per `CHARSET_AUDIT.md` (this directory) — the audit's missing
   set drives the msdf-atlas-gen charset file; audit re-run green afterward
   (gate 5's fixture includes the former top-missing chars).

Nothing else in the substrate changes. Explicitly untouched: draw-frame!
order, camera/zoom, dirty-present flag, buffer pools, font backends.

## 6. Near-live loop (watchers + epoch)

- Watch roots (config, not code): the Claude projects transcript dir for
  this repo; `docs/`; `vision/`. Debounce ≥ 500ms per path; serialized
  import per file; failures logged and retried on next change event, never
  crash the watcher loop.
- Import completion (the existing import fns return / their completion
  PStates update — implementer verifies which is observable, Phase B0) →
  bump ingest-epoch atom → `WatchIngestEpoch` pushes → client re-pulls the
  feed for its current window/clock → scene rebuilds → RAF paints. No new
  ingestors, no new truth (D-008.3; trap 2).
- **Perf promise + read plan:** one feed re-pull per epoch bump (debounced
  client-side to ≥ 1s), cost = WP1 §6's read plan. No polling loops
  anywhere (quirks discipline).

## 7. Traps ledger (naive choice → concrete failure → ruling)

| # | Naive choice | Concrete failure | Ruling |
|---|---|---|---|
| 1 | Rebuild face trees per consumer and per click (chat/flow precedent) | Click resolves against a tree that differs from the rendered one; 3× build cost; the exact drift class the retro found as-built | §4: one cached scene per change; hit-test and flatten share it (gate 13) |
| 2 | Watchers inside trail-view-module, or as new ingest paths | WP1 gate 14 (read-only by construction) breaks; or a second truth path duplicates ingest semantics and drifts | §2.4: separate plain ns; triggers call EXISTING import fns only; epoch is a counter, not truth |
| 3 | Client re-wraps/reflows the View-3 projection | Screenshot text ≠ what an agent reads at the same address — the pointer lies; wrap math diverges per viewport | §3.2 verbatim law; horizontal scroll, never re-wrap (gate 2) |
| 4 | Face logic in .cljs coupled to atoms | No executable gates possible without a browser harness; validation collapses to "looks right" | §2.1 pure cljc core; .cljs is glue only (gates 1–13 run on JVM) |
| 5 | Keep dropping uncovered glyphs (as-built behavior) | Rendered claims silently lose characters; column math desyncs; the map lies at the pixel level | §3.9 sanitizer + §5.1 shaper fix + §5.3 atlas regen (gates 4, 5) |
| 6 | Build the diagonal/bezier edge pipeline now | GPU shader work for an unvalidated aesthetic; D-001 inverted | §4: Manhattan v0; curves are a Sid-gated follow-up (retro O-1) |
| 7 | Build card windowing now | Complexity ahead of measured need; editor precedent shows the fix is retrofittable | §4: v0 builds all cards; gate 15 records the measurement that decides |
| 8 | Timeline on one clock | A July re-import of April notes floods "today" or vanishes — WP1 trap 4 reproduced at the pixel layer | §3.4: clock in the address, both stamps rendered (gate 8) |
| 9 | Insert the new scroll zone anywhere in the wheel cond | The cascade is order-sensitive (flow checked before chat today); wrong order silently steals editor/chat scroll | §12 Phase C duty: zone placed + plan-validated against the existing cascade order; behavior spot-checked at first light |
| 10 | Reuse trail.cljs agent-run card kinds for kernel material | `:reasoning`/`:tool-call` semantics collide with land kinds; collapse-id namespaces collide across panes | §2.1 `cards.cljc` is a separate builder set; collapse-ids namespaced `:trail-face/*` |
| 11 | Poll-until-rendered in watcher/integration tests | Polling can't prove a no-op (quirks law); flaky suites | §6: tests latch on import-fn return / completion read, then assert epoch delta (gate 14) |
| 12 | Second ad-hoc parser for the §8 text format | Two grammars drift; face colors lie about structure | §3.3: one marker table keyed to the version header; unknown → :normal; version mismatch → plain + notice (gate 3) |
| 13 | Feed the projection/cards straight to ops without sanitize | One emoji in a commit message desyncs a whole line's clip/hit math even after the shaper fix (count vs codepoint) | §3.9: sanitize at op emission; codepoint-correct counting everywhere in trail_face (gate 4) |

## 8. Fixtures

`test/resources/trail_face/` — EDN fixtures shaped EXACTLY per WP1 §4/§6/§8
(bundle with all six layers incl. `:in-family` verdicts + omissions; feed
window with a back-dated entry, an uncovered-declaration, a dead-end, two
disagreeing asserters, a `:written-by`-differs row; a §8 v0 text projection
sample incl. `walked unknown`; a hole-endpoint row; strings salted with
CHARSET_AUDIT's top-missing codepoints). Fixtures re-derive from WP1
CONTRACT §4/§6/§8 shapes; **if WP1 impl contact amends those shapes, the
fixtures re-derive in the same session that ingests the amendment** (named
cheap re-derivation, not a risk absorbed silently). After WP1 gates green,
one integration fixture is captured from the live wrappers and diffed
against the hand fixtures (gate 16).

## 9. What this contract refuses (extension points, not voids)

1. **No write surface, no walk capture** — D-008; read→write is the named
   second milestone. Local view-state atoms only.
2. **No canvas face, no zoom activation** — parked by the face-order
   ruling; the camera capability note lives in PRIMITIVES.md §camera.
3. **No question-unit design** — queued item; but gate 12 keeps the door
   open (hole-endpoint rows render, never crash).
4. **No framework extraction, no view-spec interpreter UI** — Track D
   direction-land; view-specs resolve through WP1 §3 addresses (data), and
   the face renders what resolves.
5. **No H3 benchmark harness** — reads the same wrappers, separate work.
6. **No dirty-present re-enable, no new GPU pipelines** — perf levers
   stay dormant until gate-15-style evidence demands them.
7. **No edits to** trail.cljs (agent-run cards), dg_flow, sidebar, editor
   behavior — the 7-step wiring touches shared files additively (mode
   branches), never alters existing modes' output (gate 17 regression).

## 10. Style gates (each names where it stops)

- **S1 Seam:** Electric bridge + faces call ONLY WP1 §7 wrappers — no
  `foreign-select`, no PState names, no kernel write fns anywhere under
  `trail_face/` or the new e/defns. *Stops at source scan; runtime paths
  are gate 14's integration test.*
- **S2 Purity:** `trail_face/*.cljc` contains no atoms, no js interop, no
  reader conditionals except where a platform shim is unavoidable (then the
  shim is 1 fn, named). *Stops at the file boundary; wiring.cljs is exempt
  by design.*
- **S3 Char-advance:** face code takes `char-advance` as an argument;
  the literal `0.56` appears nowhere in `trail_face/`. *Stops at the new
  dir; the ~30 legacy sites are out of scope (CLAUDE.md law covers them).*
- **S4 Control bytes:** NUL and control chars only as `\uXXXX` escapes in
  source and fixtures (both prior traps fired; quirks law). *Stops at
  source/fixture files.*
- **S5 Collapse-id namespace:** every interactive id under `trail_face/`
  is `:trail-face/*`-namespaced. *Stops at the new dir.*

## 11. Acceptance gates (executable; green = done)

JVM tests (`clojure -M:test`, new ns `app.client.workspace.trail-face-test`)
over §8 fixtures unless marked otherwise. Every gate names its falsifier.

1. **Address law:** each face scene (View-3, timeline, expanded card)
   contains exactly one header address line; the parsed address round-trips
   through WP1 `resolve-address` shape-equal. *Falsifier: a face without an
   address, or an address that doesn't parse.*
2. **View-3 verbatim:** rendered line count == projection line count; per
   line, the concatenated op text == the projection line, byte-for-byte
   after sanitize (fallback substitutions counted, positions preserved).
   *Falsifier: any dropped/reordered/re-wrapped character.*
3. **Marker totality:** every line of the fixture projection maps to a
   style; a mutated unknown-marker line maps to `:normal` (not dropped); a
   mutated version header yields plain + exactly one notice line.
4. **Sanitize correctness (V3-5):** for fixture strings salted with the
   audit's top-missing codepoints + surrogate pairs: op codepoint count is
   preserved, uncovered codepoints map to the fallback, advance count ==
   codepoint count. Coverage set loaded from the REAL
   `resources/public/font_atlas.json`. *Stops at the cljc layer: does not
   execute the GPU shaper — the renderer.cljs fix (§5.1) is verified by
   diff review + first light.*
5. **Atlas coverage regression:** after §5.3 regen, the audit's named
   must-have set (box-drawing `─│═` class, arrows, curly punctuation,
   ✓/★/⚠ class, **and the fallback glyph U+FFFD itself** — without it the
   sanitizer's substitution renders nothing) is present in the atlas JSON.
   Audit baseline (2026-07-04): every existing atlas is 95-glyph printable
   ASCII; 69.8% of md docs / 26.8% of transcript messages carry ≥1
   uncovered char; top-missing are structural (`─` 1.13M, `→` 371k,
   `—` 219k). *Falsifier: regen silently narrows, or ships without the
   fallback.*
6. **Clip containment (T-4):** fixture scene with cards straddling a
   `:clip?` boundary → every emitted bg rect within clip bounds; the same
   assertion over the timeline face at three scroll offsets. *Falsifier:
   any rect outside bounds (the as-built bleed).*
7. **Lane/connector geometry:** lane assignment is deterministic (same
   fixture → identical output twice); every connector endpoint touches its
   card's bounds; dead-end lanes emit no forward segment past the terminal
   card. *Falsifier: nondeterminism, floating connectors, ghost lanes.*
8. **Two clocks:** the back-dated fixture entry appears under `:arrival`
   today and not under `:claimed` today; its card renders both stamps;
   switching the clock param changes the rendered address line.
9. **Staleness triad:** attested-recent / attested-stale / never-attested
   fixtures produce three pairwise-distinct style outputs; nil ≠ fresh.
10. **Badges:** `asserted-by` present on every relation/verdict card;
    `written-by` rendered exactly when it differs; two disagreeing current
    verdicts both render badged.
11. **Omissions are pixels:** every fixture omission (bundle caps + feed
    `:feed/uncovered`) yields a visible op with its count; forced caps
    change renders, never silently narrow.
12. **Hole endpoint:** the hole-shaped fixture row renders with the
    explicit hole style; no exception, no silent skip.
13. **Cached scene identity:** two flattens from one scene atom are
    `identical?`-stable inputs; hit-testing a fixture click resolves
    against the same tree object the flatten used. *Stops at structural
    identity; real mouse routing is first-light.*
14. **Watcher loop (JVM integration):** temp watch root + fixture md file:
    write → import fires (latched on the import call's completion, no
    polling) → epoch bumped by exactly 1; identical rewrite → convergent
    re-import → feed entry count unchanged. *Stops at the server loop;
    Electric push is first-light.*
15. **Windowing evidence:** the package dir contains
    `MEASUREMENT_RAF.md` recording the `[RAF]` numbers for the timeline
    face over a real day's material (from the first-light session). If
    p95 dirty-frame > 5ms at that volume, T-5 windowing enters as an
    amendment phase; the gate asserts the artifact exists and states the
    ruling either way. *Stops at evidence-recorded; the fix itself is
    conditional by design (D-001).*
16. **Fixture fidelity (post-WP1-green):** one live capture from WP1
    wrappers over the WP1 gate fixture diffs shape-equal against this
    package's hand fixtures (keys/nesting, not values). *Falsifier: the
    hand fixtures drifted from the real shapes.*
17. **Regression:** relation-kernel + WP1 suites green unmodified; shadow
    build compiles; the touched shared files' existing-mode outputs are
    byte-identical for a non-trail fixture world (editor/flow modes render
    the same ops with the trail mode absent). *Stops at fixture-level
    regression; visual parity of live panes is first-light.*

**First light (not an IPC gate; the H1 clock-start record):** Sid opens the
two faces over real material; screenshots one; the address in the
screenshot re-resolves in CLI to the same content modulo drift stamp. The
session records `FIRST_LIGHT.md` (what rendered, what jarred, the RAF
numbers for gate 15) — this artifact is where D-001 form-break evidence for
the next slice accumulates, and where Track D's delta instrument gets its
first application when it exists.

## 12. Work-package handoff

- **Implementer:** Opus 4.8 / Codex, fresh session per phase, per
  `/work-package`. This is client+plain-server work — the `/rama` skill is
  NOT in force (no Rama modules are touched); the quirks file and CLAUDE.md
  Missionary/Electric patterns ARE (m/latest not m/ap+m/?<; eduction+deref
  for event filtering; no side effects in m/latest; try/catch not inside
  e/defn).
- **Phases:**
  - **B2-P0** — fresh-context spec re-derivation (IMPLICIT_SPEC.md): face
    laws, gate matrix (gate × law × fixture), inheriting THIS contract's
    seam context (WP1 wrappers only). *(Runs now; no WP1 dependency.)*
  - **B2-P1** — plan (PLAN.md): file-level changes incl. the 7-step wiring
    sites with exact insertion points, the rect_tree promotion, fixture
    derivations, watcher design against the verified import entry points.
    *(No WP1 dependency.)*
  - **B2-P2** — plan validation (fresh, default-fail; per-round artifacts,
    never overwrite a FAIL).
  - **B2-P3** — pure core + fixtures + gates 1–13 written & compile-checked
    (impl); **parallel-safe with WP1 impl** (fixtures are contract-shaped).
  - **B2-P4** — run gates 1–13 to green; substrate amendments (§5) land
    here with their diff review notes.
  - **B2-P5** — integration: Electric bridge + 7-step wiring + cached
    scene + scroll/mouse zones + entry command; **starts only after WP1
    gates green** (needs live wrappers); gates 14, 16, 17.
  - **B2-P6** — watchers + epoch push (gate 14 full), then **first light**
    with Sid (gate 15 evidence, FIRST_LIGHT.md, H1 clock starts).
  - **B2-P7/P8** — implementation validation + test validation (fresh,
    default-fail, line-cited), then Fable gate review.
- **File allowlist.** NEW: `src/app/client/workspace/trail_face/*` (cljc +
  wiring.cljs), `src/app/server/ingest_watchers.clj`,
  `test/app/client/workspace/trail_face_test.clj`,
  `test/app/server/ingest_watchers_test.clj`, `test/resources/trail_face/*`,
  `docs/current-mental-model/build/view-mvp/*` phase artifacts,
  regenerated `resources/public/font_atlas.{json,png}`. RENAMED:
  `rect_tree.cljs → rect_tree.cljc` (+ T-4 clamp; no other logic change).
  AMENDED, bounded to mode-branch/wiring additions per the 7-step recipe:
  `combined_text.cljs`, `editor_compute.cljs`, `runtime/scroll.cljs`,
  `runtime/mouse.cljs`, `runtime/workspace_actions.cljs`,
  `runtime/render.cljs`, `runtime.cljs`, `runtime/state.cljs` (new atoms),
  `runtime/agent_flow.cljs` (entry command), `file_viewer.cljc` (new
  e/defns), `electric_flow.cljc` (thread new atoms),
  `src/app/server/rama/util_fns.clj` (ingest-epoch mirror atom ONLY),
  `renderer.cljs` (ONLY the two shape fns per §5.1). NOTHING else; trail.cljs,
  dg_flow.cljs, sidebar.cljs, settings, cmd_panel untouched.
- **Verification duties before code (B2-P1):** (a) which import entry
  points exist and what their completion is observably (return value vs
  completion PState) — read `object_container/runtime.clj` + adapters, not
  memory; (b) JVM classpath actually compiles client cljc under
  `clojure -M:test` (spike one require); (c) the wheel-cascade order in
  `scroll.cljs` (trap 9); (d) shadow-cljs resolves the cljc rename.
- **Stop clause:** contract unbuildable / wrong under platform semantics /
  binding-doc conflict (this contract, WP1 CONTRACT, decisions.md) →
  classify implementer-fixable vs policy fork; escalate forks to
  decisions.md Open Questions as PROPOSED with verbatim citations. Never
  improvise on binding docs. Specifically pre-flagged: if WP1 impl amends
  §4/§6/§8 shapes, fixtures re-derive (implementer-fixable, §8); if the
  cljc-on-JVM classpath assumption fails, that is a policy fork (placement
  ruling 2.1 is load-bearing) — escalate, do not quietly move logic to .cljs.
- **Reviewer gate (Fable):** gates re-run independently; traps 1, 3, 5, 13
  spot-checked in the diff; falsification pass per CLAUDE.md; first-light
  artifact read against gate 15.
- **After green + first light:** H1's clock is running; observed read-only
  friction accumulates in FIRST_LIGHT.md toward the read→write milestone
  (D-008.5); the Track-D delta instrument applies at the next face gate;
  canvas stays parked.

## 13. Input manifest (for the D-006 counterfactual probe)

`decisions.md` (D-001..D-008 + 2026-07-04 rulings); `build/trail-view/
CONTRACT.md` (entire); `build/trail-view/INPUTS.md` items 2, 3, 5, 13, 14;
`build/render-substrate-retro/RETRO.md` + `PRIMITIVES.md` (entire);
`build/view-mvp/CHARSET_AUDIT.md`; source: `rect_tree.cljs` (entire),
`trail.cljs:1-150,370-520,1235-1337`, `shell.cljs` (entire),
`combined_text.cljs` (entire), `runtime/render.cljs` (entire),
`runtime/scroll.cljs`, `runtime/state.cljs`, `file_viewer.cljc`,
`renderer.cljs:480-515,1127-1250,1338-1400`, `electric_flow.cljc:455-576`;
CLAUDE.md (Missionary patterns + font recipe); `memory/
implementation-quirks.md`; the `/work-package` skill. Probe question:
"design the read-only view-MVP package contract for these two faces on
this substrate."

## 14. Judgment calls flagged for countersign (one line each)

1. Pure-cljc face core + rect_tree promotion so UI gates run on JVM — §2.1
   (the falsifiability strategy rides on it).
2. View-3 face = verbatim projection display, marker-colored, never
   re-wrapped — §3.2/§3.3, trap 3.
3. Watchers = separate plain ns + ingest-epoch mirror atom; epoch is a
   counter, not truth — §2.4/§6, trap 2.
4. Timeline v0 = feed-entry cards in threads + Manhattan relation
   connectors; curves and windowing explicitly evidence-gated — §4, traps
   6–7.
5. Atlas regen + sanitizer + shaper fix as one honesty unit (V3-5) — §3.9/
   §5, trap 5/13.
6. Mode registration via the as-built 7-step recipe, no router — §2.2
   (friction recorded for Track D, not fixed here).
7. B2-P3 runs parallel to WP1 impl on contract-shaped fixtures; only P5+
   waits for WP1 green — §12 (window efficiency vs fixture-drift risk,
   mitigated by gate 16).
8. First light is the H1 clock-start record and the D-001 evidence ledger
   for the next slice — §11.
