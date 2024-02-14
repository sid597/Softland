(ns app.client.editor.events.utils
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]))


#?(:cljs (def !pos  (atom [100 100])))
(e/def pos (e/client (e/watch !pos)))

(e/defn new-line-pos [action]
  (e/client
     (cond
       (= (:enter action)) (reset! !pos [(first pos) (+ (second pos) 20)]))))
