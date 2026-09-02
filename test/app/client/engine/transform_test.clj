(ns app.client.engine.transform-test
  (:require [app.client.engine.transform :as transform]
            [clojure.test :refer [deftest is]]))

(defn- error-data [f]
  (try
    (f)
    nil
    (catch clojure.lang.ExceptionInfo error
      (ex-data error))))

(deftest anchored-and-screen-metric-tripwire
  (let [world-transforms {:affine [2.0 0.0 0.0 2.0 30.0 40.0] :flags 0}
        anchor {:x 10.0 :y 20.0 :w 40.0 :h 30.0}
        stations [0.01 0.1 1.0 8.0 100.0 1000.0]
        point-rects
        (mapv #(transform/anchored-screen-rect
                {:x 10.0 :y 20.0 :w 0.0 :h 0.0}
                world-transforms {:x 7.0 :y 11.0 :zoom %}
                {:x -5.0 :y -5.0 :w 10.0 :h 10.0})
              stations)
        anchored-rects
        (mapv #(transform/anchored-screen-rect
                anchor world-transforms {:x 7.0 :y 11.0 :zoom %}
                {:x -0.5 :y -0.5 :w 1.0 :h 1.0})
              stations)]
    (is (every? #(= [10.0 10.0] [(:w %) (:h %)]) point-rects))
    (is (= [1.8000000000000007 81.0 8001.0 80001.0]
           (mapv :w [(first anchored-rects) (nth anchored-rects 2)
                     (nth anchored-rects 4) (last anchored-rects)])))
    (is (= {:x 2.5 :y 6.800000000000001 :w 10.0 :h 10.0}
           (first point-rects)))))

(deftest declared-group-schema-tripwire
  (is (= {:error-type :schema/invalid-value :path [:affine]}
         (select-keys
          (error-data
           #(transform/add-group
             (transform/empty-registry) 1 {:affine [1 0 0 1 0]}))
          [:error-type :path])))
  (is (= {:error-type :schema/unknown-key :path [:tilt]}
         (select-keys
          (error-data
           #(transform/add-group
             (transform/empty-registry) 1 {:tilt 0.25}))
          [:error-type :path])))
  (is (= {:error-type :schema/invalid-value :path [:camera]}
         (select-keys
          (error-data
           #(transform/add-group
             (transform/empty-registry) 1 {:camera :orbit}))
          [:error-type :path])))
  (is (= {:error-type :transform/parent-missing :parent 99}
         (select-keys
          (error-data
           #(transform/add-group
             (transform/empty-registry) 1 {:parent 99}))
          [:error-type :parent])))
  (is (= :transform/reserved-group-id
         (:error-type
          (error-data
           #(transform/add-group (transform/empty-registry) 0 {})))))
  (let [registry (transform/add-group (transform/empty-registry) 1 {})]
    (is (= :transform/duplicate-group
           (:error-type
            (error-data #(transform/add-group registry 1 {})))))))

(deftest world-transforms-group-buffer-index-tripwire
  (let [registry (-> (transform/empty-registry)
                     (transform/add-group
                      17 {:parent 0
                          :affine [0.5 0.0 0.0 0.5 40.0 20.0]}))
        world-transforms (transform/world-transforms registry)]
    (is (= {0 {:affine transform/identity-affine
               :flags 0
               :layer 0
               :stack-path [[0 0 0]]
               :buffer-index 0}
            17 {:affine [0.5 0.0 0.0 0.5 40.0 20.0]
                :flags 0
                :layer 0
                :stack-path [[0 0 0] [17 0 0]]
                :buffer-index 1}}
           world-transforms))
    (is (= 0 (transform/buffer-index world-transforms 0)))
    (is (= 1 (transform/buffer-index world-transforms 17)))
    (is (= :transform/unknown-group
           (:error-type (error-data #(transform/buffer-index world-transforms 99)))))))
