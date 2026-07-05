# Softland Design Research — Orientation (Claude Track)

Status: live router for Claude's design-research track, updated 2026-06-09.

Purpose: orient a new Claude session entering this track, then help it pick ONE
research area instead of trying to study the whole design world in one pass. A
router, not a backlog.

Read `charter.md` for the constitution (the lens, the method, the machine, the
quality bar, the boundaries). This file is the volatile part: which areas are
done, in flight, or open.

## Independence (read this first)

A parallel design track exists under a sibling folder. **Do not read it**, and do
not read prior design-research docs to "align." This track is Claude's own
clean-room cut; absorbing other tracks contaminates it. Study the source fields
themselves, through the Softland lens. The user synthesizes across tracks later.

## What this track is

Research, not building. Each goal is a **design area** studied through the Softland
lens; the output is a dossier (Map -> Extract -> Transpose -> Guard -> Open). When
two or more dossiers exist, their principles synthesize into `synthesis.md` —
design direction at the meta level. No screens, no probes here.

## Read order

```text
1. charter.md                    the constitution (lens, machine, quality bar)
2. orientation.md                this file
3. <the area you picked>/dossier.md
4. synthesis.md                  once it exists
```

Do not do a multi-document orientation dump. Read the charter, pick an area, then
go research that area itself (its masters, works, tools) — deeper than the seed
links.

Project rule: never commit `.md` files.

## Area status

| Area | Studies | Status |
|---|---|---|
| hci-infovis | legibility, navigation, calibration of structured knowledge | drafted |
| spatial-wayfinding | architecture / cartography — what makes a place orientable | drafted |
| game-worlds | inhabiting huge spaces; teaching navigation through play | drafted |
| cinematic-lens | film / photography — attention through time and frame; replay | drafted |
| music-sound | temporal structure, rhythm / pacing, transformation-as-patching | drafted |
| pro-creative-tools | Figma & pro pipelines — density, modes, node graphs | drafted |
| malleable-software | software-as-place, renovation, end-user programming | drafted |
| explorable-thinking | the REPL for knowledge; break / play models | drafted |
| collab-ai-native | beyond cursors; preserved disagreement; public form | drafted |

Status vocabulary: `not started` · `mapping` · `drafted` · `synthesized` ·
`deferred`. This table is the handoff — keep it true.

**All 9 drafted + synthesized → `synthesis.md` (2026-06-09).** Read that first
for the cross-field design direction (convergent laws, buildable primitives,
de-risking, and the four forks). The dossiers are the depth behind it.

## How to pick

```text
Pick ONE area per session unless the user asks for synthesis.

If unsure, two natural starts:
  hci-infovis        the most direct field; sets shared vocabulary the others lean on.
  malleable-software the closest to Softland's soul (software-as-place).

Otherwise pick whichever area the user is drawn to — feeling is a valid compass.
```

When you start an area, create `<area>/dossier.md` from the template below and
set its row above to `mapping`.

## Dossier template (copy into each area folder)

```text
# <area> — Dossier
Status: <mapping | drafted | synthesized>, <date>
Seed links: <the user's links for this area>

## Map
the field: its masters, key works, tools. Start from the seed links, then go
deeper — name what the field actually is and who solved what.

## Extract
the transferable design principles this field has SOLVED (not a description —
the reusable insight).

## Transpose
what Softland derives through the lens (a world holding understanding in public
form, at semantic zoom, inhabited, with preserved disagreement) — and the
vocabulary / design moves this gives a non-designer.

## Guard
what is dangerous or misleading to borrow from this field.

## Open
the questions this raises, and where to dig further.
```

## Update protocol

```text
Touch an area             -> update its status row above.
An area yields principles  -> fold them into synthesis.md (create on the 2nd area).
End of a working session    -> leave the status table true; that is the handoff.
```
