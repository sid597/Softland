(ns app.client.region3d.path-placement-test
  (:require [app.client.engine.executor :as executor]
            [app.client.path.construction :as construction]
            [app.client.path.records :as records]
            [app.client.path.pack :as pack]
            [app.client.region3d.on-plane :as on-plane]
            [clojure.test :refer [deftest is]]))

(deftest placed-ink-consumes-the-value-and-carries-its-clip
  (let [ink (construction/construct records/harness-z)
        clip (:path/value (construction/construct records/holed-concave))
        placement {:object-id :ink :address :shared
                   :component (assoc-in ink [:path/paint :clip] {:path clip :rule :even-odd})}
        result (with-redefs [executor/run (fn [& _] (throw (ex-info "placed ink ran a recipe" {})))]
                 (on-plane/placed-ink-regions placement))
        region (first (:regions result))]
    (is (= 1 (count (:regions result))))
    (is (= clip (get-in region [:clip :path])))
    (is (= :even-odd (get-in region [:clip :rule])))
    (is (pack/inside? (get-in region [:clip :pack]) :even-odd 90.0 90.0))
    (is (not (pack/inside? (get-in region [:clip :pack]) :even-odd 42.0 42.0)))
    (is (empty? (:regions (on-plane/placed-ink-regions
                           (assoc-in placement [:component :path/paint :clip :path] {:subpaths []})))))))
