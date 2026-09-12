# Block-distiller Phase 4 — opening prompt (fresh session)

**You are the P4 implementation session** of the block-kernel work package
(`docs/current-mental-model/build/sense-line-mvp/block-kernel/`). P0–P3 are DONE
and GREEN. P4 has two sub-phases: **P4a** (`refine!` → gate G7) then **P4b**
(`assemble!` → gate G10). Both ride the relation kinds P3a registered
(`:refines`, `:assembled-from`) — no new registry edit is authorized.

## Boot, in order
1. **Load skills:** `/work-package`, `/rama`, **`/rama-pitfalls`** (P4 adds two
   more RELATION-kernel foreign-append surfaces + new OC imports — run pitfalls on
   the refine/assemble append + mint design before coding).
2. **Binding docs** (`decisions.md` › `SPEC.md` › `CONTRACT.md` › `PLAN.md`):
   SPEC §5 (refinement — the demand law: adds never invalidates; engagement IS the
   provenance), §6.1 (identity by `(surface,span)` — resolve, don't duplicate),
   §8 (composition — assembly is a production event; transclude by default =
   re-address, don't copy; occurrences), §9 (holes), §11 (edges); CONTRACT §4
   (the `occurrence/assembly` + `mechanical edges` rows) / §8 (gates G7, G10) /
   §9 (stop clauses — **new row TYPE = T14 stop-clause → Sid**); PLAN §2 (the
   `refine!`/`assemble!` signatures), §5 (Phase 4).
3. **The P0–P3 code you build on** (`block_distiller.clj`): §J already has the
   full RK edge-assert path — `mechanical-edge-request` (asserter
   `sense-block/mechanical@1`, `:asserter-type :machine`, relation-scoped
   idempotency), `mechanical-edge-plan`, `assert-mechanical-edges!` — plus the OC
   demand-mint pattern `coarse-block-import` (mint a unit+anchor over a surface),
   `event-ctx`, `part-rows`, `import-request`, `import-payload`,
   `start-distiller-runtime! {:relations? true}` + `close-distiller-runtime!`.

## State you INHERIT (verified this session — do NOT re-derive)
- **P3 green: block_distiller_test 15t/740a; +object_container 6t/149a; relation_kernel
  2t/222a, 0 fail.** All three suites independently re-run.
- **The 3 kinds are registered** (`relation_kernel.clj:58-73`): `:grounds
  :assembled-from :refines` — P4 needs `:refines` + `:assembled-from`; NO further
  registry edit is authorized (anything more = stop-clause → Sid).
- **RK accepts `:asserter-type :machine`** (no allowlist — verified by the P3a
  micro-gate + G8). **RK is MICROBATCH**: barrier =
  `rtest/wait-for-microbatch-processed-count ipc module "relation-kernel-topology"
  <cumulative-N>`; NOT `:append-ack`. Refs via `rk/->target-ref :block <unit-id>`
  (N7); NO tier field — "silver" rides the asserter (N6, T14).
- **N4 = two IPCs**: `start-distiller-runtime! {:relations? true}` launches OC + RK
  on their own IPCs; the driver is a foreign client to both (works — the P3b
  `mechanical-edge-gates` deftest proves it).
- **The demand-mint pattern** (`coarse-block-import`): re-declare the surface
  (idempotent, same source-id) + add a unit + anchor, with a DISTINCT import-key
  so the OC journal does not dedup it as a prior import's replay. `refine!` reuses
  this shape (a finer unit over the SAME surface, sub-span); `assemble!` mints a
  NEW surface.

## P4a — `refine!` (gate G7)
Demand-mint a FINER block inside an existing coarse block (SPEC §5.1). Signature
(PLAN §2): `(refine! {:oc-rt :rk-rt :coarse-unit-id :sub-span :engagement …}) →
finer-unit-id`.
- The finer unit is a NEW `DerivedUnitRow` over the SAME surface, span = the
  sub-span (UTF-16, in-bounds within the coarse block; child ⊂ parent, G11);
  deterministic id (identity by `(surface, sub-span)` — SPEC §6.1: a second
  refine of the same sub-span RESOLVES to the same id, never duplicates).
- A `:refines` edge finer→coarse, asserter `sense-block/mechanical@1`, target-kind
  `:block` (both endpoints real). **The engagement is the refinement's provenance
  (SPEC §5.1 MUST)** — decide the home (an additive field on the unit / the edge's
  `:note` / an engagement id on the request); prefer the edge/`:note` (no new row).
- **G7**: physical — the finer unit present in `$$derived-units-by-id`, the coarse
  unit UNCHANGED (§5.2 adds-never-invalidates), the `:refines` edge in
  `$$relations-by-id`. If the coarse block carries a mark (relation), that mark
  persists too (exercise minimally).

## P4b — `assemble!` (gate G10)
Assembly IS a production event (SPEC §8.1): inputs = blocks, output = a NEW
surface (form `assembled`), with an `:assembled-from` edge to EVERY source block.
Signature (PLAN §2): `(assemble! {:oc-rt :rk-rt :block-ids …}) →
assembled-source-id`.
- Mint a NEW `SourceArtifactRow` (form/`source-format` for `assembled`; its own
  object-key or the conversation's — decide + cite) whose text is the assembled
  material; NO copy of the source blocks — they are RE-ADDRESSED (SPEC §8.2
  transclude-by-default).
- One `:assembled-from` edge per source block (assembled-unit → source-block,
  target-kind `:block`), asserter `sense-block/mechanical@1`.
- **⚠ The real P4 design question — occurrences (SPEC §8.3):** "re-addressed, not
  copied" = the `:assembled-from` edge IS the occurrence record, OR does §8.3's
  addressable `(block-id, assembly-surface, position)` occurrence need its own
  row? A new row TYPE = **T14 stop-clause → Sid**. Recommended reading: the
  `:assembled-from` edge (carrying position in `:note`/order) satisfies G10's
  "re-addressed (not copied)" for v0; a first-class occurrence row is a marks-layer
  extension (CONTRACT §10). Confirm with Sid ONLY if G10 cannot be met additively.
- **G10**: physical — the new `assembled` surface in `$$source-artifacts-by-id`;
  one `:assembled-from` edge per source block in `$$relations-by-id`; the source
  blocks' rows UNCHANGED (re-addressed, not copied — assert their
  `derived-content-text` is untouched).

## Stop clauses / hard rules
- Any relation_kernel.clj change (the 3 kinds are ALREADY registered — need
  nothing more) → stop-clause → Sid. Any NON-additive object_container.clj change,
  or a new row TYPE / `:projection-kind` (T14, e.g. an occurrence row) → Sid.
- File allowlist: extend `block_distiller.clj` + `block_distiller_test.clj` +
  fixtures only. Everything else = stop.
- CODE uncommitted until Sid's word; code/docs separate commits; docs auto-commit
  local branch only. NEVER read `src/app/server/env.clj`.
- ⚠ COORDINATION (still live): `relation-kinds` carries BOTH this package's
  `:grounds :assembled-from :refines` AND the code-atom package's `:requires
  :calls` (uncommitted, one tree). A code-atom fixwave is editing `code_atoms.clj`.
  At commit, confirm BOTH registry adds survive; re-run both packages' gates. A
  queued block P1/P2 fixpass (F1/F2/F3, `P1P2_FALSIFICATION.md`) also edits
  `block_distiller.clj` — coordinate so it rebases on P3/P4, and re-run
  `block_distiller_test` after.
- **Definition of P4 done:** G7 + G10 green as IPC deftests (RK microbatch barrier
  + physical OC/RK readers). Keep the P3 `mechanical-edge-gates` deftest green.

## At session end
Append a NOW entry (≤15 lines) to `docs/sessions/next-prompt.md`; on a stop-clause,
record it in `decisions.md` Open Questions (PROPOSED) with verbatim citations and
STOP. If P4 closes, write the P5 opening prompt (P5 = `river-page` via query
topologies + the real-file e2e over `7c80ce2a` + review-time G12/G13 — the package
definition of done, CONTRACT §8).
