# Block-distiller Phase 1 — opening prompt (fresh session)

**You are the P1 implementation session** of the block-kernel work package
(`docs/current-mental-model/build/sense-line-mvp/block-kernel/`). P0 is DONE and
GREEN. Your job: **per-part surfaces + OC import** — the §G row builders + §I
driver — with the **F2 delegation spike FIRST**, then the physical-read IPC
gates. Code is an ADAPTER over the existing object-container module (git-spine
shape); you add NO module/depot/PState/topology.

## Boot, in order
1. **Load skills:** `/work-package`, `/rama`, **`/rama-pitfalls`** — the last is
   REQUIRED *before you write any foreign-append code*. P1 is the first
   foreign-client surface in this package (the driver appends into the OC
   depot); run pitfalls against the driver design (append shape · ack level ·
   partition key · back-arrow · idempotency/retry-safety) before coding.
2. **Binding docs** (order: `decisions.md` › `SPEC.md` › `CONTRACT.md` ›
   `PLAN.md` › `PHASE_0.md`): read CONTRACT §2–§9, SPEC §1–§9, PLAN §0/§2/§3/§5/§7.
3. **The P0 code you build on:**
   `src/app/server/rama/object_container/block_distiller.clj` (pure layer) +
   `test/app/server/rama/object_container/block_distiller_test.clj` +
   `test/resources/block-distiller/{fixture.jsonl,golden.edn}`.

## P0 state you INHERIT (verified this session — do NOT re-derive)
- **P0 green: 10 tests / 401 assertions / 0 fail.** Pure layer complete: §A ids,
  §B classify (river/debris), §C actor (role≠actor), §D production-event, §E
  `event-parts`, §F `free-cut-*` (markdown + fence/quote/table atomicity), §H
  edge specs, and `distill-event` (pure per-event assembler). The golden asserts
  forms+spans+text exactly.
- **F1 FALSIFIED by the spike** (plan F1 was WRONG): 0/40 file-history-snapshot
  and 0/321 REAL river events throw on `read-string`. Leading-slash path keys
  parse to `ns=""` and round-trip fine; the real non-EDN shape is a *whitespace*
  key, none in `7c80ce2a`. ⇒ `read-string` of river payloads is SAFE. classify-
  first + per-event try/catch stays as defense-in-depth, but do NOT architect P1
  around a debris-read-string landmine that does not exist.
- **id routing VERIFIED against `extract-object-key`** (object_container.clj:284-339):
  `du:chat:HASH:sense-block-v0:…` → `chat:HASH` ✓; `src:tr:chat:HASH:…` →
  `chat:HASH` ✓. **Trap:** anchor-ids are `sa:du:chat:HASH:…` and
  `leading-object-key("du:chat:…")` = `"du"` (WRONG). Anchors are stored via the
  import's `:partition/key` and READ from `$$source-anchors-by-target` keyed by
  **target-id (the unit-id)** — never route/read by anchor-id.
- **Micro-deviation from the plan (logged, Sid may override):** image parts →
  **surface-only** (no eager block), not the plan's `material-part` form — an
  empty `[0,0)` span would fail G11; SPEC §4.3 (surface only) is consistent.
- **tool_result → surface only, NO block** (SPEC §4.3). The coarse
  `tool-result-span` block mints lazily on edge-demand in **P3b** (not P1).
- **Runtime toolkit** (`object_container/runtime.clj`, all verified):
  `ocr/start-object-container-runtime!` launches OC + transcript-ops on ONE ipc,
  returns every handle. `ocr/append-object-container-request!` with
  **`:append-ack`** = the deterministic barrier for the OC STREAM topology (NOT
  polling; not microbatch). Reads: `read-transcript-conversation-projection`
  (ordered, by conversation-container-id), `read-source` (by source-id),
  `read-unit`, `read-source-anchors` (by target-id), `read-import-completion`,
  `read-common-material-for-source`. Test pattern: `object_container_test.clj:15-75`
  (`test-runtime`, `append-and-await!`).

## VERIFIED record ctors (positional — the shape trap; PLAN §3.1 abbreviates these)
```
(oc/->SourceArtifactRow  source-id source-ref source-hash source-format
                         source-raw-text document-container-id content-byte-count
                         created-at-ms created-by event-id)              ; 10; created-by = G1 actor home
(oc/->DerivedUnitRow     unit-id document-container-id source-id unit-kind
                         block-path parent-slot-id source-anchor-id
                         derived-content-text derived-content-hash
                         distiller-id distiller-version event-id)         ; 12
(oc/->SourceAnchorRow    source-anchor-id target-kind target-id source-id
                         source-ref source-hash start-offset end-offset
                         block-path event-id)                            ; 10
```
Import request = MIRROR `transcript_adapter.clj:221-264` (surface build) +
`markdown_adapter.clj:447-517` (payload wrapper + envelope). Payload wrapper keys
(PLAN §3.1): `:object-key :source-artifacts :object-containers [] :revisions []
:derived-units :source-anchors :composition-edges [] :source-versions []
:projection-hints :source-line-statuses []`. Envelope adds: `:partition/key
object-key`, `:object/key object-key`, `:import/key (import-key …)`,
`:idempotency/key (import-key …)`, `:material/fingerprint
(oc/import-material-fingerprint object-key import-key payload)`, `:routing/key
[:object-container/import object-key]`. object-key =
`(tid/transcript-object-key source conversation-id)`.

## P1 build order (de-risk; each ends on its own green)
1. **`/rama-pitfalls` pass** on the driver design (above).
2. **F2 SPIKE FIRST** — the fork that may reach Sid. Assert an extra
   `:production-event {…}` key on a `SourceArtifactRow` SURVIVES the
   `$$source-artifacts-by-id` round-trip (import → physical read → key present).
   - GREEN → delegation rides on the surface (option A, ZERO edit). Proceed.
   - **RED → STOP-CLAUSE → escalate to Sid.** Option B (a declared additive
     `production-event` field) forces editing `transcript_adapter.clj:255`'s
     positional `->SourceArtifactRow` call — OUTSIDE the file allowlist. Do NOT
     silently edit it; record in `decisions.md` Open Questions + baton NOW, stop.
3. **One-unit routing smoke:** ingest ONE fixture message via
   `transcript-adapter/transcript-observation-import-request`, run the driver
   over it, physically read `$$derived-units-by-id[unit-id]` → assert non-nil
   (proves object-key routing before the full wave — P1's most-likely-red).
4. **Full P1 import + gates.** Ingest all fixture lines via the transcript
   adapter FIRST (creates per-message surfaces + conversation projection the
   driver reads, F3), then `distill-conversation!` over river events.

## P1 gates — IPC deftests, physical PState readers (T7, validation-only)
- **G6** anchor honesty: `(subs source-raw-text start end) == block text`; any
  stored `derived-content-text` hash-equals its span; planted secret absent from
  EVERY row (inherited redaction).
- **G4** idempotence: re-run driver → zero new rows, identical ids (physical count via
  `$$derived-units-by-id`; import-key dedup at object_container.clj:1829-1830).
- **G5** strata: a 2nd distiller-id adds disjoint `du:…:<other>:…`; sense-block-v0
  rows byte-unchanged.
- **G3(physical)/G11(physical):** unit-kinds + spans via `$$derived-units-by-id`
  + `$$source-anchors-by-target` == golden; spans in-bounds/non-empty/surrogate-safe.

Barrier: `:append-ack` (OC stream), never polling. Note N5: `derived-content-text`
is STORED (verified cache — no query topology exposes anchor offsets, so
`read-unit` needs it; R5 sanctions this; G6 hash-checks it).

## The R4/SC2 mechanism you implement (per-part surfaces)
The driver reads each stored per-message `source-raw-text` (= `pr-str` of the
redacted payload), `edn/read-string`s it (SAFE for river — F1 falsified), and
for each part mints a `SourceArtifactRow` whose `source-raw-text` = the exact
redacted part text, `source-id = (per-part-source-id object-key event-uuid part-path)`
(already in P0 §A, reuses `src:tr:` prefix — N1), `document-container-id =
(tid/chat-message-id object-key message-key)`. Blocks anchor into these per-part
surfaces (G6). "No second store" = no re-ingest / no re-redaction, not no new rows.

## Stop clauses / hard rules
- F2 red → Sid. Any non-additive `object_container.clj` change → Sid. Any
  relation-kernel change (P3a's 3 kinds are pre-authorized) → Sid.
- File allowlist: NEW `block_distiller.clj` (extend it) + NEW test + fixtures.
  `object_container.clj` additive-only as P0-ruled; everything else = stop-clause.
- CODE uncommitted until Sid's word. Docs auto-commit on the local docs branch,
  never pushed/merged, never mixed with code. NEVER read `src/app/server/env.clj`.
- **Definition of P1 done:** F2 fork resolved + G6/G4/G5/G3(phys)/G11(phys) green
  as IPC deftests + the one-unit routing smoke passing.

## At session end
Append a NOW entry (≤15 lines) to `docs/sessions/next-prompt.md`; on a stop-clause,
record it in `decisions.md` Open Questions (PROPOSED) with verbatim citations and
STOP. Write the P2 opening prompt if P1 closes (P2 = wire class→entry-kind,
actor→created-by, delegation→its P1-decided home; gates G1, G2).
