# block-write — LANE_A (server-additive) record

Fresh-context Lane A implementer, 2026-07-12. Built against CONTRACT v1 + the
PHASE_0 (b) pin. All code in the working tree, uncommitted (per baton: no commits).

## Verdict
Gates **G1, G2, G3, G4, G5 (IPC half), G9 — ALL GREEN, first run.** Plus a
PHASE_0 provenance gate (map-not-lie) green. Regression suites green alongside.

- `block-write-lane-a-gates`: **41 assertions, 0 fail, 0 error**.
- `block-distiller-test` + `face-projection-test`: 954 assertions, 0 fail.
- `object-container-test`: 149 assertions, 0 fail.
- `machine-cut-serve-test` + `face-transcription-test` (blocks->turns consumers):
  349 assertions, 0 fail (the `[FACE] machine-cut edge read failed` log lines are
  the existing test's own mock degradation path — pre-existing, not mine).

## Entry point (for Lane B wiring at INT)
- **ns / signature:** `app.electric-flow/submit-block-edit!`
  - `(submit-block-edit! oc-rt env)` — production arity (bumps
    `app.server.rama.util-fns/!ingest-epoch-atom`).
  - `(submit-block-edit! oc-rt env !epoch)` — epoch atom injected (test arity).
- **`env` (CONTRACT §3 envelope, client-minted except content-hash):**
  `{:request-id :idempotency-key :edit-client-id :edit-seq :time-ms :actor
    :target {:target/kind :derived-unit :target/id <unit-id>}
    :payload {:document-container-id <served block's value> :object-key <face :address>
              :content-text <buffer>}}`
  (no `:content-hash` — the server stamps it via `oc/source-hash`; a client-supplied
  hash is ignored.)
- **Returns** a plain, wire-safe map (no raw record):
  `{:accepted? :replay? :reason :errors :status :request-id :object-key :target-id}`.
  On rejection `:reason`/`:errors` carry the refusal (`:edit/stale`,
  `:request-invalid` + `:actor-not-authorized`, etc.) — G5 surfacing.
- **Electric surface:** `app.electric-flow/SubmitBlockEdit` (e/defn, mirrors
  `fv/RecordFaceWear`): `(e/server (submit-block-edit! (:oc-rt (fv/face-ctx)) env))`.
  Wired in `Main` to `!block-edit-outbox` → `!block-edit-result`
  (electric_flow.cljc:644), both threaded into `loop/start-loop!` at
  electric_flow.cljc:752 (Lane B destructures them in `runtime.cljs` at INT;
  start-loop! is varargs so the extra kwargs are inert until then).

## Files touched (all additive)
1. `src/app/server/rama/object_container/block_distiller.clj:1301-1307` — PHASE_0
   rule 1: `:document-container-id (:document-container-id unit)` on the
   `render-river-source` per-block map. No new read (`unit` already read) → seek
   plan unchanged (G9). BW-T7 cited.
2. `src/app/server/rama/face_projection.clj:88-94` — PHASE_0 rule 2:
   `:document-container-id (:document-container-id b)` threaded through
   `blocks->turns`. Purely additive; existing served keys byte-stable (MC-T8).
3. `src/app/electric_flow.cljc`
   - `:11` — server-only requires (`oc`, `ocr`, `util-fns`) under `#?@(:clj ...)`.
   - `:43-96` — `#?(:clj (defn submit-block-edit! ...))` (the entry point). BW-T9,
     BW-T5 cited in code.
   - `:107-113` — `(e/defn SubmitBlockEdit [env] ...)`.
   - `:587-588` — `!block-edit-outbox` / `!block-edit-result` atoms in `Main`.
   - `:644-646` — the server e/watch wiring (envelope → SubmitBlockEdit → result).
   - `:752-754` — thread the two atoms into `loop/start-loop!`.
4. `test/app/server/rama/object_container/block_write_test.clj` — NEW. Gates
   G1-G5(IPC)/G9 + PHASE_0 provenance gate, over the deterministic block-distiller
   transcript fixture (reused via `block-distiller-test`; no real-corpus dep).

## BW-T10 finding (debounce, VERIFIED in code)
The epoch re-pull debounce is **trailing-edge-INCLUSIVE — no final-pull swallow.**
`face_wiring.cljs:200-219`: every epoch bump `clearTimeout`s the pending timer and
re-arms a fresh `setTimeout` closing over THAT bump's `new-epoch`; the trailing timer
resets `!last-pull-epoch` to the latest `new-epoch` and calls `request!`. Because the
epoch is monotonic (`swap! inc`), each settled bump changes the request VALUE →
Electric re-pulls; the last keystroke's truth is always pulled. **No additive fix
needed on the swallow axis.** (Same shape in `trail_face/wiring.cljs:62-72`.)

**Separate tension to FLAG for INT/G7 (not BW-T10, not my fence):** that debounce
delay is **1000 ms** (`face_wiring.cljs:219`, the INV-19 ≥1s push cadence). Under
sustained 12/s typing it re-arms continuously and fires NO re-pull until a ≥1s idle
gap — which cannot meet G7's ≤50ms echo criterion. This is exactly the CONTRACT §5
narrowing-seam / G7-verdict territory and lives in a **client** file (Lane B/INT),
outside my fences. Recorded here as an open doubt; not fixed.

## Discrepancy flags (surfaced per CLAUDE.md, did not pause)
1. **STANDING allowlist vs the block_distiller.clj one-key add.** NOW.md STANDING
   names only `face_projection.clj` / artery / tests for Lane A. The
   `block_distiller.clj` single additive key is pre-blessed by CONTRACT §4 ("ONE
   additive river-page/projection field") and specified by PHASE_0 rule 1. Contract
   outranks the baton — I made the add and flag it here.
2. **Entry-point placement diverged from the baton's suggestion.** The fence note
   said the entry point *may* live in `face_projection.clj`. It CANNOT:
   `face-projection-test/g12-read-only-by-construction` greps `face_projection.clj`
   source and FAILS on the substring `append-object-container-request` — the
   projection layer is read-only **by construction** (declares no depots/topologies,
   performs no writes; the real architectural invariant, not just a grep). The write
   entry point therefore lives in the artery (`electric_flow.cljc`), beside
   `RecordFaceWear` — where the codebase's write e/defns already live (CONTRACT §5's
   "S40/artery pattern"). This honours the invariant rather than renaming around it.

## Judgment calls (for the gate reviewer)
- **Server-side hash stamp (pre-adjudicated).** `submit-block-edit!` computes
  `content-hash = (oc/source-hash content-text)` itself and ignores any client hash.
  One hash rule, server-side — no cljs crypto port, no drift (CONTRACT §4).
- **Epoch bump is accept-AND-not-replay only (BW-T9 + a correctness refinement).**
  The audit short-circuit (`object_container.clj:1818`) ack-returns the PRIOR
  decision VERBATIM on a same-request-id re-append — there is NO replay marker on
  that row. So I detect replay by a cheap pre-append `read-decision` ("was this
  request-id already decided?") OR `:replayed-from-decision-id` (the same-idem /
  different-request-id path). A replay changes no materialized state, so it owes no
  re-pull → no bump. A rejection likewise never bumps. Normal keystrokes (unique
  request-id per op-id law) always bump exactly once. G3 asserts the replay path
  bumps 0; G1/G2 assert a fresh accept bumps exactly 1.
- **`:ack` is the barrier (no polling).** `append … :ack` returns after the stream
  event tree is materialized; the decision is then read from the durable PState.
  Matches the `face_arsenal/record-wear!` idiom and the /work-package harness rule.
- **Gate fixture = the deterministic block-distiller transcript fixture**, not the
  6.9 MB real corpus — consumer-1 material (distiller-minted `DerivedUnitRow` river
  blocks, per-event `chat-message-id` document parents), green on any box.
- **Physical reads for the no-op / unchanged assertions.** G3 (byte-identical) and
  G4 (content unchanged) read `$$containers-by-id` / `$$revision-history-by-container`
  directly via `foreign-select`, never the public query surface (BW-T5;
  implementation-quirks "Physical (V1) reads vs public queries").
- **G5 actor shape.** `authorized-request?` (core.clj:349) auto-authorizes
  `:actor/type :system`, so the unauthorized actor is `:human` with empty
  capabilities — the ONLY reject cause is the missing `:object/edit` capability.

## PHASE_0 (b) proven end-to-end
The provenance gate asserts: every served river block's `:document-container-id`
equals its own `DerivedUnitRow.document-container-id` (a per-event
`oc:chat-message:…` id — PHASE_0's correct reading (b), NOT the rejected
`chat-conversation-id` (a)); and after graduation the owned
`ObjectContainerRow.document-container-id` equals that same value. The durable map
does not lie about which document the block came from.

## Open doubts
- The 1000 ms INV-19 debounce vs the ≤50 ms G7 echo (above) — INT/Lane B territory.
- G6 (caret/text co-variance) and the client-surfacing half of G5 are Lane B (per
  CONTRACT §12). This lane proves the server + entry-point half only.
- Inherited from PHASE_0: no exhaustive PState-reader census that some OTHER
  consumer-1 read joins on the container's document parent for the echo — argued
  from the graduation-overlay key (unit-id), corroborated empirically by G1/G2.
