(ns app.client.workspace.containers
  "Pure container registry and W2-A affine composition.

   A container owns one canonical six-number affine in SVG/CSS order:
   [a b c d tx ty], where x' = a*x + c*y + tx and
   y' = b*x + d*y + ty.  Container transforms compose through the parent
   chain once; the resulting value is shared by GPU paint, CPU pick, bounds,
   clipping/culling projections, and context receipts.

   Semantic container ids never index the GPU table.  Each live container has
   a compact, stable :transport-slot.  That indirection is the Q8 transport
   seam: sparse material ids do not amplify the 32-byte storage table.  cid 0
   and transport slot 0 are permanently reserved for the identity world
   container.")

(def identity-affine
  "Canonical identity [a b c d tx ty]."
  [1.0 0.0 0.0 1.0 0.0 0.0])

(def ^:private affine-epsilon 1.0e-12)

(defn- sqrt [x]
  #?(:clj (Math/sqrt (double x))
     :cljs (js/Math.sqrt x)))

(defn- cos [x]
  #?(:clj (Math/cos (double x))
     :cljs (js/Math.cos x)))

(defn- sin [x]
  #?(:clj (Math/sin (double x))
     :cljs (js/Math.sin x)))

(defn- finite-number? [x]
  #?(:clj (and (number? x) (Double/isFinite (double x)))
     :cljs (and (number? x) (js/Number.isFinite x))))

(defn- validate-affine [affine]
  (when-not (and (vector? affine)
                 (= 6 (count affine))
                 (every? finite-number? affine))
    (throw (ex-info "affine must be six finite numbers [a b c d tx ty]"
                    {:affine affine})))
  (mapv double affine))

(defn- spec->affine
  "Normalize the public transform vocabulary to the one canonical affine.
   :affine is the full-general path.  The legacy :x/:y/:scale path remains an
   adapter; :rotation and :scale-x/:scale-y add non-axis-aligned convenience
   without becoming a second stored representation."
  [spec]
  (if (contains? spec :affine)
    (validate-affine (:affine spec))
    (let [x (double (or (:x spec) 0.0))
          y (double (or (:y spec) 0.0))
          uniform (double (or (:scale spec) 1.0))
          sx (double (or (:scale-x spec) uniform))
          sy (double (or (:scale-y spec) uniform))
          theta (double (or (:rotation spec) 0.0))
          ct (cos theta)
          st (sin theta)]
      (validate-affine [(* ct sx) (* st sx) (* (- st) sy) (* ct sy) x y]))))

(def ^:private root-container
  {:parent nil
   :affine identity-affine
   :camera :world
   :layer 0
   :sibling-rank 0
   :effects nil
   :transport-slot 0})

(defn empty-registry
  "A registry holding only the reserved identity container and its compact
   transport allocator."
  []
  {:containers {0 root-container}
   :next-transport-slot 1
   :free-transport-slots (sorted-set)})

(defn- allocate-transport-slot [reg]
  (if-let [slot (first (:free-transport-slots reg))]
    [(update reg :free-transport-slots disj slot) slot]
    (let [slot (or (:next-transport-slot reg) 1)]
      [(assoc reg :next-transport-slot (inc slot)) slot])))

(defn add-container
  "Register a new container. :affine accepts [a b c d tx ty].  The existing
   :x/:y/:scale vocabulary remains behavior-identical; :rotation and
   :scale-x/:scale-y are convenience inputs.  The assigned transport slot is
   compact and independent of cid."
  [reg cid {:keys [parent camera layer sibling-rank effects] :as spec
            :or {parent nil camera nil layer 0 sibling-rank 0}}]
  (when (= cid 0)
    (throw (ex-info "cid 0 is reserved (identity/world) and cannot be added"
                    {:cid cid})))
  (when (contains? (:containers reg) cid)
    (throw (ex-info "container already exists" {:cid cid})))
  (when (and (some? parent) (not (contains? (:containers reg) parent)))
    (throw (ex-info "parent container does not exist" {:cid cid :parent parent})))
  (let [[reg slot] (allocate-transport-slot reg)]
    (assoc-in reg [:containers cid]
              {:parent parent
               :affine (spec->affine spec)
               :camera camera
               :layer layer
               :sibling-rank sibling-rank
               :effects effects
               :transport-slot slot})))

(defn set-effects
  "Replace one container's session effect declaration. Grammar validation is
   owned by frame-effects at plan compile; the registry remains a generic data
   carrier beside transforms."
  [reg cid effects]
  (when-not (contains? (:containers reg) cid)
    (throw (ex-info "container does not exist" {:cid cid})))
  (assoc-in reg [:containers cid :effects] effects))

(defn transport-slot
  "Return cid's compact GPU transport slot, or nil for an unknown cid."
  [reg cid]
  (get-in reg [:containers cid :transport-slot]))

(defn- conformal-scale
  "Return the uniform scale of an orthogonal equal-length matrix, else nil.
   The legacy :scale mutator is intentionally fail-closed on a general affine:
   callers must then supply the exact replacement :affine."
  [[a b c d _tx _ty]]
  (let [sx (sqrt (+ (* a a) (* b b)))
        sy (sqrt (+ (* c c) (* d d)))
        dot (+ (* a c) (* b d))]
    (when (and (> sx affine-epsilon)
               (> sy affine-epsilon)
               (< (abs (- sx sy)) affine-epsilon)
               (< (abs dot) affine-epsilon))
      sx)))

(defn- update-legacy-transform [affine {:keys [x y scale]}]
  (let [[a b c d tx ty] affine
        [a b c d]
        (if (some? scale)
          (let [old-scale (conformal-scale affine)]
            (when-not old-scale
              (throw (ex-info "legacy :scale is ambiguous for a non-conformal affine; supply :affine"
                              {:affine affine :scale scale})))
            (let [ratio (/ (double scale) old-scale)]
              [(* a ratio) (* b ratio) (* c ratio) (* d ratio)]))
          [a b c d])]
    (validate-affine [a b c d
                      (if (some? x) (double x) tx)
                      (if (some? y) (double y) ty)])))

(defn set-transform
  "Partially update a transform. :affine replaces the canonical value.  The
   legacy :x/:y/:scale adapter remains for existing drag/zoom callers; scaling
   a sheared or non-uniform matrix is rejected as ambiguous."
  [reg cid t]
  (when (= cid 0)
    (throw (ex-info "cid 0 is reserved and cannot be mutated" {:cid cid})))
  (when-not (contains? (:containers reg) cid)
    (throw (ex-info "unknown container" {:cid cid})))
  (let [old (get-in reg [:containers cid :affine])
        affine (if (contains? t :affine)
                 (validate-affine (:affine t))
                 (update-legacy-transform old t))]
    (assoc-in reg [:containers cid :affine] affine)))

(defn remove-container
  "Drop a childless container and return its compact transport slot to the
   allocator.  The slot is stable for the container's whole live lifetime."
  [reg cid]
  (when (= cid 0)
    (throw (ex-info "cid 0 is reserved and cannot be removed" {:cid cid})))
  (when-not (contains? (:containers reg) cid)
    (throw (ex-info "unknown container" {:cid cid})))
  (when-let [child (some (fn [[c m]] (when (= cid (:parent m)) c))
                         (:containers reg))]
    (throw (ex-info "cannot remove a container with children"
                    {:cid cid :child child})))
  (let [slot (transport-slot reg cid)]
    (cond-> (update reg :containers dissoc cid)
      (some? slot) (update :free-transport-slots (fnil conj (sorted-set)) slot))))

(defn compose-affines
  "Compose parent and child affines (parent after child)."
  [[pa pb pc pd ptx pty] [ca cb cc cd ctx cty]]
  [(+ (* pa ca) (* pc cb))
   (+ (* pb ca) (* pd cb))
   (+ (* pa cc) (* pc cd))
   (+ (* pb cc) (* pd cd))
   (+ (* pa ctx) (* pc cty) ptx)
   (+ (* pb ctx) (* pd cty) pty)])

(def ^:private world-base
  {:affine identity-affine :camera :world :stack-path []})

(defn- fallback-transport-slots
  "Old hand-built registries in tests/doc fixtures may predate allocator
   metadata.  Give them deterministic compact slots without using sparse cids."
  [containers]
  (into {0 0}
        (map-indexed (fn [i cid] [cid (inc i)]))
        (sort-by pr-str (remove #(= 0 %) (keys containers)))))

(defn- compose-one [containers fallback-slots cache seen cid]
  (cond
    (contains? cache cid) [cache (get cache cid)]
    (contains? seen cid) (throw (ex-info "container parent cycle" {:cid cid}))
    :else
    (let [c (get containers cid)]
      (when-not c
        (throw (ex-info "container does not exist during composition" {:cid cid})))
      (let [parent (:parent c)
            [cache p] (if (nil? parent)
                        [cache world-base]
                        (compose-one containers fallback-slots cache (conj seen cid) parent))
            local (if (contains? c :affine) (:affine c) (spec->affine c))
            affine (compose-affines (:affine p) local)
            eff {:affine affine
                 :camera (or (:camera c) (:camera p))
                 :layer (:layer c 0)
                 ;; W2-B consumes W2-A's canonical nested path directly and
                 ;; fills Contract O's third slot: semantic sibling rank.  No
                 ;; transform/order side table is introduced.
                 :stack-path (conj (:stack-path p)
                                   [cid (:layer c 0) (:sibling-rank c 0)])
                 :transport-slot (or (:transport-slot c)
                                     (get fallback-slots cid))}]
        [(assoc cache cid eff) eff]))))

(defn effective
  "Compose the registry to one absolute affine per cid.  Every entry carries
   the same affine read by CPU projections and GPU transport, plus its compact
   slot, camera flag, layer, and nested stack path."
  [reg]
  (let [containers (:containers reg)
        fallback-slots (fallback-transport-slots containers)
        cache (reduce (fn [m cid]
                        (first (compose-one containers fallback-slots m #{} cid)))
                      {}
                      (keys containers))]
    (reduce-kv
     (fn [m cid eff]
       (assoc m cid
              {:affine (:affine eff)
               :flags (if (= :screen (:camera eff)) 1 0)
               :layer (:layer eff)
               :stack-path (:stack-path eff)
               :transport-slot (:transport-slot eff)}))
     {}
     cache)))

(defn determinant [[a b c d _tx _ty]]
  (- (* a d) (* b c)))

(defn forward-point
  "Map a container-local point through an effective affine."
  [{[a b c d tx ty] :affine} [x y]]
  [(+ (* a x) (* c y) tx)
   (+ (* b x) (* d y) ty)])

(defn inverse-point
  "Map a world/screen point into container-local space. Singular transforms
   fail closed; pick must never invent an inside result from no inverse."
  [{[a b c d tx ty] :affine :as eff} [px py]]
  (when-not (:affine eff)
    (throw (ex-info "effective transform has no canonical :affine" {:effective eff})))
  (let [det (- (* a d) (* b c))]
    (when (< (abs det) affine-epsilon)
      (throw (ex-info "singular container affine has no inverse"
                      {:affine (:affine eff) :determinant det})))
    (let [dx (- px tx)
          dy (- py ty)]
      [(/ (- (* d dx) (* c dy)) det)
       (/ (+ (* (- b) dx) (* a dy)) det)])))

(defn transform-bounds
  "Conservative axis-aligned bounds of a local rect after the exact affine.
   All four corners participate, so rotation, reflection, shear, and nested
   non-uniform scale share one bounds/cull projection."
  [eff {:keys [x y w h]}]
  (let [points [(forward-point eff [x y])
                (forward-point eff [(+ x w) y])
                (forward-point eff [x (+ y h)])
                (forward-point eff [(+ x w) (+ y h)])]
        xs (map first points)
        ys (map second points)
        x0 (apply min xs)
        y0 (apply min ys)
        x1 (apply max xs)
        y1 (apply max ys)]
    {:x x0 :y y0 :w (- x1 x0) :h (- y1 y0)}))

(defn screen-bounds
  "Project local bounds through container affine and the exact renderer camera
   law: screen = world*zoom + pan. Screen-camera containers bypass world camera."
  [eff bounds camera]
  (let [{:keys [x y w h]} (transform-bounds eff bounds)
        screen? (= 1 (:flags eff))
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
