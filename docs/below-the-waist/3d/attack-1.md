# Attack 1 — a tool keeps its meaning when its support changes

2026-09-06. The definer's chair, successor to [3d-object-space-and-query.md](3d-object-space-and-query.md). A contribution to the composer's continuing [3d-kind-2.md](3d-kind-2.md), before a contract. Sid carries it between chairs. These are positions, constructions and explicit limits; no reserved question is closed here.

**The proposal that survives the three tools:** keep the editable definition, the domain in which content was authored, and the relation to its current spatial support separately. Evaluation produces typed values and evidence about their correspondence. An attachment construction consumes that evidence and its own preservation rule. Rendering and queries consume the resulting bindings at the same evaluated revision. A geometric merge can carry two independent paint records onto one face. An implicit object can answer a ray without inventing a face. A section can return editable geometric values without pretending that a projection has a unique inverse edit.

This retains the composer's space, occurrence, view, portal and shared evaluate/render/query picture. The addition is inside the exchanges: a face frame becomes a domain mapping; a part lineage becomes a relation over portions of domains; a hit location need not be a named subpart; a merged support need not acquire a single merged canvas. The operations below make those statements concrete.

The reading follows [HANDOFF-2.md](HANDOFF-2.md), [fact-base-2.md Parts 11–13](fact-base-2.md), the predecessor and its [handoff](HANDOFF-object-space-and-query.md), [bench 2's handover](bench-2/HANDOVER.md), [HANDOFF-8 §3](../path-kind/HANDOFF-8.md), the current [path picture](../path-kind/path-kind.md), the [ceilings](../3d-ceilings-starter.md), and Sid's [waist direction](../../../vision/LOG.md#2026-09-02--the-waist-round-two-the-ecs-layer-dead-means-wrong-form-a-data--b-gpu). The current path picture has already folded attacks 2–4: retained painting surfaces, explicit continuation state and reusable returned values are available proposals to compose with. Its older handoff's separate raster lane is not the current position.

The box widening, split, three anchors and clip/detach behavior remain bench 2's recorded evidence. This contribution extends their meaning; it does not claim to have run those browser cases again. New evidence here is source inspection at the relevant client boundaries and small arithmetic checks, described at the end. There is no new running engine or interactive bench in this file. Construction notation is illustrative, not a final schema.

At the final read, the path ledger advanced in commit `81bf26e`: it records Sid's 2026-09-06 11:27 choices of the swept round nib and the executor—records over a capability vocabulary, an ordering rule and compiled expressions. This contribution carries those recorded choices forward. The compositor remains asked there. The 3D page still carries its older Position 9; the specific 3D inputs, capabilities and returned relations below remain proposals, without re-asking the common executor choice or assuming a decision about a broader code-as-data escape hatch.

**1. What the object and the hit must preserve.** A geometric 3D thing is an identified definition whose spatial description supports particular operations. An occurrence places that definition in a space. Material bindings describe its appearance when it is drawn. A definition can be useful to a measurement or construction before any material or view is assigned. This is the predecessor's distinction between authored meaning and evaluated imaging input.

The same test applies to a 2D path, a solid and a field: retain whatever the next intended operation needs. A field's formula, a CAD feature's parameters and an artist's mesh topology are different legitimate sources. One protocol can carry their answers without giving them identical geometry.

| Identified value or location | What it identifies | What must not substitute for it |
|---|---|---|
| Definition and its authored elements | The thing edited: feature, parameter, expression node, curve knot, paint operation | An occurrence's transform or a display allocation |
| Occurrence | This use of that definition, with placement and optional local overrides | The shared definition; two copies can have different annotations |
| Evaluated part | A face, edge or other derived feature whose continuity the construction can establish | A fresh triangle index or a position in a newly sorted output list |
| Content domain | The coordinates, units, bounds and interpretation in which a path or painting was authored | Whichever UV atlas the present renderer happens to use |
| Query location | A point, interval or contributor set in a particular evaluated representation and revision | A promise of a persistent material point through arbitrary future edits |

Stable identity does not mean assigning an enduring ID to every point of a continuum. A hit on a field can identify the definition and a local point at revision 7. Keeping an annotation at that point after the field changes is another construction, with another preservation rule. Conversely, a named face alone does not identify where on that face an annotation belongs.

The public result of a query therefore has two parts: **the durable thing addressed**, and **the location/evidence obtained in this evaluation**. A mesh editor may return an authored polygon and barycentric coordinates; an implicit tool may return a field expression and an object-local point; a painting tool may return a mark and several source segment parameters. All can return the same occurrence route around those representation-specific facts.

```text
query result
  observed evaluation + view/query revisions
  route through occurrences, attachments and portals
  addressed definition/content IDs
  location: representation-specific coordinates, interval or contributors
  geometric/visibility facts, achieved error, status

tool action
  tool definition + that result + gesture state
  -> proposed patches to named authored values + next gesture state
```

The store above the kind accepts and merges authored patches. The kind evaluates a coherent version and reports affected values. It does not decide how two people merge a feature edit, a brush history or competing layer orders.

**2. Tool one: an implicit body whose behavior is saved data.** Use a field construction, with a ray query and an extent-editing tool, that the engine has no hardcoded shape name for:

```text
definition F
  unit: mm; frame: object local
  parameters with IDs: a = 2, b = 1, c = 1; all strictly positive
  expression with node IDs:
    f(x,y,z) = (x/a)^4 + (y/b)^4 + (z/c)^4 - 1
  interior: f <= 0; boundary: f = 0
  bounds construction: [-a,a] x [-b,b] x [-c,c]
  supplies:
    sample -> f and its quantity tag
    inside -> sign classification
    ray-events -> polynomial substitution, root isolation, classification
    normal -> normalized gradient where defined

extent tool
  binding: selected definition + selected parameter/axis
  begin: capture the definition revision, parameter ID and drag frame
  update: displacement in that frame -> set that named parameter
  return: F, reusable with its operations and expression intact
```

The tool runs four actual steps. It resolves the clicked occurrence to `F`; its saved axis binding selects `a`; its drag construction converts pointer motion into the chosen local length and applies its declared positive-extent constraint; evaluation substitutes the new parameter into the expression and bounds. A bare surface hit does not uniquely select `a` rather than another parameter. The handles or another saved edit rule supply that intention.

For the ray `p(t) = (3,0,0) + t(-1,0,0)`, the body at `a = 2` has events at `t = 1` and `t = 5`. The first hit is `(2,0,0)`, with normal `(1,0,0)`. At `a = 1`, the events move to `2` and `4`. The query addresses `F`, its boundary interpretation and that local point; **there is no face ID to return**. The expression and parameter IDs are available to the tool as source references. They are not an assertion that the visible point belongs exclusively to one parameter.

Changing the tool through data can change the construction, not just the dimensions. Add an inner field and replace the boundary rule with `max(f_outer, -f_inner) <= 0`. With outer x-extent 2 and inner x-extent 1, this ray lies in material on `[1,2]` and `[4,5]`. The saved program obtains the leaves' root events, orders them and evaluates the Boolean expression between events. The same boundary exchange now describes a hollow object. At a meeting of active branches, it can return several contributing expression nodes or an undefined normal. No new `:hollow-superellipsoid` engine kind is required.

The extension is nevertheless not supplied by today's primitive dispatcher. The proposed field program needs executable arithmetic, iteration over events, named value bindings and a spatial consumer interface. For this bounded polynomial construction, a concrete reusable numerical operation is:

```text
isolate-real-roots(coefficients, closed t interval,
                   requested t error, numeric rules, work limit)
  -> ordered root intervals, multiplicity/contact information where known,
     covered root-free intervals, unresolved intervals, status
```

The query may report a definite miss only when its relevant interval is covered. A tangent can have no sign change: `f(t,1,0) = t^4/a^4` touches zero at `t = 0`. A sign-change-only loop would miss it. A finite work limit must return unresolved work instead of silently converting it to empty space. With non-unit ray direction, a `t` tolerance and a physical-distance tolerance require an explicit conversion.

This field is not a signed distance. At the ray origin its value is `4.0625`, while the first boundary is one millimetre away. Treating that number as a ray step has no distance justification. Distance-based tracing needs a distance bound or the corresponding derivative/Lipschitz control; Hart's formulation makes that precondition explicit. [Hart, Sphere Tracing](https://graphics.stanford.edu/courses/cs348b-20-spring-content/uploads/hart.pdf). Root isolation is the chosen construction here, not a claim that every field should use that algorithm.

For direct opaque imaging, the saved field program must be able to contribute an intersection event to the same visibility computation as meshes:

```text
boundary event
  ray parameter interval in the named host frame
  position, oriented normal or its failure status
  occurrence/source location and active expression references
  material input/binding and query precision
```

A coloured rectangle with a raymarch shader is insufficient if the scene still treats its proxy rectangle as the object's depth and its query system cannot call the field. Either use these events in a supported ray-based renderer, or derive a mesh with a source relation and an explicit approximation. A direct field pass combined with mesh rasterization needs the field's actual boundary depth and the renderer's sidedness/visibility rules. The same participation is needed in shadow queries if the object casts shadows. The return shape of an event does not implement that integration.

The **required execution capability** is running a saved spatial program and allowing its results into geometry queries and image visibility. The polynomial solver is real missing algorithmic work, but need not be a permanently hardcoded shape primitive: a sufficiently capable saved program could implement it; an accelerated numerical library could also supply it. Today's engine supplies neither route through its closed mesh input. Naming an evaluator does not supply the solver or make a per-pixel reference solver fast.

The reusable result is still the field definition. Another construction can query its interior, take a slice or derive a mesh. An arbitrary sampled volume is a further case: grid mapping, interpolation, missing data and the selected isovalue would become inputs, and the polynomial procedure above would not automatically apply. The shared exchange survives; the algorithm and guarantees change.

**3. Tool two: paint and vector editing across a curved seam.** Give a cylinder of radius 20 mm an authored patch that crosses its parameterization seam. It may be the inside wall of the CAD bore in tool three. Its local supporting surface is:

```text
C = 40*pi mm
S0(u,v) = (20*cos(u/20), 20*sin(u/20), v)
u is unwrapped arc length; v is axial distance, both in mm
S0(u+C,v) = S0(u,v)
```

The supporting face also supplies its valid trimmed domain and orientation. On an inside bore wall the face orientation reverses the radial normal. Coordinates alone do not select the material side.

The saved content domain is the unwrapped rectangle `[C-12,C+12] x [28,52]` mm, narrower than one circumference. Its boundary is an authored content boundary, separate from the surface's UV cut. The mark has four identified knots and three identified line segments:

| Knot | Position in the authored domain, mm | Pressure |
|---|---|---:|
| A | `(C-8, 32)` | 0.2 |
| B | `(C+8, 48)` | 0.8 |
| Ck | `(C-8, 48)` | 0.9 |
| D | `(C+8, 32)` | 0.5 |

The first and third segments cross at `(C,40)`. Pressure is interpolated along each segment, then the saved response computes `width(p) = 1 + 3*p*p` mm. The widths at that crossing are 1.75 and 2.47 mm. Widths interpolated from already-squared endpoint values would describe a different tool. Lines are legitimate curve-consumable values; a fitting stage can later replace them with identified curves while retaining the gesture evidence and its source relation.

```text
annotation tool record
  captured occurrence/support + authored domain + attachment rule
  gesture samples and their acquisition context
  editable path and pressure-response construction
  tip construction: swept round nib for this example
  overlap: union; colour; opacity: 0.5
  reads as: coating or unlit annotation, explicitly selected
  output: editable mark + reusable region + support binding

painting variant of the record
  ordered dabs derived from the path and brush-spacing rule
  input painting value/revision; brush state; sample/mix/deposit steps
  output: next painting value + continuation + the retained editable source
```

The swept nib follows the choice now recorded on the path ledger. The ledger also keeps a ribbon nameable as another construction. The extension to a physical surface metric is this contribution's position: a UV-sized nib or a projected decal is a different tool, and neither may silently stand in for a nib measured on the surface.

**The seam is not a break in the stroke.** Reducing the first segment's endpoint coordinates modulo `C` turns its intended `+16` mm displacement into `16-C = -109.663706…` mm. That would draw almost around the cylinder in the other direction. Keep the unwrapped source interval and the winding/transition information. Chart fragments created for rendering or querying retain their source segment and parameter interval. Crossing a UV cut creates no authored endpoint, cap or new dab.

In this narrow cylinder patch, the embedding is one-to-one even though the renderer's chart is cut through it. Inverse hits choose the preimage inside the saved domain. For content that wraps more than one full turn, there may be several preimages. The query returns those candidates with their source intervals; the mark's overlap or deposition rule says how they combine. A single modulo coordinate has discarded information that neither the renderer nor the picker can reconstruct.

**Reparameterize without changing the surface.** Let a later evaluation describe the same surface by:

```text
S1(a,b) = S0(2*a + 10, b)
phi(u,v) = ((u-10)/2, v)
S1(phi(u,v)) = S0(u,v)
new period in a = C/2
```

The attachment composes its authored-domain map with `phi`. The path, pressures, source segment IDs and painting values do not change. The new chart has metric `diag(4,1)`: a unit change in `a` is two millimetres along the surface. A physical circle of radius 1 mm has chart radii 0.5 and 1. If a filler treats the new UV coordinates as ordinary millimetres, a point 1.5 mm away along the circumference appears only 0.75 UV units away and is wrongly included.

This gives a precise reparameterization obligation. The new embedding composed with the reported transition must agree with the old embedding on the retained domain, within its declared model error. Normals, footprint and metric transform with the map and orientation. It is not enough for four corners to agree. For nonlinear transitions, transform the map by composition; refitting a finite set of curve controls would be a new approximation of the authored mark.

OCCT's surface interface separately exposes evaluation, partial derivatives, bounds and periodicity. These are useful ingredients for such an adapter, not an attachment preservation rule. [Geom_Surface reference](https://dev.opencascade.org/doc/refman/html/class_geom___surface.html). The equality above and the metric calculation are this contribution's construction, not a claim that that API returns this transition for every edit.

**What actually executes:** inverse surface location maps acquired hits into the authored domain; the path construction builds its region there; the surface binding maps that region onto the current support; the material evaluator samples it with the full projected footprint. A vector drag patches an identified knot or pressure value. It recomputes the affected construction, not the body. A nib change patches the recipe and recomputes the region. The returned region can be reused as a mask or drawing in a planar space without the cylinder.

At the self-crossing, a union mark at opacity 0.5 is painted once. Its geometric query can return both source locations—first segment at 0.5, third at 0.5—while identifying one mark. A later-dab-wins brush has different retained evidence and behavior. Two complete 0.5 source-over deposits have alpha 0.75 on a transparent painting value. That is meaningful accumulation; the same number caused only by duplicating a chart fragment is an error. Choosing which handle a vector editor activates at the crossing belongs to its saved interaction rule.

The painting variant uses the path picture's `sample(surface, ...)` and `paint(surface, ...)` exchange. A concrete pickup variant can be saved as this sequence; all numerical choices belong to this example record:

```text
bindings: path, width(p), initial painting value, initial carry colour
items: path locations at 0.5 mm arc-length spacing in the authored domain
state: painting = initial, carry = initial carry, next item = 0
each location (point, pressure, source segment and parameter):
  picked = sample(painting, point, declared filter)
  carry' = 0.75*carry + 0.25*picked     [linear premultiplied RGBA]
  region = disc(point, width(pressure)/2)
  painting' = paint(painting, region, 0.5*carry', source-over)
  next state = (painting', carry', next item + 1)
return: painting, carry, continuation, source path/recipe references
```

Here `0.5*carry'` scales all four premultiplied channels. The declared filter and the retained medium's local-to-texel map fix the read; the input painting is read before its successor is made. Changing pickup from 0.25 to 0 or changing spacing produces another behavior by editing this record. On this cylinder patch the authored metric is physical millimetres, so the dab spacing and disc construction have no hidden UV conversion.

The surface is retained in its authored coordinates, with its map, colour interpretation and content revision. Reparameterizing the host changes the sampling map, not the stored texels. Texture-atlas repacking can make execution copies; it cannot silently replace the source painting with a resampled version.

For a brush that reads paint, all chart fragments still reach one logical painting state. Passing the UV cut does not reset the carried colour, duplicate a deposition or advance the stroke twice. A vector edit of an earlier source segment invalidates the affected dab sequence and the checkpoints that depend on it; replay produces a new painting result from the retained base and valid continuation. Orbiting or hovering only samples that result. This uses the current path proposal's continuation semantics; this 3D contribution has not implemented the replay on a curved host. If only baked pixels survive, the tool can edit those pixels, but cannot recover the original stroke controls by naming them.

**Where the cylinder stops doing the work for us:** it can be developed into a plane without changing local length. A general curved host needs a chosen surface-distance, tangent-projection or UV footprint construction. For intrinsic ink, the region means `there exists a source parameter t with d_surface(q,c(t)) <= width(t)/2`, subject to the face-domain/continuation rule. The required operation takes `(support/metric, source locations, nib/width construction, boundary continuation, requested error)` and returns a reusable region with membership queries, conservative bounds, chart pieces/source relations and unresolved portions. A metric tensor specifies local lengths; it does not itself compute finite geodesic balls, resolve a cut locus or find all chart crossings. Those are concrete geometry operations still owed. Treating an arbitrary curved face as one ordinary Euclidean planar canvas would hide them.

**4. Extend the box merge: both paint records can survive.** Bench 2's undo-cut path can report “merged from 2”, but its one top-face attachment is not two independently painted supports. Keep its three anchor constructions. Add separate content records `A_left` and `A_right` while the top is split, each with its own source, domain, painting state and attachment rule.

For a simple dimensional version, let the surviving old support domains project to `x in [0,40]` and `[60,100]` mm on a 100 mm wide top. The cut occupies `(40,60)`. Removing the cut gives one top face `F_new` with `x in [0,100]`. The geometry transition can return:

```text
old F_left  -> F_new on x in [0,40], by the retained position map
old F_right -> F_new on x in [60,100], by the retained position map
new support strip x in (40,60): no predecessor in those two old domains

resolved bindings
  A_left  -> F_new, restricted through the left correspondence
  A_right -> F_new, restricted through the right correspondence
```

Neither content record has to win ownership of a new global UV canvas. Both remain independently editable, and both can be sampled on the same geometric face. The attachment construction may keep origin distances, proportions or an edge distance as in the existing box; the returned maps must describe the resulting domains. The simple maps above choose retained physical positions for this example.

What appears in the restored strip depends on **which source was retained**. Two records authored only on the split supports do not invent paint in the strip. A single pre-cut mark retained whole under bench 2's clip rule can reappear there when its original support is restored and the rule reevaluates against it. A deliberately destructively trimmed copy cannot. The new evaluator needs the relevant source/support revision or composed history to distinguish these constructions; knowing only that two current faces became one cannot recover their past.

Overlap is the case that actually needs an ordering or combination rule. Consider two coincident/overlapping supports whose maps into the result overlap at a point. If each carries a half-opacity mark, a retained ordered layer group gives:

| Saved composition at the overlapping point | Premultiplied RGBA over transparent black |
|---|---|
| Blue over red | `(0.25, 0, 0.5, 0.75)` |
| Red over blue | `(0.5, 0, 0.25, 0.75)` |

A lineage value such as `merged` cannot choose between these. Nor can a deterministic sort by face IDs make that choice mean what the tool intended. An explicit example merge recipe can choose `[A_left, A_right]` with source-over, retaining both originals. Another can choose masks, a material blend or a baked result with provenance. Without a supplied rule for a material overlap, resolution returns the competing domains/layers and `needs-policy`. Sid's reserved merge choice stays reserved.

For coatings, compose those contributions in the host face's material evaluation, inheriting the host's visibility. Do not manufacture several almost-coplanar geometry sheets merely to preserve several paint records. Raised ink and floating annotations remain other explicit constructions with their own geometric depth. A hit on the composed coating can identify the top selectable mark under the chosen query rule and also supply contributors; it need not destroy the lower mark's address.

Raster histories add one more requirement. A brush operating *after* the merge must say whether it reads one old painting value, the composed appearance, or a newly baked medium. The merge does not retroactively replay two independent histories in some inferred common order. A saved new brush construction can take the composed result as its base. It retains the chosen input revisions and maps, so the source of its pickup colour is reviewable.

**5. Tool three: a CAD construction that returns a 2D drawing.** Make a 100 by 60 by 80 mm block with a cylindrical through-bore, radius 20 mm, centered at `(50,30)`. Its bore supplies the curved support used above, with a translation from the cylinder's local frame. This extends the constructed-box family while keeping the CAD source independent of its triangles.

```text
saved feature construction K
  dimensions with IDs: width 100, depth 60, height 80, bore-radius 20
  sketch: named rectangular edges; named centre at (width/2, depth/2)
  E: extrude the rectangular region by height
  T: extrude the named circular region through the body
  B: subtract T from E at the construction's modelling tolerance
  D: section B with the named plane z = 40 mm
  return:
    body B; evaluated support domains and correspondence
    drawing D: ordered curve loops, planar frame, source relations
```

This version computes the sketch directly from dimensions; it has no hidden constraint solver. A freely constrained sketch tool would add a real `solve(variables, equations, gauge/branch choice, tolerance) -> solutions, residuals, remaining freedoms/conflicts, status` operation. Calling the direct rectangle “a solver” would conceal that extra work.

For the stated valid dimensions, the section returns an outer rectangle and an inner **circle of radius 20**, in a plane measured in millimetres. A usable circle result retains its analytic geometry and parameterization, orientation as a hole, and its source relation to `B`'s bore wall, the bore construction and the section plane. It is not a ring of display edges. Changing radius 20 to 22 changes the circular loop; the outer rectangle is unchanged. Editing height while the section plane remains in the unchanged prismatic interior leaves this section geometrically unchanged, although the body evaluation may still perform substantial work.

The drawing is reusable in two ways. Another CAD construction consumes its model curves directly—for a further extrusion, measurement or toolpath construction. The path floor receives a lowered path at a declared error, with segment-to-model-curve parameter correspondence, for display, a clip or a mask. An ordinary polynomial cubic representation is not the exact circle; the lowering does not become the measured CAD geometry. A live consumer binds `K/D`; a frozen copy binds a returned value and no longer follows `K`. This is the path picture's producer/output reuse through a spatial producer.

A drawing click returns the drawing curve's identity and parameter, plus the source relation. The radius dimension's saved reaction can set `bore-radius` and update the 3D body. Dragging an arbitrary point of a projection has no general unique inverse: several source edits can produce the same projected motion. The tool must supply a particular constraint/edit construction or make a detached drawing edit. “Provenance” explains where a curve came from; it does not select which cause the user wishes to alter.

This section does not prove arbitrary hidden-line drawing, fillets, shelling, mates or a manufacturing kernel. General hidden-line output additionally needs source-related visibility intervals for the model curves, including tangencies and overlaps; a colour/depth image does not return those intervals. The closed box bench likewise does not become a general Boolean kernel because its output is named as a solid.

There are **three different relations** in this example:

| Relation | Concrete question | Who can supply evidence |
|---|---|---|
| Operation provenance | Which current sketch edge or cutting face produced this current body face? | The operation and its adapter |
| Correspondence between evaluations | Which old output portion continues as which new output portion after radius changes? | The construction using old/new results, stable source roles and operation evidence |
| Attachment preservation | Where should the old ink lie on the new bore, and what should its width preserve? | The saved attachment rule consuming correspondence |

Kernel history is useful for the first relation; it is not automatically the other two. OCCT's `BRepTools_History` documents generated/modified/removed relations between input and output shapes, including modifications of bounds and parameterization. Its published exchange returns shape relations, not an arbitrary old-to-new UV transport of paint. **Inference from that API boundary:** a Softland adapter/construction still owes the coordinate relation and the continuity between separate feature-history evaluations; attaching “OCAF” to that arrow has not supplied them. [BRepTools_History reference](https://dev.opencascade.org/doc/refman/html/class_b_rep_tools___history.html).

Changing the bore's radius is a geometric change, unlike the earlier reparameterization. A rule that keeps material proportions can stretch the ink circumferentially; a rule that keeps millimetres from a specified generator can keep its width and occupy a different angular span. Both can use the same named bore wall. This is the curved version of the box's three anchors, not a reason to reopen or erase them.

**6. The exact exchange exposed by these constructions.** Evaluation needs to return more than `part-id + lineage word + frame`, but it does not need to decide every attachment's fate. Here is the proposed addition to the composer's exchange:

```text
evaluate(construction revision, named input revisions,
         previous evaluation when comparison is requested, demand)
  -> typed named outputs
     dependencies and changed values/parts/ranges
     operation provenance
     correspondence between explicitly named evaluations
     retained result state and status

correspondence piece
  from: evaluation, support/domain ID, valid subset
  to:   evaluation, support/domain ID, valid subset
  relation: evaluator or geometry describing corresponding locations
  forward/backward query capabilities: unique | candidates | unsupported
  orientation, coordinate/quantity context
  meaning/evidence: same points | restricted old support |
                    declared deformation/material transport |
                    approximate projection | unknown
  achieved model error where applicable

report also
  removed old domains, new domains without predecessors,
  unresolved/ambiguous domains and the limits of coverage

resolve-attachment(authored content/domain, anchor + preservation rule,
                   correspondence + old/new support values)
  -> bindings from authored content to current support domains
     clips, layer relationships, resolved/unresolved portions
     explicit detached results when the rule asks for them
     dependencies and changes to those bindings
```

The relation may be partial and may have several preimages. Forward mapping alone is insufficient for querying the mark from the visible support. Backward mapping may return multiple locations or require a separate bounded inverse operation. A queryable version takes `(relation revision, direction, location or requested subdomain, error/work demand)` and returns corresponding locations/domain pieces, explicit unmapped or unresolved portions, and achieved error. A map with an `unknown` portion must say where that portion is; a global success flag plus silently missing ink is not the proposed answer.

For the cylinder's pure reparameterization, the evidence is the equality `S1(phi(q)) = S0(q)`, and the map is invertible on the chosen authored patch. For the box split, it is a restriction onto surviving support subsets. For the merge, several restrictions lead into one result domain. For a fillet, newly created geometry may have feature provenance without any inherited paint coordinates. For an implicit field edit, there may be no material transport at all until an anchor construction supplies one. These are answers with different strengths.

Approximate nearest-surface projection can be a named fallback requested by an attachment. Its request needs the old locations, allowed target set, maximum distance, side/normal or other selection constraints, tolerance and tie behavior. Its result needs candidates, achieved displacement/error and unresolved cases. Being able to compute the nearest point does not prove that this is the same material location. The proposal therefore does not silently use nearest geometry when a correspondence is absent.

The authored attachment should retain the support revision/domain against which it was defined. Later transitions can be composed through intermediate evaluations or resolved directly against that reference. Removing intermediate caches is safe only if the retained source, composed relation or explicit detached result is sufficient for subsequent queries and edits. A pointer to a dead kernel handle is not persistent correspondence.

Stable generated part names require similar care. A construction can name a rectangular extrusion's top by its stable source role. A split creates several descendants; a sort order cannot by itself establish their continuity on a later rerun. Allocate/reuse derived identities where the relation establishes continuity, preserve predecessor sets, and return ambiguity where it does not. The store still sees fine-grained authored changes even when a Boolean forces a whole body to be reevaluated. A global topology change is an honest possible result, not a reason to change every occurrence's identity.

**7. The hardest case through this proposal.** Put the bored block in occurrence `O` in spatial space `S`. Its inner cylindrical face carries authored domain `A` and the pressure mark `k`. The page `P` contains portal `R` showing view `V` of `S`. Put the field object `Q` in `S` as the moving occluder. Another host surface carries portal `T` into land `L`. Put screenshot `I` and the section drawing `D` beside `R`.

```text
picture of the mark:
  k in A -> support binding -> O in S -> V -> R in P -> page view

query of the mark:
  page point -> R-local -> V ray -> nearest visible support in S
  -> support-domain preimages -> A -> k + source-location candidates

query through T, when T's host surface is visible:
  ... -> T-local -> view in L -> L's own answer

query of D:
  page point -> drawing frame -> model/lowered curve correspondence
  -> source relation back to K/B

query of I:
  page point -> image coordinates
```

1. **Draw.** Each acquired sample carries the observed view/evaluation context. The walker returns a bore hit in `O`; the inverse attachment maps it into `A`, preserving seam continuation. The gesture retains pressure and order. The source construction creates or updates identified path elements. A drag captures its target and coordinate rule; leaving the support invokes its declared clip/continue/detach behavior, not an automatic jump to an object behind it.

2. **Paint.** The vector reading resolves the swept region once into its coating or annotation. The accumulating/reading brush instead advances its explicit painting state in dab order. Both retain the editable source specified by their tool. The host map determines where they are sampled, and the host surface supplies depth and orientation.

3. **Vector-edit.** Move knot B or change the pressure response. Geometry and, if used, dependent paint replay change. The bore, the other occurrence's geometry and the screenshot do not acquire authored edits. A changed painting result is published as a new value; prior readable results remain valid while referenced, as required by the path proposal.

4. **Zoom the page.** The complete projection requests more or fewer samples for `R` and its nested content. The viewport/framing belongs to the portal/view declaration; allocation rounding must not alter it. The path width in authored millimetres stays the same. A logical painting value retains its authored resolution unless a separate refinement/reconstruction is requested; magnifying it does not create vector detail absent from its source.

5. **Orbit.** Change `V`, recompute view-dependent visibility, footprint and the ray. The local path and the cylinder attachment do not move relative to each other. This is a viewing operation in this example. Editing a saved camera or an animated camera would be a different explicit authored operation.

6. **Hover the crossing and click.** The opaque visible query first resolves the support, then the mark. It can return `[P, R, S, O, bore-wall, A, k]`, the two source segment/parameter locations, and the coherent revisions. A saved selection rule chooses the mark or a handle; the click then patches its named target. A geometric query and an enlarged editor handle query may deliberately answer differently from visible coating coverage, and state their modes.

7. **Move Q under the still pointer.** Its field boundary event becomes nearer than the bore. The hover result leaves `k` and addresses `Q/F` with a local field point and expression references, without a face. The active query must depend on changes capable of entering its ray/aperture, not only on its previous winner. Old/new changed bounds and a spatial index can narrow the reevaluation; when they cannot, use a broader invalidation. Camera, clipping and visibility-policy changes also invalidate it. Equal public answers need no entered/left change merely because an internal cache stamp changed.

8. **Reparameterize, split or merge the support.** A pure chart change preserves the mark through the composed map. A split clips or detaches according to the saved rule, keeping authored source intervals distinct from display fragments. A merge retains both content records; any overlapping material domains invoke their saved composition rule or report the missing policy. Publish geometry, correspondence, bindings and query state as one coherent evaluated result. An asynchronous result from the previous geometry cannot be presented as a current hit by attaching a new revision number to it.

9. **Enter the other land.** First establish that `T`'s host surface is visible in `S`; a nearer `Q` can block it. Map that host point to the declared target view in `L` and prefix the target answer with the portal route. `L` may be planar and contain another spatial view. Target depth is used inside its target view; it is not numerically compared with host depth. A saved navigation reaction may re-root controls into `L`. A picture portal alone does not provide continuous geometric travel, shared light transport or a meaningful inverse at a singular map. Recursive view/query expansion needs a finite execution policy and an explicit result at the bound.

10. **Inspect the screenshot.** `I` holds captured pixels, their interpretation and optional provenance. It can match the live picture at capture time and size, but its ordinary query ends at the image. Orbit, geometry changes and the stationary occluder do not change those bytes. Cropping/painting the image operates on that image. A queryable frozen scene is a separately retained artifact with more than these pixels.

11. **Take a 2D result back out.** The section returns model curves and source relations; `D` displays their path lowering. Its radius dimension can edit the same CAD parameter that shaped the bore. The annotation tool can also return `k` in its original planar domain, reusable as a mask or drawing. Unwrapping this cylinder patch preserves length; flattening an arbitrary curved host may distort and must return the chosen map/error, not label a projection as an isometry.

This walk specifies a possible end-to-end execution. It does not claim pointer-rate performance, a completed field renderer, a geodesic brush implementation or a complete CAD kernel. Its usefulness is that each missing step now has an input, result and an observable consequence in the same case.

**8. What new code is actually justified below the waist.** This is a capability list for the proposed floor, not a plan to add eight permanent special cases. A saved algorithm is still code that must execute; a compiled library is one way to supply that execution. The distinction that matters is whether the tool can change the algorithm by editing data through an existing capability, or must request a new engine entry.

| Capability and concrete input → output | What can already be reused | What remains missing at the checked boundary |
|---|---|---|
| Execute a saved operation: program/operation reference, typed bindings, state and numeric/capability versions → typed values, next state, dependencies and changes | The path ledger's chosen executor; its proposed bindings, sequencing, returned values and continuation semantics | Client scene evaluation accepts a fixed region model; it does not expose this construction input. Host execution/compilation and lifetime ownership must be real. The 3D capability entries and final data schema are still to define. |
| Spatial participation: evaluated source/bounds plus query/render procedures and demands → query events or consumer representations, with source locations and status | Camera rays, transforms, spatial indexes, mesh rasterization and material stages as implementations for their supported inputs | Closed mesh/primitive admission and mesh-only `pick-region` do not admit the field procedure. Image output without boundary/query participation does not supply it. |
| Polynomial field query: coefficients, ray interval, tolerance/work budget → isolated roots, covered-empty and unresolved intervals | Arithmetic can be a common numerical substrate; the field expression and Boolean event composition remain data | No such operation is supplied by the checked primitive-mesh path. Robust roots, tangencies and bounded error are algorithmic work, not additional shape names. Direct imaging still needs a suitable execution tier. |
| Surface location and footprint: support evaluator, trim/period/transition data, authored-domain binding, point or ray, tolerance → locations/candidates, derivatives, metric, orientation, valid domains and error | The planar inverse is a useful affine special case; path classification and coverage remain consumers | `ray->placement-plane` intersects local z=0. It has no periodic domain, curved inverse or transition exchange. General surface-distance footprints need further finite-neighborhood geometry; a derivative alone is insufficient. |
| Resolve support change: old/new support values and evidenced domain relations, content and preservation rule → current bindings/clips, unresolved portions and composition requirements | Bench 2 demonstrates three planar anchors and split clipping/detachment; path arrangements can handle planar domain clipping | Its frame/part-count result does not supply a curved transition or two independently painted domains. Correspondence production, inversion and composition need implementations. Layer retention/selection policy remains data. |
| Sample/paint attached content: logical painting/region, domain map, footprint, clips, order and material role → material contribution or next retained painting value | The path proposal's filler, sample/paint operations, layers and continuations | The client compositor owns targets/presentation; placed ink is a separate packed plane lane. A surface-binding material consumer and correctly ordered brush execution are still needed. UV repacking is not source painting. |
| Solid construction and derived drawing: tolerant sketches/solids, operation and model tolerance → valid geometry or failure, operand history and reusable section curves with correspondence | Common value/dependency handling; the path floor can consume explicit display lowering | The region's primitive meshes and BVH provide no tolerant B-rep Boolean/section operation. A kernel plus an adapter can supply these algorithms; it does not automatically supply cross-evaluation identity or attachment transport. |
| Observe a query coherently: query/aperture, evaluated space/views, change sets and prior answer → current answer and semantic diff, or pending/unsupported state | Existing camera and mesh-query calculations; the composer's bench demonstrates still-pointer invalidation | The current picker is a calculation, not the whole-page reactive observation service. Candidate invalidation, nested routing, mixed representation visibility and asynchronous consistency remain work. |

These distinctions keep the waist claim testable. A new polynomial field formula should use the same numerical and spatial doors. A new attachment anchor should be a saved construction over the same surface and relation values. A new blend of existing paint layers should be data over the same compositor. In contrast, a host unable to execute field queries or surface maps really does need an extension; changing its object record alone cannot give it that behavior.

The numerical/CAD libraries may be large. “A few primitives” can be a small *calling surface* over substantial reusable machinery; these examples do not show that a tiny fixed collection of elementary geometry algorithms reaches every ceiling. They do show which algorithms are shared services and which choices belong in the editable constructions.

**9. Changes have several grains.** The observable result should name its actual dependencies and affected scope, without promising that a small authored patch always causes little computation.

| Input change | Authored patch | Evaluated effects owed by this proposal |
|---|---|---|
| Field extent | One parameter ID | Field bounds/evaluator bindings, affected visibility/query work; field identity stays |
| One knot/pressure | Named curve/source element | Dependent region and brush suffix, affected painting/binding consumers; no body rebuild required by meaning |
| Pure support reparameterization | Support evaluator revision if authored; otherwise a derived representation revision | Transition/binding and consumer preparation; authored path and painting bytes stay |
| Boolean or bore-radius edit | One feature/parameter | Potentially broad body evaluation; honest part/domain correspondence; affected attachments and drawing outputs |
| Layer order on merged support | Named composition relation | Material/selection results; geometric topology and old paint histories stay |
| View or portal placement | Session view or the explicitly edited placement | Projection, visibility, footprint, routing and relevant hover; no automatic source-geometry edits |
| Moving occluder | Occurrence transform | Spatial index/visibility dependencies and active queries that it can affect |

A topology result may need several related changes published coherently. That is not a mandate to merge the whole scene as one authored value. Conversely, serializing one patch per voxel or per GPU element would not make useful identity finer. Stable chunks/ranges can carry bulk value changes; addressable authored features and operations keep their own identities. Derived representations remain available as reusable values when deliberately retained, while allocation handles and frame caches stay with execution owners.

**10. Where I take a position, and where it becomes a default.**

| Position | What supports it | Default/exit and what else changes |
|---|---|---|
| Preserve authored domains independently of current surface charts | The explicit cylinder transition preserves the embedding while changing coordinate scale and seam location | A single canonical UV canvas is convenient for an application that deliberately makes UV layout the authored object. Reparameterization then needs an explicit content transformation/resampling operation and an associated loss policy. |
| Let one support carry several independent content bindings | The disjoint merge retains both records with no conflict; the overlap calculation isolates the extra composition input | Baking to one medium can be chosen for later painting or resource limits. It creates a new value; map, resolution, provenance and any lost editability must be accounted for. It cannot masquerade as identity-preserving transport. |
| Return query locations without requiring a named face | The implicit ray has definite roots and a definite addressed definition but no modelling face decomposition | A reconstruction can deliberately create named surface patches. Their generation and continuity then become additional derived data; they are not native faces recovered from the field. |
| Use evidenced, partial domain relations between geometry evaluation and attachment resolution | Same-surface reparameterization, split restriction, merge and new fillet material have different relations | A narrow kernel can return complete maps for its supported operations. That is stronger evidence, not a reason to force arbitrary operations to guess. Approximate transport is an explicit requested construction. |
| Keep model geometry distinct from its 2D display lowering and from inverse editing | The section's analytic circle supports measurement and reuse; its display controls do not uniquely identify a CAD edit | A detached illustration may choose polynomial paths as its new authored truth. It then gives up automatic CAD correspondence/precision except what the conversion deliberately retains. |
| Share execution, identities, revisions and operation exchange across 2D/3D while allowing several implementations | The same path result becomes a material, a mask and a drawing; the same data dependency model reaches fields and solids | Different compute tiers can use compiled kernels or remote results. They still need explicit type, revision, unit and error compatibility. No frame-rate or cross-machine determinism claim follows from sharing syntax. |

The physical-width interpretation, ordered layer example and bounded polynomial route are **defaults chosen for these constructions**. The swept nib and records executor follow Sid's choices as recorded in the updated path ledger; this contribution does not settle the compositor, the merge policy or any wider executable-language extension. The invariant in the pure reparameterization case is stronger: if the operation claims that only coordinates changed and the attachment preserves the same physical content, changing the resulting ink would contradict those declared inputs. A geometric deformation makes a different claim and needs its preservation rule.

The ceilings remain ceilings. Parametric CAD still needs robust model operations and continuity under hard topology changes. Film adds deformation, time and simulation state to the support relation. Open worlds add resident working sets and latency bounds to queries and imaging. BIM adds authored dependency and edit policies above these geometric exchanges. Geospatial use adds non-affine frames, precision and residency. Fields/scans add sampling, interpretation and possibly contributions along a ray instead of boundary events. Nothing here treats the polynomial body or the developable cylinder as proof of those harder cases.

**11. The workable proposal and the remaining input/output decision.** Keep the three shared requests. Their division becomes:

```text
saved tools/constructions
  -> execute over typed values and explicit state
  -> named spatial results + provenance/correspondence + changes
  -> saved attachment/material/drawing constructions
  -> evaluated occurrences with resolved support bindings
  -> render(view, demand) and query(route, mode, demand)
  -> images and source-related answers
  -> saved tool reactions propose authored patches
```

The concrete proposal is to let the geometry result expose a **versioned, possibly partial relation of source domains**, including multiple preimages, orientation, evidence/error and uncovered regions. A separately saved attachment construction chooses what to preserve and returns bindings rather than necessarily rewriting the source mark. A merge can return two bindings to one support. A field can return a location without a face. A drawing can return model curves plus provenance without pretending to supply a unique inverse edit.

The exact open input/output decision is **what evaluation must return before an attachment may continue**: a materialized mapping for every domain it preserves, or a revision-bound correspondence value that answers demanded forward/inverse locations and can explicitly leave portions unresolved. I recommend the latter, with explicit maps for simple cases such as this cylinder and box. The mandatory result would identify the compared evaluations, the covered domains, the query capabilities and the known failures; further correspondence queries would take their own error/work demand. Merely returning two shapes and the word `modified` would not meet it.

That choice lets a CAD edit produce usable geometry while expensive correspondence is resolved on demand. It also makes pending/partial attachment state a real output that both presentation and interaction must honor: unresolved coating cannot quietly disappear, and an unknown nearer intersection cannot justify a definite visible winner behind it. A simpler implementation can instead complete the required mappings before publishing the new attached result, paying that latency up front. These are workable alternatives about completion and demand, with different scheduling/cache consequences. Neither moves preservation or layer-order policy out of its saved construction.

This sharpens the predecessor's correspondence question; it does not close it for Sid. It also remains a real decision within the records executor now chosen on the path ledger. The next discriminating implementation receipt would load the same authored mark, change only the cylinder parameterization, and then merge two separately retained paint records: unchanged source IDs/bytes, matching physical locations, an honest overlapping-layer result, and the same source address reached from a fresh query. Such a receipt would test the proposed exchange directly. Until then, “domain relation,” “inverse surface location,” “geodesic footprint,” “kernel adapter” and “spatial program participation” name work with specified inputs and outputs, not completed machinery.

**Evidence and source custody.** The own source read followed the client/folder maps and namespace/function documentation. The relevant checked boundaries are [closed mesh admission](../../../src/app/client/region3d/component.cljc#L386), [object-to-mesh](../../../src/app/client/region3d/scene.cljc#L449), [mesh-only picking](../../../src/app/client/region3d/scene.cljc#L965), [plane inverse and fixed-zoom packing](../../../src/app/client/region3d/on_plane.cljc#L72), [flat placed-ink rendering](../../../src/app/client/region3d/on_plane_renderer.cljs#L26), [scene evaluation](../../../src/app/client/region3d/scene.cljc#L1088), and [compositor ownership](../../../src/app/client/engine/compositor.cljs#L1). These show what those paths take and return; they do not establish latency, numerical behavior of a new kernel or a product interaction route. No server source was opened.

An own read of [bench 2](bench-2/seam-bench.html)'s `evaluate`, `resolveAttachment`, `attachUV` and `attachPoint` found the reported box frames/part counts, the three scale/offset rules and clip/detach handling. Its `merged from` line is generated when the top's part count decreases. This bounds the receipt used here: it is not a measurement of merging two distinct painting states or of curved parameter transport. The inherited browser measurements remain attributed to [fact-base-2 Part 11](fact-base-2.md), not to this contribution.

The new arithmetic check evaluated the stated equations in Python, using no client or bench implementation:

| Check | Observed result | What it supports, and its limit |
|---|---|---|
| Implicit axis ray, `a=2` then `a=1` | Roots `[1,5]` then `[2,4]`; substitution residuals zero | The example's events and source distinction; not a general root-isolation implementation |
| Four cylinder knots through `S0` and `S1 composed with phi` | Maximum coordinate difference `0.0` mm in this calculation | These points agree; the algebraic composition gives the whole-domain equality |
| First segment after naive modulo | `-109.66370614359172` mm instead of `+16` mm | A concrete information loss at the UV cut |
| Pressure at the two crossing locations | Widths `1.75` and `2.4699999999999998` mm | The saved response at the stated source parameters; not pixel coverage |
| New chart metric | `diag(4,1)`; physical distance `1.5` versus naive UV distance `0.75` | Why preserving only coordinates without metric/footprint changes the nib |
| Half-opacity layer orders | Blue/red `(0.25,0,0.5,0.75)`; red/blue `(0.5,0,0.25,0.75)` | An overlap needs a composition input; not a browser render receipt |

HEAD observed during the source read was `b3f1b978f78558f36e3235f20f629ae6d62d95ad`; `src/app/client/` had no working-tree changes. At final verification the four composer inputs still matched these SHA-256 prefixes: `3d-kind-2.md ec2e7b1f5ba66bff`, `HANDOFF-2.md 379148aff417add9`, `fact-base-2.md c5644a8b7f7896c3`, and `bench-2/seam-bench.html f9b5f875afe1757b`. The path picture changed from `1a6b16e7657f1e28` to `5ff1b6d16df9f278`; the own read of its diff found the newly recorded nib/executor rulings, carried into this contribution above. These identify the input artifacts, not a source freeze. Only this new contribution file is owned by this chair.
