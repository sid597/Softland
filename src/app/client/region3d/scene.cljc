(ns app.client.region3d.scene
  "Pure Region3D scene derivation.

   Keyed inputs: a canonical region row plus session camera values joined at
   the renderer edge. Door: event causes only. Ownership:
   the region row is one generation; session camera is a separate stamped
   generation. Projections: instance rows, BVH/ray pick, tape entry, pass
   fragment, inspector rows, and the full batch oracle. The maintained view
   keeps the full derivation as its equivalence fence and refits only the
   affected hierarchy subtree for transform edits. No clock exists here."
  (:require [clojure.set :as set]
            [app.client.region3d.material :as material]))

(def scene-algorithm-version :region3d/scene-v1)
(def bvh-algorithm-version :region3d/bvh-v1)
(def camera-algorithm-version :region3d/camera-v1)
(def tone-map-algorithm-version :khronos-pbr-neutral-v1)
(def ray-epsilon 1.0e-7)
(def bvh-leaf-size 8)

(defn v+ [& vectors] (apply mapv + vectors))
(defn v- [left right] (mapv - left right))
(defn v* [vector scalar] (mapv #(* % scalar) vector))
(defn hadamard [left right] (mapv * left right))
(defn dot [left right] (reduce + (map * left right)))
(defn cross [[ax ay az] [bx by bz]]
  [(- (* ay bz) (* az by))
   (- (* az bx) (* ax bz))
   (- (* ax by) (* ay bx))])
(defn length [vector] (Math/sqrt (dot vector vector)))
(defn normalize [vector]
  (let [magnitude (length vector)]
    (if (< magnitude ray-epsilon)
      (vec (repeat (count vector) 0.0))
      (v* vector (/ 1.0 magnitude)))))
(defn clamp [lo value hi] (max lo (min value hi)))
(defn mix [left right amount]
  (+ (* left (- 1.0 amount)) (* right amount)))

(def identity-mat4
  [1.0 0.0 0.0 0.0
   0.0 1.0 0.0 0.0
   0.0 0.0 1.0 0.0
   0.0 0.0 0.0 1.0])

(defn mat4-mul [left right]
  (vec
   (for [row (range 4) column (range 4)]
     (reduce +
             (for [index (range 4)]
               (* (nth left (+ (* row 4) index))
                  (nth right (+ (* index 4) column))))))))

(defn mat4-translation [[x y z]]
  [1.0 0.0 0.0 x
   0.0 1.0 0.0 y
   0.0 0.0 1.0 z
   0.0 0.0 0.0 1.0])

(defn mat4-scale [[x y z]]
  [x 0.0 0.0 0.0
   0.0 y 0.0 0.0
   0.0 0.0 z 0.0
   0.0 0.0 0.0 1.0])

(defn mat4-quaternion [[x y z w]]
  (let [xx (* x x) yy (* y y) zz (* z z)
        xy (* x y) xz (* x z) yz (* y z)
        wx (* w x) wy (* w y) wz (* w z)]
    [(- 1.0 (* 2.0 (+ yy zz))) (* 2.0 (- xy wz))       (* 2.0 (+ xz wy))       0.0
     (* 2.0 (+ xy wz))       (- 1.0 (* 2.0 (+ xx zz))) (* 2.0 (- yz wx))       0.0
     (* 2.0 (- xz wy))       (* 2.0 (+ yz wx))       (- 1.0 (* 2.0 (+ xx yy))) 0.0
     0.0                     0.0                     0.0                       1.0]))

(defn trs-matrix [{:keys [translation rotation scale]}]
  (mat4-mul (mat4-translation translation)
            (mat4-mul (mat4-quaternion rotation)
                      (mat4-scale scale))))

(defn transform-point [matrix [x y z]]
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

(defn transform-direction [matrix [x y z]]
  [ (+ (* (nth matrix 0) x) (* (nth matrix 1) y) (* (nth matrix 2) z))
    (+ (* (nth matrix 4) x) (* (nth matrix 5) y) (* (nth matrix 6) z))
    (+ (* (nth matrix 8) x) (* (nth matrix 9) y) (* (nth matrix 10) z))])

(defn inverse-mat4
  "Gauss-Jordan inverse over a row-major 4x4 vector."
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
  "Return object-id -> effective matrix. Canonical validation rejects missing
   parents and cycles before recursion; sorted ids make the result stable
   across map insertion order."
  [region]
  (let [region (material/validate-region! region)
        scene (:scene region)
        !memo (atom {})]
    (letfn [(effective [object-id]
              (or (get @!memo object-id)
                  (let [object (get scene object-id)
                        local (trs-matrix (:transform object))
                        result (if-let [parent (:parent object)]
                                 (mat4-mul (effective parent) local)
                                 local)]
                    (swap! !memo assoc object-id result)
                    result)))]
      (doseq [object-id (sort-by pr-str (keys scene))]
        (effective object-id))
      @!memo)))

(defn affected-descendants
  "Affected hierarchy set including object-id."
  [scene object-id]
  (loop [frontier [object-id] result #{}]
    (if-let [parent (first frontier)]
      (let [children (->> scene
                          (keep (fn [[candidate row]]
                                  (when (= parent (:parent row)) candidate)))
                          (sort-by pr-str))]
        (recur (into (vec (rest frontier)) children) (conj result parent)))
      result)))

(defn- mesh-row [positions normals indices]
  {:positions (vec (mapcat identity positions))
   :normals (vec (mapcat identity normals))
   :indices (vec indices)})

(defn- box-mesh [{[sx sy sz] :size}]
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

(defn- plane-mesh [{[sx sz] :size}]
  (let [x (/ sx 2.0) z (/ sz 2.0)]
    (mesh-row [[(- x) 0.0 z] [x 0.0 z] [x 0.0 (- z)] [(- x) 0.0 (- z)]]
              (repeat 4 [0.0 1.0 0.0])
              [0 1 2 0 2 3])))

(defn- sphere-mesh [{:keys [radius width-segments height-segments]}]
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

(defn- radial-mesh [{:keys [radius height radial-segments]} cone?]
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

(defn- torus-mesh [{:keys [radius tube radial-segments tubular-segments]}]
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
  "Deterministic indexed-triangle projection of a validated primitive row."
  [mesh]
  (let [object (material/canonical-object
                {:object/id :primitive
                 :object/kind :mesh
                 :parent nil
                 :transform material/default-transform
                 :provenance {:asserted-by :derive}
                 :mesh mesh
                 :material material/default-material})
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

(defn object-mesh [object]
  (when (= :mesh (:object/kind object))
    (if (= :indexed-triangles (get-in object [:mesh :kind]))
      (select-keys (:mesh object) [:positions :normals :indices])
      (primitive-mesh (:mesh object)))))

(defn- unpack-vec3 [flat index]
  (subvec (vec flat) (* index 3) (+ (* index 3) 3)))

(defn transformed-triangles [object effective-matrix]
  (when-let [{:keys [positions indices]} (object-mesh object)]
    (mapv
     (fn [triangle-index]
       (let [offset (* triangle-index 3)
             ia (nth indices offset)
             ib (nth indices (inc offset))
             ic (nth indices (+ offset 2))
             a (transform-point effective-matrix (unpack-vec3 positions ia))
             b (transform-point effective-matrix (unpack-vec3 positions ib))
             c (transform-point effective-matrix (unpack-vec3 positions ic))]
         {:object-id (:object/id object)
          :triangle-index triangle-index
          :a a :b b :c c
          :normal (normalize (cross (v- b a) (v- c a)))}))
     (range (quot (count indices) 3)))))

(defn- points-aabb [points]
  (when (seq points)
    (reduce (fn [{:keys [min max]} point]
              {:min (mapv clojure.core/min min point)
               :max (mapv clojure.core/max max point)})
            {:min (first points) :max (first points)}
            (rest points))))

(defn- triangle-aabb [{:keys [a b c]}]
  (points-aabb [a b c]))

(defn- merge-aabb [left right]
  (cond
    (nil? left) right
    (nil? right) left
    :else {:min (mapv clojure.core/min (:min left) (:min right))
           :max (mapv clojure.core/max (:max left) (:max right))}))

(defn- triangle-centroid [{:keys [a b c]}]
  (v* (v+ a b c) (/ 1.0 3.0)))

(defn build-bvh
  "Stable median BVH. Leaf topology is retained by transform-only refits."
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

(defn refit-bvh [bvh triangles-by-object affected-object-ids]
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
  "Moller-Trumbore with boundary counted as hit."
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

(defn- hit-before? [candidate current]
  (or (nil? current)
      (< (:t candidate) (- (:t current) ray-epsilon))
      (and (<= (Math/abs (double (- (:t candidate) (:t current))))
               ray-epsilon)
           (neg? (compare (pr-str (:object-id candidate))
                          (pr-str (:object-id current)))))))

(defn query-bvh
  "Nearest positive t; object-id order resolves coincident ties."
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

(defn look-at [eye target up]
  (let [z (normalize (v- eye target))
        x (normalize (cross up z))
        y (cross z x)]
    [(nth x 0) (nth x 1) (nth x 2) (- (dot x eye))
     (nth y 0) (nth y 1) (nth y 2) (- (dot y eye))
     (nth z 0) (nth z 1) (nth z 2) (- (dot z eye))
     0.0 0.0 0.0 1.0]))

(defn orbit-eye [{:keys [pivot distance yaw pitch]}]
  (let [cp (Math/cos pitch)]
    (v+ pivot
        [(* distance cp (Math/sin yaw))
         (* distance (Math/sin pitch))
         (* distance cp (Math/cos yaw))])))

(defn perspective-matrix [fov-y-deg aspect near far]
  (let [f (/ 1.0 (Math/tan (/ (* fov-y-deg Math/PI) 360.0)))]
    [(/ f aspect) 0.0 0.0 0.0
     0.0 f 0.0 0.0
     0.0 0.0 (/ far (- near far)) (/ (* near far) (- near far))
     0.0 0.0 -1.0 0.0]))

(defn ortho-matrix [scale aspect near far]
  (let [half-y (/ scale 2.0)
        half-x (* half-y aspect)]
    [(/ 1.0 half-x) 0.0 0.0 0.0
     0.0 (/ 1.0 half-y) 0.0 0.0
     0.0 0.0 (/ 1.0 (- near far)) (/ near (- near far))
     0.0 0.0 0.0 1.0]))

(defn camera-matrices [view viewport]
  (let [view (material/canonical-view view)
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

(defn ray-from-region-point [camera [local-x local-y]]
  (let [[width height] (:viewport camera)
        ndc-x (- (* 2.0 (/ local-x width)) 1.0)
        ndc-y (- 1.0 (* 2.0 (/ local-y height)))
        inverse (:inverse-view-projection camera)
        near (transform-point inverse [ndc-x ndc-y 0.0])
        far (transform-point inverse [ndc-x ndc-y 1.0])]
    {:origin near :direction (normalize (v- far near))
     :ndc [ndc-x ndc-y]}))

(defn project-point [camera point]
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

(defn derive-instance-row [object effective-matrix]
  {:object-id (:object/id object)
   :kind (:object/kind object)
   :matrix effective-matrix
   :material (:material object)
   :light (:light object)
   :placement (case (:object/kind object)
                :text (:text object)
                :ink (:ink object)
                nil)
   :transparent? (or (contains? #{:text :ink} (:object/kind object))
                     (and (= :mesh (:object/kind object))
                          (< (get-in object [:material :base-color :rgba 3] 1.0)
                             1.0)))})

(defn derive-scene [region]
  (let [region (material/validate-region! region)
        effective (compose-hierarchy region)
        object-ids (sort-by pr-str (keys (:scene region)))
        instances-by-object
        (into {} (map (fn [object-id]
                        [object-id
                         (derive-instance-row (get-in region [:scene object-id])
                                              (get effective object-id))]))
              object-ids)
        triangles-by-object
        (into {} (keep (fn [object-id]
                         (let [object (get-in region [:scene object-id])]
                           (when (= :mesh (:object/kind object))
                             [object-id
                              (transformed-triangles object
                                                     (get effective object-id))]))))
              object-ids)
        triangles (vec (mapcat #(get triangles-by-object % []) object-ids))]
    {:derive/version scene-algorithm-version
     :region region
     :effective-transforms effective
     :instances-by-object instances-by-object
     :instances (mapv instances-by-object object-ids)
     :triangles-by-object triangles-by-object
     :bvh (build-bvh triangles)
     :receipt {:full-rebuilds 1
               :instance-uploads (count object-ids)
               :bvh-build-triangles (count triangles)
               :bvh-refits 0
               :region-encodes 1}}))

(defn- compose-affected
  "Recompose only an already-canonical hierarchy subset. Unaffected parents
  are read from the retained evaluated scene; affected parents are resolved
  recursively so input map order cannot change the result."
  [scene prior-effective affected]
  (let [!memo (atom {})]
    (letfn [(effective [object-id]
              (if-not (contains? affected object-id)
                (get prior-effective object-id)
                (or (get @!memo object-id)
                    (let [object (get scene object-id)
                          local (trs-matrix (:transform object))
                          result (if-let [parent (:parent object)]
                                   (mat4-mul (effective parent) local)
                                   local)]
                      (swap! !memo assoc object-id result)
                      result))))]
      (doseq [object-id (sort-by pr-str affected)]
        (effective object-id))
      (merge prior-effective @!memo))))

(defn- maintain-affected
  [maintained next-region affected]
  (let [effective (compose-affected (:scene next-region)
                                    (:effective-transforms maintained)
                                    affected)
        instances-by-object
        (reduce
         (fn [rows id]
           (assoc rows id
                  (derive-instance-row (get-in next-region [:scene id])
                                       (get effective id))))
         (:instances-by-object maintained) affected)
        triangles-by-object
        (reduce
         (fn [rows id]
           (let [object (get-in next-region [:scene id])]
             (if (= :mesh (:object/kind object))
               (assoc rows id (transformed-triangles object
                                                    (get effective id)))
               rows)))
         (:triangles-by-object maintained) affected)
        bvh (refit-bvh (:bvh maintained) triangles-by-object affected)
        object-ids (sort-by pr-str (keys (:scene next-region)))]
    (assoc maintained
           :region next-region
           :effective-transforms effective
           :instances-by-object instances-by-object
           :instances (mapv instances-by-object object-ids)
           :triangles-by-object triangles-by-object
           :bvh bvh
           :receipt {:full-rebuilds 0
                     :instance-uploads (count affected)
                     :bvh-build-triangles 0
                     :bvh-refits (count (filter triangles-by-object affected))
                     :region-encodes 1
                     :affected-object-ids affected})))

(defn maintain-transforms
  "Apply a batch of transient or settled object transforms to the retained
  evaluated scene. Geometry topology is preserved: only affected hierarchy
  rows are recomposed and the existing BVH topology is refit."
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
                     [object-id (material/canonical-transform transform)]))
              transforms-by-object)
        changed
        (into {}
              (remove (fn [[object-id transform]]
                        (= transform (get-in scene [object-id :transform]))))
              transforms)]
    (if (empty? changed)
      (assoc maintained :receipt
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
  "Pick the nearest BVH surface, otherwise the region background."
  [{:keys [maintained camera region-point]}]
  (let [region (:region maintained)
        camera (or camera
                   (camera-matrices (:view-default region)
                                    [(get-in region [:extent :width])
                                     (get-in region [:extent :height])]))
        ray (ray-from-region-point camera region-point)]
    (if-let [hit (query-bvh (:bvh maintained) ray)]
      (select-keys (assoc hit :route :object)
                   [:route :object-id :point3 :normal :t
                    :triangle-index :boundary?])
      {:route :region-background
       :region-id (:region/id region)})))

(defn shadow-light-space
  "Pinned :region3d/shadow v1 facts for the first shadow-casting directional
   light by object-id. Returns nil for degenerate/no-mesh scenes."
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
      (let [matrix (get-in maintained [:effective-transforms light-id])
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
          (let [padding (mapv #(* % (:bounds-padding material/shadow-constants))
                              extent)
                minimum (v- (:min bounds) padding)
                maximum (v+ (:max bounds) padding)
                texel [(/ (- (nth maximum 0) (nth minimum 0))
                          (:size material/shadow-constants))
                       (/ (- (nth maximum 1) (nth minimum 1))
                          (:size material/shadow-constants))]
                snapped-min [(Math/floor (/ (nth minimum 0) (nth texel 0)))
                             (Math/floor (/ (nth minimum 1) (nth texel 1)))]
                snapped-min (mapv * snapped-min texel)
                snapped-max [(+ (nth snapped-min 0)
                                (- (nth maximum 0) (nth minimum 0)))
                             (+ (nth snapped-min 1)
                                (- (nth maximum 1) (nth minimum 1)))] ]
            {:algorithm-version material/shadow-algorithm-version
             :light-id light-id :view view
             :bounds {:min [(nth snapped-min 0) (nth snapped-min 1)
                            (nth minimum 2)]
                      :max [(nth snapped-max 0) (nth snapped-max 1)
                            (nth maximum 2)]}
             :texel-world texel
             :constants material/shadow-constants}))))))
