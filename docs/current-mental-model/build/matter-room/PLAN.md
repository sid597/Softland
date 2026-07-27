# matter-room — PLAN (P1–P4) · REWORKED after validation R1

Authored 2026-07-27 by Fable (staging session); REWORKED same day against
`PLAN_VALIDATION_R1.md` (verdict FAIL, 18 findings — every fix below cites
its finding as `[F<n>]`). Binding docs govern (CONTRACT.md as amended
2026-07-27 twice: plan-staging + R1); this plan is derived. Every symbol
cited was grep-verified in the staging session; line numbers are hints.

## Ground truth the plan stands on (all verified this session)

- The portal serve face passes `params` VERBATIM to `material-portal/open`
  (`face_projection.clj:1484`); `portal-briefing` (`face_projection.clj:
  1689`) destructures an EXPLICIT five-key map — `:master-id` must be
  plumbed there or it is silently dropped [F3].
- Master-tier loop: `object_container/facet_master.clj` —
  `import-candidate!` (:213), `activate!` (:252), `read-master` (:227).
  Instance-tier: `material_truth.clj` — `deviate!` (:51, instance branch
  needs `subject-uid`), `release-deviation!` (:58), `pin!` (:64),
  `unpin!` (:71). PREVIEW is client-only BY LAW ("the server never mints
  one", `material_truth.clj:236-240`); the lane is
  `window.__bindings.preview`/`.endPreview` (`ground.cljs:3243-3249`)
  [F2].
- Episode import builders hard-code the human actor
  (`utterance-actor`/`utterance-rows`, `episode.clj:124-156`) — P2
  parameterizes them (allowlisted, R1-finding-1 ruling) [F1].
- The server-side edit lane exists: `append-block-edit-request-durably!`
  (`object_container/runtime.clj:112`, WAL-first `:object/edit`) +
  `edit-request-validation-errors` (`object_container.clj:574`) + the
  edit-lineage machinery (`payload-edit-client-id`/`-seq`/`-lineage-key`,
  `object_container.clj:416-418`) — the resident-refresh path [F5].
- Import fingerprints are strict: same import key + different payload =
  `import-material-fingerprint-conflict-error`
  (`object_container.clj:559-565`) — residents must never re-import
  changed content [F5].
- Experience face: `(or (:conversation-address params) (some-> (first
  ids) oc/extract-object-key))` (`face_projection.clj:706-709`) — the
  address must be passed EXPLICITLY at a master anchor [F14].
- Gauge inputs sound (R1 CLEARED S7); `read-commits` spawns an untimed
  git subprocess (`git_spine.clj:75-88`) — on-demand only, face-wrapped
  timeout, SINGLE-FLIGHT with the leak bound stated in L7 (a face
  timeout abandons, never interrupts) [F9, R2-9]. Activation read
  repinned [R2-1]: the ONE shipped precedent —
  `read-revision-history` over `(active-pointer-container-id
  provenance-material/spec)` (`cluster.clj:533-539`), the SINGLE
  provenance master — `analyze-activation-history` demands exactly one
  tip (`material_circulation.clj:751-768`), so a multi-master
  concatenation is `:ambiguous` forever on clean data and would kill the
  §8 falsifier's detector; gauge v0 is honestly provenance-scoped
  (per-master gauges = the F4-residue LATER re-cut of L7). `repo-root`
  is private in cluster.clj — P4 derives
  `(System/getProperty "user.dir")` locally [F18].
- Room id: `java.util.UUID/nameUUIDFromBytes` (JDK MD5 v3, byte-stable);
  the UUID-shape validator is EXTERNAL (CLI `--session-id` + jsonl
  filename, `episode.clj:827-849`) — recorded per §9 [R1 CLEARED S4].
  Reverse mapping = in-code table over `facet-masters/master-ids` (seven)
  + the served `:portal/room` section [F12].
- Grammar refusal of matter rows is v2+-scoped: `valid-bindings?` (v1)
  never runs `site-can-feed?`; only `valid-bindings-strict?` does
  (`binding_material.cljc:229-246`); `fm:space` has no strict entry
  (`space_material.cljc:94-104`) — stated honestly in G7, residue
  recorded [F6].
- `default-material-policy-paths` contains none of this package's files —
  the package cannot self-trip the falsifier (R1 CLEARED S10).

## P1 — the matter address

**Files:** `material_portal.cljc` (pure master-identity + sentinel
helpers) · `material_portal.clj` (anchor mode in `open`) · NEW
`matter_room.cljc` (born HERE: `room-id` derivation + the in-code reverse
table — needed by P1's experience address [F14]; the derivation is
`#?(:clj …)` reader-conditional — `nameUUIDFromBytes` is JVM-only and
this `.cljc` will be required by the client for the narrowing law, so
the client NEVER derives, it reads the served `:portal/room` [R2-7]) ·
`face_wiring.cljs`
(one console verb `__portal.openMaster('fm:attention')`) · test tree.

**Behavior:**
- `open` accepts `:master-id` (an `fm:*` string). Anchor mode = master-id
  present; `:entity-id` absent. Entity mode BYTE-UNCHANGED (regression
  test fixes a fixed entity projection before/after).
- Identity: `found?` from the registry, kind `:facet-master`, facet,
  floor-master-id, and `:entity/id` = the master-id (load-bearing for the
  render title + briefing first sentence) [F17].
- Placement: `{:placement/applicable? false :placement/anchor
  :facet-master}`.
- BLAST: `:blast/basis :no-wearer-snapshot-at-anchor` at the section AND
  the override landed ON each per-master map inside `:blast/by-master`
  (`:blast/counted-over nil` + the basis key per master — the raw
  numbers are minted per master in `blast-radius`,
  `material_truth.clj:218`, so a section-level nil alone leaves every
  per-master map still saying 0) [F4, R2-8]; the `card-rows` `:blast`
  case gains a basis row so the honesty reaches the RENDERED card
  [R2-8]. WEARERS: basis names the same absence [F4].
- Every `*-here*` key (`:master/pinned-here?`, `:master/tier-here`,
  `:master/worn-here-revision-id`, `:deviations/here` …) carries the
  explicit `:not-applicable-at-anchor` sentinel — canonicalize-safe,
  present, honest [F13]. THE SENTINEL IS TRUTHY [R2-3]: `card-rows`'
  masters case (`material_portal.cljc:596` `(when (get v
  :master/pinned-here?) " · PINNED")`) becomes `true?`-guarded in P1
  (the file is on P1's list), and the test asserts the rendered anchor
  cards contain NO "PINNED" minted from a sentinel.
- Priced = `[master-id]`; masters/activation-history/recovery/history
  keyed as today, priced to the anchor; recipe = compositions among
  snapshot wearers wearing this master (basis honesty verbatim);
  bindings = this master's rows across sites; why without a stamp =
  `found? false`; chrome/lint/truncation/briefing-of as today.
- EXPERIENCE: `:material-ids [master-id]` + EXPLICIT
  `:conversation-address (episode/episode-object-key (matter-room/room-id
  master-id))` — never the master-id through `extract-object-key` [F14].
  (Room may be empty at P1 — an empty receipt read at the RIGHT address
  is honest; P2 populates it.)
- `:portal/room {room-id master-id}` served at anchor mode (the
  discoverable mapping's served half [F12]).
- Determinism: clock-free (T10); the SHIPPED form — in-JVM double-open
  byte identity + `*print-namespace-maps*` re-check, canonical sha pinned
  in the phase artifact [F16].

**Tests (IPC where OC is needed):** G1 teeth — registered anchor:
`:portal/errors` `[]`, identity `found?` true, `:entity/id` = master-id,
`:portal/masters` exactly the anchor; unknown anchor: total, `found?`
false, all questions answered [F7] · blast/wearers basis + sentinels
asserted [F4, F13] · experience `:relation-roundtrips 1` + explicit
address [F14] · entity-mode byte regression · determinism as shipped
[F16] · echo receipt (G10 machine half) [F8].

**Gates:** G1, G2, G10 machine half. **Falsifier aim:** anchor-mode
sections lying about basis (the confident-zero class) + entity-mode byte
drift.

## P2 — the room

**Files:** `matter_room.cljc` (resident composition: unit derivation +
content projection + edit-refresh composition — pure) ·
`src/app/server/episode.clj` (SCOPED [F1 + R2-2 + R2.5 fix 2]: actor
parameter on all FOUR involved builders — `utterance-import-request`
(threads the actor in), `utterance-rows`, `utterance-actor` (GAINS a
1-arity; the existing 0-arity remains and every existing caller keeps
its 0-arity call, byte-identical), AND `utterance-projection-hint`,
whose `role` slot (`episode.clj:207`) is the ONE field the render's
machine classification reads (`ground.cljs:1337` `machine? (not=
speaker "sid")` ← `face_projection.clj:127` ←
`block_distiller.clj:1385`); residents carry
`{:actor/id "softland:matter-room" :actor/type :machine}` with
part-type `:material` [R2-6] — the whole-block part (`free-cut-part`,
`block_distiller.clj:412-434`; `:text` would fan one resident into N
markdown blocks and break the one-unit edit lane) — with the KNOWN
consequence stated: `:material-part` is in `seed-noise-kinds`
(`face_projection.clj:1630`) so residents are elided from episode
seeds, INTENDED (the room resident's context rides the portal
briefing, never the conversational seed); the `episode_test.clj:139`
role pin (`(is (= "sid" (:role hint)))`) updates in the same commit) ·
`server_jetty.clj` (room-open endpoint) · `face_projection.clj`
(`:portal/room` plumb if not fully P1) · `face_wiring.cljs` +
`ground.cljs` (room entry row → the `?drill=<room-uuid>` lane —
DELIBERATE MVP: entry is a URL change + reload, not an in-land gesture;
recorded as the room's first felt cost / descent candidate [F15]) ·
test tree.

**Behavior — the resident lifecycle, pinned [F5]:**
- BIRTH ONCE: each resident has a deterministic identity-only turn-id
  `mr:<master-id>:<section>[:<revision-id>]` — NO content hash in the id.
  First open births residents via the PARAMETERIZED episode import path
  (unit + birth-position, one acked import, machine actor). Replayed
  birth (same id, same payload) converges; changed content NEVER
  re-imports (the fingerprint-conflict law).
- REFRESH VIA THE EDIT LANE [F5, R2-5]: head/bindings summary residents
  update through `:object/edit` requests
  (`append-block-edit-request-durably!`). Pinned NOW, not deferred:
  `:edit-lineage-key` = the UNIT-ID (constant per resident — a
  hash-derived lineage would void ordering and grow
  `$$edit-order-by-target` unboundedly); `:edit-client-id` =
  `"softland:matter-room"`; `:edit-seq` = STRICTLY MONOTONE, derived
  from a durable read of the unit's current edit row (`row-edit-seq`
  precedent; fallback 0 → first refresh 1) — NEVER a content hash:
  `stale-edit?` (`object_container.clj:1398-1405`) rejects `(<= seq
  last-seq)` in `edit-effects`, OUTSIDE `edit-request-validation-errors`,
  so a hash-derived seq silently stops ~half of refreshes. The edit
  actor mints `:object/edit` capability (required by
  `authorized-request?`). Same-content replay at the same seq CONVERGES
  as a journaled no-op — the identical idempotency key short-circuits
  `stale-edit?` (its conjunct `(not= idempotency-key …)`), so no
  `:edit/stale` rejection ever fires for a true replay [R2.5 fix 4 —
  do not write a test expecting one]. Gate: two refreshes in the
  "wrong" content-hash order both land.
- APPEND-ONLY TRAIL: one resident per pointer-revision; a NEW revision
  births exactly one NEW resident (its id carries the revision-id) —
  no edits needed, historically honest.
- Sid's blocks in the room are ordinary blocks; positions/camera settle
  as truth; every first-light verb A BLOCK'S CLASS CARRIES rides
  unchanged — machine residents the machine-block verb set, Sid's room
  blocks the user set (L3 + G4 as amended, site-matched [R2-10]).
- Room id served (`:portal/room`); reverse lookup = the in-code table
  [F12].
- EXPERIENCE WIDENS [F14 second half]: from P2 on, the anchor's
  experience `:material-ids` = `[master-id ∪ the room's resident
  unit-ids]` (L6's "and the room's residents"), still one batched
  relation read; test asserts a mark on a resident surfaces in the
  master's experience.

**Tests:** N opens → one row set (PState-level reader) · **activate a
new revision → re-open → exactly ONE head resident (content updated via
edit lane, not duplicated) + exactly one NEW trail resident** [F5] ·
unit-id stability across runs · zero `extract-object-key` changes + zero
new adapter namespaces + zero new import-request builders outside the
parameterized episode builders (G5 as amended [F1]) · the episode
parameterization's default path byte-identical for existing callers
(regression) · the served turn's `:speaker` for a resident = the room
actor, never `"sid"` (G5's classification proof [R2-2]) · two refreshes
in wrong content-hash order both land (the monotone-seq law [R2-5]) ·
floor drill over room residents SITE-MATCHED — machine residents
against ground machine blocks, Sid's room blocks against ground user
blocks (G4 as re-cut [R2-10]) · room-id UUID-shape recorded vs the
external validator · echo receipt (G10).

**Gates:** G3 (one-shot live half per contract), G4, G5, G10 machine
half. **Falsifier aim:** the duplicate-resident/fingerprint-conflict
class [F5] + a bespoke import composer hiding in `matter_room.cljc`
[F1].

## P3 — hands and mouth

**Files:** `verb_registry.cljc` (four entries) · `matter_room.cljc` (act
request builders + the master-anchored narrowing law) ·
`server_jetty.clj` (act endpoints + the P8 room branch) ·
`face_projection.clj` (`portal-briefing` gains `:master-id` plumbing —
the explicit five-key map is where the anchor currently dies [F3]) ·
`face_wiring.cljs` (console verbs) · `ground.cljs` (preview-lane touch
only if needed — disclosed) · test tree.

**Behavior:**
- Registry: `:matter/deviate` `:matter/activate` `:matter/rollback` =
  `:durable-via-request`; `:matter/preview` = `:pure-projection` (the
  honest class — the server never mints a preview [F2]). NO release
  chains (§5 as amended [R2-11]: these are extractions of built
  machinery; `:verb/extracted-from` is the honesty field; the P8 chain
  stays reserved for new capability). All four: `:invoke`,
  `:verb/extracted-from` naming the exact existing fns/lanes; docstrings
  state: REGISTRY-DECLARED, GRAMMAR-UNBINDABLE — required-args
  `#{:master-id}` (deviate: `#{:master-id :subject-uid}`, what
  `deviate!` cannot run without [R2-11]) — no site supplies them; v2+
  grammars refuse structurally, v1 grammars would accept-then-no-op —
  honest scope per G7 [F6]; INVOKED through the named act lane [F11].
  `declaration-rows` will publish `:verb/bindable? true` for all four —
  true in grammar terms, unfeedable in site terms; the served
  `required-args` is what makes that legible (G7's honesty clause).
  PINNED-SURFACE DISCLOSURE [R2-4]: `binding_dispatch_test.clj:153-158`
  pins the `:durable-via-request` set exactly — the pin updates in the
  same commit, its label re-cut to name BOTH durable lanes (settle ·
  act).
- Act lane (named, disclosed): jetty endpoints + console verbs →
  `matter_room.cljc` builders → EXISTING fns only: deviate →
  `material_truth/deviate!` (instance; builder carries `subject-uid`
  [F11]) | `facet-master/import-candidate!` (master candidate) ·
  activate → `facet-master/activate!` after `activation-event` grammar
  validation (malformed → error card, worn surface unharmed) · rollback
  → `activate!` at a `:portal/recovery` offer's to-revision-id · preview
  → the CLIENT lane (`__bindings.preview`/`.endPreview`), proven to
  write nothing (G7's byte-identical before/during/after) [F2].
- Ctrl+Enter in a room: the jetty P8 path branches when the turn's
  conversation-id IS a room id (in-code reverse table — server
  authoritative; a forged id can only name a DIFFERENT room, never
  widen past one master): `portal-briefing` called with
  `{:master-id …}` [F3]. G6: bytes identical human/resident AND the
  briefing contains the anchor's master-identity section (the positive
  assertion — byte-equality alone is blind [F3]).

**Gates:** G6, G7, G10 machine half [F8]. **Falsifier aim:** new WRITE
path + undisclosed invocation endpoints [F11] + widening attempts on the
narrowing law.

## P4 — citizens and gauges

**Files:** `face_projection.clj` (two serve faces: `:cascade-rows` —
row source INJECTABLE, defaulting to `cascade/rows`, labeled
`:code-owned :in-process` [F10]; `:escape-gauge` — ON-DEMAND ONLY [F9]:
`read-commits` (repo root = `(System/getProperty "user.dir")` derived
locally [F18]) × `read-revision-history` over
`(active-pointer-container-id provenance-material/spec)` — the SINGLE
provenance master, repinned [R2-1]: `analyze-activation-history` demands
exactly one tip, a multi-master concatenation is `:ambiguous` forever ×
`default-material-policy-paths` → `terminal-escape-report`; the git call
timeout-wrapped AT THE FACE, poisoned-total on expiry, SINGLE-FLIGHT
with cached last report [R2-9]) ·
`material_portal.clj` (anchor-mode `:portal/cascade` section +
truncation entries; the gauge is NOT embedded in the standard open —
the portal links the on-demand face [F9]; the seventeen-question card
floor is NOT widened) · `matter_room.cljc` (the gauge resident holds
the LAST computed report, refreshed on demand via the P2 edit lane) ·
test tree.

**Tests:** cascade rows served read-only + FIXTURE-INJECTED dark row
present and inert + zero `app.server.cascade` diff [F10] · gauge
`:measured` on a clean IPC fixture · `:ambiguous` rendered honestly on
a regression fixture · timeout path yields a poisoned-total card ·
**the standard portal open path runs no git (asserted)** [F9] ·
truncation entries present · echo receipt (G10).

**Gates:** G8, G9, G10 machine half [F8]. **Falsifier aim:** gauge
honesty (inference or inline git sneaking in) + the `:code-owned` label
lying.

## Cross-phase duties

- One phase per fresh context; gates green in-context first run (a
  non-green first run is signal, recorded in NOW).
- G10's machine half rides EVERY serve-widening phase (P1–P4) [F8];
  Sid's headed half completes at wear, non-blocking.
- Phase artifacts: diff-derived changed-file lists, allowlist
  classification (episode.clj + ground.cljs edits DISCLOSED per the
  amended §11), ≤15-line NOW entries.
- Pinned-enumeration scans grepped for every edited file's path before
  suite selection is called done (machine-cut rule) — episode.clj,
  ground.cljs, AND verb_registry.cljc especially; two pins already
  known: `episode_test.clj:139` (role "sid") and
  `binding_dispatch_test.clj:153-158` (the `:durable-via-request`
  exact set) — both update in their phase's commit [R2-2, R2-4].
- Suites reading git HEAD dynamically re-run after the closing commits.
- Echo receipts banked at capture time; the standing 52ms bar.
- Verification duties at phase start: P2 re-verifies the episode
  builders + `edit-request-validation-errors` shapes on disk; P3 re-pins
  `portal-briefing`'s param map and the act fn signatures on disk.
- Residue carried (not done here): absolute grammar refusal of matter
  rows needs a v2 strict entry on `fm:space` [F6]; room entry as an
  in-land gesture (vs the drill-URL MVP) [F15]; per-master escape
  gauges = a re-cut of L7, never a pin (gauge v0 is provenance-scoped)
  [R2-1]; the scoped `git_spine.clj` timeout edit if the abandoned-call
  leak is ever felt [R2-9].
