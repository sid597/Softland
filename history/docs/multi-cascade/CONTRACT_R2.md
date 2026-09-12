# multi-cascade — durable runner slice (R2) · CONTRACT

STAGED 2026-07-27 (Fable). Dark-lane organ #1, second slice (terrain: organ
III / CORE P ladder position 2 — "durable runner + log-consuming topology at
its first honest customer"). Binding with decisions.md; this contract governs
over every derived artifact. R1's `CONTRACT.md` remains the binding record of
the declaration slice (code `34bf7c0`); where R2 extends R1's grammar, this
file governs the extension.

**Durable-touch ⇒ FULL gate tier** (2026-07-27 cadence ruling: slim is for
additive-and-dark mechanism with zero durable touch; R2 mints depots, PStates,
a topology, and one new projection entry-kind — full independent
re-verification at gate).

## 1. Purpose

R1 gave server acts a declaration table and one dispatch point, but the
runner is fire-and-forget: a dispatched act that dies with its JVM is gone,
receipts are optimistic (`GATE.md` open doubt 1 — minted before the future
body runs), and failure truth lives only in logs. R2 mints the **durable
runner**: a row may declare `:cascade/runner :durable`, and its dispatch
becomes a durable obligation in a new cascade-log Rama module — obligation
recorded first, handler executed after the obligation is materialized,
outcome observed back into the log, and an explicit recovery sweep that
completes obligations a dead JVM left behind. Dispatch receipts for durable
rows are minted AFTER the obligation ack — doubt 1's falsifier, executed.

**First honest customer — episode-retry**, the banked stranded-lane defect
(episode-chain block on the board, "retry-rollback queued"): when a FRESH
episode's CLI spawn dies before its jsonl file exists, the lane's runtime
cell (`note-episode-turn!` stamps it before the spawn) and the durable
adoption path (`current-episode!` reads the newest turn cell's
`:episode-id`) both point at a session that never existed; every following
turn issues `--resume <dead-uuid>` and fails until the 1-hour boundary.
The repair act must itself survive JVM death — if it rides a best-effort
future and the JVM dies at turn end, the lane stays stranded, which is the
exact defect class. That is why episode-retry is the durable runner's first
honest customer and not another best-effort row.

Honesty note, carried verbatim (space-as-entity `NOW.md` punt table — the
law this slice builds under):

> Honesty note on the log topology: it observes events AT-LEAST-ONCE;
> truth-writes converge to exactly-once EFFECT via deterministic ids;
> externally-reaching reactions must each carry their own idempotency story
> (obligation-recorded-first, deterministic run-id, converge-before-call —
> the autotag pattern) or retries double-fire the external act. That is what
> :external-via-derived-worker marks.

Consumers, in order: (1) episode-retry (day one — the FIRST NEW ROW; this
activation writes the dark-interval lessons, §7) · (2) zoom band-crossing
acts (the first band-act declares against the same table and picks its
runner class) · (3) the consolidator on the clock (R4 appends time-shaped
emissions to the same log — the log is the tempo's substrate) · (4)
rows-as-editable-material (R3 reads the same table + run records).
(2)–(4) consume the SHAPE, not code built now.

Non-goals / refusals (each an extension point): NO clock pulse, timer, or
scheduler — the sweep is explicitly invoked, never self-firing (R4) · NO
retry-with-backoff policy — `:failed` runs are terminal and enumerable,
never re-swept (a retry loop without a clock is a spin; R4 owns tempo) ·
NO auto-respawn of the resident — the repair heals the LANE; Sid's next
word retries the turn (an agent spawning without a user act is a policy
fork nobody has lived a want for; refuse until a lived friction summons
it) · NO migration of autotag onto the durable runner — row #1 stays
`:best-effort`, byte-behavior-identical (its end-to-end idempotency already
lives in circulation; moving it buys nothing and touches a proven lane) ·
NO rows-as-material (R3) · NO cross-boundary `--resume` (structurally dead
by D-core design; stays dead) · NO new relation kinds, no OC/RK/llm-module
edits of any kind.

## 2. Placement ruling

**NEW Rama module** `app.server.rama.cascade-log`
(`src/app/server/rama/cascade_log.clj`), deployed as the SIXTH module (one
`MODULE_VARS` line in `bin/land`). Why not the alternatives:

- Not inside object-container or any existing module: adding
  topologies/PStates to a deployed module is a live module UPDATE (risk to
  the five running organs); a new module deploys additively. And cascade
  runs are not conversation material — PState ownership law says the organ
  owns its own truth.
- Not `material_circulation.clj`: circulation is one HAND; the runner is
  the loom's motor — same separation R1 ruled.
- Not a hand-rolled journal beside Rama: the EDN-WAL is the recorded dead
  branch (decisions.md, durable-ground).
- Not more in-process code in `cascade.clj` alone: the defect requires
  durability across JVM death; no in-process structure survives the crash
  class that produced it.

Runner + sweep code live in `app.server.cascade` (the one dispatch point
stays the one dispatch point). The repair handler lives in
`app.server.episode` (episode truth belongs to the episode organ). The
cascade-log module is deliberately NOT added to
`default-material-policy-paths` (same kernel rationale R1 recorded; policy
tier begins at R3). Reversal cost: destroy one module + revert commits —
nothing else references the new durables until the row fires (mechanism
ceremony, §7).

## 3. The cut

### 3a. Row grammar extension

Rows gain `:cascade/runner` — `:best-effort` (absent = this; row #1's map
is byte-untouched) or `:durable`. **A row may declare `:durable` only if
its `:cascade/idempotency` string names the full story** — scope,
transition law, and duplicate-execution tolerance (the honesty note as
grammar; enforced by a suite scan while rows are code, G10).

### 3b. The cascade-log module

- **Depots** (both `hash-by :run/id` — every record for one run lands on
  one partition, so every PState pair-write below is one event, atomic):
  - `*cascade-obligations-depot` — records
    `{:run/id :cascade/id :cascade/trigger :emission/id :payload
    :obligated-at-ms}`. The emitter (react!) expands matching durable rows
    JVM-side and appends ONE obligation per (row × emission) — the table
    stays JVM data; the topology stays table-blind.
  - `*cascade-observations-depot` — records
    `{:run/id :status :receipt :observed-at-ms}`, `:status` ∈
    `#{:completed :failed}`.
- **One stream topology** (`cascade-runs`) owning BOTH PStates, two
  `source>` blocks in one `<<sources` (single-owner law):
  - `$$cascade-runs` — `{String run-id → map}` (top-level keyed; the run
    map is small and bounded — no subindex needed).
  - `$$cascade-pending` — `{String run-id → Long obligated-at-ms}` (the
    sweep's read; pending-only, stays small).
  - Obligation branch: **upsert-if-not-terminal** — write the run row
    `:status :obligated` + add to `$$cascade-pending` ONLY when the run is
    absent or still `:obligated` (T6). Observation branch: overwrite status
    to the terminal value + remove from `$$cascade-pending` in the same
    event; first terminal wins — a conflicting later terminal is recorded by
    OVERWRITING the run row's `:late-conflict` value (nil until a conflict;
    then the LAST conflicting observation's
    `{:status :receipt :observed-at-ms}`), never a status regress.
    (P1-stop ruling 2026-07-27: a VALUE that converges under at-least-once
    replay, never a counter — an increment is not replay-idempotent
    without unbounded seen-identity state, which the bounded-row pin and
    the two-PState shape both forbid; conflict PRESENCE plus the latest
    evidence is the diagnostic truth R2 consumes.)
  - NO side effects in topology code, ever. Handlers run JVM-side (the
    back-arrow shape). Stream (not microbatch): at-least-once processing
    converging by deterministic keys is the house law (settled ground);
    exactly-once EFFECT lives in handler idempotency, per the honesty note.
- **Identity + resolution** (one section, one law):
  - `:emission/id` — **globally scoped**, minted by the EMITTER,
    deterministic from the trigger's discriminator; for
    `:episode/turn-closed` it is
    `"casc-em:episode-turn-closed:" + sha256(turn-id NUL terminal-status)`.
    A replayed HTTP turn re-derives the same emission; a retry that closes
    with a DIFFERENT terminal status is a NEW emission (new obligation —
    the handler's idempotent effect dedupes; no other transition mints one).
  - `:run/id` — **globally scoped**: `"casc-run:" + sha256(cascade-id NUL
    emission-id)`. ONE row forever; status transitions overwrite that row
    in place; NO transition ever mints a new run-id.
  - Resolution: consumers read a run by
    `foreign-select-one [(keypath run-id)] $$cascade-runs`; pending
    enumeration reads `$$cascade-pending` per partition (read plan: one
    seek + sequential iteration per partition; pending is small by
    construction). Surfaced as `(cascade/run ctx run-id)`,
    `(cascade/pending-runs ctx)`, `(cascade/failed-runs ctx)` — the
    console serve surface until R3.

### 3c. react! — the durable branch

For each matching `:durable` row: build the BOUNDED, EDN-serializable
durable payload (scalars/maps only — runtime handles never ride the depot;
T2's law extended durably) → `foreign-append!` the obligation with `:ack`
(after the ack, the colocated stream topology has materialized the run row
— converge-before-call) → THEN spawn the handler future → on return/throw
append the `:completed`/`:failed` observation (with a bounded receipt map).
The immediate receipt for a durable row is
`{:cascade/id … :dispatched? true :durable? true :emission/id … :run/id …}`
— minted AFTER the obligation ack. Best-effort rows keep R1's exact
behavior and their documented optimistic receipts. The entire durable
dispatch path is wrapped total: any failure logs `[CASCADE][EMIT-FAILED]`
and never fails or delays the caller (T9) — a lost emission degrades to
today's behavior and the next emission re-fires.

### 3d. The recovery sweep

`(cascade/resume-obligated! ctx {:grace-ms 60000})` — enumerate
`$$cascade-pending`; for entries older than the grace window, re-execute
the obligation's handler through the SAME runner path (handler +
observation append). Re-runs `:obligated` only; `:failed` is terminal and
enumerable, never swept (no retry loop without a clock — R4). Wired ONCE
at server boot, explicitly, after the runtimes bind — never a namespace
load side effect (dark-organ law; G9 proves classload runs nothing). The
sweep logs one `[CASCADE][SWEEP]` receipt line (count resumed / count
pending / count failed-terminal).

### 3e. Emission site #2

The turn-end waiter in `run-episode-turn` emits `:episode/turn-closed`
BEFORE the `:open → terminal` status overwrite of the turn cell
(obligation-recorded-first extends to the emission site: if the JVM dies
between emission and cell write, the repair still lands and is correct
regardless of the cell's status; T8). Payload — all EDN scalars:
`{:turn-id :terminal-status :exit-code :duration-ms :episode-id :fresh?
:seed? :thread-id :conversation-id :cwd :time-ms :jsonl-exists?}` where
`:jsonl-exists?` is observed at close time via `episode-jsonl-file`.
Emission site #1 (`:episode/turn-durable`) is byte-untouched.

### 3f. Row #2 — `:cascade/episode-retry`

- Declaration: trigger `:episode/turn-closed` · runner `:durable` ·
  effect-class `:durable-via-request` (the repair ends in an acked OC
  request; nothing leaves the land) · actor `"system:episode-retry/v1"` ·
  handler `app.server.episode/repair-stranded-lane!`.
- Idempotency string (the full story, per 3a): "run-id derived from
  (turn-id, terminal-status); the repair is a pure upsert — the defunct
  cell's import identity is TRANSITION-scoped, derived from
  (episode-id, turn-id, terminal-status), and every value byte derives
  from the emission payload, so replays and concurrent duplicate
  executions of one transition converge byte-identically on one durable
  cell, while a LATER eligible failure of the same episode-id mints a NEW
  import that advances the same projected cell (T7); the runtime rollback
  is compare-and-remove on this JVM's own lane atom; a NEW run is minted
  only by a new (turn, terminal-status) emission."
- Handler head DECLINE (`:skipped` receipt, emission stays unconditional —
  R1's T6 law): declines unless `terminal-status ∈ #{:failed :timeout}`
  AND `fresh?` AND NOT `jsonl-exists?`.
- Execution-time RE-VERIFY (T5): before repairing, re-check the jsonl file
  is STILL absent (`episode-jsonl-file cwd episode-id`); a swept replay
  must never defunct an episode that has since materialized. Declines
  `:skipped` if the file now exists.
- Repair acts, in order:
  1. **Durable**: upsert ONE defunct cell into the lane's conversation
     container, riding the EXISTING hint-only `imp:ep:` import lane (the
     geometry-cell precedent — zero kernel edits): order-key
     `ep-chain:<sha8(episode-id)>`, entry-kind `:episode-chain-defunct`,
     value `{:world-id :lane-id :episode-id :marked-at-ms}` where
     `:marked-at-ms` is the TURN's `:time-ms` from the payload — never
     wall clock (T4: every value byte replay-stable). Import identity
     (P0 ruling 2026-07-27): imp-key
     `sha("episode-defunct " episode-id " " turn-id " " (name terminal-status))`
     — its OWN, transition-scoped identity, never the turn cell's imp-key
     (T7); one stable order-key cell, overwritten in place by each new
     eligible-failure import (the turn-cell precedent,
     `episode.clj:464-489`). The
     request's actor is a `:system` actor named
     `"system:episode-retry/v1"` — the map must not lie: this is a
     machine act, never Sid's hand.
  2. **Runtime**: compare-and-remove the lane's `!episode-chains` entry
     iff it still names the dead `:episode-id` (a swept duplicate in
     another JVM touches only its own — empty — atom; harmless, T11).
- **Adoption law** (the defect's actual kill): `current-episode!`'s
  durable fallback read filters turn cells whose `:episode-id` is
  defunct-marked with `cell.time-ms ≤ marked-at-ms` before taking `last`.
  A LATER turn (time-ms > marked-at-ms) re-validates its episode-id — a
  re-minted lane-id episode is adoptable again — and if THAT re-minted
  episode later fails eligibly, its repair's new transition-scoped import
  advances the same `ep-chain:` cell to the later `:marked-at-ms`:
  same-id revalidation stays repairable, never single-use (P0 ruling
  2026-07-27, T7 carries the monotonicity argument). `decide-episode`
  stays PURE and unedited; filtering is read-layer only, and a chain-cell
  read failure degrades to today's exact behavior (total).

### 3g. Payload hygiene

No production emission payload may carry `:lines` (the canned-stream test
seam) — R1 `GATE.md` doubt 2's guard, landed with the second emission site
it predicted: an executable scan over emission sites + a react!-side
assert in test builds (G8).

## 4. Traps ledger (cite T-numbers in code comments)

- **T1 — idempotency bytes are durable truth, now including the NEW ones.**
  Naive: treat emission-id/run-id/defunct-imp-key derivations as
  refactorable. Failure: these feed durable records from day one — any
  byte change strands historical runs/cells invisibly.
  `material_circulation.clj` stays READ-ONLY; the new derivations are
  pinned at birth; G2 byte-compares across independent clusters.
- **T2 — loads pure; durable payloads are data.** Naive: payload closing
  over runtimes/fns, or a boot form in the new namespaces. Failure: a
  runtime handle in a depot record breaks serialization or, worse,
  deserializes into lies; a load side effect isn't dark. Ruling: payloads
  are EDN scalars/maps (G8 round-trips them); ctx binds at execution time;
  classload runs nothing (G9).
- **T3 — converge-before-call.** Naive: spawn the handler future before or
  concurrently with the obligation append. Failure: JVM dies after the
  effect, before the obligation — an effect with no durable record; or
  sweep sees no obligation to resume. Ruling: append + `:ack` completes
  BEFORE the handler future spawns; receipts mint after the ack.
- **T4 — replayed durable writes must be byte-stable.** Naive:
  `System/currentTimeMillis` for `:marked-at-ms` (or any replayed value).
  Failure: same imp-key + different payload bytes =
  fingerprint-conflict REJECTION (the G4b mechanism) — the repair's replay
  durably rejects instead of converging. Ruling: every replayed value byte
  derives from the emission payload; G3 replays and asserts convergence.
- **T5 — execution-time re-verification.** Naive: trust the payload's
  `:jsonl-exists?` observation at sweep time. Failure: a stale swept
  obligation defuncts an episode whose file has since appeared — repair
  becomes the wound. Ruling: the handler re-checks the file before acting;
  G5 drives the both-branches case.
- **T6 — obligation replay must not resurrect a terminal run.** Naive:
  obligation branch unconditionally writes `:obligated` + pending. Failure:
  at-least-once delivery reorders an obligation replay AFTER the terminal
  observation → completed run re-pended → sweep re-executes forever.
  Ruling: upsert-if-not-terminal in the topology; first terminal wins; G3
  asserts a replayed obligation after completion stays terminal.
- **T7 — the defunct fact gets its OWN, TRANSITION-SCOPED import
  identity.** Naive #1: ride the turn cell's `(turn-id, status)` imp-key
  with an extra key in the value. Failure: same imp-key + different
  payload = fingerprint-conflict rejection; the turn cell's own overwrite
  semantics break. Naive #2 (the P0 fork, ruled 2026-07-27): pin ONE
  imp-key per episode-id, `sha("episode-defunct " episode-id)`. Failure:
  the adoption law deliberately lets a later turn revalidate that
  episode-id; if the re-minted fresh episode fails eligibly again, the
  same imp-key now carries a later `:marked-at-ms` → fingerprint-conflict
  → the second failure is unhealable — the R2 defect reborn, and worse:
  the strand is permanent (the resume attempt that follows is
  `fresh? false`, so the handler declines forever). Ruling: own order-key
  namespace (`ep-chain:`, ONE stable cell per episode) + imp-key
  `sha("episode-defunct " episode-id " " turn-id " " (name terminal-status))`
  — the turn-cell pattern (`episode.clj:486`): the same transition
  replays byte-identically (accepted once, then fingerprint-identical
  journaled no-op); a NEW eligible failure mints a new imp-key whose
  import overwrites the one cell, advancing `:marked-at-ms`. Advancement
  is causally monotone, not last-write-lucky: a same-id fresh re-mint
  exists ONLY because the prior marker was already durably visible to
  adoption (that visibility is what made the predecessor unusable), so a
  later transition's repair always appends after the earlier marker
  landed; T6 forbids re-execution after terminal and a duplicate import
  is a journaled no-op, never a projection rewrite — every
  replay-reordering route to regression is closed. The turn cell is
  byte-untouched by the repair.
- **T8 — emission before the status overwrite.** Naive: emit after the
  cell write "so the payload matches durable truth". Failure: JVM death
  between cell write and emission loses the repair — the exact defect
  class, reborn one line later. Ruling: emit first; the repair is correct
  regardless of whether the terminal cell write landed.
- **T9 — the turn lane is never hostage to the cascade.** Naive: let an
  obligation-append failure (module down, runtime boot throw) propagate.
  Failure: the durable runner's OWN outage kills Sid's turns; and a
  cached-throw `delay` on the cascade runtime handle is a permanent
  poison (the delay-totality gate class, framework W2-F7). Ruling: total
  wrapper, `[CASCADE][EMIT-FAILED]`, degrade to today's behavior; any
  lazily-booted cascade runtime handle feeding the turn path yields
  poisoned-but-total, never a cached throw.
- **T10 — best-effort lane byte-behavior-identical.** Naive: "unify" both
  branches through the new machinery. Failure: autotag's threading/latency
  changes silently — R1's whole behavior-identity case reopens. Ruling:
  `:best-effort` dispatch is R1's exact code path; row #1's map and the
  R1 focused suite are byte-green (G1).
- **T11 — two JVMs share the durable cluster.** Naive: assume one server
  JVM (proof servers on `:8081` against the shared cluster are REAL gate
  practice; both boot the sweep). Failure: concurrent duplicate handler
  execution treated as impossible. Ruling: duplicate execution is part of
  every durable row's idempotency story (3a); the repair's durable half
  converges, its runtime half touches only its own atom.
- **T12 — trigger names stay in-process; run records are the module's own
  truth.** Naive: minting cascade events into OC/RK vocabulary, or serving
  run state through conversation projections. Failure: durable vocabulary
  creep across organ boundaries — cutover-class in disguise. Ruling: the
  cascade-log module owns run truth; the ONE projection addition is the
  `:episode-chain-defunct` cell (episode truth, in the episode's
  container); G7 scans the diff.

## 5. Acceptance gates — FULL tier

Partition sum-check (re-partitioned by the P1-stop ruling 2026-07-27 —
the staged partition assigned G2/G4 whole to P1 while §9 reserves their
react!/runner machinery for P2): P1 = {G2 module half, G3 module half,
G4 module half, G9 module half, G12}; P2 = {G1, G2 emitter half, G3
repair half, G4 sweep half, G5, G6, G7, G8, G9 purity half, G10, G11}.
Sum-check: G1✓ G2(m+e)✓ G3(m+r)✓ G4(m+s)✓ G5✓ G6✓ G7✓ G8✓ G9(m+p)✓
G10✓ G11✓ G12✓ — all twelve covered, every half owned. Fable gate
re-verifies independently (full-suite re-run, G6 re-drive, G7 re-check).
Every gate names its owner. Live receipts run with
`-Dorg.apache.logging.log4j.level=INFO` (R1 `GATE.md` doubt 3: default dev
stdout swallows INFO) and are SERVER-READ receipts (durable-ground rule).

- **G1 — best-effort lane untouched (suite).** R1's focused suite
  (`app.cascade-table-test`) green unchanged; row #1's declaration map and
  the `:best-effort` dispatch path byte-compare to R1's semantics (same
  receipts shape, same `[CASCADE]`/`[CASCADE][FAILED]` facts). Owner:
  implementer.
- **G2 — durable dispatch record (suite).** (module half) An obligation
  appended through the IPC constructors + `foreign-append!` with `:ack` →
  the run row is readable `:obligated` in `$$cascade-runs` with its
  `$$cascade-pending` entry; emission-id/run-id constructors byte-compare
  across two independent clusters on identical input (R1's G1 pattern).
  (emitter half) For a durable row: react! returns receipts carrying
  `:emission/id` + `:run/id` only after the run row is readable
  `:obligated` (P1-stop ruling 2026-07-27: react!'s durable branch is
  §9-P2 machinery — the module half proves the log, the emitter half
  proves the barrier). Owner: implementer (P1 module / P2 emitter).
- **G3 — replay + reorder convergence (suite).** (module half) Same
  obligation appended twice → one run row; obligation replayed AFTER the
  terminal observation → status stays terminal, `$$cascade-pending` stays
  empty (T6); duplicate observations → first terminal wins, and a
  conflicting later terminal lands in `:late-conflict` with re-processing
  of the same observation records converging to the same value (a VALUE,
  never a counter — the ruled §3b law). (repair half)
  Same `:episode/turn-closed` emission dispatched twice → one defunct
  cell, fingerprint-identical import (journaled no-op, T4), atom
  untouched on the second pass; a SECOND eligible failure of the same
  episode-id (a new turn's transition) → a NEW import accepted and the
  ONE `ep-chain:` cell advances to the later `:marked-at-ms` (T7).
  Owner: implementer (P1 module / P2 repair).
- **G4 — crash-recovery drill (suite).** (module half) Obligation landed
  with NO observation; a fresh runtime reads it in `$$cascade-pending`
  past the grace window, and an observation append terminalizes it +
  empties pending — the recovery substrate the sweep drives. (sweep half)
  Obligation landed with NO handler execution (simulated JVM death at the
  T3 boundary); a fresh runtime + `resume-obligated!` → handler executes
  once, observation lands, run terminal, pending empty (P1-stop ruling
  2026-07-27: the sweep resolves handlers through the runner path —
  §9-P2 machinery). Owner: implementer (P1 module / P2 sweep).
- **G5 — stranded-lane kill (suite).** Full chain sim: fresh episode
  minted → spawn death (no file) → `:episode/turn-closed` `:failed` →
  repair → defunct cell durable with the SYSTEM actor + runtime entry
  removed → next `current-episode!` (both warm-atom-cleared and
  empty-atom/JVM-restart paths) mints FRESH, never `--resume`s the dead
  uuid → a LATER successful turn's episode-id re-validates (adoption law's
  time scope) → the re-minted SAME-ID fresh episode fails eligibly AGAIN
  (no file) → repair advances the marker to the later `:marked-at-ms` →
  adoption filters the second death's cells too and the lane mints fresh
  once more (the ruled T7 semantics: revalidation is repairable, never
  single-use). Both decline branches: healthy close → `:skipped`; file
  present at execution → `:skipped` (T5). Owner: implementer (P2).
- **G6 — the honest customer, LIVE (dev cluster).** On a drill lane
  (`?drill=` conversation, never genesis): force a fresh-spawn death
  before file creation; observe `[CASCADE]` row #2 with `:run/id`, the
  obligation + terminal run rows server-read from the cluster, the defunct
  cell durable; then type the lane's next word and watch it open a FRESH
  episode and answer — the lane healed without waiting for the boundary.
  Owner: implementer drives; **Fable re-drives at gate** (FULL tier).
- **G7 — additive-durable only (style + diff).** The diff adds exactly:
  one module (two depots, one topology, two PStates), one projection
  entry-kind (`:episode-chain-defunct`) + `ep-chain:` order-key namespace,
  one depot-record vocabulary (obligations/observations). NO existing
  module, depot, PState, serve contract, or cell shape edited; every
  existing projection reader's entry-kind filter verified positive (the
  new cell is invisible unasked); `material_circulation.clj` +
  `verb_registry.cljc` byte-untouched. Boundary test recorded: additive
  new-organ durables, the durable-ground/P2b precedent class — NOT
  cutover. Scan stops at this package's diff. Owner: implementer; Fable
  re-checks.
- **G8 — payload hygiene (suite + style).** No production emission payload
  carries `:lines` (scan over emission sites + assert); durable payloads
  round-trip EDN and contain no fn/runtime values. Owner: implementer (P2).
- **G9 — loads pure + sweep discipline (suite).** Fresh classload of
  `app.server.rama.cascade-log` + the edited namespaces starts no runtime,
  runs no sweep (G3-of-R1's pattern, scan stops at this package's files);
  the sweep re-runs `:obligated` only — a `:failed` run is never
  re-executed (assert against a poisoned fixture row); sweep respects the
  grace window. Owner: implementer (P1 module / P2 purity).
- **G10 — enumeration + grammar (suite).** `(cascade/rows)` lists both
  rows; row #2 carries runner/effect-class/actor/idempotency exactly as
  §3f; the durable-runner grammar scan refuses any `:durable` row whose
  idempotency string is absent/blank (3a's law, executable while rows are
  code); `(cascade/pending-runs)` / `(cascade/failed-runs)` /
  `(cascade/run)` read live. Owner: implementer (P2).
- **G11 — full suite + pinned scans (suite).** Full exact-tree suite
  green; every pinned-enumeration scan over every edited file re-run
  (grep the test tree for each edited path before calling the selection
  done — the machine-cut rule); new-file lint clean. Owner: implementer
  (P2); Fable re-runs at gate.
- **G12 — deploy-under-proof (ops).** Environment attestation FIRST
  (cluster identity, module list before/after). `bin/land deploy` of the
  sixth module reaches RUNNING through the settle loop; the five existing
  modules stay RUNNING (server-read `moduleStatus`, not CLI exit codes —
  the durable-ground ops rule); a worker restart preserves obligations +
  offsets (no depot-history replay — native resume). **ONE-SHOT marking:**
  the first-deploy precondition (module absent) is destroyed by this gate;
  the committed harness asserts the post-package invariant instead
  (module present + RUNNING + redeploy path clean). Owner: implementer
  (P1, on the real cluster, before G6 needs it live).

## 6. Stop clauses (escalate per the work-package skill; never improvise)

- A §8 manifest line's SUBSTANCE mismatches disk — a named symbol or form
  absent, or its call shape, guard form, adoption read, waiter ordering,
  `bin/land` deploy mechanics, or runner-registry semantics differing from
  what the line describes. Locator drift alone (line numbers or counts
  moved while the substance holds — including drift from this package's
  own earlier allowlisted phases) is NOT a stop: re-locate, log the drift
  in the phase artifact, continue (P0-stop ruling 2026-07-27; §8 states
  the binding rule).
- Any edit needed to an EXISTING module, depot, PState, serve contract, or
  durable cell shape — that reclassifies work cutover-side; stop, never
  absorb.
- The `/rama` phase artifacts (P0) contradict this contract in a way both
  readings could physically build — genuine policy fork; stop for ruling.
- The repair provably cannot land as read-layer filtering + one new cell
  (i.e. `decide-episode`'s pure contract must change semantically) — stop.
- The table needs durable storage to make the runner work — that is R3
  arriving early; stop.
- Module deploy fails on the real cluster or raises any license/node-count
  question (the two-node ceiling is settled ground).
- Any need to touch `material_circulation.clj`, `verb_registry.cljc`, or
  OC/RK/llm module code.

## 7. Landing sort + dark-lane bookkeeping

The module + runner are **MECHANISM-shaped**: deploy-under-proof (G12 +
the replay/crash drills; revert by commit + module destroy). Row #2 is
kernel code this slice (policy tier begins at R3; registry law untouched —
no material selects any of this). Boundary test run at staging: all new
durables are additive new-organ state (G7 enforces the sort).

**ACTIVATION — this package fires the interval clause.** Episode-retry is
the cascade table's FIRST NEW ROW: dark capacity, declared at R1, activates
here. The close session therefore: (1) writes the dark-interval lessons to
the board note + `engine-terrain.md` Lineage — did the table drift under
main, was it remembered, were the R1 proofs still green, was the board
line accurate; (2) OPENS the stratum's first batch retro (2026-07-27
cadence ruling: one batched retro + one adversarial recheck reading the
banked NOW/GATE trails of R1 + R2). Per-package close stays slim-mechanics:
Sid's commit decision → post-commit HEAD-dynamic re-run → board prune.

## 8. Input manifest — BINDING = SUBSTANCE (P0-stop ruling 2026-07-27)

Each line binds on its named symbols, forms, and semantics; line numbers
and counts are NAVIGATION HINTS, never binding — they drift under any
edit, including this package's own phases (P1 edits `bin/land` +
`test_runner.clj`; P2 edits `cascade.clj`; a count pinned on those is a
scheduled false stop). The implementer re-verifies every line's SUBSTANCE
against disk before code; hint drift with substance intact → re-locate +
log in the phase artifact, never stop; substance mismatch → §6's first
stop clause. Hints machine-verified (grep/wc, never hand-counted)
2026-07-27 at the ruling.

- `src/app/server/cascade.clj` (whole file — small; read all of it):
  `declared-rows`, `rows`, `react!` — the R1 shape the durable branch
  extends.
- `src/app/server/episode.clj` — `:753-:757` `episode-idle-ms` ·
  `:759-:763` `!episode-chains` runtime atom · `:765-:785`
  `decide-episode` (PURE — unedited) · `:787-:812` `current-episode!`
  (adoption fallback + file-existence belt; the belt covers only the
  `fresh? ∧ episode-id=lane-id` case) · `:814-:821` `note-episode-turn!`
  (stamps BEFORE spawn — the strand's mechanism) · `:827-:834`
  `episode-jsonl-file` · `:836-:849` `summon-argv` (`--session-id` vs
  `--resume`) · `:239-:268` geometry-cell-hint + upsert-in-place +
  fingerprint-conflict semantics (the pattern the defunct cell rides) ·
  `:464-:469` `turn-order-key` · `:471-:561` `turn-record-request`
  (imp-key per (turn-id, status) — the transition-scoped overwrite
  pattern T7 adopts, and why the defunct fact must not ride ITS key) · `:563-:577`
  `record-turn!` · `:579-:588` `read-turn-records`.
- `src/app/server_jetty.clj` — `:801-:851` autotag runtime delay + handler
  (guard-in-head, T6-of-R1) · `:853-:1114` `run-episode-turn`: `:903-:914`
  episode decision + total fallback · `:916-:929` `:open` cell
  durable-BEFORE-agent · `:999-:1006` emission site #1 (byte-untouched) ·
  `:1022-:1028` summon + `note-episode-turn!` · `:1048-:1083` the waiter:
  terminal status derivation + status overwrite (emission site #2 lands
  BEFORE the overwrite, T8) · `:1090-:1107` post-turn distill (already
  total on `:no-transcript`).
- `src/app/server/rama/material_circulation.clj` — READ-ONLY:
  `:481-:486` `autotag-run-id` (the deterministic-id class) · `:571-:605`
  `autotag-material!` (obligation-recorded-first / converge branches — the
  idempotency pattern §1 cites) · `:60-:80`
  `default-material-policy-paths` (cascade files stay out; kernel
  rationale).
- `src/app/server/rama/cluster.clj` — `:76-:110` manager +
  `get-module-name` + foreign-depot/pstate bundle pattern · `:185-:200`
  runtime accessors (the shape a cascade runtime accessor follows) ·
  `:241+` `face-projection-runtime` (boot-flag seam).
- `src/app/file_viewer.cljc` — `:429-:461` `face-rt`/`face-ctx` (cluster
  vs IPC boot seam; runtime plumbing edits, if any, stop here).
- `bin/land` — `:21-:31` `MODULE_VARS` (the sixth line) · `:136-:150`
  deploy + settle loop (concurrent-launch collision note).
- `src/app/shared/verb_registry.cljc` — `:33-:36` `effect-classes`
  (`:durable-via-request` is row #2's class).
- `test/app/test_runner.clj` — `:214-:247` fail-closed inventory · `:64`
  `isolation-exceptions` (new test namespaces booting fresh runtimes need
  their classification entries — R1's stop lesson, pre-empted in §9).
- `test/app/cascade_table_test.clj` — the R1 suite G1 keeps green; its
  fixture patterns (canned `:lines` seam, fixture tables) are the reuse
  base.
- `docs/current-mental-model/build/multi-cascade/GATE.md` — the three R1
  open doubts this contract folds in (doubt 1 → G2/§3c; doubt 2 → G8/§3g;
  doubt 3 → §5's INFO-logging rule).
- Rama platform: the `/rama` skill references (phases, dataflow, pstates,
  depots, partitioners) + `/rama-pitfalls` — P0 runs the phase process;
  the staging-level pitfalls verdict is in this session's record, and P0
  re-derives it at plan level.
- Authorization seam: the implementer verifies `:system`-actor semantics
  for the defunct-cell request against the actual `authorized-request?`
  code before P2 (flagged: staged from the episode.clj docstring's claim,
  not re-read at staging).

## 9. Handoff

Phases (fresh context each; one orchestrating session with fresh-context
subagents satisfies this — 2026-07-05 amendment; `/rama` owns the module
phase mechanics):

- **P0 — module design** (`/rama` phases 0–2: spec → plan → validation,
  artifacts in this directory; the plan re-runs the `rama-pitfalls`
  protocol against the concrete topology code shape).
- **P1 — the module** (implement `cascade_log.clj` + IPC constructors;
  gates G2-module, G3-module, G4-module, G9-module, G12; deploy lands
  here so P2's live gate has a module to hit. P1 touches NO file outside
  the module + its test + `test_runner.clj` classification + `bin/land` +
  the runtime-plumbing lines — the P1-stop ruling 2026-07-27 preserved
  this fence and moved the react!/sweep gate halves to P2 instead).
- **P2 — seams + activation** (runner branch in `cascade.clj`, emission
  site #2, repair handler + adoption law in `episode.clj`, boot sweep
  wiring; gates G1, G2-emitter, G3-repair, G4-sweep, G5, G6, G7, G8,
  G9-purity, G10, G11).

Allowlist — NEW: `src/app/server/rama/cascade_log.clj` ·
`test/app/cascade_log_test.clj` · `test/app/episode_retry_test.clj`.
EDITED: `src/app/server/cascade.clj` · `src/app/server/episode.clj` ·
`src/app/server_jetty.clj` (emission site #2 + boot sweep wiring ONLY) ·
`src/app/server/rama/cluster.clj` (cascade runtime accessors ONLY) ·
`src/app/file_viewer.cljc` (runtime plumbing ONLY, if the IPC boot needs
it) · `bin/land` (one `MODULE_VARS` line) · `test/app/test_runner.clj`
(classification entries for the two new test namespaces ONLY — same
isolation class as `app.cascade-table-test`). READ-ONLY:
`material_circulation.clj` · `verb_registry.cljc` · all OC/RK/llm module
files. No live fence set (rung 3 + R1 are landed); the tree must be clean
at entry per the shared-branch git discipline.

After P2 green: ONE falsification finder aimed at the genuinely-new
machinery (the durable runner's ordering/replay envelope + the repair's
adoption law), then the FULL Fable gate (independent full-suite re-run,
G6 re-drive, G7 re-check, falsification pass), then close per §7 —
commit decision (Sid), post-commit HEAD-dynamic re-run, board prune,
dark-interval lessons, and the stratum's first batch retro. After close:
the organ holds two rows (one best-effort, one durable) and both runner
classes are proven; next slices pull per the terrain ladder (R3
rows-as-material at 2–3 real rows, R4 clock) — each its own contract.
