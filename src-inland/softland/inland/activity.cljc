(ns softland.inland.activity
  "View-owned repeated total steps. Takes authored step name, state and budget.
   Gives visible status/result and yields between steps. Holds cancellable work."
  (:require [hyperfiddle.electric3 :as e]
            [missionary.core :as m]
            [softland.inland.execution :as x]
            [softland.inland.total :as total]
            #?(:cljs [softland.inland.session :as session])))

(defn yield-flow [ms] (m/ap (m/? (m/sleep ms)) true))

(e/defn Run [s owner workspace context request]
  (e/client
    (let [!progress (session/start-progress! s request)
          progress (e/watch !progress)
          cancelled (x/Local s (str "cancel/" (:id request)))]
      (session/report! s "activity" progress)
      (e/on-unmount #(session/report! s "activity-disposed" (:id request)))
      (when (and (= :running (:status progress)) (not cancelled))
        (e/for-by identity [iteration [(:iteration progress)]]
          (let [result (e/snapshot
                         (let [v (x/Call s owner workspace (or (:context request) context) (:step request) {:state (:state progress)} 0)]
                           (e/When (total/ready? v) v)))
                yielded (e/input (yield-flow (:yield-ms request 35)))]
            (when yielded
              (reset! !progress (assoc (total/step-result result (:state progress) (:remaining progress))
                                      :iteration (inc iteration))))))
      (when (and cancelled (= :running (:status progress)))
        (reset! !progress (assoc progress :status :cancelled))))))
)
