# DESIGN-1 — the executor and the compositor's two operations, with two brushes as their first consumers

Design session: Fable at max (load at low), 2026-09-07. Written against `main` at `f1a7200` while the path kind's first production slice lands beside it (the merge `dd5d34b` and the capsule port `f1a7200` are on `main`; the language port and the boundary port are in the landing session's working tree, uncommitted, and are not read here). The one amend this file owes is named in §13.

This file is the design as data (two-chairs, the production section): the values exchanged, each level's inputs, who owns which dependency and which effect, the record examples, every affected caller by name, the deletion scope, the unresolved choices, and Sid's rule in words. It is not a contract and not a test suite before the code. The landing sessions are Codex in fresh contexts; a landing does what this file says and nothing it does not say, so this file says everything, and where it takes a position it says what supports it, when it becomes only a default, and what else changes with it. The measure a landing is judged by is the definer's numbers, reproduced through the client.

---

## 1. The rulings this design runs under

Verbatim, with times, because a paraphrase is not a ruling.

- **The executor** (Sid, 2026-09-06 11:27): "executor yeah", to the path page's Position 8: records over a vocabulary of capabilities, an ordering rule and compiled expressions; instances many, the definition once.
- **The compositor** (Sid, 2026-09-06 11:51): "Lets do A", of four options: one piece with two operations, paint and sample, owning clips, blends, layers, the screen and the surface a brush reads, painting and vector sharing it. Carried to the 3D page and not re-asked: read-surface is that sample operation bound to a 3D binding, not a second compositor.
- **Position 9, the 3D kind's input** (Sid, 2026-09-06 17:42): "okay. Let's do b. now save all the things that you need to save and whatever the rulings need to go or whatever." The three sentences it rules: the kind takes the recipe as its input and keeps it with the result, the result keeping the subject it was built from; a recipe is a record, steps over a vocabulary of capabilities with formulas in the leaves, run by one executor with several runners, a tool never in git, a capability code below the waist added once with receipts, a solver a capability; nothing in a record is a program, and foreign code enters only as a value in a slot with a declared output. Its exits are on the page and are not re-asked here.
- **The stance** (Sid, 2026-09-06 11:27 and 11:45): "our aim is not to copy anyone they are just a datapoint we go with the best experience and the best in the world for this field. From the start there is no concept of carrying forward existing solution if that is not how it shoudl be." Applied here to both benches' executors, to the Codex lane's executor the slice is porting, to the tree's compositor and to the 3D tree as it stands: each is a datapoint and nothing in this file keeps a piece because it exists.
- **No trial** (Sid, 2026-09-06, after 16:37): "i dont think there will be any more trial". The two slices this file cuts are production, judged on delivery.
- **The two reflexes** (Sid, 2026-09-06, 16:37 to 16:54): "fable tries to make shortcuts and codex only does what is told". This file fills no gap silently: where it does not know, it says so and names the receipt that decides.

**Sid's caching rule** (2026-09-06, 16:17 to 16:23): "caching is never an option it means the underlying thing is as it should be and then still need more performance therefore we nede to do caching on top". In this file's words, so that this session's own reflex cannot put a memo back: every level of the data flow below is a pure function whose inputs are explicit as data; a level reruns when its input changes and only then, and the test of the level is that a change to X reruns exactly level Y and nothing else; a memo, where one is ever added, is keyed on the level's full input as a value, never on what a run happened to read and never on a hash; no memo, bucket, batch or atlas is added, and none that exists is credited, without a trace on a representative document that shows the need. Consequences this file draws: the executor keeps nothing between runs and reports what a run read only as a diagnostic; a surface's revision chain is a value's lineage, not a cache; a continuation is a value, not a cache; the push edge is the underlying thing and lands before any per-item cache is discussed again; the pack per region key that exists today is keyed on the region's full input (its outline and rule, with the bucket inside the entry) and is left as it is, credited with nothing.

---

## 2. The picture

Two kinds, one executor, one compositor. A tool is a record. A record's program is steps over a vocabulary of capabilities; the executor moves values between capabilities in the order the record declares and does no work of its own. Every heavy thing is a capability: the path kind's geometry, the 3D kind's support and coating, and the compositor's two operations, which exist once in the engine and are bound into each kind's capability table with that kind's notion of a region's coverage. A brush that reads what it painted is a program that calls `sample` before `paint`; a read may come back pending, and then the program suspends into a continuation of bytes with no next state, resumes under a grant to the same bytes, and refuses an answer that does not belong to its request.

```
record (a tool)                                 the store above the kind
  program: steps over capabilities  ──────►  executor (engine, exists once)
  roots: tool · paint · surface · …                │ moves values, keeps order,
                                                   │ suspends, resumes, refuses
                     ┌─────────────────────────────┼───────────────────────┐
                     ▼                             ▼                       ▼
            path capabilities             compositor (engine)      3D capabilities
            source · dabs · envelope      surface · paint · sample  curve-point · surface-region
            snap · clip-region            mix · new · blends        member · read-surface
            (coverage from pack's twin)   (a region's coverage is   (coating bindings under
                                          the kind's argument)      a work budget)
```

The levels, and the rate at which each reruns (Sid's rule applied):

| level | pure function | explicit inputs | output | reruns when |
|---|---|---|---|---|
| L0 edit | the kind's recipe at the edit boundary (the slice's `construct`) | the record's source, tool, paint, identity | the kind's value (a path value; a 3D record's authored events) | the record changes |
| L1 geometry | the kind's constructions | the value, the paint's geometry fields, the declared view facets (projected scale for a device-unit width; the pan's fraction for a snapped record) | regions, dabs, the stroke's skin | the value or a declared facet changes |
| L2 program | the executor over the record's program | the program, the roots, the items, the state before an item, the resolved reads | the state after each item, the returned values, a continuation when suspended | an item, a root the program reaches, or a read's snapshot changes |
| L3 surface | the compositor's paint and sample | a surface value, a region's coverage, a paint, clips, a blend; a stack of layers and a point | a new surface value; a read | its arguments change |
| L4 pack | the packer | a region's outline and rule, the bucket | quads, bands, cover | the outline, the rule or the bucket changes |
| L5 rows and frame | the renderer | pack slots, colours, group indexes, clip chains; the camera | instance rows; one draw | a row's input changes; the camera every frame on the GPU |

Nothing below the executor reads a clock, a frame, a grant or a cache; nothing above it is a program.

---

## 3. The executor as data

One namespace, `src/app/client/engine/executor.cljc`, pure, no retained state, the same code on the JVM and in the browser. The slice landing beside this file replaces the base's executor with the Codex lane's (`{:bindings :steps :return}` with `[:get …]` expressions, positional `:args`, `each` as a step). This design keeps that expression language and changes the step form and the loop; §13 reconciles against the landed file.

### 3.1 The record's program

```clojure
{:steps  [step …]                      ; run once, in order, before the loop; may be empty
 :each   {:items expr                  ; a sequence: a root, or the output of a step above
          :item  :dab                  ; the name the current element is bound to (explicit, always)
          :state {name expr …}         ; the state before the first item; evaluated once, on a fresh run only
          :steps [step …]              ; run per item, in order
          :next  {name expr …}}        ; after an item's steps: the state for the next item
 :return expr}                         ; over the final scope: the roots, the pre-loop outputs, :state

step = {:out :name :op :capability :args {arg-name expr …}}
       ; a read step may add :pending :wait (default) | :provisional
```

- **Expressions** are the slice's one language, kept: `[:get root k …]` reads a path into the scope (keywords for map keys, integers for vector positions: `[:get :paint :stroke :color 3]`); `[:literal v]` quotes; `[:if t a b]` evaluates the chosen branch; a vector whose head is a known numeric operation (`:+ :- :* :/ :min :max :abs :sqrt :pow :sin :cos :clamp :mix :< :<= := :not`) applies it; any other vector is data and its elements are evaluated one by one (so `[[:get :dab :x] [:get :dab :y]]` is a point); a map evaluates its values; a keyword, string, number, boolean or nil is itself. There is no second syntax for a reference: a string is always a word, never a path. That closes two of the cold mind's thirteen (a bare name as a reference; references inside arrays) by construction.
- **Named arguments**, not positional. Each capability declares the argument names it takes; a step's `:args` is a map. Supports: both benches, the definer's every record, the cold-mind attack and the base's own table at HEAD all name arguments; a name the capability does not declare is reported as not consumed (bench 9's honesty check, which the judges scored against the Codex lane for lacking); a missing required name is reported before anything runs. Becomes a default only if the landed expression compiler needs positional forms for the width rules, in which case the width rule stays an expression and steps stay named. What changes with it: the Codex lane's `run-step` `call` branch evaluates a map of expressions and calls the capability with one map; the three path recipes are rewritten with named arguments (`{:out :path :op :path/anchors :args {:contours [:get :source :contours]}}`); every capability is a function of one map.
- **One loop, at the program's level**, after the pre-loop steps, with explicit carried state and an explicit `:next`. Supports: both benches' grammars and the four definer records are exactly this; bench 9's built-in construction before its program is the pre-loop steps made explicit; the continuation's shape (§3.5) needs the carried set named, and a scope where every binding is carried (the Codex `each`) cannot say what a checkpoint holds. Not in the vocabulary: a nested loop, recursion, a loop as a step. That is the executor's door's exit as ruled (iteration beyond `each` over a collection opens the third door), and no record on either bench needed it.
- **Refused before any step runs** (`{:status :refused :reason … }`): an `:op` no capability answers (`:missing-capability`, with the list); an argument name the capability does not declare (`:unconsumed-argument`); a required argument absent (`:missing-argument`); an `:out` that names a root or `:state` (`:shadows-root`; the cold mind tripped on shadowing, so it is refused rather than allowed); a program with two loops or a loop inside a step (`:one-loop`); an `:each` without `:item`. A `[:get]` into a path that does not exist is an error at the step that evaluates it, with the path, and stops the run (`:status :error`); an empty result is never a run that succeeded.

### 3.2 The run

```clojure
(executor/run record scope capabilities opts)
;; record        the program under :program plus the record's own roots (tool, paint, surface, events, …)
;; scope         the caller-supplied roots as values: {:path value, :support value, :inputs {…}, …}
;; capabilities  {:op {:args [:name …] :run (fn [args ctx] → value) :snapshot (fn [args ctx] → value)?} …}
;;               plus :vocabulary "a sentence naming the table's behaviour and version"
;; opts          {:budget n | nil (unlimited)   ; the grant for reads; never read by the executor itself
;;                :until n                       ; suspend before item n (bench 9's checkpoint)
;;                :stop-at :out                  ; run to that read step, return its request and value, commit nothing (the seam bench's xDemand)
;;                :from continuation             ; resume (§3.5)
;;                :answer {:request r :value v}} ; an answer delivered for a pending read (§3.5)
→ {:status :complete  :results {…} :state {…} :history [row …] :log [{:out :op :status :ms} …] :reads {…} :unread […]}
| {:status :suspended :reason :until | :pending | :needs-policy  :at n  :continuation c  :request r? :missing m? :read v?}
| {:status :answered  :at n :step :out :request r :value v}      ; :stop-at reached; nothing committed
| {:status :refused   :reason … :detail …}
| {:status :stale     :reason …}
| {:status :error     :at n :step :out :error msg :data …}
```

The scope the program sees is the record's roots merged with the caller's roots; a step's `:out` binds its value into the scope for the steps after it and for `:return`; inside the loop, `:state` and the item name are roots too. The capability table is per kind, assembled by the kind from the engine's operations bound with the kind's coverage and domain functions (§4) and the kind's own operations; `:vocabulary` is a sentence the continuation carries and a load checks by equality (§3.5).

`ctx` passed to a capability: `{:budget :at :step :record}`; a capability that needs the grant compares against `:budget` per call and nothing accumulates (the seam bench's rule: work charged per read, nothing cached across reads; a run of four dabs at G@2 needs a grant of 1 on every read). The executor never reads the budget, never decrements it and never decides it: the grant is the caller's, and §12 is about who that caller is.

### 3.3 The transition

Per item, in order: bind the item; run the steps; evaluate `:next` over the scope (roots, `:state`, the item, the step outputs); the new state replaces the old; a history row is appended: `{:at n :item item :state-after state :steps [{:out :op :status …} …]}` where a read step's entry carries the read's `:status`, `:contributors` and `:snapshot`, and a paint step's entry carries `:changed`. A transition consumes exactly the item, the state before it, the program, the roots the program reaches and the resolved reads; nothing else, and in particular not the grant, not a clock, not the history. That sentence is the invariant the hardest case rests on (§7): the state after an item is the same whatever the timing of the grant, because the grant is not consumed.

### 3.4 What a run read, as a diagnostic

`:reads` maps every `[:get root …]` path evaluated against the original roots (never a step output) to the value it read, in order of first reading; `:unread` lists the record's root fields no read reached (bench 9's "fields with nowhere to go"; the definer's honesty check). Neither is a key to anything. The base's `reread` and `rerun?`, which keyed a cache on reads, are gone with the slice's boundary port and do not return.

### 3.5 Suspension, the continuation, resume, and what belongs

A run suspends in three cases: `opts :until` reaches an item boundary; a read step's value is `pending`; a read step's value is `needs-policy`. In every case the run stops before `:next`: the state, the history and the position stay what they were, and the result carries a continuation. A step consumes only a resolved read: a step whose argument is a read whose `:status` is not `:resolved` is never run.

**The continuation is a value:**

```clojure
{:schema      "softland/executor-continuation/1"
 :vocabulary  "…"                          ; the table's sentence, checked by equality on load
 :record      record                       ; the record as run: the program and its own roots
 :scope       {…}                          ; the caller's roots as values (a path value; a support; inputs)
 :at          n                            ; the index of the item not yet completed
 :consumed    [item …]                     ; items 0 … n−1 as values
 :state       {…}                          ; the state before item n; a surface's :data as bytes (§4.1)
 :history     [row …]
 :request     r | nil                      ; when suspended at a read
 :missing     […] | nil}                   ; what the read named as withheld, or the candidates without a policy
```

As bytes: EDN, with every typed array encoded as `{:float32le "<base64>" :length k}`. A load checks the schema, the vocabulary sentence by equality, every array's declared length against its surface's width × height × 4, and decodes; it refuses with a reason (`{:status :refused :reason :load …}`) when any does not hold. A changed interpretation is a refusal, never a silent conversion (attack 4's exit, carried). The byte count is the client's own, not bench 9's 364,786 (that number was the bench's JSON and is the tracer's, not the definer's); the receipt is the round trip, §5.3.

**Resume:** `(executor/resume continuation opts)` runs `record` (or `opts :record`, when the caller resumes an edited record) over `scope` (or `opts :scope`) from `:at`, after four checks in this order, each a refusal with its reason when it fails: the vocabulary sentence equals the table's; the **recipe** of the resumed record equals the continuation's, where the recipe is the program plus the values of the record's roots the program references, and a reference is every `[:get root …]` that occurs in the program, found by reading the program and never by running it (the landed executor's `references` is that reading), without the identity fields and without the root the loop's `:items` walks (a 3D record's events; a 2D pickup's dabs). So the pickup's `:path/source` is not in the brush's recipe, since the program reaches the path only through the caller's `path` root, and an edit to the source is judged by the consumed items alone (§5.3's four edits); its `:path/tool` and `:path/surface` are, since the program references them; the loop's `:items`, evaluated afresh over the resumed scope, agree with `:consumed` at every index below `:at`, by value, else `:consumed-items-differ` with the first differing index (bench 9's "dab 0 differs"); the restored state's arrays decode to their declared lengths. The `:state` initial expressions are not evaluated on a resume. The pre-loop steps do run again on a resume (they are pure and per edit; their outputs are not stored). The item at `:at` runs from its first step: no output of the suspended attempt is kept, which is why the withheld run leaves nothing in the granted one (§7). Items after `:at` come from the resumed scope, so a record that gained an authored event after suspending runs it ("accepts more authored events").

**The request and belonging.** A read capability declares `:snapshot`, a function of the same arguments that returns, without doing the read's work, the values the read depends on (§4.3 says what a snapshot pins). At a read step the executor forms

```clojure
request = {:recipe recipe :consumed consumed :state state-before :at n :step :out :snapshot (snapshot args ctx)}
```

before calling `:run`. When `opts :answer` is present and its `:request` equals the request the run has reached, by value in all six parts, the answer's `:value` is the read's value and `:run` is not called; when they differ, the run returns `{:status :stale :reason …}` naming the first part that differs (the recipe changed since the read was demanded; the input state is not the one the read was demanded in; another item or step; the read's snapshot changed: order, bindings, painting or point), and nothing commits. An answer that no read consumes by the end of the run is `:stale` too ("the answer was not consumed"). Equality is by value; no hash is compared anywhere. A hash of a painting's bytes or of a request appears only in printed receipts and in the definer's tables, so a number can be copied and compared, never as the thing checked. Supports: Sid's rule (a key is the full input as a value, never a hash); the seam bench hashed for cross-process printing, and its every check is an equality that holds on values. Becomes a default only if a request's value proves too large to carry in a continuation on a real document, which no bench has shown; what changes then is a content address computed from the value and checked against the value, not a hash as the identity.

**Provisional.** A read step with `:pending :provisional` does not suspend on pending: the compositor composes the known layers with the missing ones transparent and marks the read `{:provisional {:missing […] :interpretation "missing layers read as transparent (a declared provisional read)"}}`; the step consumes it; the history row keeps the mark. This is Position 12's narrowed exit, kept as the declared exception and nothing more: retaining the provisional read makes the selected history reproducible, not the selection independent of scheduling.

### 3.6 What the executor never does

It keeps no cache and no state between runs; it reads no clock (the per-step `:ms` in the log is a measurement, not an input); it never decides or decrements a grant; it never calls a host, a device or a store; it never interprets a string as code; it never runs a step whose input is an unresolved read; it never commits an answer it cannot show belongs.

---

## 4. The compositor as data

One piece, two files. `src/app/client/engine/surface.cljc`, new, pure, holds the surface value and the two operations, the CPU runner that is the reference; `src/app/client/engine/compositor.cljs` stays the piece's physical side (the target pool, region leases, the present pass) and gains no operation in this design. The engine's README says the two files are one piece and names the GPU road as the second runner, to come after a trace (§4.5).

### 4.1 The surface value

```clojure
{:surface/id "paint"  :revision 0                 ; revision = depth in the chain of paints
 :width 128 :height 128                           ; texels
 :domain {:kind :plane :map [a b c d e f]}         ; local → texel: tx = a·x + c·y + e, ty = b·x + d·y + f
       | {:kind :chart :chart :S0 :rect [u0 v0 w h]} ; the 3D patch on a chart, in the chart's units
 :color :linear-premultiplied-rgba
 :filter :nearest
 :data  <float32, width × height × 4, row-major, texel (0,0) first>
 :key   "paint@initial" | "paint:<item>/<step>"   ; the step that made it (bench 9's identity rule)
 :parent key | nil                                ; the surface painted on
 :changed n?}                                     ; texels the last paint touched (a receipt)
```

A surface is a value: `paint` returns a new one and the input keeps its contents. Identity is the step that made it and its parent; depth alone is not an identity. Two runs painting the same step produce equal values, which is the point. The declaration a record carries is the same map without `:data`, `:key`, `:parent`, plus `:initial [r g b a]`; `(surface/new declaration)` fills a fresh surface with `:initial` under the key `"<id>@initial"`. The 2D bench's `{:clear […]}` is written `:initial […]` in the client; the 3D bench's `initial` already is.

Texel centres: on a plane, texel (x, y) has its centre at the local point that the inverse of `:map` sends (x + ½, y + ½) to; on a chart, at (u0 + (x + ½)·w/width, v0 + (y + ½)·h/height) in chart units, which the kind's domain function turns into a point on the support. The engine knows planes; a kind supplies its other domains as functions in `ctx` (§4.4). A read outside a surface's domain is transparent, `[0 0 0 0]`, with `:covered? false` on the read: a surface has no value there, and through a stack (§4.3) the read falls to the layer beneath, and at the root to nothing. Supports: "never a wrong pixel" (bench 9 refused superseded values for the same reason); the seam bench returned `unsupported` for a read outside its patch and bench 9 clamped to the edge texel, two datapoints, neither reached by any definer number (every dab of both brushes lies inside its patch), and neither right for a stack of layers.

Bytes: `:data` is stored as `js/Float32Array` in the browser and `float-array` on the JVM; arithmetic runs in doubles and stores to float32 on both, as the two benches did, so the definer's byte hashes are reachable (§6.4 says what to do when they are not). The encoding for a continuation is §3.5's `{:float32le …}`.

### 4.2 paint

```
paint(surface, region, paint, opts) → surface'
  paint   {:kind :color :rgba [r g b a] :opacity o :blend :source-over}      ; this design's landed kind
        | {:kind :surface :surface s :filter :nearest :opacity o :blend …}  ; a layer painted into its parent; designed, not landed (§4.5)
  opts    {:coverage (fn [x y] → c ∈ [0,1])   ; the region's coverage at texel (x, y), supplied by the kind
           :clips    [(fn [x y] → c) …]       ; each clip's coverage; multiplies
           :bounds   [x0 y0 x1 y1] | nil}     ; texels to visit; nil visits all
```

At every visited texel: `c = coverage(x, y) × Π clip_i(x, y)`; if `c = 0` the texel is untouched; else `src = rgba × opacity × c` on all four channels and `dst' = src + dst × (1 − src.a)`, in linear premultiplied RGBA32F. That one formula is both benches' arithmetic: bench 9's `compositeOver(dst, rgb, a·cov)` with `a = opacity × rgba[3]` and the premultiplied rgb divided out, and the seam bench's `over(rgba × opacity, dst)` at member texels, where a member's coverage is 1 at the centre and 0 otherwise. The blend table holds `:source-over` and nothing else in the two slices below; a record naming another blend is refused `:unsupported-blend` with the table printed. Supports: no definer number on either bench uses another mode; bench 9 carried the W3C separable modes and no record read them; the vocabulary grows by attack, one operation at a time. What changes when a record needs one: a function in the table and its test, nothing in the executor.

The region's coverage is the kind's argument, because the engine may not depend on a kind. The path kind binds `pack/coverage-at` over the region lowered at a quarter-texel tolerance (bench 9: `0.25 / |a|` in local units) at each texel centre mapped through the inverse of `:map`, with `:bounds` the region's texel box; the 3D kind binds the surface region's membership at the texel centre's point on the support (`member(region, point) → 0 | 1`), with `:bounds` nil (the seam bench visits every texel). A clip is a region with no paint whose coverage multiplies, so for the 3D kind a clip is a second membership and the product is the AND the bench computed, and for the path kind it is the same product bench 9 drew in one pass.

### 4.3 sample

```
sample(stack, point, filter, ctx) → read
  stack   [layer …] bottom to top; a layer is a surface value, or {:layer/kind k :sample f :snapshot g}
  read    {:status :resolved :color [r g b a] :contributors [name …] :snapshot value :covered? bool}
        | {:status :pending      :snapshot value :missing […] :known [name …]}
        | {:status :needs-policy :snapshot value :candidates [name …]}
        | {:status :unsupported  :snapshot value :reason …}
```

Layers compose bottom to top by source-over of premultiplied values: `acc' = layer + acc × (1 − layer.a)`. A surface layer's colour is its texel under `filter` at the point mapped through its domain (nearest: floor of the mapped coordinates; a point outside is transparent and not a contributor); any other layer's colour is what its `:sample` function returns for the point under `ctx`, which may be pending, needs-policy or unsupported, and then the read is that status with the layers below it named in `:known`. `:contributors` lists the layers consulted whose domain held the point (a painting layer whether transparent or not; a coating's marks whose region holds the point), not the layers that changed the colour: the cold mind read it the other way, and the contract now says which.

**The snapshot** is what the read depends on, as values: for a surface layer, the surface value itself (its data included; a hash of it is printed in receipts, never compared); for another layer, whatever its `:snapshot` function returns (for the coating: the support at its revision, the retained records, the order, each binding's chain); and the filter and the point. `sample`'s own snapshot is the vector of its layers' snapshots plus filter and point. A read capability's `:snapshot` (§3.5) is exactly this, computed without the work.

The 2D `sample` op is this function over a one-layer stack; the 3D `read-surface` op is this function over a stack the kind assembles from the word `"coating"` and the paintings a record names (§6.3). One function, one contract, two bindings.

### 4.4 mix, new, domains, the table's shape

- `mix(a, b, amount) → a·(1 − amount) + b·amount` per channel including alpha: a linear blend of premultiplied values, not a source-over (the cold mind's fifth).
- `new(declaration) → surface`, filled with `:initial`.
- Domains: `ctx :domains {:plane <built in> :chart {:to-texel (fn [surface point] → [x y]) :to-point (fn [surface x y] → point)}}`; a kind that declares a domain kind supplies both functions when it binds the engine's operations into its table. Nothing in a record is a function; the functions sit in the kind's table, which is code below the waist.
- What the engine exports for a kind's table: `surface/paint`, `surface/sample`, `surface/mix`, `surface/new`, `surface/encode`, `surface/decode`, `surface/png-bytes` (the CPU picture, for the harness), each a pure function; the kind wraps `paint` and `sample` with its coverage and domains and registers them under the record-facing names `:paint`, `:sample`, `:mix`, `:surface/new`.

### 4.5 Runners, the screen, and what is designed but not landed

- **The CPU runner is the reference** (Position 9: the CPU walker as the reference runner; bench 9's twin checked every pixel; the seam bench's every number is a CPU number). Both slices land it and reproduce the definer's numbers through it.
- **The GPU road** (bench 9's fragment road: a surface as two RGBA32F targets, `paint` as one filler draw into the live target, `sample` as a one-texel read-back, one GPU sync per dab; or the compute road, tile-local) is the second runner of the same values and lands after a trace on a real brush shows the CPU runner short of the experience. What it must match is known from bench 9: the CPU twin across the whole surface to at most 1.2e-7, which needed the per-fragment exact local position (`localP()` through the inverse of the draw's map) and which the tree's filler does not yet do ("interpolates the same varying; the same fix applies there when the time comes"). That is the first thing a GPU landing measures.
- **The screen.** Today the tree presents the scene target with a full-screen draw (`compositor/draw-present!`) and composites region3d's leased targets as quads (`region3d.renderer/composite-region!`); those are two instances of `paint(parent, rect-region, {:kind :surface …})` written before the piece had a name. This design names the surface paint kind (§4.2) so that a layer is a surface painted as a region into its parent and the screen is the root, and lands neither the kind nor the rewrite of present and composite through it: no definer number depends on a presented pixel that the client can reach (bench 9's presented pixel through a network face needs `arrange`, `locate` and `face-boundaries`, which the client's path kind does not have), and the bench is the screen: the harness writes the painting as a PNG and its numbers as files. Becomes a default only at Sid's word that the pickup must be visible in the verifier's picture; what changes then is one paint kind in the filler's instance row (a texture in place of the straight colour) and one upload of the surface, and the present and composite passes become its callers.
- **Deleted now**, as wrong form: `compositor.cljs`'s `strip-padded-rows`, `unpremultiply!` and `png-bytes!` (lines 801 to 849 at HEAD), uncalled, duplicates of a read-back the harness does itself (`harness/path.cljs` `render-path-bytes!`, `harness/region.cljs` `w4-read-texture!`); the CPU picture is `surface/png-bytes` in §4.4.

---

## 5. The path kind's pickup brush through them

### 5.1 The data flow

1. **L0, the edit boundary** (the slice's): the record's source and tool → the path value, by the kind's recipe (`records/construct` as landed).
2. **L1, dabs**: the path value + the stroke's geometry fields + the tool → the ordered dab packets, each `{:x :y :r :at :s :p :seg :u :path}` as `stroke/dabs` emits them at HEAD (the disc's path is the dab's region). This is the `:path/dabs` capability, called from the brush program's pre-loop steps.
3. **L2, the program**: the executor over the record's program with roots `tool`, `paint`, `surface`, `identity` from the record and `path` from the caller (the L0 value); the loop over the dabs; per dab: `sample` the surface at the dab's centre, `mix` into the carry, `paint` the dab's disc with the carry at the stroke's opacity.
4. **L3, the picture**: the returned surface, written by the harness as a PNG beside its numbers; on screen later (§4.5).

The caller of L0 and L2 is the harness in this slice and the store's edit reaction later; both hand values in and take values out.

### 5.2 The record, in the client's grammar

The definer's record (bench 9, attack 2; `bench-9/node-route/regress.js:2-6`), translated field for field. The width rule is written as the slice lands it (§13).

```clojure
{:path/material-id "surface-read" :path/revision 1
 :path/tool   {:size 16 :streamline 0 :fit :polyline :width <the slice's form of "size * p"> :pickup 0.5 :carry [1 0 0 1]}
 :path/source {:kind :pen :samples [[26 28 0.65 0] [102 100 0.9 90] [28 100 1 180] [102 28 0.7 270]]}
 :path/paint  {:fill nil
               :stroke {:overlap :accumulate :spacing 12 :tip :nib :width :knot :unit :local
                        :cap :round :join :round :align :center :color [1 0 0 0.62]}}
 :path/surface {:surface/id "paint" :revision 0 :width 128 :height 128
                :domain {:kind :plane :map [1 0 0 1 0 0]} :color :linear-premultiplied-rgba
                :filter :nearest :initial [0 0 1 1]}
 :path/program
 {:steps [{:out :fresh :op :surface/new :args {:declaration [:get :surface]}}
          {:out :dabs  :op :path/dabs   :args {:path [:get :path] :tool [:get :tool] :stroke [:get :paint :stroke]}}]
  :each  {:items [:get :dabs] :item :dab
          :state {:carry [:get :tool :carry] :surface [:get :fresh]}
          :steps [{:out :sample  :op :sample :args {:surface [:get :state :surface]
                                                     :point [[:get :dab :x] [:get :dab :y]] :filter :nearest}}
                  {:out :carry   :op :mix    :args {:a [:get :state :carry] :b [:get :sample :color] :amount [:get :tool :pickup]}}
                  {:out :surface :op :paint  :args {:surface [:get :state :surface] :region [:get :dab :path]
                                                     :rgba [:get :carry] :opacity [:get :paint :stroke :color 3] :blend :source-over}}]
          :next  {:carry [:get :carry] :surface [:get :surface]}}
  :return {:surface [:get :state :surface] :carry [:get :state :carry] :dabs [:get :dabs]}}}
```

The path kind's capability table for it: the slice's recipe operations, `:path/dabs` (over `stroke/dabs`), `:surface/new`, `:sample`, `:mix`, `:paint` (the engine's, bound with `pack/coverage-at` at a quarter-texel tolerance and the plane domain), `:vocabulary "path kind, production 2: sample nearest · mix linear · paint = coverage × opacity·alpha, source-over, linear premultiplied RGBA32F · dabs by arc length · capsule nib"`.

### 5.3 The numbers and where they are pinned

| receipt | value | where the definer states it | the client's test |
|---|---|---|---|
| the texel at (64, 64) after 24 dabs (the cursor at (64.5, 64.5)) | (0.013369522, 0, 0.986630440, 1), red after blue; both bench hosts agree to 1.2e-7 | `bench-9/HANDOVER.md:154`, `regress.js:16` | `test/app/client/path/pickup_test.clj`: each component within 1e-6 |
| the dab count and the chain | 24 dabs, 23 paints, the final key `paint:23/surface`, depth 24 | `HANDOVER.md:209`, `regress.js:15` | the same test: `(count dabs)`, the returned surface's `:key`, `:revision` |
| the checkpoint at dab 12 | resumed to the same 65,536 components as the straight run; through bytes and back the same | `HANDOVER.md:265-266`, `wire.js:11-23` | `run … {:until 12}` → suspended at 12; `resume` → equal `:data`; `encode` → `decode` → `resume` → equal |
| the four edits against the checkpoint | first pressure .65 → .4 refused (dab 0 differs); last x 102 → 110 loads; `tool.pickup` .5 → .25 refused; surface 64 × 64 refused | `HANDOVER.md:267`, `wire.js:26-29` | resume with the edited record: the source is not in the recipe (§3.5), the path root changes, dab 0's pressure differs, `:consumed-items-differ` at 0; the source is not in the recipe, dabs 0 to 11 agree, the resume completes over the new dabs 12 to 23; `tool` is referenced, `:recipe-differs`; `surface` is referenced, `:recipe-differs` |
| the CPU twin's 24 dab centres and radii | as bench 9's dab packets | `regress.js` prints them; `extract.js` runs the bench's pure declarations in Node | the first comparison the landing runs when the texel differs (§5.4) |

Tolerance: 1e-6 per component against the definer's decimal, because the client computes in doubles and stores in float32 as the benches did but need not order its operations identically.

### 5.4 Hidden work, named

- **"paint" hides the coverage.** The texel's third decimal depends on the coverage arithmetic at texel centres matching bench 9's `coverageAt` (the region lowered at a quarter texel, bandless, the winding rule's coverage saturated). The client's `pack/coverage-at` is the same twin ported, and the crossing check already matched the bench's `.8556` through it, which is evidence, not proof, for a 24-dab accumulation. Receipt order when the texel differs: the dab packets first (positions and radii against bench 9's), then one dab's coverage at (64, 64) against the bench's function in `bench-9/node-route` (its `bench.js` is extracted from the html by `extract.js`), then the over.
- **"sample nearest" hides a convention:** floor of the mapped point, so (64.5, 64.5) under the identity map is texel (64, 64); the 3D painting's centre convention is (x + ½). Both stated in §4.1; a landing that rounds instead of floors reads a neighbour.
- **"the continuation as bytes" hides the encoding:** little-endian float32 under base64, the same bytes from a `js/Float32Array` and from a JVM `ByteBuffer` in little-endian order; the round trip is the receipt and the byte count is not.
- **"the dab's centre" hides a field name:** `:x :y` at HEAD; if the slice renames the packet, the record's two `[:get :dab …]` change and nothing else (§13).

---

## 6. The 3D kind's coating-reading brush through them

### 6.1 What the sphere brush needs, and what it is made of

Every number of the brush is a CPU value of four things: the support (the sphere G with its charts at three revisions), the coating (two retained records bound through chains at each revision, composed by a saved order, the merge step charged one unit of work at G@2), the painting (a chart patch), and the record. None of it exists in the client. All of it is pure and runs under the JVM suite. It lands as four namespaces beside the 3D tree, `.cljc`, ported by name from the seam bench's sidecar `bench-2/node-route/sweep-and-sphere.js` (its functions are named below) and `executor.js`, with the sidecar's probes as the tests:

| namespace | holds | ported from |
|---|---|---|
| `region3d/support.cljc` | the support value `{:kind :sphere :R 200 :charts […] :revisions n}`; unit vectors from (lon, lat); the great-circle distance (the intrinsic metric, the only `distance` this slice answers); chart S0 (u = R·lon, v = R·lat) and its authored branch (u around πR); the charts S1 and S2 as maps to and from S0 with their periods; the chart→point and point→chart functions (§4.4's `:chart` domain); the great-circle arc `kA/arc-AB` and its point at t; the chart piece of a point (west, east, north) | `sphUnit sphAngle sphDist sphLon sphLat sphChart0 sphPoint0 sphPreimages sphArc sphArcDistClosed sphPieceOf gArcCentre gUnwrapU G_CHARTS gChartMetric` |
| `region3d/coating.cljc` | a retained record (its root domain, its mark, its colour); the bindings at a revision, each a chain of relations from the record's root to the current chart with its work; `locate(support, revision, point, {:budget :order})` → the composition: resolved with colour and contributors, unresolved with the missing dependency named and the known marks, needs-policy with candidates, empty; the root restriction tested on every branch's preimage | `G_RECORDS gBeta gBetaInv gBindings gMarkA gMarkB gLocate` and `over` |
| `region3d/surface_region.cljc` | the retained region: built from a seed or a point and a radius on a support; owns a copy of the record it was built from; membership as the same computation as the printed distance; area, bounds (world mm about the centre), chart pieces, `saturated` at radius ≥ πR (the whole sphere, area 4πR²); a negative radius refused; `member(region, point)` | `reachRegion sphFrozenDist` and the executor's `surface-region`, `member`, `xRegionPieces` |
| `region3d/capabilities.cljc` | the 3D table: `:curve-point`, `:surface-region`, `:member`, `:read-surface` (assembles the stack: the word `"coating"` → a layer `{:layer/kind :coating :sample (locate …) :snapshot (…)}`, a painting → itself; calls `surface/sample` with the chart domain and the budget), `:mix`, `:paint` (the engine's, bound with `member` at the texel centre's point and the chart domain), `:sample`, `:surface/new`; `:vocabulary` the sentence `executor.js` carries as `X_CAPABILITIES`, re-worded for the client | `executor.js` `X_OPS` |
| `region3d/records.cljc` | fixtures: G with A@0 and B@0, S1 and S2, the four definer records (`reach`, `sequence`, `pickup`, `clip`) translated as §6.3 | `records.js`, `G_RECORDS`, `G_CHARTS` |

The general smooth or trimmed host (a distance operation with an error argument, or a declared mesh authority) stays owed as HANDOFF-5 §6.2 says; the support protocol above is written so that a second support kind adds a namespace and no change to `coating`, `surface_region` or the table: `distance`, `chart->point`, `point->chart` and `pieces` are the support's four functions and the rest reads through them.

### 6.2 What of the 3D tree must move, and no more

**Nothing that stands in `src/app/client/region3d/` moves for the sphere brush to run there.** The five namespaces above are added beside it; `component.cljc`, `scene.cljc`, `frame.cljc`, `on_plane.cljc`, `on_plane_renderer.cljs` and `renderer.cljs` are not touched by the 3D landing, and `harness/region.cljs` gains one leg (§6.4). Supports: the brush's every receipt is a value of the support, the coating, the painting and the record; none reads the region row, the pick, the mesh kind dispatch, a placement or a pass. HANDOFF-4 §6.7's list, at Sid's word only, item by item, and why it is not this slice: the region row split into space, view, portal and lease is what puts a painted sphere on screen through a view, and this slice puts nothing on screen; segment counts and required normals out of the value is the same slice; placed ink in the pointer map is the pick, which the brush never calls; the aspect declared by the portal is the view; one grammar for the question protocol is the hit, which the brush never asks; the executor's spatial door is the pick again; the surface read bound into the executor is this slice, and it binds through the capability table, not through the region row. The mesh kind dispatch stays closed (`component.cljc:386-395`); the `:sphere` there is a mesh primitive, and the support here is a value of a new kind; when the sphere goes on screen, a thing's definition is the support and the primitive is its tessellation at a tolerance, which is Position 10's "a thing is a placement of a definition" landing in code, and that is the next slice's first move. Becomes a default only at Sid's word that the painting must show on the sphere in the verifier's picture in this slice; what changes then is the region3d fragment shader reading a coating texture through the chart and the material path opening, with no definer number to judge it by.

### 6.3 The record, in the client's grammar

The definer's attack 4 record as `records.js` carries it (`coating-pickup@0`), translated field for field; the reach, sequence and clip records translate the same way.

```clojure
{:id "coating-pickup@0"
 :support  {:id "G" :revision 2}
 :coating  {:layers ["A@0" "B@0"] :bindings ["binding-A@2" "binding-B@2"] :order ["kA" "kB"]
            :interpretation "linear-light premultiplied rgba; no lighting, no screen lease"}
 :painting {:surface/id "pickup-G" :width 64 :height 32
            :domain {:kind :chart :chart :S0 :rect [588.3185307179587 194 80 30]}
            :color :linear-premultiplied-rgba :filter :nearest :initial [0 0 0 0]}
 :tool     {:carry [1 0 0 1] :pickup 0.25 :opacity 0.5 :radius 8.75}
 :events   [{:id "dab-0" :curve "kA/arc-AB" :t 0.65} {:id "dab-1" :curve "kA/arc-AB" :t 0.7}
            {:id "dab-2" :curve "kA/arc-AB" :t 0.41} {:id "dab-3" :curve "kA/arc-AB" :t 0.65}]
 :program
 {:steps [{:out :fresh :op :surface/new :args {:declaration [:get :painting]}}]
  :each  {:items [:get :events] :item :event
          :state {:carry [:get :tool :carry] :painting [:get :fresh]}
          :steps [{:out :at        :op :curve-point    :args {:curve [:get :event :curve] :t [:get :event :t]}}
                  {:out :picked    :op :read-surface   :args {:support [:get :support] :point [:get :at]
                                                              :layers ["coating" [:get :state :painting]]
                                                              :order [:get :coating :order] :filter [:get :painting :filter]}}
                  {:out :carry     :op :mix            :args {:a [:get :state :carry] :b [:get :picked :color] :amount [:get :tool :pickup]}}
                  {:out :footprint :op :surface-region :args {:support [:get :support] :point [:get :at]
                                                              :radius [:get :tool :radius] :distance :surface}}
                  {:out :painting  :op :paint          :args {:surface [:get :state :painting] :region [:get :footprint]
                                                              :rgba [:get :carry] :opacity [:get :tool :opacity] :blend :source-over}}]
          :next  {:carry [:get :carry] :painting [:get :painting]}}
  :return {:painting [:get :state :painting] :carry [:get :state :carry]}}}
```

The caller's roots: `host` (the support value with its retained records, so a record may write `[:get :host :A :radius]` instead of retyping 16; the cold mind's twelfth) and `inputs` (a region or painting another record returned, checked as §6.5 says). The recipe (§3.5) is this record without `:id` and `:events`.

### 6.4 The numbers and where they are pinned

All from `probe-4.mjs` (its constant `D`, lines 62 to 72, and its sections A to F) and `probe-2-3.mjs`; each becomes a test under `test/app/client/region3d/`.

| receipt | value | test |
|---|---|---|
| the four reads' colours | (0.25, 0, 0.5, 0.75) · (0.5390625, 0, 0.328125, 0.8671875) · (0.5, 0, 0, 0.5) · (0.6631851196289062, 0, 0.2650909423828125, 0.9282760620117188) | `brush_test`: per dab, exact to 1e-9 (the arithmetic is dyadic) |
| the four carries | (0.8125, 0, 0.125, 0.9375) · (0.744140625, 0, 0.17578125, 0.919921875) · (0.68310546875, 0, 0.1318359375, 0.81494140625) · (0.6781253814697266, 0, 0.16514968872070312, 0.8432750701904297) | the same |
| contributors per dab | `kA, kB, pickup-G@0` · `kA, kB, pickup-G@1` · `kA, pickup-G@2` (B outside its root at t = .41) · `kA, kB, pickup-G@3` | the same, exact |
| texels changed per dab | 401 · 321 · 405 · 401 | the same, exact |
| the painting's bytes | sha256 `30cb13f2147784bdae44b4aa303d412c8980f00c09435c0983521cd291d75579` | the sha256 of the float32 bytes equals the definer's; if it does not, the components within 1e-6 and the hash difference reported as a finding (operation order), never silently loosened |
| the three-run equivalence | eager (grant 1 from the start), delayed (grant 0 → suspended → resume with 1), cold (the continuation encoded to bytes, decoded in a fresh process or a fresh JVM, resumed with 1): identical state, history and bytes | `brush_test`: the three summaries equal by value |
| withheld | budget 0: `:suspended :pending` at 0, carry still [1 0 0 1], the painting at revision 0, an empty history, `:missing` naming `B@0 → M@2` at the demanded chart point, `:known ["kA"]`, three events queued; a second withheld run gives the identical continuation | `brush_test` |
| the guess | `:pending :provisional` on the read: first carry (0.875, 0, 0, 0.875), final bytes `c1cf8170…`, every later carry different, the history rows marked provisional | `brush_test` |
| stale | an answer demanded under `[kA kB]` delivered into the reversed order: stale, empty history; the reversed run itself completes with first read (0.5, 0, 0.25, 0.75) and bytes `ae8c231c…`; no order: needs-policy, candidates `kA, kB`, no next state; a consumed answer delivered again: stale; pickup .25 → .5: the old answer stale, the new painting `4df8a532…`; the first painting as a declared input: `a5aba6ed…`, another painting under that subject refused | `brush_test`, one assertion each |
| the sequence | one authored point read at G@0, G@1, G@2: (0.25, 0, 0.5, 0.75) at each; with budget 0 the third read pending at B's merge | `coating_test` |
| the reach | 628: the antipode outside by member and by distance, area 502654.50582273136; 629: inside, area 4πR², bounds [−200, 200]³, `saturated`; 2πR: the whole sphere with ordered bounds; the caller's 150 → 140 does not reach into the result (it reports 150); a negative radius refused; zero is the seed alone; q1 inside at 144.54684956268315, area 67433.94227378976 | `surface_region_test` |
| the clip | the cold mind's dab through the reach as a clip: 6096 texels changed (7502 unclipped is the cold author's control, not asserted) | `brush_test` |

The harness leg `run-coating-brush!` in `harness/region.cljs` runs the brush eagerly on the CPU in the browser, writes the numbers above and the painting as a PNG (64 × 32, scaled by an integer for the eye) into the dump beside the region3d results, and records the painting's sha256 as a text golden. No pixel of the region3d passes changes; the goldens under `test/app/fixtures/render_engine/gpu-goldens/` are untouched by this landing.

### 6.5 Hidden work, named

- **"read-surface" hides the binding inversion.** `gLocate` tests every branch's preimage (k = −1, 0, 1 around the chart's period) against the root restriction, charges B's work against the budget before any inversion, composes by the order with `over`, and names the missing dependency in the chain's last relation's words. Port it by name; `coating_test`'s sequence numbers and the third dab's contributors (`kA, pickup-G@2` without kB) are the receipts that the restriction and the branches are right.
- **"surface-region" hides `pieces` and `saturated`.** `xRegionPieces` names the chart pieces the region meets from the region, not from its seed (the cold mind's tenth), and saturation is a comparison against πR before any distance: the reach at 629 mm is the whole sphere. The receipts are in the reach row.
- **"inputs" hides the subject check.** A record's `:inputs {:clip {:kind :surface-region :from {:record "reach@0" :output :region}}}` names another record's returned value by record and output (bench 9's `{result, output}` form, kept); the caller hands the value in under `inputs`; the executor checks that the value's own `:record` (the copy the region owns) equals the record the name resolves to, by value, and refuses `:stale` otherwise. On the bench the subject was a printed hash the author copied into the record; in the client it is the record itself, which is what Sid's rule and Position 9's "the result keeping the subject it was built from" say. The printed hash stays in receipts.
- **"the vocabulary sentence" hides a version.** The seam bench's `X_CAPABILITIES` sentence is the bench's; the client's table declares its own, and a continuation written by the bench does not load into the client (a different sentence, refused with the reason), which is right.
- **"pending" hides where the barrier is checked.** Only a read step (one whose capability declares `:snapshot`) can suspend; a `mix` that receives a pending value is the executor's error, not a suspension, because the executor never runs a step on an unresolved read (§3.5). A landing that lets `mix` see the pending map has built the barrier in the wrong place.

---

## 7. The hardest case, moment by moment

The coating-reading brush on G at revision 2, the record of §6.3, through the executor of §3 and the compositor of §4.

1. **Withheld.** `run` with `{:budget 0}`. The pre-loop step makes `fresh`, the painting at revision 0. Item 0, `dab-0`: `at` = the arc's point at t = .65, a unit vector. `picked`: the executor calls `read-surface`'s `:snapshot` first and forms the request: the recipe, no consumed items, the state before (carry [1 0 0 1], the painting at revision 0 with its bytes), at 0, step `picked`, and the snapshot (G at revision 2, the two retained records, the order [kA kB], the chains [φ] and [β φ χ], the painting at revision 0, nearest, the point). No answer is supplied, so `:run` runs: the stack is [coating, painting@0]; the coating layer's sampler is `locate` at revision 2 under budget 0: binding A's work is 0, it resolves, its mark holds the point; binding B's chain ends in χ with work 1 > 0, so B is unresolved with the dependency named; the layer returns pending with `:known ["kA"]`; `sample` returns pending with that snapshot. The value of `picked` is a read whose status is not resolved, so the executor stops before `mix`: no `:next`, no history row. It returns `:suspended :pending` at 0 with the request, the missing dependency and the continuation: schema, the vocabulary sentence, the record, the scope (`host`), at 0, consumed [], the state before (the painting's bytes under `:float32le`), an empty history, the request, the missing dependency. A second run with budget 0 returns an equal continuation.
2. **Granted after.** `resume` with `{:budget 1}`. The vocabulary sentence agrees; the recipe agrees (the same record); no consumed items to check; the state decodes to its declared length. The pre-loop step runs again and its output is not used by the restored state. Item 0 runs from its first step: `at` recomputed, the same vector; `picked`'s request formed again, equal to the stored one in all six parts; `:run` under budget 1: B's work 1 ≤ 1, both marks resolve, the composition by the order gives (0.25, 0, 0.5, 0.75) with contributors [kA kB]; the painting layer is transparent there and is a contributor; the read is resolved. `carry` = mix([1 0 0 1], (0.25, 0, 0.5, 0.75), .25) = (0.8125, 0, 0.125, 0.9375). `footprint` = the region of surface distance 8.75 about the point, owning its record. `painting` = paint(painting@0, footprint, carry, .5): 401 texels change, revision 1. `:next` sets the state; the history gains its row. Items 1 to 3 the same, each read granted 1 (nothing accumulates). Final carry (0.678125, 0, 0.165150, 0.843275); the painting's bytes `30cb13f2…`.
3. **Granted before.** `run` with `{:budget 1}` from the start reaches the same values at every step, because a transition consumes the item, the state before, the program and the resolved read, and the grant is not consumed (§3.3); and because the withheld run left no partial output in the continuation (§3.5, the item re-runs from its first step). Eager, delayed and cold are equal by value, which is the definer's three-run equivalence and Position 12's receipt.
4. **Cold.** The continuation encoded to bytes and decoded in a fresh JVM (the test) or a fresh browser (the harness) resumes to the same bytes: the load checks the schema, the sentence, the lengths; the resume checks the recipe and the consumed items; nothing else is needed, because the continuation holds every input of the transition.
5. **An answer that does not belong.** The read demanded under `[kA kB]` (`run` with `{:stop-at :picked :budget 1}` returns `:answered` with the request and the colour, committing nothing) is delivered into the record with the order reversed: the request's snapshot differs in its order, so the run is `:stale` naming the snapshot, with an empty history; the reversed run completes on its own with its first read (0.5, 0, 0.25, 0.75). The pickup edited to .5: the recipe differs, stale. Delivered a second time after it was consumed: the state before differs, stale. No order saved: the layer returns needs-policy with candidates; the run suspends with no next state.
6. **Provisional, by declaration.** `:pending :provisional` on `picked`: the compositor composes kA with B transparent, the read carries the provisional mark, `mix` consumes it: first carry (0.875, 0, 0, 0.875), and every later carry differs from the granted run's. The history says so at every row; the selection was the record's declared choice, retained, and not a race.
7. **The same executor on the 2D pickup.** The record of §5.2, `run` with `{:until 12}`: twelve dabs run, the run suspends at 12 with a continuation holding the twelve consumed dab packets and the surface after twelve paints; `resume` runs dabs 12 to 23 to the same 65,536 components as the straight run; through bytes the same. The four edits: the first sample's pressure changed alters dab 0, so the consumed items differ at 0 and the resume is refused; the last sample moved alters dabs from 12 on, the consumed items agree, the resume runs the new dabs; the pickup changed alters the recipe, refused; the surface redeclared alters the recipe, refused. No read of the pickup's surface can pend, because the surface is its own; the `pending` branch is never entered, and the same code runs both brushes.

---

## 8. The push edge

**What the slice leaves.** The path renderer takes the whole list of draw items every frame, computes one key per item, and returns early when the frame's key is unchanged; otherwise it walks every item, checking each level's key against its cache. The slice removes the run cache and makes the path value the renderer's input, so the walk is over values; it is still a walk over every item on every edit (finding 1: about 22 µs per unchanged item, 35 ms at 1,600 for a colour edit; the floor of keys alone 1.2 ms at 1,600). The push edge removes the walk: the caller names what changed.

**The edge as data.**

```clojure
(path-renderer/push! system {:upsert {material-id draw-item …}   ; a draw item as the slice defines it: the component
                              :remove #{material-id …}             ; (its path value and paint), its group, its buffer index
                              :order  [material-id …] | nil        ; the draw order, when it changed
                              :groups world-transforms | nil})     ; the group registry, when it changed
→ {:reran {:geometry #{id …} :pack #{region-key …} :rows #{id …}}}

(path-renderer/frame! system view)                                  ; per frame: the camera and the view facets only
→ {:changed? bool :reran {:geometry #{id …} :pack #{…} :rows #{…}}}
```

`push!` runs L1 to L5 for the named items and no other; `frame!` writes the camera and runs L1 to L5 for the items whose geometry declared a view facet (a device-unit width reruns when the projected scale changes; a snapped record when the pan's fraction changes; nothing else reruns on a pan) and for every region whose bucket crossed on a zoom (a region without cubics packs once for every bucket and never reruns). The renderer keeps the set of view-dependent items as the items are pushed, from their declarations, so `frame!` visits only that set; a group's move is a camera-level write and visits nothing; a group's scale enters through its items' bucket. `prepare-path-frame!` and `frame/frame-key` (the whole-list walk and its key) are deleted; `item-key` stays as the per-item value the rows are keyed on.

**Sid's rule as tests** (`frame_test`, extended): a colour edit reruns rows for that id and nothing else; a knot edit reruns geometry, pack and rows for that id only; a pan reruns nothing for an unsnapped scene and geometry for exactly the snapped ids; a zoom inside a bucket reruns nothing; a zoom across a bucket reruns pack for exactly the regions with cubics; a group move reruns nothing; a push of one item among 1,600 visits one. `:reran` is the receipt each test reads. The measurement after landing is the scale trace and `timing.clj` re-run: an edit at 1,600 costs what changed plus nothing, against finding 1's numbers.

**Where the reactive question lands, and what this slice does about it.** Nothing in the client is Missionary today (zero occurrences under `src/app/client`; every asynchronous edge is a Promise or `onSubmittedWorkDone`). The push edge is a function in this slice; the harness calls it. When the store's courier connects, a flow of diffs reduces into `push!` (`m/reduce` over the diff flow with `push!` as the reducer), and a flow of grants resumes continuations the same way; the laws that shape that wiring are the skill's L6 (a wide `m/latest` reruns whole; one flow per item, not one over the scene), R3 (an unconditional frame tick with an `identical?` skip at the consumer; nothing scheduled inside a combine function) and L11 (`m/relieve` on every DOM source). That wiring is not in either slice.

**Callers of the edge** (at HEAD; re-checked at the amend): `harness/path.cljs:75, 161, 358, 458` (`prepare-path-frame!`) and `:79, 464` (`draw-path-frame!`), `harness/region.cljs:1180` (the surround's path frame) and `:360-362` (`draw-path-instances!`, `item-range`). `region3d/on_plane_renderer.cljs` does not call the path renderer; it packs placements itself from the kind's regions and keeps its own per-placement reuse, untouched here.

---

## 9. One slice or two

**Two, in order, one landing session each.** The reasons: the executor's pending branch needs a read that can pend, and the only such read is the coating's, so the branch lands with the 3D brush or lands untested; the path pickup's host exists in the tree (the filler's twin, the dabs, the path value) while the 3D host is five new namespaces, so the shared pieces are first exercised against code that is already there and their surface of new work stays small; and the second slice then extends the first through the interfaces it left, with the first's records kept passing, which is the extension that answers whether the waist holds (two-chairs: "the extension through the existing interfaces"). Two-chairs' "Codex lands per kind" is this order.

**Slice A, the path kind's pickup** (the landing paste written from this file in `path-kind/STARTER-production.md`'s shape; the handoff `path-kind/production/HANDOFF-2.md`): the executor of §3 with `:until` suspension, the continuation and its bytes, resume with the recipe and consumed-items checks, the reads report, the refusals, and no pending branch; `engine/surface.cljc` with the surface value, `new`, `paint` in its colour kind, `sample` over a stack of surface layers, `mix`, encode and decode, `png-bytes`; the path kind's table with `:path/dabs` and the engine's operations bound; the pickup record and its five receipts (§5.3); the push edge with its tests and the re-measurement (§8); the deletions (§10); the READMEs and docstrings in the same commits.

**Slice B, the 3D kind's coating-reading brush** (the landing paste is `3d/STARTER-production.md` §2 as written, with one sentence added at its terrain: the executor's pending branch is its first milestone; the handoff `3d/production/HANDOFF-1.md`): the executor's pending and needs-policy suspension, `:snapshot` on read capabilities, the request, belonging by value, the provisional mark; a non-surface layer in `sample`'s stack and the `:chart` domain through `ctx`; the five namespaces of §6.1; the brush and its receipts (§6.4); one harness leg; the READMEs.

Becomes a default only if the definer's next 2D record needs a read that can pend before slice B starts (a surface another record returned across the store, withheld), in which case the pending branch moves into slice A and slice B's executor work is nil. What changes with the cut: two pastes and two handoffs instead of one; slice B's landing session reads `HANDOFF-2.md` before this file's §3.

---

## 10. Affected callers by name, and the deletion scope

Line numbers at `f1a7200`; the amend (§13) re-checks them against the slice's handoff.

**Slice A changes:** `engine/executor.cljc` (rewritten to §3); `engine/surface.cljc` (new); `engine/compositor.cljs` (three deletions; docstring); `engine/README.md` (the piece's two files; the executor's row); `path/records.cljc` (the recipes in named-argument form; the pickup fixture); the path kind's capability table wherever the slice put it (`component.cljc` at HEAD, `records.cljc` in the Codex lane); `path/renderer.cljs` (`push!`, `frame!`; `prepare-path-frame!` and the run cache gone); `path/frame.cljc` (`frame-key` gone); `path/README.md`; `harness/path.cljs` (the six call sites above; the pickup leg); `harness/region.cljs:1180` and `:360-362`; `test/app/client/engine/executor_test.clj` and `surface_test.clj` (new); `test/app/client/path/pickup_test.clj` (new), `frame_test.clj` (extended), `run_pure.clj` (the new namespaces added to the fast lane); the text goldens: the pickup's sha256 and texel, recorded on purpose with the reason in the commit.

**Slice B changes:** `engine/executor.cljc` (the pending branch); `engine/surface.cljc` (layer kinds; `:chart` domain); `region3d/support.cljc`, `coating.cljc`, `surface_region.cljc`, `capabilities.cljc`, `records.cljc` (new); `region3d/README.md`, `engine/README.md`; `harness/region.cljs` (one leg); `test/app/client/engine/executor_test.clj` (pending, with a toy read whose `:snapshot` and `:run` the test defines); `test/app/client/region3d/coating_test.clj`, `surface_region_test.clj`, `brush_test.clj` (new); the text golden of the painting's sha256.

**Deleted, as wrong form, not as uncalled:** `engine/expression.cljc` (the slice; one language); the base's `reread`, `rerun?`, `run-for` and the `!runs` atom (the slice; a cache keyed on observed reads); `compositor.cljs` `strip-padded-rows`, `unpremultiply!`, `png-bytes!` (slice A; a read-back duplicated where nothing reads); `path/renderer.cljs` `prepare-path-frame!` and `frame/frame-key` (slice A; the walk the push edge replaces); the Codex executor's nested `each` step and positional `:args` (slice A; §3.1).

**Untouched by both slices:** `engine/coverage.cljs`, `engine/device.cljs`, `engine/buffer_pool.cljs`, `engine/leases.cljs`, `engine/rungs.cljc`, `engine/limits.cljc`, `engine/transform.cljc`, `engine/color.cljc`, `engine/schema.cljc`; every file of `region3d/` that exists today; `text/`, `image/`; the GPU goldens.

---

## 11. Unresolved choices, each a position with its condition

1. **Named arguments** (§3.1). Position: named. Stops holding: never for a record; a landing that finds the expression compiler wants positional forms keeps expressions positional and steps named.
2. **The loop at the program's level with pre-loop steps** (§3.1). Position: taken. Stops holding when a definer record needs a second loop or a loop inside a step; then the exit is the executor door's own (a designed total language in the leaves), asked of Sid, never a nested loop slipped in.
3. **A read is a map with a status, in both kinds** (§4.3). Position: the 2D `sample` returns a read, and the pickup's `mix` takes `[:get :sample :color]`; bench 9 returned the colour bare. Stops holding: no condition found; the barrier rule needs the status.
4. **Outside a surface's domain is transparent** (§4.1). Position: taken, with `:covered? false`. Stops holding if a tool needs the edge texel (a clamp-to-edge brush); then `:filter :clamp` is a declared filter, not the default.
5. **`:source-over` only** (§4.2). Position: taken. Stops holding at the first record that names another mode; the table grows by one function and one test.
6. **The subject of an input is the record it was built from, by value** (§6.5). Position: taken. Stops holding if a record's copy is too large to carry inside every result on a real document; then a content address computed from the record and checked against it, never a hash as the identity.
7. **The CPU runner first, the GPU road after a trace** (§4.5). Position: taken, under Sid's rule and Position 9's reference runner. Stops holding when a real brush at a real size shows the twin short of the experience; the measure is the frame, and the first thing the GPU landing checks is the 1.2e-7 agreement.
8. **Nothing of region3d moves** (§6.2). Position: taken. Stops holding at Sid's word for the painting on the sphere's screen image, which is the region-row split's slice.
9. **History is not part of the request** (§3.5). Position: taken; the history is derived from the recipe, the consumed items and the state, and pinning it would pin an output. Stops holding: no condition found.
10. **Two slices** (§9). Position: taken; the condition is there.

---

## 12. The exact open question the slice leaves

Who grants work, and where a suspended continuation lives between a refusal and a grant. The executor takes the grant as an explicit input of a run and returns the continuation as a value; in both slices the only caller is the harness, and it grants by hand, which is the bench's shape and reproduces the definer's numbers. It is not the app's shape. The position this file takes, and does not land: the frame caller grants per frame under the render-seam law (the frame never waits), and a suspended continuation is a value the session holds, resumed when its dependency arrives; because the coating's work is per read and per point, a merge step at a chart location, and a row in the store per pending dab would be a row too many. It stops holding when a read's withheld work is a whole correspondence rather than a step of one (a kernel's history for a fillet, seconds, HANDOFF-5 §6.2's general host), and then the continuation is a row with a revision and a worker resumes it, which is the same value under a different owner. Nothing in §3 changes with the answer; what changes is who calls `resume` and what holds the bytes in between. That is the first design question the 3D landing may bring back through Sid, and this session stays open for it.

---

## 13. The amend this file owes

When Sid pastes that the path slice is on `main` with `path-kind/production/HANDOFF-1.md`, this session re-reads what the slice changed and amends, in place: §3.1's expression forms as landed (the width rule's form in §5.2; the numeric operation names); §5.1's name for the edit boundary (`records/construct` or as landed) and where the path kind's capability table lives; §5.2's dab field names if `stroke/dabs` changed its packet; §8's caller lines; §10's file list against the handoff's; and this header's hash. Nothing in §1, §2, §3.2 to §3.6, §4, §6, §7, §9, §11 or §12 depends on the slice.
