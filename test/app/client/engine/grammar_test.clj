(ns app.client.engine.grammar-test
  (:require [app.client.engine.grammar :as grammar]
            [clojure.test :refer [deftest is]]))

(defn- error-data [f]
  (try
    (f)
    nil
    (catch clojure.lang.ExceptionInfo error
      (ex-data error))))

(def ^:private scene-item
  {:keys #{:value}
   :validators {:value integer?}})

(def ^:private scene
  {:keys #{:scene}
   :validators {:scene [:map-of keyword? scene-item]}})

(deftest map-of-tripwire
  (is (= {:scene {:left {:value 1} :right {:value 2}}}
         (grammar/check scene
                        {:scene {:left {:value 1} :right {:value 2}}})))
  (is (= {:error-type :grammar/invalid-key
          :path [:scene "x"]}
         (select-keys
          (error-data #(grammar/check scene {:scene {"x" {:value 1}}}))
          [:error-type :path])))
  (is (= {:error-type :grammar/invalid-value
          :path [:scene :right :value]}
         (select-keys
          (error-data
           #(grammar/check
             scene {:scene {:left {:value 1} :right {:value "two"}}}))
          [:error-type :path]))))

(deftest form-explanation-tripwire
  (let [spec {:keys #{:left :right}
              :validators {:left integer? :right integer?}
              :form-validators
              [{:valid? #(= (:left %) (:right %))
                :error-type :grammar/not-equal
                :explain (fn [form]
                           {:left (:left form) :right (:right form)})}]}
        data (error-data #(grammar/check spec {:left 1 :right 2}))]
    (is (= {:error-type :grammar/not-equal
            :path []
            :left 1
            :right 2}
           (select-keys data [:error-type :path :left :right])))))
