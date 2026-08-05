# Softland Design Research — Charter (Claude Track)

Status: stable constitution for Claude's design-research track, 2026-06-09.

This is Claude's own independent cut. It is written clean-room — from the vision
and first principles — not derived from any other track's work. Keep it that way
(see Independence).

This track is **research**: studying the design fields that can inform Softland's
views, each through the Softland lens, so that a non-designer comes out with real
design vocabulary and direction. The output is **synthesized understanding**, not
built screens. Building actual views is a later application phase — explicitly not
this track.

This file is the constitution: the lens, the method, the machine, the spine of
research areas, the quality bar, the boundaries. It changes rarely. Live state —
which areas are done, in flight, or open — lives in `orientation.md`.

## The lens

Softland is a place, not a tool — "a world for holding understanding in public
form," explorable at continuous semantic zoom, inhabited rather than operated,
where disagreement is preserved until synthesis. Every area is read *through this
lens*: not "what is good design in field X," but "what does field X teach a world
that holds understanding in public form."

Keep current and near-future engineering (the object-container kernel, the
ingesters) in mind as *context* — it is not the deliverable here. Do not let the
near-term import work hijack the research.

## The method — study areas, raid through the lens

The unit of work is a **design area** — a field or tradition with its own masters,
works, and tools (photography, film, music, architecture, games, HCI,
visualization, malleable software, …). Your links seed each area; we go *deeper
than the seeds* — surface-level is failure.

This deliberately indexes by field, not by problem: at the research stage the
giants are organized by field, and the cross-cutting design *principles*
(orientation, pacing, provenance, calibration, plurality, …) are the **output we
synthesize**, not the starting unit. Giants for the parts; first-principles
assembly happens at synthesis.

## The machine — how every area is worked

```text
Map        the area: its masters, key works, tools (seed links + expand deeper)
Extract    the transferable design principles it has SOLVED
Transpose  what Softland derives through the lens — and the vocabulary / moves it
           gives a non-designer
Guard      what is dangerous or misleading to borrow
Open       the questions it raises and where to dig further
```

Each area's output is a dossier (these five sections). When two or more areas
exist, their principles synthesize into `synthesis.md` — Softland design
direction at the meta level. That synthesis is the "enough to have some design"
deliverable.

## The quality bar — what makes a dossier real

```text
Depth      goes beyond the seed links; surface-level is failure
Principle  extracts transferable insight, does not merely describe
Lens       every area connects explicitly to the Softland lens — a survey that
           does not transpose is decoration
Honesty    flags inference vs studied; flags dangers, not only borrowables
Usable     yields vocabulary and direction a non-designer can actually wield
```

## The spine — the research areas

Each area is one dossier. Folder names are semantic; a folder is created only when
the area is picked up. This is the current cut — trim, merge, or add freely.

```text
hci-infovis          making large structured knowledge legible, navigable, calibrated.
  seeds: Shneiderman ("insight not pictures"), Munzner (Nested Model), Bederson
         (treemaps/ZUI), Inselberg (parallel coords), t-SNE/UMAP, Kittur.

spatial-wayfinding   what makes a built space a PLACE you can orient in.
  seeds: Kevin Lynch (Image of the City), Christopher Alexander, cartography/GIS.
         (no user links yet — to add.)

game-worlds          making huge spaces inhabitable; teaching navigation through play.
  seeds: Jonathan Blow; + level design, game feel, Souls-like interconnection,
         minimaps / fog-of-war, diegetic UI.

cinematic-lens       directing attention through time and frame; replay as a medium.
  seeds: + montage (Eisenstein), film/video editing, cinematography, photography
         (the decisive moment, the series), Lightroom, video scrubbing.

music-sound          temporal structure, rhythm/pacing, transformation-as-patching.
  seeds: + musical form (tension/release), Ableton/DAWs, modular synths, sonification.

pro-creative-tools   density without chaos; modes, inspectors, node graphs,
                     non-destructive pipelines, multiplayer.
  seeds: Figma; + DaVinci, Houdini/Blender, Lightroom.

malleable-software   software-as-place, renovation, end-user programming
                     (closest to Softland's soul).
  seeds: Ink & Switch, Geoffrey Litt, Folk Computer (Omar Rizwan),
         malleable.systems, WonderOS, Jackson Dahl.

explorable-thinking  the REPL for knowledge; models you can break/play;
                     rediscovery over consumption.
  seeds: Nicky Case, Loopy, Growable; + Bret Victor, Distill.

collab-ai-native     beyond cursors; preserved disagreement; networked thought;
                     public form / handoff.
  seeds: Liveblocks/Fermat, Prezi, Notion; + Engelbart/Bush/Nelson, Roam/Obsidian,
         CRDT critique, argument mapping.
```

## Boundaries — forcing functions

```text
1. Research, not building. No screens, no probes, no substrate maps here.
2. Go deeper than the seed links. A dossier that only restates the links failed.
3. Always transpose through the lens. A survey with no Softland bridge is decoration.
4. Meta is allowed but bounded: each area uses the fixed dossier shape
   (Map -> Extract -> Transpose -> Guard -> Open), not infinite sprawl.
```

## Independence

This is Claude's canonical perspective, kept deliberately uncontaminated. Do not
read parallel design tracks or prior design-research docs to "align" — that
dilutes the independent cut and destroys the disagreement that makes parallel
tracks worth running. Form views from the vision and from the source fields
themselves. Cross-track synthesis is a separate, later act the user runs.

## Where things live

```text
charter.md          this file — constitution (lens, method, machine, spine, bar, boundaries)
orientation.md      live router — area status + how a new session enters and picks
start-prompt.md     paste into a new session to enter this track correctly
<area>/dossier.md   one folder per research area, created when it is picked up
synthesis.md        cross-area Softland design direction (created when 2+ areas exist)
```
