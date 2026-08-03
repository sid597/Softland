# shaping-correction — CONTRACT (one phase · implementer: Codex lane · gate: Fable, FULL tier)

Cut 2026-08-04 from Sid's settled ruling (the 2026-08-03 profiling
adjudication) + `FOUNDING-EVIDENCE.md` (the banked capture) + fresh source
reconnaissance, under the work-package skill's few-and-large rule: ONE
implementation phase, no PLAN.md, plan-grade specificity carried here. One
fresh default-fail validation round runs over this contract before the
implementer opens (see §14).

**Binding docs + precedence:** decisions.md (incl. "The render seam",
settled+amended 2026-08-03), Contract T (`build/render-engine/W1.md`), and
THIS contract bind. `NOW.md` is the baton — if it contradicts any of them,
they win; flag the discrepancy in NOW, do not pause. `FOUNDING-EVIDENCE.md`
is the evidence record: its numbers are facts, its readings are prior-pass
records, not authority.

**Manifest law:** locators below are navigation hints machine-verified with
`grep -n`/`wc -l` on 2026-08-04 against the WORKING TREE (which carries
uncommitted foreign work — §12). SUBSTANCE (named symbols, forms, semantics)
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
evidence-gated (§7 step 5), never presumed.

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
Hover: three ordinary↔largest transitions 6,028–6,124ms input→RAF, 2 slots
reshaped each; the ordinary→empty control is 20.7ms. Corpus: 172 blocks /
586,927 served chars / 177 live slots. GPU execution, paint, packing,
upload, draw, and sampled store-frame work are NOT the present hot share.
The historical 2–3-minute loading is UNEXPLAINED RESIDUE — recorded, never
attributed (invariant I10). The capture lacks a serialized adapter
attestation; every receipt this contract orders opens with one.

## 4. The invariants (I1–I10 — the ruling, made executable)

- **I1 — one material-local shaped layout authority.** Ground text has ONE
  Contract-T layout result per (block visual text × §6 key), minted through
  `text-layout/layout` with the live provider, and THAT result is carried to
  measure, wrap, bounds, caret, selection, clipping, hit testing, and paint.
  No reader re-derives geometry (Contract-T's law, now enforced on ground's
  hot path).
- **I2 — declared layout key + reuse.** The §6 key is declared in code (one
  fn, one docstring naming the keying source); hover, selection, and
  paint-only semantic rebuilds REUSE the carried result (layout-executions
  delta 0 — G3).
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
  reshape, total shaping work ≤ 2× the line's text).
- **I5 — indexed spans downstream.** Clipping, paint ranges, and per-op
  glyph selection use indexed glyph/cluster spans (cluster-start→glyph-span,
  run→glyph-span, line-id→line via the existing memo road,
  renderer.cljs:1440–1470), never repeated full-vector searches
  (`clip-result`'s line filter text_layout.cljc:669–671 and
  `position-text-op`'s per-op glyph filter renderer.cljs:1502–1510 are the
  named offenders).
- **I6 — three distinct lifecycles, three distinct receipts.** Layout
  invalidation (§6 key change) ≠ paint invalidation (repack/tint) ≠
  GPU-geometry lifecycle (clone/in-place/destroy in
  `reconcile-slot-text-geos!`, runtime/render.cljs:25–64). Each has its own
  counter; no receipt conflates them.
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
preserved; the mechanism changes:

- Hard breaks: explicit `\n` always breaks (source-line records preserved,
  incl. trailing empties — the `-1` split limit semantics).
- Greedy progression: fill each visual line with shaped clusters until the
  next candidate cut exceeds the inline-size budget. Candidate cuts are
  declared cluster ends from ONE shaped pass over the whole source line
  (T3 — never a per-suffix reshape loop).
- Whitespace preference: prefer the LAST cut whose boundary follows
  whitespace and fits; the break-consumed whitespace is not painted on
  either line (legacy's `last-index-of " "` + `triml` policy, expressed
  over cluster geometry).
- No fitting whitespace cut → hard-cut at the last fitting cluster end
  (mid-word), minimum one cluster per line (progress guarantee).
- Final segments reshape once each for line-correct bidi/contextual shaping
  (same law `shaped-segments` already states).
- Budget translation: a block's `:wrap-col` maps ONCE, at the layout seam,
  to `inline-size = wrap-col × reference-advance(font-size)` where
  reference-advance is the provider's shaped advance of U+0020 at that
  font-size — deterministic, part of the §6 key. No character-column cut
  survives as an authority anywhere on the ground path.

**Stop S1:** if exact legacy column breaks turn out to be product identity
(anyone asks the new wrap to reproduce the old cut points), STOP for Sid —
that demand conflicts with Contract T's one-geometry-truth law and is not an
implementation detail. Expected and accepted instead: machine blocks'
visible line breaks CHANGE (proportionally chosen); unwrapped blocks'
breaks do not.

## 6. Question B — layout identity vs paint-resource identity (RESOLVED)

**The layout key** (I2's declared key; one fn owns it) hashes exactly:

- Source identity — KEYING SOURCE NAMED: the block address + the revision of
  its served truth + every input that changes the VISUAL text (fold state /
  foldable wear, headers, wrap-col, machine display derivation). Where a
  revision stamp is not yet threaded (machine display text is derived), the
  key falls back to the visual-text value hash — DECLARED in the key fn,
  never silent (scene-substrate keying-source law).
- Provider identity — `provider-identity`'s fields (face-id, face-revision,
  shaper-id, shaper-version, features, variations, axes, fallback-chain,
  upem; text_layout.cljc:261–263).
- Metric regime — font-size, line-height, baseline-offset, wrap policy +
  inline-size (§5 translation included), language/direction, tab-stops,
  index-space version.

**NOT in the key** (may never invalidate layout): origin/position (carried
results are positioned by the existing anchor-delta road,
renderer.cljs:1499–1501 — a moved block never re-layouts), zoom/camera
(Contract-T legal-zoom stays enforced, moved to the consumer boundary — T7),
colors/tint/style/selection/caret/hover state, paint backend (MSDF vs Slug),
atlas coverage, GPU buffer/geo lifecycle. Grounds: "Placement/advance came
from Contract T. MSDF selects coverage metadata only" (renderer.cljs
:1538–1539, and the Slug twin :1573). A backend flip reclones GPU resources
(I6's third lifecycle) and MUST reuse layout results — unless it
demonstrably changes the shared metric regime, which is a receipt-bearing
claim, never an assumption.

**Cache lifecycle (T9):** the carried-result cache is process-local, keyed
by the §6 key, bounded by the live block set — evicted on block death/slot
destroy; its size is a diagnostics field. No unbounded memo.

## 7. The correction, ordered — and what the gates decide

1. **Linearize cluster/run membership** inside `shaped-layout`: one pass per
   line builds cluster-start→glyph-span and run→glyph-span indexes (sort
   once if glyph order isn't cluster-monotonic); per-cluster ink-bounds
   reads its span; per-run glyphs read theirs. The :417–424 filter and
   :434–438 filterv die. Fix T3 in `shaped-segments` (+ its §5 block-greedy
   sibling). Prove G+C+R on a pathological line (G1).
2. **Carry the Contract-T result and reuse it by layout key.**
   `ground-block-layout` gains the live provider through ONE declared
   provider-handle seam (T13 — never a second shaping road);
   `block-root-prim` and the other ground op emitters attach
   `:layout-result`/`:layout-line-id`/`:layout-anchor`/`:paint-source-range`
   (the `line-paint-ops` road, text_layout.cljc:527–544, already carries
   these). Reuse via the §6 cache. Ground fallback count → 0 (G2, G3).
3. **Partition invalidation.** `reconcile-slot-text-geos!` keys slot reuse
   on layout identity + a paint fingerprint instead of op-vector
   `identical?` (T5): a tint-only/hover change repacks instances from the
   carried result (paint-only repack receipt), never re-runs layout; camera
   and order changes hit neither (I8). Geo lifecycle receipts split
   fresh/in-place/recloned/destroyed (I6).
4. **Re-profile the real corpus** — the 172-block/586,927-char cold boot AND
   the ordinary↔24,891-paste hover storyboard, with the COMMITTED harness
   (§9 deliverable), receipts opening with the adapter attestation.
5. **Only if the §10 experience bars still fail**, the next mechanism is
   authorized FROM THE EVIDENCE by a ruling (Fable adjudicates, Sid vetoes):
   paint-delta handling, viewport-first residency, streamed/budgeted
   population, or another profile-supported correction. Nothing is
   pre-authorized (T11); this contract does not widen itself.

## 8. Permanent instrumentation — bounded observability, not spam

New/extended counters, ALL bounded (fixed-size maps, no per-frame console;
the G8 line gains fields; the ground report gains ≤6 lines):

- `fallback` — per-surface renderer fallback counts (I3).
- `layout-execs` — layout executions keyed by semantic cause
  (`:cold-populate` `:slot-text-change` `:hover-paint` `:selection`
  `:camera` `:order` `:other` — `:camera`/`:order` MUST stay 0, G5).
- `paint-repacks` — paint-only repacks (layout reused).
- `geo` — fresh / in-place / recloned / destroyed counts.
- `proportionality` — last-layout work receipts (glyphs, clusters, runs,
  work-units visited) so linearity is observable in the field.
- `dirty` — dirty-region coverage (extends the existing dirty-rect receipt).
- `store-frame-execs` — I9's counter.
- `layout-cache` — size + hit/miss (T9's bound made visible).

## 9. Deliverables besides code

- The committed profile harness (new files; suggested home
  `test/render_engine/` beside the existing verifiers, never editing them):
  boot→settle capture + the four-transition hover storyboard + summary
  emission with SHA-256 manifest AND serialized adapter attestation
  (`isFallbackAdapter`, description) as the receipt's FIRST field. The
  harness is what makes §7 step 4 and every future re-profile reproducible.
- A JVM proportionality test namespace (G1) with an injectable synthetic
  provider (text_layout is `.cljc`; a provider is a map with `:shape-line` —
  no browser needed).
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

Experience bars first, so nothing downstream re-litigates them:
**hover bar = 52ms input→RAF** (the standing product bar); **cold bar =
12.0s visual settle** on the same corpus/machine class. The cold bar is a
contract-author choice from the evidence (non-text cold floor ≈7s today =
35.678−28.7; the bar demands text stop dominating cold settle) — Sid
redlines the number at will; it decides §7 step 5, it does not soften I4.
Deliberately NOT gated: a per-frame ceiling during cold populate — a frame
ceiling would smuggle in streaming/residency, which is step-5 material.

- **G1 (JVM, executable):** pathological-line proportionality — one source
  line with ≥4,000 clusters, multiple runs, mixed whitespace: assert
  work-unit counters ≤ k·(G+C+R) and 4×-input → ≤ c·4× work (both cluster/run
  index build and §5 wrap-cut selection); assert total shaping calls per
  source line ≤ 1 + segment count (T3 dead).
- **G2 (live, counter):** after cold populate of the real corpus, ground
  surface fallback count = 0; every remaining fallback belongs to an I3-named
  surface and is counted per surface.
- **G3 (live, counter + wall):** the hover storyboard (ordinary ↔ 24,891-char
  paste block, both directions ×3): layout-executions delta 0, paint-only
  repack receipts present, hover input→RAF ≤ 52ms per transition.
- **G4 (live, receipt):** the three lifecycles report distinctly (I6/§8) and
  a scripted probe shows each: text edit → layout-exec + repack + in-place
  geo; hover → repack only; backend flip → reclone only (layout reused).
- **G5 (live, counter):** camera-only pan/zoom storyboard AND an order-only
  change: layout-execs `:camera`/`:order` = 0 AND text-geometry writes = 0.
- **G6 (executable):** per-op scan counters on the paint/clip path assert no
  full-vector line/cluster scan: per-op visited elements ≤ visible-span
  size + k (the I5 offenders re-routed through indexes).
- **G7 (live, receipt):** `store-frame-execs` present in the ground report,
  bounded, and NOT used to authorize anything (a doc assertion in the phase
  artifact).
- **G8 (live, wall + receipts):** cold re-profile via the committed harness
  on the 172-block/586,927-char corpus — receipt opens with adapter
  attestation; cold visual settle ≤ 12.0s; full RAF/G8/report lines banked.
- **G9 (live, wall + receipts):** hover re-profile via the harness — all
  three ordinary↔largest transitions ≤ 52ms input→RAF; the ordinary→empty
  control stays ≤ 52ms.
- **G10 (suites, byte-fidelity):** existing fences untouched and green
  (`verify_text_layout_fence.mjs`, `verify_scene_tape_fence.mjs` — run, not
  edited); W0 golden bank 21/21 byte-identical; the MSDF 47-mismatch
  counterexample stays RED; focused text/face/scene suites green; cljs
  compile 0 new warnings. Any golden diff = FAIL unless the gate traces it
  to §5's contracted wrap change on a golden that exercises block-greedy —
  then adjudication, never a silent pass.

Failure of G8/G9 bars with G1–G7+G10 green is NOT a package failure — it is
§7 step 5 firing: bank the profile, open the ruling. Failure of G1–G7/G10 is
a package failure (fix in phase).

## 11. Input manifest (machine-verified 2026-08-04; hints, substance binds)

- `src/app/client/workspace/text_layout.cljc` (797 ln): `shaped-layout` :328;
  per-cluster filter :417–424; per-run filterv :434–438; `union-bounds` :320;
  `shaped-segments` :283–307; `block-wrap-lines` :80; `wrap-line` :54;
  `layout` :506; `line-paint-ops` :527–544; `provider-identity` :261;
  `clip-result` :660 (line filter :669–671); `hit-test-result` :766;
  `caret-result` :590; `selection-result` :638.
- `src/app/client/workspace/face_primitives.cljc` (1,324 ln): `text-op` :550;
  `wrap-lines` :559; `block-render-lines` :572; `ground-block-layout`
  :587–616; `block-root-prim` :618 (bare ops from :648).
- `src/app/client/substrate/webgpu/renderer.cljs` (2,386 ln):
  `destroy-text-system!` :850; `clone-text-system` :1100; layout line-index
  memo :1440–1470; `position-text-op` :1472 (fallback :1484–1493,
  anchor-delta :1499–1501, glyph filter :1502–1510); `position-text` :1519;
  MSDF/Slug coverage-only comments :1538/:1573; `shape-text` :1603;
  `update-text-data` :1719.
- `src/app/client/workspace/runtime/render.cljs` (792 ln):
  `reconcile-slot-text-geos!` :25–64 (`identical?` reuse :48);
  `<combined-text-ops` wiring :87–96; `<store-frame` :130;
  `store-frame-changed?` :241; `text-by-vi` :248; G8 log :406.
- `src/app/client/workspace/ground.cljs` (4,798 ln): `machine-visual-lines`
  :1223–1235; `block-anatomy-view-model` :1253; `rebuild-block!` :1500;
  `reconcile!` :1794; hover flip in `pointer-move!` :4047–4059; block slot
  upserts (`ss/upsert-slot`) :1016, :2280.
- `FOUNDING-EVIDENCE.md` + `evidence/` (hash manifest there).
- Contract T: `build/render-engine/W1.md`; the seam constitution:
  decisions.md "The render seam".

Dirty-tree caveat: these source files carry uncommitted foreign work
(SEAM-STEP1 + studio/playground layers) at cut time; locators were verified
against that tree. At phase open (post-SEAM-landing), re-run the manifest
sweep (`grep -n` every symbol) before writing code; drift → re-locate + log.

## 12. Allowlist + custody (diff-derived sum-check at phase end — skill law)

MAY EDIT (code): `text_layout.cljc` · `face_primitives.cljc` · `ground.cljs`
· `webgpu/renderer.cljs` · `runtime/render.cljs`.
MAY CREATE: the §9 harness + G1 test namespace + one NEW fence script (e.g.
`test/render_engine/verify_shaping_correction_fence.mjs`); package docs under
`build/shaping-correction/`.
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
- Bars failing at G8/G9 are NOT stops (§10 last paragraph — step 5 fires).

## 14. Handoff

1. NEXT: ONE fresh default-fail validation round over this contract
   (subagent or fresh session; findings folded per the minor-fail law, FAIL
   → recut). Never overwrite a FAIL artifact.
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
  handy (fast-flipping op identity, or the slow address alone); failure:
  the scene-substrate G1 class (per-keystroke repacks + seconds-stale
  copies); ruling: §6 NAMES the keying source; derived-text fallback to
  value hash is declared in the key fn.
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
  ONE provider handle through one declared seam from the font boot.
