(ns app.client.engine.leases
  "Device-local Region3D binding owner.

   Semantic ids address desired leases and stable composite slots.  GPU lease
   objects stay here and are stamped by the compositor identity epoch."
  (:require [clojure.set :as set]))

(defn create-owner [device]
  {:device device
   :!state
   (atom {:device-epoch 0
          :compositor nil
          :desired {}
          :slots {}
          :next-slot 0
          :free-slots (sorted-set)
          :pending-slots {}
          :leases {}
          :receipt {:epoch-bumps 0 :slot-allocations 0 :slot-releases 0
                    :desired-updates 0}})})

(defn- take-slot [state]
  (if-let [slot (first (:free-slots state))]
    [(update state :free-slots disj slot) slot]
    [(update state :next-slot inc) (:next-slot state)]))

(defn- lease-key [{:keys [region/id lease-size]}]
  (when (and id lease-size)
    (into [id] lease-size)))

(defn- free-retired-slot! [owner region-id epoch slot]
  (swap! (:!state owner)
         (fn [state]
           (if (= {:epoch epoch :slot slot}
                  (get-in state [:pending-slots region-id]))
             (-> state
                 (update :pending-slots dissoc region-id)
                 (update :free-slots conj slot)
                 (update-in [:receipt :slot-releases] inc))
             state))))

(defn- retire-slot-after-submit! [owner region-id epoch slot]
  (let [queue (.-queue ^js (:device owner))]
    (-> (.onSubmittedWorkDone queue)
        (.then (fn [] (free-retired-slot! owner region-id epoch slot)))
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
                      (let [slot (get-in next-state [:slots region-id])]
                        (when (some? slot)
                          (vswap! retiring conj [region-id epoch slot]))
                        (cond-> (-> next-state
                                    (update :desired dissoc region-id)
                                    (update :slots dissoc region-id)
                                    (update :leases dissoc region-id))
                          (some? slot)
                          (assoc-in [:pending-slots region-id]
                                    {:epoch epoch :slot slot}))))
                    state closed)
                   [state desired]
                   (reduce
                    (fn [[next-state desired] region-id]
                      (let [input (get rows-by-id region-id)
                            [next-state slot]
                            (if-let [slot (get-in next-state [:slots region-id])]
                              [next-state slot]
                              (let [[allocated slot] (take-slot next-state)]
                                [(-> allocated
                                     (assoc-in [:slots region-id] slot)
                                     (update-in [:receipt :slot-allocations] inc))
                                 slot]))
                            row (assoc input :slot slot
                                      :lease-key (lease-key input)
                                      :device-epoch epoch)]
                        [next-state (assoc desired region-id row)]))
                    [state {}]
                    (sort-by pr-str new-ids))
                   changed? (not= (:desired state) desired)]
               (cond-> (assoc state :desired desired)
                 changed? (update-in [:receipt :desired-updates] inc)))))
    (doseq [[region-id epoch slot] @retiring]
      (retire-slot-after-submit! owner region-id epoch slot))
    (:desired @(:!state owner))))

(defn attach-compositor!
  "Attach the sole compositor identity producer.  A replacement bumps the
   device epoch, clears every physical lease, and deterministically reassigns
   slots while retaining desired logical rows."
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
                                            :slots {}
                                            :next-slot 0
                                            :free-slots (sorted-set)
                                            :pending-slots {}
                                            :leases {})
                                     (update-in [:receipt :epoch-bumps] inc))
                     [reset-state desired]
                     (reduce
                      (fn [[next-state rows] row]
                        (let [[next-state slot] (take-slot next-state)
                              region-id (:region/id row)
                              row (assoc row :slot slot :device-epoch epoch)]
                          [(assoc-in next-state [:slots region-id] slot)
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

(defn slot [owner region-id]
  (get-in @(:!state owner) [:slots region-id]))

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

(defn receipt [owner]
  (let [state @(:!state owner)]
    (assoc (:receipt state)
           :device-epoch (:device-epoch state)
           :desired (into {}
                          (map (fn [[id row]]
                                 [id (select-keys row
                                                  [:lease-key :shadow? :slot
                                                   :encode-rung])]))
                          (:desired state))
           :live-slots (:slots state)
           :pending-slots (:pending-slots state)
           :lease-ids (set (keys (:leases state))))))
