# Inland — intended design

This is the construction model for the Inland experiment, authorized for
implementation on main on 2026-09-11. It describes the intended design of this
build; it is not the complete Softland vision or a claim that every requirement
has been implemented. The [run/use guide](README.md) and linked source maps
explain current behavior and recorded limits.

The [original model](../../../history/docs/build-softland-in-softland/MODEL.md)
and [build brief](../../../history/docs/build-softland-in-softland/BUILD.md)
preserve the proposal context. Section 13 distinguishes requirements, choices,
and open questions. Permanent host choice, language sufficiency, and the full
source/build/recovery loop remain unproved by the recorded demonstrations.

## 1. The aim, the test, the property

**Aim.** Softland is the place where Softland is built. Everything above a
minimal floor, the views, the tools, the editor, the inspector, the arrangement
of the scene, is made inside, stored as data, by people and agents together.
Each thing made is a building block for the next.

**Test.** Point at the thing you are using to point. Change it. Keep using it.

**Property.** Something built inside Softland becomes a building block for the
next thing built inside Softland.

The aim and intended encounter motivate these requirements. A successful
pointer demonstration alone does not establish the whole construction model.

| The intended encounter says | The model requires |
|---|---|
| the thing you are using to point | the pointer is a thing with facts, not something implicit in the client |
| point at | a pixel reaches the thing under it and the definition that painted it |
| change it | the change is a data edit, and things resolve their definition by name at each use |
| keep using it | the thing's identity survives a change to its definition; no restart |
| the same material as what they act on | the inspector and the editor are made of the same material; nothing above the floor is code |

The property is one line: definitions name definitions.

## 2. Three strata

Sid's July 30 frame, taken as is.

| Stratum | What is in it | Changed how |
|---|---|---|
| Host floor | browser, GPU, Rama, and the Electric/Missionary dependencies used by this build | versioned dependencies; upgrades are implementation work, not ordinary live fact edits |
| Softland code | the floor of section 4: five operations, the painters, the capabilities the body vocabulary names | addressable at zoom 100; edited and rebuilt; kept small, with closure tested rather than assumed |
| Material | facts and definitions in Rama: every view, rule, tool, space, schema, and the seed | edited in place, live |

There is one level that is not editable live: what a fact is, what a definition
is, and the five operations. Everything made of them changes in place. They are
stable within a runtime version and change only through a rebuild with a
migration, the way a Smalltalk image's object format does. That is Softland
code, not host floor: it is addressable at zoom 100 and worked on from inside,
with the rebuild as the step.

The host choice for this experiment does not settle which dependencies Softland
should maintain permanently. Record the public Electric interfaces used and any
internals that had to be inspected or changed. Dependency size and maintenance
cost require evidence. The first build's in-world editing scope is material;
the complete source/build/recovery loop remains a later demonstration.

## 3. The material

### 3.1 Facts

A fact is: thing, attribute, value, asserted by whom, when, in which layer.

- A thing is an id, stable for the thing's life. Definitions, spaces, layers,
  events, people and agents are things.
- Attributes are open. Any attribute on any thing. Nothing has to be declared
  before it is used. Kinds are attributes like any other, added late, several
  allowed at once.
- Values are scalars, text, paths, references to things, or expressions.

The floor has no is-a. A definition applies to a thing when the thing's facts
match its pattern, and a thing can match many definitions at once. Where the
log reaches for "type" and "instance", the words are definition and thing, and
the relation is applies-to. Classifications, hierarchies and type relations
are welcome as material: relation facts and definitions over them, built by
whoever needs them, several at once. None of them is built into the floor.

Attributes carry their own facts. The one that matters most: whether an
attribute holds one value per thing, replaced at the address when written, or
many, accumulated. Selection in a session is one-valued; tags are
many-valued. The default is one-valued. Changing cardinality for existing
material is itself an admitted transition, with existing values accounted for.

### 3.2 Definitions

A definition is a thing with three attributes: a name, a pattern, a body.

- **Pattern.** What facts a thing must carry, and for a rule what event must
  have happened, for this definition to apply. Variables in the pattern bind to
  values. A pattern may mention the context: which layer, which zoom band, who.
- **Body.** The recipe of the 2026-09-06 law, exactly: named steps over a
  vocabulary of capabilities, formulas in the leaves, total, with a work
  budget per read, run by one executor with several runners. Nothing in a body
  is a program. Foreign code such as a shader enters only as a value in a slot
  with a declared output. The September 6 exit remains unchanged: if a record
  provably cannot say a needed tool twice over, a designed total leaf language
  is asked of Sid then. No unrestricted host eval is introduced. What this model
  adds to that law is only where recipes sit and what they may yield: a step
  may name another definition, and a rule's result is a change. That law
  fixes the body's shape; it does not choose how applicability is decided.
  Applicability here is the pattern.

A body yields a value, and another body may name it, supply explicit bindings,
and consume that value. `parent(subject)` can return an address without painting
anything or manufacturing an event. Views and rules are the two consumers of
body results in ordinary dispatch, not the only possible returned values.
Named invocation and applicability remain separate responsibilities.

One shape, two consumers:

| Use | Applies to | Body yields |
|---|---|---|
| view | a thing | a description for one of the three painters: text, 2D path, 3D |
| rule | an event on a thing | changes: facts to assert or retract, including new definitions; events to raise |

This does not require a third built-in kind for callable values. A tool is a bundle of
definitions. A space is a thing with views and rules. A schema is a set of rules
about attributes, run at acceptance. A question is a definition with a pattern
and no body: it announces when something matches it.

Composition is by name. A body names another definition; the floor resolves the
name at the moment of use, in the current context. Nothing is ever copied from
a definition into a thing. That is what makes "keep using it" true: change the
definition and every thing that resolves to it follows, identity intact.

Match and naming are not rivals. Match answers which definitions apply to a
situation. Naming inside a body answers how a definition uses another's
result. The floor's entry is match, never a call to a tool it knows; inside
the body, names. Whether a call-shaped entry causes the floor to grow is an
observation from three builds and from July, where each new tool, mount or
effect was a shell change; it is not a proof.

### 3.3 Events

Events can be addressed as facts: a meaningful gesture hitting thing T through
view V with bindings B and context C, an edit request, a timer firing, or a
message. An admitted request that no rule answers remains visible and pointable
as unanswered work. This does not persist every raw input. Pointer samples and
other local signals follow section 5; retention is deliberate. A saved question
exposes its matches, and an authored view or rule can announce them.

### 3.4 Layers and context

Facts live in layers. A layer is a thing. A context is a small set of facts:
who, which layers are active and in what order, which candidate is chosen where
one exists, the zoom band, the session.

Resolution and matching read through the context. The nearest active layer
wins. A thing's own fact shadows the default its definition would give. So a
personal layer over the shared base is how one person's adaptation coexists
with shared definitions. A candidate layer is how an unfinished component is
real and usable without being everyone's. A fork is a new layer on a base.
Promotion is an admitted change to the binding or accepted head a base context
follows. Contexts with independent overrides or pinned revisions remain distinct.
The builder states how references are pinned or live, how an invocation keeps
its resolution basis, and how removing a value differs from deleting an override.

### 3.5 What a definition establishes, and what happens when things change

When a definition applies it establishes one of three things. The floor knows
which, because the definition says which.

| It establishes | Lives while | Withdrawn or ended when | Examples |
|---|---|---|---|
| a maintained conclusion | its support holds and an owner demands it: the matched facts, the definition itself, the context, the reads it made | support changes cause recomputation/withdrawal; loss of the last demand releases its work | every view; a highlight while selected; a derived count |
| an asserted change | independently, until replaced or retracted through acceptance | someone or some rule retracts or replaces it | a stroke from a gesture; an edit to a definition; a note |
| an activity | its owner keeps it alive and its budget lasts | its owner ends it, a cancel event arrives, or the budget runs out | an agent task; an experiment; a long computation; an `ask` |

Maintained conclusions are derived, never written to the depot as
assertions. They may be materialized for speed, but their truth is the
derivation. Because the definition is part of the support, editing a
definition recomputes every conclusion it maintains: late binding is exactly
this. Asserted changes persist through such an edit; they are history.

Asserting rules match the event stream, on arrival, once. Editing a rule later
does not replay old gestures. A definition that wants to reinterpret history
is a maintained one over the event table, and its conclusions are derived,
not asserted. So the same store answers both "what did that click do" and
"what would that click mean now" without confusing them.

A read distinguishes a value, known absence and pending; failure or forbidden
access must not become absence. A body awaiting a required read is pending,
not false. An absence test needs a completed read or a query scope whose
completeness is known. The existing executor's read barrier is a starting
mechanism, not proof of the complete query/read contract for this model.

Conflicts are ordinary. Two maintained conclusions for the same one-valued
attribute, from two definitions, coexist as derived facts each carrying its
support, and a consumer's pattern picks between them, by layer order or by
whatever the consumer states. A conclusion supported by several derivations
survives withdrawal of only one support. Competing assertions to a one-valued
target follow that target's declared admission/choice policy, or produce an
explicit conflict. Overlay order is not a universal write policy; a target may
choose it explicitly. The floor never silently merges meanings.

Positive recursive conclusions over a finite declared set of possible facts,
with no step that manufactures new values, converge for that input basis:
deriving a fact already present changes nothing. Negation is stratified over
completed lower dependencies, not allowed through self-negating cycles. On a
source change the affected support and conclusions are maintained or withdrawn.
This is the fixpoint guarantee. A rule that asserts on its own assertions, or
work that keeps generating values, has no such guarantee and runs as an owned,
budgeted, visible, cancellable activity.
A cycle in a parent relation has a finite closure and no outermost element;
which of absent, a cycle fact, or a declared fallback the author means is the
author's to say in the definition. The floor guarantees that the interface
keeps running and that runaway work is visible; it never guarantees that an
activity finishes.

An activity's algorithm is an authored total recipe run in repeated steps by
its execution owner. Each step takes the activity's state and declared inputs,
then yields a next state, a result, a wait or an explicit effect request. Each
step is total and budgeted; the repeated scheduling belongs to the activity,
not unbounded recursion inside a body. The owner retains step state and its
input/definition basis. Checkpointing or saving observations is explicit; every
iteration need not be a durable assertion. Cancellation and yielding must reach
the work itself, and child work must not evade the parent's budget.

The shared executor has continuation and state-transition mechanisms to examine
for reuse. Their existence does not establish this authored activity model.
The nontrivial-algorithm case in the build brief tests that construction under
the existing law, before claiming a language extension is necessary.

Ownership is separate from persistence. A stored definition can sit unexecuted
forever. A view's work ends with the subscription that shows it. A maintained
conclusion in Rama lives with the topology. An activity lives with its owner
thing, a person, an agent or a space, and stops when the owner stops it or is
gone. Storing a request for work is not doing the work.

## 4. The floor

The floor does five things and paints. It knows no nouns: not tool, not
selection, not inspector, not button, not space. It knows fact, definition,
event, layer, context, and the body vocabulary.

| Operation | What it does | Where it runs | Existing piece |
|---|---|---|---|
| store | admits changes into layers under their policy | Rama for durable material; the session owner for local session facts | depots, PStates, revisions |
| resolve | name in context to definition | where its authoritative inputs are available, with explicit server/client boundaries | contextual layers are prior evidence; the new integration remains work |
| match | which definitions apply to the demanded thing/event/context; incremental and indexed | scoped by data locality, authority and demand | PState indexes and keyed diffs are ingredients, not a completed matcher |
| apply | evaluate a total body with tracked reads: maintain a result with support/demand, or run a step under an execution owner | explicit placement by capabilities, authority, dependencies and lifetime | the record executor; Electric branches; Missionary flows and tasks |
| locate | pixel to thing, view, bindings, context; provenance travels with the paint | client | the hit path, why-this-pixel |
| paint | descriptions to pixels | client | the three renderers |

The earlier rules-on-server/views-on-client split is a default, not a kind law.
A local session edit can run on the client; a view may depend on server work.
Rama remains the authority for durable acceptance. Long or external work does
not block that admission path.

The client's reactive program maintains demanded view instances and their
matching bindings, then hands descriptions to the painters. Occurrence identity
distinguishes two presentations of the same subject. Features do not live in
this host program. Local pointer and camera signals feed the appropriate views
directly; only deliberate retained changes go to Rama.

This build uses the actual Electric compiler/runtime. The compiled Electric
interpreter/planner hosts runtime-authored recipes and patterns: their accepted
changes create or remove real dependent branches and addressed source reads.
Electric carries server/client computation, changes and lifetimes; Missionary
supplies tasks and flows at appropriate source/resource boundaries. Rama supplies
durable acceptance, indexes and narrow source changes. Softland's three renderers
realize the affected presentation, including text editing; browser DOM is only
the canvas and necessary input/accessibility bridge.

The matcher, support maintenance, authored composition and placement policy are
Softland work to build on that stack. Compilation of the interpreter does not
freeze its live graph, and using Electric does not automatically expose hidden
reads inside an opaque evaluation. Both the interpreter and compiled capabilities
must participate in the tracked-read and ownership contract. No whole-world
mirror, optimistic shared update, application result cache or custom transport
substitution is used to make the demonstration appear reactive.

The floor has no application-specific tool dispatch and uses no model to infer
what a gesture means. It matches and evaluates declared recipes deterministically.
Whether a question can be asked is
whether a name resolves. Whether someone will answer is whether a rule matches.
When neither, the gap is a fact.

## 5. Three tiers of state

| Tier | Examples | Where | Becomes durable when |
|---|---|---|---|
| local signals | pointer position, camera, in-flight keystrokes | client, reactive | a rule asserts a fact from them |
| session facts | selection, zoom, pins, open places, a candidate choice | a session layer | retained by a rule or by the person |
| accepted facts | content, definitions, layers, events that mattered | Rama | at store |

The pointer test crosses all three: position is local, selection is session,
the edit to the definition is accepted. What becomes durable is what a rule
deliberately asserts.

## 6. The test, traced

Seven schematic definitions, shown as text rather than implemented syntax. In
the store they are records, shown as forms, handles or sentences and edited by
pointing. The active bundle is a session fact read by applicability; the
selector calls a named targeting definition, initially identity.

```
pointer        a thing with :at
pointer-view   when {at ?p}                          -> path: arrow at ?p                     (maintained)
select-rule    when {down on ?e in ?session, active bundle uses this rule} -> assert ?session :selection targeting(?e) (asserted, session)
halo-view      when {?session :selection ?e}        -> path: ring around ?e                   (maintained)
halo-rule      when {inspect definition ?d in ?session} -> assert ?session :inspecting ?d       (asserted, session)
inspect-view   when {?session :inspecting ?d}       -> text: name, applying definitions, body  (maintained)
edit-rule      when {submit draft row ?a of ?e as ?v} -> request accepted edit of ?e ?a ?v      (asserted, candidate or base)
```

Selection is one-valued per session, so a new selection replaces the old at
its address and the ring follows because it is maintained by the selection.
The stroke a drawing gesture would make is asserted and stays.

1. Point at the arrow. Locate gives the pointer thing, pointer-view, and the
   binding for its position.
2. Halo. The halo rule asserts that pointer-view is being inspected. The
   inspect view shows pointer-view's body.
3. Edit the body: a ring instead of an arrow. Submit the draft to my candidate
   layer. Pending or rejected submission leaves the prior accepted body active.
4. After acceptance, use that candidate context. Resolution yields the ring;
   the next affected presentation update draws it. Same pointer thing.
5. Point at the ring. Halo again, this time on the select rule. Change its body
   to select its containing object, or the thing itself when there is none.
   After acceptance either part selects the group; the tool remains selectable
   through the ordinary fallback and can be edited again.
6. Keep a second usable variation with its own address and explicit shared or
   independent definition references. Promote a candidate through acceptance;
   contexts following that base update, while pinned/overridden contexts do not.
   A fresh session recovers both saved tools from Rama.

The intended result requires no client branch for the new tool's name. These
definitions describe the inspector/editor behavior; the build must supply real
editing affordances, bindings and primitive input/render execution. Listing seven
definitions is not evidence that the experience exists. The complete first-build
walkthrough and its checks are in BUILD.md.

## 7. Agents

Agents are participants using the same admission facilities and explicit
contexts as people. The starting policy is material and editable: ordinary
content, including a requested reply, follows normal admission with visible
machine provenance; changes to shared behavior go through candidates. This is
not a universal permission restriction based on agent origin or a built-in kind.
Deterministic navigation never passes through a model.

The first build keeps a real `ask`: one provider, one bounded request and one
accepted reply. An accepted activity intent identifies its owner, input basis,
budget and status. An execution owner performs the call and submits the result
through Rama acceptance. An unknown outcome is shown as unconfirmed; no blind
retry follows a timeout or topology retry. Cancelling or closing an observing
view does not falsely claim that an external call was undone. Provider failures
and late results must have explicit outcomes. No canned reply substitutes for
the real product path; controlled failures may be injected by tests.

## 8. Acceptance, permission, provenance

Acceptance is the floor's one mechanism for durability. Endorsement is a fact
about who. Deployment is which context resolves to what. Permission facts say
who may assert which attributes on which things in which layers; acceptance
rules enforce them at store and read through an authenticated authority source;
writing another actor's name into context does not grant that actor's authority.
Every change is an event fact with an author and an address, so a remark
attaches to it by pointing, with no documentation ritual. The record says what
happened; the remark says why, which objection it answered, what was tried
and dropped. The why is human and agent work, and the model only makes it
attachable without ceremony. A diff of anything is a diff of facts, which
makes every diff uniform and possible; what a diff means, for text, for
geometry, for an interpretation, for two candidates, is a view over it and
  needs correspondence rules of its own. History is the depot, for what was
retained.

## 9. The seed

A file of definitions loaded at genesis. It is the July 18 walkthrough plus the
halo, and nothing more.

| Definition | What it gives |
|---|---|
| block-view | a thing with text shown at its position, boundary on attention |
| write-rule | a click on nothing makes a thing at that point; typing sets its text |
| send-rule | ctrl-enter raises `ask`; the resident answers with a block in the machine stratum |
| space-view | pan and zoom over a space thing; zoom bands raise events |
| halo-rule, inspect-view, edit-rule | as in section 6 |
| definition-view | a definition in human words: what it is called, what it applies to, what it does, what it is made of, what people have experienced around it |
| unanswered-view | events nobody answered, as things to point at |

The second inspector is made inside using the first. The workshop is a space
thing made inside. Nothing in the seed is code.

## 10. The log's cases, one line each

- Six views of one live conversation: six view definitions on the same things.
- Evolving the UI with the user, never a new one: edits to definitions;
  identity persists; no regeneration.
- Unfinished components real and usable: a candidate layer resolvable in my
  context.
- Show me everything people have experienced around this type: facts attached
  to the definition thing; the definition view shows them.
- The house inspector, 2D and 3D, both directions: two views on shared facts;
  edits through either are facts.
- The three synced layers, and only that part redraws: reasoning facts, a
  working layer, a 3D view; the link from a claim to geometry is a rule someone
  writes or a change an agent proposes; keyed diffs redraw only what changed.
- What a click means, without a hundred cases: rules keyed on the subject's
  facts; the halo lists which apply here.
- The workshop: a space thing with its own views and rules, reached by a rule.
- The code editor at zoom 100: bodies are values; the floor's own code is
  Softland code, addressable, rebuilt.
- Questions as unfilled patterns: a definition with a pattern and no body.
- Diff of a pipeline: a diff of facts.
- Building on top and selling it: layers plus permission facts.

## 11. What the model does not do

- It does not make Softland understandable. It makes understandability
  makeable: a definition's own view is a definition, and the halo lands on that
  view, not on the floor's record. The seed's words are design work.
- It does not fix the body vocabulary. That is learned against real
  definitions. Express behavior as a definition where the vocabulary permits it;
  classify every native addition as described below.
- It does not solve performance. It names the obligation: match is incremental
  and indexed, or the model fails at scale.
- It does not prove the floor is closed. Closure is the hypothesis that new
  application behavior composes without new application-specific dispatch or
  changes to the five operations' semantics. A construction requiring such a
  change is counterevidence, not a feature to hide in the host. The first build
  records its initial floor and all later additions while looking for this.

Every added capability is classified with evidence:

| Addition | Evidence |
|---|---|
| Optimization of expressible material | The authored expression it replaces and the relevant equivalence checks, including reads/effects |
| Required primitive | The attempted authored construction, the precise limitation, and why the existing vocabulary cannot supply it; a failed attempt alone is not a general impossibility proof |
| Application behavior moved native for convenience | An explicit admission of that choice and its effect on the closure claim |

A shared name does not make these equivalent. Compiled runners are allowed;
capability-per-tool growth must remain visible. A vocabulary gap is not by itself
proof that the body language needs extending. The September 6 exit still applies.

## 12. What breaks it

- Tool-specific dispatch or behavior placed in the compiled host to bypass the authored construction being tested.
- A capability per effect: select, highlight, open. Effects are facts; views
  react to them.
- An active-tool slot in the floor.
- Any noun in the floor.
- A general program language in bodies.
- Resolution without context.
- Copying a definition into a thing.
- A shell.

## 13. Derived, chosen, open

**Required by the aim.** Addressable material and construction. Pixel to subject
and definition. Identity through edits. Reusable authored composition. One
successful pointer demonstration does not establish these for every construction.

**Chosen.** Facts as the atom. Pattern and body as the definition shape. Match
for applicability, names for composition, activities for work. Bounded
bodies and owned repeated steps. Layers for context. Three tiers of state. Three
things a definition can establish. One-valued attributes by default. Agents
as participants with an editable starting policy. Positive recursive derivation
and stratified negation. Electric is the authorized host for this build, with one
real `ask`; permanent host adoption remains open.

**Open.**

- The members of the body vocabulary, under the September 6 law.
- What a session layer retains by default.
- The target admission/choice policies for competing assertions.
- The cost model of match at tens of agents per person and hundreds of people.
- Where activities run by default, and how an owner is chosen when a rule
  starts one.
- The migration path when the definition shape itself changes across a
  rebuild.
- Whether this implementation's dependencies and maintenance obligations justify
  permanent Electric adoption; a parallel Missionary build is not in this brief.

## 14. What the first build must establish

The capability log is read alongside four kinds of evidence: the complete
self-editing experience; durable accepted behavior through pending/rejection and
fresh-session recovery; narrow propagation of relevant changes; and disposal of
view-owned work while another user continues. The authored algorithm tests the
total-recipe/activity construction. The real `ask` tests external activity and
uncertain outcomes. BUILD.md contains five focused scenarios and the runnable
handoff, rather than making every open question a prerequisite for beginning.

Reference pinning/removal, admission requests, demand/read outcomes and the
minimum external-activity lifecycle are builder decisions within this scope.
General renderer/geometry correspondence, broad runner optimization and the full
in-world implementation rebuild/recovery loop remain later work. Any mechanism
actually used by the first build needs its minimum correctness contract then.
The build reports source facts, executed receipts and Sid's lived assessment
separately. Sid's go-ahead authorizes the experiment; later adoption is his word.
