# IMAGE ATOM — Package 2, wave 1 (CONTRACT)

**Status:** CUT 2026-08-05 (Fable, direction-born authorship per the
work-package skill). Validation R1 (fresh, default-fail) returned FAIL —
five recut-class findings (store-lane integration, pick dispatch, color
regimes, G4 vacuity, bank mechanics), preserved in
`IMAGE-ATOM-CONTRACT-VALIDATION-R1.md`; RECUT in place same day consuming
all R1 findings (map: `IMAGE-ATOM-RECUT-LEDGER-R1.md`). Validation R2
(fresh, default-fail) returned FAIL — six findings on the recut layer
(JVM-tier gap on the payload leg · no in-allowlist presentation-encode
home · per-binding batching inexpressible via the pool helper · golden
assertion without code referent · accounting fork blocking the append
road · Cg-isolation missing from the parity probe), preserved in
`IMAGE-ATOM-CONTRACT-VALIDATION-R2.md`; SECOND RECUT in place same day
(map: `IMAGE-ATOM-RECUT-LEDGER-R2.md`). Validation R3 (fresh,
default-fail, W1-conformance duty) ran CONCURRENTLY with the one external
Codex cycle (five narrow fixes applied in place; four escalations); the
two instruments CONVERGED and were merged per the parallel-authors law
into `IMAGE-ATOM-RULING-PACKET.md` (Q1–Q7). THIRD RECUT executed
2026-08-05 by the fresh Fable ruling session per its Q1–Q7 rulings (map:
`IMAGE-ATOM-RECUT-LEDGER-R3.md`). **BINDING as of this recut** — the
external feedback cycle is SPENT, no further validation rounds run;
implementation is OPEN and the FULL-tier gate carries the remaining risk.
**Position:** the first new atom admitted through the W2-B seam — the seam's
first falsifier (W1 §9.2 gate 5: "Image/path registration is the first
falsifier of that seam, not permission to add another central draw branch").
**Register:** the FULL-BREADTH CAMPAIGN LAW (`ENGINE.md` §0) governs — this
wave is a dependency boundary, never a scope gate; "minimal" below describes
seams, never the capability obligation. Contracts `W1.md` O/G/M/C bind every
descriptor and receipt named here.
**Felt gate:** NONE in this wave, by hard join — "the durable artery before
any Package-2 felt activation" (`ENGINE.md` §0). The atom lands DARK and
machine-gated; its felt activation is a STAGED OBLIGATION that joins the
durable-authoring-artery wave after Sid's studio-custody capability ruling.
This staging is recorded, not waived. A SECOND staged obligation rides
beside it (ruling Q4): the PRODUCT frame-loop join receipt — real
`draw-frame!` frames carrying real store image entries under a hand — is
owned by the felt wave; this wave's arrangement-level truth is machine-only
(the §7 synthetic-frame verifier receipt).

Sequencing fact at cut: SEAM-STEP1 code landed `5f55cf5`; shaping-correction
landed `e577311`/`6887ef1`/`c0e9bbb` with Sid's lived acceptance (PASS/PASS).
Tree clean. This package's allowlist has NO overlap with any in-flight work.

---

## 1. Purpose and consumers

Admit `:render.family/image` as a full citizen of the rendering engine:
decode · upload · color management · mipmaps · atlas/bind-group strategy,
REGISTERED through the W2-B scene tape as pipelines/batches. The W2-B gate
sentence is the spine: **adding the image family never adds a hand-positioned
branch to the central frame body** — the executable fence proves it.

Consumers, in order:

1. **The W2-B seam itself** — this wave is the proof that the registration
   law holds for a family that did not exist when the seam was built.
2. **The verifier bank** — image golden cases join the permanent W0-A bank;
   the pick-parity probe extends to the image family.
3. **Package-2 successors** — the tessellated/path atom rides the admission
   road this wave paves; the connector/chrome slices consume image material
   (CH-02 crop chrome; CH-12 eyedropper reads composites containing images).
4. **The felt activation wave (post-artery)** — Goal 2's practice ("point at
   an outside component… draw the equivalent in-land", LOG 2026-07-31) needs
   reference images IN the world. NOTE: the "HIG reference corpus" has no
   on-disk referent today (verified 2026-08-05: zero files, zero manifest) —
   this wave delivers the CAPABILITY that lets reference images enter; any
   actual corpus ingest belongs to the Goal-2 pilot, not here.

## 2. Non-goals — each an extension point, never a void

- **No path/tessellated atom** (next wave; D2=A contract ready in `W0-C.md`
  §6). Path-shaped crops are refused with it: day-one crop authority is a
  RECT in image-local space.
- **No mask/group-effect composition** (W4 owns offscreen groups, masks,
  group opacity, backdrop). The image family declares its `:visibility`
  through the existing shared clip reference only.
- **No moving sampled media** (video/GIF) — registered demand (`W0-C.md` §3
  finding 3), lands with W4's clock. The material schema carries an explicit
  `:image/time` extension point that this wave REFUSES (validator rejects).
- **No authoring surface, no felt gate** — no paste/drag/URL ingress by hand,
  no ground.cljs or face_primitives.cljc edits. Fixtures and tests construct
  image material directly.
- **No durable-bytes store** (Rama blob storage). See §3 — the D7 seam.
- **No HIG corpus ingest** (§1.4 above).
- **No scene-color default flip** — `use-persistent-render-target?` stays
  `false`; `:scene-color-enabled?` default stays `false`. The candidate seam
  may be ENABLED inside verifier/test fixtures to prove C-receipts; product
  boot behavior is untouched.
- **No text-road changes** — MSDF stays RED (47/regime), Slug untouched,
  no retirement receipt.
- **No golden byte changes** — the 21 existing PNGs remain byte-identical;
  the bank APPENDS image cases (§8).
- **No alpha-aware pick** — day one pick is the geometric interior of the
  crop-quad. Alpha-aware picking is a lawful, named extension point
  (Contract G §4.3: "an explicit pick policy with its own deterministic
  sampling/mipmap rule") that a later contract may open.

## 3. The material story — the D7 note (binary-assets-as-material)

DISCOVERIES R5 armed this tripwire: "binary-assets-as-material → note before
the image atom." This section is that note; it binds this wave and seams the
next.

**Identity.** An image SOURCE is identified by the sha256 digest of its
encoded bytes plus a declared color tag. Legal ingress tags:
`#{:srgb :embedded-profile}`. Untagged sources are REFUSED at ingress
(`:reject-untagged`) — Contract C §8.1: "Untagged RGB is invalid at family
admission." `:embedded-profile` means the decode step (browser
`createImageBitmap`) performs the declared conversion to sRGB — that
conversion IS the ingress transfer and is receipted (G6), never assumed.

**Data-resolution (same section as identity, per the framework-W2-F1 rule).**
Consumers resolve a source through ONE registry API in the new
`image_material.cljc` namespace: digest → `{:bytes-route :color-tag
:width :height :ingress-receipt}`. Day one the bytes-route is a committed
fixture asset (content-addressed filename under
`test/app/fixtures/render_engine/images/`) or a caller-supplied
ArrayBuffer whose digest is verified at ingress — never trusted from the
caller (M5). No static code map from names to textures.

**Material vs instance.** Image MATERIAL = source digest + color tag +
intrinsic dimensions + revision + provenance (Contract M template). Image
INSTANCE = placement quad (via the W2-A affine transport — never a second
transform path), crop rect in image-local space, opacity/blend references,
pick policy. A crop change is an INSTANCE edit; replacing source bytes mints
a NEW material revision (transition story: the digest IS the revision key
for source bytes; derived resources key on digest + algorithm-version +
regime, Contract G §4.1 derivations law).

**The durable seam (staged, explicit).** Where image bytes live DURABLY —
Rama blob store vs content-addressed asset route vs external reference — is
the artery wave's decision, joined to Sid's studio-custody ruling. This wave
builds the law so that decision plugs in without rework: everything
downstream of the registry API is digest-keyed, so ANY durable bytes-route
that can answer "digest → bytes" slots in without touching the family,
pipelines, or receipts. Reversal cost of deferring: zero rework (the seam is
the registry API); cost of NOT deferring: the artery decision is Sid's and
is not blocked by mechanism work (`ENGINE.md`: "dark mechanism work may
proceed meanwhile").

**Reference input.** The studio archive holds a 247-line prior draft
(`build/studio/archive/untracked/src/app/client/substrate/image_material.cljc`)
covering ingress tags/refusal, mip math, shelf atlas, instance layout, and
per-instance scene entries. Per Sid's archive ruling it "re-lands from the
archive under its own contract, never wholesale": the implementer MAY read
it as reference and MUST re-derive every law from THIS contract + `W1.md`;
no wholesale copy; divergences from the draft need no justification —
convergences with it need re-verification against W1, not provenance.
Named refused shape (R3 A6): the draft's `image-scene-entries` fn mints
per-instance TAPE entries — exactly the design R1 F1 refused and §7
replaced with the pool-lane pattern; converging on it is a defect, not
provenance.

## 4. Placement ruling + reversal cost

- **Pure laws** (validation, ingress receipt shape, mip math, placement
  tiers, atlas allocator, instance packing, entry emission): NEW
  `src/app/client/substrate/image_material.cljc` — `.cljc` so every law is
  JVM-testable without a GPU. Why not in the renderer: the tape's law
  (scene_tape.cljc:1-8) is that citizenship data is GPU-free; why not in
  scene_tape.cljc: the tape is the REGISTRY, family-specific law lives with
  the family (precedent: per-family geometry defs live in the tape only as
  descriptors; image's richer resource laws would bloat it).
- **Family registration**: `scene_tape.cljc` — `image-geometry` descriptor +
  `registration` + the id in `family-ids` (:10-17). The registry comment
  says exactly this: "Adding image/path changes this registry."
- **GPU systems** (pipeline, bind groups, texture upload, mip generation,
  batch producer): `renderer.cljs` — one `init-image-system` beside the
  existing system constructors (`init-rect-system` :681,
  `init-msdf-text-system` :866, `init-slug-text-system` :930,
  `init-text-system` :995, `init-shadow-system` :1118), one
  producer registered in `frame-family-registry` (:2170-2190). The load-time
  coverage check (:2192-2202) forces exact family coverage — the registry
  edit and the tape edit land together or nothing boots.
- **Store lane**: `rect_tree.cljc` (the `tree->images` walk) +
  `scene_store.cljc` (`flatten-ops`/`stamp-ops-container` `:images` key) +
  `scene_runtime.cljs` (payload derivation + count/order maps — a NAMED
  deliverable, R1 F1) — §7. The maintained view is untouched.
- **Fence**: `test/render_engine/verify_scene_tape_fence.mjs` — the
  `families` array (:56) gains the image id; the seeded central-branch
  self-test must still reject.
- **Verifier**: `verifier.cljs` + `run_verifier.mjs` + the goldens bank —
  §8.
- **Budget truth**: `gpu_budget.cljs` `register-texture!` (:152) and
  `replace-texture!` (:194) both price level 0 only today; with real mip
  chains both roads must price the full chain (a budget-truth fix,
  allowlisted — trap T11).

**Reversal cost:** LOW. The registration seam is append-only; removal =
delete the registry entries + fence id + producer + namespace. No schema
migration (no durable writes in this wave), no transform-path debt (rides
W2-A slots), no golden debt (bank rows are append-tagged by family).

## 5. Admission — the O/G/M/C instantiation

The family registers with `validate-family!` passing fail-closed (no
grandfathering, no absent policies). The binding descriptor choices:

**Contract G geometry descriptor** (template W1 §4.1, family row W1 §4.9
"image / moving sampled media"):
- `:authority {:kind :image-quad ...}` — the crop-quad in image-local space:
  intrinsic pixel rect ∩ declared crop rect, placed by the instance's W2-A
  affine. Classification is tri-state on that quad after the declared
  inverse transform (W1 §4.2.1).
- `:classify` — `:fill-rule :not-applicable`, `:boundary-rule :explicit`
  (quad edge; classification is tri-state — inside/boundary/outside — and
  hit-ness AT the boundary is decided by `:pick`'s half-open law below;
  Contract O resolves overlap by reverse tape order).
- `:coverage` — `:geometry-operator :aa-filter`,
  `:boundary-relation :isocontour-0.5` FOR THE QUAD EDGE (Cg). The declared
  `:aa-filter` OBLIGES a ramped quad edge: at every tested zoom regime the
  rendered boundary must produce coverage samples strictly inside the
  decisive band, so the parity probe has decisive pixels to compare (G4's
  floors depend on this; a hard non-AA edge that yields zero boundary
  samples is a Cg defect, not a vacuous pass — R1 F5). Texture alpha
  is a DECLARED `:visual-factors :texture-alpha` entry contributing to Cv
  only — W1 §4.3: "an image's transparent texel… cannot be misreported as a
  geometry failure." Receipts isolate Cg with opaque reference paint before
  testing composed Cv. `:tie-token :half`; byte-128 through `rgba8unorm`
  stays the `:half` tie (W1 §4.4), never an inside vote.
- `:time-sample :none-static` — the §4.9 "time sample declared" obligation
  made machine-visible in the descriptor itself (R3 A1); the `:image/time`
  material extension point stays validator-REFUSED per §2.
- `:pick` — `{:policy :interior :boundary :half-open-interior :hit-slop
  {:metric :screen-px :radius 0.0} :owner <slot vi + image node address>}`.
  Geometric only (§2 refusal of alpha-aware). **Boundary law (truth-pinned,
  ruling Q3):** the product read is half-open — min edges inclusive, max
  edges exclusive (`rt/hit-test`, rect_tree.cljc:372-373); that IS the
  declared boundary rule. Hit-slop is 0.0 — no slop path exists this wave
  (`hit-test` has no slop input); G4 records that no slop path ran (W1
  §4.7's "receipts report which path won"). Closed exact-boundary
  semantics and non-zero screen-px slop are NAMED extension points for a
  later pick contract. The candidate probe's boundary truth is pixel-level
  (coverage with `:half` byte-128 ties), never point topology.
  **Product-pick route, declared (R1 F3 · ruling Q3):** the PRODUCT read
  is the existing store route — the image node is an rt-node whose rect is
  hit-tested in container-local space by `rt/hit-test` through
  `scene_store.cljc/pick`'s inverse-affine (rect_tree.cljc:359;
  scene_store.cljc:305-328). **For the image family the product read and
  the declared candidate geometry are EQUAL, not divergent — equality by
  construction:** under W2-A the container transform is the ONLY transform
  (per-container `:transport-slot`; `rt-node` carries NO per-node
  transform key — rect_tree.cljc:12-28, a §11 pinned fact), so the
  container-local AABB test IS the transformed-quad interior test. The
  equality's precondition is guarded by trap T14: introducing a transform
  key on any rt-node payload breaks the equality and requires a new pick
  contract — the fence and a G8 key-set assertion fail loudly first. G4's
  overlap receipt asserts AGREEMENT between the product read and the
  candidate quad classification (no divergence sentinel exists for this
  family — the migrating text family's recorded divergence is migration
  debt, never admission precedent, W1 §1/§2/§8.2-closing). **Owner
  route:** the slot entry's pick owner stays vi; every image rt-node
  REQUIRES `[:data :address]` (the semantic instance route) — an image
  rt-node without an address is REFUSED at flatten time (`tree->images`
  fail-closed, G8 fixture) — so `deepest-addressed` returns the image
  node itself, never an ancestor; G4/G8 assert the returned address is
  the image's own. **A central `:geometry`-keyed pick branch in
  `scene_store.cljc/pick` is REFUSED** (O6; the fence's spirit extends to
  pick — the fence gains the executable forbid, G11) — no new pick
  dispatch mechanism is introduced in this wave.
- `:derivations` — `[{:kind :mips ...} {:kind :atlas ...}]`, each keyed by
  source digest + algorithm-version + regime (deterministic reconstruction
  and invalidation, W1 §4.1 closing law).
- `:regimes` — a COMPLETE non-gapped partition of legal zoom `[0.01, 1000]`
  (Q2 binds every family; W1 §9.2 gate 4), with the floor-default `[0.1, 8]`
  reported as its own ordinary regime — three bands minimum:
  `[0.01,0.1) · [0.1,8] · (8,1000]`. Every band carries ALL EIGHT required
  regime keys (`required-regime-keys`, scene_tape.cljc:305-307): `:zoom
  :extent :normalization :coordinate-precision :coverage-precision
  :lifecycle :backend :verdict` — and `validate-regimes!` (:316-340)
  hard-requires the partition to start at exactly `0.01`, end at exactly
  `1000.0`, with no gap or overlap. Instance coordinates ride f32 through
  the W2-A transport; coverage format is the `rgba8unorm` texel path;
  mip-selection behavior is declared per zoom band; backend class per the
  attestation law. Normalization: this wave inherits W0-A's single
  `"screen-constant"` normalization (verifier.cljs:525), declared; the
  second normalization axis is a receipt owed by the path-atom wave
  (shape-local pressure arrives there). Extent: W0-B Q2 priced extent as
  the dominant error axis (ENGINE.md:844-849), so G4 tests TWO placement
  extents (the standard fixture extent + a 256-world-unit placement)
  across all seven zoom regimes. No unqualified precision claim survives.
  **Helper authorization (ruling Q5):** the implementer MAY extend
  scene_tape.cljc's private descriptor helpers (`registration` :244,
  `regime` :67 — file-local; the file is MAY EDIT) to accept per-family
  `:export-projections` and a multi-band regime vector, PROVIDED the five
  existing families' emitted registration values do not move —
  `family-contracts` :285 feeds `default-family-registry` :385, validated
  on every store write (scene_store.cljc:166-169) and read per-family by
  `frame-contract-registry` (renderer.cljs:2172-2202, load-time coverage
  throw). G1 asserts the five existing registrations are VALUE-identical
  against an EDN snapshot captured at Act 0.

**Contract M citizenship** (template W1 §5.1): all fields supplied; the
receipt set M1–M12 maps onto gates in §10. M9 export: this wave PROMISES NO
export projection — the image family's `:export-projections` value is the
pinned keyword `:none-promised` (refusal as extension point; W4 owns export
seeds; emitted via the Q5-authorized helper parameterization above). M7
lifecycle: an explicit DECLARED PARTIAL REFUSAL (same honest shape as M9) —
no authoring hand exists this wave, so the hand → settle leg is REFUSED and
owned by the felt wave; the settle → scene leg (store/tape identity, §7) is
receipted at G8. M2's instance-identity half is receipted at G8 (same
material digest, two instances, nested and reordered → identities stable).
M12's corpus is receipted at G7's corpus-enumeration leg (§10).

**Contract C color citizenship** (W1 §8):
- Every image texture declares color space, channel meaning, and alpha
  association (`straight` | `premultiplied` | `opaque`); mis-tagged fixture
  inputs fail visibly (C5).
- Decode/transfer into linear happens EXACTLY ONCE at resource ingress;
  premultiplication after coverage and effective opacity are known
  (W1 §8.1.3).
- **ONE color declaration; seam state is orthogonal (ruling Q1, replacing
  R1 F4's two-regime construction — which was unlawful under the very
  sentence it cited: W1 §8.2.5 reads "an `*unorm` target requires the
  declared shader/presentation transfer. The plan proves exactly one
  conversion, never zero or two" — zero is the one count it forbids).**
  The image family declares exactly what the five admitted families
  declare: the linear-premultiplied scene contract that `validate-family!`
  hard-requires (scene_tape.cljc:364-367; `registration` :279 supplies
  it). There is NO per-family presentation-regime row, and no zero-transfer
  row is claimed anywhere. The family RENDERS under whatever seam state is
  live, exactly as rect/text/slug do today:
  - **Candidate chain (strict, receipted):** with `:scene-color-enabled?
    true` in fixture (init systems take `:scene-color` directly — no
    `runtime/render.cljs` edit; R1 traced this clean), the full chain runs:
    tagged source → ONE ingress transfer to linear → premultiplied
    source-over → ONE presentation encode to output space. **The
    presentation encode's road is FIXED by R2 N2's trace** (the product
    present path is a shaderless `copyTextureToTexture`; the attachment
    format decisions live in MUST-NOT files; a shader blit would trip the
    fence): the candidate FIXTURE's render/capture target is declared in an
    `*-srgb` format so the hardware performs the linear→output encode at
    attachment write (the declared hardware transfer, W1 §8.2.5) — created
    in the implementer-owned verifier/renderer fixture path, no shader
    blit, no product present-path change, no `electric_flow.cljc` or
    `runtime/render.cljs` edit. The ingress transfer road (`*-srgb` texture
    sample vs `*unorm` + shader transfer) remains the implementer's
    DECLARED choice, proven by C4 sentinels: exactly one ingress
    conversion and exactly one presentation encode, never zero or two.
    C4's equivalence is scoped WITHIN the candidate chain across its
    presentation modes — direct-to-target and intermediate-then-copy
    (readback is the capture itself; export is refused per M9) — which is
    what W1 §8.3 C4 enumerates.
  - **Seam-off state (the recorded migration input, never a family row):**
    with the seam default-OFF the whole pipeline — all six families — runs
    the recorded legacy passthrough. That state belongs to the SEAM
    (`scene-color-seam`, scene_tape.cljc:44-51) as W1 §8.2's closing
    records it: migration input, not precedent, and not any family's color
    declaration. Under seam-off the image path performs ZERO color
    transfers end-to-end — in particular it must NOT half-convert (an
    ingress decode into linear with no presentation encode) — receipted by
    G6's seam-off consistency leg: a pinned sRGB byte fixture renders
    byte-through unchanged with the seam off. Zero behavior change for the
    existing families is G2's byte-identity over the 21 goldens. The known
    legacy alpha defect (`src-alpha` alpha accumulation, W1 §1) is the
    seam state's recorded property, not this family's to repair.
  - C6 records the family's ONE regime row per enabled texture/format; the
    seam-off passthrough is recorded as SEAM state beside it, never as a
    family color regime. Copying the live pipelines' unassociated
    convention into the image pipeline as if it were a declaration is a
    C-receipt failure, not a style choice. Rendering images under seam-off
    is NOT refused — refusing would make image the one family whose
    visibility keys on seam state, a coupling no admitted family carries.

## 6. Resource infrastructure laws

- **Decode**: browser `createImageBitmap` from verified bytes (the one
  existing precedent: fonts.cljs:61). The colorSpaceConversion behavior is
  part of the declared ingress transfer and receipted with a profiled
  fixture (G6). Decode runs OFF the hand path; no synchronous GPU→CPU or
  decode work in a frame callback (W0-C §5.7 rule 5 spirit; fence-checkable
  as "no await inside the producer").
- **Upload**: `copyExternalImageToTexture` (precedent renderer.cljs:760-784)
  or `writeTexture` from decoded bytes — implementer's road, receipted with
  upload-enqueue + completion timings (measurement, not a gate bar — §10
  attestation law).
- **Mipmaps**: full chain for dedicated textures; chain math (level count +
  per-level sizes) lives in `image_material.cljc` and is JVM-tested; mip
  FILTERING happens in linear space or the deviation is declared and
  receipted (trap T4). Mip generation road (render-pass blit chain vs
  compute) is the implementer's; the receipt pins level-1+ bytes for one
  fixture so the filter law is falsifiable.
- **Atlas/dedicated placement**: a declared placement-tier rule — small
  sources pack into a shared atlas page (padding ≥ 2px, declared max-side,
  UV insets); sources over the tier bound get dedicated textures with full
  mip chains; atlas overflow falls back to dedicated (never silent
  eviction). The shelf/skyline choice is the implementer's; the allocator's
  invariants (no overlap, padding respected, deterministic placement for a
  given insertion sequence) are JVM-tested (G7).
- **Bind groups/batching (ruling Q2 — the sub-draw indirection):** batches
  split by texture binding (atlas page or dedicated texture); batching
  merges ONLY order-contiguous compatible entries OR carries "an explicit
  indirection that reproduces tape order" (Contract O §3.2's own second
  clause — the shape this wave uses) — an atlas does not license
  reordering transparent/pickable entries for bind-group convenience.
  **Mechanism (R2 N6 · ruling Q2):** one `pool-info` maps to one bind
  group and one count key, so the existing `store-pool-entries` helper
  CANNOT express per-binding splits. The image producer therefore mints
  exactly ONE batch entry per (vi, `:images`) — entry id
  `[:frame/store vi :images]`, no per-run id family, so the compile-tape
  duplicate-id law (scene_tape.cljc:508-515) is satisfied by construction
  — whose `:paint` carries an ORDERED sub-draw vector derived from the
  stamped ops: `{:pipeline p :sub-draws [{:bind-group bg :buffer b
  :instance-count n :first-instance o} …]}`, sub-runs order-contiguous in
  op order with correct per-run instance offsets. That vector IS the
  explicit indirection reproducing tape order. It is executed by the image
  family's REGISTERED executor — each `frame-family-registry` row already
  owns its `:execute!` (renderer.cljs:2170-2174); the image row registers
  its own sub-draw walker beside `execute-gpu-batch!` — the central loop
  (`execute-scene-tape!`) stays family-blind, zero central branches (T1).
  **Buffer home (ruling Q6):** the image instance buffer is OWNED by
  `init-image-system` (renderer.cljs, MAY EDIT), allocated through the
  existing `buffer_pool` API without editing `buffer_pool.cljs` (READ-ONLY
  context — it is in neither §12 column), and WRITTEN inside
  `draw-frame!`'s existing encode window from `(:images store-frame)` —
  the frame map is assembled inside `draw-frame!` (renderer.cljs:2346-2368)
  and `store-frame` already rides in whole from the caller, so no
  `runtime/render.cljs` edit and no new `pool-draw-info` call site (all
  four existing sites live in that MUST-NOT file). The write is
  IDENTITY-GATED: it runs only when the `:images` payload identity changes
  (the ops arrays are identity-stable across unrelated writes — the
  existing T5/G8 law), never unconditionally per frame — the render-seam
  proportionality law (decisions.md) stops at no boundary. A buffer write
  is none of the fence's four forbidden pass calls
  (verify_scene_tape_fence.mjs:76-79). The verifier fixture path
  constructs its own buffer. Day-one simplification lever: a single shared
  atlas page + few dedicated textures keeps sub-draw counts small.
- **Budget/lifecycle (M10)**: every texture registers with the gpu-budget
  tracker pricing the FULL mip chain (T11); destroy paths mirror the
  existing `destroy-*-font-resources!` shape. **The error/lifecycle sink
  is NAMED (ruling Q7 — no "existing error lane" exists; the census found
  none):** the image ingress/lifecycle receipt is a `globalThis`
  structured receipt on the established twin-check precedent (the exactly
  two existing names, renderer.cljs:2244/2250/2256) —
  `__softland_image_ingress_receipt`, carrying per-digest
  `{:status (ok | refused | unavailable | device-lost) :reason … :counts …}`
  plus totals, written by the ingress/registry path and read by the
  verifier. Asset-unavailable: an unresolvable digest renders the declared
  DETERMINISTIC placeholder paint (pinned bytes) and records
  `unavailable` in the receipt — never a silent skip, never a throw in
  the frame body. **Budget failure (M10's named clause):** ingress that
  would exceed the declared budget REFUSES the resource — placeholder
  paint + `refused/:over-budget` receipt row — receipted in G9 with an
  injected tiny budget cap. Device-loss: resources rebuild from the
  registry (digest-keyed reconstruction is the invalidation law working
  as designed), recorded as `device-lost` + rebuilt counts; machine-driven
  receipt (G9). No product error UX this wave (felt-wave material).

## 7. Store + tape integration — the pool-lane pattern (RECUT after R1
F1/F2: per-instance STORE entries are refused)

Validation R1 traced the original per-instance-entries design into three
structural defects: `scene_runtime.cljs` derives the GPU payload as
`(mapv :runtime/slot entries)` over `maintained-entries` (K entries per
slot → ops duplicated K× while `:ops-count-by-vi` collapses by vi);
`slot-entry` has five call sites incl. `remove-slot` (one key removed →
K−1 leaked) and the batch oracle at scene_store.cljc:263 (one entry per
slot → maintained ≡ oracle structurally impossible). The recut follows the
pattern EVERY existing non-rect family already uses (msdf/slug/shadow:
paint citizenship through renderer pool/system entries; product pick
through the slot's tree):

- **The store's maintained view is UNTOUCHED.** `slot-entry` keeps its one
  entry per slot; `maintained-entries`, `upsert-slot`, `remove-slot`,
  `rebuild-ordered`, and the maintained ≡ batch-oracle equivalence keep
  their exact current law. No image entries enter the STORE tape.
- **Op lane**: `rect_tree.cljc` gains `tree->images` (precedent
  `tree->rects` :198) — an rt-node carries an image paint reference
  `{:image/digest … :image/crop …}`; presence-driven emission. An image
  rt-node REQUIRES `[:data :address]` (ruling Q3's owner route) —
  `tree->images` refuses a digest-bearing node without one, fail-closed at
  flatten (write-time, never the frame body; G8 fixture asserts the
  refusal). **Clip law (T15):** when an ancestor clip clamps bounds,
  `tree->images` carries the clamp into the crop/UV insets proportionally
  (pure math, JVM-tested) so a partially-clipped image CROPS, never
  stretches — the `tree->rects` clamp precedent (:221-232) rescales UVs if
  copied blindly, and `tree->shadows` (:330-335) takes no clip argument at
  all: the two named precedents behave differently, so the image walk's
  behavior is declared here, not inherited. `flatten-ops` (:46) gains
  `:images`; `stamp-ops-container` (:55) stamps them. The payload
  derivation extends with the `:images` key: `:ops-count-by-vi`
  (scene_runtime.cljs:402-409, per-lane keyed) gains `:images`;
  **`:order-by-vi` is UNCHANGED** — it is lane-agnostic, one `:order` map
  per vi shared by all lanes (scene_runtime.cljs:410-414; a §11 pinned
  fact — R2 N13/R3 N7's correction). **The pure derivation (ordered slots
  → payload + count/order maps) is EXTRACTED into `scene_store.cljc`**
  (pinned home — the second-namespace option is DROPPED, R3 N11: §12
  authorizes no second src namespace), with `scene_runtime.cljs` keeping
  only the thin Missionary wrapper. R2 N1's trace: scene_runtime is
  `.cljs`, so without this extraction G8's JVM receipt over the payload leg
  is unexecutable. The extraction is behavior-identical and covered by the
  existing suites + G2.
- **Paint citizenship lives in the RENDERER arrangement** (where every
  family's citizenship already lives): the registered image producer mints
  ONE `[:frame/store vi :images]` entry per vi (precedent
  `store-pool-entries` renderer.cljs:1967-1990) with the slot's
  `:stack-path`, image `:part-rank` **3**, and the ordered sub-draw paint
  per §6. **Intra-slot lane order, DECLARED (ruling Q2):** within one
  slot's stack-path the total kind-layer order is part-rank `shadow 0 <
  rect 1 < slot-text 2 < image 3` — the three existing literals are pinned
  facts (§11); the image value 3 is reserved and unique across lanes (G5
  asserts uniqueness). Images therefore paint as a kind-layer ABOVE the
  slot's text, exactly as text already paints unconditionally above rects
  — a DECLARED order limitation: tree-interleaved image/rect (or
  image/text) sibling ordering within one slot is NOT expressible this
  wave; the named extension point is per-instance `sibling-rank` entries
  when a consumer demands true interleave. The image-above-text default is
  a declared, paint-only, felt-wave-revisitable choice (one integer; no
  durable touch). A part-rank collision would route cross-lane order
  through `stable-tie`'s `pr-str` fallback (scene_tape.cljc:393-397) —
  keyword spelling as z-order — which W1 §3.1 forbids outright (T13).
  Entry ids are unique per (vi, kind) by construction (one entry per lane
  per vi). **Arrangement-level receipt (ruling Q4 — machine-only):**
  `produce-frame-entries`, `update-frame-arrangement`, and
  `compile-frame-tape` are un-privatized (an allowlisted renderer.cljs
  edit; avoids the cljs private-access warning against G10's baseline) and
  the VERIFIER drives a synthetic frame — stub pool-infos + a non-empty
  `:store-frame {:images …}` — asserting maintained ≡ batch with image
  entries present, the duplicate-id throw on a seeded duplicate, and the
  declared lane order. No `draw-frame!` call, no product-loop claim: the
  product frame-loop join is the felt wave's staged obligation (preamble).
  Image sub-draws paint in tape order through the image family's
  registered executor; the central loop stays family-blind.
- **Pick**: the product read is §5's declared route — image nodes are
  hit-testable rt-nodes inside the slot tree carrying `[:data :address]`;
  cross-slot overlap resolves by the maintained view's reverse order
  exactly as today; within-slot resolution is `rt/hit-test`'s traversal
  (half-open, §5's declared boundary law). For images the product read
  EQUALS the candidate quad geometry by construction (§5 — no divergence
  sentinel; G4 receipts AGREEMENT and the image's own address in the
  result). No `pick-reverse` change, no new dispatch.

The end-to-end authoring join (ground → store with real image material by
hand) is the felt wave's; THIS wave receipts the op lane + store payload at
the .cljc/JVM level (fixture trees through the real store API), the paint
citizenship at the renderer-arrangement level (the synthetic-frame verifier
receipt + fence — the PRODUCT frame loop is NOT driven; that join is the
staged obligation in the preamble), and the GPU truth through the verifier
(real renderer pipelines, real WebGPU). The receipts SAY this scope
explicitly — the phase artifact and the `--assert-image-contract` receipt
both state the product-loop non-claim in their own fields (R2 repair (c)).

## 8. Verifier extension — the bank appends, RED is preserved
(RECUT after R1 M1–M4, M11: the mechanics are named, not assumed)

- `verifier.cljs` gains image cases: exactly THREE fixtures — (a) an opaque
  sRGB image placed via the atlas, (b) an alpha PNG on a dedicated texture
  with mips, (c) a partially-clipped instance (the T15 crop-vs-stretch
  truth is only visible in pixels) — across the SEVEN standing zoom
  regimes (zoom-cases :31-38): exactly 21 new image golden rows appended
  to the bank with the same raw/png sha256 + determinism (2× render)
  discipline.
- **Capture path (R1 M2):** image cases get their OWN capture routine
  (multi-system pass and/or configurable clear value for the C2 non-black
  ground) — the existing single-system `render-system-bytes!` path and its
  `{0,0,0,0}` clear serve the 21 existing cases UNCHANGED; editing their
  path is an S4.
- **Fixture delivery (R1 M1):** the synthetic origin in `run_verifier.mjs`
  (`serveSyntheticOrigin`, which today serves only `/`, `/js/main.js`,
  `/fonts/*`) is extended to serve
  `test/app/fixtures/render_engine/images/*` — an allowlisted edit, named
  here so it is an implementer disclosure, not a drift finding.
- **Bank-append mechanism (R1 M3/M4 · R2 N3/N5):** wholesale
  `--update-goldens` (which rewrites all 21 PNGs + manifest + environment)
  is FORBIDDEN in this wave. The implementer adds a SCOPED append road
  (e.g. `--append-image-goldens`): writes ONLY the new image rows + their
  PNG files + merged manifest metadata. R2 N3 traced that today the golden
  comparison result never reaches classification or stdout (short-circuited
  behind the permanent parity failure) — so the CASCADE REPAIR is a named
  deliverable: (a) an existing-golden byte mismatch produces its OWN
  classification and non-zero exit (an S4 stop condition made machine-
  visible), evaluated BEFORE parity classification; (b) the golden and
  image results are PRINTED in the stdout receipt and written to the
  receipt file — the stdout summary gains the exact keys
  `existingGoldens` ("21/21"), `sourceMatch`, `environmentMatch`,
  `imageGoldens`, `imageDeterminism`, and `imageParity` (today it omits
  every golden/source field — run_verifier.mjs:503-522, a verified gap);
  `--assert-image-contract` asserts their PRESENCE and values (R2 repair
  (c) complete); (c) the everything-green-except-MSDF state still yields
  exactly `candidate-pick-parity-failure`, exit 1. **Accounting isolation
  (R2 N5 · manifest home pinned, R3 A7):** image cases form their OWN case
  family in a NEW manifest section (`imageAtomCases`, beside — never
  inside — `currentManifest.images`) with their OWN receipt fields; the
  legacy `goldenComparison` stays an exactly-21-row predicate and
  `imagePass`/`updateAuthorized`/sentinel/divergence/parity accounting
  never see an image-atom row — the pinned invariants (divergence
  sentinels 7/7 · candidate parity 14/21 · MSDF 47/regime) are untouched
  by construction, not by hope. **Expected final values, pinned here (not
  deferred):** legacy — deterministic 21/21 · goldens 21/21 · parity
  14/21 · sentinels 7/7 · classification `candidate-pick-parity-failure`;
  image-atom — imageGoldens 21/21 · imageDeterminism 21/21 · imageParity
  14/14 rows decisive-pass with floors (two extents × seven regimes, G4).
  **Receipt custody for the wave's own edits (R3 A4):** the image-atom
  manifest section carries its OWN `imageAtomInputs` fingerprints for the
  four additional edited sources (`scene_tape.cljc`, `scene_store.cljc`,
  `rect_tree.cljc`, `verifier.cljs`) — the legacy `productionInputs`
  fingerprints only `renderer.cljs` (run_verifier.mjs:149-169) and its
  fields are untouched; `--assert-image-contract` asserts the image-atom
  fingerprints are current.
- **Append authorization is preflighted, never inferred after writes.** Before
  the scoped append road writes ANY file it computes the existing-bank
  comparison against the still-unmodified manifest and 21 PNGs and ASSERTS:
  the legacy filename set and row count are exact; every legacy row has
  `rawMatch`, `pngManifestMatch`, and `goldenFileMatch`; determinism is 21/21;
  `environmentMatch` is true; and the old PNG digests are unchanged. A
  renderer-source mismatch may authorize only the explicitly receipted
  `productionInputs.rendererSource` metadata refresh; it can never bypass
  those byte and environment assertions. After the append, the replay ASSERTS
  `sourceMatch`, `environmentMatch`, and the full legacy + image golden
  comparison before reporting the update authorized.
- Image pick-parity probes: quad-interior CPU classification vs GPU coverage
  at boundary pixels, byte-128 ties separated (never thresholded), same
  shape as the existing candidate-parity probes — across TWO placement
  extents (the standard fixture extent + a 256-world-unit placement; §5's
  regime law, W0-B's dominant error axis) × the seven zoom regimes = 14
  parity rows. Image parity must be DECISIVE-PASS **with declared floors
  (R1 F5): `boundary-pixel-count > 0` and `decisive-count > 0` per row** —
  a zero-sample "pass" is a Cg defect (§5 ramped-edge obligation). It must
  NOT alter the run's classification, which stays
  `candidate-pick-parity-failure` exit 1 under ordinary invocation — RED
  exactly on the preserved MSDF counterexample (47 decisive mismatches per
  regime). A run where MSDF goes green, image parity fails silently, or
  the ordinary exit code flips is a STOP (S4).
- The 21 existing PNGs: byte-identical, asserted by the run itself (via the
  §8 preflight + stdout keys). Bank extension changes manifest metadata
  (`productionInputs.rendererSource` refresh after renderer edits is LAWFUL
  and required — precedent: the shaping-correction S4 metadata-only
  migration; image-atom rows append into their OWN `imageAtomCases`
  section, never into `currentManifest.images`) — never existing image
  bytes. An image-bank update cannot bless any geometry disagreement
  (W1 §4.6).
- **Fixture provenance (R1 M11 · R2 executed receipt):** fixture images are
  minted by a COMMITTED deterministic JVM generator (Clojure +
  `javax.imageio` — no new npm dependency, no network fetch, no
  `package.json` edit). **The iCCP road is pinned by R2's executed probe:**
  plain `ImageIO.write` of an ICC-color-space image SILENTLY omits the
  `iCCP` chunk; the working road is explicit `IIOMetadata` + `mergeTree`
  with PRE-DEFLATED profile bytes (raw bytes write a corrupt chunk that a
  naive presence check passes; `ICC_Profile.getData()` is call-order
  dependent). The generator therefore ASSERTS well-formedness by re-reading
  its own output and inflating/parsing the iCCP payload — never a mere
  chunk-presence check — and byte-determinism across two runs. Generated
  bytes are committed content-addressed with digests pinned in the phase
  artifact; the browser-side ingress receipt pins the post-conversion
  pixels for the profiled fixture (G6). **ICC decode probe (Act 0,
  pre-registered — R2 N19):** before any code, the implementer probes
  whether THIS UA's `createImageBitmap` honours an embedded ICC profile
  (decode the profiled fixture in the verifier context; compare
  post-decode pixels against the pinned expectation). Probe pass →
  proceed. Probe fail → the declared ingress-transfer road is broken in
  this environment: STOP S5 with the probe receipt attached — never a
  silent fallback, never a verifier edit.
- Expected receipt-count changes are pinned in §8 above (legacy counts
  frozen; image-atom 21/21/14) and re-confirmed with before/after values
  in the phase artifact.
- **Executable expected-RED assertion mode.** `run_verifier.mjs` gains
  `--assert-image-contract`. Exit semantics are BOUND TO INVOCATION MODE
  (R3 N8): ordinary invocation retains the standing exit-1 semantics with
  classification `candidate-pick-parity-failure`; assertion mode exits 0
  IFF the only red is the preserved MSDF candidate-pick counterexample AND
  it has machine-asserted every G2/G3/G4 field: the exact 21-row legacy
  custody, the 21 appended image-atom rows + their determinism, decisive
  image parity floors per row (two extents × seven regimes), Q5/Q8, 7/7
  divergence sentinels, exact MSDF mismatch/tie counts, `sourceMatch`,
  `environmentMatch`, the full golden comparison, the §8 stdout keys, the
  `imageAtomInputs` fingerprints current, the
  `__softland_image_ingress_receipt` fields for the driven
  unavailable/device-loss/over-budget fixtures, and the product-loop
  non-claim statement field (§7). It also asserts the attestation-law
  fields: adapter identity, `isFallbackAdapter`, and a non-empty renderer
  string. A missing field is a failure; printing it is not an assertion.
  Assertion mode's exit 0 is NOT an S4 exit flip (S4 binds to ordinary
  invocation).
- **Boot-verify FIRST (Act 0)**: `npm run verify:render-engine` must exit 1
  matching the current baseline (deterministic 21/21 · goldens 21/21 ·
  candidate parity 14/21 · classification `candidate-pick-parity-failure` ·
  environment fingerprint `5ced2482f3d0b8e9a14465bba06808a04d3a5a9b7d386fcf71f40a30f557343e`)
  BEFORE any code. The Act-0 receipt also records `sourceMatch`,
  `environmentMatch`, full golden-comparison status, and both current/manifest
  renderer-source hashes; absent fields or a byte/environment mismatch → STOP
  S4, never a verifier edit. A source-metadata mismatch is named input debt
  and must be true by final G2; missing `isFallbackAdapter`/renderer-string
  attestation is likewise named input debt that must be machine-asserted in the
  final receipt. Neither debt authorizes old-byte replacement.

## 9. Traps ledger (cite trap numbers in code comments)

- **T1 · central branch.** Naive: `case :image` in `draw-frame!`/executor.
  Failure: fence rejects; the seam's whole law dies. Ruling: registration
  only — tape registry + `frame-family-registry` producer; the executor
  stays family-blind (O6).
- **T2 · untagged color.** Naive: decode whatever bytes arrive. Failure:
  double/zero transfer, fringes, unfalsifiable color. Ruling:
  `:reject-untagged` at ingress; legal tags `#{:srgb :embedded-profile}`;
  refusal fixture in M1.
- **T3 · premultiplication order.** Naive: premultiply at decode, or sample
  `*-srgb` then re-encode implicitly — or encode inside `scene_color()`
  pre-blend (renderer.cljs:7-31): that satisfies a naive one-encode count
  while encoding BEFORE fixed-function blending, which is wrong under
  blending (R3 A5). Failure: exactly-one-transfer violated (zero or two
  conversions, or one in the wrong place), dark/bright fringe on non-black
  backgrounds. Ruling: declared road + C4 sentinels on the candidate chain
  (direct and intermediate present legs, §5) + C2 fringe receipt; the
  presentation encode is the `*-srgb` attachment write, never in-shader
  pre-blend.
- **T4 · gamma-wrong mips.** Naive: box-filter mip generation on sRGB bytes.
  Failure: mip levels darken; zoom-out shifts color. Ruling: filter in
  linear (or declare + receipt the deviation); level-1 bytes pinned for one
  fixture.
- **T5 · atlas bleed.** Naive: tight packing, linear filtering. Failure:
  neighbor texels bleed at quad edges and mip levels. Ruling: declared
  padding + UV insets; bleed receipt at the deepest sampled mip of an
  atlas-placed fixture (boundary-pixel comparison vs dedicated placement).
- **T6 · accidental alpha pick.** Naive: sample alpha in pick "because it
  feels right". Failure: undeclared pick policy; parity probe unfalsifiable.
  Ruling: geometric quad interior only (§2 refusal); the probe tests the
  quad, and a transparent-texel hit inside the quad IS a hit.
- **T7 · golden blessing.** Naive: `--update-goldens` while any existing
  byte differs or MSDF "improves". Failure: the permanent counterexample's
  authority dies. Ruling: append-only bank; 21 bytes identical; MSDF 47
  preserved; classification + exit unchanged (S4 otherwise).
- **T8 · second transform path.** Naive: image quads carry their own
  matrix math. Failure: Q8's one-transport law broken — the exact debt W2-A
  paid down. Ruling: instances consume W2-A affine slots
  (`max-transform-nodes` table); zero new transform representations.
- **T9 · caller-asserted identity.** Naive: trust `{:digest …}` from the
  caller. Failure: M5 provenance dies; cache poisoning by mislabel. Ruling:
  ingress re-hashes bytes; mismatch refuses with a diagnostic receipt.
- **T10 · frame-path decode.** Naive: decode/upload inside the producer when
  a texture is missing. Failure: hand-path stall (the 28s-frame family of
  bugs). Ruling: producers are lookup-only; missing resource → placeholder
  paint + async ingress enqueue; the producer-purity check (greps the image
  producer slice for decode/`createImageBitmap`/promise-await shapes) lives
  INSIDE `verify_scene_tape_fence.mjs` — already allowlisted and already
  wired into `verify:render-engine`, so no `package.json` edit and no new
  script wiring (R1 M7).
- **T11 · budget lies.** Naive: keep `register-texture!` or
  `replace-texture!` at level-0 pricing. Failure: budget under-counts every
  mip chain (~33%); M10 receipts are unfalsifiable, and replacement can
  silently restore the lie after correct registration. Ruling: price the
  full declared chain on both roads; budget test asserts chain math for one
  fixture through registration and replacement.
- **T12 · fence dilution.** Naive: add image to the fence families list but
  skip the renderer-slice check or the seeded self-test. Failure: the fence
  passes vacuously — and R1 M5 found the CURRENT self-test already regex-
  tests a hardcoded literal rather than the real slice. Ruling: fence
  requires the id in BOTH files' slices, and the self-test is STRENGTHENED
  while in the file: the seeded central-branch violation is injected into a
  copy of the ACTUAL extracted `draw-frame`/executor slice text (so the
  self-test exercises the real extraction + forbid path), and it must still
  reject.
- **T13 · part-rank collision (ruling Q2).** Naive: reuse an existing lane's
  part-rank integer for the image lane. Failure: `compare-order` falls to
  `stable-tie`, whose non-number comparator is `(compare (pr-str a)
  (pr-str b))` (scene_tape.cljc:393-397) — keyword SPELLING becomes
  z-order, the exact substitution W1 §3.1 forbids ("stable-tie … cannot
  replace missing sibling order"). Ruling: image part-rank is the reserved
  unique 3 (§7's declared lane order); G5 asserts cross-lane rank
  uniqueness; a collision is a Contract-O violation, never a tie.
- **T14 · per-node transform (ruling Q3).** Naive: give the image rt-node
  its own transform/rotation key "for one clever fixture". Failure: §5's
  pick equality (container-local AABB ≡ transformed-quad interior) silently
  dies — pick diverges from paint with no receipt. Ruling: `rt-node`
  carries NO transform key (rect_tree.cljc:12-28, §11 pinned fact); the
  scene-tape fence gains a rect_tree read + forbid, and G8 pins the exact
  10-key `rt-node` key set; introducing a per-node transform requires a
  NEW pick contract, never a quiet key.
- **T15 · clip-clamp stretch (R2 N15).** Naive: copy `tree->rects`'s
  clip-clamp (rect_tree.cljc:221-232) into `tree->images` without touching
  UVs. Failure: clamping the quad without insetting crop/UVs RESCALES the
  sampled region — the image stretches instead of cropping (and
  `tree->shadows` :330-335 takes no clip at all, so neither precedent is
  the law). Ruling: §7's declared clip law — clamp carried into crop/UV
  insets proportionally, JVM-tested math + the partially-clipped golden
  fixture (G3 case (c)).

## 10. Gates — numbered, executable, owners; attestation law

**Attestation law (first field, every GPU receipt):** adapter identity +
`isFallbackAdapter` + renderer string. SwiftShader numbers are receipts of
THIS environment, never hardware claims. A perf number without attestation
is unclassifiable (scene-substrate G4 lesson; shaping-correction
FOUNDING-EVIDENCE rule: attestation required in all future receipts).
Timing measurements are RECORDED, not gate bars, except where a bar is
stated explicitly below.

**Executable forms (run exactly; labels below bind each gate to one or more):**
- `[JVM-FULL]` `clj -X:test full :shards 1`.
- `[RENDER-BUILD]`
  `clj -M:dev -m shadow.cljs.devtools.cli release render-verifier`.
- `[RENDER-RED]` run `[RENDER-BUILD]`, then
  `node test/render_engine/run_verifier.mjs` (ordinary invocation;
  expected exit 1, classification `candidate-pick-parity-failure`).
- `[RENDER-ASSERT]` run `[RENDER-BUILD]`, then
  `node test/render_engine/run_verifier.mjs --assert-image-contract`
  (expected exit 0 with the §8 assertion set green).
- `[SCENE-FENCE]`
  `node test/render_engine/verify_scene_tape_fence.mjs`.
- `[TEXT-FENCES]` `npm run verify:text-layout`, then
  `node test/render_engine/verify_shaping_correction_fence.mjs`.
- `[PHASE-RECEIPT]`
  `node test/render_engine/verify_image_atom_receipt.mjs docs/current-mental-model/build/render-engine/IMAGE-ATOM-P1.md`.
  The receipt verifier ASSERTS every G12 field, the diff-derived changed-file
  partition, the numeric gate floors, and locator status; it never merely
  prints fields for inspection.

- **G1 admission** `[SCENE-FENCE]` `[RENDER-ASSERT]` `[JVM-FULL]` —
  `validate-family!` passes for the image family;
  `frame-contract-registry` load-time coverage law holds (boot does not
  throw); fence green with image in the families array; seeded
  central-branch self-test still rejects; the five existing families'
  registration values are VALUE-identical against the Act-0 EDN snapshot
  (ruling Q5 — the helper parameterization moved nothing). Owner:
  implementer; gate re-runs.
- **G2 zero-regression** `[RENDER-RED]` `[RENDER-ASSERT]` — full verifier
  replay: existing 21 goldens byte-identical (printed via the §8 stdout
  keys + classification-visible per §8's cascade repair — R2 N3 made the
  bare claim assertable) · SDF 7/7 · Slug 7/7 · MSDF exactly 47 decisive
  mismatches + 2 ties per regime · divergence sentinels 7/7 · ordinary
  invocation: classification `candidate-pick-parity-failure`, exit 1;
  assertion mode: exit 0 with classification unchanged (the exit
  assertions are BOUND to invocation mode — R3 N8). Scope honesty (R3
  A3): the 21 banked goldens cover the sdf-rich-rect/msdf/slug modes;
  shadow and clip have no banked pixels — their zero-change rides the
  full JVM suite, the fence, and the synthetic-frame arrangement receipt.
  Owner: implementer; gate re-runs independently.
- **G3 image goldens** `[RENDER-ASSERT]` — the appended image cases:
  determinism 2× · raw/png sha256 recorded · all THREE fixtures (incl.
  the partially-clipped case — T15's crop-not-stretch truth) across all
  seven regimes render non-trivially (a blank capture is a harness bug
  until op counts disagree — the framework W2-INT rule: assert op-count
  parity FIRST).
- **G4 image pick parity** `[RENDER-ASSERT]` — the geometry probe renders the quad with
  OPAQUE WHITE REFERENCE PAINT saturating the compared red channel, or
  records coverage in a separate coverage-only attachment (Cg isolation —
  W1 §4.3's own law; R2 N4 traced that the probe reads channel 0, so a
  textured/colored quad would satisfy floors while comparing garbage; the
  texture is a Cv factor and is EXCLUDED from the geometry probe).
  DECISIVE-PASS across TWO placement extents × seven regimes (14 rows —
  §5's regime law) **with floors: `boundary-pixel-count > 0` AND
  `decisive-count > 0` per row, both reported in the receipt** (a
  zero-sample pass is a FAIL — R1 F5); byte-128 ties reported as ties
  (`:half`), never votes; the receipt records that no slop path ran
  (radius 0.0 — §5). Textured content correctness is the goldens' job
  (G3), never the geometry probe's. Overlap receipts run on the PRODUCT
  route (§5/§7): cross-slot image/rect overlap returns topmost by the
  maintained view's reverse order through the real `scene_store.cljc/pick`,
  and the returned address is the IMAGE'S OWN (never an ancestor's);
  within-slot overlap follows the tree traversal law. The
  product-vs-candidate receipt asserts AGREEMENT (equality by
  construction, ruling Q3) — no divergence sentinel exists for this
  family. (O2 extension, honestly scoped.)
- **G5 order receipts (tiers explicit — R2 N1)** `[JVM-FULL]`
  `[RENDER-ASSERT]` — O1 shuffle-determinism
  (registry insertion + map construction shuffled → same order hash) runs
  at the TAPE level on JVM (`scene_tape.cljc` is `.cljc`) with the image
  family registered, asserting cross-lane part-rank uniqueness (shadow 0 ·
  rect 1 · slot-text 2 · image 3 — T13). O5's batching falsifier
  (randomized registration/map order → byte-identical pixels + pick
  identities, and the image sub-draw vector ordered by stamped op order,
  never map iteration) is a BROWSER/verifier receipt. Neither leg claims
  the other's tier.
- **G6 color receipts (one declaration + seam-state legs per §5 — ruling
  Q1)** `[JVM-FULL]` `[RENDER-ASSERT]` — CANDIDATE seam
  enabled in fixture: C1 reference source-over for an image over
  opaque/transparent grounds vs CPU linear-premultiplied reference · C2 no
  fringe on non-black background (new capture path, §8) · C4 strict
  sentinels: exactly one ingress transfer + exactly one presentation
  encode (the encode made code-real behind the default-OFF seam), asserted
  on BOTH candidate presentation legs (direct-to-target and
  intermediate-then-copy), including the profiled fixture
  (embedded-profile PNG whose post-ingress pixels are pinned; Act-0 ICC
  probe gates the road — §8) · C5 straight-vs-premultiplied fixture pair
  converts to the same scene value; mis-tagged fixture fails visibly.
  SEAM-OFF consistency leg: a pinned sRGB byte fixture renders
  byte-through unchanged with the seam off (zero transfers end-to-end —
  no half-conversion); the 21-golden byte-identity itself is G2's. C6:
  every enabled texture/format records the family's ONE regime row; the
  seam-off passthrough is recorded as SEAM state, never a family regime
  row.
- **G7 resource laws (JVM)** `[JVM-FULL]` — mip chain math (counts + sizes) · atlas
  allocator invariants (no overlap, padding, deterministic placement) ·
  placement-tier rule (small→atlas, large→dedicated, overflow→dedicated) ·
  ingress refusal fixtures (untagged, digest mismatch) · M1 grammar
  round-trip + unknown-field policy + validator fail-closed (incl. the
  `:image/time` refusal) · **M12 corpus-enumeration leg (ruling on R3
  N12): a JVM test asserts every claimed envelope pressure has a
  committed fixture AND a consuming gate — the pinned pressure list:
  two extents · atlas overflow · straight/premultiplied alpha pair ·
  profiled (embedded-ICC) fixture · untagged refusal · digest mismatch ·
  unresolvable digest · partially-clipped instance.**
- **G8 store/op lane (JVM — recut per §7, R2 N1)** `[JVM-FULL]`
  `[RENDER-ASSERT]` — fixture trees through
  the real store API: `tree->images` emits from image rt-nodes (and
  REFUSES a digest-bearing node without `[:data :address]` — ruling Q3);
  the T15 crop/UV-inset math is asserted exactly on a partially-clipped
  fixture; `flatten-ops`/`stamp-ops-container` carry `:images` with
  container stamps; the EXTRACTED `.cljc` payload derivation (home:
  `scene_store.cljc`) carries the `:images` key in `:ops-count-by-vi`
  with `:order-by-vi` UNCHANGED (lane-agnostic — R3 N7) and without
  disturbing rect/shadow/text counts (asserted on JVM against the
  extracted fn); `slot-entry`/`maintained-entries`/maintained ≡
  batch-oracle equivalence are UNCHANGED and re-asserted (the existing
  suites stay green); the `rt-node` key set is pinned at exactly its ten
  current keys (T14); M2's instance-identity leg: the same material
  digest as two instances, nested and reordered → identities stable;
  `scene_store.cljc/pick` resolves an image node topmost across
  overlapping slots by reverse maintained order, returning the image's
  own address. The arrangement-level receipt with image entries present
  is the VERIFIER synthetic-frame receipt (§7, ruling Q4): maintained ≡
  batch, seeded duplicate-id rejected, declared lane order asserted —
  never a product-loop or `draw-frame!` claim.
- **G9 lifecycle/budget** `[JVM-FULL]` `[RENDER-ASSERT]` — both texture registration and replacement price
  full mip chains (one fixture asserted numerically through both roads);
  destroy paths run; device-loss rebuild receipt
  (registry-driven reconstruction) machine-driven; asset-unavailable renders
  the declared placeholder deterministically; over-budget ingress REFUSES
  (injected tiny cap — M10's budget-failure clause); every driven case
  lands its row in `__softland_image_ingress_receipt` and the verifier
  asserts the fields (ruling Q7).
- **G10 suites** `[JVM-FULL] [RENDER-BUILD]` — full JVM suite green at the final bytes (FULL-tier gate;
  the runner tier entry for new namespaces is an allowlisted +1); cljs
  compile 0 new warnings against the Act-0 captured baseline.
- **G11 fences** `[SCENE-FENCE] [TEXT-FENCES]` — scene-tape fence green with the image id in both slices
  (T12 conditions, incl. the strengthened real-slice self-test); the T10
  producer-purity check exists INSIDE the scene-tape fence and passes;
  the fence gains the pick `(case` forbid (R2 N7 — the refused central
  `:geometry`-keyed pick branch made executable; the mechanism already
  exists for the executor at fence:84) and the T14 rect_tree read +
  transform-key forbid; text-layout + shaping fences untouched and green.
- **G12 receipt hygiene** `[PHASE-RECEIPT]` — the phase artifact (`IMAGE-ATOM-P1.md`) carries:
  attestation-first receipts, before/after verifier counts, fixture digests,
  the diff-derived changed-file list sum-checked against §12 (skill law),
  and every §11 locator re-verified or drift-logged.

**Partition sum-check (skill law):** ONE phase owns G1–G12 in full; the
independent gate session re-runs G1/G2/G3/G4/G10/G11 and spot-checks the
rest against receipts. No gate needs Sid's hand in this wave (the felt gate
is a STAGED obligation outside this contract's gate list, joined to the
artery ruling — recorded in §0 and on the board).

**Receipt↔M map:** M1→G7 · M2→G7/G8 (digest stable across reload +
atlas-vs-dedicated placement + the G8 instance-identity leg:
nesting/reuse/reorder) · M3→G5 · M4→G3/G4 (two extents × seven regimes,
slop path recorded) · M5→G7 (T9 fixtures) · M6→G7 (algorithm-version
keys) · M7→declared partial refusal (§5 — hand→settle leg felt-wave-owned)
+ G8 (settle→scene store-level leg) · M8→G8+G2 (fixture reload + verifier
replay) · M9→refusal declared (§5, `:none-promised` pinned) · M10→G9
(incl. the budget-failure refusal leg) · M11→G6 · M12→G7's
corpus-enumeration leg over the committed fixture corpus + bank rows.

## 11. Input manifest — substance-binding; locators are hints
(machine-verified 2026-08-05; re-verify at phase open, drift → re-locate +
log, substance missing → S3)

- `src/app/client/substrate/scene_tape.cljc` (545 ln) — `family-ids` :10 ·
  `scene-color-seam` :44 · `regime` :67 · `registration` :244 ·
  `family-contracts` :285 · `required-regime-keys` :305 ·
  `validate-regimes!` :316 · `validate-family!` :342 · `register-family`
  :370 · `compare-scalar` :393 (pr-str fallback — T13) · `compare-order`
  :427 · `compile-tape` :501 (duplicate-id throw :508-515) ·
  `paint-forward` :525 · `pick-reverse` :534.
- `src/app/client/substrate/webgpu/renderer.cljs` (2,385 ln) —
  `max-transform-nodes` :618 · `create-msdf-font-resources` :760 (upload
  precedent) · `create-slug-texture` :786 (writeTexture precedent) ·
  `frame-order` :1923 (default part-rank 0) · `gpu-paint` :1949 ·
  `store-pool-entries` :1967 · part-rank literal call sites: shadow 0
  :2031 · rect 1 :2047 · slot-text 2 :2110 (PINNED FACTS — image reserves
  3) · `execute-gpu-batch!` :2157 · `frame-family-registry` :2170
  (per-row `:execute!` — the Q2 registered-executor seam) ·
  `frame-contract-registry` :2192 · `!frame-arrangement` :2206 ·
  `produce-frame-entries` :2209 · `update-frame-arrangement` :2215 ·
  `compile-frame-tape` :2237 · `frame-tape-twin-check!` :2243 (globalThis
  precedent names at :2244/:2250/:2256 — the ONLY two existing names) ·
  `draw-frame!` frame-map assembly :2346 · system constructors
  :681/:866/:930/:995/:1118 · non-system `init-clear-quad` :1256.
- `src/app/client/workspace/scene_store.cljc` (585 ln) — `flatten-ops` :46 ·
  `stamp-ops-container` :55 · `upsert-slot` :140 · `deepest-addressed`
  :220 · `slot-entry` :227 · `scene-tape` :254 · `rebuild-ordered` :272 ·
  `maintained-entries` :285 · `pick` :305.
- `src/app/client/workspace/rect_tree.cljc` (402 ln) — `rt-node` :12
  (PINNED FACT: exactly ten keys — `:id :type :bounds :style :actions
  :children :text :clip? :data :layout` — NO transform key; T14) ·
  `tree->rects` :198 (clip clamp :221-232) · `tree->text-ops` :259 ·
  `tree->shadows` :330 (no clip arity — T15 context) · `hit-test` :359
  (half-open :372-373, no slop input — §5's boundary law).
- `src/app/client/workspace/scene_runtime.cljs` (552 ln) — `<store-frame` :382 ·
  `:ops-count-by-vi` :402 (per-lane keyed) · `:order-by-vi` :410 (PINNED
  FACT: lane-agnostic — one `:order` map per vi; gains NO lane keys).
- `src/app/client/workspace/containers.cljc` (300 ln) — `inverse-point`
  :252 · `effective` :221 — READ-ONLY context, not allowlist.
- `src/app/client/substrate/webgpu/buffer_pool.cljs` (478 ln) —
  `pool-draw-info` :354 · `create-pool` :111 — READ-ONLY context, not
  allowlist (ruling Q6: the image pool is allocated through this API from
  `init-image-system`; the file itself is never edited).
- `src/app/client/substrate/webgpu/gpu_budget.cljs` (334 ln) —
  `register-texture!` :152 · `replace-texture!` :194.
- `src/app/client/substrate/webgpu/verifier.cljs` (805 ln) — `zoom-cases`
  :31 · `render-system-bytes!` :82 · case runner + capture machinery.
- `test/render_engine/verify_scene_tape_fence.mjs` (132 ln) — `families`
  :56.
- `test/render_engine/run_verifier.mjs` (530 ln) — `serveSyntheticOrigin`
  :102 · `productionInputs` :149 (fingerprints ONLY renderer.cljs — the
  §8 `imageAtomInputs` addition exists because of this) · update
  authorization :353 · golden compare :400-445 (`imageComparison` :420) ·
  classification cascade :447-460 · stdout summary :503-522 (carries NO
  golden/source field today — the §8 stdout keys close this) · manifest
  rows + `--update-goldens`.
- `test/render_engine/verify_shaping_correction_fence.mjs` — standing
  read-only fence; executed by G11, never edited.
- `test/app/test_runner.clj` — FULL-tier namespace registration.
- `package.json` — existing verifier/text command composition, READ-ONLY.
- `test/app/fixtures/render_engine/gpu-goldens/` — manifest.json + 21 PNGs +
  environment.json.
- Binding docs: `W1.md` §2–§5, §8, §9 · `ENGINE.md` §0 (Package 2 + hard
  joins) + §12 Q2/Q8 · `W0-C.md` §5 (frame-runtime registration shape) ·
  `W2-B-T1.md` (receipt shape + starter) · the archive draft (§3 reference
  terms).
- `src/app/client/workspace/runtime/fonts.cljs` :61 (`createImageBitmap`
  precedent) — READ-ONLY context, not allowlist.

## 12. Allowlist + custody (diff-derived sum-check at phase end — skill law)

MAY EDIT (code): `src/app/client/substrate/scene_tape.cljc` ·
`src/app/client/substrate/webgpu/renderer.cljs` ·
`src/app/client/workspace/scene_store.cljc` ·
`src/app/client/workspace/rect_tree.cljc` ·
`src/app/client/workspace/scene_runtime.cljs` ·
`src/app/client/substrate/webgpu/gpu_budget.cljs` ·
`src/app/client/substrate/webgpu/verifier.cljs` ·
`test/render_engine/verify_scene_tape_fence.mjs` ·
`test/render_engine/run_verifier.mjs` ·
`test/app/fixtures/render_engine/gpu-goldens/manifest.json` +
`environment.json` (append/metadata only — existing PNG bytes untouched) ·
`test/app/test_runner.clj` (tier entries only).
MAY CREATE: `src/app/client/substrate/image_material.cljc` ·
`test/render_engine/verify_image_atom_receipt.mjs` (G12 assertion only; direct
`node` invocation, no `package.json` edit) · new test
namespaces (`test/app/client/substrate/image_material_test.clj`,
`image_citizenship_test.clj` or equivalent) · new golden PNGs under
`gpu-goldens/` · fixture images + the deterministic JVM generator script
under `test/app/fixtures/render_engine/images/` (§8 fixture-provenance
law — no npm dependency, no `package.json` edit) · package docs under
`build/render-engine/`. (T10's producer-purity check lives inside the
scene-tape fence — no new script wiring.)
MUST NOT TOUCH: `ground.cljs` · `face_primitives.cljc` · `text_layout.cljc` ·
`editor_compute.cljs` · `runtime/mouse.cljs` · `runtime/render.cljs` ·
`electric_flow.cljc` · `buffer_pool.cljs` (READ-ONLY context — ruling Q6;
its API is consumed, never edited) · `containers.cljc` · any server file ·
the 21 existing golden PNGs · `verify_text_layout_fence.mjs` ·
`verify_shaping_correction_fence.mjs` · `profile_shaping_correction.mjs` ·
any `SEAM-STEP1*` or `shaping-correction/*` doc;
`src/app/server/env.clj` is never read.
The studio archive is READ-ONLY reference (§3).
Worktree discipline: implementer works from a clean worktree at the
post-shaping-correction HEAD; worktree branches are scaffolding, never
commit targets; NO commits by the implementer (Sid's ruling at gate); code
and docs commit separately on `docs/current-mental-model-local`; nothing is
ever pushed or merged.

## 13. Stop clauses

- **S1** — the material/durability seam cannot hold (a receipt genuinely
  requires durable bytes storage beyond the digest-keyed registry) →
  coordination ruling (the artery custody is Sid's).
- **S2** — the wave cannot land inside §12's allowlist (needs a MUST-NOT
  file or a store schema change beyond the §7 additions) → ruling.
- **S3** — a §11 manifest symbol is missing in SUBSTANCE (not mere line
  drift) → stop; hint drift re-locates + logs, never stops.
- **S4** — the verifier's RED contract breaks under ORDINARY invocation
  (existing byte diff, MSDF count change, classification/exit flip,
  fingerprint mismatch at Act 0) → stop; never edit the verifier to make
  it pass. `--assert-image-contract`'s exit 0 is NOT an exit flip (the
  exit law binds per invocation mode — R3 N8).
- **S5** — Contract C proves unsatisfiable for any receipt (a genuine
  exactly-one-transfer impossibility under the declared candidate chain,
  OR the Act-0 ICC probe shows this UA's `createImageBitmap` cannot
  perform the declared ingress conversion), with the receipt/probe
  demonstrating it → ruling with the receipt attached.
- **S6** — two binding docs genuinely conflict (both readings physically
  implementable) → escalation per the skill; the implementer never
  improvises policy.

## 14. Process + handoff

- **One phase, few-and-large** (2026-07-29 ruling): the whole package in one
  fresh implementer context (Codex lane), no PLAN.md. This contract carries
  plan-grade specificity instead.
- **Validation: the ladder is COMPLETE and SPENT** — R1 FAIL → recut · R2
  FAIL → recut · R3 (W1-conformance) ∥ the one external Codex cycle →
  merged Q1–Q7 packet → the 2026-08-05 ruling session's third recut (this
  document). Per Sid's ruling, NO further validation rounds run;
  implementation opens directly and the FULL-tier gate carries the
  remaining risk. Round artifacts never overwritten.
- **In-phase falsifier (skill law — ONE finder):** aimed at the genuinely
  new machinery: the image producer's sub-draw derivation + registered
  executor order, the candidate color chain (ingress/encode placement),
  and the lifecycle/budget refusal paths.
- **Act 0 (implementer, before any code):** §11 manifest sweep (grep -n
  every symbol; drift-log) + the §8 boot-verify baseline + cljs warning
  baseline capture + the §8 ICC decode probe + the G1 five-family
  registration-value EDN snapshot + a test-tree grep for pinned part-rank
  enumerations (the shared-surface law: whoever moves a pinned surface
  updates the scan — expected clean, since no existing lane's rank moves).
- **Gate: FULL tier** — first new-atom admission through the seam (the
  seam's first falsifier). Independent session: full suite re-run, full
  verifier replay, fence re-run, receipt cross-check, falsification pass per
  the CLAUDE.md protocol.
- **After green:** Sid's commit ruling (code and docs separate); post-commit
  HEAD-dynamic suite re-run (skill law); board flip; retro joins the batch
  boundary (2026-07-27 cadence ruling — no per-package retro).
- **Then:** the felt-activation obligation stays staged behind the artery
  ruling (board carries it); the path atom's contract opens on this wave's
  landing (D2 + E0 joins already satisfied).
