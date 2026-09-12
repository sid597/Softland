# shaping-correction — RECUT LEDGER R3 (`VALIDATION_R3.md` §10 -> the R4 candidate)

Maps the immutable R3 FAIL's ten required recut items to the R4 candidate
`CONTRACT.md`. Consumption standard remains complete + coherent + executable;
letter-only inclusion is not consumption. R1/R2/R3 are untouched. Source was
reopened only to close the exact ground text-role census; no evidence probe,
live gate, implementation command, or source edit ran.

## R3 §10 items

1. **Make §5.5's exact values readable** -> §5.5 now uses the literal
   Clojure/EDN values `[:header h j]` and `[:header h [start end]]`; the
   nested two-vector is separately declared half-open. The header line value is
   `[:header h [0 len]]`; G1 compares that exact readable value.

2. **Close the key in executable substance** -> §6 defines the complete key
   value `[source-token provider-token metric-token]`; VPT retains full visible
   body text (no hash intermediary), ordered header strings, and one role from
   a closed source-checked domain with an exact first-match assignment table.
   Stable address is exactly `[:ground-text subject-id op-role occurrence]`,
   with `subject-id` restricted to the enumerated owning scene-slot VIs;
   revision tags are `[:stamped stamp]` / `[:unstamped]`; effective provider,
   metric/default, layout-version, and index-space values occupy ordered
   vectors. The source check removes fold-header-hit (it emits no text) and
   includes current allowlisted ground overlays. Source lines and non-key
   consumer inputs are fixed/derived. Reference advance is derived after the
   key is known: one shape on positive block-greedy production MISS, zero on
   HIT/unbounded.

3. **Make the cache genuinely bounded** -> §6 owns `address->key` plus
   `key->result`, with zero-or-one current result per stable source address.
   Key changes atomically replace the old entry (size delta 0); superseded
   revisions never linger. A surviving-slot rebuild reconciles and removes
   vanished auxiliary addresses; truth/slot death removes every owned address
   idempotently. Cache size is bounded by live addresses and equals diagnostic
   address count. G1 has an exact pure lifecycle table (including 1,000 edits);
   G4 key-changing rows use replacement +1/size 0; provider change uses
   replacement A/size 0; death/slot eviction use exact owned-address counts.

4. **Separate oracle work from production work exactly** -> §8 adds
   `oracle-layout-execs`; production `layout-execs` remains miss-only;
   `oracle-layout-execs = oracle-checks` and oracle calls bypass production
   counters/cache. JVM seeded negatives are captured predicate rejections:
   successful rejection makes the test pass; no expected negative is an
   uncaught failing test assertion.

5. **Repair G4 row algebra and repetition** -> provider-change packs are
   counted under `:provider-change` once per written live slot. Every positive
   G4/G5 row has two isolated fresh-context trials with byte-equal pre-state,
   product/truth-road setup and restore outside the measured window, and exact
   replacement/bound/conservation laws. Row (g) creates its fixture afresh;
   row (l) positive runs twice; its dedicated negative is explicitly a one-
   trial mutation-rejection proof. Backend dirty `:other` is RUN-RECORDED >=1.

6. **Give the live oracle negative its own literal invocation** -> §9 ships
   four exact commands. Positive probe is
   `--mode=probe --case=positive`; negative is
   `--mode=probe --case=oracle-negative`. Case is required only for probe,
   has exactly those values, and cannot be selected by environment or run
   count. G4 pins positive exit 0 and exact negative exit 3/red-set/mismatch
   exemption.

7. **Choose one settle-timeout classification** -> navigation/readiness failure
   before environment attestation is exit 1/unclassified. Any required
   readiness or cold-settle timeout after valid attestation is product
   assertion red/exit 3; the 120s cold timeout makes G8 red and PACKAGE FAIL.
   No clause calls it unclassified.

8. **Make terminals total and disjoint** -> §10 computes one sorted unresolved
   stop set `B` and renders one canonical blocked name, including simultaneous
   stops. PACKAGE FAIL covers non-exempt assertion red in any G1-G10 gate,
   explicitly including G8/G9. Exit 2 remains wall-only bar red. The positive
   probe runs a seven-fixture pure terminal classifier table covering singleton
   and multiple stops, G8 red, red+unclassified, unclassified-only, wall-only,
   and all-green.

9. **Preserve accepted ground** -> §12 is unchanged. The R3-consumed storyboard,
   G10 two-surface W1 predicate/fingerprint/golden chain, backend lifecycle,
   T13, I4, bars, step-5 evidence gate, SEAM freeze, and existing-verifier
   custody remain unweakened.

10. **Run a wholly fresh default-fail R4** -> front matter and §14 order R4
    over this candidate, landing only as `VALIDATION_R4.md`. R1/R2/R3 remain
    immutable; implementation does not open from this candidate.

## Prior-ledger closure

- **R2 §10 item 1 / R1 items 1-2:** readable header values + existing §5.3,
  fault, reader, and semantic laws.
- **R2 item 2 / R1 item 3:** full-value key, exact address/role/provider/metric
  schema, bounded current-entry cache, separate oracle accounting.
- **R2 item 3 / R1 items 3-4:** corrected live matrix, provider pack receipt,
  deterministic isolated trials, exact owned-address eviction, backend law
  preserved.
- **R2 item 5 / R1 items 4, 6:** four literal invocations, exact selector law,
  one timeout classification, complete setup/restore/window rules.
- **R2 item 7 / R1 item 8:** sorted stop set, all-gate assertion-red coverage,
  bar-only row, executable seven-fixture table.
- R2 items 4, 6, 8, 9 and R1 items 5, 7 were already consumed by R3 and are
  preserved rather than reopened.
