(ns softland.inland.app
  "Generic Electric demand host. Takes the genesis context and material indexes.
   Gives authored views and event effects. Holds one view's lifetime and requests."
  (:require [hyperfiddle.electric3 :as e]
            [softland.inland.nodes :as n]
            [softland.inland.execution :as x]
            [softland.inland.paint :as paint]
            [softland.inland.activity :as activity]
            [softland.inland.total :as total]
            #?(:cljs [softland.inland.session :as session])
            #?(:cljs [softland.inland.render :as render])
            #?(:clj [softland.inland.store :as store])))

(e/defn Operations [s]
  (e/client
    (let [request (e/watch (:request s))]
      (e/for-by :request-id [op (if request [request] [])]
        (let [decision (e/server (e/Offload #(store/submit-result! op)))]
          (session/received! s decision))))))

(e/defn Events [s owner workspace context]
  (e/client
    (let [event (e/watch (:event s))]
      (e/for-by :id [event (if event [event] [])]
        (let [invocation (e/snapshot
                        (let [v (x/Dispatch s owner workspace context event)]
                          (e/When (total/ready? v) {:results v :context context})))
              results (:results invocation)
              supports (remove nil? (if (vector? results) results []))
              failure (or (total/first-failure results)
                          (some #(when-let [reason (total/effect-error (:value %))]
                                   {:runtime/status :failed :reason reason}) supports))
              effects (when-not failure (mapcat (fn [result]
                                (map (fn [effect]
                                       (if (= :admit (:effect effect))
                                         (assoc-in effect [:request :invocation]
                                           {:definition (:support result) :context (:context invocation) :event (:id event)}) effect))
                                     (:value result))) supports))]
          (session/report! s "last-event" {:event event :supports (mapv :support supports) :effects effects})
          (session/complete-event! s event
            (cond failure [{:effect :session :writes {"admission" {:status :failed :reason (:reason failure)}}}]
                  (seq supports) effects
                  :else [{:effect :admit :request {:kind :put :name (str "unanswered-" (:id event)) :expected-revision 0
                                                   :row {:name (str "unanswered-" (:id event)) :catalog "unanswered" :event event}}}])))))))

(e/defn Views [r s owner workspace context]
  (let [names (x/Index owner workspace context "demand/view")]
    (when (vector? names)
      (e/for-by identity [name names]
        (let [row (x/Resolve s owner workspace context name)
              bindings (when (and (not (total/blocked? row)) (= :view (get-in row [:pattern :demand])))
                         (x/Applicable s owner workspace context row {:subject "world" :context context}))]
          (when (and bindings (not (total/blocked? bindings)))
            (let [value (x/Recipe s owner workspace context (:body row) bindings 0)]
              (e/client (session/report! s name {:revision (:revision row) :value value}))
              (paint/Items r s owner workspace context name value))))))))

(e/defn LiveContext [r s owner workspace]
  (e/client
    (let [context (merge {:session owner :who "sid"} (or (x/Local s "context") {:layers ["base"]}))
          runs (vals (e/watch (:work s)))]
      (session/report! s "context" context)
      (Events s owner workspace context)
      (Views r s owner workspace context)
      (e/for-by :id [request runs]
        (activity/Run s owner workspace context request)))))

(e/defn Main []
  (e/client
    (let [owner (str (random-uuid)) workspace (session/workspace)
          ready (e/server (e/Offload #(store/ensure-workspace! workspace)))
          world (x/Read owner :rows [workspace "base/world"])]
      (when (and (= :accepted (:status ready)) (:session world))
        (let [s (session/create owner (e/snapshot (:session world)))
              visible (e/watch (:visible s))]
          (session/diagnostics! s)
          (Operations s)
          (if visible
            (let [r (n/Await (render/open #(session/deliver! s %)))]
              (LiveContext r s owner workspace)
              ;; The visible owner itself demands the surface, including while
              ;; every authored view is between completed resolution contexts.
              (some? r))
            (let [r (n/Await (render/open #(session/deliver! s %)))
                  context {:layers ["base"]}
                  value (x/Call s owner workspace context (:closed-view world) {} 0)]
              (Events s owner workspace context)
              (paint/Items r s owner workspace context "closed" value)
              (some? r))))))))
