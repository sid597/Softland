# Image atom P1 — Package 2, wave 1

## Attestation

This is a SwiftShader receipt, not a physical-GPU or product-loop claim.

- Adapter identity: vendor `google` · architecture `swiftshader` · device `""`.
- `isFallbackAdapter`: `true`, obtained from
  `GPUAdapterInfo.isFallbackAdapter`.
- Renderer string: `swiftshader`.
- Browser/runtime: Google Chrome `150.0.7871.46` · Node `v20.20.2` ·
  Puppeteer `15.2.0` · Linux x64 `6.17.0-35-generic`.
- Environment fingerprint:
  `5ced2482f3d0b8e9a14465bba06808a04d3a5a9b7d386fcf71f40a30f557343e`.
- Timing fields are recorded in `target/render-verifier/receipt.json`; no
  timing value is treated as a hardware claim or gate bar.

## Outcome

**STOP S2.** The image-atom implementation and its package-specific browser,
JVM, fence, and fixture receipts are green, but the binding `[JVM-FULL]`
invocation was red in the final gate sequence: 544 tests, 7,619 assertions,
five failures, zero errors. One failure is independently repeatable at the
current bytes in
`app.material-truth-test`: its bank excludes three deterministic `:time-ms`
literals already present in `src/app/client/workspace/ground.cljs`.
`ground.cljs` is explicitly MUST-NOT-TOUCH, and the baseline test is outside
§12. The package therefore cannot make G10—and all gates conjunctively bound
to `[JVM-FULL]`—green inside the allowlist. That is CONTRACT §13 **S2**; no
protected or out-of-allowlist repair was attempted.

This stop does not revoke the implementation receipts below. It means the
package is not presented as landed, closed, or ready for Sid's commit ruling.
The independent Fable FULL-tier gate has not opened.

## Opening and Act 0

- Branch: `docs/current-mental-model-local`.
- Opening HEAD: `eb0ef7b4e284d689f4e396a249372fdba3260bad`.
- Opening tree: clean.
- Freeze anchor: contract SHA-256
  `ebd894a58af945b761907decad74f8e7f3d39ea0a744ccadc6ce168900783800`,
  exact.
- §11 manifest sweep: every named symbol existed in substance. The hints had
  broad line drift because the implementation grew the named files; all were
  re-located and are enumerated below. No S3 condition occurred.
- Boot verifier: ordinary exit `1`, classification
  `candidate-pick-parity-failure`, deterministic `21/21`, legacy goldens
  `21/21`, candidate parity `14/21`, divergence sentinels `7/7`, exact
  environment fingerprint. `sourceMatch=false` was the named opening debt.
- Warning baseline: the pre-existing JVM namespace replacement warning only;
  Shadow reported 119 files, zero compiled, zero CLJS warnings.
- ICC probe: the initial ad-hoc expected-raster table was wrong and produced a
  harness false negative. The corrected relational probe proved
  `createImageBitmap` honors the embedded profile: pixel `[1,1]` was raw
  `[40,43,46]` and converted `[110,114,118]`; pixel `[4,2]` was raw
  `[144,147,150]` and converted `[198,200,202]`. The committed assertion uses
  those pinned values and passes.
- Five-family registration-value EDN snapshot SHA-256:
  `581daf7fc7980853e5e71df8aa4b4ad689d6d5a6adb93e4b09a994bb863aed0b`.
  The final JVM assertion retains that exact value.
- Part-rank pin grep: no existing lane-specific rank pin was found in the test
  tree. Final registration asserts shadow `0`, rect `1`, slot-text `2`, image
  `3`, unique.

## Implemented package

The package adds one registered image family with no family switch in the
central executor. One tape entry is emitted per `(vi, :images)` and carries an
ordered binding-contiguous sub-draw vector; the image family's registered
executor walks that vector. The live falsifier observed
`atlas → dedicated → atlas`, first instances `[0,1,2]`, executor draws
`[0,1,2]`, one entry per vi, maintained=batch equivalence, and duplicate-ID
rejection.

Image rt-nodes require their own `[:data :address]`; picking remains the
half-open rect-tree product route with hit slop `0.0` and no per-node transform.
Clip clamps propagate into crop/UV insets. The image instance pool is created by
`init-image-system` and written behind identity gating inside `draw-frame!`'s
encode window. The product frame-loop construction/join remains **staged** and
was neither driven nor claimed.

The candidate color chain has one family declaration and exactly one ingress
conversion plus one presentation encode. Candidate mips filter through sRGB
views in linear space. Seam-off uses `createImageBitmap` conversion `none`,
unorm mip views, and zero transfers; both the ordinary sRGB byte fixture and
the profiled fixture are pinned. Straight and premultiplied source fixtures
converge within one byte; deliberate mistagging diverges by 74 bytes.

Resources use deterministic atlas placement with extruded gutters, dedicated
textures when required, full mip-chain accounting, digest-verified source
registration, and a deterministic placeholder. Device-loss recovery builds a
fresh system on a separately acquired adapter/device and reconstructs all eight
registered digests without reusing device-owned pipeline, atlas, placeholder,
pool, or bind-group resources. The lost system records eight device-loss rows;
replacement histories each contain device-lost then ok. Unavailable and
over-budget sources render byte-identical pinned placeholders with raw SHA-256
`aab20b3aa071f60cd29cbcbc44271364792aa800eb6186042eb0d0ab7e4915e8`;
painting preserves the over-budget refusal cause.

Trap comments T1–T15 are present in the implementation. The archived studio
`image-scene-entries` per-instance shape was not copied.

## Bounded in-phase falsifier

One fresh finder ran, read-only, against the required three questions. Its
sub-draw/executor-order leg passed. It returned exactly these three
decision-changing findings:

1. **HIGH — Seam-off performs an ICC conversion but reports zero transfers.**
   Corrected by seam-dependent bitmap decode and mip formats, then receipted
   through the profiled seam-off fixture.
2. **HIGH — The claimed device-loss rebuild does not rebuild from a lost
   device.** Corrected by a fresh adapter/device/system reconstruction and
   preserved per-digest event histories.
3. **MEDIUM/HIGH — Painting a refused over-budget source erases the refusal,
   and neither placeholder path is pixel-driven.** Corrected by preserving the
   refusal row, recording placeholder rendering, and byte-driving both reasons
   against the pinned placeholder digest.

No second finder or additional review round ran.

## Verifier receipts — before and after

| Receipt | Act 0 | Final |
|---|---:|---:|
| ordinary exit / classification | `1` / `candidate-pick-parity-failure` | `1` / `candidate-pick-parity-failure` |
| legacy deterministic captures | `21/21` | `21/21` |
| legacy golden bytes | `21/21` | `21/21` |
| legacy candidate parity | `14/21` | `14/21` |
| legacy divergence sentinels | `7/7` | `7/7` |
| image golden bytes | absent | `21/21` |
| image deterministic captures | absent | `21/21` |
| image product/candidate parity | absent | `14/14`, every boundary and decisive floor positive |
| environment match | true | true |
| source match | false, named metadata debt | true, lawful metadata refresh only |
| assertion invocation | absent | exit `0`, assertion set green, classification unchanged |

The metadata refresh changed manifest hashes/rows only after exact `21/21`
legacy byte and environment preflight. None of the 21 pre-existing PNG files
changed. The RED MSDF counterexample remains exactly 47 decisive mismatches and
two ties per regime.

## Gate matrix

- G1: **BLOCKED-BY-JVM-FULL**; family admission, load-time coverage, scene
  fence, five-family value snapshot, and render assertion are individually
  green.
- G2: **PASS** — ordinary RED and assertion-mode exit laws, old bytes/counts,
  source/environment matches, 47+2 MSDF counterexample, and 7/7 sentinels.
- G3: **PASS** — 21/21 image goldens and 21/21 deterministic rerenders.
- G4: **PASS** — 14/14 equality-by-construction rows; every boundary and
  decisive floor is positive; no slop path ran.
- G5: **BLOCKED-BY-JVM-FULL**; JVM shuffle/rank assertions and browser
  arrangement/sub-draw receipts individually pass.
- G6: **BLOCKED-BY-JVM-FULL**; candidate, ICC, alpha, presentation, and
  seam-off browser receipts individually pass, and the package JVM assertions
  emitted no failure.
- G7: **BLOCKED-BY-JVM-FULL**; all material/resource/corpus JVM assertions ran
  without a package failure.
- G8: **BLOCKED-BY-JVM-FULL**; store/op/address/crop/identity JVM assertions and
  browser arrangement receipt ran without a package failure.
- G9: **BLOCKED-BY-JVM-FULL**; budget math package assertions and browser
  placeholder/refusal/replacement-device receipts pass.
- G10: **BLOCKED-BY-JVM-FULL / STOP S2** — final Shadow build is 121 files,
  three compiled, zero CLJS warnings; full JVM is five failures, zero errors.
- G11: **PASS** — scene fence: six families, zero branches, strengthened seeded
  test, producer purity, registered image executor, absent rt-node transform;
  text-layout and shaping-correction fences pass untouched.
- G12: **PASS-FOR-STOP-RECEIPT** — this artifact and its direct Node verifier
  assert the diff partition, numeric floors, fixture digests, stop evidence,
  and locator status.

### `[JVM-FULL]` stop receipt

`clj -X:test full :shards 1` exited `1` after 544 tests and 7,619 assertions:
7,614 pass, five fail, zero error. The new namespaces
`app.client.substrate.image-citizenship-test` and
`app.client.substrate.image-material-test` ran without a reported failure.
After the §12 sum-check removed an accidental existing-test diff, the two new
namespaces plus the unchanged `app.client.substrate.scene-tape-test` reran as
17 tests and 164 assertions with zero failures or errors.
Observed failing namespaces were unrelated and untouched:
`app.face-transcription-test`, `app.material-truth-test`, and
`app.server.rama.dogfood-compute-test`.

The decisive repeat probe was:

`clj -M:test -e "(require 'app.material-truth-test) ..."`

It independently produced 15 tests, 252 assertions, one failure, zero errors:
the banked deterministic-time inventory omits three `:time-ms` literals in
`src/app/client/workspace/ground.cljs`. That protected path has no worktree
change. Making this gate green requires either changing the MUST-NOT path or an
out-of-allowlist baseline/test ruling, so S2—not an improvised exception—is the
terminal result.

## Fixture digests

- `alpha-reference-straight.png` —
  `b38bcacf6298ec057c4e1b03fefa2116440d8bdd2ea31bb79dcd94396856d608`
- `atlas-opaque-srgb.png` —
  `6e744e448e1480b510b3cd8aef86b5e2c9b8367bbafdb3e1586946c779b265eb`
- `clip-stripes-srgb.png` —
  `8000e248f9550d364286a5b873a77813040762998f087f7521f0900532730c15`
- `coverage-white-srgb.png` —
  `71bafd65a2358f69ab1e3086058fa4180760a54f87b0b58cd75579b3ed74d81e`
- `dedicated-alpha-premultiplied.png` —
  `28840b6c70cb6dcd01f2f4ae0304e9c38070fcb2c1505f25d03111b158b6f30b`
- `dedicated-alpha-straight.png` —
  `c859086c6a2cd8171f4cd429fd9d62529dd97c53e5ae9f131ce216b90af7e402`
- `profiled-linear-rgb.png` —
  `8e0358b9e3830abfdbff58faf3a19907d3fd2c2fd4a79d4cc6156acbb09fabea`
- `seam-byte-srgb.png` —
  `836e03f287a153693eecd3b75e4c8ffefbc38407f1e7784a4c2a8f4471f6650a`
- Embedded ICC profile: 488 bytes, SHA-256
  `863e250a92d522afd5968dd276585559292882df06224a4c0b2cfdbda4fdfca0`;
  PNG chunks re-read as `IHDR/iCCP/IDAT/IEND`.

The deterministic JVM generator reruns to the same eight file digests.

## §11 locator re-verification and drift log

All 72 symbol locators asserted by the phase verifier are present in
substance. Current positions (original hint positions are stale, so every
movement below is line drift only):

- `scene_tape.cljc`: `family-ids` 10; `scene-color-seam` 45; `regime` 68;
  `registration` 320; `image-registration` 375; `family-contracts` 394;
  `required-regime-keys` 415; `validate-regimes!` 426; `validate-family!` 452;
  `register-family` 480; `compare-scalar` 503; `compare-order` 537;
  `compile-tape` 620; `paint-forward` 644; `pick-reverse` 653.
- `renderer.cljs`: `max-transform-nodes` 732; container buffer constructor
  734; rect constructor 795; image constructor 1083; camera constructor 1391;
  MSDF upload precedent 1425; slug texture precedent 1451; text constructor
  1660; shadow constructor 1783; clear quad 1921; `frame-order` 2588;
  `gpu-paint` 2614; pool entries 2731 with ranks 0/1/2 and image 3 in the
  adjacent family producers; `execute-gpu-batch!` 2921;
  `frame-family-registry` 2947; `frame-contract-registry` 2974;
  `!frame-arrangement` 2988; production/update/compile 2991/2997/3019;
  twin-check 3025; `draw-frame!` 3060.
- `scene_store.cljc`: `flatten-ops` 46; `stamp-ops-container` 56;
  `upsert-slot` 144; `deepest-addressed` 224; `slot-entry` 231; `scene-tape`
  258; `rebuild-ordered` 276; `maintained-entries` 289; `pick` 341. The new
  pure payload projection is `derive-store-frame` 294.
- `rect_tree.cljc`: `rt-node` 13 remains exactly ten keys with no transform;
  `tree->rects` 199; new `tree->images` 260; `tree->text-ops` 329;
  `tree->shadows` 400; `hit-test` 429.
- `scene_runtime.cljs`: `<store-frame` 382 now delegates to the pure projection;
  `ops-count-by-vi` and lane-agnostic `order-by-vi` live there.
- Read-only context reverified: `containers.cljc` `effective` 221 and
  `inverse-point` 252; `buffer_pool.cljs` `create-pool` 111 and
  `pool-draw-info` 354; `runtime/fonts.cljs` bitmap precedent 61.
- `gpu_budget.cljs`: texture register 167 and replace 210.
- `verifier.cljs`: `zoom-cases` 34, original system capture 103, original case
  runner 484, and image runner 1335.
- `verify_scene_tape_fence.mjs`: families 63. `run_verifier.mjs`: synthetic
  origin 104, production inputs 171, comparison 527, classification 687,
  stdout 770, and scoped append authorization in the 600-area.
- Test runner inventory/full, package scripts, shaping fence, environment bank,
  21 legacy bank rows, and binding/context documents were present. The image
  manifest now has a separate 21-row image section; legacy filename set and
  bytes remain exact.

No locator was missing in substance and no contradiction with the baton was
found.

## Diff-derived §12 sum-check

The machine block below is compared against `git status --porcelain=v1 -uall`
by `[PHASE-RECEIPT]`. The allowed partition contains only the §12 code files,
the named tests and receipt verifier, deterministic fixtures/generator, 21 new
`gpu-image-atom-*` goldens, manifest metadata, and package docs. It rejects any
changed legacy golden PNG, protected path, server file, text/shaping fence,
`package.json`, or read-only context file.

## Staged residue

The image-above-text kind-layer default is recorded as `true` so the felt wave
inherits it consciously. This remains a dark wave: no authoring surface, no
felt gate, and no product frame-loop claim. Physical-GPU timing and product
activation remain staged obligations.

<!-- IMAGE_ATOM_RECEIPT_JSON
{
  "schemaVersion": 1,
  "status": "STOP-S2",
  "opening": {
    "branch": "docs/current-mental-model-local",
    "head": "eb0ef7b4e284d689f4e396a249372fdba3260bad",
    "clean": true,
    "freezeAnchorMatched": true
  },
  "attestation": {
    "vendor": "google",
    "architecture": "swiftshader",
    "isFallbackAdapter": true,
    "renderer": "swiftshader",
    "environmentFingerprint": "5ced2482f3d0b8e9a14465bba06808a04d3a5a9b7d386fcf71f40a30f557343e"
  },
  "before": {
    "ordinaryExit": 1,
    "classification": "candidate-pick-parity-failure",
    "determinism": "21/21",
    "legacyGoldens": "21/21",
    "candidateParity": "14/21",
    "sentinels": "7/7",
    "sourceMatch": false
  },
  "after": {
    "ordinaryExit": 1,
    "assertExit": 0,
    "classification": "candidate-pick-parity-failure",
    "determinism": "21/21",
    "legacyGoldens": "21/21",
    "candidateParity": "14/21",
    "sentinels": "7/7",
    "sourceMatch": true,
    "imageGoldens": "21/21",
    "imageDeterminism": "21/21",
    "imageParity": "14/14"
  },
  "fixtureDigests": {
    "alpha-reference-straight.png": "b38bcacf6298ec057c4e1b03fefa2116440d8bdd2ea31bb79dcd94396856d608",
    "atlas-opaque-srgb.png": "6e744e448e1480b510b3cd8aef86b5e2c9b8367bbafdb3e1586946c779b265eb",
    "clip-stripes-srgb.png": "8000e248f9550d364286a5b873a77813040762998f087f7521f0900532730c15",
    "coverage-white-srgb.png": "71bafd65a2358f69ab1e3086058fa4180760a54f87b0b58cd75579b3ed74d81e",
    "dedicated-alpha-premultiplied.png": "28840b6c70cb6dcd01f2f4ae0304e9c38070fcb2c1505f25d03111b158b6f30b",
    "dedicated-alpha-straight.png": "c859086c6a2cd8171f4cd429fd9d62529dd97c53e5ae9f131ce216b90af7e402",
    "profiled-linear-rgb.png": "8e0358b9e3830abfdbff58faf3a19907d3fd2c2fd4a79d4cc6156acbb09fabea",
    "seam-byte-srgb.png": "836e03f287a153693eecd3b75e4c8ffefbc38407f1e7784a4c2a8f4471f6650a"
  },
  "locators": {
    "status": "all-substance-present",
    "reverifiedCount": 72,
    "driftLogged": true
  },
  "gates": {
    "matrix": {
      "G1": "BLOCKED-BY-JVM-FULL",
      "G2": "PASS",
      "G3": "PASS",
      "G4": "PASS",
      "G5": "BLOCKED-BY-JVM-FULL",
      "G6": "BLOCKED-BY-JVM-FULL",
      "G7": "BLOCKED-BY-JVM-FULL",
      "G8": "BLOCKED-BY-JVM-FULL",
      "G9": "BLOCKED-BY-JVM-FULL",
      "G10": "BLOCKED-BY-JVM-FULL-STOP-S2",
      "G11": "PASS",
      "G12": "PASS-FOR-STOP-RECEIPT"
    },
    "jvmFull": {
      "exit": 1,
      "tests": 544,
      "assertions": 7619,
      "passes": 7614,
      "failures": 5,
      "errors": 0,
      "packageNamespacesPassed": true,
      "blockingPath": "src/app/client/workspace/ground.cljs",
      "blockingPathChanged": false
    },
    "renderBuild": {"exit": 0, "files": 121, "compiled": 3, "cljsWarnings": 0},
    "renderRed": {"exit": 1, "classificationPreserved": true},
    "renderAssert": {"exit": 0, "assertionsGreen": true},
    "sceneFence": {"exit": 0, "families": 6, "centralBranches": 0},
    "textFences": {"textLayoutExit": 0, "shapingCorrectionExit": 0}
  },
  "falsifier": {
    "findings": [
      {
        "finding": "HIGH — Seam-off performs an ICC conversion but reports zero transfers.",
        "disposition": "corrected-and-receipted"
      },
      {
        "finding": "HIGH — The claimed device-loss rebuild does not rebuild from a lost device.",
        "disposition": "corrected-and-receipted"
      },
      {
        "finding": "MEDIUM/HIGH — Painting a refused over-budget source erases the refusal, and neither placeholder path is pixel-driven.",
        "disposition": "corrected-and-receipted"
      }
    ],
    "subDrawExecutorOrder": "PASS"
  },
  "stop": {
    "clause": "S2",
    "reason": "Binding JVM-FULL is red and the repeatable blocker requires a MUST-NOT or out-of-allowlist change."
  },
  "imageAboveTextKindLayerDefault": true,
  "productLoopClaim": false,
  "productLoopJoin": "staged",
  "darkWave": true,
  "feltGate": false,
  "changedFiles": [
    "docs/current-mental-model/build/render-engine/IMAGE-ATOM-NOW.md",
    "docs/current-mental-model/build/render-engine/IMAGE-ATOM-P1.md",
    "src/app/client/substrate/image_material.cljc",
    "src/app/client/substrate/scene_tape.cljc",
    "src/app/client/substrate/webgpu/gpu_budget.cljs",
    "src/app/client/substrate/webgpu/renderer.cljs",
    "src/app/client/substrate/webgpu/verifier.cljs",
    "src/app/client/workspace/rect_tree.cljc",
    "src/app/client/workspace/scene_runtime.cljs",
    "src/app/client/workspace/scene_store.cljc",
    "test/app/client/substrate/image_citizenship_test.clj",
    "test/app/client/substrate/image_material_test.clj",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-alpha-dedicated-default-max-z8.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-alpha-dedicated-default-min-z0p1.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-alpha-dedicated-default-unit-z1.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-alpha-dedicated-legal-log-z10.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-alpha-dedicated-legal-log-z100.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-alpha-dedicated-legal-max-z1000.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-alpha-dedicated-legal-min-z0p01.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-opaque-atlas-default-max-z8.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-opaque-atlas-default-min-z0p1.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-opaque-atlas-default-unit-z1.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-opaque-atlas-legal-log-z10.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-opaque-atlas-legal-log-z100.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-opaque-atlas-legal-max-z1000.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-opaque-atlas-legal-min-z0p01.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-partially-clipped-default-max-z8.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-partially-clipped-default-min-z0p1.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-partially-clipped-default-unit-z1.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-partially-clipped-legal-log-z10.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-partially-clipped-legal-log-z100.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-partially-clipped-legal-max-z1000.png",
    "test/app/fixtures/render_engine/gpu-goldens/gpu-image-atom-partially-clipped-legal-min-z0p01.png",
    "test/app/fixtures/render_engine/gpu-goldens/manifest.json",
    "test/app/fixtures/render_engine/images/alpha-reference-straight.png",
    "test/app/fixtures/render_engine/images/atlas-opaque-srgb.png",
    "test/app/fixtures/render_engine/images/clip-stripes-srgb.png",
    "test/app/fixtures/render_engine/images/coverage-white-srgb.png",
    "test/app/fixtures/render_engine/images/dedicated-alpha-premultiplied.png",
    "test/app/fixtures/render_engine/images/dedicated-alpha-straight.png",
    "test/app/fixtures/render_engine/images/generate_images.clj",
    "test/app/fixtures/render_engine/images/profiled-linear-rgb.png",
    "test/app/fixtures/render_engine/images/seam-byte-srgb.png",
    "test/app/test_runner.clj",
    "test/render_engine/run_verifier.mjs",
    "test/render_engine/verify_image_atom_receipt.mjs",
    "test/render_engine/verify_scene_tape_fence.mjs"
  ]
}
IMAGE_ATOM_RECEIPT_JSON -->
