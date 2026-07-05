# collab-ai-native — Dossier

Status: drafted, 2026-06-09
Seed links: Liveblocks/Fermat, Prezi, Notion, Jackson Dahl (Dialectic), Litt.
+ Engelbart, Bush, Nelson, IBIS/Toulmin, Wikipedia, Topos/ologs (added). Sources
named from knowledge, not freshly re-fetched.

## Map

Collaborative + AI-native knowledge tools — beyond "cursors of all the people."

- **Engelbart — "Augmenting Human Intellect" (1962), Mother of All Demos (1968)**
  — *collective IQ*; co-evolution of humans + tools; the goal is raising group
  problem-solving capacity, not convenience. **Bush — Memex (1945)**: associative
  trails. **Nelson — Xanadu**: bidirectional links, transclusion, reuse that
  preserves origin; "intertwingularity."
- **CRDT / OT / local-first** — the technical layer of real-time collaboration.
  The user's own critique: CRDT is "the first layer of abstraction over 'here are
  cursors of all the people'"; conflict-as-bad *at the current abstraction level*.
- **Figma / Fermat / multiplayer canvases / Prezi** — ambient presence; AI-native
  canvases (Fermat: AI tools as objects on a board); Prezi's ZUI structure.
- **Networked thought — Roam / Obsidian / Notion** — bidirectional links, block
  references, transclusion; the "tools for thought" renaissance. The user's own DG
  plugin lives here.
- **Argument / debate mapping — IBIS (Rittel), Toulmin, Kialo, Compendium** —
  structure disagreement: issues → positions → arguments; claims → grounds →
  warrants → rebuttals. *Conflict as structure.*
- **Wikipedia / peer review** — large-scale async knowledge collaboration; **talk
  pages** (the argument *behind* the article); edit history as provenance.
- **AI-native (Jackson Dahl "Dialectic," Litt)** — LLM as muse / dialectic
  partner; AI interaction as material, not final answer.
- **Topos Institute / ologs (Spivak)** — collective intelligence as *model
  alignment*, composing partial models, not one big mind.

## Extract — the principles they solved

1. **The goal is collective IQ, not convenience.** (Engelbart) A higher bar than
   "multiplayer editing."
2. **Trails + bidirectional links + transclusion, origin preserved.** (Bush /
   Nelson) Reuse keeps provenance; links go both ways; quote without severing.
3. **CRDT/real-time is the floor, not the ceiling.** It solves *mechanics*
   (conflict-free merge), not *disagreement of meaning*.
4. **Structure disagreement explicitly.** (IBIS / Toulmin / Kialo) Conflict as
   navigable structure, not a comment thread.
5. **The argument *behind* the artifact is first-class.** (Wikipedia talk pages,
   edit history) Process and dispute are content, not exhaust.
6. **AI as dialectic partner / material** — a perspective to compose with, output
   you can bend.
7. **Collective intelligence = aligning/composing partial models** (Topos), not
   merging into one; preserve difference until synthesis.

## Transpose — what Softland derives (through the lens)

- **This field carries "conflicts are good for progress" — and confirms it's real,
  hard, and mostly unsolved.** The user's thesis (CRDT = cursors+1; raise the
  abstraction and conflict becomes productive) is *exactly* the gap between CRDT
  and argument-mapping. Softland's job is the layer above CRDT: **preserved
  disagreement as structure** — and IBIS/Toulmin are the proven grammars to borrow
  (issues/positions/arguments; claims/grounds/warrants/rebuttals). The most direct
  external support for the plurality grammar and "preserved disagreement +
  synthesis as a native artifact."
- **Engelbart's collective-IQ bar reframes the collaboration model.** "Requests
  flow down, software flows up" is an Engelbart co-evolution loop, not a Google
  Docs clone. The bar is "does the group get smarter," not "multiple cursors."
- **Bush/Nelson: trails + bidirectional links + transclusion = provenance +
  connection.** "Reuse must preserve origin" = "fold don't sever" + SourceAnchors.
  Transclusion (live quote-without-copy) is how one object appears in many local
  worlds without duplication — the identity/functor requirement, in link form.
- **Talk-page + edit-history-as-content ↔ "attention itself becomes content" /
  "the event log is primary."** Wikipedia already treats the argument-behind and
  the full history as first-class — exactly "rendered state is projection; the
  process is truth." A working precedent for inhabitable provenance and dispute.
- **AI-native dialectic = multi-LLM + "material not output."** Dahl/Litt show
  AI-as-perspective-to-compose-with. Softland's multi-LLM (Claude/Codex preserved
  separately, synthesized later) *is this design track itself* — the `claude/` vs
  `codex/` split is a live instance of structured preserved disagreement, and
  should be rendered with the argument-mapping grammar.
- **Topos / model-alignment = the formal target for synthesis.** Synthesis =
  composing partial models along shared structure (colimit), not averaging — the
  categorical framing the user already holds; argument-mapping is its UI form.
- **Vocabulary:** collective IQ, the Memex trail, bidirectional link / transclusion,
  CRDT-is-the-floor, IBIS / Toulmin, the talk page, dialectic / AI-as-material,
  model alignment.

## Guard — what is dangerous to borrow

- **Argument maps are notoriously unused** — IBIS / Compendium have decades of
  "great in theory, dead in practice." Too much structure and no one fills it in.
  Softland must make disagreement-structure *emerge cheaply* (LLM-assisted), or
  inherit the graveyard.
- **"Conflicts are good" can romanticize noise.** Not all disagreement is
  productive; some is error or talking-past. Softland must distinguish *generative*
  tension from noise (the dynamics layer).
- **CRDT solves the wrong problem well** — shipping flawless merge and calling
  collaboration "done" leaves the hard part (meaning-level disagreement) untouched.
- **Preserved disagreement fights handoff/public-form** (same shape as malleable):
  N preserved perspectives can become unnavigable. Synthesis must be *reachable*,
  or plurality becomes paralysis — late-bound consensus needs a binding mechanism,
  not just preservation.
- **AI-as-partner can homogenize** (everyone nudged toward the model's prior) — the
  opposite of plurality; multi-model is only a partial guard.

## Open — questions this forces

1. **Can disagreement-structure (IBIS/Toulmin) be *generated* cheaply
   (LLM-assisted)** so it escapes the argument-map graveyard? (Make-or-break for
   the plurality grammar.)
2. **What distinguishes generative tension from noise** — which conflicts does
   Softland preserve, and how is that calibrated? (The dynamics-layer question.)
3. **What is the binding/synthesis mechanism** that turns preserved disagreement
   into a new artifact (colimit) without flattening — and when does it fire?
4. **Is the `claude/` vs `codex/` design split the first real test case** of
   Softland rendering structured preserved disagreement — dogfooding the plurality
   grammar on this very research?
