# GROUNDS — code-atom spec round (family = clojure-code)

2026-07-09 · code-atom spec room (Sid + Fable) · working record, direct
replacement, no banners. Scope per the opening prompt: **Clojure only**;
D-003 Regime 1 only — atoms serve sense (reading · arranging · relating);
editing code through atoms is Regime 2, gated. Family-invariant laws are
NOT restated here — they bind from `../SPEC.md` (v0, countersigned); this
document holds the clojure-code instance and its warrant.

## 0 · Artifact map

- This file — warrant + evidence (the why, the specimen record, the forks).
- `SPEC.md` (this folder) — the normative clojure-code family spec,
  extending `../SPEC.md` v0 by reference (authored at round close, Sid's
  "drive this home").
- `CONTRACT.md` (this folder) — the implementation work package derived
  from the SPEC (distiller adapter + driver; work-package shell).
- Specimen: `src/app/server/rama/relation_kernel.clj` @ HEAD (997 ln,
  4 commits) — **Sid's ruling, this session** (over markdown_adapter /
  object_container defaults).
- Independent lived trace: `docs/architecture/MAP.md` coverage ledger +
  its reading trail of `object_container.clj` (2026-07-09) — a real
  code-sense-making episode recorded before this round opened.

## 1 · The function (consumers first — Sid's six, as break criteria)

The unit derives from engagement, never from the material's aesthetics.
Sid's per-consumer test list (braid discussion 2026-07-09), each written
as a testable check against the specimen:

1. **Pointing and talking** — every span a reader actually discusses is
   addressable as a block or one refinement, no manual line math. Test
   spans: the journal gate (:672-678), the F1 absence note (:224-231),
   the retraction-rights branch of `relation-outcome` (:441-448).
2. **Callers/callees context** — from `oc/fixed-width-order-key`, reach
   its 3 call sites here (:167, :457, :479) and its definition in
   object-container, mechanically.
3. **LLM reads and searches** — an agent can assemble `relation-outcome`
   + its transitive local needs (records + helpers) without pulling 997
   lines, and the assembly records occurrences (SPEC §8).
4. **Versioning** — "what changed across WP1" answers at var grain:
   which vars superseded, which are new; the AMENDED header ties to the
   authorizing contract as evidence.
5. **Collaboration** — marks land on blocks/occurrences per SPEC §10.3;
   nothing code-specific except anchors must survive unrelated commits.
6. **Context switch** — what did I read (grounds-marks with depth), what
   superseded since — the morning-answer query at code grain. MAP.md's
   coverage ledger is the hand-rolled version of exactly this.

## 2 · Material facts (grounded in the specimen read, 2026-07-09, full 997 ln)

**Form census** of `relation_kernel.clj` @ HEAD — top-level units by
reader grammar:

- 1 `ns` form (:19-27)
- ~9 `def` (constants: registry :58-65, sentinels, markers, bucket fmt)
- ~75 `defn`/`defn-` — from one-liners (:158, :276-282 envelope
  accessors, :507-524 eighteen outcome accessors) to `relation-outcome`
  at **106 lines** (:398-503)
- 8 `defrecord` (:215-273 — the row types)
- 1 `defmodule` at **209 lines** (:616-824): depot decl + 7 PState decls
  + the microbatch dataflow + all 3 query topologies
- **~25 standalone comment blocks**: the file-header governance +
  AMENDED block (:1-17); the module banner (:29-52 — the module's
  de-facto docstring); ~15 section banners (`;; ── Kind registry ──`);
  free-standing design notes, including the F1 note (:224-231) whose
  subject is a form that deliberately does NOT exist ("there is
  deliberately NO RelationRequestRow") — a comment anchored to an
  absence.

**Findings the census forces:**

- **F1 — comments are material, but they are the OTHER line.** A plain
  reader-based cut (`clojure.tools.reader`) discards comments, and this
  file's comments are load-bearing at three grains (governance header,
  section banners, design notes — incl. the note anchored to an absence).
  Sid's reframe (this session, ratified): comments are not code-material —
  they are **sense-trail material fossilized inside the code file**,
  because before Softland a comment was the only place to put the why
  next to the code. So the free cut classes top-level units into two
  LANES, mechanically: **code lane** (forms) and **commentary lane**
  (standalone `;;` blocks). Code lane cuts now; the commentary lane may
  land LATER as its own stratum at zero cost — strata are additive
  (SPEC §14); until then comment spans sit as unblocked surface.
  Docstrings stay in the code lane: inside the form's span, surfaced via
  the name-card projection (F5). (P0-verify at contracting: exact-span
  fidelity of the parse — rewrite-clj vs indexing tools.reader.)
- **F2 — the section outline is real and mechanical but project-local.**
  The `;; ── X ──` banners give this file the same outline structure
  markdown headers give prose. Mint as a SEPARATE declared segmentation
  (project-convention rule-id), never folded into the clojure-general
  free cut.
- **F3 — giant forms prove the demand law, not a finer free cut.**
  `defmodule` (209 ln) and `relation-outcome` (106 ln) under-serve
  pointing at top-level grain — but the lived trace shows engagement
  landed on ~4 sub-regions of the OC module decl, not on all children.
  Err-coarse holds: refine on demand down the syntax tree (mechanically
  available); refinement provenance then measures where the free cut is
  too coarse (SPEC §5.1's own feedback loop decides empirically whether
  e.g. defmodule children deserve eager minting — not ruled now).
- **F4 — slot 4's two lineage cases are both real repo history:**
  - Same-var amendment: `af0e0e2` (custody, WP1 A1) changed
    `RelationDecisionRow` / `RelationEventRow` / `RelationEdgeRow` /
    `transition-row` / `rejected-decision-row` / `relation-outcome` in
    place — same ns-qualified names, new form-instances → mechanical
    `supersedes` on name-match. Also `fd59b78` amended `relation-kinds`
    (same def, stance kinds added).
  - Var move across files: `119f3f8` split 1,254 ln of
    `object_container.clj` into three adapter files; `emit-block` et al.
    changed ns → name-match breaks exactly there → move detection is
    silver (similarity), gold-ratifiable. Not hypothetical.
- **F5 — docstrings/DECLS are projections, not blocks.** The
  name+arglists+docstring card ("DECLS grade" in MAP.md) is a mechanical
  sub-span projection over a form block — derived on read, never stored
  as a separate unit.

## 3 · The five-slot template — clojure-code

| slot | answer (lean where a fork is open) |
|---|---|
| 1 · source identity | **text@commit, git authority (D-003).** Concretely: surface = **git blob** (content-addressed, sha-keyed); `(path, commit)` resolves to a blob. Version axis = the commit DAG; a file's own axis = its blob succession (this specimen: 4 blobs over its whole life). Blob identity makes every span stable across all commits that don't touch the file — for free. **RULED (Sid, this session): store the raw text AND the atomic breakdown — the transcript pattern** (raw source + derived units + anchors in the container kernel); storing ≠ owning — git remains authority. Redaction (SPEC §2.1) for code = declared **deny-list of whole files** (env.clj never enters — D-003 fail-closed); allowed blobs store verbatim. |
| 2 · free cut | **Concrete-syntax top-level units, classed into two lanes: code lane (forms) · commentary lane (standalone comments)** (F1, Sid's reframe). Mechanical, no model calls. Code lane now; commentary lane later as its own stratum, zero cost (SPEC §14). Un-minted spans stay unblocked surface (reconstruction lives on the surface, SPEC §2.3). Section-outline (F2) and analyzer cut are separate declared strata. |
| 3 · form vocabulary | Normalized head symbol, closed table + retained raw head: `clj-ns · clj-def · clj-fn (defn, defn-) · clj-record · clj-protocol · clj-macro · clj-multi (defmulti/defmethod) · clj-comment (;;) · clj-rich-comment ((comment …)) · clj-other`. Project extensions ride the table's version (registry pattern, one-line reviewed change): `defmodule`, Electric `e/defn`, … Form is fact; "this fn is a validation helper" is a mark. |
| 4 · continuity | **Three ingredients.** (i) **The var as continuant** — ns-qualified name, the durable natural key Clojure gives free; the ns itself is the file-grain continuant. (ii) **Form-text content hash** — same name + same text in a NEW blob (someone edited elsewhere in the file) = the SAME instance **re-addressed**, no event minted; only name-match + text change mints `supersedes` (F4 case 1). Without this, one commit touching line 900 fakes ~100 supersessions — af0e0e2 really changed 6–8 vars of ~100. Prior art: codeq keyed code segments by content so identical forms share identity across blobs; Unison content-addresses at definition grain with names as metadata — we import that insight at form grain while git keeps authority (both prior-art claims from general knowledge — verify before they become load-bearing). Whitespace/alpha-normalized hashing = recorded candidate only. (iii) **Rename/move = silver** similarity proposal, gold-ratifiable (F4 case 2). Standalone comments: span identity only; continuity silver via adjacency. |
| 5 · mechanical edge floor | `requires` — from the ns form, per-namespace (mechanical, certain). `supersedes` — name-match across blob succession (mechanical). def-site — var-continuant → form-instance (mechanical). `calls`/`references` — analyzer cut (clj-kondo grade), **versioned fallible-mechanical**: still form-not-kind; the deriver VERSION rides `note` while the asserter-id stays version-free (corrected at source — Fork 2 below). Rama/Electric macros are the known analyzer hazard — P0-verify at contracting. Plus the family join: SPEC §11.3's tool-call edges resolve `path@commit` → blob surface → refine on demand — the sense-line touches code exactly there, and git_spine already stores the session↔commit joins. |

The family's novum, confirmed: **slot 4 — the version axis and the var as
continuant.** Prose surfaces are immutable events; code surfaces succeed
each other under a name that persists. Nothing else in the template
required a new law — the block grammar absorbed the rest unchanged.

**Versioning is the center (Sid's push, this session).** The Regime-1
edit loop the atom must survive: edit files → commit → spine ingest →
new blobs → re-cut → lineage. It survives mechanically: re-cutting a
stored blob is a no-op (same `(blob, span)` keys, SPEC §6.1); a new blob
mints instances only where form text changed (slot 4(ii)); everything
else is re-addressed silently. Forward-compatible with pre-commit
states: a blob identity exists for any content git can hash, committed
or not, so mid-edit surfaces need no new identity mechanism if an editor
integration ever wants them. Contrast that frames ours (general
knowledge, flagged): Unison stores definitions content-addressed in its
own database and makes names metadata — the right answer when the tool
OWNS truth; git owns ours (D-003), so we reconstruct the same two
ingredients — content hash + durable name — OVER blobs rather than
instead of them.

## 4 · Strata plan (consumer → stratum; SPEC §14 plurality)

| stratum | rule kind | serves |
|---|---|---|
| reader cut (free): top-level forms, two lanes (code · commentary; commentary lane may land later) | mechanical, clojure-general | pointing/talking · collaboration · the anchor space everything else shares |
| section outline (`;; ──` banners) | mechanical, project convention, own rule-id | orientation, "read the row records" — the markdown-header analogue |
| DECLS/name-card projection | derived from continuants + form spans (F5) | inventory reading · LLM search |
| analyzer cut (clj-kondo) | versioned deriver, fallible-mechanical | callers/callees · LLM context assembly |
| lineage (blob succession × name-match) | mechanical + silver at renames | versioning · context switch |
| runtime cut (var reflection) | deferred — no consumer in Sid's six demands it | recorded candidate only (braid: "live truth") |

## 5 · Trace validation (independent: MAP.md, written before this round)

The MAP.md reading trail of `object_container.clj` engaged at exactly the
grains the template predicts, none it forbids:

- docstring/header first → comment blocks are first-class (F1) ✓
- "all 30 row records (:38-176)" — a run of consecutive forms read as a
  group → section grain (F2) ✓, each record then cited singly → form
  grain ✓
- "DECLS 1692-1760 / 2443-2501" — sub-spans inside the giant module form
  → refinement demand into defmodule (F3) ✓, at name-card grade (F5) ✓
- The coverage ledger itself is hand-rolled block bookkeeping: (file,
  depth, line-ranges, date) ≙ (surface, grounds-mark-with-depth, spans,
  provenance). The grammar absorbs MAP.md's ledger as grounds-marks over
  code blocks — the strongest validation: the lived artifact independently
  reinvented the tuple.
- MAP's truth-kind labels over modules = kind-marks (interpretation),
  correctly NOT forms ✓.

## 6 · Existing-infra mapping (inventory duty; no new module)

The container kernel already holds the shapes: `SourceArtifactRow` (raw
text + hash), `SourceAnchorRow` (start/end offsets), `DerivedUnitRow`
(unit-kind + distiller id@version), `CompositionEdgeRow`. The free cut
lands as a **distiller adapter** `clojure-form-v0` following
`markdown_adapter.clj`'s exact shape (deterministic unit ids, offset
walker); the specimen file is kin to its own future atomizer. Mechanical
edges land in relation-kernel driver-side with deterministic idempotency
keys — git_spine's exact precedent (spine idempotency, asserter in the
relation-id, replay log). New truth-kind added: none — passes the
truth-kind test.

## 7 · Open forks (leans recorded; rulings are Sid's)

- **Fork 2 — dependency edges, sharpened after Sid's AST question.**
  Two different things hide inside "AST":
  - **containment** (this `let` sits inside that `defn`) — COMPUTED from
    spans (SPEC §6.2), stored nowhere; the cut gives it free.
  - **cross-references** (`requires`, calls) — not derivable from spans;
    an analyzer PASS extracts them and its OUTPUT is stored as
    **relation rows** (relation kernel, asserter = deriver@version).
    This is Sid's own sketch this session: "pass through ast and save
    the output to relation form" — the final view is built from blocks
    + computed containment + those relations. The AST itself is never
    stored; it is a transient parse the deriver walks.
  **RULED at round close (Sid: "new kinds and now lets drive this
  home"):** relation kernel, with `:requires` + `:calls` REGISTERED as
  kinds — never overloading `:references`.
  **Asserter correction (caught at source while contracting):** this
  file's earlier lean said asserter = deriver@version. WRONG — git_spine's
  asserter is deliberately version-FREE ("the relation-id includes the
  asserter, so versioning it would fork every edge", git_spine.clj:41-44),
  and retraction requires actor == stored asserter
  (relation_kernel.clj:441-448), so a versioned asserter could never
  retract its predecessor's stale edges. Carried into SPEC/CONTRACT:
  version-free deriver actors; version rides `note`; re-runs CONVERGE on
  the same relation-ids and retract edges that disappeared. Strata live
  in notes + status history, not in forked ids. Volume + the
  Rama/Electric-macro analyzer hazard live in the CONTRACT traps.
- **Naming (all scaffolding):** blob-surface · form-instance · var
  continuant · code lane / commentary lane · re-addressed vs superseded ·
  reader cut / analyzer cut / lineage cut · name-card. Sid names by
  recurrence.

## 8 · Ratified in-flow (this session)

- Specimen = `relation_kernel.clj` (Sid, over the two defaults offered).
- **Fork 1 RULED: store the raw format AND the atomic breakdown, the
  transcript pattern** (Sid: "we store the raw format AND the atomic
  breakdown just like we did for transcript"). Blob-sha is the version
  key; git stays authority.
- **Comments are the sense line at the code lens** (Sid's reframe):
  "comment are there to make sense of code and in softland we are
  building the sense trail" → two mechanical lanes in the free cut;
  the commentary lane may land later.
- **Fork 2 RULED: new kinds** — `:requires` + `:calls` enter the
  relation-kinds registry; edges at continuant grain in the relation
  kernel (Sid at round close, with the drive-this-home instruction:
  SPEC → CONTRACT → implementation; reopen-on-need later).
- Recorded input (design altitude, not this round's build) — Sid: "the
  ui component will have versioning built into it and changing the
  version can change the other context in which it was built in iff the
  user wants … reactively controllable and fine grained permissions and
  versioning." Spec consequence: occurrence/reference targets come in
  two grades — **instance-pinned** and **continuant-floating**
  (policy-resolved, flippable). Permissions noted for a later round.

## 9 · What the SPEC section must answer when the hand-pass survives

Offsets over blob text (same UTF-16 rule?) · the form-vocabulary table +
its extension mechanism · the two-lane classification rule ·
re-addressed vs superseded semantics (form-text hash) · lineage edge
kinds and their tiers (mechanical name-match vs silver move) · occurrence
target grades (instance-pinned | continuant-floating) · the analyzer
stratum's declaration shape · the tool-call → blob join · conformance
items at code grain (idempotent re-cut on the same blob; a second
stratum disturbs nothing; census reproduction on the specimen).
