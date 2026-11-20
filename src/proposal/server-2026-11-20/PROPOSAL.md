# The server: a proposal

**20 November 2026, second writing.** Written by an exploring session
(Fable 5.2) at Sid's word. The first writing was reviewed the same day by a
fresh Fable agent at max effort, whose job was to break it. This writing holds
what that review changed, what Sid's push on Electric changed, and each call
that is still open with both of its sides. Sid, 20 November: "i will take it to
a final session and there it will think through and then we will build."

Nothing here is built. This is working material and no decision page. Three
kinds of line are kept apart:

- **Sid's words**, quoted, with the id of his line in the record;
- **facts**, each with how it was checked;
- **a session's choices**, which Sid has not ruled on and can turn.

A session that builds from this page first sends Sid the one message that
`.claude/output-styles/build.md` asks for, and waits for his go.

## In one breath

The server is one JVM program of ours between every system that connects and
Rama. It does what the store cannot do: it turns a credential into a who, and
it paces a stranger before she reaches a log that is never trimmed. An act goes
on to the door and a query to the read path. Nothing true is kept in the
server: what must last is in Rama.

For a browser, the server's part is the server half of one Electric program.
For every other system it is a few words of data over HTTP. Both stand on one
seam of Clojure and Missionary.

A word on the word. Sid, 19 November (‹54c2742c›): "server does not mean a
concrete thing to me". Here it means exactly one program: one JVM process that
we write. His decisions of 11 and 12 October call the same program "the
runtime". Which word stays is his.

## What is decided, and by whom

Sid's, each in the record or on the store's pages:

- A person signs in with Google at the server. The server finds her id from
  Google's account id, or registers her, and writes that id as the who of each
  act that she makes. The store checks on each act that this who may write
  there. Nothing in the store holds a key. His go of 19 November (‹afbff6f5›)
  to a session's answer; `src/proposal/first-act-2026-11-18/PROGRESS.md` holds
  his words and what stays his call.
- Only programs on machines that he runs may reach Rama's ports, and "a program
  that is not trusted that far talks to the runtime" (`src-store/README.md`,
  11 and 12 October, entry 4).
- A read is done in the runtime, straight from the tables, with Rama's push
  (the same section, entry 3).
- A person's id is a UUID that says nothing, and her sign-in accounts are
  joined to it by links (9 October, entry 1).
- A model's reads are kept, always (12 October, entry 4). An act from a maker
  who may not act is written to the log and then refused (entry 7); a bound on
  one sender and a block are due before a stranger can reach the door.

What Sid asked of this page, 20 November:

- "Browser only what about cli, mcp ?? Because cli would be the way to connect
  and do work for some time imo" (‹8eb577ab›).
- "we should not be needeing something outside of rama because then the
  question is like scale" (‹2c90529b›).
- "we need to think from the user requirements pov like not human agents but
  the systems that are going to connect" (‹354c5f08›).
- "I want to advocate for the electric usage ... why not all reactive i mean
  even if smth is not reactive code thwn its behavior is same as clojure
  right?" (‹cd95c152›).

Everything else on this page is a session's.

## What forces a program in front

These are facts, not choices.

- **Rama asks no name.** Its client protocol is "JVM exclusive" and "doesn't
  currently use authentication or encryption" (Nathan Marz, Rama user group,
  28 August 2023). The release notes through 1.9.0 hold no line on either
  (searched for the words). For a client that is not JVM, Marz: "You'd need to
  make a JVM-based process in-between" (30 August 2023).
- **The proof that it is her is a live sign-in.** It cannot be put into a log.
  So the check that it is her is made before the depot.
- **A read never passes a depot.** It goes to the tables. So who asks must be
  known in front of the tables.
- **Rama's own flagship build has this shape.** Its API servers "are stateless
  and do depot appends, PState reads, and query topology invokes", and run as
  "a separate cluster that aren't colocated with Rama" (Marz, 21 August 2023).
  Its sessions are kept in Rama, in a depot and a table of their own
  (`*authCodeDepot`, `$$authCodeToAccountId`), and looked up on each request
  "in case it has been revoked" (read in `twitter-scale-mastodon` on GitHub).

## The systems that connect, and what each needs

| The system | What it sends in | What it wants back | What is particular to it |
|---|---|---|---|
| A browser tab, running an Electric program | One act for each thing a person does | The present state of what is on screen, kept live | One long socket. It is in the person's hands, so nothing it says of itself is trusted |
| A CLI, one command | One act, or one query | One answer, then it exits | It must start fast: no JVM. A request and an answer |
| A CLI that stays open | Many acts in order (an import), or one query to watch | A receipt for each act; each change of what it watches | It must go on after a break without doing an act twice |
| An agent's harness through MCP (Claude Code, Codex) | A tool call: an act or a query | A tool result | A request and an answer, in JSON. One run of the agent is one session |
| A converter or a watcher (files into acts) | A long line of acts | A receipt for each | Many acts a second into one layer |
| A program on Sid's machine: an agent, a script | Acts; queries to watch | Changes, to act on them | It runs as a user of the machine, today as Sid himself |
| A service that calls in (a webhook) | An act, with a signature of its own | A yes | No session; its proof is in the request |
| Another copy of the server | Nothing | To learn at once that a session ended | It shares nothing with its sibling but Rama |
| What ends TLS in front (nginx) | Every request | Every answer | It must pass a stream, and say the true origin |
| Another deployment | Nothing yet | A published line of acts | "In future" |

The tools that run the store (a deploy, the proofs, a restore) do not connect
to the server. They reach Rama straight, as today.

Seven needs come out of the table.

1. Prove who it is. For a request, with each request; for a socket, once.
2. Send one act and get its answer. Sending it again does it once.
3. Send many acts in order without a round trip for each.
4. Read once.
5. Keep a read live: each change of the present state, pushed.
6. Follow every act of a layer, in order, missing none.
7. Send signals that should not be kept for ever: a cursor, who is present,
   the words of a model as they come, a visitor's trail, "waiting for her yes".

Needs 1 to 5 are served by what follows. Needs 6 and 7 are calls 5 and 6
below: the first writing left them out, and Sid's line of 19 November speaks
against that: "what we do now is full spectrum and is not something that will
be restricting when i want to do things later on" (‹a892cb43›).

## The architecture

```text
  BROWSER TAB            CLI, CONVERTER, MCP ADAPTER  PROGRAM ON THIS MACHINE
  Electric, both halves  one word = one request       a Unix socket: the OS
  a cookie; Origin       or one stream; the token     says which user is at
  checked                with each request            the other end (call 4)
        |                          |                           |
        | no words                 +----------- faces ---------+
        v                                        v
+------------- THE SEAM: Clojure and Missionary, no Electric --------------+
|                                                                          |
| actor    a credential -> her actor, as a flow that ends with her session |
| tasks    ask for act-ids | send -> a receipt {act-id, digest}            |
|          | ask for an answer | read                                      |
| flow     watch -> changes by place, with stamps, merged for a slow reader|
| duties   a bound on a frame; a ration of acts and of live reads;         |
|          Rama's rule that a callback returns at once                     |
|                                                                          |
|          the door, the read path  (built)                                |
+------------------------------------+-------------------------------------+
                                     v
        Rama's client. No other program connects to Rama.
+---------------------------------- RAMA ----------------------------------+
| the store: depots --> gates --> tables                          (built)  |
| a module of the server's own (new; call 1):                              |
|    token's hash -> actor, on the hash's own task                         |
|    "sessions ended lately": one push for each copy of the server         |
+--------------------------------------------------------------------------+
```

Three parts: the faces, the seam, and Rama.

### The seam

The seam is the one place that every face stands on. It is Clojure and
Missionary, and it requires no Electric. Each line is a session's choice; the
review of 20 November is where most of them come from.

- **`actor`: a credential to a flow of her actor.** The only place where a who
  is made. The flow holds her id and her session while the session lives, and
  ends when it ends. It is built from one point read (the token's hash to her
  actor) and one shared signal, "sessions ended lately", which each copy of
  the server holds once. So "a session that ends, ends at once" is said in one
  place, and every face gets it. A visitor is an actor with no id.
- **Tasks: `act-ids`, `send`, `answer`, `read`.** These are the door's own
  calls and the read path's `read-once`, with the actor in front. `send` gives
  the door's receipt, `{:act-id :digest :ack}`, at once; `answer` reads the
  verdict with the act-id and the digest. The seam does not hide the act-id.
  Fact: an act is done once only when it is sent again under the same act-id,
  and its answer is read with the act-id and the digest
  (`src-store/softland/store/door.clj`, `send-act` and `answer-of`). The first
  writing hid both, and so could not keep its own promise of need 2.
- **One flow: `watch`.** A query to a flow that gives the whole answer and
  then changes by place. Fact: that is the form in which the read path tells a
  live reader, `{:anew answer}` then `{:changed {place place-answer}}`
  (`src-store/softland/store/read_path.clj`). A slow reader is not given "the
  newest and none between": a change that is dropped leaves that place wrong
  on her side. So the seam merges by place for a slow reader: the newest
  change of each place is kept, and a whole answer anew takes the place of all
  that waits. Letting the flow go leaves the live read.
- **Duties.** The server is where a stranger first meets a log that is never
  trimmed, so the seam is no pass-through. It owns:
  - a bound on a frame, tested before the frame is read;
  - a ration of acts and of live reads for each actor, and a block;
  - the keeping of a model's reads (Sid's decision of 12 October, entry 4):
    the seam is where "this asker is a model" is first known;
  - Rama's rule that a callback returns at once. A function that the read path
    calls runs on a thread of Rama's client, and one that holds it stalls the
    reads of others (`src-store/softland/store/read_path/README.md`, "The
    asker's function runs under the read's turn"). The seam takes that rule on
    itself, so that no face has to know it.

  How much of this the first build holds is call 8.

**Why tasks and one flow, and not flows for all.** A click, a CLI command and
a tool call are one act and one answer. Only an import is a line of acts, and
order belongs to one actor's acts in one layer, since the store orders nothing
else. The first writing made `acts` a flow to a flow; that tied together
sending, answering, order and holding a sender back, which are four things.

### The words, for a system outside our build

A system that we do not compile speaks a few words of data. A session's
choice; the names are a session's too.

```text
[:who-am-i]                       ->  her id, or "a visitor"
[:act-ids  layer n]               ->  n act-ids of the layer
[:send     act-id act]            ->  a receipt {:act-id :digest :ack}
[:answer   act-id digest wait]    ->  taken | refused and why | nothing yet
                                      | not mine, with the stamp
[:read     query]                 ->  an answer
[:watch    query]                 ->  a stream: the whole answer, then
                                      changes by place, each with its stamp
```

- **One word is one request, or one stream.** The transport is HTTP: a request
  and an answer for the first five words, and an answer that stays open for
  `:watch`. The credential comes with each request. This is derived from the
  table, not taken from habit: a CLI command and a tool call are requests; a
  stream's end is the watch's end, and HTTP gives the leaving and the holding
  back; with the credential on each request any copy of the server can answer;
  and what ends TLS in front already speaks it. One socket that carries
  several streams would need an id for each, a way to leave one, and a hold
  for each. The first writing chose that socket and did not see those three.
- **The words are data, and they add no form to the store's.** An act is the
  store's act (call 2 says in which form), a query the store's query, an
  answer the store's answer. There is no route for each feature: a new thing
  that a system can do is a new key in a layer.
- **The words are declared as the act is.** Their shapes and rules are data in
  the language of `src-store/softland/store/act.clj`, so that one engine checks
  a word as it checks an act.
- **The values are EDN.** An act's values are keywords, UUIDs, sets and
  numbers of any width (`src-store/MODEL.md`, "The words"). A JSON client, MCP
  among them, carries the EDN as text.
- **Everything that the wire carries keeps its stamp.** A stamp is the count
  of a layer's decisions, with no gaps. It is the one cursor that a follower
  will need (call 6).

The MCP adapter is not in the server. It is a small program that the harness
starts; its tools are the words, and it holds the agent's credential.

### Electric: how far

Sid's premise holds. Electric's tutorial: a Clojure expression in an Electric
body "will evaluate to the same result, and produce the same side effects, just
not in the same statement order due to Electric's reactive evaluation model".
So the line is not "plain Clojure against Electric". It is state against
events. Where the newest value is what matters, the code goes up into
Electric. Where each one counts, it stays under.

What is Electric:

- **The browser's whole program, both halves.** Its server half calls the seam
  with no words between. When the socket opens, a function on the server reads
  the cookie, makes the actor flow, and starts her program with it. Who she
  is, what she watches and what she sees are values over time, and Electric
  owns them. A session that ends ends the actor, and all that hangs on it
  unmounts: no rule of ours closes a connection.
- **An act from the browser** is one `e/Token` for each thing she does, an
  act-id made first, a send as a task, and the token spent with the answer.
  Fact, read in the JAR by the reviewer: Electric starts a new server program
  at each socket open, so an effect whose branch mounts again runs again. The
  act-id made first is what makes that a resend and not a second act.

What is not Electric, and why:

- **The seam and what is under it.** An `e/defn` runs only inside a running
  Electric program; a Clojure function or a flow is called by Electric and by
  everything else. So a leaf written in Electric gains nothing and loses every
  other caller. It also keeps Electric's licence, and the churn of an alpha,
  at the browser's edge.
- **Acts in bulk.** A token while busy is "ignoring and silently discarding
  all subsequent events" (the tutorial). That fits a button and not an import.
- **Steps that must run in order.** The first sign-in is: an act, its answer,
  the next act. That is one task of Missionary, under Electric.
- **What sockets share.** One Electric program is one connection. One push for
  all who watch one row is the read path's, today.
- **A system outside our build.** Electric is one compiled program on two
  machines that we both build, and its adapter refuses a client of another
  version (read in the JAR by the reviewer). Whatever outlives a deploy, or is
  not ours, speaks the words.
- **A program's request.** With one word as one stream, the stream's life is
  the watch's life and Missionary cancels it. Electric would have nothing to
  manage there. `boot-single`, Electric's one-peer boot, is in its source but
  names two functions that no file of the JAR defines (checked in the -44 JAR
  by this session, in -45 by the reviewer).

Three rules for the Electric program, each a session's:

- **The actor is a value of the server that cannot cross the wire.** In one
  Electric program the trust line is every server expression that takes a
  value from the browser. A who that the browser can send back is one mistake
  away from being believed.
- **No state that sockets share lives in an atom of the server.** Electric
  makes that easy, and it is state outside Rama that splits when there are two
  copies. Such state goes the road of call 5.
- **The socket's upgrade checks `Origin`.** A browser adds its cookie to any
  socket that a page opens. Electric's adapter does not check it (read in the
  JAR by the reviewer).

How far Electric reaches into the read path is call 7.

### Where state lives

- **In Rama:** a person, the link from her Google account to her id, her
  layer, every act. A session's record too; in which module is call 1. The log
  is never trimmed and every program inside the network can read it, so a
  credential is kept as a hash and never as itself.
- **In the server's memory, lost at a restart with no loss of truth:** open
  sockets and streams, live reads, the handles of the door and the read path,
  and the running Electric program of each browser socket.
- **Not yet placed:** a sign-in that is under way (a CLI shows a link, and
  waits for her yes in a browser). With one copy of the server it can wait in
  memory; with two it cannot. It is a signal of need 7 (call 5).

## The calls that are open

Each is Sid's, or goes to him with a session's lean. A final session should
think each one through again and not take the lean as given.

### 1. Where a session's record lives

- **A. In a module of the server's own, in Rama.** A depot of session events
  and a table, keyed and partitioned by the token's hash; and a small feed of
  "sessions ended lately" that each copy of the server holds one push on.
  - It is what Rama's partitioning is for: a lookup by a credential goes to
    the hash's own task, as one point read, and the token is opaque.
  - It costs a second module, and a session is then no fact that a tool can
    read through the read path.
- **B. As facts in the store,** in a layer for each identity. The credential
  names where its record is, and a live read on the record ends the session.
  - It keeps one model: all that sign-in gives is acts. Sid, 15 October, as
    the revert of that build quotes him: "sign-in gives us a bunch of data and
    we just store that data in rama".
  - It freezes the store's layout into a secret that lies in a file for
    months. And live reads are counted. Derived by the reviewer and not run: a
    live read of one place in a members layer holds up to three pushes, so
    about 330 identities with a connection open use one task's default of
    1,000 before they look at anything.
- **The lean: A,** for the session and the credential only. The person, her
  link and her layer are facts in the store on both sides.

### 2. The form of an act on the wire

- **A. The act less `:who` and `:session`.** The server adds both. A system
  cannot claim another's name, because the form has no place for one. The
  store catches what is left: an act names its permission, a permission names
  its holder, and the gate refuses an act whose who is not that holder
  (`:who-not-the-permissions-holder`).
  - It is a second form beside the store's act, which touches Sid's decisions
    of 6 October: "The one form is the maker's act" (entry 2) and "`:version`
    and `:session` required" (entry 4).
- **B. The whole act on the wire.** The server refuses one whose who or
  session is not the caller's.
  - It keeps one form. It makes an error that side A has no room for.
- **The lean: A.** The reviewer held it right, and said that it is Sid's.

### 3. What a session is, so that a resend works

Fact: the digest covers the whole thing but its time, so `:who` and `:session`
are in it (`src-store/softland/store/door/digest.clj`). If the server writes a
new `:session` after a break, the same act under the same act-id reads back
`:not-mine`.

- **A. A session is the life of one credential:** a sitting for a person, a
  run for an agent. It outlives a connection, and a resend carries it again.
- **B. The digest leaves `:session` out,** as it leaves the time out. That is
  a rule of the store.
- **The lean: A.** It needs no change to the store. What a session's record
  holds is on the page of what sign-in writes.

### 4. A program on Sid's machine, and an agent's who

Fact, derived by the reviewer and not run: an agent that runs as Sid's user
can open Rama's port and can read the CLI's token file. A rule of the network
cannot part two processes of one user on one machine. Sid, 18 November
(‹35df5cbe›): "for now we might run this as a production on my macbook air or
on start of this computer".

- **A. Agents run as another user of the machine,** and Rama's ports are
  closed to that user. A program on the machine then reaches the server on a
  Unix socket, and the operating system says which user is at the other end:
  no token in a file, and none in a model's context. Not checked: that the JDK
  gives the peer's user on a Unix socket.
- **B. Agents run as Sid, as today.** Every agent's who is then a label and no
  boundary, and the pages say so plainly.
- **The lean: B for a store that is thrown away, A before a store is kept.**
  Either way an agent has a who of its own beneath its person from its first
  act, because a who in the log is for ever. The gate takes a permission given
  beneath one's own today. It cannot end a right or limit one to some keys,
  and any holder who may act can open a layer to everyone.

### 5. Signals that should not be kept for ever

- **A. A second depot in Rama with a keeping rule.** Rama can trim a depot by
  an option (Rama docs, "Depots"); the acts' depots stay untrimmed.
- **B. A relay in the server's memory.** It is state outside Rama, and it
  splits between two copies.
- **The lean: A,** by Sid's line: "we should not be needeing something outside
  of rama" (‹2c90529b›). It is a new rule of the store's ground, so it is his.
  The first build has one copy of the server and needs neither.

### 6. Following every act of a layer

- **A. One table in the store, a layer's acts by stamp, and one more word of
  the query.** It is read through the read path, with its check of who may
  see. No new word of the wire.
- **B. The server reads the depot by offset** (`foreign-depot-read`, Rama
  docs, "Defining modules"). No new table. A depot holds things as they
  arrived, and no row keeps where an act is; a stamp is the gate's, not the
  depot's.
- **C. An agent watches state and not events.** Its work is "what points at
  open" under a marked key, kept live. That is built today, and a restart
  loses nothing. It does not serve a copy to another deployment or an audit
  feed.
- **The lean: C now; A when a copy or a feed is built.** The wire keeps the
  stamp from the start, so that side A needs no change to it.

### 7. How far Electric reaches into the read path

Fact: the read path is 3,411 lines, and much of its live half is a reactive
graph built by hand. Its page: "when the setting goes to 'members' under an
open read, the push on her permission is opened then".

- **A. Keep the read path as built.** Electric calls the seam's `watch`. Its
  care for round trips, its shared pushes and its hundreds of checks stay.
- **B. Write the live half again in Electric over raw pushes.** It may be far
  smaller. It would have to earn back the round trips and the sharing, and
  its proof would be a running program and not pure functions.
- **The lean: A for the build.** One slice would settle B: one place read live
  through two layers while a permission changes, in Electric over raw pushes,
  set beside the read path's answer and its count of pushes.

### 8. How much of the seam's duties the first build holds

- **A. The bound on a frame and the thread rule,** with the place of each
  other duty named in the code.
- **B. All of them:** the rations, the block and the keeping of a model's
  reads.
- **The lean: A,** on a store that is thrown away with Sid alone. The build of
  15 October was taken off for building limits and blocks that he had not
  asked for. The store's page says when the rest is due: "before a stranger can
  reach the door".

### Open from before, and not forks of this page

- **What sign-in writes:** which acts, into which layers, by which who. It is
  the real design of sign-in and a page of its own.
- **The system's id,** who may act as the system, and how Sid comes to hold
  rights in the system's layers.
- **The network rule's setting.** Rama's REST port must be closed by it too: a
  path there can name any Clojure function on a worker (Rama docs, "REST API",
  read by the reviewer).
- **Electric's licence.** `THIRD-PARTY.md` records Hyperfiddle's terms: "free
  for bootstrappers and non-commercial use, but is otherwise a commercial
  project".

## Streaming and reactive work: what it carries, and where it stops

What it carries: each change of the present state, pushed, to any system;
one act or a line of acts with a receipt for each; a resend that does an act
once; a session that ends at once on every face.

| # | Where it stops | Where that comes from | What follows |
|---|---|---|---|
| 1 | A live read tells the present. Two acts on one place can come as one change | The read path (`src-store/MODEL.md`, step 10) | Call 6 |
| 2 | Every write is an act that is kept for ever | Sid's decision that a depot is never trimmed | Call 5 |
| 3 | A layer is one line on one task; the base decides in batches | The store's module | One busy layer bounds an import. In the base the answer comes after its batch, not with the receipt |
| 4 | Live reads are counted: 1,000 on one table on one task by default | Rama's option `pstate.proxy.limit` (`src-store/softland/store/read_path.clj`). What it counts is open in `src-store/OPEN-QUESTIONS-rama.md` | A ration of live reads for each actor; one push shared only inside one copy of the server |
| 5 | Electric gives the newest value, and holds a running program for each socket | Electric's design | Memory for each visitor. A tab must load again after a deploy |
| 6 | MCP is a call and an answer | The protocol, from memory and not looked up | An agent's harness asks again, or is told "changed" and reads again |

## The details that are still to fill in

None of these is a call of weight. Each is a session's to settle in the build,
and to tell Sid.

- **The words:** their exact shapes in `act.clj`'s language; the server's own
  refusal words; whether the door's and the read path's words pass through as
  they are.
- **The act-id:** who makes its uuid, the door or the sender; and a word that
  carries a series of acts in one request, for need 3.
- **The stream:** its form on HTTP, how it is left, and what is sent when
  nothing changes.
- **The merge by place,** as code, and the form in which Electric joins a
  watch: a table by place, or the whole answer. The read path keeps no value
  between two changes, and the root page stops any reach for a cache: the form
  must not turn the seam into a kept copy.
- **The credential:** how long, which hash, where a CLI keeps it, the cookie's
  flags, the list of origins.
- **Google:** the client id, which Sid makes; the check of the ID token (its
  signature, `aud`, `iss`, `exp`); the account's number kept as given, by his
  answers of 15 October, whose page came off with that build.
- **The sign-in under way:** the link that a CLI shows, her yes, how long it
  waits.
- **The numbers:** the bound on a frame, the rations, the waits.
- **The CLI and the MCP adapter:** their language (no JVM start), the tools
  and their descriptions.
- **The tree:** a tree of its own with its own classpath and its pages by
  level, as `src-store/CLAUDE.md` asks. It requires the door and the read
  path, and nothing of the rig, the old server or Inland.
- **The proofs:** on Rama's in-process cluster, with a stand-in for Google.
  And checks that read the source: one function calls the door; nothing
  outside the read path opens a table; the seam's classpath holds no Electric.
- **The server's own module,** if call 1 goes to A: by the phases of the Rama
  skill.

## The order of a first build, if Sid says go

1. Sid rules on calls 1, 2 and 3: they shape the wire and what is kept.
2. The page of what sign-in writes, for his redline.
3. The seam, proven on a store that is thrown away.
4. The words over HTTP, and a CLI that signs in, sends an act and watches a
   query.
5. The MCP adapter.
6. The browser's Electric program.

Before a store is kept: the network rule, agents as another user (call 4), and
the calls that `first-act-2026-11-18/PROGRESS.md` lists.

## Where each choice came from

Sid, 20 November (‹354c5f08›): "How are you making the design choices? Where
is the taste coming from?"

| The choice | Where it came from |
|---|---|
| A program in front; the check before the depot; who asks known before the tables | Forced by Rama and by the store |
| The server writes the who; no key in the store; only Sid's programs reach Rama; reads in the runtime with Rama's push | Sid's decisions |
| The server keeps no truth; a session in a depot and a table of its own, looked up when used | Rama's own build |
| A cookie for the browser | Derived: a browser adds nothing else to a socket's upgrade |
| A token with each request; a link that she approves in her browser; an adapter for MCP | The usual practice of the web. The review derived the first; the other two are not derived |
| A hash and not the token; an agent's own who | The build of 15 October had both, and it was taken off. They are named here as choices again, not carried over |
| Tasks and one flow; the receipt on the wire; the merge by place; one word as one request or one stream; the seam's duties; the server's own module; a Unix socket for a program on the machine | The review of 20 November |
| State up into Electric, events under it; Electric as the browser's whole program | Sid's push of 20 November, and the review |

## What the review found, and where it is on this page

A fresh Fable agent at max effort, 20 November. Its verdict: the skeleton
stands, and the contract of the first writing did not.

| # | The finding | Where it went |
|---|---|---|
| 1 | The four words hid the act-id and the digest, so a resend could not do an act once | The seam's tasks; the words; call 3 |
| 2 | `watch` as "the newest, and none between" loses changes by place, or stalls Rama's thread | The seam's flow and its thread rule |
| 3 | A session as facts in a layer was patched twice | Call 1 |
| 4 | "No place to write a who" holds at one wire only; on one machine an agent's who is a label | Call 4 |
| 5 | The server is no pass-through: it is where a stranger meets the log | The seam's duties; call 8 |
| 6 | One socket and flows for everything were not derived from the table | The words; the seam |
| 7 | Needs 6 and 7 cannot be left out | Calls 5 and 6 |
| 8 | Smaller: `Origin`; the wire act is not the store's act; three systems missed; the words declared as data; Rama's REST port; who first made three of the choices | Electric's rules; call 2; the table of systems; the words; "Open from before"; the table above |

The review's report is in the record under the session of 20 November that
wrote this page.

## What was checked, and what was not

- **Read by this session:** the store's pages (`src-store/MODEL.md`, the
  store's README, the decisions of 6 to 12 October), the docstrings of
  `door.clj`, `digest.clj` and `read_path.clj`, the read path's page, the
  pages of `first-act-2026-11-18/` and `sign-in-2026-10-15/`, both sessions'
  texts of 19 November in the record, Rama's docs and release notes in
  `/mnt/data/projects/research/softland/rama-reference/`, the Rama research of
  29 September in `/mnt/data/projects/research/softland/rama-modules-2026-09-29/`,
  three files of `twitter-scale-mastodon` on GitHub, Electric's tutorial and
  `electric3.cljc` in the reference copy, `boot-single` in the -44 JAR, and
  Inland's server file as an example.
- **Checked by this session after the review:** findings 1 and 2 against the
  code, and the claim on `boot-single`.
- **Taken from the review and not checked again:** what Missionary's `observe`
  does to the calling thread; what Electric's adapter does at a socket's open
  and with a client of another version; the count of about 330.
- **Not run:** anything. No line of this design has met a cluster.
- **From memory and not looked up:** what MCP gives for notifications; that
  the JDK names the peer's user on a Unix socket.
