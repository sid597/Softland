# PHASE 0 — block-kernel requirements re-derivation + empirical P0-verify

Fresh-context, READ-ONLY layer. Derives requirements from CONTRACT v2 + SPEC v0
and proves the eight load-bearing assumptions against ACTUAL source. Default-fail:
a check PASSES only with `file:line` evidence + quoted load-bearing lines.

Binding order followed: `decisions.md` › SPEC › CONTRACT › derived. Inherited
verbatim (not re-derived): CONTRACT §4 noun→row table, CONTRACT §5 flow.

Source read this pass (all under `src/app/server/rama/`): `object_container.clj`
(records 1-180; id helpers 200-258; fingerprint/validation/payload 480-795;
native-identity 1233-1279; **import topology 1692-2260**; query API 2443-2551),
`object_container/markdown_adapter.clj` (full), `object_container/transcript_adapter.clj`
(7-47,140-202,221-447,600-620), `dogfood/transcript.clj` (320-361,388-527),
`relation_kernel.clj` (1-76,104-395,600-720), `build/relation-kernel/CONTRACT.md`
§1-3, the real `7c80ce2a-…jsonl` head + full-file census, existing test harnesses
(`object_container_test.clj`, `relation_kernel_test.clj`, `git_spine_test.clj`),
`docs/reference/rama/{12,27,31}`.

---

## 0 · STOP-CLAUSE CANDIDATES (summary — read first)

**TWO candidates. Both need Sid before the plan phase closes; neither is fatal.**

1. **(a) Relation-kernel kind vocabulary is ENUM-GATED and 3 of the 5 needed
   kinds are UNREGISTERED.** `relation-kinds` (relation_kernel.clj:58-65) =
   `#{:based-on :produced :built-over :new-direction :dead-end :elaborates
   :references :confirms :refutes :supersedes}`. Of the CONTRACT §4 floor
   (`produced grounds assembled-from refines supersedes`): `:produced` ✓ and
   `:supersedes` ✓ are registered; **`:grounds`, `:assembled-from`, `:refines`
   are NOT.** Unregistered kinds are REJECTED at decision time (:327-328,
   `:relation/kind-unregistered`). This is exactly CONTRACT §9 "Relation-kernel
   kind vocabulary is enum-gated (§4)."
   → **§9 option:** the file's own comment (:55-57) sanctions "a one-line reviewed
   code change" to add kinds. **Recommendation: authorize adding
   `:grounds :assembled-from :refines` to `relation-kinds`.** Grounds: it is a
   3-word additive edit to a closed set that exists precisely to be extended by
   review; but CONTRACT §12 gates "any relation-kernel amendment" behind Sid, so
   it cannot start without his nod. Cheapest possible resolution; no design churn.

2. **(f/e) The already-ingested transcript sources hold NO clean per-part text
   to anchor honest blocks into.** `SourceArtifactRow.source-raw-text` for a
   parsed transcript message = `(pr-str redacted-payload)` — the whole event as
   an *EDN-printed Clojure map* (transcript_adapter.clj:251-259; the success obs
   sets `:transcript/redacted-payload` but NOT `:transcript/redacted-preview`,
   transcript.clj:509-516, so the `or` at :251 falls through to `pr-str`). For
   parse-error lines it is a **200-char preview** (transcript.clj:445). The clean
   prose exists only in the message container's `current-content-text`, which is a
   **lossy** `str/join "\n"` of text+tool_result parts that DROPS thinking/tool_use
   and part boundaries (transcript_adapter.clj:24-37). So no existing row stores
   the per-part canonical text SPEC §1 wants (`surface = (event-id, part-path)`),
   and blocks cut over `pr-str` would slice ESCAPED EDN, breaking G6 anchor honesty
   + SPEC §2.3 reconstruction.
   → **§9 option (R4 gate):** this is the "fenced additive request variant vs
   re-ingest under a distinct source-ref" fork. **Recommendation: the driver mints
   per-part text surfaces** (`source-ref = (event-id ∥ part-path)`, raw-text = the
   exact redacted part text) by `read-string`-ing the *already-stored* redacted
   payload (NOT re-reading the raw jsonl, NOT re-redacting — R1 preserved) and
   extracting typed parts, then anchors sense-blocks into those. Grounds: only this
   gives G6-honest anchors + SPEC §4.2 per-part forms; it reuses stored redacted
   material (no second redaction, no second chat file); the original per-message
   sources stay untouched. It stretches R4 "no second store," so it is Sid's call
   per §9/§12.

**NOT stop-clauses (verified clear):** (e)-import-mechanics ACCEPTS a second
distillation (§2 below); anchor-ids ARE deterministic (R3, item f); redaction IS
deterministic (R1, item b); OC tolerates units with nil derived-content-text (R5 —
no validation requires it, §2/e note). Design-notes for the plan (not gated):
(c) unclassified event types + `image` form; (g) river-page composition; (h)
additive delegation fields.

---

## 1 · Requirements re-derivation

### 1.1 · Driver operations, in order (from CONTRACT §5 + SPEC §3-8, verbatim flow)

The driver is plain Clojure, foreign-side, over the two kernels' PUBLIC doors
(ingest depot + query API for OC; assert depot for RK). It adds NO topology/PState.

1. **Read** the session's already-ingested material via the query API
   (`read-common-material-for-source` / `read-unit`), and/or `read-string` the
   stored redacted payloads. Never foreign-select PStates (G13). Big reads
   paginate (T10; the query API is cursor-based, §2/g).
2. **Classify** every event river/debris with a versioned classifier-id (SPEC §3.1;
   G1). Debris retained, unmarkable (T9).
3. **Resolve actor** — role ≠ actor (T1; SPEC §3.4): user-role+tool_result ⇒ `tool`;
   `isMeta`/command wrappers ⇒ `harness`; remaining user-role ⇒ human; assistant ⇒
   model id on-behalf-of the invoking chain.
4. **Mint per-part surfaces** (per candidate 2) and **free-cut** them with pure fns
   in markdown_adapter's shape (SPEC §4; G3): provider parts as given
   (thinking/text/tool_use); markdown units inside text (para/list-item/header/
   fence/quote/table; fences/tables/quotes atomic); tool_result = material, no
   pre-chunk; human message = whole block + silver subs (default ON). Every
   segmentation declares `rule-id@version` (SPEC §4.5). Offsets = UTF-16 (R2/T2).
5. **Mint deterministic ids** (R3): `du:<object-key>:sense-block-v0:<block-path>`
   (markdown_adapter:13-15 shape); anchors `sa:<unit-id>` (oc/source-anchor-id).
6. **Submit derived-unit import requests** to the OC depot, `:append-ack`, one
   `:partition/key = object-key` per conversation (T4). Re-declare the source row
   the anchors reference; keep object-containers empty (§2/e).
7. **Submit mechanical relation edges** to the RK depot (T11, driver-only): write-tool
   ⇒ `produced`, read-tool ⇒ `grounds`, assembly ⇒ `assembled-from`, refinement ⇒
   `refines`, authored succession ⇒ `supersedes`; target-kind `:block`; asserted-by
   `"sense-block/mechanical@1"`; silver; idempotency key `sha256(unit-id ∥ kind ∥
   target-id)`; **routing key = the deterministic relation-id** (§2/d).
8. **Serve the first river page** back in order for the dual benchmark (§2/g).

Retry axes (ARCHITECTURE §2, verified): driver re-run ⇒ deterministic ids converge
(G4); OC stream topology ⇒ idempotency/import-key gating (§2/e); RK microbatch ⇒
(relation-id, idempotency-key) journal (§2/d).

### 1.2 · Gate invariants restated as concrete assertions against real rows

| gate | concrete assertion (physical reader unless noted) |
|---|---|
| **G1** classify+actor | For every fixture event, a class value present; golden counts match; `foreign-select` over `$$derived-units-by-id` / the actor field shows ZERO tool_result-or-meta units attributed to the human. Physical PState read, not query API. |
| **G2** delegation | Model actor + on-behalf-of chain present on each production event; `parentUuid`/`isSidechain`/`promptId` captured per the P0-ruled realization (§2/h — additive fields). |
| **G3** free cut | `$$derived-units-by-id` unit-kinds + `$$source-anchors-by-target` spans equal the golden file byte-for-byte. |
| **G4** idempotence | Re-run the driver: `foreign-select` count of `$$derived-units-by-id` unchanged; ids identical (physical count reader). |
| **G5** strata | A second distiller-id adds its units under a DISJOINT `du:…:<other>:…` id space; `sense-block-v0` rows unchanged byte-for-byte. |
| **G6** anchor honesty | For each anchor, `subs(source-raw-text, start, end)` == expected text; any stored `derived-content-text` hash-equals its span (oc/source-hash); planted secret absent from EVERY row. **Depends on candidate 2** (anchor target must hold clean text). |
| **G7** refinement | A demand-minted sub-span unit exists with engagement provenance; the coarse unit + its (future) marks persist. |
| **G8** mechanical floor | `$$relations-by-id` holds `produced` (write-tool→block) and `grounds` (read-tool→block), asserted-by sense-block/mechanical, silver, from/to target-kind `:block`. **Depends on candidate 1.** |
| **G9** holes | An edge request with an absent endpoint persists + is queryable (RK holes; SPEC §9). |
| **G10** assembly | Two blocks → new `assembled` source + `assembled-from` edges + re-addressed (not copied) units. **`assembled-from` depends on candidate 1.** |
| **G11** span discipline | Property: every span in-bounds, non-empty, no surrogate split, child⊂parent. |
| **G12** (review) | River-page read plan = bounded seeks + sequential iteration; no N-point-read fan-out per page (§2/g). |
| **G13** (review) | Consumers compile against query topologies only; test validation readers exempt. |

### 1.3 · Entity × write matrix (CONTRACT T4 — ONE routing convention per kernel)

| thing written | request path (public door) | partition / routing key | lands in (physical PStates) |
|---|---|---|---|
| **SourceArtifactRow** (re-declared existing, or new per-part surface) | OC depot, `:object-container/import-material` | `:partition/key = object-key` (per-conversation) | `$$source-artifacts-by-id[source-id]` (oc:1940-1942) |
| **DerivedUnitRow** (sense-block-v0) | same OC import request | same `object-key` | `$$derived-units-by-id[unit-id]` + `$$source-derived-units-by-source[source-id, order-key=block-path]` (oc:2013-2029) |
| **SourceAnchorRow** (`sa:<unit-id>`) | same OC import request | same `object-key` | `$$source-anchors-by-target[target-id, anchor-id]` + `$$source-anchors-by-source[source-id, anchor-id]` (oc:2043-2057) |
| **CompositionEdgeRow** (containment) | same OC import request | same `object-key` | `$$composition-children-by-parent[parent-id, order-key]` + `$$composition-parent-by-child[child-id, parent-ref-key]` (oc:2077-2084) |
| **production-event fields** (actor + delegation) | same OC import request (additive projection-hint / additive row field — §2/h) | same `object-key` | a projection PState via the `:projection-kind` dispatch (oc:2145-2203) — see §2/h caveat |
| **relation edges** (produced/grounds/assembled-from/refines/supersedes) | RK depot, `:relation/assert` | `:relation/routing-key = relation-id` = `rel:sha1(kind,from,to,asserter)` (rk:104-113,617-621) | `$$relations-by-id[relation-id]` + `$$relations-by-target[target-key]×2` + descriptors + `$$relation-activity-by-bucket` (rk:702-724) |

Every OC write rides ONE key (`object-key`, per-conversation, `hash-by :partition/key`,
oc:1693). Every RK write rides `relation-id`. No second routing convention invented (T4).

---

## 2 · P0-verify (a)–(h)

### (a) relation-kernel kind-vocabulary openness — **STOP-CLAUSE CANDIDATE**

**Claim to prove:** edge `kind` is an open value (needs `produced grounds
assembled-from refines supersedes`); target-kind `:block` accepted.

**Evidence — kind is a CLOSED enum, 3 kinds missing:**
`relation_kernel.clj:55-65`
```
;; Closed code-level set. Requests with an unregistered kind are REJECTED at
;; decision time. Adding a kind is a one-line reviewed code change. …
(def relation-kinds
  #{:based-on :produced :built-over :new-direction :dead-end :elaborates :references
    … :confirms :refutes :supersedes})
```
`relation_kernel.clj:327-328` (in `request-shape-errors`)
```
(not (registered-kind? kind))
(conj {:type :relation/kind-unregistered :value kind})
```
`produced` ✓ `supersedes` ✓ registered; **`grounds` ✗ `assembled-from` ✗ `refines` ✗.**

**Evidence — target-kind IS open (this half passes):** `relation_kernel.clj:216`
comment `; keyword — :container :source :git-commit :doc-file :conversation :none (open)`;
`well-formed-target?` (:288-295) only checks `(keyword? (:target-kind ref))`, never
membership. `build/relation-kernel/CONTRACT.md:57-59`: `target-kind … keyword, open …
new kinds allowed`. So `:block` needs no change.

**Verdict: STOP-CLAUSE CANDIDATE.** Kind is enum-gated (CONTRACT §9 trigger) and 3
of 5 floor kinds are unregistered. §9 resolution + recommendation in §0.1. (Read, not
inferred.)

### (b) redaction determinism + versioning (R1) — **OK (versioning is by policy-name, note)**

**Claim:** redaction deterministic + recorded with rule-id@version; the block distiller
inherits the SAME canonical text, never re-redacts.

**Evidence — deterministic:** structural redaction is a fixed `sensitive-key?`
walk (`transcript.clj:452-487`, `redact-payload-with-redactions` — pure reduce-kv,
replaces sensitive values with `"[REDACTED]"`); text redaction is a fixed pattern
vector (`transcript.clj:391-436`, `text-redaction-patterns` = 6 constant regexes).
Same input ⇒ same output. Deterministic. ✓

**Evidence — never re-redacts (holds by construction):** the block distiller reads
ALREADY-STORED text (`source-raw-text`, itself post-redaction — the transcript adapter
stored `pr-str redacted-payload`, transcript_adapter.clj:251-259) and cuts it; it runs
NO redaction pass. Recommended candidate-2 path `read-string`s the stored redacted
payload — still no second redaction. ✓ So planted-secret gate G6 rides on the ORIGINAL
ingest having redacted correctly (it did, deterministically).

**Evidence — versioning is by policy NAME, not @version:** `object_container.clj:32`
`(def transcript-redaction-policies #{:standard})`; `transcript.clj:39,52-53`
`default-redaction-policy :standard` / `redaction-policies #{:standard}`. A
`policy-version` field exists on `TranscriptFileOffsetRow` (oc:167) but per-redaction
metadata records only `:redaction/kind`, `:redaction/pattern`|`:redaction/key`,
`:redaction/length` (transcript.clj:429-431,467-470) — **no numeric version token**;
the rule-id is the keyword `:standard`, carrying no `@version`.

**Verdict: OK.** Determinism holds; "never re-redacts" holds by construction (R1
satisfied). NOT the §9 "non-deterministic/unversioned" stop-clause. **Note for Sid:**
SPEC §2.1 literally says "recorded with rule-id@version"; today the rule-id is
`:standard` with no explicit version. Recommend the driver stamp the inherited policy
id (`:standard`) on its production provenance and treat `:standard` as `@1` until a
policy actually versions.

### (c) jsonl event-shape inventory vs SPEC §3 — **OK-with-findings (SPEC §15 fixture is stale; new types + `image` form)**

Real head of `7c80ce2a-…jsonl` (spans only, first 40 events) and full-file census:

**Full-file `type` census:** `assistant 199 · user 122 · attachment 111 · last-prompt
44 · mode 40 · permission-mode 40 · file-history-snapshot 40 · ai-title 39 ·
bridge-session 37 · custom-title 30 · agent-name 30 · system 27`.
**Content-part census:** `tool_use 79 · tool_result 79 · thinking 73 · text 66 · image 9`.
**Flag presence:** `isSidechain 459 (ALL) · parentUuid 459 (ALL) · userType 459 ·
promptId 121 · toolUseResult 79 · isMeta 39`.

**Mapping to SPEC §3 classes:** mode/permission-mode/file-history-snapshot ⇒
debris/harness-op ✓; `user`+isMeta ⇒ debris/meta ✓; `user`+tool_result ⇒ material
(actor `tool`, §3.4) ✓; `assistant` parts ⇒ river ✓.

**Findings (SPEC gaps, not stop-clauses):**
- **SPEC §15 fixture table is INACCURATE past event ~8.** Reality events 9-13 =
  `attachment`, 14 = `last-prompt`, 15-16 = mode/permission-mode, 17 = `ai-title`;
  the **first `assistant`/thinking is event 18, not event 9** as §15 claims. The
  committed G3/G1 golden must be regenerated from the live file, not §15.
- **Event types SPEC §3 never classified:** `attachment` (111), `last-prompt`,
  `ai-title`, `bridge-session`, `system`, `custom-title`, `agent-name`. All read as
  harness/UI debris EXCEPT `attachment`, which may carry user-pasted content (river)
  vs file-attach metadata (debris) — needs a rule. The classifier needs an explicit
  debris list + a default-debris fallback for unrecognized harness types (G1 depends
  on this).
- **`image` content parts (9)** have NO form in SPEC §4.6 (closed vocab). Free cut hits
  a part with no form. → Sid-gated: add an `image` form (§4.6 amendment) or a driver
  default (treat unknown parts as generic material). Minor.
- **Delegation raw is RICH:** `parentUuid`+`isSidechain` on 100% of message events,
  `promptId` on 121 — feeds (h) cheaply.

**Verdict: OK** for the river/debris SKELETON; the fixture table + classifier debris
list must be regenerated/extended from the real file, and `image` form is a Sid item.

### (d) relation idempotency-key scope — **OK (driver key conforms; also set routing=relation-id)**

**Claim:** RK dedups relation-scoped; the driver key `sha256(unit-id ∥ kind ∥
target-id)` conforms.

**Evidence:** `relation_kernel.clj:617-621` depot `(hash-by :relation/routing-key)`,
comment "`:relation/routing-key = relation-id`, so request, idempotency journal,
decision, event, status log, and the authoritative row all colocate on
hash(relation-id)." Journal gate `:672-678`:
```
;; duplicate (relation-id, key) → the first decision is already durable;
;; replay = write NOTHING and stop.
(local-select> [(keypath *relation-id *journal-key)] $$relation-decisions-by-idempotency :> *prior-decision)
(filter> (nil? *prior-decision))
```
Idempotency journal is keyed `[relation-id, idempotency-key]` (`:628-629`,
subindexed). `relation-id-for` (`:104-113`) = `rel:` + sha1(kind, from.kind, from.id,
to.kind, to.id, asserter). Reassert of the same relation ⇒ count-delta 0 (`:343`).

The driver's `sha256(unit-id ∥ kind ∥ target-id)` is a deterministic function of
(from=unit-id, kind, to=target-id); with the fixed asserter `sense-block/mechanical@1`
it is 1:1 with `relation-id`, so it is a valid per-relation dedup key. Re-submit ⇒
same relation-id + same journal-key ⇒ "write NOTHING." ✓ Existing test confirms the
semantics: `relation_kernel_test.clj:43,72` — "Barrier … counts consumed … no-op
replays and rejections included."

**Verdict: OK.** **Driver obligation:** set BOTH `:idempotency/key =
sha256(unit-id∥kind∥target-id)` AND `:relation/routing-key = relation-id-for(...)`.
A blank routing key is the ONLY silent drop (rk:664-667) — the driver must always
populate it. (Read, not inferred.)

### (e) second-distillation-over-existing-source import semantics (R4 gate) — **OK: import path ACCEPTS it (with driver payload obligations)**

**Claim to test:** does the import path ACCEPT a second distillation over an
already-ingested source (disjoint `du:…:sense-block-v0:…` id space) — or does a guard
reject/corrupt it?

**Evidence — the import topology is generic row-iteration with NO
"source-already-ingested" rejection.** `object_container.clj:1998-2099` loops
`payload-derived-units`/`payload-source-anchors`/`payload-composition-edges` and writes
each to disjoint keys. The only gates:
- **import-key dedup** (`:1829-1830`): `local-select> $$import-completions-by-key`
  on `*import-key`. A DISTINCT sense-block import-key (e.g. `imp:sense-block:…`) ⇒ nil
  ⇒ no conflict, proceeds. ✓
- **idempotency/material-fingerprint** (`:1793-1816`): distinct idempotency-key ⇒ no
  conflict. ✓
- **native-identity claims** (`:1910-1929`, reject at `:2243-2250`): iterates ONLY
  `payload-object-containers`. Its conflict test `native-claim-compatible?`
  (`:1251-1269`) even tolerates re-declaring the IDENTICAL document container (third
  clause: same claim-key/container-id/kind/object-key/content-hash ⇒ compatible).
- **object-key is distiller-INDEPENDENT** (`object-key-for :206-208` =
  `sha-256(source-ref ∥ NUL ∥ source-hash)`) — so a second distiller reuses the SAME
  object-key/source-id; only `du:`/`sa:` ids diverge by distiller segment (R3).

**The one hard requirement the driver MUST satisfy** — `import-request-validation-errors`
(`:705-795`):
```
(empty? source-rows)               (conj {:type :source-artifacts/missing})   ; :765-766
missing-anchors                    …:source-anchors/missing-for-targets       ; :785
missing-anchor-sources             …:source-anchors/source-missing            ; :789
missing-anchor-targets             …:source-anchors/target-missing            ; :793
```
So the payload MUST (i) include ≥1 source-artifact row, (ii) give every unit an anchor,
(iii) every anchor's `source-id` be in-payload, (iv) every anchor target be a
in-payload unit/container. The markdown-only `source/hash-mismatch` checks
(`:724-738`) are SKIPPED for transcripts because source-format is `:transcript`, not
`:markdown` (transcript_adapter.clj:614; confirmed `:source-format :transcript` :258).

⇒ **The driver re-declares the existing (transcript-format) source row, keeps
`:object-containers []` + `:revisions []`, uses a distinct import-key + idempotency-key.**
Then: no import-completion conflict, no idempotency conflict, no native-identity conflict
(empty containers ⇒ empty loop), source-hash checks skipped, all anchors resolve.

**R5 (derived text) sub-check:** the import path writes DerivedUnitRow as-is
(`:2013-2015`); NO validation requires `derived-content-text`. OC tolerates nil/empty
derived text (R5 lean supported; the §9 R5 stop-clause does NOT fire).

**Verdict: OK — the import path accepts a second distillation; no guard rejects or
corrupts it.** The disjoint id-space claim holds for units/anchors/edges; the SOURCE is
shared (correct per R4). **Caveat feeding candidate 2:** "accepts" is about MECHANICS;
WHAT the driver anchors into (the existing EDN-`pr-str`/preview raw-text) is the
separate (f) surface-fidelity problem. (Read, not inferred — full topology traced.)

### (f) transcript source/anchor granularity + anchor-id determinism (R3/R4) — **anchor-ids OK; granularity finding = candidate 2**

**Granularity (read from `transcript_adapter.clj:221-298`):**
- **object-key = per-CONVERSATION** (`transcript-object-key source conversation-id`, :227).
- **source-id + SourceArtifactRow = per-MESSAGE** (`transcript-source-id object-key
  source-line-key`, :230; source-ref = `<file-path>#<byte-offset>`, :231; source-hash =
  line-hash, :232). One immutable source artifact per message/observation.
- **Containers** = conversation (`:chat-conversation`), message (`:chat-message`),
  tool-call (`:tool-call`), tool-result (`:tool-result`) — one each per its grain.
- **NO DerivedUnitRow is minted by the transcript adapter** (grep-confirmed: only
  `->SourceArtifactRow`/`->SourceAnchorRow`/`->CompositionEdgeRow`/`transcript-container-row`).
  ⇒ **sense-block-v0's derived units are the FIRST on these sources — fully disjoint,
  zero collision.**

**SourceArtifactRow.source-raw-text is NOT clean per-part text** (the candidate-2
finding): parsed message ⇒ `(pr-str redacted-payload)` (EDN map string,
transcript_adapter.clj:251-259 + transcript.clj:509-516); parse-error ⇒ 200-char
preview (transcript.clj:445). Message-container content = lossy text+tool_result join,
drops thinking/tool_use (transcript_adapter.clj:24-37). SPEC §1 wants
`surface=(event-id,part-path)`. ⇒ candidate 2.

**Anchor-id determinism — PASSES (both schemes deterministic):**
- transcript: `sa:<object-key>:<sha256(target-id)>:<anchor-hash>` where `anchor-hash =
  sha256(source-id ∥ target-id ∥ line-hash)` (transcript_adapter.clj:140-145).
  Deterministic. ✓
- sense-block (markdown shape): `sa:<unit-id>` via `oc/source-anchor-id`
  (object_container.clj:218-220), `unit-id = du:<object-key>:sense-block-v0:<block-path>`
  (markdown_adapter.clj:13-15). Deterministic given the (deterministic) cut. ✓

**Nuance for SPEC §6.1** ("same (surface,span) ⇒ same identity"): sense-block realizes
span-identity POSITIONALLY (via `block-path`), NOT via a span-hash. So identity is
stable across re-runs of the SAME distiller (G4 idempotence holds), and cross-distiller
id-spaces are intentionally DISJOINT (R3) — a different distiller producing the same
span gets a different id by design. This matches R3; it does not give cross-distiller
span-dedup (nor should it).

**Offset coordinate mismatch (note):** transcript container anchors use BYTE offsets
into the FILE (`byte-offset`/`byte-length`, transcript_adapter.clj:142-143,151-152);
sense-block uses UTF-16 char offsets into `source-raw-text` (R2; markdown_adapter.clj:25-41
counts with `(count line)` on JVM strings = UTF-16 units). The driver must NOT reuse
transcript byte-offset conventions.

**Verdict: anchor-id determinism OK (R3 satisfied); granularity is per-message with no
existing derived units (good); the source-raw-text fidelity gap IS candidate 2.**

### (g) river-page realization (lean: composition first) — **OK: compose driver-side, no new topology (design note only)**

**Evidence:** `read-common-material-for-source` (`object_container.clj:2501-2551`) is
PER-`source-id`: returns `CommonMaterialBundle {containers derived-units anchors edges}`,
each a cursor-paginated `sorted-map-range-from` range scan with `{:allow-yield? true}`
(`:2514-2549`); derived-units come back ordered by material-ref order-key = block-path
(`:2023`). The full query surface is only 5 topologies (`:2443-2551`): read-latest-source-
by-ref, read-source-by-ref-version, read-unit, read-current-revision,
read-common-material-for-source. **There is NO query topology exposing a conversation's
ordered sources** — that ordering lives in `$$transcript-conversation-projection` (keyed
[conversation-container-id, order-key]) which no topology reads out.

A session = one object-key but MANY per-message (and, per candidate 2, per-part)
source-ids. So a whole-session river page = ordered concatenation ACROSS sources. The
**driver already holds the ordered source-ids** (it just distilled the session in file
order), so it composes: for each source in order, call `read-common-material-for-source`,
filter to `sense-block-v0` units, order by block-path, concatenate.

**Verdict: OK — composition without a new topology.** Each per-source read is a bounded
range-scan; the fan-out is one scan per message/part. For the MVP first river page this
is acceptable; it is a per-source range-scan fan-out, which G12 (review-time) should
eyeball but does not forbid. **Recommendation: composition first** (matches the CONTRACT
lean); a dedicated `read-conversation-river-page` query topology is the scale answer,
revisable at UI time (CONTRACT §10). NOT a stop-clause.

### (h) delegation-chain capture point (lean: additive fields) — **OK: recommend additive fields (driver reads raw)**

**Evidence — today's capture is minimal:** `transcript.clj:327-338`
`transcript-conversation-id` reads `:parentUuid` only as a FALLBACK for the
conversation-id (:336), NOT as a retained delegation field; `transcript-message-uuid`
(:340-347) reads uuid/id. **`isSidechain` and `promptId` are captured NOWHERE**
(grep-confirmed across transcript.clj/transcript_adapter.clj). `TranscriptConversationProjectionRow`
(oc:139-142) has no delegation fields.

**Evidence — the raw data is present** (c-census): `parentUuid`+`isSidechain` on 100%
of message events, `promptId` on 121. The driver is foreign-side and can read them
directly (raw jsonl or `read-string` of the stored redacted payload).

**Additive-field precedent (T13):** `SourceIngestCompletionRow` carries a later-added
`claimed-at-ms` field with a docstring carve-out (oc:90-97) — the sanctioned pattern for
adding a field to an existing defrecord.

**Verdict: OK — recommend ADDITIVE fields, not relation edges.** Grounds: (1) relation
edges for delegation would need NEW kinds (e.g. `on-behalf-of`), compounding candidate 1;
(2) T14 forbids NEW row TYPES without a stop-clause, but T13 PERMITS additive fields;
(3) the driver already has the values. **Realization caveat for the plan:** the
production event is per-SURFACE (SPEC §7.3), so the cleanest home is additive fields on a
production-event-carrying projection row emitted via the import `:projection-hints` path.
Note: a genuinely NEW `:projection-kind` would need a new `case>` in the import topology's
projection dispatch (oc:2145-2203) — that is a NON-additive topology edit (Sid-gated,
§12). Adding fields to an EXISTING projection row-kind + populating them is additive and
un-gated. The plan must pick the additive variant (extend an existing projection row) to
stay off the §12 gate.

---

## 3 · Rama-claim checks (against `docs/reference/rama/` + existing harnesses)

**Topology types (load-bearing — the two kernels differ):**
- OC import path = **STREAM** topology: `object_container.clj:1695`
  `(stream-topology topologies "object-container-topology")`.
- RK = **MICROBATCH** topology: `relation_kernel.clj:623`
  `(microbatch-topology topologies "relation-kernel-topology")`.

**Deterministic processed-count barrier (submit == +1; routing key always present):**
`27-testing.md:77` — "With a microbatch topology, processing is asynchronous to depot
appends even with AckLevel.ACK. So … `waitForMicrobatchProcessedCount`." `:121` — "the
number of depot records specified represents the TOTAL amount of records EVER processed
by the topology, not the number since the last call" ⇒ the test tracks a CUMULATIVE
count; each appended depot record = +1, **including replays and rejections**
(corroborated in-repo: `relation_kernel_test.clj:43,72` "counts consumed … no-op replays
and rejections included, since the count tracks depot records"). ⇒
- **RK (microbatch):** `:append-ack` is NOT sufficient (async, 27-testing:77); the
  harness MUST `rtest/wait-for-microbatch-processed-count ipc module "relation-kernel-topology"
  cumulative-N` (real usage: `relation_kernel_test.clj:85`, `git_spine_test.clj:119`).
  The driver must ALWAYS set `:relation/routing-key` (blank ⇒ silent drop pre-write,
  rk:664-667) so every append is a real +1.
- **OC (stream):** `:append-ack` foreign-append returns AFTER the record is processed, so
  the append return IS the barrier (no wait-for-microbatch); read PStates immediately
  after. This is the `object_container_test`/git-spine dual-barrier split (microbatch for
  RK, `:append-ack` for OC). No polling either way (T12 satisfied).

**Exactly-once caveat (relevant, bounded):** `12-microbatch-topologies.md:126` —
microbatch PState updates are exactly-once across retries; `:130` — depot appends made
FROM WITHIN a topology do NOT have exactly-once semantics. This does NOT bite the driver:
its appends are FOREIGN (plain-Clojure `foreign-append!`), not in-topology. (The OC import
topology's own in-topology append to `$$…source-line-completions` at oc:2231 is not on the
sense-block path unless the driver ships `:source-line-statuses`, which it won't.)
Idempotence is instead carried by the deterministic ids (G4) + import-key/idempotency
gating (§2/e) + RK journal (§2/d).

**Physical-PState reader for negative invariants (G1/G4 "physical … not the query API"):**
the sanctioned pattern is `foreign-pstate ipc module-name "$$name"` then
`foreign-select`/`foreign-select-one` with a keypath — proven in
`object_container_test.clj:23-33` (reads `$$derived-units-by-id`, `$$source-anchors-by-target`,
`$$events-by-id`, … directly) and `relation_kernel_test.clj:50,101`. `31-clj-testing.md`
adds `create-test-pstate`/`test-pstate-select-one` for unit-testing pure `deframafn`/
`deframaop` in isolation (useful for the free-cut pure fns, no IPC — CONTRACT "unit-testable
without IPC"). T7 exception (validation-only readers in tests) is exactly this.

---

## 4 · Open questions for the orchestrating session / Sid

1. **[STOP-CLAUSE 1 · relation kinds]** Authorize the one-line addition of
   `:grounds :assembled-from :refines` to `relation-kinds` (relation_kernel.clj:58-65)?
   Without it, G8/G10 (mechanical floor + assembly) cannot write and the driver's edge
   appends reject at decision time. Recommended: yes (additive, reviewed, the set exists
   to be extended). §12 gates it as "a relation-kernel amendment."

2. **[STOP-CLAUSE 2 · surface to anchor into]** Ratify the driver minting **per-part text
   surfaces** (`(event-id, part-path)`, raw-text = redacted part text via `read-string` of
   the already-stored payload) as the R4 realization — vs anchoring into the existing
   per-message `pr-str`-EDN source (fragile, breaks G6/§2.3). Recommended: per-part
   surfaces. This is the §9 "re-ingest under a distinct source-ref" option applied to the
   SAME chat (no second chat file, no re-redaction).

3. **[classifier + fixture]** SPEC §15's fixture table diverges from the live file past
   event ~8 (first assistant is event 18, not 9); regenerate the G1/G3 golden from the
   real file. Add debris rules for `attachment · last-prompt · ai-title · bridge-session ·
   system · custom-title · agent-name` (+ a default-debris fallback). Is `attachment`
   ever river (user-pasted content) or always debris?

4. **[SPEC §4.6 form gap]** `image` content parts (9 in the file) have no form. Add an
   `image` form (§4.6 amendment — Sid) or default unknown parts to a generic material
   form (driver)?

5. **[containment: store vs compute]** CONTRACT §4 maps containment to CompositionEdgeRow;
   SPEC §6.2 says hierarchy MUST NOT be stored (every tree is a query result). Does
   sense-block emit composition edges (a cache of the computed tree) or none (rely on
   computed span containment)? If it emits edges parenting under existing message
   containers, use distiller-namespaced order-keys so
   `$$composition-children-by-parent[parent-id, order-key]` (oc:2077-2080) cannot stomp a
   transcript edge under the same parent.

6. **[R1 versioning]** SPEC §2.1 wants `rule-id@version`; today redaction is policy
   `:standard` with no version token. Treat `:standard` as `@1` and stamp it on
   provenance, or introduce an explicit policy version?

7. **[production-event home (h)]** Confirm the additive delegation fields extend an
   EXISTING projection row (un-gated, additive) and do NOT introduce a new
   `:projection-kind` requiring an import-topology `case>` (Sid-gated, §12).

---

*Read-only pass complete. No source/test/depot touched; this file is the sole artifact.*
