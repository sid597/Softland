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
                [[app.client.utils :refer [!border-drag? !is-dragging? !zoom-level !last-position !viewbox !context-menu?]]
                 [missionary.core :as m]]
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

#?(:cljs
   (defn el-mouse-move-state> [movable]
     (m/observe
       (fn [!]
         (let [sample (fn [e]
                        (! (do
                             (.preventDefault e)
                             [(.-clientX e)
                              (.-clientY e)])))]
           (.addEventListener movable "mousemove" sample #js {"passive" false})
           #(.removeEventListener movable "mousemove" sample))))))


#?(:cljs (defn el-mouse-move-state< [movable]
           (->> (el-mouse-move-state> movable)
             (e/throttle 1)
             (m/reductions {} [0 0])
             (m/relieve {})
             (m/latest (fn [[cx cy]]
                         (let [ctm (.getScreenCTM movable)
                               dx  (/ (- cx (.-e ctm))
                                     (.-a ctm))
                               dy  (/ (- cy (.-f ctm))
                                     (.-d ctm))]
                           [dx dy]))))))



(e/defn rect [id node]
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
          !dragging? (atom false)
          dragging? (e/watch !dragging?)
          extra-data (:type-specific-data node)
          _ (println "extra data" extra-data)
          !xx (atom (:x node) #_(subscribe. x-p))
          xx (e/watch !xx)
          !yy (atom (:y node) #_(subscribe. y-p))
          yy (e/watch !yy)
          !hh (atom (:height extra-data) #_(subscribe. height-p))
          hh (e/watch !hh)
          !ww (atom (:width extra-data) #_(subscribe. width-p))
          ww (e/watch !ww)
          !text (atom (:text extra-data) #_(subscribe. text-p))
          block-text (e/watch !text)
          !fx (atom nil)
          !fy (atom nil)
          reset-after-drag (e/fn [msg]
                             (e/client
                               (when @!dragging?
                                 (do
                                   (println msg @!dragging?)
                                   (reset! !fx 0)
                                   (reset! !fy 0)
                                   (e/server
                                     (update-node
                                       [x-p (e/client @!xx)]
                                       {:graph-name  :main
                                        :event-id    (get-event-id)
                                        :create-time (System/currentTimeMillis)}
                                       false
                                       false)
                                     (update-node
                                       [y-p (e/client @!yy)]
                                       {:graph-name  :main
                                        :event-id    (get-event-id)
                                        :create-time (System/currentTimeMillis)}
                                       false
                                       true))
                                   (reset! !dragging? false)))))]


      (svg/g
        (svg/rect
          (dom/props {:x      xx;(subscribe. x-p)     ;(+  (subscribe. x-p) 5)
                      :y      yy ;(subscribe. y-p)     ;(+  (subscribe. y-p)  5)
                      :height hh ;(subscribe. height-p);(-  (subscribe. height-p)  10)
                      :width  ww ; (subscribe. width-p) ;(-  (subscribe. width-p)   10)
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
              (dom/div (dom/text block-text)))
          (let [[dx dy] (new (el-mouse-move-state< dom/node))]
            (when dragging?
              ;; why does it not work if I put the new-x in above let?
              (let [new-x (+ @!xx (- dx @!fx))
                    new-y (+ @!yy (- dy @!fy))]
                (reset! !xx new-x)
                (reset! !yy new-y))))


          (dom/on "mousedown"  (e/fn [e]
                                 (.preventDefault e)
                                 (.stopPropagation e)
                                 (let [cx (.-clientX e)
                                       cy (.-clientY e)
                                       ctm (.getScreenCTM dom/node)
                                       dx  (/ (- cx (.-e ctm))
                                              (.-a ctm))
                                       dy  (/ (- cy (.-f ctm))
                                             (.-d ctm))]
                                     (reset! !fx dx)
                                     (reset! !fy dy)
                                     (println "MOUSEDOWN " {:fx @!fx :fy @!fy
                                                            :xx xx :yy yy
                                                            :dx dx :dy dy
                                                            :cx cx :cy cy})
                                     (reset! !dragging? true))))
          (dom/on "mouseup"    (e/fn [e]
                                 (.preventDefault e)
                                 (.stopPropagation e)
                                 (reset-after-drag. "mouseup on element")))

          (dom/on "mouseleave"    (e/fn [e]
                                    (.preventDefault e)
                                    (.stopPropagation e)
                                    (reset-after-drag. "mouseleave on element")))
          (dom/on "mouseout"    (e/fn [e]
                                  (.preventDefault e)
                                  (.stopPropagation e)
                                  (reset-after-drag. "mouseout element"))

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