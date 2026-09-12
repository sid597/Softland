# INDEX
A STORE — slot = one view-instance's resolved tree + flat op arrays + address index; store = {:slots :index :ordered}; upsert/remove maintain index + sorted order incrementally.
B FACE TREES — build-face-tree = apply-assembly + stamp-block-addresses; face_assembly is a HARD require of scene_store, not injected.
C RECT TREE — 12-key node; column/row stack layout with padding/gap/align/auto-height (no flex grow); text height from tl/ text-layout, measured in the primitive; 8 tree->* walks; bounds broad-phase + per-family narrow predicate + bubbling dispatch.
D CONTAINERS — {:containers {cid→…} :next-transport-slot :free-transport-slots}; effective = parent ∘ local via compose-affines; transport-slot = compact GPU index decoupled from sparse cid.
E SCENE TAPE — 10 families (not 9); entry carries :order {stratum pass-class stack-path part-rank stable-tie}; forward paint / reverse pick; ordered-insert/remove on a sorted-map vs compile-tape the batch ORACLE; register-family supplies contract DATA (no paint/hit fns).
F SCENE RUNTIME — 2 atoms + 5 side atoms; container-delta journal; face-instance spawn/retire; 3 Missionary views; 2 window installers; references 5 deleted files.
G CHROME / REGION3D — chrome owns selection+snap+gesture and strangles two verbs; region3d owns a 3D session, listeners, gizmo.
H FRAME RUNTIME — W4 fixture join; boot!/mount!/sync-pulse-deadline!/export-viewport!; calls scheduler + a global compositor provider, never renderer directly.
I selection.cljc / snap.cljc — pure session models, no atoms.
J DEAD vs LIVE — 18 of 25 scene_runtime public fns have zero callers; verifier calls ONLY frame-runtime/flag-enabled-search? + felt-fixture-receipt; nothing calls any boot!/mount!.
K SURPRISES — cid recycling to avoid reactor death; the color contract that is code-real and default-off; every scene slot picks as :render.family/rect.
L GENERATIONS — P1/P3a/P3b/P4 · W2-A/W2-B · SEAM-STEP1 · IMAGE-ATOM · Region3D · W4 all coexist.

DECISION SERVED (all blocks): explaining the scene layer.

---
## A. THE STORE
FACT. A slot is one view-instance's whole render answer, precomputed. Keys:
:vi :container :container-slot :stack-path :tree :ops :addresses :meta :stratum.
:tree is a RESOLVED rect-tree in container-LOCAL coords; :ops is 8 flat GPU
arrays (text rects shadows images regions paths connectors chromes) with the
container index already baked in; :addresses is {address → #{index-path}} where
an index-path is a get-in vector like [:children 2 :children 0]. An ADDRESS is
the semantic id of a thing (usually a block unit-id) stamped at [:data :address]
— one address can appear in many slots. A VIEW-INSTANCE (vi) is the key of one
appearance, e.g. [:vi :reader-face 2] or :face-main. Store top level is
{:slots {vi→slot} :index {address→#{vi}} :ordered <sorted-map key→entry>}.
Ops are computed ONCE at upsert so untouched slots keep identical? arrays.
Core ops: empty-store, slot, slots-for-address, upsert-slot, remove-slot,
update-nodes-by-address (the Δ1 fan-out: one address → every appearance),
rebuild-ordered, maintained-entries, derive-store-frame, pick.
SOURCE src/app/client/workspace/scene_store.cljc:16-26 (shape), :97-124 (build-slot), :151-156, :169-212, :220-244, :314-319
EXTRACTION
  Store value: {:slots {vi → slot} :index {address → #{vi}}
                :ordered {[order-token entry-id] → entry}}.
  The :index fan-out is maintained incrementally by upsert/remove, NEVER
  recomputed by scanning slots at read time. Resolution route:
  address → :index → vis → slot :addresses → index-paths → nodes.
UNCERTAINTY :ordered is a sorted-map keyed [order-token entry-id]; an EDN
round-trip loses the comparator, hence the explicit rebuild-ordered door (:301-313).

## B. FACE TREES
FACT. There is no separate "build-face-tree" grammar: it is two calls.
fa/apply-assembly runs a COMPILED assembly (closures) over a data projection
plus a view-ctx {:view-instance :address :geom} and internally does one
resolve-layout; stamp-block-addresses then walks the resolved tree and copies
each block's unit-id from the node's :id vector into [:data :address], because
the store indexes ONLY [:data :address] while the assembly interpreter threads
unit-ids into :id. The compiled assembly holds builder closures and therefore
must NOT live in a slot (slots must stay serializable), so it lives in the
runtime's !vi-faces atom. face_assembly is required at the top of scene_store —
a hard dependency, not injected; face_primitives is the primitive registry
face_assembly walks, and scene_store never mentions it.
SOURCE scene_store.cljc:20-26 (require), :493-515 (build-face-tree), :449-481 (stamp-block-addresses); face_assembly.cljc:576-590
EXTRACTION
  (defn build-face-tree
    [compiled projection view-ctx unit-ids]
    (-> (fa/apply-assembly compiled projection view-ctx)
        (stamp-block-addresses unit-ids)))
UNCERTAINTY apply-assembly's docstring claims purity/totality ("a bad assembly
renders an error card"); I read the docstring, not its body.

## C. RECT TREE
FACT. rt-node returns a 12-key map: :id :type :bounds :style :actions :children
:text :clip? :data :layout (plus :text-layout and :bounds-derived keys set by
callers). Bounds are parent-relative; children paint back-to-front. Layout is a
stack, not flex: layout-children reads :layout {:direction :column|:row :gap
:padding :align :auto-height?}, walks children in order assigning :x/:y,
supports CSS-shorthand padding (n / [v h] / [t r b l]) and cross-axis align, and
with :auto-height? sets the parent's :h from the last child's edge + padding.
There is no grow/shrink/basis. Text height comes from the text-layout module
(tl/layout → measure-result :metrics :stack-advance), i.e. the shaper is called,
inside resolve-text-layout. resolve-layout is the one entry: per node it runs
layout-children then resolve-text-layout, then recurses into children.
Flattening: tree->rects, tree->text-ops, tree->shadows, tree->images,
tree->regions, tree->paths, tree->connectors, tree->chromes — eight independent
depth-first walks over the same tree, each accumulating absolute coords and an
intersected clip rect. Hit testing: hit-test does bounds broad-phase, recurses
children in REVERSE (front-to-back), and consults family-hit-predicates for a
narrow phase (path/connector/chrome supply real math; everything else is true).
It returns the root→leaf node path; dispatch-event bubbles that path leaf-first
and the first non-nil handler stops propagation.
SOURCE rect_tree.cljc:19-35, :54-131, :133-172, :174-183, :187-203, :205-, :640-668, :669-693, :697-715
EXTRACTION
  (defn resolve-layout
    [node]
    (let [laid-out  (-> node layout-children resolve-text-layout)
  ...
  (def family-hit-predicates
    "Per-family narrow-phase seam. Bounds remain the universal broad phase;
     registered families may replace only their own mathematical predicate."
UNCERTAINTY layout-children runs BEFORE resolve-text-layout on the same node and
children resolve AFTER the parent, so an :auto-height? parent sizes from its
children's PRE-resolution bounds. face_assembly's "measure in the primitive,
arrange in the engine — trap T7" says primitives pre-measure, which would make
this consistent; I did not trace a primitive to confirm.

## D. CONTAINERS
FACT. Registry = {:containers {cid → {:parent :affine :camera :layer
:sibling-rank :effects :transport-slot}} :next-transport-slot N
:free-transport-slots #{}}. cid 0 and transport slot 0 are reserved for the
identity world container. An affine is six numbers in SVG/CSS order
[a b c d tx ty]. effective walks each cid's parent chain once with memoization
and cycle detection: effective-affine = compose-affines(parent, local), i.e.
parent ∘ local; it also inherits :camera, and appends [cid layer sibling-rank]
to the parent's :stack-path. It returns {cid → {:affine :flags :layer
:stack-path :transport-slot}} where :flags 1 means screen-camera. A TRANSPORT
SLOT is a small dense integer allocated per live container (recycled on
remove) so the GPU's 32-byte-per-entry affine storage table is indexed
compactly instead of by sparse semantic cids. Point conversion: forward-point
and inverse-point (inverse via the determinant), plus transform-bounds and
screen-bounds. The renderer uploads containers/effective through
:transport-slot values into an "containers/affine-storage" buffer.
SOURCE containers.cljc:1-14, :64-86, :185-193, :206-252, :254-277; renderer.cljs:795, :801
EXTRACTION
  Semantic container ids never index the GPU table.  Each live container has
  a compact, stable :transport-slot.  That indirection is the Q8 transport
  seam: sparse material ids do not amplify the 32-byte storage table.  cid 0
  and transport slot 0 are permanently reserved for the identity world
  container.
UNCERTAINTY fallback-transport-slots invents slots for hand-built registries by
sorting cids with pr-str — deterministic but string-ordered.

## E. SCENE TAPE
FACT. TEN families, not nine: rect, shadow, msdf, slug, clip, image, path,
connector, chrome, region-3d. An ENTRY is a flat map: :entry/id :material/id
:material/revision :instance/id :family/id :order :paint :pick :visibility (+
:runtime/slot when minted by the store). The sort key is :order = {:stratum
:pass-class :stack-path :part-rank :stable-tie}; compare-order ranks stratum
(world 0 / overlay 1 / region-composite 2), then pass-class (frame-policy,
direct, intermediate, region, present), then the nested stack-path node by node
([context-id layer sibling-rank]), then part-rank, then a stable tie token —
so document order is carried as data on the row, never as a position in a
vector. Paint and pick are the SAME tape: paint-forward maps an executor over
:entries in order; pick-reverse rseq's the same vector, skipping invisible or
:none-pick entries, and returns the first hit. Two order paths coexist:
ordered-insert/ordered-remove keep a persistent sorted-map (comparator
entry-key-compare) updated per write, and compile-tape validates + sorts a whole
batch and is explicitly the ORACLE the maintained path is fenced against in
tests. register-family is pure and fail-closed: a family supplies contract DATA
only (grammar, pick policy, provenance, versioning, render geometry with
zoom-partitioned regimes that must tile [0.01,1000] with no gap, and the
scene-color tags) — no paint fn, no hit fn, no sort rank; duplicates throw.
The scene-color seam declares two resources, legacy-direct (straight alpha,
presentation-encoded) and linear-premultiplied-srgb, with the linear one
default-OFF.
SOURCE scene_tape.cljc:14-27, :53-68, :570-586, :587-602, :624-651, :653-687, :689-720, :724-747, :748-768; test/app/client/substrate/maintained_view_test.clj:26-32, :134-159
EXTRACTION
  (defn compile-tape
    "Validate and compile one immutable ordered tape.  Input order and family
     registration order are irrelevant; only Contract-O order tokens sort it."
  ...
  ;; SEAM-STEP1 T6: insert-edge validation is additive; compile-tape below
  ;; retains its independent validation and remains the batch oracle.
UNCERTAINTY the exact phrase "document order is row data — sort keys, never
position" lives in docs (history/docs/ARCHITECTURE.md:141, docs/decisions.md:570), not
in scene_tape source; the code embodiment is entry-key + compare-order.

## F. SCENE RUNTIME
FACT. Owns two primary atoms — !scene-store and !containers-registry — plus
!frame-container-delta-journal, !last-pick, !action-registry,
!region-pick-resolver, !vi-faces, !next-cid, !free-cids. A container DELTA is
minted by frame-delta/container-delta from the before/after declaration of one
cid on every registry mutation, stamped with a monotonic :delta/sequence, and
appended to the journal; consumers read frame-container-delta-snapshot and call
ack-frame-container-deltas! with a high-water mark to prune. Face-instance
lifecycle: register-face-instance! allocates a cid from a recycling pool, adds
the container FIRST, asserts it is registered, then upserts the slot;
close-instance! drops slot + container + !vi-faces entry and frees the cid;
close-all-slots! clears everything and nils !last-pick; refresh-all-slots! is
the echo fan-out (stale conversations closed, survivors rebuilt in one swap!).
Picks: pick-world inverse-transforms and delegates to ss/pick, then routes
:region3d hits through an installed resolver; record-pick!/last-pick keep the
deictic memory; bundle-for-viewport / bundle-at-world-point build the EDN
context bundle. Actions: register-action! + dispatch-action over a runtime
registry of fns (kept out of the store so scenes stay serializable). Three
Missionary views: <store-frame (m/latest ss/derive-store-frame over the store →
the GPU payload: rects/shadows/images/regions/paths/connectors/chromes,
targets-by-address, text-by-vi, clips-by-vi, ordered-vis, ops-count-by-vi,
order-by-vi), <effective (m/latest ctn/effective over the registry → {cid→eff}),
<frame-registry (m/latest identity over the registry → plan-layer topology +
effects). Two window installers: install-window-api! (window.sceneFaces —
spawn/close/move/scale/list) and install-context-window-api! (window.sceneContext
— bundle()/edn()). The !-suffixed wrappers are the edge-only mutators:
mint-container-delta!, mutate-container-registry!, alloc-cid!/free-cid!,
register-face-instance!, close-instance!, close-all-slots!, refresh-all-slots!,
upsert-main-face!, close-main-face!, set-transform!, set-effects!, record-pick!,
register-action!, install-*!.
DEAD REFERENCES (all five files are gone from src/):
  scene_runtime.cljs:13  "m/reduce edge (render.cljs), NOT here (T4)."
  scene_runtime.cljs:80  "(mouse.cljs). The context bundle re-picks from :world-point when a cmd/agent"
  scene_runtime.cljs:146 "Reproduce editor_compute's <face-assembly geom from the runtime atoms so a"
  scene_runtime.cljs:305 "render.cljs (T4: edges only, and NOT the RAF edge — a same-wave upsert keeps"
  scene_runtime.cljs:389 "Consumer-edge record of a face pick (mouse.cljs): the WORLD point pointed at"
  scene_runtime.cljs:486 "currently-worn face (face_wiring/compile-served-source!). So a mismatched"
  scene_runtime.cljs:594 "cmd/agent turn at submit time (agent_flow.cljs) and logged [SCENE-CTX]."
SOURCE scene_runtime.cljs:34-82, :105-131, :186-294, :308-355, :361-382, :429-470, :475-543, :569-599
UNCERTAINTY the docstrings describe a consumer edge in render.cljs that no
longer exists; I confirmed the FILES are absent, not what replaced them.

## G. CHROME & REGION3D RUNTIMES
FACT (chrome_runtime). Owns session selection and manipulation state:
!derive-state (chrome-derive's incremental state, holding :selection and the
desired chrome :slots), !registrations (slot-id → the scene slot it installed),
!snap-state, !gesture, !drag, !selection-io, !receipt, !host. It feeds the scene
store by reconciling: chrome-derive computes a desired slot set, reconcile-slots!
closes vanished ones and installs/updates the rest through
scene-runtime/register-face-instance! and direct swap! of !scene-store;
frame-edge! value-diffs effective transforms each frame and writes only the
chrome containers whose target moved. boot! demands explicit host hooks
(camera-snapshot, install-selection-hooks!, register-verb!) and STRANGLES two
verbs — :selection/marquee-begin and :placement/drag-group — capturing their
previous impls. It publishes __softlandChromeReceipt to globalThis.
FACT (region3d_runtime). Owns a 3D session value in !session {:version :enabled?
:focused-region :regions :panel-position :settled-diffs :focus-opens
:focus-closes}, plus !canvas, !io, !listeners, !registrations, !pick-cache,
!fixture-hooks. It registers three fixture slots (underlay/region/overlay) into
the scene store as ordinary face instances, installs its own capture listeners
(pointerdown/move/up, dblclick, keydown, wheel), and installs itself into
scene-runtime via install-region-pick-resolver! so a :region3d hit from
scene-store's pick is completed with region-local coordinates, gizmo handles and
camera — session values join at the renderer edge and never enter store
derivation.
SOURCE chrome_runtime.cljs:1-20, :82-95, :152-166, :375-411; region3d_runtime.cljs:1-45, :158-175, :204-238, :609-651
EXTRACTION
  (defn boot!
    "Prepare the parked Chrome runtime against explicit host hooks. No product
     client supplies these hooks while the land is dark."
UNCERTAINTY "the land is dark" is the code's own word for its unbooted state; I
did not find the doc that defines it.

## H. FRAME RUNTIME
FACT. A flag-only ("?live-atoms=1") W4 join whose namespace load is pure.
boot! — when the flag is on, installs eight hand-built fixture trees (opacity
underlay + group, nested outer/inner, mask group + source, backdrop, gpu-clip)
as ordinary scene slots via scene-runtime/register-face-instance!, each with a
:layer/:sibling-rank and some with container :effects {:opacity 0.5}; then
publishes a receipt. mount! — installs the Ctrl/Cmd+Shift+E export chord once.
sync-pulse-deadline! — the selection→deadline bridge: it reads the selected
count out of the global __softlandChromeReceipt and registers/unregisters a
30 Hz deadline (frame-scheduler/register-deadline! with a stop-predicate) so the
scheduler keeps drawing while a selection is live. Receipts —
felt-fixture-receipt is a pure structural summary of the fixture trees (stripe
counts, backdrop/detail boundary overlaps, clip-text overhangs and labels);
publish-receipt! merges the global __softlandFramePlanReceipt and the global
__softlandFrameCompositor's receipt() and writes __softlandFrameRuntimeReceipt.
PNG export — export-viewport! calls the global compositor provider's
exportViewport(), builds a Blob and clicks a hidden <a download>. Nothing here
calls the renderer's draw-frame! directly; the scheduler and the global
compositor provider are the seams. Callers: only verifier.cljs, and only for
flag-enabled-search? and felt-fixture-receipt — nobody calls boot!, mount!,
sync-pulse-deadline!, install-live-opacity-group!, or export-viewport!.
SOURCE frame_runtime.cljs:1-11, :37-51, :172-196, :198-260, :261-283, :285-304, :306-354; verifier.cljs:3637, :3641
EXTRACTION
  dark-lane {:flag-off? (not (frame-runtime/flag-enabled-search? ""))
  ...
  felt-fixtures (frame-runtime/felt-fixture-receipt)
UNCERTAINTY who is supposed to CALL boot!/mount! is not answerable from these
files; the CLJS entry point that once did is not in src/.

## I. selection.cljc / snap.cljc
FACT (selection). A pure, atom-free selection model over occurrence identities
{:vi :address} — exactly what picking returns — with a closed transition
vocabulary (toggle, marquee-commit, clear, frame-prune) dispatched by
`transition`, plus marquee hit math, a legacy projection down to block unit-ids,
and selection-census which counts selected blocks / fixtures / edges by family.
FACT (snap). Pure world-space snapping: extract-candidates turns targets into
world rows (excluding connector and chrome families), resolve-axis finds the
best edge/center alignment within a zoom-scaled threshold, resolve-snap returns
position, delta, alignments, guides and equal-gap ticks minted together so paint
cannot advertise a snap the arrangement did not take, and gesture-step is a
state-in/state-out cache that does zero work on an unchanged input token.
SOURCE selection.cljc:1-10, :151-159; snap.cljc:1-9, :206-246
UNCERTAINTY none material.

## J. DEAD vs LIVE
FACT. Requires: scene_runtime is required by exactly three files — chrome_runtime,
frame_runtime, region3d_runtime. Those three are required by exactly one file —
verifier.cljs. Nothing else in src/ or test/ requires any of the four.
Zero external callers in scene_runtime.cljs (18 of 25 public fns):
ack-frame-container-deltas!, any-slots?, block-unit-ids, bundle-at-world-point,
bundle-for-viewport, close-all-slots!, close-main-face!,
container-registry-snapshot, dispatch-action, frame-container-delta-snapshot,
install-context-window-api!, install-window-api!, last-pick, record-pick!,
refresh-all-slots!, register-action!, set-effects!, upsert-main-face!.
Live (called from the three sibling runtimes only): effective-transforms (8),
set-transform! (5), close-instance! (4), register-face-instance! (3),
store-snapshot (3), install-region-pick-resolver! (2), pick-world (1).
chrome_runtime: boot! and frame-edge! have zero callers; receipt zero callers.
region3d_runtime: every public fn zero callers except enabled? and
flag-enabled-search? (self-referencing within the file's own boot path).
frame_runtime: boot!, mount!, sync-pulse-deadline!, install-live-opacity-group!,
selection-active!, export-viewport!, receipt have zero external callers.
VERIFIER: it requires chrome-runtime and region3d-runtime but NEVER calls a
single symbol from either (dead requires). It calls frame-runtime twice:
verifier.cljs:3637 flag-enabled-search? and verifier.cljs:3641
felt-fixture-receipt. It never touches scene_runtime.
SOURCE grep over src+test for `<ns>/` qualified calls, excluding each defining file
UNCERTAINTY dynamic entry is possible in principle (aget/goog exports); I found
none for these namespaces, but I searched src/ and test/ only.

## K. THREE MOST SURPRISING CHOICES
1. Container ids must RECYCLE or the reactor dies.
SOURCE scene_runtime.cljs:107-113
  ;; Gate-review F3 (2026-07-13): cids must RECYCLE. A monotonic counter walks
  ;; into the renderer's 1024-container ceiling after ~992 spawn/despawn cycles,
  ;; and the resulting write-containers! throw lands OUTSIDE the draw try/catch —
  ;; reactor death from a dev affordance.
2. A full color-management contract exists in code and is deliberately switched off.
SOURCE scene_tape.cljc:53-60
  (def scene-color-seam
    "The candidate Contract-C resource is code-real but deliberately default-off.
     Direct presentation remains the byte-identical legacy route until a later
     activation receipt explicitly selects the linear-premultiplied candidate."
3. Every scene slot enters the tape as family :render.family/rect regardless of
what it actually draws — the ten-family registry describes families, but the
store mints one family for picking.
SOURCE scene_store.cljc:275-278
  ;; Current product picking is rect-tree bounds (the W0-A divergence
  ;; sentinel says so explicitly); text roads remain paint derivations.
  :family/id :render.family/rect
UNCERTAINTY runners-up worth a look: store-fns-free? walks METADATA to catch a
closure that printing would drop (scene_store.cljc:726-742); compile-tape's
:order-hash is literally "scene-order-v1/" + pr-str of the receipt, "not a
process/runtime hash" (scene_tape.cljc:743-747).

## L. GENERATIONS
FACT. At least six naming generations coexist, and they map to different files
rather than to different eras of one file.
- scene-substrate P1/P3a/P3b/P3c/P4 + "first-light P1" + trap T1–T10 + G1–G11 +
  "CONTRACT §3/§4/§5/§7" — the store/runtime pair. P3a = one singleton face
  mirrored; P3b = per-instance compiled faces (build-face-tree, !vi-faces,
  close-instance!); P1 first-light = the main face becomes an ordinary slot
  (upsert-main-face!, :pre-resolved?); P4 = the deictic seam (context-bundle,
  !last-pick, actions router). scene_store.cljc, scene_runtime.cljs.
- W2-A / W2-B / Contract-O / Contract-M / Contract-G / Contract-C / Q5 / Q8 —
  the transform + tape generation. W2-A minted the canonical affine, the compact
  transport slot and the nested stack path; W2-B added family registration,
  order tokens and the paint/pick projections. containers.cljc, scene_tape.cljc.
- SEAM-STEP1 (T1/T2/T3/T6/T9/T10) — the maintained-sorted-view retrofit that put
  :ordered into the store beside the batch compile-tape oracle. scene_store.cljc
  upsert/remove/rebuild-ordered, scene_tape.cljc ordered-insert/entry-key-compare.
- IMAGE-ATOM (G1/G8/Q5/T1/T8/T14) — the image family added as registration DATA
  so the validator and executor gained no image branch. scene_tape.cljc
  image-registration, scene_store.cljc stamp-ops-container.
- Region3D — regions as a family with their own pick route, session-owned
  resolver and placement resolution. rect_tree.cljc tree->regions,
  scene_store.cljc pick's :route :region3d, region3d_runtime.cljs.
- W4 / live-atoms + Task-18 — the newest layer: frame passes/resources/clip modes
  as vocabulary in scene_tape.cljc:591-605, the flag-only fixtures and receipts in
  frame_runtime.cljs, the chrome selection lane in chrome_runtime.cljs +
  selection.cljc + snap.cljc.
- Contract-T — the older text/clip generation still living inside rect_tree.cljc
  (wrap-line is kept purely as a "compatibility name").
SOURCE marker census across the ten files (grep of P#/W#/Contract-#/SEAM-STEP1/
IMAGE-ATOM/Region3D/trap T#/G# per file)
UNCERTAINTY the marker names are what the code calls itself; I did not read the
contract documents behind them, so which generation SUPERSEDED which is inferred
from the code's own retirement language ("the P3a singleton !face-scene mirror is
gone", "the worn face's singleton legacy render path retires").
