# W2-D — the arsenal (gates G17–G21) · lane artifact

2026-07-11 · Opus subagent, lane W2-D · CONTRACT v2 §§7, 8, 16–19, 21 · booted
`/rama` + `/rama-pitfalls` before any Rama code. Gates RUN green in-context.
Committed nothing (orchestrator owns commits).

## What built (fence: exactly the six named files)

| File | Status | Lines |
|---|---|---|
| `src/app/server/rama/object_container/assembly_adapter.clj` | NEW | 507 |
| `src/app/server/rama/face_arsenal.clj` | NEW | 385 |
| `src/app/server/ingest_watchers.clj` | additive edit | +~100 (classify-fn plumbing + `faces-classify` + `import-assembly!`) |
| `src/app/server/rama/object_container.clj` | ONE additive branch | +16 (the pre-authorized `imp:asm:` `extract-object-key` branch, G18) |
| `src/app/server/rama/face_projection.clj` | additive edit | +~118 (`:assembly` + `:face-list` registry entries) |
| `test/app/face_arsenal_test.clj` | NEW | 587 |

**Adapter** (`assembly_adapter.clj`): `markdown_adapter.clj:443-517` template.
Identity §17: object-key `asm:<name>` (name = envelope `:assembly/name` when
usable, else file stem — deterministic identity for malformed files, R1);
import key `imp:asm:<object-key>:<sha-256(source-ref-key:source-hash)>`
(literal, so the string is `imp:asm:asm:<name>:<sha>`). Ingest validation via
the SAME `.cljc` compiler + registry the client wears
(`face-assembly/compile-assembly` × `face-primitives/registry` — T18, one fn
`validate-assembly-source` used by ingest AND serve). Malformed `.edn` still
ingests as source; derived material row (`du:…:assembly`, unit-kind
`:assembly/valid|:assembly/invalid`, verdict pr-str in text — the block-kernel
R4 precedent) carries `valid?`/errors honestly. Envelope provenance → lineage
edges: CLOSED field→kind map `{:birthed-by :produced, :based-on :based-on,
:supersedes :supersedes}` (existing D-004 kinds only; no passthrough — an
unregistered kind is unconstructible, G19), directions per §8/§17, asserter =
author (`:human`) else `import:face-arsenal` (`:import`), idempotency key =
sha-256(object-key|kind|target|import-key) (git_spine.clj:224 discipline),
`edge-already-asserted?` pre-check (git-spine precedent).

**Micro-kernel** (`face_arsenal.clj`): intent-only per KERNEL-SHAPE. ONE depot
`*face-arsenal-depot` `(hash-by :face/name)`, plain-map events (the rk F1
lesson). ONE stream topology: wear event → journal-dedup by wear-id →
events + journal + counts written in ONE task event (atomic); registered
event → `(|hash "faces")` hop → roster overwrite (idempotent by value, T17).
PStates: `$$faces-by-name` (POINTER rows only: name/object-key/import-key/
status/valid?/source-ref/registered-at-ms — T16/G20), `$$wear-events-by-face`
(append-only, subindexed, order-key = fixed-width(worn-at-ms):wear-id),
`$$wear-counts-by-face` (WearCountRow: count + last-worn-ms + last-wear-id),
`$$wear-journal-by-face` (wear-id → order-key). `record-wear!`: server stamps
`worn-at-ms`, WAL line FIRST (`data/face-wear-log.ednl`, pure-edn, UTF-8,
locked — the git_spine :582-655 precedent), then depot append `:ack`
(PState-visible on return = the deterministic barrier); duplicate wear-id =
journaled no-op. `replay-wear-log!`: verbatim re-append (stamps travel
untouched), depot-only (never re-WALs), per-line isolation. Named read fns
(the G21 surface): `read-face` `list-faces` `read-wear-count`
`read-wear-events` `read-wear-journal-entry`.

**Watcher** (`ingest_watchers.clj`): optional `:classify-fn` on
`start-ingest-watchers!` + `initial-sweep!` + `run-import!` 4-arity — default
= existing `classify`, ZERO change for existing callers (3-arity delegates).
`faces-classify`: `.edn`→`:assembly`, else nil — the assembly branch exists
ONLY through it (T19; `deps.edn` can never ingest as a face). `import-assembly!`
rides the EXISTING seam (append → await-decision); on EVERY accepted decision
(replays included — that is what converges the T17 gap): arsenal
`register-face!` (warn-and-continue on failure: honest wearable-but-unlisted
window) + `assert-envelope-edges!` (skip-and-report when no rk handle). Epoch
bump unchanged (run-import! owns it).

**Kernel branch** (`object_container.clj:307-321`): `imp:asm:` routes to the
object-key partition. NOT a literal `leading-object-key` mirror — `asm:<name>`
is two segments, and leading-object-key would truncate at `"asm"` (the exact
mis-route G18 kills); the branch takes the first two colon segments of the
remainder, inline, additive-only.

**Projections** (`face_projection.clj`): `:assembly` — name → deterministic
object-key (§17) → arsenal `read-face` (status/indexed?) + OC
`read-current-revision` over `oc:doc:asm:<name>` → serve-time verdict via the
SAME compiler (T18) → `{:assembly/source :assembly/valid? :assembly/errors
:assembly/name :assembly/status :assembly/indexed? :face/rendered-at-ms …}`.
Index-missing faces still serve (name→key is deterministic); the gap is named
(`:assembly/indexed? false`), never invented around. `:face-list` — roster +
per-face wear count + last-worn, from Rama only (T14). Both TOTAL (nil/corrupt
runtimes → error data-contexts); `serve` unchanged (§7 law holds; T15: no
write anywhere near it).

## Gate evidence (RUN in-context; lane suite 6 tests / 233 assertions, 0 fail / 0 err)

Suite: `clj -M:test -e "(require 'app.face-arsenal-test 'clojure.test) (clojure.test/run-tests 'app.face-arsenal-test)"`.
Final run AFTER lane E's two real faces landed — the G17 receipt enumerated
the REAL faces dir holding `outline.edn` + `boxes-face.edn` +
`minimap-reader-face.edn`, plus 3 synthetic fixtures (valid-with-provenance /
valid-plain / malformed) in a scratch root.

- **G17 (PASS, asserted against durable state):** initial-sweep over
  [real-dir, scratch]: every `.edn` → exactly one accepted import (family
  asserted on the materialized container kind `:assembly` + durable
  completion), one latest source version (`$$source-latest-by-ref` hash = file
  bytes), one derived verdict row (`valid?` honest per file, errors carried
  for the malformed), one `$$faces-by-name` entry (`valid?` never lies);
  epoch +1 per accepted import (asserted on the sweep delta). Identical
  re-save → convergent replay: no new revision, no new version, pointer
  converged. Edited save → exactly ONE new revision on the SAME object-key,
  latest = edited bytes, pointer refreshed to the new import-key. Live-save
  through the real watcher path (classify-fn + debounce + on-import latch —
  the sanctioned no-poll signal): accepted, kind `:assembly`, indexed.
- **G18 (PASS):** unit — `extract-object-key` on an `imp:asm:` key returns
  `asm:<name>`; partition alignment vs the raw object-key; both pre-fix
  failure shapes (whole-string fall-through; `"asm"` truncation) asserted
  dead; existing families (`imp:md:` `imp:clj:` `imp:tr:…:sb:` `:else`)
  unchanged. Durable — FOREIGN `read-import-completion` returns the row on
  the 4-task cluster, exercised per real+synthetic file inside the G17 loop.
- **G19 (PASS):** the provenance-carrying face asserted exactly 3 edges —
  `produced` (conversation→face), `based-on` (face→`oc:doc:asm:outline-face`,
  bare-name normalization), `supersedes` — each durably `:asserted` with
  asserter `"sid"`/`:human` (§17); author-absent case → system actor
  (`import:face-arsenal`/`:import`, pure). After THREE sweeps (initial +
  identical + edited-under-new-import-key) each edge's history holds EXACTLY
  one transition — re-imports duplicate nothing. Unregistered kind impossible
  by construction: junk envelope fields derive zero specs; every derivable
  kind ∈ `rk/relation-kinds` (asserted); no-provenance + malformed files
  derive zero specs.
- **G20 (PASS):** wear append server-stamped (bounds-checked against the test
  clock), ordered, counted; one WAL line per accepted wear (pure edn, line ==
  event); duplicate wear-id (re-stamped clock — the client-retry shape) =
  journaled no-op: count, log length, and journaled order-key all unchanged
  (physical journal read). Pointer row shape = EXACT key set (no material
  field, T16). Boot replay on a FRESH arsenal-only cluster: counts + stamps +
  event log reproduce EXACTLY; double replay adds nothing; torn WAL line
  counted + skipped without aborting the rest. Index honesty: the fresh
  cluster (OC populated, index empty) still SERVES the face
  (`:assembly/found? true`, name→key deterministic) with
  `:assembly/indexed? false` named in the data; `:face-list` returns the
  empty roster without error — the lack signalled, never invented.
- **G21 (PASS):** mechanical read-only scan — face_projection.clj carries no
  write/topology/raw-path token, and its read surface is EXACTLY
  `#{ocr/read-source ocr/read-current-revision}` (OC named query APIs) +
  `#{face-arsenal/read-face face-arsenal/list-faces
  face-arsenal/read-wear-count}` (the arsenal's own named read fns) — no
  PState paths outside the arsenal module. Totality: nil ctx, bad name,
  absent face, corrupt arsenal handle → error data-contexts, never a throw
  (the corrupt-handle case prints its honest error line in the run). Unit:
  request→data-context through `serve` over the real cluster (valid face
  serves source+status; malformed face serves and error-cards with the T18
  verdict; face-list carries names+status+valid?+wear-count+last-worn, with
  never-worn = honest zero).

**Non-regression (run):** `ingest_watchers_test` green ·
`face_assembly/face_primitives/clojure_adapter` 23t/287a green ·
`face_integration` 3t/26a green · `face_projection_test` 87/88 — the ONE fail
is `g13-serve-dispatch` pinning the registry to `[:conversation]`, a W1-era
assertion CONTRACT §16 itself supersedes (see INT flags; file out of my
fence). Not run here (G26's job): the full serial suite, block-distiller
real-corpus receipt, git-spine repo scan, lane E's transcription suite.

## Rama-pitfalls verdict (protocol run before code)

1 EVENT BOUNDARY: PASS — wear writes one event on hash(name); roster write its
own event post-hop; OC-accept→register dual append explicitly NOT atomic
(T17: idempotent + convergent). 2 SIDE-EFFECT RETRY: PASS — topology touches
nothing external; WAL written client-side pre-append. 3 OWNERSHIP: PASS — one
topology owns all four PStates. 4 BACK-ARROW: PASS — T14/T15 honored.
5 ID IDEMPOTENCE: PASS — wear-id client-minted; worn-at-ms stamped once into
the depot record; order-key deterministic from the record. 6 ACK: PASS —
`:ack` confirmation flows, tiny volume. 7 STREAM: PASS — journal handles
at-least-once AND client re-fires. 8 PROXY: N/A. 9 SUBINDEX: PASS — events +
journal inner maps. 10 HASH EXTRACTOR: PASS — plain maps, validated non-blank
`:face/name`. OVERALL: READY-TO-CODE (was), shipped as designed.

## Judgment calls (each revert-cheap, D-010 recorded)

1. **Import-key literal reading:** `imp:asm:` + object-key + `:` + sha ⇒
   `imp:asm:asm:<name>:<sha>` (the contract's template substituted verbatim;
   the double `asm` is the price of the literal form). The G18 branch strips
   the 8-char family prefix and takes two segments back.
2. **The branch is intent-mirrored, not text-mirrored:** a literal
   `leading-object-key` copy of the `imp:clj:` branch would truncate
   `asm:<name>` at `"asm"` and FAIL G18's own success criterion (colonless
   object-keys were the verified precondition of the older branches; `asm:`
   keys break it). Two-segment inline extraction, additive-only.
3. **Routing analysis drove the read surface** (worked in full, in the
   adapter header): for colon-carrying `asm:` object-keys the
   `src:/rev:/evt:/du:/sa:/ce:` id families MISROUTE on foreign reads
   (`leading-object-key` truncation), and `read-latest-source-by-ref`'s
   artifact hop + `read-unit` + `read-common-material-for-source` inherit the
   break. Product path uses only routing-correct surfaces: the decision/audit
   seam, `read-import-completion` (new branch), `oc:doc:asm:<name>` +
   `read-current-revision` (full-remainder branch; revision hop task-local),
   `$$source-latest-by-ref` (re-hashed to the colonless ref-key). Receipts
   read misrouting-shaped keys with `{:pkey object-key}` (test-only, the G12
   carve-out precedent).
4. **Identity-anchor container (the lane's one real design finding):** the
   G17 edited-save receipt initially REJECTED with
   `:native-identity/conflict` — `native-claim-compatible?` requires an
   unchanged container content-hash across imports (the kernel's exactness
   law; markdown never trips it because its object-keys are
   content-addressed, one object per save). Assemblies keep a stable identity
   by design (§17), so the face's document container is materialized as the
   IDENTITY ANCHOR: `current-content-text`/`current-content-hash` nil, the
   bytes riding the revision chain (`current-revision-id` flips per save;
   `read-current-revision` serves the bytes). Honest — a head pointer plus
   revisions, no content copy, no kernel edit, claims compatible forever.
5. **Name fallback:** unusable envelope name (missing / non-string / colon /
   whitespace) → file stem, so malformed files keep deterministic identity
   (R1) and colons can never enter the two-segment key space. Recorded
   consequence unchanged from §17: renaming a face mints a new object.
6. **`$$faces-by-name` shape:** constant top key `"faces"` → nested
   subindexed `{name → row}` (the `$$transcript-source-lines-by-file`
   precedented navigation) instead of `:global?` — same one-task roster,
   proven read idioms, no root-path reads anywhere near the 1.6.0 crash class.
7. **`WearCountRow`** (count + last-worn-ms + last-wear-id) instead of a bare
   Long — `:face-list` serves last-worn without scanning the event log.
8. **WAL ordering + scope:** write-ahead (line BEFORE append; an append
   failure after the line converges at replay — the reverse order could
   silently lose an acked wear); one line per `record-wear!` attempt
   INCLUDING duplicates (the journal is the truth guard; replay converges;
   the gate's "WAL line per accepted wear" is satisfied as ≥). Replay appends
   depot-only — never re-WALs (a re-WALing replay would double the log every
   boot).
9. **Wear/register acks `:ack`** — confirmation flows; PState visibility on
   return is the suite's deterministic barrier. The OC seam keeps its
   existing sanctioned latch (`await-object-container-decision`); rk keeps
   its own (`await-relation`).
10. **G17 live-save runs on the scratch root**, not the committed faces dir
    (mutating repo state mid-test); the real dir gets the sweep receipt; the
    full-dir receipt re-runs at G26 per the lane prompt.
11. **Edge assertion lives in the adapter ns** (pure `edge-specs` + effectful
    `assert-envelope-edges!` — the git-spine both-halves precedent);
    `register-face!` lives in the arsenal (owns its depot). Both fire from
    `import-assembly!` post-accept, each failure-isolated (warn + converge
    later), neither able to block the decision/epoch.
12. **Status honesty:** unknown/missing envelope status → `:candidate` in the
    index (the envelope's verbatim value stays in the source; the index never
    invents).

## Stop-clause escalations

None fired. (No new relation kind, no import-key family beyond `imp:asm:`,
G20 met without the arsenal holding material, no binding-doc conflict — the
native-claim finding was resolved WITHIN the seam by materialization shape,
not by kernel edit or contract change.)

## INT flags

1. **`test/app/face_projection_test.clj` `g13-serve-dispatch`** pins
   `(keys fp/projection-registry)` to `[:conversation]` — superseded by
   CONTRACT §16 (registry gains `:assembly` + `:face-list`). One-line fix at
   INT (file is lane C's W1 test, outside my fence):
   `(= #{:conversation :assembly :face-list} (set (keys fp/projection-registry)))`.
2. **`kernel.clj` KERNEL-SHAPE** asks new kernels to add `:examples` entries;
   `face-arsenal` (intent-only, #6) belongs there — kernel.clj is outside my
   fence.
3. **Named lack (D-005 ordered, form-break-gated):** no OC read API serves
   source/unit material for colon-carrying object-keys (`read-source`,
   `read-latest-source-by-ref`'s artifact hop, `read-unit`,
   `read-common-material-for-source` all misroute for `asm:` keys). The wear
   path doesn't need them (`read-current-revision` carries the bytes); a
   block-material-for-assemblies need would be the W2-eligible kernel work
   the G12 amendment already gestures at.
4. **W2-INT wiring notes:** the merged server runtime must carry the arsenal
   handle keys + `:face-wear-log-path`; boot order = arsenal launch →
   `replay-wear-log!` → faces watcher (`initial-sweep!` +
   `start-ingest-watchers!` with `:classify-fn watchers/faces-classify`, faces
   root only); `serve` ctx gains `:arsenal-rt`; `record-wear!` throws
   IllegalArgumentException on blank ids — the outbox mints the wear-id BEFORE
   calling (§16 write-path ruling), and the e/defn should catch-and-surface
   rather than let it reach the render path.
5. **`:face-list` cost shape:** roster read + one count point-read per face
   (N = tens). If faces multiply, a fold-side stats row is the desire-path
   move — evidence would live in the wearing log itself.
6. **WAL growth:** one line per wear attempt, duplicates included; replay
   converges. Compaction is a non-problem at dev volume; noted for a durable
   future.
