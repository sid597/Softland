(ns app.client.path.construction-test
  (:require [app.client.engine.executor :as executor]
            [app.client.path.component :as component]
            [app.client.path.construction :as construction]
            [app.client.path.records :as records]
            [clojure.test :refer [deftest is testing]]))

(deftest a-recipe-edit-changes-the-value-the-retained-renderer-will-see
  (let [original (construction/construct records/harness-z)
        record (assoc records/harness-z :path/revision 2
                      :path/construction (construction/default-construction records/z-as-dabs))
        edited (construction/construct record)]
    (is (= (:path/value original) (:path/value edited)))
    (is (= 1 (count (:regions (component/regions original {})))))
    (is (= 24 (count (:regions (component/regions edited {})))) "judge A5: recipe replaced with the dabs default")
    (is (not= (component/geometry-inputs original {}) (component/geometry-inputs edited {})))
    (is (= :union (get-in original [:path/paint :stroke :overlap])) "the earlier value stays unchanged")))

(deftest a-new-source-and-returned-result-cross-the-waist
  (let [record {:path/material-id :new :path/revision 1
                :path/source {:kind :designer-defined :width 32.25}
                :path/paint {:fill {:rule :nonzero :color [0 0 0 1]}}
                :path/construction
                {:steps [{:bind :path :call :rectangle
                          :args [[:* 2 [:get :source :width]]]}]
                 :return [:get :path]}}
        rectangle (fn [w] {:subpaths [{:start [0.0 0.0] :closed? true
                                      :segments [{:kind :line :p [w 0.0]} {:kind :line :p [w 128.0]}
                                                 {:kind :line :p [0.0 128.0]}]}]})
        value (construction/construct record {:rectangle rectangle})
        clipped (assoc-in (construction/construct records/harness-z) [:path/paint :clip]
                          {:path (:path/value value) :rule :nonzero})]
    (is (= 64.5 (get-in value [:path/value :subpaths 0 :segments 0 :p 0])))
    (is (= :inside (component/classify clipped [64.0 64.0])))
    (is (= :outside (component/classify clipped [65.0 64.0])))
    (is (not (contains? value :path/source)))
    (is (not (contains? value :path/construction)))))

(deftest geometry-never-executes-a-source-recipe
  (let [value (construction/construct records/border)]
    (with-redefs [executor/execute (fn [& _] (throw (ex-info "renderer entered executor" {})))
                  construction/construct (fn [& _] (throw (ex-info "renderer entered source" {})))]
      (doseq [scale [0.01 0.1 1.0 8.0 10.0 100.0 1000.0]]
        (is (seq (:regions (component/regions value {:scale scale :pan-fraction [0.25 0.5]}))))))))

(deftest an-unavailable-capability-is-an-explicit-failure
  (is (= :geometry/arrange
         (try (construction/construct (assoc records/harness-z :path/construction
                                             {:steps [{:call :geometry/arrange}] :return nil}))
              nil
              (catch Exception e (:capability (ex-data e)))))))

(deftest returned-width-declarations-carry-their-parameters
  (let [path {:subpaths [{:start [0 0] :closed? false :segments [{:kind :line :p [20 0]}]}]}
        record {:path/material-id :width-edit :path/revision 1
                :path/tool {:size 12 :unused 99}
                :path/paint {:stroke {:width 2 :color [0 0 0 1]}}
                :path/construction {:return {:path [:literal path]
                                              :paint {:stroke {:width [:literal [:get :size]]}}}}}
        result (construction/construct record)]
    (is (= {:size 12} (:path/parameters result)))
    (is (= :inside (component/classify result [10 5])))
    (is (= :outside (component/classify result [10 7])))))
