# The door: where the build stands

For a session that takes this build over, and for the receipt to Sid. Kept by the build session
"door build" (Opus 5.5, effort xhigh), started on 9 October 2026 on Sid's line "Build the door. The
contract is src/proposal/backend-on-rama-2026-10-02/BUILD-door.md". From the plan on it is kept by a
second session (Opus 5.5, effort xhigh), which took the build over late on 9 October on Sid's line
"Take over the door's build". "The build session" below is the one that holds the build at the time.

## What to read, in this order

1. `../BUILD-door.md` — the contract. It is the full spec.
2. `src-store/README.md` — Sid's decisions of 6, 7, 8 and 9 October. It wins over the contract.
3. This folder's papers, by stage: `IMPLICIT_SPEC.md`, `DECOMPOSITION.json`, `PLAN.md`,
   `PLAN_VALIDATION.md`, and after the build `IMPLEMENTATION_VALIDATION.md`, `TEST_VALIDATION.md` and
   `FULL_SPEC_REVIEW.md`.
4. "Rulings", below: what the build session ruled on each paper, with the reason.

## The starting point, confirmed

The contract asks for two things before the first stage, by `git log` and by a run.

- The rename is in the log: `c296a459`, "The store's keys carry softland.store, "class" is "depot",
  and a gate refuses an unknown key of the store's". The head at the start was `5ebb010f`.
- The three proofs, run by this session on that tree on 9 October, each with exit 0:
  - `clojure -M:store -m softland.store.act-proof` — `{:test 8, :pass 150, :fail 0, :error 0}`
  - `clojure -M:store -m softland.store.pure-proof` — `{:test 13, :pass 449, :fail 0, :error 0}`
  - `clojure -M:store-module -m softland.store.module-proof` — `{:test 22, :pass 227, :fail 0, :error 0}`

Both are as the contract says. Nothing differed, so nothing was asked.

## The stages

The Rama skill's stages, as the store's module ran: each design stage in a fresh session (the Agent
tool, model Opus 5.5, effort xhigh), judged here. The door is a client of the module and not a module,
so each stage's paper says how it adapts the skill's template.

| stage | state | commit |
|---|---|---|
| the starting point confirmed | done | — |
| the implied spec; rulings on its 22 open points | done | `3746b4e5` |
| the split into parts: one part, `door` | done; judged sound | `1a68cde7` |
| the plan; eight rulings on it | done. Before it ran, the tree under `src-store/` and `test-store/` was checked and had not changed since `c296a459` | the commit after `980ed903` |
| the check of the plan | running: a fresh agent, started late on 9 October. When no `PLAN_VALIDATION.md` is on disk, or it is cut short, run the stage again from its start; `git diff` of `PLAN.md` shows what a cut-off run changed in the plan | |
| the build: the door, its proof, the maps | | |
| one fresh review of the whole against the contract | | |
| the receipt to Sid | | |

How a stage is run: a fresh agent reads the skill's doc for its stage, the contract and its read list,
and the papers before it; it writes one paper (or the code) and does not commit. This session reads its
report and the parts of its paper that need a ruling, checks the facts that a ruling rests on in the
source, rules, has the rulings applied, and commits with a message that says what and why. No
`Co-Authored-By`, no session link. Never push.

Scratch files of a stage (probes, the proofs' full outputs) go under
`/mnt/data/projects/research/softland/door-build-2026-10-09/`, outside the repository. The plan's
four probes and their outputs are in its folder `plan-probes/`. Nothing goes
under `/tmp`: a restart of the machine empties it, and one did during the plan.

## Rulings

**On the implied spec, 9 October.** The paper found 22 open points (P1 to P22): places where the
contract is silent, cannot be met as written, or disagrees with itself or with the module's
explanations. The rulings are at the end of `IMPLICIT_SPEC.md`, "Rulings of the build session on P1 to
P22", each with its reason. Two rules hold most of them:

- Rule A. The door makes each of its own additions right (the act-id, the depot, the seal of each
  value), or it says that it could not and appends nothing. It writes over nothing that a maker gave,
  and it makes no addition that its own declaration refuses.
- Rule B. A value that the door finds under `:v` is sealed so that it reads back, or the act is not
  appended. So the door seals a value of the form only, by `softland.store.act/value?`. This narrows
  D1 by one corner, and it is a working answer: see the first row below.

All else of a thing goes as the maker gave it, and a gate judges it (D1).

**On the split, 9 October.** One part, `door`. Its hard core is the stock of locks: the door's one
memory, the one thing that makers share at once, and the one place where the contract leaves the
builder a design. The split's writer weighed one cut, the way in of an act with no value and its
answer first, then the stock and the seal, and turned it down: the first half has no hard core, and a
request for locks is itself an act that goes in and is answered by the same way. The build session
agrees: no ruling was needed.

**On the plan, 9 October.** The plan brought four points; the build session ruled on them and added
four rulings of its own. They are in `PLAN.md`, "Rulings of the build session on the plan", each with
its reason and with what was checked in the source. In short:

1. The door asks for locks only at need, one request at a time for one stock. In the base one who
   then gets at most 256 locks for each time that a request's batch takes to close.
2. No copy of a layer's depot is built. The read for each act-id is a finding for the receipt, and
   goes to the examination of its own that the project's rule on caching asks for.
3. The door reads a request's answer and then its locks, as the contract orders.
4. The locks of one act are taken oldest run first, across the border of two runs.
5. `seal.clj` gains one function, `sealed-length`, and the door asks it. The plan had measured the
   length by a run, because the brief of the plan stage closed `seal.clj` to code; the contract
   closes the gates, the record and the declaration only. A brief of a later stage says what the
   contract says, and no more.
6. A request whose append threw is kept as in doubt and waited for once more.
7. `door/README.md` keeps the four files that a gate requires apart from the door's own three.
8. A read of a request's locks that came back ends the request, also when it gave too few.

## For the receipt: what Sid should know, as found so far

Places where the contract is not met as written, each ruled in `IMPLICIT_SPEC.md`:

1. Part 4, "it appends nothing for that act", of an append that threw. An append that throws may have
   gone through (Rama's documentation; the module's own docstring). The door's word says that it does
   not know, and carries the act-id and the digest (P1).
2. Part 4, "a malformed act goes in and comes back as the gate's refusal". It cannot hold for a thing
   that would be a stray, which gets no answer ever (P2), and not for a fault that the door's own
   additions would take away before a gate can see it: an act that already carries `:act-id` or
   `:depot`, a fact with a value beside `:sealed` or `:lock-id`, a value that is no value of the form
   (P3). Each ends in the door's own words, and nothing is appended.
3. Part 4, "a layer not made and no depot to start with": no such case can come about. The word has
   the case of a layer of which no act-id can be made (P14).
4. Part 4 says the door "reads `:layer` and `:facts` and no more"; part 3 needs `:who` and
   `:permission`, and a request needs `:version` and `:session`. The door reads the six and checks
   none (P4).
5. "Explanations" names `module/README.md` for the line on who reads a lock. The line is in
   `module/door_reads.clj` and in the store's map, and says "guard". Three sentences of the store's
   map leave something to "the door proper"; the door as built guards nothing of a gate (P9, P12).
6. D8 names one exception. The door's proof reads the depots' logs and the record round the door, and
   is a second (P13).
7. "The module proof stays as it is": its docstring's sentence "The door is not built" changes; no
   check changes, and its count stays 227 (P15).

What the build found that is not decided, as rows for the queue, decided by nobody (the text of each
is at the end of `IMPLICIT_SPEC.md`):

1. Who holds the form of a value once it is sealed.
2. Whether the door runs the maker's check (`softland.store.act/parse`) before it seals and appends.
   Sid's F8 C: "The door may warn early; the gate is the authority". The build session's view: yes.
   The depots are never trimmed, and a malformed act is where a plain value gets into one; the gate
   stays the authority. It is his to say, because a fault of form would then be the door's word and
   kept in no record.
3. Whether a lock of an act that was not accepted comes back to the stock. As built it never does.
4. What guards a gate from an act that costs much.

Not settled, and not reachable on the in-process cluster: what a Rama worker does with bytes of a Java
type that it does not have. The door seals `:v` and passes each other part as given.

From the plan and its rulings, for the receipt beside the lists above:

- What the stock costs the base: at most 256 locks for one who for each time that a request's batch
  takes to close (0.75 to 1.03 s on the in-process cluster, by the plan's probe), and that wait at
  each send that finds its stock short. What would lift it is not built: a request sent before the
  stock is empty, or several at once.
- The read of a layer's depot for each act-id that the door makes: one round trip, and one read on
  the layer's task, for a row that no act of this build changes. No copy is built.
- The door's memory has no bound but the life of its process: one stock for each layer, depot and
  who that wrote a value through it.
- `seal.clj` gained one function. No gate calls it.
- A request whose append threw is kept and waited for once more.
- Not settled by Rama's own word: whether one handle of the client may be used by many threads (one
  probe run of 16 threads had no error; the local documentation is silent); the number of a query's
  time limit (one run gave 70 s); whether a client's handles stay good when a module is started
  again in a real cluster.
- The words that the plan made, for Sid to change: the six calls, the door's five numbers, the
  fourteen words, and the words of the stock. They are in `PLAN.md`, "Words a session made".

## For a session that takes this build over

First written on the evening of 9 October, when the first build session stood at about 410,000
tokens with four stages still to run. Sid chose a new session that evening ("stop i start a new"),
and that session ended before the plan. The second session ran the plan and ruled on it. The stage
table says what is next. Everything that a build session held and a successor needs is in this
folder. Nothing that matters is only in a chat.

**Where the build stands.** The stage table above says it. One thing a table cannot say: a stage's
fresh agent is a child of the session that started it and ends with that session. A new session
cannot resume it. So when a stage's row says "running" and its paper is not on disk, or is cut short,
run that stage again in a fresh agent.

**What each stage's brief must carry.** The build session wrote each brief by hand; these are the
parts that did not change from stage to stage.

- The contract is the full spec and the only spec. `IMPLICIT_SPEC.md`, with its last section of
  rulings and rules A and B, holds requirements beside it. A stage re-opens no ruling.
- The stage reads the skill's doc for its own stage, and the skill's `SKILL.md`. The skill's
  templates are written for a module; the door is a client of one. Each paper says how it adapts the
  template, and leaves no section out: where one does not apply it says why in a line.
- One paper, or the code; no commit; nothing written outside what the stage owns.
- It decides nothing that is Sid's. It changes no code of the gates, the record or the declaration.
  Say it in those words. The plan stage's brief also closed `seal.clj`, `text.clj` and `digest.clj`,
  and the plan then worked round a private number of `seal.clj` (ruling 5 on the plan).
- Never read `src/app/server/env.clj` or any `.env` file. No form, name, call shape or default from
  the rig's code, the server's or Inland's; the rig's notes are findings only.
- Words: "act", "maker", "door", "gate", "depot", "act-id", "stock". "Key" is a fact's key and "lock"
  is an encryption key. Plain sentences, as `src-store/CLAUDE.md` asks.
- Probe files go under the scratch path above, never under `/tmp`.
- A report back of a fixed length, not the paper's text: the paper's path and headings; the points
  that need the build session's ruling, each with its passage, the choices, and a pick where the
  pick is engineering; what was read and what was run.
- Model Opus 5.5 at effort xhigh for a stage that designs or judges. Judgment is never sent to a
  lower model.

**What to hold the plan to**, beside each ruling. The build session's own list for judging
`PLAN.md`; the check of the plan is a fresh agent's, and this list is not given to it as answers.
The plan was held to it on 9 October, and it was not given to the plan's writer either. It stays
for the build's code and for the review.

1. The door has no thread of its own, and each request is sent inside a maker's call (P16).
2. The door remembers no layer's depot between two calls (implied spec O2.6). That would be a cache
   of what the cluster holds, and the project's rule sends a cache to its own examination before it
   is designed in.
3. Many makers at once: no lock goes to two facts or to two acts; the locks of one act are taken in
   one step, as neighbours of as few requests as the stock allows (O3.6, O4.14). The streaming gate
   reads one range of lock rows for each request that an act names.
4. A maker never waits without a bound on a request that is not its own, and is never handed the
   refusal of a request that carried another permission (O3.11).
5. A lock's bytes are in no word of the door's, in nothing printed or thrown, and in nothing that a
   maker gets (P18). A door that is printed must not print its stock.
6. The digest is of the very thing that is appended, with its facts as a vector (P20).
7. Each of the door's own words has a case that the in-process cluster reaches, and says which of two
   things holds: nothing was appended, or the door does not know (P1).
8. The wait: how often it reads, what that costs a task at the microbatch gate's aim, and how far the
   maker's bound reaches. The hand reads every 20 ms; at a thousand waits at once that rate is a
   thousand times fifty queries a second.
9. Each call's round trips are counted and each is justified. The skill's rule: never trade I/O or
   correctness under failure for simpler code.
10. The three other proofs keep their counts, 150, 449 and 227, unless the pure proof takes the
    door's pure parts; then the plan says by how many.
11. Every explanation that the door makes untrue is named with what it must now say. They are in
    `act.clj`, `door/README.md`, `door/digest.clj`, `record/rows.clj`, `module/door_reads.clj`, the
    store's map (five places), the tree's entry, `test-store/README.md`, the proofs' README (two
    places) and the docstring of `module_proof.clj`.

**The stages that are left, and what the build session does at each.**

- The check of the plan (`phase-2-plan-validate.md`): a fresh agent, adversarial, whose default
  verdict is fail. On a minor fail it fixes the plan itself; on a major fail the plan stage runs
  again. The build session rules on what it brings back.
- The build (`phase-build.md`, which walks `phase-3` to `phase-7`): one fresh agent writes the door,
  its proof and the maps, with `IMPLEMENTATION_VALIDATION.md` and `TEST_VALIDATION.md`, and runs the
  four proofs to a pass. The build session reads the code and the diff whole before it commits.
- One fresh review of the whole against the contract (`phase-full-spec-review.md`), then the build
  session's rulings on it.
- The commits, grouped by concern: the door; its proof; the maps. Then the receipt, as the
  contract's last section lists it. The four proofs' outputs in full go under the scratch path, and
  their count lines into the receipt.
