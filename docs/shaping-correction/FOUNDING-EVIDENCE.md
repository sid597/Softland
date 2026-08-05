# shaping-correction — FOUNDING EVIDENCE (banked 2026-08-04)

The permanent record of the 2026-08-03 navigation + hover profiling capture
that grounds the shaping-correction contract. Everything load-bearing in the
contract cites THIS file; this file depends on nothing outside
`evidence/` — not the capture chat, not `/tmp`.

## 1. Custody

Raw receipts retained: the eight files in
`docs/current-mental-model/build/shaping-correction/evidence/` (manifest §2).
They were copied 2026-08-04 ~00:44 local from
`/tmp/softland-nav-profile.nREzqw/` (a disposable Chrome-profile directory —
only these eight files were evidence; the containing directory's Chrome
profile/cache/extension state was deliberately NOT preserved and is
disposable). Hashes below were computed at the source AND re-computed at the
destination; byte-identical.

**Unresolved (published-hash cross-check):** the capture session published
SHA-256 values in its own chat; no hash manifest was ever banked to disk, and
that chat is not durable. The values below are computed-at-banking-time from
the source files (mtimes 2026-08-03 23:45–00:04 local, inside the capture
window) and are internally consistent with every number the ruling states
(§4–§5 re-derive them). They are the founding values from here on. If the
capture chat survives anywhere, a one-line eyeball compare closes this;
nothing downstream blocks on it.

**Count note:** the banking order said "nine files" and named eight; the
source directory contained exactly the eight named. Substance binds, counts
are hints (work-package manifest law) — eight files are the evidence set.

## 2. Hash manifest (SHA-256, computed at banking)

```
bc56a45da09187ec523ac483ccd23e6704e90bea73532ea729a68a5c64f302bc  navigation.cpuprofile
277bcb195963df78118c7faa7e3dfd9908c26264988c8818bffd0daac1b3c55e  navigation-console.json
e5f390c4f45d04b3acd4717203c28d1cccb55577df1509a731756e9e0f3d85e5  navigation-summary.json
0842d0b6cd7f8edea6696b75938723a172d6bcc7ab45f389e1869c71845593ef  hover-1.cpuprofile
f3c415b71927faba1eec670c234bf3c02f52d8abfd8bc8c1085e4e7f172a797f  hover-2.cpuprofile
5c37b0e8d194ca6a3cffaccf56954299cbfb1a4f177ccd88e8debf5089587d69  hover-3.cpuprofile
4984f00e7ce2d7ec75c8f8ced64325eb0ae2f947ac49e3a94e07cc9a13a4f22c  hover-4.cpuprofile
85f444e658bad693b58449f776e42048f660e6320f29975f009f3d3217c4a011  hover-summary.json
```

Verify anytime: `sha256sum evidence/*` against this table.

## 3. Capture conditions

- Date/window: 2026-08-03, ~18:14:45–18:31Z (navigation boot 18:14:45.530Z;
  hover transitions 18:30:37–18:30:53Z).
- App: the real land at the dev app, shadow-cljs live build (`#11/#12 ready`
  in console), T1 shaped provider ACTIVE (HarfBuzz road; `[FONT]` boot lines
  present).
- Browser: Chromium 150.0.7871.46, driven headlessly against a scripted
  navigation + hover storyboard; viewport 800×601, devicePixelRatio
  1.046875, canvas format rgba8unorm.
- **Environment attestation gap (unresolved):** the adapter identity was
  logged only as `[GPU-BUDGET] Adapter limits: JSHandle@object` — the object
  was never serialized, so `isFallbackAdapter`/description are NOT resolvable
  from this capture. Consequence: any GPU-side wall-clock conclusion would be
  unclassifiable (scene-substrate G4 lesson). The findings below survive this
  because they are CPU-profile call-tree facts and renderer-CPU interval
  receipts; the ruling likewise attributes the hot share to CPU work, not GPU
  execution. Every re-profile the contract orders MUST open its receipt with
  a serialized adapter attestation.
- Corpus identity (from the in-capture ground reports, all consistent):
  **172 blocks · 36 machine · 586,927 served chars · 177 live slots ·
  river-events 247 · blocks-served 916 · zoom 0.1 (navigation) / paste-block
  hovers at zoom 0.1**.

## 4. Cold navigation — measured facts

Source: `navigation.cpuprofile` (52,471 samples, 55.242s wall),
`navigation-console.json`, `navigation-summary.json`.

- **Cold visual settle 35.678s**: capture wallStart 1785780885530
  (18:14:45.530Z) → final settle RAF console line at 1785780921208. Verbatim
  final frame receipt:
  `[RAF] prep: 0.4 ms | text-gpu: 28725.0 ms | rects-gpu: 0.5 ms | draw: 21.4 ms | TOTAL: 28747.3 ms`
  preceded by `[SCENE-FACES/G8] slot text geos reshaped: 173 | live slots: 177`.
  So the final renderer frame is 28.747s, of which the `text-gpu` interval
  (CPU-side shape/pack/upload, despite the name) is 28.725s — 99.9% of it.
- **Call-tree totals** (inclusive, derived from the banked cpuprofile by the
  §7 method):
  - `app$client$workspace$text_layout$layout` — 29.109s
  - `app$client$substrate$webgpu$renderer$position_text` — 28.606s
    (`position_text_op` 28.602s)
  - `app$client$workspace$text_layout$shaped_layout` — **28.343s**
  - `app$client$workspace$runtime$render$reconcile_slot_text_geos_BANG_` — 27.683s
  - `app$client$workspace$text_layout$union_bounds` — **24.390s**
  - for contrast: `material_ink_bounds` 0.101s · `block_wrap_lines` 0.001s ·
    `ground$reconcile_BANG_` 0.846s.
- Self-time is diffuse (top self entries are cljs.core seq/equiv/aclone
  machinery) — the cost is algorithmic shape, not one hot leaf.
- The main-thread stall ledger in the final ground report records the same
  event from the watchdog's side: `{:ms 30051, :at "2026-08-03T18:15:21.210Z"}`.
- An early frame with 5 slots took 28.9ms total (`text-gpu: 26.7`) — the
  per-slot cost explodes with content size, not slot count alone.

**Reading (checked against source):** `union_bounds` sits INSIDE
`shaped_layout`'s time because the per-cluster ink-bounds pass builds a lazy
`(keep :ink-bounds (filter <whole-glyph-scan> glyphs))` that is realized
inside `union-bounds`' reduce — the O(C×G) whole-glyph filter is charged to
`union_bounds` frames. The defect is the scan, not the min/max arithmetic.

## 5. Hover — measured facts

Source: `hover-summary.json` + `hover-{1..4}.cpuprofile` (one profile per
transition, in order: largest→ordinary-1, ordinary→largest,
largest→ordinary-2, ordinary→empty).

Blocks: ordinary `…ep:3405ac6d:000000` (66 chars) ↔ largest paste block
`…ep:3c6512e2:000000` (**24,891 chars**).

| transition | input→RAF | reshaped slots | text-gpu | RAF TOTAL |
|---|---|---|---|---|
| largest→ordinary-1 | 6,124ms | 2 | 5,909.0ms | 5,925.7ms |
| ordinary→largest | 6,028ms | 2 | 5,871.4ms | 5,887.6ms |
| largest→ordinary-2 | 6,101ms | 2 | 5,923.0ms | 5,940.0ms |
| ordinary→empty | 28ms | 1 | 4.0ms | 20.7ms |

Per-transition call-tree totals (hover-1/2/3): `reconcile_slot_text_geos!`
5.761/5.677/5.775s · `shaped_layout` 5.694/5.621/5.715s · `union_bounds`
5.312/5.293/5.322s. The ordinary→empty control (hover-4) proves the fast
path: leaving a small block reshapes 1 slot in 4ms.

**Reading:** a hover flip rebuilds BOTH blocks (old + new hover target —
`ground/pointer-move!`), each rebuild mints new op vectors (tint-only paint
change), the slot reconciler's `identical?` reuse check misses, and both
slots re-run full shaped layout — the 24,891-char paste block alone is
~5.9s. Hover is a paint-only semantic change buying two full layout
executions.

## 6. Facts the ruling settles that this capture does NOT itself prove

Recorded so nobody later reads them as measured here:

- **Historical 2–3-minute loading**: UNEXPLAINED residue. This capture's
  cold settle is 35.678s; nothing here attributes the historical 2–3min
  observations. Residue, not attributed fact.
- **`<store-frame>` execution count/wall share**: not measured by this
  capture (sampled frames put it outside the hot share; no counter exists).
  The contract adds a counter to diagnostics; no reactive/store conclusion
  is licensed until that counter makes it decision-relevant.
- **Reconciliation** (`reconcile!` 0.846s here) is material but secondary —
  per the ruling, GPU execution, paint, packing, upload, draw, and sampled
  store-frame work are not the present hot share.
- Adapter attestation gap (§3) — capture-harness defect to fix at the next
  capture, not a product fact.

## 7. Derivation method (so the totals are re-computable forever)

Totals in §4–§5 were computed from the banked `.cpuprofile` files (V8
format: `nodes` tree + `samples` + `timeDeltas`) as: per-sample time from
`timeDeltas` attributed to the sampled node, then summed up the ancestor
chain, counting each function name once per sample (standard inclusive
"total" semantics; recursion not double-counted). ~40-line reference
implementation:

```js
const prof = JSON.parse(fs.readFileSync(file,'utf8'));
const nodes = new Map(prof.nodes.map(n=>[n.id,n]));
const parent = new Map();
for (const n of prof.nodes) for (const c of (n.children||[])) parent.set(c,n.id);
const timePerNode = new Map();
for (let i=0;i<prof.samples.length;i++)
  timePerNode.set(prof.samples[i],(timePerNode.get(prof.samples[i])||0)+(prof.timeDeltas[i]||0));
const totalByName = new Map();
for (const [id,us] of timePerNode){
  const seen = new Set(); let cur = id;
  while(cur!==undefined){
    const n = nodes.get(cur); if(!n) break;
    const key = n.callFrame.functionName+' @ '+n.callFrame.url+':'+n.callFrame.lineNumber;
    if(!seen.has(key)){ totalByName.set(key,(totalByName.get(key)||0)+us); seen.add(key); }
    cur = parent.get(cur);
  }
}
```

Console-derived receipts (§4 settle, RAF lines, G8 lines, ground reports)
are verbatim strings inside `evidence/navigation-console.json` and
`evidence/hover-summary.json` — grep them there.
