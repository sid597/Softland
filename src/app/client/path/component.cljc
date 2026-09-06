(ns app.client.path.component
  "Define the record a tool supplies, and turn it into regions.

   Input: a record (identity, tool numbers, a source, a paint declaration,
   optionally its own construction) and, for answers, a view and query
   points. Output: schema acceptance, the record's construction as data,
   the executor's run of it (a path, ordered regions, an optional clip,
   what was read), CPU classification and paint colours. No retained state.

   The record:
     {:path/material-id any  :path/revision any
      :path/tool   {:size :thinning :streamline :fit :taper-start :taper-end
                    :simulate-pressure? :width \"expression\" ...}   optional
      :path/source {:kind :pen :samples [[x y pressure? time?]]}
                 | {:kind :anchors :contours [{:closed? :anchors [{:id :p :in :out}]}]}
                 | {:kind :rect :x :y :w :h :r?}
      :path/paint  {:fill   nil | {:rule :nonzero|:even-odd :color [r g b a]}
                    :stroke nil | {:tip :nib|:ribbon, :width number|:knot|\"expr\",
                                   :unit :local|:device, :cap, :join, :miter-limit,
                                   :align :center|:inside|:outside, :dash [on off],
                                   :overlap :union|:accumulate, :spacing, :color}
                    :clip   nil | {:path <path value> :rule}}
      :path/snap?  bool                                              optional
      :path/construction {:steps [...] :return ...}                   optional}

   A tool's name selects nothing here: the source's kind names the
   capability that builds the path, the paint names the operations on it,
   and the default construction is data the record could have carried
   itself. Colour is never read by a construction, so a colour edit rebuilds
   no geometry.

   Folder map: README.md."
  (:require [app.client.engine.executor :as executor]
            [app.client.engine.schema :as schema]
            [app.client.path.pack :as pack]
            [app.client.path.source :as source]
            [app.client.path.stroke :as stroke]
            [app.client.path.value :as v]))

(def schema-version 3)

(defn- named-validator
  [error-type predicate]
  (fn [value]
    (when-not (predicate value)
      (throw (ex-info "Path schema rejected value" {:error-type error-type})))
    true))

(def legal-rules #{:nonzero :even-odd})
(def legal-tips #{:nib :ribbon})
(def legal-caps #{:round :butt :square})
(def legal-joins #{:round :miter :bevel})
(def legal-aligns #{:center :inside :outside})
(def legal-overlaps #{:union :accumulate})
(def legal-units #{:local :device})

(def fill-schema
  {:keys #{:rule :color}
   :validators {:rule (named-validator :path/fill-rule legal-rules)
                :color (named-validator :path/paint-color schema/valid-rgba?)}})

(def stroke-schema
  {:keys #{:color}
   :optional #{:tip :width :unit :cap :join :miter-limit :align :dash :overlap :spacing}
   :validators
   {:color (named-validator :path/paint-color schema/valid-rgba?)
    :tip (named-validator :path/stroke-tip legal-tips)
    :width (named-validator :path/stroke-width #(or (schema/non-negative-number? %) (= :knot %) (string? %)))
    :unit (named-validator :path/stroke-unit legal-units)
    :cap (named-validator :path/cap legal-caps)
    :join (named-validator :path/join legal-joins)
    :miter-limit (named-validator :path/miter-limit schema/positive-number?)
    :align (named-validator :path/align legal-aligns)
    :dash (named-validator :path/dash #(and (vector? %) (= 2 (count %)) (every? schema/non-negative-number? %)))
    :overlap (named-validator :path/overlap legal-overlaps)
    :spacing (named-validator :path/spacing schema/positive-number?)}})

(def clip-schema
  {:keys #{:path :rule}
   :validators {:path (named-validator :path/clip-path #(do (v/validate! %) true))
                :rule (named-validator :path/clip-rule legal-rules)}})

(def paint-schema
  {:keys #{}
   :optional #{:fill :stroke :clip}
   :validators {:fill (fn [value] (or (nil? value) (schema/check fill-schema value)) true)
                :stroke (fn [value] (or (nil? value) (schema/check stroke-schema value)) true)
                :clip (fn [value] (or (nil? value) (schema/check clip-schema value)) true)}})

(defn- sample? [s]
  (and (vector? s) (<= 2 (count s) 4) (every? schema/finite-number? s)))

(defn- anchor? [a]
  (and (map? a) (schema/point? (:p a))
       (or (nil? (:in a)) (schema/point? (:in a)))
       (or (nil? (:out a)) (schema/point? (:out a)))))

(def source-schemas
  {:pen {:keys #{:kind :samples}
         :validators {:kind (named-validator :path/source-kind #{:pen})
                      :samples [:vector-of sample? {}]}}
   :anchors {:keys #{:kind :contours}
             :validators {:kind (named-validator :path/source-kind #{:anchors})
                          :contours [:vector-of {:keys #{:closed? :anchors}
                                                 :validators {:closed? (named-validator :path/closed boolean?)
                                                              :anchors [:vector-of anchor? {:min 2}]}} {}]}}
   :rect {:keys #{:kind :x :y :w :h}
          :optional #{:r}
          :validators {:kind (named-validator :path/source-kind #{:rect})
                       :x (named-validator :path/rect schema/finite-number?)
                       :y (named-validator :path/rect schema/finite-number?)
                       :w (named-validator :path/rect schema/non-negative-number?)
                       :h (named-validator :path/rect schema/non-negative-number?)
                       :r (named-validator :path/rect schema/non-negative-number?)}}})

(defn- source-valid?
  [record]
  (let [s (:path/source record)]
    (if-let [spec (get source-schemas (:kind s))]
      (do (schema/check spec s) true)
      false)))

(def schema
  {:keys #{:path/material-id :path/revision :path/source :path/paint}
   :optional #{:path/tool :path/snap? :path/construction}
   :validators
   {:path/material-id (named-validator :path/material-id some?)
    :path/revision (named-validator :path/revision some?)
    :path/paint paint-schema
    :path/tool (named-validator :path/tool map?)
    :path/snap? (named-validator :path/snap boolean?)
    :path/construction (named-validator :path/construction #(and (map? %) (vector? (:steps %))))}
   :form-validators
   [{:valid? source-valid? :error-type :path/source-kind}]})

(defn validate-component!
  "Record → the same map, or a named exception. Structural acceptance;
   whether a construction runs is the executor's report."
  [record]
  (schema/check schema record))

;; ---- declarations with their defaults ----

(defn stroke-defaults
  "Stroke declaration → the same with every optional field filled."
  [s]
  (merge {:tip :nib :width :knot :unit :local :cap :round :join :round
          :miter-limit 4.0 :align :center :dash nil :overlap :union :spacing 12.0}
         s))

(defn- expression-width?
  [width]
  (and (string? width) (not= "knot" width)))

(defn stroke-options
  "Stroke declaration, tool, device scale → the tracer's options: tip,
   flattening tolerance, the width rule (a constant, the knots, or a
   function of pressure) in local units, and the dash."
  [s tool scale]
  (let [s (stroke-defaults s)
        device? (= :device (:unit s))
        k (if device? (/ 1.0 (max scale 1.0e-9)) 1.0)
        width (:width s)
        width-fn (when (expression-width? width)
                   (let [{:keys [width-fn]} (source/width-function (assoc (or tool {}) :width width))]
                     (fn [p sf] (* k (width-fn p sf 0.0)))))]
    (cond-> {:tip (:tip s)
             :tolerance (if device? (/ 0.25 (max scale 1.0e-9)) 0.1)
             :fallback-width 4.0
             :knot-scale k
             :dash (:dash s)
             :dash-phase 0.0
             :spacing (:spacing s)}
      (number? width) (assoc :width-local (* k width))
      width-fn (assoc :width-fn width-fn))))

(defn- stroke-declaration
  "Stroke declaration → its geometry-only fields, so a region's key never
   sees the colour."
  [s]
  (select-keys (stroke-defaults s) [:tip :width :unit :cap :join :miter-limit :align :dash :overlap :spacing]))

;; ---- capabilities ----

(defn- region
  [kind path rule paint-key extra]
  (merge {:kind kind :path path :rule rule :paint paint-key} extra))

(def capabilities
  "Op → function of the resolved bindings. Every op is a pure derivation."
  {:path/source
   (fn [{:keys [source tool]}]
     (let [{:keys [path meta]} (source/build source tool)]
       (assoc path :meta meta)))
   :path/snap
   (fn [{:keys [path scale pan]}]
     (let [scale (or scale 1.0) [px py] (or pan [0.0 0.0])]
       (pack/snap-path path
                       (fn [[x y]] [(+ (* x scale) px) (+ (* y scale) py)])
                       (fn [[dx dy]] [(/ (- dx px) scale) (/ (- dy py) scale)]))))
   :path/fill-region
   (fn [{:keys [path rule]}]
     [(region :fill (dissoc path :meta) (or rule :nonzero) :fill {})])
   :path/envelope
   (fn [{:keys [path tool stroke scale]}]
     (let [s (stroke-defaults stroke)
           result (stroke/envelope path s (stroke-options s tool (or scale 1.0)))]
       [(region :stroke (:path result) :nonzero :stroke
                {:polylines (:polylines result) :pieces (:pieces result)
                 :arcs (:arcs result) :open (:open result) :closed (:closed result)})]))
   :path/dabs
   (fn [{:keys [path tool stroke scale]}]
     (let [s (stroke-defaults stroke)
           result (stroke/dabs path (stroke-options s tool (or scale 1.0)))]
       (mapv (fn [dab]
               (region :dab (:path dab) :nonzero :stroke {:dab (dissoc dab :path)}))
             (:dabs result))))
   :path/clip-region
   (fn [{:keys [path rule]}]
     (region :clip path (or rule :nonzero) nil {}))})

(defn default-construction
  "Record → the construction its declarations imply, as data: the source
   builds the path; snapping, when declared, moves it to the device grid
   (reading the view's scale and pan); a fill makes a fill region; a stroke
   makes the skin as a union or the dabs as an accumulation, reading the
   view's scale only when the width is in device pixels; a clip makes a
   clip region. Colours are not bound anywhere."
  [record]
  (let [{:keys [fill stroke clip]} (:path/paint record)
        s (when stroke (stroke-defaults stroke))
        device? (= :device (:unit s))
        stroke-step (fn [op]
                      (cond-> {:out "stroke" :op op :path "path" :tool "tool" :stroke "paint.stroke.geometry"}
                        device? (assoc :scale "view.scale")))]
    {:steps (cond-> [{:out "path" :op :path/source :source "source" :tool "tool"}]
              (:path/snap? record) (conj {:out "path" :op :path/snap :path "path" :scale "view.scale" :pan "view.pan"})
              fill (conj {:out "fill" :op :path/fill-region :path "path" :rule "paint.fill.rule"})
              (and s (= :union (:overlap s))) (conj (stroke-step :path/envelope))
              (and s (= :accumulate (:overlap s))) (conj (stroke-step :path/dabs))
              clip (conj {:out "clip" :op :path/clip-region :path "paint.clip.path" :rule "paint.clip.rule"}))
     :return (cond-> {:path "path"
                      :regions (cond-> [] fill (conj "fill") s (conj "stroke"))}
               clip (assoc :clip "clip"))}))

(defn construction
  "Record → its own construction or the default one."
  [record]
  (or (:path/construction record) (default-construction record)))

(defn scope
  "Record and view ({:scale device px per local unit, :pan [x y]}) → the
   executor's roots. The stroke's geometry fields sit under
   paint.stroke.geometry so a construction can bind them without the
   colour; the tool's name is not in the scope at all."
  [record view]
  (let [p (:path/paint record)]
    {"tool" (dissoc (or (:path/tool record) {}) :name)
     "source" (:path/source record)
     "paint" (cond-> (or p {})
               (:stroke p) (assoc-in [:stroke :geometry] (stroke-declaration (:stroke p))))
     "identity" {:id (:path/material-id record) :revision (:path/revision record)}
     "view" (merge {:scale 1.0 :pan [0.0 0.0]} view)}))

(defn run
  "Record and view → the executor's result with :path, :regions (flat, in
   paint order) and :clip lifted out of the return. A run that did not
   complete has :ok? false and no regions; the log says why."
  [record view]
  (let [result (executor/run (construction record) (scope record view) capabilities)
        ret (:return result)]
    (assoc result
           :path (dissoc (:path ret) :meta)
           :meta (:meta (:path ret))
           :regions (if (:ok? result) (vec (apply concat (:regions ret))) [])
           :clip (:clip ret))))

(defn rerun?
  "Record, view, the :reads of an earlier run → true when any read value
   changed, so the run must repeat."
  [record view reads]
  (not= reads (executor/reread (scope record view) reads)))

;; ---- CPU answers ----

(def query-tolerance 0.01)

(defn region-pack
  "Region → its pack at the query tolerance, for membership and distance."
  [region]
  (:pack (pack/pack-region (:path region) query-tolerance {})))

(defn classify-regions
  "Painted regions (with :pack) and point, slop → :inside | :boundary |
   :outside: winding under each region's rule, a boundary band of slop
   around every outline. The same definition the filler estimates."
  [regions point slop]
  (let [[x y] point
        inside? (some (fn [r] (and (:pack r) (pack/inside? (:pack r) (:rule r) x y))) regions)
        distance (reduce min ##Inf (map (fn [r] (if (:pack r) (pack/outline-distance (:pack r) point) ##Inf)) regions))]
    (cond
      (<= distance (+ slop 1.0e-9)) :boundary
      inside? :inside
      :else :outside)))

(defn painted-regions
  "Record and view → the run's painted regions with packs attached."
  [record view]
  (mapv (fn [r] (assoc r :pack (region-pack r))) (:regions (run record view))))

(defn classify
  "Record, point, optional slop (≥ 0, local units) → tri-state; a bad slop
   throws. Runs the construction at the unit view unless one is given."
  ([record point] (classify record point 0.0))
  ([record point slop] (classify record point slop {}))
  ([record point slop view]
   (when-not (schema/non-negative-number? slop)
     (throw (ex-info "Path hit slop must be finite local units"
                     {:error-type :path/hit-slop :path [:slop-local] :value slop})))
   (classify-regions (painted-regions record view) point slop)))

(defn hit?
  "Record and point → true for inside or boundary."
  [record point]
  (not= :outside (classify record point)))

(defn boundary-distance
  "Record and point → the distance to the nearest painted outline."
  [record point]
  (reduce min ##Inf (map (fn [r] (pack/outline-distance (:pack r) point)) (painted-regions record {}))))

(defn region-color
  "Record and region → the straight RGBA the region paints with: the fill's
   colour for a fill region, the stroke's for a stroke or dab."
  [record region]
  (get-in (:path/paint record) [(:paint region) :color] [0.0 0.0 0.0 1.0]))

(defn canonical-component
  "Nested value → recursively sorted maps/sets with vector order kept."
  [component]
  (letfn [(canonical [value]
            (cond
              (map? value) (into (sorted-map) (map (fn [[k child]] [k (canonical child)])) value)
              (vector? value) (mapv canonical value)
              (set? value) (into (sorted-set) (map canonical) value)
              :else value))]
    (canonical component)))

(defn component-content-hash
  "Record → a stable content key excluding identity and revision."
  [record]
  [:path/content-v3
   (pr-str (dissoc (canonical-component record) :path/material-id :path/revision))])
