# Block Distiller (sense-block-v0) — RETRO

**Package:** block-kernel / sense-line-mvp
**Window closed:** 2026-07-10 (code commit pending Sid's word)
**Outcome:** Round-2 package gate **PASS**. P0–P5 + the gate-fix wave complete.
Suites 28 tests / 1250 assertions green; real `7c80ce2a` receipt clean.

This RETRO covers the P5 + gate-fix window. P0–P4 shipped earlier (`a146dc8`);
their RETRO context is in `docs/history/progressive-summary.md` and the phase docs.

## 1 · What shipped in this window

- **P5** (`block_distiller.clj`, uncommitted): `river-page` — composition-first
  render of the first bounded page of a conversation's persisted river blocks
  (F3-input ledger scan → per-part source ids → `read-common-material-for-source`
  + `read-unit` query topologies) + the guarded real-file e2e receipt.
- **Gate-fix wave** (this window): the F1 ruling + F2/F3/F4 fixes + Gate-R2
  Finding-1 closure. Details in §2.

## 2 · The gate cycle (the spine of this window)

**Round-1 gate** (fresh context, default-fail): **FAIL** — four P1 blockers
(`GATE_ROUND_1` artifact / the Round-1 verdict). Resolutions:

- **F1 (STOP CLAUSE) — G12 read literally.** `river-page` does 2 point-reads per
  block via `read-unit` (`object_container.clj:2483`) — bounded by a 64-cap but a
  per-block point-read fan-out. The §8 one-liner ("no N-point-read fan-out per
  page") strictly-failed, but PHASE_0 §2/g + CONTRACT §12/g/§10 + the P5 opening
  prompt all pre-blessed composition-first-with-bounded-point-reads for v0. **Sid
  ruled Option A** (2026-07-10): G12 amended IN PLACE in CONTRACT §8 — forbid
  *conversation-scaled* fan-out; accept a hard page cap + a page-size-bounded,
  truncation-signalled measured seek plan; the denormalized `read-conversation-
  sources` query is the §10 scale extension, built only on a used-form break
  (D-001). Option B (strict zero per-block fan-out via an additive OC physical
  shape) deferred to that form-break.
- **F2 — silent short page + dishonest seek accounting.** `river-page` could
  return a capped page as if complete, and foreign strata sharing a surface were
  read-then-discarded (O(limit²) worst case). **Fixed:** filter derived-unit refs
  by distiller-id (via `target-id`) BEFORE `read-unit` — foreign strata rejected
  with zero point-reads, so `unit-reads ≤ page size` and `1+4·limit` is an honest
  bound; new read-plan keys `:truncated? :page-complete? :river-events-total
  :river-events-rendered :surfaces-rendered :blocks-returned` — a capped page is
  never presented as complete.
- **F3 — debris class inferred, not durable.** The `sb:` ledger recorded only
  `:river`; debris was inferred (message-rows − river-markers), storing no
  classifier version. **Fixed:** an additive OC validator relaxation (Sid's
  authorized 2nd kernel edit) — `import-request-validation-errors` now rejects
  only a *truly* empty payload: `(and (empty? source-rows) (empty?
  projection-hint-rows))` — so a projection-hint-ONLY import (a class marker with
  no material) is accepted. The driver writes a durable versioned `entry-kind
  :debris` ledger row per debris event; classifier-id + reason ride in
  `:content-preview`.
- **F4 — refine! duplicated `(surface, span)` identity.** Refining a whole-message
  block onto a span already occupied by a structural sub-block minted a *second*
  `refine:` unit-id for one identity (SPEC §6.1 violation). **Fixed:**
  `resolve-existing-unit-at-span` resolves `(surface, span)` to the incumbent
  sense-block-v0 unit and REUSES its id (`:resolved? true`), asserting only the
  `:refines` edge; mints only for a genuinely new span; rejects sub-span == coarse
  span.

**Round-2 gate** (fresh context, independent): **PASS** — all four reproduced as
fixed, no surviving contract-level blocker, no named gate (G1–G13) fails; three
non-blocking findings (`GATE_ROUND_2.md`).

- **Finding 1 [MEDIUM] — closed in-window.** On the real file, 3 events are
  *river-class but surfaceless* (an empty `tool_result` → 0 blocks). They fell to a
  count-only branch: no durable class row (SPEC §3.1 gap), `:debris` +3 over,
  `:river` −3 under. **Fixed:** generalized `debris-import-request` →
  `class-hint-import-request` — a durable versioned class row for EVERY event with
  no surface import, entry-kind = the event's ACTUAL class, counted by that class.
  Pure test `f3b-surfaceless-river-counts-as-river` bites it. Second-order effect
  (correct): `river-page`'s `:river-events-total` rises with the river count,
  because the class ledger and river-page project from the SAME durable rows.
- **Finding 2 [LOW]:** no cursor past page 1 — deferred to CONTRACT §10 (DoD is
  page 1 only).
- **Finding 3 [LOW]:** multi-stratum under-fill — unreachable in v0's single
  distiller (the pre-filter bundle is bounded by `remaining` *total* refs; a
  coexisting foreign stratum could push same-distiller blocks out of the window
  without `:truncated?`). Revisit when a 2nd distiller ships.

## 3 · Receipts (measured, this window)

- **Suites** (one JVM): 28 tests / 1250 assertions / **0 failures / 0 errors**
  (block-distiller + object-container + relation-kernel). Object-container suite
  green confirms the F3-A validator relaxation is regression-free.
- **Real `7c80ce2a` receipt** (post-Finding-1, directly measured): 759 obs / **0
  parse errors** / 1676 containers; **247 river / 399 debris / 44 mechanical
  edges** (was 244/402 pre-Finding-1 — the 3 surfaceless-river events now count as
  river); **0 import rejections** (the Round-2 gate independently confirmed all
  decisions `:accepted`). River-page read-plan **invariants** hold — measured in the
  receipt and independently reproduced by the Round-2 gate: `:truncated? true` /
  `:page-complete? false` (a 32-block page over a 247-river conversation is honestly
  flagged partial), `:unit-reads == :blocks-returned` (the distiller pre-filter
  admits no wasted reads → `1+4·limit` is an honest bound), `:seek-count ≤
  :seek-bound`. `:river-events-total` tracks the river count (247) — the class
  ledger and river-page project from the same durable rows. One benign RK-startup
  `LeaderNotFoundException` (documented contention flake).

## 4 · Lessons (what earned its keep + what to route)

1. **The fresh-context real-corpus gate found what synthetics couldn't — twice.**
   Round-1's F1–F4 came from reading the code cold. Round-2's Finding-1 is
   invisible to the FIXTURE (which has no empty-`tool_result` river event) — only
   the real 402-debris chat exposed the surfaceless-river class. Same shape as
   code-atom's G-F1: the full-real-corpus layer catches a class the fixtures
   structurally cannot. **→ /work-package:** a real-corpus receipt should ASSERT a
   class-completeness invariant (every ingested event has exactly one durable class
   row: river-with-surface + river-hint + debris-hint == total), not merely run and
   print.
2. **Provenance emission must key on the THING, not the code path.** F3 and
   Finding-1 both stemmed from hanging the class row off "does it have a surface?"
   instead of "what is its class?". The durable class is a function of
   `classify-event`, independent of whether the event has material. ("The map must
   not lie" applied to class provenance; "verb first, place later".)
3. **A one-line gate text drifts from its authoring intent.** G12's §8 one-liner
   lost the v0 carve-out that §12/g + PHASE_0 §2/g + the P5 prompt all carried. When
   a gate's short form and its authorizing docs disagree, the fix is to reconcile
   the text (A), not to re-derive a stricter bar (B) that no used form needs
   (D-001). The fresh-context gate is what surfaces the literal-vs-intent gap.
4. **OC import gotchas → `implementation-quirks.md`:** (a) the import validator
   rejects empty-source payloads; a projection-hint-ONLY (class-marker) import needs
   the additive `(and (empty? source-rows) (empty? projection-hint-rows))`
   relaxation. (b) `$$source-anchors-by-source` stores span-less refs
   (`SourceMaterialRefRow`, target-id = anchor-id); to resolve `(surface,span) →
   unit` read `$$source-anchors-by-target` (by unit-id, full rows with offsets).

## 5 · Deferred / open (carried, not lost)

- **CONTRACT §10 scale extension** — a denormalized `read-conversation-sources` /
  renderable-per-source-unit query + a page cursor; built when a used form breaks
  against the per-block reads or the 64-cap (D-001).
- **Multi-stratum river surfaces** (Finding 3) — revisit when a 2nd distiller
  ships; the bundle limit + pre-filter interaction needs a per-distiller bound then.
- **River content-preview asymmetry** — river-surface rows carry the actor;
  hint-only class rows carry classifier/reason. Harmless; noted for the marks round.

## 6 · Adversarial retro-recheck

A fresh pass over this RETRO's own claims:

- **Class-completeness — the strongest claim — checked exhaustively.** Every event
  takes exactly ONE of two branches in `distill-conversation!`: a surface import
  (river-with-surface, carrying its river hint) OR `class-hint-import-request`
  (always returns a request; entry-kind = the actual class). The branches are
  exclusive and total — no event shape falls through. So "every message event has
  exactly one durable class row" holds by construction: 244 river-with-surface + 3
  river-hint (surfaceless) + 399 debris-hint = 646 message events (§3). This is the
  invariant Finding-1 closed, and the property a /work-package real-corpus receipt
  should ASSERT (lesson 1).
- **The `:debris-decisions` → `:class-hint-decisions` rename has no orphaned
  consumer.** The receipt `select-keys`es only counts; the only test reader was the
  F3 block (updated). The green 28/1250 suite would have failed on a stale reader.
- **Finding-1's fix is self-verified, NOT re-gated by a fresh context.** The Round-2
  PASS was on the pre-Finding-1 code; the fix is verified by the re-run suite
  (28/1250), the pure bite test, a direct eval, and the receipt (§3). Proportionate
  for a non-blocking count/durability correction — but an honest gap: if gate-grade
  independent verification of the Finding-1 fix specifically is wanted, the Round-2
  gate agent retains context and can re-verify it.
- **No overclaim on F1.** A is framed as ratifying pre-existing intent, not a
  concession — supported by the three authoring docs cited in §2/F1. B remains the
  correct answer WHEN a used form breaks against the per-block reads; deferred, not
  rejected.
- **Honest deferrals.** Findings 2/3 and the §10 scale path are carried in §5, not
  buried; the multi-stratum under-fill is stated as unreachable-in-v0, not fixed.
