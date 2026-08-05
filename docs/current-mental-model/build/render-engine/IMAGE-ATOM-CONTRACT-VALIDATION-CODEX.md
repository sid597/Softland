# IMAGE-ATOM contract validation — Codex

Date: 2026-08-05  
Mode: REVIEW + bounded REPAIR  
Authority: candidate; accepted §-level rulings preserved  
Object: `IMAGE-ATOM-CONTRACT.md`

## Verdict — NEEDS-FABLE

Implementation stays CLOSED. The narrow repairs below make the proposed gates
and receipts executable and remove five local ambiguities without changing the
accepted contract shape. They do not cure three protected ruling defects and
one unresolved lifecycle sink. A fresh Fable session must rule the four ranked
questions in §3 before an implementer session can open.

This is not a Package-2 scope objection. FULL-BREADTH, the image-first Package-2
position, MSDF RED, the 21 golden bytes, the default-OFF scene-color seam, and
the Sid-reserved durable-artery/felt-gate staging were treated as binding.

## 1. Input state and method

- Baton read first. Its tail initially ended with the R1 recut and named R2 as
  NEXT. During this review, foreign R2 artifacts and the Fable R2 baton entry
  appeared in the shared worktree. I stopped, re-read the new baton tail, then
  read `IMAGE-ATOM-CONTRACT-VALIDATION-R2.md` and
  `IMAGE-ATOM-RECUT-LEDGER-R2.md` in full before final classification. No
  foreign content was overwritten.
- Both R1 and R2 validation/ledger pairs were treated as consumed history, not
  authority. Five random traces spanning the two rounds were rechecked in §4;
  the four escalations below are new W1-conformance rulings or upgrades of
  R2-deferred minors, not repeats of R2's six consumed FAIL findings.
- Binding order used: `CLAUDE.md` →
  `docs/current-mental-model/decisions.md` → `ENGINE.md` §0/§12 → `W1.md`
  Contracts O/G/M/C → `W2-B-T1.md` → candidate contract.
- Every scoped source file and the 21-row manifest was read. No server file and
  specifically no `src/app/server/env.clj` was read.
- One fresh source+GPU replay was executed before repair:
  `npm run verify:render-engine`.

## 2. Narrow fixes applied in place

All rows are NARROW: the smallest correction was made and no protected §-level
ruling was changed.

| Finding | Exact before | Exact after |
|---|---|---|
| Texture replacement could restore level-0-only budget accounting after correct registration (`§4`, T11, G9, §11) | Only ``gpu_budget.cljs register-texture!`` was named; T11 said ``keep register-texture! level-0 pricing``; G9 tested one unspecified road. | Names both ``register-texture! :152`` and ``replace-texture! :194``; T11 and G9 require full-chain pricing through registration **and replacement**, numerically asserted on one fixture. |
| §4/§11 locator drift obscured the actual constructor set and omitted load-bearing forms | ``init systems :681/:866/:930/:1118/:1256`` called `init-clear-quad` a system and omitted `init-text-system :995`; §11 omitted `<store-frame`, `store-pool-entries`, `scene-tape`, `rebuild-ordered`, `render-system-bytes!`, and `serveSyntheticOrigin`. | Names the five real system constructors at ``:681/:866/:930/:995/:1118``, labels ``init-clear-quad :1256`` non-system, and adds every omitted form with its exact current locator. Substance and ownership are unchanged. |
| G4's Cg isolation did not pin the channel that the real verifier reads | ``OPAQUE REFERENCE PAINT saturating the compared channel``. | ``OPAQUE WHITE REFERENCE PAINT saturating the compared red channel, or ... a separate coverage-only attachment``. |
| The proposed scoped append could write before establishing custody, exactly as current `run_verifier.mjs` writes on lines 382–398 before comparing on 400–445 | No pre-write legacy-row assertion; source/environment/golden fields were only described as printed/classification-visible. | New §8 preflight asserts the unchanged 21-row filename set and every ``rawMatch``/``pngManifestMatch``/``goldenFileMatch``, determinism, environment, and old PNG digests **before any write**; post-append asserts source/environment/full bank. Act 0 records both source hashes and names current metadata/attestation debt without authorizing byte replacement. |
| G1–G12 were narrative and the expected RED runner had no success-form; G12 had no asserting consumer | No exact command matrix; ordinary verifier must exit 1; no receipt-checker path in §12. | Added exact `[JVM-FULL]`, `[RENDER-BUILD]`, `[RENDER-ASSERT]`, `[SCENE-FENCE]`, `[TEXT-FENCES]`, and `[PHASE-RECEIPT]` forms. `run_verifier.mjs --assert-image-contract` exits 0 only when MSDF is the sole preserved RED and asserts all receipt floors plus attestation. Added only `test/render_engine/verify_image_atom_receipt.mjs` to MAY CREATE, invoked directly by `node`; `package.json` remains read-only. |

## 3. Ranked BIGGER-SCOPE escalations — not edited

### 1. The declared zero-transfer legacy regime contradicts binding Contract C

**Classification:** BIGGER SCOPE — changes §5 color law and contradicts W1 and
ENGINE §0.

**Candidate claim.** §5 calls legacy direct-present a lawful declared
ZERO-transfer row and rejects a single exactly-one-transfer law across both
presentation modes.

**Taken-path trace.** `scene_tape.cljc:19-29` does describe the current legacy
resource as presentation-encoded with `:transfer :legacy-none`; the candidate
resource at `:31-42` promises one ingress and one presentation transfer.
`renderer.cljs:10-25` implements only sRGB→linear, while the optional
intermediate is copied raw to the swap texture at `:2376-2383`; there is no
code-real linear→output encode in the scoped tree. That verifies the historical
implementation fact, not the contract conclusion. Binding `W1.md:638-650`
requires exactly one ingress and one presentation encode; `:686-688` says
never zero or two; C4 at `:710-712` explicitly includes direct present.
`ENGINE.md:244-250` repeats tagged straight material → linear-premultiplied
scene → one declared presentation transfer and says the live pipelines are
migration input, not precedent (`W1.md:694-700`).

**Why decision-changing.** Both roads are physically implementable: preserve
the candidate's zero-transfer legacy acceptance, or obey W1 and make direct
present satisfy exactly one output transfer. They cannot both pass the binding
contract.

**Question Fable must answer.** Does §5 delete the lawful ZERO-transfer row and
require an exactly-one-transfer direct-present implementation/receipt, or is a
Sid-level W1/ENGINE recut being requested? Code reality cannot itself authorize
the latter.

### 2. §7 cannot preserve mixed image/rect sibling order with the named producer shape

**Classification:** BIGGER SCOPE — changes §7 lane design and Contract O.

R2 N6's per-binding-split defect is consumed: the candidate now gives the image
producer its own sub-run loop. This finding begins after that repair. R2 N14
noticed the undeclared part rank and left it as a phase minor; the binding W1
read upgrades it because the proposed representation cannot encode the required
mixed sibling order at all.

**Candidate claim.** §7 keeps one store entry per slot, adds one `:images`
payload/count key, and says the image producer mints
`[:frame/store vi :images]` entries split into per-binding runs, with IDs
unique per `(vi, kind)`, while mixed image/rect paint remains tape ordered.

**Taken-path trace.** `scene_runtime.cljs:391-414` collapses each slot to one
`order-by-vi` token and family counts; it carries no per-node sibling token.
`rect_tree.cljc:198-255` depth-first flattens rect paint into a family vector.
`renderer.cljs:1967-1990` emits one entry ID, one order, one bind group, and one
draw per `(vi, count-key)`; `gpu-paint` at `:1949-1956` carries only one bind
group, and the family-blind executor at `:2157-2168` binds group 0 and draws
once. `scene_tape.cljc:501-515` rejects duplicate entry IDs. Binding
`W1.md:111-116` requires sibling rank from material/instance structure and
forbids family identity as z-order; `:120-127` permits splitting only when
forward order remains equivalent.

**Why decision-changing.** Even if image-only binding runs receive unique
suffixes, the named payload has no order token capable of placing an image
between two rect siblings inside one slot. A single rect batch and multiple
image batches can implement rect-before-image or image-before-rect, but not an
arbitrary depth-first sequence such as rect/image/rect without also recutting
the rect lane or restricting image placement. The literal pinned ID also
collides for multiple binding runs.

**Question Fable must answer.** Pin the exact semantic order token and unique ID
for every binding run and show how alternating rect/image siblings are
represented. Is Package 2 allowed to recut all affected per-slot family lanes
into order-contiguous runs, or is image placement deliberately restricted to
one declared part position per slot? Either answer changes §7.

### 3. §5's product-pick route cannot implement its own boundary/slop policy

**Classification:** BIGGER SCOPE — changes §5 geometry/pick law.

R2 correctly verified that inverse-affine + local AABB is a transformed-quad
interior test. This does not repeat that settled trace. It upgrades R2 N17's
deferred slop placeholder after checking the route against W1's binding
boundary/slop citizenship.

**Candidate claim.** The image descriptor declares exact quad boundary = hit
and a declared screen-pixel hit-slop radius, then says the existing
`scene_store/pick` → `rt/hit-test` route is that product policy without new
dispatch.

**Taken-path trace.** `scene_store.cljc:305-328` inverse-transforms the point
and invokes only `rt/hit-test` on the slot tree. `rect_tree.cljc:359-380` tests
only node bounds, uses `>=` on min edges and strict `<` on max edges, and has no
hit-slop input. Neither function reads the registered geometry descriptor,
crop quad, boundary policy, or slop radius.

**Why decision-changing.** Two implementations are plausible under the text:
pin radius 0 and force node bounds to equal the crop/placement quad, or add a
family/policy-aware product pick projection. The first still excludes the
right/bottom exact boundary; the second conflicts with §5's refusal of a new
pick mechanism. The browser candidate-parity probe cannot make the actual
product read obey a policy it never consumes.

**Question Fable must answer.** What exact product mechanism makes closed quad
boundaries and the declared screen-pixel slop code-real? If the answer remains
the generic rt-node route, pin radius 0 and recut the half-open boundary law;
otherwise name the registered family-blind projection that replaces the
refused central branch.

### 4. The named “existing error lane” has no locator or source-scope referent

**Classification:** BIGGER SCOPE — the right sink is not unambiguous and may
change §6 lifecycle law or §12 custody.

**Candidate claim.** §6 requires an unavailable digest to paint a deterministic
placeholder and “report through the existing error lane”; G9 requires a
machine-driven device-loss rebuild receipt.

**Taken-path trace.** A bounded census over every scoped source/test file for
asset-unavailable, unresolved digest, device loss, placeholder paint, error
lane, and diagnostic found no asset-resource error/loss sink. The only matches
were renderer diagnostics-UI assembly at `renderer.cljs:2087-2155`; no
resource producer or registry route is connected to it. The contract names no
function, state owner, event shape, or executable receipt for either report.

**Why decision-changing.** Console reporting, renderer-owned state, and the
existing overlay diagnostics are all physically implementable and materially
different ownership choices. Choosing a sink may also force a file outside the
current allowlist. There is no unambiguous narrow rename to apply.

**Question Fable must answer.** Name the exact owner, function/state/event
shape, and in-allowlist machine assertion for asset-unavailable and
device-loss reporting, or explicitly widen §12. Which current symbol is the
claimed “existing error lane”?

## 4. Five prior-round traces sampled, not relitigated

1. **R1 F1 — verified consumed.** `scene_runtime.cljs:391-414` maps one
   maintained entry to one slot payload and collapses counts by `vi`;
   `scene_store.cljc:227-288` keeps one slot entry and the maintained/oracle
   equivalence. The R1 recut correctly refuses per-instance STORE entries.
2. **R2 N1 — verified consumed.** `scene_runtime.cljs:382-415` is indeed a
   Missionary-wrapped `.cljs` derivation, so its old JVM gate was impossible.
   Current §7 now requires extraction of ordered-slots → payload/count/order
   into an allowlisted `.cljc` home and G8 asserts that pure function on JVM;
   the renderer twin-check is separately a browser receipt.
3. **R2 N2 — verified consumed as written.** The product present road is the
   shaderless `copyTextureToTexture` at `renderer.cljs:2376-2383`; no output
   encode exists. Current §5 pins the candidate fixture/capture target to an
   `*-srgb` attachment in allowlisted renderer/verifier code and refuses a
   shader blit or MUST-NOT product-path edit. Escalation 1 is different: it
   tests the separately retained legacy ZERO-transfer row against W1.
4. **R2 N4 — trace verified; narrow closure completed.** The real probe reads
   red channel 0 (`verifier.cljs:147`, boundary rows `:232-247`) and its row
   pass can ignore zero samples (`:324-365`). The R2 recut added opaque
   reference paint and floors; this review narrowed that to opaque **white** or
   a coverage-only attachment and added a machine-asserting expected-RED mode.
5. **R2 N12 — verified consumed.** The R2 ledger's executed ICC result is
   represented exactly in current §8: explicit `IIOMetadata`/`mergeTree`,
   pre-deflated profile bytes, re-read + inflate/parse, and two-run byte
   determinism; plain `ImageIO.write` and mere chunk presence are not accepted.

## 5. Fresh executable receipt

Command: `npm run verify:render-engine`  
Observed exit: 1, expected standing RED  
Environment fingerprint:
`5ced2482f3d0b8e9a14465bba06808a04d3a5a9b7d386fcf71f40a30f557343e`

| Receipt field | Observed |
|---|---|
| Text layout / proportional shaper / text fence / scene-tape fence | green |
| CLJS release compile | 119 files, 0 warnings |
| Classification | `candidate-pick-parity-failure` |
| Determinism | 21/21 |
| Existing golden comparison | 21/21; every raw/manifest/file comparison true |
| Candidate parity | 14/21; standing MSDF RED preserved |
| Current-product divergence sentinels | 7/7 |
| Q8 affine transport / Q5 affine boundary | true / true |
| Environment match | true |
| Source match | **false** |
| Current renderer source | 122,333 bytes; `b144a282d035d479caef895f098d28f74ac16aca04237cc6d3ce799966dd5b64` |
| Manifest renderer source | 100,270 bytes; `1baa7b103f39c23950736e1c37e31ce5aef15bd0b991d05b722706676d47c0cc` |
| Browser adapter | vendor `google`, architecture `swiftshader`; `isFallbackAdapter` absent and renderer/description empty |

The source mismatch is a real stale-metadata input fact, not pixel drift: all
21 old rows still match. The attestation omissions are also real. The narrow
repair makes both debts explicit at Act 0 and machine-failing at final G2/G12;
neither can authorize golden-byte replacement.

## 6. Verified clean

- All manifest locators and stated source line counts were rechecked. The
  drifted init-constructor list, omitted load-bearing forms, and
  `replace-texture!` budget road were fixed narrowly.
- The five-family registry, fail-closed family validation, compile-time unique
  entry IDs, forward paint/reverse pick projections, exact frame-registry
  coverage, and renderer twin check exist as claimed.
- The R1 store recut preserves the current one-entry-per-slot maintained view
  and leaves the batch oracle structurally possible.
- The fixture origin currently lacks `/images/*`, so the named allowlisted
  `run_verifier.mjs` extension is real work, not a false locator.
- Current image comparison already computes `rawMatch`, `pngManifestMatch`, and
  `goldenFileMatch`; the repaired preflight can reuse those facts before any
  append write. No required gate now relies on ordinary expected-RED exit 1 as
  a “pass”.
- After the narrow allowlist repair, every named assertion file is editable or
  creatable. G11 only executes the protected text/shaping fences; it does not
  edit them. The unresolved error-lane choice is isolated as escalation 4.
- No code/test file, existing golden PNG, scene-color default, protected doc,
  server file, or stop clause was changed. No commit or push was made.

## 7. Next owner

Fresh Fable receives only the four ranked questions above. It should return
one bounded ruling artifact that either supplies coherent replacement text for
the affected protected sections or declares the candidate unsafe. No
implementation session opens from this validation alone.
