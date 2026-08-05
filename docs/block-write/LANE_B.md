# block-write — LANE B (client) build record

Lane B implementer, fresh context, 2026-07-12. Client edit affordance + request
outbox + caret/pending-input law + G6/G5-client-half units. No git commits (left
in working tree). Every server claim below file:line-verified this session.

## Files created / touched

- **`src/app/client/workspace/block_edit.cljc`** (NEW, pure) — the outbox +
  edit-buffer state machine + caret-law render model. JVM-testable, no I/O.
  - `request-id` (`:26`), `idempotency-key` (`:38`) — the op-id law (BW-T5).
  - `sid-actor` (`:52`) — `{:actor/id "human:local" :actor/type :human
    :actor/capabilities #{:object/edit}}`.
  - `mint-envelope` (`:62`) — CONTRACT §3 envelope; content-text only.
  - `init-state`/`focus`/`blur`/`input`/`on-decision`/`dismiss-refusal`
    (`:116-206`) — the buffer machine (BW-T4/T8).
  - `block-view` (`:218`) — the ONE-signal render model (BW-T6/L8, G6).
- **`src/app/client/workspace/block_edit_wiring.cljs`** (NEW, glue) — session
  atoms, fresh client-id mint (BW-T8), `atom-submit!` seam, `focus!`/`blur!`/
  `handle-keystroke!`, and the single-`m/watch` flows `<focused-view`/`<block-view`.
- **`test/app/client/workspace/block_edit_test.clj`** (NEW) — 6 tests / 47
  assertions, all green first run (`clojure -M:test`).

Nothing under my fence's forbidden set was touched (no `electric_flow.cljc`, no
`server/rama/**`, no `render.cljs`, probe files untouched).

## Reader-face render path (found)

Face data reaches the client generically (CONTRACT §7 artery), NOT per-face:

1. Server projection → `fv/FacePull freq` in `electric_flow.cljc:533-535` →
   `!face-data` atom.
2. `face_wiring.cljs/install-face-wiring!` (`:118`) mirrors `!face-data` into
   `!face-context` (state.cljs), the ONE data-context the §5 flow watches.
3. The data-context's blocks are compiled through `!face-compiled` /
   `!face-scene` (face_assembly + face_primitives) and rendered by the WebGPU
   loop in `runtime/render.cljs` via `<combined-text-ops` and
   `<editor-rects+sidebar` (both take `!face-scene`).
4. The served **block** map is minted server-side in
   `face_projection.clj/blocks->turns` (`:68-88`): `:id` (= unit-id),
   `:text`, `:kind`, `:order`, … The face-level `:address` (data-context) is the
   conversation object-key. **Lane A adds `:document-container-id` to that block
   map** (PHASE_0 rule 1-2) — my outbox reads it verbatim off the focused block.

So a block's identity + document-container-id + the face address are all present
in client atoms once Lane A's additive keys land. My code depends only on the
block keys `:id` + `:document-container-id` and the face `:address`.

## Submit-seam shape (the fn my outbox expects injected)

```
submit! : (env, done) -> nil        ; env = mint-envelope output
done    : (decision)  -> nil        ; decision = {:status :accepted|:rejected
                                    ;             :reason <keyword>?}
```

- Tests inject a synchronous stub `submit!` that calls `done` inline.
- Real path: `atom-submit!` (wiring `:44`) resets `!edit-outbox` to `env` and
  registers `done` keyed by `request-id`; the durable decision arrives in
  `!edit-result` and one `add-watch` routes it back by `:request/id`. This is the
  BACK-ARROW law (client streams INTO Rama; never a direct server call) — the
  exact shape of the existing face-wear outbox.

## INT wiring (apply outside my fence — precise, verbatim-ready)

Modeled 1:1 on the face-wear outbox path.

1. **`src/app/electric_flow.cljc`** — beside `!face-wear-outbox`/`!face-wear-result`
   (`:500-501`) add two atoms:
   ```clojure
   !edit-outbox (atom nil)
   !edit-result (atom nil)
   ```
   Beside the wear write path (`:549-551`) add the edit write path:
   ```clojure
   (let [edit (e/watch !edit-outbox)]
     (when edit
       (reset! !edit-result (fv/SubmitEdit edit))))
   ```
   Thread `:!edit-outbox !edit-outbox :!edit-result !edit-result` into the runtime
   atoms map at the install site (`:646-653` region).

2. **`src/app/file_viewer.cljc`** — Lane A's server entry point (the write e/defn,
   twin of `RecordFaceWear` `:457`):
   ```clojure
   (e/defn SubmitEdit [env]
     (e/server (submit-edit-safe! env)))   ; Lane A owns submit-edit-safe!
   ```
   `submit-edit-safe!` must: stamp `content-hash` via the kernel's own
   `source-hash` (object_container.clj:263) over `content-text`; build the request
   with `oc/object-edit-request` (`:1650`) passing my env's `:request-id`,
   `:idempotency-key`, `:edit-client-id`, `:edit-seq`, `:object-key`,
   `:document-container-id`, `:actor`, target-kind `:derived-unit`, target-id;
   append to `*object-container-requests-depot` with `:ack`
   (`runtime/append-object-container-request!` `:90`); return the durable decision
   as `{:request/id ... :decision/status ... :decision/reason ...}`.

3. **Focus + keystroke sources (runtime wiring, my namespace consumed):** a click
   hit-test on a face block rect yields its `:id` → call
   `block-edit-wiring/focus!` with (unit-id, current materialized text). Keyboard
   events while a face block is focused → `handle-keystroke!` with the focused
   block map (`:id` + `:document-container-id`), face `:address` as object-key, and
   the new (text, caret) pair. The focused block's render model comes from
   `<focused-view` (or `<block-view` per block) — a SINGLE `m/watch` over
   `!edit-state`; wire it into the face-scene compose so the focused block draws
   `:text`/`:caret` from the buffer and the refusal notice from `:refusal`.
   (If this requires an `electric_flow.cljc` edit, it does not — the focus/
   keystroke sources are client-side runtime wiring; only items 1-2 above are
   outside my fence.)

## The op-id derivation reused

The object-container edit builder's OWN default idempotency-key
(`object_container.clj:1680-1682`):
`(str "object/edit:" object-key ":" target-id ":" request-id)`. I reuse that shape
VERBATIM in `block-edit/idempotency-key`, so a client-minted key is byte-identical
to the server default — one op-id law, no second derivation (BW-T5). `request-id`
= `(str edit-client-id ":" edit-seq)`: deterministic from (client-id, seq), unique
per keystroke, replay-stable, no cljs crypto. The git-spine (`:229-230`) and
relation write path (`server_jetty.clj:939-940`) confirm the settled shape:
request-id and idempotency-key are a deterministic pair, kernel-scoped by
partition (object-key).

## Judgment calls

- **edit-client-id granularity = per app-boot / editing session** (one id minted
  at `new-session-state`, never persisted). Justification (BW-T8): the stale-storm
  danger is REUSING an old id after a seq reset; minting fresh at each boot and
  never persisting makes a reset harmless (the kernel keys staleness by
  (client-id, seq), `object_container.clj:1374-1379`). One global monotonic seq per
  session; per-lineage monotonicity holds because global-monotonic ⇒ subset-
  monotonic. Per-focus ids would also be safe but add churn for no gain.
- **content-hash is NOT in the client envelope.** Payload carries content-text
  only; Lane A stamps `content-hash` server-side with the kernel's `source-hash`
  (no cljs crypto). Pre-adjudicated; noted in `mint-envelope`.
- **Buffer is not echo-driven (BW-T4).** Echoes update materialized truth (the
  pull); the buffer is pending-INPUT, cleared only on blur or reject. On accept the
  buffer stays (it already shows the user's text; the pull will match). This avoids
  a pull mid-typing clobbering keystrokes still in flight at the 12/s open-loop
  grain (BW-T3) — "committed truth replaces it on pull" is honored at blur / for
  unfocused blocks, where truth is what shows.
- **Caret law encoding (BW-T6/L8):** caret is stored IN `:buffer` next to text —
  never computed from text via a second signal. `block-view` reads the co-varying
  pair from that one value; the focused/unfocused choice is a BRANCH on
  `:focused-id` (same value), never a downstream combine. `<block-view` takes a
  second (truth) flow but the co-varying (text,caret) pair for the focused block is
  sourced from `!edit-state` alone — no diamond on the sensitive pair.

## Open doubts

- The focus/keystroke input sources (item 3) assume face block rects carry `:id`
  for hit-testing. The block map does carry `:id` (`face_projection.clj:82`) and
  the trail face already drives node ids off it, but the face-scene rect→unit-id
  hit-test for the conversation face was not traced to a concrete client handler
  this session — INT/G7 (headed) is the place that closes it.
- `SubmitEdit`'s decision shape (`:request/id`/`:decision/status`/`:decision/reason`)
  is my assumed contract with Lane A; if Lane A returns the raw decision row, the
  `atom-submit!` result watch keys need to match its field names. Flagged for INT.
- G6 is proven at the pure-fn layer (one-signal derivation cannot tear). Whether
  the Electric/render compose actually feeds `block-view` from the single
  `!edit-state` watch (and not a reconstructed two-signal path) is an INT
  verification — the flow constructors are provided so it can.
```
