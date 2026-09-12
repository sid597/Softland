All four documents are loaded, and nothing else. No code, no other docs. On your instruction I take the four as accurate and complete accounts and reason only on top of them.

**Short answer.** In all three builds the same three things moved into records: the values a tool computes over, the tool's own parameters and program where it had one, and where-you-stand. And in all three the same four things stayed in code: how the window is composed, how input becomes intent, the rules that give a record's fields their meaning on screen, and the kinds themselves. Below those, capabilities, process owners and the store stayed code by design, which the baseline's own split expects. Read together, the honest reason the four stayed is scope, not impossibility. Each build moved exactly what its demonstration needed to edit and no more, and the Workspace handoff says outright that the omissions were not shown to be impossible on the substrate. The four that stayed are, in one sentence, the old client's block anatomy, the thing whose living in code you named as the wound. Vantage moved the most of it, and still keeps the routing and the dialog in code.

## Each build: what moved, what stayed, why

Everything in these tables is what the writeups say. The one derived line is marked.

**Place and Inside.** The ECS-layer branch. Records live in an in-memory world, seeded from Clojure source; the wire was never built, so nothing survives a reload.

| Concern | In records | In code | Stated reason |
|---|---|---|---|
| Tools | Brush recipe with roots and program, radius, the flow width expression | Executor, the capability vocabulary, client rules that assemble some tool inputs | No general recipe editor; the vocabulary is the waist |
| What a tool reads | The record and path each tool reads, compared whole to decide reruns | The loop that compares and dispatches, with specific dispatch for brush, inspector, lanes and Inside | "not a fully generic component-query scheduler" |
| Material | Host, painting, mark sources, stroke points, input events, pointer identity, traces | Event delivery, painting dispatch, reverse hit joins, record replacement | Not stated beyond ownership |
| Where you stand | Camera, placements, navigation ancestry, spread and focus, breadcrumb | Navigation, gestures, layout interpretation, projection functions | No vantage or layout editor; a bounded pass |
| Text | Text tool program, source rows, layout options, edit buffer, caret, selection | Identity-to-lines, trace-to-lines, field selection, numeric edit semantics | "would need actual vocabulary and execution support" |
| Inside | Twelve nodes with titles, descriptions, source addresses, positions, layers, links | Catalog-to-mesh and path projection, part construction, route selection, inspector policy | "client projection rules, not a general record-authored UI grammar" |
| Rendering, lifetime | Scene, Path and layout values as explicit inputs | GPU, compositor, fonts, listeners, clock, cleanup | Process resources, not record values |
| Durability | Nothing. Runtime edits do not rewrite the seed source | | The wire awaits your word |

**Workspace and Anatomy.** Durable through the existing object-container by a typed-entity adapter that carries kind and bypasses the legacy edit path. The brush replays preset dabs; there is no live stroke.

| Concern | In records | In code | Stated reason |
|---|---|---|---|
| Envelope | Id, kind, revision, components, provenance; references carry id and revision | Validation and creation rules; the kinds themselves | Attributable revisions; not the multi-asserter model |
| Subjects, results | Brush inputs, run observations, painting, source passage, notes | The brush adapter and the interpretation of those fields | Reuse an existing executable subject |
| Relations | Named relation maps with exact target references | The interpreter and which relations are exposed | Enough to traverse; not a general assertion kernel |
| The one tool | Relation to follow, list or columns, definition fields | The meaning of those choices, the control template, the drawing | "a saved behavioral variation before claiming a general tool-authoring language" |
| Representation | Subject reference, tool reference, view, a placement name | Pixel coordinates, panel sizes, template composition, layout | "A placement name does not yet encode the whole layout" |
| Working context | Baseline and candidate references, selection, note, kept references, saved view | Commands, navigation dispatch, local interaction state | One investigation; not base plus overlay |
| Pending reads | Request and result rows keyed by capability plus full arguments | Capability implementation, work allowance, continuation custody | Never store a continuation as a durable job |
| Text | Committed note and field revisions | Hidden textarea, IME, caret and selection routing, commit rules | Browser input, Softland drawing; keystrokes are not revisions |
| Anatomy | The real values it shows | Chapters, explanation text, colours, plates, reveal rules, its camera | An authored explanation, not a discovered architecture |
| Systems | | Explicit functions and commands; no scheduler, no dependency graph, no event-as-record | "compiled templates ... the largest remaining tool-authoring boundary" |

**Vantage.** Durable, with authoring layers, promotion and a saved agent turn. Milestone six is uncommitted and its checkpoint still says not started; the account describes code that is not in history.

| Concern | In records | In code | Stated reason |
|---|---|---|---|
| Where you stand | Subject, query, pins, zoom, selected layers, asserter, parent chain | Page projection; the fixed conversation, map and roots assembly | Window composition from records not implemented |
| Tool behaviour | Text, Map and Sphere records with roots, fields and programs; the prompt tool's inputs and program per turn; font, wrap and reading rules; map columns, spacing, colours | Capability tables; grouping interpretation; tool discovery, creation and mounting as a fixed catalog | New operations need code; "without pretending that font shaping itself became authored data" |
| Material, possibility | Source units, revisions, layer candidates, authorship, reply relations | Store topology: accepted writes, identity, indexing, promotion | Shared durability laws |
| Computing a view | Explicit inputs, program expressions, results, prior line state | Shaping, Path construction, coating sampling, program interpretation | Retained browser results are temporary derived state |
| Effects | Turn intent, model, effort, rendered prompt; the records effects produce | HTTP, SSE, the CLI process, the courier queue, fonts, device, GPU | Need owners and lifetimes; "not serialized as tool content" |
| Interaction | Selected source and tool-field addresses; the pending read and its snapshot | Keyboard bindings, DOM controls, gestures, the Ctrl+Enter routing and dialog, the native textarea | Wired in the browser shell |
| Rules | Reply placement and reply kind, each with a code consumer; paste-share present with no consumer | Kinds as a code-defined vocabulary | "source boundaries, not features implied by the presence of a field" |
| Code as subject | Catalog of files and form units, requires, changed units per commit, producing-turn links | The import and analyzer passes, recomputed at boot | Reuse; no second parser |

Derived, across the three tables: the builds sit on a line. Workspace has the least tool-as-program, one record with two choice fields and a fixed interpreter, and the strongest envelope. Place has the loop, declared inputs and events as records, and no durability. Vantage has the most tool-as-program, four tools that are programs and a roots panel that draws itself with the text tool, plus layers and the agent. Each is strongest where the other two are silent.

## Across all three: what wouldn't move, and what it would take

Two piles.

**Pile one stayed by design.** Capability implementations, process owners and the store. All three say it in one voice, and the baseline's split predicts it. Not a finding; the hypothesis expects a waist.

**Pile two stayed by scope.** Four things all three built in code and none moved:

1. **Composition.** Which tools are on screen and where. Place's projection functions, Workspace's compiled templates, Vantage's fixed assembly. All three store a record that names the arrangement, and code decides what the names mean.
2. **Interaction.** Input to intent. Gestures, bindings, editor semantics. Events are records in Place, the pending pointer read is a record in Vantage, and the binding from event to effect is code everywhere.
3. **Projection.** The rules that read a record's fields and decide what they mean on screen when that reading is not a tool program. Identity-to-lines, trace-to-lines, catalog-to-mesh, grouping, relation-to-drawing, arrangement. Nearly all of it in Workspace, most in Place, least in Vantage.
4. **Kinds.** Code in Vantage and Workspace; unstated in Place.

Also derived: all three dug under the waist where a limit bit. Place changed the executor's continuation handling and added skin, raster bounds and surface compare. Workspace added a surface-to-mesh adapter and a store adapter that bypasses the legacy edit path. Vantage extended the compositor to retain hits, put layer metadata into the object-container, fixed record identity that was addressing by suffix, and fixed a renderer pass. Those cluster at three boundaries: continuation, identity, and hit or pick. So the minimal-layer hypothesis held for tools, and every build still had to reach down at those three seams.

**The pattern that moves things.** All three proved one shape: a record with roots and a program, steps that name capabilities, a declaration of which records the tool reads, the executor rerunning it when those inputs change, output landing in a declared record. The brush had it at baseline. Vantage gave it to text, map, sphere and prompt. Place gave it to a width rule. Every item in pile two is that pattern not yet pointed at that domain. What it takes, per item:

- **Composition** becomes a tool. Roots are the mounted tool instances and their placements, which all three already store. The program is placement steps. The output is the list of tool-instance-to-region pairs the presenter draws. Presenters in all three already take explicit values, so the consumer half exists. Missing: a placement vocabulary, and a presenter that reads the output instead of a fixed assembly. One cost signal from the docs, below. My position: composition records change locally and save by explicit intent, the way Workspace treated Anatomy's views and Vantage derives locally then sends through the courier. Never a store round trip on the frame path.
- **Interaction** becomes events plus binding rules. Events as records, which Place has. Binding rules as records, for which the width rule shows the expression shape and Vantage's reply-placement rule shows a rule with a code consumer. Then one deliverer that evaluates every binding against every event, replacing per-tool dispatch. The editor is the hard case. My position: text input, meaning IME, caret and selection, is a capability like shaping. Two builds already use the native textarea that way. The editor's routing, what commits, to which record, in which layer, is data, which Vantage's courier already does. Exit: if pointer and caret disagree again under that split, the caret must be a source position in a record, as Place did for numbers, and the capability boundary moves down.
- **Projection** rules become tool programs, each declaring the records it reads. Missing: data-shaping steps. Group, sort, map over a collection, format text. The baseline says recipes must not gain arbitrary Clojure by being records, so there is a line to hold. My position: grow by named capabilities, which is what the builds already did, since grouping is a capability the map program calls rather than something the program expresses. Exit: when named steps can't express a projection without exploding, the grammar itself needs conditionals and iteration, and that is a substrate decision for you. Unmeasured risk: projections as interpreted programs put the executor on every frame. Place and Vantage measured tool runs on the frame path; nobody measured projection interpretation.
- **Kinds** become kind records with field declarations and validation the store reads at write time. That is a server change. Vantage extended the object-container; Place never touched the server.
- **The loop** then collapses. Once projections are programs, per-kind dispatch reduces to Place's rule: run whatever's declared inputs changed. Four owners remain, clock, deliverer, presenter, wire, and nothing tool-specific.
- **Step zero is one record shape.** Three builds, three client record shapes, two of them over the same durable owner. The moved code needs three properties: exact references with id and revision, declared inputs by record and path, and layers with an asserter. Workspace has the first, Place the second, Vantage the third. Which base to extend is yours.

The cost signal, from the Workspace handoff:

| Move | Cost | How it was built |
|---|---|---|
| Change view tab | 1 to 2 seconds | A saved workspace command awaiting server acceptance and readback |

The handoff calls that a coupling to revisit, not a floor. Vantage makes the same kind of move durable and reports no latency for it. Place keeps it in memory.

`★ Insight ─────────────────────────────────────`
- A field in a record is not a moved behaviour. All three writeups warn about this separately: a placement name with code layout, a paste-share rule with no consumer, a kind carried through an edit while kinds stay code. The test for "moved" is whether a program or rule consumes the field and whether editing it changes what happens.
- The line between pile one and pile two is not the line between old and new code. New code that implements an operation for programs to call, such as grouping or the skin, is waist. Old code that decides what a record means is anatomy. Vantage's account says this directly: "above the waist" does not mean every new namespace is a new primitive below it.
- The three foundation seams that bit every build, continuation, identity and hit, are exactly where a value stops being a value: a paused run, a name that must survive change, a pixel that must point back to source. Those are where an ECS over an existing engine leaks.
`─────────────────────────────────────────────────`

## Where the documents don't say

- **Workspace's starting commit.** Its handoff never names one, and the baseline says other builds' starts were not checked. "One common baseline" holds on the documents for Place and Vantage only.
- **What a program can express.** None of the four says. Arithmetic and field access are shown; conditionals, iteration and collection operations are unstated. This is the direct blocker for judging whether projection rules can move.
- **Whether a tool can call a tool.** No document says a program step can invoke another tool record. Vantage composes tools only in code, the roots panel drawn by a text-tool instance and the prompt drawn through the text tool.
- **Whether the capability table is itself a record.** All three treat it as code. Whether a record could register a capability is unstated.
- **Place's kinds.** Whether kinds exist as data or code is unstated.
- **Workspace's commands and dependencies.** Whether commands are records is unstated. Whether a tool declares what it reads is unstated; the handoff says only that there is no dependency graph.
- **Vantage's move latency.** Unstated, where Workspace measured its equivalent.
- **The old wound.** All three say the old client's typing, caret and paste failures remain open. None moved the editor into records, and two ride the native textarea.
