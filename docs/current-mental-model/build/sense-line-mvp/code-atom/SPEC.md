# SPEC — Code Atoms (family: clojure-code) · v0

2026-07-09 · code-atom spec room (Sid + Fable) · **status: v0 — authored at
Sid's round-close instruction ("lets drive this home and have the spec"),
live by default (D-010), Sid redlines anytime.** Warrant: `GROUNDS.md`
(same folder). This document EXTENDS `../SPEC.md` v0 (countersigned): every
family-invariant law there — layering, separability, immutability,
reconstruction, identity-by-construction, refinement, re-run strata,
occurrences, holes — binds here by reference and is not restated. Only the
clojure-code instance is normative here.

**Scope**: Clojure only (`.clj` `.cljc` `.cljs` — rama/electric/webgpu are
all Clojure). **D-003 Regime 1 only**, cited narrowly: code as view, git
authority, commit-boundary; atoms serve sense (reading · arranging ·
relating). Editing or recomposing code THROUGH atoms is Regime 2, gated on
the self-hosting test — a non-goal of this spec, not an oversight.

**SPEC ≠ BENCHMARK** (standing correction): this defines the grammar; how
well any cut reads is judged later, in use.

Reading rules: MUST / SHOULD / MAY / OPEN as in the parent spec.

---

## 1 · Nouns (delta over the parent spec)

| noun | definition |
|---|---|
| **blob-surface** | The surface for code: one git blob — the exact content of one file version, content-addressed by git. THE raw is text@commit (D-003); the blob is its storage-grade identity. |
| **form-instance** | A block in the code lane: `(blob-surface, span)` covering ONE top-level form. |
| **commentary block** | A block in the commentary lane: a standalone `;;` comment run. Sense-line material fossilized in the file (Sid's reframe). DEFERRED — reserved rule-id, lands later as its own stratum. |
| **var continuant** | The ns-qualified name a def-family form binds (`app.server.rama.relation-kernel/relation-outcome`). The durable identity that connects form-instances across blobs. The ns name is the file-grain continuant. |
| **form-text hash** | Content hash of a form-instance's exact text; stored per unit. Distinguishes *re-addressed* from *superseded* (§4). |
| **name-card** | A PROJECTION (never a stored unit): name + arglists + docstring span of a form-instance — MAP.md's "DECLS grade". |
| **re-addressed / superseded** | Lineage outcomes across a file's blob succession (§4.3). |
| **occurrence grades** | A reference to code is **instance-pinned** (exact form-instance) or **continuant-floating** (the var, resolved by a declared policy). Interface shape only in v0 (§7). |

## 2 · Material model

2.1 **Surface = git blob; store raw + atoms (RULED, Sid 2026-07-09: "we
store the raw format AND the atomic breakdown just like we did for
transcript").** The blob's full text is stored as an immutable source
artifact (transcript pattern: raw source + derived units + anchors);
`source-ref = "git-blob:<sha>"`. Storing ≠ owning: git remains authority
(D-003); the store is a materialized view of it.

2.2 **`(path, commit)` resolves to a blob; the resolution is never
stored.** Path-at-commit → blob is a pure function over the repo (D-003:
"code content resolves live through pinned addresses"). The ingest that
minted a surface MUST record its triggering `(path, commit)` as
provenance; the full mapping stays derivable, git-authoritative.

2.3 **Offsets: UTF-16 code units** over the blob text decoded as UTF-8 —
identical to the parent §2.2 rule and to the markdown adapter's existing
offset arithmetic (JVM string counts). MUST never split a surrogate pair.

2.4 **Redaction for code = a declared, versioned DENY-LIST of whole
files** (MUST, fail-closed): a deny-listed path (`src/app/server/env.clj`
first and always) mints NO surface, NO units, NO edges; every denial is
counted in the sync run's stats with the deny rule-id. Allowed blobs store
verbatim — code gets no span-level redaction in v0.

2.5 Reconstruction guarantee inherited: the stored blob text MUST
re-render byte-identical; blocks are an overlay (parent §2.3).

## 3 · The free cut (code lane)

3.1 Runs per NEW blob at sync. Mechanical, no model calls (MUST).
Declared as `clojure-form-v0@1` per the parent §4.5.

3.2 **Unit = the top-level form.** One block per form; span = the form's
exact text extent; the unit's stored text MUST equal the blob substring at
`[start, end)` byte-for-byte. Docstrings live INSIDE their form's span
(surfaced by the name-card projection, never cut out).

3.3 **Two lanes (MUST).** The cut classes top-level material into **code
lane** (forms — cut NOW) and **commentary lane** (standalone comment runs
— DEFERRED: no units in v0; their spans remain unblocked surface,
addressable by refinement, and the commentary stratum lands later at zero
cost per the parent §14). Rationale: comments are sense-trail material at
the code lens (GROUNDS F1, Sid's reframe).

3.4 **Form vocabulary v0** (closed normalization table over the form's
head symbol; grows only by spec amendment; raw head always retained):

| unit-kind | heads |
|---|---|
| `:clj/ns` | ns |
| `:clj/def` | def, defonce |
| `:clj/fn` | defn, defn- |
| `:clj/record` | defrecord, deftype |
| `:clj/protocol` | defprotocol |
| `:clj/macro` | defmacro |
| `:clj/multi` | defmulti, defmethod |
| `:clj/test` | deftest |
| `:clj/module` | defmodule *(Rama — project row)* |
| `:clj/electric-fn` | e/defn *(Electric — project row)* |
| `:clj/reader-cond` | top-level `#?`/`#?@` (atomic) |
| `:clj/rich-comment` | (comment …) |
| `:clj/other` | anything else (head retained) |

Form is fact; "this fn is a validation helper" is a mark (parent
form/kind law).

3.5 **Block-path = the binding name** (the form's second symbol) for
def-family heads; positional `%06d` for unnamed forms (`:clj/ns` uses
`"ns"`); duplicate names deterministically suffixed `#2`, `#3` … in file
order (MUST — makes unit ids name-carrying and re-cut-stable per blob).
`defmethod` appends its dispatch value to the name.

3.6 Err-coarse + demand refinement inherited (parent §5): sub-form blocks
(a branch inside `relation-outcome`, one PState decl inside `defmodule`)
mint on engagement down the syntax tree, with the engagement recorded as
provenance. No eager sub-form cutting (GROUNDS F3: refinement demand data
decides empirically whether e.g. module children ever earn eager minting).

## 4 · Identity & lineage (the family's novum)

4.1 Identity inherited: deterministic unit ids per (blob-derived
object-key, block-path); re-cutting the same blob MUST resolve, never
duplicate (parent §6.1).

4.2 **Every unit stores its form-text hash** (the existing
`DerivedUnitRow` derived-text-hash field) (MUST).

4.3 **Lineage law — three outcomes per name across a blob succession**
(evaluated per `(parent-commit, commit)` pair per changed path, at
commit-boundary sync):

- **Re-addressed**: same name, same form-text hash in the new blob →
  the SAME atom relocated; NO event, NO edge (MUST NOT mint). Equivalence
  is computable from stored hashes. (Without this, one commit touching
  line 900 fakes ~100 supersessions; `af0e0e2` really changed 6–8 vars —
  GROUNDS F4.)
- **Superseded**: same name, different hash → mechanical `:supersedes`
  edge, from = new form-instance, to = old (the parent §11.3 mechanical
  floor's succession edge, at code grain). `asserted-at-ms` = the commit's
  committer clock — never the wall clock.
- **Break**: name absent in the successor (or newborn with no
  predecessor) → rename/move detection is SILVER: a similarity-scored
  `:supersedes` proposal (git's own rename hints are admissible input),
  tier silver, gold-ratifiable by Sid's ordinary gesture. Never mint
  silver as mechanical (the map must not lie about which it has).

4.4 Prior art, honestly held: codeq content-keyed code segments across
blobs; Unison content-addresses definitions with names as metadata — the
right answer when the tool owns truth; git owns ours, so we reconstruct
content hash + durable name OVER blobs. (Both from general knowledge;
verify before load-bearing use beyond this framing.)

4.5 Whitespace/alpha-normalized hashing (reformat-detection) = recorded
candidate, NOT in v0.

## 5 · Mechanical edge floor (code)

5.1 **Two new registered relation kinds: `:requires` and `:calls`**
(RULED, Sid 2026-07-09 "new kinds") — additive one-line registry change in
the relation kernel. Both are deriver-asserted, tier silver, form-grade
facts.

5.2 **`:requires`** — from the requiring ns to the required ns
(target-kind `:ns`, target-id = ns name), derived from the ns form.
Mechanical, near-certain.

5.3 **`:calls`** — from caller var to callee var (target-kind `:var`,
target-id = ns-qualified name), derived by the analyzer pass (clj-kondo
grade — versioned, fallible-mechanical). **Continuant grain with
current-status semantics** (MUST): edges reflect the analyzed tree at the
synced HEAD; per-version detail rides evidence anchors
(`evidence-source-id` = the calling blob's source, `evidence-anchor-id` =
the calling form's anchor — fields that already exist on RelationEdgeRow).
A call that disappears at HEAD is RETRACTED, never deleted (history stays
in the status log).

5.4 **Deriver actors are VERSION-FREE** (MUST): `import:code-lineage` and
`import:code-analyzer` (names scaffolding); the deriver version rides
`note` (git_spine §2.4 precedent). Grounds: the relation-id includes the
asserter, so a versioned asserter forks every edge AND loses retraction
rights (retract requires actor == stored asserter,
relation_kernel.clj:441-448). Version-free actors make re-runs converge
and let a better deriver retract its predecessor's stale edges. Re-run
strata (parent §14) live in notes + status history.

5.5 Containment is COMPUTED from spans (parent §6.2) — the AST is a
transient parse the deriver walks, never a store (Sid's fork question,
resolved in GROUNDS §7). `defines` edges are NOT stored: name → current
instance is a derived, git-authoritative resolution (§6.3).

5.6 The tool-call joins of the parent §11.3 are unchanged; a session
touching `path@commit` resolves to the blob-surface and refines on
demand. git_spine's session↔commit joins already land on the same targets.

## 6 · Projections (computed, never stored — parent §13 pace-layer law)

6.1 **Containment / outline** from spans; **section outline** (`;; ──`
banners) is a future declared project-convention stratum, not v0.

6.2 **Name-card (DECLS)**: name + arglists + docstring span per unit.

6.3 **Current-instance resolution**: var → file (via ns) → HEAD blob →
unit. Pure, live, git-authoritative; NOT a stored index. If daily use
breaks this (D-001), an index is the named extension point.

6.4 **What-changed**: per commit, the name×hash diff (§4.3's own
computation) — the code-grain morning-answer input.

6.5 **Read-depth / coverage**: grounds-marks over code blocks with a depth
qualifier — absorbs MAP.md's hand-rolled coverage ledger tuple.

## 7 · Occurrence grades (interface shape only in v0)

Assemblies and views referencing code MUST be able to express both:
**instance-pinned** (this exact form-instance) and **continuant-floating**
(this var, resolved per a declared policy — follow-HEAD, pin-at-date, …).
Recorded from Sid's version-aware-UI direction (GROUNDS §8); no builder in
v0 beyond keeping the reference shape two-grade.

## 8 · Conformance (machinery, not quality)

An implementation conforms when it demonstrates, on THIS repo:

1. **Idempotent re-cut** — same blob ingested twice → same unit ids, no
   duplicates, import no-op provable.
2. **Census reproduction** — `relation_kernel.clj`@HEAD yields its
   top-level forms with correct normalized kinds; every unit's text ==
   its blob substring; forms + gaps reassemble the file byte-exactly.
3. **Two-lane honesty** — standalone comments mint NO units in v0 AND a
   refinement block over a comment span works (the spans are addressable
   surface).
4. **Deny-list** — `env.clj` mints nothing anywhere; the denial is
   counted with rule-id in run stats.
5. **Lineage on real history** — syncing the specimen's 4 commits mints
   `:supersedes` for exactly the vars each commit changed (ground truth
   pinned from `git show` at phase time) and ZERO edges for
   re-addressed (hash-equal) name matches.
6. **Move case** — syncing across `119f3f8` (the adapter split) yields
   SILVER proposals (never mechanical edges) for moved vars.
7. **Analyzer floor** — `:requires` edges for the specimen's 6 requires;
   `:calls` var→var matching the specimen's pinned ground truth
   (`oc/fixed-width-order-key` ← 3 call sites; `oc/extract-object-key` ←
   1), each with a resolving evidence anchor.
8. **Retract-on-disappear** — removing a call at HEAD and re-syncing
   retracts the edge (version-free asserter proof); history remains.
9. **No wall clock** — every row/edge derives its times from commit
   clocks; a re-run is byte-identical.

## OPEN — Sid's rulings pending

1. All names (blob-surface · form-instance · lanes · `:clj/*` kinds ·
   deriver actor ids · re-addressed/superseded) — scaffolding until
   recurrence; Sid names.
2. Commentary-lane timing (deferred by his ruling; lands on demand).
3. Normalized-hash reformat detection (recorded candidate).
4. Analyzer scope beyond HEAD (per-branch analysis) — v0 is HEAD-only.
