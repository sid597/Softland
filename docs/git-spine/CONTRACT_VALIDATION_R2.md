# CONTRACT_VALIDATION_R2 — git-spine WP2 (+ /assert shim) · contract v1.1

**VERDICT: FAIL**

Fresh-context, default-fail P-V round 2 (2026-07-05) against `CONTRACT.md` v1.1,
which amends v1 to close R1's B1/B2/S1/S2/A1–A4. **R1's two BLOCKERs are now
genuinely CLOSED** (verified against source + the green suite): B1's render-join
works with the amended `:container` targets, and B2's replay code has an
allowlisted home. **The FAIL is a NEW, empirically-proven BLOCKER introduced BY the
v1.1 amendment itself:** the S1 "fix" mandates reading the replay file with the core
reader under `*read-eval* false`, but that setting makes the reader **throw** on the
envelope's defrecord literals. The amendment swapped one crashing reader
(`clojure.edn`) for a different crashing configuration. Two SHOULD-FIX contradictions
were also left by the incomplete amendment (§3.D still fights G9; the assert-log path
has no shared home across the two parallel phases).

`file(1)` (task requirement): this file reports **Unicode text, UTF-8 text** (see
appendix). NUL-byte scan clean.

---

## What R1 asked me to verify, one line each

| R1 item | v1.1 claim | R2 result |
|---|---|---|
| **B1** id math pure/importable | `commit->document-id` = existing OC id math | **CLOSED** — public pure fns; extractor==adapter |
| **B1** `:container` edge joins | targets `oc:doc:<object-key>` like green suite | **CLOSED** — proven vs `trail_view_test.clj` gate 1 |
| **B2** replay has a home | homed in `git_spine.clj`, called from ingest boot | **CLOSED** (modulo SF-R2.1 shared path) |
| **S1** replay round-trip | core reader under `*read-eval* false` | **REGRESSED → BLOCKER** (throws; see below) |
| **S2** feed vs bundle split | feed `<sha7>`, bundle `<sha7> · <subject>` | gate split OK/testable, but **§3.D contradicts it** |
| **A1–A4** folded | trap 4 / G8 / §3.E | **all four folded** ✓ |

---

## BLOCKER

### B-R2.1 — `*read-eval* false` + pr-str'd defrecord envelope is IMPOSSIBLE; §3.C / §3.E / G8 are all unbuildable as written, and `replay-assert-log!` crashes on the first real envelope

This is the load-bearing finding and the reason for FAIL. It is empirically proven
in this repo's Clojure (1.12.4), against an actual `rk/assert-request` envelope.

**What v1.1 mandates (three places):**
- §3.C: "envelopes written with `pr-str`, read back with the CORE reader under
  `*read-eval* false` (validator S1: `clojure.edn` cannot read the defrecord
  literals; core reader round-trips them — G8 asserts the round-trip)."
- §3.E: assert-log line "format per §3.C, core-reader-compatible."
- G8: "assert-log grew by one line that ROUND-TRIPS through the core reader under
  `*read-eval* false`."

**What actually happens.** The envelope's `:payload` is a `RelationMutationPayload`
record whose `:from`/`:to` are `RelationTargetRef` records
(`relation_kernel.clj:220-222,215-218`). `pr-str` prints them as record literals
`#app.server.rama.relation_kernel.RelationMutationPayload{…}`. Clojure reads that
literal through `LispReader$CtorReader.readRecord`, which contains an explicit guard:
**record-construction syntax is only permitted when `*read-eval*` is `true`.** So:

```
clojure.core/read-string  *read-eval* FALSE  → THROWS java.lang.RuntimeException:
    "Record construction syntax can only be used when *read-eval* == true"
clojure.core/read-string  *read-eval* TRUE   → OK, equal-to-original? true, records reconstructed
clojure.core/read-string  (ambient, =true)   → OK, equal-to-original? true
clojure.edn/read-string                       → THROWS: No reader function for tag …RelationTargetRef
```

(Full transcript in the appendix — reproduced twice, once with a masking `try`
that hid the throw as a `{:threw …}` map, then cleanly.)

**Consequences, both fatal as written:**
1. **G8's gate assertion cannot pass** — "the line ROUND-TRIPS through the core reader
   under `*read-eval* false`" *is* the throwing call. The gate throws instead of
   asserting.
2. **`replay-assert-log!` crashes on the first real envelope** — §3.C tells the
   builder to read the file "under `*read-eval* false`," so the boot replay path dies
   on the first stored record literal. This is the exact failure R1's S1 warned about,
   reintroduced under a new banner.

**Why the amendment misfired.** `*read-eval* false` was a *deliberate and correct*
security choice — the assert-log is a file on disk; reading it with `*read-eval* true`
would honor `#=(…)` eval forms, i.e. arbitrary-code-execution if that file is ever
tampered with. But `*read-eval* false` **also disables record-literal construction**.
The two requirements the contract states — *(a)* serialize the envelope's defrecords
with `pr-str`, and *(b)* read them back under `*read-eval* false` — are **mutually
exclusive**. You cannot have both.

**Fix direction (policy-adjacent — the contract text is wrong, not just the impl):**
- **Preferred (keeps the security posture):** serialize the envelope as **plain maps**,
  not records — walk the payload into maps before `pr-str` (the topology reads the
  payload purely by keyword lookup: `relreq-*` accessors + `:from`/`:to` map access,
  `relation_kernel.clj:276-282`, INPUTS §3.3, so records are not required on the wire).
  Then **both** `clojure.edn/read-string` **and** `*read-eval* false` round-trip safely.
  This is R1-S1's alternative (b) and satisfies the security intent that motivated
  `false`.
- **Simpler but unsafe:** drop the binding and use `clojure.core/read-string` at its
  default (`*read-eval* true`) — round-trips (proven), but reinstates the `#=` eval
  surface on the on-disk log that the `false` was guarding against. Not recommended
  without a written note that the log path is trusted/permission-locked.

Either way, "core reader round-trips them under `*read-eval* false`" is FALSE. Escalate
per §9 (serialization format is a contract commitment repeated in §3.C/§3.E/G8).

Evidence: `relation_kernel.clj:215-222,276-282,872-878` · empirical repro (`clj -M:dev`,
Clojure 1.12.4) · CONTRACT §3.C, §3.E, G8.

---

## SHOULD-FIX

### SF-R2.1 — The /assert **writer** and the boot **replayer** have no shared source for `:assert-log-path`; making them share breaks §6's "PW parallel-safe" claim, and a silent path divergence breaks durability (G8-as-test won't catch it)

B2's literal complaint (replay is homeless) is closed, but the fix opened a new seam.

- The **replayer** (`replay-assert-log!`, defined in `git_spine.clj`, called from the
  ingest boot hook) reads its path from the boot `cfg`, which is built in
  `file_viewer.cljc:163` and threaded through `start-ingest-watchers!`
  (`ingest_watchers.clj:201`, destructures the cfg map). P1 owns all of this — fine.
- The **writer** (§3.E `/assert` route) lives in `wrap-file-api`, whose signature is
  `[next-handler]` (`server_jetty.clj:770`) — **there is no cfg map at the route.**
  Routes reach state only through closed-over `def`/`defonce` and already-required
  namespaces (`fv` is required, `server_jetty.clj:9`, so `fv/trail-rt` works; but no
  path config is in scope). So §3.E's "cfg `:assert-log-path`" has **no injection
  point** on the writer side.

For the writer and replayer to agree on the file, the path must come from a **shared
constant**. But:
- Putting it in `git_spine.clj` (P1's NEW file) forces `server_jetty.clj` (PW) to
  `require` it → **PW now depends on a P1 artifact**, contradicting §6 ("PW …
  Parallel-safe with P1/P2 … reads `fv/trail-rt` which already exists").
- The one ns already required by BOTH sides is `util_fns` (`server_jetty.clj:15`,
  `ingest_watchers.clj:22`) — but it is **out of the §7 allowlist** ("everything else …
  UNTOUCHED"), so a shared const cannot legally live there.

If the two sides independently pick paths, they diverge silently: the route appends to
file A, boot replay reads file B, and durability is quietly lost. **G8 will not catch
it** — as a single-process test it sets one path for both ends, so the production
divergence never manifests in the gate.

**Fix:** pin the EXACT default path derivation in the contract (e.g. a literal under
`user.dir`) so P1 and PW each compute the identical path with **zero shared code**
(preserves parallel-safety), OR name an allowlisted shared-const home and relax §6's
parallel-safety claim for PW. Note the same applies, less severely, to
`:spine-cursor-path` (single-owner P1, so lower risk).

Evidence: `server_jetty.clj:770,9,15` · `file_viewer.cljc:163-171` ·
`ingest_watchers.clj:201,22` · CONTRACT §3.C, §3.E, §6, §7.

### SF-R2.2 — §3.D still tells the FEED constructor to emit `"<sha7> · <subject>"`, directly contradicting the amended G9 (and reproducing the exact S2 defect)

The amendment fixed the gate (G9) but not the component description (§3.D):

- **§3.D** (unchanged): "commit-artifact entries → `"<sha7> · <subject>"` (from
  canonical text's first lines)" — placed "at the feed constructors."
- **G9** (amended): "commit FEED entries carry `<sha7>` derived from source-ref; the
  View-3 BUNDLE rendering (which reads content) shows `<sha7> · <subject>`."

These conflict. The feed row is a `SourceIngestCompletionRow` carrying `source-ref`
(= `"git-commit:<sha>"`) + counts and **no subject / no body**
(`markdown_adapter.clj:227-236`; feed use `trail_view.clj:503-512`) — exactly what R1's
S2 established. §3.D's "(from canonical text's first lines)" is doubly wrong at the feed
seam: wrong location (feed, not bundle) and unavailable data (no canonical text on the
completion row). The subject only becomes reachable in the **bundle**, where
`gather-target-material` carries `:material {:content-text …}`
(`trail_view.clj:129-151`) and `:material :raw :source-ref` (`:584,:654`) into `tb`, so
`render-bundle-text` (`:627-667`) can derive `<sha7> · <subject>`.

A builder following §3.D literally rebuilds the S2 wall the amendment claimed to remove.
**Fix:** align §3.D with G9 — feed constructor → `<sha7>` from source-ref; View-3 name
preference → `<sha7> · <subject>` from the bundle's content-text.

Both halves of the amended G9 ARE testable: feed `<sha7>` (derive from the completion
row's source-ref); bundle `<sha7> · <subject>` (assert the projected View-3 text over
the commit doc-id). So G9 is sound; only §3.D's stale prose is the problem.

Evidence: CONTRACT §3.D (lines 145-150) vs G9 (lines 226-231) ·
`markdown_adapter.clj:227-236` · `trail_view.clj:503-512,129-151,584,627-667`.

---

## VERIFIED CLOSED (R1's blockers — re-checked against source, they hold)

### B1 CLOSED — the id math is public/pure/importable, and the `:container` edge joins exactly like the green suite

**Part 1 — pure, importable, single-source-of-truth id math.** All OC id fns are public
`defn`s that call only `core/sha-256` + string ops (no ingest, no topology, no side
effects), so `git_spine` can call them directly:
- `oc/source-ref-key` (`object_container.clj:197-199`)
- `oc/object-key-for` (`:201-203`) — `sha256(source-ref ⊕ sep ⊕ source-hash)`
- `oc/document-id-for-object-key` (`:209-211`) — `"oc:doc:" + object-key`
- `oc/document-id-for` (`:254-256`) — the composed `(document-id-for-object-key
  (object-key-for source-ref source-hash))`
- `oc/source-hash` (`:258-260`) — `sha256(raw-text)`

`commit->document-id` is exactly `(oc/document-id-for "git-commit:<sha>" (oc/source-hash
canonical-text))`. The md path mints the **identical** id: the request builder computes
`source-hash = (oc/source-hash raw-text)` and `object-key = (oc/object-key-for
source-ref source-hash)` (`markdown_adapter.clj:385-389`); materialization recomputes
the same and stores `document-id` (= `"oc:doc:<object-key>"`) as BOTH the
`SourceArtifactRow` document-container-id field (`:213`) and the `ObjectContainerRow` id
(`:252`). So **document-container-id == document-id**, and extractor and adapter cannot
diverge **provided git_spine feeds the same canonical text to both and does not override
`source-hash`** (see A-R2.1). The v1.1 naming ("document-container-id" vs the fn
`commit->document-id") is loose but denotes the same value.

**Part 2 — the join.** `->target-ref :container id` sets `target-key =
(oc/extract-object-key id)` (`relation_kernel.clj:842-843`); for `"oc:doc:<object-key>"`
that is `object-key` (`object_container.clj:313-314`). `read-context-bundle` reads
relations under `object-keys = (map oc/extract-object-key material-ids)`
(`trail_view.clj:466-468`), and `"oc:doc:"` is an admitted `:material` prefix
(`trail_view.clj:64`). So an edge on `:container`+`oc:doc:<object-key>` surfaces when the
commit doc is rendered. This is **precisely** the green-suite precedent, which is green:
`doc1-id = (oc/document-id-for-object-key (:object/key d1))` (`trail_view_test.clj:95`),
`okey1 = (:object/key d1)` (`:98`), `d1-ref = (tref :container doc1-id)` (`:136`),
asserted `:based-on` (`:143`), rendered `(read-context-bundle rt [doc1-id conv*] {})`
(`:197`), and gate 1 asserts `bundle relation-ids == R1 rows for okey1` (`:208-214`).
View-3 (`render-bundle-text`, `trail_view.clj:627-667`; wired at
`file_viewer.cljc:185-187`) projects each target's `:relations :this` via `edge-line`
(`:620-625`), so G11's "commit and session in one thread" renders through the same path.

**Conversation half (duty §8.2) is adequately handled.** The chat-conversation container
id is `"oc:chat-conversation:chat:" + sha256((name source) ":" conversation-id)`
(`transcript_identity.clj:6-16`), and the live source is the known constant
`:claude-code` (`transcript.clj:88`). `extract-object-key` of that container id =
`"chat:<sha>"` (`object_container.clj:295-296`) — which matches the ingested
conversation's object-key — so the extractor **can** mint a joining conversation
endpoint. §2.3 correctly lets duty §8.2 choose that or the honest-dangling
`:conversation`+session-id fallback; either way G11 passes via the commit side (the
`:produced` edge is filed under the commit's object-key regardless of the far end).

### B2 CLOSED — replay code now has an allowlisted home (modulo SF-R2.1)

R1's B2 was "the replay call has no PW-owned boot hook." v1.1 reassigns replay to P1:
`replay-assert-log!` / `spine-sync!` / `extract-session-joins!` are DEFINED in the NEW
`git_spine.clj` (P1-owned, §7 NEW) and CALLED from the ingest boot via
`ingest_watchers.clj` (P1-owned, §7 "hook + cfg"); `file_viewer.cljc` receives only cfg
keys (§7 "cfg plumbing lines ONLY"). The existing boot future
(`file_viewer.cljc:156-172`) already calls `initial-sweep!` then `start-ingest-watchers!`
with an extensible `cfg`, so the spine sequence rides inside the ingest hook and
`file_viewer`'s future body need not change functionally. R1's "before watchers" ordering
constraint is gone — v1.1 says only "after the initial sweep." **Homeless code: none
remaining** (the residual is the shared *path*, SF-R2.1, not homeless code). Adding
`:repo-root :transcript-roots :spine-cursor-path :assert-log-path` to file_viewer's cfg
map IS "cfg plumbing," so §7 is internally consistent on the P1 side.

### R1 advisories A1–A4 — all folded

- **A1** (journal drops on duplicate; "decisions are replays" needs the twice-replay
  reading) → folded into **G8**: "same-cluster double replay → zero new decision/event
  rows; FRESH cluster + replay → same relation-ids … exactly one event per edge"
  (CONTRACT G8, lines 219-225).
- **A2** (stable idempotency-key is the real guard) → folded into **trap 4**: `"spine:" +
  relation-id + ":" + basis`, pre-check via `read-relation-detail` (lines 181-185).
- **A3** (`fv/trail-rt` is a bare `delay` deref; no timeout) → folded into **§3.E**:
  "wrap in future+timeout, return 503 … never hang" (lines 163-166).
- **A4** (no exported target-kind validator; `->target-ref` never throws) → folded into
  **§3.E**: "target-kinds against an explicit route-side allowlist — validator A4"
  (line 158-160). Confirmed against `relation_kernel.clj:840-846` (`:else … verbatim,
  never throw`).

---

## ADVISORY

- **A-R2.1 — state the id non-divergence precondition.** "Extractor and adapter can never
  disagree" (§2.3) holds only if git_spine (a) feeds the identical canonical text to both
  the md import-request builder and `commit->document-id`, and (b) does NOT pass an
  explicit `:source/hash`/`:source-hash` opt to the request builder (it accepts an
  override, `markdown_adapter.clj:385-386`). The natural implementation satisfies both;
  a one-line note in §3.A would make the guarantee airtight.
- **A-R2.2 — duty §8.2 "from session-id ALONE" is imprecise.** The conversation container
  id derives from (source, session-id) where source is the known constant `:claude-code`
  (`transcript.clj:88`). It IS derivable and joinable; the "alone" wording risks a
  literal-minded builder concluding "not derivable → dangling" and needlessly taking the
  worse branch. Reword to "from known inputs (the source constant + session-id)."
- **A-R2.3 — §8 duty numbering is out of order (1, 2, 6, 3, 4, 5).** The new "prove the
  join" duty is labeled 6 but placed between 2 and 3. Cross-refs (§8.2, §8.6) depend on
  the numbers, so keep the labels but reorder, or renumber. Cosmetic.
- **A-R2.4 — §1 vs §3 reuse letters A–E with different meanings** (§1.E = display names,
  §3.E = /assert shim; §1.C/D = transcript edges, §3.C = ingest_watchers). Pre-existing,
  not v1.1-introduced, but a fresh builder can misroute a duty. A one-line component-map
  would help.

---

## Appendix — empirical S1 round-trip (required deliverable)

Built a representative envelope with the repo's real `rk/assert-request` (a `:produced`
conversation→container assert, `:import` custody), `pr-str`'d it, and read it back four
ways. Clojure 1.12.4, `clj -J-Xss16m -M:dev`, this repo.

Serialized envelope (abridged):
```
{:relation/routing-key "rel:8ee2fe60…", :request/type :relation/assert, …
 :payload #app.server.rama.relation_kernel.RelationMutationPayload{
   :relation-kind :produced,
   :from #app.server.rama.relation_kernel.RelationTargetRef{:target-kind :conversation, :target-id "sess-123", :target-key "sess-123"},
   :to   #app.server.rama.relation_kernel.RelationTargetRef{:target-kind :container,    :target-id "oc:doc:abcdef0123456789", :target-key "abcdef0123456789"}, …}}
```

```
--- clojure.core/read-string  *read-eval* FALSE ---
  THREW  java.lang.RuntimeException :
         Record construction syntax can only be used when *read-eval* == true
--- clojure.core/read-string  *read-eval* TRUE (default) ---
  OK   equal-to-original?  true   payload is record? true   from is record? true
--- clojure.core/read-string  (no binding, ambient default) ---
  OK   equal-to-original?  true   payload is record? true   from is record? true
--- clojure.edn/read-string ---
  THREW  java.lang.RuntimeException :
         No reader function for tag app.server.rama.relation_kernel.RelationTargetRef
=== ambient *read-eval* value === true
```

**Conclusion:** the ONLY reader that round-trips the pr-str'd record envelope is
`clojure.core/read-string` with `*read-eval*` at its default (`true`). The v1.1-mandated
`*read-eval* false` **throws** (record-construction guard). To read under `*read-eval*
false` (or under `clojure.edn`) safely, the envelope must be serialized as **plain maps**,
not defrecords. → B-R2.1.

## Appendix — `file(1)` output
```
CONTRACT_VALIDATION_R2.md: Unicode text, UTF-8 text
```
NUL-byte scan (`grep -P '\x00'`): clean.
