# PROBE-10K — Electric at 10⁴: the pre-registered §9 measurement

**Status: EVIDENCE, measured 2026-07-05** (Fable, render-bench session).
Pre-registration: `NORTH.md §9` — *does Electric carry land-scale deltas?*
Judged pre-registered: **either outcome keeps the model**; this document is
numbers and method, not an architecture verdict. Companion docs: `NORTH.md`
(the framework), `MECHANICS.md` (frame pacing = H4 in `HARD-PROBLEMS.md`).

One-line answer up front, because the table earns it: **the knee is not
element count — it is diff shape.** Address-keyed change/append/tail-shrink
streams hold at 10⁴ with clean 60fps pacing and an O(1) idle frame; any diff
that carries a large `:permutation` (reorder, front-drop windowing, ID churn)
knees at 10³ and is unusable at 10⁴ — and the cost lives in the *diff
algebra* (minting, `->seq-differ`), upstream of both the wire and the
consumer. Order must not ride the incseq; everything else rides it well.

---

## 1 · What was measured (and that it is the real thing)

No imitation components. The probe drives:

- **Producer:** `hyperfiddle.incseq/->seq-differ` from the Electric jar in
  use (`electric-v3-alpha-20260325.114002-44`) — verified to be exactly what
  `e/diff-by` / `e/for-by` expand to (`electric3.cljc:159,243`). Measured on
  the JVM (its real home) and in-browser.
- **Protocol state:** `hyperfiddle.incseq/patch-vec` maintains the semantic
  vector in every diff consumer (its 5-phase transient apply is the
  executable spec of the six-op protocol).
- **Consumers over the real product pool** (`app.client.substrate.webgpu.
  buffer-pool`, read-only use):
  - **C2 `direct`** — the NORTH §2.4 store: `:grow`→`allocate-slot!`,
    `:permutation`→order-indirection vector only, `:shrink`→`free-slot!`
    (slot zeroed), `:change`→`update-slot!` (`writeBuffer` at slot offset).
  - **C1 `mount`** — the dormant as-built `gpu-mount` bridge driven by
    `hyperfiddle.incseq.mount-impl/mount` (the electric-dom contract).
  - **C3 `recollect`** — `keyed-diff-update-pool!` fed whole collections:
    the §9 fallback transport (windowed rows re-diffed client-side), behind
    the same store.
- **Instrument:** [RAF]-shaped per-frame phase timing (mirrors
  `runtime/render.cljs:484`): mint ms / apply ms per frame, RAF timestamps
  for pacing, diff op counts, JS heap; `queue.onSubmittedWorkDone` sampled
  every 30th frame. All frames recorded; 5ms/8ms thresholds applied at
  analysis.

**Synthetic material:** rect-grade drawables (28-float pack vocabulary,
stable string addresses, lane layout), deterministic Park-Miller LCG so JVM
and browser see identical streams. Scales n ∈ {10², 10³, 10⁴}. One diff
batch per RAF frame — a deliberate stress ceiling; real trail-view delta
rates (watcher epoch bumps, ingest) are ~0.1–10 Hz, so steady-state numbers
below carry ≥6× rate headroom.

**Mixes** (measured op counts per batch at n=10⁴):

| mix | meaning | grow/shrink/perm/change |
|---|---|---|
| cold-load | 0→n in one diff, then idle | frame 0: n grow; then 0/0/0/0 |
| change-1 | 1% of rows change value | 0/0/0/100 |
| change-10 | 10% change | 0/0/0/1000 |
| permute-rot | rotate by n/4 (re-sort; ids+values unchanged) | 0/0/**10000**/0 |
| churn-10 | window slide: drop 10% oldest, add 10% fresh ids | 1000/1000/**11000**/1000 |
| churn-100 | full ID churn every batch (trap 3) | 10000/10000/**20000**/10000 |
| grow-append | +10 fresh rows per batch | 10/0/0/10 |

**Environment:** Ryzen 9 9900X; Chrome 150 Linux. Browser runs: run 2 =
headless, WebGPU **SwiftShader** (headless cannot see the GPU here — three
flag combos tried); run 4 = windowed on DISPLAY=:0, WebGPU **AMD RDNA-3**
(hardware), full matrix. Hardware and SwiftShader agree within noise
everywhere (differences <30%, no verdict flips) — the measured costs are
CPU-bound, which makes the conclusions adapter-robust. Producer bench: JVM
21 (OpenJDK 21.0.11). Every non-corrupt cell passed verification
(`patch-vec` state == expected stream, slot count == entity count).

## 2 · Producer: minting the diff (JVM, `->seq-differ`, ms per update)

The floor for Electric's server-side `e/for-by`/`e/diff-by` work per
recompute (the Electric signal DAG adds overhead on top — unmeasured here):

| mix | 10² | 10³ | 10⁴ |
|---|---|---|---|
| cold-load (steady empty re-scan) | 0.05 | 0.16 | 2.0 |
| change-1 | 0.05 | 0.17 | 2.5 |
| change-10 | 0.04 | 0.32 | 4.8 |
| grow-append | 0.17 | 0.24 | est. ~2–5 (cell lost to run caps twice; linear-regime extrapolation — §7.5) |
| **permute-rot** | **0.62** | **115** | **73 800** (one step; capped) |
| **churn-10** | **0.37** | **47** | **19 400** (two steps; capped) |
| **churn-100** | — | **1 737** | **> 250 000** (one step did not finish in ~250s, two attempts; browser V8 minted it in 36.8s/38.4s — see §7.5) |

Two regimes, three orders of magnitude apart:

- **Linear regime** (no reorder): the differ re-scans all n and rebuilds its
  key index every update — ~0.2µs/row. At 10⁴ that is a 2–5ms per-update
  floor even for a 1-row change. Server-fine; it would already be
  frame-relevant if minted client-side (browser mint at 10⁴: 10–23ms).
- **Permutation regime**: cost explodes ~×130–×700 per decade (rotation
  composition per displaced element — quadratic-ish in displaced count).
  A reorder at 10³ costs ~0.1s; at 10⁴, tens of seconds. **Front-drop
  windowing is a reorder** (churn-10's shift permutation), which is why
  window-slide dies too while append-only stays linear.

## 3 · Consumer + frame pacing (browser, hardware run canonical)

Per-frame **apply** ms (p50/p95), pacing = RAF-delta p50, at n=10⁴:

| mix | C2 direct (diff) | C3 recollect (fallback) | pacing direct | pacing recollect |
|---|---|---|---|---|
| change-1 | 0.4 / 0.5 | 19.3 / 25.6 | 16.7 | 16.7–33 |
| change-10 | 2.9 / 4.2 | 23.3 / 28.3 | 16.7–33* | 33 |
| permute-rot (pre-minted) | 6.4 / 6.9 | 18.2 / 24.2 | 16.7 | 16.7–33 |
| churn-10 (pre-minted) | 9.8 / 10.9 | 29.6 / 35.4 | 16.7 | 33 |
| churn-100 (pre-minted) | 83 / 96 | — | 83–100 | — |
| grow-append | 0.1 / 0.2 | — | 16.7 | — |

\* direct/change-10 pacing wobble comes from the *client-side mint* sharing
the frame in live-mint cells, not from apply.

At 10³ everything is comfortable: direct ≤1.4ms worst mix (full churn 7.2),
recollect ~2–3.5ms. At 10²: all ≤0.4ms.

- **The diff dividend is ~50–90× at 10⁴**: direct applies only the delta
  (0.4ms for 100 changes) while recollect pays flat O(n) every frame
  (17–30ms — over the 8ms budget on 150/150 frames, pacing degraded to
  30fps). The §9 fallback is validated as a *shape* (same store, transport
  swapped) but it does not carry 10⁴ at 60fps; it carries ~10³ and it drops
  ordering semantics (unordered keyed slots).
- **A 10⁴-entry permutation applies in ~6.5ms** through the order-indirection
  vector — no GPU re-upload (trap 1's consumer half, answered by
  construction: order lives in the indirection, slots never move).
- **writeBuffer at slot offset ≈ 2–3µs/row end-to-end** (1000 changed rows
  ≈ 2.2–2.9ms apply incl. pack): per-call overhead, not bandwidth, is the
  cost; coalescing adjacent slots is the known consumer-side lever if this
  ever matters.
- **Idle frames are free**: cold-load cells run 149 empty-diff frames at
  0.0–0.1ms apply — the O(1) skip discipline survives the diff consumer.
- **Cold load 0→10⁴ = 27.8–37ms once** (includes pool-growth doublings from
  capacity 256 + 10⁴ allocations + writes). One visible hitch at first
  paint; pre-sizing the pool would shave the doublings.
- **GC dragon sighted, small**: heap sawtooths 25–90MB/cell in change mixes;
  occasional 30–50ms mint outliers line up with collections (H5's
  prediction; the projector edge is where persistent-map allocation lives).

## 4 · The bridge finding (as-built `gpu-mount`)

On its valid diet (change / append / tail-shrink) the dormant bridge matches
direct within noise at all scales (10⁴ change-10: 3.9ms p50). But the mount
contract passes existing *child handles* back through `insert-before` during
rotations (DOM `insertBefore` = move); `gpu-mount` allocates a fresh slot
from whatever it receives. **Any `:permutation` therefore corrupts the
pool**: the demo cell (n=100, rotate) ended 91 frames with **16 750 active
slots for 100 entities** and 160ms/frame apply (quadratic child-vec scans
over the junk). Protocol state stayed correct throughout (`rows-match?
true`) — the corruption is entirely in the bridge, not the diff algebra.
Consumer obligation recorded: the scene store consumes the six ops directly
(C2 shape); the DOM-shaped mount contract is not reusable for slot pools
whose "move" is an indirection update.

## 5 · Wire-payload proxy (pr-str bytes per diff, JVM)

change-1@10⁴ ≈ 14KB (100 real changes); change-10@10⁴ ≈ 137KB;
permute-rot@10⁴ ≈ **108KB carrying zero new values** (pure reorder);
churn-10@10⁴ ≈ 260KB. Electric's actual codec differs; ratios are the
signal: reorders ship a permutation map ~n entries whether or not anything
changed. At real trail-view rates (≤10Hz) none of these sizes threaten the
wire before the mint cost has already killed the reorder path.

## 6 · Where the knee is

| stream shape | 10² | 10³ | 10⁴ | binding stage |
|---|---|---|---|---|
| change / append / tail-shrink | ✓ | ✓ | ✓ (mint 2–5ms srv, apply ≤4.2ms, 60fps) | none — headroom ≥6× at real rates |
| cold load | ✓ | ✓ | ✓ (one ~30ms frame) | consumer, one-time |
| reorder / front-drop window | ✓ | ✗ mint ~0.1s | ✗✗ mint 12–74s | **producer (diff algebra)** |
| full ID churn | ✓ | ✗ mint 1.7s | ✗✗ mint ~37s; apply 83ms | producer, then consumer |
| fallback (re-diff client-side) | ✓ | ✓ (~2–3.5ms) | ✗ 17–30ms/frame, 30fps | consumer, flat O(n) |

Read against the pre-registration's two outcomes: **both, split by diff
shape.** For the delta shapes the trail view emits at the rates it emits
them (ingest appends, badge/value changes, settles/freezes), Gap 3's change
signal is real at 10⁴ on Electric's own protocol — conditional RAF has its
legal signal. For reorder-carrying shapes the failure is *upstream of
boundary 1*: swapping the transport (the pre-registered fallback) does not
fix it (recollect has its own 10⁴ knee), and it isn't a consumer problem
(6.5ms for a 10⁴ permutation). The design consequence is the one NORTH
already states for layout generally: **order/position is derived data on
rows** (a sort key the client applies at projection time, or server-side
windows that drop from the tail), never a sequence-position permutation
diffed through the incseq. With order out of the diff stream, nothing
measured here blocks the model at 10⁴; no rebuild candidate emerges. Model
kept — with two recorded obligations (order-as-data; C2-shaped consumer).

Pre-registered traps, disposition: (1) `:permutation` degradation —
CONFIRMED, but it fires at the *producer*, before the indirection buffer
ever matters; consumer-side the indirection answers it. (2) `:shrink` slot
lifecycle — held: `free-slot!` zeroes freed slots (no ghost rendering);
slot counts reconciled in every verified cell. (3) ID churn — CONFIRMED
collapse (grow+shrink+full permutation), by far the worst shape at every
stage.

## 7 · Threats to validity

1. **No websocket / no Electric DAG.** The probe measures the protocol's
   endpoints (real differ, real patch algebra, real pool) but not Electric's
   signal-graph overhead per diff nor its wire codec/serialization. Boundary
   1's transport hop is bounded only by the payload proxy (§5). A full
   `e/for-by` server→client run needs product entry points — out of this
   window's scope.
2. **Client-side mint contamination.** In live-mint cells the differ runs
   inside the RAF frame (real system: server-side, off this thread). Mint
   and apply are timed separately and reported separately; premint cells
   isolate apply at 10⁴. GC arenas are shared regardless.
3. **Entity-grade, not glyph-grade.** Rows are 28-float rects. Real trail
   cards fan out into glyph instances downstream (text-face); 10⁴ entities
   here ≠ 10⁴ glyph quads there. The measured path is scene-store apply,
   per the pre-registration — draw/encode is not exercised (no render
   pipeline in the bench; pool writes + queue-done sampling only).
4. **Rate is stress-shaped.** 60 batches/s everywhere; real delta cadence
   is ~0.1–10Hz. This biases every steady-state number conservative.
5. **JVM cell caps.** 10⁴ permutation cells hit their 30s time cap after
   1–2 steps (n=1–2 samples, warmup-included) — the order of magnitude is
   the finding; p50s there are not distribution estimates. churn-100@10⁴
   never completed one JVM step within ~250s (two attempts, run-level
   timeouts); the browser minted the same diff in 36.8s/38.4s. Note the
   direction: **V8 outruns the JVM ~6× on the permutation path** (permute-
   rot 12.7s browser vs 73.8s JVM), so browser mints are a *lower* bound
   for the server-side cost — strengthening, not softening, the reorder
   finding. grow-append@10⁴ JVM was lost to the same timeouts (it sits
   behind churn-100 in the cell order); its estimate comes from the linear
   regime (10³ = 0.24ms; browser 10⁴ = 12.7ms client-side).
6. **Adapters.** Headless = SwiftShader (CPU Vulkan); windowed = RDNA-3.
   Agreement within noise ⇒ conclusions CPU-bound and adapter-robust; but
   real driver/PCIe behavior under *drawing* load was not exercised.
7. **Single machine, single browser, one session.** Ryzen 9900X / Chrome
   150 / Linux 6.17. Absolute ms are machine-local; the regime boundaries
   (linear vs permutation; O(delta) vs O(n)) are the durable result.
8. **pr-str payload proxy** overstates Electric's binary-ish codec sizes;
   use ratios, not bytes.

## 8 · Reproduction

Bench sources (this window's files, NEW): `bench/src/render_probe/`
{`mixes.cljc`, `differ_bench.clj`, `consumers.cljs`, `probe10k.cljs`}.
Runner scaffolding + raw artifacts in the session scratchpad
(`deps.edn`, `compile-opts.edn`, `index.html`, `server.mjs`, `analyze.py`,
`results-run{1,2,3,4}.ndjson`, `differ-results.edn`, `analysis-run{2,4}.txt`).
Commands: JVM — `clj -M -m render-probe.differ-bench`; browser —
`clj -M -m cljs.main -co compile-opts.edn -c`, `node server.mjs`, then
Chrome at `:8787/index.html` (`?premint-only=1` for the premint subset).
Cell protocol: 20 warmup + 150 measured frames (all recorded, warmup
flagged), 15s cap, fresh pool per cell, verification after every cell.
