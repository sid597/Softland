(ns app.client.engine.leases
  "Which offscreen regions are live: desired region rows reconciled to stable
   buffer indexes and GPU leases, stamped by the compositor's identity.
   Takes: an owner; desired region rows (id and size); a region id and a
   physical lease to record; a compositor to attach.
   Gives: the desired map, a region’s buffer index, its lease, frame stats.
   Holds: one state atom per owner (device epoch, compositor, desired rows,
   buffer indexes, leases)."
  (:require [clojure.set :as set]))

(defn create-owner [device]
  {:device device
   :!state
   (atom {:device-epoch 0
          :compositor nil
          :desired {}
          :buffer-indexes {}
          :next-buffer-index 0
          :free-buffer-indexes (sorted-set)
          :pending-buffer-indexes {}
          :leases {}
          :stats {:epoch-bumps 0 :buffer-index-allocations 0 :buffer-index-releases 0
                    :desired-updates 0}})})

(defn- take-buffer-index [state]
  (if-let [buffer-index (first (:free-buffer-indexes state))]
    [(update state :free-buffer-indexes disj buffer-index) buffer-index]
    [(update state :next-buffer-index inc) (:next-buffer-index state)]))

(defn- lease-key [{:keys [region/id lease-size]}]
  (when (and id lease-size)
    (into [id] lease-size)))

(defn- free-retired-buffer-index! [owner region-id epoch buffer-index]
  (swap! (:!state owner)
         (fn [state]
           (if (= {:epoch epoch :buffer-index buffer-index}
                  (get-in state [:pending-buffer-indexes region-id]))
             (-> state
                 (update :pending-buffer-indexes dissoc region-id)
                 (update :free-buffer-indexes conj buffer-index)
                 (update-in [:stats :buffer-index-releases] inc))
             state))))

(defn- retire-buffer-index-after-submit! [owner region-id epoch buffer-index]
  (let [queue (.-queue ^js (:device owner))]
    (-> (.onSubmittedWorkDone queue)
        (.then (fn [] (free-retired-buffer-index! owner region-id epoch buffer-index)))
        (.catch (fn [_] nil)))))

(defn reconcile-desired!
  "Reconcile prepared, GPU-free desired rows.  Returns the current desired map."
  [owner rows]
  (let [rows-by-id (into {} (map (juxt :region/id identity)) rows)
        retiring (volatile! [])]
    (swap! (:!state owner)
           (fn [state]
             (let [old (:desired state)
                   old-ids (set (keys old))
                   new-ids (set (keys rows-by-id))
                   closed (set/difference old-ids new-ids)
                   epoch (:device-epoch state)
                   state
                   (reduce
                    (fn [next-state region-id]
                      (let [buffer-index (get-in next-state [:buffer-indexes region-id])]
                        (when (some? buffer-index)
                          (vswap! retiring conj [region-id epoch buffer-index]))
                        (cond-> (-> next-state
                                    (update :desired dissoc region-id)
                                    (update :buffer-indexes dissoc region-id)
                                    (update :leases dissoc region-id))
                          (some? buffer-index)
                          (assoc-in [:pending-buffer-indexes region-id]
                                    {:epoch epoch :buffer-index buffer-index}))))
                    state closed)
                   [state desired]
                   (reduce
                    (fn [[next-state desired] region-id]
                      (let [input (get rows-by-id region-id)
                            [next-state buffer-index]
                            (if-let [buffer-index (get-in next-state [:buffer-indexes region-id])]
                              [next-state buffer-index]
                              (let [[allocated buffer-index] (take-buffer-index next-state)]
                                [(-> allocated
                                     (assoc-in [:buffer-indexes region-id] buffer-index)
                                     (update-in [:stats :buffer-index-allocations] inc))
                                 buffer-index]))
                            row (assoc input :buffer-index buffer-index
                                      :lease-key (lease-key input)
                                      :device-epoch epoch)]
                        [next-state (assoc desired region-id row)]))
                    [state {}]
                    (sort-by pr-str new-ids))
                   changed? (not= (:desired state) desired)]
               (cond-> (assoc state :desired desired)
                 changed? (update-in [:stats :desired-updates] inc)))))
    (doseq [[region-id epoch buffer-index] @retiring]
      (retire-buffer-index-after-submit! owner region-id epoch buffer-index))
    (:desired @(:!state owner))))

(defn attach-compositor!
  "Attach the sole compositor identity producer.  A replacement bumps the
   device epoch, clears every physical lease, and deterministically reassigns
   buffer indexes while retaining desired logical rows."
  [owner compositor]
  (let [changed? (volatile! false)]
    (swap! (:!state owner)
           (fn [state]
             (if (identical? compositor (:compositor state))
               state
               (let [epoch (inc (:device-epoch state))
                     desired (vals (:desired state))
                     reset-state (-> state
                                     (assoc :device-epoch epoch
                                            :compositor compositor
                                            :buffer-indexes {}
                                            :next-buffer-index 0
                                            :free-buffer-indexes (sorted-set)
                                            :pending-buffer-indexes {}
                                            :leases {})
                                     (update-in [:stats :epoch-bumps] inc))
                     [reset-state desired]
                     (reduce
                      (fn [[next-state rows] row]
                        (let [[next-state buffer-index] (take-buffer-index next-state)
                              region-id (:region/id row)
                              row (assoc row :buffer-index buffer-index :device-epoch epoch)]
                          [(assoc-in next-state [:buffer-indexes region-id] buffer-index)
                           (assoc rows region-id row)]))
                      [reset-state {}]
                      (sort-by (comp pr-str :region/id) desired))]
                 (vreset! changed? true)
                 (assoc reset-state :desired desired)))))
    @changed?))

(defn desired-rows [owner]
  (->> (:desired @(:!state owner)) vals
       (sort-by (comp pr-str :region/id)) vec))

(defn topology-rows [owner]
  (mapv #(select-keys % [:region/id :shadow?]) (desired-rows owner)))

(defn buffer-index [owner region-id]
  (get-in @(:!state owner) [:buffer-indexes region-id]))

(defn lease [owner region-id]
  (get-in @(:!state owner) [:leases region-id]))

(defn record-lease! [owner region-id lease]
  (swap! (:!state owner) assoc-in [:leases region-id] lease)
  lease)

(defn forget-lease! [owner region-id]
  (swap! (:!state owner) update :leases dissoc region-id))

(defn clear-leases! [owner]
  (swap! (:!state owner) assoc :leases {}))

(defn device-epoch [owner] (:device-epoch @(:!state owner)))
(defn compositor [owner] (:compositor @(:!state owner)))

(defn stats [owner]
  (let [state @(:!state owner)]
    (assoc (:stats state)
           :device-epoch (:device-epoch state)
           :desired (into {}
                          (map (fn [[id row]]
                                 [id (select-keys row
                                                  [:lease-key :shadow? :buffer-index
                                                   :encode-rung])]))
                          (:desired state))
           :live-buffer-indexes (:buffer-indexes state)
           :pending-buffer-indexes (:pending-buffer-indexes state)
           :lease-ids (set (keys (:leases state))))))
