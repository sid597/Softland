**REFUTE-1 — receipts against DESIGN-1**

2026-09-07. Source checkout: `main` at `1501b85450965248d8471b1e8b8c661ea0254513`. The path landing is `e8dcd37981185a8c865bc4227e3800685cd429b9`, including the boundary/language port at `2391644`. The design under examination is the version last changed by `f846326f10b27efd2c8a8a017e12e2d0fa806051`: Git blob `2dd86dbcc79f14cef147ba7fa6c19d3d0379e6c7`, SHA256 `d2a20c5ab9829d91e0fad9184173287402598c825d86d51210bcaee9e81846b3`. Line numbers below refer to these bytes, before any repair.

This file contains findings about particular sentences and exchanges. Sid's executor, one compositor with paint and sample, Position 9, caching rule and no-trial ruling are inputs to the examination. The explicit choice to land CPU brush receipts first and leave the existing Region3D rendering tree in place is not treated as an accidental omission. There is no verdict on the design as a whole.

The proposed executor does not exist yet. “The design refuses” below means applying its written admission or comparison rule to its written values; it does not mean a new executor was implemented and tested. Executed receipts use the landed client or the benches' own functions. A working bench receipt is not, by itself, evidence that a differently specified production interface reproduces it.

**Execution custody.** The main pure suite ran with `clj -M:test -i test/app/client/path/run_pure.clj`: **47 tests, 399 assertions, zero failures or errors**. The two sphere commands, from the repository root, were `node docs/below-the-waist/3d/bench-2/node-route/probe-4.mjs --bench HEAD --dump` and the same command with `probe-2-3.mjs`: **46/46 and 56/56 checks passed**. Both executed the embedded bench, SHA256 `592372f97a9fba81c7e4a2edd3d6e95c7c79eddb36e6e5e9724af1cf09e176bc`; all **89** sidecar/embed custody comparisons matched.

The detached worktree `/mnt/data/projects/Softland-refute-1-scratch-20260907` is pinned to the source checkout above. Bench 9's `extract.js`, `harness.js` and `wire.js` ran from that worktree's root; `wire.js` also runs `regress.js`. The harness's existing absolute read of the main checkout's HTML is at `harness.js:3`; those bench bytes match the pinned worktree. The strict host returned the red and blue proofs, and the wire run reproduced all four edits and zero differing components after restoration/resume. Its content SHA256 is `9757e826f4594d5aa2c628302831ddee00dc57c8037340ee5bc14428d395aa82`.

Own probes ran only in that detached worktree and remain uncommitted. `refute_path.clj` reads the design examples and calls the real Clojure functions; `refute-bench.mjs` uses `loadBench('HEAD')`; `refute-path-bench.cjs` uses the strict path host; `refute_atlas.cljs`, compiled by `clj -M:test -i refute_compile.clj`, calls the real atlas, row packer and buffer pool in Node. Its device is a recording allocation/queue substitute: its receipts concern CPU slot and row handling, not GPU pixels. Raw outputs are `refute-pure.log`, `refute-probe-4.json`, `refute-probe-2-3.json`, `refute-cold-clip.log`, `refute-path-harness.log`, `refute-path-wire.log`, `refute-path.log`, `refute-bench.log`, `refute-path-bench.log` and `refute-atlas.log` there. The changed inputs and decision-changing outputs are preserved below so the claims do not depend on an unexplained pass flag.

**The hardest case, following the written design.**

| Moment | Taken path and receipt | Work concealed or contradicted by the named piece |
|---|---|---|
| Admit the sphere record | The record declares a `:painting` root and an `:out :painting`. D79 refuses an output naming a root. | The literal run stops before `fresh`, not at the withheld read. F1. |
| Assuming that collision is repaired, demand at budget 0 | The actual bench reaches item 0, leaves carry `[1 0 0 1]`, painting revision 0 and empty history, names B's withheld χ relation, and queues three events. Repeated withholding preserves the continuation. | The coating adapter must turn its nested composition into the common read value and preserve internal known marks. The general sampler's stated rule instead names only lower stack layers. F7. A's revision-2 chain is also misstated. F17. |
| Grant 1 and restart the item | The bench redoes the point/read, obtains `[0.25 0 0.5 0.75]`, mixes to `[0.8125 0 0.125 0.9375]`, and changes 401 texels. Each later read independently gets grant 1. | The production `resume` call has no capability-table argument, although its first check needs that table. F9. |
| Eager, delayed, cold | The shipped new-process cold probe agrees with eager. The additional check compares full histories, not just the shipped summary: eager/delayed and eager/JSON-restored histories agree; an ordinary checkpoint after two dabs also resumes to the same history and bytes. | These receipts hold for the named unchanged inputs. They do not establish the edited caller-root case or the serialization of reusable region inputs. F6, F9, F11. |
| Old answer delivered under reversed order | The bench refuses before a history row, with **recipe changed**; its independent reversed run starts with `[0.5 0 0.25 0.75]` and reaches `ae8c231c…`. | D381 says the refusal names the snapshot. D143's own comparison order also makes recipe the first difference. F10. |
| Old answer delivered under pickup .25 → .5 | Refused as recipe changed; the new run reaches `4df8a532…`. | This works for a record-owned pickup. It does not establish the same rule for caller-owned values. F6, F11. |
| Answer delivered again after completion | The bench returns stale because no request consumes it; the prior four history rows remain. | There is no current read request at which to discover a different state, contrary to D381's stated taken path. F10. Earlier reads also need an answer-routing rule. F8. |
| Declare provisional | Withhold only the first read, then grant the remaining three: `c1cf8170…`, only row 0 provisional. Withhold all four: `dd7d3355…`, all four rows provisional. | The record declaration does not select one of those grant schedules. D382 contradicts D145 and Position 12 here. The common sampler lacks the partial-value exchange needed to perform the guess. F7. |
| Run the path pickup to checkpoint 12 | The bench saves carry `[0.00031892763945506886 0 0.9996810704469681 1]` and `paint:11/surface`; straight and resumed final surfaces differ in 0/65,536 components. | The client needs the actual construction adapter and dabs extraction, then the same root-name collision repaired. F1–F2. |
| Apply the four checkpoint edits | Pressure edit fails at dab 0; pickup and surface edits fail fixed dependencies; the bench accepts the last-x edit. | The design's full-packet check must reject that last edit at dab 1 because `:s` changes. F5. |

The positive ordinary sphere arithmetic above is supported by all four rows, not merely the final hash:

| Dab | Read | Carry after | Changed texels |
|---|---|---|---|
| 0 | `[0.25 0 0.5 0.75]` | `[0.8125 0 0.125 0.9375]` | 401 |
| 1 | `[0.5390625 0 0.328125 0.8671875]` | `[0.744140625 0 0.17578125 0.919921875]` | 321 |
| 2 | `[0.5 0 0 0.5]` | `[0.68310546875 0 0.1318359375 0.81494140625]` | 405 |
| 3 | `[0.6631851196289062 0 0.2650909423828125 0.9282760620117188]` | `[0.6781253814697266 0 0.16514968872070312 0.8432750701904297]` | 401 |

The final SHA256 is `30cb13f2147784bdae44b4aa303d412c8980f00c09435c0983521cd291d75579`. The following findings do not refute those numbers.

**F1. Both worked records are refused by the design's own root rule.**

Design sentence: [§3.1, line 79](DESIGN-1.md), “an `:out` that names a root or `:state` (`:shadows-root`)” is refused before any step runs. Its scope rule at line 102 makes the record's fields roots. The sphere example at lines 318 and 334 has both root `:painting` and output `:painting`; the path example at lines 231–232 and 248–259 has root `surface` and output `:surface`.

Receipt: reading both literal EDN examples, and using §5.1's stated root aliases for the path record, the intersections of root names and step outputs are respectively `#{:painting}` and `#{:surface}`. This is not a missing capability in the implementation: it is the specified refusal applied to the specified first consumers. The benches allow these output names: [sphere `executor.js:257–258`](../3d/bench-2/node-route/executor.js), [path HTML:755–757](../path-kind/bench-9/waist-bench.html).

A landing must change the output bindings and every subsequent reference, or change the shadowing rule. Renaming the path paint output also changes the expected final key `paint:23/surface`, because D172 defines identity from item/step. The key receipt cannot stay unchanged by accident. No preference for either executor form is needed to establish this contradiction.

**F2. The landed path boundary cannot be called through the names, roots or result adapter the design leaves.**

Design sentences: [§5.1, lines 230–232](DESIGN-1.md) names “`records/construct` as landed”; §3.1 line 77 illustrates `:path/anchors`; §10 line 430 locates the capability table in `component.cljc` or `records.cljc`, and omits `construction.cljc` from affected callers. §3.2 accepts `record :program` plus its own roots, while the path example supplies `:path/program`, `:path/tool`, `:path/paint` and `:path/surface` and its expressions read unqualified roots.

Source and receipt: [construction.cljc:16–52](../../../src/app/client/path/construction.cljc) owns both `capabilities` and `construct`. Its table is `:path/source`, `:path/envelope`, `:path/dabs`, `:path/regions`; no `:path/anchors` is registered there. `construct` calls `executor/execute`, takes its `:value`, accepts a path or `{:path … :paint …}`, and returns a validated **component containing `:path/value`**, not a bare path. It supplies `:source`, `:tool`, `:paint`, `:identity` explicitly. [records.cljc](../../../src/app/client/path/records.cljc) contains fixtures. This is the boundary described by [HANDOFF-1:35–48](../path-kind/production/HANDOFF-1.md), not an inferred replacement architecture.

There are three separate adapter decisions to write: how a namespaced authored brush becomes the executor's program/root map; how L0's component yields the `path` supplied to L2 while preserving its paint overrides and numeric width parameters; and how the construction caller consumes the new `run` result. Merely rewriting recipes in `records.cljc` leaves the real `executor/execute` call in place. [harness/path_production.cljs:18–22](../../../src/app/client/harness/path_production.cljs) also redefines `executor/execute` to catch renderer recipe execution; renaming the executor API without updating this guard loses that check. The required pure suite has the same dependency in [construction_test.clj:41](../../../test/app/client/path/construction_test.clj) and [region3d/path_placement_test.clj:14](../../../test/app/client/region3d/path_placement_test.clj). Neither test file is in §10's affected list. Removing `execute` without translating these callers fails their namespace compilation; retaining a compatibility entry point would be another unstated choice.

The dabs adapter needs an actual value conversion too. [stroke/dabs:388–438](../../../src/app/client/path/stroke.cljc) takes **`[path opts]`** and returns **`{:dabs … :polylines … :length …}`**. The example binds that capability's output directly as the loop's sequence while passing named `path`, `tool`, `stroke`. The existing conversion is [component/stroke-options:92–112 and geometry:142–159](../../../src/app/client/path/component.cljc), followed by extracting `:dabs`. Calling these real functions on the example, filling only its width placeholder with `[:* [:get :size] [:get :p]]`, gives 24 dabs, length `281.93729461954166`, first radius `5.2`. The adapter is feasible; its selection of options and result is work a literal landing currently has to supply.

§13's amend is therefore more than updating line numbers. Its sentence that nothing in §§3.2–3.6 depends on the slice is contradicted by the roots used in those APIs and their recipe check.

**F3. “The expression language is kept” changes existing evaluated values and errors.**

Design sentence: [§3.1, line 76](DESIGN-1.md) enumerates the known operations and says every other vector is data. Its list omits `:exp`, `:floor`, `:step`, `:smoothstep`.

Source: [executor.cljc:10–21, 53–65](../../../src/app/client/engine/executor.cljc) implements all four and refuses an unknown keyword-headed vector. [executor_test.clj:16–42](../../../test/app/client/engine/executor_test.clj) pins both behaviors. The executed values are `[:exp 0] → 1.0`, `[:floor 1.9] → 1.0`, `[:step 0.5 0.7] → 1.0`, `[:smoothstep 0 1 0.5] → 0.5`; `[:typo 1]` throws `:executor/operator`. Under the written closed list and fallback, those are vectors instead.

This reaches a real consumer: [width.cljc:17–27](../../../src/app/client/path/width.cljc) calls `compile-expression`, then requires a finite numeric width. The operation list, unknown-vector rule, and preservation of that compiler API must agree. Named step arguments do not require changing numeric expression behavior.

**F4. The claimed grammar coverage fails the path bench's retained-proof record, and required arguments have no declaration.**

Design sentence: [§3.1, line 78](DESIGN-1.md), “both benches' grammars … are exactly this,” followed by “no record on either bench needed” a further iteration; §11.2 makes the first such need its exit condition. [nested.json:7–15](../path-kind/bench-9/node-route/nested.json) is already such a record: while iterating dabs, its record-defined `keep` maps over `tool.slots` and emits one `{label, surface}` record per slot. [HTML:763–764](../path-kind/bench-9/waist-bench.html) executes that definition. The strict harness's shipped record returns a red proof `[0.5 0 0 0.5]` and selected blue `[0 0 0.5 0.5]`.

Executed record edit: append `"second proof"` to `tool.slots`, append pen sample `[12,8,1,12]`, set spacing to 4. The same bench executes **two dabs, six steps**, returns **two** labeled red proofs at `[12,8]`, each `[0.5,0,0,0.5]`, and blue `[0,0,0.5,0.5]`. The design has no `definitions`, map expression or registered `keep` capability. Hardcoding a one-element vector reproduces the original display but loses this record's authored slot behavior; lifting the slots to the sole top-level loop loses the independent dab iteration. A bounded collection operation could fill the hole, but it is an addition to the named vocabulary, not an already-proven third-door necessity.

There is another translation change in the same receipt. The bench retains `state.base` although `next` names only `proofs` and `surface`; [HTML:774–779](../path-kind/bench-9/waist-bench.html) merges next fields. D108 **replaces** state. A field-for-field translation loses `base` after the first dab and cannot perform the second dab's paint. Explicit self-carry can repair it; the translation must say so. The sphere bench also merges next fields at `executor.js:320`.

Finally D77/D79 require missing **required** names to be refused before execution, while D87 supplies only `:args [:name …]`. The source table needs optional and alternative arguments: [sphere `executor.js:119–124, 139–146, 176–180`](../3d/bench-2/node-route/executor.js) accepts `seed` **or** `point`, optional distance/revision/order/budget/filter, optional clip and default opacity. Marking every listed name required rejects the literal sphere brush (no seed) and reach record (no point); marking none required cannot perform the promised admission. A required/optional/alternative convention or a validator is missing. The reach record also has no `each` or state ([records.js:21–35](../3d/bench-2/node-route/records.js)); the design should explicitly say how an absent loop runs and returns pre-loop outputs, rather than claim all four source records have the displayed shape.

**F5. The full consumed-item rule rejects the checkpoint edit the design promises to accept.**

Design sentences: [§3.5, lines 126 and 135](DESIGN-1.md) retain whole consumed items and compare them by value; §5.3 line 274 and §7 line 383 say the last sample's x `102 → 110` leaves consumed dabs 0–11 equal.

Taken client path: the exact §5.2 record → `construction/construct` → `component/stroke-options` → `stroke/dabs`. [stroke.cljc:411–428](../../../src/app/client/path/stroke.cljc) computes each packet's `:s` as traveled distance divided by the **whole subpath's** length. Changing the last x changes that denominator before the checkpoint:

| Quantity | Original | Last x = 110 |
|---|---:|---:|
| Total length | 281.93729461954166 | 287.81380155204914 |
| Dab 1 `:at` | 12.0 | 12.0 |
| Dab 1 `:s` | 0.04256265570042203 | 0.041693622527097206 |
| Dab 1 x | 34.711432206869915 | 34.711432206869915 |
| Dab 1 y | 36.2529357749294 | 36.2529357749294 |
| Dab 1 radius | 5.4292482159702615 | 5.4292482159702615 |

The whole-map comparison finds its first difference at **1**, and `:s` differs at every consumed index 1–11. Both runs still have 24 dabs. The pressure edit has its first difference at 0 (`:p`, `:r`, `:path`); pickup and surface edits leave the packets equal and change the referenced record roots, as the design expects.

Why the bench accepts is explicit, not speculative: [HTML:740–750, 787–795](../path-kind/bench-9/waist-bench.html) compiles the used item fields and keys their projections. The brush uses `dab.xy` and `dab.path`; [HANDOVER:187](../path-kind/bench-9/HANDOVER.md) says so. `wire.js:25–29` actually prints “last x 102 → 110 → loads.” The design's earlier recipe repair removed source from the fixed recipe but did not remove the whole-packet difference.

The design must choose between this full packet as the transition's declared input, with a changed expected refusal, and an explicitly defined narrower brush-item value whose full value is compared. Restoring an observed-read memo is not a repair licensed by the ruling. Omitting `:s` globally is not harmless: the grammar permits a later brush to read it, and width rules can depend on normalized progress.

**F6. Caller roots can change the saved prefix without failing any listed resume check.**

Design sentences: [§3.5, line 135](DESIGN-1.md) permits `opts :scope`, but defines recipe from **record** roots and compares consumed items. §6.3 line 340 explicitly supplies caller `host`, inviting `[:get :host :A :radius]`; §7 line 380 says the continuation holds every transition input. Holding the old scope is different from checking the new scope against it.

Source and executed witness: [sphere `executor.js:209–215`](../3d/bench-2/node-route/executor.js) supplies `host.A.radius` from retained record A; `sweep-and-sphere.js:212–216` stores that radius as data. Change only the pickup record's footprint-radius argument to `"host.A.radius"`. Run a prefix of two dabs while A's radius is 16, checkpoint it, then change that retained record data to 24. The probe changes no function. The recipe and authored events remain equal; the bench accepts the old checkpoint:

| Run under the new host | First two changed counts | Final painting SHA256 |
|---|---|---|
| Resume the old prefix | 1079, 881 | `a189c93027927929dc3d1392abd0566cd03bf7091a6c400e85ed80fb1bb32a55` |
| Fresh computation | 1555, 1349 | `1538f2ae8b2621694d7a963859065c2f27ef009b6536d98e051a976c8083edbd` |

The corresponding design expression `[:get :host :A :radius]` is syntactically visible to `references`, but `host` is not a root of this record. Excluding it from recipe, leaving events equal, and restoring the old valid-length state satisfies all four stated checks. Recomputing the current read's snapshot does not repair the two earlier paints. Substituting the retained data is the isolated probe's way of supplying a second host value; the corresponding production inputs can be two immutable host maps.

This does not say that an unchanged-input delayed run diverges; it does not. It says the edit rule lacks the dependency that decides whether a prefix can be reused. The path source exception cannot silently become an exception for every caller-supplied value. The design must name the full caller-input boundary for prefix validity, including how the source-to-dabs path is treated differently from host and reusable inputs.

**F7. The common sampler cannot yet carry the provisional value its hardest case consumes; the quoted provisional receipt also has the wrong schedule.**

Design sentences: [§4.3, lines 197–204](DESIGN-1.md) admit a pending layer read with `:known [name …]`, and say the returned status names “the layers below it” as known. §3.5 line 145 says the compositor composes known layers with missing ones transparent. §7 line 377 needs `:known ["kA"]` from a pending coating at the **bottom** of `[coating, painting]`.

Taken source path: [gLocate:264–294](../3d/bench-2/node-route/sweep-and-sphere.js) leaves known mark names inside its composition. [read-surface:151–169](../3d/bench-2/node-route/executor.js) handles the exception **inside the coating branch**: it looks those names up in `G_RECORDS`, composes their RGBA in saved order, continues through the later painting layer, and returns resolved color plus a provisional mark. The common `sample` contract supplies neither a partial color/partial layer list nor a declared way to request this interpretation from a layer. `ctx` at D104 has budget, at, step and record; the read step's `:pending` policy has no stated path into it. A name such as `kA` is not itself a sampleable generic surface layer.

Taken literal path: before the first coating layer there are **zero** lower stack layers. Applying D204 gives an empty lower-layer list, whereas D377 requires the coating's internal known mark. Returning immediately on that pending layer also does not specify how provisional sampling reaches the painting **above** it. A landing must define the partial read exchange, preservation of internal known contributors, passage of provisional policy, and continuation through higher layers. Calling this “the same function” at D208 does not supply those operations. This is where the one compositor's composition ownership needs an explicit exchange, rather than another kind-specific guess hidden in `read-surface`.

There is a separate numerical contradiction. [probe-4.mjs:99–105](../3d/bench-2/node-route/probe-4.mjs) runs budget 0 **until 1**, then budget 1 from that checkpoint. The executed alternatives are:

| Grant schedule, same provisional record | First carry | Provisional rows | Final SHA256 |
|---|---|---|---|
| First read 0; remaining reads 1 | `[0.875 0 0 0.875]` | `[true false false false]` | `c1cf81702b98fb287cbbb1432166d81e7a55750d3306c3934cd09528960d2c3d` |
| All four reads 0 | `[0.875 0 0 0.875]` | `[true true true true]` | `dd7d3355cb4adbb25d65cde25e3b5604e09708bfa8c2e97ec3c88c57e23f2f02` |

D355 supplies the first hash with plural marked rows and no schedule. D382 says “the history says so at every row” and that the selection is “not a race.” The first schedule's last carry is `[0.7258682250976562 0 0.0880279541015625 0.8138961791992188]`; the all-withheld last carry is `[0.773040771484375 0 0 0.773040771484375]`. D145 and [Position 12, line 571](../3d/3d-kind-2.md) already state the correct limit: retaining a provisional choice reproduces the selected history, not a schedule-independent selection. The test must name which history it is pinning.

**F8. The six request fields omit the current item, and answer delivery has no routing past an earlier read.**

Design sentences: [§3.5, lines 140–143](DESIGN-1.md) make request exactly `{recipe, consumed, state, at, step, snapshot}`, compare it whenever an answer is present, and name “another item or step” as a difference. The current item is neither the consumed prefix nor part of recipe when it comes from events.

Executed event witness: give each pickup event an `amount` of .25, change the mix argument to `event.amount`, demand item 0's read, then change only item 0's amount to .5. The current point, prior state, program, non-event record roots, consumed prefix and read snapshot are unchanged: all six design fields compare equal. The bench's old answer is refused, **“the answer belongs to another event or step,”** because [executor.js:291 and 334](../3d/bench-2/node-route/executor.js) also carry and compare the event value. The fresh first carries are `[0.8125 0 0.125 0.9375]` and `[0.625 0 0.25 0.875]` respectively.

This witness does **not** claim the reused read's color is numerically wrong: its sampling inputs are unchanged. It establishes that the design has dropped the bench's event-belonging condition while claiming the same “another item” refusal. Either include the current item's declared value or explicitly choose reuse across distinct current events when the read dependencies agree; that choice affects which stale receipt can be promised.

Executed routing witness: insert a copy of `picked`, named `preview`, immediately before `picked`; demand an answer for `picked`; deliver it into a fresh run with budget 1. The bench completes and logs delivery only at `picked`. Its guard at [executor.js:293–296](../3d/bench-2/node-route/executor.js) selects matching `at` and `step` **before** comparing belonging. D143 instead says that when a reached request and the supplied answer differ, the run is stale. At `preview` they necessarily differ in step. The design must specify routing and one-time consumption before it can reproduce this two-read record. “An answer no read consumes by the end is stale” does not supply that routing rule.

**F9. Cold resume lacks its capability argument and a portable value contract for all the values it says it carries.**

Design sentences: [§3.2, lines 84–87](DESIGN-1.md) explicitly supply a capability table to `run`; §3.5 line 135 calls `resume(continuation, opts)` and first checks that table's vocabulary; neither the continuation fields nor the displayed opts provide the table. D149 forbids retained executor state and host/store access. A cold process cannot recover implementation functions from a vocabulary sentence. [The bench's xResume:346–348](../3d/bench-2/node-route/executor.js) has a global `X_OPS`; that is the missing source-side mechanism, not an argument the new API already carries.

The landing needs a named table-binding route for resume. This is an API gap, not a request to serialize executable functions. The same cold path must settle the representation of non-painting scope values. [reachRegion:167–182](../3d/bench-2/node-route/sweep-and-sphere.js) returns callable `member` and `distance` closures. Executed receipt: the definer's `clip` record with the warm retained reach succeeds, changing **34** texels; the same region passed through JSON and back preserves its record/id but loses those functions and fails **`painting: a.clip.member is not a function`**. D133 describes typed-array encoding, and D298 names a future `member(region, point)`, but does not spell out the portable region fields and reconstruction/admission that replace the bench's closure-bearing result. The no-function rule at D214 makes this an actual translation obligation.

The ordinary pickup cold test cannot cover that obligation because its saved state is carry plus painting, and its unchanged host is reconstructed from the bench's code. Likewise, altering only the bench continuation's `capabilities` sentence and calling `xResume` still completes: the bench stores that sentence but does not check it. The production vocabulary check is a new behavior to implement and test, not an inherited bench check.

**F10. Two of the hardest case's stale explanations do not take the specified or executed branch.**

Design sentence: [§7, line 381](DESIGN-1.md) says reversing order is stale “naming the snapshot” and duplicate delivery is stale because “the state before differs.” D143 says to name the **first** differing part; D135 puts referenced coating values in recipe.

Receipt: the reversed order changes recipe before snapshot. [xBelongs:332–335](../3d/bench-2/node-route/executor.js) takes that branch and the run prints **`stale: the recipe changed since the read was demanded`**, with zero history rows. The design's own recipe-first check has the same consequence. Pickup .25 → .5 also names recipe, as stated.

For the duplicate in [probe-4.mjs:121–124](../3d/bench-2/node-route/probe-4.mjs), `from` is the completed four-event checkpoint. The loop has no event left; [executor.js:322](../3d/bench-2/node-route/executor.js) returns **`the answer was not consumed: no request pending at this state`**. No new request is formed and no state comparison is taken. The returned history has its existing four rows, not a new row. These are corrections to promised diagnostic receipts, not challenges to refusing the answers.

**F11. The input-subject check compares different kinds of records, and a producer record alone does not identify a returned value.**

Design sentence: [§6.5, line 367](DESIGN-1.md) says `:from {:record "reach@0" :output :region}` resolves a producing record and compares it by value with the result's own `:record`. §11.6 makes record size the only condition that would change that position.

Source and executed receipt: running the actual `X_RECORDS.reach` returns this owned record:

```edn
{:support {:id "G" :revision 0}
 :radius 150 :distance "surface"
 :seed [3.141592653589793 1.0471975511965976]}
```

The producing record has keys `id`, `what`, `support`, `tool`, `program`. They are not equal. [executor.js:119–128](../3d/bench-2/node-route/executor.js) constructs the small resolved argument record and passes it to `reachRegion`; [reachRegion:168](../3d/bench-2/node-route/sweep-and-sphere.js) copies that record. The Surface region noun's owned-copy repair prevents radius aliasing; it does not make that small record a copy of the whole producer. A literal comparison in D367 therefore refuses the correct reused reach.

The painting half is missing as well: [xMakePainting:88–91 and paint:176–185](../3d/bench-2/node-route/executor.js) return declaration, revision and bytes, **no `record` field**. The production surface shape at D160–172 likewise has no producer/output subject. Yet D356 requires the first painting as a declared input to produce `a5aba6ed…` and refuse another painting. The design must specify what is attached to a painting, who attaches it, and what record-name resolver the pure executor is handed. A string name plus a resolved input value cannot perform that resolution by itself.

There is a further condition beyond record size. Executed witness: one unchanged producer record starts from `inputs.start`; with an empty painting it returns `30cb13f2…`, and with the previous pickup painting it returns **`b39a8b14209c6af505cfaa318d2a971cad1ae28f68089b32146b75a9af3e3dad`**. This bench witness deliberately declares only the input kind, so its subject check is open; it demonstrates the dependency on the supplied value, not a successful closed subject check. D102/124 permit caller-supplied values too. Copying the producer record into both results would still give equal producer records and unequal results. Host values and provisional selected reads provide the same distinction.

The missing definition is which **production computation** the subject names: producer record, named output, resolved caller inputs and any retained read choices needed to distinguish its results. A complete subject may use the record as one component. Equating it with the record alone is not supported by these receipts or by Position 9's requirement to retain the subject actually used.

**F12. The bench does compare hashes as identities, and a changed painting map defeats those comparisons.**

Design sentence: [§3.5, line 143](DESIGN-1.md), “the seam bench hashed for cross-process printing, and its every check is an equality that holds on values.” Source comparisons contradict the first clause directly: [executor.js:267–268, 274–277, 332–335](../3d/bench-2/node-route/executor.js) compare byte/recipe/state/snapshot hashes, and compare hashed event values. The following executed case defeats the stronger claim too; it does not rely on finding a cryptographic collision.

Run the ordinary pickup eagerly. Use its painting as an input to a new run. Demand `picked` under that painting. Supply a copy with identical id, revision, grid and bytes but move the chart-domain u origin **+16**, from `588.3185307179587` to `604.3185307179587`. Both sample points remain within the patch. `xPaintingRef` at line 93 omits domain/grid/chart/filter; `xSample` at lines 99–102 consumes domain/grid. The input byte-hash check, state identity and request identity therefore remain equal.

| Read/run | Executed value |
|---|---|
| Demanded read under original map | `[0.7226240485906601 0 0.23589350283145905 0.9585175663232803]` |
| Fresh read under shifted map | `[0.650836169719696 0 0.272350013256073 0.9231862425804138]` |
| Deliver old answer under shifted map | `complete`; final SHA256 `d26d2e6c35c24ea3462d22ac03520f574f60b9f9bbb3c126e3cc51104cad5bb5` |
| Recompute under shifted map | final SHA256 `c3571f5316e89ba280f85706d2b3dde5031332a59230497a683bca887e7c7652` |

This is an accepted old answer with a demonstrably different consumed color and different resulting bytes. The design's full-surface snapshot at D206 **would reject** the changed map and is supported by this counterexample. The finding is that the design's claim about the bench's existing proof is false, and that porting its `xPaintingRef` comparisons would violate the new rule. Hashes printed in this report are output receipts, not a proposed replacement identity.

**F13. History is not derived from the three values used to justify excluding it.**

Design sentence: [§11.9, line 450](DESIGN-1.md), “the history is derived from the recipe, the consumed items and the state.” D145 already admits two selected provisional histories for the same record depending on scheduling.

Executed record witness: take the provisional pickup and change only its `next` to retain `state.carry` and `state.painting` unchanged. Its steps still perform the named reads and paints; the rule is a legal record over those operations. Run once with budget 0, once with budget 1. Recipe and events are identical, final state is identical by serialized value, and histories differ. At the first read one history retains color `[0.5 0 0 0.5]`, contributors `kA, pickup-G@0`, and the missing-B provisional interpretation; the other retains `[0.25 0 0.5 0.75]`, contributors `kA, kB, pickup-G@0`, with no provisional mark.

This refutes the derivation sentence. It does **not** establish that history must be part of every read request. Excluding history remains defensible if transition semantics are defined by current explicit inputs and resolved reads. The justification must then be that semantic boundary, with the selected read inputs retained for history reproduction. [The bench:228, 281, 291, 317](../3d/bench-2/node-route/executor.js) actually hashes history into input state and hence requests; removing that dependency is a declared design change, not a description of its existing transition.

**F14. The push grammar loses an existing two-placement scene.**

Design sentence: [§8, lines 394–398](DESIGN-1.md) keys `:upsert`, `:remove` and `:order` by `material-id`, one draw item per key. Its comment says this is the slice's draw item.

Source receipt: [harness/path.cljs:55–58, 150–165](../../../src/app/client/harness/path.cljs) defines a draw item as `{:id id :path/material record :container group}` and actually renders **the same record** twice in the tree golden: `:path-tree/root` in group 0, `:path-tree/child` in group 17. Both carry material id `:path-golden/group-tree`. These are distinct placements of one value, not duplicate records with coincidentally equal content.

The proposed upsert map can keep only one value under that material id. Putting the same id twice in `:order` still selects the same stored group twice. The literal exchange cannot say the existing fixture. It needs placement/draw-item identity for membership and order, and a stated relationship to material identity when one shared value changes. This also changes which ids `:reran` names and what “one item” means; silently manufacturing new material identities for placements would alter the input model.

**F15. The push tests contradict real view inputs, including two inputs the design itself already names.**

Design sentences: [§8, lines 404–406](DESIGN-1.md), “a group's move … visits nothing,” “a group's scale enters through its items' bucket,” “a zoom inside a bucket reruns nothing.” D404's preceding clause and D47 already say device width depends on projected scale.

Taken client path: the landed border fixture → `construction/construct` → [frame/item-view:22–31](../../../src/app/client/path/frame.cljc) → [geometry-inputs/geometry:120–159](../../../src/app/client/path/component.cljc). It explicitly uses projected scale for device width and snap, and `fraction(tx × zoom + pan)` for snapping. The own probe computes the actual geometry, not only its key:

| Edit to the border | Before/after projected scale | Before/after bucket | Geometry equal? |
|---|---|---|---|
| Zoom 3 → 3.5, identity group | 3 / 3.5 | 1 / 1 | false |
| Group x translation 0 → .1, zoom 3 | 3 / 3 | 1 / 1 | false; pan fraction 0 → .30000000000000004 |
| Group uniform scale 1 → 1.1, zoom 3 | 3 / 3.3000000000000003 | 1 / 1 | false |

[frame_test.clj:25–40](../../../test/app/client/path/frame_test.clj), passing in this run, already pins the scale/fractional-pan effects and screen-group exception. The zero-rerun sentences would contradict those fixtures, not optimize them.

Even a cubic-free unchanged region needs a new **cover** when its bucket changes. [renderer.cljs:197–231](../../../src/app/client/path/renderer.cljs) keeps its pack under `:all` but cover per bucket. On the first pickup disc the actual cover rectangles at buckets 1 and 2 are `[20.3 22.3 31.7 33.7]` and `[20.55 22.55 31.45 33.45]`. Those rectangles enter instance rows. “No repack” is supportable for that region; “never reruns” is not if it includes its cover/row level. The changed-level test needs to distinguish those operations and include group-derived view inputs.

**F16. Keeping the existing atlas and pool leaves both a required propagation edge and the unchanged-item walk.**

Design sentences: [§8, lines 404–406](DESIGN-1.md), `push!` runs for “the named items and no other,” and an edit among 1,600 “visits one”; `:reran` is the test receipt. §10 line 436 leaves `coverage.cljs` and `buffer_pool.cljs` untouched. The design does not name an alternative upload/slot protocol.

First taken path: [coverage/retain!:378–395](../../../src/app/client/engine/coverage.cljs) compacts once garbage exceeds half the written curve texels; `rebuild:364–376` resets both fill positions to zero and rewrites live packs. The executed real-atlas probe inserts a two-quad pack and then a one-quad pack, then drops the first. The unchanged second pack's slot moves:

```edn
{:curve-base 4 :band-base [6 0]} -> {:curve-base 0 :band-base [0 0]}
```

[coverage/pack-instance:218–244](../../../src/app/client/engine/coverage.cljs) embeds that band address in the row. Actual packed uint32 word 4 changes **6 → 0**. Keeping the survivor's old row samples the old band address. The existing renderer avoids that stale address by rebuilding rows after `retain!` ([renderer.cljs:279–299](../../../src/app/client/path/renderer.cljs)). A push landing must propagate slot relocation to every affected row, or specify a different slot-stability mechanism. That work is triggered by another item's removal, so an unconditional “named items and no other” promise is false.

Second taken path: [buffer_pool/batch-update-pool!:77–99](../../../src/app/client/engine/buffer_pool.cljs) iterates all new rows and compares each by index. In Node, using the actual function, 1,600 instrumented equal-valued rows followed by an edit to row 20 produce **1,600 row comparisons, one reported write, queue offset 80** (four-byte probe rows). One changed row is not one visited row. Calling this existing edge cannot establish the promised removal of the walk; a different update entry point or a renderer-owned direct update route has to be stated. The measured claim here is the count, not an extrapolated frame time.

Order and range changes need the same treatment. The existing renderer flattens per-item rows and derives ordinal `item-ranges` at lines 279–295; `harness/region.cljs:359–362` consumes those ranges. A removal or a changed region/cover count can change later row positions even when their material values do not change. The design supplies `:remove` and `:order` but no rule saying which row/range dependents enter `:reran`. Reading a set of ids that the renderer reports as rerun cannot prove that no other rows were visited. The test receipt must observe the relevant work, as the count probe does.

There is also an initial-view convention to name: `push!` receives diffs/groups, `frame!` receives view, yet `push!` promises to run the view-dependent geometry and pack levels immediately. The current `init-path-system` has no logical view argument. The design must choose when the first view becomes available and which call owns first preparation; this cannot be recovered from the deleted whole-frame call by its new signature alone.

**F17. Several cited receipts identify the wrong quantity, source location or fixture.**

These are bounded corrections to what a landing is told to pin, each with a taken source path.

| Design sentence | Source and receipt | Required correction |
|---|---|---|
| D272: “24 dabs, 23 paints,” from HANDOVER:209 | [HANDOVER:205–206](../path-kind/bench-9/HANDOVER.md) reports **one copy plus 23 in-place** GPU paints, depth 24. `regress.js:15` executes 72 steps for 24 dabs and prints **24** consumed paint calls on its CPU host; line 209 is the unmasked group-layer row. | 24 semantic paint operations. Keep host allocation/reuse counts distinct from paints and from the surface revision. |
| D271 cites HANDOVER:154 and `regress.js:16` for the nine-digit texel | [HANDOVER:150](../path-kind/bench-9/HANDOVER.md) carries the precise pickup number; line 154 is the network account. [regress.js:16](../path-kind/bench-9/node-route/regress.js) rounds components to four decimals and actually prints `[0.0134,0,0.9866,1]`. | Cite the precise definer row. The own client composition probe reproduced `[0.01336952205747366,0,0.9866304397583008,1]`; there is no numerical refutation of that target. |
| D275: “`regress.js` prints” all 24 dab centers and radii | [The whole 18-line script](../path-kind/bench-9/node-route/regress.js) prints count, step/release information and rounded colors; it exports a `run` whose result contains dabs. Executing it produced no packet table. | Name extraction from `run(pickup,…).dabs`/`dabsOf`, or name a new receipt-producing command. Do not send the landing to a printed table that is not emitted. |
| D274 cites HANDOVER:267 for the four edits | [HANDOVER:267](../path-kind/bench-9/HANDOVER.md) is the checkpoint saved from the GPU and its cross-host resume differences. The edit outcomes are in line 251 and the executed `wire.js:25–29`. | Correct the source anchor while repairing the full-packet comparison in F5. |
| D377: revision-2 binding chains `[φ]` and `[β φ χ]` | [gBindings:244–254](../3d/bench-2/node-route/sweep-and-sphere.js) gives A **`[φ χ]`** and B `[β φ χ]` at revision 2. The executed held request is made from those chains. | Include A's χ in the snapshot. The short chain belongs to the earlier chart-change revision, not this merge. |
| D300/D310 translate four `records.js` fixtures; D359 pins the “clip” at 6096 | [records.js:238–318](../3d/bench-2/node-route/records.js) has a 64×32 patch `[900,180,100,60]`, radius-4 dab at q1; executed with reach it changes **34** texels. The correctly named **cold-author** receipt is a different record: [cold/reach-dab.json](../3d/bench-2/node-route/cold/reach-dab.json), 128×64, patch `[308.3185307179587,50,640,250]`, radius 200. [COLD-ACCOUNT:38–59](../3d/bench-2/node-route/cold/COLD-ACCOUNT.md) records 6096 versus 7502. The separate `probe-4.mjs --bench HEAD --record …/cold/reach.json --then …/cold/reach-dab.json` run completes with **6096** and hash `26da35e30f3934be07e95efaf3c327739a49462a0e9ad4c8f3b379b1e8221b5b`. | D359 is not a false number: it correctly says cold mind. The gap is that the fixture/file list only names the four `records.js` translations, none of which is this record. Add the cold fixture to the landing's named source and distinguish the two clip tests. |

D352 also labels any unmatched sphere hash with components within 1e-6 a finding “(operation order).” Executed counterexample to that attribution: change only the authored pickup from `.25` to `.25000001`, keeping the exact same bench implementation and operation order. The maximum component difference is **5.960464477539063e-8**, within the design's tolerance, and the hash changes to **`4ce490fc5e0c07b8fc6be085ae2a7c14cb0acb068d1ca20de33ab68521c326ea`**. A slightly wrong translated input can therefore pass that component bound and produce a hash mismatch. The prescribed record's current hash matches; a future mismatch needs its cause established from a taken path, not assigned from the tolerance alone.

**F18. “Only four support functions; the rest reads through them” is not the named port's dependency structure.**

Design sentence: [§6.1, line 302](DESIGN-1.md) promises a second support kind can add one namespace with no change to coating, surface-region or table because they use only `distance`, `chart->point`, `point->chart`, `pieces`. The table at lines 296–299 names concrete functions to port.

Taken source path: [reachRegion:170–182](../3d/bench-2/node-route/sweep-and-sphere.js) computes saturation, area and world bounds directly with **G_R**, spherical cap sin/cos and a sphere unit-vector seed, not through any of those four operations. [xRegionPieces:191–203](../3d/bench-2/node-route/executor.js) uses the cap's angular radius and sphere-specific longitude/latitude cases. [gBindings/gLocate:244–294](../3d/bench-2/node-route/sweep-and-sphere.js) use the concrete chart maps/periods and retained A/B restrictions. The actual coating table rejects any named curve except `kA/arc-AB`.

This does not prove that another host is impossible, or call for landing one now. It shows that **porting those bodies by name is not yet the four-function dispatch design claimed**. Keeping the sphere port while marking the future protocol incomplete is a defensible default; promising no changes to its consumers requires allocating area/bounds/saturation, curve addressing and binding correspondence to explicit support values/operations. HANDOFF-5's owed general smooth/trimmed host does not provide that missing interface. The four-function claim must be narrowed or completed before a later landing treats it as established.

**F19. The first-vocabulary ruling and the slice's vocabulary have an unrecorded gap.**

Design sentences: [§1, line 15](DESIGN-1.md) carries Position 9 as given; §6.1's table and §§9–10 specify the first 3D vocabulary and its complete landing scope. [Position 9, line 559](../3d/3d-kind-2.md) ends with a concrete production obligation: “Production's first 3D vocabulary starts from five slots: a region as a clip on a read, region algebra, a painting that covers a region, a distance field, a material.” The source is a ruling-bearing position, not a request to introduce runtime code.

The design's written exchanges cannot express those slots as specified: `sample(stack, point, filter, ctx)` has no read-region argument or declared region masking policy; the capability list has no region algebra; `surface/new` requires an authored plane/chart grid and no region-covering declaration; the table has no distance-field or material value slot. The three cold-author gaps are independently named at [README:128](../3d/bench-2/node-route/README.md) and [3d-kind-2:605](../3d/3d-kind-2.md). Paint's optional **clip** does not implement a clip on a **read**, and a host distance used to construct a cap is not the foreign distance-field output slot.

The two current brushes' successful numbers do not refute these obligations. The finding is the unsaid disposition: the design explicitly postpones the general host and the screen rewrite, but neither allocates these first-vocabulary slots to the two slices nor states a departure from that first-vocabulary scope. The repair belongs to the design's account of the ruling and cut. This file does not choose a new ruling or demand a trial to settle it.

**F20. The future Missionary reducer sentence has the wrong accumulator result for its own API.**

Design sentence: [§8, line 408](DESIGN-1.md), a flow of diffs reduces “with `push!` as the reducer.” The displayed `push!` at lines 394–398 takes `system, diff` and returns `{:reran …}`, not system.

Taken call sequence, independent of scheduling: first reducer call is `push!(system, diff1) → stats1`; second is `push!(stats1, diff2)`. The second call has no renderer system. The actual Missionary recipe in [electric-docs, R3:278–288](../../../.agents/skills/electric-docs/SKILL.md) explicitly returns its next accumulator on every path; the law about a pure combine function does not fix a reducer's return type. Grant reduction has the analogous need to say whether its accumulator is a continuation, a run result, or an owner containing one.

The design correctly leaves this wiring out of both slices, and no current Missionary failure is claimed. The finding is in the literal wiring sentence: it needs an accumulator-preserving wrapper, a closure over the stable system, or a changed return contract. Per-item flow granularity and the unconditional frame edge are separate choices and are not refuted by this arity/data-flow mismatch.

**F21. The suspension state machine leaves two reachable cases without a specified continuation path.**

Design sentences: [§3.1, line 64](DESIGN-1.md) runs arbitrary capability steps before the loop; §3.5 lines 116/135 allow a read to suspend and say state initialization is **never** evaluated on resume. Its continuation has item position and carried state, with no “before state initialization” phase. A pre-loop read is not forbidden by admission.

Apply those clauses to a record with a pre-loop read and `:each :state {:carry [:get :pre-read :color] …}`. With budget 0, the read suspends before initial state can be evaluated. On grant, pre-loop steps rerun, but restoring the pre-initialization state and forbidding initialization leaves `:state :carry` absent. A literal landing must either refuse suspending pre-loop reads, retain a phase that distinguishes this boundary, or initialize exactly once after that read resolves. The two worked brush records happen to have only `new`/dabs before their loops, so their green receipts do not select an answer. The source benchmark's simpler single-loop executor does not supply this added pre-loop lifecycle.

There is also no run-result transition for the common sampler's `:unsupported` value. D201 admits it; D116 lists only pending/needs-policy suspension; D94–99 list no unsupported run result; D149 prohibits a dependent step consuming an unresolved read. The bench actually returns unsupported before the next step ([executor.js:310](../3d/bench-2/node-route/executor.js)). The design must assign this case to refusal/error with its preserved reason, or explicitly add the status, rather than leave the landing to decide what happens when a named layer/filter/curve is unsupported. This is a missing branch, not evidence that it should be treated as transparent.

**F22. The pure paint signature has no input for the result identity it must produce.**

Design sentences: [§4.1, lines 167–172](DESIGN-1.md) make a surface's identity its producing item/step and parent; §4.2 lines 181–186 expose `paint(surface, region, paint, opts)` with coverage, clips and bounds in opts. None of those arguments carries the producing item or step. The executor's `ctx` does carry at/step, but D214–215 only describe binding coverage and domains into the engine operations, without assigning the result-identity handoff.

Taken source and executed receipt: [path HTML:727, 842–856](../path-kind/bench-9/waist-bench.html) explicitly passes `ctx` to the host, and `resultKey` uses its iteration and step. In the definer's two-paint proof, change the blue step's RGBA to the same red `[1,0,0,1]`. Both paint calls now receive the same base surface, region, color, opacity and blend, but produce keys **`proof:0/red`** and **`proof:0/blue`**; both samples are `[0.5,0,0,0.5]`. The distinction comes from the call's place in the record, not from the pigment or the surface revision.

A pure function of the four listed argument values cannot recover that distinction. The engine must receive a declared result location, or the capability wrapper must attach the identity after painting. The design has not selected that boundary. It matters immediately to the path's `paint:23/surface` receipt and to the renamed output needed by F1, without requiring a GPU allocator or any new cache.

**Where the ten positions hold, and where a different position is supported.**

The following records the consequences of the findings; it does not propose a replacement architecture.

| §11 position | What the receipts support; where a different choice is only a default; what changes with it |
|---|---|
| 1. Named arguments | No contrary position. Both benches' records support named arguments. F4 requires declared optional/alternative arguments, and F2 requires changing the actual caller. Width formulas can keep positional operator operands independently. |
| 2. One top-level loop | The stated “no record needed more” support is false: `nested.json` already maps a collection inside its dab computation. A bounded collection capability is a supported candidate before claiming a total-language exit is necessary. It remains a default until its exact input/output form is designed; it changes vocabulary and the translated proof record, not Sid's executor ruling. Partial-state carry and absent-loop execution also need explicit translation. |
| 3. Read maps with status | No contrary position. The barrier and its positive unchanged-input receipts support status-bearing reads. F7 concerns the missing partial-value protocol, not returning bare color again. |
| 4. Transparent outside domain | No counterexample to this declared change for the two worked brushes; the first consumers remain in their patches. Bench clamp/unsupported behavior is explicitly acknowledged by the design. There is no receipt here requiring clamp as the default. |
| 5. Source-over only | No contrary position for the named brush arithmetic. The path CPU probe reproduces the quoted texel with the proposed premultiplied formula. A different blend remains a vocabulary addition when a record requires it. |
| 6. Subject equals producer record | F11 supports a fuller subject identifying the producing computation and output, including caller inputs and selected reads when they distinguish results. The exact representation is a design default still to be written. Choosing it changes result envelopes, input declarations/resolution, continuation encoding and stale checks; record size is not the only condition that matters. |
| 7. CPU first | No contrary position. The receipts here are CPU semantics, with the atlas probe bounded to CPU custody. No GPU throughput or parity result from the old benches is promoted into new client behavior. |
| 8. Existing Region3D tree stays | No contrary position for these CPU brush slices. D306 names the retained tree and [HANDOFF-4 §6.7](../3d/HANDOFF-4.md) reserves the listed space/view/portal/lease and rendering changes to Sid's word. F18 addresses the promised support extension interface, not an unauthorized tree migration. |
| 9. History outside request | Exclusion can stand on a pure current-input transition boundary; the stated derivation proof cannot. F13 supplies equal recipe/items/state with different legitimate histories. Adding history back merely because the bench hashes it is not supported as necessary. Keeping it out changes the bench's belonging semantics and requires a distinct retained history-input account. |
| 10. Two slices | No receipt here requires changing the ordering. The pending branch can land with the coating read. The cut must explicitly place the omitted fixture and vocabulary obligations in F17/F19 and leave an A-to-B capability/continuation interface that can actually be supplied on cold resume. |

**The exact remaining question.**

The design's §12 asks who grants work and where the continuation lives. The harness can already supply a grant explicitly; that owner choice cannot settle the value comparisons above. Bench 9 offers a program/read-field projection and a surface declaration; the sphere bench offers recipe/state/request hashes and a small region argument record. The receipts show why none can simply be cited as the complete production answer: whole dabs reject the promised edit, caller-root edits reuse the wrong prefix, a producer record does not distinguish its differently supplied results, and byte-only painting identity accepts the wrong mapped sample.

**When a tool's record is unchanged but a caller-supplied host or returned value changes, what exact complete value does the executor compare to decide whether its saved prefix and the answer for its current item still belong—and where is that value carried in the record, continuation and returned result?**

---

**Second round, against the repair at `9ca4a57` (Codex, 2026-09-07; pasted by Sid into the design session, verbatim).**

Yes—one concrete hole remains in the new subject rule. I checked the repair at 9ca4a57 and ran the reach example in the detached scratch worktree.

§3.5's recipe rule (docs/below-the-waist/production/DESIGN-1.md:143) starts from the loop's steps, :next and state initialisation; it excludes :return. But the reach record has no loop: its pre-loop surface-region step reads support and tool, then returns the region. Applying the rule literally reaches neither root.

The executed receipt is the definer's reach record (docs/below-the-waist/3d/bench-2/node-route/records.js:6), with only its radius changed:

| Radius | Run | Distance to the definer's q1 | q1 inside? |
|---|---|---|---|
| 150 | complete | 144.54684956268315 | true |
| 140 | complete | 144.54684956268315 | false |

Those are different returned regions. Yet the specified subject (docs/below-the-waist/production/DESIGN-1.md:155) is identical: same program, no reached roots, no consumed items, no reads, output :region. The :from check compares producer roots only "wherever the subject's recipe holds them," so it cannot reject the retained radius-150 region when the caller resolves reach@0 to the radius-140 record.

The different regions are executed evidence; the subject collision follows from the repaired rules as written. The repair needs to distinguish the dependencies of a resumed transition from those of a returned result. A returned result's subject must reach through :return and its pre-loop producers, including records without a loop. Simply putting every pre-loop root into the checkpoint recipe would undo the pickup's last-x acceptance.

Two other points:

- The last-x projection already has stronger evidence than "derived from the table." My original probe compared every field of all twelve prefix packets. Dab 0 was equal; dabs 1–11 differed only in :s. That includes an executed comparison of their complete :path values. The saved output (/mnt/data/projects/Softland-refute-1-scratch-20260907/refute-path.log:5) records it. The landing should still pin the new executor's acceptance, but the input equality itself was checked.

- The five-slot cut remains your scope decision. The new paragraph makes the departure explicit. My finding established the mismatch with Position 9; it did not give me authority to order four additional capabilities into slice B. Naming a proposed departure also does not amend your ruling by itself.

I have no further push for the collection operation: explicitly naming the nested proof as beyond the grammar answers the coverage claim I challenged. The subject problem above is the concrete addition I would send back to the design chair. I made no file changes.

*Disposition (the design session, same day): folded into DESIGN-1 §3.5 as the program's second reading, from `:return`; the reach numbers above are in the design's text; the F5 receipt is cited at §5.3; the five slots stay the chair's position under Sid's word.*

---

**Third round, against `e3fe8e3` (Codex, 2026-09-07; pasted by Sid into the design session, verbatim).**

I checked e3fe8e3. The reach counterexample is closed: reading from :return now brings support and tool into the region's subject. I also verified that the resume recipe's rules, the resume procedure, the item projection and the slice A paste are unchanged.

There is one remaining error in the new subject paragraph, in this sentence:

> "an output that does not reach :state has empty :consumed and :reads."

That is correct for the reach record's reads, but not for every record the design admits. §3.5 permits pre-loop reads (docs/below-the-waist/production/DESIGN-1.md:123), and a read may declare :pending :provisional.

I tested that case in the same detached scratch worktree, using the bench's existing executor and capabilities. The probe record computes the brush's first point—kA/arc-AB at t = 0.65—reads the coating at G@2 with pending: provisional, and returns that read. It has no loop or state. The record and every root remain identical between runs:

| Grant | Run | Returned colour | Provisional? |
|---|---|---|---|
| 0 | complete | [0.5, 0, 0, 0.5] | yes |
| 1 | complete | [0.25, 0, 0.5, 0.75] | no |

The taken path is xRun → curve-point → read-surface (docs/below-the-waist/3d/bench-2/node-route/executor.js:139). These are executed bench results. Translating this record into the design's pre-loop grammar is permitted by its stated rules.

Under the repaired subject rule (docs/below-the-waist/production/DESIGN-1.md:155), both subjects still have the same program, reached roots and output name, with empty consumed items and empty reads. The returned values differ, but the subject discards the provisional choice that distinguishes them.

The small correction is: retain the reads reached by the returned output, including pre-loop reads and their provisional marks, independently of whether the output reaches :state. A record without a loop still has empty consumed items. It has empty reads only when its output depends on none.

That preserves the reach repair and the pickup's checkpoint rule. It is the one clause I would send back. No files changed.

*Disposition (the design session, same day): the clause is replaced in DESIGN-1 §3.5; a pre-loop read reached by an output enters that output's `:reads` with its provisional mark; the probe's two colours are in the design's text.*
