## 4. Contract G — unconditional geometry truth

### 4.1 Every family declares the relationship

Every drawable registration contains a geometry descriptor. Illustrative
syntax; the obligations are binding:

```clojure
{:geometry/version 1
 :authority {:kind <rounded-box|glyph-outline|centerline-pressure|path|quad|mesh|...>
             :source-id <material-or-asset-id>
             :source-revision <revision>
             :algorithm-version <when semantics require one>
             :local-space <declared-coordinate-space>}
 :classify {:result #{:inside :boundary :outside}
            :fill-rule <nonzero|even-odd|not-applicable>
            :boundary-rule :explicit}
 :coverage {:geometry-operator <aa-filter|stroke-expand|depth|...>
            :operator-version <version>
            :boundary-relation <isocontour-0.5|derived-effect|not-applicable>
            :reference-isocontour <0.5|not-applicable>
            :visual-factors <texture-alpha|mask|effect|opacity declarations>
            :tie-token :half
            :quantization <declared-per-regime>}
 :pick {:policy <interior|centerline-width|layout-cluster|alpha-aware|none>
        :boundary :hit
        :hit-slop {:metric :screen-px :radius <number-or-input-policy>}
        :owner <semantic-instance-id-route>}
 :bounds {:math <derived-from-authority>
          :paint <derived-from-coverage-support>
          :pick <derived-from-policy-and-slop>}
 :derivations [{:kind <outline|mesh|atlas|bands|mips|bvh|...>
                :source-revision <revision>
                :algorithm-version <version>
                :tolerance-lod <declared>
                :normalization <declared>
                :precision <declared>
                :backend <declared>
                :regime <declared>}]
 :regimes [<complete legal-domain partition>]}
```

There is one authority, not necessarily one representation. Derived outlines,
meshes, atlases, acceleration structures, and coverage fields are readers or
caches. Their keys must make reconstruction and invalidation deterministic.

### 4.2 Mathematical classification

1. Classification occurs in the authority's local space after the instance's
   declared inverse affine transform. W2-A migrates affine and its transport
   together; W1 does not create a second transform path.
2. The classifier is tri-state: `:inside`, `:boundary`, or `:outside`. Fill
   rules, open/closed roles, cap/join/dash, hole semantics, glyph outline rules,
   and mesh/depth rules belong to the authority descriptor or its pinned
   algorithm—not to a caller's threshold.
3. Exact mathematical boundary counts as a semantic pick hit. When two entries
   share that boundary, Contract O resolves the winner. A road may use a
   versioned numeric predicate, but it must declare coordinate precision and
   backend regime; it may not silently turn numeric uncertainty into inside.
4. Effects still declare geometry: the source authority, the named coverage
   operator, and finite render support/budget. A shadow normally declares
   `:pick :none`; blur coverage outside the source interior does not make the
   shadow a second pick body.

### 4.3 Visual coverage and the `0.5` isocontour

Coverage has two named stages. **Geometric coverage** `Cg` is the road's
deterministic `[0,1]` projection of the authority through its transform, AA or
stroke operator, filter footprint, LOD, precision, and backend. **Final visual
coverage** `Cv` is `Cg` after declared texture alpha, masks, effects, and other
paint factors. Neither value is material or the base pick predicate. Receipts
isolate `Cg` with opaque, unmasked reference paint before testing the composed
`Cv`; an image's transparent texel or a group's opacity cannot be misreported
as a geometry failure.

For a direct shape/glyph coverage road, `:boundary-relation
:isocontour-0.5` means logical `Cg = 0.5` represents the authority's
mathematical boundary. At a test sample whose mathematical class is not
`:boundary`, `Cg > 0.5` must agree with `:inside` and `Cg < 0.5` with
`:outside`. A pre-registered numeric error bound may measure the road's contour
error, but the bound must derive from declared precision/filter facts before
results; no post-hoc alpha threshold, widened boundary band, golden update, or
ignored mismatch may convert disagreement into a pass.

Coverage may lawfully extend outside an interior through a declared stroke,
blur, texture, mask, or other operator. An effect road declares
`:boundary-relation :derived-effect` and the exact transformation from source
authority to `Cv`; it cannot pretend its blur's `0.5` contour is a second
mathematical boundary. Extended support affects paint bounds, not the authority
or base pick body. Alpha-aware image picking, if a family chooses it, is an
explicit pick policy with its own deterministic sampling/mipmap rule.

### 4.4 Half-coverage and quantization — Q6 adjudicated

Logical half coverage is its own class, `:half`; it is never a Boolean vote.
When exact `8/16 = 0.5` coverage is written through `rgba8unorm`, round-to-nearest
storage produces byte `128`, whose decoded numeric value is `128/255`. **Byte
128 remains the `:half` tie token; it is not coerced to inside because
`128/255 > 0.5`.** A verifier that sees only this byte reports a boundary tie
and consults mathematical classification for pick. Other formats must declare
their equivalent logical-half encoding or retain pre-quantized coverage for the
receipt.

Therefore W0-B Q6's ten cases—CPU center outside, exact 8/16 sample coverage,
stored byte 128—are not geometry mismatches under W1. The failed comparison
rule `CPU inside iff decoded coverage >= 0.5` is retired. The counterexample
remains in the receipt set so a future implementation cannot regress to that
rule. The permanent verifier's current byte-128 handling already records this
tie rather than thresholding it (`src/app/client/substrate/webgpu/verifier.cljs:236-265`).

### 4.5 Zero-coverage interior — Q5 adjudicated

W0-B Q5's pixels `(61,43)` and `(66,84)` were classified CPU-inside while the
raster road emitted alpha zero. **They remain decisive false negatives.** A
raster top-left/edge ownership rule is an implementation fact, not permission
to contradict semantic geometry. The road passes only by:

1. proving through the canonical tri-state classifier that the sample is
   exactly `:boundary`, then applying the declared boundary/tie rule; or
2. repairing coverage/raster geometry so an `:inside` sample is not emitted as
   zero and replaying the pinned pixels.

Relabeling an `:inside` result, adding hit slop, or lowering an alpha threshold
does not repair Q5. Hit slop is tested separately and never changes paint.

### 4.6 MSDF counterexample — permanent and still RED

The current verifier run at this W1 settlement reproduced 47 decisive
atlas-isocontour-vs-outline mismatches in each of seven screen-constant zoom
regimes: 329 total, while SDF and Slug had zero decisive mismatches. MSDF also
had two byte-128 ties per regime; those ties are correctly excluded, and the 47
remaining cases are not ties. The mismatch therefore violates the shared
authority/isocontour law and remains a road repair obligation.

The production bank may continue recording deterministic MSDF pixels, but an
image-bank update cannot bless geometry disagreement. If the MSDF mode retires,
the fixture remains as a negative implementation-provenance test: a road backed
by an atlas whose `0.5` isocontour is not derived from the pinned authoritative
outline cannot claim geometry parity.

### 4.7 Hit slop

Hit slop is a pick-only morphological dilation measured in screen pixels and
mapped through the inverse transform into the authority's local metric. It is
declared per family/input modality and returns the same semantic identity as
the base body.

- Slop never changes coverage, paint bounds, stack order, export, or durable
  geometry.
- Slop does not escape an effective clip/mask unless that clip explicitly
  declares a pick-slop policy.
- `:pick :none` cannot be made pickable by slop.
- Base interior/boundary hits are evaluated before slop; receipts report which
  path won.
- Overlapping slop regions resolve by reverse scene-tape order.

### 4.8 Precision/backend regimes — Q2 binding

Every geometry/coverage derivation declares a complete, non-gapped partition of
the legal zoom domain `[0.01,1000]` together with shape extent, normalization,
coordinate precision, coverage format/precision, lifecycle, and backend. The
floor-default `[0.1,8]` is reported separately but is not the legal domain.

W0-B Q2 proved that unqualified fp16 is unlawful: centered normalization
reduces error but does not cover the legal domain; visible error begins for
256-unit extents at legal zoom and for 4,096+ extents inside the default clamp.
Consequently a family must select a declared higher-precision, subdivision,
renormalization, or other measured backend for every uncovered region. A
regime tag records where a road applies; it never makes legal material
unwearable. Derived-cache identity includes this regime tuple.

### 4.9 Family-policy completeness

The registration must apply the same schema without exemptions:

| Family class | Authority and required relationship |
|---|---|
| rich rect / chrome | mathematical rounded/rectilinear path, border/fill subparts, SDF or mesh coverage, interior/slop pick policy |
| text | font-file/revision glyph outlines positioned by one layout result; MSDF/Slug/other coverage is derived; object/cluster/caret pick route declared |
| path / freehand ink | path grammar or D2=A centerline-pressure truth; versioned outline/mesh coverage; centerline/outline pick readers prove parity |
| image / moving sampled media | quad/path/crop/mask authority; texture color/alpha and time sample declared; geometric or alpha-aware pick explicit |
| shadow / glass / noise / texture effect | source geometry plus named operator/support; normally `:pick :none`; effect never becomes source material |
| group / mask / overlay | explicit geometry plus shared visibility/order policy; screen/world/hybrid metric space declared |
| 3D mesh / region | mesh/material revision and local transform; region compositor geometry outside, depth/ray identity router inside |

