# Imported Topology View - Principal Design Research

Status: founder/principal-designer research synthesis, 2026-06-08.

This is not a Claude Design prompt. It is the design thesis behind the view
work: what research emerged, what problem Softland is really exposing, and how
to judge whether a designer understands the product rather than merely styling
it.

## Origin Prompt

```text
no i want you to think for yourself what is the research that emerged go deep
dive ... i don't even know what good questions to ask .. i am a founder looking
for the absolute best designer how would you tell me that you are the one ....
i gave you the whole lineage of past work and what i visioning now the ball is
in your court .... do not think or reply from claude design pov use this whole
entrypoint to research now you use your thinking on top ....
```

## Hard Verdict

Softland is not primarily designing a graph, a dashboard, a canvas, an editor,
or a file explorer.

Softland is designing an accountable transformation environment.

The first view must let a person see what outside material became inside
Softland without losing origin, identity, uncertainty, or local context.

The central product anxiety is:

```text
Did this transformation preserve understanding,
or did it destroy context while making the result look organized?
```

The first view exists to answer that anxiety.

## The New Design Problem

Most products show one of two things:

```text
source
  the raw material as authored or captured

result
  the cleaned, indexed, summarized, rendered, or transformed product
```

Softland must show the relation between them.

In Softland, imported material moves through this path:

```text
raw outside material
  -> SourceArtifact
  -> ObjectContainer / DerivedUnit
  -> Revision
  -> SourceAnchor
  -> CompositionEdge / RelationEdge
  -> Projection / View
```

The design problem is not to make this pipeline visible as backend machinery.
The design problem is to make its epistemic consequence visible:

```text
what came in
what became native
where it came from
how it is grounded
what structure was preserved
what relation is accepted
what is only candidate
what is partial, stale, disconnected, or failed
```

That is why the product category is new.

## Product Name For The Experience

Engineering name:

```text
Imported Topology View
```

Experiential design name:

```text
Source-to-World View
```

Reason:

```text
The view shows how source material becomes Softland terrain.
```

This phrase is useful because it keeps both sides alive:

```text
source
  the raw preserved outside material

world
  the native Softland substrate the user can later inhabit
```

## The Irreducible Experience

The first great interaction is not a graph.

The first great interaction is:

```text
I select a native Softland object.
I see what it is.
I see where it came from.
I see the exact raw source span.
I see how Softland transformed it.
I see what accepted relations it has.
I see what is missing, uncertain, stale, or unresolved.
I can move to nearby objects without losing my place.
```

If the design cannot do this for one imported object, it cannot do Softland.

Everything else is secondary.

## The Research That Emerged

The lineage does not point to one reference product. It points to a set of
obligations.

### 1. Memex / Engelbart / Xanadu

Design obligation:

```text
trails, links, and origin paths must be first-class
```

Bush gives associative trails. Engelbart gives augmentation and view control.
Nelson gives bidirectional links, transclusion, and the demand that reuse keep
origin visible.

For Softland:

```text
SourceAnchor is not backend metadata.
CompositionEdge is not hidden plumbing.
The connection itself is terrain.
```

### 2. Kay / Smalltalk / MVC / Views Over One Model

Design obligation:

```text
identity must survive representation
```

The same object may appear as an outline row, local graph card, timeline event,
inspector entity, canvas placement, or agent context item.

For Softland:

```text
views are projections, not copies
```

If switching views makes an object feel duplicated, the design fails.

### 3. Shneiderman / Munzner / Heer / Visualization Research

Design obligation:

```text
orientation must precede detail and spectacle
```

The design must start from task and data abstraction, not visual novelty.
Overview, zoom/filter, and details-on-demand are not old slogans here. They are
the basic cognitive contract for entering complex terrain.

For Softland:

```text
the view must show scope before local inspection,
and local inspection before global topology
```

### 4. Source Maps / DevTools / Lineage Systems

Design obligation:

```text
transformation must be debuggable
```

Source maps let developers debug the authored code even when the runtime runs a
transformed artifact. Softland has the same trust problem with imported
knowledge.

For Softland:

```text
native object -> exact raw source span
raw source span -> native object
```

This should be exact, reversible, and cheap.

### 5. Obsidian Local Graph / DataHub / dbt Lineage

Design obligation:

```text
relationships must be scoped
```

Global graph as first view is a trap. It feels visionary and quickly becomes
unreadable.

For Softland:

```text
default to local neighborhoods, typed edges, depth limits, and counts
```

The graph is a mode of local orientation, not the world itself.

### 6. Dynamicland / Ink & Switch / WonderOS / Folk Computing

Design obligation:

```text
the system must feel inhabitable and later malleable
```

This lineage prevents the product from becoming a database admin panel. It
does not mean the first view should be free-form or magical.

For Softland:

```text
build the trustable place first,
then the place can become malleable
```

### 7. Nicky Case / Distill / Explorable Explanations

Design obligation:

```text
understanding should be inspectable, not merely presented
```

The view should not assert that structure exists. It should let the user inspect
how the structure is grounded.

For Softland:

```text
clicking should reveal grounds, not just navigate
```

## Five Laws For Softland View Design

### Law 1: Transformation Must Be Inspectable

If the system changes representation, the user must be able to inspect the
mapping.

In Softland:

```text
SourceArtifact -> ObjectContainer / DerivedUnit
```

is not an implementation detail. It is the heart of trust.

### Law 2: Identity Must Survive Projection

The same thing must remain obviously the same thing across outline, graph,
timeline, inspector, canvas, and future agent context.

Design consequences:

```text
stable selection
stable labels
stable breadcrumbs
stable status grammar
stable identity affordance
```

### Law 3: Compression Must Be Recoverable

The system may fold detail, but it must not sever it.

Fold:

```text
full raw source body
distant graph neighborhoods
long revision history
secondary metadata
candidate relation details
```

Never sever:

```text
source anchor presence
object identity
truth state
warning/partial state
path back to local context
```

### Law 4: Uncertainty Must Not Masquerade As Truth

Accepted, candidate, inferred, partial, stale, failed, disconnected, and
unanchored are different states.

They must be encoded redundantly:

```text
label
shape
stroke
opacity
placement
icon
interaction
```

Never color alone.

### Law 5: The World Is Entered Locally

People do not understand a world by seeing all of it.

They enter through:

```text
a source
an object
a relation
a source anchor
a timeline event
a warning
a disconnected island
a question
```

The first view should always have:

```text
local focus
nearby topology
visible scope
way back
```

## The Pane Model, Rewritten As Epistemic Jobs

Do not define the layout as:

```text
left = nav
center = content
right = details
```

That produces a generic product.

Define it as:

```text
left = Source World
  where did this material come from?

center = Native World
  what did it become in this scope?

right = Trust World
  why should I trust this object?
```

This is the three-pane shell, but with Softland meaning.

## The First Three Prototypes

### Prototype 1: Trust Inspector

Question:

```text
Can a user trust one imported object?
```

Screen:

```text
selected ObjectContainer
raw source excerpt
native representation
SourceAnchor highlight
accepted relations
candidate/warning states
```

Signature interaction:

```text
hover native field -> highlight raw span
hover raw span -> highlight native object/field
click anchor -> reveal object in local context
click relation -> inspect relation as first-class object
```

Reason:

```text
If this fails, the whole product fails.
```

### Prototype 2: Scoped Import Explorer

Question:

```text
Can a user understand one imported silo?
```

Screen:

```text
left: source artifacts
center: outline/list of native objects
right: Trust Inspector
```

Use examples:

```text
markdown file -> document + derived units + anchors
transcript file -> conversation + messages + tool calls/results + anchors
```

Reason:

```text
This proves source-level orientation before cross-source topology.
```

### Prototype 3: Local Topology View

Question:

```text
Can a user understand how one object connects across silos?
```

Screen:

```text
left: source/silo context
center: selected object neighborhood
right: selected object/relation inspector
bottom: accepted/candidate/disconnected lanes
```

Reason:

```text
Topology should come after trust and source orientation, not before.
```

## What The First View Should Not Be

Reject these early:

```text
global graph as homepage
AI summary page
generic file manager
analytics dashboard
pretty canvas of everything
database admin panel
metadata table with no source reveal
```

The right first view is:

```text
source inventory
+ object browser
+ trust inspector
+ local topology
+ provenance/history
```

## The Visual Grammar To Invent

Softland's design system should not start with color palette.

It should start with truth-state grammar.

Required visual states:

```text
raw source
native object
derived unit
graduated object
source anchor
accepted edge
candidate edge
partial parse
stale anchor
unanchored object
disconnected island
projection
local-world placement
```

The first design-system question is:

```text
How does truth appear?
```

Then:

```text
How does uncertainty appear?
How does provenance appear?
How does derivation appear?
How does locality appear?
How does compression appear?
```

Only after that should the designer pick the cosmetic system.

## What A Great Designer Would Understand

A weak designer says:

```text
I can make the graph beautiful.
```

A strong designer says:

```text
This is not a graph problem. It is a trust problem.
```

A great designer says:

```text
The product must show transformation from source to native object without
lying, and the first prototype should prove that mapping for one object before
we design the world map.
```

That is the bar.

## Founder Questions To Ask A Designer

Use these to evaluate whether someone understands the product.

```text
1. What is the difference between a graph that looks informative and a graph
   that helps orientation?

2. How would you visually distinguish accepted relations from candidate
   relations without relying only on color?

3. How would you let a user inspect the exact raw source behind a native object?

4. What should never be hidden in an epistemic interface?

5. What should stay folded by default?

6. How would the same object appear in outline, graph, timeline, and inspector
   without feeling duplicated?

7. How would you design a partial import failure so it increases trust instead
   of feeling broken?

8. When should a canvas be used, and when is a list better?

9. What is your favorite example of an interface that makes transformation
   inspectable?

10. What is the first prototype you would build for Softland?

11. How do you prevent a clean UI from overstating what the system knows?

12. How do you help another mind enter a project without privately rebuilding
   all the context?
```

The strongest answer to question 10 is probably:

```text
a scoped source-to-native inspector with local topology
```

not:

```text
a beautiful global map
```

## Evaluation Rubric

Score any design against these questions.

### Orientation

```text
Can the user tell where they are?
Can they tell what source/silo/scope they are inside?
Can they backtrack after following a relation?
```

### Provenance

```text
Can the user reveal exact raw source?
Can they see anchor status?
Can they inspect transformation, not just result?
```

### Identity

```text
Does the same object remain stable across views?
Does selection persist across outline/graph/timeline/inspector?
Does the design avoid copy-like duplication?
```

### Truth And Uncertainty

```text
Are accepted and candidate relations impossible to confuse?
Are partial/stale/error/disconnected states visible?
Does the UI avoid fake certainty?
```

### Compression

```text
Are details folded without being severed?
Can distant neighborhoods stay compressed while counts and status remain
visible?
```

### Locality

```text
Does the view default to a meaningful local neighborhood?
Does it avoid all-world graph spectacle?
Does it show enough adjacent context to move intelligently?
```

### Handoff

```text
Could another person enter this view and understand enough context without the
original author narrating it?
```

## Anti-Patterns

Reject a design if:

```text
the first screen is a global graph
the source is hidden in a modal
the inspector is just metadata fields
candidate links look like accepted links
there is no partial/error/import-warning state
objects in different views feel like separate objects
the design uses cards everywhere and loses density
the graph has nodes with no readable labels
the raw/native distinction is unclear
the design cannot show a transcript message and markdown block in the same
substrate
the visual style is more impressive than trustworthy
```

These are product failures, not preference disagreements.

## What To Borrow

Borrow shells, not souls:

```text
IDE explorer
  stable source inventory, breadcrumbs, outline

DevTools source maps
  exact authored/result mapping and reveal-in-source trust

Figma inspector
  selection-driven right pane, stable object focus

DataHub / dbt lineage
  local dependency neighborhoods, status, upstream/downstream framing

Obsidian local graph
  depth-limited neighborhoods, not global graph as truth

Linear
  dense rows, filters, keyboard speed, low chrome

Notion / Coda
  one data substrate, many views

Dynamicland / Ink & Switch
  inhabitable and malleable computing spirit

Nicky Case / Distill
  inspectable explanation and vivid clarity
```

## What To Invent

Invent the grammar for accountable transformation:

```text
raw -> native
derived -> graduated
anchored -> unanchored
accepted -> candidate
local -> global
folded -> recovered
source time -> Softland time
projection -> action request
```

This is the design territory Softland owns.

## Grounding Examples

### Softland Project

Imported material:

```text
markdown docs
code files
chat transcripts
commits/history
```

The view should show:

```text
which docs entered
which transcript messages became native
which code artifacts are connected
which decisions link chat -> doc -> commit
which source anchors prove the chain
which parts are disconnected
```

### Discourse Graph Project

Imported material:

```text
Roam graph
codebase
Linear issues
chat threads/transcripts
```

The view should show:

```text
Roam pages/blocks as native objects
Q/C/E/D/R/F or other DG relation types when present
Linear issues and source anchors
chat evidence and decisions
accepted relations vs candidate bridges
isolated silos that need later connection
```

## The Principal Design Position

The first great Softland view is not:

```text
wow, a map of everything
```

It is:

```text
I imported a messy body of work.
Softland shows me what it became.
Nothing important was severed.
Nothing uncertain pretends to be true.
I can enter locally.
I can trace back to source.
I can hand this view to another mind.
```

That is the design bar.

## Source Trail

Primary and useful references:

```text
Vannevar Bush, As We May Think
https://www.theatlantic.com/magazine/archive/1945/07/as-we-may-think/303881/

Douglas Engelbart, Augmenting Human Intellect
https://www.dougengelbart.org/content/view/138/

Project Xanadu
https://www.xanadu.com/

Alan Kay and Adele Goldberg, Personal Dynamic Media
https://tinlizzie.org/VPRIPapers/m1977001_dynamedia.pdf

Tamara Munzner, Visualization Analysis and Design
https://www.cs.ubc.ca/~tmm/vadbook/

W3C PROV Overview
https://www.w3.org/TR/prov-overview/

Chrome DevTools Source Maps
https://developer.chrome.com/docs/devtools/javascript/source-maps

Dynamicland Intro
https://dynamicland.org/2024/Intro/

Ink & Switch, Malleable Software
https://www.inkandswitch.com/essay/malleable-software/

Ink & Switch, Local-first Software
https://www.inkandswitch.com/essay/local-first/

Kittur Lab
https://www.kittur.org/

Nicky Case
https://ncase.me/

MetwareBio, t-SNE vs UMAP for omics visualization
https://www.metwarebio.com/tsne-vs-umap-omics-visualization/

Inselberg, Parallel Coordinates
https://data.scitevents.org/Documents/Previous_Invited_Speakers/2012/DATA2012_Inselberg.pdf
```

