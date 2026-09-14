# Inland integration — fact base (session of 14 September 2026)

Gathered context for the next session, so it can start as chat. Sources are
named so any claim can be re-checked. Nothing was run. Marks: **checked** means
a read-only hunter returned file and line, or a folder map states it;
**derived** means one session's reasoning from checked facts; **assumed** means
neither. Sections 1–5 are facts. Section 6 is one session's derivations and may
be redlined. Section 7 is the questions carried forward.

## 1. What was read

Docs, by the session itself:

- `docs/builds/inland/integration.md` (settled starter, 13 Sept),
  `docs/builds/inland/intended-design.md` (whole), `docs/builds/inland/README.md`.
- `docs/decisions.md` lines 33–190 (Rama is truth; the render boundary; tools
  are records over a vocabulary).
- `docs/carry-on.md` §2.3–2.5 (atomic unit; raw and native material; history
  and layers), §3 (views that answer questions), §5 (building Softland in
  Softland), §6 (foundation and editable layers), §7 (a world that responds
  coherently), §8 (the research loop).
- `vision/LOG.md` lines 1346–1374 (two entries of 7 Sept, the entry of 13 Sept)
  and the 30 July entry's three-strata and sequencing paragraphs (lines 940–993).
- `history/docs/build-softland-in-softland/NATIVE-CAPABILITIES.md` (whole).
- Maps: `src/app/server/README.md`, `src/app/server/{rama,ingest,worn,page}/README.md`,
  `src/app/client/README.md`, `src/app/client/engine/README.md`,
  `src-inland/README.md`, `src-inland/softland/inland/README.md`,
  `resources/inland/README.md`, both `CLAUDE.md` under `src/app/`.

Code, by three read-only Opus hunts (server, Inland, client) reporting facts
with file and line; the load-bearing ones are in §3–§4. Not read:
`src/app/server/env.clj`, `test-inland/`, and `bin/land` / `bin/inland` beyond
what the hunters quoted.

## 2. The product side

From `docs/builds/inland/integration.md` (settled 13 Sept; not a decision in
decisions.md; the first workpiece has to prove it):

- Aim: a question acquires the representations and actions it needs while we
  work on it. The integration is the work.
- First workpiece: how an accepted edit becomes a visible change, in each
  system, traced by reference. Inland: gesture, Electric recipe, proposal, Rama
  admission, owned ProxyState, Electric recomputation, realization. Canonical:
  UI action, event into Rama, materialized state out, render boundary.
- Forced first step: repo material referenced as facts (a passage at a
  revision, a function at a revision, a decision's clause, a check and its
  result). Not a visualization.
- A tool made inside must: (1) refer to the actual things and show the result
  or show that it is stale when they change; (2) present them, accept
  interaction, use the relevant computations and actions; (3) stay inspectable
  and changeable after first use (today: an EDN record editor, and the tool
  says so); (4) reference at the grain of meaningful work, never file grain;
  (5) no invented components: every component is a reference to a thing with
  a revision.
- Requirements are recorded inside the medium as unanswered work (a miss is a
  fact with a subject and a time), extracted by a scribe chair, adjudicated
  at settlement. Not by Sid.
- Divergences the workpiece should expose: one truth or two (Inland's isolated
  Rama against "Rama is truth"); one vocabulary or two above the waist (rows,
  components and the ECS layer against facts and definitions).

Sid's words, verbatim from `vision/LOG.md`:

- 30 July, the three strata: "Re-evaluate the ladder and boundary map using
  three strata — material, Softland code, and host floor — and distinguish what
  is merely still code after this package from what must genuinely remain
  outside the land forever." The sequencing: "we will get to the code editor
  and lower zoom level once i have built on top of the current one ... when
  the need arises we will do more abstraction out and bring the zoom level 100
  inside as well ... we will build all the strange loopy loops".
- 7 Sept (voice): "the first thing that client from the base layers is a lens
  planned code base itself ... and then from there, I could use Softland to
  modify the codebase itself".
- 13 Sept: "maybe the best thing we can do is have like all the affordances to
  be able to run all of these in context nd we don't have to pre bake anything
  ... we have just too many options and a clear instance of implementation
  that needs to exist and work out".

From `docs/builds/inland/intended-design.md` (Inland's construction model,
authorized 11 Sept):

- Three strata (§2): material, edited in place live; Softland code (the five
  operations, the painters, the capabilities the body vocabulary names),
  addressable at zoom 100, edited and rebuilt with a migration; host floor,
  versioned dependencies.
- A fact is thing, attribute, value, asserted by whom, when, in which layer. A
  thing is an id. Kinds are attributes. No is-a in the floor (§3.1).
- A definition is name, pattern, body. Views and rules are the two ordinary
  consumers. Composition is by name, resolved at each use. Nothing is copied
  from a definition into a thing (§3.2).
- Events can be facts. An admitted request no rule answers stays visible as
  unanswered work (§3.3). Layers and context: nearest active layer wins;
  candidates are layers; promotion is an admitted change (§3.4).
- A definition establishes one of three things: a maintained conclusion
  (derived, never written), an asserted change, an activity (§3.5).
- The floor: store, resolve, match, apply, locate, paint. No nouns (§4). Three
  tiers of state: local signals, session facts, accepted facts (§5).
- What breaks it (§12): tool-specific dispatch in the host; a capability per
  effect; an active-tool slot; nouns in the floor; a general program language
  in bodies; resolution without context; copying a definition into a thing;
  a shell.
- Open (§13): body vocabulary members; session layer retention;
  competing-assertion policies; match cost at scale; where activities run;
  migration when the definition shape changes; permanent Electric adoption.

From `docs/decisions.md`: Rama is truth (events in, materialized state out, no
side-channel state). The render boundary: the engine is compiled code that is
physical or holds uncommitted state; geometry generators are data; rows are
truth and derived rows; the dirty check is the row's revision; dead means wrong
form for the waist, called or not. Tools are records over a vocabulary: a
recipe is a record, one executor with several runners, nothing in a record is a
program, foreign code only as a value in a slot with a declared output.

## 3. Checked facts, canonical `src/*`

### 3.1 Server storage and identity

- Object key = sha-256 of the source ref, a NUL, and the source hash of the
  whole text: `src/app/server/rama/object_container.clj:216`. Container row
  and revision row: `object_container.clj:108` and `:113` (a revision has
  parent-revision-id, content-text, content-hash, order-key, created-by,
  event-id). Revision ids: `rev:<object-key>:<request-id>` for edits (`:246`),
  import-derived otherwise (`:253`).
- "Typed rows" are defrecords stored in PStates; depot envelopes are plain maps
  (`relation_kernel.clj:245`).
- Decision row fields: `object_container.clj:77`. Accepted decisions have
  reason nil and errors empty (`:547`). Rejections carry a reason keyword and
  errors, for example `:edit/stale` with the client sequence (`:1701–1714`).
  Relation decisions carry the envelope actor (`relation_kernel.clj:265`).

### 3.2 Repo ingest lane

- Clojure forms are cut without evaluation. The unit's local id is the binding
  name, deduplicated in file order; the unit id is
  `du:<object-key>:clojure-form-v0:<name>`:
  `src/app/server/ingest/clojure_adapter.clj:159`, `:294`.
- The object key for code folds the git blob sha and the whole file's text
  hash: `src/app/server/ingest/code_import.clj:170`, `:379`. Any edit anywhere
  in a file re-mints the ids of every form in that file.
- Continuity across commits is reconstructed afterwards as `:supersedes`
  relations, for named units only, by name match and form-text hash:
  `code_import.clj:160`, `:251`, `:405–428`.
- Source refs: `git-blob:<sha>` for code; `git-commit:<sha>` for commits,
  imported through the markdown adapter; the file path for watched markdown;
  `chat:<sha>` object keys for transcripts (`transcript_identity.clj:11`, `:51`).
- The ingest epoch is a process-local counter, not a revision, not persisted:
  `src/app/server/rama/ingest_epoch.cljc:1–19`.
- Ingest is explicit and never on boot: `bin/land` (`ingest)` case),
  `door/server_jetty.clj:1634`. `bin/land:29–35` deploys five module vars:
  object-container, transcript-ops, relation-kernel, trail-view, face-arsenal.

### 3.3 No path from stored material to running code

- No `eval`, `load-string`, `load-file`; the `sci` dependency is never
  required; every `read-string` is `clojure.edn/read-string`; the only
  `requiring-resolve` calls take literal symbols written in source
  (`episode/cascade.clj:45`, `episode/machine_cut.clj:516`). No multimethods on
  stored kinds.
- Worn's "compile" reads EDN and validates it against a code-owned grammar,
  returning `{:valid? :errors :grammar :material}`:
  `src/app/server/worn/facet_engine.cljc:69`, `:145`. Validator predicates live
  in code specs (`text_body_material.cljc:30–48`).

### 3.4 Worn material

- A facet grammar is version-keyed material keys plus validators in a
  code-owned spec (`text_body_material.cljc:30`). An instance is a synthesized
  child master per facet and subject (`facet_master.clj:489`, `:535`, `:558`).
  The active pointer is a separate block container whose revision content is
  an activation event (`facet_master.clj:37`). `activate!` (`:273`) checks that
  the revision exists, belongs to this master, and compiles (`:288–305`); it
  does not validate the event's kind, scope or grounds.

### 3.5 Where "which view or action applies" is decided

- Face-to-projection map and projection registry:
  `src/app/server/page/face_projection.clj:1798`, `:1776–1796`; dispatcher
  `:1853–1878`.
- Gesture to verb: `worn/binding_material.cljc:358` (`resolve-binding`), pure,
  instance over master over floor within a depth, over material rows.
- Verb table, declarations only, no dispatcher: `page/verb_registry.cljc:23`.
  Act-kind case: `page/matter_room.cljc:174`. File-kind case:
  `ingest/ingest_watchers.clj:104`. Source-format case: `rama/trail_view.clj:62`.
  Cascade rows: `episode/cascade.clj:34–36`.

### 3.6 What page returns; the HTTP surface

- Conversation data-context keys: `face_projection.clj:176–194`. Material
  portal keys (identity, placement, recipe, masters, bindings, wearers,
  deviations, activation-history, experience, lint, truncation, why, blast,
  history, recovery, errors, query-plan): `material_portal.clj:958–998`.
- All routes are POST under `door/server_jetty.clj:1389`; no GET, no browser or
  Electric handler (`:1611`). Routes: relation/assert; matter-room open,
  deviate, activate, rollback, say; facet-master drill; episode utterance
  (server-sent events), block-birth, geometry.

### 3.7 Push and dependencies

- No websocket, no foreign proxy, no subscribe, no missionary require in server
  source. One server-sent-events lane streams a model turn
  (`server_jetty.clj:101`, `:346`, `:490`). The epoch atom has no subscriber
  (`ingest_epoch.cljc:19`).
- Electric is only in the `:inland` alias of `deps.edn`. The `shadow-cljs.edn`
  main build is `:inland` (entry `softland.inland.boot`); the other builds are
  harnesses.

### 3.8 Client

- Executor record `{:program {:steps :each :return} :roots}`, step
  `{:out :op :args}`: `src/app/client/engine/executor.cljc:334`. Expression
  dispatch: `:literal :get :if` built in, else an operations table of
  arithmetic and comparison ops (`:15–26`, `:44–71`). Step ops are
  capability-table lookups (`path/construction.cljc:21–36`,
  `region3d/capabilities.cljc:48–69`); an unknown op is refused (`:113–114`).
- No pointer or keyboard handling, selection or hit state outside `harness/`.
  Mesh pick `region3d/scene.cljc:965–982` returns route, object-id, point,
  normal, t, triangle; harness and tests only. Text queries
  `text/layout.cljc:1487` (point to source), `:1333` (source to screen),
  `:1372` (range to rects); harness and tests only.
- No network beyond asset fetches; no require of any server namespace.
- Path record keys `:path/material-id :path/revision :path/source :path/paint`
  (`path/records.cljc:86–92`); draw item `{:id :path/material :container}`
  (`harness/path.cljs:57`). Named placement diffs `{:upsert :remove :order
  :groups}` return `{:state :reran :affected :drop-packs}`; only supplied ids
  and their dependents are re-prepared: `path/placements.cljc:95–154`.

## 4. Checked facts, Inland `src-inland/`

### 4.1 Records and address

- A definition is a map with `:name`, optional `:pattern`, and
  `:body {:steps [...] :return ...}`; for example `containing-object-or-self`
  at `resources/inland/seed.edn:18`, and `editor-view` with pattern
  `{:demand :view :reads [{:session "tab" :equals "instrument"} {:session "inspecting"}]}`
  at `:64`. No `:id`, `:version` or `:workspace` key in any seed record.
- The full address is composed at admission: workspace as routing
  (`module.clj:130`), `layer/name` row key (`total.cljc:191`), revision
  assigned by admission (`module.clj:41`); version key `[layer name revision]`
  (`total.cljc:186`).

### 4.2 Applicability

- The index is a Rama PState `$$index` (`module.clj:134`) with buckets
  `event/<kind>` and `demand/<kind>` computed at admission (`total.cljc:170–182`,
  `module.clj:109–113`) and read through a proxy. `Dispatch`
  (`execution.cljc:218–235`) and `Query` (`:194–216`) return
  `{:support [name revision] :value ...}` per applicable definition.
  Conditions (`:163–184`) read a session cell, a context path, or an addressed
  field; a blocked (pending) value propagates rather than reading as false
  (`:176–179`).

### 4.3 Admission

- `outcome*` (`module.clj:26–99`) checks: envelope and actor in
  `#{"sid" "resident" "executor"}`, stated not to be authentication
  (`:29–30`, `:46–48`); expected revision for put, promote, remove and
  delete-override (`:50–51`); activity lifecycle (`:52–53`); parse under a
  24000-character cap and structural row errors (`total.cljc:141–168`);
  machine-behavior policy for resident writes to base (`:58–59`); promotion
  source revision (`:63`). No reference resolution; no workspace ownership
  check.
- Rejection value `{:request-id :name :layer :operation :status :reason
  :revision}` (`:19–24`). Acceptance writes `$$rows`, `$$versions`, `$$index`,
  and `$$decisions` at `[workspace request-id]` (`:170–189`); `$$workspaces`
  only on an accepted seed (`:192–194`). Revision is the current row's
  revision plus one (`:35`, `:41`); delete-override restarts the counter
  (`:6–7`, `:67–69`).

### 4.4 Change seen without asking

- `store/request-proxy` calls `foreign-proxy-async` on a keypath with a
  callback (`store.clj:88–94`); the callback emits the proxy's value into a
  flow (`:114–120`); the first value comes from the acquisition future
  (`:122–130`); cancel closes the proxy (`:131–135`). Values are tagged
  `:failed`, `:absent` or `:value` (`:104–107`). No polling.
- `ReadStatus` wraps that flow as an Electric server input
  (`execution.cljc:18–26`). `Field` (`:79–100`) issues narrow reads per layer
  and returns the value, nil for absence or tombstone, or a tagged pending or
  failed map.

### 4.5 The floor, as compiled vocabulary that authored records name

- Step ops `:value :read :session :call :index :query :derive`
  (`total.cljc:14`); expression forms `:literal :get :if :or :and` (`:37–41`);
  finite leaves (`:17–24`): str, pr, count, first, rest, empty?, contains?,
  concat, distinct, conj, assoc, dissoc, merge, keys, vals, nth, lookup, join,
  not=, boolean, take, drop, parse; plus the engine executor's arithmetic leaf
  functions merged in (`:16`).
- Effects `:session :admit :event :visibility :activity :cancel`
  (`total.cljc:117–123`, `session.cljs:52–63`). Paint kinds
  `:text :path :hit :input :scene :repeat` (`paint.cljc:103–113`); shapes rect,
  ellipse, line (`:51–55`). Bootstrap addresses: `base/world`,
  `base/admission-policy`, workspace `workbench`, default layers `["base"]`,
  seed ids, port 8127 (`app.cljc:103`, `module.clj:155`, `server.clj:77,88`,
  `store.clj:17,86`).

### 4.6 Realization

- Paint occurrence maps (kind, id, order, face, text, box, size, color; shape,
  fill, stroke; event; cell, label; shapes, options; items, definition,
  bindings), validated under a 256-occurrence budget with distinct ids
  (`paint.cljc:62–95`, `:97–114`). `nodes.cljc:21–53` wraps each occurrence as
  an Electric input over `render/node` and calls the render mutator.
- `render.cljs:10–23` requires `app.client.engine.{device,color,transform,
  compositor,leases}`, `app.client.path.renderer`,
  `app.client.text.{fonts,layout,renderer}`,
  `app.client.region3d.{renderer,scene}`. A changed path node is pushed as a
  single-key upsert `{:upsert {id {:path/material material :container 0}}}`
  through `path-renderer/push!` (`render.cljs:288–295`); teardown pushes
  `{:remove #{id}}` (`:240`). `placements/change` is not called from Inland
  directly.

### 4.7 Pointing and opening a definition

- Click, then `render/point`, then `target-at` (authored hit boxes by order,
  returning the registered action map) or `scene-hit` (Region3D picker,
  returning the object id), then `deliver` with a gesture id and point
  (`render.cljs:183–200`, `:75–91`). The hit action carries the authored
  `{:kind :subject}` plus `:definition` (the view's name) and `:occurrence`
  (`paint.cljc:106`).
- `session/deliver!` (`session.cljs:71–79`), then `emit!` (`:30–34`, a single
  event slot), then `app/Events` (`app.cljc:31–60`), then `Dispatch`. The
  authored `point-rule` (`seed.edn:22`) resolves the target through
  `pointing-target` (`:20`) and writes the selection, inspecting, draft and
  tab cells.
- "Open definition" has no dedicated function. Rules write the name into the
  `inspecting` cell; `editor-view` reads it back and resolves through
  `Resolve`, `ResolveLayers`, and `Read` on `$$rows`, or `$$versions` when
  pinned (`execution.cljc:55–77`).

### 4.8 Versions and staleness

- Ordinary references carry a name only. Pins carry `{:layer :revision}`
  (`seed.edn:36`, `:78`) and read `$$versions` (`execution.cljc:62`, `:75–77`,
  `:85–86`).
- Admission checks the expected revision of the edited row (`module.clj:50–51`);
  drafts capture `:base` on load (`seed.edn:22`, `:28`, `:30`); promotion
  checks the candidate's recorded base revision (`:63`).
- Nothing detects that a referenced definition changed after the reference was
  made. Support `[name revision]` is recorded at answer time
  (`execution.cljc:198`, `:211`, `:233`) and attached to admissions as
  `:invocation` and `:basis` (`app.cljc:52–53`, `module.clj:61`). The word
  `stale` appears only as GPU lease bookkeeping.

### 4.9 Two clusters, two runners

- Inland connects to its own cluster: conductor 1997, zookeeper 2217, root
  `inland-electric` (`store.clj:25–36`), provisioned under `.inland-runtime` by
  `bin/inland:63–74`, one module `softland.inland.module/material`
  (`bin/inland:24`, `:140–142`). The jar packages `src-inland` plus
  `src/app/client/engine` (`bin/inland:119`). No reference to any canonical
  module or PState in `src-inland` or `bin/inland`.
- Inland never calls `executor/run`; its only use of the engine executor is the
  `operations` table (`total.cljc:16`). Expressions are evaluated by
  `total/expression` (`total.cljc:26–45`); steps by the Electric-hosted
  `execution/Step`.

### 4.10 Native capability log

`history/docs/build-softland-in-softland/NATIVE-CAPABILITIES.md` records what
was compiled during the build, each row classified (required primitive,
reused physical primitive, transport configuration, or correctness repair)
with the authored construction that pressed on it: Rama admission and version
writes; ReadStatus, layer and name resolution, indexed pattern reads, named
calls; finite collection operations; EDN parsing; demand-local support union
and finite closure; session effects; the repeated-step owner; the external
executor with a durable claim; renderer adapters and hidden input;
font-provider disposal; input scissor and render-failure containment; the
Jetty message limit; reactive completeness and one-shot event consumption.
After the first loop: surface lifetime, input scissor delivery, step
scheduling, provider envelope parsing. None is classified as convenience.
Electric interfaces used are listed there; dependency internals modified: none.

## 5. Discrepancies noticed

- A thing is an id (intended-design §3.1) versus a name in a layer
  (implementation, §4.1 above).
- Requirement (1) "shows that it is stale" versus no staleness detection: live
  references re-resolve silently, pins are never re-checked (§4.8).
- Requirement (4) on grain versus canonical's file-grain object key for
  form-grain units (§3.2).
- "One executor, several runners" (decisions.md) versus Inland's own evaluator
  and Electric step runner, sharing only leaf functions with the engine
  executor (§4.9).
- Accepted decisions in canonical carry no reason or rule id (§3.1). The
  material portal's `why` key was not examined.
- The intended seed (intended-design §9) lists an `unanswered-view`; the
  unanswered handling seen in code is the compiled fallback (`app.cljc:59–60`).
  Whether an authored unanswered view exists in the actual seed was not
  checked.

## 6. One session's derivations (14 Sept; not rulings)

Corrections to the mental model that framed the walk:

1. Rama-or-code is two of three strata, and a thing can sit in two with a
   correspondence. The Rama row about code is a reference to a form at a
   revision, not the code.
2. The store ignores the asker. The asker's context does the resolving, and
   that context is facts (a session layer). Canonical's instance, pin, shared
   active, floor precedence is the same idea in another vocabulary.
3. The asker does not compose. The view definition composes. Reads carry
   status. A page from several reads is not a snapshot, in both systems.
4. Things do not know what X means. Rules keyed on facts and events do. An
   unanswered event is a fact.
5. Updates are not sent. Views are maintained conclusions whose support
   changed.
6. Tool updating tool means a small fixed floor, live material above it, and
   (derived) an owned activity that rebuilds the floor when the floor is the
   thing being changed: the draft of a form at a revision as material, the
   rebuild as an activity of the same shape as `ask`, the running revision as
   an admitted fact. Nothing in a record becomes a program.
7. "The thing should be in the database" today has two databases and two
   vocabularies. The forced first step is making canonical units referenceable
   as facts, not moving code into Rama.

Positions with reasons (derived):

- The proxy mechanism (§4.4) depends on Inland's schema only through the
  connection map and the value tagging. It is the most portable piece Inland
  has.
- The two-cluster split is a launcher fact, not an architectural one.
  Canonical already reads across modules through mirrors
  (`src/app/server/rama/README.md`).
- Leaning against copying canonical units into Inland's module as facts:
  copying reproduces a second account of identity, which the starter names as
  a failure of generality.
- Canonical's verb registry has the "capability per effect" shape the intended
  design names as what breaks the model. Its binding rows are already
  material.
- Assumed, from Rama documentation and not from this repo: a module update
  preserves PStates under migration rules.

## 7. Questions carried forward

For this instance, the first workpiece:

1. What is the thing when the material is a function? The stable identity is
   the name with its supersedes chain; the blob-scoped unit is a version of
   it. Who computes that mapping, the ingest lane or the reader?
2. One truth or two, concretely: copy canonical units into Inland's module as
   facts; give Inland's read a second kind that proxies canonical PStates; or
   add a facts-shaped projection PState on the canonical side that the same
   proxy watches. The latter two need one cluster.
3. What is the revision in a reference? Canonical references carry a blob or
   commit. Inland references carry a name, with a revision only at answer time
   (support) or in pins. Is recorded support enough for "traced by reference"?
4. Where does accepted-edit-to-visible-change break in canonical? At both
   ends: no client event in, no change out toward a view. Which end first, and
   is the proxy the answer at the second?
5. Which compiled decisions are material in the wrong form? Binding rows are
   material; verbs and projections are code. Does the first tool need a
   projection to become a definition?
6. What does the tool say when it is stale? A fact the store asserts, a
   conclusion derived from support versus current, or a pin comparison?
7. Which runner runs the workpiece's body: the engine executor (capability
   tables, continuations) or Inland's Electric step runner (tracked reads)?
   They share leaves and nothing else.

Buildups:

1. The rebuild as an activity: owner, budget, input basis, admitted outcome,
   when the thing changed is Softland code.
2. Migration when the definition shape changes across a rebuild
   (intended-design §13, open).
3. The cost of match at tens of agents per person: an index bucket per kind,
   then per-candidate condition reads.
4. The second domain (biology): what had to change, and was any of it a second
   account of identity, revisions, references or authored tools.

## 8. Housekeeping at session end

- `docs/builds/inland/README.md` had an uncommitted stray `Y` before its title
  hash at session time. Not touched.
