Shared baseline: `eb1e367` on `main`.
Shared closing commit: this commit; engine `3abea77`, split `156b950`, manifest `a4199c7`.
Shared verifier receipt: PASS — `14 tests / 87 assertions / 0 failures / 0 errors`; render `6/6` guards, `5/5` representative + `7/7` DejaVu goldens; 42 sorted source inputs; Google SwiftShader fallback (`GPUAdapterInfo.isFallbackAdapter`).

## Text
Question: T1 requires a ninth replayed Slug golden, but §8 forbids text from editing the shared runner that hardcodes seven DejaVu cases plus one Ubuntu case; does runner custody admit the minimal text-golden registration, or does T1 close on the returned deterministic tree receipt instead?
Ruling (Fable, 2026-09-02): custody stands; T1 closes now on the returned tree receipt (deterministic hash `6160b2a7…be30` + the placement check) as its tripwire; the golden PNG and its runner row are registered by the last closer at package close, one bounded block beside F7's. Contract §4d and §7 T1 amended.

## Region
BUILT + RECEIPTED; SID ACCEPTANCE PENDING — `a2a0a39`, `5c9e313`, `66e22bb`, `c6c96fc`.
R1–R5: PASS — Region3D JVM `15 tests / 109 assertions / 0 failures / 0 errors`; render `6/6` guards and `6/6` representative goldens.
Custody: frozen Region artifact set unchanged through shared specimen `c6bd601`; removal greps clean; foreign `.claude/memory/` dirt preserved.
