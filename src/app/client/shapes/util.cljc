(ns app.client.shapes.util
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [clojure.edn :as edn]
            [app.client.shapes.rect :refer [rect]]
            [app.client.shapes.circle :refer [circle]]
            [clojure.pprint :as pprint]
            [app.client.mode :refer [theme]]
            [app.client.utils :refer [ ui-mode edges nodes
                                      is-dragging?  zoom-level last-position subscribe
                                      viewbox  context-menu? reset-global-vals new-uuid]]

            #?@(:clj
                [[app.server.llm :refer [chat-complete]]
                 [com.rpl.rama.path :as path :refer [subselect ALL FIRST keypath select]]
                 [app.client.utils :refer [!ui-mode !edges !nodes]]
                 [app.server.rama.util-fns :as rama :refer [!subscribe nodes-pstate get-event-id add-new-node]]])))


(e/defn create-new-child-node [parent-id child-uid x y cm-text]
  (e/client
    (let [edge-id (new-uuid)
          rect-props {:id child-uid
                      :x x
                      :y y
                      :width 400
                      :height 800
                      :type "rect"
                      :text " "
                      :fill (:editor-background (theme. ui-mode))}
          edge-props {:id   edge-id
                      :from parent-id
                      :to   child-uid
                      :type "line"
                      :color (:edge-color (theme. ui-mode))}]
      (e/server
        (swap! !nodes assoc child-uid rect-props)
        (swap! !edges assoc edge-id edge-props)
        (swap! !nodes assoc-in [parent-id :text] cm-text)
        (chat-complete
           {:messages [{:role "user" :content cm-text}]
            :render-uid child-uid})))))

(e/defn new-line-el [[k {:keys [id x1 y1 x2 y2 stroke stroke-width]}]]
  (e/client
    (println "dom props")
    (svg/line
      (dom/props {:id "draw"
                  :x1  x1
                  :y1  y1
                  :x2  x2
                  :y2  y2
                  :stroke (:edge-color (theme. ui-mode))
                  :stroke-width 4}))))

(e/defn shape-selector [id node-count]
  (let [cnc (e/watch node-count)
        color (nth ["red" "blue" "green" "yellow" "purple"
                    "orange" "pink" "teal" "indigo" "cyan"] (rand-int 9))]
    (if (<= (count cnc ) 12)
      ;(rect. id :rect)
      (circle. id 4 color)
      (circle. id 1 color))))