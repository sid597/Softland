# Session Resume — Rama Rename + Dogfood Runtime State

Status: post `1ef1cbd`, 2026-05-13.

This file is a task handoff only. It is not the global context pack. If the
user opens with a new direction or a design question, start from
`docs/current-mental-model/00-start-here/new-chat-bootstrap.md` instead.

## Branch / Commit State

We are working in:

```text
/mnt/data/projects/Softland
```

The current branch is:

```text
docs/current-mental-model-local
```

This is the private/local docs branch. Do not push it unless the user changes
that rule. Public code lives on `main`. Docs and code/test commits are allowed
to be separate on this branch.

Most recent code commit for this thread:

```text
1ef1cbd rama: split text kernel and rename space
```

That commit touched only:

```text
src/app/server/rama/**
test/app/server/rama/**
```

It completed PR1 as a mechanical vocabulary rename plus mechanical
`core.clj`/`text_kernel.clj` split. It did not add `defkernel` generation and
did not change Rama topology behavior.

## Current Source Shape

Active Rama source paths:

```text
src/app/server/rama/core.clj
  Shared contracts/utilities:
  ActionRequest, ActionDecision, KernelEvent validation and helpers,
  default actor/context/causal/policy helpers, ordering/routing helpers,
  compat request/event helpers, and shared kernel-contract-table.

src/app/server/rama/text_kernel.clj
  V0/V1 text instance:
  text-kernel-module, *text-requests-depot, text-kernel-topology,
  text request/event builders, interpreters, materializations,
  runtime lifecycle, append/read helpers.

src/app/server/rama/dogfood/space.clj
  Dogfood space runtime:
  space-kernel-module, *space-action-depot, space-topology,
  space/turn request builders, space/turn materializations,
  LLM request/control mirrors derived from accepted space actions.

src/app/server/rama/dogfood/llm.clj
  LLM-owned runtime:
  llm-run lifecycle, llm-turn-run concept, control handling,
  executor claim/observation lifecycle, raw item/cost/run materializations.

src/app/server/rama/dogfood/compute.clj
src/app/server/rama/dogfood/transcript.clj
  Compute and transcript runtimes. These now require shared contracts as
  app.server.rama.core, not as a kernel alias.
```

Active Rama test paths:

```text
test/app/server/rama/text_kernel_test.clj
test/app/server/rama/dogfood_space_test.clj
test/app/server/rama/dogfood_llm_test.clj
test/app/server/rama/dogfood_compute_test.clj
test/app/server/rama/dogfood_transcript_test.clj
```

## Vocabulary

Use current names in active code and new docs:

```text
old world-thread     -> current space
old world-turn       -> current turn
old world.clj        -> current space.clj
old world-kernel     -> current text-kernel for the text instance,
                        or space-kernel for the dogfood runtime
old :world/append    -> current :action/append
```

Semantic data changed too:

```text
:world-thread/*       -> :space/*
:world-turn/*         -> :turn/*
:target/kind :world-thread -> :target/kind :space
[:world-thread id]    -> [:space id]
:parent-thread/id     -> :parent-space/id
:child-thread/id      -> :child-space/id
:source-world-turn/id -> :source-turn/id
:resolution/world-turn-id -> :resolution/turn-id
```

Historical docs may still say world-kernel, world-thread, or world-turn.
Preserve that when the doc is provenance, but translate to space/turn when
touching active code or writing a new handoff.

## Runtime API Names

Text runtime:

```clojure
text-kernel/start-text-runtime!
text-kernel/close-text-runtime!
```

Space runtime:

```clojure
space/start-space-runtime!
space/close-space-runtime!
space/append-space-action!
space/space-action-request
space/space-create-request
space/space-turn-request
space/space-routing-key
space/read-space
space/read-turn
space/read-turns-by-space
space/read-space-graph
space/await-space
space/await-turn
```

LLM cross-module names:

```text
$$llm-thread-by-space
$$llm-turn-run-by-turn
read-run-for-turn
```

Do not rename LLM's own `llm-turn-run` concept. It is LLM-owned vocabulary.

## Rama Safety Notes

Keep these behavioral contracts unchanged unless the user explicitly starts a
new architecture change:

```text
same depot boundaries
same partitioning contracts
same intra-topology |hash hops
same mirror-depot plus depot-partition-append! semantics
same :retry-mode :all-after on observation depots
```

The keyword collision between space requests and LLM controls is intentional:

```text
space request validation dispatches on :request/type
LLM control validation dispatches on :control/type
LLM run request validation rejects :request/type :turn/cancel and :turn/steer
```

Tests for this live in `dogfood_space_test.clj`.

## Verification From `1ef1cbd`

Targeted Rama suite passed before commit:

```text
Testing app.server.rama.text-kernel-test
Testing app.server.rama.dogfood-space-test
Testing app.server.rama.dogfood-llm-test
Testing app.server.rama.dogfood-compute-test
Testing app.server.rama.dogfood-transcript-test

Ran 52 tests containing 447 assertions.
0 failures, 0 errors.
```

Useful focused command:

```bash
clojure -M:test -e "(require 'clojure.test 'app.server.rama.text-kernel-test 'app.server.rama.dogfood-space-test 'app.server.rama.dogfood-llm-test 'app.server.rama.dogfood-compute-test 'app.server.rama.dogfood-transcript-test) (clojure.test/run-tests 'app.server.rama.text-kernel-test 'app.server.rama.dogfood-space-test 'app.server.rama.dogfood-llm-test 'app.server.rama.dogfood-compute-test 'app.server.rama.dogfood-transcript-test)"
```

Rama test JVMs can linger after printing the summary. Check `ps` before
assuming a failed hang.

## Current Pending State

There is no active implementation handoff from this file. The Rama rename/text
split code is committed. If the user says "continue", first infer whether they
mean:

```text
1. continue docs cleanup on the private branch
2. inspect/review the `1ef1cbd` code commit
3. start the next Rama dogfood runtime feature
```

Do not assume a next feature just because this file exists.

## Guardrails

- Never read `src/app/server/env.clj`.
- Do not use `git add -A`.
- Keep docs-only commits and code/test commits separate unless the user says
  otherwise.
- Do not push `docs/current-mental-model-local`.
