# framework — thread file (faces-as-assemblies)

## STANDING (v0, frozen at package staging 2026-07-11 — CONTRACT.md supersedes details when it lands)

- **Binding:** `docs/current-mental-model/decisions.md` + (pending) `build/framework/CONTRACT.md`. `ROAD.md` v2 is direction-grade input, NOT binding.
- **Precedence rule:** this file is a baton, not a source of truth; if it contradicts CONTRACT.md or decisions.md, those win — flag the discrepancy in NOW, don't pause.
- **Process:** the `/work-package` skill governs (one phase per fresh CONTEXT; validation default-fail; never overwrite a FAIL artifact; ≤15-line NOW entries, FAIL findings verbatim exempt).
- **Scope guard (pre-contract):** new files only; no edits to `rect_tree.cljc` / `ui_primitives.cljs`; `electric_flow.cljc` owned solely by lane W1-C once the wave opens; probe code stays UNCOMMITTED (evidence, not product).
- **Hard rules:** never read `src/app/server/env.clj`; code and docs in separate commits; docs only on the local docs branch (never pushed/merged); relation-kind additions require explicit authorization (closed enum, D-004 — block-kernel precedent).
- **NOT without Sid:** wave dispatch (spend); countersign of the two decision-log entries (middle regime; self-hosting test formulation); relation-kind additions.
- **Prompts:** probe → `docs/sessions/framework-probe-opening-prompt-2026-07-11.md` (dispatchable NOW, ∥ contract) · contract → `framework-contract-opening-prompt-2026-07-11.md` (Fable) · W1 lanes → `framework-w1-lane-prompts-2026-07-11.md` (GATED on CONTRACT v1; ⟨§?⟩ refs pinned by the contract session).

## NOW (≤15 lines per entry; newest last)

- 2026-07-11 · Fable (`3-way-design-framework-converge`) · **package STAGED.** ROAD v2 committed (`6febdb1`) after ingesting two independent source-reviews — mechanism re-grounded (pure-fn primitives over rect-tree; Electric = bridge only; U4/U5 off the critical path). Execution map drawn: Fable line = contract · probe-brief · wave gates · log-entry drafts; two waves × three parallel lanes; machine-cut timing rides W2 (D-005), live tailer waits on its pre-registered form-break (2026-07-06 gate-raised default). All opening prompts written (see STANDING). Next: dispatch probe ∥ Fable writes CONTRACT; W1 dispatches only after CONTRACT v1 pins the lane prompts. No code exists; nothing on main.
