# The Editing Waist — map + cut outcome (the form sessions read)

Twin of the artifact <https://claude.ai/code/artifact/0136193c-fbff-473d-a059-fde0bd620b0f>
(HTML + SVG, for Sid's eyes; version "cut-landed-2026-08-20"). Same content, same
structure — the drawn map is the tables in §2. This file is the agent-facing primary:
read it instead of fetching the HTML; addenda land here first.

**State as of:** commit `adc30c9` (2026-08-20) on `docs/current-mental-model-local`,
worktree clean, nothing pushed or merged.
**Authority:** a dated picture, not law. The waist *definition* is settled ground in
`docs/decisions.md` ("The editing waist", Sid, 2026-08-19). Current *status* is
`docs/below-the-waist/NOW.md`. The task and its execution thread:
`docs/below-the-waist/waist-cleanup-task.md`. The code outranks this page.
**Provenance:** built from the execution log (Codex, 2026-08-20) + NOW.md + the commit;
spot-checked 2026-08-21 (tree census, `git diff d507182 adc30c9`, conductor/worker logs
read-only). Not re-derived from the code line by line — where that matters below, the
line says so.
**Two halves, two grades (amended 2026-08-21).** The *deletion* record (§2a–2b, §6–§9,
and every number in §7) is diff-backed and mechanically verified — Codex re-checked it
against the commit and it held. The *survival* record ("what exists now") is read-derived:
a deletion is a diff, a survival claim is semantic, and names in this tree describe the
generation that minted them rather than the thing running. Two careful passes (this map,
then a Codex audit) each got parts of it wrong. Survival rows below are therefore
**provisional until R5** — the restart readback is the instrument that settles them
(§5, runner: `bin/r5_readback.clj`).

**Load this when:** building anything above the waist · touching `src/app/client/` or
`server_jetty.clj` · asked "what was cut / what exists now / what is pending" · cutting the
next contract on this seam. Do not load it for unrelated Rama or docs work.

---

## 1. The line (the definition this map applies)

| | below — engine | above — material |
|---|---|---|
| form | must be the shared engine | can be composition on top |
| reason | everyone must agree | surfaces may differ |
| consequence | changes slowly, one at a time, with receipts | parallel product work, can't break the core |

**The two-second check:** could one surface swap in its own version without breaking
anyone else — yes → it goes above as material; no → it's engine, below.

**The ruling (decisions.md, Sid, 2026-08-19)** instantiates this for editor code: the
floor — the only editor code that must exist as shared engine — is four things:
**the shaper** (text in, placement out; nobody else measures), **primitive edit verbs**
(insert / delete-range / replace-range), **validators** (what makes an edit acceptable,
refused durably), and **the commit artery** (edit in, durable accepted/rejected out, keyed
truth back). Text, wrap, cursor, selection, keys, behavior composition, placement, and
presentation are material above.

---

## 2. The map — component by component, as the cut landed

Legend: **GONE** = deleted whole at `adc30c9` (number after a file = its gross lines
deleted) · **STAYS** = in the tree, byte-identical unless marked ✎ (edited) · **PARKED** =
stays, no live door, guarded by `npm run verify:render-engine` · **CUSTODY** = stays under
Sid's explicit custody ruling, one-cut deletion debt · ◆ = a file the waist line cut
through · ★ = quarry: deleted, but a named mechanism inside is dug from git history
(`git show d507182:<path>`) when the verbs slot is built.

### 2a. Above the waist — material & surfaces (lights out 2026-08-20)

| component | outcome | files | notes |
|---|---|---|---|
| **GROUND — the live editor** | GONE | `workspace/ground.cljs` ◆ 4,954 · `ground_edit.cljc` 465 · `face_wiring.cljs` ◆ 771 · `agent.cljs` — gone. `face_assembly.cljc` ✓ lives, untouched. `src/components/design_tokens.cljc` ✓ stays. | keys · caret · selection · paste · wrap policy · gestures — gone; deleted whole, no migration, no copy. **`face_primitives.cljc` KEPT — CUSTODY**: the 16-builder block render channel + the block words in anatomy vocabulary stay only because TrailView, anatomy and the verifier goldens draw through it today (Sid, 2026-08-20: block-as-atom is waist; this channel is neither waist nor future architecture); deleted whole — no migration, no second copy, one cut-over — when the future presentation layer draws blocks. |
| **T2 — flag editor** | GONE | `editing_runtime.cljs` 919 · `text_editing.cljc` ★ 483 · `editing_segmentation.cljs` ★ · `t2_block_wiring.cljs` · `t2_block_join.cljc` ★ | own keys · IME · grapheme kernel — gone whole; no verbs were built in its place (slot still empty). ★ the op kernel lives at `d507182`. |
| **OTHER FACES & CONSOLE** | GONE | `trail_face/wiring.cljs` · `trail_face/sanitize.cljc` · `live_edges.cljc` · `seam_demo.cljs` · portal · halo · inspector (lived in `face_wiring.cljs` ◆) · `live_atoms.cljs` 376 · `src/global_flow.cljs` | all gone — the concept survives (smalltalk-vm); the wiring burned; matter-room routes stayed server-side ✓. |
| **SERVER-SIDE MATERIAL** | STAYS · untouched | `facet_master.clj` · `material_truth.clj` · `material_circulation.clj` · `machine_cut.clj` · `verb_release.clj` · `code_atoms.clj` · `cascade.clj` · `material_portal.clj` + the face entries in `face_projection.clj` ◆ | organs on the kernel — each addable alone. |
| **MATERIAL ROWS — gesture × facet → verb tables + masters** | STAYS · untouched | `src/app/shared/*_material.cljc` (binding · attention · positioned · foldable · anatomy · facet · facet_masters · text_body · threaded · space · matter_room · verb_registry · provenance · invocation · activation_event · reply_to_block) + `material_inspector` · `material_portal` — 18 `.cljc` files in `src/app/shared/` | data rows both sides of the wire read; not recut — nothing to recut against until building. |
| **PLUG-IN RENDER FAMILIES** — clients of the pipeline & frame registries | PARKED · verifier green | chrome{runtime·derive·material·gpu} · connector{material·route·gpu} · path{material·tessellation·gpu} · image{material·gpu} · `snap.cljc` · region3d{runtime·pointer·placement·scene·evaluation·material·rungs·gpu·bindings·placement_gpu} · effects{frame_effects·effect_view·plan_view} · `selection.cljc` | the live door died with the client. `chrome_runtime.cljs` ✎ now takes explicit host hooks (was Ground). R3 green: 152 files / 0 warnings. |

### 2b. The waist — the seven files the line cut through, outcomes

| file | engine half / material half (anchors at `d507182`) | outcome |
|---|---|---|
| `text_layout.cljc` | measurement/shaping (floor: the shaper) `:682 shaped` / wrap policy (material) `:399–547 wrap cuts` | **Stayed whole ✓** — `verify:text-layout` green |
| `block_edit.cljc` | the envelope law (floor: artery) `:52–79` / the keydown splice (a private verbs impl) `:85–115` | **Gone ✓**; the id law is quarry at `d507182` |
| `events.cljs` | the listener spine (engine) `:160–172` / the ground's semantic keymap (material) `:112–149` | **Gone ✓** |
| `runtime.cljs` | the engine boot hardcoded both surfaces by name `:89 t2 install · :100 ground install` | **Gone ✓** |
| `face_wiring.cljs` | the epoch re-stamp law (engine) `:745–765` / the portal/halo/console surface (material) `:317–583` | **Gone ✓** |
| `face_projection.clj` | the serve registry (engine) `:1910–1944` / every face entry (material) `:495–1835` | **Stayed ✓**, untouched, dormant |
| `ground.cljs` | a surface owning the camera `:78` and the only live layout cache `:168` | **Gone ✓** (−4,954, the single largest deletion); camera/placement restore duty is served truth, redrawn by whatever comes next |

### 2c. Below the waist — THE EDITING FLOOR (the ruled four)

| slot | outcome | files | notes |
|---|---|---|---|
| **THE SHAPER** | STAYS ✓ | `workspace/text_shaper.cljs` · `text_layout.cljc` ◆ (+ `text_layout_planes.cljc`, internal) · `workspace/runtime/fonts.cljs` | text in, placement out — nobody else measures. Guarded by `npm run verify:text-layout` — R2 PASS · 6 tests / 32 assertions. |
| **PRIMITIVE EDIT VERBS** | EMPTY — STILL EMPTY ✓ | (none) — insert · delete-range · replace-range | built only when the first thing on top needs it; quarry: T2's op kernel at `d507182`. |
| **VALIDATORS** | STAYS ✓ untouched | `server/rama/object_container.clj` — validate `:575–622` · edit-effects `:1414` · decide `:2321–2483` | what makes an edit acceptable — refused durably, reason surfaced; hash · capability · stale-seq · idempotency · replay. |
| **COMMIT ARTERY** | SPLIT · landed | gone: `block_edit.cljc` ◆ ★ · `edit_transport.cljc` ★ · `block_edit_wiring.cljs` · `electric_flow` submit door 382. stays: `object_container/runtime.clj` (the admission road) untouched. **new: `src/app/server/block_edit.clj` (44 lines, rehomed)** | client half died with the editors; the one server edit op the matter room still calls (ObjectContainer append/decide) moved out of the deleted Electric namespace — a rehome, not a second edit system. depot → validate → decide → revisions → keyed truth: untouched grain. |

### 2d. Below the waist — PLATFORM SUBSTRATE (shared engine, not editor code)

| component | outcome | files | notes |
|---|---|---|---|
| **INPUT SPINE** | GONE | `workspace/events.cljs` ◆ · `runtime.cljs` ◆ · `runtime/{state·keyboard·mouse·touch·scroll}.cljs` · `runtime/render.cljs` 629 | the whole tree is gone — no pump survived; `render.cljs` was boot glue beside `runtime.cljs` and went with the spine (the ruling map had parked it; the task doc was right). `runtime/fonts.cljs` stays — shaper. |
| **READ WIRE — one Electric websocket** | SPLIT · landed | gone: `src/app/electric_flow.cljc` main 382 · `src/app/file_viewer.cljc` (whole) 411 · `src/global_flow.cljs` · `resources/public/index.html` 149. stays: `server/rama/util_fns.cljc` ✎ (13 lines now: the `!ingest-epoch-atom` only; the dead text-kernel shims removed) | "how is it even acceptable to keep the single atom survives" — went whole; 10 face-ctx sites now use `cluster/face-projection-runtime`, the relation assert route uses `cluster/trail-runtime` ✓. No `file_viewer` IPC boot was moved or copied. |
| **SERVE REGISTRY** | STAYS ✓ · **LIVE, not dormant** (corrected 2026-08-21) | `server/rama/face_projection.clj` ◆ (registry + entries) · `block_distiller.clj` | reading did not die in the dark — it was **absorbed into the write path**. `face-projection/serve` is called server-internally at `server_jetty.clj:823` (inside `open-matter-room!`, serving `:material-portal`) and `:999` (the rollback act's recovery offer). What died is the *HTTP/Electric read road*, not the registry. Earlier "dormant during the dark" was wrong; so was the audit's "no consumer". |
| **TRUTH-OWNING SIBLINGS** | STAYS ✓ untouched | `relation_kernel.clj` · `face_arsenal.clj` | each owns its own depot + PStates: relation-kernel `*relation-request-depot` + 8 PStates; face-arsenal `*face-arsenal-depot` + 4 PStates. |
| **TRAIL VIEW — a read projection, NOT a kernel** (corrected 2026-08-21) | STAYS ✓ untouched · REQUIRED | `trail_view.clj` | declares **no depot and no PState of its own** — 13 `mirror-pstate` lines over object-container + one `<<query-topology "context-bundle"`. It is a materialized view. The map previously filed it beside the truth-owners; that is a category error the shared word "kernel" hides. Naming landmine: `cluster/trail-runtime` is the handle the assert route uses, and its own docstring says `:module-name = the RELATION kernel name` — a bundle called "trail" whose primary module is the relation kernel. |
| **EPISODE LANE** | STAYS · edited ✎ | `src/app/server_jetty.clj` ✎ ×2 · `server/episode.clj` · transcript helpers (live, in `dogfood/transcript.clj`) | turns · births · geometry = placement + camera cells · SSE. **One of ten surviving write roads, not the only one** (corrected 2026-08-21) — agents /
curl / CLI; the full roster is §2f. R4 proved these three; it never established they were
alone. ✎ product EDN routes own the raw body (moved outside `wrap-params`; `curl -d` reaches them) · ✎ source-less top-level turns land durably; birth-receipt / gold / wear lookups run only when a source is addressed. R1 ✓ boots server-only · R4 ✓. |
| **TRUTH KERNEL — object container** | STAYS ✓ untouched | `object_container.clj` · `object_container/runtime.clj` · `core.clj` · `objects.cljc` · `cluster.clj` · `git_spine.clj`; adapters: transcript_identity · transcript · markdown · assembly · clojure | depot · decide · revisions (whole text) · idempotency. The **five deployed module definitions live in four source files** (object_container.clj
holds two: `object-container-module` `:1746` and `object-container-transcript-ops-module`
`:2608`; then relation_kernel.clj `:634`, trail_view.clj `:284`, face_arsenal.clj `:166`);
all four files are byte-identical after the cut; data as-is under `/mnt/data/rama/data` — nothing migrated. **R5 restart readback: PENDING** (§5). |
| **SCENE + GPU MACHINE** | PARKED · guarded ✓ | `workspace/scene_store.cljc` · `scene_runtime.cljs` ✎ · `rect_tree.cljc` · `containers.cljc` · `scene_tape.cljc` · `frame_scheduler.cljc` · `frame_graph.cljc` · `frame_inputs.cljc` · `frame_delta.cljc` · `frame_semantic_state.cljc` · `frame_runtime.cljs` · `substrate/webgpu/renderer.cljs` · `buffer_pool.cljs` · `gpu_budget.cljs` · `verifier.cljs` ✎ (−561: T2 / live-client lanes removed) | live door died with the client. R3 PASS: 152 files · 0 warnings · base/chrome/Region3D goldens green. Its own ruling still comes later. → pixels (later). |

**Internal flow below the line — as it actually runs now** (corrected 2026-08-21; the
earlier "unchanged" line drew the deleted Electric read wire and was false):

    HTTP request (agent · curl · CLI)
      → the one Jetty middleware (`wrap-file-api`)
      → route handler
      → cluster runtime handle (`cluster/face-projection-runtime` | `trail-runtime`)
      → deployed depot  → decide → durable PStates
      ↳ some handlers also call `face-projection/serve` on the way (an internal read
        inside the write path — §2d SERVE REGISTRY)

There is no puller and no read road. The only thing that flows *back* is the response
body of the POST that caused it (the episode turn's SSE stream rides its own POST).

### 2e. Drafts strip — landed 2026-08-20

- **GONE** — the four never-deployed drafts, −4,856 lines + 7 tests/probes:
  `server/rama/text_kernel.clj` 733 · `server/rama/kernel.clj` 840 + its kernel_shape test ·
  `server/rama/dogfood/space.clj` 2,177 · `server/rama/dogfood/compute.clj` 1,106 — with
  their tests and probes.
- **PARKED** — `dogfood/transcript.clj` module def, relabeled ✎ (its helper fns are LIVE in
  the episode lane) · `dogfood/transcript_ingest.clj`.
- **LIVES** — `dogfood/llm.clj` (runs, in-memory by design).

**Why the drafts went (addendum 2026-08-21, checked at `d507182`):** "on Rama" was never
the criterion; "deployed and something runs through it" was. None of the four was a
deployed module — the cluster runs exactly five (`bin/land` `MODULE_VARS`: object-container
×2, relation-kernel, trail-view, face-arsenal) and `cluster.clj` states it cannot launch
modules; the four only ever launched *themselves* in-process through
`com.rpl.rama.test create-ipc / launch-module!` inside their own tests and probes — no
cluster PStates, no data, no route. They were the May–June kernel generation (05-13 "split
text kernel and rename space", 05-19 "identity headers to all 5 kernels", fix sessions to
06-12). **What superseded what** (corrected 2026-08-21 — the earlier blanket "the
deployed ObjectContainer generation superseded" them was too broad): `text_kernel.clj` was
an alternate text/artifact/revision/unit truth store — Object Container **does** supersede
that ownership. `space.clj` was an alternate Space/turn/object/context truth island — the
deployed world model supersedes or abandons it. `kernel.clj` was documentation-shaped data,
not an executable system. `compute.clj` was a Rama-owned OS command executor — **Object
Container never provided that capability and did not replace it**; compute was deleted as
an undeployed, uncalled, unchosen execution architecture with no durable cluster state, and
there is no retained replacement. That distinction keeps the "no migration, no second copy"
ruling honest without inventing a replacement that does not exist. `kernel.clj` was a prose
spec + a `defkernel KERNEL-SHAPE` data snapshot ("DATA, not code generation. Nothing in the
codebase calls it"); its two "requirers" were `;;` comment lines — the comment-match that
made task v2 rule it alive (Codex caught it, v3 fixed it, task doc `:107–109`, `:348`).
The one place a live path could have hidden: `util_fns.cljc` was a 164-line shim adapter
over a lazily-started in-process text kernel (`!kernel-runtime` delay); eight namespaces
required `util-fns`, and all 14 uses were `util-fns/!ingest-epoch-atom` — zero shim calls,
the delay never fired. The ruling was a dead-code call ("drafts, tree only (git keeps)",
task doc `:107`), not a waist call — the contrast being `dogfood/transcript.clj`, same
vintage, **parked not deleted** because its helpers *are* live.

### 2f. What is actually running now (added 2026-08-21 — read-derived, R5 settles it)

Everything in §2a–2e answers *what was cut*. This answers *what a builder would find*. It
exists because the map's survival claims sent a reader toward rebuilding things that are
already here — and the cut's own ruling was **no migration, no second copy**. A survival
map that under-reports is a machine for producing second copies.

**The whole HTTP surface — ten routes, all POST, no read road.** One middleware
(`wrap-file-api` in `server_jetty.clj`) owns every route:

| road | route | line |
|---|---|---:|
| relation assert | `/api/relation/assert` | 1394 |
| matter room | `/api/matter-room/open` · `deviate` · `activate` · `rollback` · `say` | 1416 · 1435 · 1448 · 1461 · 1474 |
| material drill | `/api/material/facet-master/drill` | 1488 |
| episode (R4-proven) | `/api/episode/utterance` · `block-birth` · `geometry` | 1522 · 1542 · 1575 |

No `GET` read endpoint survives. The episode turn's SSE stream is a *response body on a
POST* (`text/event-stream` at `:468`), not a read road.

**Where the surviving system lives — two places, not one.**

| | what runs there |
|---|---|
| **external Rama cluster** | the five deployed module definitions (`bin/land` `MODULE_VARS`): object-container ×2, relation-kernel, trail-view, face-arsenal — four truth-owners + one read view |
| **the app JVM** | Jetty + `wrap-file-api`; the server material organs (`cascade` · `material-circulation` · `material-truth` · `facet-master` · `face-projection`); episode + transcript **helper fns** (live even though the draft transcript *module* is parked); `server/block_edit.clj`; and the **lazy in-process LLM runtime** (`dogfood/llm.clj`, `delay (llm/start-llm-runtime!)` at `server_jetty.clj:347`) |
| **absent** | Electric client · every product surface · the read wire · the input pump · the whole client write path |

"Not one of the five deployed modules" does **not** mean dead — the retained LLM runtime
and the transcript helpers are the counterexamples.

**The naming drift (why this section had to be written).** Names here record the generation
that minted them, not the thing running. Three found in one pass:

- `wrap-file-api` — the middleware that is the *entire app's front door*. Its docstring:
  *"Handle /api/\* routes for file explorer sidebar. Returns EDN responses consumable by
  ClojureScript client."* `file_viewer` is deleted; there is no ClojureScript client.
- **"kernel"** covers three unlike things: a truth-owning Rama module (object-container,
  relation-kernel, face-arsenal), a read projection that owns nothing (trail-view), and
  the deleted `kernel.clj` prose spec.
- `cluster/trail-runtime` — a bundle named "trail" whose `:module-name` is the **relation**
  kernel, by its own docstring.
- **`read-transcript-conversation-projection` is not keyed by the address.** Its parameter
  is a *conversation-container-id* — `oc:chat-conversation:<address>` — and every real
  caller passes `(tid/chat-conversation-id object-key)`. Passing the bare address returns
  `[]`, not an error. This cost four false FAILs on the first R5 run: the reader concluded
  turns and geometry were missing when 43 turns and 406 cells were sitting there. The
  runner now delegates all key derivation to the app's own accessors (`episode/read-*`)
  rather than re-deriving it — the general fix for this whole class.

Durable names are **not** free to rename: `mirror-pstate` binds by literal string
(`"$$containers-by-id"`), so a PState rename is a durable-state migration and collides with
the preserve-as-is rule. What is free — and what is owed — is a **lexicon**: every name
recorded next to its role, so the word stops standing in for the thing. It rides R5's
output (§5).


---

## 3. The four crossings (as they ran at `d507182` — all four died with the client)

1. **① Raw event ↑** — every key/click/paste entered the engine's one listener spine, then
   routed up to whichever surface held focus or the intercept slot.
   `events.cljs:160–172 · runtime.cljs:108–116` — **dead**.
2. **② Writes ↓** — the surface minted a whole-text envelope per keystroke into the commit
   artery; placement settles, block births, and Ctrl+Enter sends went down the episode
   lane. `ground_edit.cljc:298–328 · electric_flow.cljc:305 · ground.cljs:2080, 2596,
   2801` — **client half dead**.
3. **③ Served truth ↑** — accepted truth bumped the epoch; one second later the whole face
   context re-pulled; accepted edits also got the un-debounced keyed block-truth echo.
   `file_viewer.cljc:130 · face_wiring.cljs:745–765 · block_edit_wiring.cljs:94–115` —
   **no puller**.
4. **④ Scene slots ↓** — the surface handed retained-layout scene slots down to the one
   store; the tape ordered them and the GPU machine painted.
   `render.cljs:183–237 · scene_store.cljc:48–95 · renderer.cljs:3678` — **dead**.

After the cut **no client crossing survives** — that is what R1–R4 proved. What survives
is a server-only HTTP write surface of ten routes (§2f), of which R4 exercised three
(turns, births, geometry). "The only write surface is the episode lane" was wrong. No Electric client, no pulls, no wake signal. The anchors are line
numbers at `d507182`; the files are gone — `git show d507182:<path>` still finds them.

---

## 4. What landed — the five close scenarios as they ran

| receipt | what it proves | result |
|---|---|---|
| R1 · `clj -M:dev -m dev` | the app boots server-only — Jetty on :8080, no client bundle, no Electric | **PASS** |
| R2 · `npm run verify:text-layout` | the shaper floor survived the cut intact | **PASS** · 6 tests / 32 assertions |
| R3 · `npm run verify:render-engine` | the parked machine compiles and every retained guard holds | **PASS** · 152 files · 0 warnings |
| R4 · the exact contract curls | the only live write road works with no client: block birth, geometry settle, and a turn whose first SSE event is the durable ack (`:episode-durable`) | **PASS** · after two in-session repairs (§6) |
| R5 · full restart + named readback | every written kind reads back after the cluster bounces: unit text / current revision / decisions (`ocr/read-unit`, `read-current-revision`, `read-decision`) · `$$transcript-conversation-projection` cells (the `:episode-turn` cell for `waist-close-turn`, the settled unit geometry cell, `geo:camera`) · wear (`face-arsenal/read-wear-count :outline-face`, pre-restart `nil` → must still be `nil`) · relations (`rk/read-relations-for-targets`, one starter-culture target) · one transcript-ops offset point read | **PASS 2026-08-21** · 8 PASS / 0 FAIL on the live cluster, then the bounce + baseline compare: **0 DRIFT**. See §5. |

R4 receipts: durable address `chat:77088a4f028100d3e93a99d291e2a184c7ed6ca66b3a998a74f9fbddfe62b34b`;
unit `du:chat:77088a4f…:episode-native-v0:ep:a2fb6f3d:000000` settled at `(240.0, 180.0)`,
camera `(0.0, 0.0, zoom 1.0)`; the turn `waist-close-turn`'s first SSE event
`{:event :episode-durable :kind :episode-durable :address "chat:77088a4f…" :import-key
"imp:ep:chat:77088a4f…:ebb069d4…"}`. The full resident-agent run after the durable SSE was
deliberately not part of the receipt.

Pre-freeze checks: focused server tests 9 tests / 100 assertions green · product-client
absence tripwire 1 test / 6 assertions green · focused suite after the two receipt-born
repairs 26 tests / 158 assertions, 0 failures · `git diff --check` clean · five deployed
module sources unchanged · no executable remaining require of a deleted product namespace.

---

## 5. Closed — R5 green, accepted (2026-08-21)

**Both open items closed on 2026-08-21.** R5 ran green and Sid accepted the cut. Under the
work-package law that is the whole gate — no validation ladder, no second-model sign-off.
**The road above the waist is open.**

The record of how R5 ran is kept below, because two of its findings outlive it.

**R5, the restart readback — PASS 2026-08-21.**

The runner exists and ran: `bin/r5_readback.clj`, read-only. On the live cluster
**8 PASS · 0 FAIL**, every named kind present:

| kind | read back |
|---|---|
| turns | 43, including `waist-close-turn` with its receipt |
| placement | 406 geometry cells |
| camera | `{:x 0.0 :y 0.0 :zoom 1.0}` — exactly R4's recorded value |
| unit + text | `read-unit` green; content-text is real prose, hash `00da3ad2…` |
| wear | `outline-face` = **641** |
| face roster | 4 |
| relations | 67 edges under a starter-culture target |

Three things this settled:

1. **Hypothesis (b) is CONFIRMED — the conductor flag was a red herring.** The reads
   never consult `conductorReady`, and they work. The `Unexpectedly missing operation
   metadata` noise did not block anything. No investigation is owed there unless a read
   actually fails.
2. **The map's own R5 spec had a key-type error.** It expected wear for `:outline-face`
   to be `nil` pre-restart and `nil` after. The PState is keyed by **String** — the
   keyword read missed, and the miss was recorded as the baseline. The real value is 641.
   A `nil` in this system is as likely to be a wrong key as an absent fact.
3. **Nothing was lost.** The 3.5 GB under `/mnt/data/rama/data` is live and intact.

**What remains is the actual bounce.** The runner writes a fact baseline on first run
(`/mnt/data/rama/r5-baseline.edn`: turn count, geometry cell count, camera, content hash,
wear count, roster size, relation count, unit id, offset) and compares against it on every
run after. Two commands close R5:

    bin/land down && bin/land up          # see the wedge warning below
    clj -M:dev -e '(load-file "bin/r5_readback.clj")'

**Sid ran the bounce on 2026-08-21 and the compare came back 0 DRIFT.** Every written kind
survives a full cluster restart, as the hard rule required. R5 closed; the baseline file
stays as the regression instrument for any future bounce.

**The conductor observation, kept for the record.** Observed in the conductor/worker logs (read-only pass,
2026-08-21): on every boot on 2026-08-20 (10:43 · 10:59 · 11:01) the conductor's state
handler runs `[cluster-shutdown-complete]`, then its dispatch loop throws
`Unexpectedly missing operation metadata` for one module runtime (target `c925595f…`)
about twice a second, from its first dispatch until the daemons were killed. The same
exception appears at 08-19 17:16 — the last pre-cut boot — and clears within seconds
there; that cluster then ran healthy through the morning's R4 writes. The workers came
up, logged `a leader candidate already exists for this replica … continuing` per task,
and were serving sessions by 11:01:32; worker stdout warns `java.io.tmpdir directory does
not exist`. This is **not** the LEADER-FALLING wedge that `bin/land unwedge` repairs — the
ZK state was already `[:cluster-shutdown-complete]` before and after the recovery path.
**Cause: unknown. Nothing here is attributed to the cut** (the five module sources and
their PStates were not touched). Hypotheses to kill, cheapest first: (a) the daemons
inherited an environment from the execution session's sandbox (the tmpdir warning) → boot
once from a plain shell; (b) the reads may not gate on that conductor flag at all → check
`moduleStatus` RUNNING ×5 and run the five named reads regardless. The data sits under
`/mnt/data/rama/data` either way.

**How to run it (added 2026-08-21).** `bin/r5_readback.clj` — read-only, `read-*` fns only,
no depot append, no ingest, no migration. Two commands:

    bin/land up                                        # wait for RUNNING x5
    clj -M:dev -e '(load-file "bin/r5_readback.clj")'

It exits `2` if the cluster is down, `1` if any named read is not green, `0` on a clean
readback, and prints a PASS/FAIL line per kind: container · current revision · conversation
projection (turn `waist-close-turn`, geometry, camera cells) · derived units (ids
*discovered* from the projection, not hardcoded) · wear count for `outline-face` (pre-restart
`nil` → must still be `nil`) · relations · transcript file offset. Overrides via
`R5_ADDRESS` · `R5_TURN` · `R5_FACE` · `R5_RELATION_TARGET` · `R5_FILE_KEY`.
Hypothesis (b) is what the runner tests directly: it never consults the conductor flag, it
just asks for the reads. **R5 is not a checkbox — it is the instrument that settles §2f.**

**Sid's acceptance — GIVEN 2026-08-21.** Under the work-package law, code-complete is
Codex's state; accepted is Sid's word, and it was given. The above-waist road is open.

---

## 6. The receipts changed the code twice — and four calls the ruling map didn't make

1. **EDN routes now own the raw body.** `curl -d` sends
   `application/x-www-form-urlencoded`; `wrap-params` sat outside the product routes and
   drank the stream, so `parse-edn-body` saw `""` and block birth came back
   `{:status :rejected, :error :bad-request}`. Measured in-process
   (`{:body "", :params {}, :form-params {}}`), not guessed. The product EDN routes moved
   outside `wrap-params`; an exact-curl regression test pins it. `server_jetty.clj:1606`.
2. **Source-less top-level turns land durably.** The contract's own turn body
   (`{:turn-id "waist-close-turn" :content-text "close receipt turn" :time-ms …}`) carries
   no `:source-unit-id`; `run-episode-turn` refused it with `:missing-source-block` before
   recording, though the durable turn record already held the source as optional. Now a
   turn without an addressed source records durably; birth-receipt lookup, gold selection
   and invocation-wear lookup run only when a source is addressed (`addressed?` branch);
   source-less turns use the invocation code floor. **This is the one place the cut
   widened what the server accepts — worth Sid's eye at acceptance.**
   `server_jetty.clj:439 · :521`.
3. **`src/app/server/block_edit.clj` (44 lines, new).** The one server-side edit operation
   (ObjectContainer append / decide) that surviving matter-room code still calls, lifted
   out of the deleted Electric namespace. A rehome, not a second edit system — no client,
   no envelope law, no verbs.
4. **The verifier lost its T2 / live-client lanes** (−561 in `verifier.cljs`, −4,483 in
   `test/render_engine/run_verifier.mjs`; `profile_shaping_correction.mjs` 2,687,
   `verify_image_atom_receipt.mjs`, `verify_shaping_correction_fence.mjs` deleted whole)
   rather than mocking the dead editor; the retained verifier guards base renderer · image
   · path · connector · chrome · frame runtime · Region3D. Small compile repairs:
   `scene_runtime.cljs` stopped depending on deleted workspace events; `chrome_runtime.cljs`
   takes explicit host hooks; the verifier explicitly loads the retained
   chrome/frame/Region3D runtimes.
5. **`runtime/render.cljs` went with the spine**, not the parked machine: the task doc
   listed it as boot glue beside `runtime.cljs` (`:124`); the ruling map had parked it —
   the map was off, the doc was right, Codex followed the doc.
6. **The 16-builder block render channel stayed** — the one authority fork that surfaced
   mid-execution (`face-primitives/registry` held 16 Ground-era block widget builders; "all
   Ground representation goes now" vs "keep the registry-reachable closure used by retained
   faces and goldens" could not both hold). Sid ruled (2026-08-20): block-as-atom is waist;
   this render channel is neither waist nor future architecture; it remains only because
   TrailView, anatomy and the verifier goldens draw through it today; deleted whole — no
   migration, no second copy, one cut-over — when the future presentation layer draws
   blocks. Recorded in the task doc's Execution thread (`:394`).

Also on the record: dev/prod/build entry points became server-only (`src-dev/dev.cljc`
starts Jetty and nothing else; Electric + RCF out of `deps.edn`, Missionary kept directly
for surviving parked code; shadow builds reduced to `:render-verifier` only; client
compilation and static index/manifest injection out of the uberjar path; Electric websocket
and stale-client middleware removed). Repository custody: execution began at `d507182`;
the one overlapping user change (a 2-add/2-delete paren move in `editing_runtime.cljs`)
was inspected before whole-file removal and recorded in the Execution thread (`:402`);
`src/app/server/env.clj` was never read.

---

## 7. The numbers

| category | paths | added | deleted | net |
|---|---:|---:|---:|---:|
| production source | 42 | 188 | 17,349 | −17,161 |
| tests + verifier tooling | 30 | 221 | 11,593 | −11,372 |
| runtime resources (`resources/public/index.html`) | 1 | 0 | 149 | −149 |
| build / dependency config | 2 | 4 | 20 | −16 |
| docs (NOW.md, board line, task thread) | 3 | 30 | 1 | +29 |
| **total — commit `adc30c9`** | **78** | **443** | **29,112** | **−28,669** |

Production Clojure/CLJS under `src*`: 84,406 → 67,245 lines (−20.3%), 135 → 104 files; 32
files deleted whole + 1 added (93.8% of production deletions were whole files — an excision,
not a shuffle). Of the 17,349 production deletions: product client/workspace 28 files
11,424 · never-deployed Rama drafts 4 files 4,856 · inside 9 surviving files 1,069.
Largest single deletion: `ground.cljs` 4,954 (28.6% of production deletions). Tests +
verifier tooling: 40,639 → 29,267 (−28%); 16 whole test/tool files = 6,825 lines (draft
Rama tests/probes 7 files 2,878 · obsolete client/editor tests 6 files 752 · obsolete
renderer receipt/profile scripts 3 files 3,195). Production + tests/verifier together:
125,045 → 96,512 (−22.8%).

Broad fast suite after the cut: 304 tests / 2,809 assertions, 7 failures + 1 error — all
eight reproduced at `d507182` before the cut (three anatomy numeric goldens, one
image-registration pinned count, one Region3D EDN replay, two face-transcription goldens,
one machine-cut golden): foreign debt, not the cut's.

NOW.md's "pre-close diff: 75 paths, +384/−29111" is the pre-close working-diff snapshot
(before the then-untracked `block_edit.clj`, the NOW file, and the board line); the
committed total above is authoritative. Not a contradiction.

### Whole-file deletion inventory (production)

Root product surfaces: `src/app/electric_flow.cljc` · `src/app/file_viewer.cljc` ·
`src/global_flow.cljs`. Ground/editor/write roads (`src/app/client/workspace/`):
`agent.cljs` · `block_edit.cljc` · `block_edit_wiring.cljs` · `edit_transport.cljc` ·
`editing_runtime.cljs` · `editing_segmentation.cljs` · `events.cljs` · `face_wiring.cljs` ·
`ground.cljs` · `ground_edit.cljc` · `live_atoms.cljs` · `live_edges.cljc` ·
`seam_demo.cljs` · `t2_block_join.cljc` · `t2_block_wiring.cljs` · `text_editing.cljc`.
Product runtime/input tree: `runtime.cljs` · `runtime/keyboard.cljs` · `runtime/mouse.cljs`
· `runtime/render.cljs` · `runtime/scroll.cljs` · `runtime/state.cljs` ·
`runtime/touch.cljs`. Trail-face client: `trail_face/sanitize.cljc` ·
`trail_face/wiring.cljs`. Never-deployed Rama drafts: `server/rama/dogfood/compute.clj` ·
`dogfood/space.clj` · `server/rama/kernel.clj` · `server/rama/text_kernel.clj`.
Runtime resource: `resources/public/index.html`.

Surviving production files modified (9): `src-build/build.clj` · `src-dev/dev.cljc` ·
`src-prod/prod.cljc` · `client/substrate/webgpu/verifier.cljs` ·
`client/workspace/chrome_runtime.cljs` · `client/workspace/scene_runtime.cljs` ·
`server/rama/dogfood/transcript.clj` · `server/rama/util_fns.cljc` · `server_jetty.clj`.
Added (1): `server/block_edit.clj`.

---

## 8. The cut ruling — all lights out (Sid, 2026-08-19) · landed 2026-08-20

The product client is deleted whole: both editors, the input tree, the read wire, every
face and console surface. Nothing waits for a replacement — "no fuck no they go now."
Typing AND reading go dark: "removing both read and write … all lights go out i am fine we
will build things correctly."

The app during the dark is the server: the jetty routes plus the five deployed module
definitions — *and* the app-JVM organs, transcript helpers and lazy LLM runtime that ride
beside them (§2f).
Agents and CLI keep writing turns, blocks, and placement — everything acked durable. The
shaper and the parked render engine survive as test-guarded code (`verify:text-layout`,
`verify:render-engine` after its repair).

Hard rule: every written kind — text, revisions, decisions, turns, placement cells, camera
cells, wear, relations, offsets — is preserved as-is and reads back after a full restart.
Migration, if ever, is a later act.

Corrections landed after the map first published: **kernel.clj goes** (its "living" mark
was a comment-match) · **face_assembly lives, face_primitives splits** (real requirers
traced) · **TrailView is required, not tolerated** — the cluster runtime is built on it and
surviving routes read through it · **the draft transcript pipeline parks**, its helper
functions are live.

The falsification session ran (Codex gate 3, `d507182`) and the execution session ran the
cuts (`adc30c9`). R5 came back green and Sid accepted on 2026-08-21 — **nothing remains;
building on top is a new act, and it is open.**

---

## 9. Placement notes (the judgment calls, kept for the record)

The **scene + GPU machine** sits below as substrate — no surface could swap in its own
renderer without breaking the one canvas and tape order — while the presentation *grammar*
(face primitives, anatomy parts) sits above per the ruling. The **plug-in render families**
sit above because each plugs into the pipeline/frame registries without touching another
family. The **server organs** sit above because each writes through the kernel's own
depot/decide and is addable alone. The **input spine** was below as plumbing, and the
keymaps that interpret keys were material above — which is exactly why `events.cljs` was a
cut file. One placement was off: `runtime/render.cljs` sat in the parked-machine box on the
ruling map; the task doc listed it as boot glue with the spine, and it went with the spine.

---

*Placements = the two-second check + the decisions.md four-piece-floor ruling; verdicts =
the all-lights-out cut ruling (all Sid, 2026-08-19), applied to the traced editing inventory
of the same date — every tag from a traced call chain out of `dev/start! →
app.electric-flow/main → start-loop!` at `d507182`, every dependency claim from ns-form
greps. After-state from the execution log, NOW.md and commit `adc30c9` (78 paths, +443 /
−29,112). The code always outranks this page.*
