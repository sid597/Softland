(ns app.client.utils
  (:require [hyperfiddle.electric :as e]
            #?@(:clj
                 [
                  [app.server.rama :as rama :refer [!subscribe nodes-pstate get-event-id add-new-node]]
                  [com.rpl.rama.path :as path :refer [subselect ALL FIRST keypath select]]]
                :cljs [[cljs.reader :as reader]])))


(defn new-uuid []
  (keyword (str (random-uuid))))
#_(defn log [message & args]
    (js/console.log message args))

#?(:clj (defn read-local-file [file-path]
          (slurp file-path)))

#?(:clj (def get-dg-nodes-from-local
         (let
           [file-path "/Users/sid597/Downloads/dg11.edn"
            file-content (read-local-file file-path)]
           (clojure.edn/read-string (first (clojure.edn/read-string file-content))))))

#?(:clj (def unique-dg-nodes
          (let [unique (atom #{})]
            (doseq [node get-dg-nodes-from-local]
              (let [src (first node)
                    des (last node)]
                (when (not (contains? @unique (:title des)))
                  (swap! unique conj src))
                (when (not (contains? @unique (:title src)))
                  (swap! unique conj des))))
            (vec @unique))))


(e/defn add-dg-new-nodes []
  (e/client
    (let [!x (atom 3200.01)
          !y (atom 600.01)
          idx (atom 1)]
      (e/for-by
        identity
        [node (e/server unique-dg-nodes)]
        (e/client
          (do
           (println "idx--" @idx "node --" node "--x" @!x "--y " @!y)
           (let [id        (new-uuid)
                 _ (println "1. ==>" @!x @!y (mod @idx 6) @idx)
                 node-data {id {:id id
                                :x @!x
                                :y @!y
                                :type-specific-data {:text  (:title node)
                                                     :uid (:uid node)
                                                     :width 400
                                                     :height 800}
                                :type "rect"
                                :fill  "lightblue"}}]
             (do
               (if (= 0 (mod @idx 6))
                 (do
                  (reset! !x 3200.01)
                  (swap! !y + 450)
                  (println "==>" @!x @!y (mod @idx 6) @idx))
                 (do
                  (swap! !x + 450)
                  (println "==>" @!x @!y (mod @idx 6) @idx)))
               (swap! idx + 1)
               (println "-->" node-data)

               (e/server
                 (let [event-data {:graph-name  :main
                                   :event-id    (get-event-id)
                                   :create-time (System/currentTimeMillis)}]
                   (add-new-node node-data
                     event-data
                     true
                     true)))))))))))


(= 0 (mod 2 5))

#?(:clj (def !ui-mode (atom :dark)))
(e/def ui-mode (e/server (e/watch !ui-mode)))





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

(e/def nodes (e/server (e/watch {})))


#?(:cljs (def !new-line (atom {:start nil
                               :end   nil})))
#?(:cljs (def !border-drag? (atom false)))
#?(:cljs (def !is-dragging? (atom false)))
#?(:cljs (def !zoom-level (atom 1.5)))
#?(:cljs (def !last-position (atom {:x 0 :y 0})))
#?(:cljs (def !viewbox (atom [0 0 2000 2000])))
#?(:cljs (def !added-new-nodes? (atom false)))

(e/def viewbox (e/client (e/watch !viewbox)))
(e/def last-position (e/client (e/watch !last-position)))
(e/def zoom-level (e/client (e/watch !zoom-level)))
(e/def is-dragging? (e/client (e/watch !is-dragging?)))
(e/def new-line (e/client (e/watch !new-line)))
(e/def added-new-nodes? (e/client (e/watch !added-new-nodes?)))



(e/defn subscribe [path]
  (e/server (new (!subscribe (concat [(keypath :main)]
                               path)
                   nodes-pstate))))




#?(:cljs (def !context-menu? (atom nil)))
(e/def context-menu? (e/client (e/watch !context-menu?)))



(e/defn reset-global-vals []
  (e/client
    (cond
      (some? @!context-menu?) (reset! !context-menu? nil))))

