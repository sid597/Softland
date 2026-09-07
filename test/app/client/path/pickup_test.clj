(ns app.client.path.pickup-test
  (:require [clojure.test :refer [deftest is]]
            [app.client.engine.executor :as e]
            [app.client.engine.surface :as s]
            [app.client.engine.value-bytes :as vb]
            [app.client.path.construction :as c]
            [app.client.path.records :as r]
            [app.client.path.surface :as path-surface]
            [app.client.path.source :as source]))

(defn scope [record] {:path (:path/value (c/construct record))})
(defn run [record opts] (e/run (c/program-record record) (scope record) c/capabilities opts))
(defn resume [continuation record]
  (e/resume continuation c/capabilities {:record (c/program-record record) :scope (scope record)}))

(deftest the-definers-pickup-and-checkpoint-through-bytes
  (let [straight (run r/pickup {}) checkpoint (run r/pickup {:until 12})
        c (:continuation checkpoint) bytes (e/encode c) loaded (e/decode bytes c/capabilities)
        tail (e/resume loaded c/capabilities {})
        painting (get-in straight [:results :surface])
        texel (:color (s/sample [painting] [64.5 64.5] :nearest {}))]
    (is (= :complete (:status straight)) (pr-str (dissoc straight :history :results)))
    (is (= :suspended (:status checkpoint)))
    (is (= 12 (:at c)))
    (is (= :load (:reason (e/resume (assoc-in c [:state :surface :data] (float-array 4)) c/capabilities {}))))
    (is (= 24 (count (get-in straight [:results :dabs]))))
    (is (= 24 (:revision painting)))
    (is (= "paint:23/painted" (:key painting)))
    (is (= 24 (count (filter #(= :paint (:op %)) (:log straight)))))
    (doseq [[expected actual] (map vector [0.01336952205747366 0 0.9866304397583008 1] texel)]
      (is (< (Math/abs (- expected actual)) 1e-6)))
    (is (= 65536 (alength (:data painting))))
    (is (vb/equal? (:data painting) (get-in tail [:results :surface :data])))
    (is (vb/equal? (:history straight) (:history tail)))
    (is (= "paint@initial" (get-in straight [:history 0 :steps 0 :snapshot :layers 0 :key])))
    (is (vb/equal? (:subjects straight) (:subjects tail)))
    (is (vb/equal? (:results straight) (:results (e/resume c c/capabilities {}))))
    (doseq [[record reason] [[(assoc-in r/pickup [:path/source :samples 0 2] 0.4) :consumed-items-differ]
                             [(assoc-in r/pickup [:path/tool :pickup] 0.25) :recipe-differs]
                             [(update r/pickup :path/surface assoc :width 64 :height 64) :recipe-differs]]]
      (is (= reason (:reason (resume loaded record)))))
    (is (= {:at 0} (:detail (resume loaded (assoc-in r/pickup [:path/source :samples 0 2] 0.4)))))
    (let [edited (assoc-in r/pickup [:path/source :samples 3 0] 110.0)
          resumed (resume loaded edited)]
      (is (= :complete (:status resumed)))
      (is (vb/equal? (:results (run edited {})) (:results resumed))))
    (println "PICKUP" {:checkpoint-bytes (alength bytes) :texel texel :dabs 24 :components 65536})))

(deftest affine-domain-coverage-is-computed-in-texels
  (let [declaration (assoc (:path/surface r/pickup) :width 4 :height 4 :initial [0 0 0 0]
                          :domain {:kind :plane :map [0 2 -2 0 4 0]})
        path (:path (source/build-rect {:x 0 :y 0 :w 1 :h 1}))
        painted (path-surface/paint {:surface (s/new declaration) :region path :rgba [1 0 0 1]
                                     :opacity 1 :blend :source-over} {:at 0 :step :painted})]
    (is (= [1.0 0.0 0.0 1.0] (:color (s/sample [painted] [0.25 0.25] :nearest {}))))
    (is (= [0.0 0.0 0.0 0.0] (:color (s/sample [painted] [1.25 0.25] :nearest {}))))))
