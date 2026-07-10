# The Framework Road — faces made of Softland

2026-07-10 · synthesis of three independent Fable runs over Sid's 2026-07-10 vision message (verbatim: `vision/LOG.md` 2026-07-10 entry). **v2, 2026-07-11:** mechanism re-grounded against the shipped render source after two independent Fable source-reviews, each claim re-verified at source by the authoring session (`ui_primitives.cljs` · `rect_tree.cljc` · `runtime/render.cljs` · `trail_face/wiring.cljs` · `electric_flow.cljc` · the law register). v1 aimed the mechanism at the wrong layer (Electric components); the destination, adjudications, grammar, guard, and step ladder survive unchanged. Status: ROAD (direction-grade synthesis; becomes a package contract on Sid's go). Vocabulary is working scaffolding — names finalize by recurrence, and the naming is Sid's.

## Where the three runs converge — the invariant core

1. **No import boundary.** The design output is born in the land's own runnable vocabulary. Never render-then-componentize; there is no moment where a dead picture becomes alive, because nothing was ever dead.
2. **The load-bearing split.** *Primitives* — pure cljc scene builders (fns → rt-node trees; the vocabulary; the slow lane: real code, hot-reloaded in seconds). *Assemblies* — pure EDN data describing arrangement (the fast lane; **a face is an assembly**). *One interpreter* — a pure function that walks assembly data and instantiates primitives through a registry. Leaves are code; composition is data. Every live-medium system in history converged on this split (Smalltalk classes vs morph-compositions, HyperCard runtime vs stacks, Retool engine vs app-JSON) because author-at-runtime is only tractable when structure is data and only vocabulary is code.
3. **Faces are land objects.** Stored in Rama with name, belief line, provenance (birthed-by conversation), lineage (`based-on` / `supersedes` / `dead-end`), `asserted-by`, status (candidate / worn / retired). Sid spec'd this himself on 2026-07-05: "view-specs as assertions … a spec in itself that I can change later or fork or inherit."
4. **D-001 governs growth at both layers.** Primitives minted only by gap-fill when a face in use breaks against the vocabulary; the grammar formalizes only after several faces share it. The framework precipitates from worn faces, never from the abstract.
5. **The destination is a loop, not a feature.** Conversations produce faces; faces render conversations. The AI writes arrangement-data through the lawful path; wearing — not argument — decides which faces survive.

## What the substrate actually is (read at source, 2026-07-11)

- **Electric is the truth bridge, nothing more — and that is full-strength use, not underuse.** `electric_flow.cljc` (604 lines, five `e/defn`s, one `dom/div` in the whole app) renders two DOM nodes (preview overlay + WebGPU canvas), boots the Missionary loop, and runs the bridges: `e/watch` server atoms → fill `!remote-*` client atoms (R4), plus request atoms flowing the other way (the trail-face `!trail-request` → pull → `!trail-data` loop). Electric's compiler earns its rent on the network — a managed, reactive server→client value channel nothing else provides. ~30 lines of `Watch*` bridges replace thousands of lines of hand-rolled subscription plumbing.
- **The render path is pure Missionary + plain cljs.** `runtime.cljs` is an `m/join` of event consumers plus one render consumer; `runtime/render.cljs:81` builds ONE `<world-snapshot` `m/latest` over the derived flows and watches, samples it on RAF (`:121-124`) with an `identical?` skip, then diff-uploads to GPU pools (keyed pool diff, `:135-151`). Electric appears nowhere in it.
- **The primitive vocabulary already exists, as pure functions.** `ui_primitives.cljs` is headed "COMPONENT LIBRARY (pure fns → rt-node trees)" — panels, cards, badges, list items, tabs, scrollbars — over shared design tokens (`components.design-tokens` cljc) and `rect_tree.cljc`: a layout engine (`resolve-layout`, `:layout {:direction :column/:row :gap :padding :align :auto-height?}` — `:row` verified present at `:81`), text wrapping (`wrap-line`), clipping, hit-testing, and event dispatch (`:actions` + innermost-first `dispatch-event`).
- **A worn face already implements the whole loop this road describes.** `trail_face/` is pure cljc ("(data, geometry) → data") except one wiring file whose header states the seam law in the source's own voice: *"this file touches NO server names — the Electric pull happens in electric_flow/file_viewer; this glue only moves data between client atoms"* (S1/S2).
- **The desire path is already worn.** `rect_tree.cljc:3`: "Everything is a rect. The tree replaces scattered compute-*-rects fns with one generic walk." The codebase took the first step toward the interpreter on its own; the assembly walker is the second step of a walk the code already started.

## The five nouns, re-grounded

- **Primitives** — pure cljc functions returning rt-node trees. Half the vocabulary already exists (`ui_primitives.cljs` + `trail_face/` cards/lanes/threads as proto-primitives). Step 1 is extraction and gap-fill, not invention.
- **Registry** — a plain map `{keyword → builder-fn}`. Persisted form: keywords/addresses in Rama (registry entries as code-as-addressed-material — D-009's own note names them "the first candidate"); runtime form: the loaded map.
- **Interpreter** — a pure function: compiled assembly × data-context → rt-node tree, living in the derived-flow layer and slotting into the `<world-snapshot` `m/latest` chain like every existing pane. Written in `.cljc`, so assembly + fixture → golden rt-tree snapshot tests run JVM-side with no browser — the block-kernel goldens discipline applied to UI.
- **Assemblies** — pure EDN in Rama (the grammar below).
- **Projections + the one artery** — server-side Clojure shaping Rama truth, delivered by ONE generic face-pull keyed by the assembly's declared data contract (its `:bind` paths + address). This replaces today's per-face hand-wired capillaries (`!trail-request`/`!trail-data` + a wiring file per face) — the one place real dis-harmony exists today, and the framework's direct fix. Electric's role *grows* with every face, at the seam where it wins.

**Demoted from v1:** the e/fn-values mechanism (U4/U5, the archived Slack thread). e/fns-as-values stays true and recorded in the skill, but nothing on this road's critical path depends on it — the registry holds plain fns, recursion is plain-function recursion, and there is no siting question in a pure-data client layer. The "one spike from law" platform risk v1 named does not exist on the real substrate.

## The seam law — and why faces are not Electric programs (permanent answer)

**Law (promoting `wiring.cljs` S1/S2 to framework constitution):** Electric owns truth transport (server↔client, into atoms). Missionary owns the frame path. The rt-node tree is the boundary object. Electric never enters the frame loop; the frame loop never touches server names. Harmony between the two systems arrives as a **shared contract** (the assembly's `:bind` declaration), never as shared code.

The receipts, from the verified register:

- **L15 — the experiment was run and left a corpse.** The dormant gpu-mount bridge pointed Electric's DOM-shaped mount contract (`insert-before` = *move this identity handle*) at GPU slot pools: a 91-frame rotate at n=100 ended with 16,750 active slots for 100 entities. The diff algebra stayed correct while DOM ownership semantics corrupted the pool. L15's own prescription: if Electric ever drives the GPU boundary, it consumes the six diff ops directly into a purpose-built store — never transplanted dom machinery.
- **L16 — differential economics punish face-shaped updates.** change/append/tail-shrink are linear; any reorder enters the permutation regime (~×130–700 per decade); no cheap no-change path. Faces reorder (sorts, windowed scrolls, z-order).
- **L8 fence — frame coherence is by construction today.** Whether Electric's compiled DAG settles atomically is explicitly untested; a scene graph is diamond city (parent/child geometry co-vary). One `m/latest` world sampled per RAF cannot tear mid-frame. Screenshots-as-addresses and hit-testing assume the screen is never a torn state.
- **L13 — `try` is TODO inside `e/defn`.** The layer that renders user- and AI-authored faces needs error containment more than any other layer; the pure interpreter wraps every assembly in plain-Clojure validation and renders an error card. In-graph it would have nothing.
- **The airlock.** Everything left of the atoms is allowed to be asynchronous (network, Rama round-trips, LLM latency, reconnects); everything right is synchronous and frame-paced. Delete the seam and network scheduling lives inside the frame budget; keep it and the land keeps rendering on last-known truth through a reconnect, for free.
- **The soul reason, decisive even if every platform fact were fixed upstream:** faces must be *data*, so the thing that renders them must be an *interpreter*. AI-designed faces stay arrangement-data flowing through the lawful center loop — validated, provenanced, error-carded. Faces-as-Electric-programs would be code generation into the compiled substrate: the Regime-2 gate, deliberately uncrossed. Data faces are also what makes faces *material*: JVM-testable, agent-legible (the View-3 law applied to the UI itself), diffable, forkable. A softland/dom that re-fused resolution, reactivity, and transport would rebuild the DOM's convenience and pay the DOM's price: opacity. The split is the map.

**The harmonization ladder** (each rung D-001-gated, pre-named):
1. **Now:** Electric as bridge; assemblies compile at wear-time to rect-tree builders.
2. **This road:** Electric as the one generic artery — assembly `:bind` contracts replace hand-wired capillaries.
3. **Gap-3 (measured, pre-registered in R3's sunset note):** Electric incseq diffs enter the render side as the *invalidation signal* for the measured-safe shapes (change/append/tail-shrink). Live chat is append-only — the pre-qualified first consumer.
4. **On a worn face breaking against rebuild cost (L6):** point-writes consuming the six diff ops into the retained store, per L15's own prescription. The true "softland/dom" — arriving with evidence instead of as architecture.

## Grammar v0 — five keys, one guard

`{:prim :props :children :each :bind}`. A face, concretely:

```clojure
{:assembly/name   "outline-face"
 :assembly/belief "belonging is indentation; time is the scroll"
 :root
 {:prim :stack :props {:dir :v :gap 12}
  :children
  [{:each [:turns]                         ; descend data context per item
    :template
    {:prim :turn-card
     :props {:speaker {:bind [:speaker]}}  ; :bind = path into current context
     :children
     [{:each [:blocks]
       :template
       {:prim :block-card
        :props {:kind {:bind [:kind]}}
        :children [{:prim :text-run :props {:value {:bind [:text]}}}]}}]}}]}}
```

- `:each` is a `mapv` inside the compiled builder — plain function iteration, no Electric. Items still require **stable ids** — for keyed GPU pool-diff stability (`pool/keyed-diff-update-pool!`), not for `e/for-by`.
- **Performance truth (corrected from v1):** the atom path is governed by L6 (coarse invalidation — any change rebuilds the visible scene; sub-millisecond at conversation scale, and the RAF gate already passed at corpus scale) + R3's `identical?` skip + keyed pool diffs + dirty-rect scissoring. L16's append-only happy path applies only if Gap-3 (rung 3) is ever wired.
- **The two-stage law (replaces v1's "compile once at mount"):** *wear-time* — parse, validate, compile assembly EDN → builder closure, once. *Data-change-time* — apply builder × data-context → rt-node tree, inside the one `m/latest` (every existing pane re-derives its scene on data change; that is the correct shape here too). *Frame-time* — sample + diff only. Interpretation cost lives on the data-change path, never the frame path.
- **`:actions` (interaction, reserved now, built later):** rt-node already carries `:actions` with innermost-first `dispatch-event`. Assemblies name actions by **keyword, resolved through an action registry** — references, not logic, so the arrangement-only guard holds. v0 faces are read-only + command-worn; the eventual shape is assemblies declaring gesture → ActionRequest (the trail face's expansion-click → request re-stamp is the precedent).

**The guard:** assemblies are **arrangement only** — no conditionals, no expressions, no logic, ever. Computation lives in projections (real server-side Clojure) or primitives (real cljc). Anything that wants to be a program gets to be a real one, in the code lane. This one rule prevents the inner-platform death and is the kernel's own graduation pattern applied to UI: things start as code, graduate to data when the pattern stabilizes.

Why this dodges the two walls every no-code tool dies at: the primitive set is not a vendor's (a missing primitive is a cljc function away — zoom to bedrock), and the built things are not opaque (every assembly is a provenance-carrying object with a trail).

## The road

**Step 0 — the thin vertical slice.** *Half a day. Evidence before architecture — re-aimed at the real unknowns; all Electric probes dropped.* One hand-written EDN assembly → compiled builder → rt-node tree → on screen beside the existing UI, through the real `<world-snapshot` path. Checks while there: (a) `wrap-line` behavior on long prose blocks at conversation scale; (b) the per-pane scroll convention inside an assembly; (c) interpreter rebuild cost at ~hundreds of nodes on data change (precedent — sidebar and trail rebuild pure scenes today — says fine; measure anyway); (d) **the measure step** — layout is position-only: `layout-children` stacks children by their pre-existing `:w`/`:h`, and `resolve-text-layout` writes wrapped lines into `:text` without updating the node's `:h` (`rect_tree.cljc:75-192`), so a prose primitive must compute its own height (wrapped-line count × line-height — the `build-empty-state`/`ui-panel-group` pattern) *before* stacking; a walker leaning on `:auto-height?` + `:text-layout` together produces zero-height prose cards. Rule of thumb: **measure inside the primitive, arrange in the engine.** Already retired at source: `:row` layout exists (`rect_tree.cljc:81`); prose wrapping has an engine hook, not just a bare fn (`:text-layout` auto-wraps and stacks text ops, `rect_tree.cljc:154`). Exit: the walker is real on the real path, and the checks are numbers, not guesses.

**Step 1 — grammar v0 + interpreter + the vocabulary; one hand-written face.** The five-key grammar; the guard; the two-stage compiler; the vocabulary by **extraction and gap-fill** from `ui_primitives.cljs` + `trail_face/` (cards/lanes/threads are proto-primitives, hand-wired today — exactly what assemblies replace). Genuinely new primitives are few: wrapped text-run for prose blocks (mostly a thin wrapper over the existing `:text-layout` engine hook plus Step 0's check-(d) measure step), indent-rail, little else before Step 3. Two falsifiers: (a) *mechanical* — re-express the sidebar list (or a trail-face slice) as an assembly producing a **structurally equal rt-node tree**; (b) *golden* — assembly + fixture → golden rt-tree snapshot tests, JVM-side (`.cljc` interpreter), the block-kernel goldens discipline. Exit: the Outline face, hand-written EDN, rendering a hardcoded conversation in a workspace panel. *This step is the framework package's contract — and it is smaller than v1 sized it.*

**Step 2 — real material: the conversation projection + the artery.** Server-side projection from the block kernel — green 2026-07-09/10 (gate round 2 PASS; real-chat receipt: 247 river blocks / 399 debris rows / 44 edges): conversation → turns → blocks-with-kinds, delivered by the **one generic face-pull** (request = face + address + params → projection → data-context atom) — the first face that never hand-wires transport. Bottom-bar command picks a conversation; **replay/scrub rides free** (a bounded read — 90% of "live" at 10% of the plumbing). Exit: a real past conversation through the Step-1 face — the first honest wearing; every lack the face exposes becomes the ordered data work (D-005 in its intended direction).

**Step 3 — plurality: the design round's faces as assemblies.** *(Goosebump demo #1.)* Transcribe the strongest candidates in primitive-cheapness order (Outline → Margin → Arcs); missing primitives minted by gap-fill in the code lane, minutes each, hot-swapped; the grammar grows only against the guard. Exit: the same conversation in 2–3 arrangements, flipped live by command. The six designed faces are the grammar's design test suite (the worn UI is the mechanical one — Step 1's falsifier).

**Step 4 — the arsenal: faces and components become land objects.** Assemblies into Rama as objects (name, belief, provenance, status) with lineage edges; registry entries as addressed code objects. Pragmatic authoring v0: `.edn` files + a watcher (sibling of the markdown ingest) — the zoom-100 editor is already the assembly editor: save → ingest streams → panel re-renders. The sidebar lists faces; `face <name>` wears one; **wearing events are logged** — the desire-path instrument that decides which face earns primitive investment. Also here: register the existing workspace parts (editor, sidebar, shell) as the first component objects — the land learns to see its own UI. Back-arrow holds: authoring streams INTO Rama; the UI reads Rama.

**Step 5 — structure-honest faces: the machine cut as RelationEdges.** An LLM annotator via the existing LLM-kernel intent→executor pattern asserts block roles (nucleus / satellite / aside), pair bindings, episode boundaries — as D-004 edges with `asserted-by` first-class (`:grounds :assembled-from :refines` registered; `pairs-with` the one candidate addition). Faces that need structure (Arcs, Spread) become honest; machine-guessed renders visibly machine-guessed — silver marks, Sid's reactions as gold.

**Step 6 — live: the current conversation in a worn face.** *(Goosebump demo #2.)* A tailer on the Claude Code session JSONL streams increments through the existing convergent re-ingest → block kernel → projection → the worn face re-renders as we talk. Today this rides L6 coarse rebuild — measured fine at conversation scale; append-only also pre-qualifies live chat as Gap-3's first consumer if that rung ever fires. Exit: you talk in the terminal; the conversation assembles itself inside Softland while you type.

**Step 7 — the orchestrator: the AI writes assemblies.** *(The design conversation moves in.)* Stage A — immediately after Step 4, zero new machinery: Claude Code writes/edits assembly `.edn` through the watcher. Stage B — in-land: assembly proposals as provenance-first observations through the A2-sanctioned worker path (D-008 A2, countersigned 2026-07-08), schema-validated (malformed → error card, never a crash), `based-on` edges to the birthing conversation, rendered beside the chat as the event lands. Exit: the first design conversation conducted wholly in Softland — Softland design v0. The claude.ai harness retires to exploratory rounds; the daily loop lives at home.

**Horizon — named, not scheduled.** Minting new *leaf* primitives in-land (the code lane's editor is the zoom-100 editor; true runtime eval stays gated on a form-break, probably never needed while hot-reload covers the mint loop). Sharing: assemblies travel as provenanced values — a transport problem later, not a redesign. Interaction beyond `:actions` references: assemblies declaring gesture → ActionRequest, entered when read-only friction demands it (D-008's milestone language, applied to faces).

**Birth-law compliance (render-north, D-009 instruments):** the D-009 H6-keyed scene store does not exist yet (`view-instance` appears nowhere in src; Δ3 store promotion fires at the face-2 contract). The road rides the current rect-tree path now, and the interpreter **carries `(view-instance, address)` through from birth** so Δ1 compliance is a rename when the store lands, not a rework; the assembly grammar sits above that seam, unaffected. **Δ7 (slug-glyph expansion) is a named dependency** — reading faces are text-heavy and the design-language typography gates on it (it gates the *look*, not the walker).

**The open face-2 wave:** nothing here interrupts it; it shares the Δ3/Δ7 dependencies; and `trail_face/` is prior art the Step-1 extraction harvests directly (its hand-wiring is the before-picture of the artery).

## Deliberately not building

- No Electric-native primitives on this road (the seam law; U4/U5 stay in the skill for future Electric-level work).
- No Gap-3 wiring until a worn face breaks against rebuild cost — the ladder's rungs are evidence-paced.
- No DOM face layer (D-009's island engine).
- No logic in assemblies, ever, in v0 (the guard).
- No drag-and-drop builder UI (the editor + the AI are the authoring surfaces).
- No SCI-in-assemblies until a worn face demonstrates the need.
- No general framework / no-code-platform ambitions until ≥3 faces share the vocabulary.
- No multi-user sharing machinery now (designed-for, not built).

## Where this sits in the decision log

Threads closures, touches none: D-001 governs both growth layers; D-002-A1 names what faces render (sense-line units); D-004 carries the machine cut; D-005 runs Steps 2–3; D-008-A2 is the orchestrator's write path; D-009 rules the substrate (and the seam law here is its enforcement at the framework layer); D-010 lets every revert-cheap step proceed by default.

Two entries for the log when Sid says go:

1. **The middle regime, named.** The assembly layer is the land's furniture described in the land's own DATA — neither Regime-1 external code nor gated Regime-2 self-written code. No code enters Rama, only arrangement; the compiled substrate stays git-authored.
2. **The Regime-2 self-hosting test, formulated** — answering the open question sitting in the log since D-003: *the test passes when a design conversation produces a usable view without leaving the land and without hand-translation.* Step 7B is its first worked instance.

The seam law + harmonization ladder enter the framework package CONTRACT (contract-level law, not log-level), so "why not Electric-native faces" never gets re-derived.

## Sizing

Step 0 = half a day. Step 1 = the framework package contract — and smaller than v1 sized it: the only genuinely new machine is the **EDN walker + `:bind` resolution + two or three block primitives** (wrapped text-run, indent-rail), over a component library, layout engine, text pipeline, and event dispatch that already exist. Steps 2–4 = one delivery-mode wave, mostly assembling what exists (block kernel green, R4 shipped, ingest convergent, watchers precedented). Step 5 = one package over the LLM kernel. Step 6 = small. Step 7A = free once 4 exists; 7B = a small worker + validation.

**The road to goosebump #1 (Step 3) is close** — substrate, vocabulary, material, and designs all exist. Immediate next move: **Step 0**, the half-day vertical slice.
