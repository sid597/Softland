# Executable model of the runtime and store joins (21 November 2026)

Written before any `src-runtime` code, following the precedent of
`src/proposal/formal-model-2026-09-24/`, to replace multi-round prose Q&A on
distributed mechanics with machine-checked falsification.

No Rama, no network, no disk. Plain Clojure (`1.12.4`) and `test.check`
(`1.1.0`) generate random histories of concurrent sign-ins across two stateless
runtime JVM copies (`:c0`, `:c1`), mid-flight `kill -9` runtime crashes,
1-round-trip maker sends and retries, secret rotations, layer `:depot`
re-classing (`:streaming` -> `:microbatch`), agent branch layers, permission
delegation / cascading revocation / re-giving, settings writes, live watches
across `Store` and `Trails`, batched movement leases, and multi-act branch
promotions.

## Run it

```bash
cd src/proposal/runtime-2026-11-21/model
clojure -J-XX:TieredStopAtLevel=1 -M:run 2500 20261121
clojure -J-XX:TieredStopAtLevel=1 -M:run 2500 7
```

## Files

- `src/runtime/model/core.clj`: Pure state machine of two runtime copies and
  three modules (`Store`, `Auth`, `Trails`), parameterized by six explicit
  design readings (`:perm-id`, `:settings-guard`, `:two-module-bootstrap`,
  `:act-wire-id`, `:watch-standing`, `:promote-form`).
- `src/runtime/model/properties.clj`: Seven invariants (`P1` to `P7`).
- `src/runtime/model/run.clj`: Stateful `test.check` generators, configuration
  ablation matrix, and minimal-counterexample trace shrinker.
