(ns app.client.engine.placement-test
  (:require [app.client.engine.placement :as containers]
            [clojure.test :refer [deftest is]]))

(defn- error-data [f]
  (try
    (f)
    nil
    (catch clojure.lang.ExceptionInfo error
      (ex-data error))))

(deftest anchored-and-screen-metric-tripwire
  (let [effective {:affine [2.0 0.0 0.0 2.0 30.0 40.0] :flags 0}
        anchor {:x 10.0 :y 20.0 :w 40.0 :h 30.0}
        stations [0.01 0.1 1.0 8.0 100.0 1000.0]
        point-rects
        (mapv #(containers/anchored-screen-rect
                {:x 10.0 :y 20.0 :w 0.0 :h 0.0}
                effective {:x 7.0 :y 11.0 :zoom %}
                {:x -5.0 :y -5.0 :w 10.0 :h 10.0})
              stations)
        anchored-rects
        (mapv #(containers/anchored-screen-rect
                anchor effective {:x 7.0 :y 11.0 :zoom %}
                {:x -0.5 :y -0.5 :w 1.0 :h 1.0})
              stations)]
    (is (every? #(= [10.0 10.0] [(:w %) (:h %)]) point-rects))
    (is (= [1.8000000000000007 81.0 8001.0 80001.0]
           (mapv :w [(first anchored-rects) (nth anchored-rects 2)
                     (nth anchored-rects 4) (last anchored-rects)])))
    (is (= {:x 2.5 :y 6.800000000000001 :w 10.0 :h 10.0}
           (first point-rects)))))

(deftest declared-container-grammar-tripwire
  (is (= {:error-type :grammar/invalid-value :path [:affine]}
         (select-keys
          (error-data
           #(containers/add-container
             (containers/empty-registry) 1 {:affine [1 0 0 1 0]}))
          [:error-type :path])))
  (is (= {:error-type :grammar/unknown-key :path [:tilt]}
         (select-keys
          (error-data
           #(containers/add-container
             (containers/empty-registry) 1 {:tilt 0.25}))
          [:error-type :path])))
  (is (= {:error-type :grammar/invalid-value :path [:camera]}
         (select-keys
          (error-data
           #(containers/add-container
             (containers/empty-registry) 1 {:camera :orbit}))
          [:error-type :path])))
  (is (= {:error-type :placement/parent-missing :parent 99}
         (select-keys
          (error-data
           #(containers/add-container
             (containers/empty-registry) 1 {:parent 99}))
          [:error-type :parent])))
  (is (= :placement/reserved-cid
         (:error-type
          (error-data
           #(containers/add-container (containers/empty-registry) 0 {})))))
  (let [registry (containers/add-container (containers/empty-registry) 1 {})]
    (is (= :placement/duplicate-container
           (:error-type
            (error-data #(containers/add-container registry 1 {})))))))

(deftest effective-container-slot-tripwire
  (let [registry (-> (containers/empty-registry)
                     (containers/add-container
                      17 {:parent 0
                          :affine [0.5 0.0 0.0 0.5 40.0 20.0]}))
        effective (containers/effective registry)]
    (is (= {0 {:affine containers/identity-affine
               :flags 0
               :layer 0
               :stack-path [[0 0 0]]
               :transport-slot 0}
            17 {:affine [0.5 0.0 0.0 0.5 40.0 20.0]
                :flags 0
                :layer 0
                :stack-path [[0 0 0] [17 0 0]]
                :transport-slot 1}}
           effective))
    (is (= 0 (containers/slot effective 0)))
    (is (= 1 (containers/slot effective 17)))
    (is (= :placement/unknown-container
           (:error-type (error-data #(containers/slot effective 99)))))))
