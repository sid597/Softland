# Inland integration — starter (settled 2026-09-13)

The thread aim for bringing Inland's construction model into Softland. It
records what settled on 13 September across three chairs and two rounds of
feedback. It is not a decision in decisions.md; the first workpiece has to
prove it before anything there changes. Sid's words are in vision/LOG.md
(entries of 7 and 13 September).

## Aim

A question acquires the representations and actions it needs while we work
on it. The integration is the work. A definitive environment for
understanding software is not a prerequisite and is not being designed.

## The first workpiece

How an accepted edit becomes a visible change, in each system.

- Inland: gesture → Electric recipe → proposal → Rama admission → owned
  ProxyState → Electric recomputation → realization (src-inland/README.md).
- Canonical: UI action → event into Rama → materialized state out → render
  boundary (docs/decisions.md, src/app/server/README.md,
  src/app/client/README.md).

Trace both by reference. Run what can be run from the surface and attach
the receipt. Where the two paths diverge, the integration's questions
appear: one truth or two (Inland's isolated Rama against "Rama is truth"),
and one vocabulary or two for the layer above the waist (decisions.md's
rows, components and ECS layer against Inland's facts and definitions).
Carry the workpiece through: develop a candidate, make the source and doc
changes, run the behavior that matters, examine the result from the same
place.

## The forced first step

Nothing can be by-reference until repo material can be referenced as facts:
a passage at a revision, a function at a revision, a decision's clause, a
check and its result. That is the first implementation step. It is not a
visualization. Everything after it is chosen by the workpiece.

## What a tool made inside must satisfy

1. It refers to the actual things being worked on and keeps that
   correspondence as they change; when the material changes, the tool shows
   the result or shows that it is stale.
2. It presents them, accepts interaction, and can use the relevant
   computations and actions: run a check, open a definition, carry a
   candidate toward an observed result.
3. It remains inspectable, changeable and reusable after first use, by the
   people using it. Today that means editing an EDN record; the visual
   recipe editor does not exist, and the tool says so in its own margin.
4. Its references are at the grain of meaningful work. A reference at file
   grain repeats the July failure: "the granularity is just to big ...
   nothing gets communicated" (vision/LOG.md, 2026-07-07).
5. No invented components. Every component is a reference to a thing with a
   revision. The invented-components sketch of 13 September is the failure
   to beat.

The form is free: an outline, a comparison, a trace, an interactive
example, whichever the question needs at that moment. The residency is
fixed: the workpiece lives in the medium as referenced material, not in a
markdown file, even when markdown would be faster. Finding where the medium
fails is the deliverable.

## How requirements get recorded

Not by Sid. Inside the medium, a request no rule answers stays visible and
pointable as unanswered work, and a question with a pattern and no body
announces its matches (intended-design §3.2, §3.3). A miss is a fact with a
subject and a time. Outside the medium, a scribe chair on a lower model
reads the session and extracts the misses. Sid's meta-acts are the ones he
already makes: "stale", "show this differently", "I can't do that here".
The main chair adjudicates the extracted misses at settlement.

## Relationships and lenses

Keep relationships as encountered: a decision prescribes a behavior, an
implementation does something, a check examined one thing and warrants one
claim, a candidate changes a mechanism. Discourse-graph interpretations are
lenses added over that material, not its schema (vision/LOG.md, 2026-07-07,
"the inquiry named").

## Biology

The next independent pressure, not scheduled. When a paper and a structure
go through the same capabilities, the question is what had to change and
why. A parser, a domain computation or a representation capability is
growth. A second account of identity, revisions, references or authored
tools is a failure of generality.

## Limits carried in

Inland's editor is a small EDN record editor. Inland uses isolated storage
and does not migrate the canonical server's data. The full
source/build/recovery loop is unproved. These are the connections the
integration exists to establish, not assumptions it may make.
