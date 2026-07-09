# Block-kernel P3 — fresh-context falsification (opening prompt)

**You are QC layer 4 — the fresh-context adversarial falsification** of Phase 3 of
the block-kernel work package. You did NOT write this code. Your job is to **break
it**, not confirm it. This is the layer the work-package skill requires precisely
because *"author self-review never substitutes for a fresh-context layer."*

**Default-fail:** a claim is "OK" only after you trace it, with file:line citations,
against the actual code + binding docs. Prefer reading + executing expressions
against the real structure over trusting any prose (including the docs').

## Do NOT read (stay independent)
`docs/current-mental-model/build/sense-line-mvp/block-kernel/P3_FALSIFICATION.md` —
that is the AUTHOR'S self-review (same context that wrote P3). Reading it anchors
you on its framing. Form your own view first; if you want to reconcile at the end,
do it last.

## Boot
1. Load `/rama` + `/rama-pitfalls` (P3b is a relation-kernel foreign-append surface).
2. The P3 diff is UNCOMMITTED in the working tree — `git diff` + inspect `??` files.
   The changes (all on branch `docs/current-mental-model-local`, code uncommitted):
   - `src/app/server/rama/relation_kernel.clj` — `:grounds :assembled-from :refines`
     added to `relation-kinds` (~:58-73). [P3a]
   - `src/app/server/rama/object_container/block_distiller.clj` — §H NEW pure fns
     (`tool-edge-kind`, `tool-index`, `hole-endpoint-id`, `mechanical-edge-plan`);
     NEW `event-ctx` + a REFACTOR of `event-import-request` to use it; §J NEW edge
     driver (`tool-result-part`, `coarse-block-import`, `mechanical-edge-request`,
     `assert-mechanical-edges!`); REFACTORED `distill-conversation!` (adds `:rk-rt`
     + pass 2) + `start-distiller-runtime!` (opts) + NEW `close-distiller-runtime!`. [P3b]
   - `test/app/server/rama/object_container/block_distiller_test.clj` — NEW deftests
     `p3a-relation-kinds-registered`, `mechanical-edge-plan-pure`,
     `mechanical-edge-gates`; requires `rk` + `rtest` added.
   - `test/resources/block-distiller/fixture.jsonl` — `a-1` gained `tool_use(Read tu-2)`
     + `tool_use(Write tu-3)`; `u-tr` gained `tool_result(tu-2)`. `golden.edn` regen'd.
3. Binding docs to check against: `block-kernel/CONTRACT.md` §8 (G8/G9), §4, §7 ·
   `SPEC.md` §11.1/§11.3, §9, §4.3, §5.1, §4.6 · `block-kernel/PLAN.md` §3.4, §7
   (N3/N4/N6/N7) · `relation_kernel.clj` (`->target-ref` :838, `assert-request`
   :875, `request-shape-errors` :309, the microbatch topology). Sid ruled **N3 =
   option A** (demand-mint a coarse `:tool-result-span` block as the `:block` endpoint).

## Hunt by CLASS (each → CONFIRMED / PLAUSIBLE / REFUTED, with a line-cited scenario)
1. **Coarse-block mint idempotence** — `coarse-block-import` builds a synthetic
   `endpoint-uuid` for a DISTINCT import-key. Does a re-run truly not duplicate the
   coarse block? Does re-declaring the tool_result surface converge (not corrupt)?
   Trace the OC import fingerprint/journal dedup — don't trust the test.
2. **The hole endpoint** — `hole-endpoint-id` → `du:<ok>:sense-block-v0:pending-
   result:<tuid>`. Does `extract-object-key` route it correctly? Is the G9 `nil`
   read PROOF of a hole, or could it be nil from MIS-routing (false-green)? Could a
   real block ever collide with the hole id?
3. **event-ctx refactor** — is `event-import-request` behavior byte-identical
   post-refactor? Any drift in event-id / created-at-ms / imp-key / request-id?
   (The golden diff claims only `a-1` +2 blocks — verify that's the WHOLE delta.)
4. **Microbatch barrier** — `wait-for-microbatch-processed-count` on `:edge-count`.
   Is submit==+1 guaranteed (routing-key always present, no ingress drop)? Is the
   cumulative count right across the G4 re-run (2×)? Can it return pre-materialization?
5. **Edge endpoints/direction vs SPEC §11.3** — from=tool_use, to=coarse-block/hole.
   Consistent with "write-tool → produced edge to the world artifact touched"? Are
   from/to genuinely target-kind `:block` with a `:target-key` (`->target-ref`)?
6. **Determinism / retry-safety** — any `System/currentTimeMillis` / `random-uuid`
   in edge or block id/time? Is `asserted-at-ms` replay-stable (production time)?
7. **Fixture/golden** — golden matches fixture exactly? Planted secret absent from
   EVERY row? New tool_use blocks honest spans? 2nd tool_result → right coarse block?
8. **Do the tests PROVE the gates or MASK via public API?** — G8/G9 read
   `$$relations-by-id` (V1) + `$$derived-units-by-id`. Physical reads, or could a
   public-API dedup/gate hide a bug? Is asserter/kind/status actually pinned?
9. **Two-IPC launch (N4)** — OC + RK on two IPCs; foreign-client-to-both. Ownership
   or resource hazard? Barrier targets the right cluster?
10. **Cross-package coordination** — the shared `relation-kinds` edit (block +3,
    code-atom +2, both uncommitted). Any collision, or a test pinning the exact set?

## Verify by running (don't take the author's counts)
- `clojure -M:test -e "(require '[clojure.test :as t] '[app.server.rama.object-container.block-distiller-test]) (t/run-tests 'app.server.rama.object-container.block-distiller-test)"` — author claims **15t/740a/0f**.
- Same for `app.server.rama.relation-kernel-test` (claims **2t/222a/0f**) and
  `app.server.rama.object-container-test` (claims **6t/149a/0f**).
- Report if your independent run disagrees. NEVER read `src/app/server/env.clj`.

## Output
Write your verdict to `docs/current-mental-model/build/sense-line-mvp/block-kernel/
P3_DIFF_FALSIFICATION.md` (CLAUDE.md review-protocol sections: Architecture; Failure
modes attempted with scenarios; Writers/readers/clearers per changed state; Async
ordering; Error-path cleanup; Open doubts). Each finding: **CONFIRMED** (traced to a
concrete input→wrong-output) or **PLAUSIBLE** (suspected, unproven), file:line +
scenario, most-severe first. An empty findings list is a valid honest result — do
NOT invent findings to look thorough. Append a NOW entry to `next-prompt.md`; if
CONFIRMED blockers exist, they gate P4 (fix default-fail, re-green, re-run).
