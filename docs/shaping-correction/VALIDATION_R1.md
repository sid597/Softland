# shaping-correction contract — validation round 1 (fresh context, default-fail) — VERDICT: FAIL

Run 2026-08-04 from a fresh Codex context over the artifact as cut at
`9e6ae869cb8933022beb72d399e65181d2fae32b`, on
`docs/current-mental-model-local`. Mode = GATE; altitude = contract; authority =
candidate. `FOUNDING-EVIDENCE.md` + its eight raw receipts were treated as facts;
Contract T (`build/render-engine/W1.md`) and decisions.md "The render seam" were
treated as binding. Source was read from the foreign-dirty working tree named by
the contract. No source, evidence, contract, existing verifier, or foreign path
was edited.

This artifact is the immutable FAIL record ordered by CONTRACT §14. It is never
overwritten. A recut contract must consume the findings below, then a wholly new
default-fail round must validate the recut.

---

**VERDICT: FAIL.** The contract does not yet give one executable implementation
road. Six defects either admit materially different behavior, make an acceptance
gate uncomputable, or demand a receipt the live lifecycle cannot produce:

1. §5 does not close whitespace consumption or header semantics, and no gate
   checks the wrap result's required edge cases.
2. G4's backend-flip expectation collapses paint repack and GPU-geo reclone even
   though both §6 and the live backend path require them to occur distinctly.
3. G1 and G6 contain unbound pass constants/quantities; most live gates lack a
   pinned executable environment and observation window.
4. §6 does not name all actually unstamped visual-text sources or the exact
   projection of worn material that may enter the key; selection/fold/wear
   invalidation has no gate.
5. §9, G3, and G9 prescribe incompatible hover storyboard cardinalities.
6. G10 simultaneously requires 21/21 byte identity and permits a §5 golden diff,
   while leaving the focused suites, compile command, warning baseline, and
   expected nonzero verifier classification unnamed.

The T13 provider-handle/§12 allowlist question is **not** a defect: the live
provider is already reachable from ground without touching a MUST-NOT file. The
evidence bank and every §11 manifest substance locator also validate cleanly.

## 1. Evidence and manifest integrity

### 1.1 Founding evidence

`sha256sum evidence/*` reproduced all eight hashes in
`FOUNDING-EVIDENCE.md:35-42` exactly. The raw navigation profile contains 37,507
nodes, 52,471 samples, and 52,471 time deltas. The banked console contains the
verbatim 173-slot / 28,725.0ms `text-gpu` final frame, and
`hover-summary.json` contains the four reported transitions and their exact
6,124 / 6,028 / 6,101 / 28ms input-to-RAF durations. The unresolved published
hash cross-check and missing serialized adapter attestation remain exactly as
recorded (`FOUNDING-EVIDENCE.md:19-26,57-65`); this round does not promote either
to a fact.

The bank therefore supports the contract's causal starting point:
`shaped-layout` 28.343s contains `union-bounds` 24.390s because the lazy
per-cluster whole-glyph scan is realized there
(`FOUNDING-EVIDENCE.md:83-104`), and hover rebuilds two tint-changed slots through
the op-vector identity miss (`FOUNDING-EVIDENCE.md:115-132`).

### 1.2 §11 re-grep — no manifest-substance stop

All five line counts still equal the manifest: `text_layout.cljc` 797,
`face_primitives.cljc` 1,324, `renderer.cljs` 2,386, `runtime/render.cljs` 792,
and `ground.cljs` 4,798. Every named symbol re-grepped at the stated line:

- `text_layout.cljc`: `wrap-line` 54; `block-wrap-lines` 80;
  `provider-identity` 261; `shaped-segments` 283; `union-bounds` 320;
  `shaped-layout` 328; `layout` 506; `line-paint-ops` 527; `caret-result` 590;
  `selection-result` 638; `clip-result` 660; `hit-test-result` 766. The named
  filters are present at 417-424, 434-438, and 669-671.
- `face_primitives.cljc`: `text-op` 550; `wrap-lines` 559;
  `block-render-lines` 572; `ground-block-layout` 587; `block-root-prim` 618;
  bare root ops begin at 648.
- `renderer.cljs`: `destroy-text-system!` 850; `clone-text-system` 1100;
  `line-index-for-layout` 1454; `position-text-op` 1472 and its fallback / anchor
  / glyph-filter roads at 1484-1493 / 1499-1501 / 1502-1510; `position-text`
  1519; the MSDF and Slug coverage-only comments 1538 and 1573; `shape-text`
  1603; `update-text-data` 1719.
- `runtime/render.cljs`: `reconcile-slot-text-geos!` 25; op-vector `identical?`
  48; `<combined-text-ops` 87; `<store-frame` 130; `store-frame-changed?` 241;
  `text-by-vi` 248; G8 log 406.
- `ground.cljs`: `machine-visual-lines` 1223; `block-anatomy-view-model` 1253;
  `rebuild-block!` 1500; `reconcile!` 1794; hover rebuilds 4047-4059; the two
  manifest-named `ss/upsert-slot` sites 1016 and 2280.

S3 does not fire. The working-tree changes in `renderer.cljs`,
`runtime/render.cljs`, and `ground.cljs` remain foreign custody; this validation
uses their current substance and changes none of it.

## 2. G1-G10 executability: form, owner, environment

Global ownership is adequately declared: the Codex implementation lane owns the
one phase (`CONTRACT.md:1,279-282,403-408`), and Fable FULL independently reruns
the gates (`:412-415`). No gate silently transfers implementation work to Sid.
The defects are in form and environment:

| gate | form | owner | environment | round-1 result |
|---|---|---|---|---|
| G1 | `k` and `c` have no numeric values or derivation; the work-unit vocabulary is not sum-checked; only complexity, not wrap correctness, is asserted (`CONTRACT.md:293-297`). | Codex, then Fable | JVM is explicit. | **FAIL** — no unique pass predicate. |
| G2 | Target state is clear, but no command/script, counter reset point, settled-frame criterion, or browser/build precondition defines “after cold populate” (`:298-300`). | Codex, then Fable | “live” only. | **MINOR-FAIL** — executable after an exact harness entry point and observation window are named. |
| G3 | Counter/wall assertions are clear, but “both directions ×3” means six ordinary/large transitions and conflicts with §9/G9; no start/reset point or live environment is named (`:301-303`). | Codex, then Fable | “live” only. | **FAIL** — the storyboard has no single cardinality. |
| G4 | The scripted cases are named, but backend “reclone only” is physically false and an arbitrary text edit does not guarantee the intended in-place capacity case (`:304-306`). | Codex, then Fable | “live” only; script unnamed. | **FAIL** — one expected receipt contradicts the lifecycle being tested. |
| G5 | The zeros are clear, but the exact pan/zoom trace, the order-only writer/action, counter reset point, and which `geo` field is “text-geometry writes” are not named (`:307-308`). | Codex, then Fable | “live” only. | **MINOR-FAIL** — exact actions and receipt fields required. |
| G6 | `k`, “visible-span size”, the fixture, and the executable owner of renderer-side scan counters are undefined (`:309-311`). | Codex, then Fable | “executable” gives no JVM/CLJS/browser environment. | **FAIL** — no unique pass predicate or runnable environment. |
| G7 | A scalar `store-frame-execs` field plus a phase-artifact assertion is a checkable bounded-memory receipt (`:312-314`; §8 at :246-259). | Codex, then Fable | live ground report. | **PASS as a gate shape**, assuming the field and artifact are produced. |
| G8 | Corpus, attestation-first receipt, and 12.0s bar are clear, but “same machine class” is undefined and the to-be-created harness has no pinned invocation, browser mode/version/viewport/DPR, readiness rule, or environment-match policy (`:315-317`; §9 :263-268). | Codex, then Fable | committed harness, otherwise unspecified. | **MINOR-FAIL** — exact invocation and comparison environment required. |
| G9 | Wall checks are clear, but “all three ordinary↔largest transitions” conflicts with G3's six; harness environment is inherited but still unpinned (`:318-320`). | Codex, then Fable | committed harness, otherwise unspecified. | **FAIL** — storyboard conflict. |
| G10 | Some artifacts are named, but the composite verifier intentionally exits nonzero for the required MSDF RED; focused suites and compile command are unnamed; “0 new warnings” has no baseline; and 21/21 byte identity contradicts the later §5-diff carve-out (`:321-327`). | Codex, then Fable | suites/CLJS/verifier, not pinned to commands/preconditions. | **FAIL** — the gate can report both failure and success for the same lawful state. |

### FAIL-class finding 1 — G1/G6 are algebraic placeholders, not gates

**Claim in artifact:** G1 asserts work `≤ k·(G+C+R)` and a 4× growth ratio
`≤ c·4×`; G6 asserts per-op visits `≤ visible-span size + k`
(`CONTRACT.md:293-297,309-311`).

**Concern:** neither `k` nor `c` is bound, and “work units” can omit an operation
without violating a declared sum. For any observed implementation, an author can
choose larger constants after the run. G6 also does not say whether
`visible-span` counts lines, clusters, glyphs, or source units. These are not
default-fail predicates.

**Evidence/rule:** the render seam requires a fenced oracle, not a cultural
complexity claim (`decisions.md:405-424`). Contract I4 requires actual G+C+R
construction (`CONTRACT.md:106-109`).

**Required recut:** bind the counter vocabulary and constants before code. At
minimum, enumerate counters for input glyph visit, cluster-index insert/read,
run-index insert/read, wrap-candidate visit, final-segment shaping, and per-op
span visit; state the exact sum; give numeric `k`/`c` (or an exact formula with
no free constants); define visible span in glyph-index units; name the JVM test
command for G1 and the CLJS/browser or pure extracted test command for G6. Seed
a full-vector scan and prove each gate fails.

### FAIL-class finding 2 — G3/G9/§9 describe different tests

**Claim in artifact:** the committed harness implements “the four-transition
hover storyboard” (`CONTRACT.md:263-268`); G3 runs ordinary↔largest “both
directions ×3” (`:301-303`); G9 checks “all three ordinary↔largest transitions”
plus the ordinary→empty control (`:318-320`).

**Concern:** those are respectively four total transitions, six measured
ordinary/large transitions, and three measured ordinary/large transitions plus
one control. The banked evidence is the third shape: largest→ordinary,
ordinary→largest, largest→ordinary, ordinary→empty
(`FOUNDING-EVIDENCE.md:108-120`). An implementer can satisfy one clause and fail
another with the same run.

**Required recut:** choose one storyboard once and reference it from G3/G9. The
strongest reading already stated by G3 is: warm/select the two pinned blocks;
reset counters; alternate exactly six measured transitions (three each
direction); then ordinary→empty as one measured control; wait for exactly one
post-input RAF/G8 pair per transition; fail on a missing/extra pair. Call this a
seven-transition storyboard everywhere, or deliberately choose the original
four-transition replay everywhere. Do not retain both counts.

## 3. §5 wrap semantics — source trace and falsification

Contract T makes wrap a reader of the one result and forbids character-advance
reconstruction (`W1.md:527-534,582-592`). The contract correctly removes the
legacy column-cut authority and makes proportional shaped geometry choose
breaks (`CONTRACT.md:139-179`). Four scenarios were traced:

### 3.1 Explicit hard breaks and empty lines — semantically closed, ungated

`source-line-records` uses `split #"\n" -1` and assigns a record to every entry
(`text_layout.cljc:265-276`). Thus `"a\n\n"` produces `"a"`, `""`, `""`, and
the legacy wrapper preserves the same vector. This agrees with §5's hard-break
law (`CONTRACT.md:154-155`). An empty source line also reaches `shaped-segments`
as one empty final segment (`text_layout.cljc:288-295`).

But G1 checks only work counts and G10 guarantees no golden that exercises this
case (`CONTRACT.md:293-297,321-327`). An implementation that drops the trailing
empty still passes all written predicates.

### 3.2 Whitespace consumption — two physically different implementations are legal

Legacy `block-wrap-lines` searches an `(inc col)` prefix for the last ASCII
space, emits text *before* that one space, then applies `triml` to the remainder
(`text_layout.cljc:89-96`). On the live function:

```clojure
(tl/block-wrap-lines ["abc   def"] 5)
;; => ["abc  " "def"]
```

Current shaped wrapping instead chooses a cluster end whose boundary follows
whitespace and slices at that end (`text_layout.cljc:296-307`), placing the
whitespace in the preceding segment's source range/text. §5 combines that
after-whitespace candidate with “the break-consumed whitespace is not painted
on either line” (`CONTRACT.md:160-163`) but does not say:

- whether the consumed cluster is excluded from the preceding segment, retained
  in its source range but omitted from paint, or represented as an explicit
  skipped source range;
- whether one whitespace cluster or a run is consumed;
- whether “whitespace” is U+0020 only (legacy) or the `\s` class already used by
  `whitespace-at?` (`text_layout.cljc:278-281`);
- how caret, selection, hit test, and copy map across a consumed-but-unpainted
  range.

Those choices yield different `:lines`, source ranges, advances, carets, and
selection rectangles while all satisfying the prose.

### 3.3 Headers — current shaped path drops them; the contract does not define the repair

The legacy result wraps only the body and then prepends headers unchanged
(`text_layout.cljc:99-115`). Ground passes `:headers` into layout
(`face_primitives.cljc:606-616`) and assumes the first `nh` result lines are the
unwrapped headers (`:638-660`). The shaped path neither destructures nor reads
`:headers`; it builds visual records only from body source records
(`text_layout.cljc:328-352`). A synthetic shaped-provider probe against the live
tree returned:

```clojure
{:shaped-block-greedy ["abc def"] ; inline-size 3.1, header "HEADER" dropped
 :shaped-word         ["abc" " " "def"]}
```

The contract orders a `:block-greedy` sibling but never says that headers remain
an unwrapped synthetic prefix, how they are shaped, or what tagged source/index
space their non-body ranges inhabit. Because Contract T forbids untagged source
offsets (`W1.md:576-580`), this is not an implementation detail.

### 3.4 Minimum progress and wrap-col translation — stated, not fully fenced

The minimum-one-cluster rule is clear for a normal provider
(`CONTRACT.md:164-165`), but the current fallback to code-unit `1` when a
nonempty provider result contains no clusters (`text_layout.cljc:299-303`) can
split a surrogate and is not “one cluster.” The contract must classify an empty
cluster list for nonempty text as provider-contract failure (and name S5/G1) or
define a lawful tagged fallback.

The translation `wrap-col × shaped U+0020 advance` is conceptually clear
(`CONTRACT.md:168-172`), but no gate proves it uses the same provider identity,
features, variations, language, tab regime, and font size as the body, executes
once per layout key rather than per cut, or defines nonpositive/missing
`wrap-col`. A wrong hard-coded space advance can pass every current gate.

### FAIL-class finding 3 — §5 needs an executable semantic table, not only a growth test

**Required recut:** define a structured segment/cut result with body source
range, optional consumed-whitespace range, and paint range; state precisely how
caret/selection/copy cross the consumed range. Preserve headers as ordered,
unwrapped synthetic prefix lines, name their tagged index/source domain, and
shape each header once with the live provider. State the nonempty/no-cluster
failure. Define reference-advance as one shape of U+0020 with the body's complete
shape options and bind it into the key.

Add JVM semantic assertions (not only work counters) for at least: leading
space; one and multiple break spaces; tab/NBSP policy; no fitting cluster;
mid-word hard cut; empty text; interior and trailing empty lines; a header longer
than the budget; two headers plus a wrapped body; proportional space advance;
and a cluster spanning multiple UTF-16 units. Assert exact line text, source
ranges, consumed ranges, layout IDs, caret stops, selection regions, and shaping
call count.

## 4. §6 key against every ground rebuild path

### 4.1 Actual inputs and rebuilds

Ground's current rebuild census is wider than a served block revision:

- the focused block renders the optimistic queue projection, not served truth
  (`ground.cljs:1416`; `ground_edit.cljc:445-478`);
- machine folds derive display plus material-authored header copy
  (`ground.cljs:1190-1221`);
- paste fold state substitutes both body and header (`ground.cljs:1418-1428`);
- anatomy can add four blank root rows and invocation text-bearing children
  (`ground.cljs:1273-1286`; `anatomy_material.cljc:548-588`);
- refusal, notice, boundary, conflict, gold, and silver emit independent ground
  text ops (`face_primitives.cljc:755-844`);
- the rebuild signature also contains hover, selection, boundary/group state,
  placement, whole worn-material maps, marks, and metrics
  (`ground.cljs:1476-1481`). That signature is intentionally broader than a
  layout key.

Scenario trace:

| change | live rebuild path | lawful layout-key result |
|---|---|---|
| ordinary served or optimistic text edit | truth-overlay watch / focused queue → `rebuild-block!` (`ground.cljs:4603-4608,1416`) | **flip** on the actual visual-text value/source token |
| hover old→new | `pointer-move!` rebuilds both (`ground.cljs:4047-4059`) | **reuse** both layouts; paint/attention only |
| caret or text/machine selection move | selection verbs rebuild the subject (`ground.cljs:3731-3765`) | **reuse** root layout; new selection/caret reader output only |
| machine fold | fold verb mutates `!folds` and rebuilds (`ground.cljs:3700-3713`) | **flip** because display/header text changes |
| paste fold | `paste-projection` changes body/header (`ground.cljs:1418-1428`) | **flip** because visual text changes |
| foldable header-copy/default change | material watch re-reconciles (`ground.cljs:4609-4618`; `foldable_material.cljc:27-40`) | **flip only if** projected header/default changes visual text |
| foldable binding-only revision | same material watch; binding rows do not alter text (`foldable_material.cljc:44-69`) | **reuse**; hashing the whole wear/revision is unlawful |
| provenance tint / attention box / hover / group-selection wear | rebuild signature changes paint/presence | **reuse** root layout |
| anatomy/invocation wear | compiled parts may change which text ops exist and their actual strings | key each emitted visual string/metric input; unchanged strings reuse |
| origin/drag, camera, order, backend, atlas/GPU lifecycle | transform/sink changes | **reuse** (`CONTRACT.md:198-208`) |
| provider/font metric/wrap inputs | font or semantic geometry changes | **flip** |

### FAIL-class finding 4 — “served revision + foldable wear” is not a closed key on this ground

**Claim in artifact:** the key hashes block address, “the revision of its served
truth,” and every visual-text input; an unstamped machine display falls back to
visual-text hash (`CONTRACT.md:183-196`).

**Concern:** the current block projection carries no universal served revision:
`context-blocks` retains the served block maps and builds `:block-index` by id
(`ground.cljs:1745-1749,1803-1806`), while the narrow truth overlay is a plain
unit-id→string map (`block_edit_wiring.cljs:141-166`). Focused optimistic text is
newer than both. Auxiliary ground strings have no served block revision at all.
Conversely, hashing the complete `view`, `sig`, or “foldable wear” would invalidate
on hover/selection/binding/tint changes that §6 expressly excludes.

**Required recut:** name a total key constructor over values available inside the
allowlist. For every unstamped ground op—not only machine display—use a declared
visual-text value hash plus stable source address/op role; when an actual
content-revision stamp exists, pair it with the final visual-text projection
token so optimistic/paste/fold projections cannot alias it. Replace “foldable
wear” with the exact visual projection (`:foldable/defaults` as used for this
subject, `:foldable/header-copy`, and paste projection/header inputs); exclude
bindings, contribution stamps, and whole-wear identity. Key auxiliary ground
ops by their actual text/source role. State that the broad rebuild `sig` is
never a layout key.

Extend a gate (G3/G4 is sufficient; no new gate number required) with explicit
deltas: hover 0; caret move 0; text selection move 0; machine selection move 0;
provenance/attention paint-only wear 0; foldable binding-only revision 0;
fold/header-copy/paste projection exactly the affected block(s); ordinary
optimistic edit exactly one; provider revision exactly all affected live text.
Include cache hit/miss and eviction receipts for block death and slot destroy.

## 5. §7 step 3 / G4 lifecycle contradiction

### FAIL-class finding 5 — backend flip must repack and reclone

**Claim in artifact:** §6 correctly keeps backend out of the layout key and says
a backend flip reclones GPU resources while reusing layout
(`CONTRACT.md:198-208`). §7 step 3 separately names paint-only repack and geo
lifecycle (`:229-234`). G4, however, demands “backend flip → reclone only
(layout reused)” (`:304-306`).

**Concern/evidence:** on a backend change the live runtime recreates the parent
text system and clones fresh child geos (`runtime/render.cljs:320-345,396-404`).
Every new/recloned slot immediately calls `shape!`, which calls
`update-text-data` (`runtime/render.cljs:39-58`). `update-text-data` produces and
writes different MSDF and Slug instance layouts (`renderer.cljs:1727-1780`). A
backend flip therefore has three distinct facts:

1. layout executions = 0 and the same layout IDs are reused;
2. paint/backend instances are repacked for the new coverage road;
3. GPU text systems are recloned (old resources destroyed, new resources
   created).

Counting only (3) makes `paint-repacks` lie; forbidding (2) leaves the new backend
without its required instance data. This violates I6's “three distinct
lifecycles, three distinct receipts” (`CONTRACT.md:117-121`).

**Required recut:** change the G4 row to:

```text
same-capacity, metric-changing text edit -> layout-exec + paint-repack +
slot-geo in-place update; hover/tint -> layout 0 + paint-repack + geo lifecycle 0;
backend MSDF<->Slug -> layout 0, identical layout IDs, backend paint-repack +
geo reclone/destroy receipts.
```

Define “same-capacity” (or use a same-glyph-count edit) so the text-edit case
cannot accidentally test buffer growth. If buffer growth is itself a desired
lifecycle, add it as a fourth scripted case with its own expected allocation
receipt; do not call it an in-place proof.

## 6. §12 allowlist sufficiency and T13 provider-handle seam

**PASS — no S2 stop is needed.** Steps 1-3 fit the existing allowlist:

- Step 1 is wholly in allowed `text_layout.cljc` plus its new JVM test/fence.
- Ground already stores the complete runtime atom map in `!refs`
  (`ground.cljs:85,4578-4584`). `metrics` currently reads `!active-font`
  (`ground.cljs:177-184`). That atom is initialized with the boot font's
  `:layout-provider` (`runtime/state.cljs:89-96`) and same-id enriched after an
  async font load (`runtime/fonts.cljs:176-197`). Thus an allowed edit to
  `ground.cljs` can read exactly that live handle, add it to the existing
  `metrics`/`:geom` value (`ground.cljs:1386-1404`), and an allowed edit to
  `face_primitives.cljc` can pass it to `ground-block-layout`. The already-live
  non-ground face path demonstrates the same field shape at
  `scene_runtime.cljs:94-109`; it need not be edited.
- Step 2's carried op fields fit allowed `face_primitives.cljc`, `ground.cljs`,
  and `renderer.cljs`. Selection/caret already converge through
  `ground-block-layout` (`face_primitives.cljc:701-708,746-753`).
- Step 3's slot reuse and lifecycle live in allowed `runtime/render.cljs` and
  `renderer.cljs`. Block-death and acknowledged-delete sites are in allowed
  ground (`ground.cljs:1923-1928,3159-3167`); runtime reconciliation already
  sees vanished text slots and destroys their geos
  (`runtime/render.cljs:61-64`), so cache eviction can be driven without editing
  `scene_runtime.cljs` or `scene_store.cljc`.

The recut should name this route explicitly as the one T13 seam:
`ground !refs -> :!active-font -> :layout-provider -> metrics/geom ->
ground-block-layout`. Do not read the provider from renderer state and do not
boot another shaper.

## 7. G10 and the §5 carve-out

### FAIL-class finding 6 — G10 has two verdicts for one lawful state

**Claim in artifact:** “W0 golden bank 21/21 byte-identical” is mandatory, then
“Any golden diff = FAIL unless” it is the contracted §5 wrap change, in which
case it goes to adjudication (`CONTRACT.md:321-327`).

**Concern:** a golden that lawfully exercises block-greedy cannot be both
byte-identical and changed. The carve-out does not say whether adjudication
temporarily blocks G10, can authorize a new expected golden, or merely explains
a still-failing diff. In addition, `npm run verify:render-engine` expands to the
text checks, both existing fences, the render-verifier compile, and
`run_verifier.mjs` (`package.json:8-9`), but its required MSDF result is an exit
1 / `candidate-pick-parity-failure` state (W1.md:723-738). A raw “green” command
check would reject the required state. “Focused text/face/scene suites” and
“cljs compile 0 new warnings” have no commands or warning baseline.

**Required recut:** name every command and its expected process/result
classification. For `npm run verify:render-engine`, require the structured close
conditions: text/fences/compile succeed; 21/21 determinism and existing golden
rows match; Q5/Q8/SDF/Slug stay green; exactly the registered seven MSDF rows
remain RED with 47 decisive mismatches and two ties per regime; overall
classification remains `candidate-pick-parity-failure` even though process exit
is nonzero. Name the focused namespaces/commands and the exact CLJS release
command plus pre-phase warning baseline.

Choose one golden policy:

- preferred: the existing W0 engine goldens do not exercise ground block-greedy,
  so 21/21 remains absolute and any diff is FAIL; §5 correctness lives in the new
  semantic/harness receipts; or
- if a named golden truly exercises §5, G10 remains **BLOCKED FOR ADJUDICATION**
  until the diff is proven to be only the contracted break change and Sid/Fable
  rules whether a separately reviewed golden update is legal. It is never both
  “21/21 byte-identical” and passed by exception in the same run.

## 8. Experience bars, §7 step 5, and stop clauses

The architectural scope rule is coherent and should be preserved: failure of
the 52ms/12.0s bars does not relax I4, does not authorize residency/streaming,
and must bank the post-linearization profile before another mechanism can be
ruled (`CONTRACT.md:238-242,284-291,329-331`). S1-S5 correctly reserve product
identity, custody, substance, constitutional ambiguity, and platform
impossibility (`:385-399`). On the current source trace, S1 is not asserted, S2
is not needed, S3 is false, and S5 is unproven.

There is one exact status fix. G8/G9 are headed “Acceptance gates,” yet a red bar
is declared neither package failure nor stop (`CONTRACT.md:279-282,329-331,399`).
The implementing session is also forbidden to improvise the next mechanism.
Without a named third result, the phase can neither say PASS nor FAIL nor wait.

**Exact minor-fail correction:** define the terminal classification:

```text
LINEAR-CORRECTION GREEN / EXPERIENCE-BAR RED — STEP-5 RULING REQUIRED
```

On that result, bank all receipts, make no further code change under this
contract, close the implementation phase as technically green for G1-G7+G10,
and open the Fable/Sid step-5 ruling. The overall experience gate remains red;
SEAM felt-gate closure does not proceed. This is not S1-S5 and does not recut
this contract unless the evidence reveals a contract defect. If both bars pass,
classify `PACKAGE PASS` and proceed to the independent FULL gate.

Also pin the bar environment: harness command; production build/URL readiness;
headed vs headless browser; browser version; viewport and DPR; adapter
attestation; CPU/GPU/OS identity; warm/cold cache policy; exact corpus identity;
counter reset; settle definition; timeout. “Same machine class”
(`CONTRACT.md:286`) is not an executable comparison rule.

## 9. What must be preserved in the recut

- One Contract-T result for all seven readers and zero ground fallback
  (`CONTRACT.md:88-105`; W1.md:531-534,582-600).
- G+C+R construction, no C×G/R×G, and only final-segment reshape after one cut
  pass (`CONTRACT.md:106-116,216-228`).
- Layout / paint / GPU-resource lifecycles remain distinct; the correction above
  makes the backend case honor that partition rather than weakening it.
- Origin, camera/zoom, paint/style/selection/caret/hover, backend, atlas, and GPU
  lifecycle stay out of layout identity (`CONTRACT.md:198-208`).
- Cache bounded by live blocks with observable eviction (`:210-212`).
- The §7 step-5 evidence gate and the ban on pre-authorized paint delta,
  residency, and streaming (`:238-242`).
- The §12 foreign-custody fence and frozen SEAM-STEP1 files (`:367-383`).
- The founding numbers, unexplained historical residue, and mandatory future
  adapter attestation remain exactly as recorded (`FOUNDING-EVIDENCE.md:57-65,
  134-149`).

## 10. Recut ledger — exact required changes

1. Replace §5's whitespace prose with an explicit segment/source/consumed-range
   representation and exact reader behavior; define unwrapped synthetic headers,
   no-cluster failure, and same-options U+0020 reference advance.
2. Add the wrap semantic scenario table to the JVM gate, while retaining the
   pre-bound linearity counters/constants and seeded-negative proof.
3. Make §6's source token total for optimistic, paste, machine, auxiliary, and
   unstamped text; replace whole-wear wording with exact visual projections; add
   selection/fold/wear/eviction scenarios to a live gate.
4. Correct G4 to backend repack + reclone + layout reuse, and make its text edit
   capacity-controlled.
5. Choose one hover storyboard count and use it in §7, §9, G3, and G9.
6. Pin executable actions, counter windows, commands, and environments for
   G2-G6 and G8-G10; bind every free constant/quantity.
7. Rewrite G10 around the expected nonzero verifier classification and choose
   one noncontradictory golden carve-out policy.
8. Add the explicit experience-bar-red terminal classification and pin the
   cross-run environment-matching policy.
9. Name the already-sufficient T13 route exactly; do not widen §12.
10. Re-run the entire validation as a new round. This R1 FAIL remains unchanged.
