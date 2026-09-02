# The same cleanup on image, region and text

*the second contract in `docs/seam-cuts/` · cut 2026-09-02 · cutter Fable 5.1, effort high · one shared step, then three builds in parallel, one close*

*Vocabulary: per `docs/decisions.md` "The render boundary" glossary (swept 2026-09-02, words only). Code identifiers, namespaces and file names keep their names until the step that touches each file; a doc names the identifier in backticks and the concept in this vocabulary.*

**Ground.** Source baseline `8dad07c`. The path cleanup is closed and accepted (`9caa2ab`; `docs/seam-cuts/NOW.md`). What it removed from path is the template: `docs/seam-cuts/CONTRACT.md` §1 lists the removals, §5 the shapes agreed across the row boundary. This page was cut from code read read-only on 2026-09-02: the twelve-file fact base (`FACTS-2-4.md`) and four bounded read-only sweeps (render test harness, image, region, text); every claim below carries its line at `8dad07c`. Read code, not docs.

**Sid's rulings, verbatim (2026-09-02).**
- "i don't want to keeping coming back to claude and do things linearly iffffff they are not needed"
- "i don't like these terms at all" (atom, seam cut, waist). This page says step, cleanup, engine, row boundary.
- The dead criterion and the order of work, as carried in CONTRACT.md's header: "Things that should be dead are not explicitly... that are not being called from anywhere, but like that exists for some future valid case and are implemented in the form that they should be" · "first make the existing code how it should be and only then fold the other things in … we are going to remove now".
- "we don't have a driver apart from the test code its fine that is how it should be as of now" — the standing word under which these steps close dark (§3, corner 6).
- "yes move them please" — the word that landed this cut's starter.

**Sid's two touches:** this page (open) · his word on the one close evidence for image, region and text (accept). The shared step needs no touch: it is behavior-identical and the render test harness's byte-identical run is its evidence; its NOW lines are the signal to paste the three kind starters. Nothing else stops for Sid.

---

## 0. The order of work, falsified against code

The prior (Fable, in chat, 2026-09-02): one shared step, then image and region in parallel, text small and later. What the code changed:

1. **Text runs in the parallel wave, not after it.** After the render test harness split every kind edits only its own files (§4). Text's cleanup touches `text/*` and `verifier/text.cljs` and nothing the others touch. Its dirty check is a non-goal here, not deferred: the layout key (`text/layout.cljc:317`) and the layout / paint / GPU-geometry partition (`layout-cache-acquire`, `layout-cache-report`, `:1119-1179`) that `docs/shaping-correction/CONTRACT.md` §6 and §7.3 contracted are in the tree, tested by two namespaces, and called by nothing in `src/` since their consumers left at the waist cut. Wiring them into the entry point is that package's next act, under its own ownership of `layout.cljc` and `painter.cljs`.
2. **The CPU color law has one home because oracles and diagnostics must reproduce the shader, not because renderers should convert on the CPU.** Region is a lit world in a linear working space: its five uniform and instance colors (`region3d/painter.cljs:502-508`, sites `:551 :552 :582 :621 :1065`) and the on-plane ink's per-vertex color (`on_plane.cljc:119-131`; one caller, `on_plane_painter.cljs:200-202`, landing at `@location(5)` untouched) are linearized unconditionally, mode or no mode, and the compositor encodes at present (`engine/compositor.cljs:38`). Neither region shader converts: zero `srgb` and zero `device/` in both renderers. So region's two copies stay CPU-side and call one function; the JVM oracle (`test/app/client/region3d/oracle.cljc:31-33`) and the render test harness's diagnostic (`verifier/core.cljs:759-766`) are the other two consumers. `engine/color.cljc` holds resource maps and no conversion today (`:1-48`); it gains the pair. Image's copy is WGSL (`image/painter.cljs:84-87`) and folds into `device/scene-color-wgsl` with one exact call shape (§4b).
3. **The split needs a sixth file.** `core.cljs` requires the kinds for dispatch; the kinds need the shared helpers; a kind requiring core is a cycle. The render test harness's shared helpers (`:33-107`; `zoom-cases :39`; the q8 transport `:176-206`; `:1537-1576`; `w4-read-texture! :1646`; and five pixel helpers the image and text layers lend to path and region: `boundary-pixels :207`, `byte-delta :703`, `pixel-rgba :711`, `srgb->linear :759`, `linear->srgb-byte :764`) becomes `verifier/shared.cljs`; core keeps dispatch and `start!`. The text section's GPU setup lives inside `run-verifier!` (`:2789-2818`) and is lifted into `run-text-slug!`; `load-t1-provider` (`:1577`) has zero callers and goes.
4. **Image's drawn thing has no revision and its revisioned thing is never drawn.** The draw item the renderer takes is `{:id :x :y :w :h :image/digest :image/uv :image/opacity :image/tint :container-idx}` (`verifier/core.cljs:513-518`); the component row with `:image/revision` reaches only `material-cache-key` and a test fixture (`image/material.cljc:103,145`; `material_test.clj:22`). A dirty check on the row's revision needs the row to be what is drawn: the row absorbs rect, crop and paint, and the draw item becomes `{:image/material row :container cid}` (§5, F1).

What held: the buffer index resolver moves into the engine (path holds the only `slot`; `on_plane.cljc:153` already resolves `(:container region-op)` through an world-transform map, the right form next entry point); the manifest is completed, as a glob rather than a longer hand list (it drifted by four files); image and region build in parallel; the shared step goes first because it is what makes the rest parallel.

**The shape of the work.** One session for the shared step, behavior-identical, closing on a byte-identical render test harness run. Then three sessions at once, one per kind, each owning its files. One NOW, one accept. Sid pastes twice: the shared starter, then the three kind starters when the shared NOW lines are committed.

---

## 1. Scope

Each kind is made the right form at its row boundary with no change to what it draws, by the criterion that governed path: dead means wrong form, called or not; right form for a valid future case stays, called or not. After this package a row of any kind is a validated EDN map with an id and a revision; a draw item names its group by id; every renderer dirty-checks on revisions, resolves buffer indices through the transform hierarchy, and converts color through one law; every schema is data checked by one engine; no renderer holds a ledger; no diagnostic, fixture or process-global lives in `src/` for these kinds.

### 1a. The shared step (behavior-identical)

| piece | verdict | why |
|---|---|---|
| `slot` in `path/frame.cljc:16-22` | move to `engine/placement.cljc` | three kinds need it; a GPU buffer index is resolved by the tree, once |
| `placement.cljc:29` private `finite-number?` | replace with `grammar/finite-number?` | routed by CONTRACT.md §1 |
| `validate-affine` (`placement.cljc:33-39`), `spec->affine`'s open destructure (`:41-57`), `add-container`'s three throws (`:82-104`) | replace in one move | the one placement schema across kinds (routed here by CONTRACT.md §2): a group spec is declared data checked by the engine; the registry's rejections are named. Two callers in the tree (`verifier/core.cljs:1482`, `frame_test.clj:34`), both pass an affine. |
| `engine/color.cljc`: no conversion function | add `srgb-channel->linear`, `linear->srgb-channel` | the CPU half of the one color law; five consumers (§0.2) |
| `engine/color.cljc`: no color schema | add `tagged` | the one color schema across kinds, routed here by CONTRACT.md §2; it is region's tagged shape |
| `engine/grammar.cljc`: no rule for a map keyed by ids; no data on a form rejection | add `[:map-of key-pred spec]` and `:explain` | region's scene is `object-id → object`; its cycle rejection must name the object |
| `verifier/core.cljs`: 2926 lines, four sections, one file | split by kind: `shared`, `image`, `path`, `region`, `text`, `core` | one file, three sessions is the corpse in memory `feedback-parallel-sessions-shared-branch-git`, twice |
| `verifier/core.cljs:1577` `load-t1-provider` | remove | zero callers |
| `run_verifier.mjs:275-295` hand-listed `sourceInputs` | replace with a glob | a hand list of what feeds the build is wrong form; it is four files behind today |

### 1b. Image

Removed now, called or not:

| piece | verdict | why |
|---|---|---|
| `:image/ingress-receipt` on the source row, the `ingress-receipt` constant, the equality check (`material.cljc:19-27, 44, 64-68`) | remove | the row carries a copy of the reader's own constant: equal or rejected, it says nothing. The decode chain is the engine's. No test asserts the rejection (`material_test.clj:53-60`). |
| the stats ledger: `:!receipt`, `receipt-row-counts`, `publish-image-receipt!`, `record-image-receipt!`, the `__softland_image_ingress_receipt` global (`painter.cljs:283-306, 448-450, 562, 580, 617-637, 665-677, 705-708`); `image-ingress-receipt` in the render test harness | replace | the global has zero readers; the ledger's readers are six render test harness sites. One renderer read is real: the draw item section consults it so a rejected digest keeps its status when its placeholder paints (`:665-677`). Status becomes a value in the residency map; rejections and rebuilds become return values (§4b). |
| `:container-idx` on the draw item (`verifier:518` → `painter:141` → `material.cljc:336`) | replace | a literal 0 copied into word 13, never through the tree. The draw item names `:container`; vertex packing resolves through `placement/slot`. |
| the prepare-key dirty check `{:images images :resources @!resources}` under `=` (`painter.cljs:695-698`); `:!last-prepare-key`; `:!last-images` (reset, never dereferenced) | replace | a deep compare of every draw item and every resident texture, every frame; nothing on the drawn thing carries a revision. One key: `[material-id revision container]` per draw item plus the residency revision. |
| `:!shape-rev`, `:frame-input/identity` (`painter.cljs:445`) | remove | seeded, never read; cargo from other kinds |
| `create-linear-image-variant` (`:736-772`) | remove | zero callers; a second private pipeline where the shared law belongs |
| `image-color-mode-declaration`, `configure-image-color-shader`, the private `srgb_channel_to_linear` (`:21-28, 84-87`) | replace | copies of `device/scene-color-wgsl` and `configure-scene-color-shader`; the blend is already shared (`:188`) |
| `validate-source!`, `validate-material!`, `material-required-keys`, `material-optional-keys`, `legal-*` as throw guards (`material.cljc:16-17, 38-69, 100-135`) | replace in one move | twelve checks, every one expressible as declared schema; the source schema becomes fail-closed (a widening; every in-tree source row carries exactly the checked keys, `verifier:490-500`) |
| `:image/extensions` and the `:image/time` rejection (`:104, 122`) | remove | an open bag inside a closed schema; the closed schema rejects `:image/time` as unknown |
| the draw item's `:x :y :w :h :image/uv :image/tint :image/opacity :image/digest :id` beside a component row the renderer never sees | replace | the row becomes the drawn thing (`:image/rect`, `:image/crop`, `:image/paint`); the draw item is `{:image/material row :container cid}` |

Kept, right form: `sha256-digest?`, `register-verified-source` (the digest proof), `empty-source-registry`, `resolve-source`, `source-cache-key`, `material-cache-key`, `normalize-crop`, `classify-quad`, `half-open-hit?`, `clip-placement`, `crop->uv`, `mip-level-count`, `mip-sizes`, `texture-bytes`, `atlas-config`, `placement-tier`, `empty-atlas`, `atlas-place`, `placement-plan`, `instance-words` (takes the resolved buffer index; fat, not wrong), `contiguous-binding-runs`, the `-srgb` texture view (`painter:265-281`), `:!source-bytes`, `:!source-registry`, `:!atlas`, `:!resources`, the mip system, the device-loss rebuild as a returning function.

### 1c. Region

| piece | verdict | why |
|---|---|---|
| `:container-idx` on the draw item (`painter.cljs:972`) into the composite row (`:739, :745`) | replace | a buffer index memcpy'd into the composite buffer. The right form is next entry point: `on_plane.cljc:153` resolves `(:container region-op)` through the world-transform map. The draw item names `:container`; `upload-composites!` resolves through `placement/slot`. |
| the per-region deep `=` dirty check (`painter.cljs:976-987`) | replace | `row` holds the whole derived scene: `:maintained` with `:instances`, `:triangles-by-object`, `:bvh` (`scene.cljc:641-653`), plus `:op`, `:gpu`, `:placements`, `:shadow-space`. `=` walks the geometry per region per frame. One key per region: `[id revision container]` plus `[zoom dpr session-revision]`. |
| no revision on the region row (`:region/revision` appears nowhere) | add `:region/revision` | the dirty check needs it; the writer writes it; fixtures write the content hash |
| `:!last-composite-key`, `:!last-regions` (`:454-455`) | remove | never read, never written |
| region's `:!shape-rev` (`:449`); `shape-rev`, `system-token` (`:780-786`, used `:883`) | remove | region's own is never incremented; the reader tokens the path system's `:!shape-rev`, a field the path cleanup deleted |
| `:!receipt` in the renderer (`:456, :999, :1146, :1157`), `region3d-receipt` (`:1202`), on-plane's `:!receipt` (`on_plane_painter.cljs:122, 286-294, 318`), `placement-receipt` (`:321`) | replace | ledgers read only by the render test harness (`core.cljs:1828, 1987, 1998, 2483-2485, 2525`); the per-frame return of `prepare-region3d-frame!` carries that frame's counts; the render test harness accumulates |
| `validate-tagged-color!`, `validate-parent-graph!`, `validate-region!`, the fourteen `canonical-*` that check and rewrite, `legal-*` as throw guards (`material.cljc:16-20, 99-118, 120-454`) | replace in one move | a declared schema checked by the engine after one pure canonicalization that fills defaults and normalizes (§4c, F6) |
| `validate-region!` run twice per full derive (`scene.cljc:624` and inside `compose-hierarchy`, `:133`) | replace | validation runs once at the entry point; derivations take a validated row |
| the open extension (`material.cljc:2-4` "Unknown fields are kept"; `material_test.clj:81-83` pins `:future/shape :preserved`) | close | the row boundary's structural tier: "a schema that is data — no unknown key rides" (`docs/decisions.md`, the render boundary). The test flips to a named rejection. F2. |
| region's `finite-number?` (`:80`; ten internal uses, zero external) | remove | `grammar/finite-number?`, identical |
| `legal-display-modes` (`:20`, zero uses); `legal-camera-kinds` (`:19`, error data only) | remove | dead; the lens `case` discriminates |
| `tagged-linear` (`painter.cljs:502-508`), `srgb-channel->linear` (`on_plane.cljc:119-123`) | replace | `color/srgb-channel->linear`, the one home; byte-identical constants |

Kept, right form: `scene.cljc`'s returned `:receipt` (`:affected-object-ids` is the transform-update payload read at `:888`; the five counters are what a proportionality consistency check reads: a returned value, not a ledger), `compose-hierarchy` and the affected-id maintenance, the BVH, `pick-region`, the shadow space, `adapt-legacy-color` (`on_plane.cljc:21`, the one legal kind→kind edge, until path's paint is tagged: F3), `linear-premultiplied` (as a caller of the shared function), `project-region-anchor` (`on_plane.cljc:147-178`; one test caller, right form, the connector approach's valid case), the binding owner, the lease and composite machinery, `canonical-*` as pure fill and normalize.

### 1d. Text

| piece | verdict | why |
|---|---|---|
| `:container-idx` read on the style (`glyph_pack.cljs:138-140`) and the text map (`painter.cljs:605`); no writer anywhere in `text/*` | replace | the draw item template carries `:container`; `pack-op!` resolves through `placement/slot`; `update-text-data` takes the world-transform map. Fail closed: a draw item without `:container` is rejected (F8). |
| `:!shape-rev` (`painter.cljs:361, 437, 755-757`), `:frame-input/identity` (`:360, :436`) | remove | written, never read |
| `:span-receipt` on the positioned draw item (`:551`) | remove | no reader |
| `:input-hash` under `:receipts` (`layout_oracle.cljc:578`; `layout.cljc:303, 1082`) | remove | no reader (`:source-lines` under the same key is read at `layout.cljc:1617` and stays) |
| `!text-layout-fallbacks` global atom and `text-layout-fallback-report` (`painter.cljs:490-496, 517`) | replace | a process-global diagnostic in `src/`, read by the render test harness's F3 consistency check (`core.cljs:2672-2693`); the fallback count is a fact of the call, returned under `:fallbacks` |
| the zoom layer twice (`layout.cljc:1046-1050`, `layout_oracle.cljc:535-539`; the same two numbers, the same throw) | replace | one `legal-zoom?` in `layout.cljc`; the oracle calls it |

Kept: `text-clip-runs`, `contiguous-state-runs` (`painter.cljs:764-793`; uncalled; partition by `[:clip :container]`, right form once `:container` is an id), `layout-key` and the layout cache (shaping-correction's, untouched), `:layout/id`, everything the shared color section already does (`painter.cljs:89, 311, 340`).

Not text's: its dirty check (§2, first non-goal).

---

## 2. Non-goals (each routed, none a void)

- **Does not build text's dirty check.** `update-text-data` repacks and writes on every call (`painter.cljs:734-758`; two render test harness callers, `:411, :459`). The dirty check exists: `tl/layout-key` and `layout-cache-acquire` / `layout-cache-report` (`layout.cljc:317, 1119-1179`), contracted by shaping-correction §6 and §7.3, called by two test namespaces and nothing in `src/`. Wiring it into the entry point is **shaping-correction's** next act after Sid's acceptance of its STEP 2. The question it must answer first: is text's per-draw item key `layout-key`'s `[source provider metrics]`, or the row boundary's `[id revision container]`? They have different owners.
- **Does not draw the layout row boundary.** The render boundary section says layout passes are rows (caret, selection, hit and on-plane read them). Which of layout's outputs are rows and which are engine-internal is its own step, cut after shaping-correction's residency step. **LATER.**
- **Does not move fill to the GPU** (step B). Path's paint onto `color/tagged`, the LOD table, and the zoom range to the server's space facet (`legal-zoom?` is text's copy of that envelope) ride B.
- **Does not build the runtime, the geometry generators, the uncommitted store, the pointer, the wire, the clock, or culling** (the geometry-generators-as-data step A and the missing engine, in the order a canvas needs them).
- **Does not touch the retention coverage check global** (`__softlandLayoutRetention`, `text/layout_planes.cljc:770-772`, read by `test/render_engine/memory_receipt.mjs:191-300`). An diagnostic in `src/` with a live reader; it becomes a return when the memory receipt is rebuilt. **LATER.**
- **Does not change region's scene evaluation** beyond validating once and gating by key: `compose-hierarchy`, the affected-id maintenance, the BVH, shadows, leases and the compositor are untouched.
- **Does not change the image atlas, mip or residency policy.** Engine-internal; the residency revision is its only new surface.
- **Does not give `project-region-anchor` a production caller.** The connector approach is its case; alive as is.
- **Does not decide where placed text and ink inside a region live.** `:region3d/resolved-placements` rides on the region draw item as today, derived from the session snapshot the key stamps; the layout row boundary step decides.
- **Does not unify the client schema engine with the server's `facet-engine`.** LATER, by a dependency-direction ruling (CONTRACT.md F1).
- **Does not share the three vector-math kits.** LATER.
- **Does not run a felt pass.** Sid's standing word in the header; corner 6 is named in §3 under it.
- **Does not amend `docs/decisions.md`.** The position is landed; the vocabulary glossary lands beside it in a parallel session and sweeps this page's terms.

---

## 3. Laws (pointers, never restated)

- The oath on everything below the row boundary: deterministic and versioned, same input → byte-identical output. Fails under S0, I4, R4, T3 if any golden moves.
- Source structure: kind → engine, never engine → kind, never kind → kind except `region3d/on-plane` (CLAUDE.md). `verifier/*` is the driver, not a kind: it may require any kind, and `verifier/region.cljs` requires `verifier/path.cljs` for its fixture builders (`path-op`, `path-ink-material`, `path-polygon-material`; today `core.cljs:1770, 2434-2447`). Fails if any `image/*`, `text/*`, or `region3d/*` file other than on-plane requires another kind.
- The render boundary (`docs/decisions.md`, "The render boundary"): no GPU buffer index on a draw item (I1, R1, T1) · no process-local stamp on a component (I2) · the dirty check is the row's revision (I3, R3) · the schema of a kind is declared data checked by one engine (I2, R2) · the color lives once (I4, R4) · recompute proportional to change (R3's counts) · revision stamps at every joint where async mixing is inherent (image's residency revision, region's session revision).
- "Dead means wrong form for the waist, called or not" (`docs/decisions.md`, Sid's criterion verbatim under it). Every row of §1 cites its form, never its call count.
- Rulings quote Sid verbatim (CLAUDE.md). The header carries them.
- Work-package law: contract, then execution straight through, two Sid touches, one close evidence (`.claude/skills/work-package/SKILL.md`). Corner 6 (never a third consecutive dark step) stands; these steps close dark under Sid's word quoted in the header, which he withdraws in one line.
- The parallel-sessions git discipline (memory `feedback-parallel-sessions-shared-branch-git`): three sessions in one tree on `main`, file-disjoint by §4's ownership; the two shared files every kind touches (`test/app/test_runner.clj`, `docs/seam-cuts/NOW-2-4.md`) take whole-line appends committed alone and at once, so a swept sibling line is complete and harmless. Exact paths staged; never `CLAUDE.md`, `CLAUDE-1.md`, or `.claude/memory/*`. Commits are few and legible: one per milestone, each big enough to read as one change, never a fix-by-fix tail (Sid, 2026-09-02: commits "too small to know anything").
- Token economy (CLAUDE.md): every starter lists boot bytes; code is read by boundary; only the artifact under repair is read whole.

---

## 4. Entry points (exact)

Ownership: a session edits only the files under its heading. The shared step precedes all three kinds and touches every shared file once.

### 4a. The shared step

**`src/app/client/engine/placement.cljc`**
- `slot [effective container] → int`, or throws `{:error-type :placement/unknown-container :container c}`; moved from `path/frame.cljc:16-22` verbatim but for the error type.
- `container`: the declared group spec, public data: `{:optional #{:affine :x :y :scale :scale-x :scale-y :rotation :parent :camera :layer :sibling-rank :effects} :validators {:affine six-finite? :x finite :y finite :scale finite :scale-x finite :scale-y finite :rotation finite :camera #{:world :screen} :layer integer? :sibling-rank integer?} :form-validators [{:valid? affine-xor-legacy? :error-type :placement/affine-form}]}`; `:parent` and `:effects` are `any?` (effects were validated by a namespace that left the tree; the docstring at `:106-108` that names it goes). `spec->affine` runs on a checked spec; `validate-affine` and `finite-number?` (`:29-39`) gone.
- `add-container` rejects by name: `:placement/reserved-cid`, `:placement/duplicate-container`, `:placement/parent-missing` (today's three throws at `:90-96`, as data). Requires `app.client.engine.grammar`.

**`src/app/client/engine/grammar.cljc`**
- `[:map-of key-pred spec-or-pred]`: the value must be a map (`:grammar/map-required`); every key passes `key-pred` (`:grammar/invalid-key`, path `(conj path k)`); every value is checked as a vector item is, `check-form!` for a spec map or `check-predicate!` for a predicate, at `(conj path k)`. About 15 lines beside `check-vector!`.
- a form validator accepts an optional `:explain (fn [form] map)`, called only on failure; its map merges into the rejection's `ex-data`. Two lines in `check-form!`.

**`src/app/client/engine/color.cljc`**
- `srgb-channel->linear [v]`, `linear->srgb-channel [v]`: the constants of `device.cljs:18-20` and `compositor.cljs:38` (0.04045, 12.92, 0.055, 1.055, 2.4; 0.0031308); doubles; reader-conditional `Math/pow`.
- `tagged`: `{:keys #{:rgba :color-space :alpha-association} :validators {:rgba grammar/valid-rgba? :color-space #{:srgb} :alpha-association #{:straight}}}`. Requires `app.client.engine.grammar`.

**`src/app/client/path/frame.cljc`**: `slot` gone; `frame-key` unchanged. **`src/app/client/path/painter.cljs:139`**: `(placement/slot effective (:container op))`. **`test/app/client/path/frame_test.clj:36`**: the buffer index law moves to `test/app/client/engine/placement_test.clj` with the new error type; path's S1 regression test in `verifier/path.cljs` expects `:placement/unknown-container`.

**The render test harness split** (`src/app/client/verifier/`), by the layers measured at `8dad07c`:
- `shared.cljs` (new, about 12KB): `:33-107` (`canvas-size`, `color-format`, `promise-mapv`, `bytes->hex`, `sha256-*`, `opaque-png-data-url`) · `zoom-cases :39` · `q8-effective`, `run-q8-transport!` `:176-206` · `:1537-1576` (`selected-limits`, `adapter-information`, `shader-digests`) · `w4-read-texture! :1646` · the promoted `boundary-pixels :207`, `byte-delta :703`, `pixel-rgba :711`; `srgb->linear :759` and `linear->srgb-byte :764` become one-line calls to `color/*`.
- `image.cljs` (about 30KB): `:479-1096`, entry `run-image-atom!`.
- `path.cljs` (about 19KB): `:1097-1536`, entry `run-path-atom!`.
- `region.cljs` (about 45KB): `:1681-2547`, entry `run-region3d-floor!`; requires `verifier.path` for `path-op`, `path-ink-material`, `path-polygon-material`.
- `text.cljs` (about 26KB): `:108-475` minus the q8 pair; `:1589-1645` (`t1-layout-receipt`; `load-t1-provider` dropped); `:2549-2747` (the flat section); plus `run-text-slug!` lifted verbatim from `run-verifier!` `:2789-2818` (both text systems, `decode-glyph-curves`, the harness, `run-case!` over the cases, `run-ubuntu-mixed-case!`). `image-record :389` is the slug section's record and moves here.
- `core.cljs` (about 11KB after): the `ns`; `run-verifier!` calling `text/run-text-slug!`, `image/run-image-atom!`, `path/run-path-atom!`, `region/run-region3d-floor!`, `text/t1-layout-receipt`, `text/run-text-flat-road!` and assembling the same result map with the same keys; `run-region3d-floor-verifier!`; `start!`. `shadow-cljs.edn` untouched: `app.client.verifier.core/start!` remains the entry.
- Every section's result map keeps the keys `run_verifier.mjs:155-226` reads; a kind session may add keys, never rename or drop one. This is what keeps `run_verifier.mjs` untouched after this step.

**`test/render_engine/run_verifier.mjs:275-295`**: `sourceInputs` = every `.cljs` and `.cljc` under `src/app/client/` plus every `.mjs` under `test/render_engine/`, sorted by path; the evidence records the count beside the hash.

**Tests:** new `test/app/client/engine/color_test.clj` (S0) and `test/app/client/engine/grammar_test.clj` (S0), both registered in `test/app/test_runner.clj` `pure-namespaces`.

### 4b. Image (after the shared step)

**`src/app/client/image/material.cljc`** (after: about 300 lines)
- `source`: the declared source schema (§6). `validate-source! [s]` = `(grammar/check source s)`.
- `grammar`: the declared row schema (§6). `validate-material! [m]` = `(grammar/check grammar m)`.
- `canonical-material`, `material-cache-key`: take a validated map, never re-check.
- `instance-words`: takes `{:rect :uv :tint :opacity :slot}`; the buffer index arrives resolved.
- Gone: `source-refusal-policy`, `ingress-receipt`, `material-required-keys`, `material-optional-keys`, `material-schema-version` (the schema is the schema), every `when-not … throw`.

**New: `src/app/client/image/frame.cljc`**: `frame-key [ops residency-rev] → [[[material-id revision container] …] residency-rev]`. About 12 lines, JVM-tested.

**`src/app/client/image/painter.cljs`** (after: about 560 lines)
- the fragment shader is `(str device/scene-color-wgsl image-fragment-main)`; `image-fragment-main` samples, computes `cg`, and returns exactly `let t = scene_color(vec4<f32>(tint.rgb, sampled.a * tint.a), cg); return vec4<f32>(sampled.rgb * t.rgb, t.a);`, the one call shape that reproduces both modes (`device.cljs:21-30` against `painter.cljs:96-105`: the sample is already linear through the `-srgb` view, so the law must see the tint alone). Configured through `device/configure-scene-color-shader`. `image-color-mode-declaration`, `configure-image-color-shader`, the private `srgb_channel_to_linear`, `create-linear-image-variant` gone.
- system map holds `:device :pipeline … :!atlas :!resources :!source-registry :!source-bytes :!prepared :!last-frame-key :!residency-rev`. Gone: `:!receipt`, `:!shape-rev`, `:frame-input/identity`, `:!last-images`, `:!last-prepare-key`; `:!prepared-images` renamed `:!prepared`.
- residency is a value: each entry of `:!resources` is `{:status :ok|:refused|:unavailable :reason kw-or-nil :binding … :uv …}`; every bind, replace, rejection, free and rebuild `swap!`s `:!residency-rev` `inc`.
- `register-image-source! [system source bytes] → promise of {:status :ok|:refused :digest d :reason r}`; the `.catch` at `:580` returns the rejection instead of `nil`.
- `rebuild-image-resources! [lost replacement] → promise of {:image-system replacement :rebuilt n :resources-fresh? bool}`; the two ledger loops and the `:device-loss` block gone.
- `prepare-image-frame! [system ops effective] → {:changed? bool :writes n :instances n}`; `:writes` is the pool's per-instance write count as `batch-update-pool!` returns it, one meter, never reshaped to a contract number (repair ruled 2026-09-02: the first build reported `:writes 1` beside a measured `:item-writes 2`): `key = (frame/frame-key ops @!residency-rev)`; equal to `@!last-frame-key` → `{:changed? false :writes 0 …}`; else resolve each draw item through the residency map and `(placement/slot effective (:container op))`, `batch-update-pool!`, store the key. A rejected or unavailable digest resolves to the placeholder binding with its status untouched.
- `draw-image-frame!`, `destroy-image-system!` unchanged in meaning.

**`src/app/client/verifier/image.cljs`**
- fixtures write source rows without `:image/ingress-receipt`; image rows carry `:image/rect`, `:image/crop` (today's `:image/uv` times the intrinsic size, exact), `:image/paint`, and `:image/revision` as the content hash.
- `image-op` → `{:image/material row :container cid}`.
- the section builds a registry, `placement/effective`, `device/write-containers!`, and passes `effective` to `prepare-image-frame!`.
- the three ledger-shaped lifecycle assertions (`:970-977`, `:1006-1013` at `8dad07c` numbering) read the rebuild's return; `run-color-receipts!` keeps its keys; `image-ingress-receipt` gone.
- I1's golden added, registered in the manifest's `imageAtomCases`.

**Tests:** `test/app/client/image/material_test.clj` rewritten to §7 (the `:image/ingress-receipt` fixture line goes); new `test/app/client/image/frame_test.clj` (I3's key law); `test_runner.clj` gains `app.client.image.frame-test`, one line, committed alone.

### 4c. Region (after the shared step)

**`src/app/client/region3d/material.cljc`** (after: about 380 lines)
- `canonical-region [r] → r'`: pure fill and normalize, never rejects: the v1→v2 migration (`:412-421`), the `default-*` merges, mesh `:params`, and quaternion normalization only when `|len − 1| ≤ 1e-3` (outside that, the quaternion passes through unnormalized and the schema rejects it).
- `grammar`: the declared region schema (§6). `validate-region! [r]` = `(grammar/check grammar (canonical-region r))`; returns the canonical map.
- `object`, `mesh`, `primitive`, `indexed-triangles`, `light`, `placed-text`, `placed-ink`, `placed-ref`, `transform`, `lens`, `view`, `extent`, `background`, `ambient`, `material-spec`: the specs as public data; every color is `color/tagged`.
- `acyclic?`, `parents-exist?`, `ids-match-keys?` with `:explain` naming the `:object/id` and `:cycle-at`; discriminated unions by the `geometry-matches-kind?` idiom (`path/material.cljc:70-76`).
- Gone: `validate-tagged-color!`, `validate-parent-graph!` as throwers, `finite-number?`, `legal-display-modes`, `legal-camera-kinds`, every `when-not … throw`, the "Unknown fields are kept" docstring line.

**New: `src/app/client/region3d/frame.cljc`**: `region-key [op zoom dpr session-revision] → [id revision container zoom dpr session-revision]`; `frame-key [ops zoom dpr session-revision]`. About 15 lines, JVM-tested.

**`src/app/client/region3d/painter.cljs`** (after: about 1120 lines)
- `tagged-linear` (`:502-508`) → `color/srgb-channel->linear` at its five sites.
- `prepare-region3d-frame! [system {:keys [regions]} session opts]`: `opts` gains `:effective`, required. Per draw item, `key = (frame/region-key op zoom dpr (:revision session-layout-snapshot))` (the snapshot's `:revision` is read at `on_plane.cljc:44` today); equal to the prepared row's key → the row is reused `identical?`; else the existing `scene/evaluate-scene` section runs from the prior `:maintained` (a full derive on a new region, affected-id maintenance on a changed one). Returns `{:regions {id {:changed? bool :full-rebuilds n :instance-uploads n :bvh-refits n :region-encodes n}} :composite-uploads n :held-passes n}`: that frame's counts, never accumulated.
- `upload-composites!` and `composite-row-bytes` take `:slot`, resolved as `(placement/slot effective (:container op))`.
- Gone: `:!last-composite-key`, `:!last-regions`, `:!shape-rev`, `shape-rev`, `system-token`, `:path-system` in the row, `:!receipt`, `region3d-receipt`, the `(if (= row old) old row)` pass.

**`src/app/client/region3d/scene.cljc`**: `compose-hierarchy` (`:133`) and `derive-scene` (`:624`) take a validated region and never re-validate. Nothing else moves.

**`src/app/client/region3d/on_plane.cljc`**: `srgb-channel->linear` gone; `linear-premultiplied` calls `color/srgb-channel->linear`. `adapt-legacy-color`, `project-region-anchor` unchanged.

**`src/app/client/region3d/on_plane_painter.cljs`**: `:!receipt`, `placement-receipt` gone; `prepare-placements!` (`:238`) already returns `:packs`, `:changed?`, `:placements`, `:census` and adds `:uploads`, `:ink-vertices`, `:over-limit` to that return instead of the swap at `:286-294`; `draw-placements!` (`:305`) returns its draw count instead of the swap at `:318`.

**`src/app/client/verifier/region.cljs`**
- fixtures write `:region/revision` as the content hash; `region3d-op` → `{:region/material row :container cid :region3d/resolved-placements …}` with the composite rect on the row as `:region/rect`.
- the section's registry, `effective` and `write-containers!` already exist for the surround path system (`:2431-2447` at `8dad07c`); the same `effective` goes to `prepare-region3d-frame!`.
- the S5 lifecycle and lower-resolution evidence (`:1983-2417`) accumulate the per-frame returns; `region3d-receipt` gone.
- R1's golden added: the first region golden on disk (`gpu-region3d-floor-tree.png`), under a new manifest key `region3dFloorCases` the runner compares like `pathAtomCases` (one bounded block in `run_verifier.mjs`, region's only touch of that file; F7).

**Tests:** `material_test.clj` rewritten to §7 (the `:future/shape` case flips to `:grammar/unknown-key`; the cycle case matches `:region/parent-cycle` and reads `:cycle-at` from `ex-data`); `oracle.cljc:31-33` calls `color/srgb-channel->linear`; new `test/app/client/region3d/frame_test.clj` (R3's key law); `scene_test.clj`, `on_plane_test.clj` pass unchanged but for the validated-row entry; `test_runner.clj` gains `app.client.region3d.frame-test`, one line, committed alone.

### 4d. Text (after the shared step; ownership in the starter)

**`src/app/client/text/layout.cljc`**: `line-paint-ops` (`:1245-1261`) carries `:container` from the template onto every draw item; `legal-zoom?` public (`:1046`), the throw unchanged; `:input-hash` gone from `:receipts` (`:303, :1082`).
**`src/app/client/text/layout_oracle.cljc`**: `:535-539` calls `layout/legal-zoom?`; `:578` `:input-hash` gone.
**`src/app/client/text/glyph_pack.cljs`**: `pack-op!` takes the buffer index from the draw item, `(placement/slot effective (:container op))`, resolved once per draw item before the glyph loop; the `:container-idx` read gone.
**`src/app/client/text/painter.cljs`**: `update-text-data` takes `:effective` in its opts and threads it through `pack-instances-flat` to `pack-op!`; `:605` reads `(:container txt)`; `:!shape-rev`, `:frame-input/identity`, `:span-receipt`, `!text-layout-fallbacks`, `text-layout-fallback-report` gone; `shape-text` and `pack-instances-flat` return `:fallbacks {…}` with today's keys (`:490-496`).
**`src/app/client/verifier/text.cljs`**: templates and hand-written draw items carry `:container 0` (`:456-457, 2662, 2668-2670, 2717` at `8dad07c`); both text sections build a registry and pass `effective`; the F3 consistency check reads `:fallbacks` from the return; T1 returns its tree evidence (the deterministic hash and the placement check). Its golden PNG and runner row are NOT text's: `run_verifier.mjs:206-210` hardcodes the golden list and `:302` asserts exactly one Ubuntu golden, so registering a ninth is a runner edit; the last closer registers it at package close, one bounded block beside F7's (ruling, Fable, 2026-09-02, on text's fork in `NOW-2-4.md`).
**Tests:** `test/app/client/text/flat_road_test.clj` updated for `:container` and the returned fallbacks; no new namespace.

---

## 5. The row boundary per kind (the only shapes agreed across it)

```clojure
;; ---- image ----
;; a source, as registered (EDN; the digest is proven by the byte reader)
{:image/digest "…64 hex…" :image/color-tag :srgb|:embedded-profile
 :image/width w :image/height h :image/bytes-route r
 :image/alpha-association :straight|:premultiplied|:opaque}
;; an image row, as it travels
{:image/material-id id :image/revision rev :image/source-digest d
 :image/color-tag tag :image/intrinsic-size [w h] :image/provenance p
 :image/rect {:x :y :w :h}                  ; container-local
 :image/crop {:x :y :w :h}                  ; optional; image pixels; clamped by normalize-crop
 :image/paint {:tint {:rgba [r g b a] :color-space :srgb :alpha-association :straight}
               :opacity o}}
;; an op the painter takes
{:image/material <validated row> :container <cid>}
;; the frame gate
[[[material-id revision container] …] residency-rev]
;; residency, floor-private, a value per digest
{:status :ok|:refused|:unavailable :reason kw-or-nil :binding … :uv […]}

;; ---- region ----
;; a region row, as it travels (v2 after canonical-region)
{:region/id id :region/revision rev :region3d/version 2
 :extent {…} :scene {object-id {:object/id object-id :object/kind k …}}
 :view {…} :background {…} :ambient {…}
 :region/rect {:x :y :w :h}}                ; container-local composite quad
;; an op the painter takes
{:region/material <validated row> :container <cid>
 :region3d/resolved-placements …}           ; session-derived, as today (§2)
;; the frame gate, per region
[id revision container zoom dpr session-revision]
;; the per-frame return
{:regions {id {:changed? b :full-rebuilds n :instance-uploads n :bvh-refits n :region-encodes n}}
 :composite-uploads n :held-passes n}

;; ---- text ----
;; an op template, as the caller hands it (line-paint-ops assocs the line onto it)
{:size s :r :g :b :a :container <cid>}
;; the pack's return
{:raw-buffer … :line-offsets … :num-instances n :fallbacks {…}}

;; ---- every kind ----
(placement/slot effective container)        ; int, or throws :placement/unknown-container
color/tagged                                ; {:rgba :color-space :alpha-association}
(color/srgb-channel->linear v)              ; the CPU half of device/scene-color-wgsl
```

Invariant the writers own and fixtures honor, unchanged from path: **a revision changes whenever the row's content changes.** Image's residency revision and region's session revision are the engine's stamps at the async joints; they are never on a row.

---

## 6. The schemas as data (skeletons the implementers complete)

```clojure
;; image/material.cljc
(def source
  {:keys #{:image/digest :image/color-tag :image/width :image/height
           :image/bytes-route :image/alpha-association}
   :validators {:image/digest sha256-digest? :image/color-tag legal-source-tags
                :image/width pos-int? :image/height pos-int? :image/bytes-route some?
                :image/alpha-association legal-alpha-associations}})
(def rect {:keys #{:x :y :w :h}
           :validators {:x grammar/finite-number? :y grammar/finite-number?
                        :w grammar/positive-number? :h grammar/positive-number?}})
(def paint {:keys #{:tint :opacity}
            :validators {:tint color/tagged
                         :opacity #(and (grammar/finite-number? %) (<= 0.0 % 1.0))}})
(def grammar
  {:keys #{:image/material-id :image/revision :image/source-digest :image/color-tag
           :image/intrinsic-size :image/provenance :image/rect :image/paint}
   :optional #{:image/crop}
   :validators {:image/material-id some? :image/revision some?
                :image/source-digest sha256-digest? :image/color-tag legal-source-tags
                :image/intrinsic-size #(and (vector? %) (= 2 (count %)) (every? pos-int? %))
                :image/provenance some? :image/rect rect :image/crop rect :image/paint paint}})

;; region3d/material.cljc (colors are color/tagged; every legal-* set is a membership rule)
(def transform {:keys #{:translation :rotation :scale}
                :validators {:translation vec3? :scale vec3? :rotation unit-quaternion?}})
(def lens      {:keys #{:kind …}
                :validators {:kind #{:perspective :orthographic}}
                :form-validators [{:valid? lens-matches-kind? :error-type :region/lens-kind}
                                  {:valid? near-before-far?  :error-type :region/lens-near-far}]})
(def primitive {:keys #{:kind :params} :validators {:kind legal-primitive-kinds}
                :form-validators [{:valid? params-match-kind? :error-type :region/primitive-kind}]})
(def indexed-triangles
  {:keys #{:positions :normals :indices}
   :form-validators [{:valid? normals-match-positions? :error-type :region/mesh-normals}
                     {:valid? indices-in-range?        :error-type :region/mesh-index}]})
(def light     {:keys #{:kind :color :intensity …}
                :validators {:kind legal-light-kinds :color color/tagged …}
                :form-validators [{:valid? light-matches-kind?     :error-type :region/light-kind}
                                  {:valid? spot-cone-ordered?      :error-type :region/light-cone}
                                  {:valid? shadow-only-directional? :error-type :region/light-shadow}]})
(def object    {:keys #{:object/id :object/kind :transform :provenance} :optional #{:parent …}
                :validators {:object/kind legal-object-kinds :transform transform …}
                :form-validators [{:valid? body-matches-kind? :error-type :region/object-kind}]})
(def grammar
  {:keys #{:region/id :region/revision :region3d/version :extent :scene :view
           :background :ambient :region/rect}
   :validators {:region3d/version #{2} :scene [:map-of some? object] :extent extent
                :view view :background background :ambient ambient :region/rect rect …}
   :form-validators
   [{:valid? ids-match-keys? :error-type :region/object-id-mismatch}
    {:valid? parents-exist?  :error-type :region/parent-missing :explain missing-parent}
    {:valid? acyclic?        :error-type :region/parent-cycle   :explain cycle-at}]})
```

Error types are named per rejection so a rejection is readable as data. Nothing beyond `[:map-of …]` and `:explain` is invented in the engine.

---

## 7. Decisive scenarios (frozen as regression tests at close)

**S0 — the shared step changes no byte.** `npm run verify:render-engine` PASS with every golden in `test/app/fixtures/render_engine/gpu-goldens/manifest.json` and every evidence equal to the path close's run (NOW.md: holed `645f9536…410b`, translucent `5bf612e2…ea79`, tree `ca96286b…53ba`; all 33 PNGs by manifest); `shaderDigests` unchanged; the evidence's `sourceInputs` count is the glob's. JVM: the path suite green with `:placement/unknown-container`; `placement_test`: a five-number affine, a spec with `:tilt`, `:camera :orbit`, and a parent that does not exist are each rejected by name, and the registry the render test harness's path section builds (`core.cljs:1482`) composes to the same `effective` map; `color_test`: the four breakpoints (0, 0.04045, 0.0031308, 1) exact, `0.5 → 0.21404114048223255` within 1e-15, encode∘decode identity within 1e-12 at 0.0, 0.25, 0.5, 0.75, 1.0; `grammar_test`: `[:map-of keyword? spec]` rejects a string key as `:grammar/invalid-key` at `[:scene "x"]`, checks each value at its path, and a failing form validator's `:explain` map rides in `ex-data`. Wrong build that passes a weaker test: a split that leaves `run-text-slug!`'s systems inside `run-verifier!`; §10's ownership check catches it when the text session must edit core.

**I1 — the hierarchy runs.** Render test harness golden. A registry with group 17 under the root at `[0.5 0 0 0.5 40 20]`; one image row drawn with `:container 0` and again with `:container 17`; `effective` → `write-containers!` → `prepare-image-frame!`. The golden shows the second quad at half size offset by (40, 20). A draw item naming a group the hierarchy lacks throws `:placement/unknown-container` before any write. Wrong build that passes a weaker test: the buffer index taken as the cid; 17 gets buffer index 1.

**I2 — rows travel and the schema rejects by name.** JVM. `(edn/read-string (pr-str row))` of each fixture validates, has `nil` metadata, and gives the same `material-cache-key` and the same 13 instance words. Rejections, each by its named type: an unknown top-level key; `:image/extensions`; a source carrying `:image/ingress-receipt`; a source missing `:image/bytes-route`; a tint of five numbers; a rect with `:h 0`; an opacity of 1.5; a crop given as a vector. Wrong build that passes a weaker test: `validate-source!` left open-world; the extra-key rejection catches it.

**I3 — the dirty check is the revision plus residency.** JVM for the key, render test harness for the writes; `:writes` is the pool's per-instance count. Frame 1: two draw items, both resident → changed? true, writes 2. Frame 2: the same draw items → changed? false, writes 0. Frame 3: draw item one re-written with a new revision and the same content → changed? true; writes is whatever the pool measures for identical bytes, pinned at the repair with the reason. Frame 4: the same draw items; a texture lands for a digest that was a placeholder (the residency revision bumps) → changed? true, writes ≥ 1, and that instance's binding is the texture's. Wrong build that passes a weaker test: a key without the residency revision reports changed? false on frame 4 and draws the placeholder forever; a deep-compare dirty check reports changed? false on frame 3; a return that reshapes the pool's count to a contract number (the first build's `(if (pos? n) 1 0)`).

**I4 — the shared color law paints the same bytes.** Render test harness. The 21 image goldens (`imageAtomCases`) byte-identical in the default mode; `run-color-receipts!`'s linear-mode rows equal to today's within one 8-bit step per channel (the multiply association, `(s·t)·α` against `s·(t·α)`); anything larger is a wrong build. Wrong build that passes a weaker test: `scene_color(vec4(sampled.rgb * tint.rgb, …), cg)`, which linearizes an already-linear sample; the linear-mode rows diverge by far more than a step.

**I5 — device loss without a ledger.** Render test harness. `rebuild-image-resources!` returns `:rebuilt` equal to the registered source count and `:resources-fresh? true`; a digest rejected before the loss reports `:refused` with its reason after it; a placeholder painted for a rejected digest never relabels it `:unavailable`. Wrong build that passes a weaker test: the draw item section defaults every non-resident digest to `:unavailable`; the rejected-then-painted case catches it.

**R1 — the hierarchy runs.** Render test harness golden, the first region golden on disk. The `region3d-seam-fixture` region drawn with `:container 17` under `[0.5 0 0 0.5 40 20]`: its composite quad at half size offset by (40, 20), the surround paths unchanged. An unknown group rejected before upload. Wrong build that passes a weaker test: `composite-row-bytes` writes the cid; it passes while cid equals buffer index.

**R2 — rows travel and the schema rejects by name.** JVM. The `region3d-seam-fixture` region round-trips through EDN with `nil` metadata, the same canonical map, and the same `:instances` and BVH from `derive-scene`. Rejections by name: `:future/shape` at the top level (`:grammar/unknown-key`); a parent cycle (`:region/parent-cycle`, with `:cycle-at` in `ex-data`); a parent that does not exist; a spot light with inner > outer; a torus with tube ≥ radius; an index ≥ vertex count; a quaternion of length 1.01; `:cast-shadow true` on a point light; a placed text with an extra key; `:region3d/version 3`. Wrong build that passes a weaker test: canonicalization normalizing every quaternion, so the 1.01 case passes; the tolerance rule catches it.

**R3 — the dirty check is the revision, and work is proportional.** JVM for the key, render test harness for the counts. Frame 1: one region → changed? true, full-rebuilds 1. Frame 2: the same draw item, zoom and session → changed? false, every count 0, the prepared row `identical?`. Frame 3: the revision re-written with the same content → changed? true, full-rebuilds 0, instance-uploads 0. Frame 4: one object's transform moved under a new revision → instance-uploads 1, bvh-refits 1, full-rebuilds 0. Frame 5: zoom crosses a lease-size step → changed? true, region-encodes 1. Wrong build that passes a weaker test: the deep `=` kept behind the key reports changed? false on frame 3; a key without zoom reuses the old lease on frame 5.

**R4 — one color home paints the same bytes.** Render test harness and JVM. The region determinism hashes and the lit evidence unchanged with `tagged-linear` and on-plane's decode replaced; `scene_test` s4 unchanged with the oracle calling `color/srgb-channel->linear`. Wrong build that passes a weaker test: `linear-premultiplied` routed through the shader's mode switch, which changes nothing in the default mode and the on-plane ink bytes in the linear one.

**R5 — ledgers gone, evidence from returns.** The S5 lifecycle and lower-resolution evidence pass, built from `prepare-region3d-frame!`'s returns; `grep -n "receipt" src/app/client/region3d/painter.cljs src/app/client/region3d/on_plane_painter.cljs` is empty.

**T1 — the hierarchy runs.** Render test harness evidence through the slug section: the mixed-face case with its template at `:container 17` under the affine; the glyphs at half size offset by (40, 20). Pinned now as the deterministic tree hash plus the placement check; pinned as a golden PNG at package close, when the last closer adds its runner row (§4d). An unknown group rejected before upload. Wrong build that passes a weaker test: `pack-op!` writing the cid into word 24.

**T2 — diagnostics are returns.** The F3 consistency check reads `:fallbacks` from `pack-instances-flat`'s return: carried draw items 0, uncarried 1 per draw item, as today (`core.cljs:2690-2693`). `grep -rn "__softland" src/app/client/text/painter.cljs src/app/client/text/glyph_pack.cljs` is empty.

**T3 — nothing else moved.** The 8 slug goldens and the flat section rows F1–F3 byte-identical; `layout_oracle` rejects zoom 0.001 with the same message through `layout/legal-zoom?`.

Representative goldens at close: I1, R1, T1 (new); the 21 image and 8 slug goldens unchanged.

---

## 8. MUST-NOTs (real only)

- Never read `src/app/server/env.clj`.
- No server, Rama, `/mnt/data/rama`, archive, or durable-data mutation (the standing fence).
- Never a Co-Authored-By line; exact paths staged; never `CLAUDE.md`, `CLAUDE-1.md`, or `.claude/memory/*`.
- A kind session never edits `verifier/core.cljs`, `verifier/shared.cljs`, `shadow-cljs.edn`, another kind's files, or `run_verifier.mjs` beyond F7's one block.

---

## 9. Forks written, with defaults

- **F1, where the image's rect lives.** Default: on the row (`:image/rect`, `:image/crop`, `:image/paint`); the draw item is component plus group. The alternative, a revision on today's draw item, is the same row under another name with the component row left unread.
- **F2, region's open extension.** Default: closed. The row boundary's structural tier says no unknown key rides; `material_test.clj:81-83` flips to a rejection. Sid reopens it in one line if he wants it open.
- **F3, the one color schema.** Default: `color/tagged` is region's tagged shape; image's `:tint` takes it now; path's paint (`{:color [rgba] :opacity :color-space :alpha-association}`) switches at step B, which reshapes the path vertex anyway. `adapt-legacy-color` stays until then.
- **F4, image's residency in the dirty check.** Default: a engine-internal counter in the key, bumped on every residency change. The alternative, the resource map in the key, is the deep compare again.
- **F5, region's view.** Default: the view (pivot, distance, yaw, pitch) is row content, so an orbit is a new revision and the affected-id path handles it. It is the region's own camera as data, not the canvas camera; the canvas camera never enters a region key.
- **F6, canonicalize then check.** Default: `canonical-region` fills and normalizes without rejecting; the schema rejects once. Quaternions: normalized only within 1e-3 of unit, then required unit within 1e-9.
- **F7, region's first golden.** Default: `region3dFloorCases` in the manifest and one bounded compare block in `run_verifier.mjs`, region's only touch of that file.
- **F8, a text draw item without `:container`.** Default: fail closed, like path's F2; the world is 0 and templates name it.
- **F9, the render test harness's shared helpers.** Default: `verifier/shared.cljs`; helpers in core would make the kinds require the entry.

---

## 10. Close

The shared step closes on S0 and writes three lines at the top of `docs/seam-cuts/NOW-2-4.md` (baseline, closing commit, the render test harness evidence line), then commits; that is the signal to paste the three kind starters. Each kind freezes its scenarios as regression tests, keeps its goldens, appends its close (≤4 lines under its own heading) to `NOW-2-4.md`, and commits that append alone. The last kind to close writes the terminal line (adapter first; changed files from `git diff --name-only`, never memory), then flips the board pointer in `docs/next-prompt.md`; after that no edit to any step's source, tests or tooling. Ownership check at every close: `git diff --name-only <baseline>..HEAD` shows only the files under that session's §4 heading plus the two shared one-line files. Foreign suite failures are debt (known: `clj -X:test` red on an unclassified tools test since `b86a5d2`). One accept, Sid's word.

---

## 11. The starters (paste-able, Codex lane)

### 11a. The shared step

```
Preflight, before the first prompt: permission mode, remote-control and MCP set now; never mid-session.

Boot, by seam, sizes first (about 85KB; core.cljs is the artifact under repair: skeleton, then the
windows named, then move bands by line range with a script, never by retyping):
  docs/seam-cuts/CONTRACT-2-4.md   header, §0, §1a, §3, §4a, §5 tail, §7 S0, §8, §9 F9, §10   read PRIMARY
  src/app/client/engine/placement.cljc                        11KB   whole
  src/app/client/engine/grammar.cljc                           5KB   whole
  src/app/client/engine/color.cljc                             2KB   whole
  src/app/client/engine/device.cljs                           12KB   lines 1-45
  src/app/client/path/frame.cljc                               1KB   whole
  src/app/client/path/painter.cljs                            10KB   lines 120-160
  test/app/client/path/frame_test.clj                          2KB   whole
  test/app/client/engine/placement_test.clj                    1KB   whole
  src/app/client/verifier/core.cljs                          144KB   `grep -n "^(def"` first; then 1-107,
                                                                     176-206, 700-770, 1537-1680, 2748-2926
  test/render_engine/run_verifier.mjs                         15KB   lines 30-60, 150-230, 270-300
  test/app/test_runner.clj                                    25KB   lines 31-52
  shadow-cljs.edn                                              1KB   whole; read, never edit
  docs/seam-cuts/NOW.md                                        2KB   the golden hashes S0 pins

You own the whole step: build, surface bugs, fix before source freeze. It changes no byte the verifier
sees except the error type :placement/unknown-container and the manifest's file list. Plan: OWNER you ·
SOURCE this contract §4a · DONE WHEN S0 is green and NOW-2-4.md's first three lines are committed.
Ambiguity takes §9's default plus a note; a genuine fork is one question in NOW-2-4.md, routed around
while unblocked work continues. Commits: four at most, one per milestone (engine · split · manifest · tests), each big enough to
read as one change, the message saying what changed and why; never a fix-by-fix tail (Sid,
2026-09-02: "too small to know anything"). On main, exact paths, no Co-Authored-By. Do not stage CLAUDE.md, CLAUDE-1.md, or anything under .claude/memory/.
```

### 11b. Image (paste once NOW-2-4.md's shared lines are committed; runs beside 11c and 11d)

```
Preflight, before the first prompt: permission mode, remote-control and MCP set now; never mid-session.

Boot, by seam, sizes first (about 110KB; painter.cljs whole is the artifact under repair; take the rest
by milestone, grammar → frame → painter → verifier road, never all at once):
  docs/seam-cuts/CONTRACT-2-4.md   header, §0, §1b, §2, §3, §4b, §5, §6 image, §7 I1-I5, §8, §9   read PRIMARY
  src/app/client/image/material.cljc                          15KB   whole
  src/app/client/image/painter.cljs                           37KB   whole
  src/app/client/verifier/image.cljs                          30KB   `grep -n "^(def"`; then the fixtures
                                                                     (image-fixtures, ingress-corpus!, image-op),
                                                                     run-lifecycle-receipt!, run-color-receipts!,
                                                                     run-image-atom!
  src/app/client/path/material.cljc                            9KB   lines 1-95 (the grammar shape)
  src/app/client/path/painter.cljs                            10KB   the prepare-path-frame! window (the gate shape)
  src/app/client/engine/grammar.cljc                           5KB   whole
  src/app/client/engine/color.cljc                             3KB   whole
  src/app/client/engine/device.cljs                           12KB   lines 1-45
  src/app/client/engine/placement.cljc                        11KB   the slot and effective windows
  test/app/client/image/material_test.clj                      8KB   whole

You own the whole piece: build, surface bugs, fix before source freeze. Sid's word: "we are going to
remove now"; §1b is the list; call count is never the criterion. Your files are §4b's and nothing else
(§8); the one-line add to test_runner.clj is committed alone, at once; your road's result keys stay.
Plan: OWNER you · SOURCE this contract · DONE WHEN I1-I5 are tripwires and your close lines are appended
to NOW-2-4.md under "image". Ambiguity takes §9's default plus a note; a genuine fork is one question in
NOW-2-4.md under your heading, routed around while unblocked work continues. Commits: few, one per milestone, each big enough to read as one change, the message saying what
changed and why; never a fix-by-fix tail (Sid, 2026-09-02: "too small to know anything"). On main,
exact paths, no Co-Authored-By. Do not stage CLAUDE.md, CLAUDE-1.md, or anything under
.claude/memory/.
```

### 11c. Region (paste once NOW-2-4.md's shared lines are committed; runs beside 11b and 11d)

```
Preflight, before the first prompt: permission mode, remote-control and MCP set now; never mid-session.

Three milestones in one session, booted one at a time (about 65KB, 45KB, 35KB); nothing is read whole
but material.cljc, on_plane.cljc and on_plane_painter.cljs:

  Milestone 1, the grammar (§1c, §4c material, §6 region, §7 R2):
  docs/seam-cuts/CONTRACT-2-4.md   header, §0, §1c, §2, §3, §4c, §5, §6 region, §7 R1-R5, §8, §9   read PRIMARY
  src/app/client/region3d/material.cljc                       20KB   whole
  src/app/client/engine/grammar.cljc                           5KB   whole
  src/app/client/engine/color.cljc                             3KB   whole
  src/app/client/path/material.cljc                            9KB   lines 1-95 (the grammar shape)
  test/app/client/region3d/material_test.clj                   6KB   whole

  Milestone 2, the gate and the returns (§4c painter, scene, on-plane; §7 R3, R5):
  src/app/client/region3d/on_plane.cljc                        8KB   whole
  src/app/client/region3d/on_plane_painter.cljs               14KB   whole
  src/app/client/region3d/painter.cljs                        60KB   `grep -n "^(def"`; then 430-520, 730-800,
                                                                     800-1050, 1130-1210
  src/app/client/region3d/scene.cljc                          39KB   `grep -n "^(def"`; then 125-160, 615-720,
                                                                     880-895
  src/app/client/engine/placement.cljc                        11KB   the slot and effective windows
  test/app/client/region3d/oracle.cljc                         7KB   lines 1-45
  test/app/client/region3d/scene_test.clj                     10KB   `grep -n "deftest"`

  Milestone 3, the road (§4c verifier; §7 R1, R4):
  src/app/client/verifier/region.cljs                         46KB   `grep -n "^(def"`; then the fixture builders
                                                                     (region3d-* through region3d-op), the entry,
                                                                     every site that read region3d-receipt
  test/render_engine/run_verifier.mjs                         15KB   the pathAtomCases compare block only (F7)

You own the whole piece: build, surface bugs, fix before source freeze. Sid's word: "we are going to
remove now"; §1c is the list; call count is never the criterion. Your files are §4c's and nothing else
(§8; F7 is your one block in run_verifier.mjs); the one-line add to test_runner.clj is committed alone,
at once; your road's result keys stay. Plan: OWNER you · SOURCE this contract · DONE WHEN R1-R5 are
tripwires and your close lines are appended to NOW-2-4.md under "region". Ambiguity takes §9's default
plus a note; a genuine fork is one question in NOW-2-4.md under your heading, routed around while
unblocked work continues. Commits: few, one per milestone, each big enough to read as one change, the message saying what
changed and why; never a fix-by-fix tail (Sid, 2026-09-02: "too small to know anything"). On main,
exact paths, no Co-Authored-By. Do not
stage CLAUDE.md, CLAUDE-1.md, or anything under .claude/memory/.
```

### 11d. Text (paste once NOW-2-4.md's shared lines are committed; runs beside 11b and 11c)

```
Preflight, before the first prompt: permission mode, remote-control and MCP set now; never mid-session.

Custody: text/layout.cljc and text/painter.cljs are shaping-correction's files. No shaping-correction
session runs while this one does; the board names the holder.

Boot, by seam, sizes first (about 80KB; layout.cljc by window, never whole):
  docs/seam-cuts/CONTRACT-2-4.md   header, §0, §1d, §2 first two non-goals, §3, §4d, §5, §7 T1-T3, §8, §9 F8   read PRIMARY
  src/app/client/text/painter.cljs                            41KB   `grep -n "^(def"`; then 355-365, 430-440,
                                                                     485-520, 540-610, 730-826
  src/app/client/text/glyph_pack.cljs                          8KB   lines 100-196
  src/app/client/text/layout.cljc                             78KB   lines 295-325, 1040-1065, 1240-1265, 1610-1620
  src/app/client/text/layout_oracle.cljc                      31KB   lines 530-585
  src/app/client/verifier/text.cljs                           30KB   whole
  src/app/client/engine/placement.cljc                        11KB   the slot and effective windows
  test/app/client/text/flat_road_test.clj                      7KB   whole

You own the whole piece: build, surface bugs, fix before source freeze. Sid's word: "we are going to
remove now"; §1d is the list; call count is never the criterion. The dirty check is not yours (§2, first
non-goal); leave layout-key and the layout cache untouched. Your files are §4d's and nothing else (§8);
your road's result keys stay. Plan: OWNER you · SOURCE this contract · DONE WHEN T1-T3 are tripwires and
your close lines are appended to NOW-2-4.md under "text". Ambiguity takes §9's default plus a note; a
genuine fork is one question in NOW-2-4.md under your heading, routed around while unblocked work
continues. Commits: few, one per milestone, each big enough to read as one change, the message saying what
changed and why; never a fix-by-fix tail (Sid, 2026-09-02: "too small to know anything"). On main,
exact paths, no Co-Authored-By. Do not stage CLAUDE.md,
CLAUDE-1.md, or anything under .claude/memory/.
```

### 11e. The package close (paste after image, region and text have all appended their close lines; one fresh Codex session)

```
Preflight, before the first prompt: permission mode, remote-control and MCP set now; never mid-session.

Boot, by seam, sizes first (about 60KB):
  docs/seam-cuts/CONTRACT-2-4.md   header, §4b (prepare-image-frame!), §4d (T1), §7 I3 + T1, §8, §10, §11e   read PRIMARY
  docs/seam-cuts/NOW-2-4.md                                    5KB   whole
  src/app/client/image/painter.cljs                           37KB   the prepare-image-frame! window only
  src/app/client/verifier/image.cljs                          30KB   the I3 window only (grep "frame-1")
  src/app/client/verifier/text.cljs                           30KB   the T1 window only (grep "tree")
  test/render_engine/run_verifier.mjs                         15KB   lines 150-320 (the golden lists, the slug
                                                                     compare, region's F7 block)
  test/app/fixtures/render_engine/gpu-goldens/manifest.json          whole
  test/app/test_runner.clj                                    25KB   lines 31-60

Five acts, in order, each its own commit:
1. Image repair (ruled 2026-09-02): `:writes` in prepare-image-frame! becomes the pool's per-instance
   count as batch-update-pool! returns it; `:item-writes` goes; I3's regression test pins the measured
   numbers per frame (frame 1 = 2, frame 2 = 0; frames 3 and 4 as measured, with the reason in the
   test). Then §7 I3 in the contract carries the pinned numbers.
2. T1's ninth Slug golden: record the PNG, add its manifest row, and add its runner row beside
   region's F7 block; the "exactly one Ubuntu golden" assertion learns the count. The banked hash
   6160b2a7…be30 must be the PNG's raw hash.
3. Full render test harness on the final tree: `npm run verify:render-engine` PASS, every golden and
   guard; then the full JVM suite `clj -X:test`; foreign red (the tools test since b86a5d2) is named as
   debt, never fixed here.
4. The terminal line of NOW-2-4.md: adapter first, then changed files from
   `git diff --name-only eb1e367..HEAD`, never memory; then the ownership check: every source file in
   that diff sits under one kind's §4 heading or §4a's.
5. Flip the board line in docs/next-prompt.md (seam cuts → this contract closed, Sid acceptance
   pending). After that, no edit to any step's source, tests or tooling.

Commits: five, one per act, each big enough to read as one change, the message saying what changed
and why (Sid, 2026-09-02: "too small to know anything"). On main, exact paths, no Co-Authored-By. Do
not stage CLAUDE.md, CLAUDE-1.md, or anything under .claude/memory/.
```

## 12. The falsification round (paste-able, one fresh Codex-class session, at Sid's hand)

```
Read docs/seam-cuts/CONTRACT-2-4.md primary and the files it names, by the windows it names. Falsify
it: claim → source, evidence cited, no verdict authority, no recut. For each of §7's scenarios name one
more wrong build that passes it. For each value a consumer needs, quote the written road from its
producer; the image residency status, the region session revision and the text container each cross a
file. Expand every "the/its" on a load-bearing noun to its referent; where two readings exist, say so.
Check §4's custody against §1: any removal whose file sits outside its session's heading is a finding.
Return ≤3 decision-changing findings with anchors; everything else is a list of lines. The author
repairs in-session.
```
