# Server checks

[Parent: server verification](../../README.md) ·
[Server source entry](../../../src-server/README.md).

A check is explained once, by its sentence in its test file; a run prints that
sentence. This page says, for each file, what it checks, how to run it, what a
pass shows, what state it changes, and what it does not reach.

| File | What it checks | State that it changes |
|---|---|---|
| [proof.clj](proof.clj) | Nothing: it is what the three proofs share, one way to say a check and to end the process with a status. | The counts of the run, in memory. |
| [pure_proof.clj](pure_proof.clj) | By cases, with no cluster: a session's record, the feed, a credential, the words and their check, one frame, what waits for a slow reader, and the check of an ID token against tokens that a stand-in signs. Then it reads the source under `src-server` for what the tree may hold. | None. |
| [seam_proof.clj](seam_proof.clj) | On the in-process cluster, with two copies of what a server holds over one store: what sign-in writes; a credential to an actor; the tasks; a watch, a slow reader of it, and its leaving; a session that ends through one copy and is seen at the other; a session whose time passes; her no. | People, lanes and acts in the store that it started. |
| [http_proof.clj](http_proof.clj) | Through real requests to a Jetty that runs the handler: the frame and the word, sign-in by Google's post and by the stand-in's, the cookie and the origin, a program's sign-in with her yes and her no, the words, the stream of a watch and its end, sign-out. | The same, and two servers on the loopback address. |

Run each from the repository root:

```sh
clojure -M:server -m softland.server.pure-proof
clojure -M:server -m softland.server.seam-proof
clojure -M:server -m softland.server.http-proof
```

## What a pass shows

- **The pure proof:** each rule of the pure parts holds for the cases written,
  and for 500 random runs of a live read's messages a reader for whom a part
  waited and was merged ends with the same answer as one who took each message.
  The source holds what the server's page says under "Boundaries".
- **The seam's proof:** the claims that need a cluster hold on one machine, in
  process: a hash is bound once; an act sent again is done once; a fact names
  the actor as its writer; a session that ends through one copy of the server
  ends a watch and a standing that the other copy holds, and no live read is
  left behind.
- **The proof over HTTP:** the statuses, the cookie's flags, the origin rule,
  the way on after a sign-in and the stream are as the face's docstring says,
  for the requests written. Its client writes the `Origin` that it is told to:
  what a browser writes is the browser check's to hold.

## What no check reaches

Each is where a next check comes from.

- **The feed when a copy missed changes.** The pure proof holds what a copy
  takes from a value of the feed. No run made Rama leave a copy out of a
  callback, so the reading of every listened row again, and what a listener is
  told when that read fails, ran in no proof.
- **A failure of Rama under the server:** a read of a row that throws, an
  append that throws, a push that Rama ends. The words for them are made in the
  code, and no run gave one.
- **More than one process.** The two copies of the seam's proof are two clients
  in one JVM. Two server processes on one cluster were not run.
- **A cluster that keeps its data,** and `--conductor`.
- **Google itself.** Its keys are fetched by `fetch-keys`, which no proof calls;
  the check ran on a stand-in's tokens only. A real sign-in needs the client id,
  which Sid makes.
- **A browser, in these proofs.** The pages are asked for here by an HTTP
  client that writes the `Origin` it is told to. What a real browser sends is
  held by the check of the browser's face
  ([drive.cjs](../../../test-server-browser/README.md)), which found on 20
  November 2026 that the server refused its own forms. No browser has sent
  Google's post.
- **Load:** many streams, many waits, a slow system that holds a stream open.
  What was looked at once, and is no check: the JVM's trace of virtual threads
  that wait while they hold a monitor, and so keep their carrier thread
  (`clojure -J-Djdk.tracePinnedThreads=short -M:server -m softland.server.http-proof`).
  On 20 November 2026 it printed nothing for the proof over HTTP. For the
  seam's proof it printed one stack, the proof's own slow reader, which waits
  inside the function that takes a thing of a flow. A wait inside Missionary's
  `via` prints nothing: an earlier reading of the same trace, which took a
  frame of `via` in that stack for the cause, was wrong.
- **What a frame costs to read,** but by two cases: a token over the bound is
  turned away fast, in both proofs. Measured once and held by no check: the
  largest frame that somebody signed in may send, filled with numbers of the
  longest token, took 11.8 seconds of one thread to read; filled with strings,
  a third of a second.
- **What the review of 20 November 2026 ran and no check holds:** a visitor who
  opens some 900 watches on distinct places of a lane that is open to
  everyone, after which nobody opens a live read of that lane; a person who
  says yes on a link that somebody else made; a second person who says yes
  first on a program's link.
- **A copy of the server whose push on the feed Rama ends,** and one whose
  rows are gone after the module is launched anew: the code tells its
  listeners that the sessions cannot be reached, and no run made either
  happen.
- **What ends TLS in front,** and that it passes a stream line by line.
- **The time of a session over 30 days,** but by a session of 1.5 seconds.
