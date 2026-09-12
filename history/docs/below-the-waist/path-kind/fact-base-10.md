# Path kind — fact base, session 10 (the composer's chair, 2026-09-06)

Session 10 inherited the picture from session 9 (`HANDOFF-9.md`), met the definer's `attack-2.md` first, then worked the two things Sid said at the end of session 9 that the picture had not caught up with: clips, blend modes and layer stacks ("we should be ahead of everyone or at the very least adopt the best in field"), and the executor ("one executor idk seems like the constraints and hard part would be getting them in line throughput seems small"). Everything here is CHECKED, DERIVED, FIELD or POSITION as marked; Sid ruled on nothing this session.

## Part 1 — what was checked in the tree this session (fence: src/app/client/ only; a code-hunter's read, 2026-09-06)

The compositor, which the page had never gathered because "on top" never asked for it.

- API: WebGPU throughout (`GPUDevice`, `createRenderPipeline`, WGSL). No compute pipeline or pass anywhere (`createComputePipeline`, `beginComputePass`, `workgroup`: no hits). No swapchain in `src/app/client/` (`getCurrentTexture`, `.configure`: no hits); present targets a harness-owned texture (`harness/region.cljs:387-395`).
- Offscreen targets exist: `engine/compositor.cljs:157-189` `create-target!` (RENDER_ATTACHMENT | TEXTURE_BINDING | COPY_SRC | COPY_DST), `:191, :239` `acquire-target!` / `release-target!` pooled by `[format w h sample-count]`, a 512 MiB default budget (`:27`), epoch reclaim (`:112, :260`). Region leases (`:451-560`): `color-msaa` rgba16float ×4, `depth` depth24plus ×4, `resolve` rgba16float ×1, optional `shadow` depth32float 2048², sizes quantized to 256 up to 4096.
- Present: `compositor.cljs:362-374` one pipeline per output format; `:46-66` the fragment unpremultiplies, encodes linear → sRGB, re-premultiplies; no blend key, an overwrite. `draw-present!` `:644-671`. Caller: `harness/region.cljs:393` only.
- The named "composite": `region3d/renderer.cljs:1390-1412` `composite-region!` draws a region lease's resolve texture as an instanced quad (`:406-419`, target rgba16float with `blend-state`) into the caller's open pass; the bind group `:1366-1378`. That is a surface painted as a region, hardwired to 3D regions.
- Scissor: `compositor.cljs:612-628` `apply-scissor!` per draw; callers `text/renderer.cljs:851, :854` (inside `draw-instances!`, from a `:scissor` vector the caller supplies) and the present (`:665`, nil). `text-clip-runs` (`text/renderer.cljs:811-836`) has no caller. Nowhere else.
- No stencil anywhere (only `:depthStencil` descriptor keys at `region3d/renderer.cljs:392, :403`, `on_plane_renderer.cljs:113`); no mask texture; no `setBlendConstant`.
- Groups: `engine/transform.cljc:65-79` a group is `:affine :x :y :scale :scale-x :scale-y :rotation :parent :camera` — no opacity, blend, clip or z. GPU row `device.cljs:65-73, :88-137`: 32 bytes, `[axis-x.xy, axis-y.xy, translation.xy, flags:u32, pad]`, flags = one bit, screen or world (`transform.cljc:242-261`). The group index is baked per kind (`path/renderer.cljs:158`, `text/glyph_pack.cljs:170`, `image/renderer.cljs:747`, `region3d/renderer.cljs:1040, :1412`).
- Blend states: `engine/color.cljc:60-82` two modes, legacy straight alpha (`src-alpha / one-minus-src-alpha`) and linear premultiplied (`one / one-minus-src-alpha`); `device.cljs:53-62` names them; region3d and on-plane hardcode premultiplied (`region3d/renderer.cljs:309-315`). The word "blend mode" does not appear.
- Per-item opacity only: path paint `:opacity` folded into the vertex colour (`path/component.cljc:31-38, :318-324`); image `:opacity` into the tint alpha (`image/component.cljc:91-93, :363-373`); on-plane ink (`region3d/on_plane.cljc:120-126`). No group opacity, no per-frame fade.
- Draw order: no engine-level cross-kind ordering; region3d sorts its meshes (`region3d/renderer.cljs:894-913`); across kinds the order is whatever the harness paints in (`harness/region.cljs:336-354, :391`).
- Pipelines and passes: path (`path/renderer.cljs:107-127`, no depth, no MSAA), text (`text/renderer.cljs:392-409`), image (`image/renderer.cljs:173-192`; a mip pipeline `:223-230` with its own passes `:245-281`), region3d mesh ×2 (`region3d/renderer.cljs:380-394`, depth24plus, MSAA 4), shadow (`:396-404`), composite, worn, rejection (`:406-445`), on-plane (`on_plane_renderer.cljs:91-113`). Passes begun: `begin-target-pass!` (`compositor.cljs:630-642`), present, `encode-shadow!` (`region3d/renderer.cljs:1268-1292`), `encode-interior!` (`:1294-1321`, MSAA resolve into `resolve`), the mip pass.

## Part 2 — what bench 9 measured this session (headless SwiftShader, zoom 3, DPR 1, canvas 1022 × 761; `bench-9/HANDOVER.md` has the table)

- The three numbers at one pixel, the crossing of the harness Z at (64, 64): union 0.62; accumulate (24 dabs at 0.62) CPU 0.86 · GPU 0.85; a layer (24 dabs at alpha 1, the group at 0.62) 0.62 both.
- A stroke through a mask (the draw stroke clipped by the trefoil): at (92, 64) clip coverage 1.00, stroke coverage 1.00, alpha 0.85 both; the clip is 24 curves in the same draw; no extra target.
- Dabs that multiply on paper: at (64, 64) CPU (0.795, 0.342, 0.797, 1) · GPU (0.796, 0.341, 0.796, 1); 24 backdrop copies, 52,703 device px per frame.
- The pickup brush (attack 2's record verbatim): 24 dabs, revision 24; dab 19 sampled (0.019, 0, 0.981, 1) → carries (0.010, 0, 0.990, 1) on the GPU and on the CPU twin; texel (64, 64) (0.013, 0, 0.987, 1) on both; 24 texel reads = 4–26 ms of GPU round-trips in software, paints 2–6 ms, the CPU twin 5–9 ms; presented once, never re-run on a camera move. In Node with the same functions: texel (64, 64) = (0.013369522, 0, 0.986630440, 1) against attack 2's (0.013369522403, 0, 0.986630477597, 1); dab 19's carry 0.009688745886 against 0.009688745811 (float32 storage).
- `width: "size*p*p"`: dab 4 radius 4.677200212096, attack 2's number; the emitter now evaluates the width function at the dab's own interpolated pressure.
- The network (attack 2's record verbatim): 5 vertices (1 discovered), 8 pieces, 16 half-edges, 4 faces of 2400, total 9600; the value A → B → X1 → A; at the seed (60, 10) winding −1, alpha 0.60 both; 0.1–0.2 ms. C moved to [100, 100]: the crossing at (48, 48), .48 on AC and .6 on BD; faces 2880, 3120, 2080, 1920; the seed's face still selected.
- The curved edge: 7 vertices (3 discovered), 12 pieces, 24 half-edges, 6 faces 3800, 900, 3800, 900, 100, 100, total 9600; crossings at AC .75/.5/.25 × BD .25/.5/.75 at (90, 60), (60, 40), (30, 20); the value A → B → X3 → X2 → X1 → A, 2 cubic + 3 line, 13 quadratics at τ .01, coverage 1 at the seed; 0.6–0.8 ms.
- A `CA-copy` edge: one geometric edge with two owners, span AC:[0,1] ↔ CA-copy:[1,0], still 4 faces.
- `op: "read"`: "unknown operation", the construction stops, nothing painted. Every fixture's record: "every field of the record was read by something below the waist"; an empty pen burst lists its stroke fields as unread, which is true.
- Every older fixture (draw, pen, border, star, z, pressure, holed) still agrees CPU against GPU at the cursor.
- Two GPU traps found and fixed on the way: two sampler types on one texture unit is an `INVALID_OPERATION` and the draw silently does nothing (every sampler is now bound to a typed unit, a dummy when unused); binding the backdrop copy on the active unit knocked the clip's band sampler off its unit (it has its own unit now).

## Part 3 — derivations

- A layer at opacity α over dabs at alpha 1 gives the union look: the layer's alpha is 1 wherever any dab covers, so the composite is α everywhere the stroke is, 0.62 at the crossing; with dabs at α painted directly it is α + (1 − α)·α = 0.8556 (DERIVED; measured).
- Over transparent nothing every blend mode is over: the W3C formula mixes the source toward B(backdrop, source) by the backdrop's alpha, which is 0 (DERIVED; the bench shows it).
- Isolation is needed only for: a translucent group over overlapping children, a non-over blend on a group, or a mask applied to the composite rather than to each child; for a mask m and children A over B, (m·A) over (m·B) ≠ m·(A over B) unless m ∈ {0, 1} (DERIVED). Otherwise the group's regions paint straight through. The number of live layers is the nesting depth.
- The executor does no per-element work: on the bench the 72 steps of the brush cost 0.2–0.8 ms of sequencing; the 24 GPU syncs cost 4–26 ms; the CPU twin's 24 paints cost 5–9 ms. Where the serial cost lives is the surface read, not the executor (measured; the ratio is the point, not the software-rendered milliseconds).
- Plain accumulation needs no sync: one instanced draw in painter's order, which the API guarantees (FIELD, the WebGL/WebGPU primitive-order rule); only a brush that reads what it painted needs the previous dab resolved.
- The count of pieces is closed by their outputs: a record (the executor), a path (the source stage), a region and an answer (the geometry), coverage (the packer and filler), a surface (the compositor). A seventh piece would need a seventh kind of output (POSITION).

## Part 4 — FIELD claims made this session (unverified here; the definer may pin them)

- Skia's canvas carries a clip stack and calls `saveLayer` only when a group needs isolation; its coverage-AA clips multiply into the draw; advanced blend modes read the destination. Vello's scene is a flat encoding with push/pop clip and layer commands; the fine rasteriser keeps a per-tile clip stack and blend stack and never allocates a full-screen layer. Rive's renderer reads the framebuffer in place (pixel local storage / raster order). Browsers isolate a group only for opacity < 1 with overlapping children, a blend, or a mask on the composite.
- PDF 1.4's transparency model carries clips, transparency groups (isolated, knockout) and blend modes as part of the imaging model.
- Krita's indirect painting mode and Photoshop's brush layer paint dabs opaque into a stroke layer that is then composited at the stroke's opacity; Procreate and Krita smudge by reading the surface per dab; Krita paints on the CPU in tiles with threads.
- Drawpile paints on the CPU so every client computes identical pixels; a multiplayer surface that a brush reads drifts otherwise.
- Houdini compiles VEX to native SIMD; Blender's geometry nodes evaluate fields as arrays; both are one cook engine over native nodes. WebGPU has no framebuffer fetch; reading the backdrop needs a copy or a compute pass.
- The W3C Compositing and Blending Level 1 formulas: Cs' = (1 − αb)·Cs + αb·B(Cb, Cs), then source-over.

## Part 5 — the definer's attack 2, folded (the record of the meet)

Read in full after the bench work of session 9 was landed; folded at full weight; nothing ranked. Where each finding landed:

| Finding (attack 2) | Landed |
|---|---|
| Position A: the fifth piece executes a construction with inputs, outputs, state and host calls; `execute(recipe, inputs, state) → results + next state` | Position 8 rewritten as the executor; bench 9's `runProgram` over a capability table (`sample`, `mix`, `paint`, `arrange`, `locate`, `face-boundaries`, definitions); both records run unchanged |
| Position B: a surface interface beside geometry (sample and paint; resolution, coordinates, content, ordering) | Position 9: the compositor is that interface, and the same piece a clip, a blend and a layer need; the "brushes beside" box left figure 1 |
| Position C: the arrangement as a reusable geometry result; face selection an editable caller | the answers (`arrange`, `locate`, `face-boundaries`); bench 9's arrangement (lines exact, curves refined by Newton from flattened seeds); the seed is the record's policy |
| Position D: sources, logical results and execution resources distinct; materialize what the next operation needs | "surfaces are values" in figure 1; the CPU host copies on paint; the GPU host paints in place and the handover says where they would differ |
| "anchors already are a path" scoped; a network has an authored graph and an executing construction | figure 1's source row; the waist-test table's network row |
| intersections as contacts and overlapping spans; splits with provenance; tangencies and ordering ties are geometric work | the answers; the bench merges exactly coincident edges and lists partial spans, tangencies and ties as not handled |
| dab packets carry the source location and attributes; the width function at the dab, not interpolated between evaluated widths | fixed (4.677200212096); the dab-packet line in the answers |
| unconsumed fields and unknown operations are findings; an empty drawing is not the tool having run | the bench lists the fields nobody read and stops at an unknown operation |
| rates: the surface advances with brush steps or a replay; presentation with the camera | the axis table's per-brush-step row; the bench counts presentations since the run |
| the GPU route keeps an ordered read-before-write, never the target as its own input | the backdrop copy on its own unit; the carry-on-GPU route on the fix list |
| the page-change table (Position 8 spelled out, the pen scoped, the contract's beside-block, the answers, Position 2 and the drawing, the rate table, the bench readouts, the fix list) | all done, as the page's "attack 2, folded" table says row by row |

Not done from attack 2: partial overlapping spans, tangent contacts that do not cross, ties in the tangent order, holes in general (assigned by containment on the bench), a replay from a checkpoint (the bench replays from the start on every edit).

## Part 6 — the two positions, and what would change them (the page carries the full text)

- **Position 8, the executor**: a vocabulary of capabilities (the real waist for behaviour; grows by attack), a sequencing rule over records (per edit, per brush step, never per frame; moves values only), per-element expressions compiled to the host's form; instances many, the definition once. Sid's "getting them in line" is the vocabulary; his "throughput seems small" is the surface read, which is the compositor's and bounded to brushes that read. Changes it: a tool needing control flow the format cannot say opens the sci door for that class; a ruling that tools are code above a data-only contract kills it.
- **Position 9, the compositor**: `paint(surface, region, paint, clips, blend) → surface` and `sample(surface, point) → colour`; a clip multiplies in the same draw; a blend reads the backdrop; a layer is a surface painted as a region, only under isolation; the screen is the root; a painting surface advances per brush step; a brush that reads is a construction calling sample. Adopt Vello's scene and Skia's canvas as the contract, said as data; implement on the filler road (bench 9 does); the compute road is the end state with the same contract. Changes it: a ruling that layer stacks are a document structure above the kind moves three fields off the group; two clients needing identical pixels of a surface-reading brush makes the CPU twin or one authority the truth.

## Part 7 — fix list carried (never rank penalties)

- From session 9, still open: the offset stroker (the envelope flattens the centerline; ×5–8 curves); the nib's butt cap on a taper; degenerate nib pieces; the wet route re-packs the whole stroke; even-odd antialiasing on edges; arrow caps.
- From attack 2 and session 10: partial overlapping spans, tangencies, tangent-order ties, general holes; state and replay dependencies (a paint on an old revision differs between the bench's hosts); the carry-on-GPU route for the sample (no CPU round-trip); layers at the group's cover, not the target's size; a group mask on a soft edge versus clipping each child; blends on a layer or a presented surface read the whole target as backdrop; the `each` item's name is the plural minus `s`, a bench convention.
- Determinism across clients for a surface-reading brush: open, and Sid's.

## Part 8 — what the definer can do with bench 9 now

- Paste any record with `clip`, `group`, `surface`, `program`, `blend`, or a `network` source; the bench lists the fields nobody read and stops at an unknown operation, painting nothing. A deep link `#tool=…&at=x,y` puts the cursor at a point so a readout can be quoted; `--dump-dom` prints the panel (read it with an HTML parser; the text is nested).
- The two attack-2 records are fixtures; edit `pickup`, read `surface.initial` instead of `state.surface`, move C, replace BD, add `CA-copy`.
- The next constructions the bench cannot run yet: a program that paints twice from one revision (the two hosts will disagree, by design of the GPU host); a tangent contact; a partial overlap; a group mask on a soft edge; a replay from a checkpoint.
