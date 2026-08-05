# Electric / Missionary Claim VERDICTS — Stage 2 adjudication

Verification of the 20 behavioral claims harvested in `HACKS-LEDGER.md`. Each
entry gives a **verdict** (PLATFORM-REAL / AGENT-ERROR / STALE-VERSION /
UNVERIFIED), the **evidence** (regression-test name, jar cite, doc URL, or probe
transcript), a **confidence**, and — where the verdict contradicts a repo doc — a
**PROPOSED-AMENDMENT** naming the doc and the correction. Proposals only; Fable
and Sid decide.

**This is a VERDICT layer, not new law.** Nothing here amends a doc; the
PROPOSED-AMENDMENT lines are candidates for the skill rewrite.

## Method & environment

- **Versions actually run** (confirmed): Electric `v3-alpha-SNAPSHOT` jar =
  `electric-v3-alpha-20260519.115706-45` (SNAPSHOT jar md5-identical to the -45
  timestamped jar); Missionary **`b.46`** (the -45 POM's `<missionary>` dep,
  confirmed by reading the jar POM); Clojure 1.12.4; ClojureScript 1.11.132.
- **JVM-repro (9 claims: 1,2,3,6,7,8,9,10,11)** — pure-Missionary probes, no DOM,
  no Electric runtime, run under `clojure -M:test`. The VERIFIED, deterministic
  ones are permanent regression tests in `test/app/missionary_claims_test.clj`
  (named `claim-NN-…`). Failed-to-reproduce / falsified / timing-only probes are
  reproduced here verbatim as code blocks, NOT in the suite (which must stay
  green).
- **primary-source (5: 12,13,15,16,17)** — read from the resolved jar in `~/.m2`
  (`unzip`), the in-repo reference docs, the shipped source, and one web search.
- **compile-check (3: 5,18,20)** — minimal `eval` against the project classpath;
  exact error text captured (root cause unwrapped).
- **UNVERIFIABLE-cheaply (3: 4,14,19)** — not attempted; why + what would settle.

### Sibling evidence (from the parallel render-bench window)

- **`../render-north/PROBE-10K.md`** — verification-grade numbers/method: Electric
  v3's real incseq protocol driven at 10²/10³/10⁴. Cited where a claim touches
  incseq/diff scaling, differ cost, or Electric-vs-JVM performance (Claim 15).
- **`./PROBE-EVIDENCE.md`** — 12 numbered first-hand observations (11
  VERIFIED-HERE, 1 OBSERVED-ONCE). Its **explicit fence**: the render probe did
  NOT run `e/defn`/the Electric DAG, no websocket transport, and **no
  `m/latest`/`m/ap`/`m/eduction` flows**. So it CANNOT corroborate the
  pure-Missionary crash/fork claims (1, 2, 3-mechanism, 6, 8, 9), try/catch
  (5), server→client transfer (13), or the paradigm claim (17) — those rest on
  MY probes / source reads. PROBE-EVIDENCE is verification-grade for the
  **consumer/diff side**: the incseq mount contract (Claim 15, obs. #8), that
  `e/diff-by`/`e/for-by` are `->seq-differ` (obs. #9), and the RAF rule's
  architectural half + its Gap-3 sunset (§3). Not over-credited beyond that.

---

## Claims

### Claim 1 — m/ap + multiple nested m/?< over m/watch crashes "Watch cancelled"

**Verdict: PLATFORM-REAL** (with a mechanism correction). **Confidence: HIGH.**

**Evidence:** regression `claim-01-nested-ap-forks-crash-watch-cancelled`, plus
probe transcript (build -45, Missionary b.46):

```
A) single m/ap + 2 nested m/?< (consumed DIRECTLY, no m/latest):
   {:outcome [:crash "missionary.Cancelled: Watch cancelled."], :seen [[0 0]]}
B) m/latest(identity, <that same m/ap>):
   {:outcome [:crash "missionary.Cancelled: Watch cancelled."], :seen [[0 0]]}
B2) m/latest(vector, m/ap(?<a), m/ap(?<b))  ; two SEPARATE single-?< m/aps:
   {:outcome :still-running, :seen [[0 0] [1 0] [1 2] [3 2]]}   ; NO crash
GOOD) m/latest(vector, m/watch a, m/watch b):
   {:outcome :still-running, :seen [[0 0] [1 0] [1 2] [3 2]]}   ; NO crash
```

The crash is real and reproduces on the first input change. **Correction to the
mechanism:** the trigger is **multiple nested `m/?<` inside ONE `m/ap`** over
watches — it crashes even when consumed *directly*, with **no `m/latest`
involved** (shape A). Two *separate* single-`m/?<` `m/ap`s fed to `m/latest`
(shape B2) do **not** crash. So the ledger's framing ("that is then passed as an
input to m/latest … m/latest propagates that cancellation") mislocates the cause:
the outer `m/?<` cancels the *inner* `(m/watch !b)` fork, and cancelling a live
`m/watch` surfaces `missionary.Cancelled: "Watch cancelled."`. CLAUDE.md's rule
title ("NEVER use m/ap with multiple m/?< inside flows fed to m/latest") is
directionally right but the "fed to m/latest" qualifier is not load-bearing.

**PROPOSED-AMENDMENT (CLAUDE.md:29–51 / the skill):** restate the ban as *"never
nest multiple `m/?<` over `m/watch` inside a single `m/ap`"* — the crash is
intrinsic to the nested preemptive fork, independent of any downstream
`m/latest`. Keep `m/latest`-of-watches as the sanctioned combiner.

**Doc-vs-doc contradiction #1 (flagged):** `reactive_master.md:26-27` ADVOCATES
`m/ap` + `m/?<` "to define relationships" — the exact pattern this claim shows
crashes over watches. The two skills disagree; this verdict resolves it in favor
of the ban. **PROPOSED-AMENDMENT (`.claude/skills/reactive_master.md:26-27`):**
retract/qualify "Use m/ap to define relationships" for `m/watch`-derived state;
route to `m/latest`. (Proposal only — I do not own that file.)

### Claim 2 — discrete event filtering must use m/eduction + @deref, not m/ap + m/?<

**Verdict: PLATFORM-REAL** (stronger than stated). **Confidence: HIGH.**

**Evidence:** regression `claim-02-eduction-filter-safe-ap-fork-crashes`:

```
BAD  m/ap + m/?<(>events) + m/?<(m/watch !focus)
   sequence: push :e1 ; focus→:other ; focus→:editor ; push :e2
   {:outcome [:crash "missionary.Cancelled: Watch cancelled."], :seen [:e1 nil :e1]}
GOOD (->> >events (m/eduction (filter #(= @!focus :editor)))):
   {:outcome :still-running, :seen [:e1 :e2]}
```

The bad shape fails two ways. **(a) It spuriously reacts to focus changes:** each
`(reset! !focus …)` re-runs the body via the inner `m/?<`, emitting `nil` (when
`focus≠:editor`) then re-emitting the stale `:e1` — treating a non-event source as
an event stream. Those focus re-emissions are themselves clean. **(b) The crash
comes from a NEW EVENT:** pushing `:e2` restarts the OUTER `(m/?< >events)` fork,
which cancels the still-live *nested* inner `(m/?< (m/watch !focus))` → "Watch
cancelled" — the identical mechanism as Claim 1 (an outer preemptive fork
cancelling a nested `m/watch`), not the focus change itself. The eduction shape
reads `@!focus` at filter time, ignores focus changes entirely, and passes
`[:e1 :e2]` cleanly with no crash. Matches the shipped `events.cljs:175-219`
routers.

### Claim 3 — side effect (RAF request!) in m/latest → identical? skip kills the RAF loop

**Verdict: PLATFORM-REAL** (mechanism verified; the "dies permanently" strength
is the correct consequence). **Confidence: HIGH.**

**Evidence:** regression `claim-03-latest-not-rerun-on-sample-trigger`:

```
combine-fn calls over 5 m/sample triggers, 0 input changes: 1   (NOT 5)
combine-fn calls after 2 input changes:                     3   (initial + 2)
```

The load-bearing platform fact: **`m/latest` is demand-driven and memoizing — it
re-runs its combine fn only when one of its INPUT flows emits, NOT when
`m/sample` samples it on a trigger tick.** So a `request!()` living inside the
combine fn fires exactly once (the initial run) and then never again while the
watched atoms are idle → the RAF loop is not re-scheduled → it dies. This is the
precise basis for CLAUDE.md's "NEVER put side effects inside m/latest (especially
RAF scheduling)" rule; the fix (unconditional self-sustaining RAF + `identical?`
skip at the reducer) is correct.

**Cross-window corroboration of the FIX (not the mechanism):** `PROBE-EVIDENCE.md
§3` CONFIRMS the rule's architectural half — applying diffs only at the consumer
edge held idle frames at 0.0–0.1ms across 149-frame idle stretches at 10⁴
(`PROBE-10K §3`); the O(1)-idle discipline survives a diff-consuming store.

**PROPOSED-AMENDMENT (CLAUDE.md:70–99, the RAF rule's "Rule:" line) — sunset
path.** The rule already pre-registers its own sunset ("dirty-present requires
Electric diffs as the change signal — Gap 3"). `PROBE-EVIDENCE.md §3` +
`PROBE-10K §6` now give that sunset measured support: a real incseq diff stream
is a workable change signal at 10⁴ for **change / append / tail-shrink** shapes
(mint 2–5ms server-side, apply ≤4.2ms p95, 60fps) — **with a new boundary that
reorder-shaped diffs (large `:permutation`) must never be the change-signal
carrier** (they knee at the producer at 10³; see Claim 15 and NORTH §9). Record
this as the concrete condition under which conditional/dirty RAF becomes legal.
Note (per the render-bench window): *no CLAUDE.md claim was falsified by the
render probe* — the one relocation (permutation trouble is at the producer, not
the consumer) belongs to **NORTH §9's trap ledger, not CLAUDE.md**.

### Claim 4 — rapid input changes can STARVE m/latest so no RAF is scheduled

**Verdict: UNVERIFIED.** **Confidence: n/a.**

Not attempted — a starvation/ordering race. A deterministic JVM repro would need
to force "derived `m/latest` restarts before it settles," which requires
controlling the Missionary reactor's scheduling; a stress harness would be flaky
and non-diagnostic. Neither render-bench doc drove `m/latest`, so no help there.
**What would settle it:** an instrumented in-browser capture under real input
flooding (resize-drag) showing the combine fn failing to complete a run and no
`requestAnimationFrame` issued; or a controlled-scheduler harness. It is stated
once in CLAUDE.md (:97) and may be a restatement of Claim 3 rather than an
independent phenomenon.

### Claim 5 — try/catch inside e/defn throws "try is TODO"

**Verdict: PLATFORM-REAL — and NOT stale** (the stale-version suspicion is
REFUTED). **Confidence: HIGH.**

**Evidence:** compile-check against the current -45 build (root cause unwrapped):

```
control  (e/defn OkProbe [] 1)                          => COMPILED-OK
(e/defn P [] (try 1 (catch :default _ 2)))   => clojure.lang.ExceptionInfo: try is TODO
(e/defn P [] (try 1 (catch Throwable _ 2)))  => clojure.lang.ExceptionInfo: try is TODO
(e/defn P [] (try 1 (finally 2)))            => clojure.lang.ExceptionInfo: try is TODO
```

`try` in ANY form (with `:default`, with a class, or `finally`-only) fails at
`e/defn` macroexpansion with the exact recorded message **`try is TODO`** on the
CURRENT pinned build. The ledger's "possibly stale-version" suspicion is refuted:
the ban is live on -45, not an artifact of an older build. Error handling must
stay outside the Electric reactive context, as CLAUDE.md:101-104 says. (Contrast
`PROBE-EVIDENCE.md` fence: it did NOT run `e/defn`, so this rests on the
compile-check alone.)

### Claim 6 — m/latest arg count must equal fn arity, else "silent corruption (no error)"

**Verdict: AGENT-ERROR** (on the failure-mode wording). **Confidence: HIGH.**

**Evidence:** regression `claim-06-latest-arity-mismatch-throws-arityexception`:

```
fn arity 3, 2 flows: {:outcome [:crash "clojure.lang.ArityException: Wrong number of args (2) ..."]}
fn arity 2, 3 flows: {:outcome [:crash "clojure.lang.ArityException: Wrong number of args (3) ..."]}
fn arity 2, 2 flows (control): {:outcome :still-running, :seen [{:a :A, :b :B}]}
```

The claim's distinctive assertion — *"silent corruption … with no thrown
error"* — is **false**. An arity mismatch throws a **loud
`clojure.lang.ArityException`** the moment the combine fn is first invoked (i.e.
on the first value). Values do NOT silently bind to the wrong params. The
operational advice ("match the flow count to the fn arity") stands, but for the
opposite reason: you get a crash, not silent miswiring.

**PROPOSED-AMENDMENT (`memory/implementation-quirks.md:12-16`):** replace "silent
corruption (no error)" with "a loud `ArityException` on first emission." (The
long-arity `m/latest` blocks in the render path are safe because arity *matches*,
not because mismatch would be silent.)

### Claim 7 — m/observe without m/relieve buffers one event / stale-first delivery

**Verdict: PLATFORM-REAL for the backpressure hazard; the exact "stale-first
buffered-value-drains-first" ordering is UNVERIFIED (not reproduced by a
synchronous probe).** **Confidence: MEDIUM-HIGH (hazard); LOW (exact wording).**
Not in the suite (the clean demonstration is timing-based).

**Evidence (probe transcript, build -45):**

```
(a) eager m/reduce consumer, push A then B synchronously:
    {:outcome :still-running, :seen [:A :B]}          ; kept up, in order, no loss
(b) NO relieve, LAGGING consumer (never transfers), push A then B:
    pushA=:ok  pushB=:BLOCKED  notify-count=1          ; 2nd push BLOCKS the producer
(c) m/relieve keep-latest, LAGGING consumer, push A then B:
    pushA=:ok  pushB=:ok       notify-count=1          ; relieve absorbs the burst
```

What reproduces: `m/observe` applies backpressure — with a consumer that hasn't
transferred, the **second producer push blocks** (shape b); `m/relieve` coalesces
the burst so the producer never blocks (shape c). This is exactly why the
codebase wraps DOM sources (keyboard/wheel/resize/mouse) in `m/relieve` — you
cannot block the DOM event thread. What did NOT reproduce with a synchronous
probe is the *specific* "a raw mousedown triggers processing of a previously
buffered, stale mouseup" ordering — that was a live diagnosis
(`add-watch`+`console.trace`, `progressive-summary.md:414-419`) and is a
concurrent-timing manifestation of the same root, not deterministically
constructible here. **What would settle the exact ordering:** a controlled
two-consumer harness that lags by exactly one transfer, or the original live
trace. The hazard and the `m/relieve` mitigation are real; the vivid "stale-first"
story is the same hazard seen through a specific race.

### Claim 8 — coarse invalidation: one m/latest over N atoms re-runs the whole fn on any change

**Verdict: PLATFORM-REAL.** **Confidence: HIGH.**

**Evidence:** regression `claim-08-latest-coarse-invalidation`:

```
runs after: initial + change !a + change !c   (!b never touched)  => 3
```

A single `m/latest` over three watches re-runs its entire combine fn on *every*
input change, including branches that don't read the changed atom. This is the
direct basis for the flow-split work and the "invalidation domain is coarse by
design" note. (The perf symptom — a 1440ms LONGTASK every 530ms — is the
downstream consequence when the fn is expensive; the re-run itself is what this
regression locks in.)

### Claim 9 — impure derivation: reset! inside m/latest creates hidden edges

**Verdict: PLATFORM-REAL** (with a precision correction). **Confidence: HIGH.**

**Evidence:** regression `claim-09-hidden-edge-deref-not-declared-input`:

```
(a) reset! !b INSIDE <d's combine fn: !b's OTHER watchers DID fire (b-runs = 2)
    => the reset! side effect is VISIBLE to m/watch consumers, not invisible.
(b) @!c read inside the fn but NOT declared a flow input:
    after (reset! !c :y): runs stayed 1, seen = [0 :x]   (STALE — fn did not re-run)
    after (reset! !a 1) : runs = 2,      seen = [1 :y]   (only a declared-input change re-runs it)
```

The real "hidden edge Missionary can't see" is precisely (b): **an atom read via
`@` that is not declared as a flow input is untracked — changing it does not
re-run the fn, so the read goes stale** until a declared input changes. The
ledger's framing that a `reset!` inside a fn makes its target's watchers "not
notified as caused-by-this-flow" is imprecise: (a) shows the `reset!` DOES fire
`!b`'s watchers — the side effect is visible; what's untracked is the flow's
*dependency* on it, not the notification. The anti-pattern (side effects in
derivations) is real; the mechanism is the untracked-`@`-read edge.

### Claim 10 — m/observe / m/ap are single-subscription; need a fresh instance per subscription

**Verdict: AGENT-ERROR.** **Confidence: HIGH.** Not in the suite (documents a
falsification).

**Evidence (probe transcript, build -45):**

```
shared ONE m/observe instance, subscribed twice:
   setup-runs=2  sub1={:seen [:v]}  sub2={:seen [:v]}     ; BOTH work
factory (fresh m/observe per subscription):
   setup-runs=2  sub1={:seen [:v]}  sub2={:seen [:v]}     ; BOTH work
shared m/watch def, subscribed twice:
   sub1={:seen [:w :w2]}  sub2={:seen [:w :w2]}           ; BOTH work
```

The claim — *"an m/observe/m/ap can be subscribed to only once; sharing one as a
top-level def and subscribing twice FAILS"* — is **false**. A plain `m/observe`
is freely re-subscribable: each subscription re-runs its setup fn independently
(`setup-runs=2` for two subs), and both receive values. `m/watch` likewise. The
genuine single-subscription/shared-state property belongs to **`m/signal` /
`m/stream`** (memoized/multicast flows), not to `m/observe`/`m/ap`. The
codebase's factory pattern (`make-raf-flow`, `make-blink-timer`) is harmless
defensive practice, but the STATED REASON ("must be called fresh … not shared!")
is incorrect — sharing them would not fail. The "text appears then disappears"
bug in `insights.md` was some other cancellation, not an `m/observe`
re-subscription limit.

**PROPOSED-AMENDMENT (`docs/history/insights.md:9-23`):** correct "m/observe/m/ap
are single-subscription" → they are re-subscribable; the single-subscription
property is `m/signal`/`m/stream`. Reframe the factory advice as isolating
per-subscription mutable state, not as avoiding a subscription failure.

### Claim 11 — m/sample arg order: continuous-first, discrete-trigger-last, else "Undefined continuous flow"

**Verdict: PLATFORM-REAL** (with a precision on the trigger). **Confidence: HIGH.**

**Evidence:** regression `claim-11-sample-requires-initially-ready-sampled-inputs`:

```
CORRECT (m/sample vector (m/watch !a) (m/seed (range 3))):
   [:done nil], seen [[:v 0] [:v 1] [:v 2]]
WRONG, not-initially-ready flow in a SAMPLED slot
   (m/sample vector (m/observe (fn [!] #())) (m/seed ...)):
   [:crash java.lang.Error "Undefined continuous flow."]
```

The exact error **`Undefined continuous flow.`** reproduces. Upstream contract
(`missionary-reference.txt:875`): `(sample f sampled* sampler)` — "Each sampled
input must be initially ready." **Precision:** the error is triggered by a
*not-initially-ready* flow in a sampled slot, not by arg order per se — a
`m/seed` (which IS initially ready) placed in a sampled slot does not throw.
CLAUDE.md's reliance is correct because the real trigger there is `>raf` (an
`m/observe`, never initially ready), so putting it in a sampled slot throws.

### Claim 12 — m/join cancellation: any branch failing cancels the others and fails join

**Verdict: PLATFORM-REAL** (upstream-documented + reproduced). **Confidence: HIGH.**

**Evidence:** upstream doc `docs/reference/missionary-reference.txt:783` verbatim:
*"If any task fails, others are cancelled then join fails with this error.
Cancelling propagates to children tasks."* Plus regression
`claim-12-join-cancels-siblings-on-failure`:

```
join(blocker, boomer): outcome = [:fail "boom"], other-cancelled = true
```

A throwing branch fails the whole join with that error and cancels the sibling —
the mechanism behind the "Watch cancelled" tear-downs when any one of the ~12
consumers under `runtime.cljs:363`'s top-level `(m/join vector …)` dies.

### Claim 13 — "e/watch only works within a single peer — no server→client pipe" (recorded as stale)

**Verdict: AGENT-ERROR** (also stale). The recorded quirk is **contradicted** by
Electric v3 semantics AND by shipped, running code. **Confidence: HIGH.**

**Evidence (primary source, build -45):**
- `hyperfiddle/electric3.cljc:157-163` — `e/watch` is
  `` `(check-electric watch (input (m/watch (watchable! ~ref)))) `` — i.e. it
  wraps `m/watch` in **`e/input`**, producing a reactive Electric value (not a
  raw Missionary flow).
- `hyperfiddle/electric3.cljc:206` — `(defmacro server [& body] …(::lang/site
  :server …))` sets the site to `:server`.
- Shipped bridges `src/app/file_viewer.cljc:107-190` — six
  `(e/server (e/watch util-fns/!…-atom))` defns.
- Consumed **on the client**: `src/app/electric_flow.cljc:460` opens
  `(e/client …)`, and `:484-489` do `(reset! !sidebar-truth (fv/WatchSidebarTruth))`
  etc. — the client stores the value of a server-side `e/watch`. Comment: *"Reactive
  sync: Rama PState → Electric → client atom. Re-runs whenever the server-side
  PState changes."*

So `(e/server (e/watch atom))` **does** transfer a reactive value server→client;
the entire Rama-truth → client path depends on it. The recorded quirk conflated
two different things: a *raw Missionary* `m/watch` flow indeed cannot cross peers,
but Electric's site-transfer of an `e/watch` (an `e/input`) value can and does.
(`PROBE-EVIDENCE.md` fence: the render probe ran no wire/transfer, so this rests
on the macro definition + shipped code, which is itself the counter-evidence.)

**PROPOSED-AMENDMENT (`memory/implementation-quirks.md:49-51`):** retract the
"e/watch only works within a single peer / no server→client pipe / HTTP for data
transfer" quirk. Replace with: `e/watch` = `e/input` over `m/watch`;
`(e/server (e/watch atom))` transfers reactively to the client and is the shipped
Rama-truth bridge.

### Claim 14 — foreign-proxy-async on global PStates crashes Rama 1.6.0; atom-mirror + e/watch workaround

**Verdict: UNVERIFIED (cheaply).** **Confidence: n/a** (provenance is strong;
this stage did not re-probe it).

Not attempted — reproducing needs a live Rama 1.6.0 `InProcessCluster` with a
global PState and a `foreign-proxy-async` root-path subscription over the RocksDB
wire path (`-Xss16m`, seconds-long boots). Neither render-bench doc touches Rama.
The workaround is heavily relied on (`util_fns.cljc:122-134` mirror atoms,
`file_viewer.cljc` bridges) and provenance is concrete (commit `c292169`, S40).
**What would settle it:** the ledger's own sketch — a Rama-cluster probe
subscribing `foreign-proxy-async` at path `[]` on a global PState and observing
the RocksDBWrapper serialization crash, plus the proxy-callback-returns-`nil`
requirement. Version-pinned (`deps.edn:16` = 1.6.0), so it is real *for this
repo* regardless.

### Claim 15 — incseq/mount contract is 5 named callbacks; gpu-mount implements it but is DORMANT

**Verdict: PLATFORM-REAL** (the contract) **but MISNAMED and, as-built,
SEMANTICALLY BROKEN for permutations.** **Confidence: HIGH** (triple-corroborated).

**Evidence:**
- **Jar source** `hyperfiddle/incseq/mount_impl.cljc:7` —
  `(defn mount [append-child replace-child insert-before remove-child nth-child] …)`.
  The 5-callback contract, names, and order are **real and current**, and
  `gpu-mount`'s five keyed fns match the arities the mount algorithm calls them
  with (append 2-arg, replace 3, insert-before 3, remove 2, nth 2). Used by
  electric-dom: `electric_dom3.cljc:28` `[hyperfiddle.incseq.mount-impl :refer
  [mount]]`.
- **Misname:** the real fn is **`hyperfiddle.incseq.mount-impl/mount`**, a
  *positional 5-arg* fn — NOT `incseq/mount`. The public `hyperfiddle.incseq` ns
  ships no `mount` var (its API is the diff algebra: `permutation`, `arrange`,
  `items`, …; `mount-items` at incseq.cljc:775 is inside a comment). So
  `gpu-mount`'s docstring sketch `(incseq/mount …)` references a var that does
  not exist.
- **Dormant:** grep finds `gpu-mount` only at its def and inside its own
  docstring's "Usage" sketch — no external call site. Confirmed.
- **Semantically broken for reorders** (`PROBE-10K §4` + `PROBE-EVIDENCE.md`
  obs. #8, VERIFIED-HERE): the mount contract passes existing **child handles**
  back through `insert-before` during rotations (DOM `insertBefore` = *move*);
  `gpu-mount` instead **allocates a fresh slot** from whatever it receives. So
  **any `:permutation` corrupts the pool** — a 91-frame n=100 rotate demo ended
  with **16,750 active slots for 100 entities** at 160ms/frame, while `patch-vec`
  state stayed correct (`rows-match? true`). The corruption is in the bridge, not
  the diff algebra.

So the ledger's "platform-real but unexercised — drift undetected" is confirmed
and sharpened: names/order/arities are right, but the location is misnamed AND
the allocate-on-insert semantics would corrupt on the first reorder if wired.
This is a *contract-semantics* fact (the DOM mount contract is not reusable for a
slot pool whose "move" is an indirection update), corroborated across SwiftShader
and AMD-hardware runs.

**PROPOSED-AMENDMENT (`src/app/client/substrate/webgpu/buffer_pool.cljs:419-462`
gpu-mount docstring):** (1) fix `incseq/mount` → `hyperfiddle.incseq.mount-impl/mount`
(positional 5-arg); (2) add the load-bearing warning: `insert-before` means MOVE
an existing child, so an allocate-on-insert callback corrupts the pool on any
`:permutation`; the scene store consumes the six ops directly (C2 shape) instead
— do NOT wire this bridge as-is. (The permutation-cost relocation itself belongs
to **NORTH §9's trap ledger**, not CLAUDE.md.)

### Claim 16 — trail-view-runtime is a defonce delay deferring Rama boot to the first /trail pull

**Verdict: PLATFORM-REAL** (standard Clojure semantics; not a falsifiable platform
claim). **Confidence: HIGH.**

**Evidence:** `src/app/file_viewer.cljc:145-172` — `#?(:clj (defonce
trail-view-runtime (delay (let [rt (trail-view/start-trail-view-runtime!)] … (future
(initial-sweep! …) (start-ingest-watchers! …)) rt))))`; `:174 (defn trail-rt []
@trail-view-runtime)`; four `Trail*` `e/defn`s deref `(trail-rt)`.
`start-trail-view-runtime!` is real at `src/app/server/rama/trail_view.clj:749`.
Clojure `defonce` + `delay` guarantees the boot is forced exactly once on first
`@`, then memoized; async ingest via `future` is a design choice, not a
falsifiable behavior. The only risk is first-pull boot latency, not correctness.

### Claim 17 — Electric/Missionary is "continuous synchronous programming" (Van Roy), NOT FRP

**Verdict: PLATFORM-REAL in substance, but one sub-claim ("atomic settle before
the next change") is FALSIFIED at the raw `m/latest` layer, and the "Van Roy"
attribution is unconfirmed.** **Confidence: HIGH (the glitch); MEDIUM (the label).**

**Corroborated (primary source / web):** the *continuous, synchronous,
demand-driven/pull* framing is Getz's own — Electric is "arrowized continuous
time dataflow," Clojure fns run synchronously, `e/watch` compiles to Missionary,
and "the model is actually pull … child notifies of a new value and parent
transfers when it wants to" (backpressure/laziness). (WebSearch, 2026; see
Sources.) The pull model is confirmed by Claim 3's own probe.

**Falsified sub-claim — `m/latest` is NOT glitch-free** (probe transcript, 4/4
runs deterministic):

```
diamond: !a -> <b=(inc a), <c=(dec a); combine [b c]. reset! !a 10, then 20:
   run0 seen [[1 -1] [1 9]  [11 9]  [11 19] [21 19]]  glitch? true
   run1 seen [[1 -1] [11 -1][11 9]  [21 9]  [21 19]]  glitch? true
   run2 seen [[1 -1] [1 9]  [11 9]  [11 19] [21 19]]  glitch? true
```

On a diamond dependency (two paths from `!a`), a downstream `m/latest` observes
**inconsistent intermediate pairs** (e.g. `[11 -1]` — `b` from `a=10`, `c` still
from `a=0`) before settling. So the graph does NOT "settle atomically before the
next change is observed" — transient glitches occur within a single change's
propagation (the final settled value is consistent). Missionary does not
topologically batch diamonds. **Caveat:** this is the *raw `m/latest`* layer;
whether Electric v3's full compiled DAG is glitch-free is untested here
(`PROBE-EVIDENCE.md` did not run the DAG). The ledger's own suspicion — "the
taxonomy label is approximate; 'precisely Van Roy category X' is overstatable" —
is upheld.

**PROPOSED-AMENDMENT (CLAUDE.md:19-27):** soften the "settles atomically before
the next change is observed (the synchronous hypothesis)" line — raw `m/latest`
diamonds glitch; consistency requires deriving co-varying values from a *single*
`m/latest` over the shared source (`(m/latest (fn [a] [(inc a) (dec a)]) (m/watch
!a))`), not two `m/latest`s combined. Mark the "Van Roy taxonomy" attribution as
unverified-from-Getz (it is a defensible framing, not a sourced quote).

### Claim 18 — Missionary flows are not derefable with @

**Verdict: PLATFORM-REAL.** **Confidence: HIGH.**

**Evidence:** regression `claim-18-flows-not-derefable-with-deref`:

```
(deref (m/watch (atom 0))) => throws java.lang.ClassCastException  ; a flow is not IDeref
(deref (atom 0))           => 0
```

A flow is a function, not a reference type; `@flow` throws. Only atoms/refs are
derefable. Confirms the primitive distinction; the code always samples flows and
only `@`-derefs atoms.

### Claim 19 — Missionary flow composition gravitates toward a single m/join orchestration point

**Verdict: UNVERIFIED (design-opinion).** **Confidence: n/a.**

An architectural tendency, not a falsifiable platform behavior — "gravitates" has
no crisp failure condition. It is observably true of *this* codebase
(`runtime.cljs:363-395` is a single terminal `(m/join vector …)`), which the
provenance already notes. **What would settle it as a general law:** nothing
empirical from a probe; at best a survey of independent Missionary codebases —
out of scope and low-value. Real as an observation; a heuristic, not a fact.

### Claim 20 — the :prod shadow build is pre-broken by the v2/v3 namespace drift

**Verdict: PLATFORM-REAL.** **Confidence: HIGH.**

**Evidence:** compile-check + jar inspection.

```
(require 'hyperfiddle.electric3)  => REQUIRE-OK            ; v3 ns present
(require 'hyperfiddle.electric)   => java.io.FileNotFoundException:
   Could not locate hyperfiddle/electric__init.class, hyperfiddle/electric.clj
   or hyperfiddle/electric.cljc on classpath.
```

The -45 jar ships `hyperfiddle/electric3.cljc` (v3) and impl namespaces but **no
top-level `hyperfiddle/electric.cljc`** (v2). `src-prod/prod.cljc:9` requires
`[hyperfiddle.electric :as e]` — a namespace the resolved jar does not provide —
so the `:prod` build cannot compile as written; only `:dev` is runnable.
Corroborates `implementation-quirks.md:166`. Harmless while `:prod` is unused,
load-bearing the day someone ships. (No amendment needed — the ledger and quirks
already record it; this just confirms it at the classpath.)

---

## Summary

### Counts by verdict (20 claims)

| Verdict | Count | Claims |
|---|---:|---|
| **PLATFORM-REAL** | 14 | 1, 2, 3, 5, 7, 8, 9, 11, 12, 15, 16, 17, 18, 20 |
| **AGENT-ERROR** | 3 | 6, 10, 13 |
| **UNVERIFIED** | 3 | 4, 14, 19 |
| **STALE-VERSION** | 0 | — (two stale *suspicions* examined: Claim 5 refuted as current; Claim 13 resolved as agent-error) |

Qualifications carried inside PLATFORM-REAL entries: **1** (mechanism = nested
`m/?<`, not "fed to m/latest"), **7** (backpressure real; exact "stale-first"
ordering unverified), **9** (hidden edge = untracked `@`-read, not invisible
`reset!`), **11** (trigger = not-initially-ready, not mere arg order), **15**
(contract real but misnamed `incseq.mount-impl/mount` and allocate-on-insert
corrupts on `:permutation`), **17** ("atomic settle" sub-claim falsified — raw
`m/latest` diamonds glitch).

Regression tests kept (9, all green):
`claim-01,02,03,06,08,09,11,12,18` in `test/app/missionary_claims_test.clj`.
Falsified/timing-only probes kept as code blocks here, not in the suite: claims
**7, 10, 17**.

### Top 3 corrections the future skill rewrite MUST encode

1. **The `m/ap` + `m/?<` ban is about NESTED multiple `m/?<` over `m/watch`
   inside ONE `m/ap` — not "fed to m/latest."** It crashes ("Watch cancelled")
   when consumed directly; two *separate* single-`?<` `m/ap`s under `m/latest` do
   not. And `reactive_master.md:26-27` still ADVOCATES this exact banned pattern
   ("use m/ap to define relationships") — the doc-vs-doc contradiction must be
   resolved in favor of the ban, with `m/latest`-of-watches as the combiner.
   (Claims 1, 2.)

2. **`e/watch` DOES transfer server→client in Electric v3 — Claim 13 is false.**
   `e/watch` = `e/input` over `m/watch`; `(e/server (e/watch atom))` is
   transferred to and consumed by the client in six shipped bridges
   (`file_viewer.cljc` → `electric_flow.cljc` `e/client`). Retract the "no
   server→client pipe / use HTTP" quirk; it conflated a raw Missionary flow
   (can't cross peers) with Electric-managed transfer (does).

3. **The incseq mount contract is `hyperfiddle.incseq.mount-impl/mount` (5
   positional callbacks), and `insert-before` means MOVE an existing child.** The
   dormant `gpu-mount` bridge misnames it `incseq/mount` and allocates-on-insert,
   so any `:permutation` corrupts the pool (16,750 slots for 100 entities —
   `PROBE-10K §4`, `PROBE-EVIDENCE #8`). Fix the reference and record the
   allocate-vs-move hazard; the DOM mount contract is not reusable for a slot
   pool as-is.

Runners-up worth encoding: `m/latest` arity mismatch is a **loud ArityException,
not silent corruption** (Claim 6); `m/observe`/`m/ap` are **re-subscribable** —
single-subscription is a `m/signal`/`m/stream` property (Claim 10); the
**try/catch-in-`e/defn` ban is CURRENT on build -45**, not stale (Claim 5); raw
`m/latest` **diamonds glitch** — "atomic settle" is not a Missionary-layer
guarantee (Claim 17); and the RAF rule's **Gap-3 sunset is now measurably viable**
for change/append/tail-shrink diff shapes, with reorder diffs barred as the
carrier (Claim 3, `PROBE-EVIDENCE §3`).

### Sources

- Missionary b.46 jar (`~/.m2/.../missionary-b.46.jar`); Electric -45 jar
  (`electric-v3-alpha-20260519.115706-45.jar`) — `electric3.cljc`,
  `incseq/mount_impl.cljc`, `incseq.cljc`, `electric_dom3.cljc`, POM.
- `docs/reference/missionary-reference.txt` (`sample` :875, `join` :783).
- Shipped source: `file_viewer.cljc`, `electric_flow.cljc`, `events.cljs`,
  `runtime/render.cljs`, `buffer_pool.cljs`, `trail_view.clj`, `src-prod/prod.cljc`.
- `../render-north/PROBE-10K.md` (§2 producer knee, §4 gpu-mount corruption, §6);
  `./PROBE-EVIDENCE.md` (obs. #8, #9; §3 RAF discipline + Gap-3 sunset).
- Probe transcripts: this session's `clojure -M:test` runs (build -45 / b.46).
- Web (Claim 17): [Electric Clojure — clojure.org](https://clojure.org/events/2023/electric-clojure--1316978772),
  [Dustin Getz — Differential Dataflow for UI (keynote)](https://www.youtube.com/watch?v=QIam96cpIoI),
  [dustingetz.com](https://www.dustingetz.com/).
