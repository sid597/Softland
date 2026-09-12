# The Day Job

> LOG extraction — Sid's words verbatim from `vision/LOG.md` (repo root),
> cited by entry; nothing new written here. Group: **what is it for —
> outcome level**. This file owns the oldest specialised purpose: working
> with agents on code — onboard a codebase, make sense of what an agent
> did, review and accept with the reasoning intact. The previous version
> of Softland was built for exactly this; its fullest statements are two
> model-written briefs, cited below as provenance (not quoted as Sid):
> `OLD DOCS/vision/design-brief-reasoning-trails.md` and
> `OLD DOCS/vision/threaded-engineering-workspace.md`. Trims marked […].

## The main line of questioning (LOG ~2026-06-28, notebook pages)

> There are 2 diff problems. code ingestor
> Okay ~~say~~ its ingested then what how am I going to use it? this is the main line of questioning what I am making 2 cases same.
> Import → work with llm on code → check what maps to what and then accept or reject code changes.
> ↓
> Like the goal of this whole exercise is to make the user/reader onboard the codebase and make sense and work with it,
> ↓
> In this case ~~what~~ how do we show the thread of divergence, and committing etc. Basically the goal is to have a much better view and control of the flow:
> Existing state → Existing code —(agent works)→ Diff → code diff ↔ User facing diff (chat to fix, circular)
> New request for feature or bug → (feeds back into Existing code)
> And at the end do the standard commit push deploy etc. For this task what I care about is the view of underlying flow and hence the best way to store data

## Code is the richer world's spine (LOG ~2026-06-12)

> So referencing the code in some context is not only for the agent .. code is part of any software dev cycle, it is directly related to product, design, project management in the company, discussing future, making quick mockups, branches and forks are what basically a different version of the product ... so this is a much richer world in softland and code is very imp  part of it ... .

## The sense line (LOG 2026-07-12, notebook re-derivation)

> These arrows between files did not communicate anything when I looked at them in design. So started thinking and analysing why, it was due to not able have granular structure whole files communicate nothing what is needed is a sense line not the artifact got created when pov.
