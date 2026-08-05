# IMAGE-ATOM contract validation R1 (fresh, default-fail)

**Session:** fresh-context, default-FAIL validation round over
`IMAGE-ATOM-CONTRACT.md` (CUT 2026-08-05). No loyalty to the author.
Every finding below carries a `file:line` trace; claims I could not trace
are in "What was NOT verified".

**Method note (investigation fence):** everything asserted here is a
STRUCTURE claim read from source (path exists / field carried / value
hardcoded). I ran no verifier, no suite, no fence. Where a claim needs a
runtime receipt I say so and mark it a hypothesis with its kill-probe.

## Verdict: FAIL

Five findings require recutting a section (§5, §7, §8, G4, G6). Ten more
are in-place fixes. The contract is close — its admission/registration
spine (§4, §5's descriptor, §9's traps) traces clean against the code, the
§11 manifest is substance-correct at every locator, and the fence's
renderer-slice extraction genuinely sees an image producer. What fails is
(a) the store lane in §7, which as written silently corrupts the live GPU
payload and leaves three unnamed forks, and (b) two colour/parity gates
that as written can only fail or can only vacuously pass.

---

## Findings

### F1 · FAIL — §7's per-instance store entries silently corrupt the live GPU payload; §4 calls the required repair "count plumbing if needed"

**Claim under test.** §7: "Each image instance emits its OWN tape entry"
in the store's maintained view; §4: "Store lane: `scene_store.cljc` +
`rect_tree.cljc` (+ `scene_runtime.cljs` count plumbing if needed)".

**Trace.** The store's `:ordered` maintained view is the SOLE input to the
live GPU payload, and that derivation assumes exactly one entry per slot:

- `src/app/client/workspace/scene_runtime.cljs:391` — `entries (ss/maintained-entries store)`
- `:393` — `ordered (mapv :runtime/slot entries)`  ← one slot per entry
- `:395` — `:rects (into [] (mapcat (comp :rects :ops)) ordered)`
- `:400-406` — `:ops-count-by-vi` keyed by `(:vi slot)`
- `:409-412` — `:order-by-vi` keyed by `(get-in entry [:runtime/slot :vi])`

consumed by

- `src/app/client/substrate/webgpu/renderer.cljs:1967-1990` `store-pool-entries`,
  which loops `(:ordered-vis store-frame)` and accumulates
  `offset (+ offset instance-count)` from `:ops-count-by-vi` (:1973, :1989),
  minting `entry-id [:frame/store vi count-key]` (:1975).

With K entries per slot, `ordered` repeats the SAME slot K times, so
`:rects` is duplicated K× in the uploaded array, while `:ops-count-by-vi`
and `:order-by-vi` are maps keyed by `vi` and collapse to one value.
Result: instance offsets desync from the uploaded buffer (visual
corruption of existing rect content), `:ordered-vis` repeats and
`store-pool-entries` emits duplicate `[:frame/store vi :rects]` ids — which
`scene_tape.cljc:508-515` (`compile-tape`, "Scene tape entry ids must be
unique") throws on whenever the twin-check flag is set, and which
`update-frame-arrangement` (renderer.cljs:2215-2235) silently dedupes when
it is not.

This also falsifies the contract's own G2 promise ("image entries never
re-sort existing families … the 21 goldens byte-identical — ordering of
existing content is untouched"): the ordering is untouched, but the rect
INSTANCE STREAM is not.

**Exact repair.** In §7, replace "+ `scene_runtime.cljs` count plumbing if
needed" with a named obligation: `<store-frame`
(`scene_runtime.cljs:388-412`) must derive its rect/shadow/text payload
from DISTINCT slots (or from entries filtered to the non-image families),
never from `(mapv :runtime/slot entries)`, and `store-pool-entries`
(`renderer.cljs:1967-1990`) keeps its one-entry-per-vi contract. Add
`scene_runtime.cljs:388-412` and `renderer.cljs:1967-1990` to §11 as
substance-binding.

### F2 · FAIL — §7 leaves `slot-entry`'s fan-out unnamed; G8's "maintained ≡ batch-oracle" then has a reading that cannot pass and a reading that leaks keys

**Claim under test.** §7: "The maintained ordered view is patched at the
same write sites (`upsert-slot` :140 / `ordered-insert`/`ordered-remove`) —
no new reconstruction door; `rebuild-ordered` stays the one sanctioned
rebuild." G8: "maintained ≡ batch-oracle equivalence holds with image
entries".

**Trace.** `slot-entry` (`scene_store.cljc:227-252`) returns exactly ONE
map, with `:family/id :render.family/rect` hardcoded at `:243`. It has
FIVE call sites, of which §7 names one:

- `:165` — `ordered-remove` of the OLD entry inside `upsert-slot`
- `:166-169` — `ordered-insert` of the NEW entry inside `upsert-slot`
- `:182` — `remove-slot`
- `:263` — `scene-tape`, the declared **batch oracle**
- `:283` — `rebuild-ordered`

Consequences of the literal reading (leave `slot-entry` alone, insert image
entries by some other path):

1. `remove-slot` (`:174-183`) removes exactly ONE key
   (`(slot-entry old {})`), so a slot with K image entries leaks K−1 stale
   keys in `:ordered`. `pick` (`:305-328`) then walks dead entries whose
   `:runtime/slot` holds a destroyed tree, and G8's equivalence fails.
2. `scene-tape` (`:254-270`) — the oracle G8 compares against — still emits
   `(mapv #(slot-entry % effective-transforms) (vals (:slots store)))`,
   one rect entry per slot. Equivalence is then structurally impossible,
   so G8 can only fail.
3. `upsert-slot`'s inherited-opts logic (`:152-157`) carries
   `:container-slot`/`:stack-path` forward but knows nothing about image
   entries, so a content-only rebuild drops them.

**Exact repair.** §7 must specify the fan-out explicitly: `slot-entry` →
a multi-entry producer (`slot-entries`, returning a vector whose head is
the existing rect entry with its rank preserved), and enumerate all five
call sites as required edits. Add `scene-tape :254` and `rebuild-ordered
:272` to the §11 `scene_store.cljc` row (§11 currently lists `slot-entry
:227` but not the two functions that consume it).

### F3 · FAIL — §7 never names the pick-dispatch mechanism for `:geometry :image-quad`; one implementable reading violates O6, and the fence does not catch it

**Claim under test.** §7: "image entries carry `:pick {:geometry
:image-quad :owner …}` and resolve through the SAME `pick-reverse`
projection (scene_store.cljc:305)". G4: overlap fixtures "return topmost by
reverse tape order through the REAL `pick-reverse` route".

**Trace.** `pick` (`scene_store.cljc:305-328`) builds ONE family-blind
`hit` closure. It reads `(:runtime/slot entry)` (`:315`) and calls
`rt/hit-test (:tree s) lx ly` (`:320`) for EVERY entry regardless of
family. `hit-test` (`rect_tree.cljc:359-372`) is axis-aligned `:bounds`
containment. Nothing anywhere reads `(:pick entry)` beyond
`pick-reverse`'s `(not= :none (:pick entry))` liveness test
(`scene_tape.cljc:541`). So `:geometry :image-quad` is decorative unless
some new code interprets it, and the contract does not say where that code
lives. Two readings, both physically implementable:

- **(a)** a family/geometry branch inside `pick`'s closure. This is a
  hand-positioned central PICK branch, which Contract O6 forbids (`W1.md`
  §3.3: "adding a family changes its registration and implementation,
  never a hand-positioned central draw/pick branch"). The fence would NOT
  catch it: `verify_scene_tape_fence.mjs:90-94` only requires three tokens
  in the `pick` form and forbids `sort-by` and the legacy layer lambda —
  there is no forbid on `case`/`cond` in `pick`.
- **(b)** a namespace-level `{family-id → hit-fn}` table in `scene_store`.
  Lawful: `store-fns-free?` (`:569-585`) walks only the store VALUE, not
  namespace defs, so a `def` of fns is legal.

Two physically implementable readings of a binding doc is precisely S6, and
the implementer hits it mid-phase with no authority to choose.

**Exact repair.** §7 must name the mechanism. Recommend (b), and add the
missing fence forbid to §9's T12 / §8's fence conditions: a family or
`:geometry` `case`/`cond` inside the extracted `pick` form is a fence
failure.

### F4 · FAIL — §5's C4 sentence is unsatisfiable under direct-present as the code stands; G6 therefore carries a gate that can only fail or force an undeclared S5

**Claim under test.** §5: "the choice is DECLARED in the descriptor and
PROVEN by C4 sentinels: exactly one conversion, never zero or two, on BOTH
presentation modes (direct-present legacy AND the candidate
linear-premultiplied seam enabled in fixture)". G6 repeats it and adds C1
("vs CPU linear-premultiplied reference") and C2 ("no fringe on non-black
background").

**Trace.** The legacy scene-colour descriptor is explicit about having NO
transfer at all:

- `scene_tape.cljc:19-29` — `legacy-direct-color`:
  `:working-space :presentation-encoded`, `:transfer :legacy-none`,
  `:alpha-association :straight`, `:blend {:color [:src-alpha
  :one-minus-src-alpha] :alpha [:src-alpha :one-minus-src-alpha]}`.
- `renderer.cljs:16-25` — the shader helper implements exactly that. The
  legacy branch returns `vec4<f32>(straight.rgb, straight.a * coverage)`
  with **no** transfer (`:17-19`); the candidate branch linearises and
  premultiplies (`:20-24`).
- `renderer.cljs:27-31` / `:33-37` — `configure-scene-color-shader` /
  `scene-color-blend` select the branch and map the factors; every
  `init-*-system` threads it (`:683-686`, `:868`, `:932`, `:1120`, `:1257`).
- **There is no linear→output encode anywhere in the tree.** A grep for
  `linear_to_srgb`, `to_srgb`, `pow(… 1.0/2.4)`, `0.4166` over
  `renderer.cljs` + `electric_flow.cljc` returns zero hits. So the
  candidate seam performs ONE transfer (ingress) and ZERO presentation
  transfers, despite `linear-premultiplied-color` declaring
  `:presentation-transfer :linear-to-output-once` (`scene_tape.cljc:38`).

Consequence for an image under **direct-present**:

- `*-srgb` format, or a declared shader transfer → linear values written
  into a presentation-encoded target. Exactly one transfer, but the image
  is visibly wrong beside its rect/text neighbours and cannot match a CPU
  linear-premultiplied reference (C1).
- `*unorm` with no shader transfer → matches the neighbours, but performs
  **zero** conversions, which §5 explicitly declares a failure.

And C1/C2 fail under direct-present for a second, independent reason: the
legacy ALPHA factor is `src-alpha`, i.e. `out.a = a² + dst.a(1−a)` — which
`W1.md` §1 and §8.2 already record as the migration defect ("use
`src-alpha` even for alpha accumulation"). No image pipeline can make that
composite equal a linear-premultiplied reference.

So §5's literal C4 sentence has no satisfiable implementation under
direct-present for at least one legal ingress tag, and G6 as written is a
gate whose only outcomes are failure or a mid-phase S5 escalation that the
contract presents as an exceptional event.

**Exact repair — pick one:**

1. **Scope it.** In §5 and G6, restrict C1/C2 and strict-C4 to the
   candidate-seam fixture. Under direct-present require a
   DECLARED-ZERO-scene-transfer receipt row that records
   `:transfer :legacy-none` / `:working-space :presentation-encoded`
   (scene_tape.cljc:19-29) as the reason — the honest reading of the landed
   seam. Drop "never zero" for that mode.
2. **Pre-satisfy S5.** Declare in §5 that S5 is already discharged for
   direct-present with the code citation above attached, and remove "on
   BOTH presentation modes" from C4. Then G6's mode matrix is
   candidate-only and executable.

Either way §5 must stop implying that the landed legacy road is a
one-transfer road; it declares itself a zero-transfer road.

### F5 · FAIL — G4's "DECISIVE-PASS" is satisfiable with zero decisive pixels: as written, the image parity gate cannot fail

**Claim under test.** G4: "quad-boundary probe DECISIVE-PASS across the
seven regimes; byte-128 ties reported as ties". §8: "Image parity must be
DECISIVE-PASS".

**Trace.** The parity machinery derives its entire row set from AA
boundary pixels and passes when there are no mismatches — including when
there are no rows:

- `verifier.cljs:232-247` `boundary-pixels` keeps only pixels with
  `(> coverage 8)` and `(< coverage 247)`;
- `:147-148` `pixel-red` reads **channel 0** — the machinery assumes the
  red byte IS coverage (true for the white rect and white glyph fixtures);
- `:359` `:pass? (zero? (count mismatches))`;
- `:360-363` `:verdict "decisive-parity"` when neither mismatches nor ties
  exist;
- `run_verifier.mjs:335` `parityPass = parity.every(row => row.pass)`.

A textured quad drawn as two triangles with no edge AA emits coverage `0`
or `255` only. `boundary-pixels` returns `[]` →
`boundary-pixel-count 0`, `decisive-count 0`, `mismatch-count 0`,
`pass? true`, verdict `decisive-parity`. The gate reports DECISIVE-PASS
having compared nothing. §5's declaration
(`:geometry-operator :aa-filter`, `:boundary-relation :isocontour-0.5`
"FOR THE QUAD EDGE") is the only thing that would produce a ramp, and no
gate asserts it was implemented.

Second, smaller edge of the same finding: for a non-white image the red
byte is image colour, not coverage, so `parity-receipt` would compare
garbage. §5 half-covers this ("Receipts isolate Cg with opaque reference
paint") but G4 does not require it.

**Exact repair.** G4 must assert per-regime floors on
`boundary-pixel-count` and `decisive-count` (> 0, ideally a pre-registered
minimum derived from the quad's screen perimeter at that zoom), and must
require the Cg-isolation fixture to be an opaque white reference paint so
channel 0 is coverage. §5 must state that the `:aa-filter` /
`:isocontour-0.5` quad-edge declaration obliges the image fragment shader
to emit a ramped edge coverage — otherwise the declaration is decorative
and T6's probe is unfalsifiable.

---

### M1 · MINOR — the verifier cannot fetch fixture images at all; G3/G6 have no byte-delivery road

`run_verifier.mjs:102-147` `serveSyntheticOrigin` serves exactly three
things: `/` and `/index.html` (`:111-118`), `/js/main.js` (`:119-125`), and
`/fonts/*` under a `fontRoot` containment check (`:127-141`). Everything
else returns 404 (`:142`). A fixture PNG under
`test/app/fixtures/render_engine/images/` is unreachable from the page, so
`createImageBitmap` has nothing to decode and G3/G6 cannot run. Two
implementable roads, neither named: add an `/images/` route (allowlisted —
`run_verifier.mjs` is MAY EDIT) or embed base64 bytes in `verifier.cljs`.
**Repair:** name the fixture-serving route in §8 and extend §11's
`run_verifier.mjs` row from "golden compare + manifest rows +
`--update-goldens`" to include `serveSyntheticOrigin` :102.

### M2 · MINOR — the capture path cannot composite two families in one pass, which C1/C2 require; and the obvious fix is an S4

`verifier.cljs:82-128` `render-system-bytes!` takes ONE `system`, hardcodes
`clearValue {:r 0 :g 0 :b 0 :a 0}` (`:104`), format `rgba8unorm`
(`:20`), one bind group at index 0 (`:108`), one vertex buffer (`:109`) and
`.draw pass 6 …` (`:110`); `opaque-png-data-url` forces alpha 255
(`:76`). C1 ("image over opaque/transparent grounds") and C2 ("no fringe on
non-black background") need a rect drawn first into the same target.
Editing `render-system-bytes!`'s clear value or format would change all 21
existing golden bytes → S4. **Repair:** §8 must require a NEW additive
multi-system capture function and state that `render-system-bytes!` is
byte-frozen for the existing 21 cases.

### M3 · MINOR — §8's "the 21 existing PNGs: byte-identical, asserted by the run itself" stops being an assertion the moment rows are appended

`run_verifier.mjs:440-445` `imagePass` requires
`imageComparison.length === expectedManifest.images.length`, so a run with
35 current rows against a 21-row manifest fails on LENGTH, not on bytes.
The classification cascade (`:447-458`) reaches `!imagePass` only after
`!parityPass`, which is permanently true in the RED state — so the golden
verdict never reaches the classification at all. The printed summary
(`:503-522`) has no golden field whatsoever; there is no "goldens 21/21"
value the command emits (that figure lives in `W1.md` §9.1 as a
receipt-file reading). **Repair:** G2 (and §8's Act-0 baseline) must name
the exact evidence path — `target/render-verifier/receipt.json` →
`goldenComparison.rows`, filtered to the 21 known filenames, all of
`rawMatch`/`pngManifestMatch`/`goldenFileMatch` true — instead of "asserted
by the run itself".

### M4 · MINOR — "append/metadata only" describes an intent, not a mechanism; the only mechanism rewrites everything and is authorised without a byte check

`--update-goldens` (`run_verifier.mjs:382-398`) writes EVERY golden PNG
(`:384-389`), then manifest.json (`:390-393`) and environment.json
(`:394-397`) wholesale. Authorisation is
`updateAuthorized = determinismPass && divergencePass && q8TransportPass &&
q5AffineBoundaryPass` (`:353-354`) — it does **not** include `imagePass`,
so a changed existing PNG would be blessed silently. §12's
"append/metadata only" and T7's ruling both assume this path without
naming it; the alternative reading (hand-append rows to manifest.json) is
near-impossible to execute (needs the run's `rawSha256` and `pngSha256`)
and still leaves `sourceMatch` false. **Repair:** §8 names
`--update-goldens` as the sanctioned mechanism and forbids hand-editing
manifest.json; G12 adds a post-update re-assertion that the 21 PNG blobs
are git-clean and that `environmentFingerprintSha256` still equals the
Act-0 pin (nothing in the code guards that — a Chrome auto-update between
Act 0 and the bank update would be baked in silently).

### M5 · MINOR — G1/G11's "seeded central-branch self-test still rejects" is vacuous by construction

`verify_scene_tape_fence.mjs:109-113` regex-tests a hardcoded string
literal (`seededCentralBranch`), not the production tree. It cannot fail
for any change the implementer makes. T12's ruling ("the seeded
central-branch violation still rejects") inherits that vacuity, so as
written T12's second condition is a no-op. **Repair:** state that the
self-test is a fence-integrity check only, and add the falsifier T12
actually needs — a seeded `:render.family/image` draw branch injected into
the extracted `drawFrame` slice, asserted rejected.

### M6 · MINOR — four gates name no command

- **G10 "full JVM suite green"** — `package.json` has exactly three scripts
  (`build`, `verify:text-layout`, `verify:render-engine`); the full suite
  runs through `test/app/test_runner.clj`, whose entrypoint the contract
  never names.
- **G10 "cljs compile 0 new warnings against the Act-0 captured baseline"**
  — no capture command named; §14's Act 0 says "cljs warning baseline
  capture" with no invocation.
- **G11 "text-layout + shaping fences untouched and green"** —
  `verify_shaping_correction_fence.mjs` exists on disk
  (`test/render_engine/`) but appears in NO npm script;
  `verify:render-engine` chains `verify:text-layout` →
  `verify_scene_tape_fence.mjs` → shadow release → `run_verifier.mjs`.
- **G9 "device-loss rebuild receipt … machine-driven"** — no command, no
  named induction mechanism.

**Repair:** put the exact invocation on each gate line (the skill's
owner/command law).

### M7 · MINOR — the new T10 producer-purity fence cannot be wired inside the allowlist

§12 MAY CREATE permits "one new fence/probe script if T10's
producer-purity check needs its own file", but `package.json` is NOT in MAY
EDIT, so the script can only ever be invoked by hand and G11's "the T10
producer-purity check exists and passes" has no repeatable home.
**Repair:** add `package.json` (script entries only) to §12 MAY EDIT, or
state that G11 accepts a raw `node test/render_engine/<file>.mjs`
invocation recorded in the phase artifact.

### M8 · MINOR — §8's "new modes vs new cases" fork invalidates G2's own pinned "divergence sentinels 7/7"

§8 says "at minimum TWO fixtures … across the SEVEN standing zoom regimes
(zoom-cases :31-38): 14+ new goldens". Two readings. If image fixtures
become new `zoom-cases` rows rather than new MODES inside the existing
seven, then `expectedDivergenceRows` (`run_verifier.mjs:227-233`) maps over
EVERY `result.cases` row and `divergencePass` requires all of them
(`:336`) — while `expected-divergence?` (`verifier.cljs:547-554`) is a
rounded-rect-corner fact (`corner-cpu` false AND `corner-gpu < 128`) that
an image case would not reproduce. G2's literal line "divergence sentinels
7/7" then fails, and the printed `productBoundsDivergenceSentinels`
fraction moves. **Repair:** §8 states "additional modes within the existing
seven `zoom-cases`; no new case rows", and §8's before/after pin list adds
the parity-row denominator (21 → 28/35) so the gate session is not
surprised.

### M9 · MINOR — T11's budget-truth ruling misses `replace-texture!`

`gpu_budget.cljs:152` `register-texture!` prices
`width*height*layers*bytes-per-pixel` — level 0 only, exactly as §4/T11
say. But `replace-texture!` (`:194`) carries the same level-0 pricing and
is the natural route when an image source's bytes are replaced (a new
material revision, §3). §11 and T11 name only `:152`. **Repair:** extend
T11 and §11's `gpu_budget.cljs` row to `replace-texture! :194`.

### M10 · MINOR — §5's descriptor demands values the shared private helpers hardcode; say so, and pin the five existing families' emitted values

`validate-family!` CAN express the image descriptor (traced: required key
lists at `scene_tape.cljc:297-307`, `:353-362`; the `family-ids`
membership check at `:350-352` is satisfied by §4's registry edit; the
scene-colour check at `:364-367` is satisfied by the shared helper). But
two shared privates need parameterising:

- `registration` (`:244-283`) hardcodes
  `:export-projections :declared-by-future-exporter` (`:255`), while §5
  demands the field "says exactly that" this wave promises no export;
- `regime` (`:67-76`) hardcodes `{:zoom {:min 0.01 :max 1000.0}}`, while §5
  demands "a COMPLETE non-gapped partition … with the floor-default
  `[0.1, 8]` reported as its own ordinary regime".

The partition is satisfiable — `validate-regimes!` (`:316-340`) requires
`first-min` exactly `0.01`, `last-max` exactly `1000.0`, and consecutive
`max == min` — so `[0.01,0.1] [0.1,8] [8,1000]` validates. **Repair:** §5
should say the shared helpers are parameterised **without changing the five
existing families' emitted values**, because `family-contracts` (`:285-295`)
feeds `default-family-registry` (`:384-385`) which every store write
validates against (`scene_store.cljc:166-169`).

### M11 · MINOR — the profiled-PNG fixture's ICC bytes have no source in the tree, and the naive road dead-ends into a package.json edit (S2)

G6 requires "the profiled fixture (embedded-profile PNG whose post-ingress
pixels are pinned)"; §3 makes `:embedded-profile` a legal ingress tag and
§6 says the `colorSpaceConversion` behaviour "is receipted with a profiled
fixture". Feasibility trace: there is **no** `.icc`/`.icm` file anywhere in
the repo, and `package.json` carries no PNG-writing or image dependency
(no `sharp`, no `pngjs`) — devDeps are `karma*` + `puppeteer` only. Adding
one requires `package.json` + lockfile edits, which are outside §12 → S2
mid-phase. The clean road exists and costs nothing but is unnamed: a JVM
generator via `javax.imageio` + `java.awt.color.ICC_Profile.getInstance`
(a non-sRGB built-in profile gives a genuinely non-identity decode
conversion), or a hand-rolled PNG writer over node's builtin `zlib` with a
`gAMA`/`cHRM` tag — but `gAMA`/`cHRM` is not an *ICC* embedded profile, so
§3's tag semantics would need to accept it. **Repair:** §6 names the
generator road and the profile source; §3 states whether
`gAMA`/`cHRM`-only counts as `:embedded-profile`. No network fetch is
needed on either road (good — nothing in §12 would permit one).

---

### A1 · ADVISORY — §7 and the archive draft mean different things by "per-instance entry"; naming the owning arrangement removes most of F1–F3's ambiguity

The draft's `image-scene-entries`
(`docs/current-mental-model/build/studio/archive/untracked/src/app/client/substrate/image_material.cljc:206-230`)
emits **renderer-side frame** entries whose `:paint` is
`{:source-key … :vertex-count 6 :instance-count 1 :first-vertex 0
:first-instance index}` — a planning value with no pipeline/bind-group,
which the GPU layer finalises. That matches `gpu-paint`
(`renderer.cljs:1949-1957`) and `execute-gpu-batch!` (`:2157-2168`), which
read `[:pipeline :bind-group :buffer :vertex-count :instance-count
:first-vertex :first-instance :scissor]` and would `.setPipeline pass nil`
on the draft's value as-is. §7 instead puts per-instance entries in the
**store's** `:ordered` (`scene_store.cljc:127`). Those are two different
arrangements — the renderer's `!frame-arrangement`
(`renderer.cljs:2206-2207`, explicitly comment-fenced at `:2204-2205`: "the
scene store never learns these transient entries") and the store's
`:ordered`. The contract never states which owns image entries, and §7's
prose reads as if they are the same tape. Saying it plainly (store =
semantic order + JVM receipts; renderer arrangement = GPU payload;
`store-frame` is the one bridge) is the cheapest fix for F1, F2 and F3 at
once.

### A2 · ADVISORY — §6's batching law and §7's per-instance entries are two mechanisms; the wave doesn't say which lands

§6: "batches split by texture binding … batching merges ONLY
order-contiguous compatible entries". §7: "Each image instance emits its
OWN tape entry". The draft resolved this with `contiguous-source-runs`
(archive `image_material.cljc:232-247`) declared "the planning seam … the
opening executor draws per entry". Both are implementable and neither
violates policy, so this is not a stop clause — but stating "draw per
entry this wave; run-merging is a named extension point" saves a design
round.

### A3 · ADVISORY — `execute-gpu-batch!` supports exactly one bind group at index 0 and a non-indexed 6-vertex draw

`renderer.cljs:2157-2168`: `.setPipeline`, `(when bind-group (.setBindGroup
pass 0 bind-group))`, `(when buffer (.setVertexBuffer pass 0 buffer))`,
`.draw`. An image pipeline must fit that shape. The MSDF precedent shows it
is achievable — sampler + texture view + camera + sizes + containers all
packed into one 5-entry bind group (`:742-749`). Worth stating so the
implementer does not design a two-group layout and then need an executor
edit that T1/O6 would put under review.

### A4 · ADVISORY — the synchronous `.cljc` sha256 precedent the contract does not cite

§4 requires `image_material.cljc` to be `.cljc` so "every law is
JVM-testable without a GPU", and T9/M5 require ingress to re-hash bytes.
The cited precedent for browser work is `fonts.cljs:61`
(`createImageBitmap`), but no digest precedent is given — and the verifier's
own `sha256-bytes` (`verifier.cljs:55-57`) uses `crypto.subtle`, which is
**async**. A promise-shaped ingress API would collide with the pure
JVM-testable law. The synchronous precedent exists:
`text_layout.cljc:974-982` — `java.security.MessageDigest` on JVM,
`goog.crypt.Sha256` in CLJS, one reader conditional. Cite it in §3 or §6.

### A5 · ADVISORY — two environment fingerprints live in the binding set

`W2-B-T1.md` §3 records `e79490f8882cd785…` (pre-amendment);
`W1.md` §9.1 and this contract's §8 record
`5ced2482f3d0b8e9a14465bba06808a04d3a5a9b7d386fcf71f40a30f557343e`.
On-disk `test/app/fixtures/render_engine/gpu-goldens/environment.json`
`fingerprintSha256` **is** `5ced2482…` — verified, so the contract's Act-0
pin is correct and consistent with the current tree. Flag the stale sibling
in the Act-0 sweep so it is not read as a mismatch.

### A6 · ADVISORY — three contract claims traced CLEAN (recorded so the gate session need not re-derive them)

1. **§4's "the registry edit and the tape edit land together or nothing
   boots" is TRUE.** `frame-contract-registry`
   (`renderer.cljs:2192-2198`) throws at namespace load when
   `(set (keys frame-family-registry))` ≠ `(set scene-tape/family-ids)`.
2. **Duty 2(c) resolves clean: the fence's renderer slice WOULD see an
   image producer.** `verify_scene_tape_fence.mjs:52-54` slices from
   `"(def ^:private frame-family-registry"` to `"(defn- compile-frame-tape"`
   — the image family entry inside `frame-family-registry`
   (`renderer.cljs:2170-2190`) is inside that slice, and `contracts` is the
   whole `scene_tape.cljc` file (`:44`). Adding the id to `families`
   (`:56`) makes both `requireToken` checks (`:96-99`) real.
3. **Duty 8: the scene-colour fixture does NOT require a MUST-NOT file.**
   Every `init-*-system` accepts `:scene-color` directly
   (`renderer.cljs:683-686`, `:868`, `:932`, `:1120`, `:1257-1258`), so the
   verifier can construct a candidate-seam system without touching
   `runtime/render.cljs` (`use-persistent-render-target? false` at `:18`,
   which the fence pins at `verify_scene_tape_fence.mjs:103`) or
   `electric_flow.cljc`. No scheduled S2 there. Likewise
   `test/app/test_runner.clj`'s `assert-tier-partition!` (`:234-251`)
   requires a new namespace in exactly one tier, and `full-receipt-floor`
   (`:161-163`, `{:test 397 :assertions 5416}`) is a floor that only grows
   — the allowlisted "+1" is exactly right.

### A7 · ADVISORY — §11 manifest verified clean in SUBSTANCE at every locator

Every symbol resolves and every line count is exact. Spot-verified:
`scene_tape.cljc` 545 ln — `family-ids` :10 · `scene-color-seam` :44 ·
`registration` :244 · `family-contracts` :285 · `validate-family!` :342 ·
`register-family` :370 · `paint-forward` :525 · `pick-reverse` :534.
`renderer.cljs` 2385 ln — `max-transform-nodes` :618 ·
`create-msdf-font-resources` :760 · `create-slug-texture` :786 ·
**`gpu-paint` :1949** (a `defn-`; present) · `execute-gpu-batch!` :2157 ·
`frame-family-registry` :2170 · `frame-contract-registry` :2192 ·
`compile-frame-tape` :2237 · `frame-tape-twin-check!` :2243 · init systems
:681 (rect) / :866 (msdf) / :930 (slug) / :1118 (shadow) / :1256
(clear-quad). `scene_store.cljc` 585 ln — :46 / :55 / :140 / :227 / :285 /
:305 all exact. `rect_tree.cljc` 402 ln — :12 / :198 / :259 / :330 / :359
all exact. `scene_runtime.cljs` 552 ln — `:ops-count-by-vi` :402 exact.
`gpu_budget.cljs` `register-texture!` :152 exact. `verifier.cljs` 805 ln —
`zoom-cases` :31 exact. Fence 132 ln — `families` :56 exact.
**No S3 condition exists.** Two gaps rather than errors: §11 omits
`scene-tape :254` / `rebuild-ordered :272` (F2), `scene_runtime.cljs`'s
`<store-frame` :388 (F1), `store-pool-entries` :1967 (F1),
`serveSyntheticOrigin` :102 (M1), `render-system-bytes!` :82 (M2), and
`replace-texture!` :194 (M9).

Also verified: §1.4's claim that the "HIG reference corpus" has no on-disk
referent — `find` for any `*hig*` path outside caches/goog returns nothing,
and `test/app/fixtures/render_engine/` contains only `gpu-goldens`.
§2's claim that `use-persistent-render-target?` stays `false` —
`runtime/render.cljs:18`. And the Act-0 environment pin matches disk (A5).

---

## Scenario traces run

1. **Image entry through the store's maintained view into the live GPU
   payload** — `upsert-slot` → `slot-entry` → `:ordered` →
   `maintained-entries` → `<store-frame` → `store-pool-entries` →
   `frame-entry`/`gpu-paint` → `execute-gpu-batch!`. (F1, F2, A1)
2. **Slot removal with K image entries** — `remove-slot` → single
   `ordered-remove` → leaked keys → `pick` walking dead entries → G8
   equivalence. (F2)
3. **Batch-oracle equivalence with image entries** — `scene-tape`
   (one-entry-per-slot) vs `:ordered` (K-per-slot). (F2)
4. **A pick at an image quad's interior** — `pick` → `pick-reverse` →
   family-blind `hit` closure → `rt/hit-test` AABB containment; who reads
   `:geometry :image-quad`. (F3)
5. **Family admission** — image registration through `registration` /
   `geometry` / `regime` helpers → `validate-family!` required-key lists →
   `validate-regimes!` partition arithmetic → `register-family` →
   `default-family-registry` → `frame-contract-registry` coverage throw.
   (M10, A6.1)
6. **The fence over an added image family** — `findForm` extraction,
   `registryStart`/`registryEnd` slice bounds, `families` loop, the pick
   forbids, the seeded self-test literal. (F3, M5, A6.2)
7. **One frame's colour path under both scene-colour modes** —
   `legacy-direct-color` / `linear-premultiplied-color` descriptors →
   `configure-scene-color-shader` → `scene_color` WGSL branches →
   `scene-color-blend` factors → target write; searched for a
   linear→output encode and found none. (F4)
8. **An image parity probe through the existing verifier machinery** —
   `render-system-bytes!` → `boundary-pixels` red-channel filter →
   `parity-receipt` pass/verdict → `run_verifier.mjs` `parityPass` →
   classification cascade. (F5, M2)
9. **Bank extension end to end** — new `imageRows` → `sourceMatch`
   fingerprint over `{shaderDigests, productionInputs}` after a
   renderer.cljs edit → `imagePass` length guard → the classification
   `else-if` cascade → `--update-goldens` write set and its
   `updateAuthorized` predicate. (M3, M4, M8)
10. **Fixture bytes reaching `createImageBitmap`** —
    `serveSyntheticOrigin` route table and the 404 default. (M1)
11. **Allowlist collision sweep** — every gate's required file against
    §12 MUST-NOT: `runtime/render.cljs` (not needed — `:scene-color`
    kwarg), `electric_flow.cljc` (not needed), `package.json` (needed for
    M7 wiring — the one real collision), `test_runner.clj` (allowlisted,
    partition + floor both compatible).
12. **Fixture generator feasibility** — repo scan for ICC bytes and for a
    PNG-writing dependency; JVM `javax.imageio`/`ICC_Profile` and node
    `zlib` roads. (M11)
13. **Digest law across CLJ/CLJS** — `crypto.subtle` async vs the
    `text_layout.cljc` synchronous reader-conditional precedent. (A4)
14. **§11 locator sweep** — `grep -n` for all 30 named symbols plus `wc -l`
    on all seven files; environment fingerprint compared to disk. (A5, A7)

## What was NOT verified (honest gaps)

- **Nothing was executed.** No `npm run verify:render-engine`, no suite, no
  fence run, no compile. Every claim above is read from source. The Act-0
  fingerprint check is a file read of `environment.json`, not a run — a
  real Act 0 could still mismatch if the environment drifted.
- **Whether `createImageBitmap` under HeadlessChrome 150 / SwiftShader
  honours `colorSpaceConversion` for an embedded profile.** This is a
  behaviour/magnitude claim, not a structure claim. Kill-probe: decode one
  profiled PNG in the verifier page and print the post-decode pixel. F4's
  repair does not depend on it, but G6's profiled-fixture leg does.
- **Whether a valid ICC-tagged PNG can be produced deterministically by
  either road in M11.** I traced the absence of ICC bytes and of an image
  dependency, and named two plausible roads; I built neither. Kill-probe:
  generate one and re-read its `iCCP` chunk.
- **Whether device loss is inducible under this puppeteer/SwiftShader
  stack** (G9). `device.destroy()` is the obvious lever; unverified.
- **The exact AA behaviour of any candidate image fragment shader.** F5's
  "no-AA quad emits only 0/255" is standard rasterisation (general
  graphics knowledge, not repo-verified for a shader that does not yet
  exist). The finding stands regardless: G4 does not assert a non-zero
  decisive count either way.
- **`W0-C.md`, `DISCOVERIES.md`, `decisions.md` were not read in full.** I
  read `W1.md` whole, `W2-B-T1.md` whole, `ENGINE.md` §0 and its Package-2
  section and the "execution front NOW" paragraph. So the contract's
  citations to `W0-C.md` §3/§5.7/§6 and to DISCOVERIES R5 are unchecked.
- **`face_assembly.cljc` / `containers.cljc` / `text_layout.cljc`
  interactions** with an image rt-node were not traced beyond
  `rect_tree.cljc`'s four walks and `hit-test`.
- **Parallel-work collision** was accepted from the contract's own header
  ("tree clean … NO overlap"); I did not re-derive it from git.
