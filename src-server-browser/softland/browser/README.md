# softland/browser — the browser's face

[Tree entry](../../README.md).

One Electric program, its host, and the program that runs them. The server
half of the program calls the seam, as the face over HTTP does; the client
half draws a plain page.

| File | Responsibility | State |
|---|---|---|
| [app.cljc](app.cljc) | The Electric program, and the one function that boots it on both peers. On the server: her standing, her own layer's id, the steps of an act, and the live read. On the client: the page, its forms, and the acts under way. | On the server, none of its own: what a socket's program holds is Electric's, and ends with the socket. On the client, the page's state: the acts under way and the entity that is watched. |
| [boot.cljs](boot.cljs) | The browser's entry: boots the client's half into the page, with the page's state made once for each load. | The page's state, and the function that stops the client. |
| [host.clj](host.clj) | A Ring handler in front of the face over HTTP: tests a socket's `Origin`, boots the program for a socket that passes, serves `/app` and the built files under `/js/`. Also Jetty's WebSocket limits. | None. |
| [main.clj](main.clj) | The command: starts the cluster and the server as `softland.server.main` does, wraps its handler with the host, and serves it. | The cluster and what the server opened, for the life of the process. |

## How they relate

`main` calls `softland.server.main/start` with `:app-path "/app"`, so the
sign-in page links to the program, and wraps the handler that it gives with
`host/wrap`. A request for `/app` gives a page that loads `/js/main.js`, which
`boot` starts. The client opens a socket on the same path; `host` tests its
`Origin` and, when it passes, boots the server's half of `app` with the seam
and the token of the socket's cookie. From then on the two halves are one
program, and the server's half speaks to the store through the seam alone.

## The three rules, and where each is kept

1. **The actor is a value of the server that cannot cross the wire.** In
   `app.cljc`: her standing, which holds the actor, is held in the server half
   of `Main`, and the actor is used only inside `e/server` expressions. The
   seam, sign-in and watching are required on the server peer alone, so a call
   of the seam written on the client does not compile. The client is sent
   `seam/about` and her own layer's id. Every act and every query is built on
   the server from her who and her own layer; the client hands back only an
   entity, a key, a value, and an act-id that the server gave it, which the
   server tests against her own layer before it sends anything.
2. **No state that sockets share lives in an atom of the server.** No file
   here defines state on the server. The state of a socket is Electric's; the
   state of a page is the client's.
3. **The socket's upgrade checks `Origin`.** In `host.clj`, before the
   program is booted: a browser adds its cookie to a socket that any page
   opens, and Electric's adapter checks no origin. A socket from another
   origin is closed at once with 1008.

## What is not built

- An answer `:nothing-yet` after the wait is kept as the act's answer; the act
  is not asked about again.
- The acts under way are lost when the page is reloaded: they are kept in the
  page, not in the browser's storage.
- Every change of a live read sends the whole answer to the client, not the
  places that changed.
- One entity is watched at a time, from her own layer only.
- No ration of acts or of live reads: that is the seam's, and not built there
  either.
- No styling, and no part of Softland's own renderer.
