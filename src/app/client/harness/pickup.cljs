(ns app.client.harness.pickup
  "The definer's pickup through browser CPU values and continuation bytes.
   Takes no external state; gives texel, chain, four edit outcomes, byte
   equivalence, hash and PNG. Owns only the temporary run values. The repo
   verifier compares the text golden; pickup_test.clj is the JVM twin."
  (:require [app.client.engine.executor :as executor]
            [app.client.engine.surface :as surface]
            [app.client.engine.value-bytes :as vb]
            [app.client.path.construction :as construction]
            [app.client.path.records :as records]
            [app.client.harness.shared :as shared]))

(defn- scope [record] {:path (:path/value (construction/construct record))})
(defn- run [record opts]
  (executor/run (construction/program-record record) (scope record) construction/capabilities opts))

(defn run-check!
  "CPU pickup and checkpoint round trip → promise of browser evidence.
   Hashes are printed/golden evidence only; equality compares full values."
  []
  (let [t0 (js/performance.now)
        straight (run records/pickup {})
        checkpoint (run records/pickup {:until 12})
        c (:continuation checkpoint)
        bytes (executor/encode c)
        restored (executor/decode bytes construction/capabilities)
        resumed (executor/resume restored construction/capabilities {})
        painting (get-in straight [:results :surface])
        texel (:color (surface/sample [painting] [64.5 64.5] :nearest {}))
        edits (mapv (fn [[label record]]
                      (let [r (executor/resume restored construction/capabilities
                                                {:record (construction/program-record record) :scope (scope record)})]
                        (cond-> {:edit label :status (:status r) :reason (:reason r) :detail (:detail r)}
                          (= :complete (:status r)) (assoc :matches-fresh? (vb/equal? (:results r) (:results (run record {})))))))
                    [["first-pressure" (assoc-in records/pickup [:path/source :samples 0 2] 0.4)]
                     ["last-x" (assoc-in records/pickup [:path/source :samples 3 0] 110.0)]
                     ["pickup" (assoc-in records/pickup [:path/tool :pickup] 0.25)]
                     ["dimensions" (update records/pickup :path/surface assoc :width 64 :height 64)]])
        equal-bytes? (vb/equal? (:data painting) (get-in resumed [:results :surface :data]))
        equal-history? (vb/equal? (:history straight) (:history resumed))
        equal-subjects? (vb/equal? (:subjects straight) (:subjects resumed))
        png (surface/png-bytes painting)
        elapsed (- (js/performance.now) t0)]
    (.then (shared/sha256-bytes (vb/float-bytes (:data painting)))
           (fn [sha]
             {:texel texel :sha256 sha :dabs (count (get-in straight [:results :dabs]))
              :key (:key painting) :revision (:revision painting) :components (alength (:data painting))
              :checkpoint {:at (:at c) :bytes (alength bytes) :equal-bytes? equal-bytes?
                           :equal-history? equal-history? :equal-subjects? equal-subjects?}
              :edits edits :cpu-ms elapsed
              :image {:file "cpu-path-pickup.png" :png-data-url (str "data:image/png;base64," (vb/base64 png))}
              :pass? (and (= :complete (:status straight)) (= :complete (:status resumed))
                          (= 12 (:at c)) (= 24 (:revision painting)) (= "paint:23/painted" (:key painting))
                          (= 24 (count (get-in straight [:results :dabs])))
                          equal-bytes? equal-history? equal-subjects?
                          (every? true? (map #(< (js/Math.abs (- %1 %2)) 1e-6) texel
                                            [0.01336952205747366 0 0.9866304397583008 1]))
                          (= [:consumed-items-differ nil :recipe-differs :recipe-differs] (mapv :reason edits))
                          (true? (:matches-fresh? (nth edits 1))))}))))
