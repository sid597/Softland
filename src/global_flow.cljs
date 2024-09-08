(ns global-flow
  (:import (missionary Cancelled))
  (:require [missionary.core :as m]
            [hyperfiddle.electric-dom3 :as dom]
            [contrib.missionary-contrib :as mx]))

(defn current-time-ms []
  (js/Date.now))

(defn debounce [dur >in]
  (m/ap
    (let [x (m/?< >in)]
      (try (m/? (m/sleep dur x))
           (catch Cancelled e
             (m/amb))))))

(def ctr (atom 0))

(def !global-atom (atom nil))

(defn global-client-flow []
  (m/signal ;; https://clojurians.slack.com/archives/C7Q9GSHFV/p1691599800774709?thread_ts=1691570620.457499&cid=C7Q9GSHFV
    (m/latest
      (fn [x]
        ;(println "X" x)
        x)
      (m/watch !global-atom))))

(def !node-pos-atom (atom nil))
(defn node-pos-flow []
  (m/signal ;; https://clojurians.slack.com/archives/C7Q9GSHFV/p1691599800774709?thread_ts=1691570620.457499&cid=C7Q9GSHFV
    (m/latest
      (fn [x]
        (println "NODE POS UPDATE" x)
        x)
      (m/watch !node-pos-atom))))

(def !all-nodes-map (atom []))
(def !quad-tree (atom nil))


(def !canvas (atom nil))
(def !squares (atom nil))
(def !adapter (atom nil))
(def !device (atom nil))
(def !context (atom nil))
(def !format (atom nil))
(def !command-encoder (atom nil))
(def !all-rects (atom nil))
(def !width (atom nil))
(def !height (atom nil))
(def !canvas-y (atom nil))
(def !canvas-x (atom nil))
(defonce !offset (atom nil))
(defonce !zoom-factor (atom nil))


(defn mouse-down?> [node]
  (->> (mx/mix (m/observe (fn [!] (dom/with-listener node "mousedown"
                                    (fn [e] (.preventDefault e) (! [(.-clientX e) (.-clientY e)])))))
         (m/observe (fn [!] (dom/with-listener node "mouseup" (fn [_] (! nil))))))
    (m/reductions {} nil)
    (m/relieve {})))


(defn await-promise
  "Returns a task completing with the result of given promise"
  [p]
  (let [v (m/dfv)]
    (.then p
      #(v (fn [] %))
      #(v (fn [] (throw %))))
    (m/absolve v)))

