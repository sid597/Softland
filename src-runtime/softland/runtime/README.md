# Runtime — the parts of the runtime

[Parent: softland](../README.md) · [Runtime source entry](../../README.md).

## The parts

| File | Responsibility | State and lifetime |
|---|---|---|
| [seam.clj](seam.clj) | The seam: a credential to a caller; each act and each query held against its caller; the words, done through the store's door, the store's read path and Auth; a watch; and a caller's standing over time. | In the memory of one process: its numbers and its threads. Nothing between two calls. |
| [standing.clj](standing.clj) | One watch of Auth for each root that has something open in this process, shared by everything of that root. | In the memory of one process: a root's listeners and its one watch, while it has listeners. |
| [sign_in.clj](sign_in.clj) | What sign-in does: an account becomes a person with a lane of her own; a browser's sign-in; her name; a program's sign-in that is under way; a deployment's birth. | None of its own; it writes Auth through its client, and acts through the seam. |
| [line.clj](line.clj) | The line: one connection, many words and many watches, in one session, ended when its caller stops being live. It knows no socket. | In the memory of one process, for a connection's life: the words that wait and its watches. |
| [jetty.clj](jetty.clj) | What carries the faces: one Jetty server, a Ring handler for the pages, and one path upgraded to a WebSocket for the line, with the frame's bound set for each connection, a bound on how long one write waits, and the close of a connection that is not heard from. | The server and the open connections, each with when it was last heard from, for the ping. |
| [http.clj](http.clj) | The pages of sign-in, of her name and of her answer to a program; a program's two requests; and who a connection to the line is. | None. |
| [google.clj](google.clj) | The check of Google's ID token, and the fetch of Google's public keys. | None; the fetch keeps nothing. |
| [google_stand_in.clj](google_stand_in.clj) | A stand-in for Google that signs ID tokens with a key pair of its own, for the proofs and for a store that is thrown away. | A key pair in the memory of the process. |
| [main.clj](main.clj) | The program: `serve` and `setup`. It connects to Rama, opens everything, and serves the faces. | What it opened, until the process ends. |

## How the parts relate

```text
a command line, an agent's harness:              a browser:
the words, over the line                         the pages of sign-in
        |                                             |
        v                                             v
   jetty.clj --- line.clj                         http.clj --- google.clj
        |            \                              |    \
        |             \                             |     sign_in.clj
        +--------------+-----------+----------------+        |
                                   v                         |
                               seam.clj  <-------------------+
                 /         |          \         \
      standing.clj     the door    the read path   (src-store)
           |               \          /
           v                v        v
   Auth's client       the store's module `Store`
   (src-auth) -> the module `Auth`
   ------------------------- Rama --------------------------
```

A connection to the line arrives at [jetty.clj](jetty.clj), which asks
[http.clj](http.clj)'s `accept` who it is: the seam makes a caller from the
credential, or the request is answered over HTTP and no socket opens.
[line.clj](line.clj) then reads each frame with the wire's `frame`, checks the
word with the wire's `words`, and hands it to the seam with the caller. The
seam holds an act's who and session, and a query's asker, against the caller,
and calls the store's door or its read path. The results go back as they are.

[sign_in.clj](sign_in.clj) is called by [http.clj](http.clj) at a sign-in,
after [google.clj](google.clj) passed the token, and by [main.clj](main.clj)
for the setup. It is the one caller of the seam's `send-as-system` and
`read-as-system`.

## Boundaries

- Two files require the store's door and its read path: `seam.clj`, and
  `main.clj`, which only opens them. The seam calls the door's `send-act` in
  one place, and its `send-acts`, for a series, in one place.
- Four files require Auth's client: `main.clj`, `seam.clj`, `standing.clj` and
  `sign_in.clj`. No face does.
- The runtime opens no table and no depot of Rama's itself.
- The seam writes no who and no session into an act, and no asker into a
  query. The acts of the system are built in `sign_in.clj`, whole.
- Nothing here requires Electric, the rig, the server of 20 November, the old
  server or Inland.

The runtime's pure proof reads the source for each of these.

## Engineering choices

Each is a session's, kept and not decided.

- **The runtime refuses an act that names another who.** It writes nothing
  for it. Sid's line of 12 October on a maker who may not act, "why don't we
  store both?", is of an act under the maker's own who; an act under a who
  that did not send it would be a false line in a log that is never trimmed.
- **A credential names its root and its sign-in.** So one read on one task
  finds it, and Auth needs no table from a secret to its holder.
- **A browser's sign-in is taken from a cookie alone, a program's from a
  header alone,** and Auth holds which a sign-in is.
- **A push is a signal, and a read decides.** What Auth pushes can be behind a
  write, and it is thinned by the clock of whichever copy wrote last under
  the root, so a thing that is not in it has not ended for that. Nothing is
  ended on a push alone, but when the watch itself is lost: a read says
  whether someone ended the thing, and its time is held against this copy's
  clock.
- **Ids are made or derived here.** No system chooses the id of a person, an
  actor, a sign-in or a session.
- **Words are done one at a time for a connection,** in the order in which
  they arrived.
- **The numbers:** a secret lasts 7 days, the one before it a minute, a
  session a day from its last moving on, an ask 10 minutes, an invitation 7
  days; a connection holds
  256 waiting words, and 128 watches (a visitor's 8), beneath 256 for all of a
  root's connections and 64 for all visitors in one copy; one copy takes 30
  posts to `/sign-in/*` from one address in 10 seconds, 20 polls of one ask in
  that time, and opens 30 sessions under one sign-in in a minute; a ping every
  20 seconds, a connection not heard from for a minute is closed, and one write
  of a frame waits at most 30 seconds. Each is in `seam.clj`'s
  `default-numbers`, in `line.clj` or in `jetty.clj`.

## Not built

Counts of acts and of reads by person in Auth, and a block: `seam.clj`,
`ration`, is the one place, and it is handed what the face said of the
request. Its first layer, in each copy's memory, is built: the paces of
sign-in posts by address, of an ask's polls and of a sign-in's new sessions,
and the caps of live watches by root, for visitors and for one connection.
The keeping of
reads, which is the trail's (piece 5). MCP (piece 6). The browser's client
(piece 7). An agent's branch lane and `promote` (pieces 6 and 8). The
replacing of a browser's secret by the pages. A word for erasing a person:
she can unlink each of her accounts and take back the invitations that she
made, and who else may erase her is not decided.

## Words a session made

"The seam", "a face", "a caller", "the line", "a copy" of the runtime,
"standing", "a root", "an ask", "born" for a store that has its base. Each is
Sid's to change.
