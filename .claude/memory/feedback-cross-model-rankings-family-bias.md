---
name: cross-model-rankings-family-bias
description: "When several sessions rank each other's answers, each model family ranks its own family higher; Codex rankers partition perfectly and replicate one judgment; fix the criterion before the ranker and weight a family's rankings as one vote"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: f1be673a-6079-4c07-b986-7d9c7f5beb5c
  modified: 2026-09-05T11:43:54.937Z
---

Observed 2026-09-05 on the path-kind round: eight sessions (four Codex, four
Claude) ranked all eight answers twice. Codex rankers gave all sixteen top-four
slots to Codex answers in both rounds and produced one ordering four times
(only sibling pairs swapped). Claude rankers gave eight of sixteen to their own
family and three of four put a Codex answer first after reading critiques.
Sid's words: "each model puts its own sibling better than the other model
class and this is much much more so for codex models."

**Why:** each family ranks by a criterion that is its own writing style. The
Codex criterion (no sentence that would be false in permanent code) is binary,
so one found error sinks an answer below any answer that commits to less; the
Claude criterion (positions, receipts, register, something to feel) is graded,
so critique moves it. Both families accept every error once shown, so error
lists survive the bias and rankings do not.

**How to apply:**
- Name the phase and fix the criterion before any ranking; in exploration,
  score framing and the whole picture and put errors on a fix list
  ([[exploration-rank-framing-over-correctness]]).
- Treat a family's near-identical rankings as one vote, not four.
- Use the Codex rule to gate a contract page and the Claude rule to build;
  keep one session of each family open so cross-family reading is structural.
- If ranking again, require each ranker to name one error in its own family
  and one strength in the other, with artifacts opened.
