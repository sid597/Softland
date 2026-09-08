(ns app.client.view.run
  "Run a view on the executor: the records it stands in front of, through its
   tools in the order it names. Takes a store value, a view record and the
   view's texel size. Gives the frame (every tool instance's run, the runs'
   paintings at their places, the placements, the caret) and a hit for a
   point. Holds nothing; timing belongs to the caller. Evidence: run_test.clj.

   Wiring is data: a tool's :inputs name the producer record and output they
   take, and this runner hands each tool the producer's retained value with
   its subject. A tool marked :per :run is instantiated once per run the
   view's subject names, keyed [tool run]; its painting is a surface the
   runner sizes to the layout's box, so a run's paint copies only its own
   box, and the view composites the runs' surfaces at their places. The
   scope a tool runs under is the view: its subject as :where, its pins by
   name, the store's records as :store, the run as :run.

   Between frames each instance resumes the continuation it kept, without
   its history: a run whose keys grew runs its new glyphs alone; a resume the
   executor refuses (a root changed, an item changed) is a fresh run."
  (:require [app.client.engine.executor :as executor]
            [app.client.view.store :as store]
            [app.client.view.table :as table]))

(defn painting-declaration
  "View, a surface id and a local box {:x :y :w :h} → the surface that box
   paints into: local units to texels through the view's zoom, the box's
   origin at texel zero."
  [view id {:keys [x y w h]}]
  (let [z (:zoom view)]
    {:surface/id id :width (max 1 (int (Math/ceil (* z w)))) :height (max 1 (int (Math/ceil (* z h))))
     :domain {:kind :plane :map [z 0 0 z (- (* z x)) (- (* z y))]}
     :color :linear-premultiplied-rgba :filter :nearest :initial [0 0 0 0]}))

(defn texel->local
  "View and a texel of the view's canvas → the local point at that texel's centre."
  [view x y]
  (let [z (:zoom view) [ox oy] (:origin view)]
    [(+ ox (/ (+ x 0.5) z)) (+ oy (/ (+ y 0.5) z))]))

(defn local->texel
  "View and a local point → the view's texel coordinates."
  [view [x y]]
  (let [z (:zoom view) [ox oy] (:origin view)]
    [(* z (- x ox)) (* z (- y oy))]))

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
  "Store and view → the roots every tool of the view runs under."
  [store view]
  (merge {:store (store/records store) :where (:subject view)}
         (into {} (for [[name id] (:pins view)] [name (store/record store id)]))))

(defn records
  "Store and view → every tool record the view names, by id: what the
   executor's input check is allowed to know."
  [store view]
  (into {} (for [id (conj (:tools view) (:hit-tool view)) :when id]
             [id (tool-record (store/record store id))])))

(defn- resolve-inputs
  "A tool, the instance's run id and the runs so far → its declared inputs,
   each the producer instance's retained output; a per-view producer serves
   every run."
  [tool run-id runs]
  (into {} (for [[name {:keys [from]}] (:inputs tool) :when from
                 :let [{:keys [record output]} from
                       producer (or (get runs [record run-id]) (get runs [record nil]))]]
             [name (retained producer output)])))

(defn run-tool
  "Store, scope, the view's records, the capability table, the runs so far,
   a tool id, the run id this instance is for and the instance's run from
   the previous frame → the executor run, resumed from the previous
   continuation when the executor admits it, fresh otherwise."
  [store base records table runs id run-id previous]
  (let [tool (store/record store id)
        record (tool-record tool)
        inputs (resolve-inputs tool run-id runs)
        scope (cond-> base (seq inputs) (assoc :inputs inputs))
        resumed (when-let [held (:continuation previous)]
                  (executor/resume (assoc held :history []) table
                                   {:record record :scope scope :records records}))]
    (if (= :complete (:status resumed))
      (assoc resumed :resumed? true)
      (assoc (executor/run record scope table {:records records}) :resumed? false))))

(defn- complete [runs key] (let [r (get runs key)] (when (= :complete (:status r)) (:results r))))

(defn frame
  "Store and view → the frame: the view's per-view tools run in order, then
   each per-run tool for each run the subject names, in order; the runs'
   paintings with their boxes, in run order; placements and caret; each
   instance's status. Options: :previous, the last frame, whose instances
   each instance resumes from; :clock, a function giving milliseconds,
   times each instance; :table, the capability table (the fonts it was
   built with), the box-font table when absent."
  ([store view] (frame store view {}))
  ([store view {:keys [clock previous table] :or {table table/table}}]
   (let [base (scope store view)
         known (records store view)
         tools (map #(store/record store %) (:tools view))
         per-view (map :id (remove #(= :run (:per %)) tools))
         per-run (map :id (filter #(= :run (:per %)) tools))
         run-one (fn [acc id run-id sc]
                   (let [t0 (when clock (clock))
                         r (run-tool store sc known table (:runs acc) id run-id (get-in previous [:runs [id run-id]]))]
                     (cond-> (assoc-in acc [:runs [id run-id]] r)
                       clock (assoc-in [:ms [id run-id]] (- (clock) t0)))))
         acc (reduce (fn [acc id] (run-one acc id nil base)) {:runs {} :ms {}} per-view)
         run-ids (some #(get-in (complete (:runs acc) [% nil]) [:runs :ids]) per-view)
         acc (reduce (fn [acc run-id]
                       (let [run (store/record store run-id)]
                         (reduce (fn [acc id]
                                   (let [box (some #(get-in (complete (:runs acc) [% run-id]) [:outline :box]) per-run)
                                         sc (cond-> (assoc base :run run)
                                              box (assoc :painting (painting-declaration view (str (:id view) "/" run-id "/painting") box)))]
                                     (run-one acc id run-id sc)))
                                 acc per-run)))
                     acc run-ids)
         runs (:runs acc)
         last-painting (fn [run-id] (some #(:painting (complete runs [% run-id])) (reverse per-run)))]
     {:runs runs
      :ms (:ms acc)
      :order (vec run-ids)
      :resumed (set (for [[key r] runs :when (:resumed? r)] key))
      :paintings (vec (for [run-id run-ids
                            :let [box (some #(get-in (complete runs [% run-id]) [:outline :box]) per-run)
                                  surface (last-painting run-id)]
                            :when (and box surface)]
                        {:run run-id :box box :surface surface}))
      :placements (into {} (for [run-id run-ids
                                 :let [p (some #(get-in (complete runs [% run-id]) [:placements :items]) per-run)]
                                 :when p]
                             [run-id p]))
      :caret (some (fn [run-id] (some #(get-in (complete runs [% run-id]) [:caret :at]) per-run)) run-ids)
      :statuses (into {} (for [[key r] runs] [key (select-keys r [:status :reason :error])]))})))

(defn hit
  "Store, view, a frame and a local point → the first run whose hit tool
   answers at that point, or nil. Options: :table as for frame."
  ([store view frame point] (hit store view frame point {}))
  ([store view frame point {:keys [table] :or {table table/table}}]
   (let [base (assoc (scope store view) :point point)
         known (records store view)]
     (some (fn [run-id]
             (let [r (run-tool store (assoc base :run (store/record store run-id)) known table (:runs frame) (:hit-tool view) run-id nil)]
               (when (= :complete (:status r)) (get-in r [:results :hit]))))
           (:order frame)))))
