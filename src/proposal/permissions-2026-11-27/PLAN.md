# Plan: Permission Rebuild — Single-Quarterback Execution, Verification & Technical Map

**Date:** 27 November 2026  
**Quarterback & Source of Truth:** `Window A` + `PLAN.md` + `./scripts/fire` (updated with empirical Loop 0a Rama `1.6.0` IPC probe + 20,857-line `test-store/` scan receipts).  
**Authority:** A session's planning text throughout. `PERMISSIONS.md` §6 holds Sid's decisions; `BUILD.md` holds the behavioral requirements of Batches 1–4 (re-ordered here by layer and verification gate rather than four full-stack passes). Nothing here overrides a decision of Sid's, and live execution receipts supersede unrun assumptions.

---

## 1. Governing Heuristics (`H1–H4`), Roster & Uncontamination Wall

### 1.1 The 4 Quarterback Heuristics (`H1–H4`)

- **`H1` — Hardness = Silent-Pass Risk $\times$ Backtrack Radius $\implies$ Route by what judges the work:**
  1. *Reading-judged shapes & words (`CONTRACT.md`, rule sentences, final diff vs. §6):* Unanchored cross-family views — `Fable max` (`Window B`) derives `CONTRACT.md` + `ALLOWLIST.md` blind from `PERMISSIONS.md` + `BUILD.md` + empirical probe facts (`LOOP0-EMPIRICAL-FACTS.md`), and `Window A` attacks/diffs it against `Window A`'s frozen pre-Fable derivation (`window-a-contract-draft.md`), with `SID'S EXPLICIT CALLS` strictly separated from `ENGINEERING DEFAULTS` up front.
  2. *Fast `<20s` pure-run-judged code (`Loop 2B` pure kernel):* `Window A` builds all Batch 1–4 pure rules **once** (no throwaway intermediate batch rules) against `Window B`'s locked `Loop 2A` Judge, which `Window A` may never edit.
  3. *Riskiest stateful Rama + live-read wiring (`Loop 3`, ~500 lines):* Default is fresh `Fable xhigh` (`max` on `read_path.clj`) building it so `Neon + Carbon` attack `Loop 3` **cross-family** on a cluster. *(Fallback if `Fable` quota is tight at `Loop 3`: `Window A` builds `Loop 3`, and `Codex` + blind `Carbon` attack it from the static brief.)*
  4. *High-volume mechanical periphery (`Loop 1` pre-cut & `Loop 4` 23k proof/caller migration):* `Window A` fans out fast parallel subagents, gated by **both** frozen `ALLOWLIST.md` (zero unlisted sentence edits without stopping for Sid) **and** 100% refusal-keyword hit parity across `act.clj`, `declaration.clj`, and `decision.clj`.
- **`H2` — Strict Uncontamination:**
  - `Window B` (Judge) and `Window C` (Breakers) **never** read `PLAN.md`, `HANDOFF-implementer.md`, `/mnt/data/projects/research/softland/permissions-loop0-probe/`, or builder chat. `Window B` derives behavioral rules solely from `PERMISSIONS.md` §6 + `BUILD.md`, uses `CONTRACT.md` for stored shapes only, and is **sole owner** of `src/proposal/runtime-2026-11-21/gate-histories/` start-to-finish. Prompts to other windows are neutral file-path-only briefs.
- **`H3` — Defaults, Not Conflations — and Zero Silent Caches:**
  - Split stored fields/IDs iff lifecycles or cardinalities differ (keep party birth-kind, `Auth`'s `Caller.root` maker, and per-permission `:lent-from` apart per Items 8/11/19; keep `:sitting` apart from `:covers` until `F28`; separate author party from collective holder `:anyone`/`:anyone-signed-in`). Live-read snapshot design rests on measured Loop 0a probe numbers with **zero stored `:ancestors` cache**.
- **`H4` — Single Quarterback UX:**
  - `Window A` keeps `PLAN.md`, `state/loop-*.md`, and `./scripts/fire` aligned with reality. Every stop for Sid or another window ends with one **`WHERE WE STAND & NEXT UP`** block: (a) what the run proved + any plan pushback, (b) what runs next (including parallel tracks), and (c) either Sid's exact call or the neutral file-only prompt to paste into the other window. When another window finishes, Sid types `"continue"` here.

### 1.2 Roster & File Wall

| Role | May Read | Must NEVER Read | Owns / Writes |
|---|---|---|---|
| **`Window A` (Quarterback & Builder)** | Everything (`PLAN.md`, `HANDOFF-implementer.md`, `PERMISSIONS.md`, `BUILD.md`, `LOOP0-EMPIRICAL-FACTS.md`, `CONTRACT.md`, `ALLOWLIST.md`, `window-a-contract-draft.md`, Judge test output) | — | `./scripts/fire`, `PLAN.md`, `FIRE.edn`, `LOOP0-EMPIRICAL-FACTS.md`, `state/loop-{0a,0c,1,2b,4}.md`, `src-store/` (pure in `2B`), `test-store/` (except `gate-histories/`), 6 caller files |
| **`Window B` (`Fable max` / `xhigh` — Unanchored Contract & Blind Judge)** | `PERMISSIONS.md`, `BUILD.md`, `LOOP0-EMPIRICAL-FACTS.md`, `/mnt/data/projects/research/softland/permissions-test-store-scan/check-sentences.txt`, `CONTRACT.md`, `ALLOWLIST.md`, `src-store/` (read-only), `gate-histories/` | **`PLAN.md`, `HANDOFF-implementer.md`, `permissions-loop0-probe/`**, Builder chat | `CONTRACT.md`, `ALLOWLIST.md`, `state/loop-0b.md`, `src/proposal/runtime-2026-11-21/gate-histories/` (sole owner), `state/loop-{2a,5}.md` |
| **`Window C` (Blind Cluster Breakers)** | `PERMISSIONS.md`, `BUILD.md`, `CONTRACT.md`, `ALLOWLIST.md`, `rama-pitfalls`, `git diff`, live code | **`PLAN.md`, `HANDOFF-implementer.md`, `permissions-loop0-probe/`**, Builder chat | Breaker probes, `state/loop-5-break.md` |

### 1.3 Built-In Overview (`./scripts/fire` + `state/`)

1. **Per-step receipt files** under `src/proposal/permissions-2026-11-27/state/loop-<id>.md` (`0a`, `0b`, `0c`, `1`, `2a`, `2b`, `3`, `4`, `5`), each written only by its owning window (`status`, `commit`, `verified`, `next`).
2. **Per-proposal `FIRE.edn`** (`permissions-2026-11-27/FIRE.edn`, `runtime-2026-11-21/FIRE.edn`; reads `design-pitfalls-review-2026-11-26/` without staging untracked files owned by other sessions).
3. **`./scripts/fire`**: read-only 1-screen dashboard showing every active track, per-loop receipts, overlap-free vs. held-overlap tracks, and the single `NEXT:` action.

---

## 2. Codebase Rings & Refined Stage 1–5 DAG

```text
CODEBASE ANATOMY:
┌──────────────────────────────────────────────────────────────────────┐
│ RING 3: PERIPHERY (~23,000 lines — minutes, serialized cluster.lock) │
│   7 proof files in test-store/ + run_again_fixture + 6 caller sites  │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ RING 2: THIN RAMA & LIVE-READ SHELL (~1,500 lines)             │  │
│  │   tables.clj, walk.clj, 2 gate topologies, read_path.clj       │  │
│  │  ┌──────────────────────────────────────────────────────────┐  │  │
│  │  │ RING 1: PURE KERNEL (~1,500 lines — runs in 3–19 sec)    │  │  │
│  │  │   act, declaration, rows, chain, first_act, decision,    │  │  │
│  │  │   read_path/{query,lanes,sources,answer,acts}            │  │  │
│  │  └──────────────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘

EXECUTION DAG:
  ┌────────────────────────────────────────────────────────────────────┐
  │ STAGE 1 (LOOP 0a -> 0b -> 0c): Unanchored 2-Family Contract Gate   │
  │  • 0a (Window A — DONE): ./scripts/fire + Rama 1.6.0 IPC probe     │
  │    (0 flickers streaming + :base via home; 206 proxies on 450      │
  │    viewers) + 2,081-sentence scan -> LOOP0-EMPIRICAL-FACTS.md      │
  │    (neutral) + frozen private window-a-contract-draft.md.          │
  │  • 0b (Window B: Fable max): Derives 1-page CONTRACT.md (shapes    │
  │    ONLY, SID'S CALLS vs DEFAULTS) + ALLOWLIST.md unanchored.       │
  │  • 0c (Window A -> Sid): Adversarial break + blind 2-family diff   │
  │    against window-a-contract-draft.md -> Sid picks ID & says "go". │
  └─────────────────────────────────┬──────────────────────────────────┘
                                    │
         ┌──────────────────────────┴──────────────────────────┐
         ▼ (Window A + fast Flash subagents)                   ▼ (Window B: Fable xhigh)
┌────────────────────────────────────┐             ┌──────────────────────────────────────┐
│ LOOP 1: Party + Permission Pre-Cut │             │ LOOP 2A: Blind Judge & Simulator     │
│ Trial on pure_proof.clj wrapping   │             │ Rules from BUILD.md + PERMISSIONS §6 │
│ BOTH party IDs & permissions in    │             │ (CONTRACT.md for shapes ONLY).       │
│ old form; fan out 6 proofs +       │             │ Sole owner of gate-histories/ +      │
│ 6 callers. src-store untouched;    │             │ mid-transfer non-flicker player.     │
│ all 23k proofs 100% green.         │             │ Matches today minus ALLOWLIST;       │
└────────────────┬───────────────────┘             │ kills mutants; Sid reads sentences.  │
                 │                                 └──────────────────┬───────────────────┘
                 └──────────────────────────┬─────────────────────────┘
                                            ▼
                   ┌──────────────────────────────────────────────────┐
                   │           LOOP 2B: Full Pure Kernel              │
                   │  Window A writes all Batch 1-4 pure rules ONCE   │
                   │  in Ring 1 (never edits Window B's Judge).       │
                   │  Judged by: Window B's locked Loop 2A (<20s).    │
                   └────────────────────────┬─────────────────────────┘
                                            ▼
                   ┌──────────────────────────────────────────────────┐
                   │     LOOP 3: Ring 2 Rama & Live-Read Wiring       │
                   │  Fresh Fable xhigh (max on read_path.clj)        │
                   │  [or Window A if Fable quota is tight].          │
                   │  Wires tables, walk, 2 gates & snapshot live-read│
                   │  ONCE using Loop 0a's proven mechanism.          │
                   └────────────────────────┬─────────────────────────┘
                                            ▼
                   ┌──────────────────────────────────────────────────┐
                   │   LOOP 4 & 5: Ring 3 Migration + Cluster Break   │
                   │  • Window A flips helper (today's rules as       │
                   │    values): ONLY frozen ALLOWLIST sentences may  │
                   │    change + 100% refusal-keyword hit parity;     │
                   │    adds BUILD.md proofs; updates 6 callers,      │
                   │    measures & fixture (NEVER gate-histories/).   │
                   │  • Window C (Blind Neon + Carbon on cluster)     │
                   │    attacks Loop 3 & full branch.                 │
                   │  • Window B (Fable): cold read of git diff vs    │
                   │    PERMISSIONS §6 + drafts Decisions entry.      │
                   └──────────────────────────────────────────────────┘
```

---

## 3. Deep Technical Inventory & Loop 0a Empirical Receipts

### 3.1 Seam #1 — Live-Read Non-Flicker Transfers, `acts.clj` Routing & Measured Proxy Counts

- **Code receipts:** `read_path.clj:718-886`, `read_path/sources.clj:41-46, 118-190`, `read_path/lanes.clj:25-150`, `read_path/acts.clj:12-16, 92-133`, `read_path/query.clj:89`, `record/rows.clj:504-508`, `test-store/softland/store/read_path_pure_proof.clj:39-41`.
- **Loop 0a live Rama `1.6.0` IPC probe receipts (`/mnt/data/projects/research/softland/permissions-loop0-probe/`, `6.94s`, `4` tasks):**
  1. **Multi-proxy callback tearing reproduced:** Evaluating standing directly from `@push` across independent proxies tore on a 1-act public$\to$member swap when the end callback arrived `40ms` before the grant callback (`[:yes :may-not-see :may-not-see :yes]`).
  2. **2-round `foreign-select` skew reproduced:** Committing a 1-act permission swap between Round 1 and Round 2 produced `:may-not-see` while a 1-turn task query returned `:yes`.
  3. **1-turn single-task `<<query-topology` + single-flight `foreign-invoke-query-async` proven:**
     - `:streaming` (`*routing-id = lane`): `30` consecutive 1-act swaps with `40ms` delayed grant callbacks $\implies$ **`0` flickers** (`34` snapshots), zero callback-thread blocking, and live in-place lender lowering/raising immediately capped/uncapped borrowers with **zero stored `:ancestors` cache**.
     - `:microbatch` `:base` via `home` on non-deciding task (`:base` on `task=2`, `:home-0` on `task=3`): copying both `$$microbatch-facts` and the holder-index PState in the same `(|all)` batch $\implies$ **`0` proxy flickers** (`11` snapshots) and **`0` poll flickers** (`293` concurrent `2ms` polls).
  4. **Measured proxy counts (omitting `:read-after` on shared triggers):** `200`-viewer public lane = **`3` proxies total** (`2` facts + `1` holder index); `200`-agent + `250`-sitting-bio members lane = **`206` proxies total** (`3` facts + `203` holder index), well below `900` (`query.clj:89`).
  5. **Code-scan addition (`read_path/acts.clj:12-16, 92-133`):** `read-acts` also checks standing (`holder` / `visitor` / `outside`) and routes `:base` reads by **`:base` itself** (the deciding task, where `$$microbatch-line`/`things`/`task` sit) rather than `home`. Passing `*routing-id = lane` for `acts.clj` and `*routing-id = home` (on `:base`) / `lane` (on `:streaming`) for fact reads lets one `<<query-topology` serve both cleanly.

### 3.2 Seam #2 — Party Kind, Author-vs-Holder Split, `Item 8 / 11 / 19` Separation & Bootstrap Determinism

- **Code receipts:** `act.clj:57-106, 163, 222`, `declaration.clj:110-120`, `gate/first_act.clj:5-7`, `auth/schema.clj:84-85, 117, 303-334`, `runtime/seam.clj:39-40, 124-167, 611-735`, `runtime/sign_in.clj:89-107, 153-155, 214-288`, `command/main.clj:306-316`, `server/sign_in.clj:164-169`, `wire/words.clj:157-160`.
- **Checks enforced in Stage 1c against `CONTRACT.md`:**
  1. **No untagged-ID-defaults-to-person hole:** Every valid party ID in `:who` / `:holder` must carry its party kind (`person` / `agent` / `run` / `:system` / `:anyone` / `:anyone-signed-in`), e.g. `(a1)` space-free party keywords `:person.<uuid>` / `:agent.<uuid>` / `:run.<uuid>` or `(a2)` kind-tagged `UUIDv8`.
  2. **Author vs. collective holder split (Loop 0a scan finding):** `:anyone` and `:anyone-signed-in` must be valid in `:holder` only, **never** in `:who` on `:act` — otherwise a direct depot caller could send `{:who :anyone :permission nil :facts [{:e pub-id :k :softland.store/permission :v nil}]}` to close a public lane under own-end (`PERMISSIONS.md:985-986`).
  3. **`Item 8 / 11 / 19` non-conflation:** Party birth-kind (in store ID), agent maker (`Caller.root` in `Auth`/`seam`, never in store ID per 9 Oct Entry 1), and whom an agent/run acts for on a step (`:lent-from` per permission) stay strictly apart.
  4. **Bootstrap initial-permission determinism:** Initial lane permission UUID derived deterministically from `lane` alone (never from `person`), keeping `sign_in.clj:89-107` `enter!` crash-retries byte-identical with zero `Auth` schema edits.
  5. **`Loop 1` pre-cut scope adjustment (Loop 0a scan finding):** Across `test-store/`, bare `:alice`, `:bob`, and `(uuid n)` appear directly in `:who` and `:asker` (plus `act_proof.clj:250,464` and `pure_proof.clj:915` test `:who` UUID/257-char-id cases). `Loop 1`'s pre-cut helper must wrap **both** test party literals (`person`/`agent`/`run`) **and** permission construction so `Loop 4` flips both cleanly.

### 3.3 Seam #3 — Gate Intra-Act Overlay, Bounded `:lent-from` Walk & Subindexed Owner Set

- **Code receipts:** `gate/decision.clj:189-416`, `gate/chain.clj:69-85`, `module/walk.clj:68-95`, `record/tables.clj:165-167`, `BUILD.md:129-150`.
- **Three invariants to hold in `Loop 2B` / `Loop 3` and verify in Stage 1c:**
  1. **Effective rights intersection (`∩`)** across `:lent-from` (lowering a lender caps borrowers in place; raising uncaps them).
  2. **Explicit `:lent-from` chain depth + cycle guard (Loop 0a scan finding):** Today `walk/decide` terminates in $\le 31$ ancestor reads only because nested `:permission-id` vectors hit `act/max-depth = 32`. With flat UUID pointers, both `walk/decide` and the single-task rights query must cap `:lent-from` depth (e.g. `32` + cycle guard) so neither non-yielding topology can stall a single-threaded Rama task.
  3. **Sequential intra-act overlay ($S_{\text{pre}} \to S_{\text{post}}$) + subindexed retry-idempotent owner set in `$$task`:** Whole-act checks verify no permission changed twice, no permission created/changed in the act left `:lent-from` one ended in the same act (`BUILD.md:131`), and $\ge 1$ standing non-lent owner remains via a subindexed set (not a counter) in `$$task` on the deciding task (zero `|all` copy).

---

## 4. Loop-by-Loop Playbook & Neutral File-Only Starters

> **Commit & cluster rule:** Run `git log --oneline -3` and `git status -sb` first; stage **only** owned paths by name (never `git add -A`); include zero attribution lines; update `state/loop-<id>.md` in the same commit; always serialize cluster runs behind `/mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock`.

### Stage 1 (`Loop 0a -> 0b -> 0c`) — Probe, Unanchored `CONTRACT.md` + `ALLOWLIST.md` & 2-Family Break
- **`Loop 0a` (`Window A`) — DONE:** Built `./scripts/fire` + `FIRE.edn` + `state/`; ran Rama `1.6.0` IPC probe; tokenized `2,081` `test-store/` sentences; wrote neutral `LOOP0-EMPIRICAL-FACTS.md` and frozen private `window-a-contract-draft.md`.
- **`Loop 0b` (`Window B`: `Fable max` in Claude Code) — Neutral starter to paste:**
  > `Read .claude/skills/design-pitfalls/SKILL.md, .agents/skills/rama-pitfalls/SKILL.md, src/proposal/permissions-2026-11-27/PERMISSIONS.md (§6 and §12), src/proposal/permissions-2026-11-27/BUILD.md, and src/proposal/permissions-2026-11-27/LOOP0-EMPIRICAL-FACTS.md (plus /mnt/data/projects/research/softland/permissions-test-store-scan/check-sentences.txt and read-only source files under src-store/, src-runtime/, src-auth/, src-command/, src-server/, src-wire/, and test-store/ as needed; do NOT read PLAN.md or HANDOFF-implementer.md). Touch zero files in src-store/ or test-store/. (1) Derive and write a 1-page src/proposal/permissions-2026-11-27/CONTRACT.md containing concrete EDN/Rama data shapes, PState schemas, and query signatures ONLY — zero retelling of behavioral gate/read rules — strictly separating SID'S EXPLICIT CALLS (with concrete trade-offs where Sid must choose, including store Party ID representation while keeping party kind, Auth maker custody, and per-permission :lent-from apart per Items 8/11/19, and :sitting apart from :covers) from ENGINEERING DEFAULTS. (2) Derive and write src/proposal/permissions-2026-11-27/ALLOWLIST.md listing every test-store/softland/store/*proof.clj check sentence (file:line + sentence + reason) that BUILD.md Batches 1–4 or CONTRACT.md intentionally changes. (3) Audit CONTRACT.md against design-pitfalls and rama-pitfalls, update src/proposal/permissions-2026-11-27/state/loop-0b.md (status: DONE), commit ONLY CONTRACT.md, ALLOWLIST.md, and state/loop-0b.md with no attribution lines, and stop.`
- **`Loop 0c` (`Window A` $\to$ `Sid`):** Sid types `"continue"` in `Window A`. `Window A` runs `design-pitfalls` + `rama-pitfalls` + its 10-point breaker checklist and diffs `Fable`'s `CONTRACT.md` + `ALLOWLIST.md` against `window-a-contract-draft.md`, updates `state/loop-0c.md`, and presents any real breaks + the exact Party ID / contract call to `Sid`. `Sid`'s `"go"` freezes `CONTRACT.md` and `ALLOWLIST.md`.

### Stage 2 (`Loop 1` || `Loop 2A` — Run in Parallel, Zero Shared Files)
- **`Loop 1` (`Window A` + fast `Flash` subagents):**
  - Trial extracting both the party-ID helper and permission-builder helper on `test-store/softland/store/pure_proof.clj` in today's form; once green, fan out subagents across the remaining 6 `test-store/` proof files + 6 caller sites. Touch zero files in `src-store/` or `gate-histories/`.
  - *Exit gate:* `git diff src-store/` empty; all 7 store proofs + `test-runtime` 100% green behind `cluster.lock`; commit + write `state/loop-1.md`.
- **`Loop 2A` (`Window B`: `Fable xhigh` — Neutral starter to paste once `CONTRACT.md` is frozen):**
  > `Read src/proposal/permissions-2026-11-27/PERMISSIONS.md (§6), src/proposal/permissions-2026-11-27/BUILD.md, src/proposal/permissions-2026-11-27/CONTRACT.md (for stored shapes only), src/proposal/permissions-2026-11-27/ALLOWLIST.md, src/proposal/runtime-2026-11-21/gate-histories/README.md, and test-store/softland/store/read_path_pure_proof.clj:1-60,2150-2200 (do NOT read PLAN.md or HANDOFF-implementer.md). Upgrade src/proposal/runtime-2026-11-21/gate-histories/ (which you own solely) to test all rules of BUILD.md Batches 1–4 + the statement-to-prove + broken-copy mutants, and add a mid-transfer non-flicker check across callback permutations for single-act multi-row changes. Verify that on today's default values your oracle agrees with untouched src-store/ everywhere outside ALLOWLIST.md and fails red on mutants. Print your plain-English rule sentences for Sid, commit owned gate-histories/ files + src/proposal/permissions-2026-11-27/state/loop-2a.md with no attribution lines, and stop.`

### Stage 3 (`Loop 2B`) — Full Pure Kernel Written Once (`Window A`)
- Write all Batch 1–4 pure rules **once** across Ring 1 (`act.clj`, `door/{declaration,digest,text,additions,words}.clj`, `record/rows.clj`, `gate/{chain,first_act,decision}.clj`, `read_path/{query,lanes,sources,answer,acts,words}.clj`). Never edit `gate-histories/` or `ALLOWLIST.md`.
- *Exit gate:* `Window B`'s locked `Loop 2A` pure histories + mutants + mid-transfer non-flicker player pass 100% in `<20s`; commit + write `state/loop-2b.md`.

### Stage 4 (`Loop 3`) — Ring 2 Rama & Live-Read Wiring (`Fresh Fable xhigh, max on read_path.clj` — or `Window A`)
- **Neutral starter for fresh `Fable`:**
  > `Read .agents/skills/rama/SKILL.md, .agents/skills/rama-pitfalls/SKILL.md, src-store/CLAUDE.md, src/proposal/permissions-2026-11-27/CONTRACT.md, src/proposal/permissions-2026-11-27/BUILD.md, src/proposal/permissions-2026-11-27/LOOP0-EMPIRICAL-FACTS.md, and the updated pure namespaces under src-store/softland/store/ (do NOT read PLAN.md or HANDOFF-implementer.md). Wire record/tables.clj, module/{walk,streaming_gate,microbatch_gate,door_reads}.clj, module.clj, door.clj, and read_path.clj per CONTRACT.md. Verify against src/proposal/runtime-2026-11-21/gate-histories/ (read-only) and live-transfer + microbatch :base copy checks behind /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock, commit owned files + src/proposal/permissions-2026-11-27/state/loop-3.md with no attribution lines, and stop.`

### Stage 5 (`Loop 4` & `Loop 5`) — Periphery Migration + Blind Cluster Break + Final Diff Read
- **`Loop 4` (`Window A` + fast subagents):** Record baseline refusal-keyword hit counts; flip `Loop 1` party + permission helpers (encoding old permissions with today's rules as values); rewrite direct-row cases (`pure_proof.clj:1624-1782`); add proofs for every new line of `BUILD.md`; update 6 caller sites; run `read_measures` & `gate_measures`; regenerate `run_again_fixture` after cluster replay passes. Never edit `gate-histories/` or `ALLOWLIST.md` (stop for Sid if any unlisted sentence fails). Verify 100% green behind `cluster.lock` + 100% refusal-keyword hit parity; commit + write `state/loop-4.md`.
- **`Loop 5` (`Window C` Blind Cluster Breakers $\to$ `Window B` Final Read $\to$ `Sid`):**
  - *Neutral static brief for `Window C` (`Fresh Neon XHigh` + `Carbon High` [or `Codex`]):*
    > `Read .agents/skills/rama-pitfalls/SKILL.md, src/proposal/permissions-2026-11-27/PERMISSIONS.md (§6), src/proposal/permissions-2026-11-27/BUILD.md, src/proposal/permissions-2026-11-27/CONTRACT.md, src/proposal/permissions-2026-11-27/ALLOWLIST.md, and git diff against baa19ffe (do NOT read PLAN.md or HANDOFF-implementer.md). Attack the store, read_path, and runtime callers on an InProcessCluster behind /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock: test mid-transfer callback races, microbatch :base copy skew, act-line standing on :base, deep/cyclic :lent-from chains, collective-holder author spoofing, duplicate/conflict replay, poison depot records, proxy/weight bounds, refusal-keyword coverage, and regenerated tables.edn validity. Write reproducible breaks with file:line and probe receipts to src/proposal/permissions-2026-11-27/state/loop-5-break.md.`
  - *Final read in `Window B` (`Fable`):* Replays `gate-histories/cluster.clj`, reads `git diff` cold against `PERMISSIONS.md §6`, and drafts the dated `src-store/README.md` Decisions entry in `state/loop-5.md` for Sid's approval.

---

## 5. Deferred Post-Build Skill Follow-Ups (Held for Sid's Word)

1. **`.agents/skills/rama-pitfalls/SKILL.md` §8 (`PROXYSTATE PATH SHAPE`):** Add measured Loop 0a receipt on multi-proxy callback tearing across single-task events (`40ms` callback skew $\implies$ `:may-not-see`) and 2-round `foreign-select` skew vs. 1-turn `<<query-topology`.
2. **`.claude/skills/design-pitfalls/SKILL.md` Question #11:** Temporal conflation across plan batches (earlier batch encoding a rule/table that depends on a default staying narrow when a later batch on the same page widens it).
