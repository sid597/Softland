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
                              (let [res {:id   id
                                         :time (current-time-ms)
                                         :cord [(.-clientX e)
                                                (.-clientY e)]}]
                                ;(println "1. OBSERVE MOUSE STATE" res)
                                res)))))]

           (.addEventListener movable "mousemove" sample #js {"passive" false})
           #(.removeEventListener movable "mousemove" sample))))))


#?(:cljs (def !global-atom (atom nil)))
#?(:cljs (defn global-client-flow []
           (m/signal ;; https://clojurians.slack.com/archives/C7Q9GSHFV/p1691599800774709?thread_ts=1691570620.457499&cid=C7Q9GSHFV
             (m/latest
               (fn [x]
                 (swap! !counter inc)
                 (println "3. Global client flow" x "-c-" @!counter)
                 (assoc x :c @!counter))

               (m/watch !global-atom)))))

#?(:cljs (defn el-mouse-move-state< [movable id dragging?]
           (->> (el-mouse-move-state> movable id dragging?)
             (e/throttle 10)
             (m/reductions {} {:cord [0 0]
                               :time (current-time-ms)})
             (m/relieve {})
             (m/latest (fn [new-data]
                         ;(println "2. Throttled mouse state" new-data)
                         (reset! !global-atom new-data))))))


#?(:cljs (def !node-pos-atom (atom nil)))
#?(:cljs (defn node-pos-flow []
           (m/signal ;; https://clojurians.slack.com/archives/C7Q9GSHFV/p1691599800774709?thread_ts=1691570620.457499&cid=C7Q9GSHFV
             (m/latest
               (fn [x]
                 (let [nd (assoc x :c @!counter)]
                   ;(println "5. Node pos flow " nd)
                   nd))

               (m/watch !node-pos-atom)))))



#?(:cljs (defn server-update []
           (->> (node-pos-flow)
             (debounce 100)
             (m/reductions {} {:x 0 :y 0 :id 0})
             (m/relieve {})
             (m/latest (fn [new-data]
                         ;(println "6. SERVER " new-data)
                         new-data)))))

(e/defn watch-server-update [path]
  (e/client
    (let [x? (= :x (second path))
          y? (= :y (second path))
          new-data (subscribe. path)]
      (println "8. WATCH SERVER UPDATE" new-data "-c-" @!counter)
      ;(println "-- NEW DATA FROM SERVER --" new-data "--" path)
      (if x?
        (reset! !global-atom {:id (first path)
                              :x new-data})
        (reset! !global-atom {:id (first path)
                              :y new-data})))))




(e/defn rect [id node]
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
          xx (:pos (e/watch !xx))
          !yy (atom {:pos (-> node :y :pos)
                     :time (-> node :y :time)})
          yy (:pos (e/watch !yy))
          !hh (atom {:height (:height extra-data)
                     :time (:time node)})
          hh  (:height (e/watch !hh))
          !ww (atom {:width (:width extra-data)
                     :time (:time node)})
          ww  (:width (e/watch !ww))
          !text (atom (:text extra-data) #_(subscribe. text-p))
          block-text (e/watch !text)
          !fx (atom nil)
          !fy (atom nil)
          reset-after-drag (e/fn [msg]
                             (e/client
                               (when @!dragging?
                                 (do
                                   (println "RESET AFTER DRAG")
                                   (reset! !fx 0)
                                   (reset! !fy 0)
                                   (when (not= (e/server (first (get-path-data [(keypath :main) id :x] nodes-pstate)))
                                           @!xx))

                                   (reset! !dragging? false)))))]
      (watch-server-update. x-p)
      (watch-server-update. y-p)
      (svg/g
        (svg/rect
          (dom/props {:x      xx;(subscribe. x-p)     ;(+  (subscribe. x-p) 5)
                      :y      yy ;(subscribe. y-p)     ;(+  (subscribe. y-p)  5)
                      :height hh;(subscribe. height-p);(-  (subscribe. height-p)  10)
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
              (dom/div (dom/text "GM: "@!xx)))
          (new (el-mouse-move-state< dom/node id dragging?))
          (let [server-data (new (server-update))
                nid (:id server-data)
                nx (:x server-data)
                ny (:y server-data)]

            (when (= nid id)
                ;(println "7. SERVER DATA UPDATE "  server-data)
                (e/server
                 (update-node
                     [x-p nx]
                     {:graph-name  :main
                        :event-id    (get-event-id)
                        :create-time (System/currentTimeMillis)}
                     false
                     false)
                 (update-node
                     [y-p ny]
                     {:graph-name  :main
                        :event-id    (get-event-id)
                        :create-time (System/currentTimeMillis)}
                     false
                     true))))
         (let [new-data (new (global-client-flow))]
           (when  (= id (:id new-data))
               #_(cljs.pprint/pprint (assoc new-data :4 (current-time-ms)
                                                     :4-3 (- (current-time-ms) (:3 new-data))
                                                     :4-2 (- (current-time-ms) (:2 new-data))
                                                     :4-1 (- (current-time-ms) (:1 new-data))))
               ;(println "global flow --" new-data)
               (cond (and (some? (:cord new-data))
                       (not= cord-x (:x (:cord new-data)))
                       (not= cord-y (:y (:cord new-data)))) (let [[cx cy]  (:cord new-data)
                                                                  time     (:time new-data)
                                                                  ctm      (.getScreenCTM dom/node)
                                                                  dx       (/ (- cx (.-e ctm))
                                                                              (.-a ctm))
                                                                  dy       (/ (- cy (.-f ctm))
                                                                              (.-d ctm))
                                                                  cur-x (:pos @!xx)
                                                                  cur-y (:pos @!yy)
                                                                  x (+ cur-x (- dx @!fx))
                                                                  y (+ cur-y (- dy @!fy))
                                                                  new-x {:pos x :time time}
                                                                  new-y {:pos y :time time}]
                                                              ;(println "4. Client side update: " new-x "::" new-y "::" new-data)
                                                              (reset! !node-pos-atom {:x new-x
                                                                                      :y new-y
                                                                                      :id id})
                                                              (println " --XX-- " new-x "::" @!xx "::::" new-data "::" cur-x @!fx dx)
                                                              (reset! cord-x cx)
                                                              (reset! cord-y cy)
                                                              ;(println " -------YY----- UPDATING ----------------" new-y "::" @!yy "::::" new-data)
                                                              (reset! !xx new-x)
                                                              (reset! !yy new-y))
                     ;; Only happen for server based updates
                     (some? (:x new-data))   (let [ct (:time @!xx)
                                                   nt (-> new-data :x :time)
                                                   nx (-> new-data :x :pos)
                                                   new-x {:pos nx :time nt}]
                                               ;(println "9.1 Data from server for X" new-data "::" @!xx)
                                               (when (> (- nt ct) 0)
                                                 (reset! !xx new-x)
                                                 (reset! !node-pos-atom {:x new-x
                                                                         :id id})))
                     (some? (:y new-data))   (let [ct (:time @!yy)
                                                   nt  (-> new-data :y :time)
                                                   ny (-> new-data :y :pos)
                                                   new-y {:pos ny :time nt}]
                                               ;(println "9.2 Data from server for Y" new-data "::" @!yy)
                                               (when (> (- nt ct) 0)
                                                 (reset! !yy new-y)
                                                 (reset! !node-pos-atom {:y new-y
                                                                         :id id})))
                     :else                   (println "THIS IS SOME OTHER TYPE OF DATA: " new-data))))

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