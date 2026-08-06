(ns app.client.substrate.connector-material-test
  (:require [app.client.substrate.connector-material :as material]
            [clojure.test :refer [deftest is testing]]))

(defn paint [color]
  {:color color :opacity 1.0 :width 2.0
   :color-space :srgb :alpha-association :straight})

(defn connector
  ([] (connector :connector/test))
  ([id]
   {:connector/relation-id id
    :connector/row-stamp [:event 1]
    :connector/dress-revision 1
    :connector/kind :references
    :connector/from {:bind :node :target :a :anchor :boundary}
    :connector/to {:bind :node :target :b :anchor :boundary}
    :connector/route {:policy :straight :waypoints []}
    :connector/heads {:from :none :to :triangle :size-k 4.0}
    :connector/label nil
    :connector/paint (paint [0.6 0.7 0.8 0.9])
    :connector/status :asserted
    :connector/provenance {:actor-id "sid" :asserter-type :human}}))

(deftest merged-material-grammar-is-fail-closed
  (is (= (connector) (material/validate-material! (connector))))
  (doseq [[label mutate error-pattern]
          [["binding" #(assoc % :connector/from
                              {:bind :node :target :a :anchor :center})
            #"boundary"]
           ["kind" #(assoc % :connector/kind :pairs-with)
            #"kind"]
           ["route" #(assoc-in % [:connector/route :policy] :curve)
            #"policy"]
           ["label" #(assoc % :connector/label
                            {:text "" :at 0.5 :offset [0.0 -8.0]})
            #"text"]]]
    (testing label
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            error-pattern
                            (material/validate-material! (mutate (connector)))))))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unknown"
                        (material/validate-material!
                         (assoc (connector) :connector/private-metric 0.56)))))

(deftest dress-edits-change-only-the-client-revision
  (let [before (connector)
        after (-> before
                  (material/set-route :elbow/v1 2)
                  (material/set-waypoints [[12.0 9.0]] 3)
                  (material/set-label {:text "ref" :at 0.5
                                       :offset [0.0 -8.0]} 4)
                  (material/set-paint (paint [0.8 0.4 0.3 0.7]) 5))]
    (is (= (:connector/row-stamp before) (:connector/row-stamp after)))
    (is (= [(:connector/row-stamp before) 5]
           (material/composite-revision after)))
    (is (= "sid" (get-in after [:connector/provenance :actor-id])))
    (is (= :elbow/v1 (get-in after [:connector/route :policy])))
    (is (= [[12.0 9.0]] (get-in after [:connector/route :waypoints])))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"new revision"
                          (material/set-route after :straight 5)))))

(deftest provenance-is-visible-and-row-shaped
  (let [human (material/projection-color :references :human)
        machine (material/projection-color :references :llm)]
    (is (not= human machine))
    (is (= :human (material/provenance-class :human)))
    (is (= :machine (material/provenance-class :agent)))
    (is (= #{:actor-id :asserter-type}
           (set (keys (:connector/provenance (connector))))))))

(deftest cpu-classifier-is-the-pick-truth
  (let [route {:status :resolved
               :stroke-width 2.0
               :stroke-points [[0.0 0.0] [10.0 0.0]]
               :from-head nil
               :to-head [[10.0 0.0] [7.0 2.0] [7.0 -2.0]]}]
    (is (= :inside (material/classify route [4.0 0.4])))
    (is (= :boundary (material/classify route [4.0 1.0])))
    (is (= :outside (material/classify route [4.0 1.01])))
    (is (= :inside (material/classify route [8.0 0.0])))
    (is (= :boundary (material/classify route [10.0 0.0])))
    (is (material/hit? route [8.0 0.0]))
    (is (not (material/hit? route [5.0 4.0])))))

(deftest corpus-census-counts-instances-and-overlap
  (let [route (fn [id status points]
                {:edge-instance-id id :status status
                 :anchor-points points :stroke-points points})
        census (material/corpus-census
                [(route [:a :x :y] :resolved [[0 0] [10 0]])
                 (route [:b :x :y] :resolved [[0 0] [10 0]])
                 (route [:c :x :missing] :unresolved [])
                 (route [:d :x :y] :degenerate [])
                 (route [:e :x :y] :mixed-camera [])])]
    (is (= {:edge-instances 5 :resolved 2 :unresolved 1
            :degenerate 1 :mixed-camera 1 :overlap-groups 1}
           census))
    (is (material/assert-corpus-coverage!
         {:straight-arrow-label [:grammar]
          :elbow-waypoints-heads [:route]
          :provenance-overlap [:paint]
          :cross-container [:transform]
          :unresolved [:census]
          :multi-occurrence [:identity]
          :legal-zoom-extremes [:regime]}))))
