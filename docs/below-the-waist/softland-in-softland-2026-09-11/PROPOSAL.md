# Proposal: the layer between the store and the renderers becomes records

A position from the 2026-09-11 chat, written to be attacked. It says what to
build from where Softland is, why, and how, and what to show first. Claims
about what exists are graded: **checked** means read in source this session,
**said** means a builder's or chair's statement, **position** means this
proposal's stance. Facts with file and line anchors: `FACTS.md` beside this
file. Section 12 records the holes found in the proposal's own drafts and how
each was closed, so an attacker can see what was already tried.

## 0. The aim this proposal serves (Sid's, as this chair holds it; Sid redlines)

**What.** Softland is the place where Softland is built. Everything above a
minimal floor, views, tools, the editor, the inspector, the arrangement of
the scene, is made inside, stored as records, by humans and agents together,
and each thing made is usable to make the next. Sid, 2026-07-30: Smalltalk-
like from the start, only a minimal VM-like floor beneath. Sid, 2026-09-02:
anything above the waist is data, creatable, saved for reuse, higher-order
things built from it, infinite compositions.

**Why.** A land renovatable by its inhabitants down to bedrock. Nobody needs
a terminal to change how the place works, and asking for a change is free
for a small model or a person pointing. Softland's own making is the first
territory the map serves.

**How.** One general in-land language over records. Reactive and
incremental, so a change reruns only what read it. Rama holds accepted
state one revision per entity, so many hands work without merge deadlocks.
The floor is minimal and its line is designed to move inward; zoom 100, the
code editor inside, comes when the need arises. There is no current loop and
no future loop: the pointer is the first test of a property the whole system
must have in every case.

**Turing complete, in this proposal's sense.** The in-land language has
conditionals, unbounded recursion, data construction and abstraction by
naming, so any computation over records is expressible, and a program can
read and write programs because a program is a component like any other.
That is the definition to work towards. Sections 6 and 7 say how the
language meets it and how the walk tests it.

## 1. The proposal in five sentences

Softland has three renderers that take a description and make pixels. Between
the store and the renderers sits a layer with a fixed job: records to
descriptions, pixel to record, gesture to change. In every build so far that
layer was code, and only a few of its knobs were records. This proposal moves
the layer itself into records of three kinds, views, tools and behaviors, run
by one runner that knows no view or tool by name, over a floor made of the
renderers, the store's primitive writes, the reactive runtime and raw input.
The first build re-hosts the accepted workbench's own verbs as records and
proves it by deletions.

## 2. Where Softland is (checked unless marked)

- `main` at `43e6a94` has the engines: text, path, region3d, compositor, and
  an executor that evaluates expressions and runs one-loop recipes over a
  compiled capability table. Its expression operators are numeric and
  comparison only. No product shell; client roots are probes.
- The accepted build (`codex/smalltalk-electric`, 1149 lines) is a working
  shell: a Rama module holding entities and components partitioned by
  workspace, Electric peers, rendering through Softland's own engines, a hit
  registry and a 3D pick that yield entity ids. Its only record-authored
  behavior is a three-word selector, `hit`, `parent`, `or`, interpreted at
  gesture time. Everything else in the layer is code: a compiled component
  tree (`ui.cljc`), a compiled dispatcher of five event kinds (`view.cljs`
  `deliver!`), every button a closure, a closed operation set of seed, edit,
  create (`module.clj` `outcome`), and the click's consequences, select and
  open, as compiled resets (`app.cljc` `Gestures`). A new gesture replaces
  the previous one in a single atom, so an in-flight gesture branch is
  cancelled by the next click.
- The stopped build had bindings as records whose `:does` named one of two
  compiled handlers, on a whole-document store.
- The reactive builds showed that a computation declaring what it reads gets
  rerun by the runtime with no hand-written rule list, and that hiding a view
  releases its subscription (said).
- The wall is the one seen from the block side on 2026-07-30: then the
  structure was code and the values were material; now the arguments are
  material and the verbs are code. The direction doc of that day pre-named
  behavior bodies as the wall that would remain.

## 3. Why this and not something else

**Why the layer's job is not in question.** Every build wrote the same three
arrows: read records and emit renderer descriptions; take a pixel hit and
name a record; take a gesture and change records. Nobody designed that job;
it was forced by a store on one side and renderers on the other. The only
open question was whether the layer could be records. The proposal does not
invent a layer. It moves one.

**Why records and not a component library in code.** The workbench already
is a component tree in code, the same shape as any UI library. That shape is
right for composition and wrong for where it lives: source files, a text
editor, a rebuild. Softland in Softland means the three parts every
component has, what it looks like, what it does when touched, what it is made
of, live in the store and are changed by pointing. Smalltalk's screen was a
component library inside the image.

**Why one runner and a dumb floor.** Sid's worry: the system becomes the
grand orchestrator that knows everything, when it could just know whether the
question can be asked and whether someone will answer it. Every compiled
`case` in the layer is the floor knowing an answer. A runner that looks up
the binding for this gesture and runs whatever record it finds knows only
whether someone answers. That is what makes "change it and keep using it"
free: nothing compiled changes because nothing compiled knew.

**Why ECS is half of it.** Entities and components give identity, structure,
addressability and references. They say nothing about how one record invokes
another or when a write is allowed. The accepted build's own conclusion was
that addressability mattered most and no scheduler was needed. The proposal
keeps the noun half as it is and adds the verb half: programs as components,
one runner, a closed primitive set.

**Why reactive.** A view record says what it reads. The runtime reruns that
view when those records change and nothing else, and drops its reads when
the view goes away. The accepted build already does this for one thing: the
`parent` read opens when the rule mentions it and closes when it stops. With
views as records that property covers the screen without anyone writing a
rerun rule. Missionary and Electric carry reads and lifetimes; they author
nothing, which is why they are floor.

**Why behaviors at the pure grain.** The selector, "the containing subject
of this thing", is wanted by the click and by the hover preview. The click
writes; the preview must not. Splitting behaviors into pure ones and event
ones lets both use the same record. This is the grain Codex named, and it is
what makes a behavior a building block for the next thing rather than a whole
tool copied.

**Why the language is general, and why its form is rows.** The aim is a
system that builds further, so the in-land language must express any
computation over records: named behaviors with inputs are functions, `run`
is application, `:if` is choice, recursion is unbounded iteration, maps and
vectors are data. Its form is flat rows of steps because that is what a
small model edits correctly, Sid's easiness bar from July, and an agent
changes a tool through the same operations a person does. Form and
generality are separate choices. Eval and SCI are not the road to
generality; they are the road to editing the floor's own Clojure from
inside, which is zoom 100 and a different thing.

**Why not more ECS first.** Kinds as records, a scheduler, a placement
language: none of them moves a verb. The ECS round's reads said the record
shape was cheap every time.

**Why not a DOM or React shell.** Rendering through Softland's engines is
settled; the DOM shortcut was stopped on 2026-09-10 for that reason.

**Why the accepted worktree as the base.** It has the store, the wire, the
pick and the rendering connected and receipted. Starting elsewhere repeats
the divergence that produced five record shapes and four store paths.

## 4. What: the floor

The floor is code, fixed for this round, and small. It reads six component
names as its lookup protocol and nothing about what they contain:
`:home` on the workspace root entity, `:active-tool` and `:shows` on a
session's root running use, `:bindings` on a tool, `:program` and
`:schedule` on anything runnable. It writes a fixed set of facts on running
uses: `:of`, `:subject`, `:aim`, `:pointer`, `:pending`, `:error`, and the
drafts that `field` nodes declare. Raw input handling, pointer movement and
native text input, is floor code and stays.

- **Renderers.** Text, path, region3d, compositor. Unchanged. They take the
  description node kinds the accepted build already uses: text, path, scene,
  hit. One kind is added: `field`, a text input bound to a named running-use
  component, the accepted build's `Field` made a node kind. The keyboard, the
  caret and the shaping are floor.
- **Input.** The canvas delivers gestures, down, up, click, key, with the
  pixel hit resolved by the floor: the hit registry for drawn nodes, the BVH
  pick for 3D. A gesture reaches the runner as
  `{:gesture :click :hit "orb" :node <hit node or nil> :view <running use> :at [x y]}`.
  Hover is not a gesture: the floor writes `[use :aim]` and `[use :pointer]`
  on the running use under the pointer, and programs read them. A click with
  the modifier held arrives as a different gesture kind, `:meta-click`; the
  floor knows the modifier, not its meaning.
- **The store.** The accepted module generalized. Entities and components
  partitioned by workspace; a `:revision` component per entity maintained by
  the store and bumped by any accepted write to that entity; decisions
  deduplicated by request id. It accepts one operation kind: a batch of
  primitive writes, `put` component, `remove` component, `create` entity,
  applied atomically with one decision. Each write carries an expected
  revision when the run that produced it read the target's `:revision`;
  otherwise it carries none and the store applies it without a check. So an
  edit made from an inspector is compare-and-set and a blind recolour is
  not. A fresh workspace is seeded by one batch whose content is a data file
  in code, the way an image ships its first objects; that file names `:home`
  on the root, the first view to show. A `put` of `:program` may carry the
  program as EDN text; the module parses it, rejects a parse error with its
  reason, and stores data, as the accepted module does for its rule today.
  Programs are validated at acceptance inside the module (section 6); the
  executor's admission is `.cljc` and runs there. The `$$tools` catalog becomes a `:tools` component on the root,
  written by a behavior like any other component. A `remove`, or a change of
  `:schedule` from `:pure` to `:event`, is refused while the entity's id
  occurs as a value anywhere in the workspace's accepted components; an
  over-approximate scan, cheap at this size.
- **The floor's surface.** Everything the floor can do is exposed to the
  language once, not demo by demo: every existing capability table (the
  region3d and path vocabularies the recipes already run over), every node
  kind the renderers accept, every store write, every running-use write, and
  every floor-written fact including time as a readable source. If the floor
  can do it, a program can say it. Section 6 lists the workbench's subset by
  name; the rule is the union.
- **The runner.** One language, one admission, two hosts. The language is
  the existing executor's, extended (section 6). The Electric host runs
  programs on two schedules: *event*, once per gesture, may write;
  *continuous*, per mounted view, every `read` a narrow subscription, reruns
  when a read changes, yields a description tree, may not write. The
  executor's existing pure host with byte continuations keeps running
  recipes, painting and the sphere, unchanged. The two hosts share the
  expression compiler, the step shape, `references` and admission; the step
  loop exists twice, the accepted build's `Interpret` is twelve lines of it.
  That duplication is named, not hidden.
- **The event queue.** One gesture in flight per session, first in first
  out. A gesture's branch runs to its end, batch committed or error written,
  before the next starts. The accepted build replaces instead, which cancels
  an in-flight gesture on the next click; a queue is the change.
- **Mounting.** The floor mounts exactly what `[session :shows]` lists, a
  vector of `{:view address :subject address}`, seeded from `[root :home]`;
  a child `view` node inside a description mounts a nested running use under
  its parent's lifetime.
- **The reactive runtime.** Missionary and Electric, as in the accepted
  build.

## 5. What: the records

Records keep the accepted build's shape: an entity id maps to components.
Three record kinds exist by convention of which components they carry; no
kind field, and no kind is needed by the floor. Pure means no writes; a pure
program may read.

- **Behavior.** `{:label "containing-subject" :schedule :pure :program {…}}`.
  A program with named inputs and a return value, callable by address.
  `:schedule` is `:pure` or `:event`. An `:event` behavior may write and may
  be run only from event programs.
- **Tool.** `{:label "Pointer" :bindings {:click "pointer-click"} :presentation "lens"}`.
  Bindings map a gesture kind to the address of an `:event` behavior.
  Presentation is the address of a view. A tool has no program of its own; it
  is bindings plus a look. Another tool wants the behavior without the look
  or the running state, so the behavior is the unit of reuse.
- **View.** `{:label "Library" :schedule :pure :program {…}}`. Its return is
  a description tree: nodes of kind text, path, scene, hit, field, and child
  nodes of kind `view` naming another view record and a subject. Every node
  carries a stable id. A `hit` node names the entity it stands for and an
  action: the address of an `:event` behavior, or `:tool`, meaning defer to
  the active tool's binding; absent means `:tool`. A button is a `hit` node
  whose entity is the behavior it runs, so a `:meta-click` on it opens that
  behavior. A `field` node names the
  running-use component it binds, so two fields in one view never collide.
- **Running use.** Created by the floor when a view is mounted or a session
  opens; disposed when it unmounts. An entity like any other, never accepted,
  never sent to Rama, addressed by the same paths. Components: the floor's
  facts (section 4), field drafts under the names the fields declare, and
  whatever event programs `set`. The session root carries `:active-tool`,
  `:shows`, `:selected` and `:open`, because selection and the open editor
  are shared by every area of the screen, as they are in the accepted build.

The three-word language dissolves. `hit` is the event's `:hit`. `parent` is
`read [thing :parent]`. `or` is `:if`. Nothing is special about the pointer.

## 6. How: the program language and the runner

Position: extend the existing executor's language and admission rather than
write a third evaluator. The accepted build already wrote a second one. The
expression language stays: `:get`, `:literal`, lazy `:if`, arithmetic.
Programs stay steps of `{:out :op :args}` with one loop. A keyword `:op` is
a primitive; a string `:op` is an address. Six extensions:

1. **`:op` may be an address.** The runner runs that record's program in a
   nested branch under the caller's step, with `:args` as its inputs and its
   own bindings; the caller sees only the return. This is `run`. Composed
   reads inherit cancellation because the callee's reads live in the
   caller's branch: the accepted build's nested `parent` read is owned by the
   branch that mentions it and closes when it ends.
2. **Environment roots.** Every program sees `self` (the record running),
   `view` (the running use it runs in), `session` (the root running use) and
   `subject`; event programs also see `hit`, `node` and `event`.
3. **`:each` over maps, without loop state, with `:key` and `:index`.** The
   stopped build ended on the missing map enumeration. A pure map over a
   collection is the common case for views; `:key` names the field that
   identifies an item, the item itself when scalar; `:index` binds the
   position. Loop state stays available for recipes.
4. **Expression operators for data**: `:str`, `:conj`, `:without`,
   `:count`, `:nth`, `:keys`, `:assoc`, `:dissoc`. The table today is
   numeric and comparison only. With these, maps and vectors are
   constructible, and a program can build a program as data.
5. **World primitives**, a closed table, by schedule:
   - pure: `read [entity component]`, `run` of a `:pure` target, and the
     description constructors `text`, `path`, `scene`, `hit`, `field`,
     `view`.
   - event, in addition: `set [use component value]` on running-use state,
     applied when the step runs; `new-id`, a fresh id per step; `put`,
     `remove`, `create [id components]` on accepted material, collected
     during the run and submitted as one batch after the last step; `show
     [view subject]`, which appends to `[session :shows]`; `run` of an
     `:event` target. A `put` of a `:program` component is an ordinary
     write, so a program builds programs.
   - both, beyond the workbench's subset: the whole floor surface of
     section 4, by the union rule.
6. **Admission by schedule**, at acceptance in the module. Every keyword
   `:op` is in the table for the program's schedule; every static address in
   `run`, `view` and `hit` exists; a computed address that is missing at run
   time fails into `[use :error]`; a `:pure` program reaches no event
   primitive and no `:event` target, checked transitively by walking targets'
   programs with a visited set that stops at cycles. Recursion is allowed: a
   behavior may `run` itself or a chain that returns to it. The guard is a
   budget of steps per gesture and per rerun, carried in the environment the
   way the executor already carries `:budget`; exceeding it fails into
   `[use :error]`, never an admission rule that caps what can be built. One
   walk per accepted edit at workbench size.

**Dispatch.** A gesture arrives with its hit, its node and its running use.
If the node carries an address, the runner runs that behavior with the
node's entity as `hit`. Otherwise it reads `[session :active-tool]`, that
tool's `:bindings` for the gesture kind, and runs the bound behavior. That
is the accepted build's order, hit registry before scene pick, made general.
A `:meta-click` skips the node's action and goes to the active tool's binding
for `:meta-click`; the pointer binds it to `pointer-click`. That is how a
button, or any node with an action, can itself be pointed at and opened: the
halo gesture, Smalltalk's answer to the same problem. No case on gesture
kinds exists; the floor asks whether a binding exists.

**Late binding.** The runner keeps the reach live: the active tool, its
bindings, the behaviors, every action address in the mounted description
trees, and every program any of them can `run`, as one continuous
derivation, the way the accepted build keeps the active tool's
definition subscribed so activation carries address and definition together.
At a gesture the runner snapshots that live reach; no round trip, and one
gesture runs against one consistent set of definitions. Reads of material
resolve live and may be pending; definitions are frozen for the gesture.
References follow the latest accepted revision. Pinning to a revision is out
of this round (section 10).

**Lifetimes.** An event program is an Electric branch that lives until its
last step, then commits its batch, then ends; the queue starts the next
gesture after that. If its running use unmounts first, the branch cancels:
`set`s already applied are gone with the use, and no batch is submitted. A
continuous program's branch lives with its running use.

**Identity of steps.** The accepted build keys its Electric loop by opcode
and names stable authored-node identities as the next load-bearing step.
The runner keys each step by `[phase :out :op]`, and each loop item by its
`:key`; `:out` is single-assignment per phase already. Editing one step ends
that step's branch and its reads; the others keep theirs.

**Levels.** Each step is its own branch with its own reads, so a change to
one read reruns the steps that read it and nothing above them: a camera
change reruns the scene step and not the label steps. The input levels the
reactive builds measured fall out of per-step identity; nobody declares
them.

**Effects and reruns.** A write inside a continuous program would fire on
every rerun. That is why the schedule split exists and why admission refuses
event primitives in `:pure` programs. Effects belong to event programs, once
per gesture. Views are pure derivations.

**Text editing.** The editor is a view whose tree includes a `field` node
bound to a named draft component. The floor writes the draft on input
events; the apply button is a `hit` node whose action is a behavior that
reads the draft and the subject's `:revision` and emits a `put` of the
subject's `:program` with that revision expected. This round edits a
program as text; `program-rows` is for reading and pointing, not for editing
a row in place. Editing rows in place is a view and a set of behaviors like
any other, and comes after the walk passes.

## 7. What to show first: the walk

In the accepted worktree, on its own stage, with the orb, the ring, the
assembly and the pointer. Each step is a deletion plus an addition.

1. **Store.** `outcome` becomes batch writes with program admission; the
   seed, edit and create branches are deleted; `$$tools` becomes
   `[root :tools]`; `:revision` per entity; `:home` on the root.
2. **Runner.** The six extensions; `Interpret` generalized to run steps
   keyed by `[phase :out :op]`; the primitive table; dispatch by node action
   then binding; the event queue; the live reach; mounting from
   `[session :shows]`.
3. **Seed records.** Pure behaviors `containing-subject` and `target-preview`
   (the hover text, which runs `containing-subject` on `[use :aim]`). Event
   behaviors `select`, `open`, `activate`, `close-view`, `apply-edit`,
   `keep-variation`. Tool `pointer` bound to `pointer-click`, whose rows are:
   run `containing-subject` on `hit`; `select` the result; `open`. Views
   `library`, `tool-row`, and `program-rows`, which shows a program as rows
   with each `run` step's target as a `hit` node naming that record.
   `tool-row` and `program-rows` are the ECS round's record-authored
   inspector: field choice, arrangement and actions in the record. Frame,
   Stage, Attention and the editor's layout stay compiled this round and
   still name `assembly`, `orb` and `ring`; that is the red left on the map.
   Their buttons' actions become addresses, and compiled views mount record
   views through the same `view` node. The compiled resets in `Gestures` are
   deleted.
4. **The second tool, from inside.** Keep a variation of the pointer; open
   its click binding in `program-rows`; change its rows to
   `put [hit :shape :color] coral`. Name it recolour. No compiled change.
5. **The shared behavior, from inside.** In recolour's rows, point at
   `containing-subject`; it opens as its own record; change it to return the
   thing itself; the pointer, the preview and every tool that runs it change
   behavior.
6. **A program that writes a program.** Author a behavior `make-recolour`
   whose rows read a colour from `[session :selected]`'s shape, build a
   click program as data with `:assoc` and `:conj`, `create` a tool with
   that program bound to click, and `conj` it onto `[root :tools]`. Run it
   from a button. A tool made by a tool appears in the library and works.
   This is the step the aim requires and the walk did not have.
7. **Recursion.** Author `depth`, which runs itself on `read [thing :parent]`
   until there is none, and show it in `tool-row` for a nested subject. The
   budget is exercised by a deliberately cyclic parent, which fails into the
   error, not into a frozen screen.

What Sid sees, moment by moment. Point at the ring: Assembly is selected, as
today, and the preview under the hand says so before the click. Pick up
recolour from the library, point at the orb: the orb turns coral, in the
other session too, and the ring's preparation count does not move. Point at
recolour: its rows open, one of them says `containing-subject`. Point at that
word: it opens as its own record. Change it, accept. Point at the orb with
the pointer: the orb itself is selected now, not the assembly, and the
preview said so first. Point at the library: its program opens. Add a
revision column, accept: the library shows it, and nothing else on the
screen re-prepared. Hold the modifier and point at the Apply button: the
button does not fire; `apply-edit` opens as its own record, and the editor
is on the same list as everything it edits.

**Pass is deletions, plus two constructions.** `Gestures`' two resets, the
three operation branches, the five-kind case, every button closure, and the
library's compiled loop are gone from the tree. Recolour and the third tool
needed no compiled case. A tool was made by a tool, and a recursive
behavior ran and was stopped by the budget.

**Numbers, as files the harness writes.** Subscriptions opened and closed per
edit, as the accepted build already counts. Scene, headline and label
preparation counts unchanged across tool edits. Library reruns on `:tools`
reads only. Run time per rerun of `library` and `program-rows`, and per
gesture for `pointer-click`, against section 9.

## 8. My own falsification pass on the design's state

- **Running-use state.** Writers: event programs via `set`; the floor for
  its facts. Readers: view programs. Clearer: the floor on unmount, which
  disposes the entity and every read of it. Two `set`s on one component in
  one run: last wins; runs are serialized by the queue. Error path: a
  program that throws keeps the `set`s already applied, submits no batch,
  and the floor writes the error to `[use :error]`.
- **Operations.** Writer: the batch at the end of an event run. Ordering:
  the queue means a second gesture starts after the first's batch is
  submitted; its reads see the store as of acceptance or, if the decision
  is still pending, the previous accepted revision, as the accepted build
  enforces; a stale expected revision rejects with a reason written to
  `[use :error]`. Two sessions keeping a variation at once both `put`
  `[root :tools]` with the revision they read; one rejects and its user
  retries, the accepted build's existing law. `:pending` is set by the floor at submit and cleared on
  the decision.
- **Dispatch.** Failure mode: a button click captured by the pointer tool's
  binding, selecting the button's entity instead of applying the edit. It
  cannot happen because a node with an address is dispatched to that address
  before any binding is consulted. A node without an address defers, which
  is how the instrument's own hit opens the tool.
- **Cancelled gesture.** Failure mode: a fast second click cancelling the
  first gesture's branch before its batch commits, the accepted build's
  current behavior. It cannot happen because the queue holds the second
  gesture until the first ends.
- **Mid-gesture definition change.** Failure mode: a callee accepted between
  the gesture and the resolution of a read. It cannot happen because the
  whole reach is snapshotted from the live derivation at the gesture.
- **Unmount before resolve.** Failure mode: a gesture's batch landing after
  its view is gone. It cannot happen because the branch cancels with the
  use and the batch is built only after the last step.
- **Views reaching writes.** Failure mode: a `:pure` view running a behavior
  later edited to `set`. It cannot happen because that edit is refused while
  anything references the behavior, and a `:pure` program may only `run`
  `:pure` targets.
- **Two entities from one run.** Failure mode: `keep-variation` needing two
  fresh ids and getting one. It cannot happen because `new-id` is a step,
  not a root.
- **Shape.** Every read is `[entity component]`; a missing component reads as
  absent, not a placeholder, which the accepted build's reactive table
  already enforces.
- **Done gate for step 3 of the walk.** One plausible failure: `program-rows`
  showing one row for two steps with the same `:out` in different phases.
  It cannot happen because the row id includes the phase.

## 9. What would kill this proposal

1. Something the floor can do that a program cannot say, after the union
   rule of section 4 is applied. Then the surface is not exposed and the
   proposal's generality claim is false. Codex's prediction table named this
   row first.
2. View program reruns above the echo bar, the standing 52 ms p95. Then view
   programs compile to Electric ahead of time as a step; if that step needs
   the whole program static, "views as records" is half true.
3. An event program's Electric branch costing more per gesture than the
   compiled body did, to the point that clicking feels slow, or the queue
   visibly backing up behind a pending read. Then event programs run on the
   pure host with one-shot reads, and only views stay in Electric.
4. An edit to a program mid-gesture breaking identity on the screen despite
   the reach snapshot. Then pinning comes forward.
5. The executor's recipe and continuation machinery breaking under the six
   extensions (`executor_test`, `brush_test`). Then the base is wrong and a
   separate evaluator is the second evaluator again.
6. Running-use entities costing more than atoms for hover and drafts. Then
   running state stays client-side with addresses, and uniformity is dented.
7. The live reach growing past what a session can keep subscribed once
   behaviors call behaviors a few levels deep. Then the reach is snapshotted
   by one server read at the gesture, and latency is the price.
8. Codex's test, invocation is not decision: if `select` and `open` only
   name primitives one to one, nothing moved. The walk answers it if
   `pointer-click` sequences two of them on a computed argument, recolour
   writes accepted material, and `program-rows` chooses its fields, all
   without a compiled case.

## 10. Boundaries of this round

Pinning references to a revision. A query primitive; indexes are components
on the root, maintained by the behaviors that create things. Continuous
gestures, drag and paint, which ride the executor's recipe path, not
per-event programs; the language reaches them through the same capability
tables, and per-point programs are a cost question, not a scope one.
Stage, Frame and Attention as records: staging of the first build, not a
limit of the system; time is a readable source, so the lens animation is
expressible as a view. Discoverability, the
"at a loss" finding, which is design-track work and not fixed by this
mechanism. Undo and history beyond decisions. Hostile programs and
multi-machine deployment, as the accepted build already excluded. The way
back for painted surfaces, pixel to stroke, a separate wall.

## 11. Checked, derived, assumed

Checked: everything in section 2; the executor's admission rules, program
shape and operator table; the accepted build's gesture snapshot, gesture
replacement, nested read lifetime, hit registry before scene pick, and
reactive absence; the stopped build's binding dispatch.
Derived: the layer's three arrows; the schedule split; the primitive and
operator tables; the dispatch order; the live reach and its snapshot; the
queue; the deletions list; the identity key.
Assumed: that the executor's language extends without breaking recipes; that
Electric hosts an interpreter keyed by `[phase :out :op]` at the size of the
library view; that a program of a few hundred steps stays legible as rows;
that the module can walk a workspace partition at acceptance cheaply. No
cost number exists for any of it.

## 12. Holes found in the drafts, and how each closed

First pass:

- A button click would have gone to the active tool's binding. Closed by
  dispatch order: node action first, then binding; `:tool` as the deferral.
- Two fields shared one `:draft` component. Closed: a `field` names the
  component it binds.
- A behavior edited after acceptance could add a write under a view. Closed
  by `:schedule` on behaviors and refusing the edit while referenced.
- Revisions had no home once `:definition` dissolved. Closed: `:revision`
  per entity, store-maintained.
- Hover as a per-event program would run an Electric branch per mouse move.
  Closed: hover is floor-written state, not a gesture.
- Close-view and activate needed the floor to know their meaning. Closed:
  `[session :shows]` and `[session :active-tool]` are state the floor reads,
  written by ordinary behaviors.
- `run` as textual inlining collided `:out` names. Closed: nested branch with
  its own bindings.
- The walk's fifth step needed the program as rows with hits. Closed: the
  `program-rows` view, listed in step 3.
- A callee could change between gesture and read resolution. Closed: the
  reach snapshot.
- Step identity by `[:out :op]` collided across phases. Closed: phase added.
- "One evaluator" hid two hosts. Stated: one language and admission, the
  Electric host for views and events, the pure host for recipes, the step
  loop twice.

Second pass:

- The walk needed string and list operators the expression table lacks:
  node ids, catalog updates, closing a view. Closed: `:str`, `:conj`,
  `:without`, `:count`, `:nth`.
- A fast second click would cancel an in-flight gesture before its batch,
  which the accepted build does today. Closed: the event queue.
- `select` and `open` wrote to the local view, but the editor opens in
  another area. Closed: they write to the session root, and `session` is an
  environment root.
- The reach snapshot would have cost a server round trip per gesture.
  Closed: the reach is a live derivation, snapshotted locally.
- One `new-id` per run could not create two entities. Closed: `new-id` is a
  step.
- The hover preview could not reuse the click's selector without also
  running its writes. Closed by the pure and event grain: `target-preview`
  runs `containing-subject`, the click composes it with `set`s.
- Expected revisions had no rule for writes from programs. Closed: a write
  carries the revision the run read, else none.
- The first view to show was a name in floor code. Closed: `[root :home]`.
- Reference detection for refusing a remove had no rule. Closed: any
  occurrence of the id as a value in the workspace's accepted components.
- Loop items had no identity or position. Closed: `:key` and `:index`.
- A computed action address could dodge admission. Stated: static addresses
  checked at acceptance, computed ones fail at run into `[use :error]`.

Third pass:

- A node with an action could never be pointed at to open its definition,
  so the editor's own buttons were outside the loop. Closed: `:meta-click`,
  the halo gesture, dispatched to the tool's binding past the node's action.
- Button behaviors were outside the live reach, so a button click would
  have needed a round trip. Closed: mounted node actions join the reach.
- A button node named no entity, so a `:meta-click` on it would have opened
  nothing. Closed: a button's entity is the behavior it runs.
- The walk never said how a row is changed. Stated: by text this round;
  rows are for reading and pointing.

Fourth pass, against the aim (Sid: "no current or future self-editing loop;
a system that can then be used to build further"):

- Cycles through `run` were refused, which made the language non-general.
  Closed: recursion allowed, a budget per gesture and per rerun as the guard.
- Primitives were added demo by demo. Closed: the floor's surface exposed
  once by the union rule.
- Rows were presented as a bound and eval as the road to generality.
  Closed: form and generality separated; eval is the zoom-100 road for the
  floor's own code.
- Programs writing programs was possible but unstated and untested. Closed:
  stated in section 6, tested in the walk's sixth step.
- The walk read as the target. Closed: section 0 states the aim; the walk is
  the first test of a property.
- The store never said it parses program text. Closed: `put` of `:program`
  as EDN text, parsed and admitted in the module.
- Input levels were claimed without a mechanism. Stated: per-step branches
  give them.
- Two sessions writing the catalog at once had no stated outcome. Stated:
  one rejects and retries.

## 13. Vocabulary, Sid's terms beside this proposal's

| Sid's term | Here |
|---|---|
| the floor, the minimal VM-like floor | renderers, input, store primitives, runner, reactive runtime |
| the land, above the waist, stored as data | records: things, behaviors, tools, views, running uses |
| the next layer on top of the renderers | views, tools, behaviors as records, plus the runner |
| the way back, function-prime, pixel to record | the hit registry and pick, delivered with the gesture |
| the grand orchestrator | every compiled case in the layer; replaced by lookup |
| three strata, zoom 100 | material = records; Softland code = primitives' implementations; host floor = runtime and engines |
| point at the thing you use to point, change it, keep using it | the pointer as a tool record; reach snapshot at the gesture; follow latest |
| a building block for the next thing built inside | a pure behavior record, callable by address from any program |
| softland in softland, strange loops | the editor as a view whose buttons run behavior records, and `program-rows` showing a program as things you can point at |
