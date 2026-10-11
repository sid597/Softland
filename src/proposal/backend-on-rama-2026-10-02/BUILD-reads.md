# Build: the reads — from the tables back out to a tool, once or live

Cut by: Sid's session of 11 and 12 October 2026, the one that went through the read sheet with him in chat
and ran the push probe (Opus 5.5, effort max).
For: one build, run by the Rama skill's stages (see "How the build runs").
Status: a starter, cut at the end of that session at Sid's word to go fast. Sid's decisions of 11 and 12
October 2026 are the contract, with those of 6 to 9 October. Unlike the door's handoff, this one does not
say that nothing is left to ask. It names what is **not decided** and is kept only so that the build can
run, and what the builder **chooses** and lists as provisional. The build decides none of Sid's open
questions. The stage that writes the implied spec checks each "as built" line of this page against the
code, since the cutter read docstrings and maps and not every function (see "What this page rests on").
Before the first stage: run the four proofs (`test-store/README.md`) and write their count lines on the
build's page as the "before"; read `git log --oneline -5` and `git status --short`. Another session's
files are in the working tree and not committed: `decisions/groups/g-reads.txt`, `decisions/groups/g-check.txt`
and a staged rename beside them. Leave them, and stage your own paths by name.
The decisions, in Sid's words, are in `src-store/README.md`. Everything else a builder chooses is
engineering, written with its reason in a docstring, never as a decision.

## What this build is

The way out. The door is the one way by which an act goes into the store. This build is the one way by which
a fact comes back out to a tool, once or kept live. It has two halves: what the gates must keep so that a
read can find it, and the read path that asks.

```
A TOOL — in the runtime's process. It hands the read path a query written as data.
        │  "this place" · "this entity" · "this run of places" · "what points at this thing"   once, or live
        ▼
THE READ PATH — one piece of code in the runtime's process, as the door is.
   a  WHO asks, and may this asker see this layer?      the layer's setting · the asker's permission
   b  WHICH layers, nearest first?                      what the layer builds on, or the view's own list
   c  a read in each layer: once, or LIVE               Rama gives the value, and pushes each change of it
        │
        ▼
RAMA — the gates' tables, read straight by the read path. No query of the store stands between.
        │  the rows of each layer
        ▼
THE READ PATH puts the layers together: for each place the NEAREST layer that holds a fact wins.
```

When this build passes, a caller in the runtime's process asks for a place, an entity, a run of places, or
what points at a thing, through a list of layers, and gets the value now and each change after. The read
path has a proof of its own against the module. The four proofs of today stay and pass.

What this build is not: sign-in; a screen, a view or a tool; a network edge; a record of who read what;
any change to what the log keeps.

## Read first, in this order

1. `src-store/CLAUDE.md` — the explanation rules. They bind every file of this build.
2. `src-store/README.md` — the tree, and the decisions sections. The last one, of 11 and 12 October, is
   this build's own. Read it with the sheet that it answers, `decisions/groups/g-reads.txt`: its first page
   is the picture of the way out, and "Checked on 10 and 11 October" lists facts about Rama's push. Where
   the sheet and the decisions section differ, the section is later and is Sid's.
3. `reads/push-probe/RESULTS.md` — what Rama's push can watch, as run on 11 October. The two probe files
   beside it are the run. They are a probe: no form, name or table shape comes from them.
4. `src-store/softland/store/README.md` — the store's page: "How the parts relate", "What the gate does
   not check, said plainly", "Decisions this module stands on", "Engineering choices" (its "Closing"
   paragraph is the base's hold-back), "Words a session made, Sid's to change".
5. The code that this build changes or reads, each by its namespace docstring first:
   `record/tables.clj` and `record/rows.clj` (the tables, a fact's row, `place-key`,
   `holds-plain-value?`); `gate/decision.clj` (the check of a permission, `store-key-fault`);
   `module.clj` (the two depots, and on which task a layer's facts are);
   `module/streaming_gate.clj`, `module/microbatch_gate.clj`, `module/door_reads.clj`.
6. `door.clj` and `door/README.md` — the way in. The read path is its counterpart, and
   `BUILD-door.md` is the handoff of the same family. This build changes its rule D8 (see R1 below).
7. `BUILD-store-module.md` — the store's rules 1 to 21. This build changes rules 13 and 16 (see R7).
8. `decisions/OPEN-QUESTIONS.md` — the queue. Answers never live there.
9. `store-module/PROGRESS.md`, items 18 and 24 (the base's second, the place that gives no row, and the
   repair that the papers name), and `door/PROGRESS.md`.
10. The Rama skill (`/rama`), before any Rama code is read or written. `reference/rama/docs` is an extract
    of March 2026 whose pages name Rama 1.5.0; the store runs 1.6.0. A line read there is "read, not run".
11. Last, and only as findings: the rig's notes under `src/proposal/rig-2026-09-25/` on what Rama forced.
    No form, no name, no call shape and no default comes from the rig's code, the server's or Inland's.

## The decisions this build stands on

All are in `src-store/README.md`. The section of 11 and 12 October, by entry:

| Entry | What Sid decided | Sheet |
|---|---|---|
| 1 | A layer has a setting, "members" or "everyone". A permission says what it allows: see, or see and act. | W1 C |
| 2 | A layer says which layers it builds on, in order, as a fact on the layer. A view may give another list. | W2 C |
| 3 | A read is done in the runtime, straight from the tables, with Rama's push. The pushed value is not a cache. | W3 |
| 4 | Only programs on machines that Sid runs may reach Rama's ports. | W9 A |
| 5 | The tables keep a value plain, when a layer has not chosen otherwise. | W10 A |
| 6 | A query is written as data and made of a few words. One more order is kept for the keys that a layer marks. | W11 |
| 7 | From a fact, a read can get who wrote it, and under which permission. | W12 B |
| 8 | The base's second is fine. The base stays a microbatch layer. | W13 |
| 9 | The other sheet, "Before real data is kept", comes after this build. | — |

Earlier decisions that this build leans on: the seal is below the form, and the door seals (6 October, 3);
a permission's form (6 October, 8); a fact with no value is a retract, kept from the code and not decided
(6 October, 6); the members of a layer set its depot as a fact on the layer (7 October, 3); the base is a
microbatch layer (7 October, 4); a signed-in user's own layer, and who roams when not signed in (8 October,
2); a permission ties back to one person (8 October, 3); the store's keys, and that no layer names an owner
(8 October, 5); one lock seals one value (9 October, 2).

Three lines of Sid's hold the whole build:

- 11 July, vision log, line 555: "I want to write directly to rama and then stream from there that is the
  best case scenerio that i want to target and only and only and only if that is not feasable i would go
  for the optimistic update route but never by default".
- 11 October: "can this not be set for each user, tool, layer, group from data?? because we don't know how
  different tools,users or groups might want right??"
- 10 October: "the softland system is providing the full spectrum".

## What to build

The split into parts is the skill's stage to make. This is one reading of the whole, in two halves.

### A. What the gates keep

- **A1. A plain value in the facts table** (entry 5). A gate opens each sealed value when it checks it. It
  now keeps those plain bytes in the fact's row, and the row names no lock. The act in the log is not
  changed: the door seals as built, and a gate still refuses an unsealed value that arrives. The locks
  table and the door's requests stay as built; the log's copy needs them until the other sheet is settled.
- **A2. The writer with each fact** (entry 7). A fact's row also holds who wrote it, and the permission
  that the act named. A read that does not ask for them is sent nothing of them.
- **A3. The layer's setting** (entry 1). One more store key: a fact on the layer whose value is "members"
  or "everyone". It takes a second write.
- **A4. What a permission allows** (entry 1). A permission gets one part more: see, or see and act. A
  permission that was kept without the part reads as "see and act", so the code carries both forms. A gate
  refuses an act under a permission that says "see"; the sheet's words are "an act needs 'act'".
- **A5. What a layer builds on** (entry 2). One more store key: a fact on the layer that holds an ordered
  list of layers. It takes a second write.
- **A6. A marked key, and one more order** (entry 6). A fact on the layer marks a key. For a marked key
  the gates keep each fact in one more order, by value, so that "what points at this thing" is a run of
  neighbouring rows. The mark takes a second write, since a mark can be taken off.
- **A7. The base, for a reader** (entry 8). A read shows no fact of a batch that could still be tried
  again, and a place that holds a fact never gives nothing. The papers name a repair that gives both: keep
  each batch's clock, so that a second try comes out the same (`store-module/PROGRESS.md`, item 24). It
  is tried first, with a run. If the run does not hold, the row before is kept beside the new one.

### B. The read path

- **B1. A query written as data** (entry 6). Its first words: a place; an entity, or any run of
  neighbouring places; what points at a thing. Each is asked once, or kept live.
- **B2. Who asks, and may she see** (entry 1). The read path reads the layer's setting and the asker's
  permission. Until sign-in is built, who asks is what the caller says.
- **B3. Which layers** (entry 2). The list that the layer gives, or the view's own. A layer of the list
  that the asker may not see is left out, and the read says that it left one out. For each place the
  nearest layer that holds a fact wins, also when that fact says "nothing here".
- **B4. Live** (entry 3). A live read is Rama's push, one for each layer of the list. What makes a read
  rightful is itself a fact: the asker's permission, and now the layer's setting, which can be written
  again. The read path keeps a live read on each, and ends the reads that are no longer rightful.
- **B5. One live read, shared** (entry 3). Those who look at the same thing share one push. It ends when
  the last one leaves. Sid, 11 October: "i also don't consider it a cache".

### C. The measures

Sid, 7 October: "idk lets go by whats measured and we will know better when we build more and optimise".
The build measures, and sets no target: a gate's slowest 1 in 100 at 100 acts a second; the same with live
reads open on the layer; the opening of a canvas of 5,000 facts; how late a push is. The base on a real
cluster cannot be measured here, since no cluster runs: the receipt says so.

## What the probe found, in short

Rama 1.6.0, in-process, one machine, one watcher, small values, each case once. The table is in
`reads/push-probe/RESULTS.md`.

- One push can watch **one place**, or **a run of neighbouring places**, in a table of the facts table's
  shape. It sends only the changed, new or removed place. A place outside the run sends nothing.
- `place-key` joins layer, entity and key with a space, and Rama keeps a table's keys in order. So the
  places of one entity are one run, and a push over that run is "one entity, live" with the table as built.
- A run of 20,000 places opened, and one change in it still came as one small diff. A window of the first
  few places of a run stays right when a new place sorts into it.
- A set of ids held as one value sends the one id that was added or removed. So does a run over a set
  that keeps its elements apart.
- A table that a microbatch topology writes can be watched in the same way; its pushes came later.
- **A push on a whole table, or on a whole collection that keeps its elements apart, does not refuse at
  once. The call hung for about 100 seconds and then failed.**
- A push covers one task. A streaming layer is whole on its task. A microbatch layer's facts are on the
  tasks of their entities (the namespace docstring of `module.clj`), so one entity of the base is one
  push, and a run across entities of the base is not.

## The builder's rules

Each has its source. The plan decides how each is met.

- **R1.** No tool reads Rama by itself. The read path is the one reader of facts in the runtime (entry 3),
  as the door is the one way in. So the module has two clients in the runtime, and rule D8 of the door's
  handoff, written while W3 was open, no longer holds as written.
- **R2.** Nothing is shown before it is in the tables. No optimistic update, and no store of our own that
  we keep fresh beside Rama's push (the vision log's line above; `CLAUDE.md`, "Systemic repair and
  caching"). If the build finds itself reaching for one, it stops and says so on its page.
- **R3.** The read path never opens a push on a whole table or on a whole collection that keeps its
  elements apart (the probe). Rama does not refuse it cleanly, so the read path must.
- **R4.** A read that did not ask for a part of a row is sent nothing of it (entry 7).
- **R5.** A screen is shown the newest state. A caller that must see every act cannot rest on a push
  (the sheet, W3: a reader can be left out of one change and is then sent the value anew).
- **R6.** Every word that the build makes is plain, says what the thing is, and is listed for Sid under
  "Words a session made, Sid's to change". The sheet names the words that are wanted: who may see a layer,
  what a permission allows, what a layer builds on. No word comes from the rig's code.
- **R7.** The store's rules 1 to 21 hold where they touch. This build changes two. Rule 13: the setting,
  what a layer builds on, and the mark of a key each take a second write; a permission's place still
  takes one. Rule 16: a gate keeps the plain bytes of a value in the facts table.
- **R8.** What is marked not decided below is built so that the other answer is a small change, and its
  docstring says that it is not decided.

## Not decided — kept only so that the build can run

Sid, 12 October, said yes to: "The three extras stay undecided, and the build treats them as temporary."
The build builds each as written here, and decides none.

- **One permission for each holder in a layer**, as built. One holder cannot hold "see" from one giver and
  "act" from another. The store's page says of it "Sid should know this one".
- **Who may write a layer's setting, what it builds on, and the mark of a key.** Sid's word for the
  setting is "the owner", and no layer names an owner as a fact. Kept: whoever may act in the layer, as
  for its depot (7 October, 3).
- **What a layer starts with.** Kept: a new layer is "members", the base is "everyone", and a layer builds
  on nothing until a fact says so. With "everyone" a person who is not signed in sees the base, which is
  half of the queue's K14.
- **A permission that says "see" stays "see".** A permission's place takes one write, and nothing ends a
  permission. To raise one, or to end one, is not built and not decided.
- **A read that left a layer out says so.** It tells the asker that a layer exists which she may not see.
  Sid let the follow-on stand; nobody asked him about that edge.

## The builder chooses, and lists as provisional

None of these is a decision of Sid's. Each is written with its reason, and named in the receipt.

- The form of a query, and how a caller in the runtime hands it to the read path.
- Where the read path's code lives. The door's place in this tree is the precedent of the same family.
- The form of the fact that marks a key, and of the table that A6 adds: its key, and what its rows hold.
  The probe showed more than one form that Rama can watch; the choice is engineering, with a measure.
- What happens to the facts of a key that were written before it was marked. The sheet says that a table
  can be filled later from the log, the gates' answers and the locks, while all three are whole.
- How many pushes one read opens, and what a read does when a run is very large.

## Not in this build, by decision

- **The other sheet.** The log's copy plain, the purge, forget, whose reads are kept, backups, copies,
  machines, the number of tasks. So this build keeps no read, and the door seals for the log as built.
  Sid's lines on that sheet are under entry 9; none is a decision yet.
- **A first asker.** No view, no screen, no tool, no agent. The proof is the only asker. On 12 October Sid
  said yes to a build of two things: the read path, and what the gates keep.
- Sign-in, and who may act as the system. A network edge, and how a screen reaches the runtime.
- Looking back, finding by words, what stands on a fact, everything one person wrote (the sheet's W11, 4
  to 7). The whole act from a fact (W12 C).
- A layer whose members hold their own locks.
- How a permission ends. A change of a layer's depot. Publishing and promotion. What a key is (K5).
- A real cluster and a second node.

Do not build any of these. Where the code must leave room for one, say so in the docstring.

## Explanations, as the tree's rules say

`src-store/CLAUDE.md` binds every file. Each namespace and each function has a docstring; each folder a
README with a parent link; no function lists in READMEs; no history of corrections. The read path enters
the tree's README map, with its question in the table at the top. Its own explanation holds "Decisions
this build stands on" (the entries of 11 and 12 October by number, the rest by pointer), "Engineering
choices", "Not decided, kept for now", and "Words a session made, Sid's to change". Each line of the
store's page that this build makes untrue is replaced, not amended: among them that a gate keeps nothing
of the plain bytes, that a store key takes one write, and that nothing reads a fact for a caller. The
proofs' README gains a row for the read path's proof. Sid's answers are in `src-store/README.md` as his,
never in these files as new text.

## How the build runs

By the Rama skill's stages, as the store's module and the door ran: the implied spec, the split into
parts, the plan and the check of the plan each in a fresh session, then one build session, then one fresh
review of the whole against this page. The build session judges each stage's paper. The papers go in
`src/proposal/backend-on-rama-2026-10-02/reads/`, never under `src-store/` or `test-store/`. A build's
page, `reads/PROGRESS.md`, records where the build stands, for a hand-over.

Sid's way for a build: write a part whole, then prove it. No test or review loop after each small edit.
Keep the checks that the skill sets at its stages, and add none.

## Commits and the receipt

Commit on `main`, grouped by concern, each message saying what and why. No push. No `Co-Authored-By`, no
`Claude-Session`, no attribution line of any kind. Read `git log` and `git status` before each commit, and
stage named paths only: other sessions commit in this tree.

The receipt to Sid: the line "proven by a hand-played asker, used by no inhabitant yet", so that presence,
tested behaviour and use stay apart; the commit hashes; the proofs' outputs with their count lines before
and after; the measures of C; every provisional choice; every not-decided thing as it was built; every
place where this page could not be met as written, and why; what the build found that is not decided,
proposed as rows for the queue and decided by nobody; `git status --short`.

Never read `src/app/server/env.clj` or any `.env` file.

## What this page rests on

- **Sid's:** the decisions section of 11 and 12 October, and the lines quoted above.
- **Run:** the push probe, in-process on Rama 1.6.0. It did not load the store's module.
- **Read in the code, by docstring:** the facts table's schema (`record/tables.clj`), `place-key`
  (`record/rows.clj`), the check of a permission and `store-key-fault` (`gate/decision.clj`), the two
  depots and where a layer's facts are (`module.clj`), "a permission fact sits on its holder" (the store's
  page). The cutter did not read the gates' functions, the door, or the proofs, and ran no proof.
- **Read in Rama's docs, not run:** that a private table refuses every client; that a table can be added
  at a module update and filled from the start of a log; that a function in a client's path must be on
  the module's classpath, and a lambda cannot be.
- **A session's:** the reading of the whole into A and B, each rule's wording, and each "kept" line under
  "Not decided".
