(ns softland.inland.session
  "A view owns local cells, input drafts and event delivery. Takes generic effects.
   Gives session facts and admission proposals. Holds no accepted-world mirror."
  (:require [clojure.string :as str]))

(defn workspace []
  (let [v (.get (js/URLSearchParams. (.-search js/location)) "workspace")]
    (if (and v (re-matches #"[A-Za-z0-9-]{1,64}" v)) v "workbench")))
(defn create [] {:id (str (random-uuid)) :cells (atom {}) :event (atom nil)
                :request (atom nil) :result (atom nil) :visible (atom true)
                :work (atom {}) :cancel (atom {})})
(defn cell [s k]
  (or (get @(:cells s) k)
      (get (swap! (:cells s) #(if (contains? % k) % (assoc % k (atom nil)))) k)))
(defn emit! [s event]
  (reset! (:event s) (assoc event :id (str (random-uuid)))))
(defn submit! [s op]
  (let [op (merge {:workspace (workspace) :request-id (str (random-uuid)) :layer "base"} op)]
    (reset! (:result s) nil) (reset! (:request s) op)
    (reset! (cell s "admission") {:status :pending :request-id (:request-id op)}) op))
(defn effects! [s effects]
  (doseq [effect effects]
    (case (:effect effect)
      :session (doseq [[key value] (:writes effect)] (reset! (cell s key) value))
      :admit (submit! s (:request effect))
      :event (emit! s (:event effect))
      :visibility (do (when-not (:value effect)
                        (doseq [[output _] @(:work s)]
                          (swap! (cell s output) #(if (= :running (:status %)) (assoc % :status :cancelled) %)))
                        (reset! (:work s) {}))
                      (reset! (:visible s) (:value effect)))
      :activity (let [request (:request effect)]
                  (swap! (:work s) assoc (:output request) request))
      :cancel (reset! (cell s (str "cancel/" (:owner effect))) true)
      nil)))
(defn deliver! [s event]
  (case (:kind event)
    :native-input ((:run event) (:point event))
    :aim (reset! (cell s "pointer") (:id event))
    (emit! s (-> event (assoc :subject (or (:subject event) (:id event) "world")) (dissoc :id)))))
(defn initialize! [s values]
  (doseq [[key value] values] (reset! (cell s key) value)))
(defn start-progress! [s request]
  (let [a (cell s (:output request))]
    (reset! a (if (and (integer? (:budget request)) (<= 1 (:budget request) 128))
                {:status :running :state (:state request) :remaining (:budget request) :iteration 0}
                {:status :failed :reason "Use an integer step budget from 1 to 128."})) a))
(defn report! [s key value]
  (let [root (or (.-__inland js/window) #js {})]
    (aset root key (clj->js value))
    (aset root "owner" (:id s))
    (set! (.-__inland js/window) root)))
(defn diagnostics! [s]
  (set! (.-__inlandCell js/window) (fn [key] (clj->js @(cell s key)))))
(defn received! [s result]
  (reset! (:result s) result) (reset! (cell s "admission") result))
