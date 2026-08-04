# shaping-correction — CONTRACT (one phase · implementer: Codex lane · gate: Fable, FULL tier)

Cut 2026-08-04 from Sid's settled ruling (the 2026-08-03 profiling
adjudication) + `FOUNDING-EVIDENCE.md` (the banked capture) + fresh source
reconnaissance, under the work-package skill's few-and-large rule: ONE
implementation phase, no PLAN.md, plan-grade specificity carried here.

**RECUT 2026-08-04 (R3 candidate):** this text consumes `VALIDATION_R2.md`
(immutable FAIL) §10 items 1–9 — and, through them, completes
`VALIDATION_R1.md` §10 items 1–8 in EXECUTABLE SUBSTANCE (R2's consumption
standard: letter-only inclusion is not consumption). The mapping is
`RECUT_LEDGER_R2.md` (R1's map: `RECUT_LEDGER_R1.md`, historical). One
wholly new default-fail validation round (R3) runs over THIS text before
the implementer opens (§14). R1 and R2 are immutable; no implementation
opens from this candidate.

**Binding docs + precedence:** decisions.md (incl. "The render seam",
settled+amended 2026-08-03), Contract T (`build/render-engine/W1.md`), and
THIS contract bind. `NOW.md` is the baton — if it contradicts any of them,
they win; flag the discrepancy in NOW, do not pause. `FOUNDING-EVIDENCE.md`
is the evidence record: its numbers are facts, its readings are prior-pass
records, not authority.

**Manifest law:** locators below are navigation hints machine-verified with
`grep -n`/`wc -l` on 2026-08-04 against the WORKING TREE (which carries
uncommitted foreign work — §12), re-verified by the R1 and R2 rounds'
re-greps and this recut's machine extractions (W1 §9.1's fingerprint,
`run_verifier.mjs`'s close fields, `server_jetty.clj`'s port default —
all read from disk 2026-08-04, never hand-copied). SUBSTANCE (named
symbols, forms, semantics) binds. Hint drift with substance intact →
re-locate, log in the phase artifact, never stop. Substance missing →
stop clause S3.

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

Per source line, from ONE shaped pass (T3). The law is SEGMENT-RELATIVE:
the **current segment** is the not-yet-cut suffix of the source line —
initially the whole line; after each cut, the suffix beginning at that
cut's end (the consumed run's end for a break cut, the cut cluster end
for a hard cut). Candidate search, prefix definition, and the fit test
read the CURRENT SEGMENT only, never the whole source line; the same
segment content yields the same cut decision regardless of how many cuts
preceded it.

1. **Segment-leading whitespace is painted, never a candidate.** A
   maximal break-whitespace run that starts at the current segment's
   first offset is painted content on that segment's first visual line.
   This ONE rule covers source-line indentation (the first segment) AND
   any later segment that begins with whitespace (e.g. the remainder
   after a hard cut inside a long whitespace run). It is never
   break-consumed; if it alone overflows the budget, rule 3's hard cut
   applies inside it.
2. **Break candidates.** Candidate cuts are declared cluster ends. A
   **break candidate** is the end of a maximal break-whitespace cluster
   run that (i) starts AFTER the current segment's first offset — at
   least one painted cluster precedes it in THIS segment (so a
   segment-leading run is excluded by construction, and an empty painted
   prefix can never be chosen) — and (ii) has non-whitespace content
   after it in the current segment. (Trailing whitespace at
   end-of-source-line is therefore never break-consumed — it is painted
   content, hard-wrapped by rule 3 if it overflows.)
   **Whitespace preference:** choose the LAST break candidate whose
   PAINTED PREFIX (all current-segment clusters before the whitespace
   run) fits the inline-size budget. The fit test applies to the painted
   prefix only — the consumed whitespace is unpainted, so its advance
   never participates. The ENTIRE maximal break-whitespace run is
   consumed (`:consumed-range`), painted on neither line. The next
   current segment starts at the run's end.
3. **Hard cut:** if no break candidate's painted prefix fits, cut at the
   last fitting cluster end (mid-word). If not even one cluster fits,
   take exactly one cluster (progress guarantee). Hard cuts consume
   nothing; the next current segment starts at the cut cluster end.
4. Continuation lines never begin with break-consumed whitespace, by
   construction of rule 2 (a consumed run is consumed WHOLE); when a
   continuation segment does begin with whitespace (hard-cut remainder),
   rule 1 paints it.
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
body's shape options.

**Header index space (EXACT — this schema, not an example):** header
indices live in a PER-HEADER tagged domain. A header offset is
`[:header h j]` — `h` = the header's 0-based ordinal in the block's
visible header order, `j` = an offset into THAT header's text in the same
index units as body source offsets (UTF-16 code units), `0 ≤ j ≤ len`.
A header range is `[:header h [start end)]`, `start`/`end` in the same
per-header domain; a range never spans two headers and never mixes with
body offsets. Consequences, each exact:
- a header line's `:source-range` = `[:header h [0 len)]`, `:text` = the
  full header text, `:consumed-range` ALWAYS absent (headers never wrap,
  nothing is ever consumed);
- caret stops on a header are `[:header h j]` for every `j` in `[0, len]`;
- hit-test, selection, and copy over a header resolve in this domain and
  NEVER alias body source offsets (Contract T forbids untagged/aliased
  offsets); nothing translates between the header and body domains.
G1 asserts this schema literally (the two-headers-plus-wrapped-body case
compares the full tagged values). Ground's existing consumption contract
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

**Reference-advance fault totality (§5.6's gate class, applied to the
U+0020 reference shape — TOTAL over every acquisition outcome):**
- the shaped result yields a positive, numeric advance → that value is
  `reference-advance`;
- EVERY other outcome — empty cluster list, missing advance field,
  non-numeric advance, zero advance, negative advance — is ONE provider
  contract fault with ONE lawful consequence: `provider-fault` increments
  by exactly 1 (once per layout key, like the shape itself), and the
  layout proceeds AS IF `wrap-col` were missing — no inline-size, no
  wrapping, unbounded lines. No throw, no fallback constant, no
  code-unit heuristic. G1 asserts each fault class as its own row
  (synthetic provider variants), each row checking the single
  `provider-fault` increment AND the unbounded-line consequence.

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

- **The visual projection token (VPT) — EXACT representation, defined
  once, used verbatim in the key fn AND in G1's expected key values:**
  `[body-hash header-texts op-role]` where
  - `body-hash` = value hash of the block's final VISIBLE body string —
    the string after EVERY projection (fold display, paste projection,
    `:foldable/defaults`/`:foldable/header-copy` as projected) has been
    applied; for a single-string op (auxiliary, anatomy, refusal, notice,
    …) the op's string itself;
  - `header-texts` = the vector of visible header strings in display
    order (`[]` for single-string ops) — a header text change flips the
    key even when the body is unchanged;
  - `op-role` = the op-role keyword.
  Fold, paste, and default/header-copy state enter the key ONLY through
  their effect on these three components; a projection/provenance/
  attention change that leaves all three unchanged produces an EQUAL key
  (paint-only — G4 rows j/k prove it live).
- **Source token, by case:**
  - When an actual content-revision stamp exists for the text, the token
    is the pair `[stamp VPT]` — so optimistic/paste/fold projections can
    never alias the stamped truth.
  - Otherwise (optimistic focused text via `block-view`
    ground_edit.cljc:445; paste projection via `paste-projection`
    ground_edit.cljc:121; machine fold display + header copy via `run-view`
    ground.cljs:1190; auxiliary and anatomy strings; any unstamped ground
    text): the token is the pair `[VPT stable-source-address]`. Declared
    in the key fn, never silent.
- **Visual-projection inputs, exactly:** the fold state as applied to this
  subject, `:foldable/defaults` and `:foldable/header-copy` as PROJECTED
  into visible text (shared/foldable_material.cljc:31–40), and the paste
  projection inputs. EXCLUDED even though they ride the same wear maps:
  bindings (foldable_material.cljc:44–69), contribution stamps, whole-wear
  identity. A binding-only material revision produces an EQUAL key (G1
  key-law assertion; G4 row j live).
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
destroy. TWO eviction owners, each with its own receipt: (1) truth death —
the block-death sites (ground.cljs:1923–1928, 3159–3167) evict ALL keys
the dead block owns; (2) vanished-slot destruction — the destroy site
(runtime/render.cljs:61–64) evicts the vanished slot's owned keys. A block
may own MORE than one live entry (body, headers, auxiliary/anatomy ops):
eviction expectations are `size − owned-key-count`, never `size − 1`, with
the owned count read pre-action via the §8 owned-keys hook. Diagnostics
carry size, hit/miss counts, and `:id-digest` — a hash of the sorted live
layout-ID set, the cheap identical-IDs receipt G4 case e reads. No
unbounded memo.

**Cache-correctness oracle (the render-seam fenced-view law, made
executable — a stale reuse must be CAUGHT, not inferred from counters):**
a reused cached result must be VALUE-EQUAL to a fresh `text-layout/layout`
batch computation over the same inputs. The oracle is a dev-mode
comparison (enabled only in oracle windows — bounded observability, T10):
while enabled, every cache HIT recomputes through the batch entry and
deep-compares the full retained result (line records with `:text`/
`:source-range`/`:consumed-range`, geometry, caret/hit spans, layout IDs).
`layout-cache` gains `:oracle-checks` / `:oracle-mismatches` fields (§8).
Gate owners: **G6** (JVM half — positive: hit-reuse deep-equals fresh
recompute; negative: a SEEDED stale entry yields exactly one
`:oracle-mismatches` increment and the assertion fails) and **G4 row l**
(live). This replaces §14's former unnamed "in-phase falsifier" duty for
cache staleness — the oracle is gate-owned, not cultural.

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
- `paint-repacks` — instance-data packs, keyed by the same cause
  vocabulary. **Pack-cause law:** a pack that follows a layout execution
  carries the EXEC's cause and is counted under it; a paint-only pack
  (layout reused) carries its paint cause. So a text edit's pack rides
  `:slot-text-change`, a hover repack rides `:hover-paint` (G3 asserts an
  exact count), and a provider change's packs ride the exec road — G4 row
  (i) asserts paint-repacks 0 there.
- `geo` — fresh / in-place / recloned / destroyed / capacity-grown counts.
- `proportionality` — last-layout work receipts in G1's exact vocabulary
  (glyph-visits, cluster-index-writes/reads, run-index-writes/reads,
  wrap-candidate-visits, shape-calls) so linearity is observable in the
  field.
- `provider-fault` — §5.6/§5.7's totality receipt.
- `slot-text-writes` — slot text-geometry write/reshape count (the G8
  line's reshape counter, now a DECLARED permanent counter; G4/G5 read it
  as window deltas). One increment per slot whose text geometry is
  written in a frame.
- `dirty` — dirty/present-region marks by cause, EXACTLY seven fields:
  `{:slot-text :entry :exit :hover :order :camera :other}` (I7's
  ownership made countable; `:other` absorbs marks with no named cause —
  recorded everywhere, asserted only where a gate row names it). Expected
  values are assigned to existing gates — G4 rows (text edit / hover /
  exit), G5 rows (camera / order), G8 (cold entry−exit conservation) —
  never to a new gate number.
- `store-frame-execs` — I9's counter.
- `layout-cache` — size + hits/misses + `:id-digest` (T9's bound and G4's
  identical-IDs receipt made visible) + `:oracle-checks` /
  `:oracle-mismatches` (§6's oracle) + the per-block owned-keys read.
  **Conservation law (binds every G4/G5 counter window):** miss delta =
  layout-execs total delta — every miss executes exactly once, every hit
  executes zero. Hits are unconstrained unless a row names them.

Dev-exposed hooks (diagnostics namespace, allowlist files only, no product
surface) — the COMPLETE enumeration; every scripted G4/G5 action below is
either one of these hooks or a literal UI drive, never a private harness
mutation: counter RESET (the G3/G4/G5 window opener) · id-digest read ·
cache size + per-block owned-keys read · oracle enable/disable · seeded
stale-cache entry (oracle negative ONLY, dev-only) · truth-road text edit ·
capacity-append edit · caret move · text-selection move ·
machine-selection move · machine fold-verb toggle · paste flip · backend
flip (settings road) · font/provider change · binding-only material
revision · contribution-stamp/attention wear change (paint-only
provenance) · block delete (truth road) · sibling order swap (truth road,
no text change). Camera pan/zoom are literal pointer scripts (UI drives),
not hooks.

## 9. Deliverables besides code

- **The committed profile harness** — `test/render_engine/
  profile_shaping_correction.mjs` (new file, beside the existing verifiers,
  never editing them), driving the dev app under §10's environment law.
  **Literal invocations (LAW — the harness ships exactly these three
  forms; every gate that names a mode runs its form verbatim):**
  - `node test/render_engine/profile_shaping_correction.mjs --mode=cold --url=http://localhost:8080`
  - `node test/render_engine/profile_shaping_correction.mjs --mode=hover --url=http://localhost:8080`
  - `node test/render_engine/profile_shaping_correction.mjs --mode=probe --url=http://localhost:8080`
  `--mode=` is the ONLY mode selector (unknown/missing mode → exit 1);
  `--url=` defaults to `http://localhost:8080` (the Jetty dev default,
  server_jetty.clj:2500 — no binding doc or the founding evidence pins any
  other address) and the EFFECTIVE url is recorded in the receipt.
  Readiness is harness-owned: the harness itself awaits §10's settle
  definition (cold) or hover-current confirmation (hover/probe) — no
  external sleep, no wrapper script. Receipt's FIRST field: serialized
  adapter attestation (`isFallbackAdapter`, description); then environment
  fields, then measurements; SHA-256 manifest over emitted artifacts.
  **Exit law (TOTAL and disjoint; §10's terminal classifications consume
  it):** classification is decided in this order, first match wins —
  - `1` = harness/environment fault (environment-law mismatch, fallback
    adapter, settle timeout of the harness's own machinery, unknown
    mode): the gate DID NOT RUN; receipt informational. Decided FIRST — a
    run that cannot attest its environment is `1` regardless of any
    assertion state.
  - `3` = ≥1 product counter/receipt assertion red (any mode; wall-bar
    state is recorded but cannot rescue the run).
  - `2` = every counter/receipt assertion green AND ≥1 wall bar red —
    `cold`/`hover` only; `probe` has no wall bars and NEVER exits 2.
  - `0` = every assertion for the mode green (including its wall bars
    where the mode has them).
  Exactly one exit matches any run; the seeded oracle negative (G4 row l)
  doubles as the proof that exit `3` is reachable.
- **The seven-transition hover storyboard** (THE one storyboard — this
  ordered list is the ONLY definition; §7 step 4, G3, and G9 reference it
  and restate nothing): pin the ordinary block and the 24,891-char paste
  block `…ep:3c6512e2:000000`. Acts, in order:
  1. UNMEASURED warm-up: hover the largest (paste) block once; await its
     ONE RAF/G8 pair.
  2. UNMEASURED positioning move: hover the ordinary block; await its ONE
     RAF/G8 pair. (These two pre-reset pairs are the storyboard's only
     unmeasured events; they appear in NO measured delta.)
  3. RESET counters (§8 hook) — only after the harness confirms the
     ordinary block is the CURRENT hover.
  4. SIX measured transitions: `(ordinary→largest, largest→ordinary)` ×3.
  5. The SEVENTH measured transition: ordinary→empty (the control) —
     lawful because act 4's alternation ENDS on ordinary.
  Each measured transition waits for exactly ONE post-input RAF/G8 pair;
  a missing or extra pair = FAIL. Measured paint-repacks `:hover-paint`
  total = **13** (six ordinary↔largest × 2 slots + the control's 1).
  (The banked four-transition capture remains the comparison baseline:
  each measured ordinary↔largest transition compares against its
  6.0–6.1s ancestors individually.)
- **The JVM test namespace** — `app.client.workspace.shaping-correction-test`
  (new file `test/app/client/workspace/shaping_correction_test.clj`) with an
  injectable synthetic provider (text_layout is `.cljc`; a provider is a map
  with `:shape-line` — no browser needed). Home of G1's linearity +
  semantic + reader-law cases, G6's span AND oracle assertions, and §6's
  key-law assertions.
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
- **App:** the shadow-cljs `:dev` build served by the running dev server
  (the Jetty server, default `http://localhost:8080` — §9's invocation
  law pins it; server_jetty.clj:2500); build id and effective URL
  recorded.
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
  `:consumed-range`, caret stops, HIT-TEST results, selection regions,
  COPY output, layout-ID stability across identical inputs, and
  shape-call count):** leading space (painted — §5.3 rule 1's first
  segment, uniquely derivable); one break space; multiple break spaces
  (`"abc   def"` = §5.3's worked example); segment-leading whitespace on
  a CONTINUATION segment (hard cut inside a whitespace run longer than
  the budget → the remainder is painted, rule 1, never consumed); tab as
  break-whitespace; NBSP never a break; no fitting cluster
  (single-cluster progress); mid-word hard cut; empty text; interior and
  trailing empty source lines preserved; trailing whitespace painted
  (never break-consumed); a header longer than the budget stays
  unwrapped; two headers + wrapped body (the §5.5 schema asserted on FULL
  tagged values — `[:header h [0 len)]` ranges, `[:header h j]` carets);
  proportional space advance (synthetic provider's distinctive U+0020
  advance drives inline-size — no hard-coded constant); a cluster
  spanning multiple UTF-16 code units never split (provider-fault path
  returns the §5.6 poisoned-total result, `provider-fault` incremented);
  §5.7's reference-advance fault table — one row per fault class (empty
  cluster list · missing advance · non-numeric · zero · negative), each
  asserting exactly one `provider-fault` increment AND the unbounded-line
  consequence.
  **Reader-law rows (§5.4 made executable, not prose):**
  - hit-test past the painted end of a wrapped line → the caret stop at
    consumed-start; once with a single-space consumed run and once with a
    multi-space run (never an interior consumed offset, never the next
    line);
  - caret stops at every offset of a consumed run: interior offsets
    display at the preceding line's painted end; `consumed-end` displays
    at the next line's start;
  - selection wholly INSIDE a consumed range → zero highlight width, at
    the preceding line's painted end;
  - selection ending exactly AT `consumed-end` → highlights to the
    preceding line's painted end, resumes nothing;
  - copy of a range wholly inside a consumed range → exactly the
    consumed characters; copy of a selection spanning the wrap → includes
    the FULL consumed run (source-range-based, §5.4).
  **Key-law assertions (§6):** the stamped token's EXACT representation —
  the computed key for a pinned fixture equals the literal
  `[stamp [body-hash header-texts op-role]]` built inline in the test
  (same VPT construction, no re-derivation road); binding-only material
  revision → EQUAL key; contribution-stamp/attention wear change → EQUAL
  key; header-copy projection change → unequal (header-texts component);
  fold display change → unequal (body-hash component); projection
  metadata change with identical visible body + headers + role → EQUAL;
  the key fn's signature admits only §6 inputs (the rebuild `sig` is
  unpassable).
  **Seeded negatives (the gate must be able to fail):** a variant with the
  old per-cluster whole-glyph filter exceeds the work-unit bound; a variant
  with a full-vector per-op scan fails G6's span assertion.
- **G2 (live, counter).** Command: §9's literal `cold` invocation. At
  settle: ground-surface fallback count = 0; every remaining fallback
  belongs to an I3-named surface and its per-surface counter is present in
  the report. Run-level mapping: exit 0 or 2 → G2 green (2 means only a
  wall bar is red — G8's affair); exit 3 with a G2 assertion in the red
  set → G2 red; exit 1 → unclassified.
- **G3 (live, counter + wall).** Command: §9's literal `hover` invocation,
  running THE seven-transition storyboard (§9's ordered list — nothing
  restated here): measured layout-execs delta 0 across ALL causes;
  paint-repacks `:hover-paint` delta = **13**; each of the SEVEN measured
  transitions ≤ 52ms input→RAF; exactly one RAF/G8 pair per measured
  transition; the receipt shows the two PRE-RESET pairs (warm-up +
  positioning) outside every measured delta. Run-level mapping as G2's
  (a >52ms transition alone → exit 2: G3's counter half green, G9 red).
- **G4 (live, receipt).** Command: §9's literal `probe` invocation —
  expected exit 0 — plus ONE separate negative invocation (row l) with
  expected exit **3**. Scripted rows via the §8 hooks (each row's action
  is a listed hook or a literal UI drive — no private harness mutation),
  counters reset before each row. **Row laws (bind every row):**
  1. UNNAMED = 0: every §8 counter field a row does not name has expected
     delta EXACTLY 0 — omission is an assertion, not a gap. Sole
     standing exemption: dirty `:other` is recorded, asserted only where
     named.
  2. CONSERVATION: layout-cache miss delta = layout-execs total delta;
     hits are unconstrained unless named.
  3. DETERMINISM: every row runs TWICE (reset between); both runs must
     produce identical deltas. A field marked RUN-RECORDED binds to its
     first-run value (no post-hoc fitting).
  Rows, expected deltas EXACT:
  - **(a) same-glyph-count text edit** (replace one non-whitespace ASCII
    char with another, same cluster/glyph count, pinned ordinary block,
    truth-road hook): layout-execs `:slot-text-change` +1 ·
    paint-repacks +1 · geo in-place +1 · slot-text-writes +1 · dirty
    `:slot-text` +1 · cache miss +1, size +1 (the superseded key lingers
    until its block's eviction — T9's bound, recorded).
  - **(b) hover flip** old→new: paint-repacks `:hover-paint` +2 · dirty
    `:hover` +2 · all else 0.
  - **(c) caret move; text-selection move; machine-selection move**
    (three probes): layout-execs 0 (`:selection` named 0) · geo 0 ·
    slot-text-writes 0 · misses 0 · paint-repacks RUN-RECORDED ≤ 2 per
    probe (0 if selection paint rides an overlay road; the affected-slot
    count if it rides a repack road — whichever, identical across both
    runs) · dirty six causes 0.
  - **(d) machine fold toggle; paste flip** (two probes, each):
    layout-execs `:fold-projection` +1 (exactly the affected block) ·
    paint-repacks +1 · slot-text-writes +1 · geo: {in-place + recloned +
    capacity-grown} sum = 1 for the affected slot (glyph-count delta
    picks which), fresh = destroyed = 0 · dirty `:slot-text` +1 · cache
    miss +1, size +1 · no other block's counters move.
  - **(e) backend MSDF↔Slug flip** (settings road, provider handle
    unchanged — PRECONDITION receipt: `provider-identity` fields compare
    equal before/after): layout-execs 0 · `layout-cache` `:id-digest`
    EQUAL before/after · paint-repacks = live text-slot count (the new
    coverage road's repack) · geo recloned = live slot count · destroyed =
    prior slot count · in-place 0 · dirty `:other` recorded.
  - **(f) capacity-growing edit** (append a 4,096-char suffix to the
    pinned ordinary block): layout-execs `:slot-text-change` +1 ·
    paint-repacks +1 · geo capacity-grown +1 for THAT slot, fresh /
    in-place / recloned / destroyed 0 · slot-text-writes +1 · dirty
    `:slot-text` +1 · cache miss +1, size +1 · every other slot's
    lifecycle counters 0. (Buffer growth is its own lifecycle receipt,
    never an in-place proof.)
  - **(g) truth-death eviction** (owned-key exact): fixture = a block
    owning body + ≥1 header + ≥1 auxiliary op; pre-read O = its
    owned-key count and S = its slot count (§8 hooks, in-receipt);
    scripted truth-road block delete: cache size −**O** (never −1) · geo
    destroyed = S · dirty `:exit` +S · layout-execs 0 · misses 0.
  - **(h) vanished-slot eviction** (the SECOND eviction owner, exercised
    separately): drive a product road that removes ≥1 live slot while
    its block's truth SURVIVES (candidate: machine fold-toggle
    collapsing the machine's output slots; the harness asserts the live
    slot set genuinely shrank by V ≥ 1). Expected = the driving action's
    own row deltas (fold: row d's) PLUS geo destroyed +V · cache size
    additionally −(pre-read owned keys of the vanished slots) · dirty
    `:exit` +V. ALTERNATIVE (allowed by R1 item 3): if NO product road
    reaches the destroy site (runtime/render.cljs:61–64) without truth
    death, the implementer instead PROVES convergence — phase-artifact
    receipt: the source trace plus row (g)'s run showing the destroy
    site firing inside truth death. Either way receipt-bearing, never a
    silent skip.
  - **(i) same-backend provider change** (font/provider-change hook —
    the mirror of (e): PRECONDITION receipt shows `provider-identity`
    fields UNEQUAL before/after, backend unchanged): pre-read A = live
    owned-key count (cache size, in-receipt): layout-execs
    `:provider-change` = **A** (all-and-only affected live keys) ·
    misses = A · `:id-digest` UNEQUAL before/after · slot-text-writes =
    live slot count · geo {in-place + recloned + capacity-grown} sum =
    live slot count, fresh = destroyed = 0 · paint-repacks 0 (packs
    ride the exec road here, not the reuse road — §8's pack-cause law) ·
    dirty `:slot-text` = live slot count · cache size +A (superseded
    keys linger until block death — recorded, T9-bounded).
  - **(j) live binding-only reuse** (binding-only revision hook, pinned
    ordinary block — the live twin of G1's JVM key equality): ALL §8
    deltas 0; `:id-digest` EQUAL.
  - **(k) provenance/attention paint-only** (contribution-stamp /
    attention wear-change hook): layout-execs 0 · misses 0 ·
    `:id-digest` EQUAL · geo 0 · slot-text-writes 0 · paint-repacks
    RUN-RECORDED ≤ affected-slot count (identical across both runs) ·
    dirty six causes 0.
  - **(l) cache oracle, live** (§6's oracle): oracle-enable hook; replay
    row (b) once: `:oracle-checks` > 0 · `:oracle-mismatches` 0. Then
    the SEPARATE negative invocation: seed a stale entry for the pinned
    ordinary block's current key (dev hook), hover onto it — expected
    `:oracle-mismatches` = 1 and the run assertion-red with process exit
    **3** (this negative is also §9's proof that exit 3 is reachable).
    Oracle disabled after; oracle fields are 0 in every other row.
- **G5 (live, counter).** Command: §9's literal `probe` invocation,
  expected exit 0; G4's three row laws bind. Exact probes:
  - **camera** — pan = scripted pointer drag on empty ground, ±200px both
    axes; zoom = 4 wheel steps in + 4 out at a fixed point: layout-execs
    0 (`:camera` named 0) · slot-text-writes 0 · geo 0 · paint-repacks 0 ·
    misses 0 · dirty `:camera` ≥ 1 (frame-driven mark count — declared
    nondeterministic, RUN-RECORDED), all other dirty causes 0.
  - **genuine order-only** — sibling order swap via the truth road, NO
    text change (the §8 hook): layout-execs 0 (`:order` named 0) ·
    slot-text-writes 0 · geo 0 · paint-repacks 0 (carried results
    reposition via the anchor-delta road — T8) · misses 0 · dirty
    `:order` ≥ 1 RUN-RECORDED, all other causes 0. THIS row proves I8's
    order-only zero cost — the fold probe below does not.
  - **origin-shift cascade** (honestly named — a fold, NOT order-only) —
    fold-toggle a block ABOVE the observed blocks: total layout delta =
    1 (exactly the folded block, `:fold-projection`) · slot-text-writes
    = 1 · every origin-shifted block's counters 0.
- **G6 (JVM + fence, executable).** The span-selection helpers live in
  shared `text_layout.cljc`; the renderer's paint/clip paths call them
  (I5). JVM half (same G1 command/namespace): per-op visited glyph records
  ≤ **span-glyph-count + 8**, where the visible span is in GLYPH-INDEX
  units — the glyphs whose cluster `:source-range` start offsets lie
  within the op's paint range; line resolution goes through the
  line-id→line index — visited lines ≤ 2 (the requested id + one nearest
  fallback); the seeded full-scan negative FAILS these assertions.
  **Oracle half (§6's cache-correctness oracle, JVM):** in the same
  namespace/command — positive: a cache HIT's reused result deep-equals a
  fresh `text-layout/layout` batch recompute over the same inputs (full
  retained value: line records, geometry, caret/hit spans, layout IDs);
  negative: a SEEDED stale entry yields exactly one `:oracle-mismatches`
  increment and the assertion fails. Static half:
  `node test/render_engine/verify_shaping_correction_fence.mjs` —
  expected exit 0 — asserts the renderer paint/clip roads route through
  the indexed helpers and carry no full-vector scan forms (the I5 named
  offenders stay dead).
- **G7 (live, receipt — rides G4's run).** Command: G4's `probe`
  invocation (no separate command); window: each G4 counter window.
  Receipt fields: `store-frame-execs` AND `raf-frames` (frames the
  harness observed in the same window). Bound (structural, exact): per
  window, `store-frame-execs ≤ raf-frames` — at most one store-frame
  execution per observed frame. Expected result: bound holds in EVERY
  window; run-level exit is G4's own (a bound violation joins the red set
  → exit 3). The counter authorizes nothing (I9) — that doc assertion
  lives in the phase artifact.
- **G8 (live, wall + receipts).** Command: §9's literal `cold`
  invocation, environment law, settle per its definition: cold visual
  settle ≤ **12.0s**; receipt opens with the adapter attestation; full
  RAF/G8/report lines banked; dirty conservation at settle: `:entry` −
  `:exit` = live slot count (177 on the matching corpus) and `:hover` =
  0 (no pointer in a cold run). Expected exit 0; 2 = only the wall bar
  red (the terminal-classification road).
- **G9 (live, wall + receipts).** Command: §9's literal `hover`
  invocation, THE seven-transition storyboard (§9's list): all SEVEN
  measured transitions ≤ **52ms** input→RAF (six ordinary↔largest + the
  ordinary→empty control). Expected exit 0; 2 = counters green, a
  transition over the bar.
- **G10 (suites, byte-fidelity — every command pinned with its expected
  result):**
  1. G1/G6 namespace (command above) → exit 0.
  2. Focused suites, ONE run, the LITERAL command (exit = fail+error
     sum; expected exit 0):
     `clj -M:test -e "(require 'app.client.workspace.text-layout-test 'app.client.workspace.block-edit-test 'app.client.workspace.ground-edit-test 'app.client.workspace.scene-store-test 'app.client.substrate.scene-tape-test 'app.client.substrate.maintained-view-test 'app.anatomy-test 'app.face-primitives-test 'app.face-projection-test) (let [r (apply clojure.test/run-tests '[app.client.workspace.text-layout-test app.client.workspace.block-edit-test app.client.workspace.ground-edit-test app.client.workspace.scene-store-test app.client.substrate.scene-tape-test app.client.substrate.maintained-view-test app.anatomy-test app.face-primitives-test app.face-projection-test])] (System/exit (+ (:fail r) (:error r))))"`
  3. `node test/render_engine/verify_text_layout_fence.mjs` → exit 0 and
     `node test/render_engine/verify_scene_tape_fence.mjs` → exit 0 (run,
     never edited).
  4. `node test/render_engine/verify_shaping_correction_fence.mjs` →
     exit 0.
  5. `npm run verify:render-engine` → **expected process exit 1**,
     matching Contract T's registered state (W1.md §9.1) on BOTH of the
     verifier's output surfaces (they differ — run_verifier.mjs:491–510
     vs the receipt file; conflating them was R2's finding):
     - **Console structured close (fields AS EMITTED):** `pass` false ·
       `classification` `"candidate-pick-parity-failure"` ·
       `deterministic` `"21/21"` · `candidateParity` `"14/21"` ·
       `productBoundsDivergenceSentinels` `"7/7"` · `images` 21 ·
       `q8AffineTransport` true · `q5AffineRasterBoundary` true ·
       `updateRequested` false · `updateAuthorized` false ·
       `environmentFingerprint`
       `"e79490f8882cd785f32b5bb82cadd425dc90f2d7616cc9f0debf8a0f1c476282"`
       — the fingerprint is asserted DIRECTLY because the verifier's
       classification cascade tests parity BEFORE environment match
       (run_verifier.mjs:435–448): the classification alone can never
       prove the environment; this field does.
     - **Receipt file:** `goldenComparison` = 21/21 rows byte-identical;
       per-regime candidate parity: SDF 7/7 pass; Slug 7/7 pass with
       exactly two declared byte-128 ties per regime; MSDF 7/7 RED with
       47 decisive mismatches per regime (329 total) plus two correctly
       separated ties per regime. The receipt file path is banked in the
       phase artifact.
     ANY other exit code, close-field value, classification, or count =
     G10 red — except a GOLDEN divergence, which routes through the
     custody law below BEFORE any red/green is recorded.
  6. CLJS compile: `clj -M:dev -m shadow.cljs.devtools.cli compile dev` →
     exit 0 with NO new distinct warnings against the BASELINE captured by
     running the identical command at phase open BEFORE any edit (baseline
     output banked VERBATIM in the phase artifact). **Warning identity
     (the normalization, exact):** a warning's signature is the triple
     [repo-relative resource path · warning type/key · message text with
     ANSI escapes stripped, all decimal integer literals removed, and
     whitespace collapsed]. "New distinct" = a signature present in the
     candidate run's multiset and absent from the baseline multiset;
     line/column drift alone never makes a warning new; a DISAPPEARED
     warning is recorded, never red.
  **Golden custody (ONE state chain — never two names for one diff):**
  per R1's verification the W0 engine goldens do not exercise ground
  block-greedy, so 21/21 byte-identical is the expected state, and ANY
  golden diff has exactly ONE immediate result: **G10 BLOCKED — stop S6**
  (golden custody adjudication), recorded as neither FAIL nor PASS while
  blocked; the package terminal is PACKAGE BLOCKED — S6 (§10 table row
  1). A belief that the diff stems from §5's contracted change changes
  nothing pre-ruling. After the Fable/Sid ruling, exactly one of:
  (α) the diff is illegal → G10 FAIL, no golden edit, fix in phase;
  (β) a separately reviewed golden update is explicitly authorized → the
  update lands under that review and a WHOLLY FRESH G10 run follows (the
  blocked run is never retroactively renamed). A run is never both
  "byte-identical" and "passed by exception"; a diff is never
  simultaneously FAIL and BLOCKED.

### Terminal classifications (TOTAL and DISJOINT — rows are tested in
order, first match wins, so exactly one name fits any end state.
Precedence: **BLOCKED > PACKAGE FAIL > UNCLASSIFIED (environment) >
EXPERIENCE-BAR RED > PACKAGE PASS.** Run-level statuses — a specific
harness run's exit 1 "unclassified", exit 3 "assertion red" — feed these
package rows; a RUN status never IS the package terminal by itself.)

1. **PACKAGE BLOCKED — S<n>** (the family S1…S6) — a stop clause fired
   and its ruling has not landed. Blocked is neither red, green, bar-red,
   nor unclassified; while blocked, NO fail/pass ruling is recorded for
   the gate the stop touches (S6: a golden diff blocks G10 BEFORE any
   fail/pass — the custody chain in G10). Resolution: the ruling
   reclassifies the state under the rows below, or recuts the contract.
2. **PACKAGE FAIL** — no stop pending, AND ≥1 of G1–G7 or G10 red —
   including any live run at harness exit 3 (product assertion red;
   G4 row l's deliberate negative invocation is exempt: its exit 3 IS
   its expected result). Fix in phase. An unclassified run elsewhere
   does not rescue: FAIL binds on the red gate regardless of other
   gates' state.
3. **UNCLASSIFIED (environment)** — no stop pending, no gate red, AND ≥1
   required live gate's only available run is exit 1 / fallback-adapter /
   environment-mismatched: that RUN is unclassified (never red, never
   green; receipt informational) and the PACKAGE cannot close — re-run
   under the environment law. (When a red gate coexists with an
   unclassified run, row 2 already matched: the package is FAIL and the
   affected run stays recorded as unclassified.)
4. **LINEAR-CORRECTION GREEN / EXPERIENCE-BAR RED — STEP-5 RULING
   REQUIRED** — no stop pending, G1–G7+G10 green, every required live
   run classified (no exit-1 holes), and a G8/G9 bar red WITH a valid
   environment-matched receipt (harness exit 2): bank all receipts, make
   NO further code change under this contract, close the implementation
   phase as technically green for G1–G7+G10, and open the Fable/Sid §7
   step-5 ruling. The overall experience gate remains red; SEAM felt-gate
   closure does not proceed. This is not S1–S6 and does not recut this
   contract unless the evidence reveals a contract defect.
5. **PACKAGE PASS** — G1–G10 all green including both bars → the
   independent FULL-tier gate review (§14).

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
  :4–11; `src/app/server_jetty.clj` — dev default port 8080 :2500 (§9's
  URL pin); `test/render_engine/run_verifier.mjs` — console structured
  close :491–510, classification cascade :435–448, receipt-file fields
  :464–484 (G10 item 5's two surfaces).
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

Every fired stop names the package terminal **PACKAGE BLOCKED — S<n>**
(§10 table row 1) until its ruling lands; a stop never coexists with a
FAIL/PASS ruling on the gate it blocks.

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
- **S6** — golden custody adjudication: any W0 golden diff → G10 BLOCKED
  (recorded as neither FAIL nor PASS — G10's custody chain; package
  terminal PACKAGE BLOCKED — S6); Fable/Sid rule whether the diff is the
  contracted §5 change and whether a separately reviewed golden update is
  legal. Never an implementer edit, never a silent pass, never a dual
  FAIL+BLOCKED name.
- Bars failing at G8/G9 are NOT stops — that is the LINEAR-CORRECTION
  GREEN / EXPERIENCE-BAR RED terminal classification (§10; step 5 fires).

## 14. Handoff

1. NEXT: ONE wholly new fresh default-fail validation round (R3) over this
   recut (subagent or fresh session; findings folded per the minor-fail
   law, FAIL → recut again). `VALIDATION_R1.md` and `VALIDATION_R2.md`
   are immutable and never overwritten; R3 lands as `VALIDATION_R3.md`.
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
