# Board — the one file that says where everything is

Every line is a POINTER + STATUS — detail lives in the linked doc, history in
git (`git log -- docs/sessions/next-prompt.md` holds every prior form).
Parallel sessions write their OWN thread file (`build/<pkg>/NOW.md`) and touch
only their own line here; cross-thread flags live here; prune at close. If a
fresh session (or Sid) can't orient from this file top-to-bottom in two
minutes, the board is broken — fix it.

**Ground:** `docs/current-mental-model/decisions.md` (settled ground — read once, then build) · active contracts: **block-write** (close pending Sid's S3 + G8). scene-substrate CLOSED 07-13 (its CONTRACT stays the binding spec for the staged P3c/P5+ slices).
**Register:** direction sessions boot ONLY from `sense-line-model.md` + BETS
North + `vision/LOG.md` tail (CLAUDE.md rule) — never from this board.
**Vision:** LOG routed through **2026-07-12** incl. Sid's notebook pages (transcribed verbatim + images `vision/images/2026-07-12-notebook-{1,2,3}.png`; FOREST carries his five-unlock ordering; notebook History corrected against the record: `docs/history/sense-line-story.md`) **+ the base-layer commission** (routed same-session → decisions.md base ruling + `build/scene-substrate/`). Open pulls (prune as absorbed):
- LOG 07-11 + notebook 07-12 **designer edit-mode / figma-style** (direct edit over live faces, 80→95%) → BETS Candidate C2; notebook = intake material; sharpening at next intake sitting. Substrate landed 2026-07-12 (`c788188`): every rendered rt-node carries `[:data :assembly/src-path]` = its template node — the click-to-edit-the-template prerequisite for C2.
- LOG 07-10 + notebook 07-12 **design-loop-in-land — "a tool to build the tool: design, deploy and use it all at once all from softland"** (Sid ranks this unlock #1) → BETS Candidate C1; sharpening at next intake sitting
- LOG 07-11 **"optimized on every dimension while maintaining explorable explanations + lineage, live and composable"** → North-grade sentence, Sid's hand only — next vision sitting
- LOG 07-11 **branch-from-base** (two worlds built out of one base, code-diff-like) → input to the spatial contracts (SPACE lane)

## FOREST — Sid's five unlocks (his 12-07 notebook ordering; verbatim in LOG + `vision/images/2026-07-12-notebook-*`)

Ordered by unlock potential — "how powerful and useful softland would be
right now and in future" (Sid's own ranking; state current per item):

**0. THE BASE (under all five) — scene-substrate CLOSED 2026-07-13.**
   One client scene store keyed (view-instance, address) + per-container
   transforms + one pick/context seam + actions-as-data; absorbs
   container-transforms / point-and-say / scene-diff. Worn at 240Hz, 60×
   gate scale; records `build/scene-substrate/GATE.md` + `RETRO.md`.
   Staged next slice: P3c (main-face flip + overlay echo lane to copies +
   per-slot diff); gesture slice (mouse-drag copies) at P5+.

1. **Design Unlock** — everything buildable + controllable in-frame by agent
   and mouse/keyb. First rung REAL: faces-as-assemblies + arsenal (07-11),
   `boxes-paired-face` wearing the real corpus (07-12), click-to-edit
   substrate landed (`c788188`: every rendered rt-node carries its template
   node's `:assembly/src-path`). Missing: the in-land design surface +
   figma-style direct edit → BETS Candidates C1/C2; the notebook IS their
   intake material. Next concrete: intake sitting → first probe contract.
2. **Editor + write + per-object control** — Sid: "once we have this every
   other thing I can just directly build into it." Whole-loop question
   ANSWERED (07-12): direct Rama round-trip, echo p95 7.66ms, NO optimistic
   layer. **block-write BUILT same day** — the write organ already existed
   in object-container (`:object/edit` stream); package = connect fingers to
   it; all suites green; left on Sid: S3 stall-clause ruling + G8 wearing
   (`build/block-write/INT.md`). Per-object pan/zoom = container-transforms
   contract (after Boxes/Minimap wear); editor-feel fixes await fingers.
3. **Semantic breaking of block types** — containers exist (block-kernel
   CLOSED); the semantic LAYER is not implemented yet — by design it rides
   local-LLM marking (llm-module ready). First live piece landed 07-12:
   machine-cut's annotation runs minting `pairs-with` edges over the real
   corpus. The lens questions (dg / olog / panproto, multi-marking per node,
   layers) = the marks/kinds round, after dual-read grounds which marks are
   real. Kinds-round evidence pile below.
4. **Representation of semantic blocks** — face machinery DONE (three faces
   wear a real conversation); design candidates exist (claude.design rounds);
   reconciliation-UI brief ready (one paste). The composition questions (how
   do blocks compose; thing = group = obj family) feed the same design round.
5. **3D render** — both probes DONE (Box3D deterministic native↔wasm; islands
   60fps @ 1.5M cubes; MSDF-in-perspective verified); build staged behind the
   islands CONTRACT + Sid's Box3D answer. Last on Sid's list, staged
   accordingly.

**CLOCK:** the bet check ~**2026-07-19** — is new thinking starting inside
Softland or still on paper? (`BETS.md` verdict log.)

## SID'S MOVES — everything that waits on you alone, cheapest first

1. **Wear session at the dev app:** the editor (`build/editor-feel/NOW.md`
   verdict) · Boxes/Minimap (gesture-feel notes feed the P3c/gesture
   slices) · **block-write G8** (edit a block in minimap-reader-face;
   steps: `build/block-write/INT.md` §5). (scene-substrate's final looks
   came off this list — worn 07-13, package CLOSED; machine-cut G14
   likewise closed 07-13.)
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

- **scene-substrate — CLOSED 2026-07-13** (P1–P4 built + falsified + worn
  by Sid + gate PASS 0-HIGH + retro; records `build/scene-substrate/
  GATE.md` + `RETRO.md`; retro recheck = Sid's call on cost). Staged next
  slice when pulled: **P3c** — main-face flip + overlay echo lane to
  copies + per-slot echo diff + second assembly artery (CONTRACT §P3c +
  gate F1b/F8).
- **block-write — GATE REVIEW PASSED (2026-07-12); close waits on Sid's S3
  ruling + G8 wearing.** S2 fired + ruled (river-page raw-text → overlay
  fix); finder's F1 HIGH fixed at gate (union-map truth pull + clear-all
  prune); suites 74a+1078a+349a green. Record: `build/block-write/INT.md`
  §6 + `FALSIFY.md`. T11 RESOLVED: the edits are COMMITTED (`ad19b96`);
  machine-cut close updated framework's g21 read-surface scan to admit
  `ocr/read-unit` (stale-red at HEAD — the scan block-write's suites never
  ran).
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
- **machine-cut — CLOSED 2026-07-13** (all gates incl. G14 worn in the wild
  during scene-substrate G11 — WAL replay receipt in the boot log; quiet-box
  suite 94t/1476a green at HEAD after one stale cross-package scan fix;
  retro + routed lessons: `build/machine-cut/RETRO.md`). Adversarial recheck
  of the retro NOT run — Sid's call on cost.
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
- **spatial cluster — FOLDED INTO scene-substrate (2026-07-12 ruling,
  decisions.md):** `container-transforms` = its transform leg ·
  `point-and-say`'s pick/bundle half = its P4 seam · `scene-diff` = a
  staged first consumer. Remaining separate: `islands` CONTRACT (probe
  findings in; awaits Sid's Box3D reading answer: engine/docs/both +
  first-form pick) · `G-perf` standing gate clause (pending). Analysis
  trail: `build/spatial/ROAD.md` (superseded where it staged separate
  packages).
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

**block-write** — STANDING + NOW at `build/block-write/NOW.md`.
Charter: STANDING (frozen at open) + NOW (≤15-line entries) live in
`build/<package>/NOW.md`; one line under LANES; prune at close (/work-package
skill governs the mechanics). Precedence unchanged: thread files and this
board NEVER outrank CONTRACT.md or settled ground — flag discrepancies
inline, don't pause.
