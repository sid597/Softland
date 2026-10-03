# Citations at HEAD

Run 2026-09-27T22:51:38.513828 by `rig.cite.run` on an in-process cluster; its records are thrown away. HEAD is `main` 89c8e1a9, `rig` 5aef8471. A citation is bound at the commit that last changed its line (`git blame`) and marked against each later reading of its file; its state here is against the file's reading at HEAD. The placeholders behind every choice are in `PLAN-citation.md` section 9.

**342 citations.** 4 moved, 202 not-bound-yet, 5 stale, 131 true.

Not bound yet, by reason: markdown-target 178, not-inside-one-form 16, inside-unnamed-form 7, lines-past-end 1.

Stale, by why: changed 5.

Runs still refused in the last pass: none.

## src/proposal/citation-2026-09-27/HANDOFF.md

| line | citation | written | bound to | state at HEAD |
|---|---|---|---|---|
| 33 | `reads.clj:218-229` → `src/proposal/rig-2026-09-25/src/rig/store/reads.clj` | 89c8e1a9 | `layer-fields` 218-230 @1e63217e | moved: now lines 219-230 (form at 219-231) |
| 42 | `PROGRESS.md:264-267` → `src/proposal/frame-2026-09-15/PROGRESS.md` | 89c8e1a9 |  | not bound yet: markdown-target |
| 44 | `src/app/server/ingest/code_import.clj:29` → `src/app/server/ingest/code_import.clj` | 89c8e1a9 | `lineage-asserter-actor-id` 26-30 @89c8e1a9 | true (written at HEAD) |
| 45 | `relation_kernel.clj:441-448` → `src/app/server/rama/relation_kernel.clj` | 89c8e1a9 |  | not bound yet: not-inside-one-form |
| 92 | `src/app/server/ingest/code_import.clj:251-322` → `src/app/server/ingest/code_import.clj` | 89c8e1a9 | `commit-lineage` 251-321 @89c8e1a9 | true (written at HEAD) |
| 105 | `src/rig/store/recipe.clj:118-123` → `src/proposal/rig-2026-09-25/src/rig/store/recipe.clj` | 89c8e1a9 | `capabilities` 118-123 @1e63217e | stale: the form's text changed (now 138-152) |
| 124 | `src/rig/store/envelope.clj:25-29` → `src/proposal/rig-2026-09-25/src/rig/store/envelope.clj` | 89c8e1a9 | `act-parts` 25-29 @1e63217e | true |
| 126 | `gate.clj:252-254` → `src/proposal/rig-2026-09-25/src/rig/store/gate.clj` | 89c8e1a9 | `refusal-with-rows` 207-267 @1e63217e | moved: now lines 253-255 (form at 208-268) |
| 127 | `gate.clj:138` → `src/proposal/rig-2026-09-25/src/rig/store/gate.clj` | 89c8e1a9 | `control-value-ok?` 106-138 @1e63217e | moved: now lines 139-139 (form at 107-139) |
| 136 | `src/app/server/ingest/markdown_adapter.clj:20-23` → `src/app/server/ingest/markdown_adapter.clj` | 89c8e1a9 | `derived-unit-id` 20-23 @89c8e1a9 | true (written at HEAD) |
| 136 | `:99-105` → `src/app/server/ingest/markdown_adapter.clj` | 89c8e1a9 | `markdown-block-v0` 93-189 @89c8e1a9 | true (written at HEAD) |
| 138 | `code_import.clj:14` → `src/app/server/ingest/code_import.clj` | 89c8e1a9 | `ns` 1-23 @89c8e1a9 | true (written at HEAD) |
| 139 | `reads.clj:578-590` → `src/proposal/rig-2026-09-25/src/rig/store/reads.clj` | 89c8e1a9 | `visible?` 578-589 @1e63217e | moved: now lines 585-597 (form at 585-596) |
| 141 | `micro.clj:882-885` → `src/proposal/rig-2026-09-25/src/rig/store/micro.clj` | 89c8e1a9 | `micro-extras` 869-902 @1e63217e | true |
| 145 | `src-dev/dev.cljc:8` → `src-dev/dev.cljc` | 89c8e1a9 | `config` 7-9 @89c8e1a9 | true (written at HEAD) |
| 146 | `server_jetty.clj:475` → `src/app/server/door/server_jetty.clj` | 89c8e1a9 | `run-episode-turn` 457-749 @89c8e1a9 | true (written at HEAD) |
| 146 | `:676` → `src/app/server/door/server_jetty.clj` | 89c8e1a9 | `run-episode-turn` 457-749 @89c8e1a9 | true (written at HEAD) |
| 155 | `LEDGER.md:168` → `src/proposal/frame-2026-09-15/LEDGER.md` | 89c8e1a9 |  | not bound yet: markdown-target |
| 163 | `ingest/code_import.clj:251-322` → `src/app/server/ingest/code_import.clj` | 89c8e1a9 | `commit-lineage` 251-321 @89c8e1a9 | true (written at HEAD) |
| 164 | `ingest/clojure_adapter.clj:209-297` → `src/app/server/ingest/clojure_adapter.clj` | 89c8e1a9 |  | not bound yet: not-inside-one-form |

## src/proposal/formal-model-2026-09-24/STARTER-round-3.md

| line | citation | written | bound to | state at HEAD |
|---|---|---|---|---|
| 32 | `LEDGER.md:168` → `src/proposal/frame-2026-09-15/LEDGER.md` | e1a9d055 |  | not bound yet: markdown-target |
| 33 | `PROGRESS.md:28-31` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |

## src/proposal/inland-integration-2026-09-14/FACTS.md

| line | citation | written | bound to | state at HEAD |
|---|---|---|---|---|
| 120 | `src/app/server/rama/object_container.clj:216` → `src/app/server/rama/object_container.clj` | c3496287 | `object-key-for` 216-219 @c3496287 | true |
| 121 | `object_container.clj:108` → `src/app/server/rama/object_container.clj` | c3496287 | `ObjectContainerRow` 108-111 @c3496287 | true |
| 121 | `:113` → `src/app/server/rama/object_container.clj` | c3496287 | `RevisionRow` 113-115 @c3496287 | true |
| 126 | `relation_kernel.clj:245` → `src/app/server/rama/relation_kernel.clj` | c3496287 |  | not bound yet: not-inside-one-form |
| 127 | `object_container.clj:77` → `src/app/server/rama/object_container.clj` | c3496287 | `ObjectContainerDecisionRow` 77-80 @c3496287 | true |
| 130 | `relation_kernel.clj:265` → `src/app/server/rama/relation_kernel.clj` | c3496287 | `RelationDecisionRow` 265-271 @c3496287 | true |
| 137 | `src/app/server/ingest/clojure_adapter.clj:159` → `src/app/server/ingest/clojure_adapter.clj` | c3496287 | `block-path-for` 159-172 @c3496287 | true |
| 137 | `:294` → `src/app/server/ingest/clojure_adapter.clj` | c3496287 | `derived-unit-id` 294-297 @c3496287 | true |
| 139 | `src/app/server/ingest/code_import.clj:170` → `src/app/server/ingest/code_import.clj` | c3496287 | `cut-named-units` 159-184 @c3496287 | true |
| 139 | `:379` → `src/app/server/ingest/code_import.clj` | c3496287 | `code-sync!` 326-464 @c3496287 | true |
| 143 | `code_import.clj:160` → `src/app/server/ingest/code_import.clj` | c3496287 | `cut-named-units` 159-184 @c3496287 | true |
| 143 | `:251` → `src/app/server/ingest/code_import.clj` | c3496287 | `commit-lineage` 251-321 @c3496287 | true |
| 143 | `:405–428` → `src/app/server/ingest/code_import.clj` | c3496287 | `code-sync!` 326-464 @c3496287 | true |
| 146 | `transcript_identity.clj:11` → `src/app/server/rama/object_container/transcript_identity.clj` | c3496287 | `transcript-object-key` 11-14 @c3496287 | true |
| 146 | `:51` → `src/app/server/rama/object_container/transcript_identity.clj` | c3496287 | `transcript-source-line-key` 51-57 @c3496287 | true |
| 148 | `src/app/server/rama/ingest_epoch.cljc:1–19` → `src/app/server/rama/ingest_epoch.cljc` | c3496287 |  | not bound yet: not-inside-one-form |
| 150 | `door/server_jetty.clj:1634` → `src/app/server/door/server_jetty.clj` | c3496287 | `start-server!` 1634-1650 @c3496287 | true |
| 158 | `episode/cascade.clj:45` → `src/app/server/episode/cascade.clj` | c3496287 | `react!` 27-54 @c3496287 | true |
| 158 | `episode/machine_cut.clj:516` → `src/app/server/episode/machine_cut.clj` | c3496287 | `default-load-river-blocks` 510-519 @c3496287 | true |
| 162 | `src/app/server/worn/facet_engine.cljc:69` → `src/app/server/worn/facet_engine.cljc` | c3496287 | `compile-form` 69-143 @c3496287 | true |
| 162 | `:145` → `src/app/server/worn/facet_engine.cljc` | c3496287 | `compile-source` 145-157 @c3496287 | true |
| 163 | `text_body_material.cljc:30–48` → `src/app/server/worn/text_body_material.cljc` | c3496287 | `spec` 30-48 @c3496287 | true |
| 168 | `text_body_material.cljc:30` → `src/app/server/worn/text_body_material.cljc` | c3496287 | `spec` 30-48 @c3496287 | true |
| 169 | `facet_master.clj:489` → `src/app/server/worn/facet_master.clj` | c3496287 | `instance-master-id` 489-493 @c3496287 | true |
| 169 | `:535` → `src/app/server/worn/facet_master.clj` | c3496287 | `instance-state` 535-556 @c3496287 | true |
| 169 | `:558` → `src/app/server/worn/facet_master.clj` | c3496287 | `write-instance-revision!` 558-686 @c3496287 | true |
| 171 | `facet_master.clj:37` → `src/app/server/worn/facet_master.clj` | c3496287 | `active-pointer-container-id` 37-40 @c3496287 | true |
| 178 | `src/app/server/page/face_projection.clj:1798` → `src/app/server/page/face_projection.clj` | c3496287 | `face->projection-kind` 1798-1806 @c3496287 | true |
| 178 | `:1776–1796` → `src/app/server/page/face_projection.clj` | c3496287 | `projection-registry` 1776-1796 @c3496287 | true |
| 180 | `worn/binding_material.cljc:358` → `src/app/server/worn/binding_material.cljc` | c3496287 | `resolve-binding` 358-386 @c3496287 | true |
| 182 | `page/verb_registry.cljc:23` → `src/app/server/page/verb_registry.cljc` | c3496287 | `verbs` 23-291 @c3496287 | true |
| 183 | `page/matter_room.cljc:174` → `src/app/server/page/matter_room.cljc` | c3496287 | `activation-request` 167-206 @c3496287 | true |
| 184 | `ingest/ingest_watchers.clj:104` → `src/app/server/ingest/ingest_watchers.clj` | c3496287 | `run-import!` 92-127 @c3496287 | true |
| 184 | `rama/trail_view.clj:62` → `src/app/server/rama/trail_view.clj` | c3496287 | `offset-unit-for` 58-65 @c3496287 | true |
| 185 | `episode/cascade.clj:34–36` → `src/app/server/episode/cascade.clj` | c3496287 | `react!` 27-54 @c3496287 | true |
| 189 | `face_projection.clj:176–194` → `src/app/server/page/face_projection.clj` | c3496287 | `shape-conversation` 153-194 @c3496287 | true |
| 192 | `material_portal.clj:958–998` → `src/app/server/page/material_portal.clj` | c3496287 | `open` 628-1007 @c3496287 | true |
| 193 | `door/server_jetty.clj:1389` → `src/app/server/door/server_jetty.clj` | c3496287 | `wrap-file-api` 1389-1608 @c3496287 | true |
| 202 | `server_jetty.clj:101` → `src/app/server/door/server_jetty.clj` | c3496287 |  | not bound yet: not-inside-one-form |
| 202 | `:346` → `src/app/server/door/server_jetty.clj` | c3496287 | `write-event!` 345-349 @c3496287 | true |
| 202 | `:490` → `src/app/server/door/server_jetty.clj` | c3496287 | `run-episode-turn` 457-749 @c3496287 | true |
| 203 | `ingest_epoch.cljc:19` → `src/app/server/rama/ingest_epoch.cljc` | c3496287 | `!ingest-epoch-atom` 19-19 @c3496287 | true |
| 211 | `src/app/client/engine/executor.cljc:334` → `src/app/client/engine/executor.cljc` | c3496287 | `run` 334-340 @c3496287 | true |
| 214 | `path/construction.cljc:21–36` → `src/app/client/path/construction.cljc` | c3496287 | `capabilities` 18-37 @c3496287 | true |
| 215 | `region3d/capabilities.cljc:48–69` → `src/app/client/region3d/capabilities.cljc` | c3496287 | `table` 48-69 @c3496287 | true |
| 217 | `region3d/scene.cljc:965–982` → `src/app/client/region3d/scene.cljc` | c3496287 | `pick-region` 965-982 @c3496287 | true |
| 219 | `text/layout.cljc:1487` → `src/app/client/text/layout.cljc` | c3496287 | `hit-test-result` 1487-1492 @c3496287 | true |
| 223 | `path/records.cljc:86–92` → `src/app/client/path/records.cljc` | c3496287 | `border` 83-92 @c3496287 | true |
| 224 | `harness/path.cljs:57` → `src/app/client/harness/path.cljs` | c3496287 | `path-draw-item` 57-60 @c3496287 | true |
| 226 | `path/placements.cljc:95–154` → `src/app/client/path/placements.cljc` | c3496287 |  | not bound yet: lines-past-end |
| 234 | `resources/inland/seed.edn:18` → `resources/inland/seed.edn` | c3496287 |  | not bound yet: inside-unnamed-form (other) |
| 238 | `module.clj:130` → `src-inland/softland/inland/module.clj` | c3496287 | `material` 129-195 @c3496287 | true |
| 238 | `total.cljc:191` → `src-inland/softland/inland/total.cljc` | c3496287 | `row-key` 187-191 @c3496287 | true |
| 239 | `module.clj:41` → `src-inland/softland/inland/module.clj` | c3496287 | `outcome*` 26-99 @c3496287 | true |
| 240 | `total.cljc:186` → `src-inland/softland/inland/total.cljc` | c3496287 | `version-key` 184-186 @c3496287 | true |
| 244 | `module.clj:134` → `src-inland/softland/inland/module.clj` | c3496287 | `material` 129-195 @c3496287 | true |
| 245 | `total.cljc:170–182` → `src-inland/softland/inland/total.cljc` | c3496287 | `index-keys` 170-182 @c3496287 | true |
| 246 | `module.clj:109–113` → `src-inland/softland/inland/module.clj` | c3496287 | `index-change` 109-113 @c3496287 | true |
| 247 | `execution.cljc:218–235` → `src-inland/softland/inland/execution.cljc` | c3496287 | `Dispatch` 218-235 @c3496287 | true |
| 255 | `module.clj:26–99` → `src-inland/softland/inland/module.clj` | c3496287 | `outcome*` 26-99 @c3496287 | true |
| 259 | `total.cljc:141–168` → `src-inland/softland/inland/total.cljc` | c3496287 |  | not bound yet: not-inside-one-form |
| 273 | `store.clj:88–94` → `src-inland/softland/inland/store.clj` | c3496287 | `request-proxy` 88-94 @c3496287 | true |
| 278 | `execution.cljc:18–26` → `src-inland/softland/inland/execution.cljc` | c3496287 | `ReadStatus` 18-26 @c3496287 | true |
| 285 | `total.cljc:14` → `src-inland/softland/inland/total.cljc` | c3496287 | `vocabulary` 14-14 @c3496287 | true |
| 291 | `total.cljc:117–123` → `src-inland/softland/inland/total.cljc` | c3496287 | `effect-error` 108-125 @c3496287 | true |
| 291 | `session.cljs:52–63` → `src-inland/softland/inland/session.cljs` | c3496287 | `effects!` 44-64 @c3496287 | true |
| 292 | `paint.cljc:103–113` → `src-inland/softland/inland/paint.cljc` | c3496287 | `Item` 97-114 @c3496287 | true |
| 295 | `app.cljc:103` → `src-inland/softland/inland/app.cljc` | c3496287 | `Main` 93-120 @c3496287 | true |
| 295 | `module.clj:155` → `src-inland/softland/inland/module.clj` | c3496287 | `material` 129-195 @c3496287 | true |
| 295 | `server.clj:77` → `src-inland/softland/inland/server.clj` | c3496287 | `-main` 71-90 @c3496287 | true |
| 295 | `88` → `src-inland/softland/inland/server.clj` | c3496287 | `-main` 71-90 @c3496287 | true |
| 296 | `store.clj:17` → `src-inland/softland/inland/store.clj` | c3496287 | `workspaces` 17-17 @c3496287 | true |
| 296 | `86` → `src-inland/softland/inland/store.clj` | c3496287 | `ensure-workspace!` 81-86 @c3496287 | true |
| 303 | `paint.cljc:62–95` → `src-inland/softland/inland/paint.cljc` | c3496287 |  | not bound yet: not-inside-one-form |
| 303 | `:97–114` → `src-inland/softland/inland/paint.cljc` | c3496287 | `Item` 97-114 @c3496287 | true |
| 303 | `nodes.cljc:21–53` → `src-inland/softland/inland/nodes.cljc` | c3496287 |  | not bound yet: not-inside-one-form |
| 305 | `render.cljs:10–23` → `src-inland/softland/inland/render.cljs` | c3496287 | `ns` 1-23 @c3496287 | true |
| 310 | `render.cljs:288–295` → `src-inland/softland/inland/render.cljs` | c3496287 | `path!` 288-295 @c3496287 | true |
| 319 | `render.cljs:183–200` → `src-inland/softland/inland/render.cljs` | c3496287 | `acquire!` 136-223 @c3496287 | true |
| 319 | `:75–91` → `src-inland/softland/inland/render.cljs` | c3496287 |  | not bound yet: not-inside-one-form |
| 321 | `paint.cljc:106` → `src-inland/softland/inland/paint.cljc` | c3496287 | `Item` 97-114 @c3496287 | true |
| 322 | `session.cljs:71–79` → `src-inland/softland/inland/session.cljs` | c3496287 | `deliver!` 71-79 @c3496287 | true |
| 323 | `app.cljc:31–60` → `src-inland/softland/inland/app.cljc` | c3496287 | `Events` 31-60 @c3496287 | true |
| 324 | `seed.edn:22` → `resources/inland/seed.edn` | c3496287 |  | not bound yet: inside-unnamed-form (other) |
| 330 | `execution.cljc:55–77` → `src-inland/softland/inland/execution.cljc` | c3496287 |  | not bound yet: not-inside-one-form |
| 335 | `seed.edn:36` → `resources/inland/seed.edn` | c3496287 |  | not bound yet: inside-unnamed-form (other) |
| 335 | `:78` → `resources/inland/seed.edn` | c3496287 |  | not bound yet: inside-unnamed-form (other) |
| 335 | `execution.cljc:62` → `src-inland/softland/inland/execution.cljc` | c3496287 | `ResolveLayers` 55-68 @c3496287 | true |
| 335 | `:75–77` → `src-inland/softland/inland/execution.cljc` | c3496287 | `Resolve` 70-77 @c3496287 | true |
| 337 | `module.clj:50–51` → `src-inland/softland/inland/module.clj` | c3496287 | `outcome*` 26-99 @c3496287 | true |
| 338 | `seed.edn:22` → `resources/inland/seed.edn` | c3496287 |  | not bound yet: inside-unnamed-form (other) |
| 338 | `:28` → `resources/inland/seed.edn` | c3496287 |  | not bound yet: inside-unnamed-form (other) |
| 338 | `:30` → `resources/inland/seed.edn` | c3496287 |  | not bound yet: inside-unnamed-form (other) |
| 342 | `execution.cljc:198` → `src-inland/softland/inland/execution.cljc` | c3496287 | `Query` 194-216 @c3496287 | true |
| 342 | `:211` → `src-inland/softland/inland/execution.cljc` | c3496287 | `Query` 194-216 @c3496287 | true |
| 342 | `:233` → `src-inland/softland/inland/execution.cljc` | c3496287 | `Dispatch` 218-235 @c3496287 | true |
| 343 | `app.cljc:52–53` → `src-inland/softland/inland/app.cljc` | c3496287 | `Events` 31-60 @c3496287 | true |
| 343 | `module.clj:61` → `src-inland/softland/inland/module.clj` | c3496287 | `outcome*` 26-99 @c3496287 | true |
| 349 | `store.clj:25–36` → `src-inland/softland/inland/store.clj` | c3496287 | `connect!` 25-36 @c3496287 | true |
| 355 | `total.cljc:16` → `src-inland/softland/inland/total.cljc` | c3496287 | `leaves` 15-24 @c3496287 | true |
| 356 | `total.cljc:26–45` → `src-inland/softland/inland/total.cljc` | c3496287 | `expression` 26-45 @c3496287 | true |
| 389 | `app.cljc:59–60` → `src-inland/softland/inland/app.cljc` | c3496287 | `Events` 31-60 @c3496287 | true |

## src/proposal/rig-2026-09-25/RIG.md

| line | citation | written | bound to | state at HEAD |
|---|---|---|---|---|
| 104 | `micro_test.clj:414-416` → `src/proposal/rig-2026-09-25/test/rig/store/micro_test.clj` | b4629030 | `micro-store` 73-699 @b4629030 | stale: the form's text changed (now 74-702) |
| 106 | `micro.clj:1197-1240` → `src/proposal/rig-2026-09-25/src/rig/store/micro.clj` | b4629030 | `fact-rows` 1197-1240 @b4629030 | stale: the form's text changed (now 1365-1411) |
| 160 | `micro_prepare_test.clj:245` → `src/proposal/rig-2026-09-25/test/rig/store/micro_prepare_test.clj` | c9684356 | `parse-and-digests` 221-264 @c9684356 | stale: the form's text changed (now 221-273) |

## src/proposal/store-next-2026-09-25/CONCLUSION.md

| line | citation | written | bound to | state at HEAD |
|---|---|---|---|---|
| 32 | `rama-check/RESULTS.md:45` | e1a9d055 |  | not bound yet: markdown-target |
| 36 | `BENCH_NOTES-stream.md:56` → `src/proposal/rig-2026-09-25/BENCH_NOTES-stream.md` | e1a9d055 |  | not bound yet: markdown-target |
| 38 | `page/matter_room.cljc:66-70` → `src/app/server/page/matter_room.cljc` | e1a9d055 | `matter-actor` 66-70 @e1a9d055 | true |
| 39 | `rama/envelope.clj:367` → `src/app/server/rama/envelope.clj` | e1a9d055 | `authorized-request?` 367-377 @e1a9d055 | true |
| 40 | `rama/relation_kernel.clj:490-497` → `src/app/server/rama/relation_kernel.clj` | e1a9d055 | `relation-outcome` 448-552 @e1a9d055 | true |
| 43 | `src-inland/softland/inland/module.clj:129-136` → `src-inland/softland/inland/module.clj` | e1a9d055 | `material` 129-195 @e1a9d055 | true |
| 48 | `SPEC.md:59` | e1a9d055 |  | not bound yet: markdown-target |
| 55 | `PLAN-locks-and-forgetting.md:739-752` → `src/proposal/rig-2026-09-25/PLAN-locks-and-forgetting.md` | e1a9d055 |  | not bound yet: markdown-target |
| 119 | `docs/builds/inland/integration.md:41-45` → `docs/builds/inland/integration.md` | e1a9d055 |  | not bound yet: markdown-target |
| 121 | `envelope.clj:55-56` → `src/app/server/rama/envelope.clj` | e1a9d055 | `actor-types` 55-56 @e1a9d055 | true |
| 121 | `81-86` → `src/app/server/rama/envelope.clj` | e1a9d055 | `default-actor` 81-86 @e1a9d055 | true |
| 122 | `relation_kernel.clj:49-56` → `src/app/server/rama/relation_kernel.clj` | e1a9d055 | `relation-kinds` 49-74 @e1a9d055 | true |
| 123 | `worn/activation_event.cljc:19-47` → `src/app/server/worn/activation_event.cljc` | e1a9d055 |  | not bound yet: not-inside-one-form |
| 124 | `episode/material_circulation.clj:137-142` → `src/app/server/episode/material_circulation.clj` | e1a9d055 | `receipt-from-context` 94-142 @e1a9d055 | true |
| 193 | `.claude/skills/rama/references/depot-reference.md:263-275` → `.claude/skills/rama/references/depot-reference.md` | e1a9d055 |  | not bound yet: markdown-target |
| 194 | `query-topologies.md:3` | e1a9d055 |  | not bound yet: markdown-target |
| 194 | `5` | e1a9d055 |  | not bound yet: markdown-target |
| 194 | `151` | e1a9d055 |  | not bound yet: markdown-target |
| 195 | `src/proposal/rama-check-2026-09-25/RESULTS.md:45` | e1a9d055 |  | not bound yet: markdown-target |
| 196 | `BENCH_NOTES-stream.md:56` → `src/proposal/rig-2026-09-25/BENCH_NOTES-stream.md` | e1a9d055 |  | not bound yet: markdown-target |
| 197 | `PLAN-locks-and-forgetting.md:723-760` → `src/proposal/rig-2026-09-25/PLAN-locks-and-forgetting.md` | e1a9d055 |  | not bound yet: markdown-target |
| 198 | `PLAN-stream-store.md:71` → `src/proposal/rig-2026-09-25/PLAN-stream-store.md` | e1a9d055 |  | not bound yet: markdown-target |
| 198 | `120-121` → `src/proposal/rig-2026-09-25/PLAN-stream-store.md` | e1a9d055 |  | not bound yet: markdown-target |
| 199 | `docs/builds/inland/integration.md:41-45` → `docs/builds/inland/integration.md` | e1a9d055 |  | not bound yet: markdown-target |
| 200 | `src-inland/softland/inland/module.clj:129-136` → `src-inland/softland/inland/module.clj` | e1a9d055 | `material` 129-195 @e1a9d055 | true |
| 202 | `envelope.clj:18-56` → `src/app/server/rama/envelope.clj` | e1a9d055 |  | not bound yet: not-inside-one-form |
| 202 | `81-86` → `src/app/server/rama/envelope.clj` | e1a9d055 | `default-actor` 81-86 @e1a9d055 | true |
| 202 | `113-119` → `src/app/server/rama/envelope.clj` | e1a9d055 | `default-policy` 113-119 @e1a9d055 | true |
| 202 | `367` → `src/app/server/rama/envelope.clj` | e1a9d055 | `authorized-request?` 367-377 @e1a9d055 | true |
| 203 | `server_jetty.clj:1062-1065` → `src/app/server/door/server_jetty.clj` | e1a9d055 | `matter-room-say!` 1038-1191 @e1a9d055 | true |
| 204 | `matter_room.cljc:66-70` → `src/app/server/page/matter_room.cljc` | e1a9d055 | `matter-actor` 66-70 @e1a9d055 | true |
| 205 | `relation_kernel.clj:490-497` → `src/app/server/rama/relation_kernel.clj` | e1a9d055 | `relation-outcome` 448-552 @e1a9d055 | true |
| 206 | `object_container.clj:113` → `src/app/server/rama/object_container.clj` | e1a9d055 | `RevisionRow` 113-115 @e1a9d055 | true |
| 206 | `628-657` → `src/app/server/rama/object_container.clj` | e1a9d055 |  | not bound yet: not-inside-one-form |

## src/proposal/store-next-2026-09-25/WHERE-THE-PLAN-FITS.md

| line | citation | written | bound to | state at HEAD |
|---|---|---|---|---|
| 136 | `vision/LOG.md:1306-1310` | e1a9d055 |  | not bound yet: markdown-target |
| 139 | `vision/LOG.md:1360` | e1a9d055 |  | not bound yet: markdown-target |
| 141 | `vision/LOG.md:1304-1310` | e1a9d055 |  | not bound yet: markdown-target |
| 141 | `vision/LOG.md:1356-1362` | e1a9d055 |  | not bound yet: markdown-target |
| 149 | `history/docs/build-softland-in-softland/HANDOFF.md:224-229` → `history/docs/build-softland-in-softland/HANDOFF.md` | e1a9d055 |  | not bound yet: markdown-target |
| 151 | `history/docs/build-softland-in-softland/HANDOFF.md:217-229` → `history/docs/build-softland-in-softland/HANDOFF.md` | e1a9d055 |  | not bound yet: markdown-target |
| 151 | `docs/builds/inland/intended-design.md:22` → `docs/builds/inland/intended-design.md` | e1a9d055 |  | not bound yet: markdown-target |
| 159 | `docs/builds/inland/integration.md:36-38` → `docs/builds/inland/integration.md` | e1a9d055 |  | not bound yet: markdown-target |
| 161 | `docs/builds/inland/integration.md:35-45` → `docs/builds/inland/integration.md` | e1a9d055 |  | not bound yet: markdown-target |
| 169 | `PROGRESS.md:191-192` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 171 | `src/proposal/frame-2026-09-15/PROGRESS.md:188-195` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 179 | `SPEC.md:99-101` | e1a9d055 |  | not bound yet: markdown-target |
| 181 | `SPEC.md:95-101` | e1a9d055 |  | not bound yet: markdown-target |
| 181 | `README.md:48` | e1a9d055 |  | not bound yet: markdown-target |
| 201 | `docs/builds/inland/integration.md:27` → `docs/builds/inland/integration.md` | e1a9d055 |  | not bound yet: markdown-target |
| 211 | `src/proposal/inland-integration-2026-09-14/FACTS.md:59` → `src/proposal/inland-integration-2026-09-14/FACTS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 226 | `src/proposal/frame-2026-09-15/LEDGER.md:201` → `src/proposal/frame-2026-09-15/LEDGER.md` | e1a9d055 |  | not bound yet: markdown-target |
| 228 | `LEDGER.md:197-203` → `src/proposal/frame-2026-09-15/LEDGER.md` | e1a9d055 |  | not bound yet: markdown-target |
| 228 | `LEDGER.md:213` → `src/proposal/frame-2026-09-15/LEDGER.md` | e1a9d055 |  | not bound yet: markdown-target |
| 236 | `SPEC.md:14-15` | e1a9d055 |  | not bound yet: markdown-target |
| 238 | `SPEC.md:9-15` | e1a9d055 |  | not bound yet: markdown-target |
| 245 | `src-inland/softland/inland/module.clj:129-136` → `src-inland/softland/inland/module.clj` | e1a9d055 | `material` 129-195 @e1a9d055 | true |
| 262 | `docs/carry-on.md:248` → `docs/carry-on.md` | e1a9d055 |  | not bound yet: markdown-target |
| 264 | `docs/carry-on.md:248` → `docs/carry-on.md` | e1a9d055 |  | not bound yet: markdown-target |
| 272 | `LEDGER.md:158` → `src/proposal/frame-2026-09-15/LEDGER.md` | e1a9d055 |  | not bound yet: markdown-target |
| 274 | `LEDGER.md:158` → `src/proposal/frame-2026-09-15/LEDGER.md` | e1a9d055 |  | not bound yet: markdown-target |
| 274 | `PARTS-4-8.md:146-153` → `src/proposal/frame-2026-09-15/PARTS-4-8.md` | e1a9d055 |  | not bound yet: markdown-target |
| 281 | `PROGRESS.md:67-74` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 281 | `PROGRESS.md:109-112` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 288 | `RIG.md:19-42` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 309 | `LEDGER.md:215` → `src/proposal/frame-2026-09-15/LEDGER.md` | e1a9d055 |  | not bound yet: markdown-target |
| 311 | `LEDGER.md:211-216` → `src/proposal/frame-2026-09-15/LEDGER.md` | e1a9d055 |  | not bound yet: markdown-target |
| 311 | `CORNERS.md:12` → `src/proposal/frame-2026-09-15/CORNERS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 318 | `history/proposals/first-record-2026-09-20/README.md:4` → `history/proposals/first-record-2026-09-20/README.md` | e1a9d055 |  | not bound yet: markdown-target |
| 325 | `PROGRESS.md:24-40` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 332 | `PROGRESS.md:42-107` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 340 | `RIG.md:224-225` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 342 | `RIG.md:221-225` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 362 | `PROGRESS.md:45-47` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 362 | `PROGRESS.md:60-62` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 362 | `docs/carry-on.md:360-364` → `docs/carry-on.md` | e1a9d055 |  | not bound yet: markdown-target |
| 369 | `PROGRESS.md:91-102` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 376 | `PROGRESS.md:138-145` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 384 | `RIG.md:79-81` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 386 | `RIG.md:76-83` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 393 | `RIG.md:214-219` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 414 | `docs/carry-on.md:137` → `docs/carry-on.md` | e1a9d055 |  | not bound yet: markdown-target |
| 416 | `PROGRESS.md:34-35` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 416 | `PROGRESS.md:75-86` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 416 | `docs/carry-on.md:133-140` → `docs/carry-on.md` | e1a9d055 |  | not bound yet: markdown-target |
| 423 | `PROGRESS.md:162-165` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 430 | `RIG.md:100` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 430 | `RESULTS.md:75-90` → `runs/127376b0/RESULTS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 430 | `RESULTS.md:102-109` → `runs/127376b0/RESULTS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 437 | `BENCH_NOTES-stream.md:44-56` → `src/proposal/rig-2026-09-25/BENCH_NOTES-stream.md` | e1a9d055 |  | not bound yet: markdown-target |
| 467 | `PROGRESS.md:215-217` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 474 | `PROGRESS.md:218-222` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 474 | `README.md:162-193` | e1a9d055 |  | not bound yet: markdown-target |
| 495 | `PROGRESS.md:67` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 497 | `PROGRESS.md:67` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 504 | `RESULTS.md:26-68` → `runs/127376b0/RESULTS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 504 | `RESULTS.md:111-133` → `runs/127376b0/RESULTS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 504 | `RIG.md:103-120` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 511 | `RIG.md:13-66` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 518 | `RIG.md:205-231` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 518 | `README.md:64-74` | e1a9d055 |  | not bound yet: markdown-target |
| 535 | `SPEC.md:56-111` | e1a9d055 |  | not bound yet: markdown-target |
| 545 | `RIG.md:85-120` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 554 | `SPEC.md:56-63` | e1a9d055 |  | not bound yet: markdown-target |
| 554 | `RIG.md:19-42` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 563 | `SPEC.md:65-72` | e1a9d055 |  | not bound yet: markdown-target |
| 563 | `PROGRESS.md:103-104` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 572 | `SPEC.md:74-80` | e1a9d055 |  | not bound yet: markdown-target |
| 581 | `SPEC.md:82-87` | e1a9d055 |  | not bound yet: markdown-target |
| 590 | `SPEC.md:89-93` | e1a9d055 |  | not bound yet: markdown-target |
| 599 | `SPEC.md:95-101` | e1a9d055 |  | not bound yet: markdown-target |
| 608 | `SPEC.md:103-107` | e1a9d055 |  | not bound yet: markdown-target |
| 608 | `RIG.md:205-231` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 617 | `SPEC.md:109-111` | e1a9d055 |  | not bound yet: markdown-target |
| 617 | `RIG.md:250-252` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 630 | `RIG.md:52-57` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 683 | `PROGRESS.md:103-104` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 740 | `RESULTS.md:111-133` → `runs/127376b0/RESULTS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 788 | `RIG.md:79-81` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 790 | `PROGRESS.md:45-47` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 790 | `PROGRESS.md:60-62` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 795 | `SPEC.md:59-60` | e1a9d055 |  | not bound yet: markdown-target |
| 797 | `PLAN-locks-and-forgetting.md:739-752` → `src/proposal/rig-2026-09-25/PLAN-locks-and-forgetting.md` | e1a9d055 |  | not bound yet: markdown-target |
| 804 | `RIG.md:76-83` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 804 | `PROGRESS.md:45-47` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 804 | `PLAN-locks-and-forgetting.md:739-752` → `src/proposal/rig-2026-09-25/PLAN-locks-and-forgetting.md` | e1a9d055 |  | not bound yet: markdown-target |
| 814 | `PROGRESS.md:85-86` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 820 | `BENCH_NOTES-stream.md:44-56` → `src/proposal/rig-2026-09-25/BENCH_NOTES-stream.md` | e1a9d055 |  | not bound yet: markdown-target |
| 820 | `RIG.md:221-225` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 820 | `PROGRESS.md:85-86` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 830 | `PROGRESS.md:75-84` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 830 | `PROGRESS.md:172-177` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 839 | `RIG.md:100` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 839 | `RESULTS.md:75-90` → `runs/127376b0/RESULTS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 839 | `PROGRESS.md:75-84` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 849 | `PROGRESS.md:162-165` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 855 | `RESULTS.md:102-109` → `runs/127376b0/RESULTS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 855 | `PROGRESS.md:162-165` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 855 | `RIG.md:187-188` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 865 | `PROGRESS.md:81-83` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 865 | `PROGRESS.md:34-35` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 870 | `carry-on.md:137` → `docs/carry-on.md` | e1a9d055 |  | not bound yet: markdown-target |
| 875 | `PROGRESS.md:81-83` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 875 | `PROGRESS.md:34-35` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 875 | `docs/carry-on.md:137` → `docs/carry-on.md` | e1a9d055 |  | not bound yet: markdown-target |
| 885 | `LEDGER.md:158` → `src/proposal/frame-2026-09-15/LEDGER.md` | e1a9d055 |  | not bound yet: markdown-target |
| 896 | `LEDGER.md:158` → `src/proposal/frame-2026-09-15/LEDGER.md` | e1a9d055 |  | not bound yet: markdown-target |
| 906 | `PROGRESS.md:56-57` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 912 | `PROGRESS.md:56-57` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 922 | `PROGRESS.md:105-107` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 933 | `PROGRESS.md:105-107` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 933 | `LEDGER.md:201` → `src/proposal/frame-2026-09-15/LEDGER.md` | e1a9d055 |  | not bound yet: markdown-target |
| 951 | `PROGRESS.md:181-186` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 959 | `docs/builds/inland/integration.md:41-45` → `docs/builds/inland/integration.md` | e1a9d055 |  | not bound yet: markdown-target |
| 963 | `envelope.clj:55-56` → `src/app/server/rama/envelope.clj` | e1a9d055 | `actor-types` 55-56 @e1a9d055 | true |
| 963 | `81-86` → `src/app/server/rama/envelope.clj` | e1a9d055 | `default-actor` 81-86 @e1a9d055 | true |
| 964 | `relation_kernel.clj:49-56` → `src/app/server/rama/relation_kernel.clj` | e1a9d055 | `relation-kinds` 49-74 @e1a9d055 | true |
| 965 | `worn/activation_event.cljc:19-47` → `src/app/server/worn/activation_event.cljc` | e1a9d055 |  | not bound yet: not-inside-one-form |
| 966 | `episode/material_circulation.clj:137-142` → `src/app/server/episode/material_circulation.clj` | e1a9d055 | `receipt-from-context` 94-142 @e1a9d055 | true |
| 978 | `SPEC.md:95-101` | e1a9d055 |  | not bound yet: markdown-target |
| 992 | `RIG.md:281-285` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1022 | `PARTS-4-8.md:146-153` → `src/proposal/frame-2026-09-15/PARTS-4-8.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1034 | `PROGRESS.md:172-186` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1040 | `PROGRESS.md:172-177` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1040 | `README.md:285` | e1a9d055 |  | not bound yet: markdown-target |
| 1057 | `RIG.md:250-252` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1065 | `page/matter_room.cljc:66-70` → `src/app/server/page/matter_room.cljc` | e1a9d055 | `matter-actor` 66-70 @e1a9d055 | true |
| 1065 | `rama/envelope.clj:367-377` → `src/app/server/rama/envelope.clj` | e1a9d055 | `authorized-request?` 367-377 @e1a9d055 | true |
| 1065 | `rama/relation_kernel.clj:490-497` → `src/app/server/rama/relation_kernel.clj` | e1a9d055 | `relation-outcome` 448-552 @e1a9d055 | true |
| 1065 | `door/server_jetty.clj:1060-1066` → `src/app/server/door/server_jetty.clj` | e1a9d055 | `matter-room-say!` 1038-1191 @e1a9d055 | true |
| 1071 | `RIG.md:205-219` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1097 | `RIG.md:187-188` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1097 | `PROGRESS.md:81-83` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1097 | `gate.clj:186-193` → `src/proposal/rig-2026-09-25/src/rig/store/gate.clj` | e1a9d055 | `stamp-for` 186-193 @fdc79b6c | stale: the form's text changed (now 332-349) |
| 1105 | `PROGRESS.md:53-55` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1106 | `PROGRESS.md:129-135` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1110 | `PROGRESS.md:48-52` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1111 | `PROGRESS.md:109-112` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1114 | `PROGRESS.md:136-137` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1115 | `PROGRESS.md:150-158` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1121 | `PROGRESS.md:18-19` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1123 | `PROGRESS.md:60-62` → `src/proposal/frame-2026-09-15/PROGRESS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1130 | `SPEC.md:95-101` | e1a9d055 |  | not bound yet: markdown-target |
| 1132 | `LEDGER.md:201` → `src/proposal/frame-2026-09-15/LEDGER.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1134 | `README.md:162-186` | e1a9d055 |  | not bound yet: markdown-target |
| 1135 | `RIG.md:1-6` → `src/proposal/rig-2026-09-25/RIG.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1145 | `README.md:45-74` | e1a9d055 |  | not bound yet: markdown-target |
| 1145 | `BENCH_NOTES-stream.md:40-70` → `src/proposal/rig-2026-09-25/BENCH_NOTES-stream.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1148 | `vision/LOG.md:1296-1383` | e1a9d055 |  | not bound yet: markdown-target |
| 1148 | `docs/carry-on.md`:133-140` → `docs/carry-on.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1148 | `187-280` → `docs/carry-on.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1148 | `360-364` → `docs/carry-on.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1149 | `LEDGER.md`:154-160` → `src/proposal/frame-2026-09-15/LEDGER.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1149 | `195-222` → `src/proposal/frame-2026-09-15/LEDGER.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1149 | `PARTS-4-8.md`:146-153` → `src/proposal/frame-2026-09-15/PARTS-4-8.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1149 | `CORNERS.md`:12` → `src/proposal/frame-2026-09-15/CORNERS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1149 | `367` → `src/proposal/frame-2026-09-15/CORNERS.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1149 | `docs/builds/inland/integration.md`:27-60` → `docs/builds/inland/integration.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1149 | `history/docs/build-softland-in-softland/HANDOFF.md`:205-235` → `history/docs/build-softland-in-softland/HANDOFF.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1150 | `page/matter_room.cljc`:66-70` → `src/app/server/page/matter_room.cljc` | e1a9d055 | `matter-actor` 66-70 @e1a9d055 | true |
| 1150 | `:226` → `src/app/server/page/matter_room.cljc` | e1a9d055 | `say-request` 218-261 @e1a9d055 | true |
| 1150 | `rama/envelope.clj`:18-19` → `src/app/server/rama/envelope.clj` | e1a9d055 |  | not bound yet: not-inside-one-form |
| 1150 | `113-119` → `src/app/server/rama/envelope.clj` | e1a9d055 | `default-policy` 113-119 @e1a9d055 | true |
| 1150 | `367-377` → `src/app/server/rama/envelope.clj` | e1a9d055 | `authorized-request?` 367-377 @e1a9d055 | true |
| 1150 | `rama/relation_kernel.clj`:355-381` → `src/app/server/rama/relation_kernel.clj` | e1a9d055 | `request-shape-errors` 355-381 @e1a9d055 | true |
| 1150 | `490-497` → `src/app/server/rama/relation_kernel.clj` | e1a9d055 | `relation-outcome` 448-552 @e1a9d055 | true |
| 1150 | `door/server_jetty.clj`:1060-1066` → `src/app/server/door/server_jetty.clj` | e1a9d055 | `matter-room-say!` 1038-1191 @e1a9d055 | true |
| 1152 | `depot-reference.md:263-275` | e1a9d055 |  | not bound yet: markdown-target |
| 1152 | `query-topologies.md:3` | e1a9d055 |  | not bound yet: markdown-target |
| 1152 | `5` | e1a9d055 |  | not bound yet: markdown-target |
| 1152 | `151` | e1a9d055 |  | not bound yet: markdown-target |
| 1152 | `PLAN-stream-store.md:71` → `src/proposal/rig-2026-09-25/PLAN-stream-store.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1152 | `120-121` → `src/proposal/rig-2026-09-25/PLAN-stream-store.md` | e1a9d055 |  | not bound yet: markdown-target |
| 1152 | `module.clj:129-136` → `src-inland/softland/inland/module.clj` | e1a9d055 | `material` 129-195 @e1a9d055 | true |
| 1152 | `object_container.clj:113` → `src/app/server/rama/object_container.clj` | e1a9d055 | `RevisionRow` 113-115 @e1a9d055 | true |
| 1152 | `628-657` → `src/app/server/rama/object_container.clj` | e1a9d055 |  | not bound yet: not-inside-one-form |
