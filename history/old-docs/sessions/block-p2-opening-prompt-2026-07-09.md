# Block-distiller Phase 2 — opening prompt (fresh session)

**You are the P2 implementation session** of the block-kernel work package
(`docs/current-mental-model/build/sense-line-mvp/block-kernel/`). P0 + P1 are
DONE and GREEN. Your job: **classification & production-event VISIBILITY** —
make the river/debris class and the delegation chain physically queryable, then
the **G1 + G2** IPC gates. Code stays an ADAPTER over the existing
object-container module (no new module/depot/PState/topology).

## Boot, in order
1. **Load skills:** `/work-package`, `/rama`. (`/rama-pitfalls` only if you add
   any new foreign-append shape — P2 rides P1's proven import path, so likely no.)
2. **Binding docs** (`decisions.md` › `SPEC.md` › `CONTRACT.md` › `PLAN.md` ›
   `PHASE_0.md`): CONTRACT §3(R*)/§4/§7-§9, SPEC §3/§3.4/§7, PLAN §7/F2 (the
   projection-hint home) + §3.1, gates G1/G2.
3. **The P1 code you build on:**
   `src/app/server/rama/object_container/block_distiller.clj` (§A–§I) +
   `test/app/server/rama/object_container/block_distiller_test.clj` +
   `test/resources/block-distiller/{fixture.jsonl,golden.edn}`.

## P1 state you INHERIT (verified this session — do NOT re-derive)
- **P1 green: block_distiller_test 12 tests / 606 assertions / 0 fail;
  +object_container_test 18/755/0.** §G row builders + §I driver landed:
  `distill-conversation!` reads stored per-message payloads (F3 physical read of
  `$$transcript-conversation-projection` → `$$source-artifacts-by-id`), classifies
  first, distills RIVER events, and submits one OC import per event
  (`:append-ack` + `await-object-container-decision`).
- **F2 RESOLVED — option A (ZERO edit).** The spike proved an extra
  `:production/*`+`:delegation/*` map assoc'd onto a per-part `SourceArtifactRow`
  under key `:production-event` **survives the `$$source-artifacts-by-id`
  round-trip**. So P1 ALREADY STORES the delegation home:
  `(:production-event (ocr/read-source oc-rt <per-part-source-id>))` → the full
  `production-event` map (class, actor, actor-role, parent-uuid, is-sidechain,
  prompt-id, on-behalf-of, context/parents, time-ms). **G2 is a READ, not new
  storage.** No `transcript_adapter.clj` carve-out was needed.
- **G1 actor home ALREADY STORED.** Every per-part surface's `created-by` = the
  part's resolved actor (`resolve-actor`): a tool_result surface's created-by =
  `"tool"`, a meta/command surface would be `"harness"`, assistant = model id,
  human = `"human:<userType>"`. So **the "ZERO human-attributed tool_result"
  half of G1 is a physical read of `created-by` on the tool_result surface** —
  no new work, just the gate.
- **Routing law (N1, verified):** OC PStates declare
  `{:key-partitioner partition-by-object-key}` (object_container.clj:1706/1720/1724);
  read+write both route via `extract-object-key`, so id *shape* is everything.
  Per-part source-ids reuse `src:tr:`; unit-ids are `du:chat:HASH:sense-block-v0:…`.
- **Decision-row shape:** `ObjectContainerDecisionRow` → accessors `:status`
  (`:accepted`/`:rejected`), `:reason`, `:errors`. Import actor must be a valid
  core actor-type `#{:human :agent :system :bot}` — the distiller uses `:system`
  (`bd/import-actor`, id `import:sense-block-v0`); `:machine` is RK-only.

## What P2 ADDS
1. **Class → projection entry-kind (the ONE new store).** For each RIVER import,
   add a `TranscriptConversationProjectionRow` to the payload's `:projection-hints`
   with `entry-kind :river` and a **distiller-namespaced order-key** `"sb:" <…>`
   so it neither overwrites nor is confused with the transcript adapter's
   `:message` rows on the SAME conversation key (PLAN §7/F2 — co-tenancy is the
   cost; the `sb:` prefix is the guard). Uses the EXISTING `:transcript-
   conversation-projection` projection-kind + EXISTING declared `entry-kind`
   field — **new VALUES only (T14-ok), NO new `:projection-kind`, NO `case>`
   edit.** **VERIFY FIRST** (before coding): the projection dispatch
   (object_container.clj:~2160-2171) writes ANY `entry-kind` value (does not
   whitelist) — if it rejects `:river`, that is a §9 stop-clause (a `case>` edit),
   escalate to Sid. Read it and confirm.
2. **G1 gate (IPC + pure).** Pure half exists (`g1-classification`,
   `g1-actor-resolution-law`). Physical half: (a) every tool_result/meta surface's
   `created-by` is NEVER `"human:*"`; (b) river-event count with a `:river`
   sb-projection hint == golden river count (4); (c) counts match golden
   (4 river / 6 debris). **Open design call (raise if it bites):** debris events
   are NOT imported (an empty-source import is invalid, object_container.clj:765),
   so there is no per-debris physical class row today. Lean: G1 = river-side
   entry-kinds + the pure classifier (debris class = the absence of a sense-block
   hint + the retained transcript `:message` row); an explicit debris-visibility
   record is deferred unless Sid wants it (it would need a projection-only path).
3. **G2 gate (IPC).** Read `(:production-event (read-source …))` on the sidechain
   assistant's surface (fixture event `a-sub`): assert `:production/actor` =
   `"claude-fable-5"`, `:delegation/is-sidechain` true, `:delegation/on-behalf-of`
   `"a-1"`; and a non-sidechain assistant (`a-1`) has `on-behalf-of` nil. This is
   the physical mirror of the existing pure `g2-delegation-chain`.

## P2 gates — physical readers (T7, validation-only)
- **G1** classification & actor: physical `$$source-artifacts-by-id[src].created-by`
  + `$$transcript-conversation-projection[conv-id]` filtered to `sb:`-order-key
  entries; ZERO human-attributed tool_result/meta.
- **G2** delegation chain: physical `:production-event` on the per-part surface;
  model actor + on-behalf-of present.

Fold both into the existing `p1-import-gates` deftest (it already ingests the
fixture + runs `distill-conversation!`), OR add a sibling deftest sharing the
one-launch pattern — but keep IPC launches minimal (object_container_test:149).
Barrier stays `:append-ack` + `await-object-container-decision`.

## Stop clauses / hard rules
- Projection dispatch rejects a new `entry-kind` value, OR the class hint needs a
  new `:projection-kind`/`case>` edit → §9 stop-clause → Sid. Any non-additive
  `object_container.clj` change → Sid. Relation-kernel changes (P3a's 3 kinds are
  pre-authorized) → Sid.
- File allowlist: extend `block_distiller.clj` + the test + fixtures.
  `object_container.clj` additive-only (T13); everything else = stop-clause.
- CODE uncommitted until Sid's word. Docs auto-commit on the local docs branch,
  never pushed/merged, never mixed with code. NEVER read `src/app/server/env.clj`.
- **Definition of P2 done:** G1 (physical) + G2 (physical) green as IPC deftests;
  the class hint lands as a `sb:`-namespaced projection row without an OC edit.

## At session end
Append a NOW entry (≤15 lines) to `docs/sessions/next-prompt.md`; on a stop-clause,
record it in `decisions.md` Open Questions (PROPOSED) with verbatim citations and
STOP. If P2 closes, write the P3a opening prompt (P3a = register `:grounds
:assembled-from :refines` in `relation-kinds` — the ONE authorized
relation_kernel.clj edit — with its accept micro-gate; then P3b = mechanical edge
floor, gates G8/G9).
