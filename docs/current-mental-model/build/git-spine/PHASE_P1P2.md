# PHASE_P1P2 — git-spine WP2 components A + B + C (P1) + extractor (P2)

Fresh-context Opus builder, 2026-07-05. Owns: NEW `src/app/server/rama/git_spine.clj`
+ `test/app/server/rama/git_spine_test.clj`; AMEND `src/app/server/ingest_watchers.clj`
(hook + cfg) and `src/app/file_viewer.cljc` (cfg plumbing only). Does NOT touch
`server_jetty.clj` / `trail_view.clj` (other builders). Contract v1.2 binding.

> **Path drift note (INPUTS re-grep):** `file_viewer.cljc` lives at
> `src/app/file_viewer.cljc`, NOT `src/app/client/workspace/file_viewer.cljc`.
> The suite runner has no `:main-opts`; tests run via
> `clojure -M:test -e "(require '[clojure.test :as t] 'NS) (t/run-tests 'NS)"`.

---

## §8 Builder verification duties — discharged with re-grepped cites

### Duty 1 — re-grep every INPUTS cite relied on (all HELD; line numbers drifted ≤ a few)

| Fact | INPUTS | Re-verified at |
|---|---|---|
| `object-key-separator` = `(str (char 0))` (NUL) | §1.6 | `object_container.clj:23` |
| `source-ref-key` = sha256(source-ref) | §1.6 | `object_container.clj:197-199` |
| `object-key-for` = sha256(ref ⊕ sep ⊕ hash) | §1.6 | `object_container.clj:201-203` |
| `document-id-for-object-key` = `"oc:doc:"+ok` | §1.6 | `object_container.clj:209-211` |
| `document-id-for` (composed) | R2 | `object_container.clj:254-256` |
| `source-hash` = sha256(str raw-text) | §1.6 | `object_container.clj:258-260` |
| `extract-object-key "oc:doc:<ok>"` → `(subs s 7)` = ok | R1 | `object_container.clj:313-314` |
| `extract-object-key` `:else` → verbatim | R1 | `object_container.clj:334` |
| `extract-object-key "oc:chat-conversation:…"` → `chat:<sha>` | R1 | `object_container.clj:295-296` |
| `SourceArtifactRow` shape | §1.4 | `object_container.clj:82-84` |
| `ObjectContainerRow` id field | §5.4 | `object_container.clj:99-102` |
| `markdown-source-import-request` builder | §1.1 | `markdown_adapter.clj:462-509` |
| md request zero path assumptions (format hard-coded `:markdown`) | R2 | `markdown_adapter.clj:500-501` |
| `append-relation-request!` (throws on blank routing key) | §3.1 | `relation_kernel.clj:917-926` |
| `read-relation-detail` (pre-check wrapper) | §3, A2 | `relation_kernel.clj:940-943` |
| `assert-request` / `envelope` / payload is records | §3.3 | `relation_kernel.clj:872-878, 853-870, 867` |
| `->target-ref`: container/source/doc-file/conversation → `extract-object-key`; never throws | §3.5, A4 | `relation_kernel.clj:835-846` |
| `relation-id-for` (deterministic, includes asserter) | §3.6 | `relation_kernel.clj:104-111` |
| `relation-kinds` (`:based-on` `:produced` registered) | §3.4 | `relation_kernel.clj:58-65` |
| `RelationTargetRef` / `RelationMutationPayload` records | §3.3 | `relation_kernel.clj:215-218, 220-222` |
| transcript source `:claude-code`; glob `~/.claude/projects/**/*.jsonl` | §2, §6 | `transcript.clj:79, 88` |
| ingest boot seam: `import-md!` uses `markdown-source-import-request` | §1.1 | `ingest_watchers.clj:64-75` (call at :73) |
| file_viewer boot future + cfg | §6.1, §6.4 | `file_viewer.cljc:156-172` (cfg :163) |
| OC append / await arities | §6.7 | `object_container/runtime.clj:90-94, 314-329` |
| read-context-bundle join: material-ids → object-keys → `read-relations-for-targets` | R1 B1 | `trail_view.clj:540-541` |
| green-suite barrier + join precedent | R1/R2 | `trail_view_test.clj:42-47, 70, 135-137, 208-214` |

### Duty 2 — Conversation endpoint: DERIVABLE → target the container (NOT the dangling fallback)

The chat-conversation container id derives from **known inputs** (the source
constant `:claude-code` + the session's `conversation-id`), so it is joinable —
I target it, mirroring the green suite's `(tref :conversation conv*)`
(`trail_view_test.clj:135`).

- `transcript-object-key source conversation-id` = `"chat:" + sha256((name source) ":" conversation-id)` (`transcript_identity.clj:6-8`).
- `chat-conversation-id object-key` = `"oc:chat-conversation:" + object-key` (`transcript_identity.clj:14-16`).
- The live adapter mints exactly this: `object-key (transcript-object-key source conversation-id)`, `conversation-container-id (chat-conversation-id object-key)` (`transcript_adapter.clj:226-227, 254`), where `source = (:transcript/source obs) = :claude-code` and `conversation-id = (:transcript/conversation-id obs)`.
- `conversation-id` is derived by `transcript-conversation-id` which prefers `sessionId` (`transcript.clj:327-338`, the `:sessionId` clause at :332). **Verified against a real transcript:** the jsonl entries carry `sessionId`, and the filename stem == `sessionId` (`0c93ba06-…` ↔ field). So the extractor derives the identical `conversation-id`.
- **Join:** `->target-ref :conversation <container-id>` → `target-key = extract-object-key(container-id) = "chat:<sha>"` (`object_container.clj:295-296`), which equals the ingested conversation container's object-key. Endpoint joins.

**Decision recorded:** conversation endpoint = `:conversation` + full `oc:chat-conversation:chat:<sha>` id. The `"from session-id ALONE"` wording (§8.2) is imprecise (A-R2.2): it derives from (source-constant, session-id), both known — so it IS derivable, and the honest-dangling fallback is NOT taken.

### Duty 6 — Prove the join for EVERY emitted target kind (asserted in gate tests)

`read-context-bundle` reads relations under `object-keys = (map oc/extract-object-key material-ids)` (`trail_view.clj:540`), then `rk/read-relations-for-targets` keyed on those object-keys. An edge surfaces on a rendered object iff an endpoint's `target-key == extract-object-key(object-id)`.

| Emitted endpoint | target-id | target-key (`->target-ref`) | Rendered object's object-key | Joins? |
|---|---|---|---|---|
| commit (`:container`) | `oc:doc:<ok>` = `commit->document-id sha` | `extract-object-key` → `<ok>` (`object_container.clj:313-314`) | ingested doc id `oc:doc:<ok>` → `<ok>` | **YES** |
| conversation (`:conversation`) | `oc:chat-conversation:chat:<sha>` | → `chat:<sha>` (`object_container.clj:295-296`) | ingested conv object-key `chat:<sha>` | **YES** |
| doc (`:container`) | `oc:doc:<ok'>` from (file-path, current content-hash) | → `<ok'>` | ingested md doc `oc:doc:<ok'>` | **YES** (version-addressed) |

Where `commit->document-id sha = (oc/document-id-for "git-commit:<sha>" (oc/source-hash canonical-text))` — the SAME id the md path mints for `commit->import-request` (single-source id math, no divergence provided the identical canonical text feeds both and no `:source/hash` override is passed — A-R2.1). Gates **G4/G6** assert `read-relations-for-targets` returns the edge under the endpoint's object-key (mirrors `trail_view_test.clj:208-214`).

**Doc-endpoint version-addressing (recorded per §2.3):** the doc target-id is computed from the file's **current** content-hash + its path-as-ingested (the watcher's `source-ref` = `.getPath file`, `ingest_watchers.clj:72`). If the file changes, both the ingested doc id and a re-run's edge target move together. The edge is emitted only for a `:file_path` that (a) canonicalizes under a land root and (b) exists on disk (so the content is hashable). Exact path-string match with the watcher's `source-ref` is required for the production render join; the P1/P2 gates test the cap + roots-filter, the render join is G11 (final phase).

### Duty 7 — Depot accepts plain-map payload; production replay reconstructs TYPED (belt-and-suspenders)

The topology consumes the payload **purely via keyword lookups**, so a plain-map payload is structurally acceptable:
- decision path: `payload (relreq-payload request)`, `from (:from payload)`, `to (:to payload)`, `kind (:relation-kind payload)` (`relation_kernel.clj:310-314`);
- event/edge construction: same keyword reads (`relation_kernel.clj:412-418`), endpoint copies `(:target-key from)` / `(:target-key to)` (`:499-500`);
- ref validation: `(keyword? (:target-kind ref))`, `(present-string? (:target-id ref))`, `(present-string? (:target-key ref))` (`:291-293`) — all satisfied by a plain map.

**Decision recorded:** `replay-assert-log!` reconstructs a TYPED payload via the public `rk/map->RelationMutationPayload` + `rk/map->RelationTargetRef` constructors (auto-generated public defrecord ctors), carrying `:request/id`, `:idempotency/key`, `:relation/routing-key` VERBATIM. This yields an envelope byte-identical to a fresh `assert-request` (contract trap 5: "reconstructing payload TYPES from the map is permitted; minting anything is not"). **Empirical probe:** the replay gate (`replay-durability-test`) appends BOTH a reconstructed-typed envelope AND a raw plain-map-payload envelope; the recorded result is in the gate report below.

### Duty 3 — md-adapter accepts synthetic source-ref `"git-commit:<sha>"`

`markdown-source-import-request` (`markdown_adapter.clj:462-509`) makes zero path assumptions: `source-ref` feeds only `source-ref-key`/`object-key` hashing (`object_container.clj:197-203`); no `File.`, no basename, no `.md` check; `:source/family :markdown` `:source/format :markdown` are hard-coded (`markdown_adapter.clj:500-501`). Confirmed buildable — `commit->import-request` = `(md/markdown-source-import-request canonical-text "git-commit:<sha>")`.

### Duty 4 — real transcript jsonl shape (one file opened + inspected)

- **Dir:** `~/.claude/projects/**/*.jsonl`, source `:claude-code` (`transcript.clj:79, 88`). cfg default `:transcript-roots` = `[<home>/.claude/projects]`.
- **entry uuid field = `:uuid`** — present on message entries, ABSENT on some meta/summary lines (23 of 41 scanned). **Recorded fallback: entry uuid, else the line number** (`"line:<n>"`), per §3.B.
- **tool_use block:** keys `#{:caller :id :input :name :type}`; `:input` is a map — Bash → `{:command "…"}`, Edit/Write → `{:file_path "…"}`.
- **tool_result block:** keys `#{:content :tool_use_id :type}`; `:content` is a **string OR a vector** of `{:type "text" :text …}` — the parser handles both.
- Content path: `(get-in entry [:message :content])` is the block vector.
- **`sessionId` == filename stem** (verified) — the session-id anchor.

### Duty 5 — `git log --name-status -z` shape on merge + rename (this repo)

Verified with `od -c` on `67f75eb…` (rename) and `846cc85…` (merge):
- **rename:** `R100` NUL `old-path` NUL `new-path` NUL → normalized `old -> new`.
- **add/modify/delete:** single status letter NUL `path` NUL.
- **merge:** `%P` = two space-separated parent shas; **name-status is EMPTY** (git shows no files for a merge by default) → `files:` is empty for merges; the merge still emits **one `:based-on` edge per parent** (G4).
- **Canonical text uses `%at`/`%ct`** (author/committer UNIX epoch seconds — locale/tz-free) formatted via `java.time.Instant/.toString` → `…Z` ISO-8601 UTC. This is byte-stable by construction (trap 6); no reliance on git's TZ handling.
- **Field framing:** `--pretty=format:%x1e%H%x1f%P%x1f%an%x1f%ae%x1f%at%x1f%ct%x1f%s%x1f%b%x1f --name-status -z` — records split on `\x1e` (RS), fields split on `\x1f` (US), name-status blob (after the 8th `\x1f` + git's `\n`) split on NUL. Control-char delimiters can't occur in git identity/subject; `-z` guarantees clean NUL path framing (trap 8).

---

## Component design (as-built summary)

**A — adapter half (pure):** `read-commits` (ProcessBuilder `git log --all`, repo from cfg), `commit->canonical-text` (labeled, sorted, UTC — the cross-builder interface), `commit->document-id`, `commit->import-request`. **B — sync + extractor:** `spine-sync!` (append-all-then-await-all commit ingests — NEVER serial 5s; parent `:based-on` edges with `read-relation-detail` pre-check + stable key `"spine:"+relation-id+":"+basis`), `extract-session-joins!` (stream jsonl line-by-line; repo-verified shas only via prefix-match against the commit index; `:produced` conv→commit + conv→doc capped one per (session,file); cursor cost-only). **C — replay + hook:** `replay-assert-log!` (reads `data/relation-assert-log.ednl`, `clojure.edn/read-string` plain-map lines, typed reconstruction, verbatim ids), `run-git-spine-boot!` in `ingest_watchers.clj` sequencing replay → spine-sync → extract; `file_viewer.cljc` gains cfg keys + one delegating hook call.

## Gate results (namespace-level — G10 full suite deferred to HQ per the 2026-07-05 throughput ruling)

`clojure -M:test` on `app.server.rama.git-spine-test` → **5 tests, 113 assertions,
0 failures, 0 errors** (one JVM, no cross-deftest interference). Amended
`ingest_watchers.clj` + `file_viewer.cljc` load clean under `-M:test`; kernel/OC
code is untouched, so their suites were NOT run (G10 = HQ final integration).

| Gate | Verdict | Test (deftest) | Evidence |
|---|---|---|---|
| **G1** adapter purity | PASS | `g1-adapter-purity-and-g3-parity` | canonical text byte-identical + whole import request identical (incl `:idempotency/key`) across two builds AND two independent repo reads; deterministic `:time-ms`/`:request/id` remove the wall clock |
| **G2** double-ingest convergence | PASS | `g2-g4-spine-sync-gates` | 2nd `spine-sync!` re-accepts all 5 commits (a non-deterministic re-run would fingerprint-CONFLICT → `:rejected`), `:edges-appended`=0, each decision stays `:accepted`, source artifact present. "No new rows" is the kernel's convergent-replay guarantee (INPUTS §1.9); the replay marker is a transient re-emit, not observable via `read-decision`, so convergence is shown via determinism (G1) + all-accepted + the edge pre-check |
| **G3** parity | PASS | `g1-…` / `g2-g4-…` | `read-commits` count == `git rev-list --all --count` (5 on the fixture); runtime `:commits`==`:ingested`==5 |
| **G4** parent edges | PASS | `g2-g4-spine-sync-gates` | every non-root commit has `:based-on` edge(s); the merge commit yields 2 (one per parent); 5 edges total; 2nd sync → `:edges-appended` 0 (zero new event rows); duty-6 join asserted (edge surfaces under the commit doc's object-key) |
| **G5** extractor honesty | PASS | `g5-g6-g7-extractor-gates` | REAL sha → `:produced` edge with `evidence-source-id "transcript:<session>"`, `evidence-anchor-id "e2"` (the jsonl entry uuid), `note "spine-v1|sha-verified"`; FAKE sha → NO edge; exactly 1 verified-sha edge; duty-6 commit join asserted |
| **G6** session×doc cap | PASS | `g5-g6-g7-extractor-gates` | two Edits of one in-roots doc → exactly ONE `:produced` edge (`note "spine-v1|file-write"`); file outside the land roots → no edge; duty-6 doc join asserted |
| **G7** cursor cost-only | PASS | `g5-g6-g7-extractor-gates` | unchanged file skipped via cursor; delete cursor → reprocess (`:files`>0, `:skipped`=0), 0 new edge appends, edge set byte-identical |
| **G12** file(1) text | PASS | `g12-files-are-text` | all 5 new/changed files report `text`, 0 NUL bytes (also confirmed out-of-band with `file`+python) |
| replay durability (G8-lite, mine) | PASS | `replay-durability-and-duty7-probe` | FRESH cluster + replay → edge asserted with the SAME relation-id, exactly 1 event; same-cluster DOUBLE replay → journal drops, still 1 event; the §3.C line is pure edn (no `#app.server…` tag, `clojure.edn/read-string` round-trips). Full P1+PW pair test (G8) is HQ final phase |

### Duty §8.7 — EMPIRICAL result

The `replay-durability-and-duty7-probe` test appended an envelope whose `:payload`
(and `:from`/`:to`) are **plain maps** (via `envelope->plain-map`, not reconstructed
to records) directly to the relation depot — **the edge materialized**. So the depot
accepts a plain-map payload directly (matching the keyword-lookup code path,
`relation_kernel.clj:310-314,412-418,291-293`). Production `replay-assert-log!`
nonetheless reconstructs TYPED payloads via `rk/map->RelationMutationPayload` +
`rk/map->RelationTargetRef` for byte-parity with a fresh `assert-request` — both are
contract-legal (trap 5), and both are now empirically proven in the same launch.

### Stop-clause classification

None triggered. No binding-doc conflict, no platform-wrong finding, no kernel-schema
touch. One contract expectation was corrected at test time (not a stop clause): G2's
"2nd sync all replay-decisions" wording implied an observable per-commit replay
marker, but the kernel's replay re-emits the decision transiently without rewriting
the stored row — so convergence is asserted through the observable, equivalent
signals above. Recorded as a test-authoring correction, not a policy fork.
