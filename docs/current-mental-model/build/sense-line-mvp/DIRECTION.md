# Sense-Line MVP — Direction & Working Map

2026-07-08 · status: PROPOSED working map, Sid redlines · **boots from `docs/current-mental-model/sense-line-model.md` — read that first; this doc is deliberately mid-altitude.** Written by Fable in session `18d63935` on Sid's word ("Let's get started on the docs").

Two of Sid's raw-note rulings are recorded here at raw-note grade (his words, not yet formal decisions): **no full-past-data run** and **not read-only**. Formalization = the two proposed amendments in §5, entering `decisions.md` as PROPOSED on his word.

---

## 0 · SPEC ≠ BENCHMARK — the standing correction (Sid, 2026-07-08)

> "First we need to define and work on what exactly are the node types and the relations. … The end goal of this whole journey would be: what is the spec, what are the different kinds of nodes, what are the relations between these nodes. And once we have this artifact concretely, THEN we move to the benchmarking. … These are two different things: how the model performs on the spec, and what the spec is."

Encoded consequence: the SPEC ROOM's example-chat pass exists to **develop and test the spec itself** (does the schema fit reality?). Gold-marking for benchmarks happens only **after spec v0 freezes**. Any session that converges these two has drifted — re-read this section.

## 1 · Coverage — every area from Sid's notes, and where it lives

| Sid's area (his words) | where it lives | state 2026-07-08 |
|---|---|---|
| "Spec of … the various nodes we are going to tag different parts of replies as" + relations | **SPEC ROOM** (§3.1) | open — first room to run; Sid works closely |
| "What is the architecture of this system?" | two homes: system-level frame = `sense-line-model.md` §architecture-bet (settled at altitude); Rama-level mapping = **RAMA ROOM** (§3.3) | frame settled; mapping open |
| "Are we going to run the whole past data? My answer is no." | ruled by Sid (raw-note grade) — recorded here | forward-capture first; ONE example chat for spec-dev; past re-reads stay optional forever (the log appreciates) |
| "Should we work from the read-only pov? My answer is no." | ruled by Sid (raw-note grade); formal rescope = amendment A2 (§5) | start from existing editor/code; marker writes ride the lawful worker→Rama path |
| "Is the framework ready for webgpu and electric?" | fact gathered (§4.1); scope-recheck flag → **PRODUCT-SIDE ROOM** | contracted + countersigned, NOT built |
| "What do we have on rama side?" | fact gathered (§4.2); contract-grade verification → **RAMA ROOM** | organs exist; 4 gaps = the sense layer |
| "What the ui will be" — designer-first, Sid's full attention | **DESIGN ROOM** (§3.2) | opens on direction+canon now; needs one hand-marked example (from SPEC ROOM) for grain-feel fixtures; ui-design-pass opening prompt needs a v2 under sense-line-first |
| "Do we have the benchmark apparatus?" + the two benchmark types + the unit question | fact gathered (§4.3); **BENCHMARK ROOM** (§3.4) | apparatus exists (LM-1); runs AFTER spec v0 |

## 2 · Dependency map

```
THIS SESSION ─── sense-line-model.md (boot doc, top stratum)
      │          DIRECTION.md (this working map)
      │          CLAUDE.md §Session-Registers + memory (workflow fix)
      │
      ├────────────► SPEC ROOM  ── Sid+Fable, close iteration; BRANCH of this chat
      │                │          OUT: SPEC.md — block grammar · node kinds · relations
      │                │               (developed by reason + applying to ONE example chat)
      │                │
      │                ├── spec v0 ──────────► RAMA ROOM      (fresh; /rama skill;
      │                │                        reader · block grain · consolidator ·
      │                │                        morning-answer projection · return path)
      │                │
      │                ├── spec v0 + gold ───► BENCHMARK ROOM (fresh; bench-1 marker-vs-gold;
      │                │                        bench-2 self-marking effect = fork-decider)
      │                │
      │                └── one hand-marked ──► DESIGN ROOM    (fixtures: grain & kinds
      │                    example                             become feel-able in render)
      │
      ├────────────► DESIGN ROOM boot        (Sid's attention; starts NOW from
      │                                       direction + design canon; fixture-dependent
      │                                       work waits on SPEC ROOM's example)
      │
      ├────────────► AMENDMENTS A1/A2        (Sid's hand; §5; enter decisions.md as PROPOSED
      │                                       on his word)
      │
      └────────────► PRODUCT-SIDE ROOM       (exists; face-2 scope recheck under
                                              sense-line-first — ruled there, not here)
```

Rendered version: `deps.md` (same DAG as a diagram). Plain-English dependencies: **the SPEC ROOM gates three rooms** (rama, benchmark, design-fixtures). Design's *systems exploration* and the amendments and the face-2 recheck depend only on this session's docs. Nothing anywhere depends on ingesting the past.

## 3 · The rooms

### 3.1 SPEC ROOM — the spec itself (Sid + Fable, closely; branch of this chat)
Definition of done (Sid's words): the artifact stating **what the spec is — what kinds of nodes, what relations between them**, plus the block grammar (what a good chunk break looks like). Method: reason-based discussion + applying candidates to ONE example chat via Fable's manual pass, iterated until it fits. Layering discipline (from the schema deaths of 2026-07-07): combine by LAYERS, never by merging — block (material unit, stored as **address+offsets into raw text, never copies**) → mark kinds (folksonomy; scaffold start: tension/claim/question/verdict; grown by use) → two edge families kept separate (epistemic: supports/opposes/informs · process: based-on/supersedes/produced) → episode (slow-pass grouping + yield). DG vocabulary = the epistemic family; episode = the process grammar; ologs = notation for writing the schema down, not a rival ontology. Chunking and labeling stay separable stages (a re-run of either must not break the other).

### 3.2 DESIGN ROOM — the UI (Sid's full attention; fresh session(s); designer-first)
Sid's learning from the trail view: designers first, then implementation. Boots from this doc + the design canon; the existing ui-design-pass opening prompt (2026-07-06) is pre-break and needs a v2 reflecting sense-line-first. Grain and kinds are only judgeable when SEEN — the SPEC ROOM's hand-marked example becomes fixture material here; expect spec↔design iteration, not a one-way handoff.

### 3.3 RAMA ROOM — the mapping (fresh; /rama skill; contract-grade)
Needs spec v0. Scope: block grain over stored raw replies (sub-entry spans — verify what transcript ingest stores today); marks as provenance-first assertions (the relation kernel's `asserted-by`/proposed pattern is already the silver/gold substrate — the missing noun is the block as addressable target); the consolidator (episode closure); the morning-answer projection; reader-worker placement (intent+executor pattern for anything OS-touching); the return path's assembly point. Also rules in-line vs trailing capture for the pipe (informed by bench-2).

### 3.4 BENCHMARK ROOM — marker performance (fresh; after spec v0; apparatus exists)
Unit = the block (Roam-block/paragraph grain; store the raw full reply AND the blocks; "parsing, not reading"). Bench-1: local marker vs Fable's gold marking of the same chat — seeded by the first post-spec gold pass; corrections in live use keep feeding it (the river generates its own eval set). Bench-2 (Sid's design, elevated): does asking the harness model to self-mark its replies (citation-style structure) improve or degrade the work itself? **This decides where the mint lives for agent output** — author-mint (git-style, at write) vs reader-mint (separate marker). Note the asymmetry: Sid's side of the river is never author-minted; no ceremony on the human is the constraint the architecture stands on. Budget rule stands: local + subscription, no API dollars.

### PRODUCT-SIDE ROOM (exists, not opened by this map)
Carries: face-2 scope recheck under sense-line-first (likely survives — it is substrate: store, camera, birth laws — but the check is that room's, one paragraph there).

## 4 · Facts gathered (2026-07-08, Fable; orientation-grade — contract-grade re-verification belongs to the owning rooms)

**4.1 Framework (WebGPU + Electric):** face-2 contract v1.1 exists and is COUNTERSIGNED; three builder-lane prompts drafted (`build/face-2/LANE_PROMPTS_DRAFT.md`); the build wave NOT started — dispatch halted, belongs to a product-side session. So: ready-to-build, zero built.

**4.2 Rama side:** organs that exist — md + transcript ingestors (landed/hardened Jun 2026); git-spine (commits, parents, lineage, stats); relation kernel (typed edges, `asserted-by` provenance, /assert route, activity query) — closed and gate-passed; object-container address space (containers + content hashes); trail-view projection module; trail-room render face (R-1/R-2); intent→executor pattern for OS side effects; local model serving via llama-server (OpenAI-compatible; used by LM-1 bench); claude -p / codex spawning demonstrated **as bench apparatus** (promoting it to a runtime organ is real work, not a checkbox). **The four gaps — all of them the sense layer itself:** (1) the reader/marker (nothing marks the river), (2) block grain (sub-entry spans not first-class — verify), (3) the consolidator (no episode closure), (4) standing answers + the return path (no morning answer, no briefing assembly). Substrate verdict: sufficient as far as orientation-grade reading shows; the entire gap has the same shape as the thesis.

**4.3 Benchmark apparatus:** exists and warm from LM-1 — runner, pre-registration discipline, grading pipeline, local serving. The paused model sweep does not block marker benchmarks. New question bank needed once spec v0 fixes kinds.

## 5 · Proposed decision amendments (full text; enter decisions.md as PROPOSED on Sid's word; ruled by Sid only)

**A1 — amend D-002 (first form = the trail view): rescope the UNIT.** The first form's unit moves from containers (sessions/files/commits as peers) to sense-line units (episodes and their marks), per the 2026-07-07 form-break (`atomic-unit-2026-07-07.md` §1: container granularity cannot meet D-002's stated goal "seeing it makes sense of like a decision tree"). The container trail demotes to one evidence-lens. D-002's goal and View-3 agent-legibility survive unchanged; what changes is the unit the view renders.

**A2 — amend D-008 (read-only MVP): rescope the WRITE boundary.** The machine marker writes marks as provenance-first observations via the lawful worker→Rama path (back-arrow compliant; the view still never mutates truth). Human write-gestures through the UI remain gated as before until spec'd. Grounds: Sid 2026-07-08 ("should we work read-only? my answer is no — we already have code written out; start from where we are"), and D-008's original rationale (view-mutation danger) is untouched by machine observations.

## 6 · Sequencing & delegation

Order: this session's docs+workflow batch (done) → SPEC ROOM (gates three rooms) → design fixtures / rama mapping / benchmarks as the tree unlocks them. Nothing runs the whole past. Delegation split (Sid's): rooms produce check-this/do-this work and analysis for Fable and cheaper models; every RULING — amendments, names, grain verdicts, design picks — routes to Sid, Fable arguing but never deciding.

## 7 · Deliberately not done here

No commits (Sid's word required; docs stay on the local docs branch, docs-only commits, never pushed). No decisions.md append yet (§5 awaits his word). No spec content pre-empted (that's the SPEC ROOM's, with him). Pending small honesty item, separate approval: CLAUDE.md Terminology still carries the dead `Q→C→E→D→R→F` string (superseded by the verified Q/C/E/Source base) — one-line fix awaiting Sid's explicit OK.
