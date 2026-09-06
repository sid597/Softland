(ns app.client.path.component
  "The renderer's path-value input and its pure geometry level.

   Takes a path value, paint declarations, numeric parameters, and explicit
   geometry view inputs. Gives regions and CPU answers. Holds no state and
   executes no source recipe. Caller-side recipes live in construction.cljc.
   Evidence: component_test.clj, frame_test.clj and construction_test.clj.

   Folder map: README.md."
  (:require [app.client.engine.schema :as schema]
            [app.client.path.pack :as pack]
            [app.client.path.width :as width]
            [app.client.path.stroke :as stroke]
            [app.client.path.value :as v]))

(def schema-version 4)

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
    :width (named-validator :path/stroke-width #(or (schema/non-negative-number? %) (= :knot %) (vector? %)))
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

(def schema
  {:keys #{:path/material-id :path/revision :path/value :path/paint}
   :optional #{:path/parameters :path/snap?}
   :validators {:path/value (fn [value] (v/validate! value) true)
                :path/paint (fn [paint] (schema/check paint-schema paint) true)
                :path/parameters (named-validator :path/parameters
                                                  #(and (map? %) (every? schema/finite-number? (vals %))))
                :path/snap? (named-validator :path/snap boolean?)}})

(defn validate-component!
  "Path component → itself or a named validation error; no source-kind gate."
  [component]
  (schema/check schema component)
  component)

(defn stroke-defaults
  "Stroke declaration → the same with every optional field filled."
  [s]
  (merge {:tip :nib :width :knot :unit :local :cap :round :join :round
          :miter-limit 4.0 :align :center :dash nil :overlap :union :spacing 12.0}
         s))

(defn- expression-width?
  [width]
  (vector? width))

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
                   (let [{:keys [width-fn]} (width/width-function (assoc (or tool {}) :width width))]
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

(defn geometry-inputs
  "Component and view → this geometry level's complete input value.
   Color and identity are absent. Scale is present for device width or snap;
   fractional device pan only for snap. Nothing observes recipe reads."
  [component view]
  (let [paint (:path/paint component)
        device? (= :device (get-in paint [:stroke :unit]))
        snap? (boolean (:path/snap? component))
        scale (or (:scale view) 1.0)
        pan (or (:pan-fraction view)
                (mapv #(- % (Math/floor %)) (or (:pan view) [0.0 0.0])))]
    (when (and (or device? snap?) (not (schema/positive-number? scale)))
      (throw (ex-info "Positive projected scale required" {:error-type :path/scale})))
    {:path (:path/value component)
     :parameters (or (:path/parameters component) {})
     :fill (when-let [fill (:fill paint)] {:rule (:rule fill)})
     :stroke (when-let [s (:stroke paint)] (stroke-declaration s))
     :clip (:clip paint)
     :snap? snap?
     :view (cond-> {} (or device? snap?) (assoc :scale scale)
                       snap? (assoc :pan-fraction pan))}))

(defn geometry
  "Complete geometry input → {:path :regions :clip}. Ordinary computation,
   with no executor, observation, retained result, or mutable resource."
  [{:keys [path parameters fill stroke clip snap? view]}]
  (let [scale (or (:scale view) 1.0)
        [px py] (or (:pan-fraction view) [0.0 0.0])
        path (if snap?
               (pack/snap-path path (fn [[x y]] [(+ (* x scale) px) (+ (* y scale) py)])
                               (fn [[x y]] [(/ (- x px) scale) (/ (- y py) scale)])) path)
        regions (cond-> [] fill (conj {:kind :fill :path path :rule (:rule fill) :paint :fill}))
        regions (if-not stroke regions
                    (let [opts (stroke-options stroke parameters scale)]
                      (if (= :accumulate (:overlap stroke))
                        (into regions (map (fn [dab] {:kind :dab :path (:path dab) :rule :nonzero
                                                     :paint :stroke :dab (dissoc dab :path)}))
                              (:dabs (stroke/dabs path opts)))
                        (conj regions (merge (stroke/envelope path stroke opts)
                                             {:kind :stroke :rule :nonzero :paint :stroke})))))]
    {:path path :regions regions :clip (when clip (assoc clip :kind :clip))}))

(defn regions
  "Component and explicit view → its pure geometry result."
  [component view]
  (geometry (geometry-inputs component view)))

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
        answer (fn [r]
                 (cond
                   (nil? (:pack r)) :outside
                   (<= (pack/outline-distance (:pack r) point) (+ slop 1.0e-9)) :boundary
                   (pack/inside? (:pack r) (:rule r) x y) :inside
                   :else :outside))
        answers (map (fn [r]
                       (let [body (answer r) clip (if-let [clip (:clip r)] (answer clip) :inside)]
                         (cond (or (= :outside body) (= :outside clip)) :outside
                               (or (= :boundary body) (= :boundary clip)) :boundary
                               :else :inside))) regions)]
    (cond (some #{:inside} answers) :inside
          (some #{:boundary} answers) :boundary
          :else :outside)))

(defn painted-regions
  "Component and view → painted regions and clip with packs attached."
  [record view]
  (let [result (regions record view)
        clip (when-let [clip (:clip result)] (assoc clip :pack (region-pack clip)))]
    (mapv (fn [r] (cond-> (assoc r :pack (region-pack r)) clip (assoc :clip clip))) (:regions result))))

(defn classify
  "Record, point, optional slop (≥ 0, local units) → tri-state; a bad slop
   throws. Uses the unit geometry view unless one is given."
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

(defn component-content-key
  "Component → its complete content value, excluding identity and revision.
   No serialization or hash stands in for the value."
  [record]
  (dissoc record :path/material-id :path/revision))
