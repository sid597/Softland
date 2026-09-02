Shared baseline: `eb1e367` on `main`.
Shared closing commit: this commit; engine `3abea77`, split `156b950`, manifest `a4199c7`.
Shared verifier receipt: PASS — `14 tests / 87 assertions / 0 failures / 0 errors`; render `6/6` guards, `5/5` representative + `7/7` DejaVu goldens; 42 sorted source inputs; Google SwiftShader fallback (`GPUAdapterInfo.isFallbackAdapter`).

## Text
Question: T1 requires a ninth replayed Slug golden, but §8 forbids text from editing the shared runner that hardcodes seven DejaVu cases plus one Ubuntu case; does runner custody admit the minimal text-golden registration, or does T1 close on the returned deterministic tree receipt instead?
Ruling (Fable, 2026-09-02): custody stands; T1 closes now on the returned tree receipt (deterministic hash `6160b2a7…be30` + the placement check) as its tripwire; the golden PNG and its runner row are registered by the last closer at package close, one bounded block beside F7's. Contract §4d and §7 T1 amended.
TEXT SOURCE FROZEN + RECEIPT PASS — source `67d9a68`; T1–T3 are tripwires; Sid acceptance pending.
T1: PASS — banked SwiftShader tree raw SHA-256 `6160b2a793cab2a92a6c9f0485bb0df0836800511ea11a26bd635bf52476be30` is deterministic; word-24 slots `[1 1]`; missing/unknown containers refuse. Ninth PNG/row remains with the last package closer per ruling.
T2: PASS — carried fallbacks `0`, uncarried `1/op`, call-local; removal scans clean; focused text suite `26 tests / 349 assertions / 0 failures / 0 errors` and both text fences pass.
T3: PASS — shared legal-zoom bounds hold; the eight existing Slug fixtures and runner are untouched by the source diff; isolated text-verifier CLJS compile passes (one pre-existing foreign warning).
Custody (`git diff --name-only 67d9a68^..67d9a68`): `text/{glyph_pack.cljs,layout.cljc,layout_oracle.cljc,painter.cljs}`, `verifier/text.cljs`, `text/flat_road_test.clj`; foreign `.claude/memory/` dirt preserved.

## Region
BUILT + RECEIPTED; SID ACCEPTANCE PENDING — `a2a0a39`, `5c9e313`, `66e22bb`, `c6c96fc`.
R1–R5: PASS — Region3D JVM `15 tests / 109 assertions / 0 failures / 0 errors`; render `6/6` guards and `6/6` representative goldens.
Custody: frozen Region artifact set unchanged through shared specimen `c6bd601`; removal greps clean; foreign `.claude/memory/` dirt preserved.

## Image
SOURCE FROZEN + RECEIPT PASS 2026-09-02 — I1–I5 are tripwires; Sid acceptance pending.
Commits: runner `eaa4832`; grammar `e718443`; frame `372f1c7`; painter `f9bbc83`; verifier/golden `dfe0ebe`.
JVM: PASS — `7 tests / 62 assertions / 0 failures / 0 errors`.
Render: PASS — SwiftShader; `6/6` guards; all representative + `7/7` DejaVu goldens; 44 source inputs.
Image: PASS — 22 records, 14 parity rows, I3 gate/residency, I4 color, I5 rebuild/refusal; removal fence 0.
I1 golden: `e1d93240…f3b7e5` raw; `58be6105…cf59e` PNG, manifest-matched.
Note: `:writes` is the frame update count; `:item-writes` preserves the measured per-instance GPU write count (2 on the two-op first frame).
Custody: `image/{frame,material,painter}`, `verifier/image`, their two JVM tests, test-runner line, image manifest row, I1 PNG.
