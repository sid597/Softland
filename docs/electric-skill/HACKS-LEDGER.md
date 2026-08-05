# Electric / Missionary Hacks & Behavioral-Claim Ledger

Harvest of every Electric/Missionary behavioral claim, workaround, or hack this
project has accumulated. **This is a HARVEST, not a verdict.** Each entry states
the claim falsifiably, lists every place it appears, notes where code relies on
it, proposes a cheap verification method, and records a labelled SUSPICION (not a
conclusion). Nothing here is judged true or false. Line numbers are as-of
2026-07-05 on branch `docs/current-mental-model-local`.

---

## Header: versions, drift, counts

### Exact versions in use
- **Electric**: `com.hyperfiddle/electric {:mvn/version "v3-alpha-SNAPSHOT"}` -- `deps.edn:3`.
  Resolved SNAPSHOT in `~/.m2` is timestamped build **`v3-alpha-20260519.115706-45`**
  (latest per `maven-metadata-clojars.xml` `lastUpdated 20260519115706`; older
  builds -16/-19/-25/-34/-38/-42/-44 also cached). Namespace consumed in app
  code is **`hyperfiddle.electric3`** (`src/app/electric_flow.cljc:3`,
  `src/app/file_viewer.cljc:6`, `src-dev/dev.cljc:4`).
- **Missionary**: **`b.46`** -- NOT directly pinned in `deps.edn`; it arrives
  transitively via Electric's POM
  (`~/.m2/.../electric/v3-alpha-SNAPSHOT/electric-v3-alpha-SNAPSHOT.pom` ->
  `<groupId>missionary</groupId> <version>b.46</version>`; every timestamped
  Electric POM -42/-44/-45 agrees on b.46). `~/.m2` also caches `b.43`, `b.44`.
- **ClojureScript**: `1.11.132` (`deps.edn:11`). **Clojure**: `1.12.4` (`deps.edn:10`).
- **shadow-cljs**: `2.28.23` (`deps.edn:34`, via `:dev`/`:build` aliases).
- Electric shadow reload hook wired ONLY on the `:dev` build
  (`shadow-cljs.edn:11` `hyperfiddle.electric.shadow-cljs.hooks3/reload-clj`);
  the `:prod` build (`shadow-cljs.edn:12-16`) has no Electric hook.

### The v2 / v3 drift finding
- **`src-prod/prod.cljc:9` requires `[hyperfiddle.electric :as e]`** -- the **v2**
  namespace. The pinned dependency is v3-alpha-SNAPSHOT, whose jar provides
  `hyperfiddle.electric3` (v3), NOT `hyperfiddle.electric` (v2). So the `:prod`
  entrypoint references a namespace the resolved jar does not ship.
- Everything else is on v3: `src-dev/dev.cljc:4` and `src/app/*.cljc` use
  `hyperfiddle.electric3`; `prod.cljc` also uses v3-only DOM contracts elsewhere
  but names the v2 ns at the top.
- Corroborated in `memory/implementation-quirks.md:166`:
  "The repo's `:prod` shadow build is pre-broken (prod.cljc requires Electric v2
  `hyperfiddle.electric`); compile-check the `:dev` build in-process via shadow
  api instead."
- **Net**: the app runs only through the `:dev` build; the `:prod` build is
  believed non-compiling as written. (See Claim 20.)

### Count of claims by proposed verification method (20 distinct claims)
- **JVM-repro** (pure Missionary test, no DOM): 9  -> claims 1, 2, 3, 6, 7, 8, 9, 10, 11
- **primary-source** (Electric/Missionary source or upstream docs; web access available): 5 -> claims 12, 13, 15, 16, 17
- **compile-check** (e/defn / build macroexpansion behavior): 3 -> claims 5, 18, 20
- **UNVERIFIABLE-cheaply** (needs live Rama cluster or timing/starvation race): 3 -> claims 4, 14, 19

### What the two skills actually teach (inventory, not claims)
- **`.claude/skills/electric-docs/SKILL.md`** teaches **zero behavioral claims**.
  It is a pure file-map / lookup table pointing at upstream Electric+Missionary
  source files, with a "Quick Lookup" table. NOTE the paths are partly stale: it
  cites `docs/electri_tutorial.txt` and `docs/missionary-complete-reference.txt`
  which **do not exist**; the real files are `docs/reference/electric-tutorial.txt`,
  `docs/reference/missionary-reference.txt`, and `docs/electric/*.cljc` (the
  latter DO exist: electric3.cljc, electric_dom3.cljc, incseq.cljc, etc.).
- **`.claude/skills/reactive_master.md`** teaches philosophy/voice, not APIs:
  events-as-rivers, one-writer-per-atom, pure derivation, terminal-only side
  effects, pull model for game loops, backpressure via `m/relieve`/`m/sample`.
  It **advocates `m/ap` + `m/?<` for derivation** (lines 26-27) -- the exact
  pattern CLAUDE.md later BANNED (see Claim 1, "Surprises").

### Reference-doc provenance note
`docs/reference/{electric-codebase.txt, electric-tutorial.txt, missionary-reference.txt}`
and `docs/electric/*.cljc` are **upstream copies** (Electric/Missionary source &
tutorial). Claims traceable to them are marked "upstream-documented" below; the
project-discovered part is the *specific failure application*.

---

## Claims

### 1. `m/ap` with multiple `m/?<` forks, when fed into `m/latest`, crashes with "Watch cancelled"
- **Claim**: A derived flow built as `(m/ap (let [a (m/?< (m/watch !a)) b (m/?< (m/watch !b))] ...))` that is then passed as an input to `m/latest` will, when any watched atom changes, cancel the old `m/ap` branch; `m/latest` propagates that cancellation and tears down the whole flow graph, surfacing as `Reactor failure: missionary.Cancelled {message: 'Watch cancelled.'}`.
- **Provenance**: `CLAUDE.md:31-51`; `docs/history/insights.md:224-330` (full Session-10 writeup + before/after table); `docs/history/insights.md:42-49` (the `m/join`/cancel-propagation basis); `docs/history/progressive-summary.md:366-370` (Lesson 1); `memory/implementation-quirks.md:17`; `memory/core-reframes.md:218` ("unstable under cancellation").
- **Upstream basis**: `docs/reference/missionary-reference.txt:339-372` documents `m/?<` as *preemptive fork* that can be "shutdowned when more values become available"; line 760 "Cancelling propagates to upstream flow." The mechanism is upstream; the "crashes m/latest" application is project-discovered.
- **Relied on in code**: The fix is pervasive -- the render pipeline uses `m/latest` for all combiners (`src/app/client/workspace/runtime/render.cljs:81-118` `<world-snapshot`), and discrete filters use `m/eduction` instead (`src/app/client/workspace/events.cljs:179-219`). CLAUDE.md encodes the ban as a hard rule.
- **Verification**: **JVM-repro** -- build the bad shape with two `m/watch` atoms + `m/ap`/`m/?<` inside an `m/latest`, mutate one atom, assert a `Cancelled` reaches the reducer; then swap to `m/latest`-of-watches and assert no cancellation. No DOM needed.
- **SUSPICION: platform-real** (mechanism is upstream-documented; failure shape reproduced repeatedly in-project across S10/S12).

### 2. Discrete event filtering must use `m/eduction` + `@deref`, not `m/ap` + `m/?<` on `m/watch`
- **Claim**: Filtering a discrete event stream by a mutable predicate atom via `(m/ap (let [e (m/?< >events) f (m/?< (m/watch !focus))] (when (= f :x) e)))` cancels the in-flight branch every time `!focus` changes; reading the atom with `@!focus` inside an `m/eduction (filter ...)` transducer avoids the fork/cancel entirely.
- **Provenance**: `CLAUDE.md:53-68`; `docs/history/insights.md:306-329`; `docs/history/insights.md:442`; `memory/core-reframes.md:219`.
- **Relied on in code**: `src/app/client/workspace/events.cljs:175-219` -- the header comment "Do NOT use m/ap with m/?< on m/watch here" plus all five focus routers (`<global-events`, `<editor-keys`, `<cmd-panel-keys`, `<chat-input-keys`, `<settings-panel-keys`) implemented as `(->> >keyboard (m/eduction (filter (fn [e] (and (= @!focus ...) ...)))))`. Consumed at `runtime.cljs:350-354`.
- **Verification**: **JVM-repro** -- feed a seeded discrete flow through both shapes while mutating the predicate atom; assert the `m/ap` shape drops/cancels events and the eduction shape passes them.
- **SUSPICION: platform-real** (same cancellation mechanism as Claim 1).

### 3. A side effect (RAF `request!`) inside `m/latest` creates a circular dependency, so an `identical?` skip at the consumer kills the RAF loop permanently
- **Claim**: If the RAF-scheduling call lives inside the `m/latest` combining fn, then sampling depends on the fn running and the fn running depends on being sampled; when the consumer-level `identical?` check skips an idle frame, the fn is not called, `request!()` never fires, and the animation loop dies and never restarts. The project's fix is an **unconditional self-sustaining RAF flow** + `identical?` skip at the reducer.
- **Provenance**: `CLAUDE.md:70-99` (bad/good code + "Why" + "Rule: dirty-present requires Electric diffs (Gap 3)"); practical justification at `src/app/client/workspace/runtime.cljs:370-373` ("Setting canvas.width/height clears the swap chain, but unconditional RAF redraws within 16ms").
- **Relied on in code**: `src/app/client/workspace/events.cljs:145-157` `make-raf-flow` (always re-schedules via `js/requestAnimationFrame`, unconditional); `src/app/client/workspace/runtime/render.cljs:81-118` (pure `m/latest` `<world-snapshot`), `:121-123` (`m/reduce` with `(identical? world (:prev-world prev-state))` skip), `:558` (`(m/sample vector <world-snapshot >raf)`); canvas resize is synced inside the same RAF at `render.cljs:434-443`.
- **Verification**: **JVM-repro** -- model RAF as a discrete self-ticking flow and "request" as a side-effecting volatile; reproduce the good (unconditional) vs bad (request-inside-latest) shapes and assert the bad one stops ticking once a value repeats identically. No DOM/GPU needed.
- **SUSPICION: platform-real** (consistent with demand-driven `m/latest` semantics; but the "dies PERMANENTLY" strength has only been argued, see Claim 4 for the related starvation variant).

### 4. Rapid input changes (e.g. resize animations) can STARVE `m/latest` so its fn never runs and no RAF is scheduled
- **Claim**: Under a fast burst of input changes, derived `m/latest` flows restart before they settle, so the combining fn never completes a run; any scheduling that depended on that run (conditional/dirty RAF) is never issued.
- **Provenance**: `CLAUDE.md:97` (single sentence appended to the side-effects-in-latest rule).
- **Relied on in code**: Indirect -- it is a second justification for the unconditional-RAF choice (Claim 3 reliance sites). No standalone consumer.
- **Verification**: **UNVERIFIABLE-cheaply** -- starvation is a timing/scheduling race; a deterministic JVM-repro would need to force "restart before settle" ordering, which is not obviously constructible without controlling the reactor scheduler. Could attempt a stress harness but result would be flaky.
- **SUSPICION: unknown** (plausible from demand-driven sampling, but stated once, never with a captured repro; could be a restatement of Claim 3 rather than an independent phenomenon).

### 5. `try/catch` is not supported inside `e/defn` and throws "try is TODO"
- **Claim**: Placing a `try/catch` form inside an `e/defn` body fails at macroexpansion/compile with a "try is TODO" error; error handling must be done outside the Electric reactive context.
- **Provenance**: `CLAUDE.md:101-104`; `docs/history/insights.md:332-343`; `memory/implementation-quirks.md:48`; `memory/core-reframes.md:221` ("storms (try/catch in Electric)").
- **Relied on in code**: **Encoded in docs / avoided in practice.** No `e/defn` in the tree contains `try`; server-side `try/catch` lives only in plain `#?(:clj ...)` defns (e.g. `src/app/file_viewer.cljc:81-86` inside `read-file-content`, a non-Electric fn). The "try is TODO" string is NOT present in the local Electric source snapshot `docs/electric/electric3.cljc` (only an unrelated TODO at line 6), so this is a runtime/compile-observed claim, not copied from the reference.
- **Verification**: **compile-check** -- attempt to macroexpand/compile a minimal `(e/defn X [] (try 1 (catch :default _ 2)))` against the pinned v3 build and observe the error (or its absence on this build).
- **SUSPICION: platform-real, possibly stale-version** (Electric v3-alpha is a moving SNAPSHOT; the ban was recorded on an earlier build than -45, so current-build behavior is worth a re-check).

### 6. `m/latest` arg count MUST equal the combining fn's param count, or you get silent corruption (no error)
- **Claim**: If the number of flows passed to `m/latest` does not match the arity of its combining fn, values bind to the wrong parameters (or params go nil) with no thrown error -- a silent miswiring.
- **Provenance**: `memory/implementation-quirks.md:12-16` (lists the exact arg counts per flow: `<editor-rects` splits of 4/3/4/7/7/17/combiner-6; `<combined-text-ops` 4/7/7/21; `<cmd-panel-rects` 11).
- **Relied on in code**: Every `m/latest` in the tree depends on this holding. Most load-bearing instance: `src/app/client/workspace/runtime/render.cljs:81-118` -- `<world-snapshot`'s fn takes 13 params and is passed exactly 13 flows (`<text-data`, `<editor-rect-data`, `<sidebar-flow`, `<cmd-rect-data`, `<settings-rect-data`, `<settings-text-data`, then 7 `m/watch`es). Also `combined_text.cljs`, `editor_compute.cljs`, `cmd_panel.cljs`, `settings_view.cljs`.
- **Verification**: **JVM-repro** -- call `m/latest` with an arity mismatch and assert values land on the wrong params / no exception is thrown.
- **SUSPICION: platform-real** (structural consequence of variadic `m/latest`; heavily exercised).

### 7. `m/observe` without `m/relieve` buffers one event, and a new push can drain the previously-buffered value first
- **Claim**: An `m/observe` source without `m/relieve` retains a one-slot buffer; when `!` is called with a new value, the *previously buffered* value may be delivered first within the same synchronous dispatch -- so a raw mousedown can trigger processing of a stale, previously-queued mouseup, flipping shared state true->false within ~1ms with no intermediate events.
- **Provenance**: `memory/implementation-quirks.md:52-56`; `docs/history/progressive-summary.md:414-419` (Lesson 10, with stack trace `down_h@events.cljs:54 -> Observe.cljs:37 -> Reduce.cljs -> runtime.cljs:1506`); `docs/history/progressive-summary.md:626-630`.
- **Relied on in code**: The mitigation (`m/relieve`, or moving state to raw DOM) is live: `src/app/client/workspace/events.cljs` applies `m/relieve` to blink/wheel/resize/mouse sources (grep: relieve at events.cljs and `src/global_flow.cljs:76`); drag-select was moved entirely to raw `addEventListener` and ALL Missionary writes to `!dragging?` removed (`mouse/install-drag-select!` wired at `runtime.cljs:357`).
- **Verification**: **JVM-repro** -- construct an `m/observe` without `m/relieve`, push A then B synchronously, assert consumer sees A before B (stale-first); add `m/relieve` and assert coalescing.
- **SUSPICION: platform-real** (diagnosed live via `add-watch` + `console.trace`; buffering behavior of `m/observe` is documented upstream).

### 8. Coarse invalidation: one `m/latest` watching N atoms re-runs the ENTIRE fn on ANY atom change, and the cost is in reactor propagation OUTSIDE RAF
- **Claim**: A single `m/latest` block with 25+ watched atoms recomputes its whole combining fn whenever any one atom changes (e.g. the 530ms caret blink), including branches that do not read the changed atom; because this work happens in Missionary reactor propagation (synchronous, before the next change is observed), RAF/frame counters look fast and mislead -- symptom was a 1440ms LONGTASK every 530ms on a large file.
- **Provenance**: `memory/implementation-quirks.md:19-27` (Coarse Invalidation + Impurity, S38); `docs/history/progressive-summary.md:409-412` (Lesson 9, S17); `docs/history/progressive-summary.md:493` ("expensive work happens in Missionary's m/latest reactor propagation ... outside RAF").
- **Relied on in code**: Motivates the flow splits -- `<content-text`/`<chrome-text` split (`progressive-summary.md:56`), document-vs-visual flow separation, and the wide-but-split `<world-snapshot`/`<combined-text-ops` shapes in `render.cljs` and `combined_text.cljs`. The invalidation domain is still coarse by design (CLAUDE.md notes dynamic subscription is not done).
- **Verification**: **JVM-repro** -- one `m/latest` over several atoms with an instrumented fn; mutate an "unrelated" atom and assert the fn re-runs (counter increments).
- **SUSPICION: platform-real** (direct consequence of `m/latest` recomputing on any input; measured as LONGTASK in-project).

### 9. Impure derivation (a `reset!` inside a `m/latest` fn) creates hidden edges Missionary cannot track
- **Claim**: Mutating an atom via `reset!`/`swap!` inside a flow's combining fn is invisible to Missionary's dependency graph -- the flow does not become a tracked dependency of that atom's readers, so downstream derivations can go stale or oscillate.
- **Provenance**: `memory/implementation-quirks.md:20-26` (names `build-right-detail` at `dg_flow.cljs:492` mutating `!detail-max-scroll` via `reset!` at lines 502/565); "Side effects inside derivations are un-Electric -- they create hidden edges Missionary can't see." Related oscillation symptom: `memory/implementation-quirks.md:89-94` (rect-vs-text pipeline disagreement -> `m/latest` sees alternating states -> infinite re-render).
- **Relied on in code**: Stated as an anti-pattern to be removed (Purity leg of the S38 three-part fix). The named impurity historically lived in `src/app/client/workflows/dg_flow.cljs`.
- **Verification**: **JVM-repro** -- a `m/latest` fn that resets atom B while combining atom A; assert B's watchers are not notified as caused-by-this-flow and that re-runs depend only on declared inputs.
- **SUSPICION: platform-real** (follows from Missionary tracking only declared flow inputs; the specific oscillation was observed in-project).

### 10. `m/observe` / `m/ap` flows are single-subscription; each subscription needs a fresh instance (factory fns); `m/watch` is exempt
- **Claim**: A flow created by `m/observe` or `m/ap` can be subscribed to only once; sharing one as a top-level `def` and subscribing twice fails (e.g. "text appears then disappears"). Wrap in a factory fn to mint a fresh flow per subscription. `m/watch` on an atom is safe as a `def` because the atom persists independently.
- **Provenance**: `docs/history/insights.md:9-23` (Fresh vs Shared Flows); `docs/history/insights.md:345-348` ("Text appears then disappears -> shared flow got cancelled").
- **Relied on in code**: `src/app/client/workspace/events.cljs:145-148` (`make-raf-flow` docstring: "Must be called fresh for each subscription, not shared!") and `:159-162` (`make-blink-timer` same); `src/app/client/workspace/runtime.cljs:341-347` calls each factory fresh per runtime instance ("Event flows (fresh per instance)").
- **Verification**: **JVM-repro** -- subscribe to a single `m/observe` instance twice, assert failure; subscribe to two factory-minted instances, assert success.
- **SUSPICION: platform-real** (standard Missionary single-consumer flow semantics).

### 11. `m/sample` arg order is continuous-first, discrete-trigger-last, or you get "Undefined continuous flow"
- **Claim**: `m/sample`'s signature is `(m/sample f continuous-flow* discrete-trigger)`; passing the discrete trigger first (`(m/sample vector >raf <world)`) throws "Undefined continuous flow". The sampled flows must be "initially ready" continuous flows.
- **Provenance**: `docs/history/insights.md:27-38`; `docs/history/insights.md:219-222` (debugging tips).
- **Relied on in code**: `src/app/client/workspace/runtime/render.cljs:558` -- `(m/sample vector <world-snapshot >raf)` (continuous `<world-snapshot` first, discrete `>raf` last).
- **Verification**: **JVM-repro** (cheapest) or **primary-source** -- swap arg order and assert the error; the `m/sample` contract is in `docs/reference/missionary-reference.txt`.
- **SUSPICION: platform-real** (matches upstream `m/sample` contract).

### 12. `m/join` cancellation: if any branch fails or completes, all branches are cancelled ("Watch cancelled")
- **Claim**: In `m/join`, if any task/branch fails or completes unexpectedly, the others are cancelled and join fails with that error -- surfacing as "Watch cancelled" when one joined consumer dies.
- **Provenance**: `docs/history/insights.md:42-49`. **Upstream-documented**: `docs/reference/missionary-reference.txt:783` verbatim "If any task fails, others are cancelled then join fails with this error"; :211-212 the same behavior narrated.
- **Relied on in code**: `src/app/client/workspace/runtime.cljs:363` -- the top-level `(m/join vector ...)` orchestrates ~12 consumers (timers, resize, scroll, mouse, keyboard routers, render). If any one throws, the whole loop tears down per this claim.
- **Verification**: **primary-source** (upstream docs state it) plus a trivial **JVM-repro** joining two flows where one throws.
- **SUSPICION: platform-real** (directly upstream-documented).

### 13. `e/watch` "only works within a single peer -- no server->client Missionary pipe" (STALE: current code contradicts it)
- **Claim (as recorded)**: `e/watch` cannot pipe a value from server to client; the recommended pattern was "HTTP for data transfer, Electric for bootstrap only."
- **Provenance**: `memory/implementation-quirks.md:49-51` (under "Electric 3").
- **Contradiction in current code**: `src/app/file_viewer.cljc:107-135` and `:189-190` define `(e/defn WatchSidebarTruth [] (e/server (e/watch util-fns/!sidebar-truth-atom)))` and siblings (`WatchUserSettings`, `WatchAgentTrail`, `WatchFlowSession`, `WatchWorkspaceTruth`, `WatchIngestEpoch`) that DO transfer a server-side `e/watch` result to the client, consumed at `src/app/electric_flow.cljc:484-489`. So the current architecture relies on server->client `e/watch` transfer working -- the opposite of the recorded quirk.
- **Relied on in code**: The entire Rama-truth -> client path (`file_viewer.cljc:107-190`, `electric_flow.cljc:471-508`) depends on server->client `e/watch` working.
- **Verification**: **primary-source / compile-check** -- confirm against Electric v3 transfer semantics that `(e/server (e/watch atom))` returns a client-transferred reactive value; the running app is itself the counter-evidence.
- **SUSPICION: stale-version / agent-error** -- the quirk likely predates the v3 site-transfer model (or conflated "raw Missionary pipe" with "Electric-managed transfer"); recorded once, never retracted, now contradicted by shipped code.

### 14. `foreign-proxy-async` subscription on global PStates crashes Rama 1.6.0; workaround is a server atom mirror + `e/watch` bridge, and proxy-callback must return nil
- **Claim**: Per-key/global-root `foreign-proxy-async` subscription on a global PState crashes Rama 1.6.0 (RocksDBWrapper serialization in the wire protocol). Workaround: mirror truth into a server-side atom updated after each Rama write, and expose it via `(e/server (e/watch !atom))`. Additionally, the proxy-callback must return `nil` (not the Missionary notifier result) or the wire-protocol serialization fails on ALL PState subscriptions.
- **Provenance**: `src/app/server/rama/util_fns.cljc:96-101` (the quarantine rationale: "per-key Rama subscription (foreign-proxy-async on global PStates) crashes Rama 1.6.0 (S40 quirk)"); `src/app/file_viewer.cljc:107-113` (docstring "foreign-proxy-async is broken in Rama 1.6.0 test IPC, so this uses e/watch on the atom instead"); `docs/history/progressive-summary.md:659-665` (sidebar PState + Electric bridge + proxy-callback-returns-nil fix); commit `c292169` ("Use atom mirror for sidebar truth (foreign-proxy-async broken in Rama 1.6.0)"); `MEMORY.md` "S40 Rama subscription -> server atom workaround"; `deps.edn:16` pins `com.rpl/rama {:mvn/version "1.6.0"}`.
- **Relied on in code**: `util_fns.cljc:122-134` (all mirror atoms: `!sidebar-truth-atom`, `!settings-truth-atom`, `!agent-trail-atom`, `!workspace-truth-atom`, `!flow-session-atom`, `!ingest-epoch-atom`); `file_viewer.cljc:107-190` (all `Watch*` bridges). The mirror atoms are formally quarantined (`util_fns.cljc:103-120` `transitional-mirror-quarantine`).
- **Verification**: **UNVERIFIABLE-cheaply** -- reproducing requires a live Rama 1.6.0 InProcessCluster with a global PState and a `foreign-proxy-async` root-path subscription (and the RocksDB wire path). A JVM-repro is possible but not "cheap" (needs `-Xss16m`, cluster boot, seconds each). Marked Rama-side, not pure-Missionary.
- **SUSPICION: platform-real, version-pinned** (specific to Rama 1.6.0; may not reproduce on other Rama versions -- but the version is pinned, so it is real for this repo).

### 15. Electric's `incseq/mount` contract is 5 named callbacks; `gpu-mount` implements it but is DORMANT (never wired)
- **Claim**: Electric's differential `incseq/mount` expects five callbacks -- `append-child`, `replace-child`, `insert-before`, `remove-child`, `nth-child` -- to bridge `e/for-by` diffs to a sink. `gpu-mount` builds exactly these to drive the GPU buffer pool, but nothing calls it: the live render path diffs pools manually instead.
- **Provenance**: `src/app/client/substrate/webgpu/buffer_pool.cljs:409-462` (`gpu-mount`, docstring names the 5 callbacks + the `incseq/mount` usage sketch at :429-430); `docs/history/progressive-summary.md:43` ("gpu-mount returns 5 callbacks matching Electric's incseq/mount contract").
- **Relied on in code / dormancy check**: **Dormant.** Grep finds `gpu-mount` only at its definition and inside its own docstring -- no external call site, no live `incseq/mount` require. The render loop instead calls `pool/ordered-diff-update-pool!`, `pool/keyed-diff-update-pool!`, `pool/batch-update-pool!` directly (`render.cljs:141-142,298,314,325`). So this is a built-but-unwired bridge.
- **Verification**: **primary-source** -- confirm the mount callback names/arities against `docs/electric/incseq.cljc` (the differential-dataflow / `e/for` implementation is in-repo). Optionally **compile-check** that `gpu-mount`'s shape satisfies `incseq/mount`.
- **SUSPICION: platform-real but unexercised** -- contract likely correct per upstream, but never run, so drift from the real `incseq/mount` arity/order would be undetected.

### 16. `trail-view-runtime` is a `defonce` delay that defers Rama-cluster boot to the first `/trail` pull (defonce-delay IPC boot bridge)
- **Claim**: The trail face's server runtime is a `(defonce trail-view-runtime (delay ...))`; the FIRST `/trail` Electric pull forces the delay, paying an in-process Rama IPC cluster boot (seconds), after which ingest streams asynchronously and epoch bumps refresh the face near-live. It reuses "the util-fns text-kernel delay precedent."
- **Provenance**: `src/app/file_viewer.cljc:145-172` (the `defonce`+`delay` with the OI-1 comment and async `future` ingest sweep); `:174` `(defn trail-rt [] @trail-view-runtime)`; consumed by `TrailBundle`/`TrailFeed`/`TrailConversation`/`TrailText` at `:176-187` inside `e/server`; `start-trail-view-runtime!` is real at `src/app/server/rama/trail_view.clj:672`.
- **Relied on in code**: All four `Trail*` `e/defn` bridges deref `(trail-rt)`; `electric_flow.cljc:492-508` drives them via `(e/watch !trail-request)`.
- **Verification**: **primary-source** -- Clojure `delay`/`defonce` semantics guarantee once-only forced boot; the async-ingest timing is a design choice, not a falsifiable platform claim.
- **SUSPICION: platform-real** (standard Clojure delay; the only risk is boot-cost/latency, not correctness).

### 17. Electric/Missionary is "continuous synchronous programming" (Van Roy taxonomy), NOT FRP
- **Claim**: On Van Roy's taxonomy the model is continuous + synchronous + demand-driven, not FRP: no `Behavior = Time -> a`, no accumulated event history; `m/watch` yields "current value of this atom" (not a function of time); when an atom changes, `m/latest` recomputes and the whole graph settles atomically before the next change is observed (synchronous hypothesis); flows are sampled on demand (pull).
- **Provenance**: `CLAUDE.md:19-27` (attributed to Dustin Getz, Electric's creator); echoed in `memory/core-reframes.md:213-241` ("Electric and Missionary Are Climate").
- **Relied on in code**: Foundational framing, not a single call site. The whole render path (sources -> `m/latest` chains -> `m/sample` on RAF -> GPU) is built as continuous-synchronous; the pull-model reducer (`render.cljs:121-558`) is the concrete expression.
- **Verification**: **primary-source** -- corroborate against Getz's public statements and Van Roy's "Programming Paradigms" taxonomy (web access available). The "atomic settle before next change" sub-claim is partially JVM-testable.
- **SUSPICION: platform-real** (classification claim; "precisely Van Roy category X" is the kind of mapping CLAUDE.md itself warns can be overstated under review -- treat the taxonomy label as approximate).

### 18. Missionary flows are not derefable with `@`
- **Claim**: A Missionary flow is not a reference type; `@flow` / `(deref flow)` does not yield its current value (only atoms/refs are derefable). You must sample/subscribe.
- **Provenance**: `memory/feedback_precision_over_validation.md:12` ("verify the primitives are correct (e.g., Missionary flows are not derefable with @)").
- **Relied on in code**: Implicit everywhere -- code always samples flows (`m/sample`, `m/reduce`, `m/latest`) and only `@`-derefs atoms (e.g. `@!focus` in eductions, `@!gpu-budget` in render). No code `@`-derefs a flow.
- **Verification**: **compile-check / JVM-repro** -- attempt `@(m/watch (atom 0))` vs `@(atom 0)` and observe the flow case does not return a value.
- **SUSPICION: platform-real** (basic Missionary type distinction).

### 19. Missionary flow composition gravitates toward a single `m/join` orchestration point (so the orchestrator file stays large)
- **Claim**: Because Missionary composes concurrent consumers under one `m/join`, the system tends toward a single orchestration site; the monolith/orchestrator file will always be "large-ish," and the refactor goal is to keep only genuine orchestration there, not trapped computation.
- **Provenance**: `memory/core-reframes.md:223` ("Missionary's flow composition gravitates toward a single m/join orchestration point").
- **Relied on in code**: Observably true of `src/app/client/workspace/runtime.cljs:363-395` -- one `(m/join vector ...)` is the single terminal orchestration site for the whole client.
- **Verification**: **UNVERIFIABLE-cheaply** -- this is an architectural tendency/opinion, not a falsifiable platform behavior; "gravitates" has no crisp failure condition.
- **SUSPICION: unknown (design-opinion)** -- real as an observation of THIS codebase, but stated as a general law it is closer to a heuristic than a platform fact.

### 20. The `:prod` shadow build is pre-broken by the v2/v3 namespace drift
- **Claim**: `src-prod/prod.cljc` names the v2 namespace `hyperfiddle.electric`, which the resolved v3-alpha-SNAPSHOT jar does not provide, so the `:prod` build does not compile as written; only the `:dev` build is runnable.
- **Provenance**: `src-prod/prod.cljc:9`; `deps.edn:3` (v3 pin); `shadow-cljs.edn:12-16` (`:prod` build has no Electric reload hook, unlike `:dev` at :11); `memory/implementation-quirks.md:166`.
- **Relied on in code**: The dev workflow (`clj -A:dev -X dev/-main`, `MEMORY.md` feedback_dev_workflow) is the only exercised boot path; `prod.cljc` is not compiled in normal work.
- **Verification**: **compile-check** -- attempt a `:prod` shadow release build and observe the unresolved `hyperfiddle.electric` namespace (or, if a v2 shim is on the classpath, its absence).
- **SUSPICION: platform-real / stale-version** -- almost certainly a v2->v3 migration leftover in `prod.cljc`; harmless while prod is unused, load-bearing the day someone ships.

---

## Surprises (flagged as observations, not verdicts)

1. **Doc-vs-doc contradiction on the m/ap+m/?< pattern.** `.claude/skills/reactive_master.md:26-27` and `docs/history/insights.md:129-137` ADVOCATE `m/ap` + `m/?<` as the way to derive continuous state -- the exact pattern `CLAUDE.md:31-51` later BANNED (Session 10) when fed to `m/latest`. `reactive_master.md` still tells the reader to "Use m/ap to define relationships." The two skills disagree and the older one was never reconciled.
2. **Claim 13 is contradicted by shipped code.** The recorded quirk "e/watch only works within a single peer -- no server->client pipe" is the opposite of what `file_viewer.cljc`'s six `(e/server (e/watch ...))` bridges do in production. A stale claim sitting un-retracted next to code that depends on the reverse.
3. **The `:prod` build cannot compile** as written (Claim 20). The project has run only on `:dev` -- an entire deployment path is dormant/broken.
4. **`electric-docs` SKILL.md teaches no behavioral claims and has stale paths** -- it points at `docs/electri_tutorial.txt` / `docs/missionary-complete-reference.txt`, neither of which exists (real files are `docs/reference/*.txt` + `docs/electric/*.cljc`). The skill is a lookup index, not a knowledge base.
5. **`gpu-mount` (Claim 15) is a fully-built, never-wired incseq bridge.** The Electric differential->GPU path was implemented (5 callbacks, Phase 6D) but the live render loop diffs pools by hand; the bridge has zero call sites.
6. **`src/global_flow.cljs` is ~90% dormant.** Only `await-promise` is imported (by `electric_flow.cljc:11`); the file still carries a live `m/ap`+`m/?<`+`(catch Cancelled ...)` `debounce` (`:10-15`), two `m/signal`/`m/latest` flows citing a 2023 Clojurians Slack thread (`:21-36`), and ~25 unused `defonce` atoms. It is a live namespace holding mostly fossil reactive code.
7. **`missionary` is unpinned in `deps.edn`.** Its version (`b.46`) is entirely at the mercy of whatever the moving `electric v3-alpha-SNAPSHOT` POM points at; a SNAPSHOT refresh could silently change the Missionary version under the project.
