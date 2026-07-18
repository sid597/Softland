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

