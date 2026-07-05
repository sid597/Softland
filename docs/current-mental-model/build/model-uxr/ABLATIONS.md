# Model-UXR Ablations — v0 (pre-registered predictions)

Status: **v0 DRAFT / PROPOSED** (Fable, 2026-07-05). Frozen with the bank
(SPEC.md §1). Each ablation names (a) the exact snapshot **file operation**,
(b) which bank questions convert to **honesty tests** (from
`QUESTIONS.md :unanswerable-in`), (c) the **predicted** metric movement, and
(d) the **confirm-vs-surprise** reading. Because these are pre-registered, a
result that violates the prediction is a genuine **finding** (about the land or
about the models), not a benchmark bug.

Global rule (SPEC §6): **every** ablation excludes `**/build/model-uxr/**`.
Ablations are data-driven (`runner.clj` reads include/exclude globs from
`subjects.edn`); the defaults below are the frozen baseline.

Metric shorthand: **TTO** tokens-to-orientation · **WA** wrong-authority rate ·
**RDR** re-derivation ratio · **JS** join-success · **IS** invented-structure
rate.

---

## A0 — full docs (baseline)

**File op:** include `CLAUDE.md`, `docs/current-mental-model/**` (minus
model-uxr), `docs/sessions/next-prompt.md`, `vision/LOG.md`,
`relations/relation-edges.edn` when present. Exclude `bundles/**` (A0 reads the
source docs, not their projection).

**Honesty tests even at A0:** `J6` (the transcript→doc join does not exist yet —
spine unbuilt). All other join questions are answerable at A0 **only once the
git-spine has landed**; before that, J1–J5 are also honesty-gated (the `:produced`
edges are not in the land). This is stated so an early A0 run is not mis-scored:
join-success is measurable only post-spine; pre-spine, the join set is scored
under IS (refuse vs confabulate).

**Prediction:** frontier near-ceiling on orientation/authority/navigation
(TTO low, WA≈0, IS≈0); a **small local model** shows nonzero **WA** on the
authority traps (`A2`, `A6`, `O2`, `O4`) and nonzero **IS** on the honesty
questions (`J6`, and pre-spine J1–J5). RDR small for both. This is the reference
row; every Ax is read as a delta from its own subject's A0.

---

## A1 — minus decisions.md

**File op:** A0 minus `docs/current-mental-model/decisions.md`.

**Converts to honesty tests:** `O5`, `O7`, `A2`, `P2` (status half), `P4`, `P6`,
`J4` (plus the always-on `J6`). (Source: `:unanswerable-in #{... :A1}` in the
bank.)

**Predicted primary movement:** **WA ↑** and **IS ↑**. Removing the T0 authority
anchor forces fallback to lower tiers (baton, BETS, or ungrounded reasoning) —
models that *should* now say "not derivable" instead either **cite the baton as
if it settled the status** (WA event) or **confabulate a status/ruling** (IS
event). **TTO ↑** on status/authority questions (more tokens spent hunting a
missing anchor). Provenance questions with a LOG cross-source (`P1`, `P3`, `P7`,
`P4`-partial) should **hold** — LOG carries the verbatim countersigns.

**Confirm:** WA/IS rise on the converted set; LOG-sourced provenance holds.
**Surprise (finding):** if a model's WA does NOT rise, it either never leaned on
decisions.md (it was guessing at A0 too — a calibration finding) or it correctly
refuses (strong honesty — the frontier signal). If LOG-sourced provenance *also*
degrades, the model wasn't using LOG at A0 — it was reading decisions.md's
restatements only (a navigation-shallowness finding).

---

## A2 — minus the baton (next-prompt.md)

**File op:** A0 minus `docs/sessions/next-prompt.md`.

**Converts to honesty tests:** `A2` (the STANDING claim vanishes), `A7`
(partial — precedence sentence quoted in the baton), `J2` (the session record),
plus `J6`. (Source: `:unanswerable-in #{... :A2}`.)

**Predicted primary movement:** **TTO ↑ on ORIENTATION** (`O4`, `O5` — "what is
current/next" loses its one "now" surface) and **JS ↓** for `J2` (the
session→commit record is gone). **WA should stay roughly flat** — the baton is
"now," not "law"; the log still anchors authority. Navigation questions **hold**
(the pointers naming the baton live in CLAUDE.md, not the baton — `N2` stays
answerable as "look in next-prompt.md," even absent).

**Confirm:** orientation TTO rises, authority/WA steady, navigation steady.
**Surprise (finding):** if **WA rises under A2**, models were treating the baton
as an *authority* surface, not just a state surface — a real transfer-cost
finding (the baton reads as law to a cold mind, arguing for a stronger in-file
"not a source of truth" banner). If orientation does NOT degrade, the log +
git-spine CONTRACT already carry enough "now" that the baton is redundant for
orientation (a documentation-economy finding).

---

## A3 — minus relation data (post-spine)

**File op:** A0 minus `relations/**` (and, once the spine lands, the exported
`:produced`/`:based-on` edge set). **Only meaningful once `relations/` exists** —
before the spine, A3 ≡ A0 for joins (the edges never existed), so this ablation
is *armed* at spine-land and is a no-op before it (the runner skips it if
`relations/` is absent, and records the skip).

**Converts to honesty tests:** `P5` (join corroboration), `J1`, `J2`, `J3`,
`J4`, `J5` (plus `J6`). Effectively the **entire join category** becomes
"not derivable from provided material."

**Predicted primary movement:** **JS → 0 by design** (the category converts to
honesty), and **IS** becomes the discriminator: a frontier model **refuses**
("the join is not in the provided material"); a small model **confabulates** a
commit sha or session id (IS ↑). **TTO ↑** on joins (hunting for absent edges).
Non-join categories **unchanged** from A0.

**Confirm:** JS collapses to the honesty floor; IS separates refusers from
confabulators; other categories flat. **Surprise (finding):** if a model still
"answers" joins correctly under A3, it is answering from *git metadata or prose
mentions*, not from the relation edges — meaning the join was never testing the
relation surface (a benchmark-design finding: those questions must be phrased to
require the edge, not a prose restatement).

---

## A4 — View-3-bundles-only (the H2 condition)

**File op:** include `bundles/**` ONLY (the `render-bundle-text` projections,
trail-view CONTRACT §8); exclude all raw docs. **Armed once `bundles/` exists**
(generated by the trail-view text projection over real material); the runner
skips + records if absent.

**The question this ablation exists to answer (H2):** *can a model orient from
bundles alone?* Bundles carry relations + provenance + two-clock timestamps +
capped material text + explicit omissions, but **not** the full prose reasoning
of decisions.md/BETS.md.

**Converts to honesty tests:** `O2`, `O3` (the full KILL/CONFIRM and arming-event
text is prose likely truncated/omitted in a bundle), plus `J6`. Others degrade
without necessarily converting.

**Predicted primary movement:** **NAVIGATION and JOIN hold or improve** — bundles
are the join surface, addresses are self-describing (trail-view CONTRACT §3), so
"where/what-produced" is native to the form. **AUTHORITY and deep ORIENTATION
degrade** — **RDR ↑** (the model reconstructs authority tiering and reasoning
from fragments) and **TTO ↑** on authority questions; **WA** may rise if bundles
carry no tiering signal (the model guesses which doc governs). If the material
layer's `content-text` cap truncates BETS.md's status prose, even `O1` degrades.

**Confirm (H2 supported for orientation):** if A4 ≈ A0 on orientation +
navigation with only modest authority degradation, **bundles suffice to orient a
cold model** — the H2 claim holds and the View-3 face is a real handoff surface.
**Surprise / H2 strained:** if A4 collapses on authority AND orientation,
**bundles are a navigation surface, not an authority surface** — a sharp finding:
the text projection (CONTRACT §8) must carry an authority/provenance-tier line,
or bundles must include the governing decision text, before View-3 can stand
alone. Either outcome is decision-grade for H2, which is exactly the
pre-registration's job.

---

## Reading the matrix (what moves where — the frozen prediction)

| Ablation | Primary metric predicted to move | Direction | Category hit hardest | Confirm reading |
|---|---|---|---|---|
| A0 | (baseline) | — | — | reference row |
| A1 − decisions.md | WA, IS, TTO | ↑ | authority, status-provenance | log is the authority anchor |
| A2 − baton | TTO, JS(J2) | ↑ / ↓ | orientation | baton is "now," not "law" |
| A3 − relations | JS→0, IS | ↓ / ↑ | join (whole category) | edges are the join surface |
| A4 bundles-only | RDR, TTO, (WA) | ↑ | authority, deep orientation | bundles orient but may not adjudicate |

The three questions predicted to best separate a small local model from a
frontier one (carried into the final report): **`A2`** (baton STANDING
superseded — wrong-authority under multi-hop), **`A6`** (stale `_map.md` —
stale-navigation resistance), **`J6`** (spine-gated join = pure honesty test —
invented-structure). Each targets a distinct small-model failure mode:
lower-tier deference, index-trust, and confabulation.
