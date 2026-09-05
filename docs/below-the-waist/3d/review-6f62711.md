# Independent review and bench checks of the 6f62711 proposal

2026-09-06. Exploration review of the supplied session summary and the relevant sections of `3d-kind.md` at commit `6f62711`, with two bounded checks of its bench. This evaluates that independent proposal against Sid's original brief. An earlier version incorrectly framed it as an addition to another session's model; Sid corrected that framing. The recipient's standalone feedback is [feedback-6f62711.md](feedback-6f62711.md).

**Decision served:** which positions this chart/portal proposal supports, and which definitions remain unfinished on its own terms. Its concrete imaging and interaction mechanisms are useful; their sufficiency for every ceiling remains a question for this proposal to address.

Preserve: rendering as a callable capability whose result another construction can consume; chart coverage participating in material evaluation; regions separated from the referenced spatial content; explicit nested query routes; direct rendering and retained surfaces as alternative executions for the supported portal case.

Three qualifications for this proposal:

1. **A chart is a coordinate/attachment mechanism; its contents still need an interpretation.** Coating ink can contribute to the host material. An unlit annotation or raised thread has different behaviour. A curved chart maps a 2D domain to a curved surface, with depth varying over that surface, and may require seams, a metric and source correspondence. A flat box-face chart demonstrates this mechanism's planar case. It does not prove that all 2D/3D computation reduces to it. Solid queries, sections, constraints and volume operations still exist before imaging. Participating media require transport through a volume, beyond a nearest-surface colour answer. [PBRT volume transport](https://pbr-book.org/4ed/Light_Transport_II_Volume_Rendering).
2. **Keep coordinate composition, camera control and reactive query maintenance distinct.** The bench's page zoom changes the final placement/aperture map while retaining the inner camera. For projective cameras, these transformations can be combined into a matrix, with an effective screen scale; that does not make the local camera's focal length the authored variable being changed. Curved charts and general portal mappings require mapped coordinates/rays and intersections, beyond one universal matrix product. Hover may reuse the hit function, but additionally needs invalidation from scene changes and changed-answer publication. [PBRT projective cameras](https://www.pbr-book.org/4ed/Cameras_and_Film/Projective_Camera_Models).
3. **Make rendering callable, with a declared output type.** A shadow depth result, radiance for further lighting, a display image, and an identity result are not the same sampled quantity. The page's universal `display-referred, premultiplied` surface wording should be limited to display-colour outputs. A broader request declares view/time, output channels and meanings, sampling/quality and resource demand. Its result carries validity, source/view context and achieved quality. Physical allocations remain replaceable derived resources. One such capability is necessary for reusable render constructions; it does not alone implement all the geometry or executable extension interfaces the ceilings require.

The next definition exercise for this proposal is its chart question: what the saved chart and attachment contain, and what host changes mean for their contents. Work a curved, editable host through material evaluation and editable queries, including correspondence when the supporting geometry changes. The claim for one waist also needs to address construction and query operations beyond chart nesting.

**Captured bench conditions.** At the first check, local `bench-0/seam-bench.html` was byte-identical to `6f62711`; SHA-256 `b81386cf777819c4b50c1ff94bd216a38e4302b5ea815cc2959e3e1e1f04400e`. Isolated browser context, direct execution, window portal. Canvas 572 by 620 CSS pixels at the probe's environment capture; DPR 1.046875. WebGL renderer reported ANGLE / AMD Radeon RX 7900 XTX / radeonsi / OpenGL 4.6. These checks did not rerun the handover's SwiftShader matrix or establish latency, curved-chart behaviour or GPU identity through the inner portal. Bench state was restored and the page unloaded afterward. This review did not edit source or bench files. A subsequent shared-workspace update and its focused check are recorded below; the original receipt describes the committed version.

**Predictions registered before execution.** Page-wheel mode should change page zoom and aperture placement without changing `camProj()` or the ray at a fixed normalized region coordinate. A scene-only occluder change should make a fresh query change while the existing hover panel remains unchanged, because the frame loop does not call `showChain`.

**Camera receipt.** To put the tested point inside the canvas, initialize page zoom to 1 and pan to `[-300,0]`. Choose normalized region point `(0.4,0.56)`. Dispatch a wheel event through the bench's actual wheel handler, in its `focal` mode, with deltaY `-log(2)/0.0016`, then wait two animation frames.

| Observed quantity | Before | After |
|---|---|---|
| Page zoom | 1 | 2 |
| Region aperture width/height | 640 / 440 | 1280 / 880 |
| Inner field of view | 45 degrees | 45 degrees |
| Inner projection vertical scale | 2.414213562373095 | 2.414213562373095 |
| Complete inner camera and projection matrix | Reference values | Identical |
| Ray at normalized region coordinate `(0.4,0.56)` | Reference value | Identical |

Relevant source: [page and camera maps](/mnt/data/projects/Softland/docs/below-the-waist/3d/bench-0/seam-bench.html:260), [direct execution](/mnt/data/projects/Softland/docs/below-the-waist/3d/bench-0/seam-bench.html:317), [wheel handler](/mnt/data/projects/Softland/docs/below-the-waist/3d/bench-0/seam-bench.html:401).

Interpretation: the bench implements magnification of the page and the region's aperture. An equivalent effective projection into the final canvas can have a larger focal scale. An optical zoom of the local camera within an unchanged aperture is a distinct operation. Dolly is a third operation. The input policy can choose among them; the terminology should preserve which state actually changes.

**Stationary-hover receipt.** Reset zoom to 1 and pan to `[-300,0]`. Query and display the chain at canvas position `[256,296.4]`. It hits box face 4, at chart coordinate approximately `[0.728,0.491]`, inside stroke A. Without dispatching a pointer event, move the sphere in runtime state onto that ray at `oldHit.t - 1`, mark S1 and shadow dirty, and wait two animation frames.

| Observed result | Value |
|---|---|
| Additional S1 renders | 1 |
| Existing hover panel | Still box front face / stroke A inside |
| Fresh explicit CPU query | Sphere |
| Fresh GPU ID comparison | Sphere; agrees with CPU |

This is a synthetic scene-data update, not a demonstration of a UI control for moving the sphere. It exercises the original hard case's relevant dependency: the scene changes under a stationary pointer. The rendering/query functions can resolve the new target; the existing display of the answer is not refreshed by that scene change.

Relevant source: [frame loop](/mnt/data/projects/Softland/docs/below-the-waist/3d/bench-0/seam-bench.html:310), [GPU ID pass](/mnt/data/projects/Softland/docs/below-the-waist/3d/bench-0/seam-bench.html:337), [CPU chain](/mnt/data/projects/Softland/docs/below-the-waist/3d/bench-0/seam-bench.html:362), [pointer-driven publication](/mnt/data/projects/Softland/docs/below-the-waist/3d/bench-0/seam-bench.html:411).

The smallest conceptual addition is a query subscription that depends on the current pointer, relevant scene/view state and query policy, then publishes changes to the addressed result. This keeps the shared hit function and the nested chain. The receipt establishes this missing join in this bench; it does not measure a production reactive implementation or choose Sid's preferred navigation behaviour.

**Concurrent update, checked separately.** Before this review finished, another writer added the sphere position to scene invalidation, retained the last pointer position, and refreshed the chain from the frame loop when the scene key changed. The updated bench's SHA-256 was `d445dfd2e08b130ad5e057685d6b8a284a3f881d5de5cb3a1ee7ce6e486c1c3e` before and after the follow-up check. In a new isolated context, again using direct execution and a window portal, dispatch one pointer move to `[256,296.4]` at page zoom 1 and pan `[-300,0]`. The panel names the box front face and stroke A. Apply the same synthetic sphere placement into the ray, with no subsequent pointer event, and wait two animation frames. One S1 render occurs; the panel now names the sphere and removes the chart row's target. Its automatic GPU ID read and a fresh explicit CPU query both name the sphere. State was restored and the page unloaded. The revised bench therefore demonstrates the added scene-to-hover join for this case. No further claim about query modes, dependency precision, latency or nested GPU identity follows from it.
