# scene-substrate — CONTRACT v1 (Fable, 2026-07-12; in force)

The base layer under the five unlocks, ruled into settled ground 2026-07-12
(decisions.md, "One render substrate" entry). Derivation + the five-unlock
test: `DERIVATION.md` (same dir). This contract binds the package's phases;
on conflict: decisions.md > this contract > thread file.

## 1. Purpose

Make the one-render-substrate law real in code: one client scene store keyed
`(view-instance, address)`, per-container transforms composed in-shader, one
pick seam returning addresses + context bundles, actions as data. After this
package: any number of view-instances live in frame simultaneously, each
independently pannable/zoomable; every rendered thing resolves to an address
(for the mouse and for agents, through the same seam); writes keep riding
`:object/edit` + the committed echo. Faces, design surfaces, marks, and
later islands are material built INTO this floor.

## 2. Consumers, in order

1. **Faces** (P3 migrates the face path; boxes/minimap/reader wear it first).
2. **block-edit** (its pick + overlay re-point at the store; the write loop
   is untouched).
3. **The design unlock's probes (C1/C2)** — click component → template
   address → edit → echo; per-object drag/resize while designing.
4. **point-and-say** — the context-bundle seam (agent = verifier, not
   hunter-gatherer of context).
5. **Semantic marks** — mark on address → fan-out to every appearance.
6. **scene-diff wearing, islands compositing** — staged consumers; their
   contracts come later and must find nothing to re-plumb.

## 3. Non-goals / refusals (each an extension point, never a void)

- **No islands pipeline** (extension: a container whose content is a
  rendered texture; the transform/composite seam this package builds is
  exactly where it plugs).
- **No gesture-FSM rewrite** — the screen-zone cascade stays until the
  world camera goes live for real (DELTA-B1 Δ14's trigger, unfired).
- **No registry-replaces-case refactor** (Δ12) — opportunistic, per mode,
  when a mode migrates (P5+).
- **No legacy-producer migration in v1 beyond the face path** — editor,
  sidebar, chrome, chat/flow migrate in staged P5+ slices; the store must
  coexist with the monolithic path during migration (trap T6).
- **No optimistic write echo** (settled ground; measured and closed).
- **No new Rama organ.** Settled arrangements commit as assembly events on
  the EXISTING assembly kernel; vocabulary line lands with P3 wearing, not
  before. In-flight gestures stay client-side at 60Hz (trap T10).
- **No rope-tree editor internals** (Sid's chunks ruling — blocks are the
  chunks).
- **No conditional RAF / text-run interning** (Δ15/Δ16 — measurement-gated
  triggers, recorded, unfired).

## 4. Placement ruling

- `src/app/client/workspace/scene_store.cljc` — the store: pure functions
  over an EDN store value (JVM-testable; beside `rect_tree.cljc`, which it
  composes). NOT under `substrate/webgpu/` — the store is
  renderer-agnostic; the GPU is one consumer.
- `src/app/client/workspace/containers.cljc` — container registry +
  transform tree → effective (composed) transforms; pure. Client-side state
  atom + GPU buffer writer live where consumed (`runtime/` + renderer).
- `renderer.cljs` gains: container storage buffer, instance-stride change
  (container index), the two-camera select in the four vertex shaders.
- Reversal cost: store/containers namespaces are additive (delete to
  remove). The one non-additive touch is the instance stride + shader
  change; its seam is container 0 = identity (trap T6) — reverting = drop
  the index field and the buffer, pixels unchanged.

## 5. Pinned shapes (identity AND data-resolution, together)

**View-instance id**: any EDN value, equality-keyed, minted by the
producer; convention `[:vi <kind-kw> <n>]`. Opaque to the store.

**Slot** (the store's unit; one per view-instance):

```clojure
{:vi        [:vi :reader-face 1]
 :container <cid>                 ; int; resolves via the container registry
 :tree      <rt-tree>             ; resolve-layout output, CONTAINER-LOCAL coords
 :ops       {:text [...] :rects [...] :shadows [...]} ; flattened LOCAL ops,
                                  ; computed AT upsert, identity-stable
 :addresses {<address> #{<path> ...}} ; subtree index: address → node paths
 :meta      {:face <assembly address> :src <data address>
             :assembly/src-path <path>}}
```

**Store value**: `{:slots {vi → slot} :index {address → #{vi ...}}}` — the
fan-out index is maintained by upsert/remove, never computed by scan at
read time. Resolution route: `address → (:index store) → vis →
(:addresses slot) → paths → nodes`. One atom holds the store; updates only
at the reduce/consumer edge (CLAUDE.md law).

**Container registry**: `{:containers {cid {:parent <cid|nil>
:x <f> :y <f> :scale <f> :camera :world|:screen :layer <int>}}
:effective {cid [ex ey escale flags]}}` — the transform TREE composes
client-side into `:effective` (groups = nested containers for free); only
effective transforms upload. cid 0 is reserved: identity, `:world`.

**GPU**: storage buffer `array<Container>` (offset vec2f, scale f32, flags
u32 — 16 B); every instance carries `container_idx: u32`. Vertex math:
`world = c.offset + local * c.scale`, then the existing camera transform,
where flags bit0 = screen-camera (pan/zoom become identity). Store coords
are f64 (plain CLJS numbers); buffers hold container-relative f32 — this
IS the Δ2 anchor convention. v1 validated zoom range [0.1, 10]; far-origin
deep-zoom re-basing is a named later, not silently claimed.

**Pick**: `(pick store containers point)` — containers by :layer desc,
inverse-transform point per container, `rect-tree/hit-test` the slots in
that container; returns `{:vi :path :address :src-path :actions :point-local}`
for the deepest addressed node. **Context bundle** (P4):
`{:vi :address :src-path :camera <world-camera + container transform>
:visible #{addresses in viewport, ranked by screen area}}` — plain EDN;
every address must resolve back through existing read APIs.

**Actions**: node `:data` carries descriptors `{:action <kw>
:target <address> …}`; a registered handler map dispatches
(`register-action!`). Descriptors are proto-ActionRequests. Closures in
slot data are a gate failure (G2 walks the value).

**Overlay stratum**: slots may carry `:stratum :world | :overlay`
(default `:world`); overlay slots share the base slot's container and
compose after it. v1 pins the FIELD and the compose order only
(shape-now-fill-later, Δ4); block-edit's existing overlay composition
stays as-is until P3 maps it on.

## 6. Phases

**P1 — the store + containers (pure, JVM-tested).** `scene_store.cljc`,
`containers.cljc`, tests in `test/app/client/workspace/scene_store_test.clj`.
No renderer coupling. Gates G1–G3.

**P2 — the transform leg (GPU).** Container buffer + instance stride +
shader select in all four pipelines; renderer API
`(write-containers! sys effective-transforms)`; container 0 default
everywhere. A dev probe (uncommitted, tagged, delete-to-remove — the
islands-probe pattern) spawns N quads/text runs across containers and
drives one transform. Gates G4–G6. **Fable implements** (risk center:
stride/alignment across four instance layouts).

**P3 — first producer migration: the face path.** apply-assembly output
enters the store as slots (container-local); per-slot text geometries
(extend the existing chrome/content geo pattern — same `update-text-data`
machinery scoped per slot, NEVER a second text path); face pick +
block-edit wiring re-point at store pick; TWO simultaneous reader-face
instances as the proof of plurality. Legacy paths untouched. Gates G7–G8.

**P4 — the pick/context seam + actions router.** Unified pick; context
bundle EDN; cmd-bar affordance attaches the bundle to an agent turn
(first form of "agent as verifier"); trail-face's `case` migrates to the
descriptor router. Gates G9–G10.

**P5+ — staged extensions (named, not scheduled):** editor-as-container ·
sidebar/chrome on the screen camera (kills the counter-bake tax) ·
chat/flow one-tree fix riding their migration (Δ5) · conditional RAF at
Δ15's trigger · scene-diff wearing · islands compositing (behind Sid's
Box3D answer).

## 7. Traps ledger (cite by number in code comments)

- **T1** Actions as closures in slot data. → Non-serializable scenes,
  agent-illegible, replay-hostile. → Descriptors only; G2 enforces by
  walking the value for `fn?`.
- **T2** Reorder diffs as `:permutation` ops. → PROBE-10K: permutation
  shapes glitch; order is data. → Reorders travel as `:change` on rank
  fields.
- **T3** Two `m/latest` chains derived from the store, combined
  downstream. → Diamond glitch (L8) emits torn frames deterministically.
  → Co-varying values derive in ONE `m/latest` over the store watch.
- **T4** Side effects (uploads, logs) inside `m/latest`. → R3 violation;
  re-runs on every sample. → Uploads happen at the reduce/consumer edge.
- **T5** Flatten recomputed per frame or per unrelated change. → The
  monolith reborn; identical?-skip dies. → `:ops` computed at upsert;
  unchanged slots MUST be `identical?` frame-over-frame (G3).
- **T6** Migrating stride without a safe default. → Every legacy instance
  breaks at once. → `container_idx` defaults to 0 = identity world
  container; pixel-identical back-compat is gated (G5).
- **T7** Chrome keeps counter-baking scroll into world coords. → The
  per-scroll re-layout tax survives the refactor that exists to kill it.
  → Screen-fixed content rides the screen-camera flag (P5 for legacy
  chrome; new slots from birth).
- **T8** Hit-testing in screen space against transformed containers. →
  Click space diverges from render space (the R-1 bug class). → Pick
  inverse-transforms the point into container-local BEFORE `hit-test`;
  one tree serves render and hit-test (Δ5).
- **T9** Per-slot subscriptions via `m/ap` forks over the store watch. →
  L1 crash class. → One watch; filtering via `m/eduction` + deref (R2).
- **T10** Committing an event per mousemove. → Log flood; gesture ≠
  assertion. → In-flight transforms are client-side at 60Hz; only SETTLE
  commits (assembly arrangement event, P3+).
- **T11** Uploading absolute world f32 after migration. → Δ2 violation;
  jitter at zoom/distance. → Producers emit container-local; the
  container offset is the anchor.
- **T12** A second text pipeline for per-slot text. → W3 violation; two
  shaping paths drift. → Per-slot geos go through the SAME
  `update-text-data` machinery (chrome/content precedent), scoped per
  slot; backend swap (msdf/slug) keeps working.

## 8. Acceptance gates (numbered; executable unless marked wearing)

- **G1 (P1)** Δ1 falsifier, verbatim from the ruled birth law: ONE address
  in TWO view-instances with independent geometry; an address-level write
  patches both slots via the index; pick on each copy returns (same
  address, different appearance). JVM test.
- **G2 (P1)** EDN round-trip: a store value with action descriptors
  round-trips `pr-str`/`read-string` identically; a recursive walk finds
  zero `fn?` anywhere in slots. JVM test.
- **G3 (P1)** Identity preservation: upserting slot B leaves slot A's
  `:ops` `identical?`; re-upserting an equal tree for A is allowed to
  replace, but an UNCHANGED store never reallocates ops. JVM test.
- **G4 (P2)** Live probe: ≥16 containers, ≥10k glyphs total; drag+zoom ONE
  container at 60fps for 60s — `[RAF]` p95 ≤ 8ms; op arrays `identical?`
  across the gesture (zero CPU re-layout — counter assertion); untouched
  containers' buffers get zero writes (writer counters). Read plan for the
  promise: gesture → container transform write (16 B) → redraw; store and
  text geos untouched.
- **G5 (P2)** Back-compat: the existing app (editor + sidebar + chrome +
  a worn face) renders pixel-identical with container 0 (screenshot
  compare via the framework evidence harness; op-count parity asserted
  FIRST per skill Amendment D).
- **G6 (P2)** The dormant zoom wakes: world camera zoom driven through
  [0.5, 2] on the probe; MSDF and slug both render (zoom-aware dilation
  paths already exist); no NaN/clip artifacts at the range ends.
- **G7 (P3)** Plurality live: TWO `[:vi :reader-face n]` instances of the
  SAME conversation in frame, independent transforms; a block edit in one
  echoes into BOTH (fan-out index proof); face + missionary + block-edit
  suites green.
- **G8 (P3)** Isolation: a keystroke in face A reshapes only face A's text
  geo (per-slot counter); the legacy monolith path sees zero text-data
  updates from face edits.
- **G9 (P4)** Pick + bundle: pick returns (same address, different
  appearance) across the two instances; the context bundle round-trips EDN
  and every address in it resolves through existing read APIs (foreign-
  read-style gate).
- **G10 (P4)** Actions: trail-face actions dispatch through the router; a
  scene tree EDN round-trips and replays its action descriptors
  identically (Δ6 falsifier).
- **G11 (wearing — before daily use, skill layer 5)** Sid drives: spawn
  two faces, drag/zoom each independently, edit a block in one, see the
  echo in both, pick a node and read its bundle. Verdict in NOW.md.

## 9. Stop clauses (beyond the standard ones)

- Per-slot text geos cannot preserve the msdf/slug backend swap without a
  second text path (T12 wall) → STOP, escalate.
- Container-0 back-compat cannot hold pixel-identical (stride ripple) →
  STOP — the seam claim is wrong, re-derive before patching.
- Any producer genuinely NEEDS absolute coords (local-frame assumption
  breaks) → STOP — that's a wall; the design is wrong somewhere
  (CLAUDE.md binding rule), re-derive.
- Two binding docs conflict → escalate per skill; never improvise.

## 10. Input manifest

`rect_tree.cljc` (rt-node grammar, resolve-layout, hit-test) ·
`face_assembly.cljc` + `face_primitives.cljc` (apply-assembly output) ·
`runtime.cljs:28-67` (source atoms) · `runtime/render.cljs` (frame loop,
geos, pools, dirty logic) · `substrate/webgpu/renderer.cljs` (four
pipelines, Camera struct lines 7/131/234/283, `update-text-data` 1348,
`draw-frame!` 1507+) · `runtime/mouse.cljs` (click paths 142-449) ·
`block_edit_wiring.cljs` + `block_edit.cljc` (the proven write loop) ·
`combined_text.cljs` (flatten + offset sites) · `build/render-north/
DELTA-B1.md` + `NORTH.md` + `MECHANICS.md` · `build/spatial/ROAD.md` ·
`DERIVATION.md` · CLAUDE.md Missionary laws + `.claude/skills/
electric-docs/SKILL.md` · `memory/implementation-quirks.md`.

## 11. Handoff

P1 → fresh-context implementer (Opus 4.8) under this contract; gates run
green in the same context. P2 → Fable direct (risk center). P3/P4 →
fresh contexts under contract after P1+P2 land. One batched falsification
finder + one gate review per wave (machine-cut retro rule: ONE finder on
new machinery). After G11: close + retro per the work-package skill.
Uncommitted-tree coordination: block-write + machine-cut edits sit
uncommitted in `runtime/*`, `electric_flow.cljc`, `face_projection.clj` —
P2/P3 diffs must stay additive next to them; the machine-cut close
session coordinates commits (board ⚠ T11 flag stands).
