# editable-material CAMPAIGN — Codex prompts, P2–P8

How to use: send ONE prompt, only after the previous package's gate PASS is
recorded in NOW.md by a Fable gate session ("gate Pn" there → PASS → copy the
next prompt from here). Never two lanes in one working tree (lived 07-24);
parallel P3∥P4 only in a separate git worktree, else strictly sequential.
Every prompt assumes a fresh Codex session with no memory — the repo docs are
the memory. P1's resume line lives at the bottom.

Skeleton invariants (all packages): AUTHORITY = DIRECTION.md + CAMPAIGN.md,
never edited by the implementer; NOW.md is Codex's ONLY doc surface; commit
code-only at package end, exact paths, house message style, never push; stop
clauses end the run with findings in NOW.md — a Fable session adjudicates.

---

## P2 — the material inspector (send after P1 gate PASS)

```
MODE: IMPLEMENT · one package only · repo /mnt/data/projects/Softland
AUTHORITY: DIRECTION.md + CAMPAIGN.md — do not reopen, redesign, or
expand. Prereq: P1 gate PASS recorded in
docs/current-mental-model/build/editable-material/NOW.md — read its
whole tail first (P1's conventions + any gate rulings bind you).

Package P2 — the material inspector. Read: DIRECTION.md (§layer
identities, §Probe-FINAL for what P1 built) · CAMPAIGN.md §P2 +
§Standing-stops · the P1 code it lands on (provenance_material,
face_projection projection, face_wiring pull).

Deliver ONE deterministic read-only projection: pick a block → entity →
its provenance attachment → facet-master → active revision (+ latest,
distinct) → the three contribution sites → current wearers → revision
trail (candidates, activations, rollbacks, times). Server-side batched
query through the existing projection registry — never client N+1
joins. Surface: window.__material console API first; a minimal face
only if genuinely cheap. Every answer must be reproducible: same
inputs, same world, byte-equal output.

FENCES: read-only — no editing UI, no writes, no new relation kinds, no
new PStates, no second facet, nothing from DIRECTION §Horizon; echo
machinery untouched; cljs 0 warnings; tests green.
STOPS: any need for a write path or a new durable truth → NOW.md + END.
Output: proof list — each query above proven / not proven, with the
exact console calls a human can replay.
```

## P3 — the facet spread, worn five (send after P2 gate PASS)

```
MODE: IMPLEMENT · one package only · repo /mnt/data/projects/Softland
AUTHORITY: DIRECTION.md + CAMPAIGN.md. Prereq: P2 gate PASS in NOW.md;
read the NOW.md tail — P1/P2 conventions bind you.

Package P3 — extract the remaining worn facets as served material, ONE
AT A TIME, in order: attention (border/box policy) → foldable (header
text + fold defaults) → positioned (reply-gap, placement defaults,
birth rules — settle cells stay its instance data) → threaded (column
adoption reach) → text-body policy slice (wrap floor/fallback). Each
extraction: policy constants leave code for a facet-master riding P1's
EXACT conventions (identity, revisions, active-pointer, serve/epoch,
stamps master+revision, error-card totality); render byte-identical at
current values; echo bar unchanged; the old constants DELETED.

MANDATORY HALT AFTER FACET 2 (attention): run the second-wearer reuse
test (DIRECTION §spine FAIL-tells — parallel serve artery, source-
specific branch, compat adapter, reinterpreted v0 material, divergent
activation concept, duplicated identity machinery, hidden compositor
cases, weakened totality/perf) + the reasoning-cost check (are two
facets harder to reason about than the monolith was?). Write both
verdicts to NOW.md and END THE RUN — facet 3 waits for the Fable
adjudication. This halt is a gate inside the campaign, not friction-
waiting.

Composition: when two facets first collide in one slot, declare merge
behavior + priority on the masters and render conflicts as lint — the
minimum vocabulary the collision demands, invented FROM it, never
ahead of it.

FENCES: no recipes, no bindings/dispatch, no bulk extraction, no new
facets beyond the worn five, nothing from §Horizon; strangler only.
STOPS: reuse-test FAIL · reasoning-cost FAIL · echo regression → NOW.md
+ END. Output: per-facet proof list + the facet-2 verdicts.
```

## P4 — circulation, Gate 2 (send after P2; may run ∥ P3 ONLY in a separate worktree)

```
MODE: IMPLEMENT · one package only · repo /mnt/data/projects/Softland
AUTHORITY: DIRECTION.md + CAMPAIGN.md + build/first-light/CONTRACT.md
(§4, §7 P3 — this package FULFILLS first-light P3 under that
contract's terms; no fork; flag discrepancies in NOW.md inline).
Prereq: P2 gate PASS; read NOW.md tail.

Package P4 — pressure acquires honest context with no ritual. Deliver:
(1) RECEIPTS — mechanical context captured at birth/utterance/mark by
extending the existing pick/context-bundle machinery: created-during,
picked-at, placement, worn master+revision references. A receipt is
co-presence ONLY and must never render as aboutness. As-of resolution
of worn context comes from activation history — no snapshots.
(2) EDGES — relation-kernel kinds grown by ONE reviewed line each
(instance-of-shaped; about-shaped), tests included, flagged loudly in
NOW.md for the gate. Adjudicate first-light flag A (wish-edge target
kind) explicitly in NOW.md at design time.
(3) THE GOLD PATH — point → say → the utterance lands durable,
edge-attached, with a visible projection ON the target (first-light P3
deliverable, its G5 gate criteria verbatim).
(4) AMBIENT SILVER — the autotag lane proposes about-edges, visibly
machine-stratum, deduped (repeated identical machine failures condense,
never inflame), observation ≠ interpretation ≠ proposal.
(5) STARTER CULTURE — retroactive kinds as predicates over the existing
#TASK/#Feedback blocks; pre-material records get silver associations,
NEVER rewritten as historical receipts.
(6) THE QUERY — "everything experienced around this material" served,
batched, with receipt/silver/gold composition exposed.

FENCES: no promotion/activation machinery (P6's), no bindings, no new
Rama module; kernel growth = the reviewed-line law only; map-must-not-
lie everywhere (inference marked, silver visible). STOPS: any need for
a material-truth owner beyond OC + relation kernel → NOW.md + END
(that condensation decision is P6's, adjudicated).
Output: Gate-2 proof — one real banked pressure enters during ordinary
work, receipt automatic, queryable path to the material world,
deterministic return to origin; terminal-escape detector armed (a
code-lane commit changing material-owned policy with no activation
event = one escape).
```

## P5 — bindings + dispatch (send after P3 gate PASS)

```
MODE: IMPLEMENT · one package only · repo /mnt/data/projects/Softland
AUTHORITY: DIRECTION.md + CAMPAIGN.md. Prereq: P3 gate PASS (facets
exist to carry claims); read NOW.md tail.

Package P5 — meaning becomes material; the one rule replaces the cond
ladders. Deliver, strangler-style, ONE gesture family at a time
(suggested order: fold-header click → block click/focus → drag/move →
wheel; camera/space bindings LAST, always on the code floor):
binding rows as served material carried by facets (closed data
grammar: gesture, phase, modifiers, verb name+version, priority —
material never names arbitrary code); ONE dispatch law in the kernel —
normalized gesture → one pick over the containment path → innermost
matching claim → named verb → unclaimed falls to the space; verb
registry entries carry effect class (pure-projection ·
durable-via-request · external-via-derived-worker); instance-level
binding rows legal (locality tiers); the interaction table served as a
projection (gesture × facet → verb, per-row master link); same-depth
conflicts render as lint, deterministic temporary winner, never
silent; after each family migrates, its dispatch function's branch
count is FROZEN — new meanings arrive as rows.

FENCES: no side effects in reactive queries, ever; no new verbs beyond
those extracted from existing behavior; no recipes; echo bar + input
latency unchanged; code-floor bindings (space pan/zoom, click-focus)
unbreakable by any data revision — prove it with a malformed-bindings
drill. STOPS: dispatch nondeterminism you cannot resolve with declared
priority · any gesture family that will not fit the closed grammar →
NOW.md + END. Output: per-family proof + the drill receipt.
```

## P6 — the truth loop whole, Gates 3 + 4 (send after P4 AND P5 gate PASS)

```
MODE: IMPLEMENT · one package only · repo /mnt/data/projects/Softland
AUTHORITY: DIRECTION.md + CAMPAIGN.md + first-light CONTRACT (§7 P4/P5
— this package fulfills them; no fork). Prereq: P4 + P5 gates PASS;
read NOW.md tail. /rama + rama-pitfalls skills BEFORE any Rama design.

Package P6 — deviation → candidate → preview → scoped activation →
announced change → reversal. Deliver: instance deviations (scoped,
visible, diffable vs inherited); pins; candidate revisions previewed
through the membrane pattern (candidate renders against REAL material,
active face untouched — first-light P4); scoped activation as accepted
events with blast-radius shown before/alongside (all unpinned wearers
in scope + future wearers, by reference — NEVER fan-out writes);
rollback = activation of a prior revision; change announces itself at
three scales (local breath at wearers · recoverable trace naming
revision/scope/actor/reversal · ambient material-weather projection);
case reports DERIVED from the event trail, claiming only declared
grounds; standable history (project the material world at a historical
cut). The material-truth owner: platform-check OC + relation kernel
FIRST; condense one named owner ONLY for truths genuinely homeless
(attachments-by-scope, pins, wear indexes) — one module max, additive,
backup via bin/land before any deploy (it fails closed now).

GATE 3 (metabolism): pick ONE banked friction from Sid's corpus (reply
width or muted-gray legibility — both recorded in his notes). Run it:
pressure → record → candidate → preview → then STOP AND REQUEST SID'S
ONE-LINE ACTIVATION WORD (his constitutional touchpoint — never
self-activate the first repair) → announced change → zero terminal
anywhere. GATE 4 (immunity): land the P1 wound trace as the record's
first immune-memory entry; re-run the malformed drill because the
kernel changed; previous revision reworn from inside.

FENCES: activation ≠ consensus ≠ deletion (plurality law — concurrent
candidates stay separately addressable); latest ≠ active enforced
everywhere; no portal self-editing; nothing from §Horizon beyond the
sanctioned owner condensation. STOPS: fan-out write temptation ·
homeless-truth ambiguity · anything requiring existing-data migration
→ NOW.md + END. Output: both gate receipts + the Sid-word transcript
line.
```

## P7 — the portal proper (send after P6 gate PASS)

```
MODE: IMPLEMENT · one package only · repo /mnt/data/projects/Softland
AUTHORITY: DIRECTION.md + CAMPAIGN.md. Prereq: P6 gates 3+4 PASS; read
NOW.md tail.

Package P7 — the material world becomes an inhabitable place. Deliver:
the portal as a deterministic, total, batched projection — open from a
pick: identity, resolved recipe (NAME the block recipe here iff the
worn-five composition has recurred — recipes are exhaust; if named, it
describes and never gates), facet attachments, active masters +
revisions (latest/candidate/active/pinned distinct), bindings + verbs,
wearers/appearances, deviations + pins with diffs, activation history,
experience items (receipt/silver/gold composition exposed, exact
origin reverse-links), conflicts/lint, truncation metadata. Why-this-
pixel answered from any rendered contribution. Blast-radius view
pre-activation. Historical cuts standable. The resident summoned
inside the portal receives EXACTLY this projection as briefing — no
LLM in the projection path itself, ever.

FENCES: portal renders through the layer's own machinery wherever
possible BUT portal self-editing stays OFF (that's P8+, after wear);
dull floor: error cards, previous-revision recovery, and a code-floor
rendering no data revision can break — prove with a drill; describe-
never-gate (an untyped/partial entity still projects). STOPS: any
query needing client N+1 joins · any place determinism would require
an LLM call → NOW.md + END. Output: the portal question list from
DIRECTION answered one by one with replayable calls.
```

## P8 — one verb born from inside (send after P7 gate PASS)

```
MODE: IMPLEMENT · one package only · repo /mnt/data/projects/Softland
AUTHORITY: DIRECTION.md + CAMPAIGN.md. Prereq: P7 gate PASS; read
NOW.md tail.

Package P8 — the react-bits seam closes: a BANKED behavioral wish
("reply to just this block" — vision/LOG.md 2026-07-18) travels the
full code-lane route AS MATERIAL: the wish (already recorded) → linked
implementation in the code lane → build/test receipts land as
addressable material (address code by blob-sha + path anchors, never
copy — external-code-is-a-view law) → the verb registered versioned,
effect-class durable-via-request → a material binding row references
it → the behavior works in the land → the whole chain queryable from
the portal (wish → code → receipts → verb → binding → worn).

FENCES: implementation is REAL code with tests (no interpreted
behavior DSL — the V4 wall stands); the binding remains closed-grammar
data; hot reload is the dev loop, the activation event is the
deployment record; nothing self-modifies the portal. STOPS: the wish
demanding more than one verb + one binding (scope creep) → NOW.md +
END. Output: the end-to-end chain replayed + queryable, and the
terminal-escape gauge reading for the whole run.
```

---

## P1 resume line (fill <STAMP> from the ledger's clean-shutdown entry)

```
P1 unblocked — resume from the deploy step. Cluster recovered (five
modules RUNNING; your forceClusterOpen did clear the drain — a fresh
boot completed it). bin/land backup now FAILS CLOSED (cd4fcb4); a
valid clean-shutdown backup exists at /mnt/data/rama/backups/<STAMP>
(ledger: clean-shutdown-observed); your 20260724-021401 snapshot is
annotated crash-consistent-only. Resume exactly where the stop fired:
single-module update → provenance-material-ingest! → restart-restores-
active drill → browser activation + ?drill= error-card drill → echo
sample → full suite → code-only commit. Fences unchanged.
```
