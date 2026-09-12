# Trail-View Data Contract — WP1 (data layer only; no pixels)

References below to `BETS.md` are historical; that document is retired.
For vision orientation, use [carry-on](../../../docs/carry-on.md) as a reference summary
and [the vision log](../../../vision/LOG.md) as the primary source.

Status: v1 **BINDING** (Fable, 2026-07-04, Track-A WP1 contract session;
**countersigned by Sid 2026-07-04 in-session** — "Countersigned as yes").
Implements D-002 (first form = trail view) and D-005 (view-first) on the
read side; consumes D-004's relation kernel via its query topologies; rides
D-008 (read-only MVP, CLOSED the same moment). Handoff live per §12 and the
`.claude/skills/work-package/SKILL.md` process; Phase A1 (custody) is the
first implementation phase.

v1.1 amendments (2026-07-04, same day, from Phase-0 coherence findings —
ruled by Fable as contract author, before any plan/code tokens; executed as
a sweep, not a banner): **F-1** two-clock feed ruled ARRIVAL-ONLY window
selection with `:order` for in-window claimed ordering (§3 address, §6,
gate 4, trap 4, new §9.10); **F-2..F-6** text clarifications (§11 style-gate
scope, §3 `:rev` semantics, §5.2 fold dominance + `:supersedes`, §4
sibling-row rule, gate 7). Full finding trail:
`IMPLICIT_SPEC.md` §Phase-0 findings + its post-resolution addendum;
honesty note in decisions.md D-006 evaluation notes.

Authority note: prior trail-view/design docs are input, NOT authority (Sid
2026-07-04). Ground truth used here: `decisions.md`, `build/trail-view/
INPUTS.md` (items cited as "I-n"), the code (`relation_kernel.clj`,
`object_container.clj` + adapters), and the on-disk Rama references.

---

## 1. Purpose and scope

The trail-view data layer is the read surface every face of the trail view
rides: **context bundles** (the unit that leaves the chatbox, I-1), the
**recent-activity feed** (today's material, I-13), the **conversation trail**
(the threaded/DAG timeline's node expansion), **verdict rows** (what we came
to believe, I-8), **staleness fields** (I-2), and **view addresses** (a
rendered face is a resolvable pointer, I-14). Pure Rama: queries, shapes, and
two small write-side amendments to the relation kernel. No DOM, no WebGPU, no
watcher processes.

Consumers, in order (I: "Consumers, in order"):
1. **Agents** — View 3 text bundles + the queries directly (H2 handoff, H3
   machine benchmark). The load-bearing face: minimal-token legibility (I-3).
2. **Sid's threaded/DAG timeline face** — second face, same queries, first
   pixel surface (face-order ruling, decisions.md 2026-07-04).
3. **DG-plugin teammates** (H3 human half, later; adapter-shaped).

Non-goals in §9; each is an extension point, not a void.

## 2. Placement ruling

**A new read-only module: `trail-view-module`** (new file
`src/app/server/rama/trail_view.clj`, ns `app.server.rama.trail-view`),
declaring ONLY mirror depots/PStates/queries and query topologies — **zero
depots, zero stream/microbatch topologies, zero own PStates**.

Read-only MVP (D-008/I-14) therefore holds **by construction**: a module with
no depots and no ETL topologies physically cannot write truth. This converts
"zero kernel writes from the view" from a policy promise into a structural,
gate-testable property (gate 14).

Why not the alternatives:
- **Client-side assembler only (no module):** the conversation projection,
  outline, and registry scans have no existing query topologies —
  a pure-client assembler would need direct `foreign-select` on kernel
  PStates from every consumer, scattering PState-shape coupling across
  Electric, agents, and the benchmark harness. The module concentrates that
  coupling in one seam.
- **New queries added inside object-container / transcript-ops modules:**
  edits to 2,648 hardened lines of import path for read-only features; view
  read shapes would then evolve by redeploying the ingest kernel. Blast
  radius for no benefit — mirrors give the same data.
- **Inside relation-kernel-module:** the view is cross-kernel by nature
  (material + relations); the relation kernel stays the edge-truth module.

**Exception, ruled here:** three small amendments land IN
`relation_kernel.clj` (§5) because the facts they record exist only there:
custody recording (only the topology sees the envelope), stance kinds (the
registry is the kernel's), and the relation-activity projection (only the
accepted branch knows a transition was accepted — a mirror consumer of the
request depot would have to re-derive accept/reject policy: trap 3).

Reversal cost: the trail-view module holds **no state** — being wrong about
its existence or internal shape costs consumers a wrapper-function rename,
never a data migration. The activity PState (§5.3) is the only new state; it
is a rebuildable projection (replay/re-assert regenerates it), and its read
goes through a query topology (R3), so relocating it later changes no
consumer. The seam: **faces and agents consume ONLY the client wrappers in
§7** — never PStates, never raw foreign-select.

Authoritative-backend ruling: the view reads the **object-container kernel**
path (the hardened Jun 7–8 ingest target) plus the relation kernel. The
standalone `transcript-module` (dogfood/transcript.clj) and
`TranscriptIngestModule` (dogfood/transcript_ingest.clj) materialize parallel
`tc:*`-shaped state and are **not** sources for the view; material living
only there re-enters through convergent re-import (deterministic ids make
this cheap by design). The `space-kernel` plumbing graph stays out (D-004).

## 3. Address grammar (I-14: every face renders its own address)

An address is a **one-line literal EDN list**: `(trail/<query> <params-map>)`.

```clojure
(trail/context-bundle    {:targets ["oc:doc:9f.."] :layers :all :caps {}})
(trail/recent-activity   {:window {:from-ms 1782..0 :to-ms 1782..9} :order :arrival})
(trail/conversation-trail {:conversation "oc:chat-conversation:chat:ab.." :cursor nil :limit 200})
(trail/relation-detail   {:relation-id "rel:3c.."})
;; spec-driven form (view-specs-as-data, I-3):
(trail/via {:spec "oc:doc:<object-key>" :rev :latest :overrides {:caps {:children 10}}})
```

Laws:
1. **Self-description:** every query result carries `:bundle/address` (resp.
   `:feed/address`, `:trail/address`) — the exact address that produced it.
   Every text projection renders the address in its header. A screenshot is
   then a resolvable pointer, not just pixels.
2. **Round-trip:** parsing a rendered address and re-invoking the wrapper
   returns a result of the same shape over current truth (gate 2). Addresses
   resolve to NOW; `:bundle/rendered-at-ms` is the drift detector. There is
   NO as-of/time-travel resolution (§9.1).
3. **Readability:** addresses are plain EDN with the documented param
   vocabulary — an agent can compose one without a lookup table (trap 8).
4. `trail/relation-detail` is an address over the relation kernel's existing
   R2; the wrapper calls R2 directly. Addresses cover every rendered face,
   whichever module serves it.

**View-specs are documents** (I-3: versioned, assertable, in the land): a
view-spec is an ordinary ingested doc whose content is one EDN map
`{:spec/name ".." :spec/query trail/<name> :spec/params {..} :spec/face-hints {..}}`.
Resolution (`trail/via`): read the spec doc — `:latest` via
`read-latest-source-by-ref`; a pinned `:rev` names a SOURCE VERSION (the
spec doc's source-version-key, resolved via `read-source-by-ref-version` on
the doc's source-ref, joined from the container's `source-id`; `:rev`
values are source-version keys, NOT container revision-ids — OC exposes no
arbitrary-revision query and stays untouched; F-3 clarification, v1.1) —
then merge `:overrides` (shallow, params-level), invoke. Agents manipulate views by
editing spec docs through the existing CLI write path — the view itself still
writes nothing. Spec revisions ride existing revision machinery; a pinned
address keeps resolving against its pinned revision (gate 12).

## 4. The context bundle (I-1: everything, layered — never a choice)

`context-bundle [targets opts]` → one roundtrip, batch-first (whole visible
DAG region at once, matching R1's batch-first design).

Accepted target ids (v0): `oc:doc:*`, `oc:chat-conversation:*`, `oc:block:*`,
`oc:chat-message:*`, `oc:tool-call:*`, `oc:tool-result:*`, `du:*`, `src:*`,
`rel:*` (delegates to R2 projection), git-commit shas (post-WP2; renders as
unresolved until the spine lands — dangling is a view state, D-004 trap 3).
Unknown ids are returned under `:bundle/omissions` with reason
`:target/unrecognized` — never silently dropped.

```clojure
{:bundle/address        '(trail/context-bundle {..})
 :bundle/rendered-at-ms 1782...                       ; wrapper-stamped, client side
 :bundle/targets        {"<target-id>" <target-bundle>}
 :bundle/omissions      [<omission> ...]}

<target-bundle> =
{:id ".."  :kind :doc|:conversation|:block|:message|:tool-call|:tool-result|:source|:relation|:unresolved
 :address  '(trail/context-bundle {:targets [".."]})
 ;; L0 identity
 :identity {:created-by ".." :current-revision-id ".."|nil :content-hash ".."|nil
            :container-kind ".."|nil :source-id ".."|nil}
 ;; L1 raw — honest about stored vs addressed (D-003: address, don't copy)
 :material {:content-text ".."|nil                    ; current graduated/derived text, capped
            :raw {:stored? true|false                 ; md: true; transcripts: false
                  :source-ref ".." :source-id ".."
                  :offset-unit :chars|:bytes          ; md anchors are CHAR offsets; transcript anchors are BYTE offsets (trap 12)
                  :byte-count N|nil
                  :text ".."|nil                      ; only when :stored? AND opts asked
                  :anchor {:file-path ".." :start N :end N}|nil}
            :anchors [{:anchor-id "sa:.." :start N :end N} ...]}
 ;; L2 native structure
 :structure {:parent   {:id ".." :edge-id "ce:.."}|nil
             :children [{:id ".." :order-key ".." :kind ".." :preview ".."} ...]  ; capped
             :order-semantics :block-path|:byte-offset}
 ;; L3 relations — from relation kernel R1, grouped by endpoint id
 :relations {:this      {<kind> [<edge> ...]}
             :in-family {"<other-id>" {<kind> [<edge> ...]}}}
 ;; L4 verdicts — derived fold over stance-kind rows in :relations (§5.2)
 :verdicts  {:current {"<asserter-actor-id>" [<verdict> ...]}
             :derived-from :relations}                 ; fold is projection, rows are truth
 ;; L5 time + staleness (I-2)
 :times     {:created      {:claimed-ms N|nil :arrival-ms N|nil}
             :last-changed {:claimed-ms N|nil :arrival-ms N|nil}
             :last-attested-ms N|nil                   ; max stance/relation re-assertion touching this target
             :last-walked-ms  nil}                     ; ALWAYS nil in WP1 — see §9.2
 :omissions [{:layer :relations :dropped N :cap N :cursor ".."} ...]}

<edge> = projection of RelationEdgeRow:
{:relation-id ".." :kind :based-on :from {:kind :doc :id ".."} :to {:kind .. :id ..}
 :status :asserted|:retracted
 :asserted-by ".." :asserter-type :human|:llm|:import|:system
 :written-by ".."|nil                                  ; envelope actor when ≠ asserter (§5.1)
 :evidence {:source-id ".."|nil :anchor-id ".."|nil}
 :note ".."|nil :first-asserted-at-ms N :last-changed-at-ms N}
```

Key-family grouping (`:this` vs `:in-family`): a doc and its blocks — and a
conversation and its messages — share one partition key
(`extract-object-key`), so ONE R1 read per target returns the whole family's
relations. The bundle attributes each row to its actual endpoint id:
rows on the requested id land in `:this`; rows on siblings (e.g. a verdict on
block `du:<key>:000003` when the doc was requested) land in `:in-family`
keyed by that id. This is how "doc §X refuted-by <actor>" (I-8) rolls up into
the doc's bundle in one seek — and how a block-level bundle stays scoped.
(Naive alternatives both fail: trap 6.) A family row touching two distinct
siblings — neither endpoint the requested id — files ONCE under
`:in-family`, keyed by its `from` endpoint id (F-6, v1.1; R1's per-target
dedup by relation-id already prevents doubles).

Omissions law (exactness rule, D-003): every cap, truncation, page boundary,
and unresolvable id appears in an `:omissions` entry with counts and, where
resumable, a cursor. Mechanically checked by gate 3: returned + omitted ==
fixture totals. A bundle never silently narrows (trap 7).

Layer opt-in: `:layers` selects which of L0–L5 are assembled (`:all`
default); `:caps` bounds children/relations/text sizes per layer. Defaults in
the implementation are contract-visible constants.

## 5. Relation-kernel amendments (the only write-side changes)

Bounded edits to `relation_kernel.clj` + its test ns. Everything else in the
kernel is untouched; the existing suite (2 tests, 165 assertions) must stay
green (gate 16). Depot envelopes remain plain maps with top-level namespaced
keys; typed records ride only inside payload fields and PStates (the
partitioner-read key stays on the plain map envelope — cycle-1 F1 rule).
Events stay engine-neutral maps/records with no Rama-specific fields (I-11).

### 5.1 Custody recording (I-7, I-14 — hard-gated BEFORE phase-1 daily use)

Verified 2026-07-04: the kernel persists only the payload's
`asserter-actor-id`; the envelope `:actor` is checked for retraction rights
and dropped. Phase-1's write pattern (Sid instructs in CLI, agent appends:
payload asserter = `sid`, envelope actor = the agent) would make every write
look like Sid's own hand — custody vanishes (trap 10; decisions.md
object-kernel-revision ruling, discipline 2).

Amendment:
- `RelationDecisionRow`, `RelationEventRow`, and `RelationEdgeRow` each gain
  `envelope-actor-id` + `envelope-actor-type`, copied verbatim from the
  envelope `:actor` at decision time. The edge row carries last-transition
  custody; the full custody trail lives in events/decisions.
- **Identity is NOT amended** (scope: `relation-id` stays
  sha1(kind, from, to, asserter) — asserter-scoped, custody-free). Two agents
  writing Sid's same assertion converge on ONE relation with custody recorded
  per transition. Putting the writer into identity would fork one fact into
  per-agent duplicate rows (trap 11).
- Bundle/edge projections expose `:written-by` only when it differs from
  `:asserted-by` (token economy; the map still never lies — gate 10).
- Migration: the relation corpus predating this field is dev-stage; a clean
  relaunch + convergent re-import regenerates rows with custody. **Sequencing
  law: this amendment lands FIRST (Phase A1), before real assertions
  accumulate.**

### 5.2 Stance kinds — verdict rows ARE relations (I-8, I-10)

Registry addition (the designed-for one-line change, relation-kernel
CONTRACT §3):

```clojure
;; adds to relation-kinds:
:confirms :refutes :supersedes
```

- **Shape: binary, directed** — `from` = the judgment carrier (the recheck
  section, the superseding doc, the conversation/message where the judgment
  was uttered), `to` = the judged thing. `evidence-anchor-id` points at the
  exact span expressing the judgment. Degenerate case (pure stance, no
  artifact): `from` = the CLI conversation/message container where it was
  asserted — the session is always ingested, so the span exists. No unary
  stance form (one kind = one reading; trap 5).
- `:supersedes`: `from` = the replacement, `to` = the displaced. Distinct
  from `:built-over`/`:new-direction` (construction lineage): supersession is
  belief displacement.
- **The verdict layer is a fold, not storage** (`current-verdicts`, a pure
  named fn): per judged target — a stance row's `to` endpoint; all three
  kinds fold uniformly, `:supersedes` included (F-4, v1.1) — per asserter,
  the latest **asserted** stance-kind row; **latest = max
  `status-changed-at-ms`** (bumped on every re-assert), tiebreak
  `relation-id` (F-5, v1.1). Retracted stances drop out of `:current`; history
  stays reachable via R2 addresses. Disagreeing asserters are BOTH current,
  badged, never merged (I-10; D-004 asserter-in-identity gives this free).
- Attestation = re-assertion (existing kernel behavior bumps
  `status-changed-at-ms` on re-assert): `last-attested-ms` in bundle L5 =
  max over stance/relation transitions touching the target. No new machinery.
- Everything verdicts inherit for free by being relations: deterministic
  identity (re-import converges), idempotency journal, retraction rights,
  status history, evidence anchoring, descriptor-gated kind-filtered reads
  (traps 1–2).

Registry scope note: the registry grows to 10 kinds; the per-target
descriptor PState bound remains ≤ 2 × registry rows (now 20) — still
non-subindexed, still bounded.

### 5.3 Relation-activity projection + R3 (feeds §6)

New PState in the existing microbatch topology, written ONLY in the accepted
branch (rejected decisions and journal-replayed duplicates write nothing —
gate 9):

```clojure
(defrecord RelationActivityRow
  [order-key bucket relation-id relation-kind from-kind from-id to-kind to-id
   asserter-actor-id asserter-type envelope-actor-id relation-status
   previous-status event-id claimed-at-ms arrival-at-ms])

$$relation-activity-by-bucket
  {String (map-schema String RelationActivityRow {:subindex? true})}
;; bucket    = fixed-width UTC day index (quot arrival-at-ms 86400000), zero-padded
;; order-key = (oc/fixed-width-order-key arrival-at-ms event-id)
;;             — unique per event (event-id embeds relation-id + transition
;;             order-key); scope: one row per accepted transition, per bucket.
```

- **Two clocks (decisions.md two-clock discipline):** `claimed-at-ms` =
  payload `asserted-at-ms` (client-supplied semantic time; the
  importer-timestamp discipline, I-6). `arrival-at-ms` = envelope
  `:request/sent-at-ms` — a NEW envelope key stamped by the client builders
  (`assert-request`/`retract-request` gain it); fallback when absent:
  `asserted-at-ms`. **The topology never reads the wall clock** — a
  crash-replayed microbatch must re-derive byte-identical rows, and a
  wall-clock stamp would double-bucket entries under replay (trap 4b; the
  object-container kernel has exactly this divergence at
  `object_container.clj:428,448,467` — do not copy it).
- Write = fourth partition hop (`|hash bucket`) inside the same microbatch —
  exactly-once per attempt, same rationale as the existing triple write
  (relation-kernel CONTRACT trap 1).
- Bucket hotspot bound: one task hosts one UTC day's writes; phase-1 volume
  is tens/day. Promotion criterion, pre-named: shard bucket keys
  (`day:hash(relation-id) mod k`) when a day's write volume makes the single
  task visible in latency.
- **R3 query topology `relation-activity`** `[bucket-lo bucket-hi]` →
  ordered rows (subindexed range reads per bucket, `{:allow-yield? true}`).
  The seam stays absolute: nothing outside the kernel reads its PStates —
  the trail-view module consumes R3 as a mirror query.

## 6. The recent-activity feed (I-13: TODAY's material)

`recent-activity [window opts]` → the two-clock feed.

```clojure
{:feed/address '(trail/recent-activity {..})
 :feed/rendered-at-ms N
 :feed/entries [<entry> ...]                          ; ordered by chosen clock, desc
 :feed/omissions [..]}

<entry> =
{:entry/kind    :relation-transition | :transcript-file-updated | :source-ingested
 :entry/target  {:id ".." :kind ..}
 :entry/address '(trail/context-bundle {:targets [".."]})
 :time/claimed-ms N|nil                               ; nil is honest (md ingest has no claimed time yet)
 :time/arrival-ms N
 :entry/actor   {:asserted-by ".."|nil :written-by ".."|nil}
 :entry/detail  {..}}                                 ; kind/status for relations; file-path for files; source-ref for docs
```

- **Window selection is ALWAYS arrival-time** (F-1 ruling, v1.1): the feed
  answers "what did the land learn in this window" — Sid's daily-relief
  loop. `:order :arrival|:claimed` (default `:arrival`) orders entries
  WITHIN the selected window; entries ALWAYS carry both stamps. A July
  re-ingest of April notes appears in today's arrival window stamped
  claimed=April — orderable and filterable by that stamp — and never
  invents an April feed entry (trap 4; gate 4). Cross-land CLAIMED-window
  selection is refused (§9.10): the arrival-keyed buckets cannot serve it
  in O(window-activity), and no consumer demands it — semantic claimed-time
  trails are per-target reads (status logs, bundles). Promotion path,
  pre-named: a twin claimed-keyed bucket index (same accepted-branch write
  idiom, one more hop) the day a rendered face demands claimed windows.
- Sources, per branch:
  1. `:relation-transition` — R3 over the buckets covering the window.
  2. `:transcript-file-updated` — scan of the ops-module's
     `$$transcript-file-offsets` (one row per watched file), filtered by
     `updated-at-ms`; conversation address resolved via the tail row of
     `$$transcript-source-lines-by-file` for that file (`source-id` →
     object-key → `oc:chat-conversation:` id).
  3. `:source-ingested` — scan of `$$source-latest-by-ref` /
     `$$source-ingest-completions-by-ref` filtered by the window.
- **Named gaps (the map must not lie; D-005 — gaps order the next slices):**
  direct editor revisions (ObjectEditPayload) do not surface until edit
  activity is projected — acceptable while the CLI is the write surface
  (D-008); session metadata (model, session_id) is thin on the OC path —
  the feed renders what exists and the gap names the importer enrichment
  slice. Both are recorded as omission-kind `:feed/uncovered` in the feed's
  standing `:feed/omissions` (a STATIC declaration, so an agent reading the
  feed knows what the feed cannot see — gate 3 checks it is present).
- Read plan (performance promise): cost is O(#watched-files + #doc-refs +
  window-activity), all small registries at phase-1 corpus scale; single
  roundtrip. Promotion criterion, pre-named: materialize a material-activity
  index (in the ops module or trail-view gains a depot — a D-001 decision at
  that time) when registry scans exceed ~50ms at real corpus size. Until
  then: no new material-side state (D-001: no imagined-scale machinery).

## 7. Query surface and read plans (the seam)

Trail-view module query topologies (+ client wrappers, the ONLY product
surface — style gate: no `foreign-select` on any kernel PState outside this
module and the relation kernel's own V1 test readers):

| Query | Args | Internally reads | Plan (per target) |
|---|---|---|---|
| `context-bundle` | targets, opts | mirrors: `$$containers-by-id`, `$$revisions-by-id`, `$$source-artifacts-by-id`, `$$source-anchors-by-target`, `$$composition-{children-by-parent,parent-by-child}`, `$$outline-by-document`, `$$transcript-conversation-projection`, `$$derived-units-by-id`, `$$unit-graduations-by-id`; mirror-queries: `read-common-material-for-source`, relation R1 | ≤1 container/source point-read + 1 structure page + 1 R1 invocation (descriptor-gated, 1-seek dominant path) + local folds |
| `recent-activity` | window, opts | mirrors: `$$transcript-file-offsets`, `$$transcript-runs`, `$$source-latest-by-ref`, `$$source-ingest-completions-by-ref`, `$$transcript-source-lines-by-file` (tail reads); mirror-query: R3 | §6 read plan |
| `conversation-trail` | conversation, cursor, limit | mirror: `$$transcript-conversation-projection` (+ `$$transcript-last-message-by-conversation`) | 1 subindexed page per call, byte-offset order |

Client wrappers (`app.server.rama.trail-view`): `read-context-bundle`,
`read-recent-activity`, `read-conversation-trail`, `read-relation-detail`
(delegates to kernel R2), `->address` / `resolve-address` (EDN round-trip),
`current-verdicts` (pure fold, exported for tests and faces),
`render-bundle-text` (§8).

**Platform verification duty (implementer Phase 1, before code):** confirm
against the on-disk references (a) mirror PStates/queries usable from query
topologies (`docs/reference/rama/18-module-dependencies.md`,
`13-query-topologies.md:203-207` — colocated invocation is documented; the
leading-partitioner restriction for mirror state partitioners at
`13:196` is noted), and (b) module-dependency launch order in the test
harness (source modules launch first). **Named fallback if query→mirror-query
is unsupported:** `context-bundle` returns the material layers; the client
wrapper composes R1/R3 results into the bundle (2 roundtrips instead of 1).
The wrapper signature and result shape DO NOT change — consumers are
insulated either way. Wall-clock latency is not a gate; shape and honesty
are.

## 8. View-3 text projection (the agent face IS a contract artifact)

`render-bundle-text [bundle]` — deterministic, versioned (`;; trail-text v0`
header line), sections in fixed order with fixed markers:

```
;; trail-text v0 @ <rendered-at ISO-8601>
;; address: (trail/context-bundle {..})
== <target-id> (<kind> "<display-name>")
   created <claimed|arrival> by <actor> | last-changed <..> | attested <..|never> | walked unknown
-- material: <n> children, <bytes> raw (stored|addressed <file>#<start>-<end>)
-- relations:
   -> based-on <id>            by sid @<t> [ev sa:..]
   <- produced <id>            by import:transcript @<t>
-- verdicts:
   sid: refutes <id> via <from-id> @<t> [ev sa:..] (current)
   llm:opus-4-8/..: confirms <id> @<t> (current — disagrees)
-- omissions: <none | list>
```

Laws: address in header (I-14); one line per relation/verdict; asserter badge
always present, `written-by` badge only when it differs; two-clock stamps
where they diverge; `walked unknown` rendered explicitly while the field is
nil (absence of walk data must not read as freshness — I-2, exactness).
Style gate + budget (gate 13): the gate fixture (§11) renders ≤ 4,000
characters with all markers present. **Where this gate stops:** it checks
format, presence, and budget mechanically; whether the text actually orients
a cold agent is H3's benchmark, not an IPC test (I-4 anticipated, not
claimed).

## 9. What this contract refuses (extension points, not voids)

1. **No as-of/time-travel reads.** Addresses resolve to NOW;
   `rendered-at-ms` detects drift. The log-primary substrate keeps replayed
   projections possible later; nothing here forecloses them (trap 9).
2. **No walk capture.** `last-walked-ms` exists in the shape, is ALWAYS nil
   in WP1, and renders as unknown. Honest walk events are a write surface —
   gated on the read→write milestone (D-008's named second milestone), not
   smuggled in through the view (trap 11b).
3. **No question rows** — but nothing precludes them (I-5): hole-shaped
   patterns would enter as new registry kinds and/or a reserved target-kind,
   and the kernel's unary/`:none` machinery already proves one-sided rows
   work. Dedicated design item stays queued.
4. **No confidence/credential algebra** (decisions.md open question; Regime-2
   adjacent per today's object-kernel-revision ruling).
5. **No cross-land text search**; bundles and feeds are key/window-addressed.
6. **No auto-summarization inside bundles.** Every `:preview` is a mechanical
   truncation (length-capped prefix), never a paraphrase. LLM-derived
   summaries are asserted material by an actor, not assembler magic (the map
   must not lie about authorship).
7. **No DG-protocol kinds** (H4 waits), **no canvas geometry**, **no
   permission model** (H3 benchmark scoping is harness-level read exposure
   for now; kernel-level permissions are their own future contract).
8. **No watcher processes** (they are OS-side triggers over existing
   ingestors — required by the MVP loop, out of the data layer; see §12
   "after green").
9. **No edits to the object-container module** — the view reads it via
   mirrors only.
10. **No claimed-window cross-land selection** (F-1 ruling, v1.1): the
    feed's window is arrival-time only; `:order` covers in-window claimed
    ordering. Extension point: a claimed-keyed twin bucket index (same
    accepted-branch write idiom, one more hop) when a rendered face demands
    claimed windows.

## 10. Traps ledger (naive choice → concrete failure → ruling)

| # | Naive choice | Concrete failure | Ruling |
|---|---|---|---|
| 1 | Verdicts as a new bespoke row type/module | Re-implements identity, idempotency, retraction, history, evidence anchoring minus the hardening; second write path drifts from the first; disagreement preservation must be rebuilt | §5.2: stance kinds in the existing registry; verdicts inherit the kernel |
| 2 | Stance as a mutable field (or stuffed in `:note`) | No kind-filtered reads (descriptor gating can't see fields); "all refuted things" becomes read-everything-and-parse; stance history illegible | §5.2: stance-as-kind; current-verdict is a named pure fold |
| 3 | Activity index in trail-view via mirror depot on `*relation-request-depot` | Requests ≠ facts: rejected and journal-replayed requests pollute the feed, or accept/reject policy is re-derived in a second module and drifts | §5.3: written in the kernel's accepted branch; read via R3 |
| 4 | Feed ordered by one clock | A July re-import of April notes floods "today" (claimed-only) or today's learnings vanish into April (arrival-only) | §6 (v1.1): entries carry BOTH stamps; arrival-only window selection + explicit `:order` param |
| 4b | Stamp arrival time with `now-ms` inside the topology | Microbatch crash-replay re-stamps differently → same event lands in two buckets; exactly-once broken at the semantic level (OC has this exact divergence — `decided-at-ms`) | §5.3: client-stamped `:request/sent-at-ms`; topology never reads the clock |
| 5 | Unary stance marks (`X :refuted` with `:none`) alongside binary | One kind, two readings (mark vs edge); grouping and direction filters mislead | §5.2: stance kinds are binary-only; the judgment carrier is always addressable (the CLI session at minimum) |
| 6 | R1 family rows: filter to requested id only — or don't attribute at all | Filter-only: section verdicts invisible from the doc bundle (I-8 broken). No attribution: sibling relations blend into the target's, provenance of "whose row is this" lost | §4: `:this` vs `:in-family` grouping by endpoint id |
| 7 | Silent caps (top-N children/relations) | Bundle reads as "everything" when it isn't — the exactness rule broken at the data layer; agent benchmarks silently under-informed | §4: required `:omissions` with counts/cursors; gate 3 reconciles totals mechanically |
| 8 | Opaque addresses (hash/uuid handles) | Screenshot pointer needs a lookup table; agents can't compose queries; token cost of address resolution | §3: literal EDN addresses, round-trip law, gate 2 |
| 9 | Promise as-of bundles now | PStates are current-state; honest time-travel needs replay machinery nobody demanded (D-001) | §9.1: NOW-resolution + rendered-at drift stamp; extension point |
| 10 | Keep trusting payload asserter alone through phase 1 | Agent-written assertions on Sid's instruction are indistinguishable from Sid's own hand at the truth layer; misfire audit impossible | §5.1: envelope actor persisted on decision/event/edge rows BEFORE daily use |
| 11 | Put the envelope actor INTO relation identity | One fact (Sid's assertion) forks into per-writing-agent duplicate rows; view shows phantom disagreement of Sid with himself | §5.1: custody is metadata; identity stays asserter-scoped |
| 11b | Capture walks from the view "since we're rendering anyway" | The read path becomes a writer — read-only-by-construction lost, D-008 pre-empted by the back door | §9.2: nullable field now, capture at the read→write milestone |
| 12 | Treat all anchor offsets as bytes | Markdown anchors are CHAR offsets (`markdown_adapter.clj` uses `count`), transcript anchors are BYTE offsets — evidence spans misresolve on any multi-byte character | §4: `:offset-unit` on every raw/anchor block; gate 5 resolves one of each |
| 13 | Read material from the standalone transcript modules (`tc:*`) | Two parallel truth shapes for the same conversations; the view's picture depends on which module ingested a file; joins silently miss | §2: OC path is authoritative; TR/TI out of scope; re-import converges |

## 11. Acceptance gates (IPC tests; gates green = done)

Shared fixture: 1 markdown doc (≥3 blocks) + 1 transcript conversation
(≥4 messages, ≥1 tool call) ingested through the OC path; relations:
`based-on` (cross-doc), `produced` (conversation→doc), `dead-end` (unary);
verdicts: `refutes` + `confirms` on a block, from two asserters; one
back-dated relation (claimed = 60 days ago, sent-at = now). Reuse the
relation-kernel harness barrier (`wait-for-microbatch-processed-count`-style
deterministic waits; no polling as proof — implementation-quirks discipline).

1. **Bundle completeness:** `context-bundle` on the doc + conversation
   returns all six layers; every relation row equals the corresponding R1
   row (seam consistency); `:in-family` carries the block verdicts.
2. **Address round-trip:** each result's address parses and re-invokes to an
   equal result (modulo `rendered-at-ms`).
3. **Omissions honesty:** with `:caps` forced below fixture sizes,
   returned + omitted counts == fixture totals per layer; the feed exposes
   its standing `:feed/uncovered` declaration.
4. **Two clocks (v1.1):** the back-dated relation appears in today's
   arrival window carrying a claimed-ms 60 days old; `:order :claimed`
   places it before today-claimed entries in that window; an arrival window
   covering only 60-days-ago does NOT return it; a claimed-window selection
   request is refused by the wrapper (§9.10); both stamps present on every
   entry.
5. **Evidence anchors resolve:** the `refutes` verdict's anchor joins to a
   `SourceAnchorRow`; one md anchor (char-unit) and one transcript anchor
   (byte-unit) both carry the correct `:offset-unit` and resolve to the
   expected span text in the fixture's source.
6. **Disagreement preserved:** both asserters' stances are `:current`,
   badged, unmerged (I-10).
7. **Supersession fold:** same asserter retracts `confirms`, asserts
   `refutes` → `:current` shows only `refutes`; history reachable via the
   R2 address; the retracted row visible with `include-retracted?`; a
   fixture `supersedes` row folds into `:current` keyed on its displaced
   (`to`) endpoint (v1.1).
8. **Attestation staleness:** re-asserting an existing relation bumps the
   target's `last-attested-ms`; a never-attested target shows nil; every
   bundle's `last-walked-ms` is nil and the text projection prints
   `walked unknown`.
9. **Activity exactness:** each accepted transition (assert, re-assert,
   retract) adds exactly one R3 row; journal-replayed duplicates and
   rejected requests add zero; a re-launched module replaying the batch
   yields byte-identical activity rows (no wall-clock in the row).
10. **Custody:** envelope actor ≠ payload asserter → both durably on
    decision + event + edge rows; bundle edge shows `:written-by`; when they
    match, `:written-by` is omitted from the projection.
11. **Conversation trail:** ordered by byte-offset; page cursor works;
    convergent re-import of the same transcript leaves order and count
    unchanged.
12. **View-spec resolution:** `trail/via` with `:latest` reflects a spec-doc
    revision; a pinned `:rev` keeps resolving to the old params.
13. **Text projection (style gate):** fixture bundle renders with all §8
    markers, the address header, asserter badges, and ≤ 4,000 chars.
    Stops at format/budget; semantic orientation is H3's benchmark.
14. **Read-only by construction:** `trail-view-module` declares no depots
    and no ETL topologies (assert via module inspection or the harness's
    foreign-depot lookup failing); the module's source contains no
    `foreign-append!`/`local-transform>`.
15. **Registry regression:** unregistered kinds still rejected; the three
    new kinds accepted; descriptor bound holds (≤ 2 × registry per target).
16. **Kernel regression:** the pre-existing relation-kernel suite passes
    unmodified (165 assertions), plus its gates re-run green with the
    amended records.

Style gates (with stop boundaries): typed defrecords in PStates — the depot
envelope stays a plain map, records may ride inside payload fields (stops at
the envelope key layer); partition/order helpers imported from
object-container, never re-derived locally (stops at pure formatting
helpers); `{:allow-yield? true}` on every unbounded range read; NUL and
control bytes only as `\u0000`-style escapes in source (both prior traps
fired here — and a third time writing THIS contract, caught at Phase-0
close); PState-access scope (F-2 clarification, v1.1): relation-kernel
PStates are read by NOTHING outside the kernel's own V1 test readers —
R1/R2/R3 are the only relation reads anywhere; object-container/ops PStates
are read only via the trail-view module's declared mirrors — product/client
code never `foreign-select`s them directly.

## 12. Work-package handoff

- **Implementer:** Opus 4.8 or Codex under the `/rama` skill's phased
  process; this contract is Phase-0 input. **Phase A** (relation kernel):
  A1 custody (§5.1, FIRST), A2 stance kinds (§5.2), A3 activity + R3 (§5.3)
  — gates 9, 10, 15, 16. **Phase B** (trail-view module + wrappers + text
  projection + fixtures) — remaining gates. One phase per fresh session.
- **File allowlist:** new: `src/app/server/rama/trail_view.clj`,
  `test/.../trail_view_test.clj` (+ fixture resources). Amended:
  `src/app/server/rama/relation_kernel.clj` (ONLY: registry set, envelope
  builders gaining `:request/sent-at-ms`, the three record shapes, the
  accepted-branch activity write, R3) and the relation-kernel test ns
  (additive, plus the mechanical updates the §5.1 record-arity change forces
  on existing positional constructors). NOTHING else — the object-container
  module is untouched.
- **Verification duties before code (Phase 1):** the §7 mirror-invocation
  check; microbatch replay determinism of the activity write; harness
  launch order for module dependencies.
- **Stop clause:** contract unbuildable as specified, wrong under platform
  semantics, or two binding docs genuinely conflict → classify
  (implementer-fixable vs policy fork) → escalate policy forks to
  decisions.md Open Questions as PROPOSED. Never improvise on binding docs.
- **Reviewer gate (Fable):** gates green independently re-run; traps 3, 4b,
  10, 12 spot-checked in the diff; falsification pass per CLAUDE.md.
- **After green:** the threaded/DAG timeline face and View-3 rendering
  consume §7 wrappers (Track B2's view-MVP package); watcher triggers get
  specced as the tiny OS-side loop they are; the H3 benchmark harness reads
  the same wrappers; WP2 (D-003 spine) starts only on Sid's explicit go and
  emits `produced`/`based-on` with evidence anchors that land in these same
  bundles.

## 13. Input manifest (for the D-006 counterfactual probe)

Given to any model reproducing this contract: `decisions.md` (D-001..D-007 +
the 2026-07-04 open-question rulings incl. object-kernel-revision + face
order); `build/trail-view/INPUTS.md`; `build/relation-kernel/CONTRACT.md`;
`src/app/server/rama/relation_kernel.clj` (entire);
`object_container.clj` lines 23–360, 1685–1770, 2436–2648;
`object_container/transcript_identity.clj`;
`object_container/transcript_adapter.clj` lines 140–260, 280–510;
`object_container/markdown_adapter.clj` lines 78–170, 188–330, 440–460;
`object_container/runtime.clj` lines 74–235;
`docs/reference/rama/18-module-dependencies.md`,
`13-query-topologies.md`; `vision/LOG.md` 2026-07-04 entries; `BETS.md`
(H1–H3); the `/rama` and `/work-package` skills. Probe question: "design the
trail-view data-layer contract for this substrate."

## 14. Judgment calls flagged for countersign (one line each)

1. Verdicts as relation stance kinds (not a separate store) — §5.2, traps 1–2.
2. Activity projection lives in the relation kernel + R3 (not the view
   module) — §5.3, trap 3.
3. OC ingest path is the view's sole material source; standalone transcript
   modules out of scope — §2, trap 13.
4. Custody recorded, NOT in identity — §5.1, traps 10–11.
5. Walk capture deferred to read→write; field nullable now — §9.2, trap 11b.
6. View-specs are ordinary versioned docs, resolved by address — §3.
7. Read-only enforced structurally (no-depot module), gate 14 — §2.
8. Relation-kernel amendments sequenced custody-first, before daily use — §5.1.
9. D-008 (read-only MVP) proposed as its own decision via Roam card; this
   contract assumes it.
