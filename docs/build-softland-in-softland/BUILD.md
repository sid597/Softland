# Build Softland in Softland

Prepared at Sid's request after the proposal discussion. **Ready for implementation
go-ahead; implementation has not started.** Approval of this brief authorizes
this experiment, including actual Electric in its isolated worktree. It does not
permanently replace main's host policy or assert that the model has been proved.

**The build.** One Electric host, Softland's existing text/2D/3D renderers, and a
small place where the pointing instrument, its targeting rule, its presentation,
the inspector and the definition editor are made from addressable definitions.
Match determines applicability; bodies call named definitions and return values.
Use the September 6 total-recipe restriction. Keep one real `ask`: one provider,
one bounded request and one accepted reply. No parallel Missionary implementation
is included. The same brief can be handed to another explicitly authorized builder
without changing the required experience or tests.

The [model](MODEL.md) explains the construction. This brief bounds the build;
historical proposals and the old diff are context, not additional scope.

**Starting code and custody.** Reuse the committed Smalltalk/Electric experiment
at `1ad55eeb14aec79618c6013755c6e7c17066779f` in
`/mnt/data/projects/codex-smalltalk-electric`. Its recorded source freeze was
`3b0ecec`; later commits retain the handoff and illustrated account. Its narrow
DSL and compiled selection/inspector wiring are evidence and reusable mechanisms,
not the finished definition-driven product this brief asks for.

After approval, create a fresh worktree, preserving the original and main's dirty work:

```sh
git -C /mnt/data/projects/codex-smalltalk-electric worktree add \
  -b codex/softland-in-softland /mnt/data/projects/codex-softland-in-softland \
  1ad55eeb14aec79618c6013755c6e7c17066779f
```

If that path/branch already exists, inspect it and choose a distinct unused name;
never reset, clean or overwrite it. Copy this brief and MODEL.md into the new
worktree's `docs/build-softland-in-softland/` so its handoff is self-contained.
No push or merge. Local commits on the implementation branch are allowed.

Read AGENTS.md and CLAUDE.md from that worktree. For boot, use the size list at the
end of this file and only the cited decision sections. Prior proposals, the full
vision log, the board's historical body and unrelated packages are not an opening
reading sequence. The direct build request and this approved scope authorize
Electric despite older reference-only guidance.

Useful source entry points, inspected as starting mechanisms:

| Source | Reuse or replace |
|---|---|
| `src-smalltalk/softland/smalltalk/execution.cljc` — `Read`, `Interpret` | Actual Electric branches with addressed reads; generalize beyond hit/parent/fallback |
| `src-smalltalk/softland/smalltalk/app.cljc` — `Gestures`, `Workbench` | Locate the compiled selection/editor wiring that must become authored behavior |
| `src-smalltalk/softland/smalltalk/nodes.cljc` — `Text`, `Path`, `Hit`, `Scene` | Electric ownership around Softland rendering; preserve resource lifetimes |
| `src/app/client/engine/executor.cljc` — `recipe`, `run`, `resume` | Existing total recipes, read barrier and continuation/state mechanisms; verify the needed semantics before reusing |
| `bin/smalltalk`, `deps.edn` alias `:smalltalk` | Isolated launch/build/check wiring; the alias pins Electric `v3-alpha-20260519.115706-45` |

Put new mechanisms in their own namespaces. Keep engine hooks small. Do not read
`src/app/server/env.clj`, inspect credential values, mutate `/mnt/data/rama` or the
preservation archive, or write to earlier experiment worktrees. Use a new runtime
directory inside the implementation worktree and distinct available ports; check
ownership before starting/stopping anything. Reuse Rama binaries read-only and
real isolated durable storage, not an in-memory cluster standing in for saved work.

**The encounter.** Keep one scene: a group with two visible parts and a visible,
selectable active instrument. A person can see what is selected, open the actual
definition behind the instrument, understand the rule, edit it, submit it, and
continue using it. The instrument opening into its construction is the visual
center. Retain the strong materials/depth of the existing workbench; make the next
action legible without reading a handoff. The editor may be small, but its rules,
bindings and presentation are authored material over generic primitives. Native
input, text composition, hit delivery, execution and rendering may be compiled.
Tool names, activation, selection effects and inspector opening are not client
branches. A dropdown selecting hardcoded examples does not satisfy the brief.

Add only the writing/reply surface needed for `ask`, candidate use/promotion,
unanswered meaningful requests, and the authored algorithm. This is not a desktop,
general scene editor, marketplace, full permission product or general source IDE.
The complete in-world compiler/renderer rebuild loop remains later work.

**Execution invariants.** Durable edits and definitions go through Rama admission;
shared presentation and active shared behavior follow accepted revisions. Drafts
and session facts have explicit owners. Pending, rejected and unconfirmed are
distinct; a rejected request keeps the accepted behavior. Save/retrieve tools and
candidates from Rama, with explicit identity and revision/context references.

Runtime-authored patterns, calls and reads must create real tracked computations
inside the actual Electric runtime. Index and scope matching by context and demand.
An opaque whole-world evaluation placed inside Electric, a manually synchronized
world mirror, an optimistic shared update, an application result cache or a custom
transport substitute does not meet the requirement. Query plans, continuations,
computed values and GPU resources are legitimate runtime retention with named
owners and cancellation. Preserve occurrence identity and multiple support for a
derived conclusion. Closing one view ends its work, not durable material or an
independently owned activity another view observes.

Maintained results withdraw with support; accepted strokes/edits persist; completed
commands are not replayed when a rule changes. Reinterpretation of old events may
be a maintained query. Absence requires a complete relevant read; pending, failed
and forbidden reads are not absence. Use positive recursive derivation with
stratified negation. Placement follows authority, dependencies, capabilities and
lifetime, with explicit server/client boundaries; view/rule labels are not placement laws.

**Activities.** An authored total step takes its state and declared inputs and
yields a next state/result/wait/effect request. Its owner repeats the step under a
budget, with cancellation and yielding. Existing executor continuations are a
candidate mechanism, not evidence that this entire path exists. Ordinary agent
content follows normal admission with a visible machine mark; changes to shared
behavior use candidates. This starting policy is material and editable.

For `ask`, admit a durable request with identity, owner, input/definition basis,
provider/model parameters, limits and status. The executor acts on accepted intent
and submits the reply through admission. Define single execution ownership,
duplicate-result handling, failure/cancellation/late-result handling and the point
at which an interrupted request becomes unconfirmed. No blind retry of a possibly
performed call. Closing a view does not undo a call or discard its accepted reply.
Use the normal runtime environment for provider configuration without reading the
forbidden file or printing secrets. If configuration is unavailable, continue
independent work and report the specific missing configuration; do not substitute
a canned reply. Real calls stay within explicit small request limits.

**Plan once, then build.** The builder writes a short checklist in its local
working notes: OWNER, SOURCE in this brief, DONE WHEN. State the concrete choices
for session storage, stable references/pins/removal, admission request identity
and preconditions, demand/read outcomes, the total-step record and activity
ownership. Make routine decisions and build through in-scope defects. Do not turn
the full model's open questions into a prerequisite matrix or a new contract series.

**Five focused demonstrations.** Add meaningful checks as the mechanisms are built;
run the experience in a real browser using the existing renderers.

1. **Open and change the active instrument.** Initially each part selects itself.
   Through ordinary addressing, inspect the actual executable definition, change
   it to containing-object-or-self and submit. Exercise a pending request, a
   rejected edit and acceptance. Only acceptance changes subsequent selection.
   Both parts then select the group; the tool still selects itself and can be
   edited again. Edit its presentation through authored material as well.
2. **Keep and recover a useful variation.** Create a second usable tool through
   the product, activate/inspect/edit both without name-specific code, and show
   candidate behavior separately from the base. Recover the accepted definitions
   in a fresh browser and after restarting the app against the same real isolated
   Rama storage. Show the defined pin/live and promotion behavior.
3. **Propagate and dispose narrowly.** Use two live views. Change an accepted
   definition/relation, observe the relevant changed branches and presentation,
   and record unchanged unrelated work. Remove a read, a matching support and a
   view; check withdrawal, retained alternative support, complete relevant absence,
   no completed-event replay, and release of view-only subscriptions/resources.
   The other view remains usable and durable edits remain present.
4. **Author and reuse an algorithm under the existing law.** Default case: walk
   connected scene subjects using an authored total step with frontier/visited
   state, including a branch and cycle, and call it from another definition.
   Show its result through an ordinary authored view. No native traversal helper
   may hide the algorithm. Exercise budget exhaustion/cancellation and explain
   the outcome while the interface remains usable. Record any expressiveness
   limitation with the attempted authored construction before proposing an extension.
5. **Use the resident.** Enter a request, invoke real `ask`, observe owned activity
   status and an accepted reply with machine provenance. Exercise a controlled
   failure or interrupted/uncertain call and show unconfirmed without blind retry.
   A reopened view retrieves accepted activity/reply state. Test doubles may
   exercise faults, but do not count as the real provider demonstration.

**What the handoff reports.** At the first working pointer loop, record the compiled
floor/vocabulary and its source revision. Log every native addition in the build,
including those made before this point; identify later additions separately.
Classify each as optimization, required primitive or convenience. Attach the
authored expression and equivalence check, the attempted construction and precise
limit, or the explicit convenience admission respectively. A failed attempt alone
is not a proof of impossibility. Record which Electric interfaces were used, any
dependency internals changed, and the concrete maintenance/integration friction;
do not substitute a speculative dependency-size ratio for those observations.

Judge that log alongside the actual experience, durable acceptance/recovery,
narrow propagation and disposal. Finish with one launch command and URL, a short
interaction walkthrough, the actual runtime/data path, focused check results,
three representative rendered captures, and failures/unproved claims stated
plainly. Separate source structure, executed receipts and Sid's lived assessment.
Passing checks provides evidence; adoption remains Sid's decision.

**Boot sizes (bytes at preparation).**

| Boot file or primary decision window | Bytes |
|---|---:|
| `AGENTS.md` at the pinned code base | 7,175 |
| `CLAUDE.md` at the pinned code base | 21,043 |
| Main `docs/decisions.md`: Electric-native arc through the section before the spatial model, including the scoped exception | 12,506 |
| Main `docs/decisions.md`: Tools are records over a vocabulary | 1,842 |
| `docs/build-softland-in-softland/MODEL.md` | 29,621 |
| `docs/smalltalk-electric/HANDOFF.md` at the pinned code base | 8,896 |
| `docs/smalltalk-electric/NOW.md` at the pinned code base | 1,373 |
| `docs/build-softland-in-softland/BUILD.md` (this file) | 13,272 |
| **Total** | **95,728** |

The scoped exception is recorded in main; the pinned code base predates it.
Approval of this brief supplies the isolated-build permission without copying
main's unrelated changes.

Use the codebase's instruction files, this brief, MODEL.md and the old experiment
HANDOFF/NOW; together they fit below the approximately 100KB boot budget. Read only
the decisions sections “Electric-native arc” including its scoped exception and
“Tools are records over a vocabulary”. The old full proposals are optional context.
