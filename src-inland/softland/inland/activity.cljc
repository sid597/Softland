(ns softland.inland.activity
  "Electric owner for repeated authored total steps.
   Takes a request with id, step reference, state, budget, output cell and optional
   pinned context; gives session progress and requested effects. Holds iteration
   snapshots, timers and their tracked reads; borrows the enclosing view/session.
   The recipe owns the algorithm, while this owner supplies repetition and yielding.
   This work ends with the view and is distinct from the durable external resident."
  (:require [hyperfiddle.electric3 :as e]
            [missionary.core :as m]
            [softland.inland.execution :as x]
            [softland.inland.total :as total]
            #?(:cljs [softland.inland.session :as session])))

(defn yield-flow
  "Iteration identity and milliseconds → continuous false, then true after sleep.
   The identity argument makes a new Electric input for each iteration although the
   flow body only uses ms. Cancellation ends the pending Missionary sleep."
  [_iteration ms]
  (m/relieve (fn [_ v] v)
    (m/reductions (fn [_ v] v) false (m/ap (m/? (m/sleep ms)) true))))

(e/defn Run
  "Session, scope and activity request → owned repeated calls and progress effects.
   Requires progress initialized by the initiating event. Each iteration snapshots
   one ready recipe result, waits its bounded delay, then advances under owner guards.
   Cancellation marks running progress cancelled; unmount cancels demanded reads and
   timers. The step receives state and explicitly reads other inputs itself."
  [s owner workspace context request]
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
