(ns app.client.workspace.t2-block-join
  "Pure join between one served durable block and the existing T2 edit kernel.

   Full projections own availability/membership. The edit queue owns accepted
   local projection while a keyed read is causally outstanding. Only a keyed
   response carrying the nonce armed by that acceptance may release the wait."
  (:require [app.client.workspace.block-edit :as block-edit]
            [app.client.workspace.edit-transport :as transport]
            [app.client.workspace.text-editing :as editing]))

(defn occurrence-vi [unit-id]
  [:vi :t2-real-block unit-id])

(defn eligible-block? [block]
  (and (some? (:id block))
       (string? (:text block))
       (some? (:document-container-id block))
       (some? (:object-key block))))

(defn context-blocks [context]
  (vec (mapcat #(or (:blocks %) []) (or (:turns context) []))))

(defn select-block
  "Retain the current eligible unit; otherwise choose the final eligible block
   in the exact delivered turn/block vector order."
  [context current-unit-id]
  (let [eligible (vec (filter eligible-block? (context-blocks context)))]
    (or (some #(when (= current-unit-id (:id %)) %) eligible)
        (peek eligible))))

(defn t2-real-block-spec
  "Temporary retained occurrence placement. These are client visual facts,
   not durable block geometry and not the Ground occurrence."
  [block]
  {:vi (occurrence-vi (:id block))
   :address [:derived-unit (:id block)]
   :kind :real-block
   :text (:text block)
   :font-size 20.0
   :line-height 28.0
   :baseline-offset 21.0
   :wrap-col 39
   :origin [18.0 14.0]
   :bounds {:x 0.0 :y 0.0 :w 510.0 :h 168.0}
   :container {:x 60.0 :y 420.0 :layer 18 :sibling-rank 18}
   :text-color [0.94 0.95 0.98 1.0]})

(defn init []
  {:target nil
   :edit-client-id nil
   :next-seq 0
   :queue nil
   :awaiting-keyed-truth nil
   :keyed-candidate nil
   :refusal nil})

(defn view [state]
  (when-let [block (get-in state [:target :block])]
    (let [projected (transport/projection (:queue state))]
      {:unit-id (:id block)
       :vi (get-in state [:target :vi])
       :spec (assoc (t2-real-block-spec block) :text (:text projected))
       :text (:text projected)
       :caret (:caret projected)
       :refusal (:refusal state)
       :awaiting-keyed-truth (:awaiting-keyed-truth state)})))

(defn reconcile-full
  "Apply one whole served context. While a keyed read is awaited, full text is
   availability only. A replacement target receives a deliberately fresh
   edit-client id supplied by the effectful owner."
  [state context fresh-edit-client-id]
  (let [current-id (get-in state [:target :block :id])
        selected (select-block context current-id)]
    (cond
      (nil? selected)
      (init)

      (not= current-id (:id selected))
      {:target {:block selected :vi (occurrence-vi (:id selected))}
       :edit-client-id fresh-edit-client-id
       :next-seq 0
       :queue (transport/seed (:text selected) (count (:text selected)))
       :awaiting-keyed-truth nil
       :keyed-candidate nil
       :refusal nil}

      :else
      (let [state (assoc-in state [:target :block] selected)]
        (if (:awaiting-keyed-truth state)
          state
          (update state :queue transport/adopt-truth (:text selected)))))))

(defn commit
  "Turn one T2 semantic result into one existing object/edit envelope.
   Motion, composition preedit, no-op/refused results, and foreign VIs emit
   nothing. T2's tagged caret is validated and becomes a plain UTF-16 offset
   only at this transport boundary."
  [state {:keys [vi text caret ops]}]
  (let [target (:target state)]
    (if-not (and target (= vi (:vi target)) (seq ops) (string? text))
      {:state state :envelope nil}
      (let [_ (editing/assert-safe-offset! text caret)
            caret-offset (editing/offset caret)
            seq-number (:next-seq state)
            block (:block target)
            envelope (block-edit/mint-envelope
                      {:block block
                       :object-key (:object-key block)
                       :content-text text
                       :edit-client-id (:edit-client-id state)
                       :edit-seq seq-number})
            entry {:seq seq-number
                   :request-id (:request-id envelope)
                   :text text
                   :caret caret-offset}]
        {:state (-> state
                    (update :next-seq inc)
                    (update :queue transport/enqueue entry)
                    (assoc :refusal nil))
         :envelope envelope}))))

(defn- nonce-at-least? [actual expected]
  (and (number? actual) (number? expected)
       (>= (long actual) (long expected))))

(defn- release-candidate [state]
  (let [{:keys [unit-id request-nonce text found?]} (:keyed-candidate state)
        awaited (:awaiting-keyed-truth state)
        target-id (get-in state [:target :block :id])]
    (if (and found?
             (= target-id unit-id)
             (nonce-at-least? request-nonce (:nonce awaited))
             (transport/settled? (:queue state)))
      (-> state
          (update :queue transport/adopt-truth text)
          (assoc :awaiting-keyed-truth nil :keyed-candidate nil))
      state)))

(defn on-decision [state request-id decision]
  (let [{:keys [queue matched? accepted? reason] :as transition}
        (transport/decide (:queue state) request-id decision)]
    (if-not matched?
      state
      (let [state (assoc state :queue queue)]
        (if accepted?
          (cond-> (assoc state :refusal nil :keyed-candidate nil)
            (number? (:request-nonce decision))
            (assoc :awaiting-keyed-truth
                   {:request-id request-id
                    :nonce (:request-nonce decision)}))
          (-> state
              (assoc :refusal {:unit-id (get-in state [:target :block :id])
                               :reason reason})
              release-candidate))))))

(defn on-keyed-truth
  "Admit only the current target's causally matching keyed truth. A matching
   response received while later intent remains in flight is retained until
   that flight settles; an older nonce is ignored permanently."
  [state unit-id {:keys [found? text request-nonce] :as truth}]
  (let [awaited (:awaiting-keyed-truth state)
        target-id (get-in state [:target :block :id])]
    (if (and awaited found? (= target-id unit-id)
             (nonce-at-least? request-nonce (:nonce awaited)))
      (-> state
          (assoc :keyed-candidate
                 (assoc truth :unit-id unit-id))
          release-candidate)
      state)))
