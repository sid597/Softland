(ns global-flow
  (:require [missionary.core :as m]))

(defn await-promise
  "Returns a task completing with the result of given promise"
  [p]
  (let [v (m/dfv)]
    (.then p
      #(v (fn [] %))
      #(v (fn [] (throw %))))
    (m/absolve v)))
