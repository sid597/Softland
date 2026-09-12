# Softland Editor — Performance Optimisation Report

## Session: Large File CPU Investigation

---

## Problem Statement

When opening a large file (e.g. `package-lock.json`, 5420 lines) in the Softland WebGPU editor, **one CPU core pins at 90–100%** and the `requestAnimationFrame` rate drops from ~400/3s (133fps) to **4–6/3s (~2fps)**. The editor becomes unresponsive even though the user is idle (no typing, scrolling, or interaction).

This does NOT happen with small files (<500 lines) — those run at full frame rate with 0–6% idle CPU.

---

## Investigation Timeline

### Phase 1: Identify which JavaScript code is hot

**Added:** Debug counter system (`!debug-counters`) — batched 3-second logging of RAF ticks, flow recomputation counts, GPU upload counts.

**Finding:** After the initial file load, all JavaScript counters are near-zero when idle:
- `text-ops=0` — `<combined-text-ops` NOT recomputing
- `gpu-upload=0` — no GPU buffer re-uploads
- `editor-rects=2` — only blink timer (expected)
- `frame-skip=2` — most frames skipped via `identical?` fast path

**Conclusion:** Our JavaScript render loop is barely running. The CPU spike is NOT from our code.

### Phase 2: Identify which atoms are being modified

**Added:** `!atom-change-counters` with `add-watch` on all 7 atoms feeding `<combined-text-ops`:
`!editor-doc`, `!cmd-panel`, `!scroll-y`, `!viewport`, `!folded-lines`, `!settings`, `!active-font`

**Finding:** Zero atom changes when idle. No atom is being modified in a loop.

**Conclusion:** The reactive flow graph is stable. The CPU spike comes from outside our JavaScript.

### Phase 3: Identify the OS process

**Ran:** `top` and `htop`

**Finding:** **Chrome renderer process** (PID 583385) at **100% CPU**, using 708MB RES. No Java/JVM process in sight. Our JavaScript is barely running, but Chrome itself is saturated.

### Phase 4: Hypothesis — GPU buffer too large

**Theory:** `update-text-data` was creating a GPU instance buffer with ALL glyph instances for ALL 5420 lines (~200K+ glyphs, ~10MB Float32Array). Chrome's WebGPU-to-Vulkan layer on Linux might be doing CPU-side validation on the full buffer every frame.

**Added:** `[GPU-BUFFER]` logging in `update-text-data` to measure instance count.

**Fix attempted:** Viewport culling at the data level in `<combined-text-ops` — `filterv` render-ops to only include lines within the visible viewport (+2 line buffer). Simplified the `draw-frame!` editor text section to draw all instances (since buffer is pre-culled).

**Result:** Buffer dropped from **20,231 instances / 971KB** to **1,779 instances / 85KB** — a 91% reduction. But **CPU still at 90–100%**, RAF still at 4–6/3s.

**Conclusion:** GPU buffer size is NOT the root cause.

### Phase 5: Profile the text pipeline

**Added:** `performance.now()` timing around the three stages in `<combined-text-ops`:
1. `tokenize` — `(mapv tokenize-fn lines)` — Lezer parse per line
2. `folds` — `(detect-folds-fn lines ...)` — full-document Lezer re-parse
3. `layout` — `(layout-fn tokenized ...)` — iterate all lines with fold checks

**Finding (5420 lines):**
```
tokenize:  1.5 ms     (already optimised to visible-only)
folds:   245.5 ms     ← Full Lezer re-parse of entire 5420-line file!
layout: 1167.5 ms     ← Iterates ALL 5420 lines with per-line fold checks
TOTAL:  1414.5 ms     ← 1.4 SECONDS blocking main thread
```

For comparison, 508-line file: `tokenize: 1.3ms, folds: 12.1ms, layout: 23.1ms, TOTAL: 36.5ms`

**Conclusion:** The initial `<combined-text-ops` call blocks the main thread for **1.4 seconds**. This causes 16+ RAF violations. After this, Chrome's RAF scheduler drops to ~2fps and **never recovers** for the lifetime of the tab.

### Phase 6: Fast path for large files

**Fix attempted:** For files >500 lines, skip fold detection (full Lezer re-parse) and only layout visible lines (~50) with adjusted Y offset. Identity line-mapping (no folds).

**Result:** `[PERF]` log no longer appears (below 10ms threshold), confirming tokenize + layout is now fast. But **CPU still at 100% on one core**, RAF still at 4–6/3s.

**Conclusion:** The optimisations successfully reduced text pipeline cost from 1414ms to <10ms, but Chrome remains in a degraded state.

---

## Current State

### What IS working (optimisations that landed)

| Optimisation | Before | After | Reduction |
|---|---|---|---|
| Idle frame skipping (`identical?` on world) | 45–76% idle CPU (small files) | 0–6% idle CPU | ~95% |
| Viewport-scoped tokenization | 5420 Lezer parses | ~50 Lezer parses | 99% |
| Viewport-scoped layout (large files) | 5420 iterations + fold checks | ~50 iterations | 99% |
| Fold detection skip (large files) | 245ms full-document Lezer re-parse | 0ms (skipped) | 100% |
| GPU buffer viewport culling | ~200K instances / 10MB | ~2K instances / 110KB | 99% |
| Draw call simplification | Per-draw viewport culling math | Draw all (pre-culled) | Simpler |
| `text-ops` pipeline total | 1414ms | <10ms | 99.3% |

### What is NOT working

**The core problem persists:** After loading a 5420-line file, Chrome's renderer process pins one CPU core at 100% and RAF drops to ~2fps. This happens DESPITE all our JavaScript being near-idle.

### Evidence that the problem is in Chrome, not our code

1. `text-ops=0`, `gpu-upload=0`, `atom-changes=0` when idle
2. `raf=4-6` per 3s — Chrome can barely fire requestAnimationFrame
3. `htop` shows Chrome process at 100%, not Java/JVM
4. Small files (508 lines) work perfectly with the same code paths
5. All optimisations reduced JS work to <10ms but Chrome CPU is unchanged

---

## ✅ ROOT CAUSE FOUND — Session 17 (Chrome Trace Analysis)

### The Kill Chain

Every **530ms**, the blink timer fires → `!caret-visible` changes → Missionary `m/latest` propagates through `<editor-rects` → calls `compute-editor-rects` → which performs **full-document operations** on ALL 5420 lines:

1. **`detect-fold-regions`** (`electric_flow.cljc:220`):
   - `(str/join "\n" lines)` — concatenates all 5420 lines into ~200KB string
   - `.parse @lezer-parser full-text` — **full Lezer re-parse** of entire document
   - Tree walk calling `offset->line-col` per node (linear scan of line-lengths each time)

2. **`find-matching-bracket`** (`electric_flow.cljc:181`):
   - `(str/join "\n" lines)` — creates ANOTHER ~200KB string
   - `.parse @lezer-parser full-text` — **second full Lezer re-parse**
   - Another tree walk with `offset->line-col`

3. **`line-mapping` loop** (`loop.cljs:678-688`):
   - Iterates ALL 5420 lines
   - Each iteration calls `(some (fn [...] (and (contains? folded start-line) ...)) regions)` — O(n × m)

**Total: ~1440ms blocking the main thread** — exactly matching `[LONGTASK] 1442.0 ms self`

### Why the Debug Counters Were Misleading

| Metric | Value | Why Misleading |
|---|---|---|
| `text-ops=0` | `<combined-text-ops` idle | ✅ Correct — the optimised function IS idle |
| `editor-rects=2` | Fires only twice/3s | Each call takes **1440ms**, not logged by counter |
| `[FRAME] avg=0.4ms` | RAF callback fast | Expensive work is in **Missionary reactor propagation**, outside RAF |
| `gc=0` | No GC | Pure JS computation, not GC |

The 1440ms long task is the Missionary `m/latest` propagation (synchronous on main thread), not the RAF callback. RAF only reads the pre-computed result.

### Chrome Trace Evidence (75s recording, 354K events)

- **36 "mega-frames"** of ~1.5s each, running back-to-back with 2–8ms gaps
- **CPU profiler top functions**: `cljs.core.some` (3.6%), `contains?` (3.3%), `detect_fold_regions` (3.2%), `offset->line-col` (1.1%), `find_matching_bracket` — all called from `compute-editor-rects`
- **97.3% of RAF callbacks take <1ms** (fast path working), but the main thread is blocked between RAFs
- **GC is negligible** (1.3s out of 75s trace) — this is pure computation

### The Fix

`compute-editor-rects` recomputes **document-level structures** (fold regions, line mapping, bracket matching) on every **caret blink**. These should only recompute when the **document changes**.

1. **Extract fold regions + line mapping into a cached `m/latest` flow** watching only `!editor-doc` + `!folded-lines`
2. **Cache bracket matching** — only recompute when cursor position or document changes
3. **For large files (>500 lines), skip `detect-folds-fn`** in `compute-editor-rects` (same fast path as `<combined-text-ops`)

---

## Files Modified

> Current path note (post-refactor): the old `src/app/client/webgpu/loop.cljs` work now lives primarily across `src/app/client/workspace/runtime.cljs`, `editor_compute.cljs`, and `combined_text.cljs`. The old `src/app/client/webgpu/editor.cljs` is now `src/app/client/substrate/webgpu/renderer.cljs`.

| File | Changes |
|---|---|
| `src/app/client/workspace/runtime.cljs` + related workspace modules | Debug counters, atom change watchers, viewport-scoped tokenization, large-file fast path, performance timing |
| `src/app/client/substrate/webgpu/renderer.cljs` | GPU buffer size logging, simplified draw call (pre-culled data) |

## Debug Infrastructure Added

- `!debug-counters` — 3-second batched logging of RAF/flow/GPU tick counts
- `!atom-change-counters` — tracks which atoms are being modified and how often
- `[PERF]` timing — tokenize/folds/layout millisecond breakdown
- `[GPU-BUFFER]` logging — instance count and byte size on every upload
- `[ATOM-CHANGES]` logging — which atoms changed in each 3-second window
- `[LONGTASK]` — PerformanceObserver for long tasks (>50ms)
- `[PERF-OBS]` — 3-second summary of long task count/max and GC count/max
