(ns app.client.engine.transform
  "Compose one coordinate system for CPU and GPU consumers.

   Input: a group registry, local transforms/points/bounds, and sometimes a
   camera. Output: updated registry values or projected geometry. The
   registry owns group identities and stable compact GPU indexes; this
   namespace does not retain it globally. It normalizes convenience
   transforms to six-number affines, composes parent chains, and uses the
   same affine representation for projections and upload.

   Folder map: README.md."
  (:require [app.client.engine.schema :as schema]))

;; Semantic group IDs and compact GPU buffer indexes are distinct domains.
;; Group 0 is the reserved identity; world-base carries neutral projection
;; state.
(def identity-affine
  "Canonical identity [a b c d tx ty]."
  [1.0 0.0 0.0 1.0 0.0 0.0])

(def ^:private affine-epsilon 1.0e-12)

(defn- sqrt
  "Scalar → its square root.

   CLJ/CLJS numeric adapter."
  [x]
  #?(:clj (Math/sqrt (double x))
     :cljs (js/Math.sqrt x)))

(defn- cos
  "Scalar → its cosine.

   CLJ/CLJS numeric adapter."
  [x]
  #?(:clj (Math/cos (double x))
     :cljs (js/Math.cos x)))

(defn- sin
  "Scalar → its sine.

   CLJ/CLJS numeric adapter."
  [x]
  #?(:clj (Math/sin (double x))
     :cljs (js/Math.sin x)))

(defn- six-finite?
  "Value → valid six-number affine?

   Shape plus shared numeric checks."
  [value]
  (and (vector? value)
       (= 6 (count value))
       (every? schema/finite-number? value)))

(defn- affine-xor-legacy?
  "Group spec → whether affine and convenience fields are not mixed.

   Key-presence check. Resolves representation ambiguity at ingress."
  [spec]
  (or (not (contains? spec :affine))
      (not-any? #(contains? spec %)
                [:x :y :scale :scale-x :scale-y :rotation])))

(def group
  "The declared public schema for a group specification."
  {:optional #{:affine :x :y :scale :scale-x :scale-y :rotation
               :parent :camera}
   :validators {:affine six-finite?
                :x schema/finite-number?
                :y schema/finite-number?
                :scale schema/finite-number?
                :scale-x schema/finite-number?
                :scale-y schema/finite-number?
                :rotation schema/finite-number?
                :parent any?
                :camera #{:world :screen}}
   :form-validators [{:valid? affine-xor-legacy?
                      :error-type :transform/affine-form}]})

(defn- spec->affine
  "Checked spec → [a b c d tx ty].

   Copies explicit affine or composes scale/rotation/translation. One stored
   representation."
  [spec]
  (if (contains? spec :affine)
    (mapv double (:affine spec))
    (let [x (double (or (:x spec) 0.0))
          y (double (or (:y spec) 0.0))
          uniform (double (or (:scale spec) 1.0))
          sx (double (or (:scale-x spec) uniform))
          sy (double (or (:scale-y spec) uniform))
          theta (double (or (:rotation spec) 0.0))
          ct (cos theta)
          st (sin theta)]
      [(* ct sx) (* st sx) (* (- st) sy) (* ct sy) x y])))

(def ^:private root-group
  {:parent nil
   :affine identity-affine
   :camera :world
   :buffer-index 0})

(defn empty-registry
  "No input → registry with identity group 0 and allocator.

   Pure constructor."
  []
  {:groups {0 root-group}
   :next-buffer-index 1
   :free-buffer-indexes (sorted-set)})

(defn- allocate-buffer-index
  "Registry → [updated-registry index].

   Reuses the smallest free index, otherwise increments. Intended for stable
   compact allocation."
  [reg]
  (if-let [buffer-index (first (:free-buffer-indexes reg))]
    [(update reg :free-buffer-indexes disj buffer-index) buffer-index]
    (let [buffer-index (or (:next-buffer-index reg) 1)]
      [(assoc reg :next-buffer-index (inc buffer-index)) buffer-index])))

(defn add-group
  "Registry, ID, spec → new registry; throws for reserved/duplicate ID or
   missing parent.

   Validates then normalizes and allocates. Parents must exist before
   children."
  [reg group-id spec]
  (when (= group-id 0)
    (throw (ex-info "group-id 0 is reserved (identity/world) and cannot be added"
                    {:error-type :transform/reserved-group-id :group-id group-id})))
  (when (contains? (:groups reg) group-id)
    (throw (ex-info "group already exists"
                    {:error-type :transform/duplicate-group :group-id group-id})))
  (let [{:keys [parent camera]
         :or {parent nil camera nil}
         :as spec} (schema/check group spec)]
    (when (and (some? parent) (not (contains? (:groups reg) parent)))
      (throw (ex-info "parent group does not exist"
                      {:error-type :transform/parent-missing
                       :group-id group-id :parent parent})))
    (let [[reg buffer-index] (allocate-buffer-index reg)]
    (assoc-in reg [:groups group-id]
              {:parent parent
               :affine (spec->affine spec)
               :camera camera
               :buffer-index buffer-index}))))

(defn- assigned-buffer-index
  "Registry, ID → index or nil."
  [reg group-id]
  (get-in reg [:groups group-id :buffer-index]))

(defn set-transform
  "Registry, existing nonroot ID, transform spec → new registry.

   Replaces only affine. Schema accepts parent/camera fields but this
   operation does not apply them; callers must understand the narrower
   contract."
  [reg group-id t]
  (when (= group-id 0)
    (throw (ex-info "group-id 0 is reserved and cannot be mutated" {:group-id group-id})))
  (when-not (contains? (:groups reg) group-id)
    (throw (ex-info "unknown group" {:group-id group-id})))
  (assoc-in reg [:groups group-id :affine]
            (spec->affine (schema/check group t))))

(defn remove-group
  "Registry and childless nonroot ID → registry with index returned to free
   set.

   Scans for children before removal. O(number of groups) child check."
  [reg group-id]
  (when (= group-id 0)
    (throw (ex-info "group-id 0 is reserved and cannot be removed" {:group-id group-id})))
  (when-not (contains? (:groups reg) group-id)
    (throw (ex-info "unknown group" {:group-id group-id})))
  (when-let [child (some (fn [[c m]] (when (= group-id (:parent m)) c))
                         (:groups reg))]
    (throw (ex-info "cannot remove a group with children"
                    {:group-id group-id :child child})))
  (let [buffer-index (assigned-buffer-index reg group-id)]
    (cond-> (update reg :groups dissoc group-id)
      (some? buffer-index) (update :free-buffer-indexes (fnil conj (sorted-set)) buffer-index))))

(defn compose-affines
  "Parent and child affines → parent-after-child affine.

   Direct six-coefficient arithmetic."
  [[pa pb pc pd ptx pty] [ca cb cc cd ctx cty]]
  [(+ (* pa ca) (* pc cb))
   (+ (* pb ca) (* pd cb))
   (+ (* pa cc) (* pc cd))
   (+ (* pb cc) (* pd cd))
   (+ (* pa ctx) (* pc cty) ptx)
   (+ (* pb ctx) (* pd cty) pty)])

(def ^:private world-base
  {:affine identity-affine :camera :world})

(defn- fallback-buffer-indexes
  "Hand-built group map → deterministic ID/index mapping.

   Sorts IDs by printed representation. Supports registries lacking
   allocator metadata, adding a second admission path."
  [groups]
  (into {0 0}
        (map-indexed (fn [i group-id] [group-id (inc i)]))
        (sort-by pr-str (remove #(= 0 %) (keys groups)))))

(defn- compose-one
  "Groups, fallback indexes, cache, ancestor set, ID → [cache
   world-transform]; throws for cycles/missing groups.

   Recursive parent composition with per-call memoization. Very deep trees
   depend on call-stack capacity."
  [groups fallback-buffer-indexes cache seen group-id]
  (cond
    (contains? cache group-id) [cache (get cache group-id)]
    (contains? seen group-id) (throw (ex-info "group parent cycle" {:group-id group-id}))
    :else
    (let [c (get groups group-id)]
      (when-not c
        (throw (ex-info "group does not exist during composition" {:group-id group-id})))
      (let [parent (:parent c)
            [cache p] (if (nil? parent)
                        [cache world-base]
                        (compose-one groups fallback-buffer-indexes cache (conj seen group-id) parent))
            local (if (contains? c :affine)
                    (:affine c)
                    (spec->affine (schema/check group c)))
            affine (compose-affines (:affine p) local)
            world-transform {:affine affine
                             :camera (or (:camera c) (:camera p))
                             :buffer-index (or (:buffer-index c)
                                               (get fallback-buffer-indexes group-id))}]
        [(assoc cache group-id world-transform) world-transform]))))

(defn world-transforms
  "Registry → ID-to-absolute-affine/flags/index map.

   Composes every group, then emits compact transport rows. Intended for
   whole-registry derivation; no incremental cross-call cache."
  [reg]
  (let [groups (:groups reg)
        fallback-buffer-indexes (fallback-buffer-indexes groups)
        cache (reduce (fn [m group-id]
                        (first (compose-one groups fallback-buffer-indexes m #{} group-id)))
                      {}
                      (keys groups))]
    (reduce-kv
     (fn [m group-id world-transform]
       (assoc m group-id
              {:affine (:affine world-transform)
               :flags (if (= :screen (:camera world-transform)) 1 0)
               :buffer-index (:buffer-index world-transform)}))
     {}
     cache)))

(defn world-transform-scale
  "World-transform map and ID → maximum axis length, defaulting to identity.

   Computes the two affine column lengths. This is a scale proxy, not the
   maximum singular value under shear; quality-sensitive users need that
   distinction."
  [world-transforms group-id]
  (let [[a b c d] (or (get-in world-transforms [group-id :affine])
                      [1.0 0.0 0.0 1.0])
        sx (sqrt (+ (* a a) (* b b)))
        sy (sqrt (+ (* c c) (* d d)))]
    (max sx sy)))

(defn buffer-index
  "World-transform map and ID → integer index; throws if unknown.

   Checked lookup. No silent alias to identity."
  [world-transforms group-id]
  (let [buffer-index (get-in world-transforms [group-id :buffer-index] ::missing)]
    (when (= ::missing buffer-index)
      (throw (ex-info "Draw item names an unknown group"
                      {:error-type :transform/unknown-group
                       :group-id group-id})))
    (int buffer-index)))

(defn determinant
  "Affine → 2×2 determinant."
  [[a b c d _tx _ty]]
  (- (* a d) (* b c)))

(defn forward-point
  "World transform and local point → transformed point.

   Applies affine directly. Expects valid affine."
  [{[a b c d tx ty] :affine} [x y]]
  [(+ (* a x) (* c y) tx)
   (+ (* b x) (* d y) ty)])

(defn inverse-point
  "World transform and transformed point → local point; throws for
   missing/singular affine.

   Explicit inverse with determinant epsilon. Singular geometry cannot
   produce an invented pick."
  [{[a b c d tx ty] :affine :as world-transform} [px py]]
  (when-not (:affine world-transform)
    (throw (ex-info "world transform has no canonical :affine" {:world-transform world-transform})))
  (let [det (- (* a d) (* b c))]
    (when (< (abs det) affine-epsilon)
      (throw (ex-info "singular group affine has no inverse"
                      {:affine (:affine world-transform) :determinant det})))
    (let [dx (- px tx)
          dy (- py ty)]
      [(/ (- (* d dx) (* c dy)) det)
       (/ (+ (* (- b) dx) (* a dy)) det)])))

(defn transform-bounds
  "Affine and local rectangle → world axis-aligned bounds.

   Projects all four corners. Intended for affine rectangles, including
   shear/reflection."
  [world-transform {:keys [x y w h]}]
  (let [points [(forward-point world-transform [x y])
                (forward-point world-transform [(+ x w) y])
                (forward-point world-transform [x (+ y h)])
                (forward-point world-transform [(+ x w) (+ y h)])]
        xs (map first points)
        ys (map second points)
        x0 (apply min xs)
        y0 (apply min ys)
        x1 (apply max xs)
        y1 (apply max ys)]
    {:x x0 :y y0 :w (- x1 x0) :h (- y1 y0)}))

(defn screen-bounds
  "Transform, local bounds, camera → screen bounds.

   Applies world camera unless the transform is screen-fixed; normalizes
   negative zoom bounds."
  [world-transform bounds camera]
  (let [{:keys [x y w h]} (transform-bounds world-transform bounds)
        screen? (= 1 (:flags world-transform))
        zoom (if screen? 1.0 (double (or (:zoom camera) (:scale camera) 1.0)))
        pan-x (if screen? 0.0 (double (or (:x camera) 0.0)))
        pan-y (if screen? 0.0 (double (or (:y camera) 0.0)))
        x0 (+ (* x zoom) pan-x)
        y0 (+ (* y zoom) pan-y)
        x1 (+ (* (+ x w) zoom) pan-x)
        y1 (+ (* (+ y h) zoom) pan-y)]
    {:x (min x0 x1)
     :y (min y0 y1)
     :w (abs (- x1 x0))
     :h (abs (- y1 y0))}))

(defn anchored-screen-rect
  "Bounds, transform, camera, pixel offsets → screen rectangle.

   Projects then adds offsets, including width/height offsets. Does not
   itself enforce positive final dimensions."
  [anchor-bounds world-transform camera offset]
  (let [{:keys [x y w h]} (screen-bounds world-transform anchor-bounds camera)]
    {:x (+ x (double (or (:x offset) 0.0)))
     :y (+ y (double (or (:y offset) 0.0)))
     :w (+ w (double (or (:w offset) 0.0)))
     :h (+ h (double (or (:h offset) 0.0)))}))
