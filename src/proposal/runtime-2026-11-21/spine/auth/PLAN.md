# Plan: Auth

Whole-module Phase 1 design for the Rama module `Auth` and its foreign client namespace `softland.auth.client`, covering all 18 calls of [SPEC.md](SPEC.md) (with [IMPLICIT_SPEC.md](IMPLICIT_SPEC.md) wherever it does not conflict with `SPEC.md`). No implementation code.

## Reads

Every read judges no clock (`SPEC.md` "No clock"): timestamps (`:until`, `:previous`, `:ends`, `:begun`) are returned as stored and evaluated against the runtime's own clock. Every read validates its input in `softland.auth.client` before touching Rama; invalid input returns `{:could-not :read-the-event}`, a closed client returns `{:could-not :closed}` (including from `watch-root` in place of the stop function), and any Rama exception or timeout returns `{:could-not :reach-the-module :error ...}`.

| Read | Access method | Target PState(s) & path expression(s) | Partition | Cost (seeks + iters) & latency | Throughput |
|---|---|---|---|---|---|
| `who-is` `{:root P :sign-in G :session S-or-nil}` | Query topology `"who-is"` (`foreign-invoke-query`) | `$$root-data`: `(keypath "<P>\0r")`; if `RootEntered`, `(keypath "<P>\0g:<G>")`, conditionally `(keypath "<P>\0s:<S>")` when `S != nil`, conditionally `(keypath "<P>\0a:<A>")` only when `G` is ended and `A != P` | `\|hash P` (leading partitioner; 1 RT directly to 1 task) | 1–4 point seeks (typically 3 with `S`, 2 without `S`), 0 iter-reads; ~1.0–2.0 ms disk + 1 RT | Highest by orders of magnitude: every HTTP request & connection open |
| `ask-of` `U` | Direct `foreign-select-one` | `$$asks`: `[(keypath U)]` | `\|hash U` (1 RT directly to 1 task) | 1 point seek, 0 iter-reads; ~0.5 ms disk + 1 RT | Polls × waiting asks (low–moderate) |
| `under` `P` | Query topology `"under"` (`foreign-invoke-query`) | `$$root-data`: `(keypath "<P>\0r")`; if `RootEntered` and `next-actor-seq > 0`, `(sorted-map-range "<P>\0ao:" "<P>\0ao;")` with `{:allow-yield? true}`; if `RootEntered` and `next-sign-in-seq > 0`, `$$live`: `(sorted-map-range "<P>\0g:" "<P>\0g;")` | `\|hash P` (leading partitioner; 1 RT directly to 1 task) | 1–3 seeks + `A` agent iters + `L` live sign-in iters (~1–7 ms even for 1,000 agents) | Low: person opens agents/sign-ins view |
| `watch-root` `P` `f` | Shared `foreign-proxy-async` per `(client, P)` + internal liveness query `"ping-task"` | `$$live`: `[(sorted-map-range "<P>\0" "<P>\1")]` with `{:pkey P}` | `\|hash P` (1 persistent subscription per `(runtime copy, P)`) | Opening: 1 range scan (`1 seek + (L + O) × 5µs`). Steady state: 0 seeks per push (incremental `KeyDiff`/`KeyRemoveDiff`/`MultiDiff`) | One subscription per `(runtime copy, watched root)`, up to 20,000 per task |
| *(Internal)* `ping-task` `task-id` | Query topology `"ping-task"` (`foreign-invoke-query-async`), run every 2 s only while watches are open | None (in-memory check of current task id) | `\|direct task-id` (leading partitioner; 1 RT directly to `task-id`) | 0 seeks, 0 iter-reads; < 0.5 ms | `N` in-memory RPCs / 2 s per runtime copy that holds >= 1 watch |

**Output projections and contract details:**

- **`who-is`**: Returns `{:sign-in {:actor A :kind K :hash H :until t1 :previous {:hash H0 :until t2}-or-nil :ended? bool :actor-ended? bool}-or-nil, :session {:sign-in G' :until t3 :closed? bool :driven-by D-or-nil}-or-nil, :own-layer L-or-nil, :own-layer-made? bool}`. Never exposes `:making` or PII (`account`, `email`, `:says`). `:driven-by` is returned inside `:session` (`{:kind :person}`, `{:kind :model :model text}`, or `nil` when absent) both while the session is open and after it closes.
- **`ask-of`**: Projects `AskWaiting` and in-flight `AskAnswering` identically to `{:hash H :asks text :ends t1 :state :waiting :answer nil}` so uncommitted answers are never visible. Projects `AskYes` to `:state :yes :answer {:root P :actor A :sign-in G}` and `AskNo` to `:state :no :answer nil`. Absent code returns `nil`.
- **`under`**: Returns `{:actors [{:actor A :made-by B :label text :ended? bool} ...] :sign-ins [{:sign-in G :actor A :kind K :label text-or-nil :begun t :until t1} ...]}`. Agents (`ao:` range) are stored in ascending 16-hex sequence order and exclude `P` herself. Live sign-ins (`$$live` `g:` range) are sorted in memory by `:seq` (tens of items) to preserve exact begin order. Unknown or waiting `P` short-circuits after 1 seek and returns `{:actors [] :sign-ins []}`.
- **`watch-root`**: Projects `$$live` entries into `{:sign-ins {G {:actor A :kind K :hash H :until t1 :previous {:hash H0 :until t2}-or-nil}} :sessions {S {:sign-in G :until t3}}}`. Deliberately omits `:driven-by`, `:begun`, `:label`, and `:seq` from the delivered map. Initial `ResyncDiff` is suppressed so no call is owed at registration and a root with nothing live receives no call until something live appears. Every subsequent change delivers the updated value; unreachability delivers `:softland.auth.client/not-reached` once and terminates the watch.

## Writes

Every write is validated in `softland.auth.client` before touching Rama; malformed input returns `{:could-not :read-the-event}` and appends nothing to any depot. Every valid write appends exactly one record to one depot with `:ack` (waits for depot replication + full stream event-tree completion + PState replication across all participating tasks) and receives its exact contract result map via `(ack-return> result)` under topology `"auth"`. Any Rama exception or client timeout is caught and returned as `{:could-not :reach-the-module :error ...}` (which makes no promise about whether the write was taken; re-sending the same map is always safe).

**Lifecycle calls (no depot append):**

- **`open` `(open cluster module-name)`**: Resolves foreign handles to the 4 depots (`*root-events`, `*asks`, `*invites`, `*enters`), 2 directly-read PStates (`$$asks`, `$$live`), and 3 query topologies (`"who-is"`, `"under"`, `"ping-task"`), verifies module presence and reads `num-partitions` from `(foreign-object-info $$live)`. Never throws: missing module returns `{:could-not :find-the-module ...}`, unreachable cluster/timeout returns `{:could-not :reach-the-module ...}`. Safe for concurrent use across all threads of a runtime JVM; holds no Auth state other than open watch subscriptions and a closed flag.
- **`close!` `(close! client)`**: Idempotently marks the client closed, stops the background `ping-task` liveness monitor, closes all open `foreign-proxy` handles, clears the watch registry, and returns `nil`. Calls no watch callback `f` after returning (suppresses `DestroyedDiff` and uses a per-watch turn + re-entrant `ThreadLocal` guard so `close!` never deadlocks if invoked from inside `f`). Subsequent calls on the client (including `watch-root`) return `{:could-not :closed}`.

**The 12 write operations:**

| Write | Depot | Depot partitioner | Event record type | Takes out under `:root`? | Result delivered via `ack-return>` | Repeat key (checked first) | Refusal order (`SPEC.md` governs) | Latency & throughput |
|---|---|---|---|---|---|---|---|---|
| `invite` | `*invites` | `(hash-by :claim)` | `InviteEvent` | No (`SPEC.md` line 332) | `{:invited P}` or `{:refused ...}` | Same `(P, C)` (even after `P` entered) | `:claim-in-use`, `:person-in-use` | Multi-hop (3 hops uncontended, ~3–5 ms); low–bursty (list invites) |
| `enter` | `*enters` | `(hash-by :account)` | `EnterEvent` | No (`SPEC.md` line 332) | `{:person P :own-layer L' :making {U' M'} :own-layer-made? bool}` or `{:refused :not-invited}` | Already-linked account returns fixed `P, L', M'` + present flag, updates `:says` | `:not-invited` | Returning: 1 hop (~1–2 ms). First-time: 3 hops (1 claim) or 5 hops (2 claims); moderate |
| `own-layer-made` | `*root-events` | `(hash-by :root-id)` (`:person`) | `OwnLayerMadeEvent` | No (`SPEC.md` line 332) | `{:made P}` or `{:refused :no-such-person}` | Already `:own-layer-made? true` -> `{:made P}` | `:no-such-person` | 0 hops (~1 ms); once per entered person + repeats |
| `begin-sign-in` | `*root-events` | `(hash-by :root-id)` (`:root`) | `BeginSignInEvent` | Yes (`:at`) | `{:begun G}` or `{:refused ...}` | Same `(P, G, A, K, initial-hash H)` (even after secret rotations or end) | `:no-such-actor`, `:ended`, `:in-use` | 0 hops (~1–2 ms); few/day/person + agents |
| `replace-secret` | `*root-events` | `(hash-by :root-id)` (`:root`) | `ReplaceSecretEvent` | Yes (`:at`) | `{:replaced G}` or `{:refused ...}` | `G` under `P` with current hash `= H'` and previous hash `= H` (even if ended) | `:not-live`, `:wrong-secret` | 0 hops (~1–2 ms); high volume (with `open-session`) |
| `end-sign-in` | `*root-events` | `(hash-by :root-id)` (`:root`) | `EndSignInEvent` | Yes (`:at`) | `{:ended G}` or `{:refused :no-such-sign-in}` | `G` under `P` already ended -> `{:ended G}` | `:no-such-sign-in` | 0 hops (~1–5 ms, O(open sessions of `G`)); low–moderate |
| `make-actor` | `*root-events` | `(hash-by :root-id)` (`:root`) | `MakeActorEvent` | Yes (`:at`) | `{:made A}` or `{:refused ...}` | Same `(P, A, made-by B)` (even if `A` or `B` ended) | `:no-such-maker`, `:ended`, `:in-use`, `:too-deep` | 0 hops (~1–2 ms); rare (up to thousands/person lifetime) |
| `end-actor` | `*root-events` | `(hash-by :root-id)` (`:root`) | `EndActorEvent` | Yes (`:at`) | `{:ended A}` or `{:refused ...}` | Agent `A` under `P` already ended -> `{:ended A}` | `:no-such-actor`, `:is-a-person` | 0 hops, atomic cascade O(live subtree + live sign-ins + open sessions); rare |
| `open-session` | `*root-events` | `(hash-by :root-id)` (`:root`) | `OpenSessionEvent` | Yes (`:at`) | `{:open S}` or `{:refused ...}` | Same `(P, S, sign-in G)`: if open & `t1 > until`, extends `:until` (never alters `:driven-by`); if closed/taken-out or `t1 <= until`, no-op | `:not-live`, `:in-use` | 0 hops (~1–2 ms); highest write volume (thousands/day/agent + 1/half-day refresh) |
| `close-session` | `*root-events` | `(hash-by :root-id)` (`:root`) | `CloseSessionEvent` | Yes (`:at`) | `{:closed S}` or `{:refused :no-such-session}` | `S` under `P` already closed -> `{:closed S}` | `:no-such-session` | 0 hops (~1–2 ms); high volume (below `open-session`) |
| `ask` | `*asks` | `(hash-by :code)` | `AskEvent` | No (`SPEC.md` line 332) | `{:asked U}` or `{:refused :in-use}` | Same `(U, hash H)` (even if answered or over) | `:in-use` | 0 hops (~1 ms); low–moderate, unbounded over time |
| `answer-ask` | `*asks` | `(hash-by :code)` | `AnswerAskEvent` | Yes (`:at`, on `:root P`, on every path including refusals & standing answers) | `{:yes {:root P :actor A :sign-in G}}`, `{:no U}`, or `{:refused ...}` | Standing answer (`:yes` or `:no`) on `U` returns what stands for any caller/time (after take-out under `P`) | `:no-such-ask`, *(standing answer)*, `:over`, `:no-such-actor`, `:ended`, `:in-use` | 2 hops on `:yes`/`:no` (`H(U) -> H(P) -> H(U)`), 1 hop on early refusal/standing (`H(U) -> H(P)` for take-out); low |

**How a write's result gets back to its caller:**
Every write appends via `(foreign-append! depot event :ack)` and extracts `(get ack-map "auth")`. Because Rama does not deliver `AckReturnMap` until the entire stream event tree (including all cross-task hops and all PState replication to leader + ISR followers) finishes (`11-stream-topologies.md`, `14-depots.md:206`), receiving the result map guarantees that every PState mutation of that write is committed and readable from every copy of the runtime. If `ack-map` is missing `"auth"` or `foreign-append!` throws or times out, the client returns `{:could-not :reach-the-module ...}`.

## PState Design

`Auth` declares **six** partitioned PStates, all owned by the single stream topology `"auth"`, with **zero** global PStates, **zero** `Object` schema positions, and **zero** un-subindexed collections that can exceed an explicitly enforced bound.

### Key-encoding conventions (pure 7-bit ASCII for sorted range safety)

All composite keys in range-scanned PStates (`$$root-data`, `$$live`) use strictly 7-bit ASCII characters (`0-9`, `a-f`, `-`, `:`, `\u0000`, `\u0001`, `;`). Because UTF-8 byte order and Java UTF-16 order are identical on 7-bit ASCII, no key can trigger Rama's rare-codepoint range mismatch (`src-store/OPEN-QUESTIONS-rama.md`, section 5).

- **UUID text `<P>`, `<A>`, `<G>`, `<S>`, `<B>`, `<op>`**: canonical 36-char lowercase UUID string.
- **Order-preserving 64-bit timestamp `<hex-t>`**: `(bit-xor t Long/MIN_VALUE)` formatted as 16 lowercase hex digits. Maps the full signed `Long` range `[Long/MIN_VALUE, Long/MAX_VALUE]` strictly monotonically into `["0000000000000000", "ffffffffffffffff"]`, so `until <= at` is the exact range `["<P>\0exp:", "<P>\0exp:<hex-at>;")` with zero overflow or underflow (`:` is `0x3a`, `;` is `0x3b`).
- **Order-preserving 64-bit sequence `<hex-seq>`**: unsigned 16 lowercase hex digits (`0000000000000000` upwards).

### 1. `$$root-data` — Durable per-root records, history, and internal indexes

- **Partitioning**: `|hash P` (all keys for root `P` live on task `hash(P) mod N`; accessed exclusively via `local-select>` / `local-transform>` after depot alignment or `(|hash *root)`).
- **Schema**: `{String -> IRootDatum}` where `IRootDatum` is a Java interface implemented by strictly typed records (no `Object`, no optional-field soup):
  - Key `"<P>\0r"` (1 per root `P`):
    - `RootReserving [claim-kind<Keyword> claim-val<String> invited-by-kind<Keyword> invited-by-id<UUID|nil> invited-at<Long>]`
    - `RootInvited [claim-kind<Keyword> claim-val<String> invited-by-kind<Keyword> invited-by-id<UUID|nil> invited-at<Long>]`
    - `RootEntered [claim-kind<Keyword|nil> claim-val<String|nil> invited-by-kind<Keyword> invited-by-id<UUID|nil> invited-at<Long> linked-provider<Keyword|nil> linked-subject<String|nil> own-layer<UUID> making-uuid<UUID> making-session<UUID> own-layer-made?<Boolean> next-actor-seq<Long> next-sign-in-seq<Long>]`
  - Key `"<P>\0a:<A>"` (1 per agent of `P`; never deleted):
    - `ActorRec [actor<UUID> made-by<UUID> depth<Long> label<String> ended?<Boolean> seq<Long>]`
  - Key `"<P>\0ao:<hex-seq>"` (1 per agent of `P`, sorted by creation order for `under`; never deleted):
    - `ActorOrderRec [actor<UUID> made-by<UUID> label<String> ended?<Boolean>]`
  - Key `"<P>\0ch:<B>:<A>"` (1 per parent→child agent edge, for `end-actor` cascade; never deleted):
    - `ChildEdge [child<UUID>]`
  - Key `"<P>\0g:<G>"` (1 per sign-in of `P`, live or ended; never deleted):
    - `SignInRec [sign-in<UUID> actor<UUID> kind<Keyword> initial-hash<String> hash<String> until<Long> prev-hash<String|nil> prev-until<Long|nil> ended?<Boolean> begun<Long> label<String|nil> seq<Long>]`
  - Key `"<P>\0ag:<A>:<G>"` (1 per *live* sign-in of actor `A`; removed via `NONE>` when `G` ends):
    - `ActorSignInEdge [sign-in<UUID> until<Long>]`
  - Key `"<P>\0s:<S>"` (1 per session of `P`, open or closed; never deleted):
    - `SessionRec [session<UUID> sign-in<UUID> until<Long> closed?<Boolean> driven-by<IDrivenBy|nil>]`, where `IDrivenBy` is implemented by `DrivenByPerson []` and `DrivenByModel [model<String>]` (nullable when `:driven-by` was omitted).
  - Key `"<P>\0sg:<G>:<S>"` (1 per *open* session of sign-in `G`; removed via `NONE>` when `S` closes):
    - `SignInSessionEdge [session<UUID> until<Long>]`
  - Key `"<P>\0exp:<hex-until>:<g|s>:<id>"` (1 per *live* sign-in or *open* session; removed via `NONE>` when ended/closed or replaced):
    - `ExpiryEntry [kind<Keyword> id<UUID> parent-id<UUID> until<Long>]` (`parent-id` is `actor` for `:sign-in`, `sign-in` for `:session`).
  - Key `"<P>\0ok:<op-id>"` (1 per `answer-ask` attempt evaluated on `P`, for cross-partition helping idempotency; never deleted):
    - `AskOutcomeYes [root<UUID> actor<UUID> sign-in<UUID>]`, `AskOutcomeNo [code<String>]`, `AskOutcomeRefused [reason<Keyword>]`.
- **Readers & writers**: Read by `who-is`, `under`, and every root-touching write. Written by `invite`, `enter`, `own-layer-made`, `begin-sign-in`, `replace-secret`, `end-sign-in`, `make-actor`, `end-actor`, `open-session`, `close-session`, `answer-ask`.
- **Growth**: History keys (`a:`, `ao:`, `ch:`, `g:`, `s:`, `ok:`) grow monotonically with lifetime actors (thousands/root), sign-ins (unbounded), sessions (millions/root for busy agents), and answered asks. Live index keys (`ag:`, `sg:`, `exp:`) stay strictly bounded by currently live sign-ins (tens/root) + open sessions (tens–hundreds/root).

**Alternatives weighed for `$$root-data`:**
- *Option A (chosen): Single flat top-level map `$$root-data` (`{String -> IRootDatum}`).*
  - `who-is` (with `S`, live `G`): 3 direct top-level point seeks (`r`, `g:<G>`, `s:<S>`) = `1.5 ms` disk, 0 iter-reads.
  - `open-session` (common write): 1 range seek on `exp:` (empty = `0.5 ms`) + 1 seek `s:<S>` + 1 seek `g:<G>` = `1.5 ms` reads + 3 direct no-read writes (`s:`, `sg:`, `exp:`) + 1 `$$live` write.
  - `under` (1,000 agents, 10 live sign-ins): 1 seek `r` + 1 range scan `ao:` (`0.5 ms + 1,000 × 5µs = 5.5 ms`) + 1 range scan `$$live` (`0.55 ms`) = `~6.55 ms`.
  - Memory overhead: **1 RocksDB PState partition per task** for all root history and indexes, with strong prefix locality (`"<P>\0*"` keys sit in adjacent SSTable blocks).
- *Option B: Nested `{UUID -> fixed-keys-schema {:sign-ins (subindexed) :sessions (subindexed) :actors (subindexed) :actor-order (subindexed) :children (subindexed) :expiries (subindexed) ...}}`.*
  - Every `local-select>` / `local-transform>` navigating `(keypath P :subindex K)` reads the top-level fixed-keys record at `P` first to locate the subindex handle, adding an extra seek per navigation (`who-is` with 3 separate `local-select>` calls costs 5 seeks = `2.5 ms` vs 3 seeks = `1.5 ms` — **67% more disk seeks on the highest-throughput read**; `open-session` pays extra parent reads on every index update). This matches `src-store/softland/store/record/tables.clj` ("With the layer as the top-level key and three subindexed maps under it, each row would cost two seeks to read and one more to write, where each costs one here").
- *Option C: Six separate flat top-level PStates (`$$roots`, `$$actors`, `$$sign-ins`, `$$sessions`, `$$root-indexes`, `$$ask-outcomes`).*
  - Identical seek count to Option A (`1.5 ms` for `who-is`), **6× the RocksDB partition memory/block-cache overhead per task**, and scatters one root's hot `r`/`g:`/`s:` keys across 3 separate RocksDB instances instead of contiguous blocks.
- *Option D (for take-outs): Omit `exp:` index and scan `$$live` on every `:root` write.*
  - On a root with 200 open sessions, every `open-session` / `replace-secret` scans 200 entries (`0.5 ms + 1.0 ms = 1.5 ms`); on a busy agent root with 2,000 open sessions, every write pays `10.5 ms` of scanning (20× worse throughput). With `exp:`, a write where nothing expired pays 1 seek + 0 iter-reads (`0.5 ms`) regardless of open-session count; maintaining `exp:` adds 0 extra reads (pure `keypath` + `termval` / `NONE>`).
- **Chosen: Option A with `exp:`** — minimum seeks on `who-is`, O(expired) take-outs, single RocksDB partition per task for all root history/indexes.

### 2. `$$live` — Watched live standing per root

- **Partitioning**: `|hash P` (written on task `H(P)`; watched via `foreign-proxy-async` with `{:pkey P}`).
- **Dynamic option set at module launch**: `(set-launch-pstate-dynamic-option! setup "$$live" "pstate.proxy.limit" 20000)`.
- **Schema**: `{String -> ILiveEntry}` where `ILiveEntry` is a Java interface implemented by:
  - Key `"<P>\0g:<G>"` (1 per live sign-in of `P`):
    - `LiveSignIn [sign-in<UUID> actor<UUID> kind<Keyword> hash<String> until<Long> prev-hash<String|nil> prev-until<Long|nil> begun<Long> label<String|nil> seq<Long>]`
  - Key `"<P>\0s:<S>"` (1 per open session of `P`; **no `:driven-by`**):
    - `LiveSession [session<UUID> sign-in<UUID> until<Long>]`
- **Readers & writers**: Read by `watch-root` (`foreign-proxy-async` on `[(sorted-map-range "<P>\0" "<P>\1")]` with `{:pkey P}`) and `under` (range scan `"<P>\0g:" .. "<P>\0g;"` on `H(P)`). Written by `begin-sign-in`, `replace-secret`, `end-sign-in`, `end-actor`, `open-session`, `close-session`, `answer-ask` (`:yes`), and take-out on any `:root` write.
- **Growth**: Strictly bounded by currently live sign-ins (tens/root) and open sessions (tens–hundreds/root); every ended sign-in and closed/taken-out session is removed via `NONE>` in the same atomic event. Contains zero PII and zero byte arrays (ensuring incremental `KeyDiff`/`KeyRemoveDiff`/`MultiDiff`, verified by probe).

**Alternatives weighed for `$$live`:**
- *Option A (chosen): Separate top-level `{String -> ILiveEntry}` PState watched via `(sorted-map-range "<P>\0" "<P>\1")`.*
  - Each `open-session` / `close-session` / `replace-secret` writes 1 small key (~64–150 bytes) with 0 reads (`keypath` + `termval`/`NONE>`), and Rama pushes only the incremental diff (`KeyDiff`, `KeyRemoveDiff`, or atomic `MultiDiff` for multi-key ends/take-outs — verified on Rama 1.6.0).
  - Fully top-level RocksDB-indexed: no un-subindexed collection can ever exceed 100 entries even if an agent opens thousands of concurrent sessions.
  - Isolates `pstate.proxy.limit = 20000` and range-proxy diff dispatch from all non-live history/index writes in `$$root-data`, and keeps the small hot live working set out of `$$root-data`'s unbounded-history LSM tree.
- *Option B: Non-subindexed `{UUID -> {:sign-ins {G ...} :sessions {S ...}}}` watched at `(keypath P)`.*
  - Fails the Rama skill's subindexing rule: open sessions per root have no protocol-enforced cap ("tens to hundreds" in `SPEC.md`, no pacing limit in this build), so a single map per root can exceed 100 elements, forcing full-map read-modify-write and full-map schema validation on every `open-session`.
- *Option C: Merge `$$live` into `$$root-data` under key prefix `"<P>\0l:"`.*
  - Every `local-transform>` on `$$root-data` (every write to `r`, `a:`, `ao:`, `ch:`, `g:`, `ag:`, `s:`, `sg:`, `exp:`, `ok:` — 4–5 writes per `open-session`) would run range-proxy subscriber matching on the same PState holding 20,000 range proxies, multiplying reactivity CPU cost by ~4–5× and churning `NONE>` tombstones in the same RocksDB LSM tree as permanent history.
- **Chosen: Option A** — only design that is simultaneously subindexed/O(1)-write, delivers 1-proxy-per-root incremental diffs, and isolates hot proxied live state from unbounded history.

### 3. `$$accounts` — Account-to-person link (PII-bearing)

- **Partitioning**: `|hash AccountKey` (colocated with `*enters` depot `(hash-by :account)`).
- **Schema**: `{AccountKey -> IAccountState}` where:
  - `AccountKey [provider<Keyword> subject<String>]`
  - `AccountLinking [op-id<UUID> code-claim<ClaimKey|nil> email-claim<ClaimKey|nil> own-layer<UUID> making-uuid<UUID> making-session<UUID> says<IPersistentMap> at<Long>]` (transient while first-time `enter` is in flight; cleared via `NONE>` on `:not-invited` or promoted to `AccountLinked` on success).
  - `AccountLinked [person<UUID> own-layer<UUID> making-uuid<UUID> making-session<UUID> says<IPersistentMap> says-at<Long>]`
  - `:says` is explicitly bounded by `SPEC.md` line 101–103 to at most 32 `String -> String` entries (enforced at client validation before append; `< 50` entries, must not be subindexed).
- **Readers & writers**: Read and written exclusively by `enter` (0 hops for Step 1 on `H(AccountKey)`).
- **Growth**: 1 entry per entered person (millions); refused enters leave 0 entries.
- **Why this design is obvious**: `enter` is the sole operation keyed by `[provider subject]`, requires a 1-seek point lookup, and isolates `[provider subject]` + `:says` into one erasable PState.

### 4. `$$claims` — Invitations by claim (email claims PII-bearing; code claims PII-free)

- **Partitioning**: `|hash ClaimKey` (colocated with `*invites` depot `(hash-by :claim)`).
- **Schema**: `{ClaimKey -> IClaimState}` where:
  - `ClaimKey [by<Keyword> value<String>]` (`by` is `:email` or `:code`; `value` is lower-case email or 64-hex hash).
  - `ClaimWaiting [person<UUID> invited-by-kind<Keyword> invited-by-id<UUID|nil> invited-at<Long>]`
  - `ClaimClaiming [person<UUID> invited-by-kind<Keyword> invited-by-id<UUID|nil> invited-at<Long> op-id<UUID> phase<Keyword>]` (transient during 2-claim first-time `enter`; holds only non-PII `op-id` UUID).
  - `ClaimUsed [person<UUID> invited-by-kind<Keyword> invited-by-id<UUID|nil> invited-at<Long> used-by-op<UUID>]` (holds only non-PII `used-by-op` UUID — never stores `AccountKey` or `:says`).
- **Readers & writers**: Read and written by `invite` and `enter`.
- **Growth**: 1 entry per invited person (millions, including never-proven invitations).
- **Why this design is obvious**: Claims are looked up globally by `(by, value)` with 1 seek; storing `used-by-op` (UUID) instead of `AccountKey` keeps code claims completely free of PII.

### 5. `$$asks` — Device-grant asks by 8-character code

- **Partitioning**: `|hash code` (colocated with `*asks` depot `(hash-by :code)`).
- **Schema**: `{String -> IAskState}` where `IAskState` is implemented by:
  - `AskWaiting [hash<String> asks<String> ends<Long> born-over?<Boolean>]`
  - `AskAnswering [hash<String> asks<String> ends<Long> born-over?<Boolean> op-id<UUID> yes?<Boolean> root<UUID> actor<UUID|nil> new-agent-label<String|nil> sign-in<UUID|nil> until<Long|nil> at<Long>]` (transient during `answer-ask`; projected by `ask-of` as `:state :waiting :answer nil`).
  - `AskYes [hash<String> asks<String> ends<Long> root<UUID> actor<UUID> sign-in<UUID>]`
  - `AskNo [hash<String> asks<String> ends<Long>]`
- **Readers & writers**: Read by `ask-of` (1 `foreign-select-one` seek), `ask`, and `answer-ask`. Written by `ask` and `answer-ask`.
- **Growth**: 1 entry per bound code, never deleted ("A code is bound for good"), zero PII.
- **Why this design is obvious**: `ask-of` is a single-key point lookup by 8-char code `U`; storing `AskAnswering` inline on `$$asks[U]` gives single-task mutual exclusion on `U` with 0 extra seeks.

### 6. `$$enter-ops` — Two-claim `enter` commit coordinator (PII-free)

- **Partitioning**: `|hash op-id` (UUID).
- **Schema**: `{UUID -> IEnterOpState}` where `IEnterOpState` is implemented by:
  - `EnterOpTryEmail []`
  - `EnterOpWon [person<UUID> won-phase<Keyword>]`
  - `EnterOpFailed []`
- **Readers & writers**: Read and written ONLY by 2-claim first-time `enter` (when both `:code-hash` and `:email` are non-nil) and by any concurrent `enter` encountering a `ClaimClaiming` entry on `$$claims`. Never touched by returning users, 1-claim first-time enters, or any read.
- **Growth**: 1 tiny record (~32–48 bytes) per 2-claim first-time `enter` only; zero PII.

**Alternatives weighed for `$$enter-ops`:**
- *Option A (chosen): Separate `$$enter-ops` keyed by non-PII `op-id` UUID.*
  - Allows a competing `enter` (`Acc2`) encountering `ClaimClaiming(op1)` on `$$claims[C]` to resolve `op1` via `|hash op1` **without** storing `Acc1` (PII) inside `$$claims[C]`.
- *Option B: Store `enter` phase on `$$accounts[Acc1]` and place `Acc1` inside `ClaimClaiming`.*
  - Leaks PII (`[provider subject]`) transiently into `$$claims[[:code H]]` and couples code-claim resolution to account-key storage; saves 1 hop only on the rare 2-claim first-time `enter` (at most once per human lifetime).
- **Chosen: Option A** — strictly upholds zero-PII isolation in code claims at negligible one-time cost.

### Summary of PStates grouped by `(key type, partitioner)`

| `(key type, partitioner)` group | PStates | Justification for any group of 2+ |
|---|---|---|
| `(String, \|hash P)` | `$$root-data`, `$$live` | Distinct key structure and access/proxy regime: `$$live` holds only 2 live prefixes (`g:`, `s:`) watched via `foreign-proxy` range scans (`pstate.proxy.limit = 20000`); `$$root-data` holds 10 non-watched history/index prefixes (`r`, `a:`, `ao:`, `ch:`, `g:`, `ag:`, `s:`, `sg:`, `exp:`, `ok:`). Merging them multiplies range-proxy subscriber dispatch 4–5× on every write and mixes unbounded history with high-churn live deletions in one RocksDB LSM tree. |
| `(AccountKey, \|hash AccountKey)` | `$$accounts` | Single PState in group. |
| `(ClaimKey, \|hash ClaimKey)` | `$$claims` | Single PState in group. |
| `(String, \|hash code)` | `$$asks` | Single PState in group (different partitioner from `\|hash P`). |
| `(UUID, \|hash op-id)` | `$$enter-ops` | Single PState in group. |

## Depots

`Auth` declares **four** partitioned depots and **zero** global or internal (`:disallow`) depots. All four are consumed by the single stream topology `"auth"` with `{:start-from :beginning :retry-mode :all-after}` so local per-partition order is preserved across retries.

| Depot | Partitioner | Event types | Primary colocated PStates | PII present? | Erasure / thinning isolation |
|---|---|---|---|---|---|
| `*root-events` | `(hash-by :root-id)` (`UUID`: `:root` for 7 root writes, `:person` for `own-layer-made`) | `OwnLayerMadeEvent`, `BeginSignInEvent`, `ReplaceSecretEvent`, `EndSignInEvent`, `MakeActorEvent`, `EndActorEvent`, `OpenSessionEvent`, `CloseSessionEvent` | `$$root-data`, `$$live` (0 hops for all 8 events!) | **None** | Never touched by PII erasure; eligible for independent future thinning via `depot.max.entries.per.partition` |
| `*asks` | `(hash-by :code)` (8-char `String`) | `AskEvent`, `AnswerAskEvent` | `$$asks` (0 hops for `ask`; 1–2 hops for `answer-ask` to `H(P)`) | **None** | Never touched by PII erasure; isolated from `*root-events` so cross-task hops in `answer-ask` cannot trigger `:all-after` retries on `*root-events` |
| `*invites` | `(hash-by :claim)` (`ClaimKey`) | `InviteEvent` | `$$claims` (Step 1: 0 hops on `H(C)` to check repeat & `:claim-in-use` before touching `P`) | **Email only** (when `:claim` is `{:by :email ...}`) | Isolated low-volume depot; email claims can be scrubbed/tombstoned later via `depot-migration` without scanning `*root-events` or `*asks` |
| `*enters` | `(hash-by :account)` (`AccountKey`) | `EnterEvent` (accepted & refused alike, `SPEC.md` line 376) | `$$accounts` (0 hops for Step 1 on `H(AccountKey)`) | **Yes** (`account`, `email`, `says`) | Isolated low-volume depot; account/email/`:says` can be erased later via `depot-migration` (`DEPOT-TOMBSTONE` or redaction) or per-depot trim |

**Why four depots and zero `:disallow` internal depots:**
1. Each depot matches the primary partition key of its first processing step (`P`, `U`, `C`, `Acc`), avoiding wasted initial hops and ensuring local ordering for same-key writes.
2. Splitting `*asks` from `*root-events` keeps `*root-events` 100% single-task (0 partitioner hops), so a transient remote-task failure during `answer-ask` never triggers `:all-after` replay on `*root-events`.
3. Splitting `*invites` and `*enters` from `*root-events` and `*asks` isolates all PII (`account`, `email`, `:says`) into the two low-volume depots so future erasure never touches the high-volume non-PII logs.
4. No `:disallow` internal depot is needed because all 6 PStates are owned by the single stream topology `"auth"` and cross-key coordination executes directly via partitioners inside `"auth"`.

## Topologies and PStates

`Auth` declares **one** ETL topology — the stream topology `"auth"` — and **zero** microbatch topologies. (Query topologies are listed in the next section.)

### Topology `"auth"`: `stream`

- **Why stream (not default microbatch):** Both stream triggers from the Rama skill apply to every write of `Auth`:
  1. **Ack coordination and computed return values**: Every one of the 12 writes must return a computed decision map (`{:invited P}`, `{:person P ...}`, `{:begun G}`, `{:refused ...}`, etc.) back to the caller, AND `SPEC.md` line 93 mandates *"A write returns when its effect can be read by any copy of the runtime."* Microbatch topologies do not participate in `foreign-append! :ack` (`microbatch.md:134`) and cannot use `ack-return>` (`stream.md:250`).
  2. **Latency and single-owner PStates**: High-volume writes (`open-session`, `replace-secret`) target tens of milliseconds, whereas microbatch adds >= 300 ms batch/poll delay and global task-0 cycle coordination. Furthermore, Rama restricts each PState to one owning topology: every PState in `Auth` (`$$root-data`, `$$live`, `$$accounts`, `$$claims`, `$$asks`, `$$enter-ops`) is synchronously mutated and must be visible before the calling write returns, leaving zero asynchronous/deferrable derived views to split into a microbatch topology.
- **Why one stream topology (and not two):** Rama's topology count rule allows at most one stream topology; splitting buys zero isolation (all stream topologies share one latency profile) and permanently partitions PState write ownership (`invite`, `enter`, `answer-ask`, and the 8 `*root-events` writes all must write `$$root-data` and `$$live`).
- **Source configuration**: All 4 depots consumed with `{:start-from :beginning :retry-mode :all-after}`.
- **No `depot-partition-append!` inside `"auth"`**: Zero internal depots are used, eliminating cross-topology commit-boundary and duplicate-append risks.

### Per-concern stream check

| Processing concern | Requires stream? | Reason |
|---|---|---|
| `invite` (`$$claims`, `$$root-data`) | Yes | Must return `{:invited P}` / `{:refused ...}` via `ack-return>`, with `$$claims` and `$$root-data` visible to any copy on return. |
| `enter` (`$$accounts`, `$$claims`, `$$enter-ops`, `$$root-data`) | Yes | Must return `{:person P :own-layer L' :making ... :own-layer-made? bool}` / `{:refused :not-invited}` via `ack-return>`, with `P` entered and `Acc` linked on return. |
| `own-layer-made` (`$$root-data`) | Yes | Must return `{:made P}` / `{:refused :no-such-person}` via `ack-return>`, with `:own-layer-made? true` visible to `who-is` and `enter` on return. |
| `begin-sign-in`, `replace-secret`, `end-sign-in`, `make-actor`, `end-actor`, `open-session`, `close-session` (`$$root-data`, `$$live`) | Yes | Must return computed accept/refusal via `ack-return>`, update `$$root-data` and `$$live` atomically in tens of ms, and be visible to `who-is`, `under`, and `watch-root` on return. |
| `ask`, `answer-ask` (`$$asks`, `$$root-data`, `$$live`) | Yes | Must return `{:asked U}`, `{:yes ...}`, `{:no U}`, or `{:refused ...}` via `ack-return>`, with `ask-of`, `who-is`, `under`, and `watch-root` consistent on return. |

### Stream retry-safety and idempotency audit (every write)

| Write | Naturally idempotent or stateful? | Resolution against stream retry & partial multi-partition failure |
|---|---|---|
| `own-layer-made` | Naturally idempotent (`termval true` on `$$root-data["<P>\0r"]`) | Single-task (0 hops). Retry after commit sees `:own-layer-made? true` and writes `true` again -> `{:made P}`. |
| `make-actor` | **Non-idempotent if naive** (increments `:next-actor-seq` and writes `ao:<seq>`) | **Deduplicated by entity key `a:<A>`**: single-task atomic batch (0 hops). On retry after commit, Step 1 finds existing `a:<A>` with matching `:made-by B` (repeat rule) and returns `{:made A}` without incrementing `:next-actor-seq` or writing a second `ao:` entry. On crash before commit, all writes discard atomically. |
| `begin-sign-in` | **Non-idempotent if naive** (increments `:next-sign-in-seq`) | **Deduplicated by entity key `g:<G>` + immutable `:initial-hash`**: single-task atomic batch (0 hops). On retry after commit (even after later `replace-secret` or `end-sign-in`), Step 1 matches `(actor, kind, initial-hash)` on `g:<G>` and returns `{:begun G}` without incrementing `:next-sign-in-seq`. |
| `replace-secret` | Stateful (rotates `hash` and `prev-hash`) | **Made idempotent by exact `(old-hash, new-hash)` check**: single-task atomic batch (0 hops). On retry after commit, `hash == H'` and `prev-hash == H` matches the repeat rule and returns `{:replaced G}` with no mutation. If a newer rotation already ran, returns `:wrong-secret` with no mutation. |
| `end-sign-in`, `close-session`, `end-actor` | Naturally idempotent (monotonic transition to ended/closed + `NONE>` on `$$live` / `exp:` / `ag:` / `sg:`) | Single-task atomic batch (0 hops; no yield inside `end-actor`). Retry after commit sees already ended/closed and returns `{:ended G}` / `{:closed S}` / `{:ended A}` with no mutation. |
| `open-session` | Naturally idempotent (monotonic `max` on `:until`, immutable `:driven-by`, closed never reopens) | Single-task atomic batch (0 hops). Retry after commit sees `s:<S>` with same `G`; if `t1 <= current-until` or closed, no mutation -> `{:open S}`. |
| `ask` | Naturally idempotent (`termval` on `$$asks[U]` if absent) | Single-task atomic batch (0 hops). Retry after commit sees `$$asks[U]` with same `H` -> `{:asked U}` with no mutation. |
| `invite` | Multi-partition (hops `H(C) <-> H(P)`) | **Made idempotent by monotonic state machine + wait-free helping**: `$$claims[C]` transitions monotonically `nil -> ClaimWaiting(P) -> ClaimUsed(P)` and `$$root-data["<P>\0r"]` transitions `nil -> RootReserving(C) -> RootInvited(C) -> RootEntered(C)` (detailed trace in Deep-dive §1). Retry from start at any hop resumes and converges to the exact committed state. |
| `enter` | Multi-partition (hops `H(Acc) <-> H(C) <-> H(op) <-> H(P)`) | **Made idempotent by monotonic state machine + `op-id` ownership + wait-free helping**: `$$accounts[Acc]` records `AccountLinking(op-id)` before touching claims; `$$claims[C]` records `op-id` in `ClaimClaiming`/`ClaimUsed`; 2-claim enters commit via `$$enter-ops[op-id]` (detailed trace in Deep-dive §1). Retry at any hop converges to one person, one own-layer, one `:making`. |
| `answer-ask` | Multi-partition (hops `H(U) <-> H(P)`) | **Made idempotent by `AskAnswering(op-id)` + per-attempt `$$root-data["<P>\0ok:<op-id>"]` outcome record**: `H(P)` evaluates `op-id` once (recording `AskOutcomeYes` / `AskOutcomeNo` / `AskOutcomeRefused` atomically with `a:<A>`/`g:<G>` creation and `:next-actor-seq`/`:next-sign-in-seq` increment). Any retry or helper arriving at `H(P)` replays the recorded outcome with zero duplicate mutations (detailed trace in Deep-dive §1). |
| Take-outs (on `:root` writes) | Monotonic (`NONE>` on `exp:`, `$$live`, `ag:`, `sg:`; `ended?/closed? = true`) | Ending an already-ended item is a no-op. Note on pathological clock skew under uncheckpointed stream replay: analyzed explicitly in `Walls` (item 3). |

### PStates owned by `"auth"` (full typed schemas, zero `Object`)

- `$$root-data`: `{String -> IRootDatum}` (`RootReserving`, `RootInvited`, `RootEntered`, `ActorRec`, `ActorOrderRec`, `ChildEdge`, `SignInRec`, `ActorSignInEdge`, `SessionRec`, `SignInSessionEdge`, `ExpiryEntry`, `AskOutcomeYes`, `AskOutcomeNo`, `AskOutcomeRefused`)
- `$$live`: `{String -> ILiveEntry}` (`LiveSignIn`, `LiveSession`); launch option `pstate.proxy.limit = 20000`
- `$$accounts`: `{AccountKey -> IAccountState}` (`AccountLinking`, `AccountLinked`)
- `$$claims`: `{ClaimKey -> IClaimState}` (`ClaimWaiting`, `ClaimClaiming`, `ClaimUsed`)
- `$$asks`: `{String -> IAskState}` (`AskWaiting`, `AskAnswering`, `AskYes`, `AskNo`)
- `$$enter-ops`: `{UUID -> IEnterOpState}` (`EnterOpTryEmail`, `EnterOpWon`, `EnterOpFailed`)

## Query Topologies

`Auth` declares **three** query topologies (`"who-is"`, `"under"`, `"ping-task"`). All three begin with a built-in single-task leading partitioner using only topology input arguments so Rama evaluates routing client-side and sends a single network round trip straight to the target task (`query-topologies.md:120`). `ask-of` uses direct `foreign-select-one` (1 key, 1 PState) rather than a query topology.

### 1. `"who-is"` `[*root *sign-in *session :> *result]`

- **Leading partitioner**: `(|hash *root)` — 1 round trip directly to `H(*root)`, ends with `(|origin)`.
- **Pre-agg only (no aggregator)**: produces exactly one result map per invocation; no yielding (strictly bounded 1–4 point seeks).
- **Dynamic conditional reads (`<<if`)**:
  1. Always read `$$root-data["<P>\0r"]` (1 seek). If absent or not `RootEntered` (unknown or waiting person), **short-circuit immediately**: skip sign-in, session, and actor reads and emit `{:sign-in nil :session nil :own-layer nil :own-layer-made? false}`.
  2. When `RootEntered`: read `$$root-data["<P>\0g:<G>"]` (1 seek).
  3. Only when `*session` is non-nil: read `$$root-data["<P>\0s:<S>"]` (1 seek). When `*session` is nil, bind `nil` with 0 reads.
  4. Only when sign-in `G` exists, `(:ended? G)` is `true`, AND `(:actor G) != *root`: read `$$root-data["<P>\0a:<A>"]` (1 seek) to obtain `:actor-ended?`. (When `G` is live, `end-actor` guarantees its actor is live -> `:actor-ended? false` with 0 reads; when `(:actor G) == *root`, a person never ends -> `:actor-ended? false` with 0 reads.)
- **Concrete input examples (`N` total reads vs `M` meaningful reads):**
  - *Example 1* — Unknown or waiting root `P`, any `G`, `S = S1`: reads `r` (not entered -> short-circuit) -> **N = 1, M = 1** (`N == M`).
  - *Example 2* — Entered root `P`, live sign-in `G` (human or agent), `S = nil`: reads `r` + `g:<G>` -> **N = 2, M = 2** (`N == M`).
  - *Example 3* — Entered root `P`, live sign-in `G` (human or agent), `S = S1` (dominant HTTP request): reads `r` + `g:<G>` + `s:<S1>` -> **N = 3, M = 3** (`N == M`).
  - *Example 4* — Entered root `P`, ended agent sign-in `G` (`A != P`), `S = S1`: reads `r` + `g:<G>` + `s:<S1>` + `a:<A>` -> **N = 4, M = 4** (`N == M`).
- **Fixed or variable**: **Variable** (`M ∈ {1, 2, 3, 4}`).
- **Dynamic approach**: Sequential `<<if` guards on `RootEntered`, `(some? *session)`, and `(and> (some? *sig) (:ended? *sig) (not= (:actor *sig) *root))` ensure zero wasted seeks across every input class.

### 2. `"under"` `[*root :> *result]`

- **Leading partitioner**: `(|hash *root)` — 1 round trip directly to `H(*root)`, ends with `(|origin)`.
- **Pre-agg only (no aggregator)**: produces one `{:actors [...] :sign-ins [...]}` map per invocation.
- **Dynamic conditional reads (`<<if`)**:
  1. Always read `$$root-data["<P>\0r"]` (1 seek). If absent or not `RootEntered`, short-circuit and return `{:actors [] :sign-ins []}`.
  2. Only when `(:next-actor-seq r) > 0`: range-scan `$$root-data` on `(sorted-map-range "<P>\0ao:" "<P>\0ao;")` with `{:allow-yield? true}` (1 seek + `A` iter-reads over a consistent snapshot, preventing a root with thousands of agents from stalling `who-is` on the task thread). When `next-actor-seq == 0`, bind `[]` with 0 reads.
  3. Only when `(:next-sign-in-seq r) > 0`: range-scan `$$live` on `(sorted-map-range "<P>\0g:" "<P>\0g;")` (1 seek + `L` iter-reads) and sort by `:seq` in memory. When `next-sign-in-seq == 0`, bind `[]` with 0 reads.
- **Concrete input examples (`N` total reads vs `M` meaningful reads):**
  - *Example 1* — Unknown or waiting root `P`: reads `r` only -> **N = 1, M = 1** (`N == M`).
  - *Example 2* — Newly entered root `P` (0 agents, 0 sign-ins ever): reads `r` only (`next-actor-seq = 0`, `next-sign-in-seq = 0`) -> **N = 1, M = 1** (`N == M`).
  - *Example 3* — Entered root `P` with 0 agents and 3 live sign-ins: reads `r` + `$$live` `g:` range -> **N = 2, M = 2** (`N == M`).
  - *Example 4* — Entered root `P` with 250 agents (live + ended) and 8 live sign-ins: reads `r` + `ao:` range (1 seek + 250 iters) + `$$live` `g:` range (1 seek + 8 iters) -> **N = 3, M = 3** (`N == M`).
- **Fixed or variable**: **Variable** (`M ∈ {1, 2, 3}`).
- **Dynamic approach**: `<<if` guards on `RootEntered`, `(pos? next-actor-seq)`, and `(pos? next-sign-in-seq)`.

### 3. `"ping-task"` `[*task-id :> *ok]`

- **Leading partitioner**: `(|direct *task-id)` — 1 round trip directly to `*task-id`, ends with `(|origin)`.
- **Purpose**: Lightweight in-memory liveness probe used by `softland.auth.client`'s background watch monitor (every 2 s, only when >= 1 watch is open) to detect task/worker/module loss within seconds for `:softland.auth.client/not-reached`.
- **Concrete input examples**:
  - *Example 1* — `*task-id = 0`: reads `(ops/current-task-id)` in memory -> **N = 0, M = 0** (`N == M`).
  - *Example 2* — `*task-id = 15`: reads `(ops/current-task-id)` in memory -> **N = 0, M = 0** (`N == M`).
- **Fixed or variable**: **Fixed** (`M = 0` PState reads on every input).

## Partitioning efficiency

### Optimal placement derivation (`f(key) -> task(s)`)

The dominant read by orders of magnitude is `who-is(P, G, S)`, issued on every HTTP request and connection open (`SPEC.md` line 386). Its inputs always name a root `P` (UUID), a sign-in `G` under `P`, and an optional session `S` under `P`. `SPEC.md` requires:
1. `who-is` is **one network round trip to one task** (`line 387`).
2. **No one task that everything passes through** (`line 382`).
3. Events of one root `P` (including take-outs and 32-deep `end-actor` cascades) are decided in one atomic order (`line 406`).

Therefore the optimal placement `f` maps **every key belonging to root `P`** (`r`, `a:<A>`, `ao:<seq>`, `ch:<B>:<A>`, `g:<G>`, `ag:<A>:<G>`, `s:<S>`, `sg:<G>:<S>`, `exp:...`, `ok:<op>`, and `$$live` entries for `P`) to **exactly one task `f(P) = {t_P}`**, distributed uniformly across all `N` tasks:
- **Why not `|all` (`f(P) = {0..N-1}`)?** At millions of persons and high `open-session` / `replace-secret` write rates, replicating every root write to all `N` tasks multiplies write CPU and disk usage by `N` (anti-scalable) and cannot satisfy atomic per-root take-outs or 20,000-per-task `watch-root` bounds.
- **Why not splitting `P`'s sign-ins/sessions across multiple tasks via `|direct` or stored placement?** Splitting `g:<G>` and `s:<S>` onto different tasks than `r` forces `who-is` into 2–3 cross-task hops (violating "one task"), breaks single-task atomicity for take-outs and `end-actor` cascades, and adds a placement-table lookup seek (+1 seek on every `who-is` and write, raising weighted seeks from 2.89 to 3.89+ with zero latency benefit).
- **Why `|hash` implements `f` cleanly for all five keyspaces (`P`, `Acc`, `C`, `U`, `op-id`)**:
  - **Keyspace cardinality**: Millions of random UUIDs (`P`, `op-id`), millions of accounts (`Acc`) and claims (`C`), unbounded 8-char codes (`U`). At `N = 128`, `M / N` is `>= 10^4`, so balls-into-bins hash variance `√(2·ln N / (M/N))` is `< 3%` — negligible.
  - **Per-key hot-spot check**: Under `SPEC.md`, one root has tens of live sign-ins and tens–hundreds of open sessions (~20 KB total hot state). Even if a single power-user root receives thousands of `who-is`/sec from a fleet of agents, its active `r`, `g:`, and `s:` keys stay resident in RocksDB's 256 MB block cache on `t_P`, and `who-is` touches `0` history rows and `0` iterator reads. No global singleton key exists anywhere in the module.

### Validation tables for dominant read `who-is` (`N = 1`, `N = 16`, `N = 128` tasks)

Every invocation routes directly to `1` task via leading `(|hash *root)` (total tasks touched across the cluster = `1` in all rows and at all `N`). Both **Total tasks touched/op** and **Total RocksDB seeks/op** (summed across the whole cluster) are tabulated below.

#### N = 1 task (single-task baseline)

| Data category | Frequency proportion | Tasks touched/op | Total RocksDB seeks/op | Iterator reads/op |
|---|---|---|---|---|
| Entered root, live sign-in (human or agent), `S != nil` | 0.85 | 1 | 3 | 0 |
| Entered root, live sign-in (human or agent), `S = nil` | 0.10 | 1 | 2 | 0 |
| Entered root, ended agent sign-in (`A != P`), `S != nil` | 0.03 | 1 | 4 | 0 |
| Unknown or waiting root `P` (short-circuit on `r`) | 0.02 | 1 | 1 | 0 |

**Weighted tasks touched = 1.00  |  Weighted RocksDB seeks = 2.89  |  Weighted iterator reads = 0.00**
*(Calculation: `0.85×3 + 0.10×2 + 0.03×4 + 0.02×1 = 2.55 + 0.20 + 0.12 + 0.02 = 2.89`.)*

#### N = 16 tasks

| Data category | Frequency proportion | Tasks touched/op | Total RocksDB seeks/op | Iterator reads/op |
|---|---|---|---|---|
| Entered root, live sign-in (human or agent), `S != nil` | 0.85 | 1 | 3 | 0 |
| Entered root, live sign-in (human or agent), `S = nil` | 0.10 | 1 | 2 | 0 |
| Entered root, ended agent sign-in (`A != P`), `S != nil` | 0.03 | 1 | 4 | 0 |
| Unknown or waiting root `P` (short-circuit on `r`) | 0.02 | 1 | 1 | 0 |

**Weighted tasks touched = 1.00  |  Weighted RocksDB seeks = 2.89  |  Weighted iterator reads = 0.00**

#### N = 128 tasks

| Data category | Frequency proportion | Tasks touched/op | Total RocksDB seeks/op | Iterator reads/op |
|---|---|---|---|---|
| Entered root, live sign-in (human or agent), `S != nil` | 0.85 | 1 | 3 | 0 |
| Entered root, live sign-in (human or agent), `S = nil` | 0.10 | 1 | 2 | 0 |
| Entered root, ended agent sign-in (`A != P`), `S != nil` | 0.03 | 1 | 4 | 0 |
| Unknown or waiting root `P` (short-circuit on `r`) | 0.02 | 1 | 1 | 0 |

**Weighted tasks touched = 1.00  |  Weighted RocksDB seeks = 2.89  |  Weighted iterator reads = 0.00**

**Verdict**: Both weighted tasks touched (`1.00`) and weighted RocksDB seeks (`2.89`) remain completely flat from `N = 1` to `N = 128`. Adding hardware scales aggregate throughput linearly with zero per-request overhead growth.

### Write partitioner justification summary

| Depot / PState write | Partitioner | Justification |
|---|---|---|
| `*root-events` -> `$$root-data`, `$$live` | `(hash-by :root-id)` / `\|hash P` | Millions of UUID roots; 0 hops for all 8 root-scoped writes; single-task atomicity for take-outs and 32-deep cascades. |
| `*asks` -> `$$asks` (and `\|hash P` for `answer-ask`) | `(hash-by :code)` / `\|hash U` | Unbounded 8-char random codes; 0 hops for `ask` and `ask-of`; 1–2 hops for low-volume `answer-ask`. |
| `*invites` -> `$$claims` (and `\|hash P`) | `(hash-by :claim)` / `\|hash C` | Millions of email/code claims; 0 hops to reject `:claim-in-use` in `SPEC.md` refusal order before touching `P`. |
| `*enters` -> `$$accounts` (and `\|hash C`, `\|hash op`, `\|hash P`) | `(hash-by :account)` / `\|hash Acc` | Millions of accounts; 0 hops to detect returning account, 1 hop to `H(P)` for `:own-layer-made?`. |
| `$$enter-ops` | `\|hash op-id` | Random UUID per 2-claim first-time `enter`; uniform distribution; keeps PII out of `$$claims`. |

## Design Decisions

### 1. Top-level RocksDB indexing (zero un-subindexed unbounded collections)
Every collection that can grow per root or globally — actors (`a:`, `ao:`, `ch:`), sign-ins (`g:`, `ag:`), sessions (`s:`, `sg:`), expiries (`exp:`), ask outcomes (`ok:`), live sign-ins and open sessions (`$$live`), accounts (`$$accounts`), claims (`$$claims`), asks (`$$asks`), and 2-claim enter coordinators (`$$enter-ops`) — is stored as individually indexed entries in top-level RocksDB-backed PState maps. There are **no** nested subindexed maps (avoiding the extra parent seek per lookup) and **only one** non-subindexed inner map anywhere in the schema: `:says` inside `AccountLinking`/`AccountLinked`, whose size is explicitly enforced to **at most 32 entries** by client input validation before append (`SPEC.md` line 101–103; `< 50` entries, so subindexing would only add overhead).

### 2. Colocation between depots and PStates
- `*root-events` (`(hash-by :root-id)`) colocates with `$$root-data` and `$$live` (`|hash P`): all 8 single-root writes (`own-layer-made`, `begin-sign-in`, `replace-secret`, `end-sign-in`, `make-actor`, `end-actor`, `open-session`, `close-session`) execute with **0 partitioner hops** and complete in a single atomic streaming batch on `H(P)`.
- `*asks` (`(hash-by :code)`) colocates with `$$asks` (`|hash U`): `ask` executes with **0 hops**; `answer-ask` checks `U` on `H(U)` with **0 initial hops** before hopping to `H(P)`.
- `*invites` (`(hash-by :claim)`) colocates with `$$claims` (`|hash C`): `invite` checks repeat and `:claim-in-use` on `H(C)` with **0 initial hops**, matching `SPEC.md`'s refusal order before hopping to `H(P)`.
- `*enters` (`(hash-by :account)`) colocates with `$$accounts` (`|hash Acc`): returning users resolve their link on `H(Acc)` with **0 initial hops** and pay only 1 hop (`H(P)`) to read `:own-layer-made?`.

### 3. Input validation at the client edge (`:read-the-event`)
`softland.auth.client` validates every argument of every write and read **before** appending to any depot or invoking Rama (`SPEC.md` line 103–105: invalid write writes nothing, not even an event):
- **Map shape**: every write/read input map must be a Clojure map with required keys present; unknown keys on top-level call maps and sub-maps (`:claim`, `:making`, `:new-agent`, `:driven-by`) are rejected with `{:could-not :read-the-event}` so typos never silently drop fields (see `A session's choices`).
- **Times** (`:at`, `:until`, `:previous-until`, `:ends`): JVM fixed-width integer (`Byte`, `Short`, `Integer`, `Long`; rejects `BigInt`, `BigInteger`, floats, nil) coerced to exact `Long` before depot append (preventing Rama's exact-class schema mismatch between `Integer` and `Long`, `pstate-schema.md:191`).
- **IDs** (`:person`, `:root`, `:actor`, `:made-by`, `:sign-in`, `:session`, `:own-layer`, `:making` `:uuid` and `:session`): `java.util.UUID`.
- **Hashes** (`:hash`, `:old-hash`, `:new-hash`, `:code-hash` when non-nil, code-claim `:hash`): `String` of length exactly 64 matching `[0-9a-f]{64}` (lower-case hex only).
- **Codes** (`:code`, `ask-of` `U`): `String` of length exactly 8 (`char` count).
- **Texts** (`:label`, `:asks`, `:email`, provider `subject`, `:driven-by` `:model`): `String` with `(.length s) <= 256` (JVM `char`s). Note: `:label` on `make-actor` and `:new-agent` is non-nil; `:label` on `begin-sign-in` is `text-or-nil`.
- **Provider** (first element of `:account [provider subject]`): `clojure.lang.Keyword` whose full name (including namespace + `/` if qualified) has `(.length ...) <= 256` (`SPEC.md` line 360).
- **`:says`**: map with `<= 32` entries, every key a `String` of `<= 256` chars and every value a `String` of `<= 2,048` chars.
- **`:claim`**: `{:by :email :email text}` or `{:by :code :hash 64-lower-hex}` (no other `:by` values, no nil value).
- **`:invited-by`**: `java.util.UUID` or `:system`.
- **`:kind`**: `:browser` or `:program`.
- **`:driven-by`** (optional on `open-session`): absent/`nil`, `{:kind :person}` (no extra keys), or `{:kind :model :model text}` (with `:model` a `String` of `<= 256` chars and no extra keys).
- **`:yes?`**: `Boolean`. When `:yes? false` on `answer-ask`, `:code`, `:yes?`, `:root`, and `:at` are required; `:actor`, `:sign-in`, `:until`, and `:new-agent` may be `nil`/absent (if present and non-nil, their types are still validated — recorded in `A session's choices`).

### 4. Error handling, `open`, `close!`, and callback discipline
- **No call throws**: every public function in `softland.auth.client` wraps Rama interaction in `try/catch Throwable`, mapping missing module on `open` to `{:could-not :find-the-module :module ... :error ...}`, closed client to `{:could-not :closed}`, invalid input to `{:could-not :read-the-event ...}`, and any Rama/network/timeout failure to `{:could-not :reach-the-module :error ...}`. Every blocking Rama call uses a bounded timeout (`CompletableFuture/get` with e.g. 10 s default).
- **Client-side watch deduplication & incremental projection**: `softland.auth.client` keeps one shared `foreign-proxy-async` per watched root `P`, maintains the projected `{:sign-ins {...} :sessions {...}}` map incrementally from `KeyDiff` / `KeyRemoveDiff` / `MultiDiff` (rebuilding from the full proxy map only on `ResyncDiff` / `NewValueDiff`) so callback work on Rama's 2-thread proxy pool is O(changed entries) rather than O(|live|).
- **Callback isolation & `close!` safety**: exceptions thrown by a caller's `f` are caught per-watcher so they stop neither later calls to that watcher nor other watchers of `P` (`IMPLICIT_SPEC.md:812`). A `ThreadLocal` marks Rama's callback thread while invoking `f`, preventing deadlocks if `f` invokes its own stop function or `close!`.

### 5. Cooperative multitasking / yielding policy
- **Writes on `H(P)` (including `end-actor` 32-deep cascade)**: deliberately **never yield** (`no yield-if-overtime`, `no :allow-yield? true`). Per `dataflow.md:156` and `199`, yielding breaks same-key ordering and read-to-write atomicity, which would let a concurrent `make-actor` or `begin-sign-in` interleave mid-cascade and leave a live child under an ended parent. Worst-case `end-actor` (thousands of actors) completes locally in sub-second time (`IMPLICIT_SPEC.md:522` permits seconds).
- **`"who-is"` query topology**: never yields (1–4 point seeks, 0 scans).
- **`"under"` query topology**: uses `{:allow-yield? true}` on the `ao:` range scan (up to thousands of agents). Because `"under"` is read-only and Rama's yielded select executes over a consistent PState snapshot (`dataflow.md:195`), yielding keeps `"who-is"` latency flat even during large `"under"` reads.

## State primitive selection

Every piece of durable Auth state lives in partitioned Rama PStates; the Rama module uses **zero `TaskGlobal`s** and **zero external systems**.

- **`$$root-data` (PState, durable)**: Primary source of truth for per-root person state, actors, sign-ins, sessions, ask outcomes, and live-lookup/expiry indexes. Per-source-event write volume: `O(1)` for `own-layer-made`, `make-actor`, `begin-sign-in`, `replace-secret`, `open-session`, `close-session`, `invite`, `enter`; `O(expired)` for take-outs; `O(open sessions of G)` for `end-sign-in`; `O(live subtree of A + live sign-ins + open sessions)` for `end-actor` (bounded by live entities beneath `A`, never by history).
- **`$$live` (PState, durable)**: Materialized live-only view of each root's live sign-ins and open sessions for `watch-root` (`foreign-proxy-async`) and `under`'s live sign-ins. Per-source-event write volume: `O(1)` per live sign-in/session mutation, `O(expired)` on take-out, `O(live items ended)` on `end-sign-in`/`end-actor`. Durable so watches immediately see exact live state across worker restarts and leader failovers without rebuilding from depots.
- **`$$accounts` (PState, durable)**: Primary source of truth for `[provider subject] -> person` links, fixed `:own-layer`/`:making`, and latest `:says`. Per-source-event write volume: `O(1)` per `enter` (0 writes on refused single-claim/no-claim enters after cleanup).
- **`$$claims` (PState, durable)**: Primary source of truth for claim (`:email` or `:code`) -> waiting/used person binding. Per-source-event write volume: `O(1)` per `invite` or `enter`.
- **`$$asks` (PState, durable)**: Primary source of truth for device-grant asks by 8-char code. Per-source-event write volume: `O(1)` per `ask` or `answer-ask`.
- **`$$enter-ops` (PState, durable)**: Durable commit-decision state for 2-claim first-time `enter` so cross-partition retries and concurrent helpers converge deterministically even across worker restarts. Per-source-event write volume: `O(1)` (0 writes for returning users and 1-claim enters).
- **Module `TaskGlobal`s: None.** No in-memory cache is placed inside the Rama module. Per `AGENTS.md` ("Systemic repair and caching"), `who-is` already achieves `1–4` direct RocksDB seeks (~1–2 ms, well within single-digit ms latency) with active keys naturally served by RocksDB's block cache; an in-memory `TaskGlobal` cache would add heap/GC pause risk on latency-sensitive task threads without solving any latency violation.
- **Client-side ephemeral subscription registry (`softland.auth.client`, non-durable in-JVM state)**: Holds only active `watch-root` listener maps, one shared Rama `ProxyState` handle per `(client, root P)` plus its incrementally maintained projected map, a closed flag, and the scheduled `ping-task` liveness handle. Holds zero Auth truth (`SPEC.md` line 21: "each [runtime copy] holds no state of its own"); lost cleanly on runtime process exit, with Rama expiring server-side proxy subscriptions via heartbeat timeout (`15-pstates.md:728`). (Per Sid's 11 October ruling in `src-store/README.md`, Rama's `ProxyState` is live push state, not a cache.)

## Resource usage analysis

Sized at a representative full-life deployment of **1,000,000 persons** across **N = 64 tasks** (~15,625 persons/task), assuming an average of 5 lifetime agents/person (with power users up to thousands), 50 lifetime sign-ins/person (10 live), 500 lifetime sessions/person (20 open; busy agent roots up to millions of closed sessions), and 20 asks/person.

### Disk usage (PStates and depots)

| Store | Entry key + value size (serialized + ~24 B RocksDB overhead) | Growth rate | Est. entries/task (1M persons, N=64) | Est. disk/task |
|---|---|---|---|---|
| `$$root-data` (`r`) | 38 B key + ~180 B `RootEntered` = ~240 B | 1/person (bounded by persons) | 15,625 | ~3.8 MB |
| `$$root-data` (`a:`, `ao:`, `ch:`) | `a:`: ~160 B; `ao:`: ~140 B; `ch:`: ~115 B (~415 B/agent total) | 1/agent made (up to thousands/person) | ~78,125 agents | ~32 MB |
| `$$root-data` (`g:`) | 75 B key + ~300 B `SignInRec` (with `initial-hash`, `hash`, `prev-hash`, `label`) = ~400 B | Unbounded over time (ended kept) | ~781,250 | ~312 MB |
| `$$root-data` (`s:`) | 75 B key + ~105 B `SessionRec` (with `driven-by`) = ~205 B | Unbounded over time (highest history growth; busy agents: thousands/day) | ~7.8M (500/person) to ~156M (10k/person) | ~1.6 GB (to ~32 GB heavy agent fleet) |
| `$$root-data` (`ok:`) | 76 B key + ~75 B `AskOutcome*` = ~175 B | 1/answered ask attempt | ~312,500 | ~55 MB |
| `$$root-data` (`exp:`, `ag:`, `sg:`) | ~110 B each (live items only; `NONE>` removed on end/close) | Bounded by live sign-ins + open sessions | ~1.1M live index keys (~470k live items × ~2.3 keys) | ~120 MB |
| `$$live` (`g:`, `s:`) | `LiveSignIn`: ~310 B; `LiveSession`: ~125 B | Strictly bounded by live sign-ins (tens/root) + open sessions (tens–hundreds/root) | ~156k sign-ins + ~312k sessions | ~88 MB (stays hot in 256 MB RocksDB block cache) |
| `$$accounts` | ~60 B `AccountKey` + ~600 B `AccountLinked` (typical `:says`; max ~75 KB if `:says` is 32 × 2 KB) = ~685 B avg | 1/entered person (refused enters write 0) | 15,625 | ~11 MB |
| `$$claims` | ~50 B `ClaimKey` + ~75 B `Claim*` = ~150 B | 1/invited person | 15,625 | ~2.3 MB |
| `$$asks` | 8 B key + ~200 B `Ask*` = ~235 B | Unbounded over time | ~312,500 | ~73 MB |
| `$$enter-ops` | 16 B UUID key + ~32 B `EnterOp*` = ~72 B | Only 2-claim first-time `enter`s (<= 1/person) | <= 15,625 | <= 1.1 MB |
| Depots (`*root-events`, `*asks`, `*invites`, `*enters`) | ~120–300 B/event (`*enters` ~650 B avg with `:says`) | Append-only until future thinning/migration (`*root-events` dominates via `open-session` + `replace-secret`) | ~20M–200M events/task over years | ~3–30 GB/task (independent per-depot trim possible later) |

### Memory usage (TaskGlobals and runtime/server proxy footprint)

- **Module `TaskGlobal`s**: **0 bytes** — no `TaskGlobal` is declared in `Auth`. Zero custom heap or GC pause pressure added to Rama task threads.
- **Server-side `$$live` `ProxyState` tracking (at 20,000 watched roots/task)**: Each active range proxy holds a small subscription entry and range predicate on the `$$live` task partition (~1–2 KB/subscription -> ~20–40 MB/task at 20,000 active watches). Rama's `worker.child.opts` default `-Xmx4096m` comfortably accommodates this alongside RocksDB's 256 MB off-heap block cache.
- **Client-side watch registry (`softland.auth.client`, per runtime JVM copy)**: One shared Rama `ProxyState` + projected Clojure map per locally watched root `P` (not per watcher callback). For a root with 10 live sign-ins (~2.5 KB) and 100 open sessions (~8 KB), total client memory is ~15–20 KB per watched root (~200 MB for 10,000 active watched roots on one runtime copy). All client structures are flat maps of small immutable records/maps, updated incrementally on `KeyDiff`/`KeyRemoveDiff` to minimize allocation and GC pressure on Rama's 2-thread callback pool.

### Minimization

- **No redundant caches**: Zero module-level caches; `who-is` reads directly from `$$root-data` in 1–4 point seeks (~1–2 ms).
- **Primitive / flat representations**: Timestamps (`Long`), sequence numbers (`Long`), booleans (`Boolean`), and `IDrivenBy` / `IRootDatum` / `ILiveEntry` records use flat fields rather than nested maps of boxed metadata. `:says` is stored only in `$$accounts` (and `*enters`), never copied to `$$root-data` or `$$live`.
- **Every data duplication is quantified and justified**:
  1. `$$live` duplicates live fields of `g:` and `s:` from `$$root-data`: required to separate hot watched state (`pstate.proxy.limit = 20000`, `watch-root` range scan, `under` live sign-ins) from millions of cold history entries (`SPEC.md` line 400–401). Eliminating `$$live` would force `watch-root` to scan/proxy unbounded history or pay 4–5× proxy-dispatch CPU on every history/index write.
  2. `ao:` duplicates `{actor, made-by, label, ended?}` from `a:`: reduces `under(P)` for 1,000 agents from 1,000 point seeks (`~500 ms`) to 1 range scan (`~5.5 ms`, **90× faster**) at the cost of 1 extra write per rare `make-actor`/`end-actor`.
  3. `exp:`, `ag:`, `sg:`, `ch:` are lightweight structural indexes (~110 B each, removed on end/close except `ch:`): reduce take-out from O(all live items) to O(expired items) (`0.5 ms` when nothing expired) and reduce `end-sign-in` / `end-actor` from scanning unbounded history to O(live items actually ended).
  4. `$$root-data` (`"<P>\0r"`) stores `:linked-provider`/`:linked-subject` and `:claim-kind`/`:claim-val` alongside `$$accounts` and `$$claims`: costs ~80 B/person and enables O(1) future PII erasure by person ID `P` without full-table scans of `$$accounts` or `$$claims`.

## Deep-dive settlements and traces

### 1. Agreement across keys

All three multi-key writes (`invite`, `enter`, `answer-ask`) execute inside the single stream topology `"auth"` using **monotonic state machines with wait-free helping**: no global coordinator task, no spin-wait/polling loops, no stuck reservations after a worker crash, and zero leaked mutations from losing or refused attempts. Every task is single-threaded, so each local read-check-write segment between partitioner hops is atomic; every cross-task hop flushes and replicates the prior task's PState writes before the child event executes on the destination task (`stream.md:41`), and `:ack` with `ack-return>` returns to the caller only after the entire event tree has completed and replicated on every task it touched (`14-depots.md:206`).

#### 1A. `invite` (Claim `C` on task `H(C)`, Person `P` on task `H(P)`)

**Protocol and safety invariant:**
- `$$claims[C]` transitions monotonically: `nil -> ClaimWaiting(P) -> ClaimUsed(P)`. Once bound to `P`, `C` never unbinds and never changes `:person`.
- `$$root-data["<P>\0r"]` transitions: `nil -> RootReserving(C) -> RootInvited(C) -> RootEntered(C)`, OR `RootReserving(C) -> nil / RootReserving(C')` **only after observing `$$claims[C].person != P`**. (If an `enter` beats Step 4, `RootReserving(C) -> RootEntered(C)` directly.)
- **Commit rule**: An event writes `ClaimWaiting(P)` into `$$claims[C]` only after observing `RootReserving(C)` on `H(P)` and currently observing `$$claims[C] == nil`.
- **Single-claim safety proof**: `P` can never commit two claims `C1 != C2`. To leave `RootReserving(C1)` for `RootReserving(C2)`, an event must first observe `$$claims[C1].person != P`; by monotonicity of `$$claims[C1]`, `C1` can never have been or later become `ClaimWaiting(P)`.
- **Normal path (3 hops)**:
  - **Step 1 (`H(C)`, 0 hops from `*invites`)**: Read `$$claims[C]`. If present with `:person == P`, hop `|hash P` to Step 4 (ensuring `RootInvited`/`RootEntered`, returning `{:invited P}`). If present with `:person != P`, return `(ack-return> {:refused :claim-in-use})` immediately (0 hops; enforces `SPEC.md` refusal order `:claim-in-use` before `:person-in-use`). If `nil`, hop `|hash P` to Step 2.
  - **Step 2 (`H(P)`)**: Read `$$root-data["<P>\0r"]`:
    - `RootInvited` / `RootEntered`: if same `C`, `(ack-return> {:invited P})`; else `(ack-return> {:refused :person-in-use})`.
    - `nil`: write `RootReserving(C, B, t)`, hop `|hash C` to Step 3.
    - `RootReserving(C0, B0, t0)`:
      - If `C0 == C` (retry or concurrent duplicate of same `(P, C)`): hop `|hash C` to Step 3.
      - If `C0 != C` (competing invite in flight on `P`): **help `C0`** — hop `|hash C0`, read `$$claims[C0]`:
        - If `nil`: write `ClaimWaiting(P, B0, t0)` into `$$claims[C0]` (safe because nobody can clear `RootReserving(C0)` while `$$claims[C0]` is `nil`), hop `|hash P`, promote `RootReserving(C0)` to `RootInvited(C0)`, return `(ack-return> {:refused :person-in-use})`.
        - If `:person == P`: hop `|hash P`, promote `RootReserving(C0)` to `RootInvited(C0)`, return `(ack-return> {:refused :person-in-use})`.
        - If `:person != P` (`C0` lost to another person): hop `|hash P`; if still `RootReserving(C0)`, overwrite with `RootReserving(C, B, t)` and proceed to Step 3 (`|hash C`); else loop on Step 2 with the updated `r`.
  - **Step 3 (`H(C)`)**: Read `$$claims[C]`:
    - `nil`: write `ClaimWaiting(P, B, t)` (**commit point**), hop `|hash P` to Step 4.
    - `:person == P` (retry or helper already committed): hop `|hash P` to Step 4.
    - `:person != P` (another person won `C` during the hop): hop `|hash P`, clear `RootReserving(C)` back to `nil` (only if `r` is still `RootReserving(C)`), return `(ack-return> {:refused :claim-in-use})`.
  - **Step 4 (`H(P)`)**: If `r` is `RootReserving(C)` or `nil`, promote to `RootInvited(C, B, t)` (if already `RootInvited(C)` or `RootEntered(C)`, no-op). Emit `(ack-return> {:invited P})`.

**Trace I-1 — Two concurrent `invite`s on one claim `C` for two persons `P1`, `P2` (Copy 1 and Copy 2):**
1. Copy 1 appends `InviteEvent(P1, C)` and Copy 2 appends `InviteEvent(P2, C)` to `*invites`; both land on partition `H(C)`.
2. Both pass Step 1 while `$$claims[C]` is `nil`; Copy 1 hops to `H(P1)` and writes `RootReserving(C)`; Copy 2 hops to `H(P2)` and writes `RootReserving(C)`.
3. Both hop back to `H(C)` (Step 3). `H(C)` is single-threaded; say Copy 1 runs first.
4. Copy 1 sees `$$claims[C] == nil`, writes `ClaimWaiting(P1)`, hops to `H(P1)`, promotes `P1` to `RootInvited(C)`, returns `{:invited P1}`.
5. Copy 2 runs next on `H(C)`, sees `ClaimWaiting(P1)` (`P1 != P2`), hops to `H(P2)`, clears `RootReserving(C)` via `NONE>`, returns `{:refused :claim-in-use}`.
6. Result: `C -> P1`, `P1 -> C`, `P2` clean (`nil`). Neither copy sees a partial state after return.

**Trace I-2 — Two concurrent `invite`s for one person `P` on two claims `C1`, `C2` (Copy 1 and Copy 2), including race where `C1` loses to `P2`:**
1. Copy 1 (`P, C1`) passes Step 1 (`H(C1)`), reaches `H(P)` first, writes `RootReserving(C1)`, hops to `H(C1)`.
2. Copy 2 (`P, C2`) passes Step 1 (`H(C2)`), reaches `H(P)` while `P` is `RootReserving(C1)`, and hops to `H(C1)` to help `C1`.
3. *Subcase A (`C1` is free)*: Whichever of Copy 1 or Copy 2 arrives at `H(C1)` first writes `ClaimWaiting(P)` into `$$claims[C1]`. The second sees `ClaimWaiting(P)`. Copy 1 promotes `P` to `RootInvited(C1)` and returns `{:invited P}`; Copy 2 promotes `P` (idempotent) and returns `{:refused :person-in-use}`. `C2` is never written (`nil`).
4. *Subcase B (Copy 3 `invite(P2, C1)` won `H(C1)` first)*: Copy 1 sees `ClaimWaiting(P2)`, hops to `H(P)`, clears `RootReserving(C1)` (if still `C1`), returns `{:refused :claim-in-use}`. Copy 2 also sees `ClaimWaiting(P2)` at `H(C1)`, hops to `H(P)`, replaces `RootReserving(C1)` (or `nil`) with `RootReserving(C2)`, hops to `H(C2)`, commits `ClaimWaiting(P)`, confirms `RootInvited(C2)` on `H(P)`, and returns `{:invited P}`. Even if Copy 2 beats Copy 1 back to `H(P)`, Copy 1 checks `(= (:claim r) C1)` before clearing, so Copy 1 never disturbs `RootReserving(C2)`.

**Trace I-3 — Failure & retry at every hop of `invite(P, C)`:**
- *Crash after Step 2 (`RootReserving(C)` committed on `H(P)`), before Step 3*: Rama retries from Step 1 on `H(C)`. If `C` is still `nil`, Step 2 sees `RootReserving(C)` (`C0 == C`), proceeds to Step 3, commits `ClaimWaiting(P)`, confirms `RootInvited(C)` -> `{:invited P}`. If `P2` won `C` while crashed, Step 1 sees `ClaimWaiting(P2)` -> `{:refused :claim-in-use}` (any later `invite(P, C2)` sees `RootReserving(C)`, checks `H(C)`, sees `P2 != P`, and evicts it; no read exposes `RootReserving`).
- *Crash after Step 3 (`ClaimWaiting(P)` committed on `H(C)`), before Step 4*: Rama retries from Step 1 on `H(C)`, sees `ClaimWaiting(P)` (`:person == P`), hops directly to Step 4 on `H(P)`, promotes to `RootInvited(C)` (or no-ops if an `enter` already set `RootEntered(C)`), returns `{:invited P}`.
- *Crash after Step 4 before `:ack` reaches Copy 1*: Copy 1 gets `{:could-not :reach-the-module}` and retries `invite(P, C)` (from Copy 1 or Copy 2). Step 1 sees `ClaimWaiting(P)` / `ClaimUsed(P)`, Step 4 sees `RootInvited(C)` / `RootEntered(C)`, returns `{:invited P}`.
- **Read-after-write visibility**: `(ack-return> {:invited P})` executes only in Step 4 after both `$$claims[C]` and `$$root-data["<P>\0r"]` are committed and replicated; any copy calling `enter` or `invite` immediately observes both.

#### 1B. `enter` (Account `Acc` on `H(Acc)`, Claims `C_code`/`C_email` on `H(C)`, Coordinator `op` on `H(op)`, Person `P` on `H(P)`)

**Protocol and double-claim prevention:**
Each `enter` call generates a random UUID `op-id` in the client and appends `EnterEvent` to `*enters` (`hash-by :account`).
- **Step 1 (`H(Acc)`, 0 hops from `*enters`)**: Read `$$accounts[Acc]`:
  - **`AccountLinked(P, L0, M0)`** (returning user — 99.9% of enters): update `:says` and `:says-at` on `$$accounts[Acc]`, hop `|hash P` to read `:own-layer-made?` from `$$root-data["<P>\0r"]`, return `(ack-return> {:person P :own-layer L0 :making M0 :own-layer-made? flag})`. (1 hop total; touches no claim and no `$$enter-ops`.)
  - **`nil` (unlinked account)**:
    - If both `C_code` and `C_email` are `nil`: write nothing to PStates; `(ack-return> {:refused :not-invited})`. (0 hops; event remains in `*enters`.)
    - Else: write `AccountLinking(op1, C_code, C_email, L, M, S, t)` to `$$accounts[Acc]` and proceed to Step 2 for `op1`.
  - **`AccountLinking(op0, ...)`**:
    - If `op0 == op1` (Rama retry of this event): proceed to Step 2 for `op1`.
    - If `op0 != op1` (concurrent `enter` of same `Acc` from Copy 2, or prior crashed attempt): **help `op0` finish Step 2**. If `op0` wins `P0`, `Acc` becomes `AccountLinked(P0, L0, M0)`, Copy 2 updates `:says` to `S2, t2` on `H(Acc)` if decided after `op0`, reads `:own-layer-made?` on `H(P0)`, and returns `{:person P0 :own-layer L0 :making M0 :own-layer-made? flag}` — **Copy 2's own claims are never touched**. If `op0` fails (`:not-invited`), Copy 2 clears `AccountLinking(op0)` on `H(Acc)` (if still `op0`) and loops back to Step 1 for `op1`.
- **Step 2 (Claim acquisition)**:
  - **1-claim fast path** (only `C_code` OR only `C_email` is non-nil; zero `$$enter-ops` writes):
    - Hop `|hash C`, read `$$claims[C]`:
      - `ClaimWaiting(P)`: write `ClaimUsed(P, op)` directly -> go to Step 3 (Finalize `P`, `Acc`).
      - `ClaimUsed(P, used-op)`: if `used-op == op`, go to Step 3; else miss -> hop `|hash Acc`, clear `AccountLinking(op)` via `NONE>` (if still `op`), return `{:refused :not-invited}` (0 PState entries retained).
      - `ClaimClaiming(P, other-op, other-phase)` (collision with a 2-claim enter): help `other-op` via `|hash other-op` on `$$enter-ops` (see below); if `other-op` won `C`, promote `C` to `ClaimUsed(P, other-op)` and miss; if `other-op` aborted `other-phase`, overwrite `C` with `ClaimUsed(P, op)` and go to Step 3.
      - `nil`: miss -> clear `AccountLinking(op)` on `H(Acc)` via `NONE>`, return `{:refused :not-invited}`.
  - **2-claim coordinated path** (both `C_code` AND `C_email` non-nil; prevents a delayed helper or retry from claiming `C_code` after `op` has already fallen through to `C_email` while a concurrent `invite` creates `C_code`):
    - Phase `:code` (`C_code` first):
      - Hop `|hash C_code`.
      - If `ClaimWaiting(P)`: tentatively mark `ClaimClaiming(P, op, :code)`, hop `|hash op` to `$$enter-ops[op]`:
        - If `$$enter-ops[op]` is `nil`: **commit!** Write `EnterOpWon(P, :code)` -> hop `|hash C_code` to promote `ClaimUsed(P, op)` -> go to Step 3.
        - If already `EnterOpWon(P, :code)`: hop `|hash C_code` to ensure `ClaimUsed(P, op)` -> go to Step 3.
        - If `EnterOpTryEmail`, `EnterOpWon(*, :email)`, or `EnterOpFailed` (delayed helper after `:code` already missed!): **abort!** Hop `|hash C_code`, restore `ClaimClaiming(P, op, :code)` back to `ClaimWaiting(P)` (if still `op, :code`), then follow `$$enter-ops[op]`'s state.
      - If `ClaimClaiming(P, op, :code)` (same `op`): hop `|hash op` and run the same commit/abort check.
      - If `ClaimClaiming(P, other-op, other-phase)` (`other-op != op`): hop `|hash other-op` to read `$$enter-ops[other-op]`:
        - If `nil` or `EnterOpWon(P, other-phase)` (or `EnterOpTryEmail` when `other-phase == :email`): help `other-op` commit `EnterOpWon(P, other-phase)` if not yet won, hop `|hash C_code` to promote `ClaimUsed(P, other-op)` -> `op` missed `C_code`!
        - Else (`other-op` moved past `other-phase`): hop `|hash C_code`, evict stale `ClaimClaiming` and write `ClaimClaiming(P, op, :code)`, then hop `|hash op` to commit `op`!
      - If `ClaimUsed(P, op)`: already won `P` -> Step 3.
      - If `nil` or `ClaimUsed(*, other-op)`: `op` missed `C_code` -> hop `|hash op`: if `$$enter-ops[op]` is `nil`, write `EnterOpTryEmail` (**commit transition to email phase**); if already `EnterOpWon(P, ph)`, ensure won claim is `ClaimUsed` and go to Step 3; if `EnterOpFailed`, go to miss cleanup.
    - Phase `:email` (reached **only** after `$$enter-ops[op]` is durably `EnterOpTryEmail`):
      - Hop `|hash C_email` with the exact same `ClaimClaiming(P, op, :email) -> |hash op` commit check (where `EnterOpTryEmail` commits `EnterOpWon(P, :email)`).
      - On miss: hop `|hash op`, write `EnterOpFailed` (if still `EnterOpTryEmail`), hop `|hash Acc`, clear `AccountLinking(op)` via `NONE>` (if still `op`), return `{:refused :not-invited}`.
- **Step 3 (Finalize `P`, then `Acc`)**:
  - Hop `|hash P` (`$$root-data["<P>\0r"]`): if not yet `RootEntered`, transition `RootInvited`/`RootReserving` to `RootEntered` fixing `:own-layer L`, `:making {U M}`, `:own-layer-made? false`, `:linked-provider`/`:linked-subject`, `:next-actor-seq 0`, `:next-sign-in-seq 0`. Read fixed `L', M'` and present `:own-layer-made?` flag.
  - Hop `|hash Acc` (`$$accounts[Acc]`): transition `AccountLinking(op)` to `AccountLinked(P, L', M', S, t)` (if already `AccountLinked`, preserve fixed `L', M'`).
  - Emit `(ack-return> {:person P :own-layer L' :making M' :own-layer-made? flag})`.

**Trace E-1 — Two concurrent `enter`s of one new account `Acc` through Copy 1 (proving `C1 -> P1`, offer `L1, M1`) and Copy 2 (proving `C2 -> P2`, offer `L2, M2`):**
1. Both append to `*enters`, landing on partition `H(Acc)`. Copy 1 runs Step 1 first: writes `AccountLinking(op1, C1, L1, M1)` and hops to `H(C1)`.
2. Copy 2 runs Step 1 next on `H(Acc)`: sees `AccountLinking(op1, C1, L1, M1)` (`op1 != op2`). Copy 2 **helps `op1`** — hops to `H(C1)` (never touching `H(C2)`!).
3. On `H(C1)`, whichever of Copy 1 or Copy 2 arrives first marks `C1` as `ClaimUsed(P1, op1)`; the second sees `ClaimUsed(P1, op1)` (`== op1`).
4. Both proceed to `H(P1)`: first transitions `P1` to `RootEntered(L1, M1, made?=false)`; second sees `RootEntered(L1, M1)` already set.
5. Both proceed to `H(Acc)`: first sets `AccountLinked(P1, L1, M1)`; second sees `AccountLinked(P1, L1, M1)` (updates `:says` if Copy 2 is the later enter).
6. Both return `{:person P1 :own-layer L1 :making M1 :own-layer-made? false}`. `C2` remains `ClaimWaiting(P2)` untouched!

**Trace E-2 — Two concurrent `enter`s of two different accounts `Acc1` and `Acc2` proving the same waiting claim `C -> P` (Copy 1 and Copy 2):**
1. Copy 1 writes `AccountLinking(op1, C)` on `H(Acc1)` and hops to `H(C)`. Copy 2 writes `AccountLinking(op2, C)` on `H(Acc2)` and hops to `H(C)`.
2. `H(C)` is single-threaded; say Copy 1 runs first and writes `ClaimUsed(P, op1)` (or commits `ClaimClaiming -> EnterOpWon -> ClaimUsed`).
3. Copy 2 runs next on `H(C)`, sees `ClaimUsed(P, op1)` (`op1 != op2`), misses `C` (falls back to its `:email` claim if it had a distinct one, else hops to `H(Acc2)`, clears `AccountLinking(op2)` via `NONE>`, returns `{:refused :not-invited}`).
4. Copy 1 finishes `H(P)` and `H(Acc1)` and returns `{:person P :own-layer L1 :making M1 :own-layer-made? false}`.

**Trace E-3 — Two-claim `enter` racing concurrent `invite(P1, C_code)` + delayed helper:**
1. Copy 1 (`op1`, `C_code` currently `nil`, `C_email = ClaimWaiting(P2)`) checks `H(C_code)` (nil), hops to `H(op1)`, writes `EnterOpTryEmail`, hops to `H(C_email)`.
2. Meanwhile, `invite(P1, C_code)` commits `ClaimWaiting(P1)`. A delayed helper Copy 2 (started before `EnterOpTryEmail`) arrives at `H(C_code)` and writes `ClaimClaiming(P1, op1, :code)`, then hops to `H(op1)`.
3. On `H(op1)`, Copy 2 sees `EnterOpTryEmail` (not `nil`), so Copy 2 **aborts** `C_code`, hops back to `H(C_code)`, and restores `ClaimWaiting(P1)`. Meanwhile Copy 1 claims `C_email` -> `EnterOpWon(P2, :email)` -> `ClaimUsed(P2, op1)`. Only `P2` is ever entered; `C_code` remains waiting for `P1`.

**Trace E-4 — Failure & retry at every hop of `enter`:**
- *Crash after `AccountLinking(op1)` before `H(C)`*: retry sees `AccountLinking(op1)` (`op0 == op1`), resumes Step 2.
- *Crash after `ClaimClaiming` before `H(op1)`*: retry sees `ClaimClaiming(P, op1, phase)` on `H(C)`, hops to `H(op1)`, commits `EnterOpWon`, finishes.
- *Crash after `EnterOpWon(P, ph)` before `ClaimUsed`*: retry checks `H(op1)` (or `H(C)`), sees `EnterOpWon(P, ph)`, promotes `C` to `ClaimUsed(P, op1)`, enters `P`, links `Acc`.
- *Crash after `ClaimUsed(P, op1)` before `H(P)`*: retry sees `ClaimUsed(P, op1)` (`== op1`), enters `P`, links `Acc`.
- *Crash after `RootEntered(P)` before `AccountLinked(Acc)`*: retry sees `RootEntered(P)` (preserving fixed `L, M` and reading present `:own-layer-made?`), marks `AccountLinked(Acc)`.
- *Crash after `AccountLinked(Acc)` before `:ack` reaches client*: client gets `:reach-the-module` and retries `enter`; Step 1 sees `AccountLinked`, reads present `:own-layer-made?` on `H(P)`, returns the exact same `{:person P :own-layer L :making M :own-layer-made? flag}`.
- **Read-after-write visibility**: `(ack-return> ...)` executes only after `$$claims[C]` (`ClaimUsed`), `$$root-data["<P>\0r"]` (`RootEntered`), and `$$accounts[Acc]` (`AccountLinked`) have all committed and replicated.

#### 1C. `answer-ask` (Code `U` on `H(U)`, Root `P` on `H(P)`)

**Protocol and visibility-order invariant:**
Each `answer-ask` call generates a random UUID `op-id` and appends `AnswerAskEvent` to `*asks` (`hash-by :code`).
- **Step 1 (`H(U)`, 0 hops from `*asks`)**: Read `$$asks[U]`:
  - `nil` (`:no-such-ask`), OR `AskYes` / `AskNo` (standing answer already committed):
    Hop `|hash P` to run take-out under `P` by `:at` (`SPEC.md` line 326–329 requires take-out on every write naming `:root`), then emit `(ack-return> result)`. (1 hop total.)
  - `AskWaiting(H, asks, ends, born-over?)`:
    - If `(or born-over? (>= at ends))`: hop `|hash P` to run take-out under `P` by `:at`, then emit `(ack-return> {:refused :over})`. (1 hop total.)
    - Else (in time): write `AskAnswering(H, asks, ends, born-over?, op, yes?, P, A, new-agent-label, G, until, at)` into `$$asks[U]` (**acquires exclusive slot on `U`; `ask-of` still projects this as `:state :waiting :answer nil`**). Hop `|hash P` to Step 2.
  - `AskAnswering(..., op0, yes0?, P0, ...)` (concurrent answer in flight on `U`, or Rama retry of `op0`):
    - If `op0 == op` (retry of this event): hop `|hash P` to Step 2.
    - If `op0 != op` (competing `answer-ask` from Copy 2): **help `op0`** — run Step 2 on `H(P0)` and Step 3 on `H(U)` for `op0`!
      - If `op0` was ACCEPTED (`{:yes ...}` or `{:no U}`): `U` is now permanently `AskYes` / `AskNo`. If `P != P0`, hop `|hash P` to run take-out under `P` by Copy 2's `:at`. Emit `(ack-return> op0-result)` (both callers receive the standing answer).
      - If `op0` was REFUSED on `H(P0)`: Step 3 reverted `$$asks[U]` to `AskWaiting` (if still `op0`). Loop back to Step 1 on `H(U)` so Copy 2 can now try its own answer `op`!
- **Step 2 (`H(P)`, the commit point for `P` and `op`)**:
  1. Run take-out under `P` by `ans.at`!
  2. Read `$$root-data["<P>\0ok:<op>"]`: if already present (`AskOutcomeYes` / `AskOutcomeNo` / `AskOutcomeRefused`), skip validation and mutation (100% idempotent across retries and concurrent helpers!) and hop `|hash U` to Step 3 with the recorded outcome.
  3. Else evaluate `ans` against `$$root-data` on `H(P)`:
     - Read `r = $$root-data["<P>\0r"]`. If not `RootEntered`: record `AskOutcomeRefused(:no-such-actor)` at `ok:<op>`, hop `|hash U` to Step 3.
     - If `ans.yes? == false` (a **no**): record `AskOutcomeNo(U)` at `ok:<op>`, hop `|hash U` to Step 3. (Judges no other part.)
     - If `ans.yes? == true` (a **yes**):
       - If `new-agent-label == nil`: check `A == P` or `a:<A>` exists (else `AskOutcomeRefused(:no-such-actor)`), check `A` not ended (else `AskOutcomeRefused(:ended)`), check `g:<G>` absent (else `AskOutcomeRefused(:in-use)`).
       - If `new-agent-label != nil`: check `A != P`, `a:<A>` absent, and `g:<G>` absent (else `AskOutcomeRefused(:in-use)`).
       - If accepted: **atomically in this single-task segment on `H(P)`**:
         - If `new-agent-label != nil`: assign `a-seq = next-actor-seq`, increment `next-actor-seq`, write `a:<A>` (depth 1, `made-by = P`, `ended? = false`), `ao:<a-seq>`, `ch:<P>:<A>`.
         - Assign `g-seq = next-sign-in-seq`, increment `next-sign-in-seq`, write `g:<G>` (`kind = :program`, `initial-hash = ask.hash`, `hash = ask.hash`, `label = ask.asks`, `begun = ans.at`, `until = ans.until`, `ended? = (<= ans.until ans.at)`). If `(> ans.until ans.at)`, also write `LiveSignIn` to `$$live`, `exp:` and `ag:` to `$$root-data`.
         - Write `AskOutcomeYes(P, A, G)` to `ok:<op>`.
         - Hop `|hash U` to Step 3! (**Notice: `A` and `G` commit and replicate on `H(P)` BEFORE Step 3 commits `AskYes` on `H(U)`!**)
- **Step 3 (`H(U)`)**:
  Read `$$asks[U]`:
  - If outcome is `AskOutcomeYes(P, A, G)`: if `$$asks[U]` is still `AskAnswering(op)`, write `AskYes(H, asks, ends, P, A, G)`. Emit `(ack-return> {:yes {:root P :actor A :sign-in G}})`.
  - If outcome is `AskOutcomeNo(U)`: if still `AskAnswering(op)`, write `AskNo(H, asks, ends)`. Emit `(ack-return> {:no U})`.
  - If outcome is `AskOutcomeRefused(reason)`: if still `AskAnswering(op)`, restore `AskWaiting(H, asks, ends, born-over?)`. Emit `(ack-return> {:refused reason})`.

**Trace AA-1 — Two persons `P1` (Copy 1, `:yes` with new agent `A1` + `G1`) and `P2` (Copy 2, `:yes` with new agent `A2` + `G2`) answer `U` at the same moment:**
1. Copy 1 arrives first on `H(U)`, writes `AskAnswering(op1, P1, A1, G1)`, hops to `H(P1)`.
2. Copy 2 arrives next on `H(U)`, sees `AskAnswering(op1)`, hops to `H(P1)` to help `op1` (never touching `P2`'s agents or sign-ins!).
3. On `H(P1)`, whichever of Copy 1 / Copy 2 arrives first creates `A1` and `G1` and writes `ok:<op1> = AskOutcomeYes(P1, A1, G1)`. The second sees `ok:<op1>` already recorded and performs zero mutations.
4. Both hop back to `H(U)`: first commits `AskYes(P1, A1, G1)`; second sees `AskYes` already committed.
5. Copy 1 returns `{:yes {:root P1 :actor A1 :sign-in G1}}`. Copy 2 hops `|hash P2` to run take-out under `P2` by Copy 2's `:at`, then returns `{:yes {:root P1 :actor A1 :sign-in G1}}`.
6. **Zero leaked state on `P2`** (neither `A2` nor `G2` was created), and **`ask-of(U)` never returned `:yes` before `A1` and `G1` were durable on `H(P1)`**.
7. *If instead `P1` was refused (`:ended`)*: `H(P1)` records `ok:<op1> = AskOutcomeRefused(:ended)`; `H(U)` restores `AskWaiting`; Copy 1 returns `{:refused :ended}`; Copy 2 loops back to Step 1 on `H(U)`, acquires `AskAnswering(op2)`, succeeds on `H(P2)` creating `A2` and `G2`, commits `AskYes(P2, A2, G2)`, and returns `{:yes {:root P2 :actor A2 :sign-in G2}}`.

**Trace AA-2 — `answer-ask` (`:yes` as existing agent `A` of `P`) racing `end-actor(P, A)` through Copy 1 and Copy 2:**
1. Copy 1 (`answer-ask`) acquires `AskAnswering(op1)` on `H(U)` and hops to `H(P)`. Copy 2 (`end-actor`) lands directly on `H(P)`.
2. `H(P)` is single-threaded:
   - *If `end-actor` runs first*: `a:<A>` becomes `:ended? true`. Copy 1 then runs Step 2 on `H(P)`, sees `a:<A>` ended, writes `ok:<op1> = AskOutcomeRefused(:ended)` (no sign-in created!), hops to `H(U)`, restores `AskWaiting`, returns `{:refused :ended}`. The ask keeps waiting!
   - *If Copy 1 Step 2 runs first*: `a:<A>` is live -> creates `G` (live in `$$root-data` and `$$live`), writes `ok:<op1> = AskOutcomeYes(P, A, G)`. `end-actor` runs next on `H(P)`, cascades to `G`, marks `G` `:ended? true` and removes `G` from `$$live`. Copy 1 finishes Step 3 on `H(U)` -> `AskYes(P, A, G)` -> returns `{:yes {:root P :actor A :sign-in G}}`. `who-is(P, G)` shows `G` with `:ended? true, :actor-ended? true`.

**Trace AA-3 — Failure & retry at every hop of `answer-ask`:**
- *Crash after `AskAnswering(op1)` before Step 2*: retry sees `AskAnswering(op1)` (`op0 == op1`), hops to `H(P)`, evaluates `op1`, commits `H(U)`.
- *Crash after Step 2 committed `A1`/`G1`/`ok:<op1>` on `H(P)` (or committed `AskOutcomeRefused` on `H(P)`) before Step 3*: retry sees `AskAnswering(op1)` on `H(U)`, hops to `H(P)`, reads `ok:<op1>` (exact same outcome, zero re-evaluation even if `P1`'s state changed since!), hops to `H(U)` and finishes Step 3!
- *Crash after Step 3 committed `AskYes` on `H(U)` before `:ack` reached client*: client retries `answer-ask`; Step 1 sees `AskYes`, runs take-out on `H(P)`, returns `{:yes {:root P :actor A :sign-in G}}`.
- **Read-after-write visibility**: `(ack-return> ...)` executes only after `H(P)` (including take-out and any new `A`/`G`/`$$live`) AND `H(U)` (`AskYes`/`AskNo`) have committed and replicated. Once `ask-of(U)` shows `:yes`, `who-is`, `under`, and `watch-root` on `H(P)` already hold `A` and `G`.

### 2. Live standing, take-outs, 32-deep end-actor cascade, and history separation

#### What is live under root `P`
- **Actors**: `P` herself is at depth 0 and never ends in this build. Each agent `A` lives at depth `1..32` (`a:<A>` holds `:made-by`, `:depth`, `:ended?`, `:seq`), ordered in `ao:<hex-seq>` for `under`, with outgoing child edges in `ch:<B>:<A>`. An actor is live iff `:ended? false` — because `end-actor` eagerly marks every descendant `:ended? true` in one atomic transaction, no read ever traverses parent pointers.
- **Sign-ins**: Live sign-ins exist in `$$live` (`"<P>\0g:<G>" -> LiveSignIn`), `$$root-data` (`g:<G>` with `:ended? false`), `ag:<A>:<G>`, and `exp:<hex-until>:g:<G>`. A sign-in's expiry time is strictly its present secret's `:until` (`SPEC.md` line 335); `:previous` stays readable until the next replacement and never extends a sign-in's lifetime.
- **Sessions**: Open sessions exist in `$$live` (`"<P>\0s:<S>" -> LiveSession`, omitting `:driven-by`), `$$root-data` (`s:<S>` with `:closed? false`, keeping `:driven-by`), `sg:<G>:<S>`, and `exp:<hex-until>:s:<S>`.

#### Step-by-step trace: Take-out on any write naming `:root P`
Which writes trigger take-out (`SPEC.md` lines 326–334): `begin-sign-in`, `replace-secret`, `end-sign-in`, `make-actor`, `end-actor`, `open-session`, `close-session`, and `answer-ask` (all paths). `invite`, `enter`, `own-layer-made`, and `ask` name no `:root` and take nothing out.

*Concrete scenario*: Under root `P`, sign-in `G1` (`:until 1000`) has two open sessions: `S1` (`:until 900`) and `S2` (`:until 2000`, outliving `G1`). Sign-in `G2` (`:until 5000`) has open session `S3` (`:until 1000`) and open session `S4` (`:until 3000`). Now `open-session {:root P :sign-in G2 :session S3 :until 6000 :at 1000}` (a repeat attempting to extend `S3` right at `t = 1000`) arrives on `H(P)`:
1. **Before judging the write** (whether it will be taken, a repeat, or refused), execute take-out for `at = 1000` on `H(P)`:
   - Range-scan `$$root-data` on `["<P>\0exp:", "<P>\0exp:<hex-1000>;")`. This single seek returns exactly three expired `ExpiryEntry` rows in order: `(S1, 900)`, `(G1, 1000)`, `(S3, 1000)`. (`S2` at 2000, `S4` at 3000, `G2` at 5000 are never touched!)
   - **Entry 1 (`S1`, session, 900 <= 1000)**: remove `exp:<hex-900>:s:<S1>`; read `s:<S1>` (`:closed? false`); set `s:<S1>` `:closed? true` (preserving `:driven-by`); `NONE>` `$$live["<P>\0s:<S1>"]` and `sg:<G1>:<S1>`.
   - **Entry 2 (`G1`, sign-in, 1000 <= 1000)**: remove `exp:<hex-1000>:g:<G1>`; read `g:<G1>` (`:ended? false`); set `g:<G1>` `:ended? true`; `NONE>` `$$live["<P>\0g:<G1>"]` and `ag:<A1>:<G1>`. **Cascade to remaining open sessions of `G1`**: range-scan `sg:<G1>:` -> finds `S2` (even though `S2`'s `:until = 2000 > 1000`!). Set `s:<S2>` `:closed? true`, `NONE>` `$$live["<P>\0s:<S2>"]`, `NONE>` `exp:<hex-2000>:s:<S2>`, `NONE>` `sg:<G1>:<S2>`.
   - **Entry 3 (`S3`, session, 1000 <= 1000)**: remove `exp:<hex-1000>:s:<S3>`; set `s:<S3>` `:closed? true`; `NONE>` `$$live["<P>\0s:<S3>"]` and `sg:<G2>:<S3>`.
2. **Now judge `open-session(G2, S3, :until 6000, :at 1000)`**:
   - Read `s:<S3>`: exists under `G2` -> repeat! Because `S3` is now `:closed? true` (taken out in Step 1), its `:until` is **not** moved to `6000` and it is **not** reopened (`SPEC.md` line 292–293).
   - Emit `(ack-return> {:open S3})`.
3. **Commit & watch**: All writes commit atomically on `H(P)`. `watch-root` subscribers receive **one** atomic `MultiDiff` removing `G1`, `S1`, `S2`, `S3` together, leaving `{:sign-ins {G2 ...} :sessions {S4 ...}}`.
4. **Born-expired edge case (`t1 <= at`)**: On `begin-sign-in`, `replace-secret`, `open-session`, or `answer-ask (:yes)` where `until <= at`, the record is written to `$$root-data` (`g:<G>` or `s:<S>`) already marked `:ended? true` / `:closed? true` (and for `replace-secret`, any existing open sessions of `G` are closed and removed from `$$live`/`exp:`/`sg:`), and the item is never inserted into `$$live`, `exp:`, `ag:`, or `sg:`.

#### Step-by-step trace: 32-deep `end-actor` cascade with O(1) read depth
*Concrete scenario*: Root `P` (depth 0) made `A1` (depth 1) -> `A2` (depth 2) -> ... -> `A32` (depth 32). An attempt `make-actor` with `:made-by A32` reads `a:<A32>`, sees `:depth = 32`, and returns `{:refused :too-deep}`. `A1` has 500 historical ended sign-ins and 10,000 historical closed sessions; across the subtree `A1..A32` there are 12 live sign-ins and 40 open sessions; `A15` was already ended earlier (with 20 ended descendants `A15_1..A15_20`). Now `end-actor {:root P :actor A1 :at t}` arrives on `H(P)`:
1. Run take-out under `P` by `t`.
2. Read `r` (`RootEntered`) and `a:<A1>` (`:ended? false`; `A1 != P`).
3. Run iterative non-yielding BFS queue initialized to `[A1]` on `H(P)`:
   - Pop `X`, read `a:<X>`. If `(:ended? X)` is already `true` (e.g., `A15`), **skip `X` and do not scan `ch:<X>:`** (its subtree `A15_1..A15_20` was already ended when `A15` ended -> O(live subtree) only!).
   - Mark `a:<X>` `:ended? true` and update `ao:<hex-(:seq X)>` `:ended? true` (1 direct no-read write each).
   - Range-scan `ag:<X>:` (`1 seek + live-sign-ins-of-X × 5µs`). **Zero ended sign-ins of `X` are in `ag:`**, so the 500 historical ended sign-ins of `A1` are never touched!
   - For each live sign-in `G` of `X`:
     - Set `g:<G>` `:ended? true` (leaving `:actor` intact; old ended sign-ins of `X` are untouched).
     - `NONE>` `$$live["<P>\0g:<G>"]`, `exp:<hex-(:until G)>:g:<G>`, `ag:<X>:<G>`.
     - Range-scan `sg:<G>:` (`1 seek + open-sessions-of-G × 5µs`). **Zero closed sessions are in `sg:`**, so the 10,000 historical closed sessions are never touched!
     - For each open session `S` of `G`: set `s:<S>` `:closed? true`, `NONE>` `$$live["<P>\0s:<S>"]`, `exp:<hex-(:until S)>:s:<S>`, `sg:<G>:<S>`.
   - Range-scan `ch:<X>:` and enqueue all children of `X`.
4. Loop terminates after visiting only the live descendants of `A1` (depths 1..32, no recursion stack). All PState writes commit as **one atomic streaming batch** on `H(P)`; `watch-root` receives one atomic `MultiDiff` removing all 12 sign-ins and 40 sessions at once; `(ack-return> {:ended A1})` returns.
5. **Why no read costs more with depth (1..32)**:
   - `who-is` on a sign-in of `A32`: reads `r`, `g:<G>`, optionally `s:<S>`, and (since `G` is now ended and `A32 != P`) reads `a:<A32>` directly (**1 seek**, seeing `:ended? true` directly on `A32`!). Never walks `A32 -> A31 -> ... -> A1 -> P`. Cost is identical at depth 1 and depth 32!
   - `who-is` on any of `A1`'s 500 pre-existing ended sign-ins: reads `a:<A1>` directly (**1 seek**), now returning `:ended? true, :actor-ended? true` without `end-actor` ever having touched those 500 ended sign-in rows (`IMPLICIT_SPEC.md:1460–1462` and `537–539` both hold simultaneously!).
   - `under(P)`: reads `ao:` range (already has `:ended? true` on every ended actor in `A1..A32`) and `$$live` `g:` range (already empty of `A1..A32`'s sign-ins). Cost is O(total agents + live sign-ins), independent of tree depth!
   - `watch-root(P)`: reads `$$live` only, independent of tree depth!

#### History kept apart from what is live
- `$$live` contains **strictly** currently live sign-ins and currently open sessions (`NONE>` on every end/close/take-out).
- `exp:`, `ag:`, `sg:` in `$$root-data` contain **strictly** currently live items (`NONE>` on every end/close/take-out).
- `a:`, `ao:`, `ch:`, `g:`, `s:`, `ok:` in `$$root-data` and `$$asks`, `$$claims`, `$$accounts` retain permanent history (`SPEC.md` line 399: "Nothing is deleted").
- Neither `watch-root`, take-outs, `end-sign-in`, `end-actor`, nor `under`'s `:sign-ins` ever scans `g:` or `s:` history; `who-is` does point lookups by exact key (`r`, `g:<G>`, `s:<S>`, conditionally `a:<A>`), costing the same after 10 million closed sessions as after 1.

### 3. who-is: one round trip, one task, bounded read, seek arithmetic

**Step-by-step execution trace of `(who-is client {:root P :sign-in G :session S-or-nil})`:**
1. **Client edge validation** (`softland.auth.client`, 0 network calls): check client not closed (else `{:could-not :closed}`), input map has valid UUID `:root P`, UUID `:sign-in G`, and `nil` or UUID `:session S` (else `{:could-not :read-the-event}`).
2. **Client-side direct routing (1 network round trip to 1 task)**: invoke query topology `"who-is"` with `(P, G, S)`. Because its first statement is the built-in single-task partitioner `(|hash *root)`, Rama computes `task = hash(P) mod N` on the client and dispatches directly to task `H(P)` with zero intermediate random-task hop (`query-topologies.md:120`).
3. **Read 1 — Root record (`$$root-data["<P>\0r"]`)**: 1 RocksDB point seek.
   - If absent or `RootReserving` / `RootInvited` (unknown or waiting person): short-circuit immediately; skip Reads 2–4 and return `{:sign-in nil :session nil :own-layer nil :own-layer-made? false}` across `(|origin)`.
   - If `RootEntered`: extract `:own-layer` (`L`) and `:own-layer-made?` (`bool`). (`:making`, `:linked-provider`, `:linked-subject`, `:claim-*` are never returned.)
4. **Read 2 — Sign-in record (`$$root-data["<P>\0g:<G>"]`)**: 1 RocksDB point seek.
   - If absent (`nil`): sign-in part is `nil`; skip Read 4.
   - If `SignInRec`: extract `:actor A`, `:kind K`, `:hash H`, `:until t1`, `:previous` (`{:hash prev-hash :until prev-until}` when `prev-hash` non-nil, else `nil`), `:ended?`.
5. **Read 3 — Session record (`$$root-data["<P>\0s:<S>"]`, conditional on `S != nil`)**:
   - If `S == nil`: 0 seeks, session part is `nil`.
   - If `S != nil`: 1 RocksDB point seek. If `SessionRec` exists, project `{:sign-in G' :until t3 :closed? bool :driven-by D-or-nil}` where `G'` is `(:sign-in sess)` (which may differ from `G`), `:closed?` is `true` for explicitly closed, cascaded, or taken-out sessions, and `:driven-by` projects `DrivenByPerson` -> `{:kind :person}`, `DrivenByModel(m)` -> `{:kind :model :model m}`, or `nil` -> `nil` — preserved identically after the session closes. If absent, session part is `nil`.
6. **Read 4 — Actor record (`$$root-data["<P>\0a:<A>"]`, conditional on ended agent sign-in only)**:
   - If `G` is `nil`, OR `(:ended? G)` is `false` (live sign-in guarantees live actor via atomic `end-actor` cascade), OR `(:actor G) == P` (person never ends): 0 seeks, `:actor-ended? false`.
   - Only if `G` exists, `(:ended? G)` is `true`, AND `(:actor G) != P`: 1 RocksDB point seek on `a:<A>` to read `(:ended? actor-rec)`. Even if `A` is at depth 32, this is 1 seek because `end-actor` stamped `:ended? true` directly onto `a:<A>`.
7. **Return (`|origin`)**: assemble and return the bounded response map in the same single round trip.

**Seek & latency arithmetic (Rama skill model: `0.5 ms` / SSD seek, `5 µs` / iterator Next):**

| Case | PState keys read | Seeks | Iter-reads | Worst-case cold SSD disk cost | Aggregate cluster work (throughput) |
|---|---|---|---|---|---|
| Unknown or waiting root `P` | `r` | 1 | 0 | `1 × 0.5 ms = 0.5 ms` | 1 seek on 1 task |
| Entered root, live `G` (human or agent), `S = nil` | `r`, `g:<G>` | 2 | 0 | `2 × 0.5 ms = 1.0 ms` | 2 seeks on 1 task |
| Entered root, live `G` (human or agent), `S != nil` (common HTTP request) | `r`, `g:<G>`, `s:<S>` | 3 | 0 | `3 × 0.5 ms = 1.5 ms` | 3 seeks on 1 task |
| Entered root, ended agent `G` (`A != P`), `S != nil` | `r`, `g:<G>`, `s:<S>`, `a:<A>` | 4 | 0 | `4 × 0.5 ms = 2.0 ms` | 4 seeks on 1 task |

- **Independence from history and depth**: Because every key is looked up directly in RocksDB's top-level LSM index (and `who-is` never scans `ao:`, `ag:`, `sg:`, `exp:`, or `$$live`), a person with 5,000,000 closed sessions, 50,000 ended sign-ins, and a 32-deep agent hierarchy pays the exact same `1–4` seeks (`0.5–2.0 ms` cold SSD; sub-millisecond warm in RocksDB's 256 MB block cache) and `1` network round trip to `1` task as a person who entered one second ago.
- **Why 3 seeks cannot be reduced further without breaking the spec**: `r` (`:own-layer-made?` flips via `own-layer-made` without naming `G`), `g:<G>` (independent sign-in), and `s:<S>` (independent session that may belong to a different sign-in `G' != G` under `P`) are three distinct entities that can each be mutated independently or queried when the others are absent. Skipping `a:<A>` on all live and human sign-ins already saves 1 full seek (~25% aggregate disk I/O reduction) on 97%+ of production traffic.

### 4. watch-root: bounded value, 20,000 watched roots per task, and not-reached

#### Why `watch-root`'s value stays small
- `watch-root` subscribes exclusively to `$$live` on range `["<P>\0", "<P>\1")` with `{:pkey P}`.
- `$$live` stores **only** live sign-ins (`LiveSignIn`) and open sessions (`LiveSession`, **without `:driven-by`**). Every `end-sign-in`, `close-session`, `end-actor` cascade, and `:root`-write take-out deletes ended/closed entries from `$$live` via `NONE>` in the same single-task atomic batch.
- Consequently, a root with 10 live sign-ins, 100 open sessions, and 5,000,000 closed historical sessions has a `$$live` slice of only 110 entries (~15 KB). Every `open-session`, `close-session`, or `replace-secret` writes 1 key (~64–150 B) and pushes an incremental `KeyDiff` or `KeyRemoveDiff` (or 1 atomic `MultiDiff` for multi-item ends/take-outs, verified by probe `auth_rama_probe_2.clj`).
- `softland.auth.client` maintains the projected `{:sign-ins {...} :sessions {...}}` map incrementally from `KeyDiff` / `KeyRemoveDiff` / `MultiDiff` (falling back to full rebuild only on `ResyncDiff` or `NewValueDiff`), so per-change work on Rama's 2-thread proxy callback pool is O(changed entries), keeping callbacks non-blocking.

#### How many roots can be watched on one task (`pstate.proxy.limit = 20000`)
- Empirical measurement (`WORKFLOW-AND-MODEL.md` §2.1, `probes/proxy_limit_probe.clj`) proved that `pstate.proxy.limit` bounds subscriptions strictly **per-(module, PState, task-partition)** across **all** connected client handles combined, and that `Auth/$$live` does not share a limit with `Store` or with `Auth/$$root-data`.
- At module launch, `Auth` executes `(set-launch-pstate-dynamic-option! setup "$$live" "pstate.proxy.limit" 20000)`, lifting the limit from Rama's 1,000 default to **20,000 subscriptions per task** (matching Red Planet Labs' TPC-C benchmark setting; tunable dynamically without redeploy via `rama setOption`).
- **Client-side deduplication**: Within one `Auth` client instance (one runtime JVM copy), multiple `(watch-root client P f)` calls for the same root `P` share **one** `foreign-proxy` handle. When the last watcher for `P` invokes its stop function, the client closes that `foreign-proxy` immediately, releasing its server-side slot.
- **Capacity across cluster**: At `N = 16` tasks, `$$live` supports 320,000 concurrent `(root, runtime-copy)` subscriptions; at `N = 64` tasks, 1,280,000. (Note: if `K` distinct runtime copies watch the same root `P` simultaneously, each copy holds 1 subscription, consuming `K` of `P`'s task's 20,000 slots.)

#### Step-by-step trace: Registration, register-before-`who-is`, and steady-state updates
1. Caller calls `(watch-root client P f)`. Client checks closed flag (if closed, returns `{:could-not :closed}` in place of the stop function, `SPEC.md` line 361) and validates `P` is a UUID and `f` is `ifn?` (else `{:could-not :read-the-event}`).
2. Client atomically joins (or creates) the shared proxy entry for `P` in its in-JVM watch map:
   - *If a live shared proxy for `P` is already open*: attaches watcher `(wid, f)` and returns `stop` immediately (0 network calls, no call to `f` at registration).
   - *If no shared proxy for `P` is open*: initiates `(foreign-proxy-async [(sorted-map-range "<P>\0" "<P>\1")] $$live {:pkey P :callback-fn ...})` and **awaits its open future with a bounded 5 s timeout** before returning `stop`.
3. When Rama opens the proxy on task `H(P)`, Rama delivers the initial `ResyncDiff` (`V0`). The client sets its local projected map to `V0`, flips `initialized?` to `true`, and **suppresses calling `f`** (`SPEC.md` line 214: no call owed at registration; `IMPLICIT_SPEC.md` line 827: no call when nothing live).
4. `watch-root` returns `stop`. Because the server subscription on `H(P)` is **already active** when `watch-root` returns, a caller that registers first and then calls `who-is` cannot miss an `end-sign-in` / `end-actor` / take-out that happens after `who-is` (no blind window between registration and proxy activation).
5. On any subsequent write to `$$live` for `P` from any runtime copy:
   - Rama delivers `KeyDiff` / `KeyRemoveDiff` / `MultiDiff` / `ResyncDiff` on Rama's proxy callback thread.
   - Client updates its projected map `{:sign-ins {...} :sessions {...}}` (suppressing a post-open `ResyncDiff` only if the projected map is value-equal to the previous one; delivering any change).
   - For each active watcher `(wid, f)` under `P`, under that watcher's turn lock (skipped if stopped, closed, or already notified `not-reached`), calls `(f projected-map)` inside `try/catch Throwable` so a throwing `f` never kills other watchers or later callbacks.
   - Writes that do not mutate `$$live` (`invite`, `enter`, `own-layer-made`, `make-actor`, `ask`, `:no`, refusals/repeats with no take-out) produce zero callbacks on `$$live`.

#### Step-by-step trace: The one call of `:softland.auth.client/not-reached` within seconds
Probe `auth_rama_probe.clj` proved on Rama 1.6.0 that destroying a module with open `foreign-proxy` handles does **not** emit `UngracefulTerminationDiff` within 15 seconds under Rama's default `foreign.proxy.failure.window.seconds = 120` / `threshold = 10` (~100 s). Because `(open cluster module-name)` accepts an already-constructed `cluster`, `Auth`'s client cannot rely on `UngracefulTerminationDiff` alone to meet *"within seconds of the loss"* (`SPEC.md` line 212). Instead, three coordinated triggers feed one shared exact-once termination routine `fail-watch!`:

1. **Trigger 1 — Initial open failure or timeout (<= 5 s)**: If `foreign-proxy-async` throws or fails to open within 5 s (e.g., module unreachable or `pstate.proxy.limit` exceeded, avoiding Rama's 100 s retry storm), `watch-root` removes the failed entry, returns a no-op `stop` function, and dispatches `fail-watch!` asynchronously on the client's monitor executor so `f` receives `:softland.auth.client/not-reached` once.
2. **Trigger 2 — Rama `UngracefulTerminationDiff` (immediate)**: If Rama forcibly terminates a proxy, the callback invokes `fail-proxy!` for `P`, detaching `P`'s shared proxy from the registry (so re-registering opens a fresh proxy), closing the dead handle, and calling `fail-watch!` on each attached watcher.
3. **Trigger 3 — Client-side `ping-task` liveness monitor (within ~2–5 s)**:
   - While the client holds >= 1 active watch, a single daemon scheduler pings all `N` tasks (`0 .. num-partitions - 1`) in parallel every **2 seconds** using `"ping-task"` (`(|direct *task-id)`, 0 RocksDB seeks, 2 s timeout).
   - *Healthy steady state*: all `N` tasks reply `true` in < 1 ms -> 0 per-watch probes, negligible overhead (`N` tiny in-memory RPCs / 2 s per runtime copy).
   - *Full module/cluster/network loss*: all `N` tasks fail or time out within 2 s -> `fail-proxy!` runs immediately for every watched root `P`, delivering `:softland.auth.client/not-reached` once to every watcher within **2–4 seconds** of the loss.
   - *Partial worker loss (subset of tasks unreachable)*: the monitor issues a bounded 1 s `foreign-select-async` probe (`[(sorted-map-range-from "<P>\0" 1)]` with `{:pkey P}`) for active watched roots (only during the outage); every watched root whose task fails to respond within 1 s receives `:softland.auth.client/not-reached` once within **3–5 seconds**, while watches on healthy tasks continue uninterrupted.
4. **Exact-once, stop, and `close!` guarantees**:
   - Each watcher holds a state flag (`:active` -> `:dead` | `:stopped`) and a per-watcher turn object. `fail-watch!` atomically transitions `:active -> :dead` inside the turn, invokes `(f :softland.auth.client/not-reached)` once (catching any throw), and discards the watcher. Any racing diff callback or second failure trigger sees `:dead` and does nothing.
   - Calling `stop` transitions `:active -> :stopped` (outside `f` it synchronizes with the turn; if called re-entrantly from inside `f`, detected via a `ThreadLocal` callback mark so it marks `:stopped` immediately without deadlocking) and closes the shared `foreign-proxy` when the last watcher for `P` leaves. Calling `stop` twice is a no-op.
   - Calling `(close! client)` stops the `ping-task` monitor, transitions all watchers to `:stopped` (never sending `:not-reached`), suppresses `DestroyedDiff`, and closes all shared `foreign-proxy` handles; no `f` is ever called again after `close!`.

### 5. What can be erased later

`SPEC.md` (lines 24–28, 371–376) defines what names a human being as three items: **her account** (`[provider subject]`), **her email**, and **what her provider said** (`:says`). All three must be erasable later from every PState and every event log, while every non-PII fact (person UUID `P`, `:own-layer`, `:making`, actors, sign-ins, sessions, asks) stays intact and reachable without them. This build implements **none** of the erasure or thinning operations; the layout below keeps all of them cleanly possible without schema redesign.

#### Complete PII inventory across every PState and every depot

| Storage object | Holds account (`[provider subject]`)? | Holds email? | Holds `:says`? | How future erasure removes it |
|---|---|---|---|---|
| `$$accounts` | **Yes** (key `AccountKey`) | Transiently only in `AccountLinking` while in flight; **No** in `AccountLinked` | **Yes** (latest map on `AccountLinked`) | Single `NONE>` transform on `$$accounts[AccountKey]` (O(1), 0 reads). After RocksDB compaction, zero bytes remain on disk. |
| `$$claims` | **No** (stores only non-PII `op-id` UUID in `ClaimClaiming`/`ClaimUsed`) | **Yes, for email claims only** (key `ClaimKey(:email, E)`); **No** for code claims (`ClaimKey(:code, H)`) | **No** | Single `NONE>` transform on `$$claims[ClaimKey(:email, E)]` (O(1), 0 reads). Code claims require no erasure. |
| `$$root-data` (`"<P>\0r"`) | **Yes** (`linked-provider`, `linked-subject` pointer fields on `RootEntered`) | **Yes, for email claims only** (`claim-val` when `claim-kind == :email`) | **No** | Future `erase-person-pii(P)` reads `r` (1 seek) to locate `AccountKey` and `ClaimKey(:email, E)` in O(1) without scanning `$$accounts`/`$$claims`, replaces `linked-provider`/`linked-subject`/`claim-kind`/`claim-val` with `nil` (keeping `RootEntered`, `:own-layer`, `:making`, `:own-layer-made?`, and sequences intact), then hops to `H(Acc)` and `H(C)` to `NONE>` them. |
| `$$root-data` (`a:`, `ao:`, `ch:`, `g:`, `ag:`, `s:`, `sg:`, `exp:`, `ok:`) | **No** | **No** | **No** | Never touched by PII erasure. |
| `$$live` | **No** | **No** | **No** | Never touched by PII erasure. |
| `$$asks` | **No** | **No** | **No** | Never touched by PII erasure. |
| `$$enter-ops` | **No** (UUID `op-id` and UUID `P` only) | **No** | **No** | Never touched by PII erasure. |
| Depot `*enters` | **Yes** (every `EnterEvent`, accepted & refused alike) | **Yes** (`:email`) | **Yes** (`:says`) | Isolated low-volume depot. Future module update applies Rama `(depot-migration id fn)` (`depot-migration.md`) returning `DEPOT-TOMBSTONE` (or redacted record) for matching account/email — including refused enters (`SPEC.md:376`) — which rewrites the depot via Rama's two-log switch and deletes the old log from disk. Alternatively, because `$$accounts` is durable storage in its own right (`SKILL.md:28`), `*enters` can also enable independent per-depot trimming (`depot.max.entries.per.partition`) without affecting `*root-events` or `*asks`. |
| Depot `*invites` | **No** | **Yes, for email claims only** (`:claim`) | **No** | Isolated low-volume depot. Future module update applies `(depot-migration id fn)` returning `DEPOT-TOMBSTONE` (or redacted claim) for matching `:person` / `:email`, physically removing email bytes from disk. |
| Depot `*root-events` | **No** | **No** | **No** | Never scanned or migrated for PII erasure. Eligible later for independent thinning via `depot.max.entries.per.partition`. |
| Depot `*asks` | **No** | **No** | **No** | Never scanned or migrated for PII erasure. Eligible later for independent thinning via `depot.max.entries.per.partition`. |

#### Design invariants that keep future erasure and thinning clean
1. **No non-PII entity is keyed by PII**: `P`, actors, sign-ins, sessions, and asks are keyed solely by random UUIDs or 8-char codes. Erasing `$$accounts[Acc]` and `$$claims[[:email E]]` breaks zero lookups in `who-is`, `under`, `watch-root`, `ask-of`, or any root write.
2. **Zero PII leakage into code claims or coordination state**: Using random `op-id` UUIDs in `ClaimUsed`, `ClaimClaiming`, and `$$enter-ops` ensures `AccountKey` never escapes `$$accounts`, `$$root-data["<P>\0r"]`, and `*enters`.
3. **Structured typed depot records**: Every `*enters` and `*invites` record stores `:account`, `:email`, `:says`, and `:claim` as named top-level fields (never opaque serialized strings), so a future `depot-migration` function can match and tombstone/redact by account, email, or person ID in one pass.
4. **Operational note on Rama depot erasure**: Rama's `depot-migration` requires a module update (`operate.md`), so batch/deploy-driven erasure of historical depot records works out of the box with zero extra infrastructure today; if sub-second online erasure without a module update is ever required later, isolating `*enters` and `*invites` lets a future build add per-person envelope encryption or short depot retention on those two depots alone without touching `*root-events` or `*asks`. (Also note: erasing `$$claims[[:email E]]` necessarily forgets that email `E` was previously bound, since keeping an unerasable fingerprint of `E` would itself retain a linkable identifier for `E`.)
5. **Nothing built in this pass**: No erase API, no `depot-migration`, and no `depot.max.entries.per.partition` trim option is declared in this build.

### 6. No bottleneck task, repeat rules, and refusal orders

#### No one task that everything passes through
- **Zero global depots, zero global PStates, zero tick depots, zero `|global` partitioners, zero `|all` partitioners.**
- Every operation touches only tasks determined by hashing its own entity key(s):
  - All 8 `*root-events` writes, `who-is`, `under`, and `watch-root`: strictly task `H(P)`.
  - `ask` and `ask-of`: strictly task `H(U)`.
  - `answer-ask`: strictly tasks `H(U)` and `H(P)` (and `H(P0)` only when helping a concurrent conflicting answer).
  - `invite`: strictly tasks `H(C)` and `H(P)` (and `H(C0)` only when helping a concurrent conflicting invite for the same `P`).
  - `enter`: strictly tasks `H(Acc)` and `H(P)` for returning users; plus `H(C)` (and `H(op)` for 2-claim enters) on first-time enters.
  - `ping-task`: `|direct t` independently across `0..N-1` (0 seeks).
- No write and no read of one person ever touches a task shared by all persons.

#### Repeat rules and exact refusal orders for all 12 writes

Where `SPEC.md` and `IMPLICIT_SPEC.md` differ, **`SPEC.md` governs**:
1. **Take-out scope** (`SPEC.md` line 331–332 overrides `IMPLICIT_SPEC.md` Q2): `invite`, `enter`, `own-layer-made`, and `ask` name no `:root` and take nothing out. The 8 writes naming `:root` take out expired sign-ins/sessions by `:at` before judging repeat or refusals.
2. **Refusal order** (`SPEC.md` line 357–359 overrides `IMPLICIT_SPEC.md` Q5): *"Where two refusals apply to one write, a repeat is recognised first, then the refusals in the order in which the contract lists them. A session's id that is another sign-in's, open or not, is `:in-use`."* This specifically governs three operations where `IMPLICIT_SPEC.md` placed `:in-use` ahead of the contract's order: `begin-sign-in`, `make-actor`, and `open-session`.

| Write | Step 0: Take-out under `:root`? | Step 1: Repeat recognition (returns first result) | Step 2+: Exact refusal order (`SPEC.md` contract order) |
|---|---|---|---|
| `invite` | No | `$$claims[C]` exists with `:person == P` (or `r` is `RootInvited`/`RootEntered` with same `C`) -> `{:invited P}` (even after `P` entered; `:invited-by`, `:at` dropped) | 1. `:claim-in-use` (`C` names another person, waiting or used)<br>2. `:person-in-use` (`P` has another claim, waiting or entered) |
| `enter` | No | `$$accounts[Acc]` is `AccountLinked(P, L', M')` -> updates `:says` & `:says-at`, reads present `:own-layer-made?` on `H(P)`, drops offer & claims -> `{:person P :own-layer L' :making M' :own-layer-made? bool}` | 1. `:not-invited` (`Acc` unlinked and proves neither a waiting `:code-hash` claim nor a waiting `:email` claim) |
| `own-layer-made` | No | `r` is `RootEntered` with `:own-layer-made? true` -> `{:made P}` (no change) | 1. `:no-such-person` (`r` absent, `RootReserving`, or `RootInvited`) |
| `begin-sign-in` | Yes (`:at`) | `g:<G>` exists with same `(actor A, kind K, initial-hash H)` -> `{:begun G}` (even after `replace-secret` or end; `:until`, `:label`, `:at` dropped) | 1. `:no-such-actor` (`r` not `RootEntered`, or `A != P` and `a:<A>` absent)<br>2. `:ended` (`A != P` and `a:<A>` has `:ended? true`)<br>3. `:in-use` (`g:<G>` exists with different `(A, K, initial-hash)`) |
| `replace-secret` | Yes (`:at`) | `g:<G>` exists with `(:hash g) == H'` AND `(:prev-hash g) == H` -> `{:replaced G}` (even if `G` ended or just taken out; `t1`, `t2` dropped) | 1. `:not-live` (`g:<G>` absent or `:ended? true`)<br>2. `:wrong-secret` (`(:hash g) != H`, including after a newer rotation or when `H` is `prev-hash`) |
| `end-sign-in` | Yes (`:at`) | `g:<G>` exists with `:ended? true` (including just taken out) -> `{:ended G}` (no change) | 1. `:no-such-sign-in` (`g:<G>` absent under `P`) |
| `make-actor` | Yes (`:at`) | `a:<A>` exists with `(:made-by a) == B` -> `{:made A}` (even if `A` or `B` ended; `:label`, `:at` dropped) | 1. `:no-such-maker` (`r` not `RootEntered`, or `B != P` and `a:<B>` absent)<br>2. `:ended` (`B != P` and `a:<B>` has `:ended? true`)<br>3. `:in-use` (`A == P` or `a:<A>` exists with another maker)<br>4. `:too-deep` (`(:depth B) == 32`) |
| `end-actor` | Yes (`:at`) | `a:<A>` exists with `:ended? true` -> `{:ended A}` (no change) | 1. `:no-such-actor` (`r` not `RootEntered`, or `A != P` and `a:<A>` absent)<br>2. `:is-a-person` (`r` is `RootEntered` and `A == P`) |
| `open-session` | Yes (`:at`) | `s:<S>` exists with `(:sign-in s) == G`: if `:closed? false` and `t1 > (:until s)`, moves `:until` to `t1` (never alters `:driven-by`); if `:closed? true` or `t1 <= (:until s)`, no change -> `{:open S}` | 1. `:not-live` (`g:<G>` absent or `:ended? true`)<br>2. `:in-use` (`s:<S>` exists under another sign-in `G2 != G`, open or closed) |
| `close-session` | Yes (`:at`) | `s:<S>` exists with `:closed? true` (including just taken out) -> `{:closed S}` (no change) | 1. `:no-such-session` (`s:<S>` absent under `P`) |
| `ask` | No | `$$asks[U]` exists with `(:hash ask) == H` -> `{:asked U}` (even if answered or over; `:asks`, `:ends`, `:at` dropped) | 1. `:in-use` (`$$asks[U]` exists with different `:hash`) |
| `answer-ask` | Yes (`:at`, on `:root P`, on every path) | `$$asks[U]` is `AskYes` -> `{:yes {:root P0 :actor A0 :sign-in G0}}`; `AskNo` -> `{:no U}` (whatever this caller's `:yes?`, `:root`, or `:at` is) | 1. `:no-such-ask` (`$$asks[U]` absent)<br>*(standing answer check runs here)*<br>2. `:over` (`born-over?` or `:at >= :ends`)<br>3. `:no-such-actor` (`r` not `RootEntered`; or `:yes?` with `new-agent == nil` and `A != P` and `a:<A>` absent)<br>4. `:ended` (`:yes?` with `new-agent == nil` and `A != P` and `a:<A>` `:ended? true`)<br>5. `:in-use` (`:yes?` with `new-agent != nil` and `(A == P or a:<A> exists)`, OR `g:<G>` exists) |

**Boundary note on refusals:** Any refused write binds nothing (`SPEC.md` line 91): sending the exact same write after state changes (e.g., `enter` after a later `invite`, `begin-sign-in` after `P` enters, `answer-ask` after `A` is created) is evaluated fresh and can succeed. Only `:root`-write take-outs persist across refusals (`SPEC.md` line 328–329).

## Contested choices

| Choice | Alternative | Ground that settles it |
|---|---|---|
| Single stream topology `"auth"` with monotonic state machines + wait-free helping for `invite`, `enter`, `answer-ask` | Microbatch topology (either for all writes or for multi-key writes alone) | Microbatch cannot participate in `foreign-append! :ack` or `ack-return>`, adds >= 300 ms latency (missing tens-of-ms targets on `open-session`/`replace-secret`), coordinates every cycle on task 0, and cannot share PState write ownership of `$$root-data`/`$$live` with a stream topology. Moreover, microbatch does not eliminate conflicting same-batch cross-key races (`invite(P, C1)` vs `invite(P, C2)`). |
| Flat top-level `$$root-data` (`{String -> IRootDatum}`) with 10 typed ASCII key prefixes | Nested `{UUID -> fixed-keys-schema}` with subindexed inner maps, or 6 separate flat PStates | Nested subindexes pay an extra parent seek on every lookup (`who-is` jumps from 3 seeks / 1.5 ms to 5 seeks / 2.5 ms — +67% I/O on the dominant read). Six separate flat PStates have identical seeks but multiply per-task RocksDB memory/cache overhead 6× and destroy prefix block locality for root `P`. |
| Separate top-level `$$live` (`{String -> ILiveEntry}`) watched by `(sorted-map-range "<P>\0" "<P>\1")` | Non-subindexed `{UUID -> {:sign-ins ... :sessions ...}}`, or merging `$$live` into `$$root-data` | Open sessions have no protocol cap (can exceed 100), so a non-subindexed map causes full-map read-modify-write per `open-session` and violates the subindexing rule. Merging `$$live` into `$$root-data` runs range-proxy diff matching on every history/index write (4–5× extra reactivity overhead) and churns `NONE>` tombstones in the permanent history LSM tree. Probe verified `$$live` delivers incremental `KeyDiff`/`KeyRemoveDiff`/`MultiDiff`. |
| Sorted expiry index `exp:<hex-until>:<kind>:<id>` in `$$root-data` | Scanning all entries of `$$live` for `P` on every `:root` write | With `exp:`, take-outs when nothing has expired cost 1 seek + 0 iter-reads (`0.5 ms`) even for a root with 2,000+ open sessions; scanning `$$live` costs `0.5 ms + O(open sessions × 5 µs)` on every `open-session` and `replace-secret`. Maintaining `exp:` uses zero-read `termval` / `NONE>` writes. |
| Separate PII-free `$$enter-ops` (`{UUID -> IEnterOpState}`) for 2-claim `enter`, with a 1-claim fast path that skips `$$enter-ops` | Storing `AccountKey` inside `$$claims` (`ClaimClaiming`), or direct `C_code -> C_email` fallback without a commit record | Direct fallback allows a retry or delayed helper to claim `C_code` after a concurrent `invite` while the primary attempt claims `C_email` (double-claiming two persons for one account). Storing `AccountKey` in `$$claims` leaks PII into code claims. `$$enter-ops` eliminates double-claiming with zero PII and zero overhead on returning users and 1-claim enters. |
| `answer-ask` on `*asks` (`hash-by :code`) with `AskAnswering` + `ok:<op-id>` on `H(P)` | `answer-ask` on `*root-events` (`hash-by :root-id`), or writing `AskYes` on `H(U)` before writing `A`/`G` on `H(P)` | Committing `AskYes` before `H(P)` exposes `:yes` in `ask-of` while `who-is` still returns `nil` (violating `IMPLICIT_SPEC.md:670`). Putting `answer-ask` on `*root-events` adds an extra hop (3 vs 2) and exposes `*root-events` to cross-task `:all-after` retry storms. Recording `ok:<op-id>` on `H(P)` for both accepts and refusals makes retries and concurrent helpers strictly deterministic even if `P`'s state changes afterward. |
| Conditional actor lookup in `who-is` (read `a:<A>` only when `G` is ended and `A != P`) | Always reading `a:<A>` in `who-is`, or denormalizing `:actor-ended?` onto every `g:<G>` row | Denormalizing `:actor-ended?` onto `g:<G>` breaks when `end-actor` runs after `G` has already ended (`IMPLICIT_SPEC.md:1460` requires `:actor-ended? true` without scanning unbounded ended sign-ins). Always reading `a:<A>` wastes 1 seek (+33% disk I/O) on every live sign-in, where atomic `end-actor` guarantees the actor is live. |
| Non-yielding BFS cascade in `end-actor`; `{:allow-yield? true}` on `ao:` scan in `"under"` | Yielding inside `end-actor`, or never yielding in `"under"` | Yielding inside `end-actor` breaks read-to-write atomicity (`dataflow.md:199`), allowing concurrent `make-actor`/`begin-sign-in` to slip under an ending subtree. `"under"` is read-only over a snapshot (`dataflow.md:195`), so yielding there protects `who-is` latency with zero correctness risk. |
| Client-side `ping-task` liveness monitor (2 s interval, 0 seeks) + 5 s open timeout + `UngracefulTerminationDiff` | Relying solely on Rama's `UngracefulTerminationDiff` | Probe `auth_rama_probe.clj` proved Rama's `ProxyState` does not emit `UngracefulTerminationDiff` within 15 s of `destroy-module!` under default client config (~100 s for 10 failures in 120 s), failing `SPEC.md`'s *"within seconds of the loss"*. `ping-task` delivers `:not-reached` in 2–5 s without requiring custom `RamaClusterManager` config from callers of `open`. |
| `watch-root` awaits `foreign-proxy-async` open (bounded 5 s) before returning `stop` | Returning `stop` immediately before the proxy registers on `H(P)` | Returning before server subscription registration opens a blind window in `SPEC.md`'s *"registers first and then reads with `who-is`"* pattern: an `end-sign-in` occurring right after `who-is` could land before the proxy opens and be swallowed by the suppressed initial `ResyncDiff`. |
| Four depots (`*root-events`, `*asks`, `*invites`, `*enters`) | One depot or two combined depots | Separating `*invites` and `*enters` isolates all PII into the two lowest-volume logs for future `depot-migration` erasure without scanning `*root-events` or `*asks`, and gives each write 0 initial hops to its primary key. |
| Store immutable `:initial-hash` beside mutable `:hash` on `SignInRec` | Match `begin-sign-in` repeat against current `:hash` | After `replace-secret` rotates `:hash` from `H1` to `H2`, a delayed repeat of the original `begin-sign-in` (carrying `H1`) would falsely return `:in-use` instead of `{:begun G}`. |
| No per-event dedup table for `*root-events` | Store every processed event UUID in `$$root-data` | Every entity state already deduplicates its own write; a per-event table adds +1 seek and +1 write (+33% I/O) on every `open-session`/`replace-secret` plus unbounded storage solely to guard an extreme clock-skew-exceeds-session-lifetime crash window (documented in `Walls`). |

## Walls

Where a requirement in `SPEC.md` or `IMPLICIT_SPEC.md` cannot be met strictly as written under Rama's execution model without breaking another core requirement:

1. **Cross-partition mid-flight read visibility (`IMPLICIT_SPEC.md` line 115–116 vs `SPEC.md` line 382 + Rama's independent task commits).**
   - *The wall*: `IMPLICIT_SPEC.md` line 115–116 requires *"No read shows part of one write's effects."* In Rama (both stream and microbatch — `stream.md:41`, `microbatch.md:128`), distinct partitions commit independently with no multi-partition read snapshot. Every single-partition write (all 9 writes on `P` or `U`, including 32-deep `end-actor` cascades and take-outs) satisfies this strictly at every instant, and after **any** write returns (`SPEC.md` line 93), every copy reads the full effect on all partitions. However, during the ~0.5 ms window **while `answer-ask` or first-time `enter` is still in flight**, a concurrent reader polling two different partitions can observe `A`/`G` on `H(P)` ~0.5 ms before `ask-of(U)` flips from `:waiting` to `:yes` (or observe `C` marked used on `H(C)` ~0.5 ms before `who-is(P)` shows `:own-layer`).
   - *Position*: Do not break `SPEC.md` line 382 (*"No one task that everything passes through"*) or `who-is` = 1 round trip to 1 task to add a global task or distributed read locks. Ordering `answer-ask` so `H(P)` commits **before** `H(U)` guarantees the critical causal direction (`ask-of` `:yes` => `who-is`, `under`, and `watch-root` already hold `A` and `G`, `IMPLICIT_SPEC.md:670`), and a losing `answer-ask` writes nothing to `H(P)`. Accept Rama's settled-result semantics during the sub-millisecond in-flight window.

2. **Isolated single-proxy errors on a healthy task under caller-supplied `cluster` config (`SPEC.md` line 211–213 vs `(open cluster module-name)`).**
   - *The wall*: `SPEC.md` requires `:softland.auth.client/not-reached` *"within seconds of the loss."* Probe `auth_rama_probe.clj` proved Rama 1.6.0 does not emit `UngracefulTerminationDiff` within 15 s under default client config (`foreign.proxy.failure.window.seconds = 120`, `threshold = 10` -> ~100 s). Our `ping-task` monitor + 5 s open timeout detects full cluster/module loss, partial worker/task loss, and open-time `pstate.proxy.limit` rejection within **2–5 seconds**. However, if a *single* already-open `foreign-proxy` encounters repeated server-side errors while its task remains healthy and answers `ping-task`, its termination timing is controlled by the `RamaClusterManager` passed into `(open cluster module-name)`, which `Auth`'s client cannot reconfigure after construction.
   - *Position*: Keep the `(open cluster module-name)` signature unchanged; rely on `ping-task` + open timeout for all connectivity/worker/module losses within 2–5 s, and recommend that the runtime configure `foreign.proxy.failure.window.seconds = 10` and `foreign.proxy.failure.window.threshold = 2` when constructing `RamaClusterManager`.

3. **Uncheckpointed stream replay under backward clock skew larger than an entity's entire lifetime (`SPEC.md` lines 82–83, 326–334, 341–344 + `stream.md:166`).**
   - *The wall*: If Event 1 (`:at = 300`, fast clock) commits on `H(P)`, Event 2 (`:at = 150`, slow clock) immediately creates a sign-in/session `X` with `:until = 250` (`150 < 250 <= 300`), and the worker crashes *before* checkpointing stream progress (`topology.stream.checkpoint.progress.threshold`), Rama replays Event 1 (`:at = 300`) when `X` already exists in `$$live`. Event 1's replayed take-out at `300` ends `X` even though Event 1 originally preceded Event 2. (Under NTP-kept clocks — which `SPEC.md` line 343 already requires — and normal 1-day session spans, `until` is never behind a preceding copy's clock.)
   - *Position*: Do not add an unbounded per-event dedup table on `*root-events` (+1 seek and +1 write, +33% I/O on every `open-session`/`replace-secret`) to defend against clock skew exceeding a session's lifetime during an uncheckpointed crash. Accept this narrow window under `SPEC.md`'s existing clock-keeping requirement.

4. **`pstate.proxy.limit = 20000` bounds `(root, runtime-copy)` subscriptions, not distinct roots across unlimited copies (`SPEC.md` lines 363–365, 395–397).**
   - *The wall*: Empirical probe `proxy_limit_probe.clj` proved `pstate.proxy.limit` counts total open `ProxyState` subscriptions per `(module, PState, task)` across all connected runtime processes combined. Client-side deduplication ensures 1 subscription per `(root, runtime-copy)`, so 20,000 holds 20,000 distinct watched roots per task when each root connects to 1 runtime copy, or `20,000 / K` distinct roots if every root holds simultaneous connections across `K` runtime copies.
   - *Position*: Launch `$$live` with `pstate.proxy.limit = 20000` as specified (`SPEC.md:363`). Because it is a Rama dynamic option, operators can raise it to `20000 × K` at runtime via `rama setOption` if multi-copy fan-out per root grows.

## A session's choices, each Sid's to turn

1. **`:driven-by` on `open-session` and `who-is` (added to [SPEC.md](SPEC.md)):** Optional `{:kind :person}` or `{:kind :model :model <text <= 256 chars>}`, kept with the session after close, unchanged on repeat, omitted from `watch-root`, absent/nil means not said and the runtime keeps reads. Rests on Sid's 12 October decision, entry 4 (`src-store/README.md`) and runtime page entry 8.
2. **`answer-ask` with `:yes? false` (a denial):** `:actor`, `:sign-in`, `:until`, and `:new-agent` may be `nil` or omitted when `:yes? false` (still type-validated if non-nil), so callers denying an ask do not invent dummy UUIDs/timestamps. Alternative: require all fields non-nil per the literal signature map.
3. **Unknown extra keys in input maps:** Rejected with `{:could-not :read-the-event}` on both top-level call maps and sub-maps (`:claim`, `:making`, `:new-agent`, `:driven-by`) so typos (e.g. `:driven_by`) fail fast rather than silently dropping. Alternative: ignore unknown keys for forward compatibility.
4. **Provider keyword length bound:** Measured on full keyword text (`ns/name` if namespaced, else `name`) `<= 256` JVM `char`s. Alternative: disallow namespaced provider keywords and check `(name provider) <= 256`.
5. **Fixed-width integer timestamps:** Accept JVM `Byte`, `Short`, `Integer`, and `Long` (rejecting `BigInt`, `BigInteger`, floats, nil) and coerce to `Long` before depot append. Alternative: accept `Long` only.
6. **Born-over `ask` (`:ends <= :at`):** Persist `:born-over? true` on `$$asks[U]` so any later `answer-ask` returns `{:refused :over}` even from a runtime copy whose clock is behind `:ends`, while `ask-of` continues returning `:state :waiting` with `:ends`. Alternative: judge `answer-ask` solely by `:at >= :ends`.
7. **`watch-root` awaits `foreign-proxy-async` open (up to 5 s) before returning `stop`:** Eliminates the register-before-`who-is` missed-end race; on open failure/timeout, returns a no-op `stop` and delivers `:softland.auth.client/not-reached` once on the client callback executor. Alternative: return `stop` immediately without waiting for proxy registration.
8. **`watch-root` liveness monitor cadence and timeouts:** `ping-task` every **2 s** (2 s timeout) while >= 1 watch is open, with a **1 s** per-watched-root fallback probe during partial worker outages (`not-reached` within 2–5 s), plus recommending `foreign.proxy.failure.window.seconds = 10` / `threshold = 2` on the caller's `RamaClusterManager`.
9. **Client blocking operation timeout:** Default **10 seconds** for writes, `who-is`, `under`, and `ask-of` before returning `{:could-not :reach-the-module}` (keeping `(open cluster module-name)` unchanged).
10. **PII erasure pointers on `$$root-data["<P>\0r"]`:** Store `:linked-provider`/`:linked-subject` and `:claim-kind`/`:claim-val` pointers on `RootEntered` so future erasure by person ID `P` runs in O(1) without scanning `$$accounts` or `$$claims`; keep `*invites` and `*enters` isolated for future Rama `depot-migration` or per-depot trim.
11. **`depot.ack.failure.on.any.streaming.failure` left at Rama's default (`true`):** First-attempt stream failures return `{:could-not :reach-the-module}` and rely on the contract's idempotent retry. Alternative: override to `false` as a fixed module config so Rama waits out retries up to the append timeout.
12. **Internal Rama object names:** Depots `*root-events`, `*asks`, `*invites`, `*enters`; PStates `$$root-data`, `$$live`, `$$accounts`, `$$claims`, `$$asks`, `$$enter-ops`; stream topology `"auth"`; query topologies `"who-is"`, `"under"`, `"ping-task"`. None cross the `softland.auth.client` contract.

## What was run and what was only read

### Run on Rama 1.6.0 `InProcessCluster` (in this session, behind `cluster.lock`)
1. `/mnt/data/projects/research/softland/runtime-spine-2026-11-21/plan-probes/auth_rama_probe.clj`:
   - Verified `foreign-proxy` on a top-level `{String ILiveEntry}` PState (`definterface` + `defrecord`) with `[(sorted-map-range from to)]` and `{:pkey root}`: initial value `{}` via `ResyncDiff`, followed strictly by incremental diffs (`NewValueDiff`, `KeyDiff`, `KeyRemoveDiff`) — zero spurious `ResyncDiff`s on subsequent mutations.
   - Verified `set-launch-pstate-dynamic-option!` with `"pstate.proxy.limit" 20000` accepted on module launch.
   - Measured post-`destroy-module!` behavior on open `foreign-proxy` handles under default client config: **neither** flat nor nested proxies emitted `UngracefulTerminationDiff` within 15 seconds (confirming why client-side `ping-task` liveness probing is required to deliver `:softland.auth.client/not-reached` within seconds).
2. `/mnt/data/projects/research/softland/runtime-spine-2026-11-21/plan-probes/auth_rama_probe_2.clj`:
   - Verified multiple `local-transform>` writes (`NONE>` on 1 sign-in + 2 sessions) inside one single-task stream topology event deliver **one atomic `MultiDiff`** callback to `foreign-proxy` (zero intermediate snapshots where a sign-in disappears before its sessions).
   - Verified `<<query-topology` `"who-is"` with leading `(|hash *root)`, conditional `<<if` skips (0 actor seek when live, 1 actor seek when ended agent), and `(|origin)` compiles and returns exact values in 1 invocation.
   - Verified `<<query-topology` `"ping-task"` with leading `(|direct *task-id)` routes directly to tasks `0..3` (`[true true true true]`) and `(foreign-object-info pstate)` returns `{:num-partitions 4}`.

### Read only (not re-run in this session)
- **Project specs and decisions**: `AGENTS.md`, `src/proposal/runtime-2026-11-21/PROPOSAL.md`, `src/proposal/runtime-2026-11-21/spine/auth/SPEC.md` (with Step 1 `:driven-by` addition committed at `37bc0e01`), `src/proposal/runtime-2026-11-21/spine/auth/IMPLICIT_SPEC.md`, `src-store/README.md`, `src-store/OPEN-QUESTIONS-rama.md`, `src/proposal/runtime-2026-11-21/WORKFLOW-AND-MODEL.md` (including its previously measured `probes/proxy_limit_probe.clj` output for `pstate.proxy.limit` scoping).
- **Store reference code (patterns only, not run)**: `src-store/softland/store/record/tables.clj`, `src-store/softland/store/read_path.clj`, `src-store/softland/store/read_path/sources.clj`.
- **Rama skill and local Rama documentation**: `.agents/skills/rama/SKILL.md` and references (`phase-1-plan.md`, `artifact-plan.md`, `artifact-plan-validation.md`, `app-design.md`, `query-topologies.md`, `pstate-schema.md`, `pstate-schema-clojure-api.md`, `patterns.md`, `depot-design.md`, `depot-reference.md`, `depot-migration.md`, `microbatch.md`, `stream.md`, `batch.md`, `dataflow.md`, `paths.md`, `core-concepts.md`, `formal-model.md`, `syntax.md`, `task-globals.md`, `pstate-migration.md`, `operate.md`), plus `/mnt/data/projects/research/softland/rama-reference/docs/` (`11-stream-topologies.md`, `14-depots.md`, `15-pstates.md`, `17-serialization.md`, `26-all-configs.md`).
- **Untouched / unopened**: `spine/auth/second-view/`, `spine/runtime-oracle/`, `test-auth/`, `src-auth/`, `overnight-audit/`, `spine/auth/DECOMPOSITION.json`, `spine/auth/PLAN-invite-and-enter.md`, `src/app/server/env.clj`, and any `.env` file.
- **Not yet run**: No `Auth` module code, no `softland.auth.client` code, no 20,000-proxy stress measurement, no multi-copy runtime kill-`9` proof, and no `PLAN_VALIDATION.md` check (reserved for a separate checking session).
