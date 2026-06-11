# The Code Ingestor — Versioning and Storage (Spec)

Status: Claude-proposed skeleton with all conversation-settled decisions filled in.
Date: 2026-06-11.
Gates, in order: (1) Sid ratifies/amends every DECIDED and PROPOSED marker,
(2) research pass per `RESEARCH-BRIEF.md` fills the marked sections,
(3) Codex falsification gate, (4) only then Rama Phase 0.

Provenance: this session's versioning discussion + `versioning-paradigms-research.md`
(haul 1, claims NOT yet adversarially verified — see brief) + `PRODUCT.md`
(2026-06-10 contract, treated as INPUT: absorbed or revised, not pointed at) +
source verification of `src/app/server/rama/object_container.clj` (line cites below
are to that file unless noted).

Markers:

```text
DECIDED    settled in this conversation, pending Sid ratification
INHERITED  from PRODUCT.md or kernel source; survived the reopening unchanged
PROPOSED   new in this spec; weakest class, needs explicit ratification
OPEN(c)    undecided; c = the criterion that will decide it
VERIFY     awaiting the research pass or a source read
```

Rigor classes (the immutability gradient applied — every section is COVERED;
the class says what may change later):

```text
FROZEN     row/event vocabulary: evolves by addition only, never reinterpretation
REVISABLE  machinery and read models: rewrite freely; their WRITE OUTPUT is FROZEN
```

Normative tools used throughout:

- **Producer matrix** — every FROZEN field is justified against all three
  producers of commitment structure, or removed:
  `T` = translated (source arrives versioned: git), `N` = native/declared
  (an inhabitant commits in the loop), `D` = derived (an observer discovers
  structure in the log after the fact and proposes it).
- **Grain triple** — identity grain ≠ version grain ≠ address grain; the three
  dials are set independently and stated explicitly.

---

## Part 1 — What We Are Building (the pipeline spine) [REVISABLE, write output FROZEN]

One adapter in the existing family (`.md` → markdown, transcript → transcript,
code → THIS), emitting the same `:object-container/import-material` request
family. Zero new kernel base PStates for ingestion itself; Part 4 adds the
manifest/ref row family (new rows, purely additive). Stage structure inherited
from the two proven adapters [INHERITED]:

```text
registration -> connector -> normalizer -> interpreter -> request-builder -> kernel
```

**Registration** [INHERITED]: repo becomes an ObjectContainer; repo-id is
DECLARED at registration (user-named, never inferred from remote URL); local
root path(s) recorded for trail-resolution path normalization.

**Connector (acquisition)** [INHERITED, mechanics REVISABLE]:

```text
harvest    one-time: full tree at one commit
watch      passive: new commits on ONE configured branch; trigger = commit
           event, never filesystem events (working tree has no stable version id)
backfill   same idempotent import run over a bounded commit range; a policy
           knob, not new machinery; allowed, not required, slice 1
```

**Normalizer** [REVISABLE]: bytes at (repo, path, commit) → normalized material
record. Byte-correct reading (kernel `sha-256-bytes` discipline, transcript F4
precedent). Text decoding policy: VERIFY (what do md/tr normalizers do with
encoding errors; binary files are out of scope slice 1, see Part 5).

**Interpreter** [code REVISABLE, policy FROZEN]: the stage that ENCODES Parts
2–4 into emissions. What mints a container, what mints a revision, what
manifest gets pinned, what claims get recorded. Its policy is this spec; its
implementation is disposable.

**Request-builder** [REVISABLE, output FROZEN]: assembles import-material
payloads + manifest/ref emissions under the Part 6 write contract, with the
`imp:code:` idempotency family.

**Scope & safety** [INHERITED — unchanged from PRODUCT.md cluster 4]:
git-tracked only (gitignored can never enter; `env.clj` structurally excluded);
extension allow-list per repo policy (default `.clj .cljc .cljs .json`; `.md`
explicitly OUT — docs belong to the markdown ingestor's privacy policy; `.edn`
named candidate, off by default); hard deny-list naming `src/app/server/env.clj`,
fail-closed if unloadable. Denial at the connector; denied content never reaches
the normalizer, payloads, or logs.

**First consumer** [INHERITED]: trail-to-code resolution (transcript tool-call
path + timestamp → file container + revision current at that moment + exactness
flag). The consumer that keeps every layer below honest.

---

## Part 2 — Strategy (paradigm selections, with rejections recorded) [FROZEN]

Each axis from the versioning research, the selection, and what was rejected.
All load-bearing research claims carry VERIFY until the brief's verify pass runs.

| Axis | Selection | Rejected, and why |
|---|---|---|
| Identity strategy (per grain) | **Minted container ids anchored by claimed native ids** — file's native id = (repo-id, repo-relative-path), recorded via NativeIdentityClaimRow (:129, $$native-identity-claims-by-container :1738) [DECIDED] | *Content-derived identity* (Unison): we do not own Clojure's semantics (macros, reader conditionals, load order) and must round-trip text; content hashes are VALUE addressing only. *Pure name identity with no minted id*: rename destroys the thread with no recovery point. |
| Composition model | **Model A + Katz: per-member revision threads, plus configurations as FIRST-CLASS objects (manifests, Part 4)** [DECIDED, VERIFY Katz] | *Model B total versioning*: proliferation cascade (ORION). *Model C product versioning*: cross-version references inexpressible (Molhado) — anchors and trails REQUIRE container@revision as a first-class address. *Model D hash-pinning as identity*: kept at value layer only. |
| Change representation | **Snapshot artifacts as stored truth; delta and annotation views are read models** [DECIDED, per research Finding 4] | Delta-as-truth: chains degrade with history length (OSTRICH); replay cost lands on every reader. |
| Authority | **Git remains authority for code history; the kernel records what Softland has SEEN of git, never what git "is"** — every mirrored row carries commit identity in source-ref [INHERITED] | Kernel-as-authority for mirrored code: would make every force-push a kernel lie. |
| History mutability | **Append-only seen-history.** Ref rewrite upstream (rebase/force-push) → record the new lineage; never delete the old facts. Divergence is data. [DECIDED] | Mirroring git's forgetting: the log substrate is strictly richer than git history; discarding that is the one thing the substrate forbids. |
| Inference vs declaration | **Declared > inferred (MolhadoRef principle, VERIFY).** Inference enters only as claims with provenance + confidence, graduating via acceptance. Identity quality ceiling is set by where editing acts happen: mirror phase = heuristic ceiling; native phase = declared acts. [DECIDED] | Accepting similarity heuristics (git rename detection) as truth at ingest: forbidden by the existing contract, kept forbidden. |

---

## Part 3 — Chunks (granularity) [FROZEN]

**The grain ladder and the three dials** [DECIDED]:

```text
grain      identity dial                  version dial                 address dial
repo       ObjectContainer (declared id)  manifest per ingested commit —
file       ObjectContainer                Revision per (commit where    byte-span SourceAnchor
           (claimed path native-id)       file changed)                 into immutable artifact
form       DerivedUnit, slice 2;          inherits file revision        block-path + span
           graduate on touch              until graduated               (markdown-block precedent)
symbol     parked until parser            —                             —
           identity is stable
directory  NOT a container (projection from paths)                      [INHERITED]
```

- Identity grain, version grain, and address grain are independent dials.
  Slice 1: identity at file grain, versions at commit grain, addressing at
  byte-span grain. Forms arrive later as a NEW identity grain without touching
  the version or address machinery. [DECIDED]
- Semantic chunks (units of meaning: files, forms) are an identity question.
  STORAGE chunking (content-defined dedup, Prolly-style) is a value-layer
  optimization — Part 5 OPEN, never an identity mechanism. [DECIDED]
- Rename/move: new path = new native id = new container in slice 1; continuity
  later as explicit claims (never accepted truth), policy OPEN(GumTree /
  RefactoringMiner research — need realistic precision/recall before a claims
  policy is honest). [INHERITED + OPEN]
- Clojure form identity (slice 2 prep): name-anchored with declared/claimed
  continuity; known hard cases to spec then: defmethod (name + dispatch value),
  generated defs (defrecord), reader conditionals, non-def top-level forms.
  OPEN(form consumer exists). [PROPOSED]

---

## Part 4 — Versioning Framework [FROZEN]

### 4.1 The three levels of intentionality [DECIDED]

```text
1 arrival      depot log: ordering + timestamps, physical time.
               Retention is free; QUERYABILITY IS EARNED (no ambient as-of;
               every historical view is deliberately materialized).
2 acceptance   kernel decisions: request -> decision -> accepted facts.
               EXISTS ($$requests/$$decisions/$$events :1689-1697).
               Every Revision is already an intentional act at single-container grain.
3 commitment   sparse, named assertion that a CONFIGURATION across many
               containers is a meaningful waypoint, with ancestry.
               THIS SPEC ADDS IT: manifests + refs.
```

### 4.2 Verified kernel baseline (what exists today)

```text
RevisionRow      [revision-id container-id parent-revision-id content-text
                  content-hash order-key created-at-ms created-by event-id]   (:104-106)
                 -> per-container LINEAR chain, SINGLE scalar parent
$$revision-history-by-container (:1710)                                       -> timeline exists
$$source-versions-by-ref / $$source-latest-by-ref (:1701-1703)                -> per-ref version index exists
ObjectEditPayload [.. edit-client-id edit-seq edit-lineage-key ..] (:45-48)   -> native declared edits
                                                                                 already a request type
                                                                                 (matrix column N has
                                                                                 kernel precedent)
```

**Design consequence of verification** [DECIDED, revises an earlier in-chat
position]: `parent-revision-id` is a scalar field in an already-deployed,
serialized defrecord. Reshaping deployed records is a migration event, not an
edit. Therefore the version DAG lives where the model says it lives anyway —
at the CONFIGURATION level, in NEW row types (additive evolution). RevisionRow
keeps its scalar parent as the honest "first-parent" subset; file-level
merge-lineage across branches is OPEN(branch plurality lands) and would arrive
as claims/edges, not as a RevisionRow reshape.

### 4.3 Manifest row family [PROPOSED — the core new vocabulary]

A manifest is a bound configuration (every reference pinned) minted as a
first-class object. Every field justified against the producer matrix (T/N/D):

```text
ManifestRow
  manifest-id          minted kernel id, NOT the commit sha            [T: sha is evidence not id; N/D: no sha exists]
  repo-container-id    which repo's configuration                      [all three]
  pins                 {container-id -> revision-id}, FULL tree        [bound configuration: all-pinned (C&W);
                                                                        absence of a container = deleted/absent
                                                                        at this configuration — answers
                                                                        code-at-time deletion honestly]
  parent-manifest-ids  LIST of manifest-id                             [T: git parents (slice 1 populates
                                                                        first-parent only, list shape anyway);
                                                                        N: previous ref position;
                                                                        D: joined causal braids may be several]
  authority            :git-mirror | :native | :derived                [forced by the matrix; the calibration
                                                                        bit at commitment level — rendering
                                                                        MUST distinguish, the map must not lie]
  evidence             typed by authority:                             [T: {commit-sha branch committed-at-ms}
                                                                        N: {request-id audit-id}
                                                                        D: {event-range rationale confidence
                                                                            model-id}]
  status               :proposed | :accepted | :superseded             [T/N mint :accepted; D mints :proposed
                                                                        and graduates — reuses the kernel's
                                                                        claim/graduation PATTERN]
  superseded-by        manifest-id or nil                              [derived commitments are revisable;
                                                                        supersession, never deletion]
  created-at-ms created-by event-id                                    [kernel row conventions]

RefRow (thin mutable naming layer — the ONLY mutable thing, git's refs lesson)
  ref-name             e.g. "repo/<repo-id>/branch/main"
  repo-container-id
  manifest-id          current target
  moved-at-ms moved-by event-id
  (+ $$ref-history append index — ref moves are events, history kept)
```

Slice 1 mints: one manifest per ingested commit on the watched branch
(authority :git-mirror), one ref per watched branch, moved per commit.

Pins are the FULL tree per manifest [PROPOSED]: self-contained bound
configurations, no chain-walk to answer code-at-time; cost at dogfood scale
~114 pins × 318 commits ≈ 36k entries — trivial. Structural sharing /
pin compaction: OPEN(scale: when pins × manifests stops being trivial;
git-tree/Prolly precedent named in research).

**Atomicity honesty** [PROPOSED, rama-pitfalls discipline]: manifest and the N
file imports it pins land on different partition keys; cross-`|hash` writes are
NOT one event. Invariant: a manifest must never be accepted while a pinned
revision is missing → manifest emission is GATED on the N per-file import
completions (existing ImportCompletionRow machinery :94-97 is the gate);
ref move last; partial progress visible in run status, never silent.

### 4.4 Derived commitment (the LLM-over-log capability) [PROPOSED, vocabulary now, machinery later]

Retroactive commitment: an observer (LLM agent) reads a span of the depot log,
finds "this braid of events was one coherent piece of work," and proposes a
manifest (authority :derived, status :proposed, evidence = event-range +
rationale + confidence). Graduates to :accepted by ratification or by use.

Constraints already known:
- Segment by CAUSAL BRAID, not wall-clock window — a day of multi-agent log is
  braided threads; time-slices cut across them (semantic-time-is-a-DAG rule).
- Visibly machine-authored until ratified; supersedable by better segmentation.
- Quality thresholds OPEN(empirical — first segmenter runs); prior art to pull:
  process mining, commit untangling (research brief items 5, 3).

Slice 1 builds NO segmenter. The schema carries the vocabulary (authority,
status, evidence-range) so the first derived manifest needs zero migration.

### 4.5 Version-count wall [DECIDED, VERIFY numbers]

Research haul: timestamp-grain systems failed at ~21k versions. Commit-grain
mirroring respects the wall by construction; save/keystroke-grain native
editing does not → native commit grain is OPEN(native editing exists) with the
known direction: working-layer fine grain folds/compacts into committed
revisions; fine grain and configuration track are different stores.

---

## Part 5 — Storage [FROZEN decisions, REVISABLE mechanics]

### 5.1 Verified current reality (source, not guesses)

```text
SourceArtifactRow.source-raw-text          raw content INLINE in the PState row (:82-84)
                                           + content-byte-count, source-hash, source-format
RevisionRow.content-text                   full content INLINE per revision (:104-106)
ObjectContainerRow.current-content-text    DENORMALIZED third copy on the container (:99-102)
DerivedUnitRow.derived-content-text        unit-level copy for derived units (:108-111)
```

So today: content is text, stored inline, keyed by id (hash is a FIELD, not the
key — no content-addressed store, no dedup), with ~3 copies per current version
(artifact raw + revision content + container current). At slice-1 scale
(114 files, whole repo a few MB of text) this is trivially fine. [VERIFIED]

### 5.2 Decisions

- **Text is primary, structure is derived** [DECIDED]: code must round-trip
  exactly (rewrite-clj lossless principle; Unison's no-round-trip stance is
  unavailable — we don't own the format). Parsed structure is always an index
  over stored text, never a replacement.
- **Value hash = kernel sha-256 over raw bytes** [INHERITED]: never git's
  object id (sha1/sha256 varies by repo format); git blob id kept as source
  METADATA for cross-reference.
- **Mint one SourceArtifact + Revision per (path, content-version)** — i.e.
  per file per ingested commit WHERE THE FILE CHANGED; unchanged files mint
  nothing [INHERITED]. Same content-hash reappearing at the same path (revert)
  mints a new Revision pointing at... OPEN(decide cheap: reuse prior artifact
  by hash, or mint duplicate artifact; pure storage economics, no identity
  meaning — VERIFY what md adapter does on identical re-import).
- **Inline text storage continues for slice 1** [DECIDED]: the kernel's
  existing discipline, proven by two adapters. No CAS, no blob store now.
- **Binary content is out of scope slice 1** [DECIDED]: allow-list is
  text-only; `source-raw-text` is a text field. Binary assets =
  OPEN(non-text corpus arrives; would need byte storage or externalization).

### 5.3 OPEN register, storage

```text
dedup across paths/repos/copies        OPEN(second corpus lands — DG-plugin repo)
content externalization threshold      OPEN(artifact volume passes dogfood scale;
                                       criterion: PState row size or partition
                                       storage becomes operationally visible)
inline-copy redundancy (3x)            OPEN(same criterion; candidate fix is
                                       hash-keyed content table — a READ-MODEL
                                       restructuring, possible by replay, which
                                       is why it may stay open safely)
storage chunking (Prolly/packfile)     OPEN(scale; research brief item 6)
depot retention / replay interplay     OPEN(ops: replay-based rebuilds assume
                                       depot retention; confirm trim policy
                                       before relying on replay for migrations)
```

---

## Part 6 — Write Contract [FROZEN — the core of the spec]

Exhaustive emission list. The interpreter may emit NOTHING not listed here;
additions later are allowed (additive evolution), reinterpretation never.

Per harvested/watched commit, per changed allow-listed file, one
import-material request carrying:

```text
SourceArtifact        file bytes-as-text at this version; source-ref =
                      {repo-id, repo-relative-path, commit-sha};
                      source-hash = kernel sha-256; git blob id in metadata
ObjectContainer       stable "code:"-prefixed key from (repo-id, path);
                      created iff absent
NativeIdentityClaim   the path claim, kernel's existing row (:129)
Revision              content at this version; parent = container's previous
                      revision (first-parent honest subset)
SourceAnchor          whole-file span into the immutable artifact (drift-proof
                      by construction)
CompositionEdge       repo -> file, path-ordered
```

Per ingested commit, after ALL its file imports complete (gate = import
completions, 4.3):

```text
ManifestRow           per 4.3; authority :git-mirror; full pins; parents list
RefRow move           watched-branch ref -> new manifest
deletion              expressed by pin ABSENCE in the manifest (no tombstone
                      machinery slice 1; container tombstone semantics stay parked)
```

Idempotency [INHERITED, extended]:

```text
import-key        "imp:code:" + object-key + ":" + sha-256(commit + ":" + path + ":" + content-hash)
                  third member of imp:md: / imp:tr: family (prefix checks :286-289)
manifest-key      "imp:code-manifest:" + repo-id + ":" + commit-sha   [PROPOSED]
fingerprint       kernel import-material-fingerprint (:1517) — reruns replay
                  into accepted decisions; changed material under same key =
                  fingerprint conflict, as for the other sources
```

Evolution rules [DECIDED]:
- Rows are serialized defrecords: never reshape a deployed record; new fields
  via Rama's documented record-evolution path only (VERIFY exact mechanism
  against Rama docs before Phase 0), new row TYPES preferred.
- Never write a field whose meaning might change; fine to not-yet-write a
  field whose need can't yet be named.

---

## Part 7 — Read Models [REVISABLE — replayable into existence]

Designed for the named consumer; new ones may be added by replay later.

```text
file-by-path index        (repo-id, path) -> container
version timeline          container -> revision history
                          (note: $$revision-history-by-container and
                          $$source-versions-by-ref already exist as precedent)
code-at-time              (repo, path, t) -> revision current at nearest
                          manifest <= t on watched branch, via the ref history
manifest-at-time          (repo, t) -> manifest  (the group-level question that
                          per-file chains alone CANNOT answer — Pelgrin, VERIFY)
trail-resolution join     transcript tool-call (abs path + timestamp)
                          -> repo-root normalization -> container + revision
                          + EXACTNESS FLAG (uncommitted working-tree state is
                          approximate BY CONSTRUCTION; content-hash comparison
                          upgrades to exact when tool result carries content;
                          out-of-repo paths -> explicit "unresolved", never
                          a silent miss)                              [INHERITED]
```

The exactness flag is formally a one-bit bitemporal projection (valid-time =
when the agent saw the bytes vs transaction-time = what Softland ingested,
XTDB framing). Kept one bit; named so it can grow.

---

## Part 8 — OPEN Register (consolidated; each entry names its deciding criterion)

```text
native commit grain                  OPEN(native editing/committing UX exists)
form granularity + graduation        OPEN(form-level consumer exists; slice 2)
symbol identity                      OPEN(parser identity stable — contract's own condition)
rename/move continuity policy        OPEN(GumTree/RefactoringMiner accuracy research)
derived-commitment quality           OPEN(empirical segmenter runs)
merge/synthesis semantics            OPEN(literature-open frontier; standing stance:
                                     synthesis is a recorded act — a multi-parent
                                     manifest with provenance — never an algebra claim)
branch plurality                     OPEN(second watched ref needed; ref vocabulary ready)
multi-repo identity policy           OPEN(second corpus: DG-plugin repo)
file deletion tombstones             OPEN(consumer needs container-level deletion
                                     semantics beyond manifest absence)
storage items                        see 5.3
cross-reference edges                OPEN(RelationEdge is spec-only; calls/requires
                                     imports deferred knowingly)            [INHERITED]
durable trail->code edges            OPEN(projection-time join proves product-critical) [INHERITED]
content-level secret scanning        OPEN(structural rules are the slice-1 boundary;
                                     .json residual risk recorded)          [INHERITED]
ComputeDepot linkage                 OPEN(builds/runs referencing ingested versions —
                                     future consumer)                       [INHERITED]
```

---

## Acceptance Criteria (slice 1)

PRODUCT.md's 12 criteria stand [INHERITED], renumbered here 1–12 unchanged in
substance, plus three new from this spec:

```text
 1. Softland repo registers and harvests at HEAD.
 2. Watch on the same branch; new commits appear without re-harvest.
 3. Harvest and watch produce identical native container/edge types.
 4. A code file keeps ONE container identity across commits; versions are
    Revisions inside it.
 5. Reruns/backfills create no duplicates (imp:code: replay discipline).
 6. env.clj and anything gitignored cannot be ingested; deny-list failure
    refuses to run.
 7. .md files are not ingested by the code path.
 8. Every Revision traces to a SourceArtifact naming repo, path, commit;
    anchors resolve to exact bytes.
 9. Tool-call (path + timestamp) resolves through repo-root normalization to
    the right container + revision.
10. Resolution carries the exactness flag; approximation visible, never silent.
11. Out-of-repo paths resolve to explicit "unresolved".
12. Deletion visible to code-at-time as absence at that manifest.
13. Every ingested commit yields exactly one accepted manifest whose pins all
    resolve; the watched-branch ref points at it.                      [NEW]
14. A manifest is never accepted with a missing pinned revision (gate holds
    under interleaved/partial-failure runs).                           [NEW]
15. Manifest rendering distinguishes authority (:git-mirror today); a future
    :derived manifest is schema-expressible with zero migration.       [NEW]
```

---

## Relationship to Other Documents

- `PRODUCT.md` (this folder): the product contract this spec absorbs. Where
  they differ, THIS spec wins after ratification; PRODUCT.md cluster 3
  ("version-manifest row", one line) is superseded by Part 4.3.
- `versioning-paradigms-research.md`: haul 1. Load-bearing claims marked
  VERIFY until the brief's verify pass.
- `RESEARCH-BRIEF.md` (this folder): the targeted second pass feeding Parts
  2–5 and the rename/derived OPENs.
- A domain-general versioning-layer extraction (for the chat ingestor and
  native commits to reuse) is a LATER refactor of Part 4 into common-infra
  docs, only if/when a second producer arrives. Not now.
