(ns app.client.shapes.rect
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric-svg :as svg]
            [app.client.electric-codemirror :as cm]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [app.client.mode :refer [theme]]
            [app.client.shapes.util :as sutil :refer [create-new-child-node]]
            [app.client.utils :refer [ ui-mode edges nodes
                                      is-dragging?  zoom-level last-position subscribe
                                      viewbox  context-menu? reset-global-vals]]
            [app.client.editor.core :refer [canvas]]
            [app.client.style-components.buttons :refer [icon-button]]
            [hyperfiddle.electric-ui4 :as ui]
            #?@(:cljs
                [[app.client.utils :refer [!border-drag? !is-dragging? !zoom-level !last-position !viewbox !context-menu?]]]
                :clj
                [[com.rpl.rama.path :as path :refer [subselect ALL FIRST keypath select]]
                 [app.client.utils :refer [!ui-mode !edges !nodes]]
                 [app.server.rama :as rama :refer [!subscribe nodes-pstate get-event-id add-new-node]]])))


(e/defn card-topbar [id]
  (e/client
    (dom/div
      (dom/props {:id  (str "card-topbar-" id)
                  :style {:background-color "white"
                          :display "flex"
                          :flex-direction "row"
                          :align-items "center"
                          :gap "8px"
                          :justify-content "space-between"
                          :border-bottom "1px solid black"
                          :padding "5px"}})
      (dom/div
        (dom/style {:display "flex"
                    :gap "8px"})
        (icon-button. :drag-pan-icon)
        (icon-button. :closed-lock-icon))

      (dom/div
        (dom/style {:display "flex"
                    :gap "8px"})
        (icon-button. :maximise-icon)
        (icon-button. :close-small-icon)))))

(e/defn outlined-button [name]
  (e/client
    (dom/button
      (dom/props {:class "outlined-button"
                  :style {:background "white"
                          :padding "2px 5px"
                          :font-size "14px"
                          :border "none"
                          :font "200  13px IA writer Quattro S"
                          :display "flex"}})
      (dom/text name))))

(e/defn button-bar []
  (e/client

    (dom/div
      #_(dom/props {:class "button-sep"
                    :style {:display "flex"
                            :flex-direction "column"
                            :color "black"
                            :padding "5px"
                            :border "1px solid black"
                            :height "400px"
                            :border-radius "10px"
                            :font "200  17px IA writer Quattro S"}})
      #_(dom/text "Strategy: ")
      (dom/div
        (dom/props {:class "button-bar"
                    :style {:display "flex"
                            :flex-direction "row"
                            :gap "8px"
                            :justify-content "space-between"
                            :padding "5px"
                            :color "black"
                            :font "200  17px IA writer Quattro S"
                            :overflow-x "auto"}})
        (outlined-button. "1")
        (outlined-button. "2")
        (outlined-button. "3")
        (outlined-button. "4")
        (outlined-button. "5")
        (outlined-button. "6")
        (outlined-button. "7")))))


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
                          :height 400;(subscribe. height-p)
                          :rx     "10"
                          #_#_:fill   (:editor-border (theme. ui-mode))})
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
              (dom/props {:x      (subscribe. x-p)     ;(+  (subscribe. x-p) 5)
                          :y      (subscribe. y-p)     ;(+  (subscribe. y-p)  5)
                          :height 398 ;(subscribe. height-p);(-  (subscribe. height-p)  10)
                          :width  (subscribe. width-p) ;(-  (subscribe. width-p)   10)
                          ;:fill   "black"
                          :style {:display "flex"
                                  :flex-direction "column"
                                  :border "1px solid black"
                                  :border-radius "10px"
                                  :overflow "scroll"}})
              (dom/div
                (dom/props {:style {:background-color "white"
                                    :height           "100%"
                                    :width            "100%"
                                    :display          "flex"
                                    :overflow         "scroll"
                                    :flex-direction   "column"
                                    :border-radius    "10px"}})
                (card-topbar. id)

                (dom/div
                  (dom/props {:class "middle-earth"
                              :style {:padding "5px"
                                      :background "whi}te"
                                      :display "flex"
                                      :flex-direction "column"
                                      :height "100%"}})
                  (dom/div
                    (dom/props {:id    (str "cm-" dom-id)
                                :style {:height   "100%"
                                        :overflow "scroll"
                                        :background "white"
                                        :box-shadow "black 0px 0px 2px 1px"
                                        ;:border "1px solid black"
                                        ;:border-radius "10px"
                                        :margin-bottom           "10px"}})
                    #_(canvas. dom-id)
                    (new cm/CodeMirror
                      {:parent dom/node}
                      read
                      identity
                      (subscribe. text-p)))
                 (button-bar.))


                #_(dom/div
                    (dom/button
                      (dom/props {:style {:background-color "white"
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

