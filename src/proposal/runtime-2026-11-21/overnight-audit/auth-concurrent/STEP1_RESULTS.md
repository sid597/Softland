# Step 1 — Executable Multi-Task Model of `PLAN.md` (`invite`, `enter`, `answer-ask`)

- **Scenarios × modes run:** 24 scenarios × 4 modes = **96 runs** (4025 ms)
- **Distinct `(state, queues, fault-budget)` configurations explored:** **51,214** (0 capped runs)
- **Quiescent terminal histories checked against all 5 invariants + causal linearizability:** **604**

## Answers to the Five Questions

| # | Question | Broken? | Distinct bug classes | Total scenario×mode hits |
|---|---|---|---|---|
| 1 | Does one account ever get two persons, or one claim two accounts, or one person two claims? | **YES (FAIL)** | `one-account-two-persons`, `one-claim-two-accounts` | 11 |
| 2 | Does one ask ever get two answers that stand? | **NO (0 breaks)** | — | 0 |
| 3 | Is anything left forever in a state that is neither done nor refused? | **YES (FAIL)** | `root-reserving-forever`, `claim-used-without-entered-root`, `claim-claiming-forever`, `entered-root-without-matching-account-link` | 12 |
| 4 | Does a call (or re-run) give a result/state disallowed by SPEC.md + Sid's 2 calls? | **YES (FAIL)** | `final-state-or-says-violates-spec`, `no-valid-causal-linearization-of-results`, `client-rerun-violates-spec` | 24 |
| 5 | After a write returns, can any read still miss its effect? | **YES (FAIL)** | `answer-ask-takeout-missed-on-return`, `enter-account-link-misses-effect` | 6 |

## Whole Witness Histories (Shortest Counterexample per Distinct Bug Class)

### Witness 1: `:q1-uniqueness` / `:one-account-two-persons`

- **Scenario:** 3C-03: 1-claim enter(Acc1,C1) + 1-claim enter(Acc1,C2) + invite(P1,C1)
- **Execution / retry mode:** `:individual`
- **What broke:** Account [:google "sub-1"] got two persons: #{:P1 :P2}
- **Completed call results:**
  - `:c1` `[:enter {:account [:google "sub-1"], :says {"s" "1"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 100}]` (invoked step 0, returned step 5) -> `{:refused :not-invited}`
  - `:c2` `[:enter {:account [:google "sub-1"], :says {"s" "2"}, :email "bob@example.com", :code-hash nil, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 200}]` (invoked step 0, returned step 14) -> `{:person :P2, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :own-layer-made? false}`
  - `:c3` `[:invite {:person :P1, :claim {:by :email, :email "alice@example.com"}, :invited-by :system, :at 150}]` (invoked step 0, returned step 9) -> `{:invited :P1}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:516` | H(Acc=[:google "sub-1"]) unlinked -> writes AccountLinking(op=[:op :c1]), hops to Step 2 |
| 2 | `:c2` (#2) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:519` | H(Acc=[:google "sub-1"]) sees AccountLinking(op0=[:op :c1] != [:op :c2]) -> helps op0 finish Step 2 |
| 3 | `:c3` (#3) | `[:claim {:by :email, :email "alice@example.com"}]` | `:invite/step-1-check-claim` | `PLAN.md:472` | H(C={:by :email, :email "alice@example.com"}) sees $$claims[C]=nil -> hops \|hash P=:P1 to Step 2 |
| 4 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-2-1claim-acquire` | `PLAN.md:526` | H(C={:by :email, :email "alice@example.com"}) 1-claim sees nil -> miss, hops \|hash Acc |
| 5 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-2-miss-cleanup` | `PLAN.md:524,526,542` | H(Acc=[:google "sub-1"]) clears AccountLinking(op=[:op :c1]) if still op -> ack-return {:refused :not-invited} |
| 6 | `:c3` (#3) | `[:root :P1]` | `:invite/step-2-check-or-reserve-root` | `PLAN.md:475` | H(P=:P1) sees r=nil -> writes RootReserving(C={:by :email, :email "alice@example.com"}), hops \|hash C to Step 3 |
| 7 | `:c3` (#3) | `[:claim {:by :email, :email "alice@example.com"}]` | `:invite/step-3-commit-claim` | `PLAN.md:483` | H(C={:by :email, :email "alice@example.com"}) Step 3 sees nil -> commits ClaimWaiting(P=:P1), hops \|hash P to Step 4 |
| 8 | `:c2` (#2) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-2-1claim-acquire` | `PLAN.md:523` | H(C={:by :email, :email "alice@example.com"}) 1-claim fast path sees ClaimWaiting(P=:P1) -> writes ClaimUsed(P, op=[:op :c1]) directly, hops \|hash P to Step 3 |
| 9 | `:c3` (#3) | `[:root :P1]` | `:invite/step-4-promote-root` | `PLAN.md:486` | H(P=:P1) Step 4 ensures RootInvited(C={:by :email, :email "alice@example.com"}) -> ack-return {:invited :P1} |
| 10 | `:c2` (#2) | `[:root :P1]` | `:enter/step-3a-enter-root` | `PLAN.md:544` | H(P=:P1) Step 3a ensures RootEntered (fixed L'=:L1, made?=false) -> hops \|hash Acc |
| 10 | `:c2` (#101) | `[:account [:google "sub-1"]]` | `:FAULT/individual-retry` | `stream.md:94-102` | Crash before :enter/step-3b-link-account on [:account [:google "sub-1"]]; prior hops stay committed; Rama retries :c2 from Step 1 on [:account [:google "sub-1"]] |
| 11 | `:c2` (#101) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:516` | H(Acc=[:google "sub-1"]) unlinked -> writes AccountLinking(op=[:op :c2]), hops to Step 2 |
| 12 | `:c2` (#101) | `[:claim {:by :email, :email "bob@example.com"}]` | `:enter/step-2-1claim-acquire` | `PLAN.md:523` | H(C={:by :email, :email "bob@example.com"}) 1-claim fast path sees ClaimWaiting(P=:P2) -> writes ClaimUsed(P, op=[:op :c2]) directly, hops \|hash P to Step 3 |
| 13 | `:c2` (#101) | `[:root :P2]` | `:enter/step-3a-enter-root` | `PLAN.md:544` | H(P=:P2) Step 3a ensures RootEntered (fixed L'=:L2, made?=false) -> hops \|hash Acc |
| 14 | `:c2` (#101) | `[:account [:google "sub-1"]]` | `:enter/step-3b-link-account` | `PLAN.md:545-546,519` | H(Acc=[:google "sub-1"]) Step 3b finalizes AccountLinked -> ack-return {:person :P2 :own-layer :L2 :own-layer-made? false} |

### Witness 2: `:q1-uniqueness` / `:one-claim-two-accounts`

- **Scenario:** 3C-06: Trace E-3 continuation — Acc2 and Acc3 (1-claim & 2-claim) meet stale ClaimClaiming on C_CODE
- **Execution / retry mode:** `:pure`
- **What broke:** Person/claim for :P1 was taken by two accounts: #{[:google "sub-2"] [:github "sub-3"]}
- **Completed call results:**
  - `:c1` `[:enter {:account [:google "sub-2"], :says {"a" "2"}, :email nil, :code-hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 100}]` (invoked step 0, returned step 12) -> `{:person :P1, :own-layer :L3, :making {:uuid :m-uuid-3, :session :m-sess-3}, :own-layer-made? false}`
  - `:c2` `[:enter {:account [:github "sub-3"], :says {"a" "3"}, :email nil, :code-hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", :own-layer :L3, :making {:uuid :m-uuid-3, :session :m-sess-3}, :at 110}]` (invoked step 0, returned step 11) -> `{:person :P1, :own-layer :L3, :making {:uuid :m-uuid-3, :session :m-sess-3}, :own-layer-made? false}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c2` (#2) | `[:account [:github "sub-3"]]` | `:enter/step-1-check-account` | `PLAN.md:516` | H(Acc=[:github "sub-3"]) unlinked -> writes AccountLinking(op=[:op :c2]), hops to Step 2 |
| 2 | `:c1` (#1) | `[:account [:google "sub-2"]]` | `:enter/step-1-check-account` | `PLAN.md:516` | H(Acc=[:google "sub-2"]) unlinked -> writes AccountLinking(op=[:op :c1]), hops to Step 2 |
| 3 | `:c2` (#2) | `[:claim {:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]` | `:enter/step-2-1claim-acquire` | `PLAN.md:525` | H(C={:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}) 1-claim sees ClaimClaiming(P=:P1, other-op=:op-stale) -> hops \|hash other-op |
| 4 | `:c1` (#1) | `[:claim {:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]` | `:enter/step-2-1claim-acquire` | `PLAN.md:525` | H(C={:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}) 1-claim sees ClaimClaiming(P=:P1, other-op=:op-stale) -> hops \|hash other-op |
| 5 | `:c2` (#2) | `[:op :op-stale]` | `:enter/step-2-1claim-help-op` | `PLAN.md:525,537` | H(other-op=:op-stale) sees :EnterOpWon (other-op aborted :code) -> hops \|hash C to claim |
| 6 | `:c1` (#1) | `[:op :op-stale]` | `:enter/step-2-1claim-help-op` | `PLAN.md:525,537` | H(other-op=:op-stale) sees :EnterOpWon (other-op aborted :code) -> hops \|hash C to claim |
| 7 | `:c2` (#2) | `[:claim {:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]` | `:enter/step-2-1claim-after-help` | `PLAN.md:525` | H(C={:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}) overwrites aborted ClaimClaiming with ClaimUsed(P=:P1, op=[:op :c2]), hops \|hash P to Step 3 |
| 8 | `:c2` (#2) | `[:root :P1]` | `:enter/step-3a-enter-root` | `PLAN.md:544` | H(P=:P1) Step 3a ensures RootEntered (fixed L'=:L3, made?=false) -> hops \|hash Acc |
| 9 | `:c1` (#1) | `[:claim {:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]` | `:enter/step-2-1claim-after-help` | `PLAN.md:525` | H(C={:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}) overwrites aborted ClaimClaiming with ClaimUsed(P=:P1, op=[:op :c1]), hops \|hash P to Step 3 |
| 10 | `:c1` (#1) | `[:root :P1]` | `:enter/step-3a-enter-root` | `PLAN.md:544` | H(P=:P1) Step 3a ensures RootEntered (fixed L'=:L3, made?=false) -> hops \|hash Acc |
| 11 | `:c2` (#2) | `[:account [:github "sub-3"]]` | `:enter/step-3b-link-account` | `PLAN.md:545-546,519` | H(Acc=[:github "sub-3"]) Step 3b finalizes AccountLinked -> ack-return {:person :P1 :own-layer :L3 :own-layer-made? false} |
| 12 | `:c1` (#1) | `[:account [:google "sub-2"]]` | `:enter/step-3b-link-account` | `PLAN.md:545-546,519` | H(Acc=[:google "sub-2"]) Step 3b finalizes AccountLinked -> ack-return {:person :P1 :own-layer :L3 :own-layer-made? false} |

### Witness 3: `:q3-left-unsettled` / `:claim-claiming-forever`

- **Scenario:** 3C-04: 2-claim enter(Acc1, code=C_CODE, email=C1) + enter(Acc1, C1) + invite(P1, C_CODE)
- **Execution / retry mode:** `:individual`
- **What broke:** $$claims[{:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}] left forever in ClaimClaiming: {:state :ClaimClaiming, :person :P1, :invited-by :system, :invited-at 120, :op-id [:op :c1], :phase :code}
- **Completed call results:**
  - `:c1` `[:enter {:account [:google "sub-1"], :says {"s" "1"}, :email "alice@example.com", :code-hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 100}]` (invoked step 0, returned step 14) -> `{:person :P2, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}`
  - `:c2` `[:enter {:account [:google "sub-1"], :says {"s" "2"}, :email "alice@example.com", :code-hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 200}]` (invoked step 0, returned step 16) -> `{:person :P2, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}`
  - `:c3` `[:invite {:person :P1, :claim {:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}, :invited-by :system, :at 120}]` (invoked step 0, returned step 13) -> `{:invited :P1}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:516` | H(Acc=[:google "sub-1"]) unlinked -> writes AccountLinking(op=[:op :c1]), hops to Step 2 |
| 2 | `:c2` (#2) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:519` | H(Acc=[:google "sub-1"]) sees AccountLinking(op0=[:op :c1] != [:op :c2]) -> helps op0 finish Step 2 |
| 3 | `:c3` (#3) | `[:claim {:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]` | `:invite/step-1-check-claim` | `PLAN.md:472` | H(C={:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}) sees $$claims[C]=nil -> hops \|hash P=:P1 to Step 2 |
| 4 | `:c1` (#1) | `[:claim {:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]` | `:enter/step-2-2claim-try-claim` | `PLAN.md:539` | H(C={:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}) 2-claim missed :code -> hops \|hash op=[:op :c1] |
| 5 | `:c1` (#1) | `[:op [:op :c1]]` | `:enter/step-2-2claim-code-missed` | `PLAN.md:539` | H(op=[:op :c1]) commits EnterOpTryEmail -> hops \|hash C_email |
| 6 | `:c3` (#3) | `[:root :P1]` | `:invite/step-2-check-or-reserve-root` | `PLAN.md:475` | H(P=:P1) sees r=nil -> writes RootReserving(C={:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}), hops \|hash C to Step 3 |
| 7 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-2-2claim-try-claim` | `PLAN.md:541` | H(C={:by :email, :email "alice@example.com"}) 2-claim phase=:email sees ClaimWaiting(P=:P2) -> writes ClaimClaiming(P, op=[:op :c1], :email), hops \|hash op |
| 8 | `:c1` (#1) | `[:op [:op :c1]]` | `:enter/step-2-2claim-commit-op` | `PLAN.md:541` | H(op=[:op :c1]) commits EnterOpWon(P=:P2, :email) -> hops \|hash C to promote ClaimUsed |
| 9 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-2-2claim-promote-used` | `PLAN.md:531-532` | H(C={:by :email, :email "alice@example.com"}) promotes to ClaimUsed(P=:P2, op=[:op :c1]) -> hops \|hash P to Step 3 |
| 10 | `:c1` (#1) | `[:root :P2]` | `:enter/step-3a-enter-root` | `PLAN.md:544` | H(P=:P2) Step 3a ensures RootEntered (fixed L'=:L1, made?=false) -> hops \|hash Acc |
| 11 | `:c3` (#3) | `[:claim {:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]` | `:invite/step-3-commit-claim` | `PLAN.md:483` | H(C={:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}) Step 3 sees nil -> commits ClaimWaiting(P=:P1), hops \|hash P to Step 4 |
| 12 | `:c2` (#2) | `[:claim {:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]` | `:enter/step-2-2claim-try-claim` | `PLAN.md:530` | H(C={:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}) 2-claim phase=:code sees ClaimWaiting(P=:P1) -> writes ClaimClaiming(P, op=[:op :c1], :code), hops \|hash op |
| 13 | `:c3` (#3) | `[:root :P1]` | `:invite/step-4-promote-root` | `PLAN.md:486` | H(P=:P1) Step 4 ensures RootInvited(C={:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}) -> ack-return {:invited :P1} |
| 14 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-3b-link-account` | `PLAN.md:545-546,519` | H(Acc=[:google "sub-1"]) Step 3b finalizes AccountLinked -> ack-return {:person :P2 :own-layer :L1 :own-layer-made? false} |
| 14 | `:c2` (#101) | `[:op [:op :c1]]` | `:FAULT/individual-retry` | `stream.md:94-102` | Crash before :enter/step-2-2claim-commit-op on [:op [:op :c1]]; prior hops stay committed; Rama retries :c2 from Step 1 on [:account [:google "sub-1"]] |
| 15 | `:c2` (#101) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:513` | H(Acc=[:google "sub-1"]) sees AccountLinked(P=:P2) -> overwrites :says/:says-at (at=200), hops \|hash P for :own-layer-made? |
| 16 | `:c2` (#101) | `[:root :P2]` | `:enter/step-1b-returning-read-flag` | `PLAN.md:513` | H(P=:P2) reads :own-layer-made?=false -> ack-return {:person :P2 ...} |

### Witness 4: `:q3-left-unsettled` / `:claim-used-without-entered-root`

- **Scenario:** 3C-03: 1-claim enter(Acc1,C1) + 1-claim enter(Acc1,C2) + invite(P1,C1)
- **Execution / retry mode:** `:individual`
- **What broke:** Cross-partition half-state on claim: [:claim-used-without-entered-root {:by :email, :email "alice@example.com"} {:state :ClaimUsed, :person :P1, :invited-by :system, :invited-at 150, :used-by-op [:op :c1]} {:state :RootInvited, :claim {:by :email, :email "alice@example.com"}, :invited-by :system, :invited-at 150}]
- **Completed call results:**
  - `:c1` `[:enter {:account [:google "sub-1"], :says {"s" "1"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 100}]` (invoked step 0, returned step 5) -> `{:refused :not-invited}`
  - `:c2` `[:enter {:account [:google "sub-1"], :says {"s" "2"}, :email "bob@example.com", :code-hash nil, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 200}]` (invoked step 0, returned step 13) -> `{:person :P2, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :own-layer-made? false}`
  - `:c3` `[:invite {:person :P1, :claim {:by :email, :email "alice@example.com"}, :invited-by :system, :at 150}]` (invoked step 0, returned step 9) -> `{:invited :P1}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:516` | H(Acc=[:google "sub-1"]) unlinked -> writes AccountLinking(op=[:op :c1]), hops to Step 2 |
| 2 | `:c2` (#2) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:519` | H(Acc=[:google "sub-1"]) sees AccountLinking(op0=[:op :c1] != [:op :c2]) -> helps op0 finish Step 2 |
| 3 | `:c3` (#3) | `[:claim {:by :email, :email "alice@example.com"}]` | `:invite/step-1-check-claim` | `PLAN.md:472` | H(C={:by :email, :email "alice@example.com"}) sees $$claims[C]=nil -> hops \|hash P=:P1 to Step 2 |
| 4 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-2-1claim-acquire` | `PLAN.md:526` | H(C={:by :email, :email "alice@example.com"}) 1-claim sees nil -> miss, hops \|hash Acc |
| 5 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-2-miss-cleanup` | `PLAN.md:524,526,542` | H(Acc=[:google "sub-1"]) clears AccountLinking(op=[:op :c1]) if still op -> ack-return {:refused :not-invited} |
| 6 | `:c3` (#3) | `[:root :P1]` | `:invite/step-2-check-or-reserve-root` | `PLAN.md:475` | H(P=:P1) sees r=nil -> writes RootReserving(C={:by :email, :email "alice@example.com"}), hops \|hash C to Step 3 |
| 7 | `:c3` (#3) | `[:claim {:by :email, :email "alice@example.com"}]` | `:invite/step-3-commit-claim` | `PLAN.md:483` | H(C={:by :email, :email "alice@example.com"}) Step 3 sees nil -> commits ClaimWaiting(P=:P1), hops \|hash P to Step 4 |
| 8 | `:c2` (#2) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-2-1claim-acquire` | `PLAN.md:523` | H(C={:by :email, :email "alice@example.com"}) 1-claim fast path sees ClaimWaiting(P=:P1) -> writes ClaimUsed(P, op=[:op :c1]) directly, hops \|hash P to Step 3 |
| 9 | `:c3` (#3) | `[:root :P1]` | `:invite/step-4-promote-root` | `PLAN.md:486` | H(P=:P1) Step 4 ensures RootInvited(C={:by :email, :email "alice@example.com"}) -> ack-return {:invited :P1} |
| 9 | `:c2` (#101) | `[:root :P1]` | `:FAULT/individual-retry` | `stream.md:94-102` | Crash before :enter/step-3a-enter-root on [:root :P1]; prior hops stay committed; Rama retries :c2 from Step 1 on [:account [:google "sub-1"]] |
| 10 | `:c2` (#101) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:516` | H(Acc=[:google "sub-1"]) unlinked -> writes AccountLinking(op=[:op :c2]), hops to Step 2 |
| 11 | `:c2` (#101) | `[:claim {:by :email, :email "bob@example.com"}]` | `:enter/step-2-1claim-acquire` | `PLAN.md:523` | H(C={:by :email, :email "bob@example.com"}) 1-claim fast path sees ClaimWaiting(P=:P2) -> writes ClaimUsed(P, op=[:op :c2]) directly, hops \|hash P to Step 3 |
| 12 | `:c2` (#101) | `[:root :P2]` | `:enter/step-3a-enter-root` | `PLAN.md:544` | H(P=:P2) Step 3a ensures RootEntered (fixed L'=:L2, made?=false) -> hops \|hash Acc |
| 13 | `:c2` (#101) | `[:account [:google "sub-1"]]` | `:enter/step-3b-link-account` | `PLAN.md:545-546,519` | H(Acc=[:google "sub-1"]) Step 3b finalizes AccountLinked -> ack-return {:person :P2 :own-layer :L2 :own-layer-made? false} |

### Witness 5: `:q3-left-unsettled` / `:entered-root-without-matching-account-link`

- **Scenario:** 3C-06: Trace E-3 continuation — Acc2 and Acc3 (1-claim & 2-claim) meet stale ClaimClaiming on C_CODE
- **Execution / retry mode:** `:individual`
- **What broke:** Cross-partition half-state on entered person: [:entered-root-without-matching-account-link :P1 {:own-layer :L3, :next-sign-in-seq 0, :invited-at 10, :next-actor-seq 0, :own-layer-made? false, :invited-by :system, :state :RootEntered, :linked-account [:github "sub-3"], :making {:uuid :m-uuid-3, :session :m-sess-3}, :claim {:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}} nil]
- **Completed call results:**
  - `:c1` `[:enter {:account [:google "sub-2"], :says {"a" "2"}, :email nil, :code-hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 100}]` (invoked step 0, returned step 11) -> `{:person :P1, :own-layer :L3, :making {:uuid :m-uuid-3, :session :m-sess-3}, :own-layer-made? false}`
  - `:c2` `[:enter {:account [:github "sub-3"], :says {"a" "3"}, :email nil, :code-hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", :own-layer :L3, :making {:uuid :m-uuid-3, :session :m-sess-3}, :at 110}]` (invoked step 0, returned step 14) -> `{:refused :not-invited}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c2` (#2) | `[:account [:github "sub-3"]]` | `:enter/step-1-check-account` | `PLAN.md:516` | H(Acc=[:github "sub-3"]) unlinked -> writes AccountLinking(op=[:op :c2]), hops to Step 2 |
| 2 | `:c1` (#1) | `[:account [:google "sub-2"]]` | `:enter/step-1-check-account` | `PLAN.md:516` | H(Acc=[:google "sub-2"]) unlinked -> writes AccountLinking(op=[:op :c1]), hops to Step 2 |
| 3 | `:c2` (#2) | `[:claim {:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]` | `:enter/step-2-1claim-acquire` | `PLAN.md:525` | H(C={:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}) 1-claim sees ClaimClaiming(P=:P1, other-op=:op-stale) -> hops \|hash other-op |
| 4 | `:c1` (#1) | `[:claim {:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]` | `:enter/step-2-1claim-acquire` | `PLAN.md:525` | H(C={:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}) 1-claim sees ClaimClaiming(P=:P1, other-op=:op-stale) -> hops \|hash other-op |
| 5 | `:c2` (#2) | `[:op :op-stale]` | `:enter/step-2-1claim-help-op` | `PLAN.md:525,537` | H(other-op=:op-stale) sees :EnterOpWon (other-op aborted :code) -> hops \|hash C to claim |
| 6 | `:c1` (#1) | `[:op :op-stale]` | `:enter/step-2-1claim-help-op` | `PLAN.md:525,537` | H(other-op=:op-stale) sees :EnterOpWon (other-op aborted :code) -> hops \|hash C to claim |
| 7 | `:c2` (#2) | `[:claim {:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]` | `:enter/step-2-1claim-after-help` | `PLAN.md:525` | H(C={:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}) overwrites aborted ClaimClaiming with ClaimUsed(P=:P1, op=[:op :c2]), hops \|hash P to Step 3 |
| 8 | `:c2` (#2) | `[:root :P1]` | `:enter/step-3a-enter-root` | `PLAN.md:544` | H(P=:P1) Step 3a ensures RootEntered (fixed L'=:L3, made?=false) -> hops \|hash Acc |
| 9 | `:c1` (#1) | `[:claim {:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]` | `:enter/step-2-1claim-after-help` | `PLAN.md:525` | H(C={:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}) overwrites aborted ClaimClaiming with ClaimUsed(P=:P1, op=[:op :c1]), hops \|hash P to Step 3 |
| 10 | `:c1` (#1) | `[:root :P1]` | `:enter/step-3a-enter-root` | `PLAN.md:544` | H(P=:P1) Step 3a ensures RootEntered (fixed L'=:L3, made?=false) -> hops \|hash Acc |
| 11 | `:c1` (#1) | `[:account [:google "sub-2"]]` | `:enter/step-3b-link-account` | `PLAN.md:545-546,519` | H(Acc=[:google "sub-2"]) Step 3b finalizes AccountLinked -> ack-return {:person :P1 :own-layer :L3 :own-layer-made? false} |
| 11 | `:c2` (#101) | `[:account [:github "sub-3"]]` | `:FAULT/individual-retry` | `stream.md:94-102` | Crash before :enter/step-3b-link-account on [:account [:github "sub-3"]]; prior hops stay committed; Rama retries :c2 from Step 1 on [:account [:github "sub-3"]] |
| 12 | `:c2` (#101) | `[:account [:github "sub-3"]]` | `:enter/step-1-check-account` | `PLAN.md:518` | H(Acc=[:github "sub-3"]) sees own AccountLinking(op=[:op :c2]) -> continues to Step 2 |
| 13 | `:c2` (#101) | `[:claim {:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]` | `:enter/step-2-1claim-acquire` | `PLAN.md:524` | H(C={:by :code, :hash "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}) 1-claim sees ClaimUsed by other op=[:op :c1] -> miss, hops \|hash Acc |
| 14 | `:c2` (#101) | `[:account [:github "sub-3"]]` | `:enter/step-2-miss-cleanup` | `PLAN.md:524,526,542` | H(Acc=[:github "sub-3"]) clears AccountLinking(op=[:op :c2]) if still op -> ack-return {:refused :not-invited} |

### Witness 6: `:q3-left-unsettled` / `:root-reserving-forever`

- **Scenario:** 2C-01: two invites of different persons on one claim
- **Execution / retry mode:** `:individual`
- **What broke:** $$root-data[:P2] left forever in RootReserving: {:state :RootReserving, :claim {:by :email, :email "alice@example.com"}, :invited-by :system, :invited-at 200}
- **Completed call results:**
  - `:c1` `[:invite {:person :P1, :claim {:by :email, :email "alice@example.com"}, :invited-by :system, :at 100}]` (invoked step 0, returned step 6) -> `{:invited :P1}`
  - `:c2` `[:invite {:person :P2, :claim {:by :email, :email "alice@example.com"}, :invited-by :system, :at 200}]` (invoked step 0, returned step 7) -> `{:refused :claim-in-use}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:invite/step-1-check-claim` | `PLAN.md:472` | H(C={:by :email, :email "alice@example.com"}) sees $$claims[C]=nil -> hops \|hash P=:P1 to Step 2 |
| 2 | `:c2` (#2) | `[:claim {:by :email, :email "alice@example.com"}]` | `:invite/step-1-check-claim` | `PLAN.md:472` | H(C={:by :email, :email "alice@example.com"}) sees $$claims[C]=nil -> hops \|hash P=:P2 to Step 2 |
| 3 | `:c1` (#1) | `[:root :P1]` | `:invite/step-2-check-or-reserve-root` | `PLAN.md:475` | H(P=:P1) sees r=nil -> writes RootReserving(C={:by :email, :email "alice@example.com"}), hops \|hash C to Step 3 |
| 4 | `:c2` (#2) | `[:root :P2]` | `:invite/step-2-check-or-reserve-root` | `PLAN.md:475` | H(P=:P2) sees r=nil -> writes RootReserving(C={:by :email, :email "alice@example.com"}), hops \|hash C to Step 3 |
| 5 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:invite/step-3-commit-claim` | `PLAN.md:483` | H(C={:by :email, :email "alice@example.com"}) Step 3 sees nil -> commits ClaimWaiting(P=:P1), hops \|hash P to Step 4 |
| 6 | `:c1` (#1) | `[:root :P1]` | `:invite/step-4-promote-root` | `PLAN.md:486` | H(P=:P1) Step 4 ensures RootInvited(C={:by :email, :email "alice@example.com"}) -> ack-return {:invited :P1} |
| 6 | `:c2` (#101) | `[:claim {:by :email, :email "alice@example.com"}]` | `:FAULT/individual-retry` | `stream.md:94-102` | Crash before :invite/step-3-commit-claim on [:claim {:by :email, :email "alice@example.com"}]; prior hops stay committed; Rama retries :c2 from Step 1 on [:claim {:by :email, :email "alice@example.com"}] |
| 7 | `:c2` (#101) | `[:claim {:by :email, :email "alice@example.com"}]` | `:invite/step-1-check-claim` | `PLAN.md:472` | H(C={:by :email, :email "alice@example.com"}) sees $$claims[C].person=:P1 != :P2 -> immediate 0-hop ack-return {:refused :claim-in-use} |

### Witness 7: `:q4-disallowed-result` / `:client-rerun-violates-spec`

- **Scenario:** 3C-03: 1-claim enter(Acc1,C1) + 1-claim enter(Acc1,C2) + invite(P1,C1)
- **Execution / retry mode:** `:pure`
- **What broke:** Re-running :c1 [:enter {:account [:google "sub-1"], :says {"s" "1"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 100}] (which originally gave {:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}) returned {:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false} / left state disallowed by SPEC.md + Sid's 2 calls
- **Completed call results:**
  - `:c1` `[:enter {:account [:google "sub-1"], :says {"s" "1"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 100}]` (invoked step 0, returned step 8) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}`
  - `:c2` `[:enter {:account [:google "sub-1"], :says {"s" "2"}, :email "bob@example.com", :code-hash nil, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 200}]` (invoked step 0, returned step 10) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}`
  - `:c3` `[:invite {:person :P1, :claim {:by :email, :email "alice@example.com"}, :invited-by :system, :at 150}]` (invoked step 0, returned step 6) -> `{:invited :P1}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:516` | H(Acc=[:google "sub-1"]) unlinked -> writes AccountLinking(op=[:op :c1]), hops to Step 2 |
| 2 | `:c3` (#3) | `[:claim {:by :email, :email "alice@example.com"}]` | `:invite/step-1-check-claim` | `PLAN.md:472` | H(C={:by :email, :email "alice@example.com"}) sees $$claims[C]=nil -> hops \|hash P=:P1 to Step 2 |
| 3 | `:c3` (#3) | `[:root :P1]` | `:invite/step-2-check-or-reserve-root` | `PLAN.md:475` | H(P=:P1) sees r=nil -> writes RootReserving(C={:by :email, :email "alice@example.com"}), hops \|hash C to Step 3 |
| 4 | `:c3` (#3) | `[:claim {:by :email, :email "alice@example.com"}]` | `:invite/step-3-commit-claim` | `PLAN.md:483` | H(C={:by :email, :email "alice@example.com"}) Step 3 sees nil -> commits ClaimWaiting(P=:P1), hops \|hash P to Step 4 |
| 5 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-2-1claim-acquire` | `PLAN.md:523` | H(C={:by :email, :email "alice@example.com"}) 1-claim fast path sees ClaimWaiting(P=:P1) -> writes ClaimUsed(P, op=[:op :c1]) directly, hops \|hash P to Step 3 |
| 6 | `:c3` (#3) | `[:root :P1]` | `:invite/step-4-promote-root` | `PLAN.md:486` | H(P=:P1) Step 4 ensures RootInvited(C={:by :email, :email "alice@example.com"}) -> ack-return {:invited :P1} |
| 7 | `:c1` (#1) | `[:root :P1]` | `:enter/step-3a-enter-root` | `PLAN.md:544` | H(P=:P1) Step 3a ensures RootEntered (fixed L'=:L1, made?=false) -> hops \|hash Acc |
| 8 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-3b-link-account` | `PLAN.md:545-546,519` | H(Acc=[:google "sub-1"]) Step 3b finalizes AccountLinked -> ack-return {:person :P1 :own-layer :L1 :own-layer-made? false} |
| 9 | `:c2` (#2) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:513` | H(Acc=[:google "sub-1"]) sees AccountLinked(P=:P1) -> overwrites :says/:says-at (at=200), hops \|hash P for :own-layer-made? |
| 10 | `:c2` (#2) | `[:root :P1]` | `:enter/step-1b-returning-read-flag` | `PLAN.md:513` | H(P=:P1) reads :own-layer-made?=false -> ack-return {:person :P1 ...} |

**Follow-up re-run trace:**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `[:rerun :c1]` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:513` | H(Acc=[:google "sub-1"]) sees AccountLinked(P=:P1) -> overwrites :says/:says-at (at=100), hops \|hash P for :own-layer-made? |
| 2 | `[:rerun :c1]` (#1) | `[:root :P1]` | `:enter/step-1b-returning-read-flag` | `PLAN.md:513` | H(P=:P1) reads :own-layer-made?=false -> ack-return {:person :P1 ...} |

### Witness 8: `:q4-disallowed-result` / `:final-state-or-says-violates-spec`

- **Scenario:** 2C-06: two returning enters of linked account with differing clocks (t1=300 vs t2=100)
- **Execution / retry mode:** `:pure`
- **What broke:** Final PState state (e.g. :says from latest :at, or live take-out) disagrees with every causal linearization: accounts={[:google "sub-1"] {:state :AccountLinked, :person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :says {"s" "older-100"}, :says-at 100}}
- **Completed call results:**
  - `:c1` `[:enter {:account [:google "sub-1"], :says {"s" "newer-300"}, :email nil, :code-hash nil, :own-layer :L9, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 300}]` (invoked step 0, returned step 3) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}`
  - `:c2` `[:enter {:account [:google "sub-1"], :says {"s" "older-100"}, :email nil, :code-hash nil, :own-layer :L8, :making {:uuid :m-uuid-3, :session :m-sess-3}, :at 100}]` (invoked step 0, returned step 4) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:513` | H(Acc=[:google "sub-1"]) sees AccountLinked(P=:P1) -> overwrites :says/:says-at (at=300), hops \|hash P for :own-layer-made? |
| 2 | `:c2` (#2) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:513` | H(Acc=[:google "sub-1"]) sees AccountLinked(P=:P1) -> overwrites :says/:says-at (at=100), hops \|hash P for :own-layer-made? |
| 3 | `:c1` (#1) | `[:root :P1]` | `:enter/step-1b-returning-read-flag` | `PLAN.md:513` | H(P=:P1) reads :own-layer-made?=false -> ack-return {:person :P1 ...} |
| 4 | `:c2` (#2) | `[:root :P1]` | `:enter/step-1b-returning-read-flag` | `PLAN.md:513` | H(P=:P1) reads :own-layer-made?=false -> ack-return {:person :P1 ...} |

### Witness 9: `:q4-disallowed-result` / `:no-valid-causal-linearization-of-results`

- **Scenario:** 3C-02: sequential causal chain invite(P2,C1) -> invite(P1,C2) while invite(P1,C1) in flight
- **Execution / retry mode:** `:pure`
- **What broke:** Returned results cannot occur in ANY causal order under SPEC.md + Sid's 2 calls: [{:id :c2, :call [:invite {:person :P2, :claim {:by :email, :email "alice@example.com"}, :invited-by :system, :at 110}], :invoked-at-step 0, :returned-at-step 5, :result {:invited :P2}} {:id :c3, :call [:invite {:person :P1, :claim {:by :email, :email "bob@example.com"}, :invited-by :system, :at 120}], :invoked-at-step 5, :returned-at-step 9, :result {:invited :P1}} {:id :c1, :call [:invite {:person :P1, :claim {:by :email, :email "alice@example.com"}, :invited-by :system, :at 100}], :invoked-at-step 0, :returned-at-step 10, :result {:refused :person-in-use}}]
- **Completed call results:**
  - `:c1` `[:invite {:person :P1, :claim {:by :email, :email "alice@example.com"}, :invited-by :system, :at 100}]` (invoked step 0, returned step 10) -> `{:refused :person-in-use}`
  - `:c2` `[:invite {:person :P2, :claim {:by :email, :email "alice@example.com"}, :invited-by :system, :at 110}]` (invoked step 0, returned step 5) -> `{:invited :P2}`
  - `:c3` `[:invite {:person :P1, :claim {:by :email, :email "bob@example.com"}, :invited-by :system, :at 120}]` (invoked step 5, returned step 9) -> `{:invited :P1}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:invite/step-1-check-claim` | `PLAN.md:472` | H(C={:by :email, :email "alice@example.com"}) sees $$claims[C]=nil -> hops \|hash P=:P1 to Step 2 |
| 2 | `:c2` (#2) | `[:claim {:by :email, :email "alice@example.com"}]` | `:invite/step-1-check-claim` | `PLAN.md:472` | H(C={:by :email, :email "alice@example.com"}) sees $$claims[C]=nil -> hops \|hash P=:P2 to Step 2 |
| 3 | `:c2` (#2) | `[:root :P2]` | `:invite/step-2-check-or-reserve-root` | `PLAN.md:475` | H(P=:P2) sees r=nil -> writes RootReserving(C={:by :email, :email "alice@example.com"}), hops \|hash C to Step 3 |
| 4 | `:c2` (#2) | `[:claim {:by :email, :email "alice@example.com"}]` | `:invite/step-3-commit-claim` | `PLAN.md:483` | H(C={:by :email, :email "alice@example.com"}) Step 3 sees nil -> commits ClaimWaiting(P=:P2), hops \|hash P to Step 4 |
| 5 | `:c2` (#2) | `[:root :P2]` | `:invite/step-4-promote-root` | `PLAN.md:486` | H(P=:P2) Step 4 ensures RootInvited(C={:by :email, :email "alice@example.com"}) -> ack-return {:invited :P2} |
| 6 | `:c3` (#3) | `[:claim {:by :email, :email "bob@example.com"}]` | `:invite/step-1-check-claim` | `PLAN.md:472` | H(C={:by :email, :email "bob@example.com"}) sees $$claims[C]=nil -> hops \|hash P=:P1 to Step 2 |
| 7 | `:c3` (#3) | `[:root :P1]` | `:invite/step-2-check-or-reserve-root` | `PLAN.md:475` | H(P=:P1) sees r=nil -> writes RootReserving(C={:by :email, :email "bob@example.com"}), hops \|hash C to Step 3 |
| 8 | `:c3` (#3) | `[:claim {:by :email, :email "bob@example.com"}]` | `:invite/step-3-commit-claim` | `PLAN.md:483` | H(C={:by :email, :email "bob@example.com"}) Step 3 sees nil -> commits ClaimWaiting(P=:P1), hops \|hash P to Step 4 |
| 9 | `:c3` (#3) | `[:root :P1]` | `:invite/step-4-promote-root` | `PLAN.md:486` | H(P=:P1) Step 4 ensures RootInvited(C={:by :email, :email "bob@example.com"}) -> ack-return {:invited :P1} |
| 10 | `:c1` (#1) | `[:root :P1]` | `:invite/step-2-check-or-reserve-root` | `PLAN.md:474` | H(P=:P1) sees :RootInvited with other claim {:by :email, :email "bob@example.com"} != {:by :email, :email "alice@example.com"} -> ack-return {:refused :person-in-use} |

### Witness 10: `:q5-read-misses-effect` / `:answer-ask-takeout-missed-on-return`

- **Scenario:** 2C-12: same person P1 answers ask from two copies with differing clocks (t1=500, t2=2000; G0/S0 expire at 1500)
- **Execution / retry mode:** `:pure`
- **What broke:** answer-ask (root=:P1, at=2000) returned {:yes {:root :P1, :actor :P1, :sign-in :G1}}, but reads (watch-root / under / who-is) still see expired live items: {:stale-sign-in [:G0 {:actor :P1, :kind :browser, :hash "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc", :until 1500, :previous nil}], :stale-session [:S0 {:sign-in :G0, :until 1500}]}
- **Completed call results:**
  - `:c1` `[:answer-ask {:code "ASK00002", :yes? true, :root :P1, :actor :P1, :new-agent nil, :sign-in :G1, :until 5000, :at 500}]` (invoked step 0, returned step 5) -> `{:yes {:root :P1, :actor :P1, :sign-in :G1}}`
  - `:c2` `[:answer-ask {:code "ASK00002", :yes? false, :root :P1, :actor :P1, :new-agent nil, :sign-in :G2, :until 5000, :at 2000}]` (invoked step 0, returned step 6) -> `{:yes {:root :P1, :actor :P1, :sign-in :G1}}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c1` (#1) | `[:code "ASK00002"]` | `:answer-ask/step-1-check-ask` | `PLAN.md:585` | H(U=ASK00002) writes AskAnswering(op=[:op :c1], P=:P1) -> hops \|hash P to Step 2 |
| 2 | `:c2` (#2) | `[:code "ASK00002"]` | `:answer-ask/step-1-check-ask` | `PLAN.md:588` | H(U=ASK00002) sees competing AskAnswering(op0=[:op :c1], P0=:P1) -> hops \|hash P0 to help op0 |
| 3 | `:c1` (#1) | `[:root :P1]` | `:answer-ask/step-2-eval-on-root` | `PLAN.md:592-604` | H(P=:P1) take-out at 500 + records ok:[:op :c1]=:AskOutcomeYes -> hops \|hash U=ASK00002 to Step 3 |
| 4 | `:c2` (#2) | `[:root :P1]` | `:answer-ask/step-2-eval-on-root` | `PLAN.md:592-593` | H(P=:P1) take-out at 500 + replays cached ok:[:op :c1]=:AskOutcomeYes -> hops \|hash U=ASK00002 to Step 3 |
| 5 | `:c1` (#1) | `[:code "ASK00002"]` | `:answer-ask/step-3-finalize-ask` | `PLAN.md:607` | H(U=ASK00002) commits AskYes -> ack-return {:yes {:root :P1, :actor :P1, :sign-in :G1}} |
| 6 | `:c2` (#2) | `[:code "ASK00002"]` | `:answer-ask/step-3-finalize-ask` | `PLAN.md:607,589` | H(U=ASK00002) commits AskYes (helper P==P0=:P1, skips hop per PLAN.md:589) -> ack-return {:yes {:root :P1, :actor :P1, :sign-in :G1}} |

### Witness 11: `:q5-read-misses-effect` / `:enter-account-link-misses-effect`

- **Scenario:** 3C-03: 1-claim enter(Acc1,C1) + 1-claim enter(Acc1,C2) + invite(P1,C1)
- **Execution / retry mode:** `:individual`
- **What broke:** enter returned {:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false} for [:google "sub-1"] but $$accounts holds {:own-layer :L2, :email-claim {:by :email, :email "bob@example.com"}, :says {"s" "2"}, :state :AccountLinking, :account [:google "sub-1"], :code-claim nil, :at 200, :op-id [:op :c2], :making {:uuid :m-uuid-2, :session :m-sess-2}}
- **Completed call results:**
  - `:c1` `[:enter {:account [:google "sub-1"], :says {"s" "1"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 100}]` (invoked step 0, returned step 14) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}`
  - `:c2` `[:enter {:account [:google "sub-1"], :says {"s" "2"}, :email "bob@example.com", :code-hash nil, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 200}]` (invoked step 0, returned step 15) -> `{:person :P2, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :own-layer-made? false}`
  - `:c3` `[:invite {:person :P1, :claim {:by :email, :email "alice@example.com"}, :invited-by :system, :at 150}]` (invoked step 0, returned step 12) -> `{:invited :P1}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:516` | H(Acc=[:google "sub-1"]) unlinked -> writes AccountLinking(op=[:op :c1]), hops to Step 2 |
| 2 | `:c2` (#2) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:519` | H(Acc=[:google "sub-1"]) sees AccountLinking(op0=[:op :c1] != [:op :c2]) -> helps op0 finish Step 2 |
| 3 | `:c3` (#3) | `[:claim {:by :email, :email "alice@example.com"}]` | `:invite/step-1-check-claim` | `PLAN.md:472` | H(C={:by :email, :email "alice@example.com"}) sees $$claims[C]=nil -> hops \|hash P=:P1 to Step 2 |
| 4 | `:c3` (#3) | `[:root :P1]` | `:invite/step-2-check-or-reserve-root` | `PLAN.md:475` | H(P=:P1) sees r=nil -> writes RootReserving(C={:by :email, :email "alice@example.com"}), hops \|hash C to Step 3 |
| 4 | `:c1` (#101) | `[:claim {:by :email, :email "alice@example.com"}]` | `:FAULT/individual-retry` | `stream.md:94-102` | Crash before :enter/step-2-1claim-acquire on [:claim {:by :email, :email "alice@example.com"}]; prior hops stay committed; Rama retries :c1 from Step 1 on [:account [:google "sub-1"]] |
| 5 | `:c1` (#101) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:518` | H(Acc=[:google "sub-1"]) sees own AccountLinking(op=[:op :c1]) -> continues to Step 2 |
| 6 | `:c2` (#2) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-2-1claim-acquire` | `PLAN.md:526` | H(C={:by :email, :email "alice@example.com"}) 1-claim sees nil -> miss, hops \|hash Acc |
| 7 | `:c2` (#2) | `[:account [:google "sub-1"]]` | `:enter/step-2-miss-cleanup` | `PLAN.md:516` | H(Acc=[:google "sub-1"]) cleared failed helped op0=[:op :c1] and looped to Step 1: H(Acc=[:google "sub-1"]) unlinked -> writes AccountLinking(op=[:op :c2]), hops to Step 2 |
| 8 | `:c2` (#2) | `[:claim {:by :email, :email "bob@example.com"}]` | `:enter/step-2-1claim-acquire` | `PLAN.md:523` | H(C={:by :email, :email "bob@example.com"}) 1-claim fast path sees ClaimWaiting(P=:P2) -> writes ClaimUsed(P, op=[:op :c2]) directly, hops \|hash P to Step 3 |
| 9 | `:c2` (#2) | `[:root :P2]` | `:enter/step-3a-enter-root` | `PLAN.md:544` | H(P=:P2) Step 3a ensures RootEntered (fixed L'=:L2, made?=false) -> hops \|hash Acc |
| 10 | `:c3` (#3) | `[:claim {:by :email, :email "alice@example.com"}]` | `:invite/step-3-commit-claim` | `PLAN.md:483` | H(C={:by :email, :email "alice@example.com"}) Step 3 sees nil -> commits ClaimWaiting(P=:P1), hops \|hash P to Step 4 |
| 11 | `:c1` (#101) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-2-1claim-acquire` | `PLAN.md:523` | H(C={:by :email, :email "alice@example.com"}) 1-claim fast path sees ClaimWaiting(P=:P1) -> writes ClaimUsed(P, op=[:op :c1]) directly, hops \|hash P to Step 3 |
| 12 | `:c3` (#3) | `[:root :P1]` | `:invite/step-4-promote-root` | `PLAN.md:486` | H(P=:P1) Step 4 ensures RootInvited(C={:by :email, :email "alice@example.com"}) -> ack-return {:invited :P1} |
| 13 | `:c1` (#101) | `[:root :P1]` | `:enter/step-3a-enter-root` | `PLAN.md:544` | H(P=:P1) Step 3a ensures RootEntered (fixed L'=:L1, made?=false) -> hops \|hash Acc |
| 14 | `:c1` (#101) | `[:account [:google "sub-1"]]` | `:enter/step-3b-link-account` | `PLAN.md:545-546,519` | H(Acc=[:google "sub-1"]) Step 3b finalizes AccountLinked -> ack-return {:person :P1 :own-layer :L1 :own-layer-made? false} |
| 15 | `:c2` (#2) | `[:account [:google "sub-1"]]` | `:enter/step-3b-link-account` | `PLAN.md:545-546,519` | H(Acc=[:google "sub-1"]) Step 3b finalizes AccountLinked -> ack-return {:person :P2 :own-layer :L2 :own-layer-made? false} |

## All Scenario × Mode Runs

| Scenario | Mode | States | Terminal histories | Capped? | Violations |
|---|---|---|---|---|---|
| 2C-01: two invites of different persons on one claim | `pure` | 27 | 3 | false | none |
| 2C-01: two invites of different persons on one claim | `individual` | 113 | 12 | false | `root-reserving-forever` |
| 2C-01: two invites of different persons on one claim | `all-after` | 121 | 9 | false | `root-reserving-forever` |
| 2C-01: two invites of different persons on one claim | `timeout-overlap` | 184 | 8 | false | none |
| 2C-02: two invites of one person under two claims | `pure` | 28 | 2 | false | none |
| 2C-02: two invites of one person under two claims | `individual` | 118 | 6 | false | none |
| 2C-02: two invites of one person under two claims | `all-after` | 125 | 4 | false | none |
| 2C-02: two invites of one person under two claims | `timeout-overlap` | 196 | 6 | false | none |
| 2C-03: duplicate invite of same (person, claim) from two copies | `pure` | 14 | 1 | false | none |
| 2C-03: duplicate invite of same (person, claim) from two copies | `individual` | 61 | 5 | false | none |
| 2C-03: duplicate invite of same (person, claim) from two copies | `all-after` | 40 | 2 | false | none |
| 2C-03: duplicate invite of same (person, claim) from two copies | `timeout-overlap` | 74 | 3 | false | none |
| 2C-04: invite racing 1-claim enter proving that claim | `pure` | 24 | 2 | false | none |
| 2C-04: invite racing 1-claim enter proving that claim | `individual` | 95 | 6 | false | none |
| 2C-04: invite racing 1-claim enter proving that claim | `all-after` | 117 | 5 | false | `final-state-or-says-violates-spec` |
| 2C-04: invite racing 1-claim enter proving that claim | `timeout-overlap` | 175 | 6 | false | none |
| 2C-05: two 1-claim enters of one account proving C1 and C2 (clocks differ: t1=200 vs t2=100) | `pure` | 16 | 1 | false | `final-state-or-says-violates-spec` |
| 2C-05: two 1-claim enters of one account proving C1 and C2 (clocks differ: t1=200 vs t2=100) | `individual` | 56 | 4 | false | `final-state-or-says-violates-spec` |
| 2C-05: two 1-claim enters of one account proving C1 and C2 (clocks differ: t1=200 vs t2=100) | `all-after` | 52 | 2 | false | `final-state-or-says-violates-spec` |
| 2C-05: two 1-claim enters of one account proving C1 and C2 (clocks differ: t1=200 vs t2=100) | `timeout-overlap` | 98 | 4 | false | `final-state-or-says-violates-spec` |
| 2C-06: two returning enters of linked account with differing clocks (t1=300 vs t2=100) | `pure` | 6 | 1 | false | `final-state-or-says-violates-spec` |
| 2C-06: two returning enters of linked account with differing clocks (t1=300 vs t2=100) | `individual` | 15 | 3 | false | `final-state-or-says-violates-spec` |
| 2C-06: two returning enters of linked account with differing clocks (t1=300 vs t2=100) | `all-after` | 25 | 2 | false | `final-state-or-says-violates-spec` |
| 2C-06: two returning enters of linked account with differing clocks (t1=300 vs t2=100) | `timeout-overlap` | 20 | 3 | false | `final-state-or-says-violates-spec` |
| 2C-07: two accounts proving one claim (1-claim enters) | `pure` | 28 | 2 | false | none |
| 2C-07: two accounts proving one claim (1-claim enters) | `individual` | 124 | 6 | false | none |
| 2C-07: two accounts proving one claim (1-claim enters) | `all-after` | 139 | 4 | false | none |
| 2C-07: two accounts proving one claim (1-claim enters) | `timeout-overlap` | 206 | 6 | false | none |
| 2C-08: two accounts proving shared code claim (2-claim fallback to email) | `pure` | 59 | 2 | false | none |
| 2C-08: two accounts proving shared code claim (2-claim fallback to email) | `individual` | 348 | 6 | false | none |
| 2C-08: two accounts proving shared code claim (2-claim fallback to email) | `all-after` | 347 | 4 | false | none |
| 2C-08: two accounts proving shared code claim (2-claim fallback to email) | `timeout-overlap` | 617 | 6 | false | none |
| 2C-09: 2-claim enter (code nil, email waiting) racing invite on the code claim | `pure` | 53 | 2 | false | none |
| 2C-09: 2-claim enter (code nil, email waiting) racing invite on the code claim | `individual` | 349 | 6 | false | none |
| 2C-09: 2-claim enter (code nil, email waiting) racing invite on the code claim | `all-after` | 352 | 4 | false | none |
| 2C-09: 2-claim enter (code nil, email waiting) racing invite on the code claim | `timeout-overlap` | 559 | 6 | false | none |
| 2C-10: returning enter racing own-layer-made | `pure` | 7 | 2 | false | none |
| 2C-10: returning enter racing own-layer-made | `individual` | 14 | 4 | false | none |
| 2C-10: returning enter racing own-layer-made | `all-after` | 25 | 4 | false | none |
| 2C-10: returning enter racing own-layer-made | `timeout-overlap` | 18 | 4 | false | none |
| 2C-11: two persons answering one ask yes with new agents at once | `pure` | 12 | 1 | false | none |
| 2C-11: two persons answering one ask yes with new agents at once | `individual` | 40 | 3 | false | none |
| 2C-11: two persons answering one ask yes with new agents at once | `all-after` | 36 | 2 | false | none |
| 2C-11: two persons answering one ask yes with new agents at once | `timeout-overlap` | 72 | 3 | false | none |
| 2C-12: same person P1 answers ask from two copies with differing clocks (t1=500, t2=2000; G0/S0 expire at 1500) | `pure` | 12 | 2 | false | `answer-ask-takeout-missed-on-return` |
| 2C-12: same person P1 answers ask from two copies with differing clocks (t1=500, t2=2000; G0/S0 expire at 1500) | `individual` | 35 | 5 | false | `answer-ask-takeout-missed-on-return` |
| 2C-12: same person P1 answers ask from two copies with differing clocks (t1=500, t2=2000; G0/S0 expire at 1500) | `all-after` | 41 | 5 | false | `answer-ask-takeout-missed-on-return` |
| 2C-12: same person P1 answers ask from two copies with differing clocks (t1=500, t2=2000; G0/S0 expire at 1500) | `timeout-overlap` | 63 | 6 | false | `answer-ask-takeout-missed-on-return` |
| 2C-13: answer-ask yes for existing agent A1 racing end-actor(A1) | `pure` | 10 | 2 | false | none |
| 2C-13: answer-ask yes for existing agent A1 racing end-actor(A1) | `individual` | 26 | 4 | false | none |
| 2C-13: answer-ask yes for existing agent A1 racing end-actor(A1) | `all-after` | 40 | 4 | false | none |
| 2C-13: answer-ask yes for existing agent A1 racing end-actor(A1) | `timeout-overlap` | 39 | 4 | false | none |
| 2C-14: two answers straddling ask :ends (P1 at=500 before ends=1000, P2 at=1200 after ends) | `pure` | 12 | 1 | false | none |
| 2C-14: two answers straddling ask :ends (P1 at=500 before ends=1000, P2 at=1200 after ends) | `individual` | 40 | 3 | false | none |
| 2C-14: two answers straddling ask :ends (P1 at=500 before ends=1000, P2 at=1200 after ends) | `all-after` | 36 | 2 | false | none |
| 2C-14: two answers straddling ask :ends (P1 at=500 before ends=1000, P2 at=1200 after ends) | `timeout-overlap` | 72 | 3 | false | none |
| 3C-01: three invites (P1,C1), (P2,C1), and (P1,C2) — refusal order & helping | `pure` | 151 | 4 | false | none |
| 3C-01: three invites (P1,C1), (P2,C1), and (P1,C2) — refusal order & helping | `individual` | 870 | 17 | false | `root-reserving-forever` |
| 3C-01: three invites (P1,C1), (P2,C1), and (P1,C2) — refusal order & helping | `all-after` | 884 | 9 | false | `root-reserving-forever` |
| 3C-01: three invites (P1,C1), (P2,C1), and (P1,C2) — refusal order & helping | `timeout-overlap` | 1,540 | 15 | false | none |
| 3C-02: sequential causal chain invite(P2,C1) -> invite(P1,C2) while invite(P1,C1) in flight | `pure` | 62 | 4 | false | `no-valid-causal-linearization-of-results` |
| 3C-02: sequential causal chain invite(P2,C1) -> invite(P1,C2) while invite(P1,C1) in flight | `individual` | 320 | 17 | false | `root-reserving-forever`, `no-valid-causal-linearization-of-results` |
| 3C-02: sequential causal chain invite(P2,C1) -> invite(P1,C2) while invite(P1,C1) in flight | `all-after` | 378 | 9 | false | `root-reserving-forever`, `no-valid-causal-linearization-of-results` |
| 3C-02: sequential causal chain invite(P2,C1) -> invite(P1,C2) while invite(P1,C1) in flight | `timeout-overlap` | 543 | 15 | false | `no-valid-causal-linearization-of-results` |
| 3C-03: 1-claim enter(Acc1,C1) + 1-claim enter(Acc1,C2) + invite(P1,C1) | `pure` | 90 | 4 | false | `client-rerun-violates-spec`, `no-valid-causal-linearization-of-results` |
| 3C-03: 1-claim enter(Acc1,C1) + 1-claim enter(Acc1,C2) + invite(P1,C1) | `individual` | 599 | 20 | false | `no-valid-causal-linearization-of-results`, `final-state-or-says-violates-spec`, `claim-used-without-entered-root`, `one-account-two-persons`, `enter-account-link-misses-effect` |
| 3C-03: 1-claim enter(Acc1,C1) + 1-claim enter(Acc1,C2) + invite(P1,C1) | `all-after` | 660 | 11 | false | `no-valid-causal-linearization-of-results`, `claim-used-without-entered-root`, `one-account-two-persons`, `final-state-or-says-violates-spec` |
| 3C-03: 1-claim enter(Acc1,C1) + 1-claim enter(Acc1,C2) + invite(P1,C1) | `timeout-overlap` | 987 | 18 | false | `no-valid-causal-linearization-of-results`, `final-state-or-says-violates-spec`, `one-account-two-persons`, `enter-account-link-misses-effect` |
| 3C-04: 2-claim enter(Acc1, code=C_CODE, email=C1) + enter(Acc1, C1) + invite(P1, C_CODE) | `pure` | 386 | 2 | false | `client-rerun-violates-spec` |
| 3C-04: 2-claim enter(Acc1, code=C_CODE, email=C1) + enter(Acc1, C1) + invite(P1, C_CODE) | `individual` | 2,760 | 11 | false | `final-state-or-says-violates-spec`, `claim-claiming-forever` |
| 3C-04: 2-claim enter(Acc1, code=C_CODE, email=C1) + enter(Acc1, C1) + invite(P1, C_CODE) | `all-after` | 2,156 | 5 | false | `claim-claiming-forever` |
| 3C-04: 2-claim enter(Acc1, code=C_CODE, email=C1) + enter(Acc1, C1) + invite(P1, C_CODE) | `timeout-overlap` | 6,722 | 10 | false | `final-state-or-says-violates-spec` |
| 3C-05: 2-claim enter(Acc1) + 1-claim enter(Acc2, C_CODE) + 1-claim enter(Acc3, C_CODE) | `pure` | 405 | 3 | false | none |
| 3C-05: 2-claim enter(Acc1) + 1-claim enter(Acc2, C_CODE) + 1-claim enter(Acc3, C_CODE) | `individual` | 2,921 | 12 | false | none |
| 3C-05: 2-claim enter(Acc1) + 1-claim enter(Acc2, C_CODE) + 1-claim enter(Acc3, C_CODE) | `all-after` | 2,838 | 6 | false | none |
| 3C-05: 2-claim enter(Acc1) + 1-claim enter(Acc2, C_CODE) + 1-claim enter(Acc3, C_CODE) | `timeout-overlap` | 6,002 | 12 | false | none |
| 3C-06: Trace E-3 continuation — Acc2 and Acc3 (1-claim & 2-claim) meet stale ClaimClaiming on C_CODE | `pure` | 68 | 4 | false | `one-claim-two-accounts` |
| 3C-06: Trace E-3 continuation — Acc2 and Acc3 (1-claim & 2-claim) meet stale ClaimClaiming on C_CODE | `individual` | 338 | 14 | false | `one-claim-two-accounts`, `entered-root-without-matching-account-link` |
| 3C-06: Trace E-3 continuation — Acc2 and Acc3 (1-claim & 2-claim) meet stale ClaimClaiming on C_CODE | `all-after` | 351 | 10 | false | `one-claim-two-accounts`, `entered-root-without-matching-account-link` |
| 3C-06: Trace E-3 continuation — Acc2 and Acc3 (1-claim & 2-claim) meet stale ClaimClaiming on C_CODE | `timeout-overlap` | 886 | 18 | false | `one-claim-two-accounts` |
| 2C-15: Trace E-3 continuation — two 2-claim enters (Acc2, Acc3) evicting stale ClaimClaiming (PLAN.md:537) | `pure` | 240 | 8 | false | `one-claim-two-accounts` |
| 2C-15: Trace E-3 continuation — two 2-claim enters (Acc2, Acc3) evicting stale ClaimClaiming (PLAN.md:537) | `individual` | 1,766 | 28 | false | `one-claim-two-accounts` |
| 2C-15: Trace E-3 continuation — two 2-claim enters (Acc2, Acc3) evicting stale ClaimClaiming (PLAN.md:537) | `all-after` | 1,715 | 20 | false | `one-claim-two-accounts` |
| 2C-15: Trace E-3 continuation — two 2-claim enters (Acc2, Acc3) evicting stale ClaimClaiming (PLAN.md:537) | `timeout-overlap` | 6,834 | 28 | false | `one-claim-two-accounts` |
| 3C-08: invite(P1,C1) in flight; invite(P2,C2) returns -> invite(P1,C2) helps C1 without rechecking C2 (PLAN.md:479-480) | `pure` | 30 | 1 | false | none |
| 3C-08: invite(P1,C1) in flight; invite(P2,C2) returns -> invite(P1,C2) helps C1 without rechecking C2 (PLAN.md:479-480) | `individual` | 123 | 3 | false | none |
| 3C-08: invite(P1,C1) in flight; invite(P2,C2) returns -> invite(P1,C2) helps C1 without rechecking C2 (PLAN.md:479-480) | `all-after` | 145 | 2 | false | none |
| 3C-08: invite(P1,C1) in flight; invite(P2,C2) returns -> invite(P1,C2) helps C1 without rechecking C2 (PLAN.md:479-480) | `timeout-overlap` | 178 | 3 | false | none |
| 3C-09: answer-ask yes with new-agent A1 racing make-actor(P1, A1) and second answer-ask | `pure` | 31 | 2 | false | none |
| 3C-09: answer-ask yes with new-agent A1 racing make-actor(P1, A1) and second answer-ask | `individual` | 109 | 7 | false | none |
| 3C-09: answer-ask yes with new-agent A1 racing make-actor(P1, A1) and second answer-ask | `all-after` | 135 | 4 | false | none |
| 3C-09: answer-ask yes with new-agent A1 racing make-actor(P1, A1) and second answer-ask | `timeout-overlap` | 175 | 7 | false | none |
| 3C-07: three answers on one ask — refused answer (:no-such-actor) + yes + no | `pure` | 47 | 2 | false | none |
| 3C-07: three answers on one ask — refused answer (:no-such-actor) + yes + no | `individual` | 287 | 8 | false | none |
| 3C-07: three answers on one ask — refused answer (:no-such-actor) + yes + no | `all-after` | 283 | 4 | false | none |
| 3C-07: three answers on one ask — refused answer (:no-such-actor) + yes + no | `timeout-overlap` | 568 | 9 | false | none |

## What the Model Leaves Out, and How Far the Search Went

- **How far the search went:** Every 2-caller scenario (`2C-01`..`2C-14`) ran exhaustively to 0 remaining unexplored states across `:pure` (0 faults + post-quiescence client re-runs), `:individual` (crash + source retry at every in-flight hop), `:all-after` (partition replay from every prior depot index), and `:timeout-overlap` (duplicate attempt while slow hop remains queued). All 3-caller scenarios (`3C-01`..`3C-07`) ran with the same four modes up to 35,000 deduplicated system states per `(scenario, mode)` cell.
- **What the model includes:** Exact single-task atomic steps of `PLAN.md` §1A (`invite`), §1B (`enter` 1-claim and 2-claim), §1C (`answer-ask`), §2 (`take-out`, `end-actor`, `own-layer-made`), and all 4 reads (`who-is`, `ask-of`, `under`, `watch-root` / `$$live`), FIFO ordering per `(src-task, dst-task)` channel (`stream.md:37`), independent single-threaded tasks per entity key, differing clocks, helpers, and causal linearizability against `SPEC.md` + Sid's 2 calls of 22 November.
- **What the model leaves out:**
  1. Physical JVM/RocksDB serialization, network sockets, and `ForeignProxy` diff transport (client-side callback threading and `ping-task` timers).
  2. Multiple cascading faults (`> 1` injected fault per 2/3-caller trace), and `>= 4` simultaneous callers (note: `3C-06` explicitly tests the 2-caller continuation from `PLAN.md` Trace E-3's 2-caller setup, which corresponds to 4 callers end-to-end).
  3. Mid-flight foreign reads *before* a write returns (`PLAN.md` Wall 1 — the model checks read-after-write immediately upon each write's return and at quiescence, as asked).
  4. Uncheckpointed `:root-events` backward-clock replay on pure single-task root writes (`PLAN.md` Wall 3, evaluated separately in `PLAN_VALIDATION.md` Step 2d).
