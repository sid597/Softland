---
name: implementation-quirks
description: Coding gotchas, tool workarounds, and anti-patterns learned through implementation pain
type: reference
---

## Rama 1.6.0 — foreign-proxy-async Serialization Issues

### proxy-callback must return nil
`proxy-callback` wraps a Missionary notifier `!` for use as a Rama `foreign-proxy-async` callback. The callback's return value is serialized by Rama's wire protocol (Nippy/Worp). If the return value is opaque (e.g., the Missionary notifier's internal result), serialization fails with `Serializer not defined`.

**Fix**: Always return `nil` from proxy callbacks:
```clojure
(defn proxy-callback [emit]
  (fn [new-val _diff _old-val]
    (emit new-val)
    nil))
```

### Root-path [] on global PStates returns RocksDBWrapper
`(foreign-proxy-async [] pstate)` on a global `{Keyword Object}` PState causes the callback result envelope to contain a `RocksDBWrapper` — Rama's internal RocksDB backing store. This is NOT serializable and crashes with `Serializer not defined for type class rpl.rama.durable.RocksDBWrapper`.

Per-key `foreign-select` works fine: `(foreign-select [(keypath :field)] pstate)`.

**Workaround (current)**: Server-side atom updated by application code, bridged to Electric via `e/watch`.

**Possible fix (untested)**: Per-key subscriptions with `(keypath :field)` path instead of `[]`, combined with `m/latest`.

### defonce for Rama IPC references
The `ipc` binding in `util_fns.cljc` uses `def` (not `defonce`), so namespace reload creates a new IPC. All `foreign-pstate`/`foreign-depot` refs are also `def`. This means hot-reload works but loses Rama state. For sidebar truth, `!sidebar-truth-atom` uses `defonce` to survive reloads and initializes from `get-sidebar-state` at boot.

## Render Pacing — Unconditional RAF Until Electric Diffs

### Canvas resize must be atomic with draw
Setting `canvas.width` or `canvas.height` destroys the WebGPU swap chain texture. The next `getCurrentTexture()` returns blank. Canvas resize and draw MUST happen in the same RAF callback. Never set canvas dimensions in the resize consumer — do it in the draw function, using the same viewport snapshot that drives layout.

### Dirty-present is a gap-3 feature, not a gap-0 optimization
The S38 refactoring plan sequences three gaps: (1) commitment boundary, (2) atoms → PStates, (3) snapshot → differential. Dirty-present (conditional RAF triggered by change notifications) belongs to gap 3 — it requires Electric's structural diffs (`e/diff-by`, `e/for-by`) as the change signal. Without Electric diffs, you must manufacture change notifications yourself (atom watches, side effects in `m/latest`), which creates fragile coupling or violates Missionary's continuous flow contract.

**Current correct pattern:** unconditional RAF loop (`make-raf-flow`) + `identical?` skip at consumer level. Measured at 98.5% idle skip rate, 0-6% idle CPU.

### Don't deref live atoms inside the render reducer to "fix" stale snapshots
If the world snapshot is stale during rapid input changes, the fix is NOT to bypass it by derefing atoms directly. That creates a timing split: canvas dimensions from live state, content layout from stale snapshot → visible jitter. Instead, ensure the snapshot pipeline always settles (unconditional RAF guarantees sampling).

## WebGPU WGSL — Slug Shader Gotchas

### Integer inter-stage variables need @interpolate(flat)
WGSL requires `@interpolate(flat)` on any integer type (`u32`, `i32`, `vec4<u32>`) passed between vertex and fragment shaders. Without it, the shader module fails to compile with "no matching overload" or similar. Float types interpolate by default; integers cannot be interpolated.

```wgsl
// ❌ BAD — will not compile
@location(3) glyph: vec4<u32>,

// ✅ GOOD
@location(3) @interpolate(flat) glyph: vec4<u32>,
```

Both the vertex output struct AND the fragment input must have the annotation.

### Bit shift operators require u32 shift amount
WGSL's `<<` and `>>` operators require the right operand to be `u32`, even when shifting an `i32` value. If a constant is `u32` (like `const kFoo: u32 = 12u`), use it directly — don't cast to `i32`.

```wgsl
// ❌ BAD — i32 shift amount
let width = 1i << i32(kLogBandTextureWidth);

// ✅ GOOD — u32 shift amount used directly
let width = 1i << kLogBandTextureWidth;
```

## Electric 3 — e/input vs e/watch

- `e/input`: consumes a Missionary continuous flow (signal). Use for `m/signal`-wrapped flows.
- `e/watch`: watches a Clojure atom/ref. Use for plain atoms.
- Both work in `e/server` blocks and transfer values to `e/client` automatically.
- The `Tap` pattern in the Electric codebase shows the canonical `e/input` + `m/signal` + `m/observe` bridge.

## Domain-Identity Keywords

`(keyword prefix path)` creates keywords with namespace = prefix, name = raw path:
```clojure
(keyword "e" "/home/sid/foo.cljs") → :e//home/sid/foo.cljs
(namespace *1) → "e"
(name *1) → "/home/sid/foo.cljs"
```

Sub-elements preserve namespace: `(keyword (namespace parent) (str (name parent) "_hl"))`.

## Sidebar Slice — Structural Hashes and Semantic Truth

### Only hash geometry-affecting UI keys
When caching a shared resolved scene like `!sidebar-scene`, the structural hash must include only inputs that change visible geometry/text layout. For the sidebar this ended up being `:scroll-y`, `:dir-cache`, and `:home-dirs`.

If `:hover-id` or in-flight request bookkeeping (`:in-flight-dirs`, `:in-flight-files`) leaks into the structural hash, hover and dedupe updates will force unnecessary scene resets and `text-same?` misses.

### Separate semantic selection from loaded file content
`selected-file` (committed truth + optimistic overlay) is semantic sidebar state. `!current-file` is the I/O consequence of reading the file into the editor.

Do not drive active sidebar highlighting from `!current-file` or selection will lag behind click intent and stale file fetches can overwrite the visible selection story.

### File-read failure must clear committed sidebar truth too
If a file click emits `:sidebar/file-select` before `/api/read-file` succeeds, rolling back only local atoms is not enough. Remote sidebar truth will re-apply the unreadable file on reconnect/reload.

On read failure (including "file too large"), clear:
- local `!selected-artifact`
- local `!current-file`
- committed sidebar truth via `emit-sidebar-action! :sidebar/file-select {:path nil :name nil}`

### `:sidebar/file-select` round-trip log is not file-open latency
The `:sidebar/file-select` HTTP round-trip log measures the Rama sidebar commit/ack path, not the `/api/read-file` latency. Large values there do not mean the editor was slow to load file contents.

If file-open latency matters, add a dedicated read-path timing log inside `fetch-file!`.

## Documentation Provenance — Preserve Origin Prompts For Systems Questions

When a systems-level question creates a durable distinction or architecture
note, preserve the user prompt/question that caused the note to exist.

Use this especially for recurring Rama kernel questions like:

- logical lifecycle vs physical depot layout
- source-of-truth depots vs derived depots
- fine-grained addressability vs per-unit materialization
- policy scope vs policy override

Until Rama trails preserve this automatically, docs should include a lightweight
origin block:

```text
Origin prompt:
  <the user question that forced this distinction>

Why this matters:
  <why this question is likely to recur>
```

This prevents docs from becoming contextless assertions. The point is not to
quote every chat turn; it is to keep the pressure that made the distinction
necessary.

### Rama rename vocabulary after `1ef1cbd`
Active code after the May 13 rename/split uses:

- shared contracts: `src/app/server/rama/core.clj`
- text instance: `src/app/server/rama/text_kernel.clj`
- dogfood space runtime: `src/app/server/rama/dogfood/space.clj`
- dogfood space tests: `test/app/server/rama/dogfood_space_test.clj`
- text kernel tests: `test/app/server/rama/text_kernel_test.clj`

Older docs may say world-kernel, world-thread, or world-turn. Preserve that
when the doc is historical provenance, but use space/turn vocabulary for active
code, new handoffs, and new tests.

## Faces-as-assemblies framework (2026-07-11 close)

Coding gotchas surfaced by the framework package (Waves 1+2). Each cites its
artifact; source is `src/app/client/workspace/face_*` + `src/app/server/rama/
face_arsenal.clj` + `.../object_container/assembly_adapter.clj`.

### A `delay` caches a THROWN exception and re-throws on every deref
Clojure's `delay`/`Delay` memoizes its result **including a thrown exception**
— once the body throws, every subsequent `@deref` re-throws the cached
exception. A lazily-booted runtime handle behind a `delay` that feeds the
render path is therefore a one-shot bomb: a single boot failure becomes a
**permanent** failure for every client that derefs it (the Electric render
path has no `try` upstream — L13). Rule: wrap the boot body in try/catch and
return a **poisoned-but-TOTAL** value on failure (nil handles + an honest
`failed?` flag → error data-contexts), never let the throw reach the deref.
Grounds: framework W2-F7 (`file_viewer.cljc` face-projection-runtime delay);
`W2-GATE.md`.

### Headless WebGPU capture: bundled Chromium has none; Vulkan swapchain won't composite into CDP
The repo's bundled puppeteer Chromium has **no WebGPU** — a WebGPU driver must
use **system Chrome** (v150 worked) with `--enable-unsafe-webgpu
--enable-features=Vulkan`. Even then, the Vulkan swapchain does **not**
composite into CDP/X screenshots on this box (tried headful+xvfb, headless=new,
swiftshader — all blank). GPU visual capture is an environment lack, not a
render failure. Fall back to a **labeled CPU rasterization of the live
`!face-scene` tree** (the exact rects + text-ops the GPU consumes). When
rasterizing: **rect ops carry FLAT `r g b a` keys; text ops nest per-node** —
a harness that assumes one op-shape yields a blank PNG while the render is
fine. The op COUNTS always match the scene; assert op-count parity and treat a
blank frame as a harness mapping bug first. Grounds: `W1-INT.md` env-lack #5,
`W2-INT.md` run record.

### `imp:asm:` two-segment object-keys break `leading-object-key` routing
An assembly's object-key is `asm:<name>` — it carries an internal colon — so
the import key `imp:asm:asm:<name>:<sha>` (the literal `markdown-import-key`
shape) cannot route via a literal `leading-object-key` mirror of the
`imp:clj:` branch: `leading-object-key` truncates `asm:<name>` at `"asm"`. The
`extract-object-key` branch must take the **first two colon segments** of the
remainder (`object_container.clj:307-321`, additive). Corollary — the
colonless-object-key precondition the older `src:/rev:/evt:/du:/sa:/ce:`
id-family read APIs rely on means **those APIs MISROUTE on foreign reads of
`asm:` keys**; product reads use only routing-correct surfaces
(`read-import-completion`, `oc:doc:asm:<name>` + `read-current-revision`,
`$$source-latest-by-ref` re-hashed to the colonless ref-key). Test-only
receipts read misrouting-shaped keys with `{:pkey object-key}` (the G12
carve-out precedent). Grounds: `W2-D-arsenal.md` judgment 2-3, INT flag 3.

### Identity-anchor container for STABLE-identity OC objects
OC's `native-claim-compatible?` requires an **unchanged container content-hash
across imports** (the kernel's exactness law). Markdown never trips it because
its object-keys are content-addressed (one object per save). An object whose
identity is **stable by design** (an assembly keyed on its NAME, §17) trips
`:native-identity/conflict` on every EDITED save. Resolve **within the seam**
(no kernel edit): materialize the document container as an **IDENTITY ANCHOR**
— `current-content-text`/`current-content-hash` nil, the bytes riding the
revision chain (`current-revision-id` flips per save; `read-current-revision`
serves them). A head pointer + revisions, no content copy, claims-compatible
forever. Grounds: `W2-D-arsenal.md` judgment 4 (the lane's one real design
finding, resolved without a stop-clause).

### One `.cljc` compiler, two call sites = verdict parity by construction
When a server ingest-validator and a client renderer must agree on whether a
value is valid, call the **same** `.cljc` fn over the **same** registry from
both sides (server `assembly_adapter`/`face_projection` and client
`face_wiring` both call `face-assembly/compile-assembly` ×
`face-primitives/registry`). Verdicts are then equal by construction; drift is
impossible while both exist. The G7 source-form diff test pins the copied
builders inside the shared namespace so the "one namespace" premise holds.
Grounds: framework T18; `W2-D-arsenal.md`.
