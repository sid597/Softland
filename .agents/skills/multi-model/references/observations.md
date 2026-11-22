# Multi-Model Trial Log (Filed by Arrangement, Not Model Roles)

Entries record what arrangement ran, what it caught, what got past it, and what it cost — never permanent roles for any model. Every new entry must also state its confounds explicitly (`harness · role · same-or-different prompt across models`); no entry through 22 Nov 2026 ran under equal conditions across models, so none separates model weights from harness or assigned role. Update **only on Sid's word**, scored from receipts (commits, files, or `softland-memory`).

---

## Trial Log

### 21 Nov 2026 · Store Pieces 1 & 2 · Single-session build (`Fable 5.2, max, Claude Code`) -> blind breaker (`Carbon, High, Antigravity`)
- **caught:** Piece 1 held (`2882b55b`, `8bbac6d0`). On Piece 2 (`34fdab0e`, `3544d026`), blind pure + cluster probes caught 4 live bugs (`1A` re-grant drop on `act-uuid`, `1B` `:see`-holder burning `:act-permission`, `1C` `door` vs. `hex-for-its-permission` mismatch, `2A` `sources.clj:580` `{:anew}` leak); Fable then spotted why tying `:act-permission` to `act-uuid` breaks retry-after-approval.
- **got past:** Author's in-session proofs (`module-proof`, `door-proof`) stayed green on all 4 bugs; running orchestrator + multi-stage builder at `max` in one long chat burned ~95% of weekly Fable quota.
- **cost & receipts:** 1 long build session + 1 overnight breaker · `2882b55b`, `34fdab0e`, `b45b997a` (`REVIEW-store-piece-2.md`), `fc43d248` (`gate-histories/`).

### 21 Nov 2026 · Piece 4 scope & isolation · Cross-family readings against `vision/LOG.md` & stack (`Fable 5.2, max` · `Carbon, High` · `Neon, XHigh`)
- **caught:** Carbon caught that `127.0.0.1` loopback does not isolate local agents under user `sid` (against Fable's loopback proposal) and measured `pstate.proxy.limit` per `(module, PState, task)`; Fable caught missing `:driven-by` person-vs-model field (`12 Oct entry 4`, `37bc0e01`), `Trails` touching `src-wire`, relative `cluster.lock` distinct-inode bug across worktrees, and Carbon cutting Pieces 5 & 8 against recorded decisions.
- **got past:** Pre-Nov-13 `/rama` plan template (`Design difficulty log`) triggered `reasoning_extraction` stops on Opus (upstream fix `968b027` sat on unmerged branch `batch-block-improvements`, not `master` `0b46060`; later patched locally via `scripts/sync-rama-skill.sh`); decomposition template produced an over-sliced 4-stage `Auth` split (`e4d3c56a`); Neon smoothed live cross-model disagreements into "converged" summaries and trusted stale `store.jar` mtime.
- **cost & receipts:** High Sid paste/mediation tax across chat windows · `37bc0e01`, `968b027`, `e4d3c56a`.

### 21–22 Nov 2026 · Store Piece 3 Stage-1 plan · Plan (`Fable 5.2, max`) -> blind Phase-2 falsifier (`Carbon, High`)
- **caught:** Failed `PLAN-gates-keep-the-line.md` (`VERDICT: major-fail`) before `src-store` edits — 32 MiB unpermissioned payload stalling task thread, Java array / `BigDecimal` weight bypasses, double walk, `1 + 2k` hidden seeks.
- **got past:** Author's plan self-check missed the DoS/cost surfaces.
- **cost & receipts:** 2 sessions, 0 wasted module edits · `b45b997a` (`PLAN_VALIDATION-gates-keep-the-line.md`).

### 21–22 Nov 2026 · Auth (`Piece 4`) · 3-way blind split from `SPEC.md` (`Opus 5.5, max` reference · `Carbon, High` blind suite · `Neon, XHigh` unified `PLAN.md`) -> state-space model (`Carbon, High`)
- **caught:** Reference (`20c1efda`, 20/20 broken copies caught) vs. blind suite (`d325e482`) vs. `PLAN.md` (`01c6ea70`, backed by Rama `foreign-proxy` loss probe) exposed 9 spec divergences and 27 edge questions before module code; morning state-space runner (`a36c2cfb`, 51,270 interleavings in 1.6s) broke 4 of 5 invariants in Neon's unchecked multi-hop `PLAN.md` (`11` dual-person/dual-account races, `12` stuck states, `28` spec mismatches, `6` read-after-write misses).
- **got past:** Opus's `reference.clj` held 2 unbounded fields (`:decided` Q7 caught by Fable, `:replaced` Q12 caught by Neon) and ignored `_module-name` in `open` (`7cebbac2`); Carbon's `properties.clj:754` "concurrent" check ran sequentially on 1 thread; Carbon misattributed `01c6ea70`/`20c1efda` without checking logs and wrote 3 contaminated morning prompts (caught by Fable: showing `PLAN.md` to `reference.clj`, modeling `$$answers` in `gate-histories`, letting builder edit `checks.clj`).
- **cost & receipts:** 3 overnight + 3 morning check sessions (too many human-pasted pre-code waves before `src-auth` existed) · `01c6ea70`, `20c1efda`, `d325e482`, `a36c2cfb`, `7cebbac2`.

### 21–22 Nov 2026 · Runtime Spine (`src-runtime`, `src-command`, `src-wire`) · Oracle bring-up (`Neon, XHigh`) -> independent audit (`Carbon, High` + `Opus/Fable`)
- **caught:** Neon brought up ~5,000 lines (`f2ff04ca`: `pure-proof` 22/22, `oracle-proof` 6/6, `command-proof` 16/16), bytecode-verified Rama 1.6.0's hardcoded `0.0.0.0` binds, and isolated 2 test defects in `checks.clj` (repaired separately in `a1cb405e`); morning audit (`177d252a`, `REVIEW-runtime-spine.md`) broke the night changes independently and settled Question 17 (`seam.clj/moved-on!` vs. closed-session `{:open S}`).
- **got past:** `checks.clj` carried 2 bugs (`first` shadowing, 6s vs. 12s window) until audited separately; real `src-auth` + persistent-cluster Join proof still pending.
- **cost & receipts:** 1 overnight build + 1 morning audit · `d03a03cb`, `f2ff04ca`, `a1cb405e`, `177d252a`.

---

## What Is Untested (Treat as Open Trials, Not Assumptions)

- **Domains:** WebGPU/shaders, UI/UX & visual craft, Electric/Missionary reactive client architecture, greenfield product exploration.
- **Arrangements & families:** `Codex` as a third model family on current Softland slices (all trials above used `Claude` + `Gemini` only), `Fable` at `high`/`xhigh` effort (`max` only above), and `Neon` implementing a Rama module end-to-end (`src-auth/` is the first live trial).
