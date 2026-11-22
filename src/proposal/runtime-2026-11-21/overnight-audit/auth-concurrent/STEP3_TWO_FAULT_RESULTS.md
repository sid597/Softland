# Step 3 — Independent 1-Fault and 2-Fault Model Check of `spine/auth/agreement/`

## Suite 1: 0/1-Fault Check (`fault-budget = 0/1`, `check-reruns? = true`)

- **Runs:** 32 scenarios × 4 modes = **128 runs** (3588 ms)
- **States explored:** **97,307** (0 capped runs)
- **Quiescent terminal histories checked:** **539**
- **Violations across Q1..Q5:** **0**

## Suite 2: Two Faults in One History (`fault-budget = 2`, `check-reruns? = true`)

- **Runs:** **136 runs** across 36 scenarios (`25` carbon + `7` `X-01..X-07` + `4` `TF-01..TF-04`) × `:individual`, `:all-after`, `:timeout-overlap`, and `:mixed` (50553 ms)
- **States explored:** **1,188,828** (1 capped runs at `max-states = 250,000`)
- **Quiescent terminal histories checked:** **1,910**
- **Violations across Q1..Q5:** **6**

### Runs reaching `max-states` cap

- `3C-05: 2-claim enter(Acc1) + 1-claim enter(Acc2, C_CODE) + 1-claim enter(Acc3, C_CODE)` (`timeout-overlap`, 2 faults): 250,000 states, 6 terminal histories, 0 violations

## Violations Found

### Witness 1: `:q4-disallowed-result` / `:no-valid-causal-linearization-of-results`

- **Scenario:** TF-01: first-time enter(Acc1, C1) racing own-layer-made(P1) + returning enter(Acc1, older :at)
- **Execution / retry mode:** `:individual`
- **What broke:** Returned results cannot occur in ANY causal order under SPEC.md + Sid's 2 calls: [{:id :c2, :call [:own-layer-made {:person :P1, :at 210}], :invoked-at-step 0, :returned-at-step 7, :result {:made :P1}} {:id :c3, :call [:enter {:account [:google "sub-1"], :says {"s" "older-50"}, :email "alice@example.com", :code-hash nil, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 50}], :invoked-at-step 7, :returned-at-step 12, :result {:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? true}} {:id :c1, :call [:enter {:account [:google "sub-1"], :says {"s" "init-200"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 200}], :invoked-at-step 0, :returned-at-step 14, :result {:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? true}}]
- **Completed call results:**
  - `:c1` `[:enter {:account [:google "sub-1"], :says {"s" "init-200"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 200}]` (invoked step 0, returned step 14) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? true}`
  - `:c2` `[:own-layer-made {:person :P1, :at 210}]` (invoked step 0, returned step 7) -> `{:made :P1}`
  - `:c3` `[:enter {:account [:google "sub-1"], :says {"s" "older-50"}, :email "alice@example.com", :code-hash nil, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 50}]` (invoked step 7, returned step 12) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? true}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:enter-step-1-start-linking` | H(Acc=[:google "sub-1"]) unlinked -> writes AccountLinking(op=[:op :c1], status=:probing), hops to E2 probe |
| 2 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-2-probe-claim` | `PLAN.md:enter-step-2-usable` | H(C={:by :email, :email "alice@example.com"}) probe phase=:email sees usable :ClaimWaiting(P=:P1) -> hops \|hash Acc to E2' choose |
| 3 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-2b-choose` | `PLAN.md:enter-step-2b-commit-choice` | H(Acc=[:google "sub-1"]) E2' commits status=:chosen C={:by :email, :email "alice@example.com"} on op=[:op :c1] -> hops \|hash op to confirm |
| 4 | `:c1` (#1) | `[:op [:op :c1]]` | `:enter/step-2c-confirm-choice` | `PLAN.md:enter-step-2c-confirmed` | H(op=[:op :c1]) confirmed not failed -> hops \|hash C={:by :email, :email "alice@example.com"} to E3 |
| 5 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-3-use-claim` | `PLAN.md:enter-step-3-use` | H(C={:by :email, :email "alice@example.com"}) E3 sees ClaimWaiting(P=:P1) -> writes ClaimUsed(P, op=[:op :c1]), hops \|hash P to E4 |
| 6 | `:c1` (#1) | `[:root :P1]` | `:enter/step-4-enter-root` | `PLAN.md:enter-step-4-root` | H(P=:P1) E4 ensures RootEntered (fixed L'=:L1, made?=false) -> hops \|hash Acc to E5 |
| 7 | `:c2` (#2) | `[:root :P1]` | `:own-layer-made/step-1` | `PLAN.md:own-layer-made` | H(P=:P1) sets :own-layer-made? true -> ack-return {:made :P1} |
| 8 | `:c3` (#3) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:enter-step-1-help-linking` | H(Acc=[:google "sub-1"]) sees AccountLinking(op=[:op :c1], status=:chosen, chosen={:by :email, :email "alice@example.com"}) |
| 9 | `:c3` (#3) | `[:op [:op :c1]]` | `:enter/step-2c-confirm-choice` | `PLAN.md:enter-step-2c-confirmed` | H(op=[:op :c1]) confirmed not failed -> hops \|hash C={:by :email, :email "alice@example.com"} to E3 |
| 10 | `:c3` (#3) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-3-use-claim` | `PLAN.md:enter-step-3-already-ours` | H(C={:by :email, :email "alice@example.com"}) E3 sees own ClaimUsed(P=:P1, op=[:op :c1]) -> hops \|hash P to E4 |
| 11 | `:c3` (#3) | `[:root :P1]` | `:enter/step-4-enter-root` | `PLAN.md:enter-step-4-root` | H(P=:P1) E4 ensures RootEntered (fixed L'=:L1, made?=true) -> hops \|hash Acc to E5 |
| 11 | `:c1` (#101) | `[:account [:google "sub-1"]]` | `:FAULT/individual-retry` | `stream.md:94-102` | Crash before :enter/step-5-link-account on [:account [:google "sub-1"]]; prior hops stay committed; Rama retries :c1 from Step 1 on [:account [:google "sub-1"]] |
| 12 | `:c3` (#3) | `[:account [:google "sub-1"]]` | `:enter/step-5-link-account` | `PLAN.md:enter-step-5-link` | H(Acc=[:google "sub-1"]) E5 finalizes AccountLinked(P=:P1) with monotonic :says -> ack-return |
| 13 | `:c1` (#101) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:enter-step-1-returning` | H(Acc=[:google "sub-1"]) sees AccountLinked(P=:P1) with at=200 <= 200 -> hops \|hash P for :own-layer-made? |
| 14 | `:c1` (#101) | `[:root :P1]` | `:enter/step-1b-returning-read-flag` | `PLAN.md:enter-step-1b-flag` | H(P=:P1) reads :own-layer-made?=true -> ack-return {:person :P1 ...} |

### Witness 2: `:q4-disallowed-result` / `:client-rerun-violates-spec`

- **Scenario:** TF-01: first-time enter(Acc1, C1) racing own-layer-made(P1) + returning enter(Acc1, older :at)
- **Execution / retry mode:** `:all-after`
- **What broke:** Re-running :c1 [:enter {:account [:google "sub-1"], :says {"s" "init-200"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 200}] (which originally gave {:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}) returned {:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? true} / left state disallowed by SPEC.md + Sid's 2 calls
- **Completed call results:**
  - `:c1` `[:enter {:account [:google "sub-1"], :says {"s" "init-200"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 200}]` (invoked step 0, returned step 8) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}`
  - `:c2` `[:own-layer-made {:person :P1, :at 210}]` (invoked step 0, returned step 2) -> `{:refused :no-such-person}`
  - `:c3` `[:enter {:account [:google "sub-1"], :says {"s" "older-50"}, :email "alice@example.com", :code-hash nil, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 50}]` (invoked step 2, returned step 10) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:enter-step-1-start-linking` | H(Acc=[:google "sub-1"]) unlinked -> writes AccountLinking(op=[:op :c1], status=:probing), hops to E2 probe |
| 2 | `:c2` (#2) | `[:root :P1]` | `:own-layer-made/step-1` | `PLAN.md:own-layer-made` | H(P=:P1) r not RootEntered -> ack-return {:refused :no-such-person} |
| 3 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-2-probe-claim` | `PLAN.md:enter-step-2-usable` | H(C={:by :email, :email "alice@example.com"}) probe phase=:email sees usable :ClaimWaiting(P=:P1) -> hops \|hash Acc to E2' choose |
| 4 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-2b-choose` | `PLAN.md:enter-step-2b-commit-choice` | H(Acc=[:google "sub-1"]) E2' commits status=:chosen C={:by :email, :email "alice@example.com"} on op=[:op :c1] -> hops \|hash op to confirm |
| 5 | `:c1` (#1) | `[:op [:op :c1]]` | `:enter/step-2c-confirm-choice` | `PLAN.md:enter-step-2c-confirmed` | H(op=[:op :c1]) confirmed not failed -> hops \|hash C={:by :email, :email "alice@example.com"} to E3 |
| 6 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-3-use-claim` | `PLAN.md:enter-step-3-use` | H(C={:by :email, :email "alice@example.com"}) E3 sees ClaimWaiting(P=:P1) -> writes ClaimUsed(P, op=[:op :c1]), hops \|hash P to E4 |
| 7 | `:c1` (#1) | `[:root :P1]` | `:enter/step-4-enter-root` | `PLAN.md:enter-step-4-root` | H(P=:P1) E4 ensures RootEntered (fixed L'=:L1, made?=false) -> hops \|hash Acc to E5 |
| 8 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-5-link-account` | `PLAN.md:enter-step-5-link` | H(Acc=[:google "sub-1"]) E5 finalizes AccountLinked(P=:P1) with monotonic :says -> ack-return |
| 9 | `:c3` (#3) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:enter-step-1-returning` | H(Acc=[:google "sub-1"]) sees AccountLinked(P=:P1) with at=50 <= 200 -> hops \|hash P for :own-layer-made? |
| 10 | `:c3` (#3) | `[:root :P1]` | `:enter/step-1b-returning-read-flag` | `PLAN.md:enter-step-1b-flag` | H(P=:P1) reads :own-layer-made?=false -> ack-return {:person :P1 ...} |
| 10 | `[:c2]` (#101) | `[:root :P1]` | `:FAULT/all-after-replay` | `PLAN.md:195, stream.md:96` | Rama :all-after / uncheckpointed replay on [:root :P1] from idx=0 for calls [:c2] |
| 11 | `:c2` (#101) | `[:root :P1]` | `:own-layer-made/step-1` | `PLAN.md:own-layer-made` | H(P=:P1) sets :own-layer-made? true -> ack-return {:made :P1} |

**Follow-up re-run trace:**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `[:rerun :c1]` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:enter-step-1-returning` | H(Acc=[:google "sub-1"]) sees AccountLinked(P=:P1) with at=200 <= 200 -> hops \|hash P for :own-layer-made? |
| 2 | `[:rerun :c1]` (#1) | `[:root :P1]` | `:enter/step-1b-returning-read-flag` | `PLAN.md:enter-step-1b-flag` | H(P=:P1) reads :own-layer-made?=true -> ack-return {:person :P1 ...} |

### Witness 3: `:q4-disallowed-result` / `:no-valid-causal-linearization-of-results`

- **Scenario:** TF-01: first-time enter(Acc1, C1) racing own-layer-made(P1) + returning enter(Acc1, older :at)
- **Execution / retry mode:** `:all-after`
- **What broke:** Returned results cannot occur in ANY causal order under SPEC.md + Sid's 2 calls: [{:id :c2, :call [:own-layer-made {:person :P1, :at 210}], :invoked-at-step 0, :returned-at-step 2, :result {:refused :no-such-person}} {:id :c1, :call [:enter {:account [:google "sub-1"], :says {"s" "init-200"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 200}], :invoked-at-step 0, :returned-at-step 8, :result {:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}} {:id :c3, :call [:enter {:account [:google "sub-1"], :says {"s" "older-50"}, :email "alice@example.com", :code-hash nil, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 50}], :invoked-at-step 2, :returned-at-step 11, :result {:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? true}}]
- **Completed call results:**
  - `:c1` `[:enter {:account [:google "sub-1"], :says {"s" "init-200"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 200}]` (invoked step 0, returned step 8) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}`
  - `:c2` `[:own-layer-made {:person :P1, :at 210}]` (invoked step 0, returned step 2) -> `{:refused :no-such-person}`
  - `:c3` `[:enter {:account [:google "sub-1"], :says {"s" "older-50"}, :email "alice@example.com", :code-hash nil, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 50}]` (invoked step 2, returned step 11) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? true}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:enter-step-1-start-linking` | H(Acc=[:google "sub-1"]) unlinked -> writes AccountLinking(op=[:op :c1], status=:probing), hops to E2 probe |
| 2 | `:c2` (#2) | `[:root :P1]` | `:own-layer-made/step-1` | `PLAN.md:own-layer-made` | H(P=:P1) r not RootEntered -> ack-return {:refused :no-such-person} |
| 3 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-2-probe-claim` | `PLAN.md:enter-step-2-usable` | H(C={:by :email, :email "alice@example.com"}) probe phase=:email sees usable :ClaimWaiting(P=:P1) -> hops \|hash Acc to E2' choose |
| 4 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-2b-choose` | `PLAN.md:enter-step-2b-commit-choice` | H(Acc=[:google "sub-1"]) E2' commits status=:chosen C={:by :email, :email "alice@example.com"} on op=[:op :c1] -> hops \|hash op to confirm |
| 5 | `:c1` (#1) | `[:op [:op :c1]]` | `:enter/step-2c-confirm-choice` | `PLAN.md:enter-step-2c-confirmed` | H(op=[:op :c1]) confirmed not failed -> hops \|hash C={:by :email, :email "alice@example.com"} to E3 |
| 6 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-3-use-claim` | `PLAN.md:enter-step-3-use` | H(C={:by :email, :email "alice@example.com"}) E3 sees ClaimWaiting(P=:P1) -> writes ClaimUsed(P, op=[:op :c1]), hops \|hash P to E4 |
| 7 | `:c1` (#1) | `[:root :P1]` | `:enter/step-4-enter-root` | `PLAN.md:enter-step-4-root` | H(P=:P1) E4 ensures RootEntered (fixed L'=:L1, made?=false) -> hops \|hash Acc to E5 |
| 8 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-5-link-account` | `PLAN.md:enter-step-5-link` | H(Acc=[:google "sub-1"]) E5 finalizes AccountLinked(P=:P1) with monotonic :says -> ack-return |
| 9 | `:c3` (#3) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:enter-step-1-returning` | H(Acc=[:google "sub-1"]) sees AccountLinked(P=:P1) with at=50 <= 200 -> hops \|hash P for :own-layer-made? |
| 9 | `[:c2]` (#101) | `[:root :P1]` | `:FAULT/all-after-replay` | `PLAN.md:195, stream.md:96` | Rama :all-after / uncheckpointed replay on [:root :P1] from idx=0 for calls [:c2] |
| 10 | `:c2` (#101) | `[:root :P1]` | `:own-layer-made/step-1` | `PLAN.md:own-layer-made` | H(P=:P1) sets :own-layer-made? true -> ack-return {:made :P1} |
| 11 | `:c3` (#3) | `[:root :P1]` | `:enter/step-1b-returning-read-flag` | `PLAN.md:enter-step-1b-flag` | H(P=:P1) reads :own-layer-made?=true -> ack-return {:person :P1 ...} |

### Witness 4: `:q4-disallowed-result` / `:no-valid-causal-linearization-of-results`

- **Scenario:** TF-01: first-time enter(Acc1, C1) racing own-layer-made(P1) + returning enter(Acc1, older :at)
- **Execution / retry mode:** `:timeout-overlap`
- **What broke:** Returned results cannot occur in ANY causal order under SPEC.md + Sid's 2 calls: [{:id :c2, :call [:own-layer-made {:person :P1, :at 210}], :invoked-at-step 0, :returned-at-step 7, :result {:made :P1}} {:id :c3, :call [:enter {:account [:google "sub-1"], :says {"s" "older-50"}, :email "alice@example.com", :code-hash nil, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 50}], :invoked-at-step 7, :returned-at-step 11, :result {:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? true}} {:id :c1, :call [:enter {:account [:google "sub-1"], :says {"s" "init-200"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 200}], :invoked-at-step 0, :returned-at-step 12, :result {:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? true}}]
- **Completed call results:**
  - `:c1` `[:enter {:account [:google "sub-1"], :says {"s" "init-200"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 200}]` (invoked step 0, returned step 12) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? true}`
  - `:c2` `[:own-layer-made {:person :P1, :at 210}]` (invoked step 0, returned step 7) -> `{:made :P1}`
  - `:c3` `[:enter {:account [:google "sub-1"], :says {"s" "older-50"}, :email "alice@example.com", :code-hash nil, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 50}]` (invoked step 7, returned step 11) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? true}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:enter-step-1-start-linking` | H(Acc=[:google "sub-1"]) unlinked -> writes AccountLinking(op=[:op :c1], status=:probing), hops to E2 probe |
| 2 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-2-probe-claim` | `PLAN.md:enter-step-2-usable` | H(C={:by :email, :email "alice@example.com"}) probe phase=:email sees usable :ClaimWaiting(P=:P1) -> hops \|hash Acc to E2' choose |
| 3 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-2b-choose` | `PLAN.md:enter-step-2b-commit-choice` | H(Acc=[:google "sub-1"]) E2' commits status=:chosen C={:by :email, :email "alice@example.com"} on op=[:op :c1] -> hops \|hash op to confirm |
| 4 | `:c1` (#1) | `[:op [:op :c1]]` | `:enter/step-2c-confirm-choice` | `PLAN.md:enter-step-2c-confirmed` | H(op=[:op :c1]) confirmed not failed -> hops \|hash C={:by :email, :email "alice@example.com"} to E3 |
| 5 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-3-use-claim` | `PLAN.md:enter-step-3-use` | H(C={:by :email, :email "alice@example.com"}) E3 sees ClaimWaiting(P=:P1) -> writes ClaimUsed(P, op=[:op :c1]), hops \|hash P to E4 |
| 6 | `:c1` (#1) | `[:root :P1]` | `:enter/step-4-enter-root` | `PLAN.md:enter-step-4-root` | H(P=:P1) E4 ensures RootEntered (fixed L'=:L1, made?=false) -> hops \|hash Acc to E5 |
| 7 | `:c2` (#2) | `[:root :P1]` | `:own-layer-made/step-1` | `PLAN.md:own-layer-made` | H(P=:P1) sets :own-layer-made? true -> ack-return {:made :P1} |
| 7 | `:c1` (#101) | `[:account [:google "sub-1"]]` | `:FAULT/timeout-overlap-retry` | `stream.md:260` | Stream timeout re-emits :c1 from Step 1 while prior attempt's :enter/step-5-link-account remains queued on [[:root :P1] [:account [:google "sub-1"]]] |
| 8 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-5-link-account` | `PLAN.md:enter-step-5-link` | H(Acc=[:google "sub-1"]) E5 finalizes AccountLinked(P=:P1) with monotonic :says -> ack-return |
| 9 | `:c3` (#3) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:enter-step-1-returning` | H(Acc=[:google "sub-1"]) sees AccountLinked(P=:P1) with at=50 <= 200 -> hops \|hash P for :own-layer-made? |
| 10 | `:c1` (#101) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:enter-step-1-returning` | H(Acc=[:google "sub-1"]) sees AccountLinked(P=:P1) with at=200 <= 200 -> hops \|hash P for :own-layer-made? |
| 11 | `:c3` (#3) | `[:root :P1]` | `:enter/step-1b-returning-read-flag` | `PLAN.md:enter-step-1b-flag` | H(P=:P1) reads :own-layer-made?=true -> ack-return {:person :P1 ...} |
| 12 | `:c1` (#101) | `[:root :P1]` | `:enter/step-1b-returning-read-flag` | `PLAN.md:enter-step-1b-flag` | H(P=:P1) reads :own-layer-made?=true -> ack-return {:person :P1 ...} |

### Witness 5: `:q4-disallowed-result` / `:client-rerun-violates-spec`

- **Scenario:** TF-01: first-time enter(Acc1, C1) racing own-layer-made(P1) + returning enter(Acc1, older :at)
- **Execution / retry mode:** `:mixed`
- **What broke:** Re-running :c1 [:enter {:account [:google "sub-1"], :says {"s" "init-200"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 200}] (which originally gave {:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}) returned {:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? true} / left state disallowed by SPEC.md + Sid's 2 calls
- **Completed call results:**
  - `:c1` `[:enter {:account [:google "sub-1"], :says {"s" "init-200"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 200}]` (invoked step 0, returned step 8) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}`
  - `:c2` `[:own-layer-made {:person :P1, :at 210}]` (invoked step 0, returned step 2) -> `{:refused :no-such-person}`
  - `:c3` `[:enter {:account [:google "sub-1"], :says {"s" "older-50"}, :email "alice@example.com", :code-hash nil, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 50}]` (invoked step 2, returned step 10) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:enter-step-1-start-linking` | H(Acc=[:google "sub-1"]) unlinked -> writes AccountLinking(op=[:op :c1], status=:probing), hops to E2 probe |
| 2 | `:c2` (#2) | `[:root :P1]` | `:own-layer-made/step-1` | `PLAN.md:own-layer-made` | H(P=:P1) r not RootEntered -> ack-return {:refused :no-such-person} |
| 3 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-2-probe-claim` | `PLAN.md:enter-step-2-usable` | H(C={:by :email, :email "alice@example.com"}) probe phase=:email sees usable :ClaimWaiting(P=:P1) -> hops \|hash Acc to E2' choose |
| 4 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-2b-choose` | `PLAN.md:enter-step-2b-commit-choice` | H(Acc=[:google "sub-1"]) E2' commits status=:chosen C={:by :email, :email "alice@example.com"} on op=[:op :c1] -> hops \|hash op to confirm |
| 5 | `:c1` (#1) | `[:op [:op :c1]]` | `:enter/step-2c-confirm-choice` | `PLAN.md:enter-step-2c-confirmed` | H(op=[:op :c1]) confirmed not failed -> hops \|hash C={:by :email, :email "alice@example.com"} to E3 |
| 6 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-3-use-claim` | `PLAN.md:enter-step-3-use` | H(C={:by :email, :email "alice@example.com"}) E3 sees ClaimWaiting(P=:P1) -> writes ClaimUsed(P, op=[:op :c1]), hops \|hash P to E4 |
| 7 | `:c1` (#1) | `[:root :P1]` | `:enter/step-4-enter-root` | `PLAN.md:enter-step-4-root` | H(P=:P1) E4 ensures RootEntered (fixed L'=:L1, made?=false) -> hops \|hash Acc to E5 |
| 8 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-5-link-account` | `PLAN.md:enter-step-5-link` | H(Acc=[:google "sub-1"]) E5 finalizes AccountLinked(P=:P1) with monotonic :says -> ack-return |
| 9 | `:c3` (#3) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:enter-step-1-returning` | H(Acc=[:google "sub-1"]) sees AccountLinked(P=:P1) with at=50 <= 200 -> hops \|hash P for :own-layer-made? |
| 10 | `:c3` (#3) | `[:root :P1]` | `:enter/step-1b-returning-read-flag` | `PLAN.md:enter-step-1b-flag` | H(P=:P1) reads :own-layer-made?=false -> ack-return {:person :P1 ...} |
| 10 | `[:c2]` (#101) | `[:root :P1]` | `:FAULT/all-after-replay` | `PLAN.md:195, stream.md:96` | Rama :all-after / uncheckpointed replay on [:root :P1] from idx=0 for calls [:c2] |
| 11 | `:c2` (#101) | `[:root :P1]` | `:own-layer-made/step-1` | `PLAN.md:own-layer-made` | H(P=:P1) sets :own-layer-made? true -> ack-return {:made :P1} |

**Follow-up re-run trace:**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `[:rerun :c1]` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:enter-step-1-returning` | H(Acc=[:google "sub-1"]) sees AccountLinked(P=:P1) with at=200 <= 200 -> hops \|hash P for :own-layer-made? |
| 2 | `[:rerun :c1]` (#1) | `[:root :P1]` | `:enter/step-1b-returning-read-flag` | `PLAN.md:enter-step-1b-flag` | H(P=:P1) reads :own-layer-made?=true -> ack-return {:person :P1 ...} |

### Witness 6: `:q4-disallowed-result` / `:no-valid-causal-linearization-of-results`

- **Scenario:** TF-01: first-time enter(Acc1, C1) racing own-layer-made(P1) + returning enter(Acc1, older :at)
- **Execution / retry mode:** `:mixed`
- **What broke:** Returned results cannot occur in ANY causal order under SPEC.md + Sid's 2 calls: [{:id :c2, :call [:own-layer-made {:person :P1, :at 210}], :invoked-at-step 0, :returned-at-step 2, :result {:refused :no-such-person}} {:id :c1, :call [:enter {:account [:google "sub-1"], :says {"s" "init-200"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 200}], :invoked-at-step 0, :returned-at-step 8, :result {:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}} {:id :c3, :call [:enter {:account [:google "sub-1"], :says {"s" "older-50"}, :email "alice@example.com", :code-hash nil, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 50}], :invoked-at-step 2, :returned-at-step 11, :result {:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? true}}]
- **Completed call results:**
  - `:c1` `[:enter {:account [:google "sub-1"], :says {"s" "init-200"}, :email "alice@example.com", :code-hash nil, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :at 200}]` (invoked step 0, returned step 8) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? false}`
  - `:c2` `[:own-layer-made {:person :P1, :at 210}]` (invoked step 0, returned step 2) -> `{:refused :no-such-person}`
  - `:c3` `[:enter {:account [:google "sub-1"], :says {"s" "older-50"}, :email "alice@example.com", :code-hash nil, :own-layer :L2, :making {:uuid :m-uuid-2, :session :m-sess-2}, :at 50}]` (invoked step 2, returned step 11) -> `{:person :P1, :own-layer :L1, :making {:uuid :m-uuid-1, :session :m-sess-1}, :own-layer-made? true}`

**Whole step-by-step history (with `PLAN.md` line citations):**

| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |
|---|---|---|---|---|---|
| 1 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:enter-step-1-start-linking` | H(Acc=[:google "sub-1"]) unlinked -> writes AccountLinking(op=[:op :c1], status=:probing), hops to E2 probe |
| 2 | `:c2` (#2) | `[:root :P1]` | `:own-layer-made/step-1` | `PLAN.md:own-layer-made` | H(P=:P1) r not RootEntered -> ack-return {:refused :no-such-person} |
| 3 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-2-probe-claim` | `PLAN.md:enter-step-2-usable` | H(C={:by :email, :email "alice@example.com"}) probe phase=:email sees usable :ClaimWaiting(P=:P1) -> hops \|hash Acc to E2' choose |
| 4 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-2b-choose` | `PLAN.md:enter-step-2b-commit-choice` | H(Acc=[:google "sub-1"]) E2' commits status=:chosen C={:by :email, :email "alice@example.com"} on op=[:op :c1] -> hops \|hash op to confirm |
| 5 | `:c1` (#1) | `[:op [:op :c1]]` | `:enter/step-2c-confirm-choice` | `PLAN.md:enter-step-2c-confirmed` | H(op=[:op :c1]) confirmed not failed -> hops \|hash C={:by :email, :email "alice@example.com"} to E3 |
| 6 | `:c1` (#1) | `[:claim {:by :email, :email "alice@example.com"}]` | `:enter/step-3-use-claim` | `PLAN.md:enter-step-3-use` | H(C={:by :email, :email "alice@example.com"}) E3 sees ClaimWaiting(P=:P1) -> writes ClaimUsed(P, op=[:op :c1]), hops \|hash P to E4 |
| 7 | `:c1` (#1) | `[:root :P1]` | `:enter/step-4-enter-root` | `PLAN.md:enter-step-4-root` | H(P=:P1) E4 ensures RootEntered (fixed L'=:L1, made?=false) -> hops \|hash Acc to E5 |
| 8 | `:c1` (#1) | `[:account [:google "sub-1"]]` | `:enter/step-5-link-account` | `PLAN.md:enter-step-5-link` | H(Acc=[:google "sub-1"]) E5 finalizes AccountLinked(P=:P1) with monotonic :says -> ack-return |
| 9 | `:c3` (#3) | `[:account [:google "sub-1"]]` | `:enter/step-1-check-account` | `PLAN.md:enter-step-1-returning` | H(Acc=[:google "sub-1"]) sees AccountLinked(P=:P1) with at=50 <= 200 -> hops \|hash P for :own-layer-made? |
| 9 | `[:c2]` (#101) | `[:root :P1]` | `:FAULT/all-after-replay` | `PLAN.md:195, stream.md:96` | Rama :all-after / uncheckpointed replay on [:root :P1] from idx=0 for calls [:c2] |
| 10 | `:c2` (#101) | `[:root :P1]` | `:own-layer-made/step-1` | `PLAN.md:own-layer-made` | H(P=:P1) sets :own-layer-made? true -> ack-return {:made :P1} |
| 11 | `:c3` (#3) | `[:root :P1]` | `:enter/step-1b-returning-read-flag` | `PLAN.md:enter-step-1b-flag` | H(P=:P1) reads :own-layer-made?=true -> ack-return {:person :P1 ...} |

## Two-Fault Per-Run Table

| Scenario | 2-Fault Mode | States | Terminal histories | Capped? | Violations |
|---|---|---|---|---|---|
| 2C-01: two invites of different persons on one claim | `individual` | 328 | 25 | false | 0 |
| 2C-01: two invites of different persons on one claim | `all-after` | 251 | 11 | false | 0 |
| 2C-01: two invites of different persons on one claim | `timeout-overlap` | 1,235 | 21 | false | 0 |
| 2C-01: two invites of different persons on one claim | `mixed` | 2,893 | 41 | false | 0 |
| 2C-02: two invites of one person under two claims | `individual` | 318 | 12 | false | 0 |
| 2C-02: two invites of one person under two claims | `all-after` | 319 | 6 | false | 0 |
| 2C-02: two invites of one person under two claims | `timeout-overlap` | 1,228 | 12 | false | 0 |
| 2C-02: two invites of one person under two claims | `mixed` | 2,915 | 20 | false | 0 |
| 2C-03: duplicate invite of same (person, claim) from two copies | `individual` | 173 | 15 | false | 0 |
| 2C-03: duplicate invite of same (person, claim) from two copies | `all-after` | 66 | 3 | false | 0 |
| 2C-03: duplicate invite of same (person, claim) from two copies | `timeout-overlap` | 423 | 7 | false | 0 |
| 2C-03: duplicate invite of same (person, claim) from two copies | `mixed` | 1,133 | 23 | false | 0 |
| 2C-04: invite racing 1-claim enter proving that claim | `individual` | 418 | 12 | false | 0 |
| 2C-04: invite racing 1-claim enter proving that claim | `all-after` | 477 | 6 | false | 0 |
| 2C-04: invite racing 1-claim enter proving that claim | `timeout-overlap` | 2,309 | 12 | false | 0 |
| 2C-04: invite racing 1-claim enter proving that claim | `mixed` | 5,305 | 20 | false | 0 |
| 2C-05: two 1-claim enters of one account proving C1 and C2 (clocks differ: t1=200 vs t2=100) | `individual` | 515 | 7 | false | 0 |
| 2C-05: two 1-claim enters of one account proving C1 and C2 (clocks differ: t1=200 vs t2=100) | `all-after` | 161 | 3 | false | 0 |
| 2C-05: two 1-claim enters of one account proving C1 and C2 (clocks differ: t1=200 vs t2=100) | `timeout-overlap` | 4,528 | 7 | false | 0 |
| 2C-05: two 1-claim enters of one account proving C1 and C2 (clocks differ: t1=200 vs t2=100) | `mixed` | 7,290 | 11 | false | 0 |
| 2C-06: two returning enters of linked account with differing clocks (t1=300 vs t2=100) | `individual` | 174 | 7 | false | 0 |
| 2C-06: two returning enters of linked account with differing clocks (t1=300 vs t2=100) | `all-after` | 108 | 3 | false | 0 |
| 2C-06: two returning enters of linked account with differing clocks (t1=300 vs t2=100) | `timeout-overlap` | 1,042 | 7 | false | 0 |
| 2C-06: two returning enters of linked account with differing clocks (t1=300 vs t2=100) | `mixed` | 1,556 | 11 | false | 0 |
| 2C-16: two sequential returning enters of linked account with equal :at=200 and different :says | `individual` | 32 | 6 | false | 0 |
| 2C-16: two sequential returning enters of linked account with equal :at=200 and different :says | `all-after` | 39 | 3 | false | 0 |
| 2C-16: two sequential returning enters of linked account with equal :at=200 and different :says | `timeout-overlap` | 94 | 6 | false | 0 |
| 2C-16: two sequential returning enters of linked account with equal :at=200 and different :says | `mixed` | 211 | 11 | false | 0 |
| 2C-07: two accounts proving one claim (1-claim enters) | `individual` | 1,089 | 12 | false | 0 |
| 2C-07: two accounts proving one claim (1-claim enters) | `all-after` | 969 | 6 | false | 0 |
| 2C-07: two accounts proving one claim (1-claim enters) | `timeout-overlap` | 11,611 | 12 | false | 0 |
| 2C-07: two accounts proving one claim (1-claim enters) | `mixed` | 21,939 | 20 | false | 0 |
| 2C-08: two accounts proving shared code claim (2-claim fallback to email) | `individual` | 1,286 | 12 | false | 0 |
| 2C-08: two accounts proving shared code claim (2-claim fallback to email) | `all-after` | 1,074 | 6 | false | 0 |
| 2C-08: two accounts proving shared code claim (2-claim fallback to email) | `timeout-overlap` | 14,784 | 12 | false | 0 |
| 2C-08: two accounts proving shared code claim (2-claim fallback to email) | `mixed` | 27,635 | 20 | false | 0 |
| 2C-09: 2-claim enter (code nil, email waiting) racing invite on the code claim | `individual` | 625 | 12 | false | 0 |
| 2C-09: 2-claim enter (code nil, email waiting) racing invite on the code claim | `all-after` | 557 | 6 | false | 0 |
| 2C-09: 2-claim enter (code nil, email waiting) racing invite on the code claim | `timeout-overlap` | 3,980 | 12 | false | 0 |
| 2C-09: 2-claim enter (code nil, email waiting) racing invite on the code claim | `mixed` | 8,517 | 20 | false | 0 |
| 2C-10: returning enter racing own-layer-made | `individual` | 37 | 6 | false | 0 |
| 2C-10: returning enter racing own-layer-made | `all-after` | 69 | 6 | false | 0 |
| 2C-10: returning enter racing own-layer-made | `timeout-overlap` | 130 | 6 | false | 0 |
| 2C-10: returning enter racing own-layer-made | `mixed` | 284 | 12 | false | 0 |
| 2C-11: two persons answering one ask yes with new agents at once | `individual` | 99 | 7 | false | 0 |
| 2C-11: two persons answering one ask yes with new agents at once | `all-after` | 57 | 3 | false | 0 |
| 2C-11: two persons answering one ask yes with new agents at once | `timeout-overlap` | 348 | 7 | false | 0 |
| 2C-11: two persons answering one ask yes with new agents at once | `mixed` | 615 | 11 | false | 0 |
| 2C-12: same person P1 answers ask from two copies with differing clocks (t1=500, t2=2000; G0/S0 expire at 1500) | `individual` | 92 | 7 | false | 0 |
| 2C-12: same person P1 answers ask from two copies with differing clocks (t1=500, t2=2000; G0/S0 expire at 1500) | `all-after` | 55 | 3 | false | 0 |
| 2C-12: same person P1 answers ask from two copies with differing clocks (t1=500, t2=2000; G0/S0 expire at 1500) | `timeout-overlap` | 321 | 7 | false | 0 |
| 2C-12: same person P1 answers ask from two copies with differing clocks (t1=500, t2=2000; G0/S0 expire at 1500) | `mixed` | 635 | 11 | false | 0 |
| 2C-13: answer-ask yes for existing agent A1 racing end-actor(A1) | `individual` | 42 | 6 | false | 0 |
| 2C-13: answer-ask yes for existing agent A1 racing end-actor(A1) | `all-after` | 81 | 6 | false | 0 |
| 2C-13: answer-ask yes for existing agent A1 racing end-actor(A1) | `timeout-overlap` | 112 | 6 | false | 0 |
| 2C-13: answer-ask yes for existing agent A1 racing end-actor(A1) | `mixed` | 331 | 12 | false | 0 |
| 2C-14: two answers straddling ask :ends (P1 at=500 before ends=1000, P2 at=1200 after ends) | `individual` | 99 | 7 | false | 0 |
| 2C-14: two answers straddling ask :ends (P1 at=500 before ends=1000, P2 at=1200 after ends) | `all-after` | 57 | 3 | false | 0 |
| 2C-14: two answers straddling ask :ends (P1 at=500 before ends=1000, P2 at=1200 after ends) | `timeout-overlap` | 348 | 7 | false | 0 |
| 2C-14: two answers straddling ask :ends (P1 at=500 before ends=1000, P2 at=1200 after ends) | `mixed` | 615 | 11 | false | 0 |
| 3C-01: three invites (P1,C1), (P2,C1), and (P1,C2) — refusal order & helping | `individual` | 3,483 | 44 | false | 0 |
| 3C-01: three invites (P1,C1), (P2,C1), and (P1,C2) — refusal order & helping | `all-after` | 2,931 | 12 | false | 0 |
| 3C-01: three invites (P1,C1), (P2,C1), and (P1,C2) — refusal order & helping | `timeout-overlap` | 15,335 | 44 | false | 0 |
| 3C-02: sequential causal chain invite(P2,C1) -> invite(P1,C2) while invite(P1,C1) in flight | `individual` | 1,042 | 33 | false | 0 |
| 3C-02: sequential causal chain invite(P2,C1) -> invite(P1,C2) while invite(P1,C1) in flight | `all-after` | 1,148 | 9 | false | 0 |
| 3C-02: sequential causal chain invite(P2,C1) -> invite(P1,C2) while invite(P1,C1) in flight | `timeout-overlap` | 4,421 | 33 | false | 0 |
| 3C-03: 1-claim enter(Acc1,C1) + 1-claim enter(Acc1,C2) + invite(P1,C1) | `individual` | 6,586 | 27 | false | 0 |
| 3C-03: 1-claim enter(Acc1,C1) + 1-claim enter(Acc1,C2) + invite(P1,C1) | `all-after` | 2,960 | 6 | false | 0 |
| 3C-03: 1-claim enter(Acc1,C1) + 1-claim enter(Acc1,C2) + invite(P1,C1) | `timeout-overlap` | 50,672 | 27 | false | 0 |
| 3C-04: 2-claim enter(Acc1, code=C_CODE, email=C1) + enter(Acc1, C1) + invite(P1, C_CODE) | `individual` | 9,240 | 22 | false | 0 |
| 3C-04: 2-claim enter(Acc1, code=C_CODE, email=C1) + enter(Acc1, C1) + invite(P1, C_CODE) | `all-after` | 3,467 | 6 | false | 0 |
| 3C-04: 2-claim enter(Acc1, code=C_CODE, email=C1) + enter(Acc1, C1) + invite(P1, C_CODE) | `timeout-overlap` | 95,433 | 22 | false | 0 |
| 3C-05: 2-claim enter(Acc1) + 1-claim enter(Acc2, C_CODE) + 1-claim enter(Acc3, C_CODE) | `individual` | 29,625 | 30 | false | 0 |
| 3C-05: 2-claim enter(Acc1) + 1-claim enter(Acc2, C_CODE) + 1-claim enter(Acc3, C_CODE) | `all-after` | 22,249 | 9 | false | 0 |
| 3C-05: 2-claim enter(Acc1) + 1-claim enter(Acc2, C_CODE) + 1-claim enter(Acc3, C_CODE) | `timeout-overlap` | 250,000 | 6 | true | 0 |
| 3C-06: Trace E-3 continuation — Acc2 and Acc3 (1-claim & 2-claim) meet stale ClaimClaiming on C_CODE | `individual` | 1,457 | 12 | false | 0 |
| 3C-06: Trace E-3 continuation — Acc2 and Acc3 (1-claim & 2-claim) meet stale ClaimClaiming on C_CODE | `all-after` | 1,202 | 6 | false | 0 |
| 3C-06: Trace E-3 continuation — Acc2 and Acc3 (1-claim & 2-claim) meet stale ClaimClaiming on C_CODE | `timeout-overlap` | 32,641 | 12 | false | 0 |
| 3C-06: Trace E-3 continuation — Acc2 and Acc3 (1-claim & 2-claim) meet stale ClaimClaiming on C_CODE | `mixed` | 51,170 | 20 | false | 0 |
| 2C-15: Trace E-3 continuation — two 2-claim enters (Acc2, Acc3) evicting stale ClaimClaiming (PLAN.md:537) | `individual` | 1,851 | 12 | false | 0 |
| 2C-15: Trace E-3 continuation — two 2-claim enters (Acc2, Acc3) evicting stale ClaimClaiming (PLAN.md:537) | `all-after` | 1,412 | 6 | false | 0 |
| 2C-15: Trace E-3 continuation — two 2-claim enters (Acc2, Acc3) evicting stale ClaimClaiming (PLAN.md:537) | `timeout-overlap` | 46,737 | 12 | false | 0 |
| 2C-15: Trace E-3 continuation — two 2-claim enters (Acc2, Acc3) evicting stale ClaimClaiming (PLAN.md:537) | `mixed` | 72,544 | 20 | false | 0 |
| 3C-08: invite(P1,C1) in flight; invite(P2,C2) returns -> invite(P1,C2) helps C1 without rechecking C2 (PLAN.md:479-480) | `individual` | 396 | 10 | false | 0 |
| 3C-08: invite(P1,C1) in flight; invite(P2,C2) returns -> invite(P1,C2) helps C1 without rechecking C2 (PLAN.md:479-480) | `all-after` | 395 | 3 | false | 0 |
| 3C-08: invite(P1,C1) in flight; invite(P2,C2) returns -> invite(P1,C2) helps C1 without rechecking C2 (PLAN.md:479-480) | `timeout-overlap` | 1,018 | 10 | false | 0 |
| 3C-09: answer-ask yes with new-agent A1 racing make-actor(P1, A1) and second answer-ask | `individual` | 316 | 18 | false | 0 |
| 3C-09: answer-ask yes with new-agent A1 racing make-actor(P1, A1) and second answer-ask | `all-after` | 295 | 6 | false | 0 |
| 3C-09: answer-ask yes with new-agent A1 racing make-actor(P1, A1) and second answer-ask | `timeout-overlap` | 1,072 | 18 | false | 0 |
| 3C-07: three answers on one ask — refused answer (:no-such-actor) + yes + no | `individual` | 1,167 | 28 | false | 0 |
| 3C-07: three answers on one ask — refused answer (:no-such-actor) + yes + no | `all-after` | 514 | 6 | false | 0 |
| 3C-07: three answers on one ask — refused answer (:no-such-actor) + yes + no | `timeout-overlap` | 6,734 | 33 | false | 0 |
| X-01: 2-claim enter (code nil, email waiting) while invite(P1, C_CODE) -> Acc2 takes email | `individual` | 3,021 | 20 | false | 0 |
| X-01: 2-claim enter (code nil, email waiting) while invite(P1, C_CODE) -> Acc2 takes email | `all-after` | 3,073 | 6 | false | 0 |
| X-01: 2-claim enter (code nil, email waiting) while invite(P1, C_CODE) -> Acc2 takes email | `timeout-overlap` | 28,081 | 20 | false | 0 |
| X-01: 2-claim enter (code nil, email waiting) while invite(P1, C_CODE) -> Acc2 takes email | `mixed` | 57,417 | 30 | false | 0 |
| X-02: two 2-claim enters of one account with inverted claim order and differing clocks | `individual` | 774 | 7 | false | 0 |
| X-02: two 2-claim enters of one account with inverted claim order and differing clocks | `all-after` | 204 | 3 | false | 0 |
| X-02: two 2-claim enters of one account with inverted claim order and differing clocks | `timeout-overlap` | 7,642 | 7 | false | 0 |
| X-02: two 2-claim enters of one account with inverted claim order and differing clocks | `mixed` | 11,630 | 11 | false | 0 |
| X-03: same person P1 answers ask yes + yes from two copies with differing clocks (t1=500, t2=2000) | `individual` | 92 | 7 | false | 0 |
| X-03: same person P1 answers ask yes + yes from two copies with differing clocks (t1=500, t2=2000) | `all-after` | 55 | 3 | false | 0 |
| X-03: same person P1 answers ask yes + yes from two copies with differing clocks (t1=500, t2=2000) | `timeout-overlap` | 321 | 7 | false | 0 |
| X-03: same person P1 answers ask yes + yes from two copies with differing clocks (t1=500, t2=2000) | `mixed` | 635 | 11 | false | 0 |
| X-04: refused high-:at enter(Acc1, C1=nil, at=300) racing valid enter(Acc1, C2, at=100) | `individual` | 641 | 7 | false | 0 |
| X-04: refused high-:at enter(Acc1, C1=nil, at=300) racing valid enter(Acc1, C2, at=100) | `all-after` | 274 | 3 | false | 0 |
| X-04: refused high-:at enter(Acc1, C1=nil, at=300) racing valid enter(Acc1, C2, at=100) | `timeout-overlap` | 5,502 | 7 | false | 0 |
| X-04: refused high-:at enter(Acc1, C1=nil, at=300) racing valid enter(Acc1, C2, at=100) | `mixed` | 8,542 | 11 | false | 0 |
| X-05: answer-ask(U, yes? true) racing ask(U) on same code | `individual` | 16 | 3 | false | 0 |
| X-05: answer-ask(U, yes? true) racing ask(U) on same code | `all-after` | 58 | 5 | false | 0 |
| X-05: answer-ask(U, yes? true) racing ask(U) on same code | `timeout-overlap` | 30 | 3 | false | 0 |
| X-05: answer-ask(U, yes? true) racing ask(U) on same code | `mixed` | 190 | 11 | false | 0 |
| X-06: refused no-claim high-:at enter(Acc1, at=300) racing valid enter(Acc1, C2, at=100) | `individual` | 344 | 7 | false | 0 |
| X-06: refused no-claim high-:at enter(Acc1, at=300) racing valid enter(Acc1, C2, at=100) | `all-after` | 448 | 5 | false | 0 |
| X-06: refused no-claim high-:at enter(Acc1, at=300) racing valid enter(Acc1, C2, at=100) | `timeout-overlap` | 1,727 | 7 | false | 0 |
| X-06: refused no-claim high-:at enter(Acc1, at=300) racing valid enter(Acc1, C2, at=100) | `mixed` | 6,201 | 19 | false | 0 |
| X-07: answer-ask(U, existing A1) racing make-actor(P1, A1) | `individual` | 42 | 6 | false | 0 |
| X-07: answer-ask(U, existing A1) racing make-actor(P1, A1) | `all-after` | 84 | 6 | false | 0 |
| X-07: answer-ask(U, existing A1) racing make-actor(P1, A1) | `timeout-overlap` | 114 | 6 | false | 0 |
| X-07: answer-ask(U, existing A1) racing make-actor(P1, A1) | `mixed` | 333 | 12 | false | 0 |
| TF-01: first-time enter(Acc1, C1) racing own-layer-made(P1) + returning enter(Acc1, older :at) | `individual` | 1,026 | 25 | false | 1 |
| TF-01: first-time enter(Acc1, C1) racing own-layer-made(P1) + returning enter(Acc1, older :at) | `all-after` | 557 | 13 | false | 2 |
| TF-01: first-time enter(Acc1, C1) racing own-layer-made(P1) + returning enter(Acc1, older :at) | `timeout-overlap` | 10,974 | 25 | false | 1 |
| TF-01: first-time enter(Acc1, C1) racing own-layer-made(P1) + returning enter(Acc1, older :at) | `mixed` | 18,543 | 55 | false | 2 |
| TF-02: 2-claim enter(Acc1, code+email) losing code to Acc2 and crashing/replaying on email | `individual` | 1,286 | 12 | false | 0 |
| TF-02: 2-claim enter(Acc1, code+email) losing code to Acc2 and crashing/replaying on email | `all-after` | 1,074 | 6 | false | 0 |
| TF-02: 2-claim enter(Acc1, code+email) losing code to Acc2 and crashing/replaying on email | `timeout-overlap` | 14,784 | 12 | false | 0 |
| TF-02: 2-claim enter(Acc1, code+email) losing code to Acc2 and crashing/replaying on email | `mixed` | 27,635 | 20 | false | 0 |
| TF-03: invite(P1, C1) + invite(P2, C1) + enter(Acc1, C1) under double fault | `individual` | 5,891 | 76 | false | 0 |
| TF-03: invite(P1, C1) + invite(P2, C1) + enter(Acc1, C1) under double fault | `all-after` | 5,453 | 22 | false | 0 |
| TF-03: invite(P1, C1) + invite(P2, C1) + enter(Acc1, C1) under double fault | `timeout-overlap` | 32,543 | 66 | false | 0 |
| TF-03: invite(P1, C1) + invite(P2, C1) + enter(Acc1, C1) under double fault | `mixed` | 76,883 | 116 | false | 0 |
| TF-04: ask(U) + refused/early answer-ask(U, at=600) + late answer-ask(U, at=2000) under double fault | `individual` | 93 | 7 | false | 0 |
| TF-04: ask(U) + refused/early answer-ask(U, at=600) + late answer-ask(U, at=2000) under double fault | `all-after` | 68 | 3 | false | 0 |
| TF-04: ask(U) + refused/early answer-ask(U, at=600) + late answer-ask(U, at=2000) under double fault | `timeout-overlap` | 322 | 7 | false | 0 |
| TF-04: ask(U) + refused/early answer-ask(U, at=600) + late answer-ask(U, at=2000) under double fault | `mixed` | 658 | 11 | false | 0 |
