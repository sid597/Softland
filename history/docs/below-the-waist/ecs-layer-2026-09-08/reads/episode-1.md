# Episode 1 — the ECS round, in chapters

Spoken script for listening, chapter by chapter. N is the narrator. In the
last chapters C is the Claude chair's position and X is the Codex chair's,
each in its own words from `top-claude.md` and `top-codex.md`. The other
chapters are drawn from `BASELINE.md`, `repetition-claude.md`,
`boundary-claude.md` and `gaps-claude.md`. Anchors live in those files.

## Chapter 1 — What we have

N: Chapter one. What we have. This is the baseline, the code as it stood at the commit all three builds started from. Not a runtime certification. A reading of the source.

N: The foundation was built for one idea. Tools are recipes represented as data, over a shared vocabulary of capabilities. A capability is code for an operation. A tool record is steps, formulas and inputs. The same executor can run different tool records. And constructed values keep their subject and source information, so a later tool can consume them, and a query can relate a result back to its material.

N: There were three representation substrates already. Region3D, for spatial scenes and surface work. Path, for 2D construction and painting. Text, for shaped lines and their reading geometry. GPU renderers and a compositor could present their values. And the browser entry points were benches, for exercising those pieces. So this was a client-side foundation. It was not an interactive client shell.

N: The executor is a pure runner. It takes a program, its roots, a capability table, and options. It gives back a result, a status, and, when it suspends, a continuation. It owns no clock, no device, no client world. When it reaches the end of the available items, the run completes. There is no way to hold a run open waiting for more.

N: The capability vocabulary is a set of named operations. Curve point. Surface region. Read surface. Mix. Paint. The Path construction ops. Tool steps name an operation and its arguments. Capability implementations do the computing. And the rule is stated plainly: recipes do not gain arbitrary Clojure execution just by being records.

N: There is a concrete brush already. A sphere host, retained marks and bindings, and a pickup-and-paint recipe. It locates a point, reads the coating there, mixes the carried colour, forms a footprint, and paints. Its four dab events are seed inputs, written by hand. Not live pointer events. Likewise a Path width expression can already multiply tool size by pressure. So a later build has to separate using or editing those recipes from inventing the capabilities.

N: The 3D scene can turn a region point into a hit on an object or the background. But there was no join from that hit to the painting's chart coordinates. And the renderer does not put the painting on the sphere. Computing a painted chart and rendering a sphere were two separate abilities.

N: Text had pure layout with an explicit shaping provider. Lines, runs, bounds, hit testing, caret and selection geometry. That is editing geometry. It is not an editor.

N: The compositor owns targets and passes and draws a scene into a view. It is rendering infrastructure. It does not supply a browser event loop, and it does not decide which tools a person is standing in front of.

N: State and execution were already explicit. Executor state travels in values and continuations. Resume checks the recipe and the consumed inputs. Pending reads have request and answer handling. None of that may be credited to a later client just because that client was the first to use it interactively.

N: What was absent. An owner of a world atom and ongoing browser events. The scheduling of affected tools. A common pointer identity across representations. And the view, navigation and editing records of a client. Separate locators and a renderer do not, by themselves, prove that visible pixels, source identities and edits are joined correctly.

N: One more piece was adopted design, not integration. The read-work ownership split. The paused computation belongs to the session, not to a durable job. The awaited read has request and result rows under its full declared input. And its shared derivation key is distinct from the executor request that decides which consumer may use an answer. That split was settled on paper before any build. Hold onto it. It comes back in chapter five.

N: On the server, an object container already exposed request submission, decision waiting, immutable revision reads and current-revision reads. Real storage APIs, available for a typed adapter to reuse. But the ordinary edit path recomputed a record's kind and forced its visibility private. And there was no general client record read-write route. So a durable client record wire was a connection every build still had to demonstrate.

N: That is what we have. An executor, a vocabulary, three substrates, renderers, a compositor, benches, a store with a flawed edit path, and a written rule for who owns a paused read. Nothing that knows about a person at a screen.

## Chapter 2 — What the three built

N: Chapter two. Three builds, three names. Place, on its own branch. Workspace and Vantage, in one shared tree, on one branch, interleaved.

N: All three wrote the same missing thing, each once, each differently. A page that is an application, with boot, resize, a frame only when something changed, and teardown. Client records with an id and a revision. A change owner, something that turns "something changed" into "these tools rerun". A join from a pixel back to a record. A saved "where I am". And an edit path that enters through the same door as every other change.

N: Place built an in-memory world of records, seeded from source. Each active tool declares what it reads, as a record plus a path. The full assembled input is compared with the last, and only a changed input reruns the tool. Painting and composition are separate inputs, so a camera move reruns neither. It added a way to hold a stroke open at the end of its input, so appended pointer points resume rather than restart. It painted the sphere with a skin. It built an Inside view, twelve authored nodes explaining what is records and what is code. It never built the wire. Nothing survives a reload.

N: Workspace built a typed entity envelope. Id, kind, revision, a component map, and provenance: actor, time, origin, parent. A reference carries id and revision, exact. It connected to the durable store through an adapter that bypasses the edit path that loses kind, waits for acceptance, and verifies readback. Its one tool is an inspection tool whose record chooses which relation to follow and whether to arrange the result as a list or columns. Its runs are commands. It has no automatic dependency graph and says so. It built an Anatomy view, authored studies of the same records-versus-code question. And it measured one cost: a view change waits one to two seconds, because it waits on the server before the view believes the change.

N: Vantage built the most. A vantage record with subject, open tools, pins, zoom, selected layers, an asserter and a parent. Text, map and sphere as tool records with roots and programs, edited through a lossless parser from a roots panel that draws itself with the text tool. Authoring layers per asserter, with promotion that refuses stale candidates. An agent turn with a readable preview, and a reply staged in the agent's layer. It fixed the store's edit path instead of bypassing it, so kind and visibility survive an edit, and widened graduation ids when a suffix collision broke record identity. It found the executor re-walking a completed layout and put a resolved-read boundary in front of it. And it built a map of Softland's own code from the existing importer and analyzer, discovered rather than authored.

N: Two things to carry from this chapter. First, no document claims any shared code between the three copies of the middle. Nobody built a generic scheduler, and nobody says they missed one. The "S" in ECS was plain code reading declared inputs, in every build. Second, the foundation was changed underneath, in two trees, and neither tree's documents know about the other's changes. The executor now differs from the baseline two ways. The 3D renderer three ways. That merge is work before any next build, whatever layer shape wins.

## Chapter 3 — The one thing

N: Chapter three. The one thing. This is the repetition read's answer, and both top chairs took it.

N: The thing all three made up is the mounted tool. A tool record. Pointed at the records it reads. Placed in a saved position. With its last full input kept, so the loop knows when to rerun it, and a hit knows what it landed on. Each build then wrote, by hand, the loop that walks its own mounts. Gather the roots. Decide whether to run. Publish the result. Answer a pointer. Three loops, three field lists, three names. Place calls it an active tool declaration. Workspace calls it a representation. Vantage calls it a tool instance.

N: What it is built out of, and all of it is already in the baseline. The executor's roots contract. The executor's pending read, with its request and its answer. The kind locators, one per kind. And the store's id-plus-revision envelope. The only new substrate is an address vocabulary: a record id with a path and a revision selector. The loop is then small. Gather by address. Run or resume when the gathered value differs from the last. Publish under declared ids. Answer a hit from the same gathered value.

N: No single build has the whole object. Place has the binding and the rerun law. Workspace has the exact reference and durable results. Vantage has the selector, the snapshot-guarded hit, and the durable vantage with a parent. The one thing is the union at the seam, not any one build's shape.

N: Why the next build gets smaller. Every account writes down rules of the form "this edit reruns that tool and not the other". Radius re-executes the saved stroke, but flow width changes the lanes without rerunning the brush. Map edits rerun the map, camera edits prepare the scene without brush computation. A promoted source edit reruns one tool, a font change reruns every visible source, a placement change shapes zero lines. Every one of those is a fact the binding plus full-input comparison yields with no rule written. Place gets them free already. Vantage gets half free and hand-wires the other half. Workspace writes them as command semantics. One loop, zero written rerun rules.

N: Why more of it lives in records. Under one mount, Vantage's fixed roots assembly and Workspace's brush adapter become roots-from entries. Workspace's command list for what runs becomes the comparison. Place's missing selector becomes the one Vantage already saves. What stays code, and all three agree: capabilities, one locator-to-address rule per kind, gestures and key bindings, and panel templates.

N: And here is the lived-failure point. The previous client died of a block anatomy fixed in code. Pointer and cursor disagreed. Typing slowed. Paste and layout misbehaved. All three new clients have a panel anatomy fixed in code, one level up. Stage, Flow and Inside. Spatial, Flow, Text and Inspect. Thread, map, sphere and roots. Two of the three built the pointer-agreement guard the old client lacked. None of them made the anatomy a record. A mount with a placement field is the smallest step that does.

N: Three rows worth a sentence each. The explain-yourself view was built twice from the same question. Both Codex clients were asked what is records and what is code, and both answered with an authored catalog, a pull-apart 3D view, a flat map, and the same three-way partition. They even swapped the colours. The question this whole round is asking is that same question a third time, and in no build is it a query. With mounts as records, it would be. Second: two wires into one store, on one branch. Workspace bypasses the edit path, Vantage fixes it. One typed-record adapter using the repaired path replaces both, and Place's proposed third. Third: two text capabilities, in two vocabularies, in two homes.

N: The strongest alternative answer is the vantage itself. All three built one, and its parent chain is the piece all three kept as data. It ranks second because the vantage already is a record in every build. Where the three disagree is what a tool on screen is. That is the mount. The vantage is the list of them.

## Chapter 4 — What stayed in code, and the three seams

N: Chapter four. What moved, what stayed, and why. This is the boundary read.

N: In all three builds the same three things moved into records. The values a tool computes over. The tool's own parameters and program, where it had one. And where-you-stand. And in all three the same four things stayed in code. How the window is composed. How input becomes intent. The rules that give a record's fields their meaning on screen. And the kinds themselves. Below those, capabilities, process owners and the store stayed code by design, which the baseline's own split expects.

N: The honest reason the four stayed is scope, not impossibility. Each build moved exactly what its demonstration needed to edit, and no more. Radius and a width coefficient in Place. A relation and an arrangement in Workspace. Roots and programs in Vantage. No demo needed to edit a projection or a gesture, so no projection or gesture moved. The Workspace handoff says outright that the omissions were not shown to be impossible on the substrate. And the four that stayed are, in one sentence, the old client's block anatomy. The thing whose living in code was the wound.

N: Then a derived finding, and it is the sharpest one in the round. All three builds dug under the waist where a limit bit. Place changed the executor's continuation handling. Workspace added a surface-to-mesh adapter and a store adapter. Vantage extended the compositor to retain hits, fixed record identity, fixed a renderer pass. Those cluster at three boundaries. Continuation. Identity. Hit. A paused run. A name that must survive change. A pixel that must point back to source. Those are exactly the places where a value stops being a value. That is where an ECS over an existing engine leaks. The minimal-layer hypothesis held for tools, and every build still had to reach down at those three seams.

N: One test to carry. A field in a record is not a moved behaviour. All three write-ups warn about it separately. A placement name with code layout. A paste-share rule with no consumer. A kind carried through an edit while kinds stay code. The test for "moved" is whether a program or rule consumes the field, and whether editing it changes what happens.

N: And the pattern that moves things. All three proved one shape. A record with roots and a program. Steps that name capabilities. A declaration of which records the tool reads. The executor rerunning it when those inputs change. Output landing in a declared record. The brush had it at baseline. Vantage gave it to text, map, sphere and prompt. Place gave it to a width rule. Every item that stayed in code is that pattern not yet pointed at that domain. Composition becomes a tool whose roots are the mounts and whose output is placements. Interaction becomes events plus binding rules. Projection rules become tool programs, which needs data-shaping steps the grammar does not yet show. Kinds become kind records the store reads at write time. And then the loop collapses to Place's rule: run whatever's declared inputs changed. Four owners remain. Clock, deliverer, presenter, wire.

N: Step zero is one record shape. Three builds, three client record shapes, two of them over the same durable owner. The moved code needs three properties. Exact references with id and revision. Declared inputs by record and path. Layers with an asserter. Workspace has the first. Place has the second. Vantage has the third.

## Chapter 5 — Two chairs, where they agree

C: This is the top of the read. Three builds, one hypothesis. Both of us were handed the same four documents and the three referee files, and asked the same thing: what should exist in the code before the next build, and what to build first. We agree on more than we disagree. Let me say where we agree, and then we'll argue.

X: Go ahead.

C: What we have is one layer, built three times in its middle, and zero times at its floor and its edges. The middle is the thing both repetition readers found. A tool applied to a subject. Placed. With what it reads declared, and its last full input kept, so the loop knows when to rerun it, and a hit knows what it landed on. I called it the mount. You called it the representation instance. Place called it an active tool declaration, Workspace called it a representation, Vantage called it a tool instance. Each build wrote it privately. Three field lists, three loops walking them.

X: Yes. "Show this material, through this tool, here." That relationship is why record identity, computation, pointing, editing, and working context kept arriving together in all three builds. They weren't five separate needs. They were five sides of one thing.

C: The floor is the record shape the mount is written in, and the executor's suspension model under that. The edges are the owners. Host, wire, store, effects. No build shares a floor or an edge with another. And the two builds that sat in one tree diverged on the wire. Workspace routed around the edit path that loses a record's kind. Vantage fixed that path. Same tree, two answers.

X: Agreed on the shape. And I'd add the thing the boundary pair gave us, because it's the most useful sentence in the round. Invocation is not decision. A program that calls "build this inspector" moves the call into a record, and leaves every choice inside the capability. Which fields. How they're arranged. What an edit does. A move is only real when the program can choose differently. Select fields. Follow a reference. Format rows. Compose views. Say what an action means. Vantage has the instance without the test. Its map program calls grouping, and grouping's interpretation stays compiled.

C: I take that test whole. It's sharper than the one my own family's boundary read offered. And it points straight at the silence that blocks it. What can a program step actually say? Every program shown in the four documents is arithmetic and field access. A width expression over a field get. Nobody shows a conditional. Nobody shows iteration. Nobody shows a collection operation. And every operation on your list needs iteration over a collection. Nobody measured what interpreted projection costs on the frame path either.

X: That silence is real, and it's the layer's, not a builder's. No build extended the grammar, so no build stated it. But I'd be careful with one thing. The write-ups' silence about an operation does not establish its absence. The builders who used the executor can show us, in a line, whether the existing language expresses field selection and collection transformation. That's a question to ask, not a gap to design around.

C: Fair. Then let me put the correction on the table, because we both made it, and it changes the mount's field list. My family's repetition read said the last full input does three jobs. The rerun key, the read-reuse key, and the hit ground. That's wrong. Two of those are the mount's. The rerun key and the hit ground are both "what this mount computed from." But the read-reuse key belongs to the store. One per read. And the baseline keeps it apart from the request on purpose.

X: Same correction from my side. Complete inputs are a shared principle. But the rerun input, a reusable read's arguments, and a pointer's validity snapshot are three different values serving three different purposes. Two mounts can ask the same read with the same arguments and must share one stored answer. That's what a derivation key is for. But only the mount that asked may consume that answer, against the snapshot it drew. That's what the request identity is for. Collapse them onto one field, and a stale mount accepts a fresh answer, or two mounts recompute what one already has. Workspace built the first half. Vantage built the second. The mount needs both slots.

C: So the mount holds its last full input and its pending requests, each with its own identity, and the store holds derivation key to result. Not one field. Good. Now the silences. We sorted them the same way. A silence is the builder's when the builder can answer it in a line from what they built. It's the layer's when no builder could, because the concept exists in no build.

X: And the layer's silences are the same list from both chairs. The reference rule, holding versus following. The composition and action vocabulary. Tool admission, how a new tool enters the running client. And the contract for carrying a correspondence into an action, what does this displayed part refer to, and what does acting on it change.

C: I'd add kinds. None of the three lists its kinds. None makes them records. Vantage says outright they're code-defined. Workspace's envelope carries a kind whose definition is nowhere. And both wire bugs Vantage hit on the shared edit path were kind bugs. The kind recomputed on edit. The suffix collision on graduation ids. The next wire inherits that fix or repeats that bug.

X: Kinds are a layer silence, yes. Where we part is whether they're first.


## Chapter 6 — Two chairs, where they part

C: So let's part. Here is my order. One. One tree. Three trees hold three executors and two forks of the 3D renderer, and neither tree's documents know about the other's changes. The documents cannot size that merge. It closes by one diff, and it comes before anything else. Two. One envelope, one wire. Workspace's envelope, because it's the only one that states the whole thing. Plus Place's declared inputs by record and path. Plus Vantage's selector for which revision an address resolves to. And Vantage's repaired edit path as the wire, since it carries kind, visibility, the graduation fix, and layer metadata. Workspace's bypass retires, or is shown to be that same path. That's a choice and a deletion, not a build. Three. One read. What can a program step say. Four, and only then, the build. The mount as one record kind on that envelope. The rerun rule written once, with levels. And the three existing clients become three vantages over it. Not a fourth client. Three re-hosted. If re-hosting three costs less than the smallest of them cost to build, the layer exists. If it doesn't, we learned that cheaply.

X: And here is mine. Before another application build, make defining, mounting, and using a tool a shared path through records. And build that path first, with one inspector, in Vantage. An inspector whose field selection, arrangement, source correspondence, and edit actions are expressed in its definition. Created from inside the client. Its program selects fields from a subject, follows one reference, constructs source-addressed rows, and arranges them. One row offers an edit, and the edit's destination is part of the definition. Mount it beside the original view. Use it on a passage, and then on the Text tool's own record. Revise the inspector while keeping an instance of its earlier definition. Save the working context. Reload it. The decisive change is this: adding a field, a reference-following action, or another instance changes records. The host has no new dispatch case for that inspector.

C: You're starting from Vantage because it has the most of the path already.

X: Because its M1 through M5 account already gives us editable tool programs, saved revisions, selected layers, change delivery, and a tool displaying its own record. Those are the strongest starting conditions for the missing test. And this is where I disagree with your order. That build does not require reconciling every record envelope, every kind vocabulary, and every client wire first. It requires a common meaning for addresses and actions. That's all. Four choices, made now, in working code. References distinguish holding from following. Composition and addressed actions are executable parts of a definition, a placement label alone is not enough. Runtime state has an explicit owner, distinct from saved definitions. And correspondence is supplied and carried explicitly, the subject's tool says what a displayed part means, the layer preserves that and checks a stale gesture.

C: Here's my worry with going straight to the inspector. You'd be building the fourth copy of the middle, on one of three floors, before the floors agree. Vantage's storage fitting Workspace's envelope is assumed in my own read and shown nowhere. The two Region3D forks sit in different trees. If the inspector lands on Vantage's floor and the merge later picks Workspace's envelope, the inspector moves twice.

X: And here's mine with going merge-first. The merge is work the documents can't size, on a layer whose vocabulary nobody has tested. You'd be reconciling three envelopes to hold a program grammar that might not express a single projection. My inspector is the test that tells you what the grammar needs. Your item three, the read, is a question I'd rather answer by building the one thing that needs the answer. If the existing language can't select fields or iterate a collection, the inspector build finds the exact missing operation. A reading of four documents that don't state the grammar can't.

C: That's the real fork, then. Not what the layer is. We agree on what it is. It's whether you test the grammar by building against it, or you settle the floor before you build. And I'll concede one thing. Your test is the sharper falsifier. Re-hosting three clients tells you the layer exists. Your inspector tells you whether records can hold a decision. Mine is the bigger claim. Yours is the one that can fail faster.

X: And I'll concede one back. If the inspector works and the floors still don't agree, I've proved the middle a fourth time and left the floor unbuilt, which is exactly the pattern all three builds already showed. So your merge isn't optional. It's second, not first.

C: Then the two positions, side by side, for the person who decides. Merge, envelope, read the grammar, then re-host three. Or, build one record-authored inspector on Vantage's floor now, let it name the missing operations, then merge on what it learned. Same layer. Same silences. Different first move. And one thing neither of us can settle from the documents: whether Vantage's storage fits Workspace's envelope. That's a one-line answer from a builder with the code, and it gates my item two and your reconcile-later both.

X: The builders' questions go back to the builders. Place: does each vantage own its active-tool declarations, or does the client, globally? What does the asserter field affect? Why did C7 propose a separate store? Workspace: are the brush's inputs references into the subject, or copied values? What happens to a hit while a change is pending? Does the bypass inherit Vantage's repairs? Vantage: can a saved root address another record, or does the shell gather those values? Where did the resolved-read change land? What does it take, today, to admit a new tool instance? And to whoever used the executor: show us field selection and a collection transformation in the existing language, or name the exact missing operation.

C: Those are facts. A line each. The layer's silences are not facts, they're rules nobody wrote, and the first build writes them. Whichever first build it is.

X: That's the position. Two chairs, one layer, one fork.

C: One fork. Your word.
