# The Studio — arc brief

*(Working name — Sid redlines per his naming law: the name must make him ask
"is the thing built enough, is it doing the thing it was supposed to." The
session that birthed this arc: engineer-to-designer-optimised-UI-VM,
2026-07-31; Sid's verbatim words in `vision/LOG.md` same date.)*

## What this arc is

smalltalk-ui-vm made the UI material: every block renders through the
assembly interpreter wearing revisioned anatomy; structure is data with
preview → activate → rollback lanes and one shared human/agent hand. Then
Sid opened the Workshop unscaffolded — the armed C2 instrument — and it
read exactly as the halo had: "what is this what do i do about it????"

The diagnosis is a generator, not a feature bug: every gate the project
runs proves truth-to-substrate; none ever measured use-to-human. So each
face ships as an inspector of the machinery. The Workshop answers "how are
you made?" — completely, truthfully — when the inhabitant's standing
question is "what can I do with you?" The substrate (the VM) is right and
done; what's missing is the surface above it — the pencil, not the
debugger. Do NOT iterate the current workshop screens toward friendliness
(a hexdump never sands into a pencil): they become the x-ray; the studio
is a new primary face over the SAME lanes.

**The studio: the world with the pencil out.** Design development and
iteration happen directly in Softland, by Sid's hand and by agents through
the same lanes.

## The two goals (Sid, verbatim in LOG 2026-07-31)

1. **Design development and iteration directly on Softland** — the paper
   loop, in-land: draw a thing, it is real, annotate beside it, redraw a
   variant, iterate. "We have the affordances on the architecture side but
   not on the ui yet." Richer and more chaos-filled in reality than any
   example — forest over steps, never a 1-1 mapping.
2. **Build on top of existing UI by transforming it to the Softland
   equivalent** — point the AI at an outside component (e.g. an Apple HIG
   label); it draws the equivalent in-land, iterating visibly; the clone
   lands as material — interactive and modifiable by construction, never
   an artifact drop. Goal 2 depends on Goal 1's primitives; only the
   agent-eyes piece is independently buildable.

## Canonical script (the paper page — photos in the origin session)

Iteration 1: a rectangle drawn, text written inside — "this is a block i
can type in." A margin note beside it: what's wrong, what's missing
(a, b, c). Iteration 2 drawn below, referencing the whole of iteration 1
("how do I refer that this block refers to the whole work done in
iteration 1"). Iteration 3 on top of 2, or truly new. All iterations
coexist on one page; nothing is thrown away. The lived gates of this arc
are cut from this script.

The agent's mirror of the same script (Sid's second page): AI reads the
outside target, draws Existing → Iteration 1 → Iteration 2 → done, with
Sid watching and marking approval. His iteration trail and the agent's are
the same structure — the variant field is the shared workspace of both
hands.

## Settled laws (Sid ruled, 2026-07-31 — do not reopen; new walls route
## back to him with a first-principles case)

1. **World, not room.** The studio is the ordinary canvas with the pencil
   out — never a mode. A drafting table is a spatial convention (a frame
   you work within). Safety is temporal (versions), not spatial (walls).
   The anatomy room survives as the x-ray.
2. **Drafts are real.** In-progress components are never dummies — real
   and functional from the first stroke. Draft-ness is worn visibly (an
   unlanded look) so hands always know real from provisional.
3. **Draw freely, mint deliberately.** Becoming a permanent named
   component happens at accept. Multiple candidates per master coexist;
   one is the default. Unaccepted drafts persist as versioned history —
   how we got here — and intermediate versions stay retrievable for
   future use.
4. **Promotion is test-gated, never broadcast.** Accepting a draft does
   not instantly rewrite live instances. Between candidate and default
   sits a validation pass: agents take real instances in their real
   variety and pass real flows through the candidate — build test, then
   automated tests — breakage reported before the default flips. Rollback
   stays.
5. **Behavior is in-arc from the start.** "No no no not for later … we
   have the whole infra already." Drawn parts get wired to EXISTING
   registered verbs through the existing binding grammar — a drawn button
   does something. Minting NEW verbs stays fenced to the next arc.
6. **No lived script may ever require the x-ray.** The studio never
   routes hard things to the anatomy room — no plugs, no quick fits, no
   patches ("never never never"). X-ray is curiosity and forensics only.
7. **Margin thinking is required.** Structured annotation — notes,
   arrows, connectors attached to things — is in-arc even without ink.
8. **Ink: punted, door open forever.** Nothing built may preclude
   stroke-as-material later (tldraw-class canvas, writing tablet, iPad
   futures — "we want all the modalities"). Ink is a human-only modality;
   agents never ink — birth stays structured so the shared hand survives.
9. **The surface speaks Sid-words.** No fm:*, no hashes, no substrate
   vocabulary on the studio (component, draft, variant, note, accept,
   put back). A human naming layer over drafts/revisions. The substrate
   keeps its own names underneath; only the surface translates.
10. **Sid's hand is the gate; the agent rides from day one.** Direct
    manipulation is how the vocabulary becomes his — mediated-only design
    repeats the alienation. The agent is an ungated second hand on the
    same canvas through the existing lane; agent eyes (specimen readback)
    is a separate, parallelizable piece.
11. **Two gates every phase.** Receipt gates (truth-to-substrate,
    unchanged) AND lived gates (a cold task script performed on the real
    surface — done without reading anything, narratable). Phase grain =
    the smallest honestly-judgeable loop; the deferral criterion is
    judgment-relevance, never cheapness. Arc gate: Sid does one real
    design task he actually cares about, start to finish, in Softland
    instead of paper — and prefers it.

## Open at the substrate — the spike answers these before any contract

1. Can multiple revisions/candidates of one master render live
   simultaneously (the variant field's enabler)? Compile is per
   [master-id revision-id], which suggests yes — verify in code.
2. Where does a draft's structure live: per-instance structure (a new
   tier?) vs every draft silently minted as its own master at first
   stroke (needs a draft/real distinction in the registry; trails durable
   by construction)? The rulings constrain the choice (drafts real +
   functional · trails persist · candidates coexist, one default) — cost
   each route, recommend.
3. Generic drawing primitives (rect, line, text-run, frame): what exists
   in the registered vocabulary today, what needs commissioning.
4. Wiring an existing verb to a drawn part: how much of the binding
   grammar already suffices.
5. Drawing latency: can strokes ride the optimistic-local echo class
   (the 52ms-bar family)? "Structured-instant" loses its adjective
   otherwise.
6. Annotation substrate: relation to the existing marks machinery.
7. Triage the red refusal banners visible at Workshop open
   (fm:provenance parse error "EOF while reading" + fm:space
   zoom-clamp-invalid) — durable defect or leftover experiment; also a
   standing design smell (teaching cards built for agents at top billing
   to a human).

## Ladder sketch (a sketch, not a contract — the contract session
## finalizes with spike answers in hand)

Pencil + behavior wire (draw a thing, it does something) → variant field
(duplicate, edit at the pixels, accept through the test-gated promote,
reverse) → names + margin thinking → agent eyes + Goal 2 pilot (clone one
outside component end-to-end as the demo gate).

## Next session (build register) — starter

**P1 AND the independent gate are DONE — the package is at G7.** The
gate record is `GATE.md` (verdict: **PASS to G7 with residues**; the G6
falsifier round is banked verbatim in `G6.md`; its one confirmed HIGH —
FAIL-report laundering via instance-set re-mark — was repaired in place
and re-verified at the final bytes; the caller-elected-gating finding
was reclassified literal-vs-intent per the contract's own S1/T3 scope).
Code is UNCOMMITTED pending Sid's ruling. What remains needs Sid, in
order: (1) **G7 — the lived gate: cold, ONE-SHOT, on the real land,
LAST.** The task, in his hands, no reading first: draw a rectangle with
text → make it do something → duplicate the block as a draft and change
one visible thing → accept through the test gate → put it back. PASS =
completed without reading anything + narratable + the anatomy room never
opened. His verbatim reaction lands in GATE.md; a G7 FAIL routes to a
direction session, never a patch. (Logistics-only note, GATE.md §8: the
draw handle is a world-space slot near the origin.) (2) The commit
ruling — code and docs commits separate, on
`docs/current-mental-model-local`, never pushed; then re-run the
HEAD-dynamic suites at the committed HEAD (close rule).
