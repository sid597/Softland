# Opening prompts — framework Wave 1, three parallel lanes

**GATED: dispatch only after `build/framework/CONTRACT.md` v1 exists.** The contract session pins every ⟨§?⟩ below to a real section number — until then these are drafts, not dispatchable.
The three lanes run **concurrently** (three fresh sessions or three subagent contexts) — their file fences are disjoint by construction. Per the 2026-07-05 delivery-mode ruling there is NO per-lane falsification: lanes run their own contract gates to green in-context; the wave ends with W1-INT (integration) → one serial suite → one batched falsification-by-class → one Fable gate.

Model per lane: Opus 4.8 (Codex 5.5 fast-mode is a valid alternative per the 2026-07-06 pool ruling).

## Common boot (all lanes, in this order)

1. `build/framework/CONTRACT.md` — BINDING. On any conflict with other docs, the contract and `decisions.md` win.
2. `build/framework/NOW.md` — STANDING (precedence rule, fences, hard rules) + latest NOW entries.
3. `docs/current-mental-model/build/framework/ROAD.md` — direction context, not binding.
4. `build/framework/PROBE.md` — the Step-0 numbers + the probe code (uncommitted in tree, or described there): harvest or discard per your lane's judgment, record which.
5. Lane-specific sources listed below.

Common hard rules: new files only unless your lane's fence says otherwise · never read `src/app/server/env.clj` · code and docs in SEPARATE commits, docs only on the local docs branch (never pushed/merged) · dev app is `clj -A:dev -X dev/-main`, never standalone shadow-cljs · gates green in-context is your definition of done · stop clause per the /work-package skill: a genuine binding-doc conflict is escalated to `decisions.md` Open Questions with options + recommendation, NEVER improvised · exit = ≤15-line NOW entry (findings verbatim on any FAIL) + your phase artifact in `build/framework/`.

---

## Lane W1-A — the interpreter + golden harness

**Builds:** the real two-stage interpreter per CONTRACT ⟨§?⟩: `compile-assembly` (EDN → validated closure; malformed → error-card rt-node, never a throw) + `apply-assembly` (closure × data-context → rt-node tree), carrying `(view-instance, address)` per ⟨§?⟩; the golden harness (JVM: assembly + fixture → golden rt-tree snapshot, the block-kernel goldens discipline); the Outline face as fixture EDN.
**Sources:** `rect_tree.cljc` · the probe walker (harvest candidate).
**Fence:** new files only — suggested `src/app/client/workspace/face_assembly.cljc` + test ns + `test/.../fixtures/`. Test against 2–3 locally-defined stub primitives implementing the contract's builder-fn interface ⟨§?⟩ — do NOT wait on lane B's vocabulary; the interface is fixed by the contract, so goldens written against stubs stay valid.
**Gates:** contract gates ⟨§?⟩ green in-context, incl. validation/error-card cases and the grammar's arrangement-only guard (a logic-shaped assembly must be REJECTED, with a test proving it).

## Lane W1-B — the vocabulary

**Builds:** the primitive vocabulary per CONTRACT ⟨§?⟩ in a new ns (suggested `src/app/client/workspace/face_primitives.cljc`): extraction/wrapping of existing `ui_primitives.cljs` + `trail_face/` builders (cards/lanes/threads are proto-primitives) into the contract's builder-fn interface, plus the two genuinely new primitives: **wrapped text-run** (prose blocks via `wrap-line`) and **indent-rail**.
**Sources:** `ui_primitives.cljs` · `trail_face/cards.cljc` · `rect_tree.cljc`.
**Fence:** new files only; `ui_primitives.cljs`/`trail_face/` are READ-ONLY (extraction copies/wraps into the new ns — existing callers keep working untouched).
**Gates:** contract gates ⟨§?⟩ green in-context: builder-level structural-equivalence tests (call your wrapped builders and the originals with identical inputs; rt-trees structurally equal — this is the fixture-fidelity discipline at builder grain). The assembly-level sidebar falsifier runs at W1-INT, not here.

## Lane W1-C — the projection + the artery

**MANDATORY skill boots before any code: `/rama` and `/electric-docs`.**
**Builds:** (1) the conversation projection per CONTRACT ⟨§?⟩ — server-side, READ-ONLY over the block-kernel's existing PStates (conversation → turns → blocks-with-kinds; bounded replay read = the scrub source): **no new depots, no new topologies, no kernel edits** (gate it, trail-view gate-14 style); (2) the one generic face-pull in `electric_flow.cljc` per ⟨§?⟩ — request atom (face + address + params) → Electric pull → data-context atom, the R4 mirror-atom pattern; S1/S2 seam law holds (the pull lives in electric_flow, the glue touches no server names).
**Sources:** `build/sense-line-mvp/block-kernel/CONTRACT.md` + `block_distiller.clj` (read shapes) · `electric_flow.cljc` (the trail-face request/pull loop is the before-picture) · `trail_face/wiring.cljs`.
**Fence:** new server ns + **sole ownership of `electric_flow.cljc` for the wave** (no other lane touches it).
**Gates:** contract gates ⟨§?⟩ green in-context: JVM projection tests over real ingested material (the `7c80ce2a` receipt conversation is the fixture corpus — full-real-corpus receipt discipline applies: assert completeness, don't just print counts).

---

## W1-INT — integration step (after all three lanes; orchestrating session, small)

Wire the real registry map (A's interpreter + B's vocabulary), mount the first assembly-hosted pane through `<world-snapshot`, connect C's data-context atom, run: (1) the **sidebar structural-equality falsifier** (assembly → interpreter → rt-tree ≡ existing sidebar builder output), (2) the Outline face over the real projection in the dev app (screenshot for Sid), (3) the full serial suite. Then the wave's ONE batched falsification-by-class (fresh Opus subagents) → Fable gate → code commits per current package practice → Step-2 exit: a real past conversation rendered through the Outline face, replay/scrub working.
