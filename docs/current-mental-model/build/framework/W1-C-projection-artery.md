# W1-C — the projection + the artery

2026-07-11 · Lane W1-C (Opus 4.8 subagent). Realizes framework CONTRACT §7 (the one
generic artery) + gates G10–G13. **Status: DONE — G10–G13 green in-context** (7 tests /
73 assertions, 0 fail / 0 error, over the REAL `7c80ce2a` corpus). Booted `/rama` +
`/electric-docs` before any code (mandatory). Committed nothing (orchestrator owns commits).

## What was built

| File | Role |
|---|---|
| `src/app/server/rama/face_projection.clj` | Projection registry (plain map, ONE `:conversation` entry) + server-side face dispatch. READ-ONLY over the block kernel's query surface. |
| `src/app/file_viewer.cljc` (surgical add) | `FacePull` e/defn (the one generic pull) + first-light server runtime provider. |
| `src/app/electric_flow.cljc` (surgical add) | `!face-request`/`!face-data` atoms + the generic (no-case) request-watch loop. |
| `src/app/client/workspace/face_wiring.cljs` | Client glue — derives the request, mirrors the data-context; INV-19 epoch debounce; S1/S2 seam law. |
| `test/app/face_projection_test.clj` | G10–G13. |

## The shape (what W1-INT wires against)

**Request atom** `!face-request` (declared in `electric_flow.cljc` `main`, derived by
`face_wiring/install-face-wiring!` from `!face-state`):
```clojure
{:face    <assembly id — name/kw>          ; e.g. :outline → server maps to :conversation
 :address <conversation object-key>         ; e.g. "chat:10c22…" ; :default → first-light address
 :params  {:limit <1..64> :until-ms <ms|nil>}  ; :until-ms = replay/scrub cut
 :epoch   <last-seen ingest epoch>}
```

**Data-context atom** `!face-data` (declared in `electric_flow.cljc` `main`, reset whole by
the FacePull watch loop). Top-level keys:
- `:turns` — `[{:id <event-uuid> :speaker <actor> :order <event-order> :blocks [...]}]`;
  each block `{:id <unit-id> :kind <form-kw> :text <str> :order <[eo pi ok]> :time-ms <ms> :part-path :block-path}`.
  (Assemblies bind `[:turns]`, then per-turn `[:speaker]`/`[:blocks]`, per-block `[:kind]`/`[:text]`.)
- `:conversation/address` · `:conversation/limit` · `:conversation/until-ms`
- `:conversation/river-events-total` (247 on the real corpus — the TRUE durable total, never page size)
- `:conversation/blocks-returned` · `:conversation/truncated?` · `:conversation/page-complete?`
- `:conversation/debris-excluded?` (true — the projection serves river; debris reported excluded, never dropped)
- `:conversation/paging-lack` (`:river-page-has-no-cursor` when the page ceiling hit — the D-005 data-work marker)
- `:conversation/read-plan` (river-page's measured seek plan) · `:face/rendered-at-ms` (honest server stamp)
- Error shape (bad address / unknown face): `:conversation/error` + `:conversation/requested-face` + `:turns []` (never a throw).

## Design decisions grounded in empirical probes (measure before assuming)

I stood up the real `7c80ce2a` corpus (present on this box, 6.9MB) and probed the block
kernel's read surface before committing the projection shape. Findings:

1. **247 river EVENTS (turns), not 247 blocks.** `distill-conversation!`'s `:river` == the
   read-plan's `:river-events-total` == 247 (399 debris). `river-page` renders a bounded
   page — hard-capped at `bd/max-river-page-size` = **64 blocks, no cursor**. So the
   `:conversation` projection serves ONE bounded page + signals truncation. G10 asserts
   within-page fidelity (bijection with river-page, order-preserving) + honest total (247)
   + truncation — the strongest honest claim under the kernel's own 64-cap. Full-corpus
   view/scrub needs cursor paging = **block-kernel CONTRACT §10 scale extension** (D-001-gated
   kernel work), NOT built here.

2. **`:until-ms` time source.** NO block-material query API surfaces a per-block timestamp:
   block-distiller units do NOT graduate (verified `read-unit` → `:graduation nil`), and
   `DerivedUnitRow` has no time field. The honest, bounded source is the per-part
   `SourceArtifactRow.created-at-ms` (= the message production time, since the import request
   carries `:time-ms (:created-at-ms ctx)` — verified `event-import-request` at
   `block_distiller.clj:790`), read via `ocr/read-source` — ONE point-read per DISTINCT
   rendered source (≤ page surfaces ≤ `:limit`). **Verified monotone-nondecreasing across the
   real page** ⇒ `:until-ms` cuts are prefix-consistent (G11). v0 `:until-ms` scrubs WITHIN
   the first page (a consequence of #1).

## Gates

| Gate | Result | Key number / property |
|---|---|---|
| **G10** projection receipt (real corpus, ASSERTED) | **PASS** | 247 river-events durable == `:conversation/river-events-total`; flatten(turns→blocks) == river-page(64) block-for-block (`:id`/`:kind`/`:text`/`:order` bijection, order-preserving, no dup/drop); turn order = river order, monotone + distinct; every block has kind + positive monotone `:time-ms`; `:truncated? true`; debris 399 excluded-not-missing. |
| **G11** bounded prefix-consistent scrub | **PASS** | `:until-ms` cuts prefix-consistent (result(t-lo) ⊑ result(t-mid) ⊑ result(full) at block-id grain; every kept block ≤ cut); per-page cost bounded by `:limit`: `seek-bound = 1 + 4·limit`, `seek-count ≤ seek-bound`, events/surfaces/unit-reads ≤ limit — independent of conversation length. |
| **G12** read-only by construction (review-time) | **PASS** | Mechanical source scan: no `defmodule`/`<<sources`/`local-transform>`/`foreign-append`/`append-*-request`/`*-topology` in `face_projection.clj`. Reads ONLY `river-page` (block material via read-common-material-for-source + read-unit) + `read-source` (created-at-ms filter, F3-class, bounded ≤ limit). No depots, no topologies, no PState paths, no new indexes, no kernel edits. |
| **G13** generic artery (review-time + unit test) | **PASS** | ONE `!face-request`, ONE `FacePull` e/defn, ONE `!face-data`; NO face-keyword `case` in `electric_flow.cljc`/`file_viewer.cljc` (dispatch is `face-projection/serve` → projection registry). Registry unit-tested as a plain fn: request→data-context, stub-registry dispatch + unknown-face error path (no throw); client glue touches no server names (S1/S2). |

Test invocation: `clj -A:test -M -e "(require 'app.face-projection-test 'clojure.test) (clojure.test/run-tests 'app.face-projection-test)"` → **7 tests, 73 assertions, 0 fail, 0 error**. G10/G11 are guarded by `find-real-transcript` (skip off-box); they RAN and asserted on this box.

## Traps honored (cited in code comments)

- **T8** — no face dispatch in Electric; `serve` routes server-side via the projection registry.
- **T12** — read-only composition over existing query APIs; no depots/topologies/kernel edits.
- **T5** — registry is a runtime keyword→fn map; persisted form (W2) = keyword + address, never fn values.
- **INV-19** — the ingest-epoch PUSH is the only re-pull trigger (≥1s debounce); no polling.
- **S1/S2** — `face_wiring.cljs` moves data between client atoms only; the pull lives in electric_flow/file_viewer.

## Flags for W1-INT / the gate (not stop-clauses — surfaced honestly)

1. **`:until-ms` reads `ocr/read-source.created-at-ms`** — the ONE read outside the strict
   three named APIs (river-page / read-common-material-for-source / read-unit), because none
   of the three carries a timestamp. It is a bounded (≤ limit) filter-grade read of the
   F3 input class the block kernel already blesses, needs NO kernel edit / NO new index →
   inside G12's spirit; documented at the top of `face_projection.clj`. Gate reviewer should
   confirm they accept this framing (the alternative — a kernel read that surfaces time — is a
   Wave-2 kernel edit, deliberately not taken).
2. **v0 face shows only the first bounded page** (≤64 blocks) of any conversation; scrub is
   within that page. For the 247-event `7c80ce2a` corpus the Outline face wears ~the first 52
   events. This is a KERNEL constraint (river-page has no cursor), surfaced as
   `:conversation/paging-lack` and the first ordered data-work item (D-005). G15's wearing demo
   will expose it; that is the intended dynamic ("every lack the face exposes orders the data work").
3. **First-light runtime cost** — `file_viewer/face-projection-runtime` boots an in-process OC
   cluster and harvests + distills `7c80ce2a` in a `future` (minutes on the 6.9MB corpus,
   the trail-view-runtime OI-1 precedent). FacePull serves empty until the distill lands, then
   the epoch re-pull fills it. The heavy live exercise is W1-INT/G15; my gates drive
   `face-projection/serve` directly over a test-stood-up runtime.

## Fence compliance

New files: `face_projection.clj`, `face_wiring.cljs`, `test/app/face_projection_test.clj`.
Surgical additions ONLY to `electric_flow.cljc` + `file_viewer.cljc` (sole lane-C ownership) —
the artery additions, no unrelated refactors. Did NOT thread `!face-request`/`!face-data` into
`loop/start-loop!` or call `install-face-wiring!` — that mount touches shared runtime files and
is W1-INT's job. Read-only elsewhere. Both `.cljc` files compile clean on the clj side.
