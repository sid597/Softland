# The stored-forms build of 28 November, stacked and proved

Written 28 November 2026 on Sid's machine after stacking the three branches,
putting the exact words of `‹b8e1bdaf›` and `«59efa1cb»` into
`src-store/README.md`, and running all the proofs once on the top of the
stack. Paths are from the repository root.

This page gathers the whole build across the three earlier pages:

- `HANDOFF-stored-forms-2026-11-28.md` (on `main` at `5b7abc6d`)
- `HANDOFF-emails-and-program-sign-in-2026-11-28.md` (from `program-sign-in-2026-11-28`)
- `HANDOFF-act-form-2026-11-28.md` (from `act-form-2026-11-28`)

What is Sid's here: his answers of 28 November (`‹bfa41fde›` answering
`«91f1705d»`, and `‹b8e1bdaf›` answering `«59efa1cb»`), and his prompt for
this finishing run (including leaving open how a program learns what it
holds). Every other rule, shape, default and mend is a session's and his to
turn.

---

## 1. Where the three branches stand

All three branches share merge-base `de68997d` with `main` (`b28da7cc`, which
holds `5b7abc6d` and `b28da7cc` — the first handoff and a memory note, touching
no file that the three branches touch). Nothing was rebased and nothing was
pushed by this run.

| Stack order | Branch | Base | Head | Origin | Diff against base |
|---|---|---|---|---|---|
| 1 | `auth-stores-2026-11-28` | `main` (`de68997d`) | `0f2024f7` | `0f2024f7` (already at head) | 41 files, +5,766 / −1,612 |
| 2 | `program-sign-in-2026-11-28` | `auth-stores-2026-11-28` (`0f2024f7`) | `a4f92dfe` | `a4f92dfe` (already at head) | 41 files, +1,810 / −421 |
| 3 | `act-form-2026-11-28` | `program-sign-in-2026-11-28` (`a4f92dfe`) | this commit (on `efcbf833`) | `80bf5eff` (fast-forwards) | 56 files, +4,120 / −3,001 (before this page) |
| **Whole stack** | `de68997d..act-form-2026-11-28` | `de68997d` | this commit | — | 102 files, +11,605 / −4,943 (before this page) |

Every proof ran on `efcbf833` and passed with zero failures, so no mend commit
was needed on any of the three branches.

---

## 2. What was built, with its commits

### 2.1 Branch 1 — `auth-stores-2026-11-28` (`de68997d..0f2024f7`, 10 commits)

Auth's stored forms and emails, accounts and invitations (F30, F6, F17, F18):

| Commit | What it holds |
|---|---|
| `575ee201` | **F30:** who made an agent (`:made-by`) and whose ending ends it (`:ends-with`) are stored apart; `end-actor` and the runtime's `beneath?` follow `:ends-with`; depth bound stays on `:made-by` |
| `b4bb7da7` | **F6:** an ask takes `:asking-hash` beside `:hash`; the waiting program polls with `:asking-secret` so the credential's secret crosses no wire before her yes |
| `93120b41` | **F17:** a time that passes ends nothing; writes thin expired items from `$$live` (`thin` / `thin-watched-on-root!`) without marking them ended; `$$live` carries `:ends` so unwatched ends reach every runtime copy; `who-is` and root writes check the sign-in's and actor's records (`signin-ended?`) |
| `06494caa` | **F18 (Auth):** Auth takes an email's hash (`{:by :email :hash H}` and `:email-hash`), never the address to match; `$$claims` keeps person, waiting invitation (with `:ends` and `:op-id`) and links apart; `$$invite-ops` remembers invite ops; `take-back` and `unlink` name the op that made what they remove; new reads `invitation-of` and `accounts-of`; round on `AccountLinking` and release at step 5 |
| `660fb689` | Design brief, pre-build adversarial break of the steps (`emails-settled.md`), and Auth builder's report |
| `7bccc448` | **F18 (runtime, wire, setup):** SHA-256 of `"softland/email/" + lower(address)` in one place; `--invitation-ms` (7 days default); `:invite` (including `:herself`), `:take-back-invite`, `:accounts`, `:unlink-account`; `setup!` brought to the new contract |
| `6e5a52ca` | Review mend (findings 1, 2, 8): beside a person's links only her own invite (`invited-by = P`) writes a waiting invitation; runtime generates a fresh `:op-id` per `:invite` word so expired/taken-back invitations renew for the same person; `setup!` reports `:entered?` |
| `9ef9ae48` | Review mend (findings 3, 5, 6, 7): `enter` step 5 releases a `:failing` linking; code claim rechecked once when both probes fail; rule of a replayed `enter` after `unlink` stated in `SPEC.md`; harness checks depot records and provider `:says` erasure |
| `45fabb15` | Review mend (finding 4): person's account entry keyed by its link (`"<P>\0na:<account-key>\0<link-op>"`) so a late step 4 from an unlinked link cannot overwrite or clear a newer link's entry |
| `0f2024f7` | Review report (`review-of-the-auth-branch.md`), briefs and runtime builder's report |

### 2.2 Branch 2 — `program-sign-in-2026-11-28` (`0f2024f7..a4f92dfe`, 12 commits)

A program that signs in, what the yes page lends, and Auth's attribution marks
(F14, F15, F2 Auth part):

| Commit | What it holds |
|---|---|
| `44036ec6` | The yes page signs in a new agent or an existing live agent of hers, and lends what she ticks (`see`, `write` in her own lane) as her own act under ids derived from `(root, sign-in)` |
| `1fa41485` | WIP saved when the cloud container restarted mid-build |
| `769531cc` | Check of an agent signed in again writes under the lend of its first sign-in |
| `1062da9a` | **F14, F15:** Auth refuses `:program` sign-ins and `answer-ask` yeses that name the root as actor (`:is-a-person`); runtime `person?` requires `actor = root` and `:browser` sign-in; person-only wire words refuse programs with `:do-that-as-an-agent` (`:do-that-at-a-browser` removed); all runtime/CLI checks sign programs in as agents |
| `fff2b5ab` | **F2 (Auth):** 22 November calls in `SPEC.md`, `QUESTIONS.md`, Auth READMEs and proof sentences marked as "a session's default, Sid's to turn" |
| `a3a8709b` | Builders' report and review report (`review-of-program-sign-in.md`) |
| `52a53c41` | Review mend (finding 2): after a yes, the page reads `sign-in/lent-now` and answers 200, 409 or 503 from what stands |
| `22f0e0e7` | Review mend (finding 3): `seam/caller-now` answers a legacy stored `:program` sign-in whose actor is its root as `:ended` |
| `289bd140` | Review mend (findings 4, 6): yes page compares `:actor-named` rather than label; honest messages when an agent cannot be signed in or `under` fails; CLI help notes `give` to a person is refused without `:give` |
| `70d685ff` | Review mend (finding 5): three remaining 22 November attribution marks and seven sentences made readable |
| `9c03ea6a` | A new agent's id at her yes is derived from `(root, code, label)` so a second answer naming a differently named new agent is not told "Done" |
| `a4f92dfe` | Branch handoff (`HANDOFF-emails-and-program-sign-in-2026-11-28.md`) |

### 2.3 Branch 3 — `act-form-2026-11-28` (`de68997d..80bf5eff` + merge `cc7c1b1c` + `efcbf833` + this page)

The form of an act and the store's attribution marks (F24, F23, F22, F2 store
part), stacked on top of `program-sign-in-2026-11-28`:

| Commit | What it holds |
|---|---|
| `34a88494` | **F24:** `:subjects` and `:claimed-when` removed from an act's form (now 8 parts; carrying either is refused `:unknown-part`) |
| `567018b8` | **F23:** a fact's `:k` is `:id` (a readable keyword or a UUID) |
| `ea6cbb06` | **F22 (source & pure proofs):** depot word removed from `:act-id` (`[lane uuid]`, `:fact-id` `[[lane uuid] index]`) and `:depot` removed from depot records; door reads `[lane :softland.store/depot]` per send (`send-act`, `send-acts`) and on `answer-of` |
| `37a5be43` | WIP: cluster proofs brought to two-slot `act-id` |
| `18b605cf` | `door-proof`, `module-proof` and `read-path-proof` brought to two-slot `act-id` |
| `620cf968` | Runtime seam `send-all-now` sends a series through `door/send-acts` (one depot-fact read per lane for the series) |
| `4ad925e5` | WIP: run-again fixture, judge `reference.clj`, and three pages |
| `506a0376` | Run-again fixture remade for the two-slot `act-id` and two proof faults fixed |
| `a31ad649` | Judge's `gate-histories/reference.clj` (`fix` mode) brought to the 28 November act form |
| `5aac40b4` | `src-server/softland/server/sign_in.clj` drops `:subjects` |
| `0dcfa73c` | Entry 3 of "Decisions of 28 November 2026 — the form of an act" and pointers from older entries |
| `51f2aff2` | **F2 (store):** 6 October entries 4 & 5 and 13 October entry 6 marked "a session's default, Sid's to turn" |
| `cc3faa99` | Runtime `pure-proof` updated to check both `send-act` and `send-acts` in `seam.clj` |
| `9aa2bf92` | `run-again-proof` check 4 reads the UUID from slot 2 of `[lane uuid]` so its planned failures fire |
| `cc2766af` | Review mend: entry 3 headed by Sid's decision rather than the session's mechanism; entry 2 corrected on former `:subjects` writers and `:points-at` |
| `864542a7` | Review mend: `door-proof` sentence verifying a send follows `[lane :softland.store/depot]` rather than `depot-at-making` |
| `cdbb6a5a` | Review mend: sentences for where a series stops in the seam and in the door; `send-all-now` docstring states interrupt behaviour |
| `136838ad` | Review mend: `src-server`'s `send-all` also uses `door/send-acts` |
| `e3744154` | Review mend: twelfth making of the run-again fixture (415 records, 1,807 rows), covering UUID keys and refusals of `:depot`, `:subjects` and `:claimed-when` |
| `105b483d` | Review mend: judge's README and `deps.edn` note that `-M:record` and `-M:run today` hold the old act form |
| `80bf5eff` | Branch handoff (`HANDOFF-act-form-2026-11-28.md`) |
| `cc7c1b1c` | **Stack merge (`Merge program-sign-in-2026-11-28 into act-form-2026-11-28`):** resolves the three conflicts in `test-runtime/softland/runtime/{README.md,pure_proof.clj,command_pure_proof.clj}` keeping both branches' checks, and drops `:subjects #{agent}` from `sign-in/lend!` in `src-runtime/softland/runtime/sign_in.clj` |
| `efcbf833` | **Record words in `src-store/README.md`:** puts the exact questions 4a–4c from `«59efa1cb»` and Sid's answers from `‹b8e1bdaf›` into entries 1–3 of "Decisions of 28 November 2026 — the form of an act", and counts `sign-in/lend!` as the seventh place that formerly wrote `:subjects` |

---

## 3. What ran on the top of the stack (`efcbf833`), with counts

Every proof below ran once on `efcbf833`. Every cluster proof ran one at a time
behind `flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock`.
None failed.

### 3.1 Store proofs and the judge

| Proof | Command | Result | Time |
|---|---|---|---|
| `store.act-proof` | `clojure -M:store -m softland.store.act-proof` | `{:test 8, :pass 157, :fail 0, :error 0}` | 1 s |
| `store.pure-proof` | `clojure -M:store -m softland.store.pure-proof` | `{:test 21, :pass 779, :fail 0, :error 0}` | 2 s |
| `store.permission-proof` | `clojure -M:store -m softland.store.permission-proof` | `{:test 14, :pass 72, :fail 0, :error 0}` | 1 s |
| `store.read-path-pure-proof` | `clojure -M:store -m softland.store.read-path-pure-proof` | `{:test 21, :pass 289, :fail 0, :error 0}` | 176 s |
| `store.door-proof` | `clojure -M:store-module -m softland.store.door-proof` | `{:test 26, :pass 292, :fail 0, :error 0}` | 31 s |
| `store.module-proof` | `clojure -M:store-module -m softland.store.module-proof` | `{:test 49, :pass 511, :fail 0, :error 0}` | 54 s |
| `store.read-path-proof` | `clojure -M:store-module -m softland.store.read-path-proof` | `{:test 39, :pass 758, :fail 0, :error 0}` (251 `PASSED` lines; did not hit `main`'s 1-in-3 sentence flake) | 66 s |
| `store.run-again-proof` | `clojure -M:store-module -m softland.store.run-again-proof` | `{:test 5, :pass 49, :fail 0, :error 0}` (twelfth fixture, 415 records / 1,807 rows + check 4's 124 rows, 0 diffs) | 71 s |
| The judge (`gate-histories`) | `clojure -M:run 2000 1` (in `src/proposal/runtime-2026-11-21/gate-histories`) | **6 of 6 active runs GREEN:** `rules` (2,000 histories, 125,404 things, 122,096 decisions), `fix` (2,000 histories, 124,861 things, 121,565 decisions), `fix-mutants` (33/33 caught), `mutants` (69/69 caught), `live` (holds), `live-mutants` (19/19 caught) | 220 s |

### 3.2 Auth proofs, 1,000-history cluster check, harness modes, and full-life measures

| Proof | Command | Result | Time |
|---|---|---|---|
| `auth.reference-proof` | `clojure -M:auth -m softland.auth.reference-proof 42` | `238 passed, 0 failed` | 12 s |
| `auth.module-check` (pure) | `clojure -M:auth -m softland.auth.module-check 42 1000` | `51 passed, 0 failed` (1,000 histories × 3 modes + broken copies + broken clients; `:says-of-every-enter` noted `NOT CAUGHT` through contract as expected) | 40 s |
| `auth.module-check` (cluster, **full 1,000 histories**) | `clojure -M:auth -m softland.auth.module-check 42 1000 softland.auth.module-harness/launch-module` | `3 passed, 0 failed` — **1,000 histories, 80,175 steps, 70,965 writes** (8,580 repeats, 4,002 unfit), **745,404 reads**, **2,644 watches** across 4,010 root changes, 244 deep 32-agent chains, 285 `:too-deep`, 1,411 links made, 623 unlinked, 927 invitations taken back, 0 retries | 823 s |
| `auth.full-life-measure` | `clojure -M:auth -m softland.auth.full-life-measure` | `5 passed, 0 failed` (`m1=true m2=true m3=true m3b=true m4=true`: `who-is` flat 1–4 seeks at 10,000 closed sessions; 20,000 watched roots on 1 task; `end-actor` 331.8 ms under 3k actors / 2k sign-ins / 5k sessions; `end-actor` flat over 3,000 prior ended agents; 0 shared-task bottlenecks) | 68 s |
| `auth.module-harness ends-with` | `clojure -M:auth -m softland.auth.module-harness ends-with` | `1 passed, 0 failed` | 8 s |
| `auth.module-harness reruns` | `clojure -M:auth -m softland.auth.module-harness reruns` | `14 passed, 0 failed` | 7 s |
| `auth.module-harness invitations` | `clojure -M:auth -m softland.auth.module-harness invitations 10` | `11 passed, 0 failed` (includes `*invites` depot & table erasure checks) | 13 s |
| `auth.module-harness late-open` | `clojure -M:auth -m softland.auth.module-harness late-open` | `3 passed, 0 failed` (21,000 watches > `pstate.proxy.limit`) | 21 s |
| `auth.module-harness partial-outage` | `clojure -M:auth -m softland.auth.module-harness partial-outage` | `2 passed, 0 failed` (swept in 3,032.7 ms) | 14 s |
| `auth.module-harness unfit-record` | `clojure -M:auth -m softland.auth.module-harness unfit-record` | `2 passed, 0 failed` | 12 s |
| `auth.module-harness slice-b` | `clojure -Sdeps '<+:auth auth-concurrent/src>' -M:auth -m softland.auth.module-harness slice-b 2` | `PASSED` Slice b root rules 100% + carbon `step3-run` 2 rounds, 14 multi-thread batches (`0 failures`) | 89 s |
| `auth.module-harness step3` | `clojure -Sdeps '<+:auth auth-concurrent/src>' -M:auth -m softland.auth.module-harness step3 2` | `PASSED` `says` (6/6) + `not-reached` (3/3) + carbon `step3-run` all 9 scenarios with live watches, 2 rounds, 18 multi-thread batches (`0 failures`) | 151 s |

### 3.3 Wire, command-line, runtime (IPC and persistent cluster), attacks, and server proofs

| Proof | Command | Result | Time |
|---|---|---|---|
| `wire.pure-proof` | `clojure -M:wire -m softland.wire.pure-proof` | `42 passed, 0 failed` | 1 s |
| `runtime.pure-proof` | `clojure -M:runtime -m softland.runtime.pure-proof` | `37 passed, 0 failed` | 7 s |
| `runtime.command-pure-proof` | `clojure -M:runtime -m softland.runtime.command-pure-proof` | `38 passed, 0 failed` | 1 s |
| `runtime.proof` | `clojure -M:runtime -m softland.runtime.proof` | `72 passed, 0 failed` | 50 s |
| `runtime.command-proof` | `clojure -M:runtime -m softland.runtime.command-proof` | `16 passed, 0 failed` | 38 s |
| `runtime.command-review-proof` | `clojure -M:runtime -m softland.runtime.command-review-proof` | `11 passed, 0 failed` | 35 s |
| `runtime.two-lines-proof` (IPC) | `clojure -M:runtime -m softland.runtime.two-lines-proof` | `3 passed, 0 failed` | 18 s |
| `runtime.clock-ahead-proof` | `clojure -M:runtime -m softland.runtime.clock-ahead-proof` | `4 passed, 0 failed` | 17 s |
| `runtime.review-faults-proof` | `clojure -M:runtime -m softland.runtime.review-faults-proof` | `6 passed, 0 failed` | 20 s |
| `runtime.open-session-proof` | `clojure -M:runtime -m softland.runtime.open-session-proof` | `5 passed, 0 failed` | 18 s |
| `runtime.line-review-proof` | `clojure -M:runtime -m softland.runtime.line-review-proof` | `14 passed, 0 failed` | 17 s |
| `attacks.slow-readers` | `clojure -M:runtime -m softland.runtime.attacks.slow-readers` | `1 passed, 0 failed` | 38 s |
| `attacks.yes-page` | `clojure -M:runtime -m softland.runtime.attacks.yes-page` | `1 passed, 0 failed` | 15 s |
| `attacks.closed-session` | `clojure -M:runtime -m softland.runtime.attacks.closed-session` | `1 passed, 0 failed` | 22 s |
| `attacks.unwatched-standing` | `clojure -M:runtime -m softland.runtime.attacks.unwatched-standing` | `1 passed, 0 failed` | 16 s |
| `attacks.lost-replacement` | `clojure -M:runtime -m softland.runtime.attacks.lost-replacement` | `1 passed, 0 failed` | 26 s |
| `attacks.sign-in-pace` | `clojure -M:runtime -m softland.runtime.attacks.sign-in-pace` | `1 passed, 0 failed` | 14 s |
| `attacks.watch-caps` | `clojure -M:runtime -m softland.runtime.attacks.watch-caps` | `1 passed, 0 failed` | 17 s |
| `runtime.cluster-proof` (**persistent Rama 1.6.0 cluster**) | `clojure -M:runtime -m softland.runtime.cluster-proof --conductor 127.0.0.1 --conductor-port 1983` | `80 passed, 0 failed` | 103 s |
| `runtime.worker-kill-proof` (**persistent Rama 1.6.0 cluster**) | `clojure -M:runtime -m softland.runtime.worker-kill-proof --conductor 127.0.0.1 --conductor-port 1983` | `7 passed, 0 failed` | 44 s |
| `runtime.two-lines-proof` (**persistent Rama 1.6.0 cluster**) | `clojure -M:runtime -m softland.runtime.two-lines-proof --conductor 127.0.0.1 --conductor-port 1983` | `4 passed, 0 failed` | 31 s |
| `server.pure-proof` | `clojure -M:server -m softland.server.pure-proof` | `68 passed, 0 failed` | 1 s |
| `server.seam-proof` | `clojure -M:server -m softland.server.seam-proof` | `43 passed, 0 failed` | 17 s |
| `server.http-proof` | `clojure -M:server -m softland.server.http-proof` | `37 passed, 0 failed` | 13 s |

Every item listed in section 8 of `HANDOFF-emails-and-program-sign-in-2026-11-28.md`
as never run (`cluster-proof`, `two-lines-proof`, `open-session-proof`,
`worker-kill-proof`, the six remaining attacks, `full-life-measure`, `slice-b`,
and `module-check` on a cluster at its full 1,000 histories) ran and passed.

### 3.4 What could not be run, and why

1. **`softland.store.line-statements`** (`test-store/softland/store/line_statements.clj`):
   requires `piece3-statements.*` under `src/proposal/runtime-2026-11-21/store-piece-3/statements/src`
   (not on `:store-module`'s `:replace-paths`), and `piece3-statements.runner`
   itself hardcodes the pre-28-November three-slot `:act-id` (`(nth act-id 1)`
   as `:depot`).
2. **The judge's `-M:record` and `-M:run today` modes**
   (`src/proposal/runtime-2026-11-21/gate-histories`): require an exported copy
   of `src-store` from before the permission fix (`25cafbfe`) and hold the
   pre-28-November act form, as noted in `105b483d`.
3. **Formal model checks of Auth's multi-task steps**
   (`src/proposal/runtime-2026-11-21/spine/auth/agreement/` and
   `overnight-audit/auth-concurrent` `:step1`): still encode the pre-28-November
   Auth contract (take-out as an end, single-hash ask, and the old `invite` /
   `enter` without email hashes, `take-back`, `unlink`, rounds, or step-5
   release).
4. **`module-harness` modes `slice-a` and `slice-c` separately:** not run as
   separate processes because `step3 2` runs `says`, `not-reached`, and all 9
   carbon scenarios with live watches (a strict superset of `slice-a` and
   `slice-c`). Note that `slice-b` and `step3` require adding
   `src/proposal/runtime-2026-11-21/overnight-audit/auth-concurrent/src` to
   `:auth`'s `:replace-paths` via `-Sdeps`, because `:auth` in `deps.edn`
   isolates `src-auth` and `test-auth`.
5. **Live Google HTTPS OAuth/JWKS and real browser clicks:** all proofs run
   against `google_stand_in` and `test-runtime`'s HTTP/WebSocket drivers.

---

## 4. Choices that are Sid's to turn

Everything below is a session's default, design choice, or fork, gathered from
the three handoff pages and from this stacking run.

### 4.1 Unmade call left open by Sid's direction

- **How a program learns what it holds when an existing agent is signed in
  again (`HANDOFF-emails-and-program-sign-in-2026-11-28.md` §6, §8).** When she
  signs an existing agent of hers in again with no box ticked, it keeps what it
  holds in the store, but the command line computes its default permission from
  *this* sign-in (`lent-at-yes`), which nobody wrote; so without `--permission`
  its writes are refused, and ticking the boxes at every sign-in adds a new
  standing lend each time (up to the store's cap of 32 per holder). Two builder
  mends hit real walls: the store forbids reusing an ended permission's id
  (`:a-permission-id-takes-one-permission`), and `sees-under?` forbids an agent
  lent only `:write` from reading its own lend in a lane it cannot `:see`. Three
  roads to close it, each Sid's:
  1. Store rule: a holder may always read the permissions it holds, even in a
     lane it cannot `:see`.
  2. Runtime/wire rule: the runtime tells a program at sign-in which permissions
     the yes page has lent its agent.
  3. Yes-page rule: a yes with boxes ticked for an existing agent ends the
     page's earlier lend(s) and lends anew.

### 4.2 Auth's first three changes (F30, F6, F17)

1. **How an end reaches an unwatched sign-in or session (F17):** `end-actor`
   still writes `ended?`/`closed?` on what is currently in `$$live`, while
   `who-is` and root writes also read the sign-in's and actor's records
   (`signin-ended?`, adding 1–2 seeks on agent sign-ins). Alternatives: an
   index of unwatched sign-ins per actor, or dropping the `$$live` cascade
   marks and always reading the actor/sign-in records.
2. **Each thing thins from `$$live` by its own `:until` (F17):** a session can
   remain watched after its sign-in has thinned; `LiveSession` stores `actor`
   for `end-actor`.
3. **`:ends` in `$$live` counts end-writes, not ended things (F17):** repeats
   and already-ended items add 0.
4. **No healing of `$$live` after a write from a clock far ahead (F17):** a
   far-ahead write empties `$$live` for that root without ending anything, so
   unwatched standing sign-ins are absent from `under` until their secret is
   next replaced (ending by id still works). An `exp:`-index healing design was
   sketched by the builder and not built.
5. **Asking secret vs credential secret (F6):** the runtime refuses an ask
   whose `:hash` and `:asking-hash` are equal (`:read-the-word`); Auth itself
   does not compare them. Two asks match as a repeat only when both hashes
   match; same `:hash` with a different `:asking-hash` is `:in-use` (question
   31).
6. **Smaller F30/F17 defaults:** `:ends-with` starts as `:made-by` and no call
   sets another value yet; the 32-depth bound stays on `:made-by`; `end-actor`'s
   2-scan from the actor's `seq` assumes whatever ends with an agent was made
   after it; `Caller.until` on an open connection is not refreshed on `:who-am-i`
   after `:replace-secret`; `opened-in` reports `:not-live` on expired time as
   `{:why :ended}`.

### 4.3 Emails, accounts and invitations (F18)

1. **`invite` keeps an unswept memory of its op (`$$invite-ops`):** departs
   from the 22 November default ("no memory of calls") so an `invite` replayed
   by Rama under `:all-after` cannot resurrect a taken-back invitation or an
   unlinked person.
2. **An `enter` replayed by Rama after `unlink` is judged as a new enter at its
   old `:at`:** stated in `SPEC.md`, not closed. If an older waiting invitation
   still matches, replay can relink without a new sign-in and restore `:says`
   in `$$accounts`. Closing this class cleanly would take a per-sign-in record,
   an account tombstone, or moving Auth's writes onto an exactly-once
   microbatch topology.
3. **Clock rule `invited-at <= at < ends`:** trusts runtime copy clocks (a
   behind clock gets `:not-invited` until it catches up; a far-ahead invite
   waits until its `:invited-at`).
4. **Claim shape and who writes beside links:** `$$claims` separates person,
   waiting invitation, and links; once links stand for `P`, only an invite with
   `invited-by = P` writes a waiting invitation beside them (others return
   `{:invited P}` without opening a 7-day door).
5. **`take-back` and `unlink` name the op that made what they remove:**
   `take-back {:person :claim :invite}` always returns `{:taken-back P}`;
   `unlink {:account :link :person}` returns `{:unlinked P}`,
   `:another-persons`, or `:linking`.
6. **Account entries keyed by link (`"<P>\0na:<account-key>\0<link-op>"`):**
   `accounts-of` can momentarily return one account twice across overlapping
   links.
7. **Person id derivation and re-inviting after full unlink:** person id is
   derived from `(inviter-root, word-uuid, claim)`. Sending the same `:invite`
   word again renews an expired/taken-back invitation — and if a person
   unlinked every account, the first inviter sending the same word invites her
   anew (for `setup!`, this is the owner's way back in; whether an ordinary
   inviter may do so is open for Sid).
8. **`enter` helping steps:** round counter on `AccountLinking`; step 5
   releases on failure/mismatch (leaving the consumed invitation spent); code
   claim rechecked once when both probes fail.
9. **Erasure, `:herself`, and surface defaults:**
   - Email hash is plain SHA-256 of `"softland/email/" + lower(address)` (offline
     guessable by someone holding the log, vs an HMAC secret whose loss would
     strand bound emails).
   - Whether `:says` should carry the provider's email at all, and whether
     `[provider subject]` should also be hashed.
   - Removing `:says`/subjects from `*enters` depot logs requires an operator
     depot migration (not built); no list of invitations naming a person and no
     sweep of expired invitations or `$$invite-ops` is built.
   - `:herself` takes an unverified email string (a mistyped address lets
     whoever owns that email enter as her; proving the second account in the
     same browser would close it); adding an account requires a browser while
     `:unlink-account` is a wire word; `--invitation-ms` defaults to 7 days; no
     CLI subcommands were added for the four person-only email words.

### 4.4 A program that signs in (F14, F15, lend)

1. **No program is a person:** Auth refuses with `:is-a-person`; runtime
   `person?` requires `actor = root` and `:browser` sign-in; legacy stored
   `:program` sign-ins for a root are answered as `:ended` by the runtime;
   person-only wire words refuse programs with `:do-that-as-an-agent` and stay
   in the CLI's table so the runtime's refusal is returned.
2. **New agent id at yes:** derived from `(root, ask-code, label)`.
3. **Yes page and lend defaults:** new agent starts with `see` and `write`
   ticked; existing agent starts unticked without displaying current permissions;
   lend grants no `:give` (programs cannot `give` to a person); each lend opens
   a 24-hour browser session (counted against the 30/min per-sign-in cap) so
   duplicate page posts share one session and one act; HTTP 200 / 409 / 503 copy
   reads `lent-now` for what stands.

### 4.5 The form of an act (F24, F23, F22)

1. **Two rows under one `act-id` across the two gates (F22):** if a non-door
   appender puts the same raw act onto the wrong depot as well, the lane's gate
   accepts it and the other gate refuses it (`:lane-belongs-to-the-other-gate`)
   under the same `[lane uuid]` and digest. Today `answer-of` reads only the
   lane's current depot, so nothing reaches the other gate's row; if changing
   a lane's speed is built later, earlier acts would be looked up in the other
   record unless wrong-gate arrivals become strays in `walk/begin` (a store
   rule change left for Sid).
2. **No `:subjects` fact added anywhere, and finding a person's acts is open
   (F24):** all seven former writers of `:subjects` (including `sign-in/lend!`)
   already name the person or agent in `:holder` or `:e`. For future acts that
   do not, `{:e <act-uuid> :k :subjects :v #{...}}` is found by the act's id,
   requires `:write`, is visible to `:see`, requires marking `:subjects`, and
   `:points-at` matches whole values rather than set members — so how to query
   all acts about a person remains open for Sid.
3. **UUID keys (F23):** `:k` is `:id`; range queries order keywords and UUIDs
   together by printed text (`query.clj`); no gate enforces that a UUID key has
   name/description facts.
4. **Series sends through `door/send-acts` (F22):** both `src-runtime` and
   `src-server` read each lane's depot fact once per series and append `n`
   acts, then read answers. An interrupt mid-series can append one more act and
   leave `{:could-not :read-the-answer}` on earlier receipts; `ration` sees the
   whole series upfront.

### 4.6 Choices taken in this stacking run

1. **Merged, never rebased (`cc7c1b1c`):** `program-sign-in-2026-11-28`
   (`a4f92dfe`) was merged into `act-form-2026-11-28` (`80bf5eff`), keeping
   pushed commits and their cloud `Claude` author identities untouched.
2. **`sign-in/lend!` brought to the new act form (`cc7c1b1c`):** dropped
   `:subjects #{agent}` with no replacement fact, matching the other six former
   `:subjects` writers.
3. **Test conflicts (`cc7c1b1c`):** kept every check from both branches in
   `test-runtime/softland/runtime/{README.md,pure_proof.clj,command_pure_proof.clj}`.
4. **Store README decisions (`efcbf833`):** quoted 4a, 4b and 4c verbatim from
   `«59efa1cb»` and `‹b8e1bdaf›` in `src-store/README.md`, and updated entry 2's
   count of former `:subjects` writers from six to seven.
5. **Harness classpath (`slice-b`, `step3`):** passed `auth-concurrent/src` via
   `clojure -Sdeps` during the proof run rather than adding `src/proposal/...`
   to `:auth` in `deps.edn`.

---

## 5. What the reviews found

1. **Review of `auth-stores-2026-11-28`** (`review-of-the-auth-branch.md`):
   broke 8 things in `de68997d..7bccc448`. Seven were mended in `6e5a52ca`,
   `9ef9ae48` and `45fabb15` (fresh `:op-id` per `:invite` word and only `P`
   writing waiting invitations beside `P`'s links; `setup!` after entry;
   link-keyed person account entries after the mending builder falsified a
   timestamp-based mend under clock skew; code-claim recheck when both probes
   fail; step-5 release of `:failing` linkings; `*invites` depot and provider
   `:says` erasure checks). One (replayed `enter` after `unlink` matching an
   older waiting invitation) was documented in `SPEC.md` and left as choice 4.3.2.
2. **Review of `program-sign-in-2026-11-28`** (`review-of-program-sign-in.md`):
   broke 6 things in `0f2024f7..fff2b5ab`. Five were mended in `52a53c41`,
   `22f0e0e7`, `289bd140`, `70d685ff` and `9c03ea6a` (`lent-now` read after
   yes; legacy root `:program` sign-ins answered as `:ended`; `:actor-named`
   comparison instead of label; new agent id derived from label too; three
   missed 22 November marks). Finding 1 — how a re-signed-in agent learns what
   it holds — hit the two walls in §4.1 and is left open for Sid.
3. **Review of `act-form-2026-11-28`** (`HANDOFF-act-form-2026-11-28.md` §5):
   found no code fault requiring mend; 7 sentence, fixture, doc and
   `src-server` `send-all` improvements were mended in `cc2766af..105b483d`,
   and 5 design notes are recorded in §4.5.
4. **Review of the earlier 27 and 28 November builds on `main`**
   (`HANDOFF-stored-forms-2026-11-28.md` §10): audited `de68997d` before these
   three branches and is unmended across the stack — F8 CLI journal edge cases
   (unreadable journal overwritten, `--as` run dropping earlier entries,
   unlocked concurrent `keep!`), F11 seam-refused watch returning `:ended` vs
   line-refused `:gives`, F9 `{:could-not :read-the-answer}` nested under
   `:answer` exiting 0, F7 `begins-as-data?` rejecting dates/times/ordinals
   like `2026-11-28` and `3rd`, words behind `:sign-out` running before
   standing closes, `give` 3rd arg checked inside `with-line`, trailing unknown
   flag reporting `:flag-with-no-value`, stale `:no-fact` output in
   `attacks/README.md:142`, and `read-path-proof`'s 1-in-3 sentence fault at
   `read_path_proof.clj:132` (`(every? :anew erin-later)`).

---

## 6. What is left

1. **Push `act-form-2026-11-28` and open the three stacked pull requests**
   (commands and PR bodies below; `main`, `auth-stores-2026-11-28` and
   `program-sign-in-2026-11-28` on `origin` already match local).
2. **Sid's call on how a program learns what it holds** (§4.1), plus any choices
   in §4.2–4.6 he wants turned.
3. **Earlier `main` review findings** (§5.4), if and when Sid schedules a mend
   pass for them.
4. **Out-of-step historical/proposal docs not edited:** `DECISIONS-AND-FIXES.md`
   §7, `FIRE.edn`, `MODEL.md` (`[who lane granted-in]`), `spine/auth/{PLAN,BUILD,IMPLICIT_SPEC,PLAN_VALIDATION}.md`
   and `agreement/`, and `SPEC.md` / `src-runtime/README.md` noting that each
   yes with boxes ticked adds an unended lend up to 32 per holder.
5. **No data migration** for pre-28-November Auth clusters or depot logs, and no
   TLA+/model check yet for `enter`'s helping steps with claims that come back.

---

## 7. Commands and text for the three pull requests

### Commands

`main` (`b28da7cc`), `auth-stores-2026-11-28` (`0f2024f7`) and
`program-sign-in-2026-11-28` (`a4f92dfe`) are already at those commits on
`origin`. Only `act-form-2026-11-28` has new local commits (fast-forward on
`80bf5eff`):

```sh
git -C /mnt/data/projects/Softland-act-form push -u origin act-form-2026-11-28

gh pr create --base main --head auth-stores-2026-11-28 \
  --title "Auth: stored forms, emails as hashes, accounts and invitations" \
  --body-file <pr-1-body>

gh pr create --base auth-stores-2026-11-28 --head program-sign-in-2026-11-28 \
  --title "Sign-in: no program is a person, the yes page's lend, and 22 Nov marks" \
  --body-file <pr-2-body>

gh pr create --base program-sign-in-2026-11-28 --head act-form-2026-11-28 \
  --title "Store and stack: the form of an act" \
  --body-file <pr-3-body>
```

*(The exact ready-to-run `gh pr create` commands with inline heredocs are in the
session reply and match the three bodies below.)*

### PR 1 — `auth-stores-2026-11-28` into `main`

**Title:** `Auth: stored forms, emails as hashes, accounts and invitations`

**Body:**
```markdown
## What this holds

First of three stacked pull requests (`de68997d..0f2024f7`, 10 commits):

- **Who made an agent and whose ending ends it (F30):** stored apart (`:made-by` and `:ends-with`); `end-actor` and the runtime's `beneath?` follow `:ends-with`, while the 32-deep chain bound stays on `:made-by`.
- **Asking secret for a waiting program (F6):** `ask` takes `:asking-hash` beside `:hash`, and waiting polls send `:asking-secret` so the credential's secret crosses no wire before her yes.
- **Passing time ends nothing (F17):** root writes thin expired sign-ins and sessions from `$$live` without marking them ended; `$$live` carries `:ends` so unwatched ends reach every runtime copy; `who-is` and root writes check the sign-in's and actor's records.
- **Emails, accounts and invitations (F18):** Auth matches emails by hash (`{:by :email :hash H}` and `:email-hash`), never storing an address in invitations; `$$claims` keeps the person, a waiting invitation (with `:ends` and `:op-id`) and links apart so a person can hold several accounts and re-use freed claims; `take-back` and `unlink` name the op that made what they remove; `invitation-of` and `accounts-of` read them; the runtime and wire add `:take-back-invite`, `:accounts`, `:unlink-account` and `:herself` on `:invite` (7-day default `--invitation-ms`).
- **Adversarial review and mends (`6e5a52ca`, `9ef9ae48`, `45fabb15`):** beside her links only her own invite writes a waiting invitation, with a fresh `:op-id` per `:invite` word; `enter` step 5 releases `:failing` linkings and rechecks the code claim once when both probes fail; person account entries are keyed by link so late hops cannot clear a newer link.

## What ran

On this branch (`45fabb15` / `9ef9ae48`):
- Auth `reference-proof 42` (233 passed, 0 failed), pure `module-check 42 1000` (49/0), cluster `module-check` 100 histories (3/0), `module-harness reruns` (14/0) and `invitations 10` (11/0)
- Wire `pure-proof` (40/0), runtime `pure-proof` (28/0), `command-pure-proof` (36/0), `runtime.proof` (60/0)

On the top of the 3-PR stack (`efcbf833`), the full suite ran once and passed with 0 failures, including Auth cluster `module-check 42 1000` (1,000 histories, 80,175 steps, 70,965 writes, 745,404 reads, 2,644 watches), `full-life-measure` (5/5), `module-harness` (`ends-with`, `reruns`, `invitations 10`, `late-open`, `partial-outage`, `unfit-record`, `slice-b 2`, `step3 2`), all runtime IPC and persistent-cluster proofs, and all 7 attacks.

## What is open

- Session choices for Sid to turn (listed in `HANDOFF-emails-and-program-sign-in-2026-11-28.md` §5 and `HANDOFF-stored-forms-2026-11-28.md` §8): `$$invite-ops` memory of invite ops; a replayed `enter` after `unlink` judged at its old `:at`; plain SHA-256 email hash vs keyed hash; whether `:says` should keep the provider's email; `:herself` taking an unverified address; unbuilt `$$live` healing after a far-ahead clock; operator depot migration to scrub `:says` from `*enters` logs; sweeping expired invitations and `$$invite-ops`.
- Formal model checks (`spine/auth/agreement/` and `auth-concurrent` `:step1`) still describe the pre-28-November contract and were not run.
```

### PR 2 — `program-sign-in-2026-11-28` into `auth-stores-2026-11-28`

**Title:** `Sign-in: no program is a person, the yes page's lend, and 22 Nov marks`

**Body:**
```markdown
## What this holds

Second of three stacked pull requests (`0f2024f7..a4f92dfe`, 12 commits, on `auth-stores-2026-11-28`):

- **No program is a person (F14, F15):** Auth refuses `:program` sign-ins and `answer-ask` yeses that name the root as actor (`:is-a-person`); runtime `person?` requires `actor = root` and a `:browser` sign-in; legacy stored `:program` sign-ins for a root are answered as `:ended`; person-only wire words refuse programs with `:do-that-as-an-agent` (`:do-that-at-a-browser` leaves the wire); runtime and CLI proofs sign programs in as agents.
- **The same agent again, and what the yes page lends:** the yes page offers a new agent (with `see` and `write` in her own lane ticked by default) or an existing live agent of hers (unticked by default, "it keeps what it holds"), sends her lend act under ids derived from `(root, sign-in)`, and reads `lent-now` after the send to report 200, 409 or 503 from what stands. A new agent's id at her yes is derived from `(root, code, label)`.
- **Whose call a thing is (F2, Auth's part):** the 22 November calls in `SPEC.md`, `QUESTIONS.md`, Auth's READMEs and proof sentences are marked as a session's defaults, Sid's to turn.

## What ran

On this branch (`9c03ea6a`):
- Auth `reference-proof 42` (238/0), pure `module-check` (51/0), cluster `module-check` 100 histories (3/0)
- Wire `pure-proof` (41/0), runtime `pure-proof` (30/0), `command-pure-proof` (37/0)
- `runtime.proof` (72/0), `command-proof` (16/0), `review-faults-proof` (6/0), `attacks.yes-page` (1/0)

On the top of the 3-PR stack (`efcbf833` — which also removes `:subjects` from `sign-in/lend!` to match the new act form), the full proof run passed with 0 failures across Auth (1,000 cluster histories, `full-life-measure`, `slice-b 2`, `step3 2`), store, wire, runtime IPC and persistent-cluster proofs (`cluster-proof` 80/0, `worker-kill-proof` 7/0, `two-lines-proof` 4/0), `open-session-proof` (5/0), and all 7 attacks.

## What is open

- **Left open for Sid's call (not part of this build):** how a program learns what it holds when an existing agent is signed in again with no box ticked (`HANDOFF-emails-and-program-sign-in-2026-11-28.md` §6 and §8). In the store the agent keeps its earlier lends, while the command line's default computes the lend id of its *current* sign-in (which nobody wrote unless boxes were ticked again, and ticking at every sign-in accumulates lends up to 32 per holder). Closing this requires a rule choice of Sid's (letting a holder always read its own permissions even without `:see` on the lane, having the runtime tell a program its page lends at sign-in, or having a yes end earlier page lends and lend anew).
- Other session choices for Sid to turn (`HANDOFF-emails-and-program-sign-in-2026-11-28.md` §6): `:is-a-person`, `(root, code, label)` agent id derivation, lend holding no `:give` (so programs cannot `give` to a person), 24-hour browser session per lend, and yes-page status/copy details.
```

### PR 3 — `act-form-2026-11-28` into `program-sign-in-2026-11-28`

**Title:** `Store and stack: the form of an act`

**Body:**
```markdown
## What this holds

Third of three stacked pull requests (`act-form-2026-11-28` merged on top of `program-sign-in-2026-11-28`):

- **`:subjects` and `:claimed-when` removed from an act (F24):** an act has eight parts (`act.clj`); carrying either removed part is refused `:unknown-part`. All seven former `:subjects` writers across `src-runtime`, `src-command` and `src-server` — including `sign-in/lend!` from `program-sign-in-2026-11-28` (`cc7c1b1c`) — drop `:subjects` without adding a replacement fact, as each act already names the person or agent in `:holder` or `:e`.
- **A fact's `:k` is an `:id` (F23):** `:k` accepts a readable keyword or a UUID across validation, digesting, gates, rows and the read path.
- **Depot word out of `:act-id` (F22):** `:act-id` is `[lane uuid]` (`:fact-id` is `[[lane uuid] index]`) and depot records carry no `:depot`. The door reads `[lane :softland.store/depot]` on `send-act`, `send-acts` (used by both `src-runtime` and `src-server` seams for series sends: 1 read per lane + `n` appends) and `answer-of`.
- **Fixture, judge, store README and F2 marks:** twelfth making of the `run-again-proof` fixture (415 records, 1,807 rows, covering UUID keys and refusals of `:depot`, `:subjects`, `:claimed-when`); `gate-histories` `fix` mode updated to the new act form; 6 Oct entries 4–5 and 13 Oct entry 6 marked as session defaults; entries 1–3 of "Decisions of 28 November 2026 — the form of an act" in `src-store/README.md` updated with the exact words of `«59efa1cb»` and `‹b8e1bdaf›` (`efcbf833`); whole-build handoff in `HANDOFF-stacked-build-2026-11-28.md`.

## What ran

Full proof run once on the stacked head (`efcbf833`), all passing with 0 failures:
- **Store:** `act-proof` (157/0), `pure-proof` (779/0), `permission-proof` (72/0), `read-path-pure-proof` (289/0), `door-proof` (292/0), `module-proof` (511/0), `read-path-proof` (758/0), `run-again-proof` (49/0, 0 row diffs)
- **Judge (`gate-histories` `2000 1`):** all 6 active runs GREEN (`rules`, `fix` over 124,861 things, `fix-mutants` 33/33, `mutants` 69/69, `live`, `live-mutants` 19/19)
- **Auth:** `reference-proof 42` (238/0), pure `module-check 42 1000` (51/0), cluster `module-check 42 1000` (3/0 across 1,000 histories, 80,175 steps, 745,404 reads, 2,644 watches), `full-life-measure` (5/5), `module-harness` (`ends-with` 1/0, `reruns` 14/0, `invitations 10` 11/0, `late-open` 3/0, `partial-outage` 2/0, `unfit-record` 2/0, `slice-b 2` 14/14 batches, `step3 2` 18/18 batches + `says` 6/6 + `not-reached` 3/3)
- **Wire, runtime, CLI, attacks & server:** `wire.pure-proof` (42/0), `runtime.pure-proof` (37/0), `command-pure-proof` (38/0), `runtime.proof` (72/0), `command-proof` (16/0), `command-review-proof` (11/0), `two-lines-proof` IPC (3/0), `clock-ahead-proof` (4/0), `review-faults-proof` (6/0), `open-session-proof` (5/0), `line-review-proof` (14/0), all 7 attacks (1/0 each), persistent-cluster `cluster-proof` (80/0), `worker-kill-proof` (7/0) and `two-lines-proof` (4/0), `server.pure-proof` (68/0), `server.seam-proof` (43/0), `server.http-proof` (37/0)

Not run: `store.line-statements` (`piece3_statements` is off `:store-module` and hardcodes 3-slot `act-id`), judge `-M:record` / `today` (pre-fix store export & old act form), Auth formal models (`spine/auth/agreement/` and `auth-concurrent` `:step1`, pre-28-Nov contract), and live Google OAuth / browser clicks.

## What is open

- Changing a lane's speed after making is not built (and with it: whether a wrong-depot copy should become a stray in `walk/begin` so two gates never hold rows under one `act-id`, and switching the read path from `depot-at-making` to the lane's depot fact).
- How to query all acts about a person when an act's own facts do not name her (`:points-at` matches whole values, not set members).
- Full summary of choices, reviews and open items across all three branches: `src/proposal/design-pitfalls-review-2026-11-26/HANDOFF-stacked-build-2026-11-28.md`.
```
