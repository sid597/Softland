All four documents read, nothing else loaded. Here is the diff, reasoned only from those four.

**The short version.** The baseline handed every build the same kit: a pure executor, brush and Path tool records, text layout, three renderers, a compositor, two harness pages, and on the server a revision store whose edit path loses a record's kind. None of it knew about a person at a screen. All three builds then wrote the same missing thing, each once, each differently: a client world of id-plus-revision records, an owner that turns "something changed" into "these tools rerun", a join from a pixel back to a record, a saved "where I am", and an edit path that enters through the same door as every other change. That world is what the hypothesis called the ECS layer. It now exists three times, in two trees, and no document claims any shared code between the copies. Nobody built a generic component scheduler, and nobody says they missed one. The "S" was plain code reading declared inputs in every build.

Legend for the tables: every cell is read from the named document. "Not said" means the document is silent. Lines marked derived are mine.

## What all three had to build

| Piece the baseline lacked | Place (ecs-layer tree) | Workspace | Vantage |
|---|---|---|---|
| **A page that is an application.** Device, font and canvas boot, resize, a frame only when something changed, teardown. Baseline had two harness benches. | `place` build and page; the runtime owns listeners, the world atom and one pending frame; the clock sleeps when idle | own entry, explicit frame delivery bounded in flight; a Canvas2D copy-back fallback because swap-chain capture failed under software Vulkan | served page; animation frames only present results |
| **Client records with id and revision.** Baseline had revisions only on the server. | world records; replacement advances the revision; records carry an asserter | entity = id, kind, revision, component map, provenance (actor, time, origin, parent); a reference carries id and revision; two values under one revision are refused | vantage and source values with identity, asserter and parent; immutable revisions in the object container, one container per asserter |
| **The change owner.** Change in, which tool inputs changed, run or resume through the executor, publish under declared output ids, present. | deliverer plus a pure loop; each tool declares what it reads as record plus path; the full assembled input is compared with the last; declared outputs; the painting declaration and the painting result have separate ids | events go to local state or to a server command; accepted entities are indexed and projected into renderer inputs, then a frame is requested; rerun is by command, no input rule is stated | courier out, change markers in over SSE; dirty keys are reread; complete returned values decide what reruns; one in-flight refresh owner that discards stale responses |
| **Input levels.** Camera motion must not rerun painting. | coating and composition are separate inputs; camera changes neither | surface geometry retained when only the camera changes | map, camera and brush edits each rerun only their own computation |
| **Pixel to record identity, across representations.** Baseline said this join was absent. | a pointer-identity record separate from the stable inspection record; reverse hit joins in code; hover membership | dab and flow-step selection tied to source anchors and positions; spatial inspection is a surface sample plus the known dab positions and radius | pointer-down starts an executor read against complete snapshots and the later event answers it; the compositor now returns a layer's hit information; the map picks on real Path geometry |
| **Painting on the sphere, and 3D hit to chart coordinates.** Baseline said the renderer did not consume the painting. | skin material, a 256×128 full-sphere painting, direct float-surface GPU upload, bounded raster work | a surface-to-mesh adapter: painted cells become native meshes through the chart-to-support geometry | native Region3D meshes, mechanism not said; a mesh-alpha fix so the opaque body stops overpainting the coating |
| **A saved "where I am" with ancestry and exact return.** | vantage with a from-link, camera, placements and trail | workspace: baseline and candidate references, selected run and view, note, kept references, saved view | vantage: subject, open tools, pins, zoom, selected layers, asserter, parent |
| **An edit path through the same door as other changes.** | Enter emits a normal change event naming the source record; a radius edit replays the stroke; the caret is a source position | field commit becomes a command that makes a new revision, checked against the expected workspace revision | each keystroke is a layer revision through an ordered courier with acknowledgement and retry |
| **A tool record edited by field address from the UI.** | numeric radius and the flow width coefficient | the relation followed and list-versus-columns, saved as a tool revision, pinned original restorable | roots and program EDN through lossless parser spans, via the same layer courier |
| **Text drawn on the page through Softland.** | an executor text capability living inside the client tree; the text tree untouched | the native text system called directly, not as a tool record | a text tool record in the text tree whose steps run through the executor, with per-line inputs |

`★ Insight ─────────────────────────────────────`
Every latency ceiling the three documents report came from the change owner, not from rendering. Place's stroke grew from 45 to 305 ms because a whole stroke reran per point, fixed by holding a continuation at exhausted input. Vantage's profile found the executor re-walking an already completed layout, fixed by a resolved-read boundary. Workspace's 1 to 2 second navigation stall is the view adopting a change only after the server accepts it. The hard part of the layer is "what reruns, and when does the view believe a change", and the three builds are three answers with numbers attached.
`─────────────────────────────────────────────────`

## What only some built, and what now sits below the waist

Two of three:

| Piece | Place | Workspace | Vantage |
|---|---|---|---|
| Kind-preserving wire to the durable owner | not built; C7 awaits your word, and its ASK proposes a separate place-records store | an adapter through the material-import and revision APIs that waits for accept and verifies readback; it bypasses the kind-losing edit path | uses the edit path and fixes kind and visibility preservation inside it; adds layer metadata and a current-layer subindex |
| Read-work custody wired, the DESIGN-1 §12 split | pending reads left as barriers | request and result rows under full declared arguments; the continuation stays transient; the caller grants work | pending-pointer reads against snapshots; resolved-read boundary on completed layouts |
| Executor or read boundary changed for a measured ceiling | hold-at-end | none said | resolved-read boundary |
| Traces as records the client can point at | trace ring feeding the flow lanes and the log | recorded steps and read or change observations per run | measured, not said to be records |
| An explain-the-construction mode | Inside | Anatomy | none |

One build only: authoring layers with promotion and stale refusal (Vantage; Workspace has two retained results, which its own document says are not layers). An agent turn with a readable preview, model and effort on the turn row, and a reply staged in the agent's layer (Vantage; Workspace has context text only). Softland's code as the subject through the existing import and analyzer (Vantage; Place hand-authored twelve nodes). Store-to-browser change markers (Vantage).

Foundation changes already made, by tree. These are landed or dirty code that the next build inherits whatever layer shape wins:

- **Place tree:** executor hold-at-end; float-surface GPU upload; Region3D capabilities, component and renderer changed, plus new raster bounds, skin, skin GPU and surface-equality namespaces. Compositor, Path and text trees untouched.
- **Workspace tree, from Workspace:** the surface-to-mesh adapter in Region3D; server workspace store and model. No new Rama module.
- **Workspace tree, from Vantage:** the text tool in the text tree; compositor returns layer hit info; mesh-alpha pass selection; object container gains layer metadata, a current-layer subindex, kind and visibility preservation on edit, graduation ids by full unit identity, schema extraction for the JVM method-size ceiling, and a subindexed-map serialization fix; turn rows carry model, effort and prompt; the CLI runner is extracted. M6, including the code map and the mesh-alpha fix, is uncommitted.

Derived: the executor now differs from the baseline in two trees in two ways, and the Region3D renderer in three ways, and neither tree's documents know about the other's change. That merge is work before any next build.

## Where the documents don't say

1. **Workspace, how a rerun is decided.** No input-declaration rule appears. It reads as "the command decides", and the 1 to 2 second stall is the cost.
2. **Workspace, whether its inspection tool runs through the executor.** The document names "a few interpreters" in its records namespace. It reads as the brush through the executor and the inspection tool through its own small interpreter.
3. **Place, how keys reach the editor.** Key ticks are counted, but textarea versus key listener is not said. Workspace and Vantage both say a hidden native textarea.
4. **Vantage, how the sphere shows the painting.** "Native Region3D meshes" and nothing more. Workspace's cell-mesh adapter is in the same tree. Whether it was reused is not said.
5. **Vantage, whether the executor file changed** for the resolved-read boundary, or only the value boundary around it.
6. **Two record models in one tree.** Workspace's entity envelope and Vantage's object-container-backed values live in one checkout. Neither document mentions the other's model or claims shared code.
7. **Place's asserter field.** Records carry an asserter with no layers. What it does is not said.
8. **The persistence fork.** Place's deferred ASK proposes a separate store. Workspace and Vantage each say they refused a second truth store. The Place document gives no reason for wanting one.
9. **Kinds.** All three say kinds stay a code-defined vocabulary. None lists its kinds, so whether the three vocabularies agree is unknown. Derived: three kind lists.
10. **The baseline's flagged stale harness text reader.** No build mentions it.

`★ Insight ─────────────────────────────────────`
One baseline defect, the edit path recomputing a record's kind and forcing visibility private, was hit by both wire-builders and answered two ways in the same tree: Workspace routes around it, Vantage fixes it. The Vantage document says the same-suffix graduation-id bug belonged in the shared mechanism because a client-only workaround would leave identity wrong elsewhere. That is the argument for fixing over bypassing, and the next build inherits both answers unless one is chosen.
`─────────────────────────────────────────────────`

One notch beyond what you asked, my positions on which copy to keep for each common piece, each with its exit. Record envelope: Workspace's, the only one that states the whole envelope, and Vantage's storage already fits it. Rerun rule: Place's declared inputs and outputs with full-input compare, delivered Vantage's way, derive locally now and let the courier and change markers catch up. Wire: Vantage's fixed edit path, with birth through common import as both wire-builders already do. Sphere painting: Place's skin, since it is the same surface the chart reads and it has forty pointed-location checks. The exit is merge cost, because Workspace's adapter is already in the workspace tree. Pointer: Vantage's snapshot-checked read as the mechanism, Place's two records, pointer identity versus stable inspection, as the state. Text: Vantage's tool in the text tree, measured at a thousand lines.

This stays in chat until it settles. When it does, it belongs beside the baseline under the ecs-layer docs folder.
