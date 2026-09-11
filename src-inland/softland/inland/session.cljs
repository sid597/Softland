(ns softland.inland.session
  "Browser-session storage and generic effect delivery.
   Takes session defaults, native events and validated effect records; gives local
   facts, admission proposals and repeated-step progress. Holds per-key atoms, one
   event slot, one request slot, visibility and activity handles for this session.
   These cells are drafts/attention, not a copy of Rama rows. Closing content clears
   its step work but retains cells for the reopen surface; page lifetime owns the
   session. Window diagnostics retain the latest inspected values for this page."
  (:require [clojure.string :as str]
            [softland.inland.total :as total]))

(defn workspace
  "URL workspace parameter → bounded identifier, otherwise workbench.
   Accepts 1–64 ASCII letters, digits or hyphens; performs no durable lookup."
  []
  (let [v (.get (js/URLSearchParams. (.-search js/location)) "workspace")]
    (if (and v (re-matches #"[A-Za-z0-9-]{1,64}" v)) v "workbench")))
(defn create
  "Owner id and default values → a fresh session with independent cell atoms.
   Call once per Electric session owner; plain defaults become local mutable cells."
  [id defaults] {:id id :cells (atom (into {} (for [[key value] defaults] [key (atom value)]))) :event (atom nil)
                :request (atom nil) :result (atom nil) :visible (atom true)
                :work (atom {}) :cancel (atom {})})
(defn cell
  "Session and key → existing atom, allocating a nil-valued atom if missing.
   The session retains allocated cells until its page lifetime ends."
  [s k]
  (or (get @(:cells s) k)
      (get (swap! (:cells s) #(if (contains? % k) % (assoc % k (atom nil)))) k)))
(defn emit!
  "Event data → install an event with a fresh id in the session's delivery slot.
   This is a single replaceable slot; it does not queue concurrent events."
  [s event]
  (reset! (:event s) (assoc event :id (str (random-uuid)))))
(defn submit!
  "Operation → install a request and local pending status; return its envelope.
   Adds default workspace/id/base layer, with explicit operation fields taking
   precedence. It does not write accepted state or retry; Operations owns submission."
  [s op]
  (let [op (merge {:workspace (workspace) :request-id (str (random-uuid)) :layer "base"} op)]
    (reset! (:result s) nil) (reset! (:request s) op)
    (reset! (cell s "admission") {:status :pending :request-id (:request-id op)}) op))
(declare start-progress!)
(defn effects!
  "Validated effect vector → ordered local writes or proposal/owner changes.
   Handles session, admission, event, visibility, activity and cancellation effects.
   Closing visibility marks running view work cancelled and clears its handles.
   Admission only replaces the request slot; this loop does not await acceptance."
  [s effects]
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
                  (start-progress! s request)
                  (swap! (:work s) assoc (:output request) request))
      :cancel (reset! (cell s (str "cancel/" (:owner effect))) true)
      nil)))
(defn complete-event!
  "Snapshotted event and effects → apply effects and consume that same event.
   An effect may emit a new event; compare ids so the new delivery is preserved."
  [s event effects]
  (effects! s effects)
  (swap! (:event s) #(when (not= (:id %) (:id event)) %)))
(defn deliver!
  "Native hit/hover event → local pointer update, native focus, or authored event.
   Aim updates the pointer cell directly. Other gestures get a subject and fresh
   command id, keeping physical event delivery separate from authored selection."
  [s event]
  (case (:kind event)
    :native-input ((:run event) (:point event))
    :aim (reset! (cell s "pointer") (:id event))
    (emit! s (-> event (assoc :subject (or (:subject event) (:id event) "world")) (dissoc :id)))))
(defn start-progress!
  "Activity request → initialize and return its output cell.
   Integer budgets 1–128 start at iteration zero with explicit state/remaining work;
   invalid budgets produce failed progress. The initiating event calls this once."
  [s request]
  (let [a (cell s (:output request))]
    (reset! a (if (and (integer? (:budget request)) (<= 1 (:budget request) 128))
                {:status :running :state (:state request) :remaining (:budget request) :iteration 0}
                {:status :failed :reason "Use an integer step budget from 1 to 128."})) a))
(defn advance!
  "Owned request, iteration snapshot and result → effects plus next progress.
   Checks current request identity, running status and cancellation before acting.
   Admission effects receive stable activity/iteration ids and context provenance.
   Effects are proposed before storing progress; this is not a queue that awaits
   durable effects before the next iteration. A failed outcome applies no effects."
  [s request current result]
  (let [progress (cell s (:output request))
        next (assoc (total/step-result result (:state current) (:remaining current))
                    :iteration (inc (:iteration current)))]
    (when (and (= (:id request) (get-in @(:work s) [(:output request) :id]))
               (= :running (:status @progress))
               (not @(cell s (str "cancel/" (:id request)))))
      (when-not (= :failed (:status next))
        (effects! s (mapv (fn [effect]
                           (if (= :admit (:effect effect))
                             (-> effect
                               (assoc-in [:request :request-id] (str (:id request) "/step/" (:iteration current)))
                               (assoc-in [:request :invocation] {:activity (:id request) :iteration (:iteration current) :context (:context request)}))
                             effect)) (:effects result))))
      (reset! progress next))))
(defn report!
  "Session, diagnostic key and value → replace that key in window.__inland.
   Records the current owner alongside the value; diagnostic retention is page-owned."
  [s key value]
  (let [root (or (.-__inland js/window) #js {})]
    (aset root key (clj->js value))
    (aset root "owner" (:id s))
    (set! (.-__inland js/window) root)))
(defn diagnostics!
  "Session → expose a read accessor for local cells on window.
   The accessor uses cell, so asking for a missing key allocates a local nil cell."
  [s]
  (set! (.-__inlandCell js/window) (fn [key] (clj->js @(cell s key)))))
(defn received!
  "Observed admission result → publish it in the result and admission cells.
   This does not alter an accepted row or independently validate the server decision."
  [s result]
  (reset! (:result s) result) (reset! (cell s "admission") result))
