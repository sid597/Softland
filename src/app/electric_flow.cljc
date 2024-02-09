(ns app.electric-flow
  (:import [hyperfiddle.electric Failure Pending])
  (:require contrib.str
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric-dom2 :as dom]
            [app.background :as bg]
            [app.flow-calc :as fc]
            [app.electric-codemirror :as cm]
            [app.env :as env :refer [oai-key]]
            #?@(:cljs
                [[clojure.string :as str]
                 [missionary.core :as m]
                 [app.mode :as th :refer [dark-mode light-mode theme]]]
                :clj
                [[missionary.core :as m]
                 [com.rpl.rama :as r]
                 [com.rpl.rama.path :as path :refer [subselect ALL FIRST keypath select]]
                 [app.rama :as rama :refer [!subscribe nodes-pstate get-event-id add-new-node]]
                 [wkok.openai-clojure.api :as api]
                 [clojure.core.async :as a :refer [<! >! go]]])))




#_(defn log [message & args]
    (js/console.log message args))

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

(e/def nodes (e/server (e/watch !nodes)))


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


#?(:clj
   (defn chat-complete [{:keys [messages render-uid]}]
     (println "render uid " render-uid)
     (let [events (api/create-chat-completion
                    {:model "gpt-3.5-turbo"
                     :messages messages
                     :stream true}
                    {:api-key oai-key})]

       (go
         (loop []
           (let [event (<! events)]
             (when (not= :done event)
               (let [res (-> event
                           :choices
                           first
                           :delta
                           :content)
                     cur-val (-> @!nodes
                                 render-uid
                                 :text)]
                ;(println "res" res cur-val)
                (swap!
                  !nodes
                  update-in
                  [render-uid :text]
                  (constantly
                    (str
                     cur-val
                     res))))

               (recur))))))))

(e/defn subscribe [path]
  (e/server (new (!subscribe (concat [(keypath :main)]
                               path)
                   nodes-pstate))))

(e/defn circle [[k {:keys [id x y r color dragging?]}]]
  (let [x-p          [ id :x]
        y-p          [ id :y]
        r-p          [ id :type-specific-data :r]
        color-p      [ id :type-specific-data :color]
        dragging?-p  [ id :type-specific-data :dragging?]]
    (e/client
      (svg/circle
        (dom/props {:id id
                    :cx (subscribe. x-p)
                    :cy (subscribe. y-p)
                    :r  (subscribe. r-p)
                    :fill (subscribe. color-p)})
        (dom/on  "mousemove" (e/fn [e]
                                 (.preventDefault e)
                                 (when dragging?
                                  (println "dragging element")
                                  (let [el      (.getElementById js/document (name id))
                                        [x y]   (fc/element-new-coordinates1 e el)]
                                    (e/server (swap! !nodes assoc-in [k :x]  x))
                                    (e/server (swap! !nodes assoc-in [k :y] y))))))
        (dom/on "mousedown"  (e/fn [e]
                               (.preventDefault e)
                               (.stopPropagation e)
                               (println "pointerdown element")
                               (e/server (swap! !nodes assoc-in [k :dragging?] true))))
        (dom/on "mouseup"    (e/fn [e]
                                 (.preventDefault e)
                                 (.stopPropagation e)
                                 (println "pointerup element")
                                 (e/server (swap! !nodes assoc-in [k :dragging?] false))))
        (dom/on "mouseleave"    (e/fn [e]
                                  (.preventDefault e)
                                  (.stopPropagation e)
                                  (println "mouseleave element")
                                  (e/server (swap! !nodes assoc-in [k :dragging?] false))))
        (dom/on "mouseout"    (e/fn [e]
                                (.preventDefault e)
                                (.stopPropagation e)
                                (println "mouseout element")
                                (e/server (swap! !nodes assoc-in [k :dragging?] false))))))))


(e/defn line [[k {:keys [id color to from]}]]
  (e/client
    (let [tw (subscribe. [ to :width])
          th (subscribe. [ to :height])
          fw (subscribe. [ from :width])
          fh (subscribe. [ from :height])
          tx (subscribe. [ to :x])
          ty (subscribe. [ to :y])
          fx (subscribe. [ from :x])
          fy (subscribe. [ from :y])]
      (svg/line
        (dom/props {:style {:z-index -1}
                    :id id
                    :x1  (if  tw
                           (+  tx) (/ tw 2)
                           tx)
                    :y1  (if th
                           (+ ty (/ th 2))
                           ty)
                    :x2  (if fw
                           (+ fx (/ fw 2))
                           fx)
                    :y2  (if fh
                           (+ fy (/ fh 2))
                           fy)
                    :stroke color
                    :stroke-width 4})))))


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

                   (dom/on "click" (e/fn [e]
                                     (let [child-uid (new-uuid)
                                           x         (e/server (rama/get-path-data x-p pstate))
                                           y         (e/server (rama/get-path-data y-p pstate))]
                                       (when (some? cm-text)
                                         (println "cm-text -->" cm-text)
                                         (create-new-child-node. id child-uid (+ x 600) y cm-text)))))))))))))))


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

#?(:cljs (def !context-menu? (atom nil)))
(e/def context-menu? (e/client (e/watch !context-menu?)))


(e/defn context-menu []
  (e/client
    (println "called context menu" (type (:x context-menu?)) (:y context-menu?))
    (let [x (:x context-menu?)
          y (:y context-menu?)]
      (println "context menu" x y)
      (dom/div
        (dom/props {:style {:position "absolute"
                            :z-index "1000"
                            :top  y
                            :left x
                            :background-color (:context-menu (theme. ui-mode))
                            :height "100px"
                            :padding "5px"
                            :width "100px"}
                    :id "context-menu"})
        (dom/div
          (dom/button
            (dom/props {:style {:background-color (:button-background (theme. ui-mode))
                                :color (:button-text (theme. ui-mode))
                                :border "none"
                                :border-width "5px"
                                :font-size "1em"
                                :height "40px"
                                :width "100%"}})
           (dom/text
             "New node")
           (dom/on "click" (e/fn [e]
                             (e/client
                              (println "gg clicked")
                              (let [id (new-uuid)
                                    [cx cy] (fc/browser-to-svg-coords e viewbox (.getElementById js/document "sv"))
                                    node-data {id {:id id
                                                   :x cx
                                                   :y cy
                                                   :type-specific-data {:text "GM Hello"
                                                                        :width 400
                                                                        :height 800}
                                                   :type "rect"
                                                   :fill  "lightblue"}}]
                                 (println "id" id)
                                 (println "node data" cx cy)
                                 (e/server
                                   (let [event-data {:graph-name  :main
                                                     :event-id    (get-event-id)
                                                     :create-time (System/currentTimeMillis)}]
                                     (add-new-node node-data
                                                   event-data
                                                   true
                                                   true)))

                               (reset! !context-menu? nil)))))))))))


(e/defn reset-global-vals []
  (e/client
    (cond
      (some? @!context-menu?) (reset! !context-menu? nil))))


(e/defn theme-toggle []
  (e/client
    (dom/button
      (dom/props
        {:top "100px"
         :left "1000px"
         :style {:background-color (:button-background (theme. ui-mode))
                 :color (:button-text (theme. ui-mode))
                 :border "none"
                 :margin "0px"
                 :padding "0px"
                 :font-size "10px"
                 :height "20px"
                 :width "100%"}})
      (dom/text "Theme")
      (dom/on "click" (e/fn [e]
                        (e/server
                         (reset! !ui-mode (if (= :dark @!ui-mode)
                                            :light
                                            :dark))))))))


(e/defn view []
  (let [current-selection (atom nil)]
    (e/client
      (theme-toggle.)
      (when (some? context-menu?)
        (println "context menu is " @!context-menu? (some? @!context-menu?))
        (context-menu.))
      (svg/svg
        (dom/props {:viewBox (clojure.string/join " " viewbox)
                    :id "sv"
                    :style {:min-width "100%"
                            :min-height "100%"
                            :z-index 1
                            :background-color (:svg-background (theme. ui-mode))
                            :top 0
                            :left 0}})
        (dom/on "contextmenu" (e/fn [e]
                                (e/client
                                  (println "contextmenu" @!context-menu?)
                                  (reset! !context-menu? {:x (str (.-clientX e) "px")
                                                           :y (str (.-clientY e) "px")})
                                  (.preventDefault e)
                                  (println "contextmenu" @!context-menu?))))

        (dom/on "mousemove" (e/fn [e]
                              (when (= 0
                                      (.-button e))
                                (when @!border-drag?
                                  (let [el (.getElementById js/document "sv")
                                        [x y] (fc/element-new-coordinates1 e el)]
                                    (println "border draging" x y)
                                    (e/server
                                     (swap! !edges assoc-in [:raw :x2] x)
                                     (swap! !edges assoc-in [:raw :y2] y))))

                                (cond
                                  (and is-dragging?
                                    (= "background"
                                      (:selection
                                        @current-selection))
                                    (:movable?
                                      @current-selection))    (let [svg-el (.getElementById js/document "sv")
                                                                    [nx ny] (fc/find-new-coordinates e last-position viewbox svg-el)
                                                                    ex      (.-clientX e)
                                                                    ey      (.-clientY e)]
                                                                (println "gg")
                                                                (swap! !viewbox assoc 0 nx)
                                                                (swap! !viewbox assoc 1 ny)
                                                                (reset! !last-position {:x ex
                                                                                        :y ey}))
                                  #_#_@!border-drag? (println "border draging")))))
        (dom/on "mousedown" (e/fn [e]
                                (.preventDefault e)
                                (when (= 0
                                        (.-button e))
                                  (let [ex (e/client (.-clientX e))
                                        ey (e/client (.-clientY e))]
                                    (println "pointerdown svg")
                                    (reset! current-selection {:selection (.-id (.-target e))
                                                               :movable? true})
                                    (reset! !last-position {:x ex
                                                            :y ey})
                                    (reset! !is-dragging? true)
                                    (reset-global-vals.)))))
        (dom/on "wheel" (e/fn [e]
                          (when (= (.-id (.-target e))
                                  "background")
                            (.preventDefault e)
                            (println "wheeled" viewbox (fc/browser-to-svg-coords e viewbox (.getElementById js/document "sv")))
                            (let [coords (fc/browser-to-svg-coords e viewbox (.getElementById js/document "sv"))
                                  wheel   (if (< (.-deltaY e) 0)
                                            1.01
                                            0.99)
                                  new-view-box  (fc/direct-calculation viewbox wheel coords)
                                  new-zoom-level (* zoom-level wheel)]
                                (reset! !zoom-level new-zoom-level)
                                (reset! !viewbox new-view-box)))))

        (dom/on "mouseup" (e/fn [e]
                               (.preventDefault e)
                               (when (= 0
                                       (.-button e))
                                 (println "pointerup svg")
                                 (reset! !is-dragging? false)
                                 (reset! !border-drag? false))
                              (when @!border-drag?
                                (println "border draging up >>>")
                                (reset! !border-drag? false))))

        (bg/dot-background. (:svg-dots (theme. ui-mode)) viewbox)

        (e/server
          (e/for-by identity [node-id (new (!subscribe (keypath :main) nodes-pstate))]
            (let [type (subscribe. [:main (first node-id) :type])]
             (e/client
               (println "type" type (= "circle" type))
               (rect. (first node-id))
               #_(cond
                       (= "circle" type)  (circle. node-id)
                       (= "rect" type)    (rect. node-id nodes-pstate)))))
          #_(e/for-by identity [edge edges]
              (let [[_ {:keys [type x2 y2]}] edge]
                (e/client
                  (println "edge type" type edge)
                  (cond
                    (and
                      (= "raw" type)
                      (not-every? nil? [x2 y2])) (new-line-el. edge)
                    (= "line" type)              (line. edge))))))))))



#_(e/defn tt []
    (e/client
      (dom/div
        (dom/text "Scroeboard:")
        (e/server
          (e/for-by identity [res (new (!subscribe [] nodes-pstate))]
            (let [type (new (!subscribe [ res :type] nodes-pstate))]
             (e/client
              (println "res" type)
              (dom/div
                (dom/text "===================")
                (dom/text (str "hloo" type))
                (dom/text "===================")))))))))




(e/defn main [ring-request]
  (e/client
    (binding [dom/node js/document.body]
      (view.))))



