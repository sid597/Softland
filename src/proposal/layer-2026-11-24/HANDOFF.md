# The store's layer and Sid's layer: what 24 November found, and the way to the next checkpoint

[The starter for the next session](STARTER.md) ·
[the review that broke a session's definition](REVIEW.md) ·
[the runtime's page](../runtime-2026-11-21/PROPOSAL.md) ·
[the store's decisions](../../../src-store/README.md)

**24 November 2026.** Written by the Claude session that Sid worked with that
day (`73e60f65` in the record), at his "go". Nothing on this page was run.
Three kinds of statement are kept apart:

- **Sid's:** his words, with the id of his line in the record.
- **In the code:** read in the file that is named.
- **A session's:** a reading, a position or a guess. It is his to turn.

## Start here

**Where the work stands: step 1 of 5 is done. Step 2 is next.**

| | Step | Who | Size |
|---|---|---|---|
| 1 | This page, the starter and the review's report | The session of 24 November | Done |
| 2 | Find where the code above the store, and the plans for pieces 5 to 8, work according to the store's definition of layer. No code changes | A new session | About 1 to 2 hours. A guess: those trees were not read |
| 3 | Sid's two calls ([below](#sids-two-calls-step-3)) | Sid | 5 minutes |
| 4 | The rename, if he chooses one, and the corrections to the pages | A build session | About 4 to 8 hours. Not measured |
| 5 | Pieces 5 to 8 | Their own sessions | Not sized here |

Sid said "go" to these steps on 24 November. The steps and their sizes are a
session's. After them, as a piece of its own: who may see what, at any size.

## What Sid is asking

In his words, of 24 November:

- "why do we need to answer and discuss this at this stage? we have the
  stamps for each act right? why does the question of what dod we do for the
  layers arise at this point?" (‹23f4a742›)
- "my point is why do we even need this? the exact problem that would arise
  with this is that we will have a definition of layer in the system now and
  things will work according to that and at some point i would have to update
  that and migrate (or even if not migrate) the concept of layer is going to
  stay now in the codebase which introduces misalignment with me and my
  future work that we do on top. This is like a very very bad thing and a
  case of time sink because then i see something that is done but not to the
  spec i want so i would have to go inside and figure out why now the only 2
  options left are to either rip it off the code base or make it the correct
  way." (‹1466199a›)
- Of his own model: "a single act is a single act, a layer is a named group
  in wich there can be X no. of acts (but in a layer we don't talk using the
  acts because its too granular, its composition of smaller layer, the
  smallest layer is a collection of acts type (I have yet to refine my own
  mental model and terminology for this though)" (‹80b8d52d›)
- To another session that morning: "I want to close what is open so that the
  focus can shift on to the piece 5-8 time is running out." (‹d8624165›)

How the day went, a session's account. He read a summary of piece 2 of the
store's permissions and objected that the granularity looked fixed at "either
layer or 1-act". The session answered with designs, then with a definition of
"layer", which a review broke ([REVIEW.md](REVIEW.md)). The day's finding is
that the store's layer and his layer are different things, and that sessions
keep answering his sentences about layers in the store's terms.

## What the day found

### 1. The store's layer carries five jobs, and only the first is needed

| On the store's layer today | Where it is | Why it is there |
|---|---|---|
| The order of its acts (stamps), which Rama task holds it, how fast its acts are decided (its depot), the first slot of every act-id | `src-store/MODEL.md`, "The words" and "What the store keeps"; the store's decisions of 6 October (entry 5), 7 October (entries 3 and 4), 13 October (entries 2 and 3) | Needed. Order exists inside one partition only. Sid: "There is local ordering and then everything is partitioned by the layer key" (‹f8a673a6›, 13 October, dictated) |
| Who may write: a permission names a layer, and it stands by rows of that layer | `gate/chain.clj`, `stands?`; `gate/decision.clj`, `standing`; the rule `:permission-not-in-layer` | Sid's decisions of 6 October (entry 8) and 8 October (entry 3). Pieces 1 and 2 built on it on 21 and 22 November |
| Who may see: a setting of the layer, members or everyone, and any permission that stands in it | `read_path/layers.clj`, `standing` | Sid's "yes" of 12 October to a session's words, which end: "The three extras stay undecided, and the build treats them as temporary" (decisions of 11 and 12 October, entry 1) |
| What a read stacks: the layer's builds-on list, 64 at most, read one level; a view may give another list; the nearest layer that holds a fact wins | `read_path/layers.clj`, `in-order`; `door/declaration.clj`, `max-builds-on` | Sid's "yes" (decisions of 11 and 12 October, entry 2). The numbers are the build's, kept and not decided |
| Its settings, and the one holder who writes them | `gate/decision.clj`, `who-may-write-fault` | Sid's "2A" (‹93a88844›, 20 November) |

Two more things follow the same id: a replace reaches only a fact of the
act's own layer, and a key is marked for one layer (`src-store/MODEL.md`).

A session's reading: the system needs the first row. It never needed the
other four to be what a layer is. They were attached as first rules, with his
yes at the time, and then built on and reported as done.

### 2. "One act" was built for writing, and he meant seeing

- **Sid's** (‹45c7127b›, 21 November; a long prompt, which the record cannot
  tell from a paste; no session text holds the words earlier): "each act is
  the smallest atomic unit right and we can have permission over that for e.g
  the example of google sign in act(s) those should only be either with the
  system for security or only the user since its personal", and "not give
  access to data to model or students so that we tell them answer eixists but
  we are testing you and that require granular permissions". Both examples
  are about who may see a piece of data.
- **In the code:** a yes for one act is a permission to have one act taken,
  once (`d3290f37`). "A yes for one act lets nobody see"
  ([piece 2's spec](../runtime-2026-11-21/store-piece-2/SPEC.md), R8). The
  store's page says of his line: "Not built for what a reader sees: a thing is
  hidden by the layer that it sits in" (decisions of 20 and 21 November,
  entry 4).
- **Sid's,** the same evening: "auth is by layer? but did i not say that its
  complicated and we have to have it granular? what am i missing"
  (‹a92b99ef›).
- **A session's reading:** the same line of his holds a second, separate
  thing, how often someone is asked: "the spectrum of everytime to always
  allow and that can be per run", for "read write access". Piece 2 joined the
  two into a yes for one act. The yes is still a real thing, approval of one
  act of an agent. It is not the answer to who may see an act.

### 3. What he has already said about the model

Each was opened in the record by the session of 24 November.

**What a person sees, and how:**
- "a bunch of blocks makes a para, a bunch of para makes a section, bunch of
  section makes a page … each user based on the authenticatoin on each one of
  these have their view generated" (`vision/LOG.md`, line 90, about 28 April)
- "the question is basically what am i allowed to query" (line 91)
- To a session that proposed "coarser policy regions and derive fine-grained
  permissions by inheritance": "don't they have fine grained?? i think they do
  over each post … who you follow and what you see depends on a query", of
  Rama's Mastodon (‹4c3d7052›, 1 May)
- "the permission graph query out based on the user permission vs the data
  permission we only need to match the top ones"; "iff the readers have same
  level of clearence they will all see the same thing" (‹45c7127b›)

**Whom it reaches:**
- "if a user is part of a group they get access to the group resources"
  (`vision/LOG.md`, line 91)
- "an individual is always a part of a groups, a group is part of another
  group ... world is the open group that every group if make open their data
  publishes to" (‹56a55527›, 2 October)
- "the user which has access to write data will say make this open to xyz"
  (‹f692bacf›, 1 May)

**What must hold:**
- "we can combine these to make a bigger one but can't decompose it to atomic
  units later on" (‹e36008e1›, 23 September)
- Agents with "almost all of the permissions that I have" that still cannot
  "read my environment variable keys" (‹b6b0f700›, 18 November, a long prompt,
  dictated)
- "a lean proof that if we make a layer attached to system and another users
  and agents it is impossible for them to be exposed to anyone else. note this
  needs to cover time travel and all the edge cases like dependency of
  different things" (‹469ff0a4›, 13 November)

**The word itself:**
- "i also use layer like user defined lauers for their arbitray work butherr
  we mean this which is fine imo", of the store's use (‹61dbaec7›, 6 October)
- "i keep saying layer but i don't know what it means and how to carve it"
  (‹24214232›, 3 September). He said the same in ‹bb0ad8a0› (21 October) and
  ‹80b8d52d› (24 November).

### 4. Each carving of "layer" by a session has failed

- Four relations and three algebras, 21 October
  ([the levels map](../levels-2026-10-21/MAP.md)). He met its convergence
  with "its not a beautiful layer that is there, it feels like forcing"
  (‹55a15012›).
- Two relations, "inside" and "builds on", 24 November. The review broke it:
  neither word is his, none of the session's sortings of his phrases
  survives, and it cannot say "almost all, but not". Its twelve holes and its
  table of assumptions are in [REVIEW.md](REVIEW.md).
- By the review, and not opened by the session: four axes in
  `src/proposal/frame-2026-09-15/PICTURE.md`.

A session's reading, which Sid has not answered: his word moves across three
questions, and he answers each at its own size. What shares one order: "same
unit (block, page, canvas, box etc etc)" (‹48bd1d46›). Who may see or change
a piece of data: any size from one act up. What a read looks through:
whatever is stacked "on top".

## What is in the code from this week

| In the code | Commit | Rests on |
|---|---|---|
| A permission ends, what was given beneath it ends, it can be given again; only the first holder writes settings | `2882b55b`, 21 November | Sid's "2A" (‹93a88844›) and "yess" (‹45c7127b›) |
| A permission for one run | `3544d026`, 21 November | Sid's "per run of some task/query/agent" (‹45c7127b›); the form is a session's |
| A yes for one act, on an id of its own, used up | `d3290f37`, 22 November | A session's reading of "everytime" and "one act" (finding 2) |
| Each layer's line; its acts and refusals read by stamp | `7f1242f3` and `2eff35ef`, 22 November | Sid's "5.2 B", as carbon narrowed it |
| The read path: an unseen layer tells nothing; the rows above a permission are read after it | `27d883a3` and `5e9eca8d`, 22 November | Two reviews of piece 2 |

**Not in the code:** seeing part of a layer.

## Faults inside the built code, each with its clock

1. **A holder has one slot for a layer permission.** A second give while the
   first stands is refused `:holder-has-a-permission` (`gate/decision.clj`,
   `give-over-fault` and `permission-place`). Before the first real record.
2. **A permission that was never given is told `:permission-ended`** when the
   holder's slot holds an end (`gate/decision.clj`, `standing`;
   [the second repair](../runtime-2026-11-21/BUILD-store-piece-2-second-repair.md),
   open item 4). Before the first real record. A session's reading: it has
   the root of fault 1, and a permission at a place of its own would settle
   both.
3. **About 450 or 300 members can watch one members-only layer live through
   one runtime process.** The read path holds 900 pushes on one table under
   one routing id, under Rama's default of 1,000 on one table on one task for
   all clients, and each viewer holds pushes of her own on her permission and
   on each one above it (the second repair, fork 2; `src-store/softland/store/read_path.clj`,
   "How many pushes"). Before hundreds watch one such layer.
4. **A fact written after access ended can still be told,** until the end's
   push arrives; and the read path's rule rests on an assumption about Rama
   that no probe checked (the second repair, open items 3 and 2).
5. **Runs never made:** a yes in a batch that fails and runs again, a yes
   across a restart, a yes on a large act, more than one machine
   ([the first repair](../runtime-2026-11-21/BUILD-store-piece-2-repair.md),
   open item 5).

## The checkpoint we are heading to

A session's position. Sid said "go" to the steps toward it; he has not
answered each bullet.

It is not "layer defined correctly": he has said three times that the word is
not carved. It is that the system makes no claim about what a layer is.

- The store's unit is named and described only as the thing acts are ordered
  in.
- The four attached rules are written as rules that use that unit as their
  size for now, each with its clock.
- Pieces 5 to 8 need only "which unit do I write into" and "may she see
  this".
- His layer is nowhere in the code until he has it. It can be built later
  over acts, because acts are kept whole, with an id and a stamp.

## Step 2, for the session that takes it

**The question:** where does the code above the store, and what is planned
for pieces 5 to 8, work according to the store's definition of layer?

**Read,** entering through each tree's `README.md`:
`src-runtime/softland/runtime/`, `src-wire/softland/wire/`,
`src-auth/softland/auth/`, `src-command/softland/command/`, and in
[the runtime's page](../runtime-2026-11-21/PROPOSAL.md) entries 5, 6 and 8
and "The order of the build". Never `src/app/server/env.clj` or a `.env`
file.

**Bring back:**
1. For each of the four attached rules (who may write, who may see, what a
   read stacks, the settings and their one writer): each place that works
   according to it, with the file, the function and what it takes for
   granted.
2. Each place where the word is in a form that is stored or sent: `:layer` in
   an act, the first slot of an act-id, `:layers` and `:from-layer` in a
   query, the store's keys, the wire's words.
3. Sid's two calls, each laid out the way he asked on 21 November: the
   situation in a Softland example, the options, what each leads to, and a
   pick.

**What a grep showed, not read:** `src-runtime/softland/runtime/seam.clj`
calls `read-path/read-once` and `read-path/read-live` for its reads;
`src-runtime/softland/runtime/sign_in.clj` and
`src-command/softland/command/main.clj` are the two files outside the store
that name a permission or who may see.

**How Sid asked to be answered** on 24 November, by a skill text that he
pasted into the session (`i-have-adhd`): the next action first; numbered
steps; where the work stands, restated each turn; time estimates in units;
five items at most in a list; no preamble. A new session does not have it
unless he gives it.

## Sid's two calls (step 3)

A session's first cut. The session that takes step 2 lays them out in full.

**1. A word for the store's unit.**
- Keep "layer": no work now. Each later page and piece carries two meanings
  of his central word.
- A word of his for the thing acts are ordered in: "layer" is in no code
  until his is built. It is a rename across the store, the wire, the runtime,
  Auth, the command line and the pages, and it changes stored forms (`:layer`
  in an act, the first slot of an act-id). That is free while no real data is
  kept; after the first real record it is a new version of the act (decisions
  of 13 October, entry 6).
- A session's pick: the rename. One word with two meanings is why sessions
  keep answering his sentences about layers in the store's terms, and each
  piece built adds uses of it. The word is his. "Line" is taken: it is the
  store's table of a layer's decisions by stamp.

**2. Seeing at a size finer than the store's unit: before pieces 5 to 8, or
after.**
- After: until it is built, a hidden thing sits in a unit of its own. That is
  his own sketch for an agent's case, secrets in "the root layer"
  (‹b6b0f700›). Its clock: before other people keep data that must be hidden
  inside a shared unit, because what was shown cannot be un-shown.
- Before: the design comes first. It is open: his layer is not carved, and
  the work he pointed at, Rama's Mastodon (‹4c3d7052›), has not been read for
  this. Not sized.
- A session's pick: after, with the checkpoint above reached first.

## What not to lean on from the session of 24 November

- Its designs: "five settings" on every permission, "parts inside a layer",
  "inside" and "builds on". The review broke the last; the first two were
  never checked against his lines.
- Its "stays" for what is in the code, and its "it can wait" as first put.
  Sid's answer to both is ‹1466199a›.
- It changed position three times that day. What stayed the same from the
  review on is finding 1 and the three questions under finding 4.

## Not checked

- The runtime, the wire, Auth and the command line: a grep, no reading.
- The reviews of the spine and of Auth of 22 and 23 November: not opened.
- The review's counts, and the lines it quotes that this page does not: not
  opened by the session. It read about 420 of the 1,428 lines of
  `vision/LOG.md`.
- Rama's Mastodon, and any other work of the field on who may see: not read
  for this. A gatherer skimmed two sources on 21 November (decisions of 20 and
  21 November, entry 7).
- Nothing was run.

## Where his words are

Every id on this page is a line in the record (softland-memory); open one
with `memtool line <id>`. ‹45c7127b› and ‹b6b0f700› are long prompts. The
lines of 24 November in this session are ‹6db69c3b›, ‹ede46748›, ‹eb55ddea›,
‹23f4a742› and ‹1466199a›; ‹80b8d52d› and ‹d8624165› are his lines to two
other sessions that day. The session's own texts are in the record under
`73e60f65`.
