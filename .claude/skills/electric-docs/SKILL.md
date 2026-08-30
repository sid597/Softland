---
name: electric-docs
description: Missionary reactive programming for Softland — the project's VERIFIED behavioral laws + recipes (regression-tested), plus Hyperfiddle Electric v3 source kept as REFERENCE ONLY for how to do things the reactive way in Missionary and Clojure. Electric is NOT a dependency (removed at the 2026-08-20 waist cut; Hyperfiddle left Clojure 2026-08 — v3 is a frozen proprietary alpha, v4 is JS + WASM). Use when writing or reviewing Missionary flows/tasks (m/ap, m/sp, m/?, m/seed, m/reduce, m/watch, m/signal, m/latest, m/sample, m/observe, m/relieve), the client host's courier and frame edge, "Watch cancelled" crashes, keyed diffs / incseq shape, reactive dataflow design — or when reading Electric-era code in git history (e/defn, e/server, e/client, e/for, e/diff-by, e/watch, electric-dom). Never as a template: Electric targets the DOM; Softland targets its own engine.
allowed-tools: Read, Grep, Glob
---

# Missionary & Electric — Verified Laws + Reference Index
*(Electric = reference only since 2026-08-30 — see §0)*

Rewritten 2026-07-05 from verified material only; amended 2026-07-11
(precedence note refreshed to post-A1–A3 reality, U4 rescoped to the
CONTRACT's plain-fn registry, coverage note added, U1–U3 compressed to
stubs — see AMENDMENTS.md dated entry). Drafted by an Opus stage-3 agent
from VERDICTS.md; fact-checked, amended, and signed by Fable — the
signature means the laws below were checked against the verdicts, not
just assembled from them. Sources of truth, in precedence order:
1. `docs/electric-skill/VERDICTS.md` — adjudicated verdicts + evidence
2. `test/app/missionary_claims_test.clj` — executable laws (cited below by test name)
3. `docs/electric-skill/HACKS-LEDGER.md` — provenance
4. `docs/electric-skill/PROBE-EVIDENCE.md` and
   `docs/render-north/PROBE-10K.md` — empirical, valid only within their stated fences

Anything below labeled LAW is PLATFORM-REAL per VERDICTS.md, with its exact
scope. Do not widen a law past its stated scope. Do not teach anything from
the UNVERIFIED section as law.

**Precedence vs CLAUDE.md:** CLAUDE.md is the BINDING project doc, and
since 2026-07-05 it AGREES with these laws — amendments A1–A3 (m/ap ban
rescoped to the nested fork per L1; "settles atomically" replaced with the
L8 diamond glitch; R3's measured sunset added) were applied same-day under
Sid's in-session extension (`build/electric-skill/AMENDMENTS.md`, Tier-1
note). CLAUDE.md carries the one-line versions; cite this skill when the
precise mechanism, scope fence, or evidence matters. Only A4 (CLAUDE.md
slimming) remains unapplied.

---

## 0 · Status (2026-08-30) — read this first

- **Electric is not a dependency.** It left the tree with the product
  client at the waist cut (`adc30c9`, 2026-08-20); `deps.edn` carries
  Missionary directly (`b.46`). No `src/` file requires `hyperfiddle.*`.
- **Hyperfiddle left Clojure** (Slack, read by Sid 2026-08-30): Electric v4
  is a JavaScript-embedded language with a low-level kernel compiled to
  WebAssembly and a portable WASM server runtime that reaches JVM backends;
  v3 (Clojure) is a frozen alpha under a proprietary license ("free for
  bootstrappers and non-commercial use, but is otherwise a commercial
  project" — README, checked 2026-08-30). Never propose re-adding it as a
  foundation; memory `reference-electric-v3-orphaned-v4-pivot`.
- **How to use this skill now** — Sid's ruling, verbatim (2026-08-30):
  "for existing electric i think they can be used as reference on how to do things reactive way in missionary and clojure so that is useful part imo … i think we have move past that we are going to copy electric blindly because its for dom ours is multi-engine softland target".
  So: the Missionary laws and recipes (§2 L1–L12, §3 R1–R3) BIND the
  client host's courier and frame edge (`docs/electric-native/CONTRACT.md`
  §3). The Electric layer (L13–L16, R4, U4–U5, the §5 source index) is
  REFERENCE — read it for the shape of keyed diffs, the item/applier split,
  demand-driven transfer; never copy an applier or a DOM-shaped contract
  (L15 is the receipt: the DOM mount broke on a GPU pool). Softland's
  target is its own store + GPU applier with one painter per family
  (decisions.md "The render seam": borrowed algebra, never borrowed
  appliers).
- Anything below that says "pinned build", "SNAPSHOT bump", or cites
  `src/app/...` Electric files describes the pre-cut tree; the laws stay
  true of Missionary `b.46`, and of Electric build `-45` as a reading aid.

---

## 1 · Versions (read this before trusting anything)

- **Electric**: NOT in `deps.edn` (removed `adc30c9`, 2026-08-20). The laws
  below were verified against `v3-alpha-20260519.115706-45` (claims tests)
  and `20260325.114002-44` (the render probe) — kept as the reference builds
  for reading Electric-era code in git history (`adc30c9^`). Pinned source
  copies live under `docs/electric/` (§5).
- **Missionary**: **`b.46`**, pinned DIRECTLY in `deps.edn` (no longer
  transitive). **After any Missionary bump, re-run
  `test/app/missionary_claims_test.clj`** — it exists to catch a bump
  changing these behaviors before the app does. Upstream current: `b.47`
  (EPL-2.0).
- **App namespace (history)**: the deleted client used v3 —
  `hyperfiddle.electric3`. `src-prod/prod.cljc` no longer requires any
  Electric namespace (the old v2/v3 prod-drift note is retired).
- Clojure 1.12.4, ClojureScript 1.11.132, shadow-cljs 2.28.23.

---

## 2 · LAWS (platform-real, each with exact scope + evidence)

### Missionary crash laws

**L1 — Multiple `m/?<` over `m/watch` nested in ONE `m/ap` crashes "Watch cancelled" on the first input change.**
Scope: the trigger is the **nested preemptive fork itself** — the outer
`m/?<` fork restart cancels the still-live inner `(m/watch ...)` fork, and
cancelling a live `m/watch` throws `missionary.Cancelled: "Watch cancelled."`.
It crashes **when consumed directly — `m/latest` is NOT the trigger**. Two
*separate* single-`m/?<` `m/ap` flows fed to `m/latest` are legal (no crash).
The sanctioned combiner remains `m/latest` over raw watches (Recipe R1).
Evidence: `claim-01-nested-ap-forks-crash-watch-cancelled`; VERDICTS Claim 1
shapes A (direct, crashes), B2 (separate single-?< m/aps, no crash).

**L2 — The same mechanism kills `m/ap`-shaped event filters, two ways.**
`(m/ap (let [e (m/?< >events) f (m/?< (m/watch !pred))] ...))`:
(a) every predicate-atom change spuriously re-runs the body (re-emitting
stale events / nil) — a non-event source treated as an event stream;
(b) the **next real event** restarts the outer fork, cancelling the nested
`m/watch` → "Watch cancelled" crash. Fix is Recipe R2 (`m/eduction` + `@`).
Evidence: `claim-02-eduction-filter-safe-ap-fork-crashes`; VERDICTS Claim 2.

**L3 — `m/join`: one failing branch cancels all siblings, and join fails with that branch's error.**
Scope: tasks and joined flow consumers alike. This is why one dying consumer
under a top-level `(m/join vector ...)` (e.g. `runtime.cljs`) surfaces as
"Watch cancelled" everywhere.
Evidence: `claim-12-join-cancels-siblings-on-failure`;
`docs/reference/missionary-reference.txt:783` (upstream-documented).

### `m/latest` semantics laws

**L4 — `m/latest` is demand-driven and memoizing: its combine fn re-runs ONLY when an input flow emits — never because `m/sample` sampled it.**
Measured: 5 sample ticks with idle inputs → 1 combine-fn call (the initial).
This is the platform basis for "never put side effects (especially RAF
scheduling) inside `m/latest`": a `request!()` inside the fn fires once and
never again while inputs are idle, so a conditional RAF loop dies. See
Recipe R3 for the sanctioned pattern and its measured sunset.
Evidence: `claim-03-latest-not-rerun-on-sample-trigger`; VERDICTS Claim 3.

**L5 — `m/latest` arity mismatch throws a LOUD `clojure.lang.ArityException` on first emission. It is NOT silent corruption.**
Scope: fn-arity ≠ flow-count in either direction crashes the flow the moment
the combine fn is first invoked. Values never silently bind to wrong params.
(Match counts anyway — but the failure mode is a crash, not miswiring. The
old "silent corruption" wording was an agent error.)
Evidence: `claim-06-latest-arity-mismatch-throws-arityexception`; VERDICTS Claim 6.

**L6 — Coarse invalidation: a single `m/latest` over N watches re-runs its ENTIRE combine fn on ANY one input's change**, including branches that never
read the changed atom. Wide combine fns pay full cost on every blink-grade
tick; split flows so each branch watches only what it reads.
Evidence: `claim-08-latest-coarse-invalidation`; VERDICTS Claim 8.

**L7 — The hidden edge is the untracked `@`-read, not an invisible side effect.**
Scope: an atom read via `@` inside a combine fn but NOT declared as a flow
input is **untracked** — changing it does not re-run the fn; the read stays
stale until a *declared* input changes. Conversely, a `reset!` performed
inside a combine fn IS visible to that atom's other watchers — the side
effect is not hidden; the flow's *dependency* on it is. Keep derivations
pure; declare every dependency as a flow input.
Evidence: `claim-09-hidden-edge-deref-not-declared-input`; VERDICTS Claim 9.

**L8 — Raw `m/latest` diamonds GLITCH, deterministically. There is no atomic settle at the Missionary layer.**
Scope: two flows derived from one source, combined downstream, emit
inconsistent intermediate pairs (e.g. `[b-from-new-a, c-from-old-a]`) before
settling — reproduced 4/4 runs. The final settled value is consistent; the
transients are not. Consistency of co-varying values requires deriving them
in a **single** `m/latest` over the shared source:
`(m/latest (fn [a] [(inc a) (dec a)]) (m/watch !a))` — never two `m/latest`s
combined. **Fence: this is the raw-Missionary layer; whether Electric v3's
compiled DAG is glitch-free is untested.** The "graph settles atomically"
claim in older docs is falsified at this layer.
Evidence: VERDICTS Claim 17 probe transcript (falsified sub-claim; not in
the suite — kept verbatim in VERDICTS).

### Flow-primitive laws

**L9 — `m/sample`: every SAMPLED input must be initially ready; the discrete trigger goes LAST.**
Scope: the exact error `Undefined continuous flow.` is triggered by a
**not-initially-ready flow in a sampled slot** (e.g. `m/observe` — never
initially ready), not by argument order per se: an initially-ready flow like
`m/seed` in a sampled slot does not throw. In practice "continuous first,
trigger last" is the correct habit because triggers are usually `m/observe`
shaped (RAF).
Evidence: `claim-11-sample-requires-initially-ready-sampled-inputs`; VERDICTS
Claim 11; `docs/reference/missionary-reference.txt:875`.

**L10 — Flows are not derefable.** `@(m/watch !a)` throws (a flow is a
function, not IDeref). Sample/subscribe flows; `@`-deref only atoms/refs.
Evidence: `claim-18-flows-not-derefable-with-deref`.

**L11 — `m/observe` applies real backpressure: with a consumer that has not transferred, the SECOND producer push BLOCKS the producer. `m/relieve` coalesces so the producer never blocks.**
This is why every DOM source (keyboard/wheel/resize/mouse) must be wrapped in
`m/relieve` — you cannot block the DOM event thread. **Fence:** the hazard
and mitigation are verified; the vivid "a raw mousedown drains a stale
buffered mouseup first" ordering was a live-race diagnosis, NOT
deterministically reproduced — treat it as a manifestation of this hazard,
not an independent law.
Evidence: VERDICTS Claim 7 probe transcript (timing-based; not in the suite).

**L12 — `m/observe` and `m/ap` flows ARE re-subscribable** (correction of an
old false belief). Each subscription independently re-runs the observe setup
fn; two subscribers both receive values. The genuine single-subscription /
shared-process property belongs to **`m/signal` / `m/stream`** (memoized,
multicast). Factory fns (`make-raf-flow` etc.) are still good practice — but
their value is isolating per-subscription mutable state, not avoiding a
subscription failure.
Evidence: VERDICTS Claim 10 probe transcript (falsification; not in the suite).

### Electric-layer laws

**L13 — `try` in ANY form inside `e/defn` throws `"try is TODO"` at macroexpansion — CONFIRMED CURRENT on build -45, not stale.**
Scope: `(catch :default ...)`, `(catch Throwable ...)`, and `finally`-only
all fail. Error handling must live outside the Electric reactive context
(plain fns called from Electric).
Evidence: VERDICTS Claim 5 compile-check (control `e/defn` compiled OK; all
three try forms threw `clojure.lang.ExceptionInfo: try is TODO`).

**L14 — `e/watch` = `e/input` over `m/watch`, and it DOES transfer server→client.**
`(e/server (e/watch !atom))` yields a reactive Electric value consumed on the
client — this is the shipped Rama-truth bridge (`file_viewer.cljc:107-190`
`Watch*` defns → `electric_flow.cljc:484-489` client resets). Scope of the
old confusion: a **raw Missionary flow** cannot cross peers; **Electric-managed
site transfer of an `e/watch` value can and does.** The recorded quirk
"e/watch only works within a single peer / use HTTP for data" is retracted.
Evidence: VERDICTS Claim 13 — jar source `hyperfiddle/electric3.cljc:157-163`
(watch = `(input (m/watch ...))`) + shipped, running bridges.

**L15 — The incseq mount contract is `hyperfiddle.incseq.mount-impl/mount` — a POSITIONAL 5-arg fn `(mount append-child replace-child insert-before remove-child nth-child)` — and `insert-before` means MOVE an existing child.**
There is **no public `hyperfiddle.incseq/mount` var** (that name in older
docstrings is wrong). During `:permutation` rotations the mount algorithm
passes existing **child handles** back through `insert-before` (DOM
`insertBefore` = move); during append it passes new items. A callback that
**allocates on insert therefore corrupts state on ANY `:permutation`** — the
dormant `gpu-mount` bridge, driven this way, ended a 91-frame n=100 rotate
with 16,750 active slots for 100 entities while the diff algebra stayed
correct. The DOM-shaped mount contract is NOT reusable for a slot pool whose
"move" is an indirection update: consume the six diff ops directly instead
(the C2 store shape). Do not wire `gpu-mount` as-is.
Evidence: VERDICTS Claim 15 (jar source `hyperfiddle/incseq/mount_impl.cljc:7`);
PROBE-EVIDENCE obs. #8; PROBE-10K §4.

**L16 — `e/diff-by` / `e/for-by` are `->seq-differ` end to end, and diff cost is bound by DIFF SHAPE, not element count.**
Measured (raw incseq layer, JVM + browser, 10²–10⁴):
- change / append / tail-shrink stay **linear** (~0.2µs/row re-scan floor;
  2–5ms per update at 10⁴ server-side; apply ≤4.2ms p95 at 60fps). There is
  **no cheap no-change path**: an unchanged 10⁴ collection still costs ~2ms
  to re-diff.
- **any reorder** (rotate, front-drop windowing, ID churn) enters the
  permutation regime: ~×130–700 per decade, ~0.1s at 10³, 12–74s per mint at
  10⁴ — unusable, and the cost is at the **producer** (diff algebra), so
  swapping transport/consumer does not save it. Front-drop windowing IS a
  reorder (full-shift `:permutation`); append-only is not.
- Consequence: **order/position must be derived data on rows** (a sort key
  applied at projection time), never a sequence-position permutation diffed
  through the incseq.
**Fence: measured on the raw incseq protocol (real differ, real patch
algebra, real pool) — no Electric DAG overhead, no websocket, no
`m/latest`/`m/ap` flows were run.** Do not credit these numbers past that.
Second fence: these measurements ran on snapshot build **-44** (PROBE-10K's
producer cite), while every claims regression ran **-45** — the law's
direction is corroborated across both, but the exact numbers belong to -44;
re-measure after any snapshot bump.
Evidence: PROBE-EVIDENCE obs. #1–6, #9; PROBE-10K §2, §6 (source identity:
jar `electric3.cljc:159,243`).

---

## 3 · VERIFIED RECIPES (the good patterns, with why)

**R1 — Combine watches with `m/latest`, never nested `m/?<` forks.**
```clojure
(defn <derived [!a !b !c]
  (m/latest (fn [a b c] (compute a b c))
            (m/watch !a) (m/watch !b) (m/watch !c)))
```
Why: L1 — the nested-fork shape crashes on first change; `m/latest` over raw
watches recomputes without cancellation. Match fn arity to flow count (L5 —
mismatch is a loud ArityException). Mind L6 (whole fn re-runs on any input)
and L8 (co-varying values from ONE `m/latest`, or diamonds glitch).
Evidence: `claim-01…` GOOD shape; live in `runtime/render.cljs` `<world-snapshot`.

**R2 — Filter discrete events with `m/eduction` + `@deref`, never `m/ap` + `m/?<` over a watch.**
```clojure
(defn <editor-keys [>keyboard !focus]
  (->> >keyboard
       (m/eduction (filter (fn [e] (= @!focus :editor))))))
```
Why: L2 — the `m/ap` shape spuriously re-emits on predicate change and
crashes on the next event. The eduction reads the predicate atom at filter
time (an intentional untracked read — the filter should NOT re-run on focus
change), no forks, no cancellation.
Evidence: `claim-02…` GOOD shape; live in `workspace/events.cljs` focus routers.

**R3 — Unconditional RAF + `identical?` skip at the consumer; keep `m/latest` pure.**
```clojure
(let [>raf   (make-raf-flow)                     ; self-sustaining, always ticks
      <world (m/latest (fn [a b c] {:a a :b b :c c})   ; PURE — no request!() inside
                       (m/watch !a) (m/watch !b) (m/watch !c))]
  (m/reduce (fn [prev [world _t]]
              (if (identical? world (:prev prev))
                prev                              ; O(1) skip on idle frames
                (do (draw! world) {:prev world})))
            init
            (m/sample vector <world >raf)))
```
Why: L4 — `m/latest` re-runs only on input emission, so RAF scheduling inside
the combine fn fires once and the loop dies. Unconditional RAF costs one
pointer compare per 16ms; the O(1)-idle discipline is measured (idle frames
0.0–0.1ms across 149-frame stretches at 10⁴ — PROBE-10K §3).
**Measured sunset note (2026-07-05):** the rule's pre-registered sunset —
"dirty-present requires Electric diffs as the change signal (Gap 3)" — now
has measured support: a real incseq diff stream IS a workable change signal
at 10⁴ for **change / append / tail-shrink** shapes (mint 2–5ms server-side,
apply ≤4.2ms p95, 60fps), **with reorder-shaped diffs (large `:permutation`)
barred as the carrier** (producer knee at 10³ — L16). Until that signal is
wired, this recipe remains the correct pattern.
Evidence: `claim-03…`; PROBE-EVIDENCE §3; PROBE-10K §6. Note L9: `>raf` is
`m/observe`-shaped (not initially ready) so it must be the trigger slot,
never a sampled slot.

**R4 — Server-truth to client: mirror atom + `(e/server (e/watch !atom))`.**
Server code writes truth into a server-side atom after each authoritative
write; the client consumes `(e/server (e/watch !atom))` as a reactive value.
Why: L14 — `e/watch` is `e/input` over `m/watch` and transfers server→client;
this is the shipped Rama→UI bridge (six `Watch*` defns in
`file_viewer.cljc`). Caveat: the *reason* the mirror exists (the Rama 1.6.0
`foreign-proxy-async` root-path crash) is UNVERIFIED-here — see U2 below.
The bridge itself is verified shipped behavior regardless.
Evidence: VERDICTS Claim 13; `file_viewer.cljc:107-190`; `electric_flow.cljc:484-489`.

---

## 4 · UNVERIFIED (honest section — never teach these as law)

**U1 (ledger Claim 4) — "Rapid input changes can STARVE `m/latest` so its fn never runs and no RAF is scheduled."**
UNVERIFIED — no deterministic repro; may just restate L4. Would settle:
instrumented in-browser capture under real input flooding showing the
combine fn not completing with no RAF issued. R3 already covers the
practical consequence. Detail: VERDICTS Claim 4.

**U2 (ledger Claim 14) — "`foreign-proxy-async` on global PStates crashes Rama 1.6.0; proxy-callback must return nil."**
UNVERIFIED here — provenance strong (commit `c292169`, S40; rationale in
`util_fns.cljc:96-134`) but not re-probed; needs a live 1.6.0
InProcessCluster on the RocksDB wire path. Rama is version-pinned, so the
R4 workaround stays correct either way. Detail: VERDICTS Claim 14.

**U3 (ledger Claim 19) — "Missionary composition gravitates toward a single `m/join` orchestration point."**
Design-opinion, true of THIS codebase (`runtime.cljs` has one terminal
`m/join`); no crisp failure condition. A heuristic for orchestrator-file
discussions, never a platform fact. Detail: VERDICTS Claim 19.

**U4 (Slack, recorded 2026-07-10) — "e/fn values may be returned in plain data
structures (maps/vectors) from an `e/defn` and dispatched at runtime; e/fns
are values — concretely pointers/ids into the common program DAG."**
Status: UPSTREAM-CONFIRMED, not verified on our pinned build. Source: Electric
Slack `#hyperfiddle` (noonian + Dustin Getz) — verbatim archive at
`docs/electric-skill/SLACK-THREAD-efn-values.md`.
Dustin: "e/fns are values… concretely pointers/ids (serializable, you can
println them) and these pointers can move between sites. The pointers are
interpreted in the context of the common program dag… Either site can boot a
e/fn." noonian reproduced the shape: an `e/defn` returning
`{:Identity (e/fn …) :SignIn! (e/fn …) :SignOut! (e/fn …)}` works; his
original failure was a concurrency bug elsewhere, not the data-structure
return.
Consequences IF it verifies here: (a) a primitive **registry**
`{keyword → e/fn}` with runtime dispatch is sanctioned Electric.
**Currently NOT load-bearing (rescoped 2026-07-11):** the landed v0
assembly interpreter chose PLAIN builder fns — a plain map
`{prim-kw → fn}` over rect-tree, persisted as keyword + code address,
never fn values (framework CONTRACT §6 + trap T5; Step-0 PROBE ran this
shape green). U4 becomes load-bearing only if primitives later become
e/fns dispatched inside the Electric DAG — write the settle-test BEFORE
any lane leans on that; (b) the pointers are meaningful only inside a
running session's program DAG — "serializable" means wire-level between the
two sites, NOT durable: **persist keywords and resolve through the registry;
never persist e/fn values** (a pointer would not survive a recompile);
(c) diagnostic smell from the thread: an e/fn invoked as a plain Clojure fn
compiles fine and fails at runtime with an arity error.
Would settle it: claims-test on the pinned build — e/defn returning a map of
e/fns, keyword dispatch, call through Electric application; PLUS the open
question the thread does NOT answer: **recursive self-application for nested
trees** (interpreter recursion depth — historically Electric's sharp edge).

**U5 (Slack, recorded 2026-07-10) — "Electric v3 site resolution is DYNAMIC,
not lexical: an e/fn body without site markers evaluates on whichever site
boots it."**
Status: UPSTREAM-CONFIRMED (Dustin Getz, same thread as U4), not verified on
our pinned build. Verbatim: "under the electric v3 rule of dynamic site
resolution (NOT lexical), the body will be evaluated on the site that boots
the fn" — plus: an e/fn contains both client and server regions, and the
non-booting side's regions pay latency relative to the booter.
Consequences IF it verifies here: every registry primitive must carry its
site explicitly INSIDE its own body (`e/client` for DOM/GPU-facing
primitives), or siting becomes an accident of the interpreter's boot site;
unsited closures capturing unserializable references (atoms) are a foot-gun —
they only work when booted on the owning site (Dustin's recommendation in the
thread: make the site explicit inside each e/fn).
Would settle it: claims-test — boot one unsited e/fn from a client context
and from a server context, observe evaluation site via platform-conditional
effect; confirm an inner `e/client` pins it regardless of booter.

---

## 5 · Reference index (lookup — link, don't duplicate)

**Coverage note (honest):** the LAWS above are Missionary-deep but
Electric-thin (L13–L16 only). For e/Token, forms, virtual scroll, e/for-by
edge behavior, and transfer semantics beyond L14 there are NO verified laws
yet — read the source below and treat any behavioral conclusion you draw
as UNVERIFIED until probed. Candidate next probes, in value order: e/Token
lifecycle; transfer-boundary serialization failures; the
e/fn-invoked-as-plain-Clojure-fn runtime arity smell (U4 consequence (c)).
(2026-08-30: Electric is out of the tree and orphaned upstream — these
Electric probes are retired; the live probe list is
`docs/electric-native/PROBLEM-SPACE.md` "Probes owed".)

Upstream copies of Electric/Missionary source and tutorials live in-repo.
Grep within the file for the symbol you need.

| Need | Read this file |
|------|----------------|
| e/defn, e/fn, e/server, e/client, e/for, e/for-by, e/diff-by, e/watch, e/input, e/amb, e/Token, e/Offload, e/snapshot, e/on-unmount, e/boot-* | `docs/electric/electric3.cljc` |
| dom/div, dom/text, dom/On, dom/On-all, dom/props, dom/node, Focused?, HTML elements | `docs/electric/electric_dom3.cljc` |
| DOM attributes/classes/styles internals | `docs/electric/electric_dom3_props.cljc`, `docs/electric/electric_dom3_events.cljc` |
| Input!, Checkbox!, Form!, Button!, TxButton!, validation | `docs/electric/electric_forms5.cljc` |
| Virtual scroll: Window, Raster, Tape, Scroll-window | `docs/electric/electric_scroll0.cljc` |
| SVG elements | `docs/electric/electric_svg3.cljc` |
| Scoped CSS, keyframes | `docs/electric/electric_css3.cljc` |
| e/Token internals | `docs/electric/electric_tokens.cljc` |
| incseq diff algebra, e/for implementation, patch-vec | `docs/electric/incseq.cljc` |
| Debug/tracing | `docs/electric/debug3.cljc`, `docs/electric/contrib/trace3.cljc` |
| mix, throttle, poll-task, delay-flow | `docs/electric/contrib/missionary_contrib.cljc` |
| Electric tutorial (Two Clocks, dom/On inputs, e/Token, typeahead, virtual scroll, transfer) | `docs/reference/electric-tutorial.txt` |
| Missionary API: m/ap, m/sp, m/?, m/?>, m/?<, m/seed, m/reduce, m/eduction, m/zip, m/latest, m/sample, m/watch, m/signal, m/cp, m/dfv, m/mbx, m/sem, m/relieve | `docs/reference/missionary-reference.txt` |
| Electric codebase walkthrough | `docs/reference/electric-codebase.txt` |

(Older skill versions pointed at `docs/electri_tutorial.txt` and
`docs/missionary-complete-reference.txt` — those files do not exist; the
paths above are the real ones.)

Project evidence files (this skill's own basis):
- `docs/electric-skill/VERDICTS.md` — full verdicts, probe transcripts, proposed doc amendments
- `test/app/missionary_claims_test.clj` — 9 green regression tests (claims 1, 2, 3, 6, 8, 9, 11, 12, 18)
- `docs/electric-skill/AMENDMENTS.md` — pending old→new corrections for other docs
- `docs/render-north/PROBE-10K.md` — incseq scaling numbers and their fences
- `docs/electric-skill/SLACK-THREAD-efn-values.md` — verbatim upstream thread behind U4/U5 (e/fn values in data structures; v3 dynamic siting)
