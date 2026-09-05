(ns app.client.engine.leases
  "Maintain logical region identity across physical allocations.

   Input: desired region rows and compositor attachments. Output: stable
   buffer indexes, current desired rows/leases, topology rows and
   statistics. One owner atom holds logical identities, device epoch,
   free/pending index sets, and references to physical leases. It does not
   allocate textures. Closed indexes become reusable after submitted GPU
   work completes; compositor replacement increments an epoch and resets
   physical associations.

   Folder map: README.md."
  (:require [clojure.set :as set]))

(defn create-owner
  "Device → owner with fresh state atom.

   Explicit state container."
  [device]
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

(defn- take-buffer-index
  "State → [new-state index].

   Smallest free index or next integer."
  [state]
  (if-let [buffer-index (first (:free-buffer-indexes state))]
    [(update state :free-buffer-indexes disj buffer-index) buffer-index]
    [(update state :next-buffer-index inc) (:next-buffer-index state)]))

(defn- lease-key
  "Desired row → [region-id width height] or nil.

   Requires ID and size. Malformed desired rows are not otherwise rejected
   here."
  [{:keys [region/id lease-size]}]
  (when (and id lease-size)
    (into [id] lease-size)))

(defn- free-retired-buffer-index!
  "Owner, region ID, epoch, index → swapped state.

   Frees only the matching pending record. Stale callbacks cannot free a
   replacement index."
  [owner region-id epoch buffer-index]
  (swap! (:!state owner)
         (fn [state]
           (if (= {:epoch epoch :buffer-index buffer-index}
                  (get-in state [:pending-buffer-indexes region-id]))
             (-> state
                 (update :pending-buffer-indexes dissoc region-id)
                 (update :free-buffer-indexes conj buffer-index)
                 (update-in [:stats :buffer-index-releases] inc))
             state))))

(defn- retire-buffer-index-after-submit!
  "Owner and retirement identity → promise.

   Waits for queue completion, then conditionally frees. Rejection is
   swallowed; no observable recovery path here."
  [owner region-id epoch buffer-index]
  (let [queue (.-queue ^js (:device owner))]
    (-> (.onSubmittedWorkDone queue)
        (.then (fn [] (free-retired-buffer-index! owner region-id epoch buffer-index)))
        (.catch (fn [_] nil)))))

(defn reconcile-desired!
  "Owner, desired rows → reconciled map; mutates identities and schedules
   retirement.

   Diffs ID sets, sorts allocation order, retains live indexes. Duplicate
   input IDs collapse through map construction."
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
  "Owner and compositor → changed?; resets physical bindings on identity
   change.

   Epoch bump plus deterministic reassignment. Physical generation changes
   are explicit."
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

(defn desired-rows
  "Owner → desired rows sorted by printed ID.

   Deterministic projection. Sorts on every read."
  [owner]
  (->> (:desired @(:!state owner)) vals
       (sort-by (comp pr-str :region/id)) vec))

(defn topology-rows
  "Owner → ordered ID/shadow rows.

   Projects desired rows."
  [owner]
  (mapv #(select-keys % [:region/id :shadow?]) (desired-rows owner)))

(defn buffer-index
  "Owner and region ID → current index or nil."
  [owner region-id]
  (get-in @(:!state owner) [:buffer-indexes region-id]))

(defn lease
  "Owner and region ID → recorded physical lease or nil."
  [owner region-id]
  (get-in @(:!state owner) [:leases region-id]))

(defn record-lease!
  "Owner, region ID, lease → same lease; records it.

   Single atom update. Trusts caller ownership."
  [owner region-id lease]
  (swap! (:!state owner) assoc-in [:leases region-id] lease)
  lease)

(defn forget-lease!
  "Owner and region ID → updated state; removes reference.

   Does not destroy physical storage. Lifetime stays with compositor."
  [owner region-id]
  (swap! (:!state owner) update :leases dissoc region-id))

(defn clear-leases!
  "Owner → updated state with empty references.

   Clears logical references; physical resource destruction belongs to the
   compositor."
  [owner]
  (swap! (:!state owner) assoc :leases {}))

(defn device-epoch
  "Owner → current generation integer."
  [owner] (:device-epoch @(:!state owner)))
(defn compositor
  "Owner → attached compositor or nil."
  [owner] (:compositor @(:!state owner)))

(defn stats
  "Owner → diagnostic snapshot of counters, desired keys, indexes and lease
   IDs.

   Projects state rather than returning GPU objects."
  [owner]
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
