(ns global-flow
  (:import (missionary Cancelled))
  (:require [missionary.core :as m]))

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
