# Research Brief — Pass 2 for the Code Ingestor Spec

Date: 2026-06-11. Feeds: `SPEC.md` (this folder), specifically every VERIFY
marker and the OPEN entries that name this brief.

Haul 1 (`versioning-paradigms-research.md`) fetched 25 sources / 119 claims but
its own calibration note says the adversarial verify pass was CUT. This pass is
smaller and harder: 7 items, each with the question it must answer, the spec
section that consumes it, and a done-when. **Adversarial verification is
REQUIRED this round** for anything marked load-bearing — a claim survives only
with a primary-source citation a skeptic agent failed to refute.

Output location: `build/code-ingestor/research-pass-2.md` + per-item raw notes.
Method: workflow fan-out (fetch → extract → adversarial verify → synthesize),
same harness as haul 1 plus the verify stage that was cut.

---

## Item 1 — Verify pass on haul-1 load-bearing claims (PRIORITY 1)

The spec currently leans on these; each is marked VERIFY in SPEC.md:

```text
a. Pelgrin et al. 2021: member-level versioning alone CANNOT reconstruct
   group-level history (the necessity proof for manifests)        -> Part 7 manifest-at-time
b. Molhado: pure product versioning makes cross-version references
   inexpressible (the rejection of Model C)                       -> Part 2 composition axis
c. MolhadoRef: refactoring ops DECLARED as first-class entities, never
   inferred; measured 2.2–2.8x storage cost; correct rename history -> Part 2 inference axis
d. Katz 1990: group check-in / configurations as first-class versioned
   objects; "versions minted at semantically meaningful points"    -> Part 4.3
e. Version-count wall: ~21k versions load failure; 6h query timeout
   at 1,299 versions (which systems, which conditions — is the
   number transferable or artifact-specific?)                      -> Part 4.5
f. Pijul: conflicts as first-class representable states            -> Part 8 merge stance
```

Question: does the primary source actually say this, with what scope limits?
Done when: each claim marked CONFIRMED / WEAKENED(how) / REFUTED with citation.

## Item 2 — codeq, primary material (PRIORITY 2)

Haul 1 named it a coverage gap ("known from the seed blog post"). We invoke
Rich Hickey's code-as-data lineage in Part 2 partly from general knowledge.

Questions: codeq's actual schema (code quantums — what grains?); how identity
of a def is anchored (name? namespace+name?); how it handled re-def /
file-move; why it stalled (technical or adoption?).
Consumes: Part 2 identity axis, Part 3 form-grain prep.
Done when: codeq's identity scheme is restated in OUR vocabulary (minted /
content / name-based, per grain) with its observed failure modes.

## Item 3 — Inferred-continuity state of the art (PRIORITY 1, blocks an OPEN)

GumTree (AST diff), RefactoringMiner (refactoring detection), commit
untangling (Herzig & Zeller lineage and successors).

Questions: published precision/recall for rename/move/refactoring detection on
real repos (per operation type if available); is anything Clojure/Lisp-capable
or is it all Java-centric; what input do they need (full AST? VCS history?);
untangling: how do they decompose tangled changesets and with what accuracy?
Consumes: Part 3 rename-continuity OPEN (the claims policy needs realistic
confidence priors — if detection is 95%+ on renames, claims can be bolder than
if it's 70%); Part 4.4 derived commitment (untangling IS retroactive
segmentation of code changes).
Done when: a table of operation-type → best-known accuracy → tool → input
requirements, with a one-paragraph "what this licenses our claims policy to do."

## Item 4 — Software Heritage SWHIDs (PRIORITY 2)

Intrinsic, content-derived identifiers for source artifacts at planetary scale.

Questions: exact SWHID construction (what's hashed, how trees/releases are
identified); how they handle the identity-vs-naming split (origins vs
artifacts); what identity questions they explicitly punt on (continuity?
rename?); scale numbers (dedup ratios if published).
Consumes: Part 2 (does anything there change our value-vs-identity split?),
Part 5 (content-addressed storage engineering precedent at scale).
Done when: one page mapping SWHID onto our identity/value/place triple +
any storage lesson worth importing.

## Item 5 — Process mining (PRIORITY 3, feeds derived commitment)

Van der Aalst lineage: discovering process structure from event logs.

Questions: what the field calls our problem (trace clustering? case id
inference? episode segmentation?); established techniques for segmenting an
event stream into coherent "cases" WITHOUT a predeclared case id — accuracy
and assumptions; anything on causally-ordered (DAG) logs vs purely sequential
logs; anything on LLM-assisted process discovery (2023+).
Consumes: Part 4.4 (segment-by-braid constraint — does the field confirm
wall-clock windows are the wrong cut?).
Done when: the derived-commitment OPEN has named prior art + the braid
constraint is either supported or challenged by the field's findings.

## Item 6 — Versioned-text storage engineering (PRIORITY 3)

git packfiles (delta chains + zlib, repack heuristics), Fossil's delta
encoding, Dolt/Prolly chunking.

Questions: at SMALL scale (hundreds of files, thousands of versions), what
does dedup/compaction actually buy — when does naive full-copy storage stop
being fine (numbers, not vibes); what's the simplest scheme that preserves
O(change) diff cost (Prolly claim, verify)?
Consumes: Part 5.3 OPEN entries (externalization threshold, 3x redundancy,
chunking) — gives them honest trigger numbers.
Done when: a "you are fine until approximately X" statement with reasoning,
so the storage OPENs have real criteria instead of "at scale."

## Item 7 — Image-system form-grain versioning (PRIORITY 4, slice-2 prep only)

ENVY/Epicea (Smalltalk method-level versioning), Monticello, Stellation
(fine-grained SCM), PIE (Goldstein/Bobrow layers).

Questions: how method/form-grain identity was anchored in systems that HAD it
in production; what the unit-of-change UX was; why fine-grained SCM lost to
file-grained (Stellation specifically).
Consumes: Part 3 form-grain OPEN (slice 2). Lowest priority — skip if budget
runs short; nothing in slice 1 depends on it.
Done when: form-grain identity precedents table, or explicitly deferred.

---

## Out of scope for this pass

- Automerge/Yjs CRDT history (haul-1 gap, but relevant only to working-tree
  streams — OPEN(native editing), not slice 1).
- Xanadu primary sources (vision-level, no spec section consumes it).
- Anything re-fetching haul-1 sources beyond the Item-1 verify pass.

## Budget & sequencing

Items 1+3 first (they block VERIFY markers and one OPEN with a real decision
behind it). 2+4 second. 5+6 third. 7 only with leftover budget.
If the pass must shrink: do Item 1 fully and Item 3's rename-accuracy half —
those are the only two the spec cannot honestly ratify without.
