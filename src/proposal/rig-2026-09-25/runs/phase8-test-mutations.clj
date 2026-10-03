;; Phase 8 test validation (TEST_VALIDATION-replays.md): each model-side test
;; against a plausible bug. One function at a time is redefined into the bug
;; (with-redefs, this JVM only; nothing is written), the test meant to catch
;; it is run, and the line says whether it failed. No cluster.
;; Run from the rig folder:
;;   clojure -Sdeps '{:aliases {:probe {:extra-paths ["test" "../formal-model-2026-09-24/src"]
;;     :extra-deps {org.clojure/test.check {:mvn/version "1.1.0"}} :jvm-opts ["-Xss16m"]}}}' \
;;     -M:probe -i runs/phase8-test-mutations.clj
(require '[clojure.test :as t] '[rig.replay-test :as rt] '[formal.model :as fm])

(defn failures [test-syms]
  (binding [t/*report-counters* (ref t/*initial-report-counters*)
            t/*test-out* (java.io.StringWriter.)]
    (t/test-vars (map #(ns-resolve 'rig.replay-test %) test-syms))
    (let [{:keys [fail error]} @t/*report-counters*] (+ fail error))))

(defmacro mutant [what tests bindings]
  `(let [n# (with-redefs ~bindings (failures '~tests))]
     (println (format "%-8s %-78s %s" (if (pos? n#) "CAUGHT" "MISSED") ~what (str n# " failing")))))

(println "baseline, no mutation:" (failures '[lockstep-ends-where-run-ends lockstep-finds-every-effect the-guard-catches-decisions-it-cannot-order every-step-of-the-fixed-histories-has-a-row answers-per-name-reduce-to-one the-model-report-is-reproduced known-differences-cite-their-cases the-world-grants-every-model-permission judging-an-answer judging-an-exit-refusal judging-a-fact the-verdict-rules the-api-table the-report-renders holds-follow-the-model the-group-keeps-a-reader seed-answers-are-found-in-every-shape extra-rows-are-facts-the-model-lacks control-references-for-every-kind a-call-or-a-wait-that-does-not-return-is-cut]) "failing")

(let [seen-of rt/seen-of lockstep rt/lockstep op-effects rt/op-effects reduce-answers rt/reduce-answers
      judge-answer rt/judge-answer judge-exit-refusal rt/judge-exit-refusal judge-fact rt/judge-fact
      rig-pid rt/rig-pid control-refs rt/control-refs answers-in rt/answers-in report-lines rt/report-lines]
  (mutant "seen-of calls every found value open" [lockstep-ends-where-run-ends]
          [rt/seen-of (fn [st e] (update (seen-of st e) :values #(into {} (for [[k v] %] [k (if (= v :missing) v :open)]))))])
  (mutant "the lockstep skips the closing read" [lockstep-ends-where-run-ends lockstep-finds-every-effect]
          [rt/lockstep (fn [c h] (let [ls (lockstep c h)] (-> ls (update :end update :reads pop) (update :steps pop))))])
  (mutant "op-effects ignores [:prepare]'s delta (F7)" [lockstep-finds-every-effect]
          [rt/op-effects (fn [i op b a] (assoc (op-effects i op b a) :prepared []))])
  (mutant "op-effects misses person forgets" [lockstep-finds-every-effect]
          [rt/op-effects (fn [i op b a] (assoc (op-effects i op b a) :forgets []))])
  (mutant "reduce-answers takes the first partition's answer (F6)" [answers-per-name-reduce-to-one]
          [rt/reduce-answers (fn [as] (when (seq as) (select-keys (first as) [:answer :reason :stamp])))])
  (mutant "the guard never trips" [the-guard-catches-decisions-it-cannot-order]
          [rt/guard-refusal (constantly nil)])
  (mutant "a history outside the rows is played" [every-step-of-the-fixed-histories-has-a-row]
          [rt/history-refusal (constantly nil)])
  (mutant "a forget in the group is played" [every-step-of-the-fixed-histories-has-a-row]
          [rt/effect-refusal (constantly nil)])
  (mutant "a named configuration is the wrong reading" [the-model-report-is-reproduced]
          [rt/named-configs (assoc-in rt/named-configs [:a :configs 3 1] (assoc fm/baseline :wrap :owner-and-an-other))])
  (mutant "a named check is dropped" [the-model-report-is-reproduced]
          [rt/named-configs (assoc-in rt/named-configs [:b :checks] [:p1 :p2])])
  (mutant "KD10 no longer cites A4" [known-differences-cite-their-cases]
          [rt/known-differences (mapv #(if (= 10 (:n %)) (update % :cases disj "A4") %) rt/known-differences)])
  (mutant "a known difference cites no case and gives no why" [known-differences-cite-their-cases]
          [rt/known-differences (mapv #(if (= 13 (:n %)) (assoc % :cases #{}) %) rt/known-differences)])
  (mutant "KD7 is dropped from the table" [known-differences-cite-their-cases]
          [rt/known-differences (vec (remove #(= 7 (:n %)) rt/known-differences))])
  (mutant "a session permission maps to the person's own" [the-world-grants-every-model-permission]
          [rt/rig-pid (fn [rn [w l where :as pid]] (if (= :session where) [(rn w) (rn l) (rn l)] (rig-pid rn pid)))])
  (mutant "a group permission maps without its root" [the-world-grants-every-model-permission]
          [rt/rig-pid (fn [rn [w l where :as pid]] (if (= :group l) [(rn w) (rn l) (rn l)] (rig-pid rn pid)))])
  (mutant "KD1 matches a lease refused for any reason" [judging-an-answer]
          [rt/judge-answer (fn [m r l] (if (and (= :no (:answer m)) (= :no (:answer l))) {:ok? true :kd 1} (judge-answer m r l)))])
  (mutant "a refused lease passes when the act is yes (F11)" [judging-an-answer]
          [rt/judge-answer (fn [m r l] (judge-answer m r (when (= :yes (:answer r)) nil)))])
  (mutant "KD10 explains any refusal after a forget (F3)" [judging-an-exit-refusal]
          [rt/judge-exit-refusal (fn [r f l] (if (= :yes (:answer f)) {:ok? true :kd 10} (judge-exit-refusal r f l)))])
  (mutant "KD10 explains a refusal for a reader never forgotten" [judging-an-exit-refusal]
          [rt/judge-exit-refusal (fn [r f l] (judge-exit-refusal r {:answer :yes :stamp 1000} l))])
  (mutant "judge-fact ignores the erasure's date" [judging-a-fact]
          [rt/judge-fact (fn [rn refs x row] (judge-fact rn refs x (cond-> row (:erased-at row) (assoc :stamp 0))))])
  (mutant "judge-fact takes any control fact" [judging-a-fact]
          [rt/judge-fact (fn [rn refs x row] (if (contains? fm/control-keys (:k x)) {:ok? true :kd 20} (judge-fact rn refs x row)))])
  (mutant "control-refs drops the crossing's request" [control-references-for-every-kind]
          [rt/control-refs (fn [rn a b k v] (if (= :crossed k) [(b (:source v))] (control-refs rn a b k v)))])
  (mutant "a status worse than predicted passes" [the-verdict-rules]
          [rt/worse-than-predicted? (constantly false)])
  (mutant "the holds are the first commit's rule (IV1)" [holds-follow-the-model]
          [rt/decided-later? (fn [p step nm] (not (#'rt/decided-in? step nm)))])
  (mutant "the group's reader ignores forgets (IV5)" [the-group-keeps-a-reader]
          [rt/live-member (fn [p] ((:rn @p) :alice))])
  (mutant "make-group!'s re-class answer is not seen" [seed-answers-are-found-in-every-shape]
          [rt/answers-in (fn [x] (if (and (map? x) (contains? x :made)) [(:made x)] (answers-in x)))])
  (mutant "a lease row is counted as a fact the model lacks" [extra-rows-are-facts-the-model-lacks]
          [rt/rig-only-keys (disj rt/rig-only-keys :lease)])
  (mutant "a call is not bounded" [a-call-or-a-wait-that-does-not-return-is-cut]
          [rt/bounded-call (fn [what thunk] (thunk))])
  (mutant "stage 1 is looked for in the wrong namespace" [the-api-table]
          [rt/apis (mapv #(if (= :client/record (:id %)) (assoc % :var 'rig.store.nowhere/record) %) rt/apis)])
  (mutant "the report drops the rig's line under other configurations" [the-report-renders]
          [rt/report-lines (fn [s] (remove #(and (clojure.string/starts-with? % "      rig:    ") (re-find #"as under baseline" %)) (report-lines s)))]))
