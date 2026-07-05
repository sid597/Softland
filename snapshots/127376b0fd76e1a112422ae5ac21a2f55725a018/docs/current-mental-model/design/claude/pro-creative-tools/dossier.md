# pro-creative-tools — Dossier

Status: drafted, 2026-06-09
Seed links: Figma (named, no link). + Lightroom, DaVinci Resolve, Houdini/Nuke,
Blender, Ableton-as-tool, the IDE/Vim/command-palette (added). Sources named
from knowledge, not freshly re-fetched.

## Map

How expert software delivers density, speed, and power *without* chaos.

- **Figma** — multiplayer-native (ambient presence, no "save"); **components +
  variants + auto-layout** (one master, many instances, constraints);
  selection-driven inspector; one document, many views; plugins. A shared object
  graph everyone edits live.
- **Adobe Lightroom** — **non-destructive develop** (edits are instructions over
  the preserved raw negative); the catalog (one library, many collections); the
  histogram as live instrument; presets. The Zone System, digitized.
- **DaVinci Resolve** — **node-based color grading**; **pages as modes** (edit /
  color / fusion / fairlight — task-tuned workspaces inside one app); **scopes**
  (waveform / vectorscope as ground-truth readouts).
- **Houdini / Nuke / Blender / TouchDesigner** — **fully procedural node graphs**:
  change a parameter upstream and everything downstream recomputes live —
  non-destructive + live at the architecture level.
- **The IDE / Vim / Emacs** — **modal, composable command language**;
  keyboard-speed; the **command palette** (fuzzy access to every action); LSP
  semantic navigation.

## Extract — the principles they solved

1. **Density without chaos via modes/workspaces.** Distinct task-tuned
   environments inside one document (Resolve pages, Blender workspaces) — depth
   without a cluttered single screen.
2. **One model, many views/instances.** A single source of truth projected into
   many views and reused via instances (Figma components, Lightroom catalog).
3. **Non-destructive, recomputable pipelines.** Edits are instructions over
   preserved source; change upstream, downstream recomputes (Lightroom, Houdini).
4. **Node graphs for procedural power** — complex transformation as an
   inspectable, parameterized, recomputable graph.
5. **Scopes / ground-truth instruments.** A calibrated readout (waveform,
   histogram) trusted over the subjective image; experts trust instruments.
6. **Composable command language + palette.** Power via a small composable
   vocabulary and fuzzy access to everything — not menu-diving.
7. **Components/presets: crystallized decisions, instanced.** Decide once, instance
   everywhere; update the master, all update.
8. **Ambient multiplayer over a structured model** (Figma).

## Transpose — what Softland derives (through the lens)

- **Modes/workspaces = how to hold many grammars in one place without chaos.**
  Softland must carry visual + temporal + transformational + plurality grammars.
  Resolve's "pages" say: don't cram them into one screen — offer *task-tuned
  workspaces within one world* (one world, several inhabitable modes). This is the
  concrete answer to pacing + density, and it matches "commands → ritual → place":
  a mode is a crystallized workflow.
- **Non-destructive recomputable pipeline = Softland's Rama/projection model,
  already validated as UX.** Lightroom/Houdini *ship* the architecture Softland's
  engineering wants — immutable source + instruction layers + live recompute =
  event-sourcing + projections + back-arrow. So the back-arrow model isn't exotic;
  it's how professionals already work. Major de-risking of the whole runtime bet.
- **Scopes = the calibration instrument.** "The map must not lie" gets a buildable
  form: a *scope* — a calibrated, separate readout of epistemic state (grounded /
  contested / stale), trusted like a waveform, distinct from the persuasive
  content surface.
- **Components/instances = local worlds + identity across projections.** Figma
  master/instance (identity survives instancing; detach / override / push) is the
  functor in tool form, with a working interaction model for local-world reuse and
  the merge-up loop.
- **Command palette = "commands are larval workflows."** "Verb first, place later"
  has a proven UI: the palette is where intent enters as a verb before maturing
  into a ritual/mode. Softland's command surface should be a fuzzy palette that
  *graduates* frequent verbs into modes.
- **Ambient presence over a structured model** is the bridge to "beyond cursors":
  Figma works because presence rides a *structured* object graph — which is what
  makes conflicts legible rather than mere cursor-collisions (see collab-ai-native).
- **Vocabulary:** modes/workspaces, one-model-many-views, non-destructive pipeline,
  node graph, scopes/instruments, command palette, components/instances, ambient
  presence.

## Guard — what is dangerous to borrow

- **Pro tools assume expert users who invested in learning** — modal depth is a
  *steep* slope, the opposite of the gentle slope (malleable-software). Softland
  must decide which surfaces are expert vs. inhabitable-for-all. **This is a
  central architecture fork between two dossiers.**
- **Modes can fragment "one world."** Too many workspaces and continuous-zoom
  unity dies; modes must feel like rooms in one place (identity must persist
  across them).
- **Node graphs overwhelm** (Houdini is famously hard) — power ≠ inhabitability;
  the patching/graveyard risk again.
- **Instruments only help if trusted** — designing the *culture of trust* in the
  scope is as hard as the readout.

## Open — questions this forces

1. **What are Softland's "pages/modes,"** and how do they stay rooms-in-one-world
   rather than separate apps?
2. **Is the non-destructive recompute pipeline the *inhabitant-facing* model**,
   not just the backend? (If yes, Lightroom/Houdini already prototype Softland UX.)
3. **What is Softland's "scope"** — the calibrated epistemic instrument trusted
   over the persuasive surface?
4. **How do the gentle slope and modal pro-depth coexist** — which surfaces are
   for-all vs. expert-mode? (A top-level architecture decision.)
