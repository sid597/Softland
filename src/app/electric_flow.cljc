(ns app.electric-flow
  (:import [hyperfiddle.electric Failure Pending])
  (:require [contrib.str :refer [pprint-str]]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.shapes.rect :refer [rect]]
            [app.client.shapes.draw-rect :refer [draw-rect]]
            [app.client.quad-tree :refer [total-forces]]
            [app.client.shapes.line :refer [line]]
            #?@(:cljs
                [[clojure.string :as str]
                 [app.client.shapes.line :refer [edge-update]]
                 [global-flow :refer [!all-nodes-map debounce !quad-tree global-client-flow !node-pos-atom node-pos-flow !global-atom current-time-ms]]
                 [app.client.shapes.rect :refer [server-update]]
                 [app.client.quad-tree :refer [build-quad-tree approximate-force total-forces]]
                 [app.client.flow-calc :refer [browser-to-svg-coords]]
                 [contrib.electric-contrib :refer-macros [after]]
                 [missionary.core :as m]]
                :clj
                [[missionary.core :as m]
                 [com.rpl.rama.path :as path :refer [subselect ALL FIRST keypath select]]
                 [app.server.rama.util-fns :refer[roam-query-request !subscribe get-path-data nodes-pstate update-node get-event-id node-ids-pstate]]])))
(hyperfiddle.rcf/enable!)


(e/defn event-to-map [e viewbox]
  (e/client
    {:clientX (.-clientX e)
     :clientY (.-clientY e)
     :x (.-x e)
     :y (.-y e)
     :pageX (.-pageX e)
     :pageY (.-pageY e)
     :offsetX (.-offsetX e)
     :offsetY (.-offsetY e)
     :viewbox viewbox}))

(e/defn theme [mode]
  (e/client
    (if (= :dark mode)
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
       :context-menu-text "#1B1A17"}
      {:svg-background "#f5f5f5"
       :svg-dots "black"
       :editor-background "lightblue"
       :editor-text "#75c9f3"
       :editor-border "#28A5FF72"
       :button-background "#1184FC33"
       :button-border "#977DFEA8"
       :button-text "#0D141F"
       :button-div "#FEBB0036"
       :edge-color "#71FF8F4B"
       :context-menu "#111a29"
       :context-menu-text "#75c8f2"})))


#?(:clj (def !edges (atom {:sv-line {:id :sv-line
                                     :type "line"
                                     :to   :83696284-4eb1-481e-bbb7-b8d555495b76
                                     :from :08fe4616-4a43-4b5c-9d77-87fc7dc462c5
                                     :color "red"}})))

(e/def edges (e/server (e/watch !edges)))

#?(:cljs
   (defn mouse-move-state> [movable]
     (m/observe
       (fn [!]
         (let [sample (fn [e] (! (do (.preventDefault e)
                                     [(.-clientX e)
                                      (.-clientY e)])))]

           (.addEventListener movable "mousemove" sample #js {"passive" false})
           #(.removeEventListener movable "mousemove" sample))))))


#?(:cljs (defn mouse-move-state< [movable]
           (->> (mouse-move-state> movable)
             (e/throttle 16)
             (m/reductions {} [0 0])
             (m/relieve {})
             (m/latest (fn [x]
                         x)))))


#?(:cljs (defn pinch-state> [pinchable]
           (m/observe
             (fn [!]
               (let [sample (fn [e] (! (when (= "background" (.-id (.-target e)))
                                         (do
                                          (.preventDefault e)
                                          (let [rect (.getBoundingClientRect (.getElementById js/document "svg-parent-div"))
                                                left (.-left rect)
                                                top (.-top rect)
                                                width (.-width rect)
                                                height (.-height rect)]
                                             {:delta (.-deltaY e)
                                              :clientX (.-clientX e)
                                              :clientY (.-clientY e)
                                              :left left
                                              :top top
                                              :width width
                                              :height height})))))]
                 (.addEventListener pinchable "wheel" sample #js {"passive" false})
                 #(.removeEventListener pinchable "wheel" sample))))))

#?(:cljs (defn latest-wheel [e !viewbox]
           ;(println "----" e)
           (when-let [{:keys [delta clientX clientY left top width height]} e]
             ;(println "WHEEL" e)
             (let [[vx vy vw vh]   @!viewbox
                   scale           (if (< delta 0)
                                     1.06
                                     0.95)
                   ratio-x         (/ vw width)
                   ratio-y         (/ vh height)
                   svg-x           (+ (* (- clientX left) ratio-x) vx)
                   svg-y           (+ (* (- clientY top) ratio-y) vy)
                   new-width       (* vw scale)
                   new-height      (* vh scale)
                   dx              (* (- vw new-width)
                                     (/ (- svg-x vx) vw))
                   dy              (* (- vh new-height)
                                     (/ (- svg-y vy ) vh))
                   new-box         [(+ vx dx)
                                    (+ vy dy)
                                    new-width
                                    new-height]]
               (do
                 ;(println "prev box" @!viewbox)
                 ;(println "new-box" new-box)
                 [new-box scale])))))


#?(:cljs (defn pinch-state< [pinchable !viewbox]
           (->> (pinch-state> pinchable)
             (e/throttle 1)
             (m/reductions {} {:delta 1
                               :clientX 0
                               :clientY 0
                               :left 0
                               :top 0
                               :width 1
                               :height 1})
             (m/relieve {})
             (m/latest (fn [e]
                         (latest-wheel e !viewbox))))))

(e/defn ccc [x y c r t]
  (e/client
   (svg/g
     (svg/circle
        (dom/props {:cx x
                    :cy y
                    :r r
                    :fill c}))
     (svg/text
       (dom/props {:x x
                   :y (- y (rand-int 15))  ;; Adjust y position to place text above the circle
                   :fill "white"
                   :font-family "Arial"
                   :font-size 1
                   :text-anchor "middle"})
       (dom/text (str t))))))

#?(:cljs
   (defn delayed [ms >in]
          (m/sp (let [v (m/?< >in)]
                  (m/? (m/sleep ms v))))))


#?(:cljs (defn start-seeding [nodes-list qt]
           (m/sp
             (cljs.pprint/pprint qt)
             (loop [nodes nodes-list]
               (if (seq nodes)
                 (let [node (first nodes)
                       id (:id node)
                       _   (println "***************** " id " ***************************")

                       initial-params {:theta 0.1 :min-distance 0.1 :force-scale 0.1}
                       {:keys [fx fy]} (approximate-force
                                         {:x (:x node)
                                          :y (:y node)
                                          :id (:id node)}
                                         qt
                                         0.5)
                                         ;initial-params)
                       new-x (+ fx (:pos (:x node)))
                       new-y (+ fy (:pos (:y node)))
                       time (current-time-ms)
                       res {:type :tick
                            :nid id
                            :time time
                            :new-node-pos {:new-x new-x
                                           :new-y new-y}}]
                    (println  " TOTAL FORCE ::" id "::" fx fy)
                    (m/? (m/sleep 10 (reset! !global-atom res)))
                    ;(println "DONE RESET" @!global-atom)
                    #_(swap! nodes-list update id assoc
                        :x {:pos new-x :time time}
                        :y {:pos new-y :time time})
                   (recur (rest nodes)))
                 nodes-list)))))

#?(:cljs (defn start-seeding0 [nodes-list]
             (m/sp
              (let [;nodes-list (atom all-nodes)
                    xl (mapv #(:pos (:x %)) nodes-list)
                    yl (mapv #(:pos (:x %)) nodes-list)
                    min-x (apply min xl)
                    max-x (apply max xl)
                    min-y (apply min yl)
                    max-y (apply max yl)]
                (println "SP" xl)
                (dotimes [n 1]
                  (println "+==========++++++++++===========+++++++++++==" min-x min-y max-x max-y)
                  (let [qt (build-quad-tree nodes-list
                             min-x
                             min-y
                             (+ 5 (- max-x min-x))
                             (+ 5 (- max-y min-y)))
                        _ (cljs.pprint/pprint nodes-list)
                       ; _ (cljs.pprint/pprint qt)
                        updated-nodes (m/? (start-seeding nodes-list qt))]
                    (println "Iteration" n "completed")))
                @nodes-list))))



(e/defn view []
  (e/client
    (pprint-str "IN THE VIEW")
    (let [!proto-node-id  (atom nil)
          proto-node-id  (e/watch !proto-node-id)
          !draw? (atom false)
          draw? (e/watch !draw?)
          !cpos (atom nil)
          !viewbox (atom [-2028 61 1331 331])
          viewbox (e/watch !viewbox)
          !ui-mode (atom :dark)
          ui-mode (e/watch !ui-mode)
          !is-dragging? (atom false)
          is-dragging? (e/watch !is-dragging?)
          !last-position (atom {:x 0 :y 0})
          last-position (e/watch !last-position)
          !zoom-level (atom 1.5)
          zoom-level (e/watch !zoom-level)
          !s-circles (atom [])
          s-circles (e/watch !s-circles)
          !circles (atom [])
          circles (e/watch !circles)
          !ctr (atom 0)
          ctr (e/watch !ctr)
          max-x (atom 0)
          min-x (atom 10000000)
          min-y (atom 10000000)
          max-y (atom 0)]

      #_(dom/on js/document "mousemove" (e/fn [e]
                                          (println "mouse move on document")
                                          (reset! !cpos (event-to-map. e viewbox))))
     (dom/div
       (dom/props {:style {:display "flex"
                           :height "100%"
                           :width "100%"
                           :overflow "hidden"
                           :justify-content "space-between"
                           :flex-direction "column"}})
       (dom/div
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
                              (reset! !ui-mode (if (= :dark @!ui-mode)
                                                 :light
                                                 :dark))))))
       (dom/div
         (dom/props {:id "action-buttons"
                     :style {:flex "row"}})
         (dom/div
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
             (dom/text "randomise")
             (dom/on "click" (e/fn [e]
                               (println "RANDOMISE CLICKED")
                               (let [ch (- (nth viewbox 3) 1400)
                                      cw (- (nth viewbox 2) 1400)
                                      cx (+ 200 (first viewbox))
                                      cy (+ 200 (second viewbox))]
                                   (reset!
                                     !global-atom
                                     {:type :randomise
                                      :time (current-time-ms)
                                      :x-min  cx
                                      :x-max (+ cw cx)
                                      :y-min  cy
                                      :y-max (+ ch cy)}))))))
         (dom/div
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
             (dom/text "zoom out")
             (dom/on "click" (e/fn [e]
                               (e/client
                                 (println "ZOOM OUT CLICKED" "MAX-MIN " @min-x @min-y @max-y @max-x)
                                 (swap! !viewbox assoc 0 0 #_(- @min-x 900))
                                 (swap! !viewbox assoc 1 0 #_(- @min-y 900))
                                 (swap! !viewbox assoc 2 500 #_(+ 1900 (- @max-x @min-x)))
                                 (swap! !viewbox assoc 3 500 #_(+ 1900 (- @max-y @min-y))))))))
         (dom/div
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
             (dom/text "Build quad tree")
             (dom/on "click" (e/fn [e]
                               (e/server
                                 (println "SENDING LLM REQUEST CALL")
                                 (roam-query-request
                                   []
                                   {:graph-name :main
                                    :event-id (get-event-id)
                                    :create-time (System/currentTimeMillis)}))
                               #_(e/client (reset! !quad-tree (build-quad-tree (into [] (vals @!all-nodes-map))
                                                                (- @min-x 900)
                                                                (- @min-x 900)
                                                                (+ (- @max-x @min-x) 1900)
                                                                (+ (- @max-y @min-y) 1900))))))))
         (dom/div
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
             (dom/text "RUN SIM")
             (dom/on "click" (e/fn [e]
                               (reset! !global-atom {:type :start-seeding :time (current-time-ms)
                                                     :all-nodes (e/server (first (get-path-data [(keypath :main)] nodes-pstate)))}))))))


       (dom/div
         (dom/props
           {:style {:background-color (:button-background (theme. ui-mode))
                    :color (:button-text (theme. ui-mode))
                    :border "none"
                    :margin "0px"
                    :display "flex"
                    :padding "0px"
                    :font-size "10px"
                    :width "100%"}})
         (dom/div
                 (dom/props {:style {:margin-left "50px"
                                     :padding "10px"}})
                 (dom/text (e/watch !cpos))))

       (draw-rect.)

       (let [{:keys [type action time nid type-specific-data ] :as inp} (new (global-client-flow))]
         (when (= type :draw-rect)
           (reset! !proto-node-id nid)
           (reset! !is-dragging? false))


        (when (and time (= type :start-seeding))
          (println "ALL " @!all-nodes-map)
          ((start-seeding0  @!all-nodes-map #_(:all-nodes inp))
           (fn [r] (println "SUCCESS SEEDING: " r))
           (fn [e] (println "ERROR SEEDING: " e)))))

       (dom/div
         (dom/props {:id "svg-parent-div"
                     :style {:display "flex"
                             :flex 1}})
         (reset! !viewbox [0 0 (.-clientWidth dom/node) (.-clientHeight dom/node)])
         (svg/svg
           (dom/props {:viewBox (clojure.string/join " " viewbox)
                       :id "sv"
                       :style {:z-index 1
                               :background-color (:svg-background (theme. ui-mode))
                               :top 0
                               :left 0}})
           (let [[clientX clientY ] (new (mouse-move-state< dom/node))
                 [vx vy vw vh] viewbox
                 svg (.getElementById js/document "svg-parent-div")
                 sg (.getElementById js/document "sv")
                 cw  (.-clientWidth svg)
                 ch  (.-clientHeight svg)
                 xf  (/ cw vw)
                 yf  (/ ch vh)
                 dx  (/ (- clientX (:x last-position))
                       xf)
                 dy  (/ (- clientY (:y last-position))
                       yf)
                 nx  (Math/round (- vx dx))
                 ny  (Math/round (- vy dy))]
             (when (and (some? proto-node-id) draw?)
                (let [bbox            (.getBoundingClientRect dom/node)
                      view-box-width  (nth viewbox 2)
                      view-box-height (nth viewbox 3)
                      ratio-x         (/ view-box-width (.-width bbox))
                      ratio-y         (/ view-box-height (.-height bbox))
                      svg-x           (+ (* (- clientX (.-left bbox)) ratio-x) (nth viewbox 0))
                      svg-y           (+ (* (- clientY (.-top bbox)) ratio-y) (nth viewbox 1))]
                  (println "==" proto-node-id)
                  (reset! !global-atom {:width svg-x :height svg-y :nid @!proto-node-id :type :node-update})))

             (when is-dragging?
               (do
                 (swap! !viewbox assoc 0  nx)
                 (swap! !viewbox assoc 1  ny)
                 (reset! !last-position {:x clientX
                                         :y clientY}))))

           (let [[new-box scale] (new (pinch-state< dom/node !viewbox))]
             (when (and (some? new-box)
                     (some? scale))
               (reset! !viewbox new-box)
               (reset! !zoom-level (* @!zoom-level scale))))

           (dom/on "mousedown" (e/fn [e]
                                 (when (= 0
                                         (.-button e))
                                   (.preventDefault e)
                                   (println "mousedown" proto-node-id)
                                   (let [ex (e/client (.-clientX e))
                                         ey (e/client (.-clientY e))]
                                     (do
                                       (reset! !last-position {:x ex
                                                               :y ey})
                                       (when (some? proto-node-id)
                                         (let [[nx ny] (browser-to-svg-coords e viewbox dom/node)]
                                           (println "DRAWING SO UPDATING" nx ny proto-node-id)
                                           (reset! !draw? true)
                                           (reset! !global-atom {:px nx :py ny :type :node-update :nid @!proto-node-id})))
                                      (when (nil? proto-node-id)
                                        (do
                                          (println "DRAGGING? " is-dragging?)
                                          (reset! !is-dragging? true))))))))
                                     ;(println "DOWN" is-dragging?)
           #_(when (some? tx))


           (dom/on "mouseup" (e/fn [e]
                               (.preventDefault e)
                               (when (= 0
                                       (.-button e))
                                 (println "pointerup svg " proto-node-id is-dragging?)
                                 (when (some? proto-node-id)
                                  (do
                                   (reset! !proto-node-id nil)
                                   (reset! !draw? false)))
                                 (when (and is-dragging?
                                         (nil? proto-node-id))
                                   (reset! !is-dragging? false))

                                 #_(swap! !is-dragging? not))))
           (svg/defs
              (svg/pattern
                (dom/props {:id "dotted-pattern"
                            :width 6
                            :height 6
                            :patternUnits "userSpaceOnUse"})
                (svg/circle
                  (dom/props {:cx 1
                              :cy 1
                              :r 0.5
                              :fill "#91919a"}))))
           (svg/rect
              (dom/props
                {:id "background"
                 :x (first viewbox)
                 :y (second viewbox)
                 :width (nth viewbox 2)
                 :height (nth viewbox 3)
                 :fill "url(#dotted-pattern)"}))


           (e/server
             (e/for-by identity [id  (new (!subscribe [:main ] node-ids-pstate))]
               (println "ID" id)
               (let [node-data (first (get-path-data [(keypath :main) id ] nodes-pstate))]
                 (e/client
                   (let [x (-> node-data :x :pos)
                         y (-> node-data :y :pos)
                         w (-> node-data :type-specific-data :width :pos)
                         h (-> node-data :type-specific-data :height :pos)
                         mx (+ x 3)
                         my (+ y 3)
                         type (-> node-data :type)]
                     (do
                       (println "---> NODE DATA <----" node-data)
                       (println "NODE " id mx my)
                       (swap! !all-nodes-map conj node-data)
                       (when (> @min-x x ) (reset! min-x x))
                       (when (< @max-x mx) (reset! max-x mx))
                       (when (> @min-y y) (reset! min-y y))
                       (when (< @max-y my) (reset! max-y my))
                       (cond
                         (= "img" type) (rect. id node-data :img)
                          :else (rect. id node-data :rect)))))))
             #_(e/for-by identity [edge edges]
                 (let [[k v] edge
                       target-node (first (get-path-data
                                            [(keypath :main) (:to v)]
                                            nodes-pstate))
                       source-node (first (get-path-data
                                            [(keypath :main) (:from v)]
                                            nodes-pstate))]
                   (e/client
                     (println "edge " v)
                     (println "target node" target-node)
                     (line. source-node target-node v)))))))))))




(e/defn main [ring-request]
  (e/client
    (binding [dom/node js/document.body]
      (view.))))