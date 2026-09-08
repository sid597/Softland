(ns app.client.view.run
  "Run a view on the executor: the records it stands in front of, through its
   tools in the order it names. Takes a store value, a view record and the
   painting size in texels. Gives the frame (every tool's run, the painting,
   the placements) and a hit for a point. Holds nothing; timing belongs to the
   caller. Evidence: run_test.clj.

   Wiring is data: a tool's :inputs name the producer record and output they
   take, and this runner hands each tool the producer's retained value with
   its subject. The scope a tool runs under is the view: its subject as
   :where, its pins by name, the store's records as :store, and the painting
   declaration derived from its zoom and origin."
  (:require [app.client.engine.executor :as executor]
            [app.client.view.store :as store]
            [app.client.view.table :as table]))

(defn painting-declaration
  "View and texel size → the surface the view paints into: local units to
   texels through the view's zoom and origin."
  [view width height]
  (let [z (:zoom view) [ox oy] (:origin view)]
    {:surface/id (str (:id view) "/painting") :width width :height height
     :domain {:kind :plane :map [z 0 0 z (- (* z ox)) (- (* z oy))]}
     :color :linear-premultiplied-rgba :filter :nearest :initial [0 0 0 0]}))

(defn tool-record
  "Tool record → executor record: its parameters and declared inputs are its roots."
  [tool]
  {:program (:program tool)
   :roots (cond-> {:tool (:tool tool)} (:inputs tool) (assoc :inputs (:inputs tool)))})

(defn retained
  "A run and an output name → that output with its subject beside it."
  [run out]
  (assoc (get-in run [:results out]) :subject (get-in run [:subjects out])))

(defn scope
  "Store, view and texel size → the roots every tool of the view runs under."
  [store view [width height]]
  (merge {:store (store/records store)
          :where (:subject view)
          :painting (painting-declaration view width height)}
         (into {} (for [[name id] (:pins view)] [name (store/record store id)]))))

(defn run-tool
  "Store, base scope, the runs so far and a tool id → that tool's executor run,
   its declared inputs resolved from the earlier runs."
  [store base runs id]
  (let [tool (store/record store id)
        from (for [[name {:keys [from]}] (:inputs tool) :when from] [name from])
        inputs (into {} (for [[name {:keys [record output]}] from]
                          [name (retained (get runs record) output)]))
        records (into {} (for [[_ {:keys [record]}] from]
                           [record (tool-record (store/record store record))]))]
    (executor/run (tool-record tool)
                  (cond-> base (seq inputs) (assoc :inputs inputs))
                  table/table
                  {:records records})))

(defn frame
  "Store, view and texel size → the view's tools run in order, the painting
   the last completed painter gave, the placements the layout gave, and each
   tool's status. An optional :clock (a function giving milliseconds) times
   each tool; without one nothing is timed."
  ([store view size] (frame store view size {}))
  ([store view size {:keys [clock]}]
   (let [base (scope store view size)
         {:keys [runs ms]} (reduce (fn [{:keys [runs] :as acc} id]
                                     (let [t0 (when clock (clock))
                                           r (run-tool store base runs id)]
                                       (cond-> (assoc-in acc [:runs id] r)
                                         clock (assoc-in [:ms id] (- (clock) t0)))))
                                   {:runs {} :ms {}} (:tools view))
         complete (fn [id] (let [r (get runs id)] (when (= :complete (:status r)) (:results r))))]
     {:runs runs
      :ms ms
      :painting (some #(:painting (complete %)) (reverse (:tools view)))
      :placements (some #(:placements (complete %)) (:tools view))
      :caret (some #(:at (:caret (complete %))) (:tools view))
      :statuses (into {} (for [id (:tools view)] [id (select-keys (get runs id) [:status :reason :error])]))})))

(defn hit
  "Store, view, a frame and a local point → the view's hit tool run at that point."
  [store view frame point]
  (run-tool store (assoc (scope store view [1 1]) :point point) (:runs frame) (:hit-tool view)))
