# Server documentation — completed parallel assignment

Completed on 2026-09-13. The maintained result is the
[server source hierarchy](../../../src/app/server/README.md) and its
[upkeep guidance](../../../src/app/server/AGENTS.md).

All six writing scopes were completed and integrated. The coordinator checked
all 49 source files against the starting version, in both Clojure and ClojureScript
reader branches: executable forms and non-documentation metadata were preserved.
Namespace and function docstring coverage checks also passed.
All immediate source files are reachable from their containing maps, local links
resolve, and the eight Mermaid diagrams render. No product namespace, product
server, cluster or provider was executed for these checks.

The assignment below is retained as history. Its session assignments and
permissions applied to this pass; they do not establish a standing workflow
or authorize future work.

---

# Current server documentation — parallel work

This is the shared assignment for the six sessions Sid opened on 2026-09-13.
It is a one-time documentation task, not a standing development procedure.
The documentation cleanup session owns this file and the final integration.

Sid has started all six sessions and authorized the coordinator to review,
integrate, verify, commit, and then push the completed work. Worker permissions
remain unchanged: only the coordinator stages, commits, or pushes.

## What I want

I want the current server explained hierarchically, like the client: a useful
map of responsibilities and relationships, followed by explanations with the
code at the level where they belong. Someone entering a folder should know
what it does, what it owns, how it connects, and where to read next.

Explain the implementation we actually have. Existing code does not determine
the architecture we want. Historical worklogs can explain how something came
about, but their proposals and unfinished tasks are not current requirements.
This task documents the current server; it does not redesign or repair it.

I have authorized the documentation work. Carry your assigned scope through
writing and checking without waiting for another planning approval. If a
factual question remains unresolved, describe its precise limit and complete
the parts you can establish.

## Assignments

All sessions use `/mnt/data/projects/Softland`, on the existing `main` checkout.
The session key in your kickoff message determines your ownership.

| Session key | Exclusive write scope | Suggested model / effort | Completion note |
|---|---|---|---|
| `rama` | `src/app/server/rama/` | GPT-6 Astra / xhigh | `rama.md` |
| `worn` | `src/app/server/worn/` | GPT-6 Astra / xhigh | `worn.md` |
| `ingest` | `src/app/server/ingest/` | GPT-6 Astra / high | `ingest.md` |
| `episode` | `src/app/server/episode/` | GPT-6 Astra / high | `episode.md` |
| `page` | `src/app/server/page/` | GPT-6 Astra / high | `page.md` |
| `door-tools` | `src/app/server/door/` and `src/app/server/tools/` | GPT-6 Astra / high | `door-tools.md` |
| Cleanup session / coordinator | `src/app/server/README.md`, `src/app/server/AGENTS.md`, shared navigation and this assignment | GPT-6 Astra / xhigh | Final integration report |

The effort allocation is a recommendation, not a measured comparison.
Each worker owns its folder READMEs and documentation in its source files.
The coordinator writes the server-level map after checking the folder maps
and their relationships. Workers do not edit that shared parent map or this
assignment file.

## Start and read

1. Read `CLAUDE.md`, `.claude/memory/MEMORY.md`, and `docs/decisions.md`.
   Follow `docs/carry-on.md` when needed to understand the vision; preserve its
   distinction between a reference summary and the primary `vision/LOG.md`.
2. Read `src/app/client/AGENTS.md` and `src/app/client/README.md` for the
   existing hierarchy and maintenance approach. Borrow that organization;
   derive the server's explanation from server source.
3. Record the starting Git HEAD and any existing changes within your scope.
   Write your completion note with `status: working` before editing.
4. List the files, namespace declarations, definitions and existing docstrings
   in your scope. Then read the relevant implementation passages. Follow
   callers and dependencies outside your folder as needed to establish the
   relationship; read access is wider than write ownership.
5. Load `.agents/skills/rama/SKILL.md` before reading or explaining Rama code.
   Use relevant reference sections to resolve semantics. This is documentation
   of existing code, not a new Rama module implementation or a redesign.
   Use Electric/Missionary guidance when those semantics are relevant.

Never read `src/app/server/env.clj`, any other `env.clj`, or `.env` files.
Refer to imported environment symbols by name without inspecting their values.
Do not recover deleted documents or guidance from Git history. Reading a Git
baseline of an existing, allowed source file to verify your own change is fine.

## What to write

- Each folder README explains its responsibility, boundaries, state ownership
  and the relationships among its immediate files or child folders. Link to
  the next level. Add child READMEs where there are child scopes.
- Namespace docstrings explain the file's computational role, inputs,
  outputs/effects, owned or borrowed state, and relevant approach.
- Function docstrings describe inputs, results/effects and meaningful
  preconditions or limitations. Keep non-obvious local reasoning beside the
  function. Small functions need small explanations.
- Use a small diagram when it clarifies the actual dataflow or ownership.
  Avoid duplicating descendant function catalogs in parent READMEs.

Follow real calls, registrations and lifecycle code before claiming a path
is active. Where relevant, distinguish durable state from process handles,
requests from accepted outcomes, and resource acquisition from release.
Document retry or transaction behavior only as far as the traced source and
dependency semantics support it. A definition or grep hit alone does not prove
a live path, tested behavior, performance, or usefulness.

Preserve useful existing explanations, improving or relocating them when
necessary. Describe limits where they affect the reader. If intended behavior
and current code disagree, keep them distinguishable and report the discrepancy;
do not bless a possible bug by silently making it the intended contract.

`history/` is optional evidence for a specific question. Do not load it wholesale
or transcribe old implementation logs into current READMEs. `reference/` holds
dependency material; check its scope/version when relying on it.
`src-inland/` already has its own map. Link or inspect it for a relevant caller
relationship; do not fold it into your server write scope.

## Work safely in parallel

You are not alone in the checkout. The coordinator is moving historical docs
and references while the other five sessions document their own folders.
Preserve their edits, and adapt to the current paths. Do not restore files
because their changes are unrelated to your assignment.

Only edit prose, comments and declaration docstrings inside your owned scope.
Preserve executable forms, arities, metadata, schemas, configuration and tests.
Do not add implementations, rename namespaces, reformat whole source files or
repair bugs in this task. Report a needed code change with its source anchor.

Do not stage, commit, push, switch branches, create worktrees or revert others'
work. The coordinator will review and commit the integrated documentation.
Do not edit another worker's completion note. If an adjacent explanation needs
changing, name the exact relationship in your own note for integration.

Use bounded read-only collection when useful, following project guidance.
Keep interpretation and final wording with the session responsible for the
scope; do not expand the six-way writing assignment into overlapping writers.

## Check and finish

Review your diff against the starting source. Changes must remain documentation
only. Check local links and whitespace for your owned files. Inspect both ends
of important cross-folder claims, retaining source paths and symbols for the
coordinator. Do not run the application, start a cluster, call providers, or
create new implementation tests just to validate prose. Existing test source
may support a statement about coverage; do not describe it as a fresh run.

Write your note at
`/mnt/data/softland-server-docs-2026-09-13/<completion-note>`.
Create that directory if needed; it is outside the repository and is not `/tmp`.
Use only the filename assigned to you above. Start it with `status: working`,
then replace it with `status: complete` after writing and checking. If an
external obstacle prevents completion, use `status: blocked` and explain what
remains. The note should contain:

- Session key, starting HEAD, and exact files changed or created.
- The main ownership and cross-folder relationships, with source anchors.
- Checks actually performed and their results.
- Unresolved facts or disagreements, including what would resolve them.

Finish with a concise report in your chat. Do not wait for Sid to respond to
mark completed work complete. Completion here means your assigned documentation
and checks are finished; the coordinator still reviews the combined result.

## Coordinator integration

The coordinator reads the six completion notes, reviews the actual diffs and
spot-checks the source behind important claims. It resolves contradictory
ownership descriptions against source, writes the server-level README and
server AGENTS maintenance guidance, and repairs shared navigation once.
It verifies executable-form preservation without evaluating product namespaces,
checks the assembled links, and makes a grouped documentation commit containing
only the reviewed work. Pushing still requires Sid's instruction.

Do not call the hierarchy finished while a scope is incomplete or its important
relationships remain unresolved. Report the actual state if a worker is blocked.
