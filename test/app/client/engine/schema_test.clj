(ns app.client.engine.schema-test
  (:require [app.client.engine.schema :as schema]
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
         (schema/check scene
                        {:scene {:left {:value 1} :right {:value 2}}})))
  (is (= {:error-type :schema/invalid-key
          :path [:scene "x"]}
         (select-keys
          (error-data #(schema/check scene {:scene {"x" {:value 1}}}))
          [:error-type :path])))
  (is (= {:error-type :schema/invalid-value
          :path [:scene :right :value]}
         (select-keys
          (error-data
           #(schema/check
             scene {:scene {:left {:value 1} :right {:value "two"}}}))
          [:error-type :path]))))

(deftest form-explanation-tripwire
  (let [spec {:keys #{:left :right}
              :validators {:left integer? :right integer?}
              :form-validators
              [{:valid? #(= (:left %) (:right %))
                :error-type :schema/not-equal
                :explain (fn [form]
                           {:left (:left form) :right (:right form)})}]}
        data (error-data #(schema/check spec {:left 1 :right 2}))]
    (is (= {:error-type :schema/not-equal
            :path []
            :left 1
            :right 2}
           (select-keys data [:error-type :path :left :right])))))
