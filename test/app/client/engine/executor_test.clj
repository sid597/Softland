(ns app.client.engine.executor-test
  (:require [app.client.engine.executor :as executor]
            [clojure.test :refer [deftest is testing]]))

(deftest one-language-in-formulas-and-capability-arguments
  (let [rule [:* [:get :size] [:pow [:get :p] 2.0]]
        input {:size 40 :p 0.55}
        compiled (executor/compile-expression rule)
        program {:steps [{:bind :width :call :identity :args [rule]}] :return [:get :width]}]
    (is (< (Math/abs (- 12.1 (compiled input))) 1e-12))
    (is (= (compiled input) (:value (executor/execute program input {:identity identity}))))
    (is (= #{[:size] [:p]} (executor/references rule)))
    (is (= (executor/execute program input {:identity identity})
           (executor/execute program input {:identity identity})) "no clock in the semantic value")))

(deftest expression-vocabulary-and-lazy-branches
  (doseq [[expression expected] [[[:+ 2 [:* 3 4]] 14]
                                [[:pow 2 [:pow 3 2]] 512.0]
                                [[:/ 10 4] 2.5]
                                [[:max 1 3] 3]
                                [[:clamp 2 0 0.5] 0.5]
                                [[:step 0.5 0.7] 1.0]
                                [[:smoothstep 0 1 0.5] 0.5]
                                [[:mix 1 2 0.5] 1.5]
                                [[:floor 1.9] 1.0]
                                [[:exp 0] 1.0]
                                [[:if true 7 [:get :missing]] 7]
                                [[:literal [:unknown :data]] [:unknown :data]]]]
    (is (= expected (executor/evaluate expression {}))))
  (is (= #{[:yes] [:no]} (executor/references [:if true [:get :yes] [:get :no]])))
  (is (= {:path [1 2] :label "source.x"}
         (executor/evaluate {:path [1 2] :label "source.x"} {}))))

(deftest errors-stop-before-the-next-capability
  (let [calls (atom [])]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unavailable"
                         (executor/execute {:steps [{:call :missing} {:call :later}]}
                                           {} {:later #(swap! calls conj :later)})))
    (is (empty? @calls)))
  (doseq [expression [[:get :missing] [:pow 2] [:unknown 1] [:sqrt -1]]]
    (is (thrown? clojure.lang.ExceptionInfo (executor/evaluate expression {}))))
  (is (thrown? Exception (executor/evaluate [:/ 1 0] {}))))

(deftest each-carries-values-and-restores-the-item-binding
  (let [program {:bindings [[:sum 0]]
                 :steps [{:each [:get :numbers] :item :n
                          :steps [{:bind :sum :value [:+ [:get :sum] [:get :n]]}]}]
                 :return {:sum [:get :sum] :n [:get :n]}}
        result (executor/execute program {:numbers [1 2 3] :n :outer} {})]
    (is (= {:sum 6 :n :outer} (:value result)))
    (is (thrown? clojure.lang.ExceptionInfo
                 (executor/execute {:steps [{:each [1 2] :steps []}]} {} {})))))
