# Starter: build the ECS layer

Raw prompt below the line, in Sid's first person. Flavorings at the end; Sid
picks one and appends it. The plan this was cut from is `PLAN.md` beside this
file; a Claude room does not load it, a Codex room gets it attached.

---

For the past few sessions I have been thinking about what to do next, since
the 3D and path work is done. What I want is the minimal layer we can build on
top of the existing code. This was always supposed to sit on an ECS layer and
we don't have that. That is the goal. So let's put money where mouth is: build
it, in a separate worktree off main, and once the build exists we will arrive
at the next layer.

I think there is a lot we can do with Softland using Softland. There is value
in all three, text, 2D and 3D, to see the flow of the architecture live and
analyse it. You are the best at UI, so I expect whatever you make to be quite
high quality.

The bet this build tests. The client is not a screen. Last time I built a
screen, twice, and what I stood in front of was a page of blocks whose anatomy
was code: typing slowed down, click and cursor disagreed, a paste took half
the space, ctrl-enter never showed me the model, effort or precontext, I typed
kinds by hand, the reply landed under the prompt. This time the client is a
loop over records. The floor already has the records, a pure runner for tool
records, and a painter. What is missing is the loop that ticks them and the
record that says where I stand. Four systems in code, the wire, the pointer,
the clock, the deliverer, plus a few record kinds: where I stand, event,
identity, trace, stroke. The test: with only these, I point at anything on
screen and it tells me what it is, where it is and who made it, at the grain
of the record; and I change a tool record from where I stand and it takes hold
live, with no engine code touched to make it so.

What exists, checked this week, so you don't rediscover it:

- The executor, `src/app/client/engine/executor.cljc`: `run [record scope
  capabilities opts]`, record is `{:program :roots}`, statuses `:complete
  :suspended :answered :stale :refused :error`, plus `resume`, `encode`,
  `decode`. It holds no state and no clock. The recipe it compares on resume
  leaves out roots that only feed the loop's items, so a record whose items
  grew resumes and continues (executor_test.clj:58, brush_test.clj:103-105).
  `:until N` suspends before item N; a completed run has no continuation, and
  `:until (count items)` does not suspend. Steps are meaning-level ops
  (`:curve-point :surface-region :read-surface :mix :paint`, `:path/source
  :path/dabs`), never draw ops.
- The brush, `src/app/client/region3d/records.cljc`: `host` (a sphere, id
  "G", R 200, charts, marks A and B, bindings) and `pickup` (roots support,
  coating, a 64x32 painting, tool {carry pickup opacity radius}, and four
  authored events on a curve; loop steps at, picked, carry, footprint,
  painted). Its items are authored events, not pointer input. Drivers:
  brush_test.clj:55-74, brush_wire.clj (`clj -M:test -m
  app.client.region3d.brush-wire save|resume DIR`), harness/coating.cljs:42-76.
- Surface and sampling: `engine/surface.cljc` `paint` and `sample` are pure
  float arrays; `region3d/coating.cljc` `locate` computes a row per binding
  with `:binding :record :mark :inside :distance` and keeps them under
  `:bindings`, while `sample` carries only contributor names.
  `region3d/support.cljc` has `point->chart` and `chart->point`.
- Screen to sphere: `region3d/scene.cljc` `camera-matrices`,
  `ray-from-region-point`, `pick-region` (a world point on a primitive mesh,
  needs a camera and a maintained scene). Nothing joins that point to
  `point->chart`. Path hit: `path/component.cljc` `classify` in local units
  with `engine/transform.cljc` `inverse-point`. Text hit:
  `text/layout.cljc` `hit-test-result` gives line and col; `caret-result` and
  `selection-result` exist.
- The sphere on screen: `region3d/renderer.cljs` draws a region value (a
  scene of primitive meshes with a view and lights; see how
  harness/region.cljs:96-135 builds one). It has never taken the coating or
  the painting. The painted coating has never been on the rendered sphere.
  The frame order the harness uses is region.cljs:373-418.
- Compositor, `engine/compositor.cljs`: `create-compositor!`,
  `acquire-target!`, `begin-target-pass!`, `draw-present!` (takes an output
  view), `release-after-submit!`. Nothing in `src/app/client` ever gets a
  webgpu canvas context or configures one. Readback:
  harness/shared.cljs:265-290. PNG encoding: `engine/surface_png.cljc`.
- Path: `path/construction.cljc` `construct` runs a path record through the
  executor into a component; `path/renderer.cljs` `init-path-system`,
  `push!`, `frame!`, `draw-path-frame!`. The renderer must never enter the
  executor (harness/path_production.cljs:18-22). A path record's width rule is
  `[:* [:get :size] [:get :p]]` (path/records.cljc:60-66).
- Text: `text/layout.cljc` `layout` is pure and does not use the executor;
  `text/renderer.cljs` `init-text-system`, `update-text-data`,
  `draw-text-system!`. Text's red is one stale reader in
  harness/text.cljs:486-491 (`result-runs` is the fix); the verifier gate does
  not include that check. Fonts load from `/fonts/manifest.json`
  (`text/fonts.cljs`), `dejavu-sans-mono` and `ubuntu-sans-variable`, with
  HarfBuzz WASM.
- There is no client. The only browser entry is the render verifier
  `app.client.harness.core/start!` (offscreen textures, readback, a result on
  `window`). No animation frame, no listeners. No HTML in `resources/public`.
  Builds in `shadow-cljs.edn`: `:render-verifier` and `:shaper-border-probe`,
  no dev-http. The verifier (test/render_engine/run_verifier.mjs) synthesizes
  the page and origin and launches Chrome with `--enable-unsafe-webgpu
  --use-angle=swiftshader`. Shadow's server starts with `clj -A:dev -M -m
  shadow.cljs.devtools.cli server`. Tests: `clj -X:test`, pure lane list in
  test/app/test_runner.clj:31-70.
- The wire: every server route is POST EDN in
  `src/app/server/door/server_jetty.clj` `wrap-file-api` (relation/assert,
  matter-room/*, episode/utterance as SSE that spawns claude, block-birth,
  geometry). No route reads a record. The store is Rama
  (`src/app/server/rama/object_container.clj`, `relation_kernel.clj`);
  `LAND_CLUSTER=0` runs it in-process. The edit path forces kind to
  `:document` or `:text-block` and visibility to `:private`
  (object_container.clj:1493-1497). Never open `src/app/server/env.clj`.
- On this box, CDP screenshots of a WebGPU canvas came back black once; the
  verifier's texture readback is the receipt path that works. Headful Chrome
  on DISPLAY :0 with `--use-angle=vulkan --enable-features=Vulkan,WebGPU,
  UnsafeWebGPU --ignore-gpu-blocklist --no-sandbox` gets the real adapter.

Since it's ECS and the current client is this bare, you will have to build a
few base components. I want to see them as checkpoints and milestones of what
these different layers are. This is the ladder as I see it now; it might
change as you build and that is fine, but I should know:

- C0, the page and the present: a third build, one HTML page with one canvas,
  the device, one font, the compositor presenting to the canvas; the sphere on
  screen with the pickup painting beside it as a flat image; resize; a
  readback hook. I see the sphere at localhost and a PNG from readback.
- C1, the tick and the deliverer: events as records, a tick that runs only when
  something changed, a stroke record that grows under my pointer, a second
  brush record whose events are my dabs, resume when the record only grew and
  run when the recipe changed, a trace row per tick. I paint into the chart by
  hand and I see ms per tick across a stroke.
- C2, the skin: the composed coating baked onto the rendered sphere. I see my
  strokes on the sphere under the pointer.
- C3, the pointer and the inspector: an identity record each tick from what's
  under the pointer (sphere: pick to unit vector to the coating's binding
  rows; text: line to the record it came from), shown by a text tool running
  through the executor. I hover a mark and read binding, record, mark, tool,
  revision; I hover the inspector and it tells me which line and which record.
- C4, three tools, one subject: the trace as flow lanes drawn by the path tool
  with width from time, the trace as a log drawn by the text tool, my strokes
  on the sphere; pointing at a thick spot names the run that took the time.
- C5, where I stand: the vantage as a record (subject, tools open with their
  places, camera, pins, where it came from); two of them; switch and return;
  the chain drawn as a small path.
- C6, change a tool from where I stand: point at a value in the inspector,
  type, the record is replaced with a new revision, dependents re-run; the
  brush radius and the flow's width rule; the caret stays; the commit diff
  touches no engine file to make it take hold; ms per keystroke in the trace.
- C7, the wire: the place's records read from and written to the store; reload
  and I am back where I stood. This needs server work, and that is the one
  place you stop and ask.

Keep a `CHECKPOINTS.md` in `docs/below-the-waist/ecs-layer-2026-09-08/`, one
entry per component: what it is, where the receipts are, the numbers that
mattered, what moved in the ladder and why, and what you filled in with your
own judgment. One commit per component, tests and docs in the same commit.

Don't ask me first. Fill the gaps yourself; whatever I say, take it as an
approximation. Once you get to a state where you tried everything but can't
build forward, write me up what you were going to build, what all you tried,
where you're at, and what the missing critical piece is that needs my
approval.

---

## Flavorings

A Claude room fills what the ladder leaves open (the chart math for the skin,
the shape of a continuation that outlives a completed run, the look) with its
own judgment and can check every line of the terrain against the code as it
goes. A Codex room lands exactly the shapes the plan gives and stops where
they stop, so it gets the plan attached with the record kinds and the tick
spelled out. The three exploration flavorings (widest, one mechanism, whole
system) do not fit a build and are left out.

```
claude, build: work in a worktree off main; read no docs, vision, board or memory files beyond what you already have; code as you need it; never src/app/server/env.clj.
codex, build: attached: docs/below-the-waist/ecs-layer-2026-09-08/PLAN.md. i am asking you to build the ECS layer component by component as the plan lays it out, and what i want from it is a place i can stand in by C6, with CHECKPOINTS.md telling me what each layer is.
```
