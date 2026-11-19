# The store's first who: handoff of 18 November 2026

**19 November 2026.** Sid turned the choice of D that this page records:
nothing in the store holds a key ([PROGRESS.md](PROGRESS.md) has his words
and the grounds). Step one, which was built from this page as commit
`8ba0da10`, was taken off `main` by `3873f78b`. This page stays as the record
of the exploring session. It is not a source for a build: what it says to
build, its table of later steps and its leans on keys are superseded. Two of
its threads are still open and are carried on the progress page: Sid's agents
and reading, and running a store that keeps its data.

This page passes one exploring session on to a session that builds the first
step and discusses what is open. The exploring session ran on 17 and 18
November 2026 and is named "signin build" (id
`9c83607d-6b73-47ff-a45b-1e93e1df22ca`).

It is working material and not a decision page. Three kinds of line are kept
apart:

- **Sid's words**, quoted as he typed, dictated or wrote them.
- **Facts**, marked as checked by the session, or as reported by a gatherer
  and not opened by the session.
- **Leans**: positions of the session with their grounds, which Sid has not
  ruled on. A lean can be turned.

Paths are relative to the repository root unless they start with `/`.

## The problem

Every act in the store says who wrote it, and the store believes whatever it
is told: "Until sign-in is built, any sender can make any layer that is not
made, for any holder" (`src-store/softland/store/gate/first_act.clj`, lines
30 to 33, checked), and "`:system` is treated as any who"
(`src-store/softland/store/door.clj`, line 111, checked).

Sign-in was built on 15 October 2026 as what would make "who" true, and it
was taken off the same night (`src/proposal/sign-in-2026-10-15/RECEIPT.md`;
that folder is a record and not a source for a build).

The session's finding: a sign-in is itself a write, and that write needs a
who. It cannot be the person signing in, because she is what the sign-in
produces. So a store's first who has to exist before any sign-in, and nothing
had said what it is. Sid's line of 2 November (‹ce0db85e›): "even if i have
google sign in enabled someone before that should exist to carry in the
googles request on behalf of a user".

## What Sid said

In order. Two ids are from the record; the other lines can be found there by
their words. The handwritten pages are kept at
`/mnt/data/projects/research/softland/first-act-pages-2026-11-18/`, and what
is quoted from them is the session's reading of his hand.

- 17 November: "ok but I am saying the system writes inside softland itself I
  am not sayign that we hardcode it".
- 17 November (‹ecd05909›): "do we make aome hash but then it will be
  different for different deployments and maybe in a few months softland will
  already come with the system user and some pre data needed to build on
  top".
- 18 November, handwritten page 1: "Empty store who is the first writer? I
  think it is some softland system or smth. and this is in store as well like
  in a way I am pre baking a user". And: "I want to have a proof of
  correction that doing thing in a particular way is sound and can't be
  broken as a security vulnerability in future."
- 18 November, page 2: "First writer the system → sign-in → Me → My LLM". An
  LLM that cannot do a task in its box: "anything that is not able to figure
  out or in your hands you can raise it to the system". And: "currently I am
  both the system's proxy, Me as the user of system and on top builder".
- 18 November, page 3: "every deployment atleast should be registered with me
  in the min way like this is the deployment with this name and id, why?
  First so that no collision and second to maintain their system layer if
  want to sub to the system layer changes." And: "we should have a concrete
  spec and proof I think formal proof that given this spec and requirements
  and implement it is not possible to break in given any scenario."
- 18 November, his drawing of the structure:

  ```
  My Softland System (also first user in my deployment)
   └ sign in → Me
        └ makes → My LLM  ··· Informs ···> My Softland System
  In future: Registers → deployment
                └ has its first user same as softland but prefilled
                     └ sign in └ …
  ```

- 18 November, with the Linux comparison: "the softland has the "root"
  equivalent user plus its updating its own deployment iff needde be and then
  all other deployments can subscribe to those changes and get the new ones
  based on their own heuristics".
- 18 November: "the store needs every act to have a who ... and the very
  first act can't say who is being registered without a who, but the who does
  not exist so what is it verifying against." And: "the solutions that we
  come up will should hold throught eternity over the cases i have defined
  and are yet to be defined."
- 18 November: "I think I prefer D .. lets talk about how it is to be done."
- 18 November, dictated: "Yeah, we need to think about what happens on a
  restart on the same data."
- 18 November, dictated: "The root user is basically the code base, and the
  developers and the management has access to change it. That is what the
  root would map to in Softland case." And: "So the first act would be
  creating some kind of root user. Okay, that's good."
- 18 November, dictated, the choice: "I think the answer might be D, and the
  choice is D."
- 18 November, on how to work: "We should be thinking from first principles,
  this is a pure engineering and analytical and critical thinking part". He
  added the same to `CLAUDE.md` that day.

The thread he opened last and asked to carry is under "Keys, agents and
reading" below.

## The ways out, and D

The first act cannot be checked against anything inside the store: checking a
who means comparing an act with something an earlier act wrote, and the first
act has nothing earlier. So the first act does not identify root; it defines
root. What can be chosen is what that definition is anchored to. The session
listed six anchors and thinks the list is complete, because these are the
only things that exist when an empty store looks at its first act: the act
itself, the order of arrival, the code, what the deployer gave the program,
the machine, and the world outside.

- a. Root is a word in the code. This is the store today.
- b. Root is a public key in the code.
- c. Root is what the deployment's settings name.
- **d. The first act itself carries a key and is signed with it.** Sid's
  choice.
- e. Someone above countersigns the first act.
- f. The machine's chip vouches for what is running.

The session's view, which Sid raised himself as a question: these are one
mechanism with a spectrum of settings. D is the general form; a, b, c, e and
f are D with particular values. So the code implements one rule, and what a
first act may say is the spectrum.

## Step one: what to build

The smallest version of D, in order:

```
1  a setup tool makes a key pair
2  it asks the door for an act id for the base
3  it builds the base's first act, signs it, sends it, and keeps the
   signed act as a file
4  the gate accepts it only if the base is not made yet and the
   signature matches the key the act itself declares
5  from then on, an act by root in the base is accepted only if its
   signature matches root's key, read from the base
```

Sign-in, persons, programs, handover, any layer other than the base, and any
store that is kept are not in step one.

### Before any code: the message `build.md` asks for

`.claude/output-styles/build.md`, "Before building from a page", asks the
building session to send Sid one short message and wait for his go. What it
needs is here.

**His decisions that step one rests on:**

- The choice of D (18 November, quoted above).
- "The system stands at the top of the base's chain" (`src-store/README.md`,
  "Decisions of 8 October 2026", entry 1, checked).
- The tables must be reproducible from the kept acts (the prompt of the build
  "run again", `src-store/README.md`, lines 875 to 878, checked).

**The session's choices, each to be marked as the builder's and shown to
him:**

- The store's first act is the base's first act, and root is the holder it
  names. For such an act the two rules of the making act that name `system`
  become "the key this act declares".
- An act can carry a signature. That is a second version of the act's form.
  Acts of version 1 keep their meaning, so the kept fixture still gives its
  tables.
- The making act gains a third fact: its holder's public half and the kind of
  proof, under a new store key.
- What is signed is the existing digest, taken without the signature part.
- The check sits in `standing`. The first act is checked against the key it
  carries and reads nothing. A later act by a holder who has such a fact in
  the layer costs one more row read.
- Verifying uses the JDK's Ed25519 and is wrapped so that it never throws.
- The word for the new store key. In this tree "key" is the key of a fact and
  "lock" is an encryption key, and a check enforces it (see the code map).
- Java 21 for the store, which is what this machine runs.
- The private half made by the setup tool is written to a file. This is for
  the throwaway stores of the checks only (see "Keys, agents and reading").

**The new things step one adds: seven.** A second version of the act's form;
one store key; one rule in the gate; a verify function; a digest without the
signature part; a setup tool; the checks. One thing changes: for acts of the
new version, `making-act?` no longer names `system`.

**Where to stop and bring it to him:** any rule for a layer other than the
base; anything that changes what an act of version 1 means; anything written
into `docs/decisions.md` or a "Decisions of …" section, which is shown item
by item first; and any step towards a store that is kept.

Nothing in step one is permanent. It runs on the in-memory cluster of the
checks, and a store's first act becomes permanent only when a store is kept.

### What the checks should show

Five statements, for every sequence of acts:

1. At most one first act of the base is ever accepted.
2. An act is accepted as root's only if made with root's key.
3. Nothing from another who inside the store, and nothing that names another
   key, is accepted as root's.
4. Sending the kept first act again changes nothing: a store that has it
   answers without deciding again.
5. A restart on the same data writes nothing and changes nobody's proof or
   permission. This one needs a store that is kept, so in step one it can
   only be stated.

## The code as it stands

Reported by a gatherer on 18 November, with file and line; the session opened
only what is marked checked. Paths are under `src-store/softland/store/`
unless they say otherwise.

- **The act's form.** `act.clj`, `shapes`, lines 152 to 163: `:version` (must
  be 1), `:who`, `:layer`, `:permission`, `:session`, `:based-on`,
  `:because-of`, `:claimed-when`, `:subjects`, `:facts` (checked). A part
  outside the shape is refused `:unknown-part` (`conform-parts`, line 375),
  so a signature part is refused today.
- **A value cannot be bytes** (`act.clj`, lines 286 to 311), so a public half
  and a signature travel as text.
- **The one form on arrival.** `door/declaration.clj`: the door adds
  `:act-id`, `:depot` and `:door-time` (lines 326 to 335); `check` is at
  lines 693 to 724.
- **The bytes to sign exist.** `door/text.clj` gives a text of anything that
  is the same in every process (lines 224 to 229). `door/digest.clj` gives
  SHA-256 over that text for the whole arrived thing with `:door-time` taken
  out (lines 21 to 37, 54 to 79, 81 to 96). The act id and `:depot` are
  inside it. A maker who sends `:facts` as a list gets another digest than
  one who sends a vector.
- **Act ids.** `door.clj`, `act-id-for`, lines 360 to 381: `[layer depot
  uuid]`, the last a random UUID, and it can be taken ahead of sending. In
  `module/walk.clj`, lines 282 to 288: if an answer already sits at the act
  id, the walk writes nothing and ends. So a signature over the digest is
  good for one acceptance in one layer.
- **The making act.** `gate/first_act.clj`, `making-act?`, lines 81 to 122:
  the gate's depot is the one the layer starts with; the permission is
  `[holder layer layer]`; `:who` is the holder or `system`; for the base the
  holder is `system`; and the act holds exactly two facts, the layer's depot
  and the holder's permission. `gate/decision.clj`, `standing`, lines 154 to
  224, accepts it only when the layer is not made. `places`, lines 332 to
  363, leaves a making act out of two checks.
- **`system` and `base`** are defined at `gate/first_act.clj`, lines 44 to
  53 (checked). The docstring says the words are Sid's and the keywords a
  session's. The kept fixture
  `test-store/softland/store/run_again_fixture/acts.edn` holds 33 acts by
  `:system` and 178 in `:base`.
- **Nothing writes the base's first act today** but the checks. There is no
  start-up code, and no program opens the door.
- **What a gate reads** (`module/walk.clj`, lines 246 to 403): the answer
  row; the permission row of the act's who, and only if that is nil the
  layer's depot row; the places the act replaces; the marked keys for an
  accept; the last stamp. Every key begins with the act's own layer
  (`record/rows.clj`, lines 173 to 179).
- **The store's keys** (`door/declaration.clj`, `store-keys`, lines 231 to
  260): depot, permission, who-may-see, builds-on, marked-keys. The last
  three take a second write. `who-may-write-fault` (`gate/decision.clj`,
  lines 283 to 300) always gives nil and is marked "Not decided".
- **A permission row** sits at `"<layer> <holder> :softland.store/permission"`
  (`gate/decision.clj`, lines 111 to 119), and check (d) reads that one row
  (lines 208 to 215).
- **A session** is checked for its shape and nothing else.
- **The door's five calls** (`door.clj`, lines 19 to 29): `open`,
  `act-id-for`, `act-ids-for`, `send-act`, `answer-of`.
- **Signatures.** Nothing pins a Java version for the store. This machine
  runs OpenJDK 21.0.13, where `Signature.getInstance("Ed25519")` resolves.
  The pure code must load with Clojure alone (`src-store/README.md`, lines 59
  to 63), which the JDK's own classes do not break. `door/digest.clj` already
  uses `MessageDigest`.
- **Checks.** `test-store/softland/store/pure_proof.clj`: a case is
  `[sentence (fn [] …) expected]` under `holds`; the first-act cases are near
  lines 1158 to 1247, and `(deftest the-first-act …)` is at lines 3866 to
  3867. Run with `clojure -M:store -m softland.store.pure-proof`. The check
  `no-code-of-the-store-names-a-lock` (lines 3576 to 3588) fails on any
  symbol or keyword with a word that starts with "lock", "seal" or "stock".
- **A changed form.** The gatherer reads decision 6 of 13 October
  (`src-store/README.md`) as: a changed act form is a new version, or a
  deliberate change of rule with the fixture made again. The session did not
  open that entry.

## A gate reads only its own layer

Checked by the session on `src-store/softland/store/README.md`:

- "A decision reads rows of the act's own layer only, so it needs no order
  between two layers, and the depots hold none" (lines 91 and 92).
- "A decision is made of the thing and of the gate's own rows on the layer's
  task, and of nothing else" (lines 94 to 99).
- "The walk never gives its task back. Between its first read and its last
  write stands no partitioner, no yield, no read that may yield and no
  append, and none may be added" (lines 813 to 822).

And on `src-store/README.md`, lines 879 to 882, a session's finding that the
page says Sid has not seen: "for a second run to give the same, a gate can
read nothing whose order no depot holds."

So a gate in one layer cannot look up a key that lives in another layer.
Step one does not meet this, because root's key and root's acts are both in
the base. Step two does. The session's lean for then: the key that may act
under a permission is written in that layer's own line, beside the
permission; a layer's first act declares its holder's key, and each give
names the next holder's key. What ties layers into one deployment would be
root's own layers listing which layers belong. What it costs: changing a key
is one act per layer where it holds a permission, and "who may make a layer"
cannot be a rule of a gate.

## The later steps, and what each raises

| Step | What it raises |
|---|---|
| 2. Root gives rights to a program and to a person's own key | Whether programs are their own who; how root's key is replaced; the lean above |
| 3. Sign-in stores what Google said and makes the person | Whether Sid's answers of 15 October stand: no client secret, keep Google's account number, agents as their own identities. No page holds them since the revert |
| 4. After sign-in | What the browser holds so that her next act carries her |
| 5. Ending a permission | Its design. Nothing in the store ends a permission, and every "if stolen" case needs it |
| 6. The first store that is kept | Who holds root's key; where it runs; one machine against the decision of 12 October |
| Later | Agents; informing the system; other deployments |

The session's leans on some of these, none ruled on:

- **Replacing root's key.** Only by a key committed in advance, as KERI does:
  the working key has no power to replace itself. Then the committed key is
  the real top and the one to keep furthest away. Two are committed, kept in
  two places, so that losing one is survivable.
- **No waiting periods and no dated expiry as rules of a gate**, because a
  gate reads no clock. The queue page records that under the decisions of 14
  October; the session did not open the entry.
- **Who holds root's key.** Seven answers were laid out: the person is root;
  a root key the person holds and rarely uses, with the person's own key
  working under its give; the server; nobody after a group is named; several
  keys that must agree; someone above; only code deploys. The session leans
  to the second for Sid's deployment. It buys something only when root's key
  is on another machine than the server.
- **The setup is deliberate.** An empty store at start means "wait for
  setup", and an empty store where one was expected means lost data and
  should stop loudly.

## Keys, agents and reading

Sid opened this at the end and asked that it be thought through in the next
session. His words, dictated on 18 November:

- "When you say with a key, the act itself carries. You are here not making
  any choice about who is making the key and where the key is going to be
  stored and how is it going to be managed, refreshed, or anything like that.
  Is that a separate infrastructure in itself?"
- "Since I have a key, I have to store it somewhere that anyone except me as
  the human can read, because I will be part of many groups. I will have my
  own agents, which might have like almost all of the permissions that I
  have, because I want them to work through things, but still I want them to
  not be able to read my environment variable keys or some other type of
  data." The session read "anyone except me" as "no one except me".
- "who can read root's data even if they have some kind of key, who can write
  it or read it. I think we did not think through this in this session".
- "Then there would be also code related to how we give read and write
  access ... this is an independent thing that all layers might have as a
  spectrum".

The session's answer to the first, as a lean:

- The store only ever sees public halves and signatures. Everything about a
  private half happens outside the store, on the side of whoever acts. So it
  is a separate thing from the store.
- It is not an infrastructure to build. Making a pair is one call. Keeping a
  private half is done by things that exist: a file, the operating system's
  keychain, hardware that signs without giving the key out, a passkey in the
  browser, a cloud's key service. What is ours is one small seam: something
  that is handed bytes and gives back a signature, and hides where the
  private half is. The store's code never changes with the keeper.
- Replacing a key has a store side, which is step two, and an outside side,
  which is the same seam.
- What stays to be decided, at the step where each becomes real, is the
  default keeper for each kind of who: root, a server program, a person, an
  agent.

What his second point changes, and what it opens:

- **A private half that matters cannot be a file that his own agents can
  read.** Agents run on his machine as his operating-system user, so they can
  read any file he can. The session had named "a file outside the repo" as
  the default place for root's private half; that holds for throwaway stores
  only. For a kept store the keeper has to be one that will not give the key
  out and that asks for a human gesture on each use.
- **An agent never needs his key.** It is its own who, with its own key and a
  give beneath his. What he keeps to himself is what that give leaves out.
- **Reading was left out of the session.** Everything above is about who may
  write. What stands on reading, from `src-store/README.md`, "Decisions of 11
  and 12 October 2026", checked in part:
  - A layer has a setting, members or everyone, and a permission says see, or
    see and act (entry 1).
  - A read is done in the runtime, straight from the tables (entry 3).
  - "Only programs on machines that Sid runs may reach Rama's ports", and
    each such program "is trusted with the whole store, in any person's
    name" (entry 4).
  - The tables keep a value plain; whoever wants a sealed value seals it
    before the door (entry 5, and entry 2 of 12 October).
- **So two things are open on reading.** First, a read has to prove who asks,
  as an act does; nothing does that today. Second, his agents are programs on
  machines he runs, so the rule about Rama's ports does not separate him from
  them: an agent that can reach the ports can read every table. Keeping
  something from such an agent means either the agent is boxed away from the
  ports, or the value is sealed before it reaches the store with a key the
  agent cannot use.
- **Use without seeing.** For a secret such as a key to an outside service,
  the question is whether an agent can be given the use of it through a
  narrow program without ever being shown it.
- His ask on 13 November for a proof that a layer cannot be exposed to
  anyone else (‹469ff0a4›) belongs here. A session answered it then and left
  seven questions; the exploring session did not read them.

## Running a store that keeps its data

Reported by a gatherer on 18 November; Rama's behaviour is read from the
docs and not run, except what the July pages saw.

- The checks use an in-memory cluster that writes under a new folder in
  `/tmp` each time and never reopens one. On it every start is an empty
  store.
- A one-node Rama 1.6.0 cluster ran on this computer in July 2026, in the
  earlier server's work (`history/docs/durable-ground/`, `bin/land`,
  `ops/rama.yaml`). Its pages record that acknowledged writes survived a
  killed worker, a full stop and start, and an unplanned reboot. Its install
  and data are still at `/mnt/data/rama/`, and nothing of it runs (checked by
  the session). That work is an example: its findings are evidence, and none
  of its forms is a source.
- A real cluster needs its data folder set on a disk that lasts, a known
  ZooKeeper folder, the daemons started under supervision at boot, and a
  client that opens a cluster manager.
- Traps seen in July: an interrupted shutdown is remembered and resumes at
  the next boot; a drain sometimes hung, and killing the cluster during it
  left it stuck until one ZooKeeper entry was rewritten by hand; start at
  boot was never set up; the backups left out ZooKeeper's data; a restore
  onto a clean machine was never run.
- The MacBook Air has 8 GB, and Rama's defaults want about 1 GB for the
  conductor and 4.5 GB for each worker. The docs name macOS and do not name
  Apple silicon or sleep. The record shows an install attempt there in April
  that failed and no cluster running there.
- Rama is free for production up to two nodes. Its backups are in the paid
  tier; on the free tier a backup is taken by hand with the cluster stopped.
- Rama's ports have no login.
- `src-store/README.md`, decisions of 12 October, entry 5: "Three machines,
  three copies and Rama's own backups, when real data is kept". One machine
  as production departs from that, and it is Sid's call.
- From inside, a store that is new looks the same as a store that lost all
  its data. That is why the first act is kept as a file and why sending it
  again must be harmless.

## What other systems do

Two research folders hold the sources with quotes and links. A subagent
fetched them; the session opened none.

- `/mnt/data/projects/research/softland/root-identity-and-merge-2026-11-18/`:
  systems whose instances start apart and later exchange or merge.
- `/mnt/data/projects/research/softland/top-key-custody-2026-11-18/`: where a
  top key lives, how it is replaced, and what loss and theft cost.

What the session took from them:

- Giving each instance its own root is normal and harmless. What makes a
  merge painful is an instance's own root writing starting data that is not
  its own. Systems that merge well make the starting data once and each
  instance receives it.
- Root owns almost nothing in the mature systems: the list of who may do
  what, and its own replacement.
- Loss and theft pull in opposite directions. The systems answer with
  succession fixed in advance, and with splitting a key among holders.
- Whoever runs the machine and holds the backups is above any key.
- A stolen key is survivable only if someone notices, so every use of the
  top key raises an alarm; and the tool that signs as root shows in plain
  words exactly what is being signed.

For other deployments, which Sid marks "in future": his own picture is above.
The session's leans: a deployment's root is its own, born in its own first
act; his system layer arrives unchanged underneath, and the deployment's own
system layer sits on top; publishing is its own act, a mark on the line that
says "up to here is for subscribers", made by a who whose proof is not on the
serving machine.

## The proof

Sid asks for a concrete spec and a formal proof. The session's lean on the
road, in order: a page of statements in plain words that he redlines; a small
model of the rules run through a model checker, which shows a break as a
list of steps; the proof itself; and a tie to the code by running the model
and the real gate on the same generated acts. The gate's decision is pure
code, which is what makes this reachable
(`src-store/softland/store/gate/decision.clj`, namespace docstring, checked:
"each can be proven without a cluster").

A proof can say that every who's box holds, whoever is steering what is
inside it. It cannot say that an agent does the right thing inside its box.

## Not verified

- Whether the session's reading of each handwritten word is right; "in the
  min way" was read as "minimal".
- Everything in "The code as it stands" that is not marked checked.
- Decision 6 of 13 October and decision 1 of 14 October, which the session
  knows only through a gatherer and the queue page.
- What one more row read costs for each signed act. Nothing measured it.
- Anything about running on the Mac, a start at boot, and a restore onto a
  clean machine. Only a run settles those.
- Whether a signing key used in two stores needs the store named inside the
  signed bytes. For root it does not, since each store's root has its own
  key.

The gatherers' full reports are in the exploring session's transcript and
nowhere else. What a build needs from them is on this page.
