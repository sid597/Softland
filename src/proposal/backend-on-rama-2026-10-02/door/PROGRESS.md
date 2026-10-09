# The door: where the build stands

For a session that takes this build over, and for the receipt to Sid. Kept by the build session
"door build" (Opus 5.5, effort xhigh), started on 9 October 2026 on Sid's line "Build the door. The
contract is src/proposal/backend-on-rama-2026-10-02/BUILD-door.md".

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
| the implied spec; rulings on its 22 open points | done | this commit |
| the split into parts | | |
| the plan | | |
| the check of the plan | | |
| the build: the door, its proof, the maps | | |
| one fresh review of the whole against the contract | | |
| the receipt to Sid | | |

How a stage is run: a fresh agent reads the skill's doc for its stage, the contract and its read list,
and the papers before it; it writes one paper (or the code) and does not commit. This session reads its
report and the parts of its paper that need a ruling, checks the facts that a ruling rests on in the
source, rules, has the rulings applied, and commits with a message that says what and why. No
`Co-Authored-By`, no session link. Never push.

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
