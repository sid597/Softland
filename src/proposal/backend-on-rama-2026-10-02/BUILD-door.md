# Build: the door — from a maker's act into a depot, and the answer back

Cut by: one of the two instances of Sid's session of 9 October 2026, the one that went through the door's
four questions with him in chat (Fable 5.1, effort max); the other instance landed his decisions and the
rename.
Amended by: the same instance, 9 October 2026, on six points from the other: the cutter's name; the
receipt's line on use; the scope of D8; the reason of the lock rule; the three proof counts; and the door's
own proof in place of a module proof driven through the door.
For: one build, run by the Rama skill's stages (see "How the build runs"). Status: Sid's decisions of 6,
7, 8 and 9 October 2026 are the contract. Nothing is left to ask before the build. The open rows that the
door touches are named under "Not in this build, by decision"; this build decides none of them.
Written for the tree as the rename of 9 October leaves it: the word **depot** in place of "class" (the
fact on a layer, the act's part and the act-id slot), the namespace **`softland.store`** on the store's
three keys, and a gate's refusal of a key in that namespace that it does not know. That rename is another
session's commit, `c296a459`, made before this build starts; on that tree the three proofs give act 150,
pure 449, module 227. Before the first stage: confirm both, with `git log` and a run; if either differs, stop
and ask.
The decisions, with Sid's reasons, are in `src-store/README.md`: the act's twelve (6 October), the store's
ten (7 October), who acts and what the store reads (8 October), the door's four (9 October). Everything else
a builder chooses is engineering, written with its reason in a docstring, never as a decision. The rules the
build session holds the build to are under "The builder's rules".

## What this build is

The door: the one piece of code by which a maker's act goes into the store and its answer comes back.
It is the proof's hand made general. Today `test-store/softland/store/module_proof.clj` plays the door by
hand: it makes an act-id, asks for locks by an act, seals each value, appends to a depot, and reads the
answer by act-id. When this build passes, any maker in the runtime's process does the same with two calls,
one to send and one to read the answer, and the door has a proof of its own against the module. The module
proof stays as it is, so that the store stays proven without the door.

```
MAKER ─act─► THE DOOR [act-id · depot · locks · seal · append] ─► DEPOT ─► GATE ─► RECORD
                                                                                      │
MAKER ◄── accepted / refused: reason / nothing yet / not mine ◄── THE DOOR ◄── answer by act-id
```

What the door is not: a check of the act (the gate is the authority, and anything can still append to a
depot without the door); sign-in; a network edge; a reader of facts for a screen; a bell.

## Read first, in this order

1. `src-store/CLAUDE.md` — the explanation rules; they bind every file of this build.
2. `src-store/README.md` — the tree, and the four decisions sections. The 9 October section is the door's
   own four; read it with the sheet it answers, `decisions/groups/g-door.txt`, whose first page is the
   picture of one act's way in.
3. `src-store/softland/store/act.clj` — the one form. Read it whole.
4. `src-store/softland/store/door/README.md` and its four files — what the door and the gates agree on: the
   declaration (the store keys and the limits, as the rename left them), the seal, the one text, the digest.
5. `src-store/softland/store/module.clj` and `module/door_reads.clj`, whole — the two depots and the three
   queries the door reads: `answer-by-act-id`, `locks-of-request`, `depot-of-layer`.
6. The hand in `test-store/softland/store/module_proof.clj`: `new-act-id`, `thing`, `send!`, `decide!`,
   `ask-for-locks!`, `take-lock!`, `seal-with`, `sealed`, `append-to-base!`, `await-answer`,
   `ask-for-base-locks!`. This is what the door generalizes. Read it as the finding of what works against
   the module, not as the door's form.
7. `decisions/OPEN-QUESTIONS.md` — the queue. Answers never live there. The rows this build must not decide
   are listed below by id.
8. `store-module/PROGRESS.md` and `store-module/FULL_SPEC_REVIEW.md` — what the module's build left, and
   the rulings of its review.
9. The Rama skill (`/rama`), before any Rama code is read or written.
10. Last, and only as findings: the rig's notes under `src/proposal/rig-2026-09-25/` on what Rama forced.
    No form, no name, no call shape and no default comes from the rig's code, the server's or Inland's.

## The decisions this build stands on

From the act (6 October): the one form is the maker's act, plain values (act 2, act 3). The door's
additions — the act-id, the depot, each value sealed — are the door's own small declaration, which the gate
checks on arrival (act 2). The seal is below the form: the door seals, the maker never does (act 3). An id is
a readable keyword or a UUID (act 9, the half on an id, which is Sid's).

From the store (7 October): two depots, the streaming depot and the microbatch depot (F6 A, Q7 a). The
members of a layer set its depot, as a fact on the layer (F1 A). The base is a microbatch layer; every other
layer starts streaming (F4 C). The door reads an act's answer from the record by act-id; an ack is a signal
and never the truth (Q22 A, F7 A). The act's id is the **act-id**, `[layer depot uuid]` (decision 10, with
the word of 9 October).

From who acts, and what the store reads (8 October): the system stands at the top of the base (1). A
signed-in person has a personal layer and a permission in it, made by the system at first sign-in (2) —
sign-in is not built, and the proof plays those acts. Permission is a hierarchy that ties back to one
person (3). A stamp is the clock time and a counter (4). The store's keys are three (5).

From the door's four (9 October), each with what it means for this build:
- **?4 A** — a person's id is a UUID that says nothing; her sign-in accounts hang off it by links. The door
  makes no person and no id for one; that is sign-in's. It carries `:who` as given.
- **H1 A** — one lock seals one value. The door asks for locks and seals each value with its own. The
  switch that moves a layer to one lock for each act is not built and not this build's.
- **K3 A** — the system can hold many layers. The door treats `:system` as a who like any other and checks
  nothing about it.
- **K4 B** — the store's keys carry the namespace `softland.store`: `:softland.store/depot`,
  `:softland.store/permission`, `:softland.store/lock-request`. The door knows them for one reason: a value
  under one of them stays plain. A gate refuses any other key in that namespace.

Words. **depot** — Sid's word of 9 October for what "class" named: which of the two depots takes a layer's
acts, `:streaming` or `:microbatch`, his two words of 6 October. **maker** — "whatever makes an act: a
screen, a converter, an agent" (the door's sheet); Sid, 9 October: "maker is fine general word for what we
are doing imo". **act-id**, **key**, **lock**, **seal**, **forget**, **the base**, **the system** as the
door's sheet defines them. A word that the build has to make says in plain words what the thing is and is
listed for Sid (builder's rule D7).

## What to build

**One namespace, the door**, requiring Rama's client API, and nothing of the gates' or the record's
internals: it uses the module's surface only, two depots and three queries. *Placement, the cutter's
engineering choice, the builder may vary with its reason:* `src-store/softland/store/door.clj`, namespace
`softland.store.door`, at the top of the tree beside `act.clj` and `module.clj`; the folder `door/` keeps
the four pure agreements and says in its README that the door itself now sits above it. The pure parts of
the door (the act-id's making, telling a value fact from a store-key fact and a retract) may be split into a
pure namespace so that the pure proof can hold them; builder's choice.

1. **Opening the door.** The door is opened on a cluster handle and the module's name, and holds the two
   depots' handles and the three queries' handles. It owns no durable state. Its one memory is a stock of
   locks (part 3), the memory of one process. In this build the door is the module's only client in the runtime
   (3 October: the runtime is the only Rama client), and nothing else in the runtime touches a depot or a
   query. Where a read's steps run (W3) is open and not decided here.
2. **Act-ids on request, ahead of sending.** `act-id-for` a layer gives `[layer depot uuid]`: the layer's
   depot as the module gives it (`depot-of-layer`), or the depot that the layer starts with when it is not
   made (`first_act.clj`), and a random UUID. A maker may ask for the ids of a series before it sends the
   first, as `act.clj` allows; the door keeps none of them. An act-id is made once and never reused.
3. **Locks, asked for as acts under the maker's who and permission.** When the door needs locks for a
   maker's acts in a layer, it sends a lock-request act whose `:who` and `:permission` are the maker's,
   whose `:layer` is the act's, and whose one fact is `{:e <the uuid of the request's own act-id>
   :k :softland.store/lock-request :v n}`; it reads the request's answer by act-id and then its locks with
   `locks-of-request`. A lock-id is `[request-act-id index]`. *Cutter's rule, with its reason:* a lock is
   used only for values of acts that carry the same `:who` and `:layer` as the request that made it. Forget is not designed (K11, H10, H11); the rule keeps room for a forget of a
   person that finds her locks by her requests, and it costs nothing now. It is the cutter's, not a decision. The stock — per layer and who, how far ahead it asks, how many at once (the declaration's
   limit is the ceiling) — is engineering, with its reason in the docstring. A lock's bytes never go into a
   depot (the store's builder's rule 5). A refused request is handed to the maker as the gate's refusal; the
   door retries nothing on its own. A lock that a stock loses at a restart is a row that seals nothing, for
   good; the door never destroys one (forget is not its work); the plan says how small a stock keeps that
   cost, and the receipt counts the rows it left.
4. **Seal and send.** Given a maker's act in its one form, the door reads `:layer` and `:facts` and no
   more. For each fact that carries a value and is not under a store key, it takes a lock from the stock and
   seals the value to its place with `seal.clj`, giving `:sealed` bytes and a `:lock-id`; a fact under a
   store key keeps its plain value; a retract has nothing to seal. It adds `:act-id` and `:depot`, appends
   the thing to the depot the act-id names — the streaming depot with the ack level `:ack`, the microbatch
   depot with the ack the module's docstring describes — and gives the maker the act-id, the digest of what
   it sent (`digest.clj`), and the ack as a signal, never as the answer (F7 A). The door checks nothing of
   the act's form; a malformed act goes in and comes back as the gate's refusal. The door never fills a
   missing part, never reorders facts, never names a permission for the maker, and changes the act only by
   the three additions its declaration names. When the door cannot do a step — no `:layer`, a layer not
   made and no depot to start with, a stock empty and the request refused, an append that threw — it tells
   the maker in the door's own words, which are not a gate's refusal and are kept in no record, and it
   appends nothing for that act. The words are the builder's, plain, listed for Sid (D7).
5. **The answer back.** `answer-of` an act-id and the digest the door gave on sending reads
   `answer-by-act-id` and gives one of four: accepted, refused with the gate's reason, nothing yet, or not
   mine — an answer under this act-id whose digest is not of what was sent (the store's builder's rule 11).
   For a microbatch act, the door waits as the maker asks, in a bound the maker gives, reading by act-id
   until an answer is there. That wait is one question about one act-id. It offers no subscription and no
   stream of changes, and it decides nothing of W4, what tells a screen that a fact changed. The door keeps
   nothing of what it sent once the call returns; the maker holds the act-id and the digest.
6. **The sender, until sign-in.** The door takes `:who`, `:session` and `:permission` as the maker gives
   them. It cannot check that the sender is the who it names, and does not; neither can the gate, and its
   README says so. The door's docstring says the same in plain words. A sign-in built later stands in front
   of the door and refuses before this code runs; it changes none of the five parts above. Who may act as
   the system (K9) is not decided; a maker that names `:system` is treated as any who.
7. **The door's own proof.** A fourth proof, `test-store/softland/store/door_proof.clj` (the name is the
   builder's), on Rama's in-process cluster as the module proof runs, through the door for everything the
   door covers: acts to both depots, lock requests, sealing, answers, the base's batches. Its own cases: each
   of the door's own words; the not-mine answer; that a lock never crosses a who; that no lock's bytes are
   ever appended; and that what the door sends for an act is what the hand sends for the same act, but for
   the random parts. The module proof stays as it is: the store stays proven without the door, and its hand
   stays the proof that anything can append to a depot without the door. The act proof and the pure proof
   are unchanged, unless the door's pure parts are split out and the pure proof takes them. The counts
   before are act 150, pure 449, module 227; the receipt gives them after, with the door proof's own line.

## The builder's rules

The store's rules 1 to 21 (`BUILD-store-module.md`) hold where they touch the door; 5, 11, 15, 16 and 20
do. These are the door's own. Each has its source. The plan decides how each is met.

- **D1.** The door checks nothing of the act; the gate does (act 2, door/README.md). The door's own words
  name only what the door could not do, never a judgment of the act.
- **D2.** An act-id is made once and never reused; the door gives ids on request and keeps none (act.clj).
- **D3.** A lock is requested under the maker's who and permission, and used only for that who in that
  layer (part 3). The stock is the memory of one process; a restart asks again.
- **D4.** An ack is never the answer (F7 A). The door reads the record by act-id, and compares digests.
- **D5.** A wait for an answer is bounded by the maker's word and is one question about one act-id. No
  subscription, no bell; W4 is open.
- **D6.** The door never opens a sealed value, never reads a fact of the record for a maker (group 6 is
  open), and reads locks only of the requests it made itself.
- **D7.** Every word the door makes is plain, says what the thing is, and is listed for Sid in the door's
  README under "Words a session made, Sid's to change". No word comes from the rig's code.
- **D8.** In this build the door is the module's only client in the runtime; where a read's steps run (W3)
  is open. The module proof's hand is the one exception, and the proofs' README says so.

## Not in this build, by decision

- A first maker: no converter, no screen, no agent. The proof is the only maker. (The cut agreed on
  9 October: naming a maker would pull a converter's decisions and K9 into a build that needs neither.)
- Who may act as the system (K9).
- Sign-in: persons, their UUIDs, the links to their accounts (K7, K8, K10, Q16); sessions and their ids,
  an ingestor's session, acting after a session closes (E5, E6, Q19).
- Reads of facts for a screen, resolve, context, the tower of layers (group 6, W1 to W8); what tells a
  screen that a fact changed (W4); bells and the feed.
- The switch of a layer to one lock for each act; forget's destroy step and what the store hands to the
  one who forgets (K11, H10, H11).
- A network edge. The door is in-process Clojure; how a screen reaches the runtime is above the act
  (group 7) and not this build.
- Re-class (?9), promotion (?10), what a key is (K5). The two words of K6 are given by the rename.
- Any change to the gates, the record or the declaration. The module's surface — two depots, three
  queries — is enough for the door. If the build finds that it is not, that is a finding for the receipt,
  not a change.
Do not build any of these. Where the door's code must leave room for one, say so in the docstring.

## Explanations, as the tree's rules say

`src-store/CLAUDE.md` binds every file. Each namespace and each function has a docstring; each folder a
README with a parent link; no function lists in READMEs; no history of corrections. The door's namespace
enters the tree's README map. `door/README.md` no longer says the door is not built: it says where the door
sits and what the folder gives it. The door's own explanation holds **"Decisions this build stands on"**
(the four of 9 October by row, the rest by pointer), **"Engineering choices"** (the placement, the stock,
the wait, the door's words, how each of D1 to D8 is met), and **"Words a session made, Sid's to change"**.
`module/README.md`'s line that any caller can read a lock's bytes stays true and now names the door as the
caller it meant. The proofs' README gains a row for the door's proof and leaves the module proof's row as
it is. Sid's answers are in `src-store/README.md` as his, never in these files as new text.

## How the build runs

By the Rama skill's stages, as the store's module ran: the implied spec, the split into parts, the plan
and the check of the plan each in a fresh session, then one build session, then one fresh review of the
whole against this contract. The build session judges each stage's paper. The papers go in
`src/proposal/backend-on-rama-2026-10-02/door/`, never under `src-store/` or `test-store/`. A build's page,
`door/PROGRESS.md`, records where the build stands, for a hand-over.

## Commits and the receipt

Commit on `main`, grouped by concern (the door; its proof; the maps), each message saying
what and why. No push. No `Co-Authored-By`, no `Claude-Session`, no attribution line of any kind. The
receipt to Sid: the line "proven by a hand-played maker, used by no inhabitant yet", so that presence,
tested behaviour and use stay apart; the commit hashes; the four proofs' outputs in full, with the count
lines before (act 150, pure 449, module 227) and after; every provisional choice made (the placement, the stock, the wait, the words); every place this
contract could not be met as written, and why; what the build found that is not decided, proposed as rows
for the queue and decided by nobody; `git status --short` (expect only `src/app/server/README.md` modified
and `docs/.obsidian/`, `docs/Untitled.md` untracked — leave them).

Never read `src/app/server/env.clj` or any `.env` file.
