(ns app.client.shapes.rect
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric-svg :as svg]
            [app.client.electric-codemirror :as cm]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-ui5 :as ui5]
            [hyperfiddle.electric-ui4 :as ui4]
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
                 [app.server.rama :as rama :refer [!subscribe nodes-pstate get-event-id add-new-node update-node]]])))


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


(e/defn rect [[id {:keys [y x type id type-specific-data]}]]
  (e/client
    (let [#_#_!cm-text (atom nil)
          #_#_cm-text  (e/watch !cm-text)
          #_#_read     (fn [edn-str]
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
          dragging? (atom false)
          h (:height type-specific-data)
          w (:width type-specific-data)]

      #_(svg/g
          #_(svg/rect
              (dom/props {:id     dom-id
                          :x      (subscribe. x-p)
                          :y      (subscribe. y-p)
                          :width  (subscribe. width-p)
                          :height 400;(subscribe. height-p)
                          :rx     "10"
                          #_#_:fill   (:editor-border (theme. ui-mode))})
              (dom/on "click" (e/fn [e]
                                (println "clicked the rect.")))
              #_(dom/on "mousedown" (e/fn [e]
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
              #_(dom/on "mouseup" (e/fn [e]
                                    (println "mouseup the rect.")
                                    (reset! !border-drag? false)))))
     (when (and (some? x)(some? y)(some? h)(some? w))
      (svg/foreignObject
        (dom/props {:x      x;(subscribe. x-p)     ;(+  (subscribe. x-p) 5)
                    :y      y ;(subscribe. y-p)     ;(+  (subscribe. y-p)  5)
                    :height h ;(subscribe. height-p);(-  (subscribe. height-p)  10)
                    :width  w ; (subscribe. width-p) ;(-  (subscribe. width-p)   10)
                    :fill   "white"
                    :id     id
                    :style {:display "flex"
                            :flex-direction "column"
                            :border "1px solid black"
                            :border-radius "10px"
                            :background-color "white"
                            :overflow "scroll"}})
        #_(dom/div
            (dom/props {:style {:background-color "white"
                                :display          "flex"
                                :overflow         "scroll"
                                :color            "black"
                                :flex-direction   "column"
                                :font-size        "23px"
                                :padding          "20px"
                                :border-radius    "10px"}})

            (dom/div (dom/text (:text type-specific-data))))
        (dom/on "mousemove" (e/fn [e]
                              (e/client
                                (.preventDefault e)
                                (when @dragging?
                                  (println "MOUSE MOVE element")
                                  ;(println "////////" "&&&&&7" (fc/browser-to-svg-coords e viewbox (.getElementById js/document (name id))))
                                  ;(js/console.log "--**" (.getElementById js/document (name id)))
                                  (let [[cx cy] (fc/element-new-coordinates1 e (.getElementById js/document (name id)))
                                        new-x  (- cx (/ w 2))
                                        new-y  (- cy (/ h 2))]
                                    ;(println "new x " new-x "ox" x "new y " new-y "oy" y)
                                    (when (and (some? new-x)
                                            (some? new-y))
                                      ;(println "NOT NIL new x " new-x "new y " new-y)
                                      (e/server
                                         (update-node
                                           [text-p "HELLO WORLD"]
                                           {:graph-name  :main
                                            :event-id    (get-event-id)
                                            :create-time (System/currentTimeMillis)}
                                           true
                                           false)
                                        (update-node
                                          [x-p new-x]
                                          {:graph-name  :main
                                           :event-id    (get-event-id)
                                           :create-time (System/currentTimeMillis)}
                                          true
                                          false)
                                        (update-node
                                          [y-p new-y]
                                          {:graph-name  :main
                                           :event-id    (get-event-id)
                                           :create-time (System/currentTimeMillis)}
                                          true
                                          true))))))))


        (dom/on "mousedown"  (e/fn [e]
                               (.preventDefault e)
                               (.stopPropagation e)
                               (println "MOUSEDOWN element")
                               (reset! dragging? true)))
        (dom/on "mouseup"    (e/fn [e]
                               (.preventDefault e)
                               (.stopPropagation e)
                               (println "pointerup element")
                               (reset! dragging? false)))
        #_(dom/on "mouseleave"    (e/fn [e]
                                    (.preventDefault e
                                      (.stopPropagation e)
                                      (println "mouseleave element")
                                      (reset! dragging? false))))
        #_(dom/on "mouseout"    (e/fn [e]
                                  (.preventDefault e)
                                  (.stopPropagation e)
                                  (println "mouseout element")
                                  (reset! dragging? false))

            #_(card-topbar. id)

            #_(dom/div
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
                  (dom/textarea (dom/text (subscribe.  text-p)))
                  #_(new cm/CodeMirror
                      {:parent dom/node}
                      read
                      identity
                      (subscribe. text-p)))
               #_(button-bar.))


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
                                          (create-new-child-node. id child-uid (+ x 600) y cm-text)))))))))))))

#_(update-node
    [[:08fe4616-4a43-4b5c-9d77-87fc7dc462c5 :x] 2000]
    {:graph-name  :main
     :event-id    (get-event-id)
     :create-time (System/currentTimeMillis)})