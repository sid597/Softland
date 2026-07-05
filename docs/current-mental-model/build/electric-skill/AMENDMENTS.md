# Electric-skill Stage 3 — AMENDMENTS (ready-to-apply old→new pairs)

Date: 2026-07-05. Basis: `VERDICTS.md` (this directory) + regression suite
`test/app/missionary_claims_test.clj`. Every entry below is a doc still
carrying a **falsified or mislocated** claim per VERDICTS provenance, with
the exact current text (OLD) and the correction (NEW). Nothing in this file
has been applied by the Stage-3 session except the two skill files it owns
(see "Already applied" at the bottom).

**Tiers**
- **SID-COUNTERSIGN** — anything in `CLAUDE.md` (binding project
  instructions; Sid signs off before any edit).
- **HQ-APPLY** — repo docs, memory files, and code-comment fixes a future
  authorized session applies without countersign. Notes:
  - `implementation-quirks.md` / `core-reframes.md` live under
    `~/.claude/projects/-mnt-data-projects-Softland/memory/` — the Stage-3
    session was barred from editing that directory; the applying session is not.
  - `.cljs` entries are comment-only but are CODE files: apply them in a
    **code commit on a code branch**, never mixed with .md changes
    (repo commit policy).

---

## Tier 1 — SID-COUNTERSIGN (CLAUDE.md, 3 amendments / 5 line pairs)

> **APPLIED 2026-07-05** by Fable under Sid's in-session extension of the
> time-box blanket ("make the call just note it down somewhere" — chat,
> verbatim). Reversible by reverting the docs commit; Sid may reverse on
> review. Tier 2 (H1–H13) remains PENDING: doc/memory items apply next
> session; H11/H12 are code files and ride code commits.

### A1 — Re-scope the m/ap ban (VERDICTS Claim 1; test `claim-01-nested-ap-forks-crash-watch-cancelled`)

The crash trigger is **multiple `m/?<` nested in ONE `m/ap` over watches** —
it fires when consumed directly; "fed to m/latest" is not load-bearing, and
two separate single-`m/?<` `m/ap`s under `m/latest` do NOT crash.

**A1a — CLAUDE.md:31 (section heading)**

OLD:
```
### ⚠️ NEVER use m/ap with multiple m/?< inside flows fed to m/latest
```
NEW:
```
### ⚠️ NEVER nest multiple m/?< over m/watch inside a single m/ap
```

**A1b — CLAUDE.md:51 (the Why line)**

OLD:
```
**Why:** `m/ap` + `m/?<` cancels old branches on input change. `m/latest` propagates cancellations, killing the entire flow graph.
```
NEW:
```
**Why:** the outer `m/?<` fork's restart cancels the still-live NESTED `(m/watch ...)` fork, and cancelling a live `m/watch` crashes with "Watch cancelled" — even when the `m/ap` is consumed directly, with no `m/latest` involved. Two SEPARATE single-`m/?<` `m/ap`s under `m/latest` do not crash; `m/latest` over raw watches stays the sanctioned combiner. (Verified: `claim-01-nested-ap-forks-crash-watch-cancelled`, build -45.)
```

### A2 — Scope the "atomic settle" / paradigm claims (VERDICTS Claim 17)

Raw `m/latest` diamonds glitch deterministically (4/4 probe runs); there is
no atomic settle at the Missionary layer (Electric's compiled DAG untested).
The Van Roy attribution is a framing, not a sourced Getz quote.

**A2a — CLAUDE.md:21 (paradigm intro)**

OLD:
```
Electric/Missionary is **continuous synchronous programming** on Van Roy's taxonomy (not FRP). This classification comes from Dustin Getz (Electric's creator). Key distinctions:
```
NEW:
```
Electric/Missionary is **continuous synchronous programming** (not FRP). The continuous/synchronous/demand-driven framing is Dustin Getz's own (Electric's creator); the specific "Van Roy taxonomy" mapping is a defensible framing, not a sourced Getz quote. Key distinctions:
```

**A2b — CLAUDE.md:23 (the Synchronous bullet)**

OLD:
```
- **Synchronous**: When an atom changes, `m/latest` recomputes and the entire graph settles atomically before the next change is observed (the synchronous hypothesis).
```
NEW:
```
- **Synchronous (scoped)**: When an atom changes, `m/latest` recomputes with the new value — but raw `m/latest` DIAMONDS GLITCH: two flows derived from one source, combined downstream, deterministically emit inconsistent intermediate pairs before settling. There is no atomic settle at the Missionary layer (Electric's compiled DAG is untested on this). Co-varying values must be derived in a SINGLE `m/latest` over the shared source, never two `m/latest`s combined. (VERDICTS.md Claim 17.)
```

### A3 — RAF rule sunset note (VERDICTS Claim 3; PROBE-10K §6; PROBE-EVIDENCE §3)

**A3 — CLAUDE.md:99 (the Rule line of the RAF pattern)**

OLD:
```
**Rule:** Dirty-present (conditional RAF) requires Electric diffs as the change signal (Gap 3 of the differential migration). Until then, unconditional RAF + `identical?` skip is the correct pattern. Cost: one pointer compare per 16ms.
```
NEW:
```
**Rule:** Dirty-present (conditional RAF) requires Electric diffs as the change signal (Gap 3 of the differential migration). Measured 2026-07-05 (PROBE-10K §6): that signal is now viable at 10⁴ for change/append/tail-shrink diff shapes (mint 2–5ms server-side, apply ≤4.2ms p95, 60fps) — reorder-shaped diffs (large `:permutation`) must NEVER be the change-signal carrier (producer knees at 10³). Until the diff signal is actually wired, unconditional RAF + `identical?` skip remains the correct pattern. Cost: one pointer compare per 16ms.
```

### A4 — Slim CLAUDE.md's Missionary/Electric section to pointers (voiced by Sid at session close 2026-07-05: "i thought it is more about anything that is useful for every session")

CLAUDE.md loads into EVERY session; its ~80-line pattern blocks are now
redundant with the VERIFIED skill (`.claude/skills/electric-docs/SKILL.md`,
regression-tested). Replace the three pattern blocks (m/ap ban · eduction
recipe · RAF rule, including all code examples) with a pointer section:

NEW (whole-section replacement):
```
## Critical Missionary/Electric Patterns — see the electric-docs skill
Verified laws + recipes: `.claude/skills/electric-docs/SKILL.md`
(regression-tested against the pinned build; re-run
test/app/missionary_claims_test.clj after any Electric SNAPSHOT bump).
Non-negotiables, one line each:
- NEVER nest multiple `m/?<` over `m/watch` inside a single `m/ap` (L1).
- Event filtering: `m/eduction` + `@deref`, never `m/ap` forks over watches (R2).
- No side effects in `m/latest`; unconditional RAF + `identical?` skip until
  the Gap-3 diff signal is wired — reorder diffs never the carrier (R3).
- `try` inside `e/defn` throws "try is TODO" (L13).
- Co-varying values from ONE `m/latest` — raw diamonds glitch (L8).
```
Apply AFTER Sid glances at the final wording (it deletes binding text he may
want to keep verbatim).

---

## Tier 2 — HQ-APPLY (13 amendments)

### H1 — `docs/history/insights.md:11-19` — m/observe/m/ap are re-subscribable (VERDICTS Claim 10, AGENT-ERROR)

**H1a — the code-comment line (insights.md:12)**

OLD:
```
;; BAD: Top-level def = shared, single-use
```
NEW:
```
;; Works (re-subscribable), but shares nothing useful; factory preferred
```

**H1b — the Rule line (insights.md:19)**

OLD:
```
**Rule:** `m/observe` and `m/ap` create flows that can only be subscribed to **once**. Each subscription needs its own fresh flow instance. Use factory functions for flows you'll subscribe to multiple times.
```
NEW:
```
**Rule (corrected 2026-07-05, VERDICTS.md Claim 10):** `m/observe` and `m/ap` flows ARE re-subscribable — each subscription independently re-runs the observe setup fn, and all subscribers receive values. The genuine single-subscription/shared-process property belongs to `m/signal` / `m/stream` (memoized, multicast). Factory functions remain good practice, but their value is isolating per-subscription mutable state, not avoiding a subscription failure.
```

### H2 — `docs/history/insights.md:133-134` — philosophy section advocates the banned pattern (VERDICTS Claim 1; HACKS-LEDGER Surprise 1)

OLD:
```
2. **Derived values are flows, not atoms**
   - `m/ap` + `m/?<` declares dependencies
```
NEW:
```
2. **Derived values are flows, not atoms**
   - `m/latest` over `m/watch` declares dependencies (never multiple `m/?<` nested in one `m/ap` — that crashes "Watch cancelled"; see CLAUDE.md ban)
```

### H3 — `docs/history/insights.md:224-261` — Session-10 writeup mislocates the mechanism (VERDICTS Claim 1)

**H3a — insights.md:228 (the Problem statement)**

OLD:
```
The error `Reactor failure: missionary.Cancelled {message: 'Watch cancelled.'}` happens when using `m/ap` with multiple `m/?<` forks inside flows that feed into `m/latest`.
```
NEW:
```
The error `Reactor failure: missionary.Cancelled {message: 'Watch cancelled.'}` happens when multiple `m/?<` forks over `m/watch` are nested inside ONE `m/ap`. It crashes even when that flow is consumed directly — feeding it to `m/latest` is NOT required (verified 2026-07-05, VERDICTS.md Claim 1 shape A).
```

**H3b — insights.md:246 (falsified "fine standalone" item)**

OLD:
```
4. This is fine for standalone flows...
```
NEW:
```
4. The outer fork's restart cancels the still-live NESTED `(m/watch ...)` fork — cancelling a live `m/watch` throws "Watch cancelled". This crashes even standalone (consumed directly, no `m/latest` — verified on build -45).
```

**H3c — insights.md:248 (the misleading pivot heading)**

OLD:
```
**BUT when combined with m/latest:**
```
NEW:
```
**m/latest is NOT the trigger (corrected 2026-07-05) — it only spreads the failure:**
```

**H3d — insights.md:261 (the mislocated causal conclusion)**

OLD:
```
So when `<derived-flow-using-m/ap` cancels during a fork, `m/latest` sees the cancellation and propagates it, killing the entire flow graph!
```
NEW:
```
Propagation through `m/latest` spreads the failure wider, but it is not the cause: the same `m/ap` crashes consumed directly, and two SEPARATE single-`m/?<` `m/ap`s fed to `m/latest` do NOT crash (VERDICTS.md Claim 1, shapes A and B2).
```

### H4 — `docs/history/insights.md:345-346` — "shared flow" misdiagnosis (VERDICTS Claim 10)

OLD:
```
### Text appears then disappears
- Shared flow got cancelled (use factory functions)
```
NEW:
```
### Text appears then disappears
- A consumer's flow got cancelled upstream (check `m/join` siblings — one dying branch cancels all). NOT an `m/observe` re-subscription limit: m/observe/m/ap are re-subscribable (VERDICTS.md Claim 10)
```

### H5 — `docs/history/insights.md:442` — Session-12 lesson repeats the mislocation (VERDICTS Claim 1)

OLD:
```
3. **Critical Missionary lesson**: `m/ap` + `m/?<` forks cancel old branches, which kills `m/latest` consumers. Use `m/latest` for combining continuous values, `m/eduction` + `deref` for filtering discrete events.
```
NEW:
```
3. **Critical Missionary lesson**: multiple `m/?<` forks nested in one `m/ap` over watches crash ("Watch cancelled") on input change — even consumed directly; `m/latest` is not the trigger. Use `m/latest` for combining continuous values, `m/eduction` + `deref` for filtering discrete events.
```

### H6 — `docs/history/progressive-summary.md:43` — incseq/mount misname + missing corruption warning (VERDICTS Claim 15)

OLD (sentence within the Phase 6D bullet):
```
`gpu-mount` returns 5 callbacks matching Electric's `incseq/mount` contract (`append-child`, `replace-child`, `insert-before`, `remove-child`, `nth-child`) with internal `!children` ordering vector.
```
NEW:
```
`gpu-mount` returns 5 callbacks for Electric's `hyperfiddle.incseq.mount-impl/mount` contract (positional 5-arg; there is no public `incseq/mount` var) with internal `!children` ordering vector — but note (2026-07-05, VERDICTS.md Claim 15): the contract's `insert-before` means MOVE an existing child, so gpu-mount's allocate-on-insert corrupts the pool on any `:permutation` (PROBE-10K §4: 16,750 slots for 100 entities). Never wired; do not wire as-is.
```

### H7 — `docs/history/progressive-summary.md:367` — Lesson 1 pattern line (VERDICTS Claim 1)

OLD:
```
**Pattern:** `m/ap` + multiple `m/?<` inside flows fed to `m/latest`
```
NEW:
```
**Pattern:** multiple `m/?<` over `m/watch` nested inside a single `m/ap` (crashes even consumed directly; `m/latest` is not the trigger — VERDICTS.md Claim 1)
```

### H8 — `memory/implementation-quirks.md:12` — "silent corruption" falsified (VERDICTS Claim 6; test `claim-06-latest-arity-mismatch-throws-arityexception`)

OLD:
```
- **m/latest arg counts MUST match fn params** — silent corruption if they don't
```
NEW:
```
- **m/latest arg counts MUST match fn params** — a mismatch throws a LOUD `clojure.lang.ArityException` on first emission (NOT silent corruption; corrected 2026-07-05, VERDICTS.md Claim 6). The long-arity blocks below are safe because arity MATCHES, not because mismatch would be quiet
```

### H9 — `memory/implementation-quirks.md:49-50` — e/watch single-peer quirk retracted (VERDICTS Claim 13, AGENT-ERROR)

OLD:
```
- `e/watch` only works within a single peer — no server→client missionary pipe
- Pattern: HTTP for data transfer, Electric for bootstrap only
```
NEW:
```
- RETRACTED 2026-07-05 (VERDICTS.md Claim 13): `e/watch` = `e/input` over `m/watch` and DOES transfer server→client — `(e/server (e/watch !atom))` is the shipped Rama-truth bridge (file_viewer.cljc:107-190 → electric_flow.cljc:484-489). What cannot cross peers is a RAW Missionary flow; Electric-managed transfer of an e/watch value can and does. The old "HTTP for data transfer, Electric for bootstrap only" pattern is obsolete
```

### H10 — `memory/core-reframes.md:218` — climate line carries the mislocation (VERDICTS Claim 1)

OLD:
```
- Some patterns are unstable under cancellation (m/ap + m/?< in m/latest)
```
NEW:
```
- Some patterns are unstable under cancellation (multiple m/?< nested in one m/ap over watches — crashes even without m/latest)
```

### H11 — `src/app/client/substrate/webgpu/buffer_pool.cljs:419-430` — gpu-mount docstring (VERDICTS Claim 15) — CODE FILE, code commit

OLD (docstring lines within `gpu-mount`):
```
  "Create Electric-compatible mount callbacks for a buffer pool.
   Returns 5 callbacks matching Electric's incseq/mount contract.
   Maintains internal ordering state for position-based child lookup.
   Callbacks handle allocation/deallocation — callers pass item data in,
   get handles out. nth-child returns the handle at position i.

   Usage with Electric:
     (let [{:keys [append-child replace-child insert-before
                   remove-child nth-child]} (gpu-mount pool)]
       (incseq/mount append-child replace-child insert-before
                     remove-child nth-child))"
```
NEW:
```
  "Create mount callbacks shaped for hyperfiddle.incseq.mount-impl/mount
   (a POSITIONAL 5-arg fn; there is NO public incseq/mount var).
   Maintains internal ordering state for position-based child lookup.
   Callbacks handle allocation/deallocation — callers pass item data in,
   get handles out. nth-child returns the handle at position i.

   ⚠️ DO NOT WIRE AS-IS (2026-07-05, VERDICTS.md Claim 15 / PROBE-10K §4):
   the mount contract passes existing CHILD HANDLES back through
   insert-before during :permutation rotations (DOM insertBefore = MOVE);
   this implementation allocates a fresh slot on every insert, so any
   :permutation corrupts the pool (measured: 16,750 active slots for 100
   entities after 91 rotate frames). The scene store must consume the six
   diff ops directly (C2 shape) instead of this DOM-shaped bridge.

   Contract shape, for reference only:
     (let [{:keys [append-child replace-child insert-before
                   remove-child nth-child]} (gpu-mount pool)]
       (hyperfiddle.incseq.mount-impl/mount
        append-child replace-child insert-before remove-child nth-child))"
```

### H12 — `src/app/client/workspace/events.cljs:147` and `:161` — factory docstrings state the falsified reason (VERDICTS Claim 10) — CODE FILE, code commit

OLD (two occurrences, `make-raf-flow` and `make-blink-timer`):
```
   IMPORTANT: Must be called fresh for each subscription, not shared!
```
NEW (both occurrences):
```
   Factory isolates per-subscription mutable state; NOTE m/observe/m/ap
   flows ARE re-subscribable (VERDICTS.md Claim 10) — this is state
   hygiene, not a subscription limit.
```

### H13 — `.claude/skills/reactive_master.md:8-9` — stale reference paths (HACKS-LEDGER Surprise 4; deferred from Stage 3's own edit, which was scoped to the banner + lines 26-27 only)

OLD:
```
- `docs/missionary-complete-reference.txt` (The Flow Bible)
- `docs/electri_tutorial.txt` (The Electric Way)
```
NEW:
```
- `docs/reference/missionary-reference.txt` (The Flow Bible)
- `docs/reference/electric-tutorial.txt` (The Electric Way)
```

---

## Explicitly NO amendment (so nobody "fixes" them)

- **CLAUDE.md:101-104 (try/catch ban)** — CONFIRMED CURRENT on build -45
  (VERDICTS Claim 5; the stale-version suspicion is REFUTED). Stands as written.
- **CLAUDE.md:53-68 (m/eduction recipe)** — verified stronger than stated
  (VERDICTS Claim 2). Stands as written.
- **`memory/implementation-quirks.md:166` (:prod pre-broken)** — confirmed at
  the classpath (VERDICTS Claim 20); already recorded correctly.
- **Claim 14 provenance sites** (`util_fns.cljc` quarantine comments,
  `progressive-summary.md:659-665`, MEMORY.md S40 fork) — UNVERIFIED, not
  falsified; they stay until a Rama-cluster probe settles it either way.

## Already applied by Stage 3 (do not re-apply)

- `.claude/skills/electric-docs/SKILL.md` — full rewrite (version header,
  16 laws with test/verdict citations, 4 verified recipes, UNVERIFIED
  section for claims 4/14/19, stale paths fixed).
- `.claude/skills/reactive_master.md` — dated supersession banner + lines
  26-27 corrected (`m/ap`+`m/?<` derivation advice → `m/latest` over watches).

## Tier counts

| Tier | Amendments | Line pairs |
|---|---:|---:|
| SID-COUNTERSIGN (CLAUDE.md) | 3 (A1-A3) | 5 |
| HQ-APPLY (repo docs, memory files, code comments) | 13 (H1-H13) | 17 |
