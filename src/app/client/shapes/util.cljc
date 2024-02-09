(ns app.client.shapes.util
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric-svg :as svg]
            [app.electric-codemirror :as cm]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.flow-calc :as fc]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [app.mode :refer [theme]]

            #?@(:clj
                [[app.server.llm :refer [chat-complete]]
                 [com.rpl.rama.path :as path :refer [subselect ALL FIRST keypath select]]
                 [app.server.rama :as rama :refer [!subscribe nodes-pstate get-event-id add-new-node]]])))




#?(:clj (def !ui-mode (atom :dark)))
(e/def ui-mode (e/server (e/watch !ui-mode)))

(defn new-uuid []
  (keyword (str (random-uuid))))



#?(:clj (def !edges (atom {#_#_:sv-line {:id :sv-line
                                         :type "line"
                                         :to   :sv-circle
                                         :from :sv-circle1
                                         :color "black"}})))

(e/def edges (e/server (e/watch !edges)))

#?(:clj (def !nodes (atom {#_#_:sv-circle {:id :sv-circle :dragging? false
                                           :x 700
                                           :y 100
                                           :r 100
                                           :type "circle"
                                           :color "red"}
                           #_#_:sv-circle1 {:id :sv-circle1
                                            :dragging? false
                                            :x 900
                                            :y 300
                                            :r 100
                                            :type "circle"
                                            :color "green"}
                           :rect       {:id :rect
                                        :x 500
                                        :y 600
                                        :text "GM Hello"
                                        :width 400
                                        :height 800
                                        :type "rect"
                                        :fill  "lightblue" #_"#111110" #_"#1E1523" #_"#1B1A17" #_"#111A27"}})))

(e/def nodes (e/server (e/watch {})))


#?(:cljs (def !new-line (atom {:start nil
                               :end   nil})))
#?(:cljs (def !border-drag? (atom false)))
#?(:cljs (def !is-dragging? (atom false)))
#?(:cljs (def !zoom-level (atom 1.5)))
#?(:cljs (def !last-position (atom {:x 0 :y 0})))
#?(:cljs (def !viewbox (atom [0 0 2000 2000])))

(e/def viewbox (e/client (e/watch !viewbox)))
(e/def last-position (e/client (e/watch !last-position)))
(e/def zoom-level (e/client (e/watch !zoom-level)))
(e/def is-dragging? (e/client (e/watch !is-dragging?)))
(e/def new-line (e/client (e/watch !new-line)))




(e/defn subscribe [path]
  (e/server (new (!subscribe (concat [(keypath :main)]
                               path)
                   nodes-pstate))))


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
