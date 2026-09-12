# The vantage round — the layer above the waist that makes the client

The chair's position, 2026-09-08, written at Sid's word after the "what next" round (three sessions' answers read against the code; receipts below are reads from this session, grades marked). Checkpoints, not a contract: the prototype is the spec, tests are built with the code, the READMEs and docstrings carry what is implemented, and this file changes in the same commit as the code whenever a milestone changes shape. Names are the chair's; Sid renames at settlement.

Sid's words for it: the ECS layer, the minimal layer we can build on top of the existing code, what I stand in front of, Softland using Softland, the three layers (text, 2D, 3D) to see the flow of the architecture live.

## The hypothesis the build tests

The client is a rendering of one record, the vantage, over asserter-owned layers of records, through tools that are records with roots (the person's level) and a program (the computation). If that holds:

1. Pointing at anything on screen returns what it is, where it is, who made it, through which tool, at the roots level.
2. Changing a root or a program step of a tool redraws the thing without engine code.
3. The agent's view is the same tool with a string as its output device, readable before the run.
4. A reply lands in the agent's layer at the record it concerns, and becomes shared truth only when promoted.
5. The four block-editor deaths are data edits: paste share, what ctrl+enter shows, where a reply attaches, kinds set by tools.

It fails, early, if the typing loop cannot run per record at typing speed, or if a tool's roots read at the wrong level to Sid. Both are findings, not reasons to hack.

## The layers, in ECS words, with what exists

| Layer | ECS word | Sid's word | Today (read this session unless marked) | New in this round |
|---|---|---|---|---|
| Vantage | the world | what I stand in front of | Nothing says what a person looks at, through what, with what held. Nearest: the camera cell per world, x, y, zoom, `src/app/server/episode/episode.clj:237-345`; zoom limits and gesture bindings as a space master `src/app/server/worn/space_material.cljc:16-41`; block placement `positioned_material.cljc:20-29` (the reply gap is the number 34.0; the rule that a reply sits below the prompt was code) | The record: subject query, open tools, pins, zoom, asserter, parent. Made on every move. Saved as it is. The trail is the parent chain. |
| Layer | scratch world | the working layer, the uncommitted store | Two stubs nobody reads: a branch field written on every edit request `src/app/server/rama/object_container.clj:1730` and merged as `main` by the envelope `rama/envelope.clj:16,182`, read only by a non-blank check; an overlay target kind in the envelope's kind set `envelope.clj:47-50` with no other reference. A record comes back as a document after its first edit: the edit path recomputes kind as document or text block and forces visibility private `object_container.clj:1493-1498`; the import wrote document and private `ingest/clojure_adapter.clj:370-373`. | The store reads base plus one open layer; edits from the client land as revisions tagged with the layer; promote folds them; kind carried on edit. |
| Tool records | components (roots) and system (program) | tools as data | Path: `src/app/client/path/records.cljc:35-58`, capabilities `path/construction.cljc:18-37`. 3D: `src/app/client/region3d/records.cljc:60-81`, capabilities `region3d/capabilities.cljc:48-69`. The runner splits a record into roots and program (`records.cljc:30-31`). The harness edits four roots and reruns, `harness/pickup.cljs:35-38`. Tested, not live. | The text tool. The prompt-rendering tool. The map tool. A tool's roots carry plain names for themselves as data, so the roots panel shows Sid's words and keys show on demand. |
| Text | a system that is still code | text | One pure layout function `src/app/client/text/layout.cljc:960-968` returning lines, runs, glyphs, clusters, metrics, a clip plan; caret `:1333`, selection `:1372`, hit test `:1487` as functions over that result; its own GPU draw `text/renderer.cljs:718-730` from `glyph_pack.cljs:218`; touches the compositor only for a scissor. No tool record, no vocabulary names text, nothing samples it. Layout test red because the harness reader expects an older result shape (read by the "what next" round's hunter; the layout itself unshown). | Text as a compositor layer kind that declares its own sample (the hit test) and snapshot (layout id, source revision), the road the coating took. Layout granted to the executor as a capability. Painting stays on the GPU road reading the same layout result. |
| Picking | picking | point at a thing and see its anatomy | The coating's locate returns binding and record ids `region3d/coating.cljc:27-73`; the surface's sample merge keeps color, contributors, coverage and drops them `engine/surface.cljc:122-160`. | The merge carries what layers return. The pointer as a pending read answered by events; a stale answer refused by snapshot. |
| The loop | scheduler | the clock | Nothing reruns by itself; a run is an explicit caller act `engine/executor.cljc:334-340`; the utterance route streams a turn's events to its caller as server-sent events `server_jetty.clj:1524-1543, 573-716`, the one live channel that exists. | Store change, rerun of the tools whose inputs changed, repaint. |
| The wire | | | The new client calls the server for nothing, and no route returns a thread's turns or a container's revisions to any client: every route is a POST under one handler `src/app/server/door/server_jetty.clj:1389-1606` (relation assert, matter room, facet drill, the utterance stream, block birth, geometry). The conversation face that assembles geometry, camera and turn records `page/face_projection.clj:470` has no caller in src. Store reads that exist on the JVM: container, revision history, current revision, unit `rama/object_container/runtime.clj:363-388`, relations by target `rama/relation_kernel.clj:951-960`, a conversation's rows and turn records `episode/episode.clj:573-582`. | Read a thread and the vantage rows; write layer edits and vantage moves. Existing routes where one fits; one added where none does. |
| The turn | | ctrl+enter | The durable row is written before the spawn, open, and again after it with the final status `episode/episode.clj:557-571`, called from `server_jetty.clj:505, 685`; its fields are world, turn, source unit, content and its hash, position, status, time, previous turn, thread, episode, receipt. Model and effort come from the invocation facet (defaults: sonnet, low, thread plus two, `worn/invocation_material.cljc:23-29`), read only when the turn has a source unit, into the spawn arguments and a printed line `server_jetty.clj:397-437, 611-640`; the prompt is composed in code at the same seam. | Three fields on the row: model, effort, the rendered prompt. The prompt is a tool's output. |
| The page | | the screen | None a person opens. The client is one browser build entered from the harness core `shadow-cljs.edn:5-17`, run only under the verifier's Puppeteer with SwiftShader, which serves an html string by request interception `test/render_engine/run_verifier.mjs:20-121`; no html under resources, no watch build, no keyboard handling anywhere in the client (grep: none); caret, selection and hit test are called with fixed arguments in the harness only `harness/text.cljs:466-470`. The product server is Jetty on 8080 `src-dev/dev.cljc`. | A served page, a watch build, WebGPU in a real browser. The first base component. |
| The store's boot | | | The app boot rides a localhost Rama conductor by default and the in-process cluster with `LAND_CLUSTER=0` `door/cluster.clj:32-39`; ingest sweeps, the transcript distill and the git spine sit on that boot path `cluster.clj:281-293, 459-541`; no Rama process is running right now. What is in the store is what the boot you run puts there. | Nothing new; the builder runs the boot that feeds it and says which. |
| Code as records | | how a change travels | The code import writes a document per file and a derived unit per top-level form with its kind (ns, def, fn, record, protocol, macro, test) and a source anchor with start, end and block path `ingest/clojure_adapter.clj:62-78, 397-424`; the analyzer pass writes requires between namespaces and calls between vars, retracting stale ones `ingest/code_import.clj:639-805`; relation kinds are requires, calls, supersedes `relation_kernel.clj:62-88`. Neither pass has a caller outside tests; the tests run them against this repo scoped by a commit filter. | The map tool reads these; a boot step or route runs the passes. |
| Kinds | component types | folk types, #TASK by hand | Closed lists in code; kind lost on edit. | Carried on edit. Vantage and layer added to the list in code. Kinds as records are the next round, not this one. |

## The milestones

Each lands as one commit on the round's branch: code, its tests, the README and docstrings, and this file's checkpoint line. Each is something Sid sees on the vantage page, which is the client from M1, not a bench; the harness pages stay benches.

**M1. The page and the text tool as a record.** First the page a person opens: served html, a watch build, WebGPU in a real browser, since today the client exists only under the verifier's Puppeteer. Then the text tool. Roots: source (which records), font, wrap, rules (paste share, reply attaches at, kind by tool). Program: layout, place. The placed result is a layer with sample and snapshot. The vantage page draws a fixture thread (chat messages in the store's shape, one reply with a relation to the record it concerns) through the tool. You see: the thread, typeset with real fonts; a roots panel in plain words; change the font size, the line width, the reply rule, and it redraws; type into a record and it redraws per keystroke; click a run and the page shows line, run, source record, tool, and refuses the click if the layout it was answered against is stale. The ceiling measured here: per-keystroke rerun time for 10, 100 and 1000 lines, written by the page to a file; if the whole text relays out per keystroke, per-line layout as a level with explicit inputs is the fix, before any memo.

**M2. The vantage and the wire.** The vantage record with its kind in the store; the page opens at the last vantage and draws a real thread from the store through the text tool; reload returns to the same place; moving to another thread makes a new vantage with the old as parent, and the trail is drawn from the chain; pins hold record ids across the move. The wire reads only, through one added route that serves the conversation face, which exists as a function with no caller. You see: your own chat turns, put in the store by the boot's transcript ingest, on the page you will work in.

**M3. Layers.** Keystrokes land as revisions in your layer; the base is unchanged; the layer's records draw distinguished by the tool's rule; promote makes them base; a second tab on the same thread sees base until promotion; a chat message edited in a layer is still a chat message. The vantage carries whose layers it shows. The wire writes.

**M4. The agent's window.** Ctrl+enter on the vantage shows, before the run, the prompt as it will be sent, drawn by the text tool, with model, effort and precontext beside it as the invocation facet's values; the prompt is the prompt-rendering tool's output over the same roots; the existing spawn runs it; the reply lands in the agent's layer at the record it concerns, distinguished, promotable. The row carries the three fields.

**M5. The pointer and the loop.** Pointing is a pending read in the executor that a tool step can wait on; events answer it; a stale answer is refused. The loop reruns the tools whose inputs changed when the store changes, including an agent's reply arriving. The roots panel is now the text tool drawing the tool's own record, so changing a root and a program step is editing a record on the same page, live.

**M6. The map.** Softland's architecture as records drawn by the path and 3D tools in the same vantage: namespaces and their requires from the code import and the analyzer pass, run against the repo into the store by a boot step, as regions and strokes by a map tool; the sphere with its coating beside the thread; a change as a subject query (the units a commit touched, the namespaces they sit in, the agent turns that produced it) lighting the map; zoom moves the subject query from module to namespace to function with pins holding. The three layers, one pointer, one set of layers.

The order is by dependency. M1 needs nothing from the store; M2 needs the boot with its ingest and one read route; M3 the store's layer read and the kind fix; M4 the seam; M5 the executor's waiting shape, which exists; M6 the code passes run into the store, which exist as functions.

## The shape of the round

- A worktree off `main` on its own branch; one commit per milestone by exact path; push and merge are Sid's, after he looks. Process, guards and the commit discipline: `docs/below-the-waist/two-chairs.md`.
- The server keeps its shape. Inside the round: kind carried on edit, the layer read, the two kinds, the turn row's three fields, the prompt from a tool at the spawn seam, and the routes the wire needs. Anything beyond that is the writeup below.
- Sid's rule for the blocked state, his words: once you get to a state where you tried everything and cannot build forward, write up what you were going to build, what all you tried, where you are, and what is the missing critical piece that needs approval. That lands as `ASK-N.md` beside this file, committed, and the session stops there.
- Milestones may change while building; they change in this file, in the same commit, so Sid knows.
- Everything Sid says is an approximation; the builder fills the rest with its own judgment and says where it did.

## Positions the chair took, each with its exit

- Keystrokes are records in your layer, saved as they are; settlement is promotion. What is in flight to the server is the courier's queue, never a store-side buffer. Exit: if per-keystroke writes cost typing latency, the queue absorbs it, not the store.
- Text paints on its GPU road and samples through the layer protocol, both from one layout result. Exit: any glyph where draw and hit test disagree is a second source, fixed at the layout result.
- The prompt-rendering tool replaces the composed prompt for vantage turns; the old block editor's path keeps its own. Exit: if replies get worse under the tool's rendering than the hand-built prompt, that is the level detector on the agent's side, a finding, not a revert.
- Vantage and layer are kinds in code for this round. Exit: if adding a kind touches more than the list and one reader, that is a finding about kinds as records.
- DOM is allowed for panels through M3; from M5 the panels are tool instances. Exit: none; that is the self-hosting test.
- The wire is EDN over Jetty like the routes that exist, with one read route added for the conversation face and the utterance stream as the first live channel. Exit: the store's own change feed replaces it when the loop needs more than one turn's events.

## What Sid holds, verbatim

"when you see the ceilings do not try to hack through them, we want to build through them. caching is never an option, it means the underlying thing is as it should be and then still need more performance therefore we need to do caching on top. the data should be saved as it is. the prototype is the spec, no contracts. best in field is a datapoint; nothing is carried forward for existing. and be anal about what exists exactly; we deviate from what is true the moment we say "works" about something we only read."

"you are the best at ui so i expect whatever you make to be quite high quality."

## Checkpoints

| Milestone | State | Commit | What changed in shape |
|---|---|---|---|
| M1 | not started | | |
| M2 | not started | | |
| M3 | not started | | |
| M4 | not started | | |
| M5 | not started | | |
| M6 | not started | | |
