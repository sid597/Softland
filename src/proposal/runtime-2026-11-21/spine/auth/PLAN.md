# Plan: Auth

Whole-module design for the Rama module `Auth` and its foreign client namespace `softland.auth.client`, covering all 18 calls of [SPEC.md](SPEC.md) (including Sid's two calls and additions of 22 November, and [IMPLICIT_SPEC.md](IMPLICIT_SPEC.md) wherever it does not conflict with `SPEC.md`). Revised per [PLAN_VALIDATION.md](PLAN_VALIDATION.md) Revisions 1–6 and 8 (Revision 7 is closed for this build: Auth's actor depth stays at 32 makers below root, matching `SPEC.md` and `reference.clj`). Cross-key agreement verified on carbon's unchanged model checker in [`agreement/`](agreement/README.md) (`128` scenario×mode runs, `88,261` states, `0` breaks).

## Reads

Every read judges no clock (`SPEC.md` "No clock"): timestamps (`:until`, `:previous`, `:ends`, `:begun`) are returned as stored and evaluated against the runtime's own clock. Every read validates its input in `softland.auth.client` before touching Rama; invalid input (including a non-client first argument) returns `{:could-not :read-the-event}`, a closed client returns `{:could-not :closed}` (including from `watch-root` in place of the stop function), and any Rama exception or timeout returns `{:could-not :reach-the-module :error ...}`.

| Read | Access method | Target PState(s) & path expression(s) | Partition | Cost (seeks + iters) & latency | Throughput |
|---|---|---|---|---|---|
| `who-is` `{:root P :sign-in G :session S-or-nil}` | Query topology `"who-is"` (`foreign-invoke-query`) | `$$root-data`: `(keypath "<P>\0g:<G>")` **first**; fast path skips `"<P>\0r"` when `G` exists and `:own-layer-made-at-begin?` is `true` (falls back to `(keypath "<P>\0r")` only when `G` is absent or `:own-layer-made-at-begin?` is `false`); conditionally `(keypath "<P>\0s:<S>")` when `S != nil`; conditionally `(keypath "<P>\0a:<A>")` only when `G` is ended and `A != P` | `\|hash P` (leading partitioner; 1 RT directly to 1 task) | 1–4 point seeks (typically **2** with `S`, **1** without `S`), 0 iter-reads; ~0.5–1.5 ms disk + 1 RT | Highest by orders of magnitude: every HTTP request & connection open |
| `ask-of` `U` | Direct `foreign-select-one` | `$$asks`: `[(keypath U)]` | `\|hash U` (1 RT directly to 1 task) | 1 point seek, 0 iter-reads; ~0.5 ms disk + 1 RT | Polls × waiting asks (low–moderate) |
| `under` `P` | Query topology `"under"` (`foreign-invoke-query`) | `$$root-data`: `(keypath "<P>\0r")`; if `RootEntered` and `next-actor-seq > 0`, `(sorted-map-range "<P>\0ao:" "<P>\0ao;")` with `{:allow-yield? true}`; if `RootEntered` and `next-sign-in-seq > 0`, `$$live`: `(sorted-map-range "<P>\0g:" "<P>\0g;")` (live sign-ins only, sorted by `:seq`; never walks ended sign-ins) | `\|hash P` (leading partitioner; 1 RT directly to 1 task) | 1–3 seeks + `A` agent iters + `L` live sign-in iters (~1–7 ms even for 1,000 agents) | Low: person opens agents/sign-ins view |
| `watch-root` `P` `f` | Shared `foreign-proxy-async` per `(client, P)` + internal liveness query `"ping-task"` | `$$live`: `[(sorted-map-range "<P>\0" "<P>\1")]` with `{:pkey P}` | `\|hash P` (1 persistent subscription per `(runtime copy, P)`) | Opening: 1 range scan (`1 seek + (L + O) × 5µs`). Steady state: 0 seeks per push (incremental `KeyDiff`/`KeyRemoveDiff`/`MultiDiff`) | One subscription per `(runtime copy, watched root)`, up to 20,000 per task |
| *(Internal)* `ping-task` `task-id` | Query topology `"ping-task"` (`foreign-invoke-query-async`), run every 2 s (5 s timeout + 3 s fallback probe = 8 s outage ceiling) only while watches are open | None (in-memory check of current task id) | `\|direct task-id` (leading partitioner; 1 RT directly to `task-id`) | 0 seeks, 0 iter-reads; < 0.5 ms | `N` in-memory RPCs / 2 s per runtime copy that holds >= 1 watch |

**Output projections and contract details:**

- **`who-is`**: Returns `{:sign-in {:actor A :kind K :hash H :until t1 :previous {:hash H0 :until t2}-or-nil :ended? bool :actor-ended? bool}-or-nil, :session {:sign-in G' :until t3 :closed? bool :driven-by D-or-nil}-or-nil, :own-layer L-or-nil, :own-layer-made? bool}`. Never exposes `:making` or PII (`account`, `email`, `:says`). `:driven-by` is returned inside `:session` (`{:kind :person}`, `{:kind :model :model text}`, or `nil` when absent) both while the session is open and after it closes.
- **`ask-of`**: Projects `AskWaiting` and in-flight `AskAnswering` identically to `{:hash H :asks text :ends t1 :state :waiting :answer nil}` so uncommitted answers are never visible. Projects `AskYes` to `:state :yes :answer {:root P :actor A :sign-in G}` and `AskNo` to `:state :no :answer nil`. Absent code returns `nil`.
- **`under`**: Returns `{:actors [{:actor A :made-by B :label text :ended? bool} ...] :sign-ins [{:sign-in G :actor A :kind K :label text-or-nil :begun t :until t1} ...]}`. Agents (`ao:` range) are stored in ascending 16-hex sequence order and exclude `P` herself. Live sign-ins (`$$live` `g:` range) hold live sign-ins only (carrying `:seq`) and are sorted in memory by `:seq` (tens of items) so `under` preserves exact begin order and never walks ended sign-ins. Unknown or waiting `P` short-circuits after 1 seek and returns `{:actors [] :sign-ins []}`.
- **`watch-root`**: Projects `$$live` entries into `{:sign-ins {G {:actor A :kind K :hash H :until t1 :previous {:hash H0 :until t2}-or-nil}} :sessions {S {:sign-in G :until t3}}}`. Deliberately omits `:driven-by`, `:begun`, `:label`, `:seq`, `:initial-hash`, `:own-layer`, and `:own-layer-made-at-begin?` from the delivered map. Initial `ResyncDiff` is suppressed so no call is owed at registration and a root with nothing live receives no call until something live appears. Every subsequent change delivers the updated value; unreachability delivers `:softland.auth.client/not-reached` once and terminates the watch.

## Writes

Every write is validated in `softland.auth.client` before touching Rama; malformed input (including a non-client first argument) returns `{:could-not :read-the-event}` and appends nothing to any depot. Every valid write appends exactly one record to one depot with `:ack` (waits for depot replication + full stream event-tree completion + PState replication across all participating tasks) and receives its exact contract result map via `(ack-return> result)` under topology `"auth"`. Any Rama exception or client timeout is caught and returned as `{:could-not :reach-the-module :error ...}` (which makes no promise about whether the write was taken; re-sending the same map is always safe).

**Lifecycle calls (no depot append):**

- **`open` `(open cluster module-name)`**: Validates `cluster` (a Rama cluster manager / `InProcessCluster`) and `module-name` (a `String`; non-cluster or non-string returns `{:could-not :find-the-module ...}` per `SPEC.md:134–136` and `reference.clj`). Resolves foreign handles to the 4 depots (`*root-events`, `*asks`, `*invites`, `*enters`), 2 directly-read PStates (`$$asks`, `$$live`), and 3 query topologies (`"who-is"`, `"under"`, `"ping-task"`), verifies module presence and reads `num-partitions` from `(foreign-object-info $$live)`. Never throws: missing module returns `{:could-not :find-the-module ...}`, unreachable cluster/timeout returns `{:could-not :reach-the-module ...}`. Safe for concurrent use across all threads of a runtime JVM; holds no Auth state other than open watch subscriptions and a closed flag.
- **`close!` `(close! client)`**: If given anything that is not an Auth client, returns `nil` (`SPEC.md:138–140`, `reference.clj`). Idempotently marks the client closed, stops the background `ping-task` liveness monitor, closes all open `foreign-proxy` handles, clears the watch registry, and returns `nil`. Calls no watch callback `f` after returning (suppresses `DestroyedDiff` and uses a per-watch turn + re-entrant `ThreadLocal` guard so `close!` never deadlocks if invoked from inside `f`). Subsequent calls on the client (including `watch-root`) return `{:could-not :closed}`.

**The 12 write operations:**

| Write | Depot | Depot partitioner | Event record type | Takes out under `:root`? | Result delivered via `ack-return>` | Repeat key (checked first) | Refusal order (`SPEC.md` governs) | Latency & throughput |
|---|---|---|---|---|---|---|---|---|
| `invite` | `*invites` | `(hash-by :claim)` | `InviteEvent` | No (`SPEC.md` line 332) | `{:invited P}` or `{:refused ...}` | Same `(P, C)` (even after `P` entered) | `:claim-in-use`, `:person-in-use` (enforced via `H(C)` re-check before `:person-in-use`) | Multi-hop (3 hops uncontended, ~3–5 ms); low–bursty (list invites) |
| `enter` | `*enters` | `(hash-by :account)` | `EnterEvent` | No (`SPEC.md` line 332) | `{:person P :own-layer L' :making {U' M'} :own-layer-made? bool}` or `{:refused :not-invited}` | Already-linked account returns fixed `P, L', M'` + present flag; updates `:says` only when `(> at says-at)` (Sid's Call 2) | `:not-invited` | Returning: 1 hop (`at <= says-at`) or 3 hops (`at > says-at` with `H(op)` replay check, ~2–3 ms). First-time: 5–6 hops (~4–6 ms); moderate |
| `own-layer-made` | `*root-events` | `(hash-by :root-id)` (`:person`) | `OwnLayerMadeEvent` | No (`SPEC.md` line 332) | `{:made P}` or `{:refused :no-such-person}` | Already `:own-layer-made? true` -> `{:made P}` | `:no-such-person` | 0 hops (~1 ms); once per entered person + repeats |
| `begin-sign-in` | `*root-events` | `(hash-by :root-id)` (`:root`) | `BeginSignInEvent` | Yes (`:at`) | `{:begun G}` or `{:refused ...}` | Same `(P, G, A, K, initial-hash H)` (even after secret rotations or end) | `:no-such-actor`, `:ended`, `:in-use` | 0 hops (~1–2 ms); few/day/person + agents |
| `replace-secret` | `*root-events` | `(hash-by :root-id)` (`:root`) | `ReplaceSecretEvent` | Yes (`:at`) | `{:replaced G}` or `{:refused ...}` | `G` under `P` with current hash `= H'` and previous hash `= H` (even if ended) | `:not-live`, `:wrong-secret` | 0 hops (~1–2 ms); high volume (with `open-session`) |
| `end-sign-in` | `*root-events` | `(hash-by :root-id)` (`:root`) | `EndSignInEvent` | Yes (`:at`) | `{:ended G}` or `{:refused :no-such-sign-in}` | `G` under `P` already ended -> `{:ended G}` | `:no-such-sign-in` | 0 hops (~1–5 ms, 1 `$$live` `s:` scan); low–moderate |
| `make-actor` | `*root-events` | `(hash-by :root-id)` (`:root`) | `MakeActorEvent` | Yes (`:at`) | `{:made A}` or `{:refused ...}` | Same `(P, A, made-by B)` (even if `A` or `B` ended) | `:no-such-maker`, `:ended`, `:in-use`, `:too-deep` (depth 32) | 0 hops (~1–2 ms); rare (up to thousands/person lifetime) |
| `end-actor` | `*root-events` | `(hash-by :root-id)` (`:root`) | `EndActorEvent` | Yes (`:at`) | `{:ended A}` or `{:refused ...}` | Agent `A` under `P` already ended -> `{:ended A}` | `:no-such-actor`, `:is-a-person` | 0 hops, 3-seek 2-scan (`a:` + `ao:` + `$$live`) atomic cascade via flat `ops/explode` (`< 2 s` even at 3,000/2,000/5,000); rare |
| `open-session` | `*root-events` | `(hash-by :root-id)` (`:root`) | `OpenSessionEvent` | Yes (`:at`) | `{:open S}` or `{:refused ...}` | Same `(P, S, sign-in G)`: if open & `t1 > until`, extends `:until` (never alters `:driven-by`); if closed/taken-out or `t1 <= until`, no-op | `:not-live`, `:in-use` | 0 hops (~1–2 ms); highest write volume (thousands/day/agent + 1/half-day refresh) |
| `close-session` | `*root-events` | `(hash-by :root-id)` (`:root`) | `CloseSessionEvent` | Yes (`:at`) | `{:closed S}` or `{:refused :no-such-session}` | `S` under `P` already closed -> `{:closed S}` | `:no-such-session` | 0 hops (~1–2 ms); high volume (below `open-session`) |
| `ask` | `*asks` | `(hash-by :code)` | `AskEvent` | No (`SPEC.md` line 332) | `{:asked U}` or `{:refused :in-use}` | Same `(U, hash H)` (even if answered or over) | `:in-use` | 0 hops (~1 ms); low–moderate, unbounded over time |
| `answer-ask` | `*asks` | `(hash-by :code)` | `AnswerAskEvent` | Yes (`:at`, on `:root P`, on every path including refusals, standing answers, and helped `P == P0`) | `{:yes {:root P :actor A :sign-in G}}`, `{:no U}`, or `{:refused ...}` | Standing answer (`:yes` or `:no`) on `U` returns what stands for any caller/time (after take-out under `P`) | `:no-such-ask`, *(standing answer)*, `:over`, `:no-such-actor`, `:ended`, `:in-use` | 2 hops on `:yes`/`:no` (`H(U) -> H(P) -> H(U)`), 1 hop on early refusal/standing (`H(U) -> H(P)` for take-out); low |

**How a write's result gets back to its caller:**
Every write appends via `(foreign-append! depot event :ack)` and extracts `(get ack-map "auth")`. Because Rama does not deliver `AckReturnMap` until the entire stream event tree (including all cross-task hops and all PState replication to leader + ISR followers) finishes (`11-stream-topologies.md`, `14-depots.md:206`), receiving the result map guarantees that every PState mutation of that write is committed and readable from every copy of the runtime. If `ack-map` is missing `"auth"` or `foreign-append!` throws or times out, the client returns `{:could-not :reach-the-module ...}`.

## PState Design

`Auth` declares **six** partitioned PStates, all owned by the single stream topology `"auth"`, with **zero** global PStates, **zero** `Object` schema positions, and **zero** un-subindexed collections that can exceed an explicitly enforced bound.

### Key-encoding conventions (pure 7-bit ASCII for sorted range safety)

All composite keys in range-scanned PStates (`$$root-data`, `$$live`) use strictly 7-bit ASCII characters (`0-9`, `a-f`, `-`, `:`, `\u0000`, `\u0001`, `;`). Because UTF-8 byte order and Java UTF-16 order are identical on 7-bit ASCII, no key can trigger Rama's rare-codepoint range mismatch (`src-store/OPEN-QUESTIONS-rama.md`, section 5).

- **UUID text `<P>`, `<A>`, `<G>`, `<S>`, `<B>`, `<op>`**: canonical 36-char lowercase UUID string.
- **Order-preserving 64-bit timestamp `<hex-t>`**: `(bit-xor t Long/MIN_VALUE)` formatted as 16 lowercase hex digits. Maps the full signed `Long` range `[Long/MIN_VALUE, Long/MAX_VALUE]` strictly monotonically into `["0000000000000000", "ffffffffffffffff"]`, so `until <= at` is the exact range `["<P>\0exp:", "<P>\0exp:<hex-at>;")` with zero overflow or underflow (`:` is `0x3a`, `;` is `0x3b`).
- **Order-preserving 64-bit sequence `<hex-seq>`**: unsigned 16 lowercase hex digits (`0000000000000000` upwards).

### 1. `$$root-data` — Durable per-root records, history, and expiry/outcome indexes

- **Partitioning**: `|hash P` (all keys for root `P` live on task `hash(P) mod N`; accessed exclusively via `local-select>` / `local-transform>` after depot alignment or `(|hash *root)`).
- **Schema**: `{String -> IRootDatum}` where `IRootDatum` is a Java interface implemented by strictly typed records (no `Object`, no optional-field soup; **7 key prefixes** — `ch:`, `ag:`, and `sg:` are deleted per Revision 2):
  - Key `"<P>\0r"` (1 per root `P`):
    - `RootReserving [claim-kind<Keyword> claim-val<String> invited-by-kind<Keyword> invited-by-id<UUID|nil> invited-at<Long>]`
    - `RootInvited [claim-kind<Keyword> claim-val<String> invited-by-kind<Keyword> invited-by-id<UUID|nil> invited-at<Long>]`
    - `RootEntered [claim-kind<Keyword|nil> claim-val<String|nil> invited-by-kind<Keyword> invited-by-id<UUID|nil> invited-at<Long> linked-provider<Keyword|nil> linked-subject<String|nil> own-layer<UUID> making-uuid<UUID> making-session<UUID> own-layer-made?<Boolean> next-actor-seq<Long> next-sign-in-seq<Long>]`
  - Key `"<P>\0a:<A>"` (1 per agent of `P`; never deleted):
    - `ActorRec [actor<UUID> made-by<UUID> depth<Long> label<String> ended?<Boolean> seq<Long>]`
  - Key `"<P>\0ao:<hex-seq>"` (1 per agent of `P`, sorted by creation order for `under` AND 1-scan `end-actor` cascade; never deleted):
    - `ActorOrderRec [actor<UUID> made-by<UUID> depth<Long> label<String> ended?<Boolean> seq<Long>]`
  - Key `"<P>\0g:<G>"` (1 per sign-in of `P`, live or ended; never deleted; carries immutable `own-layer` and `own-layer-made-at-begin?` for 2-seek `who-is` per Revision 3):
    - `SignInRec [sign-in<UUID> actor<UUID> kind<Keyword> initial-hash<String> hash<String> until<Long> prev-hash<String|nil> prev-until<Long|nil> ended?<Boolean> begun<Long> label<String|nil> seq<Long> own-layer<UUID> own-layer-made-at-begin?<Boolean>]`
  - Key `"<P>\0s:<S>"` (1 per session of `P`, open or closed; never deleted):
    - `SessionRec [session<UUID> sign-in<UUID> until<Long> closed?<Boolean> driven-by<IDrivenBy|nil>]`, where `IDrivenBy` is implemented by `DrivenByPerson []` and `DrivenByModel [model<String>]` (nullable when `:driven-by` was omitted).
  - Key `"<P>\0exp:<hex-until>:<g|s>:<id>"` (1 per *live* sign-in or *open* session; removed via `NONE>` when ended/closed or replaced):
    - `ExpiryEntry [kind<Keyword> id<UUID> parent-id<UUID> until<Long>]` (`parent-id` is `actor` for `:sign-in`, `sign-in` for `:session`).
  - Key `"<P>\0ok:<op-id>"` (1 per `answer-ask` attempt evaluated on `P`, or Step-1b `:no-such-ask` refusal on `P`, for cross-partition helping and replay idempotency; never deleted):
    - `AskOutcomeYes [root<UUID> actor<UUID> sign-in<UUID>]`, `AskOutcomeNo [code<String>]`, `AskOutcomeRefused [reason<Keyword>]`.
- **Readers & writers**: Read by `who-is`, `under`, and every root-touching write. Written by `invite`, `enter`, `own-layer-made`, `begin-sign-in`, `replace-secret`, `end-sign-in`, `make-actor`, `end-actor`, `open-session`, `close-session`, `answer-ask`.
- **Growth**: History keys (`a:`, `ao:`, `g:`, `s:`, `ok:`) grow monotonically with lifetime actors (thousands/root), sign-ins (unbounded), sessions (millions/root for busy agents), and answered asks. Live expiry keys (`exp:`) stay strictly bounded by currently live sign-ins (tens/root) + open sessions (tens–hundreds/root).

**Alternatives weighed for `$$root-data`:**
- *Option A (chosen): Single flat top-level map `$$root-data` (`{String -> IRootDatum}`) with 7 prefixes (`r`, `a:`, `ao:`, `g:`, `s:`, `exp:`, `ok:`), omitting `ch:`, `ag:`, and `sg:`.*
  - `who-is` (dominant HTTP request, `S != nil`, live `G`, `:own-layer-made-at-begin? true`): **2** direct top-level point seeks (`g:<G>`, `s:<S>`) = `1.0 ms` disk, 0 iter-reads; `S = nil` is **1** seek (`0.5 ms`).
  - `open-session` (common write): 1 range seek on `exp:` (empty = `0.5 ms`) + 1 seek `s:<S>` + 1 seek `g:<G>` = `1.5 ms` reads + **2** direct no-read writes (`s:`, `exp:`) + 1 `$$live` write (no `sg:` write!).
  - `end-actor` (3,000 actors, 2,000 sign-ins, 5,000 sessions): **3** seeks total (`a:<A>`, `ao:` forward scan, `$$live` scan) — 39.6× faster than maintaining and traversing `ch:`/`ag:`/`sg:` (Revision 2).
  - `under` (1,000 agents, 10 live sign-ins): 1 seek `r` + 1 range scan `ao:` (`0.5 ms + 1,000 × 5µs = 5.5 ms`) + 1 range scan `$$live` (`0.55 ms`) = `~6.55 ms`.
  - Memory overhead: **1 RocksDB PState partition per task** for all root history and expiry index, with strong prefix locality (`"<P>\0*"` keys sit in adjacent SSTable blocks).
- *Option B: Nested `{UUID -> fixed-keys-schema {:sign-ins (subindexed) :sessions (subindexed) :actors (subindexed) :actor-order (subindexed) :expiries (subindexed) ...}}`.*
  - Every `local-select>` / `local-transform>` navigating `(keypath P :subindex K)` reads the top-level fixed-keys record at `P` first to locate the subindex handle, adding an extra seek per navigation (`who-is` jumps from 2 seeks to 4 seeks — **+100% disk seeks on the highest-throughput read**). Matches `src-store/softland/store/record/tables.clj`.
- *Option C: Six separate flat top-level PStates (`$$roots`, `$$actors`, `$$sign-ins`, `$$sessions`, `$$expiries`, `$$ask-outcomes`).*
  - Identical seek count to Option A, **6× the RocksDB partition memory/block-cache overhead per task**, and scatters one root's hot `r`/`g:`/`s:` keys across 3 separate RocksDB instances instead of contiguous blocks.
- *Option D (for take-outs): Omit `exp:` index and scan `$$live` on every `:root` write.*
  - On a root with 200 open sessions, every `open-session` / `replace-secret` scans 200 entries (`1.5 ms`); on a busy agent root with 2,000 open sessions, every write pays `10.5 ms` of scanning (20× worse throughput). With `exp:`, a write where nothing expired pays 1 seek + 0 iter-reads (`0.5 ms`) regardless of open-session count; maintaining `exp:` adds 0 extra reads (pure `keypath` + `termval` / `NONE>`).
- **Chosen: Option A with `exp:` and without `ch:`/`ag:`/`sg:`** — minimum seeks on `who-is` (2 seeks), O(expired) take-outs, 3-seek 2-scan `end-actor`, single RocksDB partition per task for all root history/expiries.

### 2. `$$live` — Watched live standing per root

- **Partitioning**: `|hash P` (written on task `H(P)`; watched via `foreign-proxy-async` with `{:pkey P}`).
- **Dynamic option set at module launch**: `(set-launch-pstate-dynamic-option! setup "$$live" "pstate.proxy.limit" 20000)`.
- **Schema**: `{String -> ILiveEntry}` where `ILiveEntry` is a Java interface implemented by:
  - Key `"<P>\0g:<G>"` (1 per live sign-in of `P`; carries full `SignInRec` non-PII fields so `end-actor` / `end-sign-in` / take-out construct updated `SignInRec` with `:ended? true` without point-seeking `g:<G>`):
    - `LiveSignIn [sign-in<UUID> actor<UUID> kind<Keyword> initial-hash<String> hash<String> until<Long> prev-hash<String|nil> prev-until<Long|nil> begun<Long> label<String|nil> seq<Long> own-layer<UUID> own-layer-made-at-begin?<Boolean>]`
  - Key `"<P>\0s:<S>"` (1 per open session of `P`; carries immutable `:driven-by` so `end-actor` / `end-sign-in` / take-out construct closed `SessionRec` without point-seeking `s:<S>`; `watch-root`'s client projection strictly omits `:driven-by`, delivering only `{:sign-in G :until t3}`):
    - `LiveSession [session<UUID> sign-in<UUID> until<Long> driven-by<IDrivenBy|nil>]`
- **Readers & writers**: Read by `watch-root` (`foreign-proxy-async` on `[(sorted-map-range "<P>\0" "<P>\1")]` with `{:pkey P}`), `under` (range scan `"<P>\0g:" .. "<P>\0g;"` on `H(P)`, live sign-ins only), `end-sign-in` / take-out (range scan `"<P>\0s:" .. "<P>\0s;"`), and `end-actor` (range scan `"<P>\0" .. "<P>\1"`). Written by `begin-sign-in`, `replace-secret`, `end-sign-in`, `end-actor`, `open-session`, `close-session`, `answer-ask` (`:yes`), and take-out on any `:root` write.
- **Growth**: Strictly bounded by currently live sign-ins (tens/root) and open sessions (tens–hundreds/root); every ended sign-in and closed/taken-out session is removed via `NONE>` in the same atomic event. Contains zero PII and zero byte arrays (ensuring incremental `KeyDiff`/`KeyRemoveDiff`/`MultiDiff`, verified by probe). Because `:driven-by` is immutable once a session opens, carrying it on `LiveSession` never generates spurious proxy diffs.

**Alternatives weighed for `$$live`:**
- *Option A (chosen): Separate top-level `{String -> ILiveEntry}` PState watched via `(sorted-map-range "<P>\0" "<P>\1")`.*
  - Each `open-session` / `close-session` / `replace-secret` writes 1 small key (~80–200 bytes) with 0 reads (`keypath` + `termval`/`NONE>`), and Rama pushes only the incremental diff (`KeyDiff`, `KeyRemoveDiff`, or atomic `MultiDiff` for multi-key ends/take-outs — verified on Rama 1.6.0).
  - Fully top-level RocksDB-indexed: no un-subindexed collection can ever exceed 100 entries even if an agent opens thousands of concurrent sessions.
  - Isolates `pstate.proxy.limit = 20000` and range-proxy diff dispatch from all non-live history/index writes in `$$root-data`, and keeps the small hot live working set out of `$$root-data`'s unbounded-history LSM tree.
- *Option B: Non-subindexed `{UUID -> {:sign-ins {G ...} :sessions {S ...}}}` watched at `(keypath P)`.*
  - Fails the Rama skill's subindexing rule: open sessions per root have no protocol-enforced cap, so a single map per root can exceed 100 elements, forcing full-map read-modify-write and full-map schema validation on every `open-session`.
- *Option C: Merge `$$live` into `$$root-data` under key prefix `"<P>\0l:"`.*
  - Every `local-transform>` on `$$root-data` (every write to `r`, `a:`, `ao:`, `g:`, `s:`, `exp:`, `ok:`) would run range-proxy subscriber matching on the same PState holding 20,000 range proxies, multiplying reactivity CPU cost by ~3–4× and churning `NONE>` tombstones in the same RocksDB LSM tree as permanent history.
- **Chosen: Option A** — only design that is simultaneously subindexed/O(1)-write, delivers 1-proxy-per-root incremental diffs, feeds 2-scan `end-actor` with zero per-item `g:`/`s:` seeks, and isolates hot proxied live state from unbounded history.

### 3. `$$accounts` — Account-to-person link (PII-bearing)

- **Partitioning**: `|hash AccountKey` (colocated with `*enters` depot `(hash-by :account)`).
- **Schema**: `{AccountKey -> IAccountState}` where:
  - `AccountKey [provider<Keyword> subject<String>]`
  - `AccountLinking [op-id<UUID> code-claim<ClaimKey|nil> email-claim<ClaimKey|nil> own-layer<UUID> making-uuid<UUID> making-session<UUID> says<IPersistentMap> at<Long> status<Keyword> chosen<ClaimKey|nil>]` (transient while first-time `enter` is in flight; `status` is `:probing`, `:chosen`, or `:failing`; cleared via `NONE>` on `:not-invited` after `EnterOpFailed` commits on `H(op)`, or promoted to `AccountLinked` on success).
  - `AccountLinked [person<UUID> own-layer<UUID> making-uuid<UUID> making-session<UUID> says<IPersistentMap> says-at<Long>]`
  - `:says` is explicitly bounded by `SPEC.md` line 101–103 to at most 32 `String -> String` entries (enforced at client validation before append; `< 50` entries, must not be subindexed). Updated on `AccountLinked` **only when `(> event.at says-at)`** (Sid's Call 2).
- **Readers & writers**: Read and written exclusively by `enter` (0 hops for Step 1 on `H(AccountKey)`).
- **Growth**: 1 entry per entered person (millions); refused enters leave 0 entries in `$$accounts`.
- **Why this design is obvious**: `enter` is the sole operation keyed by `[provider subject]`, requires a 1-seek point lookup, serializes claim choice vs refusal on `H(Acc)` (`AccountLinking.status`), and isolates `[provider subject]` + `:says` into one erasable PState.

### 4. `$$claims` — Invitations by claim (email claims PII-bearing; code claims PII-free)

- **Partitioning**: `|hash ClaimKey` (colocated with `*invites` depot `(hash-by :claim)`).
- **Schema**: `{ClaimKey -> IClaimState}` where:
  - `ClaimKey [by<Keyword> value<String>]` (`by` is `:email` or `:code`; `value` is lower-case email or 64-hex hash).
  - `ClaimWaiting [person<UUID> invited-by-kind<Keyword> invited-by-id<UUID|nil> invited-at<Long>]`
  - `ClaimUsed [person<UUID> invited-by-kind<Keyword> invited-by-id<UUID|nil> invited-at<Long> used-by-op<UUID>]` (holds only non-PII `used-by-op` UUID — never stores `AccountKey` or `:says`).
  - *(Note: `ClaimClaiming` is eliminated completely by the revised choose-at-account protocol in Revision 1 — `$$claims[C]` transitions strictly `nil -> ClaimWaiting(P) -> ClaimUsed(P, op)` with zero transient claim states!)*
- **Readers & writers**: Read and written by `invite` and `enter`.
- **Growth**: 1 entry per invited person (millions, including never-proven invitations).
- **Why this design is obvious**: Claims are looked up globally by `(by, value)` with 1 seek; storing `used-by-op` (UUID) instead of `AccountKey` keeps code claims completely free of PII, and strict two-state monotonicity (`ClaimWaiting -> ClaimUsed`) makes stuck claims impossible.

### 5. `$$asks` — Device-grant asks by 8-character code

- **Partitioning**: `|hash code` (colocated with `*asks` depot `(hash-by :code)`).
- **Schema**: `{String -> IAskState}` where `IAskState` is implemented by:
  - `AskWaiting [hash<String> asks<String> ends<Long> born-over?<Boolean>]`
  - `AskAnswering [hash<String> asks<String> ends<Long> born-over?<Boolean> op-id<UUID> yes?<Boolean> root<UUID> actor<UUID> new-agent-label<String|nil> sign-in<UUID> until<Long> at<Long>]` (transient during `answer-ask`; projected by `ask-of` as `:state :waiting :answer nil`).
  - `AskYes [hash<String> asks<String> ends<Long> root<UUID> actor<UUID> sign-in<UUID>]`
  - `AskNo [hash<String> asks<String> ends<Long>]`
- **Readers & writers**: Read by `ask-of` (1 `foreign-select-one` seek), `ask`, and `answer-ask`. Written by `ask` and `answer-ask`.
- **Growth**: 1 entry per bound code, never deleted ("A code is bound for good"), zero PII.
- **Why this design is obvious**: `ask-of` is a single-key point lookup by 8-char code `U`; storing `AskAnswering` inline on `$$asks[U]` gives single-task mutual exclusion on `U` with 0 extra seeks.

### 6. `$$enter-ops` — Refused `enter` replay guard (PII-free)

- **Partitioning**: `|hash op-id` (UUID).
- **Schema**: `{UUID -> IEnterOpState}` where `IEnterOpState` is implemented by:
  - `EnterOpFailed []` (written **only** when an `enter` attempt `op-id` is refused with `:not-invited`).
- **Readers & writers**: Written ONLY when an `enter` is refused (`:not-invited`), read by first-time `enter` before mutating a chosen claim (Step E2c) and by returning/helping `enter` with `(> at says-at)` before updating `:says` (Step E1a). Never touched by any read or by returning enters whose `:at <= says-at`.
- **Growth**: 1 tiny record (~24 bytes) per **refused** `enter` attempt only (0 records written for successful enters!); zero PII; a subsequent fresh client call generates a fresh `op-id`, so "a refusal binds nothing" holds strictly.

**Alternatives weighed for `$$enter-ops`:**
- *Option A (chosen): Separate PII-free `$$enter-ops` keyed by random `op-id` UUID recording `EnterOpFailed` on refusal only.*
  - Prevents Rama's `:all-after` post-commit replay of a refused `enter` depot record from claiming a later-created invitation or overwriting a later-linked account's `:says`, while leaving `$$accounts[Acc]` completely empty (`nil`) after refusal and storing zero PII.
- *Option B: Store refused `op-id` sets inside `$$accounts[Acc]`.*
  - Leaves an entry on `$$accounts[Acc]` for refused (never-invited) accounts, grows per refused attempt inside a non-subindexed field, and retains `AccountKey` PII on refused enters.
- **Chosen: Option A** — strictly upholds zero-entry refused `$$accounts`, zero PII in `$$enter-ops`, and exact `:all-after` replay safety.

### Summary of PStates grouped by `(key type, partitioner)`

| `(key type, partitioner)` group | PStates | Justification for any group of 2+ |
|---|---|---|
| `(String, \|hash P)` | `$$root-data`, `$$live` | Distinct key structure and access/proxy regime: `$$live` holds only 2 live prefixes (`g:`, `s:`) watched via `foreign-proxy` range scans (`pstate.proxy.limit = 20000`); `$$root-data` holds 7 non-watched history/expiry/outcome prefixes (`r`, `a:`, `ao:`, `g:`, `s:`, `exp:`, `ok:`). Merging them multiplies range-proxy subscriber dispatch 3–4× on every write and mixes unbounded history with high-churn live deletions in one RocksDB LSM tree. |
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
| `end-sign-in`, `close-session`, `end-actor` | Naturally idempotent (monotonic transition to ended/closed + `NONE>` on `$$live` / `exp:`) | Single-task atomic batch (0 hops; no yield inside `end-actor`; flat `ops/explode` writes). Retry after commit sees already ended/closed and returns `{:ended G}` / `{:closed S}` / `{:ended A}` with no mutation. |
| `open-session` | Naturally idempotent (monotonic `max` on `:until`, immutable `:driven-by`, closed never reopens) | Single-task atomic batch (0 hops). Retry after commit sees `s:<S>` with same `G`; if `t1 <= current-until` or closed, no mutation -> `{:open S}`. |
| `ask` | Naturally idempotent (`termval` on `$$asks[U]` if absent) | Single-task atomic batch (0 hops). Retry after commit sees `$$asks[U]` with same `H` -> `{:asked U}` with no mutation. |
| `invite` | Multi-partition (hops `H(C) <-> H(P)`) | **Made idempotent by monotonic state machine + wait-free helping + Step-1 cleanup + `H(C)` re-check**: `$$claims[C]` transitions monotonically `nil -> ClaimWaiting(P) -> ClaimUsed(P)` and `$$root-data["<P>\0r"]` transitions `nil -> RootReserving(C) -> RootInvited(C) -> RootEntered(C)` (Deep-dive §1A; proven in [`agreement/`](agreement/README.md)). |
| `enter` | Multi-partition (hops `H(Acc) <-> H(C) <-> H(op) <-> H(P)`) | **Made idempotent by choose-at-account state machine + `AccountLinking.status` + `$$enter-ops[op] = EnterOpFailed` replay guard + monotonic `:says`**: `$$claims[C]` never enters a transient state; `AccountLinking.status` (`:probing | :chosen | :failing`) serializes choice vs refusal on `H(Acc)`; `EnterOpFailed` on `H(op)` blocks `:all-after` replay of refused enters (Deep-dive §1B; proven in [`agreement/`](agreement/README.md)). |
| `answer-ask` | Multi-partition (hops `H(U) <-> H(P)`) | **Made idempotent by `AskAnswering(op-id)` + per-attempt `$$root-data["<P>\0ok:<op-id>"]` checked BEFORE take-out**: `H(P)` evaluates `op-id` once (and Step 1b records `AskOutcomeRefused(:no-such-ask)` on `H(P)`). Any retry or helper arriving at `H(P)` replays the recorded outcome without re-running `ans.at` take-out; helpers always run `self-input`'s take-out on `(:root self-input)` at `(:at self-input)` (Deep-dive §1C; proven in [`agreement/`](agreement/README.md)). |
| Take-outs (on `:root` writes) | Monotonic (`NONE>` on `exp:` and `$$live`; `ended?/closed? = true`) | Ending an already-ended item is a no-op. Note on pathological clock skew under uncheckpointed stream replay on `*root-events`: analyzed explicitly in `Walls` (item 3). |

### PStates owned by `"auth"` (full typed schemas, zero `Object`)

- `$$root-data`: `{String -> IRootDatum}` (`RootReserving`, `RootInvited`, `RootEntered`, `ActorRec`, `ActorOrderRec`, `SignInRec`, `SessionRec`, `ExpiryEntry`, `AskOutcomeYes`, `AskOutcomeNo`, `AskOutcomeRefused`)
- `$$live`: `{String -> ILiveEntry}` (`LiveSignIn`, `LiveSession`); launch option `pstate.proxy.limit = 20000`
- `$$accounts`: `{AccountKey -> IAccountState}` (`AccountLinking`, `AccountLinked`)
- `$$claims`: `{ClaimKey -> IClaimState}` (`ClaimWaiting`, `ClaimUsed`)
- `$$asks`: `{String -> IAskState}` (`AskWaiting`, `AskAnswering`, `AskYes`, `AskNo`)
- `$$enter-ops`: `{UUID -> IEnterOpState}` (`EnterOpFailed`)

## Query Topologies

`Auth` declares **three** query topologies (`"who-is"`, `"under"`, `"ping-task"`). All three begin with a built-in single-task leading partitioner using only topology input arguments so Rama evaluates routing client-side and sends a single network round trip straight to the target task (`query-topologies.md:120`). `ask-of` uses direct `foreign-select-one` (1 key, 1 PState) rather than a query topology.

### 1. `"who-is"` `[*root *sign-in *session :> *result]`

- **Leading partitioner**: `(|hash *root)` — 1 round trip directly to `H(*root)`, ends with `(|origin)`.
- **Pre-agg only (no aggregator)**: produces exactly one result map per invocation; no yielding (strictly bounded 1–4 point seeks).
- **Dynamic conditional reads (`<<if`, Revision 3 — 2-seek fast path)**:
  1. Always read `$$root-data["<P>\0g:<G>"]` **first** (1 seek).
  2. **Fast path** (>99.99% of requests — `*sig` is non-nil AND `(:own-layer-made-at-begin? *sig)` is `true`): `P` is guaranteed `RootEntered` with `:own-layer = (:own-layer *sig)` and `:own-layer-made? = true` — **skip reading `"<P>\0r"` completely (`0` seeks on `r`)!**
  3. **Fallback** (only when `*sig` is `nil` OR `(:own-layer-made-at-begin? *sig)` is `false`): read `$$root-data["<P>\0r"]` (1 seek). If absent or not `RootEntered`, short-circuit and emit `{:sign-in nil :session nil :own-layer nil :own-layer-made? false}`.
  4. When entered and `*session` is non-nil: read `$$root-data["<P>\0s:<S>"]` (1 seek). When `*session` is nil, bind `nil` with 0 reads.
  5. Only when `*sig` is non-nil, `(:ended? *sig)` is `true`, AND `(:actor *sig) != *root`: read `$$root-data["<P>\0a:<A>"]` (1 seek) to obtain `:actor-ended?`.
- **Concrete input examples (`N` total reads vs `M` meaningful reads):**
  - *Example 1* — Entered root (`made?=true`), live sign-in `G`, `S = nil`: reads `g:<G>` only -> **N = 1, M = 1** (`N == M`).
  - *Example 2* — Entered root (`made?=true`), live sign-in `G`, `S = S1` (dominant HTTP request): reads `g:<G>` + `s:<S1>` -> **N = 2, M = 2** (`N == M`).
  - *Example 3* — Entered root (`made?=true`), ended agent sign-in `G` (`A != P`), `S = nil`: reads `g:<G>` + `a:<A>` -> **N = 2, M = 2** (`N == M`).
  - *Example 4* — Entered root (`made?=true`), ended agent sign-in `G` (`A != P`), `S = S1`: reads `g:<G>` + `s:<S1>` + `a:<A>` -> **N = 3, M = 3** (`N == M`).
  - *Example 5* — Unknown or waiting root `P` (any `G`, `S = S1`): reads `g:<G>` (miss) + `r` (not entered -> short-circuit) -> **N = 2, M = 2** (`N == M`).
  - *Example 6* — Entered root, `G` begun before `own-layer-made` (`:own-layer-made-at-begin? false`), `S = S1`: reads `g:<G>` + `r` + `s:<S1>` (+ `a:<A>` if ended agent) -> **N = 3–4, M = 3–4** (`N == M`).
- **Fixed or variable**: **Variable** (`M ∈ {1, 2, 3, 4}`).
- **Dynamic approach**: Sequential `<<if` guards on fast-path `(:own-layer-made-at-begin? *sig)`, `RootEntered`, `(some? *session)`, and `(and> (some? *sig) (:ended? *sig) (not= (:actor *sig) *root))`.

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
- **Purpose**: Lightweight in-memory liveness probe used by `softland.auth.client`'s background watch monitor (every 2 s, 5 s timeout + 3 s fallback probe = 8 s outage ceiling, only when >= 1 watch is open) to detect task/worker/module loss within seconds for `:softland.auth.client/not-reached` without false triggers during worst-case 2-scan `end-actor` cascades.
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

Therefore the optimal placement `f` maps **every key belonging to root `P`** (`r`, `a:<A>`, `ao:<seq>`, `g:<G>`, `s:<S>`, `exp:...`, `ok:<op>`, and `$$live` entries for `P`) to **exactly one task `f(P) = {t_P}`**, distributed uniformly across all `N` tasks:
- **Why not `|all` (`f(P) = {0..N-1}`)?** At millions of persons and high `open-session` / `replace-secret` write rates, replicating every root write to all `N` tasks multiplies write CPU and disk usage by `N` (anti-scalable) and cannot satisfy atomic per-root take-outs or 20,000-per-task `watch-root` bounds.
- **Why not splitting `P`'s sign-ins/sessions across multiple tasks via `|direct` or stored placement?** Splitting `g:<G>` and `s:<S>` onto different tasks than `r` forces `who-is` into 2–3 cross-task hops (violating "one task"), breaks single-task atomicity for take-outs and `end-actor` cascades, and adds a placement-table lookup seek (+1 seek on every `who-is` and write, raising weighted seeks from 1.93 to 2.93+ with zero latency benefit).
- **Why `|hash` implements `f` cleanly for all five keyspaces (`P`, `Acc`, `C`, `U`, `op-id`)**:
  - **Keyspace cardinality**: Millions of random UUIDs (`P`, `op-id`), millions of accounts (`Acc`) and claims (`C`), unbounded 8-char codes (`U`). At `N = 128`, `M / N` is `>= 10^4`, so balls-into-bins hash variance `√(2·ln N / (M/N))` is `< 3%` — negligible.
  - **Per-key hot-spot check**: Under `SPEC.md`, one root has tens of live sign-ins and tens–hundreds of open sessions (~20 KB total hot state). Even if a single power-user root receives thousands of `who-is`/sec from a fleet of agents, its active `g:` and `s:` keys stay resident in RocksDB's 256 MB block cache on `t_P`, and `who-is` touches `0` history rows and `0` iterator reads. No global singleton key exists anywhere in the module.

### Validation tables for dominant read `who-is` (`N = 1`, `N = 16`, `N = 128` tasks; Revision 3 + Revision 8)

Every invocation routes directly to `1` task via leading `(|hash *root)` (total tasks touched across the cluster = `1` in all rows and at all `N`). Under Revision 3's 2-seek fast path (`g:<G>` read first, skipping `r` when `:own-layer-made-at-begin?` is `true`) and Revision 8's complete category set (including ended-agent `S = nil`), both **Total tasks touched/op** and **Total RocksDB seeks/op** are tabulated below.

#### N = 1 task (single-task baseline)

| Data category | Frequency proportion | Tasks touched/op | Total RocksDB seeks/op | Iterator reads/op |
|---|---|---|---|---|
| Entered root (`made?=true`), live sign-in (human or agent), `S != nil` | 0.850 | 1 | 2 | 0 |
| Entered root (`made?=true`), live sign-in (human or agent), `S = nil` | 0.100 | 1 | 1 | 0 |
| Entered root (`made?=true`), ended agent sign-in (`A != P`), `S != nil` | 0.025 | 1 | 3 | 0 |
| Entered root (`made?=true`), ended agent sign-in (`A != P`), `S = nil` | 0.005 | 1 | 2 | 0 |
| Unknown or waiting root `P` (`g:<G>` miss -> `r` miss) | 0.020 | 1 | 2 | 0 |

**Weighted tasks touched = 1.00  |  Weighted RocksDB seeks = 1.925 (~1.93)  |  Weighted iterator reads = 0.00**
*(Calculation: `0.850×2 + 0.100×1 + 0.025×3 + 0.005×2 + 0.020×2 = 1.700 + 0.100 + 0.075 + 0.010 + 0.040 = 1.925`, down `-33.4%` from the Phase-1 3-seek design's `2.89`.)*

#### N = 16 tasks

| Data category | Frequency proportion | Tasks touched/op | Total RocksDB seeks/op | Iterator reads/op |
|---|---|---|---|---|
| Entered root (`made?=true`), live sign-in (human or agent), `S != nil` | 0.850 | 1 | 2 | 0 |
| Entered root (`made?=true`), live sign-in (human or agent), `S = nil` | 0.100 | 1 | 1 | 0 |
| Entered root (`made?=true`), ended agent sign-in (`A != P`), `S != nil` | 0.025 | 1 | 3 | 0 |
| Entered root (`made?=true`), ended agent sign-in (`A != P`), `S = nil` | 0.005 | 1 | 2 | 0 |
| Unknown or waiting root `P` (`g:<G>` miss -> `r` miss) | 0.020 | 1 | 2 | 0 |

**Weighted tasks touched = 1.00  |  Weighted RocksDB seeks = 1.925 (~1.93)  |  Weighted iterator reads = 0.00**

#### N = 128 tasks

| Data category | Frequency proportion | Tasks touched/op | Total RocksDB seeks/op | Iterator reads/op |
|---|---|---|---|---|
| Entered root (`made?=true`), live sign-in (human or agent), `S != nil` | 0.850 | 1 | 2 | 0 |
| Entered root (`made?=true`), live sign-in (human or agent), `S = nil` | 0.100 | 1 | 1 | 0 |
| Entered root (`made?=true`), ended agent sign-in (`A != P`), `S != nil` | 0.025 | 1 | 3 | 0 |
| Entered root (`made?=true`), ended agent sign-in (`A != P`), `S = nil` | 0.005 | 1 | 2 | 0 |
| Unknown or waiting root `P` (`g:<G>` miss -> `r` miss) | 0.020 | 1 | 2 | 0 |

**Weighted tasks touched = 1.00  |  Weighted RocksDB seeks = 1.925 (~1.93)  |  Weighted iterator reads = 0.00**

**Verdict**: Both weighted tasks touched (`1.00`) and weighted RocksDB seeks (`1.925`) remain completely flat from `N = 1` to `N = 128`. Adding hardware scales aggregate throughput linearly with zero per-request overhead growth.

### Write partitioner justification summary

| Depot / PState write | Partitioner | Justification |
|---|---|---|
| `*root-events` -> `$$root-data`, `$$live` | `(hash-by :root-id)` / `\|hash P` | Millions of UUID roots; 0 hops for all 8 root-scoped writes; single-task atomicity for take-outs and 32-deep 2-scan cascades. |
| `*asks` -> `$$asks` (and `\|hash P` for `answer-ask`) | `(hash-by :code)` / `\|hash U` | Unbounded 8-char random codes; 0 hops for `ask` and `ask-of`; 1–2 hops for low-volume `answer-ask`. |
| `*invites` -> `$$claims` (and `\|hash P`) | `(hash-by :claim)` / `\|hash C` | Millions of email/code claims; 0 hops to detect repeat or `:claim-in-use` on `H(C)` before touching `P`. |
| `*enters` -> `$$accounts` (and `\|hash C`, `\|hash op`, `\|hash P`) | `(hash-by :account)` / `\|hash Acc` | Millions of accounts; 0 hops to detect returning account and serialize claim choice vs refusal on `H(Acc)`. |
| `$$enter-ops` | `\|hash op-id` | Random UUID per `enter` attempt; records `EnterOpFailed` on refusal only so `:all-after` replay never flips a refused enter or overwrites `:says`. |

## Design Decisions

### 1. Top-level RocksDB indexing (zero un-subindexed unbounded collections)
Every collection that can grow per root or globally — actors (`a:`, `ao:`), sign-ins (`g:`), sessions (`s:`), expiries (`exp:`), ask outcomes (`ok:`), live sign-ins and open sessions (`$$live`), accounts (`$$accounts`), claims (`$$claims`), asks (`$$asks`), and refused-enter guards (`$$enter-ops`) — is stored as individually indexed entries in top-level RocksDB-backed PState maps. There are **no** nested subindexed maps (avoiding the extra parent seek per lookup) and **only one** non-subindexed inner map anywhere in the schema: `:says` inside `AccountLinking`/`AccountLinked`, whose size is explicitly enforced to **at most 32 entries** by client input validation before append (`SPEC.md` line 101–103; `< 50` entries, so subindexing would only add overhead).

### 2. Colocation between depots and PStates
- `*root-events` (`(hash-by :root-id)`) colocates with `$$root-data` and `$$live` (`|hash P`): all 8 single-root writes (`own-layer-made`, `begin-sign-in`, `replace-secret`, `end-sign-in`, `make-actor`, `end-actor`, `open-session`, `close-session`) execute with **0 partitioner hops** and complete in a single atomic streaming batch on `H(P)`.
- `*asks` (`(hash-by :code)`) colocates with `$$asks` (`|hash U`): `ask` executes with **0 hops**; `answer-ask` checks `U` on `H(U)` with **0 initial hops** before hopping to `H(P)`.
- `*invites` (`(hash-by :claim)`) colocates with `$$claims` (`|hash C`): `invite` checks repeat and `:claim-in-use` on `H(C)` with **0 initial hops**, matching `SPEC.md`'s refusal order before hopping to `H(P)`.
- `*enters` (`(hash-by :account)`) colocates with `$$accounts` (`|hash Acc`): returning users resolve their link on `H(Acc)` with **0 initial hops**.

### 3. Input validation at the client edge (`:read-the-event`, Revision 6 + Sid's Call 1)
`softland.auth.client` validates every argument of every write and read **before** appending to any depot or invoking Rama (`SPEC.md` line 103–105, 121–141: invalid write writes nothing, not even an event), wrapped in `try/catch Throwable` so sorted maps with throwing comparators return `{:could-not :read-the-event}`:
- **Client/cluster argument checks**: `(open non-cluster _)` or `(open cluster non-string)` returns `{:could-not :find-the-module ...}`. `(close! non-client)` returns `nil`. Any write, read, or `watch-root` whose first argument is not an Auth client returns `{:could-not :read-the-event}`.
- **Map shape (strict key presence & no extra keys)**: every write/read input map must be a Clojure map containing **every** named key via `(contains? m k)` — including every `-or-nil` key (`:email` and `:code-hash` on `enter`, `:label` on `begin-sign-in`, `:new-agent` on `answer-ask`, `:session` on `who-is`) as `nil` when holding nothing. `:driven-by` on `open-session` **alone** may be omitted (`SPEC.md:121–124`). Unknown keys on top-level call maps and sub-maps (`:claim`, `:making`, `:new-agent`, `:driven-by`) are rejected with `{:could-not :read-the-event}`.
- **Times** (`:at`, `:until`, `:previous-until`, `:ends`): JVM fixed-width integer (`(int? x)`: `Byte`, `Short`, `Integer`, `Long`; rejects `BigInt`, `BigInteger`, floats, nil) coerced to exact `Long` before depot append (preventing Rama's exact-class schema mismatch between `Integer` and `Long`, `pstate-schema.md:191`).
- **IDs** (`:person`, `:root`, `:actor`, `:made-by`, `:sign-in`, `:session`, `:own-layer`, `:making` `:uuid` and `:session`): `java.util.UUID`.
- **Hashes** (`:hash`, `:old-hash`, `:new-hash`, `:code-hash` when non-nil, code-claim `:hash`): `String` of length exactly 64 matching `[0-9a-f]{64}` (lower-case hex only).
- **Codes** (`:code`, `ask-of` `U`): `String` of length exactly 8 (`char` count).
- **Texts** (`:label`, `:asks`, `:email`, provider `subject`, `:driven-by` `:model`): `String` with `(.length s) <= 256` (JVM `char`s). Note: `:label` on `make-actor` and `:new-agent` is non-nil; `:label` on `begin-sign-in` is `text-or-nil`.
- **Emails** (`:claim {:by :email :email s}` on `invite`, and non-nil `:email` on `enter` — including for already-linked accounts): `String` of `<= 256` chars AND lower-case (`(= s (.toLowerCase ^String s java.util.Locale/ROOT))`, `SPEC.md:130–132`, `reference.clj:132`).
- **Provider** (first element of `:account [provider subject]`): `clojure.lang.Keyword` whose full text `(subs (str provider) 1)` (including namespace + `/` if qualified) has `<= 256` chars (`SPEC.md:360`).
- **`:says`**: map with `<= 32` entries, every key a `String` of `<= 256` chars and every value a `String` of `<= 2,048` chars.
- **`:claim`**: `{:by :email :email lower-text}` or `{:by :code :hash 64-lower-hex}` (exact keys, no other `:by` values, no nil value).
- **`:invited-by`**: `java.util.UUID` or `:system`.
- **`:kind`**: `:browser` or `:program`.
- **`:driven-by`** (sole optional key, on `open-session`): absent/`nil`, `{:kind :person}` (no extra keys), or `{:kind :model :model text}` (with `:model` a `String` of `<= 256` chars and no extra keys).
- **`:yes?` and `answer-ask` (Sid's Call 1)**: `:yes?` is `Boolean`. Even when `:yes? false` (a denial), **all 8 keys** must be present and fit their stated forms: `:code` 8-char text, `:yes?` bool, `:root` UUID, `:actor` UUID, `:new-agent` `nil` or `{:label text}`, `:sign-in` UUID, `:until` fixed-width int, `:at` fixed-width int.
- **`watch-root` callback `f`**: validated with `(fn? f)` (NOT `ifn?`, rejecting keywords, maps, sets, symbols, and vars).

### 4. Error handling, `open`, `close!`, and callback discipline
- **No call throws**: every public function in `softland.auth.client` wraps Rama interaction in `try/catch Throwable`, mapping missing module / invalid open args to `{:could-not :find-the-module :module ... :error ...}`, closed client to `{:could-not :closed}`, invalid input to `{:could-not :read-the-event ...}`, and any Rama/network/timeout failure to `{:could-not :reach-the-module :error ...}`. Every blocking Rama call uses a bounded timeout (`CompletableFuture/get`, 10 s default).
- **Client-side watch deduplication & incremental projection**: `softland.auth.client` keeps one shared `foreign-proxy-async` per watched root `P`, maintains the projected `{:sign-ins {...} :sessions {...}}` map incrementally from `KeyDiff` / `KeyRemoveDiff` / `MultiDiff` (rebuilding from the full proxy map only on `ResyncDiff` / `NewValueDiff`) so callback work on Rama's 2-thread proxy pool is O(changed entries) rather than O(|live|).
- **Callback isolation & `close!` safety**: exceptions thrown by a caller's `f` are caught per-watcher so they stop neither later calls to that watcher nor other watchers of `P` (`IMPLICIT_SPEC.md:812`). A `ThreadLocal` marks Rama's callback thread while invoking `f`, preventing deadlocks if `f` invokes its own stop function or `close!`.

### 5. Cooperative multitasking / yielding policy
- **Writes on `H(P)` (including 2-scan `end-actor` cascade)**: deliberately **never yield** (`no yield-if-overtime`, `no :allow-yield? true`). Per `dataflow.md:156` and `199`, yielding breaks same-key ordering and read-to-write atomicity. With Revision 2's 3-seek 2-scan cascade (`ao:` + `$$live`) and flat `ops/explode` writes (zero recursive `loop<-`), `end-actor` at 3,000 actors / 2,000 sign-ins / 5,000 sessions completes well under 2 s (far below the 8 s `ping-task` outage ceiling).
- **`"who-is"` query topology**: never yields (1–4 point seeks, 0 scans).
- **`"under"` query topology**: uses `{:allow-yield? true}` on the `ao:` range scan (up to thousands of agents). Because `"under"` is read-only and Rama's yielded select executes over a consistent PState snapshot (`dataflow.md:195`), yielding keeps `"who-is"` latency flat even during large `"under"` reads.

## State primitive selection

Every piece of durable Auth state lives in partitioned Rama PStates; the Rama module uses **zero `TaskGlobal`s** and **zero external systems**.

- **`$$root-data` (PState, durable)**: Primary source of truth for per-root person state, actors, sign-ins, sessions, ask outcomes, and expiry index. Per-source-event write volume: `O(1)` for `own-layer-made`, `make-actor`, `begin-sign-in`, `replace-secret`, `open-session`, `close-session`, `invite`, `enter`; `O(expired)` for take-outs; `O(open sessions of G)` for `end-sign-in`; `O(live subtree of A + live sign-ins + open sessions)` for 2-scan `end-actor` (3 seeks total, never scanning `g:`/`s:` history).
- **`$$live` (PState, durable)**: Materialized live-only view of each root's live sign-ins and open sessions for `watch-root` (`foreign-proxy-async`), `under`'s live sign-ins, `end-sign-in`, and 2-scan `end-actor`. Per-source-event write volume: `O(1)` per live sign-in/session mutation, `O(expired)` on take-out, `O(live items ended)` on `end-sign-in`/`end-actor`. Durable so watches immediately see exact live state across worker restarts and leader failovers without rebuilding from depots.
- **`$$accounts` (PState, durable)**: Primary source of truth for `[provider subject] -> person` links, fixed `:own-layer`/`:making`, and latest `:says` (updated only when `at > says-at`). Refused enters leave 0 entries.
- **`$$claims` (PState, durable)**: Primary source of truth for claim (`:email` or `:code`) -> waiting/used person binding (`nil -> ClaimWaiting -> ClaimUsed` only).
- **`$$asks` (PState, durable)**: Primary source of truth for device-grant asks by 8-char code.
- **`$$enter-ops` (PState, durable)**: Durable PII-free refusal record (`EnterOpFailed` only) guarding `:all-after` stream replay of refused `enter` depot records. 0 writes on successful enters.
- **Module `TaskGlobal`s: None.** No in-memory cache is placed inside the Rama module. Per `AGENTS.md` ("Systemic repair and caching"), `who-is` already achieves `1–2` direct RocksDB seeks (~0.5–1.0 ms) with active keys naturally served by RocksDB's block cache.
- **Client-side ephemeral subscription registry (`softland.auth.client`, non-durable in-JVM state)**: Holds only active `watch-root` listener maps, one shared Rama `ProxyState` handle per `(client, root P)` plus its incrementally maintained projected map, a closed flag, and the scheduled `ping-task` liveness handle. Holds zero Auth truth (`SPEC.md` line 21: "each [runtime copy] holds no state of its own").

## Resource usage analysis

Sized at a representative full-life deployment of **1,000,000 persons** across **N = 64 tasks** (~15,625 persons/task), assuming an average of 5 lifetime agents/person (with power users up to thousands), 50 lifetime sign-ins/person (10 live), 500 lifetime sessions/person (20 open; busy agent roots up to millions of closed sessions), and 20 asks/person.

### Disk usage (PStates and depots)

| Store | Entry key + value size (serialized + ~24 B RocksDB overhead) | Growth rate | Est. entries/task (1M persons, N=64) | Est. disk/task |
|---|---|---|---|---|
| `$$root-data` (`r`) | 38 B key + ~180 B `RootEntered` = ~240 B | 1/person (bounded by persons) | 15,625 | ~3.8 MB |
| `$$root-data` (`a:`, `ao:`) | `a:`: ~160 B; `ao:`: ~160 B (~320 B/agent total; `ch:` deleted) | 1/agent made (up to thousands/person) | ~78,125 agents | ~25 MB |
| `$$root-data` (`g:`) | 75 B key + ~320 B `SignInRec` (with `initial-hash`, `own-layer`, `own-layer-made-at-begin?`) = ~420 B | Unbounded over time (ended kept) | ~781,250 | ~328 MB |
| `$$root-data` (`s:`) | 75 B key + ~105 B `SessionRec` (with `driven-by`) = ~205 B | Unbounded over time (highest history growth; busy agents: thousands/day) | ~7.8M (500/person) to ~156M (10k/person) | ~1.6 GB (to ~32 GB heavy agent fleet) |
| `$$root-data` (`ok:`) | 76 B key + ~75 B `AskOutcome*` = ~175 B | 1/answered ask attempt | ~312,500 | ~55 MB |
| `$$root-data` (`exp:`) | ~110 B each (live items only; `NONE>` removed on end/close; `ag:`, `sg:` deleted) | Bounded by live sign-ins + open sessions | ~470k live expiry keys | ~52 MB |
| `$$live` (`g:`, `s:`) | `LiveSignIn`: ~345 B; `LiveSession`: ~160 B | Strictly bounded by live sign-ins (tens/root) + open sessions (tens–hundreds/root) | ~156k sign-ins + ~312k sessions | ~104 MB (stays hot in 256 MB RocksDB block cache) |
| `$$accounts` | ~60 B `AccountKey` + ~600 B `AccountLinked` (typical `:says`; max ~75 KB if `:says` is 32 × 2 KB) = ~685 B avg | 1/entered person (refused enters write 0) | 15,625 | ~11 MB |
| `$$claims` | ~50 B `ClaimKey` + ~75 B `Claim*` = ~150 B | 1/invited person | 15,625 | ~2.3 MB |
| `$$asks` | 8 B key + ~200 B `Ask*` = ~235 B | Unbounded over time | ~312,500 | ~73 MB |
| `$$enter-ops` | 16 B UUID key + ~16 B `EnterOpFailed` = ~56 B | Refused `enter` attempts only (0 on accepted enters) | Low (refused attempts) | < 1 MB |
| Depots (`*root-events`, `*asks`, `*invites`, `*enters`) | ~120–300 B/event (`*enters` ~650 B avg with `:says`) | Append-only until future thinning/migration (`*root-events` dominates via `open-session` + `replace-secret`) | ~20M–200M events/task over years | ~3–30 GB/task (independent per-depot trim possible later) |

### Memory usage (TaskGlobals and runtime/server proxy footprint)

- **Module `TaskGlobal`s**: **0 bytes** — no `TaskGlobal` is declared in `Auth`. Zero custom heap or GC pause pressure added to Rama task threads.
- **Server-side `$$live` `ProxyState` tracking (at 20,000 watched roots/task)**: Each active range proxy holds a small subscription entry and range predicate on the `$$live` task partition (~1–2 KB/subscription -> ~20–40 MB/task at 20,000 active watches). Rama's `worker.child.opts` default `-Xmx4096m` comfortably accommodates this alongside RocksDB's 256 MB off-heap block cache.
- **Client-side watch registry (`softland.auth.client`, per runtime JVM copy)**: One shared Rama `ProxyState` + projected Clojure map per locally watched root `P` (not per watcher callback). For a root with 10 live sign-ins (~2.5 KB) and 100 open sessions (~8 KB), total client memory is ~15–20 KB per watched root (~200 MB for 10,000 active watched roots on one runtime copy). All client structures are flat maps of small immutable records/maps, updated incrementally on `KeyDiff`/`KeyRemoveDiff` to minimize allocation and GC pressure on Rama's 2-thread callback pool.

### Minimization

- **No redundant caches**: Zero module-level caches; `who-is` reads directly from `$$root-data` in 1–2 point seeks (~0.5–1.0 ms).
- **Primitive / flat representations**: Timestamps (`Long`), sequence numbers (`Long`), booleans (`Boolean`), and `IDrivenBy` / `IRootDatum` / `ILiveEntry` records use flat fields rather than nested maps of boxed metadata. `:says` is stored only in `$$accounts` (and `*enters`), never copied to `$$root-data` or `$$live`.
- **Every data duplication is quantified and justified**:
  1. `$$live` duplicates live fields of `g:` and `s:` from `$$root-data` (including `:initial-hash`, `:own-layer`, `:own-layer-made-at-begin?`, `:driven-by`): required to separate hot watched state (`pstate.proxy.limit = 20000`, `watch-root` range scan, `under` live sign-ins) from millions of cold history entries (`SPEC.md` line 400–401) AND to let 2-scan `end-actor`, `end-sign-in`, and take-outs close/end live items with 0 point seeks on `g:`/`s:` and 0 `ch:`/`ag:`/`sg:` indexes.
  2. `SignInRec` (`g:<G>`) duplicates immutable `:own-layer` (16 B) and `:own-layer-made-at-begin?` (1 B) from `"<P>\0r"`: cuts dominant `who-is` seeks from 3 to 2 (`-33.4%` cluster disk I/O) at 0 extra write seeks.
  3. `ao:` duplicates `{actor, made-by, depth, label, ended?, seq}` from `a:`: powers both `under(P)` (1 range scan vs 1,000 point seeks, 90× faster) and Revision 2's 1-scan actor subtree cascade in `end-actor` (deleting `ch:` completely).
  4. `exp:` is a lightweight sorted expiry index (~110 B each, removed on end/close): reduces take-out from O(all live items) to O(expired items) (`0.5 ms` when nothing expired).
  5. `$$root-data` (`"<P>\0r"`) stores `:linked-provider`/`:linked-subject` and `:claim-kind`/`:claim-val` alongside `$$accounts` and `$$claims`: costs ~80 B/person and enables O(1) future PII erasure by person ID `P` without full-table scans of `$$accounts` or `$$claims`.

## Deep-dive settlements and traces

### 1. Agreement across keys

All three multi-key writes (`invite`, `enter`, `answer-ask`) execute inside the single stream topology `"auth"` using **monotonic state machines with wait-free helping**: no global coordinator task, no spin-wait/polling loops, no stuck reservations after a worker crash, and zero leaked mutations from losing or refused attempts. Every task is single-threaded, so each local read-check-write segment between partitioner hops is atomic; every cross-task hop flushes and replicates the prior task's PState writes before the child event executes on the destination task (`stream.md:41`), and `:ack` with `ack-return>` returns to the caller only after the entire event tree has completed and replicated on every task it touched (`14-depots.md:206`).

#### 1A. `invite` (Claim `C` on task `H(C)`, Person `P` on task `H(P)`)

**Protocol and safety invariant (Revision 1 — proven in [`agreement/`](agreement/README.md)):**
- `$$claims[C]` transitions monotonically: `nil -> ClaimWaiting(P) -> ClaimUsed(P)`. Once bound to `P`, `C` never unbinds and never changes `:person`.
- `$$root-data["<P>\0r"]` transitions: `nil -> RootReserving(C) -> RootInvited(C) -> RootEntered(C)`, OR `RootReserving(C) -> nil / RootReserving(C')` **only after observing `$$claims[C].person != P`**. (If an `enter` beats Step 4, `RootReserving(C) -> RootEntered(C)` directly.)
- **Commit rule**: An event writes `ClaimWaiting(P)` into `$$claims[C]` only after observing `RootReserving(C)` on `H(P)` and currently observing `$$claims[C] == nil`.
- **Single-claim safety proof**: `P` can never commit two claims `C1 != C2`. To leave `RootReserving(C1)` for `RootReserving(C2)`, an event must first observe `$$claims[C1].person != P`; by monotonicity of `$$claims[C1]`, `C1` can never have been or later become `ClaimWaiting(P)`.
- **Normal path (3 hops)**:
  - **Step 1 (`H(C)`, 0 hops from `*invites`)**: Read `$$claims[C]`.
    - If present with `:person == P`: hop `|hash P` to Step 4 (ensuring `RootInvited`/`RootEntered`, returning `{:invited P}`).
    - If present with `:person != P`: hop `|hash P` (`step-3-clear-lost-root`) to clear any orphaned `RootReserving(C)` (if `r` is still `RootReserving(C)`) before emitting `(ack-return> {:refused :claim-in-use})` (Revision 1, Witness 6 — closes orphaned `RootReserving` and email PII retention on crashed retry).
    - If `nil`: hop `|hash P` to Step 2.
  - **Step 2 (`H(P)`)**: Read `$$root-data["<P>\0r"]`:
    - `RootInvited` / `RootEntered`: if same `C`, `(ack-return> {:invited P})`; else hop `|hash C` (`step-2-recheck-claim`) before returning `:person-in-use` (Revision 1, Witness 9 — enforces `:claim-in-use` ahead of `:person-in-use` across concurrent causal chains).
    - `nil`: write `RootReserving(C, B, t)`, hop `|hash C` to Step 3.
    - `RootReserving(C0, B0, t0)`:
      - If `C0 == C` (retry or concurrent duplicate of same `(P, C)`): hop `|hash C` to Step 3.
      - If `C0 != C` (competing invite in flight on `P`): **help `C0`** — hop `|hash C0`, read `$$claims[C0]`:
        - If `nil`: write `ClaimWaiting(P, B0, t0)` into `$$claims[C0]`, hop `|hash P`, promote `RootReserving(C0)` to `RootInvited(C0)`, then hop `|hash C` (`step-2-recheck-claim`) to verify `:claim-in-use` vs `:person-in-use`.
        - If `:person == P`: hop `|hash P`, promote `RootReserving(C0)` to `RootInvited(C0)`, then hop `|hash C` (`step-2-recheck-claim`).
        - If `:person != P` (`C0` lost to another person): hop `|hash P`; if still `RootReserving(C0)`, overwrite with `RootReserving(C, B, t)` and proceed to Step 3 (`|hash C`); else loop on Step 2 with the updated `r`.
  - **Step 2-recheck (`H(C)`, reached only when `P` already holds another claim)**: Read `$$claims[C]`:
    - If `nil`: `(ack-return> {:refused :person-in-use})`.
    - If `:person == P`: hop `|hash P` to Step 4.
    - If `:person != P`: `(ack-return> {:refused :claim-in-use})`.
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

**Trace I-2 — Causal refusal-order chain (`invite(P2, C1)` then `invite(P1, C2)` while `invite(P1, C1)` is in flight — Witness 9):**
1. `c1 = invite(P1, C1)` sees `$$claims[C1] == nil` in Step 1 and hops to `H(P1)`.
2. Before `c1` runs on `H(P1)`, `c2 = invite(P2, C1)` commits `{:invited P2}` and then `c3 = invite(P1, C2)` commits `{:invited P1}`.
3. `c1` arrives at `H(P1)`, sees `RootInvited(C2)`, and hops `|hash C1` (`step-2-recheck-claim`).
4. `H(C1)` sees `ClaimWaiting(P2)` (`P2 != P1`), so `c1` returns `{:refused :claim-in-use}` — matching every valid causal linearization under `SPEC.md:104–105, 153–154`.

**Trace I-3 — Failure & retry at every hop of `invite(P, C)`:**
- *Crash after Step 2 (`RootReserving(C)` committed on `H(P)`), before Step 3*: Rama retries from Step 1 on `H(C)`. If `C` is still `nil`, Step 2 sees `RootReserving(C)` (`C0 == C`), proceeds to Step 3, commits `ClaimWaiting(P)`, confirms `RootInvited(C)` -> `{:invited P}`. If `P2` won `C` while crashed, Step 1 sees `ClaimWaiting(P2)` (`P2 != P`), hops `|hash P` to `NONE>` `RootReserving(C)` (Witness 6 closed — zero orphaned `RootReserving` or email PII!), and returns `{:refused :claim-in-use}`.
- *Crash after Step 3 (`ClaimWaiting(P)` committed on `H(C)`), before Step 4*: Rama retries from Step 1 on `H(C)`, sees `ClaimWaiting(P)` (`:person == P`), hops directly to Step 4 on `H(P)`, promotes to `RootInvited(C)` (or no-ops if an `enter` already set `RootEntered(C)`), returns `{:invited P}`.
- *Crash after Step 4 before `:ack` reaches Copy 1*: Copy 1 gets `{:could-not :reach-the-module}` and retries `invite(P, C)`. Step 1 sees `ClaimWaiting(P)` / `ClaimUsed(P)`, Step 4 sees `RootInvited(C)` / `RootEntered(C)`, returns `{:invited P}`.
- **Read-after-write visibility**: `(ack-return> {:invited P})` executes only in Step 4 after both `$$claims[C]` and `$$root-data["<P>\0r"]` are committed and replicated.

#### 1B. `enter` (Account `Acc` on `H(Acc)`, Claims `C_code`/`C_email` on `H(C)`, Replay Guard `op` on `H(op)`, Person `P` on `H(P)`)

**Protocol and safety invariants (Revisions 1 & 5 — uniform choose-at-account, zero `ClaimClaiming`, proven in [`agreement/`](agreement/README.md)):**
Each `enter` call generates a random UUID `op-id` in the client and appends `EnterEvent` to `*enters` (`hash-by :account`). Both 1-claim and 2-claim first-time enters execute the **same** uniform state machine:
- `$$claims[C]` has only **two** states (`ClaimWaiting(P)` -> `ClaimUsed(P, op)`), never `ClaimClaiming`.
- `AccountLinking` on single-threaded `H(Acc)` carries `:status` ∈ `{:probing, :chosen, :failing}` and `:chosen` (`ClaimKey|nil`). `:probing -> :failing` and `:probing -> :chosen(C)` are mutually exclusive atomic transitions on `H(Acc)`. `:failing` never returns to `:probing` or `:chosen`; `:chosen(C)` returns to `:probing` **only** via strict CAS after observing permanent `ClaimUsed(*, other-op)` on `H(C)`.
- `$$enter-ops[op]` on `H(op)` holds **only** PII-free `EnterOpFailed`, written before `AccountLinking(op)` is deleted on refusal (and on no-claim refusal). Every `:chosen(C)` path checks `H(op)` before mutating `$$claims[C]`, and every returning/helping enter with `(> at says-at)` checks `H(self-op)` before updating `:says` — sealing `:all-after` stream replay of refused depot records without leaving any entry in `$$accounts[Acc]`.
- `:says` and `:says-at` on `AccountLinked` update **only when `(> at says-at)`** (Sid's Call 2).

**Step-by-step protocol:**
- **Step E1 (`H(Acc)`, 0 hops from `*enters`)**: Read `$$accounts[Acc]`:
  - **`AccountLinked(P, L', M', says, says-at)`**:
    - If `(<= at says-at)`: hop `|hash P` (`E1b`) to read `:own-layer-made?`, return `{:person P :own-layer L' :making M' :own-layer-made? flag}`. (1 hop total, 0 writes.)
    - If `(> at says-at)`: hop `|hash self-op` (`E1a`) to verify `$$enter-ops[self-op]` is not `EnterOpFailed` (if `EnterOpFailed`, immediately `(ack-return> {:refused :not-invited})` with 0 mutations); else hop `|hash Acc` (`E1c`) to update `:says`/`:says-at` iff `(> at says-at)`, then hop `|hash P` (`E1b`) to read `:own-layer-made?` and return.
  - **`nil` (unlinked account)**:
    - If both `C_code` and `C_email` are `nil`: hop `|hash self-op` (`E2c-direct`) -> write `$$enter-ops[self-op] = EnterOpFailed` -> `(ack-return> {:refused :not-invited})`. (`$$accounts[Acc]` stays `nil`.)
    - Else: write `AccountLinking(self-op, C_code, C_email, L, M, S, t, status=:probing, chosen=nil)` to `$$accounts[Acc]` and route to Step E2 probe.
  - **`AccountLinking(op, ..., status, chosen)`** (own retry or competing enter helping `op`):
    - `:status == :failing`: hop `|hash op` (`E2c-fail`) -> ensure `EnterOpFailed` -> hop `|hash Acc` (`E2d`).
    - `:status == :chosen`: hop `|hash op` (`E2c-confirm`) with `chosen`.
    - `:status == :probing`: route to Step E2 read-only probe for `op`.
- **Step E2 (`H(C)` — strictly READ-ONLY probe, never mutates `$$claims`)**:
  - Probe `C_code` first (if present), else `C_email`.
  - Usable iff `$$claims[C]` is `ClaimWaiting(P)` or `ClaimUsed(P, op)` -> hop `|hash Acc` (`E2'`) with `probed = C`.
  - If `:code` missed (`nil` or `ClaimUsed(*, other-op)`) and `C_email` is present: hop `|hash C_email` recording whether `:code` was `nil`.
  - If `:email` missed with `ClaimUsed(*, other-op)` AND `:code` was previously `nil`: re-check `C_code` once (`:code-recheck`) so a refusal `:not-invited` always has a single-instant linearization point (`X-01`).
  - If no claim is usable: hop `|hash Acc` (`E2'`) with `probed = nil`.
- **Step E2' (`H(Acc)` — choose vs latch failure)**: Read `acc = $$accounts[Acc]`:
  - If `acc` is no longer `AccountLinking(op)`: loop to Step E1 for `self-op`.
  - If `:status == :failing`: hop `|hash op` (`E2c-fail`).
  - If `:status == :chosen`: hop `|hash op` (`E2c-confirm`) with `(:chosen acc)`.
  - If `:status == :probing` and `probed != nil`: atomically set `:status :chosen, :chosen probed` on `H(Acc)`, hop `|hash op` (`E2c-confirm`).
  - If `:status == :probing` and `probed == nil`: atomically latch `:status :failing` on `H(Acc)`, hop `|hash op` (`E2c-fail`).
- **Step E2c (`H(op)` — replay guard)**:
  - *Confirm (`E2c-confirm`)*: read `$$enter-ops[op]`. If `EnterOpFailed`, hop `|hash Acc` (`E2d`) without touching `C`; else hop `|hash chosen` (`E3`).
  - *Mark failed (`E2c-fail`)*: write `$$enter-ops[op] = EnterOpFailed`, hop `|hash Acc` (`E2d`).
- **Step E2d (`H(Acc)` — finish refusal)**: If `acc` is still `AccountLinking(op)`, delete via `NONE>`. If `op == self-op`, `(ack-return> {:refused :not-invited})`; else (helper) loop to Step E1 for `self-op`.
- **Step E3 (`H(chosen)` — claim commit)**: Read `$$claims[chosen]`:
  - `ClaimWaiting(P)`: write `ClaimUsed(P, op)` (**commit point**) -> hop `|hash P` (`E4`).
  - `ClaimUsed(P, op)`: hop `|hash P` (`E4`).
  - `ClaimUsed(*, other-op)` (another account won `chosen` first): hop `|hash Acc` (`E3b`) -> if `acc` is STILL `AccountLinking(op, :status :chosen, :chosen chosen)`, reset to `:status :probing, :chosen nil` (strict CAS!), then loop to Step E1.
- **Step E4 (`H(P)` — enter root)**: If `r` is not yet `RootEntered`, transition `RootInvited`/`RootReserving` to `RootEntered` fixing `:own-layer L`, `:making {U M}`, `:own-layer-made? false`, `:linked-provider`/`:linked-subject`, `:next-actor-seq 0`, `:next-sign-in-seq 0`. Read fixed `L', M'` and present `:own-layer-made?` flag -> hop `|hash Acc` (`E5`).
- **Step E5 (`H(Acc)` — link account)**:
  - If `acc` is `AccountLinking(op)`: write `AccountLinked(P, L', M', target.says, target.at)`.
  - If `helping?` (`op != self-op`) AND `(> self.at (:says-at acc1))`: hop `|hash self-op` (`E1a`) to confirm `self-op` is not `EnterOpFailed` before upgrading `:says` (and reading `:own-layer-made?` on `H(P)`).
  - Else if `(not helping?)` AND `(> self.at (:says-at acc1))`: update `:says`/`:says-at` directly and emit `(ack-return> ...)`.
  - Else: emit `(ack-return> {:person P :own-layer L' :making M' :own-layer-made? flag})`.

**Why Witnesses 1–5, 7–8, 11, and `:all-after` replays are all impossible under this protocol:**
- *No double-claiming / no one-account-two-persons (Witnesses 1, 4, 11)*: Step E2 never mutates claims; `:chosen` is committed on `H(Acc)` before E3 mutates a claim; `:failing` latches on `H(Acc)` so no delayed helper can choose a claim once refusal begins; `EnterOpFailed` on `H(op)` stops any `:all-after` replay or delayed attempt from entering E3.
- *No one-claim-two-accounts / no stuck `ClaimClaiming` (Witnesses 2, 3, 5)*: `ClaimClaiming` does not exist; `ClaimWaiting -> ClaimUsed(P, op)` is a single atomic write on single-threaded `H(C)`.
- *Exact monotonic `:says` (Witnesses 7, 8, `2C-16`, `X-04`, `X-06`)*: `:says` upgrades strictly on `(> at says-at)` and only after verifying `self-op` is not `EnterOpFailed`.

#### 1C. `answer-ask` (Code `U` on `H(U)`, Root `P` on `H(P)`)

**Protocol and visibility-order invariant (Revision 4 — proven in [`agreement/`](agreement/README.md)):**
Each `answer-ask` call generates a random UUID `op-id` and appends `AnswerAskEvent` to `*asks` (`hash-by :code`).
- **Step 1 (`H(U)`, 0 hops from `*asks`)**: Read `$$asks[U]`:
  - `nil` (`:no-such-ask`): hop `|hash P` (`Step 1b`) to check `ok:<self-op>` (if absent, run take-out under `P` by `:at` and record `ok:<self-op> = AskOutcomeRefused(:no-such-ask)` so `:all-after` replay after a later `ask(U)` never flips `:no-such-ask` to accepted), then emit `(ack-return> {:refused :no-such-ask})`.
  - `AskYes` / `AskNo` (standing answer already committed): hop `|hash P` (`Step 1b`) to run take-out under `P` by `:at` (`SPEC.md` line 326–329), then emit `(ack-return> result)`.
  - `AskWaiting(H, asks, ends, born-over?)`:
    - If `(or born-over? (>= at ends))`: hop `|hash P` (`Step 1b`) to run take-out under `P` by `:at`, then emit `(ack-return> {:refused :over})`.
    - Else (in time): write `AskAnswering(H, asks, ends, born-over?, op, yes?, P, A, new-agent-label, G, until, at)` into `$$asks[U]` (**acquires exclusive slot on `U`; `ask-of` still projects this as `:state :waiting :answer nil`**). Hop `|hash P` to Step 2.
  - `AskAnswering(..., op0, yes0?, P0, ...)` (concurrent answer in flight on `U`, or Rama retry of `op0`):
    - If `op0 == op` (retry of this event): hop `|hash P` to Step 2.
    - If `op0 != op` (competing `answer-ask` from Copy 2): **help `op0`** — run Step 2 on `H(P0)` and Step 3 on `H(U)` for `op0`!
      - If `op0` was ACCEPTED (`{:yes ...}` or `{:no U}`): `U` is now permanently `AskYes` / `AskNo`. Copy 2 **always** hops `|hash P` (`Step 1b`) to run take-out under `P` by Copy 2's `:at` (even when `P == P0`, fixing Witness 10!), then emits `(ack-return> op0-result)`.
      - If `op0` was REFUSED on `H(P0)`: Step 3 reverted `$$asks[U]` to `AskWaiting` (if still `op0`). Loop back to Step 1 on `H(U)` so Copy 2 can now try its own answer `op`!
- **Step 2 (`H(P)`, the commit point for `P` and `op`)**:
  1. **Check `$$root-data["<P>\0ok:<op>"]` FIRST (Revision 4)**: if already present (`AskOutcomeYes` / `AskOutcomeNo` / `AskOutcomeRefused`), **skip take-out, validation, and mutation** (so replays and helpers never re-execute `ans.at` take-out!) and hop `|hash U` to Step 3 with the recorded outcome.
  2. Else run take-out under `P` by `ans.at`, then evaluate `ans` against `$$root-data` on `H(P)`:
     - Read `r = $$root-data["<P>\0r"]`. If not `RootEntered`: record `AskOutcomeRefused(:no-such-actor)` at `ok:<op>`, hop `|hash U` to Step 3.
     - If `ans.yes? == false` (a **no**): record `AskOutcomeNo(U)` at `ok:<op>`, hop `|hash U` to Step 3. (Judges no other part; client already verified all 8 parts fit.)
     - If `ans.yes? == true` (a **yes**):
       - If `new-agent-label == nil`: check `A == P` or `a:<A>` exists (else `AskOutcomeRefused(:no-such-actor)`), check `A` not ended (else `AskOutcomeRefused(:ended)`), check `g:<G>` absent (else `AskOutcomeRefused(:in-use)`).
       - If `new-agent-label != nil`: check `A != P`, `a:<A>` absent, and `g:<G>` absent (else `AskOutcomeRefused(:in-use)`).
       - If accepted: **atomically in this single-task segment on `H(P)`**:
         - If `new-agent-label != nil`: assign `a-seq = next-actor-seq`, increment `next-actor-seq`, write `a:<A>` (depth 1, `made-by = P`, `ended? = false`, `seq = a-seq`) and `ao:<a-seq>` (carrying `depth`, `seq`; no `ch:`).
         - Assign `g-seq = next-sign-in-seq`, increment `next-sign-in-seq`, write `g:<G>` (`kind = :program`, `initial-hash = ask.hash`, `hash = ask.hash`, `label = ask.asks`, `begun = ans.at`, `until = ans.until`, `ended? = (<= ans.until ans.at)`, `own-layer = (:own-layer r)`, `own-layer-made-at-begin? = (:own-layer-made? r)`). If `(> ans.until ans.at)`, also write `LiveSignIn` to `$$live` and `exp:` to `$$root-data` (no `ag:`).
         - Write `AskOutcomeYes(P, A, G)` to `ok:<op>`.
         - Hop `|hash U` to Step 3! (**Notice: `A` and `G` commit and replicate on `H(P)` BEFORE Step 3 commits `AskYes` on `H(U)`!**)
- **Step 3 (`H(U)`)**:
  Read `$$asks[U]`:
  - If outcome is `AskOutcomeYes(P, A, G)`: if `$$asks[U]` is still `AskAnswering(op)`, write `AskYes(H, asks, ends, P, A, G)`. If `helping?` (`op != self-op`), hop `|hash (:root self-input)` (`Step 1b`) to run take-out at `(:at self-input)` before returning; else emit `(ack-return> {:yes {:root P :actor A :sign-in G}})`.
  - If outcome is `AskOutcomeNo(U)`: if still `AskAnswering(op)`, write `AskNo(H, asks, ends)`. If `helping?`, hop `|hash (:root self-input)` (`Step 1b`) to run take-out at `(:at self-input)` before returning; else emit `(ack-return> {:no U})`.
  - If outcome is `AskOutcomeRefused(reason)`: if still `AskAnswering(op)`, restore `AskWaiting(H, asks, ends, born-over?)`. If `helping?`, loop back to Step 1 on `H(U)` for `self-op`; else emit `(ack-return> {:refused reason})`.

**Trace AA-1 — Two copies answering `U` for the same person `P1` with differing clocks (`t1 = 500`, `t2 = 2000`; `G0/S0` expire at `1500` — Witness 10):**
1. Copy 1 (`t1 = 500`) locks `AskAnswering(op1)` on `H(U)` and hops to `H(P1)`. Copy 2 (`t2 = 2000`) arrives on `H(U)`, sees `AskAnswering(op1)`, and helps `op1`.
2. Step 2 on `H(P1)` runs `take-out(P1, 500)` (`G0/S0` still live), creates `G1`, and writes `ok:<op1> = AskOutcomeYes`.
3. Step 3 on `H(U)` commits `AskYes`. Copy 1 returns `{:yes ...}`. Copy 2 (`op1 != op2`, `helping? = true`) hops `|hash P1` (`Step 1b`), runs `take-out(P1, 2000)` (ending `G0` and closing `S0`), and then returns `{:yes ...}` — guaranteeing every read after Copy 2 returns sees `G0` and `S0` taken out.

### 2. Live standing, take-outs, 32-deep 2-scan `end-actor` cascade, and history separation

#### What is live under root `P` (Revision 2 — `ch:`, `ag:`, `sg:` deleted; live sign-in order in `$$live`)
- **Actors**: `P` herself is at depth 0 and never ends in this build. Each agent `A` lives at depth `1..32` (`a:<A>` holds `:made-by`, `:depth`, `:ended?`, `:seq`), ordered in `ao:<hex-seq>` (`ActorOrderRec` carries `:actor`, `:made-by`, `:depth`, `:label`, `:ended?`, `:seq`) for both `under` and 1-scan `end-actor` cascades. *(Revision 7 is closed for this build: Auth's depth stays at `32` makers below root, matching `SPEC.md` and `reference.clj`.)* An actor is live iff `:ended? false` — because `end-actor` eagerly marks every descendant `:ended? true` in one atomic transaction, no read ever traverses parent pointers.
- **Sign-ins**: Live sign-ins exist in `$$live` (`"<P>\0g:<G>" -> LiveSignIn`, holding live sign-ins only with `:seq` so `under` never walks ended sign-ins), `$$root-data` (`g:<G>` with `:ended? false`), and `exp:<hex-until>:g:<G>`. A sign-in's expiry time is strictly its present secret's `:until` (`SPEC.md` line 335); `:previous` stays readable until the next replacement and never extends a sign-in's lifetime.
- **Sessions**: Open sessions exist in `$$live` (`"<P>\0s:<S>" -> LiveSession`, carrying immutable `:driven-by` internally while `watch-root`'s client projection emits `{:sign-in G :until t3}` only), `$$root-data` (`s:<S>` with `:closed? false`, keeping `:driven-by`), and `exp:<hex-until>:s:<S>`.

#### Step-by-step trace: Take-out on any write naming `:root P`
Which writes trigger take-out (`SPEC.md` lines 326–334): `begin-sign-in`, `replace-secret`, `end-sign-in`, `make-actor`, `end-actor`, `open-session`, `close-session`, and `answer-ask` (all paths). `invite`, `enter`, `own-layer-made`, and `ask` name no `:root` and take nothing out.

*Concrete scenario*: Under root `P`, sign-in `G1` (`:until 1000`) has two open sessions: `S1` (`:until 900`) and `S2` (`:until 2000`, outliving `G1`). Sign-in `G2` (`:until 5000`) has open session `S3` (`:until 1000`) and open session `S4` (`:until 3000`). Now `open-session {:root P :sign-in G2 :session S3 :until 6000 :at 1000}` arrives on `H(P)`:
1. **Before judging the write**, execute take-out for `at = 1000` on `H(P)`:
   - Range-scan `$$root-data` on `["<P>\0exp:", "<P>\0exp:<hex-1000>;")`. This single seek returns exactly three expired `ExpiryEntry` rows: `(S1, 900)`, `(G1, 1000)`, `(S3, 1000)`. (When empty — the common case — take-out ends after 1 seek + 0 iter-reads!)
   - Because at least one expired entry is a sign-in (`G1`), scan `$$live` `["<P>\0", "<P>\1")` once (1 seek) to collect all live sign-ins and open sessions of `P` in memory (`G1`, `S1`, `S2`, `S3` — including `S2` whose `:until = 2000 > 1000` because its parent `G1` expired!). (If only sessions expired, read only `$$live["<P>\0s:<S>"]` — 0 range scans on `$$live`.)
   - Compute `rd-upserts` (`g:<G1>` `:ended? true`, `s:<S1>/<S2>/<S3>` `:closed? true` preserving `:driven-by`), `rd-deletes` (`exp:` keys for `G1, S1, S2, S3`), and `live-deletes` (`g:<G1>`, `s:<S1>/<S2>/<S3>`) in pure Clojure, and apply them via flat `ops/explode` + `local-transform>`.
2. **Now judge `open-session(G2, S3, :until 6000, :at 1000)`**: `s:<S3>` is now `:closed? true` (taken out in Step 1), so its `:until` is **not** moved to `6000` and it is **not** reopened -> `(ack-return> {:open S3})`.
3. **Commit & watch**: All writes commit atomically on `H(P)`. `watch-root` subscribers receive **one** atomic `MultiDiff` removing `G1`, `S1`, `S2`, `S3` together.
4. **Born-expired edge case (`t1 <= at`)**: On `begin-sign-in`, `replace-secret`, `open-session`, or `answer-ask (:yes)` where `until <= at`, the record is written to `$$root-data` (`g:<G>` or `s:<S>`) already marked `:ended? true` / `:closed? true` (and for `replace-secret`, any open sessions of `G` found via `$$live` `s:` scan are closed and removed from `$$live` and `exp:`), and the item is never inserted into `$$live` or `exp:`.

#### Step-by-step trace: 32-deep 2-scan `end-actor` cascade (Revision 2: 3 seeks total, zero recursive `loop<-`)
*Concrete scenario*: Root `P` (depth 0) made `A1` (depth 1) -> `A2` (depth 2) -> ... -> `A32` (depth 32), plus 3,000 actors, 2,000 live sign-ins, and 5,000 open sessions (plus 10,000 historical closed sessions). Now `end-actor {:root P :actor A1 :at t}` arrives on `H(P)`:
1. Run take-out under `P` by `t`.
2. Read `r` (`RootEntered`) and **Seek 1**: point-read `a:<A1>` (`:ended? false`; `A1 != P`, giving `start-seq = (:seq A1)`).
3. **Seek 2 — Forward `ao:` range scan**: `(local-select> [(sorted-map-range "<P>\0ao:<hex-start-seq>" "<P>\0ao;")] $$root-data :> *ao-map)`. Because `make-actor` assigns strictly increasing `seq` (`seq(maker) < seq(child)` always holds), a single linear pass in pure Clojure over `*ao-map` finds every live descendant in `A1`'s subtree (`ended-actors = #{A1 ...}`) and builds the `a:` and `ao:` `:ended? true` upserts directly — with **0 per-actor point seeks** and **0 `ch:` index**.
4. **Seek 3 — Single `$$live` range scan**: `(local-select> [(sorted-map-range "<P>\0" "<P>\1")] $$live :> *live-map)`. Because ASCII `'g' < 's'`, all `LiveSignIn` entries precede all `LiveSession` entries:
   - Pass 1 over `LiveSignIn`s: for every `g` with `(contains? ended-actors (:actor g))`, add `G` to `ended-sign-ins`, build ended `SignInRec` upsert for `g:<G>` directly from `LiveSignIn` (0 point seeks on `g:<G>`!), add `exp:<until>:g:<G>` to `rd-deletes`, and add `"<P>\0g:<G>"` to `live-deletes`.
   - Pass 2 over `LiveSession`s: for every `s` with `(contains? ended-sign-ins (:sign-in s))`, build closed `SessionRec` upsert for `s:<S>` directly from `LiveSession` (preserving `:driven-by` with 0 point seeks on `s:<S>`!), add `exp:<until>:s:<S>` to `rd-deletes`, and add `"<P>\0s:<S>"` to `live-deletes`.
   - **Zero historical ended sign-ins or closed sessions are ever touched!**
5. **Flat atomic application (zero recursive `loop<-`)**: Apply `rd-upserts`, `rd-deletes`, and `live-deletes` via top-level `(ops/explode ...)` + `(local-transform> ...)` in one atomic stream segment on `H(P)` (eliminating JVM stack overflow on non-yielding `loop<-`).
6. **Bound**: **3 RocksDB seeks total** regardless of subtree size (down from `18,000` seeks in the Phase-1 BFS — 39.6× faster in Probe C: `65.41 ms` vs `2,590.78 ms` at 600/400/1,000; `< 2 s` bound at 3,000/2,000/5,000, far below the `8 s` `ping-task` outage ceiling).
7. **Why no read costs more with depth (1..32)**: `who-is` reads `a:<A32>` directly (1 seek, `:ended? true`); `under(P)` scans `ao:` and `$$live` `g:` directly; `watch-root(P)` reads `$$live` only.

#### History kept apart from what is live
- `$$live` contains **strictly** currently live sign-ins (with `:seq` for live-only `under` ordering) and currently open sessions (`NONE>` on every end/close/take-out).
- `exp:` in `$$root-data` contains **strictly** currently live items (`NONE>` on every end/close/take-out).
- `a:`, `ao:`, `g:`, `s:`, `ok:` in `$$root-data` and `$$asks`, `$$claims`, `$$accounts`, `$$enter-ops` retain permanent history (`SPEC.md` line 399: "Nothing is deleted").
- Neither `watch-root`, take-outs, `end-sign-in`, `end-actor`, nor `under`'s `:sign-ins` ever scans `g:` or `s:` history; `who-is` does point lookups by exact key (`g:<G>`, conditionally `r`, `s:<S>`, `a:<A>`), costing the same after 10 million closed sessions as after 1.

### 3. `who-is`: one round trip, one task, 2-seek fast path (Revision 3 + Revision 8)

**Step-by-step execution trace of `(who-is client {:root P :sign-in G :session S-or-nil})`:**
1. **Client edge validation** (`softland.auth.client`, 0 network calls): check client is an Auth client (else `{:could-not :read-the-event}`), check client not closed (else `{:could-not :closed}`), check input map has exact keys `#{:root :sign-in :session}` (`contains?` on `:session` even when `nil`) with UUID `:root P`, UUID `:sign-in G`, and `nil` or UUID `:session S` (else `{:could-not :read-the-event}`).
2. **Client-side direct routing (1 network round trip to 1 task)**: invoke query topology `"who-is"` with `(P, G, S)`. Leading `(|hash *root)` routes directly to task `H(P)`.
3. **Read 1 — Sign-in record (`$$root-data["<P>\0g:<G>"]`) FIRST**: 1 RocksDB point seek.
   - **Fast path** (>99.99% of entered requests — `*sig` is non-nil AND `(:own-layer-made-at-begin? *sig)` is `true`): `P` is guaranteed `RootEntered` with `:own-layer = (:own-layer *sig)` and `:own-layer-made? = true` -> **skip `"<P>\0r"` completely (`0` seeks on `r`)!**
4. **Read 2 (conditional fallback) — Root record (`$$root-data["<P>\0r"]`)**: executed **only** when `*sig` is `nil` OR `(:own-layer-made-at-begin? *sig)` is `false` (1 seek).
   - If absent or not `RootEntered`: short-circuit immediately and emit `{:sign-in nil :session nil :own-layer nil :own-layer-made? false}`.
   - If `RootEntered`: obtain `:own-layer` and current `:own-layer-made?`.
5. **Read 3 (conditional) — Session record (`$$root-data["<P>\0s:<S>"]`)**: executed only when entered AND `S != nil` (1 seek).
6. **Read 4 (conditional) — Actor record (`$$root-data["<P>\0a:<A>"]`)**: executed only when `*sig` is non-nil, `(:ended? *sig)` is `true`, AND `(:actor *sig) != P` (1 seek).
7. **Return (`|origin`)**: assemble and return the bounded response map in the same single round trip.

**Seek & latency arithmetic (Rama skill model: `0.5 ms` / SSD seek, `5 µs` / iterator Next):**

| Case | PState keys read | Seeks | Iter-reads | Worst-case cold SSD disk cost | Aggregate cluster work (throughput) |
|---|---|---|---|---|---|
| Entered root (`made?=true`), live `G`, `S = nil` | `g:<G>` | **1** | 0 | `1 × 0.5 ms = 0.5 ms` | 1 seek on 1 task |
| Entered root (`made?=true`), live `G`, `S != nil` (dominant HTTP request) | `g:<G>`, `s:<S>` | **2** | 0 | `2 × 0.5 ms = 1.0 ms` | 2 seeks on 1 task |
| Entered root (`made?=true`), ended agent `G` (`A != P`), `S = nil` | `g:<G>`, `a:<A>` | **2** | 0 | `2 × 0.5 ms = 1.0 ms` | 2 seeks on 1 task |
| Entered root (`made?=true`), ended agent `G` (`A != P`), `S != nil` | `g:<G>`, `s:<S>`, `a:<A>` | **3** | 0 | `3 × 0.5 ms = 1.5 ms` | 3 seeks on 1 task |
| Unknown or waiting root `P` (`g:<G>` miss -> `r` miss) | `g:<G>`, `r` | 2 | 0 | `2 × 0.5 ms = 1.0 ms` | 2 seeks on 1 task |
| Edge: `G` begun before `own-layer-made` (`made-at-begin?=false`), `S != nil` | `g:<G>`, `r`, `s:<S>` (+ `a:<A>` if ended agent) | 3–4 | 0 | `1.5–2.0 ms` | 3–4 seeks on 1 task |

- **Independence from history and depth**: Every key is looked up directly in RocksDB's top-level LSM index with 0 scans, so a person with 5,000,000 closed sessions and a 32-deep agent tree pays the exact same `1–3` seeks (`2` on the dominant HTTP request) as a brand-new person.
- **Why 2 seeks is optimal for `(G, S != nil)`**: `g:<G>` and `s:<S>` are independent entities (`S` may belong to a different sign-in `G' != G` under `P`), while `:own-layer` (immutable) and `:own-layer-made-at-begin?` (monotonic `false -> true`) are embedded on `g:<G>` at 0 extra write seeks — cutting weighted cluster RocksDB seeks from `2.89` to `1.925` (`-33.4%`).

### 4. watch-root: bounded value, 20,000 watched roots per task, and not-reached

#### Why `watch-root`'s value stays small
- `watch-root` subscribes exclusively to `$$live` on range `["<P>\0", "<P>\1")` with `{:pkey P}`.
- `$$live` stores **only** live sign-ins (`LiveSignIn`) and open sessions (`LiveSession`, carrying immutable `:driven-by` internally while `watch-root`'s client projection strictly omits `:driven-by` and delivers `{:sign-in G :until t3}`). Every `end-sign-in`, `close-session`, `end-actor` cascade, and `:root`-write take-out deletes ended/closed entries from `$$live` via `NONE>` in the same single-task atomic batch.
- Consequently, a root with 10 live sign-ins, 100 open sessions, and 5,000,000 closed historical sessions has a `$$live` slice of only 110 entries (~15 KB). Every `open-session`, `close-session`, or `replace-secret` writes 1 key (~64–150 B) and pushes an incremental `KeyDiff` or `KeyRemoveDiff` (or 1 atomic `MultiDiff` for multi-item ends/take-outs, verified by probe `auth_rama_probe_2.clj`).
- `softland.auth.client` maintains the projected `{:sign-ins {...} :sessions {...}}` map incrementally from `KeyDiff` / `KeyRemoveDiff` / `MultiDiff` (falling back to full rebuild only on `ResyncDiff` or `NewValueDiff`), so per-change work on Rama's 2-thread proxy callback pool is O(changed entries), keeping callbacks non-blocking.

#### How many roots can be watched on one task (`pstate.proxy.limit = 20000`)
- Empirical measurement (`WORKFLOW-AND-MODEL.md` §2.1, `probes/proxy_limit_probe.clj`) proved that `pstate.proxy.limit` bounds subscriptions strictly **per-(module, PState, task-partition)** across **all** connected client handles combined, and that `Auth/$$live` does not share a limit with `Store` or with `Auth/$$root-data`.
- At module launch, `Auth` executes `(set-launch-pstate-dynamic-option! setup "$$live" "pstate.proxy.limit" 20000)`, lifting the limit from Rama's 1,000 default to **20,000 subscriptions per task** (matching Red Planet Labs' TPC-C benchmark setting; tunable dynamically without redeploy via `rama setOption`).
- **Client-side deduplication**: Within one `Auth` client instance (one runtime JVM copy), multiple `(watch-root client P f)` calls for the same root `P` share **one** `foreign-proxy` handle. When the last watcher for `P` invokes its stop function, the client closes that `foreign-proxy` immediately, releasing its server-side slot.
- **Capacity across cluster**: At `N = 16` tasks, `$$live` supports 320,000 concurrent `(root, runtime-copy)` subscriptions; at `N = 64` tasks, 1,280,000. (Note: if `K` distinct runtime copies watch the same root `P` simultaneously, each copy holds 1 subscription, consuming `K` of `P`'s task's 20,000 slots.)

#### Step-by-step trace: Registration, register-before-`who-is`, and steady-state updates
1. Caller calls `(watch-root client P f)`. Client checks that `client` is an Auth client (else `{:could-not :read-the-event}`), checks closed flag (if closed, returns `{:could-not :closed}` in place of the stop function, `SPEC.md` line 361), and validates `P` is a UUID and `f` is `(fn? f)` (NOT `ifn?`, per Revision 6; else `{:could-not :read-the-event}`).
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
3. **Trigger 3 — Client-side `ping-task` liveness monitor (within ~2–8 s; Revision 2 outage ceiling)**:
   - While the client holds >= 1 active watch, a single daemon scheduler pings all `N` tasks (`0 .. num-partitions - 1`) in parallel every **2 seconds** using `"ping-task"` (`(|direct *task-id)`, 0 RocksDB seeks, **5 s** timeout).
   - *Healthy steady state*: all `N` tasks reply `true` in < 1 ms -> 0 per-watch probes, negligible overhead (`N` tiny in-memory RPCs / 2 s per runtime copy).
   - *Full module/cluster/network loss*: all `N` tasks fail (immediately on `destroy-module!` / connection refused, or within the 5 s timeout on silent blackhole) -> `fail-proxy!` runs immediately for every watched root `P`, delivering `:softland.auth.client/not-reached` once to every watcher within **2–7 seconds** of the loss.
   - *Partial worker loss (subset of tasks unreachable)*: the monitor issues a bounded **3 s** `foreign-select-async` probe (`[(sorted-map-range-from "<P>\0" 1)]` with `{:pkey P}`) for active watched roots (only during the outage); every watched root whose task fails to respond within 3 s receives `:softland.auth.client/not-reached` once within **5–10 seconds**, while watches on healthy tasks continue uninterrupted. Because the `5 s + 3 s = 8 s` outage ceiling sits well above worst-case 2-scan `end-actor` (`< 2 s` at 3,000/2,000/5,000), a heavy single-task cascade never triggers false `:not-reached` across sibling roots.
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
| `$$claims` | **No** (stores only non-PII `op-id` UUID in `ClaimUsed`) | **Yes, for email claims only** (key `ClaimKey(:email, E)`); **No** for code claims (`ClaimKey(:code, H)`) | **No** | Single `NONE>` transform on `$$claims[ClaimKey(:email, E)]` (O(1), 0 reads). Code claims require no erasure. |
| `$$root-data` (`"<P>\0r"`) | **Yes** (`linked-provider`, `linked-subject` pointer fields on `RootEntered`) | **Yes, for email claims only** (`claim-val` when `claim-kind == :email`) | **No** | Future `erase-person-pii(P)` reads `r` (1 seek) to locate `AccountKey` and `ClaimKey(:email, E)` in O(1) without scanning `$$accounts`/`$$claims`, replaces `linked-provider`/`linked-subject`/`claim-kind`/`claim-val` with `nil` (keeping `RootEntered`, `:own-layer`, `:making`, `:own-layer-made?`, and sequences intact), then hops to `H(Acc)` and `H(C)` to `NONE>` them. |
| `$$root-data` (`a:`, `ao:`, `g:`, `s:`, `exp:`, `ok:`) | **No** | **No** | **No** | Never touched by PII erasure. |
| `$$live` | **No** | **No** | **No** | Never touched by PII erasure. |
| `$$asks` | **No** | **No** | **No** | Never touched by PII erasure. |
| `$$enter-ops` | **No** (UUID `op-id` and `EnterOpFailed` only) | **No** | **No** | Never touched by PII erasure. |
| Depot `*enters` | **Yes** (every `EnterEvent`, accepted & refused alike) | **Yes** (`:email`) | **Yes** (`:says`) | Isolated low-volume depot. Future module update applies Rama `(depot-migration id fn)` (`depot-migration.md`) returning `DEPOT-TOMBSTONE` (or redacted record) for matching account/email — including refused enters (`SPEC.md:376`) — which rewrites the depot via Rama's two-log switch and deletes the old log from disk. Alternatively, because `$$accounts` is durable storage in its own right (`SKILL.md:28`), `*enters` can also enable independent per-depot trimming (`depot.max.entries.per.partition`) without affecting `*root-events` or `*asks`. |
| Depot `*invites` | **No** | **Yes, for email claims only** (`:claim`) | **No** | Isolated low-volume depot. Future module update applies `(depot-migration id fn)` returning `DEPOT-TOMBSTONE` (or redacted claim) for matching `:person` / `:email`, physically removing email bytes from disk. |
| Depot `*root-events` | **No** | **No** | **No** | Never scanned or migrated for PII erasure. Eligible later for independent thinning via `depot.max.entries.per.partition`. |
| Depot `*asks` | **No** | **No** | **No** | Never scanned or migrated for PII erasure. Eligible later for independent thinning via `depot.max.entries.per.partition`. |

#### Design invariants that keep future erasure and thinning clean
1. **No non-PII entity is keyed by PII**: `P`, actors, sign-ins, sessions, and asks are keyed solely by random UUIDs or 8-char codes. Erasing `$$accounts[Acc]` and `$$claims[[:email E]]` breaks zero lookups in `who-is`, `under`, `watch-root`, `ask-of`, or any root write.
2. **Zero PII leakage into code claims or coordination state**: Using random `op-id` UUIDs in `ClaimUsed` and `$$enter-ops` ensures `AccountKey` never escapes `$$accounts`, `$$root-data["<P>\0r"]`, and `*enters`.
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
| `enter` | No | `$$accounts[Acc]` is `AccountLinked(P, L', M')` -> updates `:says` & `:says-at` **only when `(> at says-at)`** (Sid's Call 2, after verifying `self-op` not `EnterOpFailed`), reads present `:own-layer-made?` on `H(P)`, drops offer & claims -> `{:person P :own-layer L' :making M' :own-layer-made? bool}` | 1. `:not-invited` (`Acc` unlinked and proves neither a waiting `:code-hash` claim nor a waiting `:email` claim) |
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
| Single stream topology `"auth"` with model-checked monotonic state machines + wait-free helping for `invite`, `enter`, `answer-ask` | Microbatch topology for all writes, or hybrid microbatch for `{invite, first enter}` + stream for root/ask writes | Rama enforces strict single-topology write ownership per PState (`IllegalWriteException` verified in Probe B), so `answer-ask` cannot move to microbatch without a 2-cycle internal-depot ping-pong against `$$root-data`/`$$live`, and moving `invite` + first `enter` to microbatch adds 2 PStates (`$$mb-persons`, `$$mb-decisions`), an extra internal handoff before `enter` returns (~350–400 ms), and task-0 microbatch barrier coordination (`SPEC.md:446`). The revised single-stream state machines are exhaustively verified on carbon's unchanged checker (`agreement/`: 128 runs, 88,261 states, 0 breaks across all 4 fault/retry modes), keep zero task-0 coordination, ~5 ms latency, and single PState ownership. |
| Flat top-level `$$root-data` (`{String -> IRootDatum}`) with 7 typed ASCII key prefixes (`ch:`, `ag:`, `sg:` deleted) | Nested `{UUID -> fixed-keys-schema}` with subindexed inner maps, 6 separate flat PStates, or keeping `ch:`/`ag:`/`sg:` | Nested subindexes pay an extra parent seek on every lookup (`who-is` jumps from 2 seeks to 4 seeks — +100% I/O on the dominant read). Six separate flat PStates multiply per-task RocksDB memory/cache overhead 6× and destroy prefix block locality. Deleting `ch:`, `ag:`, `sg:` and using 2-scan `end-actor` cuts `end-actor` from 18,000 seeks to 3 seeks (39.6× faster in Probe C) and removes 1 write per `open-session`. |
| Separate top-level `$$live` (`{String -> ILiveEntry}`) watched by `(sorted-map-range "<P>\0" "<P>\1")` | Non-subindexed `{UUID -> {:sign-ins ... :sessions ...}}`, or merging `$$live` into `$$root-data` | Open sessions have no protocol cap (can exceed 100), so a non-subindexed map causes full-map read-modify-write per `open-session` and violates the subindexing rule. Merging `$$live` into `$$root-data` runs range-proxy diff matching on every history/index write (3–4× extra reactivity overhead) and churns `NONE>` tombstones in the permanent history LSM tree. Carrying immutable `:driven-by` on `LiveSession` (omitted by `watch-root` projection) and full fields on `LiveSignIn` gives 0-seek `g:`/`s:` updates on `end-actor`/`end-sign-in`/take-out with zero spurious diffs. |
| Sorted expiry index `exp:<hex-until>:<kind>:<id>` in `$$root-data` | Scanning all entries of `$$live` for `P` on every `:root` write | With `exp:`, take-outs when nothing has expired cost 1 seek + 0 iter-reads (`0.5 ms`) even for a root with 2,000+ open sessions; scanning `$$live` costs `0.5 ms + O(open sessions × 5 µs)` on every `open-session` and `replace-secret`. Maintaining `exp:` uses zero-read `termval` / `NONE>` writes. |
| Uniform choose-at-account `enter` with PII-free `$$enter-ops[op] = EnterOpFailed` on refusal only (zero `ClaimClaiming`) | Uncoordinated 1-claim fast path, or `ClaimClaiming` + `EnterOpTryEmail`/`EnterOpWon` | The Phase-1 1-claim fast path and `ClaimClaiming` broke Q1, Q3, Q4, and Q5 in Step 1 (`Witnesses 1–5, 11`). Choosing on single-threaded `H(Acc)` with `:status (:probing/:chosen/:failing)` + read-only E2 probe + `EnterOpFailed` replay guard eliminates `ClaimClaiming` entirely, writes 0 records to `$$enter-ops` on successful enters, leaves `$$accounts[Acc] = nil` on refusal, and passes 128/128 model-check runs with 0 breaks. |
| `answer-ask` on `*asks` (`hash-by :code`) with `AskAnswering` + `ok:<op-id>` checked BEFORE take-out + unconditional helper take-out | `answer-ask` on `*root-events`, or writing `AskYes` on `H(U)` before `H(P)` | Committing `AskYes` before `H(P)` exposes `:yes` in `ask-of` while `who-is` still returns `nil`. Checking `ok:<op-id>` before `take-out` eliminates replay take-outs on `answer-ask` at 0 extra seeks; always running helper take-out on `(:root self-input)` at `(:at self-input)` closes Witness 10 (`P == P0`). |
| 2-seek `who-is` (`g:<G>` first, embedding immutable `:own-layer` & `:own-layer-made-at-begin?` on `SignInRec`) + conditional `a:<A>` on ended agent sign-ins | 3-seek `who-is` (`r` always read first), or always reading `a:<A>` | Embedding `:own-layer` + `:own-layer-made-at-begin?` on `g:<G>` costs 0 extra write seeks and cuts weighted cluster RocksDB seeks from `2.89` to `1.925` (`-33.4%`). Reading `a:<A>` only when `G` is ended and `A != P` preserves `:actor-ended? true` on pre-existing ended sign-ins without scanning ended sign-in history. |
| Non-yielding 3-seek 2-scan `end-actor` with flat `ops/explode` writes; `{:allow-yield? true}` on `ao:` scan in `"under"` | Non-yielding recursive-`loop<-` BFS, yielding inside `end-actor`, or never yielding in `"under"` | Yielding inside `end-actor` breaks read-to-write atomicity (`dataflow.md:199`). Recursive `loop<-` BFS overflows the JVM stack and stalls tasks past 3 s (`PLAN_VALIDATION.md` Item c). 2-scan + flat `ops/explode` stays atomic, takes 3 seeks (`< 2 s` at 3,000/2,000/5,000), and never overflows. `"under"` is read-only over a snapshot, so yielding there protects `who-is`. |
| Client-side `ping-task` liveness monitor (2 s cadence, 5 s timeout + 3 s fallback probe = 8 s ceiling, 0 seeks) + 5 s open timeout + `UngracefulTerminationDiff` | Relying solely on Rama's `UngracefulTerminationDiff`, or 2 s + 1 s = 3 s ceiling | Rama's default `UngracefulTerminationDiff` takes ~100 s after `destroy-module!`. A 3 s ceiling causes false `:not-reached` storms if any heavy write takes 3 s; raising the ceiling to `5 s + 3 s = 8 s` keeps `:not-reached` within seconds of real outages while staying safely above worst-case 2-scan `end-actor` (`< 2 s`). |
| `watch-root` awaits `foreign-proxy-async` open (bounded 5 s) before returning `stop` | Returning `stop` immediately before the proxy registers on `H(P)` | Eliminates the blind window in `SPEC.md`'s *"registers first and then reads with `who-is`"* pattern. |
| Four depots (`*root-events`, `*asks`, `*invites`, `*enters`) | One depot or two combined depots | Separating `*invites` and `*enters` isolates all PII into the two lowest-volume logs for future `depot-migration` erasure without scanning `*root-events` or `*asks`, and gives each write 0 initial hops to its primary key. |
| Store immutable `:initial-hash` beside mutable `:hash` on `SignInRec` | Match `begin-sign-in` repeat against current `:hash` | After `replace-secret` rotates `:hash` from `H1` to `H2`, a delayed repeat of the original `begin-sign-in` (carrying `H1`) would falsely return `:in-use` instead of `{:begun G}`. |
| No per-event dedup ring buffer on `*root-events` (cost tradeoff, Revision 8) | Store a bounded ring buffer of the last `W = topology.stream.checkpoint.progress.threshold` event IDs per partition (~16 KB/task) | Preventing Wall 3 on `*root-events` does **not** require unbounded storage (~16 KB/task bounded ring suffices), but checking and updating it still adds +1 seek and +1 write on every `open-session` and `replace-secret`. Under `SPEC.md`'s NTP clock-keeping rule (`line 343`), paying +33% write I/O on the hottest writes for a clock-skew-exceeds-session-lifetime crash window is rejected as a deliberate cost tradeoff (see `Walls` item 3). (`answer-ask` already closes replay take-outs at 0 extra seeks via `ok:<op>` checked first.) |

## Walls

Where a requirement in `SPEC.md` or `IMPLICIT_SPEC.md` cannot be met strictly as written under Rama's execution model without breaking another core requirement:

1. **Cross-partition mid-flight read visibility (`IMPLICIT_SPEC.md` line 115–116 vs `SPEC.md` line 382 + Rama's independent task commits — Rama's limit).**
   - *The wall*: In Rama (both stream and microbatch — `stream.md:41`, `microbatch.md:128`), distinct partitions commit independently with no multi-partition read snapshot. Every single-partition write (all 9 writes on `P` or `U`, including 32-deep 2-scan `end-actor` cascades and take-outs) satisfies atomic read visibility strictly at every instant, and after **any** write returns (`SPEC.md` line 93), every copy reads the full effect on all partitions (verified in `agreement/` Q5 = 0 breaks). However, during the sub-millisecond window **while `answer-ask` or first-time `enter` is still in flight**, a concurrent reader polling two different partitions can observe `A`/`G` on `H(P)` ~0.5 ms before `ask-of(U)` flips from `:waiting` to `:yes` (or observe `C` marked used on `H(C)` ~0.5 ms before `who-is(P)` shows `:own-layer`).
   - *Position*: Do not break `SPEC.md` line 382 (*"No one task that everything passes through"*) or `who-is` = 1 round trip to 1 task. Ordering `answer-ask` so `H(P)` commits **before** `H(U)` guarantees `ask-of` `:yes` => `who-is`, `under`, and `watch-root` already hold `A` and `G`. Accept Rama's settled-result semantics during the sub-millisecond in-flight window.

2. **Isolated single-proxy errors on a healthy task under caller-supplied `cluster` config (`SPEC.md` line 211–213 vs `(open cluster module-name)` — Rama's client-API boundary).**
   - *The wall*: Probe `auth_rama_probe.clj` proved Rama 1.6.0 does not emit `UngracefulTerminationDiff` within 15 s under default client config (`foreign.proxy.failure.window.seconds = 120`, `threshold = 10` -> ~100 s). Our `ping-task` monitor (5 s timeout + 3 s fallback probe = 8 s ceiling) + 5 s open timeout detects full cluster/module loss, partial worker/task loss, and open-time `pstate.proxy.limit` rejection within seconds without false triggers during heavy `end-actor` cascades. However, if a *single* already-open `foreign-proxy` encounters repeated server-side errors while its task remains healthy and answers `ping-task`, its termination timing is controlled by the `RamaClusterManager` passed into `(open cluster module-name)`.
   - *Position*: Keep `(open cluster module-name)` unchanged; rely on `ping-task` + open timeout for all connectivity/worker/module losses within seconds, and recommend `foreign.proxy.failure.window.seconds = 10` / `threshold = 2` on caller-constructed `RamaClusterManager`s.

3. **Uncheckpointed `*root-events` stream replay under backward clock skew larger than an entity's entire lifetime (cost tradeoff of the plan, NOT a Rama limit — Revision 8).**
   - *The note*: If Event 1 (`:at = 300`, fast clock) commits on `H(P)`, Event 2 (`:at = 150`, slow clock) immediately creates a sign-in/session `X` with `:until = 250` (`150 < 250 <= 300`), and the worker crashes *before* checkpointing stream progress (`topology.stream.checkpoint.progress.threshold`), Rama replays Event 1 (`:at = 300`) when `X` already exists in `$$live`. Event 1's replayed take-out at `300` ends `X` even though Event 1 originally preceded Event 2. (On `answer-ask`, Revision 4 eliminates this completely at 0 extra seeks by checking `ok:<op>` before take-out.)
   - *Position*: Eliminating this window on `*root-events` is feasible with a bounded per-partition ring buffer of `topology.stream.checkpoint.progress.threshold` event IDs (~16 KB/task — NOT unbounded storage), at the cost of +1 seek and +1 write (+33% I/O) on every `open-session` and `replace-secret`. Under `SPEC.md`'s existing NTP clock-keeping requirement (`line 343`) and normal session lifetimes, we deliberately decline that +33% write tax.

4. **`pstate.proxy.limit = 20000` bounds `(root, runtime-copy)` subscriptions across all copies (`SPEC.md` lines 363–365, 395–397 — matches spec verbatim).**
   - *The mechanism*: `pstate.proxy.limit` counts total open `ProxyState` subscriptions per `(module, PState, task)` across all connected runtime processes combined, matching `SPEC.md`'s wording *"over all copies of the runtime together"*. Client-side deduplication ensures 1 subscription per `(root, runtime-copy)`, and operators can raise `pstate.proxy.limit` dynamically via `rama setOption` if multi-copy fan-out per root grows.

## A session's choices, each Sid's to turn

1. **`:driven-by` on `open-session` and `who-is` (in [SPEC.md](SPEC.md)):** Optional `{:kind :person}` or `{:kind :model :model <text <= 256 chars>}`, kept with the session after close, unchanged on repeat, omitted from `watch-root`, absent/nil means not said and the runtime keeps reads. Rests on Sid's 12 October decision, entry 4 (`src-store/README.md`) and runtime page entry 8.
2. **Actor depth bound (Revision 7 — closed for this build):** Auth's depth stays at `32` makers below root (`P` depth 0, agents `A1..A32` depth `1..32`, `:too-deep` when `made-by` is at depth `32`), matching `SPEC.md` and `reference.clj`. (Note: `src-store/softland/store/act.clj` caps `:permission-id` nesting at 32 including the root permission itself = 31 delegated makers; kept separate for this build per Sid's instruction.)
3. **Unknown extra keys in input maps:** Rejected with `{:could-not :read-the-event}` on both top-level call maps and sub-maps (`:claim`, `:making`, `:new-agent`, `:driven-by`), matching `SPEC.md:121–127` and `reference.clj`.
4. **Provider keyword length bound:** Measured on full keyword text `(subs (str kw) 1)` (`ns/name` if namespaced, else `name`) `<= 256` JVM `char`s, matching `reference.clj`.
5. **Fixed-width integer timestamps:** Accept JVM `Byte`, `Short`, `Integer`, and `Long` (`int?`; rejecting `BigInt`, `BigInteger`, floats, nil) and coerce to `Long` before depot append.
6. **Born-over `ask` (`:ends <= :at`):** Persist `:born-over? true` on `$$asks[U]` so any later `answer-ask` returns `{:refused :over}` even from a runtime copy whose clock is behind `:ends`, while `ask-of` continues returning `:state :waiting` with `:ends`.
7. **`watch-root` awaits `foreign-proxy-async` open (up to 5 s) before returning `stop`:** Eliminates the register-before-`who-is` missed-end race; on open failure/timeout, returns a no-op `stop` and delivers `:softland.auth.client/not-reached` once on the client callback executor.
8. **`watch-root` liveness monitor cadence and timeouts (Revision 2):** `ping-task` every **2 s** (**5 s** timeout) while >= 1 watch is open, with a **3 s** per-watched-root fallback probe during partial worker outages (**8 s** outage ceiling, above worst-case 2-scan `end-actor` `< 2 s`), plus recommending `foreign.proxy.failure.window.seconds = 10` / `threshold = 2` on the caller's `RamaClusterManager`.
9. **Client blocking operation timeout:** Default **10 seconds** for writes, `who-is`, `under`, and `ask-of` before returning `{:could-not :reach-the-module}` (keeping `(open cluster module-name)` unchanged).
10. **PII erasure pointers on `$$root-data["<P>\0r"]`:** Store `:linked-provider`/`:linked-subject` and `:claim-kind`/`:claim-val` pointers on `RootEntered` so future erasure by person ID `P` runs in O(1) without scanning `$$accounts` or `$$claims`; keep `*invites` and `*enters` isolated for future Rama `depot-migration` or per-depot trim.
11. **`depot.ack.failure.on.any.streaming.failure` left at Rama's default (`true`):** First-attempt stream failures return `{:could-not :reach-the-module}` and rely on the contract's idempotent retry.
12. **Internal Rama object names:** Depots `*root-events`, `*asks`, `*invites`, `*enters`; PStates `$$root-data`, `$$live`, `$$accounts`, `$$claims`, `$$asks`, `$$enter-ops`; stream topology `"auth"`; query topologies `"who-is"`, `"under"`, `"ping-task"`. None cross the `softland.auth.client` contract.

## What was run and what was only read

### Exhaustive model check of the revised agreement (`spine/auth/agreement/`)
- Ran carbon's unchanged `auth-concurrent.model-check`, `auth-concurrent.spec-oracle`, and `auth-concurrent.step1-run/build-scenarios` over `spine/auth/agreement/src/auth_concurrent/plan_steps.clj` (Revisions 1, 2, 3, 4, 5) across **32 scenarios** (all 25 carbon scenarios + 7 extra adversarial scenarios `X-01..X-07`) × **4 fault/retry modes** (`:pure`, `:individual`, `:all-after`, `:timeout-overlap`) = **128 runs** (`3,421 ms`), **88,261** deduplicated states, **539** quiescent terminal histories, client re-runs checked in all 4 modes: **0 capped runs, 0 violations across all 5 questions** (`agreement/RESULTS.md`).

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
