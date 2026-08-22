# What the Waist Kept — the three areas, one level down (the form sessions read)

Twin of `kept-code-map.html` (Sid's eyes; published at
https://claude.ai/code/artifact/72f31744-71d8-4ff5-b0f6-a0d04374284f — its own URL, not the
waist map's). Both edited together in every commit — never one without the other. Subject: what the waist cut KEPT — the shared material rows, the
server, the parked render engine — drawn one level below the editing-waist map's boxes. Server band
redrawn 2026-08-22: every non-Rama waist box opened into its file-groups with labelled arrows;
the five deployed modules stay the opaque floor.

**What this page is:** a picture of the kept code as it stands on 2026-08-21 (server band 2026-08-22) — each area
opened into its pieces; for every piece: what it is · what goes in · what comes out · why
it is shaped so · who it talks to · its observed state. Arrows are drawn only where the
code actually calls (ns-form requires, route handlers, cluster handles); islands are shown
as islands.

**What this page is NOT:** not a ruling. No piece carries a verdict. The question Sid
asked — "is this how I want it architected?" — is open; this page exists so it can be
answered by looking. Not per-function (the three maps under `docs/below-the-waist/` hold
that grain). Not a whole-system zoom ladder — one level, fixed.

**Grain:** one fixed level below the waist map for the surviving non-Rama code — every
non-Rama waist box (FRONT DOOR · EPISODE LANE · MATERIAL SYSTEM write / serve · CASCADE ·
ANNOTATORS + EDIT ENTRY · INGEST / MIGRATION · CLUSTER HANDLES · the shared rows · the
scene/GPU machine) opened into its file-groups, with the arrows between them labelled by the
value crossing the seam. The five deployed Rama modules (object-container · transcript-ops ·
relation-kernel · trail-view · face-arsenal) are NOT opened: they remain opaque terminal boxes
at the waist-map grain — the floor non-Rama arrows terminate on. Not per-function (the three
maps under `docs/below-the-waist/` hold that grain); not a zoom ladder.

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
          ▲ imported as pure .cljc by the material system (both halves) · the front door · cluster.clj · verb_release
 THE SERVER — one JVM  server_jetty.clj + src/app/server/** · ~1.0 MB (organs ~0.7 MB) — every non-Rama waist box opened
   [FRONT DOOR 10 POST] ─┬→ EPISODE LANE     [SSE door + turn loop] → [episode.clj] → [transcript helpers]      [CASCADE] ◄ react! (SSE door)
                         │                        └→ face_projection (briefing·seed)  └→ block_distiller (distill)       └→ row #1 → door → circulation
                         ├→ SERVE HALF       [block_distiller] ◄ [face_projection] ⇄ [material_portal]    [material_truth] ◄ door deviate!
                         │                                          │ read-master                             │ deviate!·pin!·unpin! (writes) · reads
                         ├→ WRITE HALF       [facet_master]   [verb_release ·island]   [material_circulation] → [LLM SEAT]
                         │                                                                   ▲ door: gold · reference · autotag
                         ├→ ANNOTATORS       [block_edit] [machine_cut] [code_atoms] — three islands
                         │  INGEST           [ingest_watchers] → [git_spine]   [transcript_ingest ·island — a whole undeployed module]
                         └→ CLUSTER HANDLES  [cluster.clj — opens the handles] [core.clj — grammar library] [util_fns — the atom] [objects.cljc ·orphan]
                                                   │ depot · PState · query handles (the door opens them, the organs receive them)
 THE CLUSTER — five deployed modules (bin/land MODULE_VARS) · not opened · the opaque floor · data as-is
   [object-container 2d·26p·5q] [transcript-ops 2d·3p] [relation-kernel 1d·8p·3q] [trail-view 0d·0p·13m·4q] [face-arsenal 1d·4p]
 PARKED RENDER ENGINE  src/app/client/** · 48 files · 1.6 MB · one entry: the verifier build
   [scene values] → [frame computation] → [WebGPU core]            [verifier — builds, fences, goldens]
   [presentation grammar ◫] [scene runtime] [shaper] [render families]
```

Cross-area arrows (all of them — there are four):
1. **server → shared**: the material system (write + serve halves), the front door, `cluster.clj`
   and `verb_release` import the shared rows as pure `.cljc` (`episode.clj` imports none). No
   `.cljs` file anywhere imports `shared/`.
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
| **The nine specs** | `attention` 7.8K · `positioned` 5.4K · `foldable` 6.8K · `text_body` 1.8K · `threaded` 2.9K · `space` 4.1K · `provenance` 2.4K · `invocation` 4.4K · `anatomy` 31.1K (= 66.7K) | each is the data spec of one block-face behaviour: hit areas · placement/reply-birth defaults · fold policy · wrap columns · thread-adoption distance · zoom bounds + bindable place/marquee · provenance tint · model/effort/precontext for a reply · the block's anatomy grammar | declarations (maps + small pure fns); nothing flows in at runtime → spec maps the registry aggregates; `facet_master` imports each as a candidate master into the object container through `cluster/facet-materials-ingest!` (`cluster.clj:332-457`, a clj -X / REPL entry — no bin/land verb, not the boot path) | each was a lived friction on the (now deleted) block face during the campaign, written as served policy instead of client code (`decisions.md` editable-material; P1–P8 gates) | `facet_masters` (registry) · `cluster.clj` facet-materials-ingest! · `face_projection` served tables | LIVE via registry; `text_body` and `space` have no production call outside registry aggregation (DORMANT) |
| **Binding + verb grammar** | `binding_material.cljc` 29.7K · `verb_registry.cljc` 17.3K | the closed grammar tying a gesture (tap/drag/key) on a site at a tier to a verb — pure resolver, drills, served tables; and the closed verb registry (versions, effects, args, reservations, releases) | a gesture descriptor + served binding rows + the registry → the verb to dispatch (pure), served tables for faces | "material may select machines only from a closed registry with declared contracts" (build model); camera gestures structurally uncapturable by material (space-as-entity) | 4 specs · `face_projection:1072-1131` · `material_portal:312` · `verb_release:144` | LIVE; `resolve-binding` itself has no source caller now (drill + tests: DORMANT) |
| **Activation event** | `activation_event.cljc` 13.5K | the one closed form of an activation / rollback / pin / unpin act, its serializer and the legacy parser (one read shape, two source generations) | an act map → canonical bytes written into the object container, and parsed back | "POLICY merges by activation — malformed refused at the gate, one-act rollback" (build model); the form is durable bytes, so it is fixed | `matter_room` · `material_truth` · `material_circulation` · `material_portal` · `face_projection` · `facet_master` | LIVE |
| **Matter room** | `matter_room.cljc` 22.2K | pure addressing of a *room* (one entity/master world), normalization of the five acts (open · deviate · activate · rollback · say), deterministic resident projection (which agent seats appear) | room address + act params → normalized act + resident list (`:resident/time-ms` is an ordinal, not a time) | the portal surface was one conversation per room; the server needed a pure, testable core of the room idea (code; campaign P6–P8) | the five matter-room handlers (`server_jetty:804-1024`) · `material_portal.clj` · `face_projection` | LIVE |
| **Portal (pure) · inspector · reply-to-block** | `material_portal.cljc` 44.4K · `material_inspector.cljc` 7.3K · `reply_to_block.cljc` 3.9K | the pure half of the material portal (questions, anchors, canonical bytes, anatomy composition, render model, briefing); extraction/normalization of contribution stamps + wearer snapshots; the pure addressed-reply request / narrowing / prompt composition | served rows + room + stamps → a render model · a briefing text · a reply request | the portal was the x-ray of material (anatomy room, studio arc); "context comes by BRIEFING, assembled from the land's blocks, never by merging transcripts" (one canvas, many conversations) | `material_portal.clj:644-1098` · `face_projection:1809-1834` · `server_jetty:432,613` · `verb_release` · **`scene_store.cljc:684`** (the one client consumer) | LIVE; `reply-to-block/request` and `wearers-from-scene-store` have test callers only (DORMANT) |

---

## 3. AREA B — the server (`src/app/server_jetty.clj` + `src/app/server/**`) — every non-Rama waist box opened

One JVM. ~1.0 MB of server source of which ~0.7 MB is JVM-side organs and ~0.4 MB is the
five modules + their adapters (the floor, §4). Births span four generations: 2024-04 (the
front door), 2026-06 (object container, transcript pipeline), 07-03→07-12 (relation kernel,
trail view, arsenal, face projection, machine cut, code atoms), 07-18→07-31 (durable ground,
episodes, the material system, cascade), 08-20 (`block_edit.clj`).

Paths are relative to `src/app/server/` — most organs live under `rama/`. Each waist box is a
sub-section: first its **arrows** (the calls between its file-groups and to its neighbours,
each with the value crossing the seam and the anchor), then one row per file-group. One fact
frames the band: only `server_jetty.clj` requires `cluster.clj` — the door opens the handle
bundles (`face-projection-runtime` :477 · :608 · :1420-1478; `trail-runtime` :1397) and threads
`oc-rt` / `rk-rt` into every organ call; no organ reaches the cluster on its own.

### 3a. FRONT DOOR — one file; not opened further (its exits are the stubs on the picture)

**Exits.** episode ×3 → the SSE door (same file) · room open / rollback → `face_projection/serve
{:face :material-portal}` (:823 · :999) · deviate / activate / rollback / drill → `facet_master`
(`import-candidate!` :950 · `activate!` :975, :1010 · `ensure-master!` + `malformed-drill!`
:1503-1505) · deviate → `material_truth/deviate!` (:943) · say / halo · gold · autotag →
`material_circulation` (`bank-reference!` :1134 · `select-gold-receipt` + `bank-gold!` :530-539 ·
`autotag-material!` :375) · refresh → `block_edit/submit-block-edit!` (:786) · relation/assert →
the rk handle (`trail-runtime`, :1397) — the one door that skips every organ.

| file-group | file(s) | what it is | in → out | why | talks to | state |
|---|---|---|---|---|---|---|
| **Front door** | `server_jetty.clj` 79.5K (born 2024-04-20) | the one Jetty middleware `wrap-file-api` IS the route table (:1387-1604): ten POST doors matched by string on `uri`, every handler body in this file; `wrap-params` sits outside it so EDN routes read the raw body (:1606-1614); it alone requires `cluster.clj` | HTTP POST (EDN or form body; today only tests/curl) → EDN response; the episode turn answers with a `text/event-stream` body on the same POST | the 2024 file-explorer API file every generation bolted its doors onto; no GET survives because the read wire was the Electric websocket that went with the client | every file-group below · the five modules through the handles | LIVE (no production caller) |

### 3b. EPISODE LANE — the SSE door + turn loop → `episode.clj` → the transcript helpers

**Arrows.** SSE door → `episode.clj`: turn fields → {address · receipt · decision}
(`record-turn!` jetty:503 → episode:592) · conv + thread → {episode-id · fresh? · seed?}
(`current-episode!` :490 → :822) · prompt + session → argv (`summon-argv` :612 → :871) · cwd +
episode-id → distill summary (`post-turn-distill!` :704 → :942); the birth and geometry doors
→ `append-utterance!` (:731) · `settle-geometry!` (:355). `episode.clj` → transcript helpers:
jsonl file + stored offset → observations · next offset (episode:916-934, the one call site).
SSE door → cascade: `react!` `:episode/turn-durable` {object-key · source-unit-id · text ·
receipt · gold-receipt} (:589). SSE door → `face_projection`: `portal-briefing` (:556) ·
`episode-seed` (:607) → prose for the prompt. `episode.clj` → `block_distiller`:
`distill-conversation!` → distilled block rows + epoch bump (:942-961). `episode.clj` →
circulation: `receipt-from-context` (:519 · :670); SSE door → circulation: `select-gold-receipt`
· `bank-gold!` (:530-539). No arrow points back: neither `episode.clj` nor `transcript.clj`
requires the door.

| file-group | file(s) | what it is | in → out | why | talks to | state |
|---|---|---|---|---|---|---|
| **SSE door + turn loop** | in `server_jetty.clj` — `run-episode-turn` :439-720 · the episode routes :1522-1600 | the Jetty handler for the three episode doors: parses the body, asks `episode.clj` for the episode and the open turn record, banks the gold receipt, fires `cascade/react!`, builds the argv, spawns the claude CLI as a subprocess (`stream-cli-process`), writes its stdout as SSE frames, then calls `post-turn-distill!` and records the final status; birth and geometry are synchronous append + await imports answered in EDN | POST body {content-text · turn-id · time-ms} · birth {text · position} · geometry {cells · camera} → SSE frames (`:episode-durable` · `:run-done` / `:run-error` · `:episode-distilled`) · EDN 200/409 | "Episodes bound every CLI session" + "one canvas, many conversations" (settled 07-21/22); the reply streams back on the POST — no GET wire survives | `episode.clj` · cascade · circulation (gold) · `face_projection` (briefing · seed) · the handle bundles | LIVE (the canvas that posted is gone) |
| **episode.clj** | `episode.clj` 49.5K · requires core · oc · ocr · tid · block-distiller · transcript · circulation · util-fns — never cluster, no shared row | the lane's durable record: identity + row builders (:72-291) · `settle-geometry!` (:355) · the instance registry (:398-481) · `turn-record-request` / `record-turn!` (:500-592) · receipts (:608-645) · utterance import (:654-763) · the episode-chain decision (`decide-episode` · `current-episode!` · `note-episode-turn!`, :800-849) · `summon-argv` (:871) · harvest + distill (:895-961) | turn fields · utterance {text · position · scene-context} · geometry {cells · camera} · prompt / session + the `oc-rt` handed in → OC rows (SourceArtifact · DerivedUnit · SourceAnchor + `:episode-*` hints) · turn records + receipts · {episode-id · fresh? · seed?} · argv (`claude --session-id | --resume … -p … stream-json`) · distill summary {river · native · debris} | the lane is permanent, the session is not — the episode chain decides the session (D-core); seed by briefing, never jsonl replay; both actors durably recorded | transcript helpers · `block_distiller` · circulation (`receipt-from-context`) · util_fns epoch bump (:961) · the object container through the handle | LIVE |
| **Transcript helpers** | `rama/dogfood/transcript.clj` 70.8K — 6 fns used, all from one site (episode:916-934): `transcript-request` · `file-id` · `source-file-key` · `read-complete-appended-lines` · `import-observations-into-object-container!` · `append-and-await-object-container-file-state!`; its `transcript-module` is NOT deployed | the offset-cursor harvest of one jsonl file into the container | the episode's jsonl file + its stored offset → observations → OC import; file-state → the transcript-ops depot | one file, one conversation, never a sweep (episode §E) | object-container + transcript-ops through the handle | LIVE (the six fns); the rest of the file dormant |
| **Cascade** | `cascade.clj` 2.0K | one in-process declaration table of server acts against named triggers + `react!`, the one dispatch point (per-row future, isolated failures, declaration-order receipts) | a trigger (`:episode/turn-durable`) + payload → dispatched handlers; row #1 = `server-jetty/run-ambient-autotag!` (:349), named as a symbol — the only src pointer back into the front door | multi-cascade R1 (settled 07-26): server acts are data; dark capacity proven honest | the SSE door (`react!` :589) → circulation → LLM seat | LIVE |

### 3c. MATERIAL SYSTEM — SERVE HALF — block distillation → registry + dispatch ⇄ portal joining; composition beside

**Arrows.** `face_projection` → `block_distiller`: `river-page` (:516-574) — conversation-id ·
page → river blocks with time. `face_projection` → `material_portal`: `open` (serve injected,
:1808) → `:portal/result` · edn · render · briefing; `material_portal` → `face_projection`:
sub-serve ×5 (facet-materials · material-inspector · interaction-table · material-truth ·
material-experience, :746-811) + cascade-rows when anchored (:835) → each face's data-context
(the code's own comment says "the five sub-serves"; escape-gauge is a link only, :819, never
served inline). `face_projection` → `material_truth`: `served-instances` (:988) · `blast-radius`
· `master-announcements` · `case-report` · `world-at` (:1190-1210). `face_projection` →
`facet_master` (write half): `read-master` (:923) → compiled grammar + material.
`material_truth` → `facet_master`: `deviate!` · `pin!` · `unpin!` · `release-deviation!`
(:54-74, writes) · `instance-state` · `activation-trail` · `worn-at` (reads). The door →
`face_projection`: `serve {:face :material-portal}` (:823 · :999); the door → `material_truth`:
`deviate!` (:943) · `served-instance` (:410). Among the four, only `face_projection` requires
the other three; `material_truth` and `material_portal` require neither each other nor
`block_distiller`.

| file-group | file(s) | what it is | in → out | why | talks to | state |
|---|---|---|---|---|---|---|
| **Block distiller** | `object_container/block_distiller.clj` 85.9K · requires core · oc · markdown-adapter · tid · ocr · rk | distils object-container rows into block-shaped units: `classify-event` · `distill-event` (:171-537) · `import-request` (:690) · `distill-conversation!` (:942) · `refine!` · `assemble!` (:1058-1187) · `river-page` (:1397) · its runtime (:1539-1550) | OC reads through the `ocr` handle (conversation projection · sources · anchors · units · common material) + a conversation-id or a page request → distilled block rows as OC import requests + mechanical relation edges (:935-979) · bounded river pages of blocks-with-time | one distiller — the admission road decides, this shapes (code; no settled bullet names it) | ← `face_projection` (river-page) · ← episode (distill-conversation!) · ← cluster · machine_cut; → OC depot · rk depot | LIVE |
| **Face projection** | `rama/face_projection.clj` 102.2K · registry :1837 · `serve` :1910 · `portal-briefing` :2016 · `episode-seed` :1986; requires 25 namespaces incl. `assembly_adapter` (:38) | the read-only registry of the twelve face data-contexts and their dispatch: conversation (:495, river-page) · assembly (:808) · face-list (:858) · facet-materials (:1013) · interaction-table (:1096) · material-truth (:1142) · material-inspector (:1463) · cascade-rows (:1514) · escape-gauge (:1652) · block-truth (:1703) · material-experience (:740) · material-portal (:1794) | a face request {:face id · :params} (from the door, or recursively from the portal) · a briefing / seed ask from the SSE door → an EDN page (the data-context) · briefing prose · seed prose | face_projection owns SERVING, facet_master owns the mechanism (`material_truth:4-6`); briefings assembled from the land's blocks | `facet_master` (read-master) · `material_truth` · `block_distiller` · `material_portal` · `verb_release` (read-release :1113) · `assembly_adapter` · arsenal · rk · `cascade/rows` · `git_spine` | LIVE; no caller for `:assembly` · `:face-list` · `:escape-gauge` · `:block-truth` (DORMANT) |
| **Material portal (server half)** | `rama/material_portal.clj` 55.3K · never requires `face_projection` (serve injected, ns doc :12-17); the pure half is `shared/material_portal.cljc` | the clock-free join that opens one entity/master world: `open` (:672) → five sub-serves (+ cascade-rows when anchored; escape-gauge a link only, `:on-demand? true`) · `render` (:1083) · `briefing` (:1093); reads `facet_master/activation-trail` directly (:192) | a master-id + the injected serve fn → `:portal/result` (canonical, clock-free) · `:portal/edn` · `:portal/render` · `:portal/briefing` · `:portal/unanswered` | the portal "projects the material world inhabitably" (campaign gates) | `face_projection` (both ways) · `facet_master` (trail) · shared rows | LIVE — only via `face_projection` `:material-portal` |
| **Material truth** | `rama/material_truth.clj` 20.1K · "everything here is DERIVED … no truth of its own" (:11-12) | derived composition over `facet_master`: `deviate!` · `release-deviation!` · `pin!` · `unpin!` (:51-71, delegating) · `rebuild-instance-registry!` (:77) · `served-instance(s)` (:118 · :148) · `blast-radius` (:178) · `change-kind` · `announcement` · `master-announcements` (:249-317) · `case-report` (:354) · `world-at` (:398) | a master / instance id + `facet_master` reads + `ocr/read-revision` → served tiers · blast radius · announcements · case report · history | registry upkeep + served tiers + blast radius as composition, never custody (code) | ← `face_projection` · ← the door (:943 · :410) · → `facet_master` · `episode/register-instance-masters!` | LIVE |

### 3d. MATERIAL SYSTEM — WRITE HALF — the mechanism · the release chain · the circulation driver (+ the LLM seat beside)

**Arrows.** The door → `facet_master`: `import-candidate!` (:950) · `activate!` (:975 · :1010)
· `ensure-master!` · `malformed-drill!` (:1503-1505). circulation → `facet_master`:
`active-pointer-container-id` (:777) — the only call inside the box. circulation → LLM seat:
turn-run request → run · items · decisions (:655-698). The door → circulation: `bank-gold!`
(:530-539) · `bank-reference!` (:1134) · `autotag-material!` (:375, from
`run-ambient-autotag!`); cascade row #1 → the door → circulation. `verb_release` calls and is
called by neither box-mate — an island inside the write half; its only src caller is
`face_projection` (`read-release` :1113); it has no door seam.

| file-group | file(s) | what it is | in → out | why | talks to | state |
|---|---|---|---|---|---|---|
| **Facet master** | `object_container/facet_master.clj` 34.4K | the object-container adapter that imports a facet spec as a candidate master and activates / deviates / pins / releases pointer revisions; instance masters; the activation trail | a facet spec · the acts (`import-candidate!` · `activate!` · `deviate!` · `pin!` · `unpin!` · release · `ensure-master!` · `malformed-drill!`) → OC rows via `oc-rt` (SourceArtifact · Revision · ObjectContainer · SourceAnchor — masters · instances · pointers · the trail) | "served, revisioned, deviate-able, pinnable, previewable, activatable, reversible material" (editable-material, settled) | ← door · `material_truth` · `face_projection` · circulation · `cluster/facet-materials-ingest!` (:332, clj -X / REPL — no bin/land verb) | LIVE |
| **Verb release** | `rama/verb_release.clj` 8.9K | the fixed 8-node source-address release chain for the reply verb; `read-release` reads the release pointer | a release request → OC import rows (runtime handle); `read-release` → the release pointer | "material may select machines only from a closed registry with declared contracts" (build model) | → `code_atoms` (3 address helpers :34-39) · ← `face_projection` read-release (:1113); no door seam | read LIVE · write DORMANT (tests) |
| **Material circulation** | `rama/material_circulation.clj` 46.6K | the foreign-client driver writing receipts, semantic edges (gold · reference), machine records, autotag (via the LLM seat), as-of wears, "experience" — two handles threaded separately: `rk-rt` for edges, `oc-rt` for hint-only circulation records | turn receipt / context · wish→target · say→subject · autotag candidates → receipts · edges (`:references` · `:felt-at` · `:instance-of`) · circulation records · machine records · wears | "the map must not lie" — receipts carry provenance; autotag rides as cascade row #1 (multi-cascade R1) | ← door · episode (receipt-from-context) · → llm (:655-698) · → `facet_master` (:777) · relation kernel + object container through the handles | LIVE |
| **LLM seat** | `rama/dogfood/llm.clj` 99.1K · `llm-module` (4 depots · 26 PStates) NOT deployed; in-process via `delay` (jetty:347) | the in-process LLM turn-run runtime: request → claim → stream-json adapter → items / decisions / usage; backend `:claude`, auth `:subscription` | a turn-run request from circulation (`turn-run-request` → append → `claim-run!` → `run-adapter-turn` → `await-materialized`) → run rows · items · decisions · usage (in memory) · the autotag result | "the server floor (kernels, write organ, echo, llm seat) already exists" · model lanes ride subscriptions, never API keys (Sid) | `material_circulation` | LIVE helper; module not deployed |

### 3e. ANNOTATORS + THE EDIT ENTRY — three islands

**Arrows.** None among the three. In: the door → `block_edit/submit-block-edit!` (:786);
`cluster/machine-cut-bridge!` → `machine_cut/replay-wal!` (:529-536, inside `bin/land migrate`
— not `bin/land ingest`); `verb_release` → `code_atoms` (:34-39). `face_projection:324` reads
the constant `machine-cut-actor-id` — a def, not a call.

| file-group | file(s) | what it is | in → out | why | talks to | state |
|---|---|---|---|---|---|---|
| **Block edit** | `block_edit.clj` 2.0K (born 08-20) | the one server object-edit entry: `submit-block-edit!` → object-container stream → durable accepted/rejected decision | an object edit {target · content-text · idempotency-key …} → one OC edit request → a decision map; bumps the epoch atom on a fresh accept | the commit artery floor piece (edit in, durable accepted/rejected out) | ← `matter-room-refresh!` (jetty:779-786) · util_fns | LIVE |
| **Machine cut** | `rama/machine_cut.clj` 55.2K | conversation-pair annotator — pure core + LLM/WAL/relation shell; only `replay-wal!` is called | conversation pairs → `:pairs-with` edges (rk-rt) + an `.ednl` WAL (:546, a path no caller reaches; WAL OFF on the cluster, `cluster.clj:174,290`) | "Relations are typed edges" | ← `cluster/machine-cut-bridge!` (:529-536, bin/land migrate) · → relation kernel through the handle | DORMANT |
| **Code atoms** | `rama/code_atoms.clj` 61.8K | git-backed code ingestion / lineage + HEAD analyzer deriving `:requires` / `:calls`; only its three pure address helpers are used | a git HEAD → OC import rows (clj blobs) + `:requires` / `:calls` / `:supersedes` edges — when run (it isn't) | "External code is a view — address code, never copy it" | ← `verb_release` (:34-39) | DORMANT |

### 3f. INGEST / MIGRATION — a two-stage lane + one undeployed module

**Arrows.** `cluster/ingest!` (:292, `bin/land ingest`) → `ingest_watchers` `initial-sweep!` ·
`run-git-spine-boot!` (:304-307); `ingest_watchers` → `git_spine`: `replay-assert-log!` →
`spine-sync!` → `extract-session-joins!` (:279-303, in that order; `git_spine` never calls
back); `transcript_ingest`: called by nothing in src, calls neither neighbour.

| file-group | file(s) | what it is | in → out | why | talks to | state |
|---|---|---|---|---|---|---|
| **Ingest watchers** | `ingest_watchers.clj` 19.2K · requires ocr · markdown-adapter · assembly-adapter · transcript · face-arsenal · git-spine · util-fns | the boot-time sweep + the git-spine sequence, run only by `bin/land ingest` | files on disk (md · jsonl · assembly envelopes) → OC import rows + arsenal roster rows + lineage edges (one bundled runtime) | "Boot-time ingest is OFF the startup path (`bin/land ingest` explicit)" (durable ground) | → `git_spine` · object container + arsenal through the handle | DORMANT (explicit command) |
| **Git spine** | `rama/git_spine.clj` 33.2K | the three-stage git spine: assert-log replay → spine-sync → extract session joins | git history · the assert log (path nil = OFF) → commit-import rows + `:based-on` / `:produced` edges (one runtime, rk inside) | "No hand-rolled journals — the EDN WAL beside Rama is the recorded dead branch; the `.ednl` logs were bridges, replayed once, retired" | ← `ingest_watchers` · `read-commits` ← `face_projection` escape-gauge (no caller) | DORMANT |
| **Transcript ingest** | `rama/dogfood/transcript_ingest.clj` 47.0K | a whole Rama module — `TranscriptIngestModule` (:527, 4 depots · 12 PStates) — NOT deployed; the pre-object-container ingest kept as a draft | — | — | called by nothing in src (tests only); calls neither neighbour | DORMANT (a draft) |

### 3g. CLUSTER HANDLES — the one way down (one file opens the handles; three helpers the waist box listed)

**Arrows.** The door opens `face-projection-runtime` (:477 · :608 · :1420-1478) and
`trail-runtime` (:1397) and threads them into every organ — no other file requires
`cluster.clj`. `cluster.clj` → the five modules: depot · PState · query handles (the arrows to
the floor). `cluster.clj` entries → `ingest_watchers` (`ingest!`) · `machine_cut`
(`machine-cut-bridge!`) · `facet_master` (`facet-materials-ingest!`). `core.clj` ← 16
requirers across every box; `util_fns` ← 7; `objects.cljc` ← the door only, no fn called.

| file-group | file(s) | what it is | in → out | why | talks to | state |
|---|---|---|---|---|---|---|
| **cluster.clj** | `rama/cluster.clj` 29.1K | opens foreign depot / PState / query handles to the five deployed modules — Rama's `get-module-name` inside each bundle fn, `memo-total` caching the bundle (:187-200); `trail-runtime` merges relation-kernel + trail-view reads (:137) and is the `:rk-rt` of `face-projection-runtime` (:243); the explicit entries `ingest!` (:292) · `migrate!` (:582 → `first-light-ingest!` · `machine-cut-bridge!`) · `facet-materials-ingest!` (:332); WAL paths nil (:174 · :290); never launches a module | module vars → handle maps | "Rama is truth" + durable ground: the JVM is a client of a real single-node cluster; `bin/land` owns daemon / deploy / backup | ← the door · → the five modules | LIVE |
| **core.clj** | `rama/core.clj` 35.2K | the request / decision grammar — a library, not a handle: ids + hashes · defaults · unit-id routing · action-request / kernel-event · validation · accepted / rejected decisions · compat layer · canonical + fingerprint · replay / dedup gates · dead-letter · authz / audit (:51-804) | a request / event map → validated · decided · fingerprinted maps | one grammar for what the admission road accepts (code) | required by 16 files across every box | LIVE (library) |
| **util_fns.cljc** | `rama/util_fns.cljc` 0.4K | the one surviving atom: `!ingest-epoch-atom` (non-durable, reset on restart; quarantined as a transitional mirror in its own map) | — → "re-pull the faces" | a transitional mirror, named as such (code) | bumped by `block_edit` · episode · circulation · watchers · `facet_master` · `machine_cut` · the door | LIVE |
| **objects.cljc** | `rama/objects.cljc` 3.3K | not a kernel file: a CLI-provider argv builder (`provider-default-argv` for claude · codex · gemini) + `parse-claude-json-output` | — | — | required by the door; neither fn is called anywhere in src | DORMANT — an island |

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
- **File-group topology (server band, 2026-08-22):** ns-form requires and call sites per
  file-group from three read-only gathering passes (episode lane · serve half · write half /
  annotators / ingest / handles), spot-checked by the authoring session (`objects.cljc` users ·
  `verb_release`'s caller · the door's `face_projection` entries · `cluster.clj` requirers ·
  `bin/land` verbs · `cluster.clj` entry fns). Corrections that pass surfaced, applied in both
  twins: the episode lane uses **6** transcript helper fns (not 9), all from one site;
  `verb_release` has no door seam — its only src caller is `face_projection`; `objects.cljc` is a
  CLI-provider argv builder nothing calls, not a kernel file; the facet-master import entry is
  `facet-materials-ingest!` (clj -X / REPL), not a boot path; `machine_cut`'s caller is
  `machine-cut-bridge!` inside `bin/land migrate`, not `bin/land ingest`; `get-module-name` is
  Rama's, memoised one level up; `trail-runtime` merges relation-kernel + trail-view reads; the
  portal sub-serves five faces unconditionally (+ cascade-rows when anchored); `episode.clj`
  imports no shared row and never requires `cluster`.
- **Births:** `git log --diff-filter=A --follow` per surviving file, 2026-08-21.
- **Bytes:** `find src -type f -printf '%s %p'`, 2026-08-21.
- **Outranks this page:** the code, always; `docs/decisions.md` for the "why";
  `editing-waist-map.md` for the cut itself. This page holds no verdicts; the open
  question ("is this how I want it architected?") is Sid's, and the line that would make
  it judgeable is not yet settled.
