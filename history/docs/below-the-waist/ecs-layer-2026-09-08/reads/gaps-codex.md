I’ll read the four documents as the complete ground, identify what each build had to add, and group those additions into pieces for the next build, marked 1/3, 2/3, or 3/3. Where the documents don’t establish a need or a shared piece, I’ll say so.


The baseline already supplies the executor, resumable computation, renderers, picking and text geometry. Place’s main addition was joining them into a live record world: input changes records, affected tools run, output appears, and pointing and editing lead back to the source. I’m checking how much of that same connection the other two builds had to supply.


All three had to add client state, event delivery, source-linked inspection, editing and working context above the existing primitives. Their deeper additions differ: two connected durable records, two made text an executor tool, and one built authoring layers and agent turns. I’ll keep those counts separate from gaps all three mention but didn’t complete.


The important boundary is between a repeated need and a shared solution. All three wrote their own view composition and interaction code, but the documents don’t establish a common authoring vocabulary. Persistence has a different status: Workspace and Vantage built the connection; Place explicitly left it unfinished. I’ll make both distinctions visible in the list.


**Eight pieces recur across all three builds: seven client mechanisms and one rendering connection.** Those are the strongest candidates for the shared layer to provide before the next build.

The counts below mean a build **supplied or used something beyond the baseline**. An explicitly unfinished capability is marked separately. Shared-tree reuse counts as a need in both designs; it does not establish independent implementation.

The baseline already supplies execution, continuations, rendering, individual locators, text geometry and server revision machinery. The additions are the client ownership and connections around those capabilities. [Baseline]( /mnt/data/projects/Softland-ecs-layer/docs/below-the-waist/ecs-layer-2026-09-08/BASELINE.md:79)

| Piece to build out | Count | [Place / Inside](/mnt/data/projects/Softland-ecs-layer/docs/below-the-waist/ecs-layer-2026-09-08/ECS-LAYER-WRITEUP.md:65) had to supply | [Workspace / Anatomy](/mnt/data/projects/Softland-ecs-workspace-20260908/docs/ecs-workspace/SESSION-HANDOFF.md:35) had to supply | [Vantage](/mnt/data/projects/Softland-ecs-workspace-20260908/docs/below-the-waist/vantage/ROUND-ACCOUNT.md:50) had to supply |
|---|---|---|---|---|
| **1. A live client host** | **3/3** | A real page/canvas, boot, resize, presentation and resource lifetime. | A browser application, native rendering host, browser text-input bridge and frame delivery. | A served page, browser shell and explicit ownership of presentation, input and asynchronous resources. |
| **2. A client record world and reference rules** | **3/3** | An in-memory world of id/revision/asserter-bearing records, active tools, events and results. | Typed entities with components, provenance, exact revision references and immutable edits. | Client-readable source records, saved tools and vantages, and selection of versions through authoring layers. |
| **3. Change delivery and computation dispatch** | **3/3** | An input queue, event records, record replacement, affected-tool execution and an idle-sleeping clock. | Browser actions and server commands that adopt accepted entities, execute the relevant operation and request presentation. | Immediate local derivation, ordered edit delivery, store-change subscriptions, targeted rereads and reruns. |
| **4. Subject/tool/view composition** | **3/3** | Rules assembling tool inputs and projecting records into scenes, inspector text, flow lanes, trails and Inside. | Subject/tool/representation references, inspection interpreters, panel templates and placement rules. | Vantage-selected subjects and tools, plus the conversation/map/roots assembly and projection rules. |
| **5. A rendered-hit-to-source connection** | **3/3** | Screen → spatial/chart hit → binding, record, field, tool and revision; inspector lines retain their own sources. | Spatial samples and selected run steps connected to known dab positions, recipe anchors and inspected records. | Text, roots-panel and map hits connected to source/tool addresses; sphere sampling through its pointer mechanism. |
| **6. An editing loop over record values** | **3/3** | Numeric drafts, source-position caret/selection, commit/cancel and ordinary record-change delivery. | Numeric and note editing, focus/selection, commit rules and saved inspection-tool revisions. | Passage and tool-source editing, root/program selection, incomplete-EDN handling and durable edit delivery. |
| **7. Viewpoint and working-context records** | **3/3** | Vantage camera, placements, ancestry and exact return within the browser lifetime. | Baseline/candidate selection, kept references, notes, tool choices and saved camera/context. | Subject, tools, pins, zoom, selected layers and durable parent-linked navigation. |
| **8. Computed painting/coating → visible 3D surface** | **3/3** | Float-surface upload and a sphere-skin material, sharing the painting with the flat chart. | A surface-to-mesh adapter mapping actual painted cells onto the sphere support. | Native meshes displaying retained coating and brush output, plus a renderer repair needed to make the coating visible. |

There are important limits to what those shared counts establish:

- **Change delivery is 3/3; selecting reruns by comparing complete input values is documented in Place and Vantage, 2/3.** Workspace uses explicit application commands and expressly lacks an automatic dependency graph.
- **Subject/tool/view composition is 3/3 as newly written machinery.** Each build still implements substantial composition and interaction rules in application code.
- **The painted-surface connection is shared rendering work.** Its recurrence reflects all three using the sphere/coating specimen; that alone does not make it part of a general ECS mechanism.

The remaining additions have narrower incidence:

| Additional piece | Count | What had to be added |
|---|---|---|
| **A durable client record wire** | **2/3 — Workspace, Vantage** | Adapt client values to the existing durable owner, preserve their kinds and identities, deliver accepted edits, and restore the working context. Workspace uses a typed adapter with exact readback; Vantage connects common edits and repairs kind/visibility preservation. **Place explicitly leaves this unfinished as C7.** |
| **Text as an executor-backed tool** | **2/3 — Place, Vantage** | Connect text layout/placement capabilities to tool programs and inputs. Workspace uses native text layout/rendering, but its account does not describe making text an executor tool. |
| **Inspectable execution/activity records and their view projections** | **2/3 — Place, Workspace** | Place creates measured tick/run traces and source-linked flow/log views. Workspace creates inspectable brush-run observations and step/source/spatial correspondence. Vantage’s analyzer relationships and commit receipts have different meanings. |
| **A context-selection tool that produces readable text** | **2/3 — Workspace, Vantage** | Workspace renders selected fields and exact references as inspectable context. Vantage executes a prompt-tool record and saves a preview. Place names agent work as absent. |
| **Hold an execution open when current input runs out** | **1/3 — Place** | Extend the executor with `hold-at-end`, so appended pointer points resume a stroke. Baseline run/resume already existed; this exhaustion behavior did not. |
| **Connect pending read work to durable request/result records** | **1/3 — Workspace** | Store read identities and full declared arguments/results; grant work from the caller; keep the suspended continuation transient. The ownership rule was inherited design, but this connection was missing. |
| **Bind a pending pointer operation to the view snapshot it began against** | **1/3 — Vantage** | Start a pending read, validate the later answer against complete snapshots, and refuse a gesture whose viewed material changed. |
| **Per-asserter working layers and promotion** | **1/3 — Vantage** | Candidate revisions over shared base, selected-layer reads, attribution, stale-candidate/base checks and explicit promotion. Place and Workspace both identify this as missing. |
| **An agent turn connected to client records and layers** | **1/3 — Vantage** | Connect the saved preview and configuration to invocation, retain the rendered prompt in the turn, own the process, and stage the reply for promotion. Existing turn storage and CLI execution were inherited. |
| **Connect stored code knowledge to an explorable subject** | **1/3 — Vantage** | Invoke existing import/analyzer passes, build the catalog and map projection, connect source selection and changed forms, and attach producing-turn receipts. Place’s authored architecture catalog and Workspace’s Anatomy studies do not supply this connection. |

The supporting passages are the accounts’ descriptions of [Place’s execution and inspection additions](/mnt/data/projects/Softland-ecs-layer/docs/below-the-waist/ecs-layer-2026-09-08/ECS-LAYER-WRITEUP.md:225), [Workspace’s application, storage and read-work connections](/mnt/data/projects/Softland-ecs-workspace-20260908/docs/ecs-workspace/SESSION-HANDOFF.md:88), and Vantage’s [layers and agent path](/mnt/data/projects/Softland-ecs-workspace-20260908/docs/below-the-waist/vantage/ROUND-ACCOUNT.md:104), [pointer and change delivery](/mnt/data/projects/Softland-ecs-workspace-20260908/docs/below-the-waist/vantage/ROUND-ACCOUNT.md:162), and [code connection](/mnt/data/projects/Softland-ecs-workspace-20260908/docs/below-the-waist/vantage/ROUND-ACCOUNT.md:197).

Some additions were repairs or performance work within those pieces. Place added bounded coating raster work and exact surface-bit comparison. Vantage repaired text-result traversal, compositor hit retention, stored-record identity collisions, rendering pass selection and source-span addressing. Workspace added a presentation fallback for its observed browser setup. These belong with the relevant implementation being carried forward; the documents do not establish them as separate universally needed ECS facilities. [Place details](/mnt/data/projects/Softland-ecs-layer/docs/below-the-waist/ecs-layer-2026-09-08/ECS-LAYER-WRITEUP.md:233), [Vantage text details](/mnt/data/projects/Softland-ecs-workspace-20260908/docs/below-the-waist/vantage/ROUND-ACCOUNT.md:62), [Vantage repairs](/mnt/data/projects/Softland-ecs-workspace-20260908/docs/below-the-waist/vantage/ROUND-ACCOUNT.md:296), [Workspace fallback](/mnt/data/projects/Softland-ecs-workspace-20260908/docs/ecs-workspace/SESSION-HANDOFF.md:106).

**The most consequential common gap is the vocabulary for composing and interacting with tools.** All three supplied local implementations of it. All three still require source changes for substantial new interface behavior. Their editable records cover selected behaviors—brush parameters, inspection relations and arrangements, text/map programs—but none supplies general creation, mounting, composition and interaction through records. That is a **3/3 remaining gap**, distinct from a completed shared mechanism. [Place boundary](/mnt/data/projects/Softland-ecs-layer/docs/below-the-waist/ecs-layer-2026-09-08/ECS-LAYER-WRITEUP.md:339), [Workspace boundary](/mnt/data/projects/Softland-ecs-workspace-20260908/docs/ecs-workspace/SESSION-HANDOFF.md:123), [Vantage boundary](/mnt/data/projects/Softland-ecs-workspace-20260908/docs/below-the-waist/vantage/ROUND-ACCOUNT.md:256).

The documents give concrete examples from which to derive that vocabulary, but **they do not specify one common record/reference model, composition interface or interaction language**. They also leave general correspondence across representations and across subject changes unresolved. Those are the remaining design questions exposed by the builds; another general executor, renderer or durable store is not the missing piece documented here.

<oai-mem-citation>
<citation_entries>
MEMORY.md:1-2|note=[Located prior ECS context; comparison claims grounded in the four supplied documents]
</citation_entries>
<rollout_ids>
</rollout_ids>
</oai-mem-citation>