(ns app.client.path.push
  "Apply named placement changes to the physical path renderer.
   Takes a renderer system, caller diffs and view values; gives work receipts.
   The system owns current placements, atlas and row storage. No frame walk
   discovers edits. Evidence: frame_test.clj and harness/path_push.cljs."
  (:require [clojure.set :as set]
            [app.client.engine.buffer-pool :as pool]
            [app.client.engine.coverage :as coverage]
            [app.client.path.component :as component]
            [app.client.path.placements :as placements]))

(defn- rows [atlas entry]
  (let [clip (when-let [[r p] (:clip entry)] {:slot (coverage/slot atlas (:key p)) :rule (:rule r)})]
    (vec (for [[region p] (:regions entry) :when p
               :let [slot (coverage/slot atlas (:key p)) color (component/region-color (get-in entry [:item :path/material]) region)]
               [x0 y0 x1 y1] (get-in p [:cover :rects])]
           {:rect [x0 y0 (- x1 x0) (- y1 y0)] :slot slot :color color :rule (:rule region)
            :index (:group-index entry) :clip clip}))))

(defn apply!
  "System, named diff, optional replacement view → measured preparation.
   Slot relocation and row-range shifts propagate to their users; only dirty
   placements build/write rows. Counters are at the direct write edge."
  [system diff view]
  (let [old @(:!placements system)
        {:keys [state affected drop-packs reran]} (placements/change old diff view)
        atlas (:atlas system)
        removed (coverage/remove! atlas drop-packs)
        moved-users (reduce into #{} (map #(get-in state [:pack-users %]) (:moved removed)))
        dirty (set/union (:rows reran) moved-users)
        ;; Only packs reached by changed placements are offered to the atlas.
        _ (doseq [id affected p (placements/pack-entries (get-in state [:entries id]))]
            (coverage/insert! atlas (:key p) (:pack p)))
        [state writes row-writes] (reduce (fn [[s calls n] id]
                                           (let [entry (get-in s [:entries id]) row-values (rows atlas entry)
                                                 [start _] (get-in s [:ranges id])
                                                 written (pool/write-range! (:pool system) start row-values)]
                                             [(assoc-in s [:entries id :rows] row-values)
                                              (+ calls (:ranges written)) (+ n (:rows written))]))
                                         [state 0 0] (sort-by #(get-in state [:ranges % 0]) dirty))
        ;; Unchanged entries retain their current rows. Preparation may update
        ;; identity/input fields without changing a row's data.
        state (reduce (fn [s id]
                        (if (contains? dirty id) s
                            (assoc-in s [:entries id :rows] (get-in old [:entries id :rows])))) state affected)
        _ (pool/set-count! (:pool system) (:row-count state))
        flushed (coverage/flush! atlas)
        _ (when (:regrown? flushed) ((:refresh-bind-group! system)))
        reran (assoc reran :rows dirty)]
    (reset! (:!placements system) state)
    {:changed? (boolean (or (seq dirty) (seq (:geometry reran)) (seq (:pack reran)) (seq (:dropped removed))))
     :reran reran :visited affected
     :derivations (count (:geometry reran)) :packs (count (:pack reran))
     :instances (:row-count state) :instance-writes row-writes :row-ranges-written writes :row-comparisons 0
     :atlas (merge (coverage/stats atlas) flushed) :relocated (:moved removed)}))

(defn push! [system diff] (apply! system diff nil))
(defn frame! [system view] (apply! system {} view))
