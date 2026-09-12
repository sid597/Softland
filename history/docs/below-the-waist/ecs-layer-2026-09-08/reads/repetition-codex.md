I’ll read the four documents as the ground, look for mechanisms all three independently built, and name the shared layer pieces only where they would make the next build smaller or move more of it into records. If a writeup leaves something unspecified, I’ll call that out.


The baseline already supplied recipe execution, rendering and individual hit tests. The shared addition is emerging in how the builds connect those abilities: records describing the working view, bindings from records into tools, and a way back from a visible result to the material it represents. I’m checking which parts all three actually describe, and what each shared piece would remove from a fourth build.


All three built the machinery for showing a subject through a chosen tool, retaining enough identity to inspect it, and editing records from that view. They also each built a record for preserving working context. The clearest possible saving is to make those relationships one shared mechanism; the documents show that much of the surrounding mounting, panel layout and interaction wiring still lives separately in each client’s code.


I have five candidates, with the strongest being a shared “representation instance”: this subject, through this tool, with these view inputs and placement. The main limits are also clear. The builds do not all provide durable storage or authoring layers, and Vantage explicitly leaves its sphere separate from the code map. So the common layer can carry declared correspondences, but these accounts do not establish a universal way to discover them.


**The strongest common invention is a representation instance: a particular tool applied to particular material, with its own view state and placement, retaining enough identity to inspect and change what it shows.** All three built that relationship. They distributed it differently across records and application code.

I would make that one piece of the layer, supported by four other pieces they also repeatedly built: **source correspondence, edit bindings, working context, and an event host.** Each has a concrete reason to make the next build smaller.

The baseline matters here: recipes, capability execution, rendering, individual locators and revision storage already existed. The new work connected those abilities into an interactive place. That connection is what there is to extract. [Baseline]( /mnt/data/projects/Softland-ecs-layer/docs/below-the-waist/ecs-layer-2026-09-08/BASELINE.md:79)

Here is the correspondence across the three accounts:

| Shared piece | [Place / Inside](/mnt/data/projects/Softland-ecs-layer/docs/below-the-waist/ecs-layer-2026-09-08/ECS-LAYER-WRITEUP.md:163) | [Workspace / Anatomy](/mnt/data/projects/Softland-ecs-workspace-20260908/docs/ecs-workspace/SESSION-HANDOFF.md:74) | [Vantage](/mnt/data/projects/Softland-ecs-workspace-20260908/docs/below-the-waist/vantage/ROUND-ACCOUNT.md:256) |
|---|---|---|---|
| **Representation instance** | Active tools declare record inputs and outputs; vantages supply camera and placements; client projections assemble the views. | Representation records independently reference a subject and tool, choose a view, and name a placement. | A vantage selects subjects and tools; tool roots/programs produce views; the page mounts them through a fixed assembly. |
| **Source correspondence** | Sphere/chart hits, inspector lines and trace geometry lead back to records, fields, tools and revisions. | Spatial positions, run steps and recipe passages connect through declared anchors and exact references. | Text hits retain source/tool anatomy; map strokes retain underlying relations; pointer reads check view snapshots. |
| **Edit binding** | An inspector field addresses brush radius or the flow expression; committing changes its source record. | Fields revise brush inputs, notes and the inspection definition while earlier references remain available. | Passage and tool-source edits use the same revision/layer delivery path; root selection addresses a source field. |
| **Working context** | Vantage and `where` records retain camera, placements, navigation ancestry and return position. | Workspace records retain runs, tool choices, kept references, notes and saved view state. | Vantage records retain subject/query, tools, pins, zoom, selected layers and parent. |
| **Event host** | Input delivery changes records, runs affected tools and requests presentation; the clock sleeps when idle. | Events update local state or send commands; accepted values are projected into renderer inputs and a frame is requested. | The courier and change delivery update explicit inputs; computations run and frames present their results. |

**1. Representation instance — “show this material through this tool, here.”**

This is the central extraction because it separates things every build needed to vary independently: the subject, the tool definition, and the circumstances in which that tool is being used.

As one layer piece, its record would identify the subject or input references, tool definition, view parameters and placement. Shared code would resolve those inputs, invoke the tool and mount its output through the existing rendering capabilities.

Workspace already gives this relationship a particularly explicit record shape. Place distributes it across active-tool declarations, vantages and projection functions. Vantage distributes it across its vantage value, tool records and page assembly.

**The next build gets smaller in both code and records:** applying an existing tool to another subject becomes creating an instance record; placing another instance beside the first becomes composition data. The next client stops writing its own version of “select these records, apply that tool, put the result there.”

There is still a boundary to choose: all three retain substantial composition in code. Their accounts establish the repeated responsibility, but do not supply a finished common language for panel composition, layout and mounting.

**2. Source correspondence — “what does this part of the result refer to?”**

All three had to add meaning to the result of a geometric hit. The baseline could locate a mesh, painted Path membership or text position. The builds connected those locations to the material someone could inspect or edit.

I would make that connection an explicit result accompanying a representation: an output part refers to these subject addresses, tool addresses and relevant revisions or snapshots. The layer consumes that correspondence for inspection, selection and editing.

**The next build gets smaller primarily by reusing code.** It supplies the mappings peculiar to its subject, then inherits the machinery for carrying those mappings through picking, inspection and source-directed actions. More of the correspondence can also live in records instead of being reconstructed separately by each panel.

This extraction has a clear limit in the documents. Workspace describes a bounded mapping for the brush specimen. Place has specific reverse joins. Vantage explicitly says its sphere is a separate tool beside the architecture map and thread. The shared piece can **carry declared correspondence**; these accounts do not establish a general way to discover correspondence between arbitrary representations or preserve its meaning through arbitrary subject changes. [Workspace’s boundary](/mnt/data/projects/Softland-ecs-workspace-20260908/docs/ecs-workspace/SESSION-HANDOFF.md:50), [Vantage’s boundary](/mnt/data/projects/Softland-ecs-workspace-20260908/docs/below-the-waist/vantage/ROUND-ACCOUNT.md:237)

**3. Edit binding — “this control changes this addressed value.”**

The common invention goes beyond displaying editable tool data. Each build connected a visible control or source selection to an addressed value, handled input, revised that value, and made its consumers reflect the change.

As one layer piece, an edit binding would name the target field, editing capability, any value conversion, and commit policy. Shared machinery would provide the editing session and deliver the resulting record change.

The conversion matters: Place’s flow coefficient editor turns a number into part of an expression. The commit policy matters too: Place commits its numeric edit on Enter; Workspace commits different fields through different actions; Vantage saves individual keystroke revisions. Those differences should be expressible choices.

**The next build gets smaller in both ways:** ordinary fields become binding records using existing editing capabilities. A new inspector no longer needs another implementation of field targeting, input routing and record replacement. New editing semantics still need an implementation.

Durable acceptance and authoring-layer promotion can extend this path. They are additional capabilities demonstrated by particular builds; Place’s in-memory edit loop already establishes the smaller common piece.

**4. Working context — “retain the circumstances of this work.”**

All three found that retaining the subject alone was insufficient. They needed to retain which tools were being used, what was selected or held nearby, and the view state needed to return.

I would make working context a record referring to representation instances and retained material, with their saved view state and navigation relationships. Restoring it would be a shared operation.

**The next build gets smaller because it supplies the context values instead of inventing another state bundle and restoration path.** This is especially useful when two instances show the same subject through different tools, or an earlier run must remain beside a candidate.

The common result is context represented as values. Place restores it within the browser lifetime; Workspace and Vantage also persist it. The reports also use different reference semantics: Workspace emphasizes exact revisions, while Vantage’s pins retain identifiers. A unified piece needs to express what a reference follows or holds; the documents do not give one shared resolution rule.

**5. Event host — “own input, updates and presentation.”**

Each build wrote an application host around machinery that previously ran on benches: browser input, local interaction state, computation delivery, frame requests, and process/resource lifetime.

**This makes the next build smaller through shared code.** Another subject should inherit those owners and connect its tools and actions to them. It should not need another browser shell just to turn changed material into an updated native view.

The common evidence supports an event host. Place and Vantage additionally describe complete-value boundaries for deciding which computations run. Workspace explicitly uses application commands and does not claim an automatic dependency graph. A general dependency scheduler would require further design; it is not already present in three different forms.

The biggest saving comes when these pieces work together. A fourth build adding another inspection of a run should supply:

- The subject records and any new computation.
- A representation instance choosing the tool and placement.
- Correspondence for the result’s meaningful parts.
- Edit bindings for the fields it exposes.
- Working-context records for what should remain available.

It should inherit mounting, source-directed inspection, ordinary field editing, context restoration and event/presentation ownership.

**That is the substantive support these builds give the ECS-layer hypothesis:** they repeatedly constructed the same relationships around different subjects and tools. Making those relationships executable records would absorb recurring application code. The documents leave the exact composition vocabulary open, but they identify what it must replace—and why merely standardizing an entity envelope would capture only part of the saving.

<oai-mem-citation>
<citation_entries>
MEMORY.md:43-45|note=[Initial worktree orientation only; analysis uses the four supplied documents]
</citation_entries>
<rollout_ids>
01a081be-aa80-7530-b32e-bd5533913148
</rollout_ids>
</oai-mem-citation>