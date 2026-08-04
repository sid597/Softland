# shaping-correction — recut ledger (R1 → R2 candidate)

Recut 2026-08-04. `VALIDATION_R1.md` is the immutable FAIL record; this
ledger maps its §10 items 1–10 to where the recut CONTRACT.md consumes each.
All accepted laws (R1 §9's preserve list) and all foreign work are
untouched; no source, evidence, or existing verifier was edited.

1. **§5 whitespace/representation/headers/no-cluster/reference-advance** →
   CONTRACT §5.1–§5.7: per-line `:text`/`:source-range`/`:consumed-range`
   representation (consumed always a suffix; paint range derived);
   break-whitespace pinned to {U+0020, U+0009} (new trap T14 — JVM/CLJS
   `\s` NBSP divergence); cut-selection law with the painted-prefix fit
   test, whole-run consumption, trailing-whitespace-is-painted rule, and
   the `"abc   def"` worked example; reader law (caret/hit/selection/copy)
   across consumed ranges; headers = unwrapped synthetic prefix, tagged
   `[:header i]` domain, shaped once each; nonempty-text/empty-cluster =
   provider fault → poisoned-but-TOTAL result + `provider-fault` counter
   (never code-unit-1 surrogate splits); reference-advance = one U+0020
   shape per key with the body's full shape options, in the §6 key.
2. **Wrap semantic scenario table in the JVM gate** → G1: fourteen semantic
   cases with exact text/range/caret/selection/shape-call assertions,
   alongside the retained linearity counters; both seeded negatives
   (quadratic filter; full-vector scan) prove the gates can fail.
3. **§6 totality + exact projections + delta scenarios** → CONTRACT §6:
   total key constructor (stamp × visual-projection token when stamped;
   value hash × source address × op role otherwise) covering optimistic
   (`block-view` ground_edit.cljc:445), paste (`paste-projection` :121 —
   locator corrected), machine fold display/header copy, auxiliary prims,
   anatomy strings, and all unstamped text; "foldable wear" replaced by the
   exact visual projections (defaults-as-applied, header-copy, paste
   inputs) with bindings/stamps/whole-wear excluded; "the rebuild `sig` is
   never a layout key" stated, enforced by the key fn's signature; live
   delta scenarios landed in G4 (a)–(g) + G5's order probe; binding-only
   reuse is a G1 JVM key-law assertion; cache hit/miss/eviction receipts in
   G4 (g) with the new `:id-digest` field.
4. **G4 lifecycle correction** → G4 case (e) verbatim per R1: backend flip
   = layout 0 + identical layout IDs (id-digest receipt) + backend
   paint-repack + geo reclone/destroy; case (a) is capacity-controlled
   (same-glyph-count edit); buffer growth is its own case (f) with a
   capacity-grown receipt. I6/§7-step-3 wording aligned.
5. **One storyboard cardinality** → §9 defines THE seven-transition
   storyboard (unmeasured warm-up, reset, six measured ordinary↔largest,
   measured ordinary→empty control, exactly one RAF/G8 pair each); §7 step
   4, G3, and G9 all reference that single definition. The banked
   four-transition capture stays the per-transition comparison baseline.
6. **Pinned actions/windows/commands/environments; constants bound** → §10
   environment law (machine/attestation-first/Chromium≥150/800×601 DPR
   1.046875/`:dev` build/profile policy/corpus/counter windows/settle
   definition/120s timeout/cross-run comparison policy); G1 constants k=4,
   growth c=5, sort allowance G·⌈log₂(G+1)⌉, work-units = exact six-counter
   sum; G6 span bound +8 in glyph-index units, line visits ≤2; every gate
   names its command and expected exit; harness exit law 0/1/2 in §9.
7. **G10 rewrite** → G10 item 5 pins `npm run verify:render-engine` at
   expected exit 1 with the full W1 §9.1 state (classification
   `candidate-pick-parity-failure`, 21/21 determinism, 21/21 golden, 7/7
   sentinels, 14/21 parity, SDF 7/7, Slug 7/7 + two byte-128 ties/regime,
   MSDF 7/7 RED with 47/regime = 329 + two separated ties/regime); focused
   suites named as namespaces with the runner form; compile command pinned
   with a phase-open warning baseline; golden policy = R1's preferred
   option (21/21 absolute, any diff FAIL) with new stop S6 for
   adjudication — never both byte-identical and passed-by-exception.
8. **Experience-bar-red terminal classification** → §10 terminal
   classifications: PACKAGE PASS / PACKAGE FAIL / LINEAR-CORRECTION GREEN
   · EXPERIENCE-BAR RED — STEP-5 RULING REQUIRED (bank receipts, no
   further code change, phase technically green for G1–G7+G10, Fable/Sid
   ruling opens, SEAM felt-gate closure does not proceed) / UNCLASSIFIED
   (environment) for fallback-adapter or mismatch runs — no bar verdict on
   a non-matching environment (cross-run comparison policy).
9. **T13 route named, §12 unwidened** → §7 step 2 + trap T13: `ground
   !refs → :atoms → :!active-font → :layout-provider → metrics/:geom →
   ground-block-layout`; never renderer state, never a second shaper.
   §12's allowlist text is unchanged (the fence script name is now
   concrete, within the already-granted MAY CREATE).
10. **Fresh round** → §14.1: R2 runs as a wholly new default-fail round
    over the recut, landing as `VALIDATION_R2.md`; R1 stays immutable.

Manifest note: §11 gains the recut-cited locators (visual-lines /
source-line-records / whitespace-at?; auxiliary prims; metrics / run-view /
current-block-render-inputs / context-blocks / install-ground! /
block-death / verb / watch sites; backend-flip road; pack fns; a READ-ONLY
context list incl. ground_edit.cljc — where `paste-projection` :121
corrects the R1-era hint — block_edit_wiring.cljs, runtime/state.cljs,
runtime/fonts.cljs, foldable_material.cljc, package.json, shadow-cljs.edn).
§3's hover sentence now states the banked four-transition capture exactly.
