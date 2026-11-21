# `auth-blind` — Blind Property Suite for Auth's Contract

Written solely from [`src/proposal/runtime-2026-11-21/spine/auth/SPEC.md`](../../spine/auth/SPEC.md) ("The client's contract", "Rules for every call", "What each write does", "What takes a thing out of what is live", "What the runtime does, and Auth does not check", "What must hold at full life") plus tonight's `:driven-by` addition on `open-session`.

## Run

From this folder (`src/proposal/runtime-2026-11-21/overnight-audit/auth-blind`):

```sh
clojure -M:run
```

## What is here

- `src/auth_blind/generators.clj`: `test.check` generators for 18-call histories (`open`, `close!`, `invite`, `enter`, `own-layer-made`, `begin-sign-in`, `replace-secret`, `end-sign-in`, `make-actor`, `end-actor`, `open-session`, `close-session`, `ask`, `answer-ask`, `who-is`, `ask-of`, `under`, `watch-root`), with repeated writes across two clients, advancing `:at` times, `:driven-by` options, and invalid inputs.
- `src/auth_blind/properties.clj`: 23 properties, one for each thing the spec says must hold, each named with the spec's own sentence. `run-suite` accepts any Auth implementation as a map of the 18 keywords to functions.
- `src/auth_blind/mutants.clj`: 27 broken variants, one contract rule broken at a time, all caught by the suite.
- `src/auth_blind/model.clj`: pure in-memory spec model used only to check that the suite is satisfiable and to derive the single-rule broken variants.
- `src/auth_blind/run.clj`: CLI runner (`clojure -M:run [seed]`).
