# What exists, graded — facts extracted in the visioning session of 2026-09-07

Facts only. What the code, the docs, the archive and two test runs say, with a receipt for
each claim. Nothing either chair concluded from these facts is in this file, on purpose.
The chairs' positions live in the session's chat and Sid's copies of it.

**Grades.** Every claim carries the highest grade its receipt supports and no higher.
- **read** — the shape is in source, read by a hunter at HEAD `5635613`.
- **data** — rows exist in the completed archive of 2026-08-24 (`/mnt/data/projects/Softland-archive-20260824T105543Z-497e11e`, source HEAD `497e11e`).
- **runtime-under-test** — boots and passes under the in-process Rama cluster the JVM suite uses, at HEAD `5635613`.
- **live** — reachable from the client a person uses today.
- **fit** — exercised, in a live loop, for the purpose a claim names.

Nothing in this file is graded **live** or **fit**. See §1.

## 0. Index — the facts that moved the session most

1. The below-the-waist client (`src/app/client/`) makes zero `/api/` calls; only font and image fetches. The door's ten routes have no caller in the new client. (read)
2. The full JVM suite at HEAD `5635613`: 477 tests, 5,817 assertions, 4 failures, 4 errors, all 67 namespaces loaded; in-process Rama clusters started in every shard; no failure in a `rama` namespace; failures in page (2 namespaces), worn, ingest, and one client namespace. No durable cluster process is running. (runtime-under-test)
3. No mark in either client kind has an id below the mark. Path: identity is the caller's, segments carry no id, knot ids are minted from the sample index and renumber on insert; a hit answers inside/boundary/outside for the whole record. Region3d: identity is the object id; a pick names a triangle index. (read)
4. A region3d mark is its own world with its own orbit camera, composited as a flat quad in land order; the law page names this the leak ("two windows onto one world are impossible"). (read; docs)
5. The engine's executor is pure, returns a `:subjects` provenance per result field, and its docstring says no read report is a dependency key. There is no mark id, no frame input set, no z-order or shared depth across marks, and no change tracking below "replay the record". (read)
6. Every server-derived id is content-addressed: `object-key = sha256(source-ref ␀ source-hash)`. Editing a file mints a new object key, and every unit and anchor under it gets a new id; continuity across versions is one pointer, latest-by-ref. No re-anchoring code exists. (read)
7. The container kernel decides a container's kind by one comparison, document or text-block. Codex's store probe imported a container of kind `:sphere-record`, read it back as `:sphere-record`, edited it, and read it back as `:document`. (read; Codex probe receipt)
8. The relation kernel has a fixed registry of 18 kinds; the asserter is inside a relation's identity; every relation row carries evidence source and anchor ids; an existing test writes two assertions over one pair, retracts one, and the other with its history stands. (read; runtime-under-test)
9. Facet masters: immutable content-hash-keyed revisions plus an active pointer whose source is an activation event (activate, rollback, pin, unpin; actor; scope; grounds from a closed set). Zero hits for memo, cache, stale, fresh in `worn/`. (read; runtime-under-test for the stored-definition cases Codex ran)
10. Zero hits, in the eleven 3D docs, the path doc, the server maps, the census, and `docs/decisions.md`, for: discourse graph, reasoning units, hypothesis (one incidental), instrument, measurement (two incidental), attachment, correspondence (outside the 3D law page's own nouns), freshness, pins at zoom depths, many agents editing, atom-to-cell zoom. Sid's own words on those exist on disk only in `docs/below-the-waist/3d/STARTER-iceberg.md`. (docs)
11. The four ingest adapters mint unit identity four different ways: markdown blocks by a six-digit position in file order; clojure forms by binding name; git commits by sha; transcript lines by byte offset plus line hash. None declares which rule it chose. (read)
12. The below-the-waist board file (`docs/below-the-waist/NOW.md`) is dated 2026-08-28, predates the whole 3D round, lists as open a question the law page answered as Position 4, and its inventory-check debt note is stale at HEAD. The two server maps are dated 2026-08-21 and predate the folder fold. (docs; runtime-under-test)

## 1. Tree and receipts

**HEAD** `5635613b621a35fc089f09bc779e8829f51a5369`; 28 dirty files at run time, none under `src/` or `test/`.

**Suite run** (this session, one run): `clj -X:test full`, six shards plus three flake lanes, wall 4 min 48 s.

```
 namespaces loaded        67 / 67, inventory check passed
 tests / assertions       477 / 5,817
 failures / errors        4 / 4
 in-process Rama          started in every shard (per-shard port ranges; listeners observed)
 durable cluster          none running; nothing on the UI port; /mnt/data/rama holds
                          data with no process serving it
 repo files touched       none (git status identical before and after)
```

Failures and errors, all at HEAD:
- `app.server.page.face-projection-test` — 2 failures: the registry no longer exposes `:assembly` (`face_projection_test.clj:110,121`).
- `app.server.page.material-portal-test` — 2 errors: `Cannot open <nil> as a Reader` in the matter-room and halo tests (`io.clj:291`); path resolution untraced.
- `app.server.worn.space-material-test` — 1 failure: expects 8 facet specs, 7 registered (`space_material_test.clj:183`).
- `app.server.ingest.git-import-test` — 1 failure + 1 error: fixture path under a docs tree removed by commit `91dee80` (stale fixture, not server logic).
- `app.client.region3d.on-plane-test` — 1 error: `:text/layout-provider-required` (`layout.cljc:967`).

The runner (`test/app/test_runner.clj`) has no directory or namespace selector; tiers are hand-registered (39 pure, 5 shared-cluster, 23 isolation, 3 flake). Log: the session scratchpad, `server-tests.log`.

**Codex's independent runs at the same HEAD** (its receipts, in its detached scratch worktree `/mnt/data/projects/Softland-claim-probe-20260907/`, spot-checked here for the kind boundary only):
- client: 79 tests, 706 assertions, green; Node benches green (46 + 55 checks).
- cold execution: a withheld brush saved in one JVM and resumed in another; state, history, results and subjects matched the eager run.
- store probe: four cases, 247 assertions, green — independent asserters; local definition change; pinning; storing and reconstructing a sphere reach computation.
- the sphere round trip: reach record and inputs stored through the existing import path as an encoded string, queried back, reconstructed into a map the executor ran, region retained with subject; radius edited 150→140; area 67,433.94→59,101.60 mm²; original record retrievable; retained region unchanged; subjects differ; new revision points to the previous. **Kind boundary:** imported `:container-kind :sphere-record` reads back as `:document` after the edit (`claim_probe.clj:63,82`; `object_container.clj:1493`).
- Codex reports one search accidentally matched `env.clj` and returned only its namespace line.

## 2. Client kinds as built (read; JVM tests as noted)

### 2.1 path (`src/app/client/path/`, 14 files, 11 `.cljc`)
- Pipeline order, source to pixels: records → construction → source → width → value → stroke → nib → component → pack → frame → placements → push.cljs → renderer.cljs; `surface.cljc` is a CPU side branch.
- Sources are pen samples, a rectangle's numbers, or designer anchors; all compile to one value (`source.cljc:9-10,198-207`). Stored value: subpaths with `:closed? :start :segments [{:kind :line|:quad|:cubic ...}] :knots [{:id :width :pressure :time}]`; local units are CSS pixels at zoom 1, Y down (`value.cljc:9-17`).
- Camera enters at exactly two places: device-unit width divides by scale (`component.cljc:99,105`) and the pack's scale bucket (`pack.cljc:28-48`). Zoom inside a bucket is a camera move; across a bucket is a repack (`pack.cljc:12-15`). `value`, `stroke`, `nib`, `source`, `width` never see the camera.
- Identity: `[:path/material-id :path/revision]` supplied by the caller; "Identity and authored source are caller concerns" (`construction.cljc:40`). Segments carry no id (`value.cljc:108-110`). Knot ids are `(str "s" i)` from the sample index (`source.cljc:128`); anchors carry authored ids (`records.cljc:116-119`).
- Hit: `component/classify` → `:inside | :boundary | :outside` for the whole record (`component.cljc:204-213`); no segment, point, or id in the result.
- Emits rows `{:rect :slot :color :rule :index :clip}` to the renderer (`push.cljs:12-18`). `records/pickup` is the colour-pickup brush fixture (samples the surface it paints into), not a hit record (`records.cljc:35-58`).
- No file in scope touches server, data, claim, or bind. No probe/dead/temporary markers.
- Tests (JVM): value 7, source 6, stroke 8, nib 3, pack 8, component 7, construction 6, frame 8, pickup 2 (checkpoint at step 12, byte-identical resume; edit kinds classify as `:consumed-items-differ` / `:recipe-differs` / clean resume).

### 2.2 region3d (`src/app/client/region3d/`, 6 files)
- A region is its own world: own `:view` orbit rig `{:pivot :distance :yaw :pitch :lens}` (`component.cljc:46-52`), rendered to its own lease, composited as one quad through the land camera's pan/zoom (`renderer.cljs:245-267,1386-1408`). The land camera never becomes the 3D camera.
- Scene: a map of object-id → object; kinds `#{:mesh :light :empty :text :ink}`; parent hierarchy; per-object TRS (`component.cljc:24,72-76,501-517`). Geometry is six primitives or supplied indexed triangles; no instancing, no volumes, no line kind (`component.cljc:373-408`). Limits: 65,536 vertices, 131,072 triangles per mesh, extent 1e4, 8 lights.
- Identity: `:object/id` only. A pick returns `{:route :object :object-id :point3 :normal :t :triangle-index :boundary?}` or region background (`scene.cljc:965-983`). One BVH per derived scene; text and ink placements are not in it (`scene.cljc:526,970-978`).
- Change: `evaluation-key` splits static component from transforms; `:full` / `:none` / `:transform`; transform changes refit the BVH over affected descendants (`scene.cljc:1071-1125,926-964`). Camera is excluded from the key. Frame key: `[region-id revision container zoom dpr session-revision]` (`frame.cljc`).
- Depth per region lease, cleared per pass; transparency by per-object sort, meshes and ink as separate groups; docstring: "Per-object sorting cannot resolve intersecting transparency exactly" (`renderer.cljs:894-914,1291-1317`). PBR (GGX, Smith, Schlick, Lambert), one directional 2048 shadow map, PBR-neutral tone map.
- `:provenance {:asserted-by :act}` is required on every object and never read; `:ref {:address}` on text/ink is not resolved here (`component.cljc:460-484`; `on_plane.cljc:7-8`).
- on-plane = 2D marks on an object-local z=0 plane inside the region; ink is the supported GPU kind; resolved text returns `:unsupported-kind` (`on_plane.cljc:67-132`; `on_plane_renderer.cljs:14-15`). `region3d/on_plane` requires path.component, path.pack, text.layout; nothing outside region3d and harness requires region3d.
- Tests (JVM): component 3, scene 8, on-plane 2, path-placement 1, frame 2. `renderer.cljs` and `on_plane_renderer.cljs` have no JVM test.

### 2.3 engine (`src/app/client/engine/`, 15 files)
- `schema.cljc` is validation mechanics only; "Family schemas supply vocabulary" (`schema.cljc:8-9`). The one declared engine schema is the transform group (`transform.cljc:65-79`).
- Executor (`executor.cljc`): pure; "Holds no state or clock"; `run` returns complete/error/refused, a demanded read, or a continuation at `:until` or a pending/needs-policy read (`:317-322`); complete carries `:results :subjects :state :history :log :reads :read-order :unread` (`:296-300`); `:subjects` = per return field, the recipe, roots and reads that produced it (`:200-216`). "No read report is a dependency key" (`:7`); the read observer is diagnostic (`:139-140`). `resume` replays pre-loop work and executes remaining items, guarded by equality on the whole recipe (`:344-354`).
- Required by exactly `harness/pickup.cljs` and `harness/path_production.cljs`; no kind namespace requires the executor. Compositor is required by `region3d/renderer.cljs` and harness only.
- Surfaces: `:surface/id` + `:revision` + `:key`, parent-key chain per paint; a pickup names contributors as `"surface-id@revision"` strings (`surface.cljc:36-40,87-88,105`).
- Three unjoined id domains: surface id/revision/key; transform group-id + buffer-index; region id in leases. No mark id. No frame function or frame input set; the camera is an argument to two projection functions and a GPU buffer.
- No z-order, no shared depth across marks; depth exists only as a per-region-lease attachment (`compositor.cljs:518-543`). The only `dirty` is the coverage atlas row range; no change tracking of marks.
- Tests (JVM): executor 6, executor-pending 7, surface 4, transform 4, rungs 7, schema 3, color 2, limits 2. None for compositor, coverage, leases, buffer-pool, device (`.cljs`).

## 3. The 3D and path docs, as they stand (docs)

- Law in force per `docs/decisions.md` "Tools are records over a vocabulary (settled 2026-09-06)": `docs/below-the-waist/path-kind/path-kind.html` and `docs/below-the-waist/3d/3d-kind-2.html`.
- `3d-kind-2.md`: a space is the entity (planar or spatial); things live in spaces; a view is a camera onto a space and belongs to a person; a portal is a thing in one space that shows a view of another (`:53`). "Today's Region3D is a portal on the page with the whole world stored inside it, so two windows onto one world are impossible" (`:73`); "Two are the leak: the world inside the window, and render resolution inside the value" (`:392`). "Store what the source made; make triangles at the camera's tolerance" (`:67-69`). One question protocol; not every shape has every answer (`:65`). Nouns at `:370-390`: Face (named by the construction step, with a lineage and a frame, Position 10); Attachment (content, face, authored domain, anchor rule, a clause per lineage on split/delete/merge, what it reads as, whose it is); Correspondence (three relations: operation provenance, correspondence between evaluations, attachment preservation); Location (a hit's second half, status per requested quantity, pending says which); Surface region (a retained result with its subject kept); Snapshot (what a stateful read pins; a coherent set of the read's dependencies, not a global scene revision; an answer for a superseded snapshot is discarded or recomputed, never relabelled). Fix list: the split-and-cap rule is not built (`:631`). Ceilings table with a "where it cracks" column (`:436-452`).
- `3d-object-space-and-query.md` (uncommitted, Codex lane) ends on: "When a parametric edit splits, merges or deletes the face supporting the stroke, what result must the geometry operation return" (`:334-346`).
- `3d/HANDOFF-5.md` (composer): "No decision remains open for Sid"; Position 9 closed 2026-09-06 17:42; items 13/14/16 hold retained regions with subject, the brush's read as the compositor's sample through a binding with the executor's obligations, and the executor over the bench's records; still owed: a general smooth/trimmed host distance, the coverage lane's consumer, a 0.10 mm parity residual, region as a clip on a read, region algebra, a painting that covers a region, travel, query modes with a translucent thing, a shadow, a portal back.
- `production/DESIGN-1.md` §12 (per Codex, updated 2026-09-07): the frame caller grants per frame and the frame never waits; a suspended continuation is a value the session holds; a shared read is keyed by its full inputs, distinct from the executor's consumer-transaction request; request/result storage, the runner, and ownership by held keys remain to be wired.
- `path-kind.md`: three field tools compile to one value (a path, a paint, an identity); "A function is data only if something below can run it. That runner is a fifth piece, and the picture did not have it" (`:217-233`); the flat-triangle renderer lane is rejected, "settle the contract first" (`:366-379`).
- `3d/STARTER-iceberg.md` (2026-09-07 14:06): Sid's own five messages from the design session, verbatim, one per turn; the first asks why "the brush that reads the coating on the sphere before it paints" is the hardest.
- `3d-ceilings-starter.md`: six ceilings (parametric CAD, procedural DCC and film, real-time open world, BIM, planet-scale geospatial and twins, fields and scans) and four things 2D never had; warns the 2D round stamped every ceiling met and each broke.
- The three feedback files converge on "make one definition concrete"; `feedback-a85fd1e.md`: one answer vocabulary should not require every representation to have every answer; separate authoritative information from derived work; derived-part identity needs correspondence and attachment policy.
- Term sweep over these docs: see §0 item 10.

## 4. The server as built, by folder (graded per item)

Tier order enforced by `bin/server_tiers.clj`: door/tools 5 → page 4 → episode 3 → worn 2 → ingest 1 → rama 0; edges point down; five allowlisted exceptions E1–E5 (material-truth→episode, binding-material→verb-registry, cascade→server-jetty, ingest/transcript→llm, rama/transcript-ingest→ingest/transcript); `app.server.env` pinned to rank 0 and excluded from the scan.

### 4.1 rama — the kernels (read; data; runtime-under-test green)
- Four modules declare the archive's 44 PStates: object-container 29, object-container-transcript-ops 3, relation-kernel 8, face-arsenal 4. `transcript_ingest.clj`'s twelve PStates are not among the 44 (in-process store; its own module is a parked draft). `ingest_epoch.cljc` self-declares non-durable, session-local.
- Container row: `container-id container-kind object-key visibility source-id source-anchor-id source-unit-id document-container-id current-revision-id current-content-text current-content-hash created-at-ms created-by event-id` (`object_container.clj:108-111`). Kind: `(if (= document-id container-id) :document :text-block)` (`:1493-1495`); visibility hardcoded `:private`.
- Revision row: `revision-id container-id parent-revision-id content-text content-hash order-key created-at-ms created-by event-id` (`:113-115`); minted on every accepted `:object/edit` (`:1510-1518`); no content comparison; rejections are validation, `:target/not-found`, `:edit/stale` (`:1432-1446`); stale = a lower-or-equal `edit-seq` from a different idempotency key on the same lineage (`:1394-1402`).
- Decisions: request and decision rows under one audit id; gate `decision-dedup-gate` returns `:proceed | :replay | :conflict` from a stored decision plus a sha-256 fingerprint of the canonical request (`envelope.clj:552-612`); the idempotency key is client-supplied and a blank one is rejected (`object_container.clj:428,596`); `decided-at` copies the request's clock so a replay is byte-identical (`envelope.clj:361-363`).
- Derived units: `unit-id document-container-id source-id unit-kind block-path parent-slot-id source-anchor-id derived-content-text derived-content-hash distiller-id distiller-version event-id` (`:117-120`). Graduation = first edit of a derived unit with no graduation row: mints `oc:block:<object-key>:<unit-local-id>`, event `:object/graduated`, anchor copied, outline node and composition edge rewritten (`:1448,1467,1530-1545`).
- Native identity claims: claim-key = container-id = source-native-id, status literal `:accepted`, asserted by the import path from the container row (`:1291-1307`); used for re-import compatibility (`:1309-1327`); a conflict rejects. They pin one container to its source-native id; they do not declare two containers the same thing.
- Sources: artifact (immutable bytes), version (one per ref+hash; latest-by-ref), anchor (`target-kind target-id source-id source-ref source-hash start-offset end-offset block-path`), thin back-reference rows by source. Ids: `source-ref-key = sha256(source-ref)`, `object-key = sha256(source-ref ␀ source-hash)`, `source-id = "src:"+object-key`, `document = "oc:doc:"+object-key`, `source-anchor-id = "sa:"+target-id` (`:206-232`).
- Relations (`relation_kernel.clj`): row `relation-id relation-kind from to asserter-actor-id asserter-type relation-status evidence-source-id evidence-anchor-id note first-asserted-at-ms status-changed-at-ms event-id request-id envelope-actor-id envelope-actor-type` (`:271-277`); `relation-id = "rel:" + sha1(kind, from-kind, from-id, to-kind, to-id, asserter-actor-id)`; fixed 18 kinds `:based-on :produced :built-over :new-direction :dead-end :elaborates :references :confirms :refutes :supersedes :requires :calls :grounds :assembled-from :refines :pairs-with :instance-of :felt-at` (`:62-87`); unregistered kind = durable rejected decision (`:350`); target descriptor kinds `:container :source :git-commit :doc-file :conversation :none` (`:237-240`), not a span inside a container; status `#{:asserted :retracted}` with a status log; activity buckets by arrival UTC day; two clocks kept (claimed vs arrival). Test: `test/app/server/rama/relation_kernel_test.clj:255` — two assertions over one pair, one retracted, the other and its history intact.
- Trail view: mirror-only page bundles, pure `assemble-bundle`, caps `{:children 50 :relations 200 :text 4000}` with omission rows; thirteen mirrors over twelve object-container PStates plus transcript-file-offsets (`trail_view.clj:53-64,291-304,490-535`). Face arsenal: `face-name object-key import-key status valid? source-ref registered-at-ms`; wear events/counts/journal; `record-wear!` (`face_arsenal.clj:269`) is called only from tests.
- Transcript identity: `chat:sha256(source:conversation-id)` object key; per-message, per-tool-call, per-tool-result containers; source line key `<source>:<file-id>:<byte-offset>:<line-hash>` (`transcript_identity.clj:8-48`).
- Grep in scope: `:question/id nil` in the envelope's default context (`envelope.clj:87`) is the only "question"; "claim" = lease tokens and native identity claims; "evidence" = the two ids on relation rows; hypothesis, snapshot, continuation: not found.

### 4.2 ingest — the world into rows (read; git-import test has one stale fixture)
- Markdown: units `:markdown/heading | :list-item | :paragraph`, flat blocks with `:parent-block-path`; unit id `du:<object-key>:markdown-block-v0:<%06d>` — the index is positional (`markdown_adapter.clj:17-19,84-94`).
- Clojure: one unit per top-level form, closed kind table (`clojure_adapter.clj:62-78`); unit id `du:<object-key>:clojure-form-v0:<block-path>` where block-path is the binding name (deduped `~2`), `ns` for the ns form, `%06d` for unnamed forms (`:135-164,283-285`).
- Git: the unit is a commit rendered as canonical text and pushed through the markdown builder; ref `git-commit:<sha>`; request ids pinned to the sha so re-runs are byte-identical (`git_import.clj:48-53,131-186`). Code import: forms inside blobs, ref `git-blob:<sha>`; relations `:supersedes :requires :calls` (kondo).
- Transcript: per JSONL line; dedup key file-id (device+inode) + byte offset + line hash; containers per conversation, message, tool call, tool result; message identity = the CLI's message uuid hashed, else the line key (`transcript_adapter.clj:225-360`; `transcript.clj:306-331`). A second lane distills messages into sense-blocks with positional ids (`transcript_import.clj:30-40`). The model name was not found as a row field (UNCERTAINTY).
- Anchors: `[start, end)` offsets into the exact source version plus that version's hash and a locator (`markdown_adapter.clj:301-312`; transcript anchors by byte range). No re-anchoring function in scope; grep for "latest" over the seven adapter/importer files is empty.
- Convergence: import keys `imp:md|clj|tr:...` from ref-key and hash; relation edge keys `sha256(from ∥ kind ∥ to)`; `edge-already-asserted?` pre-check; git replay from `data/relation-assert-log.ednl`.
- Typed reasoning units: none. Zero hits for question, hypothesis, discourse, supports, opposes, informs. The only typed relations minted are mechanical (`:based-on`, `:produced`/`:grounds` from tool use, `:supersedes`, `:requires`, `:calls`).
- Watchers: JDK WatchService over roots, `.md` and `.jsonl` only, 500 ms debounce, single-thread importer; write nothing themselves; bump the in-memory epoch on accept (`ingest_watchers.clj:36-100`).

### 4.3 worn — revisioned material (read; Codex's stored-definition cases green)
- A facet master = a static spec + durable revisions of an EDN source form; revision id is content-hash keyed and knowable before append (`facet_master.clj:462-469`); instance masters add a subject digest. Active pointer is a separate container whose source text is an activation event (`facet_master.clj:29-30,252,334-341`).
- Activation event: `:activation/revision-id :kind :scope :actor :time-ms :grounds`; kinds `:activate :rollback :pin :unpin`; scopes `:scope/all-unpinned :scope/subject`; grounds `{:ground/relation #{:grounded-in :responds-to} :ground/kind #{:experience :deviation :conflict} :ground/id}` (`activation_event.cljc:14-49`). Trail order is causal along parent-revision-id, not clock-sorted.
- Resolution: valid instance revision → shared active → code floor, total; malformed material degrades to the floor (`facet_engine.cljc:175-177,370-390`). No invalidation: "the pointer is what is worn ... no fan-out ever happens" (`material_truth.clj:163-176`). Deviations store a snapshot of the inherited form; the diff is computed on read (`facet_engine.cljc:305-307,461-466`).
- Zero hits in `worn/` for memo, cache, stale, fresh, question, evidence.
- Contribution stamp: `{:material/subject :attachment :master :revision :site :role :slot}` (`facet_engine.cljc:194-207`). Binding rows are gesture→verb dispatch `{:binding/gesture :phase :modifiers :verb :priority}` under sites, with no on-change clause and no target revision (`binding_material.cljc:43-49,127-146`). `provenance_material` is a colour tint. `positioned` is reply-birth policy, `space` is zoom bounds; neither holds a block's coordinates.
- `material_truth` writes only through facet-master; its index is "not the truth". Test: `space_material_test.clj:183` expects 8 specs, 7 registered.

### 4.4 episode — agents as participants (read; dogfood-llm tests green in this run)
- Turn cell: `{:world-id :turn-id :source-unit-id :content-text :content-hash :position :status :time-ms :prev-turn-id :thread-id :episode-id :receipt}` written as an import before the agent is spawned; "later edits/moves never rewrite what the resident answered" (`episode.clj:465-505,537-551`). Actor defaults to sid/human; a machine resident passes its own actor through the same lane (`:89-108`).
- Receipt: identities, transforms, placement and material revision references only; `:receipt/worn-materials` rows `[:material/subject :attachment :master :revision :site :role :slot]`; `resolve-receipt-as-of` from activation history, "no current-state snapshot is substituted" (`material_circulation.clj:39-48,88-137,884-898`).
- Episode = a CLI session lane with a 1 h idle boundary; durable chain is the turn cells' `:episode-id` (`episode.clj:753-826`). The model's reply enters via a post-turn harvest of the CLI's jsonl through the transcript path (`:871-915`).
- `llm.clj`: lanes `#{:codex :claude}`, auth `#{:subscription :api-key}`, 23 PStates, run row with `:status :pending`, claims as executor locks (`claim-run!`), pending-by-task, executor-active-runs, dead letters, controls `#{:approval/resolve :turn/cancel :compact/request :turn/steer}`; idempotency key defaults to `"space-event:<turn-id>:<bundle-id>"` (`:199-201`); model may not be set in the payload and is observed from the system-init line; cost per result and rolled up per thread; a process that exceeds its timeout is destroyed and the run fails with a stderr tail; no resume of a dead run found (`:2141-2183`; UNCERTAINTY on the module fold).
- `machine_cut.clj`: a model-driven `:pairs-with` annotator; input hash over ordered (event-uuid, unit-id, sha(text)); run hash = sha(address, input-hash, annotator-version, salt); `verify-bundle-hash` fails the run `failed-stale` if material changed; WAL-first; `replay-wal!` re-applies with zero adapter calls; annotator version bump = a new asserter; the entry point has no caller in `src/` (`:114-204,376-395,716-736,889-899`).
- `material_circulation.clj`: gold `:references` edges with actor "sid"; silver `:felt-at` and `:instance-of` proposals with actor `llm:material-autotag/v1`; three-stage machine records with `:agent` actor type and `:llm` epistemic asserter. `cascade.clj`: one declared row, turn-durable → ambient autotag.
- Grep: question/hypothesis 0; "claim" in llm = executor lock; "evidence" = the two ids on edges; continuation 0.

### 4.5 page and door (read; two page namespaces red at HEAD)
- Routes (`server_jetty.clj:1396-1577`, all POST): `/api/relation/assert`, `/api/matter-room/{open,deviate,activate,rollback,say}`, `/api/material/facet-master/drill`, `/api/episode/utterance` (SSE stream; spawns `claude`), `/api/episode/block-birth`, `/api/episode/geometry`. No websockets. No route serves a face projection; `serve` is reached only inside two matter-room handlers. The client mints `request-id`, `idempotency-key`, `edit-client-id`, `edit-seq`, `block-id`, `settle-id`, `turn-id`; no session, no auth.
- Freshness signal: one session-local counter, `ingest-epoch/!ingest-epoch-atom`, bumped on accepted births and on tombstone settles (`:1563,1596`).
- `block_edit.clj`: `append-block-edit-request-durably!` then `read-decision`; epoch bumps on accepted non-replay (`:32-38`).
- `material_portal.clj`: `open` is one function call fanning out to five sub-projections; returns one flat map; `:portal/query-plan` is a hand-written declaration, not a computed key; no input hash, no cache; not stored as a row (`:619-989`).
- `portal_questions.cljc`: seventeen literal rows in a vector, each `:question/ask` prose with a path into the portal result; "answered" = the path is present; no answer state stored (`:126,:879-891`).
- `verb_registry.cljc`: a closed map of ~30 verbs in code; effect classes `#{:pure-projection :durable-via-request :external-via-derived-worker}`; continuations `#{:invoke :begin :move :end}`; `:verb/required-args` declared; no output declared. `verb_release.clj`: an eight-node EDN chain (wish → implementation → test-receipt → build-receipt → verb → binding → activation → worn) imported as source material.
- `face_projection.clj`: a registry map of projection functions; a face is a name resolved to a projection kind; projections declare no inputs; every projection stamps a render clock the portal strips.
- `cluster.clj`: foreign depot/pstate/query handles by module name; first-success memo; no queue or retry beyond that; conductor host localhost, port from Rama's default.

### 4.6 The archive (data)
44 PStates, 491,561 entries, 866,865 leaves, cluster of 5 modules RUNNING at export. Top by entries: requests-by-audit-id 63,938; decisions-by-audit-id 63,938; events-by-id 63,908; revisions-by-id 56,008; source-anchors-by-target 48,533; composition-parent-by-child 45,695; derived-units-by-id 43,211; import-completions-by-key 13,902; source-artifacts-by-id 6,075; containers-by-id 5,322; native-identity-claims-by-container 5,302; relations-by-id 4,437; source-versions-by-ref 4,113. Tail: unit-graduations-by-id 144; faces-by-name 1; wear events 13 across the four face-arsenal PStates.

### 4.7 Settled law that names these (docs; `docs/decisions.md`)
- Provenance first-class everywhere: `asserted-by` (sid | llm | import) rides every relation; machine output visibly distinct; silver/gold tiers (`:285-287`).
- Relations are typed edges with a closed kind enum grown by one reviewed line; "All discourse structure rides this kernel" (`:288-290`). Two clocks, claimed vs arrival (`:291-293`).
- Episodes: a lane is permanent, the CLI session is not; an hour of silence closes the episode (`:346-365`).
- "The unit is the sense-line: episodes and their marks. Files, sessions, and commits are one evidence lens, not the unit" (`:149-156`).
- Glossary renames: "entity template ← face: an authored tree of entities stored as data, with declared inputs, instantiated at runtime" (`:551`); "geometry generator ← verb" (`:549`).
- Waist round two (`:455-530`): the two residency axes (granularity; camera as input); the engine's list including the uncommitted store, the derivation cache, the hit builtin; "description in, geometry out, or geometry in, pixels out, without knowing what the thing is or what a gesture means"; geometry generators are data; rows are truth and derived rows; the boundary is a row boundary; dead means wrong form; one vocabulary. The pointer, uncommitted store, wire, clock and culling are named as engine code that does not exist yet.
- Tools are records over a vocabulary (`:805-834`): a construction is the kind's input; every result keeps the subject it was built from (record, revision, snapshot); a recipe is a record; one executor, several runners (the CPU walker, a compiled GPU tier, the store's own derivation); a tool never enters git; a capability is code below the waist; nothing in a record is a program.
- The editing waist and the clean-up-first ruling (Sid, 2026-08-19, `:372-400`): every file passes one test — floor piece, plumbing under the floor, kernel, or material above the waist — or is deleted or rewritten as material; "only when the tree passes ... does building on top begin"; the data already written is preserved as is.
- The caching rule (Sid, 2026-09-06, verbatim, `docs/below-the-waist/two-chairs.md:114`): "caching is never an option it means the underlying thing is as it should be and then still need more performance therefore we nede to do caching on top"; applied: a memo's key is the level's full input, never what a run happened to read and never a hash.
- Open questions still listed: "Confidence/credential algebra for the trail→code join"; "Question-as-first-class-unit design (design track)" (`:910-911`).
- Absent from `decisions.md`: graduation, derived unit, identity claim, source anchor, machine cut, archive (the archive's custody rule lives in `CLAUDE.md`). "Native discourse protocol" appears once, as an analogy (`:801`).
- `docs/below-the-waist/NOW.md` (2026-08-28): "Next: the same sort over `src/app/server` + `src/app/shared`" (`src/app/shared` does not exist at HEAD).

## 5. Absences, in one place
Zero hits across the scoped 3D docs, path doc, server maps, census, `decisions.md`, and the six server folders, unless noted: **attachment**, **correspondence** (outside the 3D law page's nouns), **freshness**; **hypothesis** (one incidental line); **instrument** (glossary retirement and one "fence instrument"); **measurement** (two incidental); **continuation** (only verb phase hooks and the executor); **pin** (activation pin/unpin only); **zoom** (client and space bounds only); **question** (the portal's seventeen literals; `:question/id nil`); **claim** (native identity claims; executor locks; facet claims); **evidence** (the two ids on relation rows).

## 6. Receipts index
```
client
knot ids from sample index            src/app/client/path/source.cljc:128
segments carry no id                  src/app/client/path/value.cljc:108-110
identity is the caller's              src/app/client/path/construction.cljc:40
path hit = inside/boundary/outside    src/app/client/path/component.cljc:204-213
camera enters at two points           src/app/client/path/component.cljc:99,105 · pack.cljc:28-48
pick returns triangle-index           src/app/client/region3d/scene.cljc:965-983
own world, orbit camera               src/app/client/region3d/component.cljc:46-52
composited as a land quad             src/app/client/region3d/renderer.cljs:245-267,1386-1408
provenance slot, never read           src/app/client/region3d/component.cljc:480-484
no instancing, volumes, lines         src/app/client/region3d/component.cljc:373-408
text unsupported on GPU               src/app/client/region3d/on_plane_renderer.cljs:14-15
change classifier                     src/app/client/region3d/scene.cljc:1071-1125
subjects per result                   src/app/client/engine/executor.cljc:200-216
no read report is a dependency key    src/app/client/engine/executor.cljc:7,139-140
contributors as id@revision           src/app/client/engine/surface.cljc:105
no shared depth across marks          src/app/client/engine/compositor.cljs:518-543
server
commit gate proceed/replay/conflict   src/app/server/rama/envelope.clj:587-612
request fingerprint                   src/app/server/rama/envelope.clj:552-560
revision minted per accepted edit     src/app/server/rama/object_container.clj:1510-1518
container kind is one comparison      src/app/server/rama/object_container.clj:1493-1495
stale edit by lineage sequence        src/app/server/rama/object_container.clj:1394-1402
graduation                            src/app/server/rama/object_container.clj:1448,1467
native identity claim                 src/app/server/rama/object_container.clj:1291-1327
object key = sha(ref ␀ hash)          src/app/server/rama/object_container.clj:206-232
relation row and id                   src/app/server/rama/relation_kernel.clj:271-277
relation kinds, fixed 18              src/app/server/rama/relation_kernel.clj:62-87
target descriptor kinds               src/app/server/rama/relation_kernel.clj:237-240
two asserters test                    test/app/server/rama/relation_kernel_test.clj:255
trail view mirrors and caps           src/app/server/rama/trail_view.clj:53-64,291-304
markdown block id is positional       src/app/server/ingest/markdown_adapter.clj:84-87
clojure form id is the name           src/app/server/ingest/clojure_adapter.clj:151-164
transcript line key                   src/app/server/rama/object_container/transcript_identity.clj:42-46
anchors per source version            src/app/server/ingest/markdown_adapter.clj:301-312
facet revision content-hash keyed     src/app/server/worn/facet_master.clj:462-469
activation event fields               src/app/server/worn/activation_event.cljc:14-49
blast radius, no fan-out              src/app/server/worn/material_truth.clj:163-176
three-tier resolution with floor      src/app/server/worn/facet_engine.cljc:370-390
contribution stamp                    src/app/server/worn/facet_engine.cljc:194-207
turn cell, durable before spawn       src/app/server/episode/episode.clj:465-505
receipt resolved as-of                src/app/server/episode/material_circulation.clj:884-898
llm idempotency key                   src/app/server/episode/llm.clj:199-201
run dies, no resume                   src/app/server/episode/llm.clj:2141-2183
machine cut run hash, failed-stale    src/app/server/episode/machine_cut.clj:162-204
page edit = two kernel calls          src/app/server/page/block_edit.clj:32-38
global epoch as freshness             src/app/server/door/server_jetty.clj:1563,1596
route table                           src/app/server/door/server_jetty.clj:1396-1577
seventeen static questions            src/app/server/page/portal_questions.cljc:126
verbs: inputs declared, no outputs    src/app/server/page/verb_registry.cljc:21-33
docs and data
sense-line is the unit                docs/decisions.md:149-156
entity template with declared inputs  docs/decisions.md:551
clean-up first ruling                 docs/decisions.md:372-400
waist round two                       docs/decisions.md:455-530
tools are records                     docs/decisions.md:805-834
question as first-class unit, open    docs/decisions.md:910-911
the caching rule, verbatim            docs/below-the-waist/two-chairs.md:114
attachment / correspondence / snapshot docs/below-the-waist/3d/3d-kind-2.md:370-390
split-and-cap rule not built          docs/below-the-waist/3d/3d-kind-2.md:631
the leak                              docs/below-the-waist/3d/3d-kind-2.md:73,392
definer's open question               docs/below-the-waist/3d/3d-object-space-and-query.md:334-346
archive manifest                      /mnt/data/projects/Softland-archive-20260824T105543Z-497e11e/manifest.edn
Codex store probe and receipt         /mnt/data/projects/Softland-claim-probe-20260907/claim_probe.clj:41-82 · claim-receipts.log
```
