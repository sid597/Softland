(ns app.electric-flow
  (:require contrib.str
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric-dom2 :as dom]
            [applied-science.js-interop :as j]
            [app.data :as data]
            [app.background :as bg]
            [app.flow-calc :as fc]
            [app.electric-codemirror :as cm]
            [hyperfiddle.electric-ui4 :as ui]
            [app.env :as env :refer [oai-key]]
            #?@(:cljs
                [[clojure.string :as str]]

                :clj
                [[missionary.core :as m]
                 [wkok.openai-clojure.api :as api]
                 [com.rpl.rama :as r]
                 [com.rpl.rama.path :as rpath]
                 [rama-module :as rmod :refer [node-events-module]]
                 [com.rpl.rama.test :as rtest :refer [create-ipc launch-module!]]
                 [clojure.core.async :as a :refer [<! >! go]]])))

;; ====== RAMA ======

(e/def ipc
  (e/server
    (do
      (let [c (create-ipc)]
        (println "R: Start ipc, launch module")
        (launch-module! c node-events-module {:tasks 4 :threads 2})))))

;; Define clj defs

(e/def event-depot (e/server (r/foreign-depot ipc (r/get-module-name node-events-module) "*node-events-depot")))
(e/def nodes-pstate (e/server (r/foreign-pstate ipc (r/get-module-name node-events-module) "$$node-pstate")))



#_(e/defn subscribe []
      (->> (m/observe
             (fn [!]
               (let [proxy (e/server
                             (r/foreign-proxy rpath/ALL nodes-pstate
                               {:callback (fn [new-val diff old-val]
                                            (println "R: nodes-pstate callback" new-val diff old-val)
                                            (! new-val))
                                :pkey :rect}))]
                 #(.close @proxy))))
        ; discard stale values, DOM doesn't support backpressure
        (m/relieve {})))



;; ====== ELECTRIC ======
#_(defn log [message & args]
    (js/console.log message args))

#?(:clj (def !ui-mode (atom :light)))
(e/def ui-mode (e/server (e/watch !ui-mode)))


(def dark-mode
  {:svg-background "#111110"
   :svg-dots "#3B3A37"
   :editor-background "#111110" #_"#191919"
   :editor-text "#6F6D66"
   :editor-border "#2A2A28"
   :button-background "#222221"
   :button-border "#3C2E69"
   :button-text "#6F6D66"
   :button-div "#33255B"
   :edge-color "#71FF8F4B"
   :context-menu "#1B1A17"
   :context-menu-text "#1B1A17"})
(def light-mode
  {:svg-background "#f5f5f5"
   :svg-dots "#b9bdc4"
   :editor-background "lightblue"
   :editor-text "#75c9f3"
   :editor-border "#28A5FF72"
   :button-background "#1184FC33"
   :button-border "#977DFEA8"
   :button-text "#0D141F"
   :button-div "#FEBB0036"
   :edge-color "#71FF8F4B"
   :context-menu "#111a29"
   :context-menu-text "#75c8f2"})

(defn theme [mode]
  (case mode
    :dark dark-mode
    :light light-mode))


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


(e/defn circle [[k {:keys [id x y r color dragging?]}]]
    (svg/circle
      (dom/props {:id id
                  :cx x
                  :cy y
                  :r  r
                  :fill color})
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
                              (e/server (swap! !nodes assoc-in [k :dragging?] false))))))

(e/defn line [[k {:keys [id color to from]}]]
  (let [{tw :width
         th :height
         tx :x
         ty :y} (to (e/server nodes))
        {fh :height
         fw :width
         fx :x
         fy :y} (from (e/server nodes))]
    (svg/line
      (dom/props {:style {:z-index -1}
                  :id id
                  :x1  (if tw
                         (+ tx (/ tw 2))
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
                  :stroke-width 4}))))


(e/defn create-new-child-node [parent-id child-uid x y cm-text]
  (let [edge-id (new-uuid)
        rect-props {:id child-uid
                    :x x
                    :y y
                    :width 400
                    :height 800
                    :type "rect"
                    :text " "
                    :fill (:editor-background (theme ui-mode))}
        edge-props {:id   edge-id
                    :from parent-id
                    :to   child-uid
                    :type "line"
                    :color (:edge-color (theme ui-mode))}]
    (e/server
     (swap! !nodes assoc child-uid rect-props)
     (swap! !edges assoc edge-id edge-props)
     (swap! !nodes assoc-in [parent-id :text] cm-text)
     (chat-complete
        {:messages [{:role "user" :content cm-text}]
         :render-uid child-uid}))))


(e/defn rect [[_ {:keys [x y width height fill id text]}]]
  (let [!cm-text (atom nil)
        cm-text  (e/watch !cm-text)
        read (fn [edn-str]
               (println "Read string:" (edn/read-string edn-str))
               (reset! !cm-text (str edn-str))
               (try (edn/read-string edn-str)
                    (catch #?(:clj Throwable :cljs :default) t
                      #?(:clj (clojure.tools.logging/error t)
                         :cljs (js/console.warn t)) nil)))
        write (fn [edn] (with-out-str (pprint/pprint edn)))
        dom-id (str "dom-id-" (str id))]

   (svg/g
    (svg/rect
      (dom/props {:id  dom-id
                  :x x
                  :y y
                  :width width
                  :height height
                  :fill (:editor-border (theme ui-mode))})
      (dom/on "click" (e/fn [e]
                        (println "clicked the rect.")))


      (dom/on "mousedown" (e/fn [e]
                            (println "mousedown the rect.")
                            (let [el (.getElementById js/document (name dom-id))
                                  [x y] (fc/element-new-coordinates1 e  el)]
                              (e/server
                                (swap! !edges assoc :raw {:id :raw
                                                          :type "raw"
                                                          :x1 x
                                                          :y1 y
                                                          :x2 nil
                                                          :y2 nil
                                                          :stroke (:edge-color (theme ui-mode))
                                                          :stroke-width 4})))
                            (reset! !border-drag? true)))
      (dom/on "mouseup" (e/fn [e]
                          (println "mouseup the rect.")
                          (reset! !border-drag? false))))
    (svg/foreignObject
      (dom/props {:x (+  x 5)
                  :y (+  y 5)
                  :height (- height 10)
                  :width  (- width 10)
                  :fill "black"
                  :style {:display "flex"
                          :flex-direction "column"
                          :overflow "scroll"}})
      (dom/div
          (dom/props {:style {:background-color fill
                              :height "100%"
                              :display "flex"
                              :overflow "scroll"
                              :flex-direction "column"}})

          (dom/div
            (dom/props {:id (str "cm-" dom-id)
                        :style {:height "100%"
                                :overflow "scroll"
                                :width "100%"}})
            (new cm/CodeMirror {:parent dom/node} read identity text))
          (dom/div
           (dom/button
             (dom/props {:style {:background-color (:button-background (theme ui-mode))
                                 :border "none"
                                 :padding "5px"
                                 :border-width "5px"
                                 :font-size "18px"
                                 :color (:button-text (theme ui-mode))
                                 :height "50px"
                                 :width "100%"}})
             (dom/text
               "Save")

             (dom/on "click" (e/fn [e]
                               (let [child-uid (new-uuid)]
                                 (when (some? cm-text)
                                   (println "cm-text -->" cm-text)
                                   (create-new-child-node. id child-uid (+ x 600) y cm-text))))))))))))


(e/defn new-line-el [[k {:keys [id x1 y1 x2 y2 stroke stroke-width]}]]
  (println "dom props")
  (svg/line
    (dom/props {:id "draw"
                :x1  x1
                :y1  y1
                :x2  x2
                :y2  y2
                :stroke (:edge-color (theme ui-mode))
                :stroke-width 4})))

#?(:cljs (def !context-menu? (atom nil)))
(e/def context-menu? (e/client (e/watch !context-menu?)))


(e/defn context-menu []
  (e/client
    (println "called context menu" (:x context-menu?) (:y context-menu?))
    (let [x (:x context-menu?)
          y (:y context-menu?)]
      (println "context menu" x y)
      (dom/div
        (dom/props {:style {:position "absolute"
                            :z-index "1000"
                            :top  y
                            :left x
                            :background-color (:context-menu (theme ui-mode))
                            :height "100px"
                            :padding "5px"
                            :width "100px"}
                    :id "context-menu"})
        (dom/div
          (dom/button
            (dom/props {:style {:background-color (:button-background (theme ui-mode))
                                :color (:button-text (theme ui-mode))
                                :border "none"
                                :border-width "5px"
                                :font-size "1em"
                                :height "40px"
                                :width "100%"}})
           (dom/text
             "New node")
           (dom/on "click" (e/fn [e]
                             (println "gg clicked")
                             (let [id (new-uuid)
                                   [cx cy] (fc/browser-to-svg-coords e viewbox (.getElementById js/document "sv"))]
                                (println "id" id)
                                (e/server
                                  (swap! !nodes assoc id {:id id
                                                          :x cx
                                                          :y cy
                                                          :width 400
                                                          :height 800
                                                          :type "rect"
                                                          :text "gm"
                                                          :fill (:editor-background (theme ui-mode))}))
                              (reset! !context-menu? nil))))))))))


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
         :style {:background-color (:button-background (theme ui-mode))
                 :color (:button-text (theme ui-mode))
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
                            :background-color (:svg-background (theme ui-mode))
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
        (bg/dot-background. (:svg-dots (theme ui-mode)) viewbox)
        (e/server
          (e/for-by identity [node nodes]
            (let [[_ {:keys [type]}] node]
              (e/client
                (println "type" type (= "circle" type))
                (cond
                      (= "circle" type)  (circle. node)
                      (= "rect" type)    (rect. node)))))
          (e/for-by identity [edge edges]
            (let [[_ {:keys [type x2 y2]}] edge]
              (e/client
                (println "edge type" type edge)
                (cond
                  (and
                    (= "raw" type)
                    (not-every? nil? [x2 y2])) (new-line-el. edge)
                  (= "line" type)              (line. edge))))))))))


(e/defn main [ring-request]
  (e/client
    (binding [dom/node js/document.body]
      (view.))))

