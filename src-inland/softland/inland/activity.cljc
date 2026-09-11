(ns softland.inland.activity
  "View-owned repeated total steps. Takes authored step name, state and budget.
   Gives visible status/result and yields between steps. Holds cancellable work."
  (:require [hyperfiddle.electric3 :as e]
            [missionary.core :as m]
            [softland.inland.execution :as x]
            [softland.inland.total :as total]
            #?(:cljs [softland.inland.session :as session])))

(defn yield-flow [_iteration ms]
  (m/relieve (fn [_ v] v)
    (m/reductions (fn [_ v] v) false (m/ap (m/? (m/sleep ms)) true))))

(e/defn Run [s owner workspace context request]
  (e/client
    (let [!progress (session/cell s (:output request))
          progress (e/watch !progress)
          cancelled (x/Local s (str "cancel/" (:id request)))]
      (session/report! s "activity" progress)
      (e/on-unmount #(session/report! s "activity-disposed" (:id request)))
      (when (and (= :running (:status progress)) (not cancelled))
        (e/for-by :iteration [current [progress]]
          (let [result (e/snapshot
                         (let [v (x/Call s owner workspace (or (:context request) context) (:step request) {:state (:state current)} 0)]
                           (e/When (total/ready? v) v)))
                delay (total/step-delay result (:yield-ms request 35))
                yielded (e/input (yield-flow [(:id request) (:iteration current)] delay))]
            (when yielded
              (session/advance! s request current result)))))
      (when (and cancelled (= :running (:status progress)))
        (reset! !progress (assoc progress :status :cancelled))))))
