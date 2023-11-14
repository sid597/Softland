(ns app.electric-flow
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric-dom2 :as dom]
            [app.data :as data]
            [hyperfiddle.electric-ui4 :as ui]))


#?(:clj (def node (atom "GM")))
(e/def nod (e/server (e/watch node)))

#?(:clj (def !msgs (atom [1 2 3])))

(e/def msgs (e/server (e/watch !msgs)))

(e/def edges (e/server (e/watch data/!edges)))

#?(:clj (def !nodes (atom {:sv-circle {:id "sv-circle"
                                       :draggable? true
                                       :x 700
                                       :y 100
                                       :r 80
                                       :type "circle"
                                       :color "red"}
                           :sv-circle1 {:id "sv-circle1"
                                        :draggable? true
                                        :x 900
                                        :y 300
                                        :r 60
                                        :type "circle"
                                        :color "green"}})))

(e/def nodes (e/server (e/watch !nodes)))

(e/defn TwoClocks []
  (println "nod " nod msgs)
  (e/server
    (e/for-by identity [node nodes]
      (e/client
        (dom/div (dom/text node))
        (let [[k {:keys [id x y r color draggable?]}] node]
          (println "node" node id x y r color draggable?)
          (svg/svg (dom/props {:viewBox "0 0 3000 1000"})
            (svg/circle
              (dom/props {:id id
                          :cx x
                          :cy y
                          :r  r
                          :fill color})
              (dom/on  "click" (e/fn [e]
                                 (e/server (swap! !nodes assoc-in  [k :r] (+ r 4))))))))))))


(e/defn main []
    (TwoClocks.))


