# Board — the one file that says where everything is

Every line is a POINTER + STATUS — detail lives in the linked doc, history in
git (`git log -- docs/sessions/next-prompt.md` holds every prior form).
Parallel sessions write their OWN thread file (`build/<pkg>/NOW.md`) and touch
only their own line here; cross-thread flags live here; prune at close. If a
fresh session (or Sid) can't orient from this file top-to-bottom in two
minutes, the board is broken — fix it.

**Ground:** `docs/current-mental-model/decisions.md` (settled ground — read once, then build) · active contract: **machine-cut** (`build/machine-cut/CONTRACT.md`, in force 2026-07-12).
**Register:** direction sessions boot ONLY from `sense-line-model.md` + BETS
North + `vision/LOG.md` tail (CLAUDE.md rule) — never from this board.
**Vision:** LOG routed through **2026-07-12** (governance ruling → settled-ground rewrite, done). Open pulls (prune as absorbed):
- LOG 07-11 **designer edit-mode** (figma-like direct edit over live faces, 80→95%) → BETS Candidate C2; sharpening at next intake sitting. Substrate landed 2026-07-12 (`c788188`): every rendered rt-node carries `[:data :assembly/src-path]` = its template node — the click-to-edit-the-template prerequisite for C2.
- LOG 07-10 **design-loop-in-land beyond the self-hosting loop** (accepted faces → arsenal → Softland-as-design-orchestrator → deploy + share) → BETS Candidate C1; sharpening at next intake sitting
- LOG 07-11 **"optimized on every dimension while maintaining explorable explanations + lineage, live and composable"** → North-grade sentence, Sid's hand only — next vision sitting
- LOG 07-11 **branch-from-base** (two worlds built out of one base, code-diff-like) → input to the spatial contracts (SPACE lane)

## FOREST — what everything converges on

Softland replaces the wall as Sid's daily sensemaking surface. Three lanes
serve that; one clock times it:
- **READ (sense-line):** the trail view over Softland's own material — now at
  the **dual-read falsifier** (does the machine's read match Sid's?).
- **WRITE (editor):** type straight into the land — stream transport PROVEN
  (p95 7.66ms); **block-write BUILT, all suites green; G7 measured → S3
  ruling + G8 wearing wait on Sid** (`build/block-write/INT.md`).
- **SPACE (spatial/3D):** per-object control, islands, point-and-say — probes
  DONE, contracts staged behind Sid's wear + Box3D answer.
- **CLOCK:** the bet check ~**2026-07-19** — is new thinking starting inside
  Softland or still on paper? (`BETS.md` verdict log.)

## SID'S MOVES — everything that waits on you alone, cheapest first

1. **Wear session at the dev app (~35 min covers four at once):** the editor
   (`build/editor-feel/NOW.md` verdict) · Boxes/Minimap (unlocks the
   container-transforms contract) · the new `boxes-paired-face` (closes
   machine-cut G14 — the WAL replays at any boot) · **block-write G8**
   (edit a block in minimap-reader-face; steps: `build/block-write/INT.md`
   §5 — dev app is RUNNING now).
1b. **Rule block-write S3 (one read):** echo p95 passes every run with
   margin; the ≤1-stall/min clause fails (2/4/7/2 thin ~130ms tail, invisible
   while typing — pending-input paints immediately). Numbers + options:
   `build/block-write/INT.md` §3. Rec: re-express the tail bound as
   p99 ≤100ms (all runs pass).
2. **Redline the policy-model proposal (one read):**
   `build/policy-model/PROPOSAL.md` — LAW candidates + the open forks in §2
   marked for you; nothing downstream blocks on it (block-write's §6 seam
   already fits it).
3. **Dual-read of window W1** (your half of the falsifier, your pace) —
   `build/sense-line-mvp/dual-read/PROTOCOL.md`.
4. **Box3D reading answer** (engine / docs / both + first-form pick) —
   unlocks the islands contract.
5. **One paste**: `build/sense-line-mvp/dual-read/DESIGN_BRIEF.md` into a
   fresh design chat (reconciliation UI).
6. **~07-19:** the H1 answer — where does new thinking start?

## MACHINE MOVES — running or ready, no Sid needed

- **machine-cut CLOSE session — NEXT** (HEAD suite · G14 record · retro;
  adversarial recheck = Sid's call on cost). ⚠ `boxes-paired-face.golden.edn`
  was REGENERATED 2026-07-12 (`c788188`, face `:assembly/src-path` provenance
  stamp; src-path-only, verified vs prior; full face suite green) — do NOT regen
  it from a stale apply-assembly; re-run `machine-cut-serve-test` after any rebase.
- **block-write — BUILT (lanes + INT done, 2026-07-12); falsification finder
  + gate review running in the orchestrating session.** S2 fired + ruled
  (river-page raw-text → overlay fix, gated). ⚠ T11: UNCOMMITTED additive
  edits open in `face_projection.clj`, `electric_flow.cljc`,
  `block_distiller.clj` + new client ns — machine-cut close session:
  coordinate here before touching those files. Code commit = Sid's call at
  close.
- **Fable's independent dual-read** — fresh session, PROTOCOL anti-anchoring.
- **src/ code-size/verbosity audit — DONE (2026-07-12):**
  `build/code-audit/REPORT.md` — 47k lines / 83 files; ~440 safe mechanical
  shrink + ~130 gated + kernel.clj (830) relocatable; probe (1,482) + pinned
  face-copies (~340) called out separately; headline + cautions Fable-verified.

## LANES — the trees

### READ — sense-line (the product lane)
Path (agreed 07-09): spec ✅ → substrate ✅ (block-kernel + code-atom CLOSED
07-09/10) → **dual-read ⟵ HERE** → dogfood reconciliation UI → marks/kinds
round → benchmark room (question bank waits on kinds).
- **dual-read — OPEN (07-10).** Material picked by Sid: the dissolution chain;
  window W1 = conversation `18d63935` (51 river events; ingested, receipt +
  braid notes in `build/sense-line-mvp/dual-read/NOW.md`). PROTOCOL.md drafted
  (Sid redlines); read material + design fixture generated. Next: Sid's read
  (his pace) ∥ Fable's read (fresh session, PROTOCOL §anti-anchoring) ∥ the
  design session below.
- **reconciliation UI — brief ready, fork travels with it.** Sid opens a fresh
  design session with `build/sense-line-mvp/dual-read/DESIGN_BRIEF.md` (one
  paste); the task-UI vs cards-wall fork is its EXPLORE item 2 — rendered both
  ways, picked by feel there. Design pass precedes build.
- **machine-cut — WAVE DONE + COMMITTED (`ceb84da` code · `f864c74` enum);
  gate PASS w/ 2 open items; close session NEXT (2026-07-12).** Built + gated
  in one day: lanes GREEN, G13 receipt PASS (real run over the real corpus,
  $0.37), falsification 4-HIGH cluster fixed at gate with regressions,
  contract amended in place. **Open: G14 wearing** (interrupted at Sid's
  cost-close; the WAL replays at any boot — wear `boxes-paired-face` when the
  dev app is next up) + the **quiet-box serial re-run at committed HEAD**
  (post-fix run 93t/1458a with 6 llm timing flakes under concurrent boot;
  green isolated). Full record: `build/machine-cut/INT.md` + `NOW.md`. Cost
  flag (Sid, verbatim ledger ~990k subagent tokens): retro rule proposed —
  falsification defaults to ONE finder on the new machinery. Close session
  runs: HEAD suite · G14 · retro (adversarial recheck = Sid's call on cost).
- Residues — LATER: block-kernel (`block-kernel/RETRO.md` §5: §10 cursor query
  · multi-stratum under-fill · content-preview asymmetry) · code-atom —
  **F4 fault-injection + N5 history-sort + the analyzer-sync! settle (GATE §10
  doubt 2) CLOSED 2026-07-12** (`f06f511`, driver 9t/198a); remaining = the
  durable analyzer-basis fork (Sid's) + the `:blobs-unparseable` CI-pin nit.
- Kinds-round evidence pile (carried verbatim for the marks/kinds round):
  "sidetrackkkk" branch receipt · serves/invokes family question · cross-scheme
  bridges (panproto lenses).

### WRITE — editor
- **block-write — CONTRACT v1 IN FORCE (Fable, 2026-07-12); lanes await
  dispatch.** Read-pass discovery re-scoped the package: the write organ
  EXISTS — object-container stream `:object/edit` (validation,
  graduation/revision lineage, stale-seq protection, object-key-scoped
  idempotency journal), and `read-unit` already overlays edits into
  river-page. Package = connect fingers to it: reader-face edit affordance +
  request outbox, `document-container-id` resolution pin, epoch-bump echo
  (pre-named single-unit narrowing), browser E2E vs the unchanged criterion,
  wearing. `build/block-write/CONTRACT.md` (traps BW-T1-T10, gates G1-G9,
  claims file:line-verified) · thread file `build/block-write/NOW.md`.
  **Policy-model design — PROPOSAL LANDED (2026-07-12):**
  `build/policy-model/PROPOSAL.md` — Sid's two questions kept separate;
  monotonic-narrowing + tower-cut LAW candidates; question 2 framed as
  option spaces; plugs the §6 seam without re-plumbing. Awaiting Sid's
  redline; block-write does NOT block on it.
- **editor-feel — CODE DONE + verified, awaiting Sid's wear (07-11).** All 5
  hot-path causes removed/gated: Phase-4B mirror OFF by default
  (`editor-rama-mirror?`, gated in save-editor-doc! → covers mouse.cljs too),
  per-key `(mapv count)` → `:lengths` cached on the doc (reused by
  `<bracket-match`), fold rescan throttled ~150ms (leading, initially-ready —
  JVM-probed). Compiles clean in the live dev server (3 files, 0 warn);
  face+missionary suite 85t/1625a green. Left: `[RAF]`/typing-burst capture +
  Sid's fingers (dev app is live). Thread file: `build/editor-feel/NOW.md`.
- write-echo — DONE (2026-07-12). Stream echo p95 7.66ms, 0/1620 stalls
  (numbers/method: `build/write-echo/NOW.md`); the ruling lives in settled
  ground. Probes stay UNCOMMITTED (`write_echo_probe.clj`,
  `stream_echo_probe.clj`).

### SPACE — spatial/3D
- **spatial cluster — contracts staged, not dispatched (07-11).**
  `container-transforms` (contract AFTER Sid wears Boxes/Minimap — dev app
  running) · `islands` CONTRACT (probe findings in; awaits Sid's Box3D
  reading answer: engine/docs/both + first-form pick) · `point-and-say` +
  `scene-diff` (dimension-agnostic, can rehearse in 2D over live faces) ·
  `G-perf` standing gate clause (one-paragraph template amendment, pending).
  Direction trail: vision/LOG.md 2026-07-11 entries; full analysis:
  `build/spatial/ROAD.md`.
- box3d-spike — DONE (07-11): Box3D (MIT) builds native + single-threaded wasm
  (163 KB gzip); native↔wasm trace **byte-identical** → sim = replayable
  trail; writer seam proven from JS (`b3World_GetBodyEvents`→HEAPF32).
  Caveat: determinism verified one host. Findings:
  `build/box3d-spike/REPORT.md`. → islands rung-2 unblocked.
- islands-probe — DONE (07-12): all P1–P6 proven on real HW (Radeon 7900 XTX,
  headed Chrome): sleeping island ~free · cost decouples from cadence · MSDF
  text in a perspective pass VERIFIED (fwidth-derived screenPxRange); 60 fps
  to 1.5M cubes, single-buffer ceiling ~1.6M (default 128 MB storage limit).
  Report + screenshots: `build/islands-probe/REPORT.md`. Probe code
  UNCOMMITTED in tree (`island_probe.cljs` + 5 mount lines in `render.cljs`,
  tagged `islands-probe 2026-07-11`; delete to remove). Feeds the islands
  CONTRACT: boot device-limits/timestamp-query, scene-as-data dirty policy,
  compositing/z-order, fwidth-MSDF text path.

### STANDING
- design queue — LATER: question-unit design (noted 07-04; settled-ground Open
  Questions + trail-view INPUTS.md item 5) · model-UXR question bank
  (post-kinds; apparatus warm, DIRECTION.md §4.3) · block/code two-arm braid
  (noted 07-09).
- framework — CLOSED + retro'd (07-11): `build/framework/RETRO.md` (54/880
  green at committed HEAD); its pair-structure residue spawned machine-cut.

## Active package blocks

**machine-cut** — STANDING + NOW at `build/machine-cut/NOW.md`.
**block-write** — STANDING + NOW at `build/block-write/NOW.md`.
Charter: STANDING (frozen at open) + NOW (≤15-line entries) live in
`build/<package>/NOW.md`; one line under LANES; prune at close (/work-package
skill governs the mechanics). Precedence unchanged: thread files and this
board NEVER outrank CONTRACT.md or settled ground — flag discrepancies
inline, don't pause.
