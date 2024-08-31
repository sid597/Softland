(ns app.client.shapes.circle
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [app.client.utils :refer [ ui-mode edges nodes
                                      is-dragging?  zoom-level last-position subscribe
                                      viewbox  context-menu? reset-global-vals]]

            #?@(:clj
                [[com.rpl.rama.path :as path :refer [subselect ALL FIRST keypath select]]
                 [app.client.utils :refer [!ui-mode !edges !nodes]]
                 [app.server.rama.util-fns :as rama :refer [!subscribe nodes-pstate get-event-id add-new-node]]])))




(e/defn circle [id size color]
  (println "cirtle id" id)
  (let [x-p          [ id :x :pos]
        y-p          [ id :y :pos]
        r-p          [ id :type-specific-data :r]
        color-p      [ id :type-specific-data :color]
        dragging?-p  [ id :type-specific-data :dragging?]]
    (e/client
      (println "SELECT: ")
      (svg/circle
        (dom/props {:id id
                    :cx (subscribe. x-p) ;(+ 100 (rand-int 1000)) ;
                    :cy  (subscribe. y-p) ;(+ 400 (rand-int 300));
                    :r  size ;(subscribe. r-p)
                    :fill color}) ;(subscribe. color-p)})
        #_(dom/on  "mousemove" (e/fn [e]
                                 (.preventDefault e)
                                 (when dragging?
                                   (println "dragging element"
                                     (let [el      (.getElementById js/document (name id))
                                           [x y]   (fc/element-new-coordinates1 e el)]
                                       (e/server (swap! !nodes assoc-in [k :x]  x))
                                       (e/server (swap! !nodes assoc-in [k :y] y)))))))
        #_(dom/on "mousedown"  (e/fn [e]
                                 (.preventDefault e)
                                 (.stopPropagation e)
                                 (println "pointerdown element")
                                 (e/server (swap! !nodes assoc-in [k :dragging?] true))))
        #_(dom/on "mouseup"    (e/fn [e]
                                 (.preventDefault e)
                                 (.stopPropagation e)
                                 (println "pointerup element")
                                 (e/server (swap! !nodes assoc-in [k :dragging?] false))))
        #_(dom/on "mouseleave"    (e/fn [e]
                                    (.preventDefault e)
                                    (.stopPropagation e)
                                    (println "mouseleave element")
                                    (e/server (swap! !nodes assoc-in [k :dragging?] false))))
        #_(dom/on "mouseout"    (e/fn [e]
                                  (.preventDefault e)
                                  (.stopPropagation e)
                                  (println "mouseout element")
                                  (e/server (swap! !nodes assoc-in [k :dragging?] false))))))))

