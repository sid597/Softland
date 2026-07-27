# matter-room — the engine's ontology served as inhabitable material

**Status: STAGED 2026-07-27** · Fable-authored in the direction session that
settled the shape (Sid: *"Hellyyyy yeaaahhhh letsss build it man this is what
is stopping me right now"* · *"i agree on the first part and that is the
initial design needed"* — verbatim in `vision/LOG.md` 2026-07-27). "matter-room"
is a WORKING NAME (DIRECTION §Only-Sid: names finalize by recurrence, Sid
names; the land/matter/engine triple is this session's candidate vocabulary,
unratified).

Parent frames, binding and unrepeated here: `build/editable-material/
DIRECTION.md` (the settled spine — identities, paradigm rulings, aliveness
laws) · decisions.md "How engine work lands" (two dials, two ceremonies,
boundary test) · the work-package skill (process). This contract EXTENDS the
DIRECTION; any clash is a stop-clause event, never a silent pick.

## §1 Purpose

Sid lives at the matter zoom level: open a TYPE (a facet-master), stand in its
room, see everything about it — revisions, wearers, bindings, accumulated
record, gauges — talk to a resident with the type itself as shared referent,
and shape it (deviate → preview → activate → reverse) without leaving the
land. First-light primitives capture thoughts; they cannot talk about the
entities themselves (Sid, 2026-07-27). The room is where that talk lives.

Three settled laws exist but do not yet meet, and the room is their meeting:

- **The portal** (P7): deterministic, total, batched projection — but anchored
  on one entity pick, rendered as a read-only card floor, and its own briefing
  says "This portal is read-only."
- **The truth loop** (P6): deviation → candidate → preview → scoped activation
  → announced change → reversal — built, gated, driven from the console lane.
- **The strange loop** (DIRECTION §The layer): "the portal is itself an
  instance of Softland space with its own material in the layer it opens" —
  declared; the current face is cards, not a space.

DIRECTION's precondition — "Four gates before the portal edits itself" — is
MET (Gates 1–4 all closed, board 2026-07-25/26). This package spends that
earned permission.

## §2 Consumers, in order

1. **Sid** — matter-shaping at the type, in-land.
2. **The room resident** — the LLM summoned inside a type's room, briefed by
   the same projection (the P8 pattern, one floor down).
3. **The engine-room (future)** — the next floor's spec accretes from this
   room's recorded descents; explicitly OUT of scope (§4).

## §3 The laws this package builds

Numbered for phase reference. Each names its existing substrate — this
package is policy/serve/material work over built machinery; a parallel
artery anywhere is a second-wearer FAIL tell (DIRECTION §settled spine).

- **L1 Citizenship.** Every noun the engine reads AS DATA becomes visitable
  material: facet-masters (served — exists), the space-type (`fm:space` —
  exists), binding rows + interaction table (served — exists), verb registry
  declarations (served — exists), **cascade rows (NEW: read-only projection
  of `app.server.cascade/rows`)**, recipes-as-exhaust (served — exists).
  Machinery is NOT a citizen: dispatch internals, circulation, replay,
  `react!` stay under the floor. Cascade rows serve labeled `:code-owned
  :in-process` — R1's own law (T2/T5: neither rows nor triggers are durable
  truth) rides into the label; when R2 lands its durable table the
  projection's SOURCE swaps and the label follows, zero contract change.
- **L2 Type-anchored opening.** The portal opens AT a master
  (`:master-id` anchor), not only at an entity pick — Sid's ruling verbatim
  (LOG 07-23): "when i ask for the type we open up the portal to a different
  layer, the component type, and this component type layer has the
  accumulated record… it is deterministic opening." Same seventeen questions,
  same totality (describe-never-gate: an unknown master-id still projects),
  same canonical bytes. Basis honesty carried: sections state their basis and
  truncation exactly as the entity-anchored portal does today
  (`:recipe/basis`, blast subjects, experience query-plan).
- **L3 The room is a space.** A master's room is a REAL conversation
  container: residents are ordinary durable blocks — the projection lands as
  machine material (idempotent import), Sid's blocks land as his blocks.
  Consequences, which are the point: every first-light verb A RESIDENT'S
  BLOCK CLASS carries works in the room UNCHANGED (machine residents carry
  the machine-block verb set; Sid's room blocks the user set — R2 finding
  10's honesty scope); threads anchor to residents durably; camera +
  positions settle as truth.
  The room's container id is DETERMINISTIC and UUID-SHAPED, derived from the
  master id, with a discoverable mapping (T6). Opening twice duplicates
  nothing (T2).
- **L4 Referent.** Ctrl+Enter in the room briefs the resident with the TYPE's
  portal — the accumulated record included — via the same both-sides
  narrowing law P8 built for blocks (`reply-to-block/narrowed-portal-open`):
  a forged or stale client anchor cannot widen the server-side briefing.
  Human and resident read the same projection, byte for byte.
- **L5 Ceremony.** ALL matter edits ride the EXISTING truth loop. THREE
  durable acts — deviate, activate (scoped), rollback (activate previous) —
  become registry verbs (`:durable-via-request`) dispatching into P6's
  request→decision→event machinery (`material_truth.clj` instance acts;
  `facet_master.clj` `import-candidate!`/`activate!`). **Preview is
  `:pure-projection`** — the server never mints a preview by P6's own law
  ("a preview is by definition something that did not happen",
  `material_truth.clj:236-240`); the preview verb names the existing
  client preview lane and is proven to write NOTHING. *(Amended
  2026-07-27, R1 finding 2.)* The four verbs are registry-declared but
  GRAMMAR-UNBINDABLE by construction (required-args no site supplies);
  their invocation lane is the named act lane (jetty endpoints + console),
  disclosed in the registry docstrings — the registry entry is the
  effect-class home, not a gesture claim. *(R1 finding 11.)* No new write
  path, no new edit machinery. The portal briefing's "read-only" sentence
  updates to name the verbs and their effect classes — permission stays
  with the artery, never the prose.
- **L6 Record.** "Show me everything people have experienced around this
  type" is the room's standing answer: `experience-around-many` fed the
  master-id (and the room's residents) — ONE batched relation read, per its
  own no-second-artery law. Threads and marks born in the room land as edges
  that travel both ways (DIRECTION §Records).
- **L7 The gauge on the wall.** `terminal-escape-report` (built; log-derived
  commits × activation events × `default-material-policy-paths`) serves into
  the room: status, count, candidates. The F4 residue (`:ambiguous` on the
  durable cluster until per-master/causal-tip keying; ruled LATER at P8)
  renders HONESTLY as ambiguous — this package surfaces the gauge and does
  not re-key it. Descents are detected, never declared. **The gauge is
  ON-DEMAND, never inline:** `read-commits` spawns a git subprocess with
  unbounded output and no timeout (`git_spine.clj:75-88`), so the gauge
  face is an explicitly on-demand serve (timeout-wrapped AT THE FACE,
  poisoned-total on expiry — no `git_spine.clj` edit) whose LAST computed
  report is held in the room's gauge resident; the standard portal open
  path never runs git. *(Amended 2026-07-27, R1 finding 9.)* **Leak
  bound stated (R2 finding 9):** a face-level timeout abandons, not
  interrupts — the blocked thread + git process run to natural
  completion; bounded by SINGLE-FLIGHT (at most one gauge computation in
  flight; concurrent and post-expiry asks get the cached last report).
  The scoped `git_spine.clj` `.waitFor(timeout)` + `destroyForcibly`
  edit is the named LATER escalation if the leak is ever felt.

## §4 Non-goals — each a named extension point, not a void

- **No new sites, gestures, or modifiers.** The binding grammar stays closed
  (`binding-material/sites`, `legal-gestures`, `modifier-keys` untouched).
  Room residents are ordinary blocks, so existing sites suffice. Pressure
  here = stop clause; widening is a kernel change + grammar version.
- **No recipes-as-revisioned-objects.** `:why/recipe-revision-id` stays nil
  (horizon item, DIRECTION §Tempo).
- **No material kernel module.** If a truth minted here is HOMELESS (no
  existing owner among OC / relation kernel / activation events), STOP —
  that fork is the DIRECTION's named horizon decision, Sid/Fable rules.
- **No F4 fix** (escape-gauge linearity keying — LATER per the P8 ruling).
- **No cascade R2 work, no new cascade rows.** Dark-lane #1's activation
  trigger (the first NEW row) belongs to episode-retry; this package reads
  `cascade/rows` and edits NOTHING in `app.server.cascade`.
- **No semantic-kinds work** (masters ≠ kinds stays absolute).
- **No naming.** Sid names at recurrence; every name here is scaffolding.
- **No engine-room.** The next floor down (contracts/gates/receipts as
  residents; the inviolate-kernel question) is recorded direction, out of
  scope; its spec accretes from THIS room's descent record.

## §5 Placement ruling

- **Projection widening** in the existing portal pair
  (`src/app/shared/material_portal.cljc` + `src/app/server/rama/
  material_portal.clj`) — the type anchor is the same projection asked at a
  different address; a second portal namespace would be a parallel artery
  (FAIL tell). Reversal cost: additive params + sections, revert by commit.
- **Room containers** ride the existing conversation/object-container
  machinery — no new Rama module, no new PState, no sixth deploy. The room
  is CONTENT, not schema. Reversal: rooms are containers like any other;
  abandoning the pattern strands no truth. *(Plan-staging ruling
  2026-07-27: resident birth uses the episode import path — no new import
  family, no `extract-object-key` branch; see §6 P2 + G5.)*
- **Matter verbs** in `src/app/shared/verb_registry.cljc` and a new pure
  seam namespace (`src/app/shared/matter_room.cljc`, name-scaffolding)
  modeled on `reply_to_block.cljc`. NO release chains (amended
  2026-07-27, R2 finding 11): release chains are birth certificates for
  NEW capability born from a wish; these verbs are EXTRACTIONS of
  built-and-gated P6 machinery, and `:verb/extracted-from` is the
  registry's honesty field for exactly that — the P8 chain shape stays
  reserved for genuinely new capability. Reversal: registry entries are
  data; the seam is one file.
- **NOT the dark lane.** This is rim/policy work merging by ACTIVATION
  (contact-gated), not a dark organ: no new module, no dark interval. The
  dark lane's #2 slot stays held; the board notes this package as the
  lived-friction pull the slot's comment anticipated.
- **Boundary test:** no event-vocabulary, cell-shape, or serve-contract
  BREAKS — serve gains additive faces/params only; new durable content rides
  existing write paths. Anything cutover-class appearing = stop clause.

## §6 Phases

One phase per fresh context (skill default); each runs its gates green
in-context; one fresh falsifier per phase aimed at the genuinely-new
machinery (sizing rule). Phase artifacts in this directory; changed-file
lists diff-derived at artifact time.

- **P1 — the matter address.** `portal/open` accepts a master anchor:
  `:master-id` (with `:entity-id` absent), master-identity section (spec
  facet, floor id, revisions, active/latest), all seventeen questions
  answered at the anchor, experience fed `[master-id]`+basis ids, unknown
  master drill, determinism in the shipped in-JVM form (G1 as amended —
  §6 swept per R2 finding 12), console + JVM calls documented in the card
  face. Deliverables: projection widening + suite + drill receipt.
  Gates: G1, G2, G10 machine half.
- **P2 — the room.** Deterministic UUID-shaped room container per master
  (T6) + idempotent birth of the room's machine residents **through the
  EXISTING episode import path** (the birth-import law, `episode.clj`
  :628-633: unit + birth-position, one acked import; machine
  classification rides the §11 render chain — the `role` slot, NOT the
  distilled-reply lane, whose actor resolves via `resolve-actor` in the
  river and does not transfer here) + entry (the portal face gains a room
  row; navigation rides the existing UUID conversation lane) + the floor
  drill run over room residents. *(Amended 2026-07-27 at plan staging: the
  originally pre-registered NEW import prefix is NOT needed — a new family
  would be a parallel import artery, the exact second-wearer FAIL tell; G5
  re-cut accordingly.)* Deliverables: room mint + resident birth + client
  entry + suite. Gates: G3, G4, G5, G10 machine half.
- **P3 — hands and mouth.** The four matter verbs registered
  (`:matter/deviate`, `:matter/preview`, `:matter/activate`,
  `:matter/rollback` — scaffolding names; NO release chains, §5 as
  amended per R2 finding 11 — `:verb/extracted-from` is the honesty
  field for extractions of built machinery), the three durable ones
  dispatching into the EXISTING P6 machinery, preview riding the client
  lane (L5 as amended; a new write path = FAIL); Ctrl+Enter in the room
  = master-anchored narrowed briefing (L4) via a `matter_room.cljc` seam
  modeled on `reply_to_block.cljc`; briefing read-only sentence updated
  (L5). Gates: G6, G7, G10 machine half.
- **P4 — citizens and gauges.** Cascade rows served read-only
  (`:code-owned :in-process` label, source-swap note); terminal-escape
  gauge served ON-DEMAND with honest status (L7/G9 as amended — never
  inline in the portal open); truncation entries for every new
  section. Gates: G8, G9, G10 machine half.

**Gate partition sum-check (rule of 2026-07-26; restated per R2 finding
12):** P1{G1,G2,G10m} ∪ P2{G3,G4,G5,G10m} ∪ P3{G6,G7,G10m} ∪
P4{G8,G9,G10m} = {G1..G10} ✓ — no gate unassigned; G10's machine half
rides every serve-widening phase by §8's own carve-out, Sid's headed
half completes at wear; owners named per gate below.

## §7 Traps ledger — cite by number in code comments

- **T1 gallery.** Naive: render the room as ephemeral projection nodes.
  Failure: no anchored record, no threads, every verb needs a parallel
  implementation — three second-wearer FAIL tells at once (parallel serve
  artery · duplicated identity machinery · different activation concept).
  Ruling: residents are durable blocks in a real container (L3).
- **T2 duplicate residents.** Naive: import projection blocks with fresh ids
  per open. Failure: every open duplicates the room (at-least-once replay
  law violated). Ruling: deterministic projection-unit ids derived from
  (master-id, section, item identity); re-open converges; the suite asserts
  N opens → one row set via a PState-level reader (negative invariants need
  a physical reader).
- **T3 second truth artery for the record.** Naive: scan containers for
  record items. Failure: `experience-around-many`'s own docstring law —
  callers supply records from ONE conversation projection read; a scan is
  the second artery. Ruling: feed the batched helper; assert
  `:relation-roundtrips 1` in the gate.
- **T4 grammar widening.** Naive: mint room-specific claim sites. Failure:
  the site enum is closed; a new site is a kernel change + grammar version —
  and unnecessary, because residents are ordinary blocks. Ruling: existing
  sites only; pressure = stop.
- **T5 cascade touch.** Naive: serve cascade rows by making them durable or
  adding a row for the room. Failure: R1's T2/T5 (declarations are inert
  data) violated AND dark-lane #1's activation trigger (first NEW row =
  episode-retry) stolen. Ruling: read-only projection of `cascade/rows`,
  zero edits under `app.server.cascade`, label carries the in-process
  source.
- **T6 room id vs the CLI spawn.** Naive: room container id =
  `"room:fm:attention"`. Failure: non-UUID ids cannot spawn the resident CLI
  (board-recorded drill-lane constraint; verify against `episode.clj`'s
  minted-uuid law at plan time). Ruling: deterministic UUID-SHAPED id
  derived from the master id — `java.util.UUID/nameUUIDFromBytes`
  (JDK-specified MD5 v3, byte-stable across JVMs), pinned in the phase
  artifact. **Discoverability substitute (amended 2026-07-27, R1 finding
  12):** the master→room mapping is an IN-CODE reverse table derived over
  `facet-masters/master-ids` (finite, seven today) + the served
  `:portal/room` section — no durable mapping row; zero durable state
  where a derivation suffices. The UUID-shape law's validator is EXTERNAL
  (the CLI: `summon-argv --session-id`, the jsonl filename) — recorded
  per §9's stop clause; no in-code check exists to cite.
- **T7 briefing widening knob.** Naive: accept client-supplied master/wearer
  sets into the room briefing. Failure: P8's exact defense exists because a
  forged `:entity-id` widened nothing — the narrowing law runs both sides.
  Ruling: the master anchor is authoritative server-side; `:narrowed? true`
  semantics preserved (an honestly empty set stays empty).
- **T8 escape gauge re-derived.** Naive: compute escapes from filesystem
  mtimes or current values. Failure: the report's own law — log-derived
  commit rows × activation events only. Ruling: serve the built report;
  ambiguous renders as ambiguous (F4 carried, not fixed).
- **T9 read-only prose outliving the artery.** Naive: leave the portal
  briefing's "read-only" sentence after verbs exist. Failure: the map lies
  — a resident told it cannot edit will not use its lawful hands. Ruling:
  the sentence names the four verbs + effect classes the moment G7 passes,
  same phase, same commit.
- **T10 clock in the projection.** Naive: stamp the room's projection with
  wall-clock. Failure: P7's determinism gates byte-compare across JVMs and
  reruns (`without-clock` exists for exactly this). Ruling: new sections are
  clock-free; time riding any row is claimed-time from durable truth, never
  serve-time.

## §8 Acceptance gates

Tier per the 2026-07-27 cadence ruling: SLIM default (this is
additive-and-dark-free rim work; no durable-touch cutover). Every gate names
its owner; machine gates are executable as IPC tests unless marked live.

- **G1 (P1, machine — re-cut 2026-07-27, R1 findings 7 + 16).**
  Master-anchored open with TEETH (`unanswered` = `[]` alone is a
  tautology — every section is fallback-total): at a registered master
  anchor, `:portal/errors` = `[]`, `:portal/identity` `:entity/found?`
  true AND `:entity/id` = the master-id, `:portal/masters` contains
  exactly the anchored master; at an unknown master-id, still total,
  `found?` false, every question answered. Determinism in the SHIPPED
  form: in-JVM double-open byte identity + the `*print-namespace-maps*`
  re-check, with the canonical sha pinned in the phase artifact (a
  two-JVM harness does not exist and is not owed).
- **G2 (P1, machine — re-cut 2026-07-27, R1 findings 4 + 13 + 14).** Basis
  honesty, named per section: BLAST at an anchor open with no wearer
  snapshot declares `:blast/basis :no-wearer-snapshot-at-anchor` and
  `:blast/counted-over nil` — NEVER a confident zero; WEARERS names the
  same absence in its basis; every `*-here*` key (`:master/pinned-here?`,
  `:deviations/here`, tier-here …) carries the explicit
  `:not-applicable-at-anchor` sentinel — "nothing deviates here" is not
  the same answer as "there is no here"; experience at the anchor reports
  `:relation-roundtrips 1`, `:batched? true`, and carries an EXPLICIT
  `:conversation-address` (the room's, once derivable) — never the
  master-id fed through `extract-object-key`; from P2 on,
  `:material-ids` includes the room's resident unit-ids beside the
  master-id (L6's second half — R2 resolution of R1 finding 14).
  **Rendered-surface clauses (R2 findings 3 + 8):** the sentinel is
  truthiness-proof — the anchor's rendered cards contain NO "PINNED"
  minted from a sentinel (`card-rows` reads become `true?`-guarded in
  P1); blast's override lands ON each per-master map
  (`:blast/counted-over nil` + basis inside `:blast/by-master`), and the
  rendered blast card carries the basis row — asserted on the CARD, not
  only the data.
- **G3 (P2, machine + one-shot live).** Deterministic idempotent room: in
  suite (IPC), N opens → one container, stable resident ids, zero
  duplicates asserted via PState-level read. LIVE: the FIRST open on the
  durable cluster is ONE-SHOT (the no-room pre-state dies with it) — the
  committed harness asserts the post-package invariant (re-open
  convergence), never the dead pre-state.
- **G4 (P2, machine + Sid's felt half at wear — re-cut 2026-07-27, R2
  finding 10).** Carried verbs, SITE-MATCHED: the floor drill resolves
  over room residents identically to ground MACHINE blocks (machine
  hit-area verbs: tap→release, drag, fold, machine selection) and over
  Sid's own room blocks identically to ground user blocks; camera
  reservation holds in the room (`camera-gesture-reserved?` untouched).
  "Identically to ground blocks" unqualified was false by construction —
  user and machine sites carry different verb sets. Sid's felt half
  completes at his wear, non-blocking, recorded.
- **G5 (P2, machine — amended 2026-07-27 at plan staging).** NO new import
  family: residents ride the existing episode import path with a machine
  actor and deterministic ids. Executable form: the phase diff contains
  ZERO changes to `extract-object-key` and ZERO new adapter namespaces;
  the fresh falsifier hunts a parallel import artery; the original
  foreign-read concern (the `imp:clj:`/`imp:sense-block:` `:else` class)
  is therefore structurally absent — recorded here so the refusal has a
  name. **Executable form extended (2026-07-27, R1 finding 1):** the grep
  also hunts NEW request builders targeting
  `:object-container/import-material` outside the parameterized episode
  builders — a bespoke import composer in `matter_room.cljc` would be a
  second artery in substance while passing the narrower grep. **And the
  classification proof (R2 finding 2):** the served turn's `:speaker` for
  a room resident is the room actor, never `"sid"` — asserted, since
  actor-envelope fields alone do not reach the render path.
- **G6 (P3, machine — strengthened 2026-07-27, R1 finding 3).** Referent
  law: master-anchored briefing byte-identical human/resident AND a
  POSITIVE content assertion — the briefing bytes at a room turn contain
  the anchor's master-identity section (byte-equality alone is blind: two
  identically-degraded entity-mode briefings would pass it); forged client
  anchor cannot widen — the room conversation-id is the server-side
  authority (both-sides test, the P8 shape).
- **G7 (P3, machine — re-cut 2026-07-27, R1 findings 2 + 6 + 11).** The
  THREE durable verbs end in accepted events through the EXISTING
  machinery (`deviate!`/`import-candidate!`/`activate!`); PREVIEW is
  proven to write nothing — served material byte-identical
  before/during/after (the GATE_P6 G3 shape); the falsifier greps the
  diff for any new WRITE path AND discloses every new act/invocation
  endpoint (a new lane is legal only named); the served active bindings
  contain NO row naming a matter verb, and the refusal's honest scope is
  stated: v2+ strict grammars refuse arg-starved matter rows
  structurally, v1 grammars accept-then-no-op (absolute refusal would
  need a v2 entry on `fm:space` — outside this package, recorded
  residue); malformed candidate → error card, worn surface unharmed;
  effect classes declared; floor rows untouched; T9's sentence updated in
  the same commit. **Declaration honesty (R2 finding 11):**
  `:matter/deviate` declares `#{:master-id :subject-uid}` (what
  `deviate!` cannot run without), the others `#{:master-id}`;
  `declaration-rows` will publish `:verb/bindable? true` for all four —
  true in grammar terms and unfeedable in site terms — and the served
  declaration carries `required-args`, which is what makes the
  unbindability legible to a reader; the `binding_dispatch_test.clj`
  exact-set pin on `:durable-via-request` verbs updates in the same
  commit with its label re-cut to name BOTH durable lanes (settle lane ·
  act lane).
- **G8 (P4, machine — re-cut 2026-07-27, R1 finding 10).** Cascade
  citizenship: rows served read-only with the `:code-owned :in-process`
  label; `git diff --name-only` shows zero changes under
  `src/app/server/cascade.clj`; the `:cascade-rows` face takes its row
  source as an INJECTABLE argument defaulting to `cascade/rows`, and the
  dark-row inertness proof runs on a FIXTURE-INJECTED row in the test —
  never a row present in the real serve (the two original clauses were
  contradictory: the real table has no injection seam and may not gain
  one here).
- **G9 (P4, machine — re-cut 2026-07-27, R1 findings 9 + 18).** The gauge
  serves ON-DEMAND (L7): measured status on IPC fixtures; `:ambiguous`
  rendered honestly on ambiguous history; policy-paths listed; no
  filesystem/mtime inference; the face's git call is timeout-wrapped and
  poisoned-total on expiry (single-flight, R2 finding 9); the
  activation-event read is PINNED to the ONE shipped precedent —
  `read-revision-history` over `(active-pointer-container-id
  provenance-material/spec)` (cluster.clj:533-539), the SINGLE provenance
  master — because `analyze-activation-history` demands exactly one tip
  and a multi-master concatenation is `:ambiguous` FOREVER on clean data,
  which would kill the §8 falsifier's detector (R2 finding 1; gauge v0 is
  honestly provenance-scoped — per-master gauges are the F4-residue LATER
  item, a re-cut of L7, never a pin); the standard portal open path runs
  no git (asserted by the suite).
- **G10 (P2 + every serve-widening phase, machine half + Sid's headed
  half).** The echo bar: block echo p95 under the standing 52ms bar with the
  room's serves live — machine half via the existing receipt harness
  in-phase; Sid's headed receipt completes at wear (the space-package G7
  pattern), non-blocking.

**Pre-registered package falsifier (the forcing function, L-criterion):**
after the room stands, the FIRST material-policy change Sid wants routes
through the room (deviate → preview → activate, in-land). A code-lane commit
changing a `default-material-policy-paths` surface while the room stands and
no stop-clause blocked the in-land path = one detected escape AND the
package's criterion FAILED (DIRECTION's conductivity falsifier,
instantiated). The gauge (L7) is the detector; no one has to remember.

## §9 Stop clauses

Standard (skill): contract unbuildable as specified · wrong under platform
semantics · two binding docs genuinely conflict. Named for this package:

- A homeless durable truth appears (→ the material-kernel-module fork, §4).
- Any pressure to widen sites/gestures/modifiers (§4).
- Any need to edit `app.server.cascade` or touch R2's surfaces (§4).
- The UUID-shape law (T6) contradicted at implementation — record the exact
  constraint from `episode.clj` and stop.
- Manifest lines bind on SUBSTANCE; locator drift re-locates + logs, never
  stops (2026-07-27 manifest law).

Escalation per skill: decisions.md Open Questions with verbatim citations,
options, recommendation, re-run list. Ruling execution is a sweep.

## §10 Input manifest — substance binding; line numbers are hints
(machine-verified by grep 2026-07-27; none of these facts are invalidated by
this package's own allowlist)

- `src/app/shared/material_portal.cljc` — `questions` (17 entries, :51),
  `render-model` (:698), `briefing` (:747), `canonical-edn`, `unanswered`.
- `src/app/server/rama/material_portal.clj` — `open` (:653; params
  `:entity-id :wearers :conversation-id :master-ids :narrowed? :scope :cut
  :why :drill?`), the five sub-serves (`:facet-materials`,
  `:material-inspector`, `:interaction-table`, `:material-truth`,
  `:material-experience`).
- `src/app/shared/binding_material.cljc` — READ-ONLY reference: `sites`
  (:77), `space-claim` (:439), `resolve-binding` (:373),
  `camera-gesture-reserved?` (:128), grammar v1/v2 validators.
- `src/app/shared/verb_registry.cljc` — `verbs` (12 entries;
  `:resident/reply-to-block` v1 :197 is the pattern), `bindable?` (:234),
  `effect-classes`, `well-formed-registry?`.
- `src/app/shared/reply_to_block.cljc` — `narrowed-portal-open` (:16),
  `request` (:39), `compose-resident-prompt`.
- `src/app/server/rama/material_circulation.clj` —
  `terminal-escape-report` (:418), `default-material-policy-paths` (:60),
  `experience-around-many` (:1004), `analyze-activation-history`.
- `src/app/server/cascade.clj` — READ-ONLY source: `rows` (:16), `react!`
  (:22), R1 T2/T5 comments.
- `src/app/shared/facet_masters.cljc` — `specs` (:14), `by-id` (:23),
  seven masters incl. `fm:space`.
- `src/app/shared/activation_event.cljc` — scopes
  (`all-unpinned-scope` used at portal `open`).
- `src/app/server/episode.clj` — the minted-uuid law (T6 verification
  duty at plan time).
- `src/app/server/rama/face_projection.clj` — the portal serve face
  (params → `material-portal/open` verbatim, :1484), `portal-briefing`
  driver (:1689), `material-experience-projection` (:697, address =
  material-id).
- `src/app/server_jetty.clj` — the P8 Ctrl+Enter path
  (`narrowed-portal-open` → `portal-briefing` → prompt compose,
  :958-1040).
- `src/app/server/rama/object_container/facet_master.clj` — the
  master-tier loop: `candidate-import-request` (:154),
  `import-candidate!` (:213), `read-master` (:227), `activate!` (:252),
  `ensure-master!` (:344).
- `src/app/server/rama/material_truth.clj` — the instance-tier acts:
  `deviate!` (:51), `release-deviation!` (:58), `pin!` (:64), `unpin!`
  (:71), `blast-radius` (:178), `world-at` (:398).
- `src/app/server/rama/git_spine.clj` — `read-commits` (:140; rows carry
  `:files` via name-status parse).
- `src/app/server/episode.clj` — the birth-import law (:628-633), the
  minted-uuid law (:808, :838), `episode-object-key` (:72).
- `build/editable-material/DIRECTION.md` — the settled spine; GATE_P6/P7/P8
  residue lists (F4 verbatim).
*(Manifest amended 2026-07-27 at plan staging — recon locators above
machine-verified by grep in the staging session.)*

## §11 Allowlist (code edits; docs under `build/matter-room/` free)

`src/app/shared/material_portal.cljc` · `src/app/server/rama/
material_portal.clj` · `src/app/shared/verb_registry.cljc` · NEW
`src/app/shared/matter_room.cljc` · `src/app/server/rama/
face_projection.clj` (serve faces + portal face additions) ·
`src/app/server_jetty.clj` (act endpoints + the P8 room branch) ·
`src/app/server/episode.clj` (P2 ONLY, scoped: actor/unit-kind
parameterization of the utterance import builders — all FOUR involved:
`utterance-import-request` (threads the actor in), `utterance-rows`,
`utterance-actor` (GAINS an arity; existing 0-arity callers untouched),
AND `utterance-projection-hint`, whose `role` slot is
the ONE field the render's machine classification actually reads
(`ground.cljs:1337` `machine? (not= speaker "sid")` ←
`face_projection.clj:127` ← `block_distiller.clj:1385` ←
`episode.clj:207`) — the R1-finding-1 + R2-finding-2 ruling; defaults
byte-preserve current behavior; the `episode_test.clj` role pin updates
with it; any edit disclosed in the phase artifact) ·
`src/app/client/workspace/ground.cljs` (P2 room entry; P3 preview-lane
touch if needed — disclosed) · client entry + room face files (named
per-phase from the plan, diff-derived at artifact time) · test tree. *(Allowlist amended 2026-07-27 at plan
staging: jetty + face_projection were provably required — the P8 briefing
path lives at `server_jetty.clj:958-1040`, the portal face at
`face_projection.clj:1484` — and their absence was a scheduled false
stop.)* CONDITIONAL, disclose-in-artifact: `material_truth.clj` /
`object_container/facet_master.clj` — edited ONLY if a P3-pinned call
site demands a seam; any edit is disclosed in the phase artifact, never
silent. NEVER: `app/server/cascade.clj`, `binding_material.cljc`'s
enums, `env.clj` (never read). A file edited here gets every
pinned-enumeration scan in the test tree grepped for its path before
suite selection is done.

## §12 Handoff

Implementer: cheaper models (Codex/Opus) one phase per fresh context under
this contract; Fable orchestrates, adjudicates, gates (slim tier). Plan →
plan-validation (fresh, default-fail) → phases. After G1–G10 green: board
line flip, residue note in the gate record, retro joins the stratum batch
(cadence ruling 2026-07-27); Sid's wear arms the §8 falsifier — the first
matter-shaping act in-land is the package's true gate.

Only-Sid: names · spend · docs-branch push (never) · North · irreversible
forks. Commit decision per close is Sid's; his 2026-07-27 in-session word
covers building the package; veto any line anytime.
