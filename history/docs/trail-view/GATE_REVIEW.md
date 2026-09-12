# WP1 Gate Review — trail-view data layer (Track A)

Reviewer: Fable, 2026-07-05 (the orchestrating session; amended-process run).
Judged: the code, read in full — `src/app/server/rama/trail_view.clj` (726
lines) + `test/app/server/rama/trail_view_test.clj` + the Phase-A kernel diff
(A1/A2/A3, previously green). Prior-pass records used as input, NOT authority:
`DIFF_FALSIFICATION_R1.md` (fresh-context Opus, zero blockers),
PLAN/PLAN_VALIDATION_R1, the baton trail. Binding docs: CONTRACT v1.1,
decisions.md.

## Verdict: PASS

Suite run independently in THIS session, twice, one JVM per run
(relation-kernel-test + trail-view-test):

- Pre-review: **3 tests / 296 assertions / 0 failures** (222 Phase A + 74
  Phase B).
- **Gate 10's trail-view half had NO executed assertion** (bundle/text
  custody projection; Phase A proved rows only). Closed at gate by a
  reviewer-authored TEST-ONLY addition (6 assertions: divergent `:written-by`
  on the bundle edge, "via <writer>" text badge, convergent absence of both).
- Post-addition: **3 tests / 302 assertions / 0 failures** — delta +6 exact,
  nothing vacuous. Gates 1–16 green in one suite run. Definition of done met.

## Ruling on the parked question: client-composition vs single-roundtrip

The module takes CONTRACT §7's pre-named fallback (material via the module's
mirror topologies — the spike-proven `|hash$$` mechanics; R1/R2/R3 + OC
source-ref queries composed client-side). Ruled **contract-sanctioned**:
§7 insulates consumers ("wrapper signature and result shape DO NOT change…
latency is not a gate; shape and honesty are"), and gates 1–3 verify shape
and honesty held. Style gates hold: relation reads ONLY via kernel R1/R2/R3;
OC PStates ONLY via declared mirrors.

Honest D-006-style scoring: by the LETTER of the file's own banner ("if the
plan needs to change, FAIL the phase"), switching from the PLAN's adopted
single-roundtrip without a phase-FAIL is a ding. By SPIRIT: the contract
pre-authorized exactly this path, PLAN_VALIDATION pre-registered the Phase-B
smoke test as the F-8 falsifier with this fallback as the response, and the
header declares the deviation openly with rationale. Both readings recorded;
deviation ACCEPTED at gate.

## Architecture

Read-only by construction CONFIRMED: zero depots, zero ETL topologies, zero
own PStates; 13 `mirror-pstate` declarations + 4 query topologies; the client
wrappers are the only product surface; assemblers pure. Gate 14 asserts it
three ways (source greps, foreign-depot negative, module inspection).
Mirror routing correct throughout: every mirror read behind `(|hash$$ …)` on
the EXTRACTED object-key; cross-partition scans via `|all$$` + `|origin`.
Advisory N3 resolved as recommended: `resolve-via` reads the spec doc via
`read-context-bundle` first, then pins by source-version-key.

## Trap spot-checks (§12 reviewer duties: traps 3, 4b, 10, 12)

- **3** (activity from requests): written ONLY in the kernel's accepted
  branch, 4th hop after the journal filter; view consumes R3. HELD.
- **4b** (wall clock in topology): zero wall-clock reads in any topology;
  `System/currentTimeMillis` appears only in client wrappers — the contract's
  "wrapper-stamped, client side". HELD.
- **10** (custody): rows proven in Phase A; projection proven by the gate-10
  addition this review. HELD.
- **12** (offset units): gate 5 resolves one md CHAR-unit anchor to its span
  and one transcript BYTE-unit anchor to offset+length. HELD.

## Failure modes attempted

1. Divergent-custody write rendered as Sid's own hand → refuted (gate-10
   block: badge present on divergence, absent on convergence).
2. Replay double-buckets feed entries → refuted (A3 gate 9: byte-identical
   replay; client stamps only).
3. Silent narrowing under caps → refuted (gate 3: returned + omitted ==
   fixture totals; standing `:feed/uncovered` present).
4. Cursor skip/dup at page boundary → refuted (fixed-width order-keys +
   `(char 1)` suffix; gate 11 paging + falsification probe).
5. Unary `:none` row keyed by nil in `:in-family` → refuted (group-relations
   branch order; from-endpoint keying).
6. Back-dated re-import flooding "today" → refuted (gate 4: arrival-only
   window, claimed stamp carried, old-window exclusion, refusal path throws).

## Writers / readers / clearers

The module adds NO state — nothing to own, clear, or leak. The one new state
in the package (A3's `$$relation-activity-by-bucket`) lives in the kernel:
writer = accepted branch only; readers = R3 (+ V1 test reader); clearer =
none by design (append-only, rebuildable projection). Wrappers hold no
in-flight flags, caches, or locks.

## Async ordering risks

A bundle is a composite of two snapshots (module material query + client R1
call); a relation landing between them can be one read newer than the
material. ACCEPTED semantics: addresses resolve to NOW (§9.1), rendered-at is
the drift detector, and mirrors are async snapshots regardless of path. Same
acceptance for the feed's three-branch merge.

## Error-path cleanup

Wrappers are pure reads — failure leaves no state behind.
`close-trail-view-runtime!` swallows close exceptions (test harness only).
`resolve-via` on a spec doc lacking a source-ref throws from the OC query —
acceptable degenerate input pre-daily-use.

## Open doubts (non-blocking; falsifier/fix named)

1. `:written-by` is present-with-nil on convergent edges; contract letter says
   "omitted" (token economy). Text projection unaffected. Fix: `cond->` in
   `project-edge` (~3 lines) when bundle EDN token cost matters.
2. Feed address round-trip untested (gate 2 exercises bundle + trail only).
   Fix: 3-line test next time the file is open.
3. `material-id-prefixes` admits `sa:`/`ce:`/`oc:chat-artifact:` beyond §4's
   v0 list; git-commit shas land in `:bundle/omissions` rather than as
   `:kind :unresolved` dangling targets. Align when WP2 makes shas real.
4. `render-bundle-text` renders every `:this` relation as `->` + the `:to`
   id — direction fidelity vs §8's `->`/`<-` example when the target IS the
   `to` endpoint. Gate 13 (format/budget) passes as specified; orientation
   quality is H3's benchmark. Cheap fix: arrow by endpoint role.
5. No `:allow-yield?` on mirror subselects — Rama 1.6.0 REJECTS it on mirror
   selects (falsification note 2; quirks updated). Watch at corpus growth;
   §6 promotion criteria pre-named.
6. Runtime map exposes OC `foreign-pstate` handles labeled fixture-only;
   seam held by convention there (grep: consumed only by the test ns).
   Falsifier: grep for new product callers.
7. `rand-nth` task counts still not injectable (cycle-1 residue, carried).
8. Marathon-reported "445 assertions" vs this session's 296/302 on the two
   Rama namespaces — unreconciled counting (likely includes the OC suite);
   immaterial: this review trusts only its own runs.

## Process note

Implementation was authored by the marathon (Track-B-origin) Fable session's
fresh Opus subagent after a Sid-authorized double-dispatch collision (both
sessions held mandates for Phase B; this session's two builders stood down
with zero writes). Implementer, falsification reviewer, and gate judge were
three distinct contexts. Package is gate-complete; commit and close are
Sid's.
