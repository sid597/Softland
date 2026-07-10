# Opening prompts — framework Wave 1, three parallel lanes

**PINNED against `build/framework/CONTRACT.md` v1 (2026-07-11)** — every § reference below is real. Lanes are dispatchable on Sid's wave word (spend stop, CONTRACT §14).
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

**Builds:** the real two-stage interpreter per CONTRACT §5 over grammar v0 per §4: `compile-assembly` (EDN → validated closure; malformed → error-card rt-node, never a throw — §4 error-card semantics, registry-independent constructor) + `apply-assembly` (closure × data-context × view-ctx → rt-node tree), carrying `(view-instance, address)` + the apply-report per §5 (Δ1 carry, trap T10); the golden harness (JVM: assembly + fixture → golden rt-tree snapshot, the block-kernel goldens discipline); the Outline face as fixture EDN.
**Sources:** `rect_tree.cljc` · CONTRACT §4–§6, §9 (cite trap numbers in code comments) · the probe walker (harvest candidate).
**Fence:** new files only — `src/app/client/workspace/face_assembly.cljc` + `test/app/face_assembly_test.clj` + `test/app/fixtures/faces/` (CONTRACT §2). Test against 2–3 locally-defined stub primitives implementing the contract's builder-fn interface (§6, fixed: `(fn [ctx props children] → rt-node)`) — do NOT wait on lane B's vocabulary; the interface is fixed by the contract, so goldens written against stubs stay valid.
**Gates:** CONTRACT §11 **G1–G6** green in-context — golden walk, the guard's five rejection classes, error-card totality (incl. the corrupted-registry case), two-stage purity + Δ1 carry, `:each` id discipline, `:bind` discipline.

## Lane W1-B — the vocabulary

**Builds:** the primitive vocabulary per CONTRACT §6 in `src/app/client/workspace/face_primitives.cljc`: verbatim-copy extraction of the named `ui_primitives.cljs` builders + the `trail_face/` cljc builders the Outline face needs (cards/lanes are proto-primitives) into the §6 builder-fn interface, plus `:stack` and the two genuinely new primitives: **`:text-run`** (prose blocks; wraps AND sets its own `:h` — the §6 measure rule / trap T7) and **`:indent-rail`**. The registry map is assembled here as a plain value.
**Sources:** `ui_primitives.cljs` · `trail_face/cards.cljc` · `rect_tree.cljc` · CONTRACT §6, §9 (cite trap numbers in code comments).
**Fence:** new files only; `ui_primitives.cljs`/`trail_face/` are READ-ONLY (extraction copies verbatim into the new ns — existing callers keep working untouched; the copy-then-harmonize ruling is CONTRACT §2).
**Gates:** CONTRACT §11 **G7–G9** green in-context: G7 mechanical builder fidelity (source-form DIFF test against the originals' defn forms as data — NOT call-and-compare; the originals are cljs and the suite is JVM; allowlist starts empty), G8 the two new primitives' measure goldens, G9 JVM purity. The assembly-level worn-UI falsifier (G14) runs at W1-INT, not here.

## Lane W1-C — the projection + the artery

**MANDATORY skill boots before any code: `/rama` and `/electric-docs`.**
**Builds:** (1) the conversation projection per CONTRACT §7 — `src/app/server/rama/face_projection.clj`, a projection REGISTRY (plain map, one `:conversation` entry) READ-ONLY over the block-kernel's existing query APIs (conversation → turns → blocks-with-kinds; `:until-ms` bounded replay read = the scrub source): **no new depots, no new topologies, no kernel edits** (G12, trail-view gate-14 style; trap T12); (2) the one generic face-pull per §7 — `FacePull` e/defn in `file_viewer.cljc` + the request-watch loop in `electric_flow.cljc` — request atom `{:face :address :params :epoch}` → Electric pull → ONE data-context atom, the R4 mirror-atom pattern + the INV-19 epoch-debounce re-pull, via new `face_wiring.cljs`; S1/S2 seam law holds (the pull lives in electric_flow/file_viewer, the glue touches no server names); NO face-keyword dispatch in Electric (trap T8).
**Sources:** CONTRACT §7, §9 · `build/sense-line-mvp/block-kernel/CONTRACT.md` + `block_distiller.clj` (read shapes; `river-page` :1305) · `electric_flow.cljc` :492-508 (the trail-face request/pull loop is the before-picture) · `trail_face/wiring.cljs`.
**Fence:** new server ns + `face_wiring.cljs` + **sole ownership of `electric_flow.cljc` AND `file_viewer.cljc` for the wave** (no other lane touches them).
**Gates:** CONTRACT §11 **G10–G13** green in-context: G10 full-real-corpus receipt (the `7c80ce2a` conversation; ASSERT completeness against durable state — 247-river baseline — never just print counts), G11 bounded prefix-consistent scrub, G12 read-only by construction, G13 generic-artery review + the projection-registry unit test.

---

## W1-INT — integration step (after all three lanes; orchestrating session, small)

Wire the real registry map (A's interpreter + B's vocabulary), mount the first assembly-hosted pane through `<world-snapshot` per CONTRACT §5's flow discipline and touch-list (`runtime/state.cljs` · `editor_compute.cljs` mode + flow · `combined_text.cljs` · `runtime/mouse.cljs` · `runtime/scroll.cljs` · `runtime/workspace_actions.cljs` command · `runtime.cljs` wiring install — orchestrating session only, CONTRACT §2 fences), connect C's data-context atom, then run: (1) **G14, both instances** — (a) the JVM trail-face-slice structural-equality falsifier into the suite, (b) the live sidebar equality check in the dev app, result verbatim in the INT artifact; (2) **G15** — the Outline face over the real projection in the dev app, bottom-bar command picks the conversation, `:until-ms` scrub works, screenshot for Sid, every exposed lack logged as ordered data work (D-005); (3) the full serial suite + §10 SLOT numbers recorded (apply-cost, wrap cost, scroll convention). Then **G16**: the wave's ONE batched falsification-by-class (fresh Opus subagents) → Fable gate → code commits per current package practice → Step-2 exit: a real past conversation rendered through the Outline face, replay/scrub working.
