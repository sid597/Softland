(ns agreement.check
  "Runs carbon's unchanged `auth-concurrent.model-check`,
  `auth-concurrent.spec-oracle`, and `auth-concurrent.step1-run/build-scenarios`
  over the revised `auth-concurrent.plan-steps` in `spine/auth/agreement/src/`,
  plus additional targeted causal-chain scenarios that stress the revised
  protocol. Writes `RESULTS.md` in `spine/auth/agreement/` and exits non-zero
  if any violation or capped run occurs."
  (:require [auth-concurrent.model-check :as mc]
            [auth-concurrent.plan-steps :as ps]
            [auth-concurrent.spec-oracle :as oracle]
            [auth-concurrent.step1-run :as s1]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn extra-scenarios
  "Additional scenarios beyond carbon's 25, targeting edge cases of the revised
  choose-at-account `enter` and `invite` protocols."
  []
  [(let [[p o] (s1/seed-both [[:invite {:person :P2 :claim s1/C1 :invited-by :system :at 10}]])]
     {:name "X-01: 2-claim enter (code nil, email waiting) while invite(P1, C_CODE) -> Acc2 takes email"
      :init-pstates p :init-oracle o
      :entities {:accounts [s1/Acc1 s1/Acc2] :codes [] :persons [:P1 :P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account s1/Acc1 :says {"a" "1"} :email (:email s1/C1) :code-hash (:hash s1/C_CODE)
                                      :own-layer :L1 :making s1/M1 :at 100}]}
              {:id :c2 :call [:invite {:person :P1 :claim s1/C_CODE :invited-by :system :at 105}]}
              {:id :c3 :call [:enter {:account s1/Acc2 :says {"a" "2"} :email (:email s1/C1) :code-hash nil
                                      :own-layer :L2 :making s1/M2 :at 110}]
               :sequential-after [:c2]}]})

   (let [[p o] (s1/seed-both [[:invite {:person :P1 :claim s1/C1 :invited-by :system :at 10}]
                              [:invite {:person :P2 :claim s1/C2 :invited-by :system :at 10}]])]
     {:name "X-02: two 2-claim enters of one account with inverted claim order and differing clocks"
      :init-pstates p :init-oracle o
      :entities {:accounts [s1/Acc1] :codes [] :persons [:P1 :P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account s1/Acc1 :says {"s" "c1-100"} :email (:email s1/C1) :code-hash (:hash s1/C_CODE)
                                      :own-layer :L1 :making s1/M1 :at 100}]}
              {:id :c2 :call [:enter {:account s1/Acc1 :says {"s" "c2-250"} :email (:email s1/C2) :code-hash (:hash s1/C_CODE2)
                                      :own-layer :L2 :making s1/M2 :at 250}]}]})

   (let [[p o] (s1/seed-both [[:entered-person {:person :P1 :claim s1/C1 :account s1/Acc1 :own-layer :L1 :making s1/M1 :at 10 :own-layer-made? true}]
                              [:begin-sign-in {:root :P1 :actor :P1 :sign-in :G0 :kind :browser :hash s1/H_ASK :until 1500 :label "web" :at 20}]
                              [:open-session {:root :P1 :sign-in :G0 :session :S0 :until 1500 :driven-by {:kind :person} :at 20}]
                              [:ask {:code "ASK00099" :hash s1/H_ASK :asks "cli" :ends 3000 :at 50}]])]
     {:name "X-03: same person P1 answers ask yes + yes from two copies with differing clocks (t1=500, t2=2000)"
      :init-pstates p :init-oracle o
      :entities {:accounts [s1/Acc1] :codes ["ASK00099"] :persons [:P1] :sign-ins [:G0 :G1 :G2] :sessions [:S0]}
      :calls [{:id :c1 :call [:answer-ask {:code "ASK00099" :yes? true :root :P1 :actor :P1
                                           :new-agent nil :sign-in :G1 :until 5000 :at 500}]}
              {:id :c2 :call [:answer-ask {:code "ASK00099" :yes? true :root :P1 :actor :P1
                                           :new-agent nil :sign-in :G2 :until 5000 :at 2000}]}]})

   (let [[p o] (s1/seed-both [[:invite {:person :P2 :claim s1/C2 :invited-by :system :at 10}]])]
     {:name "X-04: refused high-:at enter(Acc1, C1=nil, at=300) racing valid enter(Acc1, C2, at=100)"
      :init-pstates p :init-oracle o
      :entities {:accounts [s1/Acc1] :codes [] :persons [:P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account s1/Acc1 :says {"s" "refused-300"} :email (:email s1/C1) :code-hash nil
                                      :own-layer :L1 :making s1/M1 :at 300}]}
              {:id :c2 :call [:enter {:account s1/Acc1 :says {"s" "linked-100"} :email (:email s1/C2) :code-hash nil
                                      :own-layer :L2 :making s1/M2 :at 100}]}]})

   (let [[p o] (s1/seed-both [[:entered-person {:person :P1 :claim s1/C1 :account s1/Acc1 :own-layer :L1 :making s1/M1 :at 10 :own-layer-made? true}]])]
     {:name "X-05: answer-ask(U, yes? true) racing ask(U) on same code"
      :init-pstates p :init-oracle o
      :entities {:accounts [s1/Acc1] :codes ["ASK00100"] :persons [:P1] :sign-ins [:G1] :sessions []}
      :calls [{:id :c1 :call [:answer-ask {:code "ASK00100" :yes? true :root :P1 :actor :P1
                                           :new-agent nil :sign-in :G1 :until 5000 :at 100}]}
              {:id :c2 :call [:ask {:code "ASK00100" :hash s1/H_ASK :asks "cli" :ends 3000 :at 100}]}]})

   (let [[p o] (s1/seed-both [[:invite {:person :P2 :claim s1/C2 :invited-by :system :at 10}]])]
     {:name "X-06: refused no-claim high-:at enter(Acc1, at=300) racing valid enter(Acc1, C2, at=100)"
      :init-pstates p :init-oracle o
      :entities {:accounts [s1/Acc1] :codes [] :persons [:P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account s1/Acc1 :says {"s" "no-claim-300"} :email nil :code-hash nil
                                      :own-layer :L1 :making s1/M1 :at 300}]}
              {:id :c2 :call [:enter {:account s1/Acc1 :says {"s" "linked-100"} :email (:email s1/C2) :code-hash nil
                                      :own-layer :L2 :making s1/M2 :at 100}]}]})

   (let [[p o] (s1/seed-both [[:entered-person {:person :P1 :claim s1/C1 :account s1/Acc1 :own-layer :L1 :making s1/M1 :at 10 :own-layer-made? true}]
                              [:ask {:code "ASK00101" :hash s1/H_ASK :asks "cli" :ends 3000 :at 50}]])]
     {:name "X-07: answer-ask(U, existing A1) racing make-actor(P1, A1)"
      :init-pstates p :init-oracle o
      :entities {:accounts [s1/Acc1] :codes ["ASK00101"] :persons [:P1] :sign-ins [:G1] :sessions []}
      :calls [{:id :c1 :call [:answer-ask {:code "ASK00101" :yes? true :root :P1 :actor :A1
                                           :new-agent nil :sign-in :G1 :until 5000 :at 100}]}
              {:id :c2 :call [:make-actor {:root :P1 :actor :A1 :made-by :P1 :label "bot" :at 100}]}]})])

(defn -main [& _]
  ;; Verify we actually loaded our revised plan_steps.clj, not carbon's!
  (assert (str/includes? (:doc (meta (the-ns 'auth-concurrent.plan-steps))) "Revised single-task cross-key steps")
          "Loaded wrong auth-concurrent.plan-steps namespace!")
  (let [carbon-scenarios (s1/build-scenarios)
        extras (extra-scenarios)
        scenarios (into (vec carbon-scenarios) extras)
        modes [:pure :individual :all-after :timeout-overlap]
        t0 (System/currentTimeMillis)
        results (vec (for [sc scenarios
                           m modes]
                       (let [r (mc/explore-scenario sc m {:max-states 60000
                                                          :check-reruns? true})]
                         (println (format "[%-16s] %-86s states=%-6d terms=%-6d capped=%-5s violations=%d"
                                          (name m) (:name sc) (:states-explored r) (:terminal-histories r)
                                          (str (:capped? r)) (count (:violations r))))
                         (flush)
                         r)))
        elapsed-ms (- (System/currentTimeMillis) t0)
        total-states (reduce + (map :states-explored results))
        total-terms (reduce + (map :terminal-histories results))
        capped-runs (count (filter :capped? results))
        all-violations (mapcat :violations results)
        distinct-witnesses
        (->> all-violations
             (group-by (juxt :question :subkind))
             (map (fn [[_ vs]] (first (sort-by #(count (:trace %)) vs))))
             (sort-by (juxt (comp str :question) (comp str :subkind)))
             vec)
        by-q (group-by :question all-violations)
        out-file (io/file "RESULTS.md")
        md (StringBuilder.)]
    (.append md "# Revised Agreement — Exhaustive Multi-Task Model Check Results\n\n")
    (.append md (format "- **Scenarios × modes run:** %d scenarios (%d carbon + %d extra) × %d modes = **%d runs** (%d ms)\n"
                        (count scenarios) (count carbon-scenarios) (count extras) (count modes) (count results) elapsed-ms))
    (.append md (format "- **Client re-runs checked:** in ALL 4 modes (`check-reruns? true`)\n"))
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
    (when (seq distinct-witnesses)
      (.append md "\n## Witness Histories\n\n")
      (doseq [[i w] (map-indexed vector distinct-witnesses)]
        (.append md (s1/format-violation-md (inc i) w))))
    (.append md "\n## All Scenario × Mode Runs\n\n")
    (.append md "| Scenario | Mode | States | Terminal histories | Capped? | Violations |\n")
    (.append md "|---|---|---|---|---|---|\n")
    (doseq [r results]
      (.append md (format "| %s | `%s` | %,d | %,d | %s | %s |\n"
                          (:scenario r) (name (:mode r)) (:states-explored r) (:terminal-histories r)
                          (:capped? r)
                          (if (seq (:violations r))
                            (str/join ", " (map #(str "`" (name (:subkind %)) "`") (:violations r)))
                            "none"))))
    (spit out-file (str md))
    (println (format "\nSummary: runs=%d states=%,d terminals=%,d capped=%d violations=%d elapsed=%dms"
                     (count results) total-states total-terms capped-runs (count all-violations) elapsed-ms))
    (when (seq distinct-witnesses)
      (doseq [w distinct-witnesses]
        (println "VIOLATION:" (:question w) (:subkind w) "in" (:scenario w) (:mode w) "-" (:detail w))))
    (System/exit (if (and (zero? (count all-violations)) (zero? capped-runs)) 0 1))))
