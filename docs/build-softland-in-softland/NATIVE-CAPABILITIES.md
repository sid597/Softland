# Native capability log

The first working browser pointer loop used source **`0501468`** (2026-09-11).
The real Region3D picker selected Orb and Ring; pointing at the visible instrument
selected Pointer and opened `targeting`; invalid executable material was rejected
while the accepted rule kept working. This initial receipt stopped at a test-driver
timing error before the acceptance demonstration. Everything in the table below
predates that loop. Later native additions are recorded separately below.
Initial implementation checkpoint: `eb7b940`. Sid completed normal Electric login.

| Compiled mechanism | Classification and concrete reason | Authored construction / check |
|---|---|---|
| Rama admission, row/version/index writes, contextual addresses | Required primitive. The prior `:edit/:create` tool schema could not admit a definition with a pattern, preserve a candidate layer or query its event index. Durable ordering and revision arbitration require an authority. | `module.clj`; real IPC admission, duplicate-envelope, promotion, removal and retained-version scenarios in `bin/inland check`. |
| Electric `ReadStatus`, layer/name resolution, indexed pattern reads, named step/call branches | Required primitive. The old `[:hit] / [:parent …] / [:or …]` interpreter had no named invocation, demand index or session effect result. Reads inside an opaque pure evaluator would be untracked. | `point-rule → pointing-target → targeting`, plus the editor and presentation definitions. The browser removes a parent read and checks proxy withdrawal while unrelated headline and 3D preparation counters remain unchanged. |
| Finite collection and record operations | Required vocabulary primitives, not a traversal helper. Existing numeric leaves could not remove one frontier item, append adjacent values or test visited membership. The authored `walk-step` explicitly performs those operations; `walk-from-scene` calls it. | Five finite read/value/call operations expose frontier/visited transitions; focused check reaches Assembly/Orb/Ring through a branch and cycle and exhausts a one-step budget. No claim that this vocabulary is universally sufficient. |
| EDN record parsing/printing | Required primitive for a record editor over native text input. Values are read as EDN, never evaluated as host source. | `edit-rule` sends text to Rama admission. Invalid syntax and unsupported step capabilities are rejected. |
| Demand-local support union and finite relational closure (`:query`, `:derive`) | Required primitive for multiple derivations and positive recursion. A single selected value loses the identity of alternative support. The finite solver receives all facts/rules explicitly and checks strata; it does not read the store. | `support-orb` and `support-ring` establish the same conclusion. `no-parent` tests complete lower absence. A finite recursive record in the focused suite derives reachability through a cycle, withdraws it on source removal and rejects self-negation. The default authored walk does not call this solver. |
| Session cell writes, admission proposals, event delivery, owner visibility/cancellation | Required effect primitives. The old UI had compiled `select`, `activate` and inspector branches. The replacement applies generic data effects. | All those application meanings now occur in seed rules, including `point-rule`, `inspect-rule`, `edit-rule`, `activate-rule`, `close-rule` and `reopen-rule`. |
| Repeated-step owner with yielding, budget and cancellation | Required scheduler primitive. A total recipe cannot schedule its own unbounded repetition. It receives state, then yields next state/result under an owner. | The authored `walk-rule` supplies the step reference, pinned definition basis, state, budget and output cell. The runner contains no graph traversal. |
| External executor, durable claim and outcome admission | Required external capability. Rama retries cannot own an irreversible provider call. A process lock owns execution; a durable claim precedes the call. | `ask-rule` creates accepted intent. Controlled failure/uncertainty and late-result ownership are checked separately from the real provider/browser demonstration; see the final handoff for its outcome. |
| Softland path/text/Region3D adapter, hidden native input, caret and selection geometry | Reused required physical primitives. Browser input/composition and GPU resources need native owners. | The previous experiment's `nodes`, `render`, `input`, `reactive` and geometry mechanisms were adapted. Labels, arrangement, pointing semantics, editor controls, lighting and instrument appearance are authored records. No native lens/tool renderer remains. |
| Font-provider disposal and failed-acquisition cleanup | Reused required resource primitive. A provider previously had no way to release its HarfBuzz handles. | Selected changes from `1ad55ee` were applied to current main's `fonts.cljs` and `shaper.cljs`; no broader source reset or merge. |
| Bounded native input viewport and render failure containment | Required physical boundary. A complete editor definition is taller than its own input field; the attempted full text layout would cover the authored controls. The input uses a caret-following scroll offset and a native GPU scissor; invalid paint yields a diagnostic. | `paint/Field`, `render/text!`, `paint/descriptions-error`. Browser editing passed; the opened-instrument capture shows the long record clipped inside its field with usable controls below it. |
| Jetty WebSocket message limit | Transport configuration, no language extension. The first browser connection produced `69,085 > 65,536` and closed before a usable pointer loop. | The normal Electric/Ring connection now has a bounded 1MB text/binary limit. No dependency internals or transport implementation changed. |
| Reactive collection completeness and one-shot event consumption | Required correctness of the declared pending/absence and command contracts, no new vocabulary. The first event was classified unanswered before an event-index subscription opened, despite Rama containing `point-rule`. | Index/dispatch/query outputs remain pending until every scoped branch reports. The repaired browser ran the pointing/opening/rejection loop. Completed events consume their local input after effects, so remounting does not replay them. |

## Native additions after the first pointer loop

1. **Surface/context lifetime repair — required ownership correction, no new
   vocabulary.** The candidate-context gesture closed and reopened the GPU owner
   (opened 1→2, closed 0→1), with a visible gap. A debugger trace reached the
   resource disposer through Electric/Missionary cancellation. Moving the context
   into `LiveContext` alone did not fix it. Making the visible root itself demand
   its surface retained the owner while all authored views were temporarily pending.
   The repeated debugger receipt kept opened=1, recorded zero disposals, and showed
   the candidate context with a live canvas. Context changes still re-resolve views;
   this receipt is about the surface owner, not zero work on a scope change.
2. **Input scissor delivery repair — required physical correction, no new
   vocabulary.** The first opened-instrument capture showed definition text
   covering its controls. The existing text draw helper replaces the incoming
   scissor; the adapter now supplies the input clip through that helper's public
   draw descriptor. The authored input construction is unchanged. The subsequent
   opened-instrument capture verifies containment.
3. **Repeated-step scheduling and outcome completion — required by the activity
   contract.** The authored walk requested a valid owner/budget/basis but stayed at
   iteration 0 without opening a pinned definition read. The runner now starts its
   output state in the owning event, keys snapshots by iteration state, and uses
   an initialized continuous timer input. A returned `:wait-ms` is bounded to one
   second; returned `:effects` allow bounded local/record work, at most one admission
   per iteration, with a stable activity/iteration request identity. Nested activity
   spawning is explicitly unavailable, so a child cannot escape the budget. The
   authored walk itself still owns every frontier/visited operation.
4. **Provider terminal-envelope parsing — required external-outcome correction.**
   The first real browser request returned output the single-object adapter did
   not recognize and was labelled failed. The raw response was not retained, so
   that attempt proves neither provider refusal nor a completed reply. Its test
   record is preserved. The adapter now accepts only an explicit terminal result
   from object/array/stream framing, otherwise records unconfirmed, and retains
   bounded usage/cost/retry metadata. The existing main Claude parser documents
   object and array formats; its raw-output fallback was not reused. CLI 2.1.268's
   installed code accepts `CLAUDE_CODE_MAX_RETRIES=0`; this owner sets it explicitly.
   A focused stream fixture then exposed a second framing defect: `json/read-str`
   accepts the first object without consuming trailing objects. The decoder now
   identifies newline framing before selecting the terminal result; the fixture
   checks the actual returned reply and stream classification. A direct bounded
   CLI probe exited successfully, but does not count as the in-product ask.
   The subsequent 120-token terminal error reported 480 aggregate output tokens;
   zero SDK retry events is not proof that the CLI makes no internal continuations.
   Minimal-customization mode reduced the reported prompt context but did not make
   that allowance sufficient. Raising the authored allowance to 400 produced a
   real accepted browser reply (247 output tokens, $0.002681, zero retry events).
   The native invocation now selects CLI safe mode and a small resident prompt;
   normal authentication/permissions remain active, with no available tools.
   This is external-adapter configuration, not a new authored-language primitive.
   No account or credential configuration was read or changed.

No native additions have been classified as optimizations: no measured need or
equivalence proof is claimed. No tool-name-specific native convenience is intended;
all five browser demonstrations and browser/app restart recovery have exercised the authored experience.
Sid's lived assessment remains separate from those checks.

Electric interfaces used: `e/defn`, `e/declare`, `e/client`, `e/server`, `e/input`,
`e/watch`, `e/diff-by`, `e/for`, `e/for-by`, `e/snapshot`, `e/When`, `e/Offload`,
`e/on-unmount`, `e/boot-client`, `e/boot-server`, its Ring WebSocket adapter and
Shadow reload hook. Dependency internals modified: **none**. Concrete friction:
fresh-checkout compiler activation; Jetty's default 64KB message limit;
foreign Missionary task macros must be kept
outside Electric bodies; accepted source reads must preserve pending/absence;
render occurrence identity must own GPU/native-input teardown.

Retention owners: Rama owns accepted rows, versions, decisions and activity state;
each Electric view owns its source proxies, local session cells and step work;
each render occurrence owns its target resources; the app process owns its Rama
connection, external executor and diagnostics. There is no accepted-world mirror
or application result cache. Finite derivation values are owned by their demand.

The final reply-preview repair is authored material, not a native addition: finite
`take`/`join`/`str` expressions add an ellipsis, and an ordinary `inspect` event
opens the complete accepted reply record. The record editor, targeting variation,
and authored walk demonstrations required no new tool-specific client branch.
