(ns softland.inland.reactive
  "Async source to Electric boundary. Owns only the subscribed source's lifetime.
   An empty table denotes not-yet-delivered; a singleton carries its actual value."
  (:require [missionary.core :as m]))

(defn table
  "Flow → initialized continuous zero-or-one table for Electric input.
   Reductions supplies [] synchronously. No placeholder value is accepted state.
   Downstream cancellation cancels the original source, including pending setup."
  [flow]
  (m/relieve (fn [_ next] next)
    (m/reductions (fn [_ value] [value]) [] flow)))

(defn received
  "Internal acquisition packet → value or thrown source error."
  [{:keys [value error]}]
  (if error (throw error) value))

#?(:cljs
   (defn resource
     "Promise acquisition and disposer → owned flow. Cancellation during pending
      acquisition disposes the eventual resource; failures propagate as failures."
     [acquire! dispose!]
     (m/eduction
       (map received)
       (m/observe
         (fn [emit]
           (let [!closed (atom false) !resource (atom nil)]
             (-> (js/Promise.resolve nil)
                 (.then (fn [_] (acquire!)))
                 (.then (fn [resource]
                          (if @!closed (dispose! resource)
                            (do (reset! !resource resource) (emit {:value resource})))))
                 (.catch (fn [error] (when-not @!closed (emit {:error error})))))
             (fn []
               (reset! !closed true)
               (when-let [resource @!resource] (dispose! resource)))))))))
