# block-write — CONTRACT v1

Authored by Fable 2026-07-12. Binding per the work-package skill; decisions.md
(settled ground) outranks this file; this file outranks every baton/thread doc.
Every load-bearing claim below was source-verified this session at code HEAD
(see §11 input manifest).

## 1 · Purpose, evidence, consumers, scope

**Purpose.** Connect Sid's fingers to the land's existing write organ: editing
a block in the reader face becomes a durable Rama write whose echo is the
render. After this package, what you see after typing IS materialized truth —
no optimistic text echo, no local-only edits.

**Evidence chain (do not reopen).** Microbatch direct-write measured ~210ms
cadence and was rejected; stream direct-write measured echo p95 7.66ms /
0/1620 stalls and was committed (decisions.md "What we're building NOW" →
Writing; numbers + method in `build/write-echo/NOW.md`). The transport is
settled: **direct write over a STREAM topology, echo streamed back.**

**The discovered fact this contract is built on (verified, §11):** the write
organ already exists. The object-container kernel is a stream topology
(`object_container.clj:1725`) with a full edit request path:

- `:object/edit` request type, validated end-to-end (`:558-598`): content
  hash, partition key = object-key, idempotency key, edit-client-id +
  edit-seq, actor authorization.
- Graduation model (`edit-effects`, `:1390-1537`): first edit of an imported
  block **graduates** it (`:object/graduated`) into an owned
  `ObjectContainerRow` kind `:text-block`; later edits are `:object/revised`
  with a parent-revision chain (`RevisionRow`). The import underneath is
  never mutated; lineage rides `UnitGraduationRow`. The map does not lie.
- Ordering protection: `EditOrderRow` + `stale-edit?` (`:1366-1379`) rejects
  out-of-order (client-id, seq) writes as durable `:edit/stale` decisions.
- Idempotency journal: `$$decisions-by-idempotency` (`:1730`),
  **object-key-scoped** (rides `hash-by :partition/key`, `:1723`).
- Read-back already overlays edits: `read-unit` "performs the unit +
  graduation point reads" (`block_distiller.clj:1317`), and `river-page`
  (`:1305`) — the reader face's only block-material source
  (`face_projection.clj:16-19`) — is built on it.

**What is missing (the package):** nobody CALLS this from a surface. The only
declared `:object/edit` actors are import adapters. There is no edit
affordance on the reader face, no client request outbox, no echo wiring from
an accepted edit to a face re-pull, and no measured browser E2E. Block-write
builds exactly that connection and proves it.

**Consumers, in order:**
1. **Sid editing conversation blocks in the reader face** (this package
   builds + proves it).
2. Design-medium writes — assembly revisions ride the SAME organ (the
   assembly adapter already declares `:object/edit` capability,
   `assembly_adapter.clj:380`). Extension point; not built here.
3. The zoom-100 file editor's migration off the microbatch text-kernel.
   Extension point; not built here (text-kernel stays untouched).

## 2 · Placement ruling

**No new Rama module. No new depot. No new topology.** The write path is the
existing `*object-container-requests-depot` (`object_container.clj:1723`) →
object-container stream topology → `:object/edit`. Client work lands in a new
workspace namespace (Lane B); server work is **additive-only** on the
projection/serve layer (Lane A). 

Rejected alternatives, with grounds:
- **A new stream write module** (the shape the write-echo probe used): the
  probe proved the topology CLASS, not a module boundary. Building it for
  real would create a SECOND write channel beside `:object/edit` — two truth
  paths for the same blocks. That is the parallel-path failure CLAUDE.md's
  top section forbids.
- **Text-kernel (microbatch) write-through:** measured, rejected, settled.
  Not reopened by this contract in any form.

**Reversal cost:** low by construction. The client outbox is one namespace;
projection additions are additive keys; if `:object/edit` proves the wrong
grain for typing, the seam is the outbox (swap the request shape), not the
face or the kernel.

## 3 · The write loop (the ride)

Focused block → local edit buffer (the in-flight keystroke state; NOT an
optimistic echo — it renders as pending-input inside the focused block only)
→ per-keystroke request → `append-object-container-request!`
(`runtime.clj:90`) with `:ack` → decision + event + rows materialize (same
stream event) → epoch bump → generic `FacePull` re-read → block re-renders
from truth; caret position derives from the SAME pulled signal as the text.

- **Envelope (all fields existing kernel vocabulary, none invented):**
  `request-id` (client-minted, unique per keystroke) · `idempotency-key`
  **derived deterministically from request-id** (settled op-id law;
  object-key-scoped) · `edit-client-id` (fresh per editing session) ·
  `edit-seq` (monotonic per client-id) · `actor` = sid with capability
  `:object/edit` (`core/authorized-request?`, `core.clj:343`) · `target`
  `{:target/kind :derived-unit :target/id <unit-id>}` · payload
  `{document-container-id, object-key, content-text, content-hash}`.
- **Target stability:** the client targets the derived-unit id FOREVER, even
  after graduation — the kernel resolves unit→container through the
  graduation join (`edit-effects` `:1420-1432`, verified). The client never
  tracks container ids.
- **Grain:** keystroke-grain, open-loop, no pre-batching to flatter echo
  (event-grain law; write-echo pinned it). Volume is trivial; revision
  compaction is a named LATER (§9).
- **Echo definition** (for G7): keydown → `:ack` return (ack ⇒ event tree
  complete ⇒ materialized; measured p95 3.90ms on this substrate) → epoch
  bump → FacePull → paint.

## 4 · Identity AND data-resolution (one section, by rule)

**Block identity** = `unit-id`, already the face block's `:id`
(`face_projection.clj:82`). **Conversation identity** = the face `:address`
(the conversation object-key, `face_projection.clj:362-385`), which IS the
edit request's `object-key` and partition key.

**Resolution table — where each request field comes from:**

| field | source | status |
|---|---|---|
| `object-key` / partition key | face request `:address` | exists |
| `target/id` | block `:id` (= unit-id) in face data | exists |
| `content-text` / `content-hash` | client buffer; sha via the kernel's own `source-hash` rule (`:554`) | Lane B |
| `document-container-id` | **must be pinned by the plan** — candidates verified: derivable via `tid/chat-conversation-id` (`transcript_identity.clj:14`, used by river-page `:1323`) or carried as ONE additive river-page/projection field | Phase 0 pins; stop-clause if ambiguous |
| `request-id`, `idempotency-key`, `edit-client-id`, `edit-seq` | client-minted (op-id law) | Lane B |

Per the skill's identity⇒data-resolution rule: if Phase 0 pins
`document-container-id` as a projection field, the SAME phase artifact states
the face binding that carries it — no static code map beside a fixed schema.

## 5 · Echo wiring + the pre-named narrowing seam

Primary echo: on `:ack`, bump the conversation's face epoch (server mirror
atom, the S40/artery pattern — `WatchIngestEpoch` + generic `FacePull`,
`electric_flow.cljc:478-545`) → debounced re-pull → re-render. Ack ⇒
materialized, so the bump never races truth.

Per-keystroke full `river-page` re-pull at 12/s is bounded (seek bound
`1 + 4*limit`, `block_distiller.clj:1314-1318`) but unproven at this cadence:
**G7 owns the verdict.** If G7's first measure misses budget, the pre-named
narrowing is a **single-unit pull** (a projection serve for one edited block
via `read-unit`) — narrower read, same transport, same criterion. Transport
and criterion are NOT reopened by that narrowing.

Caret law (L8): caret position and block text derive from ONE pulled signal —
never two signals combined downstream. While a block is focused, in-flight
keystrokes render as pending-input in that block only; committed truth
replaces it on pull. Strict rule: pending-input may never render OUTSIDE the
focused block, and a lost echo leaves the block on last-materialized truth,
never on phantom text.

**Refusal visibility (map must not lie):** a rejected decision
(`:edit/stale`, `:actor-not-authorized`, validation) must SURFACE — the
focused block reverts to materialized truth and shows a refusal notice with
the decision reason. Silent drops are a gate failure (G5).

## 6 · Policy seam (built: the seam; not built: the vocabulary)

Authority is checked server-side at the decision step and is ALREADY data:
actor capability sets (`core.clj:343-350`) + `edit-target-kinds`
(`object_container.clj:547`). Consumer 1 ships with exactly that — sid's
actor carries `:object/edit`; every accept/reject is a durable decision row.

NOT built here (its own session, opening prompt at
`docs/sessions/policy-design-opening-prompt-2026-07-12.md`): the policy
vocabulary — per-component policy ("what can this component do", authored in
design, as data) and policy visibility/granting ("which policies can a
component even know/bind"). The contract guarantees only: the decision step
is the single enforcement point, policy inputs are data, and affordance
rendering (client) never substitutes for the server check. Whatever the
design round produces plugs into THIS seam without re-plumbing.

## 7 · Traps ledger (cite BW-T numbers in code comments)

- **BW-T1 second write pipe.** Naive: build the probe's module for real →
  two truth channels for blocks. Ruling: `:object/edit` only; a wall here
  means re-derive, never a parallel path.
- **BW-T2 microbatch rescue.** Naive: "just tune the text-kernel" →
  measured cadence, settled rejection. Any text-kernel write-through is out.
- **BW-T3 pre-batching keystrokes** to flatter echo → forbidden; grain is
  the event, echo is measured at keystroke grain (12/s open-loop).
- **BW-T4 optimistic text echo** → forbidden (settled). Pending-input inside
  the focused block is input state, not an echo channel; it never renders
  outside the focused block and never survives a refusal or a lost echo
  (block falls back to materialized truth). G6/G8 exercise this boundary.
- **BW-T5 idempotency-key reuse across different content** → the journal
  no-ops the second write and masks a real edit. Op-id = f(request-id),
  fresh per keystroke; replay of the SAME request must be byte-identical →
  journaled no-op (G3).
- **BW-T6 caret on a second signal** → L8 diamond glitch: text/caret tear at
  burst. One `m/latest` over the one pulled source.
- **BW-T7 client tracks graduated container-id** → drift + a second
  identity. Client targets unit-id forever; kernel resolves (verified
  `:1420-1432`).
- **BW-T8 edit-seq across reconnects** → resumed old client-id with reset
  seq = `:edit/stale` storm. Fresh edit-client-id per editing session; seq
  monotonic within it (the kernel keys staleness by (client-id, seq),
  `:1374-1379`).
- **BW-T9 epoch bump from render path** → R3 violation (side effects in
  `m/latest`). Bumps happen in the ack continuation/event callback only.
- **BW-T10 debounce swallowing the final pull** → last keystroke's truth
  never renders. The re-pull debounce must be trailing-edge-inclusive (the
  artery's existing debounced-epoch shape; verify, don't assume).

## 8 · Acceptance gates (each executable; one-liners carry their carve-outs)

- **G1 round-trip (IPC):** append `:object/edit` on a real imported
  derived-unit → `:ack` → `read-unit` returns the edited content via the
  graduation overlay (first edit = `:object/graduated`).
- **G2 revision chain (IPC):** second edit → `:object/revised`;
  parent-revision-id links; both revisions durable in `$$revisions-by-id`.
- **G3 replay (IPC):** re-append the byte-identical request (same
  request-id + idempotency-key) → journaled no-op; PState state
  byte-identical, asserted via a physical PState reader (never the public
  query surface).
- **G4 stale ordering (IPC):** out-of-order edit-seq → durable `:edit/stale`
  rejection; content unchanged (physical reader).
- **G5 refusal visibility (IPC + client unit):** actor without
  `:object/edit` → durable rejection; client surfaces the reason and reverts
  the buffer (no silent drop).
- **G6 caret/text co-variance (JVM/unit):** one-signal derivation — a burst
  of pulls never renders caret from pull N and text from pull N±1; and
  pending-input never renders outside the focused block.
- **G7 browser E2E echo (headed, real reader face):** 60s sustained 12/s
  keystroke-grain edits on a real conversation block: **echo p95 ≤ 50ms AND
  ≤1 stall >100ms** (the settled criterion, third reuse, unchanged), echo =
  keydown→painted truth; report the numbers in the gate artifact. The §5
  narrowing seam (single-unit pull) may be engaged to pass; transport and
  criterion may not change.
- **G8 wearing (live, before daily use):** Sid edits a real conversation
  block in the reader face; edited content survives app restart; the
  underlying import rows are bit-unchanged (lineage intact via graduation
  rows); a mid-word refusal (forced stale) visibly reverts.
- **G9 read-plan conservation (IPC):** untouched blocks' river-page
  read-plan metadata unchanged by any projection addition (seek bound stays
  `1 + 4*limit`).

## 9 · Non-goals (each an extension point, none a void)

- **Policy vocabulary + visibility/granting** → its own design session
  (opening prompt written; runs in PARALLEL, no upstream blocker).
- **Design-layer edit mode** (click component → edit assembly) → separate
  package; its missing prerequisite is the interpreter provenance stamp
  (rt-nodes → assembly path), a small named faces-world item. Assembly saves
  will ride THIS organ (consumer 2).
- **Block create / delete / split / merge** → edit-of-existing-content only
  in v0; creation is the next op on the same envelope.
- **File-editor (text-kernel) migration** → rider 3, untouched here.
- **Multi-writer conflict machinery** → single writer today; the kernel's
  stale-seq protection is the v0 answer; CRDT/OT waits for its form-break.
- **Revision compaction** → keystroke-grain revisions accumulate by design
  (history is terrain); a compaction/stratum policy is LATER.
- **Clustered-substrate re-measure** → pre-registered at substrate change
  (settled); not this package.

## 10 · Stop clauses (escalate, never improvise)

- **S1:** Phase 0 finds `document-container-id` resolution genuinely
  ambiguous (two physically-implementable readings) → escalate with both,
  recommendation attached.
- **S2:** G1 shows the graduation overlay does NOT return revised content
  through `read-unit`/river-page in some class of blocks → the §1 discovered
  fact broke; stop, re-verify, contract amends.
- **S3:** G7 fails AFTER the §5 narrowing → returns to Sid with numbers;
  pre-registered next: local caret affordance only (text truth still
  streamed); full optimistic stays off the table (settled).

## 11 · Input manifest (reproduce-the-contract set; verified 2026-07-12)

- `src/app/server/rama/object_container.clj` — `:1723-1745` (depot +
  PStates), `:547-598` (edit validation), `:1366-1379` (stale),
  `:1390-1549` (edit-effects), `:343-350` in `core.clj` (authorization).
- `src/app/server/rama/object_container/block_distiller.clj` — `:1305-1335`
  (river-page + seek bound), `:1317` (graduation point reads), `:594`
  (distiller actor capabilities).
- `src/app/server/rama/object_container/runtime.clj` — `:78-81` (query
  handles), `:90` (append helper), `:133+` (read fns).
- `src/app/server/rama/face_projection.clj` — `:1-29` (read discipline),
  `:60-90` (block shape), `:362-385` (face request shape).
- `src/app/server/rama/object_container/transcript_identity.clj` — `:14`.
- `src/app/electric_flow.cljc` — `:478-545` (artery: epochs + FacePull).
- `build/write-echo/NOW.md` — both measurement records (criterion, numbers).
- decisions.md — "What we're building NOW" → Writing + "Settled architecture"
  (op-id law, back-arrow, two clocks).

## 12 · Handoff

- **Lanes** (dispatch is Sid's — subagent waves spend money): **Lane A**
  (server-additive): projection field pin + single-unit pull serve (if §5
  narrowing pre-built: no — build only on G7 miss) + epoch bump on edit
  events + gates G1-G5, G9. **Lane B** (client): reader-face edit affordance,
  request outbox (envelope §3), caret/pending-input law, G6. **INT:** G7
  measure + G8 wearing, this orchestrating session.
- **Coordination flag (board, T11 class):** machine-cut's code is COMMITTED
  (`ceb84da`); its CLOSE session may still land HEAD-suite fixes touching
  `face_projection.clj`/artery. Lanes may dispatch on Sid's go; if the close
  session opens face-file fixes concurrently, coordinate via the board line.
- **After green:** wearing (G8) → gate review (falsification pass per
  CLAUDE.md) → close + retro per the skill; consumer-2 (assembly saves) and
  the policy session's output land as follow-ons on this seam.
