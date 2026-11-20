# The server: what was built, and each fork that the build took

**20 November 2026.** Written by the session that built it (Fable 5.2), for
Sid, who was away while it was built. It began from
[PROPOSAL.md](PROPOSAL.md). Nothing here is a decision of his unless it cites
one. A session does not judge its own build: a fresh agent reviewed the
server's first three commits with the job of breaking them, and what it found
is below.

## What he asked, and what the page said

His words, typed on 20 November (‹aea9400a›): "Attached is the proposal for the
server that stands in front of the store: sign-in, and how a browser, a CLI and
MCP connect to it. I want your view of it before we build." And: "Once you have
run your side then you should go and implement it please i will be afk for a
few hours and the job is on you to implement it. Go".

The proposal's own order wanted two things from him before any code: his
rulings on calls 1 to 3, and his redline on a page of what sign-in writes. He
said go without them. So the build took its own side of each call, wrote the
least form of what sign-in writes, and kept every such choice where one small
change turns it. All of it runs on a store that is thrown away. No "Decisions
of …" section was touched, and no code of the store.

## What exists now

| Part | Where | What it gives |
|---|---|---|
| The server's module | `src-server/softland/server/module.clj`, `session/` | Which credential stands for whom, in Rama: a depot of two events, a row under each credential's hash, and a feed of the rows that changed lately |
| The seam | `src-server/softland/server/seam.clj` | A credential to an actor; the door's calls and the read path's with the actor in front, as tasks and flows; a session that ends, ends every open thing on every copy |
| The words | `words.clj`, `frame.clj`, `http.clj` | Eight words of data over HTTP, one request each, and one stream for a watch |
| Sign-in | `sign_in.clj`, `google.clj`, `http.clj` | Google's ID token checked; the person found or registered; her own layer made; a cookie for a browser; a link and her yes for a program |
| The program | `main.clj` | One command that starts all of it on an in-process cluster |
| The command line and MCP | `src-cli/` | One program for Node: sign in, write, read, watch; and the same program as an MCP server on stdio |
| The browser | `src-server-browser/` | A small Electric program whose server half calls the seam |

How to run it, from the repository root:

```sh
clojure -M:server -m softland.server.main --stand-in-google     # the server, on http://localhost:8400
clojure -M:cli -m shadow.cljs.devtools.cli release softland-cli  # builds the command line, once
bin/softland sign-in                                             # shows a link; say yes in the browser
bin/softland write :chair-7 :colour :green
bin/softland who                                                 # her id and her own layer
```

And with the browser's program:

```sh
clojure -M:server-browser -m shadow.cljs.devtools.cli release softland-browser
clojure -M:server-browser -m softland.browser.main --stand-in-google   # http://localhost:8401, then /app
```

`--stand-in-google` lets anybody who reaches the server sign in as any account.
It is there because the Google client id is yours to make; the proposal named
a stand-in for the proofs only, so as a way to run it is this build's addition.
It does not start together with `--conductor`. With `--google-client-id` the
server shows Google's button and checks Google's tokens; that road was run
against a stand-in's tokens only.

## The forks, each for his word

The first eight are the proposal's calls. "Built as" is which side the code
is on now.

| # | The call | Built as | What turns it |
|---|---|---|---|
| 1 | Where a session's record lives | A module of the server's own. It holds the credential only: its hash, who, the session's id, and until when | `softland.server.module` and its client; the seam reads a standing and nothing else of it |
| 2 | The form of an act on the wire | The act without `:who` and `:session`; the server adds both. The same for a query's asker | `as-maker` and `as-asker` in the seam; `server-parts` in the words |
| 3 | What a session is | The life of one credential | Nothing in the store changes either way |
| 4 | A program on his machine, and an agent's who | Agents run as he does. Every credential stands for a person; an agent's own who is not built | A design of what registers an agent. The proposal's call stays open |
| 5 | Signals that are not kept for ever | Nothing | No part of this build needs one |
| 6 | Following every act of a layer | Nothing new. Every place-answer and every answer carries its stamp | A word, and a table by stamp, when a copy or a feed is built |
| 7 | How far Electric reaches into the read path | The read path as built; Electric calls the seam's `watch` | A slice of the read path's live half in Missionary, set beside it |
| 8 | The seam's duties in the first build | The bounds on a frame and the thread rule. The rest is named at `ration` in the seam | The store's page says when they are due: before a stranger can reach the door |

Forks that the proposal did not name, and the build took:

| # | The fork | Built as | Why, and the other side |
|---|---|---|---|
| 9 | The form of a word | A map with a `:word`, where the proposal wrote a vector | The act and the query are maps of named parts under one engine, and a tool call's arguments are named. Vectors are shorter to type |
| 10 | A sign-in that is under way | Kept nowhere. The program makes its token and shows a link with the token's hash; her yes is the first event | The proposal placed it as a signal, and my first answer to you placed it in Rama. Nothing has to be written before her yes, so no stranger writes to a log by asking. What it gives up, which the review ran: the server cannot make a link grow old; whoever says yes first on a link is who the program acts as. A record made by the server, with an end, is the other side |
| 11 | Two more words | `:send-all`, for acts in order with no round trip between them (need 3); `:sign-out`, for a program that ends its own session | The proposal listed the first as a detail to fill in. The second has no other road for a program |
| 12 | How she learns where her own layer is | `:who-am-i` gives it; the server reads it as the system | See [SIGN-IN-WRITES.md](SIGN-IN-WRITES.md), row 5 |
| 13 | Her no | A hash that stands for nobody is refused for good; one that stands for her is ended; one that stands for another person is left alone | A hash is shown on a link, so knowing one must not end another person's session |
| 14 | An act that names a who | Turned away by the server with nothing written, as the door turns away an act that holds one of the door's own parts | Your line of 12 October on a maker who may not act: "why don't we store both? Like the data is useful." The other side: write the actor's who over it and send it on, so that the try is in the log |
| 15 | The check of Google's token | Written here with the JDK's signature, seven tests in a fixed order | A library for tokens. The build of 15 October used one. The review tried to break the check and could not |
| 16 | The language of the command line | ClojureScript on Node, since Node is on this machine and babashka is not. A JavaScript number cannot hold the store's integers, so every value passes through as the text that was typed | Babashka: Clojure that starts fast and holds the store's numbers. It would have to be installed |
| 17 | The numbers | A session lasts 30 days. A frame of somebody signed in is the store's bound on one act and one MiB; of anybody else, 64 KiB; no token of a frame is longer than the store's bound on one value's text. A wait is 60 s at most; a quiet stream gets an empty line each 15 s; the feed holds 32 changes | Each is one `def`, marked kept and not decided |
| 18 | The trees and their words | `src-server`, `src-cli`, `src-server-browser`; "the server" | "The runtime" is the word of your decisions of October |

## What was run

On Rama's in-process cluster, one machine, Rama 1.6.0. Nothing met a cluster
that keeps its data, and nothing met Google.

| Proof | Command | Result |
|---|---|---|
| Pure parts and the source | `clojure -M:server -m softland.server.pure-proof` | 68 passed, 0 failed |
| The seam, two copies over one store | `clojure -M:server -m softland.server.seam-proof` | 43 passed, 0 failed |
| The face over HTTP | `clojure -M:server -m softland.server.http-proof` | 37 passed, 0 failed |
| The browser's face, headless Chrome | `node test-server-browser/drive.cjs` against a running server | Every step passed: a sign-in through the real form, her yes on the real page, an act accepted, the live read showing it and changing with a second act with no reload, the open page turning at a sign-out |
| The socket's origin | `node test-server-browser/origin.cjs` | A socket from another origin is closed with 1008 |
| The command line and MCP | `bin/softland` against a running server, by hand; `test-cli/sign-in-with-stand-in.sh` signs it in with no person | A sign-in with her yes, a write and its read, an integer of 30 digits read back digit for digit, a watch that showed a change as it was written, the refusals, the MCP round, sign-out. No proof holds these; no harness of an agent was run |

What a pass shows, and what no check reaches, is in
`test-server/softland/server/README.md`. The store's own proofs were not run
again: no file of the store changed.

## What the review found

A fresh Fable agent, on servers of its own, with the job of breaking the first
three commits. It ran most of what follows. "Fixed" is in the commit
`3cbec56e`.

| # | What it found | What was done |
|---|---|---|
| 1 | One request with no credential held a thread for 22 seconds, and at the frame's bound would have for hours: a long run of digits is read as one number | Fixed: the credential before the frame, a small frame for nobody, a longest token for every frame. The worst frame of somebody signed in still takes 11.8 seconds to read; a ration stops that, and none is built |
| 2 | A link that anybody makes, with a text of her choosing on the server's own page, gives her a session as whoever says yes | The page now leads with the warning and marks the text as the program's own words. Nothing else holds. Before a stranger can reach the server |
| 3 | A visitor who opens some 900 watches on a layer that is open to everyone leaves no live read of it for anybody, its owner too | Not changed: it is the ration of live reads. The pages now say it plainly |
| 4 | A tab in `next` sent a signed-in browser to another site | Fixed: where a browser goes on to is built, never followed |
| 5 | Whoever says yes first on a program's link owns that program, and the program is not told | Not changed. Fork 10 |
| 6 | A real browser's own forms were refused, since the pages' referrer policy made it write `Origin: null` | Fixed before the review ended, when the browser check found it |
| 7 | A sign-out with any text of a token's form wrote a row and a change on the feed, for ever | Fixed: only an actor ends a session |
| 8 | The task that I wrote by hand in place of Missionary's `via` was wrong in its cancelling, and my reason for writing it was false | Taken back: `via` does not hold its carrier. I misread the trace |
| 9 | Google's keys were fetched before a token was looked at | A text that the check turns away unread costs no request now. A well-formed token still costs one fetch each; keeping the keys is a cache, and the root page stops there |
| 10 | The frame's bound of 32 MiB let any account put more into the log than a gate can take | Fixed: the bound is the store's own bound on one act and one MiB |
| 11 | The server turned an act that names a who away before the log, and its page said that it checks nothing | The page says what it turns away. Fork 14 puts the other side to you |
| 12 | The stand-in could be started on a cluster that keeps its data | Fixed: it does not start with `--conductor` |
| 13 | Five sentences said more than the code did | Corrected |
| 14 | Every beginning and end of a session goes through one task; `people` is one layer on one task; a push that Rama ends would have left a copy deaf | The last is fixed: listeners are told and end. The first two are stated on the pages, with the way on |
| 15 | Against your line of 15 October: what Google said is written again at each sign-in, and read back at each `:who-am-i` | The first is row 3 of [SIGN-IN-WRITES.md](SIGN-IN-WRITES.md), yours to turn. The second reads one fact, her own layer's id, and nothing that Google said |
| 16 | Checks that no proof made | Added for 1, 4 and 7; the source check no longer rests on a name |

What it tried to break and could not: the check of an ID token; the parsing of
the header and the cookie; the origin rule; a hash bound once, under replay
and either order; two first sign-ins of one account at once, 20 times of 20; a
watch left behind; the feed under 400 ends at once.

## What a second review found: what the build assumed

Sid asked for it when he came back, with a point of view of his own: what were
the constraints that the model assumed, which show as the boundaries that it
built on, and what would the software be if they were relaxed. A fresh session
that knew nothing of this one made its own list from the code before it read
this page. Its report is whole in
[REVIEW-assumptions.md](REVIEW-assumptions.md). In short:

- **Sixty assumptions.** Six rest on a fact that was checked. Eight are his
  decisions. Eighteen are the proposal's, fourteen this build's and named,
  nine this build's and never named, three are habit. Twenty-four are real
  constraints: relax one and a wall moves.
- **Two shape most of the build.** Every actor is a person, and only the
  server can vouch for her. And the server is a thin layer in one process that
  adds that who to the store's two forms and does nothing else.
- **A third lets those two stand:** that there is a time before strangers, on
  a store that is thrown away, so that guards and attribution can wait. This
  page leans on it throughout. It is a session's phrase of 12 October and no
  line of his; and his agents already act as him on text from anywhere.
- **What it says would come out differently from the relaxed side:** the row
  of a session would bind a credential to an actor with a kind and a chain to
  its person, from the first commit; a read would be an event at the seam; the
  seam's contract would be data, so that where a face runs is no wall; and the
  place for duties would see the request and not only the actor.
- **What it says would come out the same:** the thread rule, the merge by
  place, act-ids asked first with the receipt and its digest, the bounds on a
  frame, the one function that calls the door, a token kept as a hash, the
  cookie and origin rules, and no truth in memory.

What this session ran or corrected after it:

- **A program that holds a token can sign in another program as her:** run,
  and true. The two carriers of a credential keep another site's page out, not
  a program that holds a token.
- **The keeping of reads was misnamed** here and in the seam, after the
  proposal, as "a model's reads" that wait until something says who is a
  model. His decision of 12 October is "a model's, always; a person's, until
  she turns it off": by his default every read of somebody signed in is kept,
  and this build keeps none. The pages now say so. Where a kept read lands is
  not decided, and is his.
- **Sign-in's acts named nobody as their subject.** They do now: each act that
  holds her account's number, what Google said of her, or her own layer's
  first permission carries her id in `:subjects`.
- **An agent's own who.** The review holds call 4, as built, against a long
  prompt of his of 15 October (‹e5a2200e›): "best is to have each agent their
  own identity". The record cannot tell whether he typed or pasted that
  prompt; no session wrote the line before it. This session did not have it
  in front of it when it built call 4 to the other side.

## What the two builders found awkward

Each built against the wire or the seam and said where it did not sit well.
None is changed; each is a thing to weigh when the words are next looked at.

- **A program that waits for her yes is answered 401.** `:who-am-i` with a
  token that stands for nobody yet says `:not-known` with the status of "no
  session", so a program has to read that 401 as "keep waiting".
- **`:ended` says two things:** her no, and a session that ended later.
- **A watch fails in two ways:** as one form with a status, when it could not
  begin, and as a stream whose last line is the end.
- **A gate's refusal is a 200,** so the command line exits 0 on an act that was
  refused, which reads oddly in a script.
- **A session's end is told in `:until`, and nothing renews it.**
- **From Electric's side:** every change of a live read crosses the wire as the
  whole answer in this slice; sending only the changed places needs the change
  to cross as a keyed table. And what a page must keep across a socket that
  opens again has to live outside the Electric program: Electric `-45` makes a
  new client root, and not only a new server program, at each opening.

## Before a stranger can reach it

In the order in which they would hurt.

1. **A ration of acts, of live reads and of what a request costs, and a block.**
   Today a visitor can take every live read of an open layer, and anybody with
   a Google account can append to a log that is never trimmed, one frame after
   another.
2. **A program's sign-in.** A link's end, a page that lists the programs that
   act as her with a way to end each, and what the program is told of whose it
   became.
3. **Who may become a person.** Today anybody whom Google proves.
4. **The keeping of reads** (your decision of 12 October, entry 4: a model's,
   always; a person's, until she turns it off). This build keeps none, and
   your default keeps them all; where a kept read lands is yours to say.
5. **One finding for the store, not the server:** an act that holds an integer
   of 65,536 digits is within the store's bounds, and reading that one number
   takes 56 ms here; a gate takes the digest of the whole thing on its task's
   thread.

## What is still his

- **The Google client id.** Without it nobody signs in but through the
  stand-in. When you make it: the origins to allow are `http://localhost` and
  `http://localhost:8400`, and the address that Google posts to is
  `<origin>/sign-in/google`. Google's setup page names a referrer policy for
  `http` on `localhost`; the server's pages send
  `strict-origin-when-cross-origin`, and `softland.server.http/referrer-policy`
  is the one place if Google's button refuses there. From a search's summary
  of that page, not read in full and not run.
- **[SIGN-IN-WRITES.md](SIGN-IN-WRITES.md):** seven choices, for his redline.
- **Calls 1 to 4 and forks 10 and 14** above, built to one side.
- **Before a store is kept:** the network rule; agents as another user of the
  machine; and the calls of `first-act-2026-11-18/PROGRESS.md`.
- **A `CLAUDE.md` for the new trees,** if he wants one: a page that binds later
  sessions is his to add. The trees keep the store's rules for explanations.
- **Electric's licence,** as before. One thing to know of it: the compiler of
  Electric `-45` asks for a login the first time a build runs on a machine, and
  waits. The builder agent copied the file that the build `:inland` already
  held under `.shadow-cljs/builds/`, which git ignores, into the new build's
  folder, without reading it. Say if you want that undone.

## What changed outside the new trees

`deps.edn`: three aliases, `:server`, `:server-browser` and `:cli`.
`shadow-cljs.edn`: two builds, `:softland-cli` and `:softland-browser`.
`bin/softland`: the wrapper of the command line. Nothing else.
