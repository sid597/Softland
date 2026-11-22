# Auth (`src-auth`)

The Rama module `Auth` and its foreign client namespace `softland.auth.client`, implementing all 18 calls of Auth's contract (`src/proposal/runtime-2026-11-21/spine/auth/SPEC.md` and `PLAN.md`). Auth holds who has been invited, which account is linked to which person, each person's agents, sign-ins, sessions, and device-grant asks, and pushes live sign-in/session changes to runtime copies.

## Classpath

Included under the `:auth` alias in the repository root `deps.edn` (`src-auth/` for source, `test-auth/` for checks, Clojure `1.12.4`, Rama `1.6.0`).

## Where to enter by question

- How a runtime copy opens a client, calls the 18 contract functions, or receives `watch-root` pushes and `:softland.auth.client/not-reached`: [softland/auth/](softland/auth/README.md) (`client.clj`).
- What input forms and bounds Auth accepts before appending anything to Rama: [softland/auth/](softland/auth/README.md) (`fit.clj`).
- How Rama depots, PStates, the stream topology `"auth"`, and the query topologies `"who-is"`, `"under"`, and `"ping-task"` are wired: [softland/auth/](softland/auth/README.md) (`module.clj`).
- Record layouts, ASCII key encoding, read projections, and the pure single-task state transitions for `invite`, `enter`, `answer-ask`, take-out, and 2-scan `end-actor`: [softland/auth/](softland/auth/README.md) (`schema.clj`).

## Folders

- [softland/](softland/README.md) — Softland namespace root.
