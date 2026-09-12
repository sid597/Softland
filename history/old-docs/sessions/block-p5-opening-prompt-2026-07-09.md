# Block-distiller Phase 5 — opening prompt (fresh session)

**You are the P5 session** of the block-kernel work package
(`docs/current-mental-model/build/sense-line-mvp/block-kernel/`). P0–P4 are DONE
and GREEN. **P5 is the package's definition of done (CONTRACT §8):** build
`river-page` (render a conversation's river blocks in order, via query topologies),
run the REAL example chat (`7c80ce2a`) end-to-end and pretty-print the first river
page, and clear the review-time gates **G12** (read-plan) + **G13** (query-topology-
only consumers). No new relation kinds; no registry edit.

## Boot, in order
1. **Load skills:** `/work-package`, `/rama`, **`/rama-pitfalls`** (river-page composes
   READ query topologies — run pitfalls §7 stream-vs-microbatch / §9 subindex / the
   seek-count read-plan on the compose design BEFORE coding).
2. **Binding docs** (`decisions.md` › `SPEC.md` › `CONTRACT.md` › `PLAN.md`): SPEC §2.3
   (reconstruction/immersion — a surface re-renders its canonical text exactly), §13
   (derived statuses are PROJECTIONS, never stored), §3.2 (river/debris both retained +
   visible); CONTRACT §8 (G11/**G12**/**G13** + the DoD paragraph), §10 (river-page
   hardening is an extension point: dedicated query topology vs CommonMaterialBundle
   composition), §12/(g) (river-page realization — **lean = composition first**); PLAN §5
   (Phase 5), §7/**F3** (input enumeration has NO query door — the driver reads the
   projection PState directly; that is an INPUT substrate read, NOT what G13 governs;
   river-page's OUTPUT stays query-topology-only), N5 (`derived-content-text` is STORED so
   `read-unit` renders a block without exposing offsets — G13-clean).
3. **The P0–P4 code you build on** (`block_distiller.clj`): §I driver
   (`read-conversation-inputs` = the F3 substrate reader; `distill-conversation!`); the
   **class ledger** (`class-projection-hint` writes `entry-kind :river` under a
   `sb:%020d`-order-key entry in `$$transcript-conversation-projection`, ordered = event
   order); §K `refine!` / §L `assemble!` (P4). Read APIs: `ocr/read-common-material-for-
   source` (query topology → `{containers derived-units anchors edges}`), `ocr/read-unit`
   (query topology → `UnitReadResult`, `:unit` = `DerivedUnitRow` with `:derived-content-
   text`), `ocr/read-transcript-conversation-projection` (the class ledger + `:message`
   rows).

## State you INHERIT (verified this session — do NOT re-derive)
- **Suites GREEN, independently re-run: block_distiller_test 17t/828a; object_container
  6t/149a; relation_kernel 2t/222a, 0 fail.** (The block suite is +2t/+58a over P3's
  15t/770a — the P4 G7/G10 gates incl. idempotence.)
- **⚠ The baton was STALE and the tree had advanced** — the F1/F2 P1/P2 fixpass is ALREADY
  applied (NOT queued): `import-key` = `imp:tr:<object-key>:sb:<hash>` (routes via
  extract-object-key); `free-cut-part` drops the whole-message-coincident human sub;
  fixture has 11 events (event 10 `u-solo` = a single-line human msg → ONE `:human-message`
  block). Trust the CODE + the green suite, per the work-package precedence rule.
- **5 river events** (`u-human` `a-1` `u-tr` `a-sub` `u-solo`), 6 debris. River blocks per
  the golden (`test/resources/block-distiller/golden.edn`).
- **RK is MICROBATCH** (barrier = `rtest/wait-for-microbatch-processed-count`); OC is STREAM
  (`:append-ack` + `ocr/await-object-container-decision`). `start-distiller-runtime!
  {:relations? true}` = two IPCs (N4).
- **No stop-clause is open.** The 3 kinds (`:grounds :assembled-from :refines`) are
  registered; P4 needed no registry/OC edit.

## P5 — `river-page` (G12/G13) + e2e
Signature (PLAN §2): `(river-page {:oc-rt :object-key …} limit) → [rendered blocks in
order]`. Each rendered block = `{:order :event-uuid :actor :form :text …}` (form + the
exact material, so the page reconstructs the conversation in river order — SPEC §2.3).

**The one real P5 design fork — river-page's ORDERED enumeration (F3):** there is no query
topology that lists a conversation's ordered per-part surfaces. The `sb:` class ledger
gives ordered river EVENTS (filter `entry-kind :river`, sort by the `sb:%020d` order-key);
but the ledger entry stores event-uuid + order + role, NOT the per-part source-ids. So
river-page must, per ordered river event, resolve that event's per-part surfaces and read
each surface's units (blocks) in span order.
- **Recommended (composition-first, CONTRACT §12/g + §10):** enumerate ordered river events
  from the `sb:` ledger (a projection-PState read — the F3 INPUT category, not G13-governed);
  for each event re-derive its per-part `source-id`s (the driver holds the event-uuid; the
  per-part paths come from re-distilling the stored payload — deterministic, cheap — or from
  a per-surface read), then render each surface's blocks via `read-common-material-for-source`
  / `read-unit` (query topologies → **G13-clean OUTPUT**). Order blocks by span. If this fans
  out to N point-reads per page (G12 risk), **bank** a `read-conversation-sources` query
  topology as the hardening (CONTRACT §10 extension — a NEW query topology in
  object_container.clj = a stop-clause → Sid; do NOT build it silently). For v0 the
  composition path over ONE small chat is within G12; measure the seek plan and record it.
- **G12 (review-time):** the river-page read plan = bounded seeks + sequential iteration; no
  N-point-read fan-out per page. Audit + record the per-page seek count.
- **G13 (review-time):** consumers compile against query topologies ONLY — `river-page` uses
  `read-common-material-for-source` + `read-unit` (+ the ledger read as INPUT). No product
  path foreign-selects a PState. Test/validation readers are exempt.

**e2e / definition of done:** a documented REPL/`comment` block (or `-main`) that
`start-distiller-runtime!` → assumes/ingests `7c80ce2a` → `distill-conversation!` →
`river-page` → pretty-prints the first page in river order. The real file is spans-only
(`~/.claude/projects/-mnt-data-projects-Softland/7c80ce2a-*.jsonl`); keep the suite green
off-box (guard as `find-real-transcript` does). This is the artifact Sid + Fable each take
into the dual benchmark. **Break QUALITY is NOT gated here (SPEC ≠ BENCHMARK).**

## Stop clauses / hard rules
- A NEW query topology / any object_container.clj or relation_kernel.clj change → stop-clause
  → Sid (river-page composes EXISTING query topologies; the `read-conversation-sources`
  topology is a banked extension, not this phase's to build without a ruling).
- File allowlist: extend `block_distiller.clj` + `block_distiller_test.clj` + fixtures only.
- CODE uncommitted until Sid's word; code/docs separate commits; docs auto-commit local
  branch only. NEVER read `src/app/server/env.clj`.
- ⚠ COORDINATION (T11): `relation-kinds` carries BOTH packages' kinds (`:grounds
  :assembled-from :refines` + code-atom's `:requires :calls`). At the eventual code commit,
  confirm BOTH survive; re-run both packages' gates.
- ⚠ There is a **1-error flake** in the kernel-suite baseline (a Rama `watchable_promise`
  background-thread async error, the documented contention/probe-await family; NOT caused by
  P4, which touched neither kernel). If it recurs, treat as the known flake; if it becomes a
  hard reproducer on a specific test, flag it — it is a BASELINE issue, out of this package's
  allowlist to fix.

## At session end (this CLOSES the package)
P5 green + e2e prints page 1 ⇒ the block-kernel package is implementation-complete. Then:
run the **package gate review** (work-package skill: independent suite re-run + the batched
fresh-context falsification over the WHOLE P0–P5 diff — P4's fresh-context look rides here,
per the delivery-mode batched-falsification ruling) → record the gate verdict in
`decisions.md` + a gate artifact → **RETRO** (`block-kernel/RETRO.md`) + adversarial
retro-recheck → route lessons (/atomize, /work-package, implementation-quirks). Code commits
on Sid's word (T11 hunk care). Append a NOW entry (≤15 lines) to `next-prompt.md`.
Agreed path after the package closes (next-prompt.md top): run the example chat through →
chunks in Rama → the dual benchmark (Sid ∥ Fable, independent then merge) → dogfood
reconciliation UI → then the marks/kinds side.
