# CONTRACT_VALIDATION_R1 — git-spine WP2 (+ /assert shim)

**VERDICT: FAIL**

Fresh-context, default-fail P-V round (2026-07-05). FAIL because ≥1 finding blocks
building as written. Two BLOCKERs (F1 render-join mismatch → gate G11 unbuildable;
F2 boot-replay has no allowlisted home). Several SHOULD-FIX and ADVISORY items.

Every INPUTS cite the contract leans on was re-grepped against source; line numbers
had drifted slightly but **all FACTS held**. The contract's mechanical claims that
I could verify DID verify (in-process cluster, md-adapter reuse, deterministic ids,
journal idempotency, the read-wrapper for the pre-check). The FAIL is not about
those — it is about two integration seams the fact base did not close.

`file(1)` results (task requirement) — all three report `text`:
- `CONTRACT.md` → **Unicode text, UTF-8 text**
- `INPUTS.md` → **Unicode text, UTF-8 text, with very long lines (600)**
- `CONTRACT_VALIDATION_R1.md` (this file) → **Unicode text, UTF-8 text**

NUL-byte scan (trap 10 / gate-12 discipline): all three clean.

---

## BLOCKERS

### B1 — Edge targets (`:git-commit`+sha, `:conversation`+session-id) are DISJOINT from the rendered objects' keys ⇒ threads never surface in the bundle/View-3 ⇒ G11 unbuildable as written

This is the load-bearing finding. The whole WP is "give the trail view **threads**"
(§1), and G11 is the integration gate: "View-3 text projection over a fixture with
one transcript→commit join shows the commit and the session in ONE thread." That
render path cannot see these edges.

**How the trail view joins relations to a rendered object** (the working mechanism):
`read-context-bundle` looks up relations for a material target `tid` by
`object-key = (oc/extract-object-key tid)` and calls
`rk/read-relations-for-targets rt [object-key]`
(`trail_view.clj:466-468`). An edge surfaces on `tid` **only if one of its endpoints
has `target-key = (extract-object-key tid)`**. Edge copies are filed in
`$$relations-by-target` under the endpoint's `target-key` (`relation_kernel.clj:731,739`),
and `target-key` is computed by `->target-ref` (`relation_kernel.clj:835-846`).

**What the git-spine contract mandates for endpoints:**
- Commit edges target `:git-commit` + full sha (§2.3). `->target-ref :git-commit sha`
  ⇒ `target-key = (str sha)` (`relation_kernel.clj:844`).
- Conversation edges target `:conversation` + **bare session-id** (§8 duty 2 fallback).
  `->target-ref :conversation session-id` ⇒ `target-key = (oc/extract-object-key session-id)`;
  for a bare UUID session-id that hits the `:else` branch and returns the string
  **verbatim** (`object_container.clj:334`). So `target-key = session-id`.

**What the ingested objects' keys actually are:**
- The commit OBJECT (component A) is ingested through the md path with
  `source-ref = "git-commit:<sha>"` ⇒ its id is `"oc:doc:<object-key>"` where
  `object-key = sha256("git-commit:<sha>" ⊕ sep ⊕ source-hash)`
  (`object_container.clj:201-211`, `markdown_adapter.clj:184-186`).
  `extract-object-key("oc:doc:<object-key>") = object-key` (`object_container.clj:313-314`).
  **object-key ≠ sha.**
- The conversation CONTAINER is `"oc:chat-conversation:chat:<sha256(claude-code:conversation-id)>"`
  (`transcript_identity.clj:6-16`, `transcript_adapter.clj:227,254`).
  `extract-object-key` of it = `"chat:<sha256(...)>"` (`object_container.clj:295-296,262-271`).
  **"chat:<sha256(...)>" ≠ bare session-id.**

**Therefore both endpoints of every git-spine edge are filed under target-keys
(bare sha; bare session-id) that match NO ingested object's object-key.** Rendering
the commit object or the conversation container via `read-context-bundle` looks up
relations under `object-key` / `"chat:<sha>"` and finds **none of these edges**. The
edges are dangling relative to the material (legal per `relation_kernel.clj:285`, but
invisible to the render join). G11 has no path through `read-context-bundle` /
`render-bundle-text`.

**Corroboration — the working precedent does the opposite.** The green trail-view
suite asserts relations against the OC **container ids**, precisely so they join:
`d1-ref (tref :container doc1-id)` and `conv-ref (tref :conversation conv*)` where
`conv* = (:container-id conv)` is the real `oc:chat-conversation:...` id, NOT the bare
`conv-id "tv-conv"` (`trail_view_test.clj:135-137`). Gate 1 then asserts
`bundle-ids == R1 rows for okey1` and passes **because** those endpoints'
`target-key` equals the doc's `object-key` (`trail_view_test.clj:207-214`). The one
`:git-commit`+bare-sha edge in that suite (`:references … (tref :git-commit "sha-backdated")`,
`trail_view_test.clj:172`) is used ONLY to test the activity feed's back-dated
bucketing — never to render a join — which is exactly the dangling role.

**Why "decouple A from B/C" (§2.3) does not save it:** §2.3 correctly makes the
extractor not *require* commit objects to exist (a build-order property). But it
silently also makes edges and objects use different identities, and provides no
re-join for rendering. Build-order decoupling ≠ render-time join. As written, a
builder who follows §2.3 + §8-duty-2 produces edges that G11 cannot render; a builder
who makes G11 pass (target the container ids) violates §2.3 + §8-duty-2. The
contract's edge-target mandates and its own render gate are mutually inconsistent.

**Fix direction (policy, not implementer):** either (a) assert an additional identity
bridge (`:doc-file`/`:container` endpoint on the commit object's `oc:doc:<object-key>`
id, and the conversation's real container id) so `extract-object-key` lands on the
object-key, or (b) redefine G11 to read by the sha/session-id target-keys directly
(kernel R1), accepting that View-3/context-bundle will not show git-spine threads on
the material objects in v1. Both touch adjudications (§2.3) — escalate per §9.

Evidence: `trail_view.clj:454-477,232` · `relation_kernel.clj:835-846,731,739,285` ·
`object_container.clj:279-334,201-211,313-314` · `transcript_identity.clj:6-16` ·
`transcript_adapter.clj:227,254` · `markdown_adapter.clj:184-186` ·
`trail_view_test.clj:135-137,172,207-214`.

### B2 — Component W's boot replay has no allowlisted home

§3.E places boot replay "**in the same boot future, before watchers**." That boot
future is the `trail-view-runtime` `defonce delay`'s `(future …)` block in
**`file_viewer.cljc:164-171`** — the only place `initial-sweep!` and
`start-ingest-watchers!` are called. But:

- §6 PW: "server_jetty only; **does NOT touch file_viewer**."
- §7 allowlist: `file_viewer.cljc` is "**cfg plumbing lines ONLY — P1 only**";
  `server_jetty.clj` is "route ONLY — PW only"; "One builder per file, no exceptions."

So the PW builder cannot add the replay call to `file_viewer.cljc` (barred by §6/§7,
and it is functional boot wiring, not "cfg plumbing"). The only PW-owned file is
`server_jetty.clj`, whose `start-server!` (`server_jetty.clj:1100`) is NOT the trail
boot future and does not currently force the trail runtime — it is invoked from
`src-dev/dev.cljc:53`, and there are **no callers of `start-server!` in `src/`**.
Placing replay there would (a) contradict §3.E's stated location, (b) force the lazy
`(fv/trail-rt)` delay to boot eagerly at server start — changing the
"first /trail pull pays the boot" semantics (INPUTS §6.1) — and (c) not actually run
"before watchers" (watchers live on the delay's own future).

Contrast component **C**, which is *consistent*: §7 gives `ingest_watchers.clj`
"hook + cfg" to the P1 builder, so the `spine-sync!`/`extract-session-joins!` calls
can live inside an `ingest_watchers` fn that the boot future already calls, with
`file_viewer.cljc` getting cfg keys only. Component W has no equivalent PW-owned
boot hook.

**Fix direction:** amend the allowlist to let the replay call sit in the boot future
(`file_viewer.cljc`), or define a PW-owned boot hook in `server_jetty` and accept
eager trail-runtime boot + reword §3.E. Small change, but "as written" it is
unplaceable.

Evidence: `file_viewer.cljc:146-172` · `server_jetty.clj:1100,1104` ·
`src-dev/dev.cljc:53` · CONTRACT §3.E, §6 PW, §7.

---

## SHOULD-FIX

### S1 — "append the full serialized envelope as one **edn** line" cannot round-trip; `clojure.edn/read-string` crashes on the envelope's defrecords

The relation envelope's `:payload` is a `RelationMutationPayload` record whose `:from`
/`:to` are `RelationTargetRef` records (`relation_kernel.clj:867,220-222,215-218`).
`pr-str` prints them as record literals
(`#app.server.rama.relation_kernel.RelationMutationPayload{…}`). **Empirically
verified in this repo's Clojure:**
- `clojure.core/read-string` → reconstructs the records faithfully (round-trip equal).
- `clojure.edn/read-string` → **throws** `RuntimeException: No reader function for tag …`.

`server_jetty.clj` already uses `clojure.edn/read-string` for bodies
(`parse-edn-body`, `:99`), so a builder naturally reaching for "edn" for the replay
file will ship a boot-replay path that crashes on the first real envelope. G8's
"fresh IPC + replay of the file" *would* catch it, but the §3.E wording actively
points at the bug. Replay must use `clojure.core/read-string` (trusted internal data)
or the envelope must be converted to plain maps before serialization (the topology
reads the payload via keyword lookups — `relation_kernel.clj:413-421` — so plain maps
also work). Either way, "edn line" is a misnomer; state the reader explicitly.

Evidence: `relation_kernel.clj:220-222,215-218,853-870` · `server_jetty.clj:94-99` ·
empirical repro (`clojure -M`).

### S2 — G9's commit display-name `"<sha7> · <subject>"` is not derivable at the feed constructor (subject not carried)

Component D adds `:display-name` at the feed constructors (INPUTS §5.7b →
`trail_view.clj:493,503`). A commit ingested via the md path surfaces in the feed
through `source-activity-entry`, whose row is a `SourceIngestCompletionRow` carrying
`source-ref` (= `"git-commit:<sha>"`) + `document-container-id` +
`derived-unit-count` — and **no subject / no body text**
(`markdown_adapter.clj:227-236`; feed use at `trail_view.clj:503-512`). The sha7 is
derivable from `source-ref`; the **`· <subject>`** is not present at this seam. The
subject lives in the document body (`:current-content-text`), which is only available
in `gather-target-material` / the context-bundle path (`trail_view.clj:151,257`), not
in the feed row.

So G9's "commit entries `"<sha7> · <subject>"`" is buildable for **View-3**
(content-text available) but not for the **feed** (subject absent) without an extra
per-entry document read (outside §7's "feed :display-name … ONLY" scope) or an OC
schema change (explicitly out of scope, §1). As written, the feed can only show
`"<sha7>"`. Specify the subject-bearing seam or drop `· <subject>` from the feed.

Evidence: `markdown_adapter.clj:227-236` · `trail_view.clj:503-512,151,257` · §7.

### S3 — §8 duty-2 conversation fallback answers point 7 but guarantees a non-joining conversation endpoint

Point 7 asked what `oc/extract-object-key` does with a bare session-id: it returns it
**verbatim** (`:else`, `object_container.clj:334`) — so it IS deterministic, and the
§8-duty-2 fallback (`target-id = session-id verbatim`) is what naturally happens. But
the resulting `target-key = session-id` never equals the ingested conversation
container's key `"chat:<sha256(claude-code:session-id)>"` (`transcript_identity.clj:6-16`).
This is the conversation half of B1: the "session" side of a thread will not link to
the ingested conversation. If the intent is a joinable conversation endpoint, the
`from` must be the conversation **container id** (`oc:chat-conversation:chat:<sha>`),
not the bare session-id. Record this explicitly in the phase artifact per §8 duty 2 —
the fallback is not a neutral default, it decides B1's conversation side.

Evidence: `object_container.clj:279-334` · `transcript_identity.clj:6-16` · §8 duty 2.

---

## ADVISORY

### A1 — The journal is DROP-not-replay; G8's "decisions are replays" needs the twice-replay reading

The idempotency gate is `(filter> (nil? *prior-decision))` keyed on
`[relation-id journal-key]` (`relation_kernel.clj:676-679`) — on a duplicate it
**drops the record and writes nothing** (no re-emitted decision). In a *fresh* IPC
(as G8 says), the first replay of the file finds an empty journal, so every envelope
produces a **fresh `:accepted` decision** and writes its edge/event/activity — that is
the durability restore, not a "replay." To observe "decisions are replays, zero
duplicate events," the test must replay the file **twice into the same IPC** (2nd pass
is dropped at the gate → zero new events). The property is testable
(`read-activity-rows` / event counts + the microbatch barrier, `relation_kernel.clj:978-984`),
but the gate wording will mislead. This confirms point 3's core question in the
affirmative: identical re-append **is** idempotent at the journal — via drop, given a
**stable** idempotency-key (see A2).

### A2 — Trap-4 pre-check is a cost optimization; the journal (with a stable idempotency-key) is the real guard

Point 4: **yes, a public wrapper exists** — `rk/read-relation-detail rt relation-id`
(`relation_kernel.clj:940-943`), exposed on the trail runtime as
`:relation-detail-query` (`trail_view.clj:710`); `(:row …)`/`:relation-status`
answers "already asserted?" cheaply from the app process. So the pre-check has an
implementation path. **But** the pre-check alone is not the idempotency guarantee: a
reassert with a *different* idempotency-key passes the journal gate and re-enters the
accepted branch, writing a new event + a new activity row even though
`count-deltas` is `{:total 0 :asserted 0}` (`relation_kernel.clj:341-345,453-503`).
So trap 4's "re-runs grow event rows" is real **iff spine-sync! mints a fresh
idempotency-key per run**. Recommend deriving a **stable** idempotency-key from the
relation-id (mirroring §3.B's cursor framing: "correctness comes from the idempotency
journal"), so re-runs converge at the gate even if the pre-check races the
`:append-ack` microbatch lag (`relation_kernel.clj:920,987-989`). The pre-check then
purely saves the append round-trip.

### A3 — `fv/trail-rt` is a bare `delay` deref (blocks; no timeout)

§3.E: "Runtime not yet booted → 503 … (deref-with-timeout), never a hang."
`(fv/trail-rt)` is `@trail-view-runtime`, a plain `delay` deref (`file_viewer.cljc:174`)
— it **blocks** for the multi-second cluster boot with no timeout. The 503 path must
wrap the deref (e.g. `(deref (future (fv/trail-rt)) ms ::pending)`); a plain deref
cannot honor "never a hang." Implementer-fixable, but the contract implies a timeout
API that the handle does not provide.

### A4 — No exported "valid-target-kind?" predicate for the /assert route's validation

§3.E route must validate "known target-kinds," but `->target-ref` accepts unknown
kinds verbatim and **never throws** (`relation_kernel.clj:840-846`). There is no
exported target-kind allowlist (the set `:container :source :git-commit :doc-file
:conversation :none` lives only in the `RelationTargetRef` docstring,
`relation_kernel.clj:216`). `registered-kind?` exists for relation *kinds*
(`:286`) but not for target-kinds. The route must carry its own target-kind allowlist.
Minor, but it is net-new validation code the contract treats as reuse.

---

## Points that PASS (contract claims that DID verify)

- **§2.5 everything in-process (point 1) — VERIFIED.** The trail cluster is
  `(create-ipc)` from `com.rpl.rama.test` inside the app JVM (`trail_view.clj:37,680`),
  held by a `defonce delay` in `file_viewer.cljc:146-172`. `ingest_watchers` runs in
  that JVM on this exact handle and already `foreign-append!`s to it
  (`ingest_watchers.clj:74`). A standalone script in another JVM calling
  `start-trail-view-runtime!` would `(create-ipc)` a fresh empty cluster (trap 2), not
  reach the running one. No external path exists. Claim holds.
- **§3.A md-adapter reuse with `source-ref = "git-commit:<sha>"` (point 2) — VERIFIED.**
  The request builder makes **zero path assumptions**: `source-ref-key = sha256(source-ref)`
  (`object_container.clj:197-199`), `object-key = sha256(source-ref ⊕ sep ⊕ source-hash)`
  (`:201-203`), source-id/document-id/idempotency-key/`material-fingerprint`/
  `routing/key` all derive from those hashes (`markdown_adapter.clj:384-514`); no
  `File.`, no basename, no `.md` check; format is hard-coded `:markdown`. A synthetic
  source-ref flows through materialization and both idempotency layers cleanly.
- **Point 4 wrapper exists** — `read-relation-detail` on `:relation-detail-query`
  (see A2). The pre-check as specified HAS an implementation path.
- **Gate mechanics (point 5) mostly executable** with existing idioms: OC ingest uses
  the decision-await barrier `await-object-container-decision` (`trail_view_test.clj:36-40`);
  relation asserts use the microbatch `wait-for-microbatch-processed-count` barrier
  (`:42-47`, `relation_kernel_test.clj:84-88`); V1 PState reads via `foreign-select`
  for negative invariants; `md/source-ingest-request text ref opts` takes a plain
  source-ref string so a fixture commit is a one-liner. Fixture git repo via
  ProcessBuilder + tmp dir is standard. **Exceptions:** G11 (B1, unbuildable), G9
  (S2, partial), G8 (A1, wording). G3 needs a git shell-out in-test (fine). G8 has no
  existing in-test HTTP-handler precedent, but `wrap-file-api` is a plain ring fn and
  is directly invokable.
- **Point 6 route registration** — the /assert route is a new `cond` branch inside
  `wrap-file-api` (`server_jetty.clj:774-1064`); **no external router registration** is
  needed. (The *replay* half is the problem — B2 — not the route.) Component C's cfg
  plumbing genuinely stays within `ingest_watchers.clj` + `file_viewer.cljc` (see B2
  contrast).
- **Point 8 miscellany** — partitioning/routing sound (`hash-by :relation/routing-key`,
  append throws on blank key, `relation_kernel.clj:621,922-924`); `:append-ack` is
  correctly flagged as no-PState-visibility (`:920,987-989`) — reads poll; jsonl
  streaming has precedent (`line-seq` in `with-open`, `server_jetty.clj:1054`); the
  serial-5s-await trap is real and the contract already forbids it (§3.A, trap 7). No
  boot-order race beyond B2/A3.

---

## Appendix — `file(1)` output

```
CONTRACT.md:               Unicode text, UTF-8 text
INPUTS.md:                 Unicode text, UTF-8 text, with very long lines (600)
CONTRACT_VALIDATION_R1.md: Unicode text, UTF-8 text
```
NUL-byte scan (`grep -P '\x00'`): all three clean.
