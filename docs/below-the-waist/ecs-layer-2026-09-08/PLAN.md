# The ECS layer: the plan

Sid's words for this: the ECS layer, the minimal layer on top of the existing
code, the client, Softland using Softland to see the flow of the architecture
live. Names minted here are marked (mine) and are his to rename.

Grades used throughout: **checked** = a hunter read the code this session and
the file:line is given; **derived** = follows from checked facts; **assumed** =
mine, unverified. "Works" appears nowhere below.

## 1. The hypothesis

The client is not a screen. It is a loop over records. The floor already has
the world (records), a pure runner of tool records (the executor) and a painter
(surfaces, renderers, the compositor). What is missing is the loop that ticks
them and the record that says where you stand. Four systems in code plus a few
record kinds make the floor a place:

- systems in code: the wire, the pointer, the clock, the deliverer
- record kinds: where-you-stand (vantage, session 3's name), event, identity,
  trace, stroke

Test: with only these, pointing at anything on screen returns what it is, where
it is and who made it, at the grain of the record; and a tool record edited
from where you stand takes hold live without engine code.

Where it would fail, in order:

1. C1: the trace shows tick time growing with stroke length past the pointer
   rate. Then the executor's loop shape is the ceiling and the fix is in the
   executor, not the client.
2. C3: the identity under the pointer is not the grain Sid thinks at (a
   binding row when he means the mark, or the reverse).
3. C6: a tool edit needs a special case in engine code to take hold.
4. C7: a tool record does not come back from the store as itself (today it
   cannot: the edit path forces the kind).

## 2. What exists (checked)

**Executor.** `src/app/client/engine/executor.cljc`. `run [record scope
capabilities opts]` (334-340); record is `{:program :roots}`; opts read:
`:budget :until :stop-at :answer :records`. Statuses: `:complete` (313-316),
`:suspended` with reason `:pending|:needs-policy|:until` and a `:continuation`
(269-274, 306), `:answered` (executor_read.cljc:44-46), `:stale` (331),
`:refused` (93-94), `:error` (228-232). `resume [continuation capabilities
opts]` with `:record`/`:scope` in opts (357-373), `encode` (342-344), `decode`
(346-355). Grammar in `admission!` (96-136): `{:steps [{:out :op :args}]
:each {:items :item :fields :state :steps :next} :return}`, one loop. Docstring
line 5: "Holds no state or clock"; test executor_test.clj:25 "no clock".

- The recipe (159-164) is `{:program :roots}` restricted to roots reached by
  loop steps, `:state` and `:next` (138-157). A root referenced only by
  `:items` is not in the recipe. So appending items and resuming continues:
  executor_test.clj:58, brush_test.clj:103-105. `:until N` suspends before
  item N (299-306); the continuation carries `:consumed` as `(select-keys item
  (:fields each))` projections (166-170) and refuses when they differ
  (287-294). A root used in a loop step that changes refuses `:recipe-differs`
  (367-371); brush_test.clj:71, harness/pickup.cljs:37,58.
- A completed run returns no continuation (313-316). `:until (count items)`
  does not suspend because the guard requires `at < count` (299-306).
- Pending reads: a capability with a `:snapshot` fn may return `{:status
  :pending ...}`; the request is keyed by `[:phase :recipe :consumed :item
  :state :at :step :snapshot]` (executor_read.cljc:8, 28-30); an answer rides
  in opts as `{:answer {:request :value}}` (executor_pending_test.clj:69-74),
  addressed by `[:phase :at :step]` (32-34); refused as `:stale` for a second
  use, any differing part, or never consumed (36-40; executor.cljc:309-311).
- A capability is `{:args :needs :run (fn [args ctx]) :snapshot?}`; ctx carries
  `:budget` (executor_pending_test.clj:8-15). Tables carry a `:vocabulary`.
- Steps are meaning-level: region3d ops `:curve-point :surface-region :member
  :read-surface :surface/new :mix :paint :sample` (region3d/capabilities.cljc:
  48-69); path ops `:path/source :path/envelope :path/dabs :path/regions ...`
  (path/construction.cljc:18-37). No vertices, draw ops or entities anywhere in
  these files.

**The brush.** `region3d/records.cljc`: `host` (8-28: `:kind :sphere :id "G"
:R 200.0`, charts, marks A and B, bindings per revision); `pickup` (60-81:
roots `:support :coating :painting` (a 64x32 chart surface) `:tool {:carry
[1 0 0 1] :pickup 0.25 :opacity 0.5 :radius 8.75}` and `:events`, four
`{:id "dab-i" :curve "kA/arc-AB" :t t}`; loop steps `:at :curve-point` ->
`:picked :read-surface` -> `:carry :mix` -> `:footprint :surface-region` ->
`:painted :paint`; `:next {:carry :painting}`; `:return {:painting :carry}`);
`program-record` (30-31). Items are authored events, not pointer input. The
only suspension is coating work over `:budget` (coating.cljc:43-44). Drivers:
brush_test.clj:55-74 (JVM), brush_wire.clj:33-57 (`clj -M:test -m
app.client.region3d.brush-wire save|resume DIR`), harness/coating.cljs:42-76
(browser twin: 847 painted texels, `[64 32]`).

**Surface and sampling.** `engine/surface.cljc`: `paint` (76-107) is pure,
float arrays in and out; `sample [stack point filter ctx]` (133-159) returns
`{:status :color :contributors :covered? :snapshot}`. `region3d/coating.cljc`
`locate [host revision point {:budget :order}]` (27-73): each binding's row
keeps `:binding :record :mark :inside :distance :chain` (55-57) and the rows
survive as `:bindings` (73); `sample` carries only the `:contributors` names
(coating.cljc:58-59, 72; surface.cljc:149-152). `region3d/support.cljc`:
`point->chart [host chart-id point]` (50-53), `chart->point` (46-48), `domains`
(80-93).

**Screen to sphere.** `region3d/scene.cljc`: `camera-matrices [view viewport]`
(739-762), `ray-from-region-point [camera [x y]]` (764-777), `project-point`
(779-802), `pick-region {:maintained :camera :region-point}` -> `{:route
:object :object-id :point3 :normal :t ...}` or `{:route :region-background}`
(965-983; BVH over primitive meshes; throws without a camera). No file joins
`pick-region`'s `:point3` to `support/point->chart`. Callers today:
harness/region.cljs:613-645, 1034-1044.

**The sphere on screen.** `region3d/renderer.cljs`: `init-region3d-system!
[device camera-buffer groups-buffer]` (512-537), `attach-compositor!` (572-594),
`prepare-region3d-frame! [system {:regions} session {:zoom :dpr
:world-transforms :font-assets ...}]` (1023-1033), `encode-region-pass! [system
encoder region-id role lease]` (1320-1345), `composite-region! [pass system
region-id]` (1386-1407). A region value (harness/region.cljs:96-135):
`{:region/id :region3d/version 1 :extent {:width 640.0 :height 360.0 :depth
100.0} :background :ambient :view {:pivot :distance :yaw :pitch :lens} :scene
{id mesh}}`; a mesh `{:object/id :object/kind :mesh :transform {:translation
:rotation :scale} :mesh {:kind :sphere :params} :component {:base-color
:metallic :roughness :emissive}}` (62-70, 120-122). The renderer never takes
the coating, the painting or a surface array; the drawn sphere is
`scene/sphere-mesh` (scene.cljc:295) with a flat material. The painted
coating has never been on the rendered sphere. Frame order the harness uses:
region.cljs:373-418 (offscreen texture, leases, encoder, region pass,
`acquire-target!`, scene pass with `composite-region!`, `draw-present!`,
submit, `release-after-submit!`, readback).

**Compositor.** `engine/compositor.cljs`: `create-compositor! [device
output-format]` (380-397), `acquire-target! [pool format w h label]`
(192-199), `begin-target-pass! [encoder target load-op]` (632-643),
`draw-present! [compositor encoder scene output-view output-format]` (645-672,
linear to sRGB), `release-after-submit!` (674-682), `active-region-leases!`
(700-712), `destroy-compositor!` (604-610). Submission is caller-owned. There is
no `getContext "webgpu"` and no `configure` anywhere under `src/app/client`.
Readback: `harness/shared.cljs` `w4-read-texture!` (265-290). PNG encoding
exists in `engine/surface_png.cljc`.

**Device and fonts.** Adapter and device: harness/core.cljs:39-50. Camera
uniform: `engine/device.cljs` `create-camera-buffer` (139-147),
`update-camera [device buf floats pan-x pan-y zoom w h]` (149-160),
`create-groups-buffer` (75). Fonts: `text/fonts.cljs`
`load-font-manifest-async` (15-24) fetches `/fonts/manifest.json`;
`load-font-assets [config]` (120-173) -> `{:layout-provider ...}`; manifest
holds `ubuntu-sans-variable` and `dejavu-sans-mono`; HarfBuzz WASM at
`/fonts/harfbuzz-0.10.3.wasm` (shaper.cljs:21). All under
`resources/public/fonts`.

**Path.** `path/construction.cljc` `construct [record] | [record extensions]`
(58-77) runs the executor (64-66) and returns a component `{:path/material-id
:path/revision :path/value :path/paint :path/parameters}`; smallest record
path/records.cljc:60-66 (`:path/tool {:size 16 :fit :polyline :streamline 0}`,
`:path/source {:kind :pen :samples [[x y p] ...]}`, `:path/paint {:stroke
{:width [:* [:get :size] [:get :p]] :color [...]}}`). `path/renderer.cljs`:
`init-path-system [device fformat camera-buffer groups-buffer initial-view]`
(115-125), `push! [system {:upsert :remove :order :groups}]` (158-162),
`frame! [system view]` (164-167), `draw-path-frame! [pass system]` (179-182),
`item-range` (190-193); an item is `{:path/material component :container
group}` (harness/path.cljs:57-60). Hit: `path/component.cljc` `classify
[record point slop view]` -> `:inside|:boundary|:outside` (204-213), `hit?`
(215-218), `boundary-distance` (220-223), in component-local units;
`engine/transform.cljc` `inverse-point [world-transform [px py]]` (301-312).
The renderer must not enter the executor: harness/path_production.cljs:18-22.

**Text.** `text/layout.cljc` `layout [{:text :provider :font-size :line-height
:inline-size :wrap-policy :zoom ...}]` (960-967) is pure and does not use the
executor; result (919-967) has `:lines`, each with `:runs` (901), `:baseline`,
`:logical-bounds`, `:ink-bounds`, `:glyph-start/:glyph-end` (868, 887-898);
`result-runs` flattens (503-506). `hit-test-result [layout [x y] opts]` ->
`{:line :col :index :affinity ...}` (1487-1492); `caret-result` (1333),
`selection-result` (1372). Text's red is one stale reader: harness/text.cljs:
486-491 read `:runs`/`:clusters` at top level; the verifier gate
(run_verifier.mjs:560-564) does not include that check. `text/renderer.cljs`:
`init-text-system [device fformat camera-buffer font-assets]` (288-297),
`update-text-data [device state texts font-assets font-size & {:line-height
:world-transforms}]` (619-651), items `{:text :x :y :size :r :g :b :a
:container}` with optional `:layout-result` (482-533), `draw-text-system!
[pass sys attachment-size]` (718-730).

**The client shell.** There is none. The only browser entry is the render
verifier `app.client.harness.core/start!` (core.cljs:168-204): device, fonts,
drivers, then `window.__renderVerifierResult`. No animation frame, no
listeners, no timers beyond boot and fetch retries (grep). It draws to
offscreen textures and reads pixels back; no canvas is ever presented. Build:
shadow-cljs.edn `:render-verifier` (4-16, `:optimizations :simple`, output
`target/render-verifier/js`) and `:shaper-border-probe` (17-26); no
`:dev-http`; nREPL 9002. No HTML under `resources/public`. The verifier
synthesizes the page and origin (run_verifier.mjs:65-67, 69-135), launches
Chrome with `--enable-unsafe-webgpu --use-angle=swiftshader` (23-28) and waits
on `window.__renderVerifierDone`. Commands: `npm run verify:render-engine`
(package.json:9); server `clj -M:dev -m dev` -> Jetty 8080 (server_jetty.clj:
1627-1636); tests `clj -X:test` (deps.edn:43-46; pure lane list
test/app/test_runner.clj:31-70). Shadow's server starts with `clj -A:dev -M -m
shadow.cljs.devtools.cli server` (memory quirk).

**Old locators.** All pure, none called outside the harness and tests:
`pick-region` (above), `path/component.cljc classify`, `text/layout.cljc
hit-test-result`, `image/component.cljc half-open-hit?` (177).

**The wire and the store.** Every route is POST with an EDN body, in
`server/door/server_jetty.clj` `wrap-file-api` (1389-1606): `relation/assert`
(1397), `matter-room/*` (1416-1475), `material/facet-master/drill` (1488),
`episode/utterance` as SSE that spawns `claude` (1523), `episode/block-birth`
(1545), `episode/geometry` (1580). No GET, no websocket, no route that reads a
record. The store is Rama: `server/rama/object_container.clj` (module 1750)
and `relation_kernel.clj` (638); `LAND_CLUSTER=0` flips to an in-process
cluster (cluster.clj:33-39). Reads exist server-side only (runtime.clj:
`read-container` 363, `read-revision` 380, `read-unit` 386,
`read-source-anchors` 263; `rk/read-relations-for-targets`
relation_kernel.clj:951). The edit path recomputes kind as `:document` or
`:text-block` and forces `:private` (object_container.clj:1493-1497). Branch
`{:branch/id "main"}` is written (envelope.clj:16,182,210) and never read.
Overlay exists only in `envelope/target-kinds` (48). Container kinds:
`:document :text-block :facet-master :chat-conversation :chat-message
:tool-call :tool-result`. Relation kinds are a closed list (relation_kernel.clj:
62-88). Turn cell fields (episode.clj:489-500); spawn argv (836-853); model
and effort from `fm:invocation`, defaults sonnet/low/thread+2
(invocation_material.cljc:23-29), printed not stored (server_jetty.clj:
634-657). The code import writes `:document` containers and anchors with
offsets and block paths (clojure_adapter.clj:370-424).

**WebGPU on this box (memory, not re-proven).** CDP screenshots of a WebGPU
canvas came back black (2026-07-12); the verifier's texture readback works
headless with SwiftShader; headful on DISPLAY :0 with `--use-angle=vulkan
--enable-features=Vulkan,WebGPU,UnsafeWebGPU --ignore-gpu-blocklist
--no-sandbox` gives the real adapter (2026-08-09).

## 3. The layer as data (derived)

### The world

One atom, one value. Everything the screen shows is derived from it.

```clojure
{:records {id record}          ; every record by id: host, tools, strokes, vantage, events, identity, traces
 :runs    {tool-id {:result r :continuation c :inputs {root-key revision}}}
 :queue   [event ...]          ; undelivered events (they become records on delivery)
 :tick    n}
```

Every record carries `:id :kind :asserter :revision`. Revision increments on
replace; nothing mutates in place.

### Record kinds

- **host** (exists): `records/host`.
- **tool** (exists as `pickup`): `{:kind :tool :program :roots}`. The live
  brush is a second tool record, `pickup-live` (mine): the same program with the
  `:at` step replaced by `{:out :at :op :identity :args {:value [:get :event
  :point]}}` and `:fields [:point]`. That is the first tool edit of the build,
  and it is data.
- **painting** (lifted out of pickup's roots): the 64x32 chart surface, its own
  record so two tools can share it and the skin can rebake on its revision.
- **stroke**: `{:kind :stroke :events [{:id "dab-n" :point [x y z]} ...]}`.
  Grows on pointer moves with a button down.
- **vantage** (session 3's name; Sid's words: where to stand, bearings):

```clojure
{:kind :vantage :id "stage" :asserter "sid" :from nil
 :subject {:records ["G" "painting-1" "stroke-1" "pickup-live" "flow-*" "log-1" "inspector-1"]}
 :camera  {:pivot [0 0 0] :distance d :yaw -22 :pitch 14 :lens 35}
 :tools   [{:tool "pickup-live"  :roots-from {:support "G" :painting "painting-1" :events "stroke-1" :tool "brush-tool-1" :coating "G"} :place {:x 0 :y 0 :w 0.62 :h 1}}
           {:tool "flow-run"     :roots-from {:trace :trace}       :place {:x 0 :y 0.84 :w 0.62 :h 0.16}}
           {:tool "inspector-1"  :roots-from {:identity :identity} :place {:x 0.62 :y 0 :w 0.38 :h 0.5}}
           {:tool "log-1"        :roots-from {:trace :trace}       :place {:x 0.62 :y 0.5 :w 0.38 :h 0.5}}]
 :pins []
 :style "style-1"}
```

  `:subject` is an explicit id list in this build; the query form arrives with
  the wire. `:place` is a viewport-fraction rect. `:roots-from` is the tool's
  inputs made explicit as data: the tick re-runs a tool exactly when a record
  it names changed revision, and a test asserts that exactness (the caching
  rule: inputs explicit first, a key that is the full input, no cache until a
  trace shows the need).
- **event**: `{:kind :event :event/kind :pointer|:key|:resize|:change :at-ms
  :screen [x y] :buttons n :phase :down|:move|:up :key "a" :record id}`.
  Events are records so a saved list replays the world.
- **identity** (one record, replaced each tick): `{:kind :identity :screen
  :tool :place-local :route :object|:background|:path|:text :record :binding
  :mark :point3 :chart :line :col :source :asserter :revision}`.
- **trace**: `{:kind :trace :tick n :at-ms t :events [ids] :changed #{ids}
  :runs [{:tool :mode :run|:resume :status :ms}] :present {:ms} :pointer
  {:ms}}`. A ring of the last N.
- **text** records for the inspector and the log, derived each tick by a kind
  rule in code (below the waist in this build): `identity -> lines`, `trace
  ring -> lines`. Making those rules tool programs is next-layer work.
- **style**: the tokens in section 5, as a record the vantage names.

### Systems

All pure except present. `place/loop.cljc` (mine) holds the pure ones and runs
on the JVM under test; `place/present.cljs` holds the GPU.

```
tick [world events now] -> [world' trace]
  1 deliver  each event becomes a record; pointer+button -> stroke append
             (screen -> place-local -> ray-from-region-point/pick-region ->
             point3 -> object-local unit vector at R -> {:id :point});
             key -> edit of the focused text field; resize -> places recomputed;
             change -> record replaced, revision+1
  2 dirty    tools in the vantage whose :roots-from records changed revision
             since their last run, plus tools never run
  3 run      per dirty tool in vantage order: assemble roots from :roots-from;
             if a continuation exists, executor/resume with the new record;
             on :refused :recipe-differs fall back to executor/run
  4 point    the identity record from the last pointer position
  5 trace    the row for this tick
present [world] -> GPU: per open tool by kind -> renderer; scene target ->
             draw-present! to the canvas view; readback on demand
```

The clock is `requestAnimationFrame`, scheduled only when the queue is
non-empty or a record changed. The deliverer is step 1. The pointer is step 4
plus the reverse locators in present (highlights). The wire is C7.

Resume for a live stroke needs a continuation, and a completed run has none.
Version one of C1 runs the brush whole per tick and the trace shows the cost
per tick against stroke length. Version two lands a `:hold-at-end` option in
the executor (suspend with reason `:until` when items run out, so a later
resume with appended items continues), with a test beside executor_test.clj:58.
That is the one executor change foreseen; it lands when the numbers ask for
it.

### The waist in this build

| edited from inside (records)              | viewed only (code)                          |
|-------------------------------------------|---------------------------------------------|
| tool programs and roots (brush, flows, text tools) | executor, surface, coating, renderers, compositor |
| vantage: subject, places, camera, pins    | the tick, the deliverer, the pointer join   |
| style tokens                              | kind rules: identity -> lines, trace -> lines, trace -> flow samples |
| strokes, events, traces                   | present, readback, the drive script         |

The right-hand column's last row is the line that should move down next.

## 4. Components (checkpoints Sid sees)

| # | component | what it introduces | checkpoint receipt | test | size |
|---|-----------|--------------------|--------------------|------|------|
| C0 | the page and the present | build `:place`, `resources/public/place.html` with one canvas, `:devtools {:http-root "resources/public" :http-port 9500}`; boot = device, one font, compositor, canvas context configured; one frame: the sphere region plus the pickup painting as a flat image beside it; resize; `window.__place.readback()` | the sphere and its chart on screen at `localhost:9500/place.html`; `target/place/c0.png` | drive script: boot, readback not black | 0.5 d |
| C1 | the tick and the deliverer | `loop.cljc` `tick`; the world atom; pointer/key/resize events; rAF on dirty; stroke record; `pickup-live`; resume-else-run; trace ring | paint by hand into the chart; `c1.png` after a scripted 40-move stroke; ms per tick across the stroke | JVM: events -> stroke -> brush history rows = dabs; dirty exactness; trace shape | 1 d |
| C2 | the skin (mine) | the composed coating baked to a texture by `surface/sample` over a grid of unit vectors via `point->chart`; the sphere material samples it by normal (one region3d shader change or a `:skin` material); rebake on painting revision | strokes appear on the sphere under the pointer; `c2.png` | JVM: an 8x4 bake matches `surface/sample` at those points | 1 d |
| C3 | the pointer and the inspector | identity per tick (sphere: pick -> unit vector -> `coating/locate` rows -> the inside or nearest binding; background -> host; text: `hit-test-result` -> line -> source record); a `:text/layout` capability closing over the font provider; the inspector as a text tool through the executor; the hovered mark brightens | hover a mark: binding, record, mark, tool, revision in the inspector; hover the inspector: which line, which field, from which record; `c3.png` | JVM: identity from a synthetic pick; text tool through the executor with the layout tests' provider | 1 d |
| C4 | three tools, one subject | trace ring -> four path records (event, run, present, pointer lanes), width from ms through the existing `[:* [:get :size] [:get :p]]` rule, x by tick; the log as a text tool; pointing at a lane x or a log line returns the tick, its runs, the records touched | paint; the flow grows; point at a thick spot: the run that took the time; `c4.png` | JVM: flow samples from a fixed trace ring; classify on the flow | 0.5 d |
| C5 | where you stand | vantage records; `stage` and `flow`; switch by key or pointed control writes a new vantage with `:from`; the chain drawn as a small path; return restores camera and places | switch, return, the trail visible; `c5-stage.png`, `c5-flow.png` | JVM: switch/return as a pure world transform | 0.5 d |
| C6 | change a tool from where you stand | point at a value line in the inspector, type digits/backspace/enter/escape: a change event replaces the record, revision+1, dependents re-run; caret kept as a record field across re-layout; two edits: the brush `:radius`, the flow width rule | wider strokes; the lanes redraw; the commit diff touches no engine file to make either take hold; ms per keystroke in the trace; `c6.png` | JVM: edit -> dirty -> exact re-run; caret survives re-layout | 1 d |
| C7 | the wire (approval) | a place-records store keyed by id (kind, EDN value, revision, asserter, at) and two POST EDN routes `place/read` `place/write` in `wrap-file-api`; hydrate the world at boot; write on change; reload returns to the last vantage | reload -> the same place, strokes and edits back; `c7.png` before/after | JVM: route round trip on the in-process cluster; the loop hydrates from the read | 1-2 d |

Order: the loop is the unknown, so it comes first over the green tool; the
skin comes right after because the felt 3D is the strokes on the sphere; the
falsifier (C6) comes before the wire because the hypothesis is about the loop
and the wire is where the server needs a word.

## 5. The screen (design brief)

One canvas, the whole viewport, device-pixel aware, painted by the compositor;
no DOM beyond the canvas. Places come from the vantage, never from CSS.

- **Ground**: near-black cool `#0b0b0d`. Ink `#ece7dd`.
- **Stage** (left 62%): the sphere filling about 55% of the stage height,
  yaw -22, pitch 14, lens 35; base color warm grey `#8a847a`, roughness 0.85,
  metallic 0; one warm key light upper-left, one cool fill right, low ambient.
  The coating's regions in their own colors; the live brush carries coral
  `#ff6b4a` at 0.6 so strokes read on grey.
- **Flow lane** (bottom 16% of the stage): four lanes, event slate `#7b8fa6`,
  run amber `#e0b04a`, present teal `#4fb3a0`, pointer rose `#d76a8c`; width
  is time (ms mapped to pressure 0.15..1.0, size 10); a hairline baseline at
  25% ink; the last N ticks, N = lane width / 6 px.
- **Right column** (38%): inspector above, log below; `dejavu-sans-mono` 13 px
  at DPR, line height 18, padding 24; keys at 55% ink, values full, the
  hovered identity's record id in amber. Empty states: "nothing under the
  pointer", "no ticks yet", at 45% ink.
- **Highlight**: the hovered mark's texels brighten 20% on the next bake; the
  hovered tick's column brightens; the hovered text line gets a 1 px amber
  underline. Instant, no easing.
- **Motion**: only the flow growing and strokes appearing.

These are the `style-1` record's fields. The builder tunes values; the tokens
stay records.

## 6. Tests and receipts

- Pure loop tests on the JVM: `test/app/client/place/loop_test.clj` and
  siblings, added to the pure lane list (test_runner.clj:31-70). Every system
  in `loop.cljc` has a test beside it in the same commit.
- Screen receipts: `test/place/drive.mjs`, the verifier's launch flags and
  synthetic-origin pattern (run_verifier.mjs:23-28, 69-135) serving
  `/place.html`, `/js/place/*`, `/fonts/*`; drives `page.mouse`; calls
  `window.__place.readback()` and `window.__place.trace()`; writes
  `target/place/<c>.png` and `<c>.trace.edn`.
- `CHECKPOINTS.md` beside this file, appended per component: what it is, the
  receipt paths, the numbers that mattered, what moved in this ladder and why,
  what the builder filled in with its own judgment.
- One commit per component, on the worktree branch, explicit paths only.
  README, docstrings and code in the same commit.

## 7. Approval points

- **C7, before touching the server.** The store has no record read route, and
  its edit path forces kind and visibility. Position: a separate place-records
  store and two routes, kept apart from object-container; the kind fix at
  object_container.clj:1493-1497 is a second, separate ask.
- **Tell, do not wait**: the `:hold-at-end` executor option (C1 v2, with a
  test); the region3d material change for the skin (C2).

## 8. The next layer (named, not built)

- **Layers**: a working layer per asserter over the shared base; promote to a
  revision; every tool reads through the composition. Nothing exists under it
  today (branch written never read; overlay declared never built).
- **The agent's turn as data**: the turn cell grows model, effort and the
  rendered prompt; the prompt rendered from a vantage's records by a tool
  record; spawn from the row (the SSE route exists); the reply lands as records
  in the agent's layer at the thing it was about.
- **Typing at speed**: keystroke-grain records through the text tool, ms per
  key in the trace; the grain of Sid's turn decided by the numbers and his
  hand.
- **Code as subject**: the clojure import's units and anchors drawn by the
  text tool; trace rows anchored to the capability that ran; the flow drawn
  over the code map. That is the architecture flow over the real code.
- **Kind rules become tool programs**: identity -> lines, trace -> lines,
  trace -> flow samples move from code to records. The waist moves down.

## 9. Names and grades

| name here | whose | Sid's words |
|-----------|-------|-------------|
| the place | mine | the client, the ECS layer |
| vantage | session 3 | what to stand in front of, bearings |
| identity | sessions 1 and 3 | what it is, where it is, who made it |
| trace, the flow | mine | the flow of the architecture, live |
| the skin | mine | the strokes on the sphere |
| pickup-live | mine | the brush under my hand |

Checked: every file:line in section 2 (two hunters, this session). Derived:
sections 3 to 5. Assumed: the host's chart math is cheap enough to bake per
tick at 128x64 (fallback: bake smaller, rebake only on painting revision);
`pick-region`'s `:maintained` can be supplied from `scene/evaluate-scene` for
the place's region as the harness does; the executor passes opts through as
ctx (the text capability closes over the provider instead, so nothing rests on
it).
