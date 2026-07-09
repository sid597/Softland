# P1/P2 Falsification Review — block-distiller (QC layer 4)

Round: after P1 (per-part surfaces + OC import) and P2 (class ledger + delegation
gates) landed green. Fresh-context adversarial reviewer (Opus, default-fail),
static trace of a frozen snapshot, hunting BOTH implementation bugs and vacuous
tests. Verdict below is the reviewer's; **dispositions are the orchestrator's**
(findings classified on merit, not mirrored).

## Verdict
Reviewer: **FAIL** (default-fail rule) — **no blockers**; 2 should-fix + 1
test-weakness. Orchestrator reading: **the P1/P2 code is functionally correct**
(all gates green, correct for the fixture + real-file river events); every finding
is **latent** (no gate fails, no current functional break). Three real items to
disposition, none halting.

## Findings + dispositions

### F1 — SPEC §6.1 identity: coincident `(surface, span)` → two ids · should-fix · SPEC divergence
`free-cut-part` `:human-message` (block_distiller.clj §F, P0) emits a whole-block
`[0,len)` PLUS `free-cut-text`'s sub-blocks. For a **single-paragraph** human turn
the whole-block and the lone `:human-sub` share `(surface, span)` `[0,len)`, but
unit-ids are block-path-minted → **two units + two anchors** at one address. SPEC
§6.1 (MUST): same `(surface,span)` ⇒ same identity, never duplicates. CONTRACT R3
claims "§6.1 maps to anchor rows" — but anchor-id = `sa:unit-id` (unit-derived, not
`(surface,span)`-derived), so R3's mapping is **unrealized**. Consequence: a mark on
"this span" is ambiguous between the two units (SPEC §5.3 over-chunking surgery).
- **Inherited from P0**, not introduced by P1/P2. The fixture's human message is
  multi-block, so **no gate exercises it** — an untested divergence, not a false-green.
- **Disposition: ESCALATE to Sid** (decisions.md Open Question). §6.1 is countersigned;
  a single-paragraph human turn (present on the real 7c80ce2a) is form-break evidence.
  Recommended resolution (Sid rules): in `free-cut-part` `:human-message`, drop any
  sub-block whose span == the whole-message span (subs add only where real structure
  exists — SPEC §4.4 "where the message carries markdown structure"). Fixture golden
  unchanged (its human msg is multi-block); add a single-paragraph human case to cover it.

### F2 — `imp:sense-block:` import-key mis-routes under `extract-object-key` · should-fix · latent
`import-key` = `"imp:sense-block:" + object-key + ":" + hash` (block_distiller.clj §A).
`extract-object-key` (object_container.clj:284-339) matches neither `imp:tr:` nor
`imp:md:` → `:else` → returns the WHOLE string, not `chat:<hex>`. Impact: `$$import-
completions-by-key` is key-partitioned by `partition-by-object-key`, so a FOREIGN
`read-import-completion` (runtime.clj:149) routes to `partition(whole-string)` ≠ the
`partition(chat:HASH)` where the topology wrote it → **nil for an existing completion**.
- In-topology dedup is UNAFFECTED (local-select on the object-key task; G4 green proves
  idempotence). **No P1/P2 or package consumer calls `read-import-completion`** → latent.
  But it violates CONTRACT T4 ("never invent a second routing convention").
- **Disposition: fix (in-allowlist), Sid confirms the R4 tension.** Cheapest correct fix,
  block_distiller.clj-only: make the object-key recoverable after a handled prefix —
  e.g. `"imp:tr:" + object-key + ":sb:" + hash` (extract-object-key strips `imp:tr:` →
  `chat:HASH`; still a DISTINCT full key, so R4 "second distillation dedups on its own
  key" holds). Cost: reuses the `imp:tr:` prefix (muddies R4's "distinct prefix" wording,
  but R4's REQUIREMENT is a distinct KEY, which holds). Alt = a kernel edit adding an
  `imp:sense-block:` branch to `extract-object-key` (T13 additive, but a block-specific
  kernel branch is uglier). Surfaced to Sid because it sits on the T4/T13/R4 boundary.

### F3 — G4 idempotence gate can't tell clean-replay from conflict-reject · should-fix · test
G4 (block_distiller_test.clj) discards the re-run's `:decisions` and asserts only
`before==after` (units) + counts. A fingerprint-conflict REJECTION
(object_container.clj:1893-1908) writes no rows → before==after, counts unchanged →
**G4 greens on a reject**. A determinism regression (e.g. wall-clock in a fingerprinted
field) would flip re-runs to `:rejected` yet keep G4 green.
- **Disposition: HARDEN** — capture the re-run summary; assert its decisions are
  `:accepted` AND its river count == 4. (Determinism is currently fine — fingerprint
  excludes created-at-ms; this is a falsification-power gap, not a live failure.)

### Nits (acknowledged)
- **N2** — `sa:` anchor-ids also mis-extract, but anchors are ALWAYS keyed by target-id
  (unit-id) / source-id, never anchor-id → harmless (matches existing adapters). No action.
- **N5** — `import-payload` docstring says "12-key"; the payload has **10** keys (PLAN §3.1
  typo inherited). Also `:source-ref/:source-hash/:source-format` absent at payload top level
  → fingerprinted nil (deterministic; matches the transcript adapter). Fix: docstring → "10-key".
- **N1** — G4 has no own `(some? u)` guard; saved by G3-physical asserting it earlier in the
  same deftest. Low priority (fold into the F3 hardening).
- **N3/N4** — G6 greps literal `sk-FAKE` (adequate for the planted secret); G3-physical is a
  storage/round-trip check, semantics rest on the golden (G3-pure). Both correct-by-design.

## Hunt-list result (1–12)
SURVIVED: 1 (zero-units caught by hard `= 4`/`some?` gates), 2, 3, 4, 5 (driver order == golden
order, hard-guarded by `(= 10 (count message-rows))`), 6 (all ids route except F2/N2), 7
(determinism + two-place id agreement provably identical), 8 (class-hint co-tenancy + no
re-ingest loop), 9 (import validity incl. the 0-unit tool_result surface), 10 (NUL not blank),
12 (G6 inspects the right rows). BROKEN: 11 (→ F1). Routing anomalies: F2, N2.

## Open doubts — resolved
- Extra `:production-event` key survives the PState round-trip: **RESOLVED** — the F2 spike
  (`f2-production-event-survives-source-round-trip`) ran green this session.
- `|hash` ≡ `partition-by-object-key` task equivalence: **RESOLVED** — for object-key
  `chat:<hex>`, `extract-object-key` is identity, so `partition-by-object-key` = `|hash`;
  the green physical reads confirm write/read colocate.

## Coordination note
A parallel P3 session is live-editing `block_distiller.clj` + `block_distiller_test.clj`.
The F2/F3/N5 fixes are NOT applied here (no race-edit). They are queued for a coordinated pass
after Sid rules F1/F2 and P3 reaches a safe point.
