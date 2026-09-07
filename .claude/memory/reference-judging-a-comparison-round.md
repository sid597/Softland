---
name: reference-judging-a-comparison-round
description: What a fresh judge session of one family did that worked when two lanes were compared (path kind, 2026-09-06) — numbers only from that lane's client run here, scratch detached worktrees for experiments, the other judge's file read only after marks are set, the green-flag trap, the worker lens beside the card; consult before judging any paired delivery
metadata:
  type: reference
---

The path comparison (2026-09-06) was judged by one fresh session per family with the same prompt; the two files and their receipts are `docs/below-the-waist/path-kind/judge-claude.md` and `judge-codex.md`. What held:

- Every number cited was produced here from that lane's client at its frozen hash (its own runner, then the repo's own verifier and test lane on the same build, which is where one lane's red showed). Probes on the pure layer as `clj -M:test -i <script>` from the lane's worktree, read-only; the scripts land beside the file.
- Experiments that change code run in a detached scratch worktree of the frozen hash (`git worktree add --detach`, `node_modules` symlinked), never in the builder's worktree, and are removed after; one changed argument located a cause and the fixed picture was the receipt.
- The other judge's file is read only after the marks are set; then its findings are verified on code before any mark moves, and a section says where the files still differ and on what. Both judges lowered marks on the other's receipts; the criterion beat the family bias.
- The green-flag trap: a harness predicate that passes on "one placement resolved, positive vertex count" hid half a stroke; only the pixel diff against the golden and the repo verifier showed it. A pass flag is never a picture; ask what the predicate reads.
- A fresh Sonnet session per lane making one small change from the READMEs, in a scratch worktree, answers the card's "can someone else continue" row in minutes.
- Sid's reframing after the card: the delivery is evidence about the builder (judgment where the prompt was silent, self-report against the code, what it did with a red check), not only about the product; the correctness audit must not crowd that out. See [[feedback-fable-shortcuts-codex-literal]].
- Judge files freeze after the exchange; the production ground goes to `two-chairs.md` under Sid's word, not into a growing judge file ([[feedback-guides-integrate-never-accrete]]).
