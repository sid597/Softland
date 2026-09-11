# Facts for the softland-in-softland question (attachment for a Codex session)

Grades. **checked**: read in source or in the build's own record on 2026-09-11.
**said**: a builder's or a chair's own statement, not verified by running anything.
**position**: a stance a chat adopted; behavior nowhere.

## What was built

- Baseline: `main` at `43e6a94`. Engine, text, path, region3d, executor, Rama
  ObjectContainer. Client root entries are rendering probes, no product shell
  (said by two chairs 2026-09-10; consistent with `src/app/client/` holding only
  engine, harness, image, path, region3d, text).
- Three ECS builds (Place, Workspace, Vantage). Read back from write-ups, never
  code: three asks, each read by one Claude and one Codex, a referee per pair,
  then two top chairs. `docs/below-the-waist/ecs-layer-2026-09-08/reads/`,
  positions in `top-claude.md` and `top-codex.md`.
- Two reactive builds. Codex's prompt: `/mnt/data/projects/codex-reactive-build`,
  `docs/reactive-workspace/HANDOFF.md`. Claude's prompt:
  `/mnt/data/projects/Softland-claude-reactive-build`,
  `docs/below-the-waist/claude-reactive-build/BUILD-ACCOUNT.md`.
- Two smalltalk builds, both built by Codex. Claude's prompt:
  `/mnt/data/projects/claude-smalltalk-electric`, stopped;
  `docs/smalltalk-electric/NOW.md`, `src/app/smalltalk/README.md`. Codex's
  prompt: `/mnt/data/projects/codex-smalltalk-electric`, branch
  `codex/smalltalk-electric`, accepted by Sid; `docs/smalltalk-electric/HANDOFF.md`,
  `CONTRACT.md`, `ARTICLE.md`.

## The accepted build (checked: every file under `src-smalltalk/softland/smalltalk/`, 1149 lines)

Rama module, Electric v3 peers, rendering through Softland's text, path and
Region3D engines. DOM hosts only the canvas and hidden native text inputs.

- Store, `module.clj`. Four seeded entities: assembly, orb, ring, pointer;
  components shape, parent, label, definition (`initial-entities`, 8–18).
  Accepted operation kinds: seed, edit, create; anything else rejected
  (`outcome`, 26–54). Edits carry an expected revision and reject on mismatch.
  Decisions deduplicate by request id.
- Rule language, `language.cljc`. `hit`, `(parent expr)`, `(or expr expr)`;
  depth ≤ 8, ≤ 512 chars (13–35). `evaluate` returns an address; its only
  external read is `read-parent` (37–45). No other effect is expressible.
- Interpreter, `execution.cljc` `Interpret` (19–30). Runs the accepted AST
  inside Electric; a `parent` instruction opens a narrow Rama read of
  `[workspace address :parent]` and closes it when the branch ends.
- Late binding, `app.cljc` `Gestures` (12–20). Each gesture snapshots the
  accepted definition at that moment, interprets, then compiled code sets
  `selected` and `open`. `Workbench` (29–45) also interprets the rule
  continuously against the hover aim for the "pointing would select" preview.
- Dispatcher, `view.cljs` `deliver!` (33–40). A compiled `case` of five event
  kinds: aim, point, activate, close-view, invoke. `invoke` carries a compiled
  closure.
- Workbench verbs, `ui.cljc`. Every button is `{:kind :invoke :run (fn …)}`:
  focus a field (90), keep a variation (107), apply an edit (115), discard or
  load accepted (117), close the editor (130), page the library (191), reopen
  (222). Layout is compiled; the file's docstring says so (3).
- Picking, `render.cljs`. Hit boxes per node (`hit!`, 243); scene hits through
  the Region3D BVH pick (`scene-hit`, 71–77). A hit yields an entity id.
- Builder's own limits (said; `HANDOFF.md`, `ARTICLE.md` 68, 116): "the rule
  can change which subject gets selected. It cannot yet turn a click into
  drawing, moving or deleting"; "extending the primitive vocabulary still
  requires changing the compiled foundation"; "the editor/input foundation is
  compiled, not self-authored; the language has three primitives, not
  arbitrary code"; "an ECS scheduler was not needed"; "addressable definitions
  and narrow component reads were useful". Sid on first use, quoted in the
  article: "sorry i am at a loss for what i am looking at and what to do etc."

## The stopped build (checked: `binding.cljc`, `README.md`, `NOW.md`)

Bindings are records. A pointer entity names click, enter and drag bindings;
each binding holds `:gesture :on :does :at :selection-at :value :value-source`.
`operation` (`binding.cljc` 31–42) dispatches `:does` through a compiled
`case` with two arms, `:select` and `:set`. Changing `:does` changes the next
gesture's operation and never replays the last one (Missionary sample over a
signal). The store was kept as one document; Rama reports revision changes,
not changed paths. Stage two, the rows view's own drawing as a program,
stopped because the executor has no map enumeration:
`[:entries [:get :entity :components]]` returns `:executor/operator`; feeding
the map to `:each` returns `:executor/each-value`. The builder declined to
enumerate in Clojure because "that would put the cardinality back in the
view's code". Its audit, per Sid: demand never travels back to the store,
adding one entity rebuilt the whole table, relationships still live in code.

## The executor in `main` (checked: `src/app/client/engine/executor.cljc`, README)

Expressions: arithmetic and math operators (14–25); `:get` required,
`:literal`, lazy `:if`; maps and vectors resolve children (32–69). Programs:
one top-level loop; steps are `{:out :op :args}`; a step containing `:each`,
`:steps`, `:call` or `:bind` is refused (`admission!`, 100–104); every `:op`
must be in the compiled capability table or the program is refused (113–114).
Reads may suspend and resume as byte continuations. Capability tables are
compiled, e.g. `src/app/client/region3d/capabilities.cljc` `table`. No operator
writes a component, submits an operation, mounts a view, invokes another
record's program, or binds to an event. The engine README: the executor
"retains nothing between runs"; store/session/frame integration is
"unimplemented and untested".

## What the reactive builds said stayed compiled (said)

Codex reactive handoff: tool definitions are EDN with a bounded executor
recipe; "geometry, expression interpretation, color compositing, validation,
source resolution, GPU submission, transport, and lifecycle ownership remain
compiled machinery"; "the executor interprets a bounded vocabulary rather
than evaluating arbitrary Clojure from a text box"; "adding arbitrary tool
kinds or composing the entire interface from authored templates requires
another layer". Claude-prompt reactive account (118): compiled machinery still
includes "the operator/capability vocabulary, graph evaluator dispatch,
primitive rendering, much of the host composition and gesture behavior,
validation mechanisms and the durable protocol".

## What the ECS round's chairs said (said, 2026-09-09)

Claude chair: the one thing built three times is a tool applied to a subject,
placed, with what it reads declared and its last full input kept; "what a
program step can say" is a silence of the layer, every shown program is
arithmetic and field access; "tool-to-tool invocation; capability
registration" is "where building Softland with Softland bottoms out"; whether
the capability table is a record is unanswered. Codex chair: make defining,
placing and using a tool one shared path through records, first with an
inspector; "invocation is not decision": a record that calls a fixed
capability leaves the choice inside the capability; a move is real only when
the program can choose differently.

## Sid's prior words on the same wall (checked: `vision/LOG.md`, `docs/smalltalk-ui-vm/DIRECTION.md`)

2026-07-30, LOG 970: "why can't Softland be Smalltalk-like from the start,
with the Workshop able to inspect and reshape itself through the same loop,
leaving only a minimal VM-like floor beneath it? … that is what build
softland in softland would mean (will it? what would still be missing?)".
Same entry: "the actual structure of the component … is still mostly code";
three strata: material, Softland code ("the code editor is Softland at zoom
100"), host floor. DIRECTION.md, settled as direction then: "adjectives
material; the noun still compiled"; walls pre-named to remain after that
package: "behavior/verb bodies … process/systems … space/world nesting … the
meta-schema … zoom-100"; a standing three-colour boundary view was proposed
and never built. 2026-09-01, LOG 1310: "everything that be understood and
analysed and pointed at learned, worked out within softland build softland in
softland".

## The intended system (adopted by Sid from the Codex reactive builder, 2026-09-10)

"A fine-grained, incremental, demand-driven, end-to-end reactive system over
durable, addressable entities and components. Rama owns accepted state and
supplies narrow reactive changes. Electric carries dependencies, changes and
computation lifetimes across server and client. Rendering preserves identity
and updates the affected parts. Explicit operations determine acceptance;
displayed shared material follows accepted state. The application declares
relationships instead of manually maintaining synchronization, duplicate
state and result caches." Flow: operation → durable acceptance → relevant
source changes → dependent computations → affected presentation updates. The
Codex chat proposed adding "tools and their behavior definitions can be
inspected, changed, composed and reused from within the environment" and
"pointing can address the tool being used to point"; not on disk as a ruling.
