# Seam cuts — atom 1 path seam close (2026-09-02)

Status: **CLOSED + SID-ACCEPTED 2026-09-02.** Sid: "this session work for row 3 is accepted." Code `9caa2ab` on `main`; source baseline `1750489`.
S1 tree: cid 17 resolved to slot 1 under `[0.5 0 0 0.5 40 20]`; missing cid refused `:transform/unknown-group` before upload; new golden PASS.
S2 travel: both fixtures survived EDN round-trip with nil metadata, equal content keys/classification/mesh bytes; seven grammar refusals named in data.
S3 gate: frames 1–4 returned writes/derived `1/2, 0/0, 1/0, 1/2` for first/equal/revision/regime changes.
S4 width: the two prior mesh SHA-256 fingerprints stayed exact; tapered `(8,3)` classified inside with boundary distance `2.2`.
S5 color: the painter-local preamble is gone; legacy-direct + linear-premultiplied receipts and the holed/translucent goldens PASS unchanged.
Receipt: `9 tests / 62 assertions / 0 failures / 0 errors`; `npm run verify:render-engine` PASS, 6/6 guards; diff check and removal scan clean.
Adapter: Google SwiftShader · architecture/renderer `swiftshader` · fallback true (`GPUAdapterInfo.isFallbackAdapter`).
Goldens raw SHA-256: holed `645f9536…410b` · translucent `5bf612e2…ea79` · tree `ca96286b…53ba`.
Changed from `git diff 1750489..9caa2ab`: engine `device, schema`; path `frame, material, renderer, tessellation`; verifier `core`.
Tests/tooling: path `fixtures + frame/material/tessellation tests`; tree PNG + manifest; `test_runner.clj`; `run_verifier.mjs` (15 exact paths total).
Foreign untouched: six `.claude/memory/` status entries; known full-suite tools-test debt not re-run by this focused close. No fork surfaced.
PENDING 1 done 2026-09-02: `docs/decisions.md` "The render seam" rewritten from CONTRACT §0; PATH-SHOULD-BE reconciled; PATH-ATOM-CONTRACT and W0-C §6 carry supersession lines.
