(ns app.client.path.tessellation
  "Turns a path into triangles. Ink expands to segment quads with round caps
   and joins; shapes bridge their holes and ear-clip. Deterministic and pure.
   Takes: a path component and a zoom; or a content-hash-keyed cache plus many
   components.
   Gives: a mesh (flat vertices, counts, coverage, cache key); untouched
   components return their old mesh by identity.
   Holds nothing; the cache is a value the caller owns."
  (:require [app.client.engine.schema :as schema]
            [app.client.path.component :as path-component]))

(def legal-zoom-lods
  [{:lod/id :legal-min
    :zoom {:min 0.01 :max 0.1}
    :fan-resolution 4}
   {:lod/id :engine-default
    :zoom {:min 0.1 :max 8.0}
    :fan-resolution 8}
   {:lod/id :legal-max
    :zoom {:min 8.0 :max 1000.0}
    :fan-resolution 16}])

(defn zoom-lod [zoom]
  (when-not (and (schema/finite-number? zoom) (<= 0.01 zoom 1000.0))
    (throw (ex-info "Path zoom is outside the zoom range"
                    {:zoom zoom :legal [0.01 1000.0]})))
  (cond
    (< zoom 0.1) (first legal-zoom-lods)
    (<= zoom 8.0) (second legal-zoom-lods)
    :else (nth legal-zoom-lods 2)))

(def algorithm-version :path-tessellation-v1)
(def ^:private epsilon 1.0e-10)

(defn component-cache-key
  ([component zoom]
   (component-cache-key component algorithm-version zoom))
  ([component algorithm zoom]
   (let [canonical (path-component/canonical-component component)
         geometry (into (sorted-map)
                        (select-keys canonical [:path/kind :path/geometry]))]
     [geometry algorithm (:lod/id (zoom-lod zoom))])))

(defn- component-points [component]
  (case (:path/kind component)
    :ink (mapv :position (get-in component [:path/geometry :stroke-points]))
    :shape (into [] (mapcat :points)
                 (get-in component [:path/geometry :contours]))))

(defn- shape-normalization [component]
  (let [points (component-points component)
        xs (map first points)
        ys (map second points)
        min-x (apply min xs)
        min-y (apply min ys)
        max-x (apply max xs)
        max-y (apply max ys)
        scale (max 1.0 (- max-x min-x) (- max-y min-y))]
    {:origin [min-x min-y] :scale scale}))

(defn- normalize-point [{:keys [origin scale]} [x y]]
  [(/ (- x (first origin)) scale)
   (/ (- y (second origin)) scale)])

(defn- denormalize-point [{:keys [origin scale]} [x y]]
  [(+ (first origin) (* x scale))
   (+ (second origin) (* y scale))])

(defn- add [[ax ay] [bx by]] [(+ ax bx) (+ ay by)])
(defn- sub [[ax ay] [bx by]] [(- ax bx) (- ay by)])
(defn- scale [[x y] factor] [(* x factor) (* y factor)])
(defn- cross [[ax ay] [bx by]] (- (* ax by) (* ay bx)))
(defn- length [[x y]] (Math/sqrt (+ (* x x) (* y y))))
(defn- distance [a b] (length (sub a b)))

(defn- unit [vector]
  (let [magnitude (length vector)]
    (when (<= magnitude epsilon)
      (throw (ex-info "Path contains a zero-length line segment"
                      {:vector vector})))
    (scale vector (/ 1.0 magnitude))))

(defn- left-normal [direction]
  [(- (second direction)) (first direction)])

(defn- triangle [a b c]
  (when (> (Math/abs (cross (sub b a) (sub c a))) epsilon)
    [a b c]))

(defn- radians [vector]
  (Math/atan2 (second vector) (first vector)))

(defn- arc-delta [start end direction]
  (let [tau (* 2.0 Math/PI)]
    (case direction
      :ccw (loop [delta (- end start)]
             (if (neg? delta) (recur (+ delta tau)) delta))
      :cw (loop [delta (- end start)]
            (if (pos? delta) (recur (- delta tau)) delta)))))

(defn- fan-triangles
  [center radius start-angle end-angle direction resolution]
  (let [delta (arc-delta start-angle end-angle direction)
        steps (max 1 (long (Math/ceil (* resolution
                                         (/ (Math/abs delta) Math/PI)))))
        points (mapv (fn [index]
                       (let [angle (+ start-angle (* delta (/ index steps)))]
                         (add center [(* radius (Math/cos angle))
                                      (* radius (Math/sin angle))])))
                     (range (inc steps)))]
    (into []
          (keep (fn [[left right]] (triangle center left right)))
          (partition 2 1 points))))

(defn- normalized-ink [component normalization]
  (let [scale-factor (:scale normalization)]
    (update-in component [:path/geometry :stroke-points]
               (fn [stroke-points]
                 (mapv #(-> %
                            (update :position
                                    (partial normalize-point normalization))
                            (update :width / scale-factor))
                       stroke-points)))))

(defn- stroke-triangles-normalized [component zoom]
  (let [geometry (:path/geometry component)
        stroke-points (:stroke-points geometry)
        resolution (:fan-resolution (zoom-lod zoom))
        segments
        (mapv
         (fn [[left right]]
           (let [a (:position left)
                 b (:position right)
                 direction (unit (sub b a))
                 normal (left-normal direction)
                 left-radius (/ (:width left) 2.0)
                 right-radius (/ (:width right) 2.0)
                 a-left (add a (scale normal left-radius))
                 a-right (sub a (scale normal left-radius))
                 b-left (add b (scale normal right-radius))
                 b-right (sub b (scale normal right-radius))]
             {:a a :b b :direction direction :normal normal
              :left-radius left-radius :right-radius right-radius
              :triangles (into []
                               (keep identity)
                               [(triangle a-left a-right b-left)
                                (triangle b-left a-right b-right)])}))
         (partition 2 1 stroke-points))
        first-segment (first segments)
        last-segment (peek segments)
        start-normal (:normal first-segment)
        end-normal (:normal last-segment)
        cap-triangles
        (into
         (fan-triangles (:a first-segment)
                        (:left-radius first-segment)
                        (radians start-normal)
                        (radians (scale start-normal -1.0))
                        :ccw resolution)
         (fan-triangles (:b last-segment)
                        (:right-radius last-segment)
                        (radians (scale end-normal -1.0))
                        (radians end-normal)
                        :ccw resolution))
        join-triangles
        (into []
              (mapcat
               (fn [[incoming outgoing stroke-point]]
                 (let [turn (cross (:direction incoming)
                                   (:direction outgoing))
                       radius (/ (:width stroke-point) 2.0)
                       center (:position stroke-point)]
                   (cond
                     (> turn epsilon)
                     (fan-triangles center radius
                                    (radians (scale (:normal incoming) -1.0))
                                    (radians (scale (:normal outgoing) -1.0))
                                    :ccw resolution)

                     (< turn (- epsilon))
                     (fan-triangles center radius
                                    (radians (:normal incoming))
                                    (radians (:normal outgoing))
                                    :cw resolution)

                     :else [])))
               (map vector segments (rest segments) (rest (butlast stroke-points)))))]
    (into [] cat [(mapcat :triangles segments)
                  cap-triangles
                  join-triangles])))

(defn stroke-triangles
  "Direct-to-triangles stroke expansion. Returned coordinates are the
   component's original local f64 values; normalization is internal and is
  recorded separately on the mesh."
  [component zoom]
  (let [normalization (shape-normalization component)
        ink (normalized-ink component normalization)]
    (mapv (fn [triangle]
            (mapv (partial denormalize-point normalization) triangle))
          (stroke-triangles-normalized ink zoom))))

(defn- signed-area [points]
  (/ (reduce + 0.0
             (map (fn [[[ax ay] [bx by]]]
                    (- (* ax by) (* ay bx)))
                  (map vector points (concat (rest points) [(first points)]))))
     2.0))

(defn- orient [points desired]
  (let [ccw? (pos? (signed-area points))]
    (if (= ccw? (= desired :ccw)) (vec points) (vec (reverse points)))))

(defn- same-point? [a b]
  (<= (distance a b) epsilon))

(defn- orientation [a b c]
  (cross (sub b a) (sub c a)))

(defn- proper-segment-intersection? [a b c d]
  (let [ab-c (orientation a b c)
        ab-d (orientation a b d)
        cd-a (orientation c d a)
        cd-b (orientation c d b)]
    (and (< (* ab-c ab-d) (- epsilon))
         (< (* cd-a cd-b) (- epsilon)))))

(defn- polygon-edges [points]
  (map vector points (concat (rest points) [(first points)])))

(defn- bridge-visible? [outer holes h v]
  (let [blocked?
        (some (fn [[a b]]
                (and (not (some #(same-point? % a) [h v]))
                     (not (some #(same-point? % b) [h v]))
                     (proper-segment-intersection? h v a b)))
              (concat (polygon-edges outer)
                      (mapcat polygon-edges holes)))
        midpoint (scale (add h v) 0.5)]
    (and (not blocked?)
         (= :inside (path-component/contour-classify outer midpoint))
         (not-any? #(= :inside (path-component/contour-classify % midpoint))
                   holes))))

(defn- rotate-from [points index]
  (into (subvec points index) (subvec points 0 index)))

(defn- bridge-hole [outer remaining-holes hole]
  (let [hole (orient hole :cw)
        hole-index
        (first (sort-by (fn [index]
                          (let [[x y] (nth hole index)] [(- x) y]))
                        (range (count hole))))
        h (nth hole hole-index)
        candidates
        (->> (range (count outer))
             (filter #(bridge-visible? outer remaining-holes h (nth outer %)))
             (sort-by #(distance h (nth outer %))))
        outer-index (first candidates)]
    (when-not outer-index
      (throw (ex-info "No visible deterministic bridge exists for path hole"
                      {:hole-point h :outer outer :hole hole})))
    (let [v (nth outer outer-index)
          hole-walk (rotate-from hole hole-index)]
      (vec (concat (subvec outer 0 (inc outer-index))
                   hole-walk
                   [h v]
                   (subvec outer (inc outer-index)))))))

(defn bridge-holes
  "Bridge explicit CW holes into one CCW simple walk before ear clipping."
  [outer holes]
  (loop [polygon (orient outer :ccw)
         remaining (mapv #(orient % :cw)
                         (sort-by (fn [hole]
                                    (let [[x y] (apply max-key first hole)]
                                      [(- x) y]))
                                  holes))]
    (if-let [hole (first remaining)]
      (recur (bridge-hole polygon remaining hole) (subvec remaining 1))
      polygon)))

(defn- point-in-triangle? [point a b c]
  (let [ab (orientation a b point)
        bc (orientation b c point)
        ca (orientation c a point)]
    (and (>= ab (- epsilon))
         (>= bc (- epsilon))
         (>= ca (- epsilon)))))

(defn- diagonal-clear? [polygon prev-index next-index]
  (let [prev (nth polygon prev-index)
        next (nth polygon next-index)
        n (count polygon)]
    (not-any?
     (fn [edge-index]
       (let [edge-next (mod (inc edge-index) n)]
         (when-not (or (= edge-index prev-index)
                       (= edge-next prev-index)
                       (= edge-index next-index)
                       (= edge-next next-index))
           (proper-segment-intersection?
            prev next (nth polygon edge-index) (nth polygon edge-next)))))
     (range n))))

(defn- ear-index [polygon]
  (let [n (count polygon)]
    (some
     (fn [index]
       (let [prev-index (mod (dec index) n)
             next-index (mod (inc index) n)
             prev (nth polygon prev-index)
             point (nth polygon index)
             next (nth polygon next-index)
             convex? (> (orientation prev point next) epsilon)
             contains?
             (some (fn [other-index]
                     (when-not (or (= other-index prev-index)
                                   (= other-index index)
                                   (= other-index next-index)
                                   (same-point? (nth polygon other-index) prev)
                                   (same-point? (nth polygon other-index) point)
                                   (same-point? (nth polygon other-index) next))
                       (point-in-triangle? (nth polygon other-index)
                                           prev point next)))
                   (range n))]
         (when (and convex? (not contains?)
                    (diagonal-clear? polygon prev-index next-index))
           index)))
     (range n))))

(defn- remove-index [values index]
  (into (subvec values 0 index) (subvec values (inc index))))

(defn ear-clip
  "Deterministic ear clipping over a CCW bridged polygon."
  [points]
  (loop [polygon (orient (vec points) :ccw)
         triangles []
         fuel (* 4 (count points))]
    (cond
      (= 3 (count polygon))
      (if-let [last-triangle (apply triangle polygon)]
        (conj triangles last-triangle)
        triangles)

      (or (< (count polygon) 3) (zero? fuel))
      (throw (ex-info "Ear clipping could not consume the bridged polygon"
                      {:remaining polygon :triangle-count (count triangles)}))

      :else
      (if-let [index (ear-index polygon)]
        (let [n (count polygon)
              tri [(nth polygon (mod (dec index) n))
                   (nth polygon index)
                   (nth polygon (mod (inc index) n))]]
          (recur (remove-index polygon index)
                 (conj triangles tri)
                 (dec fuel)))
        ;; A bridge walk can contain an exactly collinear duplicate. Removing
        ;; only a zero-area vertex is deterministic and preserves the bridge.
        (if-let [collinear
                 (some (fn [index]
                         (let [n (count polygon)]
                           (when (<= (Math/abs
                                      (orientation
                                       (nth polygon (mod (dec index) n))
                                       (nth polygon index)
                                       (nth polygon (mod (inc index) n))))
                                     epsilon)
                             index)))
                       (range (count polygon)))]
          (recur (remove-index polygon collinear) triangles (dec fuel))
          (throw (ex-info "Ear clipping found no legal ear"
                          {:remaining polygon
                           :triangle-count (count triangles)})))))))

(defn- normalized-shape [component normalization]
  (update-in component [:path/geometry :contours]
             (fn [contours]
               (mapv #(update % :points
                              (fn [points]
                                (mapv (partial normalize-point normalization)
                                      points)))
                     contours))))

(defn shape-triangles [component zoom]
  (let [normalization (shape-normalization component)
        normalized (normalized-shape component normalization)
        contours (get-in normalized [:path/geometry :contours])
        outer (filter #(= :outer (:role %)) contours)
        holes (mapv :points (filter #(= :hole (:role %)) contours))
        fills
        (into []
              (mapcat
               (fn [outer-contour]
                 (let [owned-holes
                       (filterv (fn [hole]
                                  (not= :outside
                                        (path-component/contour-classify
                                         (:points outer-contour) (first hole))))
                                holes)]
                   (ear-clip (bridge-holes (:points outer-contour)
                                           owned-holes))))
               outer))]
    (mapv (fn [tri]
            (mapv (partial denormalize-point normalization) tri))
          fills)))

(defn tessellate
  ([component zoom] (tessellate component algorithm-version zoom))
  ([component algorithm zoom]
   (let [triangles (case (:path/kind component)
                     :ink (stroke-triangles component zoom)
                     :shape (shape-triangles component zoom))
         vertices (into [] cat triangles)]
     {:path.mesh/version 2
      :algorithm-version algorithm
      :lod (:lod/id (zoom-lod zoom))
      :cache-key (component-cache-key component algorithm zoom)
      :coverage :aliased-v1
      :vertices vertices
      :triangle-count (count triangles)
      :vertex-count (count vertices)})))

(defn derive-mesh-set
  "Content-keyed derivation cache. A point edit mints only its component key;
   untouched sibling mesh values are returned by identity."
  [cache components zoom]
  (reduce
   (fn [{:keys [cache meshes derived-keys]} component]
     (let [key (component-cache-key component algorithm-version zoom)]
       (if-let [mesh (get cache key)]
         {:cache cache :meshes (conj meshes mesh) :derived-keys derived-keys}
         (let [mesh (tessellate component zoom)]
           {:cache (assoc cache key mesh)
            :meshes (conj meshes mesh)
            :derived-keys (conj derived-keys key)}))))
   {:cache (or cache {}) :meshes [] :derived-keys []}
   components))
