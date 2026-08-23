# JVM bench — pick + derive-store-frame at 10/200/2000 slots (2026-08-23)

Run: `clojure -M:test -e '(load-file "docs/electric-native/receipts/2026-08-23-jvm-bench/bench.clj")'`
(the script was authored at a scratch path; it needs only `src/` + `test/` on the classpath).

Modality: JVM/HotSpot, ≥10 warmups, medians of ≥20 reps. The SCALING SHAPE is the receipt;
absolutes are indicative (V8 differs). Slot = 1 container rect + 6 text lines (7 rects, 6 text ops,
13 addresses); all slots in ONE container at distinct y offsets; non-overlapping.

Headline (see bench.out): `derive-store-frame` 0.17 / 1.41 / 16.66 ms (linear, 11.8× per 10×) vs
`upsert-slot` 0.46 / 0.38 / 0.31 ms (flat) → 54× amplification at 2000 slots. `pick` 0.28–0.45 ms at
2000 (sub-linear at these N, unbounded; `maintained-entries` rebuilds the O(N) vector per pick).
Consumed by `docs/electric-native/CONTRACT.md` §10 (b)(c) and `docs/below-the-waist/engine-two-arrows.md` §11.
