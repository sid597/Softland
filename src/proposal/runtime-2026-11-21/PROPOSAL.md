# The runtime: full-spectrum revision and build specification

**21 November 2026.** Written from the constraint-relaxation review of the
20 November build ([PROPOSAL.md](../server-2026-11-20/PROPOSAL.md),
[BUILD.md](../server-2026-11-20/BUILD.md),
[REVIEW-assumptions.md](../server-2026-11-20/REVIEW-assumptions.md)) across
three independent sessions and Sid's rulings of 21 November.

Nothing in this specification has been run yet. Three kinds of statement are
kept apart:

- **Sid's decisions** of 21 November 2026 (and the earlier store decisions they
  keep or turn);
- **checked facts** of the stack and current source;
- **engineering choices** of this plan, which hold only insofar as they serve
  the decisions and facts.

 Superseded trees (`src-server`, `src-cli`, `src-server-browser`) are moved
aside, never deleted (`AGENTS.md`, "Who decides what"), once the replacement
passes its checks.

---

## 1. Design rule

Stay strictly constrained on engineering bar (multi-process correctness,
partitioned Rama state with no single-task bottlenecks, edge bounds before
parsing, persistent-cluster proofs); stay open across Softland's full product
spectrum (humans, autonomous agent fleets, converters, services, live ZUI
views defined as data in layers) with sensible defaults instead of hardcoded
walls.

---

## 2. Sid's decisions of 21 November 2026

| # | Area | Sid's ruling | Earlier decision kept or turned |
|---|---|---|---|
| **1** | **Naming** | The program is called **`runtime`** (`src-runtime`, `softland.runtime.*`). | Matches 11–12 Oct store decisions and 3 July substrate notes; turns the 20 Nov folder name `server`. |
| **2** | **No `:who` relay (`0a = A`)** | No caller ever submits an act carrying another principal's `:who`. `:who` is always the authenticated maker sending the act; an external original author is recorded inside `:facts` (as git separates committer from author). Other deployments arrive as signed published lines later. | Keeps `:who` unambiguous in an untrimmed log with no per-user keys. |
| **3** | **Read Trail apart from `:based-on` (`0b = B`)** | `:based-on` holds only the explicit `{fact-id stamp}` premises an act logically stands on. Browse and exploration reads (`Read-1..N`, e.g. ZUI panning across layers or agent search) are recorded separately in the session's **Read Trail**, with an optional `:read-seq` cursor on an act linking `Action-M` to the session's browse history before it. | Fulfils 12 Oct entry 4 ("a model's reads, always; a person's, until she turns it off") without hitting `max-based-on` or polluting causal dependencies. |
| **4** | **Rama module split by partition key (`1 = 1A`)** | Keep `src-store` (`Store` module) strictly eternal, clock-free, untrimmed, and deterministically replayable (`*streaming-depot` and `*microbatch-depot`, partitioned by `:layer`). Put Auth/Standing (`:root-who`, `:account`), Read Trails (`:session`), and Ephemeral Signals (`:layer`, TTL/trimmed) in dedicated Rama modules. Expose presence and Read Trails through the same data-driven `read`/`watch` wire protocol. | Keeps 7 Oct entry 1 and 13 Oct `run-again` replay invariance intact; pays zero extra network hops because `:root-who`, `:account`, and `:session` already live on different tasks from `:layer` and need no cross-module mirrors. |
| **5** | **Permissions & layer settings in `src-store` (`2 = 2A`)** | **(a)** A permission can be ended by its giver or any ancestor up to the layer's root holder, cascading to invalidate everything delegated beneath it (`decision.clj`). Permissions stay layer-wide (`:see` / `:see+:act`) in this pass.<br>**(b)** Writing layer settings (`:softland.store/who-may-see`, `:builds-on`, `:marked-keys`) is restricted to the layer's root permission holder (`MODEL.md:310`). | Resolves two open holes in `src-store` (`README.md`, 11–12 Oct entry 1; `MODEL.md:310`). |
| **6** | **Canonical act on the wire, 1-RT commit, resumable `:session` (`3 = 3A`)** | **(a)** The maker sends the whole canonical `softland.store.act` (`:who`, `:session`, and maker-minted 3-slot `[layer depot uuid]`, which `door.clj:435` already accepts); the runtime compares `:who` and `:session` against the actor rather than stripping/injecting them.<br>**(b)** Interactive writes complete in **1 round-trip** (`send-and-wait`), alongside streaming/batched `send-all` for imports.<br>**(c)** `:session` names one sitting or agent run and stays constant when short-lived auth secrets rotate underneath, keeping `door/digest` stable across reconnects. | Restores 6 Oct entry 2 ("The one form is the maker's act"); keeps 6 Oct entry 5 (`[layer depot uuid]`); turns the 20 Nov wire-stripping choice. |
| **7** | **Agent identity & branch-layer default (`4 = 4A`)** | Every attached program, CLI, agent, or service gets its own `:who` (`:principal/type #{:person :program :service}`), with `:session` recording whether a `:model` drives the run. Attaching an agent defaults to creating a dedicated **branch layer** (`:builds-on` the target layer, viewed live via overlay stack) paired with a **`promote / merge`** operation into the target layer — while also allowing direct shared-layer grants when one timeline is chosen. | Matches 8 Oct entry 3 (delegated permission chains) and transformability (`branch -> merge` is lossless; `shared -> un-mix` is not). |
| **8** | **Client sampling & refusal visibility (`5.1 = A`, `5.2 = B-safe`)** | **(a)** Client Missionary flows throttle/sample high-rate ZUI input (transient cursor/camera motion -> TTL `Signals`; meaningful viewport/context reads -> `:session`-partitioned `Trails`).<br>**(b)** Add stamp-ordered whole-act reads (`stamp -> act + answer`) in `src-store`. Refused acts are readable by **explicit permission holders of the layer (`:act` or member `:see`) + the act's own `:who`**, excluding anonymous `:everyone` visitors unless enabled on the layer. | Fulfils 12 Oct entry 7 (refused acts usable by authors and maintainers) while blocking anonymous spam graffiti on `:everyone` layers. |
| **9** | **Direct HTTP MCP & Clojure CLI (`6 = 6A`)** | The runtime serves **Streamable HTTP MCP** directly (`OAuth 2.1` short rotating tokens, fixed meta-tools reading layer-declared schemas/operations as data, server-pushed change notifications — zero local binary install for Claude Code / Codex). Terminal CLI and SDK are built in Clojure sharing `.cljc` schemas, exact numeric types, silent machine-bound keys, and automated proofs. | Retires untested Node ClojureScript `src-cli` and its `Verbatim` float64 workaround. |
| **10** | **End-to-end Missionary reactivity (`7`)** | Keep proven push-driven `read_path.clj` (with per-copy share of `pstate.proxy.limit`); connect runtime and client via **Missionary flows and signals over pure data structures** across a multiplexed N-watch connection streaming incremental `{:changed ...}` place diffs to WebGPU and views-as-data (using Electric constructs only where they directly fit, never locking views into AOT `e/server` DOM bundles). | Aligns reactive architecture with WebGPU and runtime-authored views (`vision/LOG.md`). |
| **11** | **Door signing, account PII & signup gate (`8a = A`, `8b = B`, `8c = A`)** | **(8a)** No runtime door signature on acts yet; enforce Rama port isolation as code.<br>**(8b)** Store `sub` + `email` on the erasable `Auth` account link (partitioned by `[provider, subject]`, multiple replaceable accounts per person, JWKS refreshed by `kid` / `Cache-Control`), and set initial display `:name` once at registration — never dumping raw JWTs on every login.<br>**(8c)** Allowlist / invite-only signup by default, switchable by config to open signup (`B`) later. | Keeps 19 Nov no-key ruling (`8a`), 9 Oct entry 1 erasable account links (`8b`), and protects untrimmed depots until public launch (`8c`). |

---

## 3. Target architecture

```text
  WEBGPU / ZUI CLIENT           CLOJURE CLI / SDK             AGENTS (HTTP MCP & HARNESSES)
  Missionary reactive DAG       Shared .cljc forms &          Own :who + fresh :session per run;
  (+ Electric where natural);   exact numbers; RFC 8628       zero-install HTTPS MCP (OAuth 2.1
  views & queries as data;      device grant + bound key;     short rotating tokens; fixed tools
  HttpOnly cookie + Origin      non-zero exit on :refused     over layer-declared operations)
             \                              |                                /
              +-----------------------------+-------------------------------+
                                            v
  +--------------------- RUNTIME (stateless JVM copies; hard edge bounds) ---------------------+
  |  Edge guards : frame & longest-token caps before EDN read, Origin/CSRF, safe way-on,       |
  |                stand-in blocked on --conductor, visitor edge pacing, virtual-thread handoff |
  |  Standing    : token carries [root-who actor-who session-id] -> 1-hop check/watch on       |
  |                root-who's task; JWKS refreshed by kid / Cache-Control                      |
  |  Multiplexed : 1 connection carries N dynamic watches + backpressure across 3 planes:      |
  |    seam          1. Acts    whole canonical act + maker act-id; 1-RT send-and-wait + batch |
  |                  2. Reads   incremental place diffs + lossless stamp-ordered act streams   |
  |                  3. Signals bounded TTL cursors/cameras/presence + Read-Trail queries      |
  +-----------------------------------------+--------------------------------------------------+
                                            v
  +------------------------ RAMA CLUSTER (every path hash-partitioned) ------------------------+
  |  Store    (hash-by :layer)    : untrimmed act depots, cascading permission revoke,         |
  |                                 root-only layer settings, stamp -> whole-act reads,        |
  |                                 principal/session metadata acts, layer promote/merge       |
  |  Auth     (hash-by :root-who  : [provider subject] -> person-uuid (atomic unique, erasable |
  |            & :account)          email); $$standing (watched per active root-who, separate  |
  |                                 from hot $$quotas); expiring RFC 8628 device grants        |
  |  Trails   (hash-by :session)  : durable Read-Trail [session seq] -> read event             |
  |  Signals  (hash-by :layer)    : trimmed/TTL depot & PStates for live presence/cursors      |
  +--------------------------------------------------------------------------------------------+
```

### Key invariants that prevent the 20 November failure modes
1. **1-hop standing & quota rollup without proxy storms:** `$$standing` (sessions + block flag, watched via 1 shared `foreign-proxy` per active `root-who` per runtime copy) and `$$quotas` (hot request/watch counters, never watched by streams) both partition by `root-who`, replacing `$$sessions-lately` (`|global` ring buffer).
2. **No privilege escalation across credential types:** A program token is never accepted as a browser cookie (`BUILD.md:172`). Programmatic sub-agent creation can only delegate a strictly narrower permission beneath the caller's own standing.
3. **Shared `pstate.proxy.limit` budget:** Each runtime copy bounds its `read-path` live pushes to its configured share of Rama's per-table proxy limit (`OPEN-QUESTIONS-rama.md:129`).

---

## 4. Phased build order (how we fix what is built)

Each phase lands whole with its explanations and proofs before the next starts:

1. **Phase 1 — `src-store` additions (`:store` & `:store-module`):**
   - Cascading permission revocation in `act.clj`, `decision.clj`, and `read_path.clj` (live reads close when an ancestor permission ends).
   - Root-holder guard on layer settings (`:who-may-see`, `:builds-on`, `:marked-keys`).
   - Stamp-ordered whole-act & refusal read (`B-safe` visibility: explicit layer members + act's `:who`), plus optional `:read-seq` part on an act.
   - Pure `promote` act builder for merging selected or all current facts from a branch layer into a target layer with `:based-on` / `:replaces` conflict safety.
   - Prove with pure proofs, cluster gate/read proofs, and `run-again` deterministic replay proof.

2. **Phase 2 — Runtime Rama modules (`Auth`, `Trails`, `Signals` via `/rama` workflow):**
   - `Auth`: `[provider subject] -> person-uuid` uniqueness & erasable link row; `$$standing` and `$$quotas` partitioned by `root-who`; expiring RFC 8628 device-grant challenges; rotating session secrets bound to stable `:session` UUID.
   - `Trails`: `:session`-partitioned append-only Read Trail (`seq`, `at`, `query`, `seen`).
   - `Signals`: `:layer`-partitioned trimmed/TTL ephemeral presence & cursor state.

3. **Phase 3 — `src-runtime` seam, multiplexed wire & direct HTTP MCP:**
   - Shared `.cljc` word & schema declarations (declared once).
   - Missionary seam accepting whole canonical acts (`(= (:who act) actor)`), `send-and-wait` (1-RT), `send-all`, multiplexed N-watch streams over all three planes, `kid`/`Cache-Control` JWKS refresh, allowlist/invite gate, and Streamable HTTP MCP endpoint (`/mcp` with OAuth 2.1 rotating tokens + fixed layer-data meta-tools).

4. **Phase 4 — Clojure CLI (`src-cli` replacement) & Missionary client (`src-runtime-browser`):**
   - Clojure CLI with silent local key binding, RFC 8628 login, exact EDN numbers, non-zero exit on `:refused`, and automated integration checks.
   - Client Missionary reactive DAG consuming multiplexed `{:changed ...}` place diffs and throttled signal/trail emitters (ready for WebGPU and data-defined views, with a minimal inspector surface proving live writes, overlay branches, `promote`, and instant revocation).
   - Move `src-server`, old `src-cli`, and `src-server-browser` to `history/` (or `src/proposal/server-2026-11-20/built-trees/`) per `AGENTS.md`.

5. **Phase 5 — Multi-process persistent cluster proofs:**
   - Two separate runtime JVM processes against a persistent `--conductor` cluster: worker/process kills, concurrent sign-in uniqueness, cascading permission revoke closing live watches across copies, quota enforcement, and end-to-end MCP + CLI + browser runs.
