# Findings — Kernel-Contract Track (inline consolidation)

Source: `IMPLEMENTATION_VALIDATION.md` (R4, **major-fail**) — full traces incl. the C1–C19 enforcement map and KERNEL-SHAPE claim checks live there; this file is the actionable index. Subjects: text_kernel.clj + core.clj + util_fns.cljc, with kernel.clj as the as-built contract artifact. Test validation (R6) not run for this track.

## HIGH

- **K-01 Request commit spans 5 independent `|hash` hops.** Each commits independently; a status-set racing its own ingest takes a shorter hop chain and rejects `:target-unit-not-found`; `:individual` retry can regress the artifact head to an older revision. Contract requirements C4 (routing-key serialization) and C8 (atomicity) are broken by the implementation's own shape.
- **K-02 Retry re-interpretation flips committed decisions.** Replay re-interprets against later state — a committed accepted decision flips to rejected while the attempt-1 event row stays durable (orphan event); `(now-ms)` in `:decided-at` rewrites audit rows on every retry.
- **K-03 Committed events are overwritable.** No dedup/collision guard: re-append with the same request-id or a colliding proposed-event-id `termval`-overwrites a committed KernelEvent. Event immutability — the contract's central promise — is unenforced.
- **K-04 Judgments re-attach across revisions.** Unit ids (`<artifact>/line/<n>`) are not revision-scoped; after re-ingest, an old `:rejected` status silently discards different new text.
- **K-05 Poison records.** Garbage appends (missing/non-String `:request/id`, non-keyword-keyed map) violate the `{String (map-schema Keyword Object)}` write schema → the record retries forever and no decision is ever produced (no-silent-drop violated; same class as compute C-14).

## MEDIUM

- **K-06** Unbounded non-subindexed collections: `$$units-by-artifact` (per-line growth), `$$unit-status-by-branch` (all judgments under one branch key on one task), `$$text-revisions` (every revision's full content in one blob).
- **K-07** util_fns mirror atoms are the UI's source of truth with no rebuild path — a back-arrow (C12) violation in the contract layer itself; the "production" runtime is an in-memory IPC with no durability.

## LOW

- **K-08** Hygiene/drift cluster: status event ordering key `[:unit id]` ≠ routing key `[:artifact id]` (C4 drift); missing `:proposed/event-id` + capability validation, kernel-derived event ids (C3/C5/C7); contract fns duplicated core↔text_kernel (C1 drift); `-by-*` names on primary PStates (C16); redundant 5-op tails in all branches; 4 consecutive-keypath sites; `select-pstate-one` reimplements `foreign-select-one`.

## Contract scorecard

Enforced and holding: C2, C9, C10, C11, C13, C17, C18, C19. Prose-only or violated: C4, C8 (K-01), C6 (K-02), event immutability (K-03), C12 (K-07), C1/C3/C5/C7/C16 drift (K-08). KERNEL-SHAPE's claims are aspirational for the text instance in exactly those spots.

## Fix direction

(1) Single-hop (or journaled) commit + decision/event dedup guards + deterministic decided-at (K-01/02/03), (2) revision-scoped unit identity (K-04), (3) ingress validation before keyed writes (K-05), (4) schemas/subindexing + kill UI mirror atoms via e/watch on PStates (K-06/07), (5) contract-fn dedup + naming (K-08), then update kernel.clj's KERNEL-SHAPE to match reality. Resume skill Phase 3 against PLAN.md; re-run Phase 4 after.
