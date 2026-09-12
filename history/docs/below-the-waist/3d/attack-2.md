# Attack 2 — a curved region and a correspondence that can answer the next query

2026-09-06. The definer's chair, phase B of [two-chairs.md](../two-chairs.md), against the composer's `2737f4173b3e9f310ba2b593de3c95ede705ad58`. This takes up [HANDOFF-3 §6](HANDOFF-3.md): finite surface distance on a doubly curved host, then one authored mark through a chart change and a merge of two retained records on that same host.

**The picture holds. The two constructions now run beside the bench.** A 75 mm intrinsic mark includes a point at surface distance **72.273425 mm** that a metric held fixed at its seed puts **78.539816 mm** away. The retained-mark sequence changes charts, merges two support domains, and recovers the original mark and source parameter from a fresh ray. Its source bytes stay identical. A second fresh query, in a new Node process after the old evaluation objects have been discarded, still respects each old record's restriction.

What this adds to the contract is executable specificity: the distance domain and its bounds belong to the region; completeness belongs to the requested answer; a retained correspondence keeps what a later forward or backward demand needs. These add obligations to existing outputs, with no new piece. The exact sphere construction supplies the first full curved receipt. An ellipsoid construction supplies bounded answers and a deliberately unresolved demand, so the sphere's closed form is not passed off as a general solver.

**Custody and scope.** Attack 1 is folded at full weight. Its six checks in [fact-base-2 Part 14](fact-base-2.md#part-14--the-fold-of-the-definers-attack-1-the-chair-after-2026-09-06-afternoon) are carried as the composer's reproduced receipts, not redone or challenged here. The new runner extracts `over` and `composeAt` verbatim from the committed bench and calls them with this attack's records. Its sphere, distance-bound and correspondence procedures are **new reference code**, not existing bench or client capabilities. There is no new browser receipt, GPU coverage receipt, CAD Boolean implementation or production executor here.

The composer's source and page stay intact. Position 9's 3D half stays Sid's and already asked. The committed path ledger's 11:27 swept-nib and executor rulings, its 11:45 stance, and its 11:51 choice of **one compositor with paint and sample** are carried. An ordered blend in the records below is this construction's input, not a new ruling about merge policy. The notation below does not select the 3D language.

**1. The runnable material.**

| File | Contents |
|---|---|
| [attack-2/records.json](attack-2/records.json) | Authored construction inputs: the sphere, reach tool, ellipsoid probe and sequential support edits. |
| [attack-2/run.mjs](attack-2/run.mjs) | Node reference capabilities and the calls that produce the numbers below. Reads the pinned committed bench, with no live artifact URL. |
| [attack-2/receipts.json](attack-2/receipts.json) | Complete numerical output, source hashes, per-revision queries, negative controls and limits. |
| [attack-2/continuation.json](attack-2/continuation.json) | The retained records, current support, relation descriptions, old authored-domain restrictions, saved order and a new ray. Old evaluation slots 0 and 1 are `null`. Consumable in another process. |

From the repository root:

```sh
node docs/below-the-waist/3d/attack-2/run.mjs
node docs/below-the-waist/3d/attack-2/run.mjs --cold < docs/below-the-waist/3d/attack-2/continuation.json
```

The first command asserts the construction's invariants and prints the receipt. `--write` additionally regenerates the two returned JSON files. Node v20.20.2, built-ins only. The runner is bench evidence under `docs/`, following the round's requirement that records land beside the attack; it changes no client implementation. Code inspection stayed under `src/app/client/`, with the explicitly supplied bench source as the prototype exception. No server source was opened.

**2. Tool one: surface reach, with a finite distance rather than a local ellipse.**

Take a sphere `G`, radius `R = 100 mm`, with Y up:

```text
S0(u,v) = (R cos(v/R) cos(u/R), R sin(v/R), R cos(v/R) sin(u/R))
u = R longitude; v = R latitude
metric g0(u,v) = diag(cos²(v/R), 1)
```

The coordinates have length units, but a u-unit is not one millimetre of surface length away from the equator. Both principal curvatures are nonzero; no planar development preserves this metric. The example's source is a tap at longitude 180°, latitude 60°, with a round intrinsic radius 75 mm. A tap is the one-location member of the swept nib; §4 also executes a finite 40 mm source arc.

```text
surface-reach record
  support: G@0, radius 100 mm
  seed: (longitude 180°, latitude 60°)
  distance: shortest path on the entire supporting sphere
  radius: 75 mm
  return: retained intrinsic region, reusable as a clip

region membership at q
  d(q,seed) <= radius
  d(p,q) = R atan2(|p̂ × q̂|, p̂ · q̂)
```

The sphere formula is a closed-form reference for this support. It measures the shorter great-circle distance, as also specified by the [Wolfram spherical-distance operation](https://reference.wolfram.com/language/ref/SphericalDistance.html). Here the implementation uses `atan2` of cross and dot products so the computation remains useful near coincident and antipodal points. The model equations are exact; the executable numbers use binary64.

| Demand against the 75 mm record | Intrinsic answer | Competing calculation | Consequence |
|---|---:|---:|---|
| Same latitude, longitude 270° | 72.273424781342 mm | Freeze `g0` at the seed: 78.539816339745 mm | Intrinsic **inside**; frozen-metric ellipse **outside**. |
| Same longitude, latitude lowered by 0.76 radians | 76 mm | Straight chord: 74.184093882597 mm | Intrinsic **outside**; ambient ball **inside**. |
| North pole | 52.359877559830 mm | Longitude coordinates are singular there | Inside, with a regular polar-chart location. |

The first row uses `cos(distance/R) = 3/4`; it is not a numerical search result. The second is a meridian arc of known length. A metric tensor is enough to specify local lengths, but computing a finite neighborhood requires an integration/shortest-path operation or a support-specific closed form. Evaluating one tensor and drawing an ellipse does not perform that operation.

**Change the tool through its record.** Set radius 75 → 70. The first point becomes outside without changing capability code. Keep the old returned region as another construction's clip. Sampling green premultiplied paint `(0, .4, 0, .4)` through that retained 75 mm clip still returns `(0, .4, 0, .4)`; through the new 70 mm result it returns `(0, 0, 0, 0)`. The receipt calls the clip consumer on both values. The returned region retains its seed, support revision, metric interpretation and radius; its content is not an unbound polygon or a pointer to the tool's latest radius.

**Bounds are part of the result.** With unit seed normal `n = (-1/2, √3/2, 0)` and angular radius `ρ = .75`, the region is `n · p/R >= cos ρ`. Its surface area is `2πR²(1-cos ρ) = 16858.48556844744 mm²`. Its axis-aligned world bounds are:

```text
x: [-95.616091682125,  22.447204794743] mm
y: [ 29.284176809936, 100.000000000000] mm
z: [-68.163876002334,  68.163876002334] mm
```

These displayed bounds are rounded outward. For axis component `n_i`, a constrained extreme is `R(n_i cos ρ ± √(1-n_i²) sin ρ)`, unless that axis's positive or negative pole lies inside the cap, in which case the extreme is `+R` or `-R`. This proves the real-arithmetic bound over the whole region. The runner additionally checks 4,320 constructed interior/boundary locations against it: zero violations with a `1e-9 mm` floating-point comparison allowance. That sampling is a numerical check of the formula, not the proof of conservativeness.

**Chart pieces need to cover the physical region, including its pole.** Use three pieces of the same region: longitude in `[-π,0)` and `[0,π)` below or at latitude 70°, and a north chart above 70°. Each piece intersects the cap predicate. The north chart is `(x,z)`, with embedding `(x, √(R²-x²-z²), z)` and its induced metric. It is regular on this part of the northern hemisphere, including `(x,z)=(0,0)` at the pole. The longitude branches are normalized to `[-π,π)`; the artificial boundaries have single ownership in this reference partition.

Every sampled location has one owner. The pole resolves to one chart piece and one logical half-opacity contribution, alpha `.5`. Depositing the same contribution twice would give `.75` through the bench's `composeAt`. This is an arithmetic ownership check, not an antialiasing or raster-seam receipt. A packer may use overlapping execution charts, but their overlap cannot create another authored deposit. All pieces refer to the same region and source; an atlas boundary adds no mark endpoint.

**Boundary continuation must say where distance paths may run.** Remove the full open latitude band `-5 < v < 5 mm`. Put a seed at `(u,v)=(πR,10)` and query at `(πR,-10)`, both on retained support, with radius 25 mm. On the untrimmed sphere their distance is 20 mm. Calculating there and clipping the answer to the retained support includes the query. Requiring the path itself to stay in the trimmed support gives no connecting path: the components are disconnected, distance `∞`, definite outside. This is a topological proof used by the fixture, not a general obstacle solver. Both constructions can be named. A saved `clip` at the end does not choose between them.

**A nonunique witness need not make membership unresolved.** At the seed's antipode the distance is exactly `πR = 314.1592653589793 mm`; every great semicircle is a shortest path. Radius 314 is definitely outside; radius 315 is definitely inside. A tool asking for the initial drag direction or a transported brush orientation has no unique direction from this answer. The distance/membership answer is complete while the path/direction answer has a family of candidates. Do not collapse geometric multiplicity into “the solver has not finished,” or treat a definite distance as a unique transport map.

**3. Leave the sphere: an ellipsoid that must return a bounded unknown.**

Use the smooth triaxial support

```text
E(n) = (120 n_x, 100 n_y, 80 n_z) mm, |n|=1
seed = (120,0,0); query = (0,100,0)
```

The capability in this attack does not solve every geodesic on E. It supplies two globally valid kinds of evidence. Any connecting surface path is at least the endpoint chord `√(120²+100²) = 156.204993518133 mm`. Mapping a spherical path through the diagonal transform also gives the lower bound `80 × π/2`; the chord is stronger here. The mapped equatorial quarter-circle is a **feasible path**, so its length is an upper bound on the shortest distance, without claiming that this path minimizes it:

```text
γ(t) = (120 cos t, 100 sin t, 0), 0 <= t <= π/2
|γ′(t)| = √(120² sin²t + 100² cos²t)
```

The speed increases on this interval. Left/right rectangles with 1,024 panels bound that feasible path's length. With outward rounding for the printed numerical receipt:

```text
feasible-path length: [173.129439, 173.160121] mm
shortest distance:    [156.204992, 173.160121] mm
```

The second interval is the distance answer. The first interval is **not** a lower bound on the shortest distance.

| Radius | Result at `(0,100,0)` | Evidence |
|---:|---|---|
| 150 mm | Outside | Lower bound exceeds radius. |
| 160 mm | **Unresolved** | The distance enclosure straddles the requested threshold. |
| 175 mm | Inside | One exhibited surface path is shorter than radius. |

The unresolved result identifies `E@0`, the seed, the demanded point, the distance enclosure and the exhausted capability's scope. It does not report outside. Tightening this same path quadrature indefinitely will not close the 160 mm demand: it needs a stronger lower bound or another search/certification operation. This distinguishes unfinished geometric work from insufficient arithmetic precision. The real-arithmetic inequalities are proved above; the micro-millimetre outward padding is a practical binary64 allowance, not a verified interval-arithmetic implementation.

**The capability owed below the waist**, in the existing geometry service, is therefore:

```text
surface-distance-region(
  support value/revision, evaluable metric and valid domains,
  source curve/location values + radius function and units,
  allowed path domain + boundary/adjacency continuation,
  requested membership/boundary/path/source witnesses,
  error demand + work demand)
  -> retained region with those dependencies
     membership answers with distance/clearance enclosures
     conservative world/domain bounds
     chart pieces with source relations and overlap ownership
     resolved coverage, explicit unresolved portions
     witness candidates where the requested witness is nonunique
```

For a sweep its meaning is `inf_t (d_allowed(q,c(t)) - r(t)) <= 0`. On a general support, evaluating that infimum and certifying its coverage are also work; endpoint balls and a few sample queries do not suffice for arbitrary curves/radius functions. The sphere runner supplies exact balls and constant-radius minor-great-circle sweeps. The ellipsoid runner supplies the bounded point demand above. Neither supplies arbitrary trimmed-surface distance refinement.

A bought operation still has a particular geometric subject. For example, [CGAL's shortest-path package](https://doc.cgal.org/latest/Surface_mesh_shortest_path/index.html) computes on an input triangulated surface and accepts points in its faces; it contrasts that route with approximate heat-method distances. **Inference for this exchange:** even an exact shortest path on that mesh is a statement about the mesh metric. Applying it to a smooth authored face needs a declared approximation/correspondence and a distance-error argument, or it deliberately makes the mesh the supporting truth. A display tessellation tolerance by itself is not such an argument. No library is selected here.

**4. The closing receipt: one mark, rechart, merge, fresh inverse query.**

Stay on the same radius-100 sphere G. Work with two overlapping trimmed domain values A and B on it. This is the exact union of rectangular parameter domains on one known surface, not a claim to have implemented a general solid Boolean or rendered coincident geometry sheets.

| Retained root | Authored coordinates | Valid subset |
|---|---|---|
| `A@0`, red `kA` | `(u,v)` in S0 | `u ∈ [πR-80, πR+80]`, `v ∈ [60,145]`, mm |
| `B@0`, blue `kB` | `(ξ,η)`, mapped by `β(ξ,η)=(ξ+πR, η+100)` into S0 | `ξ ∈ [-5,100]`, `η ∈ [-40,45]`, mm |

`kA` is an authored 40 mm minor great-circle arc with one identified segment `arc-AB`, an intrinsic swept radius 8 mm and premultiplied red `(0.5,0,0,0.5)`. Let `n=(-1/2,√3/2,0)`, eastward tangent `e=(0,0,-1)`, and source parameter `t∈[0,1]`:

```text
θ(t) = (40t-20)/100
c(t) = 100 (n cos θ(t) + e sin θ(t))
```

The authored support-coordinate curve is `S0⁻¹(c(t))` in A's unwrapped domain. Its source ID and parameterization remain saved. The finite sweep's closest-center calculation maximizes `q̂ · (n cos θ + e sin θ)` on the closed interval `[-.2,.2]`, checking its stationary maximum and both endpoints. Thus it computes an actual spherical sweep, not a Euclidean capsule evaluated in these UV coordinates.

`kB` is a separately retained tap centered at `c(.65)`, radius 12 mm, premultiplied blue `(0,0,0.5,0.5)`, authored in B's coordinates and clipped to B's support. Its source region is retained whole; its binding is restricted. The initial saved composition is `[kA,kB]`. There is no retroactive interleaving or replay of painting histories.

**Evaluation 0 → 1: change coordinates only.** Both domains remain on G. Introduce

```text
φ(u,v) = ((u-100)/2, v) = (a,b)
S1(a,b) = S0(2a+100,b)
```

A's new binding is `φ`. B's is `φ∘β`. The runner constructs these from the previous bindings; it does not regenerate either mark. At the longitude seam, the source parameters immediately before, at and after `.5` remain distinct locations of `arc-AB`.

**Evaluation 1 → 2: merge A and B, retaining both bindings.** Their union is one domain M with base coordinates `u∈[πR-80,πR+100]`, `v∈[60,145]`. The merger also chooses a new chart, so accidentally treating the latest relation as the entire relation will be visible:

```text
χ(a,b) = (a + b/4 + 30, b) = (c,d)
S2(c,d) = S0(2c - d/2 + 40, d)

A@0 -> A@1 -> M@2: χ∘φ, restricted by A@0's domain
B@0 -> B@1 -> M@2: χ∘φ∘β, restricted by B@0's domain
```

Both compositions are executable from the returned descriptors, in both directions. The valid subsets are still tested in their own root domains. They do not become M's whole domain merely because M has one face ID. The metric in chart 2 at latitude 60° is `[[1,-.25],[-.25,1.0625]]`: the shear introduces off-diagonal terms. More generally it is `Jᵀ diag(cos²(d/R),1) J`, `J=[[2,-.5],[0,1]]`. It is a per-location tensor, not a constant attached to the sphere.

**The actual fresh query.** Send a normalized radial ray from `3c(.65)` toward the sphere. It hits at ray distance 200 mm and world point `(-49.910026996760, 86.446702565523, -5.996400647944)` mm. The query starts with that world ray each time, then locates the support, reads the current chart, inverts each retained binding, applies its restriction, and queries its retained region.

| Evaluation | Support/chart location, mm | Returned source address |
|---|---|---|
| 0, A and B in S0 | `(326.116373773273, 104.408915755942)` | `kA@0 / A@0 / arc-AB / t=.65`, and `kB@0 / B@0 / tap` |
| 1, A and B in S1 | `(113.058186886636, 104.408915755942)` | Same source addresses and authored coordinates |
| 2, M in S2 | `(169.160415825622, 104.408915755942)` | Same source addresses and authored coordinates |

B's independent authored location is `(ξ,η)=(11.957108414293,4.408915755942)` mm. The computed A parameter is `.6499999999999997`. Across the six sampled source parameters, the maximum world reconstruction difference at every stage is `2.803043812716e-14 mm`; inverse chart error is zero in this run. Algebraically `S2∘χ∘φ = S0` throughout the valid domain, so the all-point preservation claim does not depend on checking only six points. Pure reparameterization has model error zero; these floating-point residuals are not a new modeling tolerance.

The source-record hashes, unchanged through both edits:

```text
kA: ca9267c4f7c580384b46f7761f8e5e8634cd4c16b63be30d8eab7983887f935d
kB: 8b06519551c429d5c9d887a69ce248a38e724cc72c500c0c187b269ee3c36772
both records: 4d4a5d63d68f2106dc7589b8602a38ca5ca5ee2256643af058e1d6ad9f0b6dc8
```

At the overlap, the **committed bench's** `composeAt` returns blue over red `(0.25,0,0.5,0.75)`, reversed order `(0.5,0,0.25,0.75)`, and `needs-policy` with both contributors when order is absent. These are the new sequential bindings feeding the established composition operation; the layer arithmetic itself was already settled by attack 1.

**The restriction has a discriminating query too.** At source parameter `.41`, the sphere point is only 9.6 mm from the blue tap's center, inside its retained 12 mm region. But its blue-root coordinate is `ξ=-7.190692912533`, outside B's old lower bound `-5`. The merged binding therefore returns only red `kA`. Replacing the two old restrictions with M's domain would wrongly expose blue that never had support there. The new-process continuation query uses this point, which was not one of the six forward checks.

**5. What on-demand correspondence has to retain, and what pending means here.**

The complete sequential case succeeds with two representations of the same relation. The first retains `β`, `φ`, the two merge pieces, root restrictions and support/mark values, and composes the maps when queried. The second materializes the two composed affine maps, retains the same root restrictions and values, and discards its relation graph. Both give the same current inverse coordinates, contributors and composition.

The on-demand variation gives A's relation zero remaining work and B's final relation one unit of work. A query budget of zero intentionally withholds that B answer; this is a deterministic demand test, not a measurement of geometric algorithm cost. At the overlapping ray:

```text
support: G revision 2, M, t=200 mm, complete
known coating contributor: kA
unresolved: B-merge, B@0 -> M@2, at the demanded chart point
composition: unresolved, rgba=null; kB may contribute
```

Grant that one unit: the same current geometry and unchanged source records yield both contributors and `(0.25,0,0.5,0.75)`. A support hit, a coating membership answer and the composed coating color are different demands on the same evaluated result. The missing attachment answer does not make the already-known sphere intersection uncertain; it does prevent treating the red-only color as the complete current coating. Presentation and selection must consume the status of the quantity they requested.

The returned [continuation bytes](attack-2/continuation.json) retain the capability descriptions and values necessary to answer an unseen point. The runner starts a separate Node process with those bytes, without the old evaluation objects or previous point-query results. Its new `.41` ray recovers `kA / arc-AB / .41` and excludes B by B's retained restriction. A queryable reference can satisfy the obligation through retained/recomputable values; it does not have to materialize a continuum of point correspondences. This demonstrates local serialization and lifetime sufficiency for these explicit maps, not persistence of arbitrary external kernel handles or cross-machine executable-version compatibility.

Three negative controls make the sequence discriminate:

| Omission | Measured consequence |
|---|---|
| Apply only the latest `χ` to A's original coordinates, omitting `φ` | **85.192705396047 mm** world displacement at the `.65` point. |
| Omit unresolved B from the composition inputs | The bench compositor quite correctly returns red `(0.5,0,0,0.5)` for those incomplete inputs; treating it as the complete coating is the caller's false claim. |
| Remove retained `φ` without a composed replacement | The runner returns `unresolved: missing retained dependency` for the affected bindings. It cannot answer the next inverse demand by assigning `merged` to the support. |

These deliberately broken variants are controls in the new runner, not allegations of unfixed defects in bench 2. The existing bench explicitly says it lacks this combined sequence. Its `evaluateP/resolveWall` operate on the cylindrical wall; `mergeCorrespondence` operates on the box top and reads the box's slab. There is no existing same-support call chain joining them. This attack supplies that chain as new reference work.

**Position on the handoff's exact question.** I keep (b): a revision-bound correspondence value with demanded forward/backward answers and explicit unresolved portions. This sequence supports it, and (a) works as its fully materialized case. The contract equivalence requires that either returned value retain enough to serve its declared future demands. A table of previously answered points, a dead handle or only the final edge in a transition chain is neither a complete map nor a sufficient queryable relation.

This is the closing numerical receipt asked for, **on the companion Node route**. It starts at a spatial ray; page/portal routing is inherited, not re-exercised. The composer still owes its fold into the interactive bench. It does not close the general CAD adapter, the GPU implementation or Sid's 3D-language decision.

**6. Precisely what needs code, and which choices remain records.**

| Capability below the waist: input → output | This attack's implementation | Choice remaining in the record |
|---|---|---|
| Intrinsic surface region: support/metric/revision, source locations/radius function, path domain, accuracy/work demand → region, bounds, pieces, membership/witness/status answers | Exact sphere balls and constant-radius great-circle sweeps; bounded ellipsoid point demand. General distance refinement, arbitrary sweeps and boundary search remain absent. | Surface-distance, projected or UV tool; radius response; whether paths may cross a trim boundary; requested witnesses. |
| Atlas adaptation: region plus surface charts/transitions/valid subsets → chart pieces with region/source correspondence and logical overlap ownership | Sphere longitude partition plus regular north chart, checked at seam-bearing and polar locations. No GPU antialiasing demonstration. | Authored domain and material role; a chart split is an execution choice and cannot introduce another deposit. |
| Retained relation composition/inversion: revisioned pieces, source-domain restrictions, content rules, demand → current bindings or localized unresolved answers with retained dependencies | Explicit affine chart maps over one curved support, exact rectangular-domain union, both root bindings, lazy and materialized forms, cold inverse demand. No general kernel-history adapter. | Preservation, clipping and overlap order; baking is a separate returned value if chosen. |
| Compose/sample a coating: resolved region contributions, clips and saved order → premultiplied material value or unresolved/needs-policy | The bench's own `over/composeAt`; an old region reused as a clip. | Layer order and which retained result a later brush reads. No pickup brush replay is exercised. |

The checked production boundary remains [on_plane.cljc](../../../../src/app/client/region3d/on_plane.cljc#L72): its documented inverse intersects local `z=0` and returns planar placement coordinates. That source fact establishes neither a curved locator nor a surface-distance region. The new operations belong inside the geometry, attachment and compositor exchanges already on the picture. A small calling surface can hide substantial numerical work; these records now identify exactly which work it owes.

**7. The fold I would make.** Keep the space/thing/view/portal picture, the three requests, authored-domain custody, every attack-1 landing, and both retained bindings on a merge. Add these obligations to their existing return values and make the records above fixtures:

1. **A surface-distance region retains its distance subject.** Support revision, metric, allowed path domain and source radius/curve semantics travel with it. Its finite-region bounds and chart coverage are answers the operation must produce, with unresolved portions where necessary.
2. **Completeness is per requested quantity.** Membership may be definite while a shortest-path direction has multiple answers. The surface intersection may be definite while an unresolved coating binding prevents a definite composed color or coating selection. Neither nonuniqueness nor missing work may be silently converted into absence.
3. **A correspondence remains usable beyond the points already queried.** Compose every required transition and its restrictions, or retain enough to answer/recompute them later. A fresh inverse demand after the intermediate evaluation is gone must resolve correctly or report the precise missing dependency.

These are positions with bounded exits. Intrinsic distance is the meaning selected by this tool; a projected decal or deliberately UV-authored brush selects another metric and has different numbers. On-demand correspondence is a workable default when its dependencies can remain available and pending is supported. An execution tier that cannot retain/recompute them must materialize the required relation before releasing them, or expose that loss of capability. Multiplicity can be resolved by an explicit tool policy when that tool needs one witness; the metric's distance answer does not acquire that policy automatically.

The two owed constructions have records and executed numbers. The sphere's symmetry and the analytic merge remain the limits of the complete geometry shown here. General trimmed-surface refinement, the pickup brush's replay on the post, the path filler's projection, field depth parity, travel, query modes and the recursive portal receipt keep their separate places on the composer's handoff. Nothing in this attack authorizes production work or asks Sid's already-asked positions again.

**Source record.** The supplied untracked `HANDOFF-object-space-and-query.md` was read as that handoff, SHA-256 `64acfd80b3b51ab7c86783770dde522cc38e2a8441e0e45b0a3c0661e995eab7`; it has no committed version in this checkout. All composer artifacts below were read at the fixed commit above. A read-only collector mapped the exact bench functions under the round's scribe rule; the primary read those function slices, the relevant page exchange/positions, the two handoffs, attack 1, the handover and Part 14. No live artifact was read or published. Client reading followed the client and Region3D folder maps to the plane inverse. Only this attack and its companion evidence files are this chair's mutations.

| Committed input | SHA-256 |
|---|---|
| `attack-1.md` | `aa16a7ad5e621d2770befe18ba776b816c2274781b4b983f5490a9b453e2d57e` |
| `HANDOFF-3.md` | `fe307fcaa6c132e566d8619ae07f14a0f196ca87cfb576b0615b247b885e49da` |
| `3d-kind-2.md` | `b3674cfcc8119f5c7ec7b9cc83f2ef35ba29ac10c8f066e71923d3152664a603` |
| `fact-base-2.md` | `6b34fa8aab7e7b1fe15739a793be27a29bb5cce1e3dae1eb3bd05ac9cc383e34` |
| `bench-2/HANDOVER.md` | `3135e85c244f64a76212c9518cd8106f89fc3a861d0e2e9245e51a9b3897b840` |
| `bench-2/seam-bench.html` | `010c91d4e0cd4b3dad30208a5bf4b729124483374f8d8a32b8903ae00824c807` |

The exact borrowed `over/composeAt` source bytes have SHA-256 `193c03db0dbd22702e97f420ae77c0be8ea3f57282de91886bcd30c5a17edf65`. The emitted receipt records this and the fixture hash, so later folds can distinguish running this reference from running the changed interactive bench or the production client.
