# shaping-correction — CONTRACT (one phase · implementer: Codex lane · gate: Fable, FULL tier)

Cut 2026-08-04 from Sid's settled ruling (the 2026-08-03 profiling
adjudication) + `FOUNDING-EVIDENCE.md` (the banked capture) + fresh source
reconnaissance, under the work-package skill's few-and-large rule: ONE
implementation phase, no PLAN.md, plan-grade specificity carried here.

**RECUT 2026-08-04 (R2 candidate):** this text consumes `VALIDATION_R1.md`
(immutable FAIL) §10 items 1–10 exactly; the mapping is
`RECUT_LEDGER_R1.md`. One wholly new default-fail validation round (R2) runs
over THIS text before the implementer opens (§14).

**Binding docs + precedence:** decisions.md (incl. "The render seam",
settled+amended 2026-08-03), Contract T (`build/render-engine/W1.md`), and
THIS contract bind. `NOW.md` is the baton — if it contradicts any of them,
they win; flag the discrepancy in NOW, do not pause. `FOUNDING-EVIDENCE.md`
is the evidence record: its numbers are facts, its readings are prior-pass
records, not authority.

**Manifest law:** locators below are navigation hints machine-verified with
`grep -n`/`wc -l` on 2026-08-04 against the WORKING TREE (which carries
uncommitted foreign work — §12), re-verified by the R1 round's full re-grep
and this recut's spot-checks. SUBSTANCE (named symbols, forms, semantics)
binds. Hint drift with substance intact → re-locate, log in the phase
artifact, never stop. Substance missing → stop clause S3.

---

## 1. Purpose — and the position against SEAM-STEP1

Kill the text-shaping cost defect that makes the proportional land unlivable:
cold visual settle 35.678s (28.725s of it one `text-gpu` interval) and ~6s
hover stalls between an ordinary block and the 24,891-character paste block.
The dominant defect is algorithmic — `shaped-layout` does per-cluster
whole-glyph filtering, ~O(C×G), plus a smaller per-run O(R×G) rescan — and
architectural — ground establishes legacy layout/wrap geometry, emits ops
WITHOUT the result, and the renderer fallback independently re-runs shaped
layout per op. One authority, carried results, linear construction, and an
invalidation partition are the correction. Everything beyond that is
evidence-gated (§7 step 5), never presumed. Every lawful end state of the
phase has exactly one name (§10 terminal classifications).

**Position:** this package is a PREDECESSOR to SEAM-STEP1 closure. SEAM-STEP1
stays frozen and its contract is NOT widened by this package — no SEAM file
(docs or its in-flight code hunks) is touched here (§12). Sequencing: the
SEAM-STEP1 implementation lands first (its uncommitted work shares files
with this allowlist); this package's phase OPENS after that landing, runs,
and goes green; SEAM-STEP1's felt gates then run on a land that does not
stall. Sid may re-order with one line; that is a ruling, not a stop.

**Consumers, in order:** the ground/faces render path today (172-block real
corpus) · SEAM-STEP1's closure gates · the engine floors as they multiply
text-bearing atoms (every future material rides this layout authority) ·
later: the editor split's keyed-shaping upgrade (fenced-view instance #2 —
out of scope here, named so no one deletes the door).

## 2. Non-goals — each a named extension point, not a void

- NO paint-delta/instance-level-diff machinery, NO viewport-first residency,
  NO streamed/budgeted population — these are §7 step-5 candidates that only
  a post-linearization profile may authorize. Building any of them in this
  phase is scope drift (trap T11).
- NO reactive/store rewrite and NO `<store-frame>` restructuring — the
  package ADDS a counter (§8); the counter's numbers authorize nothing until
  they become decision-relevant (invariant I9).
- NO editor/sidebar/panel migration to carried layouts — those surfaces keep
  COUNTED fallback temporarily (§4 I3 names them); their upgrade is the
  editor-split road (SEAM Act 2 lineage), not this package.
- NO reconciliation-path optimization (`ground/reconcile!` measured 0.846s —
  material but secondary per the ruling); revisit only if a §7 step-4
  profile promotes it.
- NO Electric file edits; transport untouched.
- NO new render/wrap semantics beyond §5's one defined change (block-greedy
  under proportional geometry). Visual wrap for unwrapped (non-machine)
  blocks must not change.

## 3. Evidence basis (all numbers = `FOUNDING-EVIDENCE.md`)

Cold: settle 35.678s; final frame 28.747s with `text-gpu` 28.725s at the
173-slot reshape; call-tree `layout` 29.109s ⊃ `shaped_layout` 28.343s ⊃
`union_bounds` 24.390s (the lazy per-cluster whole-glyph filter is realized
inside union-bounds' reduce — the scan is the defect, not the min/max).
Hover: the banked capture holds four transitions — largest→ordinary
6,124ms, ordinary→largest 6,028ms, largest→ordinary 6,101ms (2 slots
reshaped each), and the ordinary→empty control 28ms/20.7ms input→RAF.
Corpus: 172 blocks / 586,927 served chars / 177 live slots. GPU execution,
paint, packing, upload, draw, and sampled store-frame work are NOT the
present hot share. The historical 2–3-minute loading is UNEXPLAINED
RESIDUE — recorded, never attributed (invariant I10). The capture lacks a
serialized adapter attestation; every receipt this contract orders opens
with one.

## 4. The invariants (I1–I10 — the ruling, made executable)

- **I1 — one material-local shaped layout authority.** Ground text has ONE
  Contract-T layout result per (block visual text × §6 key), minted through
  `text-layout/layout` with the live provider, and THAT result is carried to
  measure, wrap, bounds, caret, selection, clipping, hit testing, and paint.
  No reader re-derives geometry (Contract-T's law, now enforced on ground's
  hot path).
- **I2 — declared, TOTAL layout key + reuse.** The §6 key is declared in
  code (one fn, one docstring naming the keying source), is total over
  every ground text op (§6), and hover, selection, and paint-only semantic
  rebuilds REUSE the carried result (layout-executions delta 0 — G3, G4).
- **I3 — ground fallbacks reach zero; legacy surfaces counted.** The
  renderer's per-op fallback (`position-text-op`'s `(or existing (tl/layout
  …))`, renderer.cljs:1484–1493) reaches count 0 for the ground/faces
  surface. Explicitly named legacy surfaces retain TEMPORARY counted
  fallback: the `<combined-text-ops` family (editor buffer, cmd-panel,
  settings panel, sidebar, trail, chat, agent output, dg-flow;
  runtime/render.cljs:87–96) and `<settings-panel-text`. Each fallback
  increments a per-surface counter (§8); no uncounted fallback exists.
- **I4 — linear construction.** Layout construction scales with G+C+R
  (glyphs + clusters + runs), never C×G or R×G — including wrap (T3: cut
  selection from ONE shaped pass per source line; only final segments
  reshape, total shaping work ≤ 2× the line's text). G1 binds the exact
  counter vocabulary and constants.
- **I5 — indexed spans downstream.** Clipping, paint ranges, and per-op
  glyph selection use indexed glyph/cluster spans (cluster-start→glyph-span,
  run→glyph-span, line-id→line via the existing memo road,
  renderer.cljs:1440–1470), never repeated full-vector searches
  (`clip-result`'s line filter text_layout.cljc:669–671 and
  `position-text-op`'s per-op glyph filter renderer.cljs:1502–1510 are the
  named offenders). The span-selection helpers live in shared
  `text_layout.cljc` so G6 can assert them on the JVM.
- **I6 — three distinct lifecycles, three distinct receipts.** Layout
  invalidation (§6 key change) ≠ paint invalidation (repack/tint) ≠
  GPU-geometry lifecycle (clone/in-place/destroy in
  `reconcile-slot-text-geos!`, runtime/render.cljs:25–64). Each has its own
  counter; no receipt conflates them. A backend flip exercises the second
  AND third lifecycle while the first stays untouched: paint repacks for
  the new coverage road, geo reclones/destroys, layout reused with
  identical layout IDs (G4 case e).
- **I7 — dirty/present-region ownership is correct under text change.**
  Slot-text changes, block entry, block exit, hover paint, and order changes
  each participate correctly in dirty/present-region ownership (the G8
  receipt line names what reshaped and why; a CONTENT-geo edit never bumps
  the slot write count — existing law, preserved).
- **I8 — camera-only and order-only cost zero.** Zero layout executions AND
  zero text-geometry rewrites on camera-only (pan/zoom) and order-only
  changes (G5). Zoom is camera, not a layout input (T7).
- **I9 — `<store-frame>` counter, no store conclusions.** Diagnostics gain a
  `<store-frame>` execution counter (home: scene-runtime's `<store-frame`,
  runtime/render.cljs:130, surfaced in the ground report). No reactive/store
  rewrite is authorized unless count AND wall share become
  decision-relevant in a §7 step-4/5 profile.
- **I10 — residue stays residue.** The historical 2–3-minute loading remains
  recorded as unexplained residue. 28.7−24.4 is NOT a predicted post-fix
  residual; no document downstream of this contract may state it as one.

## 5. Question A — one wrapping authority (RESOLVED, with a Sid stop)

**Today:** ground wraps machine blocks by character columns
(`ground-block-layout` face_primitives.cljc:587–616 → `:wrap-policy
:block-greedy` + `:max-chars wrap-col`, legacy `block-wrap-lines`
text_layout.cljc:80–97; also `machine-visual-lines` ground.cljs:1223–1235
via `face-primitives/wrap-lines`:559) while the shaped provider wraps only
`:word` by proportional advance (`shaped-segments` text_layout.cljc:283–307)
and has NO block-greedy road. Result: column cuts chosen by a monospace
fiction, painted proportionally — two geometry truths.

**Ruling:** the shaped provider gains `:block-greedy` wrap semantics so ONE
proportional geometry truth chooses line breaks. Policy meaning is
preserved; the mechanism changes. The semantics below are LAW — each
clause admits exactly one behavior and each is asserted executably in G1.

### 5.1 Result representation

Every visual line record of a block-greedy shaped layout carries:

- `:text` — the PAINTED text of the line (never contains break-consumed
  whitespace);
- `:source-range` — the full tagged source range the line OWNS, including
  any break-consumed whitespace at its end;
- `:consumed-range` — OPTIONAL tagged subrange; when present it is exactly
  the break-consumed whitespace and is always a SUFFIX of `:source-range`;
- the paint range is DERIVED: `:source-range` minus `:consumed-range`.
  Absent `:consumed-range`, paint range = `:source-range`.

Union of all `:source-range`s over a source line = the source line, exactly,
no gaps, no overlaps. All ranges use tagged indices (Contract T's
no-untagged-offsets law, W1.md).

### 5.2 Break-whitespace class (T14)

Break-whitespace is EXACTLY the set {U+0020 SPACE, U+0009 TAB}. Not the
`\s` regex class (JVM and CLJS `\s` disagree on U+00A0 NBSP — trap T14);
not NBSP (non-breaking by definition); `\n` never reaches wrap (hard breaks
are consumed by source-line splitting first, `-1` split limit semantics
preserved, trailing empties included).

### 5.3 Cut selection law

Per source line, from ONE shaped pass (T3):

1. Candidate cuts are declared cluster ends. A **break candidate** is the
   end of a maximal break-whitespace cluster run that has non-whitespace
   content AFTER it on the same source line. (Trailing whitespace at
   end-of-source-line is therefore never break-consumed — it is painted
   content, hard-wrapped by rule 3 if it overflows.)
2. **Whitespace preference:** choose the LAST break candidate whose PAINTED
   PREFIX (all clusters before the whitespace run) fits the inline-size
   budget. The fit test applies to the painted prefix only — the consumed
   whitespace is unpainted, so its advance never participates. The ENTIRE
   maximal break-whitespace run is consumed (`:consumed-range`), painted on
   neither line. The next visual line starts at the run's end.
3. **Hard cut:** if no break candidate's painted prefix fits, cut at the
   last fitting cluster end (mid-word). If not even one cluster fits, take
   exactly one cluster (progress guarantee). Hard cuts consume nothing.
4. Leading whitespace of a SOURCE line is painted content on its first
   visual line (indentation preserved). Continuation lines never begin with
   break-consumed whitespace by construction of rule 2.
5. Final segments reshape once each for line-correct bidi/contextual
   shaping (same law `shaped-segments` already states); per-source-line
   shaping calls ≤ 1 + segment count (G1 asserts).

Worked example (LAW, G1 case): text `"abc   def"`, budget fits 5 space
advances. The run `"   "` (offsets [3,6)) is a break candidate; painted
prefix `"abc"` fits → line 1 `:text "abc"`, `:source-range [0,6)`,
`:consumed-range [3,6)`; line 2 `:text "def"`, `:source-range [6,9)`.
(Legacy `block-wrap-lines` gave `["abc  " "def"]` — visible breaks CHANGE
under this ruling; that is §5's accepted change, S1 guards it.)

### 5.4 Reader law across consumed ranges

- **Caret:** every source offset keeps a caret stop. Offsets in
  [consumed-start, consumed-end) display at the END of the preceding line's
  painted advance; offset consumed-end displays at the START of the next
  visual line.
- **Hit test:** a point past the painted end of a wrapped visual line maps
  to the caret stop at consumed-start (the first consumed offset) — never
  to an interior consumed offset, never to the next line.
- **Selection:** a selection crossing a consumed range highlights to the
  painted end of the preceding line and resumes at the next line's start;
  the consumed range contributes zero highlight width.
- **Copy:** copy is source-range-based; consumed characters ARE included.

### 5.5 Headers

Headers remain ordered, UNWRAPPED synthetic prefix lines (never wrapped,
budget notwithstanding), each shaped ONCE with the live provider and the
body's shape options. Their lines carry a tagged synthetic header index
domain (e.g. `[:header i]`) — NEVER body source offsets (Contract T forbids
untagged/aliased source offsets). Ground's existing consumption contract
(first `nh` result lines are the headers — face_primitives.cljc:606–660) is
preserved.

### 5.6 Provider-fault totality

Nonempty text whose shaped result contains an EMPTY cluster list is a
provider contract fault. The layout does NOT throw and does NOT fall back
to code-unit-1 cuts (that splits surrogates): it returns a
poisoned-but-TOTAL result — the line as ONE unwrapped segment — and
increments the `provider-fault` counter (§8), which every receipt carries.
(Empty TEXT is not a fault: it lawfully yields one empty segment.) This is
the delay-totality gate class from the work-package skill.

### 5.7 Reference advance and budget translation

`reference-advance` is the provider's shaped advance of U+0020, shaped
EXACTLY ONCE per layout key, with the body's COMPLETE shape options
(features, variations, language, tab regime) at the block's font-size.
`inline-size = wrap-col × reference-advance`. Missing or non-positive
`wrap-col` → no inline-size → no wrapping (unbounded line). The
reference-advance value is part of the §6 metric regime; G1 asserts it is
provider-shaped (a synthetic provider with a distinctive space advance
proves no hard-coded constant survives) and shaped once per key.

**Stop S1:** if exact legacy column breaks turn out to be product identity
(anyone asks the new wrap to reproduce the old cut points), STOP for Sid —
that demand conflicts with Contract T's one-geometry-truth law and is not an
implementation detail. Expected and accepted instead: machine blocks'
visible line breaks CHANGE (proportionally chosen); unwrapped blocks'
breaks do not.

## 6. Question B — layout identity vs paint-resource identity (RESOLVED)

**The layout key** (I2's declared key; ONE fn owns it, its docstring names
the keying source — scene-substrate keying-source law). The constructor is
TOTAL: every ground text op — root body text, optimistic focused text,
paste projection body and header, machine fold display, material-authored
header copy, anatomy/invocation strings, and every auxiliary op (refusal,
notice, boundary, conflict lint, gold mark, silver mark, fold-header hit) —
receives a key by this construction, with no uncovered case:

- **Source token, by case:**
  - When an actual content-revision stamp exists for the text, the token is
    (stamp × the final VISUAL-TEXT projection token) — the pair, so
    optimistic/paste/fold projections can never alias the stamped truth.
  - Otherwise (optimistic focused text via `block-view`
    ground_edit.cljc:445; paste projection via `paste-projection`
    ground_edit.cljc:121; machine fold display + header copy via `run-view`
    ground.cljs:1190; auxiliary and anatomy strings; any unstamped ground
    text): the token is (declared visual-text VALUE HASH × stable source
    address × op role). Declared in the key fn, never silent.
- **Visual-projection inputs, exactly:** the fold state as applied to this
  subject, `:foldable/defaults` and `:foldable/header-copy` as PROJECTED
  into visible text (shared/foldable_material.cljc:31–40), and the paste
  projection inputs. EXCLUDED even though they ride the same wear maps:
  bindings (foldable_material.cljc:44–69), contribution stamps, whole-wear
  identity. A binding-only material revision produces an EQUAL key (G1
  key-law assertion; G4 row g).
- **Provider identity** — `provider-identity`'s fields (face-id,
  face-revision, shaper-id, shaper-version, features, variations, axes,
  fallback-chain, upem; text_layout.cljc:261–263).
- **Metric regime** — font-size, line-height, baseline-offset, wrap policy +
  inline-size (§5.7 translation, reference-advance included),
  language/direction, tab-stops, index-space version.

**NOT in the key** (may never invalidate layout): origin/position (carried
results are positioned by the existing anchor-delta road,
renderer.cljs:1499–1501 — a moved block never re-layouts), zoom/camera
(Contract-T legal-zoom stays enforced, moved to the consumer boundary — T7),
colors/tint/style/selection/caret/hover state, paint backend (MSDF vs Slug),
atlas coverage, GPU buffer/geo lifecycle. Grounds: "Placement/advance came
from Contract T. MSDF selects coverage metadata only" (renderer.cljs
:1538–1539, and the Slug twin :1573). A backend flip repacks paint for the
new coverage road AND reclones GPU resources (I6's second and third
lifecycles) and MUST reuse layout results with identical layout IDs —
unless it demonstrably changes the shared metric regime, which is a
receipt-bearing claim, never an assumption (G4 case e carries the
provider-identity-equal precondition receipt).

**The broad rebuild signature is NEVER a layout key.** Ground's rebuild
`sig` (ground.cljs:1476–1481) intentionally carries hover, selection,
boundary/group state, placement, whole worn-material maps, marks, and
metrics; keying layout on it (or on any whole view/wear map) is the T6
failure class. The key fn takes ONLY the inputs this section names — its
signature makes `sig` unpassable.

**Cache lifecycle (T9):** the carried-result cache is process-local, keyed
by the §6 key, bounded by the live block set — evicted on block death/slot
destroy (block-death sites ground.cljs:1923–1928, 3159–3167; vanished-slot
destroy runtime/render.cljs:61–64). Diagnostics carry size, hit/miss
counts, and `:id-digest` — a hash of the sorted live layout-ID set, the
cheap identical-IDs receipt G4 case e reads. No unbounded memo.

## 7. The correction, ordered — and what the gates decide

1. **Linearize cluster/run membership** inside `shaped-layout`: one pass per
   line builds cluster-start→glyph-span and run→glyph-span indexes (sort
   once if glyph order isn't cluster-monotonic); per-cluster ink-bounds
   reads its span; per-run glyphs read theirs. The :417–424 filter and
   :434–438 filterv die. Fix T3 in `shaped-segments` (+ its §5 block-greedy
   sibling). Prove G+C+R on a pathological line (G1).
2. **Carry the Contract-T result and reuse it by layout key.**
   `ground-block-layout` gains the live provider through ONE declared
   provider-handle seam — THE T13 route, named: `ground !refs → :atoms →
   :!active-font → :layout-provider → metrics/:geom → ground-block-layout`
   (`!refs` ground.cljs:85 filled by `install-ground!` :4578; `metrics`
   :177 already reads `!active-font`; the atom boots with the font's
   `:layout-provider`, runtime/state.cljs:92–96, and is same-id enriched
   after async load, runtime/fonts.cljs:197). NEVER read the provider from
   renderer state; NEVER boot a second shaper. `block-root-prim` and the
   other ground op emitters attach
   `:layout-result`/`:layout-line-id`/`:layout-anchor`/`:paint-source-range`
   (the `line-paint-ops` road, text_layout.cljc:527–544, already carries
   these). Reuse via the §6 cache. Ground fallback count → 0 (G2, G3).
3. **Partition invalidation.** `reconcile-slot-text-geos!` keys slot reuse
   on layout identity + a paint fingerprint instead of op-vector
   `identical?` (T5): a tint-only/hover change repacks instances from the
   carried result (paint-only repack receipt), never re-runs layout; a
   backend flip repacks for the new coverage road and reclones geo while
   layout IDs stay identical (G4 case e); camera and order changes hit
   nothing (I8). Geo lifecycle receipts split
   fresh/in-place/recloned/destroyed/capacity-grown (I6, §8).
4. **Re-profile the real corpus** — the 172-block/586,927-char cold boot AND
   the seven-transition hover storyboard (§9), with the COMMITTED harness
   (§9 deliverable), receipts opening with the adapter attestation.
5. **Only if the §10 experience bars still fail** (terminal classification
   LINEAR-CORRECTION GREEN / EXPERIENCE-BAR RED), the next mechanism is
   authorized FROM THE EVIDENCE by a ruling (Fable adjudicates, Sid vetoes):
   paint-delta handling, viewport-first residency, streamed/budgeted
   population, or another profile-supported correction. Nothing is
   pre-authorized (T11); this contract does not widen itself.

## 8. Permanent instrumentation — bounded observability, not spam

New/extended counters, ALL bounded (fixed-size maps, no per-frame console;
the G8 line gains fields; the ground report gains ≤6 lines):

- `fallback` — per-surface renderer fallback counts (I3).
- `layout-execs` — layout executions keyed by semantic cause
  (`:cold-populate` `:slot-text-change` `:fold-projection`
  `:provider-change` `:hover-paint` `:selection` `:camera` `:order`
  `:other` — `:hover-paint`/`:selection`/`:camera`/`:order` MUST stay 0;
  G3/G4/G5 assert).
- `paint-repacks` — paint-only repacks (layout reused), keyed by the same
  cause vocabulary (G3 asserts an exact `:hover-paint` count).
- `geo` — fresh / in-place / recloned / destroyed / capacity-grown counts.
- `proportionality` — last-layout work receipts in G1's exact vocabulary
  (glyph-visits, cluster-index-writes/reads, run-index-writes/reads,
  wrap-candidate-visits, shape-calls) so linearity is observable in the
  field.
- `provider-fault` — §5.6's totality receipt.
- `dirty` — dirty-region coverage (extends the existing dirty-rect receipt).
- `store-frame-execs` — I9's counter.
- `layout-cache` — size + hit/miss + `:id-digest` (T9's bound and G4's
  identical-IDs receipt made visible).

Dev-exposed hooks (diagnostics namespace, allowlist files only, no product
surface): counter RESET (the G3/G4/G5 window opener), id-digest read, and
the scripted probe actions G4/G5 name (truth-road text edit, fold-verb
toggle, backend flip via the settings road, font change, block delete).

## 9. Deliverables besides code

- **The committed profile harness** — `test/render_engine/
  profile_shaping_correction.mjs` (new file, beside the existing verifiers,
  never editing them), modes `cold` | `hover` | `probe`, driving the dev
  app under §10's environment law. Receipt's FIRST field: serialized
  adapter attestation (`isFallbackAdapter`, description); then environment
  fields, then measurements; SHA-256 manifest over emitted artifacts. Exit
  law: `0` = every assertion for the mode green; `2` = counters/receipts
  green but a wall bar red (the terminal-classification road); `1` =
  harness/environment fault — the gate DID NOT RUN, receipt informational.
- **The seven-transition hover storyboard** (THE one storyboard; §7 step 4,
  G3, and G9 all reference THIS definition): pin the ordinary block and the
  24,891-char paste block `…ep:3c6512e2:000000`; hover the largest once as
  UNMEASURED warm-up; RESET counters; then SIX measured alternating
  transitions (largest→ordinary, ordinary→largest, ×3); then the SEVENTH
  measured transition ordinary→empty (the control). Each transition waits
  for exactly ONE post-input RAF/G8 pair; a missing or extra pair = FAIL.
  (The banked four-transition capture remains the comparison baseline:
  each measured ordinary↔largest transition compares against its
  6.0–6.1s ancestors individually.)
- **The JVM test namespace** — `app.client.workspace.shaping-correction-test`
  (new file `test/app/client/workspace/shaping_correction_test.clj`) with an
  injectable synthetic provider (text_layout is `.cljc`; a provider is a map
  with `:shape-line` — no browser needed). Home of G1's linearity +
  semantic cases, G6's span assertions, and §6's key-law assertions.
- **The new fence** — `test/render_engine/
  verify_shaping_correction_fence.mjs` (G6's static half).
- Post-fix profile receipts appended to the package (`RECEIPTS.md` or the
  phase artifact) — corpus identity restated (the paste block
  `…ep:3c6512e2:000000` must still exist; corpus drift is recorded, not
  hidden).
- The phase artifact with the diff-derived changed-file list sum-checked
  against §12 (skill law).

## 10. Acceptance gates (G1–G10 · sum-check: ONE phase owns all ten; the
FULL-tier gate review re-verifies independently; no Sid-owned gate blocks
this package — his wear is the campaign's standing act and SEAM-STEP1
closure's felt gates run after this lands)

### Environment law (binds G2–G5, G8, G9)

- **Machine:** the founding-capture dev box. Every receipt records
  hostname, CPU model, OS version, and the WebGPU adapter description;
  `isFallbackAdapter` is serialized FIRST — a fallback adapter (e.g.
  SwiftShader) makes the receipt UNCLASSIFIABLE and the gate does not run
  (scene-substrate G4 lesson).
- **Browser:** headless Chromium, major version ≥ 150, version recorded.
- **Viewport:** 800×601, DPR 1.046875 (the founding capture's geometry).
- **App:** the shadow-cljs `:dev` build served by the running dev server;
  build id and URL recorded.
- **Profile:** fresh disposable user-data-dir per COLD run (no warm cache);
  `hover`/`probe` modes may reuse the settled page and say so in the
  receipt.
- **Corpus:** 172 blocks / 586,927 served chars / 177 live slots, paste
  block `…ep:3c6512e2:000000` present; drift is recorded in the receipt,
  never hidden — bar comparisons hold only on the matching corpus.
- **Counter window:** counters boot at zero on page load; cold gates read
  ABSOLUTE values at settle; `hover`/`probe` gates call the §8 reset hook
  after warm-up and read DELTAS.
- **Settle definition:** cold visual settle = navigation start → the RAF
  completing the LAST frame in which any slot text geo reshaped (the final
  G8 line), confirmed by 2,000ms of RAF/report quiet AND live slot count =
  177. Timeout 120s → the gate FAILS (not a stop).
- **Cross-run comparison policy:** comparison against the founding numbers
  is valid ONLY when machine, Chromium major, viewport/DPR, corpus, and a
  non-fallback adapter all match the founding capture; on any mismatch the
  receipt stands alone (informational) and NO bar verdict is issued — the
  gate is unclassified and re-runs under this law.

### Experience bars

**Hover bar = 52ms input→RAF** (the standing product bar); **cold bar =
12.0s visual settle** on the environment above. The cold bar is a
contract-author choice from the evidence (non-text cold floor ≈7s today =
35.678−28.7; the bar demands text stop dominating cold settle) — Sid
redlines the number at will; it decides §7 step 5, it does not soften I4.
Deliberately NOT gated: a per-frame ceiling during cold populate — a frame
ceiling would smuggle in streaming/residency, which is step-5 material.

### The gates

- **G1 (JVM, executable).** Command:
  `clj -M:test -e "(require 'app.client.workspace.shaping-correction-test) (let [r (clojure.test/run-tests 'app.client.workspace.shaping-correction-test)] (System/exit (+ (:fail r) (:error r))))"`
  — expected exit 0.
  **Counter vocabulary (exact, sum-checked):** `glyph-visits`,
  `cluster-index-writes`, `cluster-index-reads`, `run-index-writes`,
  `run-index-reads`, `wrap-candidate-visits`; **work-units := the sum of
  exactly those six.** `shape-calls` is counted separately. No operation
  the layout performs on glyph/cluster/run/candidate records is outside
  this vocabulary (an uncounted scan is a G1 defect, not a loophole).
  **Linearity, constants bound:** on the pathological fixture (ONE source
  line, ≥4,000 clusters, ≥3 runs, mixed break-whitespace, cluster-monotonic
  glyph order): work-units ≤ **4·(G+C+R)** covering index build AND §5
  wrap-cut selection. Shuffled-glyph fixture (non-monotonic): work-units ≤
  4·(G+C+R) + **G·⌈log₂(G+1)⌉** (the one permitted sort, its comparisons
  counted as glyph-visits). Growth: work-units(4× input) ≤ **5 ×
  work-units(1× input)**. Shaping: per source line, shape-calls ≤ 1 +
  final-segment count; plus 1 per header (§5.5) and exactly 1
  reference-advance shape per layout key (§5.7).
  **Semantic cases (each asserts exact line text, `:source-range`,
  `:consumed-range`, caret stops, selection regions, layout-ID stability
  across identical inputs, and shape-call count):** leading space; one
  break space; multiple break spaces (`"abc   def"` = §5.3's worked
  example); tab as break-whitespace; NBSP never a break; no fitting
  cluster (single-cluster progress); mid-word hard cut; empty text;
  interior and trailing empty source lines preserved; trailing whitespace
  painted (never break-consumed); a header longer than the budget stays
  unwrapped; two headers + wrapped body (header index domain asserted);
  proportional space advance (synthetic provider's distinctive U+0020
  advance drives inline-size — no hard-coded constant); a cluster spanning
  multiple UTF-16 code units never split (provider-fault path returns the
  §5.6 poisoned-total result, `provider-fault` incremented).
  **Key-law assertions (§6):** binding-only material revision → EQUAL key;
  header-copy projection change → unequal; fold display change → unequal;
  the key fn's signature admits only §6 inputs (the rebuild `sig` is
  unpassable).
  **Seeded negatives (the gate must be able to fail):** a variant with the
  old per-cluster whole-glyph filter exceeds the work-unit bound; a variant
  with a full-vector per-op scan fails G6's span assertion.
- **G2 (live, counter).** Harness `cold` mode, environment law. At settle:
  ground-surface fallback count = 0; every remaining fallback belongs to an
  I3-named surface and its per-surface counter is present in the report.
- **G3 (live, counter + wall).** Harness `hover` mode running THE
  seven-transition storyboard (§9): layout-execs delta 0 across ALL causes;
  paint-repacks `:hover-paint` delta = **13** (6 measured ordinary↔largest
  transitions × 2 slots + the control's 1); each of the seven measured
  transitions ≤ 52ms input→RAF; exactly one RAF/G8 pair per transition.
- **G4 (live, receipt).** Harness `probe` mode, scripted cases via the §8
  hooks, counters reset before each case, expected deltas EXACT:
  - **(a) same-glyph-count text edit** (replace one non-whitespace ASCII
    char with another, same cluster/glyph count, pinned ordinary block,
    truth-road hook): layout-execs `:slot-text-change` +1 · paint-repack
    +1 · geo in-place +1 · clone/destroy/capacity-grown 0.
  - **(b) hover flip** old→new: layout 0 · paint-repacks `:hover-paint`
    +2 · geo lifecycle 0.
  - **(c) caret move; text-selection move; machine-selection move** (three
    probes): layout 0 each; `:selection` stays 0.
  - **(d) machine fold toggle; paste flip** (two probes): layout-execs
    `:fold-projection` +1 (exactly the affected block) · paint-repack +1;
    no other block's counters move.
  - **(e) backend MSDF↔Slug flip** (settings road, provider handle
    unchanged — PRECONDITION receipt: `provider-identity` fields compare
    equal before/after): layout-execs 0 · `layout-cache` `:id-digest`
    EQUAL before/after · paint-repacks = live text-slot count (the new
    coverage road's repack) · geo recloned = live slot count · destroyed =
    prior slot count · in-place 0.
  - **(f) capacity-growing edit** (append a 4,096-char suffix to the
    pinned ordinary block): layout-execs `:slot-text-change` +1 ·
    paint-repack +1 · geo capacity-grown +1 for THAT slot · every other
    slot's lifecycle counters 0. (Buffer growth is its own lifecycle
    receipt, never an in-place proof.)
  - **(g) cache lifecycle:** hit/miss deltas consistent with (a)–(f); a
    scripted block delete evicts — `layout-cache` size −1. (The
    binding-only reuse row is G1's JVM key-law assertion.)
- **G5 (live, counter).** Harness `probe` mode, exact actions: camera pan =
  scripted pointer drag on empty ground, ±200px both axes; zoom = 4 wheel
  steps in + 4 out at a fixed point; order probe = fold-toggle a block
  ABOVE the observed blocks (the origin-shift cascade). Assertions:
  layout-execs `:camera` = `:order` = 0 throughout; pan/zoom:
  slot-text-write delta 0 (the G8 reshape counter) AND all geo lifecycle
  counts 0; order probe: total layout delta = 1 (the folded block,
  `:fold-projection`), slot-text-writes delta = 1, every origin-shifted
  block 0.
- **G6 (JVM + fence, executable).** The span-selection helpers live in
  shared `text_layout.cljc`; the renderer's paint/clip paths call them
  (I5). JVM half (same G1 command/namespace): per-op visited glyph records
  ≤ **span-glyph-count + 8**, where the visible span is in GLYPH-INDEX
  units — the glyphs whose cluster `:source-range` start offsets lie
  within the op's paint range; line resolution goes through the
  line-id→line index — visited lines ≤ 2 (the requested id + one nearest
  fallback); the seeded full-scan negative FAILS these assertions. Static
  half: `node test/render_engine/verify_shaping_correction_fence.mjs` —
  expected exit 0 — asserts the renderer paint/clip roads route through
  the indexed helpers and carry no full-vector scan forms (the I5 named
  offenders stay dead).
- **G7 (live, receipt).** `store-frame-execs` present in the ground report,
  bounded, and NOT used to authorize anything (a doc assertion in the phase
  artifact).
- **G8 (live, wall + receipts).** Harness `cold` mode under the environment
  law, settle per its definition: cold visual settle ≤ **12.0s**; receipt
  opens with the adapter attestation; full RAF/G8/report lines banked.
- **G9 (live, wall + receipts).** Harness `hover` mode, THE
  seven-transition storyboard: all SEVEN measured transitions ≤ **52ms**
  input→RAF (six ordinary↔largest + the ordinary→empty control).
- **G10 (suites, byte-fidelity — every command pinned with its expected
  result):**
  1. G1/G6 namespace (command above) → exit 0.
  2. Focused suites, one run, expected exit 0:
     `app.client.workspace.text-layout-test`,
     `app.client.workspace.block-edit-test`,
     `app.client.workspace.ground-edit-test`,
     `app.client.workspace.scene-store-test`,
     `app.client.substrate.scene-tape-test`,
     `app.client.substrate.maintained-view-test`, `app.anatomy-test`,
     `app.face-primitives-test`, `app.face-projection-test` (same
     `clj -M:test -e` runner form, exit = fail+error sum).
  3. `node test/render_engine/verify_text_layout_fence.mjs` → exit 0 and
     `node test/render_engine/verify_scene_tape_fence.mjs` → exit 0 (run,
     never edited).
  4. `node test/render_engine/verify_shaping_correction_fence.mjs` →
     exit 0.
  5. `npm run verify:render-engine` → **expected process exit 1** with the
     structured close matching Contract T's registered state (W1.md §9.1):
     classification `candidate-pick-parity-failure`; determinism 21/21;
     golden comparison 21/21 byte-identical; current-product
     bounds-divergence sentinels 7/7; candidate parity 14/21; SDF 7/7
     green; Slug 7/7 green with exactly two declared byte-128 ties per
     regime; MSDF RED 7/7 with 47 decisive mismatches per regime (329
     total) plus two correctly separated ties per regime. ANY other exit
     code, classification, or count = G10 FAIL.
  6. CLJS compile: `clj -M:dev -m shadow.cljs.devtools.cli compile dev` →
     exit 0 with NO new distinct warnings against the BASELINE captured by
     running the identical command at phase open BEFORE any edit (baseline
     output banked in the phase artifact).
  **Golden policy (one, contradiction-free):** per R1's verification the
  W0 engine goldens do not exercise ground block-greedy, so **21/21
  byte-identical is ABSOLUTE — any golden diff is a G10 FAIL.** A belief
  that a diff stems from §5's contracted change does not soften the FAIL:
  it fires stop **S6** (golden custody adjudication) and G10 is BLOCKED
  FOR ADJUDICATION until Fable/Sid rule whether a separately reviewed
  golden update is legal. A run is never both "21/21 byte-identical" and
  "passed by exception."

### Terminal classifications (every lawful end state has exactly one name)

- **PACKAGE PASS** — G1–G10 all green including both bars → the
  independent FULL-tier gate review (§14).
- **PACKAGE FAIL** — any of G1–G7 or G10 red → fix in phase.
- **LINEAR-CORRECTION GREEN / EXPERIENCE-BAR RED — STEP-5 RULING
  REQUIRED** — G1–G7+G10 green, a G8/G9 bar red WITH a valid
  environment-matched receipt (harness exit 2): bank all receipts, make NO
  further code change under this contract, close the implementation phase
  as technically green for G1–G7+G10, and open the Fable/Sid §7 step-5
  ruling. The overall experience gate remains red; SEAM felt-gate closure
  does not proceed. This is not S1–S6 and does not recut this contract
  unless the evidence reveals a contract defect.
- **UNCLASSIFIED (environment)** — fallback adapter, environment mismatch,
  or harness fault (exit 1): the gate DID NOT RUN; receipt informational;
  re-run under the environment law. Never red, never green.

## 11. Input manifest (machine-verified 2026-08-04 — contract cut + the R1
round's full re-grep + recut spot-checks; hints, substance binds)

- `src/app/client/workspace/text_layout.cljc` (797 ln): `shaped-layout` :328;
  per-cluster filter :417–424; per-run filterv :434–438; `union-bounds` :320;
  `shaped-segments` :283–307; `block-wrap-lines` :80; `wrap-line` :54;
  `visual-lines` :99; `source-line-records` :265; `whitespace-at?` :278;
  `layout` :506; `line-paint-ops` :527–544; `provider-identity` :261;
  `clip-result` :660 (line filter :669–671); `hit-test-result` :766;
  `caret-result` :590; `selection-result` :638.
- `src/app/client/workspace/face_primitives.cljc` (1,324 ln): `text-op` :550;
  `wrap-lines` :559; `block-render-lines` :572; `ground-block-layout`
  :587–616; `block-root-prim` :618 (bare ops from :648; header consumption
  :638–660); selection/caret convergence :701–708, :746–753; auxiliary
  prims (`block-refusal-prim` :755, `block-notice-prim` :767,
  `block-boundary-prim` :778, `block-conflict-lint-prim` :787,
  `block-gold-mark-prim` :811, `block-silver-mark-prim` :827,
  `block-fold-header-hit-prim` :846).
- `src/app/client/substrate/webgpu/renderer.cljs` (2,386 ln):
  `destroy-text-system!` :850; `clone-text-system` :1100; layout line-index
  memo :1440–1470; `position-text-op` :1472 (fallback :1484–1493,
  anchor-delta :1499–1501, glyph filter :1502–1510); `position-text` :1519;
  MSDF/Slug coverage-only comments :1538/:1573; `shape-text` :1603;
  `pack-msdf-instances!` :1646; `pack-slug-instances!` :1675;
  `update-text-data` :1719 (backend-mismatch guard :1723–1726).
- `src/app/client/workspace/runtime/render.cljs` (792 ln):
  `reconcile-slot-text-geos!` :25–64 (`identical?` reuse :48; vanished-slot
  destroy :61–64); `<combined-text-ops` wiring :87–96; `<store-frame` :130;
  `store-frame-changed?` :241; `text-by-vi` :248; backend-flip road
  :318–346; slot reconcile call :401; G8 log :406.
- `src/app/client/workspace/ground.cljs` (4,798 ln): `!refs` :85; `metrics`
  :177; `run-view` :1190; `machine-visual-lines` :1223–1235;
  `block-anatomy-view-model` :1253; `current-block-render-inputs` :1406
  (rebuild `sig` :1476–1481); `rebuild-block!` :1500; `context-blocks`
  :1745; `reconcile!` :1794; block-death sites :1923–1928, :3159–3167;
  fold verb :3700–3713; selection verbs :3731–3765; hover flip in
  `pointer-move!` :4047–4059; `install-ground!` :4578; truth-overlay watch
  :4603–4608; block slot upserts (`ss/upsert-slot`) :1016, :2280.
- READ-ONLY context (cited by §5/§6/§7, NOT allowlist):
  `src/app/client/workspace/ground_edit.cljc` — `paste-projection` :121
  (recut-corrected locator), `block-view` :445;
  `src/app/client/workspace/block_edit_wiring.cljs` — `!truth-overlay`
  :145, `truth-text*` :160;
  `src/app/client/workspace/runtime/state.cljs` — `:!active-font` init
  :92–96; `src/app/client/workspace/runtime/fonts.cljs` — same-id provider
  enrichment :197;
  `src/app/shared/foldable_material.cljc` — `:foldable/defaults` +
  `:foldable/header-copy` :31–40, bindings :44–69;
  `package.json` — verify scripts :5–9; `shadow-cljs.edn` — `:dev` build
  :4–11.
- `FOUNDING-EVIDENCE.md` + `evidence/` (hash manifest there).
- Contract T: `build/render-engine/W1.md` (§9.1 = G10's pinned verifier
  state); the seam constitution: decisions.md "The render seam".

Dirty-tree caveat: these source files carry uncommitted foreign work
(SEAM-STEP1 + studio/playground layers) at cut time; locators were verified
against that tree. At phase open (post-SEAM-landing), re-run the manifest
sweep (`grep -n` every symbol) before writing code; drift → re-locate + log.

## 12. Allowlist + custody (diff-derived sum-check at phase end — skill law)

MAY EDIT (code): `text_layout.cljc` · `face_primitives.cljc` · `ground.cljs`
· `webgpu/renderer.cljs` · `runtime/render.cljs`.
MAY CREATE: the §9 harness + G1 test namespace + one NEW fence script
(`test/render_engine/verify_shaping_correction_fence.mjs`); package docs
under `build/shaping-correction/`.
MUST NOT TOUCH: any `SEAM-STEP1*` file; `scene_store.cljc`,
`scene_runtime.cljs`, `scene_tape.cljc`, `editor_compute.cljs`,
`runtime/mouse.cljs`, `electric_flow.cljc`, server files, the EXISTING
fence/verify scripts; `src/app/server/env.clj` is never read. Carried
results travel inside op maps/text-by-vi values — NO store schema change; if
the phase genuinely needs a MUST-NOT file → stop S2.
Foreign work is preserved everywhere: the implementer works from a clean
worktree at the post-SEAM-landing HEAD; worktree branches are scaffolding,
never commit targets; code and docs commit separately on
`docs/current-mental-model-local`; nothing is ever pushed or merged.

## 13. Stop clauses (the implementing session never improvises policy)

- **S1** — exact legacy column breaks demanded as product identity (§5).
- **S2** — the correction cannot land inside §12's allowlist (needs a
  MUST-NOT file or a store schema change) → coordination ruling (SEAM
  custody is at stake).
- **S3** — manifest SUBSTANCE missing at phase open (post-landing drift
  beyond re-location).
- **S4** — genuine conflict between this contract and Contract T / the
  render-seam constitution / decisions.md (two readings, both physically
  implementable) → escalate with verbatim citations; never a silent pick.
- **S5** — I4's proportionality unreachable as specified under platform
  semantics (e.g. the shaping provider cannot give cluster-monotonic or
  sortable glyph order) → escalate with the probe that proves it.
- **S6** — golden custody adjudication: any W0 golden diff (G10's absolute
  21/21 law) → G10 BLOCKED FOR ADJUDICATION; Fable/Sid rule whether the
  diff is the contracted §5 change and whether a separately reviewed golden
  update is legal. Never an implementer edit, never a silent pass.
- Bars failing at G8/G9 are NOT stops — that is the LINEAR-CORRECTION
  GREEN / EXPERIENCE-BAR RED terminal classification (§10; step 5 fires).

## 14. Handoff

1. NEXT: ONE wholly new fresh default-fail validation round (R2) over this
   recut (subagent or fresh session; findings folded per the minor-fail
   law, FAIL → recut again). `VALIDATION_R1.md` is immutable and is never
   overwritten; R2 lands as `VALIDATION_R2.md`.
2. Then the Codex opening prompt (baton `NOW.md` carries it after
   validation), one fresh context, whole package, gates G1–G7+G10 run to
   green in phase; G8/G9 run in phase via the harness.
3. In-phase falsifier: ONE finder aimed at the genuinely-new machinery (the
   linearized index construction + §5 wrap cuts + the §6 key/cache —
   correctness under reuse, eviction, and revision flips).
4. Fable FULL-tier gate review (cutover-class: the text hot path is
   rewired): independent suite re-run, trap spot-checks, live re-drive of
   G3/G5 probes, receipt verification, verdict + residue into `GATE.md` +
   decisions.md evaluation notes.
5. After green: commit ruling is Sid's (code + docs separate, one branch
   law); post-commit HEAD-dynamic suite re-run; board prune to a one-line
   pointer; SEAM-STEP1 closure proceeds. Retro joins the stratum batch.

## Traps ledger (cite by number in code comments)

- **T1** per-cluster whole-glyph filter — naive: filter all glyphs per
  cluster (text_layout.cljc:417–424); failure: 24.390s inside a 28.343s
  layout (the banked profile); ruling: cluster-start→glyph-span index built
  in one pass, per-cluster span reads.
- **T2** per-run glyph rescan — naive: `filterv` all glyphs per run
  (:434–438); failure: the O(R×G) term; ruling: run→glyph-span index.
- **T3** per-suffix reshape in wrap — naive: shape the whole remaining text
  per cut (`shaped-segments` :292–307 does this today); failure: O(n²) on a
  24,891-char block; ruling: ONE shaped pass chooses all cuts; only final
  segments reshape (per-line shaping calls ≤ 1 + segments; G1 asserts).
- **T4** renderer per-op fallback layout — naive: keep the `(or existing
  (tl/layout …))` road hot for ground (renderer.cljs:1484–1493); failure:
  28.606s `position_text`, layouts minted per op per reshape; ruling:
  ground carries results; fallback counted per surface, 0 for ground.
- **T5** op-vector `identical?` as the reuse key — naive: slot reuse keyed
  on the op vector (runtime/render.cljs:48); failure: a tint-only hover
  rebuild misses reuse → 5.9s reshape ×2 blocks (the banked hover profile);
  ruling: reuse keys on §6 layout identity + paint fingerprint; paint-only
  → repack, never reshape.
- **T6** unnamed keying source — naive: key the cache on whatever map is
  handy (fast-flipping op identity, the slow address alone, or the broad
  rebuild `sig`); failure: the scene-substrate G1 class (per-keystroke
  repacks + seconds-stale copies); ruling: §6 NAMES the keying source and
  is TOTAL; derived-text fallback to value hash is declared in the key fn;
  `sig` is unpassable by signature.
- **T7** zoom in the layout key — naive: keep `:zoom` in semantic input
  (today's `shaped-layout` does); failure: every zoom step invalidates all
  layouts, I8 unreachable; ruling: zoom leaves the key; legal-zoom enforced
  at the consumer boundary.
- **T8** origin in the reuse decision — naive: hash origin into the key;
  failure: a dragged block re-layouts; ruling: results are material-local;
  the anchor-delta road (renderer.cljs:1499–1501) positions carried
  results; origin excluded.
- **T9** unbounded layout cache — naive: memoize forever by key; failure:
  172 blocks × revisions × wears leak the heap; ruling: cache bounded by
  the live block set, evicted on block death/slot destroy, size in
  diagnostics.
- **T10** instrumentation as spam — naive: per-frame console lines; failure:
  the observability itself perturbs frames and drowns receipts; ruling: §8
  bounded counters; the G8 line + ≤6 report lines.
- **T11** predicted-residual scope creep — naive: pre-build residency /
  instance deltas because "28.7−24.4 will remain"; failure: unmeasured
  mechanism mandated, the ruling's explicit prohibition violated; ruling:
  §7 step 5 only, from the step-4 profile, by ruling.
- **T12** wrap-authority duality — naive: keep legacy column cuts for ground
  while shaped wrap serves elsewhere; failure: pick/selection geometry
  disagrees with painted breaks (two truths); ruling: §5; S1 if column
  identity is claimed as product.
- **T13** second provider road — naive: ground boots its own shaper handle;
  failure: two shaping environments drift (the renderer T12 class); ruling:
  ONE provider handle through THE named seam — `ground !refs → :atoms →
  :!active-font → :layout-provider → metrics/:geom → ground-block-layout`
  (§7 step 2); never renderer state, never a second shaper boot.
- **T14** `\s` as break-whitespace — naive: reuse `whitespace-at?`'s `\s`
  class for §5 cuts (text_layout.cljc:278–281); failure: JVM and CLJS
  regex `\s` disagree on U+00A0 NBSP — the same `.cljc` line wraps
  differently on the two platforms G1/G6 straddle; ruling: §5.2's explicit
  set {U+0020, U+0009}; NBSP never admits a cut.
