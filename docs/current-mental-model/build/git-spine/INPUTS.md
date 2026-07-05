# INPUTS — git-spine WP2 (+ /assert write shim)

Gathered 2026-07-05 by a fresh Opus recon agent (dispatched from the Fable HQ
session), every load-bearing claim verified by direct read. **Never re-gather —
re-grep cites before relying on them (line numbers drift), but the facts stand.**
Tags: **[V]** verified by direct read, **[I]** inferred from read facts.

---

## 1. Markdown + transcript ingest — entry points, row shapes, ids, idempotency, convergent re-import

**Live entry layer** — `src/app/server/ingest_watchers.clj` is an OS-side WatchService trigger *outside* every Rama module; it only calls existing import seams (no new truth). **[V]** (`ingest_watchers.clj:1-18`)

1.1 **MD path:** `import-md!` = `(slurp file)` → `markdown-adapter/markdown-source-import-request raw-text source-ref` → `ocr/append-object-container-request!` → `ocr/await-object-container-decision` (5 s). **[V]** (`ingest_watchers.clj:64-75`)

1.2 **JSONL path:** `import-jsonl!` = `transcript/transcript-request :transcript/watch` → `transcript/read-jsonl-observations` (offset 0) → `transcript/import-observations-into-object-container!`. So the **live transcript path is `dogfood/transcript.clj`, NOT `dogfood/transcript_ingest.clj`.** **[V]** (`ingest_watchers.clj:77-89`; `transcript.clj:1072,1095,1103`)

1.3 **Decision latch / epoch:** import "succeeds" only on `(= :accepted (:status result))`; then `util-fns/!ingest-epoch-atom` is bumped by exactly one. No fs poll. **[V]** (`ingest_watchers.clj:91-116`)

1.4 **`SourceArtifactRow`** = `[source-id source-ref source-hash source-format source-raw-text document-container-id content-byte-count created-at-ms created-by event-id]`. Note it stores the **full `source-raw-text`**. **[V]** (`object_container.clj:82-84`)

1.5 **`SourceVersionRow`** = `[source-ref-key source-ref source-hash source-id document-container-id object-key order-key created-at-ms event-id]`. **[V]** (`object_container.clj:86-88`) MD materialization builds both rows at `markdown_adapter.clj:208-226`.

1.6 **Deterministic-id minting idiom (all SHA-256 content-addressed):** `source-ref-key = sha256(source-ref)`; `object-key-for = sha256(source-ref ⊕ sep ⊕ source-hash)`; `source-id = "src:"+object-key`; `document-id = "oc:doc:"+object-key`; `source-hash = sha256(raw-text)`; `fixed-width-order-key = format "%020d:%s" time-ms request-id`. **[V]** (`object_container.clj:197-260`)

1.7 **MD request idempotency + fingerprint:** `idempotency-key = "source/ingest:"+source-ref-key+":"+source-hash`; `material-fingerprint = sha256(pr-str {…:source/ref …:source/hash …:source/format :markdown …})`; `routing/key = [:source-ref source-ref-key]`. Both key and fingerprint derive only from (ref, content-hash). **[V]** (`markdown_adapter.clj:394-411,429`)

1.8 **Idempotency-journal PStates:** `$$requests-by-audit-id {String ObjectContainerRequestRow}` and `$$decisions-by-audit-id {String ObjectContainerDecisionRow}` (both `partition-by-audit-id`), plus `$$decisions-by-idempotency`. `audit-id = partition-key+"/request/"+request-id`; `decision-id = audit-id+"/decision"`. **[V]** (`object_container.clj:1689-1693,242-248`)

1.9 **Convergent re-import is wired in the topology, not just helpers:** the ETL block `local-select`s the prior decision by audit-id, else by idempotency; on a present prior decision it checks `material-fingerprint-conflict?` → same fingerprint calls `replay-decision-row` (re-emits prior decision with `:replayed-from-decision-id`, **writes no new rows**); different fingerprint under the same key → `conflict-decision-row` (rejected). **[V]** (`object_container.clj:1778-1814`; helpers `456-494`)

1.10 **Transcript (multi-line) has a second idempotency layer:** `$$import-completions-by-key` + import-material-fingerprint compare + per-line status, so a re-imported file converges line-by-line. **[V]** (`object_container.clj:1814-1890`)

1.11 **Re-run safety, plainly:** byte-identical rewrite → identical ids/key/fingerprint → decision replayed, epoch still bumps, zero new rows; changed content under the same routing key → fingerprint conflict → rejected. **[V]** (`ingest_watchers.clj:6-10`; §1.9)

---

## 2. Do transcript rows preserve tool-call payloads? — YES (full, un-truncated), in the live path

2.1 **Live path uses `object-container/transcript_adapter.clj`.** `transcript.clj:1095` calls `transcript-adapter/transcript-observation-import-request`; requires it at `transcript.clj:7`. **[V]**

2.2 **Tool-use blocks store the FULL redacted input:** per `tool_use` block a `:tool-call` container + revision are written with `content-text = (pr-str (select-keys block [:name :input]))`. For a Bash tool that is `{:name "Bash" :input {:command "git commit …" …}}` — **command text (git commit / git push) is preserved**; Edit/Write/Read `:file_path` values likewise. **[V]** (`transcript_adapter.clj:299-315`)

2.3 **Tool-result blocks store the full result text:** `:tool-result` container/revision with `content-text = (str (or (:content block) (:text block) ""))` — so a `git commit`/`git rev-parse` result carrying a **commit sha is preserved** in the tool-result content. **[V]** (`transcript_adapter.clj:328-350`)

2.4 **The whole redacted JSONL entry is also stored** on the transcript `SourceArtifactRow` as `source-text = (pr-str (:transcript/redacted-payload obs))`. **[V]** (`transcript_adapter.clj:251-259`)

2.5 **These land in `$$containers-by-id {String ObjectContainerRow}` and `$$revisions-by-id {String RevisionRow}`** (both `partition-by-object-key`); container-kinds `:tool-call` / `:tool-result` / `:chat-message` / `:chat-conversation`. A join extractor reads these and parses the pr-str content. **[V]** (`object_container.clj:1706-1708`; kinds at `transcript_adapter.clj:281,294,311,347`)

2.6 **Redaction is KEY-NAME based, applied at parse:** masked keys = `#{"api_key" "apikey" "api-key" "token" "auth_token" "auth-token" "oauth_token" "oauth-token" "password" "secret" "authorization"}`. `:command` and `:file_path` are **not** in the set → they survive verbatim. Raw-text pattern redaction (key/token/secret/bearer regexes) applies **only to unparseable content**. **[V]** (`llm.clj:101-103`; `transcript.clj:464,399-412,504-514`)

2.7 **CAVEAT — the 200-char truncation is in the OTHER (non-live) module.** `dogfood/transcript_ingest.clj` truncates tool input to 200 chars (`redacted-input-for-tool`) and stores only a `redacted-input-preview` on its `ToolCallIndexRow`. That module is **tests-only** (see §6). The live object-container path (§2.2-2.3) does **not** truncate. **[V]** (`transcript_ingest.clj:93-97,195-197`)

2.8 **CAVEAT — the ~1.2 GB transcript corpus is NOT ingested at boot.** The boot initial-sweep roots are `docs/current-mental-model` + `vision` only; the transcript dir is explicitly deferred to an offset-incremental re-read (named follow-up). So **PStates currently hold no transcript rows unless a transcript is explicitly imported** — the storage mechanism preserves shas/commands (§2.2-2.4), but the material must first be loaded. **[V]** (`file_viewer.cljc:153-162`)

**Bottom line:** transcript↔commit joins *can* be extracted from PStates alone once transcripts are imported — command text and shas are stored full and un-truncated (subject to key-name redaction). No need to re-read raw `.jsonl` for payload content; the blocker is that the corpus isn't swept yet, not that payloads are lossy.

---

## 3. Relation-kernel write surface

3.1 **Append helper:** `append-relation-request!` — arity `[runtime request]` / `[runtime request ack-level]`, default `ack-level = :append-ack`; does a **single `foreign-append!`** of one envelope to `(:relation-request-depot runtime)`; throws `IllegalArgumentException` if `:relation/routing-key` is blank; returns the request. **[V]** (`relation_kernel.clj:917-926`)

3.2 **Ack semantics:** `:append-ack` is a **microbatch depot ack — it does NOT imply PState visibility**; reads must poll the materialized row (`await-relation`). **[V]** (`relation_kernel.clj:920,987-989`)

3.3 **Request envelope shape** (built by `envelope`, `relation_kernel.clj:853-870`) **[V]:**
```
{:relation/routing-key relation-id                     ; = relation-id-for(...)  (routing key)
 :request/id           request-id
 :request/type         :relation/assert | :relation/retract
 :request/sent-at-ms   (or sent-at-ms asserted-at-ms)  ; arrival clock §5.3
 :idempotency/key      idempotency-key
 :actor                (or actor {:actor/id asserter-actor-id :actor/type asserter-type})
 :payload              (->RelationMutationPayload relation-id kind from to
                          asserter-actor-id asserter-type
                          evidence-source-id evidence-anchor-id note asserted-at-ms)}
```
Builders: `assert-request` / `retract-request` (`relation_kernel.clj:872-886`); `RelationMutationPayload` fields at `220-222`. Accessors `relreq-*` at `276-282`.

3.4 **Registered relation-kinds (closed set):** `#{:based-on :produced :built-over :new-direction :dead-end :elaborates :references :confirms :refutes :supersedes}`. **Stance kinds** = the last three `:confirms :refutes :supersedes`. Unregistered kinds are rejected at decision time. **[V]** (`relation_kernel.clj:58-65,286`) `request-types = #{:relation/assert :relation/retract}` (`:67`); `relation-statuses = #{:asserted :retracted}` (`:69`).

3.5 **Target kinds** (RelationTargetRef doc): `:container :source :git-commit :doc-file :conversation :none`. **`:git-commit` is already a first-class target-kind**, and `->target-ref` sets its `target-key` = `(str target-id)` (the sha verbatim); container/source/doc/conversation go through `oc/extract-object-key`. **[V]** (`relation_kernel.clj:215-218,835-846`)

3.6 **`relation-id-for` (deterministic identity):** `"rel:" + sha1-hex(join NUL [kind, from.target-kind, from.target-id, to.target-kind, to.target-id, asserter-actor-id])`. **Identity includes the asserter** (trap 5). `sha1-hex` = SHA-1 hex of the joined string — deterministic hashing is what makes re-asserts converge. **[V]** (`relation_kernel.clj:104-111,79-84`) `decision-id = rel-id+"/decision/"+request-id`; `event-id = rel-id+"/event/"+order-key` (`123-124`).

3.7 **Custody fields (the writer, distinct from payload asserter):**
- `RelationDecisionRow` → `envelope-actor-id envelope-actor-type` (plus `request-material-hash`, `replayed-from-decision-id`, `status`, `reason`, `errors`, `event-id`, `decided-at-ms`). **[V]** (`relation_kernel.clj:233-239`)
- `RelationEventRow` → `envelope-actor-id envelope-actor-type` (per-transition custody). **[V]** (`241-247`)
- `RelationEdgeRow` → `envelope-actor-id envelope-actor-type` = writer of the **latest** transition (re-assert/retract overwrite). **[V]** (`249-255`)
- `RelationActivityRow` carries `envelope-actor-id` (writer) but not its type. **[V]** (`270-273`)

3.8 **Runtime handle keys** (from `start-relation-runtime!`): `:relation-request-depot` + query/pstate handles. Standalone runtime is tests-only (see §6). **[V]** (`relation_kernel.clj:891-910`)

---

## 4. Existing git-metadata reading — NONE

4.1 **No git log parsing, sha handling, commit walking, or `git` shell-out anywhere in `src/` or `scripts/`.** The only `git` occurrences: `.git` in a directory-ignore set (`file_viewer.cljc:25`); prose "Git tracks the evolution" comments (`kernel.clj:10,327,501,809`); git dependency coordinates (`deps.edn:23`). **[V]**

4.2 **All `ProcessBuilder` uses are non-git:** generic CLI-agent spawns `run-cli-process` / `stream-cli-process` (`server_jetty.clj:285,400`) and dogfood executor spawns (`llm.clj:2140`, `compute.clj:1023`). `scripts/` contains only `install-rama.sh`. **[V]**

4.3 The relation kernel *models* `:git-commit` as a target-kind (§3.5) but **nothing in the repo populates a git-commit target or reads commit metadata today.** WP2 introduces the first git reader. **[I]**

---

## 5. Display names — the F-L3 hash-id issue and the enrichment seam

5.1 **The card already prefers a display name and falls back to the raw id:** `name-ln (str glyph " " (or (:display-name target) (:id target)))`. Because the feed never supplies `:display-name`, it always renders the hash id — the F-L3 symptom is in the **data layer, not render.** **[V]** (`cards.cljc:249,254`)

5.2 **Feed constructors set `:entry/target {:id … :kind …}` from raw ids, no title** — relation `:id (:from-id row)` (`trail_view.clj:484`), file `:id (:file-key row)` (`:496`), source/doc `:id (:document-container-id row)` (`:505`). The human filename is carried *adjacent* in `:entry/detail` as `:file-path` / `:source-ref` (`:501,:511`), not promoted to a title. **[V]**

5.3 **View-3 text projection** prints the raw `tid` as heading and fills the CONTRACT's `"<display-name>"` slot with `container-kind`. **[V]** (`trail_view.clj:644`; contract slot `trail-view/CONTRACT.md:432`)

5.4 **No object-container row persists a human title/name/label/heading** — `ObjectContainerRow` (`object_container.clj:99-102`) and `SourceArtifactRow` (`:82-84`) store id/kind/hash/path/body only; the only human strings on the md path are the filename `source-ref` and the body text. **[V]**

5.5 **F-L3 finding, verbatim:** "card titles are full content-hash ids (live entries carry no display-name…). Readable names need a display-name projection upstream (WP1-side enrichment; goes to the importer-enrichment gap already declared in the feed's standing omissions)." Assigned to WP2. **[V]** (`view-mvp/FIRST_LIGHT.md:49-53,72`; `trail-view/RETRO.md:33`)

5.6 **The "importer-enrichment gap" in code == the `:session-metadata` standing omission**, not a dedicated display-name omission. Static feed omissions are exactly two: `:feed/gap :editor-revisions` and `:feed/gap :session-metadata`, attached at `trail_view.clj:530`. **[V for the two entries; I for the mapping]** (`trail_view.clj:56-60`; `trail-view/CONTRACT.md:378-385`)

5.7 **Where MD display-name enrichment plugs in — three seams:** (a) render already consumes it — zero change (`cards.cljc:249`); (b) data seam — add `:display-name` at the feed constructors (`trail_view.clj:484/496/505`) and/or `gather-target-material`'s `:identity` map (`:145-150`); for file/source branches the name is already adjacent in `:entry/detail`, so surfacable with no new OC state; (c) truth seam — a genuinely stored title (e.g. first MD heading) requires a NEW field on `ObjectContainerRow`/`SourceArtifactRow` populated at ingest = the "importer enrichment" slice. **[a: V; b,c: I]**

---

## 6. App boot / cluster start / write-affordance seam

6.1 **The live cluster the /trail UI reads is a `defonce delay` in `file_viewer.cljc`:** `trail-view-runtime` (delay) → `start-trail-view-runtime!`; accessor `trail-rt` derefs it. The FIRST /trail pull pays cluster boot; ingest then streams async. **[V]** (`file_viewer.cljc:146-174`)

6.2 **`start-trail-view-runtime!` opens ONE ipc and launches FOUR modules in order:** (1) `oc/object-container-module`, (2) `oc/object-container-transcript-ops-module`, (3) `rk/relation-kernel-module`, (4) `trail-view-module`. The returned map exposes **`:relation-request-depot`** (and `:object-container-requests-depot`). **[V]** (`trail_view.clj:680,686-689,696-698`)

6.3 **A second, separate live cluster** is the text-kernel: `defonce !kernel-runtime` (delay) → `start-text-runtime!`, deref'd by `(runtime)`; used by the HTTP server's sidebar/settings/CLI-session shims. It is **not** the cluster the trail UI reads. **[V]** (`util_fns.cljc:12-21`)

6.4 **Ingest is kicked off on the trail-view runtime handle** in a `future`: `initial-sweep!` then `start-ingest-watchers!`, cfg `{:runtime rt :roots […]}`. Roots at boot = `docs/current-mental-model` + `vision` only (transcript corpus deferred, per §2.8). **[V]** (`file_viewer.cljc:156-171`)

6.5 **Read wrappers are `e/server` fns** in `file_viewer.cljc:176-190` (`TrailBundle` / `TrailFeed` / `TrailConversation` / `TrailText` / `WatchIngestEpoch`), all reading through `(trail-rt)`. **No write/assert e/defn exists.** **[V]**

6.6 **`append-relation-request!`'s only callers today are tests** — no `e/server` / HTTP route in `src/` calls it. **[V]** (`relation_kernel.clj:917-926`; tests `relation_kernel_test.clj:81`, `trail_view_test.clj:45`)

6.7 **The existing running-server-write precedent on this exact handle** is `ingest_watchers.clj:74` — `ocr/append-object-container-request!` `foreign-append!`ing to the OC depot of the **trail-view runtime**. **[V]** (append primitive `object_container/runtime.clj:90-94`)

6.8 **Seam for a `/assert` write-affordance:** the runtime map at `(fv/trail-rt)` already carries `:relation-request-depot` (§6.2), and `rk/append-relation-request!` takes any such map and `foreign-append!`s to it. So a server fn `(rk/append-relation-request! (fv/trail-rt) request)` — mirroring the read wrappers at `file_viewer.cljc:176-187` — reaches the running cluster's relation depot. Only the wiring is missing. **[V for handle+fn; I for their composition]**

6.9 **Standalone runtimes that are tests-only (never started at app boot):** `start-relation-runtime!`, `start-object-container-runtime!`, `start-transcript-runtime!`, `start-transcript-ingest-runtime!`, `start-compute-runtime!`, `start-llm-runtime!`, `start-space-runtime!`. Only `start-text-runtime!` and `start-trail-view-runtime!` boot in the running app. **[V]** (subagent caller-grep; trail-view boot at `file_viewer.cljc:157`, text at `util_fns.cljc:15`)

---

### Cross-cutting flags for the contract author
- **`:git-commit` target-kind already exists** in the relation kernel (§3.5) with sha-verbatim `target-key` — WP2's join output can assert `:based-on`/`:produced` edges to git-commit targets with no kernel change.
- **Transcript payloads are loss-tolerant for joins** (full command text + shas, §2.2-2.4) **but the corpus isn't loaded** (§2.8) — WP2 must decide between (a) triggering transcript ingest over the 1.2 GB dir, or (b) reading raw `.jsonl` directly for the join pass.
- **The write path for `/assert` is one thin server fn away** (§6.8); the read-side pattern to copy is `file_viewer.cljc:176-187`.
- **Display-name enrichment (F-L3)** has no persisted title today (§5.4); cheapest fix is the data-layer seam (§5.7b) since the filename is already adjacent in the feed payload.
