# IMAGE-ATOM contract validation R2 (fresh, default-fail)

**Session:** fresh-context, default-FAIL round R2 over the RECUT
`IMAGE-ATOM-CONTRACT.md` (2026-08-05). No loyalty to the author OR to the R1
validator — both are verified below. Every finding carries a `file:line`
trace.

**Method note (investigation fence).** Everything here except one item is a
STRUCTURE claim read from source (path exists / field carried / value
hardcoded / arity). I ran no verifier, no suite, no fence, no compile. The
ONE executed receipt is the JVM ICC probe (duty 2 bullet 6): a real JDK
21.0.11 program was compiled, run, and its output PNGs chunk-parsed and
cross-validated with ImageMagick + PIL. It is labelled RECEIPT. Two claims
that rest on general graphics/WebGPU knowledge rather than this repo are
labelled INFERENCE.

**R1 audit note.** R1's traces re-verified TRUE at every point I re-checked,
with two exceptions recorded in ADVISORY A3: `assert-tier-partition!` does
not exist (the function at R1's cited `test_runner.clj:234-251` is
`assert-inventory!`, :231-255), and `expected-divergence?` has no definition
in `verifier.cljs` (only the keyword `:expected-divergence?` at :553).
Neither error propagated into contract text.

---

## Verdict: FAIL

Five recut-class defects. Two are R1 findings the ledger declared consumed or
dismissed but which survive in executable substance (N4, N5). Three are NEW,
introduced by the recut itself (N1, N2, N3).

The recut genuinely fixed the worst of R1: the per-instance store-entry design
is gone and the pool-lane pattern it replaced it with **does compose** — I
traced it and §7's structural claim is TRUE (see "Verified clean" below). F2
is properly dissolved. F3's fork is closed. M1/M2/M5/M7 are consumed cleanly,
and M11's road is real (with a correction).

What fails is the second layer: **the recut's new design has no executable
receipt at three of its load-bearing joints**, and two R1 findings the ledger
waved to "the phase artifact as implementation notes" are §-level after all —
one of them (N5) can block the entire bank-append road mid-phase.

---

## R1 consumption table

| R1 finding | Status | Evidence |
|---|---|---|
| **F1** per-instance store entries corrupt GPU payload; "count plumbing if needed" | **CONSUMED in design · UNCONSUMED in receipt** | §7 refuses per-instance store entries and names the payload derivation a "NAMED deliverable" (§4 + §7) — the root cause is gone. But G8 places that deliverable's receipt in the **JVM** tier, and `scene_runtime.cljs` is `.cljs` (`<store-frame` :382 uses `m/latest`/`m/watch`). §12 MAY CREATE names only `.clj` test namespaces. → **N1** |
| **F2** `slot-entry` fan-out unnamed; maintained ≡ oracle impossible | **CONSUMED** | §7: "`slot-entry` keeps its one entry per slot"; G8 re-asserts. Verified: with no image entries in the store, all five `slot-entry` call sites (`scene_store.cljc:165,169,182,263,283`) and the oracle at `:254` are untouched, and equivalence is structurally preserved. |
| **F3** no pick dispatch for `:geometry :image-quad`; O6-violating reading | **CONSUMED in design · repair partially unconsumed** | §5 declares the existing route and REFUSES a central `:geometry` branch. The route claim traces TRUE (see "Verified clean" #2). But R1's named repair — a fence forbid on `case`/`cond` in the extracted `pick` form — is absent; §5 says only "the fence's spirit extends to pick", and `verify_scene_tape_fence.mjs:93-94` forbids only `sort-by` and the legacy layer lambda. → **N7** |
| **F4** strict C4 unsatisfiable under direct-present; candidate encode absent from tree | **CONSUMED for legacy · NEW DEFECT introduced for candidate** | The legacy declared-zero-transfer row is exactly R1's repair option 1 — correct and traced (`scene_tape.cljc:19-29`). But the recut added a new obligation ("the candidate-only presentation encode becomes code-real in this wave, behind the existing default-OFF seam") whose declared location does not exist inside §12. → **N2** |
| **F5** G4 DECISIVE-PASS vacuous at zero decisive pixels | **HALF CONSUMED** | Floors `boundary-pixel-count > 0` / `decisive-count > 0` are now named in §5, §8 and G4 — R1's first half, consumed. R1's **second half** — "must require the Cg-isolation fixture to be an opaque white reference paint so channel 0 is coverage" — is still absent from G4, verbatim as R1 predicted ("§5 half-covers this but G4 does not require it"). And the floors have no failing surface. → **N4** |
| **M1** fixture bytes 404 on the synthetic origin | **CONSUMED** | §8 fixture-delivery bullet names `serveSyntheticOrigin` and the `images/*` extension. Verified: `run_verifier.mjs:102-147` serves exactly `/`, `/js/main.js`, `/fonts/*` (with the containment check at `:131` to copy), 404 at `:142`. File is MAY EDIT. |
| **M2** single-system capture + hardcoded clear | **CONSUMED** | §8 capture-path bullet: own routine for image cases, existing path frozen, "editing their path is an S4". Verified `render-system-bytes!` `verifier.cljs:82`, one system, clear hardcoded. |
| **M3** "byte-identical, asserted by the run itself" stops being an assertion once rows are appended | **UNCONSUMED** | §8 still reads verbatim "The 21 existing PNGs: byte-identical, **asserted by the run itself**" — R1's exact target text, unchanged. The substituted receipt ("G2 running green-on-21 with the appended rows present") has no code referent. → **N3** |
| **M4** `--update-goldens` wholesale; `updateAuthorized` lacks a golden guard | **PARTIALLY CONSUMED** | §8 forbids wholesale update and names a scoped `--append-image-goldens` road — implementable (see duty-2 findings). Unconsumed: the new flag is given no authorization predicate, and "writes ONLY the new image rows" mismatches the code shape (the manifest is serialized wholesale from the run at `:390-393`). → **N8** |
| **M5** fence self-test is a vacuous hardcoded literal | **CONSUMED** | T12 recut: seed into "a copy of the ACTUAL extracted `draw-frame`/executor slice text". Verified implementable: `drawFrame` is already extracted at fence `:47`; `/\.draw\s+pass/` (`:79`) still matches an injected seed. One mechanical note: `forbid` (`:70-72`) pushes to `failures` rather than returning a boolean, so the self-test needs a local predicate or a tiny refactor — inside the allowlisted file. |
| **M6** four gates name no command | **UNCONSUMED** | G9/G10/G11 unchanged. In my scoped set: G11 requires "shaping fences untouched and **green**" and `verify_shaping_correction_fence.mjs` appears in NO npm script (`package.json` scripts are exactly `build`, `verify:text-layout`, `verify:render-engine`); G8/G10 name no runner entrypoint and `test_runner.clj` has **no `-main`** (entrypoints are `clj -X:test full` / `fast` / `shard` / `registered-flake`, at `:637`/`:375`/`:444`/`:477`). → **N9** |
| **M7** T10 fence script cannot be wired inside the allowlist | **CONSUMED** | T10 moves the check INSIDE `verify_scene_tape_fence.mjs`; §12's new-script line removed. Verified: the fence IS wired (`package.json:9`), and the check fits the existing idiom exactly — `forbid` already forbids bare identifiers inside extracted slices (`/frame-idx/` `:87`, `/sort-by/` `:93`), and `findForm` reaches any `defn-`, so `findForm(renderer, "<image-producer>")` works with no helper change. |
| **M8** modes-vs-cases fork invalidates G2's own "divergence sentinels 7/7" | **UNCONSUMED** (ledger dismissed it as non-§-level; it is §-level) | §8 still says "at minimum TWO fixtures … across the SEVEN standing zoom regimes (zoom-cases :31-38): 14+ new goldens", never "additional modes; no new case rows". R1's repair sentence is absent. → **N5** |
| **M9** T11 misses `replace-texture!` | **UNCONSUMED** | Verified `replace-texture!` at `gpu_budget.cljs:194` with the same level-0 pricing. T11, §4 and §11 still name only `register-texture!` :152, while §3 makes source-byte replacement a new material revision. → **N10** |
| **M10** shared descriptor helpers hardcode values §5 demands | **UNCONSUMED** | §5 still demands `:export-projections` "says exactly that" and a 3-band regime partition; no sentence about parameterising `registration` (`scene_tape.cljc:244`) / `regime` (`:67`) **without changing the five existing families' emitted values**, which matters because `family-contracts` (`:285`) feeds `default-family-registry` (`:385`), validated on every store write. → **N11** |
| **M11** no ICC/PNG road for the profiled fixture | **CONSUMED, with a receipt-level correction** | §8 names the JVM `javax.imageio` + `ICC_Profile` generator. **RECEIPT (executed):** the road works — but only via explicit `IIOMetadata`, and the contract's literal wording describes the road that silently fails. → **N12** |

---

## New findings

### N1 · FAIL — the recut's flagship deliverable (F1's repair) has its gate in a tier that cannot execute it

**Claim under test.** G8: "**store/op lane (JVM — recut per §7)** — … the scene_runtime payload derivation + `:ops-count-by-vi`/`:order-by-vi` carry the `:images` key without disturbing rect/shadow/text counts".

**Trace.** The payload derivation is `<store-frame` at
`/mnt/data/projects/Softland/src/app/client/workspace/scene_runtime.cljs:382-415`
— a **`.cljs`** file, and the derivation is wrapped in
`(m/latest (fn [store] …) (m/watch !scene-store))` (`:389`, `:415`). A JVM
`clojure.test` namespace cannot load it. §12 MAY CREATE names only
`test/app/client/substrate/image_material_test.clj` and
`image_citizenship_test.clj` — both `.clj`. §12 MAY EDIT does not permit
creating a `.cljc`, and there is no npm script for a cljs test runner
(`package.json` scripts: `build`, `verify:text-layout`,
`verify:render-engine` only; `karma-cljs-test` is a devDep with no script).

The `.cljc` legs of G8 ARE executable — `tree->images`, `flatten-ops`
(`scene_store.cljc:46`), `stamp-ops-container` (`:55`) are all `.cljc`. It is
precisely the leg R1 F1 demanded be promoted from "plumbing if needed" to a
NAMED deliverable that has no executable vehicle.

**The same gap hits two more receipts.** §7: "Entry ids are unique per (vi,
kind) — the frame twin-check (`compile-frame-tape` vs `!frame-arrangement`)
covers them." And G5: "O1 shuffle-determinism … with image entries present.
JVM where possible, verifier for pixels." Both are renderer-arrangement
claims. `produce-frame-entries` (`renderer.cljs:2209`),
`update-frame-arrangement` (`:2215`) and `frame-tape-twin-check!` (`:2243`)
are private `defn-`s in a `.cljs` file, and the twin-check is gated on a
browser global (`:2244`). Worse, in the product path the image producer will
be called with a `frame` map that has no image `pool-info` and no image
`base-offset` — both would have to be supplied at
`runtime/render.cljs:717-751`, which is **§12 MUST NOT TOUCH** — so
`store-pool-entries`' `(and pool-info (pos? instance-count))` guard
(`renderer.cljs:1979`) makes the producer emit **zero** entries. The
twin-check then agrees trivially and "covers" nothing.

**Exact repair.** (a) Move the scene_runtime-derivation leg of G8 out of the
JVM tier and name its vehicle: the cleanest in-allowlist home is
`verifier.cljs` (allowlisted, `.cljs`, already driven in-browser by
`run_verifier.mjs`) asserting the derivation and the arrangement over a
synthetic `frame` map carrying non-empty `:store-frame {:images …
:ops-count-by-vi {vi {:images n}}}` plus a stub `:image-pool-info`; assert
entry-id uniqueness per (vi, kind) AND that rect/shadow `first-instance`
values are byte-unchanged. (b) Alternatively factor the pure derivation into
a new `.cljc` and add it to §12 MAY CREATE. (c) Either way, state explicitly
that the PRODUCT path emits zero image entries this wave because
`runtime/render.cljs` is MUST-NOT-TOUCH, and say so in the receipt rather
than letting §7's twin-check sentence imply coverage.

### N2 · FAIL — the candidate presentation encode has no in-allowlist home "behind the default-OFF seam"; the only shader insertion point collides with the fence

**Claim under test.** §5: "**The candidate-only presentation encode becomes
code-real in this wave**, behind the existing default-OFF seam — zero
behavior change when disabled, proven by G2's byte-identity over the 21
goldens." G6 makes it a gate condition: "exactly one presentation encode (the
encode made code-real behind the default-OFF seam)".

**Trace.** Where the frame reaches the surface:

- The only render-target → swapchain path is a **raw byte copy**:
  `renderer.cljs:2379-2383` `.copyTextureToTexture`. No pipeline, no bind
  group, no WGSL — **no fragment stage in which an encode can live.**
- The render target is created with
  `:usage (bit-or RENDER_ATTACHMENT COPY_SRC)` (`renderer.cljs:1285-1286`) —
  **no `TEXTURE_BINDING`**, so it cannot be sampled by a blit shader as
  created.
- No linear→output encode exists anywhere in `src/`. Exhaustive grep for
  `linear_to_srgb|to_srgb|srgb_encode|1.0/2.4|0.4166|1.055|0.0031308|12.92`
  returns exactly two hits, both in the sRGB→linear **decode**
  (`renderer.cljs:13-14`). R1's F4 trace re-verified TRUE.
- The product's colour attachment IS the swapchain view
  (`renderer.cljs:1328`) because `use-persistent-render-target?` is `false`
  (`runtime/render.cljs:18`) — and the canvas format is chosen and configured
  in **`electric_flow.cljc:519` and `:801-804`** (`getPreferredCanvasFormat`,
  no `-srgb`, `alphaMode "premultiplied"`). `electric_flow.cljc` and
  `runtime/render.cljs` are both **§12 MUST NOT TOUCH**.
- `scene-color-enabled?` has no producer: the sole `create-editor-state`
  caller (`electric_flow.cljc:742`) passes a map lacking the key, so the
  `false` default at `renderer.cljs:1334` always wins.

So the two roads:

1. **Shader blit at present.** Requires RT usage + a new present pipeline +
   a full-screen draw inside `draw-frame!`. The fence forbids
   `/\.setPipeline\s+pass/`, `/\.setBindGroup\s+pass/`,
   `/\.setVertexBuffer\s+pass/`, `/\.draw\s+pass/` inside the extracted
   `draw-frame!` form (`verify_scene_tape_fence.mjs:76-79`). A present blit
   either **trips the fence** or **defeats it by renaming the pass variable**
   — which is T12's dilution failure in a new costume.
2. **`-srgb` colour attachment** (hardware encode on store). This IS
   implementable inside the allowlist — `create-render-target` takes
   `fformat` positionally (`renderer.cljs:1284`) and each `init-*-system`
   builds `:targets [{:format fformat …}]` (`:711` and siblings), all in
   renderer.cljs, and `verifier.cljs:20` owns its own `color-format`. But
   this makes the encode a **format declaration exercised only inside a
   verifier/test fixture** — it is *not* reachable "behind the default-OFF
   seam" in the product, because the product has no intermediate target at
   all and its attachment format lives in a MUST-NOT-TOUCH file.

The contract's own sentence — "the transfer road (`*-srgb` hardware sample vs
`*unorm` + shader transfer, W1 §8.2.5) is the implementer's DECLARED choice"
— leaves road 2 open, so this is repairable cheaply. But as written, §5/G6
assert a product-path property that §12 forbids reaching, and the natural
reading sends the implementer at road 1 and into the fence. That is a
mid-phase S2/S5 scheduled by the contract — the exact failure class R1's F4
diagnosed, relocated from the legacy regime to the candidate one.

**One further trap the contract does not name.** A third road exists and is
seductive: put the encode at the end of `scene_color()`
(`renderer.cljs:16-25`) under the mode flag. That would encode **before**
fixed-function blending, and the candidate blend is `[:one
:one-minus-src-alpha]` in linear space (`scene_tape.cljc:40-41`) — so the
composite would blend in encoded space while C4's "exactly one presentation
encode" sentinel counts one and passes. A physically wrong implementation
that satisfies the gate.

**Exact repair.** In §5's candidate bullet and G6: (i) declare the encode's
location as the **colour-attachment format derivation** (`-srgb` variant
selected when `(:enabled? scene-color)`), naming
`create-render-target`/`init-*-system` target formats and `verifier.cljs`'s
`color-format` as the edit sites; (ii) state plainly that the product boot
path has NO intermediate target and its canvas format is owned by a
MUST-NOT-TOUCH file, so the candidate encode is **fixture-reachable only** in
this wave, and rewrite "code-real behind the default-OFF seam" to say that;
(iii) add a trap: an encode inside `scene_color()` (pre-blend) is a C-receipt
failure even though it satisfies a naive one-encode count.

### N3 · FAIL — G2's zero-regression receipt rests on evidence the run does not produce (M3 unconsumed)

**Claim under test.** §8: "The 21 existing PNGs: byte-identical, **asserted by
the run itself**." §8 bank-append: "receipted by **G2 running green-on-21**
with the appended rows present." G2: "existing 21 goldens byte-identical".
And N2 above leans on the same thing: "zero behavior change when disabled,
**proven by G2's byte-identity over the 21 goldens**."

**Trace.** The run computes golden status and then discards it from every
observable surface:

- `imagePass` (`run_verifier.mjs:440-445`) requires
  `imageComparison.length === (expectedManifest?.images.length || 0)` **plus**
  every row's `rawMatch && pngManifestMatch && goldenFileMatch`.
- The classification cascade (`:447-458`) is a single `if / else if` chain.
  `!parityPass` fires at **`:452`** (permanently, by the preserved MSDF
  counterexample), so `:455-458` — including `!imagePass` — are **never
  evaluated**.
- The printed summary (`:503-522`) has **twelve** fields and **no golden
  field**: `pass, classification, receipt, images, deterministic,
  q8AffineTransport, q5AffineRasterBoundary, candidateParity,
  productBoundsDivergenceSentinels, updateRequested, updateAuthorized,
  environmentFingerprint`. `images: currentManifest.images.length` (`:509`)
  is a bare row count of THIS run, not a match count.
- `imagePass` reaches exactly one durable place: `receipt.json` at
  `goldenComparison.pass` (`:493-496`), with per-row
  `rawMatch`/`pngManifestMatch`/`goldenFileMatch` at `:431-437`.

So "asserted by the run itself" is false, and "green-on-21" has no referent:
`goldenComparison.pass` is the whole-set predicate including the length
guard, so with 14+ appended rows and an updated 35-row bank it reports
green-on-35, and with a non-updated bank it reports false on length. There is
no per-subset signal anywhere.

**Exact repair** (this is R1's M3 repair, still owed, now also load-bearing
for N2): G2 and §8's Act-0 baseline must name the exact evidence path —
`target/render-verifier/receipt.json` → `goldenComparison.rows`, filtered to
the 21 known filenames, all of `rawMatch`/`pngManifestMatch`/`goldenFileMatch`
true — and state that the GATE SESSION asserts it, because the run's exit code
cannot distinguish golden drift from the standing parity red. Replace "asserted
by the run itself" and "G2 running green-on-21" with that path. Separately
note that the length guard at `run_verifier.mjs:442`, not the cascade, is the
actual appended-row obstacle — §8's "the cascade tolerates appended rows"
names the wrong mechanism.

### N4 · FAIL — G4's floors sit on top of an unasserted coverage channel, so a zero-information "decisive pass" is still reachable (F5 half-consumed)

**Claim under test.** G4: "quad-boundary probe DECISIVE-PASS across the seven
regimes **with floors: `boundary-pixel-count > 0` AND `decisive-count > 0`
per regime, both reported in the receipt** (a zero-sample pass is a FAIL —
R1 F5)".

**Trace — two independent holes.**

1. **The channel assumption is unasserted.** `pixel-red` (`verifier.cljs:147`)
   reads **channel 0** and the parity machinery treats it as coverage;
   `boundary-pixels` (`:232`) keeps pixels with `8 < coverage < 247`. For a
   coloured or textured image, channel 0 is image colour, not coverage — so
   `boundary-pixel-count` and `decisive-count` would both be comfortably `> 0`
   **while comparing garbage**. The floors are satisfied; the probe is still
   unfalsifiable. R1 named this exact second half — "must require the
   Cg-isolation fixture to be an opaque white reference paint so channel 0 is
   coverage" — and predicted its non-consumption ("§5 half-covers this but G4
   does not require it"). §5 still only says "Receipts isolate Cg with opaque
   reference paint before testing composed Cv"; **G4 imposes no such
   requirement**, and G3's two named fixtures are "an opaque sRGB image" and
   "an alpha PNG" — neither declared white.
2. **The floors have no failing surface.** Row pass comes from
   `verifier.cljs:359 :pass? (zero? (count mismatches))` and
   `run_verifier.mjs:335 parityPass = parity.every(row => row.pass)`.
   `parityPass` is already false (MSDF), so folding the floor into `row.pass`
   changes neither `classification` (`:452`) nor the exit code. "Reported in
   the receipt" is a run-and-print.

**Exact repair.** G4 must (i) require the Cg-isolation fixture to be an
**opaque white reference paint** (or name the coverage channel explicitly and
require the probe to read it), stated as a gate condition not a §5 aside; and
(ii) name the evidence path and its asserting owner —
`receipt.json` → `candidatePickParity.rows` filtered to image rows, each with
`boundaryPixelCount > 0` and `decisiveCount > 0`, asserted by the gate
session — since the run's exit code cannot express the floor.

### N5 · FAIL — the modes-vs-cases fork is still open, and one implementable reading blocks the entire bank-append road (M8 unconsumed)

**Claim under test.** §8: "at minimum TWO fixtures … across the SEVEN
standing zoom regimes (zoom-cases :31-38): 14+ new goldens appended to the
bank". G2 pins: "divergence sentinels 7/7".

**Trace.** `zoom-cases` (`verifier.cljs:31-38`) is seven rows; the bank's 21
rows are 7 caseIds × 3 modes (`sdf-rich-rect`, `msdf`, `slug`) — verified from
`manifest.json` (two rows sharing `caseId "legal-min-z0p01"` differing only by
`mode`, 21 entries total, 21 PNGs on disk). So:

- **Reading (i) — new MODES inside the existing seven cases.**
  `result.cases` stays 7, image rows 21 → 35, divergence rows stay 7. The
  "14+ = 2 × 7" arithmetic implies this reading.
- **Reading (ii) — new CASE rows.** `expectedDivergenceRows`
  (`run_verifier.mjs:227-233`) maps over **every** `result.cases` row and
  reads `renderCase.currentProductPickSentinel`;
  `divergencePass = divergences.every(row => row.expectedDivergence)`
  (`:336`). An image case would not reproduce the rounded-rect-corner
  divergence fact, so `divergencePass` goes **false** →
  `updateAuthorized = determinismPass && divergencePass && q8TransportPass &&
  q5AffineBoundaryPass` (`:353-354`) goes **false** → **no golden write of any
  kind is authorized** (`:382`), and the scoped `--append-image-goldens` road
  inherits the same hazard because the contract never states its
  authorization predicate (N8). G2's own pinned "divergence sentinels 7/7"
  becomes a self-contradiction, and the printed
  `productBoundsDivergenceSentinels` fraction (`:514`) moves.

Two physically implementable readings of a binding doc, one of which blocks
the wave's central deliverable, is S6 territory hit mid-phase. R1's repair
sentence is absent, and the ledger's blanket "none alters a §-level law" is
wrong here: this one can stop the bank.

**Exact repair.** §8 states verbatim: "image fixtures are **additional modes
within the existing seven `zoom-cases`; no new case rows**" — and adds the
before/after denominators to the pinned list, not deferred to the phase
artifact: image rows 21 → 35, determinism rows 21 → 35, parity-row
denominator 21 → its new value, divergence rows 7 → 7 (unchanged, which is
the whole point of the ruling).

---

### N6 · FAIL — §6's "batches split by texture binding" cannot be expressed by §7's chosen mechanism

**Claim under test.** §6: "**Bind groups/batching**: batches split by texture
binding (atlas page or dedicated texture); batching merges ONLY
order-contiguous compatible entries (Contract O batching law)". §7: "the
registered image producer mints `[:frame/store vi :images]` batch entries
(precedent `store-pool-entries` renderer.cljs:1967-1990) … plus
dedicated/atlas bind-group batches split order-contiguously."

**Trace.** `store-pool-entries` (`renderer.cljs:1967-1990`) takes **one**
`pool-info` for the whole lane and, per vi, emits
`(gpu-paint (:pipeline pool-info) (:bind-group pool-info) (:buffer pool-info)
instance-count offset)` (`:1982-1985`). `execute-gpu-batch!` (`:2157-2168`)
sets exactly **one** bind group at index 0 (`:2165`) and one vertex buffer
(`:2166`). The count it reads is `[:ops-count-by-vi vi count-key]` (`:1973`)
— **one `:images` number per vi, not per binding** — and the offset is a
single running accumulator (`:1970`, `:1989`).

So through the named precedent, every image in the lane shares one bind group
⇒ one texture or one atlas page. Splitting by texture binding requires
per-binding sub-runs *within* a vi, which neither existing pattern expresses:
the pool lane can't (one pool-info, one count per vi), and the text pattern
(`text-entries-for-family` `:2084-2113`, per-vi geo objects with their own
bind groups) is per-**vi**, not per-**binding**. Compounding it, the recut
says the `:images` payload rides "same shape as `:rects`/`:shadows`" — i.e. a
flat `mapcat` concat into one buffer (`scene_runtime.cljs:396-397`) — so
instances for different textures interleave in a single buffer, and drawing
them needs either per-instance texture indexing or a sort by binding, and a
sort breaks both the order-contiguity law and the offset arithmetic.

§6 and §7 do not reference each other on this. R1's A2 flagged the same
tension under the OLD §7 and it survived the recut with the mechanism changed
underneath it. Two implementable readings ⇒ S6 mid-phase.

**Exact repair.** §6 and §7 must jointly declare the day-one binding
strategy. Either (i) **ONE bind group for the whole image lane** — a single
atlas page plus a bounded texture-array for dedicated sources, indexed
per-instance — keeping `store-pool-entries`' single-pool arithmetic intact and
making "batches split by texture binding" a NAMED extension point; or (ii)
the image producer owns its **own** multi-pool loop (explicitly *not*
`store-pool-entries`), in which case §7 must name how per-binding
`first-instance` offsets are derived, declare that `:ops-count-by-vi` carries
per-binding `:images` counts, and give that arithmetic its own receipt.

---

## MINOR findings (enumerated in-place fixes)

**N7 · MINOR — F3's fence forbid is still missing; "the fence's spirit
extends to pick" is not a check.** §5 refuses a central `:geometry`-keyed
branch in `scene_store.cljc/pick`; the fence forbids only `/sort-by/` and
`/#\(-\s*\(:layer/` in the extracted `pick` form
(`verify_scene_tape_fence.mjs:93-94`). The mechanism to enforce it exists
verbatim one line above. *Repair:* add to T12/G11 —
`forbid("pick", pick, /\(case\s+/, "family/geometry dispatch in pick")` (and
`/\(cond\b/`), one line in an allowlisted file.

**N8 · MINOR — `--append-image-goldens` has no authorization predicate, and
"ONLY the new image rows" mismatches the code shape.** Flag parsing is one
line (`run_verifier.mjs:29 process.argv.includes`), so the flag is trivial;
PNG writes are per-file (`:387`) so scoping them is trivial. But
`manifest.json` is serialized **wholesale** from the current run
(`:390-393` over `currentManifest`, whose `images: imageRows(result)` at
`:329` is built entirely from this run's cases; the banked file is not read
until `:402`, after the write). So "writes ONLY the new image rows … +
merged manifest metadata" is two different roads. And `updateAuthorized`
(`:353-354`) still omits `imagePass`, so drifted existing shas can enter the
manifest silently. *Repair:* §8 states which manifest road (read-merge-splice,
or wholesale-with-unchanged-content) and states the new flag's authorization
predicate explicitly — at minimum `determinismPass && divergencePass &&
q8TransportPass && q5AffineBoundaryPass`, plus a guard that no existing
filename is written and that the 21 rows' shas are unchanged.

**N9 · MINOR — M6 unconsumed inside my scoped gates.** G11 requires the
shaping fence "green" and it is in **no** npm script; G8/G10 name no runner
entrypoint and `test_runner.clj` has no `-main` (entrypoints `full` `:637`,
`fast` `:375`, `shard` `:444`, `registered-flake` `:477`; `full-receipt-floor`
`:161`). *Repair:* put the invocation on each gate line —
`node test/render_engine/verify_shaping_correction_fence.mjs`,
`node test/render_engine/verify_text_layout_fence.mjs`,
`clj -X:test full`, and the cljs-warning capture command for the Act-0
baseline.

**N10 · MINOR — `replace-texture!` still unnamed (M9).** `gpu_budget.cljs:194`,
same level-0 pricing as `:152`, and §3 makes byte replacement a new material
revision. *Repair:* extend T11 and §11's `gpu_budget.cljs` row to
`replace-texture! :194`.

**N11 · MINOR — shared descriptor helpers still unaddressed (M10).**
*Repair:* §5 states that `registration` (`scene_tape.cljc:244`) and `regime`
(`:67`) are parameterised **without changing the five existing families'
emitted values**, because `family-contracts` (`:285`) feeds
`default-family-registry` (`:385`), which every store write validates against
(`scene_store.cljc:166-169`) and which `frame-contract-registry` reads
per-family (`renderer.cljs:2172`).

**N12 · MINOR (RECEIPT-backed) — the contract's literal ICC road produces an
UNPROFILED PNG, and the naive assertion passes on a corrupt chunk.** §8:
"minted by a COMMITTED deterministic JVM generator (Clojure + `javax.imageio`
+ `java.awt.color.ICC_Profile` for the profiled fixture)".

**RECEIPT (executed, JDK 21.0.11, `com.sun.imageio.plugins.png.PNGImageWriter`):**
- Plain `ImageIO.write(iccColorSpaceImage, "png", file)` → chunks
  `IHDR IDAT IEND`, **no `iCCP`**. Returns `true`, no warning; the profile is
  silently dropped. PIL reports `icc_profile = None`.
- Explicit road → chunks `IHDR iCCP IDAT IEND`, profile inflates to 488 B,
  cross-validated by ImageMagick (`Profile-icc: 488 bytes`) and PIL
  (`icc_profile bytes = 488`). `iCCP` **is** in the native
  `javax_imageio_png_1.0` element list (7th of 17), attributes `@profileName`
  (required) and `@compressionMethod` (enumeration `[deflate]`).
- Byte-deterministic across **three separate JVM invocations**: identical
  sha256 (`8e0358b9…` for LINEAR_RGB, `73a1fad5…` for GRAY).
- **Trap A:** the writer does **not** compress — `setUserObject` must receive
  already-deflated bytes. Passing raw bytes writes a well-formed-looking but
  corrupt chunk (`iCCP len=507`, undeflated); ImageMagick says "unknown
  compression method", PIL silently reports `None`. **A test asserting "iCCP
  chunk present" PASSES on this corrupt file.**
- **Trap B:** `ICC_Profile.getData()` is call-order dependent —
  `CS_LINEAR_RGB` differs by 3 bytes pre/post CMM activation (the `rTRC`/
  `gTRC`/`bTRC` tag-size low bytes, `16`→`14`); `CS_PYCC` **throws**
  `CMMException: LCMS error 13`.
- **Trap C:** ImageIO's PNG **reader** does not apply the embedded profile —
  a round-trip via `ImageIO.read` reports `isCS_sRGB = true`.

*Repair:* §8 names the working road — `ImageWriter` +
`getDefaultImageMetadata` + `mergeTree("javax_imageio_png_1.0", root)` with an
`iCCP` node carrying `profileName`, `compressionMethod "deflate"`, and a
**pre-deflated** user object — forbids the plain `ImageIO.write` road, pins
the profile bytes as a committed `.icc` (or pins `getData()`'s sha256 at one
fixed call point) to survive JDK/LCMS drift, and requires the fixture
assertion to **inflate the `iCCP` payload and compare its digest**, never
merely assert chunk presence.

**N13 · MINOR — `:order-by-vi` has no lane dimension; §7 and G8 assert a key
it cannot carry.** §7 and G8 both say the payload's "count/order maps
(`:ops-count-by-vi`/`:order-by-vi`) carry the `:images` key". Trace:
`:ops-count-by-vi` is `{vi {:rects n :shadows n :text-lines n}}`
(`scene_runtime.cljs:402-409`) — per-lane, so `:images` fits. `:order-by-vi`
is `{vi <order-map>}` (`:410-414`), value = the entry's whole `:order`
(`scene_store.cljc:244-248`) — **lane-agnostic, no lane keys at all**.
Adding an `:images` key there is incoherent and nothing would consume it.
*Repair:* say `:ops-count-by-vi` gains `:images`, and `:order-by-vi` is
unchanged (one order per vi, shared by all lanes).

**N14 · MINOR — the image lane's `:part-rank` and intra-slot paint order are
undeclared.** §7: entries carry "the slot's `:stack-path` and an image
`:part-rank`" — no value. Existing ranks are hand-assigned literals with
nothing enforcing uniqueness: shadow `0` (`renderer.cljs:2031`), rect `1`
(`:2047`), slot-text `2` (`:2110`); `part-rank` feeds `frame-order`
(`:1976-1977`). A collision with rect makes two entries share an order key.
More substantively, Contract O exists to make order fully declared, and
whether an image paints under or over its slot's rect background is a visible
product decision this contract never makes. *Repair:* declare the value and
the intra-slot order sentence (e.g. shadow → image → rect → text, rank 3
reserved), and have G5 assert rank uniqueness across lanes.

**N15 · MINOR — the named precedent's clip clamp would STRETCH a clipped
image.** §7: "`rect_tree.cljc` gains `tree->images` (precedent `tree->rects`
:198)". `tree->rects` **clamps** a partially-visible rect to the clip
intersection (`rect_tree.cljc:225-232`) with the comment "Radii degrade at
clamped corners (accepted)". Clamping an image quad's geometry without
adjusting its crop rect / UVs rescales the sampled region — the image
stretches instead of cropping. §2 says the family declares visibility "through
the existing shared clip reference only", but the clamp is a geometry bake at
flatten time, not a shader clip. No trap covers this. *Repair:* add a trap —
`tree->images` must carry the clip-clamp into the crop rect / UV insets, or
must not clamp at all; receipt it with a partially-clipped fixture at one
zoom.

**N16 · MINOR — the declared product-pick route returns an ADDRESS, so an
image node without `[:data :address]` is not resolvable.** G8: "`scene_store.cljc/pick`
resolves an image node topmost across overlapping slots by reverse maintained
order." Trace: `pick` (`scene_store.cljc:305-328`) calls `rt/hit-test`
(`:320`) then **requires** `deepest-addressed path` (`:321`, helper at
`:220-225`), returning nil if no node on the path carries `[:data :address]`
— and returning the deepest *addressed ancestor* otherwise. So the receipt
needs the image rt-node to carry an address; unstated. *Repair:* §5/§7 state
that an image rt-node carries `[:data :address]` (the `:owner
<semantic-instance-route>`), and G8 asserts the returned address is the
image's, not an ancestor's.

**N17 · MINOR — `:hit-slop {:radius <declared>}` is an unfilled placeholder,
and the pick path implements zero slop.** §5's pick descriptor literally
contains `<declared>`. `hit-test` is exact half-open AABB containment
(`rect_tree.cljc:372-373`) with no slop term, and the precedent family
declares `:hit-slop 0.0` (`scene_tape.cljc:120`, threaded at `:102`). Any
non-zero declared radius is a declared-but-unimplemented policy — the F3/F5
defect class. *Repair:* declare `0.0`, or name where slop is honoured.

**N18 · MINOR — G2's "zero behavior change when disabled" covers 3 of 5
families.** The 21 goldens are modes `sdf-rich-rect`, `msdf`, `slug` (verified
from `manifest.json` + the 21 filenames). Shadow and clip are **not** in the
bank, yet both thread `scene-color` (`renderer.cljs:1120`, `:1257`) and would
receive any format/blend derivation N2 introduces. *Repair:* either scope the
claim ("byte-identity over the three banked families") or add a shadow/clip
zero-diff leg.

**N19 · MINOR — G6's profiled-fixture leg rests on unverified browser
behaviour with no Act-0 probe and no fallback.** G6 requires "the profiled
fixture (embedded-profile PNG whose post-ingress pixels are pinned)". Whether
HeadlessChrome 150 / SwiftShader honours an embedded ICC profile through
`createImageBitmap` is a behaviour claim I did not test (R1 flagged the same
gap). *INFERENCE, flagged:* per the HTML spec `colorSpaceConversion` takes
only `"default"` / `"none"` — it cannot request a target space, so §3's
"declared conversion to sRGB" rides UA discretion. If the UA ignores the
profile, the leg is vacuous. *Repair:* add to Act 0 a pre-registered probe —
decode one profiled PNG in the verifier page, print the post-decode pixel,
compare against the non-identity expectation — and name the fallback (declare
the leg RED-with-receipt) if the UA does not convert.

---

## Advisory

**A1 — T10's single-form grep does not cover transitive helpers.** T10 "greps
the image producer slice". `forbid` over a `findForm` slice is the right and
existing idiom (`verify_scene_tape_fence.mjs:70-72`, `:87`, `:93`), and
`findForm` reaches any `defn-` so the producer is extractable with no helper
change — but a decode moved into a helper the producer calls would pass. Worth
naming the producer's permitted callee set, or forbidding
`createImageBitmap`/`await`/`.then` across the producer **and** its named
helpers.

**A2 — the fence's `registry` slice is coarser than G11 implies.** G11 wants
"the image id in both slices". The registry slice is `renderer.cljs`
2170→2236 (`indexOf` bounds at fence `:52-53`), which spans
`frame-family-registry`, `frame-contract-registry`, the `!frame-arrangement`
defonce, `produce-frame-entries` **and** all of `update-frame-arrangement`. The
id appearing anywhere in that 67-line span — including a comment — satisfies
the check. Pre-existing, not introduced by the recut.

**A3 — §11 locator drift and two inherited R1 errors.** §11 and §4 list init
systems as `:681/:866/:930/:1118/:1256`. Actual `init-.*-system` matches are
**681** (rect), **866** (msdf), **930** (slug), **995** (`init-text-system`,
omitted), **1118** (shadow); `:1256` is `init-clear-quad`, not a `-system`.
Hint-level, not S3 (substance is present). Also carried from R1 and worth
correcting in the drift log: `assert-tier-partition!` does not exist (the
function at `test_runner.clj:231-255` is `assert-inventory!`), and
`expected-divergence?` has no definition in `verifier.cljs` (only the keyword
at `:553`). Neither reached contract text.

**A4 — §11's cosmetic gaps from R1 are still open.** R1 asked six locators be
added as substance-binding; none were: `<store-frame` :382 (§7 cites
":391-412"; the form is :382-415), `store-pool-entries` :1967,
`serveSyntheticOrigin` :102, `render-system-bytes!` :82, `scene-tape` :254,
`rebuild-ordered` :272, `replace-texture!` :194 (N10). §11 declares locators
"hints", and §7 cites the first two inline, so this stops none of the wave.

**A5 — the JVM fixture generator lands under `test/` and meets the inventory
assert.** §12 MAY CREATE puts the generator "under
`test/app/fixtures/render_engine/images/`". `test/app/fixtures/render_engine/`
today contains only `gpu-goldens/` (verified — `images/` does not exist).
`assert-inventory!` (`test_runner.clj:231-255`) performs duplicate-tier and
stale-classification checks over discovered namespaces, so a `.clj` under
`test/` may need a tier entry; §10 G10 and §12 already permit the "+1".

---

## Verified clean (recorded so the next round need not re-derive)

1. **The pool-lane pattern DOES compose; §7's core structural claim is TRUE.**
   `store-pool-entries` takes `count-key` as a **parameter** (`renderer.cljs:1968`),
   reads `[:ops-count-by-vi vi count-key]` (`:1973`), mints
   `entry-id [:frame/store vi count-key]` (`:1975`), and keeps **one
   loop-local `offset`** seeded from `base-offset` (`:1970`, `:1989`). It is
   invoked once per kind — `:shadows` with base `editor-shadow-count`
   (`:2030-2031`), `:rects` with base `editor-rect-count` (`:2046-2047`) — so
   **each lane has its own accumulator and no cross-kind bookkeeping exists.**
   A fourth `:images` invocation therefore cannot disturb rect/shadow
   first-instance offsets, and entry-ids cannot collide across lanes because
   `count-key` is part of the id. The `:shadows` lane is a complete working
   precedent for everything §7 asks. (Caveats are N6, N13, N14, N1.)
2. **§5's product-pick claim is TRUE.** `containers/inverse-point`
   (`containers.cljc:252-265`) applies the **full** inverse affine
   (rotation/shear/scale, singular fail-closed at `:259-261`) before
   `rt/hit-test`, which is exact half-open AABB in container-local space
   (`rect_tree.cljc:372-373`, parent→child composition is pure translation).
   So the container-local AABB test *is* the transformed-quad interior test —
   and it holds precisely because `rt-node` (`:12-28`) carries no per-node
   transform, so an independently rotated instance is inexpressible.
3. **An image rt-node participates in `hit-test` with ZERO code changes.**
   `hit-test` destructures only `:bounds` and `:children` (`:367`); the sole
   predicate is bounds containment (`:372-373`). No type filter anywhere.
4. **`hit-test`'s traversal, precisely:** `(some … (rseq children))`
   (`:375-377`) — first hit in **reverse** child order (topmost sibling wins),
   descending only into the winning branch; a child is reachable only if the
   point is inside the **parent's** bounds. So it is "deepest node of the
   topmost hitting branch", not "deepest in the tree" — and it ignores
   `:clip?` entirely. §7's "the tree traversal law" is vague but not false;
   both properties are pre-existing product-vs-candidate divergences shared
   with rects.
5. **`pick` order and stop semantics as claimed.** `pick-reverse` walks
   `(rseq (:entries tape))` (`scene_tape.cljc:538`) and returns on the first
   hit (`:542-544`), the exact reverse of `paint-forward` (`:529`). `:pick` is
   read in exactly ONE place in all of `src/app/` — `scene_tape.cljc:541`
   `(not= :none (:pick entry))`, a boolean gate — and there is **no**
   `:geometry`-keyed dispatch anywhere in any pick path. R1's F3 trace
   re-verified TRUE.
6. **T10 and T12 are implementable inside the fence's existing structure.**
   `forbid` already forbids bare identifiers inside extracted slices (`:87`,
   `:93`); `findForm` reaches any `defn-`, so the image producer is
   extractable; the fence is wired at `package.json:9`.
7. **The R1 clean-traces carried in the ledger re-verified.** All 30 §11
   locators substance-correct (30/30 exact line matches except the init-system
   list, A3); `environment.json` `fingerprintSha256` on disk **is**
   `5ced2482f3d0b8e9a14465bba06808a04d3a5a9b7d386fcf71f40a30f557343e`,
   matching §8's Act-0 pin; `frame-contract-registry` (`renderer.cljs:2192-2198`)
   genuinely throws at load unless registry and `family-ids` match exactly;
   candidate-seam fixtures need no `runtime/render.cljs` edit (all five
   `init-*-system`s take `:scene-color` directly at `:683-686`, `:868-871`,
   `:932-935`, `:1120-1123`, `:1257-1258`).
8. **Tree state as the contract asserts.** `git status --short`: only the five
   untracked docs (incl. this round's artifacts); no tracked modifications.
   HEAD `c0e9bbb`. 21 PNGs in `gpu-goldens/`. No `.icc`/`.icm` anywhere in the
   repo. `test/app/fixtures/render_engine/images/` does not exist.

---

## What was NOT verified (honest gaps)

- **I did not read the binding docs.** `W1.md` (Contracts O/G/M/C),
  `ENGINE.md` §0/§12, `W2-B-T1.md`, `W0-C.md`, `DISCOVERIES.md`. Every finding
  above is **code-vs-contract** or **R1-vs-recut**; none is
  **contract-vs-W1**. So whether §5's descriptor faithfully instantiates
  W1 §4.1/§4.9, whether §6's batching law is quoted correctly from Contract O,
  and whether the two-regime colour law is lawful under Contract C §8.2.5 are
  all **unchecked by me**. R1 read `W1.md` whole and `W2-B-T1.md` whole; I
  inherited none of its W1 readings as verified. A gate session should not
  treat R2 as W1 coverage.
- **Nothing was executed except the ICC probe.** No `npm run
  verify:render-engine`, no JVM suite, no fence run, no cljs compile. The
  Act-0 fingerprint check is a file read, not a run.
- **All WebGPU/browser behaviour.** Whether `copyTextureToTexture` accepts the
  `-srgb`/non-`-srgb` format pair N2's road 2 needs (copy-compatibility is my
  INFERENCE from the WebGPU spec, not tested); whether blending on an `-srgb`
  attachment happens in linear space on this SwiftShader build; whether
  `createImageBitmap` honours an embedded ICC profile here (N19); whether
  device loss is inducible (G9). Each is a magnitude/behaviour claim needing a
  runtime probe.
- **Whether a non-AA quad actually emits only 0/255.** N4 stands regardless
  (its live edge is the channel-0 assumption, which IS traced), but the
  ramp-vs-hard-edge premise is general rasterisation knowledge, not repo-
  verified for a shader that does not exist.
- **`init-image-system`'s bind-group shape.** R1's A3 noted
  `execute-gpu-batch!` supports one bind group at index 0 and a 6-vertex
  non-indexed draw. I re-verified that (`renderer.cljs:2157-2168`) and used it
  in N6, but I did not evaluate whether a texture-array layout fits the MSDF
  precedent's 5-entry group.
- **`face_assembly.cljc` / `containers.cljc` / `editor_compute.cljs`
  interactions** with an image rt-node beyond `rect_tree`'s four walks and
  `hit-test`. Note `editor_compute.cljs` builds `{:rects … :shadows …}` maps
  at eight sites (`:344`, `:533`, `:604`, `:658`, `:734`, `:773`, `:788`,
  `:829`) and is **§12 MUST NOT TOUCH** — whether a fourth lane's absence from
  those maps breaks anything is untraced.
- **Parallel-work collision** accepted from `git status` alone; I did not
  re-derive it against other sessions' worktrees.
