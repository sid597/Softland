# shaping-correction contract — validation round 4 (fresh context, default-fail) — VERDICT: PASS

Run 2026-08-04 over the R4 candidate at
`990b814e363371f3cc892446de38e96e2a603190` (`990b814`) on
`docs/current-mental-model-local`. Mode = GATE; altitude = contract;
authority = candidate. Fresh context relative to the R4 recut author (the
recut was a Codex session per the baton; this round is the Fable lane).
The working tree carried the same pre-existing foreign entries (SEAM-STEP1 +
studio/playground layers) named by §12; none was edited, staged, adopted, or
treated as shaping-correction work. This file is the only write made by the
R4 validation round.

Immutability: `VALIDATION_R1.md`, `VALIDATION_R2.md`, and `VALIDATION_R3.md`
were treated as immutable. Their SHA-256 values are, respectively,
`5fd6ef792505d8fc74635821d60cdd4b3c423608a1f32f9554e3ff83b7bf1172`,
`05e584bfb864c987f153b0d72017f2ed6d28196f27877879188590c08ce10eb3` (both
matching their previously banked values), and
`f6448d455dec06a15eb8102ac31538b388abb05ca2a66f3c81d593eaccf9a800` (matching
the baton's recorded `f6448d45…a800`). The candidate `CONTRACT.md` at this
commit is
`3699f3fb7db413d4b8b07936e4192950f486171aae02a90b4a408028e128e19b`;
`RECUT_LEDGER_R3.md` is
`5d3368b33df74963fe96468a02fabcc29ae5a187692b40d2ede036f2c5105463`.

No founding profile, live browser gate, test suite, implementation probe, or
implementation command was run; prior rounds' evidence probes were not rerun.
This round is a static executability, semantic-totality, consumption, and
terminal-classification gate over the committed candidate and the banked
records, plus targeted read-only source verification of the claims the R4
recut newly introduced (the §6 owner/role census, the fold-header-hit
no-text claim, the Jetty port pin, the W1 fingerprint, the verifier's two
output surfaces, and the recut-corrected `ground_edit.cljc` locator).

---

## 1. Verdict

**VERDICT: PASS.** The candidate is an executable implementation contract
under the commissioned bar:

- **Every gate is runnable exactly as written.** G1–G10 each carry a literal
  command with a pinned expected result (G1 `CONTRACT.md:766-767`; G2–G5,
  G8, G9 name §9's four literal invocations `CONTRACT.md:637-642`; G6's JVM
  half rides G1's command and its static half has its own command
  `CONTRACT.md:1074-1078`; G7 rides G4's two commands `CONTRACT.md:1079-1088`;
  G10 pins six commands each with its expected exit/state
  `CONTRACT.md:1102-1148`). Every free constant of R1's finding 1 is bound;
  every counter a gate asserts is declared in §8's vocabulary.
- **Every §5/§6 clause admits exactly one behavior** under every trace this
  round could construct (§5 below). The R3 blocker — an unreadable header
  range literal — is closed with valid EDN and half-open semantics stated
  outside the datum (`CONTRACT.md:290-294`).
- **Terminals are total and disjoint**: ordered first-match rows 1–5 with a
  sorted canonical multi-stop name, an assertion-red law covering every
  G1–G10 gate including G8/G9, a single timeout classification, and an
  executable seven-fixture truth table owned by the G4 positive command
  (`CONTRACT.md:1164-1227`).

All ten `VALIDATION_R3.md` §10 items are **CONSUMED** in executable
substance (§2). Through them, the previously unconsumed R2 items 1, 2, 3, 5,
7 and R1 items 1–4, 6, 8 are **CONSUMED** (§3, §4). The previously consumed
R2 items 4, 6, 8, 9 and R1 items 5, 7 remain **unweakened** (§3, §4).
`RECUT_LEDGER_R3.md`'s consumption claims are accurate — including its
prior-ledger closure map — and, unlike the R1/R2 ledgers, do not overclaim.

Three non-blocking **advisories** are recorded in §7 with their cheap
falsifiers. None meets this chain's FAIL standard (a conforming implementer
forced to supply policy absent from or contrary to the candidate; an
unrunnable gate; a non-total or non-disjoint terminal), and none sits in a
§5/§6 semantic clause. They travel to the implementer prompt and the
FULL-tier gate as receipts; they do not amend the contract.

**This verdict does not open implementation by itself.** §1's sequencing
holds: SEAM-STEP1's code lands first, the phase opens after that landing
with the §11 manifest re-sweep, and §14 steps 2–5 follow. Sid may re-order
with one line.

## 2. `VALIDATION_R3.md` §10 consumption rulings

Consumption means complete, coherent, and executable. Presence of the
requested nouns or rows is not consumption.

| R3 §10 item | R4 consumption | Ruling |
|---:|---|---|
| 1. Make §5.5's exact values readable | **CONSUMED** | Header offset `[:header h j]` and range `[:header h [start end]]` are valid Clojure/EDN; the nested two-vector's half-open meaning is declared outside the datum; the header line value is `[:header h [0 len]]`; body offsets/ranges are exact maps/two-vectors and interval notation is declared prose-only (`CONTRACT.md:186-194,286-305`). G1's two-header case asserts the full tagged values (`CONTRACT.md:797-799`). Per-header domain, no cross-header range, no body alias, unwrapped headers, and caret laws preserved. |
| 2. Close the key in executable substance | **CONSUMED** | The key is the literal `[source-token provider-token metric-token]` with full visible body text (no hash, no collision policy needed), ordered header strings, a closed 17-role domain with an exact first-match assignment table, an exact address `[:ground-text subject-id op-role occurrence]` over an enumerated owner domain, exact revision/provider/metric tokens, and fixed/derived constructor inputs (`CONTRACT.md:355-445`). G1 pins one fully readable expected key literal — the strongest executable form — and this round verified its provider token carries 12/12 declared fields and its metric token 10/10 (`CONTRACT.md:823-852` vs `420-438`). Reference advance is derived post-key: MISS = 1 shape, HIT = 0, `[:unbounded]` = 0, asserted in G1 (`CONTRACT.md:318-331,780-784,850-852`). Census verified in source this round: §5. |
| 3. Make the cache genuinely bounded | **CONSUMED** | Two maps, zero-or-one current entry per live address, atomic replacement with size delta 0, three removal roads, `size = address count` at every observation, exact `:id-digest` definition (`CONTRACT.md:476-500`). Since the key contains the address, distinct addresses cannot share a key, so `size = address count` is sound. G1's pure lifecycle table includes the 1,000-edit row (size stays 1) and owner-death −O with idempotent second death (`CONTRACT.md:853-861`); G4 rows use replacement +1/size 0 and owned-count eviction, never −1 (`CONTRACT.md:932-1004`); T9 restates the bound (`CONTRACT.md:1395-1399`). No clause calls revision accumulation bounded. |
| 4. Separate oracle work from production work exactly | **CONSUMED** | `oracle-layout-execs` is a declared §8 counter, zero outside oracle windows, equal to `:oracle-checks` inside; oracle recomputes bypass the production cache and never enter production `layout-execs`, so `misses = layout-execs` holds in oracle windows (`CONTRACT.md:502-515,569-577,602-611`). JVM seeded negatives are captured predicate rejections whose test assertions PASS (`within-work-bound?`/`within-span-bound?`/`oracle-match?` returning false is the asserted success), so the one G1/G6 command still exits 0 (`CONTRACT.md:862-870,1068-1074`). |
| 5. Repair G4's row algebra and repetition protocol | **CONSUMED** | Provider-change packs are counted: row (i) expects `paint-repacks :provider-change = L`, agreeing with §8's pack-cause law instead of contradicting it (`CONTRACT.md:578-583,993-1004`). The run-twice law is replaced by two isolated trials, each in a fresh disposable context/page (fresh process-local cache) with byte-equal SHA-256 pre-state snapshots and product/truth-road setup/restore outside the measured window (`CONTRACT.md:891-905`) — this dissolves R3's toggle-back contradiction (each trial's first toggle misses), the delete-twice problem (row g's fixture is created afresh per trial, `CONTRACT.md:972-977`), and reset-is-not-restore. Row (l)'s positive runs in each trial; its negative is explicitly a one-trial mutation-rejection proof (`CONTRACT.md:917-920,1017-1035`). Backend dirty `:other` is RUN-RECORDED ≥ 1 with trial-2 equality (`CONTRACT.md:962`). |
| 6. Give the live oracle negative its own literal invocation | **CONSUMED** | Four literal invocations; `--case=` is required for `probe` only, has exactly `positive`/`oracle-negative`, is forbidden for `cold`/`hover`, and no environment variable or run count selects a case (`CONTRACT.md:637-648`). G4 pins positive exit 0 and negative exit 3 with red set exactly `["oracle-mismatch"]`, mismatch count 1, and the package-failure exemption bound to exactly those conditions (`CONTRACT.md:885-889,1023-1035,1180-1184`). The negative's full sequence — settle, warm both blocks, largest current, enable, RESET, seed via the one oracle-only hook, one hover, restore/disable outside the window — is spelled step by step. |
| 7. Choose one settle-timeout classification | **CONSUMED** | One law, stated identically three times: navigation/readiness failure BEFORE valid attestation = exit 1/unclassified; any required readiness or settle timeout AFTER valid attestation = product assertion red, exit 3, G8 red, PACKAGE FAIL, "never unclassified" (`CONTRACT.md:656-667,743-747,1193-1196`). No clause anywhere still calls the 120s timeout unclassified. |
| 8. Make terminals total and disjoint | **CONSUMED** | Stop-set law computes one sorted nonempty `B` with a canonical display (`PACKAGE BLOCKED — {S2,S4}`), so simultaneous stops have exactly one name; the assertion-red law routes any non-exempt red in ANY G1–G10 gate — explicitly including G8/G9 and the post-attestation timeout — to PACKAGE FAIL; the experience row is reserved for environment-matched wall-only exit 2; rows are tested in order, first match wins; the seven-fixture pure truth table runs in the G4 positive before any live probe and must report `7/7` (`CONTRACT.md:1164-1227`). Adversarial probes traced in §5 all classify uniquely — including R3's killer state (G8 dirty-conservation red with everything else green → row 2). |
| 9. Preserve accepted ground | **CONSUMED** | Verified item by item in §8 below. §12 is byte-level unchanged in substance; no SEAM file, existing verifier, store schema, or foreign work is touched; Contract T/I4/bars/step-5/W1 RED state/storyboard/backend law all stand. |
| 10. Run a wholly fresh default-fail R4 | **CONSUMED** | This round. R1/R2/R3 remain byte-identical (hashes above); no implementation opened; no prior evidence probe was rerun. |

## 3. `VALIDATION_R2.md` §10 rulings through R3's items

Previously unconsumed items:

| R2 §10 item | R4 consumption | Route |
|---:|---|---|
| 1. Close §5 completely | **CONSUMED** | R3 had already accepted the segment-relative cut law, leading-whitespace law, fault rows, and reader rows as materially repaired; the sole blocker was the unreadable header literal, closed by R3 item 1 (§2 above). The reader law is in G1 as executable rows: hit-test past painted end → consumed-start (single- and multi-space), caret stops across a consumed run, zero-width interior selection, selection ending at consumed-end, and both copy cases (`CONTRACT.md:808-822`). |
| 2. Make the §6 token and oracle exact | **CONSUMED** | Via R3 items 2 and 4: full-text VPT (hash and collision law mooted), exact `stable-source-address`, closed `op-role` mapping, exact provider/metric tokens, reference-advance acquisition order and counts, and the gate-owned oracle with separated accounting (§2 items 2, 4). |
| 3. Complete the invalidation matrix | **CONSUMED** | Via R3 items 3 and 5: the provider-change (i), binding-only (j), provenance/attention paint-only (k), truth-death owned-count (g), vanished-slot-or-convergence-proof (h), and genuine order-only (G5) rows all exist AND are now jointly executable under the isolated-trial protocol and the replacement cache algebra (§2 items 3, 5). |
| 5. Make the harness executable and total | **CONSUMED** | Via R3 items 6 and 7: four literal commands, the `--case` selector law, one timeout classification, the total/disjoint 1→3→2→0 exit order with "exactly one exit matches any run" (`CONTRACT.md:656-676`), the complete hook enumeration (`CONTRACT.md:613-630` — every G4/G5 scripted action is a listed hook or a literal pointer/hover UI drive), and G7 bound to G4's commands and windows with the exact `store-frame-execs ≤ raf-frames` bound (`CONTRACT.md:1079-1088`). |
| 7. Make terminal classification total | **CONSUMED** | Via R3 item 8 (§2). |

Previously consumed items, checked for weakening:

- **Item 4 (THE storyboard) — unweakened.** The ordered five-act list is
  intact and physically coherent: unmeasured largest warm-up → unmeasured
  positioning move to ordinary → RESET only after ordinary is confirmed
  current → six measured `(ordinary→largest, largest→ordinary)` ×3 ending
  on ordinary → the seventh `ordinary→empty` control; the two pre-reset
  RAF/G8 pairs appear in no measured delta; measured `:hover-paint` = 13 =
  6×2+1 (`CONTRACT.md:677-696`). G3/G9 reference it and restate nothing
  (`CONTRACT.md:877-884,1097-1101`).
- **Item 6 (G10 exact command/state predicate) — unweakened, and
  re-verified at source this round.** The nine-namespace literal command
  stands (`CONTRACT.md:1105-1107`); the verifier's console structured close
  emits exactly the asserted fields — `pass`, `classification`,
  `deterministic`, `candidateParity`, `productBoundsDivergenceSentinels`,
  `images`, `q8AffineTransport`, `q5AffineRasterBoundary`,
  `updateRequested`, `updateAuthorized`, `environmentFingerprint` —
  (re-read this round at `run_verifier.mjs:491-510`), the receipt file is a
  genuinely distinct surface carrying `goldenComparison`
  (`run_verifier.mjs:481`), and the pinned fingerprint
  `e79490f8882cd785f32b5bb82cadd425dc90f2d7616cc9f0debf8a0f1c476282` is
  byte-equal to `W1.md:738`. Warning-signature normalization and the
  verbatim pre-edit baseline stand (`CONTRACT.md:1138-1148`). |
- **Item 8 (preserve accepted ground) — unweakened** (§8 below).
- **Item 9 (fresh round) — honored**; R3 ran then, R4 runs now, all prior
  rounds immutable.

## 4. `VALIDATION_R1.md` §10 rulings

| R1 §10 item | R4 consumption | Route |
|---:|---|---|
| 1. Exact §5 representation and reader behavior | **CONSUMED** | Every required line value is now a representable literal (body maps/two-vectors, header tagged vectors); reader behavior is law (§5.4) AND gate (G1 reader rows). |
| 2. JVM semantic table, bound linearity, seeded negatives | **CONSUMED** | The two-header case now has readable expected values; the seeded negatives are captured predicate rejections compatible with the one exit-0 command (§2 item 4); the constants k=4, growth 5×, the log-sort allowance, span +8, and the shape-call bound all remain bound (`CONTRACT.md:769-784,1059-1067`). |
| 3. Total §6 key, exact projections, live deltas, eviction | **CONSUMED** | §2 items 2, 3, 5. Key equality is total and literal; the cache bound is real (replacement, not accumulation); the live rows are internally consistent. |
| 4. Backend repack + reclone + layout reuse; capacity-controlled edit | **CONSUMED** | Row (e) keeps the three-lifecycle law (layout 0, `:id-digest` equal, repacks = live slot count, reclone/destroy receipts, provider-token-equality precondition receipt) and rows (a)/(f) keep in-place vs capacity-grown separated — now under an executable command, trial protocol, and complete row algebra (`CONTRACT.md:956-971`). |
| 5. One hover storyboard | **Remains CONSUMED** | Unweakened (§3, item 4). |
| 6. Commands, actions, windows, environments, constants | **CONSUMED** | Four literal invocations including the distinct positive/negative probe pair; per-row setup/restore protocol replaces the missing state-restoration semantics; one timeout law (§2 items 5–7). |
| 7. Exact G10 nonzero state and one golden policy | **Remains CONSUMED** | Unweakened, source-re-verified (§3, item 6); the golden custody chain is still ONE chain — any diff → G10 BLOCKED — stop S6 before any fail/pass, never dual-named (`CONTRACT.md:1149-1162,1328-1334`). |
| 8. Experience-bar-red terminal and cross-run matching | **CONSUMED** | The encompassing terminal claim is now total for G8/G9 assertion red (row 2 names them) and disjoint for simultaneous stops (sorted `B`); the bar-red row requires the environment-matched wall-only exit 2; the cross-run comparison policy stands (`CONTRACT.md:748-752,1186-1215`). |

R1 items 9–10 stand consumed (T13 route unchanged at
`CONTRACT.md:1411-1415`; this is the fourth fresh round).

## 5. Fresh default-fail sweep — the traces that authorize PASS

### 5.1 §5 single-behavior traces

Every trace below resolves to exactly one lawful output:

- **`" abc"`, tight budget.** The leading run starts at the segment's first
  offset → rule 1 paints it; rule 2(i) excludes it from candidacy by
  construction ("at least one painted cluster precedes it in THIS
  segment"), so R2's two-lawful-outcomes fork is closed. No other
  candidate → hard cut if overflowing (`CONTRACT.md:222-249`).
- **`"abc   def"`, budget = 5 space advances.** The worked example is
  uniquely derivable: run [3,6) is a candidate, painted prefix `"abc"`
  fits, whole run consumed, line 2 starts at 6 (`CONTRACT.md:258-263`).
- **Hard-cut remainder beginning with whitespace.** Rule 4 + rule 1: the
  continuation segment's leading run is painted, never consumed; a
  whitespace run longer than the budget hard-cuts inside itself
  (`CONTRACT.md:222-229,250-253`); G1 has the dedicated case
  (`CONTRACT.md:790-793`).
- **Fully fitting line / trailing whitespace / unbounded token.** The
  pre-rule emits one final painted segment and stops; trailing whitespace
  is painted by candidate condition (ii); unbounded is unconditional
  (`CONTRACT.md:214-221,236-239`).
- **Provider faults.** Body fault (§5.6) and all five reference-advance
  fault classes (§5.7) each have one lawful consequence — poisoned-total
  result or unbounded-lines — with exactly one `provider-fault` increment,
  each a G1 row (`CONTRACT.md:307-344,803-807`).

Not legislated and lawfully so: hit-testing past the painted end of a
HARD-cut line (no consumed range exists; §5.4 governs consumed ranges only,
and hard-cut lines keep the pre-existing reader behavior of the carried
result). This changes no retained value and forks no G1 case.

### 5.2 §6 arithmetic and census verification

- **Token arity check:** G1's pinned literal key was decomposed this round:
  provider token 12 values against the 12 declared fields, metric token 10
  against 10, source token exactly `[address revision-tag VPT]`
  (`CONTRACT.md:823-852` vs `355-438`). No symbolic component survives.
- **Owner census verified in source (read-only greps this round):**
  `:ground-halo` (ground.cljs:364), `:ground-workshop` (:559),
  `:ground-workshop-specimen` (:560), `:ground-material-error` (:1029),
  `:ground-binding-lint` (:3529), and the provisional owner constructed
  EXACTLY as §6 writes it — `[:vi :ground-provisional (or thread-key
  "genesis")]` at ground.cljs:1554 — with the three provisional text
  children `:ground-activity`/`:ground-stream`/`:ground-turn-error`
  (:1590/:1596/:1609) as ground.cljs's only `:text-run` emitters.
  `block-fold-header-hit-prim` emits a `:hit-area` node and no text op
  (face_primitives.cljc:846-869), confirming its removal from the census.
  The "≈ machine guess" text op above it belongs to the silver-mark
  primitive and is covered by `:silver-mark`. The census's safety valve —
  any op outside it is a G1 totality failure forcing a recut
  (`CONTRACT.md:375-377`) — makes the census enforceable, not aspirational.
- **VPT equality traces:** binding-only revision → equal (excluded input);
  header-copy projection → unequal via `header-texts`; fold display →
  unequal via `body-text`; metadata change with identical visible values →
  equal. Each is both a §6 law and a G1 assertion
  (`CONTRACT.md:447-453,844-850`), and (j)/(k) prove the live twins.
- **`size = address count` soundness:** the key embeds the address, so two
  live addresses can never share one `key->result` entry; the diagnostic
  equality is a theorem of the construction, not a hope.

### 5.3 G4/G5 algebra traces

- **Conservation:** row (i): misses = A = layout-execs total delta ✓; rows
  (a)/(d)/(f): miss +1 = exec +1 ✓; rows (b)/(c)/(e)/(j)/(k)/G5: misses 0 =
  execs 0 ✓; row (l): production zero with oracle work in its own counter ✓.
- **Determinism:** each trial's fresh page gives a fresh cache, so binary
  toggles miss identically in both trials; row (g) deletes a per-trial
  fixture; RUN-RECORDED fields bind trial 1 → trial 2.
- **Origin-shift cascade:** global counters equal to the driving block's
  exact row-(d) deltas prove every origin-shifted block at zero — the
  per-block phrasing is assertable from the declared global counters plus
  the pre-read hooks (`CONTRACT.md:1053-1058`).
- **Row (h):** the "driving action's deltas PLUS destroyed +V / −owned"
  composition overrides row (d)'s zero-destroy expectation coherently, and
  the ALTERNATIVE convergence proof keeps the row receipt-bearing if no
  product road reaches the destroy site without truth death
  (`CONTRACT.md:978-992`).

### 5.4 Harness and terminal probes

Each probe classifies uniquely:

| Probe state | Classification | Why unique |
|---|---|---|
| Cold run: all green except G8 dirty conservation (R3's killer) | exit 3 → G8 red → row 2 PACKAGE FAIL | Row 2 names G8/G9 explicitly (`CONTRACT.md:1193-1196`). |
| Dedicated negative exits 3, red set `["oracle-mismatch"]`, count 1, rest green | exempt; not assertion red | Sole-exemption conditions exactly met (`CONTRACT.md:1180-1184`). |
| Dedicated negative exits 0 (seeded mismatch uncaught) | G4's pinned expectation fails → G4 red → row 2 | The exit-3 expectation is a G4 gate assertion (`CONTRACT.md:885-889`). |
| S2 and S4 both fired, G1 also red | row 1, one name: `PACKAGE BLOCKED — {S2,S4}` | Sorted-set law + precedence; simultaneous red recorded, not named (`CONTRACT.md:1171-1178,1186-1192`). |
| Environment-mismatched run with a red-looking bar | exit 1 decided FIRST → row 3 (if nothing else red) | Attestation precedes all assertion state (`CONTRACT.md:657-662,748-752`). |
| Wall bar red AND an assertion red in one run | exit 3 (order 1→3→2→0) → row 2 | "Wall-bar state cannot rescue the run" (`CONTRACT.md:663-667`). |
| Probe-mode assertion failure | exit 3 (probe never exits 2) | `CONTRACT.md:672-674`. |
| Settle at 20s (bar red, under the 120s deadline), rest green | exit 2 → row 4 | Bar-red ≠ assertion-red; timeout ≠ bar (`CONTRACT.md:668-676,1204-1212`). |

The seven-fixture truth table covers singleton stop, multi-stop+red, G8 red,
red+unclassified, unclassified-only, wall-only, and all-green, and any other
classifier output makes the positive probe itself exit 3
(`CONTRACT.md:1217-1227`).

### 5.5 Gate matrix

| Gate | R4 ruling | Basis |
|---|---|---|
| G1 | **PASS as gate shape** | Literal command; readable expected values throughout (header literal closed); reader rows; key literal; pure cache table; captured negatives compatible with exit 0. |
| G2 | **PASS as gate shape** | Literal cold invocation; exact target counters; run-level mapping stated. |
| G3 | **PASS as gate shape** | Literal hover invocation; THE storyboard referenced, not restated; 13/52ms/RAF-pair laws exact. |
| G4 | **PASS as gate shape** | Two literal invocations; trial protocol; four row laws; rows (a)–(l) with exact deltas; truth-table hook. |
| G5 | **PASS as gate shape** | Rides the positive invocation; exact pan/zoom scripts; genuine order-only row; honestly-named cascade row. |
| G6 | **PASS as gate shape** | JVM bound in glyph-index units with +8 and lines ≤ 2; oracle positive/negative; literal fence command. |
| G7 | **PASS as gate shape** | Bound to G4's commands/windows; exact structural bound; result routing stated. |
| G8 | **PASS as gate shape** | Literal cold invocation; settle definition; one timeout law; exact dirty conservation. |
| G9 | **PASS as gate shape** | Literal hover invocation; seven measurements; 52ms predicate. |
| G10 | **PASS as gate shape** | Six pinned commands/results; two-surface W1 predicate with fingerprint (source-re-verified); warning normalization; one golden-custody chain. |

## 6. `RECUT_LEDGER_R3.md` accuracy

The ledger's ten item mappings and its prior-ledger closure section match
the contract text and this round's rulings. Its two source-facing claims —
fold-header-hit emits no text; the overlay census covers the current
allowlisted emitters — were verified against the tree this round (§5.2).
No overclaim of the R1/R2-ledger class is present.

## 7. Advisories — non-blocking, with cheap falsifiers

None of these blocks PASS: none sits in a §5/§6 semantic clause, none makes
a gate unrunnable, none opens a terminal gap, and none permits two
implementations that differ in any gate outcome or retained value.

1. **G4 row-law 1's live proportionality clause is executable in only one
   of its two readings** (`CONTRACT.md:908-914`). "Layout rows bank and
   test it under G1's bound" — but G1's bound `4·(G+C+R)` needs G, C, R,
   which neither the §8 proportionality receipt (`CONTRACT.md:585-588`)
   nor any declared hook exposes. The only fully-declared behavior is:
   live rows bank the receipt; the bound is asserted where G+C+R are known
   (G1's fixtures, which cover both the monotonic and the sorted path).
   No false green is possible — the same `.cljc` layout path is bound by
   G1 and the renderer scans by G6's fence. Falsifier if it ever matters:
   one sentence in a future amendment either deferring the bound to G1
   explicitly or adding G/C/R fields to the receipt.
2. **`shape-calls` accounting for the reference-advance shape is derived,
   not stated** (`CONTRACT.md:780-784` with `318-331`). If the U+0020
   reference shape counted inside `shape-calls`, the per-line bound
   `≤ 1 + final-segment count` would fail on any multi-segment
   block-greedy MISS (the §5.3 worked example: 3 line shapes + 1 reference
   = 4 > 3), so only the exclusive accounting can go green — exactly one
   lawful implementation exists, reachable by deduction. A one-sentence
   statement ("reference-advance shapes are counted outside
   `shape-calls`") would spare the implementer the derivation.
3. **Rows (b)/(l)'s literal `+2` (hits, checks, `:hover-paint`, `:hover`)
   presumes the two pinned corpus blocks each own exactly ONE cached
   address** (`CONTRACT.md:939-941,1017-1022`). The founding capture's "2
   slots reshaped" evidences SLOTS; cache hits count per-op addresses. If
   either pinned block emits an auxiliary text op, the row reds against a
   correct implementation — a scheduled-false-red of the manifest-hint
   class, not an ambiguity. The falsifier is already inside the declared
   instrumentation: the trial protocol's census assertion plus the
   per-block owned-addresses hook (`CONTRACT.md:616-617,895-897`) can bank
   "owned-address count = 1 for each pinned block" as a corpus-identity
   receipt before any measured window; if that receipt ever reads > 1, the
   fix is a row recut with the pre-read form rows (g)/(i)/(j) already use.

Observations (no action): `occurrence` rides "final-display" logical-op
order (`CONTRACT.md:401-404`) — determinate, but implicit; G2's gate status
on an exit-3 run whose red set contains no G2 assertion is unassigned
(`CONTRACT.md:871-876`) — harmless, because row 2 already binds the package
terminal on whatever gate owns the red assertion.

## 8. Preserved ground — verified

- §5.3 current-segment definition, painted segment-leading whitespace,
  whole-run consumption, trailing-whitespace law — present and untouched.
- §5.7's five reference-advance fault rows and the no-wrap consequence.
- The reader-law scenario inventory, now over representable values.
- VPT visible-body/ordered-header/op-role intent with the
  inclusion/exclusion transition cases.
- Backend flip = layout reuse + paint repack + GPU reclone with the
  provider-token-equality precondition receipt (G4 row e).
- The genuine order-only G5 row, T13 route, I4 bounds and constants
  (4·(G+C+R), log-sort, 5×, +8), both experience bars (52ms/12.0s), and
  the §7 step-5 evidence gate.
- THE seven-transition storyboard with 13 measured repacks.
- G10's literal commands, two-surface W1 §9.1 predicate, full fingerprint,
  warning normalization, one S6 custody chain; W1's registered RED MSDF
  state unweakened.
- §12 unchanged: no SEAM widening, no existing-verifier edits, no store
  schema change, no foreign-work adoption; one-phase G1–G10 ownership with
  no Sid-owned blocking gate; evidence hashes untouched.

## 9. Scope and next step

Out of scope in this round: implementing anything, rerunning prior evidence
probes, running the pre-implementation gates, opening §7 step 5, editing
R1/R2/R3, touching foreign dirty-tree work, changing the allowlist, or
amending the contract.

Next, per §1's position and §14: SEAM-STEP1's implementation lands first;
the phase then opens on a clean worktree at the post-landing HEAD with the
§11 manifest re-sweep; the Codex opening prompt lands in the baton; gates
G1–G7+G10 run to green in phase with G8/G9 via the harness; the in-phase
falsifier aims at the linearized index construction + §5 wrap cuts + §6
key/cache; Fable runs the FULL-tier gate review. The §7 advisories travel
with the opening prompt as receipts for the implementer and the gate.
