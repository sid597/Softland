(ns app.client.engine.executor-test
  (:require [app.client.engine.executor :as executor]
            [clojure.test :refer [deftest is testing]]))

(def capabilities
  {:num/add (fn [{:keys [a b]}] (+ a b))
   :num/scale (fn [{:keys [x by]}] (* x by))
   :word/echo (fn [{:keys [word]}] word)
   :fail/always (fn [_] (throw (ex-info "boom" {:why :test})))})

(def scope {"tool" {:size 4 :numbers [1 2 3]} "view" {:scale 2.0 :pan [0.0 0.0]}})

(deftest bindings-resolve-literals-and-step-outputs
  (let [result (executor/run {:steps [{:out "sum" :op :num/add :a "tool.size" :b "tool.numbers.2"}
                                      {:out "scaled" :op :num/scale :x "sum" :by 10}
                                      {:out "word" :op :word/echo :word "nearest"}]
                              :return {:value "scaled" :word "word" :all ["sum" "scaled"]}}
                             scope capabilities)]
    (is (:ok? result))
    (is (= {"sum" 7 "scaled" 70 "word" "nearest"} (:values result)))
    (is (= {:value 70 :word "nearest" :all [7 70]} (:return result)))
    (is (= [:num/add :num/scale :word/echo] (map :op (:log result))))
    (testing "reads name only the original roots, with the values read"
      (is (= {"tool.size" 4 "tool.numbers.2" 3} (:reads result))))))

(deftest reads-decide-a-rerun
  (let [construction {:steps [{:out "w" :op :num/scale :x "tool.size" :by "view.scale"}] :return {:w "w"}}
        result (executor/run construction scope capabilities)]
    (is (= {"tool.size" 4 "view.scale" 2.0} (:reads result)))
    (is (= (:reads result) (executor/reread scope (:reads result))))
    (is (not= (:reads result) (executor/reread (assoc-in scope ["view" :scale] 3.0) (:reads result))))
    (is (= (:reads result) (executor/reread (assoc-in scope ["view" :pan] [5.0 5.0]) (:reads result)))
        "a field never read does not count")))

(deftest a-missing-capability-stops-the-run-and-names-itself
  (let [construction {:steps [{:out "sum" :op :num/add :a 1 :b 2}
                              {:out "x" :op :surface/sample :point [1 2]}
                              {:out "y" :op :num/add :a "sum" :b 1}]
                      :return {:y "y"}}
        result (executor/run construction scope capabilities)]
    (is (not (:ok? result)))
    (is (= [:surface/sample] (:missing result)))
    (is (= {"sum" 3} (:values result)) "steps after the missing one never ran")
    (is (= [:surface/sample] (executor/missing-capabilities construction capabilities)))))

(deftest a-throwing-capability-is-reported-not-propagated
  (let [result (executor/run {:steps [{:out "x" :op :fail/always}] :return {}} scope capabilities)]
    (is (not (:ok? result)))
    (is (= "boom" (:error (last (:log result)))))
    (is (= {:why :test} (:data (last (:log result)))))))
