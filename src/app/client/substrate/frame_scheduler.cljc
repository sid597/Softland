(ns app.client.substrate.frame-scheduler
  "Pure W4 cause scheduler plus the session deadline registry.

   The injected time value is consumed only by decide and sink-side helpers.
   It is never a scene/store derivation input. Runtime supplies the timestamp
   already carried by its frame pulse; replay rebinds the source to recorded
   logical time."
  (:require [clojure.set :as set]))

(def scheduler-version 1)
(def legal-causes
  #{:world :viewport :resource :interaction :clock :readback :device-recovery})
(def default-ring-size 64)

(defonce ^:private !clock-source (atom identity))
(defonce ^:private !deadlines (atom {}))

(defn set-clock-source!
  "Rebind the logical clock source. The function receives the host frame
   timestamp; identity is the default and replay may return recorded values."
  [source]
  (when-not (ifn? source)
    (throw (ex-info "Clock source must be callable" {:source source})))
  (reset! !clock-source source)
  true)

(defn reset-clock-source! []
  (reset! !clock-source identity)
  true)

(defn clock-time [host-frame-time]
  (@!clock-source host-frame-time))

(defn- finite-number? [x]
  (and (number? x)
       #?(:clj (Double/isFinite (double x))
          :cljs (js/Number.isFinite x))))

(defn validate-deadline!
  [{:keys [next-deadline cadence stop-predicate] :as deadline}]
  (when-not (and (finite-number? next-deadline) (not (neg? next-deadline)))
    (throw (ex-info "Deadline next-deadline must be finite and non-negative"
                    {:deadline deadline})))
  (when-not (and (finite-number? cadence) (pos? cadence))
    (throw (ex-info "Deadline cadence must be finite and positive"
                    {:deadline deadline})))
  (when-not (ifn? stop-predicate)
    (throw (ex-info "Clocked consumer requires a stop predicate"
                    {:deadline deadline})))
  deadline)

(defn register-deadline!
  "Register or replace one clocked consumer. Returns an unregister function."
  [id deadline]
  (when (nil? id)
    (throw (ex-info "Deadline id cannot be nil" {})))
  (swap! !deadlines assoc id (validate-deadline! deadline))
  (fn [] (swap! !deadlines dissoc id) true))

(defn unregister-deadline! [id]
  (swap! !deadlines dissoc id)
  true)

(defn clear-deadlines! []
  (reset! !deadlines {})
  true)

(defn deadlines [] @!deadlines)

(defn derive-causes
  "Translate the current render edge's facts into the seven settled causes."
  [{:keys [world-changed? viewport-changed? resource-ready?
           interaction-changed? clock-due? readback-pending?
           device-recovery? camera-moved? dirty-rect-pending?]}]
  (cond-> #{}
    world-changed? (conj :world)
    (or viewport-changed? camera-moved?) (conj :viewport)
    resource-ready? (conj :resource)
    (or interaction-changed? dirty-rect-pending?) (conj :interaction)
    clock-due? (conj :clock)
    readback-pending? (conj :readback)
    device-recovery? (conj :device-recovery)))

(defn initial-state []
  {:scheduler/version scheduler-version
   :last-time nil :encodes 0 :skips 0 :causes-ring []
   :clock-mode :injected/monotonic :last-plan-hash nil})

(defn- stopped? [stop-predicate]
  (try
    (boolean (stop-predicate))
    (catch #?(:clj Throwable :cljs :default) error
      (throw (ex-info "Deadline stop predicate failed" {} error)))))

(defn due-deadlines
  "Pure deadline step. Stopped consumers retire. Due consumers advance by
   whole cadence steps until their next deadline is strictly after now."
  [deadline-map now]
  (reduce-kv
   (fn [{:keys [active due retired] :as result} id deadline]
     (validate-deadline! deadline)
     (if (stopped? (:stop-predicate deadline))
       (assoc result :retired (conj retired id))
       (if (>= now (:next-deadline deadline))
         (let [next-deadline
               (loop [candidate (:next-deadline deadline)]
                 (if (> candidate now)
                   candidate
                   (recur (+ candidate (:cadence deadline)))))]
           (-> result
               (assoc-in [:active id] (assoc deadline
                                             :next-deadline next-deadline))
               (assoc :due (conj due id))))
         (assoc-in result [:active id] deadline))))
   {:active {} :due [] :retired []}
   deadline-map))

(defn- monotonic-time [prior now]
  (let [now (double now)]
    (if (some? prior) (max (double prior) now) now)))

(defn- append-ring [ring row limit]
  (let [rows (conj (vec ring) row)]
    (if (> (count rows) limit)
      (subvec rows (- (count rows) limit))
      rows)))

(defn decide
  "Pure scheduler step. One invocation represents one frame opportunity, so a
   non-empty coalesced cause set produces at most one encode."
  ([state now causes deadline-map]
   (decide state now causes deadline-map nil))
  ([state now causes deadline-map plan-hash]
   (let [state (or state (initial-state))
         now (monotonic-time (:last-time state) now)
         unknown (seq (remove legal-causes causes))
         _ (when unknown
             (throw (ex-info "Unknown frame invalidation causes"
                             {:unknown (vec unknown)})))
         deadline-step (due-deadlines deadline-map now)
         causes (cond-> (set causes)
                  (seq (:due deadline-step)) (conj :clock))
         encode? (boolean (seq causes))
         row {:time now :causes (vec (sort causes)) :encode? encode?
              :due-deadlines (vec (:due deadline-step))
              :plan-hash plan-hash}
         next-state (-> state
                        (assoc :last-time now
                               :encode? encode?
                               :causes causes
                               :time now
                               :due-deadlines (vec (:due deadline-step))
                               :retired-deadlines (vec (:retired deadline-step))
                               :last-plan-hash plan-hash)
                        (update (if encode? :encodes :skips) (fnil inc 0))
                        (update :causes-ring append-ring row default-ring-size))]
     {:encode? encode?
      :causes causes
      :time now
      :deadlines (:active deadline-step)
      :due-deadlines (vec (:due deadline-step))
      :retired-deadlines (vec (:retired deadline-step))
      :state next-state})))

(defn decide-at!
  "Runtime edge over the module deadline registry for an already-resolved
   logical time. State itself remains owned by the render reduce."
  [state logical-time causes plan-hash]
  (let [result (decide state logical-time causes @!deadlines plan-hash)]
    (reset! !deadlines (:deadlines result))
    result))

(defn decide!
  "Resolve host frame time through the injected source exactly once, then step
   the module deadline registry."
  [state host-frame-time causes plan-hash]
  (decide-at! state (clock-time host-frame-time) causes plan-hash))

(defn replay
  "Replay recorded cause/time rows through the pure decide step."
  [rows deadline-map]
  (loop [remaining rows state (initial-state) deadlines deadline-map result []]
    (if-let [{:keys [time causes plan-hash]} (first remaining)]
      (let [step (decide state time (set causes) deadlines plan-hash)]
        (recur (next remaining) (:state step) (:deadlines step)
               (conj result (select-keys step [:encode? :causes :time]))))
      {:state state :deadlines deadlines :decisions result})))

(defn receipt [state]
  (select-keys (or state (initial-state))
               [:scheduler/version :encodes :skips :causes-ring
                :clock-mode :last-plan-hash]))
