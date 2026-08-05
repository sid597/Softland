# shaping-correction contract — validation round 2 (fresh context, default-fail) — VERDICT: FAIL

**VERDICT: FAIL.** The R2 recut at
`8ce0377fa6e4f28c1d5cb07af33b4f9fcda11675` is not yet an executable
implementation contract. It materially improves R1: the backend-flip lifecycle
is now correct, the proportionality constants are bound, the T13 provider route
fits the unchanged allowlist, and G10 reproduces W1 §9.1's registered counts.
But six contract defects remain. They admit different implementations, prescribe
an impossible live trace, leave required invalidation cases unproved, or leave
lawful gate/stop states without one classification.

Mode = GATE; altitude = contract; authority = candidate. This round ran on
`docs/current-mental-model-local` at the exact candidate commit above. The
working tree also contained foreign SEAM-STEP1, Studio/playground, source, test,
and verifier changes; none was edited or interpreted as this package's work.
`VALIDATION_R1.md` was treated as the immutable governing defect ledger.
Its probes and accepted evidence profiles were not rerun or redigested. The
eight evidence hashes still equal `FOUNDING-EVIDENCE.md:35-42`; every §11 symbol
used below was freshly re-grepped in the current tree, and all five manifested
source line counts still match. No manifest-substance stop fires.

The recut was read once in full, then read end-to-end a second time for G1-G10,
cross-section, counter, allowlist, bar, stop, and trap coherence before this
verdict was written. No source, contract, R1 record, recut ledger, evidence,
existing verifier, or foreign path was changed.

## 1. R1 §10 consumption verdict

An item is `CONSUMED` only if its correction is complete, internally coherent,
and executable. Letter-only inclusion is `NOT CONSUMED`.

| R1 §10 item | R2 consumption | Ruling |
|---:|---|---|
| 1. §5 representation, whitespace, headers, provider fault, reference advance, readers | **NOT CONSUMED** | The recut adds all named topics, but leading whitespace is simultaneously a break candidate and painted content; the header range representation is only an example; reference-advance fault/nonpositive behavior is absent; hit/copy are law but not G1 assertions. Finding 1. |
| 2. JVM semantic table plus bound linearity and seeded negatives | **NOT CONSUMED** | Constants and both negatives are now bound. The semantic list still omits executable hit-test and copy assertions, and its leading-space expected result cannot be derived uniquely from §5. Finding 1. |
| 3. Total §6 key, exact projections, live deltas, eviction | **NOT CONSUMED** | The source cases are substantially enumerated, but the final visual-projection token is not represented exactly; the R1-required provider-revision and paint-only wear cases are absent; block deletion is fixed to `size -1` without an owned-key precondition; vanished-slot eviction is not tested. Finding 2. |
| 4. Backend repack + reclone + layout reuse; capacity-controlled edit | **CONSUMED IN LAW, NOT EXECUTABLE** | I6 and G4(e) now state the correct three receipts, and (a)/(f) separate in-place from growth. The probe command/exit law and parts of the counter vocabulary remain incomplete, so the item cannot yet pass as an executable gate. Finding 4. |
| 5. One hover storyboard | **NOT CONSUMED** | §9, G3, and G9 reference one textual definition, but its start state plus six alternations end on the largest block, making the declared seventh `ordinary->empty` transition impossible without an uncounted transition. Finding 3. |
| 6. Commands, actions, windows, environment, constants | **NOT CONSUMED** | JVM/fence/compile commands and constants are improved. The live harness has modes but no exact CLI invocations, has no exit for a non-wall assertion failure, and G7 has no command/window/bound. Finding 4. |
| 7. G10 expected nonzero state and one golden policy | **NOT CONSUMED** | The registered W1 counts are correct, but W1's environment fingerprint is not part of the predicate, focused suites are still shorthand, and one golden diff is declared both `G10 FAIL` and `G10 BLOCKED`. Finding 5. |
| 8. Experience-bar-red terminal and cross-run matching | **NOT CONSUMED** | The named bar-red state exists, but S1-S6 have no terminal class; environment-unclassified can overlap another red gate; assertion-red harness outcomes have no exit/class; S6 overlaps PACKAGE FAIL. Finding 5. |
| 9. Exact T13 route without widening §12 | **CONSUMED** | Current source confirms `ground !refs -> :atoms -> :!active-font -> :layout-provider -> metrics/:geom -> ground-block-layout` is available within the five-file edit allowlist. |
| 10. Fresh R2 round | **CONSUMED** | This is the wholly new default-fail round; R1 remains unchanged. |

Because items 1-8 are not all consumed in executable substance, the recut's
claim that it consumes all ten exactly (`CONTRACT.md:8-11`) is false.

## 2. FAIL-class finding 1 — §5 still does not admit exactly one behavior

### Claim in the artifact

> "The semantics below are LAW — each clause admits exactly one behavior and
> each is asserted executably in G1." (`CONTRACT.md:162-165`)

### 2.1 Leading break-whitespace has two lawful outcomes

The cut law says:

> "A **break candidate** is the end of a maximal break-whitespace cluster run
> that has non-whitespace content AFTER it on the same source line."
> (`CONTRACT.md:196-200`)

and:

> "choose the LAST break candidate whose PAINTED PREFIX (all clusters before
> the whitespace run) fits ... The ENTIRE maximal break-whitespace run is
> consumed" (`CONTRACT.md:201-206`)

but then says:

> "Leading whitespace of a SOURCE line is painted content on its first visual
> line (indentation preserved)." (`CONTRACT.md:210-212`)

Trace the required G1 leading-space case with source text `" abc"`. The leading
U+0020 run is maximal and has non-whitespace content after it, so rule 1 makes
its end a break candidate. Its painted prefix is empty and therefore fits every
nonnegative budget, so rule 2 consumes it. Rule 4 requires the same space to be
painted. One implementer can exclude the leading run; another can apply rules
1-2 literally. Both can cite §5.

The same ambiguity recurs after a hard cut leaves a continuation segment whose
first cluster is whitespace. §5 never says whether candidate search and
"prefix" are relative to the source line or the current unconsumed segment.
Current `shaped-segments` is explicitly suffix-based
(`text_layout.cljc:292-307`), so the distinction changes real implementation
behavior.

The `"abc   def"` worked example itself is otherwise coherent: the maximal
`[3,6)` run is consumed; line 1 owns `[0,6)` but paints `"abc"`; line 2 owns and
paints `[6,9)` (`CONTRACT.md:217-222`). The defect is that this example does not
resolve the leading/continuation case.

### 2.2 Header representation is not exact

§5.1 requires every visual line to carry a tagged `:source-range`
(`CONTRACT.md:169-182`). §5.5 then gives only:

> "a tagged synthetic header index domain (e.g. `[:header i]`)"
> (`CONTRACT.md:240-245`, emphasis on the artifact's `e.g.`)

That is a domain token, not an exact start/end range representation. Contract T
requires an explicitly tagged index domain and forbids untagged offsets
(`W1.md:539-544,576-580`). A vector domain, a tagged-offset map, and a
header-local `[start,end)` range all satisfy the recut's example but produce
different line, caret, selection, and paint schemas. G1 says the header index
domain is asserted but does not state which representation is expected
(`CONTRACT.md:513-515`).

### 2.3 Provider fault is not total over reference-advance acquisition

The body fault is now well classified:

> "Nonempty text whose shaped result contains an EMPTY cluster list ... returns
> a poisoned-but-TOTAL result — the line as ONE unwrapped segment"
> (`CONTRACT.md:248-255`).

But `reference-advance` is a separate provider shape of U+0020, ordered exactly
once per key (`CONTRACT.md:258-267`). The contract gives no behavior when that
shape has an empty cluster list, a missing advance, a non-number, zero, or a
negative advance. Only missing/nonpositive `wrap-col` is classified. The key
constructor and block-greedy result therefore are not total over a fault in the
very provider call that creates `inline-size`.

### 2.4 The full reader law is not executable

The prose adds caret, hit-test, selection, and copy behavior
(`CONTRACT.md:224-236`), but G1's semantic cases require only exact line text,
ranges, caret stops, selection regions, layout-ID stability, and shape-call
count (`CONTRACT.md:506-518`). No gate asserts that a hit past painted end maps
to `consumed-start`, or that copied source includes the consumed characters.
An implementation can return the correct line/caret/selection values, map the
hit to `consumed-end`, omit spaces from copy, and still satisfy every written
G1/G6 predicate. This is R1 items 1-2 consumed in prose but not substance.

### Exact required recut

1. Define cut selection as an iteration over a current unconsumed segment. State
   whether a whitespace run whose start equals that segment's start can ever be
   consumed. To preserve the stated indentation law, the minimal rule is:
   a break candidate must have at least one painted non-break cluster between
   the current segment start and the run start; segment-leading whitespace is
   painted and participates in hard-cut fitting.
2. Replace `e.g. [:header i]` with one exact tagged header index-space schema,
   including exact `:source-range`, caret-index, and paint-range forms. State
   whether each header has its own domain or shares a header domain plus ordinal.
3. Define the U+0020 reference-shape fault table. Bind missing/non-number/
   nonpositive advance and empty clusters to one total result, one
   `provider-fault` increment count, and one wrapping consequence.
4. Extend G1 with exact hit-test and copy assertions for single and multiple
   consumed offsets, including a selection wholly inside a consumed range and a
   selection whose endpoint is `consumed-end`.

## 3. FAIL-class finding 2 — §6/G4/G5 do not prove the total key they claim

### Claim in the artifact

> "The constructor is TOTAL: every ground text op ... receives a key by this
> construction, with no uncovered case" (`CONTRACT.md:278-284`).

The current-source census confirms why the totality claim matters. Ground can
render optimistic queue text (`ground.cljs:1416`;
`ground_edit.cljc:445-478`), paste body/header (`ground.cljs:1418-1428`;
`ground_edit.cljc:121-159`), fold display and material-authored headers
(`ground.cljs:1190-1221`), anatomy/invocation strings
(`ground.cljs:1253-1364`), and independent auxiliary text emitters
(`face_primitives.cljc:755-844`). Hover and selections rebuild without changing
text (`ground.cljs:3731-3765,4047-4059`).

### 3.1 The stamped visual-projection token has no exact representation

The stamped branch is:

> "(stamp × the final VISUAL-TEXT projection token)"
> (`CONTRACT.md:286-289`).

The unstamped branch is explicitly a visual-text value hash × source address ×
op role (`CONTRACT.md:290-295`). The stamped branch never says whether its
"projection token" is the final body-text hash, `[body headers]`, raw fold/paste
inputs, a projection revision, or another value. §6 later names fold/default/
header/paste inputs "as PROJECTED into visible text" (`CONTRACT.md:296-302`),
but does not turn that phrase into one constructor input. These alternatives
differ whenever projection metadata changes while final visible strings remain
equal, or a header changes while the body does not.

### 3.2 The R1-required provider and paint-only invalidation rows are absent

R1 required explicit live deltas for provider revision and provenance/attention
paint-only wear (`VALIDATION_R1.md:318-323`). The recut's counter vocabulary
contains `:provider-change` (`CONTRACT.md:382-387`), and §8 even names a font
change probe hook (`CONTRACT.md:401-404`), but G4(a)-(g) never runs it
(`CONTRACT.md:534-560`). Provider identity is key material
(`CONTRACT.md:303-305`), so the gate never proves the most important included
input invalidates all and only affected ground layouts.

Likewise, hover is tested, but provenance tint / attention-box / other
paint-only wear from R1's matrix is not. Binding-only reuse appears only as a
JVM key equality (`CONTRACT.md:519-522`) even though current material watches
re-derive the live ground (`ground.cljs:4614-4650`). A key function can pass the
unit assertion but be bypassed or over-keyed on the live op path.

The recut is internally inconsistent about where binding-only behavior is
proved: §6 promises that a material binding-only revision preserves key equality
"(G4 row g)" (`CONTRACT.md:301-302`), while G4(g) tests only cache hit/miss and
block-deletion size and sends binding-only coverage back to "G1 JVM"
(`CONTRACT.md:558-560`). Neither location proves the required live behavior.

### 3.3 Eviction and order-only coverage are not the stated laws

G4(g) requires one block deletion to make cache size exactly `-1`
(`CONTRACT.md:558-560`). The cache is keyed per layout key, while a live block
can own a body result, headers, and auxiliary/anatomy strings. No pinned deleted
fixture or precondition proves it owns exactly one live cache entry. The same
section's lifecycle law requires eviction on both block death and slot destroy
(`CONTRACT.md:331-336`), but G4 tests only a scripted block delete. Current
source has distinct truth-removal and acknowledged-delete sites
(`ground.cljs:1923-1928,3159-3167`) and a separate vanished-slot destroy road
(`runtime/render.cljs:61-64`). One does not prove the other.

G5 calls its final action an order probe, but the action is a fold toggle and is
expected to execute one `:fold-projection` layout (`CONTRACT.md:561-569`). That
is a combined text-projection change plus origin-shift cascade, not an
order-only change. It usefully proves shifted neighbors do not relayout, but it
does not prove I8's independent statement that an order-only change costs zero
(`CONTRACT.md:139-141`).

### 3.4 The binding render-seam oracle is still cultural

The render-seam constitution defines the unit as:

> "Keyed diffs in, incrementally maintained state, and the batch computation
> kept alive as the oracle, with a fence asserting the two agree ... heavy
> stateful derivations (shaping, layout) are its full case."
> (`decisions.md:421-426`)

The recut adds a keyed result cache, but no G1-G10 predicate compares a reused
cached result with a fresh `text-layout/layout` batch result through the
revision/fold/paste/provider/eviction sequence. §14's future "in-phase
falsifier" mentions correctness under reuse (`CONTRACT.md:754-756`), but it has
no command, equality predicate, fixture, or gate ownership. A stale cached
result can report the requested hit/miss/layout-exec counters and pass while
violating the binding oracle law.

### Exact required recut

1. Define one stamped projection token exactly, minimally as an exact token over
   the final visible body text plus ordered visible headers and op role; say how
   fold/paste/default inputs enter only through that projection. Use the same
   representation in the key function signature and G1 expected values.
2. Add a same-backend provider/font identity-change row to G4. Pin the affected
   live key count before the action; require exactly that many
   `:provider-change` layout executions, unequal layout-ID digest, and exact
   paint/geo consequences. Add a live binding-only row and at least one
   provenance/attention paint-only row with layout delta zero.
3. Replace cache `size -1` with `size - owned-key-count`, where the owned count
   is captured before deletion for a named fixture. Exercise truth death and
   vanished-slot destruction separately, or explicitly prove they converge on
   the same eviction owner before asserting one scenario.
4. Add a genuinely order-only hook/action whose body/header/key inputs remain
   byte-equal. Keep the existing fold/origin cascade as a separate combined
   scenario.
5. Fold the §14 reuse falsifier into G1/G6 or G4 as an executable oracle:
   compare every reused result to a fresh batch `text-layout/layout` result for
   exact layout ID, lines/ranges, glyph spans, readers, and output receipt. Add
   a fence/self-test that proves a seeded stale-cache return fails.

## 4. FAIL-class finding 3 — THE seven-transition storyboard is impossible

### Claim in the artifact

> "hover the largest once as UNMEASURED warm-up; RESET counters; then SIX
> measured alternating transitions (largest->ordinary, ordinary->largest, ×3);
> then the SEVENTH measured transition ordinary->empty"
> (`CONTRACT.md:417-423`).

### Explicit trace

After the warm-up, hover is on `largest`. The six declared alternations are:

1. largest -> ordinary
2. ordinary -> largest
3. largest -> ordinary
4. ordinary -> largest
5. largest -> ordinary
6. ordinary -> largest

The current hover after step 6 is `largest`. The required seventh transition
starts at `ordinary`. Reaching ordinary first requires an extra
largest -> ordinary input, rebuild, RAF, G8 pair, and two paint repacks. If it
occurs after reset, G3's exact `paint-repacks :hover-paint = 13` becomes at least
15; if it is hidden, §9's "exactly ONE post-input RAF/G8 pair" accounting is
false (`CONTRACT.md:421-423,529-533`).

Current source confirms hover changes only on a different picked UID and
rebuilds the old and new blocks (`ground.cljs:4052-4059`); there is no
zero-event state teleport. The banked four-transition evidence was physically
coherent because its measured sequence ended on ordinary before
ordinary -> empty (`hover-summary.json:31-87`; `FOUNDING-EVIDENCE.md:108-120`).

This is not three conflicting textual cardinalities as in R1; it is one shared
definition whose state machine cannot execute. Therefore R1 item 5 is not
consumed in substance.

### Exact required recut

Preserve the intended six ordinary/large transitions, the ordinary control, and
the exact count 13 by defining this sequence once:

1. Hover `largest` once as unmeasured warm-up and await its one RAF/G8 pair.
2. Move to `ordinary` as an unmeasured positioning step and await its one
   RAF/G8 pair.
3. RESET counters **after** ordinary is the confirmed current hover.
4. Measure `(ordinary -> largest, largest -> ordinary) ×3`; this ends on
   ordinary.
5. Measure `ordinary -> empty` as transition seven.

State that the two unmeasured pairs occur before reset and are absent from every
delta. Use this exact ordered list in §9; G3/G9 may reference it.

## 5. FAIL-class finding 4 — the live gates and counter schema are not executable

### 5.1 The harness has modes, not commands

§9 names the file and modes:

> "`profile_shaping_correction.mjs` ... modes `cold` | `hover` | `probe`"
> (`CONTRACT.md:408-416`).

G2-G5 and G8-G9 then say only "Harness `cold` mode", "Harness `hover`
mode", or "Harness `probe` mode" (`CONTRACT.md:526-535,561-569,585-590`).
Nothing says whether the executable form is
`node ... cold`, `node ... --mode=cold`, an npm script, or an environment
variable. The URL is merely "recorded" (`CONTRACT.md:455-456`), not supplied by
an exact invocation. A fresh implementer cannot run these gates exactly as
written.

G7 is even less bounded:

> "`store-frame-execs` present in the ground report, bounded, and NOT used to
> authorize anything" (`CONTRACT.md:582-584`).

It has no harness mode, command, environment, reset/absolute window, numeric or
structural meaning of "bounded", or expected exit.

### 5.2 The harness exit law has a missing assertion-red outcome

The only exits are:

> "`0` = every assertion for the mode green; `2` = counters/receipts green but
> a wall bar red; `1` = harness/environment fault"
> (`CONTRACT.md:411-416`).

No exit is assigned when a product assertion is red: for example ground
fallback is nonzero (G2), layout executes on hover (G3), a G4 delta is wrong,
or a G5 camera write occurs. `0` is forbidden, `2` requires counters green, and
`1` falsely calls the product failure a harness/environment fault. Probe mode
has no wall bar at all, so every G4/G5 assertion failure falls into this gap.

### 5.3 §8 and the gate assertions do not use one counter vocabulary

The declared `geo` fields are:

> "fresh / in-place / recloned / destroyed / capacity-grown"
> (`CONTRACT.md:388-390`).

G4(a) instead asserts `clone ... 0` (`CONTRACT.md:536-540`); `clone` is not a
declared field. G5 asserts `slot-text-write` / `slot-text-writes`
(`CONTRACT.md:565-569`), neither of which appears in §8. Calling it "the G8
reshape counter" does not specify whether it is a permanent scalar, a cause map,
the existing console field, or derived from `geo`.

§8 declares `dirty` (`CONTRACT.md:395-397`) and I7 says text change, entry,
exit, hover paint, and order must participate correctly in dirty/present-region
ownership (`CONTRACT.md:134-138`), but no gate gives an expected `dirty` result.
G8 merely banks the RAF/G8/report lines. An implementation can leave `dirty`
present but wrong for every required cause and pass G1-G10.

G4(c) and G4(d) are introduced as "expected deltas EXACT"
(`CONTRACT.md:534-535`) but omit paint and every geo field for the selection
probes, and omit all geo expectations for fold/paste (`CONTRACT.md:542-546`).
§8 says the probe actions are exposed through hooks but its concrete list omits
caret, text-selection, machine-selection, paste flip, capacity growth, pan,
zoom, and order-only mutation (`CONTRACT.md:401-404`). The harness could choose
private ways to cause these states and still claim the named hooks.

### 5.4 G10 still contains command shorthand and an incomplete exact pin

The focused suites are names followed by "same `clj -M:test -e` runner form"
(`CONTRACT.md:593-602`), not the exact command the section claims to pin. The
warning gate does not define how a "distinct warning" is normalized before
baseline comparison (`CONTRACT.md:617-620`).

The numerical W1 state is correctly copied: exit 1,
`candidate-pick-parity-failure`, 21/21 determinism and golden comparison, 7/7
bounds sentinels, 14/21 parity, SDF and Slug 7/7, MSDF 47 decisive mismatches per
each of seven regimes = 329, plus two ties per regime
(`CONTRACT.md:608-616`; `W1.md:723-736`). Preserve those values.

However, W1 §9.1 also pins environment fingerprint
`e79490f8882cd785f32b5bb82cadd425dc90f2d7616cc9f0debf8a0f1c476282`
(`W1.md:737-738`). The recut says the state matches W1 §9.1 exactly but omits
that field and says only another code, classification, or **count** fails. The
current verifier's structured close emits `environmentFingerprint`
(`run_verifier.mjs:491-505`), while candidate-parity classification is selected
before environment mismatch (`run_verifier.mjs:435-446`). The expected
classification alone therefore does not prove the W1 environment match.

### Exact required recut

1. Give literal invocations for every harness use, including URL and mode syntax,
   and state the expected exit beside G2-G5 and G7-G9. Name readiness checks or
   make the harness own them.
2. Make the harness exit table total and disjoint. For example: `0` all mode
   assertions green; `2` every non-wall assertion green and only a wall bar red;
   `3` at least one product/counter/receipt assertion red; `1` only
   harness/environment fault. Bind terminal classification to those exits.
3. Add `slot-text-writes` to §8 with one spelling and exact cause/window
   semantics, or rewrite G5 in terms of declared `geo` fields. Replace `clone`
   with the declared `fresh`/`recloned` fields. Complete every omitted G4 field.
4. Give exact `dirty` expected coverage for text edit, entry, exit, hover, order,
   and camera; assign those assertions to G4/G5/G8 without adding a gate number.
5. Enumerate every required probe action and its public dev hook or literal UI
   drive. A private harness mutation is not the truth road.
6. Spell out the focused-suite command, define warning-signature normalization,
   and add W1's exact environment fingerprint to G10's expected structured
   close (or explicitly amend the binding pin if the fingerprint is intentionally
   not invariant).

## 6. FAIL-class finding 5 — terminal classifications have gaps and overlaps with S1-S6

### Claim in the artifact

> "every lawful end state has exactly one name" (`CONTRACT.md:630`).

The table names PACKAGE PASS, PACKAGE FAIL, experience-bar red, and environment
unclassified (`CONTRACT.md:632-645`). It names no terminal state for any stop
S1-S6 (`CONTRACT.md:724-741`). S1-S5 halt implementation before a gate result;
none is pass, gate-red fail, bar-red, or environment-unclassified. S6 explicitly
leaves G10 blocked rather than red. These are lawful contract end states with no
name.

S6 also creates an internal overlap:

> "any golden diff is a G10 FAIL ... it fires stop S6 ... and G10 is BLOCKED
> FOR ADJUDICATION" (`CONTRACT.md:621-627`).

The same diff is therefore both G10 red, which triggers PACKAGE FAIL
(`CONTRACT.md:634`), and blocked, which has no terminal class. §13 repeats the
blocked reading (`CONTRACT.md:738-741`). The heading "one, contradiction-free"
does not make these two statuses equal.

Environment-unclassified also lacks package precedence. If a JVM G1 assertion
is red while the live harness has a fallback adapter, PACKAGE FAIL and
UNCLASSIFIED both match unless the latter is only a gate-local status. The text
calls all four package terminal classifications and supplies no precedence.
Finding 4's missing product-assertion exit creates an additional gap before the
terminal table is even applied.

The experience-bar state itself is well formed and should be preserved: it
requires G1-G7+G10 green, a valid environment-matched G8/G9 bar red, banks
receipts, forbids further implementation under this contract, and opens the
step-5 ruling (`CONTRACT.md:635-642`). It does not overlap PACKAGE FAIL.

### Exact required recut

1. Add one explicit terminal decision table with precedence and package-vs-run
   scope. At minimum name `PACKAGE BLOCKED — S1`, ..., `PACKAGE BLOCKED — S6`
   (or one `PACKAGE BLOCKED — S<n>` family), and state that a blocked state is
   neither red, green, bar-red, nor environment-unclassified.
2. Make golden custody one state. The minimal coherent law is: any golden diff
   first yields `G10 BLOCKED — S6`; after adjudication it becomes either G10
   FAIL with no golden edit, or an explicitly authorized separately reviewed
   golden-custody path followed by a fresh G10 run. It is not a G10 pass or fail
   before that ruling.
3. Define `UNCLASSIFIED (environment)` as the status of the affected live run,
   then define the package result when another independent gate is red. A simple
   total precedence is `BLOCKED > PACKAGE FAIL > UNCLASSIFIED > BAR RED > PASS`,
   with exact preconditions so only one row can match.
4. Integrate the total harness exits from Finding 4 into this table. An
   assertion-red run must become PACKAGE FAIL; exit 1 remains unclassified.

## 7. FAIL-class finding 6 — the counter/cache correction has no executable oracle fence

This is separated from Finding 2 because it is not merely another missing G4
row; it is a contradiction with binding settled ground.

The recut calls the cache process-local, keyed, bounded, and observable
(`CONTRACT.md:331-336`) and declares that reused layout results are the one
authority (`CONTRACT.md:95-104`). But its only cache correctness checks are key
equality/inequality, hit/miss/size counters, layout ID digest, and a future
unnamed falsifier. None compares the **contents** of a reused layout result to
the retained batch computation.

The binding render-seam constitution does not permit that to remain a review
custom:

> "a batch stage is never deleted when its incremental sibling arrives — it is
> demoted to that sibling's oracle" (`decisions.md:421-430`).

A cache hit returning the previous result under an incorrectly equal key can
keep layout-execs at zero, preserve the old ID digest, produce plausible
paint-repack counts, and still give stale wrap/caret/hit geometry. The declared
counters are not an oracle.

### Exact required recut

Make the fresh `text-layout/layout` batch call the named oracle and put the
comparison in a numbered gate. The smallest fit is G1 for pure key/result
sequences plus G4 for the live carried path. Require exact equality of semantic
outputs (ID, lines, tagged ranges, glyph/cluster/run spans, metrics, and all
reader results) after reuse and after each included/excluded input transition.
The new fence must fail a seeded cache implementation that returns the prior
result after a projection/provider change. §14 may still run a focused finder,
but it cannot substitute for the binding gate.

## 8. G1-G10 final executability matrix

| Gate | R2 verdict | Exact reason |
|---|---|---|
| G1 | **FAIL** | Numeric proportionality predicate is now good, but §5 leading whitespace is contradictory; header schema is exemplary; reference-advance faults are unclassified; hit/copy and cache-oracle assertions are absent. |
| G2 | **FAIL** | Target counters are clear; literal harness command and assertion-red exit are absent. |
| G3 | **FAIL** | The exact repack count 13 is sensible only for a physically coherent sequence; THE storyboard cannot reach its seventh start state. Literal command/exit also absent. |
| G4 | **FAIL** | Backend law is corrected, but provider/paint-only/live-binding/slot-destroy cases are absent; some deltas and action hooks are incomplete; command and assertion-red exit absent. |
| G5 | **FAIL** | Pan/zoom actions are concrete, but `slot-text-writes` is undeclared and the order probe is a fold+order combined event, not order-only. Command/exit absent. |
| G6 | **PASS as gate shape** | JVM bound, glyph-index unit, line bound, negative, fence command, and exits are explicit. It still must absorb the cache oracle required above before the package can pass settled ground. |
| G7 | **FAIL** | No command, mode, environment, observation window, definition of bounded, or expected process result. |
| G8 | **FAIL** | Environment and settle definition are strong; literal invocation, assertion-red exit, and dirty expectations are absent. |
| G9 | **FAIL** | Environment/bar are strong; impossible storyboard and missing literal invocation/exit prevent execution. |
| G10 | **FAIL** | W1 counts and expected exit 1 are correct; exact environment fingerprint is omitted, focused suites remain shorthand, warning comparison is underdefined, and a golden diff is both fail and blocked. |

## 9. Cross-section and allowlist sum-check

- **One phase / G1-G10 ownership:** PASS in structure. The Codex lane owns the
  one phase and Fable independently re-verifies; no gate silently transfers
  implementation to Sid (`CONTRACT.md:1,441-444,747-760`).
- **§9 vs §7/G3/G9:** FAIL. All references point to one storyboard, but that
  storyboard is not executable (Finding 3).
- **§8 vs gate assertions:** FAIL. Undeclared `clone` and
  `slot-text-write(s)`, unused `dirty`, untested `:provider-change`, and
  incomplete G4 deltas remain (Findings 2 and 4).
- **§12 vs ordered work/created files:** PASS. Linear indexes and shared span
  helpers fit `text_layout.cljc`; carried ground results fit
  `face_primitives.cljc`/`ground.cljs`; paint selection fits `renderer.cljs`;
  lifecycle partition fits `runtime/render.cljs`; the three new test/harness
  files and package records are allowed. Current provider access is already
  available through `!refs`/`:!active-font` without editing
  `runtime/state.cljs` or `runtime/fonts.cljs`.
- **Stops vs terminals:** FAIL. S1-S6 are unclassified; S6 overlaps G10 FAIL
  (Finding 5).
- **Experience bars / step 5:** PASS in law. A valid bar red does not weaken I4
  or pre-authorize paint delta/residency/streaming.
- **G10 verifier state:** PASS on W1's counts/classification; FAIL on the full
  exact-state predicate and S6 policy (Findings 4-5).
- **T1-T14:** T1-T5, T7-T8, T10-T14 cite live substance and coherent remedies.
  T6/T9 remain under-gated by Finding 2; T5's backend lifecycle text is now
  correct; T13's route is source-valid and allowlist-safe.

## 10. Required recut ledger — R2 FAIL -> R3 candidate

1. **Close §5 completely.** Make break-candidate search relative to the current
   segment and exclude/define segment-leading whitespace; replace the exemplary
   header tag with one exact range/index schema; classify every
   reference-advance fault/nonpositive value; add hit/copy and consumed-interior
   reader assertions to G1.
2. **Make the §6 token and oracle exact.** Define the stamped final-projection
   token over exact visible body/header/role values; add a retained batch-layout
   oracle comparison with a seeded stale-cache negative.
3. **Complete the invalidation matrix.** Add same-backend provider change,
   live binding-only reuse, provenance/attention paint-only reuse, exact
   owned-key eviction for both truth death and vanished-slot destruction, and a
   genuine order-only action. Keep the corrected backend repack + reclone +
   identical-layout-ID case unchanged.
4. **Repair THE storyboard.** Warm largest, position ordinary unmeasured, reset,
   measure `(ordinary->largest, largest->ordinary) ×3`, then
   `ordinary->empty`; bind the two pre-reset RAF/G8 pairs and preserve measured
   repacks = 13.
5. **Make the harness executable and total.** Give literal cold/hover/probe
   commands; enumerate all action hooks/truth roads; add one disjoint exit for
   product assertion failure; bind G7 to a command/window/bound; define every
   G4 field and the permanent `slot-text-writes`/`dirty` counters.
6. **Finish G10's exact command/state predicate.** Spell the focused-suite
   command, define warning-signature comparison, retain all W1 counts, and bind
   the W1 §9.1 environment fingerprint (or make an explicit binding amendment,
   not an omission).
7. **Make terminal classification total.** Add a disjoint blocked state for
   S1-S6, make S6 block before a fail/pass ruling, and specify precedence among
   blocked, fail, environment-unclassified, experience-bar red, and pass.
8. **Preserve accepted ground.** Do not widen §12, do not touch SEAM-STEP1 or
   existing verifiers, do not weaken Contract T, I4, the bars, the step-5
   evidence gate, T13, or W1's intentionally RED MSDF state.
9. **Run a wholly fresh default-fail R3.** Keep R1 and this R2 FAIL immutable;
   do not open implementation from this candidate.
