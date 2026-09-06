# Attack 3 — what the brush reads

Definer's contribution to round 2, 2026-09-06. Exploration before a contract. The composer and this chair remain peers; Sid carries the contribution. This was developed against the fold at **2737f41**. A separate [attack 2](attack-2.md) landed concurrently at **1000642**, so this contribution takes the next unused number. Both retain their own receipts. The landed attack 2 is preserved byte for byte. [Attack 1](attack-1.md) is folded history. [HANDOFF-3](HANDOFF-3.md) names the work continued here.

**The proposal survives, with a sharper distinction between a location and a read.** I agree with the composer's “one exchange, two schedulings”: a fully materialized correspondence is a queryable correspondence whose requested work has already finished. A pure chart change need not alter any authored paint. But when a brush reads paint through that correspondence, an unresolved portion becomes a dependency of the next brush state. A pending map cannot silently supply transparent paint. The brush may continue after that particular read has an answer, or after an explicit tool operation chooses a provisional input and retains it as such.

This contribution supplies three things at different levels of evidence. A new executable value construction carries a pickup painting through recharting and a two-record merge on the same cylindrical support, with fresh surface hits and an independent replay control. A second construction evaluates a surface-distance stroke on a sphere, including its crossing and a work-limited answer. Reads of the composer's existing bench expose two discrepancies: nonlinear pressure in the post's membership test, and field depth between its GPU and walker. The programs and reproduction commands are included below. Neither the new construction nor the diagnostics modifies the composer's files.

**A 3D thing can keep the model already proposed.** It is an occurrence of a definition in a space, with its retained parameters, construction, resources and named relations. Its evaluated geometry supplies the operations it knows how to answer. A face is one possible addressable domain; a field need not acquire faces to become a thing. A painting is another retained value, bound to a domain of that geometry. Placement, definition, construction node, authored path, painting and binding have distinct identities because operations can change them independently.

One qualification matters now: a two-dimensional authored domain does not make its host a Euclidean plane. Coordinates can be two-dimensional while physical distances come from a curved surface. The authored path can itself use surface locations and a declared interpolation, instead of being a flat path later bent into space. Conversely, a label can deliberately be drawn in flat artwork coordinates and stretched onto its host. These are different reusable constructions over the same location and painting exchanges. Calling both “a planar surface” would hide the choice.

The common 2D/3D model is composition of definitions, placements, views, queries and retained results, with geometry operations selected by their input domains. A page plane supplies the easy metric and inverse. A cylinder supplies a local development. A sphere supplies different distance geometry. That difference belongs in the operation's arguments and capabilities; it need not become a new native brush tool.

**The same post, one continuing construction.** Use the fold's radius R = 200 mm, circumference C = 400π mm, and authored patch [C−120,C+120] × [280,520] mm. Record A retains the bow-tie path's knot identities:

| Knot | Authored location in mm | Pressure |
|---|---|---:|
| A | (C−80,320) | 0.2 |
| B | (C+80,480) | 0.8 |
| Ck | (C−80,480) | 0.9 |
| D | (C+80,320) | 0.5 |

The width expression is w(p) = 10 + 30p² mm, applied to interpolated source pressure. For the pickup construction, divide each straight segment into intervals no longer than 4 mm; retain the source segment and parameter on each emitted dab. This gives 155 dabs, with shared knots emitted once. This spacing and nearest sampling are explicit choices of the reference construction, not a proposal for the best brush feel.

The program is saved data: read the previous painting at the dab centre; mix that color into carried pigment with amount 0.25; construct the dab's disc; source-over the carried color with deposition opacity 0.5; return the new painting and carry. The painting starts opaque blue and the carry starts red. It is a logical 96 × 96 Float32 RGBA value over the authored patch, independent of renderer charts; the reference interprets these premultiplied color components in linear light. The small interpreter in Appendix A executes named operations and references; the order and wiring live in the record. This is the same sample/mix/paint/state construction developed in the [path definer's attack 2](../path-kind/attack-2.md), with an explicit disc operation and a different footprint scale. It does not propose a second production language or reopen the path executor ruling.

Record B owns a separate 96 × 96 painting over [C−120,C+120] × [410,520] mm, with its own two-knot path from (C−60,440) to (C+60,440), pressure 0.65 and green carry. Its saved pickup amount is zero and its initial painting is transparent. A and B share operation implementations; their records, painting histories and bindings remain distinct.

For this construction, trim the post's addressable wall into two bands, v ≤ 390 and v ≥ 410, within A's patch. A retains its whole earlier source and is shown by restriction. B is authored on the upper band. Later join the bands by restoring the missing strip. These are operations on surface domains, not a claim to have implemented a watertight solid Boolean on the post. The old bands map by restriction into the restored wall; the strip has no predecessor in the immediately preceding trimmed evaluation.

A's saved preservation rule additionally permits restoring its older whole-wall attachment through the unchanged cylindrical generator. That older relation is why A can appear in the strip. A merge's immediate history alone does not prove this. A destructively trimmed A would stay absent there. B retains its upper-band restriction and gains no invented history in the strip.

The execution sequence is precise. Start replaying A's retained painting program. Before dab 20, change the wall chart from S0 to S1(a,b) = S0(2a+100,b). Before dab 80, restore the wall and retain the two bindings. Finish the replay, compose the bindings, and issue fresh ray hits in the strip and overlap. The brush is evaluating retained authored events during this sequence; it is not acquiring new pointer samples across a physically missing band. Its read source is its own preceding logical painting. A brush that reads the visible composition is the next case below.

Appendix A uses the committed bench's actual postWorld, postChart, postPreimages, hitPost and evaluateP functions. It adds the painting operations and explicit band/binding data here. Fresh hits are inverted through each current chart and checked against the authored dab locations. The new merge is not a call to the bench's box-only mergeCorrespondence.

| Readout | Result |
|---|---|
| A source/program SHA-256 before and after | 86f34ffa3b37d71c64cc758344974fafc18c66d64f60d04935a72a95f52c3337 |
| Painting prefix before rechart, both replays | 444d444956c226030d2aface3e7caadf8f1200b54a1c9c7a0d8c08a0c1c5f36e |
| Final painting, uninterrupted and interleaved | 1b7dce5c1ef1d5f70d13f30a4a9c863b9c08150ce89f8cdb5df8c553191533f1 |
| Negative control: reset carry at rechart | 3eed724f663b0187598d10471e1c161d82bfe348ce0fae6f14449e243ab20108 |
| Maximum fresh-hit location difference on these dabs | 0 mm in this run |
| Strip centre (C,400) | Before: absent support. After: A, same authored location, RGBA (0.003862565,0,0.996137440,1) |
| Overlap (C+40,440), B over A | Both record IDs; RGBA (0.000136339,0.984375,0.015488661,1) |
| Same overlap without an order | needs-policy, with both record IDs |

The zero is a measured floating-point result for this analytic fixture, not a promised error bound for inverse surfaces. Matching hashes show that the specified rechart and binding changes did not alter the brush computation; the carry-reset control shows why restarting it at a chart fragment would alter the result. The merge checks show which records the fresh support hit reaches. They do not prove browser presentation, arbitrary correspondence discovery, or a GPU pickup implementation. This is the requested continuing sequence at the exchanged-value level. The full page sequence remains an integration receipt the composer can build from it.

**The doubly curved construction is a sphere.** Let locations be unit vectors on a sphere of radius 200 mm:

~~~text
X(λ,φ) = R (cosφ cosλ, sinφ, cosφ sinλ)
u = Rλ, v = Rφ
metric in (u,v) = diag(cos²φ, 1)
d(p,q) = R atan2(|p × q|, p · q)
~~~

At latitude 60°, a change of 16 mm in the longitude coordinate has surface distance 7.99839984003628 mm. A radius-10-mm intrinsic disc contains that point; a radius-10 disc in these raw chart coordinates excludes it. The metric at the starting point is diag(0.25,1), but evaluating only that local matrix is not the finite-distance algorithm.

Save four path knots at longitudes −0.18,+0.18,−0.18,+0.18 radians and latitudes φ₀−0.1,φ₀+0.1,φ₀+0.1,φ₀−0.1, with φ₀ = π/3 and the same pressures as A. Join consecutive locations by the shorter great-circle arc, with source pressure linear in arc parameter. The branch is explicit: coincident endpoints give a constant centre; antipodal endpoints require another branch choice. Changing to UV-straight interpolation would change this path even with the same knot locations.

For a round surface nib, the reusable region means:

~~~text
m(q) = min over segments and t∈[0,1] of
       [d_surface(q, centre(segment,t)) − w(pressure(segment,t))/2]

inside if m(q) < 0; outside if m(q) > 0;
boundary according to the query's declared tolerance.
~~~

The reference uses the sphere distance directly. On a constant-speed segment of length ℓ, its objective is Lipschitz in t with bound:

~~~text
L = ℓ + 30 max(|p0|,|p1|) |p1−p0|.
~~~

The first term bounds centre motion; the second bounds the derivative of radius 5 + 15p². A midpoint sample on [a,b] gives lower bound f(mid) − L(b−a)/2 and upper witness f(mid). Subdivide the interval with the lowest bound. A negative upper witness proves membership; positive lower bounds for all remaining intervals prove exclusion; exhaustion of work returns pending with the remaining bound. For a distance oracle returning an interval, use its lower endpoint in the lower bound and its upper endpoint for the witness.

At the intersection of the two great-circle segments, the probe returns contributors 0 and 2. It excludes segment 1 after 37 distance evaluations, with minimum enclosed between 0.0448254734 and 0.4524847667 mm. Membership witnesses for segments 0 and 2 are respectively −4.0906052517 and −7.6906052517 mm. This retains two source branches of one mark. Those witnesses are not estimates of the exact minimum or unique edit parameters; an editing query can refine the source intervals, and at this constructed centreline crossing its two arc parameters can also be computed directly.

A second query uses the equatorial arc from longitude −0.2 to +0.2, constant pressure 0.5, and query latitude 0.045 at longitude zero. Its exact clearance is 9−8.75 = 0.25 mm. One distance evaluation returns pending, with enclosure [−39.75,0.25]. Forty-three evaluations return outside, with [0.0720365548,0.25]. Pending is an actual exhausted computation here, rather than a label attached to a finished answer.

This reference uses JavaScript floating-point analytic distance, without outward-rounded interval arithmetic. Its outputs test the construction away from a zero-error decision; they are not certified enclosures at arbitrary precision. The inequalities give a concrete route for a bounded implementation: distance and centre-motion bounds must include numerical error. To emit a boundary band or source intervals at requested accuracy, continue subdivision under that request instead of stopping as soon as membership is known.

The reusable region also needs conservative bounds. The entire sphere's box is valid, though coarse; an arc-centred spherical cap with angular radius (ℓ/2 + r_max)/R is tighter when useful. Representation charts may divide the region into fragments with source intervals; they create neither extra endpoints nor extra deposits. Near a cut locus, distance may be well-defined while a chosen shortest path or tangent direction has candidates. A circular distance nib does not need to pretend those directions are unique.

For an arbitrary smooth or trimmed CAD host, a surface evaluator and local metric still do not supply this distance oracle, global bounds, inverse locations or boundary continuation. A viable first adapter can use distances on a declared triangulated support. CGAL's shortest-path package takes locations on a triangulated surface and returns distances or paths on that surface; these are not automatically distances on the original CAD surface, and a tessellation tolerance alone is not a geodesic-error certificate. [CGAL's primary description](https://doc.cgal.org/latest/Surface_mesh_shortest_path/index.html). The choices are to make the mesh authoritative for that tool's metric, establish an additional approximation bound, or report that the requested CAD-surface error is unsupported.

Two policies must be separate data. One selects where distance is measured: the underlying surface or paths constrained to the trimmed face. The other selects where paint may be deposited and whether the operation continues onto adjacent supports. A hole can clip a disc under the first metric and divert a shortest path under the second. A tilted elliptical nib additionally needs an orientation and a transport rule; the circular sphere example does not supply them.

**A brush that picks up the composed coating exposes the remaining exchange.** Before a new dab on the merged support, pin the painting values and bindings it reads. Suppose the premultiplied contributions are A = (0.5,0,0,0.5) and B = (0,0,0.5,0.5), with B over A. The sample is (0.25,0,0.5,0.75). With red carry and pickup 0.25, the next carry is (0.8125,0,0.125,0.9375).

If B's correspondence is pending and the sampler treats it as transparent, it instead produces (0.875,0,0,0.875). That difference becomes input to later dabs. Completing B's map later cannot repair the painting merely by redrawing the host; the brush has already consumed the wrong value. Appendix A executes this arithmetic and checks that its pending branch returns no next carry.

A workable exchange is the surface-bound form of the shared compositor's sample operation. Sid's 11:51 choice on the [path ledger](../path-kind/path-kind.md) is one compositor with paint and sample, owning the screen and the surface a brush reads. That choice stands; the following specifies the read through a 3D binding, not another compositor:

~~~text
read-surface(
  snapshot: painting value IDs/revisions + binding/correspondence revisions
            + composition rule + quantity/colour interpretation,
  location or sample footprint,
  filter and metric/domain if the read needs them,
  requested error/work
)
  -> resolved {sample, achieved error, source/read dependencies}
   | pending {unresolved dependencies, covered portion, continuation}
   | needs-policy {overlap or branch candidates}
   | unsupported {missing operation or unattainable requested guarantee}

brush-step(program, previous-state, authored event, resolved read values)
  -> next-state + new logical painting value + changed domain
~~~

A continuation identifies the input snapshot it belongs to. If those inputs change before its answer arrives, discard that answer for the new computation or recompute from an appropriate checkpoint; do not relabel the old result. Pending does not emit the state transition. The executor can buffer authored events while that dependency resolves, and the page can continue presenting and orbiting. This is a barrier for the dependent painting step, not a requirement to freeze the scene or materialize all correspondence everywhere.

The snapshot is a coherent set of actual read dependencies, not necessarily a global scene revision. For the first probe, the read source is A's own previous painting in authored coordinates: a chart re-cut and domain merge change its presentation bindings, not that painting input. For a surface-reading brush, the read includes the ordered coating values and current mapping. For a brush whose spacing, width or pickup footprint uses physical distance on a deformed host, its metric choice is another dependency. An origin/normalized anchor rule for the centreline does not by itself decide whether nib width stretches, pigment is transported, or the brush is replayed in a new metric.

That is also the necessary fine-grained diff. A pressure-knot edit invalidates affected path construction and later painting steps that read its changed result; a valid earlier checkpoint can survive. A change of chart representation preserves an equivalent authored read when the correspondence establishes it. A composition-order edit invalidates overlap samples and any later brush state that consumed them. If the implementation cannot prove a narrower dependency, it must invalidate more broadly. Stable IDs enable these references; the store above the kind decides how authored changes merge.

**The hardest case still has one route through the model.** This is the proposed operation sequence, not a report that all of it ran in this turn.

| Action | Values used and result |
|---|---|
| Paint on the curved host inside region R on page P | Page coordinates enter R's view to form a ray. Geometry locates the visible support; its binding locates the authored domain. The tool retains source event IDs/attributes and executes its paint program. Physical-distance construction uses the host metric; flat artwork construction uses its authored metric. |
| Vector-edit the pressure stroke at its crossing | The hit addresses the occurrence, binding and one authored mark, with candidate segment/parameter locations. The saved selection reaction chooses the edit target. Changing a knot patches that knot; replay begins from a valid preceding painting state. A geometric union mark and an accumulating pickup painting retain different construction histories. |
| Zoom the page or orbit R | Views, projected error demands, rendering and hover change. The retained paint is presented again. A physically defined brush is not repainted because more display pixels become available. A deliberately screen-sized tool has an explicit camera dependency instead. |
| Re-cut the chart, then merge two ink-bearing supports | Compose the location relation, retain source and painting identities, transport both bindings by their saved rules. New support portions and unavailable predecessors remain explicit. Overlap uses saved material order or needs-policy. A read through unresolved correspondence can be pending. |
| Move the field occluder under a still pointer | Its changed bounds can enter the query even though it was not the previous winner. Reevaluate the relevant visibility/query dependencies. Its nearer answer names the field definition and a local point, with evaluation and error. An unresolved nearer interval keeps the winner pending instead of declaring the candidate behind it definite. |
| Reach portal T on a visible support and enter the other land | First resolve visibility in the containing scene. T's presentation map and pointer map connect to the target view. The route names both occurrences and lands. Crossing or re-rooting is a saved reaction; recursion/work limits have declared results. A material sample of T must say whether it reads a pinned target image or a live target dependency. |
| Inspect screenshot I beside R | I retains captured pixels, interpretation and optional source/view provenance. Its ordinary query addresses I and an image location. Later orbit, brush replay or occlusion changes do not change those captured bytes. |
| Produce and reuse the CAD section drawing D | Evaluate the parametric solid and section at a declared plane and model tolerance. Return identified analytic curves where available, approximation/error otherwise, and source relations; lower for the 2D path consumer at its display demand. A circle remains a reusable circle definition. Its 2D query returns its parameter and source relation. A saved inverse-edit reaction can bind a radius handle to the chosen CAD parameter; the relation alone does not choose that cause. |

The last row carries forward the fold's analytic-circle case. It is not the flattening of a whole spherical painting. A flat export of that painting must name its projection, domains, distortion and any loss, or retain a sampling construction referring back to the curved source. “Reusable 2D result” does not promise a distance-preserving flattening of every host. A section, projection, texture image and editable analytic drawing are different results the same construction machinery can return.

**What new code the waist still owes.** These are capability boundaries, with the work behind their names exposed.

| Capability and exact exchange | What can already contribute | What is still missing |
|---|---|---|
| Locate on a support: evaluated support + ray/point + valid domains/transitions + error demand → locations/candidates, normals/derivatives where available, achieved error or unresolved portions | Camera/transform calculations; the bench's analytic cylinder; plane inverse as a special case | The current [plane inverse](../../../src/app/client/region3d/on_plane.cljc#L72) intersects local z=0. It cannot discover curved/trimmed inverse locations or all periodic preimages. A general adapter must supply them. |
| Surface distance: authoritative metric/domain + two locations + boundary policy + error/work → distance interval, optional path/source pieces, status | The sphere construction; a triangulated-distance library for its declared surface | A local metric matrix is insufficient. Smooth trimmed hosts need a solver, bounds and transfer/error evidence, or an explicitly different metric authority. |
| Swept region: centre evaluator/source intervals + attributes/width expression + metric-distance capability + error/work → membership enclosure, conservative bounds and contributing source intervals, with pending portions | The path's construction/query role; numerical subdivision; the sphere reference | Curved finite footprints and conservative error propagation are not provided by planar path packing. Nonlinear pressure must reach the final footprint evaluation. |
| Read a logical surface: pinned paintings/bindings + quantity, footprint/filter, domain/metric and demand → value/error/dependencies or pending/policy/unsupported | Composition arithmetic and logical-image ownership; the path lane's sample/mix/paint construction | A reader through partial 3D correspondence, including footprint crossings and coherent layer resolution. Reading the current screen lease does not supply a stable authored painting input. |
| Execute the stateful step: saved operation graph + inputs + prior state → returned values/state and changed domains | The executor chosen on the path ledger; generic expression/resource operations | Bind the 3D capabilities into it. GPU reads/writes, aliasing and continuation scheduling still need implementation; this CPU reference does not establish a fast host. Sid's reserved 3D language decision remains reserved. |
| Field ray/depth participation: definition revision + evaluator capabilities + ray interval + requested geometric error/work/numeric tier → boundary intervals/locations, empty coverage, unresolved intervals and normal status; renderer consumes the same semantics | The bench's CPU and GLSL quartic construction; common visibility/camera calculations | Current [mesh admission](../../../src/app/client/region3d/component.cljc#L386) is closed, and [pick-region](../../../src/app/client/region3d/scene.cljc#L965) queries a mesh BVH. A field extension needs the spatial participation door. The numeric read below prevents treating the algorithm name as a guarantee. |
| CAD section and continuation: authored construction + previous evaluation + operation/tolerance → shape and section values, provenance, inter-evaluation correspondence or its unresolved portions | A modelling kernel can supply algorithms and operation history; the fold's analytic examples | Tolerant arbitrary modelling, source-domain transfer and the attachment rule's preservation are different operations. Kernel history does not supply the last two automatically. |

The stable calling surface can be small while these geometry and execution implementations are substantial. Another tool becomes data when it can call those operations, bind their returned values, choose policies and retain its own result. Merely accepting a record containing the name of an unimplemented operation would not establish that.

**Two existing-bench reads qualify the picture.** Both use the bytes from 2737f41, SHA-256 **010c91d4e0cd4b3dad30208a5bf4b729124483374f8d8a32b8903ae00824c807**.

First, the post's classifier decides coverage from a capsule whose radii interpolate endpoint widths. It separately computes a pressure-derived width for the returned location. Those are different footprints for 10 + 30p². At:

~~~text
q = (C−40−7.4/√2, 360+7.4/√2)
  = (1211.4044712551367, 365.23259018078045) mm
~~~

the actual classifyMark returns inside, segment 0, signed value −0.45585583256918305 mm, and reported width 13.686504320831514 mm. Independently minimizing distance minus declared radius over all three source segments puts the minimum in [0.5590205669041215,0.5596267523290708] mm: outside. The enclosure uses 200,000 intervals per segment and the centre/radius Lipschitz bound, rather than assuming a sampled minimum is exact. Appendix A reproduces both sides.

This is a wrong membership answer, not only an inaccurate readout. The post shader also receives endpoint widths and invokes the linear-radius capsule; that is a source observation, not a separately measured GPU coverage receipt. A correction must carry the pressure/width function to footprint evaluation, or approximate it with an explicit envelope/error demand. Changing only the printed width would leave the mismatch. The [path attack 2](../path-kind/attack-2.md) found the related “evaluate after interpolating pressure” seam in its dab emitter; the same meaning must survive both consumers.

Second, the field's ray polynomial is evaluated in two numeric environments. I ran the actual bench in headless Chrome 150.0.7871.46 using ANGLE/Vulkan SwiftShader. In an in-memory copy only, the fragment shader encoded the value it assigns to gl_FragDepth into RGB24, replacing its material color. I read the same pixel centre with the CPU walker and projected that hit through the bench camera. This reads the shader's depth value through color after its depth test; it is not a direct read of the depth attachment. Dither was disabled. A repeat with antialiasing disabled (SAMPLES = 0) produced the same numbers. No shader or GL errors were reported.

| Fixture | CPU projected depth | GPU encoded depth | Difference in depth | Depth-equivalent point difference |
|---|---:|---:|---:|---:|
| Solid, field=1&fx=0.5&aim=F | 0.957906733921 | 0.957864699236 | 0.0000420346843 | 2.278619 mm |
| Hollow, same outside view | 0.957906733921 | 0.957864699236 | 0.0000420346843 | 2.278619 mm |
| Solid with fa=0.1 | 0.957973501872 | 0.957978305696 | 0.00000480382415 | 0.261499 mm |
| Hollow, camera in the void | 0.988481069451 | 0.988481342106 | 0.000000272654910 | 0.002051 mm |

The first three use region pixel (226,127) in a 420 × 330 viewport. The void case uses (210,165), camera pivot at the field centre, distance 0.02 m and near plane 0.001 m. It reaches the inner wall. These are corresponding pixel-centre rays: the CPU ray starts at the near plane and the shader ray at the eye, so raw t values would not be a valid parity comparison.

As a bounded diagnostic, I translated only the shader polynomial's ray origin to bounding-box entry and restored that shift to returned t. The depth-equivalent differences became 0.100983, 0.100983, 0.062954 and 0.002051 mm respectively. This supports numerical conditioning as a contributor. It does not establish the remaining cause or a completed fix. Appendix B reproduces both shader versions without changing the source file.

The solid/hollow outer discrepancy is much larger than one RGB24 depth code at this location. I do not close GPU/walker parity from these readings. They show why a runner needs achieved geometric error or an honest limitation: “both isolate the quartic” is insufficient. This reference does not test contact-only rays, a full image of boundaries, hardware GPUs, or work-limited shader execution. A later parity claim must state its demand, numeric tier, tested domain and unresolved behavior.

**The positions and their exits are explicit.**

| Position | What supports it | When it becomes a default, and what changes |
|---|---|---|
| Recharting alone preserves the authored painting and equivalent logical reads | Same geometric points, an explicit correspondence, the post replay and negative control | A deliberate screen/UV-space brush can depend on those coordinates. Its record names that dependency; changing them may then require replay. |
| Intrinsic round ink is a surface-distance region | The sphere's chart-distance counterexample and executed membership construction | Flat artwork, projected nibs and transported anisotropic nibs remain valid tools. They change metric/orientation inputs and footprint construction. The sphere does not choose brush feel for Sid. |
| A merge retains both paint records and composes only where valid domains overlap | The binding restrictions and fresh overlap/strip queries | An explicit flatten/bake can replace histories. It returns a new retained result and declared loss/correspondence; this is not an accidental effect of merge. |
| Pending dependencies cannot provide an implicit paint sample | Next carry differs numerically if a missing layer is treated as transparent | A preview can show provisional pixels. If a tool consumes them as paint, that source becomes a retained input with its interpretation. It must not vary with an incidental completion race. |
| Numeric quality belongs in geometry answers | The same field semantics gave different projected depths on the two hosts | An application may request approximate hits/images. It still needs achieved or explicitly unbounded quality to choose refinement and interaction behavior. An algorithm label is not that report. |

**What remains workable, and the exact open input/output decision.** Keep the common definition/occurrence/view/portal model and the three consumer requests. Keep correspondence as a revision-bound relation, with explicit maps as an eager implementation. Keep paintings as logical values with authored domains and state histories. Add surface-distance operations for tools that request them; keep metric, boundary, error and continuation inputs explicit. Bind them into the shared executor.

The remaining definer question is precise: **does a stateful painting step consume only a resolved sample from a named input snapshot, with pending producing no next state; or may it consume a provisional sample, which must then be returned and retained as an explicit input to its history?** I recommend the first as ordinary behavior, with the second available only as a declared construction. Eager versus demand-driven correspondence remains a scheduling choice once that distinction is honored. This adds an input/output consequence; it does not re-ask Sid's executor ruling or answer the reserved 3D half for him.

For the composer, the foldable contribution is the continuing value sequence, the sphere construction with its error/work exchange, and the stateful-read distinction. The pressure and field reads are bounded corrections to the new bench. The general smooth curved host, browser pickup on the post, and an agreed GPU/walker accuracy receipt remain implementation work with named inputs and outputs. The full hardest-case browser sequence is still owed; it would be false to transfer this reference program's receipts to it.

**Reproduction.** Run from the repository root. The embedded programs read the bench from 2737f41 with git show, so a later fold does not silently change the subject. They write no files. Appendix A needs Node. Appendix B also needs the installed puppeteer and /usr/bin/google-chrome; it creates and closes its own headless browser.

~~~sh
python3 - <<'PY'
from pathlib import Path
import subprocess
text = Path("docs/below-the-waist/3d/attack-3.md").read_text()
for name in ("A", "B"):
    code = text.split(f"<!-- probe-{name}:begin -->\n~~~javascript\n", 1)[1].split("\n~~~\n", 1)[0]
    subprocess.run(["node"], input=code, text=True, check=True)
PY
~~~

**Appendix A: the construction and pressure receipts.**

<!-- probe-A:begin -->
~~~javascript
const vm=require('vm'),crypto=require('crypto'),assert=require('assert');
const source=require('child_process').execFileSync('git',['show','2737f41:docs/below-the-waist/3d/bench-2/seam-bench.html'],{encoding:'utf8'});
function extract(name){const a=source.indexOf('  function '+name+'('),b=source.indexOf('\n  function ',a+1);assert(a>=0&&b>a);return source.slice(a,b);}
const B=vm.createContext({TAU:2*Math.PI});
vm.runInContext(['capsuleSD','markSegments','markAt','classifyMark','postOf','postPeriod','postWorld','postChart','postPreimages','hitPost','markWidth','markKnots','markDomain','evaluateP','over'].map(extract).join('\n'),B);
const hash=x=>crypto.createHash('sha256').update(ArrayBuffer.isView(x)?Buffer.from(x.buffer,x.byteOffset,x.byteLength):JSON.stringify(x)).digest('hex');
const mix=(a,b,t)=>a.map((v,i)=>v*(1-t)+b[i]*t),R=200,C=2*Math.PI*R;
const con={revision:'P0',params:{r:.2,h:.8,at:[0,0],chart:{scale:1,offset:0}}};
const p0=B.evaluateP(con,null),p1=B.evaluateP({...con,revision:'P1',params:{...con.params,chart:{scale:2,offset:100}}},p0);
const p2=B.evaluateP({...con,revision:'P2',params:{...con.params,chart:{scale:2,offset:100}}},p1);
const knots=B.markKnots(p0.support,'unwrapped'),N=96;
const recordA={id:'paint-A',path:'path-A',domain:[C-120,280,240,240],knots};
const recordB={id:'paint-B',path:'path-B',domain:[C-120,410,240,110],knots:[{id:'b0',u:C-60,v:440,p:.65},{id:'b1',u:C+60,v:440,p:.65}]};
// The sequence and wiring are tool data. Operations below form this probe's small interpreter.
const ref=id=>({ref:id});
const program=[
 {id:'picked',op:'sample',args:[ref('surface'),ref('dab.center'),ref('domain')]},
 {id:'carryOut',op:'mix',args:[ref('carry'),ref('picked'),.25]},
 {id:'footprint',op:'disk',args:[ref('dab.center'),ref('dab.radius')]},
 {id:'surfaceOut',op:'over-region',args:[ref('surface'),ref('footprint'),ref('carryOut'),.5,ref('domain')]}
];
function sample(a,q,d){const x=Math.max(0,Math.min(N-1,Math.floor((q[0]-d[0])/d[2]*N))),y=Math.max(0,Math.min(N-1,Math.floor((q[1]-d[1])/d[3]*N)));return Array.from(a.slice(4*(y*N+x),4*(y*N+x)+4));}
const operations={
 sample,mix,disk:(center,radius)=>({center,radius}),
 'over-region':(a,f,color,alpha,d)=>{const out=a.slice(),top=color.map(x=>x*alpha);
  for(let y=0;y<N;y++)for(let x=0;x<N;x++){
   const q=[d[0]+(x+.5)*d[2]/N,d[1]+(y+.5)*d[3]/N];
   if(Math.hypot(q[0]-f.center[0],q[1]-f.center[1])<=f.radius){
    const i=4*(y*N+x);out.set(B.over(top,Array.from(a.slice(i,i+4))),i);
   }
  }return out;}
};
function step(surface,carry,dab,domain,prog=program){
 const e={surface,carry,domain,'dab.center':dab.q,'dab.radius':B.markWidth(dab.p)/2};
 for(const n of prog)e[n.id]=operations[n.op](...n.args.map(a=>a&&a.ref?e[a.ref]:a));
 return {surface:e.surfaceOut,carry:e.carryOut};
}
function dabs(ks){const out=[];for(let i=0;i+1<ks.length;i++){const a=ks[i],b=ks[i+1],n=Math.ceil(Math.hypot(b.u-a.u,b.v-a.v)/4);
 for(let j=i?1:0;j<=n;j++){const t=j/n;out.push({q:[a.u+(b.u-a.u)*t,a.v+(b.v-a.v)*t],p:a.p+(b.p-a.p)*t,source:[a.id,b.id,t]});}}return out;}
function freshLocation(ev,q,domain){
 const P=ev.support,w=B.postWorld(P,...q),normal=[Math.cos(q[0]/R),0,Math.sin(q[0]/R)];
 const hit=B.hitPost({origin:w.map((v,i)=>v+.03*normal[i]),dir:normal.map(v=>-v)},P);
 const ch=B.postChart(P,hit.point),pre=B.postPreimages(P,{u0:domain[0],u1:domain[0]+domain[2],v0:domain[1],v1:domain[1]+domain[3]},ch.a,ch.b);
 assert.equal(pre.length,1);return [pre[0].u,pre[0].v];
}
// The restored band has no P1-cut predecessor. A retains an older, whole-wall generator binding.
const bindings=[
 {id:'binding-A',record:recordA,from:'P0.whole-wall',to:'P2.wall',rule:'retain-source; restore ancestor',before:[[280,390],[410,520]],after:[[280,520]]},
 {id:'binding-B',record:recordB,from:'P1-cut.upper',to:'P2.wall',rule:'retain restriction',before:[[410,520]],after:[[410,520]]}
];
function resolveBindings(ev,q,merged){
 if(!merged&&q[1]>390&&q[1]<410)return {status:'absent-support',revision:ev.rev,ids:[]};
 const location=freshLocation(ev,q,recordA.domain);
 const selected=bindings.filter(b=>b[merged?'after':'before'].some(iv=>q[1]>=iv[0]&&q[1]<=iv[1]));
 return {status:'resolved',revision:ev.rev,location,ids:selected.map(b=>b.record.id)};
}
const inputHash=hash({recordA,program}),ds=dabs(knots),seed=new Float32Array(N*N*4);
for(let i=0;i<N*N;i++)seed.set([0,0,1,1],4*i);
function replay(mode){
 let surface=seed.slice(),carry=[1,0,0,1],ev=p0,merged=false,prefix=null,locationError=0;const trace=[];
 for(let i=0;i<ds.length;i++){
  if(i===20){prefix=hash(surface);if(mode!=='baseline')ev=p1;if(mode==='reset')carry=[1,0,0,1];}
  if(i===80&&mode!=='baseline'){merged=true;ev=p2;}
  if(i===20||i===80)trace.push({beforeDab:i,strip:resolveBindings(ev,[C,400],merged),overlap:resolveBindings(ev,[C+40,440],merged)});
  const q=freshLocation(ev,ds[i].q,recordA.domain);
  locationError=Math.max(locationError,Math.hypot(q[0]-ds[i].q[0],q[1]-ds[i].q[1]));
  // Retained authored events drive replay; inversion is a fresh hit check, not a rewrite of them.
  ({surface,carry}=step(surface,carry,ds[i],recordA.domain));
 }return {surface,carry,prefix,merged,locationError,trace};
}
const baseline=replay('baseline'),sequence=replay('sequence'),wrong=replay('reset');
assert.equal(hash(baseline.surface),hash(sequence.surface));assert.equal(baseline.prefix,sequence.prefix);
assert.equal(sequence.trace[0].strip.status,'absent-support');assert.equal(sequence.trace[1].strip.revision,'P2');assert.equal(sequence.trace[1].strip.ids.length,1);
assert.notEqual(hash(baseline.surface),hash(wrong.surface));assert.equal(inputHash,hash({recordA,program}));
let sb=new Float32Array(N*N*4),cb=[0,1,0,1],programB=JSON.parse(JSON.stringify(program));programB[1].args[2]=0;
for(const dab of dabs(recordB.knots))({surface:sb,carry:cb}=step(sb,cb,dab,recordB.domain,programB));
function at(q,merged,order){
 const resolved=resolveBindings(merged?p2:p1,q,merged);if(resolved.status!=='resolved')return resolved;
 const loc=resolved.location;
 const layers=bindings.filter(b=>b[merged?'after':'before'].some(iv=>q[1]>=iv[0]&&q[1]<=iv[1]))
 .map(b=>({id:b.record.id,binding:b.id,color:sample(b.record.id==='paint-A'?sequence.surface:sb,loc,b.record.domain)}));
 if(layers.length>1&&!order)return {status:'needs-policy',ids:layers.map(x=>x.id)};
 let color=[0,0,0,0];for(const id of order||['paint-A']){const l=layers.find(x=>x.id===id);if(l)color=B.over(l.color,color);}
 return {status:'resolved',ids:layers.map(x=>x.id),location:loc,color};
}
const stripBefore=at([C,400],false),stripAfter=at([C,400],sequence.merged,['paint-A','paint-B']);
const overlap=at([C+40,440],sequence.merged,['paint-A','paint-B']),unordered=at([C+40,440],true);
assert.equal(stripBefore.status,'absent-support');assert.equal(stripAfter.ids.length,1);assert.equal(overlap.ids.length,2);assert.equal(unordered.status,'needs-policy');
console.log(JSON.stringify({post:{dabs:ds.length,source:inputHash,trace:sequence.trace,prefix:sequence.prefix,baseline:hash(baseline.surface),sequence:hash(sequence.surface),reset:hash(wrong.surface),locationErrorMM:sequence.locationError,stripBefore,stripAfter,overlap,unordered}}));
// Independent analytic surface; unit-vector locations, metric measured in mm.
const dot=(a,b)=>a.reduce((s,v,i)=>s+v*b[i],0),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
const norm=a=>a.map(v=>v/Math.hypot(...a)),angle=(a,b)=>Math.atan2(Math.hypot(...cross(a,b)),dot(a,b));
const sphere=(l,f)=>[Math.cos(f)*Math.cos(l),Math.sin(f),Math.cos(f)*Math.sin(l)];
function arc(a,b,t){const z=angle(a,b);assert(z<Math.PI-1e-10);return z<1e-12?a:norm(a.map((v,i)=>(Math.sin((1-t)*z)*v+Math.sin(t*z)*b[i])/Math.sin(z)));}
function sweep(q,a,b,p0,p1,budget){
 const L=R*angle(a,b)+30*Math.max(Math.abs(p0),Math.abs(p1))*Math.abs(p1-p0);let work=0,upper=Infinity;
 function cell(lo,hi){const t=(lo+hi)/2,f=R*angle(q,arc(a,b,t))-B.markWidth(p0+(p1-p0)*t)/2;work++;upper=Math.min(upper,f);return {lo,hi,lower:f-L*(hi-lo)/2};}
 let cells=[cell(0,1)];
 while(true){cells.sort((a,b)=>a.lower-b.lower);const lower=cells[0].lower;
  if(upper<0||lower>0||work+2>budget)return {status:upper<0?'inside':lower>0?'outside':'pending',lower,upper,work};
  const z=cells.shift(),m=(z.lo+z.hi)/2;cells.push(cell(z.lo,m),cell(m,z.hi));
 }
}
const phi=Math.PI/3,sk=[sphere(-.18,phi-.1),sphere(.18,phi+.1),sphere(-.18,phi+.1),sphere(.18,phi-.1)];
let crossing=norm(cross(cross(sk[0],sk[1]),cross(sk[2],sk[3])));if(dot(crossing,sphere(0,phi))<0)crossing=crossing.map(v=>-v);
const pressures=[.2,.8,.9,.5],candidates=[0,1,2].map(i=>({seg:i,...sweep(crossing,sk[i],sk[i+1],pressures[i],pressures[i+1],10001)}));
const sp0=sphere(0,phi),sp1=sphere(.08,phi),intrinsic=R*angle(sp0,sp1);
const eq=[sphere(-.2,0),sphere(.2,0)],query=sphere(0,.045),limited=sweep(query,...eq,.5,.5,1),resolved=sweep(query,...eq,.5,.5,10001);
assert(intrinsic<10&&R*.08>10);assert.deepEqual(candidates.filter(x=>x.status==='inside').map(x=>x.seg),[0,2]);assert.equal(limited.status,'pending');assert.equal(resolved.status,'outside');
console.log(JSON.stringify({sphere:{intrinsicMM:intrinsic,chartMM:16,chartMetric:[Math.cos(phi)**2,1],candidates,limited,resolved}}));
// A bench witness against interpolating endpoint radii for nonlinear pressure response.
const q=[C-40-7.4/Math.sqrt(2),360+7.4/Math.sqrt(2)],legacy=B.classifyMark(knots,q);
let sampled=Infinity,best=null;const intervals=200000;
for(let seg=0;seg<3;seg++)for(let j=0;j<=intervals;j++){
 const t=j/intervals,a=B.markAt(knots,seg,t),v=Math.hypot(q[0]-a.u,q[1]-a.v)-a.w/2;
 if(v<sampled){sampled=v;best={seg,t,width:a.w};}
}
const enclosureError=(Math.hypot(160,160)+30*.9*.6)/(2*intervals);
assert(legacy.inside&&sampled-enclosureError>0);
console.log(JSON.stringify({pressure:{q,bench:legacy,minimum:[sampled-enclosureError,sampled],best}}));
// Pending composition cannot be substituted into the next stateful dab.
const ca=[.5,0,0,.5],cb2=[0,0,.5,.5],oldCarry=[1,0,0,1];
function consume(read){return read.status==='resolved'?{status:'resolved',nextCarry:mix(oldCarry,read.color,.25)}:{status:'pending',dependency:read.dependency};}
const held=consume({status:'pending',dependency:['support-P','P2','paint-B','binding-B']});
const complete=consume({status:'resolved',color:B.over(cb2,ca)}),missing=consume({status:'resolved',color:ca});
assert(!('nextCarry' in held));assert.notDeepEqual(complete,missing);
console.log(JSON.stringify({statefulRead:{held,complete,missing}}));
~~~
<!-- probe-A:end -->

**Appendix B: the field depth diagnostic.**

<!-- probe-B:begin -->
~~~javascript
const assert=require('assert'),puppeteer=require('puppeteer');
const source=require('child_process').execFileSync('git',['show','2737f41:docs/below-the-waist/3d/bench-2/seam-bench.html'],{encoding:'utf8'});
function replaceOne(s,a,b){assert.equal(s.split(a).length,2,'instrumentation anchor: '+a);return s.replace(a,b);}
(async()=>{
 const browser=await puppeteer.launch({executablePath:'/usr/bin/google-chrome',headless:true,args:['--no-sandbox','--use-angle=swiftshader','--use-gl=angle','--enable-unsafe-swiftshader']});
 try{
  const p=await browser.newPage();await p.setViewport({width:1500,height:1100,deviceScaleFactor:1});
  const errors=[];p.on('pageerror',e=>errors.push(e.message));
  console.log(JSON.stringify({browser:await browser.version()}));
  for(const shifted of [false,true])for(const scenario of ['solid','hollow','small','void']){
   let html=replaceOne(source,'antialias: true','antialias: false');
   html=replaceOne(html,'fragColor = vec4(base * lit, 1.0);','float k = floor(gl_FragDepth * 16777215.0 + 0.5); fragColor = vec4(floor(k / 65536.0), mod(floor(k / 256.0), 256.0), mod(k, 256.0), 255.0) / 255.0;');
   html=replaceOne(html,'\n})();\n</script>','\nwindow.__attack={renderView,lease,cameraOf,rayFromRegionPoint,hitField,projectPoint,m4point,fieldValue,state,S,views,page,gl,shaderLog};\n})();\n</script>');
   if(shifted){
    html=replaceOne(html,'float t0, t1; if (!rayBox(o, d, uFieldOuter, t0, t1)) return -1.0;','float t0, t1; if (!rayBox(o, d, uFieldOuter, t0, t1)) return -1.0; float shift = t0; o += shift * d; t1 -= shift; t0 = 0.0;');
    html=replaceOne(html,'n = length(g) > 1e-9 ? normalize(g) : vec3(0.0); return lo;','n = length(g) > 1e-9 ? normalize(g) : vec3(0.0); return shift + lo;');
   }
   const hash='field=1&fx=0.5&aim=F'+(['hollow','void'].includes(scenario)?'&hollow=1':'')+(scenario==='small'?'&fa=0.1':'');
   await p.goto('about:blank#'+hash);await p.setContent(html,{waitUntil:'domcontentloaded'});await p.waitForFunction(()=>window.__attack);
   const r=await p.evaluate(scenario=>{
    const a=window.__attack,ls=a.lease(),f=a.S.things.F;
    if(scenario==='void')Object.assign(a.views.V.camera,{pivot:f.at.slice(),distance:.02,near:.001});
    const cam=a.cameraOf(a.views.V,ls),rp=a.projectPoint(cam,f.at),ix=Math.floor(rp[0]),iy=Math.floor(rp[1]);
    a.gl.disable(a.gl.DITHER);a.renderView();
    const px=new Uint8Array(4);a.gl.readPixels(ix,ls.alloc[1]-1-iy,1,1,a.gl.RGBA,a.gl.UNSIGNED_BYTE,px);
    const ray=a.rayFromRegionPoint(cam,ix+.5,iy+.5),hit=a.hitField(ray,f,20000);
    if(!hit||!hit.point)throw Error('fixture did not give a definite CPU hit');
    const depth=(a.m4point(cam.vp,hit.point)[2]+1)/2,gd=(px[0]*65536+px[1]*256+px[2])/16777215;
    const gp=a.m4point(cam.inv,[2*(ix+.5)/ls.used[0]-1,1-2*(iy+.5)/ls.used[1],2*gd-1]);
    const ext=a.gl.getExtension('WEBGL_debug_renderer_info');
    return {pixel:[ix,iy],viewport:ls.used,active:hit.active,cpuDepth:depth,gpuDepth:gd,depthError:Math.abs(depth-gd),
     worldErrorMM:1000*Math.hypot(...gp.slice(0,3).map((v,i)=>v-hit.point[i])),rgba:Array.from(px),
     samples:a.gl.getParameter(a.gl.SAMPLES),glError:a.gl.getError(),shaders:a.shaderLog,
     renderer:ext?a.gl.getParameter(ext.UNMASKED_RENDERER_WEBGL):a.gl.getParameter(a.gl.RENDERER)};
   },scenario);
   assert.equal(r.glError,0);assert.equal(r.shaders.main,'');assert.equal(r.shaders.field,'');assert.equal(r.samples,0);
   console.log(JSON.stringify({shifted,scenario,...r}));
  }
  assert.deepEqual(errors,[]);
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
~~~
<!-- probe-B:end -->
