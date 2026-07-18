# Board — the one file that says where everything is

Every line is a POINTER + STATUS — detail lives in the linked doc, history in
git (`git log -- docs/sessions/next-prompt.md` holds every prior form).
Parallel sessions write their OWN thread file (`build/<pkg>/NOW.md`) and touch
only their own line here; cross-thread flags live here; prune at close. If a
fresh session (or Sid) can't orient from this file top-to-bottom in two
minutes, the board is broken — fix it.

**Ground:** `docs/current-mental-model/decisions.md` (settled ground — read once, then build) · active contracts: **first-light A** (OPEN 07-17, `build/first-light/CONTRACT.md` + RECON.md + NOW.md). durable-ground CLOSED 07-17 (gate PASS; G5c reboot receipt = Sid's slot); block-write CLOSED 07-13; scene-substrate CLOSED 07-13 (CONTRACTs remain binding records for staged later slices).
**Register:** direction sessions boot ONLY from `sense-line-model.md` + BETS
North + `vision/LOG.md` tail (CLAUDE.md rule) — never from this board.
**Vision:** LOG routed through **2026-07-17** (the genesis sitting lands: **first-light RATIFIED** + **durability reaffirmed verbatim** + the lineage harvest + the walkthrough drills; strands in the LOG 07-17 entry, routing recorded there). Prior high-water **2026-07-14, two entries** (latest: **naming + care + instance→type** — Softland is the counter-position to Dynamicland, "build the dream of dynamicland in softland"; **care as the human feature** — caretaker guides doer, humans drive, never autopilot; UI evolution mechanic = repair an instance → promotes to the type, "starting from a point of: I can chat with softland". Earlier same day: **the abiogenesis question** → decisions.md Open questions. Direction exploration ran same session — landing routes at close). Earlier: **the drawn views are references, not targets** (07-13 — REFERENCE examples for "we need a way to design in softland itself"; no view content is settled anywhere; C1/C2 reworded) · notebook pages 07-12 (verbatim + images `vision/images/2026-07-12-notebook-{1,2,3}.png`; FOREST carries the five-unlock ordering) + the base-layer commission (→ decisions.md base ruling + `build/scene-substrate/`). Open pulls (prune as absorbed):
- LOG 07-14 **care as the human feature / caretaker-guides-doer** + **Softland = build Dynamicland's dream in software** (the naming rationale) → North candidates, next vision sitting (Sid's hand only); the 07-15/17 sitting adds the joining frame **care-amplification** — intelligence became cheap, care is the scarce input the medium should compound (Fable frame, Sid-engaged; LOG 07-17)
- LOG 07-10/11/12+13 **the design unlock = the LOOP, not the drawn views** — "a tool to build the tool: design, deploy and use it all at once all from softland" (Sid ranks it #1) + figma-style direct edit as a capability ask. BETS Candidates C1/C2 carry these; **all claude.ai renders + existing example faces = reference scrap with zero authority (Sid, 07-13)** — what views should represent is an open question only Sid answers, ideally from inside the land once the loop exists. Substrate prerequisite already landed (`c788188`: every rendered rt-node carries its template's `:assembly/src-path`).
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
   Staged next slice: P3c — **main-face seam absorbs into first-light
   Phase 0 on ratification** (flip + overlay/truth + real pick context);
   copy echo, per-slot diff, second artery stay staged until pulled;
   gesture slice (mouse-drag copies) at P5+.

1. **Design Unlock** — everything buildable + controllable in-frame by agent
   and mouse/keyb. The target is the LOOP (design, deploy, use — all inside
   softland), NOT any drawn view: Sid's 07-13 correction — the claude.ai
   renders + existing example faces are reference scrap with zero authority;
   what views represent is his open question. Machinery real: faces-as-
   assemblies + arsenal (07-11), click-to-edit substrate (`c788188`), the
   scene floor (CLOSED 07-13). Next concrete: **first-light A** (RATIFIED
   2026-07-17, Sid: "i want to see the first light asap"; the 07-14 genesis
   landing amended 07-15 — `build/first-light/DIRECTION.md`: the evolution
   law; wish-unit conversation face; split A = one local worn arrangement
   repair / B = inheritance, opens only on genuine recurrence; absorbs the
   minimum P3c main-face seam). Pre-req **durable-ground CLOSED 07-17**:
   the land runs on the real single-node cluster BY DEFAULT
   (LAND_CLUSTER=0 opts back to IPC); acked writes survive kill-9 (worn);
   echo p95 7.86ms on-cluster (S3 ~6x headroom); backup+scratch-restore
   proven; records `build/durable-ground/{GATE,RETRO,NOW}.md`.
   C1/C2 = capability labels only.
2. **Editor + write + per-object control** — Sid: "once we have this every
   other thing I can just directly build into it." Whole-loop question
   ANSWERED (07-12): direct Rama round-trip, echo p95 7.66ms, NO optimistic
   layer. **block-write BUILT same day and CLOSED 07-13** — the write organ
   already existed in object-container (`:object/edit` stream); the package
   connected fingers to it. S3 ruled; corrected G8 worn across a real JVM
   replacement; close suites green (`build/block-write/RETRO.md`). Per-object pan/zoom = container-transforms
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

0. **durable-ground leftovers:** (a) the G5c REBOOT drill — the PC-reboot +
   `bin/land up` half HAPPENED 2026-07-18 (mid-P2; machine-half receipts in
   `build/first-light/P2.md`: drill episode + corpus 35/64/247 both served
   post-reboot); YOUR half = open the app and see the conversation. (b)
   `rm -rf /mnt/data/rama/data-restore-scratch` (1.6G drill leftover; rm was
   permission-blocked for the session). (c) vault leg: rsync
   `/mnt/data/rama/backups/` to the Mac. ((d) CODE commit DONE 2026-07-18 at
   Sid's word — `99e0a13` durable-ground + `34cb9e2` first-light P1+P2;
   git-spine suite pair re-run at new HEAD: 16t/365a green.)
1. **Wear session at the dev app:** the editor (`build/editor-feel/NOW.md`
   verdict) · Boxes/Minimap (gesture-feel notes feed the P3c/gesture
   slices). (block-write G8 and scene-substrate's final looks
   came off this list — worn 07-13, package CLOSED; machine-cut G14
   likewise closed 07-13.)
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
- **block-write — CLOSED 2026-07-13.** S2 fired + ruled; finder's F1 HIGH
  fixed; Sid ruled S3 (p95 <=50ms, p99 <=100ms, no optimistic echo) and wore
  corrected G8: visible stale refusal/revert plus same-unit truth across a full
  JVM replacement (`10 replayed, 0 failed`). Close suites 11t/74a + 29t/1008a
  + 27t/349a green. Records: `build/block-write/INT.md`, `FALSIFY.md`,
  `RETRO.md`. New WAL boundaries routed LATER: multi-conversation readiness,
  no-fsync tail, compaction. Code commit remains Sid's call.
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
- **block-write — CLOSED 2026-07-13.** Existing object-container edit stream is
  connected to the reader face with pending-input, Rama truth echo, refusal,
  lineage, and corrected full-JVM WAL survival. Contract + close record:
  `build/block-write/CONTRACT.md` + `RETRO.md`. LATER: long-block input loss,
  multi-conversation replay readiness, no-fsync tail, WAL compaction.
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

**first-light A — OPEN 2026-07-17 · P0–P2 DONE, G1–G4 PASS** (the genesis
package: metabolism — one local worn arrangement repair). Contract + recon
+ thread: `build/first-light/{CONTRACT,RECON,NOW}.md`; phase records
`PHASE_0.md` + `P1.md` + `P2.md`. **The flip is live** (P1: p95 26.6ms,
real picks) and **THE BARE GROUND IS LIVE** (P2, 2026-07-18): product boot
at localhost:8080 = black screen · type · Ctrl+Enter → utterance durable
in OC (`imp:ep:`, asserted-by sid, acked) BEFORE the resident agent
(CLI --session-id/--resume, one object-key with the distilled material)
→ post-turn incremental harvest+distill (user echoes → :native class rows,
never re-minted) → the face re-renders from truth. G3 passed at
POWER-CYCLE grade (the PC rebooted mid-drill — Sid's G5c — and the episode
resumed whole); G4 convergence receipted (re-run = 0 new lines, identical
counts); flags C/D/F adjudicated (C adapter-level, NO stop clause). Dev
workspace moved behind `?dev`. **The GENESIS EPISODE IS VIRGIN — the first
utterance is Sid's act (§11): open localhost:8080 and speak.** Remaining:
P3 wish (flag A adjudicates there) → P4 proposal+membrane → P5
accept/reject/reverse/explain → P6 metabolism (G9 WAITS for a real
friction — no fabrication). §9 redlines 2/3 still on contract defaults
(`:references` · contract naming); veto anytime. Code COMMITTED 2026-07-18
at Sid's word (`99e0a13` + `34cb9e2`; spine pair 16t/365a green at new
HEAD). G5c machine-half banked in passing: corpus 35/64/247 post-reboot —
Sid's app-open check completes it.

durable-ground — CLOSED 2026-07-17 (one day, P0→close): gate PASS, records
`build/durable-ground/{CONTRACT,GATE,RETRO,NOW}.md`; G5c reboot receipt =
Sid's slot (his move list, item 0a); code UNCOMMITTED (Sid's call).

Closed package records remain under `build/<package>/`; active entries are
added here only while work is live. Precedence unchanged: thread files and this
board NEVER outrank CONTRACT.md or settled ground — flag discrepancies inline,
don't pause.
