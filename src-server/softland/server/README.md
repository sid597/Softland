# Server — the parts of the server

[Parent: softland](../README.md) · [Server source entry](../../README.md).

The server is one program between every system that connects and Rama. This
folder holds all of it but its faces for a browser and for a command line,
which are trees of their own.

## The parts

| File or folder | Responsibility | State and lifetime |
|---|---|---|
| [session/](session/README.md) | The pure rules of a session: its row and events, the feed of what changed lately, a credential. | None. |
| [module.clj](module.clj) | The Rama module `Sessions`: the depot of session events, the row of each credential under its hash, and the feed. | The depot and two PStates, durable in the cluster. |
| [sessions.clj](sessions.clj) | The server's one client of its own module: it begins and ends a session, reads a row, and tells a listener when a row changed. | In the memory of one process: three handles of Rama's client, one push, the listeners, and what it last saw of the feed. |
| [words.clj](words.clj) | The words that a system outside our build can say, declared as data, with their check; and what the server says when it could not do a thing. Pure. | None. |
| [frame.clj](frame.clj) | One frame of the wire: one EDN form, read and written. Pure. | None. |
| [watching.clj](watching.clj) | What waits for a reader who is slower than the changes, merged by place; and an answer after a change. Pure. | None. |
| [seam.clj](seam.clj) | The seam: a credential to an actor, and the actor in front of the door's calls and the read path's, as tasks and flows of Missionary. | In the memory of one process: its numbers, the id of the server's own session, its threads. Nothing between two calls. |
| [sign_in.clj](sign_in.clj) | What sign-in writes into the store, as the system: the person of a Google account, found or registered, and her own lane. | None of its own; it writes acts through the seam. |
| [google.clj](google.clj) | The check of Google's ID token, and the fetch of Google's public keys. | None; the fetch keeps nothing. |
| [google_stand_in.clj](google_stand_in.clj) | A stand-in for Google that signs ID tokens with a key pair of its own, for the checks and for a run on one machine. | A key pair in the memory of the process. |
| [http.clj](http.clj) | The face over HTTP: the words, the stream of a watch, and the pages of sign-in, as a Ring handler. | None. |
| [main.clj](main.clj) | The program: it connects to Rama, opens everything, and serves the face. | What it opened, until the process ends. |

## How the parts relate

```text
a command line, an agent's harness,          a browser tab
a converter: the words over HTTP             (its Electric program is
        |                                     another tree, in front)
        v                                             |
   http.clj  --- frame.clj, words.clj                 |
        |                                             |
        +------------------+--------------------------+
                           v
                       seam.clj   --- watching.clj
                    /      |      \
        sessions.clj   the door   the read path     (src-store)
              |            \         /
              v             v       v
   module.clj `Sessions`    the store's module `Store`
   ------------------------- Rama --------------------------
```

A request arrives at [http.clj](http.clj). It bounds the frame, reads one EDN
form with [frame.clj](frame.clj), checks the word with [words.clj](words.clj),
and hands the credential to [seam.clj](seam.clj), which gives an actor or a
word. Every other call of the seam takes the actor first. The seam adds the
actor's who and session to an act and calls the store's door; it adds the asker
to a query and calls the store's read path. The results go back as they are.

[sign_in.clj](sign_in.clj) is called by a face at a sign-in, after
[google.clj](google.clj) passed the token. It is the one caller of the seam's
`system`, and it writes its acts through the same tasks as everybody.

[main.clj](main.clj) requires everything here. Nothing here requires it, but
the browser's tree, which reuses its start.

## Boundaries

- **One function calls the door's `send-act`:** `send-now` of
  [seam.clj](seam.clj). It writes the actor's who. No other code of the server
  can put an act on a depot.
- **The server opens no table of the store.** What it reads of the store, it
  reads through the read path. [sessions.clj](sessions.clj) is the one file
  that calls Rama's client, on the server's own module.
- **Nothing here requires Electric.** A task or a flow is called by an Electric
  program and by everything else.
- **An actor is no data.** Only [seam.clj](seam.clj) makes one, so a who that a
  system sends cannot become one.
- **A word takes its token from a header, and a page from a cookie.** A post
  that rests on the cookie must carry this server's own `Origin`.
- **Only a credential that stands for somebody ends a session.** A text that
  has the form of a token ends nothing and writes nothing.

The pure proof reads the source for the first three and for the caller of
`system` ([the verification map](../../../test-server/README.md)). A face
cannot make the system's actor; it reaches the system's steps by calling the
two functions of [sign_in.clj](sign_in.clj), each of which does one thing: the
person of a checked token, and where her own lane is.

The server is not a check of an act, which is a gate's; not a guard of what a
reader may see, which is the read path's; and not the network rule that keeps
other programs from Rama's ports, which nothing sets yet.

## Decisions this build stands on

Sid's decisions are in [the store source entry](../../../src-store/README.md)
and in [the first-act page](../../../src/proposal/first-act-2026-11-18/PROGRESS.md),
with his words.

| Decision | Where it shows in the server |
|---|---|
| A person signs in with Google at the server, which finds her id from Google's account id or registers her, and writes that id as the who of each act that she makes. Nothing in the store holds a key (his go of 19 November 2026) | [sign_in.clj](sign_in.clj); `as-maker` in [seam.clj](seam.clj) |
| Only programs on machines that Sid runs may reach Rama's ports, and a program that is not trusted that far talks to the runtime (11 and 12 October, entry 4) | The server is that runtime's first form. It sets no rule of the network |
| A read is done in the runtime, straight from the tables, with Rama's push (the same section, entry 3) | `read` and `watch` in [seam.clj](seam.clj) call the read path and keep no value |
| A person's id is a UUID that says nothing, and her accounts are joined to it by links (9 October, entry 1) | [sign_in.clj](sign_in.clj): a random id, and Google's number as a fact on it |
| A signed-in user has a personal lane and a permission in it, without asking (8 October, entry 2) | The making act of her own lane, in [sign_in.clj](sign_in.clj) |
| An act from a maker who may not act is written to the log, and then refused (12 October, entry 7) | The server judges nothing of an act's form or standing: a wrong act goes to the door, and a gate refuses it in the record. What it turns away itself, with nothing written: a thing that is no plain map, and an act that already holds `:who` or `:session`, as the door turns away an act that holds one of the door's own parts |
| Google's sign-in button, with no client secret; Google's number kept as it is (15 October, ‹bb59807f› and ‹5325b353›) | [google.clj](google.clj) checks an ID token; [sign_in.clj](sign_in.clj) keeps `sub` as given |

## Engineering choices

Each is the builder's, with its reason. None is a decision of Sid's, and each
is provisional. The first three are the proposal's calls 1 to 3, which are his
to turn; [the build's page](../../../src/proposal/server-2026-11-20/BUILD.md)
puts each to him with its other side.

**A session's record is the server's own, in a module of its own.** A row is
found by the hash of a secret, which no reader of the store may be served, and
a sign-in would otherwise need a who before its session exists. The store's
module keeps its two depots of acts and no other.

**A system sends an act without `:who` and `:session`, and a query without its
asker.** The server writes them. A system then has no place in which to claim
a name, and a face that forgot a comparison cannot let one through, since
there is nothing to compare.

**A session is the life of one credential.** It outlives a connection, so an
act sent again after a break has the same digest and reads its one answer.

**Tasks, and two flows.** A click, a command and a tool call are one act and
one answer, so the door's calls and the read path's `read-once` are tasks. A
live read and a credential's standing are values over time, so they are flows.

**A word is a map.** The act and the query are maps of named parts that one
engine reads, and the arguments of a tool call and of a command line are named
too. The proposal wrote the words as vectors.

**A sign-in that is under way is kept nowhere.** The program makes its token
and shows a link with the token's hash; her yes is the first event. So no
stranger writes to a log by asking to be signed in, and nothing has to agree
between two copies of the server. What it gives up is under "Not decided, and
not built": the server cannot make a link grow old, and whoever says yes on a
link is who the program then acts as.

**The credential is looked at before the frame is read.** Reading a frame can
cost far more than its bytes say, so a request whose credential stands for
nobody is held to a small frame, and every frame to a longest token
([frame.clj](frame.clj)). The larger bound, for somebody signed in, is the
store's own bound on one act and a little more: a thing that can only be
refused does not reach a log that is never trimmed.

**Where a browser goes on to after a sign-in is built, not followed.** The
text that a request gives for it is read for one of three places and written
anew ([http.clj](http.clj), `way-on`).

**One push for a whole copy of the server.** Each copy holds one push on the
feed of the sessions that changed, however many sockets and streams it holds.
A push for each open thing would be counted against Rama's bound on the pushes
of one table on one task.

**Virtual threads.** Every call of the store, and every handing on of a flow,
runs on a virtual thread of the seam's, by Missionary's `via`; Jetty gives each
request one. A wait for a person's yes and a stream that stays open for hours
each hold a thread that costs little. A face must not wait inside the function
that takes a thing of a flow ([seam.clj](seam.clj), "One limit on a face").

**No copy of a row between two requests.** A request's credential is read from
Rama at each request: one round trip, on the hash's task. A copy kept in the
process would be a cache.

**The check of an ID token is written here**, with the JDK's own signature and
no library for tokens: it is one function of seven tests in a fixed order, and
the pure proof holds each test against a token that is wrong in that part. A
library for tokens is the other answer.

## Not decided, and not built

| The thing | Where it stands |
|---|---|
| What sign-in writes | Built to the least form, on [its page](../../../src/proposal/server-2026-11-20/SIGN-IN-WRITES.md) for Sid's redline |
| Who may become a person | Anybody whom Google proves. Due before a stranger can reach the server |
| The keeping of reads | Sid decided whose reads are kept: a model's, always; a person's, until she turns it off (12 October, entry 4). This build keeps none; where a kept read lands is not decided |
| A ration of acts and of live reads, and a block | Named at `ration` in [seam.clj](seam.clj); nothing is built. What that leaves open today, as the review of 20 November 2026 ran it: a visitor with no credential who opens some 900 watches on distinct places of a lane that is open to everyone takes every live read of that lane, its owner's too, since the read path holds 900 pushes on one lane; and anybody signed in can send one frame after another that each cost seconds to read |
| A program's sign-in against somebody who means harm | The page warns her, and nothing else holds: whoever makes a link and gets her yes acts as her for the session's 30 days; whoever sees a program's link first can say yes first, and the program then acts as that person and is not told. She has no page that lists the programs that act as her, and a link does not grow old. Due before a stranger can reach the server |
| The cookie on one host | A browser sends it to every server on the same host, whatever its port: on `localhost`, to each program that listens there |
| The feed's one task | Every beginning and end of a session, of every person, is written on one task and pushed whole to every copy of the server. More feeds, each under a key of its own, is the way on when that task is the limit |
| An agent's own who | Not built: every credential stands for a person. An act's `:session` tells a browser's acts from a program's |
| Who may act as the system | The server's own steps of sign-in, and nothing that a system sends |
| Following every act of a lane; signals that are not kept for ever | No word for either. The proposal's calls 5 and 6 |
| How long the events and the rows of ended sessions are kept | For ever, as built |
| The network rule, and a cluster that keeps its data | Set and run by nothing here |

## Words a session made, Sid's to change

Each says what the thing is. None is from the rig's code, the old server's or
Inland's. "The seam", "a face" and "the words" are the proposal's.

**Namespaces.** `softland.server`, and under it `module`, `sessions`, `words`,
`frame`, `watching`, `seam`, `sign-in`, `google`, `google-stand-in`, `http`,
`main`; `softland.server.session` with `record`, `lately`, `credential`.

**The module.** `Sessions`; `*session-events`, `$$sessions`,
`$$sessions-lately`; the events `:began` and `:ended`; a row's `:state`, with
`:live` and `:ended`, and `:who`, `:session`, `:until`, `:began`, `:ended`.

**A standing.** `:state`, with `:not-known`, `:live`, `:over`, `:ended`,
`:not-reached`, `:not-a-token`.

**The words.** `:who-am-i`, `:act-ids`, `:send`, `:send-all`, `:answer`,
`:read`, `:watch`, `:sign-out`; their parts `:word`, `:lane`, `:count`,
`:act`, `:act-id`, `:acts`, `:digest`, `:wait-ms`, `:query`. The reasons of
their check are in [words.clj](words.clj).

**What the server could not do.** `:read-the-frame`, `:read-the-word`,
`:find-her-session`, `:reach-the-sessions`, `:begin-her-session`, and
`:write-her-sign-in` of [sign_in.clj](sign_in.clj); with `:why` and `:at`.

**What sign-in writes.** The lane `:people`; the keys `:account/google`,
`:account/google-says`, `:person/own-lane`.

**A token.** `sl1_`; the cookie `softland`.
