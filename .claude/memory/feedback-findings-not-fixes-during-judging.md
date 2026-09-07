---
name: findings-not-fixes-during-judging
description: While Sid is judging a comparison round, a measured problem is recorded as a finding for the production buildout (receipt, fix shape, state), never built on the comparison branch; "keep filling" does not apply there (Sid, 2026-09-06).
metadata:
  type: feedback
---

During a comparison round's judging, the builder session gathers data and records what it finds; it does not start building fixes on its branch. Sid, 2026-09-06, when the builder had begun the rows-per-item fix after his "this session is doing the job of gathering data and fixing before we do anything in the prod": "wtf have you started doing now?" and then "no this is a finding note it down so when we start doing the production buildout work it gets there".

**Why:** The comparison branches are the lab and the judging scores frozen deliveries; a fix built mid-judging changes what is being judged and spends the session's context on code the production buildout will write in its own form. "Fixing" in his sentence meant finding what is wrong, not landing repairs. [[feedback-keep-filling-never-wait]] governs build-continuation starters, not a judging phase.

**How to apply:** A measured problem goes into `docs/below-the-waist/<kind>/findings-for-production.md` as a row: the finding with numbers, the receipt's path, the fix as designed, the state (landed where, or not). One small fix that a measurement directly exposed and that the trace re-verifies (the pack-key fix, 2026-09-06) was accepted; anything larger is a finding. When unsure, ask in one line before touching code. See [[investigation-fence]] for receipts before verdicts.
