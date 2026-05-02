# Implementation Prompt -- Slice A Compute Run Command

Status: task handoff prompt, 2026-05-02.

Use this only after the user explicitly chooses to implement Slice A. It turns
the architecture in `architecture/dogfood-runtime/slice-a-compute-run-command.md`
into a planning-and-implementation session. It is not the default bootstrap for
open-ended architecture discussion.

## Copy/Paste Prompt

```text
We are working in /mnt/data/projects/Softland.

Task:
Implement Slice A.0 of the dogfood runtime:

  :compute/run-command
    -> Rama-owned run lifecycle
    -> Rama-owned claim before process spawn
    -> claim-tokened stdout/stderr/exit observations
    -> live UI-readable PState

This is an implementation session. Plan first, then implement the smallest safe
patch. Do not restart the architecture from scratch.

Hard rules:
- Never read src/app/server/env.clj.
- Do not commit .md files.
- Do not commit .gitignore changes.
- Do not use git reset --hard, git clean, or broad git restore.
- Keep docs/private context separate from code/test commits.
- Ignore unrelated dirty worktree files unless they block this task.

Before coding, read:

1. .agents/skills/think-in-rama/SKILL.md
2. docs/current-mental-model/context-map.md
3. docs/current-mental-model/README.md
4. docs/current-mental-model/implementation-review-prompt.md
5. docs/current-mental-model/architecture/dogfood-runtime/README.md
6. docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md
7. docs/reference/rama/28-clj-defining-modules.md
8. docs/reference/rama/29-clj-dataflow-lang.md
9. docs/reference/rama/15-pstates.md
10. docs/reference/rama/11-stream-topologies.md
11. docs/reference/rama/25-integrating.md
12. src/app/server/rama/core.clj
13. test/app/server/rama/world_kernel_test.clj

Implementation target:

  primary new namespace:
    src/app/server/rama/dogfood/compute.clj

  likely tests:
    test/app/server/rama/dogfood_compute_test.clj

  existing V0/V1 kernel remains:
    src/app/server/rama/core.clj

Do not implement:

- LLM / Codex / Claude agent track
- cancel
- restart reconcile / :lost
- serve / daemon lifecycle
- artifact production
- Compute -> World mutation bridge
- full UI polish beyond the minimal live state proof

Preflight: before editing code, write a short plan that answers:

1. What is the first physical record Rama sees?
2. Which depots exist in Slice A.0?
3. Which topology owns all PState writes?
4. Which PStates exist, with exact schemas?
5. How is run-id minted and carried?
6. How does the runner discover pending work?
7. How does the runner claim work without mutating PStates?
8. How does the runner learn its claim was granted?
9. What fields make an observation authorized?
10. What retry/idempotency behavior protects stdout/stderr/exit?
11. Which tests prove the back-arrow?

Load-bearing invariants:

- UI and executor append depots only.
- The topology is the only PState writer.
- The topology never spawns processes.
- The executor never mutates PStates.
- The executor must claim through Rama before spawning.
- The durable claim grant surface is the run row.
- Observations carry run-id, claim-token, and sequence.
- The UI reads Rama PStates only.
- The observation source uses {:retry-mode :all-after}.
- Out-of-order observations are not silently dropped.

Slice A.0 acceptance gate:

1. Append :compute/run-command for ["echo" "hello"].
   Expected:
     accepted decision exists
     run row exists
     run becomes pending

2. Runner claims the run through a depot append.
   Expected:
     first claim is accepted
     run row records :launching or equivalent claimed state
     claim-token and claimed-by are durable

3. Runner spawns only after reading its own durable claim grant.
   Expected:
     no process spawn before claim grant
     executor local registry is only a process-local aid

4. Runner appends observations.
   Expected:
     :started, :stdout, :exit observations are accepted
     observations are keyed by run-id and sequence
     wrong claim-token observations are rejected or ignored with durable error state

5. Live view materializes.
   Expected:
     stdout tail contains "hello"
     exit code is 0
     status becomes :succeeded

6. Failure proof with ["false"].
   Expected:
     exit code is nonzero
     status becomes :failed

7. Double claim proof.
   Expected:
     first claim wins
     second claim is rejected or no-ops durably
     no double spawn

8. UI/proxy proof, if implemented in this slice.
   Expected:
     UI reads live PState only
     no direct executor-to-UI stream

Implementation guidance:

- Prefer matching existing `app.server.rama.core` conventions:
  `{String (map-schema Keyword Object)}`, `<<sources`, `<<cond`, `case>`,
  `local-transform>`, `foreign-append!`.
- If the exact task-id-partitioned pending inbox from the architecture note is
  too risky for the first pass, propose the smallest Slice A.0 simplification
  before changing the invariant. A simple pending index for one local trusted
  runner is acceptable only if the claim-tokened Rama-owned claim remains.
- Do not preserve pseudo-Rama in code. If a sketch path does not compile, replace
  it with the correct Rama Clojure DSL.
- Add focused tests before or alongside implementation. The tests should prove
  request acceptance/rejection, claim behavior, observation folding, and terminal
  status.

Output while working:

1. Short preflight plan.
2. Code changes.
3. Tests run and result.
4. Any contract deltas discovered while implementing.
5. If docs need updates, list them separately; do not mix doc commits with code.
```

## Notes For The Human

This prompt intentionally starts implementation from the Slice A architecture
without asking the implementation agent to redesign the slice. It still requires
a short preflight because the architecture is a contract target, not a guarantee
that every Rama DSL detail is already spelled as compilable code.
