(require '[app.client.path.records :as records] '[app.client.path.component :as c]
         '[app.client.path.geometry :as g] '[app.client.path.curves :as curves]
         '[app.client.harness.path-fixtures :as fx])
(defn try* [f] (try (f) (catch Exception e (str "THROWS " (ex-message e) " " (ex-data e)))))
(def skin-construction
  {:steps [{:bind :path :call :path/samples :args [[:get :source :samples] [:get :settings]]}
           {:bind :flat :call :path/flatten :args [[:get :path] [:get :paint :stroke] [:literal 0.05] [:literal 1.0]]}
           {:bind :skin :call :path/swept :args [[:get :flat] [:literal 0.05]]}]
   :return [:get :skin]})
(def base (fx/z-record :union))
(def skin-rec (records/construct (-> base (assoc :construction skin-construction)
                                     (assoc-in [:paint :fill] {:rule :even-odd :color [0 0 0 1]})
                                     (update :paint dissoc :stroke-x))
                                 {:path/flatten g/flatten-stroke :path/swept g/swept-nib}))
(println "G. Codex: a construction over two extension capabilities returns the swept skin as the path value")
(println "   the record's path is now the skin: subpaths" (count (:subpaths (:path/value skin-rec))))
(def fill-only (assoc skin-rec :path/paint {:fill {:rule :even-odd :color [0 0 0 1]}}))
(println "   filled even-odd, classify (64,64):" (c/classify fill-only [64.0 64.0]) " (40,64):" (c/classify fill-only [40.0 64.0]))
(def fill-nz (assoc skin-rec :path/paint {:fill {:rule :nonzero :color [0 0 0 1]}}))
(println "   filled nonzero, classify (64,64):" (c/classify fill-nz [64.0 64.0]))
(let [outer (:path/value (records/construct {:identity {:id :r :revision 1} :construction records/border :source {:x 24 :y 24 :width 80 :height 80 :radius 12} :paint {:fill {:color [1 1 1 1]}}}))
      shape (c/validate-component! {:path/material-id :s :path/revision 1 :path/value outer
                                    :path/paint {:fill {:rule :nonzero :color [0.94 0.32 0.18 0.96]}
                                                 :clip {:path (:path/value skin-rec) :rule :nonzero}}})]
  (println "   rounded rect clipped by the returned skin: classify (64,64):" (c/classify shape [64.0 64.0]) " (30,60):" (c/classify shape [30.0 60.0]) " (60,40):" (c/classify shape [60.0 40.0])))
(println "   an op the table lacks →" (try* #(records/construct (assoc base :construction {:steps [{:bind :face :call :geometry/arrange :args []}] :return [:get :face]}))))
(println "   which fields did the construction read? " (try* #(keys (:bindings (app.client.engine.executor/execute records/draw base records/capabilities)))) " (no read report; the bindings are every input)")
(System/exit 0)
