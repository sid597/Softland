# shaping-correction — RECUT LEDGER R2 (VALIDATION_R2 §10 → the R3 candidate)

Maps `VALIDATION_R2.md` (immutable FAIL) §10 items 1–9 to where the R3
candidate `CONTRACT.md` consumes each, and closes the R1 §10 items 1–8
that R2 ruled NOT CONSUMED in executable substance. `RECUT_LEDGER_R1.md`
is historical. Machine-extracted facts new in this recut: W1 §9.1's full
environment fingerprint, `run_verifier.mjs`'s emitted close fields
(:491–510) vs receipt-file fields (:464–484) and classification cascade
(:435–448), `server_jetty.clj:2500`'s port-8080 default — all read from
disk 2026-08-04.

## R2 §10 items

1. **Close §5 completely** → §5.3 rewritten SEGMENT-RELATIVE with the
   current-segment definition; rule 1 makes segment-leading whitespace
   painted and never a candidate (covers indentation AND hard-cut
   remainders in one law); rule 2 requires a preceding painted cluster in
   THIS segment, so an empty painted prefix is unchoosable. §5.5 replaces
   the exemplary tag with the exact per-header schema (`[:header h j]`
   offsets, `[:header h [start end)]` ranges, the three consequences).
   §5.7 gains the total reference-advance fault table (five fault
   classes → one `provider-fault` increment + the no-wrap consequence).
   G1 gains the reader-law rows (hit-test past painted end ×2, consumed
   caret stops, selection inside / ending at consumed-end, copy ×2) and
   the continuation-segment-leading-whitespace case.
2. **Make the §6 token and oracle exact** → §6 defines the VPT
   `[body-hash header-texts op-role]` once, used verbatim in the key fn
   and in G1's expected values (stamped = `[stamp VPT]`, unstamped =
   `[VPT stable-source-address]`); §6's cache-correctness oracle is
   gate-owned — G6's JVM oracle half (positive deep-equal + seeded stale
   negative) and G4 row l live (negative = its own invocation, expected
   exit 3).
3. **Complete the invalidation matrix** → G4 rows: (i) same-backend
   provider change (UNEQUAL provider-identity precondition, pre-read A =
   live owned-key count, `:provider-change` execs = A, digest UNEQUAL,
   exact paint/geo per the §8 pack-cause law); (j) live binding-only
   reuse (all-zero, digest EQUAL); (k) provenance/attention paint-only
   (layout 0, run-recorded paint); (g) truth-death eviction with
   pre-read owned-keys O — size −O, never −1; (h) vanished-slot eviction
   exercised separately (or the R1-sanctioned convergence proof,
   receipt-bearing); G5 gains the genuine order-only sibling swap
   (all-zero) with the fold probe honestly renamed origin-shift cascade.
   Backend row (e) kept unchanged.
4. **Repair THE storyboard** → §9's ordered five-act list: unmeasured
   largest warm-up, unmeasured positioning move to ordinary, RESET after
   ordinary confirmed current, (ordinary→largest, largest→ordinary) ×3,
   then ordinary→empty as the lawful seventh (act 4 ends on ordinary).
   The two pre-reset pairs are named and appear in no delta; measured
   repacks = 13 preserved; G3/G9 reference the list and restate nothing.
5. **Make the harness executable and total** → §9 ships three LITERAL
   invocations (`--mode=cold|hover|probe --url=http://localhost:8080`,
   URL pinned to the Jetty dev default, harness-owned readiness); exit
   law total + disjoint with the new exit 3 (product assertion red) and
   ordered fault-first classification; G7 bound to G4's command, per-
   window `store-frame-execs ≤ raf-frames`, expected result stated; §8
   declares `slot-text-writes` and the seven-field `dirty` permanently;
   G4's three row laws (UNNAMED = 0 · miss = execs conservation · run-
   twice determinism) close every field on every row; the §8 hook list
   is the complete action enumeration — no private harness mutation.
6. **Finish G10's exact command/state predicate** → item 2 is the full
   literal nine-namespace runner command; item 5 pins BOTH verifier
   surfaces (console close fields as emitted incl. the full
   `environmentFingerprint`
   e79490f8882cd785f32b5bb82cadd425dc90f2d7616cc9f0debf8a0f1c476282,
   asserted directly because the cascade tests parity before
   environment; receipt file's goldenComparison 21/21 + per-regime
   counts); item 6 defines warning-signature normalization (path · type ·
   integer-stripped message; multiset comparison); all W1 counts
   retained.
7. **Make terminal classification total** → §10's table is now ordered
   rows 1–5 with declared precedence BLOCKED > FAIL > UNCLASSIFIED >
   BAR-RED > PASS; PACKAGE BLOCKED — S<n> family added (neither red nor
   green nor bar-red nor unclassified); S6 blocks G10 BEFORE any
   fail/pass (single custody chain in G10, mirrored in §13); exit 3 →
   FAIL, exit 1 → run-level unclassified with the package-vs-run scope
   stated; the red-gate-plus-unclassified-run overlap resolves to FAIL
   by row order.
8. **Preserve accepted ground** → §12 untouched; SEAM files + existing
   verifiers untouched; Contract T, I4, both bars, the §7 step-5
   evidence gate, T13, W1's RED MSDF state, G4 row (e), repacks = 13,
   the §5.3 worked example, and W1's registered counts all preserved
   verbatim or strengthened only as R2 ordered.
9. **Run a wholly fresh default-fail R3** → front matter + §14 step 1:
   R3 over this candidate; R1/R2 immutable; no implementation opens from
   this candidate.

## R1 §10 items 1–8, now closed in executable substance

- **1, 2** (§5 one-behavior + executable semantic table) → R2 items 1
  and 5 above: the leading-space expected value is uniquely derivable
  from §5.3 rule 1; hit-test/copy are G1 rows, not prose.
- **3** (total key, exact projections, live deltas, eviction) → R2
  items 2 and 3.
- **4** (backend law executable) → R2 item 5's harness work (literal
  probe invocation, exit 3, §8-vocabulary-only field names — `clone`
  no longer appears; `slot-text-writes` declared; every row field
  closed by the UNNAMED = 0 law). The backend law itself unchanged.
- **5** (one possible storyboard) → R2 item 4.
- **6** (commands/actions/windows/exits) → R2 item 5; `dirty` coverage
  assigned to G4/G5/G8 without a new gate number.
- **7** (G10 exact state, one golden policy) → R2 item 6 + the single
  custody chain.
- **8** (terminal totality) → R2 item 7.
- **9, 10** were already CONSUMED per R2 §1 and are untouched.
