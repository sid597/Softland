# Attack 4 — a result used somewhere else

2026-09-06. Definer's chair, following [HANDOFF-11's still-owed list](HANDOFF-11.md), [the folded page](path-kind.md), [fact base 11](fact-base-11.md) and [the bench handover](bench-9/HANDOVER.md). Attack 3's fixes are the starting ground. Positions 7, 8 and 9 remain Sid's; none is asked again here.

**The six pieces accommodate these next constructions. What needs extending is how their results travel: into a different input, inside another record, and through saved bytes.** A returned path already supplies the geometry of a mask. A returned record must keep the surfaces it contains readable. A serialized continuation can restart the brush in a fresh host. These are three concrete meanings of “reuse its results,” each with a binding or ownership obligation below the waist.

| Construction | What the existing bench executes | Where the proposed machinery still needs work |
|---|---|---|
| Use a selected network face as another tool's mask | The network returns a path. The existing packer and both draw operations consume that path as a mask when explicitly bound. | The record's `clip.path` has no consumer; `buildClip` accepts only an authored `clip.source`. A live reference to another construction's output also needs resolution and dependencies. |
| Return paint proofs inside records | The existing definition's `map` and `emit` construct the records. Both hosts finish the program. | The executor releases a surface still reachable inside the returned array. The GPU refuses its later read. |
| Save twelve dabs, reload their continuation from bytes, finish the stroke | With an explicit codec feeding `host.resolve`, fresh CPU and GPU hosts each reproduce their own straight run, with zero differing float components. | The record's proposed `checkpoint.load` is unread. The bench still takes a new prefix instead of loading those supplied bytes. |

The first and third gaps are bindings into operations that exist. The second is an extension of the executor's resource analysis. None of these receipts requires another geometry or coverage implementation. They also do not establish that every future tool is expressible or that the client port is complete.

**A network face used by another tool.** I ran the unchanged network construction from [attack 2](attack-2.md): `edge-curves → arrange → locate → face-boundaries`. Its seed `(60,10)` selects the top triangular face of the crossed rectangle. Below is a complete record containing that actual returned path, including its source-edge intervals. It deliberately supplies a value to the mask input, rather than reconstructing an authored pen source from the answer.

```json
{
  "tool": { "name": "rectangle masked by a returned network face" },
  "source": { "kind": "rect", "x": 0, "y": 0, "w": 120, "h": 80, "r": 0 },
  "paint": { "fill": { "rule": "nonzero", "color": [1,0,0,0.6] }, "stroke": null },
  "clip": {
    "path": {
      "subpaths": [{
        "closed": true,
        "start": { "x": 0, "y": 0 },
        "segs": [
          { "k": "L", "p": { "x": 120, "y": 0 }, "src": "AB:[0,1]" },
          { "k": "L", "p": { "x": 60, "y": 40 }, "src": "BD:[0,.5]" },
          { "k": "L", "p": { "x": 0, "y": 0 }, "src": "AC:[0,.5]" }
        ],
        "knots": [{ "id": "V1" }, { "id": "V2" }, { "id": "X1" }, { "id": "V1" }]
      }],
      "meta": { "kind": "network", "face": "f1", "area": 2400, "loop": "V1 → V2 → X1 → V1" }
    },
    "rule": "nonzero"
  },
  "snap": false,
  "identity": { "id": "face-mask", "revision": 1 }
}
```

Through the actual `applyRecord → rebuild` route, the bench reports `clip.path` and `clip.rule` unread and produces no clip. At texel centers `(60.5,10.5)` and `(60.5,70.5)`, its rectangle has alpha `0.6000000238` at both. The former is inside the selected face; the latter is outside it.

I then bound the **same returned path** through `lowerPath → packRegion → packGpu` into the existing `drawRegion` clip argument. Those two alphas become `0.6000000238` and `0`. This second receipt uses a probe supplying the missing binding; it is not a working `clip.path` record route in the bench. No shader or geometry function body changed.

That result can be used across the painting and vector constructions. I kept the pressure-varying, self-crossing source from attack 2, with the same width and spacing. Changing the network's seed to `(60,70)` supplies the bottom face instead. At the stroke crossing, texel center `(64.5,64.5)`:

| Consumer of the face path | Top-face mask | Bottom-face mask |
|---|---|---|
| Vector-style union stroke: one region, opacity `.62` | RGBA `(0,0,0,0)` | Alpha `0.6200000048` |
| Painting-style accumulation: 24 regions, opacity `.62` each | RGBA `(0,0,0,0)` | Alpha `0.8555999994` |
| The completed pickup-brush surface, presented through the mask | RGBA `(0,0,0,0)` | RGBA `(0.0133695230,0,0.9866304398,1)` |

The first two use `drawRegion`; the third uses `drawSurfaceAsRegion`. The same packed face is their mask input. The pickup brush still samples and deposits on its declared surface: this last row masks presentation of its completed result. A tool asking to clip deposition before later pickup must put the clip on those paint operations. Those two behaviors should stay separately sayable; this receipt proves the presentation case.

This gives someone a concrete new tool: choose a network face, then use that answer to constrain an existing fill, stroke or painted result. Change the seed to change the mask; change `overlap` to change how the stroke deposits; keep the returned face path to reuse it with another tool. The source network and the consumer's pressure samples remain different authored records. Neither needs a native “network-mask brush” operation.

**My provisional position is that a consumer accepting a path accepts a derived path directly.** Its authored-source convenience can remain an upstream way to obtain that same input. The immediate addition to `buildClip` is a path input shared by child and group clips. The packer then applies the consuming placement and tolerance as it already does for other regions.

The literal path above is a frozen result. For a mask that follows edits to the network, the input instead needs a reference to a particular producer and output, resolved by the executor. For example, the following is a proposed replacement for this record's `clip` field; `network-mask` denotes the separately held network construction, not a new source kind:

```json
{ "clip": { "path": { "result": "network-mask", "output": "path" }, "rule": "nonzero" } }
```

That reference syntax is illustrative, not a ruling on Position 8. Its execution obligation is concrete: resolve the producer's current returned path before packing the clip, retain its producing revision and source correspondence, and invalidate the dependent mask when that output changes. A camera change can repack it without rerunning the arrangement. A frozen copy has no such live dependency. The bench currently has neither this reference binding nor the literal path binding.

I would change this position if a mask consumer demonstrated a need for information the returned path cannot carry. Then the needed input should be named. Requiring every result to impersonate an authored `source.kind` has supplied no such benefit in these constructions. Shared packing alone does not decide the interface: the reason for a path input here is that a tool already produces that reusable answer.

**A returned record that contains a surface.** Now make the paint-preview tool return its proofs in labeled records. The following complete record uses only the bench's existing program vocabulary. The one-element `tool.slots` is ordinary tool data; the `keep` definition is its ordinary `map`/`emit` construction.

```json
{
  "tool": {
    "name": "proofs returned in a record", "size": 8, "streamline": 0,
    "fit": "polyline", "width": "size*p", "slots": ["red proof"]
  },
  "source": { "kind": "pen", "samples": [[8,8,1,0]] },
  "paint": {
    "fill": null,
    "stroke": { "overlap": "accumulate", "spacing": 12, "tip": "nib", "width": "knot",
      "unit": "local", "cap": "round", "join": "round", "color": [1,0,0,0.5] }
  },
  "surface": {
    "id": "proof", "revision": 0, "width": 16, "height": 16,
    "localToTexel": [1,0,0,1,0,0], "color": "linear-premultiplied-rgba",
    "initial": { "clear": [0,0,0,0] }
  },
  "program": {
    "each": "dabs",
    "state": { "base": "surface.initial" },
    "definitions": {
      "keep": {
        "parameter": "image", "map": "tool.slots", "as": "label",
        "emit": { "label": { "ref": "label" }, "surface": { "ref": "image" } }
      }
    },
    "steps": [
      { "out": "red", "op": "paint", "surface": "state.base", "region": "dab.path",
        "rgba": [1,0,0,1], "opacity": 0.5, "blend": "source-over" },
      { "out": "bundle", "op": "keep", "image": "red" },
      { "out": "blue", "op": "paint", "surface": "state.base", "region": "dab.path",
        "rgba": [0,0,1,1], "opacity": 0.5, "blend": "source-over" }
    ],
    "next": { "proofs": "bundle", "surface": "blue" },
    "return": ["state.proofs", "state.surface"]
  },
  "snap": false,
  "identity": { "id": "nested-proof", "revision": 1 }
}
```

Both hosts report `ok: true`; the unread-field list is empty. After the run, ask the host to sample the surface held in `state.proofs[0].surface` at `(8,8)`:

| Observation | CPU | GPU |
|---|---|---|
| Sample the returned red proof | `(0.5,0,0,0.5)` | Stale read refused |
| Sample the selected blue result | `(0,0,0.5,0.5)` | `(0,0,0.5,0.5)` |

The GPU's exact refusal is:

```text
surface proof:0/red no longer holds its contents (superseded in place by proof:0/blue): a stale read, refused
```

The word “in place” in this diagnostic covers target reuse here; the host's counters report **zero in-place paints**, two copies and one release. `compileUses` puts the last direct read of `red` at step 1, the `keep` call. It retains `bundle` through `next`, but `aliveAfter` compares direct object identity and does not find the red surface *inside* that array. The executor releases red; blue obtains its target from the pool. The wrapped reference survives as an object, while its contents do not. The CPU host's unconditional copying masks that mistake because its release operation does nothing.

Two bounded controls identify the missing dependency:

1. Add `"red": "red"` to `next` and `"state.red"` to `return`. The nested red proof now remains readable on the existing GPU host. It uses three targets including the frozen start, rather than two, with no release before the read.
2. Keep the original record unchanged, but run it with a reference adapter that declines release and passes `consume: false` to paint. Both proofs remain readable, also using three targets. This adapter supplies a conservative execution receipt; it is not a bench implementation or a proposed permanent allocation policy.

**My provisional position is that keeping a returned record keeps the logical resources reachable through it.** Otherwise a tool author has to repeat every nested surface as a top-level return merely to keep the record usable. The meaning of data composition would depend on its incidental packaging.

The executor should account for those contained references when determining lifetime, including references carried by `next` and values that escape through `return`. The compositor still owns allocation and copying. A conservative first implementation can traverse supported aggregate values and retain reachable resources; a compiler can later derive the same information from the definition's parameter, `map` and `emit`. Capability definitions that return or alias resources need to expose that behavior to the analysis. “Compile pass” does not by itself supply the analysis, and retaining a whole array cannot be treated as merely reading its outer object.

This extends attack 3's result contract; it does not replace it. I would reconsider implicit retention if the chosen construction language explicitly restricted surfaces to linear or consuming tokens. Then constructing this record must either perform a declared retain/fork or be rejected before execution. Under today's value-shaped vocabulary, the valid program and empty unread list cannot count as proof that its returned values are usable.

**A checkpoint reloaded from bytes.** Keep the pickup record and its 24 dabs unchanged. Run the first twelve dabs, save the actual carried state and surface content, discard the originating host, decode the saved representation into a fresh host, then execute the remaining twelve. This extends the in-memory continuation already on the bench; it does not change the brush recipe.

The saved prefix has the following concrete state:

| Field | Value |
|---|---|
| Next dab ordinal | `12` |
| Carried premultiplied RGBA, CPU prefix | `[0.00031892763945506886, 0, 0.9996810704469681, 1]` |
| Surface result key / parent / depth | `paint:11/surface` / `paint:10/surface` / `12` |
| Surface interpretation | `128 × 128`, linear premultiplied RGBA, local-to-texel `[1,0,0,1,0,0]` |
| Content encoding used in this probe | 65,536 float32 components, little endian: 262,144 bytes |
| CPU content SHA-256 | `9757e826f4594d5aa2c628302831ddee00dc57c8037340ee5bc14428d395aa82` |

The wire object in the probe has `format: "softland/path-checkpoint"`, `version: 1`, the existing `checkpointKey` result as `dependencies`, and these two parts:

```text
continuation
  at: 12
  state
    carry: the four numbers above
    surface: { surfaceRef: "paint:11/surface" }

resources["paint:11/surface"]
  key, parent, revision, width, height, localToTexel, color
  encoding: "rgba32f-le"
  byteLength: 262144
  data: base64 of those exact bytes
```

That is a description of the payload layout, not a pasted checkpoint with its image bytes omitted and claimed runnable. The executed probe generates the complete payload from `host.content(pre.state.surface)`: it writes each float with `DataView.setFloat32(offset, value, true)`, base64-encodes the byte array, then JSON-serializes the entire object. On load it parses JSON, checks the format and dependency key, checks the encoding and payload length against the dimensions, decodes with `getFloat32(offset, true)`, and passes the resulting `Float32Array` to the existing `host.resolve`. The carried numbers and `at` go directly to `runProgram` through `opts.from`. Resource keys in this wire object are scoped to that saved continuation, not asserted to be global identifiers.

| Executed route | Restored prefix vs saved prefix | Resumed final vs independent straight run | Work after load |
|---|---|---|---|
| CPU prefix → bytes → fresh CPU host | 0 of 65,536 float components differ | 0 of 65,536 differ | 36 steps |
| GPU prefix → bytes → fresh GPU host | 0 of 65,536 float components differ | 0 of 65,536 differ | 36 steps |

The prefix had cost 36 steps. The GPU prefix host was explicitly freed after serialization and before the destination host was created. Neither resumed route reused the originating live surface. Each comparison is to a straight run on the **same backend**; it does not close cross-device or multiplayer numeric truth.

To make the record-side construction concrete, the probe also created a complete candidate record as follows, where `wire` is that fully serialized and parsed object:

```javascript
const loadedRecord = JSON.parse(JSON.stringify(pickupRecord));
loadedRecord.checkpoint = { at: 12, load: wire };
```

Pasting that generated record through `applyRecord → rebuild` produces an unread **`checkpoint.load`**. The bench takes its own fresh prefix, then resumes that. Its green checkpoint comparison is accurate for the work it ran, but does not establish that it consumed the supplied checkpoint. The record loader needs to resolve `load`, validate its dependencies, materialize its resources and bind the continuation before the tail runs. Inline base64 is only this probe's complete transport; a content store can supply the same bytes by reference without changing the continuation's meaning.

The existing dependency key already gives useful reuse decisions on real edits to this fixture:

| Change before checking the loaded prefix | `checkpointWhy` result |
|---|---|
| First source pressure `.65 → .4` | `dab 0 differs` |
| Final source sample's x-coordinate `102 → 110` | Reusable: the fields read from the first twelve dabs are unchanged |
| `tool.pickup` `.5 → .25` | Rejected: a fixed dependency changed |
| Capability version changed in the key | Rejected: a fixed dependency changed |

These are dependencies of this recipe and emitter, not a universal rule that moving the last point cannot change an earlier prefix. A fitter or width expression that uses future samples or total length could change those earlier dab inputs.

**My provisional position is that saving a continuation saves its ordinary state and the content of its reachable resources, with an explicit representation and compatibility check.** A framebuffer handle and a typed array's incidental JSON representation cannot provide that contract. As a small control, feeding a naive `JSON.stringify`/parse of the bench's `Float32Array` content to the current CPU resolver produces an array of length zero: an object with numeric property names is not the typed payload that resolver expects.

The nested-proof case makes the generalization precise. If a future checkpoint's state contains labeled proof records, capture must find their surfaces too and resolve repeated references to the same saved logical result consistently. The current checkpoint capture/bring code checks only top-level state fields for surfaces. This is a source observation, not an additional executed nested-checkpoint receipt. The same resource reachability definition should serve lifetime and saving, with the compositor supplying content and restored surfaces.

I would narrow what gets saved for a tool that needs only a frozen image, or only a recipe and a deliberate replay from its origin. Those are different reusable results. They do not remove the carried color or continuation point needed to resume this surface-reading brush. Dimension, map, color and capability compatibility belong at load; a changed interpretation must be an explicit conversion or a failed load, rather than silently being supplied by the destination host's declaration.

**What changes on the page if these positions hold:**

| Page location | Concrete amendment |
|---|---|
| Contract: clip `{path, rule}` and construction bindings | Say the path may be an authored construction's output, a frozen derived value, or a resolved live result reference. The literal value requires no source reconstruction. Mark the bench's missing `clip.path` and result-reference consumers. |
| Contract: returned results and compositor lifetime | Extend “keeps its contents” through records and arrays, carried state, and returned values. The executor accounts for reachable logical resources; the compositor manages their physical storage. |
| Contract and per-brush-step row: checkpoint | Add the load binding, explicit content representation, resource references, and compatibility check. Distinguish the already implemented in-memory checkpoint from the fresh-host serialized continuation demonstrated here. |
| Waist test: evidence for reusable results | Add the face-mask rows, the nested proof and its stale-read refusal, and the two fresh-host reload receipts. An unread-field report and `ok: true` are helpful observations; later use of returned results is part of the construction's receipt. |

The remaining work stays a fix list: direct and referenced result inputs, aggregate resource lifetime, and checkpoint capture/load through the same resource references. Tangencies and partial overlapping spans remain the arrangement's next geometric constructions; this attack does not claim to have worked them. Offset stroking, GPU brush carry, layer allocation and Sid's open positions retain their existing status. The client port remains separate work; this contribution extends the definer's constructions against the bench.

**Execution scope and reproduction anchors.** Page commit `544cab3`; bench commit `550ce4e`; bench SHA-256 `54be8ca6248558acf0d3413f45067820450da943b565ff7b41ebc7fef2c3912d`. Node `v20.20.2` executed the pure declarations before `// ---------- GL plumbing ----------`, with the existing `normStroke` declaration. The browser probes served the unchanged HTML function bodies with an additional probe inside their closure, using headless Chrome, WebGL2, SwiftShader and RGBA32F; the main GPU probe ended with `gl.getError() = 0`. No bench or client source was edited.

The complete literal mask and nested-proof records above reproduce their entry-route findings. Network and pickup inputs are the two JSON records in attack 2. Relevant bench functions at this baseline are `compileUses`/`runProgram` (lines 734–775), `checkpointKey`/`checkpointWhy` (778–787), `cpuHost` (791 onward), `gpuHost` (1087 onward), `buildClip` inside `rebuild` (1211), checkpoint capture/bring (1261 onward), and `applyRecord` (1336). Successful direct mask binding and serialization use the explicitly described probe adapters; their record bindings are proposed, not landed. The GPU aggregate finding is a refusal on a returned value after successful execution, not a claim that the GPU returned the wrong red pixel.
