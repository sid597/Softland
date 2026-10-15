# Runtime — who is acting

[Parent: namespace root](../README.md) · [Runtime source entry](../../README.md).

This folder holds the runtime: what it takes to know who is acting, and to make
every act and read say so. Its pure part needs no cluster: nothing in it reads a
clock, takes a random source, opens a port or calls the network; a caller hands
in fresh ids, random bytes, the time, Google's key set, and what each read and
act was answered. The service drives the pure part on a cluster, and the way in
reaches the service over HTTP; each file that is not pure says so below.

## The parts

| File | Responsibility |
|---|---|
| [ids.clj](ids.clj) | The store's two known ids, the system's six layers, an account's id (a name-based UUID of its kind and number), and a name's rule and entity |
| [records.clj](records.clj) | What the runtime writes into the store: each record's layer, entity and parts, declared in the act's shape language, with rules, and the facts a record becomes; the keys each system layer marks as taking one write |
| [token.clj](token.clj) | Tokens and invitation codes: made from ids and random bytes, kept as a SHA-256 hash, read back, checked in constant time |
| [google.clj](google.clj) | The check of Google's ID token with Nimbus, against a key set and a time handed in |
| [binding.clj](binding.clj) | An actor bound to her act and her query; what a block stops; whether a count is over a limit |
| [steps.clj](steps.clj) | The form of steps that are safe to run again: reads, one act of the system at a time, what is known, and the loop |
| [start.clj](start.clj) | The runtime's start as steps: the base and the system's layers, their settings, the system's identity and name, this start's session |
| [sign_in.clj](sign_in.clj) | A first and a later sign-in, a second account linked, and an invitation made, as steps |
| [system_steps.clj](system_steps.clj) | The runtime's other work as the system, as steps: a name changed, an agent or a program made, a token's session made and ended, an agent's run recorded; and the facts of a blocked attempt |
| [service.clj](service.clj) | The runtime as a service on a cluster: the door and the read path opened on the module `Store`, the steps driven with them, an actor's acts and reads bound and held to blocks and limits, tokens asked for and approved (not pure: Rama, a clock, a random source, and the network for Google's keys) |
| [page.clj](page.clj) | The one page, as HTML from values: Google's button and its callback, and, signed in, who she is and the small forms (pure) |
| [admin.clj](admin.clj) | The admin token: made from random bytes handed in, written to a file only Sid's user can read, read back, compared in constant time, removed when the runtime stops (not pure: a file) |
| [http.clj](http.clj) | The way in over HTTP on 127.0.0.1: the routes, who is asking (the sitting's cookie, a bearer token, the admin token), the Origin and `g_csrf_token` checks, EDN and form bodies (not pure: a port, and the service) |
| [command.clj](command.clj) | Sid's command line: invite, link, block, unblock, limit, through the admin routes (not pure: a file and HTTP) |
| [main.clj](main.clj) | The runtime, run: the in-process cluster with the store's module, the service, the admin token, the server; everything thrown away when the process ends (not pure) |

## How the parts relate

A browser's sign-in comes with Google's ID token. `google` checks it and gives
the account's number and what Google said of it. `sign_in` takes those claims,
an invitation's code when there is one, and fresh ids, and gives one thing to
do at a time: reads to make, one act of the system to send, done, or a refusal.
Its acts carry facts that `records` made and checked, under the system's
permission or a making act (`steps`); its tokens and codes are `token`'s. A
`service` drives `start`, `sign_in` and `system_steps` with the store's door and
read path (`steps/drive`), each on an input made once, and waits for each act's
answer before the next; the pure proof drives `start` and `sign_in` on a model
of the store.

An act and a read of an actor go through `binding`, in `service`, which sets
`:who`, `:session` and the asker from the actor and refuses an act or query that
names another, and an agent's or a program's fact under a store key that its
right's key list does not name. An act that is not in the act's form is not
refused there: it goes on to the door with her name on it, and the gate logs
and refuses it. Whether a block stops it, and whether a count is over a limit,
are `binding`'s pure tests; `service` reads, at each act, the blocks and the
limits of acts and bytes of the sender and of each identity above it in its
chain of makers (an agent or a program and the person who made it), counts
each act toward every one of them in its memory for the last minute, and has
the system write the count of blocked attempts, and the first few whole, in
the sender's record layer (`system_steps`). Who an actor is comes from her
token, which `service` checks against the hash kept beside her session at each
request.

The way in. `main` starts Rama's in-process cluster with the store's module
`Store`, opens the runtime's service on it (`softland.runtime.service`), runs
its start, writes a new admin token (`admin`), and serves `http`. `http`
answers only at `localhost:<port>` (a request to `127.0.0.1:<port>` is sent on
there), and forbids every answer a frame. It reads each request, finds who is
asking from the sitting's HttpOnly cookie or a bearer token by the service's
`actor-of` (an agent names its run in the `Softland-Run` header), checks what a
browser's POST must carry (its Origin; at a sign-in, the `g_csrf_token` cookie
equal to the body's), and hands the request's EDN, or the input a form of the
page gives, to one call of the service; it answers EDN, or the page (`page`)
again for a form. The page reads an invitation's code from its fragment,
`#invitation=`, so that the code never leaves in a Referer. The admin routes
take only the admin token, which `command` reads from its file. `http` keeps
nothing between requests: the browser holds the cookie, and the service checks
it at each request.

The forms that the steps make and read are the store's: an act in its one form
(`softland.store.act`), a query (`softland.store.read-path.query`), an answer
(`softland.store.read-path.answer`), and the outcome of an act
(`softland.store.door/answer-of`). The runtime keeps no copy of them.

## State

None in the pure part. Each of its functions is pure but `steps/drive`, which
calls the two functions that its caller hands it. What is known during a run of
steps is a value that the caller holds and passes back; nothing here keeps it
between calls.

`service` owns, in the memory of its process: the system's session of this
start, what counted toward each identity in the last minute, each sender's count of blocked
attempts, the pending token requests (each lapses after ten minutes), and the
turns that keep one sender's counts and one request's approval in order. Its
namespace docstring says what each holds and what is lost when the process
ends. It borrows the door's and the read path's handles; the opener owns the
cluster handle.

The way in owns, while `main` runs: the in-process cluster (its files are
Rama's, in a temporary folder, removed when it closes), one server socket on
127.0.0.1 and its threads, and the admin token's file,
`~/.config/softland/runtime-admin-token` (0600), written anew at each start and
removed when the runtime stops.

## Boundaries

- The store decides whether an act's permission stands and what it allows; the
  binding does not read the record.
- What the system enforces from facts of other layers (the kind of an actor,
  sign-up, blocks, limits, no agent at the top of a chain) is the runtime's,
  and kept as facts (BUILD.md, default 13).
- Not in the pure part: the fetch of Google's keys (`service`'s default, which
  `main` hands in), the HTTP routes and the cookie (`http`), and the counting
  of acts and of blocked attempts, the token requests, the fresh ids and the
  time that the steps are given (`service`). They need a cluster, the network
  or a clock.
- `service` writes only through the door: as the system for the steps, and
  as the actor for her acts. The rights it gives or ends are her acts, which
  the gate decides; only blocks, limits and the binding stop an act before
  the door.
- The way in decides no right and keeps no record: what each route does is
  the service's call, and whether an act or a read is allowed is the binding's
  and the store's. `http` checks only who is asking and what a browser's POST
  must carry.
