(ns app.client.workspace.edit-transport
  "Pure target-local edit transport shared by Ground and the T2 real-block join.

   This namespace owns only the ordered intent queue: seed, projection, caret
   movement, bounded enqueue, durable decision correlation, settled truth
   adoption. It owns no keymap, selection, IME, atoms, or rendering.")

(def max-inflight 64)

(defn clamp-caret [text caret]
  (let [text (or text "")]
    (max 0 (min (long (or caret 0)) (count text)))))

(defn seed
  ([text caret] (seed text caret -1))
  ([text caret seq-number]
   (let [text (or text "")]
     {:confirmed {:text text
                  :caret (clamp-caret text caret)
                  :seq (long seq-number)}
      :inflight []})))

(defn projection
  "The coherent (text, caret) at the head of the queue."
  [queue]
  (when queue
    (or (peek (:inflight queue)) (:confirmed queue))))

(defn settled? [queue]
  (boolean (and queue (empty? (:inflight queue)))))

(defn map-caret
  "Move the caret on confirmed and on the projected in-flight head."
  [queue caret]
  (if-not queue
    queue
    (let [move (fn [{:keys [text] :as entry}]
                 (assoc entry :caret (clamp-caret text caret)))]
      (cond-> (update queue :confirmed move)
        (seq (:inflight queue))
        (update-in [:inflight (dec (count (:inflight queue)))] move)))))

(defn enqueue
  "Append one whole-text intent, retaining at most `max-inflight` entries."
  [queue entry]
  (if-not queue
    queue
    (update queue :inflight
            (fn [entries]
              (let [entries (vec entries)
                    entries (if (>= (count entries) max-inflight)
                              (subvec entries 1)
                              entries)]
                (conj entries entry))))))

(defn decide
  "Correlate one durable decision to this queue.

   Accepted seq N confirms N and retires every entry <= N. A correlated
   rejection drops the whole dependent flight. Unknown request ids are a
   strict no-op. Returns the transition facts alongside the next queue."
  [queue request-id decision]
  (let [entry (some #(when (= request-id (:request-id %)) %)
                    (:inflight queue))]
    (cond
      (or (nil? queue) (nil? entry))
      {:queue queue :matched? false}

      (= :accepted (:status decision))
      {:queue {:confirmed {:text (:text entry)
                           :caret (:caret entry)
                           :seq (:seq entry)}
               :inflight (vec (remove #(<= (:seq %) (:seq entry))
                                      (:inflight queue)))}
       :matched? true
       :accepted? true
       :entry entry}

      :else
      {:queue (assoc queue :inflight [])
       :matched? true
       :accepted? false
       :entry entry
       :reason (or (:reason decision) :edit-refused)})))

(defn adopt-truth
  "Adopt queryable text only at rest, retaining and clamping the caret."
  [queue truth-text]
  (if (and (settled? queue)
           (some? truth-text)
           (not= truth-text (get-in queue [:confirmed :text])))
    (update queue :confirmed
            (fn [confirmed]
              {:text truth-text
               :caret (clamp-caret truth-text (:caret confirmed))
               :seq (:seq confirmed)}))
    queue))
