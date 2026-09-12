# P3 Falsification — mechanical edge floor (block-kernel)

> ⚠ **THIS IS THE AUTHOR SELF-REVIEW — a pre-pass, NOT QC layer 4.** It was
> written by the SAME context that implemented P3, so per the work-package skill
> ("author self-review never substitutes for a fresh-context layer") it does NOT
> count as the falsification the process requires. It is recorded only as input
> for the fresh reviewer to *disagree with*, never as a passing verdict. The
> real fresh-context falsification is **PENDING** — Sid runs it in a separate
> session (see `docs/sessions/block-p3-falsification-opening-prompt-2026-07-09.md`).

Adversarial pass (CLAUDE.md review protocol) over the P3a+P3b diff:
`relation_kernel.clj` 3-kind add · `block_distiller.clj` §H/§J additions +
`event-ctx` extraction · `block_distiller_test.clj` micro-gate + edge gates ·
`fixture.jsonl` augmentation + `golden.edn` regen. Written by the P3
implementation session (Opus 4.8) as its own falsification layer; the package
gate review (Fable, full-code read) still closes the package.

**Verdict: no blocking defects. 1 test gap found + FIXED at review; 3 open
doubts, none blocking. All P3 gates green as IPC deftests.**

## Architecture

The block distiller adds NO Rama surface (no topology / depot / PState) — the
adapter-shape placement dividend (CONTRACT §2). P3b is a foreign client that
(a) demand-mints a coarse `:tool-result-span` block via OC import (N3 option A,
Sid-ruled 2026-07-09), then (b) asserts `produced`/`grounds` edges into the
relation kernel. rama-pitfalls verdict: **READY-TO-CODE** (every section PASS or
N/A; the small surface is why).

## Writers / readers / clearers per changed state

| state | writer | reader | clearer | idempotence |
|---|---|---|---|---|
| `relation-kinds` (+3) | none (const) | `registered-kind?` | — | guard intact: micro-gate proves an unregistered kind still rejects |
| coarse `:tool-result-span` unit (`$$derived-units-by-id`) | OC import topology (via driver request) | edge `to`-endpoint + river-page (future) | never (immutable) | deterministic unit-id + **distinct** deterministic import-key → OC journal dedup — **GATED** (G4-style: byte-identical after re-run) |
| `produced`/`grounds` edges (`$$relations-by-id` + endpoint copies) | RK microbatch topology | `read-relation-row` / `read-relations-for-targets` | retract (unused here) | journal `(relation-id, idem-key)` — **GATED** (G4-style: byte-identical after re-run) |

## Failure modes attempted

1. **Re-run mints a duplicate coarse block** → FALSIFIED (deterministic
   import-key → OC dedup). Was NOT explicitly gated → **FIXED AT REVIEW**: added
   coarse-block byte-identity to the G4-style test (`= coarse-before
   coarse-after`).
2. **Hole endpoint accidentally resolves to a real block** → FALSIFIED: the hole
   id `du:<ok>:sense-block-v0:pending-result:<tuid>` is not a valid
   `%06d:%02d:%06d` block-path, so it can never collide with a minted unit; G9
   asserts it is physically `nil` in `$$derived-units-by-id`.
3. **Two tool_uses collide on one edge relation-id** → FALSIFIED: distinct
   from-unit-ids (distinct part-index) → distinct `from` refs → distinct
   relation-ids; G8 asserts 3 distinct.
4. **RK rejects `:asserter-type :machine`** (the P1 OC `:machine` lesson) →
   FALSIFIED: RK has no asserter-type allowlist (`request-shape-errors`
   :309-335 checks only `:actor/id` present, kind registered, targets
   well-formed); the micro-gate + G8 physically confirm `:accepted` +
   `:asserter-type :machine` on the row.
5. **`event-ctx` extraction changed existing distillation** → FALSIFIED: the
   golden regen diff = `a-1` +2 tool_use blocks ONLY; every pre-existing block
   byte-identical (secret still `[REDACTED]`); P1/P2 physical gates
   (G3/G6/G11) green unchanged.
6. **Wall-clock in edge ids/time** → FALSIFIED: `asserted-at`/`sent-at` = the
   tool_use event's parsed transcript timestamp; ids = sha256 of source; no
   `System/currentTimeMillis` (hygiene grep clean).

## Async ordering

- The coarse-block OC import (awaited via `await-object-container-decision`)
  completes BEFORE its edge assert (RK), so the `to` endpoint resolves to a real
  block when the edge lands. Even reversed, the edge persists (holes legal). The
  driver is single-threaded; `doseq` is sequential; no interleave.
- Barrier: RK microbatch **processed-count** (cumulative; `submit! == +1` with an
  always-present routing-key = relation-id → no ingress drop). Fresh RK runtime ⇒
  cumulative == driver appends. G4-style re-run barriers on `2×` (journal replays
  are still consumed + counted). The polling `await-relation` helper is
  deliberately unused.

## Error-path cleanup

- `close-distiller-runtime!` closes both runtimes; each deftest wraps it in
  `try/finally`.
- **Open doubt (1):** coarse-block import decisions are awaited but not
  surfaced/halted-on — a rejected mint would silently degrade the edge to a hole
  (holes are legal, so not *wrong*, but invisible). Matches the P1 pattern
  (decisions collected, not gated). Falsifier already in the suite: the test
  asserts the coarse block LANDED (`read-unit-physical` non-nil), so a rejected
  mint fails the test. Non-blocking.

## Shape (executed against the actual structure)

- Edge refs built via `rk/->target-ref :block uid` → `{:target-kind :block
  :target-id uid :target-key uid}`; `well-formed-target?` (:288-293) satisfied.
  Confirmed by acceptance (G8) — not trusted from intent.
- Coarse-block payload: `part-rows` re-declares the tool_result surface (same
  deterministic source-id → converge) + 1 unit + 1 anchor; import validation
  (`anchor.source-id` must be a declared surface, :789) satisfied. Confirmed by
  acceptance + physical read of the landed unit.

## Open doubts (non-blocking; each with a falsifier)

1. Coarse-block import decision not surfaced (above) — falsifier: the
   landed-block assertion in `mechanical-edge-gates`.
2. **Edge direction** `from = tool_use → to = tool_result/hole` follows the
   plan's `(tool_use, tool_result)` ordering (§H). Whether `grounds` should point
   the other way (evidence → claim) is a *kinds-round* question, not a
   mechanical-floor correctness one; SPEC §11.1 keeps epistemic `grounds` an
   interface, family-assigned later. G8 checks kind + asserter + `:block`
   endpoints, not direction.
3. **N4 two-IPC**: verified for IPC tests (the green `mechanical-edge-gates`
   dual-launch). A production deploy uses real clusters; a foreign client spans
   them the same way. Falsifier: the green two-IPC deftest.

## Done gate

Named failure mode that cannot happen: **a re-run duplicating edges or blocks** —
both are deterministic-key, journal-deduped, and now byte-identity-gated (edges
*and* coarse blocks). P3a micro-gate + P3b G8/G9 green as IPC deftests
(RK microbatch barrier). **P3 is DONE.**

## Receipt (each suite independently re-run this session)

- `block_distiller_test` — **15 tests / 740 assertions / 0 fail**
- `object_container_test` — **6 tests / 149 assertions / 0 fail** (import target;
  no regression)
- `relation_kernel_test` — **2 tests / 222 assertions / 0 fail** (after the
  3-kind add; descriptor-count bound self-adjusts)
