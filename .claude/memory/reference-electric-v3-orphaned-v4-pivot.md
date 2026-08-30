---
name: reference-electric-v3-orphaned-v4-pivot
description: Hyperfiddle left Clojure for a JS-embedded Electric v4 (WASM server runtime); v3 is a frozen proprietary alpha — never propose re-adding it as a dependency; the transfer bench's v3 candidate is dead
metadata:
  type: reference
---

Hyperfiddle announced (Slack, pasted by Sid 2026-08-30) that Electric v4 is a
JavaScript-embedded language with a portable WASM server runtime reaching JVM
backends — "We're leaving Clojure." No public trace found (searched
2026-08-30); the paste is the only source. Electric v3 is proprietary: "free
for bootstrappers and non-commercial use, but is otherwise a commercial
project" (README, checked 2026-08-30). Softland's pin was
`v3-alpha-SNAPSHOT` (resolved 20260519), removed at the waist cut (adc30c9,
2026-08-20) with the whole product client; the tree has zero Electric
requires. Missionary is EPL-2.0 (open), b.47 latest, we pin b.46.

**Why:** the electric-native return arc (`docs/electric-native/CONTRACT.md`
§9.1) holds "Electric's keyed machinery at the transfer bench" as the
courier-replacement trigger and lists "re-add Electric now" as a lawful
counter-position. Both now point at an orphaned, unforkable alpha.

**How to apply:** never propose re-adding Electric v3 as a foundation; the
owned SSE + Missionary courier with the frozen feed format is the position.
"Electric-native" / "the correct Electric" is Sid's name for the SHAPE (two
arrows: changes by id ↓, demand ↑), not the library — never rename it for
him. v4 is at most a future courier candidate (open? JVM-embeddable beside
Rama? keyed diffs to a non-DOM target?) — unknowns until it ships. Also
unknown: v3 maintenance, v4 license/timeline, Missionary's pace under the
pivot. Kin: [[feedback-preserve-sids-vocabulary]].
