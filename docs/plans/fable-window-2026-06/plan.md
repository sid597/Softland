# Fable Window Plan — June 11 → June 22, 2026

**Premise**: free Fable access ends June 22. The goal is not "do the most work" — it's "do the work only a top model can do, and leave everything else in a state where Codex (or cheaper Claude) can finish it after the window."

**Fable's comparative advantage** (from 5 months of your own corpus): falsification gates, cross-corpus synthesis, and pattern-setting first instances. Routine implementation against a ratified spec is exactly what you already trust Codex with ("claude draws broadly then we get it into correct shape with codex" X-1043 — and the inverse holds for execution).

**Pacing reality**: you run a 30h/week day job (C-0599) and the corpus shows a 2.5-week gap in late May. This plan has 4 anchor deliverables, not 10.

---

## The four anchor deliverables (what exists on June 22)

1. **Code ingestor: ratified → gated → built** (slice 1: repo baseline + commit watch, file-level). The third ingestor completes the loop that lets Softland hold its own code — the prerequisite for zoom-100 and trail-to-code.
2. **Compute module hardened to the retro pattern** (Batch 0 + Batch 1, with adversarial probes) — the pattern-setter every later batch copies. If the pattern is set wrong, every post-window fix inherits the flaw.
3. **The living architecture map** — the explorable HTML artifact answering "what is the architecture now?", your single most repeated ask (~15+ times). Wall-printable, as requested (X-0934).
4. **Every in-flight thread Codex-resumable** — handoff files, gated specs, and the open-threads inventory, so July loses zero context.

---

## Day-banded plan

### Days 0–1 (Jun 11–12) — Decide and unblock. Nothing else matters until this is done.
The spine is blocked on ~60 minutes of YOUR decisions, not on any model:
- **Ratify the code-ingestor 6 clusters + 2 amendments** (`build/code-ingestor/index.html`, checklist at the end).
- **Rule on F5/F6**: dead, or folded into retro Batch 5. One sentence; the docs currently contradict you.
- **Rule on the LLM chat track** (B1): revive inside the window, or formally park with a one-line displacement note. It was "the unlock" on May 2 and fell silently.
- Then fire the **Codex falsification gate** on the ratified PRODUCT.md (paste-ready prompt exists per next-prompt.md).
- Parallel lane: kick off **retro Batch 0** (cross-cutting guards) — it's mechanical enough to start while the gate runs.

### Days 2–5 (Jun 13–16) — Pattern-setting builds. Fable on falsification, Codex on execution.
- **Code ingestor Rama Phase 0 → implementation** under /rama + /rama-pitfalls. Fable's job: IMPLICIT_SPEC, plan validation, and adversarial probes (the phases that were weakest pre-skill per the retro). Codex's job: the mechanical phases.
- **Compute Batch 1** (microbatch conversion + submit dedup) with Fable doing the falsification pass against the retro findings. This sets the pattern for Batches 2–5.
- **Codeq/versioning synthesis** (small, ~half a session): the research is fetched, you said "yes please lets synthesize" (C-0956) and it never ran. Output as HTML, not md. It feeds the ingestor's versioning decisions directly.

### Days 6–9 (Jun 17–20) — Synthesis only Fable can do.
- **The living architecture map**: one full Fable session reading the actual code (5 kernels, 3 ingestors, executors, depots/PStates) + UNIFIED-RETRO + contracts, producing an explorable HTML site: the loop diagram, per-module status (done/hardened/unsafe), and where each retro batch lands. This kills the #1 recurring frustration and becomes the orientation artifact for every future session.
- **Design-track "biggest version" research** (X-1390): one deep-research run on ZUI/semantic-zoom at full scale + the cat-theory design lens (X-1393), with the cost posture pre-decided: skip adversarial verify (your standing preference, C-0870/C-0952), one synthesized voice (not anthology, C-0935), HTML output. This is the most Fable-priced work in the whole inventory — after June 22 this research depth is what you lose.
- **LLM-track status audit** (only if you ruled "revive" on Day 0–1): one session to establish which of the 12 slices landed, then a scoped vertical (e.g., fork-from-turn through Rama) — NOT the full UI.

### Days 10–11 (Jun 21–22) — Exit hardening. Treat the deadline as a handoff, not a finish line.
- Every open thread gets a current next-prompt or build-folder handoff (the thing you asked for in your literal last message, C-0964).
- Retro Batches 2–5 queued as mechanical work-orders for post-window Codex (pattern + probes already set by Batch 1).
- Final commits (code/docs separate, as always). Update MEMORY.md pointers.
- Write the **post-window operating doc**: what Codex executes alone, what waits for top-model access, what you decide.

---

## Do NOT spend Fable on this

- Retro Batches 2–5 execution once Batch 1 sets the pattern → Codex.
- Roam importer implementation → post-window, against the ingester contract.
- UI bug backlog (selection, focus, flicker, scroll clamp) → mostly overtaken by the pivot; close as OBE or park.
- MacBook Rama setup → any model, any time.
- Doc reorganizations, re-running deep-research verify loops, re-litigating ratified decisions, "improve the ascii art" iterations — all of these burned window-time in the corpus.

## Decisions only you can make (the real bottleneck)

| # | Decision | Blocks | Size |
|---|---|---|---|
| 1 | Ratify code-ingestor clusters + 2 amendments | The entire spine | ~45 min |
| 2 | F5/F6: dead or retro-Batch-5 | Doc coherence, transcript batch scope | 1 sentence |
| 3 | LLM chat track: revive or park | Days 6–9 allocation | 1 paragraph |
| 4 | Token posture for design research | Whether Day 6–9 research runs at full depth | 1 sentence |

---

## Appendix: scoring (2×F + U + I + R + D, max 18)

F = Fable leverage (needs top-model depth) · U = unblocking power · I = your stated intensity (repeated asks/frustration, cited) · R = readiness (teed up now) · D = decay cost (lost if not done in window)

| Thread | F×2 | U | I | R | D | Σ | Size |
|---|---|---|---|---|---|---|---|
| Code ingestor (gate → Phase 0 → slice 1) | 6 | 3 | 3 | 3 | 2 | **17** | 3–4 sessions |
| Codeq/versioning synthesis | 6 | 2 | 3 | 3 | 3 | **17** | ½ session |
| Retro Batch 0 + compute Batch 1 | 6 | 3 | 2 | 3 | 2 | **16** | 2–3 sessions |
| Living architecture map (HTML) | 6 | 2 | 3 | 2 | 3 | **16** | 1 session |
| Design "biggest version" ZUI research | 6 | 1 | 2 | 2 | 3 | **14** | 1 session |
| LLM chat/forking vertical | 4 | 3 | 3 | 1 | 1 | **12** | 2+ sessions (cold) |
| Back-arrow UI slice | 4 | 2 | 2 | 1 | 1 | **10** | cold, post-window |
| Roam importer | 2 | 1 | 1 | 2 | 1 | **7** | post-window |
| UI bug backlog / slug controls | 2 | 1 | 1 | 1 | 1 | **6** | close-as-OBE |
| MacBook Rama setup | 2 | 1 | 1 | 1 | 1 | **6** | any time |

Notes: the LLM chat vertical scores below the spine only because R=1 (cold respin) and D=1 (equally buildable in July against existing gated contracts) — NOT because it matters less. That's exactly why it needs an explicit revive-or-park ruling instead of another silent displacement.
