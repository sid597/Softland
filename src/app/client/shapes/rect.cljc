(ns app.client.shapes.rect
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
            [app.client.shapes.util :as sutil :refer [create-new-child-node]]

            #?@(:clj
                [[com.rpl.rama.path :as path :refer [subselect ALL FIRST keypath select]]
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

(e/defn rect [id]
  (e/server
    (let [pstate nodes-pstate]
      (e/client
        (println "id" id "pstate" pstate)
        (let [!cm-text (atom nil)
              cm-text  (e/watch !cm-text)
              read     (fn [edn-str]
                         (println "Read string:" (edn/read-string edn-str))
                         (reset! !cm-text (str edn-str))
                         (try (edn/read-string edn-str)
                              (catch #?(:clj Throwable :cljs :default) t
                                #?(:clj (clojure.tools.logging/error t)
                                   :cljs (js/console.warn t)) nil)))
              write    (fn [edn] (with-out-str (pprint/pprint edn)))
              dom-id   (str "dom-id-" (str id))
              x-p      [ id :x]
              y-p      [ id :y]
              text-p   [ id :type-specific-data :text]
              width-p  [ id :type-specific-data :width]
              height-p [ id :type-specific-data :height]
              fill-p   [ id :fill]]
          (svg/g
            (svg/rect
              (dom/props {:id     dom-id
                          :x      (subscribe. x-p)
                          :y      (subscribe. y-p)
                          :width  (subscribe. width-p)
                          :height (subscribe. height-p)
                          :fill   (:editor-border (theme. ui-mode))})
              (dom/on "click" (e/fn [e]
                                (println "clicked the rect.")))
              (dom/on "mousedown" (e/fn [e]
                                    (println "mousedown the rect.")
                                    (let [el    (.getElementById js/document (name dom-id))
                                          [x y] (fc/element-new-coordinates1 e  el)]
                                      (e/server
                                        (swap! !edges assoc :raw {:id :raw
                                                                  :type "raw"
                                                                  :x1 x
                                                                  :y1 y
                                                                  :x2 nil
                                                                  :y2 nil
                                                                  :stroke (:edge-color (theme. ui-mode))
                                                                  :stroke-width 4})))
                                    (reset! !border-drag? true)))
              (dom/on "mouseup" (e/fn [e]
                                  (println "mouseup the rect.")
                                  (reset! !border-drag? false))))
            (svg/foreignObject
              (dom/props {:x      (+  (subscribe. x-p) 5)
                          :y      (+  (subscribe. y-p)  5)
                          :height (-  (subscribe. height-p)  10)
                          :width  (-  (subscribe. width-p)   10)
                          :fill   "black"
                          :style {:display "flex"
                                  :flex-direction "column"
                                  :overflow "scroll"}})
              (dom/div
                (dom/props {:style {:background-color (subscribe. fill-p)
                                    :height           "100%"
                                    :display          "flex"
                                    :overflow         "scroll"
                                    :flex-direction   "column"}})

                (dom/div
                  (dom/props {:id    (str "cm-" dom-id)
                              :style {:height   "100%"
                                      :overflow "scroll"
                                      :width    "100%"}})
                  (new cm/CodeMirror
                    {:parent dom/node}
                    read
                    identity
                    (subscribe. text-p)))


                (dom/div
                  (dom/button
                    (dom/props {:style {:background-color (:button-background (theme. ui-mode))
                                        :border           "none"
                                        :padding          "5px"
                                        :border-width     "5px"
                                        :font-size        "18px"
                                        :color            (:button-text (theme. ui-mode))
                                        :height           "50px"
                                        :width            "100%"}})
                    (dom/text
                      "Save")

                    #_(dom/on "click" (e/fn [e]
                                        (let [child-uid (new-uuid)
                                              x         (e/server (rama/get-path-data x-p pstate))
                                              y         (e/server (rama/get-path-data y-p pstate))]
                                          (when (some? cm-text)
                                            (println "cm-text -->" cm-text)
                                            (create-new-child-node. id child-uid (+ x 600) y cm-text)))))))))))))))

