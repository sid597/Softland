# first-light A — RECON (organ-vs-joint ground truth)

2026-07-17 · three fresh-context Opus recons, condensed by Fable. Every
claim was VERIFIED at the cited file:line unless marked DERIVED. This is
the contract's §10 evidence base; P0 checks it, doesn't re-mine it.

## A. Scene substrate / the P3c minimum (recon 1)

- Store value `{:slots {vi → slot} :index {address → #{vi}}}`
  scene_store.cljc:11-18; address fan-out maintained incrementally
  :121-145; per-slot `:addresses` subtree index over `[:data :address]`
  ONLY :27-41. Containers registry + effective transforms
  scene_runtime.cljs:34,55-72; ops container-stamped at upsert (T5)
  scene_store.cljc:52-65.
- Pick :188-218 returns `{:vi :path :address :src-path :actions
  :point-local}`, deepest addressed node, containers by layer desc,
  inverse-transform before hit-test; context-bundle :369-393 re-picks from
  world-point, returns vi/address/src-path/camera/visible (cap 32).
  Actions-as-data: descriptors in `:data`, handlers in runtime
  `!action-registry` scene_runtime.cljs:48; one live handler
  (:trail-face/toggle-expand :305-309).
- **Main face is NOT in the store**: singleton `<face-assembly` m/latest
  builds via apply-assembly with `:view-instance :face-main`, caches
  `!face-scene`, emits {:rects :shadows} — editor_compute.cljs:485-534;
  face-mode text ops flatten from `@!face-scene` combined_text.cljs:301-302;
  legacy hit-test fallback mouse.cljs:424-437. Store lane today = spawned
  copies only, gated face-mode render.cljs:199-246.
- **Pick-nil receipt**: scene-substrate NOW.md:56-60 (worn 07-13) — ":vi/
  :address nil = the click was on the MAIN face, which lives OUTSIDE the
  store until P3c." Nil arises: pick-world walks slots (mouse.cljs:413-421)
  → main face has none → record-pick! stores world-point only → bundle
  re-picks → nil.
- **P3c CONTRACT §P3c verbatim scope** (scene-substrate CONTRACT.md:152-163):
  wall 1 = the worn face builds over block-edit's overlay-face-context —
  store migration means threading edit-state + truth-overlay into the store
  build; wall 2 = reactively silencing legacy face-mode consumers without a
  double-draw frame; main face gets a dedicated container. Staged besides:
  overlay-merged echo lane to copies (F1b), per-slot diff
  (update-nodes-by-address has ZERO callers — F8), second assembly artery
  (spawn-by-name stop-clauses to the worn face scene_runtime.cljs:370-378).
- **Overlay path**: overlay-face-context PURE block_edit.cljc:271-301
  (focused → buffer+caret+refusal; others → truth-overlay else served);
  wired at editor_compute.cljs:511-512 watching !edit-state + !truth-overlay
  (:533-534). Store build-face-tree scene_store.cljc:250-271 runs
  apply-assembly on RAW projection — no overlay (this IS wall 1). Copies
  already measured stale/no-caret (gate F1b, GATE.md:26-42).
- **Both stamps needed**: apply-assembly stamps ROOT only
  (face_assembly.cljc:511-524); per-block `[:data :address]` =
  stamp-block-addresses scene_store.cljc:224-244, store path only
  (build-face-tree:270). Flip must apply both or picks miss blocks.
- Edit inventory (DERIVED, cross-checked): editor_compute.cljs:485-534
  → slot upsert; combined_text.cljs:301-302 + editor_compute:528-530
  silenced; render.cljs:210-246 gate + container; scene_runtime
  register/refresh path for :face-main; mouse.cljs:424-437 fallback
  retires. Plurality path is an UNCOMMITTED dev affordance
  (window.sceneFaces, scene_runtime.cljs:342-343,439-457).

## B. Faces / assemblies / revisions (recon 2)

- **Binding framework rules** (framework CONTRACT.md): assembly = OC
  material, object-key `asm:<name>` deterministic on NAME (rename = fork
  via based-on/new-direction) :363-372,827-833; import key
  `imp:asm:<object-key>:<sha>` :831-833; `:assembly/status ∈ #{:candidate
  :worn :retired}` = Sid's revisable assertion, wear counts never flip it
  :376-379,799-802; lineage = D-004 kinds only, new kind = stop clause
  :380-389; arsenal holds pointers/usage NEVER material :706-712 (T16
  :851); face→projection binding: roster default :conversation,
  `:assembly/projection` pre-named extension (face_projection.clj:552-583).
- **Serve path**: /face → wear-face! arms `!assembly-request`
  (face_wiring.cljs:70-90) → FacePull → `assembly-projection` reads
  `oc:doc:asm:<name>` current revision's content-text, REVALIDATES with the
  same .cljc compiler, returns source + verdict
  (face_projection.clj:421-469). **Serves ANY name — not worn-guarded.**
- **Client compile**: compile-served-source! face_wiring.cljs:92-116 —
  value-compared, publish-guarded to the CURRENTLY-WORN face (the single
  artery; F4 guard). Total compiler face_assembly.cljc = never-throws
  (error-card :266-300); compile-assembly wear-time once, apply-assembly
  per data change; `:src-path` = template path stamped into
  `[:data :assembly/src-path]` at apply (:306-328,395-412).
- **Revisions**: arsenal $$faces-by-name = single mutable latest pointer
  (value-overwrite per accepted import, face_arsenal.clj:214-225,331-344);
  OC underneath = REAL chain — RevisionRow into $$revisions-by-id +
  $$revision-history-by-container, container current-revision-id advanced,
  revision-id deterministic on content (object_container.clj:109-111,
  235-237,2007-2027; reads runtime.clj:363-374).
- **Wear**: client-minted wear-id outbox → RecordFaceWear → record-wear!
  (arsenal-only artery, T15); wear WAL off on cluster
  (cluster.clj:161-174); journaled no-op on duplicate wear-id
  (face_arsenal.clj:190-212).
- **The four gaps for propose→preview→accept**: (1) new source versions
  enter ONLY via watcher `(slurp file)` → import-assembly!
  (ingest_watchers.clj:113-217) — no non-file entry; machine-written
  assemblies = framework §20.3 non-goal (CONTRACT.md:949-951). NOTE:
  assembly-source-import-request already takes source TEXT — the entry is
  adapter-level. (2) current-revision-id = latest import, auto-advanced;
  no active-vs-latest pointer, no accept semantics. (3) no client compile
  channel for an unworn face (spawn-by-name stop-clause). (4) status flips
  only by file edit.
- Multi-face-over-one-conversation is DESIGNED-IN: per-vi compiled
  registry !vi-faces + build-face-tree "N different arsenal faces can
  render one live conversation at once" (scene_store.cljc:250-271,
  scene_runtime.cljs:74-82,136-151,194-225).

## C. Conversation material / agent / relations / boot (recon 3)

- **Conversation material**: transcript harvest → block-distiller → OC
  units `du:<object-key>:sense-block-v0:<path>` + per-part anchors +
  import keys `imp:tr:...` (block_distiller.clj:39-71); river/debris
  classify :167-180; default conversation pinned by `7c80ce2a` prefix
  (file_viewer.cljc:268-273,383-384); READ-ONLY after distill (G12).
- **Typed chat text is NOT durable at utterance time**: submit-agent-run!
  → POST /api/agent/stream → CLI argv (server_jetty.clj:640-667,
  objects.cljc:5-31); persistence = post-hoc `:agent-trail/saved` compat
  event + LATEST-only quarantined atom (`:durable? false`,
  util_fns.cljc:103-120,318-330). Material only via later transcript
  re-ingest of the CLI's jsonl.
- **Block-edit lane IS durable** (WAL-first :object/edit,
  runtime.clj:112-120) but targets EXISTING units — `:target/not-found`
  rejected; derived-unit graduates on first edit; **no :object/create
  from blank ground** (object_container.clj:1402-1427,1650-1720).
- **Agent = CLI subprocess** (claude/codex/gemini argv; `--resume`
  sessions; NO API keys in argv/env — subscription CLIs;
  server_jetty.clj:322,402-452,640+). Responses land in client atoms only.
  dogfood llm/space model provider lanes incl. `:subscription` default but
  are NOT booted anywhere (file_viewer.cljc:25 requires transcript only).
- **Relation kernel**: twenty kinds verbatim relation_kernel.clj:58-79
  (incl. `:references`); closed set, growth = one reviewed line :55-57;
  asserted-by in payload + relation-id includes asserter :118-125,867-892;
  envelope custody separate from asserter :247-269; /api/relation/assert
  route with target-kind allowlist `#{:container :source :doc-file
  :conversation}` (server_jetty.clj:809-854). Payload free-text = `:note`
  only → a wish needs a MATERIAL carrier + edge, not a relation row alone
  (DERIVED, adopted by CONTRACT §4).
- **ActionRequest doc** (architecture/action-request-kernel-routing.md):
  request asks / decision answers / event happened; rejected requests are
  not world facts; keystrokes batch (`:text/edit-batch`), not per-key
  requests. OC's :object/edit envelope is the closest live instance of the
  generic shape.
- **Boot today**: electric_flow.cljc:534,27-32 loads ITS OWN SOURCE into
  the editor; mode settles :file-workspace (workspace_actions.cljs:125-159);
  conversation face NOT worn at boot (FacePull only `when freq`); no login
  ceremony; actor sid implicit. **No episode concept in code** (grep
  clean); episode = design intent (sense-line-model.md:25, DIRECTION:36).
  **No input surface mints net-new durable material from blank ground.**
