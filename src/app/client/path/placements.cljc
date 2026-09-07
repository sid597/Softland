(ns app.client.path.placements
  "Pure incremental preparation of named path placements.
   Takes current values and explicit placement/group/view changes; gives
   current geometry, packs, order/ranges and affected sets. Holds no hidden
   state and no history cache. GPU allocation and writes live in push.cljs.
   Evidence: frame_test.clj; harness/path_push.cljs counts the physical edge."
  (:require [clojure.set :as set]
            [app.client.engine.transform :as transform]
            [app.client.path.frame :as frame]
            [app.client.path.component :as component]
            [app.client.path.pack :as pack]))

(defn empty-state [view]
  {:view view :groups {} :entries {} :members {} :world #{} :snapped-world #{}
   :order [] :positions {} :ranges {} :row-count 0 :packs {} :pack-users {}})

(defn cover-options
  "Pack, rule and bucket → existing cover policy, at the bucket's lower scale."
  [packed rule bucket]
  (let [[x0 y0 x1 y1] (:bbox packed) w (- x1 x0) h (- y1 y0)
        scale (pack/bucket-scale bucket) area-px (* w h scale scale)
        margin (pack/bucket-margin bucket)]
    (if (and (> area-px (* 256 256)) (> (/ area-px (max 1 (:count packed))) 2048))
      {:mode :cells :margin margin :cell (/ (max w h) 12.0) :rule rule}
      {:mode :box :margin margin})))

(defn- pack-for [packs region bucket]
  (let [rkey (frame/region-key region) cached (get packs rkey)
        slot-bucket (if (and cached (zero? (:cubics cached))) :all bucket)
        have-pack? (contains? (:packs cached) slot-bucket)
        have-cover? (contains? (:covers cached) bucket)]
    (if (and have-pack? have-cover?)
      [packs (when-let [p (get-in cached [:packs slot-bucket])]
               {:key [rkey slot-bucket] :pack p :cover (get-in cached [:covers bucket])}) false]
      (let [{:keys [pack cubics]} (if have-pack? {:pack (get-in cached [:packs slot-bucket]) :cubics (:cubics cached)}
                                    (pack/pack-region (:path region) (pack/bucket-tolerance bucket) {}))
            slot-bucket (if (zero? cubics) :all bucket)
            cover (when pack (if have-cover? (get-in cached [:covers bucket])
                                  (pack/cover pack (cover-options pack (:rule region) bucket))))]
        [(-> packs (assoc-in [rkey :cubics] cubics) (assoc-in [rkey :packs slot-bucket] pack)
             (assoc-in [rkey :covers bucket] cover))
         (when pack {:key [rkey slot-bucket] :pack pack :cover cover}) (not have-pack?)]))))

(defn pack-entries [entry]
  (keep second (concat (:regions entry) (when (:clip entry) [(:clip entry)]))))

(defn- prepare [state id item]
  (let [prior (get-in state [:entries id])
        record (:path/material item) groups (:groups state) view (:view state)
        index (transform/buffer-index groups (:container item))
        iv (frame/item-view view (get groups (:container item)))
        input (frame/item-key item view groups)]
    (if (= input (:input prior)) [state false #{} false]
        (let [gi (component/geometry-inputs record iv) geometry? (not= gi (:geometry-inputs prior))
              geometry (if geometry? (component/geometry gi) (:geometry prior))
              bucket (pack/scale-bucket (:scale iv))
              [packs clip clip-packed?] (if-let [clip (:clip geometry)]
                                         (let [[ps p lowered?] (pack-for (:packs state) clip bucket)] [ps (when p [clip p]) lowered?])
                                         [(:packs state) nil false])
              [packs regions packed] (reduce (fn [[ps rs changed] r]
                                               (let [[ps p lowered?] (pack-for ps r bucket)]
                                                 [ps (conj rs [r p]) (cond-> changed lowered? (conj (frame/region-key r)))]))
                                             [packs [] (if clip-packed? #{(frame/region-key (:clip geometry))} #{})]
                                             (if (and (:clip geometry) (nil? clip)) [] (:regions geometry)))
              n (reduce + 0 (map #(count (get-in % [1 :cover :rects])) regions))
              entry {:item item :input input :geometry-inputs gi :geometry geometry
                     :regions regions :clip clip :group-index index :count n
                     :row-input [index (:path/paint record) regions clip]}]
          [(-> state (assoc :packs packs) (assoc-in [:entries id] entry)) geometry? packed
           (not= (:row-input prior) (:row-input entry))]))))

(defn- remove-membership [state id entry]
  (if-not entry state
          (-> state (update-in [:members (get-in entry [:item :container])] disj id)
              (update :world disj id) (update :snapped-world disj id))))

(defn- add-membership [state id entry]
  (let [group (get-in entry [:item :container]) world? (not= 1 (get-in state [:groups group :flags]))]
    (cond-> (update-in state [:members group] (fnil conj #{}) id)
      world? (update :world conj id)
      (and world? (get-in entry [:item :path/material :path/snap?])) (update :snapped-world conj id))))

(defn- ranges [state old affected order-changed?]
  (let [order (:order state)
        changed-counts (filter #(not= (get-in old [:entries % :count]) (get-in state [:entries % :count])) affected)
        start (if order-changed? 0 (reduce min (count order) (keep (:positions state) changed-counts)))
        offset (if (zero? start) 0 (reduce + (get-in state [:ranges (nth order (dec start))])))
        [state moved total] (reduce (fn [[s moved offset] id]
                                      (let [n (get-in s [:entries id :count]) r [offset n]
                                            moved? (not= r (get-in old [:ranges id]))]
                                        [(assoc-in s [:ranges id] r) (cond-> moved moved? (conj id)) (+ offset n)]))
                                    [state #{} offset] (subvec order start))]
    [(if (and (not order-changed?) (= start (count order))) state (assoc state :row-count total)) moved]))

(defn change
  "Current state, {:upsert :remove :order :groups}, optional new view →
   {:state :reran :affected :drop-packs}. Only supplied ids and declared
   view/group dependents enter preparation. Order/count shifts visit their
   affected suffix; named edits of unchanged row count never walk order."
  [old {:keys [upsert remove order groups] :as diff} view]
  (let [view (or view (:view old)) groups (or groups (:groups old))
        changed-groups (when (and (:groups diff) (not (identical? groups (:groups old))))
                         (filter #(not= (get groups %) (get-in old [:groups %]))
                                 (set/union (set (keys groups)) (set (keys (:groups old))))))
        group-ids (reduce into #{} (map #(get-in old [:members %]) changed-groups))
        view-ids (cond
                   (not= (:zoom view 1.0) (get-in old [:view :zoom] 1.0)) (:world old)
                   (not= (:pan view [0 0]) (get-in old [:view :pan] [0 0])) (:snapped-world old)
                   :else #{})
        remove (set remove)
        _ (when (seq (set/intersection remove (set (keys upsert))))
            (throw (ex-info "An id cannot be removed and upserted together" {:reason :placement-diff})))
        affected (set/difference (into (set/union group-ids view-ids) (keys upsert)) remove)
        all-touched (set/union affected remove)
        state (assoc old :view view :groups groups)
        state (reduce (fn [s id] (-> s (remove-membership id (get-in old [:entries id])) (update :entries dissoc id)
                                       (update :ranges dissoc id) (update :positions dissoc id))) state remove)
        [state geometry packed row-ids]
        (reduce (fn [[s gs ps rs] id]
                  (let [item (or (get upsert id) (get-in old [:entries id :item]))
                        [s g? p r?] (prepare s id item)]
                    [(-> s (remove-membership id (get-in old [:entries id]))
                         (add-membership id (get-in s [:entries id])))
                     (cond-> gs g? (conj id)) (into ps p) (cond-> rs r? (conj id))]))
                [state #{} #{} #{}] affected)
        new-ids (set/difference (set (keys upsert)) (set (filter #(contains? (:entries old) %) (keys upsert))))
        topology? (or (seq remove) (seq new-ids) (and order (not= order (:order old))))
        next-order (if order (vec order)
                       (if topology? (into (vec (clojure.core/remove remove (:order old))) (sort-by pr-str new-ids)) (:order old)))
        _ (when (and topology? (or (not= (count next-order) (count (set next-order)))
                                   (not= (set next-order) (set (keys (:entries state))))))
            (throw (ex-info "Order must name every placement exactly once" {:reason :placement-order})))
        state (cond-> (assoc state :order next-order) topology? (assoc :positions (zipmap next-order (range))))
        [state shifted] (ranges state old affected topology?)
        ;; Update pack users by differences local to each touched placement.
        [users touched-keys] (reduce (fn [[users ks] id]
                                      (let [before (set (map :key (pack-entries (get-in old [:entries id]))))
                                            after (set (map :key (pack-entries (get-in state [:entries id]))))
                                            gone (set/difference before after) added (set/difference after before)]
                                        [(reduce #(update %1 %2 (fnil conj #{}) id)
                                                 (reduce #(update %1 %2 disj id) users gone) added)
                                         (into (into ks gone) added)])) [(:pack-users old) #{}] all-touched)
        dropped (set (filter #(empty? (get users %)) touched-keys))
        users (apply dissoc users dropped)
        state (assoc state :pack-users users)
        ;; Drop a region's retained packs only when no bucket is still used.
        ;; Each region has few bucket entries; no placement population scan.
        state (reduce (fn [s rkey]
                        (if (some #(contains? users [rkey %]) (keys (get-in s [:packs rkey :packs]))) s
                            (update s :packs dissoc rkey))) state (set (map first dropped)))]
    {:state state :affected affected :drop-packs dropped
     :reran {:geometry geometry :pack packed :rows (set/union row-ids shifted)}}))
