# scene-substrate — package retro (Fable, 2026-07-13)

Written from the full trail: NOW.md (13 entries) · CONTRACT.md · DERIVATION.md
· FALSIFY.md (waves 1–2) · GATE.md · the eleven code commits
`20ee578`→`82d9981` + three gate fixes · Sid's pasted receipts verbatim.
Adversarial recheck of this retro = Sid's call on cost (machine-cut
precedent); until run, treat scorecard claims as author-graded.

## Shape of the package

Derived, contracted, built (P1–P4), falsified twice, worn, and gated in
~30 hours of wall clock. Four fresh-context Opus implementation subagents +
Fable-direct P2 (the declared risk center); two wave falsifiers + one gate
finder (all under the ONE-finder rule); wearing ran as five G11/G4/G6
rounds with Sid's pasted receipts as ground truth. Two walls stop-claused
honestly into P3c instead of patched. All dispatched phases landed inside
one day — the fastest package to date at the largest diff (2,450 lines).

## QC-layer scorecard

1. **Contract (Fable).** Pinned shapes held end-to-end — no phase invented
   an envelope, no scope conflict fired. One hygiene miss: G4's read plan
   claimed a "16 B" write the code never did (wave-1 #3, amended in place);
   one gate letter (G9 "resolves through existing read APIs") never pinned
   its executable form, so the test asserts the weaker store-index half
   (gate F7).
2. **Executable gates.** G1–G3/G10 caught nothing post-hoc because they ran
   green in-context at each phase — working as designed (upstream receipt,
   not luck). 68t/888a re-verified independently at gate.
3. **Wave falsifiers (ONE finder each).** Wave 1 (188k): 5 LOW, two real
   latents (deepest-first address writes; probe step! tearing down the
   reactor on throw — a fix that HELD when wearing later threw for real).
   Wave 2 (82k): the HIGH orphan-slot lifecycle leak, routed INTO P3b
   mid-flight via SendMessage and closed in-slice — the finding shaped the
   phase instead of queueing behind it.
4. **Wearing (layer 5) — the biggest earner, three catch classes no suite
   could reach:** (a) ENVIRONMENT: the 91ms G4 FAIL was SwiftShader —
   Chrome WebGPU rasterizing on CPU without the Vulkan flag; the code was
   innocent, and only adapter.info could say so. (b) FIRST-CONTACT: Sid's
   first face click leaked literal `false` into record-pick! and killed
   the Electric reactor — a cljs type-contract break invisible to JVM
   suites, whose downstream symptoms (echo lag, /face erroring, no
   SCENE-CTX) all masqueraded as separate bugs. (c) DESIGN GAPS:
   off-screen spawn at real face widths, copy stacking, text interleave.
   Also earned: Sid's pre/post A/B paste DISPROVED the Rama-ingest suspect
   — measurement killed a wrong hypothesis before it cost a session.
5. **Gate finder (192k).** Catch class DISJOINT from wearing: latents only
   (per-keystroke full repack of every copy via the face-scene trigger; cid
   overflow → reactor death; unguarded bundle on the submit path). Zero
   noise findings; also REFUTED one plausible concern (backdrop pool
   corruption) by trace, which is as valuable as a catch.

Layers 4 and 5 caught disjoint defect classes in both directions — the QC
model's core bet, observed cleanly here.

## What the next contract should do differently (each traceable)

1. **GPU perf gates open with an environment attestation.** The receipt's
   FIRST field is adapter identity (`isFallbackAdapter`, device string);
   a perf number without it is unclassifiable. Grounds: one full diagnosis
   cycle spent proving 91ms was SwiftShader, not the code.
2. **A gate letter that names an external system pins its executable form
   in the same sentence.** "Resolves through existing read APIs" shipped
   with no named test shape, so the executable half silently became the
   weaker in-store claim (gate F7). Grounds: G9.
3. **Any echo/refresh trigger names its KEYING SOURCE.** The contract said
   "rebuild on projection change" but never pinned which atom's identity
   MEANS projection-changed; the implementation keyed on a fast-flipping
   scene identity while rebuilding from the slow-changing projection —
   crossed cadences = per-keystroke repack + seconds-stale copies.
   Grounds: gate F1, the package's top carried item.
4. **An allocator against a fixed GPU range recycles or caps inside the
   guarded path.** A monotonic counter + an unguarded throw outside the
   frame try/catch = deferred reactor death. Grounds: gate F3 (and wave-1
   #2 already proved the throw-outside-guard class once).

## Mechanisms that earned their keep

- **ONE-finder-per-wave** (machine-cut retro rule): three finders, 188k +
  82k + 192k, every one paid; zero redundant-fan-out spend.
- **Mid-flight finding routing via SendMessage** into a live implementer
  subagent — wave-2's HIGH closed in the same slice that would otherwise
  have shipped it.
- **Honest stop-clauses → staged phases.** P3c's wall #1 (overlay
  threading), named at P3b, turned out to be EXACTLY where the gate's top
  finding folds — the staging pre-built the right box for the next defect.
- **Wearing receipts as pasted numbers**, not impressions: they set the
  240Hz bar, disproved a suspect, and closed G4 at 60× gate scale
  (628k glyphs flat 4.2ms p50).
- **Traps ledger cited by number** — the gate finder classified findings
  IN the ledger's vocabulary (F1 = "T5 at echo granularity"), which made
  adjudication near-mechanical.

## Honest cost ledger (subagent tokens)

P1 134k · wave-1 finder 188k · P3a 245k · wave-2 finder 82k · P3b 218k ·
P4 169k · gate finder 192k ≈ **1,228k total** (Fable-direct P2 + fixes +
adjudication not metered here). Wave-1's overrun vs its stated estimate
was flagged to Sid at the time; later waves stated counts up front.

## Residue (tracked, non-blocking)

- **P3c (staged phase, now carries three named items):** main-face flip
  (walls recorded at P3b) · overlay-merged lane to copies (gate F1b) ·
  per-slot echo diff / Δ1-primitive reuse for the repack cost (gate F8).
  Plus the second assembly artery (spawn-by-name).
- **G11 residual (Sid):** one spawn(2)/(3) look after hard refresh
  (cascade + backdrop) · the G8 block-edit look (edit + restart +
  forceStale, zero copies open).
- Open doubts with falsifiers named: GATE.md §Open doubts (resolve-layout
  idempotency · G9 byte-identity test · bundle fuzz).
- F4: ct-probe × sceneFaces containers-buffer collision (two dev tools;
  both delete-to-remove).
- Design note for the legibility batch: face-mode sidebar/header text tiny.
- Naming pass on "scene-substrate" (Sid's cheap re-rule, from STANDING).
- Discipline carrying into the NEXT package contract: the environment
  attestation rule (#1 above) applies verbatim to the islands package's
  perf gates.
