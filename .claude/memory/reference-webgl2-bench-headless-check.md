---
name: reference-webgl2-bench-headless-check
description: "The proven one-command road for checking a single-file WebGL2 bench artifact headless (screenshot + panel readouts) — distinct from the WebGPU app, where headless SwiftShader fails"
metadata:
  node_type: memory
  type: reference
  originSessionId: 83c413da-2c52-4bc9-8595-ee27894bdcac
  modified: 2026-09-05T19:52:00.583Z
---

For a **WebGL2** single-file bench (the path-kind benches under `history/docs/below-the-waist/path-kind/bench-*/`), headless Chrome with SwiftShader renders real pixels and the shader compiles like on a GPU (proven 2026-09-05 by session 8 on bench 4, 2026-09-06 by session 9 on bench 9):

```
google-chrome --headless=new --no-sandbox --use-angle=swiftshader --use-gl=angle \
  --enable-unsafe-swiftshader --hide-scrollbars --window-size=1500,1000 \
  --virtual-time-budget=4000 --screenshot=out.png "file://…/bench.html#tool=pen"
```

`--dump-dom` in place of `--screenshot` prints the rendered panel, so a bench that writes its counts into the DOM (curves, bands, cover pixels, ms) can be read without a browser. State via the URL hash makes several states one command.

This is the opposite of the WebGPU app: there headless SwiftShader device-loses and canvas pixels never reach screenshots ([[reference-desktop-browser-harness-road]], [[implementation-quirks]]). WebGL2 benches are the cheap visual receipt; keep them WebGL2 for that reason.

**How to apply:** one screenshot pass over the deep-linked states, one fix pass, publish; do not build a screenshot loop around the file (artifact-design law).

**Two traps seen on bench 2 (3D round, 2026-09-06).** (1) A GLSL identifier starting with `gl_` (e.g. `float gl_ = …`) fails the compile, and a bench that falls back on link failure shows "WebGL2 unavailable" as if the context were missing; print `getShaderInfoLog` into a DOM element (`#glstat`) so `--dump-dom` shows the real cause. (2) The one-look rule still holds, but when the first look shows a failure state rather than the page, take a second look after the fix; the first one verified nothing.

**Three traps seen on bench 9 (path round, session 10, 2026-09-06).** (1) Two sampler types (`sampler2D` and `usampler2D`) pointing at one texture unit is an `INVALID_OPERATION` at draw time and the draw silently does nothing — every sampler uniform, including ones for an optional feature (a clip, a backdrop), must be set to its own unit holding a texture of its type on every draw (a 1×1 dummy when unused); `gl.getError()` printed into `#msg` after the first render found it in one look. (2) `bindTexture` binds on the *active* unit: a `copyTexSubImage2D` setup that binds its scratch texture without `activeTexture` first knocks whatever sampler was on that unit off it. (3) `--dump-dom` panel text is nested HTML; extract it with `html.parser` tracking depth, not a regex to the first `</div>`; a deep-link `&at=x,y` that sets the bench's cursor makes a readout quotable headless.

**One trap seen on bench 9 (path round, session 11, 2026-09-06).** A fragment shader that takes its local position from the interpolated vertex varying inherits the rasterizer's sub-pixel snap of the cover triangle's fractional device corners (about 0.01 local at zoom 3): a clip edge through a pixel centre read .48 instead of .5 and the GPU disagreed with the CPU twin by up to 5.8e-3 across a surface, inside the cursor's "agree" band. Derive the local position from `gl_FragCoord` through the inverse of the draw's map instead; after that the two hosts agreed to 1.2e-7. The way it was found: pick one point where a single operation must give an exact answer (one clipped dab on the mask's edge) and read there, rather than trusting a tolerance band.
