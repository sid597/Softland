# The Two Arrows — wire · host · demand · CONTRACT

**Cutter: Claude Fable 5 (`claude-fable-5`) · effort max · cut 2026-08-23.**
Law read PRIMARY for this cut: `docs/decisions.md` "The render seam" (:450–628) ·
`docs/below-the-waist/engine-two-arrows.md` §1 §3 §8 §11 · `docs/electric-native/DIRECTION.md` ·
`docs/electric-native/PROBLEM-SPACE.md` "Forks" + "Probes owed" · `docs/electric-native/RECON.md` §11 ·
`docs/scene-substrate/CONTRACT.md` §7 · work-package skill. Ground truth gathered 2026-08-23 by four
bounded read-only gatherers (entry points, routes, PStates, two JVM benches) and spot-checked by the
cutter; anchors are `file:line` in the working tree at `a24df5c`; deleted-host anchors are `adc30c9^:path:line`.
Status: **CUT — atom 1 handed to Codex (starter at §13).** Sid's touches: this header (now) · atom
acceptance (later). Nothing else waits on him; contest anything here and keep building.

In Sid's words this package is "the correct Electric": **↓ changes travel by id — one edit moves
one thing, instantly · ↑ the view tells the server what it needs — pay only for what you look at.**

---

## 0 · In one breath

1. The engine is a machine with no crank: store → tape → GPU is kept; nothing feeds the store, nothing drives a frame, no browser page exists.
2. Rama already holds every block as a keyed row (unit-id, event-id, an order-key that IS its place in the conversation, a settled position cell). The scene store already takes slots by key. The whole gap is the wire between two keyed ends, in both directions.
3. This package builds that wire and the client host around it. Down: a long-lived stream per lens that delivers **only the units that changed**, each under the act that changed it. Up: the lens tells the server **which conversations it is looking at**; the stream is scoped by that.
4. The client applies each delivery by key into the scene store — one slot per unit, one container per conversation — and a single once-per-frame sample draws only the families whose inputs moved.
5. The whole-page read (`face_projection/serve`) is never rendered from again. It stays alive as the **oracle**: every unit the feed sends must equal the page's entry for that unit.
6. The courier is SSE + Missionary today; the feed's *content* is frozen courier-agnostic so the courier is a one-file swap. Electric's keyed machinery gets its fair test at the named bench, not by waiting.
7. Three atoms: **one conversation, keyed, end to end** (three tripwires) → **demand that moves** (two conversations, grow/shrink) → **the wire's memory** (a real watermark, tombstone retirement, the oracle's duties in code).
8. What this package does NOT do — per-key store views, Electric, faces-as-data authoring, editing beyond one write route, collaboration semantics — is named below with where each goes.

---

## 1 · Scope

**The package is** the middle between the two keyed ends, both directions, for conversation containers and their block units:

- **the wire** — server-side, per lens: a long-lived SSE GET stream of *deliveries*; each delivery carries only changed units by id (row or tombstone) under one generation (= the accepted act); boot is a delivery like any other (the full membership, gen `:boot`); catch-up later rides the same shape;
- **the host** — client-side, five small namespaces: courier (stream → Missionary flow) · apply (pure per-key application into the scene store + its one reduce edge) · demand (visibility → container set → one idempotent POST) · frame (device/fonts/pipelines stand-up, the rAF sample, the per-slot text pass, the `draw-frame!` call) · main (boot); plus the block face as data;
- **demand** — the lens publishes the set of conversation containers it can see (membership, never position windows — L16); the server scopes the lens's feed by it; grow and shrink.

**The package is not** the store-contract slice (per-key views, `derive-store-frame` demotion, keyed pools), not the Block gate, not editing, not collaboration semantics, not faces-as-data authoring. Each is a named refusal (§2) with its routing.

**Atoms** (§11): 1 · one conversation container, keyed, end to end · 2 · demand that moves · 3 · the wire's memory. Package close = the seam courtroom once (full suite · the felt pass: Sid edits/moves a block in one browser window and sees it land in another).

---

## 2 · Refusals — named, one line each, each routed (never a void)

- **No per-key store views, no `derive-store-frame` demotion, no keyed GPU pool.** Hop 5 stays the O(N) lane rebuild, with a numbered trigger (§10 receipt (c): 16.7 ms at 2000 slots, JVM). The host adds ONE equality-gated identity-preserving gate at the frame edge (§6 row F3) so family dirtiness is honest. → the store-contract slice, its own contract; trigger = the receipt + a lens whose resident slots approach ~1000.
- **No Electric in `deps.edn` — now or later (2026-08-30: Hyperfiddle left Clojure; v3 is a frozen proprietary alpha).** Courier = SSE + Missionary, owned. → the transfer bench (PROBLEM-SPACE probe 1) still runs, as a test of OUR feed's algebra; the courier stays one file each side (§9.1).
- **No faces-as-data authoring / interpreter work.** The block face is ONE data file authored by this package (`resources/public/faces/block-face.edn`, 36-primitive registry as-is) — a face, not a face system. → road 3 (the spec sitting) · the Block probe.
- **No editor.** Exactly one write route round-trip (block text edit, §5 S-4) so S1 can exist; no caret, no T2 wiring, no halo, no selection/hover. → the Block probe · the editing lane.
- **No collaboration semantics.** "A second lens sees it" is delivery, not merge; no conflict UI, no CRDT, no presence. → Sid's collaboration-soul fork (PROBLEM-SPACE "Sid's alone").
- **No position windows, no per-unit demand.** Demand = container sets only. → never (L16).
- **No eviction / warm-cold residency.** Shrink = stop receiving; slots stay resident and stale until the lens grows back (then the equality-gated apply refreshes what changed). → the applier's residency business, after the Block.
- **No undo, no reified op-log.** → ledger form: undo + multiplayer decide (render seam "Open").
- **No Rama topology change in atoms 1–2.** The producer reads existing PStates; notice is in-JVM. → atom 3 adds the one change-log PState (§9.3) under the rama skill.
- **No shaping work.** The kept text pipeline is consumed as-is (per-slot identity fast path, T12). Typing lag, if felt, → road 1a's contract (`docs/shaping-correction/CONTRACT.md`), fence: receipt first.
- **No image/path/3D/connector material in the slice.** Rect + text families; the other families ride the same store/tape untouched. → the engine relay (ink · live components · Region3D).
- **No turn chrome / grouping.** Units are blocks; speaker badges and indent rails of the outline face are not rebuilt per unit. → the spec sitting.

---

## 3 · Laws — pointers, never restatements

- `docs/decisions.md` "The render seam" :450–628 — the binding law: recompute proportional to change at every layer · no execution clock as an ancestor of derivation · ownership not sampling (one swap = one generation; stamps at async joints) · the fenced incremental view + growth law (batch demoted to oracle, never deleted) · **the review rule: no derivation in the seam without its five things** (§6 here) · order is row data · minted diffs are values in one namespace · the scene store is a projection, never a second truth-owner.
- `docs/decisions.md` "One render substrate" :302.
- `docs/scene-substrate/CONTRACT.md` §7 traps T1–T12 — T1 (descriptors, no closures in slots) · T3 (one `m/latest` for co-varying values) · T4 (effects at the reduce edge) · T5 (`:ops` at upsert; unchanged slots `identical?`) · T9 (one watch, never per-slot `m/ap` forks) · T10 (gesture ≠ assertion; only SETTLE commits) · T12 (one text pipeline).
- `docs/electric-native/DIRECTION.md` road 1c court riders — flat units-by-id · generation = the accepted act · one-path recovery · tombstones linger until every lens passes · oracle demotion WITH duties transferred (committed-echo cross-check INV-19 · cap-overflow reconcile) · container-grain notification is a lawful rung.
- `docs/below-the-waist/engine-two-arrows.md` §3 (the ten hops as built) · §8 (the fill; positions held) · §11 (receipts, updated this cut).
- `.claude/skills/electric-docs/SKILL.md` §2 — the verified Missionary laws the host's flows obey.
- `docs/electric-native/PROBLEM-SPACE.md` "What falsifies this frame" — blocks growing into documents · a write-dominated swarm · making's bottleneck being authoring-iteration.

Which scenario FAILS under each law's violation: clock-as-ancestor → S2 (a pan would re-derive) · page-as-truth → S1/S3 (a page re-pull would move every slot) · mirror atoms → S1 (a second copy = a second generation authority, tearing) · order-as-position → S5 + births (a positional index shifts every later unit) · T5 → S1 (a non-identical unchanged slot re-uploads) · T10 → S2 (a mousemove would commit).

---

## 4 · The feed — the frozen content, courier-agnostic (`app.shared.feed`, `.cljc`)

One namespace, shared by server and client, JVM-tested. Plain maps only — **no records on the wire** (the server strips defrecords the way `record-free-edn` does, `server_jetty.clj` matter-room arms). EDN text in SSE `data:`; SSE `id:` carries the watermark when one exists (atom 3); until then no `id:` line.

```clojure
;; Delivery — one SSE event
{:feed/v     1
 :lens       "<lens-id>"                    ; client-minted at boot (random uuid string)
 :gen        "<event-id>" | :boot           ; the accepted act this delivery folds (latest if coalesced)
 :units      {unit-id → UnitRow | :tombstone}   ; ONLY units whose row changed vs. the lens's last-sent shadow
 :watermark  nil}                           ; atom 3: the change-log order-key; v1 nil = "reconnect means boot"

;; UnitRow — the unit's current truth, as the page would carry it + its place + its position
{:unit-id               "<du:…>"
 :container-id          "<oc:chat-conversation:chat:…>"   ; the conversation container
 :document-container-id "<…>"                 ; needed to EDIT the unit (§5 S-4)
 :object-key            "chat:<sha>"          ; needed to EDIT the unit and to derive event-ids
 :order-key             "<string>"            ; the unit's place in its container — ROW DATA, sorts lexically
 :kind                  <keyword|string>      ; as the page's block :kind
 :text                  "<content>"           ; as the page's block :text
 :revision-id           "<…>" | nil
 :event-id              "evt:<object-key>:<request-id>"   ; the act that last touched CONTENT (correlation key)
 :geometry              {:x <num> :y <num>} | nil        ; the settled cell (geo:unit:<sha8>), container-local
 :geometry-event-id     "<evt:…>" | nil}                  ; the settle act that last moved it

;; Demand — client → server, whole set, idempotent, sent only when the set changes
{:lens "<lens-id>" :containers #{"<container-id>" …}}
```

Pins, each exact:
- **generation = the accepted act.** `:gen` is the `event-id` of the act whose notice woke the producer; if two acts coalesce into one delivery, `:gen` is the later one and each UnitRow's own `:event-id` / `:geometry-event-id` carries its act — **correlation is per unit by event-id, never by delivery `:gen`** (this is the two-edits-one-tick answer at the format level).
- **membership and order are derived client-side by key** from rows (`:container-id`, `:order-key`); no index of positions or member lists crosses the wire. (§8's `:order`/`:members` indexes are client-maintained views, not wire content.)
- **tombstone** = the value `:tombstone` under the unit-id; it means "remove this slot". It is sent to every lens whose shadow still holds the unit; a lens that never held it never hears of it.
- **boot** = one delivery with `:gen :boot` and every member of every container in the lens's set, through the same apply edge — there is no second "initial load" shape.
- **unpositioned units** (`:geometry nil`) get a default position by a pure client rule (§9.4); positions never default server-side in this package.
- **`:feed/v`** bumps on any shape change; client refuses a version it doesn't know (logs, stays dark, never guesses).

---

## 5 · Entry points — exact

### Server (Clojure, `src/app/server/`)

| What | Where | Kind |
| --- | --- | --- |
| **S-1 the feed namespace** `app.server.feed` | NEW `src/app/server/feed.clj` | own namespace |
| · lens registry | `(defonce !lenses (atom {lens-id → {:containers #{C} :shadow {unit-id → UnitRow} :queue <LinkedBlockingQueue> :open? bool}}))` | in-JVM; atom 3 makes the watermark durable |
| · `unit-rows` | `(defn unit-rows [ctx container-id] → {unit-id → UnitRow})` — the per-unit shaper: the same reads the conversation page makes, per unit: projection range read (`read-transcript-conversation-projection`, `object_container/runtime.clj`), block text/kind resolved the way `face_projection/shape-conversation` (:193–211) + `block_distiller/river-page` (:1397) resolve them, geometry cells (`geo:unit:<sha8>`, `episode.clj:268–290`) joined, `:deleted? true` cells → the unit is absent (its tombstone is minted by the diff). DONE WHEN the golden holds: for every unit, `(unit-rows ctx C)` ⊇ the page's block entry for that unit on `:kind :text :revision-id`, and geometry equals `(:conversation/geometry page)` — the oracle fence (§6 row W2). | pure over `ctx` reads |
| · `deliveries-for` | `(defn deliveries-for [lens-state fresh-rows gen] → [delivery lens-state'])` — diff `fresh-rows` against `(:shadow lens-state)` by key: changed/new → row, missing → `:tombstone`; updates the shadow. PURE, `.cljc`-testable (put it in `app.shared.feed` if it needs no server reads — it doesn't). | pure |
| · `notify!` | `(defn notify! [container-id gen])` — offers `{:container-id C :gen gen}` to every open lens whose set contains C. The ONLY cross-route hook. | thin |
| · the producer loop | runs ON the connection's Jetty thread inside `write-body-to-stream`: `(loop [] (let [n (.poll queue 15 TimeUnit/SECONDS)] (if (nil? n) (write-comment! ": ping") (write-event! (delivery…)))) …)`; first iteration = boot (shadow empty → the full membership); `IOException` on write/flush ⇒ close the lens, drop it from `!lenses`. Heartbeat every 15 s (the one timer; it drives a WRITER, never a derivation). | thin reuse of the episode door |
| **S-2 the stream arm** `GET /api/feed?lens=<id>&containers=<C1,C2,…>` | thin hook: one `cond` arm in `wrap-file-api` (`server_jetty.clj:1387–1604`) returning the SAME `reify ring-protocols/StreamableResponseBody` shape + headers as the episode door (`:468–478`), reusing `write-event!` (`:316`); registers the lens, runs the producer loop. | thin hook |
| **S-3 the demand arm** `POST /api/feed/demand` `{:lens :containers}` | thin hook: one arm; replaces the lens's container set; for newly added containers the producer will boot them on its next loop (offer a `{:container-id C :gen :boot}` notice per added C); removed containers: nothing is sent; their units are DROPPED from the shadow (so re-adding sends them fresh — which the client's equality gate absorbs). 200 `{:ok true}`. | thin hook |
| **S-4 the edit arm** `POST /api/block/edit` `{:unit-id :object-key :document-container-id :content-text :request-id :edit-client-id :time-ms}` | thin hook: one arm → `block-edit/submit-block-edit!` (`src/app/server/block_edit.clj:7`, the ONE server edit entry; today its only caller is `matter-room-refresh!`, `server_jetty.clj:769`); `edit-seq` = server-computed next seq per (unit, edit-client-id) exactly as `matter-room-next-edit-seq` (`:744`) does; target `{:target/kind :derived-unit :target/id unit-id}`. Returns the result map (`:accepted? :request-id :object-key :target-id …`). **No wire route edits block text today — the ten POST routes are matter-room/relation/facet/episode arms; this arm is the first.** | thin hook |
| **S-5 notify hooks** (three, one line each, after accept) | `server_jetty.clj:1560` (block-birth accept; beside the epoch bump) · `:1591–1594` (geometry settle accept — ALL accepted settles notify, not only `:deleted?`) · the new S-4 arm after `(:accepted? result)`. `gen` = the act's event-id: birth/edit `(str "evt:" object-key ":" request-id)` (`object_container.clj:238` `event-id-for-request`), settle: the cell's event-id (`episode.clj:277`). | thin hooks |
| **S-6 the page as oracle** | `face_projection/serve` (`face_projection.clj:1910`) — unchanged, test-reachable; the feed's golden calls it in-process. No GET route is added for it in atom 1 (none exists today). | untouched |
| **S-7 static page** | one line in the middleware stack (`server_jetty.clj:1606–1613`): `ring.middleware.resource/wrap-resource "public"` ahead of the 404 catch-all, so `/host/index.html` + `/host/js/main.js` serve same-origin (EventSource needs same-origin or CORS; pick same-origin). | thin hook |

### Client (ClojureScript, NEW `src/app/client/host/`)

| What | Where | Kind |
| --- | --- | --- |
| **C-1 courier** `app.client.host.courier` | NEW `courier.cljs`: `(defn <deliveries [url] …)` — `m/observe` over an `EventSource`'s `message` events → `cljs.reader/read-string` → delivery maps; reconnect = the browser's; on `open` after a drop the client treats the next delivery as boot (it is). ONE file = the courier swap point. | own ns |
| **C-2 apply** `app.client.host.apply` | NEW `apply.cljc`: `(defn apply-delivery [store host-index delivery compiled-face view-ctx] → [store' host-index'])` PURE: per unit-id → `:tombstone` ⇒ `ss/remove-slot store [:unit uid]`; row ⇒ if `(= row (get-in host-index [:rows uid]))` skip (equality gate) else `ss/upsert-slot store [:unit uid] {:tree (ss/build-face-tree compiled-face row view-ctx #{uid}) :container (cid-of host-index (:container-id row)) … :stratum :world}` with the tree's root placed at the unit's container-local position (`:geometry` or §9.4 default). `host-index` = `{:rows {uid → row} :containers {C → cid} :members {C → #{uid}}}` maintained by key. The edge: `(defn run-apply-edge! [<deliveries] …)` — ONE `m/reduce` over the courier flow doing ONE `swap!` of `sr/!scene-store` per delivery (the precedent: `refresh-all-slots!` reduces `upsert-slot` inside one swap, `scene_runtime.cljs:278–292`). Effects (the swap) only here — T4. | own ns, pure core |
| · store entry points | `ss/upsert-slot [store vi opts]` (`scene_store.cljc:169`) · `ss/remove-slot [store vi]` (`:203`) · `ss/build-face-tree [compiled projection view-ctx unit-ids]` (`:493`) · `ss/empty-store` (`:151`) · `sr/!scene-store` (`scene_runtime.cljs:34`) · container allocation: ONE container per conversation via `sr/register-face-instance!` (`:186`) with an empty frame tree at the conversation's origin — then per-unit slots carry that `:container`; `:container-slot`/`:stack-path` as `upsert-main-face!` (`:313–333`) does. Falsifier: the 1024-container ceiling (`scene_runtime.cljs:105–130`) — one per CONVERSATION, never per unit. | existing |
| **C-3 demand** `app.client.host.demand` | NEW `demand.cljs`: `(defn visible-containers [frame-fact] → #{C})` PURE over the frame edge's visibility fact (camera rect ∩ container bounds, from `sr/<effective` + the store's per-container bounds); `(defn publish! [lens-id set])` — POST S-3 only when `set` ≠ last published; the frame edge hands it the fact once per sampled frame. | own ns |
| **C-4 frame** `app.client.host.frame` | NEW `frame.cljs`: stand-up by the verifier's recipe (adapter/device `verifier.cljs:5465–5480` · font manifest + assets `:5482–5497` · camera/containers buffers + rect/text systems `:5498–5560` — all calls into production `renderer`/`fonts`); the rAF sample edge — `(m/sample vector <world >raf)` → `m/reduce` (the deleted host's shape, `adc30c9^:src/app/client/workspace/runtime/render.cljs:167–630`, read BY SEAM); the per-slot text pass — `reconcile-slot-text-geos!` (`adc30c9^:…/render.cljs:76–165`: "changed text → reshape ONLY that slot's geo via `update-text-data`") rebuilt here over `(:text-by-vi store-frame)`; the identity-preserving gate (§6 F3) between `<store-frame` and `draw-frame!`; then `renderer/draw-frame!` (`renderer.cljs:3678`, 9 positional + kwargs; `store-frame` via `:store-frame`, text via `:extra-text-geos`, `effective-transforms`, `container-registry`, camera floats, `editor-rect-count 0`); pools: `ordered-diff-update-pool!` (`buffer_pool.cljs:275`) — **ORDERED, as the offset math in `store-pool-entries` (`renderer.cljs:2898–2915, 2970`) requires; `keyed-diff-update-pool!` (`:214`) is implemented but unwired and would break per-vi offsets** — keyed pools belong to the store-contract slice. The visibility fact is minted HERE (the sink), handed to C-3. | own ns |
| **C-5 main** `app.client.host.main` | NEW `main.cljs` `start!`: lens-id → load + compile `block-face.edn` (`fa/compile-assembly face-primitives/registry parsed`, precedent `assembly_adapter.clj:145`) → stand up frame → open courier with the initial container set (atom 1: one id from the URL `?c=<container-id>`) → run apply edge → run demand. NEW shadow build `:host` in `shadow-cljs.edn` (`:modules {:main {:entries [app.client.host.main] :init-fn app.client.host.main/start!}}`, `:output-dir "resources/public/host/js"`), NEW `resources/public/host/index.html` (one canvas, one script). | own ns |
| **C-6 the block face** | NEW `resources/public/faces/block-face.edn` — grammar 0, the inner `[:each [:blocks]]` template of `outline.edn` lifted to a root: `{:prim :stack :props {:gap 4} :children [{:prim :badge :props {:label {:bind [:kind]}}} {:prim :text-run :props {:value {:bind [:text]}}}]}` — binds resolve against the UnitRow directly. Fetched at boot like `font_atlas.json`. | data |

### Law on where code goes
New code in its own namespaces above. `renderer.cljs`, `server_jetty.clj`, `object_container.clj`, `face_projection.clj` get thin hooks only (an arm, a line, an extracted pure fn). `scene_store.cljc` gains at most one pure fn (`preserve-lane-identity`, §6 F3) if the host doesn't keep it local. `env.clj` is never read or touched.

---

## 6 · The five things — every new computation that feeds frames (render-seam review rule)

| # | Computation | Keyed inputs (minted where, routed how) | Door (no clock ancestor) | Ownership / generation | Projections (who reads, what shape) | Oracle + fence |
| --- | --- | --- | --- | --- | --- | --- |
| W1 | `deliveries-for` (server, per lens) | rows by unit-id from `unit-rows`; the lens's shadow | the notice (an accepted act); boot = first loop | one delivery = one accepted act (`:gen`); rows carry their own event-ids | the wire (flat map) | the page: every sent UnitRow ⊇ the page's block entry (W2 golden) |
| W2 | `unit-rows` (server) | PState reads by container | called by W1 only | Rama's (the act's event-id on every row) | W1 | `face_projection/serve` — the committed-echo cross-check (INV-19) named IN CODE as this golden |
| A1 | `apply-delivery` (client, pure) | delivery `:units` by key; `host-index` rows by key | the courier flow → ONE `m/reduce` edge; never a timer | one swap per delivery = one generation of the store (render seam "one swap is one generation") | `sr/!scene-store` (slots by `[:unit uid]`), `host-index` (members/containers by key) | JVM golden: applying the page's units as one boot delivery yields a store `=` to applying them as N single deliveries in any order (key-order independence); catch-up golden (S4) |
| A2 | default position rule (§9.4, pure) | the container's order-keys by unit (row data) | inside A1 | same swap | slot root offset | none needed (pure, total); tripwire covers births at the tail |
| F1 | `<store-frame` = `(m/latest ss/derive-store-frame (m/watch sr/!scene-store))` | the store value | the store watch (event-driven) | the store swap | F3 → `draw-frame!` | unchanged: O(N) rebuild is the receipted stage (§10 c); the per-key slice demotes it later WITH a fence |
| F2 | the rAF sample edge `(m/sample vector <world >raf) → m/reduce` | `<world` = ONE `m/latest` fan-in of store-frame · effective · registry · (never the camera — read at the sink, T3/§3 hop 6–7) | the frame clock — AT THE SINK ONLY | read-atomic mosaic at the pull | `draw-frame!`; the visibility fact (to C-3) | none; skip-if-unchanged by identity |
| F3 | `preserve-lane-identity [prev next]` — per lane of the store-frame, if `(= prev-lane next-lane)` keep `prev-lane`'s identity | the two store-frames | inside F2, per sampled frame with a new store-frame | same | `draw-frame!`'s `changed-families` (which is `identical?`-gated: `frame_inputs.cljc:100–112`) | degenerate fenced view (equality-gated memo, render seam "The unit"); its oracle is `=` itself; the slice replaces it with per-key views |
| F4 | per-slot text pass (`reconcile-slot-text-geos!` rebuilt) | `(:text-by-vi store-frame)` by vi | inside F2 | same | `draw-frame!` `:extra-text-geos` | identity fast path: unchanged vi ⇒ the same geo object (T5/T12); tripwire S1 counts reshapes |
| D1 | `visible-containers` (pure) + `publish!` | the visibility fact minted at F2 | the frame sample (a WRITER up the arrow — lawful: the fact is data minted at the sink; it never re-enters derivation) | the lens | the server's lens set | S2 counts requests |

A raw watch, a per-frame derive, a read-side differ anywhere else in the host is illegal (render seam review rule). The seam lint: `grep -n "add-watch\|m/watch" src/app/client/host/` must return only F1/F2's sharing points.

---

## 7 · The five decisive scenarios — frozen as tripwires/goldens at close

Each carries the wrong build that would pass a weaker wording (receipt-gaming lens), and the strengthening.

**S1 · one edit → one slot.** Lens L subscribed to conversation C (N ≥ 20 units, all positioned). One same-line-count content edit on unit U through S-4. EXPECT: exactly ONE delivery; `(count (:units d)) = 1`; `(:event-id row) = "evt:<object-key>:<request-id>"` of the edit (the client correlates its own act); store diff = exactly 1 key (`[:unit U]`); `reconcile-slot-text-geos!` reshapes exactly 1 vi; `changed-families` = `#{<the text family in use>}` (rect family unchanged because the slot's rects are `=` and F3 preserved their identity); no request went up. *Wrong build that passes a weaker wording:* re-sends the whole membership under one `:gen` (passes "one delivery") → strengthened by `count = 1` and store-diff = 1; a host that re-upserts every slot but whose slots are `=` (passes "frame shows it") → strengthened by the reshape count and the family set.

**S2 · a pan sends nothing.** Same lens; a 2-second pan/zoom that keeps C in view (the visible container set never changes). EXPECT: 0 demand requests, 0 deliveries, 0 store swaps, 0 reshapes; frames still redraw (camera read at the sink). AND the publisher is alive: exactly 1 demand request at boot (so "sends nothing" is not vacuous). *Wrong build:* a publisher that never sends (passes "zero after boot") → strengthened by the boot count = 1; a publisher that sends every frame but the server dedups (passes "server saw no change") → strengthened by counting at the client's fetch seam.

**S3 · a second lens sees a settle.** Two lenses L1, L2 on C (two EventSources; two browser windows or one page + a test client). L1 settles U's geometry through `/api/episode/geometry` (a non-delete settle). EXPECT: L2 receives exactly ONE delivery with exactly ONE unit (U) whose `:geometry` is the new cell and `:geometry-event-id` the settle's event-id; `:gen` = that event-id; L2's store diff = 1 key; no epoch integer is read anywhere in the path (`grep !ingest-epoch-atom src/app/server/feed.clj src/app/client/host/` = 0 hits). *Wrong build:* settle bumps the epoch and the lens re-pulls the page (passes "L2 sees it") → strengthened by the unit count and the grep.

**S4 · catch-up from a watermark** (atom 3 tripwire; atom 1 golden as "reconnect = boot"). L holds watermark W (atom 1: nil), loses the stream; meanwhile k acts land (edit, settle, birth, tombstone). On reconnect EXPECT: deliveries arrive through the SAME apply edge (a counter on `apply-delivery`; the page route is never hit); the store after catch-up `=` the store of a lens that never dropped; in atom 1 (W nil) the reconnect delivery is a boot whose equality-gated apply swaps exactly the k changed keys. Atom 3: the deliveries since W are exactly the k acts, in log order, `:watermark` advancing. *Wrong build:* a second "reconcile" path that reads the page into the store (passes "store equal") → strengthened by the apply counter and the page-route counter = 0.

**S5 · a tombstone retires** (atom 3 tripwire; atom 1 golden). U is deleted (settle with `:deleted? true`). EXPECT: every lens whose shadow held U receives `{U :tombstone}` once; `ss/remove-slot` runs for exactly `[:unit U]`; a lens that subscribes AFTER the delete never sees U nor a tombstone. Atom 3 adds: a lens reconnecting with W older than the delete receives the tombstone; a lens whose W is older than the retention horizon gets a boot (the cap-overflow reconcile duty, named in code). *Wrong build:* the server forgets the tombstone as soon as one lens gets it (passes for L1) → strengthened by the two-lens + late-reconnect cases.

Goldens (2–3, JVM): `apply-delivery` key-order independence · `deliveries-for` diff/tombstone algebra (shadow ∅ → boot; equal rows → nothing; missing → tombstone) · `unit-rows` ⊇ page entry (the oracle fence, in-process against a test cluster).

---

## 8 · MUST-NOTs — real ones only

- **No execution clock as an ancestor of derivation.** rAF, `performance.now`, wall clock, the server's 15 s poll: read only at the frame sink (F2), in the demand publisher (a writer), and in the producer's heartbeat (a writer). Never an input to `apply-delivery`, `derive-store-frame`, or any `m/latest`.
- **No page as truth.** The client never calls `face_projection/serve` or any page route to render. The page is the oracle, reachable from tests.
- **No mirror atoms.** One courier flow → one reduce edge → `swap! sr/!scene-store`. No `add-watch` copies, no mailbox atoms, no `reset!` of a derived value into a second atom. The visibility fact is handed as a value, not watched from a mirror.
- **Order as row data.** `:order-key` on the row; no positional index on the wire; the tape order is a sink-side view.
- **No Electric in `deps.edn` in this package** (§9.1 names the replacement trigger) · **never read or touch `src/app/server/env.clj`** · **no GPU readback** · **bytes never in the scene value** (heavy assets by id + revision) · **culling never writes the store** · **one write path for block text** (`submit-block-edit!`; a second would be the T1 second-wearer tell).

---

## 9 · Positions and the forks seen

**9.1 Courier — SSE + Missionary, owned (the Electric arm struck 2026-08-30).** Content (§4) is courier-agnostic; the courier is `host/courier.cljs` + the stream arm's writer — one file each side. The **transfer bench** (PROBLEM-SPACE probe 1 — four merge algebras × cross-wire losslessness × two-edits-one-tick) still runs when the Block probe builds the face, as a test of our own feed and apply edge — no longer as an Electric-vs-owned decider. AMENDED 2026-08-30: the `e/diff-by` arm and the counter-position "re-add Electric now and ride `e/diff-by` from day one" are struck — Hyperfiddle left Clojure (v4 = JS + WASM), v3 is a frozen proprietary alpha; its incseq source stays a reading reference for the algebra's shape (electric-docs skill §0). Original wording in git. Risk of the position, unchanged: an owned lane entrenches; the fence is the frozen format + one-file courier. One Jetty thread per lens (blocking `StreamableResponseBody`) is the courier's known ceiling; it is fine for the felt pass and named here.

**9.2 The host (composition) — the Block probe decides.** Missionary host here. AMENDED 2026-08-30: the "Electric generic host" arm of the Block probe is struck (v3 orphaned — §9.1); the probe still builds the Block face as data on the Missionary host and judges it on container close/reopen, mid-drag teardown, served-source hot-swap (decisions.md "Open"); any future second host enters through the same probe. Faces are data, the store is the target — nothing here forecloses that.

**9.3 The server's memory — in-JVM now, one change-log PState at atom 3.** Receipt (B, §10): no time-ordered change history exists across kinds — `RevisionRow` history is time-prefixed per object container (text only); geometry cells overwrite in place at deterministic keys (`geo:unit:<sha8>`); turn status overwrites; there is no push (no proxies, no server Missionary), only a process-local epoch counter. So "the since-reader is one function" was half true (text yes; geometry/membership/tombstones no). Position: atoms 1–2 run the producer as **notice → re-shape the container → diff against the lens's shadow** (one path for boot and live; the wire and the client stay proportional; the server pays O(members) shaping per act — measured by a counter, PROBLEM-SPACE probe "one swap of N units"). Atom 3 adds `$$conversation-change-log {container-id {order-key ChangeRow}}` (order-key = `fixed-width-order-key time request-id`, `object_container.clj:243`; ChangeRow = `[container-id unit-id kind event-id order-key]`), appended in the SAME topology event as the change it records (one generation authority), read with the existing `sorted-map-range-from` exclusive-cursor idiom (`trail_view.clj:649`); the watermark = its order-key; the producer then point-reads touched units; the notice bus stays as the wake-up. Rama work keeps its ladder (rama + rama-pitfalls skills).

**9.4 Default position for unpositioned units (pure client rule, v1).** Units with `:geometry nil` sit in a column at container-local `x = 0`, `y = ordinal × 120` where ordinal = the unit's rank among the container's members sorted by `:order-key` (row data; ordinal is a derived view computed inside `apply-delivery` for the touched container). A birth at the tail moves one slot; a birth in the middle moves the tail (its ranks changed — the affected set, lawful). Replacement: a server-minted default cell at birth (positions always row data) — LATER, the spec sitting. The 120 is a dial.

**9.5 Pools stay ORDERED in this package.** Receipt (A): `keyed-diff-update-pool!` is implemented and uncalled; `store-pool-entries`' per-vi `first-instance` offsets assume allocation order = draw order. Keyed pools + the offset re-cut = the store-contract slice.

**9.6 One container per conversation, one slot per unit.** `vi = [:unit unit-id]`; the conversation's cid is allocated once; unit trees are container-local with the root at the unit's position (transforms compose in-shader; a conversation move is one transform value). The bench (D) built 2000 slots in one container at distinct offsets — the shape is supported today.

**9.7 Demand v1 — grow and shrink immediately, no hysteresis, no eviction.** Thresholds are userland dials (Sid's trails ruling); none are introduced here.

Forks left for Sid: none in this package. (Act grain, chrome-as-material, collaboration soul stay where PROBLEM-SPACE lists them.)

---

## 10 · Receipts banked at this cut (probes §11 of the map called "structure only / unverified")

- **(a) The verifier never reaches `draw-frame!`** — structure confirmed: `start!` → `run-verifier!` → `run-w4-frame-runtime!` → `w4-capture!` → `compositor-gpu/draw-multipass!` (`verifier.cljs:5667, 5465, 3635, 2946`); `draw-frame!` has zero callers in `src/` (its only other hit is the static fence `test/render_engine/verify_scene_tape_fence.mjs:50`). The verifier is one-shot (no rAF anywhere in `src/app/client`). Build command: `clj -M:dev -m shadow.cljs.devtools.cli release render-verifier` (package.json `verify:render-engine`; bare `npx shadow-cljs` lacks the classpath). A stale `target/render-verifier/receipt.json` (2026-08-20) passes all 7 guards. **Consequence: the host is `draw-frame!`'s first caller since the cut — the changed-families DAG has no runtime receipt until S1 runs.**
- **(b) pick** — `scene-store/pick [store effective-transforms point]` (`scene_store.cljc:406`) → `scene_tape/pick-reverse`; JVM medians at N = 10 / 200 / 2000 slots: topmost hit 15 / 88 / 278 µs · first 39 / 145 / 448 µs · miss 24 / 129 / 452 µs; `maintained-entries` (`:314`) rebuilds the O(N) entry vector on EVERY pick (1.6 / 19 / 71 µs of that); growth ≈3.1–3.5× per 10× slots (sub-linear at these N, unbounded); a topmost hit costs 62 % of a miss — the walk short-circuits, the setup does not. The swiftshader 4.5 ms/189 figure is ~30× this JVM cost — hypothesis: that cost lives elsewhere (event path / JS bridge); kill-probe: `performance.now()` around `ss/pick` alone in the host (atom 2 carries it as a counter). JVM modality — shape is the receipt, absolutes indicative.
- **(c) `derive-store-frame`** — 0.17 / 1.41 / 16.66 ms at 10 / 200 / 2000 slots (ratio 11.8× per 10× = linear+); the `upsert-slot` that triggers it: 0.46 / 0.38 / 0.31 ms (flat). **54× amplification at 2000 slots — a dropped frame per edit.** At 200 slots ≈9 % of a 16 ms frame. This is the store-contract slice's numbered trigger. Bench: `docs/electric-native/receipts/2026-08-23-jvm-bench/bench.clj` + `bench.out` (7 rects + 6 text lines per slot; one container; JVM).
- **(d) family dirtiness is identity-based** (`frame_inputs.cljc:100–112`) — hence F3.
- **(e) the write surface** — ten POST arms, none edits block text from the wire; `submit-block-edit!` has one internal caller (`server_jetty.clj:786`).
- **(f) faces** — `face_primitives/registry` has 36 builders (12 `:block-*`), not 16; no per-unit builder exists; `build-face-tree` (`scene_store.cljc:493`) + a one-unit data-context is the lawful per-unit path.

---

## 11 · Atoms

**Atom 1 — one conversation container, keyed, end to end.** `app.shared.feed` (format + `deliveries-for`) · `app.server.feed` (S-1) + arms S-2 S-3 S-4 + hooks S-5 + S-7 · `app.client.host.*` (C-1…C-5) · `block-face.edn` (C-6) · `:host` shadow build + `resources/public/host/index.html`. Demand = the URL's one container, published at boot and on change (with one container it never changes — S2's boot-count strengthening keeps it honest). Tripwires: **S1 · S2 · S3**. Goldens: `apply-delivery` key-order independence · `deliveries-for` algebra · `unit-rows` ⊇ page entry. Close receipt per the work-package law (focused suite; foreign failures are debt; `git diff --name-only` is the file list). DONE WHEN Sid opens two browser windows on `/host/index.html?c=<genesis conversation>`, edits a block's text in one (a dev console call to the edit arm is enough — no editor), and it lands in the other with the three tripwires green.

**Atom 2 — demand that moves.** Two conversations; `visible-containers` real (camera ∩ container bounds); pan into the second grows, pan away shrinks; S2 full; the pick counter (10 b); the 1024-container fence test; reconnect-equals-boot golden (S4's atom-1 form).

**Atom 3 — the wire's memory.** `$$conversation-change-log` (9.3, rama skill); watermark = log order-key; SSE `id:` + `Last-Event-ID`; tombstone retention horizon (a dial; default 24 h) + per-lens shadows keyed durable; the oracle's two duties NAMED IN CODE — committed-echo cross-check (W2 golden, `INV-19` in the docstring) · cap-overflow reconcile (W older than horizon ⇒ boot); tripwires **S4 · S5**.

**Package close — the seam courtroom, once:** full repo suite · the felt pass (two windows, one edit, one move) · `docs/below-the-waist/engine-two-arrows.md` §8 rewritten from "fill" to "as built" (+ html twin) · the board's electric-native block flipped.

---

## 12 · Author's four-lens pass (run at the cut; findings repaired in place above)

**Chain of custody** — every value a consumer needs has a written road: `object-key` + `document-container-id` ride the UnitRow so the client can EDIT what it sees (S-4 needs both; found missing in the first draft — added §4) · `request-id` is client-minted and echoed by S-4's response; the client derives `"evt:" object-key ":" request-id` and matches the UnitRow `:event-id` (S1) · settle event-ids come from the hint row (`episode.clj:277`) into `:geometry-event-id` (S3) · the visibility fact is minted at F2 and handed to D1 as a value (S2) · the page's per-unit entry reaches W2's golden in-process via `serve` (no route needed) · tests are consumers too: goldens load `app.shared.feed` + `app.client.host.apply` (`.cljc`) on the JVM; tripwires load the host build in the browser harness (`reference-desktop-browser-harness-road` memory names the proven headful road).
**Receipt gaming** — per scenario above (§7); the shared strengthening is COUNTS at seams (units per delivery, store-diff keys, reshapes, requests, page-route hits) rather than "it shows".
**Pin or fork** — "the lens" = one EventSource + its container set + its shadow (server) / its `host-index` (client) · "unit" = a block (`du:…`), never a turn · "container" = the conversation (`oc:chat-conversation:chat:<sha>`) · "generation" = the accepted act's event-id · "membership" = `{C → #{unit-id}}` derived from rows · "order" = the row's `:order-key` · "position" = the settled cell or 9.4 · "changed family" = `frame_inputs/changed-families` after F3 · "boot" = a delivery with `:gen :boot` · "the oracle" = `face_projection/serve` in-process. Policy adjectives pinned: heartbeat 15 s · default pitch 120 · shrink immediate · retention 24 h (atom 3) — all dials.
**Obligation cross-check** — clock MUST-NOT × S2: the demand publisher runs off the frame sample — lawful because it writes up the arrow and its output never re-enters derivation (stated in §6 D1) · page MUST-NOT × W2's golden: the golden reads the page, the client never does — the grep in S3 is the fence · mirror MUST-NOT × C-2's `host-index`: it is the apply edge's own accumulator inside the ONE reduce, not a second atom (pinned: it lives in the reduce state, or in the same swap as the store if the host prefers one atom) · order MUST-NOT × 9.4: ordinals are derived inside the apply, never transmitted · one-write-path MUST-NOT × S-4: the arm calls `submit-block-edit!`, mints nothing of its own. Forced violation hunted, none found; the one near-miss — F3 compares whole lanes with `=` each swapped frame (O(lane)) — is the named stage, not a rescan hiding below the seam, because its replacement and trigger are written (§2 first line, §10 c).

---

## 13 · Starter — atom 1 (Codex; paste whole)

```
Preflight: set permission mode / remote-control / MCP BEFORE this first prompt (prefix law). Lane: Codex builds; Fable cut.
Boot (≈96 KB, read in this order, by seam): docs/electric-native/CONTRACT.md (≈46 KB, PRIMARY, whole) ·
docs/decisions.md :450–628 "The render seam" (≈17 KB, PRIMARY law) · docs/scene-substrate/CONTRACT.md §7 traps (≈3 KB) ·
docs/below-the-waist/engine-two-arrows.md §3 (:125–196, ≈6 KB) · .claude/skills/electric-docs/SKILL.md §2 (:62–227, ≈12 KB) ·
.claude/skills/work-package/SKILL.md "Execution — straight through" + "Close receipt" (:85–116, ≈3 KB) ·
CLAUDE.md Token Economy (read by seam; skeleton-first on code: `grep -n "^(def"` then windows).
Code by seam only (never whole): scene_store.cljc :97–230, :314–345, :449–520 · scene_runtime.cljs :34–130, :186–232, :278–333, :445–470 ·
frame_inputs.cljc :25–69, :100–150 · renderer.cljs :3418–3421, :3678–3700, :3794, :3847, :2898–2915 · buffer_pool.cljs :275–330 ·
verifier.cljs :5465–5560 (the stand-up recipe) · server_jetty.clj :316–320, :468–478, :721–727, :744–800, :1387–1400, :1546–1604, :1606–1613 ·
episode.clj :268–292, :355–365 · block_edit.clj (whole, 2 KB) · face_projection.clj :193–211, :655–660, :1910–1918 ·
object_container/runtime.clj :174–200, :363–370 · resources/public/faces/outline.edn (whole) ·
deleted host BY SEAM: `git show adc30c9^:src/app/client/workspace/runtime/render.cljs | sed -n '76,165p;167,300p;440,530p'`.

Direction: build atom 1 of CONTRACT.md §11 — one conversation container, keyed, end to end — whole, straight through: the
feed format (§4) · the server feed + four thin arms + three notify hooks (§5 S-1…S-7) · the client host, five namespaces +
the block face as data (§5 C-1…C-6) · the :host shadow build + resources/public/host/index.html · tripwires S1 S2 S3 + the
three goldens (§7) · the five-things rows W1 W2 A1 F1–F4 D1 (§6) obeyed, the seam lint clean. Equality-gate at the apply
edge; identity-preserve lanes at the frame edge (F3); pools ORDERED (9.5); one container per conversation, vi = [:unit uid]
(9.6); default position rule 9.4. No Rama topology change, no Electric, no editor (§2). Genesis conversation id:
episode.clj:56 `genesis-conversation-id`. Browser receipts ride the proven headful road (memory: desktop browser-harness road).
Plan OWNER + SOURCE + DONE WHEN per the work-package skill; build → your one implementation adversarial check against the
real seam → fix → source freeze → close receipt (≤15 lines, `git diff --name-only`) → flip the board pointer
(docs/next-prompt.md electric-native block: "atom 1 CLOSED, receipt at …"). Ambiguity = strongest default + a note; a
genuine fork = ONE question in the thread file, keep building around it. Commit freely on main, grouped by concern;
never push; never Co-Authored-By. Stops: only forever-consequence items (spend, env.clj, deps.edn Electric, push).
DONE WHEN: two browser windows on /host/index.html?c=<genesis> — an edit through POST /api/block/edit in one lands in
the other; S1/S2/S3 green with the counts named in §7; goldens green on the JVM; close receipt written.
```

---

*Contestable by construction: every pin carries an anchor or a receipt; findings go to the next fresh-eyes round (one bounded round at Sid's hand if he wants it; ≤1 decision-changing finding retires it) and repair in place. The map (`engine-two-arrows.md` §8/§11 + html twin) is repaired this cut where gatherers falsified it; the board and DIRECTION.md point here.*
