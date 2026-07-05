# Code Ingestor — Product Contract (Proposed)

> SUPERSEDED (2026-06-10): the canonical proposed contract is now
> `../build/code-ingestor/PRODUCT.md` (fresh cut, ranked #1 of the 8-run
> comparison). The synthesized write-up: `../build/code-ingestor/index.html`.
> The 8-run comparison (this draft preserved verbatim as run 7):
> `../build/code-ingestor/comparison.html`. Do not ratify against this draft.

Status: Claude-proposed product contract, produced autonomously at Sid's
request ("complete everything"). Every decision below is **proposed, not
ratified** — needs Sid sign-off and a Codex falsification gate before any
Rama/topology design. Produced from source code + the ingester contract only;
no design canon was loaded (per the principal-designer manual-only law).

Position in the architecture:

```text
.md file         -> Markdown Ingestor    \
chat transcript  -> Transcript Ingestor    -> Object-Container Kernel
code artifact    -> Code Ingestor        /
```

The code ingestor emits the same `:object-container/import-material` contract
as the other two. Nothing in this doc proposes new kernel machinery.

Ground truth read for this contract:

```text
src/app/server/rama/object_container.clj            (kernel rows, key helpers, idempotency)
src/app/server/rama/object_container/markdown_adapter.clj
src/app/server/rama/object_container/transcript_identity.clj
docs/current-mental-model/architecture/object-container-ingester-contract.md  (the 8 gates)
```

---

## Round 1 — Purpose & First Consumer

**Decision (proposed):** The code ingestor's first job is to make code
*addressable in the same world as conversations and docs*, at file
granularity, version-pinned to commits. The first consumer is **trail
inspection**: agent transcripts are already ingested, and their tool calls
name file paths (`Read`, `Edit`, `Write` with `file_path`). Today those are
dead strings. With code ingested, the product question becomes answerable:

```text
"What did this conversation actually touch,
 and what did that code look like at that moment?"
```

Options considered:

```text
A. trail resolution (chosen)      file-level, commit-pinned; joins the two
                                  corpora that already exist in the kernel
B. commit-metadata lane           cheapest, but containers would hold commit
                                  messages, not code — defers the actual
                                  ingestor problem instead of starting it
C. code review as trail           needs A first; review references code
D. zoom-100 editor substrate      needs symbol-level identity + working-tree
                                  state; largest possible scope; premature
E. dogfood self-ingestion         not a consumer, a corpus choice — subsumed:
                                  the Softland repo IS the first corpus
```

Why A: it is the narrowest consumer that exercises cross-source value of the
common kernel (the reason the kernel is common at all), and every later
consumer (C, D) needs its output. It also fixes the granularity decision
(files, not symbols) and the version decision (commits, not saves) without
guessing.

**Consumer-shaped acceptance test:** given a transcript tool-call container
(file path + timestamp), resolve to the code file's ObjectContainer and the
Revision current at that time. This join is computed at projection/query
time — it does NOT require new base-truth edge types (RelationEdge stays
spec-only; see Parked).

---

## Round 2 — Unit of Identity & Granularity

The two existing ingestors key identity differently, and the difference is
load-bearing:

```text
markdown:    object-key = sha256(source-ref + ":" + content-hash)
             (object_container.clj:201-203)
             -> identity per CONTENT VERSION; a changed file is a brand-new
                object-key, new document container, new universe

transcript:  object-key = "chat:" + sha256(source + ":" + conversation-id)
             (transcript_identity.clj:6-8)
             -> STABLE identity over time; versions accumulate inside it
```

**Decision (proposed):** code follows the transcript precedent, not the
markdown one. A code file must keep one identity across commits or the
product question from Round 1 ("the same file across many commits") can never
be asked.

Identity assignments:

```text
repo            -> ObjectContainer
                   repo-id is explicitly registered (a declared name), not
                   inferred from remote URL; multi-repo policy parked

code file       -> ObjectContainer
                   object-key = "code:" + sha256(repo-id + ":" + repo-relative-path)
                   path is a native id under the contract's
                   "repo/snapshot policy" hint; recorded via
                   NativeIdentityClaimRow (object_container.clj:129-132),
                   which exists for exactly this

rename/move     -> new path = new native id = new container
                   continuity across renames is parked as a future
                   relation/claim policy; slice 1 does not fake it

top-level form  -> DerivedUnit, slice 2 (not slice 1)
                   Clojure top-level forms are the natural sub-file unit for
                   this repo; they follow the markdown-block pattern
                   (derived identity, graduate on touch)

symbol          -> ObjectContainer only "when parser identity is stable"
                   (contract hint) — parked until that is true
```

**Anchoring (proposed):** SourceAnchors are version-pinned to immutable
content. `source-hash` = git blob sha; an anchor into a blob can never drift
because the blob can never change. "Anchor survival across edits" is
deliberately NOT solved — the current view maps through the revision chain,
and re-anchoring into newer revisions is a projection problem, not a
base-truth problem.

---

## Round 3 — Time & Versioning

Code is the first source whose versioning is external and richer than the
kernel's (git). The product decision is the authority split:

**Decision (proposed): git remains the authority for code history; the
kernel mirrors only what has been ingested, and every mirrored row carries
the commit sha in its source-ref so the kernel never pretends to be the
authority.** Translation between two truth systems, not replication of one
into the other.

Concrete mapping:

```text
ingest granularity   commit boundary (not file save, not working tree)
SourceArtifactRow    one per (path, blob); source-hash = git blob sha;
                     source-ref = repo-id + path + commit sha
RevisionRow          one per file change per ingested commit;
                     parent-revision-id = previous ingested revision of that
                     file along first-parent lineage of the watched branch
SourceVersionRow     commit manifest entry (which commit produced this
                     version), ordering by commit time + sha
```

Options considered and rejected for slice 1:

```text
working-tree ingestion   no stable version id to anchor to; every save would
                         mint un-replayable artifacts; needed eventually for
                         the editor (zoom 100) — parked with that consumer
merge-DAG parentage      kernel RevisionRow has a single parent-revision-id;
                         first-parent lineage is the honest subset; full DAG
                         parentage parked
```

**History depth (proposed):** baseline harvest at HEAD, then forward-only
watch. Retroactive backfill of older commits is the same idempotent import
path run over a bounded commit range — a policy knob, not new machinery.
Slice 1 ships harvest + watch; backfill is allowed but not required.

---

## Round 4 — Acquisition, Trigger & Safety

**Modes (proposed):** mirror the transcript pattern — `harvest` (full tree at
one commit) + `watch` (new commits on one configured branch). The trigger is
the commit boundary; whether detection is polling `git log` or a post-commit
hook is implementation, not contract.

**Scope (proposed):** a file is ingestable only if ALL hold:

```text
1. git-tracked            (gitignored files can never enter; this alone
                           excludes src/app/server/env.clj, which is the
                           canonical must-never-ingest file)
2. allow-list extension   .clj .cljc .cljs .json — exactly the committable
                           set from the repo's own rules; .md is explicitly
                           OUT (docs are private and belong to the markdown
                           ingestor under its own policy)
3. not on the deny-list   hard deny-list naming src/app/server/env.clj
                           explicitly — defense in depth on top of (1)
```

**Fail-closed rule:** if the deny-list cannot be loaded, ingest refuses to
run. Denial happens at the connector stage — denied content never reaches the
normalizer, the import payload, or any log line.

**Scale (proposed):** one import-material request per file per commit
(mirrors per-line transcript imports). The blob sha makes idempotency
trivial:

```text
import-key           = "imp:code:" + object-key + ":" + sha256(commit-sha + ":" + path + ":" + blob-sha)
idempotency-key      = import-key
material-fingerprint = kernel's import-material-fingerprint over the payload
```

Re-running harvest or backfill replays into accepted decisions
(fingerprints match), exactly like markdown's `imp:md:` discipline
(markdown_adapter.clj:440-442).

---

## Round 5 — First Slice

**Slice 1: "repo baseline + commit watch, file-level."**

```text
1. register repo        repo ObjectContainer, declared repo-id
2. harvest at HEAD      per allowed file, one import-material request:
                          SourceArtifact (blob)
                          file ObjectContainer (stable code: key)
                          NativeIdentityClaim (path claim)
                          Revision (blob content)
                          SourceAnchor (whole-file span into the blob)
                          CompositionEdge (repo -> file, path as order key)
                          SourceVersion (commit manifest entry)
3. watch branch         new commit -> per changed allowed file:
                          new SourceArtifact + Revision
                          container current-content/current-revision update
4. read models          file-by-path index, commit timeline,
                          code-at-version resolution
5. consumer proof       query: transcript tool-call (path + time)
                          -> code container + revision current at that time
```

Directory containers are NOT created — directories are derivable from paths
and live in projections if a consumer wants a tree view. File deletion
semantics are parked (tombstone vs visibility change) — slice 1 may ignore
deletes with that noted as a known gap.

---

## The 8 Contract Gates, Answered

```text
1. Common base truth rows?
   SourceArtifact (blob@commit), ObjectContainer (repo, file), Revision,
   SourceAnchor, CompositionEdge (repo->file), SourceVersion,
   NativeIdentityClaim (path claims). All written through
   :object-container/import-material.

2. Source-specific projections / operational indexes?
   file-by-path index, commit timeline, code-at-version resolution,
   ingest-run status (harvest/watch progress), later: code symbol index.
   All explicitly read models, never canonical identity.

3. Which module owns the common base PStates?
   The object-container kernel module, unchanged. The code ingestor adds
   zero base PStates.

4. Containers: common rows or source-local island?
   Common ObjectContainer rows via import-material. There is no
   code-local $$containers-by-id. (This is the fail condition the
   transcript path originally hit; the code plan starts on the right side.)

5. Source-local rows explicitly read models?
   Yes — commit/run/index rows mirror the transcript-ops pattern: they may
   mirror completion but never own object identity.

6. Native-id containers vs DerivedUnits?
   Native: repo-id (declared), file path (per repo/snapshot policy),
   claimed via NativeIdentityClaimRow. Derived: top-level forms (slice 2),
   any anonymous span. Symbols parked until parser identity is stable.

7. SourceArtifacts versioned and anchored how?
   Versioned by git blob sha; source-ref carries repo-id + path + commit
   sha; anchors are version-pinned to immutable blobs (no drift by
   construction).

8. Source-specific read models still required for product use?
   file-by-path, commit timeline, code-at-version — all serving the
   Round 1 consumer (trail resolution).
```

---

## Salvage From Existing Adapters

```text
harvest + watch acquisition shape          transcript ops
byte-correct content reading discipline    transcript JSONL reader (F4)
import-key / idempotency / fingerprint     markdown adapter (imp:md: pattern)
NativeIdentityClaim machinery              transcript path (native UUIDs)
connector -> normalizer -> interpreter
  -> request-builder stage structure       both adapters
```

---

## Parked (explicit deferrals, each a decision not an omission)

```text
rename/move continuity        new path = new container in slice 1; continuity
                              needs a relation/claim policy that does not exist
merge-DAG revision parentage  first-parent lineage only in slice 1
working-tree overlay          required by the zoom-100 editor consumer, not by
                              trail resolution; no stable version id today
form/symbol granularity       slice 2 (forms as DerivedUnits); symbols when
                              parser identity is stable; graduation triggers
                              for code units undefined
cross-reference edges         calls/requires/imports are RelationEdge-shaped;
                              RelationEdge is spec-only — deferred knowingly
durable trail->code edges     slice 1 joins at projection time; if the join
                              proves product-critical it may graduate to a
                              durable edge type later
multi-repo identity policy    repo-id minting/collision rules
file deletion semantics       tombstone vs visibility change
branch plurality              slice 1 watches exactly one branch
```

---

## Next Steps (gated)

```text
1. Sid ratifies / amends the round decisions above
2. Codex falsification gate on this doc (prompt prepared, see session notes)
3. Only then: Rama plan for the code adapter, written against this contract,
   using $rama / $think-in-rama / $rama-pitfalls / $rama-retro-lens
```
