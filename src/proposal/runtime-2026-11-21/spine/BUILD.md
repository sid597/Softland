# The spine (Piece 4): build report, proof findings, and open calls

**22 November 2026.** Written after the overnight integration, oracle-phase bring-up, and adversarial breaker review of Piece 4 (`src-runtime/`, `src-command/`, `test-runtime/`, `deps.edn`, and the non-production test shim `spine/runtime-oracle/`).

Everything below reflects Sid's three mid-run changes of 22 November (`02:24:59`):

1. **Step 4 skipped:** No real `src-auth/` lands tonight (Session 1 is not running). Did not wait for it; did not build `auth.jar`, run `softland.runtime.cluster-proof`, or run the persistent-cluster Piece 1 + Piece 4 Join proof.
2. **Step 3 — no guard code written:** `seam.clj/ration` remains a no-op stub and `http.clj` carries no `/sign-in/*` pacing code. The design options and tradeoffs for where the counts of `ration` live are laid out [below](#where-the-counts-of-ration-can-live) for Sid to decide in the morning.
3. **`:driven-by` in the stand-in, not on the wire:** The non-production Auth oracle shim (`spine/runtime-oracle/`) implements optional `:driven-by` on `open-session` and `who-is` per `spine/auth/SPEC.md` (`37bc0e01`). The wire (`src-wire/`) was not touched; which word of the wire should carry `:driven-by` is analyzed [below](#which-word-of-the-wire-should-carry-driven-by).
4. **Proof immutability rule honored:** No check or assertion in `test-runtime/` was modified to make a proof pass. The five failures in `test-runtime/softland/runtime/checks.clj` are documented verbatim [below](#proof-findings-in-unmodified-checksclj). `/mnt/data/projects/research/softland/runtime-spine-2026-11-21/as-written-2026-11-22` was never modified. *(Since repaired: `a1cb405e`, "Runtime: the two defects in its own checks repaired", made `checks.clj` pass whole; `softland.runtime.proof` passes 49 of 49 in §6 and §8.)*

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

*(The state of the night of 22 November. Both defects were repaired in `a1cb405e`, and `checks.clj` passes whole since: §6, §8.)*

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

### Step 6: The runtime on real Auth (`7a381f3a`)

Waited for `7a381f3a` (`Auth: the module and its client, held against the reference`) in `git log -- src-auth`, then ran the full suite against real `src-auth` in place of `runtime-oracle` / `test-auth`:

#### 6a. Runtime proofs, new proofs, and carbon's seven attacks on real `src-auth` (IPC)

**What was run:**
```text
LOCK=/mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock
SDEPS_REAL='{:aliases {:runtime {:replace-paths ["/mnt/data/projects/research/softland/runtime-spine-2026-11-21/store-at-head/src-store" "src-auth" "src-wire" "src-runtime" "src-command" "test-wire" "test-runtime"]}}}'
STORE_PROP="-J-Dsoftland.store.path=/mnt/data/projects/research/softland/runtime-spine-2026-11-21/store-at-head/src-store"

clojure -Sdeps "$SDEPS_REAL" -M:runtime -m softland.runtime.pure-proof
for ns in \
  softland.runtime.proof \
  softland.runtime.command-proof \
  softland.runtime.review-faults-proof \
  softland.runtime.open-session-proof \
  softland.runtime.attacks.slow-readers \
  softland.runtime.attacks.yes-page \
  softland.runtime.attacks.closed-session \
  softland.runtime.attacks.unwatched-standing \
  softland.runtime.attacks.lost-replacement \
  softland.runtime.attacks.sign-in-pace \
  softland.runtime.attacks.watch-caps; do
  flock "$LOCK" clojure -Sdeps "$SDEPS_REAL" "$STORE_PROP" -M:runtime -m "$ns"
done
```

**Output:**
```text
softland.runtime.pure-proof:                  22 passed, 0 failed. (exit 0)
softland.runtime.proof:                       49 passed, 0 failed. (exit 0)
softland.runtime.command-proof:               16 passed, 0 failed. (exit 0)
softland.runtime.review-faults-proof:          6 passed, 0 failed. (exit 0)
softland.runtime.open-session-proof:           5 passed, 0 failed. (exit 0)
softland.runtime.attacks.slow-readers:         1 passed, 0 failed. (exit 0)
softland.runtime.attacks.yes-page:             1 passed, 0 failed. (exit 0)
softland.runtime.attacks.closed-session:       1 passed, 0 failed. (exit 0)
softland.runtime.attacks.unwatched-standing:   1 passed, 0 failed. (exit 0)
softland.runtime.attacks.lost-replacement:     1 passed, 0 failed. (exit 0)
softland.runtime.attacks.sign-in-pace:         1 passed, 0 failed. (exit 0)
softland.runtime.attacks.watch-caps:           1 passed, 0 failed. (exit 0)
```

#### 6b. Proof cluster JARs, open ports, `cluster-proof`, `worker-kill-proof`, and `two-lines-proof`

Built both module JARs under `/mnt/data/projects/research/softland/runtime-spine-2026-11-21/build/`:
- `store.jar` (`255K`), compiled from `/mnt/data/projects/research/softland/runtime-spine-2026-11-21/store-at-head/src-store`
- `auth.jar` (`31K`), compiled from `src-auth`

Started the Rama 1.6.0 proof cluster behind `flock cluster.lock` using standalone `zoo.cfg` (bound to `127.0.0.1:2101`), `rama.yaml` (`conductor.host: 127.0.0.1`, `supervisor.port.range: 13000-13009`), and deployed `softland.store.module/Store` and `softland.auth.module/Auth` (`--tasks 4 --threads 2 --workers 1 --replicationFactor 1`).

**Ports open while the proof cluster runs (`ss -tln`):**
```text
LISTEN 0      50                      *:1983             *:*   (Conductor Thrift — wildcard)
LISTEN 0      50                      *:8898             *:*   (Conductor Admin UI — wildcard)
LISTEN 0      50     [::ffff:127.0.0.1]:2101             *:*   (Standalone ZooKeeper — loopback only)
LISTEN 0      50                      *:13000            *:*   (Supervisor HTTP — wildcard)
LISTEN 0      4096                    *:13001            *:*   (Store worker Netty — wildcard)
LISTEN 0      4096                    *:13002            *:*   (Auth worker Netty — wildcard)
```
Every cluster daemon (ZooKeeper, Conductor, Supervisor, Store worker, Auth worker) is stopped immediately after each run (`trap cleanup EXIT`), verified by `ss -tln` printing `(none listening)` across `1983, 2101, 8898, 13000-13009` and zero matching processes in `ps`.

**1) Unmodified `softland.runtime.cluster-proof`:**
Ran `test-runtime/softland/runtime/cluster_proof.clj` untouched (with `java.lang.ProcessHandle` pre-imported in `-e` so the file compiles without editing it):
```text
flock "$LOCK" ... clojure -Sdeps "$SDEPS_REAL" -M:runtime -e \
  '(in-ns (quote softland.runtime.cluster-proof)) (import (quote [java.lang ProcessHandle])) (in-ns (quote user)) (require (quote softland.runtime.cluster-proof)) (softland.runtime.cluster-proof/-main "--conductor" "127.0.0.1" "--conductor-port" "1983" "--cluster-root" ".../cluster")'
```
Output: **`55 passed, 4 failed`** (exit 1).
- **Passed (55):** all 3 runtime-copy process starts; all 44 `checks.clj` checks across 3 separate runtime JVM processes; all 4 `runtime-killed!` checks (killing a runtime JVM mid-act, reconnecting on another copy in the same session, retrying the act under its UUID, restarting a fresh copy); 2 of 3 `auth-worker-killed!` checks; the **6,000 simultaneously watched roots** check on real `Auth` (`pstate.proxy.limit` 20,000, bypassing Rama's default 1,000-per-task bound); and ran to its end without a throw.
- **Failed (4) — three pre-existing defects in unmodified `cluster_proof.clj` (not edited per path boundary):**
  1. `cluster_proof.clj:53`: omits `java.lang.ProcessHandle` from `:import`, causing a compile error (`Unable to resolve classname: ProcessHandle` at line 116) unless pre-imported.
  2. `cluster_proof.clj:110-121` (`worker-of`): inspects `(.arguments (.info ph))` via `ProcessHandle.Info.arguments()`. On Linux OpenJDK 21, `ProcessHandle.Info.arguments()` returns `Optional.empty()` when `/proc/<pid>/cmdline` exceeds ~4 KiB; Rama 1.6.0 worker processes have ~15 KiB `-cp` command lines, so `worker-of` never locates either worker (`{:killed nil, :back nil}`).
  3. `cluster_proof.clj:191` (`store-worker-killed!`): writes vector entity `[:tile n]`, which `softland.store.act` rejects (entities must be keywords or UUIDs), causing all 40 acts to be refused by the Store gate (`:accepted 0`, `:stamps 1`).

**2) Real Store & Auth worker SIGKILL proof (`test-runtime/softland/runtime/worker_kill_proof.clj`, new file):**
Added a standalone proof reading `/proc/<pid>/cmdline` directly and using valid keyword entities (`:worker-kill/tile-N`), so both real worker kills are verified without touching `cluster_proof.clj`:
```text
flock "$LOCK" ... clojure -Sdeps "$SDEPS_REAL" -M:runtime -m softland.runtime.worker-kill-proof \
  --conductor 127.0.0.1 --conductor-port 1983 --cluster-root .../cluster
```
Output:
```text
PASSED Runtime copy process starts on the proof cluster for worker-kill checks.
PASSED Rama's worker of the store's module is killed while a series of acts is on its way, and the supervisor starts it again.
PASSED After Rama's store worker is killed mid-series, retrying each unanswered act under its UUID accepts all forty acts.
PASSED Each of the forty acts was taken once across the store worker kill: forty distinct stamps, and a second sending reads the same forty answers.
PASSED Rama's worker of Auth is killed while a connection is held, and the supervisor starts it again.
PASSED When Auth's worker is killed, a held connection is not left unwatched: it is told :reach-auth and ends, or its watch recovered and answers words.
PASSED When Auth's worker is back, the same credential is let in, in the same session: nothing durable in Auth was lost with the killed worker.

7 passed, 0 failed. (exit 0)
```

**3) Where the two lines meet (`test-runtime/softland/runtime/two_lines_proof.clj`, new file):**
Proves on the real proof cluster with two separate runtime JVM processes that a watch open on both copies closes off its layer (`:anew` with `:left-out [{:layer L :why :may-not-see}]`, `:place {:holds :no-fact}`, zero subsequent writes, and gate refusal `:permission-ended`) when its permission in the store is ended — directly, through a chain permission above it, or for a run-scoped permission:
```text
flock "$LOCK" ... clojure -Sdeps "$SDEPS_REAL" -M:runtime -m softland.runtime.two-lines-proof \
  --conductor 127.0.0.1 --conductor-port 1983
```
Output:
```text
PASSED Two runtime processes start on the cluster for the two-lines proof.
PASSED A watch open on two copies of the runtime sees live changes while its permission stands, closes off the layer with :may-not-see on both copies when its permission is ended in the store, receives no subsequent writes, and acts under the ended permission are refused.
PASSED When a permission above a watcher's own ends in the store, the watcher's open watches on both copies of the runtime close off the layer and receive no further writes.
PASSED A watch held under a run-scoped permission on two copies of the runtime closes off the layer on both copies when that run permission ends in the store, and receives no further writes.

4 passed, 0 failed. (exit 0)
```

---

## 7. Summary of the 22 November Pass (for Sid)

### What changed

Six commits on `main` (each staged strictly by name; `test-runtime/softland/runtime/checks.clj`, `cluster_proof.clj`, `attacks/*`, `test-auth/`, `src-auth/`, `src-store/`, `spine/auth/`, `deps.edn`, `as-written-2026-11-22/`, and `store-at-head/` untouched):

1. **`ee2e92a5` — Step 1 (`Runtime oracle: adapt Auth client onto softland.auth.reference`):**
   - `spine/runtime-oracle/src/softland/auth/client.clj`: replaced stand-in rules with thin adapter forwarding unchanged to `softland.auth.reference`, delivering `watch-root` callbacks asynchronously in order on a dedicated daemon thread per client.
   - Moved superseded `client.clj` and `oracle_proof.clj` to `spine/runtime-oracle/set-aside/` (nothing deleted).
2. **`d8ee975c` — Step 2 (`Runtime: pass proofs on Auth reference and use Locale/ROOT for emails`):**
   - `src-runtime/softland/runtime/seam.clj`, `sign_in.clj`: lower-case emails with `Locale/ROOT` to match Auth's contract.
3. **`35aef211` — Step 3 (`Runtime: fix the five faults named in the spine review`):**
   - `src-runtime/softland/runtime/jetty.clj`, `line.clj`, `seam.clj`: non-blocking Jetty `sendPing` on the single ping thread; `ReentrantLock` in place of `synchronized`/`locking` monitors in `say!`, `standing`, and `watch`.
   - `src-runtime/softland/runtime/http.clj`: `POST /yes` compares chosen actor (including agent label via `:under`) against standing actor; returns 409 on actor mismatch and tells a person who says no after her own yes that the program is signed in as her (or her agent).
   - `src-runtime/softland/runtime/seam.clj`: `moved-on!` returns held `:until` so `standing` re-reads via `who-is` within 1s instead of extending a closed session on `{:open S}`.
   - `src-runtime/softland/runtime/line.clj`, `seam.clj`: failing `standing` flow fails closed (`:reach-auth` + `end!`).
   - `src-command/softland/command/main.clj`: persists `:next-credential` before `:replace-secret`, tries it first on connect, reuses it on retry, drops it only on promotion or definitive `:take-that`.
   - New test: `test-runtime/softland/runtime/review_faults_proof.clj`.
4. **`755b7b6e` — Step 4 (`Wire & runtime: add :open-session word and :driven-by support`):**
   - `src-wire/softland/wire/words.clj`, `test-wire/softland/wire/pure_proof.clj`: added `{:word :open-session :driven-by D}` and `driven-by?` validation.
   - `src-runtime/softland/runtime/seam.clj`, `http.clj`: handled `:open-session`, populated immutable `:driven-by` from `who-is` on `Caller`/`about`, browser faces pass `{:kind :person}`.
   - `src-command/softland/command/main.clj`: `new-session --model <name>` opens a model-driven session via `:open-session`.
   - New test: `test-runtime/softland/runtime/open_session_proof.clj`.
5. **`f7163f93` — Step 5 (`Runtime: add copy-local sign-in pacing and live-watch caps, pass all seven attacks`):**
   - `src-runtime/softland/runtime/standing.clj`: evict poisoned `delay` entry when `auth/watch-root` throws on open (found by carbon's `attacks.unwatched-standing`).
   - `src-wire/softland/wire/words.clj`: added `:take-more` (HTTP 429).
   - `src-runtime/softland/runtime/seam.clj`, `http.clj`, `main.clj`: copy-local sliding-window pacing on `POST /sign-in/*` before Google/Auth, atomic per-root and visitor live-watch caps using carbon's number keys, CLI flags for all four numbers.
6. **Step 6 commit (this commit):**
   - New tests: `test-runtime/softland/runtime/two_lines_proof.clj`, `test-runtime/softland/runtime/worker_kill_proof.clj`.
   - `src/proposal/runtime-2026-11-21/spine/BUILD.md`: Steps 6a–6b and this summary.
   - Outside repo: `/mnt/data/projects/research/softland/runtime-spine-2026-11-21/build/{store.jar,auth.jar}` built; all cluster daemons stopped.

### Each command with its output

| Step | Command / target | Result |
|---|---|---|
| 1 | `softland.auth.module-check` (seed 42, 200 histories) + async watch order on `runtime-oracle` adapter | **3 passed, 0 failed** (exit 0) |
| 2 | `runtime.pure-proof` / `runtime.proof` / `runtime.command-proof` on reference adapter | **22/22**, **49/49**, **16/16** (exit 0) |
| 3 | `runtime.review-faults-proof` before fixes -> after fixes | **0/6** (exit 1) -> **6/6** (exit 0) |
| 3 | `runtime.pure-proof` / `proof` / `command-proof` on reference after fixes | **22/22**, **49/49**, **16/16** (exit 0) |
| 4 | `runtime.open-session-proof` before impl -> after impl | **0/5** (exit 1) -> **5/5** (exit 0) |
| 4 | `wire.pure-proof` / `runtime.pure-proof` / `proof` / `command-proof` on reference | **38/38**, **22/22**, **49/49**, **16/16** (exit 0) |
| 5 | All 7 carbon attacks (`402efee6`) on reference: `slow-readers`, `yes-page`, `closed-session`, `unwatched-standing`, `lost-replacement`, `sign-in-pace`, `watch-caps` | **1/1** each (7/7, exit 0) |
| 5 | `wire.pure-proof` / `runtime.pure-proof` / `proof` / `command-proof` on reference | **38/38**, **22/22**, **49/49**, **16/16** (exit 0) |
| 6a | `runtime.pure-proof` / `proof` / `command-proof` on **real `src-auth`** (`7a381f3a`) | **22/22**, **49/49**, **16/16** (exit 0) |
| 6a | `review-faults-proof`, `open-session-proof`, and all 7 carbon attacks on **real `src-auth`** | **6/6**, **5/5**, **7×(1/1)** (exit 0) |
| 6b | Unmodified `softland.runtime.cluster-proof` on proof cluster (`store.jar` + `auth.jar`) | **55 passed, 4 failed** (exit 1; all 4 from 3 pre-existing defects in `cluster_proof.clj`) |
| 6b | `softland.runtime.worker-kill-proof` on proof cluster (`Store` + `Auth` worker SIGKILLs) | **7 passed, 0 failed** (exit 0) |
| 6b | `softland.runtime.two-lines-proof` on proof cluster (two runtime JVMs, permission ended in Store) | **4 passed, 0 failed** (exit 0) |
| 6b | `ss -tln` across `1983, 2101, 8898, 13000-13009` after stopping daemons | `(none listening)` |

### What is open

1. **Three pre-existing defects in `test-runtime/softland/runtime/cluster_proof.clj`:** missing `java.lang.ProcessHandle` import (line 53), `ProcessHandle.Info.arguments()` returning empty on Linux JDK 21 for Rama's ~15 KiB worker command lines (lines 110–121), and vector entity `[:tile n]` rejected by `softland.store.act` (line 191). Left untouched per path boundary; proved via `worker_kill_proof.clj` instead. *(Repaired on 23 November, §8; `worker_kill_proof.clj` now runs the cluster proof's own kill steps.)*
2. **Ration layer 2 (per-person write pacing in Auth) and account blocks:** deliberately excluded from Step 5; still open when needed.
3. **Remaining pre-existing items noted in `REVIEW-runtime-spine.md`:**
   - Refusing `:replace-secret` over `/line` for `:browser` sign-ins (until the pages replace browser cookies);
   - Updating `about`'s `:until` on a held connection after `:replace-secret` (currently `Caller.until` holds the initial secret's time, only shown by `:who-am-i`);
   - A web page to end a program's sign-in (today only `end-sign-in` over the line does);
   - The third check in `checks.clj` ("No live read and no watched root is left behind by connections that ended"), which does not yet assert watched roots.
4. **Browser client (Piece 7)** and real Google OAuth client ID configuration (live runs used the test stand-in provider; no money spent).
5. **Re-validating against live `src-store`:** all runs today used `/mnt/data/projects/research/softland/runtime-spine-2026-11-21/store-at-head/src-store` because another session is editing `src-store/` concurrently. Once `src-store/` settles, rebuild `store.jar` from `src-store/` and re-run without the `-Sdeps` path swap. *(Done on 23 November against `5e9eca8d`, §8.)*
6. **`spine/runtime-oracle/`:** can be retired now that real `src-auth` (`7a381f3a`) passes every proof.

### Choices of mine for Sid to turn

1. **Non-empty `:model` text on the wire (`src-wire/softland/wire/words.clj`):** `words/driven-by?` checks `:model` with `words/text?` (`1..256` characters, rejecting `""`, matching `:label` and `:email` on the wire) rather than `0..256` as Auth itself allows.
2. **`:driven-by` presence in `seam/about` (`src-runtime/softland/runtime/seam.clj`):** included whenever the caller holds a session (populated from `auth/who-is`, including one `who-is` read after `new-session?` opens a session so `:driven-by` always comes from `who-is`); omitted when the caller has no session or is a visitor.
3. **CLI surface for model sessions (`src-command/softland/command/main.clj`):** added `--model <name>` to `new-session` (opens the new session via `:open-session`, closes the previous session, saves the new session id, prints `hello`), rather than adding a separate top-level subcommand.
4. **Ration defaults and wire word (`src-wire/softland/wire/words.clj`, `src-runtime/softland/runtime/seam.clj`):** added `:take-more` (HTTP `429`) to `words/could-nots`; set the four copy-local defaults in `seam/default-numbers` to `:sign-in-window-ms 10000`, `:max-sign-in-posts-per-address 30`, `:max-watches-per-root 256`, `:max-visitor-watches 64` (matching carbon's key names), with the per-address table capped at `10000` entries in copy memory.
5. **Yes-page actor check (`src-runtime/softland/runtime/http.clj`):** when a person submits a second `yes` naming an agent while an agent stands, the runtime also checks the standing agent's label via `auth/under` so submitting a *different* agent name returns 409 rather than `"Done"`.
6. **In-flight secret replacement state (`src-command/softland/command/main.clj`):** stored under `:next-credential` inside `sign-in.edn` (same mode-0600 atomic file), tried first on subsequent connections and cleared on promotion or definitive `:take-that` refusal.
7. **Locking beyond pings (`src-runtime/softland/runtime/line.clj`, `seam.clj`):** in addition to switching `jetty.clj` to non-blocking `sendPing`, replaced `synchronized`/`locking` monitors with `ReentrantLock` in `line/say!`, `seam/standing`, and `seam/watch` so slow I/O or Auth calls do not pin JDK 21 virtual-thread carriers.
8. **Proving worker kills without editing `cluster_proof.clj` (`test-runtime/softland/runtime/worker_kill_proof.clj`):** left `cluster_proof.clj` untouched per path rules, documented its three defects verbatim, and verified actual `Store` and `Auth` worker SIGKILL + supervisor restart in a separate proof file.


---

## 8. The review of 22 November, fixed (23 November 2026)

A session reviewed `src-auth`, `src-runtime` and `src-command` on 22 November («4da7e5c2»; its probes are in `/mnt/data/projects/research/softland/review-2026-11-22/`). Sid pasted its points with a build prompt: fix the High, Medium and Low items, `cluster_proof.clj` with them; run the proofs against the live `src-store` on `main`, with `store.jar` rebuilt from it; where a point is his call, take the sound default, keep `reference.clj` agreeing, and mark it for him; leave the `.cljc` forms, the firewall of 8a and the replay dedup of `*root-events` out, as open items. That prompt is in no session text of the record, and has the shape of his line of 18:18 ("here are the points the reviewer found … now go and fix"). Auth's side was built by a fork of this session and is on [Auth's page](auth/BUILD.md), §6 ("The review pass of 23 November 2026"); this section holds the runtime and the command line, and the runs of both.

### 8.1 What was wrong, what changed, and what shows it

| Point | Was | Now | Shown by |
|---|---|---|---|
| 1 (High) | `replace-on-line!` wrote back the map that it read at the start: a second command, refused `:wrong-secret`, wrote the old credential over the first one's new one. | Each change of `sign-in.edn` is made while the command holds the file (`with-file-lock`: a lock of `sign-in.edn.lock` beside it, and an object of the process for its threads), to what the file holds then. A replacement holds the file from its decision to its last write; a command that waited finds the credential changed and sends nothing. (`src-command/softland/command/main.clj`) | The reviewer's probe `wire-fork/p3.clj`, both commands through `main/run`: the file ends with the credential that is the hash now (it held the one before), and the next command after the grace is let in (it was not). |
| 2 (High) | `$$live` one flat map, each watched root a range proxy on it. | Nested by root, the inner map subindexed. | Auth's page, §6. |
| 3 (High) | A lost watch was told to a root's connections one after another on one thread, each through a blocking write. | `standing.clj` tells each listener on a virtual thread of its own; the seam hands the end to its own thread and says it outside its lock; one write of a frame waits at most `send-ms` (30 s), past which the connection is dropped (`jetty.clj`). | `runtime-fork/probe_seam.clj` B, four trials: 8 of 8 siblings told and closed (it was 0 of 8). |
| 4 (High) | Sessions opened without bound: each connection that names none, the word `:open-session`, and each GET of the name box. | The opening of sessions is paced per sign-in in each copy (30 a minute). The name box opens none when it is shown; its post opens the session that the seam derives from the act's UUID (`:session-for`) and closes it after the act. | A probe through the seam (`seam/caller`, Auth stubbed): of 35 connections of one sign-in in a minute, 30 let in, 5 told `:take-more :sessions-paced`; `:open-session` paced the same. `open_session_proof.clj`: the box shows no session, its post opens one with `{:kind :person}` and closes it. |
| 5 (High) | A watch whose opening outlasted 5 s left its proxy open. | The late proxy is closed. | Auth's page, §6. |
| 6 (Medium) | In a partial outage, roots probed one at a time. | One deadline for all. | Auth's page, §6. |
| 7 (Medium) | The sign-in pace counted a program's own polls by `:remote-addr`; a 429 ended the command line's wait. | A poll that proves its ask is given back to the address, and an ask's polls are paced on their own (20 in 10 s); an IPv6 address counts by its /64; behind a proxy named with `--trusted-proxy`, by the last address of `X-Forwarded-For` that is not a trusted proxy (`http.clj`, `seam.clj`, `main.clj`). `sign-in` waits through `:take-more` until the ask's end. | A probe through the pages' handler (Auth's ask stubbed): three programs polling once a second for 12 s from one address, 0 of 36 refused, and a person's post after them taken; polls of a code that names no ask still use the address up; 2,000 posts from one /64, 1,970 refused (none were); behind a trusted proxy, 40 clients none refused, one client 10 of 40. The attack `sign-in-pace` still passes. |
| 8 (Medium, Sid's call) | A text with a lone surrogate was stored changed. | Default: such a text does not fit, in Auth and in the reference. The runtime cuts what a provider says (`:says`, and the name that the box offers) at whole characters and writes a lone surrogate as U+FFFD, so a sign-in is not refused for one. | Auth's page, §6. |
| 9 (Medium) | `module.clj` said that the model-checked steps run. | Says what holds. | Auth's page, §6. |
| 10 (Medium) | Proofs said more than they asserted. | `cluster_proof.clj`: its three defects repaired (the import of `ProcessHandle`; a worker found by `/proc/<pid>/cmdline`; keyword entities), and each check after a kill fails when no worker was killed; the series check prints how many acts were cut off rather than claiming some were. Whether a held connection is still watched is proven by ending its sign-in through another connection, not by `:who-am-i`. `worker_kill_proof.clj` runs the cluster proof's own two kill steps. `two_lines_proof.clj` says and holds that the watch is told the layer is left out and stays open with no end. `checks.clj`'s two first sign-ins are let go at one moment and the check holds that they were in flight together. | The runs below. |
| 11 (Medium) | One connection could take a whole cap. | 128 watches for one connection (a visitor's 8), beneath the 256 of a root and the 64 of all visitors. | `probe_seam.clj` C: one visitor's connection 56 of 64 refused and the next visitor let in; one agent's connection 128 of 256 refused and the person let in. The attack `watch-caps` still passes. |
| Low | The server's ping kept a silent peer from the idle close. | `jetty.clj` keeps when it last heard from each connection, and closes one not heard from for `idle-ms`. | `probe_idle.clj`: a peer that sends nothing is closed at 2.0 s (ping 0.5 s, idle 1.5 s); it stayed open. |
| Low | The standing's timers stayed queued up to 12 h after cancelling. | The seam's timer drops what it cancels. | `probe_seam.clj` D: after 300 connections opened and ended, 0 tasks queued. |
| Low | `new-session --model` closed the old session before it saved the new one. | The file is moved off the old session first. | `command_proof.clj`. |
| Low | The runtime's README said that ration is not built. | It says what is built. | — |
| Low | §1.4 and §2 of this page said that `checks.clj` cannot pass. | Marked as repaired in `a1cb405e`. | — |
| Low (Auth) | end-actor read every later agent; no form check in the topology; rows kept for good. | Auth's page, §6. | |
| Drift, entry 8 | The yes page chose "It acts as me". | It chooses "an agent of its own" when it opens. | — |

### 8.2 Defaults taken for Sid to turn

- **The yes page chooses "an agent of its own" when it opens** (entry 8: each program has a who of its own). A post still says which; the server's reading of a post is unchanged.
- **A text that UTF-8 cannot carry does not fit** (point 8), on Auth's page.
- **The new numbers**, each in `seam.clj`'s `default-numbers` and a flag of `main.clj`: 30 sessions opened per sign-in a minute; 20 polls of one ask in 10 s; 128 watches for one connection and 8 for a visitor's; and `jetty.clj`'s `send-ms`, 30 s.
- **`--trusted-proxy`**: no proxy is trusted unless the opener names it.
- **A page's one act has a session derived from its UUID**, a session's choice, so that the rule that no system chooses an id still holds and a form posted twice is in one session.

### 8.3 Left out, and open

- **The `.cljc` forms** (entry 10: "share the `.cljc` forms"): the tree has none; `words.clj` requires the store's `.clj`. Left out of this pass by the prompt.
- **Rama's ports kept from other programs, as code** (entry 12, 8a): the cluster's ports listen on every interface (§1, Step 1), and Auth takes any appender's word. Left out by the prompt.
- **The replay dedup of `*root-events`**, and with it the rows that Auth keeps for good: Auth's page, §6.
- **What `:driven-by` is worth** (entries 3 and 8): a program says what drives its session, and `{:kind :person}` is taken from it. It decides nothing today. By entry 3 the runtime is to write the trail of every session's reads, whatever drives it, so a program's word cannot keep a model's reads out of the trail once the trail is built; what `:driven-by` should mean in it is open.
- **A watch whose permission ends.** The runtime's page wrote "a watch closes when its permission ends"; the store's read path tells it the layer is left out and keeps it open, and the two-lines proof now holds that. Which is meant is open.
- **The pace of sessions bounds how fast they are opened, not how many stand.** A session that nobody uses still stands for a day. A bound on how many stand under one sign-in has to be counted where they all are, in Auth, and changes its contract.
- **`spine/runtime-oracle/`** can still be retired (§7).

### 8.4 The runs

All behind `flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock`, from the repository root, with the live `src-store` (`5e9eca8d`, clean) and `src-auth` at `dc9142ff`, after the last edit. The scripts and every output are under `/mnt/data/projects/research/softland/review-fixes-2026-11-23/` (`bin/`, `runs/final/`, `runs/final-cluster/`); the reviewer's probes as run against the fixes are in `probes/` there.

```text
bin/runtime-run.sh runs/final  (each: clojure -M:runtime -m <ns>, the wire's with -M:wire)
softland.wire.pure-proof:                    38 passed, 0 failed. (exit 0)
softland.runtime.pure-proof:                 22 passed, 0 failed. (exit 0)
softland.runtime.proof:                      49 passed, 0 failed. (exit 0)
softland.runtime.command-proof:              16 passed, 0 failed. (exit 0)
softland.runtime.review-faults-proof:         6 passed, 0 failed. (exit 0)
softland.runtime.open-session-proof:          5 passed, 0 failed. (exit 0)
softland.runtime.two-lines-proof:             3 passed, 0 failed. (exit 0)
softland.runtime.attacks.slow-readers:        1 passed, 0 failed. (exit 0)
softland.runtime.attacks.yes-page:            1 passed, 0 failed. (exit 0)
softland.runtime.attacks.closed-session:      1 passed, 0 failed. (exit 0)
softland.runtime.attacks.unwatched-standing:  1 passed, 0 failed. (exit 0)
softland.runtime.attacks.lost-replacement:    1 passed, 0 failed. (exit 0)
softland.runtime.attacks.sign-in-pace:        1 passed, 0 failed. (exit 0)
softland.runtime.attacks.watch-caps:          1 passed, 0 failed. (exit 0)
```

The proof cluster, from a fresh state (`bin/cluster-run.sh`: what the last run kept is moved aside; standalone ZooKeeper on `127.0.0.1:2101`, conductor, supervisor; `Store` and `Auth` launched with `--tasks 4 --threads 2 --workers 1 --replicationFactor 1`; every daemon stopped after). `store.jar` is `src-store` at `5e9eca8d`, jarred as it is; `auth.jar` is `src-auth` at `dc9142ff`.

```text
== softland.runtime.cluster-proof        59 passed, 0 failed.
== softland.runtime.worker-kill-proof     7 passed, 0 failed.
== softland.runtime.two-lines-proof       4 passed, 0 failed.
after stop, listening on the cluster's ports: (none listening)
processes of the cluster left: 0
```

An earlier run of the same, with Auth at `c1b5554f`, gave the cluster proof 58 passed and 1 failed: the series check then held that some acts were cut off by the store worker's kill, and that run had none cut off (Rama's client sent the appends again while the worker came back). The check now prints that count and holds what holds every time; that is in `d99b0db6`. The 4,467 lines of "Executor pool is shut down" in that proof's output are Rama's client destroying the 6,000 watched roots' proxies after the proof has closed the cluster handle, within 0.2 s at its end.

The probes, against the fixed code (no cluster; what they stub is named in each): `wire-fork/p3.clj` (point 1), `runtime-fork/probe_seam.clj` B, C and D, and `probe_idle.clj` (the reviewer's, with C's count of refusals and D's look into the timer made to read the new code), `probe_pace.clj` and `probe_sessions.clj` (written for points 7 and 4). Their outputs are in §8.1.

### 8.5 Commits

Runtime and command line, this session:

- `ce2b1447` Command line: two commands at once change sign-in.edn one after the other
- `9d3a9678` Runtime: a lost watch reaches every connection, and a system that does not read is ended
- `736ad947` Runtime: sessions, polls and watches are paced per sign-in, ask and connection
- `bea366f8` Runtime: the name box's post opens its session only when it will send its act
- `d99b0db6` Runtime's proofs: each says what it holds, and the cluster proof runs again
- this page

Auth, the fork (its page, §6): `42e4e786`, `444e391c`, `c1b5554f`, `a3941d66`, `68ba4e07`, `dc9142ff`, `82a8ba48`.

Nothing is pushed. `src-store/`, `test-store/` and `test-runtime/softland/runtime/attacks/` are not touched.
