# The line after attack 4: move into the client

2026-09-06. Definer's contribution after the session 12 fold, at `05efc3a` (`a6d7190` bench, `b3f1b97` page). This answers Sid's request to draw the boundary here after attack 4. It is an implementation direction and proposed first change, with acceptance remaining Sid's.

**My position: the picture is sufficiently defined to begin client implementation. End the open-ended bench build/attack cycle here.** The next substantive deliverable should change `src/app/client/`. Further constructions should normally exercise that implementation and improve it. A new architecture question needs a concrete example that changes the exchanged values or responsibilities.

The six pieces are a sufficient working map for the constructions we have run. This does not turn their count into a completeness theorem or settle Positions 7, 8 and 9 on Sid's behalf. Those positions retain their current ownership. Explicitly naming a tip in a fixture, or implementing the bench's current record vocabulary, need not declare a universal default or close the alternative language road.

## What has crossed the line

A person can supply source and behavior as records, execute a construction, receive a path or surface, and make another construction consume that result. The path keeps geometric meaning and source correspondence; a painted surface keeps the identity of its contents; a continuation captures the carried state and reachable resources needed to resume. The packer owns the view-dependent approximation; the filler owns coverage; the compositor owns painting, sampling and physical surfaces. The executor orders calls and retains results that later calls can reach.

The attacks have progressively exercised these responsibilities: three authoring tools, a self-crossing pressure stroke in union and accumulation, a brush that reads its paint, a network that discovers faces, branched paint results, group masks, paths reused as masks, nested results and serialized continuations. Attack 2 added responsibilities to the picture. Attacks 3 and 4 made those responsibilities concrete without requiring another rendering definition.

The newly committed Node route was run once for this boundary decision. Extraction, `harness.js`, `regress.js` and `wire.js` all exited 0. The nested proof retained red `(0.5,0,0,0.5)` with zero releases. The pickup ran 24 dabs / 72 steps. Its twelve-dab checkpoint restored and resumed with 0 of 65,536 components differing; the content hash was `9757e826f4594d5aa2c628302831ddee00dc57c8037340ee5bc14428d395aa82`. Prefix-pressure, pickup-setting and capability edits refused; the stated last-sample edit loaded. These are fresh Node/CPU observations. The GPU fold evidence remains the composer's scoped SwiftShader receipts in [fact base 12](fact-base-12.md) and [the handover](bench-9/HANDOVER.md), not a new hardware or multiplayer claim.

The page's cascade identifies the old code and its replacement responsibilities. It leaves several destination namespaces unnamed. Choosing those homes and wiring their callers is now ordinary implementation design; another bench attack will not choose a Clojure namespace for us.

## The first client change I propose

**Replace the existing path triangle-rendering route with path values feeding shared curve coverage, including its existing on-plane consumer.** Bring the draw, pen and border records through that route, with their behavior supplied as data. The self-crossing pressure fixture must support explicitly named union and ordered accumulation. The first change should leave an actual replacement reachable from the current client harness.

The source-facing side is a record construction over capabilities. Source conversion and stroke policy stay separately expressible from the resulting path. A pen's anchors, sampled drawing input and rectangle parameters can therefore produce the same path value without becoming three engine material kinds. A new preset or recipe changes data; its display name never selects native rendering code.

The value-facing side consumes directed line/quad/cubic subpaths, knot/source correspondence, explicit paint and identity. Geometry returns regions and spatial answers from that value. Both stroke constructions use the same filler: union paints its region once; accumulation supplies ordered footprint regions. A path returned by geometry can also feed a literal clip. Surface-reading brushes and the arrangement executor become subsequent client capabilities on these interfaces; their full implementations need not accompany the first drawing replacement.

These are proposed code homes, not an inventory of code already present:

| Home | Responsibility and change |
|---|---|
| `path/component.cljc` | Replace `:ink`/`:shape` as the rendered value grammar with the common path/paint/identity value. Keep validation through `engine/schema`; keep authored source records upstream. |
| New `engine/executor.cljc` | The construction runner and capability bindings, independent of tool names. Land the operations the first records actually use; unavailable capabilities report that fact. The record vocabulary is the current implementation choice, while its capability interfaces remain usable by another runtime. |
| New `path/geometry.cljc` | Source-to-path operations, curve/tip construction and answers. Tool recipes supply the input and policy; geometry supplies the reusable computation. CPU membership reads the same constructed region that drawing consumes. |
| New `path/pack.cljc` | Camera/placement tolerance, cubic-to-quad lowering, bands and cover data. This output and its cache are derived rendering resources. |
| New `engine/coverage.cljs` | Extract the reusable curve-coverage implementation from `text/renderer.cljs`, with WebGPU resource/draw support. Apply the fragment-position correction in the WebGPU coordinate contract. Text and paths call this shared definition; they may retain separate source resources. |
| `path/renderer.cljs` and `path/frame.cljc` | Replace the flat triangle program, seven-word vertex lane and three-band LOD dependency with region preparation, packing and draws. Separate source/geometry validity from pack/placement validity. |
| `text/renderer.cljs`, `text/glyph_pack.cljs` | Move coverage to the shared home while keeping text positioning and glyph packing with text. This change does not require a new text layout architecture. |
| `region3d/on_plane.cljc`, `region3d/on_plane_renderer.cljs`, thin callers in `region3d/renderer.cljs` | Move the existing placed-ink reader from path meshes to the same path/coverage road under its projection. Keep scene semantics, depth order and placement ownership with region3d. This is the caller migration required by removing the old path mesh route. |
| `harness/path.cljs`, `harness/region.cljs`, relevant text checks | Drive the new contracts through the existing entrypoints and migrate the old source/mesh assumptions. Retain useful geometric, color, transform and lifetime evidence. |

`engine/compositor.cljs` retains target-pool and presentation ownership. Subsequent work adds the proven surface operations, isolation, feedback and checkpoint restoration there or in focused supporting namespaces; the path renderer should not acquire a private competing surface owner. A full compositor port is a separate implementation responsibility from replacing plain path drawing.

For the first drawing implementation, the existing envelope construction is a concrete starting algorithm under its stated tolerance. The offset stroker remains a required geometry improvement; the bench's larger curve count cannot be represented as the final memory or speed result. The choice of algorithm must preserve the named tip's meaning and error contract. A change of approximation must not silently change what varying width means.

## Exactly what old code this replaces

The checked 2D route is `harness.core/run-harness! → harness.path/run-path-step! → path.renderer/prepare-path-frame! → path.tessellation/derive-mesh-set`, followed by `draw-path-range!`. The old derivation expands ink into triangle pieces and bridges/ear-clips filled contours; the renderer packs their color and group index into each vertex.

There is also a real consumer at `region3d.on-plane/pack-placed-ink → path.tessellation/derive-mesh-set`. Its output becomes a placement vertex buffer through `on-plane-renderer/prepare-placements!` and is drawn by `draw-placements!` from the region renderer. The region harness constructs these placements. Removing only the 2D caller would leave this mesh road in use.

The proposed first change therefore replaces those drawing consumers together. The old `path/tessellation.cljc` ink fans, hole bridges, ear clipping, mesh cache and zoom table can then leave this client route. The old path triangle shaders and vertex packing leave too. The value schema, frame dependencies and CPU query entrypoints are rewritten around the common geometry; deleting their entire files is not itself the objective. General shared engine, font, text-layout and scene code retains its own responsibilities.

The projected reader is included because source reading found it, not because the path chair is reopening the 3D kind's design. Its matrix/coverage adaptation and its current placed-ink example belong in the implementation's evidence. Coordinate adaptation under projection may be substantial code; it has a known input, output and owner.

## Evidence the client replacement should deliver

Five scenarios are enough to define the first implementation's obligations; more cases are added when a new failure or claim earns them.

1. **Three records, shared path route.** Pen anchors, sampled drawing and a border produce path values through declared constructions. Change geometry or behavior in each record and observe the corresponding output change. Tool names have no rendering dispatch. Merely reporting every field read is insufficient.
2. **Pressure and crossing.** The known Z source, spacing 12 and deposition opacity `.62`, yields the established union/24-dab outcomes through the actual WebGPU path. Its crossing gives `.62` for union and `.8556` for two fully covering deposits. Name the tip explicitly. Include a nonlinear pressure response at a point where interpolating widths would give the wrong footprint.
3. **One region meaning across readers.** A curved contour with a hole and a returned path used as a clip produce the declared membership and coverage, with an independently known interior/exterior and a chosen edge probe. Text exercises the extracted filler too; preserving a shader string alone is insufficient.
4. **Edits and presentation have different dependencies.** A geometry edit changes geometry; paint changes do not reconstruct the source; pan, scale, DPR and changed group placement update the appropriate pack/instance data. A pure camera change retains the camera-free path. Measure the taken calls and output, including a fractional placement that would expose the old varying-position error.
5. **The existing projected caller still works.** The placed-ink fixture uses the replacement under its actual plane projection and existing scene ordering. Its color/coverage remain readable, resource cleanup works, and no client drawing caller retains the deleted tessellation road.

These are proposed close scenarios for the client work, not claims that it has already run. The existing harness is a browser rendering entry, not an interactive product editing loop. Native client drawing is the first deliverable; pointer acquisition, collaborative document storage and the complete tool UI retain their own work.

## What stays on the implementation list

| Remaining work | Known home or decision |
|---|---|
| Tangencies, partial overlaps, tangent-order ties, general arrangement holes | Geometry's contacts/spans/arrangement contract. Implement and test them when bringing the network capability across. No extra broad architecture round is required beforehand. |
| Offset stroker, degeneracies, caps, wet-tail updates | Geometry/source algorithms and their approximation contracts. |
| GPU brush carry, economical isolated targets, backdrop regions | Executor scheduling and compositor resource execution. Hardware throughput needs measurements after a native path exists. |
| References beyond a pasted record and content beyond page memory | Caller-supplied record/content resolution with captured dependencies. A document or store supplies values through those interfaces; its storage schema does not belong in path geometry. |
| Byte checkpoints and nested resource retention in the client | Port the established state/resource contracts and codec alongside retained surfaces. Replace bench shortcuts such as the depth-eight reachability walk and plural-minus-`s` item binding with explicit implementation contracts. |
| Numeric truth when several clients paint a surface that later brushes read | Sid's open choice. Required before promising that multiplayer behavior; it does not prevent the first local path/coverage replacement. |

I would reopen the architectural picture for a construction whose necessary input, result or state cannot be carried by these interfaces, or for evidence that the proposed ownership makes a required dependency impossible. An algorithm failing at a tangent, a packer spending too many curves, or a missing namespace is work to do within the picture.

**The next chair should carry the session 12 picture into the client and use these constructions against the code it lands.** The bench remains a reference. Updating it can serve a concrete implementation question; continuing to expand it is no longer the default next task.
