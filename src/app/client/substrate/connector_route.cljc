(ns app.client.substrate.connector-route
  "Pure attachment, routing, label-layout, and bounded connector cache.

   Cache state is explicit state-in/state-out. The only ambient value is the
   product pick mirror, which holds disposable route projections and a provider
   for the current effective-transform map; it never enters scene-store data."
  (:require [app.client.substrate.connector-material :as connector-material]
            [app.client.substrate.path-material :as path-material]
            [app.client.substrate.path-tessellation :as path-tessellation]
            [app.client.workspace.containers :as containers]
            [app.client.workspace.text-layout :as tl]))

(def algorithm-version connector-material/algorithm-version)
(def ^:private epsilon 1.0e-10)
(def label-font-size 14.0)
(def label-line-height 16.8)

(defonce ^:private !live-route-cache
  (atom {:routes {} :edges {} :targets-by-address {} :effective {}
         :route-effective {}}))
(defonce ^:private !effective-provider (atom nil))

(defn set-live-effective-provider! [provider]
  (reset! !effective-provider provider)
  nil)

(defn reset-live-route-cache! []
  (reset! !live-route-cache
          {:routes {} :edges {} :targets-by-address {} :effective {}
           :route-effective {}}))

(defn live-route-cache [] @!live-route-cache)

(defn- add [[ax ay] [bx by]] [(+ ax bx) (+ ay by)])
(defn- sub [[ax ay] [bx by]] [(- ax bx) (- ay by)])
(defn- scale [[x y] scalar] [(* x scalar) (* y scalar)])
(defn- dot [[ax ay] [bx by]] (+ (* ax bx) (* ay by)))
(defn- cross [[ax ay] [bx by]] (- (* ax by) (* ay bx)))
(defn- magnitude [[x y]] (Math/sqrt (+ (* x x) (* y y))))
(defn- distance [a b] (magnitude (sub b a)))

(defn- unit [vector]
  (let [length (magnitude vector)]
    (when (> length epsilon) (scale vector (/ 1.0 length)))))

(defn- point-equal? [a b]
  (and a b (<= (distance a b) epsilon)))

(defn dedupe-consecutive [points]
  (reduce (fn [result point]
            (if (point-equal? (peek result) point) result (conj result point)))
          [] points))

(defn provider-identity [font-assets]
  (select-keys (:layout-provider font-assets)
               [:face-id :face-revision :shaper-id :shaper-version
                :features :variations :axes :fallback-chain :upem :metrics]))

(defn- point-occurrence [binding]
  [{:vi :connector/point
    :world-position (:position binding)
    :camera :world}])

(defn binding-occurrences [binding targets-by-address]
  (case (:bind binding)
    :point (point-occurrence binding)
    :node (vec (get targets-by-address (:target binding) []))
    :region-object [{:vi [:region-object (:region binding) (:object binding)]}]
    []))

(defn- select-occurrences [occurrences vi]
  (if (some? vi)
    (filterv #(= vi (:vi %)) occurrences)
    occurrences))

(defn expand-edge-instances
  "Expand raw connector ops over every visible from×to occurrence pair.
   Unresolved well-formed edges retain one non-painting instance for census."
  [connector-ops targets-by-address]
  (into []
        (mapcat
         (fn [op]
           (let [material (connector-material/validate-material!
                           (:connector/material op))
                 froms (select-occurrences
                        (binding-occurrences (:connector/from material)
                                             targets-by-address)
                        (:connector/from-vi op))
                 tos (select-occurrences
                      (binding-occurrences (:connector/to material)
                                           targets-by-address)
                      (:connector/to-vi op))
                 relation-id (:connector/relation-id material)
                 pairs (if (and (seq froms) (seq tos))
                         (for [from froms to tos] [from to])
                         [[(first froms) (first tos)]])]
             (mapv (fn [[from to]]
                     (let [from-vi (or (:vi from) :connector/unresolved-from)
                           to-vi (or (:vi to) :connector/unresolved-to)
                           instance-id (or (:connector/edge-instance-id op)
                                           [relation-id from-vi to-vi])]
                       (assoc op
                              :connector/edge-instance-id instance-id
                              :connector/from-vi from-vi
                              :connector/to-vi to-vi
                              :connector/from-target from
                              :connector/to-target to)))
                   pairs)))
         connector-ops)))

(defn- bounds-corners [{:keys [x y w h]}]
  [[x y] [(+ x w) y] [(+ x w) (+ y h)] [x (+ y h)]])

(defn- polygon-center [points]
  (scale (reduce add [0.0 0.0] points) (/ 1.0 (count points))))

(defn- cross-space-point [target-effective anchor-effective point]
  (containers/inverse-point anchor-effective
                            (containers/forward-point target-effective point)))

(defn- resolved-binding
  [binding occurrence effective anchor-container region-anchor-resolver]
  (let [anchor-effective (get effective anchor-container)]
    (cond
      (nil? anchor-effective) nil

      (= :region-object (:bind binding))
      (when-let [resolved (and region-anchor-resolver
                               (region-anchor-resolver binding anchor-container
                                                       effective))]
        (merge {:kind :point
                :vi [:region-object (:region binding) (:object binding)]
                :container anchor-container
                :quad nil}
               resolved))

      (= :point (:bind binding))
      {:kind :point
       :vi :connector/point
       :container anchor-container
       ;; Point bindings are declared world coordinates. A screen-space anchor
       ;; therefore becomes the same mixed-camera skip as a world/screen node
       ;; pair instead of silently reinterpreting the point as screen pixels.
       :camera 0
       :center (containers/inverse-point anchor-effective (:position binding))
       :quad nil}

      (nil? occurrence) nil

      :else
      (when-let [target-effective (get effective (:container occurrence))]
        (let [quad (mapv (partial cross-space-point target-effective
                                  anchor-effective)
                         (bounds-corners (:bounds occurrence)))]
          {:kind :node
           :vi (:vi occurrence)
           :container (:container occurrence)
           :camera (:flags target-effective)
           :center (polygon-center quad)
           :quad quad})))))

(defn- ray-segment-intersection [origin direction a b]
  (let [edge (sub b a)
        denominator (cross direction edge)]
    (when (> (Math/abs denominator) epsilon)
      (let [ao (sub a origin)
            ray-t (/ (cross ao edge) denominator)
            edge-t (/ (cross ao direction) denominator)]
        (when (and (>= ray-t (- epsilon))
                   (<= (- epsilon) edge-t (+ 1.0 epsilon)))
          {:t ray-t :point (add origin (scale direction ray-t))})))))

(defn- clip-from-center [resolved toward]
  (let [center (:center resolved)]
    (if (or (= :point (:kind resolved)) (nil? (:quad resolved)))
      center
      (let [direction (sub toward center)
            hits (keep (fn [[a b]]
                         (ray-segment-intersection center direction a b))
                       (map vector (:quad resolved)
                            (concat (rest (:quad resolved))
                                    [(first (:quad resolved))])))]
        (or (:point (first (sort-by :t hits))) center)))))

(defn elbow-points
  "Pinned :elbow/v1 Z route over unclipped endpoint centers."
  [[ax ay :as from] [bx by :as to]]
  (let [dx (Math/abs (- bx ax))
        dy (Math/abs (- by ay))]
    (cond
      (or (<= dx epsilon) (<= dy epsilon)) [from to]
      (>= dx dy) (let [mid (/ (+ ax bx) 2.0)]
                   [from [mid ay] [mid by] to])
      :else (let [mid (/ (+ ay by) 2.0)]
              [from [ax mid] [bx mid] to]))))

(defn route-centerline [material from-center to-center]
  (let [waypoints (get-in material [:connector/route :waypoints])
        policy (get-in material [:connector/route :policy])]
    (dedupe-consecutive
     (if (seq waypoints)
       (into [from-center] (conj (vec waypoints) to-center))
       (case policy
         :straight [from-center to-center]
         :elbow/v1 (elbow-points from-center to-center))))))

(defn- trim-polyline-start [points trim-length]
  (loop [points (vec points) remaining (double trim-length)]
    (if (or (<= remaining epsilon) (< (count points) 2))
      points
      (let [a (first points) b (second points) segment-length (distance a b)]
        (cond
          (<= segment-length epsilon) (recur (subvec points 1) remaining)
          (< remaining segment-length)
          (assoc points 0 (add a (scale (unit (sub b a)) remaining)))
          :else (recur (subvec points 1) (- remaining segment-length)))))))

(defn- trim-polyline-end [points trim-length]
  (vec (reverse (trim-polyline-start (vec (reverse points)) trim-length))))

(defn- head-triangle [tip inward length width]
  (when-let [direction (unit inward)]
    (let [base-center (add tip (scale direction length))
          normal [(- (second direction)) (first direction)]
          half-width (/ width 2.0)]
      [tip
       (add base-center (scale normal half-width))
       (sub base-center (scale normal half-width))])))

(defn- route-status [status edge-instance-id]
  {:status status :edge-instance-id edge-instance-id
   :anchor-points [] :stroke-points [] :triangles [] :vertices []
   :vertex-count 0})

(defn resolve-route-geometry
  "Resolve attachment and route semantics without tessellation or text layout."
  ([edge effective] (resolve-route-geometry edge effective {}))
  ([edge effective {:keys [region-anchor-resolver]}]
   (let [material (connector-material/validate-material!
                   (:connector/material edge))
         edge-instance-id (:connector/edge-instance-id edge)
         anchor-container (:container edge)
         anchor-camera (get-in effective [anchor-container :flags])
         from-binding (:connector/from material)
         to-binding (:connector/to material)
         from (resolved-binding from-binding
                                (:connector/from-target edge)
                                effective anchor-container
                                region-anchor-resolver)
         to (resolved-binding to-binding
                              (:connector/to-target edge)
                              effective anchor-container
                              region-anchor-resolver)
         region-anchor? #(= :region-object (:bind %))
         anchor-projections (+ (if (region-anchor? from-binding) 1 0)
                               (if (region-anchor? to-binding) 1 0))
         base-extra {:material material :edge edge
                     :anchor-projections anchor-projections}]
     (cond
       (or (nil? from) (nil? to))
       (merge (route-status
               (if (or (and (region-anchor? from-binding) (nil? from))
                       (and (region-anchor? to-binding) (nil? to)))
                 :region-anchor-absent
                 :unresolved)
               edge-instance-id)
              base-extra)

       (or (not= (:camera from) (:camera to))
           (not= anchor-camera (:camera from)))
       (merge (route-status :mixed-camera edge-instance-id) base-extra)

       :else
       (let [centers (route-centerline material (:center from) (:center to))]
         (if (< (count centers) 2)
           (merge (route-status :degenerate edge-instance-id) base-extra)
           (let [from-tip (clip-from-center from (second centers))
                 to-tip (clip-from-center to (nth centers (- (count centers) 2)))
                 anchors (dedupe-consecutive
                          (assoc (assoc centers 0 from-tip)
                                 (dec (count centers)) to-tip))]
             (if (or (< (count anchors) 2)
                     (<= (reduce + (map (fn [[a b]] (distance a b))
                                        (partition 2 1 anchors)))
                         epsilon))
               (merge (route-status :degenerate edge-instance-id) base-extra)
               (let [stroke-width (get-in material [:connector/paint :width])
                     size-k (get-in material [:connector/heads :size-k])
                     head-length (* size-k stroke-width)
                     ;; The shared path library owns round caps.  Its cap reaches
                     ;; half a stroke-width beyond the terminal centerline point,
                     ;; so leave that radius outside the node/head instead of
                     ;; letting the cap bleed into either painted surface.
                     cap-radius (/ stroke-width 2.0)
                     head-width (* 1.25 head-length)
                     from-head? (= :triangle (get-in material [:connector/heads :from]))
                     to-head? (= :triangle (get-in material [:connector/heads :to]))
                     from-head (when from-head?
                                 (head-triangle from-tip
                                                (sub (second anchors) from-tip)
                                                head-length head-width))
                     to-head (when to-head?
                               (head-triangle to-tip
                                              (sub (nth anchors (- (count anchors) 2))
                                                   to-tip)
                                              head-length head-width))
                     stroke (-> anchors
                                (trim-polyline-start
                                 (+ cap-radius
                                    (if from-head? head-length 0.0)))
                                (trim-polyline-end
                                 (+ cap-radius
                                    (if to-head? head-length 0.0))))]
                 (if (< (count stroke) 2)
                   (merge (route-status :degenerate edge-instance-id) base-extra)
                   (merge
                    {:status :resolved
                     :edge-instance-id edge-instance-id
                     :material material
                     :edge edge
                     :from from :to to
                     :anchor-points anchors
                     :resolved-anchor-tuple
                     [(:center from) (:center to) from-tip to-tip]
                     :stroke-points stroke
                     :stroke-width stroke-width
                     :from-head from-head
                     :to-head to-head
                     :anchor-clamped
                     (boolean (or (:anchor-clamped from)
                                  (:anchor-clamped to)))}
                    base-extra)))))))))))

(defn- route-path-material [resolved]
  (let [material (:material resolved)
        id (:edge-instance-id resolved)]
    {:path/material-id [:connector/stroke id]
     :path/revision (connector-material/composite-revision material)
     :path/kind :ink
     :path/geometry
     {:knots (mapv (fn [index point]
                     {:knot/id [id index] :position point :pressure 1.0
                      :gesture-time :explicitly-absent :source-event-ids []})
                   (range) (:stroke-points resolved))
      :base-width (:stroke-width resolved)
      :cap :round :join :round}
     ;; The shared path grammar owns stroke width in :path/geometry. Connector
     ;; paint carries it beside color as dress, so strip only that connector-
     ;; specific field at the library boundary.
     :path/paint (dissoc (:connector/paint material) :width)
     :path/provenance {:actor (get-in material [:connector/provenance :actor-id])
                       :act :connector-route-derivation :parents []}}))

(defn- head-triangles [resolved]
  (into []
        (mapcat (fn [triangle]
                  (when triangle (path-tessellation/ear-clip triangle))))
        [(:from-head resolved) (:to-head resolved)]))

(defn derive-mesh [resolved zoom]
  (if-not (= :resolved (:status resolved))
    resolved
    (let [stroke-mesh (path-tessellation/tessellate
                       (route-path-material resolved) zoom)
          triangles (into (:triangles stroke-mesh) (head-triangles resolved))
          vertices (into [] cat triangles)]
      (assoc resolved
             :triangles triangles
             :vertices vertices
             :vertex-count (count vertices)
             :mesh-bytes (path-tessellation/mesh-bytes {:vertices vertices})
             :coverage :aliased-v1
             :regime (:regime/id (path-material/zoom-regime zoom))))))

(defn- point-on-polyline [points t]
  (let [segments (mapv (fn [[a b]] {:a a :b b :length (distance a b)})
                       (partition 2 1 points))
        total (reduce + (map :length segments))
        target (* (min 1.0 (max 0.0 t)) total)]
    (loop [remaining segments traversed 0.0]
      (if-let [{:keys [a b length]} (first remaining)]
        (if (or (<= length epsilon) (<= target (+ traversed length)))
          (let [local-t (if (<= length epsilon) 0.0
                            (/ (- target traversed) length))]
            {:point (add a (scale (sub b a) local-t))
             :tangent (or (unit (sub b a)) [1.0 0.0])})
          (recur (next remaining) (+ traversed length)))
        {:point (or (peek points) [0.0 0.0]) :tangent [1.0 0.0]}))))

(defn layout-label
  "The connector's single Contract-T owner. The live provider is mandatory at
   product call sites; a provider identity is returned for cache receipts."
  [resolved font-assets]
  (when-let [label (get-in resolved [:material :connector/label])]
    (let [provider (:layout-provider font-assets)
          identity (provider-identity font-assets)
          _ (when-not provider
              (throw (ex-info
                      "Connector label layout requires the live text provider"
                      {:edge-instance-id (:edge-instance-id resolved)})))
          {:keys [point tangent]} (point-on-polyline (:anchor-points resolved)
                                                     (:at label))
          normal [(- (second tangent)) (first tangent)]
          [along perpendicular] (:offset label)
          origin (-> point
                     (add (scale tangent along))
                     (add (scale normal perpendicular)))
          result (tl/layout {:text (:text label)
                             :source-id [:connector/label
                                         (:edge-instance-id resolved)]
                             :source-revision
                             (connector-material/composite-revision
                              (:material resolved))
                             :source-lines [(:text label)]
                             :provider provider
                             :font-size label-font-size
                             :char-advance (tl/legacy-char-advance
                                            label-font-size 0.56)
                             :line-height label-line-height
                             :origin origin
                             :wrap-policy :none
                             :zoom 1.0})
          line (first (:lines result))
          [x y] (:baseline line)
          color (connector-material/paint-color (:material resolved))]
      {:edge-instance-id (:edge-instance-id resolved)
       :owner-vi (get-in resolved [:edge :owner-vi])
       :provider-identity identity
       :layout-result result
       :paint-op {:text (:text label)
                  :x x :y y :size label-font-size
                  :r (nth color 0) :g (nth color 1)
                  :b (nth color 2) :a (nth color 3)
                  :container-idx (get-in resolved [:edge :container-idx])
                  :layout-result result
                  :layout-line-id (:line/id line)
                  :paint-source-range (:source-range line)
                  :layout/surface :connector-label}})))

(defn- relation-row-stamp [row]
  (or (:event-id row)
      [(:status-changed-at-ms row) (:request-id row)]
      [(:relation-status row) (:first-asserted-at-ms row)]))

(defn project-edge-row
  "Pure RelationEdgeRow projection. Dress defaults are session-local and do
   not invent a durable revision or relation kind."
  [row]
  (let [kind (:relation-kind row)
        asserter-type (:asserter-type row)
        color (connector-material/projection-color kind asserter-type)]
    (connector-material/validate-material!
     {:connector/relation-id (:relation-id row)
      :connector/row-stamp (relation-row-stamp row)
      :connector/dress-revision 0
      :connector/kind kind
      :connector/from {:bind :node
                       :target (get-in row [:from :target-id])
                       :anchor :boundary}
      :connector/to {:bind :node
                     :target (get-in row [:to :target-id])
                     :anchor :boundary}
      :connector/route {:policy :straight :waypoints []}
      :connector/heads {:from :none :to :triangle
                        :size-k connector-material/default-head-size-k}
      :connector/label nil
      :connector/paint {:color color :opacity 1.0 :width 2.0
                        :color-space :srgb :alpha-association :straight}
      :connector/status (:relation-status row)
      :connector/provenance {:actor-id (:asserter-actor-id row)
                             :asserter-type asserter-type}})))

(defn empty-cache []
  {:connector-cache/version 1
   :edge-set-token []
   :entries {}
   :bound-edges-by-container {}
   :bound-edges-by-region {}
   :last-effective {}
   :last-region-doors {}
   :last-regime nil
   :last-provider-identity nil
   :route-resolutions 0
   :anchor-projections 0
   :mesh-derivations 0
   :per-edge {}})

(defn effective-value-diff [before after]
  (into #{}
        (filter (fn [container]
                  (not= (get before container) (get after container))))
        (into #{} (concat (keys before) (keys after)))))

(defn- edge-bound-containers [edge]
  (into #{(:container edge)}
        (keep :container)
        [(:connector/from-target edge) (:connector/to-target edge)]))

(defn bound-edges-by-container [edges]
  (reduce (fn [index edge]
            (reduce (fn [index container]
                      (update index container (fnil conj #{})
                              (:connector/edge-instance-id edge)))
                    index
                    (edge-bound-containers edge)))
          {}
          edges))

(defn- edge-bound-regions [edge]
  (into #{}
        (keep (fn [binding]
                (when (= :region-object (:bind binding)) (:region binding))))
        [(get-in edge [:connector/material :connector/from])
         (get-in edge [:connector/material :connector/to])]))

(defn bound-edges-by-region [edges]
  (reduce (fn [index edge]
            (reduce (fn [index region]
                      (update index region (fnil conj #{})
                              (:connector/edge-instance-id edge)))
                    index
                    (edge-bound-regions edge)))
          {}
          edges))

(defn- affected-by-containers [bound-index changed-containers]
  (into #{} (mapcat #(get bound-index % #{})) changed-containers))

(defn- material-token [edge]
  (connector-material/canonical-material (:connector/material edge)))

(defn- anchor-input-token [edge]
  [(:container edge)
   (select-keys (:connector/from-target edge)
                [:vi :container :bounds :world-position :camera])
   (select-keys (:connector/to-target edge)
                [:vi :container :bounds :world-position :camera])])

(defn- update-counter [state edge-id counter]
  (-> state
      (update counter (fnil inc 0))
      (update-in [:per-edge edge-id counter] (fnil inc 0))))

(defn derive-route-set
  "Bounded one-current-entry-per-edge-instance cache. Effective-transform
   value diffs route only changed containers through the bound-edge index."
  ([state connector-ops targets-by-address effective zoom font-assets]
   (derive-route-set state connector-ops targets-by-address effective zoom
                     font-assets {}))
  ([state connector-ops targets-by-address effective zoom font-assets
    {:keys [region-anchor-resolver region-doors]}]
  (let [state (merge (empty-cache) (or state {}))
        edges (expand-edge-instances connector-ops targets-by-address)
        edge-set-token (mapv :connector/edge-instance-id edges)
        set-changed? (not= edge-set-token (:edge-set-token state))
        next-bound-index (bound-edges-by-container edges)
        next-region-index (bound-edges-by-region edges)
        bound-index (if (= next-bound-index
                           (:bound-edges-by-container state))
                      (:bound-edges-by-container state)
                      next-bound-index)
        state (if set-changed?
                (assoc state :entries {}
                       :edge-set-token edge-set-token
                       :bound-edges-by-container bound-index
                       :bound-edges-by-region next-region-index
                       :per-edge
                       (select-keys (:per-edge state) edge-set-token))
                (assoc state :bound-edges-by-container bound-index
                             :bound-edges-by-region next-region-index))
        changed-containers (effective-value-diff (:last-effective state)
                                                 effective)
        changed-regions (effective-value-diff (:last-region-doors state)
                                              (or region-doors {}))
        regime (:regime/id (path-material/zoom-regime zoom))
        provider-id (provider-identity font-assets)
        affected (into (affected-by-containers bound-index changed-containers)
                       (affected-by-containers next-region-index changed-regions))
        edge-by-id (into {} (map (juxt :connector/edge-instance-id identity)) edges)
        affected
        (reduce (fn [ids edge]
                  (let [edge-id (:connector/edge-instance-id edge)
                        prior (get-in state [:entries edge-id])]
                    (cond-> ids
                      (nil? prior) (conj edge-id)
                      (not= (:material-token prior) (material-token edge))
                      (conj edge-id)
                      (not= (:anchor-input-token prior)
                            (anchor-input-token edge))
                      (conj edge-id)
                      (not= regime (:regime prior)) (conj edge-id)
                      (and (get-in edge [:connector/material :connector/label])
                           (not= provider-id (:provider-identity prior)))
                      (conj edge-id))))
                affected edges)
        [state resolved-this-frame derived-this-frame]
        (reduce
         (fn [[state resolved-ids derived-ids] edge-id]
           (let [edge (get edge-by-id edge-id)
                 geometry (resolve-route-geometry
                           edge effective
                           {:region-anchor-resolver region-anchor-resolver})
                 state (update-counter state edge-id :route-resolutions)
                 state (if (pos? (:anchor-projections geometry 0))
                         (-> state
                             (update :anchor-projections +
                                     (:anchor-projections geometry))
                             (update-in [:per-edge edge-id :anchor-projections]
                                        (fnil + 0)
                                        (:anchor-projections geometry)))
                         state)
                 key (when (= :resolved (:status geometry))
                       (connector-material/derivation-key
                        (:material geometry)
                        (:resolved-anchor-tuple geometry)
                        provider-id zoom))
                 prior (get-in state [:entries edge-id])
                 derive? (and (= :resolved (:status geometry))
                              (or (nil? prior)
                                  (not= key (:key prior))
                                  (not= (:material-token prior)
                                        (material-token edge))))
                 route (cond
                         derive? (derive-mesh geometry zoom)
                         (= :resolved (:status geometry)) (:route prior)
                         :else geometry)
                 label (when (= :resolved (:status route))
                         (if derive?
                           (layout-label route font-assets)
                           (:label prior)))
                 entry {:key key :route route :label label
                        :material-token (material-token edge)
                        :anchor-input-token (anchor-input-token edge)
                        :regime regime :provider-identity provider-id}
                 state (assoc-in state [:entries edge-id] entry)
                 state (if derive?
                         (update-counter state edge-id :mesh-derivations)
                         state)]
             [state
              (conj resolved-ids edge-id)
              (cond-> derived-ids derive? (conj edge-id))]))
         [state [] []]
         (sort-by pr-str affected))
        entries (mapv #(get-in state [:entries (:connector/edge-instance-id %)]) edges)
        routes (mapv :route entries)
        labels (vec (keep :label entries))
        state (assoc state :last-effective effective
                           :last-region-doors (or region-doors {})
                           :last-regime regime
                           :last-provider-identity provider-id)
        frame-receipt {:edge-instances (count edges)
                       :cache-size (count (:entries state))
                       :changed-containers changed-containers
                       :changed-regions changed-regions
                       :affected-edges affected
                       :resolved-this-frame resolved-this-frame
                       :route-resolutions (count resolved-this-frame)
                       :mesh-derivations (count derived-this-frame)
                       :derived-this-frame derived-this-frame
                       :anchor-projections
                       (reduce + 0 (map #(get-in state
                                                [:entries % :route
                                                 :anchor-projections] 0)
                                        resolved-this-frame))}]
    (reset! !live-route-cache
            {:routes (into {} (map (juxt :edge-instance-id identity)) routes)
             :edges edge-by-id
             :targets-by-address targets-by-address
             :effective effective
             :region-anchor-resolver region-anchor-resolver
             :route-effective
             (into {}
                   (map (fn [edge]
                          [(:connector/edge-instance-id edge)
                           (select-keys effective
                                        (edge-bound-containers edge))]))
                   edges)})
    {:state state :edges edges :routes routes :labels labels
     :mesh-set-key (mapv (fn [entry]
                           [(:key entry)
                            (get-in entry [:route :mesh-bytes])])
                         entries)
     :frame-receipt frame-receipt
     :census (connector-material/corpus-census routes)})))

(defn live-route
  "Return the current route for pick. If endpoint transforms changed since the
   frame snapshot, lazily re-resolve geometry without touching the rect tree."
  [edge-instance-id]
  (let [{:keys [routes edges effective route-effective region-anchor-resolver]}
        @!live-route-cache
        current-effective (if-let [provider @!effective-provider]
                            (provider)
                            effective)
        edge (get edges edge-instance-id)
        route (get routes edge-instance-id)
        current-edge-effective
        (when edge
          (select-keys current-effective (edge-bound-containers edge)))
        cached-edge-effective
        (get route-effective edge-instance-id
             (when edge
               (select-keys effective (edge-bound-containers edge))))]
    (when edge
      (if (= current-edge-effective cached-edge-effective)
        route
        (let [next-route (resolve-route-geometry
                          edge current-effective
                          {:region-anchor-resolver region-anchor-resolver})]
          (swap! !live-route-cache
                 (fn [cache]
                   (-> cache
                       (assoc-in [:routes edge-instance-id] next-route)
                       (assoc-in [:route-effective edge-instance-id]
                                 current-edge-effective))))
          next-route)))))

(defn live-hit? [edge-instance-id local-point]
  (when-let [route (live-route edge-instance-id)]
    (connector-material/hit? route local-point)))
