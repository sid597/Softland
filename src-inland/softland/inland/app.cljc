(ns softland.inland.app
  "Electric composition root for the authored workbench.
   Takes a workspace, accepted genesis record and scoped material indexes; gives
   rendered views, one-shot event effects and admission results. Holds a browser
   session and separate visible/reopen render owners; borrows Rama through execution.
   Events snapshot their accepted basis once. Maintained views keep live dependencies.
   The bootstrap world address and unanswered-event fallback are compiled choices;
   this host does not make every part of itself editable as ordinary material."
  (:require [hyperfiddle.electric3 :as e]
            [softland.inland.nodes :as n]
            [softland.inland.execution :as x]
            [softland.inland.paint :as paint]
            [softland.inland.activity :as activity]
            [softland.inland.total :as total]
            #?(:cljs [softland.inland.session :as session])
            #?(:cljs [softland.inland.render :as render])
            #?(:clj [softland.inland.store :as store])))

(e/defn Operations
  "Session request slot → latest durable decision, displayed in that session.
   Each request id owns a server offload; replacing the slot cancels its demand.
   The session supplies one slot, not an admission queue. A cancelled observation
   is not proof that Rama did not accept the already submitted operation."
  [s]
  (e/client
    (let [request (e/watch (:request s))]
      (e/for-by :request-id [op (if request [request] [])]
        (let [decision (e/server (e/Offload #(store/submit-result! op)))]
          (session/received! s decision))))))

(e/defn Events
  "Event, owner and resolution context → one application of authored effects.
   Waits for dispatch to finish its relevant reads, snapshots results plus context,
   then validates effects and attaches definition/revision provenance to admissions.
   Failures update local admission status; no matching rule stores an unanswered
   record. Consuming the event prevents completed commands replaying on rule edits."
  [s owner workspace context]
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

(e/defn Views
  "Visible owner and context → keyed authored view occurrences.
   The demand/view index selects candidates; matching patterns bind world/context
   before each recipe runs. Paint owns target resources beneath each named view.
   Incomplete indexes or patterns produce no occurrence until they become usable."
  [r s owner workspace context]
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

(e/defn LiveContext
  "Retained surface and session → context-dependent events, views and step owners.
   Session context changes replace affected resolution work without replacing the
   surface. Repeated work is keyed by request id and cancelled when its owner leaves."
  [r s owner workspace]
  (e/client
    (let [context (merge {:session owner :who "sid"} (or (x/Local s "context") {:layers ["base"]}))
          runs (vals (e/watch (:work s)))]
      (session/report! s "context" context)
      (Events s owner workspace context)
      (Views r s owner workspace context)
      (e/for-by :id [request runs]
        (activity/Run s owner workspace context request)))))

(e/defn Main
  "Browser page lifetime → seeded session and visible or reopen surface.
   Seeds through Rama, snapshots accepted session defaults once, and retains local
   cells while a closed view displays its reopen control. Each branch directly
   demands its surface so temporary pending view resolution cannot release the GPU.
   Full page cancellation ends the Electric owner; accepted material remains durable."
  []
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
