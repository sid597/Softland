STEP A CLOSED + SID-ACCEPTED 2026-09-03 — Sid: "accepted"; contract baseline `7fe2a7b`; implementation base `d73068e`; `main`.
Commits: A1 `0af9812` engine truths · A2 `76cffd5` text shadows/retention · A3 this commit, region/image/files and S1–S5 tripwires.
Google SwiftShader fallback first (`GPUAdapterInfo.isFallbackAdapter`); max texture dimension `8192`; render `6/6` guards, `7/7` representative + `7/7` DejaVu goldens.
Focused JVM `32 tests / 210 assertions / 0 failures / 0 errors`; moved probe and render-verifier builds each `0 warnings`; 41 sorted source inputs.
T1 tree raw SHA-256 unchanged: `6160b2a793cab2a92a6c9f0485bb0df0836800511ea11a26bd635bf52476be30`; no golden PNG changed.
S1–S5 are frozen at `bin/verify-seam-cuts-step-a`; the retired F1–F3 shadows are not evidence; recorded outputs are.
Tracked removals: the two atlas `.bak` files; oracle/probe custody moved to `src/app/client/harness/region_oracle.cljc` and `test/app/client/harness/shaper_border_probe.cljs`.
Untracked removals: `electric-manifest.edn`, `font_atlas.json.pre-b2-regen.bak`, five `fonts/*.pre-d7.bak` files, and `resources/public/js/`.
Defaults noted: actual checkout had five pre-d7 backups and four scene-equivalent assertions; S3 numeric-literal uniqueness is engine-scoped because other kinds use `0.055`/`2.4` as data. Step B may now boot fresh.
STEP B CLOSED + SID-ACCEPTED 2026-09-03 — Sid: "accepted"; DejaVu uses `dejavu_sans_mono.ttf` through HarfBuzz; the grid layout route is deleted.
Commits: route `cadab66` · re-record this commit; `main`.
Google SwiftShader fallback first: render `6/6` guards, `7/7` representative + `7/7` DejaVu; DejaVu provider named and unresolved glyphs `0` per case.
Fences byte-equal: Ubuntu PNG `085374f8…b6a3`; T1 tree raw `6160b2a7…be30`; DejaVu Slug curve/band `41e7c3e6…91e6` / `160766bd…e7`.
SB2: focused `4 tests / 20 assertions`; affected text `20 / 206`; semantic grid census empty; missing provider throws `:text/layout-provider-required`.
Accepted pair custody: before set removed per contract (recoverable at `0f15a0b`); after `83d13ea8…8dc`; F6 font-only default, face/index metadata added while curve/band stayed byte-equal.
