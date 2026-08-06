(ns app.client.substrate.connector-material
  "Pure citizenship and Contract-G truth for durable reference connectors.

   A connector material is the fail-closed merge of a relation-row projection
   and client-session dress. Resolved routes and meshes are disposable readers;
   none of their coordinates become relation truth."
  (:require [app.client.substrate.path-material :as path-material]))

(def schema-version 1)
(def algorithm-version :connector-route/v1)
(def boundary-epsilon 1.0e-9)
(def hit-slop-screen-px 0.0)
(def default-head-size-k 4.0)
(def legal-kinds #{:references})
(def legal-statuses #{:asserted :retracted})
(def legal-route-policies #{:straight :elbow/v1})
(def legal-heads #{:none :triangle})
(def machine-asserter-types #{:llm :agent :import :machine})
(def human-asserter-types #{:human})

(def legal-zoom-regimes path-material/legal-zoom-regimes)

(def geometry-declaration
  {:geometry/version 1
   :authority {:kind :reference-edge-route
               :source-id :connector/relation-id
               :source-revision :connector/composite-revision
               :algorithm-version algorithm-version
               :local-space :anchor-container-local-f64}
   :classify {:result #{:inside :boundary :outside}
              :fill-rule :stroke-plus-convex-heads
              :boundary-rule :boundary-is-hit}
   :coverage {:geometry-operator :direct-tessellation
              :operator-version algorithm-version
              :boundary-relation :aliased-triangle-edge-v1
              :reference-isocontour :not-applicable
              :visual-factors [:solid-color :material-alpha :effective-opacity]
              :tie-token :mathematical-boundary
              :quantization :path-regime-f32}
   :pick {:policy :centerline-width-plus-head-interior
          :boundary :hit
          :hit-slop {:metric :screen-px :radius hit-slop-screen-px}
          :owner :connector/edge-instance-id}
   :bounds {:math :resolved-route-plus-heads
            :paint :tessellated-mesh-bounds
            :pick :authority-plus-declared-slop}
   :derivations [{:kind :polyline-stroke-and-arrowhead-mesh
                  :source-revision :connector/composite-revision
                  :algorithm-version algorithm-version
                  :tolerance-lod :path-zoom-regime-fan-resolution
                  :normalization :path-shape-local-origin-scale
                  :precision :anchor-container-local-f32
                  :backend :webgpu-triangle-list
                  :regime :path-zoom-regime}]
   :regimes (mapv #(dissoc % :regime/id :fan-resolution)
                  legal-zoom-regimes)})

(def material-required-keys
  #{:connector/relation-id :connector/row-stamp :connector/dress-revision
    :connector/kind :connector/from :connector/to :connector/route
    :connector/heads :connector/label :connector/paint :connector/status
    :connector/provenance})

(def paint-required-keys
  #{:color :opacity :width :color-space :alpha-association})

(defn finite-number? [x]
  (and (number? x)
       #?(:clj (Double/isFinite (double x))
          :cljs (js/Number.isFinite x))))

(defn point? [point]
  (and (vector? point)
       (= 2 (count point))
       (every? finite-number? point)))

(defn present-string? [value]
  (and (string? value) (not (empty? value))))

(defn- exact-keys! [label value required]
  (when-not (map? value)
    (throw (ex-info (str label " must be a map") {:value value})))
  (let [actual (set (keys value))
        missing (seq (sort (remove actual required)))
        unknown (seq (sort (remove required actual)))]
    (when missing
      (throw (ex-info (str label " is missing required fields")
                      {:missing (vec missing)})))
    (when unknown
      (throw (ex-info (str label " has unknown fields")
                      {:unknown (vec unknown) :policy :reject}))))
  value)

(defn validate-binding! [binding]
  (when-not (map? binding)
    (throw (ex-info "Connector binding must be a map" {:binding binding})))
  (case (:bind binding)
    :node
    (do
      (exact-keys! "Connector node binding" binding #{:bind :target :anchor})
      (when (nil? (:target binding))
        (throw (ex-info "Connector node binding requires a target address"
                        {:binding binding})))
      (when-not (= :boundary (:anchor binding))
        (throw (ex-info "Connector v1 supports boundary node anchors only"
                        {:anchor (:anchor binding)}))))

    :point
    (do
      (exact-keys! "Connector point binding" binding #{:bind :position})
      (when-not (point? (:position binding))
        (throw (ex-info "Connector point binding requires a finite [x y]"
                        {:binding binding}))))

    (throw (ex-info "Connector binding kind is invalid"
                    {:bind (:bind binding) :legal #{:node :point}})))
  binding)

(defn validate-route! [route]
  (exact-keys! "Connector route" route #{:policy :waypoints})
  (when-not (contains? legal-route-policies (:policy route))
    (throw (ex-info "Connector route policy is invalid"
                    {:policy (:policy route) :legal legal-route-policies})))
  (when-not (and (vector? (:waypoints route))
                 (every? point? (:waypoints route)))
    (throw (ex-info "Connector waypoints must be finite [x y] values"
                    {:waypoints (:waypoints route)})))
  route)

(defn validate-heads! [heads]
  (exact-keys! "Connector heads" heads #{:from :to :size-k})
  (doseq [end [:from :to]]
    (when-not (contains? legal-heads (get heads end))
      (throw (ex-info "Connector arrowhead is invalid"
                      {:end end :head (get heads end) :legal legal-heads}))))
  (when-not (and (finite-number? (:size-k heads))
                 (pos? (:size-k heads)))
    (throw (ex-info "Connector arrowhead size-k must be positive"
                    {:size-k (:size-k heads)})))
  heads)

(defn validate-label! [label]
  (when label
    (exact-keys! "Connector label" label #{:text :at :offset})
    (when-not (present-string? (:text label))
      (throw (ex-info "Connector label text must be non-empty"
                      {:text (:text label)})))
    (when-not (and (finite-number? (:at label))
                   (<= 0.0 (:at label) 1.0))
      (throw (ex-info "Connector label :at must be in [0,1]"
                      {:at (:at label)})))
    (when-not (point? (:offset label))
      (throw (ex-info "Connector label offset must be a finite [dx dy]"
                      {:offset (:offset label)}))))
  label)

(defn validate-paint! [paint]
  (exact-keys! "Connector paint" paint paint-required-keys)
  (let [color (:color paint)]
    (when-not (and (vector? color) (= 4 (count color))
                   (every? #(and (finite-number? %) (<= 0.0 % 1.0)) color))
      (throw (ex-info "Connector paint color must be straight RGBA in [0,1]"
                      {:color color}))))
  (when-not (and (finite-number? (:opacity paint))
                 (<= 0.0 (:opacity paint) 1.0))
    (throw (ex-info "Connector paint opacity must be in [0,1]"
                    {:opacity (:opacity paint)})))
  (when-not (and (finite-number? (:width paint)) (pos? (:width paint)))
    (throw (ex-info "Connector paint width must be positive"
                    {:width (:width paint)})))
  (when-not (= :srgb (:color-space paint))
    (throw (ex-info "Connector paint requires tagged sRGB material color"
                    {:color-space (:color-space paint)})))
  (when-not (= :straight (:alpha-association paint))
    (throw (ex-info "Connector paint must be straight alpha at ingress"
                    {:alpha-association (:alpha-association paint)})))
  paint)

(defn validate-provenance! [provenance]
  (exact-keys! "Connector provenance" provenance #{:actor-id :asserter-type})
  (when-not (present-string? (:actor-id provenance))
    (throw (ex-info "Connector provenance actor-id must be a non-empty string"
                    {:actor-id (:actor-id provenance)})))
  (when-not (keyword? (:asserter-type provenance))
    (throw (ex-info "Connector provenance asserter-type must be a keyword"
                    {:asserter-type (:asserter-type provenance)})))
  provenance)

(defn validate-material!
  "Fail-closed validation over the merged relation-row projection and dress."
  [material]
  (exact-keys! "Connector material" material material-required-keys)
  (when (nil? (:connector/relation-id material))
    (throw (ex-info "Connector relation identity cannot be nil" {})))
  (when (nil? (:connector/row-stamp material))
    (throw (ex-info "Connector row stamp cannot be nil" {})))
  (when (nil? (:connector/dress-revision material))
    (throw (ex-info "Connector dress revision cannot be nil" {})))
  (when-not (contains? legal-kinds (:connector/kind material))
    (throw (ex-info "Connector relation kind is outside the day-one vocabulary"
                    {:kind (:connector/kind material) :legal legal-kinds})))
  (when-not (contains? legal-statuses (:connector/status material))
    (throw (ex-info "Connector relation status is invalid"
                    {:status (:connector/status material)
                     :legal legal-statuses})))
  (validate-binding! (:connector/from material))
  (validate-binding! (:connector/to material))
  (validate-route! (:connector/route material))
  (validate-heads! (:connector/heads material))
  (validate-label! (:connector/label material))
  (validate-paint! (:connector/paint material))
  (validate-provenance! (:connector/provenance material))
  material)

(defn canonical-material [material]
  (letfn [(canonical [value]
            (cond
              (map? value) (into (sorted-map)
                                 (map (fn [[k v]] [k (canonical v)])) value)
              (vector? value) (mapv canonical value)
              (set? value) (into (sorted-set) (map canonical) value)
              :else value))]
    (canonical (validate-material! material))))

(defn composite-revision [material]
  (let [material (validate-material! material)]
    [(:connector/row-stamp material)
     (:connector/dress-revision material)]))

(defn- revisioned-edit [material next-revision edit-id edit]
  (let [material (validate-material! material)]
    (when (= (:connector/dress-revision material) next-revision)
      (throw (ex-info "Connector dress edit requires a new revision"
                      {:revision next-revision :edit edit-id})))
    (validate-material!
     (-> (edit material)
         (assoc :connector/dress-revision next-revision)))))

(defn set-binding [material end binding next-revision]
  (when-not (#{:from :to} end)
    (throw (ex-info "Connector binding edit requires :from or :to" {:end end})))
  (validate-binding! binding)
  (revisioned-edit material next-revision :connector/set-binding
                   #(assoc % (if (= end :from) :connector/from :connector/to)
                           binding)))

(defn set-route [material policy next-revision]
  (revisioned-edit material next-revision :connector/set-route
                   #(assoc-in % [:connector/route :policy] policy)))

(defn set-waypoints [material waypoints next-revision]
  (revisioned-edit material next-revision :connector/set-waypoints
                   #(assoc-in % [:connector/route :waypoints] (vec waypoints))))

(defn set-heads [material heads next-revision]
  (revisioned-edit material next-revision :connector/set-heads
                   #(assoc % :connector/heads heads)))

(defn set-label [material label next-revision]
  (revisioned-edit material next-revision :connector/set-label
                   #(assoc % :connector/label label)))

(defn set-paint [material paint next-revision]
  (revisioned-edit material next-revision :connector/set-paint
                   #(assoc % :connector/paint paint)))

(def kind-colors
  {:references [0.65 0.65 0.65 0.90]})

(def provenance-tints
  {:human [1.08 0.96 0.72]
   :machine [0.72 0.92 1.10]
   :other [0.88 0.88 0.88]})

(defn provenance-class [asserter-type]
  (cond
    (contains? machine-asserter-types asserter-type) :machine
    (contains? human-asserter-types asserter-type) :human
    :else :other))

(defn- clamp01 [value] (min 1.0 (max 0.0 (double value))))

(defn projection-color [kind asserter-type]
  (let [[r g b a] (get kind-colors kind [0.60 0.60 0.60 0.90])
        [tr tg tb] (get provenance-tints (provenance-class asserter-type))]
    [(clamp01 (* r tr)) (clamp01 (* g tg)) (clamp01 (* b tb)) a]))

(defn paint-color [material]
  (let [{:keys [color opacity]} (:connector/paint (validate-material! material))
        [r g b a] color]
    [r g b (* a opacity)]))

(defn derivation-key
  [material resolved-anchor-tuple provider-identity zoom]
  (let [material (validate-material! material)]
    [:connector/derivation-v1
     (composite-revision material)
     resolved-anchor-tuple
     (get-in material [:connector/route :waypoints])
     algorithm-version
     (:regime/id (path-material/zoom-regime zoom))
     (when (:connector/label material) provider-identity)]))

(defn edge-instance-id? [value]
  (and (vector? value) (= 3 (count value)) (some? (first value))))

(defn- sq [value] (* value value))

(defn point-segment-distance [[px py] [ax ay] [bx by]]
  (let [dx (- bx ax)
        dy (- by ay)
        denominator (+ (sq dx) (sq dy))
        t (if (zero? denominator)
            0.0
            (min 1.0 (max 0.0
                          (/ (+ (* (- px ax) dx) (* (- py ay) dy))
                             denominator))))
        qx (+ ax (* t dx))
        qy (+ ay (* t dy))]
    (Math/sqrt (+ (sq (- px qx)) (sq (- py qy))))))

(defn- cross [[ax ay] [bx by] [cx cy]]
  (- (* (- bx ax) (- cy ay))
     (* (- by ay) (- cx ax))))

(defn triangle-classify [[a b c] point]
  (let [values [(cross a b point) (cross b c point) (cross c a point)]
        has-negative? (some #(< % (- boundary-epsilon)) values)
        has-positive? (some #(> % boundary-epsilon) values)]
    (cond
      (and has-negative? has-positive?) :outside
      (some #(<= (Math/abs %) boundary-epsilon) values) :boundary
      :else :inside)))

(defn classify
  "Tri-state CPU truth over a resolved route in anchor-container local space."
  [resolved-route point]
  (if-not (= :resolved (:status resolved-route))
    :outside
    (let [width (double (:stroke-width resolved-route))
          stroke-delta
          (when-let [segments (seq (partition 2 1 (:stroke-points resolved-route)))]
            (- (apply min (map (fn [[a b]]
                                 (point-segment-distance point a b))
                               segments))
               (/ width 2.0)))
          head-classes (map #(triangle-classify % point)
                            (keep identity [(:from-head resolved-route)
                                            (:to-head resolved-route)]))]
      (cond
        (some #{:inside} head-classes) :inside
        (some #{:boundary} head-classes) :boundary
        (and stroke-delta (< stroke-delta (- boundary-epsilon))) :inside
        (and stroke-delta (<= (Math/abs stroke-delta) boundary-epsilon)) :boundary
        :else :outside))))

(defn hit? [resolved-route point]
  (not= :outside (classify resolved-route point)))

(defn corpus-census
  "Count every edge-instance and every declared non-paint state. Exact route
   overlap is reported as groups; it never changes paint or pick order."
  [resolved-routes]
  (let [status-counts (frequencies (map :status resolved-routes))
        overlap-groups
        (->> resolved-routes
             (filter #(= :resolved (:status %)))
             (group-by #(select-keys % [:anchor-points :stroke-points]))
             vals
             (filter #(< 1 (count %)))
             count)]
    {:edge-instances (count resolved-routes)
     :resolved (get status-counts :resolved 0)
     :unresolved (get status-counts :unresolved 0)
     :degenerate (get status-counts :degenerate 0)
     :mixed-camera (get status-counts :mixed-camera 0)
     :overlap-groups overlap-groups}))

(def claimed-corpus-pressures
  #{:straight-arrow-label :elbow-waypoints-heads :provenance-overlap
    :cross-container :unresolved :multi-occurrence :legal-zoom-extremes})

(defn assert-corpus-coverage! [fixture-pressure->gates]
  (let [actual (set (keys fixture-pressure->gates))
        missing (seq (sort (remove actual claimed-corpus-pressures)))
        unconsumed (seq (sort (for [[pressure gates] fixture-pressure->gates
                                   :when (empty? gates)] pressure)))]
    (when (or missing unconsumed)
      (throw (ex-info "Connector corpus is incomplete or unconsumed"
                      {:missing (vec missing) :unconsumed (vec unconsumed)})))
    true))
