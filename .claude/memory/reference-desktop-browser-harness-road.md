---
name: reference-desktop-browser-harness-road
description: The proven road for driving the Softland app in a real browser on the dev box (WebGPU runtime receipts) — and the two roads that fail
metadata: 
  node_type: memory
  type: reference
  originSessionId: 055560d9-d301-48f9-b5e4-bbaab1fc1276
  modified: 2026-08-09T10:47:45.545Z
---

Driving the real app for runtime receipts on the dev box (proven 2026-08-09):
Puppeteer (repo's v15) + system Chrome, **headful on DISPLAY :0** with
`--use-angle=vulkan --enable-features=Vulkan,WebGPU,UnsafeWebGPU
--ignore-gpu-blocklist --no-sandbox` → real Radeon RX 7900 XTX adapter.
Server: `clj -M:dev -m dev` (shadow watch + Jetty 8080, Rama must be up).
URL `?live-atoms=1&region3d=1&seam-demo=1` = real space (n≈261) + region
fixture + labeled connectors. Gesture contract lives in
`test/render_engine/profile_shaping_correction.mjs` (pan = press empty ground
+ ≥2 moves; zoom = wheel, 1.0015^-dy; `__ground` hooks for edit/selection).

FAILING roads (don't redo): headless SwiftShader → mid-run DEVICE-LOST +
canvas stuck 300×150 (input misses); headless + vulkan ANGLE → WebGPU never
initializes. Dev-build runtime patching works (`app.client...` namespace
objects, `:none` optimizations) but single-arity fns have NO
`cljs$core$IFn$_invoke$arity$N` property — wrap with `.call(null, …)`.

Repo-visible twin (2026-08-10, for the Codex lane / any session without this
memory): `docs/reference/desktop-browser-harness-road.md` — adversarially
reviewed; its launch command is marked inferred-not-proven, so THIS memory
holds the proven invocation. If the repo doc and this memory diverge, this
memory wins until a session re-proves the road.

**Measured 2026-08-28 (twice):** the full render-engine chain — `npm run verify:render-engine` = text-layout suite → instance fence → shadow release build → Chrome verifier — runs in **~27 s wall**; the verifier alone reports ~28 s. The runner's 600 s `waitForFunction` is a ceiling, not a duration; any "minutes" estimate for a verifier run is wrong. `clj -X:test` (full JVM runner) is red on an unclassified tools test since `b86a5d2`; the focused client suite is `clj -M:test -i <script requiring the 12 app.client.*-test namespaces>` → 48 tests / 419 assertions in ~2 s.
