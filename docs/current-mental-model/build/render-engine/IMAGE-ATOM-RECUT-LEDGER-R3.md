# IMAGE-ATOM — third recut ledger (the ruling session's map)

**Session:** fresh Fable ruling session, 2026-08-05. Final adjudicator over
the frozen Q1–Q7 packet (`IMAGE-ATOM-RULING-PACKET.md`). Gathering ran on
four Opus lanes (W1/ENGINE/W2-B-T1 verbatim · scene_tape/store/rect_tree/
scene_runtime/containers forms · renderer/buffer_pool/gpu_budget/
electric_flow forms · verifier/run_verifier/fence/shadow-cljs/receipt.json);
every load-bearing citation relied on below was verified verbatim against
disk before ruling. Adjudication never delegated.

**Freeze check (Act 0):** branch `docs/current-mental-model-local` · opening
HEAD `f08c1916b78badce5213a400fe6a4ef67c32c52b` · tree clean · frozen
contract sha256 `0e84a86abd81bbb1f0e1d246f93d03b2e72aea34cd45eba2f47e1a8bc21cd859`
— all matched the ruling packet's pins. (The expected foreign
`smalltalk-ui-vm/PODCAST_SCRIPT.md` was not present in the tree at all;
noted, not a stop; nothing under that path was touched.)

**Contract:** 800 → 1,116 lines. New sha256
`ebd894a58af945b761907decad74f8e7f3d39ea0a744ccadc6ce168900783800`.
Git diff from `f08c191` is the change map; ledgers R1/R2 and every
validation artifact are immutable history — none were edited.

---

## Q1–Q7 ruling table

| Q | Disposition | Binding fact (verified) | What changed |
|---|---|---|---|
| **Q1** color regime | **ACCEPT (b)** — one declaration, seam-state orthogonal; the zero-transfer row deleted | `W1.md:686-688` ("never zero or two" — the cited sentence forbids the row) · `W1.md:710-712` (C4 equivalence) · `scene_tape.cljc:364-367` (only linear-premultiplied is declarable) · `:279` (all five families hardcoded to it) · `:44-51` (the SEAM owns legacy state) | §5 Contract-C block rewritten; G6 legacy leg → seam-off consistency receipt (zero transfers end-to-end, pinned-byte fixture) + C6 one-row law; T3 amended; R3's (a) refusal REJECTED — it would couple one family's visibility to seam state, a mechanism no admitted family carries |
| **Q2** mixed order | **AMEND (a)** — one entry per (vi, `:images`), paint = ordered sub-draw vector (W1 §3.2's "explicit indirection that reproduces tape order"), executed by the image family's REGISTERED `:execute!`; lane order declared shadow 0 < rect 1 < slot-text 2 < **image 3** | `scene_tape.cljc:393-397` (pr-str tie), `:508-515` (duplicate-id throw — the packet's per-run ids `[:frame/store vi :images]` would collide, and run ordinals in stable-tie mis-sort at 10 vs 2 under pr-str) · `renderer.cljs:2170-2174` (per-row `:execute!` — the registered-executor seam already exists) · part-rank literals :2031/:2047/:2110 | §6 mechanism rewritten; §7 lane order + capability limit + felt-wave-revisitable image-above-text default declared; T13 minted; G5 uniqueness + sub-draw-order legs. Image=3 chosen over renumbering slot-text (zero existing-lane edits → no scheduled false stop) |
| **Q3** pick route | **ACCEPT (single option) + the Codex boundary leg** | `rect_tree.cljc:12-28` (ten keys, NO transform — grep 0 hits) · `:372-373` (half-open, no slop input) · `scene_store.cljc:220-225` (ancestor fallback), `:250` (owner vi hardcoded) · `W1.md:36-39/78-81/694-700` (no grandfathering; migration input ≠ precedent) | §5 pick bullet rewritten: equality-by-construction with pinned precondition (T14 + fence forbid + G8 key-set pin); divergence-sentinel framing dropped — G4 asserts AGREEMENT; boundary law recut to half-open truth; hit-slop pinned 0.0 (closes N10); `[:data :address]` REQUIRED on image rt-nodes, refused at flatten; `:classify`'s stale "exact boundary = semantic hit" reconciled |
| **Q4** twin-check vehicle | **AMEND** — gate claim dropped (packet's grounds accepted: dark-wave vacuous; verified — verifier has zero `draw-frame`/tape-path hits; product `:images` structurally empty) but replaced, not erased: R3's own option (a) scoped machine-only | `renderer.cljs:2209/2215/2237/2243` all `defn-`; `draw-frame!`'s one caller `runtime/render.cljs:717` (MUST NOT) · `SEAM-STEP1-P1.md:101` (the established road is Sid's hand, contradicting §10) | §7: un-privatize `produce-frame-entries`/`update-frame-arrangement`/`compile-frame-tape`; VERIFIER drives a synthetic frame (stub pool-infos + non-empty `:images`): maintained ≡ batch, seeded duplicate-id rejected, lane order asserted. §10's "no gate needs Sid" preserved. Product frame-loop join = SECOND staged obligation in the preamble, beside the felt gate |
| **Q5** helper authorization | **ACCEPT, tightened** — "byte-identically" → VALUE-identical with an executable form | `scene_tape.cljc:244-283` (`registration` literal constants incl. `:export-projections` :255), `:67` (`regime` single-band hardcode), `:285→:385` feeding every store write (`scene_store.cljc:166-169`) and `frame-contract-registry` (renderer.cljs:2192-2202) | §5 authorization sentence; G1 asserts the five existing registrations value-identical against an Act-0 EDN snapshot; M9 pinned `:export-projections :none-promised` |
| **Q6** buffer home | **ACCEPT + one seam-law clause** | `pool-draw-info` def `buffer_pool.cljs:354` + exactly four call sites, all `runtime/render.cljs` (MUST NOT) · frame map assembled in `draw-frame!` :2346-2368 with `store-frame` riding in whole · fence forbids only the four pass calls (:76-79) | §6: `init-image-system` owns the pool (via `buffer_pool` API, file unedited); written in `draw-frame!`'s encode window from `(:images store-frame)`, IDENTITY-GATED (T5/G8 ops-identity law — render-seam proportionality, never per-frame unconditional); `buffer_pool.cljs` + `containers.cljc` added to §12 MUST-NOT as read-only context; §11 rows added |
| **Q7** error sink | **ACCEPT** | `globalThis` grep: exactly two existing names (renderer.cljs:2244/2250/2256); no error lane exists (diagnostics :2084-2155 is a text-slice producer; sole `console.error` is the twin divergence) | §6: `__softland_image_ingress_receipt` — per-digest `{ok\|refused\|unavailable\|device-lost, reason, counts}`; placeholder paint deterministic; OVER-BUDGET refusal joins the sink (closes M10's budget-failure gap); G9 drives unavailable/device-loss/over-budget and asserts the fields; no product error UX this wave |

## Section-by-section consumption map

- **Status block** — R3∥Codex + ruling history; BINDING as of this recut;
  cycle spent, implementation OPEN.
- **Preamble (felt gate)** — second staged obligation added: the product
  frame-loop join receipt (Q4).
- **§3** — refused shape named: the archive draft's `image-scene-entries`
  per-instance tape entries (A6).
- **§5** — `:classify` boundary tri-state reconciled · `:time-sample
  :none-static` added (A1) · pick bullet rewritten (Q3) · regimes: 3-band
  partition, all eight keys, `validate-regimes!` law, screen-constant
  normalization declared + path-wave owner, two-extent duty (N9) · helper
  authorization (Q5) · M7 declared partial refusal, M2/M12 receipt homes,
  M9 `:none-promised` (N12) · Contract-C block rewritten (Q1).
- **§6** — sub-draw indirection + registered executor (Q2) · buffer home +
  identity-gated write (Q6) · error/lifecycle sink + budget-failure
  refusal (Q7, M10).
- **§7** — `[:data :address]` requirement (Q3) · clip crop/UV-inset law
  (T15, R2 N15) · `:ops-count-by-vi` gains `:images`, `:order-by-vi`
  UNCHANGED (N7) · extraction home pinned `scene_store.cljc`, second
  namespace dropped (N11) · lane order + capability limit (Q2) ·
  synthetic-frame arrangement receipt replacing "covers them" (Q4) ·
  closing paragraph states the product-loop non-claim in receipts (R2
  repair (c)).
- **§8** — three fixtures / 21 image golden rows (T15's pixel truth) ·
  stdout keys enumerated + asserted (repair (c) complete) · manifest home
  pinned `imageAtomCases` (A7) · expected final values pinned in-contract
  (R2 N5 residue) · `imageAtomInputs` fingerprints for the wave's four
  extra edited sources (A4) · two-extent parity rows (N9) · ICC Act-0
  probe with S5 fallback (R2 N19) · assertion mode's field list extended +
  exit law bound to invocation mode (N8).
- **§9** — T3 amended (pre-blend encode trap A5; candidate-chain wording
  Q1) · T13 (part-rank/pr-str) · T14 (per-node transform) · T15
  (clip-clamp stretch) minted.
- **§10** — `[RENDER-RED]` form added; G1 value-identity leg; G2
  mode-bound exits + stdout keys + A3 scope honesty; G3 three fixtures;
  G4 agreement + two extents + address + slop-path record; G5 uniqueness +
  sub-draw order (labels de-malformed); G6 rewritten per Q1; G7 M12
  corpus-enumeration leg (pinned eight-pressure list); G8 refusal fixture +
  T15 math + key-set pin + M2 leg + synthetic-frame receipt; G9 over-budget
  + sink assertions; G11 pick case-forbid + T14 fence leg; receipt↔M map
  rewritten (M2/M4/M7/M9/M10/M12).
- **§11** — pinned facts added: rt-node ten-key set (no transform) ·
  part-rank literals · `:order-by-vi` lane-agnostic · globalThis's two
  existing names · `frame-order` default 0 · `compare-scalar` pr-str ·
  duplicate-id law · `validate-regimes!`/`required-regime-keys` ·
  `deepest-addressed` · containers.cljc + buffer_pool.cljs read-only rows ·
  run_verifier cascade/stdout/productionInputs locators.
- **§12** — `buffer_pool.cljs` + `containers.cljc` explicit MUST-NOT
  (read-only context).
- **§13** — S4 bound to ordinary invocation (assert-mode exit 0 ≠ flip);
  S5 extended to the ICC ingress-decode impossibility.
- **§14** — validation ladder recorded COMPLETE + SPENT; in-phase falsifier
  named (one finder: sub-draw/executor order + color chain + lifecycle);
  Act 0 extended (ICC probe · five-family EDN snapshot · part-rank pin
  grep).

## R2 repair (c), MINOR, and advisory disposition

- **R2 repair (c)** — CONSUMED COMPLETE: §7's "covers them" replaced by the
  real vehicles; §7 closing + §8 stdout keys (`existingGoldens ·
  sourceMatch · environmentMatch · imageGoldens · imageDeterminism ·
  imageParity`) + `--assert-image-contract` presence/value assertions +
  the product-loop non-claim receipt field.
- **R3 MINORs:** N7 CONSUMED (§7/G8 — `:order-by-vi` unchanged) · N8
  CONSUMED (§8/§13/G2 + `[RENDER-RED]`; label backticks fixed) · N9
  CONSUMED (§5 eight keys + partition law; G4 two extents; normalization
  declared with path-wave owner) · N10 CONSUMED (radius 0.0 pinned, G4
  records no-slop-path; folded into Q3) · N11 CONSUMED (home pinned
  `scene_store.cljc`) · N12 CONSUMED (M2→G8 leg · M7→declared partial
  refusal · M10 budget-failure→G9 · M12→G7 corpus-enumeration leg) · N13
  CONSUMED all four legs (T15 clip law + G3 fixture (c) · fence pick
  case-forbid in G11 · address folded into Q3 · ICC probe + S5).
- **Remaining R2 MINORs** (per R3's own mapping): N13→R3 N7 · N14→R3
  N2/Q2 · N17→R3 N10 · N7/N15/N16/N19→R3 N13 · N18→R3 A3 — all consumed
  through the rows above; nothing silently dropped.
- **R3 advisories:** A1 RECUT (`:time-sample` in descriptor) · A2 CLOSED —
  stale against the frozen bytes: the frozen contract already lists
  constructors :681/:866/:930/:995/:1118 + non-system :1256 (Codex's
  narrow fix; verified against disk grep) · A3 RECUT (G2 scope honesty:
  21 goldens cover 3 of 5 families; shadow/clip ride suite+fence+
  arrangement receipt) · A4 RECUT (`imageAtomInputs` fingerprints; legacy
  `productionInputs` untouched) · A5 RECUT (T3's pre-blend encode trap) ·
  A6 RECUT (§3 names `image-scene-entries` as the refused shape) · A7
  RECUT (`imageAtomCases` manifest section pinned) · A8 CLOSED (process:
  the freeze + this ruling session's re-pin is the answer; §14 records
  the ladder complete; no contract mechanism needed).

## Ruling-execution sweep results

Grepped the recut contract for every derived enumeration: zero surviving
occurrences of the zero-transfer/two-regime vocabulary outside the Status
history and the Q1 ruling's own explanatory text; zero per-run entry-id or
"covers them" residue; all gate labels well-formed with `[RENDER-RED]` vs
`[RENDER-ASSERT]` exit semantics bound per mode; receipt↔M map consistent
with §5's refusal shapes; trap ledger T1–T15 with all three new traps
cross-referenced from §5/§7/§8/G3/G5/G8/G11 and §11's pinned facts;
`<declared>` placeholder gone; fixture/row denominators consistent (3
fixtures · 21 image golden rows · 14 parity rows (2 extents × 7 regimes) ·
legacy 21/21/14-21/7-7 untouched); §12 MUST-NOT extended without touching
MAY EDIT/MAY CREATE; stop clauses S1–S6 intact with S4/S5 sharpened. One
contradiction found and fixed during the sweep: §5 `:classify`'s "exact
boundary = semantic hit" vs the half-open law.

## Implementation / FULL-gate residue (explicit)

Carried to the IMPLEMENTER (in-phase duties, already contract-bound):
1. Act 0: §11 symbol sweep · §8 boot-verify (expected exit 1, fingerprint
   `5ced2482…343e`, `sourceMatch:false` as named input debt) · cljs warning
   baseline · ICC decode probe (S5 on fail) · five-family registration EDN
   snapshot · part-rank pin grep over the test tree.
2. The §14 in-phase falsifier (one finder: sub-draw/executor order, color
   chain, lifecycle/budget paths).
3. INFERENCE flagged by R3, unresolved by reading: whether a non-AA image
   quad emits only 0/255 coverage (the §5 ramped-edge obligation exists
   because of it) — the implementer's shader must produce a ramped edge;
   G4 floors fail loudly if not.
4. `init-image-system` bind-group shape vs `execute-gpu-batch!`'s
   single-group assumption — dissolved by the Q2 registered executor
   (sub-draw walker sets its own groups), but the walker is new code the
   falsifier targets.

Carried to the FULL GATE (independent session):
1. Re-run G1/G2/G3/G4/G10/G11 + spot-check receipts (contract §10
   partition).
2. The A4-class custody check: `imageAtomInputs` current at final bytes;
   legacy metadata refresh lawful and byte-assertions never bypassed.
3. R3's "not verified" register inherited: WebGPU behaviors (blend-on-srgb
   attachment on SwiftShader, device-loss inducibility) are runtime facts
   the gate reads from receipts, never from prose.
4. The image-above-text kind-layer default (Q2) is felt-wave-revisitable —
   the gate confirms it is RECORDED in the phase artifact so the felt wave
   inherits the choice consciously.

Reserved to Sid, unchanged: commits · felt activation · durable-bytes
store (S1) · studio custody / durable artery · any scope change.
