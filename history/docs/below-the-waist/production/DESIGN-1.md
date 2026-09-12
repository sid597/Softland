# DESIGN-1 — the executor and the compositor's two operations, with two brushes as their first consumers

Design session: Fable at max (load at low), 2026-09-07. Written against `main` at `f1a7200` while the path kind's first production slice landed beside it; repaired in place the same day, against `REFUTE-1.md` (Codex, `564652f`: twenty-two findings, each with an executed receipt, and two further rounds on the subject's reading, appended there) and against `main` at `1501b85`, where the path slice is on `main` (`e8dcd37`, its handoff `path-kind/production/HANDOFF-1.md`) and every name below is read from the tree. The amend §13 owed is paid in this text; §13 now says what the file is written against.

This file is the design as data (two-chairs, the production section): the values exchanged, each level's inputs, who owns which dependency and which effect, the record examples, every affected caller by name, the deletion scope, the unresolved choices, and Sid's rule in words. It is not a contract and not a test suite before the code. The landing sessions are Codex in fresh contexts; a landing does what this file says and nothing it does not say, so this file says everything, and where it takes a position it says what supports it, when it becomes only a default, and what else changes with it. The measure a landing is judged by is the definer's numbers, reproduced through the client.

---

## 1. The rulings this design runs under

Verbatim, with times, because a paraphrase is not a ruling.

- **The executor** (Sid, 2026-09-06 11:27): "executor yeah", to the path page's Position 8: records over a vocabulary of capabilities, an ordering rule and compiled expressions; instances many, the definition once.
- **The compositor** (Sid, 2026-09-06 11:51): "Lets do A", of four options: one piece with two operations, paint and sample, owning clips, blends, layers, the screen and the surface a brush reads, painting and vector sharing it. Carried to the 3D page and not re-asked: read-surface is that sample operation bound to a 3D binding, not a second compositor.
- **Position 9, the 3D kind's input** (Sid, 2026-09-06 17:42): "okay. Let's do b. now save all the things that you need to save and whatever the rulings need to go or whatever." The three sentences it rules: the kind takes the recipe as its input and keeps it with the result, the result keeping the subject it was built from; a recipe is a record, steps over a vocabulary of capabilities with formulas in the leaves, run by one executor with several runners, a tool never in git, a capability code below the waist added once with receipts, a solver a capability; nothing in a record is a program, and foreign code enters only as a value in a slot with a declared output. Its exits are on the page and are not re-asked here. The page's same position ends in a production obligation, "Production's first 3D vocabulary starts from five slots: a region as a clip on a read, region algebra, a painting that covers a region, a distance field, a material"; §9 says which of the five these two slices supply and takes a position on the rest.
- **The stance** (Sid, 2026-09-06 11:27 and 11:45): "our aim is not to copy anyone they are just a datapoint we go with the best experience and the best in the world for this field. From the start there is no concept of carrying forward existing solution if that is not how it shoudl be." Applied here to both benches' executors, to the landed executor the slice ported, to the tree's compositor and to the 3D tree as it stands: each is a datapoint and nothing in this file keeps a piece because it exists.
- **No trial** (Sid, 2026-09-06, after 16:37): "i dont think there will be any more trial". The two slices this file cuts are production, judged on delivery.
- **The two reflexes** (Sid, 2026-09-06, 16:37 to 16:54): "fable tries to make shortcuts and codex only does what is told". This file fills no gap silently: where it does not know, it says so and names the receipt that decides.

**Sid's caching rule** (2026-09-06, 16:17 to 16:23): "caching is never an option it means the underlying thing is as it should be and then still need more performance therefore we nede to do caching on top". In this file's words, so that this session's own reflex cannot put a memo back: every level of the data flow below is a pure function whose inputs are explicit as data; a level reruns when its input changes and only then, and the test of the level is that a change to X reruns exactly level Y and nothing else; a memo, where one is ever added, is keyed on the level's full input as a value, never on what a run happened to read and never on a hash; no memo, bucket, batch or atlas is added, and none that exists is credited, without a trace on a representative document that shows the need. Consequences this file draws: the executor keeps nothing between runs and reports what a run read only as a diagnostic; what a transition consumes is declared in the record (the fields of an item, §3.1) and found by reading the program (the roots it reaches, §3.5), never by watching a run; a surface's revision chain is a value's lineage, not a cache; a continuation is a value, not a cache; the push edge is the underlying thing and lands before any per-item cache is discussed again; the pack per region key that exists today is keyed on the region's full input (its outline and rule, with the bucket inside the entry) and is left as it is, credited with nothing.

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
            source · envelope · dabs      surface · paint · sample  curve-point · surface-region
            regions (construction.cljc)   mix · new · blends        member · read-surface
            (coverage from pack's twin)   (a region's coverage is   (coating bindings under
                                          the kind's argument)      a work budget)
```

The levels, and the rate at which each reruns (Sid's rule applied):

| level | pure function | explicit inputs | output | reruns when |
|---|---|---|---|---|
| L0 edit | the kind's recipe at the edit boundary (`construction/construct`, landed) | the record's source, tool, paint, identity | the kind's value (a path value inside a component; a 3D record's authored events) | the record changes |
| L1 geometry | the kind's constructions | the value, the paint's geometry fields, the declared view facets (projected scale for a device-unit width; the pan's fraction for a snapped record) | regions, dabs, the stroke's skin | the value or a declared facet changes |
| L2 program | the executor over the record's program | the program, the roots it reaches, the items as the record declares them, the state before an item, the resolved reads | the state after each item, the returned values, a continuation when suspended | an item, a reached root, or a read's snapshot changes |
| L3 surface | the compositor's paint and sample | a surface value, a region's coverage, a paint, clips, a blend, the result's key; a stack of layers and a point | a new surface value; a read | its arguments change |
| L4 pack | the packer | a region's outline and rule, the bucket | quads, bands, cover | the outline, the rule or the bucket changes |
| L5 rows and frame | the renderer | pack slots, colours, group indexes, clip chains; the camera | instance rows; one draw | a row's input changes; the camera every frame on the GPU |

Nothing below the executor reads a clock, a frame, a grant or a cache; nothing above it is a program.

---

## 3. The executor as data

One namespace, `src/app/client/engine/executor.cljc`, pure, no retained state, the same code on the JVM and in the browser. The landed executor at `1501b85` is the datapoint: `{:bindings :steps :return}`, `:call`/`:bind` steps with positional `:args`, `:each` as a step, `execute` as the entry, `compile-expression`, `evaluate` and `references` as its expression side. This design keeps the expression side whole (names, signatures, the operator table, the refusals) and replaces the step form, the loop and the entry point; §13 names the tree it is written against.

### 3.1 The record's program

```clojure
{:steps  [step …]                      ; run once, in order, before the loop; may be empty
 :each   {:items  expr                 ; a sequence: a root, or the output of a step above
          :item   :dab                 ; the name the current element is bound to (explicit, always)
          :fields [:x :y :path]        ; the item is the element's projection onto these keys (explicit, always)
          :state  {name expr …}        ; the state before the first item; evaluated once, when the loop is first entered
          :steps  [step …]             ; run per item, in order
          :next   {name expr …}}       ; after an item's steps: the whole state for the next item (replaced, never merged)
 :return expr}                         ; over the final scope: the roots, the pre-loop outputs, :state

step = {:out :name :op :capability :args {arg-name expr …}}
       ; a read step may add :pending :wait (default) | :provisional
```

- **Expressions** are the landed language, kept whole: `[:get root k …]` reads a path into the scope (keywords for map keys, integers for vector positions: `[:get :paint :stroke :color 3]`); `[:literal v]` quotes; `[:if t a b]` evaluates the chosen branch; a vector whose head is one of the table's twenty-two operators (`:+ :- :* :/ :min :max :abs :sqrt :pow :sin :cos :exp :floor :clamp :step :smoothstep :mix :< :<= := :not`) applies it, and a non-finite result is `:executor/non-finite`; a vector whose head is any other keyword is refused (`:executor/operator`, "quote literal data"), so a typo is never silently data; a vector whose head is not a keyword is data and its elements are evaluated one by one (so `[[:get :dab :x] [:get :dab :y]]` is a point); a map evaluates its values; a keyword, string, number, boolean or nil is itself; a `[:get]` into a path that does not exist is `:executor/missing-binding` at the step that evaluates it, with the path, and stops the run (`:status :error`); an empty result is never a run that succeeded. There is no second syntax for a reference: a string is always a word, never a path. That closes two of the cold mind's thirteen (a bare name as a reference; references inside arrays) by construction. `compile-expression`, `evaluate` and `references` keep their names and signatures: the width rules (`path/width.cljc:17-27`) compile through them and nothing there changes.
- **Named arguments**, not positional. A capability declares `:args`, every name it accepts, and `:needs`, groups of names of which at least one must be present (`{:args [:support :seed :point :radius :distance] :needs [[:support] [:seed :point] [:radius]]}` reads: support and radius required, seed or point, distance optional). A step's `:args` is a map. Admission: a supplied name outside `:args` is `:unconsumed-argument` (bench 9's honesty check, which the judges scored against the Codex lane for lacking); a group with no name present is `:missing-argument`, naming the group. Supports: both benches, the definer's every record, the cold-mind attack and the landed table all name arguments; the seam bench's table takes `seed` or `point`, optional `distance`, `revision`, `order`, `budget`, `filter`, `clip`, and a default opacity (`bench-2/node-route/executor.js:119-124, 139-146, 176-180`), which required-or-nothing cannot say. What changes with it: `execute`, `run-steps` and the `:call`/`:bind` step forms go; the evaluator of a step evaluates a map of expressions and calls the capability with one map and `ctx`; the L0 recipes (`construction.cljc`'s `default-construction` and the fixtures in `records.cljc`) are rewritten in this grammar (`{:out :built :op :path/source :args {:source [:get :source] :tool [:get :tool]}}`); every capability in the path kind's table becomes a function of one map (`source/build`, `stroke/envelope`, `stroke/dabs`, `component/regions` are positional today and are wrapped in the table, not changed).
- **One loop, at the program's level**, after the pre-loop steps, with explicit carried state and an explicit `:next`. Supports: the two brushes and the reach and sequence records are exactly this; bench 9's built-in construction before its program is the pre-loop steps made explicit; the continuation's shape (§3.5) needs the carried set named, and a scope where every binding is carried (the landed `each`) cannot say what a checkpoint holds. Not in the vocabulary: a nested loop, recursion, a loop as a step, a named definition. The first record known to lie beyond this grammar is bench 9's nested proof record (`path-kind/bench-9/node-route/nested.json:7-15`), whose `keep` maps a record-defined body over `tool.slots` inside the dab loop; it is a bench proof, not a production brush, and it is not translated. When a production record needs that shape, the candidate is a bounded collection step (one level, no loop inside, an explicit output), a vocabulary addition with a test, not the third door; §11.2 holds the position.
- **The state is replaced.** `:next` gives the whole state for the next item; a field not named in `:next` is gone. Both benches merged `next` into the state (`bench-9/waist-bench.html:774-779`, `executor.js:320`), and bench 9's records lean on it (`state.base` survives a `next` that names only `proofs` and `surface`); a translation carries every kept field explicitly in `:next`.
- **The item is its declared fields.** `:fields` names the keys of each element the program may read; the item bound to `:item` is the element's projection onto them, a missing key is `:executor/missing-field` at that item, and a `[:get item k]` in the program with `k` outside `:fields` is refused at admission (`:unknown-field`). The consumed items (§3.5) are these projections, compared by value. This is what makes a checkpoint survive an edit that changes a dab's unread fields: a dab packet carries `:s`, its arc length over the whole subpath's length (`stroke.cljc:422`), which changes for every dab when the last sample moves, while the pickup reads `:x`, `:y` and `:path` and nothing else; comparing the whole packet would refuse the edit the definer accepted (REFUTE-1 F5). The declaration is explicit data, checked by reading the program, never a record of what a run read.
- **No loop.** A record without `:each` runs `:steps` and evaluates `:return`; it has no state, no history and no consumed items, `:until` does not apply to it, and its results have subjects like any other, read from `:return` (§3.5). The L0 recipes and the seam bench's reach record (`records.js:21-35`) are such records.
- **Refused before any step runs** (`{:status :refused :reason … }`): an `:op` no capability answers (`:missing-capability`, with the list); an argument name the capability does not declare (`:unconsumed-argument`); a needs-group with no name present (`:missing-argument`); an `:out` that names a root or `:state` (`:shadows-root`; the cold mind tripped on shadowing, so it is refused rather than allowed, and this file's own records obey it: a paint's output is `:painted`, never the root it started from); a program with two loops or a loop inside a step (`:one-loop`); an `:each` without `:item` or without `:fields`; a read of an undeclared item field (`:unknown-field`); a `:from` input whose subject does not match (`:subject`, §3.5).

### 3.2 The run

```clojure
(executor/run record scope capabilities opts)
;; record        {:program program :roots {name value …}}. The kind's boundary hands the authored record over with
;;               its fields as roots under their unqualified names and its identity fields left out: the path kind
;;               strips the :path/ namespace (construction.cljc:41-43 does this for the L0 recipe today, by hand),
;;               the 3D kind passes its fields as they are without :id. No program references an identity field.
;; scope         the caller-supplied roots as values: {:path value, :host value, :inputs {…}, …}
;; capabilities  {:op {:args [:name …] :needs [[:name …] …] :run (fn [args ctx] → value) :snapshot (fn [args ctx] → value)?} …
;;                :vocabulary "a sentence naming the table's behaviour and version"}
;; opts          {:budget n | nil (unlimited)   ; the grant for reads; never read by the executor itself
;;                :until n                       ; suspend before item n (bench 9's checkpoint)
;;                :stop-at :out                  ; run to that read step, return its request and value, commit nothing (the seam bench's xDemand)
;;                :answer {:request r :value v}  ; an answer delivered for a pending read (§3.5)
;;                :records {"name" record …}}    ; the producer records a :from input names, resolved by the caller (§3.5, §6.5)
→ {:status :complete  :results {…} :subjects {out subject …} :state {…} :history [row …] :log [{:out :op :status :ms} …] :reads {…} :unread […]}
| {:status :suspended :reason :until | :pending | :needs-policy  :at n  :continuation c  :request r? :missing m? :read v?}
| {:status :answered  :at n :step :out :request r :value v}      ; :stop-at reached; nothing committed
| {:status :refused   :reason … :detail …}
| {:status :stale     :reason …}
| {:status :error     :at n :step :out :error msg :data …}      ; a read whose status is :unsupported ends here, its reason under :data
```

The scope the program sees is the record's roots merged with the caller's roots; a step's `:out` binds its value into the scope for the steps after it and for `:return`; inside the loop, `:state` and the item name are roots too. `:results` is the value of `:return`; `:subjects` carries, for every name in `:results`, the subject of the computation that produced it (§3.5). The capability table is per kind, assembled by the kind from the engine's operations bound with the kind's coverage and domain functions (§4) and the kind's own operations; `:vocabulary` is a sentence the continuation carries and a load checks by equality (§3.5).

`ctx` passed to a capability: `{:budget :at :step :record}`; a capability that needs the grant compares against `:budget` per call and nothing accumulates (the seam bench's rule: work charged per read, nothing cached across reads; a run of four dabs at G@2 needs a grant of 1 on every read). A capability that makes a surface forms the result's key from `:at` and `:step` (§4.2). The executor never reads the budget, never decrements it and never decides it: the grant is the caller's, and §12 is about who that caller is.

### 3.3 The transition

Per item, in order: bind the item (the projection); run the steps; evaluate `:next` over the scope (roots, `:state`, the item, the step outputs); the new state replaces the old; a history row is appended: `{:at n :item item :state-after state :steps [{:out :op :status …} …]}` where a read step's entry carries the read's `:status`, `:contributors`, `:snapshot` and its provisional mark when it has one, and a paint step's entry carries `:changed`. A transition consumes exactly the item as declared, the state before it, the program, the roots the program reaches and the resolved reads; nothing else, and in particular not the grant, not a clock, not the history. That sentence is the invariant the hardest case rests on (§7): the state after an item is the same whatever the timing of the grant, because the grant is not consumed.

### 3.4 What a run read, as a diagnostic

`:reads` maps every `[:get root …]` path evaluated against the original roots (never a step output) to the value it read, in order of first reading; `:unread` lists the record's root fields no read reached (bench 9's "fields with nowhere to go"; the definer's honesty check). Neither is a key to anything. The base's `reread` and `rerun?`, which keyed a cache on reads, went with the slice's boundary port and do not return.

### 3.5 Suspension, the continuation, resume, and what belongs

A run suspends in three cases: `opts :until` reaches an item boundary; a read step's value is `pending`; a read step's value is `needs-policy`. In every case the run stops before `:next`: the state, the history and the position stay what they were, and the result carries a continuation. A step consumes only a resolved read: a step whose argument is a read whose `:status` is not `:resolved` is never run. A read step is admitted before the loop as well as inside it; a pre-loop read that pends suspends the run before the loop is entered (`:phase :before-loop` below).

**The continuation is a value:**

```clojure
{:schema      "softland/executor-continuation/1"
 :vocabulary  "…"                          ; the table's sentence, checked by equality on load
 :record      record                       ; {:program :roots} as run
 :scope       {…}                          ; the caller's roots as values (a path value; a host; inputs)
 :phase       :before-loop | :in-loop      ; whether the loop has been entered
 :at          n                            ; the index of the item not yet completed (0 before the loop)
 :consumed    [item …]                     ; items 0 … n−1 as the projections the record declares
 :state       {…} | nil                    ; the state before item n; nil before the loop; a surface's :data as bytes (§4.1)
 :history     [row …]
 :request     r | nil                      ; when suspended at a read
 :missing     […] | nil}                   ; what the read named as withheld, or the candidates without a policy
```

As bytes: EDN, with every typed array encoded as `{:float32le "<base64>" :length k}`. Every value in a continuation is data: a surface is its map with its bytes, a region is its map (§6.1), and nothing callable is ever inside one (the seam bench's region carried `member` and `distance` closures and did not survive JSON, `sweep-and-sphere.js:167-182`; here membership and distance are functions of the region namespace over the region's data). A load checks the schema, the vocabulary sentence by equality, every array's declared length against its surface's width × height × 4, and decodes; it refuses with a reason (`{:status :refused :reason :load …}`) when any does not hold. A changed interpretation is a refusal, never a silent conversion (attack 4's exit, carried). The byte count is the client's own, not bench 9's 364,786 (that number was the bench's JSON and is the tracer's, not the definer's); the receipt is the round trip, §5.3.

**The recipe.** One program is read two ways for two questions: what a resumed transition depends on, which is the recipe here and starts from the loop, and what a returned value depends on, which is the subject below and starts from `:return`; they part on a record without a loop and on the roots only `:return` reaches, and a landing keeps them apart. The recipe is the program and the values of the roots the transition reaches, wherever they came from. A root is reached when a `[:get root …]` occurs in the loop's `:steps`, in `:next` or in `:state`'s initial expressions; a `[:get out …]` on a pre-loop step's output is read as that step's `:args`, transitively, down to roots; the `:items` expression is not read (its root is the item source, compared item by item below); `:return` is not read (it consumes the final state); `:state`, the item and a loop step's output are not roots. Found by reading the program, never by running it (the landed `references`, applied per expression, is that reading). A reached root is in the recipe by value whether the record or the caller supplied it: the pickup's recipe is its program with `tool` and `paint` (reached in the loop) and `surface` (reached through `fresh`, the pre-loop output `:state` initialises from), and not `path`, which only the pre-loop dabs step reaches, so an edit to the path is judged through the items alone (§5.3's four edits); the sphere brush's recipe is its program with `support`, `coating`, `painting` and `tool`, and with the caller's `host` the moment a record writes `[:get :host :A :radius]`, so a host whose radius changed under a saved continuation is `:recipe-differs` and cannot reuse the old prefix (REFUTE-1 F6's witness: the bench accepted it and painted the wrong two dabs). A root only the pre-loop steps reach is not in the recipe, and a loop step that reads a pre-loop output directly (`[:get :dabs 0]`, which no record does) pulls that step's roots in, which is conservative and sound. The identity fields never enter: no program references them.

**Resume:** `(executor/resume continuation capabilities opts)` runs `record` (or `opts :record`, when the caller resumes an edited record) over `scope` (or `opts :scope`) from `:at`, with the table given as an argument, because a cold process has nothing else to bind the vocabulary to (the seam bench reached a global `X_OPS`, `executor.js:346-348`; the client has no global). Four checks in this order, each a refusal with its reason when it fails: the vocabulary sentence equals the table's; the recipe of the resumed record and scope equals the continuation's, by value, else `:recipe-differs` naming the first root that differs; the loop's `:items`, evaluated afresh over the resumed scope and projected onto `:fields`, agree with `:consumed` at every index below `:at`, by value, else `:consumed-items-differ` with the first differing index (bench 9's "dab 0 differs"); the restored state's arrays decode to their declared lengths. The pre-loop steps run again on a resume (they are pure and per edit; their outputs are not stored). A continuation with `:phase :in-loop` never evaluates `:state`'s initial expressions; one with `:phase :before-loop` (a pre-loop read pended) runs the pre-loop steps from the first, and when the read resolves enters the loop and evaluates them exactly then. The item at `:at` runs from its first step: no output of the suspended attempt is kept, which is why the withheld run leaves nothing in the granted one (§7). Items after `:at` come from the resumed scope, so a record that gained an authored event after suspending runs it ("accepts more authored events").

**The request and belonging.** A read capability declares `:snapshot`, a function of the same arguments that returns, without doing the read's work, the values the read depends on (§4.3 says what a snapshot pins). At a read step the executor forms

```clojure
request = {:recipe recipe :consumed consumed :item item :state state-before :at n :step :out :snapshot (snapshot args ctx)}
```

before calling `:run`: seven parts, the current item among them, because an answer demanded for one event belongs to that event and not to another with the same prefix (REFUTE-1 F8's witness: an event field the program reads, changed for the current item only, leaves the prefix, the state and the snapshot equal). An answer is `{:request r :value v}` and is routed by position: at a read step whose `:at` and `:step` differ from the answer's, the read runs normally; at the step whose position matches, the run compares the formed request with the answer's, by value in all seven parts, and when they are equal takes `:value` as the read's value without calling `:run`; when they differ, the run returns `{:status :stale :reason …}` naming the first part that differs, in this order: the recipe changed since the read was demanded; the consumed items differ; the current item differs; the input state is not the one the read was demanded in; the read's snapshot changed (order, bindings, painting or point); and nothing commits. One answer per run, consumed once; an answer whose position the run never reaches is `:stale` too ("the answer was not consumed: no request pending at this state"). Equality is by value; no hash is compared anywhere. A hash of a painting's bytes or of a request appears only in printed receipts and in the definer's tables, so a number can be copied and compared, never as the thing checked. Supports: Sid's rule (a key is the full input as a value, never a hash); and a counterexample from the seam bench, which compared hashes of bytes, recipe, state, snapshot and event values (`executor.js:267-268, 274-277, 332-335`) and whose painting reference omitted the domain, so an answer demanded under one chart rectangle was accepted under a rectangle shifted by sixteen units and painted a different colour (REFUTE-1 F12); a snapshot that is the whole surface value (§4.3) rejects it. Becomes a default only if a request's value proves too large to carry in a continuation on a real document, which no bench has shown; what changes then is a content address computed from the value and checked against the value, not a hash as the identity.

**The subject.** Every value a run returns has a subject: the computation that produced it, as data. `subject = {:recipe recipe :consumed [item …] :reads [read …] :out :name}`, read from `:return` and not from the loop: an output's recipe is the program plus the values of the roots that output's expression in `:return` reaches, where a `[:get out …]` on a pre-loop output is read as that step's `:args` (transitively, to roots) and, when that step is a read, as the read itself, which enters `:reads`; a `[:get :state …]` is read as the whole loop (the roots the transition reaches, as above, with the run's consumed items under `:consumed` and the loop's resolved reads under `:reads`); and a `[:get root …]` is that root. Every read in `:reads` is without its snapshot and with its provisional mark when it had one. An output that does not reach `:state` has empty `:consumed`; its `:reads` are empty only when it reaches no read: a record without a loop that reads the coating at G@2 under `:pending :provisional` and returns that read gives (0.5, 0, 0, 0.5) marked provisional at grant 0 and (0.25, 0, 0.5, 0.75) at grant 1 from one program and one set of roots (REFUTE-1's third round), and the mark in `:reads` is what tells the two apart. A record without a loop has subjects like any other: the seam bench's reach record returns its pre-loop surface region, whose step reads `support` and `tool`, so two reach records that differ only in the radius (150 and 140; the definer's q1 at distance 144.54684956268315 is inside one and outside the other, REFUTE-1's second round) have two subjects, where the resume reading, which starts from the loop and reaches nothing in a record without one, would have given them one and let a retained radius-150 region pass as the radius-140 record's output. `run` returns the subjects under `:subjects`, one per name in `:results`; whoever retains a result for another record's use (the harness in both slices; the store later) attaches the subject to the value under `:subject`, which surfaces and regions carry as an ordinary field. A record's input declared `:from {:record "reach@0" :output :region}` (§6.5) is checked at admission: the caller resolves the name to the producer record under `opts :records`; the input value's `:subject` must have that record's program, that record's roots by value wherever the subject's recipe holds them (the subject's caller roots, consumed items and reads are not in the producer record and are not checked against it; they are what tells two results of one record apart), and that output name; else `:refused :subject`. This is Position 9's "the result keeping the subject it was built from" without a hash: the subject is the run's request-shaped inputs plus which output, so the same record under another host, another grant schedule or another retained painting has a different subject, and a region authored by hand with the right numbers has none. Supports: the seam bench's small resolved argument record is not the producer record and a comparison of the two refuses the right region (REFUTE-1 F11); its paintings carried no producer at all; one unchanged producer record gave `30cb13f2…` from an empty painting and `b39a8b14…` from the previous pickup's, so the record alone does not name a result.

**Provisional.** A read step with `:pending :provisional` does not suspend on pending: the read's `:partial` (§4.3: the composition of everything the stack knows, the pending contribution transparent) becomes its `:color`, the read is marked `{:provisional {:missing […] :interpretation "missing contributions read as transparent (a declared provisional read)"}}`, the step consumes it, and the history row keeps the mark. Which reads were withheld is the grant owner's schedule, not the record's; the history says at each row whether that row's read was provisional, and a test of a provisional run names its schedule (§6.4). This is Position 12's narrowed exit, kept as the declared exception and nothing more: retaining the provisional read makes the selected history reproducible, not the selection independent of scheduling.

### 3.6 What the executor never does

It keeps no cache and no state between runs; it reads no clock (the per-step `:ms` in the log is a measurement, not an input); it never decides or decrements a grant; it never calls a host, a device or a store; it never interprets a string as code; it never runs a step whose input is an unresolved read; it never commits an answer it cannot show belongs; it never watches what a run read to decide what a run depends on.

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
 :changed n?                                      ; texels the last paint touched (a receipt)
 :subject subject?}                               ; when retained for another record (§3.5)
```

A surface is a value: `paint` returns a new one and the input keeps its contents. Identity within a run is the step that made it and its parent; depth alone is not an identity; the key is formed by the kind's `paint` wrapper from `ctx` (`"<surface/id>:<at>/<step>"`) and handed to the engine in `opts :key` (§4.2), because the engine's `paint` is a function of its four arguments and cannot know where in a record it was called (REFUTE-1 F22: two paints with equal arguments at two steps have two keys). Across records, identity is the subject. Two runs painting the same step from the same inputs produce equal values, which is the point. The declaration a record carries is the same map without `:data`, `:key`, `:parent`, `:changed`, `:subject`, plus `:initial [r g b a]`; `(surface/new declaration)` fills a fresh surface with `:initial` under the key `"<id>@initial"`. The 2D bench's `{:clear […]}` is written `:initial […]` in the client; the 3D bench's `initial` already is.

Texel centres: on a plane, texel (x, y) has its centre at the local point that the inverse of `:map` sends (x + ½, y + ½) to; on a chart, at (u0 + (x + ½)·w/width, v0 + (y + ½)·h/height) in chart units, which the kind's domain function turns into a point on the support. The engine knows planes; a kind supplies its other domains as functions in `ctx` (§4.4). A read outside a surface's domain is transparent, `[0 0 0 0]`, with `:covered? false` on the read: a surface has no value there, and through a stack (§4.3) the read falls to the layer beneath, and at the root to nothing. Supports: "never a wrong pixel" (bench 9 refused superseded values for the same reason); the seam bench returned `unsupported` for a read outside its patch and bench 9 clamped to the edge texel, two datapoints, neither reached by any definer number (every dab of both brushes lies inside its patch), and neither right for a stack of layers.

Bytes: `:data` is stored as `js/Float32Array` in the browser and `float-array` on the JVM; arithmetic runs in doubles and stores to float32 on both, as the two benches did, so the definer's byte hashes are reachable (§6.4 says what to do when they are not). The encoding for a continuation is §3.5's `{:float32le …}`.

### 4.2 paint

```
paint(surface, region, paint, opts) → surface'
  paint   {:kind :color :rgba [r g b a] :opacity o :blend :source-over}      ; this design's landed kind
        | {:kind :surface :surface s :filter :nearest :opacity o :blend …}  ; a layer painted into its parent; designed, not landed (§4.5)
  opts    {:coverage (fn [x y] → c ∈ [0,1])   ; the region's coverage at texel (x, y), supplied by the kind
           :clips    [(fn [x y] → c) …]       ; each clip's coverage; multiplies
           :bounds   [x0 y0 x1 y1] | nil      ; texels to visit; nil visits all
           :key      "paint:3/painted"}       ; the result's key, formed by the kind's wrapper from ctx; required
```

At every visited texel: `c = coverage(x, y) × Π clip_i(x, y)`; if `c = 0` the texel is untouched; else `src = rgba × opacity × c` on all four channels and `dst' = src + dst × (1 − src.a)`, in linear premultiplied RGBA32F. That one formula is both benches' arithmetic: bench 9's `compositeOver(dst, rgb, a·cov)` with `a = opacity × rgba[3]` and the premultiplied rgb divided out, and the seam bench's `over(rgba × opacity, dst)` at member texels, where a member's coverage is 1 at the centre and 0 otherwise. The result's `:key` is `opts :key`, its `:parent` the input's key, its `:revision` the input's plus one, its `:changed` the count of texels touched; a paint without a key is refused. The blend table holds `:source-over` and nothing else in the two slices below; a record naming another blend is refused `:unsupported-blend` with the table printed. Supports: no definer number on either bench uses another mode; bench 9 carried the W3C separable modes and no record read them; the vocabulary grows by attack, one operation at a time. What changes when a record needs one: a function in the table and its test, nothing in the executor.

The region's coverage is the kind's argument, because the engine may not depend on a kind. The path kind binds `pack/coverage-at` over the region lowered at a quarter-texel tolerance (bench 9: `0.25 / |a|` in local units) at each texel centre mapped through the inverse of `:map`, with `:bounds` the region's texel box; the 3D kind binds the surface region's membership at the texel centre's point on the support (`member(region, point) → 0 | 1`), with `:bounds` nil (the seam bench visits every texel). A clip is a region with no paint whose coverage multiplies, so for the 3D kind a clip is a second membership and the product is the AND the bench computed, and for the path kind it is the same product bench 9 drew in one pass.

### 4.3 sample

```
sample(stack, point, filter, ctx) → read
  stack   [layer …] bottom to top; a layer is a surface value, or {:layer/kind k :sample f :snapshot g}
  read    {:status :resolved :color [r g b a] :contributors [name …] :snapshot value :covered? bool}
        | {:status :pending      :snapshot value :missing […] :known [name …] :partial [r g b a]}
        | {:status :needs-policy :snapshot value :candidates [name …]}
        | {:status :unsupported  :snapshot value :reason …}
```

Layers compose bottom to top by source-over of premultiplied values: `acc' = layer + acc × (1 − layer.a)`. A surface layer's colour is its texel under `filter` at the point mapped through its domain (nearest: floor of the mapped coordinates; a point outside is transparent and not a contributor). Any other layer's `:sample` returns a read of its own in the same shape: resolved with a colour and contributors; pending with what is missing, what it knows (`:known`, the names of the contributions it could resolve) and its `:partial`, the composition of those known contributions with the missing ones transparent; needs-policy; unsupported. The stack composes through a pending layer: the layer's `:partial` takes its place in the accumulation, the layers above are sampled and composed as usual, and the stack's read is pending with the layer's `:missing`, with `:known` the layer's known names plus the contributors of every resolved layer below and above, and with `:partial` the whole composition so formed. That is what the seam bench's `read-surface` did inside its coating branch by hand (`executor.js:151-169`: the known marks composed in saved order, the painting composed over them, a provisional mark attached); here the exchange is in the contract, so the compositor composes and the coating only says what it knows, and a coating at the bottom of the stack with nothing below it has a known part all the same (REFUTE-1 F7: "the layers below it" said the wrong thing for that case, and this replaces it). A needs-policy or unsupported layer ends the read with that status; an unsupported read is the run's `:error` at that step with the reason kept (§3.2). `:contributors` lists the layers consulted whose domain held the point (a painting layer whether transparent or not; a coating's marks whose region holds the point), not the layers that changed the colour: the cold mind read it the other way, and the contract now says which.

**The snapshot** is what the read depends on, as values: for a surface layer, the surface value itself (declaration, domain, data, subject; a hash of it is printed in receipts, never compared); for another layer, whatever its `:snapshot` function returns (for the coating: the support at its revision, the retained records, the order, each binding's chain); and the filter and the point. `sample`'s own snapshot is the vector of its layers' snapshots plus filter and point. A read capability's `:snapshot` (§3.5) is exactly this, computed without the work.

The 2D `sample` op is this function over a one-layer stack; the 3D `read-surface` op is this function over a stack the kind assembles from the word `"coating"` and the paintings a record names (§6.3). One function, one contract, two bindings.

### 4.4 mix, new, domains, the table's shape

- `mix(a, b, amount) → a·(1 − amount) + b·amount` per channel including alpha: a linear blend of premultiplied values, not a source-over (the cold mind's fifth).
- `new(declaration) → surface`, filled with `:initial`.
- Domains: `ctx :domains {:plane <built in> :chart {:to-texel (fn [surface point] → [x y]) :to-point (fn [surface x y] → point)}}`; a kind that declares a domain kind supplies both functions when it binds the engine's operations into its table. Nothing in a record is a function; the functions sit in the kind's table, which is code below the waist.
- What the engine exports for a kind's table: `surface/paint`, `surface/sample`, `surface/mix`, `surface/new`, `surface/encode`, `surface/decode`, `surface/png-bytes` (the CPU picture, for the harness), each a pure function; the kind wraps `paint` (its coverage, its domain, the key from `ctx`) and `sample` (its domain, its layers) and registers them under the record-facing names `:paint`, `:sample`, `:mix`, `:surface/new`.

### 4.5 Runners, the screen, and what is designed but not landed

- **The CPU runner is the reference** (Position 9: the CPU walker as the reference runner; bench 9's twin checked every pixel; the seam bench's every number is a CPU number). Both slices land it and reproduce the definer's numbers through it.
- **The GPU road** (bench 9's fragment road: a surface as two RGBA32F targets, `paint` as one filler draw into the live target, `sample` as a one-texel read-back, one GPU sync per dab; or the compute road, tile-local) is the second runner of the same values and lands after a trace on a real brush shows the CPU runner short of the experience. What it must match is known from bench 9: the CPU twin across the whole surface to at most 1.2e-7, which needed the per-fragment exact local position (`localP()` through the inverse of the draw's map) and which the tree's filler does not yet do ("interpolates the same varying; the same fix applies there when the time comes"). That is the first thing a GPU landing measures.
- **The screen.** Today the tree presents the scene target with a full-screen draw (`compositor/draw-present!`) and composites region3d's leased targets as quads (`region3d.renderer/composite-region!`); those are two instances of `paint(parent, rect-region, {:kind :surface …})` written before the piece had a name. This design names the surface paint kind (§4.2) so that a layer is a surface painted as a region into its parent and the screen is the root, and lands neither the kind nor the rewrite of present and composite through it: no definer number depends on a presented pixel that the client can reach (bench 9's presented pixel through a network face needs `arrange`, `locate` and `face-boundaries`, which the client's path kind does not have), and the bench is the screen: the harness writes the painting as a PNG and its numbers as files. Becomes a default only at Sid's word that the pickup must be visible in the verifier's picture; what changes then is one paint kind in the filler's instance row (a texture in place of the straight colour) and one upload of the surface, and the present and composite passes become its callers.
- **Deleted now**, as wrong form: `compositor.cljs`'s `strip-padded-rows`, `unpremultiply!` and `png-bytes!` (lines 801 to 849 at HEAD), uncalled, duplicates of a read-back the harness does itself (`harness/path.cljs` `render-path-bytes!`, `harness/region.cljs` `w4-read-texture!`); the CPU picture is `surface/png-bytes` in §4.4.

---

## 5. The path kind's pickup brush through them

### 5.1 The data flow

1. **L0, the edit boundary** (landed, `path/construction.cljc:38-52`): `construct` runs the record's `:path/construction` (or `default-construction`) through the executor with `source`, `tool` (without `:name`), `paint` and `identity` as roots over the kind's table (`construction.cljc:16-22`: `:path/source`, `:path/envelope`, `:path/dabs`, `:path/regions`), accepts a bare path or `{:path … :paint …}`, and returns the validated component `{:path/material-id :path/revision :path/value :path/paint :path/parameters (:path/snap?)}`, where `:path/paint` is the record's paint with the recipe's overrides merged and `:path/parameters` the tool's numbers the stroke's width rule references (`construction.cljc:46-48`). Under this design `construct` calls `run` on the recipe in the new grammar and takes `:results`; a status other than `:complete` at L0 is the boundary's error, with the executor's reason. The brush's caller takes `:path/value` from the component.
2. **L1, dabs**: the path value + the tool + the stroke → the ordered dab packets, each `{:x :y :r :at :s :p :seg :u :path}` as `stroke/dabs` emits them (`stroke.cljc:426-428`; `:path` is the disc about the centre, the dab's region; `:at` and `:s` are the arc length within the subpath and its fraction of that subpath's polyline length). The brush's `:path/dabs` capability is `stroke/dabs` over the options `component/stroke-options` forms from the tool and the stroke (`component.cljc:92-112`: the width rule compiled, the spacing, the tolerance), returning the `:dabs` vector of its result; the table's landed entry is the bare positional function and is wrapped, not changed. Called from the brush program's pre-loop steps.
3. **L2, the program**: the executor over the record's program with roots `tool`, `paint`, `surface` from the record (unqualified by the boundary, §3.2) and `path` from the caller (the component's `:path/value`); the loop over the dabs, each projected onto `[:x :y :path]`; per dab: `sample` the surface at the dab's centre, `mix` into the carry, `paint` the dab's disc with the carry at the stroke's opacity.
4. **L3, the picture**: the returned surface, written by the harness as a PNG beside its numbers; on screen later (§4.5).

The caller of L0 and L2 is the harness in this slice and the store's edit reaction later; both hand values in and take values out.

### 5.2 The record, in the client's grammar

The definer's record (bench 9, attack 2; `bench-9/node-route/regress.js:2-6`), translated field for field into the landed record shape: the width rule sits at the stroke's `:width` as an expression over the tool's numbers and the dab's `p` (`construction.cljc:46-48`, `width.cljc:17-27`), where bench 9 wrote it in the tool and named it from the stroke.

```clojure
{:path/material-id "surface-read" :path/revision 1
 :path/tool   {:size 16 :streamline 0 :fit :polyline :pickup 0.5 :carry [1 0 0 1]}
 :path/source {:kind :pen :samples [[26 28 0.65 0] [102 100 0.9 90] [28 100 1 180] [102 28 0.7 270]]}
 :path/paint  {:fill nil
               :stroke {:overlap :accumulate :spacing 12 :tip :nib :width [:* [:get :size] [:get :p]] :unit :local
                        :cap :round :join :round :align :center :color [1 0 0 0.62]}}
 :path/surface {:surface/id "paint" :revision 0 :width 128 :height 128
                :domain {:kind :plane :map [1 0 0 1 0 0]} :color :linear-premultiplied-rgba
                :filter :nearest :initial [0 0 1 1]}
 :path/program
 {:steps [{:out :fresh :op :surface/new :args {:declaration [:get :surface]}}
          {:out :dabs  :op :path/dabs   :args {:path [:get :path] :tool [:get :tool] :stroke [:get :paint :stroke]}}]
  :each  {:items [:get :dabs] :item :dab :fields [:x :y :path]
          :state {:carry [:get :tool :carry] :surface [:get :fresh]}
          :steps [{:out :sample  :op :sample :args {:surface [:get :state :surface]
                                                     :point [[:get :dab :x] [:get :dab :y]] :filter :nearest}}
                  {:out :carry   :op :mix    :args {:a [:get :state :carry] :b [:get :sample :color] :amount [:get :tool :pickup]}}
                  {:out :painted :op :paint  :args {:surface [:get :state :surface] :region [:get :dab :path]
                                                     :rgba [:get :carry] :opacity [:get :paint :stroke :color 3] :blend :source-over}}]
          :next  {:carry [:get :carry] :surface [:get :painted]}}
  :return {:surface [:get :state :surface] :carry [:get :state :carry] :dabs [:get :dabs]}}}
```

The paint step's output is `:painted`, not `:surface`, because `surface` is a root and an output that shadows a root is refused (§3.1); the surface's key therefore reads `paint:<item>/painted`. The recipe (§3.5): the program with `tool`, `paint` and `surface`; not `path`, not `source`.

The path kind's capability table for it lives where the landed table lives, `construction.cljc`: the four landed operations in named form, `:path/dabs` in the brush's form above, `:surface/new`, `:sample`, `:mix`, `:paint` (the engine's, bound with `pack/coverage-at` at a quarter-texel tolerance, the plane domain and the key from `ctx`), `:vocabulary "path kind, production 2: sample nearest · mix linear · paint = coverage × opacity·alpha, source-over, linear premultiplied RGBA32F · dabs by arc length · capsule nib"`.

### 5.3 The numbers and where they are pinned

| receipt | value | where the definer states it | the client's test |
|---|---|---|---|
| the texel at (64, 64) after 24 dabs (the cursor at (64.5, 64.5)) | (0.01336952205747366, 0, 0.9866304397583008, 1), red after blue; both bench hosts agree to 1.2e-7 | `bench-9/HANDOVER.md:150` (the precise row; `regress.js:16` prints it rounded to four decimals, `[0.0134,0,0.9866,1]`) | `test/app/client/path/pickup_test.clj`: each component within 1e-6 |
| the dab count and the chain | 24 dabs, 24 paints (one per dab; `regress.js:15` counts 24 paint calls on its CPU host; `HANDOVER.md:205-206`'s "one copy plus 23 in place" is the GPU host's allocation, not the paint count), the final key `paint:23/painted`, revision 24 | `HANDOVER.md:205-206`, `regress.js:15` | the same test: `(count dabs)`, the returned surface's `:key`, `:revision` |
| the checkpoint at dab 12 | resumed to the same 65,536 components as the straight run; through bytes and back the same | `HANDOVER.md:265-266`, `wire.js:11-23` | `run … {:until 12}` → suspended at 12; `resume` → equal `:data`; `encode` → `decode` → `resume` → equal |
| the four edits against the checkpoint | first pressure .65 → .4 refused (dab 0 differs); last x 102 → 110 loads; `tool.pickup` .5 → .25 refused; surface 64 × 64 refused | `HANDOVER.md:251`, `wire.js:25-29` | resume with the edited record: the path is not in the recipe (§3.5), dab 0's projection differs (its radius, so its `:path`), `:consumed-items-differ` at 0; the path is not in the recipe, dabs 0 to 11 agree on `:x`, `:y` and `:path` (their `:s` differs and is not a declared field, §3.1; REFUTE-1 F5's probe compared every field of the twelve prefix packets, the complete `:path` values included, and found dab 0 equal and dabs 1 to 11 different only in `:s`), the resume completes over the new dabs 12 to 23; `tool` is reached, `:recipe-differs`; `surface` is reached through `fresh`, `:recipe-differs` |
| the CPU twin's 24 dab centres and radii | as bench 9's dab packets (the first radius 5.2, the length 281.93729461954166 through the client's own `stroke/dabs`, REFUTE-1 F2) | `regress.js` exports `run`, whose result carries `dabs`; `extract.js` runs the bench's pure declarations in Node; no script prints the table, the landing reads it from `run(...).dabs` | the first comparison the landing runs when the texel differs (§5.4) |

Tolerance: 1e-6 per component against the definer's decimal, because the client computes in doubles and stores in float32 as the benches did but need not order its operations identically.

### 5.4 Hidden work, named

- **"paint" hides the coverage.** The texel's third decimal depends on the coverage arithmetic at texel centres matching bench 9's `coverageAt` (the region lowered at a quarter texel, bandless, the winding rule's coverage saturated). The client's `pack/coverage-at` is the same twin ported, and the crossing check already matched the bench's `.8556` through it, which is evidence, not proof, for a 24-dab accumulation. Receipt order when the texel differs: the dab packets first (positions and radii against bench 9's), then one dab's coverage at (64, 64) against the bench's function in `bench-9/node-route` (its `bench.js` is extracted from the html by `extract.js`), then the over.
- **"the brush's dabs" hides an adapter.** `stroke/dabs` takes `[path opts]` and returns `{:dabs :polylines :length}`; the options come from `component/stroke-options` over the tool and the stroke; the capability composes the two and returns `:dabs`. The one choice inside it is which options: the ones `component/geometry` forms today (`component.cljc:142-159`), no others.
- **"sample nearest" hides a convention:** floor of the mapped point, so (64.5, 64.5) under the identity map is texel (64, 64); the 3D painting's centre convention is (x + ½). Both stated in §4.1; a landing that rounds instead of floors reads a neighbour.
- **"the continuation as bytes" hides the encoding:** little-endian float32 under base64, the same bytes from a `js/Float32Array` and from a JVM `ByteBuffer` in little-endian order; the round trip is the receipt and the byte count is not.
- **"the dab's centre" hides three field names:** `:x :y :path` at `1501b85`; if a later slice renames the packet, the record's `:fields` and its `[:get :dab …]` change and nothing else.

---

## 6. The 3D kind's coating-reading brush through them

### 6.1 What the sphere brush needs, and what it is made of

Every number of the brush is a CPU value of four things: the support (the sphere G with its charts at three revisions), the coating (two retained records bound through chains at each revision, composed by a saved order, the merge step charged one unit of work at G@2), the painting (a chart patch), and the record. None of it exists in the client. All of it is pure and runs under the JVM suite. It lands as five namespaces beside the 3D tree, `.cljc`, ported by name from the seam bench's sidecar `bench-2/node-route/sweep-and-sphere.js` (its functions are named below) and `executor.js`, with the sidecar's probes as the tests:

| namespace | holds | ported from |
|---|---|---|
| `region3d/support.cljc` | the support value `{:kind :sphere :R 200 :charts […] :revisions n}`; unit vectors from (lon, lat); the great-circle distance (the intrinsic metric, the only `distance` this slice answers); chart S0 (u = R·lon, v = R·lat) and its authored branch (u around πR); the charts S1 and S2 as maps to and from S0 with their periods; the chart→point and point→chart functions (§4.4's `:chart` domain); the great-circle arc `kA/arc-AB` and its point at t; the chart piece of a point (west, east, north) | `sphUnit sphAngle sphDist sphLon sphLat sphChart0 sphPoint0 sphPreimages sphArc sphArcDistClosed sphPieceOf gArcCentre gUnwrapU G_CHARTS gChartMetric` |
| `region3d/coating.cljc` | a retained record (its root domain, its mark, its colour); the bindings at a revision, each a chain of relations from the record's root to the current chart with its work; `locate(support, revision, point, {:budget :order})` → the composition as a read (§4.3): resolved with colour and contributors, pending with the missing dependency named, the known marks and their composition as `:partial`, needs-policy with candidates, empty; the root restriction tested on every branch's preimage | `G_RECORDS gBeta gBetaInv gBindings gMarkA gMarkB gLocate` and `over` |
| `region3d/surface_region.cljc` | the region as data: `{:region/kind :surface :support {:id :revision} :centre [lon lat] :radius r :distance :surface :area :bounds :pieces :saturated? :subject?}`, built from a seed or a point and a radius on a support; membership `member(region, point)` and `distance(region, point)` as functions of the namespace over that data, the same computation as the printed distance; area, bounds (world mm about the centre), chart pieces, `saturated?` at radius ≥ πR (the whole sphere, area 4πR²); a negative radius refused. The bench's region carried closures and an owned copy of its argument record; the client's carries neither: it is a value, immutable, and carries its subject when retained (§3.5) | `reachRegion sphFrozenDist` and the executor's `surface-region`, `member`, `xRegionPieces` |
| `region3d/capabilities.cljc` | the 3D table: `:curve-point`, `:surface-region`, `:member`, `:read-surface` (assembles the stack: the word `"coating"` → a layer `{:layer/kind :coating :sample (locate …) :snapshot (…)}`, a painting → itself; calls `surface/sample` with the chart domain and the budget), `:mix`, `:paint` (the engine's, bound with `member` at the texel centre's point, the chart domain and the key from `ctx`), `:sample`, `:surface/new`; each with `:args` and `:needs` as §3.1 (`:surface-region` needs seed or point); `:vocabulary` the sentence `executor.js` carries as `X_CAPABILITIES`, re-worded for the client | `executor.js` `X_OPS` |
| `region3d/records.cljc` | fixtures: G with A@0 and B@0, S1 and S2, the four definer records of `records.js` (`reach`, `sequence`, `pickup`, `clip`) translated as §6.3, and the cold author's two (`cold/reach.json`, `cold/reach-dab.json`: the 128 × 64 patch at `[308.3185307179587 50 640 250]`, the radius-200 dab through the reach), which the 6096 receipt is about (§6.4) | `records.js`, `G_RECORDS`, `G_CHARTS`, `cold/*.json` |

The general smooth or trimmed host (a distance operation with an error argument, or a declared mesh authority) stays owed as HANDOFF-5 §6.2 says. The support protocol a second host would implement is not complete in this port: `distance`, `chart->point`, `point->chart` and `pieces` are the four operations the coating and the surface region are known to need, and the ported bodies also reach the sphere directly, for the region's area, bounds and saturation from R and the cap's angles (`sweep-and-sphere.js:170-182`), for the chart pieces from the cap's angular radius (`executor.js:191-203`), and for the bindings' chart maps and periods (`sweep-and-sphere.js:244-294`). Those three places are where a second support kind touches `surface_region` and `coating`, named so that a later landing does not read "four functions" as the whole interface (REFUTE-1 F18); the protocol is completed when the second host is designed, and this slice ports the sphere by name.

### 6.2 What of the 3D tree must move, and no more

**Nothing that stands in `src/app/client/region3d/` moves for the sphere brush to run there.** The five namespaces above are added beside it; `component.cljc`, `scene.cljc`, `frame.cljc`, `on_plane.cljc`, `on_plane_renderer.cljs` and `renderer.cljs` are not touched by the 3D landing, and `harness/region.cljs` gains one leg (§6.4). Supports: the brush's every receipt is a value of the support, the coating, the painting and the record; none reads the region row, the pick, the mesh kind dispatch, a placement or a pass. HANDOFF-4 §6.7's list, at Sid's word only, item by item, and why it is not this slice: the region row split into space, view, portal and lease is what puts a painted sphere on screen through a view, and this slice puts nothing on screen; segment counts and required normals out of the value is the same slice; placed ink in the pointer map is the pick, which the brush never calls; the aspect declared by the portal is the view; one grammar for the question protocol is the hit, which the brush never asks; the executor's spatial door is the pick again; the surface read bound into the executor is this slice, and it binds through the capability table, not through the region row. The mesh kind dispatch stays closed (`component.cljc:386-395`); the `:sphere` there is a mesh primitive, and the support here is a value of a new kind; when the sphere goes on screen, a thing's definition is the support and the primitive is its tessellation at a tolerance, which is Position 10's "a thing is a placement of a definition" landing in code, and that is the next slice's first move. Becomes a default only at Sid's word that the painting must show on the sphere in the verifier's picture in this slice; what changes then is the region3d fragment shader reading a coating texture through the chart and the material path opening, with no definer number to judge it by.

### 6.3 The record, in the client's grammar

The definer's attack 4 record as `records.js` carries it (`coating-pickup@0`), translated field for field; the reach, sequence and clip records translate the same way (the reach record has no loop, §3.1).

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
  :each  {:items [:get :events] :item :event :fields [:curve :t]
          :state {:carry [:get :tool :carry] :painting [:get :fresh]}
          :steps [{:out :at        :op :curve-point    :args {:curve [:get :event :curve] :t [:get :event :t]}}
                  {:out :picked    :op :read-surface   :args {:support [:get :support] :point [:get :at]
                                                              :layers ["coating" [:get :state :painting]]
                                                              :order [:get :coating :order] :filter [:get :painting :filter]}}
                  {:out :carry     :op :mix            :args {:a [:get :state :carry] :b [:get :picked :color] :amount [:get :tool :pickup]}}
                  {:out :footprint :op :surface-region :args {:support [:get :support] :point [:get :at]
                                                              :radius [:get :tool :radius] :distance :surface}}
                  {:out :painted   :op :paint          :args {:surface [:get :state :painting] :region [:get :footprint]
                                                              :rgba [:get :carry] :opacity [:get :tool :opacity] :blend :source-over}}]
          :next  {:carry [:get :carry] :painting [:get :painted]}}
  :return {:painting [:get :state :painting] :carry [:get :state :carry]}}}
```

The caller's roots: `host` (the support value with its retained records, so a record may write `[:get :host :A :radius]` instead of retyping 16; the cold mind's twelfth) and `inputs` (a region or painting another record returned, checked as §6.5 says). The recipe (§3.5) is the program with `support`, `coating`, `painting` and `tool`, plus `host` or `inputs` the moment the program reaches them; the events are the items, projected onto `[:curve :t]`, so an event's `:id` is not consumed and a record that reads another event field (`amount`, in REFUTE-1 F8's witness) declares it.

### 6.4 The numbers and where they are pinned

All from `probe-4.mjs` (its constant `D`, lines 62 to 72, and its sections A to F) and `probe-2-3.mjs`; each becomes a test under `test/app/client/region3d/`.

| receipt | value | test |
|---|---|---|
| the four reads' colours | (0.25, 0, 0.5, 0.75) · (0.5390625, 0, 0.328125, 0.8671875) · (0.5, 0, 0, 0.5) · (0.6631851196289062, 0, 0.2650909423828125, 0.9282760620117188) | `brush_test`: per dab, exact to 1e-9 (the arithmetic is dyadic) |
| the four carries | (0.8125, 0, 0.125, 0.9375) · (0.744140625, 0, 0.17578125, 0.919921875) · (0.68310546875, 0, 0.1318359375, 0.81494140625) · (0.6781253814697266, 0, 0.16514968872070312, 0.8432750701904297) | the same |
| contributors per dab | `kA, kB, pickup-G@0` · `kA, kB, pickup-G@1` · `kA, pickup-G@2` (B outside its root at t = .41) · `kA, kB, pickup-G@3` | the same, exact |
| texels changed per dab | 401 · 321 · 405 · 401 | the same, exact |
| the painting's bytes | sha256 `30cb13f2147784bdae44b4aa303d412c8980f00c09435c0983521cd291d75579` | the sha256 of the float32 bytes equals the definer's; if it does not, the components within 1e-6 and the hash difference reported as a finding with its cause found from a taken path, never assigned to operation order by the tolerance alone (an authored pickup of .25000001 stays within 5.96e-8 of every component and changes the hash to `4ce490fc…`, REFUTE-1 F17), never silently loosened |
| the three-run equivalence | eager (grant 1 from the start), delayed (grant 0 → suspended → resume with 1), cold (the continuation encoded to bytes, decoded in a fresh process or a fresh JVM, resumed with 1): identical state, history and bytes; and an ordinary checkpoint after two dabs resumes to the same history and bytes | `brush_test`: the three summaries and the full histories equal by value |
| withheld | budget 0: `:suspended :pending` at 0, carry still [1 0 0 1], the painting at revision 0, an empty history, `:missing` naming `B@0 → M@2` at the demanded chart point, `:known ["kA"]`, three events queued; a second withheld run gives the identical continuation | `brush_test` |
| the guess | `:pending :provisional` on the read, under the schedule the bench ran (`probe-4.mjs:99-105`: the first read withheld, budget 0 until item 1, then budget 1 for the remaining three): first carry (0.875, 0, 0, 0.875), last carry (0.7258682250976562, 0, 0.0880279541015625, 0.8138961791992188), final bytes `c1cf8170…`, the history rows marked `[true false false false]`. The other schedule, all four reads withheld: the same first carry, last carry (0.773040771484375, 0, 0, 0.773040771484375), bytes `dd7d3355…`, every row marked. The test names its schedule; the declaration alone does not select one | `brush_test`, both schedules |
| stale | an answer demanded under `[kA kB]` delivered into the reversed order: stale naming the recipe (the order is a reached root, and the recipe is compared first), no history row; the reversed run itself completes with first read (0.5, 0, 0.25, 0.75) and bytes `ae8c231c…`; no order: needs-policy, candidates `kA, kB`, no next state; an answer delivered again after the run completed: stale, "not consumed: no request pending at this state", the four rows untouched; pickup .25 → .5: the old answer stale naming the recipe, the new painting `4df8a532…`; the first painting as a declared input under its subject: `a5aba6ed…`, another painting under that subject refused `:subject` | `brush_test`, one assertion each |
| the sequence | one authored point read at G@0, G@1, G@2: (0.25, 0, 0.5, 0.75) at each; with budget 0 the third read pending at B's merge | `coating_test` |
| the reach | 628: the antipode outside by member and by distance, area 502654.50582273136; 629: inside, area 4πR², bounds [−200, 200]³, `saturated?`; 2πR: the whole sphere with ordered bounds; the caller's 150 → 140 after the run does not reach into the result (it reports 150; a value); a negative radius refused; zero is the seed alone; q1 inside at 144.54684956268315, area 67433.94227378976 | `surface_region_test` |
| the clip, twice | `records.js`'s clip record: the radius-4 dab at q1 through the reach as a clip on the 64 × 32 patch `[900 180 100 60]`, 34 texels changed; the cold author's `cold/reach-dab.json` through `cold/reach.json` on the 128 × 64 patch: 6096 texels changed, bytes `26da35e3…` (7502 unclipped is the cold author's control, not asserted) | `brush_test`, one row each |

The harness leg `run-coating-brush!` in `harness/region.cljs` runs the brush eagerly on the CPU in the browser, writes the numbers above and the painting as a PNG (64 × 32, scaled by an integer for the eye) into the dump beside the region3d results, and records the painting's sha256 as a text golden. No pixel of the region3d passes changes; the goldens under `test/app/fixtures/render_engine/gpu-goldens/` are untouched by this landing.

### 6.5 Hidden work, named

- **"read-surface" hides the binding inversion.** `gLocate` tests every branch's preimage (k = −1, 0, 1 around the chart's period) against the root restriction, charges B's work against the budget before any inversion, composes by the order with `over`, and names the missing dependency in the chain's last relation's words. Port it by name; `coating_test`'s sequence numbers and the third dab's contributors (`kA, pickup-G@2` without kB) are the receipts that the restriction and the branches are right. What the port adds: the pending read carries the known marks' composition as `:partial` (§4.3), which is the bench's `read-surface` exception path moved into the layer's own answer.
- **"surface-region" hides `pieces` and `saturated?`.** `xRegionPieces` names the chart pieces the region meets from the region, not from its seed (the cold mind's tenth), and saturation is a comparison against πR before any distance: the reach at 629 mm is the whole sphere. The receipts are in the reach row.
- **"inputs" hides the subject check.** A record's `:inputs {:clip {:kind :surface-region :from {:record "reach@0" :output :region}}}` names another record's returned value by record and output (bench 9's `{result, output}` form, kept); the caller hands the value in under `inputs` with its `:subject` attached and the producer record under `opts :records`; the executor checks the subject as §3.5 says and refuses `:subject` otherwise. On the bench the subject was a printed hash the author copied into the record; in the client it is the producing computation as data. The printed hash stays in receipts.
- **"the vocabulary sentence" hides a version.** The seam bench's `X_CAPABILITIES` sentence is the bench's; the client's table declares its own, and a continuation written by the bench does not load into the client (a different sentence, refused with the reason), which is right. The bench stored its sentence and never checked it (REFUTE-1 F9); the client's check is new behaviour with its own test.
- **"pending" hides where the barrier is checked.** Only a read step (one whose capability declares `:snapshot`) can suspend; a `mix` that receives a pending value is the executor's error, not a suspension, because the executor never runs a step on an unresolved read (§3.5). A landing that lets `mix` see the pending map has built the barrier in the wrong place.

---

## 7. The hardest case, moment by moment

The coating-reading brush on G at revision 2, the record of §6.3, through the executor of §3 and the compositor of §4.

1. **Withheld.** `run` with `{:budget 0}`. The pre-loop step makes `fresh`, the painting at revision 0. The loop is entered and the state initialised: carry [1 0 0 1], the painting `fresh`. Item 0, `dab-0` projected to `{:curve "kA/arc-AB" :t 0.65}`: `at` = the arc's point at t = .65, a unit vector. `picked`: the executor calls `read-surface`'s `:snapshot` first and forms the request: the recipe, no consumed items, the item, the state before (carry [1 0 0 1], the painting at revision 0 with its bytes), at 0, step `picked`, and the snapshot (G at revision 2, the two retained records, the order [kA kB], the chains [φ χ] for A and [β φ χ] for B at revision 2, the painting at revision 0, nearest, the point). No answer is supplied, so `:run` runs: the stack is [coating, painting@0]; the coating layer's sampler is `locate` at revision 2 under budget 0: binding A's work is 0, it resolves, its mark holds the point; binding B's chain ends in χ with work 1 > 0, so B is unresolved with the dependency named; the layer returns pending with `:known ["kA"]` and `:partial` kA's colour; the stack composes the transparent painting over it and returns pending with that snapshot. The value of `picked` is a read whose status is not resolved, so the executor stops before `mix`: no `:next`, no history row. It returns `:suspended :pending` at 0 with the request, the missing dependency and the continuation: schema, the vocabulary sentence, the record, the scope (`host`), phase in-loop, at 0, consumed [], the state before (the painting's bytes under `:float32le`), an empty history, the request, the missing dependency. A second run with budget 0 returns an equal continuation.
2. **Granted after.** `resume` with the table and `{:budget 1}`. The vocabulary sentence agrees; the recipe agrees (the same record and host); no consumed items to check; the state decodes to its declared length. The pre-loop step runs again and its output is not used by the restored state. Item 0 runs from its first step: `at` recomputed, the same vector; `picked`'s request formed again, equal to the stored one in all seven parts; `:run` under budget 1: B's work 1 ≤ 1, both marks resolve, the composition by the order gives (0.25, 0, 0.5, 0.75) with contributors [kA kB]; the painting layer is transparent there and is a contributor; the read is resolved. `carry` = mix([1 0 0 1], (0.25, 0, 0.5, 0.75), .25) = (0.8125, 0, 0.125, 0.9375). `footprint` = the region of surface distance 8.75 about the point, a value. `painted` = paint(painting@0, footprint, carry, .5) under the key `pickup-G:0/painted`: 401 texels change, revision 1. `:next` sets the state; the history gains its row. Items 1 to 3 the same, each read granted 1 (nothing accumulates). Final carry (0.6781253814697266, 0, 0.16514968872070312, 0.8432750701904297); the painting's bytes `30cb13f2…`.
3. **Granted before.** `run` with `{:budget 1}` from the start reaches the same values at every step, because a transition consumes the item, the state before, the program, the reached roots and the resolved read, and the grant is not consumed (§3.3); and because the withheld run left no partial output in the continuation (§3.5, the item re-runs from its first step). Eager, delayed and cold are equal by value, which is the definer's three-run equivalence and Position 12's receipt.
4. **Cold.** The continuation encoded to bytes and decoded in a fresh JVM (the test) or a fresh browser (the harness) resumes to the same bytes under the table the fresh process binds: the load checks the schema, the sentence, the lengths; the resume checks the recipe (the host by value included) and the consumed items; nothing else is needed, because the continuation holds every input of the transition and nothing in it is a function.
5. **An answer that does not belong.** The read demanded under `[kA kB]` (`run` with `{:stop-at :picked :budget 1}` returns `:answered` with the request and the colour, committing nothing) is delivered into the record with the order reversed: the order is a reached root, so the recipe differs, and the recipe is the first part compared; the run is `:stale` naming the recipe, with no history row; the reversed run completes on its own with its first read (0.5, 0, 0.25, 0.75). The pickup edited to .5: the recipe differs, stale. Delivered a second time after the run completed: no read step remains to reach the answer's position, so it is stale as not consumed, and the history keeps its four rows. Delivered into a record with a second read `preview` inserted before `picked`: `preview` runs on its own budget, the answer is consumed at `picked` where its position and request match. No order saved: the layer returns needs-policy with candidates; the run suspends with no next state.
6. **Provisional, by declaration.** `:pending :provisional` on `picked`, the first read withheld and the rest granted: the compositor composes kA with B transparent and the painting over it, the read carries the provisional mark, `mix` consumes it: first carry (0.875, 0, 0, 0.875), and every later carry differs from the granted run's; the history says at row 0 that the read was provisional and at rows 1 to 3 that it was not; the bytes are `c1cf8170…`. All four withheld: every row marked, `dd7d3355…`. The selection was the record's declared choice under the owner's schedule, retained in the history, and not a race.
7. **The same executor on the 2D pickup.** The record of §5.2, `run` with `{:until 12}`: twelve dabs run, the run suspends at 12 with a continuation holding the twelve consumed projections and the surface after twelve paints; `resume` runs dabs 12 to 23 to the same 65,536 components as the straight run; through bytes the same. The four edits: the first sample's pressure changed alters dab 0's radius and so its `:path`, the consumed items differ at 0 and the resume is refused; the last sample moved alters dabs from 12 on and the `:s` of every dab, which is not a declared field, so the consumed items agree and the resume runs the new dabs; the pickup changed alters the recipe, refused; the surface redeclared alters `fresh` and so the recipe, refused. No read of the pickup's surface can pend, because the surface is its own; the `pending` branch is never entered, and the same code runs both brushes.

---

## 8. The push edge

**What the slice leaves.** The path renderer takes the whole list of draw items every frame, computes one key per item, and returns early when the frame's key is unchanged; otherwise it walks every item, checking each level's key against its cache. The slice removed the run cache and made the path value the renderer's input, so the walk is over values; it is still a walk over every item on every edit (finding 1: about 22 µs per unchanged item, 35 ms at 1,600 for a colour edit; the floor of keys alone 1.2 ms at 1,600). The push edge removes the walk: the caller names what changed.

**The edge as data.** A draw item is a placement: a material placed in a group, as the harness builds it today (`harness/path.cljs:55-58`: `{:id id :path/material component :container group}`). Placements are the unit, not materials: the tree golden places one material twice (`:path-tree/root` in group 0 and `:path-tree/child` in group 17, both `:path-golden/group-tree`, `harness/path.cljs:158-161`), and a map keyed by material could hold one of them (REFUTE-1 F14).

```clojure
(path-renderer/push! system {:upsert {placement-id {:path/material component :container group} …}
                              :remove #{placement-id …}
                              :order  [placement-id …] | nil        ; the draw order, when it changed
                              :groups world-transforms | nil})      ; the group registry, when it changed
→ {:reran {:geometry #{placement-id …} :pack #{region-key …} :rows #{placement-id …}}}

(path-renderer/frame! system view)                                  ; per frame: the camera and the view facets
→ {:changed? bool :reran {:geometry #{…} :pack #{…} :rows #{…}}}
```

The system holds the current view: `init-path-system` takes the initial view as an argument, `frame!` replaces it, and `push!` runs L1 to L5 for the named placements against the held view. A material shared by two placements that changes is two placements in `:upsert`; the caller names both, as it names everything.

**What reruns, by input** (the landed geometry's declared facets, `frame.cljc:22-31`, `component.cljc:120-159`, pinned in `frame_test.clj:25-40`): geometry depends on the projected scale (the zoom times the group's scale) for a device-unit width and on the fraction of `tx × zoom + pan` for a snapped record, so a zoom reruns geometry for every device-unit-width placement whether or not a bucket crosses; a group's move reruns geometry for the group's snapped placements when the fraction changes, and for no other placement; a group's scale reruns geometry for its device-unit-width placements and the pack for its regions whose bucket crossed; a pan reruns geometry for the snapped placements when the fraction changes and nothing for an unsnapped scene; a region without cubics packs once per bucket and its cover is per bucket (`renderer.cljs:197-231`: one pack under `:all`, a cover per bucket, and the cover's rectangle enters the rows), so a bucket crossing reruns its cover and rows and not its pack. The renderer keeps the set of view-dependent placements as they are pushed, from their declarations, so `frame!` visits only that set. `prepare-path-frame!` and `frame/frame-key` (the whole-list walk and its key) are deleted; `item-key` stays as the per-placement value the rows are keyed on.

**What one push touches beyond the named placements, and why `:reran` says so.** Two existing edges propagate: the coverage atlas compacts when its garbage exceeds half the written curve texels (`coverage.cljs:364-395`), and a compaction moves surviving packs' slots, whose band address is baked into every instance row (`coverage.cljs:218-244`; REFUTE-1 F16 moved an unchanged pack from `{:curve-base 4 :band-base [6 0]}` to `{:curve-base 0 :band-base [0 0]}` and its row's fourth word from 6 to 0), so a removal that triggers a compaction reruns rows for every placement whose slot moved; and a removal or an order change moves later placements' row ranges (`renderer.cljs:279-295`, consumed by `harness/region.cljs:359-362`). Both are named in `:reran :rows`; "the named placements and no other" is the rule for geometry and pack, and rows follow the slots and the ranges. The row write is a direct range write per changed row, owned by the renderer; `buffer_pool/batch-update-pool!` walks and compares every row (`buffer_pool.cljs:77-99`: 1,600 comparisons for one changed row) and is not this edge's write; `buffer_pool.cljs` gains the range write and is in slice A's list (§10).

**Sid's rule as tests** (`frame_test`, extended): a colour edit reruns rows for that placement and nothing else; a knot edit reruns geometry, pack and rows for that placement only; a pan reruns geometry for exactly the snapped placements whose fraction changed and nothing for an unsnapped scene; a zoom reruns geometry for exactly the device-unit-width placements and pack for exactly the regions whose bucket crossed; a group move reruns geometry for exactly the group's snapped placements; a push of one placement among 1,600 writes one row range and compares no other row, with the row write counted (REFUTE-1's instrumented-row probe is the shape of the receipt: the count of comparisons and writes, not the set of ids alone). `:reran` is the receipt each test reads for the sets; the counts are read at the write. The measurement after landing is the scale trace and `timing.clj` re-run: an edit at 1,600 costs what changed plus nothing, against finding 1's numbers.

**Where the reactive question lands, and what this slice does about it.** Nothing in the client is Missionary today (zero occurrences under `src/app/client`; every asynchronous edge is a Promise or `onSubmittedWorkDone`). The push edge is a function in this slice; the harness calls it. When the store's courier connects, a flow of diffs reduces into the system (`m/reduce` over the diff flow with `(fn [system diff] (push! system diff) system)`: the accumulator is the system, which `push!` mutates, and never `push!`'s return, which is a receipt), and a flow of grants resumes continuations the same way with the continuation as the accumulator; the laws that shape that wiring are the skill's L6 (a wide `m/latest` reruns whole; one flow per item, not one over the scene), R3 (an unconditional frame tick with an `identical?` skip at the consumer; nothing scheduled inside a combine function; a reducer returns its accumulator on every path) and L11 (`m/relieve` on every DOM source). That wiring is not in either slice.

**Callers of the edge** (at `1501b85`; re-checked at landing): `harness/path.cljs` (`prepare-path-frame!` and `draw-path-frame!`, six call sites), `harness/region.cljs:1180` (the surround's path frame) and `:359-362` (`draw-path-instances!`, `item-range`). `region3d/on_plane_renderer.cljs` does not call the path renderer; it packs placements itself from the kind's regions and keeps its own per-placement reuse, untouched here.

---

## 9. One slice or two

**Two, in order, one landing session each.** The reasons: the executor's pending branch needs a read that can pend, and the only such read is the coating's, so the branch lands with the 3D brush or lands untested; the path pickup's host exists in the tree (the filler's twin, the dabs, the path value) while the 3D host is five new namespaces, so the shared pieces are first exercised against code that is already there and their surface of new work stays small; and the second slice then extends the first through the interfaces it left, with the first's records kept passing, which is the extension that answers whether the waist holds (two-chairs: "the extension through the existing interfaces"). Two-chairs' "Codex lands per kind" is this order.

**Slice A, the path kind's pickup** (the landing paste `production/STARTER-slice-A.md`; the handoff `path-kind/production/HANDOFF-2.md`): the executor of §3 with `:until` suspension, the continuation and its bytes, resume with the table and the recipe and consumed-items checks, the reads report, the refusals, the subjects, and no pending branch; `engine/surface.cljc` with the surface value, `new`, `paint` in its colour kind with the key, `sample` over a stack of surface layers, `mix`, encode and decode, `png-bytes`; the path kind's table in `construction.cljc` with `:path/dabs` and the engine's operations bound; `construct` over `run`; the pickup record and its five receipts (§5.3); the push edge with its tests and the re-measurement (§8); the deletions (§10); the READMEs and docstrings in the same commits.

**Slice B, the 3D kind's coating-reading brush** (the landing paste is `3d/STARTER-production.md` §2 as written, with one sentence added at its terrain: the executor's pending branch is its first milestone; the handoff `3d/production/HANDOFF-1.md`): the executor's pending and needs-policy suspension, `:snapshot` on read capabilities, the request, belonging by value, the `:from` check, the provisional mark through `:partial`, the before-loop phase; a non-surface layer in `sample`'s stack and the `:chart` domain through `ctx`; the five namespaces of §6.1; the brush and its receipts (§6.4); one harness leg; the READMEs.

Becomes a default only if the definer's next 2D record needs a read that can pend before slice B starts (a surface another record returned across the store, withheld), in which case the pending branch moves into slice A and slice B's executor work is nil. What changes with the cut: two pastes and two handoffs instead of one; slice B's landing session reads `HANDOFF-2.md` before this file's §3.

**Position 9's five slots, and where these two slices leave them.** Of "a region as a clip on a read, region algebra, a painting that covers a region, a distance field, a material", the two slices supply a painting that covers a region (the chart patch painted through a footprint region) and a region as a clip on a paint (the clip record, a second coverage multiplied in); they do not supply a region as a clip on a read (`sample` takes a point and no region), region algebra, a distance field as a foreign slot with a declared output (the sphere's own distance builds a cap and is not that slot), or a material. That is a departure by omission from "starts from five slots", and it is this session's position, not a ruling: the two slices are the executor and the compositor with two brushes as their measure, and the remaining four slots land as capabilities when the definer's records need them, each once with receipts, which is Position 9's own rule for a capability. It stops holding at Sid's word that the first vocabulary is whole before slice B closes; then the four become table entries in slice B with a definer record behind each, and the 3D landing paste gains that terrain.

---

## 10. Affected callers by name, and the deletion scope

Line numbers at `1501b85`; a landing re-checks them against the tree it starts from.

**Slice A changes:** `engine/executor.cljc` (rewritten to §3, the expression side kept whole); `engine/surface.cljc` (new); `engine/compositor.cljs` (three deletions; docstring); `engine/buffer_pool.cljs` (the direct row range write, §8); `engine/README.md` (the piece's two files; the executor's row); `path/construction.cljc` (the table in named form with `:needs`, the brush's `:path/dabs`, the engine's operations bound, the vocabulary; `default-construction` in the new grammar; `construct` over `run` taking `:results`); `path/records.cljc` (the fixtures' recipes in the new grammar; the pickup fixture); `path/renderer.cljs` (`push!`, `frame!`, the held view; `prepare-path-frame!` gone); `path/frame.cljc` (`frame-key` gone); `path/README.md`; `harness/path.cljs` (the six call sites; the pickup leg; `init-path-system`'s view); `harness/path_production.cljs:18-22` (the guard that redefines `executor/execute` names `run`); `harness/region.cljs:1180` and `:359-362`; `test/app/client/engine/executor_test.clj` (rewritten) and `surface_test.clj` (new); `test/app/client/path/construction_test.clj:41` and `test/app/client/region3d/path_placement_test.clj:14` (their `with-redefs` of `execute` name `run`); `test/app/client/path/pickup_test.clj` (new), `frame_test.clj` (extended as §8), `run_pure.clj` (the new namespaces added to the fast lane); the text goldens: the pickup's sha256 and texel, recorded on purpose with the reason in the commit.

**Slice B changes:** `engine/executor.cljc` (the pending branch, the request, the `:from` check, the before-loop phase); `engine/surface.cljc` (layer kinds; `:partial`; the `:chart` domain); `region3d/support.cljc`, `coating.cljc`, `surface_region.cljc`, `capabilities.cljc`, `records.cljc` (new); `region3d/README.md`, `engine/README.md`; `harness/region.cljs` (one leg); `test/app/client/engine/executor_test.clj` (pending, with a toy read whose `:snapshot` and `:run` the test defines; an answer routed past an earlier read); `test/app/client/region3d/coating_test.clj`, `surface_region_test.clj`, `brush_test.clj` (new); the text golden of the painting's sha256; the cold author's two fixtures copied under `test/app/fixtures/`.

**Deleted, as wrong form, not as uncalled:** `engine/expression.cljc` (the slice; one language); the base's `reread`, `rerun?`, `run-for` and the `!runs` atom (the slice; a cache keyed on observed reads); `executor/execute`, `run-steps` and the `:call`/`:bind`/`:each`-as-a-step forms (slice A; §3.1, one entry point and no compatibility door, so the four callers above move with it); `compositor.cljs` `strip-padded-rows`, `unpremultiply!`, `png-bytes!` (slice A; a read-back duplicated where nothing reads); `path/renderer.cljs` `prepare-path-frame!` and `frame/frame-key` (slice A; the walk the push edge replaces).

**Untouched by both slices:** `engine/coverage.cljs` (its compaction is a named propagation edge in §8, not changed), `engine/device.cljs`, `engine/leases.cljs`, `engine/rungs.cljc`, `engine/limits.cljc`, `engine/transform.cljc`, `engine/color.cljc`, `engine/schema.cljc`; `path/component.cljc`, `stroke.cljc`, `width.cljc` (their functions are wrapped in the table, not changed); every file of `region3d/` that exists today; `text/`, `image/`; the GPU goldens.

---

## 11. Unresolved choices, each a position with its condition

1. **Named arguments with `:needs`** (§3.1). Position: named, with groups for required and alternative names. Stops holding: never for a record; a landing that finds the expression compiler wants positional forms keeps expressions positional and steps named.
2. **The loop at the program's level with pre-loop steps** (§3.1). Position: taken. The first record beyond it is known (bench 9's nested proof record, a bench proof and not a production brush); the position stops holding when a production record needs a collection mapped inside the loop, and then the candidate is a bounded collection step (one level, no loop inside, an explicit output), a vocabulary addition with its test, before the executor door's exit (a designed total language in the leaves) is asked of Sid.
3. **A read is a map with a status, in both kinds** (§4.3). Position: the 2D `sample` returns a read, and the pickup's `mix` takes `[:get :sample :color]`; bench 9 returned the colour bare. Stops holding: no condition found; the barrier rule needs the status.
4. **Outside a surface's domain is transparent** (§4.1). Position: taken, with `:covered? false`. Stops holding if a tool needs the edge texel (a clamp-to-edge brush); then `:filter :clamp` is a declared filter, not the default.
5. **`:source-over` only** (§4.2). Position: taken. Stops holding at the first record that names another mode; the table grows by one function and one test.
6. **The subject of a value is the computation that produced it** (§3.5): the producing run's recipe, consumed items, resolved reads and output name, carried on the value by whoever retains it, and read from `:return`, which is the program's second reading (§3.5): the resume recipe starts from the loop and a result's subject from its output, and they part on a record without a loop. Position: taken; the record alone is not a subject, since one record under two hosts or two retained paintings gives two results. Stops holding if a subject proves too large to carry inside a value on a real document (a chain of retained paintings, each carrying its input painting by value); then a content address computed from the subject and checked against it, never a hash as the identity.
7. **The CPU runner first, the GPU road after a trace** (§4.5). Position: taken, under Sid's rule and Position 9's reference runner. Stops holding when a real brush at a real size shows the twin short of the experience; the measure is the frame, and the first thing the GPU landing checks is the 1.2e-7 agreement.
8. **Nothing of region3d moves** (§6.2). Position: taken. Stops holding at Sid's word for the painting on the sphere's screen image, which is the region-row split's slice.
9. **History is not part of the request** (§3.5). Position: taken, because a request pins the inputs of one transition and the history is the record of earlier transitions and what their reads resolved to; an answer belongs to a transition, not to a past. The history is not derived from the recipe, the items and the state (a record whose `:next` ignores its reads has one final state under two schedules and two histories, REFUTE-1 F13); it is retained as the log of resolved reads, and a run's subject carries those reads. Stops holding: no condition found.
10. **Two slices** (§9). Position: taken; the condition is there.
11. **The item is its declared fields** (§3.1). Position: taken; the whole element is never the item, and a record that consumes every field declares every field. Stops holding: no condition found; a record that needs a field it did not declare adds it, and the checkpoint that held under the old declaration is refused under the new one, which is right.
12. **The recipe reaches the caller's roots** (§3.5). Position: taken; where a value came from does not decide whether a transition consumed it. Stops holding: no condition found.
13. **A pre-loop read may pend** (§3.5). Position: taken, with `:phase` in the continuation and the state initialised on entering the loop. Stops holding: never for the two brushes, whose pre-loop steps are `new` and dabs; if the phase proves a burden before any record uses it, the alternative is a refusal of reads before the loop, and the two slices' records pass either way.
14. **Placements are the push edge's unit** (§8). Position: taken, with the draw item as landed. Stops holding: no condition found; a material changed under two placements is two entries, named by the caller.

---

## 12. Read derivation custody — ruled by Sid, 2026-09-07

Sid adopted the frame/session default after slice B landed, with this
correction: **"The thing that becomes a saved job is never the brush. It is
the read."** This closes the former open question and supersedes this
section's earlier proposal to persist an expensive operation's brush
continuation as a row.

| Thing | Ownership and representation — Sid's ruling |
|---|---|
| The stroke's record | A row at the edit boundary; uncommitted before settle. |
| The paused continuation | The session's uncommitted store, keyed by the read it waits on; never a row. |
| The read it waits on | A derivation job: a request row whose idempotency key is the read's full declared inputs as a value, run by the store's own runner. |
| The job's result | A derived row under that key, because later dabs and other tools read it. |
| The grant | The frame caller, per frame, under a budget; not a row. |
| Ownership | Whoever holds the key: a live view or a record naming it. |

The stroke record plus the derived rows it reads reproduce the paused
state. That reproducible state needs no custody beyond the session using
it. The derivation being awaited — for example, coating at a declared
resolution or a host-distance read — is what receives job/result custody
under its own full-input key. Sid identifies this as the same rule the
server's machine cut applies to its own runs; this amendment records his
ruling and does not claim a new verification of that server implementation.

**Two distinct identities.** The derivation key names that read's full
declared inputs at its own level. The executor's request (§3.5, with the
landed phase addition recorded in `3d/production/HANDOFF-1.md`) names the
consumer transaction as well: recipe, current item, state and position. It
continues to decide whether an answer belongs to a brush step. It is not
the shared derivation-job key. Later dabs and other tools can name the same
derivation key without sharing a brush continuation. The grant is a work
allowance, not a row or a semantic input to that key. No hash or observed
read set substitutes for the full declared input value.

**Evidence and implementation boundary.** The sphere brush's eager,
delayed and cold equality is exercised by
`test/app/client/region3d/brush_test.clj` →
`eager-delayed-and-encoded-continuations-keep-the-same-history`, the separate
JVM commands in `brush_wire.clj`, and the saved
[receipt](../3d/production/receipts/cold-resume.edn). Answer belonging is
exercised by `answers-refuse-edited-inputs-and-are-consumed-once` and
`test/app/client/engine/executor_pending_test.clj`. Provisional execution
still names the resolved/provisional reads it consumed; differing grant
schedules can produce different reads and histories
(`provisional-schedules-are-declared-and-distinct`).

The custody table is **adopted design, not implemented or integration-tested
by slice B**. The current harness supplies grants manually. Session storage,
request/result rows, the store runner and ownership by held keys remain to
be wired under this ruling. No such storage or cache is added by this
amendment, and no cost or performance claim for those derivations is made
without a trace.

REFUTE-1 closed with a second question, which this file now answers rather than leaves: when a tool's record is unchanged but a caller-supplied host or a returned value changes, the executor compares the recipe, which holds every root the transition reaches by value, the caller's included (§3.5), and the current item as declared (§3.1); those values are carried in the continuation's `:record` and `:scope`, from which the recipe is read, and in `:consumed`; a returned value carries the computation that made it under `:subject` (§3.5), read from `:return` so that a record without a loop names the roots its result came from, which is what a `:from` input is checked against and what tells two results of one record apart.

---

## 13. The tree this file is written against

`main` at `1501b85`: the path slice landed (`e8dcd37`; the language port and the boundary port at `2391644`; `path-kind/production/HANDOFF-1.md`), so the names in §3, §5, §8 and §10 are the tree's (`construction/construct` and its table, `stroke/dabs`'s nine-key packet, the draw item's three keys, the landed operator table and its refusals). The refutation whose findings are folded in is `production/REFUTE-1.md` (`564652f`); its detached worktree and its probes are the definer's receipts and are not in the tree. A landing that starts from a later `main` re-checks the line numbers here against it and brings a design difference back to this file rather than filling it.
