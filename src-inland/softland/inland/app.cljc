(ns softland.inland.app
  "Electric composition root for the authored workbench, on the rig's store.
   Takes a logged-in person and a workspace; gives rendered views, one-shot
   gestures and admission results. Holds a browser session and separate
   visible/reopen render owners; borrows the store through execution and store.
   A gesture runs once, on the server (store/gesture!): its rules' reads are
   recorded in one act before any of its effects run; stored cells and
   admissions are written there, standing on what it read; the page applies the
   rest. Maintained views keep live dependencies through the store's standing
   reads, delivered when the store pushes. The bootstrap world address and
   unanswered-event fallback are compiled choices; this host does not make every
   part of itself editable as ordinary material."
  (:require [hyperfiddle.electric3 :as e]
            [missionary.core :as m]
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
   is not proof that the store did not accept the already submitted operation."
  [s owner]
  (e/client
    (let [request (e/watch (:request s))]
      (e/for-by :request-id [op (if request [request] [])]
        (let [decision (e/server (e/Offload #(store/submit-result! owner op)))]
          (session/received! s decision))))))

(e/defn Events
  "Event, owner and resolution context → one gesture, run once on the server.
   The page sends the event and a snapshot of its local cells; the server runs the
   rules, records their reads in one act, writes the stored cells and admissions,
   and returns the effects left for the page. Failures update local admission
   status. Consuming the event prevents completed commands replaying on rule edits."
  [s owner workspace context]
  (e/client
    (let [event (e/watch (:event s))]
      (e/for-by :id [event (if event [event] [])]
        (let [cells (session/snapshot s)
              outcome (e/server (e/Offload #(store/gesture! owner (dissoc event :run) cells)))
              failure (:failure outcome)
              decisions (:decisions outcome)
              effects (if failure
                        [{:effect :session :writes {"admission" {:status :failed :reason (:reason failure)}}}]
                        (cond-> (vec (:effects outcome))
                          (seq decisions) (conj {:effect :session :writes {"admission" (last decisions)}})))]
          (session/report! s "last-event" {:event event :supports (:supports outcome)
                                           :recorded (str (:recorded outcome)) :decisions decisions})
          (session/complete-event! s event effects))))))

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

(e/defn Marks
  "The session's marks → the page's `marks` cell: what stood on a fact the
   screen saw change, as the store's lookup found it, recorded as read."
  [s owner]
  (let [marks (e/server (e/input (m/reductions (fn [_ v] v) {:cells {} :things {}}
                                                (m/eduction (map #(store/marks-view owner %)) (store/marks-flow owner)))))]
    (e/client (reset! (session/cell s "marks") marks) nil)))

(e/defn LiveContext
  "Retained surface and session → context-dependent events, views and step owners.
   The context is the session's stored cell; nothing renders until it is read.
   Repeated work is keyed by request id and cancelled when its owner leaves."
  [r s owner workspace cells who]
  (e/client
    (let [stored (x/Cell s owner workspace {:cells cells} "context")
          runs (vals (e/watch (:work s)))]
      (Marks s owner)
      (when (and (map? stored) (vector? (:layers stored)))
        (let [context (merge stored {:session owner :who who :cells cells})]
          (session/report! s "context" context)
          (Events s owner workspace context)
          (Views r s owner workspace context)
          (e/for-by :id [request runs]
            (activity/Run s owner workspace context request)))))))

(e/defn Main
  "Browser page lifetime, for the person the host logged in → their session in the
   store, then the visible or reopen surface. The session opens before any read;
   it closes when the page goes. Seeds local cells from the accepted world record,
   and retains them while a closed view displays its reopen control. Each branch
   directly demands its surface so pending view resolution cannot release the GPU."
  [person]
  (e/client
    (let [owner (str (random-uuid)) workspace (session/workspace)
          info (e/server (let [i (e/Offload #(store/page-open! owner person))]
                           (e/on-unmount #(store/close-session! owner))
                           i))
          cells {:layer (:hand info) :thing (:thing info) :stored (:stored info)}
          world (when (:thing info) (x/Read owner :rows [workspace "base/world"]))]
      (when (:session world)
        (let [s (session/create owner (e/snapshot (:session world)))
              visible (e/watch (:visible s))]
          (session/diagnostics! s)
          (Operations s owner)
          (if visible
            (let [r (n/Await (render/open #(session/deliver! s %)))]
              (LiveContext r s owner workspace cells (:person info))
              ;; The visible owner itself demands the surface, including while
              ;; every authored view is between completed resolution contexts.
              (some? r))
            (let [r (n/Await (render/open #(session/deliver! s %)))
                  context {:layers ["base"] :cells cells :who (:person info)}
                  value (x/Call s owner workspace context (:closed-view world) {} 0)]
              (Events s owner workspace context)
              (paint/Items r s owner workspace context "closed" value)
              (some? r))))))))
