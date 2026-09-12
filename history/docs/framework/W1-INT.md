# W1-INT — integration artifact (faces-as-assemblies, Wave 1)

2026-07-11 · Fable orchestrating session (lanes A/B/C ran as parallel Opus 4.8
subagents; INT implemented directly per the 2026-07-05 (e2) ruling).
Binding: CONTRACT v1.1 §§2, 5, 11 (G14–G16).

## What was wired

The first assembly-hosted pane, mounted through `<world-snapshot` per the §5
flow discipline — the `<trail-face` shape exactly:

- **Atoms** (`runtime/state.cljs`): `:!face-state` (entry state, /face command)
  · `:!face-context` (the §7 data-context, mirrored whole — no split) ·
  `:!face-compiled` (wear-time compiled builder, trap T3) · `:!face-scene`
  (scene cache; combined_text flattens + mouse hit-tests THE SAME object,
  trap T9).
- **Artery hookup**: `electric_flow.cljc` threads `!face-request`/`!face-data`
  into `start-loop!`; `runtime.cljs` installs `face-wiring/install-face-wiring!`
  (extended with the `!face-data → :!face-context` mirror — the R4 pattern,
  matching its own docstring); INV-19 epoch debounce verbatim from trail.
- **Mode**: `:face-assembly` in `derive-effective-local-world` (wins over trail
  faces while set; full-screen; sidebar force-hidden — the trail R1 ground
  rule inherited); `:panes` entry (load-bearing — the case has no default);
  `local-world-face-assembly?` helper.
- **Flow** (`editor_compute.cljs`): ONE `m/latest` over (layout, face-state,
  face-context, compiled); input VALUE compare via `!last-face-struct`
  (trap T13; reset to sentinel on face exit so re-entry with identical inputs
  rebuilds — a latent nil-scene edge the trail flow shares); `apply-assembly`
  with `:view-instance :face-main`, address = resolved conversation address
  (falls back to the request address), geom per §5 (`:content-w` inset 32px —
  see Lacks); scene cached in `!face-scene`; multiplexer arm `:face-assembly`.
  Does NOT watch `!scroll-y` (§10 SLOT-C).
- **Text** (`combined_text.cljs`): face branch folded into the trail
  scene-flatten arm (same cached-scene law, gate 13/A3).
- **Scroll** (`runtime/scroll.cljs`): face clause clamps `!scroll-y` against
  the interpreter-declared `:assembly/content-h` (camera pan-y = −scroll-y;
  scroll never baked into the tree). Consistency fix while here (R-1 debt 1,
  the mouse.cljs rule applied to the wheel): `in-sidebar?` now consults the
  full-screen modes — the raw sidebar atom could route wheels to the HIDDEN
  sidebar in trail/face modes.
- **Mouse** (`runtime/mouse.cljs`): face-mode click guard — v0 assemblies are
  read-only (no `:actions`, §4); without the guard clicks fall through to the
  editor cursor mutation. Hit-testing arrives with `:actions` at D-008 item-5.
- **Command** (`agent_flow.cljs` + parser/wear in `face_wiring.cljs`):
  `/face <name> [<address-edn>]` (address defaults `:default` → server
  first-light address) · `/face until <ms>|off` (the `:until-ms` scrub) ·
  `/face off`. `:set` is wear-time: fetch `/faces/<name>.edn` → ONE
  `compile-assembly` against the real registry → `!face-compiled`; fetch
  failure compiles a root-less envelope → visible error card (trap T4), never
  a black screen.
- **Worn assembly**: `resources/public/faces/outline.edn` — authored against
  the REAL vocabulary (`:stack`/`:badge`/`:indent-rail`/`:text-run`), bind
  paths = the §7 data contract. A DIFFERENT artifact from lane A's
  `test/app/fixtures/faces/outline.edn` (interpreter golden over stub
  primitives) — two artifacts, two jobs, no two-truths: the worn one is
  smoke-tested against the real registry in the suite (below).
- **Dev observability**: `window.__softland_atoms` (runtime.cljs) — read-only
  atoms handle; drove every live check below.

## Touch-list deviations (recorded, not improvised)

1. **`render.cljs`** (not on the CONTRACT §2 shared list): the flow builders'
   call sites live there (`:43-66`), so threading the four face atoms required
   two mechanical pass-through edits — the identical shape the trail atoms
   already ride (`:32-33, :64`). No frame-path logic touched.
2. **`agent_flow.cljs`** (touch-list said `workspace_actions.cljs` command):
   the shipped command dispatch lives in `agent_flow.cljs`
   `submit-agent-run!` (parse chain + case arm) — the touch-list named the
   file by role; the role lives one file over. `workspace_actions.cljs` got
   the mode/panes changes as predicted.
3. **`face_wiring.cljs` / `face_assembly.cljc`** (lane files, post-lane): the
   data mirror + parser/wear fns (consistent with lane C's own docstring), and
   one INT defect fix in lane A's file (below).

## Defects found + fixed at INT

- **`face_assembly.cljc:59` — guard threw on cljs** (trap T4 class): the
  `guard-reason` `:else` used `class`/`.getSimpleName` (JVM-only); in the
  browser the guard's disallowed-value branch would throw inside the layer
  that must never throw. Platform-split with a reader conditional. Found by
  the shadow build at INT — the exact "JVM tests green, cljs half never
  compiled" class the wave QC exists to catch; G16 falsification includes a
  parity hunt for more of this class.

## Orchestrator ruling at INT

- **G12 `read-source`**: lane C's `:until-ms` reads `created-at-ms` via the
  existing `read-source` query API (no block-material API carries time).
  ACCEPTED — read-only, existing named API, `:limit`-bounded, no kernel edit;
  T12's "query APIs only" law holds. CONTRACT §11 G12 amended in place
  (dated note, D-010 reach; commit `fadd067`).

## G14 — the worn-UI structural-equality falsifier

**(a) JVM instance — PASS** (`test/app/face_integration_test.clj`, in the
suite: 2 tests / 24 assertions / 0 fail). The named slice: the expansion
card's HOLES COLUMN (`scene.cljc:366-372, :399-402`) — shipped
`hole-endpoint-card`s at y = i×(4+line-height) vs the same sub-scene as
`:stack` + `:each` + `:hole-card` through `compile/apply`:
- children structurally EQUAL — ids, bounds, styles, text, data (`=`, whole
  nodes); both flatten through the real render primitives with equal op
  counts;
- **one real arrangement finding, asserted exactly**: the shipped math
  allocates a TRAILING gap (h = n×(gap+row-h)); `:stack` gaps only BETWEEN
  children (h = n×row-h + (n−1)×gap) — container height differs by exactly
  one gap. The vocabulary cannot express "gap-after-each" without a trailing
  pad. Recorded as a named vocabulary gap; drift beyond the named delta still
  fails the suite.
- honest id fallback: hole rows carry `:relation-id` not `:id` → counted in
  the apply-report (3), asserted.
Plus the registry-wiring smoke: the WORN outline.edn compiles CLEAN against
the real 16-prim registry and applies over a projection-shaped context —
zero missing binds, zero missing ids, positive content-h, every block's
prose + speaker badges present in the flattened ops.

**(b) live instance — recorded verbatim** (`g14b-verbatim.json` alongside
this artifact; dev app, fixed state: 2 home-dirs, no project, geom 260×800):
counts match (2 = 2) and text content matches, but **structural equality
FAILS on named dimensions** — and the cause is a finding, not a bug:
`build-sidebar-tree` never went through the extracted vocabulary. It builds
raw `:sidebar-entry` nodes (path-string ids, text x=16 y=21.6 size 14,
row h 32/y-offset 48) while the vocabulary's `:list-item` is the
`ui_primitives` builder (`:ticket-row` nodes, folded keyword ids, leading-slot
text x=48 y=22.8 size 12, row h 36). The CONTRACT's G14(b) premise ("the
sidebar list ... through an assembly") embedded a factual mismatch: the
sidebar predates the vocabulary; the vocabulary's shipped callers are the
flow-intake ticket list (`dg_flow.cljs:736`). Equality at assembly grain is
PROVEN where the vocabulary is the source — instance (a) and the smoke test.
Re-expressing the sidebar faithfully needs either a `:sidebar-entry`
primitive (gap-fill, §6's "a missing primitive is a cljc function away") or
the post-wave harmonization. Logged under Lacks; the JVM instance (a) is the
suite's permanent falsifier, as the contract's own carve-out says.

## G15 — the wearing (dev app, real `7c80ce2a` conversation)

- **Full path proven live**: `/face outline` (bottom-bar command) →
  `!face-state` → generic request → `FacePull` → first-light runtime boot →
  harvest + distill of the real transcript (server log:
  `chat:10c22f9b… river=247` — exactly the G10 baseline) → data-context →
  interpreter → `<world-snapshot` → GPU pane.
- **Worn numbers**: 35 turns · 64 blocks (page 1 of 247, `:truncated? true`,
  `:conversation/paging-lack :river-page-has-no-cursor` surfaced) ·
  `:assembly/content-h 17041` · apply-report
  `{:items-without-id 0 :binds-missing 0}` — the report does not lie and has
  nothing to confess.
- **Scrub works and is prefix-visible**: `/face until 1783334010000`
  (2026-07-06T10:33:30Z, inside the opening burst) → 16 turns · 41 blocks ·
  content-h 6954; cuts at 10:45Z and 13:00Z correctly return the full first
  page unchanged (79 events precede 10:39, none in 10:39–10:45 — page-1
  truncation masks later cuts; verified against the raw transcript
  timestamps). `/face until off` restores.
- **Lifecycle defect found by the wearing, fixed, re-verified** (the G15
  dynamic doing its job): the first-light distill did NOT bump the ingest
  epoch (only `ingest_watchers.clj:113` did), so INV-19 never re-pulled and
  re-issuing an IDENTICAL `/face outline` couldn't re-fire the Electric
  watch (equal value → no propagation) — the face stayed stuck empty,
  masking the now-present truth. Fix: the distill IS an ingest — it bumps
  `util-fns/!ingest-epoch-atom` like every watcher import
  (`file_viewer.cljc`, W1-INT). **Verified on a fresh JVM**: single
  `/face outline`, zero re-requests → face auto-filled to 35/64 after the
  distill landed, purely via epoch push → 1s debounce → re-stamped request
  → re-pull.
- **Screenshots** (CPU rasterization of the live `!face-scene` — see
  environment lack #5): `W1-INT-outline-face-top.png` (first viewport:
  speaker badge `human:external`, kind badge `:human-message`, indent rail,
  Sid's actual 2026-07-06 prompt wrapped) ·
  `W1-INT-outline-face-full.png` (full 17k-px scene) ·
  `W1-INT-outline-face-scrub.png` (the 10:33:30Z cut). The prose's ~72-char
  lines are the SOURCE's own hard newlines rendered faithfully.

## §10 SLOT-A browser re-verify (record, don't gate)

Worn outline assembly over synthetic conversations, real registry, Chrome/V8
(warm 20, median of 50): **2.6 / 6.0 / 11.8 ms @ 221/551/1101 nodes** —
faster than the JVM shape-proxy (5.0/12.1/24.4), still ~linear. The §10
budget language holds with margin; no activation.

## Serial suite (G16 step 1)

`app.face-assembly-test` + `app.face-primitives-test` +
`app.face-projection-test` + `app.face-integration-test`:
**22 tests · 281 assertions · 0 failures · 0 errors.**

### Lacks exposed by the wearing (D-005: each is ORDERED data/vocabulary work)

1. **Paging**: `river-page` has no cursor — the face wears only the FIRST
   ≤64-block page of a 247-event conversation; surfaced honestly as
   `:conversation/paging-lack` in the data-context. First data-work item;
   full paging is block-kernel CONTRACT §10 scale work (kernel lane, not
   grammar growth).
2. **`content-w` does not narrow with indent** (lane A design note, seen in
   the worn face): deep-indented `:text-run`s wrap at full content width —
   the interpreter threads geom down unchanged (children build post-order).
   Mitigated at INT by insetting geom content-w 32px; a real narrowing
   mechanism (primitive-scoped geom) is a named interpreter extension point.
3. **Trailing-gap convention** (from G14(a)): `:stack` cannot express
   gap-after-each; a `:pad-after`/trailing-pad affordance is a vocabulary
   candidate if a designed face needs it.
4. **`:sidebar-entry` primitive missing** (from G14(b)): the sidebar's row
   language is not in the vocabulary; needed before the sidebar can be worn
   as an assembly.
5. **Environment lack** (not the wave's): GPU canvas capture is unavailable
   under headless/xvfb on this box (Vulkan swapchain bypasses compositor
   readback in every Chrome mode tried) — the worn-face screenshot for Sid is
   a CPU rasterization of the LIVE `!face-scene` tree (the exact rects +
   text ops the GPU consumes), labeled as such. A native-session screenshot
   remains worth taking when Sid next runs the app.

## §10 SLOT numbers (browser re-verify — record, don't gate)

- SLOT-A (apply cost, browser): see run record below (JVM shape-proxy said
  5.0/12.1/24.4 ms @ 201/501/1001).
- SLOT-C (scroll): the face/camera convention wired as specified — scene
  scroll-independent, `:assembly/content-h` declared by the interpreter,
  wheel clamps against it, camera pan carries the offset.

## Run record — how the live checks were driven

Headless Chrome (150, `--headless=new --enable-unsafe-webgpu`) via the
repo's own puppeteer devDependency; app state read/driven through the
`window.__softland_atoms` dev handle + synthetic window KeyboardEvents (the
app's one keydown listener is on `js/window`; the cmd panel boots VISIBLE —
the driver normalizes to visible+focused before typing). WebGPU renders
(real Vulkan adapter; RENDER/FRAME logs + pool diffs healthy at 262 rects)
but its swapchain never composites into CDP/X screenshots on this box in any
mode tried (headful+xvfb, headless=new, swiftshader) — hence the labeled CPU
rasterization of the live scene tree for visuals (environment lack #5).
