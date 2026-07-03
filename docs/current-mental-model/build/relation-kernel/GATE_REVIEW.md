# Fable Gate Review — relation-kernel-module

Reviewer: Claude (Fable 5), 2026-07-03. This is the CONTRACT §12 reviewer gate:
§11 tests green + traps 1/2/7 spot-checked in the diff + Falsification Pass per
the CLAUDE.md review protocol. Prior-pass records used as input, not as
authority: IMPLEMENTATION_VALIDATION.md (Phase 4), TEST_VALIDATION.md (Phase 6).
Module and test source read in full this session; the code is what was judged.

## VERDICT: PASS — gate green, work package is done pending commit decision

- **§11 gates green, independently re-run this session** (not Phase 7's word):
  `clojure -M:test` on `app.server.rama.relation-kernel-test` →
  **Ran 2 tests, 165 assertions, 0 failures, 0 errors** (exit 0). Phase 7 had
  additionally swept task counts {2,4,8} deterministically, all green.
- All 11 CONTRACT §11 gates have covering test blocks with real falsification
  power (walked in TEST_VALIDATION.md §gates table; re-checked here against the
  final post-T1–T10 suite). The §9 refusals with behavioral surface (1, 4, 5, 6)
  are exercised; (2)(3) are non-features owned by diff review — confirmed absent
  from the module.
- Style gates: typed defrecords in PStates only (map envelope on the wire — the
  F1 idiom); partition helpers imported from object-container, not reimplemented
  (`oc/extract-object-key`, `oc/fixed-width-order-key`; `positive-partition` is
  the module's own 2-line idiom, per PLAN); `{:allow-yield? true}` on every
  unbounded range read (module L645, L651, L668); consumers read ONLY via the two
  query topologies — the direct PState readers are V1-marked test-only, and the
  one direct read in the test ns (`status-log-for`, T9) is IMPLICIT_SPEC-blessed
  test inspection.

## Traps 1/2/7 spot-check (in the diff, as §12 requires)

- **Trap 1 (microbatch, not stream)** — `microbatch-topology` at
  `relation_kernel.clj:513`; the 7-write set (journal, decision, event, row,
  log, 2× copy+descriptor) spans up to three tasks inside ONE batch transaction;
  the two `|hash` hops (L609, L617) are inside the accepted branch of the same
  batch. G1 (pause-forced same-batch pair) empirically confirms intra-batch
  read-your-writes; the {2,4,8} sweep confirms cross-partition atomicity is not
  a 1-task accident. CONFIRMED.
- **Trap 2 (deterministic identity)** — `relation-id-for` (L86-93) =
  `"rel:" + sha1(kind, from.kind, from.id, to.kind, to.id, asserter)` joined on
  a NUL separator. The NUL bytes at L56/L58 were byte-verified this session
  (`cat -A` shows `^@`) — the separator genuinely cannot appear in user data, so
  field-boundary collisions are impossible. Gate 3 proves convergent re-import;
  no `System/currentTimeMillis` anywhere in the write path (grepped) — all
  key-affecting time is client-supplied `:asserted-at-ms`, so batch replay is
  deterministic. CONFIRMED.
- **Trap 7 (full-row copies, never read-modify-write)** — both endpoint copies
  are blind `termval *row` writes (L610, L618); `transition-row` (L271-286)
  preserves `first-asserted-at-ms` on status change, so the sort-key
  (`target-sort-key` embeds first-ms) is stable and copies overwrite in place
  rather than orphaning. Gate 4 asserts both endpoints agree post-retract; e5
  asserts a single physical copy across the full assert→retract→assert
  lifecycle. The descriptors DO use read-modify-write (`term` +
  `apply-descriptor-delta`) — safe because microbatch replay is exactly-once,
  and G1/Gate 3/e5 pin the counts. CONFIRMED.

## Falsification Pass (CLAUDE.md protocol)

### Architecture

One depot partitioned by `hash-by :relation/routing-key` (= relation-id) on a
plain map envelope; ONE microbatch topology computing a pure `relation-outcome`
then executing its named writes; 7 PStates (journal, 2 audit-by-id via custom
key-partitioner colocation, authoritative row, status log, target index,
bounded descriptors); TWO query topologies as the sole public read surface;
foreign client with V1-quarantined direct readers. The pure-outcome/dataflow
split keeps every decision testable outside the topology; the descriptor gate
makes empty reads seek-free and the no-filter read a single whole-map seek
(the Phase-4 F1 fix, verified at L639-651).

### Failure modes attempted (specific scenarios)

1. Mid-batch crash between from-copy and to-copy → endpoints disagree:
   impossible by microbatch transaction semantics; empirically backed by the
   task-count sweep. (Trap 1.)
2. Same-batch duplicate/convergent requests double-write copies or descriptors:
   G1 forces the schedule via pause/resume; different timestamps would mint a
   second sort-key entry if read-your-writes failed — count stays 1. Falsified.
3. Client retry replays: journal gate replays stored decision and writes
   nothing (Gate 2, T4a accepted-retract replay, T4b rejected replay — the
   rejected-decision journal write at L572-575 sits BEFORE the accepted `<<if`,
   so rejected requests replay too). Falsified.
4. Cross-relation key collapse (the F2 ruling's failure mode): Gate 11 — two
   relations, one shared key, both succeed with distinct journal entries.
5. Broken custom key-partitioner (clojure `hash` vs depot `hash-by` mod N):
   G2 + every rejected-decision read routes through
   `partition-by-decision-relation`; ≥6 relation-ids × {2,4,8} tasks — a
   systematic divergence cannot survive. Falsified.
6. Rejected request leaks truth writes: Gate 6 + T5/T6/e1/e2/e3 assert no row,
   no copy, no descriptor, and (T9) no status-log entry at the PState level —
   the R2-vacuity hole Phase 6 caught is closed.
7. Retraction cascade / cross-asserter damage: T2 retracts one of two
   co-resident asserters; the other's row, history, and visibility are
   untouched (§9 refusal 5).
8. Unary `:none` nil-key hotspot: e3 proves a `:none` target WITHOUT an
   inherited key is rejected as malformed; Gate 8 proves both copies colocate
   under the inherited key.
9. Descriptor underflow via retract-affirm double-decrement: T3 — counts
   unchanged on a same-status retract (`count-deltas` same-status arm), plus
   the `max 0` guard as belt-and-braces.

### Writers / readers / clearers (per changed state)

All 7 PStates: single writer = the one microbatch topology (one source block;
no other topology, no other module). Readers = the two query topologies
(product) + V1 foreign-selects (tests only). Clearers = **none, by contract**
(§9.6 no deletion; retraction is a status write). Growth is deliberate and
bounded where it must be: descriptors ≤ 2×|registry| per target; journal/log
are subindexed maps (audit accretes by design). No state can be "stuck masking
future truth": every accepted transition rewrites the authoritative row and
both copies in the same batch; there is no cached/derived state outside the
batch boundary.

### Async ordering risks

Per-task processing is serialized; within a batch, same-relation requests
process in depot order deterministically (G1). Cross-batch, last-writer-wins
follows depot order — correct for an event-sourced status. The one genuine
ordering surface: **history display order keys on client-supplied
`asserted-at-ms`** (`fixed-width-order-key ts request-id`), so an importer
submitting out-of-order timestamps for one relation gets history ordered by
claimed time, not arrival. This is the price of deterministic replay (no wall
clock in the topology) and matches the contract's client-computed-identity
stance. Noted as an importer discipline, not a defect (see Open doubts).

### Error-path cleanup

There is nothing to clean: no in-flight flags, locks, pending sets, or caches
anywhere in the write path — rejection is a pure alternate outcome that writes
only the decision spine. Client-side, `append-relation-request!` throws BEFORE
appending on a blank routing key (the counter/test invariant depends on this;
verified at L768-771). The only silent drop is the topology's blank-routing-key
`filter>` (L551), unreachable through the public builders and defended in depth.

### Open doubts (none blocking; recorded for the record)

1. **Envelope/payload binding is client trust.** The topology never verifies
   `:relation/routing-key == relation-id-for(payload)`. A hand-built envelope
   with a mismatched routing key would journal/write under the routing key
   while the row carries payload-derived fields. Unreachable via
   `assert-request`/`retract-request` (they derive both from one source), and
   the depot is an internal surface — but when agent-authored writers appear
   (D-003 spine), a one-line server-side recheck (reject on mismatch) is the
   cheap hardening. Extension point, not a §11 obligation.
2. **`replayed-from-decision-id` is never populated** (replay writes nothing,
   so the field has no writer). Dead audit slot; harmless; drop or wire it if
   a future consumer wants replay counts.
3. **`relations-pairs->map` dedup is O(n²) per target** (linear scan per row).
   Irrelevant at trail-view scale; revisit with pagination if a hub target
   accrues thousands of relations.
4. **Single-worker tests**: cross-worker serialization of the defrecords is
   not exercised (already on record since Phase 5; matches the compute/
   transcript kernel convention). A `:workers 2` smoke run remains the cheap
   follow-up before any multi-worker deploy.

### Done gate (named failure mode + why it cannot happen)

The one failure that would poison everything above this module — the same edge
disagreeing with itself depending on which endpoint you ask from — cannot
happen because both endpoint copies and the authoritative row are written from
ONE `*row` value inside ONE microbatch transaction (termval, no read-modify),
and the sort-key under which copies live is stable across status changes
(preserved `first-asserted-at-ms`). Gate 4's "endpoints never disagree on
status" and T10's full-record triple-equality pin it empirically.

## D-006 evaluation notes (this gate is a datapoint)

- Criterion 1 (trap count): the contract's §10 ledger names 9 traps with naive
  alternative + concrete failure; at least traps 1, 7, and 8 (stream
  partial-application, lazy-copy self-disagreement, nil-key hotspot) are
  non-obvious. **Met.**
- Criterion 3 (implementation contact, as amended): gates green with ONE
  contract amendment — the F2 idempotency-scope ruling. The letter-vs-spirit
  scoring already recorded in decisions.md stands; nothing after the ruling
  required further amendment, and the module implements the contract as
  written everywhere else.
- Criterion 2 (counterfactual probe): NOT yet run; input manifest is §13.
- Criterion 4 (token ledger): gate review ≈ one Fable session (this one),
  read-heavy, no implementation tokens.

## Post-gate amendment (same session, commit time)

At commit, git classified the module as binary because of the two raw NUL
bytes, which would have made every future diff of the kernel unreviewable. The
raw bytes were re-spelled as `"\u0000"` string-literal escapes — runtime
values proven byte-identical by eval (`(= sep (str (char 0)))` → true) — and
the full suite re-run green (165 assertions, 0 failures) before amending the
commit. The separator VALUE is unchanged; only its source spelling is.

## After the gate

Per STANDING: commit decision on the two code files is Sid's; the D-003
Regime-1 spine does not start without Sid. Package-close retro (the
`decisions.md` succession-document input) fires when Sid closes the package.
