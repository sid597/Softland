(ns app.client.editor.core
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [app.client.editor.events.keydown :refer [on-keydown]]
            [app.client.editor.parser :refer [parse-doc parse-text]]
            [app.client.editor.events.click :refer [blinker-cursor]]
            [app.client.utils :refer [viewbox ui-mode subscribe]]
            [app.client.editor.events.utils :refer [editor-text letter-width c-x c-y pos]]
            [clojure.string :as string]
            #?@(:cljs [[app.client.editor.events.utils :refer [!pos initialise-canvas]]
                       [app.client.editor.events.click :refer [on-click]]])))


#?(:cljs (defn oc []
           (println "clicked the canvas.")))

(defn nested-bold [text]
  (let [stack (atom [])
        ctr   (atom 0)
        cnt   (count text)
        ns     (atom "")
        bold-map (atom [])]
    (while (< @ctr (- cnt 2))
      (let [sb            (subs text @ctr (+ 2 @ctr))
            n             (= "**" sb)
            add?          (and n (not= \space (nth text (+ 2 @ctr))))
            remove?       (boolean
                            (and
                              (when (> @ctr 1) (not= \space (nth text (- @ctr 1))))
                              (not-empty @stack)
                              n))
            normal?  (and (empty? @stack)
                       (not add?) (not remove?))]
        (when (and (not= "" @ns)
                (or add? remove?))
          (do
            (swap! bold-map conj {:type :text
                                  :content @ns})
            (reset! ns "")))
        (cond
          remove? (do (when (not-empty @stack)
                        (let [l (last @stack)]
                          (swap! stack pop)
                          (when (empty? @stack)
                            (swap! bold-map conj {:type :bold
                                                  :content (subs text l (+ 2 @ctr))}))))
                      (swap! ctr + 2))
          add?    (do (swap! stack conj @ctr)
                      (swap! ctr + 2))
          normal? (do (swap! ns str (nth text @ctr))
                      (swap! ctr + 1))
          :else   (swap! ctr + 1))))

    (if (not-empty @stack)
      (swap! bold-map conj {:type :text
                            :content (subs text (last @stack) cnt)})
      (swap!
        bold-map
        (fn [x]
          (let [lc (subs text @ctr cnt)
                cnt (if (not= lc "**")
                      (str @ns lc)
                      @ns)]
            (println  "lc" lc)
            (println "ns" @ns)
            (println "cnt" (not-empty cnt))
            (if (not-empty cnt)
              (conj x {:type :text
                       :content cnt})
              x)))))

    @bold-map))


(e/defn canvas [id]
  (e/client
    (let [id  (str "canvas-"id)
          dpr (or (.-devicePixelRatio js/window) 1)]
      (println "pos" pos "dpr" dpr)

      (dom/div
        (dom/props
          {:style {:overflow "scroll"}})
        (dom/canvas
          (dom/props {:id id
                      :tabIndex 0
                      :height 800
                      :width 800
                      :style {:border "1px solid red"
                              :margin "30px"
                              :background-color "beige"}})
          (let [el           (.getElementById js/document id)
                ctx          (.getContext el "2d")
                rect         (.getBoundingClientRect el)
                !text        (atom "hello")
                text         (e/client (e/watch !text))
                !render-text (atom (nested-bold (string/join "" text)))
                render-text  (e/client (e/watch !render-text))
                x            (atom 0)
                y            (atom 20)]
            (initialise-canvas el rect dpr ctx)
            #_(parse-doc.)
            #_(blinker-cursor.)
            #_(parse-text.)
            (e/for-by (fn [x]
                        (let [ky (str (first x) "--" (second x))]
                         (println "for-by key: "  ky)
                         ky))
              [t (map-indexed vector text)]
              (let [c (second t)]
               (println "some change: " c)
               (println "c" c @x (letter-width c))
               (.fillText ctx c @x @y)
               (reset! x (+ @x (letter-width c))))
              #_(println "-----------t---------" t "--" "--")
              #_(println "-----------render-text---------" @!render-text "--" t))
            #_(e/for-by identity [node render-text]
                (println "node _-->" node)
                (.clearRect ctx 0 0 800 #_(letter-width (last @!text)) 200)
                (reset! x 0)
                (reset! y 0)
                (e/for-by identity [c (reverse (:content node))]
                 (println "fby" c)
                 (println "c" c @x (letter-width c))
                 (.fillText ctx c @x @y)
                 (reset! x (+ @x (letter-width c)))))
            (dom/on! "click" (fn [e]
                               (println "4. clicked canvas")
                               #_(on-click. e)))
            (dom/on "keydown" (e/fn [e]
                                (let [key                (.-key e)
                                      code               (.-code e)]
                                  (println "code" code "key" key)
                                  (cond
                                    (= "Space" code)           (swap! !text str " ")
                                    (= "Enter" code)           (swap! !text str "\n\n")
                                    (= "Backspace" code)       (do
                                                                 (println "backspace" @!text @x)
                                                                 (reset! x (- @x (letter-width (last @!text))))
                                                                 #_(.clearRect ctx 0 0 800 #_(letter-width (last @!text)) 200)
                                                                 (.clearRect ctx @x @y (letter-width (last @!text)) 20)
                                                                 (println "text before " @!text)
                                                                 (reset! !text (subs @!text 0 (dec (count @!text))))
                                                                 (println "text after " @!text))
                                    (some? (letter-width key)) (swap! !text str key #_(fn [s]
                                                                                        (str (subs s 0 1 ) "--" key (subs s 1 (count s)))))))))))))))

