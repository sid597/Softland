# The server: a review of what the build assumed

**20 November 2026.** Written by a fresh session (Fable 5.2, max effort) that
knew nothing of the building session's conversation. Sid asked for it, with a
point of view that he typed that day and that the reviewer was given in his
words, unchanged:

> What are the constraints that you assumed and thereby reflecting in the the
> boundaries you build on
>
> Contraint programming
>
> We work using the constraints as the driving what and how to build software.
> Design principles are written around what the constrainsts at one point of
> time vs future work, that may or may not happen.
>
> This makes software development for that case we use that software to get to
> new directions and then build the software again for this new one case but
> that means rethinking on the previous decision which we already know are
> based on bad design for this new case.
>
> So instead of building a software around a preset principles and then
> maintaining it and growing it. Is itself constrained by the number of
> requests made to build next version so one way to think whole building is
> from the pov what happens when the constraints are relaxed and what would be
> the software for that version. Constraints means assumptions and i want to
> see how much assumptions a model is making and thereby making the decisions
> around them.

The reviewer made its own list of assumptions from the code and the proposal
before it read [BUILD.md](BUILD.md) and [SIGN-IN-WRITES.md](SIGN-IN-WRITES.md).
Its report follows as it wrote it, addressed to Sid. Nothing in it is a
decision. Two of its claims were run by the building session afterwards, and
that is said at the end.

---

## 1. The count

I found 60 assumptions. Two of them shape most of what was built:
- every actor is a person, and only the server can vouch for her;
- the server is a thin layer, in one process, that adds that who to the store's two forms and does nothing else.

| Where it came from | How many |
|---|---|
| Forced by a fact that was checked | 6 |
| Forced by a fact that nobody checked | 2 |
| Your decision | 8 |
| The proposal's (a session's) | 18 |
| The builder's, named | 14 |
| The builder's, never named | 9 |
| Habit, and no other ground | 3 |

- **Named nowhere: 13.** These are the builder's 9 and 4 of the proposal's.
- **Named as facts, but they are choices: 2.** "A program in front", and "the proof that it is her is a live sign-in".
- **Passed off as your word: 1.** "Before a stranger can reach the door" is carried as the store page's word. The record shows a session wrote it on 12 October. You pasted it on and never typed it.

By my judgment, 24 of the 60 are real constraints: relax one and a wall moves or a part is made again. Three of those rest on checked facts. Another 18 would cost a function or a number. The last 18 change nothing that stands on them.

## 2. The assumptions that shape the most

| # | The assumption | Where it is a wall | Where it came from · named? | If it did not hold |
|---|---|---|---|---|
| 1 | Every credential stands for a person. An agent is its person, with everything she may do, for 30 days. | The session's row `{who session until}` and the actor. The yes page: "A program asks to act as you, with everything that you may do, for 30 days". One `credentials.edn` per server, read by every MCP run. `ration` cannot tell a model. | **From:** the builder (BUILD.md, call 4). It goes against three sources: your long prompt of 15 October (‹e5a2200e›: "best is to have each agent their own identity … An agent has its sessions"); your long prompt of 18 November (‹b6b0f700›: agents with "almost all of the permissions that I have"); and the proposal's own "either way an agent has a who of its own beneath its person from its first act". **Named:** partly. Never named: the departure from those three. That every agent run shares one session, where the proposal's table says "one run of the agent is one session". That a program holding a token can post the yes page itself (the cookie and the `Origin` are headers it writes), so it can sign in more programs as her and renew itself past 30 days. | A credential stands for an actor with a kind and a chain to its person: a sitting, an agent, one run. Her yes becomes an act that gives a permission beneath hers, which the gate already takes. The log then names which agent did what. Made again: the row, the actor, the yes, and the command line's sign-in and file, about 500 lines. It needs the end of a permission, which the store lacks; that rule is yours. The words, the seam's tasks and the store calls stand. On a kept store, acts written meanwhile under her who cannot be moved. |
| 2 | The server is the only witness of who: a Google sign-in once, then a bearer token. The log's `:who` is the server's word. | The module `Sessions`. `credential.clj`. `send-now`, the one caller of the door's `send-act`. A network rule that nothing sets. | **From:** your decision of 19 November, "Nothing in the store holds a key" (‹afbff6f5›). Its grounds were corrected after your go, and the first-act page leaves to you whether that changes it. Six minutes before you asked why there was a key at all, you typed "If i write its signed by me" (‹fa5d7ab8›). **Named:** yes, as yours. But the proposal also calls it a fact: "The proof that it is her is a live sign-in. It cannot be put into a log … facts, not choices." A signature can be put into a log; that is the other side of your open call. | (a) The server signs each act: one step in `send-now`, and a part in the act's form (yours to decide). This wall is in the right place, and little is made again. (b) Each actor holds its own key, agents beneath their person, and another deployment can check a published line. Then who leaves the server, and the module of sessions serves reads only. About 1,300 lines are reshaped or go, a quarter of the build. Either way, acts kept unsigned stay unsigned. |
| 3 | Every face runs inside the one server process. The seam is an in-memory API of Missionary tasks and flows. | `softland.browser.main` requires `softland.server.main`. Electric's server half calls the seam with no words between. "The actor cannot cross the wire" holds because nothing crosses. Softland's own client, if it uses Electric, must be built and deployed with the server. | **From:** the proposal ("one JVM program of ours"), under "What forces a program in front". The facts force a trusted JVM that holds the store's client. They do not force one process for every face. You chose "runtime" because "the word stays neutral about form" (vision, 3 July). The client's own page leaves open where Electric connects. **Named:** as forced, never as a choice. The proposal's session wrote, in a part you did not paste, that the chat "went from sign-in to a server and never asked that again". | A face whose server side runs elsewhere (near her, on her machine, or written by another builder) has to speak data. The words become the seam, with one connection that carries many watches; the proposal turned that down. Made again: the browser face's server half and the seam's public contract, about 300 lines. The words, the store calls and the checks stand. |
| 4 | A session's end reaches every open stream at once, on every copy of the server. | The feed `$$sessions-lately` on one global task. `lately.clj`. The listeners, `seen`, `broken` and the re-read in `sessions.clj`. `standing`. The second row read in `watch`. A timer per stream. | **From:** the proposal's goal, for copies that do not exist yet. Mastodon, which it cites, looks a session up per request; the feed for open streams is the session's own idea. **Named:** as a goal, never weighed. | With an end that holds within a time limit, or with one copy, the feed and its listeners go: some 350 lines. Nothing else moves. It is the most code in the build for a property you did not ask for. |
| 5 | A system only writes facts and reads facts. The words are the store's two forms, "There is no word for a feature", and nothing runs in the server. | Eight `words`. The MCP tools and the commands are those words. The seam is the door and the read path with a who in front. | **From:** the proposal, as a principle. Your notebook of 21 October says "for initial version only store data on server side". The same page asks "How and where to store computations functions" and for "protocols for running". **Named:** no. | The server runs things for an actor: a tool that is a record, a model's words as they come, presence, a follower of a layer (the proposal's needs 6 and 7, which were cut). The words gain verbs, and pacing and keeping become the server's main work. Little is made again and much is added. "Thin" is the wall that moves. |
| 6 | A read leaves no trace, and a visitor is nobody. | `read` and `watch` write nothing. One `the-visitor` stands for everyone. The duty is called "the keeping of a model's reads", held back because "nothing says yet who is a model". | **From:** the proposal, which quotes your 12 October entry 4 as "A model's reads are kept, always". The entry says "a model's, always; a person's, until she turns it off". So the default keeps every read, and not knowing who is a model blocks nothing. On 19 November you asked about the trail of someone "just roaming around" (‹a892cb43›). **Named:** no. | Every read becomes an event at the seam, and a visitor gets a session with no who. Reads outnumber acts, so this becomes a question of scale for Rama. Made again: `read`, `watch` and the visitor, across the seam, the face over HTTP and the browser's face. A depot is added. `ration` is ready for this; the visitor is not. |
| 7 | There is a time "before a stranger can reach it", on a store that is thrown away, so the guards and the attribution can wait. | `ration` does nothing. "Anybody whom Google proves" becomes a person. The stand-in. A link that never grows old. | **From:** three places: that session's phrase of 12 October; the proposal's call 8; and a session's reading of your 15 October revert. Your own words on that revert were "it did much more than i wanted" (‹1e92f60b›). On 18 November you spoke of running production on your MacBook Air "for now" (‹35df5cbe›). **Named:** yes. Never named: your agents act as you on text from anywhere, so a stranger is already inside. And `ration` takes the seam, the actor and what she does, but no request, so it cannot pace a visitor or a connection. On loopback the server never sees an address. | With strangers and a kept store from day one, most holes are in the right place (`ration`, `person-for`). Two are not: pacing needs the request, and a program's sign-in needs a gesture that a program cannot make. What is written meanwhile cannot be written again: agents' acts under your who, and her email in acts that name no subject. |
| 8 | Every system is a client of one central server over HTTP, at one origin, behind a proxy. | `/word`. A stream as one long response of EDN lines, with an empty line every 15 s. The header against the cookie. The set of origins. The command line's `localhost:8400`, and credentials keyed by a server's address. `X-Accel-Buffering`. | **From:** the proposal: "derived from the systems that connect … not taken from habit". The derivation holds only inside its premises: remote clients, many copies, a proxy that speaks HTTP. Its table lists only clients; another deployment is "in future". Habit sits underneath. **Named:** yes, as derived. | Her own Softland on her own machine ("users will want to have access to their own data", 5 July), or deployments that publish to each other. Then who has to travel between them (row 2, and your open call on the system's id), and the words become a protocol between servers. The identity half is made again. The words, the seam's tasks and the store calls stand. |

## 3. The rest

**Forced by a checked fact (6)**
- Something trusted stands between Rama and any program not trusted with the whole store. Rama asks no name and its client is JVM-only (Marz's posts and the release notes; read, not run).
- EDN on the wire: the store's values include keywords, UUIDs and integers of any width (`MODEL.md`).
- A function that Rama calls must return at once, so the seam hands each change to its own thread. This was run in the read path's plan: one 2.5 s stall held up others' reads.
- Rama allows 1,000 live reads per table per task (run). That is why the feed uses one push per copy, but it forces that only given row 4.
- A resend carries the same `:session`, because the digest covers it (`digest.clj`, the seam's proof). This forces a stable session, not one that lasts as long as the credential.
- The browser and the server are one build, and a tab reloads after a deploy. Electric's adapter refuses another version: the reviewer read this in the JAR, and nobody checked again.

**Forced by a fact nobody checked (2)**
- "MCP is a call and an answer", so an agent's `watch` collects for 2 to 55 seconds. The proposal says this was "from memory and not looked up". From my own memory, MCP has resource subscriptions and progress notifications; whether a harness hands them to the model, I do not know. Claude Code can also run `softland watch` in the background and wait on it.
- Google's tokens have the form that the stand-in signs. Every check ran on the stand-in's tokens, which were made from the same reading of that form. No proof fetched Google's keys.

**Your decisions (7)**
- Google is the way in, with no client secret, and its account number kept as given (15 October; 19 November).
- An act's truth is the record, read by act-id; the ack is only a signal (7 October). That the answer is pulled with a wait, act by act, is the proposal's.
- What lasts is in Rama, sessions too (‹2c90529b›).
- A read is done in the runtime, so a push is shared only inside one copy (11 and 12 October, entry 3).
- No copy is kept between requests: the session's row and Google's keys are read each time (the stop on caching).
- A person has a personal layer, made at her first sign-in, and `write` goes there (8 October, entry 2).
- The store is not changed, because a rule of the store is yours and you were away. That is why revocation sits in the server's module (see below).

**The proposal's (11)**
- Many copies that share only Rama, after Rama's Mastodon build. With one copy forever, the feed goes and nothing breaks.
- Two kinds of system: one compiled with the server (Electric), and those that speak the words. The two roads to the seam have different contracts: the browser's face writes one fact into her own layer, while the words take any act.
- Softland's own client is taken to be an Electric program served by this server, though the client's page leaves that open. Never named.
- A JSON client carries EDN inside strings, so an agent writes EDN inside JSON.
- A session is the life of one credential: 30 days, bound once, never renewed. The digest forces this only if the server, and not the maker, picks `:session` (call 2).
- An act on the wire has no `:who` or `:session`; the server writes both (call 2).
- Sessions live in a module of the server's own (call 1).
- No state that sockets share lives in an atom of the server.
- The server listens on loopback only. TLS, the true origin and the client's address belong to another program.
- An agent works at the level of the store's acts, act-ids, digests and permissions, because the MCP tools are the words. This is against your line of 4 July: "understandable by agents in minmal tokens as possible". Never named.
- The 15 October revert is read as "build the least". The line of yours that it cites (‹3165b020›) also says "there should be no readback and no other thing created". This build reads back at each sign-in and made a new module.

**The builder's, named (13)**
- Words are maps (fork 9). No constraint.
- `send-all` sends in order and stops at the first word (fork 11).
- The status is 200 for whatever the store said, so a refused act exits 0.
- The numbers: 30 days, the frame bounds, 64 KiB, 4,096, 1,024, 60 s, 15 s, 32. The 30 days is habit.
- The system acts only through the server's own steps, and each start of the server is one session of the system.
- A hash is bound once. When two first sign-ins of one account race, the one with the lower stamp is the person.
- A sign-in under way is kept nowhere (fork 10).
- Session events and ended rows are kept for ever, and the feed of 32 sits on one task.
- What Google says is written again at each sign-in, for ever (SIGN-IN-WRITES, row 3).
- Every call of the store runs on a virtual thread.
- One machine, the in-process cluster, a store that is thrown away. `--conductor` was never run.
- No identity for a service, a machine or a webhook: every system signs in as a person.
- Electric `-45`, with its build login copied from `:inland` without being read.

**The builder's, never named (5, besides the 4 in rows 1 and 7)**
- Revocation lives only in the server's module, because nothing in the store ends a permission (`MODEL.md`, step 6). Ending an agent's grant means ending its session.
- Sign-in's acts carry no `:subjects`, so the act that holds her email and name is not marked as about her. On a kept store that cannot be added later.
- The command line keeps `:who` and `:own-layer` in its file and builds acts from them. The server's own page calls such a kept copy a cache when the server does it.
- Whether a session's time has passed is judged on each copy's own clock.
- The command line is ClojureScript on Node, chosen for "no JVM start". A JavaScript number cannot hold the store's integers, so every value passes through as text (`Verbatim`), and any later command that computes on a value hits that wall. Babashka, Clojure that starts fast, is not named anywhere (I know of it from memory).

**Habit (3)**
- Whoever holds the token is her: a bearer secret. The proposal itself calls this "the usual practice of the web".
- Ring and Jetty, and `data.json`.
- Sign-in as hand-written HTML pages of the server, outside the medium.

## 4. The whole, from the relaxed side

Most of the build follows from two assumptions.

The first: an actor is a person, and only the server can vouch for her. From it come the module of sessions, the token, her yes, the command line's file, and the MCP adapter that shares that file. On this ground three things cannot be built yet: an agent's who, the keeping of reads, and a ration that knows whom it paces.

The second: the server is a thin layer in one process. It adds that who to the store's two forms and does nothing else. From it come the seam as an in-memory API, the browser's face compiled into the server, and eight words that are just the store's forms. What it cut: signals, a follower of a layer, reads that are kept, and anything that runs.

A third assumption lets those two stand: a time before strangers, on a store that is thrown away. It is a session's phrase, and two of your own texts pull apart on it. The root page says "'not needed today' is not a reason to drop or delay it". On your 15 October revert you said a build did "much more than i wanted". The build followed the second. On a thrown-away store that costs nothing. From the first kept record it costs things that cannot be written again.

Built from the relaxed side, these would come out the same, because each rests on a checked fact:
- the thread rule;
- the merge by place;
- act-ids asked for first, and the receipt with its digest;
- the bounds on a frame;
- one function that calls the door's `send-act`;
- a token kept as a hash;
- the cookie and origin rules;
- no truth kept in memory.

None of these has to move under any row of the table.

These would come out differently:
- **The actor's slot.** The session's row would bind a credential to an actor with a kind and a chain to its person, from the first commit, even while every actor was you. Your long prompt of 15 October and the proposal's own lean both said so.
- **Reads.** A read would be an event at the seam from the start, since your default keeps reads.
- **The seam's contract.** It would be data, so that where a face runs is not a wall.
- **The place for duties.** It would see the request, not only the actor.

Is there a form that does not have to be rethought when the next constraint goes? For most of the table, yes. It is the form where what varies is data in the store, not a shape in code:
- an actor as facts, with a kind and a chain to its person;
- a grant as an act;
- the seam's contract as words.

In that form, agents, scopes, runs, visitors, the keeping of reads and the server's signature arrive as new kinds, new facts and a new part of an act, and no wall moves. It needs one thing the store lacks, the end of a permission, and that rule is yours.

Two things no form absorbs:
- **Your open call:** a who the server vouches for, against a who that proves itself. For kept acts this is a one-way door, and the first kept record shuts it.
- **One central server, against each person's own Softland.** If that goes, who has to travel between deployments, and every form built around one witness is rethought.

## 5. What I did not read, and what I only suppose

- **Read whole:** the code, READMEs and docstrings of `src-server`, `src-server-browser`, `src-cli` and `bin/softland`; `PROPOSAL.md`; `src-store/MODEL.md`; the store README's decisions of 6 to 14 October; the door's namespace docstring; `first-act-2026-11-18/PROGRESS.md`; and your lines by their ids in the record. I read `BUILD.md` and `SIGN-IN-WRITES.md` only after writing my own list.
- **Read in part:**
  - the read path's namespace docstring;
  - the vision log (3–5 July, 3 August to 13 September, 21 October);
  - the first-act handoff's "Keys, agents and reading";
  - the 15 October receipt and progress pages;
  - the test READMEs and the pure proof's source checks;
  - the client's intended design, on Electric.
- **Not read:** the test bodies, `drive.cjs`, `origin.cjs`, the rest of the store's source, the fault review's report, most of the building session's transcript, Electric's source, and the MCP specification.
- **Run:** nothing.
- **Read but not run:** that a program holding a live token can post the yes page with its own cookie and `Origin` headers, and so begin a session for any hash it makes. This is from `yes!`, `actor-of-cookie`, `own-origin?` and `credential/of-cookie`. `test-cli/sign-in-with-stand-in.sh` does the same with curl, using a stand-in cookie.
- **From my memory, not looked up:** MCP's subscriptions and notifications, and babashka.
- **Supposed:**
  - that ‹e5a2200e› and ‹b6b0f700› are your words. Both are long prompts the record cannot mark as typed, but no session wrote those lines before.
  - that reads outnumber acts.
  - the line counts of what would be made again. They are my estimates over files with long docstrings, not a diff.

---

## What the building session ran afterwards

- **A program that holds a live token can sign in another program as her.**
  Run on the server of 20 November: a program signed in by her yes posted the
  yes page for a second token, with its own token as the cookie and the
  server's origin as a header, and got 200; the second token then stood for
  her. So the two carriers of a credential keep another site's page out, and
  not a program that holds a token.
- **"Due before a stranger can reach the door" is no line of Sid's.** The
  record: typed by him in no line; written by a session first on 12 October;
  it reached his prompts as text that he pasted.
- **"Best is to have each agent their own identity"** is in a long prompt of
  his of 15 October (‹e5a2200e›). The record cannot tell whether he typed or
  pasted it; no session wrote it before that.
