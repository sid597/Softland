# render-engine-gather — the evidence trail under `render-engine-map.{md,html}` (2026-08-23)

These are the read-only gatherer digests the render-engine map was composed from. They are
**receipts, not law**: every line is a FACT / SOURCE / EXTRACTION / UNCERTAINTY row with a
file:line anchor into `src/app/client/**` as of commit `e48abeb` (the tree the map read). If
a digest and the code disagree, the code wins and the map is wrong.

How they were produced: one collection wave, six bounded read-only agents, each scoped to
one piece of AREA C of `kept-code-map.md`, skeleton-first (`grep -n "^(def"`), scoped
windows, ≤8K tokens per tool call, no whole-file reads (two files under 13K read in two
windows). The composing session spot-checked the decision-changing facts against source:
the ten family ids (`scene_tape.cljc:14-26`), the two roads in `draw-frame!`
(`renderer.cljs:3678, 3918-3920`), the family-registry row shape (`renderer.cljs:3297-3341`),
the region-as-one-tape-row docstring (`region3d_scene.cljc:1235-1238`), the vertex-shader
composition (`renderer.cljs:206-221`), the store shape (`scene_store.cljc:16-26`).

| file | piece of the map | lane |
|---|---|---|
| `webgpu.md` | G · WebGPU core — renderer · compositor · buffer pool · budget | Opus-class |
| `scene.md` | C · Scene values + B · Scene runtime | Opus-class |
| `frame.md` | E · Frame computation (the eight `frame_*` files) | Opus-class |
| `families.md` | F · Render families (material + GPU halves, the plug-in contract, Region3D) | Opus-class |
| `shaper-verifier.md` | D · The shaper + V · The verifier (+ the `.mjs` runner and fences) | Opus-class |
| `skeleton-requires.edn` | the requires graph of all 48 client files (`app.client.*` / `app.shared.*` edges only) | deterministic |
| `skeleton-def-counts.tsv` | ns · bytes · lines · #defs for all 48 | deterministic |
| `skeleton-ns-docs.md` | every ns docstring verbatim (where the code states its own rationale) | deterministic |
| `skeleton-section-banners.md` | comment banners per file (internal sections) | deterministic |
| `skeleton-verifier.txt` | the verifier's defn skeleton + golden/fence def census | deterministic |

Not landed (bulk): the per-namespace `defs-*.txt` skeletons (80 KB) and the raw call-site
census (232 KB); both are reproducible with the greps named in `skeleton-*` and the call-site
summary is in the map's §7.

Known corrections these digests made to the prior map: the tape registers **ten** families
(`msdf` and `slug` are two text roads), not nine; the verifier's "fences" are the standalone
`.mjs` source checks (the word appears zero times in `verifier.cljs`); the verifier never calls
`draw-frame!` (it composes `compile-frame-tape` + `execute-frame-entry!` itself).
