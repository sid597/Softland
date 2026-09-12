# P5 gate — bindings + dispatch — PASS

2026-07-25 · fresh Fable session. Gate object: `faab5f9` (code-only, local
main line, unpushed): 15 files, +3,083/−299. Spec: PROMPTS.md §P5 under
DIRECTION §Four-stations + CAMPAIGN §P5. Diff read in full.

## Receipts (this session, independently re-derived)

- **Full suite re-run: 47 ns / 414 t / 5,728 a / 0 / 0** — above the
  397t/5,416a floor, exact match with the implementer's banked numbers.
  `dogfood-llm-probe` flaked once and cleared on its registry-protocol retry.
- **cljs cold compile** (`:cache-level :off` — a cache hit re-derives
  nothing): 270 files, 198 compiled, **0 warnings**.
- **Live floor drill** (real cluster + dev server; headless Chrome, adapter
  attested FIRST: SwiftShader fallback — state receipts, no pixel claims):
  `__bindings.drillAll()` **PASS ×3 masters, zero deviations** across
  live / absent / malformed / valid-tap-stripped worlds; the tier column is
  honest (a floored master's probes report `:floor`, other facets' probes
  untouched).
- **Served interaction table: 20 rows / 0 conflicts** from the server
  projection AND from the client tiers — row-for-row parity on every
  semantic field (gesture, phase, modifiers, site, tier, facet, verb,
  effect-class, priority). Only divergence: floor-row *labels* (client
  `code-floor:attention`/nil vs server `code-floor:fm:attention:v1` both
  fields) — cosmetic, unification owed at the next table touchpoint.
- **Families 1–3 clicked live (gap 2 closed):**
  - fold-header tap: run block h 97→1,312→97 through the header hit nodes,
    focus untouched — exactly the deleted row-arithmetic's behavior;
  - block tap → `:focus/place-caret` (mode `editing`); empty tap →
    `:anchor/place` blurs; **shift-press focuses AT DOWN** (Task 11), all
    console-receipted through the one law with tier printed;
  - drag: subject-verified `:placement/drag-group` at `:master`, displaced
    exactly +40/+40, HELD (no snap-back through +1,450 ms), settle accepted
    (**HTTP 200** — the durable-via-request class proven end-to-end), then
    restored;
  - space: pan/zoom resolve `:camera/pan` / `:camera/zoom-at-pointer` at
    `:floor` `subject=:space`; camera moved by the exact stroke and was
    restored exactly; wheel zoom reciprocal-exact.
- **Echo bar measured (gap 1 closed — P5 had only argued it):** 42 live
  typing samples through the real edit lane: **max 31.6 ms, p95 17.3,
  p50 14.9 — 0 of 42 over the 52 ms bar**, on CPU-raster SwiftShader (the
  real GPU environment is strictly faster). Typed text byte-restored.

## Falsification

- **Axis 1 — can any VALID revision make a gesture disappear?** Resolution
  is total: floor rows derive from the facet SPEC at compile time; the
  drill (suite + live, same function) proves live/absent/malformed/
  valid-stripped/v0 all resolve identically. BUT a valid revision CAN
  rebind a gesture to an **arg-starved verb** (JVM probe:
  `:fold/toggle-section` at `:block/user-hit-area` → `:claimed`, args `{}`
  → the impl no-ops totally). Not a floor break — the declared rebind power
  reaching a degenerate target; visible in the table, reversible, but
  lint-silent. Closed in CONTRACT_P6 (T10: registry required-args).
- **Axis 2 — does the containment fallthrough reproduce every pre-P5
  branch?** Every branch of 13f1dac's `pointer-down!/move!/up!/
  handle-wheel!` (byte-identical to the pre-P5 parent in those functions)
  enumerated and matched to a row or named mechanism — **parity exact**,
  including: threshold-consumes-press, tap accepts any modifiers,
  machine-silence at press/begin, marquee/pan/drag continuations, and the
  selection-anchor re-read after the press dispatch (the implementer's
  load-bearing `reread-confirmed-caret` note verified against the old
  ordering). The old fold arithmetic's `(max 0 …)` clamp is DEAD code on
  the real geometry (root bounds exclude top/left padding from the pick;
  the header nodes carry the padding-inclusive width, matching the old
  x-ignorance on the right/bottom bands). The `:anchor/place`
  preserved-as-found finding is honest — the old code's focus-read also ran
  after `set-anchor` cleared it; the old comment lied, behavior identical.
- **Axis 3 — is `:meaning-branches 0` honest?** Yes, for the dispatch
  surface. Meaning moved to declared, inspectable places: binding rows
  (material) · the claim SITE baked at render from what the block IS (the
  one `machine?` if, visible in the claim data) · the `fold-sections`
  render grammar · verb implementations behind the closed registry. The
  threshold's continuous-vs-discrete branch keys on the verb's DECLARED
  continuation shape — mechanism by the budget's own definition. The suite
  greps the migrated ladders out of the source and pins the branch counts.

## Findings (non-blocking, owners named)

1. **Instance rows at `[:space :space/ground]` shadow pan/wheel** at the
   `:instance` tier (JVM probe: pan→`:selection/marquee-begin`,
   wheel→`:anchor/place`). Unreachable from the shipped console seam
   (string subject ≠ keyword `:space`) and client-ephemeral — but the
   registry's "no data revision can … shadow the camera" overstates.
   One-line refusal → CONTRACT_P6 (instance-row ownership lands there).
2. **Verb arg-compatibility is outside the grammar** (axis-1 residue) →
   CONTRACT_P6 T10/G9.
3. **Floor-row label divergence** client vs server (above) — cosmetic.
4. **Legal-but-dead rows:** a block-site `:wheel` row can never fire
   (`handle-wheel!` passes `:hit nil` by design, which is also WHY no block
   revision can shadow zoom). The grammar refuses dead-modifier rows but
   not this class — either refuse wheel outside `:space/ground` or accept
   documented deadness → CONTRACT_P6 note.
5. **A third deterministic activation timestamp** (`time-ms 2`) shipped in
   the P5 ingest, joining P3's 0/1 — causal as-of unaffected (P4's
   parent-chain law), but the honest-times mandate now covers three
   precedents → CONTRACT_P6 R3.
6. Tap caret is computed from press-time truth (pre-P5 read up-time truth
   at press-time coords) — indistinguishable unless truth changes
   mid-click; recorded for honesty.

## Session honesty — the probe trail

- The gate prompt's warned failure class **fired in this session's own
  first navigation driver** (a stroke-start viewport adjustment pressed
  onto blocks; three blocks dragged client-side). Forensics — the durable
  camera cell origin-exact (every stroke armed the camera, so ANY flush
  would have written a non-origin camera), the 400 ms settle debounce never
  quiet between synthetic strokes, and Puppeteer `browser.close()` never
  running `beforeunload` — proved **no settle fired: zero durable damage**;
  the ephemeral displacement died with the page. The re-driver verifies
  every stroke start against live rects and fail-fasts on any block move.
- Deliberate durable traces retained (P4 probe precedent): ep:5c962bf4's
  geometry cell was rewritten by the family-3 drag + repair — final value =
  its original with a 5.7e-14 x residual (one double ulp; y exact); its
  last-write metadata carries this session's settles. The echo probe typed
  and deleted in the same block; truth text byte-restored. The camera cell's
  final settle wrote `{0,0,1}` — its pre-session value. `conflicts()` = []
  throughout; no other durable deltas.

## Verdict

**PASS.** The four stations hold as built: meaning lives in served rows,
ONE pure law dispatches them, the code floor is unbreakable by any data
revision, and both honestly-named gaps are closed with receipts.
`CONTRACT_P6.md` is authored beside this file — the owner-condensation is
pre-adjudicated there and every finding above lands as a trap or gate.

## Campaign state

P1–P5 CLOSED; Gates 1+2 closed; Gates 3+4 = P6. Next: Sid sends PROMPTS.md
§P6 (CONTRACT_P6.md now exists, so the §P6 precondition is met). Sid's open
one-liners: **"P5 ok"** · "kinds ok" · `__material.picked()` click · headed
⌁/≈ look. Ops: cluster left as found (five modules RUNNING); the dev app
this session booted is shut down again (board said "dev app down").
