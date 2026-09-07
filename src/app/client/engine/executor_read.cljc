(ns app.client.engine.executor-read
  "The executor's read barrier and answer routing.
   Takes a declared read, its complete transaction inputs and one optional
   answer. Gives a resolved value or a control result before state advances.
   Holds nothing between runs. Evidence: executor_pending_test.clj."
  (:require [app.client.engine.value-bytes :as vb]))

(def request-parts [:phase :recipe :consumed :item :state :at :step :snapshot])

(defn unresolved?
  "True when an argument contains an explicitly unresolved read, including
   inside a layer/vector. Resolved read snapshots are provenance, not inputs
   to the next operation's barrier."
  [value]
  (cond
    (and (map? value) (contains? value :status))
    (contains? #{:pending :needs-policy :unsupported} (:status value))
    (map? value) (boolean (some unresolved? (vals value)))
    (coll? value) (boolean (some unresolved? value))
    :else false))

(defn call
  "Run one capability, or consume the supplied answer at its exact phase,
   item and step. A request compares whole values; budget is never in it.
   Control results are thrown to the runner, which owns the prior state."
  [capability args ctx step opts transaction answer-used]
  (let [read? (boolean (:snapshot capability))
        request (when read?
                  (assoc (select-keys transaction request-parts)
                         :step (:out step) :snapshot ((:snapshot capability) args ctx)))
        answer (:answer opts)
        addressed? (and read? answer
                        (= (select-keys request [:phase :at :step])
                           (select-keys (:request answer) [:phase :at :step])))
        _ (when addressed?
            (when @answer-used
              (throw (ex-info "Answer already consumed" {:status :stale :reason :answer-consumed})))
            (when-let [part (first (filter #(not (vb/equal? (get request %) (get (:request answer) %))) request-parts))]
              (throw (ex-info "Answer does not belong to this request"
                              {:status :stale :reason part :request request})))
            (reset! answer-used true))
        value (if addressed? (:value answer) ((:run capability) args ctx))
        status (:status value)]
    (when (and read? (= (:stop-at opts) (:out step)))
      (throw (ex-info "Read demanded without committing"
                      {:status :answered :step (:out step) :request request :value value})))
    (cond
      (and read? (= :pending status) (= :provisional (:pending step)))
      (do
        (when-not (and (vector? (:partial value)) (= 4 (count (:partial value))))
          (throw (ex-info "A provisional read requires four partial channels"
                          {:error-type :executor/provisional-partial :read value})))
        (-> value (assoc :status :resolved :color (:partial value)
                        :contributors (:known value)
                        :provisional {:missing (:missing value)
                                      :interpretation "missing contributions read as transparent (a declared provisional read)"})))

      (and read? (contains? #{:pending :needs-policy} status))
      (throw (ex-info "Read suspended before next state"
                      {:status :suspended :reason status :request request :read value
                       :missing (if (= :needs-policy status) (:candidates value) (:missing value))}))

      (and read? (not= :resolved status))
      (throw (ex-info "Read did not resolve" {:error-type :executor/unresolved-read :read value :reason (:reason value)}))

      (unresolved? value)
      (throw (ex-info "Only a capability with :snapshot may suspend"
                      {:error-type :executor/unresolved-read :read value}))

      :else value)))
