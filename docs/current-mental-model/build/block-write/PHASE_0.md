# block-write — PHASE 0: `document-container-id` resolution

Phase-0 agent, fresh context, 2026-07-12. One job: pin how the edit-request
field `document-container-id` (CONTRACT §3 envelope, §4 resolution table) is
resolved. No code changed. Every load-bearing claim cites file:line, verified at
this HEAD.

---

## Verdict

**PINNED — (b) projection field.**

The additive field carries the **derived unit's OWN `document-container-id`**
(read from `DerivedUnitRow`), threaded projection → face → edit envelope. The
client never computes it and never tracks a container id (consistent with §3).

This is **not** `S1-AMBIGUOUS`, but it clears the S1 bar for an honest reason
worth stating up front: for consumer 1's *echo* the two readings are
functionally identical (both produce a working round-trip). The discriminator is
**provenance correctness (map-must-not-lie, CONTRACT §1 "The map does not lie")**,
which is binding and which candidate (a) violates. See "Why not S1" below.

---

## The decisive trace — what `edit-effects` DOES with the field

`edit-effects` binds `document-id (payload-document-container-id payload)`
(`object_container.clj:1427`) and uses it in exactly three places:

1. **Container-kind discriminator** — `container-row` is built with kind
   `(if (= document-id container-id) :document :text-block)`
   (`object_container.clj:1466`). For a graduating derived-unit,
   `container-id = (block-container-id object-key unit-local-id)` =
   `"oc:block:<object-key>:<unit-local-id>"` (`:1430-1431`, `:222-224`). Neither
   candidate value can equal an `oc:block:` id, so the kind is **always
   `:text-block`** regardless of the field's value. Robust to both readings.

2. **Stored lineage pointer** — `document-id` is written into the graduated
   `ObjectContainerRow.document-id` field (`object_container.clj:1475`) and into
   both event payloads (`->ObjectGraduatedPayload` / `->ObjectRevisedPayload`,
   `:1441-1455`). This is a **durable provenance record** of the block's document
   parent. It is consumed by other read surfaces —
   `trail_view.clj:592-594` renders `:entry/target {:id (:document-container-id
   row) :kind :doc}` and the entry address from the same value.

3. **Outline join (topology read)** — in the stream topology the field is used as
   a keypath: `(local-select> [(keypath *document-id *block-path)]
   $$outline-by-document :> *outline-node)` (`object_container.clj:2316-2318`).
   **But** `$$outline-by-document` is written **only** for `:markdown-outline`
   imports (`:2176-2187`); transcript imports write no outline rows. So for
   consumer-1 transcript blocks this join returns **nil under either reading** →
   no outline update. Not a discriminator here.

**Must it equal the unit's ingest-time document container?**
Functionally, **no**: validation only requires the field be a non-empty string
(`edit-request-validation-errors`, `object_container.clj:573-575`); there is no
equality check, and the read-back overlay does **not** consult it — `read-unit`
overlays edited content via the graduation row keyed by **unit-id**
(`$$unit-graduations-by-id`, written at `object_container.clj:2388-2392`;
`runtime.clj:310-312`), never via `document-id`. So the echo works with any
non-empty value.

Provenance-wise, **yes**: use 2 stamps the graduated container's document parent.
The graduated container's `source-unit-id` is the edited derived unit
(`object_container.clj:1424`, `:1474`), so its `document-id` must equal that
**source unit's own `document-container-id`** or the durable map lies about which
document the block came from — and `trail_view` will mis-attribute it.

## The value the kernel expects (why (a) is a wrong value, not just a worse one)

The units the reader face renders are **distiller-minted `DerivedUnitRow`s**, not
the coarse per-line source rows. Their `document-container-id` is set per event:

- `DerivedUnitRow` field 2 is `document-container-id`
  (`object_container.clj:113-116`).
- The distiller sets it from `event-ctx`:
  `:document-container-id (tid/chat-message-id object-key (core/sha-256 (str
  event-uuid)))` (`block_distiller.clj:740`), threaded into every unit
  (`:608`, `:618`, `:627`) and into sub-span refinements (`:1102`).

So a reader-face unit's true document container is a **per-event
`chat-message-id`** — e.g. `"oc:chat-message:<object-key>:<sha256(event-uuid)>"`
(`transcript_identity.clj:18-20`).

Candidate (a) computes `(tid/chat-conversation-id object-key)` =
`"oc:chat-conversation:<object-key>"` (`transcript_identity.clj:14-16`). That is
the **conversation** container / conversation-projection key — the value
`river-page` uses to READ the message ledger (`block_distiller.clj:1323`) and the
value the coarse `transcript_adapter` source path stamps
(`transcript_adapter.clj:254,260`). It is **not** the reader-face unit's document
parent. The two ingest paths genuinely disagree on granularity; the reader face
renders the distiller path, whose grain is per-message. Deriving (a) therefore
stamps the graduated container with a document parent the source unit **never
had**.

---

## The resolution rule the lanes implement — (b), end to end

**Field name:** `document-container-id` on the served block (no new name; it is
the existing `DerivedUnitRow` field surfaced through the projection).

**Source of truth:** `DerivedUnitRow.document-container-id`
(`object_container.clj:114`), already read per block via `read-unit` inside
`render-river-source` — the row is bound as `unit`
(`block_distiller.clj:1288-1289`).

**Binding, which key on which map, threaded through which function
(identity⇒data-resolution rule):**

1. **Lane A — projection add (server, additive):** in `render-river-source`'s
   per-block map (`block_distiller.clj:1290-1301`, the map that already carries
   `:unit-id :form :text :block-path` off `unit`), add one key:
   `:document-container-id (:document-container-id unit)`. No new read (the unit
   is already read), so the seek plan is unchanged — **G9 read-plan conservation
   holds** (`block_distiller.clj:1374-1395`).

2. **Lane A — face thread-through:** in `face-projection/blocks->turns` the face
   block map (`face_projection.clj:80-88`) currently keeps only
   `:id/:kind/:text/:order/:time-ms/:part-path/:block-path`. Add
   `:document-container-id (:document-container-id b)` so the served face block
   carries it alongside `:id`. (Purely additive; MC-T8-style byte-stability of
   existing keys preserved.)

3. **Lane B — client envelope:** the edit outbox reads `document-container-id`
   off the **focused face block** it is already holding (the same block whose
   `:id` becomes `target/id`) and copies it verbatim into payload
   `{document-container-id ...}`. The client computes nothing and tracks no
   container id — it echoes the value the face served, exactly as §3 requires.

Net invariant restored: the graduated `ObjectContainerRow.document-id` =
its `source-unit`'s `document-container-id`. The map does not lie.

---

## Scope note (consumer 1)

- **Covered:** Sid editing **conversation (transcript river) blocks** in the
  reader face — the only block class `river-page` serves today
  (`face_projection.clj:16-19`, `block_distiller.clj:1305`), all distiller-minted
  `DerivedUnitRow`s whose `document-container-id` is a per-event
  `chat-message-id`. For these, the projection field carries the correct value.
- **Outside this pin (extension points, each honest under the rule because the
  field is read straight from the row, whatever its grain):**
  - Markdown-doc / sense-block units (their `document-container-id` is a document
    container; they also carry real `$$outline-by-document` rows, `:2176`, so the
    outline join at `:2316` becomes live — the additive-field rule still serves
    the right value, but the outline interaction is only exercised when those
    classes get an edit affordance).
  - Assembly / design-medium revisions (consumer 2) and `:object-container`
    target edits — the `:object-container` topology branch reads `document-id`
    the same way (`object_container.clj:2332`); the field rule generalizes, not
    proven here.

---

## Rejected reading — (a) derive via `chat-conversation-id`

Rejected. `(tid/chat-conversation-id object-key)` yields the conversation
container, but the reader-face unit's actual document container is a per-event
`chat-message-id` (`block_distiller.clj:740`). Writing (a) would stamp the
graduated container — and every provenance surface that reads
`row-document-container-id` (`trail_view.clj:592`) — with a document parent the
source unit never had: a durable provenance lie the echo would nonetheless
"work" through, which is precisely the trap map-must-not-lie exists to catch.

## Why not S1

S1 fires only if kernel semantics **and** contract fail to discriminate two
implementable readings. Both are implementable, and the *functional echo* does
not discriminate them (the honest S1-shaped part). But **provenance semantics
do**: (a) produces a false durable `document-id`; (b) preserves the source unit's
true one. The binding invariant (CONTRACT §1) breaks the tie, so the plan pins
rather than escalates.

## Contract correction (divergence, surfaced per CLAUDE.md)

CONTRACT §4's resolution-table framing of candidate (a) — "derivable via
`tid/chat-conversation-id` … used by river-page `:1323`" — rests on a category
slip: river-page:1323 uses `chat-conversation-id` as the **conversation-projection
read key**, not as any block's document container. The two are different
containers at different grains. The table's (a) is therefore not a "cleaner
derivation of the same value" — it is a different, wrong value. This artifact
supersedes that row: `document-container-id` resolves by (b), carrying the unit's
own field.

---

### Verified vs inferred

- **Verified (read this session):** `edit-effects` uses of `document-id`
  (`:1427,1466,1475,1441-1455`); validation is non-empty-only (`:573-575`);
  graduation overlay keyed by unit-id (`:2388-2392`, `runtime.clj:310`); outline
  written only for markdown (`:2176`); distiller unit `document-container-id` =
  per-event `chat-message-id` (`block_distiller.clj:740,627`); coarse source path
  uses `chat-conversation-id` (`transcript_adapter.clj:254`); trail-view consumes
  `document-container-id` as a `:doc` target (`trail_view.clj:592`); block maps at
  `block_distiller.clj:1290-1301` and `face_projection.clj:80-88`.
- **Inferred (not executed):** that no *other* consumer-1 read joins on the
  container's `document-id` for the echo — argued from the graduation-overlay key
  and the read-plan, not from an exhaustive PState-reader census. G1/G2 will
  confirm the overlay path empirically; if some class reads container.document-id
  on the echo path, that only strengthens (b).
