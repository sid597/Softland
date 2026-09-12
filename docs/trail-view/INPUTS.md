# Trail-View Data Contract — Input Manifest (pre-contract)

References below to `BETS.md` are historical; that document is retired.
For vision orientation, use [carry-on](../carry-on.md) as a reference summary
and [the vision log](../../vision/LOG.md) as the primary source.

Gathered 2026-07-03/04 (bet-foundry + deep-thinking sessions with Sid).
Status: INPUT to the contract, not the contract. The contract itself opens in
a FRESH session per `.claude/skills/work-package/SKILL.md` (one phase per
session; Fable judgment 2026-07-04, delegated by Sid: "use your judgement to
decide").

**Authority note (Sid, 2026-07-04):** prior trail-view/design docs "were
written by lower reasoning fables" — they are input, NOT authority. Ground
truth for the contract session: `decisions.md` (D-002/D-005 binding), the
actual code (`src/app/server/rama/`, esp. `relation_kernel.clj`), the 27-04
wall-panel demand as recorded in D-002, and this manifest. Verbatim sources
for every Sid-decided item below: `vision/LOG.md` 2026-07-04 entries.

## Sid-decided inputs (binding intent, countersign at contract time)

1. **Bundle = everything, layered.** "The unit that leaves the chatbox" is
   raw + native + view-forward together — never a choice among them. Ingestors
   store raw AND native/view-forward forms.
2. **Staleness renders differently.** Decay / re-attestation semantics: at
   minimum a last-walked / last-attested field per claim/relation, and the
   view renders un-walked material visibly differently ("Yes it thould render
   differenty").
3. **First reader is the agent (View 3 load-bearing), with a hard legibility
   goal:** understandable by agents in MINIMAL tokens. Second goal: agents CAN
   manipulate views — therefore view-specs must be data in the land
   (versioned, assertable), not code outside it.
   **Face order RULED 2026-07-04** (Fable, on Sid's delegation; decisions.md
   Open Questions): View 3 → threaded/DAG timeline (Sid's first pixel
   surface) → canvas (parked).
4. **Second-user evaluation is machine-first**: a benchmark where a model with
   Sid-granted read permissions uses the system cold (H3 machine half; see
   BETS.md H3 2026-07-04 sharpening). The contract's acceptance gates should
   anticipate benchmark-style probes (orientation questions answerable only
   via the view's queries).
5. **Questions get dedicated work** ("lets make it a dedicated thing"): the
   contract must NOT preclude hole-shaped rows (a relation pattern with an
   unfilled endpoint) — extension point at minimum; the dedicated design item
   is queued separately.
   **Form-break evidence (2026-07-04, verbatim in LOG):** the decisions.md
   open-questions section broke against Sid in daily use — "unreadable...
   wall of text... gives me anxiety... I think there is very much value in me
   going over the questions and engaging but this is just not the medium."
   This is the view's user-story zero for questions: one question = one
   card — the ask in one line, the recommendation in one line, answerable
   in place. Interim medium until the view renders: his Roam graph
   (proposal→approve flow, demonstrated 2026-07-04).

## Carried obligations (relation-kernel residue, recheck-amended)

6. **Importer-timestamp discipline**: status-history order rides
   client-supplied `asserted-at-ms` (claimed-time order, not arrival order) —
   the contract must state this discipline for every emitter it defines.
7. **Envelope/payload-binding server-side recheck** before any agent-authored
   writers (gate-review open doubt #1).

## Fable-proposed inputs (Sid reaction noted; contract-time judgment)

8. **Verdict capture** (Sid: "greatt catch"; then, to the framing "the
   difference between the trail view rendering what happened and rendering
   what we came to believe": "oooooo yeah yeah" — read as leaning IN-SCOPE):
   stances/adjudications as first-class assertion rows — `doc §X refuted-by
   <actor>, evidence <anchor>` — the layer that currently dies with each
   session. The contract session should draft it in-scope; final call is
   Sid's countersign on the contract.
9. **The commit-gesture for thought = select + type + sign** (three moves,
   never data entry): select a span of already-captured material, type it
   (claim / decision / dead-end / version), sign it. The gesture is an
   assertion event over the log — same shape as RelationEdge writes.
10. **Version-assertions can disagree**: asserter-in-identity already permits
    two minds to assert different versions/boundaries over the same span —
    the view should render both, badged, not merge (late-bound consensus).
11. **Depot events stay engine-neutral** (standing rule candidate): plain
    namespaced maps, nothing Rama-specific in event schemas — the log is the
    asset; Rama is the host. (Hedge for the closed-source-bedrock tension.)
12. **Seed-corpus citation thickening** (for the later paper jumpstart, noted
    here so the shape isn't precluded): imported citations enter as thin
    `references` edges that thicken only when anchored to evidence spans —
    imported pathologies decay by default.

13. **Daily-relief requirements (Sid, 2026-07-04 form-break thread — verbatim
    in LOG):** the view must render TODAY's material, not only the archive —
    ingest cadence is per-session/daily, never batch archaeology; the
    Sid-facing face after View 3 is the threaded/forked session DAG
    (his own 27-04 panel IS a threading UI over his work); decisions,
    questions, annotations, artifacts engage in ONE place — cards over the
    same material, no medium hop. Fable note: his F4 claim (a structured
    medium costs the machine fewer tokens than a 400k linear transcript) is
    H2 restated as economics — the benchmark should measure exactly this.

14. **Read-only MVP ruling (Sid, 2026-07-04 — "for the very first MVP we
    don't need the write surface"; Fable concurs after a falsification
    pass):** the first form is a READ-ONLY view; the write surface is the
    existing Claude CLI; watchers feed near-live ingest; Sid's loop is
    work-in-CLI → view updates → screenshot back into CLI when he wants
    action. read→write is the NAMED second milestone, entered when observed
    read-only friction demands it — desire-paths sequencing for the write
    surface itself. Sharpenings:
    - every view face renders its own ADDRESS as text (the query + params
      that produced it) — a screenshot is then a resolvable pointer, not
      just pixels;
    - watchers are TRIGGERS over existing ingestors (transcripts, md,
      commits once the spine lands) — never new ingestors; safe because the
      kernels were built for convergent re-import (deterministic ids,
      idempotency journals);
    - read-only means zero kernel writes FROM THE VIEW; local view-state
      (zoom, filter, selection) is allowed; the kernel keeps taking writes
      from the CLI side throughout;
    - the LLM is phase-1's write surface (Sid instructs in CLI, agent
      asserts: payload asserter = sid, envelope actor = the agent) ⇒ the
      envelope/payload server-side recheck (item 7) graduates from "before
      D-003 agent writers" to "before phase-1 daily use";
    - the contract session should propose this ruling as **D-008** and
      deliver it to Sid as a Roam card for countersign (not a decisions.md
      wall).

## Consumers, in order

1. Agents (View 3 / context bundles / the H3 benchmark) — the load-bearing
   face and first paying reader.
2. Sid's daily sensemaking (H1's kill/confirm clock starts at first render).
3. DG-plugin teammates (H3 human half, later; adapter-shaped).

## Related queue items (not this contract)

- Question-unit dedicated design (hole-shaped rows; announcement-at-match).
- Model-UXR benchmark design ("user research for models").
- D-003 Regime-1 spine (extractor + commit-metadata adapter) — feeds this
  view; needs Sid's go.
