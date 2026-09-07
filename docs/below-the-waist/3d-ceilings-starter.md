Preflight: permission mode and toggles set before this first prompt, nothing mutates mid-session. Fable, effort max for the thinking; the first turn loads the two files below at low effort and ends in one line.

ok so this is the current waist (trust me don't go rechecking just to get to it again)

   ABOVE THE WAIST   data · ECS · humans + agents, no merge deadlocks
   ┌──────────────────────────────────────────────────────────────────────┐
   │                              (empty)                                 │
   │   today only harness/ stands here, building draw items by hand       │
   │   core (the one src-side compiled entry; a second test-side build    │
   │   exists for the shaper-border probe) · path · text · image · region │
   │   · shared (the live readback, zoom cases) · region_oracle (CPU      │
   │   oracle for scene + shading, JVM-tested)                            │
   └────────────────────────────────┬─────────────────────────────────────┘
                                    │  draw items in, pixels out
   ═════════════════════ THE WAIST = what the engine accepts ══════════════
     draw item    the row inline under a kind slot + :container (group)
                  no draw-item schema: the row inside the slot is checked
                  by its kind, :container by transform; nothing else is
                  :path/material (+ :id, unread) · :image/component ·
                  :region/material (+ :region3d/resolved-placements beside it)
     path row     material-id · revision · kind ink|shape · geometry · paint
     image row    component-id · revision · source digest · color tag ·
                  intrinsic size · provenance · rect · paint      (crop optional)
     region row   id · revision · version · extent · scene · view ·
                  background · ambient · rect
     text         NO ROW — two draw-item forms, both unchecked:
                  bare      :text · :x :y · :size (optional, else the call's
                            font-size) · :r :g :b :a · :container
                            → renderer shapes + lays out inline (a fallback)
                  pre-laid  bare + :layout-result · :layout-line-id ·
                            :layout-anchor · :paint-source-range
                            → renderer positions only
                  face arrives beside it as font-assets, a per-call argument
                  identity: :source-id / :source-revision pass through
                  unvalidated; layout-key's address+stamp has no caller in src
     group        parent + transform (affine XOR x/y/scale/scale-x/scale-y/
                  rotation; every key optional) + camera world|screen;
                  group 0 is the reserved root
     per-frame    not one waist — four:
                  all kinds   camera pan·zoom·size → device buffer (harness
                              writes it) · world-transforms → each prepare
                  path        zoom → lod
                  image       residency counter, internal, bumped by a set call
                  region      zoom · dpr · session-revision (· font-assets, unused)
                  text        font-size · line-height · snap-step · attachment-size
   ════════════════════════════════════════════════════════════════════════
   BELOW THE WAIST   compiled, agreed, changes with receipts
   ┌─ engine/ ────────────────────────────────────────────────────────────┐
   │ transform   group tree → affine + buffer index + screen flag per group│
   │ device      camera buffer · groups buffer · scene color mode (a WGSL │
   │             const flipped by string replace, default off)            │
   │ compositor  offscreen targets · leases · one present                 │
   │             (readback fns private + uncalled; live one is harness/)  │
   │ rungs       region rungs [1 2 4 8]         leases   region leases    │
   │ buffer_pool · schema (declared-map checker) · color · limits         │
   └──────▲────────────────▲────────────────────▲──────────────▲──────────┘
     path/  mesh cache keys on geometry+algorithm+lod, not id/revision
     region3d/  on_plane depends on path AND text; renderer requires it
                but never calls it; on_plane_renderer draws ink only

Fact base with every claim anchored to file:line, read at HEAD 698c69b: docs/below-the-waist/ceiling-and-waist-factbase.md (24KB). Consult it only to check a structural claim, never to re-derive the waist.

Notice one thing in the diagram before the list. The waist already holds one 3D kind: the region row is a portal with a scene, a view, a background and an ambient inside it, and on_plane draws path and text on a 3D plane. 3D does not start from zero here.

This is the state of the 2D round, the same question already asked for 2D: docs/below-the-waist/waist-argument.md (30KB). It is a map of that debate in pictures: the waist today, the layer that was proposed below it, where the adversarial pass found that layer cracking, and what is still open. Read it as open state, not as the answer. Do not reopen what it settles and do not assume what it leaves open. Whatever you answer for 3D has to compose with it: one waist for 2D and 3D, or a reason there are two.

Is it enough to do the hardest 3D things?

The hardest 3D things are again stacks of derivations, and each one has a 2D ceiling inside it:

Parametric mechanical CAD (SolidWorks, Onshape, Fusion): exact solids whose faces are planes, cylinders and NURBS; a feature history (sketch, extrude, fillet, shell, pattern) that re-runs from the changed feature down on one dimension edit; booleans and fillets as the core ops; assemblies of a hundred thousand parts held by mates; drawings, toolpaths and print slices derived from the solid. Contains 2D CAD: every feature starts as a constrained sketch. Every op exact, every result tessellated for the screen, edges and silhouettes exact at every zoom.

Procedural DCC and film (Houdini, Blender, Maya): geometry as data flowing through operator graphs; rigs that stack deformers (skeleton, skinning, blendshapes, lattices); subdivision surfaces; cloth, fluid and rigid-body simulation cached per frame; materials as shader graphs; two renderers over one scene, a real-time viewport and a path tracer. Contains the 2D animation engine. The longest chains in any field; Houdini's whole point is that every step is data.

Real-time open world (Unreal, Unity, Godot): a million instances streamed by where the camera is; LOD, impostors and culling; physically based lighting with shadows, reflections and global illumination, where every object's look depends on every light and every other object; physics and animation state machines each frame; post-process stacks; an editor many people use at once on one level; and the same world rendered twice per frame at 90Hz with the head as camera and two hands as pointer. Contains the map engine and the whiteboard. Everything frame-time, inside a budget that decides architecture.

BIM (Revit, ArchiCAD): building parts that know what they are (walls host doors; move a wall and the roof follows); relationships that propagate across one model many disciplines edit at once with element borrowing; plans, sections and schedules that are 2D derived live from the 3D. Contains page layout and the whiteboard's merging. The 3D-to-2D derivation, and multi-author editing of one model in its hardest form.

Planet-scale geospatial and digital twins (Cesium, Google Earth): coordinates from millimeters to the planet; terrain, imagery, buildings and point clouds streamed as tiles by view; level-of-detail trees; time-dynamic data. Contains the map engine. The precision ceiling and the residency ceiling at their limit.

Fields and scans (ParaView, 3D Slicer, splat viewers): volumes rendered directly; isosurfaces derived from scalar fields; billions of points or Gaussian splats streamed from octrees; meshes derived from scans. Contains the image kind and nothing else from the 2D list. Representations that are neither mesh nor path (volume, point, splat, implicit), where converting between them is the work.

The product mechanics above are general knowledge of those tools, not read from their source.

What makes these hard is the stacking again, derivation on derivation re-run on every edit, plus four things 2D never had:
- a mark's pixels depend on other marks: shadows, reflections, occlusion, transparency order. "Draw items in, pixels out" stops being per item.
- many shape representations at once (mesh, exact solid, implicit, volume, points, splats, curves and hair), and converting between them is the derivation.
- 2D and 3D nest both ways: 2D on a plane inside 3D (labels, drawings on faces, UI in space), 3D in a portal on a 2D page, and 2D derived from 3D (sections, plans, silhouettes).
- ranges and budgets: coordinates past what float32 holds, depth precision, two views per frame at 90Hz, the camera is a head and the pointer is hands.

Sid's framing, verbatim:

"we have build engine and some primitives that get hardcoded and we leave other heigher level layers for ecs and call that above the waist. Anything above the waist is stored as data, is collaborative editable, createable, agents and humans without getting into git merge deadlocks, anything in ecs layer should be creatable and then saved for reuse or build higher order things from it"

"When you see the ceilings, do not try to hack through them. We want to build through them. the question we are asking is: what is the below-the-waist layer that should exist for everything?"

So this is the ceiling, the list above. Are these reachable on the current below-the-waist, or is something missing that has to be hardcoded, and what and why? For each item in the list, how it gets built on top. And the question is the same one, now for everything: what is the below-the-waist layer that should exist for 2D and 3D in one answer?

One thing you should know going in. The 2D round's reply stamped every ceiling "met" and the adversarial pass found each one broke at its own hardest step, over four cracks nobody had named. That is the failure we have already had once, and the same ground is waiting under 3D.
