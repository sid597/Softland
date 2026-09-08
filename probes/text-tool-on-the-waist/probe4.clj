;; probe4.clj — how the continuation grows when the loop collects (placements in state),
;; and the open-stream question: can a run suspend with nothing left to consume?
(ns probe4
  (:require [app.client.engine.executor :as e]
            [app.client.path.construction :as c]
            [clojure.pprint :as pp]))
(defn report [label m] (println (str "\n=== " label " ===")) (pp/pprint m) (flush))
(defmacro probe [label & body]
  `(try (report ~label (do ~@body))
        (catch Throwable t# (report ~label {:threw (.getMessage t#) :data (ex-data t#)}))))
(def font {:glyphs {"a" {:advance 6 :box [1 3 4 5] :ink 1} "b" {:advance 6 :box [1 0 4 8] :ink 1}
                    "l" {:advance 3 :box [1 0 1 8] :ink 1} " " {:advance 3 :box [0 7 1 1] :ink 0}}})
(defn shape-by-lookup [font ks] (mapv (fn [{:keys [i ch]}] (merge {:i i :ch ch} (get-in font [:glyphs ch]))) ks))
(def table (merge c/capabilities
                  {:text/shape {:args [:keys :font] :needs [[:keys] [:font]] :run (fn [{:keys [keys font]} _] (shape-by-lookup font keys))}
                   :collect {:args [:into :item] :needs [[:into] [:item]] :run (fn [{:keys [into item]} _] (if (nil? item) into (conj into item)))}}))
(def wrap-rule [:<= [:+ [:get :state :x] [:get :g :advance]] [:+ [:get :run :at 0] [:get :tool :width]]])
(def pen-x [:if wrap-rule [:get :state :x] [:get :run :at 0]])
(def pen-y [:if wrap-rule [:get :state :y] [:+ [:get :state :y] [:get :tool :line-height]]])
(def layout-record
  {:roots {:tool {:width 30 :line-height 10}}
   :program
   {:steps [{:out :shaped :op :text/shape :args {:keys [:get :keys] :font [:get :font]}}]
    :each {:items [:get :shaped] :item :g :fields [:i :ch :advance :box :ink]
           :state {:x [:get :run :at 0] :y [:get :run :at 1] :placed [:literal []]}
           :steps [{:out :placed :op :collect
                    :args {:into [:get :state :placed]
                           :item {:i [:get :g :i] :ch [:get :g :ch] :ink [:get :g :ink]
                                  :rect [[:+ pen-x [:get :g :box 0]] [:+ pen-y [:get :g :box 1]] [:get :g :box 2] [:get :g :box 3]]}}}]
           :next {:x [:+ pen-x [:get :g :advance]] :y pen-y :placed [:get :placed]}}
    :return {:placements {:items [:get :state :placed]}}}})
(defn ks [n] (vec (map-indexed (fn [i ch] {:i i :ch (str ch)}) (apply str (take n (cycle "ball ab "))))))
(defn scope [n] {:run {:id "run-1" :kind :text/run :by "sid" :at [2 2]} :keys (ks n) :font font})

(probe "14 continuation bytes when the loop collects placements (state-after per row)"
  (into {} (for [n [25 50 100 200]]
             (let [c (:continuation (e/run layout-record (scope (inc n)) table {:until n}))]
               [n {:bytes (alength (e/encode c)) :rows (count (:history c)) :placed (count (get-in c [:state :placed]))}]))))

(probe "15 an open stream: suspend with every key consumed?"
  (let [n 7 r-until-n (e/run layout-record (scope n) table {:until n})
        r-until-n-1 (e/run layout-record (scope n) table {:until (dec n)})]
    {:until=count {:status (:status r-until-n) :has-continuation? (contains? r-until-n :continuation)}
     :until=count-1 {:status (:status r-until-n-1) :at (:at r-until-n-1)}
     :receipt "executor.cljc:305 suspends at :until only while (< at (count items)); a complete run returns no continuation"}))
(println "\nDONE")
