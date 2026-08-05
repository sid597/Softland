# `src/` code-size + verbosity audit

Read-only audit. No source file was modified. Decision-support for Sid, not a refactor.
Method: full line inventory + comment/blank/code metrics + deep read of the top 20 files (four parallel readers) + spot-verification of the highest-value claims. Every quantitative claim carries a `file:line` cite.

## 1. Executive summary (10 lines)

1. **Total `src/` = 47,000 lines across 83 `.clj/.cljs/.cljc` files** — **45,518 across 80** once the 1,482 lines of uncommitted probe code (§4) are set aside (`find src -name '*.clj*' -exec cat {} + | wc -l`, as-of this session; the tree also holds a few uncommitted block-write files, hence counts drift slightly).
2. Half the tree is one subtree: **`src/app/server/rama` = 23,438 lines (~50%)**; `client/workspace` = 13,344 (~28%).
3. The codebase is **mostly tight** — big files are big because the jobs are big. There is no dead-commented-code problem (no file has a meaningful commented-out-code block).
4. **Biggest win #1 — relocate `kernel.clj` (830 lines) out of `src/`.** It is a 478-line prose header + one inert `KERNEL-SHAPE` data map that self-describes "does not generate code. Nothing calls it" (`kernel.clj:304`). It is documentation living on the compile path.
5. **Biggest win #2 — `object_container.clj` structural de-dup (~55 safe + ~130 gated).** A 95-line literal fingerprint map (`object_container.clj:1554-1648`) and 12 near-identical dataflow write-loops.
6. **Biggest win #3 — `server_jetty.clj` POST-route boilerplate (~70 lines).** 15 hand-repeated method-guard + try/catch/log arms (`server_jetty.clj:999-1263`).
7. **Safe mechanical shrink total ≈ 440 lines** (in-file de-dup across the 20 files + ~55 cross-file Rama helper hoist + one 13-line dead branch). That is ~1% of `src/` — the tree is not fat.
8. **Gated structural shrink ≈ 130 lines** (object_container write-loops) — needs a hand-written Rama DSL macro + regression run; not safe blind.
9. **Relocatable (out of `src/`, not deleted) ≈ 863 lines** — `kernel.clj` (830) + `relation_kernel.clj` V1 test-only readers (33).
10. **Uncommitted probe scaffolding ≈ 1,489 lines is delete-to-remove** and is reported separately (§4), NOT folded into any shrink number above.

## 2. Ranked top-20 files

Ranked by size. `island_probe.cljs` (857) is excluded — it is probe code (§4). Savings are estimates; confidence = whether the shrink is semantics-preserving (H) vs needs review (M/L).

| # | File (`src/…`) | Lines | Job (≤6 words) | Dominant bloat pattern | One concrete shrink candidate | Est. |
|---|---|--:|---|---|---|--:|
| 1 | `app/server/rama/object_container.clj` | 2685 | Object-container kernel: ingest/edit/import | 12 repeated dataflow write-loops + literal fingerprint map | Data-drive `import-material-fingerprint` literal `object_container.clj:1554-1648` (M) | ~55 |
| 2 | `app/server/rama/dogfood/llm.clj` | 2442 | LLM turn-run kernel | status-promotion block copied ×5 | `promote-to-running` helper `llm.clj:988-1066` (H) | ~10 |
| 3 | `app/server/rama/dogfood/space.clj` | 2177 | Space (chat-as-place) kernel | topology fan-out repeats object/edge/projection writes | Collapse 3 `raw-llm-source?` blocks `space.clj:1820-1902` (M) | ~36 |
| 4 | `app/client/substrate/webgpu/renderer.cljs` | 1741 | WebGPU pipeline + draw loop | 4-line draw-call block repeated ×13 | `draw-pass!` helper for `renderer.cljs:1611-1728` (M) | ~30 |
| 5 | `app/server/rama/dogfood/transcript.clj` | 1522 | Transcript capture kernel | dual OC/plain watch-daemon scaffolding | `spawn-watch-thread!` for `transcript.clj:1313-1522` (M) | ~35 |
| 6 | `app/server/rama/object_container/block_distiller.clj` | 1422 | Distill transcripts → sense-blocks | design-rationale docstrings (load-bearing, keep) | Merge edge builders `block_distiller.clj:881-996` (M) | ~12 |
| 7 | `app/client/workspace/trail.cljs` | 1337 | Markdown parse → chat nodes | list-item render loop copied ×5 | Unify callout list branches `trail.cljs:804-861` (M) | ~40 |
| 8 | `app/server_jetty.clj` | 1335 | Ring/Jetty HTTP server + routing | 15 repeated POST method-guard/try/catch arms | `with-post-json` helper `server_jetty.clj:999-1263` (M) | ~70 |
| 9 | `app/client/workflows/dg_flow.cljs` | 1140 | DG workflow state machine + scenes | repeated text-op literals + a dead branch | Delete dead `:run-parallel` branch `dg_flow.cljs:943-955` (M) | ~13 |
| 10 | `app/server/rama/dogfood/compute.clj` | 1106 | Compute kernel — run OS commands | **already tight — no in-file dup** | none in-file (cross-file `await-materialized` only) (H) | ~0 |
| 11 | `app/server/rama/dogfood/transcript_ingest.clj` | 1019 | Transcript → object-container ingest | accessor wall + 3rd watch-scaffold copy | Reuse `transcript` redactor, drop `transcript_ingest.clj:99-116` (M) | ~18 |
| 12 | `app/server/rama/relation_kernel.clj` | 1011 | Typed RelationEdge kernel | 22% comments — **load-bearing, KEEP** | Relocate V1 test-only readers `relation_kernel.clj:966-998` (M) | ~0¹ |
| 13 | `app/server/rama/machine_cut.clj` | 970 | LLM pair-structure annotator | comments load-bearing (keep) | Dedup in-scope edge read `machine_cut.clj:796-798`+`908-911` (H) | ~6 |
| 14 | `app/client/workspace/face_primitives.cljc` | 959 | §6 primitive-builder vocabulary | **~340 lines PINNED copies — do NOT touch (§5)** | `wrap-and-emit-ops` helper (non-pinned) `face_primitives.cljc:701-756` (M) | ~12 |
| 15 | `app/server/rama/code_atoms.clj` | 919 | Code-atom driver (git ingest/lineage) | load-bearing F/T-trap docstrings | `retract!` helper `code_atoms.clj:819-836` (M) | ~8 |
| 16 | `app/server/rama/kernel.clj` | 830 | Kernel-shape SPEC + inert data map | **478-line prose header + inert map nothing calls** | Relocate whole file to `docs/` `kernel.clj:1-830` (M, judgment) | ~830² |
| 17 | `app/server/rama/trail_view.clj` | 824 | Read-only trail-view queries | comments load-bearing (keep) | Unify display-name fns `trail_view.clj:239-274` (M) | ~8 |
| 18 | `app/server/rama/core.clj` | 811 | Request/event envelope + validation | `(map? request)` guard repeated ×13 | Early-return in validators `core.clj:225-303` (M) | ~10 |
| 19 | `app/client/workspace/trail_face/scene.cljc` | 799 | Trail-face scene assembly | big job, tight; load-bearing comments | `kraft-label-child` helper `scene.cljc:260-306` (M) | ~8 |
| 20 | `app/server/rama/text_kernel.clj` | 733 | Text-artifact kernel | parallel request/event builders | `unit-status-fields` helper `text_kernel.clj:92-177` (M) | ~12 |

¹ `relation_kernel.clj` in-place safe deletion ≈ 0 — everything is wired or load-bearing. The 33-line V1 readers are relocatable to a test ns (net repo lines unchanged; removes them from the production kernel).
² `kernel.clj` is a **relocation**, not a deletion — it moves 830 lines off the compile path into `docs/`, needs Sid's judgment (it is intentional living documentation).

### Cross-file de-dup (spans the top-20, not a single row)
Verified literal copies in the dogfood/text kernels — hoist to `app.server.rama.core`:
- **`await-materialized` — 5 verbatim copies:** `text_kernel.clj:624`, `llm.clj:2415`, `space.clj:2154`, `transcript.clj:883`, `compute.clj:877`.
- **`select-pstate-one` — 4 verbatim copies:** `llm.clj:2305`, `space.clj:2028`, `transcript.clj:856`, `compute.clj:854`.
- **`foreign-pstate` runtime map duplicated:** `space.clj:1981-2000` re-declares ~20 entries identical to `llm.clj:1726-1747`.
- Hoisting these removes **~55 lines** across the module set at high confidence (identical bodies), but the edit spans files.

## 3. Two lists

### Safe quick wins (mechanical, low-risk, in-file, H/M confidence)
- **`server_jetty.clj:999-1263`** — factor `with-post-json`; ~70 lines. (M — error-body shapes vary per arm; helper must thread each tag.) *Largest single safe win.*
- **`trail.cljs:804-861`** — unify the two callout list branches with the top-level ones; ~40 lines. (M — preserve pixel offsets + id prefixes.)
- **`space.clj:1820-1902`** — collapse 3 identical `raw-llm-source?` edge blocks; ~36 lines. (M — Rama subgraph extraction.)
- **`transcript.clj:1313-1522`** — shared `spawn-watch-thread!` scaffolding; ~35 lines. (M — poll bodies differ.)
- **`renderer.cljs:1611-1728`** — `draw-pass!` helper over 13 draw sites; ~30 lines. (M — hot path, per-site offsets.)
- **Cross-file Rama hoist** (`await-materialized` ×5, `select-pstate-one` ×4, foreign-pstate map) → `core`; ~55 lines. (H bodies, but multi-file.)
- **`transcript_ingest.clj:99-116`** — reuse `transcript`'s redactor; ~18 lines. (M — pattern sets close but not identical; verify redaction unchanged.)
- Small pure-mechanical de-dups: `text_kernel.clj:92-177` ~12 · `block_distiller.clj:881-996` ~12 · `face_primitives.cljc:701-756` ~12 · `llm.clj:988-1066` ~10 · `code_atoms.clj:819-836` ~8 · `trail_view.clj:239-274` ~8 · `scene.cljc:260-306` ~8 · `machine_cut.clj:796-798`+`908-911` ~6.

### Needs judgment (semantic / relocation / Sid or review)
- **`kernel.clj:1-830` — relocate to `docs/`.** Inert spec (self-described `kernel.clj:304-305`: does not generate code, nothing calls it). Biggest line mover, but it is deliberate living documentation — Sid decides whether it belongs in `src/` or `docs/`.
- **`object_container.clj` write-loops (~130).** 12 near-identical dataflow loops (at `object_container.clj:1880,1940,1962,1974,2008,2029,2062,2090,2132,2165,2246,2636`). Real bloat, but collapsing needs a hand-written Rama `write-each>` macro — **not safe without re-running the pinned-build regression tests.**
- **`object_container.clj:1554-1648`** — data-drive the fingerprint literal; ~55. (M — the hash is load-bearing for import idempotency at `object_container.clj:1831`; any key drift silently breaks dedup — needs an exact reproduction test.)
- **`dg_flow.cljs:943-955`** — `:run-parallel` branch is **unreferenced in `src/`** (only its own key at `dg_flow.cljs:943`; `fire-flow-run!` is called only with `:run-sequential`/`:rework`/`:finalize` at `dg_flow.cljs:275,306,327`). ~13 lines. But `flow-prompt` is a public fn — confirm no external/dynamic caller before deleting.
- **`core.clj:225-303`** — early-return rewrite of the request/event validators; ~10. (M — control-flow change on a validation hot path; behavior is preserved but warrants a careful read.)
- **`relation_kernel.clj:966-998`** — relocate V1 test-only PState readers to a test-support ns; net repo lines unchanged.

**Do NOT chase:** the comment mass in `relation_kernel.clj` (22%), `machine_cut.clj` (13%), `trail_view.clj` (12%), `code_atoms.clj`, `block_distiller.clj`, `scene.cljc` — those comments are load-bearing design rationale (CONTRACT clauses, named F/T/G trap fixes, byte-vs-char semantics). `compute.clj` is the tightest kernel and has **no** in-file shrink (~0).

## 4. Probe code (delete-to-remove — separate from all shrink numbers above)

Uncommitted islands-probe + echo-probe scaffolding. Delete to remove; not part of §2/§3.

| Item | Lines | Git state |
|---|--:|---|
| `src/app/client/substrate/webgpu/island_probe.cljs` | 857 | untracked (`??`) |
| `src/app/probe/stream_echo_probe.clj` | 385 | untracked (`??`) |
| `src/app/probe/write_echo_probe.clj` | 240 | untracked (`??`) |
| `src/app/client/workspace/runtime/render.cljs` — 5 tagged mount points (`islands-probe 2026-07-11`) | +7 / −1 | modified (`M`) |

- **Standalone files: 1,482 lines** (857 + 385 + 240). `git status --short src/` confirms all untracked.
- **`render.cljs` mount points** (revert via `git checkout src/app/client/workspace/runtime/render.cljs`): require `render.cljs:5`; force-redraw guard `render.cljs:126` (`(not (island/driving?))`); composite comment+call `render.cljs:491-492` (`island/step!`); window-api install `render.cljs:538` (`island/install-window-api!`). Git diff = **+7 / −1**.
- **Total probe delete-to-remove ≈ 1,489 lines** (1,482 standalone + ~7 in render.cljs).

## 5. Pinned duplication — do NOT count as removable

`face_primitives.cljc` contains **intentional byte-for-form source-copies** — ~340 lines, `face_primitives.cljc:44-397`:
- support defs `list-row-h`…`list-footer-h`, `dt`, `typo-title`…`typo-caption` (`face_primitives.cljc:44-62`);
- 11 named ui builders `ui-card`/`ui-badge`/`ui-divider`/`ui-scrollbar`/`build-empty-state`/`ui-panel`/`ui-panel-header`/`ui-panel-content`/`ui-panel-footer`/`ui-panel-group`/`ui-list-item` (`face_primitives.cljc:69-329`);
- 3 trail proto-primitives `omission-line`/`omissions-block`/`hole-endpoint-card` (`face_primitives.cljc:340-397`).

**Pinning mechanism:** `test/app/face_primitives_test.clj` — `deftest g7-source-form-fidelity` (`face_primitives_test.clj:98-115`) reads `face_primitives.cljc`, `ui_primitives.cljs`, and `trail_face/cards.cljc` as DATA and asserts every `def/defn/defn-` symbol shared between them is **byte-for-form identical to its origin**; `deftest g7-named-builders-present` (`face_primitives_test.clj:89-96`) asserts all 14 named builders are present. The reviewed-divergence **allowlist starts EMPTY** (`face_primitives_test.clj:87`). These copies are deliberate and are NOT duplication wins — the only shrink candidate for this file (§2 row 14) is in the NON-copied `text-clip-prim`/`text-run-prim` code.

---
*Note (`0.56` char-advance):* CLAUDE.md marks `0.56` as a synchronized constant across `renderer.cljs`, `trail.cljs`, `dg_flow.cljs`, `face_primitives.cljc`, and others. Any shrink touching text positioning must not disturb it.

---
*Fact-check (Fable, this session):* aggregate totals re-derived from the live tree (headline corrected: 47,000/83 all, 45,518/80 ex-probe). Spot-verified at source: `kernel.clj` = 830 lines + its self-describing "does not generate code. Nothing calls it" (`kernel.clj:303-305`); the `face_primitives` pinning is `g7-source-form-fidelity` + `g7-named-builders-present` with an EMPTY divergence allowlist (`face_primitives_test.clj:85-115`); probe = 857 + 385 + 240 = 1,482 standalone lines (all untracked); the `:run-parallel` branch has no other `src/` reference (`dg_flow.cljs:943`). Findings are decision-support, not a mandate — no source was changed.
