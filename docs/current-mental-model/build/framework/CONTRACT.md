# Framework Contract — faces as assemblies (grammar v0 · interpreter · vocabulary · artery)

Status: **v1.1 (Fable, 2026-07-11).** Realizes ROAD.md v2 **Steps 1–2 plus the
Step-4 kernel-object schema (§8)** — not the whole road. Direction source:
`ROAD.md` (v2, commit `2048472`); this contract is the binding form. Every
source claim herein re-verified at file:line by the authoring session.
Binding order: `decisions.md` › this contract › derived artifacts (ROAD is
direction-grade input, not binding).

**v1.1 (same day): PROBE reconciliation.** v1 was authored before PROBE.md
landed, with §10 slots + defaults. The probe's numbers resolved every slot ON
its default (no activation fired); its one contract-changing finding —
`:text-layout` is a dead hook and riding it doubles wrap cost — is applied in
§6/§11-G8; the pane scroll contract is pinned in §5/§10. In-place amendment
per D-010 reach; v1 is in git. **Wave word GIVEN by Sid 2026-07-11** — W1 is
dispatchable; the §15 log-entry drafts remain PENDING his countersign
(explicitly not covered by the wave word).

---

## 1 · Purpose, consumers, scope

A **face** is an arrangement of the land's UI vocabulary, described as pure
EDN data (an **assembly**), instantiated by one pure **interpreter** through a
**registry** of primitive builders, fed by real material through one generic
**artery**. Faces are born in the land's own runnable vocabulary — no
render-then-componentize, no import boundary (ROAD invariant 1).

Consumers, in order:
1. **Sid wearing faces** — the Step-2 exit: a real past conversation rendered
   through the Outline face in the dev app, replay/scrub working (D-005: every
   lack the face exposes orders the data work).
2. **The design round** — Step 3 transcribes the strongest candidate faces as
   assemblies; the six designed faces become the grammar's design test suite.
3. **The orchestrator** — Step 7: the AI writes assembly data through the
   lawful A2 path (D-008 A2); schema-validated, error-carded, provenance-first.

What this contract delivers (Wave 1, three lanes + integration, §14):
grammar v0 + two-stage interpreter + JVM golden harness (lane A); the
primitive vocabulary as `.cljc` (lane B); the conversation projection + the
one generic face-pull (lane C); the Outline face over real material (W1-INT).
The §8 schema is FIXED now, IMPLEMENTED in Wave 2.

Non-goals in §12 — each an extension point, not a void.

## 2 · Placement ruling

**New namespaces (new files only, except as fenced):**

| What | Where | Why here |
|---|---|---|
| Grammar + interpreter + error card + golden helpers | `src/app/client/workspace/face_assembly.cljc` | Scene-layer concern, sibling of `rect_tree.cljc`; `.cljc` so goldens run JVM-side with no browser (the block-kernel goldens discipline applied to UI) |
| Primitive vocabulary | `src/app/client/workspace/face_primitives.cljc` | `.cljc` for the same reason; wraps/extracts the existing builders into the §6 interface |
| Conversation projection + projection registry | `src/app/server/rama/face_projection.clj` | The projection layer's precedent is `trail_view.clj` — plain Clojure over kernel query APIs, same level |
| Electric pull | `FacePull` e/defn in `src/app/file_viewer.cljc`; request-watch loop in `src/app/electric_flow.cljc` | The trail-face precedent split: e/defns live in file_viewer (`file_viewer.cljc:196-207`), wiring in electric_flow (`electric_flow.cljc:492-508`) |
| Client glue | `src/app/client/workspace/face_wiring.cljs` | Sibling of `trail_face/wiring.cljs`, same S2 exemption, same law: touches NO server names |
| Tests + fixtures | `test/app/face_assembly_test.clj` · `test/app/face_primitives_test.clj` · `test/app/face_projection_test.clj` · `test/app/fixtures/faces/*.edn` | JVM suite |

**Fences (wave concurrency):** `electric_flow.cljc` + `file_viewer.cljc` are
owned SOLELY by lane C for the wave. `rect_tree.cljc`, `ui_primitives.cljs`,
`trail_face/*` are READ-ONLY for all lanes. Shared runtime files
(`runtime/state.cljs`, `editor_compute.cljs`, `combined_text.cljs`,
`runtime/mouse.cljs`, `runtime/scroll.cljs`, `runtime/workspace_actions.cljs`,
`runtime.cljs`) are touched ONLY at W1-INT by the orchestrating session — the
lanes' fences are disjoint by construction.

**The copy-then-harmonize ruling (lane B).** `ui_primitives.cljs` contains no
js interop (verified, all 435 lines — pure fns over `rect_tree` and
`components.design-tokens`, both already cljc), so it COULD be renamed
`.cljc`. It is not, this wave: the rename puts a lane's hand on a file every
pane depends on mid-wave. Lane B COPIES builder bodies verbatim into
`face_primitives.cljc`; gate G7 pins the copies to the originals mechanically
(source-form equality, §11), so the two cannot drift while both exist. The
post-wave harmonization (rename original to `.cljc`, delete the copies, keep
the callers) is a pre-named extension point, entered only after assemblies
wear in — D-001 pacing.

**Reversal cost.** Everything client-side is new files behind one mount seam
(the mode multiplexer, §5); reverting is deleting namespaces plus the W1-INT
diff. The projection is read-only composition (§7) — no schema, no topology,
no kernel edit; reverting is deleting one file. Rewrite-costly pieces (kernel
objects, new relation kinds, new imp: prefixes) are all deferred to Wave 2
behind named authorizations (§8). Under D-010's test, everything in Wave 1 is
revert-cheap.

## 3 · Seam law + harmonization ladder (contract-level law)

Promoted from `trail_face/wiring.cljs:1-7` (the source's own voice) and the
verified law register — recorded here so "why not Electric-native faces" is
never re-derived:

**Electric owns truth transport** (server↔client, into atoms — the
`Watch*`/pull bridges). **Missionary owns the frame path** (one
`<world-snapshot` `m/latest`, `render.cljs:81-118`, sampled on RAF with an
`identical?` skip, `:121-124`). **The rt-node tree is the boundary object.**
Electric never enters the frame loop; the frame loop never touches server
names. Harmony arrives as a **shared contract** (the assembly's `:bind`
declaration + the request shape, §7), never as shared code.

Receipts (verified register; do not re-litigate): L15 (the gpu-mount corpse:
16,750 slots for 100 entities — DOM ownership semantics corrupt slot pools);
L16 (reorders enter the permutation regime; faces reorder); L8 fence (atomic
settle of Electric's DAG is untested; one `m/latest` world cannot tear
mid-frame); L13 (`try` is TODO in `e/defn` — the layer rendering AI-authored
faces needs error containment more than any other); the airlock (async left
of the atoms, synchronous right — the land renders on last-known truth
through a reconnect for free). And the soul reason, decisive even if every
platform fact were fixed: **faces must be data, so what renders them must be
an interpreter** — faces-as-Electric-programs would be code generation into
the compiled substrate, the Regime-2 gate crossed silently.

**The harmonization ladder** (each rung pre-named, D-001-gated):
1. Now: Electric as bridge; assemblies compile at wear-time to rect-tree builders.
2. This contract: Electric as the ONE generic artery (§7).
3. **Form-break-gated** (pre-registered in R3's sunset note): Electric incseq
   diffs as the invalidation signal for measured-safe shapes
   (change/append/tail-shrink); live chat is the pre-qualified first consumer.
4. **Form-break-gated** (L6 rebuild cost bites a worn face): point-writes
   consuming the six diff ops into the retained store, per L15's own
   prescription. Arrives with evidence, never as architecture.

Rungs 3–4 MUST NOT be built in this package. A session that finds itself
wiring incseq into the render side has left the contract.

## 4 · Grammar v0 — five keys, one guard

An assembly is pure EDN. Node forms (closed key set — anything else rejects):

- **prim node** `{:prim <kw> :props <map>? :children [<node>...]?}`
- **each node** `{:each [<kw>...] :template <node>}` — iterate the seq at
  path in the current data context; template applies per item with context
  descended to the item.
- **bind ref** `{:bind [<kw>...]}` — legal ONLY as a prop value (top
  position, not nested inside literal collections — nested binds reject;
  extension point). Resolves `get-in` against the CURRENT context.

ROAD's "five keys" = `:prim :props :children :each :bind`; this contract
additionally binds `:template` as `:each`'s required companion — the closed
node-key set is `#{:prim :props :children}` XOR `#{:each :template}`.

Envelope: `{:assembly/name <string, required> :assembly/grammar 0 (required)
:assembly/belief <string, optional> :root <node, required>}`. Unknown
NAMESPACED envelope keys are tolerated and preserved (the §8 schema adds
fields in Wave 2; old interpreters must not error-card new files). Unknown
NODE keys reject.

**Validation rules** (all at compile time, §5):
- V1 envelope shape as above; unsupported `:assembly/grammar` → error card.
- V2 node closed set; exactly one of `:prim` / `:each` per node.
- V3 `:prim` keyword must resolve in the registry — unknown prim → error card
  naming it.
- V4 **THE GUARD, mechanical form**: the assembly form may contain **no list
  and no symbol anywhere** (post-EDN-read walk). Keywords, strings, numbers,
  booleans, nil, vectors, maps only. Arrangement only — no conditionals, no
  expressions, no logic, ever. Computation lives in projections (real
  server-side Clojure, §7) or primitives (real cljc, §6). Anything that wants
  to be a program gets to be a real one, in the code lane.
- V5 `:props` map keys are keywords; values are EDN literals or bind refs.
- V6 `:children` is a vector of nodes; `:each` path is a non-empty vector of
  keywords; `:template` is one node.
- V7 template-node count per assembly ≤ 1,000 (compile-time cap; data-size
  caps ride the projection's page limits, not the grammar).

**Error-card semantics.** Validation failure never throws and never yields a
broken tree: `compile-assembly` returns a builder whose apply renders an
**error-card rt-node** — a valid, renderable tree carrying the assembly
name, grammar version, address, and the first ≤5 validation errors (each
with a path into the form). The error-card constructor is **built into
`face_assembly.cljc` and registry-independent** — a corrupted registry
cannot take the error path down with it. Malformed AI-authored assemblies
land as visible error cards beside the chat, never as a black screen (L13:
the render path has no try to catch them).

**Interaction (reserved now, built later):** rt-node already carries
`:actions` with innermost-first `dispatch-event` (`rect_tree.cljc:407-425`).
Assemblies will name actions by KEYWORD resolved through an action registry —
references, not logic, so the guard holds. v0 assemblies are read-only; no
`:actions` key in grammar v0 (rejecting it is V2 doing its job). Extension
point, D-008 item-5 pacing (UI gestures gated until spec'd).

## 5 · Interpreter — the two-stage law

Two pure functions in `face_assembly.cljc`; the caller owns all caching.

```clojure
(compile-assembly registry assembly-edn) → compiled
;; Pure. NEVER throws on malformed input (total function): validation
;; failures yield a compiled form whose apply renders the §4 error card.
;; Parses, validates (V1–V7), resolves prims against the registry, and
;; closes over the builder graph ONCE.

(apply-assembly compiled data-context view-ctx) → rt-node tree
;; Pure. No side effects, no atoms. view-ctx =
;;   {:view-instance <kw>  :address <the request address, §7>
;;    :geom {:viewport-w :viewport-h :font-size :char-advance
;;           :line-height :content-w :now-ms}}
;; (the trail-face geom precedent, editor_compute.cljs:377-385; :now-ms is
;; the honest server stamp, never the wall clock).
```

- **Wear-time** (face entry / assembly change): compile once.
- **Data-change-time**: apply builder × data-context → rt-node tree, inside
  the one `m/latest` — every existing pane re-derives its scene on data
  change; that is the correct shape here too (ROAD, corrected from v1's
  "compile once at mount").
- **Frame-time**: nothing. The existing RAF sample + `identical?` skip +
  keyed pool diff consume the scene like any pane. Interpretation cost lives
  on the data-change path, never the frame path.

**Δ1 carry (D-009).** `apply-assembly` stamps `(:view-instance, :address)`
from view-ctx into the produced tree root's `:data`. The D-009 H6-keyed scene
store does not exist yet (`view-instance` appears nowhere in src); carrying
the pair from birth makes Δ1 compliance a RENAME when the Δ3 store lands at
the face-2 contract, not a rework (trap T10).

**The pane scroll contract (PROBE check-b, cited at source).** The assembly
pane follows the face/camera convention, never the sidebar bake-in: the tree
is built at (0,0), **scroll-independent** (the flow does NOT watch
`!scroll-y`; baking scroll into the tree reintroduces per-wheel rebuilds and
forfeits R3's `identical?` skip); the root `:data` **declares
`:assembly/content-h`** (measured bottom-up by the walker) so the host wheel
handler clamps against it (the `:trail-face/content-h` precedent,
`scene.cljc:772`); the host routes the pane's scroll atom into the camera
pan (pan-y = −scroll-y) and clips at the pane slot. Content-height + 
scroll-independence is the pane's WHOLE scroll contract.

**Ids.** Deterministic: root id derives from `(view-instance,
assembly-name)`; child ids extend the parent id by structural path; an
`:each` item's segment is the item's `:id` value when present, else the
index. Index fallback is legal but counted (below) — reorders under index
ids churn the keyed pool diff (trap T6). A bound `:props {:id …}` overrides.

**Apply-report.** Apply attaches `{:assembly/apply-report {:items-without-id
n :binds-missing n}}` to the root's `:data`. Missing bind paths resolve to
nil (primitives render their own defaults — honest data absence renders as a
gap, never a crash) and are counted. The map must not lie about degradation
(the `:reconcile-basis-missing` precedent).

**Flow-layer caching discipline (W1-INT, binding on the mount):** the
assembly pane's flow follows the `<trail-face` shape exactly
(`editor_compute.cljs:368-418`): ONE `m/latest` over (layout, face-request
state, data-context, …); build once per input change, compare input VALUE
(never a hash — a collision freezes a stale scene forever); cache the scene
in a scene atom so `combined_text` flattens text ops from and `mouse`
hit-tests THE SAME object; any carry lives in a separate post-build atom
watched by NOTHING (trap T9); the scene is scroll-independent — scroll rides
per §10 SLOT-C. The mount point is the mode multiplexer
(`editor_compute.cljs:536-548`): one new mode, one new flow argument.

## 6 · Registry + the vocabulary

**Runtime form:** a plain map `{<prim-kw> → builder-fn}`, assembled at load
in `face_primitives.cljc` and passed to `compile-assembly`. No global
mutable registry; the map is a value (tests pass their own).

**Builder-fn interface (fixed — lane A stubs against it, lane B implements):**

```clojure
(fn [ctx props children] → rt-node)
;; ctx      {:id <node-id> :view-instance <kw> :address <addr> :geom <§5 geom>}
;; props    resolved props (binds already substituted; may contain nils)
;; children vector of already-built child rt-nodes (post-order walk)
```

Builders are pure `.cljc`, return `rect_tree.cljc` rt-nodes, and follow the
**measure rule**: a primitive computes its own `:w`/`:h` BEFORE returning —
`layout-children` positions children by their pre-existing bounds
(`rect_tree.cljc:112-113`) and `resolve-text-layout` writes wrapped lines
without updating `:h` (`:192`); a prose primitive that leans on
`:auto-height?` + `:text-layout` together produces zero-height cards (trap
T7; Step-0 check d). **Measure inside the primitive, arrange in the engine.**
Width flows down: builders default their width to `(:content-w geom)` unless
`:props :w` says otherwise; wrapping derives max-chars from width ÷
`(:char-advance geom)` (the existing `0.56`-derived advance — never a second
constant).

**v0 vocabulary (extraction and gap-fill, not invention):**
- Wrapped from `ui_primitives.cljs` (verbatim copies, G7-pinned): `:panel`
  `:panel-header` `:panel-content` `:panel-footer` `:panel-group`
  `:list-item` `:card` `:badge` `:divider` `:empty-state` `:scrollbar`.
- Wrapped from `trail_face/` cljc builders where the Outline face needs them
  (cards/lanes are proto-primitives; the extraction harvests, originals stay).
- **Genuinely new (two):** `:text-run` — prose block: **wraps ONCE via
  `wrap-line` and emits its own positioned text ops** (the
  `build-empty-state` pattern, `ui_primitives.cljs:189-242`), setting its own
  `:h` = wrapped-line-count × line-height (the measure rule made flesh).
  It must NOT ride `:text-layout`/`resolve-text-layout`: PROBE Finding 1 —
  `:text-layout` is not an `rt-node` constructor param, no shipped code sets
  it (the hook has never run in production), and riding it DOUBLES wrap cost
  (measured ≈2×, PROBE §a′) because the primitive must wrap anyway to
  measure. `:indent-rail` — the outline indent guide.
- `:stack` — the bare layout node (`rt-node` + `:layout` passthrough:
  direction/gap/padding/align — `:row` and `:column` both exist,
  `rect_tree.cljc:81`).

A missing primitive during Steps 2–3 is a cljc function away (zoom to
bedrock) — minted by gap-fill in the code lane, never by grammar growth.

**Persisted form (Wave 2, schema §8):** registry entries persist as
keyword + code ADDRESS (ns/var name, optionally blob-sha per D-003
address-don't-copy) — **never fn values, never serialized closures** (trap
T5; D-009's own note names registry entries "the first candidate" for
code-as-addressed-material). The runtime map binds keyword→fn at load; the
land stores only names and addresses.

## 7 · The artery — one generic face-pull

Replaces the per-face hand-wired capillary (`electric_flow.cljc:492-508` is
the before-picture: a `case` on `:face` with dedicated server calls — the one
real dis-harmony in the shipped substrate).

**Request shape** (client → server), one atom `!face-request`:

```clojure
{:face    <assembly identifier — name/kw>
 :address <material address — v0: a conversation object-key>
 :params  {:limit <n> :until-ms <ms|nil> …}   ; :until-ms = replay/scrub cut
 :epoch   <last-seen ingest epoch>}
```

**Server side:** `face_projection.clj` holds a **projection registry** — a
plain map `{<projection-kw> → (fn [request] → data-context)}`, plain Clojure,
unit-testable without Electric. Wave 1 registers ONE projection:
`:conversation` — conversation → turns → blocks-with-kinds over the block
kernel's EXISTING read surface (`river-page`, `read-common-material-for-source`,
`read-unit` — `block_distiller.clj:1305` carries the read-plan discipline).
**READ-ONLY: no new depots, no new topologies, no kernel edits** — the
projection module declares nothing; it physically cannot write (trail-view
gate-14 style; D-008 item 1). Scrub = the SAME projection with `:until-ms`
bounding the material — a bounded read; per-page cost is a function of page
size, never conversation length (the block-kernel G12 discipline inherited).

**Electric side:** ONE `FacePull` e/defn in `file_viewer.cljc` —
`(e/server (face-projection/serve (face-rt) request))` — and ONE watch loop
in `electric_flow.cljc`: `(e/watch !face-request)` → `FacePull` → `reset!`
ONE `!face-data` atom. **No face-specific dispatch anywhere in Electric** —
face dispatch happens server-side in the projection registry; the Electric
surface is generic and never grows per face (trap T8). Electric's role grows
with every face at the seam where it wins — by carrying more traffic, not
more code.

**Client glue** (`face_wiring.cljs`, S2): derives `!face-request` from face
state; splits nothing (the data-context arrives whole); mirrors the
ingest-epoch push → ≥1s debounced re-request pattern verbatim from
`trail_face/wiring.cljs:62-73` (INV-19: the epoch PUSH is the only re-pull
trigger; no polling anywhere). Touches NO server names (S1).

**Data-context:** the projection result + `:face/rendered-at-ms` (honest
server stamp — the `:feed/rendered-at-ms` precedent) landing in `!face-data`;
the §5 flow consumes it. The assembly's `:bind` paths are the data contract
between projection and face — harmonization as shared contract, never shared
code (§3).

## 8 · Assembly-as-kernel-object schema (FIXED now · IMPLEMENTED Wave 2)

Wave 1 persists nothing; assemblies are hand-written `.edn` fixtures. This
section fixes the schema so W1 code carries the right identity and
provenance shapes from birth, and Wave 2 realizes it without rework.

- **Object kind:** an assembly is **addressed material in the
  object-container ontology** — the authored `.edn` lands as a source (the
  markdown-ingest sibling watcher, ROAD Step 4), the validated assembly form
  is its derived material. NO new row types, NO new module (trap T11; the
  block-kernel §2 "two truths drift" ruling stands).
- **Fields:** name (Sid's handle) · belief line · `:assembly/grammar` version
  · **status ∈ #{candidate, worn, retired}** — a REVISABLE judgment riding
  OC revision/supersedes chains, never destructive edit · provenance
  (birthed-by: conversation address + author actor, `asserted-by`
  first-class — human-authored vs A2 machine-proposed visibly distinct,
  silver/gold per map-must-not-lie).
- **Lineage edges — EXISTING D-004 kinds only** (all verified registered,
  `relation_kernel.clj:58-72`): `based-on` (derived from another face) ·
  `new-direction` (fork) · `dead-end` (wearing killed it) · `elaborates`
  (refinement) · `supersedes` (displacement — registered 2026-07-05 as a
  stance kind; belief displacement is exactly what one face does to another)
  · `produced` (birthing conversation → face). **No enum edit is needed for
  this road.** `pairs-with` is the ONE pre-named candidate addition: it is a
  **named authorization item** requiring Sid's explicit countersign on a
  used-form break — never a silent enum edit (D-004 closed enum; the
  block-kernel 3-kind authorization is the precedent).
- **Import key:** if the W2 watcher mints a new family (working name
  `imp:asm:`), its `extract-object-key` routing branch (or the handled
  prefix it rides) **plus a foreign-read routing gate is a NAMED W2
  deliverable** — the skill rule; this exact latent class fired twice
  (code-atom G-F2 `imp:clj:`, block-kernel F2 `imp:sense-block:`).
- **Wearing events:** append-only log `(face, worn-at-ms, wearer, address
  worn over)` — the desire-path instrument that decides which face earns
  primitive investment. W2 realization; back-arrow holds (authoring streams
  INTO Rama; the UI reads Rama).
- **Registry entries as objects:** §6 persisted form.

## 9 · Traps ledger (implementers cite trap numbers in code comments)

| # | Naive choice | Concrete failure | Ruling |
|---|---|---|---|
| T1 | Faces as Electric components (v1's aim) | L15 corpse: DOM-shaped mount semantics over GPU pools left 16,750 active slots for 100 entities; L16 reorder economics; L13 no `try` in the layer that most needs error containment; faces stop being data — the Regime-2 gate crossed silently | §3 seam law; pure interpreter over rect-tree; Electric = transport only |
| T2 | "Just a little `if`" in assemblies | Inner-platform death: conditionals → expressions → a worse Clojure; A2-written assemblies become unauditable logic; JVM goldens stop being total | §4 guard, mechanical form: no lists, no symbols, anywhere |
| T3 | Compile per apply (or per frame) | Parse+validate+resolve on every data tick; validation errors surface at frame time — wrong layer, too late | §5 two-stage law: wear-time compile, data-change apply, frame-time sample only |
| T4 | Throw on malformed assembly / unknown prim | One bad AI-written assembly takes down the render loop (no `try` upstream, L13); the land goes black instead of showing what's wrong | §4 total functions: error-card rt-node, registry-independent constructor |
| T5 | Fn values in persisted registry/assembly | Rama can't store closures; serialized fns rot across builds; provenance dies | §6/§8: persisted = keyword + address; runtime map binds at load |
| T6 | Item ids from index | Reorder shifts every downstream id → keyed pool diff churns the whole list; identity-keyed hover/selection latches the wrong row | §5: item `:id` drives node id; index fallback legal but counted in the apply-report |
| T7 | Walker leans on `:auto-height?` + `:text-layout` for prose | Zero-height prose cards stacked at y≈0: `resolve-text-layout` never writes `:h` (`rect_tree.cljc:192`); `layout-children` reads pre-existing bounds (`:112-113`); `resolve-layout` is top-down (`:194-203`) | §6 measure rule: measure inside the primitive, arrange in the engine |
| T8 | Per-face transport (copy the trail wiring per face) | N faces × (request atom + data atom + wiring file + electric_flow case branch) — every new face pays plumbing, not design; the exact dis-harmony this framework exists to fix | §7: ONE generic pull; face dispatch server-side in the projection registry |
| T9 | Per-build output written into a watched input | Rebuild/re-pull feedback loop (trail-room trap-13, fired and fixed 2026-07-05) | §5: scene cache + carries in atoms watched by NOTHING; interpreter itself pure |
| T10 | Ad-hoc scene keying (skip Δ1) | The D-009 Δ3 H6-keyed store lands at face-2 and every assembly pane needs a rekey/rework | §5: `(view-instance, address)` carried from birth; compliance = a rename |
| T11 | Assemblies as a new row type / new module | Duplicate ontology 2,600 lines from the real one — the "two truths drift" failure the land already ruled against (block-kernel §2) | §8: assemblies ride the OC ontology + existing relation kinds |
| T12 | Projection re-reads raw jsonl / re-redacts / adds an index | Second truth; redaction divergence (block-kernel R1/R4 law); silent kernel scope creep | §7: query APIs only; read-only module; no depots, no topologies, no kernel edits |
| T13 | Hash-compare for scene-rebuild skip | A hash collision freezes a stale scene forever — "state stuck masking future truth" (found by the WP-B2 falsification pass) | §5: compare the input VALUE (`!last-trail-struct` precedent) |

## 10 · PROBE slots (FINALIZED 2026-07-11 — PROBE.md landed same day)

Pre-registered defaults + activation conditions kept verbatim below (the
pre-registration is the point); each slot carries its dated outcome. All
measurements are JVM shape-proxies (JDK 21, Ryzen 9 9900X); browser
confirmation of the constants rides W1-INT. Fable re-ran the probe suite
independently: 25/25 green, numbers reproduce within ~3%.

- **SLOT-A · apply cost** at ~hundreds of nodes on data change. Default
  ruling: single-digit ms expected (sidebar/trail precedent: pure scene
  rebuilds measure fine) → the §5 build-once discipline stands as specified.
  Activation: if the measured number is ≥ tens of ms at ~300 nodes, §5 gains
  a pre-named per-item memoization provision — only then.
  **Outcome (PROBE.md):** 5.0 / 12.1 / 24.4 ms median @ 201/501/1001 nodes,
  ~linear; arrange (`resolve-layout`) <0.3 ms — the walk dominates, ~half of
  it wrapping. ~7–8 ms at ~300 nodes → **NOT activated**; §5 stands, no
  memoization provision. Budget language: low single-digit ms at
  conversation scale, ~12–25 ms at 1000 nodes, data-change not per-frame.
- **SLOT-B · wrap-line cost** on long prose at conversation scale. Default:
  eager wrap at build (inside `:text-run`). Activation: if wrap dominates
  apply cost, `:text-run` gains a per-(text,width) wrap cache — only then.
  **Outcome (PROBE.md):** wrap-line 15.5 µs/block, 7.7 ms/500 prose blocks.
  Default SHARPENED: `:text-run` wraps ONCE and emits its own positioned ops
  (`build-empty-state` pattern, T7's measure rule) — riding the
  `:text-layout` engine hook doubles wrap cost (measured ≈49% saving
  one-wrap) AND that hook is dead code: not an `rt-node` constructor param
  (`rect_tree.cljc:45`), attached by nothing shipped, `resolve-text-layout`
  has never run in production. One-wrap apply is not wrap-dominated → cache
  **NOT activated**.
- **SLOT-C · scroll convention** inside an assembly-hosted pane. Default:
  scene scroll-independent, scroll rides the camera (pan-y = −scroll-y), the
  trail-face convention (`editor_compute.cljs:352-354`) — flows do NOT watch
  `!scroll-y`. Activation: if the probe demonstrates per-pane clip+offset is
  required (nested scrollables), W1-INT adopts the probe's convention and
  G15's wearing check exercises it. Either way the convention is recorded in
  the INT artifact.
  **Outcome (PROBE.md):** the probe independently derived the default from
  source (`renderer.cljs:41` camera pan; clamp `scroll.cljs:96-101`); no
  nested-scrollable need demonstrated → **default stands**. The pane's whole
  scroll contract: declare content-height in a blessed root `:data` key
  (walker measures it bottom-up) + stay scroll-independent — never bake
  `scroll-y` into the tree (per-wheel rebuilds; forfeits R3's skip).

PROBE's remaining contract-changes were already independently present in
v1 — `:template` named in §4's closed key set; cljs-only extraction scope in
§11's G14 split — two blind runs converging on the same facts. No further
amendment from the probe.

## 11 · Acceptance gates

JVM tests unless marked review-time/INT. Every one-liner carries its
carve-outs (skill rule — the G12 lesson). Gates green in-context is each
lane's definition of done; the wave ends with ONE serial suite + ONE batched
falsification-by-class + ONE Fable gate (the 2026-07-05 delivery-mode
ruling).

**Lane A — interpreter + goldens**
- **G1 golden walk:** the Outline assembly fixture + committed data fixture →
  `compile-assembly` → `apply-assembly` → rt-tree golden-equal (committed EDN
  snapshot; regen is an explicit diff-reviewed act — block-kernel goldens
  discipline).
- **G2 the guard bites:** assemblies containing (a) a list anywhere, (b) a
  symbol anywhere, (c) an unknown node key, (d) both `:prim` and `:each` on
  one node, (e) a nested bind — each REJECTS at compile into an error-card
  builder; one test per class.
- **G3 error-card totality:** malformed assembly AND unknown-prim assembly
  each apply to a VALID renderable rt-tree (`tree->rects` succeeds) carrying
  name + address + ≤5 errors; nothing throws; the error-card path is
  registry-independent (test passes a corrupted registry).
- **G4 two-stage + Δ1 carry:** apply is pure — same (compiled, data, ctx) →
  equal trees; two different data-contexts through ONE compiled builder →
  correct differing trees with no recompile; root `:data` carries
  `(:view-instance, :address)` and the apply-report.
- **G5 `:each` discipline:** nested each (turns→blocks) descends context;
  item `:id` drives node ids (reorder test: ids travel with items); missing
  ids fall back to index AND are counted in the apply-report.
- **G6 `:bind` discipline:** paths resolve against the current (descended)
  context; missing path → nil prop + counted; bind at node position rejects.

**Lane B — vocabulary**
- **G7 mechanical builder fidelity** (adopts the Trunk-5 STANDING PROPOSAL):
  a source-form diff test reads `ui_primitives.cljs` and
  `face_primitives.cljc`, compares each copied builder's defn form as DATA,
  and fails on ANY divergence not on the per-builder allowlist of named,
  reviewed extensions (carve-out: the ns form and reader conditionals are
  exempt; the allowlist starts empty). Hand-mirroring discipline
  demonstrably does not hold — this gate replaces it with a diff.
- **G8 the two new primitives measure:** `:text-run` over long prose at a
  fixture width wraps ONCE via `wrap-line`, emits its own positioned text
  ops, and its returned `:h` = wrapped-line-count × line-height (the T7
  regression; carve-out: `:text-layout`/`resolve-text-layout` must appear
  NOWHERE in the vocabulary — the dead hook, §6/PROBE Finding 1);
  `:indent-rail` golden. Both goldens include a
  `resolve-layout`-then-`tree->rects` pass proving non-zero, non-overlapping
  stacked bounds.
- **G9 JVM purity:** the entire vocabulary + interpreter + goldens run on
  the JVM with no browser (the suite existing and running IS the gate; no
  `js/` outside reader conditionals).

**Lane C — projection + artery**
- **G10 projection receipt (full-real-corpus, ASSERTED):** the
  `:conversation` projection over the ingested `7c80ce2a` corpus asserts —
  not prints — completeness against the block kernel's durable state: every
  river block for the conversation appears exactly once across the returned
  turns (247-block corpus receipt baseline), turn order = river order,
  per-block kind present, truncation signalled when `:limit` cuts (carve-out:
  debris rows are NOT in the face data-context by design — the projection
  serves river; the assertion counts them as excluded, never as missing).
- **G11 bounded scrub:** `:until-ms` cuts are prefix-consistent (T1 < T2 ⇒
  result(T1) is a prefix of result(T2) at block grain) and per-page cost is
  bounded by `:limit`, never conversation length (read-plan metadata
  asserted, the river-page discipline).
- **G12 read-only by construction** (review-time): `face_projection.clj`
  declares no depots, no topologies, performs no writes, and reads ONLY
  named query APIs (`river-page` / `read-common-material-for-source` /
  `read-unit`) — no PState paths, no new indexes (carve-out: test-only
  validation readers exempt per T7-block-kernel precedent).
- **G13 generic artery** (review-time on the electric diff + one unit test):
  ONE request atom, ONE `FacePull` e/defn, ONE data atom; NO face-keyword
  dispatch anywhere in `electric_flow.cljc`/`file_viewer.cljc` (dispatch
  lives in the server projection registry, unit-tested as a plain fn:
  request map in → data-context out); client glue touches no server names
  (S1/S2).

**W1-INT — integration (orchestrating session)**
- **G14 worn-UI structural-equality falsifier** (the fixture-fidelity gate
  at assembly grain), two instances:
  (a) **JVM, in the suite:** a named trail-face cljc sub-tree (criterion:
  exercises ≥1 `:each`, ≥2 nesting levels, text + rects) re-expressed as an
  assembly → interpreter → rt-tree **structurally equal** to the hand-wired
  cljc builder's output over the same committed fixture — ids, bounds,
  styles, text included (carve-out: the slice is a bounded sub-scene; the
  timeline's carry/move-chip machinery is computation and stays in code per
  the §4 guard).
  (b) **Live, at INT:** in the dev app, the sidebar list built by
  `build-sidebar-tree` and the same list through an assembly are compared
  live (console equality over the resolved trees at a fixed state); result
  recorded verbatim in the INT artifact (carve-out: live check, not a
  committed test — `sidebar.cljs` is cljs-only; the JVM instance (a) is the
  suite's permanent falsifier).
- **G15 the wearing:** the Outline face renders the real `7c80ce2a`
  conversation through the full path (projection → artery → data-context →
  interpreter → `<world-snapshot` → GPU) in the dev app; a bottom-bar
  command picks the conversation; scrub via `:until-ms` re-request works;
  screenshot lands in the INT artifact for Sid. Every lack the face exposes
  is LOGGED as ordered data work (D-005), not silently patched.
- **G16 wave close:** full serial suite green + the ONE batched
  falsification-by-class (fresh Opus subagents) + the Fable gate
  (falsification pass per CLAUDE.md protocol) + SLOT numbers recorded.
  Commits per current package practice (code and docs separate; code commit
  on close).

## 12 · Non-goals (each an extension point, none a void)

1. **No Electric-native primitives** — §3 law; U4/U5 stay recorded in the
   electric-docs skill for future Electric-LEVEL work, off this road.
2. **No Gap-3/rung-3 wiring** until a worn face breaks against rebuild cost —
   the ladder is evidence-paced; live chat is the pre-qualified first
   consumer when it fires.
3. **No DOM face layer** (D-009's island engine stays foreclosed).
4. **No logic in assemblies, ever, in v0** — the §4 guard; SCI-in-assemblies
   waits for a worn face to demonstrate need, and enters (if ever) as a
   REGISTERED primitive boundary, not grammar growth.
5. **No drag-and-drop builder UI** — the editor and the AI are the authoring
   surfaces (the zoom-100 editor is already the assembly editor).
6. **No general no-code-platform ambitions** until ≥3 faces share the
   vocabulary (D-001: the framework precipitates from worn faces).
7. **No multi-user sharing machinery** — assemblies are provenanced values;
   transport is a later problem, designed-for, not built.
8. **No `:actions` execution in v0** — reserved keyword-reference shape
   recorded in §4; enters at D-008's read→write milestone for UI gestures.
9. **No W2 work in W1**: no `.edn` watcher, no kernel objects, no wearing
   log, no `imp:asm:` — schema fixed (§8), build gated on the W1 exit.

## 13 · Input manifest (for the D-006 counterfactual probe)

Given to any model reproducing this contract: `decisions.md` (D-001–D-010 +
the Trunk-5 STANDING PROPOSAL note in D-006) · `ROAD.md` v2 (all) ·
`vision/LOG.md` 2026-07-10 entry · `rect_tree.cljc` (all) ·
`ui_primitives.cljs` (all) · `trail_face/wiring.cljs` (all) ·
`editor_compute.cljs` :280-420, :520-548 · `runtime/render.cljs` :81-151 ·
`electric_flow.cljc` :455-604 · `file_viewer.cljc` :100-210 ·
`relation_kernel.clj` :50-75 · `block_distiller.clj` :1305-1330 ·
`build/sense-line-mvp/block-kernel/CONTRACT.md` §2 §8 ·
`build/relation-kernel/CONTRACT.md` §10 · the `/work-package` SKILL.md ·
CLAUDE.md (Missionary laws + hard rules). Probe question: "design the
faces-as-assemblies framework contract for this substrate."

## 14 · Handoff

- **Wave 1, three parallel lanes** (fresh contexts; Opus 4.8, Codex 5.5
  fast-mode a valid alternative per the 2026-07-06 pool ruling), fences per
  §2, prompts at `docs/sessions/framework-w1-lane-prompts-2026-07-11.md`
  (⟨§?⟩ refs pinned by this contract): **A** interpreter + goldens (§4, §5,
  gates G1–G6) · **B** vocabulary (§6, G7–G9) · **C** projection + artery
  (§7, G10–G13; sole owner of `electric_flow.cljc` + `file_viewer.cljc`;
  boots `/rama` + `/electric-docs` before code).
- **W1-INT** (orchestrating session; Fable may implement directly per the
  2026-07-05 (e2) ruling): registry wiring, the mount (mode + flow + atoms +
  command per §5's touch-list), G14–G15, then the wave's ONE batched
  end-gate (G16). Per-lane falsification is NOT run — QC consolidates at the
  end, sized to catch contract-text-error classes (the honest ledger says
  every Fable contract so far carried at least one).
- **Stop clauses** (escalate to decisions.md Open Questions with options +
  recommendation; never improvise): a Step-1/2 face inexpressible without
  logic (grammar change = contract amendment, never a silent extension) ·
  the projection cannot meet G10/G11 without a kernel edit or new index ·
  G14(a) structurally impossible for every qualifying slice · any
  relation-kind or `imp:` need surfacing early · any two binding docs in
  genuine conflict.
- **NOT without Sid:** wave dispatch (spend) · countersign of the two §15
  log entries · `pairs-with` or any relation-kind addition · Wave 2 dispatch
  · anything touching main.
- **After G16 green:** Wave 2 (Steps 3–4: design-round faces; assemblies as
  kernel objects per §8; the watcher; wearing log; machine-cut timing rides
  W2 per D-005) under its own phase of this contract — §8 is its spec floor.

## 15 · Two decision-log entries — DRAFTED for countersign (not entered)

Per the opening prompt these are drafted here for Sid's countersign; on his
word they enter `decisions.md` verbatim as new entries (log mechanics: they
enter as PROPOSED-with-recommendation; his countersign closes them).

**D-011 (draft) — The middle regime: the land's furniture as the land's data.**
The assembly layer is a THIRD regime between D-003's two: neither Regime-1
external code (git-authored, addressed) nor gated Regime-2 self-written
code. Assemblies are the land's furniture described in the land's own DATA —
arrangement only, no code enters Rama, the compiled substrate stays entirely
git-authored (primitives, interpreter, projections are Regime-1 artifacts).
The arrangement-only guard (framework CONTRACT §4) is the regime boundary's
enforcement: anything that wants to be a program gets to be a real one, in
the code lane. Machine-written assemblies ride D-008 A2 (provenance-first
observations through the lawful worker path) and land validated + error-
carded, never executed. Evidence: the shipped substrate already splits
exactly this way (pure-fn builders vs data-shaped scene composition —
`rect_tree.cljc`'s own header records the desire path); every live-medium
system converged on structure-as-data over vocabulary-as-code (ROAD §
invariant 2). Consequence if countersigned: "no code in Rama" stays a hard
law with a precise meaning — keywords and addresses persist, fn values never
do (CONTRACT §6/§8); Regime-2's gate is untouched.

**D-012 (draft) — The Regime-2 self-hosting test, formulated.**
Answering the open question carried since D-003 ("Regime 2 self-hosting test
formulation"). The test: **a design conversation produces a usable view
without leaving the land and without hand-translation** — conversation →
proposed assembly (A2 observation, `based-on` edge to the birthing
conversation) → validated + rendered beside the chat → worn by Sid — with no
human transcription step between the conversation and the wearable face.
ROAD Step 7B is the first worked instance; wearing (not argument) decides
survival. Passing the test for ARRANGEMENT (faces) does NOT open Regime 2
for CODE (primitives, projections): minting vocabulary in-land remains gated
on its own form-break (ROAD Horizon: hot-reload covers the mint loop,
probably indefinitely). The test's scope is deliberately the middle regime's
ceiling — it proves the loop (design conversation → land object → daily use)
closes; Regime-2-for-code would need this test passed AND a used form
breaking against the code lane's hot-reload speed.
