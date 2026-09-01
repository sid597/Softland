# Seam cuts — atom 1: the path seam

*the waist, round two · atom 1 of the seam-cuts package · contract cut 2026-09-02 · cutter Fable 5.1, effort max · one session, straight through*

**Ground.** Source baseline `3d703a5`. The client is 29 files, 15,819 lines, folded by kind under `engine/ text/ image/ path/ region3d/ verifier/`; `verifier/core.cljs` is the only compiled entry and the only caller of every painter. The tree is the ground; this page is the cut. Read code, not docs. The two waist maps that fed this contract were drawn from code alone under a fence and merged in chat on 2026-09-02; their merged position is §0 below, and it is the source for the pending amendment of `docs/decisions.md`.

**Sid's rulings, verbatim (2026-09-02).**
- Settlement + both forks: `land it · A: data · B: GPU`
- The dead criterion: "the definition of what should be dead, from my perspective, Things that should be dead are not explicitly... that are not being called from anywhere, but like that exists for some future valid case and are implemented in the form that they should be"
- The order of work: "first make the existing code how it should be and only then fold the other things in … we don't have a driver apart from the test code its fine that is how it should be as of now … we should be thinking from the pov of what is the input and what is the output of each of the existing files and new ones we make … note that we are not going to use the heuristic of hey lets keep this code until better one comes then we will remove .. no we are going to remove now"
- The layer above the waist, in his name: "we leave other heigher level layers for ecs and call that above the waist. Anything above the waist is stored as data, is collaborative editable, createable, agents and humans can build freely over it without getting into git merge deadlocks, anything in ecs layer should be creatable and then saved for reuse or build higher order things from it"

**Sid's two touches:** this page (open) · his word on the close receipt (accept). No gates between. Keep going.

---

## 0. The settled position (source for the decisions.md amendment)

Two residency axes decide where a thing lives: **grain** (per pixel is floor; per vertex or per mark is the ECS layer) and **whether the camera is an input** (a derivation with a camera input is floor-private, never a row).

**Floor** (compiled, physical or in flight): fill any contours under a fill rule with per-pixel coverage · paint kinds the shader evaluates · composite ops (clip by mask, group opacity, blend) · the camera · the pointer · the clock · the wire · culling · the in-flight store (what is not yet a row) · the derivation cache (floor-private derivations of settled rows) · the hit builtin the runtime calls with screen-pixel slop converted through the effective scale · the runtime and its builtins. Sid's older criterion still names it: description in, geometry out, or geometry in, pixels out, without knowing what the thing is or what a gesture means, plus custody of GPU memory and order (`docs/below-the-waist/INSTANCE-CUT-NOW.md`).

**Verbs** are data, run by a client runtime over compiled builtins (ruling A). The stroker, the brush, layout, every shape, every compound are verb rows. The stroker emits camera-free offset contours or it is not done.

**Rows** are truth and derived rows. A derivation is a row only if something above the pack step reads it: layout passes (caret, selection, hit, on-plane read it), scene evaluation passes (pick, shadow space read it), effective affines pass (hit, clip read them). Meshes and atlas placements fail and are never rows.

**Fill** moves to the GPU (ruling B): contours under a rule with per-pixel coverage, curves evaluated per pixel the way the Slug text shader already does for glyphs. Ear clipping, hole bridging and the zoom-regime table go with that atom. The legal zoom envelope lives in the server's space facet; what remains on the floor is one flattening tolerance in screen pixels.

**The seam** is a row boundary, not a folder boundary. A band's vocabulary never appears in another band's rows: no GPU slot on an op, no process-local stamp on a material, no shader concern in a grammar. The floor translates at the door. Ops name container ids and the placement tree runs. The frame gate is the row's revision. The grammar of a kind is declared data checked by one engine. The color lives once.

**Dead** means wrong form for the waist, called or not. Right form for a valid future case is alive, called or not.

---

## 1. Scope

Atom 1 makes `src/app/client/path/*` the right form at its seam, with no change to what it draws. It is the template: the same removals repeat on image, text and region as atoms 2–4. After atom 1, a path row is a validated EDN map with a content key and a revision; an op names its container by id; the painter gates on revisions, resolves slots through the tree, and paints through the shared color law; the grammar is data; ink knots carry their width; hit and boundary agree on one formula; nothing in path/* is an instrument or a fixture.

Removed now, by Sid's criterion, called or not:

| piece | verdict | why |
|---|---|---|
| the admission stamp (`admitted?`, `stamp-admitted`, `admit-material`, `require-admitted` in material and its copy in tessellation, the restamp inside `canonical-material`) | remove | a process-local mark in Clojure metadata; `pr-str` drops it, so a row over the wire arrives unadmitted and every reader throws. Validation runs once at the door. |
| `:container-idx` read from the op | replace | a GPU slot where a container id belongs. The op names `:container`; pack resolves id to slot through `placement/effective`. Today every op says 0 and `effective` has no caller. |
| the two frame gates (`:!last-paths` + `:!last-regime` structural equality, then `:!last-mesh-set-key` over canonicalized materials) | replace | both are O(geometry) per frame; neither reads the field a row carries for this, its revision. One gate: revision + container per op, plus the regime. |
| `:!receipt`, `path-receipt`, `:!shape-rev` | remove | a ledger inside the painter; the per-frame return of `prepare-path-frame!` is the receipt. The shape revision is written and never read. |
| `path-color-mode-declaration`, `configure-path-color-shader`, the painter's `scene-color-blend`, the srgb function inside `path-fragment-shader` | remove | copies of the color law that lives in `engine/device.cljs`; text and image painters already use the shared one. |
| `example-ink-material`, `example-shape-material` | move to a test fixture | fixtures in src |
| `f32-roundtrip`, `quantization-receipt` | move to the verifier | instruments |
| `clamp-pressure`, `pressure-width`, `:base-width` on ink geometry | replace | width is decided per knot when a stroke is created, by the brush. The knot carries `:width`. Pressure and gesture time remain as optional gesture provenance the readers never use. |
| the pressure fork (`classify` interpolates along the segment, `boundary-distance` averages the two knots) | replace | one formula, `segment-delta`, shared by both |
| `material-points`, `shape-normalization`, `normalize-point`, `denormalize-point` | move into tessellation, private | numerics of the derivation |
| the hand-written validators (`validate-paint!` … `validate-material!`) | replace in one move | right meaning, wrong form: a declared grammar checked by one engine is something the land can show; imperative throws are not |
| `finite-number?` in path/material | move to the grammar engine | region3d and placement carry their own; atoms 2–4 switch them |

Kept, right form, valid case: `canonical-material`, `material-content-key`, `contour-classify`, `classify`, `hit?`, `boundary-distance`, `paint-color` (a reader of the paint row; on-plane reads it too), `legal-cap-join #{:round}` (fail-closed and honest about the stroker), the stroker, ear clipping and hole bridging (until atom B), `zoom-regime` and its table (until atom B), `algorithm-version`, `material-cache-key`, `tessellate`, `derive-mesh-set`, `init-path-system`, `ensure-capacity!`, `pack-vertices` with color per vertex (fat, not wrong), `draw-path-range!`, `destroy-path-system!`.

## 2. Refusals (each routed to LATER, none a void)

- Does not change what the stroker emits or how fills are rasterized. **Atom B** (GPU fill): stroker to contours, ear clipping and hole bridging out, regime table out, one screen-pixel tolerance.
- Does not touch image, text or region beyond the one public hook in `engine/device.cljs`. **Atoms 2–4** repeat this cut per kind; the one color grammar and the one placement grammar across kinds land there.
- Does not build the runtime, the brush verb, the in-flight store, the pointer, the wire, the clock, or culling. **Atom A** and the missing-floor atoms, in the order a canvas needs them: pointer, in-flight store, wire, clock, culling.
- Does not unify the client grammar engine with the server's `facet-engine` (see §9 F1). **LATER**, with a dependency-direction ruling.
- Does not share the three vector-math kits (tessellation, scene, placement). **LATER.**
- Does not amend `docs/decisions.md`. **Fable, fresh context**, source §0; a mechanical merge, not a re-derivation.

## 3. Laws (pointers, never restated)

- The oath on everything below and on every verb: deterministic and versioned, same input → byte-identical output. Fails under S4 if the width move changes a mesh byte.
- Source structure: kind → engine, never engine → kind, never kind → kind except `region3d/on-plane` (CLAUDE.md). Fails if `path/*` requires anything outside `engine/` and `path/`.
- Rulings quote Sid verbatim (CLAUDE.md). This page's header carries them.
- Work-package law: contract, then execution straight through, two Sid touches, close receipt (`.claude/skills/work-package/SKILL.md`).
- The parallel-sessions git discipline: exact paths staged, foreign-change check, plain commits, never a Co-Authored-By line (memory `feedback-parallel-sessions-shared-branch-git`). The working tree at cut time shows `CLAUDE.md` deleted and `CLAUDE-1.md` untracked, plus sibling edits under `.claude/memory/`; this atom stages none of them.

## 4. Entry points (exact)

**New: `src/app/client/engine/grammar.cljc`** — the grammar engine. `check [spec form] → form` or throws `ex-info` with `{:error-type kw :path [ks…] :value v}`. A spec is `{:keys #{required…} :optional #{…} :validators {k pred-or-spec} :form-validators [{:valid? f :error-type kw}]}`; unknown keys are refused at every level; a validator that is itself a spec recurses. Exports the shared predicates by the server's names: `finite-number?`, `valid-rgba?`, `non-negative-number?`, `positive-number?`, `point?`. About 60 lines. Nothing else may validate a path.

**`src/app/client/path/material.cljc`** (after: about 230 lines)
- `grammar` — the declared spec (§6), public data.
- `validate-material! [m]` = `(grammar/check grammar m)`; returns the map, no metadata.
- `canonical-material`, `material-content-key` — unchanged in meaning; take a validated map, never check admission.
- `segment-delta [a wa b wb p] → {:t :distance :half-width :delta}` — private; width interpolated linearly at the projection parameter t, clamped to [0,1]; `delta = distance − half-width`.
- `classify`, `hit?`, `boundary-distance` — ink cases read `segment-delta`; `boundary-distance` for ink = min over segments of `|delta|`.
- `contour-classify`, `paint-color` — unchanged.
- Gone from this file: the stamp trio and `require-admitted`, the examples, `clamp-pressure`, `pressure-width`, `finite-number?`, `material-points`, `shape-normalization`, `normalize-point`, `denormalize-point`, `point-segment-distance` as a public.

**`src/app/client/path/tessellation.cljc`** (after: about 430 lines)
- normalization moves in as private (`material-points`, `shape-normalization`, `normalize-point`, `denormalize-point`); `normalized-ink` divides each knot's `:width` by the scale factor (today it divides `:base-width`, line 109).
- the stroker reads `(:width knot)`: radii at lines 124–127 and 161–162 become `(/ (:width knot) 2.0)`.
- `require-admitted` gone; every entry takes a validated map.
- `f32-roundtrip`, `quantization-receipt` gone (to the verifier).
- unchanged: `legal-zoom-regimes`, `zoom-regime`, `algorithm-version`, `material-cache-key`, `stroke-triangles`, `shape-triangles`, `tessellate`, `derive-mesh-set`.

**New: `src/app/client/path/frame.cljc`** — what one frame of paths asks of the floor, pure, JVM-tested. `frame-key [ops regime] → [[material-id revision container]… regime]`. `slot [effective container] → int` or throws `{:error-type :path/unknown-container}`. About 25 lines.

**`src/app/client/path/painter.cljs`** (after: about 180 lines)
- `init-path-system` builds the fragment shader as `(str device/scene-color-wgsl path-fragment-main)` where `path-fragment-main` is the one entry that returns `scene_color(color, 1.0)`; configures through `device/configure-scene-color-shader`; blends through `device/scene-color-blend`. The painter's own copies are gone.
- system map holds: `:device :pipeline :bind-group :camera-buffer :containers-buffer :scene-color :!buffer :!capacity :!mesh-cache :!prepared :!last-frame-key`. Gone: `:!shape-rev :!last-paths :!last-regime :!last-mesh-set-key :!receipt`.
- `prepare-path-frame! [system ops zoom effective] → {:changed? bool :writes 0|1 :vertices n :derived n}`: regime from `zoom`; `key = (frame/frame-key ops regime)`; if `(= key @:!last-frame-key)` return `{:changed? false :writes 0 :vertices …}`; else derive through `derive-mesh-set`, pack with `(frame/slot effective (:container op))`, upload, store the key.
- `pack-vertices` reads `(:container op)` through `frame/slot`, never `:container-idx`.
- `draw-path-range!`, `destroy-path-system!` unchanged; `path-receipt` gone.

**`src/app/client/engine/device.cljs`** — thin hook only: `scene-color-wgsl`, `configure-scene-color-shader`, `scene-color-blend` become public (lines 16, 33, 39). Text and image painters already call them across the namespace boundary.

**`src/app/client/verifier/core.cljs`** — path road only (lines 1095–1428 and the surround-path calls at 2320–2337):
- fixtures call `validate-material!`, never `admit-material`; ink fixtures mint `:width` per knot as `(* (/ 16.0 zoom) pressure)` and keep `:pressure` as provenance; every fixture mints `:path/revision` as its content key (`(assoc m :path/revision (material-content-key m))`).
- `path-op` → `{:path/material m :container cid}`; `:id` may ride, unread.
- the path road builds a registry (`placement/empty-registry` → `add-container`), computes `placement/effective`, writes it with `device/write-containers!`, and passes it to `prepare-path-frame!`. The region3d floor's surround path system passes the effective map it already writes at line 2315.
- `f32-roundtrip` and `quantization-receipt` live here now; the upload gate (1361–1373) accumulates from `prepare-path-frame!` returns; the `:system` receipt at 1420 is the last return, not `path-receipt`.
- S1's golden case added (§7).

**Tests**
- New `test/app/client/path/fixtures.cljc` — the two example materials, plus the S4 tapered segment.
- `test/app/client/path/material_test.clj`, `test/app/client/path/tessellation_test.clj` rewritten to §7; the two existing mesh SHA-256 fingerprints at zoom 10 are kept verbatim; the quantization-receipt assertion at line 83 is dropped (it pinned an instrument).
- New `test/app/client/path/frame_test.clj` — S3's key law.
- `test/app/test_runner.clj` inventory updated for the two new test namespaces.

## 5. The seam for paths (the only shapes agreed across it)

```clojure
;; a path row, as it travels (EDN, no metadata)
{:path/material-id id  :path/revision rev  :path/kind :ink|:shape
 :path/geometry {…}    :path/paint {:color [r g b a] :opacity o
                                   :color-space :srgb :alpha-association :straight}}
;; an ink knot
{:knot/id id :position [x y] :width w}            ; optional :pressure :gesture-time (provenance, unread)
;; an op the painter takes
{:path/material <validated row> :container <cid>} ; :id may ride, unread
;; the frame gate
[[[material-id revision container] …] regime-id]
;; the slot, resolved by the floor
(get-in effective [container :transport-slot])   ; throws :path/unknown-container
```

Invariant the land's writers own and fixtures honor: **a revision changes whenever the material's content changes.** The gate trusts it; S3 pins it.

## 6. The grammar as data (skeleton the implementer completes)

```clojure
(def paint
  {:keys #{:color :opacity :color-space :alpha-association}
   :validators {:color grammar/valid-rgba?
                :opacity #(and (grammar/finite-number? %) (<= 0.0 % 1.0))
                :color-space #{:srgb}
                :alpha-association #{:straight}}})
(def knot
  {:keys #{:knot/id :position :width}
   :optional #{:pressure :gesture-time}
   :validators {:position grammar/point? :width grammar/positive-number?
                :pressure grammar/finite-number? :gesture-time grammar/finite-number?}})
(def ink-geometry
  {:keys #{:knots :cap :join}
   :validators {:knots [:vector-of knot {:min 2 :unique-by :knot/id}]
                :cap #{:round} :join #{:round}}})
(def contour {…})            ; :contour/id :role #{:outer :hole} :points ≥3 points
(def shape-geometry {…})     ; :contours ≥1, unique :contour/id, a hole needs an outer
(def grammar
  {:keys #{:path/material-id :path/revision :path/kind :path/geometry :path/paint}
   :validators {:path/kind #{:ink :shape} :path/paint paint
                :path/material-id some? :path/revision some?}
   :form-validators [{:valid? geometry-matches-kind? :error-type :path/geometry-kind}]})
```

Error types are named per key (`:path/paint-color`, `:path/knot-width`, …) so a refusal is readable as data. The engine treats a set as a membership predicate and `[:vector-of spec opts]` as a sequence rule; nothing else is invented.

## 7. Decisive scenarios (frozen as tripwires at close)

**S1 — the tree runs.** Verifier golden. A registry with container 17 added under the root with affine `[0.5 0 0 0.5 40 20]`; the same shape material drawn twice, once with `:container 0` and once with `:container 17`; `effective` → `write-containers!` → `prepare-path-frame!`. The golden shows two shapes, the second at half size offset by (40, 20). Wrong build that passes a weaker test: slot taken as the cid, which works only when cid equals slot; 17 gets slot 1. Wrong build two: pack ignores the container, invisible under identity; the non-identity affine catches it. Also pinned: an op naming a container the tree lacks throws `:path/unknown-container` before any GPU write.

**S2 — rows travel and the grammar refuses by name.** JVM. `(edn/read-string (pr-str m))` of each fixture validates, has `nil` metadata, and gives the same content key, the same `classify` answers, and a byte-identical mesh as the original; no reader ever throws "not admitted". The grammar refuses, each by its named error type: an unknown top-level key, a knot with `:tilt`, a contour with `:role :rim`, a paint missing `:opacity`, `:cap :square`, a hole without an outer, a zero-width knot. Wrong build that passes a weaker test: a stamp under another key that the test never looks for; the `nil`-metadata assertion on the read-back map catches it.

**S3 — the gate is the revision.** JVM for the key, verifier for the writes. Frame 1: two ops → derived 2, writes 1. Frame 2: the same ops, same zoom → changed? false, writes 0, derived 0. Frame 3: op one re-minted with a new revision and the same content → changed? true, writes 1, derived 0 (cache hit). Frame 4: the same ops at a zoom that crosses a regime boundary → changed? true, derived 2. Wrong build that passes a weaker test: a structural-equality gate reports writes 0 on frame 3; a gate without the regime reports derived 0 on frame 4.

**S4 — width on the knot, one formula.** JVM. The ink fixture rewritten with `:width` = old base × clamped pressure yields exactly the two SHA-256 mesh fingerprints pinned today at zoom 10. Then the tapered segment from (0,0) width 4 to (10,0) width 12, query point (8,3): `classify` → `:inside` and `boundary-distance` → 2.2 within 1e-9 (interpolated half-width 5.2 at t = 0.8, distance 3). Wrong build that passes a weaker test: the mean kept in `boundary-distance` returns 1.0.

**S5 — the shared color law paints the same bytes.** Verifier. The existing path goldens (`holed-concave`, `translucent-self-crossing`) and the path color receipt at both scene-color modes are byte-identical to the current goldens after the painter's own preamble is deleted. Wrong build that passes a weaker test: `scene_color(color, color.a)` squares alpha, invisible on opaque goldens; the translucent golden and the mode-on receipt catch it.

Representative goldens at close: the two existing path goldens unchanged, plus S1's.

## 8. MUST-NOTs (real only)

- Never read `src/app/server/env.clj`.
- No server, Rama, `/mnt/data/rama`, archive, or durable-data mutation (the standing fence).
- Never a Co-Authored-By line in a commit; stage exact paths only.

## 9. Forks written, with defaults

- **F1, the grammar engine.** The server's `worn/facet_engine.cljc` is reader-conditional and would compile for the client, but its `compile-form` requires every form to carry `:facet-master/*` keys, and a client → server require is a new dependency direction. Default: the client's own `engine/grammar.cljc` with the server's spec shape and predicate names, so specs are one meaning across the two engines. LATER: one engine in a shared place, by ruling.
- **F2, an op without `:container`.** Default: fail closed. The world container is 0 and fixtures name it.
- **F3, `paint-color`.** Stays in material; on-plane reads it, so it is a reader, not a words step.
- **F4, color per vertex.** Stays; atom B reshapes the vertex anyway.
- **F5, the stroker's normalization of width.** Width divides by the same scale factor as positions (today's behavior for base width). Pinned by S4's fingerprints.

## 10. Close

Freeze S1–S5 as tripwires; keep the two path goldens and add S1's. Changed files from `git diff --name-only`, never memory. Terminal NOW ≤15 lines into `docs/seam-cuts/NOW.md`, then flip this package's board pointer; after NOW no edit to atom source, tests or tooling. Foreign suite failures are debt (known: `clj -X:test` red on an unclassified tools test since `b86a5d2`). Receipts state the adapter first.

## 11. The implementer's starter (paste-able, Codex lane)

```
Preflight, before the first prompt: permission mode, remote-control and MCP set now; never mid-session.

Boot, by seam, sizes first (about 100KB; read the verifier and device by the windows named, never whole):
  docs/seam-cuts/CONTRACT.md (this page)            ~19KB   read PRIMARY, whole
  src/app/client/path/material.cljc                  17KB   whole
  src/app/client/path/tessellation.cljc              19KB   whole
  src/app/client/path/painter.cljs                   12KB   whole
  src/app/client/engine/placement.cljc               11KB   lines 1-215
  src/app/client/engine/device.cljs                  12KB   lines 1-45 only
  src/app/client/verifier/core.cljs                 140KB   lines 1095-1428 and 2310-2345 only
  test/app/client/path/material_test.clj              7KB   whole
  test/app/client/path/tessellation_test.clj          7KB   whole
  src/app/client/region3d/on_plane.cljc               8KB   lines 104-117 only

You own the whole atom: build, surface bugs, fix before source freeze. Sid's word:
"we are going to remove now". The criterion for dead is §1's; reachability is not it.
Plan: OWNER you · SOURCE this contract · DONE WHEN §7's five scenarios are tripwires
and §10's close receipt is written. Ambiguity takes §9's default plus a note; a
genuine fork is one question in NOW.md, routed around while unblocked work continues.
Custody crosses one context boundary only after source freeze. Commit on main,
grouped by concern, exact paths, no Co-Authored-By. Do not stage CLAUDE.md,
CLAUDE-1.md, or anything under .claude/memory/.
```

## 12. The falsification round (paste-able, one fresh Codex-class session, at Sid's hand)

```
Read docs/seam-cuts/CONTRACT.md primary and the files it names, by the windows it
names. Falsify it: claim → source, evidence cited, no verdict authority, no recut.
For each of §7's scenarios name one more wrong build that passes it. For each
value a consumer needs, quote the written road from its producer. Expand every
"the/its" on a load-bearing noun to its referent; where two readings exist, say so.
Return ≤3 decision-changing findings with anchors; everything else is a list of
lines. The author repairs in-session.
```
