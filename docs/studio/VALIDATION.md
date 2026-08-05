# studio — fresh contract validation

**VERDICT: FAIL.** The package shape is preserved, but P1 is not authorized from
the validated contract. Two large scenario traces terminate at unowned durable
transitions; a third fence ambiguity would make the promised zero-diff proof
non-machine-checkable. This is the one fresh default-fail round requested for
the 2026-07-31 cut. The contract was recut from this artifact; the round was not
re-run.

## Subject and role

- Mode: GATE; altitude: executable contract; authority: canonical candidate.
- Validated bytes: `CONTRACT.md` SHA-256
  `c7084a33109f58a228160de56cc42d4ffc78f43a2bb86fe992b6ed209230ca25`.
- Docs checkout: `/mnt/data/projects/Softland`, branch
  `docs/current-mental-model-local`, HEAD
  `daa84cc036b57dbb2b689bc6bac1c66a533abc91`.
- Code truth: `/mnt/data/projects/Softland-smalltalk-ui-vm-p2`, branch
  `codex/smalltalk-ui-vm-p2`, clean HEAD
  `685f2fc6c937225503d48e9f9654ff5179600b68`.
- `BRIEF.md` was consumed once at boot. Its eleven laws were not reopened.
- The pre-existing untracked `PODCAST_SCRIPT.md` was outside scope and untouched.

## Scenario 1 — birth → edit → restart → accept → reverse

Contract trace: a real draft is durable from first stroke (§3b, lines 55–59),
pixel edits compile to the seven existing ops (§3c, lines 60–66), and the trust
loop says the draft candidate coexists **on the master** before a gated default
flip (§3e, lines 71–84). S1 nevertheless leaves ownership between an
instance-structure tier and one new master per draft (lines 96–103).

Disk trace:

1. The available instance route synthesizes an instance master from a registered
   parent (`object_container/facet_master.clj:457-460`).
2. Its write path snapshots inherited material, merges overrides, imports the
   instance revision, and activates that instance pointer
   (`object_container/facet_master.clj:517-544,591-620`).
3. Shared activation then rejects a revision whose container is not the shared
   master's document (`object_container/facet_master.clj:252-273`).
4. The public registry remains a closed code value (`facet_masters.cljc:16-47`).

The trace therefore has no contracted transition from either proposed draft
identity to the target master whose default is meant to flip. Route (a) needs an
explicit instance→shared candidate promotion; route (b) needs a runtime master
identity/read path and a definition of which default becomes permanent. The
contract asks P0 to cost storage, but not to trace this entire promotion chain.

**Finding F1 — FAIL: draft identity and promotion ownership are under-bound.**

Smallest recut: S1 must price the whole chain—birth identity, edit target,
candidate container, preview scope, promotion target, default pointer, and
reverse—not merely where structure is stored. If neither route completes that
chain through existing ObjectContainer/revision machinery, the existing
cutover-class stop fires.

## Scenario 2 — candidate → three-instance test → report → canonical flip

Contract trace: §3f promises a breakage report, says only a passing report flips
the default, and requires the flip event to carry the report id (lines 75–84).
T3 restates fail-closed acceptance (lines 185–186).

Disk trace:

1. Canonical `facet-master/activate!` checks only candidate existence, container
   ownership, and compilation (`object_container/facet_master.clj:252-273`).
2. It then creates the pointer edit and accepts it without reading any report
   (`object_container/facet_master.clj:297-342`).
3. The only metadata door is `:activation/grounds`; grounds are closed to
   `:experience | :deviation | :conflict` with an unverified string id
   (`activation_event.cljc:23-29,66-83,127-134`).
4. The contract gives the proposed report no durable schema, write owner, read
   path, candidate binding, idempotency/conflict rule, or canonical enforcement
   point. A UI wrapper could be bypassed by the existing activation hand.

The report cannot honestly be smuggled in as an `:experience` string unless a
query proves that referenced durable fact exists, matches this candidate and
instance set, and passed. Otherwise the event only *claims* a gate occurred.
Enforcing the rule may touch the canonical activation owner and therefore also
changes the gate-tier decision; §8 escalated only for S1 durable touch.

**Finding F2 — FAIL: “no report, no flip” has no authoritative truth path.**

Smallest recut: fold the report route into S1. P0 must pin its closed shape,
durable write/read lane, candidate binding, replay/conflict semantics, honest
activation-ground encoding, and the canonical owner that rejects direct bypass.
Any new depot/PState/import family or protected-kernel change remains a STOP.
T3 must attack the lowest existing activation hand directly. FULL tier is
required if the canonical durable owner changes.

## Scenario 3 — ordinary canvas → drawing gesture → behavior → Sid-language

This trace is executable enough for P0 rather than a failure. §3a/S3 preserve
camera-reserved naked drag/wheel; §3d/S5 choose an existing registered verb; §3h
and T6 keep substrate vocabulary behind details. On disk the camera predicate is
already a named shared door (`binding_material.cljc:134`), the verb registry is
enumerable (`verb_registry.cljc:47`), the primitive registry is a plain value
(`face_primitives.cljc:1252`), and compilation consumes such a value without a
global mutable registry (`face_assembly.cljc:330-347`). S2–S5 can bind exact
choices without reopening the laws.

Preserve: world-not-mode; no x-ray in the lived script; existing gesture and
verb grammar; drawing hand as compiler; Sid-language; ink as a future primitive.

## Scenario 4 — implementation diff → protected x-ray → gate handoff

Contract trace: §2 says x-ray changes are forbidden except an S6 repair (lines
42–44), while §6 says anatomy-room surfaces stay byte-identical but names no
paths or symbols (lines 163–178). P0 is asked to pin protected-file hashes only
(§9 lines 222–229).

Disk trace: the ordinary canvas and the x-ray Workshop share `ground.cljs`—the
Workshop controller spans `ground.cljs:627-755` and its public hand is installed
at `ground.cljs:4101-4118`. A whole-file hash would forbid ordinary-canvas Studio
integration; no pin would permit accidental x-ray drift.

**Finding F3 — FAIL: the x-ray protection unit is not machine-defined.**

Smallest recut: preserve the x-ray's visible behavior/controller with P0-pinned
symbol-span hashes plus existing projection goldens, while retaining whole-file
hashes for the explicitly protected §6 files. Any diff inside a pinned x-ray
span is allowed only for an S6 repair. Shared host files are not falsely called
whole-file-protected.

## What remains right

- The eleven laws remain the authority; no law was reopened.
- P0-before-P1 is the correct uncertainty boundary.
- The S2–S5 registry/gesture/latency/verb questions are concrete and answerable.
- The seven-op compiler boundary, no-new-verb/module fence, two gate families,
  Sid-owned one-shot G7, and no-product-code P0 remain intact.
- The defect is contract ownership, not a reason to iterate the x-ray UI or
  redesign the Studio arc.

## Disposition

This is substantive `fail`, not `minor-fail`: F1 changes the transition map; F2
changes authoritative enforcement and potentially the gate tier; F3 changes the
verification unit. `CONTRACT.md` was recut with these findings as input. Per the
one-round rule, no validation re-run and no P0 followed in this context.

