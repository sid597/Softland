# Block Kernel — architecture orientation (NON-BINDING; CONTRACT.md governs)

> **⚠ SUPERSEDED IN PART (2026-07-09, same day):** §1's placement of
> block-kernel as a NEW sibling module is ON HOLD — Sid's "block = atomic
> container" challenge was upheld against `object_container.clj` (it already
> has distillers/anchors/units/edges/query-API; see CONTRACT.md banner).
> §§2–4's flow logic (foreign-side cut, deterministic ids, retry axes, read
> discipline) survives in adapter form. Redraw follows Sid's ruling.

2026-07-09 · Fable, spec room · drawn for the implementing session and for
Sid's architecture read. Every claim here is a projection of CONTRACT.md +
SPEC.md; on any drift, those win.

## 1 · Where block-kernel sits among the modules

```
                            ══ WORLD-LINE (what exists) ══
      ┌────────────────────────┐        ┌──────────────────────────────┐
      │ git-spine              │        │ object-container-module      │
      │ commits · parents ·    │        │ docs + transcripts at        │
      │ world artifacts        │        │ CONTAINER grain (convs,      │
      └───────────▲────────────┘        │ messages, tool-calls, docs)  │
                  │                     └──────────────▲───────────────┘
                  │ edge TARGETS                       │ same raw material,
                  │ (:git-commit, :doc-file)           │ DIFFERENT grain —
                  │                                    │ parallel lens, NOT a
                  │                                    │ dependency (R4)
      ══ SENSE-LINE substrate (what is said/meant) ════╪══════════════════
                  │                                    │
   ┌──────────────┴─────────────┐   driver appends  ┌──┴───────────────────┐
   │ relation-kernel (D-004)    │◀──(mechanical     │ block-kernel (NEW)   │
   │ the EDGE layer:            │   edges, idem-    │ the ADDRESS layer:   │
   │ typed, provenance-carrying │   potency keys)   │ surfaces · blocks ·  │
   │ assertions; OPEN target-   │                   │ forms · prod-events  │
   │ kinds — now incl. :block   │                   │ "what can be pointed │
   └──────────────▲─────────────┘                   │  at, at thought grain│
                  │                                 └──────────▲───────────┘
                  │ marks-as-edges (kinds round, LATER)        │ query topologies ONLY (G13)
                  │                                            │
        ┌─────────┴────────────┬───────────────────┬───────────┴────────┐
        │ marker (kinds round) │ dual benchmark    │ reconciliation UI  │
        │ LATER                │ (Sid ∥ Fable) NEXT│ (dogfood, step 5)  │
        └──────────────────────┴───────────────────┴────────────────────┘
        further out: consolidator (episodes) · return-path/briefing assembly
```

The pair to hold: **relation-kernel = how things relate; block-kernel = what
can be related, at thought grain.** Marks (kinds round) will be assertions
over both. Containers stay a parallel evidence lens (A1); blocks do not
replace them.

## 2 · The ingest flow (foreign side vs Rama side, with event boundaries)

```
FOREIGN SIDE — the driver (plain Clojure, one-shot, re-runnable)   │  RAMA SIDE — block-kernel-module
═══════════════════════════════════════════════════════════════   │  ═══════════════════════════════
                                                                   │
 ~/.claude/projects/…/7c80ce2a….jsonl                              │
      │  walk/parse (transcript.clj fns — LIBRARY reuse)           │
      ▼                                                            │
 event map ──▶ REDACT (versioned pass; canonical text minted       │
      │        here, exactly once — R1)                            │
      ▼                                                            │
 CLASSIFY river/debris  +  RESOLVE actor (role ≠ actor — T1)       │
      ▼                                                            │
 FREE CUT (pure fns): provider parts → markdown units              │
      │                → spans + forms (unit-testable, no IPC)     │
      ▼                                                            │
 MINT deterministic ids (R3):                                      │
   surf: sha256(session ∥ event-uuid ∥ part-path)                  │
   blk:  sha256(surface-id ∥ start ∥ end)                          │
      │                                                            │
      ├── foreign-append! :append-ack ──────────────▶  depot *block-events
      │                                                (hash-by :block/session-id)
      │                                                            │  ONE microbatch topology
      │                                                            ▼
      │                                       ┌─ EVENT 1 · session task ──────────┐
      │                                       │ $$session->surfaces   (subindexed)│
      │                                       │ $$session->blocks     (subindexed)│
      │                                       │ $$session->events-ledger          │
      │                                       │ $$session->prod-events            │
      │                                       └────────────────┬──────────────────┘
      │                                              (|hash *surface-id)  ← boundary!
      │                                       ┌─ EVENT 2 · id task ───────────────┐
      │                                       │ $$surfaces-by-id (pointer row)    │
      │                                       │ $$blocks-by-id   (pointer row)    │
      │                                       │  NOT atomic with event 1 — T13:   │
      │                                       │  readers nil-tolerate skew        │
      │                                       └───────────────────────────────────┘
      │
      └── mechanical edges: foreign-append! :append-ack ──▶ relation-kernel depot
          produced · grounds · assembled-from · refines        │ (its own topology,
          idempotency key = sha256(blk ∥ kind ∥ target) — T11  │  its own PStates —
          so re-runs DEDUPE inside relation-kernel             │  module untouched)
```

## 3 · The read path (the only lawful door)

```
 consumer (benchmark CLI · UI · later the marker)
      │ foreign-invoke-query
      ▼
 QUERY TOPOLOGIES — read-only, G13: no consumer touches PState paths
 ┌──────────────────────────────────────────────────────────────────────┐
 │ river-page(session, from, n)  2 seeks + iterate → ordered surfaces   │
 │                               ⋈ their blocks; text sliced ON READ    │
 │                               from the ONE stored copy (T6)          │
 │ block-text(block-id)          ptr → surface → slice                  │
 │ block-resolve(surface, span)  1 seek → id | nil (ids computable      │
 │                               client-side too — R3 determinism)      │
 └──────────────────────────────────────────────────────────────────────┘
```

## 4 · Why the shape survives retries (the three axes)

```
 driver re-run      → same deterministic ids → PStates converge (G4: zero drift)
 topology retry     → microbatch exactly-once; ZERO world effects inside topology (T3/T11)
 edge re-append     → relation-kernel idempotency PState dedupes (T11 keys)
```

## 5 · Reuse decisions (what existing code is used, and why / why not)

| existing thing | used? | how / why not |
|---|---|---|
| `dogfood/transcript.clj` reader+redaction fns | **YES — as a library** | plain defns (jsonl walk, parse, redact, line-hash); code reuse without module coupling |
| `relation-kernel-module` | **YES — as-is via its depot** | open `target-kind` accepts `:block` with zero changes; its idempotency PState is the dedupe; the marks layer lands on the same substrate later |
| `object-container-module` | **NO (parallel, not dependency)** | container grain ≠ block grain; different identity law + pace layer; 2.6k-line hardened import path = blast radius; R4 keeps an adapter as a later extension point |
| `git-spine` | not now | its commits become `produced` edge targets (`:git-commit` kind already exists) |
| `space / compute / llm` kernels | no | workspace-runtime domain; no overlap with addressing |
| `core.clj` conventions | yes | V0/V1 schema + envelope conventions followed |

## 6 · Is block-kernel common underlying functionality?

By design, yes — it is the sense-line's **address substrate**: the noun store
everything else points into (rough analogy: what the object store is to git).
Planned consumers, in arrival order: dual benchmark → reconciliation UI →
marker/kinds (marks target block-ids/occurrences) → consolidator (episodes
group blocks) → return-path/briefing (assembles blocks; buildup re-enters as
new surfaces). BUT per D-001 honesty: v0's job is ONE chat and the benchmark;
it EARNS common status when consumers actually arrive and it survives the
dual-benchmark falsifier. Nothing else builds against it before that.
