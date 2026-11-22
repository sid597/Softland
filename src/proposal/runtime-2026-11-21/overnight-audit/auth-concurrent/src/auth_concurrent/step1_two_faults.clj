(ns auth-concurrent.step1-two-faults
  "Step 3 verification of `spine/auth/agreement/` (`auth-concurrent.plan-steps`):
  1. Runs all 32 scenarios (25 carbon + 7 agreement extra) across the 4 standard
     0/1-fault modes (`:pure`, `:individual`, `:all-after`, `:timeout-overlap`)
     with `check-reruns? true` to verify the 1-fault result independently.
  2. Runs TWO FAULTS IN ONE HISTORY (`:fault-budget 2`) across all 32 scenarios
     under `:individual`, `:all-after`, `:timeout-overlap`, and `:mixed`
     (heterogeneous two-fault combinations where Fault 1 and Fault 2 can come
     from different Rama retry mechanisms in the same trace), plus additional
     compound adversarial scenarios (`TF-01`..`TF-04`)."
  (:require [agreement.check :as ag]
            [auth-concurrent.model-check :as mc]
            [auth-concurrent.plan-steps :as ps]
            [auth-concurrent.step1-run :as s1]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn two-fault-extra-scenarios
  "Additional compound scenarios specifically targeting two-fault interactions
  on the revised choose-at-account `enter`, `invite`, `own-layer-made`, and
  `answer-ask` protocols."
  []
  [(let [[p o] (s1/seed-both [[:invite {:person :P1 :claim s1/C1 :invited-by :system :at 10}]])]
     {:name "TF-01: first-time enter(Acc1, C1) racing own-layer-made(P1) + returning enter(Acc1, older :at)"
      :init-pstates p :init-oracle o
      :entities {:accounts [s1/Acc1] :codes [] :persons [:P1] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account s1/Acc1 :says {"s" "init-200"} :email (:email s1/C1) :code-hash nil
                                      :own-layer :L1 :making s1/M1 :at 200}]}
              {:id :c2 :call [:own-layer-made {:person :P1 :at 210}]}
              {:id :c3 :call [:enter {:account s1/Acc1 :says {"s" "older-50"} :email (:email s1/C1) :code-hash nil
                                      :own-layer :L2 :making s1/M2 :at 50}]
               :sequential-after [:c2]}]})

   (let [[p o] (s1/seed-both [[:invite {:person :P1 :claim s1/C_CODE :invited-by :system :at 10}]
                              [:invite {:person :P2 :claim s1/C1 :invited-by :system :at 10}]])]
     {:name "TF-02: 2-claim enter(Acc1, code+email) losing code to Acc2 and crashing/replaying on email"
      :init-pstates p :init-oracle o
      :entities {:accounts [s1/Acc1 s1/Acc2] :codes [] :persons [:P1 :P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:enter {:account s1/Acc1 :says {"s" "acc1-300"} :email (:email s1/C1) :code-hash (:hash s1/C_CODE)
                                      :own-layer :L1 :making s1/M1 :at 300}]}
              {:id :c2 :call [:enter {:account s1/Acc2 :says {"s" "acc2-100"} :email nil :code-hash (:hash s1/C_CODE)
                                      :own-layer :L2 :making s1/M2 :at 100}]}]})

   (let [[p o] (s1/seed-both [])]
     {:name "TF-03: invite(P1, C1) + invite(P2, C1) + enter(Acc1, C1) under double fault"
      :init-pstates p :init-oracle o
      :entities {:accounts [s1/Acc1] :codes [] :persons [:P1 :P2] :sign-ins [] :sessions []}
      :calls [{:id :c1 :call [:invite {:person :P1 :claim s1/C1 :invited-by :system :at 100}]}
              {:id :c2 :call [:invite {:person :P2 :claim s1/C1 :invited-by :system :at 110}]}
              {:id :c3 :call [:enter {:account s1/Acc1 :says {"s" "e"} :email (:email s1/C1) :code-hash nil
                                      :own-layer :L1 :making s1/M1 :at 120}]}]})

   (let [[p o] (s1/seed-both [[:entered-person {:person :P1 :claim s1/C1 :account s1/Acc1 :own-layer :L1 :making s1/M1 :at 10 :own-layer-made? true}]
                              [:begin-sign-in {:root :P1 :actor :P1 :sign-in :G0 :kind :browser :hash s1/H_ASK :until 1500 :label "web" :at 20}]
                              [:open-session {:root :P1 :sign-in :G0 :session :S0 :until 1500 :driven-by {:kind :person} :at 20}]])]
     {:name "TF-04: ask(U) + refused/early answer-ask(U, at=600) + late answer-ask(U, at=2000) under double fault"
      :init-pstates p :init-oracle o
      :entities {:accounts [s1/Acc1] :codes ["ASK_TF04"] :persons [:P1] :sign-ins [:G0 :G1 :G2] :sessions [:S0]}
      :calls [{:id :c1 :call [:ask {:code "ASK_TF04" :hash s1/H_ASK :asks "cli" :ends 3000 :at 50}]}
              {:id :c2 :call [:answer-ask {:code "ASK_TF04" :yes? true :root :P1 :actor :P1
                                           :new-agent nil :sign-in :G1 :until 5000 :at 600}]}
              {:id :c3 :call [:answer-ask {:code "ASK_TF04" :yes? false :root :P1 :actor :P1
                                           :new-agent nil :sign-in :G2 :until 5000 :at 2000}]}]})])

(defn -main [& args]
  (assert (str/includes? (:doc (meta (the-ns 'auth-concurrent.plan-steps))) "Revised single-task cross-key steps")
          "Must load spine/auth/agreement/src/auth_concurrent/plan_steps.clj first on classpath!")
  (let [max-states-2f (or (some-> (first args) parse-long) 250000)
        base-scenarios (into (vec (s1/build-scenarios)) (ag/extra-scenarios))
        tf-scenarios (into base-scenarios (two-fault-extra-scenarios))
        ;; 1. Reproduce 1-fault suite (with active? signature fix)
        t1-start (System/currentTimeMillis)
        one-fault-modes [:pure :individual :all-after :timeout-overlap]
        one-fault-results
        (vec (for [sc base-scenarios
                   m one-fault-modes]
               (mc/explore-scenario sc m {:max-states 80000
                                          :check-reruns? true
                                          :fault-budget (if (= m :pure) 0 1)})))
        t1-ms (- (System/currentTimeMillis) t1-start)
        one-fault-states (reduce + (map :states-explored one-fault-results))
        one-fault-terms (reduce + (map :terminal-histories one-fault-results))
        one-fault-capped (count (filter :capped? one-fault-results))
        one-fault-viols (mapcat :violations one-fault-results)

        _ (println (format "=== SUITE 1 (0/1-fault over 32 scenarios × 4 modes = %d runs) ===" (count one-fault-results)))
        _ (println (format "states=%,d terminals=%,d capped=%d violations=%d elapsed=%dms\n"
                           one-fault-states one-fault-terms one-fault-capped (count one-fault-viols) t1-ms))
        _ (flush)

        ;; 2. Two faults in one history (`fault-budget = 2`) across all 36 scenarios!
        ;; Run `:individual`, `:all-after`, `:timeout-overlap` across all 36 scenarios,
        ;; PLUS `:mixed` (cross-mechanism 2 faults in one trace) across all 2-caller + X + TF scenarios!
        t2-start (System/currentTimeMillis)
        two-fault-modes [:individual :all-after :timeout-overlap :mixed]
        two-fault-results
        (vec (for [sc tf-scenarios
                   m two-fault-modes
                   :when (or (not= m :mixed)
                             (= 2 (count (:calls sc)))
                             (str/starts-with? (:name sc) "TF-")
                             (str/starts-with? (:name sc) "X-"))]
               (let [r (mc/explore-scenario sc m {:max-states max-states-2f
                                                  :check-reruns? true
                                                  :fault-budget 2})]
                 (println (format "[2-fault %-15s] %-86s states=%-7d terms=%-6d capped=%-5s violations=%d"
                                  (name m) (:name sc) (:states-explored r) (:terminal-histories r)
                                  (str (:capped? r)) (count (:violations r))))
                 (flush)
                 r)))
        t2-ms (- (System/currentTimeMillis) t2-start)
        two-fault-states (reduce + (map :states-explored two-fault-results))
        two-fault-terms (reduce + (map :terminal-histories two-fault-results))
        two-fault-capped (filterv :capped? two-fault-results)
        two-fault-viols (vec (mapcat :violations two-fault-results))
        out-file (io/file "STEP3_TWO_FAULT_RESULTS.md")
        md (StringBuilder.)]
    (.append md "# Step 3 — Independent 1-Fault and 2-Fault Model Check of `spine/auth/agreement/`\n\n")
    (.append md "## Suite 1: 0/1-Fault Check (`fault-budget = 0/1`, `check-reruns? = true`)\n\n")
    (.append md (format "- **Runs:** %d scenarios × 4 modes = **%d runs** (%d ms)\n"
                        (count base-scenarios) (count one-fault-results) t1-ms))
    (.append md (format "- **States explored:** **%,d** (%d capped runs)\n"
                        one-fault-states one-fault-capped))
    (.append md (format "- **Quiescent terminal histories checked:** **%,d**\n"
                        one-fault-terms))
    (.append md (format "- **Violations across Q1..Q5:** **%d**\n\n"
                        (count one-fault-viols)))

    (.append md "## Suite 2: Two Faults in One History (`fault-budget = 2`, `check-reruns? = true`)\n\n")
    (.append md (format "- **Runs:** **%d runs** across 36 scenarios (`25` carbon + `7` `X-01..X-07` + `4` `TF-01..TF-04`) × `:individual`, `:all-after`, `:timeout-overlap`, and `:mixed` (%d ms)\n"
                        (count two-fault-results) t2-ms))
    (.append md (format "- **States explored:** **%,d** (%d capped runs at `max-states = %,d`)\n"
                        two-fault-states (count two-fault-capped) max-states-2f))
    (.append md (format "- **Quiescent terminal histories checked:** **%,d**\n"
                        two-fault-terms))
    (.append md (format "- **Violations across Q1..Q5:** **%d**\n\n"
                        (count two-fault-viols)))

    (when (seq two-fault-capped)
      (.append md "### Runs reaching `max-states` cap\n\n")
      (doseq [r two-fault-capped]
        (.append md (format "- `%s` (`%s`, 2 faults): %,d states, %,d terminal histories, %d violations\n"
                            (:scenario r) (name (:mode r)) (:states-explored r)
                            (:terminal-histories r) (count (:violations r)))))
      (.append md "\n"))

    (when (seq two-fault-viols)
      (.append md "## Violations Found\n\n")
      (doseq [[i v] (map-indexed vector two-fault-viols)]
        (.append md (s1/format-violation-md (inc i) v))))

    (.append md "## Two-Fault Per-Run Table\n\n")
    (.append md "| Scenario | 2-Fault Mode | States | Terminal histories | Capped? | Violations |\n")
    (.append md "|---|---|---|---|---|---|\n")
    (doseq [r two-fault-results]
      (.append md (format "| %s | `%s` | %,d | %,d | %s | %d |\n"
                          (:scenario r) (name (:mode r)) (:states-explored r)
                          (:terminal-histories r) (:capped? r) (count (:violations r)))))

    (spit out-file (str md))
    (println (format "\n=== SUITE 2 SUMMARY (2 faults in one history) ===\nruns=%d states=%,d terminals=%,d capped=%d violations=%d elapsed=%dms"
                     (count two-fault-results) two-fault-states two-fault-terms
                     (count two-fault-capped) (count two-fault-viols) t2-ms))
    (when (seq two-fault-viols)
      (doseq [v two-fault-viols]
        (println "VIOLATION:" (:question v) (:subkind v) (:scenario v) (:mode v) (:detail v))))
    (System/exit (if (and (empty? one-fault-viols) (empty? two-fault-viols)) 0 1))))
