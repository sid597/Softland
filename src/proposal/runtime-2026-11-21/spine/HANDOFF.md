# The spine: where the build stands, for the session that carries it on

**22 November 2026.** Written by the session that began the spine (Fable 5.2)
after Sid's go of 21 November, because that session grew too large to finish
it. It is a handoff, not a decision of his. The base stays
[the runtime's page](../PROPOSAL.md), "The spine, to start from", and his go to
the sheet for sign-in (below).

## What he said go to

The sheet for sign-in, 21 November, as this session put it to him:

1. **A deployment is born by one setup command, run once.** The system makes
   the base, makes a person id for the first person, gives it see-and-act in
   the base, and puts her account on the list. The runtime refuses to serve a
   store with no base.
2. **An invitation is a person id that waits for an account.** The inviter
   makes the id and gives it permissions by her own acts; a first sign-in
   links an account to it.
3. **The key for her name** is `:person/name`, written as her own act in her
   own layer. His to turn at no cost while the store is thrown away.

Two that this session took as engineering and told him: Auth is written first
and the store second, so that a break leaves nothing that no account leads
to; and her own layer's id is kept on her row in Auth.

## What exists

| Part | Where | State |
|---|---|---|
| The wire: words, credential, frame, what waits for a slow reader | `src-wire`, `test-wire` | Committed. `clojure -M:wire -m softland.wire.pure-proof`: 37 passed, 0 failed |
| Auth's spec, and its requirements | `auth/SPEC.md`, `auth/IMPLICIT_SPEC.md` | Committed. Every question of the requirements page is settled in the spec; where the two differ, the spec holds |
| Auth's four stages | `auth/DECOMPOSITION.json` | Written. `invite-and-enter`, `standing-under-a-root`, `ask-and-answer`, `watch-and-close` |
| Auth's module and client | `src-auth`, `test-auth` | **Not built.** Stage 1's plan is being written |
| The runtime: seam, standing, sign-in and setup, the line, the pages, the program | `src-runtime` | **Written, never compiled, never run.** It requires `softland.auth.client` and `softland.auth.module`, which do not exist yet |
| The command line and its library | `src-command` | Written. It loads (`clojure -M:command -e "(require 'softland.command.main)"`); nothing of it has met a runtime |
| The proofs of the runtime | `test-runtime` | Written, never run: `pure_proof`, `proof` (in-process, three copies), `command_proof` (the commands as processes), `cluster_proof` (three processes, then kills) |

Expect the runtime to need fixing when it first compiles: about 5,000 lines
were written against Auth's contract with no run between.

## What is next, in order

1. **Auth, by the Rama skill's phases**, for each of the four stages: a plan
   (`auth/PLAN-<stage>.md`), an adversarial check of the plan
   (`PLAN_VALIDATION-<stage>.md`), and a build with its checks. Then the
   skill's review of the whole against the spec.
2. **Compile the runtime against Auth** and run `softland.runtime.pure-proof`,
   then `softland.runtime.proof`, then `softland.runtime.command-proof`. Fix
   what breaks.
3. **The cluster proof**: launch Auth on the proof cluster, run
   `softland.runtime.cluster-proof`. These are the page's proofs: two runtime
   processes on a cluster that stays up, with processes killed; one person of
   two first sign-ins at once; an act repeated across a break taken once; a
   watch that closes when its session ends.
4. **The spine's page** (`BUILD.md` here): what was built, each fork, what was
   run. The trees' pages link to it already.
5. **A fresh session reviews from the diff.** The job of a review is to break
   the change.

The proof where the two lines meet (a watch closes when its permission ends)
waits for the store's line.

## Who does what: a mistake of the first session, and the rule that follows

The first session wrote the runtime and the command line by hand, and sent
Auth's requirements, its stage split and its first plan to agents of the type
`general-purpose` without naming a model. On this machine that type runs on
**Opus 5.5**, not on the model of the session that spawns it. So the design of
Auth, which is the judgment of this piece, went to a lower model, against the
root page ("the judgment stays with you and is never handed to a lower
model"). Sid caught it.

From here: **an agent that designs or judges is spawned with `model: "fable"`
named in the call.** Collection and typing can go lower. The session that
carries this on designs and judges, and hands implementation out.

What that leaves on disk: `IMPLICIT_SPEC.md` and `DECOMPOSITION.json` came
from Opus. Both are readings of requirements, and the spec's author went
through each of their questions; they stand as inputs.
`auth/second-view/PLAN-invite-and-enter.opus.md` is the plan that an Opus
agent had nearly finished for stage 1 when it was stopped. It is kept as a
second view, to hold against the plan proper once that exists, and it is not
the plan.

## A safeguard stop, and what was read from it

The first plan agent (Opus 5.5) was stopped twice with
`reasoning_extraction`. Its transcript shows that it had finished its reading
and then deliberated for eleven minutes with no tool call before the stop.
The two agents before it, on the same model, wrote sooner and were not
stopped. A reading, not a checked cause: a long stretch that holds only
deliberation is what gets stopped. The briefs since tell an agent to create
its file first and write each section as it is settled, and to give decisions
with their grounds and no log of how it got there. A brief in that form ran to
its sixth step without a stop.

## Forks that the first session took, each Sid's to turn

- Any signed-in person may invite, and an agent may not.
- One invitation has one claim: an email, or a code.
- The runtime refuses an act that names another who than its sender, and
  writes nothing for it.
- No system outside the runtime chooses the id of a person, an agent, a
  sign-in or a session: the runtime makes or derives them. An agent's id is
  the who of its acts, so a maker-chosen id could have been another person's.
- A credential names its root and its sign-in, so one read on one task finds
  it. A browser's sign-in is taken from a cookie alone and a program's from a
  header alone.
- The line is one WebSocket that carries the words as EDN; a connection's
  words are done one at a time, in order.
- In the spine an agent has its own who and a permission that its person
  gives. The branch layer and `promote` stay in pieces 6 and 8.
- The numbers: a secret lasts 7 days, a session a day from its last moving
  on, a program's sign-in that is under way 10 minutes.

## What was measured, and what was only read

- **The store's module on a real cluster.** Launched from the commit's source
  on a one-node Rama 1.6.0 cluster; the door opened and read a layer's depot.
  Nothing more of the store was run there.
- **The command line's start.** 0.53 s plain and 0.44 s with
  `-XX:TieredStopAtLevel=1`, three runs each, with its forms loaded.
- **`pstate.proxy.limit`.** Another session measured that it counts per
  module, PState and task, over all client handles
  ([WORKFLOW-AND-MODEL.md](../WORKFLOW-AND-MODEL.md), 2.1). This session did
  not run it again; the cluster proof holds a check for it.

## The proof cluster

Outside the repository, under
`/mnt/data/projects/research/softland/runtime-spine-2026-11-21/`:

- `cluster/`: an unpacked Rama 1.6.0, with `rama.yaml` set to ports of its
  own (Zookeeper 2101, conductor 1983, the cluster's page 8898, workers
  13000 to 14000). It holds nothing that is kept. Start it from that folder:
  `./rama devZookeeper`, then `./rama conductor`, then `./rama supervisor`,
  each as a process that stays. It does not come back after a restart of the
  machine, and its Zookeeper keeps nothing across one: set `data` and
  `local-zk` aside and launch the modules anew.
- `build/store.jar`: the store's source from the commit, as a jar. Launch:
  `./rama deploy --action launch --jar ../build/store.jar --module softland.store.module/Store --tasks 4 --threads 2 --workers 1`.
  Auth is launched the same way once it is built.
- `bin/with-store-at-head <alias> <namespace>`: runs a namespace with
  `src-store` taken from that snapshot, because another session edits the
  store's working tree while the spine is built.

One thing to know: the cluster's ports listen on every interface of the
machine, not on the loopback alone.

## Still Sid's

- **The Google client id.** Until he makes it, every sign-in runs on the
  stand-in, and a real Google sign-in is not proven.
- Each fork above.

## Update (22 November 2026 — overnight oracle & breaker pass)

See [BUILD.md](BUILD.md) for full proof results, adversarial breaker review
fixes across `src-runtime/` and `src-command/`, the five confirmed proof
defects in unmodified `test-runtime/softland/runtime/checks.clj`, the
`ration` placement options, the `:driven-by` wire-word recommendation, and
the Rama 1.6.0 wildcard-bind finding. Step 4 (real `src-auth/` integration,
persistent cluster proof, and Piece 1 + Piece 4 Join proof) waits for real
`src-auth/` to land.
