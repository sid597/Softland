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
(map: `IMAGE-ATOM-RECUT-LEDGER-R2.md`). Binding after a validation round
PASSES over the recut.
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
This staging is recorded, not waived.

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
  (quad edge; exact boundary = semantic hit, Contract O resolves overlap).
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
- `:pick` — `{:policy :interior :boundary :hit :hit-slop {:metric :screen-px
  :radius <declared>} :owner <semantic-instance-route>}`. Geometric only
  (§2 refusal of alpha-aware). **Product-pick route, declared (R1 F3):**
  the PRODUCT read is the existing store route — the image node is an
  rt-node whose rect is hit-tested in container-local space by
  `rt/hit-test` through `scene_store.cljc/pick`'s inverse-affine
  (rect_tree.cljc:359; scene_store.cljc:305-328); under W2-A the
  container-local AABB test IS the transformed-quad interior test. This is
  the same declared product-read/candidate-geometry relationship text
  already wears (the W0-A bounds-pick divergence sentinel is the
  precedent); the candidate quad classification is receipted by the
  verifier parity probes (G4), not by a new dispatch. **A central
  `:geometry`-keyed pick branch in `scene_store.cljc/pick` is REFUSED**
  (O6; the fence's spirit extends to pick) — no new pick dispatch
  mechanism is introduced in this wave.
- `:derivations` — `[{:kind :mips ...} {:kind :atlas ...}]`, each keyed by
  source digest + algorithm-version + regime (deterministic reconstruction
  and invalidation, W1 §4.1 closing law).
- `:regimes` — a COMPLETE non-gapped partition of legal zoom `[0.01, 1000]`
  (Q2 binds every family; W1 §9.2 gate 4), with the floor-default `[0.1, 8]`
  reported as its own ordinary regime. Instance coordinates ride f32 through
  the W2-A transport; the regime table declares coordinate precision,
  coverage format (`rgba8unorm` texel path), mip-selection behavior per zoom
  band, and backend class. No unqualified precision claim survives.

**Contract M citizenship** (template W1 §5.1): all fields supplied; the
receipt set M1–M12 maps onto gates in §10. M9 export: this wave PROMISES NO
export projection — the `:export-projections` field says exactly that
(refusal as extension point; W4 owns export seeds). M7 lifecycle: the same
semantic identity moves hand → settle → scene with no undeclared visual/
pick/order discontinuity — receipted at the store/tape level (§7), since no
authoring hand exists yet; the receipt says so honestly.

**Contract C color citizenship** (W1 §8):
- Every image texture declares color space, channel meaning, and alpha
  association (`straight` | `premultiplied` | `opaque`); mis-tagged fixture
  inputs fail visibly (C5).
- Decode/transfer into linear happens EXACTLY ONCE at resource ingress;
  premultiplication after coverage and effective opacity are known
  (W1 §8.1.3).
- **Two presentation regimes, both DECLARED — the strict law binds the
  candidate seam (R1 F4).** Validation R1 traced the code truth: the legacy
  direct-present regime declares `:transfer :legacy-none` and works in
  presentation-encoded space (scene_tape.cljc:19-29; renderer's legacy
  shader branch performs zero transfers), and the candidate seam's declared
  `:presentation-transfer :linear-to-output-once` has NO code-real encode
  anywhere in the tree. A single strict "exactly-one-transfer on both
  modes" gate is therefore unsatisfiable and is NOT this contract's law.
  The law is:
  - **Candidate regime (strict):** with `:scene-color-enabled? true` in
    fixture (init systems take `:scene-color` directly — no
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
    `runtime/render.cljs` edit. Zero behavior change when disabled is
    proven by G2's byte-identity over the 21 goldens (made assertable per
    §8's cascade repair). The ingress transfer road (`*-srgb` texture
    sample vs `*unorm` + shader transfer) remains the implementer's
    DECLARED choice, proven by C4 sentinels: exactly one ingress
    conversion and exactly one presentation encode, never zero or two.
  - **Legacy direct-present regime (declared passthrough):** sRGB-tagged
    sources pass through in presentation-encoded space — a declared
    ZERO-transfer row (the regime's own declaration makes it lawful,
    Contract C §8.2.5 shape), with a no-DOUBLE-transfer sentinel (proving
    the image path did not decode into a regime that never re-encodes).
    The known legacy alpha defect (`src-alpha` alpha accumulation, W1 §1)
    is the regime's recorded property, not this family's to repair.
  - C6 records BOTH regime rows; inter-regime divergence is a RECORDED
    known difference (they are different declared color regimes), never
    silently equated. The present live pipelines are migration input, not
    precedent (W1 §8.2 closing) — copying their unassociated convention
    into the image pipeline WITHOUT the declaration above is a C-receipt
    failure, not a style choice.

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
- **Bind groups/batching**: batches split by texture binding (atlas page or
  dedicated texture); batching merges ONLY order-contiguous compatible
  entries (Contract O batching law) — an atlas does not license reordering
  transparent/pickable entries for bind-group convenience. **Mechanism
  (R2 N6):** one `pool-info` maps to one bind group and one count key, so
  the existing `store-pool-entries` helper CANNOT express per-binding
  splits — the image PRODUCER derives order-contiguous per-binding sub-runs
  from the stamped ops itself (producers are arbitrary registered fns;
  `store-pool-entries` is precedent, not the required mechanism), minting
  one batch entry per (vi, binding-run) with unique entry ids and correct
  per-run instance offsets. Day-one simplification lever: a single shared
  atlas page + few dedicated textures keeps run counts small.
- **Budget/lifecycle (M10)**: every texture registers with the gpu-budget
  tracker pricing the FULL mip chain (T11); destroy paths mirror the
  existing `destroy-*-font-resources!` shape; asset-unavailable behavior is
  NAMED: an unresolvable digest renders a declared placeholder paint
  (deterministic, receipted) and reports through the existing error lane —
  never a silent skip, never a throw in the frame body. Device-loss:
  resources rebuild from the registry (digest-keyed reconstruction is the
  invalidation law working as designed); machine-driven receipt (G9).

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
  `{:image/digest … :image/crop …}`; presence-driven emission.
  `flatten-ops` (:46) gains `:images`; `stamp-ops-container` (:55) stamps
  them. The payload derivation + count/order maps
  (`:ops-count-by-vi`/`:order-by-vi`, scene_runtime.cljs:391-412) extend
  with the `:images` key, same shape as `:rects`/`:shadows` — a NAMED
  deliverable (R1 F1) — and **the pure derivation (ordered slots → payload
  + count/order maps) is EXTRACTED into a `.cljc` home** (`scene_store.cljc`
  or a new pure namespace; both in-allowlist), with `scene_runtime.cljs`
  keeping only the thin Missionary wrapper. R2 N1's trace: scene_runtime is
  `.cljs`, so without this extraction G8's JVM receipt over the payload leg
  is unexecutable. The extraction is behavior-identical and covered by the
  existing suites + G2.
- **Paint citizenship lives in the RENDERER arrangement** (where every
  family's citizenship already lives): the registered image producer mints
  `[:frame/store vi :images]` batch entries (precedent `store-pool-entries`
  renderer.cljs:1967-1990) with the slot's `:stack-path` and an image
  `:part-rank`, plus dedicated/atlas bind-group batches split
  order-contiguously. Entry ids are unique per (vi, kind) — the frame
  twin-check (`compile-frame-tape` vs `!frame-arrangement`) covers them.
  Image batches paint in tape order through the family-blind executor.
- **Pick**: the product read is §5's declared route — image nodes are
  hit-testable rt-nodes inside the slot tree; cross-slot overlap resolves
  by the maintained view's reverse order exactly as today; within-slot
  resolution is `rt/hit-test`'s traversal, which is the CURRENT product
  law for rect-vs-text already (the W0-A divergence sentinel names this
  product-vs-candidate relationship; the candidate quad truth is receipted
  by G4's verifier probes). No `pick-reverse` change, no new dispatch.

The end-to-end authoring join (ground → store with real image material by
hand) is the felt wave's; THIS wave receipts the op lane + store payload at
the .cljc/JVM level (fixture trees through the real store API), the paint
citizenship at the renderer-arrangement level (twin-check + fence), and the
GPU truth through the verifier (real renderer, real WebGPU). Stated
honestly in the receipts.

## 8. Verifier extension — the bank appends, RED is preserved
(RECUT after R1 M1–M4, M11: the mechanics are named, not assumed)

- `verifier.cljs` gains image cases: at minimum TWO fixtures — (a) an opaque
  sRGB image placed via the atlas, (b) an alpha PNG on a dedicated texture
  with mips — across the SEVEN standing zoom regimes (zoom-cases :31-38):
  14+ new goldens appended to the bank with the same
  raw/png sha256 + determinism (2× render) discipline.
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
  image-parity results are PRINTED in the stdout receipt and written to the
  receipt file; (c) the everything-green-except-MSDF state still yields
  exactly `candidate-pick-parity-failure`, exit 1. **Accounting isolation
  (R2 N5):** image cases form their OWN case family with their OWN receipt
  fields (image-goldens N/N, image-parity per regime); they NEVER join the
  existing sentinel/divergence/parity/updateAuthorized accounting — the
  pinned invariants (divergence sentinels 7/7 · candidate parity 14/21 ·
  MSDF 47/regime) are untouched by construction, not by hope.
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
  shape as the existing candidate-parity probes. Image parity must be
  DECISIVE-PASS **with declared floors (R1 F5): `boundary-pixel-count > 0`
  and `decisive-count > 0` per regime** — a zero-sample "pass" is a Cg
  defect (§5 ramped-edge obligation). It must NOT alter the run's
  classification, which stays `candidate-pick-parity-failure` exit 1 — RED
  exactly on the preserved MSDF counterexample (47 decisive mismatches per
  regime). A run where MSDF goes green, image parity fails silently, or
  the exit code flips is a STOP (S4).
- The 21 existing PNGs: byte-identical, asserted by the run itself. Bank
  extension changes manifest metadata (`productionInputs.rendererSource`
  refresh after renderer edits is LAWFUL and required — precedent: the
  shaping-correction S4 metadata-only migration; image rows append) — never
  existing image bytes. An image-bank update cannot bless any geometry
  disagreement (W1 §4.6).
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
  pixels for the profiled fixture (G6).
- Expected receipt-count changes (deterministic N/N, golden N/N grow by the
  appended rows) are pinned in the phase artifact with before/after values.
- **Executable expected-RED assertion mode.** `run_verifier.mjs` gains
  `--assert-image-contract`. Ordinary invocation retains the standing exit-1
  semantics. Assertion mode exits 0 IFF the only red is the preserved MSDF
  candidate-pick counterexample AND it has machine-asserted every G2/G3/G4
  field: the exact 21-row legacy custody, appended image rows, decisive image
  parity floors per regime, Q5/Q8, 7/7 divergence sentinels, exact MSDF
  mismatch/tie counts, `sourceMatch`, `environmentMatch`, and the full golden
  comparison. It also asserts the attestation-law fields: adapter identity,
  `isFallbackAdapter`, and a non-empty renderer string. A missing field is a
  failure; printing it is not an assertion.
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
  `*-srgb` then re-encode implicitly. Failure: exactly-one-transfer violated
  (zero or two conversions), dark/bright fringe on non-black backgrounds.
  Ruling: declared road + C4 sentinels on both presentation modes + C2
  fringe receipt.
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
- `[RENDER-ASSERT]` run `[RENDER-BUILD]`, then
  `node test/render_engine/run_verifier.mjs --assert-image-contract`.
- `[SCENE-FENCE]`
  `node test/render_engine/verify_scene_tape_fence.mjs`.
- `[TEXT-FENCES]` `npm run verify:text-layout`, then
  `node test/render_engine/verify_shaping_correction_fence.mjs`.
- `[PHASE-RECEIPT]`
  `node test/render_engine/verify_image_atom_receipt.mjs docs/current-mental-model/build/render-engine/IMAGE-ATOM-P1.md`.
  The receipt verifier ASSERTS every G12 field, the diff-derived changed-file
  partition, the numeric gate floors, and locator status; it never merely
  prints fields for inspection.

- **G1 admission** `[SCENE-FENCE] [RENDER-ASSERT]` — `validate-family!` passes for the image family;
  `frame-contract-registry` load-time coverage law holds (boot does not
  throw); fence green with image in the families array; seeded
  central-branch self-test still rejects. Owner: implementer; gate re-runs.
- **G2 zero-regression** `[RENDER-ASSERT]` — full verifier replay: existing 21 goldens
  byte-identical (printed + classification-visible per §8's cascade
  repair — R2 N3 made the bare claim assertable) · SDF 7/7 · Slug 7/7 ·
  MSDF exactly 47 decisive mismatches + 2 ties per regime · divergence
  sentinels 7/7 · classification `candidate-pick-parity-failure`, exit 1.
  Owner: implementer; gate re-runs independently.
- **G3 image goldens** `[RENDER-ASSERT]` — the appended image cases: determinism 2× ·
  raw/png sha256 recorded · both fixtures across all seven regimes render
  non-trivially (a blank capture is a harness bug until op counts disagree —
  the framework W2-INT rule: assert op-count parity FIRST).
- **G4 image pick parity** `[RENDER-ASSERT]` — the geometry probe renders the quad with
  OPAQUE WHITE REFERENCE PAINT saturating the compared red channel, or
  records coverage in a separate coverage-only attachment (Cg isolation —
  W1 §4.3's own law; R2 N4 traced that the probe reads channel 0, so a
  textured/colored quad would satisfy floors while comparing garbage; the
  texture is a Cv factor and is EXCLUDED from the geometry probe).
  DECISIVE-PASS across the seven regimes **with floors:
  `boundary-pixel-count > 0` AND `decisive-count > 0` per regime, both
  reported in the receipt** (a zero-sample pass is a FAIL — R1 F5);
  byte-128 ties reported as ties (`:half`), never votes. Textured content
  correctness is the goldens' job (G3), never the geometry probe's.
  Overlap receipts run on the PRODUCT route (§5/§7): cross-slot image/rect
  overlap returns topmost by the maintained view's reverse order through
  the real `scene_store.cljc/pick`; within-slot overlap follows the tree
  traversal law, receipted as the declared product-vs-candidate
  relationship (divergence-sentinel shape). (O2 extension, honestly
  scoped.)
- **G5 order receipts (tiers explicit — R2 N1)** `[JVM-FULL]
  `[RENDER-ASSERT]` — O1 shuffle-determinism
  (registry insertion + map construction shuffled → same order hash) runs
  at the TAPE level on JVM (`scene_tape.cljc` is `.cljc`) with the image
  family registered. O5's batching falsifier (randomized registration →
  byte-identical pixels + pick identities) is a BROWSER/verifier receipt.
  Neither leg claims the other's tier.
- **G6 color receipts (two regimes per §5 — R1 F4)** `[JVM-FULL]
  `[RENDER-ASSERT]` — CANDIDATE seam
  enabled in fixture: C1 reference source-over for an image over
  opaque/transparent grounds vs CPU linear-premultiplied reference · C2 no
  fringe on non-black background (new capture path, §8) · C4 strict
  sentinels: exactly one ingress transfer + exactly one presentation encode
  (the encode made code-real behind the default-OFF seam), including the
  profiled fixture (embedded-profile PNG whose post-ingress pixels are
  pinned) · C5 straight-vs-premultiplied fixture pair converts to the same
  scene value; mis-tagged fixture fails visibly. LEGACY direct-present:
  the declared zero-transfer passthrough row + the no-double-transfer
  sentinel. C6: every enabled texture/format records its regime row, BOTH
  presentation regimes included; inter-regime divergence recorded, never
  equated.
- **G7 resource laws (JVM)** `[JVM-FULL]` — mip chain math (counts + sizes) · atlas
  allocator invariants (no overlap, padding, deterministic placement) ·
  placement-tier rule (small→atlas, large→dedicated, overflow→dedicated) ·
  ingress refusal fixtures (untagged, digest mismatch) · M1 grammar
  round-trip + unknown-field policy + validator fail-closed (incl. the
  `:image/time` refusal).
- **G8 store/op lane (JVM — recut per §7, R2 N1)** `[JVM-FULL]
  `[RENDER-ASSERT]` — fixture trees through
  the real store API: `tree->images` emits from image rt-nodes;
  `flatten-ops`/`stamp-ops-container` carry `:images` with container
  stamps; the EXTRACTED `.cljc` payload derivation carries the `:images`
  key in `:ops-count-by-vi`/`:order-by-vi` without disturbing
  rect/shadow/text counts (asserted on JVM against the extracted fn);
  `slot-entry`/`maintained-entries`/maintained ≡ batch-oracle equivalence
  are UNCHANGED and re-asserted (the existing suites stay green);
  `scene_store.cljc/pick` resolves an image node topmost across
  overlapping slots by reverse maintained order. The renderer-side twin
  check with image batch entries present is a BROWSER receipt (verifier
  run), not a JVM claim.
- **G9 lifecycle/budget** `[JVM-FULL] [RENDER-ASSERT]` — both texture registration and replacement price
  full mip chains (one fixture asserted numerically through both roads);
  destroy paths run; device-loss rebuild receipt
  (registry-driven reconstruction) machine-driven; asset-unavailable renders
  the declared placeholder deterministically.
- **G10 suites** `[JVM-FULL] [RENDER-BUILD]` — full JVM suite green at the final bytes (FULL-tier gate;
  the runner tier entry for new namespaces is an allowlisted +1); cljs
  compile 0 new warnings against the Act-0 captured baseline.
- **G11 fences** `[SCENE-FENCE] [TEXT-FENCES]` — scene-tape fence green with the image id in both slices
  (T12 conditions, incl. the strengthened real-slice self-test); the T10
  producer-purity check exists INSIDE the scene-tape fence and passes;
  text-layout + shaping fences untouched and green.
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
atlas-vs-dedicated placement) · M3→G5 · M4→G3/G4 · M5→G7 (T9 fixtures) ·
M6→G7 (algorithm-version keys) · M7→G8 (declared store-level scope) ·
M8→G8+G2 (fixture reload + verifier replay) · M9→refusal declared (§5) ·
M10→G9 · M11→G6 · M12→the committed fixture corpus + bank rows.

## 11. Input manifest — substance-binding; locators are hints
(machine-verified 2026-08-05; re-verify at phase open, drift → re-locate +
log, substance missing → S3)

- `src/app/client/substrate/scene_tape.cljc` (545 ln) — `family-ids` :10 ·
  `scene-color-seam` :44 · `registration` :244 · `family-contracts` :285 ·
  `validate-family!` :342 · `register-family` :370 · `paint-forward` :525 ·
  `pick-reverse` :534.
- `src/app/client/substrate/webgpu/renderer.cljs` (2,385 ln) —
  `max-transform-nodes` :618 · `create-msdf-font-resources` :760 (upload
  precedent) · `create-slug-texture` :786 (writeTexture precedent) ·
  `gpu-paint` :1949 · `execute-gpu-batch!` :2157 · `frame-family-registry`
  :2170 · `frame-contract-registry` :2192 · `compile-frame-tape` :2237 ·
  `frame-tape-twin-check!` :2243 · `store-pool-entries` :1967 · system
  constructors :681/:866/:930/:995/:1118 · non-system `init-clear-quad` :1256.
- `src/app/client/workspace/scene_store.cljc` (585 ln) — `flatten-ops` :46 ·
  `stamp-ops-container` :55 · `upsert-slot` :140 · `slot-entry` :227 ·
  `scene-tape` :254 · `rebuild-ordered` :272 · `maintained-entries` :285 ·
  `pick` :305.
- `src/app/client/workspace/rect_tree.cljc` (402 ln) — `rt-node` :12 ·
  `tree->rects` :198 · `tree->text-ops` :259 · `tree->shadows` :330 ·
  `hit-test` :359.
- `src/app/client/workspace/scene_runtime.cljs` (552 ln) — `<store-frame` :382 ·
  `:ops-count-by-vi` :402.
- `src/app/client/substrate/webgpu/gpu_budget.cljs` (334 ln) —
  `register-texture!` :152 · `replace-texture!` :194.
- `src/app/client/substrate/webgpu/verifier.cljs` (805 ln) — `zoom-cases`
  :31 · `render-system-bytes!` :82 · case runner + capture machinery.
- `test/render_engine/verify_scene_tape_fence.mjs` (132 ln) — `families`
  :56.
- `test/render_engine/run_verifier.mjs` (530 ln) — `serveSyntheticOrigin`
  :102 · update authorization :353 · golden compare :400 · manifest rows +
  `--update-goldens`.
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
`electric_flow.cljc` · any server file · the 21 existing golden PNGs ·
`verify_text_layout_fence.mjs` · `verify_shaping_correction_fence.mjs` ·
`profile_shaping_correction.mjs` · any `SEAM-STEP1*` or
`shaping-correction/*` doc; `src/app/server/env.clj` is never read.
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
- **S4** — the verifier's RED contract breaks (existing byte diff, MSDF
  count change, classification/exit flip, fingerprint mismatch at Act 0) →
  stop; never edit the verifier to make it pass.
- **S5** — Contract C proves unsatisfiable under direct-present for any
  receipt (a genuine exactly-one-transfer impossibility, with the receipt
  demonstrating it) → ruling with the receipt attached.
- **S6** — two binding docs genuinely conflict (both readings physically
  implementable) → escalation per the skill; the implementer never
  improvises policy.

## 14. Process + handoff

- **One phase, few-and-large** (2026-07-29 ruling): the whole package in one
  fresh implementer context (Codex lane), no PLAN.md. This contract carries
  plan-grade specificity instead.
- **Validation:** ONE fresh default-fail round over this contract before the
  implementer prompt goes out (FAIL → recut; minor-fail → fixes applied in
  place, no re-run). Round artifacts never overwritten.
- **Act 0 (implementer, before any code):** §11 manifest sweep (grep -n
  every symbol; drift-log) + the §8 boot-verify baseline + cljs warning
  baseline capture.
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
