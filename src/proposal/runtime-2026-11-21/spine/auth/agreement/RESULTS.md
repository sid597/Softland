# Revised Agreement — Exhaustive Multi-Task Model Check Results

- **Scenarios × modes run:** 32 scenarios (25 carbon + 7 extra) × 4 modes = **128 runs** (3421 ms)
- **Client re-runs checked:** in ALL 4 modes (`check-reruns? true`)
- **Distinct `(state, queues, fault-budget)` configurations explored:** **88,261** (0 capped runs)
- **Quiescent terminal histories checked against all 5 invariants + causal linearizability:** **539**

## Answers to the Five Questions

| # | Question | Broken? | Distinct bug classes | Total scenario×mode hits |
|---|---|---|---|---|
| 1 | Does one account ever get two persons, or one claim two accounts, or one person two claims? | **NO (0 breaks)** | — | 0 |
| 2 | Does one ask ever get two answers that stand? | **NO (0 breaks)** | — | 0 |
| 3 | Is anything left forever in a state that is neither done nor refused? | **NO (0 breaks)** | — | 0 |
| 4 | Does a call (or re-run) give a result/state disallowed by SPEC.md + Sid's 2 calls? | **NO (0 breaks)** | — | 0 |
| 5 | After a write returns, can any read still miss its effect? | **NO (0 breaks)** | — | 0 |

## All Scenario × Mode Runs

| Scenario | Mode | States | Terminal histories | Capped? | Violations |
|---|---|---|---|---|---|
| 2C-01: two invites of different persons on one claim | `pure` | 29 | 3 | false | none |
| 2C-01: two invites of different persons on one claim | `individual` | 118 | 10 | false | none |
| 2C-01: two invites of different persons on one claim | `all-after` | 135 | 7 | false | none |
| 2C-01: two invites of different persons on one claim | `timeout-overlap` | 197 | 9 | false | none |
| 2C-02: two invites of one person under two claims | `pure` | 30 | 2 | false | none |
| 2C-02: two invites of one person under two claims | `individual` | 130 | 6 | false | none |
| 2C-02: two invites of one person under two claims | `all-after` | 141 | 4 | false | none |
| 2C-02: two invites of one person under two claims | `timeout-overlap` | 216 | 6 | false | none |
| 2C-03: duplicate invite of same (person, claim) from two copies | `pure` | 14 | 1 | false | none |
| 2C-03: duplicate invite of same (person, claim) from two copies | `individual` | 61 | 5 | false | none |
| 2C-03: duplicate invite of same (person, claim) from two copies | `all-after` | 40 | 2 | false | none |
| 2C-03: duplicate invite of same (person, claim) from two copies | `timeout-overlap` | 74 | 3 | false | none |
| 2C-04: invite racing 1-claim enter proving that claim | `pure` | 40 | 2 | false | none |
| 2C-04: invite racing 1-claim enter proving that claim | `individual` | 171 | 6 | false | none |
| 2C-04: invite racing 1-claim enter proving that claim | `all-after` | 202 | 4 | false | none |
| 2C-04: invite racing 1-claim enter proving that claim | `timeout-overlap` | 346 | 6 | false | none |
| 2C-05: two 1-claim enters of one account proving C1 and C2 (clocks differ: t1=200 vs t2=100) | `pure` | 37 | 1 | false | none |
| 2C-05: two 1-claim enters of one account proving C1 and C2 (clocks differ: t1=200 vs t2=100) | `individual` | 175 | 3 | false | none |
| 2C-05: two 1-claim enters of one account proving C1 and C2 (clocks differ: t1=200 vs t2=100) | `all-after` | 99 | 2 | false | none |
| 2C-05: two 1-claim enters of one account proving C1 and C2 (clocks differ: t1=200 vs t2=100) | `timeout-overlap` | 361 | 3 | false | none |
| 2C-06: two returning enters of linked account with differing clocks (t1=300 vs t2=100) | `pure` | 22 | 1 | false | none |
| 2C-06: two returning enters of linked account with differing clocks (t1=300 vs t2=100) | `individual` | 72 | 3 | false | none |
| 2C-06: two returning enters of linked account with differing clocks (t1=300 vs t2=100) | `all-after` | 65 | 2 | false | none |
| 2C-06: two returning enters of linked account with differing clocks (t1=300 vs t2=100) | `timeout-overlap` | 153 | 3 | false | none |
| 2C-16: two sequential returning enters of linked account with equal :at=200 and different :says | `pure` | 7 | 1 | false | none |
| 2C-16: two sequential returning enters of linked account with equal :at=200 and different :says | `individual` | 18 | 3 | false | none |
| 2C-16: two sequential returning enters of linked account with equal :at=200 and different :says | `all-after` | 23 | 2 | false | none |
| 2C-16: two sequential returning enters of linked account with equal :at=200 and different :says | `timeout-overlap` | 28 | 3 | false | none |
| 2C-07: two accounts proving one claim (1-claim enters) | `pure` | 85 | 2 | false | none |
| 2C-07: two accounts proving one claim (1-claim enters) | `individual` | 429 | 6 | false | none |
| 2C-07: two accounts proving one claim (1-claim enters) | `all-after` | 429 | 4 | false | none |
| 2C-07: two accounts proving one claim (1-claim enters) | `timeout-overlap` | 1,031 | 6 | false | none |
| 2C-08: two accounts proving shared code claim (2-claim fallback to email) | `pure` | 94 | 2 | false | none |
| 2C-08: two accounts proving shared code claim (2-claim fallback to email) | `individual` | 492 | 6 | false | none |
| 2C-08: two accounts proving shared code claim (2-claim fallback to email) | `all-after` | 480 | 4 | false | none |
| 2C-08: two accounts proving shared code claim (2-claim fallback to email) | `timeout-overlap` | 1,214 | 6 | false | none |
| 2C-09: 2-claim enter (code nil, email waiting) racing invite on the code claim | `pure` | 55 | 2 | false | none |
| 2C-09: 2-claim enter (code nil, email waiting) racing invite on the code claim | `individual` | 250 | 6 | false | none |
| 2C-09: 2-claim enter (code nil, email waiting) racing invite on the code claim | `all-after` | 254 | 4 | false | none |
| 2C-09: 2-claim enter (code nil, email waiting) racing invite on the code claim | `timeout-overlap` | 522 | 6 | false | none |
| 2C-10: returning enter racing own-layer-made | `pure` | 11 | 2 | false | none |
| 2C-10: returning enter racing own-layer-made | `individual` | 24 | 4 | false | none |
| 2C-10: returning enter racing own-layer-made | `all-after` | 37 | 4 | false | none |
| 2C-10: returning enter racing own-layer-made | `timeout-overlap` | 40 | 4 | false | none |
| 2C-11: two persons answering one ask yes with new agents at once | `pure` | 11 | 1 | false | none |
| 2C-11: two persons answering one ask yes with new agents at once | `individual` | 38 | 3 | false | none |
| 2C-11: two persons answering one ask yes with new agents at once | `all-after` | 34 | 2 | false | none |
| 2C-11: two persons answering one ask yes with new agents at once | `timeout-overlap` | 59 | 3 | false | none |
| 2C-12: same person P1 answers ask from two copies with differing clocks (t1=500, t2=2000; G0/S0 expire at 1500) | `pure` | 11 | 1 | false | none |
| 2C-12: same person P1 answers ask from two copies with differing clocks (t1=500, t2=2000; G0/S0 expire at 1500) | `individual` | 37 | 3 | false | none |
| 2C-12: same person P1 answers ask from two copies with differing clocks (t1=500, t2=2000; G0/S0 expire at 1500) | `all-after` | 33 | 2 | false | none |
| 2C-12: same person P1 answers ask from two copies with differing clocks (t1=500, t2=2000; G0/S0 expire at 1500) | `timeout-overlap` | 58 | 3 | false | none |
| 2C-13: answer-ask yes for existing agent A1 racing end-actor(A1) | `pure` | 10 | 2 | false | none |
| 2C-13: answer-ask yes for existing agent A1 racing end-actor(A1) | `individual` | 26 | 4 | false | none |
| 2C-13: answer-ask yes for existing agent A1 racing end-actor(A1) | `all-after` | 40 | 4 | false | none |
| 2C-13: answer-ask yes for existing agent A1 racing end-actor(A1) | `timeout-overlap` | 39 | 4 | false | none |
| 2C-14: two answers straddling ask :ends (P1 at=500 before ends=1000, P2 at=1200 after ends) | `pure` | 11 | 1 | false | none |
| 2C-14: two answers straddling ask :ends (P1 at=500 before ends=1000, P2 at=1200 after ends) | `individual` | 38 | 3 | false | none |
| 2C-14: two answers straddling ask :ends (P1 at=500 before ends=1000, P2 at=1200 after ends) | `all-after` | 34 | 2 | false | none |
| 2C-14: two answers straddling ask :ends (P1 at=500 before ends=1000, P2 at=1200 after ends) | `timeout-overlap` | 59 | 3 | false | none |
| 3C-01: three invites (P1,C1), (P2,C1), and (P1,C2) — refusal order & helping | `pure` | 172 | 4 | false | none |
| 3C-01: three invites (P1,C1), (P2,C1), and (P1,C2) — refusal order & helping | `individual` | 989 | 16 | false | none |
| 3C-01: three invites (P1,C1), (P2,C1), and (P1,C2) — refusal order & helping | `all-after` | 1,104 | 8 | false | none |
| 3C-01: three invites (P1,C1), (P2,C1), and (P1,C2) — refusal order & helping | `timeout-overlap` | 1,799 | 16 | false | none |
| 3C-02: sequential causal chain invite(P2,C1) -> invite(P1,C2) while invite(P1,C1) in flight | `pure` | 66 | 3 | false | none |
| 3C-02: sequential causal chain invite(P2,C1) -> invite(P1,C2) while invite(P1,C1) in flight | `individual` | 331 | 12 | false | none |
| 3C-02: sequential causal chain invite(P2,C1) -> invite(P1,C2) while invite(P1,C1) in flight | `all-after` | 462 | 6 | false | none |
| 3C-02: sequential causal chain invite(P2,C1) -> invite(P1,C2) while invite(P1,C1) in flight | `timeout-overlap` | 591 | 12 | false | none |
| 3C-03: 1-claim enter(Acc1,C1) + 1-claim enter(Acc1,C2) + invite(P1,C1) | `pure` | 197 | 2 | false | none |
| 3C-03: 1-claim enter(Acc1,C1) + 1-claim enter(Acc1,C2) + invite(P1,C1) | `individual` | 1,503 | 9 | false | none |
| 3C-03: 1-claim enter(Acc1,C1) + 1-claim enter(Acc1,C2) + invite(P1,C1) | `all-after` | 1,217 | 4 | false | none |
| 3C-03: 1-claim enter(Acc1,C1) + 1-claim enter(Acc1,C2) + invite(P1,C1) | `timeout-overlap` | 2,786 | 9 | false | none |
| 3C-04: 2-claim enter(Acc1, code=C_CODE, email=C1) + enter(Acc1, C1) + invite(P1, C_CODE) | `pure` | 367 | 2 | false | none |
| 3C-04: 2-claim enter(Acc1, code=C_CODE, email=C1) + enter(Acc1, C1) + invite(P1, C_CODE) | `individual` | 2,457 | 8 | false | none |
| 3C-04: 2-claim enter(Acc1, code=C_CODE, email=C1) + enter(Acc1, C1) + invite(P1, C_CODE) | `all-after` | 1,644 | 4 | false | none |
| 3C-04: 2-claim enter(Acc1, code=C_CODE, email=C1) + enter(Acc1, C1) + invite(P1, C_CODE) | `timeout-overlap` | 5,649 | 8 | false | none |
| 3C-05: 2-claim enter(Acc1) + 1-claim enter(Acc2, C_CODE) + 1-claim enter(Acc3, C_CODE) | `pure` | 1,205 | 3 | false | none |
| 3C-05: 2-claim enter(Acc1) + 1-claim enter(Acc2, C_CODE) + 1-claim enter(Acc3, C_CODE) | `individual` | 8,507 | 12 | false | none |
| 3C-05: 2-claim enter(Acc1) + 1-claim enter(Acc2, C_CODE) + 1-claim enter(Acc3, C_CODE) | `all-after` | 7,899 | 6 | false | none |
| 3C-05: 2-claim enter(Acc1) + 1-claim enter(Acc2, C_CODE) + 1-claim enter(Acc3, C_CODE) | `timeout-overlap` | 24,413 | 12 | false | none |
| 3C-06: Trace E-3 continuation — Acc2 and Acc3 (1-claim & 2-claim) meet stale ClaimClaiming on C_CODE | `pure` | 123 | 2 | false | none |
| 3C-06: Trace E-3 continuation — Acc2 and Acc3 (1-claim & 2-claim) meet stale ClaimClaiming on C_CODE | `individual` | 591 | 6 | false | none |
| 3C-06: Trace E-3 continuation — Acc2 and Acc3 (1-claim & 2-claim) meet stale ClaimClaiming on C_CODE | `all-after` | 564 | 4 | false | none |
| 3C-06: Trace E-3 continuation — Acc2 and Acc3 (1-claim & 2-claim) meet stale ClaimClaiming on C_CODE | `timeout-overlap` | 1,779 | 6 | false | none |
| 2C-15: Trace E-3 continuation — two 2-claim enters (Acc2, Acc3) evicting stale ClaimClaiming (PLAN.md:537) | `pure` | 141 | 2 | false | none |
| 2C-15: Trace E-3 continuation — two 2-claim enters (Acc2, Acc3) evicting stale ClaimClaiming (PLAN.md:537) | `individual` | 717 | 6 | false | none |
| 2C-15: Trace E-3 continuation — two 2-claim enters (Acc2, Acc3) evicting stale ClaimClaiming (PLAN.md:537) | `all-after` | 666 | 4 | false | none |
| 2C-15: Trace E-3 continuation — two 2-claim enters (Acc2, Acc3) evicting stale ClaimClaiming (PLAN.md:537) | `timeout-overlap` | 2,211 | 6 | false | none |
| 3C-08: invite(P1,C1) in flight; invite(P2,C2) returns -> invite(P1,C2) helps C1 without rechecking C2 (PLAN.md:479-480) | `pure` | 35 | 1 | false | none |
| 3C-08: invite(P1,C1) in flight; invite(P2,C2) returns -> invite(P1,C2) helps C1 without rechecking C2 (PLAN.md:479-480) | `individual` | 156 | 4 | false | none |
| 3C-08: invite(P1,C1) in flight; invite(P2,C2) returns -> invite(P1,C2) helps C1 without rechecking C2 (PLAN.md:479-480) | `all-after` | 176 | 2 | false | none |
| 3C-08: invite(P1,C1) in flight; invite(P2,C2) returns -> invite(P1,C2) helps C1 without rechecking C2 (PLAN.md:479-480) | `timeout-overlap` | 226 | 4 | false | none |
| 3C-09: answer-ask yes with new-agent A1 racing make-actor(P1, A1) and second answer-ask | `pure` | 31 | 2 | false | none |
| 3C-09: answer-ask yes with new-agent A1 racing make-actor(P1, A1) and second answer-ask | `individual` | 117 | 7 | false | none |
| 3C-09: answer-ask yes with new-agent A1 racing make-actor(P1, A1) and second answer-ask | `all-after` | 139 | 4 | false | none |
| 3C-09: answer-ask yes with new-agent A1 racing make-actor(P1, A1) and second answer-ask | `timeout-overlap` | 179 | 7 | false | none |
| 3C-07: three answers on one ask — refused answer (:no-such-actor) + yes + no | `pure` | 45 | 2 | false | none |
| 3C-07: three answers on one ask — refused answer (:no-such-actor) + yes + no | `individual` | 266 | 8 | false | none |
| 3C-07: three answers on one ask — refused answer (:no-such-actor) + yes + no | `all-after` | 266 | 4 | false | none |
| 3C-07: three answers on one ask — refused answer (:no-such-actor) + yes + no | `timeout-overlap` | 500 | 9 | false | none |
| X-01: 2-claim enter (code nil, email waiting) while invite(P1, C_CODE) -> Acc2 takes email | `pure` | 175 | 2 | false | none |
| X-01: 2-claim enter (code nil, email waiting) while invite(P1, C_CODE) -> Acc2 takes email | `individual` | 1,000 | 8 | false | none |
| X-01: 2-claim enter (code nil, email waiting) while invite(P1, C_CODE) -> Acc2 takes email | `all-after` | 1,095 | 4 | false | none |
| X-01: 2-claim enter (code nil, email waiting) while invite(P1, C_CODE) -> Acc2 takes email | `timeout-overlap` | 2,343 | 8 | false | none |
| X-02: two 2-claim enters of one account with inverted claim order and differing clocks | `pure` | 48 | 1 | false | none |
| X-02: two 2-claim enters of one account with inverted claim order and differing clocks | `individual` | 250 | 3 | false | none |
| X-02: two 2-claim enters of one account with inverted claim order and differing clocks | `all-after` | 126 | 2 | false | none |
| X-02: two 2-claim enters of one account with inverted claim order and differing clocks | `timeout-overlap` | 518 | 3 | false | none |
| X-03: same person P1 answers ask yes + yes from two copies with differing clocks (t1=500, t2=2000) | `pure` | 11 | 1 | false | none |
| X-03: same person P1 answers ask yes + yes from two copies with differing clocks (t1=500, t2=2000) | `individual` | 37 | 3 | false | none |
| X-03: same person P1 answers ask yes + yes from two copies with differing clocks (t1=500, t2=2000) | `all-after` | 33 | 2 | false | none |
| X-03: same person P1 answers ask yes + yes from two copies with differing clocks (t1=500, t2=2000) | `timeout-overlap` | 58 | 3 | false | none |
| X-04: refused high-:at enter(Acc1, C1=nil, at=300) racing valid enter(Acc1, C2, at=100) | `pure` | 27 | 1 | false | none |
| X-04: refused high-:at enter(Acc1, C1=nil, at=300) racing valid enter(Acc1, C2, at=100) | `individual` | 167 | 3 | false | none |
| X-04: refused high-:at enter(Acc1, C1=nil, at=300) racing valid enter(Acc1, C2, at=100) | `all-after` | 149 | 2 | false | none |
| X-04: refused high-:at enter(Acc1, C1=nil, at=300) racing valid enter(Acc1, C2, at=100) | `timeout-overlap` | 288 | 3 | false | none |
| X-05: answer-ask(U, yes? true) racing ask(U) on same code | `pure` | 5 | 1 | false | none |
| X-05: answer-ask(U, yes? true) racing ask(U) on same code | `individual` | 10 | 2 | false | none |
| X-05: answer-ask(U, yes? true) racing ask(U) on same code | `all-after` | 27 | 3 | false | none |
| X-05: answer-ask(U, yes? true) racing ask(U) on same code | `timeout-overlap` | 13 | 2 | false | none |
| X-06: refused no-claim high-:at enter(Acc1, at=300) racing valid enter(Acc1, C2, at=100) | `pure` | 17 | 1 | false | none |
| X-06: refused no-claim high-:at enter(Acc1, at=300) racing valid enter(Acc1, C2, at=100) | `individual` | 88 | 3 | false | none |
| X-06: refused no-claim high-:at enter(Acc1, at=300) racing valid enter(Acc1, C2, at=100) | `all-after` | 222 | 3 | false | none |
| X-06: refused no-claim high-:at enter(Acc1, at=300) racing valid enter(Acc1, C2, at=100) | `timeout-overlap` | 162 | 3 | false | none |
| X-07: answer-ask(U, existing A1) racing make-actor(P1, A1) | `pure` | 10 | 2 | false | none |
| X-07: answer-ask(U, existing A1) racing make-actor(P1, A1) | `individual` | 26 | 4 | false | none |
| X-07: answer-ask(U, existing A1) racing make-actor(P1, A1) | `all-after` | 41 | 4 | false | none |
| X-07: answer-ask(U, existing A1) racing make-actor(P1, A1) | `timeout-overlap` | 40 | 4 | false | none |
