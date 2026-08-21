# What the Waist Kept — the three areas, one notch down (the form sessions read)

Twin of `kept-code-map.html` (Sid's eyes; published at
https://claude.ai/code/artifact/72f31744-71d8-4ff5-b0f6-a0d04374284f — its own URL, not the
waist map's). Both edited together in every commit — never one without the other. Subject: what the waist cut KEPT — the shared material rows, the
server, the parked render engine — drawn one notch below the editing-waist map's boxes.

**What this page is:** a picture of the kept code as it stands on 2026-08-21 — each area
opened into its pieces; for every piece: what it is · what goes in · what comes out · why
it is shaped so · who it talks to · its observed state. Arrows are drawn only where the
code actually calls (ns-form requires, route handlers, cluster handles); islands are shown
as islands.

**What this page is NOT:** not a ruling. No piece carries a verdict. The question Sid
asked — "is this how I want it architected?" — is open; this page exists so it can be
answered by looking. Not per-function (the three maps under `docs/below-the-waist/` hold
that grain). Not a whole-system zoom ladder — one level, fixed.

**Grain:** one notch below the waist map's 2d boxes (TRUTH KERNEL · SERVE REGISTRY ·
EPISODE LANE · SCENE + GPU MACHINE …) — the pieces inside them; two notches where a box is
big (the material system splits into a write half and a serve half). The five cluster
modules are NOT opened — they stay at the waist map's grain, drawn as the floor every arrow
lands on.

**Authority:** a dated picture, not law. The code outranks this page. Every "why" cites the
settled bullet or the birth ruling it came from; where it came from reading what the code
does, it says so.

---

## 0. Legend — the five words this page uses

| word | meaning | how it was read |
|---|---|---|
| **in / out** | the data shape crossing the piece's boundary — what a caller hands it, what it hands back or writes | from the public fns and the routes/depots/PStates at the seam; never from docstrings alone (names in this tree record the generation that minted them, not the thing running — §6) |
| **why** | the reason the piece is shaped this way — the settled bullet (`decisions.md`) or the ruling that birthed it; when neither exists, what the code itself does | cited per row; "(code)" = read from the code, no law found |
| **LIVE** | on a call chain from a surviving door, a boot path, or a production caller today | route table + requires, `server_jetty.clj` handlers, `cluster.clj` |
| **DORMANT** | reachable only from tests, registry aggregation, or an explicit `bin/land` command — no production caller | exact-symbol census in src/ (the maps' INFERRED rows, spot-checked) |
| **PARKED** | Sid's word (waist task): the scene + GPU machine and the render families — guarded by the verifier build, no product entry | `waist-cleanup-task.md:84-97` · `shadow-cljs.edn` (one target) |
| **CUSTODY** | kept only for TrailView / anatomy / goldens — one-cut deletion debt (Sid, 2026-08-20) | `NOW.md` render-custody line |

One fact frames the whole picture: **nothing above the waist calls in.** Outside `src/`,
the only callers of the ten surviving POST doors are two test files
(`test/app/material_portal_test.clj` → matter-room; `test/app/server/relation_assert_route_test.clj`
→ block-birth). The CLI is summoned *by* the server; it never calls in. LIVE therefore
means "wired to a door or boot path", not "exercised by a product".

---

## 1. The picture (what the SVG in the twin draws)

Three bands + the floor, top to bottom:

```
 SHARED MATERIAL ROWS  src/app/shared · 18 .cljc · 232 KB · born 07-24→07-31
   [portal·inspector·reply] [facet grammar] [9 specs] [binding+verb] [activation event] [matter room]
          ▲ imported as pure .cljc by the material system + the front door
 THE SERVER — one JVM  server_jetty.clj + src/app/server/** · ~1.0 MB (organs ~0.7 MB)
   [FRONT DOOR 10 POST] → [material write half] ⇄ [material serve half]   [LLM seat]
                       → [episode lane] → [cascade]   [annotators + edit entry]   [ingest/migration]
                       → [cluster handles — the one way down] ─────────────────────────────
 THE CLUSTER — five deployed modules (bin/land MODULE_VARS) · not opened · data as-is
   [object-container 2d·26p·5q] [transcript-ops 2d·3p] [relation-kernel 1d·8p·3q] [trail-view 0d·0p·13m·4q] [face-arsenal 1d·4p]
 PARKED RENDER ENGINE  src/app/client/** · 48 files · 1.6 MB · one entry: the verifier build
   [scene values] → [frame computation] → [WebGPU core]            [verifier — builds, fences, goldens]
   [presentation grammar ◫] [scene runtime] [shaper] [render families]
```

Cross-area arrows (all of them — there are four):
1. **server → shared**: the material system (write + serve halves), the front door, `cluster.clj`
   and `episode`/`verb_release` import the shared rows as pure `.cljc`. No `.cljs` file
   anywhere imports `shared/`.
2. **client → shared**: `client/workspace/scene_store.cljc` requires
   `app.shared.material-inspector` (`scene_store.cljc:684`) — the one surviving client-side
   consumer of a material row.
3. **server → client**: `server/rama/object_container/assembly_adapter.clj` requires
   `app.client.workspace.face-assembly` + `face-primitives` — the only server→client bridge
   (the presentation grammar under custody).
4. **server → cluster**: every write and read goes through `cluster.clj` handles
   (`face-projection-runtime` · `trail-runtime`) to the five deployed modules. The JVM never
   launches a module (`bin/land` does).

No arrow runs from the render engine to the server at runtime — the verifier records
`:product-server-used? false`.

---

## 2. AREA A — shared material rows (`src/app/shared/`, 18 files, 231.8 KB)

All born 2026-07-24 → 07-31 (the editable-material campaign), all pure `.cljc`, all
LIVE through the server organs that import them; noted orphans are DORMANT.

| piece | files (bytes) | what it is | in → out | why | talks to | state |
|---|---|---|---|---|---|---|
| **Facet grammar** | `facet_material.cljc` 24.3K · `facet_masters.cljc` 2.3K | the one compiler for a *facet* (a served behaviour/appearance policy of a face): floor fallback, wear resolver, stamp maker, contribution composer; plus the ordered registry of the nine specs | a facet spec (EDN: defaults · fields · form-validators) + a wearer's subject + served rows → a compiled facet table (what the face does) + stamps (which row contributed) | "Furniture is data" — policies must be served, revisioned, deviate-able; the compiler is the one place served rows become behaviour (`decisions.md` settled architecture) | all 9 specs; `facet_master.clj:260-573` · `material_truth` · `material_portal` · `face_projection:900-1236` · `server_jetty:412` | LIVE |
| **The nine specs** | `attention` 7.8K · `positioned` 5.4K · `foldable` 6.8K · `text_body` 1.8K · `threaded` 2.9K · `space` 4.1K · `provenance` 2.4K · `invocation` 4.4K · `anatomy` 31.1K (= 66.7K) | each is the data spec of one block-face behaviour: hit areas · placement/reply-birth defaults · fold policy · wrap columns · thread-adoption distance · zoom bounds + bindable place/marquee · provenance tint · model/effort/precontext for a reply · the block's anatomy grammar | declarations (maps + small pure fns); nothing flows in at runtime → spec maps the registry aggregates; `facet_master` imports each as a candidate master into the object container at boot (`cluster.clj:343-457`) | each was a lived friction on the (now deleted) block face during the campaign, written as served policy instead of client code (`decisions.md` editable-material; P1–P8 gates) | `facet_masters` (registry) · `cluster.clj` ensure-masters · `face_projection` served tables | LIVE via registry; `text_body` and `space` have no production call outside registry aggregation (DORMANT) |
| **Binding + verb grammar** | `binding_material.cljc` 29.7K · `verb_registry.cljc` 17.3K | the closed grammar tying a gesture (tap/drag/key) on a site at a tier to a verb — pure resolver, drills, served tables; and the closed verb registry (versions, effects, args, reservations, releases) | a gesture descriptor + served binding rows + the registry → the verb to dispatch (pure), served tables for faces | "material may select machines only from a closed registry with declared contracts" (build model); camera gestures structurally uncapturable by material (space-as-entity) | 4 specs · `face_projection:1072-1131` · `material_portal:312` · `verb_release:144` | LIVE; `resolve-binding` itself has no source caller now (drill + tests: DORMANT) |
| **Activation event** | `activation_event.cljc` 13.5K | the one closed form of an activation / rollback / pin / unpin act, its serializer and the legacy parser (one read shape, two source generations) | an act map → canonical bytes written into the object container, and parsed back | "POLICY merges by activation — malformed refused at the gate, one-act rollback" (build model); the form is durable bytes, so it is fixed | `matter_room` · `material_truth` · `material_circulation` · `material_portal` · `face_projection` · `facet_master` | LIVE |
| **Matter room** | `matter_room.cljc` 22.2K | pure addressing of a *room* (one entity/master world), normalization of the five acts (open · deviate · activate · rollback · say), deterministic resident projection (which agent seats appear) | room address + act params → normalized act + resident list (`:resident/time-ms` is an ordinal, not a time) | the portal surface was one conversation per room; the server needed a pure, testable core of the room idea (code; campaign P6–P8) | the five matter-room handlers (`server_jetty:804-1024`) · `material_portal.clj` · `face_projection` | LIVE |
| **Portal (pure) · inspector · reply-to-block** | `material_portal.cljc` 44.4K · `material_inspector.cljc` 7.3K · `reply_to_block.cljc` 3.9K | the pure half of the material portal (questions, anchors, canonical bytes, anatomy composition, render model, briefing); extraction/normalization of contribution stamps + wearer snapshots; the pure addressed-reply request / narrowing / prompt composition | served rows + room + stamps → a render model · a briefing text · a reply request | the portal was the x-ray of material (anatomy room, studio arc); "context comes by BRIEFING, assembled from the land's blocks, never by merging transcripts" (one canvas, many conversations) | `material_portal.clj:644-1098` · `face_projection:1809-1834` · `server_jetty:432,613` · `verb_release` · **`scene_store.cljc:684`** (the one client consumer) | LIVE; `reply-to-block/request` and `wearers-from-scene-store` have test callers only (DORMANT) |

---

## 3. AREA B — the server (`src/app/server_jetty.clj` + `src/app/server/**`)

One JVM. ~1.0 MB of server source of which ~0.7 MB is JVM-side organs and ~0.4 MB is the
five modules + their adapters (the floor, §4). Births span four generations: 2024-04 (the
front door), 2026-06 (object container, transcript pipeline), 07-03→07-12 (relation kernel,
trail view, arsenal, face projection, machine cut, code atoms), 07-18→07-31 (durable ground,
episodes, the material system, cascade), 08-20 (`block_edit.clj`).

| piece | files (bytes) | what it is | in → out | why | talks to | state |
|---|---|---|---|---|---|---|
| **Front door** | `server_jetty.clj` 79.5K (born 2024-04-20) | the one Jetty middleware `wrap-file-api` IS the route table: ten POST doors matched by string on `uri`, every handler body in this file; `wrap-params` sits outside it so EDN routes read the raw body (`:1606-1614`) | HTTP POST — EDN or form body; today only tests/curl → EDN response; the episode turn answers with a `text/event-stream` body on the same POST (`:468`) | the 2024 file-explorer API file every generation bolted its doors onto (its docstring still describes a file explorer and a ClojureScript client); no GET survives because the read wire was the Electric websocket that went with the client (code · `editing-waist-map.md` §2f) | every JVM piece below; `cluster.clj` handles | LIVE (no production caller) |
| **Episode lane** | `episode.clj` 49.5K · `dogfood/transcript.clj` 70.8K (9 helper fns used; its `transcript-module` is NOT deployed) · the SSE handler `run-episode-turn` (`server_jetty:439`) | takes an utterance addressed to a lane, records it durably, summons the Claude CLI as a subprocess (`summon-argv`), streams the reply back, harvests the CLI's jsonl into the container, records turn / birth / geometry cells + an instance registry; then `post-turn-distill!` | utterance {text · lane/episode id · source address} · block-birth · geometry (placement + camera cells) → rows in the object container (utterances as imports · turn records · geometry cells · receipts) + SSE events (`:episode-durable` …) | "Episodes bound every CLI session" + "One canvas, many conversations" (settled 07-21/22): the lane is permanent, the session is not; both actors durably recorded; seed by briefing, never jsonl replay | object container via handles · `material_circulation` (receipt after the turn, `episode:519,670`) · `cascade/react!` after the durable turn (`server_jetty:589`) | LIVE (the canvas that posted is gone; tests/curl only) |
| **Material system — write half** | `object_container/facet_master.clj` 34.4K · `material_circulation.clj` 46.6K · `verb_release.clj` 8.9K | `facet_master`: the object-container adapter that imports a facet spec as a candidate master, activates / deviates / pins / releases pointer revisions, instance masters, the activation trail; `circulation`: the foreign-client driver writing receipts, semantic edges, machine records, autotag (via the LLM seat), as-of wears, "experience"; `verb_release`: the fixed 8-node source-address release chain for the reply verb | a matter-room act (open / deviate / activate / rollback / say · drill) · a turn receipt · a release request → object-container rows (masters · instances · pointers · activation history · receipts · records) + relation edges | "Softland's components are served, revisioned, deviate-able, pinnable, previewable, activatable, reversible material" (editable-material, settled) · "The map must not lie" (receipts carry provenance) · multi-cascade R1 (autotag rides as row #1) | shared rows (specs · activation_event · verb_registry) · handles → object container + relation kernel · `dogfood/llm.clj` (autotag turn-run, `circulation:630-671`) | LIVE (`facet_master`, `circulation`); `verb_release` write half DORMANT (tests) |
| **Material system — serve half** | `face_projection.clj` 102.2K · `material_truth.clj` 20.1K · `material_portal.clj` 55.3K · `object_container/block_distiller.clj` 85.9K | `face_projection`: the read-only registry of twelve *face data-contexts* (conversation · assembly · face-list · facet-materials · material-inspector · material-experience · cascade-rows · escape-gauge · interaction-table · material-truth · material-portal · block-truth) and their dispatch; `material_truth`: derived composition over facet_master (registry upkeep, served tiers, blast radius, announcements — owns no truth, despite its name); `material_portal.clj`: the clock-free join that opens one entity/master world by recursively serving six faces; `block_distiller`: distils object-container rows into block-shaped units | a page request (face id + subject + room) — today only from inside the matter-room handlers (`server_jetty:823` open, `:999` rollback) and the conversation seed (`:607`) → an EDN page (data-context) | the portal "projects the material world inhabitably" (campaign P-gates); briefings assembled from the land's blocks (one canvas, many conversations) | shared rows · handles (reads object container · arsenal · relation kernel) · `assembly_adapter` (face_projection requires it) | LIVE internally (serve absorbed into the write path); four entries have no caller: `:assembly` · `:face-list` · `:escape-gauge` · `:block-truth` (DORMANT) |
| **Cascade** | `cascade.clj` 2.0K | one in-process declaration table of server acts against named triggers + `react!`, the one dispatch point (per-row future, isolated failures, declaration-order receipts) | a trigger (`:episode/turn-durable`) + payload → dispatched handlers (row #1 = `server-jetty/run-ambient-autotag!`, named as a symbol in the table — the only src pointer back into the front door) | multi-cascade R1 (settled 07-26): server acts are data; dark capacity proven honest | front door (`react!` at `:589`) → circulation → LLM seat | LIVE |
| **Annotators + the edit entry** | `machine_cut.clj` 55.2K · `code_atoms.clj` 61.8K · `block_edit.clj` 2.0K (born 08-20) | `machine_cut`: conversation-pair annotator — pure core + LLM/WAL/relation shell; only `replay-wal!` is called (`cluster.clj:531-536`, `bin/land ingest`); `code_atoms`: git-backed code ingestion/lineage + HEAD analyzer deriving `:requires`/`:calls` — only its three pure address helpers are used (`verb_release:34-39`); `block_edit`: the one server object-edit entry (`submit-block-edit!` → object-container stream → durable accepted/rejected decision) | conversation pairs · a git HEAD · an object edit {target · content-text · idempotency-key …} → relation edges · code atoms · a decision map | "Relations are typed edges" (machine_cut writes edges) · "External code is a view — address code, never copy it" (code_atoms) · the commit artery floor piece (block_edit: edit in, durable accepted/rejected out) | relation kernel via handles; `block_edit` ← `matter-room-refresh!` (`server_jetty:779-786`) | `block_edit` LIVE · `machine_cut` + `code_atoms` DORMANT (tests · explicit ingest). `machine_cut:546` still holds an `.ednl` writer on a path no caller reaches; the WAL is OFF on the cluster (`cluster.clj:174,290`) |
| **Ingest / migration lane** | `ingest_watchers.clj` 19.2K · `git_spine.clj` 33.2K · `dogfood/transcript_ingest.clj` 47.0K (older ingest module; zero src callers) | the boot-time sweep + git-spine sequence (assert-log replay → spine-sync → extract) run only by `bin/land ingest` → `cluster/ingest!`; `transcript_ingest` is the pre-object-container ingest module kept as a draft | files on disk · git history · bridge logs (`data/*.ednl`, replay only) → object-container imports + relation edges | "Boot-time ingest is OFF the startup path (`bin/land ingest` explicit)" · "No hand-rolled journals — the EDN WAL beside Rama is the recorded dead branch; the `.ednl` logs were bridges, replayed once, retired" (durable ground, Sid 07-15/17) | object container + relation kernel via handles; `assembly_adapter` | DORMANT (explicit command only) |
| **LLM seat** | `dogfood/llm.clj` 99.1K (`llm-module` defined with 4 depots · 26 PStates — NOT deployed; runs in-process via `delay` at `server_jetty:347`) | the in-process LLM turn-run runtime: request → claim → stream-json adapter → items/decisions/usage, backend `:claude`, auth `:subscription` | a turn-run request (from circulation's autotag) → run rows / items / decisions (in memory), the autotag result | "the server floor (kernels, write organ, echo, llm seat) already exists" (one-render-substrate bullet) · model lanes ride subscriptions, never API keys (Sid) | `material_circulation:630-671` | LIVE helper; module not deployed |
| **Cluster handles** | `cluster.clj` 29.1K · `core.clj` 35.2K · `objects.cljc` 3.3K · `util_fns.cljc` 0.4K | opens foreign depot / PState / query handles to the five deployed modules by `get-module-name`, memoized, as runtime maps the handlers use (`face-projection-runtime` · `trail-runtime` — whose `:module-name` is the RELATION kernel's, by its own docstring); never launches a module ("T5: modules deploy via CLI only"); `core` = Rama helpers; `util_fns` = the one `!ingest-epoch-atom` | module vars → handle maps | "Rama is truth" + durable ground: the JVM is a client of a real single-node cluster; `bin/land` owns daemon/deploy/backup | every JVM piece → the five modules | LIVE |

---

## 4. THE FLOOR — five deployed modules (not opened; the waist map's grain)

`bin/land:23-28` `MODULE_VARS` deploys exactly these five; eight `defmodule` forms exist
in src/ (the other three — `transcript-module`, `TranscriptIngestModule`, `llm-module` —
are not in the list). All five source files byte-identical through the cut; data as-is
under `/mnt/data/rama/data` (R5: 43 turns · 406 geometry cells · wear 641 · roster 4 ·
67 relation edges).

| module | source | owns | queries | who reaches it today |
|---|---|---|---|---|
| **object-container** | `object_container.clj:1746` (134.7K) + `object_container/runtime.clj` 17.5K (the admission road: depot → validate → decide → revisions → keyed truth) + adapters `transcript` 32.4K · `transcript_identity` 1.6K · `markdown` 26.7K · `clojure` 33.6K · `assembly` 26.5K | 2 depots (`*object-container-requests-depot` · `*transcript-source-line-completions-depot`) · 26 PStates (requests / decisions / events · containers-by-id · revisions-by-id · revision-history · derived-units · graduations · source-artifacts/versions/anchors/edges · composition children/parent · outline · transcript projection/tool-calls/audit/last-message/source-lines · edit-order) | 5 (`read-latest-source-by-ref` · `read-source-by-ref-version` · `read-unit` · `read-current-revision` · `read-common-material-for-source`) | writes: episode lane · material write half · block_edit · ingest lane; reads: serve half · trail-view mirrors |
| **object-container-transcript-ops** | `object_container.clj:2608` | 2 depots (`*transcript-control-depot` · `*transcript-file-state-depot`) · 3 PStates (`$$transcript-runs` · `$$transcript-file-offsets` · `$$transcript-file-source-lines-by-file`) + one mirror | — | episode lane (transcript helpers) |
| **relation-kernel** | `relation_kernel.clj:634` (54.7K) | 1 depot (`*relation-request-depot`) · 8 PStates (decisions by idempotency/id · events · relations-by-id · status-log · relations-by-target · target-descriptors · activity-by-bucket) | 3 (`relations-for-targets` · `relation-detail` · `relation-activity`) | writes: `/api/relation/assert` · circulation · machine_cut · code_atoms · git_spine · assembly_adapter · block_distiller; reads: serve half |
| **trail-view** | `trail_view.clj:284` (44.6K) | 0 depots · 0 own PStates · 13 mirrors over object-container (+1 over transcript-ops) | 4 (`context-bundle` · `conversation-trail` · `recent-file-activity` · `recent-source-activity`) | handles opened (`cluster.clj:169`); no reader of its four queries found in src outside tests |
| **face-arsenal** | `face_arsenal.clj:166` (21.7K) | 1 depot (`*face-arsenal-depot`) · 4 PStates (`$$faces-by-name` · `$$wear-events-by-face` · `$$wear-counts-by-face` · `$$wear-journal-by-face`) | — | reads only: serve half (`read-face` · `list-faces` · `read-wear-count`); no wear writer left in src (the client wrote wears); `replay-wear-log!` at ingest |

---

## 5. AREA C — the parked render engine (`src/app/client/**`, 48 files, ~1.60 MB)

One build target exists: `shadow-cljs.edn` `:render-verifier`, entries
`[app.client.substrate.webgpu.verifier]`, init `start!`. Every other client namespace is
reachable only through it. Every `.cljc` here has ≥1 test; all 14 `.cljs` files have zero.
Births: the renderer core 2025-11 / 2026-03; scene store + containers 07-12; scene tape,
verifier, shaper 08-02/03; frame_* and the families 08-06 → 08-11.

| piece | files (bytes) | what it is | in → out | why | talks to | state |
|---|---|---|---|---|---|---|
| **Scene values** | `workspace/scene_store.cljc` 35.3K · `containers.cljc` 12.5K · `rect_tree.cljc` 35.2K · `substrate/scene_tape.cljc` 33.1K · `selection.cljc` 6.2K · `snap.cljc` 11.2K (=133.5K) | the pure scene: slots keyed `(view-instance, address)` holding EDN values; container hierarchy + affine registry (effective transforms, point conversion); rect-tree construction/layout/hit-testing; the one ordered tape — nine families, forward paint + reverse pick | slot upserts/removes + container transforms (+ face trees) → ordered entries for a frame · picks (address + context bundle) · descriptors | "One render substrate" (ruled 07-12): one store, transforms compose in-shader, one pick over the store; "document order is row data — sort keys, never position" (render seam) | scene runtime (its mutating caller) · frame computation · families · verifier · **`shared/material_inspector` (`scene_store.cljc:684`)** · presentation grammar (`build-face-tree`) | PARKED |
| **Scene runtime** | `scene_runtime.cljs` 29.6K · `chrome_runtime.cljs` 16.8K · `region3d_runtime.cljs` 27.5K · `frame_runtime.cljs` 15.1K (=89.0K) | the CLJS owners of the scene/container atoms, delta journal, face lifecycle, picks, Missionary views, window installers; the chrome and Region3D runtimes; `frame_runtime` = the W4 browser fixture join (boot / mount / selection→deadline / receipts / PNG export) | scene mutations from a host → atoms the renderer reads · `globalThis` receipts | "only session truths with no upstream (camera, in-flight gestures) originate client-side" (render seam) — the product loop needed an owner for them (code) | scene values · frame computation · families | PARKED; still names deleted namespaces (`render.cljs` · `mouse.cljs` · `face_wiring` · `agent_flow`); installers + Missionary views + `frame_runtime` boot/mount/export have no caller |
| **Frame computation** | `substrate/frame_scheduler` 7.9K · `frame_graph` 41.7K · `frame_inputs` 9.4K · `frame_delta` 7.8K · `frame_semantic_state` 3.0K · `frame_effects` 14.2K · `frame_effect_view` 11.7K · `frame_plan_view` 15.6K (=111.3K) | the render seam as code: scheduler (module clocks/deadlines, encode-or-skip) · graph (resource/pass compiler, structural reuse, validation, oracle, export plan) · inputs (family input declarations, change classification, ledger) · delta (the minted semantic / binding / container / region / viewport deltas) · semantic state (one reducer: batches → arrangement/effect/plan views + generation) · effects + their views | minted deltas + family entries → a frame plan + arrangement/effect views the renderer applies | "Recompute proportional to change, at every layer; no execution clock as an ancestor of derivation; the fenced incremental view with its batch oracle" (render seam, settled 08-03) | scene values → here → WebGPU core; families; verifier | PARKED; `maintain-frame-plan` has no caller; `frame-semantic-state/apply-deltas` is executed by no guard (the verifier never calls `draw-frame!`) |
| **The shaper** | `workspace/text_layout.cljc` 73.0K · `text_layout_planes.cljc` 25.2K · `text_shaper.cljs` 13.6K · `runtime/fonts.cljs` 12.5K (=124.3K) | text in, placement out: shaped block-greedy wrapping as the one break-choosing truth; one material-local layout authority under a declared layout key; font loading | text + font + width/layout key → glyph placements · line geometry | the floor piece — "the shaper (text in, placement out; nobody else measures)" (editing waist, Sid 08-19); the shaping correction contract | WebGPU core (text system) · families · verifier | PARKED-but-floor (text-layout 6/32 + shaper + fence green) |
| **Render families** | material halves `chrome_{derive,material}` 26.6K · `connector_{material,route}` 52.3K · `path_{material,tessellation}` 43.4K · `image_material` 15.3K · `region3d_{material,placement,scene,evaluation}` + `region_rungs` + `workspace/region3d_pointer` 118.5K; GPU halves `chrome_gpu` 17.2K · `connector_gpu` 22.2K · `path_gpu` 18.5K · `region3d_gpu` 81.4K · `region3d_placement_gpu` 25.8K · `region_bindings` 10.6K (=431.8K) | each family = one kind of drawable (chrome · connector · path · image · region3d) as a material half (derive geometry/route/tessellation from slots) + a GPU half (pipelines, buffers), plugged into the tape and frame registries without touching another family | family entries from the tape → GPU draw batches | "plug-in render families sit above — each plugs into the pipeline/frame registries without touching another family" (waist map placement) · the spatial model (a space appears inside another as a first-class citizen face — Region3D) | scene values (tape) · frame computation (inputs) · WebGPU core · verifier | PARKED; chrome + Region3D goldens green |
| **WebGPU core** | `webgpu/renderer.cljs` 202.9K · `compositor_gpu.cljs` 72.3K · `buffer_pool.cljs` 21.9K · `gpu_budget.cljs` 16.2K (=313.3K) | shader/system construction, buffer/texture updates, family entry production, retained state, tape execution, frame encoding (`draw-frame!`, `renderer.cljs:3678`); the multipass compositor; keyed/ordered buffer pools; the budget tracker (reserved vs active bytes, adapter limits) | a store frame + camera + systems → GPU commands, the frame | "WebGPU is a contract the applier speaks natively, never a DOM-shaped target; residency is the applier's private business" (render seam) | frame computation · families · shaper · verifier | PARKED; `draw-frame!` / `draw-comparison-frame!` have no caller — the verifier builds their systems but never invokes them |
| **The verifier** | `webgpu/verifier.cljs` 304.2K | the standalone browser harness: builds production renderer systems, runs the fences and goldens, publishes a schema-v2 result | fixtures → the verification result (152 files · 0 warnings · base / chrome / Region3D goldens) | "the verifier build is their guard until their own later ruling" (waist task, parked exception) | everything in this area; no server (`:product-server-used? false`) | the area's only LIVE entry |
| **Presentation grammar (custody)** | `workspace/face_assembly.cljc` 30.9K · `face_primitives.cljc` 61.9K (=92.8K) | the 16-builder block render channel: assemblies (arrangement-only data over a primitive vocabulary) + the primitives | an assembly + block data → a face tree the scene store builds | "Furniture is data: faces/assemblies are arrangement-only data over a primitive vocabulary" — kept only for TrailView / anatomy / goldens, one-cut deletion debt (Sid 08-20) | **`server/rama/object_container/assembly_adapter.clj` requires both — the only server→client bridge**; `scene_store/build-face-tree` | CUSTODY |

---

## 6. Names that mislead (carried from the waist map §2f — read before trusting a label)

- `wrap-file-api` — the entire front door; its docstring describes a deleted file explorer and a deleted ClojureScript client.
- "kernel" — a truth-owning module (object-container · relation-kernel · face-arsenal), a read projection that owns nothing (trail-view), and a deleted prose spec.
- `cluster/trail-runtime` — a handle bundle named "trail" whose `:module-name` is the RELATION kernel's, by its own docstring.
- `material_truth.clj` — says "truth"; owns none (derived composition over facet_master; `:1-18`).
- `read-transcript-conversation-projection` is keyed by the conversation-container id, not the address; a bare address returns `[]`, not an error (four false FAILs on the first R5 run).
- `positioned` (rect_tree) ≠ the Positioned facet; `material` / `stamp` / `space` / `canonical` mean different things above vs below the waist.
- `scene-tape` is both a fn (`scene_store`) and an ns alias; `frame-delta` the record ≠ `family-entry-delta`.

---

## 7. The numbers

| area | files | bytes | tests touching it | births |
|---|---|---|---|---|
| shared material rows | 18 | 231.8K | 23 flat files under `test/app/` (no `test/app/shared/`) | 07-24 → 07-31 |
| server — JVM organs (front door · episode · material ×2 · cascade · annotators · ingest · LLM · handles) | 20 | ~944K | 21 server test files | 2024-04 · 06-07 · 07-03→07-31 · 08-20 |
| server — the five modules + adapters (floor) | 11 | ~394K | `object-container` 25 · `runtime` 20 · `relation-kernel` 15 | 06-07 → 07-11 |
| parked render engine | 48 (34 `.cljc` · 14 `.cljs`) | ~1,600K | 27 (substrate 21 · workspace 6); all 14 `.cljs` zero | 2025-11 · 03 · 07-12 · 08-02→08-11 |
| **total surviving source** | **98** (+ `src/components/design_tokens.cljc` 1.9K) | **~3.2 MB** | 72 test files | — |

| the front door | |
|---|---|
| POST doors | 10 (`relation/assert` · `matter-room/{open,deviate,activate,rollback,say}` · `material/facet-master/drill` · `episode/{utterance,block-birth,geometry}`) |
| GET doors | 0 |
| streaming | 1 (`episode/utterance`, SSE body on the POST) |
| callers outside `src/` | 2 test files; no script, hook, or bin entry |
| middleware | `wrap-file-api` → `wrap-params` → `wrap-content-type` → not-found |

| the floor | depots | PStates | queries | mirrors |
|---|---|---|---|---|
| object-container | 2 | 26 | 5 | — |
| transcript-ops | 2 | 3 | — | 1 |
| relation-kernel | 1 | 8 | 3 | — |
| trail-view | 0 | 0 | 4 | 13 |
| face-arsenal | 1 | 4 | — | — |
| defined, not deployed | `llm-module` 4/26 · `transcript-module` 3/5 · `TranscriptIngestModule` 4/12 | | | |

---

## 8. Sources — where every claim came from, and what outranks this page

- **Call chains and routes:** `server_jetty.clj` route table (`:1387-1604`), ns-form
  requires across `src/` and `test/` (reverse-require census, 2026-08-21), `cluster.clj`,
  `bin/land:23-28,136-151`, `shadow-cljs.edn`. Spot-checked by the authoring session:
  route callers outside src (`grep -rl api/… bin .claude scripts test tools`), `.ednl`
  writers, `llm.clj` users, trail-view query readers, arsenal and relation writers,
  `block_edit` caller.
- **What each piece does:** the three descriptive maps (`material-rows-map.md`,
  `map-server-organs.md`, `map-scene-gpu.md` — CHECKED / INFERRED rows, read as data, not
  authority), then the public fn skeletons and depot/PState declarations at the seams.
- **Why:** `docs/decisions.md` settled bullets (Rama is truth · durable ground · typed
  relations · furniture is data · one render substrate · one canvas many conversations ·
  episodes · the build model · the render seam · the editing waist), the waist task
  (`waist-cleanup-task.md`), `NOW.md`; "(code)" where no law was found.
- **Births:** `git log --diff-filter=A --follow` per surviving file, 2026-08-21.
- **Bytes:** `find src -type f -printf '%s %p'`, 2026-08-21.
- **Outranks this page:** the code, always; `docs/decisions.md` for the "why";
  `editing-waist-map.md` for the cut itself. This page holds no verdicts; the open
  question ("is this how I want it architected?") is Sid's, and the line that would make
  it judgeable is not yet settled.
