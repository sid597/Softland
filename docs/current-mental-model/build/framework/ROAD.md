# The Framework Road — faces made of Softland

2026-07-10 · synthesis of three independent Fable runs over Sid's 2026-07-10 vision message (verbatim: `vision/LOG.md` 2026-07-10 entry) · adjudications grounded in the decision log, the archived e/fn-values Slack thread (`build/electric-skill/SLACK-THREAD-efn-values.md`), and the live source — read at source this session, not from memory. Status: ROAD (direction-grade synthesis; becomes a package contract on Sid's go). Vocabulary is working scaffolding — names finalize by recurrence, and the naming is Sid's.

## Where the three runs converge — the invariant core

All three runs, independently, landed on:

1. **No import boundary.** The design output is born in the land's own runnable vocabulary. Never render-then-componentize; there is no moment where a dead picture becomes alive, because nothing was ever dead.
2. **The load-bearing split.** *Primitives* — compiled Electric components (the vocabulary; slow lane; real code). *Assemblies* — pure data describing arrangement (the fast lane; a **face is an assembly**). *One interpreter* — a single generic component that walks assembly data and instantiates primitives through a registry. Leaves are code; composition is data. Every live-medium system in history converged on this split (Smalltalk classes vs morph-compositions, HyperCard runtime vs stacks, Retool engine vs app-JSON) because author-at-runtime is only tractable when structure is data and only vocabulary is code.
3. **Faces are land objects.** Stored in Rama with name, belief line, provenance (birthed-by conversation), lineage (`based-on` / `supersedes` / `dead-end`), `asserted-by`, and status (candidate / worn / retired). Sid already spec'd this on 2026-07-05: "view-specs as assertions … a spec in itself that I can change later or fork or inherit."
4. **D-001 governs growth at both layers.** Primitives are minted only by gap-fill when a face in use breaks against the vocabulary; the grammar formalizes only after several faces share it. The framework precipitates from worn faces — never from the abstract.
5. **The destination is a loop, not a feature.** Conversations produce faces; faces render conversations. The AI writes arrangement-data through the lawful path; wearing — not argument — decides which faces survive.

The runs differed on **hosting** and **sequencing**. Those are adjudicated below against the log, not by taste.

## Adjudications

**A. Where faces render — ruled by D-009, not by preference.** One run put faces in DOM with WebGPU as a later promotion tier. The log forecloses that: D-009 (ONE SUBSTRATE, closed 2026-07-06) names "specs-as-assertions gets readmitted-around via a DOM-shaped island engine" as exactly the failure the ruling exists to prevent — one scene store, one diff pipeline, one camera loop, **one spec grammar**; everything is a projection over the single substrate. The live source corroborates: there is one `dom/div` in the entire app — there is no DOM UI layer to host faces in anyway. So assemblies compile into the one scene store through the one spec grammar D-009 already names. The DOM-first run's own fallback ("target the WebGPU scene layer") is not the fallback; it is the road.

Binding consequence: **interpret at mount, never at frame.** The render path is continuous-synchronous (`m/latest` chains sampled on RAF → GPU). An assembly compiles ONCE, at mount, into that reactive DAG; after mount it is indistinguishable from hand-written UI. Per-frame interpretation would eat the frame budget and violate the no-side-effects-in-latest law.

**B. Two grammars, one mapping.** The *content grammar* (what a conversation IS — block unit, roles, nesting; the dual-read design round is deriving it, and the block kernel now materializes it) and the *render grammar* (what the substrate draws — the primitive vocabulary) are different layers. A **face is precisely the mapping between them**: an assembly binds block-shapes to primitive slots. Conflate the two and you have rebuilt the DOM — content, markup, and layout as one tangle. Keep them separate and "one conversation, six faces" is free.

**C. The orchestrator's write path — already sanctioned.** One run gated AI-written assemblies on D-008's read→write milestone. The log has moved: D-008 **A2** (countersigned 2026-07-08) rescoped the boundary — machine-proposed writes flow NOW as provenance-first observations through the lawful worker→Rama path (back-arrow compliant, silver tier visibly distinct); only human UI write-gestures stay gated. AI-authored assemblies are exactly that shape: silver proposals. Sid's ordinary reactions — wearing a face, "love iittttt", "this is shit" — are the gold ratification gestures. Nothing waits on a future milestone.

**D. What the six designed UIs are.** Not artifacts to import — the **specification of the primitive catalog**, and the grammar's completeness test ("can the grammar express all six" = v0 done). They get transcribed, not imported; the first face is expensive, the sixth cheap — and that falling cost curve is itself the measurement that the vocabulary is right.

## The mechanism it stands on (upstream-affirmed; one spike from law)

From the archived Slack thread: **e/fns are values** — "concretely pointers/ids … interpreted in the context of the common program dag" (Dustin Getz), confirmed working inside plain data structures (noonian's map-of-e/fns). So a registry `{keyword → e/fn}` dispatched at runtime is sanctioned Electric: runtime assembly of precompiled components, no compiler in the loop. Two disciplines fall straight out of the thread:

- **Persistence stores names; runtime uses pointers.** An e/fn pointer is meaningful only inside a running program's DAG. Rama stores keywords/addresses — registry entries as code-as-addressed-material, which D-009's modularity note already names "the first candidate" — and the client resolves keyword→pointer at mount.
- **Site explicitly.** Electric v3 resolves sites dynamically (by booter, not lexically); every primitive carries its own `e/client` internally, per Dustin's own recommendation in the thread.

Epistemic status, honestly: upstream-affirmed + user-confirmed, **not yet regression-proven on our pinned build**. Promoting the thread's claims (the skill's U4/U5) to laws — or fencing them — is Step 0's entire job.

## Grammar v0 — five keys, one guard

`{:prim :props :children :each :bind}`. Example (block-kernel vocabulary):

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

`:each` is a keyed `e/for-by` under the hood — and per the verified law register (L16), diff cost is bound by diff *shape*, not element count: an append-only growing transcript stays on the cheap path permanently.

**The guard:** assemblies are **arrangement only** — no conditionals, no expressions, no logic, ever. Computation lives in projections (real server-side Clojure shaping Rama truth) or primitives (real Electric). Anything that wants to be a program gets to be a real one, in the code lane. This single rule prevents the inner-platform death (the grammar rotting into an accidental programming language) and is the kernel's own graduation pattern applied to UI: things start as code, graduate to data when the pattern stabilizes.

Why this dodges the two walls every no-code tool dies at: the primitive set is not a vendor's (a missing primitive is a cljc function away — zoom to bedrock, the editor at zoom 100 is the land's own code), and the built things are not opaque (every assembly is a provenance-carrying object with a trail — "how did this face come to be" is an answerable question).

## The road

**Step 0 — the spike.** *An afternoon; evidence before architecture.* A probe page in the dev app: a hardcoded registry of 2–3 probe primitives that write through the REAL scene path (a rect, a text run — the same calls the existing faces make); a hardcoded EDN tree; a recursive walker; `:each` via `e/for-by`; unsited vs explicitly-sited boot. Proves or falsifies, on the pinned build: registry dispatch (U4), dynamic siting (U5), recursion depth (Electric's historical sharp edge; fallback = an explicit worklist loop instead of self-call), and scene-store targeting (the one item every run flagged unverified). Exit: verdicts written into the electric-docs skill as laws or fences; the probe kept as a regression test.

**Step 1 — grammar v0 + interpreter + the first vocabulary; one hand-written face.** The five-key grammar; the arrangement-only guard; the mount-time interpreter; 5–6 primitives packaged FROM existing render code (text-run, block-card, stack, indent-rail, clip) — extraction, not invention: the substrate already draws text at quality. Exit: the Outline face, hand-written EDN, rendering a hardcoded conversation in a workspace panel on the real substrate. This step is the framework package's contract — the one genuinely new machine.

**Step 2 — real material: the conversation projection.** A server-side projection from the block kernel — GREEN as of 2026-07-09/10 (gate round 2 PASS; real-chat receipt: 247 river blocks / 399 debris rows / 44 edges) — shaping conversation → turns → blocks-with-kinds, delivered over the shipped bridge (R4: server mirror atom + `e/watch`). A bottom-bar command picks a conversation; **replay/scrub rides free** (it is just a bounded read — 90% of "live" for driving faces at 10% of the plumbing). Exit: a real past conversation rendered through the Step-1 face — the first honest wearing. Every lack the face exposes becomes the ordered data work: D-005 running in its intended direction.

**Step 3 — plurality: the design round's faces as assemblies.** *(Goosebump demo #1.)* Transcribe the strongest candidates in primitive-cheapness order (Outline → Margin → Arcs); missing primitives minted by gap-fill in the code lane (minutes each, hot-swapped); the grammar grows only against the guard. Exit: the same conversation in 2–3 arrangements, flipped live by command. The six designed faces are the grammar's test suite.

**Step 4 — the arsenal: faces and components become land objects.** Assemblies move into Rama as objects (name, belief, provenance, status) with lineage edges; component-registry entries land as addressed code objects (D-009's registry note realized). Pragmatic authoring v0: assemblies as `.edn` files + a watcher (sibling of the markdown ingest) — so the zoom-100 editor is already the assembly editor: save → ingest streams → panel re-renders. The sidebar (the one component browser) lists faces; `face <name>` wears one; **wearing events are logged** — the desire-path instrument that decides which face earns primitive investment. Also here: register the existing workspace parts (editor, sidebar, shell) as the first component objects — the land learns to see its own UI. Back-arrow holds throughout: authoring streams INTO Rama; the UI reads Rama.

**Step 5 — structure-honest faces: the machine cut as RelationEdges.** An LLM annotator via the existing LLM-kernel intent→executor pattern asserts block roles (nucleus / satellite / aside), pair bindings (ask↔reply), episode boundaries — as D-004 edges with `asserted-by` first-class (`:grounds :assembled-from :refines` are already registered; `pairs-with` is the one candidate addition). Faces that need structure (Arcs, Spread) become honest, and machine-guessed structure renders visibly as machine-guessed. This is where the face layer meets the sense-line mark architecture: silver marks from the machine, Sid's reactions as gold.

**Step 6 — live: the current conversation in a worn face.** *(Goosebump demo #2.)* A tailer on the Claude Code session JSONL streams increments through the existing convergent re-ingest (deterministic ids make replay safe) → block kernel → projection → the worn face re-renders as we talk. Append-only is the diff pipeline's happy path (L16). Exit: you talk in the terminal; the conversation assembles itself inside Softland while you type — without changing where or how you chat.

**Step 7 — the orchestrator: the AI writes assemblies.** *(The design conversation moves in.)* Stage A — immediately after Step 4, zero new machinery: Claude Code writes/edits assembly `.edn` through the watcher; design-by-conversation with live feedback. Stage B — in-land: assembly proposals flow as provenance-first observations through the A2-sanctioned worker path, schema-validated (a malformed assembly renders as an error card; it can never crash the land), carrying `based-on` edges to the conversation that birthed them, rendered beside the chat as the event lands. Exit: the first design conversation conducted wholly in Softland — Softland design v0. The claude.ai harness retires to what it is good at (exploratory rounds); the daily loop lives at home.

**Horizon — named, not scheduled.** Minting new *leaf* primitives in-land (the code lane's editor is the zoom-100 editor; true runtime eval of Electric stays gated on a form-break and is probably never needed while hot-reload covers the mint loop). Sharing: assemblies travel as provenanced values — a transport problem later, not a redesign; "anyone else can use them too" was designed in from the data model up.

## Deliberately not building

- No DOM face layer — not even as a "cheap v0" (D-009's island engine).
- No logic in assemblies, ever, in v0 (the guard).
- No drag-and-drop builder UI (the editor + the AI are the authoring surfaces).
- No SCI-in-assemblies until a worn face demonstrates the need.
- No general framework / no-code-platform ambitions until ≥3 faces share the vocabulary — the framework precipitates from worn faces (D-001 applied to the framework itself).
- No multi-user sharing machinery now (designed-for, not built).

## Where this sits in the decision log

Threads closures, touches none: D-001 governs both growth layers; D-002-A1 names what faces render (sense-line units); D-004 carries the machine cut; D-005 runs Steps 2–3; D-008-A2 is the orchestrator's write path; D-009 rules the substrate fork and already names registry entries as code-as-addressed-material; D-010 lets every revert-cheap step proceed by default.

Two entries for the log when Sid says go:

1. **The middle regime, named.** The assembly layer is the land's furniture described in the land's own DATA — neither Regime-1 external code nor gated Regime-2 self-written code. No code enters Rama, only arrangement; the compiled substrate stays git-authored.
2. **The Regime-2 self-hosting test, formulated** — answering the open question that has sat in the log since D-003: *the test passes when a design conversation produces a usable view without leaving the land and without hand-translation.* Step 7B is its first worked instance.

## Sizing and package shape

Step 0 = an afternoon. Step 1 = the framework package contract (Fable contract + D-006 machinery). Steps 2–4 = a delivery-mode wave, mostly assembling things that exist (block kernel green, bridge shipped, ingest convergent, watchers precedented). Step 5 = one package over the LLM kernel. Step 6 = small (tailer + re-ingest). Step 7A = free once Step 4 exists; 7B = a small worker + validation.

The road to goosebump #1 (Step 3) is close: the substrate, the material, and the designs all exist. The only genuinely new machine is grammar + interpreter + vocabulary packaging.

**Immediate next move: Step 0.**
