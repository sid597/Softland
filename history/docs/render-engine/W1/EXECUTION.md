## 9. W1 execution receipt and downstream gate

### 9.1 Mandatory RED replay

Command run at W1 settlement:

```text
npm run verify:render-engine
```

Result: expected exit `1`, classification
`candidate-pick-parity-failure`; determinism `21/21`; golden comparison
`21/21`; current-product bounds-divergence sentinels `7/7`; candidate parity
`14/21`. Across seven zoom regimes, SDF passed `7/7`, Slug passed `7/7` with
two declared byte-128 ties per regime, and MSDF failed `7/7` with 47 decisive
mismatches per regime (329 total) plus two correctly separated ties per regime.
Environment fingerprint (2026-08-05 canonical-set amendment: adapter
features are sorted before environment identity is minted):
`5ced2482f3d0b8e9a14465bba06808a04d3a5a9b7d386fcf71f40a30f557343e`.
The amendment changes environment identity only; the 21 image bytes and every
geometry/parity receipt above remain unchanged.

This is the required state after paper settlement. W1 specifies the repair; it
does not alter production code, verifier code, fixtures, goldens, or thresholds,
and therefore does not pretend the machine rung is green.

### 9.2 Road gates after W1

1. **Q5 remains halted** until W2-A's affine implementation repairs or
   canonically reclassifies the two pinned pixels and replays them. Affine
   transport lands in that same migration, per Q8; no second transport rewrite
   is allowed after atom multiplication.
2. **Q6 semantics are settled** by §4.4. Any durable implementation receipt must
   retain the exact 8/16 → byte-128 case and prove that pick reads mathematical
   classification rather than decoded-alpha `>= 0.5`.
3. **MSDF remains RED** until repaired under Contract G or retired by the
   pre-registered Slug road receipt. Retirement does not delete or bless the
   permanent counterexample.
4. **Q2 binds every family:** no implementation receipt passes without a
   complete precision/backend regime matrix over legal zoom `[0.01,1000]` and
   tested extents/normalizations.
5. **No new atom lands before W2-B** makes registration, ordered tape, geometry,
   color/alpha, and admission validation code-real for the existing families.
   Image/path registration is the first falsifier of that seam, not permission
   to add another central draw branch.
6. **T0 proceeds in the parallel CPU lane** through Contract T. T1 shapes text
   through the same seam. Neither package may smuggle in a text-road decision.

### 9.3 Next execution front

The next front is **W2-A + T0**:

- W2-A migrates affine/nesting and Q8 transport once, including bounds, clip,
  cull, pick, reuse, Slug `inv_jac`, declared overflow, and the Q5 repair;
- T0 consolidates every text-metric owner behind the one layout result with
  behavior-identical receipts and an executable no-independent-metrics fence;
- W2-B follows at the same migration window's tail, making Contracts O/G/M/C
  code-real for the existing families before image or path exists.

The campaign remains one committed full-breadth campaign through the seam demo.
Only roads are now under test.
