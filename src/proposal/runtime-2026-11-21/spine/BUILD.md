# The spine (Piece 4): build report, proof findings, and open calls

**22 November 2026.** Written after the overnight integration, oracle-phase bring-up, and adversarial breaker review of Piece 4 (`src-runtime/`, `src-command/`, `test-runtime/`, `deps.edn`, and the non-production test shim `spine/runtime-oracle/`).

Everything below reflects Sid's three mid-run changes of 22 November (`02:24:59`):

1. **Step 4 skipped:** No real `src-auth/` lands tonight (Session 1 is not running). Did not wait for it; did not build `auth.jar`, run `softland.runtime.cluster-proof`, or run the persistent-cluster Piece 1 + Piece 4 Join proof.
2. **Step 3 — no guard code written:** `seam.clj/ration` remains a no-op stub and `http.clj` carries no `/sign-in/*` pacing code. The design options and tradeoffs for where the counts of `ration` live are laid out [below](#where-the-counts-of-ration-can-live) for Sid to decide in the morning.
3. **`:driven-by` in the stand-in, not on the wire:** The non-production Auth oracle shim (`spine/runtime-oracle/`) implements optional `:driven-by` on `open-session` and `who-is` per `spine/auth/SPEC.md` (`37bc0e01`). The wire (`src-wire/`) was not touched; which word of the wire should carry `:driven-by` is analyzed [below](#which-word-of-the-wire-should-carry-driven-by).
4. **Proof immutability rule honored:** No check or assertion in `test-runtime/` was modified to make a proof pass. The five failures in `test-runtime/softland/runtime/checks.clj` are documented verbatim [below](#proof-findings-in-unmodified-checksclj). `/mnt/data/projects/research/softland/runtime-spine-2026-11-21/as-written-2026-11-22` was never modified.

---

## 1. What was done (Steps 1, 2, and 5)

### Step 1: Proof cluster cleanup, `store.jar`, `deps.edn`, and `pure-proof`
- **Old `0.0.0.0` cluster stopped:** Terminated the four leftover Rama processes from 21 November (`371879` devZookeeper, `372089` conductor, `373484` supervisor, `375815` worker `Store`). Verified ports `1983`, `2101`, `8898`, `13000–13001` are clear (`ss -tln` returned empty).
- **Cluster config & Rama 1.6.0 loopback binding wall:**
  - Updated `/mnt/data/projects/research/softland/runtime-spine-2026-11-21/cluster/rama.yaml` to set `zookeeper.servers: ["127.0.0.1"]`, `conductor.host: "127.0.0.1"`, and `supervisor.host: "127.0.0.1"`.
  - Created `/mnt/data/projects/research/softland/runtime-spine-2026-11-21/cluster/zoo.cfg` (`clientPort=2101`, `clientPortAddress=127.0.0.1`, `admin.enableServer=false`) so standalone ZooKeeper (`zookeeper-3.8.6.jar` in `cluster/lib/`) can bind strictly to `127.0.0.1:2101`.
  - **Genuine structural wall in Rama 1.6.0 (not hacked around):** Direct bytecode inspection of `cluster/rama.jar` proves that Rama 1.6.0 hardcodes wildcard socket binds with no bind-host setting anywhere in `rpl.rama.distributed.config/CONFIG-SPEC`:
    - `rpl.rama.distributed.util.zookeeper.inproc_server$start_inproc_server` calls `new InetSocketAddress(int port)` (mitigated by standalone `zoo.cfg`).
    - `rpl.rama.distributed.daemon.conductor.ThriftServer` (port `1983`) calls `new TNonblockingServerSocket(int port)`.
    - `rpl.rama.distributed.daemon.conductor.webui_backend.ConductorAdminUiWebserver` (`8898`) and `rpl.rama.distributed.daemon.supervisor.supervisor_http_server$start_server` (`13000`) call `ring.adapter.jetty/run-jetty` without `:host`.
    - `rpl.rama.distributed.worp.core.common.ProductionNettyFactory.binding_address` (`13001+`) calls `new InetSocketAddress(int port)`.
    - Consequently, on Rama 1.6.0, keeping Conductor/UI/Supervisor/Worker ports off LAN interfaces requires OS firewall rules (`ufw`/`nftables`), a network namespace (userns `unshare -rn` is blocked by kernel policy on this host), or VPC security groups — `rama.yaml` alone only controls advertised hostnames (`HANDOFF.md` lines 148–149 already noted this).
- **Fresh `store-at-head` and `store.jar`:** Extracted `src-store` from `git archive HEAD -- src-store` (at `4682f933`, holding Store Pieces 1 & 2 including `softland/store/gate/chain.clj`) into `/mnt/data/projects/research/softland/runtime-spine-2026-11-21/store-at-head/src-store` and rebuilt `/mnt/data/projects/research/softland/runtime-spine-2026-11-21/build/store.jar` (`255K`).
- **`deps.edn` and syntax fix:** Added `"src-command"` to `:runtime` `:replace-paths` in `deps.edn` (and kept `"test-command"` removed from `:command`). Fixed the extra closing `)` in `test-runtime/softland/runtime/checks.clj` at line 214 (which closed line 195's `let` prematurely and caused line 413's unmatched delimiter; both fixes are present in the `as-written-2026-11-22` snapshot taken at 02:12).
- **`softland.runtime.pure-proof`:** Passed **22 passed, 0 failed** with all source-boundary assertions intact.

### Step 2: Non-production `runtime-oracle` shim (with `:driven-by`) & runtime bring-up
- Created `src/proposal/runtime-2026-11-21/spine/runtime-oracle/` (**non-production test shim only**):
  - `src/softland/auth/module.clj` — minimal valid `defmodule Auth` so `softland.runtime.main/in-process-cluster` launches cleanly.
  - `src/softland/auth/client.clj` — pure in-memory state-machine implementation of `spine/auth/SPEC.md` shared across runtime copies opened on the same cluster handle, with strictly ordered single-threaded async `watch-root` callbacks, `:at` expiry sweeps, refusal precedence, non-throwing input validation, post-`close!`/post-`stop` callback barriers, and optional `:driven-by` on `open-session` / `who-is` (absent/nil = not said; unchanged by repeat `open-session`; readable after `close-session`; omitted from `watch-root` snapshots).
  - `test/softland/auth/oracle_proof.clj` — 6 self-checks verifying `:driven-by`, repeat-after-close idempotency, malformed non-map inputs, and watch-stop silence.
- Executed `pure-proof`, `oracle-proof`, `proof`, and `command-proof` behind `flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock` against `store-at-head/src-store` (never loading live working-tree `src-store/`).

### Step 5: Fresh Adversarial Breaker Review & Fixes
Spawned a fresh read-only `self` subagent (`Model: inherit`, conversation `29c5cc0d-3bf1-4c89-af4c-2a04f937cfd6`) to break our diff and the entire `src-runtime/` + `src-command/` + `runtime-oracle/` trees against `as-written-2026-11-22`, `SPEC.md`, `PROPOSAL.md`, and Store Pieces 1 & 2.

**Fixed from the breaker review (zero guard code added, zero proof assertions altered):**

| File | Severity | Fix |
|---|---|---|
| `test-runtime/softland/runtime/command_proof.clj` | BLOCKER (tonight's helper) | `command-sdeps` (opt-in via `-Dsoftland.store.path=...` for Rule-2 isolation) now preserves `:replace-deps` (`clojure 1.12.4`) and `:jvm-opts` (`-XX:TieredStopAtLevel=1`) so Clojure CLI shallow alias merging cannot pull Rama onto `:command`'s classpath; guards with `not-empty`. |
| `spine/runtime-oracle/src/softland/auth/client.clj` | HIGH (×2), MEDIUM (×2) | Serialized state update + single-threaded FIFO notifier (fixes out-of-order/concurrent `watch-root` delivery); `open-session` repeat after close/expiry returns `{:open S}` without reopening per `SPEC.md:82-90, 374`; non-map input validation without arg-list destructuring throws (`SPEC.md:75`); `0..256` char `text?`; per-client callback lock guarantees zero `f` calls after `close!`/`stop`; `WeakHashMap` avoids leaking closed IPC clusters. |
| `src-runtime/softland/runtime/seam.clj` | HIGH (×3), MEDIUM (×2), LOW (×1) | `query-fault` now checks `:session` against `(.-session caller)` for Store Piece 2 `run-permission` safety; `Caller.hash` is an atom updated on successful `:replace-secret` (fixes 2nd rotation on long-lived connections); `send-all-now` checks thread interruption between acts so revoked callers stop writing; `:under` filters expired sign-ins against `now-ms` (`SPEC.md:355-357`); `standing` dispatches `check!` on `executor` instead of blocking `standing/drain!`'s single thread, holds `lock` in cancel thunk to prevent `ScheduledFuture` timer leaks, and clamps the half-life renewal timer strictly below `session-until` for short `:session-ms`; `ends-after`/`sent` use unchecked `nanoTime` arithmetic. |
| `src-runtime/softland/runtime/standing.clj` | HIGH | If `(force (:opening entry))` returns a non-fn (`{:could-not ...}` from `auth/watch-root`), immediately invokes `(pushed! standing root entry not-reached)` so broken entries are evicted and listeners fail closed instead of running permanently unwatched. |
| `src-runtime/softland/runtime/line.clj` | HIGH, MEDIUM | `end!` clears the queue before offering `::closed` and interrupts the connection's worker thread; `say!` ends the connection if `send!` throws; `:watches` stores `{:token :leave}` so an async `done` callback cannot wipe a reused watch ID. |
| `src-runtime/softland/runtime/jetty.clj` | HIGH (×2) | Re-checks `(line/open? conn)` after `(.put sessions session conn)` to eliminate early-close leaks in `ConcurrentHashMap`; dispatches per-session pings on a virtual-thread executor so one stalled TCP peer cannot starve pings to all healthy clients. |
| `src-runtime/softland/runtime/http.clj` | HIGH, MEDIUM (×4) | `POST /yes` verifies both `yes?` and winning `:root` before displaying `"Done. The program is signed in"`, returning 409 on lost races; `GET /yes` checks ask `:ends` against `now-ms`; `POST /yes` checks empty agent label only when `yes?` is true; `signed-in!` bounds `:says` keys to 256 chars and applies `(take 32)` after filtering scalar claims; `accept` returns 401 on a malformed/duplicate `softland-sign-in` cookie instead of silently downgrading to visitor. |
| `src-runtime/softland/runtime/google.clj` | HIGH | Shares one `delay`-initialized `HttpClient` instead of allocating and leaking an unclosed `HttpClient` / `SelectorManager` thread per `fetch-keys`. |
| `src-runtime/softland/runtime/main.clj` | MEDIUM | `setup` exits 2 on malformed `--person` instead of silently generating a new random UUID. |
| `src-command/softland/command/client.clj` | HIGH (×2) | Shares one `delay`-initialized `HttpClient` (eliminates up to 600 leaked clients during 10-minute `sign-in` polling); serializes `.sendText`/`.sendClose` under `(locking connection ...)`; atomically rejects inserting into `:waits`/`:watches` once `:gone` is set and removes entries on send error or timeout. |
| `src-command/softland/command/main.clj` | HIGH, MEDIUM (×2) | `keep!` creates temp file with mode `rw-------` from birth and atomically moves into place (closes world-readable TOCTOU window and torn writes); `replace-secret` skips `with-line`'s redundant pre-command auto-rotation; `new-session` refuses cleanly when not signed in instead of writing `nil` into `sign-in.edn`. |

**Remaining pre-existing findings from the breaker review (not fixed tonight — documented for next pass):**
- `jetty.clj:115-124` (MEDIUM): A request to `/line` with `Upgrade: websocket` but missing `Sec-WebSocket-Key` / `Connection: Upgrade` calls `accept` (opening an Auth session) before `(.upgrade ...)` returns `false` and sends empty 200 OK.
- `sign_in.clj:373-377` (MEDIUM): `setup!` returns a normal map (causing `main.clj setup` to exit 0) if step 3's microbatch permission-give returns `{:answer :nothing-yet}` or an unexpected refusal.
- `command/main.clj:249-260` (MEDIUM): `send-file!` reuses saved UUIDs from `<file>.sent.edn` even if `:session` changed (causing `:not-mine` with exit status 0); `query-of` (`main.clj:205-211`) does not pass `:session` yet for Store Piece 2 run-permissions; `agent new <name>` (`main.clj:336`) does not sanitize path separators in the default filename.
- `line.clj:211` (LOW): Queue-overflow in `text!` calls blocking `say!` on Jetty's WebSocket I/O thread.
- `http.clj:432-436` (LOW): `GET /yes` checks ask existence before auth cookie (404 vs 303 unauthenticated code probe).
- `checks.clj:262-264` (LOW): Binds `nothing {:live-reads 0 :watched 0}` but only asserts `(:live-reads now) = 0`, and leaves several test connections unclosed until process exit.

---

## 2. Exact Proof Runs & Outputs

All runs executed after the final edit, behind `flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock`, using `store-at-head/src-store` + `spine/runtime-oracle/`.

| Proof namespace | Command override | Result | Exit |
|---|---|---|---|
| `softland.runtime.pure-proof` | Oracle `-Sdeps` | **22 passed, 0 failed** | `0` |
| `softland.auth.oracle-proof` | Oracle `-Sdeps` | **6 passed, 0 failed** | `0` |
| `softland.runtime.proof` (unmodified `as-written-2026-11-22` `checks.clj` + `proof.clj`) | Oracle `-Sdeps` | **44 passed, 5 failed** | `1` |
| `softland.runtime.command-proof` | Oracle `-Sdeps` + `-J-Dsoftland.store.path=.../store-at-head/src-store` | **16 passed, 0 failed** | `0` |
| `softland.runtime.cluster-proof` | — | **Not run (Step 4 skipped: no real `src-auth` tonight)** | — |

### Proof findings in unmodified `checks.clj`

All 5 failures in `softland.runtime.proof` were independently verified by the fresh breaker subagent to be defects in `test-runtime/softland/runtime/checks.clj` itself, not runtime bugs. Per Sid's rule (*"Never change a proof to make it pass. A proof that cannot pass is a finding in `spine/BUILD.md`"*), `checks.clj` is left completely untouched from `as-written-2026-11-22`.

1. **Defect A — Local binding `first` shadows `clojure.core/first` (`checks.clj:111, 239, 332, 340`) — causes 3 of the 5 failures:**
   - Line 111 destructures `[{:keys [a b c first left tag]}]`, binding `first` to the account map `{:sub "1000001" :email "first@example.com" :name "First Person"}`.
   - Three assertions then call `(first ...)` intending `clojure.core/first`, which invokes the map as a function on a vector and always returns `nil`:
     - Line 239 (`Two watches on one connection...`): `(contains? (first @(:seen one)) :anew)` -> `(contains? nil :anew)` -> `false`, even though the printed `seen:` shows `:one` received `{:anew ... :value :green}` followed by `{:changed {[:chair-7 :colour] ... :value :amber}}` and `:two` received `{:anew ... :holds :no-fact}` followed by `{:changed {[:lamp-2 :lit] ... :value true}}`.
     - Line 332 (`Two first sign-ins of one invited account at the same moment...`): `(= (:invited by-email) (:person (first both)) (:person (second both)))` -> `(:person nil)` -> `false`, even though the printed `seen:` shows both copies converged to the exact same UUID (`#uuid "76361571-3a07-8c79-8eb7-95efd9c1b6cf"`).
     - Line 340 (`Both of those sign-ins lead to one own layer.`): `(and (uuid? (first layers)) (apply = layers))` -> `(uuid? nil)` -> `false`.
   - **Minimal fix for Sid to approve:** Rename the local destructuring at line 111 to `{:keys [a b c left tag] first-person :first}` and update lines 121, 122, and 125.

2. **Defect B — `short-numbers` secret expiry (`6000ms`) vs `12000ms` total sleep (`checks.clj:44, 343-367`) — causes 2 of the 5 failures:**
   - Copy `c` runs with `short-numbers {:secret-ms 6000 :previous-ms 1000 :session-ms 4000}`.
   - At `t ≈ 0`, the test creates `short` on `c`, opens `held` on `c`, and immediately calls `:replace-secret` on `held` (copy `c`, line 348). Copy `c` legitimately sets the **new** secret's `:until` to `t + 6000ms`.
   - Line 353 sleeps `1500ms`, then line 356 sleeps `5500ms` (`t ≈ 7000ms > 6000ms`), then line 364 sleeps `5000ms` (`t ≈ 12000ms > 6000ms`).
   - At `t ≈ 6000ms`, copy `c`'s `standing` timer rightfully fires, reads `who-is`, sees `until <= now`, and ends `held` with `{:could-not :find-her-sign-in :why :over}`.
   - Both line 355 (`A connection that was opened under the secret before stays open...`, printing `seen: {:why :over, :could-not :find-her-sign-in}`) and line 360 (`A session that is held by a connection is moved on...`) therefore fail — **no compliant implementation of `SPEC.md` can pass them as written**.
   - **Secondary defect at line 352 (found by breaker review):** `(line a renewed session)` connects to copy `a` (`:session-ms` = 24 hours), whose `caller-now` -> `moved-on!` immediately extends `session` from 4 seconds to 24 hours. When fixing lines 343–367, keep `session` renewals isolated to copy `c` (and replace the secret a second time through copy `a` on a separate session, or sleep 3s before replacing on `c` with shorter post-replace sleeps) so Check #3 genuinely proves `held`'s `standing` timer moved a 4-second session forward.

---

## 3. Open Design Call 1 — Where the counts of `ration` can live

Sid asked to lay out where the counts of `ration` can live (in one copy's memory vs in Auth by person, as `PROPOSAL.md` has it, since copies hold no state of their own), what each costs, and which I would take.

First, notice that **"the counts of `ration`" actually cover three things with different physics:**

1. **Pre-auth / unauthenticated edge limits:** Per-IP rate limiting on `/sign-in/*` (especially `POST /sign-in/google`, so forged RS256 tokens with fake `kid`s cannot trigger outbound HTTPS requests to Google's JWKS endpoint) and concurrent live-watch caps for unauthenticated visitors. There is **no person (`:root`)** yet.
2. **Per-person write pacing (`:send`, `:send-all`, `:invite`, `:make-agent`):** Verified `:root` exists; an account or agent fleet may hit `K` runtime copies concurrently.
3. **Concurrent live-watch caps (`:watch`, protecting Rama's `pstate.proxy.limit`):** A live watch is an ephemeral in-memory handle (`foreign-proxy-async`) owned by one specific JVM copy.

### Option A — In one copy's memory (process-local)
Token buckets by `:root` (for acts) and by `:remote-addr` (for `/sign-in/*`), plus live-watch counters by `:root` and for visitors, stored in atoms inside each copy's `Seam` / HTTP options (just as `standing.clj` holds per-process watches and `read_path.clj` holds `:counts` of open pushes).

- **Latency & Rama load:** Zero Rama round trips, zero RocksDB writes (~100ns atomic CAS). Stops floods and forged `/sign-in/google` tokens at the edge before touching Rama or Google.
- **Crash safety:** When a copy dies (`kill -9`), its memory dies with its sockets and Rama clears its dead proxies via heartbeat timeout. Live-watch counters can never leak permanently across crashes.
- **Multi-copy behavior (`K` copies):** Effective burst/rate ceiling and watch cap multiply by `K` unless requests are sticky-routed by root. A copy restart resets short windows (harmless for 1s/1m windows, weak for daily quotas).
- **Fit with `PROPOSAL.md`:** Keeps no *truth* outside Rama (losing a 1-second token bucket on crash loses nothing durable), but does hold ephemeral pacing state in copies rather than in Auth.

### Option B — In Auth, by person (literal reading of `PROPOSAL.md`)
Per-person pacing buckets and live-watch counts live in Auth's PStates partitioned by `:root` (not watched by `watch-root`).

- **Latency & Rama load:** Checking/incrementing in Auth on every act adds **1 full Rama depot append + RocksDB write** before every `door/send-act` — roughly doubling write-path round trips and cluster I/O on the hot path.
- **Crash safety (live watches):** A raw counter (`inc` on open, `dec` on close) **leaks permanently when a copy is `kill -9`'d**, locking a person out of opening watches until manual intervention. Making live-watch counts crash-safe in Auth requires **per-copy leases** (`{copy-id {:count n :expires-at t}}`) plus periodic heartbeats from every runtime copy — significant new Auth machinery.
- **Pre-auth blind spot:** Cannot protect `/sign-in/google` JWKS fetches or unauthenticated visitor watches at all (there is no person to partition by; writing unauthenticated IP events into Rama would turn an HTTP flood into a Rama cluster flood).
- **Multi-copy behavior (`K` copies):** Strictly enforces global per-person quotas and account-wide blocks across arbitrary `K` copies and 10k concurrent agent runs.

### Option C — Split by physics: edge & ephemeral handle caps in copy memory; global per-person quotas & blocks in Auth by person
1. **Unauthenticated edge guards (`/sign-in/*` per-IP pacing, visitor watch cap, frame/queue bounds):** Live in each copy's memory (and any reverse proxy in front). Zero Rama calls; stops forged tokens before Google JWKS fetches.
2. **Per-copy live-watch caps (`:max-watches-per-root`):** Live in each copy's memory beside `read_path.clj`'s own `:counts` (`:max-pushes-under-one-routing-id`). Dies cleanly on `kill -9`, zero lease machinery.
3. **Global per-person write pacing & account blocks:** Live in Auth by person:
   - **Account blocks** sit on Auth's existing watched `standing` table so a blocked root drops open connections across all `K` copies immediately with zero extra read.
   - **Global per-person write quotas** use **leased quota chunks**: a copy leases a small batch of `B` acts (e.g., `B = 50` for a 10s window) from Auth in 1 round trip and decrements locally until exhausted — amortizing Auth I/O by `50×` while bounding cross-copy overshoot to at most `(K - 1) × B`, with no lease-leak risk on crash (an unused chunk simply expires with the window).

### Recommendation (which I would take)
**Take Option C as the target architecture — and start with Option A's copy-local layer for `/sign-in/*` IP pacing and visitor/root watch caps, adding Auth's leased write chunks + watched block once real `src-auth` lands and we run multiple copies with agent fleets.**

Pure Option B (one Auth write per act, raw watch counters in Auth, no local edge guard) fails on three engineering grounds: it doubles Rama write I/O on the hot path, leaks watch counts on `kill -9` unless Auth grows copy-lease heartbeats, and leaves `/sign-in/google` JWKS spam completely unguarded because there is no person yet. Option C honors `PROPOSAL.md`'s vision (global per-person standing, blocks, and quotas live in Auth by person; no durable truth outside Rama) while respecting the physics of unauthenticated edge traffic and crash-ephemeral socket handles.

---

## 4. Open Design Call 2 — Which word of the wire should carry `:driven-by`

`spine/auth/SPEC.md` (`37bc0e01`) adds optional `:driven-by` (`{:kind :person}` or `{:kind :model :model text}`, up to 256 chars) to `open-session`, persists it on the session, returns it from `who-is` even after close, and forbids changing it on repeat `open-session`.

**Why none of the existing 15 wire words (`src-wire/softland/wire/words.clj`) can carry `:driven-by`:**
- Today, **no wire word opens a session.** `auth/open-session` is called only implicitly during WebSocket upgrade in `http/accept` (when no `softland-session` header/query is sent) and on `GET /sign-in/name` (browser name box).
- `:driven-by` is fixed at `open-session` time and cannot be changed afterward, so carrying it on `:send`, `:read`, or `:who-am-i` after the connection's session is already open would contradict `SPEC.md`.
- `:driven-by` is a property of a **session**, not an actor (`PROPOSAL.md`, entry 8: *"that 'a model drives this run' is said of the session and is not a kind of who"* — one person or one agent can run both person-driven and model-driven sessions, or switch models between runs), so carrying it on `:make-agent` is wrong.

### Recommendation: add a new wire word `{:word :open-session :driven-by D}`
Add `:open-session` to `src-wire/softland/wire/words.clj`, mirroring the existing `:close-session`:

```clojure
{:word :open-session :driven-by {:kind :person}}
{:word :open-session :driven-by {:kind :model :model "claude-opus-4-6"}}
{:word :open-session} ;; :driven-by optional; absent means not said
```

- **Why this word:**
  - Every other Auth lifecycle operation already has a wire word (`:replace-secret`, `:sign-out`, `:close-session`, `:invite`, `:make-agent`, `:end-agent`, `:under`, `:end-sign-in`). `:open-session` is the sole missing mirror of `:close-session`.
  - Structured EDN (`{:kind :model :model "..."}`) is validated cleanly by `words/check` alongside every other word — no ad-hoc HTTP header encoding.
  - Fits `softland.command.main`'s `new-session` flow and Piece 6 MCP / multi-run harnesses naturally: a signed-in caller opens a session with `:driven-by`, gets `{:open S :until t :driven-by D}`, and attaches a line with `softland-session: S` (or opens a session-less control connection to mint run sessions).
  - Browser faces (`GET /sign-in/name`, and browser WebSocket connections via cookie when no session is named) pass `{:kind :person}` directly inside `http.clj` -> `seam/caller`, since the browser face knows a human clicked the page.
- **Alternative (tradeoff):** Carry `:driven-by` on the `/line` WebSocket upgrade handshake (`http/accept`, beside `softland-session`, e.g. header `softland-driven-by`). Saves one round trip on first connect, at the cost of parsing EDN (or splitting across two headers) outside `words/check` and preventing an already-connected harness from opening a new typed session without opening a new socket. Both can coexist if zero-RTT first connect matters later, with `:open-session` as the canonical wire word.

---

## 5. Diff Summary & Session Choices (for Sid to turn)

### Diff against `/mnt/data/projects/research/softland/runtime-spine-2026-11-21/as-written-2026-11-22`
```text
src-runtime/softland/runtime/google.clj      |  7 ++++++-
src-runtime/softland/runtime/http.clj        | 47 +++++++++++++++-------
src-runtime/softland/runtime/jetty.clj       | 42 ++++++++++++----------
src-runtime/softland/runtime/line.clj        | 68 +++++++++++++++-------
src-runtime/softland/runtime/main.clj        |  3 +++
src-runtime/softland/runtime/seam.clj        | 85 ++++++++++++++--------
src-runtime/softland/runtime/standing.clj    |  4 +++-
src-command/softland/command/client.clj      | 74 +++++++++++++++-------
src-command/softland/command/main.clj        |107 +++++++++++++--------
test-runtime/softland/runtime/command_proof.clj | 18 ++++++++++++++++--
```
- `deps.edn` vs `HEAD` (`01c6ea70`): `+2/-2` (`"src-command"` added to `:runtime`, nonexistent `"test-command"` removed from `:command`).
- `test-runtime/softland/runtime/checks.clj`, `proof.clj`, `pure_proof.clj`, `cluster_proof.clj`, `drive.clj`: **0 bytes diff** vs `as-written-2026-11-22`.
- New non-production test shim (`src/proposal/runtime-2026-11-21/spine/runtime-oracle/`): 3 files, 880 lines (`module.clj`, `client.clj`, `oracle_proof.clj`). Explicitly non-production; remove or ignore once real `src-auth/` lands.

### Session choices (each Sid's to turn)
1. **Opt-in `-Dsoftland.store.path` isolation in `command_proof.clj`:** Added solely to honor Rule 2 (never loading live working-tree `src-store/` in `:command` subprocesses while another session may edit it). When unset, `command_proof.clj` runs plain `clojure -M:command` verbatim; zero assertions changed.
2. **Pre-existing breaker fixes landed tonight:** Landed all 11 HIGH and top MEDIUM correctness/security fixes found by the fresh breaker review across `src-runtime/` and `src-command/` (`standing.clj` fail-closed watch opening, `seam.clj` query `:session` check / rotatable `Caller.hash` / interrupted `send-all` / `:under` clock filter / non-blocking `standing`, `line.clj` worker interrupt + tokenized watch IDs, `jetty.clj` `sessions` leak fix + virtual-thread pings, `http.clj` `POST /yes` race + `GET /yes` expiry + malformed cookie 401 + `:says` key bound, `google.clj` & `command/client.clj` shared `HttpClient` + synchronized WS sends, `command/main.clj` atomic mode-0600 `keep!` + safe `new-session`).
3. **`runtime-oracle/` kept in `spine/runtime-oracle/`:** Kept committed as an explicitly documented non-production test shim (never on `:runtime`'s default classpath in `deps.edn`) so anyone can reproduce tonight's oracle-phase proofs until real `src-auth/` lands.
4. **Standalone `zoo.cfg` + `127.0.0.1` `rama.yaml`:** Left ready under `/mnt/data/projects/research/softland/runtime-spine-2026-11-21/cluster/` with the Rama 1.6.0 wildcard-bind bytecode receipts documented above; no cluster daemons left running.

---

## 6. Afternoon Pass (22 November 2026) — Reference Auth, Review Faults, `:open-session`, First Ration Layer, and Real Auth

### Step 1: Thin adapter onto `softland.auth.reference`

Replaced the stand-in rules in `src/proposal/runtime-2026-11-21/spine/runtime-oracle/src/softland/auth/client.clj` with a thin adapter that forwards every call unchanged to `softland.auth.reference` (adding, dropping, and loosening nothing) and delivers `watch-root` callbacks asynchronously in strict change order on a dedicated single-thread daemon executor per client. Moved the replaced stand-in `client.clj` and `oracle_proof.clj` to `src/proposal/runtime-2026-11-21/spine/runtime-oracle/set-aside/` (nothing deleted).

**What was run:**
```text
flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \
  clojure -Sdeps '{:aliases {:auth {:replace-paths ["src/proposal/runtime-2026-11-21/spine/runtime-oracle/src" "test-auth"]}}}' \
  -M:auth -e '... async watch ordering check + softland.auth.module-check (seed 42, 200 histories) ...'
```

**Output:**
```text
PASSED watch-root callbacks arrived on another thread in exact order: [0 1 2 3 4 5]
Seed of the histories: 42
The module: runtime-oracle adapter over softland.auth.reference
PASSED The module, runtime-oracle adapter over softland.auth.reference, gave what the reference gives, through the contract alone, in 200 generated histories of 16234 steps: every result of 14292 writes (1718 of them repeats, 866 whose input does not fit), every one of 108725 reads of who-is, ask-of and under, and what each of 562 watches came to after each of the 824 changes under its root; with 50 deep segments of 32 agents in a line, 52 times :too-deep, and 0 calls sent again after a :could-not that said nothing.
(The module's histories took 41.0 s.)
PASSED open of a name that the cluster does not hold, and of a thing that is no cluster, gives {:could-not :find-the-module}.
PASSED After close!, each of the sixteen calls on that client gives {:could-not :closed}, watch-root's in place of the function; close! gives nil, also again; no function that it watched with is called again; and the other client goes on.

3 passed, 0 failed.
```

### Step 2: Runtime proofs on `softland.auth.reference`

All three runtime proofs passed whole on the reference adapter without changing any check. Auditing every `softland.auth.client/*` call in `src-runtime/` against `softland.auth.reference`'s strict forms found one locale mismatch: email lower-casing used `clojure.string/lower-case` (JVM default locale) instead of `Locale/ROOT` as required by `spine/auth/SPEC.md` ("an email that Java's `toLowerCase`, in the root locale, would change does not fit"). Fixed in:
- `src-runtime/softland/runtime/seam.clj` (`:invite` email claim)
- `src-runtime/softland/runtime/sign_in.clj` (`enter!` email and `setup!` email)

**What was run (after the `Locale/ROOT` fix):**
```text
SDEPS='{:aliases {:runtime {:replace-paths ["/mnt/data/projects/research/softland/runtime-spine-2026-11-21/store-at-head/src-store" "src/proposal/runtime-2026-11-21/spine/runtime-oracle/src" "test-auth" "src-wire" "src-runtime" "src-command" "test-wire" "test-runtime"]}}}'
LOCK=/mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock
clojure -Sdeps "$SDEPS" -M:runtime -m softland.runtime.pure-proof
flock "$LOCK" clojure -Sdeps "$SDEPS" -M:runtime -m softland.runtime.proof
flock "$LOCK" clojure -Sdeps "$SDEPS" -J-Dsoftland.store.path=/mnt/data/projects/research/softland/runtime-spine-2026-11-21/store-at-head/src-store -M:runtime -m softland.runtime.command-proof
```

**Output:**
```text
softland.runtime.pure-proof:    22 passed, 0 failed. (exit 0)
softland.runtime.proof:         49 passed, 0 failed. (exit 0)
softland.runtime.command-proof: 16 passed, 0 failed. (exit 0)
```

### Step 3: The five faults from `REVIEW-runtime-spine.md`

Carbon's commit (`Runtime: the attacks as checks that fail today`) had not landed yet when Step 3 began, so wrote sentence checks in `test-runtime/softland/runtime/review_faults_proof.clj`, confirmed all 6 checks failed before any edit (**0 passed, 6 failed**, including reproducing the JDK 21 virtual-thread starvation hang `{:vthread :hung, :health :hung}`), then fixed all five faults:
1. **Non-blocking pings and non-pinning locks (`jetty.clj`, `line.clj`, `seam.clj`):** Removed `ping-exec` virtual-thread-per-ping dispatch; the single ping thread now calls Jetty's non-blocking `(.sendPing (.getRemote session) (ByteBuffer/allocate 0) noop-write-callback)` without locking the connection. Replaced `synchronized`/`locking` monitors with `ReentrantLock` in `line.clj` (`say!`) and `seam.clj` (`standing` and `watch`) so virtual threads do not pin carriers on JDK 21.
2. **Yes page (`http.clj`):** `POST /yes` now compares what she chose (as herself vs as an agent, plus checking the standing agent's label via `:under`) against what stands in Auth. A second yes naming another actor returns 409 (never `"Done"`). A person who says no after her own yes (or a mismatched second yes) is told with 409 that the program is signed in as her (or as an agent of hers, with the sign-in id); another person is only told `"This program was already answered before."`
3. **Closed session renewal backstop (`seam.clj`):** `moved-on!` now returns the `:until` time it held (written or not) instead of trusting a repeat `open-session`'s `{:open S}` as extended, so the next `standing` look fires within 1 second and reads the real standing via `who-is`.
4. **Failing standing flow (`line.clj`, `seam.clj`):** Both the completion and error callbacks of `(m/reduce ... (seam/standing ...))` in `line/open` now fail closed by sending `{:bye {:could-not :reach-auth ...}}` and ending the connection if it is still open; `seam/standing` also catches throws inside `check!` and executor dispatch and emits `:reach-auth`.
5. **Lost secret replacement answer (`src-command/softland/command/main.clj`):** Persists the new credential as `:next-credential` in `sign-in.edn` *before* sending `:replace-secret`, reuses `:next-credential` on retries, promotes it to `:credential` (removing `:next-credential`) on `:replaced` or when a subsequent connection succeeds with `:next-credential`, falls back to `:credential` on 401, and drops `:next-credential` only on a definitive Auth refusal (`:take-that`).

**What was run (before fixes -> after fixes):**
```text
flock "$LOCK" clojure -Sdeps "$SDEPS" -M:runtime -m softland.runtime.review-faults-proof
clojure -Sdeps "$SDEPS" -M:runtime -m softland.runtime.pure-proof
flock "$LOCK" clojure -Sdeps "$SDEPS" -M:runtime -m softland.runtime.proof
flock "$LOCK" clojure -Sdeps "$SDEPS" -J-Dsoftland.store.path=.../store-at-head/src-store -M:runtime -m softland.runtime.command-proof
```

**Output:**
```text
Before fixes:
  softland.runtime.review-faults-proof: 0 passed, 6 failed. (exit 1)
After fixes:
  softland.runtime.review-faults-proof: 6 passed, 0 failed. (exit 0)
  softland.runtime.pure-proof:          22 passed, 0 failed. (exit 0)
  softland.runtime.proof:               49 passed, 0 failed. (exit 0)
  softland.runtime.command-proof:       16 passed, 0 failed. (exit 0)
```

### Step 4: The wire's word for a session (`{:word :open-session :driven-by D}`)

Wrote sentence checks first in `test-runtime/softland/runtime/open_session_proof.clj`, saw all 5 checks fail before implementation (**0 passed, 5 failed**), then implemented:
- `src-wire/softland/wire/words.clj`: added `{:word :open-session :driven-by D}` (where `:driven-by` is optional, nil, `{:kind :person}`, or `{:kind :model :model text}` with `1..256` characters via `words/text?`, refusing invalid forms with `:driven-by-has-no-form` at `:driven-by`). Added checks in `test-wire/softland/wire/pure_proof.clj`.
- `src-runtime/softland/runtime/seam.clj`: implemented `:open-session` in `call-now` (calling `auth/open-session` and returning `{:open session :until until :driven-by driven-by}`); added `driven-by` to `Caller` and `about` (populated from `auth/who-is`, including after `new-session?` opens a session, and included on `about` / `:hello` / `:who-am-i` whenever the caller holds a session).
- `src-runtime/softland/runtime/http.clj`: browser faces (`of-cookie` with `:new` on `GET /sign-in/name`, and `accept` for `:cookie` WebSocket connections opening a new session) now pass `:driven-by {:kind :person}`.
- `src-command/softland/command/main.clj`: extended `new-session` with `--model <name>`, which opens a new session through `{:word :open-session :driven-by {:kind :model :model <name>}}`, closes the previous session, persists the new session id, and prints `hello` (with `:driven-by` read back from `who-is`).

**What was run (before -> after):**
```text
flock "$LOCK" clojure -Sdeps "$SDEPS" -J-Dsoftland.store.path=.../store-at-head/src-store -M:runtime -m softland.runtime.open-session-proof
clojure -Sdeps "$SDEPS_WIRE" -M:wire -m softland.wire.pure-proof
clojure -Sdeps "$SDEPS" -M:runtime -m softland.runtime.pure-proof
flock "$LOCK" clojure -Sdeps "$SDEPS" -M:runtime -m softland.runtime.proof
flock "$LOCK" clojure -Sdeps "$SDEPS" -J-Dsoftland.store.path=.../store-at-head/src-store -M:runtime -m softland.runtime.command-proof
```

**Output:**
```text
Before implementation:
  softland.runtime.open-session-proof:  0 passed, 5 failed. (exit 1)
After implementation:
  softland.wire.pure-proof:            38 passed, 0 failed. (exit 0)
  softland.runtime.pure-proof:         22 passed, 0 failed. (exit 0)
  softland.runtime.open-session-proof:  5 passed, 0 failed. (exit 0)
  softland.runtime.proof:              49 passed, 0 failed. (exit 0)
  softland.runtime.command-proof:      16 passed, 0 failed. (exit 0)
```

### Step 5: First layer of `ration` in copy memory + carbon's seven attacks

Carbon's attack suite committed as `402efee6` (`Runtime: the attacks as checks that fail today`, under `test-runtime/softland/runtime/attacks/`, never edited). Ran all seven attacks against `402efee6` (including re-running the heavier `slow-readers` in `402efee6`):
- `yes-page`, `closed-session`, and `lost-replacement` already passed from Step 3.
- `unwatched-standing` caught one more edge case in `standing.clj`: when `auth/watch-root` throws inside `(delay ...)`, `(force (:opening entry))` threw past eviction, leaving a poisoned `entry` in `roots` that failed subsequent connections for the same root. Fixed `standing.clj` to evict the entry (`pushed! standing root entry not-reached`) on throw before rethrowing.
- `sign-in-pace` and `watch-caps` failed before Step 5 as expected, naming the four guard keys in `softland.runtime.attacks.numbers`.

Implemented the first layer of `ration` in each copy's memory (the first two parts of Option C; per-person counts in Auth and blocks are not built now):
- `src-wire/softland/wire/words.clj`: added `:take-more` to `words/could-nots` (HTTP status `429`, `:why` `:sign-in-paced` or `:too-many-watches`).
- `src-runtime/softland/runtime/seam.clj`:
  - Added carbon's four keys and defaults in `default-numbers` (all in one place, marked for Sid to turn):
    - `:sign-in-window-ms 10000`
    - `:max-sign-in-posts-per-address 30`
    - `:max-watches-per-root 256`
    - `:max-visitor-watches 64`
  - Added per-copy `ration-state` (`{:sign-in (atom {}) :watches (atom {})}`) on `Seam` (isolated per copy in memory).
  - Implemented `ration-sign-in!` (bounded sliding-window per remote address using `System/nanoTime`, capping tracked addresses at `10000` to prevent memory growth under distributed floods).
  - Implemented atomic `acquire-watch!` / `release-watch!` in `seam/watch` (keyed by root UUID for signed-in callers — sharing one cap across a person and all her agents across all connections on that copy — and `:visitor` for unauthenticated callers), releasing the slot exactly once on `:leave`, connection close, read-path `:ended`, or open failure.
- `src-runtime/softland/runtime/http.clj`: paced all `POST /sign-in/*` requests via `seam/ration-sign-in!` before any call to Google (`google/fetch-keys`) or Auth, returning HTTP `429` (EDN for `/sign-in/program*`, HTML page for browser sign-in routes).
- `src-runtime/softland/runtime/main.clj`: wired CLI flags `--sign-in-window-ms`, `--max-sign-in-posts-per-address`, `--max-watches-per-root`, and `--max-visitor-watches`.

**What was run:**
```text
clojure -Sdeps "$SDEPS_WIRE" -M:wire -m softland.wire.pure-proof
clojure -Sdeps "$SDEPS" -M:runtime -m softland.runtime.pure-proof
for ns in \
  softland.runtime.attacks.slow-readers \
  softland.runtime.attacks.yes-page \
  softland.runtime.attacks.closed-session \
  softland.runtime.attacks.unwatched-standing \
  softland.runtime.attacks.lost-replacement \
  softland.runtime.attacks.sign-in-pace \
  softland.runtime.attacks.watch-caps \
  softland.runtime.proof \
  softland.runtime.command-proof; do
  flock "$LOCK" clojure -Sdeps "$SDEPS" "$STORE_PROP" -M:runtime -m "$ns"
done
```

**Output:**
```text
softland.wire.pure-proof:                     38 passed, 0 failed. (exit 0)
softland.runtime.pure-proof:                  22 passed, 0 failed. (exit 0)
softland.runtime.attacks.slow-readers:         1 passed, 0 failed. (exit 0)
softland.runtime.attacks.yes-page:             1 passed, 0 failed. (exit 0)
softland.runtime.attacks.closed-session:       1 passed, 0 failed. (exit 0)
softland.runtime.attacks.unwatched-standing:   1 passed, 0 failed. (exit 0)
softland.runtime.attacks.lost-replacement:     1 passed, 0 failed. (exit 0)
softland.runtime.attacks.sign-in-pace:         1 passed, 0 failed. (exit 0)
softland.runtime.attacks.watch-caps:           1 passed, 0 failed. (exit 0)
softland.runtime.proof:                       49 passed, 0 failed. (exit 0)
softland.runtime.command-proof:               16 passed, 0 failed. (exit 0)
```
