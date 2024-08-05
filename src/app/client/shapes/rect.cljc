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
            [hyperfiddle.rcf :refer [tests tap with %]]
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
                 [missionary.core :as m]
                 [global-flow :refer [!node-pos-atom node-pos-flow !global-atom global-client-flow current-time-ms debounce]]]
                :clj
                [[com.rpl.rama.path :as path :refer [subselect ALL FIRST keypath select]]
                 [image-resizer.resize :refer :all]
                 [image-resizer.core :refer :all]
                 [image-resizer.format :as format]
                 [image-resizer.scale-methods :refer :all]
                 [clojure.java.io :refer :all]
                 [app.client.utils :refer [!ui-mode !edges !nodes]]
                 [app.server.rama :as rama :refer [!subscribe nodes-pstate get-path-data get-event-id
                                                   send-llm-request
                                                   add-new-node update-node]]])))


#_(tests
    "GM"
    #?(:clj (doseq [x (range 2251 2327)]
                 (println  (str "/Users/sid597/Softland/resources/public/img/resized/" x ".jpeg"))
                 (format/as-file
                   ((resize-fn 2048 2048 ultra-quality)
                    (file (str "/Users/sid597/Softland/resources/public/img/DSCF" x ".JPG")))
                   (str "/Users/sid597/Softland/resources/public/img/resized/" x ".jpeg")
                   :verbatim))))





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
   (defn el-mouse-move-state> [movable id dragging?]
     (m/observe
       (fn [!]
         (let [sample (fn [e]
                        (when dragging?
                         (! (do
                              (.preventDefault e)
                             {:nid   id
                              :type  :node-update
                              :cords [(.-clientX e)
                                      (.-clientY e)]}))))]


           (.addEventListener movable "mousemove" sample #js {"passive" false})
           #(.removeEventListener movable "mousemove" sample))))))


#?(:cljs (defn el-mouse-move-state< [movable id dragging?]
           (->> (el-mouse-move-state> movable id dragging?)
             (e/throttle 10)
             (m/reductions {} {:cord [0 0]
                               :type :node-update
                               :time (current-time-ms)})
             (m/relieve {})
             (m/latest (fn [cords]
                         (reset! !global-atom cords)))
             (m/signal))))

#?(:cljs (defn server-update []
           (->> (node-pos-flow)
             (e/throttle 100)
             (m/reductions {} {:x 0 :y 0 :nid 0})
             (m/relieve {})
             (m/latest (fn [new-data]
                         (println "6. Send to SERVER " new-data)
                         new-data))
             (m/signal))))


(e/defn watch-server-update [path f]
  (e/client
    (println "SETUP WATCH : " path)
    (let [new-data (subscribe. path)]
      ;(println "8. WATCH SERVER UPDATE" new-data "-c-" @!counter)
      (println  "8. -- NEW DATA FROM SERVER --" new-data "--" path)
      (case f
        :x (reset! !global-atom {:nid (first path)
                                 :type :node-update
                                 :x new-data})
        :y (reset! !global-atom {:nid (first path)
                                 :type :node-update
                                 :y new-data})
        :w (reset! !global-atom {:nid (first path)
                                 :type :node-update
                                 :w new-data})
        :h (reset! !global-atom {:nid (first path)
                                 :type :node-update
                                 :h new-data})))))

(e/defn setup-actions [{:keys [!hh !ww node id dragging? !dragging? x-p y-p width-p height-p cord-x cord-y !xx !yy !fx !fy fx fy xx yy]}]
  (e/client
    (let [reset-after-drag (e/fn [msg]
                             (e/client
                               (when @!dragging?
                                 (do
                                   ;(println "RESET AFTER DRAG")
                                   (when (not= (e/server (first (get-path-data [(keypath :main) id :x] nodes-pstate)))
                                           @!xx))

                                   (reset! !dragging? false)))))]
      (new (el-mouse-move-state< node id dragging?))
      (let [{:keys [x y nid width height from]} (new (server-update))]
        (println  " 7. SERVER DATA UPDATE " from "::" nid "::"  x "::" y "::::" width "::" height)
        (when (and (= nid id)
                (= :client from))
          (cond (some? x)      (e/server
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
                                   true))
                (some? width)  (e/server
                                 (update-node
                                   [width-p width]
                                   {:graph-name  :main
                                    :event-id    (get-event-id)
                                    :create-time (System/currentTimeMillis)}
                                   false
                                   false)
                                 (update-node
                                   [height-p height]
                                   {:graph-name  :main
                                    :event-id    (get-event-id)
                                    :create-time (System/currentTimeMillis)}
                                   false
                                   true)))))
      (let [{:keys [nid x-min time  x-max y-min y-max cords x y width height type px py w h]} (new (global-client-flow))
              [cx cy] cords
            xpos (:pos xx)
            ypos (:pos yy)
            rand-doub (fn [time mn mx]
                        (println "mn mx" time mn mx)
                        (+ mn (* (Math/abs (- mx mn)) (rand))))]
          (do
            (println id " :: " x-min x-max y-min y-max  "::" xpos ypos)
            (when (and time (= type :randomise) (> ypos y-min) (< ypos y-max) (> xpos x-min) (< xpos x-max))
              (doall
               (println "******************" id time" :: " x-min x-max y-min y-max  "::" xpos ypos)
               (let [rx {:pos (rand-doub time x-min x-max)
                         :time time}
                     ry {:pos (rand-doub time y-min y-max)
                         :time time}]
                 (println "RX" rx ry)
                 (reset! !xx rx)
                 (reset! !yy ry)
                 (e/server
                   (update-node
                     [x-p rx]
                     {:graph-name  :main
                      :event-id    (get-event-id)
                      :create-time (System/currentTimeMillis)}
                     false
                     false)
                   (update-node
                     [y-p ry]
                     {:graph-name  :main
                      :event-id    (get-event-id)
                      :create-time (System/currentTimeMillis)}
                     false
                     true))
                 #_(reset! !node-pos-atom  {:x rx
                                            :from :client
                                            :y ry
                                            :nid id}))))

            (when (and (= type :node-update)
                       (= id nid))
             ;(println ">=====>" nid width height xx yy)
              (cond

                (some? width) (let [t (current-time-ms)
                                    h {:time t :pos (Math/abs (- height (:pos yy)))}
                                    w {:time t :pos  (Math/abs (- width (:pos xx)))}]
                                (do ;(println "WWW" (- (:pos yy) height) (:pos yy) height  "--" (- (:pos xx) width) (:pos xx) width)

                                    (reset! !hh h)
                                    (reset! !ww w)
                                    (reset! !node-pos-atom  {:width w
                                                             :from :client
                                                             :loss false
                                                             :height h
                                                             :nid nid})))
                (some? px) (let [uy {:time (current-time-ms) :pos py}
                                 ux {:time (current-time-ms) :pos px}]
                             (do (println "XXX" px py)
                                 (reset! !yy uy)
                                 (reset! !xx ux)
                                 (reset! !node-pos-atom  {:x ux
                                                          :from :client
                                                          :y uy :nid nid})))
                (and cx cy
                 (not= @cord-x cx)
                 (not= @cord-y cy)) (let [ctm      (.getScreenCTM node)
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
                                                              :from :client
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
                                                            :from :server
                                                            :nid id})))

                    (some? y)   (let [ct (:time @!yy)
                                      nt  (-> y :time)
                                      ny (-> y :pos)
                                      new-y {:pos ny :time nt}]
                                  (when (> (- nt ct) 0)
                                    (println "server " cx cy ny)
                                    (reset! !yy new-y)
                                    (reset! !node-pos-atom {:y new-y
                                                            :from :server
                                                            :nid id})))
                    (some? h)   (let [ct (:time @!hh)
                                      nt  (-> h :time)
                                      nh (-> h :pos)
                                      new-h {:pos nh :time nt}]
                                  (when (> (- nt ct) 0)
                                    (println "server h" nh)
                                    (reset! !hh new-h)
                                    (reset! !node-pos-atom {:h new-h
                                                            :from :server
                                                            :nid id})))
                    (some? w)   (let [ct (:time @!ww)
                                      nt  (-> w :time)
                                      nw (-> w :pos)
                                      new-w {:pos nw :time nt}]
                                  (when (> (- nt ct) 0)
                                    (println "server w " nw)
                                    (reset! !ww new-w)
                                    (reset! !node-pos-atom {:h new-w
                                                            :from :server
                                                            :nid id})))

                    :else         (println "THIS IS SOME OTHER TYPE OF DATA: " cx cy x y nid)))))

      (dom/on node "mousedown"  (e/fn [e]
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

     (dom/on node "mouseup"    (e/fn [e]
                                 (.preventDefault e)
                                 (.stopPropagation e)
                                 (reset-after-drag. "mouseup on element")))

     (dom/on node "mouseleave"    (e/fn [e]
                                    (.preventDefault e)
                                    (.stopPropagation e)
                                    (reset-after-drag. "mouseleave on element")))
     (dom/on node "mouseout"    (e/fn [e]
                                  (.preventDefault e)
                                  (.stopPropagation e)
                                  (reset-after-drag. "mouseout element"))))))


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
          !hh (atom {:pos (if (some? (-> extra-data :height :pos)) (-> extra-data :height :pos)(-> extra-data :height))
                     :time (if (some? (-> extra-data :height :time)) (-> extra-data :height :time)(current-time-ms))})
          hh  (e/watch !hh)
          !ww (atom {:pos (if (some? (-> extra-data :width :pos)) (-> extra-data :width :pos) (-> extra-data :width))
                     :time (if (some? (-> extra-data :width :time)) (-> extra-data :width :time) (current-time-ms))})
          ww  (e/watch !ww)
          block-text (subscribe. text-p) #_(atom (:text extra-data))
          ;block-text (e/watch !text)
          !fx (atom nil)
          fx (e/watch !fx)
          !fy (atom nil)
          fy (e/watch !fy)]
      (watch-server-update. x-p :x)
      (watch-server-update. y-p :y)
      (watch-server-update. width-p :w)
      (watch-server-update. height-p :h)
      (svg/g
        (let [x (:pos xx)
              y (:pos yy)
              h (:pos hh)
              w (:pos ww)
              !rotation (atom 0)
              rotation (e/watch !rotation)]
          (println "00 ------ 00 ----- " x y h w)

          (setup-actions. {:node dom/node
                           :width-p width-p
                           :height-p height-p
                           :!hh !hh
                           :!ww !ww
                           :id id
                           :!dragging? !dragging?
                           :dragging? dragging?
                           :x-p x-p
                           :y-p y-p
                           :cord-x cord-x
                           :cord-y cord-y
                           :!xx !xx
                           :!yy !yy
                           :!fx !fx
                           :!fy !fy
                           :fx fx
                           :fy fy
                           :xx xx
                           :yy yy})

          (svg/rect
            (dom/props {:x      x
                        :y      y
                        :height h
                        :width  w
                        :fill   "red"
                        :id     id
                        :style {:display "flex"
                                :flex-direction "column"
                                :border "1px solid black"
                                :border-radius "10px"
                                :background-color "red"
                                :overflow "scroll"}}))
          ;; BELOW IS CUSTOM HAVE TO MOVE OUT
         #_(when (= :img type)
              (let [!w      (atom @!ww)
                    w       (e/watch !w)
                    !ratio  (atom nil)
                    ratio   (e/watch !ratio)
                    !h      (atom nil)
                    h       (e/watch !h)
                    img-src (-> extra-data :path)
                    ima     (js/Image.)]
                (do
                  (set! (.-onload ima) #(do
                                          (let [wid (.-width ima)
                                                hig (.-height ima)]
                                            (reset! !ratio (/ hig wid))
                                            (reset! !h (* @!ww @!ratio))
                                            (js/console.log "******" ima "-" @!ww "-" @!ratio "-" @!h))))
                  (set! (.-src ima) img-src)
                  (println " --" w h ratio ww hh (* hh ratio) (/ ww w) (* ww ratio (/ hh w))))
                (svg/image
                  (dom/props
                    {:id (str id "-image")
                     :x x
                     :y y
                     :width w
                     :height h
                     :href img-src
                     :preserveAspectRatio "xMaxYMax meet"}))
               (let [bx  (+ 10 x)
                      by (+ w y 10)
                     tx  (+ 10 bx)
                     ty  (+ by 15)]
                 (svg/rect
                  (dom/props {:x bx
                              :y  by
                              :height 20
                              :width 80
                              :fill "green"}))

                (svg/text
                  (dom/props
                    {:x tx
                     :y ty})
                  (dom/on "click" (e/fn [e]
                                    (.preventDefault e)
                                    (println "Button clicked")
                                    (e/server
                                      (println "SENDING LLM REQUEST CALL")
                                      (send-llm-request
                                        [text-p]
                                        {:graph-name :main
                                         :event-id (get-event-id)
                                         :create-time (System/currentTimeMillis)
                                         :request-data {:url "https://api.openai.com/v1/chat/completions"
                                                        :model "gpt-4o-mini"
                                                        :messages [{:role "user"
                                                                    :content "Heya! GM"}]
                                                        :temperature 0.1
                                                        :max-tokens 200}}))))
                  (dom/text "Extract"))
                (svg/text
                  (dom/props
                    {:x tx
                     :y (+ 20 ty)})
                  (dom/text block-text))))))))))






#_(tests
    "hello"
   (println "new add") := ""
   (let [y (atom 0.1111111122)
         x (atom 0.1111111122)
         st (atom 2251)
         res (atom [])]
      (for [i (range 76)]
        (let [nx (+ @x (* i 2))
              ny (+ @y (* i 2))
              id (keyword (str (random-uuid)))]
         (println (pr-str {:function-name "add-new-node",
                           :args [{id
                                   {:y {:time 100000 :pos ny},
                                    :fill "lightblue",
                                    :type "img",
                                    :id id
                                    :x {:time 100000 :pos nx},
                                    :type-specific-data {:path (str "/img/resized/" @st ".jpeg")
                                                         :width 400,
                                                         :height 400,
                                                         :text "GM Hello"}}}
                                  {:graph-name :main}]}))
         (swap! st inc)
         (swap! y + 210)
         (swap! x + 210)))))
