# Below the waist — NOW (2026-08-28)

Status: **CLIENT KIND FOLD CLOSED + SID-ACCEPTED 2026-08-28** (his words: "ok so that client side work is done"). Prior accepted waist, instance, text-renderer, and conductor cuts remain closed.
Authority: Sid/Fable's 2026-08-28 map plus amendments 1–2; ownership only, no design or adjacent cleanup.
Tree: 23 source files became exactly 25 under `engine/`, `text/`, `image/`, `path/`, `region3d/`, `verifier/`; `substrate/`, `workspace/`, and `renderer.cljs` are absent.
Body evidence: 34 whole-file/test comparisons and all 93 renderer top-level forms are byte-identical after only namespace/alias substitutions.
Dependency evidence: zero engine→kind; zero kind→other-kind outside `region3d/placement.cljc` → text layout + path component/tessellation.
Clause placements: `image-instance-stride` → image; `slug-text-instance-stride` + `create-instance-buffer` → text.
Region placement: compositor cache/ensure/replace forms → `region3d/region3d_gpu.cljs`; generic clip projection → `engine/device.cljs`.
JVM evidence: 48 tests / 419 assertions / 0 failures / 0 errors.
Render evidence: text 5/29; instance check; release 83 files / 0 warnings; W0-A cases + five guards + seven DejaVu Slug and three representative goldens green.
Path evidence: zero exact old namespace/source/test references; SHA inputs, Shadow init, test runner, probes, and `.mjs` checks point at new ownership.
Diff evidence: `git diff --check` and new-file whitespace checks green.
Ownership: no server/shared/resources/Rama/archive path, data migration, push, merge, or foreign `.claude/memory/` mutation entered the step.
Docstrings: every client namespace header rewritten in plain words (what it is · Takes · Gives · Holds), bodies untouched — `26be7d8`; evidence: verify:render-engine chain green in 27 s (83 files / 0 warnings, render test harness pass, all guards), focused client suite 48 tests / 419 assertions / 0 / 0.
Debt (foreign, pre-existing): `clj -X:test` (the full runner) fails at `assert-inventory!` — `app.tools.export-current-data-test` was never classified into a tier (since `b86a5d2`, 2026-08-24); every session since has run focused suites. One-line fix in `test/app/test_runner.clj`, not part of any client step.
Open: Sid's 3D line — entities wear 3D components, or the land contains 3D windows? (decides the `?` cells: region3d/component, scene, evaluation). Next: the same sort over `src/app/server` + `src/app/shared` (starter handed to Sid 2026-08-28).
Next round: the waist, round two — `history/docs/seam-cuts/` (CONTRACT.md is the position; NOW.md is the ladder state).
