# first-light A · P2b — the open ground (phase record)

2026-07-18 · Fable direct. Contract: CONTRACT.md §7 P2b + G4b (gated READY
TO CODE at `38ce344`). This record opens with the two mandatory pre-coding
receipts; build receipts and the G4b drill land below as they happen.

## Receipt (a) — §9.3 geometry feasibility: PROVEN adapter-level (live)

**The lean's named mechanism is dead; the import family carries it.** The
`:object/edit` envelope cannot carry geometry adapter-level:
`edit-effects` (object_container.clj:1403) constructs every effect row
exclusively from content fields (content-text/hash, client-id, seq) — an
extra payload key dies there, and threading it through is a kernel edit
beyond §5's two deliverables (stop-clause territory). No stop needed: the
import family already carries geometry with ZERO kernel edits:

1. **Hint-only imports are already legal** — the F3 ruling (Sid, 2026-07-10;
   object_container.clj:805-809): an import with projection-hints and no
   source artifacts validates.
2. **The hint write is an upsert keyed (conversation-container-id,
   order-key)** — object_container.clj:2203-2213. A deterministic order-key
   (`geo:unit:<sha8(unit-id)>` per block; `geo:camera` for the world camera)
   is ONE settled cell overwritten in place per settle write. Settle-state
   semantics exactly: last acknowledged settle wins; no per-event history.
3. **`imp:ep:` routing is shape-based** — extract-object-key
   (object_container.clj:333-334) parses `imp:ep:<object-key>:<sha>`; a
   settle key hashed from `"geometry-settle " settle-id` routes identically
   (the src:tr:/N1b disjoint-hash-input precedent). Zero kernel edits.
4. **Extra assoc'd keys round-trip** — PROVEN LIVE on the 8-task cluster
   (probe `scratchpad/p2b_receipt_a_probe.clj`, drill conversation
   `p2b-receipt-drill-01`, genesis untouched): hint-only import with
   `:geometry {:world-id :unit-id :x :y}` assoc'd on the row → decision
   :accepted → foreign read returns the key INTACT (x 123.5 read back);
   second import, same order-key, new settle-id → still ONE row, x
   overwritten 123.5→777.0 (upsert-in-place receipt); `:content-preview`
   carries `(pr-str geometry)` as the fingerprinted belt (projection-hint
   fingerprints exclude unknown keys — the preview copy makes geometry
   participate in fingerprint conflict detection).
5. **Invisible to every existing consumer** — river-page filters positively
   (:message rows · river-ledger-row? · native-utterance-row?,
   block_distiller.clj:1420/1422/1467); read-utterance-rows filters
   :episode-utterance (probe: native lane returned zero geo rows).
6. **Read-back at boot** — the SAME projection range read river-page already
   performs; face_projection.clj (a §2-sanctioned namespace) threads
   `:geometry` + `:camera` into the served conversation context.
7. **Settle-ack** — the driver append+awaits the import decision (the
   append-utterance! barrier shape): the ack IS the acknowledged settle.

**The §9.3 position-home design call (the board's named build-time call),
DECIDED — veto anytime:** geometry lives as settled cells in the episode's
conversation projection, world-scoped values
`{:world-id <episode-object-key> :unit-id … :x :y}` (for A, the genesis
world IS the episode container; B/worlds mint real world-ids). The lean's
stronger form ("physically on the unit") is NOT taken: re-importing unit
rows per settle would require re-sending surface+anchors every drag
(import validation: anchors required per target, sources per anchor) —
heavy — and rewrites unit rows per gesture. The projection-cell home is
lighter, honest to §9.3's own law ("the log stays truth, geometry stays
projection"), and reversible (delete the cells; nothing else moved).
Turn records ride the SAME mechanism: entry-kind `:episode-turn`,
deterministic order-key `ep-turn:<%020d time>:<sha8(turn-id)>`, value =
{source-unit-id, pinned content + hash, position, status open/complete/
failed} — status updates overwrite the same cell; an abrupt JVM death
leaves `:open`, the honest fact G4b demands.

## Receipt (b) — the intent-queue / committed-render seam

**Block-write's pattern (what P2b outlaws):** block_edit.cljc renders the
focused block from `:buffer` — pending-input IS a rendered text source
(painted pending), truth takes over only on blur/refusal. P2b's law:
visible text + key-induced caret derive from the SAME confirmed revision;
the queue is invisible.

**The seam, per block:**
- State: `{:confirmed {:text :caret :seq} :inflight [{:seq :request-id
  :text :caret}…] :next-seq :refusal}`.
- Keydown: apply-keydown runs against the PROJECTION (the last inflight
  entry's text, else confirmed) — envelopes carry whole content-text, so
  each keystroke's envelope must be built on the queue's head, never on
  confirmed. The projection is never rendered. Envelope identity =
  block-write's own law verbatim (be/request-id + be/idempotency-key —
  one op-id law, no drift).
- Accepted decision for seq N: confirmed := inflight[N]'s (text, caret);
  drop inflight ≤ N. Render shows revision N — the committed echo
  (browser-path p95 26.6ms, bar 52). Client-confirmed text at seq N is
  byte-identical to the server's accepted revision N by construction
  (deterministic full-text envelopes + per-client-id seq ordering + stale
  rejection); the §5 narrowing truth pull stays as the cross-check channel
  (truth wins whenever inflight is empty).
- Rejected/stale decision: drop ALL inflight (they were built on a dead
  base) — this IS the rebase onto the last confirmed revision — and raise
  the visible refusal at the block; typing resumes from confirmed.
- Electric conflation (block-write FALSIFY F3): skipped intermediate
  envelopes never get decisions; an accepted decision for seq M > N
  retires every inflight ≤ M, so the queue self-heals. Kernel staleness
  needs monotonic seq, not contiguous — gaps are legal.
- Birth: per-block lifecycle `:unborn → :birthing → :born`. First content
  act POSTs the birth (block-id client-minted once; retries converge);
  keys during flight queue as pre-birth intent; on the birth ack
  (unit-id), confirmed := birth text and the queued intent replays through
  the same keydown path as envelopes seq 0…. Escape in :unborn leaves
  NOTHING (no unit minted). Render during :birthing shows nothing yet —
  that IS the committed-echo law, not a bug.
- Reload mid-burst: client state dies; boot renders truth = exactly the
  acknowledged prefix (G4b).

## Build receipts (appended during the phase)

### What landed (one diff; the tip pattern died in it)

- **Server** — `episode.clj` §B2: geometry cells + camera cell + turn
  records (settled-cell hint imports, all `imp:ep:`-shaped, zero kernel
  edits); `utterance-import-request` gains `:position` (birth + placement
  in ONE acked import); birth ack returns `:document-container-id` (the
  replayed first edits need envelope identity before the pull returns).
  `server_jetty.clj`: `/api/episode/block-birth` · `/api/episode/geometry`
  · `/api/episode/utterance` REWORKED to the P2b form — the client-text
  mint is DEAD; what lands durable-BEFORE-agent is the revision-pinned
  turn record (:open), overwritten :complete/:failed/:timeout at turn end.
  `block_distiller.clj` river-page exposes geo/turn rows as additive meta
  (same projection read, zero new seeks); `face_projection.clj` serves
  `:conversation/geometry` + `:conversation/camera` +
  `:conversation/turn-records` (additive keys, MC-T8 class);
  `file_viewer.cljc` resolve-request gains the `:drill-conversation-id`
  face half of the §11 drill seam.
- **Client** — `ground.cljs` REWRITTEN (the P2 tip: pre-placed caret,
  `!ground-input` single buffer, `input-tree`/`face-bottom`/`reposition!`
  — deleted, never gated). The open ground: per-block scene slots in
  their own containers · world camera (wheel zooms at pointer; drag pans)
  · §9.4 pointer grammar (~4 CSS px threshold) · birth at first content
  act · committed-echo typing over `ground_edit.cljc` (NEW pure intent-
  queue machine, JVM-tested) · revision-pinned Ctrl+Enter + busy refusal
  at the block · provisional projection replaced at distill · settle
  driver (gesture-end arm, 400ms debounce, ack = safety, beforeunload/
  visibilitychange belt) · boot restore of camera+positions, zero
  attention state. Routing branches: mouse/scroll/keyboard consumers +
  paste; render.cljs drives the real camera uniform (`draw-frame!`
  `:zoom` — the seam was already staged) + the ground scene edge replaces
  `build-main-face!` in ground mode; `workspace_actions.cljs` gains the
  SETTLED class; `block_edit_wiring.cljs` exposes the submit seam.

### Live receipts (drill episodes; genesis VIRGIN)

- **Suites**: episode 18t (incl. new geometry/turn/birth-position units) ·
  ground-edit 5t · block-edit · scene-store · trail-face · face suites —
  103t/1262a green; cljs build 0 warnings.
- **Server loop live** (`p2b-receipt-drill-01`): birth accepted + retry
  converged (identical import identity, journaled no-op) · geometry
  settle accepted · serve returns geometry/camera/turn-records · edit
  lane on the birthed unit: 2 edits accepted, truth reads back the
  committed echo byte-exact; identical-retry replays without clobbering.
- **G4b browser drills** (headed Chromium, real GPU; `?drill=` worlds):
  - arrival = NOTHING: 0 blocks, mode rest, pure black
    (`images/p2b-arrival-nothing.png`); no hidden input (canvas app — the
    only key sink is the routed consumer).
  - click → anchor caret; Escape → rest, 0 blocks; server: native rows
    stayed 0 until the first content act (final count 1 after typing).
  - burst typing 37 keys ≈28/s: every emitted key acked (37 samples),
    confirmed = final committed = server truth BYTE-EXACT
    ("first light drill line one\nsecond line"); block born at the
    clicked point (400,250) (`images/p2b-first-block.png` — caret + box
    on attention).
  - **narrow echo p50 13ms · p95 18.9ms · p99 28ms (n=48)** — bar 52ms:
    PASS with 2.7× headroom (`__ground.echo` samples, envelope→decision→
    confirmed-render).
  - reload: acknowledged prefix exact, position exact, NO focus/caret/
    box (`images/p2b-return.png` — material at rest).
  - drag (400,250)→(580,370) + settle-ack + reload → EXACT return;
    geometry cell reads (580,370). Wheel zoom ×2 + settle + reload →
    camera restored to the float (cell:
    zoom 3.5089…, x −1605.71…, y −1003.57…).
  - turn (uuid drill episode): Ctrl+Enter → turn cell :open BEFORE agent
    → provisional (activity + stream, dim) → busy refusal on a second
    send, AT the block, amber, transient
    (`images/p2b-busy-refusal.png`) → SOURCE BLOCK EDITED MID-STREAM
    (seq 47→65 while streaming) → distill: provisional REPLACED by the
    durable machine reply "ACK", silver edge tint (Law 6), beneath the
    source, left-aligned, position settled durable; reload returns both
    blocks exactly (`images/p2b-reply-block.png`,
    `images/p2b-reply-after-reload.png`). **The pin held**: the turn
    cell reads status :complete + the SEND-TIME text (no
    EDITED-MID-STREAM) + send-time position.
- **Forced-stale finding (recorded, not a failure):** a REUSED settle-id
  with different geometry is journaled as a REPLAY of the original
  accept — the stale content NEVER lands (cell read back at the first
  acked value). Truth is protected by idempotent convergence, not by
  rejection; the client's visible-revert path fires on any :rejected/
  non-2xx settle response (code + unit-tested response shapes), and the
  same-key/different-content class cannot arise from the client's own
  protocol (fresh settle-id per fire; retries re-send identical bodies).
- **One live falsification banked:** reconcile! originally stored the
  served context at fn END while rebuild-block! read truth THROUGH it —
  every block rendered from the PREVIOUS pull on the first paint after
  boot (empty 26px boxes; the drag drill missed and panned). Fixed:
  context lands first. The class: state written after its readers run.

### G4b verdict — PASS at destructive grade (clause map)

- fresh world = zero content pixels + no hidden input — PASS (drill 1 +
  screenshot; the only key sink is the routed consumer).
- click + Escape mints nothing — PASS (client mode transitions + server
  native-row count stayed 0).
- burst typing → emitted = accepted = final committed, one revision for
  text+caret — PASS (37/37 acked; truth byte-exact).
- reload mid-burst → exactly the acknowledged prefix — PASS destructively
  (44 typed, 31 acked at the kill, truth = the 31-char prefix, clean).
- Ctrl+Enter durable-BEFORE-agent + pin under mid-stream edits — PASS
  (endpoint event order + turn cell :open before spawn; cell holds
  send-time text/position while the source advanced seq 47→65 mid-stream).
- second send during a turn → visible busy refusal — PASS (amber notice AT
  the block, transient, no queue; other blocks stayed writable — the
  mid-stream edit IS the receipt).
- kill client mid-stream → no provisional survives as durable (structural:
  provisional state is client-only; nothing of it in truth) · the turn
  completed server-side and its DISTILLED reply returned at the next boot,
  placed beneath its source FROM THE DURABLE TURN RECORDS (the client's
  session memory was dead) — PASS. The honest-open branch is structural:
  the cell flips from :open only via the completion write.
- drag → settle-ack → kill the browser → exact world-scoped positions +
  camera return — PASS (browser-kill grade). The literal power-cycle half
  rides the proven class: geometry/turn cells are
  $$transcript-conversation-projection rows on the durable cluster — the
  exact PState the mid-P2 REAL power-cycle (Sid's G5c) already carried
  whole; graceful close is not relied on (fresh-context reads after hard
  page death). A literal PC re-cycle is Sid's optional re-drill.
- forced-stale geometry write — the kernel journals a same-key conflicting
  write as a REPLAY (truth stays at the acked value; stale content never
  lands); the visible-revert path covers :rejected/non-2xx responses. See
  the finding above — recorded honestly, not drilled as a browser revert.
- return restores no focus/caret/hover/selection — PASS (drill 4 + moment-6
  screenshot: material at rest).
- feel bar: narrow echo p50 13 / **p95 18.9** / p99 28 ms (n=48) on the
  spatial face — bar 52ms — PASS with 2.7× headroom.

### Honest lacks (named, wish-fodder or later)

- In-edit drag = caret placement, not text selection yet (selection needs
  render + delete-selection through the queue; §9.4 names it wish-fodder).
- :up/:down caret moves ride ground-edit's line math; :word-left/right
  no-op in blocks.
- A paste AS the first content act with multi-paragraph text births
  multiple units (free-cut grain); focus lands on the first.
- Kill-mid-stream leaves the CLI turn's material in the episode jsonl;
  the NEXT turn's incremental harvest converges it (offset cursor) — the
  turn cell stays the honest :open/:failed fact meanwhile.
- Hover picks run per mousemove (cheap at genesis scale; batch later).

