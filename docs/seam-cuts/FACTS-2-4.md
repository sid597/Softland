# Fact base: do image, region, text carry the pieces the path cleanup removed?

Collected 2026-09-02 from HEAD `cc8d980` (tree clean except `.claude/memory/*`).
Reader: this is raw ground for the "what next / parallel?" answer. Every FACT has a file:line.
Path's shape (the template) is `docs/seam-cuts/CONTRACT.md` §1 + §5.

## Sizes (lines)

| kind | files | lines |
|---|---|---|
| path (after cut) | frame 22 · material 230 · painter 226 · tessellation 440 | 918 |
| image | material 360 · painter 772 | 1132 |
| region3d | material 454 · on_plane 178 · on_plane_painter 330 · painter 1254 · scene 888 | 3104 |
| text | fonts 241 · glyph_pack 196 · layout 1642 · layout_oracle 601 · layout_planes 772 · painter 826 · shaped_line 253 · shaper 391 · shaper_oracle 209 | 5131 |
| engine | buffer_pool 326 · color 49 · compositor 762 · device 271 · grammar 115 · leases 175 · limits 43 · placement 281 · rungs 100 | 2122 |
| verifier | core 2926 · shaper_border_probe 503 | 3429 |

## Per-piece census (the eleven removals of CONTRACT §1, per kind)

### GPU slot on the op (`:container-idx` where a container id belongs)
- IMAGE: op minted with `:container-idx 0` — verifier/core.cljs:510-518; packer destructures `container-idx` — image/material.cljc:331-338; painter forwards it — image/painter.cljs:141. PRESENT.
- REGION: op carries BOTH `:container 0 :container-idx 0` — verifier/core.cljs:1750-1756; painter reads `:container-idx` into `:composite` — region3d/painter.cljs:964-980; byte packer reads it — region3d/painter.cljs:739,745. PRESENT.
- TEXT: `container-idx` on the STYLE map — text/glyph_pack.cljs:137-141; and on the text map — text/painter.cljs:605; written to the instance at :686; draw batching partitions by `[:clip :container]` — :770-791. PRESENT.
- Slot resolution through `placement/effective`: exactly two callers repo-wide — verifier/core.cljs:1485 (path road) and test/app/client/path/frame_test.clj:36. No image/region/text caller. The only slot resolver is path/frame.cljc `slot`.

### Structural frame gate
- IMAGE: `prepare-key {:images images :resources @(:!resources …)}` compared with `=` — image/painter.cljs:688-693; atoms `:!last-images :!prepared-images :!last-prepare-key` seeded at :445-447; `:!last-images` reset at :700, never read. PRESENT (whole-map structural equality, no revision).
- REGION: `:!last-composite-key` and `:!last-regions` allocated at region3d/painter.cljs:454-455 and NEVER read or written anywhere else in src/ or test/ (repo grep). Reuse is per-row structural `(if (= row old) old row)` — :964-980. DEAD ATOMS + structural reuse, no frame key.
- TEXT: NO gate. `update-text-data` packs and `.writeBuffer`s unconditionally every call — text/painter.cljs:734-758; only a `:line-offsets` diff bumps `:!shape-rev`. No `!last-*` atom in the file (atoms: :361, :437 `:!shape-rev`; :490 `!text-layout-fallbacks`; :565 local). ABSENT — building one is new behavior, not a removal. UNCERTAINTY: a gate could live in a caller; callers not traced.

### Receipt ledger inside the painter
- IMAGE: `:!receipt` atom + `publish-image-receipt!` + `record-image-receipt!` + a GLOBAL `js/globalThis "__softland_image_ingress_receipt"` — image/painter.cljs:283-306, 448-450, 585-586, 665-708; entangled with device-lost/replacement lifecycle at :617-637. PRESENT, larger than path's.
- REGION: `:!receipt` in on_plane_painter — region3d/on_plane_painter.cljs:122, 286-288, 318, `placement-receipt` :321; scene.cljc:649 `:receipt {:full-rebuilds 1 …}`. PRESENT.
- TEXT: text/layout_oracle.cljc:578 `:receipts {:input-hash …}`; `!text-layout-fallbacks` global atom + `text-layout-fallback-report` — text/painter.cljs:490-496. MINOR.

### Private copy of the color law
- IMAGE: own WGSL `srgb_channel_to_linear`, byte-identical body to device's — image/painter.cljs:82-86 vs engine/device.cljs:16-20; own `image-color-mode-declaration` (:21) and `configure-image-color-shader` (:24-26) instead of `device/configure-scene-color-shader`; does NOT concatenate `device/scene-color-wgsl`; shares only `device/scene-color-blend` (:188). PRESENT (same as path's removed copies). Second linear route `create-linear-image-variant` :736 unread.
- REGION: CPU-side `srgb-channel->linear` — region3d/on_plane.cljc:119-131, only caller `linear-premultiplied` below it. engine/color.cljc (49 lines, read whole) holds NO channel conversion: only two color-mode data maps, `scene-color-seam`, and the `scene-color` selector; the transfer is declared as data (:22-31), never computed. The only executable law is device's WGSL. → the CPU side of "one color law" has NO home today.
- TEXT: already uses the shared device law (CONTRACT §1 states it; not re-verified here).

### Hand-written validators (imperative throws where a declared grammar belongs)
- IMAGE: `validate-source!` :38, `validate-material!` :107, `legal-source-tags`/`legal-alpha-associations` :16-17 with when-not/throw — image/material.cljc; no `engine.grammar` require. PRESENT.
- REGION: `validate-tagged-color!` :99, `validate-parent-graph!` :370, `validate-region!` :423, `legal-*` sets :16-20 — region3d/material.cljc; called from scene.cljc:137,624 and painter.cljs:860. PRESENT, much larger grammar (object kinds, primitives, lights, cameras, display modes, parent graph, quaternions).
- TEXT: none. Only the inline legal-zoom band `(<= 0.01 zoom 1000)` at text/layout.cljc:1046-1063, DUPLICATED in text/layout_oracle.cljc:535-558. Boolean predicates exist (`within-span-bound?` :663, `retained-rich-map?` :660, `oracle-match?` :1142, `within-work-bound?` :1200) but refuse nothing.
- `engine/grammar.cljc` has exactly ONE consumer today: path/material.cljc:9,26.

### Own `finite-number?`
- REGION: region3d/material.cljc:80 (public), used :88-406. PRESENT. CONTRACT §1 already routes: "region3d and placement carry their own; atoms 2–4 switch them".
- ENGINE: engine/placement.cljc:29 (private), used only by `validate-affine` :31-39.
- IMAGE, TEXT: none.

### Process-local admission stamp (Clojure metadata)
- IMAGE: the "admitted chain" is a DATA key `:image/ingress-receipt` compared with `=` to a namespace constant — image/material.cljc:44, 66-70. Not metadata; survives `pr-str`. Different from path's dead stamp. Whether a chain declaration belongs ON a source row is a cut question.
- REGION, TEXT: none found (`admit`/`stamp` grep: engine/rungs.cljc `:admitted?` is a budget decision, unrelated; text/layout.cljc:322-333 `stamp` is an address stamp, unrelated).

### Fixtures in src / instruments in src
- IMAGE, REGION, TEXT: none (`example-`, `roundtrip` grep empty). Verifier fixtures live in verifier/core.cljs (image-fixtures :48; region3d-seam-fixture :1758; flat-road-lines :2556).

### Kind-specific extra questions (not in path's template)
- IMAGE: atlas / mip / texture residency + device-lost lifecycle (painter :191-280, :617-637). CONTRACT §0 rules atlas placements are never rows → floor-private cache. The receipt ledger and the lifecycle state are interleaved; the cut must sort them.
- REGION: scene.cljc:140-701 `effective-transforms` is a 4x4 matrix composer, a DIFFERENT concept from `placement/effective`; must not be conflated. Scene evaluation passes are rows per §0 (pick, shadow space read them).
- TEXT: layout (1642) + planes (772) + oracle (601) + shaped_line (253) + shaper (391) + shaper_oracle (209) are derivations; §0 rules layout passes are rows (caret, selection, hit, on-plane read them). Text sits under the OPEN shaping-correction package (board next-prompt.md:491, "SEAM stays frozen/unwidened"). Text's fill is already GPU (Slug) — atom B copies text's way, it does not reshape text.

## Shared surface — files every parallel build would touch

| file | why touched | anchor |
|---|---|---|
| src/app/client/verifier/core.cljs (2926) | every kind's road lives here; image road :510-1096 (+ :1031 entry), path :1097-1520, region :1681-2547 (entry :2418), text flat road :2549-2747, dispatch :2748-2900 | one file, three sessions |
| test/app/test_runner.clj | `pure-namespaces` fail-closed inventory :31-52 | one-line adds per kind |
| test/render_engine/run_verifier.mjs | `sourceInputs` 18-path hashed manifest :275-294; MISSING today: image/material.cljc, region3d/on_plane.cljc, region3d/material.cljc, engine/color.cljc | every cut adds paths |
| src/app/client/engine/placement.cljc | private `finite-number?` :29; `effective` :193 has 2 callers; no slot resolver here (it is path/frame.cljc) | slot must move here or be duplicated per kind |
| src/app/client/engine/grammar.cljc (115) | one consumer; region's grammar may need engine growth | shared |
| src/app/client/engine/color.cljc (49) / device.cljs | the CPU-side color law has no home | shared |

Git: one working tree, one branch, `main`. Memory `feedback-parallel-sessions-shared-branch-git` carries two corpses (2026-07-21, 2026-08-23) of `git commit -- <shared file>` sweeping a sibling's uncommitted hunks; exact-path staging does not protect inside a file. Three sessions in verifier/core.cljs is that hazard three ways.

## Law read primary for this answer
- decisions.md:98-100 corner 4: "Parallelism buys coverage, never confidence. Two lanes on two atoms is throughput; two lanes on one question is forbidden."
- decisions.md:105-108 corner 6: "never a third consecutive dark atom."
- decisions.md:446-448: "The clock is Sid's ruling latency + activation cadence — never build throughput."
- decisions.md:438-444 delivery menu: "behavior-identical direct cuts (the space-package pattern)" · "worktree spikes whose findings return as contracts, never merges".
- next-prompt.md:242 Sid 2026-07-26: "build it out seperate … use concurrently … merge when chunks done".
- CONTRACT §2: "Atoms 2–4 repeat this cut per kind; the one color grammar and the one placement grammar across kinds land there."
- memory commits-one-branch-docs-local: commits on main; worktree branches never commit targets; push/merge Sid's alone.

---

# The cut session's starter (paste-able, Fable lane, fresh context)

Landed at Sid's word, 2026-09-02: "yes move them please". The position inside it is Fable's prior from the
2026-09-02 chat, not a ruling; the cut falsifies it against code.

```
Preflight, before the first prompt: permission mode, remote-control and MCP set now; never mid-session.

Cut the contract for the same cleanup on image, region and text, one document, docs/seam-cuts/CONTRACT-2-4.md.
The path cleanup is the template: docs/seam-cuts/CONTRACT.md §0, §1, §5 and §11 are its shape.
Sid, 2026-09-02: "i don't want to keeping coming back to claude and do things linearly iffffff they
are not needed" and "i don't like these terms at all" (atom, seam cut, waist) — plain words, or his.
Position already taken, in chat, Fable 2026-09-02: one shared step first (slot resolver into engine,
one home for the CPU color law, verifier split by kind with core the only entry, manifest completed),
then image and region build in parallel; text's cut is small now and names the layout row boundary
as its own piece. Falsify that position against code; it is a prior, not a ruling.

Boot, by seam, sizes first (about 100KB primary; the rest by bounded gatherer, never whole):
  docs/seam-cuts/CONTRACT.md                                  25KB  whole
  docs/seam-cuts/FACTS-2-4.md                                 12KB  whole; raw ground, code wins
  src/app/client/path/frame.cljc + path/material.cljc         10KB  whole, the template shape
  src/app/client/engine/grammar.cljc + engine/color.cljc       6KB  whole
  src/app/client/image/material.cljc                          14KB  whole
  src/app/client/image/painter.cljs                           37KB  lines 15-30, 75-110, 155-195, 280-310, 440-455, 610-640, 685-710
  src/app/client/region3d/painter.cljs                        60KB  lines 440-470, 735-750, 955-985
  src/app/client/region3d/on_plane.cljc                        8KB  lines 110-135
  src/app/client/text/glyph_pack.cljs                          8KB  lines 130-145
  src/app/client/text/painter.cljs                            40KB  lines 600-610, 734-760
  test/render_engine/run_verifier.mjs                         15KB  lines 275-294
  test/app/test_runner.clj                                    25KB  lines 15-52
Law: CLAUDE.md, .claude/skills/work-package/SKILL.md, decisions.md "The corners" and "The render seam".
```
