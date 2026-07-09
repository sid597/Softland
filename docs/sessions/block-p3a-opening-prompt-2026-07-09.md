# Block-distiller Phase 3 — opening prompt (fresh session)

**You are the P3 implementation session** of the block-kernel work package
(`docs/current-mental-model/build/sense-line-mvp/block-kernel/`). P0–P2 are DONE
and GREEN. P3 has two sub-phases: **P3a** (register the 3 relation kinds — a tiny,
PRE-AUTHORIZED `relation_kernel.clj` edit + accept micro-gate) then **P3b** (the
mechanical edge floor — the driver asserts `produced`/`grounds` edges; gates G8/G9).

## Boot, in order
1. **Load skills:** `/work-package`, `/rama`, **`/rama-pitfalls`** (P3b is the first
   RELATION-kernel foreign-append surface in this package — run pitfalls on the
   edge-assert design: append shape · ack level/barrier · idempotency key · which
   endpoint the edge targets · retry-safety — before coding P3b).
2. **Binding docs** (`decisions.md` › `SPEC.md` › `CONTRACT.md` › `PLAN.md` ›
   `PHASE_0.md`): CONTRACT §3(R*)/§4(mechanical-edges row)/§7-§9/§12, SPEC §11
   (edges + the mechanical floor)/§9 (holes), PLAN §0(SC1)/§3.4(RK assert trace,
   N6/N7)/§5(Phase 3a/3b)/§7(N3), gates G8/G9.
3. **The P0–P2 code you build on:** `block_distiller.clj` (§H already has the pure
   edge specs: `edge-spec`, `write-tool?`/`read-tool?`, `write-tools`/`read-tools`,
   `edge-idempotency-key`) + the P2 driver + `block_distiller_test.clj` + fixtures.

## State you INHERIT (verified this session — do NOT re-derive)
- **P2 green: block_distiller_test 12/625; +object_container_test 18/772, 0 fail.**
  Surfaces carry `created-by` (actor) + `:production-event` (delegation, F2 option A);
  river events carry `sb:` class-ledger hints; G1/G2 physical.
- **§H edge specs already exist (pure, P0):** `(edge-spec kind from-id to-id)` →
  `{:kind :from-id :to-id :idempotency-key}` with `edge-idempotency-key` =
  `sha256(from ∥ (name kind) ∥ to)` (relation-scoped, T11). `write-tools`
  `#{"Edit" "Write" "MultiEdit" "NotebookEdit"}` → `:produced`; `read-tools`
  `#{"Read" "Grep" "Glob" "NotebookRead" "WebFetch" "WebSearch"}` → `:grounds`.
- **RK entry points (relation_kernel.clj, verified this session):** `relation-kinds`
  is a CLOSED set at **:58-68** — it currently holds the D-004 kinds + stance kinds
  + the code-atom round's `:requires :calls`, but **NOT** `:grounds :assembled-from
  :refines`. `registered-kind?` :289; unregistered → reject `:relation/kind-unregistered`
  at :330-331. `assert-request` :875; `append-relation-request!` :920;
  `start-relation-runtime!` :894 (returns RK foreign handles incl. `$$relations-by-id`).
- **RK is MICROBATCH** (PLAN §3.4): the barrier is NOT `:append-ack` — use
  `rtest/wait-for-microbatch-processed-count ipc module "relation-kernel-topology"
  <cumulative-N>` (relation_kernel_test / git_spine_test precedent). Journal
  `(relation-id, idempotency-key)` ⇒ re-assert writes nothing (G4-style idempotence).
- **N7 (verified):** RK targets need `{:target-kind :target-id :target-key}` — build
  refs via `rk/->target-ref :block <unit-id>` (`:block` → `:else` branch → target-key
  = verbatim unit-id), NEVER a hand-rolled map. **N6:** RK has NO tier field — "silver"
  rides the asserter `sense-block/mechanical@1`; do NOT invent a tier field (T14).

## P3a — register the kinds (PRE-AUTHORIZED; NOT a stop-clause)
CONTRACT §12 authorizes exactly this: add `:grounds :assembled-from :refines` to
`relation-kinds` (relation_kernel.clj:58-68; the set's own comment sanctions a
reviewed one-line add). This is the ONLY permitted `relation_kernel.clj` edit —
anything more = stop-clause → Sid.
- **Micro-gate** (its own deftest): an `assert-request` with EACH of the 3 new kinds
  (target-kind `:block`) is `:accepted` (today it rejects `:relation/kind-unregistered`
  at :330). Launch RK via `rk/start-relation-runtime!`; barrier = microbatch
  processed-count; read `$$relations-by-id` (or `rk/read-relation-detail`) → assert
  the row landed with the right kind.
- ⚠ This edits a CODE file (relation_kernel.clj) — it stays UNCOMMITTED with the rest
  until Sid's word, and it is a SEPARATE code commit from block_distiller.clj at commit
  time (code/docs separate; and consider rk vs distiller as separate code commits too).

## P3b — the mechanical edge floor (gates G8/G9)
The driver, over each river assistant event, pairs `(tool_use, tool_result)` and
emits the floor edges (SPEC §11.3): **write-tool → `produced`**, **read-tool →
`grounds`**, asserter `sense-block/mechanical@1`, target-kind `:block` (unit ids),
silver-by-asserter (N6). Deterministic idempotency keys (already in §H).
- **N3 — Sid-confirm BEFORE coding P3b (the one real fork):** G8 wants the edge's
  `to` endpoint to be target-kind `:block`, but SPEC §4.3 says tool_result is
  "surface only, NO pre-chunking." Recommended reading (PLAN §7/N3, orchestrator-
  leaned): mint ONE coarse whole-span block (form `tool-result-span`, span `[0,len)`)
  **on edge-demand** (SPEC §5.1 lazy-on-engagement, not eager pre-chunk) as the `:block`
  endpoint. Alt = a HOLE endpoint (SPEC §9, already covered by G9). **Surface this to
  Sid and get his ruling before implementing the tool_result endpoint** — it is the
  only P3 decision that is genuinely his.
- **N4:** `start-distiller-runtime!` currently launches OC only (P1 scope). P3b adds
  the RK runtime. Verify both modules launch on ONE `rtest/create-ipc` (git_spine_test
  dual-module precedent); fall to two IPCs if not.
- **⚠ verify-first (the P1 lesson):** the OC import path rejected actor-type `:machine`
  (valid OC set `#{:human :agent :system :bot}`). Before asserting edges, CONFIRM the
  RK path ACCEPTS `:asserter-type :machine` (grep relation_kernel.clj for an
  asserter-type validation set; assert-request :875 threads it into the actor). If RK
  rejects `:machine`, pick the RK-valid asserter type — do NOT assume.
- **Gates:** **G8** — write-tool→`produced`, read-tool→`grounds` in `$$relations-by-id`,
  asserter `sense-block/mechanical@1`, target-kind `:block`. **G9** — an edge request
  with an ABSENT endpoint persists + is queryable (holes first-class; dangling target
  legal, relation_kernel.clj:~284-285).
- Fixture note: the fixture's event 8 has a `tool_use(Edit)` (write-tool) paired with
  event 9's `tool_result(tu-1)` → a `produced` edge; add a read-tool pair (e.g. a
  `Read`/`Grep` tool_use + result) to the fixture if G8 needs a `grounds` case
  (regenerate golden if you touch the fixture — the golden regen `comment` is at the
  foot of the test ns).

## Stop clauses / hard rules
- Any `relation_kernel.clj` change BEYOND the 3 authorized kinds → Sid. Any
  non-additive `object_container.clj` change → Sid. N3 tool_result-endpoint = Sid-confirm.
- File allowlist: extend `block_distiller.clj` + test + fixtures; `relation_kernel.clj`
  ONLY the 3-kind add (P3a); `object_container.clj` additive-only. Everything else = stop.
- CODE uncommitted until Sid's word; code/docs separate commits; rk and distiller are
  separate code files (consider separate code commits). Docs auto-commit on the local
  branch, never pushed/merged. NEVER read `src/app/server/env.clj`.
- **Definition of P3 done:** P3a kinds registered + accept micro-gate green; P3b G8 +
  G9 green as IPC deftests (RK microbatch barrier); N3 endpoint ruled by Sid.

## At session end
Append a NOW entry (≤15 lines) to `docs/sessions/next-prompt.md`; on a stop-clause,
record it in `decisions.md` Open Questions (PROPOSED) with verbatim citations and STOP.
If P3 closes, write the P4 opening prompt (P4 = `refine!` G7 + `assemble!` G10, both
ride P3a's kinds; then P5 = river-page + real-file e2e + G12/G13, the package
definition of done).
