# Below the waist — NOW (2026-08-28)

Status: **CLIENT KIND FOLD CLOSED + SID-ACCEPTED 2026-08-28** (his words: "ok so that client side work is done"). Prior accepted waist, instance, text-road, and conductor cuts remain closed.
Authority: Sid/Fable's 2026-08-28 map plus amendments 1–2; custody only, no design or adjacent cleanup.
Tree: 23 source files became exactly 25 under `engine/`, `text/`, `image/`, `path/`, `region3d/`, `verifier/`; `substrate/`, `workspace/`, and `renderer.cljs` are absent.
Body receipt: 34 whole-file/test comparisons and all 93 renderer top-level forms are byte-identical after only namespace/alias substitutions.
Dependency receipt: zero engine→kind; zero kind→other-kind outside `region3d/placement.cljc` → text layout + path material/tessellation.
Clause placements: `image-instance-stride` → image; `slug-text-instance-stride` + `create-instance-buffer` → text.
Region placement: compositor cache/ensure/replace forms → `region3d/region3d_gpu.cljs`; generic clip projection → `engine/device.cljs`.
JVM receipt: 48 tests / 419 assertions / 0 failures / 0 errors.
Render receipt: text 5/29; instance fence; release 83 files / 0 warnings; W0-A cases + five guards + seven DejaVu Slug and three representative goldens green.
Path receipt: zero exact old namespace/source/test references; SHA inputs, Shadow init, test runner, probes, and `.mjs` fences point at new custody.
Diff receipt: `git diff --check` and new-file whitespace checks green.
Custody: no server/shared/resources/Rama/archive path, data migration, push, merge, or foreign `.claude/memory/` mutation entered the atom.
Docstrings: every client namespace header rewritten in plain words (what it is · Takes · Gives · Holds), bodies untouched — `26be7d8`; receipt: verify:render-engine chain green in 27 s (83 files / 0 warnings, verifier pass, all guards), focused client suite 48 tests / 419 assertions / 0 / 0.
Debt (foreign, pre-existing): `clj -X:test` (the full runner) fails at `assert-inventory!` — `app.tools.export-current-data-test` was never classified into a tier (since `b86a5d2`, 2026-08-24); every session since has run focused suites. One-line fix in `test/app/test_runner.clj`, not part of any client atom.
Open: Sid's 3D line — entities wear 3D facets, or the land contains 3D windows? (decides the `?` cells: region3d/material, scene, evaluation). Next: the same sort over `src/app/server` + `src/app/shared` (starter handed to Sid 2026-08-28).
