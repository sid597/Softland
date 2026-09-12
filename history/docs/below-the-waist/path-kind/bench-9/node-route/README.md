# The Node route, session 12 — the bench's pure declarations run without a browser

The definer's route (attack 3 and 4): everything before `// ---------- GL plumbing ----------` in `../waist-bench.html` is pure and runs in Node 20. These five files are the harness that produced the Node numbers in `fact-base-12.md` Part 1 and the session 12 section of `../HANDOVER.md`.

```
node extract.js ../waist-bench.html bench.js   # the declarations as a module (bench.js is generated, not committed)
node harness.js                                 # the nested-proof record (nested.json) on a strict CPU host: a release makes the value unreadable
node regress.js                                 # the pickup and attack 3's proofs: consumed paints, releases, the final texel
node wire.js                                    # the checkpoint as bytes: capture → wire → fresh host → resume; the definer's four edits; the controls
```

`harness.js` appends the bench's `normStroke` (it sits after the GL plumbing) and defines the strict adapter over `cpuHost`: `release` marks the value dead by object identity (not by key: two runs on one host repeat keys, which is why the GPU host keys its check on the target), and `sample`, `paint` and `content` refuse a dead value. Before session 12's fix, `harness.js` printed `released 1` and the red proof refused; after it, `released 0` and both proofs readable.
