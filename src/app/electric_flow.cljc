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

(e/def nodes (e/server (e/watch data/!nodes)))

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
              (let [nd (.getElementById js/document id)]
                 (dom/on  nd "click" (e/fn [e]
                                       (.preventDefault e)
                                       (println "dragging element" k (e/server data/!nodes))
                                       (e/server (swap! data/!nodes assoc-in  [k :r] (+ r 4)))))))))))))


(e/defn main []
    (TwoClocks.))


