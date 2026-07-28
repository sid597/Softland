# Softland — settled ground

Read this once, then go build. Everything here is in force now; git holds
history. If reality contradicts this file, reality wins — fix the file in
place. This is not a statute book: no case numbers, no statuses, no evidence
citations. What you get from reading it: what we're building, the architecture
that's already settled (so you don't re-derive it), how we work, and the short
list of things only Sid decides.

Direction lives in `BETS.md` (North — Sid's own words — plus the active bet).
Sid's raw vision stream is `vision/LOG.md`: every new entry gets routed into
the structure same-session and the board's Vision line tracks it — nothing
Sid says is allowed to drop.

## How we work

- **Default is ACTION.** Build it, wear it, keep what survives. Everything is
  approved by default; pick the strongest option, record what you chose, move.
- **Nothing is parked.** There is NOW (the board) and LATER (everything
  else). Any session may pull LATER into NOW when it serves what Sid asked.
  No permission slips, no gating ceremonies.
- **Sid's requests are approximate** — the simplest, cleanest design outranks
  his literal words; a wall means the design is wrong: re-derive, never patch
  around (CLAUDE.md top section).
- **Settled ≠ frozen.** To change anything below: bring a first-principles
  case or a measurement, straight to Sid — he rules fast. What's not okay is
  re-arguing it session after session with no new substance.
- **New capability?** Answer Sid's own questions (2026-07-11), then build the
  strongest version: what do we lose and what do we get · how controllable is
  it · how does it follow from Softland's own eyes · is it buildable,
  modifiable, liveable from INSIDE the land?
- **Quality machinery that stays** (it keeps catching real bugs, cheaply):
  one batched falsification + one gate per coding wave; fresh-context
  subagents as the validation layers (fresh context ≠ fresh session); a
  receipt over the full REAL corpus for anything that processes the whole
  corpus; every session ends with a thread-file entry + board-line flip.
  Mechanics: `.claude/skills/work-package/SKILL.md`.
- **Gates are sized by claim-risk, not by ritual** (Sid, 2026-07-26, after
  the P7-gate ledger: every real catch across eight packages came from
  driving the seam live or probing a NEW claim; suite re-runs never once
  diverged from a build session's numbers, and receipt re-derivation burned
  hours on environment fights). Default gate = SLIM: drive the new seam
  through the real artery, falsify the genuinely new claims, make the
  rulings. Full re-derivation (suite re-run, cold compile, live receipt
  reproduction) is reserved for new-organ/kernel packages or a build report
  that smells wrong — it is an escalation, not a default.
- **Role split:** Fable writes contracts, runs gates, adjudicates forks — and
  implements directly whenever that's the fastest path; cheaper models run
  phases under contracts. Phases are FEW and LARGE (Sid, 2026-07-29): one
  phase is the default when the risky surface is narrow — the contract
  carries plan-grade specificity instead of a phase ladder; mechanics in
  the work-package skill.

## What we're building NOW

**The trail view** — Sid's 27-04 wall panel, alive: timeline · what changed
concretely · the product-DAG with dead-ends, over Softland's own material
(code, commits, docs, chats), answering "where are we, how did we get here,
which crossroads did we take."

- **The unit is the sense-line**: episodes and their marks. Files, sessions,
  and commits are one evidence lens, not the unit. Model:
  `sense-line-model.md`; working map: `build/sense-line-mvp/DIRECTION.md`.
- **Agent-legible first** (View 3): the material must be legible to machine
  minds, not only to Sid — the first paying reader is an agent. Then the
  threaded/DAG timeline (Sid's first pixel surface). Canvas when we get there.
- **The view renders NOW**, ugly and gappy — each gap names the next ingestor
  slice. Import work is ordered by what the rendered view needs, never the
  other way around.
- **Writing:** today the view reads and the Claude CLI writes (the agent
  asserts on Sid's word; both actors durably recorded). The editor write path
  is committed: **direct write over a STREAM topology, echo streamed back**
  (measured: stream echo p95 7.66ms; microbatch measured ~210ms cadence and
  rejected — don't reopen it, the numbers are in `build/write-echo/NOW.md`).
  No optimistic text echo. Block-write is CLOSED (2026-07-13; S3 ruled, G8
  worn across a real JVM replacement). UI write gestures ride the scene
  substrate (the base layer, above).
- **first-light — RATIFIED 2026-07-17** (Sid: "i want to see the first light
  asap"): genesis of building-Softland-in-Softland per
  `build/first-light/DIRECTION.md`, now in force as direction. Sequence:
  the durable-ground slice first (real cluster — settled below), then
  first-light A (one local worn arrangement repair); inheritance (B) opens
  only from a genuine second friction, never a fabricated gate example;
  first-light absorbs the minimum P3c main-face/overlay/pick seam.
- **editable-material — the layer STANDS; all four DIRECTION gates CLOSED**
  (Gate 3 closed 2026-07-25 on Sid's delegated word — the first real repair
  ran the whole loop on the land: banked reply-width friction → durable
  candidate → live preview with the active face byte-untouched → declared
  activation event with grounds → announced change with a named rollback
  path). Softland's components are served, revisioned, deviate-able,
  pinnable, previewable, activatable, reversible material; the four input
  stations dispatch through served rows over an unbreakable code floor.
  The campaign build-out is COMPLETE (P1–P8 all gates PASS, 2026-07-26):
  the portal projects the material world inhabitably, and a banked wish
  travels wish → code → receipts → versioned verb → binding → worn — one
  verb born from inside, Ctrl+Enter riding the material artery with a
  server-narrowed resident briefing. Direction:
  `build/editable-material/DIRECTION.md`; gate records
  `build/editable-material/GATE_P1..P8.md`. Sid's return wear is the true
  gate — the first full breath closes first-light A.
- **space-as-entity — rungs 1+2 BUILT, all twelve gates GREEN 2026-07-26:**
  the space is the outermost rung of every claim chain (a pick miss lands
  on the space, never on nothing), and `fm:space` is the seventh served
  master — the zoom clamp was retuned to 2.0 and rolled back to 8.0 live
  on the cluster with no deploy; tap and shift-marquee are bindable master
  material; camera gestures (naked drag/wheel at ground) are structurally
  uncapturable by material at ANY tier via one reservation predicate read
  by all three lanes. Cross-field form invariants now have a compiler seam
  (`:form-validators`, read at both compile and wear — candidates refuse
  with an error card, durable malformed material falls to the floor). Gate
  record: `build/space-as-entity/GATE.md` — G7 closed on Sid's headed
  receipt (echo p95 23.3 / max 40.3 vs the 52 bar through a sustained
  wheel burst; T6 fallback never needed). **CLOSED + retro'd 2026-07-26**
  (code `43a57a0` + `f7945fd`; HEAD-dynamic suites green at committed
  HEAD; `RETRO.md` recheck-corrected). **Rung 3 (the G10 lift) BUILT,
  Fable gate PASS 2026-07-26:** the space alone gains the instance tier —
  deviate/pin/release on THE space, served and reversible, owner-scoped
  legality at all three G10 lanes, fence inherited at the write lane
  (still one var, three reads); deviation felt live at exactly 3.0 while
  shared served 8.0; pin held 8.0 over an activated shared 2.0. Gate
  record: `build/space-as-entity/RUNG3_GATE.md` (one confirmed judgment
  call — the pre-existing OC bootstrap-vs-normal import-fingerprint
  asymmetry, residue with cheap falsifier; one allowlist drift finding —
  `src-dev/dev.cljc` LAND_PINNED harness guard, default path identical,
  surfaced at commit). **Rung 3 CLOSED + retro'd 2026-07-26** (code
  `ecbd572`; HEAD-dynamic suites independently re-run green at committed
  HEAD by the retro's adversarial recheck; `RUNG3_RETRO.md`
  recheck-corrected — 2 substantive + 6 precision corrections applied;
  lessons routed to the work-package skill: diff-derived phase file
  lists + one-shot gate preconditions).
- **multi-cascade R1 (dark-lane organ #1) — BUILT, Fable gate PASS
  2026-07-26:** server acts are declared as data against named triggers —
  `app.server.cascade` holds the enumerable table (`(cascade/rows)`) and
  `react!`, the one dispatch point (per-row future, isolated failures,
  declaration-order receipts); the episode-turn lane emits
  `:episode/turn-durable` unconditionally after the durable turn, and
  autotag rides as row #1 behavior-identically (guard moved into the
  handler head, `:skipped` decline; idempotency bytes untouched —
  circulation stayed read-only). Zero durable touch; loads pure; dark
  capacity proven honest (a test-only row on a never-emitted trigger
  stays inert). Gate record: `build/multi-cascade/GATE.md` — G6
  re-driven live twice ([CASCADE] on the request thread after durable
  acceptance, resident start +1ms, autotag +16.6s async; accidental
  old-path/new-path A/B on the shared durable cluster corroborated
  behavior-identity). Code awaits Sid's commit decision; close is
  residue-note + board prune (retro joins the stratum batch — cadence
  ruling 2026-07-27, Sid: gate reviews TIERED slim/full, retros BATCHED
  at dark-lane activation / stratum milestone / ~3–4 packages; the
  work-package skill carries the rule; QC layers unchanged). Dark
  capacity activates at the first NEW row (episode-retry or first
  band-act) — that session writes the dark-interval lessons.

## Settled architecture — use it, don't re-derive it

- **Rama is truth.** Workers, agents, and UI actions stream observations and
  requests INTO Rama as events; the UI reads materialized state OUT. No
  side-channel state, no truth outside the log.
- **Durable ground (Sid 2026-07-15; reaffirmed verbatim 2026-07-17; BUILT —
  package closed 2026-07-17, gate PASS):** the land runs on a REAL
  single-node Rama cluster on Sid's PC — depots and PStates are the durable
  log; restart recovery is native (worn: kill -9 mid-typing lost zero acked
  writes, worker replaced in ~1s; on-cluster echo p95 7.86ms, S3 with ~6x
  headroom). The dev boot rides the cluster BY DEFAULT; `LAND_CLUSTER=0`
  opts back into the in-memory IPC boot; tests use IPC constructors
  directly, untouched. `bin/land` owns daemon/deploy/backup mechanics.
  Backups are the free-tier COLD procedure to a local target
  (backup+scratch-restore proven), rsync'd to the Mac vault (vault copies
  inherit never-pushed privacy; encrypt if they ever leave the two
  machines). Boot-time ingest is OFF the startup path (`bin/land ingest`
  explicit). **No hand-rolled journals** — an EDN WAL beside Rama is the
  recorded dead branch (`build/first-light/DEPLOY.md`
  correction-of-record; relapsed once in-session 07-16 and caught). The
  `data/*.ednl` logs were bridges: replayed once into the cluster
  2026-07-17, retired (kept as history). License verified 2026-07-17: Rama
  free for production ≤2 nodes. Records:
  `build/durable-ground/{GATE,RETRO}.md`; the machine-reboot receipt is
  Sid's outstanding slot. Server-trigger rule: the land moves to an always-on
  box only when the first organ must run while Sid sleeps (consolidator /
  overnight agents / phone); the idle Ryzen 5 2600 PC is earmarked for that
  day; the MacBook stays vault-only, never a runtime.
- **The map must not lie.** Provenance is first-class everywhere:
  `asserted-by` (sid | llm | import) rides every relation; machine output is
  visibly distinct from Sid's hand, always; silver/gold tiers where machine
  work grounds human claims.
- **Relations are typed edges** (`relation_kernel.clj`): a closed kind enum,
  grown by one reviewed line when a real consumer needs a kind; identity,
  idempotency, retraction, and history built in. All discourse structure
  rides this kernel.
- **Two clocks on everything that carries time:** claimed-time (orders
  semantic history) is distinct from arrival-time (when the land learned it).
  A July re-import of April notes must never render as "today."
- **External code is a view:** git owns versioning, the filesystem owns
  storage; address code (blob-sha + path anchors), never copy it; on
  conflict git wins.
- **Furniture is data:** faces/assemblies are arrangement-only data over a
  primitive vocabulary; keywords and addresses persist in Rama, fn values
  never; anything that wants to be a program becomes real code in the code
  lane. The loop already closes end-to-end: design conversation → assembly →
  validated → rendered → worn (first done 2026-07-11).
- **One render substrate — ruled the base layer under the five unlocks
  (2026-07-12):** one client scene store keyed `(view-instance, address)` —
  slots are EDN values (container-local drawables, face/template address,
  provenance channel, action DESCRIPTORS — never closures); per-container
  transforms compose in-shader under two cameras (world/screen), so any
  object in frame pans/zooms independently; one pick over the store returns
  addresses + a context bundle (agents and the mouse share the same finger);
  writes ride `:object/edit` + the committed echo. The world, the islands,
  and the text faces are projections over it — seams change projections,
  never architectures. Store coords are f64 world values; GPU buffers hold
  container-relative f32 only. Settled arrangements (where a container
  sits/scales) commit as assembly events; in-flight gestures stay
  client-side at 60Hz. No new Rama organ — the server floor (kernels, write
  organ, echo, llm seat) already exists. Derivation + the five-unlock test:
  `build/scene-substrate/DERIVATION.md`; contract in the same directory.
  (Supersedes the separate staging of container-transforms / point-and-say /
  scene-diff as independent later packages — they are legs and first
  consumers of this one organ. Islands stays staged behind Sid's Box3D
  answer. Instrument: `build/render-north/DELTA-B1.md`.) **Built, worn, and
  gate-passed 2026-07-13** (P1–P4; two wave falsifications + full-diff gate
  finder, all PASS; 240Hz receipts at 60× gate scale; records in
  `build/scene-substrate/GATE.md` + `RETRO.md`). Staged next slice: P3c —
  main-face flip + the overlay-merged echo lane to copies + per-slot echo
  diff.
- **One canvas, many conversations (Sid 2026-07-21):** on the open ground, a
  conversation lane is an ATTENTION, not an identity — each thread of talk
  scopes its own CLI session (per-thread uuid; sessions are bounded
  episodes — next bullet). One river, blocks from all threads individually
  addressable and gatherable across threads — the river is merged AT SERVE:
  canvas truth (utterances, turn records, geometry) lives in the canvas
  container; a thread's distilled material lives in its session's own
  container (harvest identity follows the jsonl line's sessionId by design —
  fighting that meant surgery on the proven fingerprint chain); the turn
  cells are the thread registry the serve merge reads from. BUILT 2026-07-21
  (`7b36a02`). Nothing durable treats a session uuid as a mind.
  Busy-refusal is per-thread — a mid-turn thread refuses at its block, the
  canvas stays live. As threads multiply, context comes by BRIEFING (assemble
  from the land's blocks), never by merging transcripts. Turn records stay
  open to a future `gather` — the set of block-ids a turn drew as context
  (Sid's quote-mechanic, LOG 2026-07-21); not built until a real friction
  summons it. Genesis is the first thread among many — no ongoing
  specialness; `?drill=` canvases keep full isolation (session AND storage).
  Multi-thread affordance grammar (how you chat with many threads) is
  deliberately NOT designed here — it accretes in-land from habitation.
- **Episodes bound every CLI session (Sid 2026-07-22):** a lane (the main
  column, or one thread's) is permanent; the CLI session serving it is not.
  An hour of lane-silence closes the episode forever — cross-boundary
  `--resume` does not exist, so the unbounded-context ride and the
  re-harvest duplication class die together (LLM statelessness means every
  turn re-sends its window; the episode keeps that window bounded and
  cache-warm instead of months-long). The next word opens a fresh session
  whose first prompt is seeded with the lane's prose thread —
  speaker-labelled, noise elided, other lanes excluded — composed from
  durable truth through the same projection that renders the screen, never
  a jsonl replay. The seed rides ONLY the CLI prompt (the flag-D skip keeps
  it from re-entering the container as blocks); the raw utterance stays the
  durable record. Identity unchanged from the bullet above: each session's
  material lives in its own container; turn cells carry the episode id —
  the durable chain the serve weaves successor containers from, into the
  lane's own column, a quiet "fresh session" line at the boundary. Within
  an episode, turns resume the same append-only file. Staged transport
  swap: the persistent stdin process (probe-proven on this machine: one
  process, many turns, one file) behind `summon-argv`. BUILT 2026-07-22
  (`a7c40e6`).
- **Stream writes are at-least-once:** every stream write path derives a
  deterministic op-id from the request-id so a replay overwrites the same
  keys — never duplicates. A replayed-event test ships with every write path.
- **Ingest is idempotent and convergent everywhere:** deterministic ids,
  re-import safe by construction; a new import-key prefix registers its
  routing and ships a foreign-read gate.

## How engine work lands — the build model (settled 2026-07-26)

The engine builds out WHILE Sid lives in the land — never evacuate to
renovate (North: "renovatable by its inhabitants down to bedrock"). The
map of what the full engine is: `engine-terrain.md` (terrain, never
route). How each piece of it reaches the land:

- **Two dials.** Build-ahead is bounded by REVERSIBILITY: additive-and-dark
  work may run as far ahead as design confidence allows — being wrong
  costs build time, never a lived migration. Activation is bounded by
  CONTACT: what goes live under Sid moves at the pace of his inhabitation.
  Contact-gating is an organ property (rim machinery, taste surfaces,
  migrations), not a global law.
- **Two ceremonies.** POLICY merges by activation — safe because malformed
  is refused at the gate and bad is contained (error card, floor
  fallback, one-act rollback). MECHANISM merges by deploy-under-proof —
  indirection seam, replay/drill proofs, revert by commit. The bridge
  between them: **material may select machines only from a closed
  registry with declared contracts** (verbs are the shipped proof —
  registry, effect classes, floor-reserved entries; the wish→code artery
  obeys it: code travels the code lane, material activates only the
  binding). A mechanism that wants activation pays registry + containment
  first.
- **Boundary test, at every contract:** a change touching durables (event
  vocabulary, cell shapes, serve contracts) is never a swap — it is
  CUTOVER-CLASS: scheduled, rare, the land holds still (the frame
  migration; grammar-breaking bumps). Mis-sorting policy / mechanism /
  cutover is the named failure mode; the falsification pass checks the
  sort.
- **The dark lane.** At most 1–2 organs in flight. Dark-organ laws:
  compiles clean in the live build · loads PURE (namespace load runs
  top-level forms — a boot side effect is not dark) · routes nowhere
  until its seam activates · suite-covered from birth · listed on the
  board with its activation trigger. The board list IS the gauge and the
  width limiter (>2 entries is itself the alarm) — honestly a discipline
  with a known board-drift rate, so each activation note checks the line
  was accurate. The dark INTERVAL (build, sit dark, activate later) is
  untested — the first activation writes its lessons (drift? forgotten?
  proofs still green?).
- **Delivery menu:** dark-additive organs · behavior-identical direct
  cuts (the space-package pattern) · strangler seams for direct-caller
  rewrites (indirection first, then old/new under proofs) · worktree
  spikes whose findings return as contracts, never merges · scheduled
  cutovers.
- **The clock** is Sid's ruling latency + activation cadence — never
  build throughput. Package counts are sizing between organs, not a
  schedule.

## Only Sid decides

Spending money · pushing/merging the docs branch (never) · `env.clj` (never
read it) · North's text · genuinely irreversible architecture forks. No fork
of that kind is currently open (durability closed 2026-07-17 — settled
architecture above).

## Open questions — undecided; say your take when you hit one

- **Multi-cascade R2 — what may `cascade/pending-runs` read?** The binding
  contract fixes `$$cascade-pending` as
  `"{String run-id → Long obligated-at-ms}"` (`CONTRACT_R2.md:124-127`) and
  fixes pending enumeration to `"one seek + sequential iteration per
  partition"` (`:156-160`), while the recovery sweep must `"re-execute the
  obligation's handler"` (`:180-186`). The fresh plan can recover the bounded
  payload only by adding one `$$cascade-runs` point read per pending key
  (`PLAN.md:51-66`): P+K seeks, with K unbounded during an outage. Both
  readings physically build, but not together.
  - **A — recommended:** amend the pending value to a typed, bounded resume
    capsule containing `:obligated-at-ms` plus the obligation data needed by
    the runner. Keep exactly two PStates and the one-seek-plus-sequential
    per-partition enumeration; define interval consistency and a last
    status/timestamp check before execution. Cost: bounded duplication while a
    run is pending. Gain: the pinned read cost remains true under outage.
  - **B:** retain the Long value and amend the read law to P+K point reads,
    with a status-and-timestamp recheck and an explicit outage-scale cost.
    Simpler schema; unbounded random-read amplification when pending grows.
  - Under either ruling: sweep §3b/§3d, T3/T6/T11, G2/G4/G10, every P0
    operation/matrix/read-cost claim, then re-run Rama P0 → plan → fresh
    validation. P1 source remains unopened until validation passes.
- **Multi-cascade R2 — how does the sixth module deploy without relaunching
  the five?** G12 says "`bin/land deploy` of the sixth module" while the five
  stay RUNNING (`CONTRACT_R2.md:461-470`), and P1 permits only `"one
  MODULE_VARS line"` in `bin/land` (`:604-615`). Disk is materially different
  from that executable claim: `bin/land:136-153` loops all entries under
  `set -e` and invokes `deploy --action launch` for each; the five entries at
  `:24-30` are already deployed, so the command cannot cleanly reach the new
  sixth entry.
  - **A — recommended:** widen only the `bin/land` deploy seam to accept one
    validated module target and an explicit first-launch/update action, then
    make G12 target cascade-log alone. Keep the all-module clean-cluster path,
    fail closed on an unknown target/action, and settle via server-read module
    status. This makes first deploy and later redeploy executable from the one
    owned ops surface.
  - **B:** amend G12 to use exact direct Rama `launch` then `update` commands
    for cascade-log and leave `bin/land deploy` clean-cluster-only. Smaller
    diff, but splits the deployed-module procedure across the contract and its
    owning script.
  - Under either ruling: sweep §2, G12, §8's `bin/land` manifest, §9's P1
    fence, PLAN deployment commands, and the board; re-run Rama P0 → plan →
    fresh validation before P1.
- **Multi-cascade R2 — what executable evidence replaces G12's unavailable
  consumed-offset receipt?** G12 requires a worker restart to preserve
  `"obligations + offsets (no depot-history replay — native resume)"`
  (`CONTRACT_R2.md:461-469`). The plan supplies only unnamed
  `"server-readable consumed positions/checkpoint"` evidence
  (`PLAN.md:525-534`). Pinned Rama 1.6.0 exposes public module status and
  placement/status commands, but no identified supported API/CLI read for a
  topology's consumed offsets; the internal streaming-state PState is not a
  supported schema the package can safely invent.
  - **A — recommended:** amend G12 to the strongest supported black-box proof:
    server-read cascade-log RUNNING and exact worker placement; persist an
    obligated row; kill only that identified worker; observe replacement and
    RUNNING; server-read the same run/pending truth; append a new observation
    after replacement and verify terminal state plus pending removal. Claim
    native state survival and resumed processing, not directly observed
    offsets or proof that no history was replayed.
  - **B:** provide and bind a supported Rama 1.6.0 offset/checkpoint
    API/command (including schema, worker targeting, and replacement
    predicate), then retain the stronger offset/no-replay claim. Undocumented
    internal PStates do not qualify without that ruling.
  - Under either ruling: sweep G12, P0 deployment semantics, PLAN's exact
    commands/APIs, and the gate receipt vocabulary; then re-run Rama P0 →
    plan → fresh validation before any live cluster mutation.
- Confidence/credential algebra for the trail→code join.
- Question-as-first-class-unit design (design track).
- The requests-vs-walls law's final strength: Sid removed the BINDING form
  from CLAUDE.md and is 50-50 on the softened version (now in memory
  working-agreements) — his call, whenever.
- The bet check ~2026-07-19: is new thinking starting inside Softland or
  still on paper? (`BETS.md` verdict log — the active bet's clock.)
