(require '[app.client.path.component :as c] '[app.client.path.records :as r])
(defn try* [f] (try (f) (catch Exception e (str "REJECTED " (ex-message e) " " (select-keys (ex-data e) [:error-type :path])))))
(println "== Claude lane: can a record hand the component a path value, or a source of an unknown kind, with its own construction? ==")
(def skin (:path (first (:regions (c/run r/harness-z {})))))
(println "1. a record whose source is a path value (kind :path) and a construction that fills it:"
  (try* #(c/validate-component! {:path/material-id :x :path/revision 1
                                 :path/source {:kind :path :path skin}
                                 :path/paint {:fill {:rule :nonzero :color [0 0 0 1]}}
                                 :path/construction {:steps [{:out "fill" :op :path/fill-region :path "source.path" :rule :nonzero}]
                                                     :return {:path "source.path" :regions ["fill"]}}})))
(println "2. the same with a :network source (attack 2's kind):"
  (try* #(c/validate-component! {:path/material-id :x :path/revision 1
                                 :path/source {:kind :network :vertices {} :edges []}
                                 :path/paint {:fill {:rule :nonzero :color [0 0 0 1]}}})))
(println "3. does the executor itself mind? run without validation, source kind :path, construction fills source.path →"
  (let [run (c/run {:path/material-id :x :path/revision 1 :path/source {:kind :path :path skin}
                    :path/paint {:fill {:rule :nonzero :color [0 0 0 1]}}
                    :path/construction {:steps [{:out "fill" :op :path/fill-region :path "source.path" :rule :nonzero}]
                                        :return {:path "source.path" :regions ["fill"]}}} {})]
    [(:ok? run) (count (:regions run))]))
(println "4. the clip already takes a path value:" (try* #(do (c/validate-component! (assoc-in r/holed-concave [:path/paint :clip] {:path skin :rule :nonzero})) "accepted")))
(System/exit 0)
