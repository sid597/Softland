# PROBE-EVIDENCE — first-hand Electric/Missionary observations from the §9 render probe

**Session:** 2026-07-05 render-bench (the only session running Electric/
Missionary machinery live today). **Scope:** verification-grade input for the
electric-skill rebuild's adjudication stage. Numbers live in
`../render-north/PROBE-10K.md` (cited by section, not duplicated). This file
is mine alone; `HACKS-LEDGER.md` is the harvest agent's.

**Versions (from the classpath actually run, `clj -Stree`):**
- `com.hyperfiddle/electric v3-alpha-SNAPSHOT` = jar
  `electric-v3-alpha-20260325.114002-44` (the resolved snapshot in `~/.m2`)
- `missionary/missionary b.46`
- `org.clojure/clojurescript 1.11.132` (project pin; overrides Electric's
  1.11.121), Clojure 1.12.4, OpenJDK 21.0.11, Chrome 150 Linux

**Bench entry points** (all NEW files, `bench/src/render_probe/`):
`differ_bench.clj` (JVM producer), `probe10k.cljs` + `consumers.cljs` +
`mixes.cljc` (browser consumer matrix over the real
`app.client.substrate.webgpu.buffer-pool`).

**Re-run commands** (from the session scratchpad, which carries `deps.edn`
and `compile-opts.edn`; see PROBE-10K.md §8):
- JVM: `clj -M -m render-probe.differ-bench`
- Browser: `clj -M -m cljs.main -co compile-opts.edn -c` → `node server.mjs`
  → Chrome at `http://127.0.0.1:8787/index.html` (append `?premint-only=1`
  for the isolated-consumer subset)

**What this probe did NOT exercise** (so the adjudicator doesn't over-credit
it): no `e/defn`/Electric DAG at runtime, no websocket transport, no
`m/latest`/`m/ap`/`m/eduction` flows, no `e/server`/`e/client` transfer. The
probe ran the incseq layer (differ, patch algebra, mount algorithm) raw plus
Missionary only as a transitive load. Entries below stay inside what was
actually observed.

---

## 1 · Numbered observations (first-hand, this session)

1. **`hyperfiddle.incseq/->seq-differ` has no cheap no-change path: an
   unchanged n-element collection still costs O(n) to re-diff.** Evidence:
   `differ_bench.clj` cold-load cells — steady empty-diff mints on an
   unchanged 10⁴ vector cost ~2.0ms p50 on the JVM (PROBE-10K §2, cold-load
   row); same shape in-browser at ~10–12ms. Status: **VERIFIED-HERE**.

2. **Value-change diffs mint in the linear regime at all probed scales:
   ~0.2µs/row scan floor plus per-change work.** Evidence: `differ_bench.clj`
   change-1/change-10 rows — 0.05ms→2.5ms (change-1) and 0.04ms→4.8ms
   (change-10) across 10²→10⁴ (PROBE-10K §2). Status: **VERIFIED-HERE**.

3. **Any reorder makes `->seq-differ` cost explode super-linearly (~×130–700
   per decade): rotate-by-n/4 minted in 0.62ms / 115ms / 73.8s at
   10²/10³/10⁴ on the JVM.** Evidence: `differ_bench.clj` permute-rot rows;
   browser corroboration 12.7s at 10⁴ (`probe10k.cljs` live-mint cell).
   Status: **VERIFIED-HERE** (10⁴ JVM is n=1 by time cap; the decade trend is
   the reproducible finding).

4. **Dropping elements from the FRONT of a keyed collection produces a
   full-shift `:permutation` and inherits the reorder cost.** Evidence:
   `mixes.cljc` churn-10 (window slide) → measured ops grow/shrink/perm/
   change = 1000/1000/**11000**/1000 at 10⁴; mint 19.4s JVM (PROBE-10K §2).
   Append-only stays linear (grow-append row). Status: **VERIFIED-HERE**.

5. **Full ID churn (every key fresh) is the worst diff shape at every stage:
   the JVM could not mint ONE such 10⁴ diff within ~250s (two attempts);
   V8 minted the same diff in 36.8s/38.4s.** Evidence: `differ_bench.clj`
   run-level timeouts + `probe10k.cljs` churn-100 setup mints (PROBE-10K §2,
   §7.5). Status: **VERIFIED-HERE** (the JVM bound; the browser numbers).

6. **V8 outruns JVM 21 ~6× on the differ's permutation path** (same diff,
   same algorithm: 12.7s browser vs 73.8s JVM at 10⁴ rotate). Server-side
   minting — where Electric actually runs the differ — is the slow side.
   Evidence: cross-table PROBE-10K §2 vs §7.5. Status: **VERIFIED-HERE**.

7. **`hyperfiddle.incseq/patch-vec` is correct and cheap as a consumer-side
   state maintainer at 10⁴: every valid cell's 150-frame stream ended with
   patch-vec state equal to the independently-generated expected collection
   (`rows-match? true`), including self-composed rotation diffs re-applied
   150×.** Evidence: `consumers.cljs` `:verify` + per-cell verification in
   all four runs (PROBE-10K §1, §3). Status: **VERIFIED-HERE**.

8. **`hyperfiddle.incseq.mount-impl/mount` passes two different value kinds
   through `insert-before`: new ITEMS in the append phase, existing CHILD
   HANDLES during permutation rotations (DOM move semantics). Callbacks that
   allocate-on-insert therefore corrupt state under any `:permutation`.**
   Evidence: read of `mount_impl.cljc` (jar source) + live demo — the
   as-built `gpu-mount` bridge ended a 91-frame rotate run with 16,750
   active slots for 100 entities while patch-vec stayed correct
   (PROBE-10K §4). This is contract semantics, not a bug in mount: it is
   correct over DOM. Status: **VERIFIED-HERE**.

9. **`e/diff-by` and `e/for-by` are `->seq-differ` end to end**: in the
   shipped jar, `e/diff-by` expands through `i/diff-by` (electric3.cljc:159)
   and `e/for-by` binds a cursor over `diff-by` (electric3.cljc:243) — so
   entries 1–6 are the mint cost of Electric's public collection API, not of
   an internal path. Evidence: jar source read this session (not a runtime
   trace). Status: **VERIFIED-HERE** (source identity; runtime DAG overhead
   on top remains unmeasured — PROBE-10K §7.1).

10. **Diff payloads for reorders are O(n) maps even when no value changed:**
    pr-str proxy ~108KB for a pure 10⁴ rotate vs ~14KB for 100 real changes.
    Evidence: `differ_bench.clj` payload column (PROBE-10K §5; pr-str is a
    proxy, ratios are the signal). Status: **VERIFIED-HERE**.

11. **The whole incseq stack (incseq, perm-impl, stateful-diff-impl,
    mount-impl) plus missionary b.46 compiles under vanilla `cljs.main`
    1.11.132 outside shadow-cljs, and runs in-browser without Electric
    booted.** Evidence: `compile-opts.edn` builds in ~1 min; all four
    browser runs. Useful for future bench/test harnesses that want incseq
    without the app. Status: **VERIFIED-HERE**.

12. **`hyperfiddle.rcf` emits compile warnings under cljs.main's AOT cache
    ("Use of undeclared Var ana/cljs?", "No such namespace: impl") with no
    observed runtime effect.** Evidence: `compile.log`/`compile2.log` in the
    scratchpad; probes ran green afterward. Status: **OBSERVED-ONCE**
    (compile-time only; did not isolate whether specific rcf features would
    break at runtime — tests were disabled throughout).

## 2 · Hacks applied to make the probe run (suspicions, not verdicts)

- **Vanilla cljs.main instead of shadow-cljs.** Tried nothing else first —
  chosen because the window forbade touching `shadow-cljs.edn`. Worked
  first try with a scratch `deps.edn` (absolute `:paths` into the repo +
  electric/rcf deps). Suspicion: **not platform** — session constraint.
  Transferable trick for skill docs: incseq benches don't need the app
  build.
- **Incremental `prn` + `flush` per JVM bench cell.** First attempt buffered
  one final EDN dump; a run-level timeout during a slow 10⁴ cell destroyed
  all results (0-byte file). Rewrote to line-per-cell. Suspicion:
  **my-error** (bench design), worth encoding as bench discipline anywhere
  near `->seq-differ` at scale, because of the next item.
- **Time caps cannot interrupt a single differ call.** My per-cell cap
  checked between steps; one churn-100@10⁴ `(differ coll')` call ran >250s,
  blowing through two outer timeouts. Suspicion: **platform-real cost
  behavior** (uninterruptible synchronous mint), bench-design consequence.
- **C1 restricted to change/append/tail-shrink mixes** after tracing the
  mount/insert-before dual-kind semantics (entry 8); one deliberate
  corruption cell kept as the demonstration. Suspicion: **platform-real
  contract semantics** — the error would be reusing the DOM-shaped mount
  contract for slot pools (allocate-on-insert), which is exactly what the
  dormant `gpu-mount` docstring invites.
- **Headless Chrome WebGPU is SwiftShader-only on this box**: three flag
  combos (`Vulkan`, `DefaultANGLEVulkan,VulkanFromANGLE`,
  `--use-webgpu-adapter=default`) all yielded `requestAdapter() → null` or
  SwiftShader; hardware (RDNA-3) required a windowed run on DISPLAY=:0 with
  `--disable-backgrounding-occluded-windows`. Suspicion: **platform-real**
  (Chrome/Linux headless limitation), unrelated to Electric — recorded
  because it will bite any future headless render bench or first-light
  automation.
- **Chrome ProcessSingleton lock**: a second launch against the same
  `--user-data-dir` aborts while the first is alive; headless Chrome
  outlives its page. Suspicion: **platform-real**, trivial — fresh profile
  dir per run.

## 3 · CLAUDE.md Electric/Missionary claims — what these runs actually touch

Most CLAUDE.md patterns concern the Electric/Missionary *reactive runtime*
(`m/ap`+`m/?<` cancellation, `m/eduction` filtering, side effects in
`m/latest`, try/catch in `e/defn`), which this probe did not run. Honest
ledger:

- **CONFIRMED — "unconditional RAF + `identical?` skip / apply at the
  consumer edge" (the m/latest side-effects rule's architectural half):**
  applying diffs only at the consumer edge kept idle frames at 0.0–0.1ms
  across 149-frame idle stretches at 10⁴ (PROBE-10K §3, cold-load rows) —
  the O(1)-idle discipline the rule exists to protect survives a
  diff-consuming store.
- **CONFIRMED — the rule's own sunset premise ("dirty-present requires
  Electric diffs as the change signal — Gap 3"):** a real incseq diff
  stream is a workable change signal at 10⁴ for change/append/tail-shrink
  shapes (mint 2–5ms server-side, apply ≤4.2ms p95, 60fps) — with the new
  boundary that reorder-shaped diffs must never be the signal carrier
  (PROBE-10K §6). This sharpens the pattern; it does not contradict it.
- **CONTRADICTED:** nothing. No CLAUDE.md Electric/Missionary claim was
  falsified by these runs.
- **UNTESTED (do not credit this probe for them):** m/ap+m/?< "Watch
  cancelled" crashes; m/eduction-vs-m/ap event filtering; side-effects-in-
  m/latest RAF death spiral; "try/catch is TODO" in e/defn; the continuous-
  synchronous paradigm claims. All remain whatever the harvest/verify
  stages establish from other evidence.
