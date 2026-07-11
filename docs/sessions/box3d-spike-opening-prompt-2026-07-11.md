# box3d-spike — opening prompt (2026-07-11)

**Session name:** `box3d-spike` · **Model: Opus 4.8** · ~half a session, de-risk only.
Everything happens OUTSIDE the repo (scratch dir). No Softland code is touched.

## The question

Sid wants Box3D (Erin Catto's new 3D rigid-body engine — v0.1.0, portable C17,
docs at https://box2d.org/documentation3d/) available to Softland eventually
(canvas physics / 3D islands rung 2). Before anything is planned around it:
**does it actually build to wasm today, and is it deterministic?** A clean
FAILURE report is a fully successful outcome — it gates the lane honestly.

## Steps

1. Locate the real repo from the docs site (likely github.com/erincatto — verify;
   v0.1.0 is weeks-old, URLs may have moved). Record commit hash.
2. Native build first (cmake): confirm it builds + samples/tests run. Note any
   SIMD/AVX flags that could complicate wasm.
3. Emscripten build (`emcmake cmake`): produce a wasm + minimal JS harness that
   steps a small falling-boxes world (e.g. 20 bodies, 1000 steps).
4. Report: builds? wasm size? steps/sec (single thread)? API surface sanity
   (world create / body create / step / read transforms from JS)?
5. **Determinism probe:** same seed/config run twice → byte-identical position
   traces? Note native-vs-wasm divergence if visible. (This decides whether a
   simulation can be a REPLAYABLE trail — it matters more than speed.)

## Output

- `build/box3d-spike/REPORT.md`: findings, exact failure text if it fails,
  a one-paragraph verdict line: BUILDS+DETERMINISTIC / BUILDS+NONDET / BLOCKED.
- Board: flip the `box3d-spike` thread line (pointer + status only).
- Docs commit on `docs/current-mental-model-local`. Nothing else committed;
  build artifacts stay in scratch.
