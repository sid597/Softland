---
name: feedback-caching-only-on-top
description: Sid's production rule (2026-09-06) — caching is never an option in place of a computation that is right; first the underlying data flow as it should be, each level's inputs explicit, then caching on top only when a trace shows the need; consult before crediting or building any cache, memo, bucket or atlas
metadata:
  type: feedback
---

Sid, 2026-09-06 (verbatim): "caching is never an option it means the underlying thing is as it should be and then still need more performance therefore we nede to do caching on top".

**Why:** the path comparison's Claude lane built its caches as the design (a run memo keyed on what a construction happened to read, a pack memo keyed on a 32-bit hash, an atlas with retention), and both judges first credited that machinery as a virtue; the memo's missing input (the program itself) was the bug the Codex judge found. The Codex lane keyed by the values themselves and got one input wrong the other way. Both bugs are one bug: a memo whose key is not the level's full input. The findings ledger's first fix was another cache; the real fix (the caller naming what changed) was its last sentence.

**How to apply:** first each level of a data flow is a pure function whose inputs are explicit as data, with a test that a change to X reruns exactly level Y and nothing else; a key is the level's full input, never observed reads and never a hash. Caching, buckets, batching, atlases come after a trace on a representative document shows the need, and machinery that already exists is not a credit until then. When reading a findings ledger, sort its rows into the underlying thing and the layer on top before any fix is picked. Landed in `docs/below-the-waist/two-chairs.md`, the production section ([[reference-two-chairs-workflow]]); the Fable reflex to reach for a memo is named in [[feedback-fable-shortcuts-codex-literal]].

**Fired 2026-09-07 (visioning session).** The chair proposed "a derivation cache keyed by reads" for change propagation. Codex caught it against this rule; the engine's own executor docstring already says "no read report is a dependency key" (`src/app/client/engine/executor.cljc:7`). The lawful shape: an index that routes a changed *declared* input to the computations that declared it; reuse and acceptance stay on full-key equality. Consult this file before proposing any index, cache or freshness signal, not only a memo.
