# Dead-path census — src/ against the signed span

Sweep date 2026-08-15 (a census is a measurement; the date is the fact).
Read-only proposal: **nothing here deletes anything.** Authority for every
verdict is the keep-test below plus Sid's rulings — nothing else. CLAUDE-1.md
(the parked old boot file) was read deliberately as *testimony* for the
"what was this path" column, never as authority. The fence held: no
DIRECTION / decisions / BETS / next-prompt / sense-line-model / vision reads;
which product paths are closed enters this document **only** as Sid's ruling
at adjudication. `src/app/server/env.clj` was not read (hard law; classified
from its path and inbound references only).

**Keep-test.** Code survives only if it is:

1. **floor** — executor + math, grammars/checkers, funnel, escape gestures;
2. **serving a named architecture part as exists/embryo** per the signed part
   files (`docs/what-softland-is/architecture/`), with idle-embryo protection:
   unwired machinery the record builds on (ink edit ops, 3D settle-diffs,
   `:assembly/src-path` stamps, unworn effect classes) is embryo, NOT dead;
3. **a live product surface on a path Sid has not closed.**

Provenance citation is NOT a keep reason — git history holds receipts.

**Method (all static structure — checked, not inferred, unless marked).**
Mechanical require/mention graph over all 160 files in `src/`, with reference
scans across `src/`, `src-dev/`, `src-prod/`, `src-build/`, `test/`, `bench/`,
`probes/`, `tools/`, `scripts/`, `shadow-cljs.edn`, `deps.edn`; reachability
closed from the real entry points; every candidate's docstring and the
load-bearing wiring (entry ns forms, jetty route table, `bin/land` deploy set,
flag joins) read directly. Runtime execution was not probed: "wired" below
means statically required and routed, never "observed running" — magnitude
claims are marked inference where they appear.

**Result: 23 candidate files (~10.8k lines) across six groups; 137 keeps.**

**Falsification round (non-kin, 2026-08-15 — ingested).** The factual layer
went to Codex as a GATE: 22 pinned claims (K1–K22) plus one open
deletion-safety lane; the keep-test, all verdicts, and the signed record were
law for the round, not attackable. Result: FACTS-BROKEN on K3/K9/K20 plus
four open-lane couplings — all ingested below (rows amended, riders added);
**zero verdict changes**. The census graph's matching unit was ns names, so
file-path strings in prose and in checker scripts were invisible — exactly
the class the round's open lane existed to catch (kernel.clj taxonomy prose,
`.mjs` checkers that read candidate source as data, a goldens manifest).
Every load-bearing Codex citation was re-verified first-hand before
amendment. The want was never attacked — no finding needed discarding under
the normative-architecture law.

---

## The wiring spine (what actually mounts — every verdict leans on this)

- **Client bundle** (`:dev`/`:prod` → `app.electric-flow/main`): mounts the
  old workspace runtime (`app.client.workspace.runtime`, aliased `loop` —
  the pre-refactor product) and, inside/beside it, the land's flag-gated
  runtimes: chrome, frame (W4), scene, region3d, editing (T2), faces,
  trail-face, and the atom joins `live_atoms`/`live_edges`/`seam_demo`
  behind `?live-atoms=1`-family flags. `ground.cljs` (first-light, the open
  ground) is routed by the same runtime loom.
- **Render verifier** (`:render-verifier` shadow build): `webgpu/verifier.cljs`
  drives the REAL renderer pipelines headlessly — the permanent W0-A checker;
  it is its own build target, so it has zero product inbound by design.
- **Server** (`server_jetty`): the Electric artery + the HTTP route families —
  land routes (`/api/episode/*`, `/api/matter-room/*`, `/api/relation/assert`,
  `/api/material/facet-master/drill`), old-workspace routes (`/api/sidebar/*`,
  `/api/workspace/save-truth`, `/api/editor/save-doc`, `/api/settings/*`,
  `/api/agent/*`, `/api/flow/save-state`, `/api/linear/issues`), the
  design-converter routes (`/api/extract/compile`, `/api/components/*`), the
  review-pack family (`/api/review-pack/*`), and harness (`/api/dev/replay-fixture`).
  The LLM runtime is declared in-process as a lazy delay
  (`server_jetty.clj:810-814`) and starts when the autotag path first forces
  it (`:842-843`) — the autotag cascade's LLM lane (non-kin round precision).
- **Rama cluster** (`bin/land`, T5 — modules deploy via CLI only): exactly
  five modules deploy — object-container, object-container-transcript-ops,
  relation-kernel, trail-view, face-arsenal. Any other module var in src/ is
  not in the deployed set.

---

## Candidates — Sid adjudicates item by item

Per item: what it is · inbound references · keep-test clause it fails ·
proposed verdict · what the path was (CLAUDE-1.md testimony; where CLAUDE-1
is silent, that is said and the file's own words stand in). Last-touch dates
are hints only: a loom ripple can touch a dead file, and stone stays old.
Anything possibly still plan-active **defers to the reconciliation session**.

### Group A — Roam-era remnants (2024; zero inbound anywhere)

**1. `src/app/server/file.clj`** (83 loc, last touch 2024-08-20)
- What: event-log save/replay for the first graph prototype — hardcoded
  `/Users/sid597/…` paths to `softland.edn` / `dg-nodes` / `dg-edges` EDN
  dumps; `save-event` / `load-events` replaying serialized fn calls
  (`rama/add-new-node` rects) into a `:main` graph.
- Inbound: none — src, test, tools, scripts all zero.
- Fails: 1, 2, and 3 (no surface reaches it).
- **Proposed: DELETE.**
- Was: CLAUDE-1.md silent. The file-based event replay from the Roam-export
  era of the prototype.

**2. `src/app/server/rama/roam_ns.clj`** (100 loc, last touch 2024-08-16)
- What: Roam attribute keyword census + `roam-readers` EDN reader map for
  ingesting Roam exports.
- Inbound: none.
- Fails: 1, 2, 3.
- **Proposed: DELETE.** (Rider: `com.roamresearch/backend-sdk` in deps.edn
  has no other src consumer — see riders below.)
- Was: CLAUDE-1.md silent. The Roam-graph import path.

### Group B — dogfood module slices never in the deployed set (test-only inbound)

**3. `src/app/server/rama/dogfood/compute.clj`** (1106 loc, last touch 2026-06-11)
- What: the Compute kernel — requested workspace commands run as physical
  work under a module-owned TaskGlobal executor; claim/observation depots
  stream start/stdout/stderr/exit back into Rama state ("the back-arrow").
  The kernel taxonomy's only `declare-object` + TaskGlobal executor.
- Inbound: zero executable src requires. Tests only (`dogfood_compute_test`,
  `dogfood_compute_probe_test`). Not in `bin/land`'s five-module deploy set.
  Prose citations exist (non-kin round, path-string grade — not consumers):
  `kernel.clj` cites the file across its taxonomy and live-snapshot tables
  (`:33,:50,:71,:135,:456,:474,:519,:529`); `probe_harness.clj:20` quotes the
  ns in a docstring example. Both are stale-prose riders on delete.
- Fails: 2 (no signed part file cites it — checked twice: this census read
  the twelve part files + exchange-05's receipt tables; the non-kin round
  re-verified the part files; provenance citation would not be a keep reason
  regardless) and 3. On clause 1: code-floor's "executor and its mathematics"
  receipts resolve to the render executor and the **escape gauge**, which
  lives at `face_projection.clj:1518-1544` + `material_portal.clj:819` — not
  here (checked).
- **Proposed: DELETE — defer to reconciliation if the Compute slice is
  plan-active.** The TaskGlobal-executor pattern it pioneered survives as
  prose in `kernel.clj`'s taxonomy and in git.
- Was: CLAUDE-1.md silent. The dogfood-runtime Compute slice: workspace
  commands as observable run state.

**4. `src/app/server/rama/dogfood/transcript_ingest.clj`** (1019 loc, 2026-06-08)
- What: a Rama module ingesting Claude Code / Codex JSONL transcripts into
  native containers (conversation projection, composition edges, anchors,
  tool-call indexes).
- Inbound: zero src. One test (`dogfood/transcript_ingest_test`). Not in the
  deploy set.
- Fails: 2 and 3. (Inference, marked as such: superseded by the deployed
  object-container-transcript-ops module + `ingest_watchers`' near-live
  import path, which own transcript ingest today.)
- **Proposed: DELETE — defer if plan-active.**
- Was: CLAUDE-1.md silent. The earlier transcript-ingest topology.

### Group C — the old workspace surfaces (the pre-land product; Sid's path ruling decides)

The mounted client is still the old workspace runtime; the land lives inside
it as flag-joins and the routed open ground. **Whether the old-workspace
product path is closed is a direction fact — every verdict in this group is
conditional on Sid's ruling.** CLAUDE-1.md's testimony for the whole group:
"`client/workspace/` — workspace UI: runtime, editor, sidebar, shell, events,
themes, etc." and "`client/workflows/` — domain workflows (dg_flow, jit)".

Explicitly **not** candidates, because the land routes through them (the
loom): `runtime.cljs`, `runtime/{keyboard,mouse,render,scroll,state,touch,
workspace_actions,agent_flow,fonts,interop}`, `events.cljs`, `rect_tree.cljc`
(the shared scene graph), and `agent.cljs` (the SSE streamer — the open
ground streams resident runs through it, `ground.cljs:2808`). Under a
closure ruling the loom gets **pruned at the deletion pass** (routes to dead
surfaces removed), while the items below delete as whole paths.

| # | File | loc | Last | Inbound (all old-app unless noted) | Fails |
|---|------|----:|------|-------------------------------------|-------|
| 5 | `client/workflows/dg_flow.cljs` | 1140 | 2026-03-29 | runtime/{agent_flow,keyboard,mouse,render,scroll,state} | 1,2 |
| 6 | `client/workflows/jit.cljs` | 228 | 2026-03-08 | runtime/agent_flow | 1,2 |
| 7 | `client/workspace/trail.cljs` | 1337 | 2026-03-10 | dg_flow, cmd_panel, combined_text, agent_flow, interop, scroll, shell | 1,2 |
| 8 | `client/workspace/shell.cljs` | 283 | 2026-03-29 | combined_text, editor_compute, mouse | 1,2 |
| 9 | `client/workspace/sidebar.cljs` | 414 | 2026-07-11 | cmd_panel, combined_text, editor_compute, **settings_view (item 14)**, runtime + 4 loom modules | 1,2 |
| 10 | `client/workspace/runtime/sidebar_io.cljs` | 239 | 2026-07-12 | runtime, agent_flow, keyboard, mouse | 1,2 |
| 11 | `client/workspace/editor_compute.cljs` | 834 | 2026-08-05 | keyboard, mouse, render | 1,2 |
| 12 | `client/workspace/cmd_panel.cljs` | 210 | 2026-07-18 | combined_text, agent_flow, keyboard, mouse, render | 1,2 |
| 13 | `client/workspace/combined_text.cljs` | 525 | 2026-08-03 | runtime/render | 1,2 |
| 14 | `client/workspace/settings_view.cljs` | 325 | 2026-04-28 | fonts, keyboard, mouse, render, state | 1,2 |
| 15 | `client/workspace/text_input.cljs` | 580 | 2026-03-08 | cmd_panel, editor_compute, keyboard | 1,2 |
| 16 | `client/workspace/themes.cljc` | 117 | 2026-03-08 | combined_text, editor_compute, keyboard, settings_view, **electric_flow** | 1,2 |
| 17 | `client/workspace/ui_primitives.cljs` | 435 | 2026-03-08 | dg_flow, jit, cmd_panel, combined_text, editor_compute, mouse, scroll, settings_view, shell, sidebar, trail | 1,2 |

What each was (one line):
- **5 dg_flow** — the DG ticket workflow: flow state machine, intake tree,
  run/review trees, ticket layout (the discourse-graph work UI; pairs with
  jetty `/api/flow/save-state` + `/api/linear/issues`).
- **6 jit** — the JIT workflow: command parsing plus hardcoded preview artifacts.
- **7 trail** — the old markdown agent-run chat trail (the kernel-material
  trail is `trail_face/*` + server `trail_view` — those keep; inference:
  this one is its predecessor).
- **8 shell** — the 3-pane file layout: Editor | Chat | Preview.
- **9/10 sidebar + sidebar_io** — the file-explorer sidebar and its HTTP I/O
  (pairs with `/api/sidebar/*`, `/api/workspace/save-truth`, `/api/list-dir`).
- **11 editor_compute** — the old editor state machine (fold/bracket/rects).
- **12 cmd_panel** — the command panel (parse + rects).
- **13 combined_text** — joins editor/sidebar/cmd-panel/trail text for GPU upload.
- **14 settings_view** — the font/theme settings panel (pairs `/api/settings/*`).
- **15 text_input** — pre-Contract-T input helpers (superseded by
  `text_editing.cljc` — inference).
- **16 themes** — syntax-highlighting themes for the old editor.
- **17 ui_primitives** — old design-token/typography/component builders over
  rect-tree (consumes `components.design-tokens`, which keeps).

Seam caveats for the deletion pass (why some of these are prune-shaped, not
lift-out-shaped — noted now so adjudication sees the real cost):
- **11 editor_compute** additionally requires `block_edit` +
  `block_edit_wiring` (Lane B, A2-receipted — keeps) and mounts
  `trail_face.scene` — those seams re-home if the old editor dies.
- **12 cmd_panel**'s `parse-agent-command` is consumed by `runtime/agent_flow`
  (loom).
- **14 settings_view**'s `font-defaults->settings` is consumed by
  `runtime/fonts` (live text system).
- **16 themes** serves `get-color` to `app.electric-flow` (`electric_flow.cljc:383-388`).
- **7 trail**'s panel-height fns are consumed by `runtime/interop` (harness — keeps).
- **9 before 14 is illegal:** `settings_view.cljs:8` requires `sidebar`
  (`sidebar-w`) — a candidate→candidate edge; item 9 deletes with or after
  item 14, never before (non-kin round).
- **11, 13 are fence-audited source:** the text-layout fence checker reads
  `combined_text.cljs` and `editor_compute.cljs` as data and fails on named
  functions (`test/render_engine/verify_text_layout_fence.mjs:25,28,146-151`)
  — the audit list updates in the same commit that deletes either (non-kin
  round).
- **17 is a source-form test dependency of the faces lane:**
  `face_primitives.cljc:14-20` copies its builders verbatim from
  `ui_primitives.cljs`, and gate G7 (`face_primitives_test.clj:63-77`) reads
  BOTH files as data and fails on drift — "so the two cannot diverge while
  both exist," its own words. On deletion, G7's ui-primitives half retires
  and the copies become sole owner (non-kin round).
- Server riders under the same ruling: the old-workspace route families in
  `server_jetty.clj` and the file-explorer half of `file_viewer.cljc`
  (the file itself keeps — it is also the artery for episode / trail-view /
  machine-cut / cluster pulls).

**Proposed verdict for the group: keep as live surface (clause 3) until Sid
rules the old-workspace path closed; on that ruling, items 5–17 DELETE as
whole feature paths (each with its jetty route family), loom pruned.**
Items with zero land coupling and stale dates (6 jit, 15 text_input, 17
ui_primitives' dg-only halves) are the lowest-risk first cuts if Sid wants a
partial ruling.

### Group D — the design-converter / components library

**18–21. `src/components/adapter.cljc` (199), `compiler.cljc` (352),
`token_matcher.cljc` (228), `css_parsers.cljc` (436)** — all last touched
2026-03-08.
- What: the design-converter pipeline — `_extractor.js` captures a real
  page's computed styles → `adapter` builds Design IR → `token_matcher`
  snaps values to design tokens → `compiler` emits rt-node trees
  "compatible with loop.cljs" (the pre-refactor client, by its old name).
- Inbound: `server_jetty` only (`/api/extract/compile`, route at
  `server_jetty.clj:1996+`); `css_parsers` only via the other three.
  Client half: the extract-preview overlay watcher in `runtime/interop.cljs`.
  Root-tree riders (outside src/): `components/` (shadcn-style registry:
  `_registry.edn`, `_extractor.js`, `_extractor_prompt.md`, `_token_maps`,
  ~60 component dirs) served by `/api/components/registry|status`; e2e test
  `test/design_converter_e2e_test.clj`.
- Fails: 1, 2. Clause 3 is Sid's ruling on the design-import path.
- **Proposed: DELETE (all four + the jetty routes + root `components/` +
  the interop overlay watcher + the e2e test) — defer if plan-active.**
- Was: CLAUDE-1.md silent. The design-converter: import a live page's design
  into the workspace as tokens + rt-nodes.
- **NOT a candidate:** `components/design_tokens.cljc` — live shared tokens,
  consumed by `ui_primitives`, `face_primitives` (faces lane), and jetty.

### Group E — gray: the coexistence demo

**22. `src/app/client/workspace/seam_demo.cljs`** (164 loc, 2026-08-08)
- What: the composed Region3D coexistence demo, enabled only by all three
  flags; installed through `live_atoms` (`live_atoms.cljs:367`).
- Inbound: `live_atoms` only (install `live_atoms.cljs:367`, flag-gated).
  Non-kin round addition: the render verifier hashes its source into
  receipts (`test/render_engine/run_verifier.mjs:609`) and the GPU goldens
  manifest records it in 3 entries — seam_demo sits in the render checker's
  fixture set; deleting it forces a goldens regeneration.
- Fails: 1 and 2 strictly (a demo composition, not machinery the record
  builds on — no part file cites it). It is, however, a wired felt fixture
  over three part-receipted materials (region3d + connector + editing), a
  checker fixture (above), and it is eight days old.
- **Proposed: KEEP-EMBRYO as the coexistence felt fixture, now on
  checker-fixture ground too — DELETE only if Sid calls the demo served
  (that deletion carries the goldens regeneration).**
- Was: CLAUDE-1.md silent. The three-atom coexistence demo.

### Group F — review-pack

**23. `src/app/server/review_pack.clj`** (457 loc, 2026-02-18)
- What: Review Pack v0 — bounded review artifacts for reviewer confidence:
  create/list/publish/summary/feedback over `/api/review-pack/*` (six routes
  in `server_jetty.clj:1887-1937`).
- Inbound: `server_jetty` only; one test (`review_pack_test`). No client
  surface in src/ consumes the API (checked: zero non-jetty references).
- Fails: 1, 2. Clause 3 is Sid's ruling on the review-pack workflow path
  (it served the PR-review process, not the land).
- **Proposed: DELETE (file + routes + test) — defer if the review workflow
  still wants it.**
- Was: CLAUDE-1.md silent. The reviewer-confidence artifact API from the
  team-review workflow era.

---

## Not candidates — the keep map (why the other 137 files stand)

By keep-test clause; part names are the signed part files.

**Floor (clause 1).**
- Render executor + math: `webgpu/{renderer, compositor_gpu, region3d_gpu,
  region3d_placement_gpu, path_gpu, connector_gpu, chrome_gpu, region_bindings,
  buffer_pool, gpu_budget}`, `path_tessellation`, `rect_tree` (the shared
  scene graph), `containers` (affine law).
- Checkers: `webgpu/verifier.cljs` — the permanent W0-A render checker with
  its own shadow build (zero product inbound **by design**; not dead).
- Grammars: `binding_material` (+ the dispatch law), `verb_registry`,
  `anatomy_material`'s edit grammar, Contract-T text floor (`text_layout`,
  `text_layout_planes`, `text_shaper`, `text_editing`).
- Funnel: `rama/core.clj` (the one logical envelope, A11), `cluster.clj`
  (the ONE cluster seam), `object_container/runtime.clj`.
- Escape gestures: the escape-gauge machinery inside `face_projection.clj`
  and `material_portal.clj` (files keep on clause 2 anyway).

**Serving parts as exists/embryo (clause 2)** — receipts are the part files':
- identity-service: `block_distiller`, `selection.cljc`, `transcript_identity`.
- citizenship-contract: `anatomy_material`, `block_edit` (+`block_edit_wiring`,
  `ground_edit`), `region3d_material`, `scene_tape`, `material_portal.cljc`,
  `face_projection`, `trail_view`, `material_circulation` (also code-floor's
  exemption receipt), `reply_to_block`.
- funnel / set-acts: `core.clj`, `dogfood/space.clj` — **idle-embryo**: only
  `kernel.clj` requires it, but set-acts' existence receipts name
  `space.clj:386-401` (frozen content-hashed bundles); the record builds on
  it. `git_spine` (batch receipt), `object_container.clj` (stale guard).
- dispatch-and-courts: `binding_material`, `region3d_pointer`, `ground.cljs`
  (first-light — also the live open-ground surface), `runtime/scroll` (wheel
  receipt; loom).
- relation-kernel: `relation_kernel.clj`, `live_edges` (R1 join to render).
- graph-economy: `git_spine`, `assembly_adapter`, `machine_cut` (silver
  resident marks), `material_circulation` (strata), `cascade.clj` (the
  autotag-cascade trigger layer — resident-sweep embryo).
- packagers: `region_rungs`, `server_jetty` (`:portal-briefing-bytes`),
  `material_portal.clj`/`.cljc` + `face_projection` (owners of the standing
  coverage defect — build-lane item, NOT a census item).
- workshops-and-publication: `activation_event`, `object_container.clj`
  (visibility hook), `facet_master`, `matter_room` (+ its jetty routes).
- universal-verbs: `verb_registry` (three selection verbs), the drill route
  (`/api/material/facet-master/drill` — go-to's named crude ancestor),
  `agent.cljs` + the `/api/agent/*` SSE loop (ask at block grain —
  round-0 receipt; `ground.cljs:2808` streams through it).
- code-floor: `code_atoms` (the code lane driver), `verb_release` (P8
  releases as material), `clojure_adapter`, `kernel.clj` (the kernel-shape
  taxonomy the modules require), `text_kernel` (+ `util_fns`, `objects.cljc`).
- The faces lane: `face_assembly`, `face_primitives`, `face_wiring`,
  `face_arsenal` (deployed module), `trail_face/*` (7 files), shared facet
  materials (`facet_material`, `facet_masters`, `attention/foldable/
  positioned/provenance/space/text_body/threaded/invocation_material`,
  `material_inspector`).
- The render-family embryo lane (ink, images, connectors, 3D, chrome, frames,
  snap): `path_material` (ink edit ops — named embryo), `image_material`,
  `connector_material`/`connector_route`, `region3d_{material,scene,placement,
  evaluation}` (3D settle-diffs — named embryo), `chrome_material`/`chrome_derive`,
  `frame_{delta,effects,effect_view,graph,inputs,plan_view,scheduler,
  semantic_state}` (unworn effect classes — named embryo), `snap`, `scene_store`.

**Live surfaces / arteries (clause 3, on paths not in question).**
`electric_flow.cljc`, `server_jetty.clj`, `file_viewer.cljc` (artery half),
`global_flow.cljs` (await-promise util), `episode.clj` + `/api/episode/*`
(first-light), `ingest_watchers.clj` (near-live import), `dogfood/llm.clj`
(the in-process LLM runtime, `server_jetty.clj:814`), `dogfood/transcript.clj`
(parser/acquisition consumed by episode, ingest-watchers, git-spine, cluster),
the flag runtimes (`chrome_runtime`, `frame_runtime`, `region3d_runtime`,
`editing_runtime`, `editing_segmentation`, `scene_runtime`, `live_atoms`),
`runtime/interop.cljs` (the browser-harness road: replay fixture +
Claude-in-Chrome globals — receipts tooling; its extract-overlay watcher is a
Group-D rider), `env.clj` (keys; unread by law).

**The loom (kept; pruned—not deleted—under any Group C closure).**
`runtime.cljs`, `runtime/{keyboard,mouse,render,scroll,state,touch,
workspace_actions,agent_flow,fonts}`, `events.cljs`, `themes.cljc`'s
electric-flow consumer, `settings_view`'s font-defaults consumer (both listed
in Group C with their seams).

---

## Deletion-pass riders (mechanical follow-ons once verdicts land)

- **deps.edn**, re-verify then remove with their paths:
  `net.clojars.wkok/openai-clojure` (zero src use today),
  `datascript` (zero src use),
  `image-resizer` (no src use found — the "resize" hits are window resizing),
  `com.roamresearch/backend-sdk` (item 2 + env.clj only),
  `org.apache.pdfbox/pdfbox-io` (check `src-build/build/slug_font.clj` first —
  fontbox is plausibly the slug-font tool's; pdfbox-io may be free).
- **Tests that die with their candidates:** `dogfood_compute_test`,
  `dogfood_compute_probe_test` (item 3); `dogfood/transcript_ingest_test`
  (item 4); `design_converter_e2e_test` (items 18–21); `review_pack_test`
  (item 23). No tests cover items 1–2 or 5–17.
- **Stale-prose sweep with any delete** (non-kin round finds; references, not
  consumers): `kernel.clj`'s taxonomy/live-snapshot rows citing `compute.clj`;
  `trail_face/cards.cljc:3` (compares against trail.cljs);
  `face_integration_test.clj:6` and `face_assembly_test.clj:134` (comment
  refs into items 9/11); `probe_harness.clj:20` (docstring example quoting
  compute's ns). `test/resources/code-atom/electric_flow.cljc.txt` is a text
  fixture — no action.
- **Root trees observed, out of census scope** (task scope was src/):
  `components/` (rides Group D), `old-infra/`, `OLD DOCS/`, `snapshots/`,
  `runs/`, `probes/`, `bench/` — a follow-up census if wanted.

## Coverage statement

160 src files: 159 read (docstring + wiring), `env.clj` classified unread by
law. 23 candidates (~10.8k loc) proposed above; 137 keeps mapped to their
clause. The require/mention graph over-approximates liveness (a mention in a
comment counts as an edge), which errs toward keeping — the safe direction
for a census; its matching unit was ns names, so file-path strings were
invisible to it — that class was swept by the non-kin falsification round
and its finds are ingested above. Sid's item-by-item adjudication is the
correction. After
adjudication: whole paths, never patches; commits grouped by concern; build
green per group; on `docs/current-mental-model-local`; never pushed.
