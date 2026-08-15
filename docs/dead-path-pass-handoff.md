# Dead-path deletion pass — handoff (mid-flight, 2026-08-16)

Rulings (Sid, in-session): **A delete (done, `f41b47b`) · B keep both (rama-side,
confirmed) · C delete all · D delete this form (want banked for a future
product form) · E keep seam_demo · F delete (PENDING — see step 2a).**
Census + rulings context: `docs/dead-path-census.md`. All judgment is done;
everything below is execution — typing and compiling against decisions
already made. Branch: `docs/current-mental-model-local`. Never push.

## State at handoff (committed as WIP in this commit)

Done, in this WIP commit (tree believed NEAR-green; not yet compiled):

- **Deleted (16):** dg_flow, jit, trail, shell, sidebar, runtime/sidebar_io,
  editor_compute, cmd_panel, combined_text, settings_view, text_input,
  themes, ui_primitives + loom files whose whole content was old-surface:
  runtime/agent_flow, runtime/workspace_actions, runtime/interop.
- **Rewritten for the always-ground boot** (the product boot was already the
  bare ground — `?dev` was the only consumer of everything deleted):
  - `runtime/state.cljs` — land-only atom map (kept: viewport/focus/scroll-y/
    settings/font*/mouse; GPU systems incl. the four pools + cmd/settings
    rect systems as empty stable-API carriers; trail-face pull lane; faces
    lane; NO editor/sidebar/flow/chat/agent atoms).
  - `runtime/fonts.cljs` — absorbed `font-defaults->settings` +
    `manifest-defaults->settings` (+ `compact-map`) from dead settings_view.
  - `runtime/keyboard.cljs` — global keys = ground escape only.
  - `runtime/scroll.cljs` — the wheel court only (region3d → ground;
    receipt counter preserved).
  - `runtime/mouse.cljs` — ground pointer routing + paste (ground branch) +
    `install-paste-intercept!` (T2 hook editing_runtime needs). drag-select
    and all zone handlers gone.
  - `runtime/render.cljs` — land frame loop: `<face-main` collapsed to
    `(m/latest (fn [fc] {:face-context fc}) (m/watch !face-context))` (its
    only consumer, `ground/on-face-bundle!`, reads ONLY :face-context —
    verified); store slots always composite (face-mode gate died with the
    editor it protected); slot-geo reconciler + scheduler + draw call
    preserved verbatim; draw-frame! API untouched (verifier depends on it);
    old feeds replaced by empty systems/false flags; diagnostics overlay
    kept. Kept hooks: install-due-deadline-consumer!,
    install-session-layout-provider! (editing_runtime consumes both).
  - `runtime.cljs` — installs ground UNCONDITIONALLY; wires trail-face/
    faces/block-edit arteries; consumers: resize, scroll, mouse, meta,
    global-keys, face-edit-keys, ground-keys, render. Blink/shimmer timers
    dropped (no consumers). `start-loop!` signature is now
    `[node device ctx geometry font-assets & {…artery kwargs…}]`.
  - `events.cljs` — dropped <editor/cmd/chat/settings-keys routers + the
    ctrl-k/g/b/s, ctrl-1/2/3, undo/redo/cut mappings. KEPT :eval
    (Ctrl+Enter = the ground's send), :copy, word-nav, arrows, editing keys,
    escape. `make-blink-timer` is now unconsumed (prune or leave).
  - `electric_flow.cljc` — lezer/sci/tokenize/layout-tokens/themes/
    source-code boot all gone; `main` rewritten: land atoms + pulls only,
    old Watch* subscriptions gone, extract overlay div gone,
    `Prepare-Geometry device pipelines [] font-assets font-config`,
    new start-loop! call. **Written but NOT compiled — check parens/refs
    first.**

## Remaining steps, in order

1. **Compile client** until green:
   `clj -M:dev -m shadow.cljs.devtools.cli compile dev` (needs `-Xss` no;
   needs node_modules — present). Expected first errors:
   - `verifier.cljs` calls `loop/start-loop!` — if it uses the OLD arity
     (tokenize fns etc.), update its call to the new signature (grep
     `start-loop!` in verifier.cljs). Also compile the verifier build:
     `clj -M:dev -m shadow.cljs.devtools.cli compile render-verifier`.
   - Any straggler reference to deleted nses/vars: fix by deletion of the
     dead branch, never by stubbing.
2a. **Group F first (its own commit, before C's server work):**
   `git rm src/app/server/review_pack.clj test/app/server/review_pack_test.clj`;
   in `server_jetty.clj` remove the require `[app.server.review-pack :as
   review-pack]` (line ~28) and the whole "===== Review Pack API ====="
   section (six route blocks, lines ~1886–1940, ending right before
   "===== Component Library Registry ====="). Server compile; commit:
   "dead paths: review-pack deleted (census item 23, Sid's ruling)".

2. **Server side of C** (grouped in the same C commit):
   - `server_jetty.clj`: delete the Linear API section (lines ~121–235:
     `linear-*`, `fetch-linear-issues`) and the old agent API:
     `!agent-runs`, timeout consts + `normalize-timeout-ms`,
     `summarize-agent-request`, `start-output-reader`, `run-cli-process`,
     `run-agent-request`, `run-agent-stream` (KEEP `preview-str`,
     `destroy-process-tree!`, `stream-cli-process`, `parse-stream-json-line`,
     `initial-stream-state`, `apply-stream-invariants`,
     `parse-stream-json-lines`, `write-event!` — the episode lane uses them).
     Delete routes: `/api/agent/run`, `/api/agent/stream`,
     `/api/agent/run-status`, `/api/agent/trail/save`, `/api/linear/issues`,
     `/api/sidebar/action`, `/api/sidebar/state`, `/api/settings/update`,
     `/api/settings/state`, `/api/workspace/save-truth`,
     `/api/editor/save-doc`, `/api/flow/save-state`, and `/api/home-dirs`,
     `/api/list-dir`, `/api/read-file` (only caller was sidebar_io — verify
     with `grep -rn "api/home-dirs\|api/list-dir\|api/read-file" src/`).
     KEEP `/api/dev/replay-fixture` (harness; census keep).
   - `file_viewer.cljc`: delete `WatchSidebarTruth`, `WatchUserSettings`,
     `WatchAgentTrail`, `WatchFlowSession`, `WatchWorkspaceTruth`; then
     grep-check `HomeDirs`/`DirContents`/`FileContent` + server halves
     (`list-home-dirs`/`list-directory`/`read-file-content`) — delete if
     caller-less after the route removals. KEEP face-ctx, trail-runtime-ref,
     Trail*, FacePull, RecordFaceWear, LiveEdges, WatchIngestEpoch.
   - `util_fns.cljc`: delete fns whose ONLY callers died — candidates:
     `get-sidebar-state`, `apply-sidebar-action`, `emit-sidebar-event!`,
     `get-settings-state`, `emit-settings-event!`, `get-agent-trail`,
     `get-latest-trail-run-id`, `save-agent-trail!`, `get-workspace-truth`,
     `emit-workspace-truth-event!`, `get-editor-doc`, `save-editor-doc!`,
     `get-flow-session-state`, `emit-flow-session-event!`,
     `get-cli-session`, `update-cli-session`, `submit-agent-run`,
     `get-agent-run`. RULE: grep each name across src/ + test/ first;
     delete only zero-caller fns. KEEP `!ingest-epoch-atom`, ingest/unitize/
     artifact/subscribe fns (land).
   - Optional same-commit prune: `agent.cljs` ticket half (lines 6–118:
     priority/normalize/parse-tickets/schema/parse-structured) — it compiles
     as-is, so this is hygiene, not a blocker; `stream-agent-run!` stays.
   - **Compile server**: `clj -J-Xss16m -M -e "(require 'app.server-jetty)"`.
3. **Test-side edits (same C commit):**
   - `test/app/face_primitives_test.clj`: G7's ui_primitives half retires —
     drop the read of `src/app/client/workspace/ui_primitives.cljs` + the
     forms-diff against it (its docstring: the copies "cannot diverge while
     both exist" — the original no longer exists; the cards.cljc half of the
     diff STAYS). Run: `clj -J-Xss16m -M:test -e "(require 'app.face-primitives-test)(clojure.test/run-tests 'app.face-primitives-test)"`.
   - `test/render_engine/verify_text_layout_fence.mjs`: remove the
     `combined_text.cljs` and `editor_compute.cljs` audit entries (lines
     ~25, ~28, ~146–151). Run `node test/render_engine/verify_text_layout_fence.mjs`.
   - Stale comment refs (reword, no behavior): `trail_face/cards.cljc:3`
     (cites trail.cljs), `test/app/face_integration_test.clj:6`,
     `test/app/face_assembly_test.clj:134`.
   - Focused tests: face_primitives, block_edit_test, trail_face_test.
4. **Commit C** (exact paths, one commit): message like
   "dead paths: the old workspace deleted (census items 5–17, Sid's ruling)
   — the product boot is the bare ground; loom rewritten land-only; jetty
   old-surface routes + Linear + old agent API removed; G7/fence-verifier
   audits re-pointed".
5. **Group D commit:** `git rm src/components/adapter.cljc compiler.cljc
   token_matcher.cljc css_parsers.cljc test/design_converter_e2e_test.clj`
   + `git rm -r components/` (root registry + _extractor.js; verify tracked
   with `git ls-files components/ | head`). In `server_jetty.clj`: drop
   requires (adapter, compiler, token-matcher; design-tokens too IF its only
   use was the extract route — grep `design-tokens/` in jetty) + routes
   `/api/extract/compile`, `/api/components/registry`,
   `/api/components/status`. `components/design_tokens.cljc` KEEPS.
   Compile both sides; commit.
6. **Deps rider commit** (separate): remove from deps.edn —
   `net.clojars.wkok/openai-clojure`, `datascript`, `image-resizer`
   (all zero-src-use, verified in census round). LEAVE
   `com.roamresearch/backend-sdk` (env.clj unreadable — Sid checks) and the
   pdfbox pair unless `grep -rn "pdfbox\|fontbox" src/ src-build/` is clean
   (Codex K21: slug_font uses AWT, not pdfbox). Server compile; commit.
7. **Census update commit:** add a Rulings section to
   `docs/dead-path-census.md` (the rulings at the top of this file,
   verbatim) + outcome receipts (commits, LOC). Delete THIS handoff file in
   that commit (its content lands in the census's outcome section).
8. Report to Sid: build green receipts + a felt-pass offer (boot the app —
   the browser-harness road memory has the proven headful recipe).

## Standing law for the finisher

Whole paths, never patches or comment-outs; commits grouped by concern;
build green per group; exact-path staging + `git status --short`/`git log
-1` before every git write (parallel sessions — see memory
`parallel-sessions-shared-branch-git`); never push; never read
`src/app/server/env.clj`; the fenced docs (DIRECTION/decisions/BETS/
next-prompt/sense-line-model/vision) stay unread.
