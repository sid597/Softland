# Code-Atom Contract — clojure form atoms over the container kernel

2026-07-09 · authored by Fable at Sid's round-close instruction ("have the
spec and then the contract derived from it so we can start the
implementation") · **v1 — BINDING** (D-010 live-by-default; Sid redlines
anytime). Derived from `SPEC.md` (same folder, v0) which extends
`../SPEC.md` (block laws, countersigned). Warrant: `GROUNDS.md`.
Package shell: /work-package skill; Rama phase mechanics: /rama skill.

## 1 · Purpose and scope

Make Clojure code addressable at form grain inside the land: every blob of
every `.clj/.cljc/.cljs` file under `src/` + `test/` becomes a stored
surface with derived form-units and span anchors; lineage
(`:supersedes`) and dependency (`:requires`/`:calls`) edges land in the
relation kernel. Consumers, in order:

1. Pointing-and-talking over code in the trail view (blocks to land marks on).
2. Callers/callees context assembly (human + LLM) via one relation read.
3. What-changed-at-form-grain projections (the code-grain morning answer).
4. Read-depth/grounds marks over code (absorbs MAP.md's coverage ledger).

**Non-goals (each an extension point, §10, never a void):** editing code
through atoms (Regime 2, D-003-gated) · commentary-lane units (deferred by
Sid's ruling) · section-outline stratum · name→instance stored index ·
semantic/LLM segmentation · non-Clojure languages · any UI.

## 2 · Placement ruling

**No new module. No new depot, topology, or PState.** (Truth-kind test:
material → object-container; assertions → relation-kernel; nothing new.)

- `src/app/server/rama/object_container/clojure_adapter.clj` — NEW. Pure
  distiller, sibling of `markdown_adapter.clj` and mirror of its fn
  inventory: cut fn (`clojure-form-v0`) · `source-materialization` twin ·
  `clojure-source-import-request` builder. Zero I/O, zero git calls.
- `src/app/server/rama/code_atoms.clj` — NEW. Driver, git_spine package
  shape ("pure adapter fns + a sync driver over the EXISTING public
  APIs"): git blob reading, deny-list, sync loop, lineage lane, analyzer
  lane. Depends on git_spine's public fns where they fit (`read-commits`)
  — never edits it.
- `relation_kernel.clj` — ONE authorized additive edit: `:requires
  :calls` join the `relation-kinds` registry (:58-65). Nothing else.
  ⚠ Coordination: the block-kernel package holds the same authorization
  for `:grounds :assembled-from :refines` on the same set literal — land
  separately, trivial rebase, both packages' gates re-run after either.
- `deps.edn` — ADDITIVE dev/test-visible deps only as P0 rules (rewrite-clj
  and/or clj-kondo; lean rewrite-clj for the cut, clj-kondo for analysis).

Reversal cost: adapter + driver are stateless over public APIs — deleting
them orphans rows (retained, inert) and retracting edges is a replay of
retracts; the registry edit is additive. The seam is the same one
git_spine proved.

## 3 · Rulings (R1–R7; implementers cite these, never re-derive)

- **R1 — store raw + atoms** (Sid): blob text stored as SourceArtifactRow
  raw text, transcript pattern; `source-ref = "git-blob:<sha>"`;
  `source-hash` = oc/source-hash of the text (object-key stays
  content-derived and deterministic). Git remains authority.
- **R2 — two lanes; code lane only in v0** (Sid): standalone comments mint
  NO units; their spans stay unblocked surface. The cut MUST still parse
  positions precisely enough that comment spans are refinable later.
- **R3 — lineage law**: re-addressed (name+hash equal) mints NOTHING;
  superseded (name equal, hash differs) mints mechanical `:supersedes`
  new→old; break cases mint SILVER proposals only. Per (parent-commit,
  commit, changed-path); merge commits evaluate per parent.
- **R4 — version-free deriver actors**: `import:code-lineage`,
  `import:code-analyzer`; version rides `note` (`"clj-atoms-v1|<basis>"`
  style). Grounds verbatim in SPEC §5.4 (retraction rights,
  relation_kernel.clj:441-448; fork-avoidance, git_spine.clj:41-44).
- **R5 — analyzer scope = HEAD** (current-status semantics): `:requires`/
  `:calls` reflect the synced HEAD tree; disappeared edges are RETRACTED.
  Lineage lane covers all commits (`read-commits` is `--all`).
- **R6 — deny-list fail-closed**: path deny-list, `src/app/server/env.clj`
  hardcoded first entry, versioned rule-id; a denied path mints NOTHING
  and is counted in run stats. The driver MUST apply the deny-list BEFORE
  any text leaves git.
- **R7 — naming block-paths**: binding name as block-path (SPEC §3.5),
  positional for unnamed, `#n` dedup, `defmethod` + dispatch value.
  (Scope note: block-path uniqueness is PER BLOB — object-key already
  includes the source hash, so ids never collide across versions.)

## 4 · Data model — SPEC nouns onto existing rows (no new row types)

| SPEC noun | row | notes |
|---|---|---|
| blob-surface | `SourceArtifactRow` (+ version/completion/document rows, md pattern) | source-ref `"git-blob:<sha>"`; format `:clojure` |
| form-instance | `DerivedUnitRow` | unit-kind `:clj/*`; block-path per R7; derived-text-hash = form-text hash (SPEC §4.2) |
| span | `SourceAnchorRow` | UTF-16 offsets; unit text == substring (G2) |
| containment | `CompositionEdgeRow` | v0: every unit's parent = the document container (flat top-level), md-adapter idiom |
| `:supersedes` / `:requires` / `:calls` | RelationEdgeRow via `rk/assert-request` | target-kinds `:code-form` (target-id = unit-id), `:var`, `:ns` — open keywords, no schema change; evidence-source-id = calling blob's source-id, evidence-anchor-id = calling unit's anchor-id |
| idempotency | import: `"imp:clj:<object-key>:…"` (md pattern, PER-BLOB scope); edges: `"code:<relation-id>:<basis>"`, basis = child commit sha (lineage) or HEAD sha (analyzer) — RELATION-scoped journal, spine pattern |

## 5 · Flow (the sync loop; all times from commit clocks)

1. `read-commits` (git_spine, `--all`) → the commit set + name-status.
2. Enumerate `.clj/.cljc/.cljs` blobs under `src/` + `test/` per commit
   (`git rev-list`/`ls-tree`/`cat-file` batch — P0 verifies the cheapest
   correct enumeration); deny-filter (R6); NEW blobs → adapter import
   request → `ocr/append-object-container-request!` → batch-await
   (spine's batch pattern, never serial awaits — git_spine.clj:244-256).
3. Lineage lane: per (parent, commit, changed path) diff the two blobs'
   name→hash maps (R3) → `:supersedes` asserts / silver proposals.
4. Analyzer lane: clj-kondo analysis over the HEAD tree → desired
   `:requires`/`:calls` set → assert missing, retract stale (R5), each
   with evidence anchors.
5. Stats out, spine-style: blobs seen/ingested/denied/converged, edges
   asserted/retracted/proposed, per lane — no silent caps anywhere.

## 6 · The free cut (pure-fn layer)

`clojure-form-v0@1`: text → `[{:block-path :unit-kind :text :start-offset
:end-offset :head :name}]`, top-level forms only, two-lane classification,
positions exact. The parse tool is P0's decision (rewrite-clj lean vs
indexing tools.reader); whatever wins MUST pass G2's byte-fidelity gate on
every file in the repo INCLUDING `relation_kernel.clj`'s `U+0000` string
literals, `.cljc` reader conditionals, and `e/defn` forms.

## 7 · Traps ledger (cite trap numbers in code comments)

- **T1 — versioned asserter**: forks every edge id AND loses retraction
  rights (rk:441-448). → R4. Naive alternative already died in GROUNDS.
- **T2 — supersedes on name-match alone**: one commit touching line 900
  fakes ~100 supersessions (af0e0e2 ground truth: 6–8 changed vars). →
  hash short-circuit, R3; G5 counts.
- **T3 — reading the working tree for history**: file text MUST come from
  `git cat-file <blob-sha>`, never the checkout (the checkout is ONE
  version; git_spine's `doc-document-id` slurps live files deliberately —
  that semantics is for version-addressed doc joins, NOT for code atoms).
- **T4 — wall clock**: any wall-clock stamp double-writes under replay
  (rk trap-4b precedent). → commit clocks only; byte-identical re-runs (G9).
- **T5 — analyzer trust over Rama/Electric macros**: clj-kondo inside
  `defmodule`/`<<sources`/`e/defn` may miss or fabricate usages. → gate on
  the specimen's pinned ground truth (G7); unresolvable forms are SKIPPED
  AND COUNTED, never guessed (no silent caps).
- **T6 — instance-grain call edges**: pairs × versions explodes and every
  version bump re-asserts the world. → continuant grain, current-status
  (R5); instance detail via evidence anchors.
- **T7 — eager sub-form cutting**: over-chunking needs identity surgery
  after marks accrue (parent §5.3). → top-level only; refinement on demand.
- **T8 — `U+0000` literals**: the specimen's sentinel strings
  (rk:74-76) are raw NULs to a naive tool chain; source stays escaped, but
  parse/span tools must not mangle them (G2 byte-compare); never put raw
  control bytes in test literals (harness NUL trap, quirks file).
- **T9 — serial awaits**: one 5s await per blob makes first sync
  hours-long. → append ALL, then await ALL (spine trap-7 pattern).
- **T10 — deny-list after read**: redaction that runs after text enters
  any buffer/log has already leaked intent. → deny by PATH before
  cat-file (R6); G4 plants the check.
- **T11 — registry collision**: two live packages edit `relation-kinds`.
  → additive one-line edits, separate commits, re-run both packages'
  gates after either lands (§2 coordination note).

## 8 · Acceptance gates (IPC tests in one suite unless marked)

SPEC §8 items 1–9, made executable — G1 idempotent re-cut · G2 census +
byte-fidelity (whole-repo parse fidelity is the P0 spike promoted to a
test on the specimen + one `.cljc` + one `.cljs` fixture) · G3 two-lane
honesty · G4 deny-list (env.clj absent everywhere, denial counted) · G5
lineage counts on the specimen's real 4 commits (expected sets pinned at
phase time from `git show`, derivation commented in the fixture) · G6
silver-only move proposals across `119f3f8` · G7 analyzer floor
(specimen's 7 ns dependencies per P0 B.1; `oc/fixed-width-order-key` ← 3 call sites,
`oc/extract-object-key` ← 1, anchors resolving) · G8 registry additive
(existing rk suite still green; unregistered-kind rejection untouched) ·
G9 no-wall-clock byte-identical re-run · G10 retract-on-disappear (R5,
version-free retraction proof) · G11 (review-time) MAP.md coverage tuple
expressible as grounds-marks — a worked example in the gate artifact, no
UI.

Test harness invariants: deterministic microbatch barrier (submit!==+1),
physical PState readers for negative invariants, injectable task counts,
one IPC launch (work-package skill / quirks file).

## 9 · Stop clauses (never improvise on binding docs)

- P0 finds NO parse tool passing whole-repo byte-fidelity → stop; options
  + evidence to Sid (the cut law may need amending, that's his call).
- clj-kondo cannot recover the G7 ground truth even with config → stop;
  analyzer lane descopes to `:requires`-only as the RECORDED option, Sid
  rules.
- Any needed edit outside §2's allowlist → stop-clause, never a silent
  edit.
- Two binding docs genuinely conflict → decisions.md Open Questions as
  PROPOSED, verbatim citations, options + recommendation.

## 10 · Extension points (refusals kept warm)

Commentary-lane distiller (reserved rule-id; Sid's timing) ·
section-outline stratum · name→instance index (only on a D-001 form-break
of §6.3 live resolution) · deletion visibility (`:dead-end` candidates) ·
per-branch analyzer scopes · reformat detection (normalized hash) ·
occurrence floating-grade resolution policies (design input recorded in
GROUNDS §8) · other languages (a new adapter each, same laws).

## 11 · Input manifest (what a fresh model needs to reproduce this)

`code-atom/SPEC.md` + `GROUNDS.md` (this folder) · `../SPEC.md` (parent
laws) · `src/app/server/rama/relation_kernel.clj` (FULL — client API,
registry, retraction law :441-448, evidence fields :249-255) ·
`object_container/markdown_adapter.clj` (FULL — the materialization +
request-builder shape to mirror) · `git_spine.clj` (FULL — driver shape,
batch-await, stable keys, version-free asserter :41-62) ·
`object_container.clj` :356-368 + :617-625 + :705-712 (payload accessors,
import validation) · decisions.md D-003/D-004 (narrow) ·
`memory/implementation-quirks.md` · /rama + /rama-pitfalls +
/work-package skills.

## 12 · Handoff

- **Phases** (one per fresh context; fresh Opus subagents for
  validation/review; Fable orchestrates + gates):
  - **P0 — parse + analysis spike** (fresh subagent; artifact
    `P0_PARSE_SPIKE.md`): (a) rewrite-clj vs indexing tools.reader over
    EVERY `.clj/.cljc/.cljs` in src+test — byte-span fidelity, total
    coverage, `U+0000`/reader-conditional/tagged-literal survival; pick +
    record; (b) clj-kondo analysis smoke on the specimen — can it recover
    the G7 ground truth, what config do Rama macros need; (c) cheapest
    correct blob enumeration per commit. Default-fail adjudication on
    return.
  - **P1 — adapter** (`clojure_adapter.clj` + test): cut, materialization,
    request builder → G1–G4 green.
  - **P2 — driver, lineage lane** (`code_atoms.clj` + test): blob sync +
    R3 lineage → G5, G6, G9 green.
  - **P3 — analyzer lane + registry edit**: `:requires :calls` + kondo
    pass + retraction → G7, G8, G10 green.
  - **Gate review** (Fable): full-code read, falsification pass, G11,
    verdict + D-006 note; then close + retro per the work-package skill.
- **Definition of done**: G1–G10 green as IPC tests in one suite + the
  documented REPL invocation syncing THIS repo and printing: the specimen
  census, `oc/fixed-width-order-key`'s callers, and `relation-outcome`'s
  supersedes chain — the dogfood receipt.
- **Budget**: local + subscription only, no API dollars (standing).
- **Hard rules**: never read `env.clj` (deny-listed AND never opened);
  code and docs in separate commits; code commits on Sid's word; docs
  commits automatic on the local docs branch.
