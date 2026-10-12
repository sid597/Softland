# What must be true before real data is kept

One page, as of 12 October 2026. Sid said yes to it that day: "One page that
lists what must be true before real data is kept" is the eighth of the eight of
Z5 on the sheet "Before real data is kept" (`decisions/groups/g-check.txt`;
his yes is entry 6 of "Decisions of 12 October 2026 — before real data is
kept" in `src-store/README.md`, called "the store's page" below).

**The day itself is his to call. No paper names it.** No paper says what the
first real data is, or by which road it comes onto this store either: no view,
no screen, no tool and no agent uses the store yet (the queue, V23).

This page decides nothing. It gathers what is already said, each line with whose
word it is. Where the store's page and this page differ, the store's page wins.

Today none of it holds. No cluster of this store runs: the proofs run inside
one process, with one copy (the sheet, Z6, "Today"). The build that lines 1 to 3
of the second list need was not built when this page was written
(`PROGRESS.md`, "Now (12 October 2026)").

## The sheet's list

The sheet's words for the eighth: "the list is this sheet's answers and its
runs: the copies and the number of tasks (Z6); who may reach Rama's ports (W9
of the read sheet); a backup and one restore; the proofs on two real nodes;
the purge, if Z2 is A or D."

| | What must be true | Whose word | What is not decided, or not done |
|---|---|---|---|
| 1 | **Three machines and three copies.** | Sid decided (Z6 C; the store's page, 12 October, entry 5): "we will do it right as rama team will suggest", and "yeah i am fine with $100 and the other hosting costs". | Which machines. A licence that allows three nodes: the free one allows two (`reference/rama/docs/19-operating-rama.md`, line 110). The price was read through a tool that gives a summary of a page, and in full by no session. |
| 2 | **The number of tasks is chosen.** Rama cannot change it after launch (`19-operating-rama.md`, line 483). | The sheet, Z6: "a session reckons the number of tasks from the machines that you name and from Rama's advice, shows the reckoning, and brings you one number to confirm before a cluster is launched." | No number is reckoned, since no machine is named. A session's, read and not run: with line 1 of the next list, the number can be changed later by a second run from the logs, with downtime (the store's page, entry 5). |
| 3 | **Only programs on machines that Sid runs may reach Rama's ports.** | Sid decided (W9 A; the store's page, 11 and 12 October, "reading back out", entry 4). The sheet's words: "the rule is a setting of the network, set and checked when a cluster is first run for real data." | Not set: no cluster runs. Rama asks no name and rejects nothing, so the network does it (a session's note there; read, not run). |
| 4 | **Backups go to storage that is not this machine, and one restore from a backup has been run.** Rama's own backups, with line 1. | Sid said yes (the first of the eight of Z5; entry 6), and decided Rama's own backups (entry 5). | Not set up: no cluster runs. The queue, V6. |
| 5 | **The proofs pass on two real nodes.** | The sheet's last page: "The proofs run inside one process, with one copy; they pass on two real nodes before real data is kept." It was listed and not asked. | Not run. The same place says that the rule which refuses a stamp more than 10 seconds ahead needs two machines' clocks to agree. The queue, V4. |
| — | ~~The purge of the log.~~ | The sheet named it "if Z2 is A or D". | It falls away. Z2 was answered plain, without the purge (entry 2). |

## What his answers of 11 and 12 October add

These are his decisions on the same sheet. The papers of this folder name the
build that carries them "before real data is kept" (`PROGRESS.md`; its
contract is `BUILD-before-real-data.md`).

| | What must be true | Whose word | What is not decided, or not done |
|---|---|---|---|
| 1 | **Every table can be made again from the logs.** What a gate decided is in a log too, so a rebuild applies the answers and decides nothing again. | Sid decided (Z1; entry 1). That his "yes" is to this box is a session's reading, marked so there. | Not built. Not designed: the form of that log, and of the rebuild. Not run: that the tables made again are the same. |
| 2 | **Every value is plain in the log, and the store keeps no locks.** The door seals nothing; a gate makes and opens no lock. | Sid decided (Z2; entry 2): "yeah lets remove it". | Not built when this page was written: the committed door seals (`src-store/softland/store/door/seal.clj`). |
| 3 | **Each log is kept whole.** Rama's trim setting stays off. | Sid decided (Z3 A; entry 3): "forever never trim". That it holds for each log of the module, and not for the logs of acts alone, is a session's reading there. | Not set on a real cluster. Nobody has priced the disk. |

## Bears on that day, and nobody has asked him

Each is a row of the queue (`decisions/OPEN-QUESTIONS.md`). None is on the
sheet's list.

- **What a key is** (K5). The sheet's last page puts it under "Before real
  data, and not yet asked". A fact's key is in the key of its row, and Rama
  changes the values of a table and not its keys; so after real data a change
  of this is a new table that is filled from the old one. Read, not run.
- **Who runs the cluster** (V14). No paper names an operator. Rama never moves
  work off a lost machine by itself.
- **By which road the first real data comes** (V23).
- **The order of a layer's acts after a failure** (V1), and **every number**
  (V5). Each waits for a run, and comes to him only if the result needs him.
  No paper says that either must be done before that day.

## Not on this page

- **What waits for sign-in.** Whose reads are kept is decided (entry 4) and is
  built when the store can tell a model from a person. The queue has the rest:
  E3, K7 to K10, V9 to V13.
- **What is due before a stranger can reach the door.** A bound on how much
  one sender can put into the log, and a way to block a sender (entry 7). That
  is another day than this one, and it is his to call too.
- **What waits for the forget build.** The store can forget nothing until
  then (entry 2). The queue: K11, V15.
- **The limit of four on the depth of a permission.** It is lifted in the same
  build (the fourth of the eight of Z5). To make a limit wider later refuses
  nothing that was accepted, so no day depends on it.

## How each could be shown to hold: a session's, and not decided

Nobody has said what counts as shown. These are a session's suggestions, one
for each line, so that the day does not rest on a claim.

- Three machines and three copies: Rama's own page for the cluster names three
  nodes and a replication of three for the module.
- The number of tasks: the reckoning is on paper, with the machines that it
  was reckoned from, and his word on the one number.
- Rama's ports: a try from a machine that he does not run gets no answer from
  any of them.
- A backup and a restore: a store is filled from a backup, and a read of it
  gives what was written before the backup was made.
- The proofs: the six of `reads/RECEIPT.md`, section 3, run against the real
  cluster and not inside one process, with their output kept.
- The tables made again: one run that makes them again from the logs and
  compares them with the tables that the gates wrote, row for row.
- No locks: the door's declaration and the module declare no table of locks
  and no request for them, and a proof sends a value and reads it plain from
  the log.
- The logs kept whole: the cluster's settings show the trim off for each
  depot.
