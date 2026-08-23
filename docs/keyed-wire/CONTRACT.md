# keyed-wire — CONTRACT (the wire · the host · the demand)

**Cut 2026-08-23 · cutter: Fable (claude-fable-5) · effort: max.**
Package of the electric-native arc; fills the middle `docs/below-the-waist/engine-two-arrows.md` §8 names as absent. One pass, one document (work-package law). Status: **CUT — falsification round OWED**: ONE bounded fresh Codex-class session at Sid's hand (never the cutter's subagents), claim→source, evidence-cited, no verdict authority, no recut; author repairs in-session-of-record. Sid touch #1 = the header check + that round; touch #2 = atom acceptance. Everything else executes without him.

---

## 1 · Scope in plain words

Both ends of the gap are already keyed — Rama rows carry `unit-id`/`event-id`; the scene store is `{:slots {vi→slot} :index {address→#{vi}} :ordered (sorted-map …)}` patched by key (`scene_store.cljc:151-156`). The middle is absent: nothing consumes what the server serves, and the engine is a machine with no crank (`draw-frame!` has **zero** runtime callers — receipt §11.P-A).

This package builds that middle, in both directions:

- **The wire (down):** a keyed feed per lens — flat units-by-id + order/membership indexes + generation per accepted act + tombstones + watermark — assembled server-side from the rows that already exist, delivered over a long-lived stream. One accepted act → one feed value → one keyed store patch. Geometry settles gain their missing notify.
- **The host (client):** the small set of namespaces that turn the feed into a living screen — a courier flow, one apply edge into the existing store, a demand publisher, and the crank that gives `draw-frame!` its first caller. The screen maintains itself from one source (road 1b rides this; road order inverted per §8 "What the cut did to the road's order").
- **The demand (up):** the lens tells the server what it needs. Demand is MEMBERSHIP — grow/shrink container sets — never position windows (L16, `engine-two-arrows.md:349`: producer-side diff cost is shape-bound; position windows put every scroll in the permutation regime). v1 grain: container ids.

The whole-page pull survives only as the ORACLE, with its two named duties (§4, §8-S4).

## 2 · Refusals (one line each; every one an extension point routed LATER, never a void)

- **Per-key store views (hop 5, the 34-watch collapse).** `derive-store-frame` stays whole-lane; it demotes to oracle at the store-contract slice, which lands at composition pressure (render-seam law names the moment). Not here.
- **Keyed pool bridge activation.** `keyed-diff-update-pool!` (`buffer_pool.cljs:214`) stays the pre-stubbed receiver; this package's uploads ride the existing identity-gated path. Activates with the per-key-views slice.
- **Editing feel / optimistic local echo.** This wire delivers truth echoes correlated by act; typing-feel and local echo belong to the editing-rewire package under the new host (1a's successor). A1's "edit" is a dev-affordance act, not an editor.
- **The host probe (road 4).** Electric-generic-host vs Missionary-host is judged at the Block, built both ways. Nothing here pre-decides it.
- **`e/diff-by` as courier.** The transfer bench decides feed transport (§6); this package landing SSE-first decides nothing about that.
- **Faces-are-data compilation.** The 36-builder registry (`face_primitives.cljc:1322-1365` — §8's "16 builders" undercounts; corrected here with anchor) serves as-is via `compile-assembly`/`build-face-tree`.
- **Undo · multiplayer.** Named beneficiaries of the minted-value discipline; not builders here.
- **Spatial index / maintained visible-set.** Pick stays a linear-over-slots walk with per-slot AABB pruning (receipt §11.P-B); the walk-cost profile decides later (seam-law open item).
- **Multi-server fan-out.** v1 pins one JVM (§10 pins); collaboration scale is LATER.
- **True deletion (revoked sources).** Sid's fork, untouched; tombstone here is feed-delivery semantics only (§8-S5).
- **SEAM-STEP1 / shaping-correction scopes stay frozen.** Nothing here widens either.
- **No Rama module/topology/PState changes.** Server work is JVM-side read + assemble + stream over existing readers. If implementation genuinely needs a Rama change, that is a fork — ONE question, work continues around it.

## 3 · Laws (pointers, read primary; never restated here)

- `docs/decisions.md` "The render seam" :450-628 — proportionality at every layer · clocks never ancestors of derivation · ownership-not-sampling (one swap = one generation) · store is projection, never a second truth-owner · the fenced incremental unit + growth law (batch demotes to oracle, never deleted) · the five-declaration derivation contract · order is row data · minted diffs are values, each destination derives its own delta behind its own door.
- `docs/decisions.md` "One render substrate" :302.
- `docs/electric-native/DIRECTION.md` road 1c court riders + stage-stability tests (one-path recovery · tombstones · oracle demotion WITH duties transferred) — a stage is STABLE only if it passes all three.
- `docs/below-the-waist/engine-two-arrows.md` §8 — the fill shape ("demand up hierarchical · delivery down flat and keyed · applied atomically per accepted act"), the smallest slice, the courier position.
- `docs/scene-substrate/CONTRACT.md` §7 traps — binding here: T1 (descriptors, no closures in rows/slots) · T2 (reorders travel as `:change` on rank fields) · T3 (co-varying values in ONE `m/latest`) · T4 (effects at the reduce/consumer edge only) · T5 (`:ops` at upsert; unchanged slots `identical?` frame-over-frame) · T8 (pick inverse-transforms before hit-test) · T9 (one watch, no per-slot forks) · T10 (gesture ≠ assertion; only SETTLE commits) · T11 (container-local f32).
- Execution law: `.claude/skills/work-package/SKILL.md` · CLAUDE.md Token Economy · `.claude/skills/electric-docs/SKILL.md` (verified Missionary laws — the apply edge and crank are built on them).

## 4 · The feed — frozen content, courier-agnostic

One EDN value per accepted act on the live path (the server MAY coalesce acts on catch-up):

```clojure
{:watermark <order-key>          ;; high-water this value brings the lens to (fixed-width-order-key form, object_container.clj:243)
 :gen       <event-id>           ;; the accepted act; last included if coalesced
 :acts      [<event-id> …]       ;; acts covered, oldest→newest; singleton on the live path
 :units     {unit-id → row | :tombstone}
 :order     {container-id → {unit-id <rank>}}   ;; rank = opaque sortable row data
 :members   {container-id → #{unit-id}}}
```

- All six keys always present; empty maps lawful. Rows are flat maps in **document vocabulary** (the fields the builders' data-contexts need — content, hashes, actor, times, geometry), never slot/render vocabulary: the client derives its own delta behind its own door (minted-diffs law).
- **Containers are units.** A container's row carries its transform/geometry; `:members` lists its children. A container move = one row change (proportionality: representation chosen so affected sets stay small).
- **Tombstone:** a deleted unit's `:units` entry is `:tombstone` and it leaves `:members` in the SAME value (atomic per act). Server keeps it in since-reads until every currently-subscribed lens's high-water passes its act; then since-reads stop carrying it (§8-S5).
- **Watermark / catch-up:** client presents its watermark on (re)connect; server replays since-W as ordinary feed values through the SAME path. Membership grow = a birth-shaped feed value for that container (full live units, current revisions) on the stream — birth rides the live path.
- **Below the compaction floor:** a lens whose watermark predates what since-reads can serve receives the ORACLE's full pull **as one birth-shaped feed value through the same apply path**. That is the oracle's cap-overflow duty discharging without a second reconcile — one-path recovery holds structurally.
- **The oracle and its two duties** (demotion WITH duties transferred, growth law): `face_projection/serve` (`face_projection.clj:1910`) stays alive as (i) the committed-echo cross-check (INV-19 — today's channel is the epoch bump at `server_jetty.clj:1558-1561`; under this package the check becomes feed-vs-page equality, golden G2) and (ii) the cap-overflow source above. The `!ingest-epoch-atom` is NOT deleted (other consumers exist — `episode.clj:946`, `ingest_watchers.clj`); it simply has no reader on the render road.

## 5 · Entry points — exact

**Server — one new namespace** `app.server.feed` (`src/app/server/feed.clj`):
- pure feed assembly (rows/decisions → the §4 value) and since-assembly over the existing cursor reader `read-revision-history` (`object_container/runtime.clj:363-370`); RevisionRow fields at `object_container.clj:109-111` (`event-id`, `order-key` already there). Birth/current-row assembly MAY reuse the projection registry's data acquisition (`face_projection.clj`) reshaped flat — golden G2 (feed-vs-oracle equality) is the fence whichever reuse level the implementer picks;
- per-lens subscription state {containers, high-water, writer} + in-JVM publish (pin §10);
- `GET /api/feed/stream` (SSE; lens-id + containers + watermark as query params) and `POST /api/feed/demand` (grow/drop) — two new uri branches in `wrap-file-api` (`server_jetty.clj:1387-1391`; zero existing GET branches to collide with). The SSE mechanics reuse `write-event!` (`server_jetty.clj:316-320`) and the proven `StreamableResponseBody` pattern (`run-episode-turn`, :439-473).
- **Thin hooks, post-accept, one call each:** the utterance accept site (`server_jetty.clj:1558-1561`) · the geometry-settle site (`episode/settle-geometry!` return at `server_jetty.clj:1585` — settles currently notify NOTHING; this is the missing bump) · the block-edit lane (`block-edit/submit-block-edit!`, `server_jetty.clj:786`, `block_edit.clj:7` — "the ONE server edit"). Each hook publishes one feed value for the accepted act.

**Shared — one new namespace** `app.shared.feed-shape` (`src/app/shared/feed_shape.cljc`): the §4 shape as data + validators; consumed by BOTH server assembly and client apply (P1 shared-gate compliant). No logic beyond validation.

**Client host — new namespaces under** `src/app/client/workspace/host/`:
- `courier.cljs` — opens the SSE GET, yields a Missionary flow of parsed feed values; owns the lens-id + watermark session state and reconnect-with-watermark. NO translation logic (MUST-NOT 6).
- `feed_apply.cljc` — PURE: `(apply-feed-value store compiled-assemblies value) → store'` — per unit `build-face-tree` (`scene_store.cljc:493-500`) + `upsert-slot` (`:169-176`) / `remove-slot` (`:203-206`); order patches touch only affected slots' rank fields (T2). Custody of rank: the feed's `:order` rank maps into the ordering opts `upsert-slot`/`build-slot` already take (`sibling-rank`/`stack-path` → the `:ordered` tape's entry key) — no new ordering machinery. JVM-tested; this is where the store-level tripwire halves live.
- `apply_edge.cljs` — the ONE consumer edge: courier flow → coalescing-microtask `m/reduce` (T4; the deleted host's pattern, `adc30c9^:runtime/render.cljs:183,220`) → ONE `swap!` per feed value (one swap = one generation). Dev counters (§8).
- `demand.cljs` — v1: subscribe-on-open with the lens's declared containers; membership changes are EDGE-TRIGGERED posts (never per-frame). A3 adds the visibility broker.
- `frame.cljs` — the crank: first `requestAnimationFrame` in kept src; single `m/sample` of the existing views per vsync (`<store-frame`/`<effective`/`<frame-registry`, `scene_runtime.cljs:445,458,465`); camera atom owned here, deref'd at the sink only; skip-if-unchanged; uploads then **`draw-frame!` (`renderer.cljs:3678`) — its first caller** (receipt §11.P-A: verifier hand-rolls its own encode and never calls it; no third encode path may be born — reuse the verifier's system-construction for GPU init, reference `verifier.cljs`, then drive the real `draw-frame!` with `frame-inputs/declared-inputs`+`changed-families`, `frame_inputs.cljc:93,134`). The host draws through `draw-frame!` only — it never grows its own hand-rolled encode; the verifier's private pass stays verifier-local.
- `main.cljs` — browser entry + dev page (new shadow-cljs build alongside `:render-verifier`); pan/zoom pointer input → camera atom (T10: in-flight client-side, only settle commits through `/api/episode/geometry`); dev keybindings that fire block-birth (`/api/episode/block-birth`) and one block edit (the `submit-block-edit!` lane) — the A1 acts.

**Thin hooks only** into existing files: `scene_runtime.cljs`, `scene_store.cljc`, `buffer_pool.cljs`, `renderer.cljs`, `server_jetty.clj` are consumed at the named fns; no rewrites inside them. New code lives in its own namespaces (work-package law).

## 6 · The courier — a stage, with its replacement trigger named

SSE-first (POSITION held, §8): the kept door grown one long-lived GET, Missionary on the client. The wire's CONTENT (§4) is frozen and courier-agnostic; each side's courier is ONE file (`feed.clj`'s stream handler / `courier.cljs`), so the swap is a file, not a rewrite — that is the entrenchment fence, enforced by MUST-NOT 6.

**Replacement trigger:** the transfer bench — PROBLEM-SPACE probe 1: four merge algebras × `e/diff-by` cross-wire losslessness × two-edits-one-tick (zero live `diff-by` uses exist; the bet is untested in BOTH directions). It runs at the host probe (road 4, the Block built both ways) at the latest, earlier if wire behavior disputes arise. The bench decides the COURIER; the Block probe decides the HOST; neither decides the other. The courier closes by the bench, never by waiting.

Transport minutiae (heartbeat comments, reconnect backoff, EventSource vs fetch-stream) are courier-internal and invisible to the feed (§10 heartbeat pin).

## 7 · Demand v1

Open: the lens declares its containers on stream-open (A1: the one conversation container). Change: `POST /api/feed/demand {lens-id add #{cid} drop #{cid}}`; the server answers a grow with a birth-shaped feed value ON the stream. A3 wires the source: the frame edge derives visible-container facts (CPU walk over store + camera — never GPU readback), hysteresis-banded, diffed, posted only on membership CHANGE. A frame-cadence writer through a door is lawful (the tween clause); per-frame posts are not (edge-trigger pin, §10). Residency ≠ existence: a dropped container's slots leave the store; re-grow re-delivers via the birth/catch-up road. Membership grain only — never position windows (L16).

## 8 · Scenarios — 3–5 decisive, with receipts and the wrong-build each receipt kills

Instrumentation (dev-flagged counters, part of A1): `feed-values-applied` · `upsert-calls` · `remove-calls` · `store-diff-keys` per value (key-identity diff) · `store-frame-execs` (revived from the deleted host) · `demand-posts` · `oracle-pulls`; server: per-lens `events-sent`/`bytes-sent`; `changed-families` count from the frame receipt.

- **S1 · one edit → one slot.** Lenses A and B subscribed to container C, unit U resident. A fires one accepted edit on U (`submit-block-edit!` lane): exactly ONE feed value (`:gen` = that act) reaches A and B; each applies in one swap; `store-diff-keys` = {U}; `upsert-calls` +1; next frame `changed-families` = 1; unchanged slots stay `identical?` (T5). *Wrong build killed:* an apply that rebuilds every face tree but diffs by value-equality would still report 1 — hence the receipt is key-IDENTITY diff plus the upsert-call counter, not value comparison.
- **S2 · a pan sends nothing.** A pans/zooms across C, membership stable: `feed-values-applied` 0 · `demand-posts` 0 · store swaps 0 · `store-frame-execs` 0 · zero wire bytes both directions (SSE keepalive comments excluded, §10 pin); the frame redraws from the camera at the sink alone. *Wrong build killed:* timer-batched sends showing a quiet window — the receipt spans the gesture plus a settle window, and MUST-NOT 1 bans the timer upstream anyway. A pan that CHANGES visible membership is A3 behavior (one demand post + birth value) — correct, not a violation.
- **S3 · a second lens sees a settle.** A drags C and settles through `/api/episode/geometry`: accept → notify (the hook this package adds at `server_jetty.clj:1585`) → ONE feed value with C's row; B patches C's slot only; `oracle-pulls` 0 during the scenario; A's echo correlates by `:gen`/`:acts` and reconciles to the identical value B holds. *Golden G1:* A's slot for C equals B's slot for C, as values. *Wrong build killed:* B "seeing it" via any page pull — the oracle-pulls counter is the receipt. This scenario IS the write→render→truth-reconciliation→clear trace for the wire (falsification-protocol golden rule).
- **S4 · catch-up from a watermark.** B disconnects at watermark W; k acts land on C; B reconnects presenting W: the server replays since-W as feed values on the stream; B applies through the SAME `apply-feed-value`; end store equals a fresh oracle projection of C (*golden G2* — also the transferred INV-19 check). Structural receipt: exactly ONE call-site of `apply-feed-value` in the host; the apply never constructs a fresh store except empty-boot. *Wrong build killed:* replay into a fresh store swapped wholesale — a second reconcile disguised.
- **S5 · a tombstone retires.** U is deleted while B holds W < that act: B's catch-up carries `U → :tombstone` (and U gone from `:members` in the same value); B `remove-slot`s U. Server-side: since-reads still carry the tombstone until every currently-subscribed lens's high-water passes the delete act, then stop; a lens below the compaction floor takes the oracle-as-birth-value road instead (§4). *Wrong build killed:* dropping tombstones right after live delivery — the receipt reads since(W) AFTER live delivery to other lenses and still finds the tombstone.

A1 freezes S1–S3 as its three tripwires (§8's "three tripwires, no more"); S4–S5 freeze at A2.

## 9 · MUST-NOTs — real, disjoint; each names the scenario that fails under its violation

1. **No execution clock above derivation.** Clocks drive writers through doors or are read at the sink; never inside feed, store, or derivation. (Violated ⇒ S2 fails.)
2. **No page as truth.** The full pull exists only as oracle with its two named duties (§4); no code path applies a page into the store outside the birth-shaped-feed-value road. (⇒ S4 fails.)
3. **No mirror atoms.** No `add-watch` copying one atom into another anywhere in the host; derived views are `m/latest`/signals over the one store (T3/T9). (⇒ S3 skews.)
4. **Order never travels as position.** Ranks as row data; reorders as `:change` on rank fields (T2); the client never derives order authority from arrival or array index. (⇒ S1 breaks under concurrent inserts.)
5. **Material truth never originates in the store.** `apply-feed-value` is the ONLY material writer; camera and in-flight gestures are the exempt session truths, and only SETTLE commits (T10). (⇒ S3 fails — B never learns.)
6. **The courier never leaks into the content.** `feed-shape`/`feed_apply` import no courier namespace; couriers import them. (⇒ the transfer bench arrives at a rewrite instead of a swap.)
7. **`env.clj` is never read or touched.**

## 10 · Pins and written forks (pin-or-fork lens output; each fork is ONE question, never a stop)

- **PIN — containers are units** (§4). Contest with served-page shapes if a container's geometry proves to live elsewhere; the feed shape stands, the mapping moves.
- **PIN — rank v1 = birth order.** Conversation blocks order by their birth act's `order-key` (time-prefixed, `object_container.clj:243`); reorder verbs later mint rank `:change`s (T2). FORK if a container class carries a different server-side order source: name it, one question.
- **PIN + default — correlation.** The accepted decision returned by the write lanes (e.g. `submit-block-edit!` returns the durable decision; `settle-geometry!` returns `{:status :address :decision}`) carries the accepted act's identity; feed `:acts` carries event-ids; the echo matches on event-id. DEFAULT if a lane's decision omits event-id: the client-minted `request-id`/`idempotency-key` (already in the edit env) is carried into the feed row for that act. Implementer picks per lane; both roads are written.
- **PIN — one JVM.** Notify = in-process publish from the post-accept hooks to subscribed stream writers. Multi-server fan-out is a refusal (§2).
- **PIN — heartbeats are transport.** SSE keepalive comments are not feed values; S2's counters count feed values and demand posts.
- **PIN — demand is edge-triggered.** Membership diffs post on change only; never per-frame. A3's hysteresis band is a handed-down default (enter/exit thresholds differ by a margin; exact margin = implementer default + note), not a fork.
- **PIN — v1 compaction floor = genesis.** Revision history replays in full today, so the below-floor road (§4) is dormant; it is written NOW so one-path recovery cannot break when retention/compaction arrives later.
- **PIN — the middle is pure.** Feed assembly (server) and `apply-feed-value` (client) are pure functions over values, JVM-tested; couriers and hooks are thin. (Extends §1.8 of the two-arrows page to the wire; it is why the tripwires can be JVM tests.)
- **PIN — A1's acts are dev-affordances** (birth + one edit via keybindings), not an editor. The editor arrives under the editing-rewire package.

## 11 · Probe receipts banked at this cut (investigation fence: structure asserted; magnitudes carried, not claimed)

- **P-A — the verifier build never reaches `draw-frame!`: CONFIRMED** (predicted). Zero runtime callers in src/test/dev; the verifier (`shadow-cljs.edn` `:render-verifier`, `verifier.cljs:5667-5696`) calls ~12 `renderer/*` primitives and hand-rolls its own encode pass (`verifier.cljs:125-171`); no `requestAnimationFrame`/`>raf` anywhere in kept src. **Consequence: A1 gives `draw-frame!` its first caller; no live frame loop exists today.**
- **P-B — pick structure: CONFIRMED, sharpened.** `scene-store/pick` (`scene_store.cljc:406-424`) → `pick-reverse` (`scene_tape.cljc:757-768`): reverse z-order, one entry per slot, early-exit on hit, O(1) visibility/`:pick :none` skip, AABB pruning only INSIDE each visited slot's tree (`rect_tree.cljc:669-693`). **Per-slot pruning, NO cross-slot spatial index — a global miss touches every pickable slot once.** The 4.5ms/189-blocks figure stays a dev-build quote, not a receipt.
- **P-C — derive-store-frame structure: CONFIRMED strongly.** `scene_store.cljc:321-378`: 14 keys, each a fresh full pass over all slots per swap; zero incremental path; fires on every store watch tick via the one `m/latest`.
- **Magnitude probes: BLOCKED in the cutting container** (network policy denies every Maven host; no JVM deps resolve — receipt in session log). They ride into **A1's close bundle**, pre-registered: (i) `store-frame-execs`-adjacent timing of `derive-store-frame` at 10/200/2000 synthetic slots — prediction: low-ms at 200, ~10× at 2000, JVM and browser both; (ii) `performance.now()` around `scene-store/pick` hit+miss at the same sizes, headful vulkan (§11 of the two-arrows page names this probe) — prediction: linear, well under 5ms at 2000. Fixture style: `test/app/client/workspace/scene_store_test.clj:24-311`. Adapter/device identity stated first in any number reported (work-package field note).
- **Two doc corrections surfaced by the gather** (for the two-arrows page's own fresh-eyes round; repaired in place this session): builder registry = 36, not 16; `ARCHITECTURE.md:150`'s INV-19 anchor is stale (real site `server_jetty.clj:1559-1560`).

## 12 · The atom ladder

- **A1 — one conversation container, keyed, end to end** (handed to Codex now; starter appendix A). The §5 namespaces at minimum viable depth: feed for ONE container over the SSE stream · host (courier · feed_apply · apply edge · demand-on-open · crank · entry) · store patched by key · `draw-frame!` cranked with changed-families · block-birth + one block edit fired by dev keybindings · the settle notify hook · the echo applying. Tripwires TW1–TW3 = S1–S3; goldens G1 (A/B slot equality post-settle) + G2 (birth batch vs oracle projection equality, JVM). Close bundle carries the two magnitude probes (§11). **Retires on landing:** the page-pull-as-truth road (oracle-with-duties stands), the epoch integer as the render road's echo channel (atom stays for its other consumers), and the mirror pattern (the host is born mirror-free — MUST-NOT 3).
- **A2 — catch-up + tombstones.** Reconnect-with-watermark · coalesced since-values · tombstone delivery + retirement + compaction floor via oracle-as-birth-value. Freezes S4–S5.
- **A3 — the visibility broker.** Frame-edge visible-container facts → hysteresis → edge-triggered grow/drop → birth-on-grow; residency ≠ existence receipts; S2's membership-change half armed.
- **LATER (named, this package's borders):** per-key store views + keyed pool bridge (the store-contract slice) · editing-rewire under the new host · transfer bench → courier verdict · host probe (road 4) · multi-server fan-out.

---

*Appendices A (A1 implementer starter) and B (falsification-round prompt) are handoff artifacts, not contract terms.*

## Appendix A — A1 starter prompt (paste to Codex)

> PREFLIGHT: set permission mode / remote-control / MCP toggles BEFORE this first prompt (never mid-session — prefix law).
> LANE: Codex builds, contract-local. Acceptance = Sid's word; this atom's one remaining Sid-touch is acceptance. After acceptance, contract-local status only.
> BOOT (≈72 KB, this order): `docs/keyed-wire/CONTRACT.md` WHOLE — binding, read primary (~34 KB) · `docs/below-the-waist/engine-two-arrows.md` §3 (:125-196) + §8 (:399-492) — the map (~11 KB) · `docs/scene-substrate/CONTRACT.md` §7 (:173-212) traps (~2 KB) · `.claude/skills/electric-docs/SKILL.md` — verified Missionary/Electric laws for the apply edge + crank (~25 KB).
> ATOM A1 — one conversation container, keyed, end to end: CONTRACT §12.A1; entry points §5; feed shape §4; pins §10; MUST-NOTs §9. OWNER: Codex. SOURCE: the contract. DONE WHEN: TW1–TW3 green + G1–G2 green + the two §11 magnitude probes run with predictions checked + close receipt written.
> Tripwires frozen as written in §8 (TW1=S1 · TW2=S2 · TW3=S3): store-level halves as JVM tests beside `test/app/client/workspace/scene_store_test.clj` fixtures; frame-level halves as scripted browser receipts over the §8 dev counters (state adapter/device identity first in any number).
> Method: gathering rides bounded read-only subagents (one wave, ≤8K filtered outputs); code read by seam, never whole files. Build the WHOLE atom straight through — implementation adversarial check on the real seam, fix before source freeze. Ambiguity = strongest default + a note; a genuine fork = ONE question, keep moving on unblocked work.
> Close: focused suite only (foreign failures are board debt) · changed files from `git diff --name-only`, never memory · terminal `docs/keyed-wire/NOW.md` ≤15 lines · flip the board pointer. After NOW: no source/test/tooling edits. Commit by concern on the working branch; pushing/merging stays Sid's.

## Appendix B — falsification-round prompt (paste to ONE fresh Codex-class session; Sid's hand, touch #1)

> MODE: REVIEW (falsification) · ALTITUDE: contract · AUTHORITY: candidate
>
> You are the ONE bounded fresh-eyes falsification round for `docs/keyed-wire/CONTRACT.md` (work-package law: claim→source, evidence-cited, NO verdict authority, no recut; the author repairs). Load `.claude/skills/review-cross-model-artifact` if present. Read the contract PRIMARY, whole. Do not rewrite it; do not solve from scratch; findings only.
>
> SOURCE SCOPE (verify against these; unverified reasoning must be labeled inference): law — `docs/decisions.md` :450-628 · `docs/scene-substrate/CONTRACT.md` §7 · `docs/electric-native/DIRECTION.md` 1c riders · `docs/electric-native/PROBLEM-SPACE.md` Forks + Probes owed · `docs/below-the-waist/engine-two-arrows.md` §3 §8 §11. Code, at the contract's cited anchors (read by seam, never whole files): `scene_store.cljc` · `scene_tape.cljc` · `rect_tree.cljc` · `frame_inputs.cljc` · `buffer_pool.cljs` · `renderer.cljs` (draw-frame! window) · `verifier.cljs` (init + encode windows) · `face_primitives.cljc` :1317-1365 · `face_assembly.cljc` · `server_jetty.clj` (:316-320 · :439-473 · :786 · :1387-1636 · :1558-1561 · :1585) · `block_edit.clj` · `episode.clj` :355-365 · `object_container.clj` :109-111 :243-245 · `object_container/runtime.clj` :363-370 · `face_projection.clj` :1910-1944.
>
> PRESERVE (candidate decisions — attack only with a receipt that invalidates): courier SSE-first as a STAGE with the transfer bench as its named replacement trigger (Sid-held position; closes by the bench, never by this round) · the refusals' LATER routing · the atom ladder grain · the frozen feed shape's existence (its FIELDS are contestable with evidence).
>
> DO NOT REOPEN: faces-are-data (ruled) · host-probe deferral to road 4 · road-order inversion (§8 position held) · dead-ceremony bans · anything decisions.md marks settled.
>
> CHECK SPECIFICALLY (the four lenses, inverted): 1 chain-of-custody — any value a consumer (tests included) needs whose producer→consumer road is unwritten or wrong at the cited anchor (rank→ordering opts · correlation per write lane · birth assembly · watermark across reconnect · the crank's draw-frame! kwargs). 2 receipt-gaming — for each S1–S5, a wrong build that passes the receipts AS WRITTEN. 3 pin-or-fork — load-bearing referents with two readings; policy adjectives with no pinned algorithm; unwritten forks. 4 obligation cross-check — MUST-NOT × entry-point/scenario forced violations; contradictions with the render-seam law read primary. 5 anchor validity — every file:line cited (two already corrected upstream: 36 builders; INV-19 at `server_jetty.clj:1558-1561`).
>
> OUTPUT — findings only; you carry NO verdict authority (acceptance is Sid's). Numbered, each: QUOTE (the contract sentence) → EVIDENCE (file:line) → BREAKS (which scenario/law fails) → SMALLEST REPAIR. Tag each DECISION-CHANGING or MINOR. End with the decision-changing count (≤1 retires this round class per the 2026-08-06 amendment). No rewrite, no recut, no alternative contract.
