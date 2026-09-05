(ns app.client.region3d.scene
  "Derive spatial state and maintain transform changes.

   Input: canonical region, prior derived scene/key, session transforms,
   camera parameters and geometric queries. Output: derived/maintained
   scene, update classification, camera matrices and hits. No
   namespace-owned scene state survives a call; local memo atoms are
   scratch. Purely returned scene state belongs to its caller.

   Full derivation builds transforms, instances, world triangles and a BVH.
   Transform-only changes update dependents and refit retained topology;
   static component changes rebuild it.

   Folder map: README.md."
  (:require [clojure.set :as set]
            [app.client.region3d.component :as component]))

(def scene-algorithm-version :region3d/scene-v1)
(def bvh-algorithm-version :region3d/bvh-v1)
(def camera-algorithm-version :region3d/camera-v1)
(def tone-map-algorithm-version :khronos-pbr-neutral-v1)
(def ray-epsilon 1.0e-7)
(def bvh-leaf-size 8)

(defn v+
  "Vectors → elementwise sum.

   Variadic map arithmetic. Expects matching dimensions."
  [& vectors] (apply mapv + vectors))
(defn v-
  "Two vectors → difference.

   Elementwise subtraction."
  [left right] (mapv - left right))
(defn v*
  "Vector/scalar → scaled vector.

   Elementwise multiplication."
  [vector scalar] (mapv #(* % scalar) vector))
(defn hadamard
  "Two vectors → elementwise product.

   Direct map."
  [left right] (mapv * left right))
(defn dot
  "Two vectors → scalar dot product.

   Multiply/reduce."
  [left right] (reduce + (map * left right)))
(defn cross
  "Two 3D vectors → perpendicular vector."
  [[ax ay az] [bx by bz]]
  [(- (* ay bz) (* az by))
   (- (* az bx) (* ax bz))
   (- (* ax by) (* ay bx))])
(defn length
  "Vector → magnitude.

   Dot then square root."
  [vector] (Math/sqrt (dot vector vector)))
(defn normalize
  "Vector → unit vector or all-zero vector below epsilon.

   Explicit degenerate fallback. Avoids division failure but can propagate a
   degenerate camera basis."
  [vector]
  (let [magnitude (length vector)]
    (if (< magnitude ray-epsilon)
      (vec (repeat (count vector) 0.0))
      (v* vector (/ 1.0 magnitude)))))
(defn clamp
  "Low/value/high → bounded scalar.

   Min/max."
  [lo value hi] (max lo (min value hi)))
(defn mix
  "Two scalars and amount → linear interpolation.

   Direct weighted sum. Amount is not clamped."
  [left right amount]
  (+ (* left (- 1.0 amount)) (* right amount)))

(def identity-mat4
  [1.0 0.0 0.0 0.0
   0.0 1.0 0.0 0.0
   0.0 0.0 1.0 0.0
   0.0 0.0 0.0 1.0])

(defn mat4-mul
  "Two row-major 4×4 vectors → matrix product.

   Fixed nested reductions. Intended for clarity; allocates intermediate
   sequences."
  [left right]
  (vec
   (for [row (range 4) column (range 4)]
     (reduce +
             (for [index (range 4)]
               (* (nth left (+ (* row 4) index))
                  (nth right (+ (* index 4) column))))))))

(defn mat4-translation
  "XYZ → translation matrix.

   Direct construction."
  [[x y z]]
  [1.0 0.0 0.0 x
   0.0 1.0 0.0 y
   0.0 0.0 1.0 z
   0.0 0.0 0.0 1.0])

(defn mat4-scale
  "XYZ → scale matrix.

   Direct construction."
  [[x y z]]
  [x 0.0 0.0 0.0
   0.0 y 0.0 0.0
   0.0 0.0 z 0.0
   0.0 0.0 0.0 1.0])

(defn mat4-quaternion
  "Unit quaternion → rotation matrix.

   Expanded formula. Trusts unit input."
  [[x y z w]]
  (let [xx (* x x) yy (* y y) zz (* z z)
        xy (* x y) xz (* x z) yz (* y z)
        wx (* w x) wy (* w y) wz (* w z)]
    [(- 1.0 (* 2.0 (+ yy zz))) (* 2.0 (- xy wz))       (* 2.0 (+ xz wy))       0.0
     (* 2.0 (+ xy wz))       (- 1.0 (* 2.0 (+ xx zz))) (* 2.0 (- yz wx))       0.0
     (* 2.0 (- xz wy))       (* 2.0 (+ yz wx))       (- 1.0 (* 2.0 (+ xx yy))) 0.0
     0.0                     0.0                     0.0                       1.0]))

(defn trs-matrix
  "Translation/rotation/scale → composed matrix.

   T×R×S. Explicit order."
  [{:keys [translation rotation scale]}]
  (mat4-mul (mat4-translation translation)
            (mat4-mul (mat4-quaternion rotation)
                      (mat4-scale scale))))

(defn transform-point
  "Matrix and XYZ → transformed point with perspective divide when w is
   neither zero nor one.

   Direct row arithmetic. W=0 returns undivided coordinates;
   projection-specific validity belongs elsewhere."
  [matrix [x y z]]
  (let [rx (+ (* (nth matrix 0) x) (* (nth matrix 1) y)
              (* (nth matrix 2) z) (nth matrix 3))
        ry (+ (* (nth matrix 4) x) (* (nth matrix 5) y)
              (* (nth matrix 6) z) (nth matrix 7))
        rz (+ (* (nth matrix 8) x) (* (nth matrix 9) y)
              (* (nth matrix 10) z) (nth matrix 11))
        rw (+ (* (nth matrix 12) x) (* (nth matrix 13) y)
              (* (nth matrix 14) z) (nth matrix 15))]
    (if (and (not (zero? rw)) (not= 1.0 rw))
      [(/ rx rw) (/ ry rw) (/ rz rw)]
      [rx ry rz])))

(defn transform-direction
  "Matrix and vector → transformed direction without translation.

   Upper-left 3×3."
  [matrix [x y z]]
  [ (+ (* (nth matrix 0) x) (* (nth matrix 1) y) (* (nth matrix 2) z))
    (+ (* (nth matrix 4) x) (* (nth matrix 5) y) (* (nth matrix 6) z))
    (+ (* (nth matrix 8) x) (* (nth matrix 9) y) (* (nth matrix 10) z))])

(defn inverse-mat4
  "Row-major matrix → inverse; singular pivot throws.

   Gauss–Jordan with largest pivot selection. Intended for general 4×4
   inversion; allocates small intermediate vectors."
  [matrix]
  (let [rows (mapv (fn [row]
                     (vec (concat (subvec (vec matrix) (* row 4) (* (inc row) 4))
                                  (for [column (range 4)]
                                    (if (= row column) 1.0 0.0)))))
                   (range 4))]
    (loop [pivot 0 rows rows]
      (if (= pivot 4)
        (vec (mapcat #(subvec % 4 8) rows))
        (let [swap-row (apply max-key
                              #(Math/abs (double (get-in rows [% pivot])))
                              (range pivot 4))
              pivot-value (get-in rows [swap-row pivot])]
          (when (< (Math/abs (double pivot-value)) ray-epsilon)
            (throw (ex-info "Region3D matrix is singular"
                            {:matrix matrix :pivot pivot})))
          (let [rows (assoc rows pivot (nth rows swap-row)
                                  swap-row (nth rows pivot))
                pivot-value (get-in rows [pivot pivot])
                pivot-row (mapv #(/ % pivot-value) (nth rows pivot))
                rows (assoc rows pivot pivot-row)
                rows (reduce
                      (fn [result row]
                        (if (= row pivot)
                          result
                          (let [factor (get-in result [row pivot])]
                            (assoc result row
                                   (mapv - (nth result row)
                                         (mapv #(* factor %) pivot-row))))))
                      rows (range 4))]
            (recur (inc pivot) rows)))))))

(defn compose-hierarchy
  "Canonical region → object/world-matrix map.

   Local memoized recursive world-transform. Assumes cycles/missing parents
   were validated before entry."
  [region]
  (let [scene (:scene region)
        !memo (atom {})]
    (letfn [(world-transform [object-id]
              (or (get @!memo object-id)
                  (let [object (get scene object-id)
                        local (trs-matrix (:transform object))
                        result (if-let [parent (:parent object)]
                                 (mat4-mul (world-transform parent) local)
                                 local)]
                    (swap! !memo assoc object-id result)
                    result)))]
      (doseq [object-id (sort-by pr-str (keys scene))]
        (world-transform object-id))
      @!memo)))

(defn affected-descendants
  "Scene and ID → descendant set including ID.

   Frontier walk; scans scene for each parent's children. O(affected objects
   × scene size), with no child adjacency index."
  [scene object-id]
  (loop [frontier [object-id] result #{}]
    (if-let [parent (first frontier)]
      (let [children (->> scene
                          (keep (fn [[candidate row]]
                                  (when (= parent (:parent row)) candidate)))
                          (sort-by pr-str))]
        (recur (into (vec (rest frontier)) children) (conj result parent)))
      result)))

(defn- mesh-row
  "Nested positions/normals and indexes → flattened indexed mesh.

   Representation adapter."
  [positions normals indices]
  {:positions (vec (mapcat identity positions))
   :normals (vec (mapcat identity normals))
   :indices (vec indices)})

(defn- box-mesh
  "Box size → indexed flat-shaded six-face mesh.

   Separate face vertices/normals. Intended for hard edges."
  [{[sx sy sz] :size}]
  (let [x (/ sx 2.0) y (/ sy 2.0) z (/ sz 2.0)
        specs [{:n [0.0 0.0 1.0]
                :p [[(- x) (- y) z] [x (- y) z] [x y z] [(- x) y z]]}
               {:n [0.0 0.0 -1.0]
                :p [[x (- y) (- z)] [(- x) (- y) (- z)]
                    [(- x) y (- z)] [x y (- z)]]}
               {:n [1.0 0.0 0.0]
                :p [[x (- y) z] [x (- y) (- z)] [x y (- z)] [x y z]]}
               {:n [-1.0 0.0 0.0]
                :p [[(- x) (- y) (- z)] [(- x) (- y) z]
                    [(- x) y z] [(- x) y (- z)]]}
               {:n [0.0 1.0 0.0]
                :p [[(- x) y z] [x y z] [x y (- z)] [(- x) y (- z)]]}
               {:n [0.0 -1.0 0.0]
                :p [[(- x) (- y) (- z)] [x (- y) (- z)]
                    [x (- y) z] [(- x) (- y) z]]}]
        positions (vec (mapcat :p specs))
        normals (vec (mapcat #(repeat 4 (:n %)) specs))
        indices (vec (mapcat (fn [face]
                               (let [base (* face 4)]
                                 [base (inc base) (+ base 2)
                                  base (+ base 2) (+ base 3)]))
                             (range 6)))]
    (mesh-row positions normals indices)))

(defn- plane-mesh
  "Two dimensions → indexed XZ plane with +Y normals.

   Four vertices/two triangles. This plane differs from placement-local XY
   planes."
  [{[sx sz] :size}]
  (let [x (/ sx 2.0) z (/ sz 2.0)]
    (mesh-row [[(- x) 0.0 z] [x 0.0 z] [x 0.0 (- z)] [(- x) 0.0 (- z)]]
              (repeat 4 [0.0 1.0 0.0])
              [0 1 2 0 2 3])))

(defn- sphere-mesh
  "Radius/segment counts → indexed latitude/longitude sphere.

   Grid with pole-specific triangles. Intended for deterministic primitive
   generation; cost grows with segment-count product."
  [{:keys [radius width-segments height-segments]}]
  (let [positions (vec
                   (for [iy (range (inc height-segments))
                         ix (range (inc width-segments))
                         :let [v (/ iy height-segments)
                               u (/ ix width-segments)
                               phi (* Math/PI v)
                               theta (* 2.0 Math/PI u)
                               normal [( * (Math/sin phi) (Math/sin theta))
                                       (Math/cos phi)
                                       (* (Math/sin phi) (Math/cos theta))]]]
                     (v* normal radius)))
        normals (mapv normalize positions)
        stride (inc width-segments)
        indices (vec
                 (mapcat
                  (fn [[iy ix]]
                    (let [a (+ (* iy stride) ix)
                          b (+ a stride)]
                      (cond
                        (= iy 0) [a b (inc b)]
                        (= iy (dec height-segments)) [a b (inc a)]
                        :else [a b (inc b) a (inc b) (inc a)])))
                  (for [iy (range height-segments)
                        ix (range width-segments)] [iy ix])))]
    (mesh-row positions normals indices)))

(defn- radial-mesh
  "Radius/height/segments and cone? → capped cylinder/cone mesh.

   Local ring helper, side and cap construction. One cone apex vertex/normal
   is a shading choice, not a general smooth-surface guarantee."
  [{:keys [radius height radial-segments]} cone?]
  (let [half (/ height 2.0)
        ring (fn [y ring-radius]
               (for [index (range radial-segments)
                     :let [angle (* 2.0 Math/PI (/ index radial-segments))]]
                 [(* ring-radius (Math/sin angle)) y
                  (* ring-radius (Math/cos angle))]))
        bottom (vec (ring (- half) radius))
        top (if cone? [[0.0 half 0.0]] (vec (ring half radius)))
        side-positions (vec (concat bottom top))
        slope (if cone? (/ radius height) 0.0)
        bottom-normals (for [[x _ z] bottom]
                         (normalize [x (* radius slope) z]))
        top-normals (if cone?
                      [(normalize [0.0 radius 0.0])]
                      (for [[x _ z] top] (normalize [x 0.0 z])))
        side-normals (vec (concat bottom-normals top-normals))
        side-indices
        (if cone?
          (vec (mapcat (fn [index]
                         [index (mod (inc index) radial-segments)
                          radial-segments])
                       (range radial-segments)))
          (vec (mapcat
                (fn [index]
                  (let [next (mod (inc index) radial-segments)
                        top-index (+ radial-segments index)
                        next-top (+ radial-segments next)]
                    [index next next-top index next-top top-index]))
                (range radial-segments))))
        base-start (count side-positions)
        base-positions (vec (cons [0.0 (- half) 0.0] bottom))
        base-normals (vec (repeat (count base-positions) [0.0 -1.0 0.0]))
        base-indices
        (vec (mapcat (fn [index]
                       [base-start
                        (+ base-start 1 (mod (inc index) radial-segments))
                        (+ base-start 1 index)])
                     (range radial-segments)))
        cap-start (+ base-start (count base-positions))
        cap-positions (if cone? [] (vec (cons [0.0 half 0.0] top)))
        cap-normals (vec (repeat (count cap-positions) [0.0 1.0 0.0]))
        cap-indices
        (if cone? []
            (vec (mapcat (fn [index]
                           [cap-start (+ cap-start 1 index)
                            (+ cap-start 1 (mod (inc index) radial-segments))])
                         (range radial-segments))))]
    (mesh-row (concat side-positions base-positions cap-positions)
              (concat side-normals base-normals cap-normals)
              (concat side-indices base-indices cap-indices))))

(defn- torus-mesh
  "Radii and two segment counts → indexed torus.

   Parametric loops and wrapped local index-of. Product-sized allocation."
  [{:keys [radius tube radial-segments tubular-segments]}]
  (let [positions
        (vec
         (for [radial (range radial-segments)
               tubular (range tubular-segments)
               :let [u (* 2.0 Math/PI (/ radial radial-segments))
                     v (* 2.0 Math/PI (/ tubular tubular-segments))
                     ring (+ radius (* tube (Math/cos v)))]]
           [(* ring (Math/sin u))
            (* tube (Math/sin v))
            (* ring (Math/cos u))]))
        normals
        (vec
         (for [radial (range radial-segments)
               tubular (range tubular-segments)
               :let [u (* 2.0 Math/PI (/ radial radial-segments))
                     v (* 2.0 Math/PI (/ tubular tubular-segments))]]
           [(* (Math/cos v) (Math/sin u))
            (Math/sin v)
            (* (Math/cos v) (Math/cos u))]))
        index-of (fn [radial tubular]
                   (+ (* (mod radial radial-segments) tubular-segments)
                      (mod tubular tubular-segments)))
        indices
        (vec
         (mapcat
          (fn [[radial tubular]]
            (let [a (index-of radial tubular)
                  b (index-of (inc radial) tubular)
                  c (index-of (inc radial) (inc tubular))
                  d (index-of radial (inc tubular))]
              [a b c a c d]))
          (for [radial (range radial-segments)
                tubular (range tubular-segments)] [radial tubular])))]
    (mesh-row positions normals indices)))

(defn primitive-mesh
  "Primitive or indexed mesh → positions/normals/indexes.

   Canonicalizes defaults then dispatches. Despite “validated” wording it
   does not perform complete schema validation itself."
  [mesh]
  (let [object (component/canonical-object
                {:object/id :primitive
                 :object/kind :mesh
                 :parent nil
                 :transform component/default-transform
                 :provenance {:asserted-by :derive}
                 :mesh mesh
                 :component component/default-component})
        mesh (:mesh object)
        params (:params mesh)]
    (case (:kind mesh)
      :box (box-mesh params)
      :plane (plane-mesh params)
      :sphere (sphere-mesh params)
      :cylinder (radial-mesh params false)
      :cone (radial-mesh params true)
      :torus (torus-mesh params)
      :indexed-triangles (select-keys mesh [:positions :normals :indices]))))

(defn object-mesh
  "Object → mesh for mesh-kind, otherwise nil.

   Indexed pass-through or primitive derivation."
  [object]
  (when (= :mesh (:object/kind object))
    (if (= :indexed-triangles (get-in object [:mesh :kind]))
      (select-keys (:mesh object) [:positions :normals :indices])
      (primitive-mesh (:mesh object)))))

(defn- unpack-vec3
  "Flat data and vertex index → triple.

   Vector slice."
  [flat index]
  (subvec (vec flat) (* index 3) (+ (* index 3) 3)))

(defn transformed-triangles
  "Object and world matrix → world triangles with object/index/normal, or
   nil.

   Expands indexed triangles and derives geometric normals. Regenerates
   primitive geometry when called during transform maintenance."
  [object world-transform-matrix]
  (when-let [{:keys [positions indices]} (object-mesh object)]
    (mapv
     (fn [triangle-index]
       (let [offset (* triangle-index 3)
             ia (nth indices offset)
             ib (nth indices (inc offset))
             ic (nth indices (+ offset 2))
             a (transform-point world-transform-matrix (unpack-vec3 positions ia))
             b (transform-point world-transform-matrix (unpack-vec3 positions ib))
             c (transform-point world-transform-matrix (unpack-vec3 positions ic))]
         {:object-id (:object/id object)
          :triangle-index triangle-index
          :a a :b b :c c
          :normal (normalize (cross (v- b a) (v- c a)))}))
     (range (quot (count indices) 3)))))

(defn- points-aabb
  "Points → min/max bounds or nil.

   Linear reduction."
  [points]
  (when (seq points)
    (reduce (fn [{:keys [min max]} point]
              {:min (mapv clojure.core/min min point)
               :max (mapv clojure.core/max max point)})
            {:min (first points) :max (first points)}
            (rest points))))

(defn- triangle-aabb
  "Triangle → bounds.

   Three-point specialization."
  [{:keys [a b c]}]
  (points-aabb [a b c]))

(defn- merge-aabb
  "Two optional bounds → union.

   Nil-aware component min/max."
  [left right]
  (cond
    (nil? left) right
    (nil? right) left
    :else {:min (mapv clojure.core/min (:min left) (:min right))
           :max (mapv clojure.core/max (:max left) (:max right))}))

(defn- triangle-centroid
  "Triangle → mean of vertices.

   Vector arithmetic."
  [{:keys [a b c]}]
  (v* (v+ a b c) (/ 1.0 3.0)))

(defn build-bvh
  "Triangles → tree or nil.

   Local recursive build: largest-extent axis, deterministic median sort,
   ≤8-triangle leaves. Intended for stable baseline; repeated per-level
   sorting is a construction tradeoff."
  [triangles]
  (let [triangles (vec triangles)]
    (when (seq triangles)
      (letfn [(build [rows]
                (let [bounds (reduce merge-aabb nil (map triangle-aabb rows))]
                  (if (<= (count rows) bvh-leaf-size)
                    {:bvh/version bvh-algorithm-version
                     :kind :leaf :bounds bounds :triangles (vec rows)
                     :object-ids (set (map :object-id rows))}
                    (let [extent (v- (:max bounds) (:min bounds))
                          axis (apply max-key #(nth extent %) (range 3))
                          ordered (vec
                                   (sort-by
                                    (fn [triangle]
                                      [(nth (triangle-centroid triangle) axis)
                                       (pr-str (:object-id triangle))
                                       (:triangle-index triangle)])
                                    rows))
                          split (quot (count ordered) 2)
                          left (build (subvec ordered 0 split))
                          right (build (subvec ordered split))]
                      {:bvh/version bvh-algorithm-version
                       :kind :branch :bounds bounds
                       :object-ids (set/union (:object-ids left)
                                              (:object-ids right))
                       :left left :right right}))))]
        (build triangles)))))

(defn refit-bvh
  "BVH, replacement triangles by object, affected IDs → updated tree
   preserving untouched node identity.

   Uses subtree object sets, replaces leaf triangles by index, recomputes
   bounds. Intended for unchanged topology; missing replacement triangles
   retain old values."
  [bvh triangles-by-object affected-object-ids]
  (when bvh
    (if (empty? (set/intersection (:object-ids bvh) affected-object-ids))
      bvh
      (case (:kind bvh)
        :leaf
        (let [old-triangles (:triangles bvh)
              touched (set/intersection (:object-ids bvh)
                                        affected-object-ids)
              replacements
              (into {}
                    (for [object-id touched]
                      [object-id
                       (into {} (map (juxt :triangle-index identity))
                             (get triangles-by-object object-id []))]))
              triangles
              (mapv (fn [{:keys [object-id triangle-index] :as triangle}]
                      (or (get-in replacements [object-id triangle-index])
                          triangle))
                    old-triangles)]
          (assoc bvh :triangles triangles
                     :bounds (reduce merge-aabb nil
                                     (map triangle-aabb triangles))))
        :branch
        (let [left (refit-bvh (:left bvh) triangles-by-object
                              affected-object-ids)
              right (refit-bvh (:right bvh) triangles-by-object
                               affected-object-ids)]
          (if (and (identical? left (:left bvh))
                   (identical? right (:right bvh)))
            bvh
            (assoc bvh :left left :right right
                       :bounds (merge-aabb (:bounds left)
                                           (:bounds right)))))))))

(defn- ray-aabb-hit?
  "Ray, bounds, max distance → intersects positive interval?

   Slab intersection with near-parallel handling. Intended for pruning."
  [{:keys [origin direction]} {min-point :min max-point :max} max-t]
  (when (and min-point max-point)
    (loop [axis 0 tmin 0.0 tmax max-t]
      (if (= axis 3)
        (<= tmin tmax)
        (let [o (nth origin axis)
              d (nth direction axis)
              lo (nth min-point axis)
              hi (nth max-point axis)]
          (if (< (Math/abs (double d)) ray-epsilon)
            (and (<= lo o hi) (recur (inc axis) tmin tmax))
            (let [inv (/ 1.0 d)
                  a (* (- lo o) inv)
                  b (* (- hi o) inv)
                  near (clojure.core/min a b)
                  far (clojure.core/max a b)]
              (recur (inc axis)
                     (clojure.core/max tmin near)
                     (clojure.core/min tmax far)))))))))

(defn ray-triangle
  "Ray and triangle → positive hit record or nil.

   Möller–Trumbore with tolerant edge inclusion. Two-sided CPU query differs
   from backface-culling GPU pipelines."
  [{:keys [origin direction]} {:keys [a b c normal] :as triangle}]
  (let [edge1 (v- b a)
        edge2 (v- c a)
        p (cross direction edge2)
        determinant (dot edge1 p)]
    (when (> (Math/abs (double determinant)) ray-epsilon)
      (let [inverse (/ 1.0 determinant)
            tvec (v- origin a)
            u (* (dot tvec p) inverse)]
        (when (<= (- ray-epsilon) u (+ 1.0 ray-epsilon))
          (let [q (cross tvec edge1)
                v (* (dot direction q) inverse)]
            (when (and (<= (- ray-epsilon) v)
                       (<= (+ u v) (+ 1.0 ray-epsilon)))
              (let [t (* (dot edge2 q) inverse)]
                (when (> t ray-epsilon)
                  (assoc triangle
                         :t t
                         :point3 (v+ origin (v* direction t))
                         :normal normal
                         :barycentric [(- 1.0 u v) u v]
                         :boundary? (or (< (Math/abs (double u)) ray-epsilon)
                                        (< (Math/abs (double v)) ray-epsilon)
                                        (< (Math/abs (double (- 1.0 u v)))
                                           ray-epsilon))))))))))))

(defn- hit-before?
  "Candidate/current hit → candidate wins?

   Distance epsilon then printed object-ID tie-break. Deterministic
   coincident-object choice."
  [candidate current]
  (or (nil? current)
      (< (:t candidate) (- (:t current) ray-epsilon))
      (and (<= (Math/abs (double (- (:t candidate) (:t current))))
               ray-epsilon)
           (neg? (compare (pr-str (:object-id candidate))
                          (pr-str (:object-id current)))))))

(defn query-bvh
  "BVH and ray → nearest hit or nil.

   Local walk prunes bounds using current best, visits left then right. Does
   not order children by ray-near distance."
  [bvh ray]
  (letfn [(walk [node best]
            (if (or (nil? node)
                    (not (ray-aabb-hit? ray (:bounds node)
                                        (or (:t best) ##Inf))))
              best
              (case (:kind node)
                :leaf (reduce (fn [current triangle]
                                (if-let [candidate (ray-triangle ray triangle)]
                                  (if (hit-before? candidate current)
                                    candidate current)
                                  current))
                              best (:triangles node))
                :branch (let [left (walk (:left node) best)]
                          (walk (:right node) left)))))]
    (walk bvh nil)))

(defn look-at
  "Eye/target/up → view matrix.

   Builds orthonormal basis. Parallel up/view vectors yield zero basis
   through normalize."
  [eye target up]
  (let [z (normalize (v- eye target))
        x (normalize (cross up z))
        y (cross z x)]
    [(nth x 0) (nth x 1) (nth x 2) (- (dot x eye))
     (nth y 0) (nth y 1) (nth y 2) (- (dot y eye))
     (nth z 0) (nth z 1) (nth z 2) (- (dot z eye))
     0.0 0.0 0.0 1.0]))

(defn orbit-eye
  "Pivot/distance/yaw/pitch → eye position.

   Spherical orbit calculation."
  [{:keys [pivot distance yaw pitch]}]
  (let [cp (Math/cos pitch)]
    (v+ pivot
        [(* distance cp (Math/sin yaw))
         (* distance (Math/sin pitch))
         (* distance cp (Math/cos yaw))])))

(defn perspective-matrix
  "FOV/aspect/near/far → perspective projection.

   Direct formula for this depth convention. Intended for validated
   nondegenerate inputs."
  [fov-y-deg aspect near far]
  (let [f (/ 1.0 (Math/tan (/ (* fov-y-deg Math/PI) 360.0)))]
    [(/ f aspect) 0.0 0.0 0.0
     0.0 f 0.0 0.0
     0.0 0.0 (/ far (- near far)) (/ (* near far) (- near far))
     0.0 0.0 -1.0 0.0]))

(defn ortho-matrix
  "Scale/aspect/near/far → orthographic projection."
  [scale aspect near far]
  (let [half-y (/ scale 2.0)
        half-x (* half-y aspect)]
    [(/ 1.0 half-x) 0.0 0.0 0.0
     0.0 (/ 1.0 half-y) 0.0 0.0
     0.0 0.0 (/ 1.0 (- near far)) (/ near (- near far))
     0.0 0.0 0.0 1.0]))

(defn camera-matrices
  "View and viewport → camera record including forward/inverse matrices.

   Canonicalizes view and composes orbit/look-at/lens. Clamps height
   denominator but not zero width or degenerate orbit poles."
  [view viewport]
  (let [view (component/canonical-view view)
        [width height] viewport
        aspect (/ (double width) (max 1.0 (double height)))
        eye (orbit-eye view)
        view-matrix (look-at eye (:pivot view) [0.0 1.0 0.0])
        lens (:lens view)
        projection
        (case (:kind lens)
          :perspective (perspective-matrix (:fov-y-deg lens) aspect
                                           (:near lens) (:far lens))
          :ortho (ortho-matrix (:ortho-scale lens) aspect
                               (:near lens) (:far lens)))
        view-projection (mat4-mul projection view-matrix)]
    {:camera/version camera-algorithm-version
     :eye eye :view view-matrix :projection projection
     :view-projection view-projection
     :inverse-view-projection (inverse-mat4 view-projection)
     :viewport [width height] :lens lens}))

(defn ray-from-region-point
  "Camera and pixel point → near-plane origin, normalized ray direction and
   NDC.

   Unprojects depth 0/1. Coordinates must match the camera viewport."
  [camera [local-x local-y]]
  (let [[width height] (:viewport camera)
        ndc-x (- (* 2.0 (/ local-x width)) 1.0)
        ndc-y (- 1.0 (* 2.0 (/ local-y height)))
        inverse (:inverse-view-projection camera)
        near (transform-point inverse [ndc-x ndc-y 0.0])
        far (transform-point inverse [ndc-x ndc-y 1.0])]
    {:origin near :direction (normalize (v- far near))
     :ndc [ndc-x ndc-y]}))

(defn project-point
  "Camera and world point → screen/depth or nil behind perspective plane.

   Explicit clip projection and positive-w check. Output may still be
   offscreen/outside depth range."
  [camera point]
  (let [matrix (:view-projection camera)
        [x y z] point
        clip [(+ (* (nth matrix 0) x) (* (nth matrix 1) y)
                 (* (nth matrix 2) z) (nth matrix 3))
              (+ (* (nth matrix 4) x) (* (nth matrix 5) y)
                 (* (nth matrix 6) z) (nth matrix 7))
              (+ (* (nth matrix 8) x) (* (nth matrix 9) y)
                 (* (nth matrix 10) z) (nth matrix 11))
              (+ (* (nth matrix 12) x) (* (nth matrix 13) y)
                 (* (nth matrix 14) z) (nth matrix 15))]
        w (nth clip 3)
        [width height] (:viewport camera)]
    (when (> w ray-epsilon)
      (let [ndc-x (/ (nth clip 0) w)
            ndc-y (/ (nth clip 1) w)]
        {:screen [(* (+ ndc-x 1.0) 0.5 width)
                  (* (- 1.0 ndc-y) 0.5 height)]
         :depth (/ (nth clip 2) w)}))))

(defn derive-instance-row
  "Object and matrix → shader/placement-facing instance plus transparency
   classification.

   Projects kind-specific data. Text/ink always enter transparent
   classification."
  [object world-transform-matrix]
  {:object-id (:object/id object)
   :kind (:object/kind object)
   :matrix world-transform-matrix
   :component (:component object)
   :light (:light object)
   :placement (case (:object/kind object)
                :text (:text object)
                :ink (:ink object)
                nil)
   :transparent? (or (contains? #{:text :ink} (:object/kind object))
                     (and (= :mesh (:object/kind object))
                          (< (get-in object [:component :base-color :rgba 3] 1.0)
                             1.0)))})

(defn derive-scene
  "Canonical region → complete spatial state and derivation counters.

   Sorted objects, instances, triangles and BVH. Serves as full oracle;
   counters describe work encoded by this function, not GPU submissions."
  [region]
  (let [world-transforms (compose-hierarchy region)
        object-ids (sort-by pr-str (keys (:scene region)))
        instances-by-object
        (into {} (map (fn [object-id]
                        [object-id
                         (derive-instance-row (get-in region [:scene object-id])
                                              (get world-transforms object-id))]))
              object-ids)
        triangles-by-object
        (into {} (keep (fn [object-id]
                         (let [object (get-in region [:scene object-id])]
                           (when (= :mesh (:object/kind object))
                             [object-id
                              (transformed-triangles object
                                                     (get world-transforms object-id))]))))
              object-ids)
        triangles (vec (mapcat #(get triangles-by-object % []) object-ids))]
    {:derive/version scene-algorithm-version
     :region region
     :world-transforms world-transforms
     :instances-by-object instances-by-object
     :instances (mapv instances-by-object object-ids)
     :triangles-by-object triangles-by-object
     :bvh (build-bvh triangles)
     :stats {:full-rebuilds 1
               :instance-uploads (count object-ids)
               :bvh-build-triangles (count triangles)
               :bvh-refits 0
               :region-encodes 1}}))

(defn- compose-affected
  "Scene, prior matrices, affected set → matrix map with changed branches
   recomposed.

   Local memoized recursion reuses unaffected parents. Intended for stable
   hierarchy."
  [scene prior-world-transforms affected]
  (let [!memo (atom {})]
    (letfn [(world-transform [object-id]
              (if-not (contains? affected object-id)
                (get prior-world-transforms object-id)
                (or (get @!memo object-id)
                    (let [object (get scene object-id)
                          local (trs-matrix (:transform object))
                          result (if-let [parent (:parent object)]
                                   (mat4-mul (world-transform parent) local)
                                   local)]
                      (swap! !memo assoc object-id result)
                      result))))]
      (doseq [object-id (sort-by pr-str affected)]
        (world-transform object-id))
      (merge prior-world-transforms @!memo))))

(defn- maintain-affected
  "Prior state, next region, affected set → updated
   instances/triangles/refit BVH and counters.

   Rebuilds only affected spatial rows; recreates ordered instance vector.
   Intended for transform-only changes."
  [maintained next-region affected]
  (let [world-transforms (compose-affected (:scene next-region)
                                    (:world-transforms maintained)
                                    affected)
        instances-by-object
        (reduce
         (fn [rows id]
           (assoc rows id
                  (derive-instance-row (get-in next-region [:scene id])
                                       (get world-transforms id))))
         (:instances-by-object maintained) affected)
        triangles-by-object
        (reduce
         (fn [rows id]
           (let [object (get-in next-region [:scene id])]
             (if (= :mesh (:object/kind object))
               (assoc rows id (transformed-triangles object
                                                    (get world-transforms id)))
               rows)))
         (:triangles-by-object maintained) affected)
        bvh (refit-bvh (:bvh maintained) triangles-by-object affected)
        object-ids (sort-by pr-str (keys (:scene next-region)))]
    (assoc maintained
           :region next-region
           :world-transforms world-transforms
           :instances-by-object instances-by-object
           :instances (mapv instances-by-object object-ids)
           :triangles-by-object triangles-by-object
           :bvh bvh
           :stats {:full-rebuilds 0
                     :instance-uploads (count affected)
                     :bvh-build-triangles 0
                     :bvh-refits (count (filter triangles-by-object affected))
                     :region-encodes 1
                     :affected-object-ids affected})))

(defn maintain-transforms
  "Prior scene and transform overrides → maintained scene; missing
   state/objects throw.

   Canonicalizes overrides, drops equal values, unions descendants and
   refits. Canonicalization is not full transform schema validation."
  [maintained transforms-by-object]
  (when-not maintained
    (throw (ex-info "Region3D transform maintenance requires a derived scene"
                    {:transforms transforms-by-object})))
  (let [region (:region maintained)
        scene (:scene region)
        transforms
        (into {}
              (map (fn [[object-id transform]]
                     (when-not (contains? scene object-id)
                       (throw (ex-info "Region3D transform target is missing"
                                       {:object-id object-id})))
                     [object-id (component/canonical-transform transform)]))
              transforms-by-object)
        changed
        (into {}
              (remove (fn [[object-id transform]]
                        (= transform (get-in scene [object-id :transform]))))
              transforms)]
    (if (empty? changed)
      (assoc maintained :stats
             {:full-rebuilds 0 :instance-uploads 0 :bvh-build-triangles 0
              :bvh-refits 0 :region-encodes 0 :affected-object-ids #{}})
      (let [next-region
            (reduce-kv (fn [value object-id transform]
                         (assoc-in value [:scene object-id :transform] transform))
                       region changed)
            affected
            (reduce set/union #{}
                    (map #(affected-descendants (:scene next-region) %)
                         (keys changed)))]
        (maintain-affected maintained next-region affected)))))

(defn pick-region
  "Maintained scene, rendered camera, region point → nearest mesh object or
   background route.

   Camera ray plus BVH. Intended for mesh picking; text/ink placements are
   not in this BVH."
  [{:keys [maintained camera region-point]}]
  (when-not camera
    (throw (ex-info "Region pick requires the rendered camera."
                    {:error-type :region3d/missing-camera})))
  (let [region (:region maintained)
        ray (ray-from-region-point camera region-point)]
    (if-let [hit (query-bvh (:bvh maintained) ray)]
      (select-keys (assoc hit :route :object)
                   [:route :object-id :point3 :normal :t
                    :triangle-index :boundary?])
      {:route :region-background
       :region-id (:region/id region)})))

(defn shadow-light-space
  "Maintained scene → first eligible light's snapped/padded shadow-space
   record or nil.

   Chooses sorted directional shadow light, bounds all mesh vertices in
   light space. Scans triangles; one shadow space serves the renderer even
   if several lights request shadows."
  [maintained]
  (let [region (:region maintained)
        light-id (->> (:scene region)
                      (filter (fn [[_ object]]
                                (and (= :light (:object/kind object))
                                     (= :directional
                                        (get-in object [:light :kind]))
                                     (true? (get-in object
                                                    [:light :cast-shadow])))))
                      (map first) (sort-by pr-str) first)
        triangles (mapcat val (:triangles-by-object maintained))]
    (when (and light-id (seq triangles))
      (let [matrix (get-in maintained [:world-transforms light-id])
            origin (transform-point matrix [0.0 0.0 0.0])
            direction (normalize (transform-direction matrix [0.0 0.0 -1.0]))
            up (if (> (Math/abs (double (dot direction [0.0 1.0 0.0]))) 0.99)
                 [1.0 0.0 0.0] [0.0 1.0 0.0])
            view (look-at origin (v+ origin direction) up)
            points (mapcat (juxt :a :b :c) triangles)
            light-points (map #(transform-point view %) points)
            bounds (points-aabb light-points)
            extent (v- (:max bounds) (:min bounds))]
        (when (every? #(> % ray-epsilon) extent)
          (let [padding (mapv #(* % (:bounds-padding component/shadow-constants))
                              extent)
                minimum (v- (:min bounds) padding)
                maximum (v+ (:max bounds) padding)
                texel [(/ (- (nth maximum 0) (nth minimum 0))
                          (:size component/shadow-constants))
                       (/ (- (nth maximum 1) (nth minimum 1))
                          (:size component/shadow-constants))]
                snapped-min [(Math/floor (/ (nth minimum 0) (nth texel 0)))
                             (Math/floor (/ (nth minimum 1) (nth texel 1)))]
                snapped-min (mapv * snapped-min texel)
                snapped-max [(+ (nth snapped-min 0)
                                (- (nth maximum 0) (nth minimum 0)))
                             (+ (nth snapped-min 1)
                                (- (nth maximum 1) (nth minimum 1)))] ]
            {:algorithm-version component/shadow-algorithm-version
             :light-id light-id :view view
             :bounds {:min [(nth snapped-min 0) (nth snapped-min 1)
                            (nth minimum 2)]
                      :max [(nth snapped-max 0) (nth snapped-max 1)
                            (nth maximum 2)]}
             :texel-world texel
             :constants component/shadow-constants}))))))

;; Re-evaluates a 3D scene between frames without redoing everything: component
;; and topology changes derive, transform changes only maintain.

(defn session-transform-map
  "Region and session row → authoritative transforms overlaid with settled
   then preview values.

   Rejects overlay IDs outside the region. Ownership stays explicit."
  [region session-row]
  (let [object-ids (set (keys (:scene region)))
        settled (or (:settled-transforms session-row) {})
        preview (:preview-transform session-row)
        overlays (cond-> settled
                   (and (:object-id preview) (:transform preview))
                   (assoc (:object-id preview) (:transform preview)))]
    (doseq [object-id (keys overlays)]
      (when-not (contains? object-ids object-id)
        (throw (ex-info "Region3D session transform target is missing"
                        {:object-id object-id}))))
    (reduce-kv (fn [result object-id object]
                 (assoc result object-id
                        (get overlays object-id (:transform object))))
               {} (:scene region))))

(defn session-region-value
  "Region and session → region with resolved transforms.

   Applies the resolved map."
  [region session-row]
  (reduce-kv (fn [value object-id transform]
               (assoc-in value [:scene object-id :transform] transform))
             region (session-transform-map region session-row)))

(defn evaluation-key
  "Region and session → static component plus resolved-transform keys.

   Excludes ID/revision/rect/view/background and separates object
   transforms. Expresses distinct dependencies, though
   building/equality-checking keys visits scene data."
  [region session-row]
  {:static-component
   (-> region
       (dissoc :region/id :region/revision :region/rect :view :background)
       (update :scene
               (fn [objects]
                 (into {} (map (fn [[object-id object]]
                                 [object-id (dissoc object :transform)]))
                       objects))))
   :transforms (session-transform-map region session-row)})

(defn evaluate-scene
  "Prior scene/key, current region/session → state/key/update kind/affected
   IDs.

   Full derive for static change; identity reuse for none; maintenance for
   transforms. Excluded background/view changes must be handled by renderer,
   as they are."
  [maintained prior-key region session-row]
  (let [next-key (evaluation-key region session-row)]
    (cond
      (or (nil? maintained)
          (not= (:static-component prior-key)
                (:static-component next-key)))
      {:maintained (derive-scene
                    (session-region-value region session-row))
       :evaluation-key next-key
       :update-kind :full
       :affected-object-ids (set (keys (:scene region)))}

      (= (:transforms prior-key) (:transforms next-key))
      {:maintained maintained
       :evaluation-key next-key
       :update-kind :none
       :affected-object-ids #{}}

      :else
      (let [changed
            (into {}
                  (remove (fn [[object-id transform]]
                            (= transform
                               (get-in prior-key [:transforms object-id]))))
                  (:transforms next-key))
            maintained (maintain-transforms maintained changed)]
        {:maintained maintained
         :evaluation-key next-key
         :update-kind :transform
         :affected-object-ids
         (get-in maintained [:stats :affected-object-ids])}))))
