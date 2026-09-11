(ns softland.inland.paint
  "Softland target for authored descriptions. Takes occurrence-keyed paint/input
   records. Gives rendered pixels and addressed events. Holds node resources only."
  (:require [hyperfiddle.electric3 :as e]
            [softland.inland.nodes :as n]
            [softland.inland.execution :as x]
            [softland.inland.geometry :as g]
            [softland.inland.scene :as scene]
            [softland.inland.total :as total]
            #?(:cljs [softland.inland.session :as session])
            #?(:cljs [softland.inland.input :as input])))

(e/declare Items)

(e/defn Field [r s id d]
  (e/client
    (let [!draft (session/cell s (:cell d))
          draft (e/watch !draft) value (str (:value draft ""))
          el (e/input (input/field id !draft :value))
          [px py w h] (:box d)
          [start end] (or (:selection draft) [(count value) (count value)])
          positioned (n/Text r (str id "/text") 52 :mono value
                       [(+ px 12) (+ py 24) (- w 24) (- h 20) (when (:focused draft) end)] (:size d 12) g/silver)]
      (input/sync! el value false)
      (.setAttribute el "aria-label" (:label d "Text input"))
      (n/Path r (str id "/border") 40
        (g/rect id (:box d) 6 [0.06 0.05 0.1 0.8] (if (:focused draft) g/blue g/line-color)))
      (n/Hit r id 60 (:box d) {:kind :native-input :run #(input/focus! id positioned %)})
      (when (:focused draft)
        (e/for-by first [row (filter (fn [[_ rect]] (<= (+ py 8) (:y rect) (- (+ py h) (:h rect) 8)))
                              (input/selection positioned start end))]
          (let [[line {:keys [x y w h]}] row]
            (n/Path r (str id "/sel/" line) 48 (g/rect id [x y w h] 0 [0.5 0.51 0.88 0.35] nil))))
        (when (= start end)
          (let [{:keys [x y h]} (input/caret positioned end)]
            (when (<= (+ py 8) y (- (+ py (nth (:box d) 3)) h 8))
              (n/Path r (str id "/caret") 55 (g/line id [[x y] [x (+ y h)]] g/coral 1.3)))))))))

(defn path-material [id d]
  (case (:shape d)
    :rect (g/rect id (:box d) (:radius d 0) (:fill d) (:stroke d))
    :ellipse (g/ellipse id (:box d) (:angle d 0) (:fill d) (:stroke d) (:width d 1))
    :line (g/line id (:points d) (:stroke d) (:width d 1))
    (g/material id (:source d) (:fill d) (:stroke d) (:width d 1))))

(defn finite-number? [v]
  (and (number? v) #?(:clj (Double/isFinite (double v)) :cljs (js/Number.isFinite v))))

(defn description-error [d]
  (try
    (cond
      (not (and (map? d) (string? (:id d)))) "Every rendered occurrence needs an identity."
      (and (not= :repeat (:kind d))
           (not (and (vector? (:box d)) (every? finite-number? (:box d))))
           (not (and (= :path (:kind d)) (= :line (:shape d))))) "Paint needs a finite box."
      :else
      (case (:kind d)
        :text (when-not (and (#{:sans :mono} (:face d :sans)) (pos? (:size d 14))) "Choose an available font and a positive size.")
        :path (do (path-material (:id d) d) nil)
        :hit (when-not (keyword? (get-in d [:event :kind])) "A hit names an event kind.")
        :input (when-not (string? (:cell d)) "An input names its owned session cell.")
        :scene (let [[_ _ w h] (:box d)] (scene/region (:shapes d) w h (:options d)) nil)
        :repeat (when-not (and (vector? (:items d)) (<= (count (:items d)) 256)
                              (= (count (:items d)) (count (distinct (:items d)))))
                  "A repeated presentation needs at most 256 distinct occurrence keys.")
        "This paint kind is not available."))
    (catch #?(:clj Throwable :cljs :default) _ "The rendering primitive cannot consume this description.")))

(defn descriptions-error [descriptions]
  (cond (not (sequential? descriptions)) "A view returns a sequence of paint descriptions."
        (> (count descriptions) 256) "A view exceeded its 256 occurrence budget."
        (not= (count (remove nil? descriptions)) (count (distinct (map :id (remove nil? descriptions)))))
        "Rendered occurrence identities must be distinct."
        :else (some description-error (remove #(or (nil? %) (= false (:visible %))) descriptions))))

(e/defn Item [r s owner workspace context occurrence d]
  (let [id (str occurrence "/" (:id d))]
    (case (:kind d)
      :text (n/Text r id (:order d 50) (:face d :sans) (str (:text d)) (:box d) (:size d 14) (:color d g/silver))
      :path (n/Path r id (:order d 40) (path-material id d))
      :hit (n/Hit r id (:order d 55) (:box d) (assoc (:event d) :definition occurrence :occurrence id))
      :input (Field r s id d)
      :scene (n/Scene r id (:shapes d) (:box d) (:options d))
      :repeat (e/for-by second [pair (map-indexed vector (:items d))]
                (let [[at item] pair
                      descriptions (x/Call s owner workspace context (:definition d)
                                     (merge (:bindings d) {:item item :at at}) 0)]
                  (Items r s owner workspace context (str id "/" item) descriptions)))
      nil)))

(e/defn Items [r s owner workspace context occurrence descriptions]
  (let [failure (if (total/blocked? descriptions) descriptions
                  (when-let [reason (descriptions-error descriptions)] {:runtime/status :failed :reason reason}))]
  (if failure
    (n/Text r (str occurrence "/read-status") 80 :sans
      (str occurrence " / " (name (:runtime/status failure)) " / " (:reason failure "Reading accepted material"))
      [1040 145 350] 12 g/coral)
    (e/for-by :id [d (remove nil? descriptions)]
      (when (not= false (:visible d)) (Item r s owner workspace context occurrence d))))))
