(ns app.client.shapes.rect
  (:import (missionary Cancelled))
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
                 [app.server.rama :as rama :refer [!subscribe nodes-pstate get-path-data get-event-id add-new-node update-node]]])))


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


#?(:cljs (defn current-time-ms []
           (js/Date.now)))
#?(:cljs (def !counter
           (atom 0)))


#?(:cljs (defn debounce [dur >in]
           (m/ap
             (let [x (m/?< >in)]
               (try (m/? (m/sleep dur x))
                    (catch Cancelled e
                      (m/amb)))))))
#?(:cljs
   (defn el-mouse-move-state> [movable id dragging?]
     (m/observe
       (fn [!]
         (let [sample (fn [e]
                        (when dragging?
                         (! (do
                              (.preventDefault e)
                             {:nid   id
                              :cords [(.-clientX e)
                                      (.-clientY e)]}))))]


           (.addEventListener movable "mousemove" sample #js {"passive" false})
           #(.removeEventListener movable "mousemove" sample))))))


#?(:cljs (def !global-atom (atom nil)))
#?(:cljs (defn global-client-flow []
           (m/signal ;; https://clojurians.slack.com/archives/C7Q9GSHFV/p1691599800774709?thread_ts=1691570620.457499&cid=C7Q9GSHFV
            (m/latest
              (fn [x]
                (println x)
                x)
              (m/watch !global-atom)))))

#?(:cljs (defn el-mouse-move-state< [movable id dragging?]
           (->> (el-mouse-move-state> movable id dragging?)
             (e/throttle 10)
             (m/reductions {} {:cord [0 0]
                               :time (current-time-ms)})
             (m/relieve {})
             (m/latest (fn [cords]
                         (reset! !global-atom cords)))
             (m/signal))))


#?(:cljs (def !node-pos-atom (atom nil)))
#?(:cljs (defn node-pos-flow []
           (m/signal ;; https://clojurians.slack.com/archives/C7Q9GSHFV/p1691599800774709?thread_ts=1691570620.457499&cid=C7Q9GSHFV
            (m/latest
              (fn [x]
                  x)
              (m/watch !node-pos-atom)))))



#?(:cljs (defn server-update []
           (->> (node-pos-flow)
             (debounce 100)
             (m/reductions {} {:x 0 :y 0 :id 0})
             (m/relieve {})
             (m/latest (fn [new-data]
                         ;(println "6. Send to SERVER " new-data)
                         new-data))
             (m/signal))))


(e/defn watch-server-update [path]
  (e/client
    (println "SETUP WATCH : " path)
    (let [x? (= :x (second path))
          new-data (subscribe. path)]
      ;(println "8. WATCH SERVER UPDATE" new-data "-c-" @!counter)
      ;(println (:time new-data) " -- NEW DATA FROM SERVER --" new-data "--" path)
      (if x?
        (reset! !global-atom {:nid (first path)
                              :x new-data})
        (reset! !global-atom {:nid (first path)
                              :y new-data})))))



(e/defn rect [id node type]
  (e/client
    (println "RECT --" id "--" node)
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
          cord-x (atom nil)
          cord-y (atom nil)
          ;_ (println "extra data" extra-data)
          !xx (atom {:pos (-> node :x :pos)
                     :time (-> node :x :time)})
          xx (e/watch !xx)
          !yy (atom {:pos (-> node :y :pos)
                     :time (-> node :y :time)})
          yy (e/watch !yy)
          !hh (atom {:height (:height extra-data)
                     :time (:time node)})
          hh  (:height (e/watch !hh))
          !ww (atom {:width (:width extra-data)
                     :time (:time node)})
          ww  (:width (e/watch !ww))
          !text (atom (:text extra-data) #_(subscribe. text-p))
          block-text (e/watch !text)
          !fx (atom nil)
          fx (e/watch !fx)
          !fy (atom nil)
          fy (e/watch !fy)
          reset-after-drag (e/fn [msg]
                             (e/client
                               (when @!dragging?
                                 (do
                                   ;(println "RESET AFTER DRAG")
                                   (when (not= (e/server (first (get-path-data [(keypath :main) id :x] nodes-pstate)))
                                           @!xx))

                                   (reset! !dragging? false)))))]
      (watch-server-update. x-p)
      (watch-server-update. y-p)
      (svg/g
        (svg/rect
          (dom/props {:x      (:pos xx);(subscribe. x-p)     ;(+  (subscribe. x-p) 5)
                      :y      (:pos yy) ;(subscribe. y-p)     ;(+  (subscribe. y-p)  5)
                      :height hh;(subscribe. height-p);(-  (subscribe. height-p)  10)
                      :width  ww ; (subscribe. width-p) ;(-  (subscribe. width-p)   10)
                      ;:fill   "red"
                      :id     id
                      :style {:display "flex"
                              :flex-direction "column"
                              :border "1px solid black"
                              :border-radius "10px"
                              :background-color "red"
                              :overflow "scroll"}}))
        (when (= :img type)
          (svg/image
            (dom/props
              {:x (:pos xx)
               :y (:pos yy)
               :width (+ ww 10)
               :height (+ hh 10)
               :href (-> extra-data :path)
               :preserveAspectRatio true})))
       (new (el-mouse-move-state< dom/node id dragging?))
       (let [{:keys [x y nid]} (new (server-update))]
           (when (= nid id)
               ;(println (:time x) " 7. SERVER DATA UPDATE "  x "::" y)
               (e/server
                (update-node
                    [x-p x]
                    {:graph-name  :main
                       :event-id    (get-event-id)
                       :create-time (System/currentTimeMillis)}
                    false
                    false)
                (update-node
                    [y-p y]
                    {:graph-name  :main
                       :event-id    (get-event-id)
                       :create-time (System/currentTimeMillis)}
                    false
                    true))))
       (let [{:keys [nid cords x y]} (new (global-client-flow))
               [cx cy] cords]
           (do
            (when  (= id nid)
                (cond (and cx cy
                        (not= @cord-x cx)
                        (not= @cord-y cy)) (let [ctm      (.getScreenCTM dom/node)
                                                 dx       (/ (- cx (.-e ctm))
                                                            (.-a ctm))
                                                 dy       (/ (- cy (.-f ctm))
                                                             (.-d ctm))]
                                             (do
                                              (swap! !xx update-in [:pos] (fn [curx]
                                                                           (+ curx (- dx fx))))
                                              (swap! !xx update-in [:time]  current-time-ms)
                                              (swap! !yy update-in [:time] current-time-ms)
                                              (swap! !yy update-in [:pos] (fn [cury]
                                                                           (+ cury (- dy fy))))
                                              (swap! !fx (constantly dx))
                                              (swap! !fy (constantly dy))
                                              (reset! cord-x cx)
                                              (reset! cord-y cy)
                                              (reset! !node-pos-atom {:x xx
                                                                      :y yy
                                                                      :nid id})))

                      ;; Only happen for server based updates
                      (some? x)   (let [ct (:time @!xx)
                                        nt (-> x :time)
                                        nx (-> x :pos)
                                        new-x {:pos nx :time nt}]
                                    (when (> (- nt ct) 0)
                                      (println "server " cx nx  cy)
                                      (reset! !xx new-x)
                                      (reset! !node-pos-atom {:x new-x
                                                              :id id})))
                      (some? y)   (let [ct (:time @!yy)
                                        nt  (-> y :time)
                                        ny (-> y :pos)
                                        new-y {:pos ny :time nt}]
                                    (when (> (- nt ct) 0)
                                      (println "server " cx cy ny)
                                      (reset! !yy new-y)
                                      (reset! !node-pos-atom {:y new-y
                                                              :id id})))
                      :else         (println "THIS IS SOME OTHER TYPE OF DATA: " cx cy x y nid)))))

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
                                    #_(println "MOUSEDOWN " {:fx @!fx :fy @!fy
                                                             :xx xx :yy yy
                                                             :dx dx :dy dy
                                                             :cx cx :cy cy})
                                    (println "** Updatae fx" @!fx @!fy)
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
                                 (reset-after-drag. "mouseout element")))))))

