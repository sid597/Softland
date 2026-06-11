;; ════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
;;
;;   K E R N E L   ─   The shape every kerneled module follows
;;
;;   STATUS: SPEC + LIVE SNAPSHOT.
;;     - The top of this file is the prose spec (theory ↔ model, CT framing, formalization matrix).
;;     - The bottom of this file is (defkernel KERNEL-SHAPE ...) — a Clojure-readable, inspectable
;;       description of what kernels look like ACROSS THE 5 CURRENT INSTANCES.
;;     - The defkernel form is DATA, not code generation. Nothing in the codebase calls it.
;;       It exists to be inspected, discussed, and edited as new kernels are built. Git tracks
;;       the evolution; this file is the single point of reference for "what is a kernel?"
;;
;;   In CT terms:
;;     prose + KERNEL-SHAPE  =  the *theory* of a kernel
;;     5 Rama instance files     =  *models* of that theory
;;     (hypothetical) generator-defkernel  =  a *functor* from DomainDesc to Module — DEFERRED
;;
;;   In Rama terms: each kernel-instance is a (defmodule ...). In our architecture we call them
;;   kernels because that's the role they play. The shared core is not an instance; it holds
;;   the common contracts those instance files use.
;;
;; ════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
;;
;;   WHY THIS FILE EXISTS
;;   ─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
;;
;;   The codebase contains FIVE instances of the same shape:
;;
;;     core               shared request/event contracts (src/app/server/rama/core.clj)
;;     text-kernel        V0/V1 text-artifact module (src/app/server/rama/text_kernel.clj)
;;     space-kernel       chat threads, turns, objects, slices, overlays, derivatives, patches
;;                          (src/app/server/rama/dogfood/space.clj)
;;     compute-kernel     shell-command execution (src/app/server/rama/dogfood/compute.clj)
;;     llm-kernel         LLM turn-run execution (src/app/server/rama/dogfood/llm.clj)
;;     transcript-kernel  CLI transcript ingest (src/app/server/rama/dogfood/transcript.clj)
;;
;;   The common pattern:
;;
;;     actor ──┐                                      ┌── stream-topology ──────┐
;;             ▼                                      │                         │
;;        *X-depot           (intent)            ─────▶                         │
;;        *X-claim-depot     (executor lock)     ─────▶  interpret → accept/    ─────▶  PStates  ──▶  e/watch  ──▶  UI
;;        *X-obs-depot       (back-arrow)        ─────▶  reject → emit events   │
;;        *X-control-depot   (optional control)  ─────▶                         │
;;                                                     └─────────────────────────┘
;;
;;   The depot family is OPTIONAL at the bottom. Only :intent-depot is always present.
;;   Some kernels are intent-only (text, space). Some have intent + execution-pair (compute,
;;   transcript). One has all four plus a control plane (llm). KERNEL-SHAPE below shows the
;;   distribution exactly.
;;
;;   Until now this pattern has been re-implemented per file with no single artifact you could
;;   point at. The (defkernel KERNEL-SHAPE ...) form below gives the pattern that single home.
;;
;; ════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
;;
;;   STRUCTURAL PICTURE — THREE ROLES, THREE EXISTENCES
;;   ─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
;;
;;   ┌──────────────────────────────────────────┬──────────────────────────────────────────┬──────────────────────────────────────────┐
;;   │ 1. THE SHAPE                              │ 2. EACH INSTANCE                          │ 3. (DEFERRED) THE APPLY STEP              │
;;   ├──────────────────────────────────────────┼──────────────────────────────────────────┼──────────────────────────────────────────┤
;;   │ What every kernel must look like.         │ A concrete kernel for one domain.         │ How a domain could become a kernel        │
;;   │ Has no depot, no PState, no module.       │ Has its own depot family + topology       │ via code generation, if/when useful.      │
;;   │                                           │ + pstates + interpret-fn.                 │                                           │
;;   │ Lives as:                                 │                                           │ TODAY: not implemented.                   │
;;   │   • the prose spec at the top of this     │   text_kernel.clj          text-kernel    │   The 5 modules are hand-written.         │
;;   │     file                                  │   dogfood/space.clj        space-kernel   │   defkernel is data-only; describes the   │
;;   │   • (defkernel KERNEL-SHAPE ...)          │   dogfood/compute.clj      compute-kernel │   shape, doesn't generate it.             │
;;   │     at the bottom of this file            │   dogfood/llm.clj          llm-kernel     │                                           │
;;   │                                           │   dogfood/transcript.clj   transcript-   │ FUTURE: deferred — see "WHY defkernel IS  │
;;   │                                           │                            kernel         │   DATA, NOT GENERATION TODAY" below.      │
;;   │                                           │                                           │                                           │
;;   │  ↓ THE PATTERN ↓                          │  ↓ THE THINGS ↓                           │  ↓ (deferred) ↓                           │
;;   └──────────────────────────────────────────┴──────────────────────────────────────────┴──────────────────────────────────────────┘
;;
;;   QUESTIONS THIS RESOLVES
;;
;;     Q.  Is kernel a thing or a pattern?
;;     A.  Both, in two different places of this same file. The PROSE at the top is the
;;         pattern. The (defkernel KERNEL-SHAPE ...) form at the bottom is the pattern made
;;         concrete and inspectable. Each instance file is one model of the pattern.
;;
;;     Q.  Can a kernel be a depot?
;;     A.  The kernel pattern has no depot. Each kernel-instance has its own depot family.
;;         The pattern dictates the family's shape; the instance fills it in.
;;
;;     Q.  How do I build a new kernel today?
;;     A.  Read KERNEL-SHAPE below. Find the closest existing instance (by which fields it
;;         uses). Copy that instance's file as a template. Edit the depots / PStates /
;;         interpret-fn for the new domain. Then come back and add an :examples entry to
;;         KERNEL-SHAPE so future builders see what you did. There is no macro that does
;;         this for you today — and that's deliberate (see below).
;;
;;     Q.  What's the CT name for "apply the pattern"?
;;     A.  Functor application. The pattern is a theory; each instance is a model of that
;;         theory; a generator macro would be a functor from DomainDesc to Module. Today
;;         we have the theory and the models; we don't have the functor.
;;
;; ════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
;;
;;   CATEGORY THEORY — THE LADDER  (simplest to richest)
;;   ─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
;;
;;   Pick the level that gives you leverage without dragging in unneeded structure.
;;   Day-to-day work uses Levels 3 and 4. Level 1–2 are how the idea would be expressed in
;;   Clojure if we had a generator. Level 5 is the design discipline that says we shouldn't
;;   have the generator yet.
;;
;;   LEVEL 1 — TRAIT / TYPECLASS
;;     Kernel is an interface; each module implements it.
;;     How 90% of working code expresses the idea. Gives almost no math.
;;
;;   LEVEL 2 — FUNCTOR    ← the categorical generalization of "function"
;;     defkernel-as-generator (HYPOTHETICAL, deferred):
;;       DomainDesc ─────▶ Module
;;            ↑                  ↑
;;        (request schema,    (depot family +
;;         interpret-fn,       stream-topology +
;;         pstate spec)        pstates + projections)
;;
;;     A functor is a structure-preserving map between categories. If we ever build the
;;     generator, each (defkernel space-kernel ...) call would be one functor application.
;;     Today the generator is deferred; the data-only defkernel macro below is just storage.
;;
;;   LEVEL 3 — ALGEBRAIC THEORY / LAWVERE THEORY    ← the technically precise home
;;     Kernel = a theory  (operations: accept-request, emit-event, reject;
;;                         laws: idempotency, deterministic acceptance, replay-invariance).
;;     text-kernel, space-kernel, ... = models of that theory.
;;     The theory lives in code as the prose + KERNEL-SHAPE form; models are the concrete
;;     modules.
;;     Reframe: "space-kernel is a model of the Kernel theory in the category of conversations."
;;              "compute-kernel is a model of the Kernel theory in the category of shell commands."
;;
;;   LEVEL 4 — F-ALGEBRA    ← the precise math of the ETL fold
;;     Each kernel's interpret + materialize forms a structure map     α : F(A) ──▶ A
;;       where F is "the depot-input functor" (request | claim | observation | control)
;;       and   A is "the PState family" (the carrier of state).
;;     The Kernel shape  =  the choice of F.
;;     Each instance     =  an F-algebra over a specific carrier A.
;;
;;     STATUS: conceptual framing.
;;       Existing modules ARE Rama ETL folds over depot inputs into PState families.
;;       The CLAIM "replay = fold the depot log through α to rebuild A" is the right mental
;;       model for what the code SHOULD support — but cold-replay / property-test proof is
;;       PLANNED, not currently guaranteed by this code. Use the framing as intuition, not
;;       as a verified property.
;;
;;   LEVEL 5 — FREE CONSTRUCTION    ← the design discipline
;;     "Don't add anything you didn't ask for." Extra structure has to be earned.
;;     This file applies the discipline to itself: we do not pre-build a code-generating
;;     defkernel because no one has asked for one. The current defkernel is data only.
;;
;; ════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
;;
;;   HOW MUCH OF THIS CODEBASE CAN BE CATEGORY-THEORIZED?
;;   ─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
;;
;;                                                                            FORMALIZABLE         PAYS BACK
;;   LAYER                                                                    CATEGORICALLY?       IN PRACTICE?
;;   ─────────────────────────────────────────────────────────                ────────────────     ─────────────
;;   1. THE KERNEL PATTERN                                                    YES — clean fit      YES — large    ←  this file
;;      Theory ↔ Model. Each kernel is an F-algebra.
;;
;;   2. EVENT-SOURCING / REPLAY                                               YES — textbook       YES (WHEN VERIFIED)
;;      Replay is a fold over an F-algebra; replay equivalence                F-algebra fit       Cold-replay /
;;      = associativity of the fold.                                                              property tests are
;;                                                                                                planned but not yet
;;                                                                                                written. Currently
;;                                                                                                aspirational.
;;
;;   3. PROJECTIONS                                                           YES — colimit        MEDIUM
;;      Each projection is a colimit over the event history                   over events          (helps reason,
;;      that touched the projected key.                                                            rarely changes
;;                                                                                                 day-to-day code)
;;
;;   4. CROSS-MODULE WIRES (mirror-depot)                                     YES — natural        MEDIUM
;;      The wire is a natural transformation between two functors             transformation       (the framing
;;      over a shared key. OPERATIONALLY it's more concrete: mirror-depot     (operationally       catches bugs;
;;      + |hash$$ + depot-partition-append! + ack-level. NT framing is        more than NT — see   doesn't drive
;;      intuition; operational contract is what code enforces.                KERNEL-SHAPE         code)
;;                                                                            :cross-module-wires)
;;
;;   5. PSTATES AS POLYNOMIAL FUNCTORS                                        YES — direct         SMALL
;;      Nested specter paths ARE the lens algebra on poly-functors.           (Spivak)             (specter already
;;                                                                                                 gives the algebra
;;                                                                                                 without the math)
;;
;;   6. SCHEMAS / CONTRACTS                                                   PARTIALLY            SMALL
;;      Types-as-propositions. In Idris, full proofs;                         (limited by          (Malli/Specs get
;;      in Clojure, runtime validation.                                       Clojure's type       most of it)
;;                                                                            system)
;;
;;   7. RETRY / RACE / PARTITIONING SEMANTICS                                 PARTIALLY            MEDIUM
;;      Operational semantics. CSP, π-calculus, process algebras are          (process calculus,   (memory already
;;      the right tools — not Set-theoretic CT.                               not regular CT)      encodes these as
;;                                                                                                 rules — that IS
;;                                                                                                 the formalization)
;;
;;   8. ELECTRIC REACTIVE LAYER                                               YES (already)        DONE
;;      Continuous-synchronous programming, Missionary continuous flows.      — Lustre lineage     (the paradigm
;;      (See Van Roy's taxonomy in CLAUDE.md.)                                                     IS the math)
;;
;;   9. UX / SEMANTIC ZOOM / ATTENTION DYNAMICS                               PARTIALLY            SMALL
;;      Vision-layer concerns. Some pieces are categorical (zoom as           (modal logic +       (CT doesn't
;;      parameterized projection); others (feel, pacing) are not Set-shaped.  dynamic systems)     describe feel)
;;
;;  10. SOFTLAND-AS-A-PLACE                                                   NO                   N/A
;;      "Software is a place" is a design stance,                            (design philosophy)
;;      not a mathematical structure.
;;
;;   HONEST READING:
;;     Layers 1–4  =  CT formalization pays back a lot.   ←  THIS FILE'S SCOPE
;;     Layers 5–8  =  Already implicit in the tools.
;;     Layers 9–10 =  Out of scope. Forcing CT here only confuses.
;;
;; ════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
;;
;;   WHERE CT HELPS vs BECOMES BOOKKEEPING
;;   ─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
;;
;;                                                  HELPS                                              BECOMES BOOKKEEPING
;;                                                  ─────────────────────────────────────────────     ─────────────────────────────────────
;;   system has many similar pieces                 ✓ name the shape; new pieces are cheap            when you have 1–2 pieces, naming the
;;                                                                                                    shape costs more than the duplication
;;
;;   you want LLMs to compose things                ✓ small, named, composable shapes shrink          over-categorized code is harder for
;;                                                    the LLM's search space dramatically             LLMs because it's drowning in
;;                                                                                                    abstraction without examples
;;
;;   correctness matters at the boundary            ✓ natural transformations & laws catch real       inside the boundary, property tests
;;                                                    wiring bugs (e.g. cross-module-mirror           do more work per minute than
;;                                                    consistency)                                    category-theoretic proof
;;
;;   team is small / homogeneous in math fluency    ✓ shared vocabulary speeds review                 if anyone has to ask "what's a
;;                                                                                                    coequalizer" mid-PR, the abstraction
;;                                                                                                    is leaking complexity
;;
;;   the domain has stable structure                ✓ kernels, depots, events, projections —         UX, animation, pacing, feel — these
;;                                                    these shapes won't change much                  have unstable structure; CT prematurely
;;                                                                                                    freezes them
;;
;;   you want to scale design across people / LLMs  ✓ "make X a kernel" is one sentence;              without a 'kernel cookbook' (worked
;;                                                    everyone knows what that means                  examples), the vocabulary doesn't
;;                                                                                                    transfer
;;
;;   THE RULE:  categorize the load-bearing skeleton, leave the surface and the dynamics flexible.
;;              Skeleton stays stable; surface evolves freely.
;;
;; ════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
;;
;;   THE "LLM BUILDS SUBSYSTEMS BY CHATTING" REQUIREMENTS
;;   ─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
;;
;;   Long-term direction: user describes a feature in chat → LLM proposes a composition →
;;   LLM builds it → property tests / verification confirm correctness. The direction is
;;   plausible because mathematical shape narrows the search space (NOT because CT proves
;;   correctness — the proof story is secondary; the vocabulary discipline is primary).
;;
;;   What the direction actually requires, and where each piece stands today:
;;
;;   REQUIREMENT                                                  STATUS                                       GATING FACTOR
;;   ─────────────────────────────────────────────────────────    ─────────────────────────────────────       ────────────────────────────
;;   1. Stable, small set of composable primitives                ✓ kernels + depots + pstates +              KERNEL-SHAPE below now
;;      (the "vocabulary" the LLM can speak)                        projections — KERNEL-SHAPE                 encodes this. Evolve it as
;;                                                                  encodes them                               new kernels reveal fields.
;;
;;   2. Strong schemas / contracts at every primitive             ◐ partial — contract is prose;              Convert to Malli; retrofit
;;      (so the LLM knows when its output is wrong)                 see kernel-contract-table in              to interpret-fn inputs /
;;                                                                  core.clj                                 outputs
;;
;;   3. Worked examples / cookbook of compositions                ✓ the 5 existing kernels ARE the           Each :examples entry in
;;      (so the LLM can pattern-match)                              examples; KERNEL-SHAPE's :examples        KERNEL-SHAPE ties an abstract
;;                                                                  fields tie each abstract field to         field to concrete uses
;;                                                                  concrete uses
;;
;;   4. Property tests as oracles                                 ✗ mostly missing                            Need property tests for
;;      (the LLM's "did I get it right?" feedback loop)                                                       replay-determinism, retry,
;;                                                                                                            partitioner consistency
;;
;;   5. Domain-level conversation language                        ◐ partial — vocabulary has space /          Lock the vocabulary;
;;      (so the user can talk to the LLM in NOUNS the              turn / llm-run / transcript but            commit PR 1 (the rename)
;;      LLM-and-code agree on)                                     rename PR pending
;;
;;   6. Verification layer the LLM can call                       ✗ not really — Clojure runtime              Bigger lift; Codex-as-reviewer
;;      (run-as-you-write, ideally type-checked or proved)         errors are the only feedback loop          may be the bridge
;;
;;   7. A planner that translates intent → composition            ✗ entirely manual today                     The genuinely hard piece;
;;      ("I want a thing that watches Y and reacts" →                                                         still research-grade
;;       "compose transcript-tailer ∘ content-hash ∘ event-emit")
;;
;;   Items 1, 2, 3, 5 are concrete engineering.
;;   Items 4 and 6 are verification-infrastructure work.
;;   Item 7 is the open research problem.
;;
;; ════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
;;
;;   WHY defkernel IS DATA, NOT GENERATION TODAY
;;   ─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
;;
;;   The (defkernel KERNEL-SHAPE ...) form below is data — a Clojure-readable description of
;;   the current observed shape. It does not generate code. Nothing calls it. It exists to be
;;   inspected, discussed, edited.
;;
;;   This is a deliberate choice. The reflex of "name the pattern → extract a macro" was
;;   strong but premature:
;;
;;     • Extracting a generator macro NOW would freeze a shape the 5 instances haven't fully
;;       aligned on. Notice in KERNEL-SHAPE: 2 kernels are intent-only; 3 have an execution
;;       pair; 1 has a control plane; 1 has an executor; 1 has cross-module writes. No two
;;       are the same.
;;
;;     • The next 1–2 kernels are likely to reveal fields KERNEL-SHAPE doesn't have yet.
;;       Building a macro from a partial shape would lock new builders into a wrong
;;       abstraction.
;;
;;     • The pattern is enforced TODAY by convention + worked examples + KERNEL-SHAPE as a
;;       field guide. That is enough for a human or LLM to build kernel #6.
;;
;;   When would a code-generating defkernel start to make sense?
;;
;;     • When 3+ new kernels (#6, #7, #8) get built against KERNEL-SHAPE without it growing.
;;     • When the duplication starts producing IDENTICAL bugs across instances.
;;     • When a human or LLM finds it genuinely faster to learn a macro than to copy an
;;       instance.
;;
;;   Until then: KERNEL-SHAPE IS the meta-schema. Git tracks its evolution. The generator
;;   layer stays absent on purpose.
;;
;; ════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
;;
;;   THE PR LADDER
;;   ─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
;;
;;   PR 0  (this file)
;;         ──────────────────────────────────────────────────────────────────────────────────────────────
;;         CREATE kernel.clj — the spec living in code, plus (defkernel KERNEL-SHAPE ...).
;;         No behavior change anywhere else. The defkernel form is data-only; nothing in
;;         the rest of the codebase depends on it.
;;
;;   PR 1  (this change)
;;         ──────────────────────────────────────────────────────────────────────────────────────────────
;;         Mechanical rename + mechanical text/core file split. No behavior change.
;;         No `kernels/` subdir. No defkernel code generation.
;;
;;         HISTORICAL RENAME MAP (pre-PR1 name -> current name):
;;
;;           A. MODULES
;;              world-kernel-module             →  text-kernel-module                  (in text_kernel.clj)
;;              world-module                    →  space-kernel-module                 (in dogfood/space.clj)
;;              world-kernel-topology           →  text-kernel-topology
;;              world-chat-topology             →  space-topology
;;
;;           B. DEPOTS
;;              *world-requests-depot           →  *text-requests-depot
;;              *world-action-depot             →  *space-action-depot
;;              (mirror-depot call sites in callers/tests also)
;;
;;           C. REQUEST-TYPE KEYWORDS
;;              :world-thread/create            →  :space/create
;;              :world-thread/fork-from-span    →  :space/fork-from-span
;;              :world-thread/reconcile         →  :space/reconcile
;;              :world-turn/compose-and-send    →  :turn/compose-and-send
;;              :world-turn/draft-save          →  :turn/draft-save
;;              :world-turn/comment-create      →  :turn/comment-create
;;              :world-turn/slice-create        →  :turn/slice-create
;;              :world-turn/derivative-create   →  :turn/derivative-create
;;              :world-turn/patch-proposal-create → :turn/patch-proposal-create
;;              :world-turn/patch-accept        →  :turn/patch-accept
;;              :world-turn/patch-reject        →  :turn/patch-reject
;;              :world-turn/tool-approval-resolve → :turn/tool-approval-resolve
;;              :world-turn/cancel              →  :turn/cancel       (⚠ see collision note below)
;;              :world-turn/compact-request     →  :turn/compact-request
;;              :world-turn/steer               →  :turn/steer        (⚠ see collision note below)
;;              :world-turn/abandon             →  :turn/abandon
;;
;;           D. PSTATES
;;              $$world-requests-by-id          →  $$space-requests-by-id
;;              $$world-decisions-by-id         →  $$space-decisions-by-id
;;              $$world-events-by-id            →  $$space-events-by-id
;;              $$world-threads                 →  $$spaces            (plural primary, see convention)
;;              $$world-thread-graph            →  $$space-graph
;;              $$world-turns                   →  $$turns
;;              $$world-turns-by-thread         →  $$turns-by-space    (-by-* secondary index)
;;              $$world-send-by-idempotency     →  $$send-by-idempotency
;;              $$world-llm-run-requests        →  $$llm-run-requests   (drop "world-" — adds nothing)
;;              $$world-llm-run-by-turn         →  $$llm-run-by-turn       (space-owned mirror)
;;              $$world-llm-controls            →  $$llm-controls
;;              $$world-llm-control-by-turn     →  $$llm-control-by-turn
;;              $$world-patch-proposals         →  $$space-patch-proposals
;;
;;           E. FNS, VARS, NAMESPACE
;;              world-routing-key               →  space-routing-key
;;              world-action-request            →  space-action-request
;;              world-thread-create-request     →  space-create-request
;;              world-only-turn-request         →  space-turn-request
;;              default-world-branch-id         →  default-space-branch-id  ("space/main")
;;              :target/kind :world-thread      →  :target/kind :space
;;              ns app.server.rama.dogfood.world → app.server.rama.dogfood.space
;;              filename: dogfood/world.clj     →  dogfood/space.clj
;;
;;           F. EXTERNAL REFERENCES
;;              require/use sites pointing at renamed namespaces  (Electric server code,
;;                                                                 HTTP handlers, tests)
;;
;;           G. RUNTIME ENTRYPOINTS
;;              start-kernel-runtime!  / close-kernel-runtime!   → start-text-runtime! / close-text-runtime!
;;              runtime map keys :world-requests-depot, :requests-by-id…  →  :text-* equivalents
;;
;;           H. ADAPTER LAYER
;;              src/app/server/rama/util_fns.cljc        :world/append capability decision
;;                                                       "world kernel" log strings
;;                                                       core alias split from text-kernel alias
;;
;;           I. CROSS-MODULE INTERNAL REFS IN llm.clj
;;              :world-thread/id  →  :space/id   (LLM module's references into space vocab)
;;              :world-turn/id    →  :turn/id
;;              $$llm-thread-by-world-thread   →  $$llm-thread-by-space
;;              $$llm-turn-run-by-world-turn   →  $$llm-turn-run-by-turn
;;
;;           J. TESTS
;;              test/app/server/rama/world_kernel_test.clj   →  text_kernel_test.clj
;;              test/app/server/rama/dogfood_world_test.clj  →  dogfood_space_test.clj
;;              test/app/server/rama/dogfood_llm_test.clj    — rename expectations inside
;;
;;           K. ALIAS MIGRATION
;;              (:require [app.server.rama.core :as kernel])  →  :as core
;;              text runtime call sites also require app.server.rama.text-kernel
;;
;;         File moves: core text instance -> text_kernel.clj; dogfood/world.clj -> dogfood/space.clj.
;;         The extraction is only the shared core/text split, not a kernel abstraction.
;;
;;   ⚠ COLLISION NOTE for PR 1
;;
;;     PR 1 makes the space request keywords :turn/cancel and :turn/steer. llm.clj also
;;     uses :turn/cancel and :turn/steer as :control/type values. Same keyword, two
;;     semantic contexts:
;;
;;       {:request/type :turn/cancel}    ← space-kernel request
;;       {:control/type :turn/cancel}    ← llm-kernel control
;;
;;     This is intentional sharing: disambiguation is structural, by field. The code keeps
;;     short NOTE comments at both dispatch sites and tests both sides of the collision.
;;
;; ════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
;;
;;   CURRENT FILE LAYOUT (after PR 1)
;;   ─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
;;
;;     src/app/server/rama/kernel.clj             ← THIS FILE: the shape (prose + KERNEL-SHAPE)
;;     src/app/server/rama/core.clj               shared contracts/utilities
;;     src/app/server/rama/text_kernel.clj        text-kernel-module
;;     src/app/server/rama/dogfood/space.clj      space-kernel-module
;;     src/app/server/rama/dogfood/compute.clj    compute-kernel-module
;;     src/app/server/rama/dogfood/transcript.clj transcript-kernel-module
;;     src/app/server/rama/dogfood/llm.clj        llm-kernel-module
;;
;;   A kernels/ subdir remains a separate unchosen decision.
;;
;;   VOCABULARY LOCK  (once PR 1 lands)
;;
;;     Softland     =  the whole place. Product / vision word. No code-level entity owns it.
;;     kernel       =  the shape, in THIS file. No bare "kernel" exists as a module.
;;     text-kernel  =  V0/V1 text-artifact + units model
;;     space-kernel =  durable local work area (chat threads, turns, objects, slices,
;;                       overlays, derivatives, patches)
;;     turn         =  one move inside a space
;;     llm-run      =  one model execution attempt caused by a turn
;;     transcript   =  passive observed provider/source log
;;
;;   PSTATE NAMING CONVENTION (from existing modules):
;;     PLURAL primary tables:      $$spaces, $$turns, $$compute-runs, $$llm-turn-runs
;;     -by-* secondary indexes:    $$turns-by-space, $$llm-thread-by-space
;;     $$projection-* for views:   $$projection-chat-canvas, $$projection-run-detail
;;
;; ════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════

(ns app.server.rama.kernel)

;; ────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
;;   The defkernel macro — data-only.
;;
;;   Stores the given shape map under the given name as inspectable Clojure data.
;;   Does NOT expand to a Rama defmodule. Does NOT generate code. The 5 existing modules
;;   are hand-written; this macro describes the shape they share, it does not produce them.
;;
;;   Read it as: "this is what we mean when we say 'kernel,' filled in with current examples."
;;
;;   To inspect:  (:kernel/shape KERNEL-SHAPE)
;;                (get-in (:kernel/shape KERNEL-SHAPE) [:claim-depot :present-in])
;; ────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

(defmacro defkernel
  "Describe a kernel as data. STATUS: documentation-only today.

   Stores the given shape map under name. Does not generate a defmodule. Does not
   call anything else. Exists to give the kernel pattern a single inspectable home
   that grows in lockstep with the codebase.

   When new kernels are built or existing kernels change, edit KERNEL-SHAPE below
   to reflect what is now true. Git tracks the evolution.

   See \"WHY defkernel IS DATA, NOT GENERATION TODAY\" in the spec above for why
   this macro deliberately stops at storage."
  [name shape]
  `(def ~name {:kernel/name '~name :kernel/shape ~shape}))


;; ────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
;;   KERNEL-SHAPE — the current observed shape, drawn from the 5 instances
;; ────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

(defkernel KERNEL-SHAPE
  ;; The current observed shape of a Softland kernel, derived from:
  ;;     text-kernel        src/app/server/rama/text_kernel.clj
  ;;     space-kernel       src/app/server/rama/dogfood/space.clj
  ;;     compute-kernel     src/app/server/rama/dogfood/compute.clj
  ;;     transcript-kernel  src/app/server/rama/dogfood/transcript.clj
  ;;     llm-kernel         src/app/server/rama/dogfood/llm.clj
  ;;
  ;; This map is DESCRIPTIVE, not prescriptive. As new kernels are built, edit existing
  ;; entries or add fields here to reflect what's actually there. The :examples sub-maps
  ;; tie each abstract field to its concrete uses; keep them in sync when modules change.
  ;;
  ;; Distribution at the time of this snapshot:
  ;;     intent-only kernels (text, space)              :  2 of 5
  ;;     intent + execution-pair (compute, transcript)  :  2 of 5
  ;;     intent + execution + control (llm)             :  1 of 5
  ;;
  ;; No two kernels use exactly the same subset of fields. That's why the macro is
  ;; deliberately not a code generator yet — see "WHY defkernel IS DATA" above.

  {;; ────────────────────────────────────────────────────────────────────────────────
   ;; INGRESS PARTITIONER  (always present)
   ;; The :hash-by key for the intent depot. Sets event-boundary atomicity:
   ;; every event keyed alike lands on the same task, and multiple PStates can be
   ;; written within one (<<sources ...) without crossing partitions. Cross-key
   ;; writes inside the same source body require re-hashing via (|hash ...).
   ;; ────────────────────────────────────────────────────────────────────────────────
   :ingress-partitioner
   {:required? true
    :examples
    {:text-kernel       :routing/key       ; transitional; text_kernel/routing-key-contract
     :space-kernel      :routing/key
     :compute-kernel    :run/id
     :transcript-kernel :transcript/request-id
     :llm-kernel        :llm-turn-run/id}
    :notes
    "text-kernel and space-kernel currently partition by :routing/key (a transitional
     placeholder, semantically a 'semantic-vector'). The other 3 kernels partition by
     their domain-specific id directly. Open question for after PR 1: should text/space
     migrate to domain-specific keys (e.g. :artifact/id, :space/id)?"}

   ;; ────────────────────────────────────────────────────────────────────────────────
   ;; INTENT DEPOT  (always present — 5 of 5)
   ;; The "do this" request stream. ActionRequests enter here; the topology folds
   ;; them into ActionDecisions, then into KernelEvents.
   ;; ────────────────────────────────────────────────────────────────────────────────
   :intent-depot
   {:required?    true
    :rama-pattern '(declare-depot setup *X-depot (hash-by <ingress-partitioner>))
    :write-via    :foreign-append!
    :ack-level    :append-ack
    :examples
    {:text-kernel       '*text-requests-depot
     :space-kernel      '*space-action-depot
     :compute-kernel    '*compute-depot
     :transcript-kernel '*transcript-depot
     :llm-kernel        '*llm-depot}}

   ;; ────────────────────────────────────────────────────────────────────────────────
   ;; CLAIM DEPOT  (3 of 5)
   ;; Executor's lock — "I'm taking this work." Folds into the run row to flip
   ;; :pending → :claimed. ALWAYS co-occurs with :observation-depot. The pair
   ;; together IS the back-arrow rule made structural.
   ;; ────────────────────────────────────────────────────────────────────────────────
   :claim-depot
   {:required?    false
    :co-occurs    [:observation-depot]
    :rama-pattern '(declare-depot setup *X-claim-depot (hash-by <ingress-partitioner>))
    :present-in   [:compute-kernel :transcript-kernel :llm-kernel]
    :examples
    {:compute-kernel    '*compute-claim-depot
     :transcript-kernel '*transcript-claim-depot
     :llm-kernel        '*llm-claim-depot}}

   ;; ────────────────────────────────────────────────────────────────────────────────
   ;; OBSERVATION DEPOT  (3 of 5)
   ;; The back-arrow. Workers stream observations BACK into Rama; Rama is truth.
   ;; The :retry-mode :all-after setting on the source> is REQUIRED — it lets a
   ;; retried fold resume mid-stream after partial failure without re-spawning
   ;; whatever produced the observations (e.g. without re-spawning a process).
   ;; See feedback_rama_side_effects.md in memory for why this matters.
   ;; ────────────────────────────────────────────────────────────────────────────────
   :observation-depot
   {:required?      false
    :co-occurs      [:claim-depot]
    :rama-pattern   '(declare-depot setup *X-obs-depot (hash-by <ingress-partitioner>))
    :source-options {:retry-mode :all-after}
    :present-in     [:compute-kernel :transcript-kernel :llm-kernel]
    :examples
    {:compute-kernel    '*compute-obs-depot
     :transcript-kernel '*transcript-obs-depot
     :llm-kernel        '*llm-obs-depot}}

   ;; ────────────────────────────────────────────────────────────────────────────────
   ;; CONTROL DEPOT  (1 of 5)
   ;; Out-of-band cancel / steer / approve / compact. NOT folded as a normal
   ;; observation; folded with its own type-dispatching reducer
   ;; (record-control / resolve-approval / cancel-run / add-steer / add-compaction).
   ;;
   ;; Why separate from observation? Observations describe what HAPPENED; controls
   ;; describe what the user/system WANTS TO HAPPEN out of band. Different reduce
   ;; semantics, different ack semantics, different retry expectations.
   ;; ────────────────────────────────────────────────────────────────────────────────
   :control-depot
   {:required?  false
    :present-in [:llm-kernel]
    :examples
    {:llm-kernel '*llm-control-depot}}

   ;; ────────────────────────────────────────────────────────────────────────────────
   ;; TASK-GLOBAL EXECUTOR  (1 of 5)
   ;; A (declare-object ...) inside the module that creates a reactive task-global.
   ;; The executor polls pending PState rows, spawns work (via spawn-if-absent
   ;; registry), reconciles state. Canonical example: compute.clj:587 — the executor
   ;; that runs shell commands.
   ;;
   ;; LLM execution does NOT use this pattern: LLM exec lives OUTSIDE the module as
   ;; helper / runtime fns. So the executor pattern is an option, not a default. It
   ;; only applies to kernels whose work happens INSIDE the Rama process.
   ;; ────────────────────────────────────────────────────────────────────────────────
   :task-global-executor
   {:required?    false
    :present-in   [:compute-kernel]
    :rama-pattern '(declare-object setup *X-executor (X-executor-task-global <executor-opts>))
    :examples
    {:compute-kernel '*compute-executor}}

   ;; ────────────────────────────────────────────────────────────────────────────────
   ;; CROSS-MODULE WIRES  (1 of 5)
   ;; Writes into another kernel's depots from this kernel's topology. Operationally
   ;; concrete in Rama:
   ;;   declare side:  (mirror-depot setup *<their-depot> <their-module> "*<their-depot>")
   ;;   write side:    (|hash$$ <key>) (depot-partition-append! *<their-depot>
   ;;                                                            <record> :append-ack)
   ;;   read side:     (foreign-pstate <ipc> <their-module> "$$<their-pstate>")
   ;;                  declared in start-X-runtime!
   ;;
   ;; The CT framing "natural transformation" is intuition; operationally it's a
   ;; partitioned append with explicit ack. See CT ladder Level 4 note above.
   ;; ────────────────────────────────────────────────────────────────────────────────
   :cross-module-wires
   {:required?  false
    :present-in [:space-kernel]
    :rama-pattern
    {:declare '(mirror-depot setup *<their-depot> (get-module-name <their-module>) "*<their-depot>")
     :write   '[(|hash$$ <key>) (depot-partition-append! *<their-depot> <record> :append-ack)]
     :read    '(foreign-pstate <ipc> <their-module-name> "$$<their-pstate>")}
    :examples
    {:space-kernel
     {:writes-into   {:llm-kernel ['*llm-depot '*llm-control-depot]}
      :reads-via-foreign-pstate
                     {:llm-kernel ['$$llm-turn-runs
                                   '$$llm-views
                                   '$$projection-run-detail
                                   '$$llm-thread-by-space
                                   '$$llm-turn-run-by-turn]}}}}

   ;; ────────────────────────────────────────────────────────────────────────────────
   ;; INTERPRET FN  (always present)
   ;; The accept/reject machinery. Takes a request + existing state, returns an
   ;; ActionDecision (with events if accepted; with reason+errors if rejected).
   ;; This is the "α" half of the F-algebra fold (CT ladder Level 4).
   ;; ────────────────────────────────────────────────────────────────────────────────
   :interpret-fn
   {:required? true
    :signature '(fn [request existing-state] => decision-or-events)
    :examples
    {:text-kernel       "interpret-* fns dispatched by case>: :artifact/ingest,
                         :unit/status-set, :compat/record — see text_kernel.clj"
     :space-kernel      "interpret-* fns dispatched by case>: :space/create,
                         :turn/compose-and-send, :space/fork-from-span,
                         control variants — see dogfood/space.clj"
     :compute-kernel    'interpret-run-command-request
     :transcript-kernel 'request-validation-errors
     :llm-kernel        'interpret-turn-run-request}}

   ;; ────────────────────────────────────────────────────────────────────────────────
   ;; MATERIALIZE FNS  (always present)
   ;; Per-event-type pure fns producing PState writes — the "ETL" half of the
   ;; F-algebra fold. Replay equivalence follows IF the fold is associative; that's
   ;; aspirational, not currently verified (see CT ladder Level 4 STATUS).
   ;; ────────────────────────────────────────────────────────────────────────────────
   :materialize-fns
   {:required? true
    :examples
    {:text-kernel       '[artifact-materialization
                          revision-materialization
                          branch-materialization
                          status-materialization
                          units-by-id-materialization]
     :space-kernel      "many — thread-row, turn-row, bundle, llm-request,
                         object/turn/bundle/llm-run materializations,
                         chat-canvas, object-detail, object-relations projections
                         — see dogfood/space.clj"
     :compute-kernel    '[initial-run-row
                          assign-executor-task
                          pending-entry
                          run-view
                          fold-observation
                          grant-claim]
     :transcript-kernel '[initial-run-row
                          rejected-run-row
                          fold-run-status
                          increment-run-counts
                          conversation-entry
                          source-file-state-entry
                          tool-call-index-rows]
     :llm-kernel        '[initial-turn-run-row
                          fold-observation
                          fold-control
                          upsert-thread-row
                          run-detail-projection]}}

   ;; ────────────────────────────────────────────────────────────────────────────────
   ;; PSTATE SPEC  (always present)
   ;; The PState declarations (name → schema). Naming conventions:
   ;;   PLURAL primary tables:      $$spaces, $$turns, $$compute-runs, $$llm-turn-runs
   ;;   -by-* secondary indexes:    $$turns-by-space, $$llm-thread-by-space
   ;;   $$projection-* for views:   $$projection-chat-canvas, $$projection-run-detail
   ;;
   ;; Some PStates are declared but never written — they're scaffolding for future
   ;; contracts (text-kernel's $$policies and $$projection-cache are the canonical
   ;; examples). Scaffolding is OK as long as it's documented; don't silently leave
   ;; unused declarations behind.
   ;; ────────────────────────────────────────────────────────────────────────────────
   :pstate-spec
   {:required? true
    :counts {:text-kernel       11   ; 9 actively written + 2 scaffold ($$policies, $$projection-cache)
             :space-kernel      24
             :compute-kernel    4
             :transcript-kernel 4
             :llm-kernel        20}}

   ;; ────────────────────────────────────────────────────────────────────────────────
   ;; PROJECTIONS  (variable)
   ;; Pre-rolled views, materialized incrementally on every relevant event. The UI
   ;; subscribes via e/watch — no client-side joining needed.
   ;;
   ;; Heavy in space-kernel (chat canvas, object detail, object relations) and
   ;; llm-kernel (run detail, llm-views). Lighter in compute (run view). Text has
   ;; only scaffold; transcript has none.
   ;; ────────────────────────────────────────────────────────────────────────────────
   :projections
   {:required?      false
    :present-heavy  [:space-kernel :llm-kernel]
    :present-light  [:compute-kernel]
    :scaffold-only  [:text-kernel]
    :absent         [:transcript-kernel]
    :examples
    {:space-kernel      '[$$projection-chat-canvas
                          $$projection-object-detail
                          $$projection-object-relations]
     :llm-kernel        '[$$projection-run-detail
                          $$llm-views]
     :compute-kernel    '[$$compute-views]
     :text-kernel       '[$$projection-cache]   ; declared but unwritten — scaffold
     :transcript-kernel []}}})


;; ════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
;;
;;   HOW TO EVOLVE KERNEL-SHAPE
;;   ─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
;;
;;   When you build a new kernel (#6, #7, ...):
;;     1. Read KERNEL-SHAPE end to end. Find the closest existing :examples row.
;;     2. Build your kernel by copying that example as a template; modify what differs.
;;     3. Come back here. Add an :examples entry for your new kernel to every field
;;        you used. If you used a field nobody else uses, your example will be the
;;        first — that's fine; record it.
;;     4. If you needed a field KERNEL-SHAPE doesn't have, add it. Include:
;;          - what it is (top-level ;; comment block above the field)
;;          - whether it's required or optional
;;          - which kernels use it (:present-in)
;;          - :examples for every kernel that uses it
;;          - any :rama-pattern / :source-options / :ack-level that go with it
;;     5. Commit the new kernel and the KERNEL-SHAPE change in the same PR. Git diff
;;        on this file then tells the story of how the shape grew.
;;
;;   When you change an existing kernel:
;;     - If the change affects a field listed in KERNEL-SHAPE, update the :examples
;;       entry. (If you renamed a depot, the :examples here should reflect the new name.)
;;     - If the change introduces a new pattern (a new way to do something kernels
;;       already did), consider whether KERNEL-SHAPE needs a new sub-field to capture
;;       the variation.
;;
;;   Reading KERNEL-SHAPE diffs over time tells the codebase's structural story. Treat
;;   it like a living index of what kernels ARE; it stays useful only if it stays in sync.
;;
;;   If/when the pattern stabilizes enough to extract a generator macro (see "WHY
;;   defkernel IS DATA, NOT GENERATION TODAY" above), this file is where that macro
;;   would live too — the data form would become the input to the code-generating form.
;;
;; ════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
