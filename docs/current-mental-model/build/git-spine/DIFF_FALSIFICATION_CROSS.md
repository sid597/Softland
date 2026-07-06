# DIFF_FALSIFICATION_CROSS — cross-package drift review (t4-room × t4-spine)

**Stamp:** Trunk-4 · 2026-07-06 · cross-package falsification reviewer (read-only;
no repo file modified). Uncommitted working tree vs HEAD 9552aa4. Method: static
read of the live seam + a **pure-JVM probe** (`scratchpad/cross-falsify/probe.clj`,
`clojure -M:test`, no Rama cluster) that executes the client builders against
entries shaped EXACTLY as the live `trail_view.clj` constructors emit them.

The class under hunt is the one that killed at wave-1: **a projection silently
drops an edge endpoint, and the fixture hides it by carrying the endpoint the live
path never produces.** It is present again, in the same builder family.

---

## Verdict (one paragraph)

**One BLOCKER, live-confirmed.** `relation-activity-entry` emits
`:entry/detail {:kind :status :relation-id}` with NO `:from`/`:to`, even though the
source `RelationActivityRow` already carries `from-kind/from-id/to-kind/to-id`. The
client's lineage pass (`threads/rt-lineage-row`) requires both endpoints, so over
the LIVE feed it produces **zero lineage edges → zero threads → every target lands
in the band**, and the relation marks render as degenerate kraft stubs that name no
far end (probe: `:edges []`, all `:band`, scene = 3 banded cards + 2 endpoint-less
kraft-lines, 0 threads). The fixture masks this exactly as wave-1 did: its rt entries
carry `:from`/`:to`, so the gates pass green while the live path is threadless. The
**claimed-ms two-clock seam (class 2) is correct end-to-end** — the three key names
(`:claimed/at-ms` request → `:claimed-at-ms` row field → `:time/claimed-ms` entry)
map correctly at every hop, values are proper epoch-ms longs, and nil flows through
honestly (probe: commit `claimed 06-21 · arrived 06-22`; md `arrived 06-22 · claimed
unknown`). Two SHOULD-FIX drifts remain: the fixture invents an `:entry/actor
:asserted-by` on source/transcript entries that the live builders hard-code to
`nil` (band-2 reading line shows an asserter live will never render), and the
`transcript-file-updated` entry is mis-mirrored in four keys (target kind/id, actor,
detail) including the fold-key input `:entry/detail :conversation`. No crash on any
live-shaped input.

---

## Core deliverable — complete fixture-vs-live key diff

Live builders: `relation-activity-entry` (trail_view.clj:558-569),
`source-activity-entry` (582-598), `file-activity-entry` (571-580).
Row sources: `RelationActivityRow` (relation_kernel.clj:270-273),
`SourceIngestCompletionRow` (object_container.clj:90-97),
`TranscriptFileOffsetRow` (object_container.clj:165-168).

### Entry type `:relation-transition` — fixture entries 0/3/4 + R-2 marks

| key path | fixture carries | live emits (relation-activity-entry) | verdict |
|---|---|---|---|
| `:entry/kind` | `:relation-transition` | `:relation-transition` | MATCH |
| `:entry/target :id` | id | `(:from-id row)` | MATCH |
| `:entry/target :kind` | `:doc`/`:block` | `(:from-kind row)` | MATCH |
| `:entry/target :display-name` | absent | absent | MATCH |
| `:entry/address` | `(trail/context-bundle {:targets [id]})` | same (`->address`) | MATCH |
| `:time/claimed-ms` | long | `(:claimed-at-ms row)` = ts (long) | MATCH |
| `:time/arrival-ms` | long | `(:arrival-at-ms row)` (long) | MATCH |
| `:entry/actor :asserted-by` | string | `(:asserter-actor-id row)` | MATCH |
| `:entry/actor :written-by` | string \| nil | `(when neq ...)` \| nil | MATCH |
| `:entry/detail :kind` | kind kw | `(:relation-kind row)` | MATCH |
| `:entry/detail :status` | `:asserted` | `(:relation-status row)` | MATCH |
| `:entry/detail :relation-id` | `"rel:*"` | `(:relation-id row)` | MATCH |
| **`:entry/detail :from`** | **`{:kind :id}`** | **ABSENT** | **BLOCKER** |
| **`:entry/detail :to`** | **`{:kind :id}`** | **ABSENT** | **BLOCKER** |
| `:entry/detail :dead-end?` | `true` (entry 3 only) | ABSENT | see B1 note |

> The row HAS the endpoints (`to-kind/to-id/from-kind/from-id`, relation_kernel.clj:271);
> the builder simply never reads `to-*` into the detail and never wraps `from-*`.

### Entry type `:source-ingested` — fixture entry 1 + R-2 commits/folddoc/loose

| key path | fixture carries | live emits (source-activity-entry) | verdict |
|---|---|---|---|
| `:entry/kind` | `:source-ingested` | `:source-ingested` | MATCH |
| `:entry/target :id` | id | `(:document-container-id row)` | MATCH |
| `:entry/target :kind` | `:doc` | `:doc` (hard-coded) | MATCH |
| `:entry/target :display-name` | basename / sha7 | `(source-ref->display-name (:source-ref row))` | MATCH |
| `:entry/address` | `(trail/context-bundle …)` | same | MATCH |
| `:time/claimed-ms` | long (nil on entry 1's original had a val) | `(:claimed-at-ms row)` | MATCH |
| `:time/arrival-ms` | long | `(:completed-at-ms row)` = now-ms | MATCH |
| **`:entry/actor :asserted-by`** | **`"import:md"` / `"import:git-spine"`** | **`nil` (hard-coded, :596)** | **SHOULD-FIX** |
| `:entry/actor :written-by` | nil | nil | MATCH |
| `:entry/detail :source-ref` | string | `(:source-ref row)` | MATCH |
| `:entry/detail :derived-unit-count` | present (fixture entry 1 OMITS it) | `(:derived-unit-count row)` always present | minor (fixture under-spec) |

### Entry type `:transcript-file-updated` — fixture entry 2

| key path | fixture carries | live emits (file-activity-entry) | verdict |
|---|---|---|---|
| `:entry/kind` | `:transcript-file-updated` | `:transcript-file-updated` | MATCH |
| `:entry/target :id` | `"oc:chat-conversation:chat:ab12"` | `(:file-key row)` | DRIFT |
| **`:entry/target :kind`** | **`:conversation`** | **`:transcript-file`** | **DRIFT** |
| `:entry/target :display-name` | `"ab12.jsonl"` | `(basename (:file-path row))` | MATCH (shape) |
| `:time/claimed-ms` | nil | nil (hard-coded) | MATCH |
| `:time/arrival-ms` | long | `(:updated-at-ms row)` | MATCH |
| `:entry/actor :asserted-by` | `"import:transcript"` | `nil` (hard-coded) | DRIFT |
| `:entry/actor :written-by` | nil | nil | MATCH |
| **`:entry/detail :conversation`** | **`"chat:ab12"`** | **ABSENT** | **DRIFT (fold-key consumer)** |
| `:entry/detail :file-path` | string | `(:file-path row)` | MATCH |
| `:entry/detail :line-count` | ABSENT | `(:line-count row)` (real field, :167) | live-only key |

### Feed level

| key | fixture | live | verdict |
|---|---|---|---|
| `:feed/entries` / `:feed/address` / `:feed/rendered-at-ms` | present | present | MATCH |
| `:feed/omissions` | `[{:omission/kind :feed/uncovered :feed/gap _ :reason _} …]` | `feed-uncovered` (trail_view.clj:56-60) — byte-identical shape | MATCH |

> Bundle-layer shapes (verdicts / holes / anchors / `:relations`) are NOT in the feed
> path — they ride `read-context-bundle`/`assemble-bundle` and are gated by the
> separate `test/resources/trail_face/bundle.edn`. Out of this feed-fixture's scope.

---

## Findings ranked

### BLOCKER

**B1 — `relation-activity-entry` drops both lineage endpoints; live feed is
threadless.**
`src/app/server/rama/trail_view.clj:558-569`. The builder emits
`:entry/detail {:kind (:relation-kind row) :status (:relation-status row)
:relation-id (:relation-id row)}` — no `:from`, no `:to`. But
`RelationActivityRow` (relation_kernel.clj:271, constructed 480-485) already carries
`from-kind from-id to-kind to-id`. The client requires both:
- `threads/rt-lineage-row` (threads.cljc:94-104): `(and … (get-in d [:from :id])
  (get-in d [:to :id]))` → nil → row dropped → `current-lineage-rows` = `[]` →
  `components` = `{}` → `assign :assignment` = every target `:band`.
- `scene/build-timeline-scene` rt-edges (scene.cljc:708-712):
  `(when (and (mark? e) (:from d) (:to d)) d)` → empty → `dead-end-ids` sees no
  mark edges; `kraft-mark-nodes` (scene.cljc:235-285) reads `from-id`/`to-id` = nil,
  `far-id` resolves to nil, so the mark renders as a kraft stub naming NO far end —
  the "an edge may never silently vanish" invariant (trap 3) is violated live.

**Live probe (constructed rows):**
```
threads/assign :edges          = []
threads/assign :assignment     = {oc:doc:9fdoc :band, oc:doc:cmt-a2 :band,
                                  oc:doc:cmt-a1 :band, oc:doc:earlier :band, …}
build-timeline-scene childtypes = {:connector 2, :kraft-line 2, :band-note 1, :feed-card 3}
threads in carry assignment     = ()            ;; ZERO threads
```
Same entries with the FIXTURE's `:from`/`:to` restored → `:edges` has 2 rows,
`:assignment` forms real threads. That is precisely how the gate stays green while
the live face is a flat band.

*Failure scenario:* `/trail timeline` over any real corpus shows an all-band face
(honest `N unthreaded`) with **no lineage lanes at all**, and every relation mark is
an endpoint-less kraft stub. This is B1 = the wave-1 kill, recurred.
*Fix (additive, off-fence for t4-spine's server side):* in `relation-activity-entry`
read the four row fields already present —
`:from {:kind (:from-kind row) :id (:from-id row)} :to {:kind (:to-kind row) :id
(:to-id row)}` — and (for the dead-end card path) surface `:dead-end?` when
`relation-kind`/descriptor marks it. This is trail-room doubt #1 confirmed, and the
endpoints cost nothing to add: they are on the row.

**B1 note (dead-end):** the CARD's dead-end flag (`feed-entry-card`, cards.cljc:395-398)
also checks `(= :dead-end (:kind detail))`, and live `:entry/detail :kind` =
`relation-kind`, so a `:dead-end`-kind relation still flags the card. But
`lanes/dead-end-ids` (the scene-level dead-end join) runs off `rt-edges`, which is
empty live — so dead-end lane styling is lost even though the card dot survives.

### SHOULD-FIX

**S1 — source/transcript `:entry/actor :asserted-by`: fixture invents an importer,
live emits `nil`.**
`trail_view.clj:596` (source) and `:579` (file) hard-code `{:asserted-by nil
:written-by nil}`. The fixture writes `"import:md"` / `"import:git-spine"` /
`"import:transcript"`. Consumer: `cards/band-line-ops` (cards.cljc:327-331) appends
`… · <asserted-by>` to the band-2 reading line only when `asserted-by` is truthy.
**Probe:**
```
LIVE   commit band-2 → "↓ aaa1111"  +  "claimed 06-21 · arrived 06-22"     (no asserter)
FIXTURE commit band-2 → "↓ aaa1111"  +  "claimed 06-21 · arrived 06-22 · import:git-spine"
```
So gate G2 proves "reading line = stamp + asserter" over a shape the live source
branch never produces; live source/commit cards show a barer line. Decision needed:
either the fixture uses `nil` to match live, or the live builders stamp the importer
id (`import:git-spine` / `import:md`) — the SEAMS work already knows the importer at
`commit->import-request`. Flag for a contract ruling, do not silently pick.

**S2 — `transcript-file-updated` entry is mis-mirrored in four keys, including the
fold-key input.**
Fixture entry 2: target `{:kind :conversation :id "oc:chat-conversation:chat:ab12"}`,
actor `{:asserted-by "import:transcript"}`, detail `{:conversation "chat:ab12"}`.
Live `file-activity-entry`: target `{:kind :transcript-file :id (:file-key row)}`,
actor `{:asserted-by nil}`, detail `{:file-path … :line-count …}` — **no
`:conversation`**. Consumer: `lanes/entry-thread-key` (lanes.cljc:41-45) reads
`:entry/detail :conversation` FIRST. **Probe:**
```
LIVE    file entry-thread-key = "session:ab12/file0"   (family-key of file id)
FIXTURE file entry-thread-key = "chat:ab12"             (:entry/detail :conversation)
```
Different fold behavior. Pre-existing (original 5 entries, byte-identical), and a
lower-traffic branch (file activity, not the active claimed-ms/relation seams), but
the fixture's transcript entry does not mirror its live builder. No crash. Align the
fixture to the live `file-activity-entry` shape, or record why the conversation-form
is deliberately modeled.

### DOUBT

**D1 — `compressed-two-clock-stamp` / `two-clock-stamp` assume non-nil arrival.**
cards.cljc:99-116 / 77-87 call `ms->compact-date-str arrival …` unconditionally;
`ms->utc-ymd nil` → `(quot nil 86400000)` NPE. Not reachable through the feed:
`assemble-feed`'s `in-window?` (trail_view.clj:600, 610) drops any entry with nil
`:time/arrival-ms` before it renders, and all three live arrival sources are longs
(`arrival-at-ms` falls back to 0; `completed-at-ms`/`updated-at-ms` are wall clocks).
Latent only — a future caller rendering an unwindowed entry would crash. Low.

**D2 — fixture source entry 1 omits `:entry/detail :derived-unit-count`.**
Live always includes the key. No client builder consumes it (grep-verified: only
fixture + live reference `:derived-unit-count`). Cosmetic fixture under-spec.

---

## Class 2 — claimed-ms end-to-end: VERIFIED CORRECT

Three key names by design, one at each layer; each hop maps correctly:

| hop | key | site | value |
|---|---|---|---|
| request (namespaced) | `:claimed/at-ms` | git_spine.clj:201 `commit->import-request` | `(:committed-at-ms commit)` = `(* 1000 (parse-long-safe ct))` → epoch-ms long |
| row field (unnamespaced) | `:claimed-at-ms` | object_container.clj:615 `source-ingest-completion-row` → `SourceIngestCompletionRow` field 11 (:97) | copies `(:claimed/at-ms request)` |
| feed entry (`time` ns) | `:time/claimed-ms` | trail_view.clj:594 `source-activity-entry` | reads `(:claimed-at-ms row)` |
| client render | `:time/claimed-ms` | cards.cljc:105 `compressed-two-clock-stamp` | date-maths the ms |

- Arity is correct: `->SourceIngestCompletionRow` passes 11 args (:604-615) into the
  11-field record (:96-97); arg 11 `(:claimed/at-ms request)` → field 11
  `claimed-at-ms`. The dead second constructor (markdown_adapter.clj:239) also threads
  the key, so the record's new arity never throws (SEAMS seam 1 doubt 5 stands).
- **Nil is honest through every hop:** md/watcher requests never set `:claimed/at-ms`
  → row field nil → `:time/claimed-ms` nil → `compressed-two-clock-stamp` nil branch
  → `"arrived MM-DD · claimed unknown"` (probe confirms).
- The relation-transition path is independently correct: `RelationActivityRow` sets
  `claimed-at-ms = ts` (payload semantic time), `arrival-at-ms = envelope sent-at`
  (relation_kernel.clj:476-477), and `relation-activity-entry` reads both — so rt
  cards get a real two-clock too (probe: rt stamp `06-22`, compressed when equal).
- No NEW key reaches the client: the additive `:claimed-at-ms` field is invisible to
  the client (it only ever sees `:time/claimed-ms`, a pre-existing key). No client
  `case`/closed-spec can reject it. **Class-4 "did claimed-ms break a client shape" =
  NO.**

**Probe output (class 2/3):**
```
stamp commit (claimed≠arrival) = "claimed 06-21 · arrived 06-22"
stamp md     (claimed nil)     = "arrived 06-22 · claimed unknown"
stamp rt     (claimed=arrival) = "06-22"
```

---

## Per-class verdicts

- **Class 1 (fixture-vs-live key drift):** B1 confirmed (relation `:from`/`:to`);
  siblings found — S1 (`:asserted-by` on source/transcript), S2 (transcript entry
  target/actor/detail). D2 minor.
- **Class 2 (claimed-ms end-to-end):** CORRECT at all three key names and both
  builders; nil honest; no arity fault.
- **Class 3 (band-2 stamp rendering):** stamp logic correct on all live shapes
  (claimed-present / nil / equal). The only rendering drift is S1 (asserter absent
  live) — the stamp itself matches.
- **Class 4 (spine change hitting a room-read shape):** the claimed-ms addition
  introduces no new client-visible key and no closed-spec/`case` rejection; the ONLY
  live-vs-fixture entry-shape break is B1, which predates and is orthogonal to the
  claimed-ms work.

---

## Probes-attempted ledger

| probe | mechanism | result |
|---|---|---|
| `threads/assign` over live rt (no from/to) | `clojure -M:test` pure | `:edges []`, all `:band` — **lineage death confirmed** |
| same over fixture-shaped rt (from/to) | pure | 2 edges, real threads — contrast confirmed |
| `compressed-two-clock-stamp` × {commit, md-nil, rt} | pure | correct on all three |
| `feed-entry-card` band-2 × {live commit, live md, fixture commit} | pure | live shows NO asserter; fixture shows `· import:git-spine` |
| `build-timeline-scene` over full live feed | pure | no crash; `{:feed-card 3 :kraft-line 2 :connector 2 :band-note 1}`, 0 threads |
| `lanes/entry-thread-key` live-file vs fixture-file | pure | `session:ab12/file0` vs `chat:ab12` — fold-key drift |
| claimed-ms hop trace (`:claimed/at-ms`→`:claimed-at-ms`→`:time/claimed-ms`) | static read git_spine/object_container/trail_view | correct arity + key mapping |
| `RelationActivityRow` carries endpoints? | static read relation_kernel.clj:271 | YES (`to-kind/to-id/from-kind/from-id`) — fix is additive |
| `TranscriptFileOffsetRow` has `:line-count`? | static read :167 | YES (live read is real; fixture uses `:conversation` instead) |

Probe file: `scratchpad/cross-falsify/probe.clj`. No repo file modified; env.clj not read;
no Rama cluster launched (the main session's serial suite ran undisturbed).
