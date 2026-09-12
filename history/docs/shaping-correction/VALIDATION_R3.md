# shaping-correction contract — validation round 3 (fresh context, default-fail) — VERDICT: FAIL

Run 2026-08-04 over the R3 candidate at
`b314a38769ce10e24f8727cbf4f4bb359c7b601a` (`b314a38`) on
`docs/current-mental-model-local`. Mode = GATE; altitude = contract;
authority = candidate. The target package path was clean before this file was
created; the working tree contained 27 pre-existing foreign entries outside
this write. None was edited, staged, adopted, or treated as shaping-correction
work.

`VALIDATION_R1.md` and `VALIDATION_R2.md` were treated as immutable. Their
SHA-256 values remained, respectively,
`5fd6ef792505d8fc74635821d60cdd4b3c423608a1f32f9554e3ff83b7bf1172`
and
`05e584bfb864c987f153b0d72017f2ed6d28196f27877879188590c08ce10eb3`.
Their evidence probes were not rerun. No founding profile, live browser gate,
test suite, implementation probe, or implementation command was run. This
round is a static executability, semantic-totality, consumption, and terminal
classification gate over the committed candidate and the already-banked
records.

This file is the only write made by the R3 validation round.

---

## 1. Verdict

**VERDICT: FAIL.** The candidate is not yet an executable implementation
contract. Five independent FAIL classes remain:

1. §5.5's declared **exact** header-range representation is not a readable
   Clojure/EDN value, while G1 requires that representation literally.
2. §6 does not define one total key/cache/oracle behavior: key components remain
   symbolic, the stated cache bound contradicts the required retention of every
   superseded key, and oracle hits contradict the global execution/miss
   conservation law.
3. G4's supposedly exact matrix contradicts §8's pack-cause law and its own
   run-twice law; several rows cannot produce the same deltas twice from the
   state the contract says they leave behind.
4. The live harness has only one legal `probe` invocation but requires that same
   invocation to be both the positive exit-0 run and a separate negative exit-3
   run; cold settle timeout also has two incompatible classifications.
5. The package terminal table is not total: a G8 non-wall assertion red has no
   matching package terminal, and simultaneous stops have no single canonical
   blocked name.

These are contract defects, not implementation risks. A conforming implementer
cannot choose one behavior without supplying policy absent from, or contrary
to, the candidate. The implementation phase does not open.

## 2. `VALIDATION_R2.md` §10 consumption rulings

Consumption means complete, coherent, and executable. Presence of the requested
nouns or rows is not consumption.

| R2 §10 item | R3 consumption | Ruling |
|---:|---|---|
| 1. Close §5 completely | **NOT CONSUMED** | The segment-relative cut law, leading-whitespace law, fault rows, and reader rows are materially repaired. But the exact header range `[:header h [start end)]` is not a readable Clojure/EDN datum, and G1 repeats it as a literal expected value (`CONTRACT.md:273-288,678-679`). Finding 1. |
| 2. Make the §6 token and oracle exact | **NOT CONSUMED** | `[body-hash header-texts op-role]` still leaves the hash/collision law, `stable-source-address`, and the role domain unspecified. The cache oracle also performs a batch layout on every hit while §8 says every hit executes zero layouts (`CONTRACT.md:345-370,419-430,478-490,509-514`). Finding 2. |
| 3. Complete the invalidation matrix | **NOT CONSUMED** | The missing provider, binding-only, provenance, truth-death, vanished-slot, and genuine order-only rows now exist. They are not jointly executable: provider packs are both counted and zero; binary projection flips cannot satisfy the miss/size/run-twice laws while superseded keys linger; the delete row has no repeat setup; row (l) says once under an every-row-twice law (`CONTRACT.md:741-821`). Finding 3. |
| 4. Repair THE storyboard | **CONSUMED** | The unmeasured largest warm-up, unmeasured move to ordinary, post-confirmation reset, three ordinary/large round trips, and ordinary-to-empty control form one physically coherent seven-transition trace with 13 measured repacks (`CONTRACT.md:564-583`). |
| 5. Make the harness executable and total | **NOT CONSUMED** | The three main mode commands and 0/1/2/3 table are present, but no literal command or selector distinguishes G4's required positive and negative probe invocations; `--mode=` is expressly the only selector. Timeout is exit 1 in §9 and a gate failure in §10 (`CONTRACT.md:535-563,626-629,730-732,815-820`). Finding 4. |
| 6. Finish G10's exact command/state predicate | **CONSUMED** | The nine-namespace command is literal; both verifier output surfaces are distinguished; the console predicate includes the W1 §9.1 fingerprint; receipt-file counts retain the registered W1 state; warning identity is defined (`CONTRACT.md:878-924`). Static comparison against `W1.md` §9.1 and `run_verifier.mjs:435-510` supports the pin. |
| 7. Make terminal classification total | **NOT CONSUMED** | Ordered precedence and a blocked family were added, and S6 now blocks before fail/pass. But a dirty-conservation assertion red makes G8 red at harness exit 3 and matches none of rows 1-5 because PACKAGE FAIL excludes G8/G9 (`CONTRACT.md:866-872,940-976`). Multiple simultaneously fired stops also instantiate more than one `S<n>` without a collision rule. Finding 5. |
| 8. Preserve accepted ground | **CONSUMED** | The one-phase shape, §12 custody/allowlist, frozen SEAM files, existing-verifier prohibition, Contract-T authority, I4, both bars, step-5 evidence gate, T13 route, backend repack+reclone law, W1 RED MSDF state, and 13-repack storyboard remain present and unweakened. |
| 9. Run a wholly fresh default-fail R3 | **CONSUMED** | This is the new R3 round over `b314a38`; R1/R2 remain byte-identical and no implementation or prior evidence probe was run. |

`RECUT_LEDGER_R2.md` therefore overclaims consumption at lines 3-5 and 87-103.
Its item mappings are useful navigation, but items 1, 2, 3, 5, and 7 are not
closed in executable substance.

## 3. `VALIDATION_R1.md` §10 items 1-8 consumption rulings

| R1 §10 item | R3 consumption | Ruling |
|---:|---|---|
| 1. Exact §5 representation and reader behavior | **NOT CONSUMED** | Body cut/consumed semantics are substantially closed, but the header range is not a representable literal. The claimed exact result schema therefore still has no executable value for every required line. Finding 1. |
| 2. JVM semantic table, bound linearity, seeded negatives | **NOT CONSUMED** | The cases, constants, and reader rows are now enumerated, but the two-header case requires an unreadable literal. The seeded oracle/full-scan wording also says an assertion fails inside the same namespace whose one command must exit 0, without defining a captured negative/meta-test result (`CONTRACT.md:648-715,839-855`). Findings 1-2. |
| 3. Total §6 key, exact projections, live deltas, eviction | **NOT CONSUMED** | The new rows cover the requested transitions in letter, but key equality is not total and the cache cannot be both live-set-bounded and retain unbounded superseded revisions. The live rows are internally inconsistent. Findings 2-3. |
| 4. Backend repack + reclone + layout reuse; capacity-controlled edit | **CONSUMED IN LAW, NOT EXECUTABLE** | Row (e) preserves the correct three-lifecycle backend law and rows (a)/(f) separate in-place from growth. The shared G4 command and row laws remain non-executable, so the item cannot pass under the consumption standard. Findings 3-4. |
| 5. One hover storyboard | **CONSUMED** | §7, §9, G3, and G9 now resolve to the one coherent seven-transition definition. |
| 6. Commands, actions, windows, environments, constants | **NOT CONSUMED** | Literal mode commands and most windows now exist, but the positive/negative probe runs have no distinct exact invocation, row state restoration is missing, and timeout has two results. Findings 3-4. |
| 7. Exact G10 nonzero state and one golden policy | **CONSUMED** | The exact expected nonzero verifier state is pinned on both surfaces, including the environment fingerprint, and one golden diff now enters only the S6 blocked chain before adjudication. |
| 8. Experience-bar-red terminal and cross-run matching | **NOT CONSUMED** | The bar-red row and environment-match policy exist, but the encompassing terminal claim is still not total for G8 assertion red and not disjoint for multiple simultaneous stops. Finding 5. |

R1 items 9-10 are outside the requested re-audit; R2 had already ruled them
consumed, and this candidate does not disturb their T13/§12 or fresh-round
ground.

## 4. FAIL-class finding 1 — §5.5's exact header range is not an executable value

### Claim in the artifact

The contract says the header schema is exact, not exemplary:

> A header range is `[:header h [start end)]` ...
>
> a header line's `:source-range` = `[:header h [0 len)]`

and G1 must compare the full tagged values literally
(`CONTRACT.md:273-288,678-679`).

### Concern

`[:header h [start end)]` is interval notation embedded inside what is declared
to be a Clojure vector. It has an opening `[` closed by `)`, so it is not a
readable Clojure/EDN datum. The same is true of `[:header h [0 len)]`. The new
G1 namespace is a `.clj` file and cannot contain either value literally.

Treating the parenthesis as prose rather than data does not save the contract:
the text explicitly says **this schema**, **exact**, and **literally**. An
implementer must invent a representable range form, and different inventions
produce different retained values and reader APIs.

### Smallest correction

Choose one readable representation and state half-open semantics separately.
For example, use `[:header h [start end]]` and say that the nested pair denotes
`start` inclusive / `end` exclusive, or use a flat tagged form such as
`[:header-range h start end]`. Replace every §5/G1 occurrence and require the
test to compare that actual value. Preserve the per-header domain, no cross-
header range, no body alias, unwrapped header, and full caret laws.

## 5. FAIL-class finding 2 — §6's key, bound, and oracle cannot all hold

### 5.1 The supposedly exact key still has unbound identity components

The VPT is written as `[body-hash header-texts op-role]`, but `body-hash` is
defined only as a "value hash" and no function, width, collision rule, or
full-string equality guard is named (`CONTRACT.md:345-356`). A hash-only key can
alias unequal visible body strings. The dev oracle runs only in oracle windows,
so it is not a production collision policy.

For every unstamped source, the source token contains
`stable-source-address`, but that address has no exact representation, scope,
or constructor anywhere in the contract (`CONTRACT.md:361-370`). `op-role` is
likewise only "the op-role keyword" rather than a closed mapping for the total
op census. Implementations can therefore choose materially different sharing,
ownership, and eviction identities while satisfying the written vector shape.

The reference advance is also said to be shaped exactly once per layout key and
to be part of that key's metric regime (`CONTRACT.md:305-312,381-383`). The
contract does not say what pre-key identifies and reuses the reference shape,
or whether every cache lookup reshapes U+0020 before the final key can be known.
That leaves more than one lawful execution count.

### 5.2 The cache is declared bounded and required to grow without a bound

§6 says the cache is bounded by the live block set and that there is no
unbounded memo (`CONTRACT.md:406-417`). G4 then requires every text edit,
projection flip, capacity edit, and provider change to increase cache size while
the superseded keys linger until block death
(`CONTRACT.md:745-750,759-764,771-777,796-806`).

One live block can undergo arbitrarily many edits without dying. Under the G4
law, each edit leaves another cache entry. Cache size is therefore not bounded
by the live block set or by any stated per-owner maximum. Death eviction makes
the entries finite-lived only if the block eventually dies; it does not provide
a bound for a long-lived block.

### 5.3 Oracle execution contradicts the counter conservation law

While the oracle is enabled, **every cache hit** recomputes through the batch
`text-layout/layout` entry (`CONTRACT.md:419-430`). §8 defines
`layout-execs` as layout executions, then binds every G4/G5 window to
`miss delta = layout-execs total delta`, explicitly saying every hit executes
zero (`CONTRACT.md:478-490,509-514`). G4 row (l) requires at least one oracle
check on a hit (`CONTRACT.md:815-820`).

That hit both does and does not execute layout. No separate oracle-execution
counter or explicit exclusion exists. If the batch recompute increments
`layout-execs`, conservation fails because there is no miss. If it does not,
the supposedly total execution counter omits an actual layout execution.

The JVM negative has a second executable-form gap: G1/G6's one namespace
command must exit 0, but the seeded stale/full-scan variants are described as
making "the assertion fail" (`CONTRACT.md:648-650,713-715,839-855`). The
contract does not define a predicate/mutant harness whose rejection is itself a
passing test, an expected exception, or a separate expected-nonzero command.

### Smallest correction

- Put the full visible body string in the VPT, or pin a digest algorithm plus
  full-value collision equality; enumerate the exact `op-role` mapping and the
  exact `stable-source-address` tuple/domain.
- Define the pre-key/reference-advance acquisition order and its exact execution
  count on cache hits and misses.
- Give the cache a real bound: replace superseded entries per owner/role, or
  state a numeric/structural per-owner policy with eviction. Rewrite G4 size
  expectations to that policy.
- Either count oracle recomputations separately (for example,
  `oracle-layout-execs`) and explicitly exclude them from production
  `layout-execs`, or include them and change conservation accordingly.
- Express seeded negatives as captured meta-tests with a precise pass predicate,
  or give them separate literal commands and expected nonzero exits.

## 6. FAIL-class finding 3 — G4's exact matrix is internally contradictory

### 6.1 Provider packs are both counted and zero

The permanent `paint-repacks` counter counts every instance-data pack. A pack
following layout execution carries the execution cause and is counted; the text
explicitly says provider-change packs ride that road
(`CONTRACT.md:484-490`). G4 row (i) requires `A` provider-change executions,
text-geometry writes for all live slots, and then requires
`paint-repacks 0`, while citing that same pack-cause law
(`CONTRACT.md:796-806`).

"Exec road" selects the cause bucket; it does not remove the pack from the
counter. The row must either count the provider-change packs or redefine
`paint-repacks` to exclude packs after layout execution. It cannot do both.

### 6.2 The run-twice law has no state-reset semantics and contradicts rows (d), (g), and (l)

Every G4 row must run twice with a counter reset between and produce identical
deltas (`CONTRACT.md:741-743`). Counter reset does not restore product, cache,
provider, slot, or truth state, and the complete hook list contains no fixture
reconstruction or cache/product reset (`CONTRACT.md:516-528`).

The binary fold/paste row makes the contradiction concrete. Its first toggle
must miss, execute layout, and grow the cache by one, while the superseded key
lingers (`CONTRACT.md:759-764`). Toggling back on the second run returns to the
old key that the contract required the cache to retain. That run must hit rather
than miss and cannot grow size by one, so its deltas cannot equal the first run.
The delete row cannot delete the same truth twice without an undeclared fixture
recreation (`CONTRACT.md:778-782`). Row (l) directly says to replay row (b)
**once**, despite the every-row-twice law (`CONTRACT.md:815-821`).

Row (e) additionally names dirty `:other` only as "recorded", not as an exact
delta or a RUN-RECORDED value (`CONTRACT.md:765-770`), despite the claim that
the row deltas are exact and the rule that named fields are asserted.

### Smallest correction

Define a per-row fixture/setup/restore protocol outside each measured window,
including exact cache and product state before both repetitions. Add any
required setup hooks to the declared complete hook list, or make one-shot rows
explicitly one-shot with another falsification rule. Resolve row (l)'s once vs
twice cardinality. Then make provider-change pack counts agree with §8 and give
dirty `:other` an exact or RUN-RECORDED predicate.

## 7. FAIL-class finding 4 — the harness cannot select its required runs and classifies timeout twice

### 7.1 One exact `probe` command has two incompatible expected exits

§9 says the harness ships exactly three invocation forms and `--mode=` is the
only mode selector. There is exactly one legal probe form
(`CONTRACT.md:535-543`). G4 requires that probe invocation to exit 0 **plus one
separate negative invocation** for row (l) that exits 3
(`CONTRACT.md:730-732,815-820`).

No `--case=oracle-negative`, environment selector, external pre-seed command,
or other exact state transition distinguishes those two processes. An identical
literal command against the same declared mode has no contract-defined reason
to choose the positive suite once and the seeded negative another time. Running
the negative inside the ordinary probe would make the ordinary process exit 3,
contradicting its expected exit 0; treating it as an expected internal negative
would contradict the required separate process exit 3.

### 7.2 Settle timeout is both unclassified and FAIL

The ordered exit law classifies a settle timeout as harness/environment exit 1,
meaning the gate did not run and its receipt is informational
(`CONTRACT.md:549-555`). The environment law says the same 120s timeout makes
the gate FAIL (`CONTRACT.md:626-629`). The terminal table maps exit 1 to
environment-unclassified and exit 3 to assertion red
(`CONTRACT.md:940-965`).

The same timeout therefore admits two package paths. Fault-first ordering does
not resolve it because both clauses explicitly own the same event.

### Smallest correction

Add a literal, pre-bound negative selector and command, such as a dedicated
`--case=oracle-negative` form, or split the negative into a separate committed
harness. State how its environment/readiness is established and why its
expected exit 3 is exempt from package failure. Then choose exactly one timeout
law: exit 1/unclassified if the gate never ran, or exit 3/PACKAGE FAIL if the
timeout is a product assertion. Use that result in §9, the environment law, G8,
and the terminal table.

## 8. FAIL-class finding 5 — the terminal table still has a gap and a blocked-name overlap

### 8.1 A G8 assertion-red state matches no terminal row

The harness exit law permits a product counter/receipt assertion red in any
mode, including cold, to exit 3 (`CONTRACT.md:549-557`). G8's cold gate contains
non-wall assertions: adapter/receipt requirements and exact dirty conservation
(`CONTRACT.md:866-872`). Consider the lawful end state:

- no stop is pending;
- G1-G7 and G10 are green;
- the cold environment matches and the 12s wall bar is green;
- G8 dirty conservation is wrong, so the cold run exits 3 and G8 is red.

Terminal row 1 does not match (no stop). Row 2 does not match because it names
only G1-G7 or G10 red, excluding G8/G9 (`CONTRACT.md:947-958`). Row 3 does not
match because a gate is red and this is not exit 1. Row 4 requires a wall-only
exit 2 and G1-G7+G10 green; the run is assertion-red exit 3. Row 5 requires all
gates green. The state has no package terminal.

G2 cannot absorb this failure merely because it shares the cold invocation:
G2 becomes red only when a **G2 assertion** is in the red set
(`CONTRACT.md:716-721`); dirty conservation belongs to G8.

### 8.2 Simultaneous stops do not have one blocked name

Row 1 is the family `PACKAGE BLOCKED — S<n>` and §13 says every fired stop names
that terminal (`CONTRACT.md:947-952,1060-1081`). The contract does not make
S1-S6 mutually exclusive. For example, an allowlist failure (S2) and a binding-
contract conflict (S4) can both be discovered before a ruling lands. That state
instantiates both `PACKAGE BLOCKED — S2` and `PACKAGE BLOCKED — S4`, contrary to
the claim that exactly one name fits.

### Smallest correction

Make PACKAGE FAIL include every non-exempt assertion-red gate across G1-G10,
while keeping an environment-matched wall-only exit 2 on the bar-red row. Define
the deliberate oracle-negative exit 3 as an expected sub-run result that does
not make any gate red. For stops, either define a priority among S1-S6 or use
one canonical blocked value carrying a sorted set of fired stop identifiers,
for example `PACKAGE BLOCKED — {S2,S4}`. Assert the decision table with a small
truth-table test covering red+unclassified, G8 assertion red, wall-only red,
golden block, and multiple-stop cases.

## 9. Gate matrix and preserved ground

| Gate | R3 ruling | Exact reason |
|---|---|---|
| G1 | **FAIL** | The header literal cannot be read; negative/meta-test semantics are not defined. |
| G2 | **FAIL as shared live form** | The cold command exists, but its settle-timeout result is contradictory. |
| G3 | **PASS as gate shape** | The seven-transition storyboard, reset point, 13 repacks, RAF pairs, wall bar, and literal hover command now agree. |
| G4 | **FAIL** | One command cannot select positive vs negative runs; pack counts and repetition laws contradict. |
| G5 | **FAIL as shared live form** | Its rows ride the unresolved positive probe invocation and inherit G4's global row laws. The genuine order-only action itself is preserved. |
| G6 | **FAIL** | Span bounds/fence are concrete, but oracle execution accounting and seeded-negative process/test semantics are not. |
| G7 | **FAIL as shared live form** | Its bound and window are now exact, but it rides G4's non-executable probe run. |
| G8 | **FAIL** | Timeout has two classifications, and an assertion-red G8 run has no package terminal. |
| G9 | **PASS as gate shape** | The literal hover command, seven measurements, and 52ms predicate agree; package closure still depends on the repaired shared terminal law. |
| G10 | **PASS as gate shape** | Commands, W1 nonzero state, two output surfaces, fingerprint, warning identity, and single golden-custody chain are exact. |

Preserve in the recut:

- §5.3's current-segment definition, painted segment-leading whitespace,
  whole-run consumption, trailing-whitespace law, and 13-repack storyboard;
- §5.7's five explicit reference-advance fault rows and no-wrap consequence;
- the exact reader-law scenario inventory, after the header value is made
  representable;
- VPT's visible-body / ordered-header / op-role intent and the inclusion/
  exclusion transition cases;
- backend flip = layout reuse + paint repack + GPU reclone;
- the genuine order-only row, T13 route, I4 bounds, experience bars, and §7
  step-5 evidence gate;
- G10's literal commands, two-surface W1 predicate, full fingerprint, warning
  normalization, and one S6 custody chain;
- §12 unchanged: no SEAM widening, no existing verifier edits, no store-schema
  change, no foreign-work adoption.

Out of scope in this round: implementing any correction, rerunning prior
evidence probes, running the pre-implementation gates, opening §7 step 5,
editing R1/R2, touching foreign dirty-tree work, or changing the allowlist.

## 10. Required recut ledger — R3 FAIL -> R4 candidate

1. **Make §5.5's exact values readable.** Pick one valid Clojure/EDN header
   offset/range representation; state half-open semantics outside the datum;
   replace every §5/G1 literal and keep the per-header/no-alias laws.
2. **Close the key in executable substance.** Pin the body identity function and
   collision behavior (or retain the full visible body value), enumerate
   `op-role`, define `stable-source-address` exactly, and state the pre-key/
   reference-advance construction and reuse order.
3. **Make the cache genuinely bounded.** Define replacement or a finite
   per-owner/global eviction rule for superseded live-block keys. Rewrite G4
   size, hit, miss, provider-change, and death/slot-eviction expectations to the
   chosen bound. Do not call revision accumulation T9-bounded.
4. **Separate oracle work from production work exactly.** Add an oracle-layout
   execution counter or explicitly redefine `layout-execs`; make the miss/hit
   conservation equation true in oracle windows; express JVM seeded negatives
   as captured meta-tests or separate expected-nonzero commands.
5. **Repair G4's row algebra and repetition protocol.** Reconcile provider packs
   with §8; define complete pre-state/restore behavior for both repetitions;
   resolve binary toggles, deletion, provider flips, capacity edits, and row
   (l)'s once/twice conflict; bind dirty `:other` when named.
6. **Give the live oracle negative its own literal invocation.** Add a declared
   case selector or dedicated harness command with exact setup, readiness,
   expected exit 3, and package-failure exemption. Keep the ordinary probe run
   deterministically exit 0 when all positive assertions are green.
7. **Choose one settle-timeout classification.** Apply it identically in §9,
   the environment law, G8, and the package terminal table.
8. **Make terminals total and disjoint.** Route any non-exempt assertion red in
   G1-G10, including G8/G9, to PACKAGE FAIL; reserve the experience row for
   environment-matched wall-only exit 2; define one canonical result for
   simultaneous stops; add an executable terminal truth table.
9. **Preserve accepted ground.** Do not widen §12, weaken Contract T/I4/bars,
   edit SEAM or existing verifiers, pre-authorize step-5 mechanisms, change W1's
   registered RED state, or reopen the corrected storyboard/backend law.
10. **Run a wholly fresh default-fail R4.** Keep R1/R2/R3 immutable. Do not open
    implementation from the recut candidate until every item above is consumed
    completely, coherently, and executably.
