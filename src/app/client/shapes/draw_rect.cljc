(ns app.client.shapes.draw-rect
  (:require contrib.str
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-ui4 :as ui]
            [clojure.pprint :as pprint]
            [app.client.style-components.svg-icons :refer [get-icon]]
            [app.client.mode :refer [theme]]
            #?@(:cljs [[clojure.string :as str]
                       [app.client.utils :refer [new-uuid]]
                       [global-flow :refer [!global-atom current-time-ms]]])
            #?@(:clj [[app.server.rama :refer [add-new-node get-event-id]]])))


(e/defn draw-rect []
  (e/client
    (dom/button
      (dom/props {:style {:display "flex"
                          :background-color "red"}})
      (dom/text "GM")
      (dom/on "click" (e/fn [e]
                        (e/client
                          (let [nid   (new-uuid)
                                time  (current-time-ms)]
                            (do
                             (println "BEF" @!global-atom)
                             (e/server (add-new-node
                                         {nid {:y {:pos nil :time time}
                                               :fill "lightblue",
                                               :type "prototype-rect",
                                               :id nid
                                               :x {:pos nil :time time}
                                               :type-specific-data {:width nil
                                                                    :height nil
                                                                    :text "GM Hello"}}}
                                         {:graph-name :main
                                          :event-id (get-event-id)
                                          :create-time time}))
                             (reset! !global-atom {:type :draw-rect
                                                   :nid nid})
                             (println "clicked seddings"@!global-atom)))))))))

