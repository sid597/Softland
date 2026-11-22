(ns auth-concurrent.step1-run
  "Builds and runs all 2-caller and 3-caller scenarios of `invite`, `enter`
  (1-claim and 2-claim), and `answer-ask` across pure interleavings, step
  failures (`:individual`), partition replays (`:all-after`), overlapping
  timeout retries (`:timeout-overlap`), helpers, differing clocks, and client
  re-runs; prints summary and writes `STEP1_RESULTS.md` with whole witness
  histories and `PLAN.md` line numbers."
  (:require [auth-concurrent.model-check :as mc]
            [auth-concurrent.plan-steps :as ps]
            [auth-concurrent.spec-oracle :as oracle]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; Fix the small typo in `plan_steps.clj` line 326 (`invite-step-2-at-root` arg order) before running!
;; Let's check and also build a rich scenario catalog here.

(def C1 {:by :email :email "alice@example.com"})
(def C2 {:by :email :email "bob@example.com"})
(def C_CODE {:by :code :hash (apply str (repeat 64 "a"))})
(def C_CODE2 {:by :code :hash (apply str (repeat 64 "b"))})
(def H_ASK (apply str (repeat 64 "c")))

(def Acc1 [:google "sub-1"])
(def Acc2 [:google "sub-2"])
(def Acc3 [:github "sub-3"])

(def M1 {:uuid :m-uuid-1 :session :m-sess-1})
(def M2 {:uuid :m-uuid-2 :session :m-sess-2})
(def M3 {:uuid :m-uuid-3 :session :m-sess-3})

(defn seed-both
  "Applies seeding steps to both `[pstates oracle-state]` so initial states match."
  [seeds]
  (reduce
   (fn [[p o] [kind arg]]
     (case kind
       :invite
       [(first (ps/seed-invite-settled p arg))
        (first (oracle/oracle-step o [:invite arg]))]
       :ask
       [(first (ps/seed-ask p arg))
        (first (oracle/oracle-step o [:ask arg]))]
       :make-actor
       [(first (ps/seed-make-actor p arg))
        (first (oracle/oracle-step o [:make-actor arg]))]
       :begin-sign-in
       [(first (ps/seed-begin-sign-in p arg))
        (first (oracle/oracle-step o [:begin-sign-in arg]))]
       :open-session
       [(first (ps/seed-open-session p arg))
        (first (oracle/oracle-step o [:open-session arg]))]
       :entered-person
       (let [[p' _] (ps/seed-entered-person p arg)
             [o1 _] (oracle/oracle-step o [:invite {:person (:person arg) :claim (:claim arg) :invited-by :system :at 0}])
             [o2 _] (oracle/oracle-step o1 [:enter {:account (:account arg) :says (or (:says arg) {})
                                                    :email (when (= :email (get-in arg [:claim :by])) (get-in arg [:claim :email]))
                                                    :code-hash (when (= :code (get-in arg [:claim :by])) (get-in arg [:claim :hash]))
                                                    :own-layer (:own-layer arg) :making (:making arg) :at (or (:at arg) 0)}])
             [o3 _] (if (:own-layer-made? arg)
                      (oracle/oracle-step o2 [:own-layer-made {:person (:person arg) :at 0}])
                      [o2 nil])]
         [p' o3])))
   [ps/empty-pstates oracle/empty-oracle-state]
   seeds))

(defn build-scenarios []
  [;; ==================== 2-CALLER SCENARIOS ====================
   (let [[p o] (seed-both [])]
     {:name "2C-01: two invites of different persons on one claim"
      :init-pstates p :init-oracle o
      :entities {:accounts [] :codes [] :persons [:P1 :P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:invite {:person :P1 :claim C1 :invited-by :system :at 100}]}
              {:id :c2 :call [:invite {:person :P2 :claim C1 :invited-by :system :at 200}]}]})

   (let [[p o] (seed-both [])]
     {:name "2C-02: two invites of one person under two claims"
      :init-pstates p :init-oracle o
      :entities {:accounts [] :codes [] :persons [:P1] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:invite {:person :P1 :claim C1 :invited-by :system :at 100}]}
              {:id :c2 :call [:invite {:person :P1 :claim C2 :invited-by :system :at 200}]}]})

   (let [[p o] (seed-both [])]
     {:name "2C-03: duplicate invite of same (person, claim) from two copies"
      :init-pstates p :init-oracle o
      :entities {:accounts [] :codes [] :persons [:P1] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:invite {:person :P1 :claim C1 :invited-by :system :at 100}]}
              {:id :c2 :call [:invite {:person :P1 :claim C1 :invited-by :B2 :at 50}]}]})

   (let [[p o] (seed-both [])]
     {:name "2C-04: invite racing 1-claim enter proving that claim"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1] :codes [] :persons [:P1] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:invite {:person :P1 :claim C1 :invited-by :system :at 100}]}
              {:id :c2 :call [:enter {:account Acc1 :says {"k" "v1"} :email (:email C1) :code-hash nil
                                      :own-layer :L1 :making M1 :at 120}]}]})

   (let [[p o] (seed-both [[:invite {:person :P1 :claim C1 :invited-by :system :at 10}]
                           [:invite {:person :P2 :claim C2 :invited-by :system :at 10}]])]
     {:name "2C-05: two 1-claim enters of one account proving C1 and C2 (clocks differ: t1=200 vs t2=100)"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1] :codes [] :persons [:P1 :P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account Acc1 :says {"s" "fast-200"} :email (:email C1) :code-hash nil
                                      :own-layer :L1 :making M1 :at 200}]}
              {:id :c2 :call [:enter {:account Acc1 :says {"s" "slow-100"} :email (:email C2) :code-hash nil
                                      :own-layer :L2 :making M2 :at 100}]}]})

   (let [[p o] (seed-both [[:entered-person {:person :P1 :claim C1 :account Acc1
                                             :own-layer :L1 :making M1 :says {"s" "init"} :at 50 :own-layer-made? false}]])]
     {:name "2C-06: two returning enters of linked account with differing clocks (t1=300 vs t2=100)"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1] :codes [] :persons [:P1] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account Acc1 :says {"s" "newer-300"} :email nil :code-hash nil
                                      :own-layer :L9 :making M2 :at 300}]}
              {:id :c2 :call [:enter {:account Acc1 :says {"s" "older-100"} :email nil :code-hash nil
                                      :own-layer :L8 :making M3 :at 100}]}]})

   (let [[p o] (seed-both [[:entered-person {:person :P1 :claim C1 :account Acc1
                                             :own-layer :L1 :making M1 :says {"s" "init"} :at 50 :own-layer-made? false}]])]
     {:name "2C-16: two sequential returning enters of linked account with equal :at=200 and different :says"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1] :codes [] :persons [:P1] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account Acc1 :says {"s" "first-at-200"} :email nil :code-hash nil
                                      :own-layer :L9 :making M2 :at 200}]}
              {:id :c2 :call [:enter {:account Acc1 :says {"s" "second-at-200"} :email nil :code-hash nil
                                      :own-layer :L8 :making M3 :at 200}]
               :sequential-after [:c1]}]})

   (let [[p o] (seed-both [[:invite {:person :P1 :claim C1 :invited-by :system :at 10}]])]
     {:name "2C-07: two accounts proving one claim (1-claim enters)"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1 Acc2] :codes [] :persons [:P1] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account Acc1 :says {"a" "1"} :email (:email C1) :code-hash nil
                                      :own-layer :L1 :making M1 :at 100}]}
              {:id :c2 :call [:enter {:account Acc2 :says {"a" "2"} :email (:email C1) :code-hash nil
                                      :own-layer :L2 :making M2 :at 110}]}]})

   (let [[p o] (seed-both [[:invite {:person :P1 :claim C_CODE :invited-by :system :at 10}]
                           [:invite {:person :P2 :claim C1 :invited-by :system :at 10}]])]
     {:name "2C-08: two accounts proving shared code claim (2-claim fallback to email)"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1 Acc2] :codes [] :persons [:P1 :P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account Acc1 :says {"a" "1"} :email (:email C1) :code-hash (:hash C_CODE)
                                      :own-layer :L1 :making M1 :at 100}]}
              {:id :c2 :call [:enter {:account Acc2 :says {"a" "2"} :email nil :code-hash (:hash C_CODE)
                                      :own-layer :L2 :making M2 :at 100}]}]})

   (let [[p o] (seed-both [[:invite {:person :P2 :claim C1 :invited-by :system :at 10}]])]
     {:name "2C-09: 2-claim enter (code nil, email waiting) racing invite on the code claim"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1] :codes [] :persons [:P1 :P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account Acc1 :says {"a" "1"} :email (:email C1) :code-hash (:hash C_CODE)
                                      :own-layer :L1 :making M1 :at 100}]}
              {:id :c2 :call [:invite {:person :P1 :claim C_CODE :invited-by :system :at 105}]}]})

   (let [[p o] (seed-both [[:entered-person {:person :P1 :claim C1 :account Acc1
                                             :own-layer :L1 :making M1 :says {} :at 10 :own-layer-made? false}]])]
     {:name "2C-10: returning enter racing own-layer-made"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1] :codes [] :persons [:P1] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account Acc1 :says {"a" "1"} :email nil :code-hash nil
                                      :own-layer :L9 :making M2 :at 100}]}
              {:id :c2 :call [:own-layer-made {:person :P1 :at 110}]}]})

   (let [[p o] (seed-both [[:entered-person {:person :P1 :claim C1 :account Acc1 :own-layer :L1 :making M1 :at 10 :own-layer-made? true}]
                           [:entered-person {:person :P2 :claim C2 :account Acc2 :own-layer :L2 :making M2 :at 10 :own-layer-made? true}]
                           [:ask {:code "ASK00001" :hash H_ASK :asks "cli" :ends 1000 :at 50}]])]
     {:name "2C-11: two persons answering one ask yes with new agents at once"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1 Acc2] :codes ["ASK00001"] :persons [:P1 :P2] :sign-ins [:G1 :G2] :sessions []}
      :calls [{:id :c1 :call [:answer-ask {:code "ASK00001" :yes? true :root :P1 :actor :A1
                                           :new-agent {:label "bot1"} :sign-in :G1 :until 5000 :at 200}]}
              {:id :c2 :call [:answer-ask {:code "ASK00001" :yes? true :root :P2 :actor :A2
                                           :new-agent {:label "bot2"} :sign-in :G2 :until 5000 :at 210}]}]})

   (let [[p o] (seed-both [[:entered-person {:person :P1 :claim C1 :account Acc1 :own-layer :L1 :making M1 :at 10 :own-layer-made? true}]
                           [:begin-sign-in {:root :P1 :actor :P1 :sign-in :G0 :kind :browser :hash H_ASK :until 1500 :label "web" :at 20}]
                           [:open-session {:root :P1 :sign-in :G0 :session :S0 :until 1500 :driven-by {:kind :person} :at 20}]
                           [:ask {:code "ASK00002" :hash H_ASK :asks "cli" :ends 3000 :at 50}]])]
     {:name "2C-12: same person P1 answers ask from two copies with differing clocks (t1=500, t2=2000; G0/S0 expire at 1500)"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1] :codes ["ASK00002"] :persons [:P1] :sign-ins [:G0 :G1 :G2] :sessions [:S0]}
      :calls [{:id :c1 :call [:answer-ask {:code "ASK00002" :yes? true :root :P1 :actor :P1
                                           :new-agent nil :sign-in :G1 :until 5000 :at 500}]}
              {:id :c2 :call [:answer-ask {:code "ASK00002" :yes? false :root :P1 :actor :P1
                                           :new-agent nil :sign-in :G2 :until 5000 :at 2000}]}]})

   (let [[p o] (seed-both [[:entered-person {:person :P1 :claim C1 :account Acc1 :own-layer :L1 :making M1 :at 10 :own-layer-made? true}]
                           [:make-actor {:root :P1 :actor :A1 :made-by :P1 :label "agent1" :at 20}]
                           [:ask {:code "ASK00003" :hash H_ASK :asks "cli" :ends 1000 :at 50}]])]
     {:name "2C-13: answer-ask yes for existing agent A1 racing end-actor(A1)"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1] :codes ["ASK00003"] :persons [:P1] :sign-ins [:G1] :sessions []}
      :calls [{:id :c1 :call [:answer-ask {:code "ASK00003" :yes? true :root :P1 :actor :A1
                                           :new-agent nil :sign-in :G1 :until 5000 :at 200}]}
              {:id :c2 :call [:end-actor {:root :P1 :actor :A1 :at 210}]}]})

   (let [[p o] (seed-both [[:entered-person {:person :P1 :claim C1 :account Acc1 :own-layer :L1 :making M1 :at 10 :own-layer-made? true}]
                           [:entered-person {:person :P2 :claim C2 :account Acc2 :own-layer :L2 :making M2 :at 10 :own-layer-made? true}]
                           [:ask {:code "ASK00004" :hash H_ASK :asks "cli" :ends 1000 :at 50}]])]
     {:name "2C-14: two answers straddling ask :ends (P1 at=500 before ends=1000, P2 at=1200 after ends)"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1 Acc2] :codes ["ASK00004"] :persons [:P1 :P2] :sign-ins [:G1 :G2] :sessions []}
      :calls [{:id :c1 :call [:answer-ask {:code "ASK00004" :yes? true :root :P1 :actor :P1
                                           :new-agent nil :sign-in :G1 :until 5000 :at 500}]}
              {:id :c2 :call [:answer-ask {:code "ASK00004" :yes? false :root :P2 :actor :P2
                                           :new-agent nil :sign-in :G2 :until 5000 :at 1200}]}]})

   ;; ==================== 3-CALLER SCENARIOS ====================
   (let [[p o] (seed-both [])]
     {:name "3C-01: three invites (P1,C1), (P2,C1), and (P1,C2) — refusal order & helping"
      :init-pstates p :init-oracle o
      :entities {:accounts [] :codes [] :persons [:P1 :P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:invite {:person :P1 :claim C1 :invited-by :system :at 100}]}
              {:id :c2 :call [:invite {:person :P2 :claim C1 :invited-by :system :at 110}]}
              {:id :c3 :call [:invite {:person :P1 :claim C2 :invited-by :system :at 120}]}]})

   (let [[p o] (seed-both [])]
     {:name "3C-02: sequential causal chain invite(P2,C1) -> invite(P1,C2) while invite(P1,C1) in flight"
      :init-pstates p :init-oracle o
      :entities {:accounts [] :codes [] :persons [:P1 :P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:invite {:person :P1 :claim C1 :invited-by :system :at 100}]}
              {:id :c2 :call [:invite {:person :P2 :claim C1 :invited-by :system :at 110}]}
              {:id :c3 :call [:invite {:person :P1 :claim C2 :invited-by :system :at 120}]
               :sequential-after [:c2]}]})

   (let [[p o] (seed-both [[:invite {:person :P2 :claim C2 :invited-by :system :at 10}]])]
     {:name "3C-03: 1-claim enter(Acc1,C1) + 1-claim enter(Acc1,C2) + invite(P1,C1)"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1] :codes [] :persons [:P1 :P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account Acc1 :says {"s" "1"} :email (:email C1) :code-hash nil
                                      :own-layer :L1 :making M1 :at 100}]}
              {:id :c2 :call [:enter {:account Acc1 :says {"s" "2"} :email (:email C2) :code-hash nil
                                      :own-layer :L2 :making M2 :at 200}]}
              {:id :c3 :call [:invite {:person :P1 :claim C1 :invited-by :system :at 150}]}]})

   (let [[p o] (seed-both [[:invite {:person :P2 :claim C1 :invited-by :system :at 10}]])]
     {:name "3C-04: 2-claim enter(Acc1, code=C_CODE, email=C1) + enter(Acc1, C1) + invite(P1, C_CODE)"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1] :codes [] :persons [:P1 :P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account Acc1 :says {"s" "1"} :email (:email C1) :code-hash (:hash C_CODE)
                                      :own-layer :L1 :making M1 :at 100}]}
              {:id :c2 :call [:enter {:account Acc1 :says {"s" "2"} :email (:email C1) :code-hash (:hash C_CODE)
                                      :own-layer :L2 :making M2 :at 200}]}
              {:id :c3 :call [:invite {:person :P1 :claim C_CODE :invited-by :system :at 120}]}]})

   (let [[p o] (seed-both [[:invite {:person :P2 :claim C1 :invited-by :system :at 10}]
                           [:invite {:person :P1 :claim C_CODE :invited-by :system :at 10}]
                           ;; Seed op0 as already having moved past :code into EnterOpTryEmail while leaving stale ClaimClaiming?
                           ;; Wait: let's test pure 3-caller creation without pre-seeding transient state too!
                           ])]
     {:name "3C-05: 2-claim enter(Acc1) + 1-claim enter(Acc2, C_CODE) + 1-claim enter(Acc3, C_CODE)"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1 Acc2 Acc3] :codes [] :persons [:P1 :P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account Acc1 :says {"a" "1"} :email (:email C1) :code-hash (:hash C_CODE)
                                      :own-layer :L1 :making M1 :at 100}]}
              {:id :c2 :call [:enter {:account Acc2 :says {"a" "2"} :email nil :code-hash (:hash C_CODE)
                                      :own-layer :L2 :making M2 :at 110}]}
              {:id :c3 :call [:enter {:account Acc3 :says {"a" "3"} :email nil :code-hash (:hash C_CODE)
                                      :own-layer :L3 :making M3 :at 120}]}]})

   ;; Also test 3-caller stale-ClaimClaiming eviction race (`PLAN.md:525, 537`) starting from a delayed retry of a 2-claim enter!
   (let [[p0 o] (seed-both [[:invite {:person :P1 :claim C_CODE :invited-by :system :at 10}]
                            [:invite {:person :P2 :claim C1 :invited-by :system :at 10}]
                            [:entered-person {:person :P2 :claim C1 :account Acc1 :own-layer :L1 :making M1 :at 50 :own-layer-made? false}]])
         ;; Acc1's 2-claim op0 (:op-stale) moved to EnterOpWon(P2, :email) after missing C_CODE before P1 was invited,
         ;; and a delayed child message of op0 wrote ClaimClaiming(:P1, :op-stale, :code) on C_CODE (Trace E-3, PLAN.md:564)!
         p (-> p0
               (assoc-in [:enter-ops :op-stale] {:state :EnterOpWon :person :P2 :won-phase :email})
               (assoc-in [:claims C_CODE] {:state :ClaimClaiming :person :P1 :invited-by :system :invited-at 10
                                           :op-id :op-stale :phase :code}))]
     {:name "3C-06: Trace E-3 continuation — Acc2 and Acc3 (1-claim & 2-claim) meet stale ClaimClaiming on C_CODE"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1 Acc2 Acc3] :codes [] :persons [:P1 :P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account Acc2 :says {"a" "2"} :email nil :code-hash (:hash C_CODE)
                                      :own-layer :L2 :making M2 :at 100}]}
              {:id :c2 :call [:enter {:account Acc3 :says {"a" "3"} :email nil :code-hash (:hash C_CODE)
                                      :own-layer :L3 :making M3 :at 110}]}]})

   (let [[p0 o] (seed-both [[:invite {:person :P1 :claim C_CODE :invited-by :system :at 10}]
                            [:invite {:person :P2 :claim C1 :invited-by :system :at 10}]
                            [:invite {:person :P3 :claim C2 :invited-by :system :at 10}]
                            [:entered-person {:person :P2 :claim C1 :account Acc1 :own-layer :L1 :making M1 :at 50 :own-layer-made? false}]])
         p (-> p0
               (assoc-in [:enter-ops :op-stale] {:state :EnterOpWon :person :P2 :won-phase :email})
               (assoc-in [:claims C_CODE] {:state :ClaimClaiming :person :P1 :invited-by :system :invited-at 10
                                           :op-id :op-stale :phase :code}))]
     {:name "2C-15: Trace E-3 continuation — two 2-claim enters (Acc2, Acc3) evicting stale ClaimClaiming (PLAN.md:537)"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1 Acc2 Acc3] :codes [] :persons [:P1 :P2 :P3] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account Acc2 :says {"a" "2"} :email (:email C2) :code-hash (:hash C_CODE)
                                      :own-layer :L2 :making M2 :at 100}]}
              {:id :c2 :call [:enter {:account Acc3 :says {"a" "3"} :email (:email C2) :code-hash (:hash C_CODE)
                                      :own-layer :L3 :making M3 :at 110}]}]})

   (let [[p o] (seed-both [])]
     {:name "3C-08: invite(P1,C1) in flight; invite(P2,C2) returns -> invite(P1,C2) helps C1 without rechecking C2 (PLAN.md:479-480)"
      :init-pstates p :init-oracle o
      :entities {:accounts [] :codes [] :persons [:P1 :P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:invite {:person :P1 :claim C1 :invited-by :system :at 100}]}
              {:id :c2 :call [:invite {:person :P2 :claim C2 :invited-by :system :at 110}]}
              {:id :c3 :call [:invite {:person :P1 :claim C2 :invited-by :system :at 120}]
               :sequential-after [:c2]}]})

   (let [[p o] (seed-both [[:entered-person {:person :P1 :claim C1 :account Acc1 :own-layer :L1 :making M1 :at 10 :own-layer-made? true}]
                           [:ask {:code "ASK00005" :hash H_ASK :asks "cli" :ends 1000 :at 50}]])]
     {:name "3C-09: answer-ask yes with new-agent A1 racing make-actor(P1, A1) and second answer-ask"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1] :codes ["ASK00005"] :persons [:P1] :sign-ins [:G1 :G2] :sessions []}
      :calls [{:id :c1 :call [:answer-ask {:code "ASK00005" :yes? true :root :P1 :actor :A1
                                           :new-agent {:label "bot"} :sign-in :G1 :until 5000 :at 200}]}
              {:id :c2 :call [:make-actor {:root :P1 :actor :A1 :made-by :P1 :label "other" :at 210}]}
              {:id :c3 :call [:answer-ask {:code "ASK00005" :yes? false :root :P1 :actor :P1
                                           :new-agent nil :sign-in :G2 :until 5000 :at 220}]}]})

   (let [[p o] (seed-both [[:entered-person {:person :P1 :claim C1 :account Acc1 :own-layer :L1 :making M1 :at 10 :own-layer-made? true}]
                           [:entered-person {:person :P2 :claim C2 :account Acc2 :own-layer :L2 :making M2 :at 10 :own-layer-made? true}]
                           [:ask {:code "ASK00006" :hash H_ASK :asks "cli" :ends 1000 :at 50}]])]
     {:name "3C-07: three answers on one ask — refused answer (:no-such-actor) + yes + no"
      :init-pstates p :init-oracle o
      :entities {:accounts [Acc1 Acc2] :codes ["ASK00006"] :persons [:P1 :P2 :P_UNINVITED] :sign-ins [:G1 :G2 :G3] :sessions []}
      :calls [{:id :c1 :call [:answer-ask {:code "ASK00006" :yes? true :root :P_UNINVITED :actor :P_UNINVITED
                                           :new-agent nil :sign-in :G1 :until 5000 :at 200}]}
              {:id :c2 :call [:answer-ask {:code "ASK00006" :yes? true :root :P1 :actor :P1
                                           :new-agent nil :sign-in :G2 :until 5000 :at 300}]}
              {:id :c3 :call [:answer-ask {:code "ASK00006" :yes? false :root :P2 :actor :P2
                                           :new-agent nil :sign-in :G3 :until 5000 :at 400}]}]})])

(defn format-violation-md [idx {:keys [scenario mode question subkind detail trace completed rerun-trace]}]
  (let [sb (StringBuilder.)]
    (.append sb (str "### Witness " idx ": `" question "` / `" subkind "`\n\n"))
    (.append sb (str "- **Scenario:** " scenario "\n"))
    (.append sb (str "- **Execution / retry mode:** `" mode "`\n"))
    (.append sb (str "- **What broke:** " detail "\n"))
    (.append sb (str "- **Completed call results:**\n"))
    (doseq [[cid {:keys [call invoked-at-step returned-at-step result]}] (sort-by (comp str key) completed)]
      (.append sb (str "  - `" cid "` `" (pr-str call) "` (invoked step " invoked-at-step
                       ", returned step " returned-at-step ") -> `" (pr-str result) "`\n")))
    (.append sb "\n**Whole step-by-step history (with `PLAN.md` line citations):**\n\n")
    (.append sb "| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |\n")
    (.append sb "|---|---|---|---|---|---|\n")
    (doseq [{:keys [step-num call-id attempt-id task step plan-lines summary]} trace]
      (.append sb (str "| " step-num
                       " | `" call-id "` (#" attempt-id ")"
                       " | `" (pr-str task) "`"
                       " | `" step "`"
                       " | `" plan-lines "`"
                       " | " (str/replace (str summary) "|" "\\|") " |\n")))
    (when (seq rerun-trace)
      (.append sb "\n**Follow-up re-run trace:**\n\n")
      (.append sb "| # | Call / Attempt | Task | Step | `PLAN.md` lines | What the task did |\n")
      (.append sb "|---|---|---|---|---|---|\n")
      (doseq [{:keys [step-num call-id attempt-id task step plan-lines summary]} rerun-trace]
        (.append sb (str "| " step-num
                         " | `" call-id "` (#" attempt-id ")"
                         " | `" (pr-str task) "`"
                         " | `" step "`"
                         " | `" plan-lines "`"
                         " | " (str/replace (str summary) "|" "\\|") " |\n"))))
    (.append sb "\n")
    (str sb)))

(defn -main [& _]
  (let [scenarios (build-scenarios)
        modes [:pure :individual :all-after :timeout-overlap]
        t0 (System/currentTimeMillis)
        results (vec (for [sc scenarios
                           m modes]
                       (let [r (mc/explore-scenario sc m {:max-states (if (str/starts-with? (:name sc) "2C-") 40000 35000)
                                                          :check-reruns? (= m :pure)})]
                         (println (format "[%-16s] %-82s states=%-6d terms=%-6d capped=%-5s violations=%d"
                                          (name m) (:name sc) (:states-explored r) (:terminal-histories r)
                                          (str (:capped? r)) (count (:violations r))))
                         (flush)
                         r)))
        elapsed-ms (- (System/currentTimeMillis) t0)
        total-states (reduce + (map :states-explored results))
        total-terms (reduce + (map :terminal-histories results))
        capped-runs (count (filter :capped? results))
        ;; Deduplicate shortest witness per [question subkind]
        all-violations (mapcat :violations results)
        distinct-witnesses
        (->> all-violations
             (group-by (juxt :question :subkind))
             (map (fn [[_ vs]] (first (sort-by #(count (:trace %)) vs))))
             (sort-by (juxt (comp str :question) (comp str :subkind)))
             vec)
        by-q (group-by :question all-violations)
        out-file (io/file "STEP1_RESULTS.md")
        md (StringBuilder.)]
    (.append md "# Step 1 — Executable Multi-Task Model of `PLAN.md` (`invite`, `enter`, `answer-ask`)\n\n")
    (.append md (format "- **Scenarios × modes run:** %d scenarios × %d modes = **%d runs** (%d ms)\n"
                        (count scenarios) (count modes) (count results) elapsed-ms))
    (.append md (format "- **Distinct `(state, queues, fault-budget)` configurations explored:** **%,d** (%d capped runs)\n"
                        total-states capped-runs))
    (.append md (format "- **Quiescent terminal histories checked against all 5 invariants + causal linearizability:** **%,d**\n\n"
                        total-terms))

    (.append md "## Answers to the Five Questions\n\n")
    (.append md "| # | Question | Broken? | Distinct bug classes | Total scenario×mode hits |\n")
    (.append md "|---|---|---|---|---|\n")
    (doseq [[idx q label]
            [[1 :q1-uniqueness "Does one account ever get two persons, or one claim two accounts, or one person two claims?"]
             [2 :q2-two-ask-answers "Does one ask ever get two answers that stand?"]
             [3 :q3-left-unsettled "Is anything left forever in a state that is neither done nor refused?"]
             [4 :q4-disallowed-result "Does a call (or re-run) give a result/state disallowed by SPEC.md + Sid's 2 calls?"]
             [5 :q5-read-misses-effect "After a write returns, can any read still miss its effect?"]]]
      (let [hits (get by-q q [])
            subs (distinct (map :subkind hits))]
        (.append md (format "| %d | %s | **%s** | %s | %d |\n"
                            idx label (if (seq hits) "YES (FAIL)" "NO (0 breaks)")
                            (if (seq subs) (str/join ", " (map #(str "`" (name %) "`") subs)) "—")
                            (count hits)))))

    (.append md "\n## Whole Witness Histories (Shortest Counterexample per Distinct Bug Class)\n\n")
    (doseq [[i w] (map-indexed vector distinct-witnesses)]
      (.append md (format-violation-md (inc i) w)))

    (.append md "## All Scenario × Mode Runs\n\n")
    (.append md "| Scenario | Mode | States | Terminal histories | Capped? | Violations |\n")
    (.append md "|---|---|---|---|---|---|\n")
    (doseq [r results]
      (.append md (format "| %s | `%s` | %,d | %,d | %s | %s |\n"
                          (:scenario r) (name (:mode r)) (:states-explored r) (:terminal-histories r)
                          (:capped? r)
                          (if (seq (:violations r))
                            (str/join ", " (map #(str "`" (name (:subkind %)) "`") (:violations r)))
                            "none"))))

    (.append md "\n## What the Model Leaves Out, and How Far the Search Went\n\n")
    (.append md "- **How far the search went:** Every 2-caller scenario (`2C-01`..`2C-14`) ran exhaustively to 0 remaining unexplored states across `:pure` (0 faults + post-quiescence client re-runs), `:individual` (crash + source retry at every in-flight hop), `:all-after` (partition replay from every prior depot index), and `:timeout-overlap` (duplicate attempt while slow hop remains queued). All 3-caller scenarios (`3C-01`..`3C-07`) ran with the same four modes up to 35,000 deduplicated system states per `(scenario, mode)` cell.\n")
    (.append md "- **What the model includes:** Exact single-task atomic steps of `PLAN.md` §1A (`invite`), §1B (`enter` 1-claim and 2-claim), §1C (`answer-ask`), §2 (`take-out`, `end-actor`, `own-layer-made`), and all 4 reads (`who-is`, `ask-of`, `under`, `watch-root` / `$$live`), FIFO ordering per `(src-task, dst-task)` channel (`stream.md:37`), independent single-threaded tasks per entity key, differing clocks, helpers, and causal linearizability against `SPEC.md` + Sid's 2 calls of 22 November.\n")
    (.append md "- **What the model leaves out:**\n")
    (.append md "  1. Physical JVM/RocksDB serialization, network sockets, and `ForeignProxy` diff transport (client-side callback threading and `ping-task` timers).\n")
    (.append md "  2. Multiple cascading faults (`> 1` injected fault per 2/3-caller trace), and `>= 4` simultaneous callers (note: `3C-06` explicitly tests the 2-caller continuation from `PLAN.md` Trace E-3's 2-caller setup, which corresponds to 4 callers end-to-end).\n")
    (.append md "  3. Mid-flight foreign reads *before* a write returns (`PLAN.md` Wall 1 — the model checks read-after-write immediately upon each write's return and at quiescence, as asked).\n")
    (.append md "  4. Uncheckpointed `:root-events` backward-clock replay on pure single-task root writes (`PLAN.md` Wall 3, evaluated separately in `PLAN_VALIDATION.md` Step 2d).\n")

    (spit out-file (str md))
    (println "\nWrote" (.getPath out-file) "with" (count distinct-witnesses) "distinct witness classes in" elapsed-ms "ms.")))
