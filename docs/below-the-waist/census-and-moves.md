# Below the waist — census and moves

The ground for the below-the-waist phase chat. Census taken 2026-08-18 by an
opus gatherer (static reading only: require graph + call-site grep, no runtime
probe) — single-pass and unfalsified: the phase chat runs Codex adversarially
over each row (try to prove it live/reachable) before any ruling (Sid,
2026-08-19: Codex is "better than opus for hunting and figuring and also
adversarial"). Adjudication verdicts by the derivation seat; **nothing here
is ruled by Sid yet** — rulings happen in the phase chat, row by row, exact
paths.
The underlying code always wins over this file.

## The goal (Sid, 2026-08-19, verbatim)

> "i do want to get to the state where large parallelisation and concurrent
> ui/ux and all other product work i can do above the waist — everything
> below it right now is sync and linear to this — has to be done"

The phase ends when the below-waist substrate is settled enough that product
work above it can fan out in parallel without being able to break the kernel.

## Naming gloss (two unrelated things share a word)

- **trails** — motion leaving traces. RULED yes 2026-08-18 (stamps never
  paths; thresholds are userland dials; a burst's settlement always
  persists). Recorded at `docs/ARCHITECTURE.md` conflict 2 and
  `docs/electric-native/PROBLEM-SPACE.md` forks-by-decider.
- **trail-face** — the old docs-reading surface (cards/lanes/threads over the
  ingested corpus). Dark client code, retire-candidate below. Its server-side
  ingestion is live artery and is NOT touched by retiring the drawing files.

## The four moves (and the two laws)

1. **Retire whole** — dead iterations nothing live mounts. True deletes,
   whole paths, tests included, builds green (the dead-path-census pattern).
2. **Hoist** — decisions buried as constants become facts with a code-held
   floor default (the facet-master pattern; wrap-columns is the precedent).
   Same behavior day one; the decision becomes visible and dial-able.
3. **Demote with duties, never delete** — wrong-shaped-but-live paths (the
   whole-page pull artery). Build the keyed path beside it, transfer the
   named duties, then the old path steps down to a checking role.
4. **Melt by replacement** — fused monoliths are never refactored in place;
   the treaty-shaped replacement is built small, and when it carries one real
   case the monolith retires whole.

Laws: **no duty-carrier is removed before its replacement carries the
duties**; and **the moves finish the waist** — every hoist and melt forces
"what fact-kind or verb does this actually need," so the waist member list is
discovered by migration, never guessed in the abstract.

## Headline census finding

**The old pre-render-engine Electric-DOM block editor no longer exists in
src/.** It died in the dead-path census (Sid's ruling 2026-08-15) — headers
say so at `src/app/client/workspace/runtime.cljs:5`,
`runtime/mouse.cljs:5`, `runtime/state.cljs:6`. localhost:8080 boots exactly
one UI element (a full-screen canvas, `electric_flow.cljc:331-335`) plus the
scene loop (`runtime.cljs start-loop!`). The typing Sid does there is the
live scene surface: `ground.cljs` minting edit envelopes through
`block_edit.cljc` — alive, keeps.

## Entry map (what actually boots)

`shadow-cljs.edn :dev` → single module `dev/start!` → `src-dev/dev.cljc:77`
`e/boot-client … app.electric-flow/main` → `electric_flow.cljc:201 main` →
canvas at `:331-335` → `loop/start-loop!` at `:357` →
`workspace/runtime.cljs`: trail wiring `:49-53`, face wiring `:56`,
block-edit wiring `:78`, `ground/install-ground!` `:92`, consumers
`:130-135`, render consumer `:138` (region3d self-gated `?region3d=1`).
Flag-gated second world `?live-atoms=1` (`live_atoms.cljs:254`): chrome,
frame, **T2** (`editing-runtime/boot!` `:361`), seam-demo. Separate build
`:render-verifier` (entry `substrate/webgpu/verifier.cljs`, ~6,000 L) is
exercised by `package.json verify:render-engine` — NOT dead code, and it
inflates rev-dep counts (requires editing_runtime, text_editing, live_atoms,
chrome_runtime, frame_runtime, live_edges).

## Pile 1 — retire whole (Sid's yes/no per row)

**The trail-face island** — one closed island, ~1,776 lines; `scene.cljc` is
the only door in and nothing in src/ opens it; render loop has zero 'trail'
hits (`grep 'trail' runtime/render.cljs` → none); its three atoms
(`!trail-text !trail-feed !trail-bundles`, created `runtime/state.cljs:138-140`,
reset by `trail_face/wiring.cljs:79,81,83`) are read by nothing:

| path | lines | mounted by | rev-deps in src |
|---|---|---|---|
| `src/app/client/workspace/trail_face/scene.cljc` | 799 | nothing | 0 (test only) |
| `src/app/client/workspace/trail_face/cards.cljc` | 415 | nothing | 1 (scene) |
| `src/app/client/workspace/trail_face/threads.cljc` | 305 | nothing | 1 (scene) |
| `src/app/client/workspace/trail_face/lanes.cljc` | 163 | nothing | 2 (scene, threads) |
| `src/app/client/workspace/trail_face/text_face.cljc` | 94 | nothing | 1 (scene) |

Their tests retire with them: `test/app/client/workspace/trail_face_test.clj`,
`test/app/face_integration_test.clj` (referencing). **STAYS:**
`trail_face/wiring.cljs` + `trail_face/sanitize.cljc` (installed at boot,
`runtime.cljs:50`). Follow-up ruling for the phase chat: whether wiring's
trail-pull arms demote once the island is gone (they currently feed the dark
atoms every epoch — lights on in an empty room).

**Dead symbols inside live files** (zero callers in src/ each; file stays):

- `src/global_flow.cljs` — everything except `await-promise` (`:80-87`;
  `electric_flow.cljc:21` uses only that one fn).
- `src/app/file_viewer.cljc:126` `TrailConversation`.
- `src/app/client/workspace/block_edit_wiring.cljs` — `face-click!` `:247`,
  `face-blur!` `:240`, `edit-focused?` `:235`, `submit-envelope!` `:259`
  (ground has its own private one at `ground.cljs:2548`), `<focused-view`
  `:113`, `<block-view` `:124`. Coupled fact: `face-edit-keys-consumer` IS
  joined at `runtime.cljs:134` but its only `:face-edit` focus-writer is the
  zero-caller `face-click!` — statically it can never fire (static-only claim).
- `src/app/client/workspace/block_edit.cljc` — `display-text` `:256`,
  `refusal-notice` `:265`, `overlay-face-context` `:271`, `dismiss-refusal`
  `:192`.
- `src/app/client/workspace/ground.cljs:69` `ground-boot?` (the dead `?dev`
  switch).

**npm residue** — src/ requires only `bidi-js` and `harfbuzzjs/{hb,hbjs}.js`
(`text_shaper.cljs:8-10`). Unreferenced from src/: all eight `@codemirror/*`,
five `@lezer/*` + `@nextjournal/lezer-clojure` + `lezer-clojure`,
`@radix-ui/themes`, `react`, `react-dom`, `recharts`, `prop-types`,
`web-tree-sitter`, `w3c-keyname`. Caveat before cutting: confirm the build
needs none transitively.

## Pile 2 — strip the ghosts (surgery inside live files)

The old workspace died in the census; the renderer still pays its rent every
boot — buffers allocated for panels hardcoded invisible:

- `runtime/state.cljs:82-94` `:!cmd-rect-sys` (16-rect GPU buffer) →
  `:cmd-panel-visible` hardcoded `false` (`runtime/render.cljs:485`).
- `runtime/state.cljs:95-107` `:!settings-rect-sys` → `:settings-visible`
  hardcoded `false` (`render.cljs:490`).
- `runtime/state.cljs:116-127` sidebar shadow + sidebar pools →
  draw-info only (`render.cljs:498-499`).
- `render.cljs:494` `:agent-visible false` hardcoded.
- `ground.cljs:4729` reads `:!cmd-panel` — no longer created; always nil.
- Renderer branches kept alive for these:
  `substrate/webgpu/renderer.cljs:2975, 2988, 2993, 2998, 3007, 3027, 3076,
  3092-3093, 3736-3750, 3841-3849`; key sets
  `substrate/frame_inputs.cljc:32-34, 44-51, 71`.

## Pile 3 — demote later, never delete now (the artery; duty inventory)

Carries the app's only data path today. This inventory is the transfer list
the keyed-wire work consumes (electric-native DIRECTION 1c).

Client: `electric_flow.cljc` — 26 mirror atoms `:210-250`, 12 pull loops
`:251-311`, `submit-block-edit!` `:24-86`, `SubmitBlockEdit` `:88` ·
`face_wiring.cljs` — `install-face-wiring!` `:631`, fetches `:371,387,399` ·
`trail_face/wiring.cljs` — epoch → 1s debounce → re-arm `:24-84` ·
`block_edit_wiring.cljs` — outbox/result seam + single-unit narrowing pull
`:168-225` · `live_edges.cljc` — boot-static connector pull ·
`runtime/state.cljs:134-153` — the landing atoms.

Server: `file_viewer.cljc` — the 8 bridge e/defns (`FacePull:384`,
`RecordFaceWear:409`, `WatchIngestEpoch:133`, trail reads) ·
`face_projection.clj` — registry `:1815`, `serve:1888` · `face_arsenal.clj` ·
`trail_view.clj` · `util_fns.cljc:103` the one epoch mirror ·
`server_jetty.clj` — ws middleware `:51`, 12 HTTP routes `:1462-1670`, second
edit call site `:839` · `ingest_watchers.clj`, `episode.clj`,
`machine_cut.clj`, `object_container.clj` + runtime, `material_circulation.clj`,
`facet_master.clj`.

All epoch-bump sites (13 + accept-side): `file_viewer.cljc:234,291,301` ·
`server_jetty.clj:1629,1662` · `machine_cut.clj:619,809` · `episode.clj:961` ·
`material_circulation.clj:756` · `facet_master.clj:220,336` ·
`ingest_watchers.clj:199` · `electric_flow.cljc:78`.

## Pile 4 — the join (T2 eats real blocks)

The one real editor is an island: flag-gated `?live-atoms=1`, fed by two
hardcoded fixture documents (`editing_runtime.cljs:23-44`,
`install-fixtures!` `:272-285`, called from `boot!` `:739`), no server data
path (no FacePull, no fetch — checked). The move is not removal: feed it real
facts — the keyed wire's first customer ("prove it by making a block").
Landmines mapped: T2's mount rides `live_atoms.cljs`, which requires
`ground.cljs` (camera provider `:364`); `seam_demo.cljs` dies with T2;
T2-exclusive deps: `editing_segmentation.cljs`, `text_editing.cljc`
(+ verifier). Shared deps (deleting them takes T2 down): containers, events,
rect_tree, runtime/mouse, runtime/render, scene_runtime, scene_store,
text_layout.

## Scene-loop verdict (adjudicated, unruled)

Skeleton fits the settled shape (checked, static): keyed scene store with
incrementally-maintained index, flat keyed renderer frame, closed family
registry, identity-gated pools, camera outside the graph at frame clock,
faces-as-data with "no logic ever" guard. Three misfits, each already in a
pile: feeding (mirror atoms + whole pulls → pile 3), editing welded inline
into `ground.cljs` (~5k lines; melts later into primitive verbs +
cursor-entities + keymap-facts via the T2 join — pile 4), policies as
constants (caps, sizes, visibility flags → hoist).

## Unknowns / caveats (honest edges)

- Everything static: require graph + call-site grep. No runtime probe ran.
- Test-runner inclusion unread (`deps.edn:test` → `app.test-runner/full`):
  which dead tests are in the default suite gets checked at removal time;
  builds + suite green is the gate after every removal.
- npm transitive needs unverified before cutting residue.
- `resources/public/faces/` observed, not opened ("W1 HTTP fetch … RETIRED"
  per `face_wiring.cljs:15`); `style.css` is 0 bytes.

## Pointers (read before ruling; plain-words law applies to all writing)

- The recon: `docs/what-softland-is/build-softland-in-softland.md` (+
  `-threaded.md` for depth). The treaty, waist, four forces, fence
  instrument, clocks, the door.
- The wire seam: `docs/electric-native/DIRECTION.md` (road; 1c is the keyed
  wire), `PROBLEM-SPACE.md` (loads, forks), `RECON.md` (receipts).
- Principles: `docs/ARCHITECTURE.md` conflicts 2 (becoming/truth), 3
  (identity travels), 5 (cost follows attention); trails ruling recorded at
  conflict 2 and in PROBLEM-SPACE forks-by-decider.
- Register laws live in session memory (plain English to Sid; exact paths
  for anything he rules on; his deletions are rulings; no Co-Authored-By;
  commit per concern on `docs/current-mental-model-local`, never push).
- Delegation for this phase (Sid, 2026-08-19): **Codex does the code
  hunting/understanding and the adversarial checks** — package rounds with
  the ask-codex-for-feedback skill, ingest with ingest-codex-feedback;
  opus subagents only for mechanical in-harness collation; the main seat
  thinks and adjudicates.

## Boot prompt for the phase chat (copy from here)

> Read `docs/below-the-waist/census-and-moves.md` first — goal, census with
> receipts, the four moves, pointers. Then the recon faces it points at.
> This chat owns three things, Sid ruling row by row in plain English:
> (1) the removal rulings — retire / strip / demote-later / join, per the
> piles; (2) the waist member-walk — the floor's actual fact-kinds and
> verbs, discovered through the moves against what the repo already grew,
> never guessed in the abstract; (3) doc convergence — every ruling lands in
> the docs same turn, in Sid's register, so he ends the phase holding the
> full picture. Gathering and code-understanding run through Codex,
> adversarial by default: before Sid rules any retire/strip row, Codex gets
> a falsification pass on it (prove it live/reachable), packaged via the
> ask-codex-for-feedback skill and ingested via ingest-codex-feedback. The
> phase's finish line, his words: "large parallelisation and concurrent
> ui/ux and all other product work above the waist."
